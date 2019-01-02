
/*
 ** DSCShowSaveAsLinkBase
 **
 ** Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 **
 ** Program to display SaveAs Icon
 */
import java.util.HashMap;
import java.util.Vector;

import matrix.db.Context;
import matrix.db.JPO;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.apps.domain.util.MapList;

public class DSCShowSaveAsLinkBase_mxJPO {
    protected HashMap integrationNameGCOTable = null;

    protected MCADServerResourceBundle serverResourceBundle = null;

    protected MCADMxUtil util = null;

    protected MCADServerGeneralUtil serverGeneralUtil = null;

    protected IEFGlobalCache cache = null;

    protected String localeLanguage = null;

    public DSCShowSaveAsLinkBase_mxJPO(Context context, String[] args) throws Exception {
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

                    String saveAsURL = "MCADSaveAs.jsp?busDetails=" + integrationName + "|false|" + objectId;
                    return saveAsURL;
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
                    MCADGlobalConfigObject globalConfigObject = (MCADGlobalConfigObject) integrationNameGCOTable.get(integrationName);

                    String saveAsToolTip = serverResourceBundle.getString("mcadIntegration.Server.AltText.SaveAs");
                    String saveAsURL = "../integrations/MCADSaveAs.jsp?busDetails=" + integrationName + "|false|" + objectId;
                    String saveAsHref = "javascript:emxShowModalDialog('" + saveAsURL + "', 750, 600, false)";

                    htmlBuffer.append(getFeatureIconContent(saveAsHref, "iconActionSaveAs.gif", saveAsToolTip));
                }
            } catch (Exception e) {
            }

            columnCellContentList.add(htmlBuffer.toString());
        }

        return columnCellContentList;
    }

    protected String executeJPO(Context context, String objectID, String jpoName, String jpoMethod, String integrationName) throws Exception {
        MCADGlobalConfigObject globalConfigObject = (MCADGlobalConfigObject) integrationNameGCOTable.get(integrationName);

        String[] packedGCO = JPO.packArgs(globalConfigObject);
        String[] args = new String[4];
        args[0] = packedGCO[0];
        args[1] = packedGCO[1];
        args[2] = objectID;
        args[3] = localeLanguage;

        String result = util.executeJPO(context, jpoName, jpoMethod, args);

        return result;
    }

    protected String getFeatureIconContent(String href, String featureImage, String toolTop) {
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
