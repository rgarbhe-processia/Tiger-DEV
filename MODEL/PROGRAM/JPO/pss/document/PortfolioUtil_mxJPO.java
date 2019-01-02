package pss.document;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import matrix.db.Context;
import java.util.Locale;
import java.util.Calendar;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.engineering.EngineeringUtil;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.apps.domain.util.mxType;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainSymbolicConstants;
import java.util.HashMap;
import java.util.Hashtable;
import com.matrixone.apps.domain.util.eMatrixDateFormat;
import java.util.List;
import java.util.Map;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.MapList;
import matrix.db.Context;
import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeConstants;
import matrix.db.JPO;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import matrix.util.Pattern;
import matrix.util.SelectList;
import matrix.util.StringList;
import pss.constants.TigerConstants;
import pss.document.policy.DocumentUtil_mxJPO;

import com.matrixone.apps.domain.util.MqlUtil;
import java.util.Vector;
import matrix.db.BusinessObject;
import matrix.db.BusinessObjectList;
import java.util.Iterator;
import com.matrixone.apps.framework.ui.UICache;
import com.matrixone.apps.framework.ui.UIUtil;
import com.matrixone.apps.framework.ui.UINavigatorUtil;

public class PortfolioUtil_mxJPO {

    // TIGTK-5405 - 11-04-2017 - VB - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PortfolioUtil_mxJPO.class);
    // TIGTK-5405 - 11-04-2017 - VB - END

    /**
     * This Method is called to Connect the PSS_Portfolio object to Workspace Folder with the relationship 'Vaulted Objects'
     * @param context
     *            :context
     * @param args
     *            :NA
     * @return: NA
     * @throws Exception
     */
    public void connectPortfolioToFolder(Context context, String[] args) throws Exception {

        final String RELATIONSHIP_VAULTED_OBJECTS = PropertyUtil.getSchemaProperty(context, "relationship_VaultedDocuments");

        try {

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap requestMap = (HashMap) programMap.get("requestMap");
            String strWorkSpaceVaultObjectId = (String) requestMap.get("objectId");
            HashMap paramMap = (HashMap) programMap.get("paramMap");
            String strPortfolioObjectId = (String) paramMap.get("objectId");
            DomainObject domWorkSpace = DomainObject.newInstance(context, strWorkSpaceVaultObjectId);
            DomainObject domPortfolio = DomainObject.newInstance(context, strPortfolioObjectId);
            DomainRelationship.connect(context, domWorkSpace, RELATIONSHIP_VAULTED_OBJECTS, domPortfolio);

        } catch (Exception ex) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in connectPortfolioToFolder: ", ex);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }

    }// end of method

    /**
     * This Method is called to get the List of PSS_Documents connected to the PSS_Portfolio Object with the relationship 'PSS_Portfolio'
     * @param context
     *            :context
     * @param args
     *            -NA
     * @return MapList - Returns MapList containing the List of Documents connected to Portfolio
     * @throws Exception
     */

    public MapList getPortfolioContentIds(Context context, String[] args) throws Exception {

        final String RELATIONSHIP_PSS_PORTFOLIO = PropertyUtil.getSchemaProperty(context, "relationship_PSS_Portfolio");
        final String TYPE_PSS_DOCUMENT = PropertyUtil.getSchemaProperty(context, "type_PSS_Document");
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String strobjectId = (String) programMap.get("objectId");
        DomainObject domPortfolio = DomainObject.newInstance(context, strobjectId);
        StringList lstselectStmts = new StringList(3);
        lstselectStmts.addElement(DomainConstants.SELECT_ID);
        lstselectStmts.addElement(DomainConstants.SELECT_TYPE);
        lstselectStmts.addElement(DomainConstants.SELECT_NAME);
        StringList lstrelStmts = new StringList();
        lstrelStmts.add(DomainConstants.SELECT_RELATIONSHIP_NAME);
        Pattern relPattern = new Pattern(RELATIONSHIP_PSS_PORTFOLIO);
        // TIGTK-4150 - START
        relPattern.addPattern(DomainConstants.RELATIONSHIP_REFERENCE_DOCUMENT);
        // TIGTK-4150 - END
        Pattern typePattern = new Pattern(TYPE_PSS_DOCUMENT);
        MapList mlDocuments = domPortfolio.getRelatedObjects(context, relPattern.getPattern(), typePattern.getPattern(), lstselectStmts, lstrelStmts, false, true, (short) 1, null, null, 0);
        return mlDocuments;

    }// end of method

