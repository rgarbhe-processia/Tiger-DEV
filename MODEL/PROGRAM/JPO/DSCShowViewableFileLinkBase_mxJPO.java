
/*
 ** DSCShowViewableFileLinkBase
 **
 ** Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 **
 ** Program to display Viewable File Icon
 */

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import matrix.db.Context;
import matrix.db.JPO;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.IEFIntegAccessUtil;
// import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.apps.domain.util.MapList;

public class DSCShowViewableFileLinkBase_mxJPO {
    protected HashMap integrationNameGCOTable = null;

    protected MCADServerResourceBundle serverResourceBundle = null;

    protected IEFIntegAccessUtil util = null;

    // protected MCADServerGeneralUtil serverGeneralUtil = null;
    protected String localeLanguage = null;

    protected HashMap paramMap = null;

    protected IEFGlobalCache cache = null;

    public DSCShowViewableFileLinkBase_mxJPO(Context context, String[] args) throws Exception {
        // super(context, args);
    }

    public Object getHtmlString(Context context, String[] args) throws Exception {
        paramMap = (HashMap) JPO.unpackArgs(args);
        MapList relBusObjPageList = (MapList) paramMap.get("objectList");
        Map paramList = (Map) paramMap.get("paramList");

        localeLanguage = (String) paramList.get("languageStr");
        String portalName = (String) paramList.get("portCmdName");
        integrationNameGCOTable = (HashMap) paramMap.get("GCOTable");

        return getHtmlStringForTable(context, relBusObjPageList, portalName);
    }

    public Object getHtmlStringForFrameworkTable(Context context, String[] args) throws Exception {
        paramMap = (HashMap) JPO.unpackArgs(args);
        MapList relBusObjPageList = (MapList) paramMap.get("objectList");

        String portalName = (String) paramMap.get("portCmdName");

        HashMap paramList = (HashMap) paramMap.get("paramList");
        localeLanguage = (String) paramList.get("languageStr");

        integrationNameGCOTable = (HashMap) paramList.get("GCOTable");
        return getHtmlStringForTable(context, relBusObjPageList, portalName);
    }

    protected Object getHtmlStringForTable(Context context, MapList relBusObjPageList, String portalName) throws Exception {
        Vector columnCellContentList = new Vector();
        serverResourceBundle = new MCADServerResourceBundle(localeLanguage);
        cache = new IEFGlobalCache();
        util = new IEFIntegAccessUtil(context, serverResourceBundle, cache);

        String viewableFileToolTip = serverResourceBundle.getString("mcadIntegration.Server.AltText.ViewableFiles");

        Vector assignedIntegrations = util.getAssignedIntegrations(context);

        for (int i = 0; i < relBusObjPageList.size(); i++) {
            StringBuffer htmlBuffer = new StringBuffer();

            try {
                Map objDetails = (Map) relBusObjPageList.get(i);
                String objectId = (String) objDetails.get("id");
                String integrationName = util.getIntegrationName(context, objectId);

                if (integrationName != null && assignedIntegrations.contains(integrationName)) {
                    // MCADGlobalConfigObject gco = (MCADGlobalConfigObject)integrationNameGCOTable.get(integrationName);

                    // serverGeneralUtil = new MCADServerGeneralUtil(context, gco, serverResourceBundle,cache);

                    StringBuffer viewableFileListURLBuffer = new StringBuffer();
                    viewableFileListURLBuffer.append("'../integrations/DSCDerivedOutputFS.jsp?integrationName=").append(integrationName);
                    viewableFileListURLBuffer.append("&amp;objectId=").append(objectId);
                    viewableFileListURLBuffer.append(
                            "&amp;program=DSCDerivedOutputsTableData:getTableData&amp;suiteKey=DesignerCentral&amp;table=DSCTableDerivedOutput&amp;header=emxIEFDesignCenter.Common.DerivedOutput&amp;HelpMarker=emxhelpdscdlderivedoutput&amp;selection=muliple&amp;sortDirection=ascending&amp;showWSTable=false&amp;topActionbar=DSCDownloadAttachmentFiles&amp;Target Location=popup'");

                    String viewableFileListURL = viewableFileListURLBuffer.toString();

                    StringBuffer viewableFileListHrefBuffer = new StringBuffer();
                    viewableFileListHrefBuffer.append("javascript:showModalDialog(");
                    viewableFileListHrefBuffer.append(viewableFileListURL);
                    viewableFileListHrefBuffer.append(",'700','500')");

                    String viewableFileListHref = viewableFileListHrefBuffer.toString();

                    htmlBuffer.append(getFeatureIconContent(viewableFileListHref, "../iefdesigncenter/images/iconPreview.gif", viewableFileToolTip, ""));
                }
            } catch (Exception e) {
                System.out.println("DSCShowViewableFileLink: getHtmlStringForTable: " + e.toString());
            }

            columnCellContentList.add(htmlBuffer.toString());
        }

        return columnCellContentList;
    }

    protected String getFeatureIconContent(String href, String featureImage, String toolTop, String targetName) {
        StringBuffer featureIconContent = new StringBuffer();

        featureIconContent.append("<a href=\"");
        featureIconContent.append(href);
        featureIconContent.append("\" ");
        if (targetName.length() > 0) {
            featureIconContent.append("target=\"");
            featureIconContent.append(targetName);
            featureIconContent.append("\"");
        }
        featureIconContent.append(" ><img src=\"");
        featureIconContent.append(featureImage);
        featureIconContent.append("\" border=\"0\" title=\"");
        featureIconContent.append(toolTop);
        featureIconContent.append("\"/></a>");

        return featureIconContent.toString();
    }
}
