
/*
 ** DSCShowPromoteLink
 **
 ** Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 **
 ** Program to display Finalize icon.
 */
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.apps.domain.util.MapList;

public class DSCShowPromoteLink_mxJPO {
    private HashMap integrationNameGCOTable = null;

    private MCADServerResourceBundle serverResourceBundle = null;

    private MCADMxUtil util = null;

    private MCADGlobalConfigObject globalConfigObject = null;

    private IEFGlobalCache cache = null;

    private String localeLanguage = null;

    private MCADServerGeneralUtil _generalUtil = null;

    public DSCShowPromoteLink_mxJPO(Context context, String[] args) throws Exception {
    }

    private String getMajorObjectId(Context context, String objectId) {
        String majorObjectId = objectId;

        try {
            BusinessObject busObject = new BusinessObject(objectId);
            busObject.open(context);

            // [NDM] OP6
            // String busType = busObject.getTypeName();

            if (!util.isMajorObject(context, objectId))// !globalConfigObject.isMajorType(busType))
            {
                // get the major object
                majorObjectId = util.getMajorObject(context, busObject).getObjectId(context);
            }

            busObject.close(context);
        } catch (Exception e) {
        }

        return majorObjectId;
    }

    public String getURL(Context context, String[] args) throws Exception {
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        MapList relBusObjPageList = (MapList) paramMap.get("objectList");
        localeLanguage = (String) paramMap.get("LocaleLanguage");
        integrationNameGCOTable = (HashMap) paramMap.get("GCOTable");

        serverResourceBundle = new MCADServerResourceBundle(localeLanguage);
        cache = new IEFGlobalCache();
        util = new MCADMxUtil(context, serverResourceBundle, cache);

        for (int i = 0; i < relBusObjPageList.size(); i++) {
            try {
                HashMap objDetails = (HashMap) relBusObjPageList.get(i);
                String objectId = (String) objDetails.get("id");
                String integrationName = util.getIntegrationName(context, objectId);

                if (integrationName != null && integrationNameGCOTable.containsKey(integrationName)) {
                    globalConfigObject = (MCADGlobalConfigObject) integrationNameGCOTable.get(integrationName);
                    objectId = getMajorObjectId(context, objectId);

                    String finalizeURL = "MCADFinalizationFS.jsp?busDetails=" + integrationName + "|true|" + objectId;

                    return finalizeURL;
                }
            } catch (Exception e) {
            }
        }

        return "";
    }

    public Object getHtmlString(Context context, String[] args) throws Exception {
        Vector columnCellContentList = new Vector();

        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        MapList relBusObjPageList = (MapList) paramMap.get("objectList");
        localeLanguage = (String) paramMap.get("LocaleLanguage");
        integrationNameGCOTable = (HashMap) paramMap.get("GCOTable");

        serverResourceBundle = new MCADServerResourceBundle(localeLanguage);
        cache = new IEFGlobalCache();
        util = new MCADMxUtil(context, serverResourceBundle, cache);

        for (int i = 0; i < relBusObjPageList.size(); i++) {
            StringBuffer htmlBuffer = new StringBuffer();

            try {
                HashMap objDetails = (HashMap) relBusObjPageList.get(i);
                String objectId = (String) objDetails.get("id");
                String integrationName = util.getIntegrationName(context, objectId);

                if (integrationName != null && integrationNameGCOTable.containsKey(integrationName)) {
                    globalConfigObject = (MCADGlobalConfigObject) integrationNameGCOTable.get(integrationName);

                    String jpoFinalization = globalConfigObject.getFeatureJPO(MCADGlobalConfigObject.FEATURE_FINALIZE);
                    boolean bFinalize = canShowFeatureIcon(context, objectId, jpoFinalization, "canShowButton", integrationName);

                    if (bFinalize) {
                        String finalizeToolTip = serverResourceBundle.getString("mcadIntegration.Server.AltText.Finalize");
                        String finalizeURL = "../integrations/MCADFinalizationFS.jsp?busDetails=" + integrationName + "|true|" + objectId;
                        String finalizeHref = "javascript:emxShowModalDialog('" + finalizeURL + "', 750, 600, false)";

                        htmlBuffer.append(getFeatureIconContent(finalizeHref, "iconActionFinalize.gif", finalizeToolTip));
                    }

                }
            } catch (Exception e) {
                System.out.println("+++ +  DSCShowPromoteLink: " + e.toString());
            }

            columnCellContentList.add(htmlBuffer.toString());
        }

        return columnCellContentList;
    }