    /**
     * This Method is called to get the List of All connected Objects to the PSS_Portfolio Object with any relationship
     * @param context
     * @param args
     *            :NA
     * @return MapList:Returns MapList containing the List of all objects connected to Portfolio
     * @throws Exception
     */

    public MapList getReferencedByObjectIds(Context context, String[] args) throws Exception {

        // final String TYPE_PSS_DOCUMENT = PropertyUtil.getSchemaProperty(context, "type_PSS_Document");
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String strobjectId = (String) programMap.get("objectId");
        DomainObject domPortfolio = DomainObject.newInstance(context, strobjectId);

        StringList lstselectStmts = new StringList(3);
        lstselectStmts.addElement(DomainConstants.SELECT_ID);
        lstselectStmts.addElement(DomainConstants.SELECT_TYPE);
        lstselectStmts.addElement(DomainConstants.SELECT_NAME);
        StringList lstrelStmts = new StringList();
        lstrelStmts.add(DomainConstants.SELECT_RELATIONSHIP_NAME);

        MapList mlDocuments = domPortfolio.getRelatedObjects(context, "*", "*", lstselectStmts, lstrelStmts, true, false, (short) 1, null, null, 0);

        return mlDocuments;

    }// end of method

    /**
     * This Method is called to check if any "PSS_Document" objects connected to PSS_Portfolio is in other than 'Release' state, if all connected PSS_Documents are in 'Release state, only then Promote
     * 'PSS_Portfolio' to 'Release' State.
     * @param context
     * @param args
     *            -- "Object Id" of context Object
     * @return int - Returns the status whether to Promote or Restrict the Context Object
     * @throws Exception
     */
    public int checkForAssociatedDocumentState(Context context, String[] args) throws Exception {

        final String RELATIONSHIP_PSS_PORTFOLIO = PropertyUtil.getSchemaProperty(context, "relationship_PSS_Portfolio");

        final String TYPE_PSS_DOCUMENT = PropertyUtil.getSchemaProperty(context, "type_PSS_Document");
        // final String STATE_RELEASED = "RELEASED";
        // final String STATE_Released = "Released";

        int intRestrictOrPromoteStatus = 0;
        String strPFObjectId = args[0];
        DomainObject domPortFolioObj = DomainObject.newInstance(context, strPFObjectId);
        StringList lstSelectStmts = new StringList();
        StringList lstRelStmts = new StringList();
        lstSelectStmts.add(DomainConstants.SELECT_ID);
        lstSelectStmts.add(DomainConstants.SELECT_NAME);
        lstSelectStmts.add(DomainConstants.SELECT_CURRENT);
        lstSelectStmts.add(DomainConstants.SELECT_POLICY);
        lstRelStmts.add(DomainConstants.SELECT_RELATIONSHIP_ID);
        Pattern typePattern = new Pattern(TYPE_PSS_DOCUMENT);
        Pattern relPattern = new Pattern(RELATIONSHIP_PSS_PORTFOLIO);
        // MapList containing the "Documents" connected to "Portfolio" with "PSS_Portfolio" relationship
        MapList mlConnectedDocsList = domPortFolioObj.getRelatedObjects(context, relPattern.getPattern(), typePattern.getPattern(), lstSelectStmts, lstRelStmts, false, true, (short) 0, null, null, 0);

        // StringList documentList = domPortFolioObj.getInfoList(context, "from[" + RELATIONSHIP_PSS_PORTFOLIO + "].to.name");
        // Iterator i1 = documentList.iterator();
        int intConnectedDocsListSize = mlConnectedDocsList.size();

        if (intConnectedDocsListSize > 0) {
            for (int intIndex = 0; intIndex < intConnectedDocsListSize; intIndex++) {

                Map mapConnectedDocs = (Map) mlConnectedDocsList.get(intIndex);
                String strCurrentState = (String) mapConnectedDocs.get(DomainConstants.SELECT_CURRENT);
                // String strDocumentName = (String) mapConnectedDocs.get(DomainConstants.SELECT_NAME);
                // String strDocumentId = (String) mapConnectedDocs.get(DomainConstants.SELECT_ID);
                // DomainObject domDocuemntObj = DomainObject.newInstance(context, strDocumentId);
                // String strDocumentRevision = (String) domDocuemntObj.getInfo(context, DomainConstants.SELECT_REVISION);

                // Modify for Issue TIGTK-2947 START
                if (("RELEASED".equalsIgnoreCase(strCurrentState))) {
                    intRestrictOrPromoteStatus = 0;
                } else {
                    intRestrictOrPromoteStatus = 1;
                    break;
                }

                /*
                 * if (intRestrictOrPromoteStatus == 1) { String strAlertMessage = EngineeringUtil.i18nStringNow(context, "emxEngineeringCentral.Alert.PSS_DocumentsNotReleased",
                 * context.getSession().getLanguage()); MqlUtil.mqlCommand(context, "notice $1", strAlertMessage + " " + mapConnectedDocs.get(DomainConstants.SELECT_NAME) + " " + strDocumentRevision);
                 * }
                 */
            }
        }
        if (intRestrictOrPromoteStatus == 0) {
            // TIGTK-4141 - SteepGraph - 02-02-2017 - START
            // domPortFolioObj.promote(context); // Promote Portfolio
            // TIGTK-4141 - SteepGraph - 02-02-2017 - END
        } else {
            intRestrictOrPromoteStatus = 1;

            String strAlertMessage = EngineeringUtil.i18nStringNow(context, "emxEngineeringCentral.Alert.PSS_DocumentsNotReleased", context.getSession().getLanguage());

            MqlUtil.mqlCommand(context, "notice $1", strAlertMessage);
        }
        return intRestrictOrPromoteStatus;
        // Modify for Issue TIGTK-2947 END
    }// end of Method

