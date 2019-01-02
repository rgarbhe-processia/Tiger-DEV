
/*
 ** IEFShowDesignLabel
 **
 ** Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 **
 ** Program to display design label.
 */
import java.util.HashMap;
import java.util.List;
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
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADUtil;
import com.matrixone.apps.domain.util.MapList;

public class IEFShowDesignLabel_mxJPO {
    private HashMap integrationNameGCOTable = null;

    private MCADServerResourceBundle serverResourceBundle = null;

    private MCADMxUtil util = null;

    private String localeLanguage = null;

    private IEFGlobalCache cache = null;

    private String REL_VERSION_OF = "";

    private String SELECT_ON_MAJOR = "";

    private String ATTR_SOURCE = "";

    public IEFShowDesignLabel_mxJPO(Context context, String[] args) throws Exception {
    }

    private List initialize(Context context, String[] args) throws Exception {
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        MapList relBusObjPageList = (MapList) paramMap.get("objectList");
        HashMap paramList = (HashMap) paramMap.get("paramList");

        if (paramList.containsKey("languageStr"))
            localeLanguage = paramList.get("languageStr").toString();
        else {
            localeLanguage = (String) paramList.get("LocaleLanguage");

            if (localeLanguage == null || localeLanguage.equals("")) {
                Locale LocaleObj = (Locale) paramList.get("localeObj");

                if (null != LocaleObj) {
                    localeLanguage = LocaleObj.toString();
                }
            }
        }

        if (paramList != null)
            integrationNameGCOTable = (HashMap) paramList.get("GCOTable");

        if (integrationNameGCOTable == null)
            integrationNameGCOTable = (HashMap) paramMap.get("GCOTable");

        serverResourceBundle = new MCADServerResourceBundle(localeLanguage);
        cache = new IEFGlobalCache();
        util = new MCADMxUtil(context, serverResourceBundle, cache);

        return relBusObjPageList;
    }

    public Object getHtmlString(Context context, String[] args) throws Exception {
        List relBusObjPageList = initialize(context, args);
        String[] objIds = new String[relBusObjPageList.size()];

        for (int i = 0; i < relBusObjPageList.size(); i++) {
            Map objDetails = (Map) relBusObjPageList.get(i);
            objIds[i] = (String) objDetails.get("id");
        }

        return getHtmlStringForTable(context, objIds);
    }

    public Object getHtmlStringForFrameworkTable(Context context, String[] args) throws Exception {
        List relBusObjPageList = initialize(context, args);
        String[] objIds = new String[relBusObjPageList.size()];

        for (int i = 0; i < relBusObjPageList.size(); i++) {
            Map objDetails = (Map) relBusObjPageList.get(i);
            objIds[i] = (String) objDetails.get("id");
        }

        return getHtmlStringForTable(context, objIds);
    }