    public String isFinalized(Context context, String[] args) throws Exception {
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        String retVal = isFinalized(context, paramMap);
        return retVal;
    }

    public String isFinalized(Context context, HashMap paramMap) throws Exception {
        Vector columnCellContentList = new Vector();

        MapList relBusObjPageList = (MapList) paramMap.get("objectList");
        localeLanguage = (String) paramMap.get("LocaleLanguage");
        integrationNameGCOTable = (HashMap) paramMap.get("GCOTable");

        serverResourceBundle = new MCADServerResourceBundle(localeLanguage);
        cache = new IEFGlobalCache();
        util = new MCADMxUtil(context, serverResourceBundle, cache);

        for (int i = 0; i < relBusObjPageList.size(); i++) {
            StringBuffer htmlBuffer = new StringBuffer();

            try {
                HashMap objDetails = (HashMap) relBusObjPageList.get(i);
                String objectId = (String) objDetails.get("id");
                String integrationName = util.getIntegrationName(context, objectId);

                if (integrationName != null && integrationNameGCOTable.containsKey(integrationName)) {
                    globalConfigObject = (MCADGlobalConfigObject) integrationNameGCOTable.get(integrationName);

                    _generalUtil = new MCADServerGeneralUtil(context, globalConfigObject, serverResourceBundle, cache);
                    objectId = getMajorObjectId(context, objectId);
                    BusinessObject majorObject = new BusinessObject(objectId);
                    majorObject.open(context);

                    boolean isFinalized = _generalUtil.isBusObjectFinalized(context, majorObject);
                    majorObject.close(context);

                    if (isFinalized) {
                        return "true";
                    }
                }
            } catch (Exception e) {
                System.out.println("+++ +  DSCShowFinalizeLink:  isFinalized " + e.toString());
            }

            columnCellContentList.add(htmlBuffer.toString());
        }

        return "false";
    }

    private Hashtable executeJPO(Context context, String objectID, String jpoName, String jpoMethod) throws Exception {
        Hashtable resultDataTable = null;
        try {
            Hashtable JPOArgsTable = new Hashtable();
            JPOArgsTable.put(MCADServerSettings.GCO_OBJECT, globalConfigObject);
            JPOArgsTable.put(MCADServerSettings.LANGUAGE_NAME, localeLanguage);
            JPOArgsTable.put(MCADServerSettings.OBJECT_ID, objectID);
            JPOArgsTable.put(MCADServerSettings.JPO_METHOD_NAME, jpoMethod);
            // Its of no use as we dont show if at the time of creation of Page
            JPOArgsTable.put(MCADServerSettings.OPERATION_UID, "dummyUID");

            String[] packedArgumentsTable = JPO.packArgs(JPOArgsTable);
            String[] args = new String[2];
            args[0] = packedArgumentsTable[0];
            args[1] = packedArgumentsTable[1];
            String[] init = new String[] {};
            resultDataTable = (Hashtable) JPO.invoke(context, jpoName, init, jpoMethod, args, Hashtable.class);
        } catch (Exception e) {
            System.out.println("[DSCShowPromoteLink.executeJPO] Exception..." + e.getMessage());
            MCADServerException.createException(e.getMessage(), e);
        }

        return resultDataTable;
    }

    private String getFeatureIconContent(String href, String featureImage, String toolTop) {
        StringBuffer featureIconContent = new StringBuffer();
        featureIconContent.append(href);

        return featureIconContent.toString();
    }

    private boolean canShowFeatureIcon(Context context, String objectID, String jpoName, String jpoMethod, String integrationName) {
        boolean canShowIcon = false;

        try {
            Hashtable resultDataTable = executeJPO(context, objectID, jpoName, jpoMethod);
            String result = (String) resultDataTable.get(MCADServerSettings.JPO_EXECUTION_STATUS);

            if (result.equalsIgnoreCase("false")) {
                String error = (String) resultDataTable.get(MCADServerSettings.JPO_STATUS_MESSAGE);
                MCADServerException.createException(error, null);
            } else {
                canShowIcon = true;
            }
        } catch (Exception e) {
        }

        return canShowIcon;
    }

}
