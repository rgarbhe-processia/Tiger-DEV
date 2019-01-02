
/*
 ** DSCShowSynchronizeEBOMLink
 **
 ** Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 **
 ** Program to display SynchronizeEBOM Icon
 */
import java.util.HashMap;
import java.util.Vector;

import matrix.db.Context;
import matrix.db.JPO;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.apps.domain.util.MapList;

public class DSCShowSynchronizedEBOMLink_mxJPO {
    private HashMap integrationNameGCOTable = null;

    private MCADServerResourceBundle serverResourceBundle = null;

    private MCADMxUtil util = null;

    private IEFGlobalCache cache = null;

    private String localeLanguage = null;

    public DSCShowSynchronizedEBOMLink_mxJPO(Context context, String[] args) throws Exception {
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
                    MCADGlobalConfigObject globalConfigObject = (MCADGlobalConfigObject) integrationNameGCOTable.get(integrationName);

                    String jpoEBOMSync = globalConfigObject.getFeatureJPO(MCADGlobalConfigObject.FEATURE_EBOMSYNCHRONIZE);
                    boolean bEBOMSynch = canShowFeatureIcon(context, objectId, jpoEBOMSync, "canshowEBOMSynchronize", integrationName);

                    if (bEBOMSynch) {
                        String synchEBOMURL = "MCADEBOMSynchronization.jsp?busDetails=" + integrationName + "|false|" + objectId;
                        return synchEBOMURL;
                    }
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
                    MCADGlobalConfigObject globalConfigObject = (MCADGlobalConfigObject) integrationNameGCOTable.get(integrationName);

                    String jpoEBOMSync = globalConfigObject.getFeatureJPO(MCADGlobalConfigObject.FEATURE_EBOMSYNCHRONIZE);
                    boolean bEBOMSynch = canShowFeatureIcon(context, objectId, jpoEBOMSync, "canshowEBOMSynchronize", integrationName);

                    if (bEBOMSynch) {
                        String synchEBOMToolTip = serverResourceBundle.getString("mcadIntegration.Server.AltText.SynchEBOM");
                        String synchEBOMURL = "../integrations/MCADEBOMSynchronization.jsp?busDetails=" + integrationName + "|false|" + objectId;
                        String synchEBOMHref = "javascript:emxShowModalDialog('" + synchEBOMURL + "', 400, 400, false)";

                        htmlBuffer.append(getFeatureIconContent(synchEBOMHref, "iconActionEBOMSync.gif", synchEBOMToolTip));
                    }
                }
            } catch (Exception e) {
            }

            columnCellContentList.add(htmlBuffer.toString());
        }

        return columnCellContentList;
    }

    private String executeJPO(Context context, String objectID, String jpoName, String jpoMethod, String integrationName) throws Exception {
        MCADGlobalConfigObject globalConfigObject = (MCADGlobalConfigObject) integrationNameGCOTable.get(integrationName);

        String[] packedGCO = JPO.packArgs(globalConfigObject);
        String[] args = new String[5];
        args[0] = packedGCO[0];
        args[1] = packedGCO[1];
        args[2] = objectID;
        args[3] = localeLanguage;
        args[4] = "";

        String result = util.executeJPO(context, jpoName, jpoMethod, args);

        return result;
    }

    private String getFeatureIconContent(String href, String featureImage, String toolTop) {
        StringBuffer featureIconContent = new StringBuffer();

        featureIconContent.append("<a href=\"");
        featureIconContent.append(href);
        featureIconContent.append("\" ><img src=\"images/");
        featureIconContent.append(featureImage);
        featureIconContent.append("\" border=\"0\" title=\"");
        featureIconContent.append(toolTop);
        featureIconContent.append("\"></a>");

        return featureIconContent.toString();
    }

    private boolean canShowFeatureIcon(Context context, String objectID, String jpoName, String jpoMethod, String integrationName) {
        boolean canShowIcon = false;

        try {
            String result = executeJPO(context, objectID, jpoName, jpoMethod, integrationName);

            if (result.startsWith("true")) {
                canShowIcon = true;
            }
        } catch (Exception e) {
        }

        return canShowIcon;
    }
}
