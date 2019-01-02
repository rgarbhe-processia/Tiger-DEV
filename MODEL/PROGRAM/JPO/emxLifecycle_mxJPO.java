
/*
 * emxLifecycleMassApproval.java
 *
 * (c) Dassault Systemes, 1993 - 2015. All rights reserved. This program contains proprietary and trade secret information of ENOVIA MatrixOne, Inc. Copyright notice is precautionary only and does not
 * evidence any actual or intended publication of such program.
 *
 */
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import com.matrixone.apps.common.Organization;
import com.matrixone.apps.common.util.ComponentsUtil;
import com.matrixone.apps.domain.DomainAccess;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.XSSUtil;
import com.matrixone.apps.domain.util.eMatrixDateFormat;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;
import pss.constants.TigerConstants;

/**
 * This JPO includes the code related to the Lifecycle Mass Approval functionality
 */
public class emxLifecycle_mxJPO extends emxLifecycleBase_mxJPO {

    private static final String SELECT_ATTRIBUTE_SCHEDULED_COMPLETION_DATE = "attribute[" + DomainObject.ATTRIBUTE_SCHEDULED_COMPLETION_DATE + "]";

    private static final String SELECT_ATTRIBUTE_ROUTE_TASK_USER = "attribute[" + DomainObject.ATTRIBUTE_ROUTE_TASK_USER + "]";

    private static final String SELECT_ATTRIBUTE_ROUTE_SEQUENCE = "attribute[" + DomainObject.ATTRIBUTE_ROUTE_SEQUENCE + "]";

    private static final String INFO_TYPE_ACTIVATED_TASK = "activatedTask";

    private static final String INFO_TYPE_DEFINED_TASK = "definedTask";

    private static final String INFO_TYPE_SIGNATURE = "signature";

    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(emxLifecycle_mxJPO.class);

