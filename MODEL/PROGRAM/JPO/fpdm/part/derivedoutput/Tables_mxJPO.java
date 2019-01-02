// Pop context from User Agent
package fpdm.part.derivedoutput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.matrixone.MCADIntegration.utils.MCADUrlUtil;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PropertyUtil;

import matrix.db.Access;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.MQLCommand;
import matrix.util.StringList;
import pss.constants.TigerConstants;

public class Tables_mxJPO {
    private static Logger logger = LoggerFactory.getLogger("fpdm.part.specifications.Tables");

    /**
     * Get all linked Derived Output files linked to the related CAD objects<br>
     * @plm.usage Command: FPDM_MCIPartDerivedOutputs and table FPDM_PartDerivedOutputsTable
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public MapList getDerivedOutputTableData(Context context, String[] args) throws Exception {
        MapList mlNeutralFilesList = new MapList();
        try {
            HashMap<?, ?> paramMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            String sPartId = (String) paramMap.get("objectId");
            logger.debug("getDerivedOutputTableData() - sPartId = <" + sPartId + ">");

            // get linked CAD objects
            ArrayList<String> alLinkedCADs = getPartRelatedCADs(context, sPartId);
            logger.debug("getDerivedOutputTableData() - alLinkedCADs = <" + alLinkedCADs + ">");

            if (alLinkedCADs.size() > 0) {
                // get linked neutral files
                StringList slSelect = new StringList();
                slSelect.addElement(DomainConstants.SELECT_ID);
                slSelect.addElement(DomainConstants.SELECT_TYPE);
                slSelect.addElement(DomainConstants.SELECT_NAME);
                slSelect.addElement(DomainConstants.SELECT_REVISION);
                slSelect.addElement(DomainConstants.SELECT_FILE_NAME);
                slSelect.addElement(DomainConstants.SELECT_FILE_FORMAT);

                StringBuilder sbRelatioshipPattern = new StringBuilder(PropertyUtil.getSchemaProperty(context, "relationship_Viewable"));
                sbRelatioshipPattern.append(",").append(PropertyUtil.getSchemaProperty(context, "relationship_DerivedOutput"));

                DomainObject doCADObject = DomainObject.newInstance(context);
                MapList mlDerivedObjects = null;
                Map<?, ?> mDerivedObjectInfos = null;
                String sID = null;
                ArrayList<String> slFileName = null;
                ArrayList<String> slFileFormat = null;
                String sFileName = null;
                String sFileFormat = null;
                HashMap<String, Object> mNeutralFileInfos = null;
                boolean bHasAccess = false;
                for (String sCADID : alLinkedCADs) {
                    doCADObject.setId(sCADID);
                    // check access
                    Access acAccessMask = doCADObject.getAccessMask(context);
                    bHasAccess = acAccessMask.hasReadAccess();
                    logger.debug("getDerivedOutputTableData() - bHasAccess = <" + bHasAccess + ">");

                    // get related Viewable and Derived Output objects
                    mlDerivedObjects = getDerivedOutput(context, doCADObject, sbRelatioshipPattern, slSelect);
                    logger.debug("getDerivedOutputTableData() - sCADID = <" + sCADID + "> mlDerivedObjects = <" + mlDerivedObjects + ">");

                    for (Iterator<?> iterator = mlDerivedObjects.iterator(); iterator.hasNext();) {
                        mDerivedObjectInfos = (Map<?, ?>) iterator.next();
                        logger.debug("getDerivedOutputTableData() - mDerivedObjectInfos = <" + mDerivedObjectInfos + ">");

                        sID = (String) mDerivedObjectInfos.get(DomainConstants.SELECT_ID);
                        slFileName = fpdm.utils.SelectData_mxJPO.getListOfValues(mDerivedObjectInfos.get(DomainConstants.SELECT_FILE_NAME));
                        slFileFormat = fpdm.utils.SelectData_mxJPO.getListOfValues(mDerivedObjectInfos.get(DomainConstants.SELECT_FILE_FORMAT));

                        for (int i = 0; i < slFileName.size(); i++) {
                            sFileName = slFileName.get(i);
                            sFileFormat = slFileFormat.get(i);

                            mNeutralFileInfos = new HashMap<String, Object>();
                            mNeutralFileInfos.put("Type", (String) mDerivedObjectInfos.get(DomainConstants.SELECT_TYPE));
                            mNeutralFileInfos.put("ObjectName", (String) mDerivedObjectInfos.get(DomainConstants.SELECT_NAME));
                            mNeutralFileInfos.put("Revision", (String) mDerivedObjectInfos.get(DomainConstants.SELECT_REVISION));
                            mNeutralFileInfos.put("FileName", sFileName);
                            mNeutralFileInfos.put("format", sFileFormat);

                            mNeutralFileInfos.put("id", sID);
                            mNeutralFileInfos.put("rowid", sFileName + "|" + sFileFormat + "|" + sID);
                            mNeutralFileInfos.put("isDerivedOutputPage", "true");
                            mNeutralFileInfos.put("bHasAccess", Boolean.valueOf(bHasAccess));
                            mNeutralFileInfos.put("disableSelection", String.valueOf(!bHasAccess));
                            logger.debug("getDerivedOutputTableData() - mNeutralFileInfos = <" + mNeutralFileInfos + ">");

                            mlNeutralFilesList.add(mNeutralFileInfos);
                        }
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Error in getDerivedOutputTableData()\n", e);
            throw e;
        }

        return mlNeutralFilesList;
    }

    private MapList getDerivedOutput(Context context, DomainObject doCADObject, StringBuilder sbRelatioshipPattern, StringList slSelect) throws Exception {

        boolean bPushedContext = false;
        try {
            // Push context to User Agent
            ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, null, null);
            bPushedContext = true;

            // get related Viewable and Derived Output objects
            MapList mlDerivedObjects = doCADObject.getRelatedObjects(context, sbRelatioshipPattern.toString(), "*", slSelect, null, false, true, (short) 1, null, null, 0);

            return mlDerivedObjects;
        } catch (Exception e) {
            logger.error("Error in getDerivedOutput()\n", e);
            throw e;
        } finally {
            if (bPushedContext) {
                // Pop context from User Agent
                ContextUtil.popContext(context);
            }
        }

    }

    /**
     * Returns a link to the Derived Output object
     * @plm.usage table FPDM_PartDerivedOutputsTable - column Name
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     * @return
     * @throws Exception
     */
    public Vector<Boolean> getCheckboxes(Context context, String[] args) throws Exception {
        try {
            // Create result vector
            Vector<Boolean> vecResult = new Vector<Boolean>();

            // Get object list information from packed arguments
            Map<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");

            Map<?, ?> mapObjectInfo = null;
            // Do for each object
            for (Iterator<?> itrObjects = objectList.iterator(); itrObjects.hasNext();) {
                mapObjectInfo = (Map<?, ?>) itrObjects.next();

                vecResult.addElement((Boolean) mapObjectInfo.get("bHasAccess"));
            }

            return vecResult;
        } catch (Exception e) {
            logger.error("Error in getNames()\n", e);
            throw e;
        }
    }

