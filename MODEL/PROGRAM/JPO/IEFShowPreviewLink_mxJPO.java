
/*
 ** IEFShowPreviewLink
 **
 ** Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 **
 ** Program to display purge icon.
 */
import java.util.HashMap;
import java.util.Vector;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.apps.domain.util.MapList;

public class IEFShowPreviewLink_mxJPO {
    private HashMap integrationNameGCOTable = null;

    private MCADGlobalConfigObject globalConfigObject = null;

    private MCADServerResourceBundle serverResourceBundle = null;

    private MCADMxUtil util = null;

    private String localeLanguage = null;

    private MCADServerGeneralUtil generalUtil = null;

    private IEFGlobalCache cache = null;

    public IEFShowPreviewLink_mxJPO(Context context, String[] args) throws Exception {
    }

    public Object getHtmlString(Context context, String[] args) throws Exception {
        Vector columnCellContentList = new Vector();

        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        MapList relBusObjPageList = (MapList) paramMap.get("objectList");

        localeLanguage = (String) paramMap.get("LocaleLanguage");
        integrationNameGCOTable = (HashMap) paramMap.get("GCOTable");
        cache = new IEFGlobalCache();
        serverResourceBundle = new MCADServerResourceBundle(localeLanguage);

        util = new MCADMxUtil(context, serverResourceBundle, cache);

        for (int i = 0; i < relBusObjPageList.size(); i++) {
            StringBuffer htmlBuffer = new StringBuffer();

            try {
                HashMap objDetails = (HashMap) relBusObjPageList.get(i);
                String objectId = (String) objDetails.get("id");
                String integrationName = util.getIntegrationName(context, objectId);

                String sCadTypeAttrName = MCADMxUtil.getActualNameForAEFData(context, "attribute_CADType");
                String cadType = util.getAttributeForBO(context, objectId, sCadTypeAttrName);
                boolean hasImage = false;

                BusinessObject busObj = new BusinessObject(objectId);

                if (integrationName != null && integrationNameGCOTable.containsKey(integrationName)) {
                    globalConfigObject = (MCADGlobalConfigObject) integrationNameGCOTable.get(integrationName);

                    generalUtil = new MCADServerGeneralUtil(context, globalConfigObject, serverResourceBundle, cache);

                    Vector vDepDocDetails = generalUtil.getPreviewObjectInfo(context, busObj, cadType);

                    if (vDepDocDetails.size() == 3) {
                        hasImage = true;
                    }

                    if (hasImage) {
                        String sPreviewObjId = (String) vDepDocDetails.elementAt(0);
                        String sFormatName = (String) vDepDocDetails.elementAt(1);
                        String sFileName = (String) vDepDocDetails.elementAt(2);
                        String previewToolTip = serverResourceBundle.getString("mcadIntegration.Server.AltText.Preview");

                        String purgeHref = "javascript:previewObject('" + sPreviewObjId + "', '" + sFormatName + "', '" + sFileName + "')";

                        htmlBuffer.append(getFeatureIconContent(purgeHref, "iconPreview.gif", previewToolTip));
                    }
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