    /**
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @throws Exception
     *             if the operation fails
     * @grade 0
     */
    public emxLifecycle_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
    }

    /**
     * Gets Approval Status For Task Signatures Table
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            Packed arguments having table data
     * @return The vector containing values for this column
     * @throws Exception
     */
    public Vector getApprovalStatusForTaskSignatures(Context context, String[] args) throws Exception {
        try {

            final String POLICY_INBOX_TASK_STATE_COMPLETE = PropertyUtil.getSchemaProperty(context, "Policy", DomainObject.POLICY_INBOX_TASK, "state_Complete");
            final String POLICY_INBOX_TASK_STATE_REVIEW = PropertyUtil.getSchemaProperty(context, "Policy", DomainObject.POLICY_INBOX_TASK, "state_Review");

            // Create result vector
            Vector vecResult = new Vector();

            // Get object list information from packed arguments
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");
            Map paramList = (Map) programMap.get("paramList");

            // Begin : Bug 346997 code modification
            boolean isExporting = (paramList.get("reportFormat") != null);
            // End : Bug 346997 code modification

            String jsTreeID = (String) paramList.get("jsTreeID");
            Map mapObjectInfo = null;
            String strInfoType = null;
            String strCurrentState = null;
            String strRouteTaskUser = null;
            String strMQL = null;
            String strContextUser = context.getUser();
            String strContextUserId = PersonUtil.getPersonObjectID(context);
            String strAssigneeId = null;
            String strTaskId = null;
            String strLanguage = (String) paramList.get("languageStr");
            String strRoleOrGroupName = null;
            String strCurrentRouteStatus = null;
            String strTaskOrder = null;
            String strRouteNodeId = null;
            String strParentObjectId = null;
            String strParentObjectState = null;

            MapList mlRelInfo = null;
            Map mapRelInfo = null;
            String strApprovalStatus = null;

            // Find the status strings to be shown
            final String RESOURCE_BUNDLE = "emxFrameworkStringResource";
            i18nNow loc = new i18nNow();
            final String STRING_COMPLETED = (String) loc.GetString(RESOURCE_BUNDLE, strLanguage, "emxFramework.LifecycleTasks.Completed");
            final String STRING_APPROVED = (String) loc.GetString(RESOURCE_BUNDLE, strLanguage, "emxFramework.Lifecycle.Approved");
            final String STRING_REJECTED = (String) loc.GetString(RESOURCE_BUNDLE, strLanguage, "emxFramework.Lifecycle.Rejected");
            final String STRING_ABSTAINED = (String) loc.GetString(RESOURCE_BUNDLE, strLanguage, "emxFramework.Lifecycle.Abstained");
            final String STRING_NEEDS_REVIEW = (String) loc.GetString(RESOURCE_BUNDLE, strLanguage, "emxFramework.Lifecycle.NeedsReview");
            final String STRING_IGNORED = (String) loc.GetString(RESOURCE_BUNDLE, strLanguage, "emxFramework.Lifecycle.Ignored");
            final String STRING_PENDING = (String) loc.GetString(RESOURCE_BUNDLE, strLanguage, "emxFramework.LifecycleTasks.Pending");
            final String STRING_PENDING_ORDER = (String) loc.GetString(RESOURCE_BUNDLE, strLanguage, "emxFramework.LifecycleTasks.PendingOrder");
            // final String STRING_APPROVE = (String)loc.GetString(RESOURCE_BUNDLE, strLanguage, "emxFramework.Lifecycle.Approve");
            final String STRING_ACCEPT = (String) loc.GetString(RESOURCE_BUNDLE, strLanguage, "emxFramework.LifecycleTasks.Accept");
            final String STRING_AWAITING_APPROVAL = (String) loc.GetString(RESOURCE_BUNDLE, strLanguage, "emxFramework.LifecycleTasks.AwaitingApproval");
            final String STRING_ROUTE_STOPPED = (String) loc.GetString(RESOURCE_BUNDLE, strLanguage, "emxFramework.LifecycleTasks.RouteStopped");

            // Form the Accept link template
            String sAcceptURL = "'../components/emxRouteAcceptTask.jsp?taskId=${OBJECT_ID}'";
            StringBuffer strAcceptLink = new StringBuffer(64);
            strAcceptLink.append("<a href=\"javascript:submitWithCSRF(" + sAcceptURL + ", findFrame(getTopWindow(),'listHidden'));\">");
            strAcceptLink.append(STRING_ACCEPT);
            strAcceptLink.append("</a>");

            //
            // Form the Approve link template

            StringBuffer strTaskApproveLink = new StringBuffer(64);
            String sTaskApproveURL = "'../common/emxLifecycleApproveRejectPreProcess.jsp?emxTableRowId=${OBJECT_ID}^${STATE}^^${TASK_ID}&objectId=${OBJECT_ID}&suiteKey=Framework&jsTreeId="
                    + XSSUtil.encodeForURL(context, jsTreeID) + " '";
            strTaskApproveLink.append("<a href=\"javascript:submitWithCSRF(" + sTaskApproveURL + ", findFrame(getTopWindow(),'listHidden'));\">");
            strTaskApproveLink.append("<img border='0' src='../common/images/iconActionApprove.png' />");
            strTaskApproveLink.append(STRING_AWAITING_APPROVAL);
            strTaskApproveLink.append("</a>");

            // Do for each object
            for (Iterator itrObjects = objectList.iterator(); itrObjects.hasNext();) {
                mapObjectInfo = (Map) itrObjects.next();
                strInfoType = (String) mapObjectInfo.get("infoType");
                strCurrentRouteStatus = (String) mapObjectInfo.get("routeStatus");
                strRouteNodeId = (String) mapObjectInfo.get("routeNodeId");
                strParentObjectId = (String) mapObjectInfo.get("parentObjectId");
                strParentObjectState = (String) mapObjectInfo.get("parentObjectState");
                strApprovalStatus = (String) mapObjectInfo.get("approvalStatus");

                // The Approval Status reflect Completed for completed tasks;
                // Approved, Rejected or Ignored (if applicable) for tasks/signatures.
                // For unsigned tasks or signatures, the status will reflect "Pending" till the task is completed.
                // If the user has privilege to approve, then the status will reflect Approve
                // and hyperlinked will allow the user to approve.
                // The approve action will be enabled for both signatures and route tasks.
                if (INFO_TYPE_SIGNATURE.equals(strInfoType)) {
                    // If signature is completed then show accordingly else show the links for operations
                    if ("true".equalsIgnoreCase((String) mapObjectInfo.get("signed"))) {
                        if ("true".equalsIgnoreCase((String) mapObjectInfo.get("approved"))) {
                            vecResult.add(STRING_APPROVED);
                        } else if ("true".equalsIgnoreCase((String) mapObjectInfo.get("rejected"))) {
                            vecResult.add(STRING_REJECTED);
                        } else if ("true".equalsIgnoreCase((String) mapObjectInfo.get("ignored"))) {
                            vecResult.add(STRING_IGNORED);
                        }
                    } else {
                        vecResult.add((String) mapObjectInfo.get("approvalStatus"));
                    }
                } else if (INFO_TYPE_ACTIVATED_TASK.equals(strInfoType)) {
                    // Show Completed for active task else show status depending on some things
                    strCurrentState = (String) mapObjectInfo.get("currentState");
                    if (POLICY_INBOX_TASK_STATE_COMPLETE.equals(strCurrentState)) {
                        if ("Approve".equals(strApprovalStatus)) {
                            vecResult.add(STRING_APPROVED);
                        } else if ("Reject".equals(strApprovalStatus)) {
                            vecResult.add(STRING_REJECTED);
                        } else if ("Abstain".equals(strApprovalStatus)) {
                            vecResult.add(STRING_ABSTAINED);
                        } else {
                            // Show status as completed
                            vecResult.add(STRING_COMPLETED);
                        }
                    }
                    // START BUG 346838
                    else if (POLICY_INBOX_TASK_STATE_REVIEW.equals(strCurrentState)) {
                        vecResult.add(STRING_NEEDS_REVIEW);
                        // END BUG 346838
                    } else {
                        // Show "Route Stopped" for Stopped Route.
                        if ("Stopped".equals(strCurrentRouteStatus)) {
                            vecResult.add(STRING_ROUTE_STOPPED);
                        } else {
                            // If the task is not completed yet,
                            // then see if it is assigned to this user then show the approve link,
                            // otherwise show the pending link
                            strAssigneeId = (String) mapObjectInfo.get("assigneeId");
                            strTaskId = (String) mapObjectInfo.get("taskId");

                            if (strContextUserId != null && strContextUserId.equals(strAssigneeId)) {

                                // Begin : Bug 346997 code modification
                                if (isExporting) {
                                    vecResult.add(STRING_AWAITING_APPROVAL);
                                } else {
                                    String strFormattedLink = FrameworkUtil.findAndReplace(strTaskApproveLink.toString(), "${TASK_ID}", strTaskId);
                                    strFormattedLink = FrameworkUtil.findAndReplace(strFormattedLink, "${OBJECT_ID}", strParentObjectId);
                                    strFormattedLink = FrameworkUtil.findAndReplace(strFormattedLink, "${STATE}", strParentObjectState);

                                    vecResult.add(strFormattedLink);
                                }
                                // End : Bug 346997 code modification
                            } else {
                                strRouteTaskUser = (String) mapObjectInfo.get("routeTaskUser");
                                if (strRouteTaskUser == null || "".equals(strRouteTaskUser)) {
                                    // If Route Task User value is not available then just show pending
                                    vecResult.add(STRING_AWAITING_APPROVAL);
                                } else if (strRouteTaskUser.startsWith("role_") || strRouteTaskUser.startsWith("group_")) {
                                    // Check if the logged in user belongs to the group to which the
                                    // route task is assigned
                                    // tigtk-13028 :START
                                    boolean isToBeAccepted = false;
                                    DomainObject domChangeObject = DomainObject.newInstance(context, strParentObjectId);
                                    String strChangeObjectType = domChangeObject.getInfo(context, DomainConstants.SELECT_TYPE);
                                    strRoleOrGroupName = PropertyUtil.getSchemaProperty(context, strRouteTaskUser);

                                    String strSelectProgramProjectId = DomainConstants.EMPTY_STRING;
                                    int flag = 0;
                                    if (TigerConstants.TYPE_PSS_CHANGEREQUEST.equalsIgnoreCase(strChangeObjectType)) {
                                        strSelectProgramProjectId = new StringBuilder("to[").append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA).append("].from.id").toString();
                                        flag = 1;
                                    } else if (TigerConstants.TYPE_PSS_CHANGENOTICE.equalsIgnoreCase(strChangeObjectType)) {
                                        strSelectProgramProjectId = new StringBuilder("to[").append(TigerConstants.RELATIONSHIP_PSS_RELATEDCN).append("].from.to[")
                                                .append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA).append("].from.id").toString();
                                        flag = 1;
                                    } else if (TigerConstants.TYPE_CHANGEACTION.equalsIgnoreCase(strChangeObjectType)) {
                                        strSelectProgramProjectId = new StringBuilder("to[").append(TigerConstants.RELATIONSHIP_CHANGEACTION).append("].from.to[")
                                                .append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA).append("].from.id").toString();
                                        flag = 1;
                                    } else if (TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION.equalsIgnoreCase(strChangeObjectType)) {
                                        strSelectProgramProjectId = new StringBuilder("to[").append(TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION).append("].from.to[")
                                                .append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA).append("].from.id").toString();
                                        flag = 1;
                                    }
                                    String strSecurityContext = PersonUtil.getDefaultSecurityContext(context, strContextUser);
                                    String strRoleAssigned = (strSecurityContext.split("[.]")[0]);

                                    if (flag == 1) {
                                        String strProgramProjectID = (String) domChangeObject.getInfo(context, strSelectProgramProjectId);
                                        StringList slProjectTeamMembers = new StringList();

                                        if (UIUtil.isNotNullAndNotEmpty(strProgramProjectID) && UIUtil.isNotNullAndNotEmpty(strRoleOrGroupName)) {
                                            PSS_emxInboxTaskNotification_mxJPO completeTaskBase = new PSS_emxInboxTaskNotification_mxJPO(context, null);
                                            slProjectTeamMembers = completeTaskBase.getProgramProjectMembers(context, DomainConstants.EMPTY_STRING, strRoleOrGroupName, strProgramProjectID);
                                        }
                                        if (slProjectTeamMembers.contains(context.getUser()) && strRoleAssigned.equalsIgnoreCase(strRoleOrGroupName)) {
                                            isToBeAccepted = true;
                                        }
                                    } else {
                                        strMQL = "print person  '" + strContextUser + "' select isassigned[" + strRoleOrGroupName + "] dump";
                                        isToBeAccepted = "true".equalsIgnoreCase(MqlUtil.mqlCommand(context, strMQL, true));
                                    }
                                    // TIGTK-13028 :END

                                    // IR-043921V6R2011 - Changes START
                                    boolean isOrgMember = true;
                                    String sRouteId = (String) mapObjectInfo.get("routeId");
                                    MapList mlOwningOrg = DomainObject.getInfo(context, new String[] { sRouteId }, new StringList(SELECT_OWNING_ORG_ID));
                                    String sOwningOrgId = (String) ((Map) mlOwningOrg.get(0)).get(SELECT_OWNING_ORG_ID);
                                    if (sOwningOrgId != null && !"null".equals(sOwningOrgId) && !"".equals(sOwningOrgId)) {
                                        Organization org = (Organization) DomainObject.newInstance(context, sOwningOrgId);
                                        StringList busSelects = new StringList(2);
                                        busSelects.addElement(DomainConstants.SELECT_ID);
                                        busSelects.addElement(DomainConstants.SELECT_NAME);
                                        String sWhereClause = "( name == \"" + strContextUser + "\" )";
                                        MapList mlMembers = org.getMemberPersons(context, busSelects, sWhereClause, null);
                                        if (!(mlMembers.size() > 0)) {
                                            isOrgMember = false;
                                        }
                                    }

                                    // PCM : TIGTK-4560 : 28/06/2017 : AB : START
                                    // This method is used to show Approval status in "Approval Status" column
                                    // Check whether context user is delegated person or not. If it is delegated user for current task then show "Accept Task" command to User.
                                    if (!isToBeAccepted) {
                                        boolean isAbsenceDelegate = isAbsenceDelegate(context, strContextUser, strTaskId);
                                        isToBeAccepted = isAbsenceDelegate;
                                    }

                                    // PCM : TIGTK-4560 : 28/06/2017 : AB : END

                                    if (isToBeAccepted && strRoleOrGroupName.equals((String) mapObjectInfo.get("owner")) && isOrgMember) {
                                        // IR-043921V6R2011 - Changes END
                                        // Begin : Bug 346997 code modification
                                        if (isExporting) {
                                            vecResult.add(STRING_ACCEPT);
                                        } else {
                                            vecResult.add(FrameworkUtil.findAndReplace(strAcceptLink.toString(), "${OBJECT_ID}", strTaskId));
                                        }
                                        // End : Bug 346997 code modification
                                    } else {
                                        vecResult.add(STRING_AWAITING_APPROVAL);
                                    }
                                } else {
                                    // If Route Task User value is not role or group then just show pending
                                    vecResult.add(STRING_AWAITING_APPROVAL);
                                }
                            }
                        }
                    }
                } else if (INFO_TYPE_DEFINED_TASK.equals(strInfoType)) {
                    //
                    // If the route for this task is not active then show "Pending" else show "Pending Order <n>"
                    //
                    if ("Started".equals(strCurrentRouteStatus)) {
                        mlRelInfo = DomainRelationship.getInfo(context, new String[] { strRouteNodeId }, new StringList(SELECT_ATTRIBUTE_ROUTE_SEQUENCE));
                        mapRelInfo = (Map) mlRelInfo.get(0);

                        if (mapRelInfo != null) {
                            strTaskOrder = (String) mapRelInfo.get(SELECT_ATTRIBUTE_ROUTE_SEQUENCE);
                        }
                        if (strTaskOrder == null) {
                            strTaskOrder = "";
                        }
                        vecResult.add(STRING_PENDING_ORDER + " " + XSSUtil.encodeForHTML(context, strTaskOrder));
                    } else {
                        vecResult.add(STRING_PENDING);
                    }
                } else {
                    throw new Exception(ComponentsUtil.i18nStringNow("emxComponents.LifeCycle.InvalidTypeOfTaskObject", context.getLocale().getLanguage()));
                }
            }

            return vecResult;
        } catch (Exception exp) {
            logger.error("Error in getApprovalStatusForTaskSignatures: ", exp);
            throw exp;
        }
    }

    /**
     * This method is used to decide whether context user is delegated user for given task or not. If context user delegated by other user for given task this method will return true else it will
     * return false. PCM : TIGTK-4560 : 28/06/2017 : AB
     * @param context
     * @param strContextUser
     * @param strTaskID
     * @return
     * @throws Exception
     */
    public boolean isAbsenceDelegate(Context context, String strContextUser, String strTaskID) throws Exception {
        boolean isDelegate = false;
        try {
            // Get the Absent User vs Delegated user map
            Map<String, String> mpUserVsDelegatedUser = getUserVsDelegateUserMap(context, strTaskID);
            for (Entry<String, String> entryUserVsDelegatedUser : mpUserVsDelegatedUser.entrySet()) {
                if (strContextUser.equals(entryUserVsDelegatedUser.getValue())) {
                    isDelegate = true;
                }
            }
            return isDelegate;

        } catch (Exception Ex) {
            logger.error("Error in isAbsenceDelegate: ", Ex);
            throw Ex;
        }
    }

    /**
     * This method is used get Absent User vs Delegated user map.PCM : TIGTK-4560 : 28/06/2017 : AB
     * @param context
     * @param strTaskID
     * @return
     * @throws Exception
     */
    public Map getUserVsDelegateUserMap(Context context, String strTaskID) throws Exception {
        Map mpUserVsDelegatedUser = new HashMap<String, String>();
        try {
            String ATTRIBUTE_ABSENCE_START_DATE = PropertyUtil.getSchemaProperty(context, "attribute_AbsenceStartDate");
            String ATTRIBUTE_ABSENCE_END_DATE = PropertyUtil.getSchemaProperty(context, "attribute_AbsenceEndDate");
            String ATTRIBUTE_ABSENCE_DELEGATE = PropertyUtil.getSchemaProperty(context, "attribute_AbsenceDelegate");

            DomainObject domInboxTask = new DomainObject(strTaskID);
            StringList slTaskSelects = new StringList();
            slTaskSelects.add("attribute[" + DomainConstants.ATTRIBUTE_ALLOW_DELEGATION + "]");
            slTaskSelects.add(SELECT_ATTRIBUTE_ROUTE_TASK_USER);
            slTaskSelects.add(SELECT_ATTRIBUTE_SCHEDULED_COMPLETION_DATE);
            slTaskSelects.add(DomainAccess.SELECT_PROJECT);
            slTaskSelects.add(DomainConstants.SELECT_ORGANIZATION);

            // Get the InboxTask's Info
            Map mpTaskInfo = domInboxTask.getInfo(context, slTaskSelects);
            String strAllowDelegation = (String) mpTaskInfo.get("attribute[" + DomainConstants.ATTRIBUTE_ALLOW_DELEGATION + "]");
            String strRouteTaskUser = (String) mpTaskInfo.get(SELECT_ATTRIBUTE_ROUTE_TASK_USER);
            String sTaskScheduledCompletionDate = (String) mpTaskInfo.get(SELECT_ATTRIBUTE_SCHEDULED_COMPLETION_DATE);

            // If Allow Delegation is true and Role based task then Proceed further
            if ("TRUE".equalsIgnoreCase(strAllowDelegation) && strRouteTaskUser.startsWith("role_")) {
                String strRoleName = PropertyUtil.getSchemaProperty(context, strRouteTaskUser);
                String strOrg = (String) mpTaskInfo.get(DomainConstants.SELECT_ORGANIZATION);
                String strCollabSpace = (String) mpTaskInfo.get(DomainAccess.SELECT_PROJECT);

                StringList objPersonSelect = new StringList();
                objPersonSelect.add(DomainConstants.SELECT_ID);

                if (UIUtil.isNotNullAndNotEmpty(strOrg) && UIUtil.isNotNullAndNotEmpty(strRoleName) && UIUtil.isNotNullAndNotEmpty(strCollabSpace)) {
                    // String strScurityContext = strRoleName + "." + strOrg + "." + strCollabSpace;
                    String strScurityContext = "\"" + strRoleName + "\".\"" + strOrg + "\".\"" + strCollabSpace + "\" ";
                    BusinessObject boSecurityContext = new BusinessObject(DomainConstants.TYPE_SECURITYCONTEXT, strRoleName + "." + strOrg + "." + strCollabSpace, "-",
                            TigerConstants.VAULT_ESERVICEPRODUCTION);

                    DomainObject domSecurityContext = new DomainObject(boSecurityContext);

                    // get the Person list who has assigned that Security-context of Role
                    MapList mlPerson = domSecurityContext.getRelatedObjects(context, DomainConstants.RELATIONSHIP_ASSIGNED_SECURITY_CONTEXT, DomainConstants.TYPE_PERSON, objPersonSelect,
                            new StringList(), true, false, (short) 0, null, "", 0);

                    if (!mlPerson.isEmpty()) {
                        for (Object objPerson : mlPerson) {
                            Map mpPerson = (Map) objPerson;

                            // Get the person Details
                            String strPersonID = (String) mpPerson.get(DomainConstants.SELECT_ID);
                            DomainObject domPerson = DomainObject.newInstance(context, strPersonID);

                            StringList slPersonSlects = new StringList();
                            slPersonSlects.add("attribute[" + ATTRIBUTE_ABSENCE_START_DATE + "]");
                            slPersonSlects.add("attribute[" + ATTRIBUTE_ABSENCE_END_DATE + "]");
                            slPersonSlects.add("attribute[" + ATTRIBUTE_ABSENCE_DELEGATE + "]");
                            slPersonSlects.add(DomainConstants.SELECT_NAME);

                            // Get the Person's name and absence StartDate, absence EndDate.
                            Map<String, String> mpPersonInfo = domPerson.getInfo(context, slPersonSlects);
                            String sAbsenceStartDate = mpPersonInfo.get("attribute[" + ATTRIBUTE_ABSENCE_START_DATE + "]");
                            String sAbsenceEndDate = mpPersonInfo.get("attribute[" + ATTRIBUTE_ABSENCE_END_DATE + "]");
                            String strPersonName = mpPersonInfo.get(DomainConstants.SELECT_NAME);

                            SimpleDateFormat sdfEmatrix = new SimpleDateFormat(eMatrixDateFormat.getEMatrixDateFormat(), context.getLocale());
                            String sToday = sdfEmatrix.format(new Date());

                            boolean isAbsent = false;
                            if (UIUtil.isNotNullAndNotEmpty(sAbsenceEndDate) && UIUtil.isNotNullAndNotEmpty(sAbsenceStartDate)) {
                                Date dateAbsenceStart = sdfEmatrix.parse(sAbsenceStartDate);
                                Date dateAbsenceEnd = sdfEmatrix.parse(sAbsenceEndDate);
                                Date dateToday = sdfEmatrix.parse(sToday);

                                // If current date is in between Absence date of Person then set the Absent
                                if (dateToday.after(dateAbsenceStart) && dateToday.before(dateAbsenceEnd)) {
                                    isAbsent = true;
                                } else if (dateAbsenceStart.equals(dateToday) || dateAbsenceEnd.equals(dateToday)) {
                                    isAbsent = true;
                                }
                            }

                            // If Person is absent then put the name of that Person and Delegate person in map and return that map
                            if (isAbsent) {
                                String strDelegateUser = mpPersonInfo.get("attribute[" + ATTRIBUTE_ABSENCE_DELEGATE + "]");
                                mpUserVsDelegatedUser.put(strPersonName, strDelegateUser);
                            }

                        }
                    }
                }

            }

        } catch (Exception Ex) {
            logger.error("Error in getUserVsDelegateUserMap: ", Ex);
            throw Ex;
        }
        return mpUserVsDelegatedUser;
    }

    /**
     * This method is used to get Delegator for Task. It is used by PSS_APPObjectRouteDeleagteEvent notification object to get Static To List.PCM : TIGTK-4560 : 28/06/2017 : AB
     * @param context
     * @param args
     * @throws Exception
     */
    public StringList getTaskDelegator(Context context, String args[]) throws Exception {
        StringList slDelegator = new StringList();
        try {
            Map<?, ?> programMap = (Map<?, ?>) JPO.unpackArgs(args);
            Map<?, ?> mpPayload = (Map<?, ?>) programMap.get("payload");

            // Get the Delegator user from payload map and if it not null or empty add it to slDelegator StringList.
            String sDelegator = (String) mpPayload.get("toUser");
            if (UIUtil.isNotNullAndNotEmpty(sDelegator)) {
                slDelegator.add(sDelegator);
            }
        } catch (Exception Ex) {
            logger.error("Error in getTaskDelegator: ", Ex);
            throw Ex;
        }
        return slDelegator;
    }
}