    /**
     * Returns a link to the Derived Output object
     * @plm.usage table FPDM_PartDerivedOutputsTable - column Name
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     * @return
     * @throws Exception
     */
    public Vector<String> getNames(Context context, String[] args) throws Exception {
        try {
            // Create result vector
            Vector<String> vecResult = new Vector<String>();

            // Get object list information from packed arguments
            Map<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");

            Map<?, ?> mapObjectInfo = null;
            String sLabel = null;
            Boolean bHasAccess = null;
            // Do for each object
            for (Iterator<?> itrObjects = objectList.iterator(); itrObjects.hasNext();) {
                mapObjectInfo = (Map<?, ?>) itrObjects.next();
                sLabel = (String) mapObjectInfo.get("ObjectName");
                bHasAccess = (Boolean) mapObjectInfo.get("bHasAccess");

                StringBuilder sbLink = new StringBuilder();
                if (!bHasAccess) {
                    sbLink.append(sLabel);
                } else {
                    sbLink.append("<a title=\"").append(sLabel).append("\" ");
                    sbLink.append("href=\"javascript:emxTableColumnLinkClick('../common/emxTree.jsp?mode=insert&amp;emxSuiteDirectory=engineeringcentral&amp;objectId=");
                    sbLink.append((String) mapObjectInfo.get("id")).append("', '', '', 'false', 'content', '', '").append(sLabel).append("', 'false')\">");
                    sbLink.append(sLabel);
                    sbLink.append("</a>");
                }

                vecResult.addElement(sbLink.toString());
            }

            logger.debug("getNames() - vecResult = <" + vecResult + ">");

            return vecResult;
        } catch (Exception e) {
            logger.error("Error in getNames()\n", e);
            throw e;
        }
    }

