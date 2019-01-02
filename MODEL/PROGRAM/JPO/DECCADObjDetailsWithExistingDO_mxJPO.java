import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADUtil;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.MapList;

public class DECCADObjDetailsWithExistingDO_mxJPO {
    public DECCADObjDetailsWithExistingDO_mxJPO(Context context, String[] args) throws Exception {
    }

    public Object getExistingDerivedOutputList(Context context, String[] args) throws Exception {
        Vector existingDOList = new Vector();
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        HashMap paramList = (HashMap) paramMap.get("paramList");

        MapList objIdList = (MapList) paramMap.get("objectList");
        String localeLanguage = (String) paramList.get("LocaleLanguage");
        HashMap integrationNameGCOTable = (HashMap) paramList.get("GCOTable");

        IEFGlobalCache cache = new IEFGlobalCache();
        MCADServerResourceBundle serverResourceBundle = new MCADServerResourceBundle(localeLanguage);
        MCADMxUtil util = new MCADMxUtil(context, serverResourceBundle, cache);
        MCADGlobalConfigObject globalConfigObject = null;

        String objIds[] = new String[objIdList.size()];

        for (int i = 0; i < objIdList.size(); i++) {
            HashMap objectIds = (HashMap) objIdList.get(i);
            objIds[i] = (String) objectIds.get("id");
        }

        // integrationName is retrieved from first objectId.
        String integrationName = util.getIntegrationName(context, objIds[0]);

        if (integrationName != null && integrationNameGCOTable != null && integrationNameGCOTable.containsKey(integrationName))
            globalConfigObject = (MCADGlobalConfigObject) integrationNameGCOTable.get(integrationName);

        if (null != globalConfigObject) {
            Hashtable relAndEnds = globalConfigObject.getRelationshipsOfClass(MCADServerSettings.DERIVEDOUTPUT_LIKE);

            StringList busSelectionList = getBusSelectListFromRelAndEnds(relAndEnds);
            busSelectionList.add(DomainObject.SELECT_FILE_FORMAT);

            BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);

            for (int i = 0; i < buslWithSelectionList.size(); i++) {
                BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect) buslWithSelectionList.elementAt(i);

                StringList finalFormatList = new StringList();

                if (globalConfigObject.isCreateDependentDocObj()) {
                    for (int j = 0; j < busSelectionList.size(); j++) {
                        StringList formatListFromDO = busObjectWithSelect.getSelectDataList((String) busSelectionList.get(j));

                        if (null != formatListFromDO)
                            finalFormatList.addAll(formatListFromDO);
                    }
                } else {
                    finalFormatList = busObjectWithSelect.getSelectDataList(DomainObject.SELECT_FILE_FORMAT);
                }

                existingDOList.add(MCADUtil.getStringFromCollection(finalFormatList, ","));
            }
        }

        return existingDOList;
    }

    private StringList getBusSelectListFromRelAndEnds(Hashtable relsAndEnds) {
        StringList busSelectionList = new StringList();

        Enumeration relList = relsAndEnds.keys();

        // For expanding
        while (relList.hasMoreElements()) {
            String relName = (String) relList.nextElement();

            StringList relExpnSelects = new StringList();

            String relEnd = (String) relsAndEnds.get(relName);
            String expEnd = "";

            if (relEnd.equals("to"))
                expEnd = "from";
            else
                expEnd = "to";

            relExpnSelects.addElement(expEnd + "[" + relName + "]." + relEnd + "." + DomainObject.SELECT_FILE_FORMAT);

            busSelectionList.addAll(relExpnSelects);
        }

        return busSelectionList;
    }

    public Object isObjectFinalized(Context context, String[] args) throws Exception {
        Vector objectFinalizationDetails = new Vector();
        MCADServerGeneralUtil serverGeneralUtil = null;
        MCADGlobalConfigObject globalConfigObject = null;

        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        HashMap paramList = (HashMap) paramMap.get("paramList");
        String localeLanguage = (String) paramList.get("LocaleLanguage");
        HashMap integrationNameGCOTable = (HashMap) paramList.get("GCOTable");

        IEFGlobalCache cache = new IEFGlobalCache();
        MCADServerResourceBundle serverResourceBundle = new MCADServerResourceBundle(localeLanguage);
        MCADMxUtil util = new MCADMxUtil(context, serverResourceBundle, cache);

        MapList objIdList = (MapList) paramMap.get("objectList");

        for (int i = 0; i < objIdList.size(); i++) {
            HashMap objectIds = (HashMap) objIdList.get(i);
            String objectId = (String) objectIds.get("id");

            String integrationName = util.getIntegrationName(context, objectId);

            if (integrationName != null && integrationNameGCOTable != null && integrationNameGCOTable.containsKey(integrationName))
                globalConfigObject = (MCADGlobalConfigObject) integrationNameGCOTable.get(integrationName);

            serverGeneralUtil = new MCADServerGeneralUtil(context, globalConfigObject, serverResourceBundle, cache);
            BusinessObject busObj = new BusinessObject(objectId);

            busObj.open(context);
            boolean isFinalized = serverGeneralUtil.isBusObjectFinalized(context, busObj);
            busObj.close(context);

            objectFinalizationDetails.add(new Boolean(isFinalized).toString());
        }

        return objectFinalizationDetails;
    }
}
