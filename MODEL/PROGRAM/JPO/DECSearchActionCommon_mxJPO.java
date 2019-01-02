import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.StringTokenizer;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.IEFSimpleConfigObject;
import com.matrixone.MCADIntegration.server.beans.MCADConfigObjectLoader;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PropertyUtil;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

public class DECSearchActionCommon_mxJPO {
    Map objIdIntegrationNameMap = null;

    Map integrationnameGCOMap = null;

    MCADMxUtil mxUtil = null;

    public DECSearchActionCommon_mxJPO(Context context, String[] args) throws Exception {
    }

    public Object getHtmlStringForFrameworkTable(Context context, String[] args) throws Exception {
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        MapList relBusObjPageList = (MapList) paramMap.get("objectList");

        String[] objIds = new String[relBusObjPageList.size()];

        for (int i = 0; i < relBusObjPageList.size(); i++) {
            Map objDetails = (Map) relBusObjPageList.get(i);
            objIds[i] = (String) objDetails.get("id");
        }

        if (objIdIntegrationNameMap == null) {
            objIdIntegrationNameMap = new HashMap();
            objIdIntegrationNameMap = getObjIdIngrationNameMap(context, objIds);
            createAllGCO(context);
        }
        paramMap.put("GCOTable", integrationnameGCOMap);

        String[] strArgs = JPO.packArgs(paramMap);
        DSCShowActionsLinkBase_mxJPO jpo = new DSCShowActionsLinkBase_mxJPO(context, strArgs);
        // String jpoName = "DSCShowActionsLinkBase";
        // String jpoMethod = "getHtmlStringForFrameworkTable";
        return jpo.getHtmlStringForFrameworkTable(context, strArgs);
    }

    public Object getMajorStateLabelForFrameworkTable(Context context, String[] args) throws Exception {
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        MapList relBusObjPageList = (MapList) paramMap.get("objectList");

        String[] objIds = new String[relBusObjPageList.size()];

        for (int i = 0; i < relBusObjPageList.size(); i++) {
            Map objDetails = (Map) relBusObjPageList.get(i);
            objIds[i] = (String) objDetails.get("id");
        }

        if (objIdIntegrationNameMap == null) {
            objIdIntegrationNameMap = new HashMap();
            objIdIntegrationNameMap = getObjIdIngrationNameMap(context, objIds);
            createAllGCO(context);
        }
        paramMap.put("GCOTable", integrationnameGCOMap);
        String[] strArgs = JPO.packArgs(paramMap);

        DSCShowColumnLabel_mxJPO jpo = new DSCShowColumnLabel_mxJPO(context, strArgs);
        jpo.getMajorStateLabelForFrameworkTable(context, strArgs);
        // String jpoName = "DSCShowColumnLabel";
        // String jpoMethod = "getMajorStateLabelForFrameworkTable";
        return jpo.getMajorStateLabelForFrameworkTable(context, strArgs);
    }

    private void createAllGCO(Context context) throws Exception {
        String typeGlobalConfig = MCADMxUtil.getActualNameForAEFData(context, "type_MCADInteg-GlobalConfig");
        String attrIntegrationToGCOMapping = MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-IntegrationToGCOMapping");
        // ArrayList list = new ArrayList(objIdIntegrationNameMap.values());
        HashSet listIntegrationNames = new HashSet();
        listIntegrationNames.addAll(objIdIntegrationNameMap.values());
        integrationnameGCOMap = new HashMap();
        Iterator itr = listIntegrationNames.iterator();
        while (itr.hasNext()) {
            String integrationName = (String) itr.next();
            integrationnameGCOMap.put(integrationName, getGlobalConfigObject(context, integrationName, typeGlobalConfig, attrIntegrationToGCOMapping));
        }
    }

