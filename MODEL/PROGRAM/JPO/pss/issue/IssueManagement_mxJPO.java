package pss.issue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import com.matrixone.apps.common.Route;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.Relationship;
import matrix.db.RelationshipList;
import matrix.util.StringList;
import pss.constants.TigerConstants;

public class IssueManagement_mxJPO {
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(IssueManagement_mxJPO.class);

    /**
     * Description : This method is used to display Edit command at given state and required user
     * @param context
     * @param args
     * @return boolean
     * @throws Exception
     */
    public boolean showIssueEditCommand(Context context, String[] args) throws Exception {
        logger.debug("pss.issue.IssueManagement:showIssueEditCommand:START");
        boolean hasAccess = false;
        try {
            Map<?, ?> programMap = (Map<?, ?>) JPO.unpackArgs(args);
            String strIssueobjectId = (String) programMap.get("objectId");

            DomainObject.MULTI_VALUE_LIST.add("to[" + TigerConstants.RELATIONSHIP_ASSIGNEDISSUE + "].from.name");

            StringList slSelects = new StringList();
            StringList slAssignee = new StringList();

            slSelects.add("attribute[" + DomainConstants.ATTRIBUTE_CO_OWNER + "]");
            slSelects.add("attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");
            slSelects.add(DomainConstants.SELECT_CURRENT);
            slSelects.add("to[" + TigerConstants.RELATIONSHIP_ASSIGNEDISSUE + "].from.name");

            DomainObject domIssue = DomainObject.newInstance(context, strIssueobjectId);
            Map<?, ?> mpIssueInfo = domIssue.getInfo(context, slSelects);

            String strCurrentState = (String) mpIssueInfo.get(DomainConstants.SELECT_CURRENT);

            String strCoOwner = (String) mpIssueInfo.get("attribute[" + DomainConstants.ATTRIBUTE_CO_OWNER + "]");
            slAssignee.add(strCoOwner);

            if (TigerConstants.STATE_PSS_ISSUE_CREATE.equals(strCurrentState)) {
                String sIssueOriginator = (String) mpIssueInfo.get("attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");
                slAssignee.add(sIssueOriginator);
            }
            if (TigerConstants.STATE_PSS_ISSUE_ASSIGN.equals(strCurrentState) || TigerConstants.STATE_PSS_ISSUE_ACTIVE.equals(strCurrentState)) {
                StringList mlIssueAssignee = (StringList) mpIssueInfo.get("to[" + TigerConstants.RELATIONSHIP_ASSIGNEDISSUE + "].from.name");
                slAssignee.addAll(mlIssueAssignee);
            }
            String sContextUser = context.getUser();

            if (slAssignee.contains(sContextUser)) {
                hasAccess = true;
            }
            DomainObject.MULTI_VALUE_LIST.remove("to[" + TigerConstants.RELATIONSHIP_ASSIGNEDISSUE + "].from.name");
            logger.debug("pss.issue.IssueManagement:showIssueEditCommand:END");
        } catch (Exception ex) {
            logger.error("pss.issue.IssueManagement:showIssueEditCommand:ERROR ", ex);
            throw ex;
        }
        return hasAccess;
    }

    /**
     * Description : This method is used to exclude CR which already connected to Issue from the search list.
     * @param context
     * @param args
     * @return StringList
     * @throws Exception
     */

