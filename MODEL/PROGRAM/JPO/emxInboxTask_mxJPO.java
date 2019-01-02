
/*
 * emxInboxTask.java
 *
 * Copyright (c) 1992-2015 Dassault Systemes.
 *
 * All Rights Reserved. This program contains proprietary and trade secret information of MatrixOne, Inc. Copyright notice is precautionary only and does not evidence any actual or intended
 * publication of such program.
 *
 */
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import com.matrixone.apps.common.InboxTask;
import com.matrixone.apps.common.Organization;
import com.matrixone.apps.common.Route;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.eMatrixDateFormat;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

/**
 * @version AEF Rossini - Copyright (c) 2002, MatrixOne, Inc.
 */
public class emxInboxTask_mxJPO extends emxInboxTaskBase_mxJPO {

    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(emxInboxTask_mxJPO.class);

    /**
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @throws Exception
     *             if the operation fails
     * @since AEF Rossini
     * @grade 0
     */
    public emxInboxTask_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
    }

    /**
     * @param context
     * @return
     * @throws Exception
     */
    protected HashMap getInboxTaskPropertiesAccessCommands(Context context, String[] args) throws FrameworkException {
        try {
            String contextUser = context.getUser();
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap requestMap = (HashMap) programMap.get("requestMap");
            String objectId = (String) programMap.get("objectId");
            objectId = objectId != null ? objectId : (String) requestMap.get("objectId");

            String selAttrAssigneeSetDueDate = getAttributeSelect(ATTRIBUTE_ASSIGNEE_SET_DUEDATE);
            String selAttrAllowDelegation = getAttributeSelect(ATTRIBUTE_ALLOW_DELEGATION);
            String selAttrScheduledCompletionDate = getAttributeSelect(ATTRIBUTE_SCHEDULED_COMPLETION_DATE);

            InboxTask taskBean = (InboxTask) DomainObject.newInstance(context, objectId);

            String SELECT_ROUTE_ID = "from[" + RELATIONSHIP_ROUTE_TASK + "].to.id";
            String SELECT_TASK_ASSIGNEE_NAME = "from[" + RELATIONSHIP_PROJECT_TASK + "].to.name";
            String selAttrNeedsReview = getAttributeSelect(ATTRIBUTE_REVIEW_TASK);

            StringList slBusSelect = new StringList();
            slBusSelect.add(SELECT_CURRENT);
            slBusSelect.add(SELECT_OWNER);
            slBusSelect.add(Route.SELECT_ROUTE_ACTION);
            slBusSelect.add(selAttrNeedsReview);
            slBusSelect.add(SELECT_ROUTE_ID);
            slBusSelect.add(SELECT_TASK_ASSIGNEE_NAME);

            Map mapTaskInfo = taskBean.getInfo(context, slBusSelect);
            String taskState = (String) mapTaskInfo.get(SELECT_CURRENT);
            String sTaskOwner = (String) mapTaskInfo.get(SELECT_OWNER);
            String routeAction = (String) mapTaskInfo.get(Route.SELECT_ROUTE_ACTION);
            String needsReview = (String) mapTaskInfo.get(selAttrNeedsReview);
            String strTaskAssignee = (String) mapTaskInfo.get(SELECT_TASK_ASSIGNEE_NAME);
            String taskScheduledDate = (String) mapTaskInfo.get(selAttrScheduledCompletionDate);
            String assigneeDueDateOpt = (String) mapTaskInfo.get(selAttrAssigneeSetDueDate);
            String allowDelegation = (String) mapTaskInfo.get(selAttrAllowDelegation);

            boolean isAssignedToGroupOrRole = taskBean.checkIfTaskIsAssignedToGroupOrRole(context);
            boolean isApprovalRoute = "Approve".equals(routeAction);
            boolean isCommentTask = "Comment".equalsIgnoreCase(routeAction);

            // Due to Resume Process implementation there can be tasks which are not connected to route and hence we cannot find
            // the route id from these tasks. Then the route id can be found by first finding the latest revision of the task
            // and then querying for the route object.
            // Bug 302957 - Added push and pop context
            ContextUtil.pushContext(context);
            String routeId = (String) mapTaskInfo.get(SELECT_ROUTE_ID);
            boolean isReadOnly = UIUtil.isNullOrEmpty(routeId);
            if (isReadOnly) {
                DomainObject dmoLastRevision = new DomainObject(taskBean.getLastRevision(context));
                routeId = dmoLastRevision.getInfo(context, SELECT_ROUTE_ID);
                // No action commands will be shown for such tasks
                isReadOnly = true;
            }

            Route boRoute = (Route) DomainObject.newInstance(context, TYPE_ROUTE);
            boRoute.setId(routeId);

            String sSelectOwningOrgId = "from[" + RELATIONSHIP_INITIATING_ROUTE_TEMPLATE + "].to.to[" + RELATIONSHIP_OWNING_ORGANIZATION + "].from.id";
            StringList busSelects = new StringList(4);
            busSelects.addElement(SELECT_ID);
            busSelects.addElement(SELECT_OWNER);
            busSelects.addElement(Route.SELECT_ROUTE_STATUS);
            busSelects.addElement(sSelectOwningOrgId);
            Map mRouteInfo = boRoute.getInfo(context, busSelects);
            String routeOwner = (String) mRouteInfo.get(DomainConstants.SELECT_OWNER);
            String sStatus = (String) mRouteInfo.get(Route.SELECT_ROUTE_STATUS);
            String sOwningOrgId = (String) mRouteInfo.get(sSelectOwningOrgId);

            ContextUtil.popContext(context);

            boolean isRouteStarted = "Started".equals(sStatus);
            String showAbstain = EnoviaResourceBundle.getProperty(context, "emxComponents.Routes.ShowAbstainForTaskApproval");
            showAbstain = UIUtil.isNullOrEmpty(showAbstain) ? "true" : showAbstain;

            String STATE_ASSIGNED = PropertyUtil.getSchemaProperty(context, "policy", POLICY_INBOX_TASK, "state_Assigned");
            String STATE_REVIEW = PropertyUtil.getSchemaProperty(context, "policy", POLICY_INBOX_TASK, "state_Review");
            String STATE_COMPLETE = PropertyUtil.getSchemaProperty(context, "policy", POLICY_INBOX_TASK, "state_Complete");

            boolean isInAssignedState = taskState.equals(STATE_ASSIGNED);
            boolean isInReviewState = taskState.equals(STATE_REVIEW);
            boolean isInCompleteState = taskState.equalsIgnoreCase(STATE_COMPLETE);

            boolean isTaskOwner = sTaskOwner.equals(contextUser);
            boolean isRouteOwner = routeOwner.equals(contextUser);
            boolean isTaskAssignee = contextUser.equals(strTaskAssignee);

            boolean bShowAcceptCmd = true;
            boolean bEditDetails = false;
            boolean bCompleteLink = false;
            boolean bApproveLink = false;
            boolean bNeedsReview = isInReviewState && isRouteOwner;

            // ////////////////////////////////////////////////////////////////////////////
            // The command Update Assignee will be shown in Assigned state only.
            // Route owner can Update Assignee any time
            // Task assignee can Update Assignee, only if the task is delegatable
            //
            if (isInAssignedState) {
                // IR-043921V6R2011 - Changes START
                if (isAssignedToGroupOrRole && !UIUtil.isNullOrEmpty(sOwningOrgId)) {
                    Organization org = (Organization) DomainObject.newInstance(context, sOwningOrgId);
                    busSelects = new StringList(2);
                    busSelects.addElement(SELECT_ID);
                    busSelects.addElement(SELECT_NAME);
                    String sWhereClause = "( name == \"" + contextUser + "\" )";
                    MapList mlMembers = org.getMemberPersons(context, busSelects, sWhereClause, null);
                    bShowAcceptCmd = !mlMembers.isEmpty();
                }
            }

            // Show Complete link for non-Approve type of tasks
            bCompleteLink = (isInAssignedState && isTaskOwner && !isApprovalRoute);

            // for Approve tasks, show the links Approve / Reject / Abstain
            // if(!taskState.equalsIgnoreCase("Complete") && (taskState.equals("Assigned") && sTaskOwner.equals(sLoginPerson) && "Approve".equals(routeAction) || strTaskAssignee.equals(sLoginPerson)))
            // {

            if (isInAssignedState && isApprovalRoute && (isTaskOwner || isTaskAssignee)) {
                bApproveLink = true;
            }

            HashMap returnMap = new HashMap(5);

            // Edit details link is provided when any of the 3 fields in the edit task webform is displayed, otherwise we dont display thr edit details command.
            boolean bDueDateEmpty = UIUtil.isNullOrEmpty(taskScheduledDate);
            boolean bAssigneeDueDate = "Yes".equals(assigneeDueDateOpt);
            boolean showTaskComments = isTaskOwner && isInAssignedState;
            boolean showReviewComments = "Yes".equalsIgnoreCase(needsReview);

            boolean showAssigneeDueDate = (bAssigneeDueDate && bDueDateEmpty) || (bAssigneeDueDate || "TRUE".equals(allowDelegation));
            boolean canEditReviewerComments = showReviewComments && STATE_REVIEW.equalsIgnoreCase(taskState) && isRouteOwner;
            bEditDetails = (showAssigneeDueDate || showTaskComments || canEditReviewerComments);

            bCompleteLink = isRouteStarted && !isReadOnly && bCompleteLink;
            bApproveLink = isRouteStarted && !isReadOnly && bApproveLink;
            boolean bAbstainLink = bApproveLink && "true".equalsIgnoreCase(showAbstain);
            bShowAcceptCmd = isAssignedToGroupOrRole && (taskState.equals("") || taskState.equals("Assigned")) && bShowAcceptCmd;

            // PCM : TIGTK-4560 : 28/06/2017 : AB : START

            if (!bShowAcceptCmd) {
                PSS_emxLifecycle_mxJPO emxLifecycle = new PSS_emxLifecycle_mxJPO(context, null);
                boolean isAbsenceDelegate = emxLifecycle.isAbsenceDelegate(context, contextUser, objectId);
                bShowAcceptCmd = isAbsenceDelegate;
            }

            // PCM : TIGTK-4560 : 28/06/2017 : AB : END

            returnMap.put("APPTaskEdit", Boolean.valueOf(bEditDetails));
            returnMap.put("APPCompleteTask", Boolean.valueOf(bCompleteLink));
            returnMap.put("APPApproveTask", Boolean.valueOf(bApproveLink));
            returnMap.put("APPRejectTask", Boolean.valueOf(bApproveLink));
            returnMap.put("APPAbstainTask", Boolean.valueOf(bAbstainLink));
            returnMap.put("APPAcceptInboxTask", Boolean.valueOf(bShowAcceptCmd));
            returnMap.put("APPPromoteInboxTask", Boolean.valueOf(bNeedsReview));
            returnMap.put("APPDemoteInboxTask", Boolean.valueOf(bNeedsReview));

            return returnMap;
        } catch (Exception e) {
            logger.error("Error in getInboxTaskPropertiesAccessCommands: ", e);
            throw new FrameworkException(e);
        }

    }

    /**
     * This method returns the details of Inbox Task for a perticular user
     * @param context
     * @param busSelects
     * @param typePattern
     * @param whereExpression
     * @return MapList
     * @throws Exception
     * @since V6R2014x
     */
    private MapList getInboxTaskDetails(Context context, StringList busSelects, String typePattern, String whereExpression) throws Exception {

        MapList returnMapList = DomainObject.findObjects(context, typePattern, QUERY_WILDCARD, // namepattern
                QUERY_WILDCARD, // revpattern
                QUERY_WILDCARD, // owner pattern
                QUERY_WILDCARD, // vault pattern
                whereExpression, // where exp
                true, busSelects);

        return returnMapList;
    }

    /**
     * This method returns daily Inbox Task & WBS Task object details togeather in a MapList for Widgets
     * @param context
     * @param args
     * @return MapList
     * @throws Exception
     * @since V6R2014x
     */
    public MapList getUserDailyTaskForWidgets(Context context, String[] args) throws Exception {
        String sAttrScheduledCompletionDate = PropertyUtil.getSchemaProperty("attribute_ScheduledCompletionDate");
        String strAttrCompletionDate = "attribute[" + sAttrScheduledCompletionDate + "]";

        SimpleDateFormat sdf = new SimpleDateFormat(eMatrixDateFormat.getInputDateFormat(), context.getLocale());
        // SimpleDateFormat sdf = new SimpleDateFormat(eMatrixDateFormat.getInputDateFormat(), Locale.US);
        Calendar todayCalenderEnd = Calendar.getInstance(context.getLocale());
        // Calendar todayCalender = Calendar.getInstance( Locale.US);
        todayCalenderEnd.set(Calendar.HOUR, 11);
        todayCalenderEnd.set(Calendar.MINUTE, 59);
        todayCalenderEnd.set(Calendar.SECOND, 59);
        todayCalenderEnd.set(Calendar.MILLISECOND, 0);
        todayCalenderEnd.set(Calendar.AM_PM, Calendar.PM);

        String sTodayDateEnd = sdf.format(todayCalenderEnd.getTime());

        Map programMap = (Map) JPO.unpackArgs(args);
        // StringList busSelects = (StringList) programMap.get("JPO_BUS_SELECTS");
        StringList busSelects = new StringList();
        busSelects.add(DomainConstants.SELECT_ID);
        busSelects.add(DomainConstants.SELECT_NAME);
        busSelects.add(DomainConstants.SELECT_TYPE);
        busSelects.add(DomainConstants.SELECT_CURRENT);
        busSelects.add(DomainConstants.SELECT_OWNER);
        busSelects.add("project");
        busSelects.add(strAttrCompletionDate);

        MapList retMapList = new MapList();
        MapList tempIBTaskMapList = new MapList();

        // Fetch Inbox Task object details
        String typePattern = DomainObject.TYPE_INBOX_TASK;
        String whereExpression = "owner == context.user AND current != 'Complete' AND " + strAttrCompletionDate + " <= '" + sTodayDateEnd + "'";
        System.out.println("ML WHEREXP " + whereExpression);

        tempIBTaskMapList = getInboxTaskDetails(context, busSelects, typePattern, whereExpression);
        for (int i = 0; i < tempIBTaskMapList.size(); i++) {
            retMapList.add((Map) tempIBTaskMapList.get(i));
        }
        return retMapList;
    }

}
