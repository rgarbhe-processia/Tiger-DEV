
/*
 ** DSCShowViewerLinkBase
 **
 ** Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 **
 ** Program to display Checkout Icon
 */
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectItr;
import matrix.db.BusinessObjectList;
import matrix.db.Context;
import matrix.db.FileList;
import matrix.db.Format;
import matrix.db.FormatList;
import matrix.db.JPO;
import matrix.db.MQLCommand;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.utils.MCADUrlUtil;
import com.matrixone.apps.domain.util.i18nNow;

public class DSCShowViewerLinkBase_mxJPO {
    protected HashMap integrationNameGCOTable = null;

    protected MCADServerResourceBundle serverResourceBundle = null;

    protected MCADMxUtil util = null;

    protected String localeLanguage = null;

    protected HashMap paramMap = null;

    protected String strFileFormat = "";

    public DSCShowViewerLinkBase_mxJPO(Context context, String[] args) throws Exception {
        // super(context, args);
    }

    public DSCShowViewerLinkBase_mxJPO() throws Exception {
        // super(context, args);
    }

    public Object getHtmlString(Context context, String[] args) throws Exception {
        HashMap formatViewerMap = null;
        return (getHtmlString(context, (HashMap) JPO.unpackArgs(args), formatViewerMap));
    }

    public Object getHtmlString(Context context, HashMap paramMap, HashMap formatViewerMap) throws Exception {
        BusinessObject busObj = null;
        BusinessObjectList busObjList = (BusinessObjectList) paramMap.get("objectList");

        Vector columnCellContentList = new Vector();
        try {
            String lang = (String) context.getSession().getLanguage();
            String sTipView = i18nNow.getI18nString("emxTeamCentral.ContentSummary.ToolTipView", "emxTeamCentralStringResource", lang);

            StringBuffer htmlBuffer = new StringBuffer(300);

            if (null != busObjList && busObjList.size() > 0) {
                BusinessObjectItr busObjItr = new BusinessObjectItr(busObjList);
                while (busObjItr.next()) {
                    busObj = (BusinessObject) busObjItr.obj();
                    String objectId = busObj.getObjectId(context);
                    FormatList formats = busObj.getFormats(context);

                    for (int i = 0; i < formats.size(); i++) {
                        String format = ((Format) formats.get(i)).getName();
                        String[] viewerServletAndTip = getViewerServletAndTip(context, formatViewerMap, format);
                        String sViewerServletName = viewerServletAndTip[0];
                        String tipView = viewerServletAndTip[1];

                        if (sViewerServletName == null || sViewerServletName.length() == 0)
                            continue;

                        if (tipView != null && tipView.length() != 0)
                            sTipView = tipView;

                        FileList list = busObj.getFiles(context, format);

                        for (int j = 0; j < list.size(); j++) {
                            matrix.db.File file = (matrix.db.File) list.get(j);
                            String fileName = file.getName();
                            String sFileViewerLink = "/servlet/" + sViewerServletName;
                            String viewerURL = "../iefdesigncenter/emxInfoViewer.jsp?url=" + sFileViewerLink + "&id=" + objectId + "&format=" + format + "&file=" + MCADUrlUtil.hexEncode(fileName);
                            String viewerHref = viewerURL;
                            viewerHref = "javascript:openWindow('" + viewerURL + "')";
                            String url = getFeatureIconContent(viewerHref, "iconActionViewer.gif", sTipView + " (" + format + ")");

                            htmlBuffer.append(url);
                            htmlBuffer.append("&nbsp");
                        }
                    }
                }
            }
            columnCellContentList.add(htmlBuffer.toString());
        } catch (Exception e) {
            System.out.println("DSCShowViewerLink: getHtmlString: " + e.toString());
        }

        return columnCellContentList;
    }

    protected String[] getViewerServletAndTip(Context context, HashMap formatViewerMap, String format) throws Exception {
        String[] viewerServletAndTip = new String[2];
        String viewerInfo = "";

        boolean cacheViewers = false;

        if (formatViewerMap == null)
            cacheViewers = false;
        else
            cacheViewers = true;

        if (cacheViewers && formatViewerMap.containsKey(format)) {
            viewerInfo = (String) formatViewerMap.get(format);
        } else {
            // format and store all of them in a String seperated by comma
            MQLCommand prMQL = new MQLCommand();
            prMQL.open(context);
            prMQL.executeCommand(context, "execute program $1 $2", "eServicecommonGetViewers.tcl", format);
            viewerInfo = prMQL.getResult().trim();
            if (cacheViewers)
                formatViewerMap.put(format, viewerInfo);
        }

        if (null != viewerInfo && viewerInfo.length() > 0) {
            StringTokenizer viewerTokenizer = new StringTokenizer(viewerInfo, "|", false);
            String sErrorCode = "";
            if (viewerTokenizer.hasMoreTokens())
                sErrorCode = viewerTokenizer.nextToken();
            if (sErrorCode.equals("0")) {
                if (viewerTokenizer.hasMoreTokens())
                    viewerServletAndTip[0] = viewerTokenizer.nextToken();
                if (viewerTokenizer.hasMoreTokens())
                    viewerServletAndTip[1] = viewerTokenizer.nextToken();
            }
        }
        return viewerServletAndTip;
    }

    protected String getFeatureIconContent(String href, String featureImage, String toolTop) {
        StringBuffer featureIconContent = new StringBuffer();

        featureIconContent.append("<a href=\"");
        featureIconContent.append(href);
        featureIconContent.append("\" ><img src=\"../iefdesigncenter/images/");
        featureIconContent.append(featureImage);
        featureIconContent.append("\" border=\"0\" title=\"");
        featureIconContent.append(toolTop);
        featureIconContent.append("\"></a>");

        return featureIconContent.toString();
    }

}