    /**
     * This Method is called to Update the Previous Revision of the Portfolio to 'Obsolete' State when the current revision object is promoted to 'Released' State
     * @param context
     * @param args
     *            -- "Object Id" of context Object
     * @return int - Returns the status whether to Promote or Restrict the Context Object
     * @throws Exception
     * @Modified on : 23-08-2018 : TIGTK-16808
     */

    public int updatePreviousRevision(Context context, String[] args) throws Exception {
        try {
            int intStatus = 0;
            String strPFObjectId = args[0];
            final String KEY = EnoviaResourceBundle.getProperty(context, "emxComponents.Document.Key_Util");
            if (UIUtil.isNotNullAndNotEmpty(strPFObjectId)) {
                DomainObject domPortFolioObj = DomainObject.newInstance(context, strPFObjectId);

                BusinessObject boPreviousRev = domPortFolioObj.getPreviousRevision(context);
                if (boPreviousRev.exists(context)) {
                    DomainObject domPFPreviousrev = DomainObject.newInstance(context, boPreviousRev);

                    StringList slObjSelects = new StringList(3);
                    slObjSelects.add(DomainConstants.SELECT_CURRENT);
                    slObjSelects.add(DomainConstants.SELECT_TYPE);
                    slObjSelects.add(DomainConstants.SELECT_ID);

                    StringList slRelSelects = new StringList(2);
                    slRelSelects.add(DomainRelationship.SELECT_ID);
                    slRelSelects.add(DomainRelationship.SELECT_NAME);

                    Pattern relPattern = new Pattern(DomainConstants.RELATIONSHIP_REFERENCE_DOCUMENT);
                    relPattern.addPattern(DomainConstants.RELATIONSHIP_VAULTED_DOCUMENTS);
                    MapList mlConnectedObjectsOfPrevRev = domPFPreviousrev.getRelatedObjects(context, // context
                            relPattern.getPattern(), // relationship Pattern
                            DomainConstants.QUERY_WILDCARD, // type Pattern
                            slObjSelects, // object Selects
                            slRelSelects, // relationship Selects
                            true, // to direction
                            false, // from direction
                            (short) 0, // recurseToLevel
                            DomainConstants.EMPTY_STRING, // objectWhere
                            DomainConstants.EMPTY_STRING, // relationshipWhere
                            (short) 0, // limit
                            null, // includeType
                            null, // includeRelationship
                            null); // includeMap

                    for (int i = 0; i < mlConnectedObjectsOfPrevRev.size(); i++) {
                        Map<?, ?> mpConnectedObjectsOfPrevRev = (Map<?, ?>) mlConnectedObjectsOfPrevRev.get(i);
                        String strRelId = (String) mpConnectedObjectsOfPrevRev.get(DomainRelationship.SELECT_ID);
                        String strState = (String) mpConnectedObjectsOfPrevRev.get(DomainConstants.SELECT_CURRENT);
                        String strType = (String) mpConnectedObjectsOfPrevRev.get(DomainConstants.SELECT_TYPE);
                        String strSystemTypeName = PropertyUtil.getAliasForAdmin(context, "Type", strType, false);
                        StringBuffer sbKeyBuffer = new StringBuffer();
                        sbKeyBuffer.append(KEY);
                        sbKeyBuffer.append(strSystemTypeName);
                        StringList slStateValues = FrameworkUtil.split(EnoviaResourceBundle.getProperty(context, sbKeyBuffer.toString()), "|");

                        String strPrevRevConnectedObjectId = (String) mpConnectedObjectsOfPrevRev.get(DomainConstants.SELECT_ID);
                        String strRelationshipName = (String) mpConnectedObjectsOfPrevRev.get(DomainRelationship.SELECT_NAME);
                        DomainObject domPrevRevConnectedObject = DomainObject.newInstance(context, strPrevRevConnectedObjectId);
                        if (slStateValues.contains(strState)) {
                            // connect previous PF revision objects to current PF revision
                            DomainRelationship.connect(context, domPrevRevConnectedObject, strRelationshipName, domPortFolioObj);
                            // disconnect previous PF revision objects
                            DomainRelationship.disconnect(context, strRelId);
                        }
                    }
                    context.setUser(TigerConstants.PERSON_USER_AGENT);
                    domPFPreviousrev.setState(context, TigerConstants.STATE_PSS_PORTFOLIO_OBSOLETE);
                    intStatus = 0;
                }
            }
            return intStatus;
        } // Fix for FindBugs issue RuntimeException capture
        catch (RuntimeException e) {
            logger.error("Error in updatePreviousRevision: ", e);
            throw e;
        } catch (Exception ex) {
            logger.error("Error in  pss.document.PortfolioUtil : updatePreviousRevision(): ", ex);
            throw ex;
        }
    }// end of method