    private Object getHtmlStringForTable(Context context, String[] objIds) throws Exception {
        Vector columnCellContentList = new Vector();
        try {
            REL_VERSION_OF = MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");
            String SELECT_ON_VERSIONOF_REL = "from[" + REL_VERSION_OF + "]";
            SELECT_ON_MAJOR = SELECT_ON_VERSIONOF_REL + ".to.";
            ATTR_SOURCE = "attribute[" + MCADMxUtil.getActualNameForAEFData(context, "attribute_Source") + "]";

            /* Latest Version information is fetched to support bulk loading object where VersonOf Relationship not exist */
            String REL_LATEST_VERSION = MCADMxUtil.getActualNameForAEFData(context, "relationship_LatestVersion");
            String SELECT_LATEST_VERSION = "to[" + REL_LATEST_VERSION + "].from.";
            String IS_VERSION_OBJ = MCADMxUtil.getActualNameForAEFData(context, "attribute_IsVersionObject");
            String SELECT_ISVERSIONOBJ = "attribute[" + IS_VERSION_OBJ + "]";

            StringList busSelectionList = new StringList(7);
            busSelectionList.addElement("id");
            busSelectionList.addElement("type");

            busSelectionList.addElement(ATTR_SOURCE); // To get Integrations name
            busSelectionList.addElement(SELECT_ON_MAJOR + "type"); // from minor
            busSelectionList.addElement(SELECT_ON_VERSIONOF_REL); // from minor for Bulk loading check
            busSelectionList.addElement(SELECT_LATEST_VERSION + "type"); // from minor
            busSelectionList.addElement(SELECT_ISVERSIONOBJ);

            BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);

            for (int i = 0; i < buslWithSelectionList.size(); i++) {
                BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect) buslWithSelectionList.elementAt(i);

                String designName = "";
                String integrationName = null;

                String busType = busObjectWithSelect.getSelectData("type");
                String busId = busObjectWithSelect.getSelectData("id"); // [NDM] OP6
                String integrationSource = busObjectWithSelect.getSelectData(ATTR_SOURCE);
                String sIsVersion = busObjectWithSelect.getSelectData(SELECT_ISVERSIONOBJ);
                boolean isVersion = Boolean.valueOf(sIsVersion).booleanValue();

                if (integrationSource != null) {
                    StringTokenizer integrationSourceTokens = new StringTokenizer(integrationSource, "|");

                    if (integrationSourceTokens.hasMoreTokens())
                        integrationName = integrationSourceTokens.nextToken();
                }

                if (integrationName != null && integrationNameGCOTable.containsKey(integrationName)) {
                    MCADGlobalConfigObject gco = (MCADGlobalConfigObject) integrationNameGCOTable.get(integrationName);
                    if (!isVersion || gco.isTemplateType(busType)) // gco.isMajorType(busType) // [NDM] OP6
                    {
                        designName = busType;
                    } else {
                        /* Extra Check is added to support bulk loading object where VersonOf Relationship not exist */
                        String isVersionOfExist = busObjectWithSelect.getSelectData(SELECT_ON_VERSIONOF_REL);

                        if (!MCADUtil.getBoolean(isVersionOfExist))
                            designName = busObjectWithSelect.getSelectData(SELECT_LATEST_VERSION + "type");
                        else
                            designName = busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "type");
                    }
                } else {
                    designName = busType;
                }
                designName = MCADMxUtil.getNLSName(context, "Type", designName, "", "", localeLanguage);
                columnCellContentList.add(designName);
            }
        } catch (Exception e) {

        }
        return columnCellContentList;
    }

    public Object getHtmlStringForSearchTable(Context context, String[] args) throws Exception {
        List relBusObjPageList = initialize(context, args);

        Vector columnCellContentList = new Vector();

        String[] objIds = new String[relBusObjPageList.size()];

        for (int i = 0; i < relBusObjPageList.size(); i++) {
            Map objDetails = (Map) relBusObjPageList.get(i);
            objIds[i] = (String) objDetails.get("id");
        }

        try {
            REL_VERSION_OF = MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");
            SELECT_ON_MAJOR = "from[" + REL_VERSION_OF + "].to.";
            ATTR_SOURCE = "attribute[" + MCADMxUtil.getActualNameForAEFData(context, "attribute_Source") + "]";

            StringList busSelectionList = new StringList();

            busSelectionList.addElement("id");
            busSelectionList.addElement("type");

            busSelectionList.addElement(ATTR_SOURCE); // To get Integrations name
            busSelectionList.addElement(SELECT_ON_MAJOR + "type"); // from minor

            BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);

            for (int i = 0; i < buslWithSelectionList.size(); i++) {
                BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect) buslWithSelectionList.elementAt(i);

                String designName = "";
                String integrationName = null;

                String busType = busObjectWithSelect.getSelectData("type");
                String integrationSource = busObjectWithSelect.getSelectData(ATTR_SOURCE);

                if (integrationSource != null) {
                    StringTokenizer integrationSourceTokens = new StringTokenizer(integrationSource, "|");

                    if (integrationSourceTokens.hasMoreTokens())
                        integrationName = integrationSourceTokens.nextToken();
                }

                designName = MCADMxUtil.getNLSName(context, "Type", busType, "", "", localeLanguage);
                columnCellContentList.add(designName);
            }
        } catch (Exception e) {

        }
        return columnCellContentList;
    }

    public Object getLatestTypeForFrameworkTable(Context context, String[] args) throws Exception {
        String paramKey = "latestid";
        List relBusObjPageList = initialize(context, args);

        String[] objIds = new String[relBusObjPageList.size()];
        try {
            if (!MCADUtil.checkForDataExistense(paramKey, relBusObjPageList)) {
                MCADIntegGetLatestVersion_mxJPO objGetLatestJPO = new MCADIntegGetLatestVersion_mxJPO(context, integrationNameGCOTable, localeLanguage);

                for (int i = 0; i < relBusObjPageList.size(); i++) {
                    Map idMap = (Map) relBusObjPageList.get(i);
                    objIds[i] = idMap.get("id").toString();
                }
                Map returnTable = objGetLatestJPO.getLatestForObjectIds(context, objIds);

                for (int i = 0; i < relBusObjPageList.size(); i++) {
                    Map idMap = (Map) relBusObjPageList.get(i);
                    String busId = idMap.get("id").toString();
                    String latestId = (String) returnTable.get(busId);
                    objIds[i] = latestId;

                    idMap.put("latestid", latestId);
                }
            } else {
                for (int i = 0; i < relBusObjPageList.size(); i++) {
                    Map idMap = (Map) relBusObjPageList.get(i);
                    objIds[i] = idMap.get(paramKey).toString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return getHtmlStringForTable(context, objIds);
    }

    public Object getHtmlStringForRelatedObject(Context context, String[] args) throws Exception {
        List relBusObjPageList = initialize(context, args);

        Vector columnCellContentList = new Vector();

        String[] objIds = new String[relBusObjPageList.size()];

        for (int i = 0; i < relBusObjPageList.size(); i++) {
            Map objDetails = (Map) relBusObjPageList.get(i);
            objIds[i] = (String) objDetails.get("id");
        }

        try {
            REL_VERSION_OF = MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");
            SELECT_ON_MAJOR = "from[" + REL_VERSION_OF + "].to.";
            ATTR_SOURCE = "attribute[" + MCADMxUtil.getActualNameForAEFData(context, "attribute_Source") + "]";

            StringList busSelectionList = new StringList();

            busSelectionList.addElement("id[connection]");
            busSelectionList.addElement("type");

            busSelectionList.addElement(ATTR_SOURCE); // To get Integrations name
            busSelectionList.addElement(SELECT_ON_MAJOR + "type"); // from minor

            BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);

            for (int i = 0; i < buslWithSelectionList.size(); i++) {
                BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect) buslWithSelectionList.elementAt(i);

                String designName = "";
                String integrationName = null;

                String busType = busObjectWithSelect.getSelectData("type");
                String integrationSource = busObjectWithSelect.getSelectData(ATTR_SOURCE);

                if (integrationSource != null) {
                    StringTokenizer integrationSourceTokens = new StringTokenizer(integrationSource, "|");

                    if (integrationSourceTokens.hasMoreTokens())
                        integrationName = integrationSourceTokens.nextToken();
                }

                designName = MCADMxUtil.getNLSName(context, "Type", busType, "", "", localeLanguage);
                columnCellContentList.add(designName);
            }
        } catch (Exception e) {

        }
        return columnCellContentList;
    }
}