    private String getGlobalConfigObjectName(Context context, String integrationName, String attrIntegrationToGCOMapping) throws Exception {
        String gcoName = null;

        String rpeUserName = PropertyUtil.getGlobalRPEValue(context, ContextUtil.MX_LOGGED_IN_USER_NAME);

        IEFSimpleConfigObject simpleLCO = IEFSimpleConfigObject.getSimpleLCO(context, rpeUserName);

        if (simpleLCO.isObjectExists()) {
            Hashtable integNameGcoMapping = simpleLCO.getAttributeAsHashtable(attrIntegrationToGCOMapping, "\n", "|");
            gcoName = (String) integNameGcoMapping.get(integrationName);
        } else {
            IEFGetRegistrationDetails_mxJPO registrationDetailsReader = new IEFGetRegistrationDetails_mxJPO(context, null);
            String args[] = new String[1];
            args[0] = integrationName;
            String registrationDetails = registrationDetailsReader.getRegistrationDetails(context, args);
            gcoName = registrationDetails.substring(registrationDetails.lastIndexOf("|") + 1);
        }

        return gcoName;
    }

    protected MCADGlobalConfigObject getGlobalConfigObject(Context context, String integrationName, String typeGlobalConfig, String attrIntegrationToGCOMapping) throws Exception {
        MCADGlobalConfigObject gcoObject = null;

        String gcoName = this.getGlobalConfigObjectName(context, integrationName, attrIntegrationToGCOMapping);

        if (gcoName == null && MCADMxUtil.isSolutionBasedEnvironment(context)) {
            IEFGetRegistrationDetails_mxJPO registrationDetailsReader = new IEFGetRegistrationDetails_mxJPO(context, null);
            String args[] = new String[1];
            args[0] = integrationName;
            String registrationDetails = registrationDetailsReader.getRegistrationDetails(context, args);

            gcoName = registrationDetails.substring(registrationDetails.lastIndexOf("|") + 1);
        }

        MCADConfigObjectLoader configLoader = new MCADConfigObjectLoader(null);
        gcoObject = configLoader.createGlobalConfigObject(context, mxUtil, typeGlobalConfig, gcoName);

        return gcoObject;
    }

    private Map getObjIdIngrationNameMap(Context context, String[] objIds) throws Exception {
        // String jpoName = "IEFGuessIntegrationContext";
        // String jpoMethod = "getIntegrationNameForBusIds";
        //
        // Hashtable jpoArgsTable = new Hashtable();
        // jpoArgsTable.put("objIds", objIds);
        //
        // String[] jpoArgs = JPO.packArgs(jpoArgsTable);
        String language = "en-us";
        mxUtil = new MCADMxUtil(context, new MCADServerResourceBundle(language), new IEFGlobalCache());

        try {
            // return (Map)mxUtil.executeJPO(context,jpoName, jpoMethod, jpoArgs, Hashtable.class);
            return getIntegrationNameForBusIds(context, objIds);
        } catch (Exception e) {
        }
        return null;
    }

    public Hashtable getIntegrationNameForBusIds(Context context, String[] args) {
        Hashtable busIdIntegrationNameTable = new Hashtable(args.length);

        String SELECT_SOURCE_ATTR = "attribute[" + (String) PropertyUtil.getSchemaProperty(context, "attribute_Source") + "]";
        StringList busSelects = new StringList(1);

        busSelects.addElement("id");
        busSelects.addElement(SELECT_SOURCE_ATTR);

        try {
            BusinessObjectWithSelectList busWithSelectList = BusinessObject.getSelectBusinessObjectData(context, args, busSelects);

            for (int i = 0; i < busWithSelectList.size(); i++) {
                BusinessObjectWithSelect busWithSelect = busWithSelectList.getElement(i);

                String busID = busWithSelect.getSelectData("id");
                String integrationSource = busWithSelect.getSelectData(SELECT_SOURCE_ATTR);

                StringTokenizer integrationSourceTokens = new StringTokenizer(integrationSource, "|");

                String integrationName = "";

                if (integrationSourceTokens.hasMoreTokens())
                    integrationName = integrationSourceTokens.nextToken();
                busIdIntegrationNameTable.put(args[0], integrationName);
            }
        } catch (Exception ex) {
        }

        return busIdIntegrationNameTable;
    }

}