    /**
     * This Method is called to get the Higher Revision Icon displayed for the docuemnts with Latest Revision(if available)
     * @param context
     *            :context
     * @param args
     *            :NA
     * @return List - Returns the List of Documents of Higher Revision available
     * @throws Exception
     */

    public List getHigherRevisionIcon(Context context, String[] args) throws Exception {
        // final String RELATIONSHIP_VAULTED_OBJECTS = PropertyUtil.getSchemaProperty(context, "relationship_VaultedDocuments");
        final String RELATIONSHIP_PSS_PORTFOLIO = PropertyUtil.getSchemaProperty(context, "relationship_PSS_Portfolio");
        final String TYPE_PSS_PORTFOLIO = PropertyUtil.getSchemaProperty(context, "type_PSS_Portfolio");

        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        MapList mlBusinessObjectList = (MapList) programMap.get("objectList");
        Map mapparamMap = (Map) programMap.get("paramList");

        String parentId = (String) mapparamMap.get("objectId");
        DomainObject domPFObj = DomainObject.newInstance(context, parentId);

        String strPFobjState = domPFObj.getInfo(context, DomainConstants.SELECT_CURRENT);

        int iNumOfObjects = mlBusinessObjectList.size();
        List lstHigherRevExists = new Vector(iNumOfObjects);
        // Findbug Issue correction start
        // Date: 22/03/2017
        // By: Asha G.

        // Findbug Issue correction end

        String strHigherRevisionIconTag = "";
        String HIGHER_REVISION_ICON = "";
        String TOOLTIP_HIGHER_REVISION_ICON = "";

        if (!("InWork".equalsIgnoreCase(strPFobjState))) {

            // Reading the tooltip from property file.
            TOOLTIP_HIGHER_REVISION_ICON = EnoviaResourceBundle.getProperty(context, "emxTeamCentralStringResource", context.getLocale(),
                    "emxTeamCentral.PortfolioContent.PSS_ReleasedPortfolioHigherRevisionExists");

            HIGHER_REVISION_ICON = ("<img src=\"../common/images/iconSmallHigherRevisionDisabled.gif\"/>");
        } else {

            HIGHER_REVISION_ICON = ("<img src=\"../common/images/iconSmallHigherRevision.gif\"/>");

            TOOLTIP_HIGHER_REVISION_ICON = EnoviaResourceBundle.getProperty(context, "emxTeamCentralStringResource", context.getLocale(), "emxTeamCentral.PortfolioContent.PSS_HigherRevisionExists");
        }
        for (int j = 0; j < iNumOfObjects; j++) {

            Map mapDocuments = (Hashtable) mlBusinessObjectList.get(j);

            String objectId = (String) mapDocuments.get(DomainConstants.SELECT_ID);
            DomainObject domDocObj = DomainObject.newInstance(context, objectId);
            // String connectionId = domDocObj.getInfo(context, "to[" + RELATIONSHIP_PSS_PORTFOLIO + "].id");

            // Added for Issue TIGTK-3433 START
            StringList busSelects = new StringList(1);
            busSelects.add(DomainConstants.SELECT_ID);

            StringList relSelects = new StringList(1);
            relSelects.add(DomainConstants.SELECT_RELATIONSHIP_ID);

            String connectionId = null;

            MapList mlPortfolioConnectedList = domDocObj.getRelatedObjects(context, RELATIONSHIP_PSS_PORTFOLIO, TYPE_PSS_PORTFOLIO, busSelects, relSelects, true, false, (short) 0, "", "", 0);

            if (mlPortfolioConnectedList != null && !mlPortfolioConnectedList.isEmpty()) {
                int mlPortfolioConnectedListSize = mlPortfolioConnectedList.size();

                for (int k = 0; k < mlPortfolioConnectedListSize; k++) {
                    Map mpPortfolioObjList = (Map) mlPortfolioConnectedList.get(k);
                    String strPortfolioObjectId = "";
                    strPortfolioObjectId = (String) mpPortfolioObjList.get(DomainConstants.SELECT_ID);

                    if (strPortfolioObjectId.equals(parentId)) {
                        connectionId = (String) mpPortfolioObjList.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                        break;
                    }

                }
            }
            // Added for Issue TIGTK-3433 END

            BusinessObject boLastRevisionObject = domDocObj.getLastRevision(context);
            String strDocobjectIdLatest = boLastRevisionObject.getObjectId();

            if (!(strDocobjectIdLatest).equals(objectId)) {
                if (!("InWork".equalsIgnoreCase(strPFobjState))) {
                    strHigherRevisionIconTag = "<a href=\"#\" TITLE=\"" + " " + TOOLTIP_HIGHER_REVISION_ICON + "\">" + HIGHER_REVISION_ICON + "</a>";

                } else {
                    strHigherRevisionIconTag = "<a href=\"javascript:updateContentsToLatestRevision('" + objectId + "','" + connectionId + "','" + parentId + "')\" TITLE=\"" + " "
                            + TOOLTIP_HIGHER_REVISION_ICON + "\">" + HIGHER_REVISION_ICON + "</a>";

                }
            } else {
                strHigherRevisionIconTag = "";
            }
            lstHigherRevExists.add(strHigherRevisionIconTag);
        }
        return lstHigherRevExists;
    }// end of method

