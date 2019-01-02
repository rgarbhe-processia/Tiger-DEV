
/*
 ** IEFShowLockedByLabel
 **
 ** Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 **
 ** Program to display Revision Label
 */
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.IEFIntegAccessUtil;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PersonUtil;

public class IEFShowLockedByLabel_mxJPO {
    private HashMap integrationNameGCOTable = null;

    private MCADServerResourceBundle serverResourceBundle = null;

    private IEFIntegAccessUtil util = null;

    private String localeLanguage = null;

    private IEFGlobalCache cache = null;

    public IEFShowLockedByLabel_mxJPO(Context context, String[] args) throws Exception {
    }

    public Object getLockerId(Context context, String[] args) throws Exception {
        Vector columnCellContentList = new Vector();

        Vector lockerNames = (Vector) this.getHtmlString(context, args);

        for (int i = 0; i < lockerNames.size(); i++) {
            String lockerName = (String) lockerNames.elementAt(i);

            String lockerId = "";

            try {
                if (lockerName != null && !lockerName.equals(""))
                    lockerId = PersonUtil.getPersonObjectID(context, lockerName);
            } catch (Exception e) {
                System.out.println("Person Object Does not Exists : " + lockerName);
            }

            columnCellContentList.add(lockerId);
        }

        return columnCellContentList;
    }

    public Object getHtmlString(Context context, String[] args) throws Exception {
        Vector columnCellContentList = new Vector();

        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        MapList relBusObjPageList = (MapList) paramMap.get("objectList");
        cache = new IEFGlobalCache();

        HashMap paramList = (HashMap) paramMap.get("paramList");

        if (paramList != null) {
            if (paramList.containsKey("languageStr"))
                localeLanguage = paramList.get("languageStr").toString();
            else {
                localeLanguage = (String) paramList.get("LocaleLanguage");
                if (localeLanguage == null || localeLanguage.equals("")) {
                    Locale LocaleObj = (Locale) paramList.get("localeObj");

                    if (null != LocaleObj)
                        localeLanguage = LocaleObj.toString();
                }
            }

            integrationNameGCOTable = (HashMap) paramList.get("GCOTable");
        }

        if (integrationNameGCOTable == null)
            integrationNameGCOTable = (HashMap) paramMap.get("GCOTable");

        serverResourceBundle = new MCADServerResourceBundle(localeLanguage);

        util = new IEFIntegAccessUtil(context, serverResourceBundle, cache);

        Vector assignedIntegrations = util.getAssignedIntegrations(context);

        String[] objIds = new String[relBusObjPageList.size()];
        for (int i = 0; i < relBusObjPageList.size(); i++) {
            Map objDetails = (Map) relBusObjPageList.get(i);
            objIds[i] = (String) objDetails.get("id");
        }

        String REL_VERSION_OF = MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");
        String SELECT_ON_MAJOR = "from[" + REL_VERSION_OF + "].to.";
        String ATTR_SOURCE = "attribute[" + MCADMxUtil.getActualNameForAEFData(context, "attribute_Source") + "]";
        String IS_VERSION_OBJ = MCADMxUtil.getActualNameForAEFData(context, "attribute_IsVersionObject");
        String SELECT_ISVERSIONOBJ = "attribute[" + IS_VERSION_OBJ + "]";

        StringList busSelectionList = new StringList(6);

        busSelectionList.addElement("id");
        busSelectionList.addElement("type");
        busSelectionList.addElement(ATTR_SOURCE); // To get Integrations name.
        busSelectionList.addElement("locker"); // To get Integrations name.
        busSelectionList.addElement(SELECT_ON_MAJOR + "locker"); // from Minor.
        busSelectionList.addElement(SELECT_ISVERSIONOBJ);

        BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);

        for (int i = 0; i < buslWithSelectionList.size(); i++) {
            BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect) buslWithSelectionList.elementAt(i);
            String sIsVersion = busObjectWithSelect.getSelectData(SELECT_ISVERSIONOBJ);
            boolean isVersion = Boolean.valueOf(sIsVersion).booleanValue();

            String integrationName = null;

            String integrationSource = busObjectWithSelect.getSelectData(ATTR_SOURCE);
            String lockerName = "";

            if (integrationSource != null) {
                StringTokenizer integrationSourceTokens = new StringTokenizer(integrationSource, "|");

                if (integrationSourceTokens.hasMoreTokens())
                    integrationName = integrationSourceTokens.nextToken();
            }

            if (integrationName != null && integrationNameGCOTable.containsKey(integrationName) && assignedIntegrations.contains(integrationName)) {
                // [NDM] Start OP6
                // MCADGlobalConfigObject gco = (MCADGlobalConfigObject)integrationNameGCOTable.get(integrationName);

                // String busType = busObjectWithSelect.getSelectData("type");
                String busId = busObjectWithSelect.getSelectData("id");

                if (!isVersion)// gco.isMajorType(busType)) // [NDM] End OP6
                {
                    lockerName = busObjectWithSelect.getSelectData("locker");
                } else {
                    lockerName = busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "locker");
                }
            }
            lockerName = MCADMxUtil.getNLSName(context, "Person", lockerName, "", "", localeLanguage);
            columnCellContentList.add(lockerName);
        }

        return columnCellContentList;
    }
}
