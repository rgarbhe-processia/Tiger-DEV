
/**
 * IEFListCollectionsByUser.java Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries,
 * Copyright notice is precautionary only and does not evidence any actual or intended publication of such program JPO to retun all collection for hte context user Project. Infocentral Migration to UI
 * level 3 $Archive: $ $Revision: 1.2$ $Author: ds-unamagiri$
 * @since AEF 9.5.2.0
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.BusinessObjectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.MQLCommand;

import com.matrixone.MCADIntegration.utils.MCADUrlUtil;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.apps.domain.util.XSSUtil;

public class IEFListCollectionsByUser_mxJPO {
    public IEFListCollectionsByUser_mxJPO(Context context, String[] args) throws Exception {
    }

    @com.matrixone.apps.framework.ui.ProgramCallable
    public Object getTableData(Context context, String[] args) throws Exception {
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        String suiteKey = (String) paramMap.get("suiteKey");

        MapList collectionList = new MapList();

        try {
            String isBrowserIE = (String) paramMap.get("isBrowserIE");
            String charSet = (String) paramMap.get("charSet");

            if (charSet == null || charSet.trim().equals(""))
                charSet = "UTF-8";

            com.matrixone.apps.domain.util.i18nNow loc = new com.matrixone.apps.domain.util.i18nNow();
            // get the sets in matrix by context user
            MQLCommand mql = new MQLCommand();
            boolean ret = mql.executeCommand(context, "list set user $1", context.getUser());
            if (ret) {
                String result = mql.getResult();
                StringTokenizer tokenizer = new StringTokenizer(result, "\n");
                ArrayList tokens = new ArrayList();
                while (tokenizer.hasMoreElements()) {
                    String setName = (String) tokenizer.nextElement();
                    if (setName.startsWith("."))
                        continue;
                    // href on Collection Name will open up a emxTree window display list of objects
                    // in that collection
                    StringBuffer sbHref = new StringBuffer();
                    sbHref.append("../common/emxTree.jsp?treeLabel=");
                    if (isBrowserIE.equalsIgnoreCase("true")) {
                        sbHref.append(java.net.URLEncoder.encode(java.net.URLEncoder.encode(FrameworkUtil.encodeURL(setName, charSet))));
                    } else {
                        sbHref.append(java.net.URLEncoder.encode(FrameworkUtil.encodeURL(setName, charSet)));
                    }
                    sbHref.append("&amp;treeMenu=IEFCollectionsMenu&amp;AppendParameters=True&amp;DefaultCategory=IEFCollectionItems&amp;mode=insert&amp;relID=null&amp;jsTreeID=null&amp;suiteKey="
                            + suiteKey + "&amp;portalCmdName=IEFCollectionsMyDesk");
                    // sbHref.append("&amp;setName=" + MCADUrlUtil.hexEncode(setName));

                    sbHref.append("&amp;setName=" + XSSUtil.encodeForURL(context, setName));
                    matrix.db.Set set = new matrix.db.Set(setName);
                    set.open(context);
                    BusinessObjectList busList = set.getBusinessObjects(context);
                    set.close(context);
                    String sCount = String.valueOf(busList.size());
                    // get the Description of the Collection
                    String output = MqlUtil.mqlCommand(context, "list property $1 $2 $3", "on", "set", setName);
                    int endNameIndex = output.indexOf("value");
                    String sDescription = "";

                    if (endNameIndex > -1)
                        sDescription = output.substring(endNameIndex + 6);

                    if (sDescription.equalsIgnoreCase("null") || sDescription == null)
                        sDescription = "";

                    if (busList.size() >= 0) {
                        HashMap map = new HashMap();
                        // add Collection Name
                        map.put("CollectionName", setName);
                        // unique indentifier for each row
                        map.put("ID", setName);
                        // add Item Description
                        map.put("CollectionDescription", sDescription);
                        // add Item Count
                        map.put("CollectionCount", sCount);
                        // add Launch URL
                        map.put("LaunchURL", sbHref.toString());
                        map.put("id", XSSUtil.encodeForURL(context, setName));
                        collectionList.add(map);
                    }
                }
            }

        } catch (Exception ex) {
            collectionList = new MapList();
        }
        return collectionList;
    }// End of function

    public Vector getCollectionDescription(Context context, String[] args) throws Exception {
        Vector columnCellContentList = new Vector();

        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        MapList relBusObjPageList = (MapList) paramMap.get("objectList");

        for (int i = 0; i < relBusObjPageList.size(); i++) {
            String collectionDescription = null;

            try {
                HashMap objDetails = (HashMap) relBusObjPageList.get(i);
                collectionDescription = (String) objDetails.get("CollectionDescription");
            } catch (Exception e) {
            }

            columnCellContentList.add(collectionDescription);
        }

        return columnCellContentList;
    }// End of function

    public Vector getCollectionCount(Context context, String[] args) throws Exception {
        Vector columnCellContentList = new Vector();

        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        MapList relBusObjPageList = (MapList) paramMap.get("objectList");

        for (int i = 0; i < relBusObjPageList.size(); i++) {
            String collectionCount = null;

            try {
                HashMap objDetails = (HashMap) relBusObjPageList.get(i);
                collectionCount = (String) objDetails.get("CollectionCount");
            } catch (Exception e) {
            }

            columnCellContentList.add(collectionCount);
        }
        return columnCellContentList;
    }// End of function

    public Vector getCollectionName(Context context, String[] args) throws Exception {
        Vector columnCellContentList = new Vector();
        StringBuffer htmlBuffer = new StringBuffer();
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);

        String charSet = (String) paramMap.get("charSet");
        if (charSet == null || charSet.trim().equals(""))
            charSet = "UTF-8";

        paramMap.put("displayCheckout", "true");
        paramMap.put("displayViewer", "true");

        MapList relBusObjPageList = (MapList) paramMap.get("objectList");

        for (int i = 0; i < relBusObjPageList.size(); i++) {
            String collectionName = null;

            try {
                HashMap objDetails = (HashMap) relBusObjPageList.get(i);

                String objectId = (String) objDetails.get("ID");
                htmlBuffer = new StringBuffer();
                collectionName = (String) objDetails.get("CollectionName");

                StringBuffer treeLabel = new StringBuffer();
                treeLabel.append(java.net.URLEncoder.encode(java.net.URLEncoder.encode(FrameworkUtil.encodeURL(collectionName, charSet))));

                String checkoutHref = (String) objDetails.get("LaunchURL");
                String checkoutToolTip = null;
                checkoutHref = "javascript:openWindow('" + checkoutHref + "')";

                htmlBuffer.append(getFeatureIconContent(checkoutHref, "../common/images/iconSmallCollection.gif", "_" + collectionName, checkoutToolTip));
                htmlBuffer.append(getViewerURL(context, objectId, " ", collectionName));
            } catch (Exception e) {
            }

            columnCellContentList.add(htmlBuffer.toString());
        }

        return columnCellContentList;
    }// End of function

    public Vector getLaunchURL(Context context, String[] args) throws Exception {
        Vector columnCellContentList = new Vector();
        StringBuffer htmlBuffer = new StringBuffer();
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        MapList relBusObjPageList = (MapList) paramMap.get("objectList");

        for (int i = 0; i < relBusObjPageList.size(); i++) {
            String launchUrl = null;

            try {
                HashMap objDetails = (HashMap) relBusObjPageList.get(i);
                htmlBuffer = new StringBuffer();
                launchUrl = (String) objDetails.get("LaunchURL");

                launchUrl = "javascript:openWindow('" + launchUrl + "')";

                String launchUrlToolTip = null;

                htmlBuffer.append(getFeatureIconContent(launchUrl, "../common/images/iconActionNewWindow.gif", "", launchUrlToolTip));
            } catch (Exception e) {
            }

            columnCellContentList.add(htmlBuffer.toString());
        }

        return columnCellContentList;
    }// End of function

    protected String getFeatureIconContent(String href, String featureImage, String title, String toolTop) {
        StringBuffer featureIconContent = new StringBuffer();

        featureIconContent.append("<a href=\"");
        featureIconContent.append(href);
        featureIconContent.append("\" ><img src=\"");
        featureIconContent.append(featureImage);
        featureIconContent.append("\" border=\"0\" title=\"");
        featureIconContent.append(toolTop);
        featureIconContent.append("\"/>");
        featureIconContent.append(title);
        featureIconContent.append("</a>");

        return featureIconContent.toString();
    }// End of function

    protected String getViewerURL(Context context, String objectId, String format, String fileName) {
        try {
            String lang = (String) context.getSession().getLanguage();
            String sTipView = i18nNow.getI18nString("emxTeamCentral.ContentSummary.ToolTipView", "emxTeamCentralStringResource", lang);
            StringBuffer htmlBuffer = new StringBuffer();

            // format and store all of them in a String seperated by comma
            MQLCommand prMQL = new MQLCommand();
            prMQL.open(context);

            prMQL.executeCommand(context, "execute program $1 $2 ", "eServicecommonGetViewers.tcl", format);

            String sResult = prMQL.getResult().trim();
            String error = prMQL.getError();
            String sViewerServletName = "";

            if (null != sResult && sResult.length() > 0) {
                StringTokenizer viewerTokenizer = new StringTokenizer(sResult, "|", false);
                String sErrorCode = "";

                if (viewerTokenizer.hasMoreTokens())
                    sErrorCode = viewerTokenizer.nextToken();

                if (sErrorCode.equals("0")) {
                    if (viewerTokenizer.hasMoreTokens())
                        sViewerServletName = viewerTokenizer.nextToken();
                    if (viewerTokenizer.hasMoreTokens())
                        sTipView = viewerTokenizer.nextToken();
                }
                if (sViewerServletName == null || sViewerServletName.length() == 0)
                    return "";

                String sFileViewerLink = "/servlet/" + sViewerServletName;
                String viewerURL = "../iefdesigncenter/emxInfoViewer.jsp?url=" + sFileViewerLink + "&amp;id=" + objectId + "&amp;format=" + format + "&amp;file=" + MCADUrlUtil.hexEncode(fileName);
                String viewerHref = viewerURL;

                viewerHref = "javascript:openWindow('" + viewerURL + "')";
                String url = getFeatureIconContent(viewerHref, "iconActionViewer.gif", "", sTipView + " (" + format + ")");

                htmlBuffer.append(url);
                htmlBuffer.append("&nbsp");
            }

            return htmlBuffer.toString();
        } catch (Exception e) {
        }

        return "";
    }// End of function
}// End of Class
