
/*
 ** DSCShowMarkupLink
 **
 ** Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 **
 ** Program to display Checkout Icon
 */

import java.util.HashMap;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.FileList;
import matrix.db.Format;
import matrix.db.FormatList;
import matrix.db.JPO;
import matrix.db.MQLCommand;
import matrix.util.MatrixException;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADUrlUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.i18nNow;

public class DSCShowMarkupLink_mxJPO {
    private HashMap integrationNameGCOTable = null;

    private MCADServerResourceBundle serverResourceBundle = null;

    private MCADMxUtil util = null;

    private String localeLanguage = null;

    private HashMap paramMap = null;

    private String strFileFormat = "";

    public DSCShowMarkupLink_mxJPO(Context context, String[] args) throws Exception {
        // super(context, args);
    }

    public String getViewableId(Context context, String busObjectId) throws MCADException {
        String mqlResult = null;
        String relViewable = "";

        try {
            relViewable = MCADMxUtil.getActualNameForAEFData(context, "relationship_Viewable");
            MQLCommand mqlCmd = new MQLCommand();
            mqlCmd.open(context);
            mqlCmd.executeCommand(context, "print bus $1 select $2 dump $3", busObjectId, "from[" + relViewable + "].to.id", "|");
            mqlResult = mqlCmd.getResult().trim();
            mqlCmd.close(context);

            if (mqlResult == null || mqlResult.length() <= 0)
                mqlResult = busObjectId;

            mqlResult = mqlResult.trim();

        } catch (MatrixException matrixException) {
            MCADServerException.createException(matrixException.getMessage(), matrixException);
        }

        return mqlResult;
    }

    public Object getHtmlString(Context context, String[] args) throws Exception {
        BusinessObject busObj = null;

        Vector columnCellContentList = new Vector();
        try {
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            MapList relBusObjPageList = (MapList) paramMap.get("objectList");

            String objectId = (String) paramMap.get("objectId");

            String lang = (String) context.getSession().getLanguage();
            MCADServerResourceBundle _serverResourceBundle = new MCADServerResourceBundle(lang);
            String sTipView = i18nNow.getI18nString("emxTeamCentral.ContentSummary.ToolTipView", "emxTeamCentralStringResource", lang);

            StringBuffer htmlBuffer = new StringBuffer(300);
            for (int k = 0; k < relBusObjPageList.size(); k++) {

                HashMap objDetails = (HashMap) relBusObjPageList.get(k);
                objectId = (String) objDetails.get("id");

                busObj = new BusinessObject(objectId);
                busObj.open(context);

                FormatList formats = busObj.getFormats(context);

                for (int i = 0; i < formats.size(); i++) {
                    String format = ((Format) formats.get(i)).getName();

                    // format and store all of them in a String seperated by comma
                    MQLCommand prMQL = new MQLCommand();
                    prMQL.open(context);
                    prMQL.executeCommand(context, "execute program $1 $2", "eServicecommonGetViewers.tcl", format);
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
                    }
                    // for each formats get all files attached each attachments represents
                    // one single row in the table
                    FileList list = busObj.getFiles(context, format);

                    for (int j = 0; j < list.size(); j++) {
                        matrix.db.File file = (matrix.db.File) list.get(j);
                        String fileName = file.getName();

                        String viewerURL = "";

                        String sFileViewerLink = "/servlet/" + sViewerServletName;

                        viewerURL = "../iefdesigncenter/emxInfoViewer.jsp?url=" + sFileViewerLink + "&mid=" + getViewableId(context, objectId) + "&format=" + format + "&file="
                                + MCADUrlUtil.hexEncode(fileName);

                        String viewerHref = "";// viewerURL;
                        if (sViewerServletName == null || "".equals(sViewerServletName)) {
                            Hashtable messageTokens = new Hashtable();
                            messageTokens.put("FORMAT", format);
                            String msg = _serverResourceBundle.getString("mcadIntegration.Server.Message.NoViewerRegisteredForFormat", messageTokens);
                            viewerHref = "javascript:showAlert('" + msg + "')";
                        } else {
                            viewerHref = "javascript:openWindow('" + viewerURL + "')";
                        }
                        String url = "";
                        String typeMarkup = MCADMxUtil.getActualNameForAEFData(context, "type_Markup");
                        if (busObj.getTypeName().indexOf(typeMarkup) >= 0)
                            url = getFeatureIconContent(viewerHref, "iconActionView.gif", sTipView, true);
                        else
                            continue;

                        htmlBuffer.append(url);
                        htmlBuffer.append("&nbsp");

                        columnCellContentList.add(htmlBuffer.toString());
                        htmlBuffer = new StringBuffer(300);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("DSCShowMarkupLink: getHtmlString: " + e.getMessage());
        } finally {
            if (busObj != null && busObj.isOpen())
                busObj.close(context);
        }
        return columnCellContentList;
    }

    private String getFeatureIconContent(String href, String featureImage, String toolTop, boolean displayAction) {
        StringBuffer featureIconContent = new StringBuffer();

        if (displayAction) {
            featureIconContent.append("<a href=\"");
            featureIconContent.append(href);
            featureIconContent.append("\" ><img src=\"images/");
            featureIconContent.append(featureImage);
            featureIconContent.append("\" border=\"0\" title=\"");
            featureIconContent.append(toolTop);
            featureIconContent.append("\"></a>");
        } else {
            featureIconContent.append("<a>&nbsp</a>");
        }
        return featureIconContent.toString();
    }
}
