
/*
 ** IEFShowFinalizeLink
 **
 ** Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 **
 ** Program to display Finalize icon.
 */
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;

import matrix.db.Context;
import matrix.db.JPO;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.apps.domain.util.MapList;

public class IEFShowFinalizeLink_mxJPO {
    private HashMap integrationNameGCOTable = null;

    private MCADServerResourceBundle serverResourceBundle = null;

    private MCADMxUtil util = null;

    private String localeLanguage = null;

    private MCADGlobalConfigObject globalConfigObject = null;

    private IEFGlobalCache cache = null;

    public IEFShowFinalizeLink_mxJPO(Context context, String[] args) throws Exception {
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
                        String finalizationDetails = integrationName + "|" + objectId;
                        String finalizationAsHref = "javascript:parent.showFinalizationPage('" + integrationName + "','" + finalizationDetails + "')";

                        htmlBuffer.append(getFeatureIconContent(finalizationAsHref, "iconActionFinalize.gif", finalizeToolTip));
                    }

                    String jpoUndoFinalization = globalConfigObject.getFeatureJPO(MCADGlobalConfigObject.FEATURE_UNDOFINALIZE);
                    boolean bUndoFinalize = canShowFeatureIcon(context, objectId, jpoUndoFinalization, "canShowButton", integrationName);
                    if (bUndoFinalize) {
                        String undoFinalizeToolTip = serverResourceBundle.getString("mcadIntegration.Server.AltText.UndoFinalize");
                        String undoFinalizeURL = "../integrations/MCADUndoFinalization.jsp?busDetails=" + integrationName + "|true|" + objectId;
                        String undoFinalizeHref = "javascript:parent.emxShowModalDialog('" + undoFinalizeURL + "', 400, 400, false)";

                        htmlBuffer.append(getFeatureIconContent(undoFinalizeHref, "iconActionUndoFinalize.gif", undoFinalizeToolTip));
                    }
                }
            } catch (Exception e) {

            }

            columnCellContentList.add(htmlBuffer.toString());
        }

        return columnCellContentList;
    }

    private Hashtable executeJPO(Context context, String objectID, String jpoName, String jpoMethod) throws Exception {
        Hashtable resultDataTable = null;
        try {
            Hashtable JPOArgsTable = new Hashtable();

            JPOArgsTable.put(MCADServerSettings.GCO_OBJECT, globalConfigObject);
            JPOArgsTable.put(MCADServerSettings.LANGUAGE_NAME, localeLanguage);
            JPOArgsTable.put(MCADServerSettings.OBJECT_ID, objectID);
            JPOArgsTable.put(MCADServerSettings.JPO_METHOD_NAME, jpoMethod);

            String[] packedArgumentsTable = JPO.packArgs(JPOArgsTable);
            String[] args = new String[2];
            args[0] = packedArgumentsTable[0];
            args[1] = packedArgumentsTable[1];

            System.out.println("[IEFShowFinalizeLink]INVOKING JPO.." + jpoName);
            String[] init = new String[] {};
            resultDataTable = (Hashtable) JPO.invoke(context, jpoName, init, jpoMethod, args, Hashtable.class);
        } catch (Exception e) {
            System.out.println("[IEFShowFinalizeLink.executeJPO] Exception..." + e.getMessage());
            MCADServerException.createException(e.getMessage(), e);
        }

        return resultDataTable;
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