    /**
     * Returns a link to the Derived Output object
     * @plm.usage table FPDM_PartDerivedOutputsTable - column Revision
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     * @return
     * @throws Exception
     */
    public Vector<String> getRevisions(Context context, String[] args) throws Exception {
        try {
            // Create result vector
            Vector<String> vecResult = new Vector<String>();

            // Get object list information from packed arguments
            Map<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");

            Map<?, ?> mapObjectInfo = null;
            Boolean bHasAccess = null;
            // Do for each object
            for (Iterator<?> itrObjects = objectList.iterator(); itrObjects.hasNext();) {
                mapObjectInfo = (Map<?, ?>) itrObjects.next();
                bHasAccess = (Boolean) mapObjectInfo.get("bHasAccess");

                if (!bHasAccess) {
                    vecResult.addElement("No Access");
                } else {
                    vecResult.addElement((String) mapObjectInfo.get("Revision"));
                }
            }

            logger.debug("getRevisions() - vecResult = <" + vecResult + ">");

            return vecResult;
        } catch (Exception e) {
            logger.error("Error in getRevisions()\n", e);
            throw e;
        }
    }

    /**
     * Returns a link to the Derived Output object
     * @plm.usage table FPDM_PartDerivedOutputsTable - column FileName
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     * @return
     * @throws Exception
     */
    public Vector<String> getFileNames(Context context, String[] args) throws Exception {
        try {
            // Create result vector
            Vector<String> vecResult = new Vector<String>();

            // Get object list information from packed arguments
            Map<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");

            Map<?, ?> mapObjectInfo = null;
            Boolean bHasAccess = null;
            // Do for each object
            for (Iterator<?> itrObjects = objectList.iterator(); itrObjects.hasNext();) {
                mapObjectInfo = (Map<?, ?>) itrObjects.next();
                bHasAccess = (Boolean) mapObjectInfo.get("bHasAccess");

                if (!bHasAccess) {
                    vecResult.addElement("No Access");
                } else {
                    vecResult.addElement((String) mapObjectInfo.get("FileName"));
                }
            }

            logger.debug("getFileNames() - vecResult = <" + vecResult + ">");

            return vecResult;
        } catch (Exception e) {
            logger.error("Error in getFileNames()\n", e);
            throw e;
        }
    }

    /**
     * Returns a link to the Derived Output object
     * @plm.usage table FPDM_PartDerivedOutputsTable - column Actions
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     * @return
     * @throws Exception
     */
    public Vector<String> getActionLinks(Context context, String[] args) throws Exception {
        try {
            // Create result vector
            Vector<String> vecResult = new Vector<String>();

            StringBuffer htmlBuffer = null;
            // Get object list information from packed arguments
            Map<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");

            String sObjectId = null;
            String sFileName = null;
            String sFileFormat = null;
            Map<?, ?> mapObjectInfo = null;
            Boolean bHasAccess = null;
            StringBuilder sbCheckoutHref = null;
            // Do for each object
            for (Iterator<?> itrObjects = objectList.iterator(); itrObjects.hasNext();) {
                mapObjectInfo = (Map<?, ?>) itrObjects.next();
                bHasAccess = (Boolean) mapObjectInfo.get("bHasAccess");

                if (!bHasAccess) {
                    vecResult.addElement("No Access");
                } else {
                    htmlBuffer = new StringBuffer();
                    sObjectId = (String) mapObjectInfo.get(DomainConstants.SELECT_ID);
                    sFileName = (String) mapObjectInfo.get("FileName");
                    sFileFormat = (String) mapObjectInfo.get("format");

                    sbCheckoutHref = new StringBuilder();
                    sbCheckoutHref.append("javascript:openWindow('");
                    sbCheckoutHref.append("../iefdesigncenter/DSCComponentCheckoutWrapper.jsp?objectId=");
                    sbCheckoutHref.append(sObjectId);
                    sbCheckoutHref.append("&amp;action=download&amp;format=");
                    sbCheckoutHref.append(sFileFormat);
                    sbCheckoutHref.append("&amp;fileName=");
                    sbCheckoutHref.append(MCADUrlUtil.hexEncode(sFileName));
                    sbCheckoutHref.append("&amp;refresh=false&amp;");
                    sbCheckoutHref.append("')");

                    htmlBuffer.append(getFeatureIconContent(sbCheckoutHref.toString(), "../../common/images/iconActionDownload.gif", "Download"));
                    htmlBuffer.append(getViewerURL(context, sObjectId, sFileFormat, sFileName));

                    vecResult.addElement(htmlBuffer.toString());
                }
            }

            return vecResult;
        } catch (Exception e) {
            logger.error("Error in getActionLinks()\n", e);
            throw e;
        }
    }