    /**
     * This Method is called to set the value of Attribute 'PSS_RevisionReason' and Revise the portfolio.
     * @param context
     *            :context
     * @param args
     *            :NA
     * @return :NA
     * @throws Exception
     */

    public void setRevisionReasonAndRevisePortfolio(Context context, String[] args) throws Exception {

        String ATTRIBUTE_REVISION_REASON = PropertyUtil.getSchemaProperty(context, "attribute_PSS_RevisionReason");
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        Map requestMap = (Map) programMap.get("requestMap");
        String strObjectId = (String) requestMap.get("objectId");
        DomainObject domObj = DomainObject.newInstance(context, strObjectId);
        BusinessObject lastRevObj = domObj.getLastRevision(context);
        String nextRev = lastRevObj.getNextSequence(context);
        String objectId = lastRevObj.getObjectId();
        String lastRevVault = lastRevObj.getVault();
        domObj.setId(objectId);
        BusinessObject revBO = domObj.revise(context, nextRev, lastRevVault);
        ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
        String strRevisionReason = (String) requestMap.get("PSS_RevisionReason");
        revBO.setAttributeValue(context, ATTRIBUTE_REVISION_REASON, strRevisionReason);
        ContextUtil.popContext(context);
    }// end of method

    /**
     * This Method is to get the Higher Revision of Document(if available) updated on the Portfolio Object if the Portfolio is in 'Released' state
     * @param context
     *            :context
     * @param args
     *            :NA Modified : TIGTK-2312 Issue regarding to the higher revision Modified Date: 28/07/16
     * @return :NA
     * @throws Exception
     */
    public void updateContentToLatestRevision(Context context, String[] args) throws Exception {
        try {
            final String RELATIONSHIP_PSS_PORTFOLIO = PropertyUtil.getSchemaProperty(context, "relationship_PSS_Portfolio");
            HashMap paramMapForJPO = (HashMap) JPO.unpackArgs(args);
            String strobjectId = (String) paramMapForJPO.get("objectId");
            String strParentId = (String) paramMapForJPO.get("parentId");

            // Added for Issue TIGTK-3433 START
            String strConnectionId = (String) paramMapForJPO.get("connectionId");
            // Added for Issue TIGTK-3433 END

            // DomainObject domPortObject = new DomainObject(strParentId);
            // String strConnectionId = domPortObject.getInfo(context, "from[" + RELATIONSHIP_PSS_PORTFOLIO + "].id");

            DomainObject domObj = new DomainObject(strobjectId);
            BusinessObject busObj = domObj.getLastRevision(context);
            String strDocIdLatest = busObj.getObjectId();
            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            DomainRelationship.disconnect(context, strConnectionId, true);
            DomainRelationship.connect(context, strParentId, RELATIONSHIP_PSS_PORTFOLIO, strDocIdLatest, true);
        } catch (Exception e) {
            throw e;
        } finally {
            ContextUtil.popContext(context);
        }
    }// end of method

    // TIGTK-6669 - 18-04-2017 - VP - START
    public int checkForLatestReleasedRevisionOfObject(Context context, String[] args) throws Exception {
        int nCheckresult = 0;
        try {
            String strObjectId = args[0];

            BusinessObject busCurrentObject = new BusinessObject(strObjectId);
            BusinessObject busNextRevObject = busCurrentObject.getNextRevision(context);
            if (busNextRevObject.exists(context)) {
                DomainObject domNextRevObject = DomainObject.newInstance(context, busNextRevObject);
                String strStateOfNextRevision = domNextRevObject.getInfo(context, DomainConstants.SELECT_CURRENT);
                if (strStateOfNextRevision.equals(TigerConstants.STATE_PSS_PORTFOLIO_RELEASED)) {
                    nCheckresult = 1;
                } else {
                    nCheckresult = checkForLatestReleasedRevisionOfObject(context, new String[] { domNextRevObject.getObjectId(context) });
                }
            }
        } catch (Exception e) {
            logger.error("Error in checkForLatestReleasedRevisionOfObject: ", e.toString());
            throw e;
        }
        return nCheckresult;
    }
    // TIGTK-6669 - 18-04-2017 - VP - END

}// end of class