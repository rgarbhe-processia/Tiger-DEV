
/*
 ** DSCShowRenameLink
 **
 ** Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 **
 ** Program to show rename icon.
 */
import java.util.HashMap;
import java.util.Vector;

import matrix.db.Context;
import matrix.db.JPO;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.apps.domain.util.MapList;

public class DSCShowRenameLink_mxJPO {
    private HashMap integrationNameGCOTable = null;

    private MCADServerResourceBundle serverResourceBundle = null;

    private MCADMxUtil util = null;

    private IEFGlobalCache cache = null;

    private String localeLanguage = null;

    public DSCShowRenameLink_mxJPO(Context context, String[] args) throws Exception {
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
                    String renameURL = "MCADGenericFS.jsp?pageDetails=Rename|MCADRename.jsp|" + integrationName + "|true|" + objectId;
                    return renameURL;
                }
            } catch (Exception e) {
            }

            return "";
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
                    String renameToolTip = serverResourceBundle.getString("mcadIntegration.Server.AltText.Rename");
                    String renameURL = "../integrations/MCADGenericFS.jsp?pageDetails=Rename|MCADRename.jsp|" + integrationName + "|true|" + objectId;
                    String renameHref = "javascript:emxShowModalDialog('" + renameURL + "', 400, 400, false)";

                    htmlBuffer.append(getFeatureIconContent(renameHref, "iconActionRename.gif", renameToolTip));
                }
            } catch (Exception e) {
            }

            columnCellContentList.add(htmlBuffer.toString());
        }

        return columnCellContentList;
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

}