    /**
     * Returns a link to the Derived Output object
     * @plm.usage table FPDM_PartDerivedOutputsTable - column DetailPopup
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     * @return
     * @throws Exception
     */
    public Vector<String> getDetailPopups(Context context, String[] args) throws Exception {
        try {
            // Create result vector
            Vector<String> vecResult = new Vector<String>();

            // Get object list information from packed arguments
            Map<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");

            String sObjectId = null;
            StringBuilder sbLink = null;
            Map<?, ?> mapObjectInfo = null;
            Boolean bHasAccess = null;
            // Do for each object
            for (Iterator<?> itrObjects = objectList.iterator(); itrObjects.hasNext();) {
                mapObjectInfo = (Map<?, ?>) itrObjects.next();
                bHasAccess = (Boolean) mapObjectInfo.get("bHasAccess");

                if (!bHasAccess) {
                    vecResult.addElement("No Access");
                } else {
                    sObjectId = (String) mapObjectInfo.get("id");

                    sbLink = new StringBuilder("javascript:showNonModalDialog('");
                    sbLink.append("../common/emxTree.jsp?mode=insert&amp;emxSuiteDirectory=engineeringcentral&amp;objectId=");
                    sbLink.append(sObjectId);
                    sbLink.append("',550,875)");

                    vecResult.addElement(getFeatureIconContent(sbLink.toString(), "../../common/images/iconActionNewWindow.gif", ""));
                }
            }

            return vecResult;
        } catch (Exception e) {
            logger.error("Error in getDetailPopups()\n", e);
            throw e;
        }
    }

    private String getFeatureIconContent(String href, String featureImage, String toolTop) {
        StringBuffer featureIconContent = new StringBuffer();

        featureIconContent.append("<a href=\"");
        featureIconContent.append(href);
        featureIconContent.append("\" ><img src=\"images/");
        featureIconContent.append(featureImage);
        featureIconContent.append("\" border=\"0\" title=\"");
        featureIconContent.append(toolTop);
        featureIconContent.append("\"/></a>");

        return featureIconContent.toString();
    }

    private String getViewerURL(Context context, String objectId, String format, String fileName) {
        try {
            String sTipView = EnoviaResourceBundle.getProperty(context, "TeamCentral", "emxTeamCentral.ContentSummary.ToolTipView", context.getSession().getLanguage());

            StringBuffer htmlBuffer = new StringBuffer();

            // format and store all of them in a String separated by comma
            MQLCommand prMQL = new MQLCommand();
            prMQL.open(context);
            prMQL.executeCommand(context, "execute program $1 $2", "eServicecommonGetViewers.tcl", format);

            String sResult = prMQL.getResult().trim();
            // String error = prMQL.getError();
            String sViewerServletName = "";

            if (sResult.length() > 0) {
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
                if (sViewerServletName.length() == 0)
                    return "";

                String sFileViewerLink = "/servlet/" + sViewerServletName;
                String viewerURL = "../iefdesigncenter/emxInfoViewer.jsp?url=" + sFileViewerLink + "&amp;id=" + objectId + "&amp;format=" + format + "&amp;file=" + MCADUrlUtil.hexEncode(fileName);
                String viewerHref = "javascript:openWindow('" + viewerURL + "')";
                String url = getFeatureIconContent(viewerHref, "../../iefdesigncenter/images/iconActionViewer.gif", sTipView + " (" + format + ")");

                htmlBuffer.append(url);
            }

            return htmlBuffer.toString();
        } catch (RuntimeException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    /**
     * Get Related CAD objects IDs (Part Specification and Charted Drawing relationships)
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sPartID
     *            the Part ID
     * @return
     * @throws Exception
     */
    private ArrayList<String> getPartRelatedCADs(Context context, String sPartID) throws Exception {
        ArrayList<String> alLinkedCADs = new ArrayList<String>();
        try {
            StringBuilder sbRelatioshipPattern = new StringBuilder(DomainConstants.RELATIONSHIP_PART_SPECIFICATION);
            sbRelatioshipPattern.append(",").append(TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING);

            DomainObject doPart = DomainObject.newInstance(context, sPartID);

            // get related Viewable and Derived Output objects
            MapList mlLinkedCADs = doPart.getRelatedObjects(context, sbRelatioshipPattern.toString(), "*", new StringList(DomainConstants.SELECT_ID), null, false, true, (short) 1, null, null, 0);

            String sCADID = null;
            for (Object oCADInfo : mlLinkedCADs) {
                sCADID = (String) ((Map<?, ?>) oCADInfo).get(DomainConstants.SELECT_ID);
                if (!alLinkedCADs.contains(sCADID)) {
                    alLinkedCADs.add(sCADID);
                }
            }

        } catch (Exception e) {
            logger.error("Error in getPartRelatedCADs()\n", e);
            throw e;
        }

        return alLinkedCADs;
    }
}
