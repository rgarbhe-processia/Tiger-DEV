
/*
 ** DSCWebFormActionsLink
 **
 ** Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 **
 ** Program to display Action Icons
 */
import matrix.db.Context;
import java.util.Hashtable;
import matrix.db.BusinessObject;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.server.beans.IEFIntegAccessUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import java.util.HashMap;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import matrix.db.JPO;
import matrix.db.BusinessObjectList;

public class DSCWebFormActionsLink_mxJPO extends DSCWebFormActionsLinkBase_mxJPO {
    /**
     * Constructor.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @throws Exception
     *             if the operation fails
     * @since Sourcing V6R2008-2
     */
    public DSCWebFormActionsLink_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
    }

    // TIGTK-9505: Start
    public String getHtmlString(Context context, String[] args) throws Exception {
        StringBuffer htmlBuffer = new StringBuffer(300);

        paramMap = (HashMap) JPO.unpackArgs(args);
        HashMap requestMap = (HashMap) paramMap.get("requestMap");
        String objectId = (String) requestMap.get("objectId");
        attrIsVersionObject = MCADMxUtil.getActualNameForAEFData(context, "attribute_IsVersionObject");
        attrSource = MCADMxUtil.getActualNameForAEFData(context, "attribute_Source");

        try {
            localeLanguage = (String) requestMap.get("languageStr");
            serverResourceBundle = new MCADServerResourceBundle(localeLanguage);
            cache = new IEFGlobalCache();
            util = new IEFIntegAccessUtil(context, serverResourceBundle, cache);
            _mxUtil = new MCADMxUtil(context, serverResourceBundle, cache);

            HashMap attributeMap = jpoUtil.getCommonDocumentAttributes(context, objectId);
            BusinessObject busObj = new BusinessObject(objectId);
            busObj.open(context);
            String majorObjectId = objectId;

            // to get the ECO connected List
            Hashtable emptyHashtable = new Hashtable();
            String sRelName = MCADMxUtil.getActualNameForAEFData(context, "relationship_NewSpecificationRevision");
            BusinessObjectList busList = util.getRelatedBusinessObjects(context, busObj, sRelName, "to", emptyHashtable);

            if (busList != null && busList.size() == 0) {
                sRelName = MCADMxUtil.getActualNameForAEFData(context, "relationship_AffectedItem");
                busList = util.getRelatedBusinessObjects(context, busObj, sRelName, "to", emptyHashtable);
                if (busList != null && busList.size() == 0) {
                    sRelName = MCADMxUtil.getActualNameForAEFData(context, "relationship_ChangeAffectedItem");
                    busList = util.getRelatedBusinessObjects(context, busObj, sRelName, "to", emptyHashtable);
                }
            }

            if (true == isVersionObject(attributeMap)) {
                BusinessObject majorObject = util.getMajorObject(context, busObj);
                majorObject.open(context);
                majorObjectId = majorObject.getObjectId();
                majorObject.close(context);
            }

            htmlBuffer.append(addSubscriptionLink(context, majorObjectId));

            String validObjectId = getViewerValidObjectId(context, attributeMap, objectId, majorObjectId);

            String integrationName = getIntegrationName(attributeMap);

            if (integrationName != null && util.getAssignedIntegrations(context).contains(integrationName)) {
                MCADGlobalConfigObject globalConfigObject = getGlobalConfigObject(context, objectId);

                String mode = (String) requestMap.get("mode");

                String jpoName = globalConfigObject.getFeatureJPO("OpenFromWeb");

                // TIGTK-9505: commented code to display open icon :Start
                /*
                 * if (null != jpoName && !"".equals(jpoName)) { htmlBuffer.append(addOpenFromWebLink(context, globalConfigObject, integrationName, objectId, jpoName)); }
                 */
                // TIGTK-9505: commented code to display open icon :End

                // [NDM] QWJ
                htmlBuffer.append(addDownloadStructureLink(context, objectId, isVersionObject(attributeMap)));

                // [NDM] : H68
                if (false == isVersionObject(attributeMap))
                    htmlBuffer.append(addLockUnlockLink(context, majorObjectId, integrationName, mode));

                // Check for connected ECO and disply of ECO icon
                if (busList != null && busList.size() > 0) {
                    BusinessObject relatedObj = null;
                    String ecoObjId = "";
                    String ecoObjType = "";
                    for (int j = 0; j < busList.size(); j++) {
                        relatedObj = busList.getElement(j);
                        ecoObjType = relatedObj.getTypeName();
                        // if(!"".equals(ecoObjType) && ecoObjType.equals("ECO"))
                        // {
                        ecoObjId = relatedObj.getObjectId();
                        // }
                    }

                    if (!"".equals(ecoObjId)) {
                        // htmlBuffer.append(addECOIconLink(context, ecoObjId));
                        htmlBuffer.append(addChangeMgmtIconLink(context, ecoObjId, ecoObjType));
                    }
                }
            } else {
                htmlBuffer.append(addDownloadLink(context, objectId));
            }

            htmlBuffer.append(addViewerLink(context, validObjectId));

            busObj.close(context);
        } catch (Exception e) {
        }

        return htmlBuffer.toString();
    }

    private String addDownloadStructureLink(Context context, String objectId, boolean isVersionedObject) // [NDM] QWJ
    {
        String downloadStructureToolTip = serverResourceBundle.getString("mcadIntegration.Server.AltText.DownloadStructure");

        String downloadStructureURL = "../integrations/DSCMCADGenericActionsProcess.jsp?action=DownloadStructure" + "&amp;Target Location=hiddenFrame" + "&amp;emxTableRowId=" + objectId
                + "&amp;isVersionedObject=" + isVersionedObject + "&amp;fromLocation=Table";
        String downloadStructureIcon = "../../common/images/iconActionDownload.gif";

        String url = getFeatureIconContent(downloadStructureURL, downloadStructureIcon, downloadStructureToolTip, null, "listHidden");

        return url;
    }
    // TIGTK-9505: End
}