    public StringList excludeConnectedCRsToIssue(Context context, String[] args) throws Exception {
        Map<?, ?> programMap = (Map<?, ?>) JPO.unpackArgs(args);
        String strIssueobjectId = (String) programMap.get("objectId");
        DomainObject domIssue = DomainObject.newInstance(context, strIssueobjectId);
        StringList slConnectedCRs = domIssue.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_GOVERNINGISSUE + "].to.id");
        return slConnectedCRs;
    }

    /**
     * Description : This method is used to get CRs list connected to Issue.
     * @param context
     * @param args
     * @return MapList
     * @throws Exception
     */
    public MapList getConnectedCRToIssue(Context context, String[] args) throws Exception {

        // TIGTK-10702 : TS : 17/11/2017: Start
        MapList mlReturnList = new MapList();
        try {
            String strContextUser = context.getUser();
            HashMap<?, ?> paramMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            String strIssueID = (String) paramMap.get("objectId");
            StringList slObjectSle = new StringList(1);
            slObjectSle.addElement(DomainConstants.SELECT_ID);
            slObjectSle.add("attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");

            StringList slRelSle = new StringList(1);
            slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);
            DomainObject domIssueObj = new DomainObject(strIssueID);
            String strIssueCOOwner = domIssueObj.getAttributeValue(context, DomainConstants.ATTRIBUTE_CO_OWNER);
            MapList mlChangeRequest = domIssueObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_GOVERNINGISSUE, TigerConstants.TYPE_PSS_CHANGEREQUEST, slObjectSle, slRelSle, false, true,
                    (short) 1, null, null, 0);
            if (strIssueCOOwner.equalsIgnoreCase(strContextUser)) {
                mlReturnList.addAll(mlChangeRequest);

            } else {
                if (!mlChangeRequest.isEmpty()) {
                    Iterator itrCR = mlChangeRequest.iterator();

                    while (itrCR.hasNext()) {
                        Map MRelatedCR = (Map) itrCR.next();
                        String strOriginator = (String) MRelatedCR.get("attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");

                        if (!strContextUser.equalsIgnoreCase(strOriginator)) {
                            MRelatedCR.put("disableSelection", "true");

                        }
                        mlReturnList.add(MRelatedCR);
                    }
                }
            }
        } catch (Exception Ex) {
            logger.error("Error in getConnectedCRToIssue : ", Ex);
        }

        return mlReturnList;

    }
    // TIGTK-10702 : TS : 17/11/2017: End
    // TS-184 Create and Edit Issue From Global Actions -- TIGTK -6309 : 08/08/2017 : KWagh

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author KWagh This method will be called on edit form to list Project Manager
     */
    public StringList getProjectManager(Context context, String[] args) throws Exception {
        // TIGTK-15545:28-06-2018:START
        StringList slProjectManagerList = new StringList();
        // TIGTK-15545:28-06-2018:END
        try {

            String strIssueObjId = DomainConstants.EMPTY_STRING;
            Map<?, ?> programMap = (Map<?, ?>) JPO.unpackArgs(args);

            strIssueObjId = (String) programMap.get("OID");
            // TIGTK-15545:12-07-2018:START
            String strCOOwner = (String) programMap.get("CurrentCoOwner");
            // TIGTK-15545:12-07-2018:END
            StringList slRolesList = new StringList();
			pss.jv.common.PSS_JVUtil_mxJPO objPSS_JVUtil = new pss.jv.common.PSS_JVUtil_mxJPO();
			boolean isJVUser = objPSS_JVUtil.isJVProgramProject(context, strIssueObjId);
			if(isJVUser)
			{
				slRolesList.addElement(TigerConstants.ROLE_PSS_PROGRAM_MANAGER_JV);
			}
			else
			{
            slRolesList.addElement(TigerConstants.ROLE_PSS_PROGRAM_MANAGER);
			}

            pss.ecm.ui.CommonUtil_mxJPO commonObj = new pss.ecm.ui.CommonUtil_mxJPO(context, args);
            StringList slProjectManagerNameList = commonObj.getProgramProjectTeamMembersForChange(context, strIssueObjId, slRolesList, false);
            Iterator<?> itrPM = slProjectManagerNameList.iterator();
            while (itrPM.hasNext()) {
                String strUserName = (String) itrPM.next();
                String strUserID = PersonUtil.getPersonObjectID(context, strUserName);
                // TIGTK-15545:12-07-2018:START
                if (UIUtil.isNotNullAndNotEmpty(strCOOwner)) {
                    if (!strUserName.equalsIgnoreCase(strCOOwner)) {
                        slProjectManagerList.add(strUserID);
                    }
                } else {
                    // TIGTK-15545:12-07-2018:END
                    slProjectManagerList.add(strUserID);
                }

            }

        } catch (Exception e) {
            logger.error("Error in getProjectManager: ", e);
            throw e;
        }
        return slProjectManagerList;
    }

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author Kwagh This method will be called on Create form to list Project Manager
     */
    public StringList getProjectManagerOnIssueCreation(Context context, String[] args) throws Exception {
        StringList slPM = new StringList();
        try {
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            String strProgramProjectOID = (String) programMap.get("programProjectCode");
            DomainObject domProject = DomainObject.newInstance(context, strProgramProjectOID);
            // TIGTK-15545:11-07-2018:STARTS
            String strCoOwnerId = (String) programMap.get("coOwnerId");
            // TIGTK-15545:11-07-2018:STARTS
            StringList objSelect = new StringList();
            objSelect.add(DomainConstants.SELECT_ID);

			pss.jv.common.PSS_JVUtil_mxJPO objPSS_JVUtil = new pss.jv.common.PSS_JVUtil_mxJPO();
			boolean isJVUser = objPSS_JVUtil.isJVProgramProject(context, strProgramProjectOID);

			String strRelWhereclause = null;
			if(isJVUser)
			{
				strRelWhereclause = "attribute["+ TigerConstants.ATTRIBUTE_PSS_ROLE + "] == \"" + TigerConstants.ROLE_PSS_PROGRAM_MANAGER_JV + "\"";
			}else
				strRelWhereclause = " attribute[" + TigerConstants.ATTRIBUTE_PSS_ROLE + "] == \"" + TigerConstants.ROLE_PSS_PROGRAM_MANAGER + "\"";

            MapList mlConnectedPM = domProject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS, DomainConstants.TYPE_PERSON, objSelect, DomainConstants.EMPTY_STRINGLIST,
                    false, true, (short) 0, null, strRelWhereclause, 0);
            for (Object objPM : mlConnectedPM) {
                Map<?, ?> mPM = (Map<?, ?>) objPM;
                String strPMId = (String) mPM.get(DomainConstants.SELECT_ID);
                // TIGTK-15545:12-07-2018:STARTS
                if (UIUtil.isNotNullAndNotEmpty(strCoOwnerId)) {
                    if (!strCoOwnerId.equalsIgnoreCase(strPMId)) {
                        slPM.add(strPMId);
                    }
                } else {
                    // TIGTK-15545:12-07-2018:ENDS
                    slPM.add(strPMId);
                }

            }
            // Rutuja Ekatpure:18/8/2017:integer cannot be cast to map:Start
            if (slPM.size() == 0) {
                slPM.add("");
            } // Rutuja Ekatpure:18/8/2017:integer cannot be cast to map:End
        } catch (Exception e) {
            logger.error("Error in getProjectManagerOnIssueCreation: ", e);
            throw e;
        }
        return slPM;
    }

    // TS-184 Create and Edit Issue From Global Actions -- TIGTK -6309 : 08/08/2017 : KWagh

    // TS-184 Create and Edit Issue From Global Actions -- TIGTK -6309 : 08/08/2017 : KWagh
    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author KWagh This method will be used to define access on web-form field (ProjectCode,ProjectDescription)
     */
    public boolean checkAccessToProgramField(Context context, String[] args) throws Exception {

        boolean bResult = false;
        try {

            Map<?, ?> programMap = (Map<?, ?>) JPO.unpackArgs(args);

            String strfromCommand = (String) programMap.get("fromCommand");
            if (strfromCommand.equalsIgnoreCase("global") || strfromCommand.equalsIgnoreCase("ChangeManagement")) {
                bResult = true;
            }

        } catch (Exception e) {
            logger.error("Error in checkAccessToProgramField: ", e);
            throw e;
        }
        return bResult;
    }

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author KWagh This method will be used to define access on web-form field (ProjectCode,ProjectDescription)
     */
    public boolean checkAccessToProgramFieldSLC(Context context, String[] args) throws Exception {

        boolean bResult = false;
        try {

            Map<?, ?> programMap = (Map<?, ?>) JPO.unpackArgs(args);

            String strfromCommand = (String) programMap.get("fromCommand");
            if (strfromCommand.equalsIgnoreCase("SLC")) {
                bResult = true;
            }

        } catch (Exception e) {
            logger.error("Error in checkAccessToProgramFieldSLC: ", e);
            throw e;
        }
        return bResult;
    }
    // TS-184 Create and Edit Issue From Global Actions -- TIGTK -6309 : 08/08/2017 : KWagh

    // TS-184 Create and Edit Issue From Global Actions -- TIGTK -6309 : 08/08/2017 : KWagh
    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author Kwagh This method is used get Lead Project Manager on Issue creation and Validation process.
     */
    public String getLeadProjectManager(Context context, String[] args) throws Exception {

        String strLeadPM = DomainConstants.EMPTY_STRING;
        try {

            Map<?, ?> programMap = (Map<?, ?>) JPO.unpackArgs(args);

            String strProjectID = (String) programMap.get("strProgramProjectId");

            DomainObject domProject = DomainObject.newInstance(context, strProjectID);

            StringList objectSelect = new StringList(DomainConstants.SELECT_NAME);

			pss.jv.common.PSS_JVUtil_mxJPO  objPSS_JVUtil = new pss.jv.common.PSS_JVUtil_mxJPO();

			boolean isJVUser = objPSS_JVUtil.isJVProgramProject(context, strProjectID);
			String relWhere = null;
			if(isJVUser)
			{
				relWhere = "attribute[" + TigerConstants.ATTRIBUTE_PSS_POSITION + "]==\"Lead\"&&attribute[" + TigerConstants.ATTRIBUTE_PSS_ROLE + "]=='" + TigerConstants.ROLE_PSS_PROGRAM_MANAGER_JV
						+ "'";
			}
			else
			{
				relWhere = "attribute[" + TigerConstants.ATTRIBUTE_PSS_POSITION + "]==\"Lead\"&&attribute[" + TigerConstants.ATTRIBUTE_PSS_ROLE + "]=='" + TigerConstants.ROLE_PSS_PROGRAM_MANAGER
						+ "'";
			}
            pss.ecm.enoECMChange_mxJPO ecmChange = new pss.ecm.enoECMChange_mxJPO();
            MapList mlLeadPM = ecmChange.getMembersFromProgram(context, domProject, objectSelect, DomainConstants.EMPTY_STRINGLIST, DomainConstants.EMPTY_STRING, relWhere);

            Iterator<?> itrLeadPM = mlLeadPM.iterator();
            while (itrLeadPM.hasNext()) {
                Map<?, ?> mLeadPM = (Map<?, ?>) itrLeadPM.next();
                strLeadPM = (String) mLeadPM.get(DomainConstants.SELECT_NAME);
            }
        } catch (Exception e) {
            logger.error("Error in getLeadProjectManager: ", e);
            throw e;
        }

        return strLeadPM;
    }
    // TS-184 Create and Edit Issue From Global Actions -- TIGTK -6309 : 08/08/2017 : KWagh

    // TIGTK-6418:Rutuja Ekatpure:10/8/2017:Start
    /****
     * this method called from Reject Issue command for checking access
     * @param context
     * @param args
     * @return boolean true/false
     * @throws Exception
     */
    public boolean checkIssueRejectAccess(Context context, String[] args) throws Exception {
        logger.debug("pss.issue.IssueManagement_mxJPO:checkIssueRejectAccess:Start ");
        boolean bResult = false;
        Map<?, ?> programMap = (Map<?, ?>) JPO.unpackArgs(args);
        String strIssueObjId = (String) programMap.get("objectId");
        StringList slSelects = new StringList();
        slSelects.add("attribute[" + DomainConstants.ATTRIBUTE_CO_OWNER + "]");
        slSelects.add("attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");
        slSelects.add(DomainConstants.SELECT_CURRENT);
        DomainObject domIssue = DomainObject.newInstance(context, strIssueObjId);

        Map<?, ?> mpIssueInfo = domIssue.getInfo(context, slSelects);

        String strIssueState = (String) mpIssueInfo.get(DomainConstants.SELECT_CURRENT);
        String strCoOwner = (String) mpIssueInfo.get("attribute[" + DomainConstants.ATTRIBUTE_CO_OWNER + "]");
        String strOriginator = (String) mpIssueInfo.get("attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");

        StringList slIssueState = new StringList();
        slIssueState.add(TigerConstants.STATE_PSS_ISSUE_CREATE);
        slIssueState.add(TigerConstants.STATE_PSS_ISSUE_ASSIGN);
        slIssueState.add(TigerConstants.STATE_PSS_ISSUE_ACTIVE);
        slIssueState.add(TigerConstants.STATE_PSS_ISSUE_REVIEW);

        String strContextUser = context.getUser();
        // TIGTK-9941 -- START
        if (strIssueState.equalsIgnoreCase(TigerConstants.STATE_PSS_ISSUE_CREATE) || strIssueState.equalsIgnoreCase(TigerConstants.STATE_PSS_ISSUE_ASSIGN)
                || strIssueState.equalsIgnoreCase(TigerConstants.STATE_PSS_ISSUE_ACTIVE))
        // TIGTK-9941 -- END
        {
            if (strContextUser.equalsIgnoreCase(strOriginator) || strContextUser.equalsIgnoreCase(strCoOwner))
                bResult = true;
        }
        // TIGTK-9941 -- START
        else if (strIssueState.equalsIgnoreCase(TigerConstants.STATE_PSS_ISSUE_REVIEW))
        // TIGTK-9941 -- END
        {
            if (strContextUser.equalsIgnoreCase(strCoOwner))
                bResult = true;
        }
        logger.debug("pss.issue.IssueManagement_mxJPO:checkIssueRejectAccess:End ");
        return bResult;
    }
    // TIGTK-6418:Rutuja Ekatpure:10/8/2017:End

    // TIGTK-6415 : TS : 10/08/2017 : START
    public int checkAllRelatedCRState(Context context, String[] args) throws Exception {
        logger.debug("pss.issue.IssueManagement:checkAllRelatedCRState:START");
        int intReturn = 0;
        try {
            String strIssueObjId = args[0];
            DomainObject domIssue = DomainObject.newInstance(context, strIssueObjId);
            StringList slObjectSelects = new StringList();
            slObjectSelects.add(DomainConstants.SELECT_ID);
            slObjectSelects.add(DomainConstants.SELECT_CURRENT);
            MapList mlRelatedCRObjects = domIssue.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_GOVERNINGISSUE, TigerConstants.TYPE_PSS_CHANGEREQUEST, slObjectSelects, null, false, true,
                    (short) 0, null, null, 0);
            StringList slAllowedCRStateList = new StringList();
            slAllowedCRStateList.add(TigerConstants.STATE_COMPLETE_CR);
            slAllowedCRStateList.add(TigerConstants.STATE_REJECTED_CR);

            for (Object objCR : mlRelatedCRObjects) {
                Map<?, ?> mObjectMap = (Map<?, ?>) objCR;
                String strCRCurrent = (String) mObjectMap.get(DomainConstants.SELECT_CURRENT);
                if (!slAllowedCRStateList.contains(strCRCurrent)) {
                    intReturn = 1;
                    break;
                }
            }
            logger.debug("pss.issue.IssueManagement:checkAllRelatedCRState:END");
        } catch (Exception e) {
            logger.error("pss.issue.IssueManagement:checkAllRelatedCRState:ERROR", e);
            throw e;
        } finally {
            if (intReturn == 1) {
                String strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                        "PSSEnterpriseChangeMgt.Notice.CompleteOrRejectAllRelatedCR");

                MqlUtil.mqlCommand(context, "notice $1", strMessage);
            }
        }

        return intReturn;
    }
    // TIGTK-6415 : TS : 10/08/2017 : END

    // TIGTK-6406:Pranjali Tupe :2/8/2017:START
    /****
     * Action Trigger on Create state of PSS_Issue to create a Route between Review and Accepted state
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public void createReviewRouteForIssue(Context context, String[] args) throws Exception {

        logger.debug("pss.issue.IssueManagement:createReviewRouteForIssue:START");
        try {
            String sIssueID = args[0];
            String strRouteBaseState = args[1];
            String strRouteBasePurpose = args[2];
            String strRouteScope = args[3];
            String strRouteCompletionAction = args[4];
            String strRouteAutoStopOnRejection = args[5];
            String strRouteStateCondition = args[6];
            String strIssueName = args[8];
            String ATTRIBUTE_ROUTE_COMPLETION_ACTION = PropertyUtil.getSchemaProperty(context, "attribute_RouteCompletionAction");
            String ATTRIBUTE_STATE_CONDITION = PropertyUtil.getSchemaProperty(context, "attribute_StateCondition");
            String ATTRIBUTE_SCOPE = PropertyUtil.getSchemaProperty(context, "attribute_Scope");
            String ATTRIBUTE_AUTO_STOP_ON_RECJECTION = PropertyUtil.getSchemaProperty(context, "attribute_AutoStopOnRejection");

            DomainObject domIssue = DomainObject.newInstance(context, sIssueID);
            // Create new Route Object if route is not present.
            StringList slRouteIds = domIssue.getInfoList(context, "from[" + DomainConstants.RELATIONSHIP_OBJECT_ROUTE + "].to.id");
            if (slRouteIds != null && !slRouteIds.isEmpty()) {
                return; // Route is already present. So Do not create it.
            }
            // Create Route object and Set mandatory attributes on Route object.
            Hashtable<String, String> htObjectRouteRelAttributeMap = new Hashtable<>();
            htObjectRouteRelAttributeMap.put("routeBasePurpose", strRouteBasePurpose);
            htObjectRouteRelAttributeMap.put(sIssueID, strRouteBaseState);
            htObjectRouteRelAttributeMap.put(ATTRIBUTE_SCOPE, strRouteScope);
            htObjectRouteRelAttributeMap.put(ATTRIBUTE_STATE_CONDITION, strRouteStateCondition);
            htObjectRouteRelAttributeMap.put(DomainConstants.ATTRIBUTE_ROUTE_BASE_STATE, strRouteBaseState);

            Hashtable<String, String> htRouteAttributeMap = new Hashtable<>();
            htRouteAttributeMap.put(ATTRIBUTE_ROUTE_COMPLETION_ACTION, strRouteCompletionAction);
            htRouteAttributeMap.put(ATTRIBUTE_AUTO_STOP_ON_RECJECTION, strRouteAutoStopOnRejection);
            htRouteAttributeMap.put(DomainConstants.ATTRIBUTE_ROUTE_BASE_PURPOSE, strRouteBasePurpose);

            Route routeObject = createRoute(context, sIssueID, htRouteAttributeMap, htObjectRouteRelAttributeMap);

            String strCOOwner = domIssue.getAttributeValue(context, DomainConstants.ATTRIBUTE_CO_OWNER);

            conneteIssueApprovalRouteWithCoOwner(context, strCOOwner, strIssueName, routeObject);
            routeObject.setOwner(context, strCOOwner);

            logger.debug("pss.issue.IssueManagement:createReviewRouteForIssue:END");
        } catch (RuntimeException ex) {
            logger.error("pss.issue.IssueManagement:createReviewRouteForIssue:ERROR ", ex);
            throw ex;
        }
    }

    // TIGTK-6406:Pranjali Tupe :2/8/2017:END
    /**
     * This Method Connects Issue Co-Owner with Route
     * @param context
     * @param strCOOwner
     * @param strIssueName
     * @param routeObject
     * @throws FrameworkException
     */
    private void conneteIssueApprovalRouteWithCoOwner(Context context, String strCOOwner, String strIssueName, Route routeObject) throws FrameworkException {
        String strPersonId = PersonUtil.getPersonObjectID(context, strCOOwner);
        DomainObject domIssueCoOwner = DomainObject.newInstance(context, strPersonId);
        DomainRelationship domRelRouteNodeId = DomainRelationship.connect(context, routeObject, DomainConstants.RELATIONSHIP_ROUTE_NODE, domIssueCoOwner);
        DomainRelationship domRelRouteNode = new DomainRelationship(domRelRouteNodeId);

        // setting attribute values on relationship.
        HashMap<String, String> hmRouteNodeRelationshipAttributes = new HashMap<String, String>();
        hmRouteNodeRelationshipAttributes.put(DomainConstants.ATTRIBUTE_DUEDATE_OFFSET, "7");
        hmRouteNodeRelationshipAttributes.put(DomainConstants.ATTRIBUTE_DATE_OFFSET_FROM, "Route Start Date");
        hmRouteNodeRelationshipAttributes.put(DomainConstants.ATTRIBUTE_ASSIGNEE_SET_DUEDATE, "No");
        hmRouteNodeRelationshipAttributes.put(DomainConstants.ATTRIBUTE_TEMPLATE_TASK, "No");
        hmRouteNodeRelationshipAttributes.put(DomainConstants.ATTRIBUTE_REVIEW_TASK, "No");
        hmRouteNodeRelationshipAttributes.put(DomainConstants.ATTRIBUTE_ALLOW_DELEGATION, "TRUE");
        hmRouteNodeRelationshipAttributes.put(DomainConstants.ATTRIBUTE_ROUTE_ACTION, "Approve");
        hmRouteNodeRelationshipAttributes.put(DomainConstants.ATTRIBUTE_TASK_REQUIREMENT, "Optional");
        hmRouteNodeRelationshipAttributes.put(DomainConstants.ATTRIBUTE_ROUTE_INSTRUCTIONS, "Do Review For Issue: " + strIssueName);
        hmRouteNodeRelationshipAttributes.put(DomainConstants.ATTRIBUTE_TITLE, "Approval Task For Issue: " + strIssueName);
        hmRouteNodeRelationshipAttributes.put(DomainConstants.ATTRIBUTE_ROUTE_SEQUENCE, "1");
        domRelRouteNode.setAttributeValues(context, hmRouteNodeRelationshipAttributes);
    }

    /**
     * This Method Creates and returns Route Object.
     * @param context
     * @param sIssueID
     * @param htRouteAttributeMap
     * @param htObjectRouteRelAttributeMap
     * @return
     * @throws FrameworkException
     */
    private Route createRoute(Context context, String sIssueID, Hashtable<String, String> htRouteAttributeMap, Hashtable<String, String> htObjectRouteRelAttributeMap) throws FrameworkException {

        Route routeObject = new Route();
        Map<?, ?> routeMap = Route.createRouteWithScope(context, sIssueID, null, null, true, htObjectRouteRelAttributeMap);
        String strRouteId = (String) routeMap.get("routeId");
        routeObject.setId(strRouteId);
        routeObject.setAttributeValues(context, htRouteAttributeMap);
        return routeObject;

    }

    // TIGTK-6406:Pranjali Tupe :3/8/2017:START
    /****
     * Check Trigger on various states of PSS_Issue to check promote access
     * @param context
     * @param args
     * @return int
     * @throws Exception
     */
    public int checkIssuePromoteAccess(Context context, String[] args) throws Exception {
        logger.debug("pss.issue.IssueManagement:checkIssuePromoteAccess:START");
        int hasAccess = 1;
        try {
            // Get Issue object id and next state from args[].
            String sIssueID = args[0];
            String sNextState = args[1];

            // Create DomainObject of Issue from Issue ID and get State of Issue.
            DomainObject domIssue = DomainObject.newInstance(context, sIssueID);

            // Get list of assignee if NextState is not equal to Assign.
            StringList slAssignee = new StringList();
            StringList slBusSelect = new StringList();

            slBusSelect.add(DomainConstants.SELECT_NAME);
            slBusSelect.add(DomainConstants.SELECT_ID);

            StringList slRelSelect = new StringList(1);
            slRelSelect.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

            if (!"state_Assign".equals(sNextState)) {
                MapList mlIssueAssignee = domIssue.getRelatedObjects(context, TigerConstants.RELATIONSHIP_ASSIGNEDISSUE, DomainConstants.TYPE_PERSON, slBusSelect, slRelSelect, true, false, (short) 1,
                        null, null, 0);

                if (!mlIssueAssignee.isEmpty()) {
                    for (Object objAssignee : mlIssueAssignee) {
                        Map<?, ?> mAssignee = (Map<?, ?>) objAssignee;
                        String sAssigneeName = (String) mAssignee.get(DomainConstants.SELECT_NAME);
                        slAssignee.add(sAssigneeName);
                    }
                }
            }
            // TIGTK-10069 : START: 3-oct-2017
            if (TigerConstants.STATE_PSS_ISSUE_ASSIGN.equals(sNextState)) {
                // check context user has admin roles :PLM support Team , Global admin
                if (PersonUtil.hasAssignment(context, TigerConstants.ROLE_PSS_GLOBAL_ADMINISTRATOR) || PersonUtil.hasAssignment(context, TigerConstants.ROLE_PSS_PLM_SUPPORT_TEAM)) {
                    hasAccess = 0;
                }
            }
            // TIGTK-10069 : END: 3-oct-2017
            // Get Co-Owners of Issue object
            String sCOOwners = domIssue.getAttributeValue(context, DomainConstants.ATTRIBUTE_CO_OWNER);
            if (UIUtil.isNotNullAndNotEmpty(sCOOwners)) {
                slAssignee.add(sCOOwners);
            }
            // Get Context User.
            String sContextUser = context.getUser();
            // If Context user is Issue assignee or Co-Owner of Issue object then only user can promote the object (i.e return 0) otherwise return 1.

            if (slAssignee.contains(sContextUser)) {
                hasAccess = 0;
            }
            // TIGTK-12763 : VB:START
            else {
                if (PersonUtil.hasAssignment(context, TigerConstants.ROLE_PSS_GLOBAL_ADMINISTRATOR) || PersonUtil.hasAssignment(context, TigerConstants.ROLE_PSS_PLM_SUPPORT_TEAM)) {
                    hasAccess = 0;
                }
            }
            // TIGTK-12763 : VB:END
            logger.debug("pss.issue.IssueManagement:checkIssuePromoteAccess:END");

        } catch (Exception ex) {
            hasAccess = -1;
            logger.error("pss.issue.IssueManagement:checkIssuePromoteAccess:ERROR ", ex);
            throw ex;
        } finally {
            if (hasAccess == 1) {
                String strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSSEnterpriseChangeMgt.Notice.NoPromoteAccess");
                MqlUtil.mqlCommand(context, "notice $1", strMessage);
            }
        }
        return hasAccess;

    }
    // TIGTK-6406:Pranjali Tupe :3/8/2017:END

    // TIGTK-6409:Pranjali Tupe :8/8/2017:START
    /****
     * Check Trigger on various states of PSS_Issue to check demote access
     * @param context
     * @param args
     * @return int
     * @throws Exception
     */
    public int checkIssueDemoteAccess(Context context, String[] args) throws Exception {
        logger.debug("pss.issue.IssueManagement:checkIssueDemoteAccess:START");
        int hasAccess = 1;
        try {
            // Get Issue object id and next state from args[].
            String sIssueID = args[0];
            String sNextState = args[1];

            StringList slSelects = new StringList();

            slSelects.add("attribute[" + DomainConstants.ATTRIBUTE_CO_OWNER + "]");
            slSelects.add("attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");

            DomainObject domIssue = DomainObject.newInstance(context, sIssueID);
            Map<?, ?> mpIssueInfo = domIssue.getInfo(context, slSelects);

            String strCoOwner = (String) mpIssueInfo.get("attribute[" + DomainConstants.ATTRIBUTE_CO_OWNER + "]");
            String sIssueOriginator = (String) mpIssueInfo.get("attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");
            StringList slAssignee = new StringList();
            if (UIUtil.isNotNullAndNotEmpty(strCoOwner)) {
                slAssignee.add(strCoOwner);
            }

            if (TigerConstants.STATE_PSS_ISSUE_CREATE.equals(sNextState)) {
                slAssignee.add(sIssueOriginator);
            } else if (TigerConstants.STATE_PSS_ISSUE_ACTIVE.equals(sNextState)) {

                HashMap<String, String> programMap = new HashMap<String, String>();
                programMap.put("id", sIssueID);
                // TIGTK-13538 : 05-03-2018 : START
                programMap.put("calledFrom", "checkIssueDemoteAccess");
                // TIGTK-13538 : 05-03-2018 : END
                String[] strArgs = JPO.packArgs(programMap);
                pss.ecm.notification.IssueNotificationUtil_mxJPO IssueNotificationUtilObj = new pss.ecm.notification.IssueNotificationUtil_mxJPO(context, null);
                StringList slRoteAssigneeList = IssueNotificationUtilObj.getIssueRouteAssignee(context, strArgs);
                slAssignee.addAll(slRoteAssigneeList);
            }
            // Get Context User.
            String sContextUser = context.getUser();
            // If Context user is Issue assignee or Co-Owner of Issue object then only user can promote the object (i.e return 0) otherwise return 1.

            if (slAssignee.contains(sContextUser)) {
                hasAccess = 0;
            } else {
                // TIGTK-12763 : VB:START
                if (PersonUtil.hasAssignment(context, TigerConstants.ROLE_PSS_GLOBAL_ADMINISTRATOR) || PersonUtil.hasAssignment(context, TigerConstants.ROLE_PSS_PLM_SUPPORT_TEAM)) {
                    hasAccess = 0;
                }
                // TIGTK-12763 : VB:END
            }
            logger.debug("pss.issue.IssueManagement:checkIssueDemoteAccess:END");
        } catch (Exception ex) {
            hasAccess = -1;
            logger.error("pss.issue.IssueManagement:checkIssueDemoteAccess:ERROR ", ex);
            throw ex;
        } finally {
            if (hasAccess == 1) {
                String strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSSEnterpriseChangeMgt.Notice.NoDemoteAccess");
                MqlUtil.mqlCommand(context, "notice $1", strMessage);
            }
        }
        return hasAccess;

    }
    // TIGTK-6409:Pranjali Tupe :3/8/2017:END

    // TIGTK-6409:Pranjali Tupe :3/8/2017:START
    /****
     * Action Trigger on Active state of PSS_Issue to demote the Issue to Create state
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public void demoteIssueToCreate(Context context, String[] args) throws Exception {
        logger.debug("pss.issue.IssueManagement:demoteIssueToCreate:START");
        try {
            // Get Issue object from args[].
            String sIssueID = args[0];
            // Create DomainObject of Issue from Issue ID
            DomainObject domIssue = DomainObject.newInstance(context, sIssueID);
            // Call setState(Cotext,String) API on created DomainObject of Issue
            domIssue.setState(context, TigerConstants.STATE_PSS_ISSUE_CREATE); // Demote Issue to Create state

            logger.debug("pss.issue.IssueManagement:demoteIssueToCreate:END");
        } catch (Exception ex) {
            logger.error("pss.issue.IssueManagement:demoteIssueToCreate:ERROR ", ex);
            throw ex;
        }

    }
    // TIGTK-6409:Pranjali Tupe :3/8/2017:END

    // TIGTK-6412:Pranjali Tupe :3/8/2017:START
    /****
     * Action Trigger on Active state of PSS_Issue to start the Route between Review and Accepted state
     * @param args
     * @return
     * @throws Exception
     */
    public void startReviewRouteForIssue(Context context, String[] args) throws Exception {

        logger.debug("pss.issue.IssueManagement:startReviewRouteForIssue:START");
        boolean isContextPushed = false;
        try {
            // Get Issue object from args[].
            String sIssueID = args[0];
            // Create DomainObject of Issue from Issue ID.
            DomainObject domIssue = DomainObject.newInstance(context, sIssueID);
            // Get all Routes connected to Issue.
            StringList objectSelect = new StringList();
            objectSelect.add(DomainConstants.SELECT_ID);
            String ATTRIBUTE_ROUTE_STATUS = PropertyUtil.getSchemaProperty(context, "attribute_RouteStatus");
            String SELECT_ATTRIBUTE_ROUTE_STATUS = "attribute[" + ATTRIBUTE_ROUTE_STATUS + "]";
            objectSelect.add(SELECT_ATTRIBUTE_ROUTE_STATUS);

            MapList mlConnectedRoute = domIssue.getRelatedObjects(context, DomainConstants.RELATIONSHIP_OBJECT_ROUTE, DomainConstants.TYPE_ROUTE, objectSelect, null, false, true, (short) 1, null,
                    null, 0);
            // Iterate over Route MapList and Check for Route Status. If Status is stopped or finished resume the Route else Start the Route.

            for (Object objConnectedRoute : mlConnectedRoute) {
                Map<?, ?> mpRoute = (Map<?, ?>) objConnectedRoute;
                String sRouteID = (String) mpRoute.get(DomainConstants.SELECT_ID);
                String sRouteStatus = (String) mpRoute.get(SELECT_ATTRIBUTE_ROUTE_STATUS);
                Route route = new Route(sRouteID);
                String sUser = context.getUser();
                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, TigerConstants.PERSON_USER_AGENT), "", "");
                isContextPushed = true;
                route.setOwner(context, sUser);
                ContextUtil.popContext(context);
                isContextPushed = false;
                if ("Stopped".equals(sRouteStatus) || "Finished".equals(sRouteStatus)) {
                    // Restarting the already connected Route
                    route.resume(context);
                } else {
                    route.setAttributeValue(context, ATTRIBUTE_ROUTE_STATUS, "Started");
                    route.setState(context, Route.STATE_ROUTE_IN_PROCESS);
                }
            }
            logger.debug("pss.issue.IssueManagement:startReviewRouteForIssue:END");
        } catch (Exception ex) {
            logger.error("pss.issue.IssueManagement:startReviewRouteForIssue:ERROR ", ex);
            throw ex;
        } finally {
            if (isContextPushed == true) {
                ContextUtil.popContext(context);
            }
        }
    }

    // TS-185 Issue-Create From SLC and Change Management -- TIGTK -6312 : 09/08/2017 : KWagh
    /**
     * This function is used to display list of Issue objects connected to object as "Reported Against"
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author Kwagh
     */
    public MapList getConnectedIssuesToObject(Context context, String[] args) throws Exception {
        try {
            Map<?, ?> programMap = (Map<?, ?>) JPO.unpackArgs(args);
            String strObjectID = (String) programMap.get("objectId");

            DomainObject domObject = DomainObject.newInstance(context, strObjectID);
            StringList slObjectSelects = new StringList();
            slObjectSelects.addElement(DomainConstants.SELECT_ID);
            StringList slRelSelects = new StringList();
            slRelSelects.addElement(DomainRelationship.SELECT_ID);

            MapList mlConnectedIssues = domObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_ISSUE, DomainConstants.QUERY_WILDCARD, slObjectSelects, slRelSelects, true, false, (short) 1,
                    DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, 0);

            return mlConnectedIssues;
        } catch (Exception ex) {
            logger.error("Error in getConnectedIssuesToObject: ", ex);
            throw ex;
        }
    }
    // TS-185 Issue-Create From SLC and Change Management -- TIGTK -6312 : 09/08/2017 : KWagh
    // TIGTK-6412:Pranjali Tupe :3/8/2017:END

    // TIGTK-6415:Pranjali Tupe :16/8/2017:START
    /****
     * check Trigger on Accepted state of PSS_Issue. If there are no related CR to a Issue then allow only Co-owner to promote the Issue
     * @param args
     * @return int
     * @throws Exception
     */
    public int checkIssuePromoteAccessAtAcceptedState(Context context, String[] args) throws Exception {
        logger.debug("pss.issue.IssueManagement:checkIssuePromoteAccessAtAcceptedState:START");
        int hasAccess = 1;
        try {
            // Get Issue object id from args[].
            String sIssueID = args[0];
            // Create DomainObject of Issue from Issue ID
            DomainObject domIssue = DomainObject.newInstance(context, sIssueID);

            StringList slAssignee = new StringList();
            StringList slObjectSelects = new StringList();
            slObjectSelects.add(DomainConstants.SELECT_ID);
            slObjectSelects.add(DomainConstants.SELECT_CURRENT);
            MapList mlRelatedCRObjects = domIssue.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_GOVERNINGISSUE, TigerConstants.TYPE_PSS_CHANGEREQUEST, slObjectSelects, null, false, true,
                    (short) 0, null, null, 0);

            if (mlRelatedCRObjects.isEmpty()) {
                // Get Co-Owner of Issue object
                String sCOOwners = domIssue.getAttributeValue(context, DomainConstants.ATTRIBUTE_CO_OWNER);
                if (UIUtil.isNotNullAndNotEmpty(sCOOwners)) {
                    slAssignee.add(sCOOwners);
                }
                // Get Context User.
                String sContextUser = context.getUser();
                // If Context user is Co-Owner of Issue object then only user can promote the object
                if (slAssignee.contains(sContextUser)) {
                    hasAccess = 0;
                } // TIGTK-12763 : VB:START
                else {
                    if (PersonUtil.hasAssignment(context, TigerConstants.ROLE_PSS_GLOBAL_ADMINISTRATOR) || PersonUtil.hasAssignment(context, TigerConstants.ROLE_PSS_PLM_SUPPORT_TEAM)) {
                        hasAccess = 0;
                    }
                } // TIGTK-12763 : VB:END
            } else {
                hasAccess = 0;
            }

            logger.debug("pss.issue.IssueManagement:checkIssuePromoteAccessAtAcceptedState:END");

        } catch (Exception ex) {
            hasAccess = -1;
            logger.error("pss.issue.IssueManagement:checkIssuePromoteAccessAtAcceptedState:ERROR ", ex);
            throw ex;
        } finally {
            if (hasAccess == 1) {
                String strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSSEnterpriseChangeMgt.Notice.NoPromoteAccess");
                MqlUtil.mqlCommand(context, "notice $1", strMessage);
            }
        }
        return hasAccess;
    }
    // TIGTK-6415:Pranjali Tupe :16/8/2017:START

    // TIGTK-6412:Pranjali Tupe :21/8/2017:START
    /****
     * This trigger Checks if Mandatory Attributes are filled before Issue is promoted to Review state
     * @param context
     * @param args
     * @return int
     * @throws Exception
     */
    public int checkIssueMandatoryAttributesAtReview(Context context, String[] args) throws Exception {
        logger.debug("pss.issue.IssueManagement:CheckIssueMandatoryAttributesAtReview:START");
        int hasAccess = 1;
        try {
            String sIssueID = args[0];
            DomainObject domIssue = DomainObject.newInstance(context, sIssueID);
            String strCountermeasure = domIssue.getInfo(context, "attribute[" + TigerConstants.ATTRIBUTE_PSS_COUNTERMEASURE + "]");

            if (!strCountermeasure.isEmpty()) {
                hasAccess = 0;
            }

            logger.debug("pss.issue.IssueManagement:CheckIssueMandatoryAttributesAtReview:END");
        } catch (Exception ex) {
            hasAccess = -1;
            logger.error("pss.issue.IssueManagement:CheckIssueMandatoryAttributesAtReview:ERROR ", ex);
            throw ex;
        } finally {
            if (hasAccess == 1) {
                String strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                        "PSSEnterpriseChangeMgt.Notice.IssueMandatoryAttributeAtReviewState");
                MqlUtil.mqlCommand(context, "notice $1", strMessage);
            }
        }
        return hasAccess;
    }
    // TIGTK-6412:Pranjali Tupe :21/8/2017:END

    // TIGTK-6406:Pranjali Tupe :21/8/2017:START
    /**
     * Description : This method is used to display Add Existing and Remove command for Issue Reported Against
     * @param context
     * @param args
     * @return boolean
     * @throws Exception
     */
    public boolean showIssueReportedAgainstCommands(Context context, String[] args) throws Exception {
        logger.debug("pss.issue.IssueManagement:showIssueReportedAgainstCommands:START");
        boolean hasAccess = false;
        try {
            Map<?, ?> programMap = (Map<?, ?>) JPO.unpackArgs(args);
            String strIssueobjectId = (String) programMap.get("objectId");

            DomainObject.MULTI_VALUE_LIST.add("to[" + TigerConstants.RELATIONSHIP_ASSIGNEDISSUE + "].from.name");
            StringList slAssignee = new StringList();

            StringList slSelects = new StringList();
            slSelects.add("attribute[" + DomainConstants.ATTRIBUTE_CO_OWNER + "]");
            slSelects.add("attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");
            slSelects.add(DomainConstants.SELECT_CURRENT);
            slSelects.add("to[" + TigerConstants.RELATIONSHIP_ASSIGNEDISSUE + "].from.name");

            DomainObject domIssue = DomainObject.newInstance(context, strIssueobjectId);
            Map<?, ?> mpIssueInfo = domIssue.getInfo(context, slSelects);

            String strCoOwner = (String) mpIssueInfo.get("attribute[" + DomainConstants.ATTRIBUTE_CO_OWNER + "]");
            slAssignee.add(strCoOwner);

            String strCurrentState = (String) mpIssueInfo.get(DomainConstants.SELECT_CURRENT);
            if (TigerConstants.STATE_PSS_ISSUE_CREATE.equals(strCurrentState)) {
                String sIssueOriginator = (String) mpIssueInfo.get("attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");
                slAssignee.add(sIssueOriginator);
            }
            if (TigerConstants.STATE_PSS_ISSUE_ASSIGN.equals(strCurrentState) || TigerConstants.STATE_PSS_ISSUE_ACTIVE.equals(strCurrentState)) {
                StringList mlIssueAssignee = (StringList) mpIssueInfo.get("to[" + TigerConstants.RELATIONSHIP_ASSIGNEDISSUE + "].from.name");
                slAssignee.addAll(mlIssueAssignee);
            }
            // Get Context User.
            String sContextUser = context.getUser();

            if (slAssignee.contains(sContextUser)) {
                hasAccess = true;
            }
            DomainObject.MULTI_VALUE_LIST.remove("to[" + TigerConstants.RELATIONSHIP_ASSIGNEDISSUE + "].from.name");
            logger.debug("pss.issue.IssueManagement:showIssueReportedAgainstCommands:END");
        } catch (Exception ex) {
            logger.error("pss.issue.IssueManagement:showIssueReportedAgainstCommands:ERROR ", ex);
            throw ex;
        }
        return hasAccess;
    }

    // TIGTK-6406:Pranjali Tupe :21/8/2017:END
    // TIGTK-6406:Pranjali Tupe :21/8/2017:START
    /**
     * Description : This method is used to display Add Existing and Remove command for Issue Resolved By
     * @param context
     * @param args
     * @return boolean
     * @throws Exception
     */
    public boolean showIssueResolvedByCommands(Context context, String[] args) throws Exception {
        logger.debug("pss.issue.IssueManagement:showIssueResolvedByCommands:START");
        boolean hasAccess = false;
        try {
            Map<?, ?> programMap = (Map<?, ?>) JPO.unpackArgs(args);
            String strIssueobjectId = (String) programMap.get("objectId");
            StringList slSelects = new StringList();
            ArrayList<String> alAssignee = new ArrayList<String>();
            String strCurrentState = DomainConstants.EMPTY_STRING;
            slSelects.add("attribute[" + DomainConstants.ATTRIBUTE_CO_OWNER + "].value");
            slSelects.add("attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "].value");
            // START :: TIGTK-13568 :: ALM-5676
            slSelects.add(DomainConstants.SELECT_CURRENT);
            slSelects.add("to[" + TigerConstants.RELATIONSHIP_ASSIGNEDISSUE + "].from.name");
            BusinessObject boIssue = new BusinessObject(strIssueobjectId);
            BusinessObjectWithSelect bows = boIssue.select(context, slSelects);
            // START :: TIGTK-18267 :: ALM-6416
            StringList slIssueAssignee = bows.getSelectDataList("to[" + TigerConstants.RELATIONSHIP_ASSIGNEDISSUE + "].from.name");
            if (slIssueAssignee != null && !slIssueAssignee.isEmpty()) {
                alAssignee.addAll(slIssueAssignee);
            }
            // alAssignee.addAll(bows.getSelectDataList("to[" + TigerConstants.RELATIONSHIP_ASSIGNEDISSUE + "].from.name"));
            // END :: TIGTK-18267 :: ALM-6416
            alAssignee.add(bows.getSelectData("attribute[" + DomainConstants.ATTRIBUTE_CO_OWNER + "].value"));
            alAssignee.add(bows.getSelectData("attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "].value"));
            strCurrentState = bows.getSelectData(DomainConstants.SELECT_CURRENT);
            // END :: TIGTK-13568 :: ALM-5676
            String sContextUser = context.getUser();
            /* if (alAssignee.contains(sContextUser){ */
            // START :: TIGTK-13568 :: ALM-5676
            if (alAssignee.contains(sContextUser) && UIUtil.isNotNullAndNotEmpty(strCurrentState)
                    && (TigerConstants.STATE_PSS_ISSUE_ACTIVE.equals(strCurrentState) || TigerConstants.STATE_PSS_ISSUE_ASSIGN.equals(strCurrentState))) {
                hasAccess = true;
            }
            // END :: TIGTK-13568 :: ALM-5676
            logger.debug("pss.issue.IssueManagement:showIssueResolvedByCommands:END");
        } catch (Exception ex) {
            logger.error("pss.issue.IssueManagement:showIssueResolvedByCommands:ERROR ", ex);
            throw ex;
        }
        return hasAccess;
    }
    // TIGTK-6406:Pranjali Tupe :21/8/2017:END

    // TIGTK-9548:Pooja Mantri :30/8/2017:START
    /**
     * Description : This method is used to display Add Existing and Remove command for Issue Reference Documents category
     * @param context
     * @param args
     * @return boolean
     * @throws Exception
     */
    public boolean showIssueReferenceDocumentsCommands(Context context, String[] args) throws Exception {
        logger.debug("pss.issue.IssueManagement:showIssueReferenceDocumentsCommands:START");
        boolean hasAccess = false;
        try {
            Map<?, ?> programMap = (Map<?, ?>) JPO.unpackArgs(args);
            String strIssueobjectId = (String) programMap.get("objectId");

            StringList slAssignee = new StringList();
            DomainObject.MULTI_VALUE_LIST.add("to[" + TigerConstants.RELATIONSHIP_ASSIGNEDISSUE + "]." + DomainConstants.SELECT_FROM_NAME);
            StringList slSelects = new StringList();
            slSelects.add("attribute[" + DomainConstants.ATTRIBUTE_CO_OWNER + "]");
            slSelects.add("attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");
            slSelects.add(DomainConstants.SELECT_CURRENT);
            // TIGTK-13528 : 05-03-2018 : START
            slSelects.add("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "]." + DomainConstants.SELECT_FROM_ID);
            slSelects.add("to[" + TigerConstants.RELATIONSHIP_ASSIGNEDISSUE + "]." + DomainConstants.SELECT_FROM_NAME);
            // TIGTK-13528 : 05-03-2018 : END

            DomainObject domIssue = DomainObject.newInstance(context, strIssueobjectId);
            Map<?, ?> mpIssueInfo = domIssue.getInfo(context, slSelects);
            String strCoOwner = (String) mpIssueInfo.get("attribute[" + DomainConstants.ATTRIBUTE_CO_OWNER + "]");
            String strCurrentState = (String) mpIssueInfo.get(DomainConstants.SELECT_CURRENT);
            // TIGTK-13528 : 05-03-2018 : START
            StringList slIssueAllowedState = new StringList();
            slIssueAllowedState.add(TigerConstants.STATE_PSS_ISSUE_CREATE);
            slIssueAllowedState.add(TigerConstants.STATE_PSS_ISSUE_ASSIGN);
            slIssueAllowedState.add(TigerConstants.STATE_PSS_ISSUE_ACTIVE);
            slIssueAllowedState.add(TigerConstants.STATE_PSS_ISSUE_REVIEW);
            // TIGTK-13528 : 05-03-2018 : END
            if (slIssueAllowedState.contains(strCurrentState)) {
                slAssignee.add(strCoOwner);
            }

            // TIGTK-13528 : 05-03-2018 : START
            if (strCurrentState.equalsIgnoreCase(TigerConstants.STATE_PSS_ISSUE_CREATE)) {
                String sIssueOriginator = (String) mpIssueInfo.get("attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");
                slAssignee.add(sIssueOriginator);
            } else if (strCurrentState.equalsIgnoreCase(TigerConstants.STATE_PSS_ISSUE_ASSIGN) || strCurrentState.equalsIgnoreCase(TigerConstants.STATE_PSS_ISSUE_ACTIVE)) {
                StringList slAssigneeList = (StringList) mpIssueInfo.get("to[" + TigerConstants.RELATIONSHIP_ASSIGNEDISSUE + "]." + DomainConstants.SELECT_FROM_NAME);
                if (slAssigneeList != null && !slAssigneeList.isEmpty()) {
                    slAssignee.addAll(slAssigneeList);
                }
            } else if (strCurrentState.equalsIgnoreCase(TigerConstants.STATE_PSS_ISSUE_REVIEW)) {
                HashMap<String, String> mProgramMap = new HashMap<String, String>();
                mProgramMap.put("id", strIssueobjectId);
                mProgramMap.put("calledFrom", "showIssueReferenceDocumentsCommands");
                String[] strArgs = JPO.packArgs(mProgramMap);
                pss.ecm.notification.IssueNotificationUtil_mxJPO IssueNotificationUtilObj = new pss.ecm.notification.IssueNotificationUtil_mxJPO(context, null);
                StringList slRouteAssigneeList = (StringList) IssueNotificationUtilObj.getIssueRouteAssignee(context, strArgs);
                slAssignee.addAll(slRouteAssigneeList);
            } else if (strCurrentState.equalsIgnoreCase(TigerConstants.STATE_PSS_ISSUE_ACCEPTED)) {
                pss.ecm.ui.CommonUtil_mxJPO commonUtilObj = new pss.ecm.ui.CommonUtil_mxJPO(context, args);
                StringList slPMList = commonUtilObj.getProgramProjectTeamMembersForChange(context, strIssueobjectId, new StringList(TigerConstants.ROLE_PSS_PROGRAM_MANAGER), true);
                slAssignee.addAll(slPMList);
            }
            // TIGTK-13528 : 05-03-2018 : END

            // Get Context User
            String sContextUser = context.getUser();
            if (slAssignee.contains(sContextUser)) {
                hasAccess = true;
            }
            DomainObject.MULTI_VALUE_LIST.remove("to[" + TigerConstants.RELATIONSHIP_ASSIGNEDISSUE + "]." + DomainConstants.SELECT_FROM_NAME);
            logger.debug("pss.issue.IssueManagement:showIssueReferenceDocumentsCommands:END");
        } catch (Exception ex) {
            logger.error("pss.issue.IssueManagement:showIssueReferenceDocumentsCommands:ERROR ", ex);
            throw ex;
        }
        return hasAccess;
    }

    // TIGTK-9548:Pooja Mantri :30/8/2017:END
    // TIGTK-7577:Hiren Tarapara :22/8/2017:START
    /****
     * This method is used as programHTMLOutput field in Clone Issue Webform
     * @param context
     * @param args
     * @return String
     * @throws Exception
     */
    public String getIssueCloneFormFields(Context context, String[] args) throws Exception {
        logger.debug("pss.issue.IssueManagement:getIssueCloneFormFields:START");
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        Map fieldMap = (HashMap) programMap.get("fieldMap");
        Map settingsMap = (HashMap) fieldMap.get("settings");
        String strSettingName = (String) settingsMap.get("attribute");
        StringBuffer strBufReturn = new StringBuffer();
        strBufReturn.append("<select name=\"" + strSettingName + "\" >"); // Define the <select> element
        String strPropertyKey = "PSS_EnterpriseChangeMgt.CloneIssue.SelectBox." + strSettingName;
        String strLanguage = context.getSession().getLanguage();
        String strPropertyValue = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", new Locale(strLanguage), strPropertyKey);
        StringList slPropertyValues = FrameworkUtil.split(strPropertyValue, ",");
        for (int i = 0; i < slPropertyValues.size(); i++) {
            strBufReturn.append(" <option value=\"" + slPropertyValues.get(i) + "\" >" + slPropertyValues.get(i) + "</option>");
        }
        strBufReturn.append("</select>");
        logger.debug("pss.issue.IssueManagement:getIssueCloneFormFields:END");
        return strBufReturn.toString();

    }

    // TIGTK-7577:Hiren Tarapara :22/8/2017:END
    /****
     * This method is used as post Process JPO for clone Issue.
     * @param context
     * @param args
     * @return String[]
     * @throws Exception
     */
    public String[] cloneAndConnectIssue(Context context, String[] args) throws Exception {
        logger.debug("pss.issue.IssueManagement:cloneAndConnectIssue:START");
        String[] oidsArray = null;
        try {

            HashMap programMap = JPO.unpackArgs(args);
            String strIssueobjectId = (String) programMap.get("objectId");
            String strIssueNoOfClone = (String) programMap.get("IssueCloneCopies");
            String strIssueLinkReportedAgainstItem = (String) programMap.get("LinkReportedAgainstItem");
            String strIssueLinkReferenceDoc = (String) programMap.get("LinkReferenceDocument");
            String strIssueLinkAssignee = (String) programMap.get("LinkAssignee");
            String strIssueLinkToOriginal = (String) programMap.get("LinkToOriginal");
            String strProgramProjectOID = (String) programMap.get("ProgramProjectOID");
            int noofClone = Integer.parseInt(strIssueNoOfClone);
            // Create Domain Object of Context Issue and copy all the attributes
            DomainObject domOriginalIssue = DomainObject.newInstance(context, strIssueobjectId);
            String strAutoNumberSeries = DomainConstants.EMPTY_STRING;
            oidsArray = new String[noofClone];
            for (int i = 0; i < noofClone; i++) {
                // TIGTK-17379 : 22-10-2018 : START
                HashMap<String, String> hmSetAttributes = new HashMap<String, String>();
                // TIGTK-17379 : 22-10-2018 : END
                String strAutoName = DomainObject.getAutoGeneratedName(context, DomainObject.SYMBOLIC_type_Issue, strAutoNumberSeries);
                DomainObject domClonedIssue = new DomainObject(domOriginalIssue.cloneObject(context, strAutoName));
                String clonedObjectId = domClonedIssue.getId(context);
                oidsArray[i] = clonedObjectId;
                // TIGTK-17379 : 22-10-2018 : START
                hmSetAttributes.put(TigerConstants.ATTRIBUTE_BRANCH_TO, "None");
                hmSetAttributes.put(DomainConstants.ATTRIBUTE_ORIGINATOR, context.getUser());
                domClonedIssue.setAttributeValues(context, hmSetAttributes);
                // TIGTK-17379 : 22-10-2018 : END
                // TIGTK-16781 : 20-08-2018 : START
                if ("Clone".equalsIgnoreCase(strIssueLinkReferenceDoc)) {
                    cloneAndConnectReferenceDocumentsToIssue(context, domOriginalIssue, clonedObjectId);
                }
                // TIGTK-16781 : 20-08-2018 : END
            }
            DomainObject domProgramProject = DomainObject.newInstance(context, strProgramProjectOID);
            DomainRelationship.connect(context, domProgramProject, TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA, true, oidsArray);
            // Associate Affected Items to Cloned Object
            if ("Yes".equalsIgnoreCase(strIssueLinkReportedAgainstItem)) {
                connectReportedAgainstItemsToCloneIssue(context, domOriginalIssue, oidsArray);
            }
            // Associate Reference Document to Cloned Object
            if ("No".equalsIgnoreCase(strIssueLinkReferenceDoc)) {
                for (String strCloneId : oidsArray) {
                    disConnectDocument(context, strCloneId);
                }
            }
            // Associate Assignee from Original Issue To Clone Issue Object
            if ("Yes".equalsIgnoreCase(strIssueLinkAssignee)) {
                linkAssigneeToCloneIssue(context, domOriginalIssue, oidsArray);
            }
            // Associate Clone Issue To Original Issue Object
            if ("Yes".equalsIgnoreCase(strIssueLinkToOriginal)) {
                linkOriginalIssueToCloneIssue(context, domOriginalIssue, oidsArray);
            }
        } catch (Exception ex) {
            logger.error("Error in cloneAndConnectIssue in JPO pss.issue.IssueManagement : ", ex);
            ex.printStackTrace();
            throw ex;
        }
        logger.debug("pss.issue.IssueManagement:cloneAndConnectIssue:END");
        return oidsArray;

    }

    /****
     * This method is used to connect the Original Issue to Clone Issue Object
     * @param context
     * @param domOriginalIssue
     * @param args
     * @return void
     * @throws Exception
     */
    public void linkOriginalIssueToCloneIssue(Context context, DomainObject domOriginalIssue, String[] arCloneIds) throws Exception {
        logger.debug("pss.issue.IssueManagement:linkOriginalIssueToCloneIssue:START");
        boolean isContextPushed = false;
        try {
            ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            isContextPushed = true;
            DomainRelationship.connect(context, domOriginalIssue, TigerConstants.RELATIONSHIP_PSS_CLONEDFROMISSUE, true, arCloneIds); // New Relationship used to connect Cloned Issue To Original Issue
        } catch (Exception ex) {
            logger.error("Error in linkOriginalIssueToCloneIssue in JPO pss.issue.IssueManagement : ", ex);
            throw ex;
        } finally {
            if (isContextPushed) {
                ContextUtil.popContext(context);
                isContextPushed = false;
            }
        }
        logger.debug("pss.issue.IssueManagement:linkOriginalIssueToCloneIssue:END");
    }

    /****
     * This method is used to connect the Assignee from Original Issue to Clone Issue Object.
     * @param context
     * @param domOriginalIssue
     * @param args
     * @return void
     * @throws Exception
     */
    public void linkAssigneeToCloneIssue(Context context, DomainObject domOriginalIssue, String[] arCloneIds) throws Exception {
        logger.debug("pss.issue.IssueManagement:linkAssigneeToCloneIssue:START");
        try {
            StringList slAssigneeList = domOriginalIssue.getInfoList(context, "to[" + TigerConstants.RELATIONSHIP_ASSIGNEDISSUE + "].from.id");
            if (!slAssigneeList.isEmpty()) {
                for (String strCloneId : arCloneIds) {
                    DomainObject domCloneIssue = DomainObject.newInstance(context, strCloneId);
                    DomainRelationship.connect(context, domCloneIssue, TigerConstants.RELATIONSHIP_ASSIGNEDISSUE, false, (String[]) slAssigneeList.toArray(new String[slAssigneeList.size()]));
                }
            }
        } catch (Exception ex) {
            logger.error("Error in linkAssigneeToCloneIssue in JPO pss.issue.IssueManagement : ", ex);
            throw ex;
        }
        logger.debug("pss.issue.IssueManagement:linkAssigneeToCloneIssue:END");
    }

    /****
     * This method is used to connect the Reference Documents from Original Issue to Clone Issue Object
     * @param context
     * @param domOriginalIssue
     * @param args
     * @return void
     * @throws Exception
     */
    public void cloneAndConnectReferenceDocumentsToIssue(Context context, DomainObject domOriginalIssue, String strCloneId) throws Exception {
        logger.debug("pss.issue.IssueManagement:cloneAndConnectReferenceDocumentsToIssue:START");
        try {
            // TIGTK-16781 : 20-08-2018 : START
            disConnectDocument(context, strCloneId);
            // TIGTK-16781 : 20-08-2018 : START
            StringList slReferenceDocsList = domOriginalIssue.getInfoList(context, "from[" + DomainConstants.RELATIONSHIP_REFERENCE_DOCUMENT + "].to.id");
            // Iterate over the Reference Document and generate the Clone Reference Document
            Iterator itrReferenceDoc = slReferenceDocsList.iterator();
            while (itrReferenceDoc.hasNext()) {
                String strAutoNumberSeries = DomainConstants.EMPTY_STRING;
                String strReferenceDocId = (String) itrReferenceDoc.next();
                DomainObject domDocument = DomainObject.newInstance(context, strReferenceDocId);
                String strAutoName = DomainObject.getAutoGeneratedName(context, "type_Document", strAutoNumberSeries);
                DomainObject domCloneDocument = new DomainObject(domDocument.cloneObject(context, strAutoName));
                // Connect Clone Reference Document to each Clone Issue Object
                // TIGTK-16781 : 20-08-2018 : START
                DomainObject domCloneIssue = DomainObject.newInstance(context, strCloneId);
                // TIGTK-16781 : 20-08-2018 : END
                DomainRelationship.connect(context, domCloneIssue, DomainConstants.RELATIONSHIP_REFERENCE_DOCUMENT, domCloneDocument);

            }
        } catch (Exception ex) {
            logger.error("Error in cloneAndConnectReferenceDocumentsToIssue in JPO pss.issue.IssueManagement : ", ex);
            throw ex;
        }
        logger.debug("pss.issue.IssueManagement:cloneAndConnectReferenceDocumentsToIssue:END");
    }

    /****
     * This method is used to connect the Reported Against Items from Original Issue to Clone Issue Object.
     * @param context
     * @param domOriginalIssue
     * @param args
     * @return void
     * @throws Exception
     */
    public void connectReportedAgainstItemsToCloneIssue(Context context, DomainObject domOriginalIssue, String[] arCloneIds) throws Exception {
        logger.debug("pss.issue.IssueManagement:connectReportedAgainstItemsToCloneIssue:START");
        try {
            StringList slReportedAgainstItemsList = domOriginalIssue.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_ISSUE + "].to.id");
            if (!slReportedAgainstItemsList.isEmpty()) {
                for (String strCloneId : arCloneIds) {
                    DomainObject domCloneIssue = DomainObject.newInstance(context, strCloneId);
                    DomainRelationship.connect(context, domCloneIssue, TigerConstants.RELATIONSHIP_ISSUE, true,
                            (String[]) slReportedAgainstItemsList.toArray(new String[slReportedAgainstItemsList.size()]));
                }
            }
        } catch (Exception ex) {
            logger.error("Error in connectReportedAgainstItemsToCloneIssue in JPO pss.issue.IssueManagement : ", ex);
            throw ex;
        }
        logger.debug("pss.issue.IssueManagement:connectReportedAgainstItemsToCloneIssue:END");
    }

    /****
     * This method is used to disconnect the Reference Documents from Original CR to Clone CR Object
     * @param context
     * @param arCloneIds
     * @return void
     * @throws Exception
     */
    public void disConnectDocument(Context context, String strCloneId) throws Exception {
        logger.debug("disConnectDocument: START ");
        try {
            BusinessObject domCloneIssue = new BusinessObject(strCloneId);
            RelationshipList relationList = domCloneIssue.getAllRelationship(context);
            Iterator it = relationList.iterator();
            while (it.hasNext()) {
                Relationship relationship = (Relationship) it.next();
                if (UIUtil.isNotNullAndNotEmpty(relationship.getTypeName()) && relationship.getTypeName().equalsIgnoreCase(DomainConstants.RELATIONSHIP_REFERENCE_DOCUMENT)) {
                    domCloneIssue.disconnect(context, relationship);

                }
            }
        } catch (Exception ex) {
            logger.error("disConnectDocument : ", ex);
        }
        logger.debug("disConnectDocument: END ");
    }

    /****
     * This method is used to get List of CLoned Issue to display in table.
     * @param context
     * @param args
     * @return MapList
     * @throws Exception
     */
    public MapList getCloneList(Context context, String[] args) throws Exception {
        logger.debug("pss.issue.IssueManagement:getCloneList:START");
        MapList returnList = new MapList();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap requestValuesMap = (HashMap) programMap.get("RequestValuesMap");
            String[] cloneIssueIds = (String[]) requestValuesMap.get("objIds");
            StringList slIds = FrameworkUtil.split(cloneIssueIds[0].toString(), "-");
            for (int i = 0; i < slIds.size(); i++) {
                HashMap ObjectMap = new HashMap();
                ObjectMap.put(DomainConstants.SELECT_ID, slIds.get(i).toString());
                returnList.add(ObjectMap);
            }
        } catch (Exception ex) {
            logger.error("Error in getCloneList in JPO pss.issue.IssueManagement : ", ex);
            ex.printStackTrace();
            throw ex;
        }
        logger.debug("pss.issue.IssueManagement:getCloneList:END");
        return returnList;
    }

    /**
     * Description : This method is used to include Eligible CR.In search result only show the CRs having CR initiator (CR Originator) as logged in person.PCM : TIGTK-10702 : AB : 27/10/2017
     * @param context
     * @param args
     * @return StringList
     * @throws Exception
     */

    public StringList includeEligibleCR(Context context, String[] args) throws Exception {
        StringList slReturnCRList = new StringList();
        try {
            StringList objectSelect = new StringList(DomainConstants.SELECT_ID);

            // Create where clause for get the Change Request

            String sbObjectWhere = " attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]==\"" + context.getUser() + "\"";

            // Get All Change Request for which Originator is context user
            MapList resultsList = DomainObject.findObjects(context, // eMatrix context
                    TigerConstants.TYPE_PSS_CHANGEREQUEST, // type pattern
                    DomainConstants.QUERY_WILDCARD, // name pattern
                    DomainConstants.QUERY_WILDCARD, // revision pattern
                    DomainConstants.QUERY_WILDCARD, // owner pattern
                    TigerConstants.VAULT_ESERVICEPRODUCTION, // vault pattern
                    sbObjectWhere, // where expression
                    true, // Expand Type
                    objectSelect); // object selects

            if (!resultsList.isEmpty()) {
                int iSize = resultsList.size();
                for (int i = 0; i < iSize; i++) {
                    HashMap<?, ?> mapProgramProjectObject = (HashMap<?, ?>) resultsList.get(i);
                    slReturnCRList.add(mapProgramProjectObject.get(DomainConstants.SELECT_ID));
                }
            } else {
                slReturnCRList.add("");
            }
        } catch (Exception e) {
            logger.error("Error in includeEligibleCR : ", e);
            e.printStackTrace();
            throw e;
        }
        return slReturnCRList;
    }

    /**
     * To Include Person connected to Program-Project of issue in add existing functionality of Issue
     * @param context
     * @param args
     * @return StringList
     * @throws Exception
     *             if fails
     */
    @SuppressWarnings("rawtypes")
    public StringList includeMembersOfProgProj(Context context, String[] args) throws Exception {

        try {
            StringList slProgramProjectIncludeOIDList = new StringList();
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            String strIssueId = (String) paramMap.get("objectId");
            if (UIUtil.isNotNullAndNotEmpty(strIssueId)) {
                DomainObject domIssueObject = DomainObject.newInstance(context, strIssueId);
                String strProgramProjectId = domIssueObject.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
                DomainObject domProgramProjectObject = DomainObject.newInstance(context, strProgramProjectId);
                slProgramProjectIncludeOIDList = domProgramProjectObject.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS + "].to.id");
            }
            return slProgramProjectIncludeOIDList;
        } catch (Exception e) {
            logger.error("Error in includeMembersOfProgProj : ", e);
            throw e;
        }

    }

    /**
     * Method to check whether the person is owner or co-owner or Assignee of issueand showing the command.
     * @param context
     *            - the eMatrix <code>Context</code> object
     * @param args
     *            - args contains a Map with the following entries objectId - The object Id of Context object
     * @return - boolean (true or false)
     * @throws Exception
     *             if the operation fails
     * @since 15x.HF15
     */
    public boolean removeAssigneeCommandDisplay(Context context, String[] args) throws Exception {
        boolean hasAccess = false;
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strIssueobjectId = (String) programMap.get("objectId");
            StringList slAssignee = new StringList();
            StringList slSelects = new StringList(4);
            slSelects.add("attribute[" + DomainConstants.ATTRIBUTE_CO_OWNER + "]");
            slSelects.add("attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");
            slSelects.add(DomainConstants.SELECT_CURRENT);
            slSelects.add("to[" + TigerConstants.RELATIONSHIP_ASSIGNEDISSUE + "].from.name");
            BusinessObject boIssue = new BusinessObject(strIssueobjectId);
            BusinessObjectWithSelect bowsIssue = boIssue.select(context, slSelects);
            // START :: TIGTK-18267 :: ALM-6416
            String strCurrentState = bowsIssue.getSelectData(DomainConstants.SELECT_CURRENT);
            String strCoOwner = bowsIssue.getSelectData("attribute[" + DomainConstants.ATTRIBUTE_CO_OWNER + "]");

            if (TigerConstants.STATE_PSS_ISSUE_CREATE.equals(strCurrentState)) {
                String sIssueOriginator = bowsIssue.getSelectData("attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");
                slAssignee.add(sIssueOriginator);
                slAssignee.add(strCoOwner);
            }
            if (TigerConstants.STATE_PSS_ISSUE_ASSIGN.equals(strCurrentState) || TigerConstants.STATE_PSS_ISSUE_ACTIVE.equals(strCurrentState)) {
                StringList slIssueAssignee = bowsIssue.getSelectDataList("to[" + TigerConstants.RELATIONSHIP_ASSIGNEDISSUE + "].from.name");
                if (slIssueAssignee != null && !slIssueAssignee.isEmpty())
                    slAssignee.addAll(slIssueAssignee);
                slAssignee.add(strCoOwner);
            }
            // END :: TIGTK-18267 :: ALM-6416
            String sContextUser = context.getUser();

            if (slAssignee.contains(sContextUser)) {
                hasAccess = true;
            }
        } catch (Exception ex) {
            logger.error("Error in removeAssigneeCommandDisplay : ", ex);
            throw ex;
        }
        return hasAccess;
    }

    /**
     * Description : This method is used to display Add Existing and Remove command for Issue Reference Documents category
     * @param context
     * @param args
     * @return boolean
     * @throws Exception
     * @author psalunke : TIGTK-13528
     * @since : 05-03-2018
     */
    public boolean showIssueReferenceDocumentsRemoveCommand(Context context, String[] args) throws Exception {
        logger.debug("pss.issue.IssueManagement : showIssueReferenceDocumentsRemoveCommand : START");
        boolean hasAccess = false;
        try {
            Map<?, ?> programMap = (Map<?, ?>) JPO.unpackArgs(args);
            String strIssueobjectId = (String) programMap.get("objectId");
            StringList slAssignee = new StringList();
            DomainObject.MULTI_VALUE_LIST.add("to[" + TigerConstants.RELATIONSHIP_ASSIGNEDISSUE + "]." + DomainConstants.SELECT_FROM_NAME);
            StringList slSelects = new StringList();
            slSelects.add("attribute[" + DomainConstants.ATTRIBUTE_CO_OWNER + "]");
            slSelects.add("attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");
            slSelects.add(DomainConstants.SELECT_CURRENT);
            slSelects.add("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "]." + DomainConstants.SELECT_FROM_ID);
            slSelects.add("to[" + TigerConstants.RELATIONSHIP_ASSIGNEDISSUE + "]." + DomainConstants.SELECT_FROM_NAME);

            DomainObject domIssue = DomainObject.newInstance(context, strIssueobjectId);
            Map<?, ?> mpIssueInfo = domIssue.getInfo(context, slSelects);
            String strCoOwner = (String) mpIssueInfo.get("attribute[" + DomainConstants.ATTRIBUTE_CO_OWNER + "]");
            String strCurrentState = (String) mpIssueInfo.get(DomainConstants.SELECT_CURRENT);

            slAssignee.add(strCoOwner);

            if (strCurrentState.equalsIgnoreCase(TigerConstants.STATE_PSS_ISSUE_CREATE)) {
                String sIssueOriginator = (String) mpIssueInfo.get("attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");
                slAssignee.add(sIssueOriginator);
            } else if (strCurrentState.equalsIgnoreCase(TigerConstants.STATE_PSS_ISSUE_ASSIGN) || strCurrentState.equalsIgnoreCase(TigerConstants.STATE_PSS_ISSUE_ACTIVE)) {
                StringList slAssigneeList = (StringList) mpIssueInfo.get("to[" + TigerConstants.RELATIONSHIP_ASSIGNEDISSUE + "]." + DomainConstants.SELECT_FROM_NAME);
                if (slAssigneeList != null && !slAssigneeList.isEmpty()) {
                    slAssignee.addAll(slAssigneeList);
                }
            } else if (strCurrentState.equalsIgnoreCase(TigerConstants.STATE_PSS_ISSUE_REVIEW) || strCurrentState.equalsIgnoreCase(TigerConstants.STATE_PSS_ISSUE_ACCEPTED)) {
                HashMap<String, String> mProgramMap = new HashMap<String, String>();
                mProgramMap.put("id", strIssueobjectId);
                mProgramMap.put("calledFrom", "showIssueReferenceDocumentsCommands");
                String[] strArgs = JPO.packArgs(mProgramMap);
                pss.ecm.notification.IssueNotificationUtil_mxJPO IssueNotificationUtilObj = new pss.ecm.notification.IssueNotificationUtil_mxJPO(context, null);
                StringList slRouteAssigneeList = (StringList) IssueNotificationUtilObj.getIssueRouteAssignee(context, strArgs);
                slAssignee.addAll(slRouteAssigneeList);
            }

            // Get Context User
            String sContextUser = context.getUser();

            if (slAssignee.contains(sContextUser)) {
                hasAccess = true;
            }
            DomainObject.MULTI_VALUE_LIST.remove("to[" + TigerConstants.RELATIONSHIP_ASSIGNEDISSUE + "]." + DomainConstants.SELECT_FROM_NAME);
            logger.debug("pss.issue.IssueManagement : showIssueReferenceDocumentsRemoveCommand : END");
        } catch (Exception ex) {
            logger.error("pss.issue.IssueManagement : showIssueReferenceDocumentsRemoveCommand : ERROR ", ex);
            throw ex;
        }
        return hasAccess;
    }

}
