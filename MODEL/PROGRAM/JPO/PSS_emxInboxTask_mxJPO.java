import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeConstants;
import com.matrixone.apps.common.InboxTask;
import com.matrixone.apps.common.Person;
import com.matrixone.apps.common.Route;
import com.matrixone.apps.common.util.ComponentsUtil;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MailUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.XSSUtil;
import com.matrixone.apps.domain.util.eMatrixDateFormat;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.Attribute;
import matrix.db.AttributeItr;
import matrix.db.AttributeList;
import matrix.db.BusinessObject;
import matrix.db.BusinessObjectAttributes;
import matrix.db.Context;
import matrix.db.ExpansionIterator;
import matrix.db.JPO;
import matrix.db.MQLCommand;
import matrix.db.Relationship;
import matrix.util.Pattern;
import matrix.util.StringList;
import pss.constants.TigerConstants;

public class PSS_emxInboxTask_mxJPO extends emxInboxTask_mxJPO {

    private static final long serialVersionUID = 1L;

    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PSS_emxInboxTask_mxJPO.class);

    // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - END
    // TIGTK-7745:Rutuja Ekatpure:28/7/2017:Start
    public static final String SELECT_ALLOW_DELEGATION = getAttributeSelect(ATTRIBUTE_ALLOW_DELEGATION);

    public static final String SELECT_TASK_ASSIGNEE_ID = "from[" + RELATIONSHIP_PROJECT_TASK + "].to.id";

    public static final String SELECT_TASK_ASSIGNEE_FIRST_NAME = "from[" + RELATIONSHIP_PROJECT_TASK + "].to." + Person.SELECT_FIRST_NAME;

    public static final String SELECT_TASK_ASSIGNEE_LAST_NAME = "from[" + RELATIONSHIP_PROJECT_TASK + "].to." + Person.SELECT_LAST_NAME;

    public static final String SELECT_ROUTE_ID = "from[" + RELATIONSHIP_ROUTE_TASK + "].to.id";

    public static final String SELECT_ROUTE_NODE_ID = getAttributeSelect(ATTRIBUTE_ROUTE_NODE_ID);

    public static final String SELECT_TITLE = getAttributeSelect(ATTRIBUTE_TITLE);

    public static final String strAttrRouteAction = "attribute[" + DomainConstants.ATTRIBUTE_ROUTE_ACTION + "]";

    public static final String strAttrCompletionDate = "attribute[" + DomainConstants.ATTRIBUTE_SCHEDULED_COMPLETION_DATE + "]";

    public static final String strAttrTitle = "attribute[" + DomainConstants.ATTRIBUTE_TITLE + "]";

    public static final String strAttrTaskCompletionDate = "attribute[" + DomainConstants.ATTRIBUTE_ACTUAL_COMPLETION_DATE + "]";

    public static final String strAttrTaskApprovalStatus = getAttributeSelect(DomainObject.ATTRIBUTE_APPROVAL_STATUS);

    public static final String routeIdSelectStr = "from[" + DomainConstants.RELATIONSHIP_ROUTE_TASK + "].to.id";

    public static final String routeTypeSelectStr = "from[" + DomainConstants.RELATIONSHIP_ROUTE_TASK + "].to.type";

    public static final String routeNameSelectStr = "from[" + DomainConstants.RELATIONSHIP_ROUTE_TASK + "].to.name";

    public static final String routeOwnerSelectStr = "from[" + DomainConstants.RELATIONSHIP_ROUTE_TASK + "].to.owner";

    public static final String objectNameSelectStr = "from[" + DomainConstants.RELATIONSHIP_ROUTE_TASK + "].to.to[" + DomainConstants.RELATIONSHIP_ROUTE_SCOPE + "].from.name";

    public static final String objectIdSelectStr = "from[" + DomainConstants.RELATIONSHIP_ROUTE_TASK + "].to.to[" + DomainConstants.RELATIONSHIP_ROUTE_SCOPE + "].from.id";

    public static final String routeApprovalStatusSelectStr = "from[" + DomainConstants.RELATIONSHIP_ROUTE_TASK + "].to." + Route.SELECT_ROUTE_STATUS;

    public static final String workflowIdSelectStr = "to[" + TigerConstants.RELATIONSHIP_WORK_FLOW_TASK + "].from.id";

    public static final String workflowNameSelectStr = "to[" + TigerConstants.RELATIONSHIP_WORK_FLOW_TASK + "].from.name";

    public static final String workflowTypeSelectStr = "to[" + TigerConstants.RELATIONSHIP_WORK_FLOW_TASK + "].from.type";

    public static final String strAttrworkFlowDueDate = "attribute[" + TigerConstants.ATTRIBUTE_WORK_FLOW_DUE_DATE + "]";

    public static final String strAttrTaskEstimatedFinishDate = "attribute[" + DomainConstants.ATTRIBUTE_TASK_ESTIMATED_FINISH_DATE + "]";

    public static final String strAttrTaskFinishDate = "attribute[" + DomainConstants.ATTRIBUTE_TASK_ACTUAL_FINISH_DATE + "]";

    public static final String strAttrworkFlowCompletinDate = "attribute[" + DomainConstants.ATTRIBUTE_ACTUAL_COMPLETION_DATE + "]";

    // PCM TIGTK-9994: 29/09/2017 : KWagh : START
    private static final String sAttrReviewersComments = PropertyUtil.getSchemaProperty("attribute_ReviewersComments");

    private static final String sTypeInboxTask = PropertyUtil.getSchemaProperty("type_InboxTask");

    private static final String sAttrReviewTask = PropertyUtil.getSchemaProperty("attribute_ReviewTask");
    // PCM TIGTK-9994: 29/09/2017 : KWagh : End

    // TIGTK-7745:Rutuja Ekatpure:28/7/2017:End
    public PSS_emxInboxTask_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
    }

    // TIGTK-6395:rutuja Ekatpure:27/4/2017:start
    /****
     * method returns task content in vector format to show as hyperlink on task content column
     * @param context
     * @param args
     *            contains object list of task objects
     * @return Vector
     * @throws Exception
     */
    public StringList showTaskContents(Context context, String[] args) throws Exception {
        StringList slShowTaskContents = new StringList();

        try {
            String sAccDenied = EnoviaResourceBundle.getProperty(context, "emxComponentsStringResource", context.getLocale(), "emxComponents.Common.AccessDenied");
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");
            String SELECT_ROUTE_ID = "from[" + DomainConstants.RELATIONSHIP_ROUTE_TASK + "].to.id";
            if (objectList != null) {
                Iterator<Map<?, ?>> itrTask = objectList.iterator();
                while (itrTask.hasNext()) {
                    Map<?, ?> mpTaskInfo = itrTask.next();
                    String objectId = (String) mpTaskInfo.get(DomainConstants.SELECT_ID);
                    // TIGTK-7588 : TS : 0/07/2017 : START
                    String objectType = (String) mpTaskInfo.get(DomainConstants.SELECT_TYPE);
                    String strCRObjectId = "";
                    String strCRObjectName = "";
                    StringBuffer sContentString = new StringBuffer();
                    if (TigerConstants.TYPE_PSS_ROLEASSESSMENT.equals(objectType)) {
                        DomainObject domObject = DomainObject.newInstance(context, objectId);
                        StringList slObjSelect = new StringList();
                        slObjSelect.add("to[" + TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.id");
                        slObjSelect.add("to[" + TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.name");
                        Map<?, ?> mapCRDetails = domObject.getInfo(context, slObjSelect);
                        strCRObjectId = (String) mapCRDetails.get("to[" + TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.id");
                        strCRObjectName = (String) mapCRDetails
                                .get("to[" + TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.name");
                        String sNextUrl = "./emxTree.jsp?objectId=" + XSSUtil.encodeForJavaScript(context, strCRObjectId);
                        String sFinalUrl = "javascript:emxTableColumnLinkClick('" + sNextUrl + "','null','null',false,'content','')";
                        sContentString = sContentString.append("<a  href=\"" + sFinalUrl + "\">" + XSSUtil.encodeForHTML(context, strCRObjectName) + "</a>");
                        slShowTaskContents.add(sContentString.toString());
                    }
                    // TIGTK-7588 : TS : 0/07/2017 : END
                    else {

                        DomainObject taskObject = DomainObject.newInstance(context, objectId);
                        String sRouteObjId = taskObject.getInfo(context, SELECT_ROUTE_ID);
                        if (UIUtil.isNotNullAndNotEmpty(sRouteObjId)) {
                            Route routeObject = new Route(sRouteObjId);
                            StringList selListObj = new StringList(2);
                            selListObj.add(DomainConstants.SELECT_NAME);
                            selListObj.add(DomainConstants.SELECT_ID);

                            StringList selListRel = new StringList();

                            MapList mlTaskContentObjsList = routeObject.getConnectedObjects(context, selListObj, selListRel, false);
                            Iterator<Map<?, ?>> itrTaskContent = mlTaskContentObjsList.iterator();
                            StringBuffer sbContent = new StringBuffer();
                            while (itrTaskContent.hasNext()) {
                                Map<?, ?> mpContent = itrTaskContent.next();
                                String sNextUrl = "./emxTree.jsp?objectId=" + XSSUtil.encodeForJavaScript(context, (String) mpContent.get(DomainConstants.SELECT_ID));

                                String sFinalUrl = "javascript:emxTableColumnLinkClick('" + sNextUrl + "','null','null',false,'content','')";
                                sbContent = sbContent.append("<a  href=\"" + sFinalUrl + "\">" + XSSUtil.encodeForHTML(context, (String) mpContent.get(DomainConstants.SELECT_NAME)) + "</a>");
                            }
                            slShowTaskContents.add(sbContent.toString());
                        } else {
                            slShowTaskContents.add(sAccDenied);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error in showTaskContents: ", e);
        }
        return slShowTaskContents;
    }
    // TIGTK-6395:rutuja Ekatpure:27/4/2017:End

    /**
     * Updates the assignee for the Inbox Task.
     * @param context
     *            The Matrix Context object
     * @returns Hashmap
     * @throws Exception
     *             if the operation fails
     * @since R212
     */

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @com.matrixone.apps.framework.ui.PostProcessCallable
    public HashMap updateAssignee(Context context, String[] args) throws Exception {

        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap requestMap = (HashMap) programMap.get("requestMap");
        // Added by Suchit G. on 14/07/2017 for TIGTK-9041: START
        HashMap paramMap = (HashMap) programMap.get("paramMap");
        // Added by Suchit G. on 14/07/2017 for TIGTK-9041: END
        String languageStr = (String) requestMap.get("languageStr");
        String timeZone = (String) requestMap.get("timeZone");
        String taskId = (String) requestMap.get("objectId");
        // Modified by Suchit G. for TIGTK-9041 on 14/07/2017 to get
        // PSS_ReassignComments instead of the original ReassignComments: START
        String reassignComments = (String) paramMap.get("PSS_ReassignComments");
        // TIGTK-11455----START
        if (UIUtil.isNullOrEmpty(reassignComments)) { // In case of MCA comments in "ReassignComments"
            reassignComments = (String) paramMap.get("ReassignComments");
        }
        // TIGTK-11455----END
        // Modified by Suchit G. for TIGTK-9041 on 14/07/2017 to get
        // PSS_ReassignComments instead of the original ReassignComments: END
        String taskScheduledDate = (String) requestMap.get("DueDate");
        String assigneeDueTime = (String) requestMap.get("routeTime");
        String newTaskAssignee = (String) requestMap.get("NewAssignee");
        // TIGTK-10709 -- START
        String strContextUser = context.getUser();
        // TIGTK-10709 -- END
        Locale locale = (Locale) requestMap.get("localeObj");

        // To see if the new assignee is a person or group
        String cmd = MqlUtil.mqlCommand(context, "print user \"" + newTaskAssignee + "\" select isaperson isagroup dump |");
        boolean isPerson = "TRUE|FALSE".equalsIgnoreCase(cmd);
        boolean isGroup = "FALSE|TRUE".equalsIgnoreCase(cmd);

        HashMap resultsMap = new HashMap();

        InboxTask inboxTaskObj = (InboxTask) DomainObject.newInstance(context, DomainConstants.TYPE_INBOX_TASK);
        // Route routeobj =
        // (Route)DomainObject.newInstance(context,DomainConstants.TYPE_ROUTE);
        if (taskId != null && !"".equals(taskId)) {
            inboxTaskObj.setId(taskId);
        }

        if (!UIUtil.isNullOrEmpty(taskScheduledDate)) {
            double clientTZOffset = Double.valueOf(timeZone);
            taskScheduledDate = eMatrixDateFormat.getFormattedInputDateTime(context, taskScheduledDate, assigneeDueTime, clientTZOffset, locale);
        }
        // Get the old assignee
        String sTaskOldAssignee = inboxTaskObj.getInfo(context, "from[" + DomainConstants.RELATIONSHIP_PROJECT_TASK + "].to.name");

        String sRTUAttrValue = inboxTaskObj.getAttributeValue(context, DomainConstants.ATTRIBUTE_ROUTE_TASK_USER);
        sTaskOldAssignee = !"".equalsIgnoreCase(sRTUAttrValue) ? PropertyUtil.getSchemaProperty(context, sRTUAttrValue) : sTaskOldAssignee;

        // Check if both old assignee and new assignee are same then alert the
        // end user
        if (newTaskAssignee.equals(sTaskOldAssignee)) {
            resultsMap.put("Message", EnoviaResourceBundle.getProperty(context, "emxComponentsStringResource", new Locale(languageStr), "emxComponents.ChangeAssignee.NewAssignee"));
            return resultsMap;
        }

        Route route = (Route) DomainObject.newInstance(context, DomainConstants.TYPE_ROUTE);
        String allowDelegation = (String) inboxTaskObj.getInfo(context, getAttributeSelect(ATTRIBUTE_ALLOW_DELEGATION));
        String taskRouteAction = (String) inboxTaskObj.getInfo(context, getAttributeSelect(ATTRIBUTE_ROUTE_ACTION));

        String sTaskOldAssigneeId = inboxTaskObj.getInfo(context, "from[" + DomainConstants.RELATIONSHIP_PROJECT_TASK + "].to.id");
        DomainObject dmoLastRevision = new DomainObject(inboxTaskObj.getLastRevision(context));
        String sRouteId = dmoLastRevision.getInfo(context, "from[" + DomainConstants.RELATIONSHIP_ROUTE_TASK + "].to.id");
        if ("TRUE".equalsIgnoreCase(allowDelegation)) {
            inboxTaskObj.setAttributeValue(context, inboxTaskObj.ATTRIBUTE_SCHEDULED_COMPLETION_DATE, taskScheduledDate);
        }

        reassignComments = (reassignComments == null || "".equals(reassignComments.trim()) || "null".equals(reassignComments)) ? "" : reassignComments;
        // If the selected new Assignee is a group then invoke reAssignToGroup()
        if (isGroup) {
            reAssignToGroup(context, newTaskAssignee, inboxTaskObj, sRouteId, languageStr);
        }
        // If the selected new Assignee is a person
        if (isPerson) {
            String sTaskNewAssigneeId = !UIUtil.isNullOrEmpty(newTaskAssignee) ? PersonUtil.getPersonObjectID(context, newTaskAssignee) : "";

            if (!UIUtil.isNullOrEmpty(sTaskNewAssigneeId) && !UIUtil.isNullOrEmpty(sTaskOldAssigneeId) && !sTaskNewAssigneeId.equals(sTaskOldAssigneeId)) {
                // For delegation functionality the Allow Delegation attribute
                // should be TRUE. So we will do the same momentarily if the
                // Allow Delegation is No
                boolean isAllowedDelegationMomentarily = false;
                if (!"True".equalsIgnoreCase(allowDelegation)) {
                    isAllowedDelegationMomentarily = true;
                    inboxTaskObj.setAttributeValue(context, inboxTaskObj.ATTRIBUTE_ALLOW_DELEGATION, "TRUE");
                }

                // ////////////////////////// START ABSENCE DELEGATION
                // HANDLING/////////////////////////////
                // Check if the new assignee has configured absence delegation
                // and he is absent
                if ("Yes".equalsIgnoreCase(allowDelegation)) {
                    final String SELECT_ATTRIBUTE_ABSENCE_DELEGATE = "attribute[" + TigerConstants.ATTRIBUTE_ABSENCEDELEGATE + "]";
                    final String SELECT_ATTRIBUTE_ABSENCE_END_DATE = "attribute[" + TigerConstants.ATTRIBUTE_ABSENCEENDDATE + "]";
                    final String SELECT_ATTRIBUTE_ABSENCE_START_DATE = "attribute[" + TigerConstants.ATTRIBUTE_ABSENCESTARTDATE + "]";

                    SimpleDateFormat dateFormat = new SimpleDateFormat(eMatrixDateFormat.getInputDateFormat(), context.getLocale());
                    StringList slBusSelect = new StringList();
                    slBusSelect.addElement(SELECT_ATTRIBUTE_ABSENCE_DELEGATE);
                    slBusSelect.addElement(SELECT_ATTRIBUTE_ABSENCE_START_DATE);
                    slBusSelect.addElement(SELECT_ATTRIBUTE_ABSENCE_END_DATE);

                    Vector vecAlreadyVisitedNewAssignees = new Vector();

                    while (!vecAlreadyVisitedNewAssignees.contains(sTaskNewAssigneeId)) {
                        DomainObject dmoNewAssignee = new DomainObject(sTaskNewAssigneeId);
                        Map mapNewAssigneeInfo = dmoNewAssignee.getInfo(context, slBusSelect);

                        String strAbsenceDelegate = (String) mapNewAssigneeInfo.get(SELECT_ATTRIBUTE_ABSENCE_DELEGATE);
                        String strAbsenceStartDate = (String) mapNewAssigneeInfo.get(SELECT_ATTRIBUTE_ABSENCE_START_DATE);
                        String strAbsenceEndDate = (String) mapNewAssigneeInfo.get(SELECT_ATTRIBUTE_ABSENCE_END_DATE);

                        // If the absence delegation is configured then
                        if (strAbsenceDelegate != null && !"".equals(strAbsenceDelegate) && strAbsenceStartDate != null && !"".equals(strAbsenceStartDate) && strAbsenceEndDate != null
                                && !"".equals(strAbsenceEndDate)) {

                            // Is the new user absent?
                            Date dtAbsenceStart = dateFormat.parse(strAbsenceStartDate);
                            Date dtAbsenceEnd = dateFormat.parse(strAbsenceEndDate);
                            Date dtToday = new Date();
                            if (dtToday.after(dtAbsenceStart) && dtToday.before(dtAbsenceEnd)) {
                                vecAlreadyVisitedNewAssignees.add(sTaskNewAssigneeId);
                                sTaskNewAssigneeId = PersonUtil.getPersonObjectID(context, strAbsenceDelegate);
                            } else {
                                break;
                            }
                        } else {
                            break;
                        }
                    } // ~while

                    if (vecAlreadyVisitedNewAssignees.contains(sTaskNewAssigneeId)) {
                        resultsMap.put("Message", "Circular reference found while traversing absence delegation chain.");
                    }
                    vecAlreadyVisitedNewAssignees.clear();
                } // ~if allowdelegation is Yes
                  //
                  // //////////////////////////END ABSENCE DELEGATION
                  // HANDLING/////////////////////////////

                // Delegate the current task to the new assignee
                // TIGTK-7745:Rutuja Ekatpure:27/7/2017:start
                // inboxTaskObj.delegateTask(context, sTaskNewAssigneeId);
                delegateTask(context, taskId, (String) sTaskNewAssigneeId);
                // TIGTK-7745:Rutuja Ekatpure:27/7/2017:End
                // If the attribute Allow Delegation was set "TRUE" momentarily then reset it back
                if (isAllowedDelegationMomentarily) {
                    inboxTaskObj.setAttributeValue(context, inboxTaskObj.ATTRIBUTE_ALLOW_DELEGATION, "FALSE");
                }

                // /////////////////////////////////////////////////////////////////////////////////////
                // Send the reassignment notification comment provided by the
                // user reassigning the task

                // Find the route owner
                route.setId(sRouteId);
                String strRouteOwner = route.getInfo(context, DomainObject.SELECT_OWNER);
                // Setting the allowDelegation as false once the task is
                // delegated to some other user by Task Owner. So that the new
                // Task Owner
                // cannot delegate the task once again. If first time itself the
                // task delegation is done by route owner then we should not
                // change the value.
                if (!strRouteOwner.equals(context.getUser()))
                    allowDelegation = "false";
                // Ended

                // Find the new and old task assignee name
                java.util.Set userList = new java.util.HashSet();
                userList.add(sTaskNewAssigneeId);

                Map mapInfo = com.matrixone.apps.common.Person.getPersonsFromIds(context, userList, new StringList(com.matrixone.apps.common.Person.SELECT_NAME));
                String strNewTaskAssigneeName = (String) ((Map) mapInfo.get(sTaskNewAssigneeId)).get(com.matrixone.apps.common.Person.SELECT_NAME);

                // If current user is the route owner then send notification to
                // new user and old task assignee
                // If current user is the old task assignee then send
                // notification to route owner and old task assignee

                // Form the subject
                String strSubject = EnoviaResourceBundle.getProperty(context, "emxComponentsStringResource", new Locale(languageStr), "emxComponents.ReassignRouteApprover.Notification.Subject");
                String strBody = EnoviaResourceBundle.getProperty(context, "emxComponentsStringResource", new Locale(languageStr), "emxComponents.ReassignRouteApprover.Notification.Body");

                // Adding task reassign comments to body

                StringBuffer messageBody = new StringBuffer();
                // Findbug Issue correction start
                // Date: 22/03/2017
                // By: Asha G.
                messageBody.append(strBody);
                messageBody.append(reassignComments);
                // Findbug Issue correction end
                DomainObject routeobj = new DomainObject(sRouteId);

                // Addition for Tiger - PCM stream by SGS starts

                String strChangeObjectId = routeobj.getInfo(context, "to[" + RELATIONSHIP_OBJECT_ROUTE + "].from.id"); // create
                // the
                // Domain
                // Object
                // of
                // Change
                // Request
                // PCM TIGTK-6772 | 19/04/17 :Pooja Mantri : Start
                String strType = "";
                if (UIUtil.isNotNullAndNotEmpty(strChangeObjectId)) {
                    DomainObject Changerequestobj = new DomainObject(strChangeObjectId);
                    StringList selects = new StringList();
                    selects.add(SELECT_TYPE);
                    selects.add(SELECT_DESCRIPTION);
                    selects.add(SELECT_NAME);
                    selects.add("from[" + ChangeConstants.RELATIONSHIP_CHANGE_COORDINATOR + "].to.id");
                    selects.add("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
                    selects.add("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.name");
                    selects.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_PARALLELTRACK + "]");
                    selects.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRREASONFORCHANGE + "]");
                    selects.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_PARALLELTRACKCOMMENT + "]");
                    selects.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE + "]");
                    selects.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRTARGETCHANGEIMPLEMENTATIONCOMMENT + "]");

                    Map map = Changerequestobj.getInfo(context, selects); // to
                                                                          // retrieve
                                                                          // the
                                                                          // Change
                                                                          // Request
                                                                          // selectables
                    strType = (String) map.get("type");
                }
                // PCM TIGTK-6772 | 19/04/17 :Pooja Mantri : End

                // TIGTK-10709 -- START
                Map payload = new HashMap();
                payload.put("toList", new StringList(newTaskAssignee));
                payload.put("inboxtaskId", taskId);
                payload.put("fromList", strContextUser);
                // TIGTK-11455 : START Reassign comments
                payload.put("Comments", reassignComments);
                // TIGTK-11455 : END
                Map<String, String> mapTypeVsNotificationName = new HashMap<String, String>();
                mapTypeVsNotificationName.put(TigerConstants.TYPE_PSS_CHANGEREQUEST, "PSS_CRTaskReassignNotification");
                mapTypeVsNotificationName.put(TigerConstants.TYPE_CHANGEACTION, "PSS_CATaskReassignNotification");
                mapTypeVsNotificationName.put(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION, "PSS_MCATaskReassignNotification");
                mapTypeVsNotificationName.put(TigerConstants.TYPE_PSS_CHANGENOTICE, "PSS_CNTaskReassignNotification");
                mapTypeVsNotificationName.put(TigerConstants.TYPE_PSS_ISSUE, "PSS_IssueTaskReassignNotification");
                String strNotificationObjName = (String) mapTypeVsNotificationName.get(strType);
                if (UIUtil.isNotNullAndNotEmpty(strNotificationObjName)) {
                    emxNotificationUtil_mxJPO.objectNotification(context, strChangeObjectId, strNotificationObjName, payload);
                }
            }
        }

        BusinessObject boTask = new BusinessObject(taskId);
        boTask.open(context);

        BusinessObjectAttributes boAttrGeneric = boTask.getAttributes(context);
        AttributeItr attrItrGeneric = new AttributeItr(boAttrGeneric.getAttributes());
        AttributeList attrListGeneric = new AttributeList();

        String sAttrValue = "";
        String sTrimVal = "";
        while (attrItrGeneric.next()) {
            Attribute attrGeneric = attrItrGeneric.obj();
            sAttrValue = (String) requestMap.get(attrGeneric.getName());

            // Validating for the attribute allow delegation and updating the
            // value
            if (attrGeneric.getName().equals(DomainConstants.ATTRIBUTE_ALLOW_DELEGATION)) {
                sAttrValue = allowDelegation;
            }
            // Ended
            if (sAttrValue != null) {
                sTrimVal = sAttrValue.trim();
                if (attrGeneric.getName().equals(DomainConstants.ATTRIBUTE_APPROVAL_STATUS) && sTrimVal.equals("Reject")) {
                    Pattern relPattern = new Pattern(DomainConstants.RELATIONSHIP_ROUTE_TASK);
                    Pattern typePattern = new Pattern(DomainConstants.TYPE_ROUTE);
                    BusinessObject boRoute = ComponentsUtil.getConnectedObject(context, boTask, relPattern.getPattern(), typePattern.getPattern(), false, true);

                    if (boRoute != null) {
                        boRoute.open(context);
                        AttributeItr attributeItr = new AttributeItr(boRoute.getAttributes(context).getAttributes());

                        Route routeObj = (Route) DomainObject.newInstance(context, boRoute);

                        StringList routeSelects = new StringList(3);
                        routeSelects.add(Route.SELECT_OWNER);
                        routeSelects.add(Route.SELECT_NAME);
                        routeSelects.add(Route.SELECT_REVISION);
                        Map routeInfo = routeObj.getInfo(context, routeSelects);

                        String routeOwner = (String) routeInfo.get(Route.SELECT_OWNER);
                        String routeName = (String) routeInfo.get(Route.SELECT_NAME);
                        String routeRev = (String) routeInfo.get(Route.SELECT_REVISION);

                        while (attributeItr.next()) {

                            Attribute attribute = attributeItr.obj();

                            if (attribute.getName().equals(DomainConstants.ATTRIBUTE_ROUTE_STATUS)) {
                                Map attrMap = new Hashtable();
                                attrMap.put(DomainConstants.ATTRIBUTE_ROUTE_STATUS, "Stopped");
                                routeObj.modifyRouteAttributes(context, attrMap);
                                /* send notification to the owner */
                                String[] subjectKeys = {};
                                String[] subjectValues = {};

                                String[] messageKeys = { "name", "IBType", "IBName", "IBRev", "RType", "RName", "RRev" };
                                String[] messageValues = { (context.getUser()), Route.TYPE_INBOX_TASK, boTask.getName(), boTask.getRevision(), Route.TYPE_ROUTE, routeName, routeRev };

                                StringList objectIdList = new StringList();
                                objectIdList.addElement(taskId);

                                StringList toList = new StringList();
                                toList.add(routeOwner);
                                MailUtil.sendNotification(context, toList, null, null, "emxFramework.ProgramObject.eServicecommonCompleteTask.SubjectReject", subjectKeys, subjectValues,
                                        "emxFramework.ProgramObject.eServicecommonCompleteTask.MessageReject", messageKeys, messageValues, objectIdList, null);
                                break;
                            }
                        }
                        boRoute.close(context);
                    }
                }
                attrGeneric.setValue(sTrimVal);
                attrListGeneric.addElement(attrGeneric);
            }
        }

        // Update the attributes on the Business Object
        boTask.setAttributes(context, attrListGeneric);
        boTask.update(context);
        String RelationshipId = FrameworkUtil.getAttribute(context, boTask, DomainConstants.ATTRIBUTE_ROUTE_NODE_ID);

        route.setId(sRouteId);

        // Get the correct relId for the RouteNodeRel given the attr routeNodeId
        // from the InboxTask.
        RelationshipId = route.getRouteNodeRelId(context, RelationshipId);

        // Updating the relationship Attributes
        Map attrMap = new Hashtable();

        Relationship relRouteNode = new Relationship(RelationshipId);
        relRouteNode.open(context);
        AttributeItr attrRelItrGeneric = new AttributeItr(relRouteNode.getAttributes(context));
        while (attrRelItrGeneric.next()) {
            sTrimVal = null;
            Attribute attrGeneric = attrRelItrGeneric.obj();
            sAttrValue = attrGeneric.getName();
            if (sAttrValue.equals(DomainConstants.ATTRIBUTE_SCHEDULED_COMPLETION_DATE)) {
                sTrimVal = taskScheduledDate;
            } else if (sAttrValue.equals(DomainConstants.ATTRIBUTE_ALLOW_DELEGATION)) {
                sTrimVal = allowDelegation;
            } else if (sAttrValue.equals(DomainConstants.ATTRIBUTE_ROUTE_ACTION)) {
                sTrimVal = taskRouteAction;
            }
            if (sTrimVal != null) {
                attrMap.put(sAttrValue, sTrimVal);
            }
        }
        Route.modifyRouteNodeAttributes(context, RelationshipId, attrMap);
        relRouteNode.close(context);
        boTask.close(context);

        return resultsMap;

    }

    /*
     * This Method get Subject for email Notification
     * 
     * @param context
     * 
     * @param args
     * 
     * @throws Exception Owner : Vishal B
     */
    public String getSubject(Context context, String[] args) throws Exception {

        final StringBuffer subjectKey = new StringBuffer();
        String strMsgTigerKey = DomainConstants.EMPTY_STRING;
        String strRejected = DomainConstants.EMPTY_STRING;
        String strCheckFor = DomainConstants.EMPTY_STRING;
        String strchangeNotice = DomainConstants.EMPTY_STRING;
        String cnObjectName = DomainConstants.EMPTY_STRING;
        String MCOObjectName = DomainConstants.EMPTY_STRING;
        String ProjectName = DomainConstants.EMPTY_STRING;
        // String PlantCode = DomainConstants.EMPTY_STRING;
        // String cnObjectId = DomainConstants.EMPTY_STRING;

        String strLanguage = context.getSession().getLanguage();
        strMsgTigerKey = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.Tiger");
        strRejected = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.Rejected");
        strCheckFor = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.For");
        strchangeNotice = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Type.PSS_ChangeNotice");

        try {
            Map info = (Map) JPO.unpackArgs(args);
            String taskObjectId = (String) info.get("id");
            HashMap mapGetReturn = getRelatedId(context, taskObjectId);

            cnObjectName = (String) mapGetReturn.get("cnObjectName");
            MCOObjectName = (String) mapGetReturn.get("MCOObjectName");
            ProjectName = (String) mapGetReturn.get("ProjectName");
            // PlantCode = (String) mapGetReturn.get("PlantCode");
            // cnObjectId = (String) mapGetReturn.get("cnObjectId");

            subjectKey.append(strMsgTigerKey + " ");
            subjectKey.append(strchangeNotice + " " + cnObjectName + " " + " ");
            subjectKey.append(strRejected + " " + MCOObjectName + " " + " ");
            subjectKey.append(strCheckFor + " " + ProjectName + " ");

        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getSubject: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

        return (subjectKey.toString());
    }

    /*
     * This Method get Message for email Notification
     * 
     * @param context
     * 
     * @param args
     * 
     * @throws Exception Owner : Vishal B
     */
    public String getMessageText(Context context, String[] args) throws Exception {
        final StringBuffer msgBuffer = new StringBuffer();
        // String PlantCode = DomainConstants.EMPTY_STRING;
        String cnObjectId = DomainConstants.EMPTY_STRING;
        String PSS_EFFECTIVITY_DATE = DomainConstants.EMPTY_STRING;
        String MBOM = "Pending";
        String strMsgTigerKey = DomainConstants.EMPTY_STRING;
        String strLanguage = context.getSession().getLanguage();
        String strCNDescription = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.CNDescription");
        String strEffectivityDate = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.EffectivityDate");
        String strRelated150mbom = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.Related150mbom");
        String strRelatedPlant = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.RelatedPlant");
        String RejectionComment = "Rejection Comment";
        // TIGTK-7468 - 06-07-2017 - PTE - START
        String strRejected = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.Rejected");
        String strCheckFor = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.For");
        // TIGTK-7468 - 06-07-2017 - PTE - END

        try {
            Map info = (Map) JPO.unpackArgs(args);
            String taskObjectId = (String) info.get("id");
            HashMap mapGetReturn = getRelatedId(context, taskObjectId);
            String baseURL = (String) info.get("baseURL");
            cnObjectId = (String) mapGetReturn.get("cnObjectId");
            DomainObject domCN = new DomainObject(cnObjectId);
            PSS_EFFECTIVITY_DATE = domCN.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_EFFECTIVITYDATE);
            String Description = domCN.getInfo(context, "description");
            DomainObject domTask = new DomainObject(taskObjectId);
            String getRejection = domTask.getAttributeValue(context, "Comments");
            String MCOObjectName = (String) mapGetReturn.get("MCOObjectName");
            String ProjectName = (String) mapGetReturn.get("ProjectName");
            final StringBuffer subjectKey = new StringBuffer();
            int position = baseURL.lastIndexOf("/");
            String strBaseURLSubstring = baseURL.substring(0, position);
            String sRelatedObjectNextUrl = strBaseURLSubstring + "/emxTree.jsp?objectId=" + mapGetReturn.get("PlantCode");
            // TIGTK-7468 - 06-07-2017 - PTE - START
            String sCNName = strBaseURLSubstring + "/emxTree.jsp?objectId=" + cnObjectId;
            subjectKey.append(" " + sCNName + " " + " ");
            subjectKey.append(strRejected + " " + MCOObjectName + " " + " ");
            subjectKey.append(strCheckFor + " " + ProjectName + " ");

            strMsgTigerKey = strMsgTigerKey + subjectKey.toString() + ("\n");
            // TIGTK-7468 - 06-07-2017 - PTE - END
            strMsgTigerKey = strMsgTigerKey + strCNDescription + " " + Description + ("\n");
            strMsgTigerKey = strMsgTigerKey + strEffectivityDate + " " + PSS_EFFECTIVITY_DATE + ("\n");
            strMsgTigerKey = strMsgTigerKey + strRelated150mbom + " " + MBOM + ("\n");
            strMsgTigerKey = strMsgTigerKey + strRelatedPlant + " " + sRelatedObjectNextUrl + ("\n");
            strMsgTigerKey = strMsgTigerKey + RejectionComment + " " + getRejection;
            strMsgTigerKey = strMsgTigerKey.trim();

            msgBuffer.append(strMsgTigerKey);

        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getMessageText: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

        return (msgBuffer.toString());
    }

    /*
     * This Method get RelatedID for email Notification
     * 
     * @param context
     * 
     * @param args
     * 
     * @throws Exception Owner : Vishal B
     */
    public HashMap getRelatedId(Context context, String strTaskId) {

        HashMap mreturnMap = new HashMap();
        String cnObjectName = DomainConstants.EMPTY_STRING;
        String MCOObjectName = DomainConstants.EMPTY_STRING;
        String ProjectName = DomainConstants.EMPTY_STRING;
        String PlantCode = DomainConstants.EMPTY_STRING;
        String cnObjectId = DomainConstants.EMPTY_STRING;

        try {
            if (UIUtil.isNotNullAndNotEmpty(strTaskId)) {
                DomainObject domTask = new DomainObject(strTaskId);
                String RouteObjectId = domTask.getInfo(context, "from[" + DomainConstants.RELATIONSHIP_ROUTE_TASK + "].to.id");
                if (UIUtil.isNotNullAndNotEmpty(RouteObjectId)) {
                    DomainObject domRoute = new DomainObject(RouteObjectId);
                    cnObjectId = domRoute.getInfo(context, "to[" + DomainConstants.RELATIONSHIP_OBJECT_ROUTE + "].from.id");
                    cnObjectName = domRoute.getInfo(context, "to[" + DomainConstants.RELATIONSHIP_OBJECT_ROUTE + "].from.name");

                    if (UIUtil.isNotNullAndNotEmpty(cnObjectId)) {
                        DomainObject CNDOM = new DomainObject(cnObjectId);

                        String MCOobjectId = CNDOM.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_RELATEDCN + "].from.id");
                        MCOObjectName = CNDOM.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_RELATEDCN + "].from.name");
                        if (UIUtil.isNotNullAndNotEmpty(MCOobjectId)) {
                            DomainObject domMCO = new DomainObject(MCOobjectId);
                            PlantCode = domMCO.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGRELATEDPLANT + "].to.id");
                            ProjectName = domMCO.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.name");

                        }
                    }
                }

            }
            mreturnMap.put("cnObjectName", cnObjectName);
            mreturnMap.put("MCOObjectName", MCOObjectName);
            mreturnMap.put("ProjectName", ProjectName);
            mreturnMap.put("PlantCode", PlantCode);
            mreturnMap.put("cnObjectId", cnObjectId);

        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getRelatedId: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

        return mreturnMap;

    }

    // PCM TIGTK-3411 | 02/16/17 : PTE Starts
    // Method is clone from JPO emxInboxTaskBase for TIGTK-3411 issue
    /**
     * check trigger on promote event of state "Review" of policy "Inbox Task" if the task is to be reviewed and the user is not the Route owner, an MQL error message needs to be shown since Review
     * can be done by the route owner only
     * @param context
     *            the eMatrix Context object
     * @return int - "0" if check is true, "1" if check is false
     * @throws Exception
     *             if the operation fails
     */
    public int triggerCheckPromoteOnReviewState(matrix.db.Context context, String[] args) throws Exception {
        // build selects
        String strAttrRouteAction = "attribute[" + DomainConstants.ATTRIBUTE_ROUTE_ACTION + "]";
        StringList selects = new StringList();
        selects.addElement("attribute[" + ATTRIBUTE_REVIEW_TASK + "]");
        selects.addElement("from[" + RELATIONSHIP_ROUTE_TASK + "].to.owner");
        selects.addElement("current");
        selects.addElement("owner");
        selects.addElement(strAttrRouteAction);
        selects.addElement("attribute[" + DomainObject.ATTRIBUTE_COMMENTS + "]");
        selects.addElement("attribute[" + ATTRIBUTE_APPROVAL_STATUS + "]");

        // get the details required
        Map taskMap = getInfo(context, selects);
        String reviewTask = (String) taskMap.get("attribute[" + ATTRIBUTE_REVIEW_TASK + "]");
        String routeOwner = (String) taskMap.get("from[" + RELATIONSHIP_ROUTE_TASK + "].to.owner");
        String taskOwner = (String) taskMap.get("owner");
        String comments = (String) taskMap.get("attribute[" + DomainObject.ATTRIBUTE_COMMENTS + "]");
        String status = (String) taskMap.get("attribute[" + DomainObject.ATTRIBUTE_APPROVAL_STATUS + "]");

        String current = (String) taskMap.get("current");

        // the Task must be promoted after review only if the user is the Route
        // owner
        if (DomainConstants.STATE_INBOX_TASK_REVIEW.equals(current) && reviewTask.equalsIgnoreCase("Yes") && !context.getUser().equals(routeOwner)) {
            // show MQL error message
            String msg = EnoviaResourceBundle.getFrameworkStringResourceProperty(context, "emxFramework.InboxTaskJPO.triggerCheckPromoteOnReviewState.CannotPromoteRouteInReviewState",
                    context.getLocale());
            MqlUtil.mqlCommand(context, "notice '" + msg + "'");
            return 1;
        } else if (!"Notify Only".equals((String) taskMap.get(strAttrRouteAction)) && DomainConstants.STATE_INBOX_TASK_ASSIGNED.equals(current)) {
            if (!(context.getUser().equals(taskOwner) || context.getUser().equals("User Agent"))) {
                String msg = EnoviaResourceBundle.getFrameworkStringResourceProperty(context, "emxFramework.InboxTaskJPO.triggerCheckPromoteOnAssignedState.CannotPromoteRouteTaskState",
                        context.getLocale());
                MqlUtil.mqlCommand(context, "notice '" + msg + "'");
                return 1;
            } else if (UIUtil.isNullOrEmpty(comments)
                    && ("true".equalsIgnoreCase(EnoviaResourceBundle.getProperty(context, "emxComponents.Routes.ShowCommentsForTaskApproval")) || "Reject".equalsIgnoreCase(status))) {
                // Modified for TIGTK-3411 by PTE
                String strObjId = (String) taskMap.get("id");
                DomainObject domTaskID = DomainObject.newInstance(context, strObjId);
                String strConectedRouteId = domTaskID.getInfo(context, "from[" + RELATIONSHIP_ROUTE_TASK + "].to.id");
                DomainObject domObjRoute = DomainObject.newInstance(context, strConectedRouteId);
                String strConectedCAId = domObjRoute.getInfo(context, "to[" + RELATIONSHIP_OBJECT_ROUTE + "].from.id");
                DomainObject domObj = DomainObject.newInstance(context, strConectedCAId);
                String strType = domObj.getInfo(context, DomainConstants.SELECT_TYPE);
                String strObjCurrent = domObj.getInfo(context, DomainConstants.SELECT_CURRENT);
                // Modify for PCM TIGTK-5878 | 4/4/17 : PTE
                if (!strType.equalsIgnoreCase(TigerConstants.TYPE_CHANGEACTION)
                        && (!strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_CHANGEREQUEST) && strObjCurrent.equalsIgnoreCase(TigerConstants.STATE_INREVIEW_CR))
                        && !strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION) && !strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_CHANGENOTICE)) {
                    String msg = EnoviaResourceBundle.getFrameworkStringResourceProperty(context, "emxFramework.FormComponent.MustEnterAValidValueFor", context.getLocale());
                    msg += " " + EnoviaResourceBundle.getFrameworkStringResourceProperty(context, "emxFramework.Lifecycle.Comments", context.getLocale());

                    MqlUtil.mqlCommand(context, "notice '" + msg + "'");
                    return 1;
                }
            }
        }
        return 0;
    }

    // PCM TIGTK-3411 | 02/16/17 : PTE Ends

    // TIGTK-7745:Rutuja Ekatpure:27/7/2017:Start
    /****
     * this method is copied from InboxTask jar for customising delegate task functionality,we have removed notification code from this method
     * @param context
     * @param paramString
     * @throws FrameworkException
     */
    public void delegateTask(Context context, String strTaskId, String paramString) throws FrameworkException {
        Object localObject1 = null;
        try {
            ContextUtil.startTransaction(context, true);

            InboxTask inboxTaskObj = (InboxTask) DomainObject.newInstance(context, DomainConstants.TYPE_INBOX_TASK);
            inboxTaskObj.setId(strTaskId);
            StringList slInfoSelect = new StringList(10);
            slInfoSelect.add("type");
            slInfoSelect.add("name");
            slInfoSelect.add(SELECT_TITLE);
            slInfoSelect.add(SELECT_ALLOW_DELEGATION);
            slInfoSelect.add(SELECT_TASK_ASSIGNEE_CONNECTION);
            slInfoSelect.add(SELECT_TASK_ASSIGNEE_NAME);
            slInfoSelect.add(SELECT_TASK_ASSIGNEE_FIRST_NAME);
            slInfoSelect.add(SELECT_TASK_ASSIGNEE_LAST_NAME);
            slInfoSelect.add(SELECT_TASK_ASSIGNEE_ID);
            slInfoSelect.add(SELECT_ROUTE_ID);
            slInfoSelect.add(SELECT_ROUTE_NODE_ID);

            Map mpReturnInfo = inboxTaskObj.getInfo(context, slInfoSelect);

            String strAllowDelegation = (String) mpReturnInfo.get(SELECT_ALLOW_DELEGATION);

            if ("FALSE".equalsIgnoreCase(strAllowDelegation)) {
                try {
                    throw new FrameworkException(EnoviaResourceBundle.getFrameworkStringResourceProperty(context, "emxFramework.Beans.InboxTask.DelegationNotAllowed", context.getLocale()));
                } catch (FrameworkException ex) {
                    logger.error("Error in delegateTask: ", ex);
                    String strMessage = EnoviaResourceBundle.getFrameworkStringResourceProperty(context, "emxFramework.Beans.InboxTask.DelegationNotAllowed", context.getLocale());
                    ex.addMessage(strMessage);
                    throw ex;
                }

            }

            String strTaskAssigneeID = (String) mpReturnInfo.get(SELECT_TASK_ASSIGNEE_ID);

            String strAttrAbsenceDelegateSelect = "attribute[" + TigerConstants.ATTRIBUTE_ABSENCEDELEGATE + "]";
            String strAttrAbsenceStartDateSelect = "attribute[" + TigerConstants.ATTRIBUTE_ABSENCESTARTDATE + "]";
            String strAttrAbsendDateSelect = "attribute[" + TigerConstants.ATTRIBUTE_ABSENCEENDDATE + "]";
            String strScheduleCompltionDateSelect = "attribute[" + DomainObject.ATTRIBUTE_SCHEDULED_COMPLETION_DATE + "]";

            String strScheduleCompltionDate = inboxTaskObj.getInfo(context, strScheduleCompltionDateSelect);
            Object ObjScheduleCompltionDate = null;
            Date localDate = new Date();
            if ((strScheduleCompltionDate != null) && (!("".equals(strScheduleCompltionDate.trim())))) {
                strScheduleCompltionDate = strScheduleCompltionDate.trim();
                ObjScheduleCompltionDate = eMatrixDateFormat.getJavaDate(strScheduleCompltionDate);
            } else {
                ObjScheduleCompltionDate = localDate;
            }

            DomainObject domObject = DomainObject.newInstance(context, paramString);
            StringList slSelectList = new StringList();
            slSelectList.add(strAttrAbsenceDelegateSelect);
            slSelectList.add(strAttrAbsenceStartDateSelect);
            slSelectList.add(strAttrAbsendDateSelect);
            Map mpInfo = domObject.getInfo(context, slSelectList);

            String strAttrAbsenceDelegate = (String) mpInfo.get(strAttrAbsenceDelegateSelect);
            String strAttrAbsenceStartDate = (String) mpInfo.get(strAttrAbsenceStartDateSelect);
            String strAttrAbsendDate = (String) mpInfo.get(strAttrAbsendDateSelect);
            String strPersonObjId = null;
            Date dateAbsenceStartDate = null;
            Date dateAbsendDate = null;

            if ((strAttrAbsenceDelegate != null) && (!("".equals(strAttrAbsenceDelegate.trim())))) {
                strAttrAbsenceDelegate = strAttrAbsenceDelegate.trim();
                strPersonObjId = PersonUtil.getPersonObjectID(context, strAttrAbsenceDelegate);
            }

            if ((strAttrAbsenceStartDate != null) && (!("".equals(strAttrAbsenceStartDate.trim())))) {
                strAttrAbsenceStartDate = strAttrAbsenceStartDate.trim();
                dateAbsenceStartDate = eMatrixDateFormat.getJavaDate(strAttrAbsenceStartDate);
            }

            if ((strAttrAbsendDate != null) && (!("".equals(strAttrAbsendDate.trim())))) {
                strAttrAbsendDate = strAttrAbsendDate.trim();
                dateAbsendDate = eMatrixDateFormat.getJavaDate(strAttrAbsendDate);
            }

            if ((strPersonObjId != null) && (dateAbsenceStartDate != null) && (dateAbsendDate != null)
                    && (((((Date) ObjScheduleCompltionDate).equals(dateAbsenceStartDate)) || (((Date) ObjScheduleCompltionDate).after(dateAbsenceStartDate))))
                    && (((((Date) ObjScheduleCompltionDate).before(dateAbsendDate)) || (((Date) ObjScheduleCompltionDate).equals(dateAbsendDate))))) {
                paramString = strPersonObjId;
            }

            if (strTaskAssigneeID.equals(paramString)) {
                try {
                    throw new FrameworkException(EnoviaResourceBundle.getFrameworkStringResourceProperty(context, "emxFramework.Beans.InboxTask.InvalidNewOwner", context.getLocale()));
                } catch (FrameworkException e) {
                    logger.error("Error in delegateTask: ", e);
                    String strMessage = EnoviaResourceBundle.getFrameworkStringResourceProperty(context, "emxFramework.Beans.InboxTask.InvalidNewOwner", context.getLocale());
                    e.addMessage(strMessage);
                    throw e;
                }

            }

            String strTaskAssigneeName = (String) mpReturnInfo.get(SELECT_TASK_ASSIGNEE_NAME);

            DomainObject domObjTaskAssignee = new DomainObject();
            domObjTaskAssignee.setId(strTaskAssigneeID);

            DomainObject domObject1 = new DomainObject();
            domObject1.setId(paramString);
            String strName = domObject1.getInfo(context, "name");

            String strRouteNodeId = (String) mpReturnInfo.get(SELECT_TASK_ASSIGNEE_CONNECTION);

            DomainRelationship.setToObject(context, strRouteNodeId, domObject1);

            strRouteNodeId = (String) mpReturnInfo.get(SELECT_ROUTE_NODE_ID);

            String strRouteObjectId = (String) mpReturnInfo.get(SELECT_ROUTE_ID);
            Route localRoute = (Route) DomainObject.newInstance(context, strRouteObjectId);

            strRouteNodeId = localRoute.getRouteNodeRelId(context, strRouteNodeId);
            try {
                ContextUtil.pushContext(context);

                DomainRelationship.setToObject(context, strRouteNodeId, domObject1);

                inboxTaskObj.setOwner(context, strName);
            } catch (Exception ex) {
                logger.error("Error in delegateTask: ", ex);
                throw ex;
            } finally {
                ContextUtil.popContext(context);
            }

            DomainObject domObjRoute = new DomainObject();
            domObjRoute.setId(strRouteObjectId);

            Route.grantAccessToNewAssignee(context, domObjRoute, strTaskAssigneeName, strName);

            ContextUtil.commitTransaction(context);
        } catch (RuntimeException localException1) {
            ContextUtil.abortTransaction(context);
            logger.error("Error in delegateTask: ", localException1);
            throw localException1;
        } catch (Exception e) {
            logger.error("Error in  PSS_emxInboxTaskBase_mxJPO.java:delegateTask: ", e);
        }
    }

    /**
     * showType - shows the due date for the task
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds the following input arguments: 0 - objectList MapList
     * @returns Object of type Vector
     * @throws Exception
     *             if the operation fails
     * @since AEF Rossini
     * @grade 0
     */
    public Vector showDueDate(Context context, String[] args) throws Exception {
        Vector showDueDate = new Vector();
        boolean bDisplayTime = false;

        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");
            HashMap paramList = (HashMap) programMap.get("paramList");

            String dueDate = "";
            String dueDateOffset = "";
            String dueDateOffsetFrom = "";

            Iterator objectListItr = objectList.iterator();
            while (objectListItr.hasNext()) {
                dueDate = "";

                Map objectMap = (Map) objectListItr.next();
                String taskDueDate = "";
                String sTypeName = (String) objectMap.get(DomainObject.SELECT_TYPE);

                if ((DomainObject.TYPE_INBOX_TASK).equalsIgnoreCase(sTypeName)) {
                    taskDueDate = (String) objectMap.get(strAttrCompletionDate);
                } else if ((DomainObject.TYPE_TASK).equalsIgnoreCase(sTypeName)) {
                    taskDueDate = (String) objectMap.get(strAttrTaskEstimatedFinishDate);
                } else if (TigerConstants.TYPE_WORK_FLOW_TASK.equalsIgnoreCase(sTypeName)) {
                    taskDueDate = (String) objectMap.get(strAttrworkFlowDueDate);
                } else if (DomainConstants.RELATIONSHIP_ROUTE_NODE.equals(sTypeName)) {
                    StringBuffer sb = new StringBuffer();
                    taskDueDate = (String) objectMap.get(strAttrCompletionDate);
                    dueDateOffset = (String) objectMap.get(getAttributeSelect(DomainObject.ATTRIBUTE_DUEDATE_OFFSET));
                    dueDateOffsetFrom = (String) objectMap.get(getAttributeSelect(DomainObject.ATTRIBUTE_DATE_OFFSET_FROM));
                    boolean bDueDateEmpty = UIUtil.isNullOrEmpty(taskDueDate) ? true : false;
                    boolean bDeltaDueDate = (!UIUtil.isNullOrEmpty(dueDateOffset) && bDueDateEmpty) ? true : false;

                    if (!bDeltaDueDate) {
                        sb.append(taskDueDate);
                        sb.append(" ");
                    } else {

                        sb.append(dueDateOffset);
                        sb.append(" ");
                        sb.append(EnoviaResourceBundle.getProperty(context, "emxComponentsStringResource", context.getLocale(), "emxComponents.common.DaysFrom"));
                        sb.append(" ");
                        sb.append(i18nNow.getRangeI18NString(DomainObject.ATTRIBUTE_DATE_OFFSET_FROM, dueDateOffsetFrom, context.getLocale().getLanguage()));
                    }
                    taskDueDate = sb.toString();
                }
                // added for BR 029: TIGTK-7588 : PCM : TS :Phase-2.0:Start
                else if (TigerConstants.TYPE_PSS_ROLEASSESSMENT.equals(sTypeName)) {
                    String strRAObjId = (String) objectMap.get(DomainConstants.SELECT_ID);
                    DomainObject domObj = DomainObject.newInstance(context, strRAObjId);
                    taskDueDate = (String) domObj.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT + "].attribute[" + TigerConstants.ATTRIBUTE_PSS_DUEDATE + "]");
                }
                // added for BR 029: TIGTK-7588 : PCM : TS :Phase-2.0:Start

                // Below date conversion is not required since config table column settings does the conversion
                /*
                 * if (taskDueDate != null && taskDueDate.length() > 0) { DateFormat df = null; if (bDisplayTime) { df = DateFormat.getDateTimeInstance(iDateFormat, iDateFormat, locale); } else { df =
                 * DateFormat.getDateInstance(iDateFormat, locale); }
                 * 
                 * dueDate = df.format(eMatrixDateFormat.getJavaDate(taskDueDate)); } showDueDate.add(dueDate);
                 */

                HashMap cellMap = new HashMap();
                cellMap.put("ActualValue", taskDueDate);

                Locale locale = context.getLocale();
                String timeZone = (String) (paramList != null ? paramList.get("timeZone") : programMap.get("timeZone"));
                double clientTZOffset = Double.valueOf(timeZone);

                if (!UIUtil.isNullOrEmpty(taskDueDate)) {
                    try {
                        taskDueDate = eMatrixDateFormat.getFormattedDisplayDateTime(taskDueDate, clientTZOffset, locale);
                    } catch (Exception dateException) {
                        // do nothing,This exception is added to avoid formatting of taskduedate if the value is not of type date i.e for ex: 4 days after Route start Date
                        logger.error("Error in showDueDate :: ", dateException);
                    }
                }
                cellMap.put("DisplayValue", taskDueDate);
                showDueDate.add(cellMap);

            }
        } catch (Exception ex) {
            logger.error("Error in showDueDate :: ", ex);
            throw ex;
        }
        return showDueDate;
    }

    /**
     * To get the Approval Status
     * @param context
     * @param args
     * @return StringList
     * @throws Exception
     * @since V6R2015x
     */
    public StringList getApprovalStatusInfo(Context context, String[] args) throws Exception {

        final String STRING_COMPLETED = EnoviaResourceBundle.getFrameworkStringResourceProperty(context, "emxFramework.LifecycleTasks.Completed", context.getLocale());
        final String STRING_APPROVED = EnoviaResourceBundle.getFrameworkStringResourceProperty(context, "emxFramework.Lifecycle.Approved", context.getLocale());
        final String STRING_REJECTED = EnoviaResourceBundle.getFrameworkStringResourceProperty(context, "emxFramework.Lifecycle.Rejected", context.getLocale());
        final String STRING_ABSTAINED = EnoviaResourceBundle.getFrameworkStringResourceProperty(context, "emxFramework.Lifecycle.Abstained", context.getLocale());
        final String STRING_NEEDS_REVIEW = EnoviaResourceBundle.getFrameworkStringResourceProperty(context, "emxFramework.Lifecycle.NeedsReview", context.getLocale());
        final String STRING_AWAITING_APPROVAL = EnoviaResourceBundle.getFrameworkStringResourceProperty(context, "emxFramework.LifecycleTasks.AwaitingApproval", context.getLocale());
        final String STRING_ROUTE_STOPPED = EnoviaResourceBundle.getFrameworkStringResourceProperty(context, "emxFramework.LifecycleTasks.RouteStopped", context.getLocale());

        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        MapList relBusObjPageList = (MapList) programMap.get("objectList");
        StringList slLinks = new StringList(relBusObjPageList.size());
        String sTaskLink = "";
        for (int i = 0; i < relBusObjPageList.size(); i++) {
            Map collMap = (Map) relBusObjPageList.get(i);
            String sTaskType = (String) collMap.get(DomainObject.SELECT_TYPE);
            String strCurrentState = (String) collMap.get(DomainObject.SELECT_CURRENT);
            String strApprovalStatus = (String) collMap.get(strAttrTaskApprovalStatus);
            String strRouteStatus = (String) collMap.get(routeApprovalStatusSelectStr);
            if (DomainConstants.TYPE_INBOX_TASK.equals(sTaskType)) {
                if (DomainConstants.STATE_INBOX_TASK_COMPLETE.equalsIgnoreCase(strCurrentState)) {
                    if ("Approve".equals(strApprovalStatus)) {
                        slLinks.add(STRING_APPROVED);
                    } else if ("Reject".equals(strApprovalStatus)) {
                        slLinks.add(STRING_REJECTED);
                    } else if ("Abstain".equals(strApprovalStatus)) {
                        slLinks.add(STRING_ABSTAINED);
                    } else {
                        slLinks.add(STRING_COMPLETED);
                    }
                } else if (DomainConstants.STATE_INBOX_TASK_REVIEW.equals(strCurrentState)) {
                    slLinks.add(STRING_NEEDS_REVIEW);

                } else {
                    if ("Stopped".equals(strRouteStatus)) {
                        slLinks.add(STRING_ROUTE_STOPPED);
                    } else {
                        slLinks.add(STRING_AWAITING_APPROVAL);
                    }
                }
            }
            // added for BR 029: TIGTK-7588 : PCM : TS :Phase-2.0:Start
            else if (TigerConstants.TYPE_PSS_ROLEASSESSMENT.equals(sTaskType)) {
                String strRAObjId = (String) collMap.get(DomainConstants.SELECT_ID);
                DomainObject domObj = DomainObject.newInstance(context, strRAObjId);
                String strDecsison = (String) domObj.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT + "].attribute[" + TigerConstants.ATTRIBUTE_PSS_DECISION + "]");
                if (TigerConstants.ATTRIBUTE_PSS_DECISION_RANGE_ABSTAIN.equals(strDecsison)) {
                    slLinks.add(TigerConstants.ATTRIBUTE_PSS_DECISION_RANGE_ABSTAIN);
                } else if (TigerConstants.ATTRIBUTE_PSS_DECISION_RANGE_NOGO.equals(strDecsison)) {
                    slLinks.add(TigerConstants.ATTRIBUTE_PSS_DECISION_RANGE_NOGO);
                } else if (TigerConstants.ATTRIBUTE_PSS_DECISION_RANGE_GO.equals(strDecsison)) {
                    slLinks.add(TigerConstants.ATTRIBUTE_PSS_DECISION_RANGE_GO);
                } else if (TigerConstants.ATTRIBUTE_PSS_DECISION_RANGE_NODECISION.equalsIgnoreCase(strDecsison)) {
                    slLinks.add(TigerConstants.ATTRIBUTE_PSS_DECISION_RANGE_NODECISION);
                }
            }
            // added for BR 029: TIGTK-7588 : PCM : TS :Phase-2.0:End
            else {
                slLinks.add(DomainConstants.EMPTY_STRING);
            }
        }
        return slLinks;
    }

    /**
     * getMyDeskTasks - gets the list of Tasks the user has access
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds the following input arguments: 0 - objectList MapList
     * @returns Object
     * @throws Exception
     *             if the operation fails
     * @since AEF Rossini
     * @grade 0
     */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public Object getMyDeskTasks(Context context, String[] args) throws Exception {

        try {
            DomainObject taskObject = DomainObject.newInstance(context);
            DomainObject boPerson = PersonUtil.getPersonObject(context);
            String PROJECT_POLICY = "to[" + DomainConstants.RELATIONSHIP_PROJECT_ACCESS_KEY + "].from.from[" + DomainConstants.RELATIONSHIP_PROJECT_ACCESS_LIST + "].to.policy";
            StringList selectTypeStmts = new StringList();
            StringList selectRelStmts = new StringList();
            selectTypeStmts.add(taskObject.SELECT_NAME);
            selectTypeStmts.add(taskObject.SELECT_ID);
            selectTypeStmts.add(DomainConstants.SELECT_DESCRIPTION);
            selectTypeStmts.add(taskObject.SELECT_OWNER);
            selectTypeStmts.add(taskObject.SELECT_CURRENT);
            selectTypeStmts.add(strAttrRouteAction);
            selectTypeStmts.add(strAttrCompletionDate);
            selectTypeStmts.add(strAttrTaskCompletionDate);
            selectTypeStmts.add(strAttrTaskApprovalStatus);
            selectTypeStmts.add(getAttributeSelect(DomainObject.ATTRIBUTE_ROUTE_ACTION));
            selectTypeStmts.add("attribute[" + DomainObject.ATTRIBUTE_ROUTE_INSTRUCTIONS + "]");
            selectTypeStmts.add(strAttrTitle);
            selectTypeStmts.add(objectIdSelectStr);
            selectTypeStmts.add(objectNameSelectStr);
            selectTypeStmts.add(routeIdSelectStr);
            selectTypeStmts.add(routeNameSelectStr);
            selectTypeStmts.add(routeOwnerSelectStr);
            selectTypeStmts.add(routeApprovalStatusSelectStr);

            selectTypeStmts.add(taskObject.SELECT_TYPE);
            selectTypeStmts.add(routeTypeSelectStr);
            selectTypeStmts.add(workflowIdSelectStr);
            selectTypeStmts.add(workflowNameSelectStr);
            selectTypeStmts.add(workflowTypeSelectStr);
            selectTypeStmts.add(strAttrworkFlowDueDate);
            selectTypeStmts.add(strAttrTaskEstimatedFinishDate);
            selectTypeStmts.add(strAttrworkFlowCompletinDate);
            selectTypeStmts.add(strAttrTaskFinishDate);
            selectTypeStmts.add(PROJECT_POLICY);
            /* selectTypeStmts.add(Route.SELECT_APPROVAL_STATUS); */
            String sPersonId = boPerson.getObjectId();

            Pattern relPattern = new Pattern(DomainConstants.RELATIONSHIP_PROJECT_TASK);
            relPattern.addPattern(DomainConstants.RELATIONSHIP_ASSIGNED_TASKS);
            relPattern.addPattern(TigerConstants.RELATIONSHIP_WORKFLOW_TASK_ASSIGNEE);

            Pattern typePattern = new Pattern(DomainConstants.TYPE_INBOX_TASK);
            typePattern.addPattern(DomainObject.TYPE_TASK);
            typePattern.addPattern(TigerConstants.TYPE_WORK_FLOW_TASK);
            typePattern.addPattern(DomainObject.TYPE_CHANGE_TASK);

            taskObject.setId(sPersonId);

            ContextUtil.startTransaction(context, false);
            ExpansionIterator expItr = taskObject.getExpansionIterator(context, relPattern.getPattern(), typePattern.getPattern(), selectTypeStmts, selectRelStmts, true, false, (short) 1, null, null,
                    (short) 0, false, false, (short) 100, false);

            com.matrixone.apps.domain.util.MapList taskMapList = null;
            try {
                taskMapList = FrameworkUtil.toMapList(expItr, (short) 0, null, null, null, null);
                selectTypeStmts.add("attribute[" + TigerConstants.ATTRIBUTE_REVIEW_COMMENT_NEEDED + "]");
                selectTypeStmts.add("from[" + DomainConstants.RELATIONSHIP_ROUTE_TASK + "].to.owner");
                Pattern relationshipPattern = new Pattern(DomainConstants.RELATIONSHIP_ROUTE_TASK);
                relationshipPattern.addPattern(DomainConstants.RELATIONSHIP_PROJECT_ROUTE);
                Pattern taskTypePattern = new Pattern(DomainConstants.TYPE_INBOX_TASK);
                taskTypePattern.addPattern(DomainConstants.TYPE_ROUTE);
                MapList mlReviewTasks = taskObject.getRelatedObjects(context, relationshipPattern.getPattern(), taskTypePattern.getPattern(), selectTypeStmts, null, true, false, (short) 2, null,
                        null);
                Iterator reviewTasksIterator = mlReviewTasks.iterator();
                while (reviewTasksIterator.hasNext()) {
                    Map reviewTasksDetails = (Map) reviewTasksIterator.next();
                    if (DomainConstants.TYPE_INBOX_TASK.equals(reviewTasksDetails.get(DomainObject.SELECT_TYPE))) {
                        if (DomainConstants.STATE_INBOX_TASK_REVIEW.equals(reviewTasksDetails.get(DomainObject.SELECT_CURRENT))
                                && "Yes".equals(reviewTasksDetails.get("attribute[" + TigerConstants.ATTRIBUTE_REVIEW_COMMENT_NEEDED + "]"))
                                && context.getUser().equals(reviewTasksDetails.get("from[" + DomainConstants.RELATIONSHIP_ROUTE_TASK + "].to.owner"))) {
                            taskMapList.add(reviewTasksDetails);
                        }
                    }
                }
            } finally {
                expItr.close();
            }
            ContextUtil.commitTransaction(context);

            // Added for 318463
            // Get the context (top parent) object for WBS Tasks to dispaly appropriate tree for WBS Tasks
            MQLCommand mql = new MQLCommand();
            String sTaskType = "";
            String sTaskId = "";
            String sMql = "";
            boolean bResult = false;
            String sResult = "";
            StringTokenizer sResultTkz = null;
            MapList finalTaskMapList = new MapList();
            Iterator objectListItr = taskMapList.iterator();
            while (objectListItr.hasNext()) {
                Map objectMap = (Map) objectListItr.next();
                sTaskType = (String) objectMap.get(DomainObject.SELECT_TYPE);
                String policy = (String) objectMap.get(PROJECT_POLICY);
                if ("Project Space Hold Cancel".equalsIgnoreCase(policy)) {
                    continue;
                }
                // if Task is WBS then add the context (top) object information
                if ((DomainObject.TYPE_TASK).equalsIgnoreCase(sTaskType)) {
                    sTaskId = (String) objectMap.get(taskObject.SELECT_ID);
                    sMql = "expand bus " + sTaskId + " to rel " + DomainConstants.RELATIONSHIP_SUBTASK + " recurse to 1 select bus id dump |";
                    bResult = mql.executeCommand(context, sMql);
                    if (bResult) {
                        sResult = mql.getResult().trim();
                        // Bug 318325. Added if condition to check sResult object as not null and not empty.
                        if (UIUtil.isNotNullAndNotEmpty(sResult)) {
                            sResultTkz = new StringTokenizer(sResult, "|");
                            sResultTkz.nextToken();
                            sResultTkz.nextToken();
                            sResultTkz.nextToken();
                            objectMap.put("Context Object Type", (String) sResultTkz.nextToken());
                            objectMap.put("Context Object Name", (String) sResultTkz.nextToken());
                            sResultTkz.nextToken();
                            objectMap.put("Context Object Id", (String) sResultTkz.nextToken());
                        }
                    }
                }
                finalTaskMapList.add(objectMap);
            }
            // added for BR 052 :PCM:PKH:Phase-2.0:Start
            MapList mlTaskList = new MapList();
            MapList mlTaskTOBeAccepected = (MapList) this.getTasksToBeAccepted(context, args);
            Iterator itrTask = mlTaskTOBeAccepected.iterator();
            while (itrTask.hasNext()) {
                Map mTask = (Map) itrTask.next();
                mTask.put(DomainConstants.SELECT_LEVEL, "1");
                mlTaskList.add(mTask);
            }
            if (mlTaskList.size() > 0) {
                finalTaskMapList.addAll(mlTaskList);
            }

            // added for BR 052 :PCM:PKH:Phase-2.0:End

            // added for BR 029: TIGTK-7588 : PCM : TS :Phase-2.0:Start
            MapList mlRATaskList = new MapList();
            pss.ecm.impactanalysis.ImpactAnalysisUtil_mxJPO impactAnalysisUtilObj = new pss.ecm.impactanalysis.ImpactAnalysisUtil_mxJPO();
            MapList mlRoleAssessment = (MapList) impactAnalysisUtilObj.getRoleAssessments(context, args);
            Iterator itrRATask = mlRoleAssessment.iterator();
            while (itrRATask.hasNext()) {
                Map mTask = (Map) itrRATask.next();
                mTask.put(DomainConstants.SELECT_LEVEL, "1");
                mlRATaskList.add(mTask);
            }
            if (mlRATaskList.size() > 0) {
                finalTaskMapList.addAll(mlRATaskList);
            }
            // added for BR 029: TIGTK-7588 : PCM : TS :Phase-2.0:Start
            return finalTaskMapList;
        }

        catch (Exception ex) {
            logger.error("Error in getMyDeskTasks :: ", ex);
            throw ex;
        }
    }

    // PCM TIGTK-9994: 29/09/2017 : KWagh : START
    /**
     * Access Program to display the Task Abstain link
     * @param context
     *            Object
     * @param args
     *            String array
     * @throws Exception
     */

    public boolean displayAbstainLink(Context context, String[] args) throws Exception {

        Boolean bShowAbstian = false;
        HashMap detailsMap = getInboxTaskPropertiesAccessCommands(context, args);
        Boolean bIsAbstain = (Boolean) detailsMap.get("APPAbstainTask");

        Map programMap = (Map) JPO.unpackArgs(args);

        String objectId = (String) programMap.get("objectId");

        String strSelectChangeObjectID = "from[" + DomainConstants.RELATIONSHIP_ROUTE_TASK + "].to.to[" + DomainConstants.RELATIONSHIP_OBJECT_ROUTE + "].from.id";

        DomainObject taskObj = DomainObject.newInstance(context, objectId);
        String strChangeObjectID = taskObj.getInfo(context, strSelectChangeObjectID);

        if (UIUtil.isNotNullAndNotEmpty(strChangeObjectID)) {
            DomainObject domChange = DomainObject.newInstance(context, strChangeObjectID);
            String strType = domChange.getInfo(context, DomainConstants.SELECT_TYPE);
            String strCurrent = domChange.getInfo(context, DomainConstants.SELECT_CURRENT);
            if (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_CHANGEREQUEST) && (strCurrent.equalsIgnoreCase(TigerConstants.STATE_INREVIEW_CR))) {

                bShowAbstian = true;
            } else if ((strType.equalsIgnoreCase(ChangeConstants.TYPE_CHANGE_ACTION)) && (strCurrent.equalsIgnoreCase(TigerConstants.STATE_CHANGEACTION_INAPPROVAL))) {

                bShowAbstian = true;

            } else if ((strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_CHANGENOTICE)) && (strCurrent.equalsIgnoreCase(TigerConstants.STATE_INREVIEW_CN))) {

                bShowAbstian = true;

            } else if ((strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_ISSUE) && (strCurrent.equalsIgnoreCase(TigerConstants.STATE_PSS_ISSUE_REVIEW)))) {

                bShowAbstian = true;

            } else if ((strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION) && (strCurrent.equalsIgnoreCase(TigerConstants.STATE_PSS_MCA_INREVIEW)))) {

                bShowAbstian = true;

            }

            if (bShowAbstian) {
                bIsAbstain = false;
            }

        }

        return bIsAbstain;

    }

    // PCM TIGTK-9994: 29/09/2017 : KWagh : END

    // PCM TIGTK-9994: 29/09/2017 : KWagh : START
    /**
     * Range Values for Appproval Status in Inbox Task form
     * @param context
     * @param args
     * @return
     * @throws FrameworkException
     */

    public Map getTaskApprovalStatusOptions(Context context, String[] args) throws FrameworkException {

        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            Map requestMap = (Map) programMap.get("requestMap");
            Map paramMap = (Map) programMap.get("paramMap");
            String sLanguage = (String) paramMap.get("languageStr");
            String objectId = (String) requestMap.get("objectId");

            Map returnMap = new HashMap(2);
            StringList rangeDisplay = new StringList(3);
            StringList rangeActual = new StringList(3);

            String strSelectChangeObjectID = "from[" + DomainConstants.RELATIONSHIP_ROUTE_TASK + "].to.to[" + DomainConstants.RELATIONSHIP_OBJECT_ROUTE + "].from.id";
            boolean bhideAbstian = false;
            DomainObject taskObj = DomainObject.newInstance(context, objectId);
            String strChangeObjectID = taskObj.getInfo(context, strSelectChangeObjectID);

            if (UIUtil.isNotNullAndNotEmpty(strChangeObjectID)) {
                DomainObject domChange = DomainObject.newInstance(context, strChangeObjectID);
                String strType = domChange.getInfo(context, DomainConstants.SELECT_TYPE);
                String strCurrent = domChange.getInfo(context, DomainConstants.SELECT_CURRENT);
                if (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_CHANGEREQUEST) && (strCurrent.equalsIgnoreCase(TigerConstants.STATE_INREVIEW_CR))) {

                    bhideAbstian = true;
                } else if ((strType.equalsIgnoreCase(ChangeConstants.TYPE_CHANGE_ACTION)) && (strCurrent.equalsIgnoreCase(TigerConstants.STATE_CHANGEACTION_INAPPROVAL))) {

                    bhideAbstian = true;

                } else if ((strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_CHANGENOTICE)) && (strCurrent.equalsIgnoreCase(TigerConstants.STATE_INREVIEW_CN))) {

                    bhideAbstian = true;

                } else if ((strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_ISSUE) && (strCurrent.equalsIgnoreCase(TigerConstants.STATE_PSS_ISSUE_REVIEW)))) {

                    bhideAbstian = true;

                } else if ((strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION) && (strCurrent.equalsIgnoreCase(TigerConstants.STATE_PSS_MCA_INREVIEW)))) {

                    bhideAbstian = true;

                }
            }
            StringList selects = new StringList();
            selects.addElement("attribute[" + sAttrReviewTask + "]");
            selects.addElement("attribute[" + sAttrReviewersComments + "]");

            // get the details required
            Map taskMap = taskObj.getInfo(context, selects);
            String reviewTask = (String) taskMap.get("attribute[" + sAttrReviewTask + "]");
            String reviewComments = (String) taskMap.get("attribute[" + sAttrReviewersComments + "]");

            if ("Yes".equals(reviewTask) && UIUtil.isNotNullAndNotEmpty(reviewComments)) {

                String promote = (String) EnoviaResourceBundle.getFrameworkStringResourceProperty(context, "emxFramework.Lifecycle.Promote", context.getLocale());
                String demote = (String) EnoviaResourceBundle.getFrameworkStringResourceProperty(context, "emxFramework.Lifecycle.Demote", context.getLocale());
                rangeActual.addElement("promote");
                rangeDisplay.addElement(promote);
                rangeActual.addElement("demote");
                rangeDisplay.addElement(demote);
                returnMap.put("field_choices", rangeActual);
                returnMap.put("field_display_choices", rangeDisplay);

            } else {
                matrix.db.AttributeType attribName = new matrix.db.AttributeType(DomainConstants.ATTRIBUTE_APPROVAL_STATUS);
                attribName.open(context);
                // actual range values
                List attributeRange = attribName.getChoices();

                attribName.close(context);
                List attributeDisplayRange = i18nNow.getAttrRangeI18NStringList(DomainConstants.ATTRIBUTE_APPROVAL_STATUS, (StringList) attributeRange, sLanguage);
                attributeDisplayRange.remove(attributeRange.indexOf("Ignore"));
                attributeRange.remove("Ignore");
                attributeDisplayRange.remove(attributeRange.indexOf("Signature Reset"));
                attributeRange.remove("Signature Reset");
                attributeDisplayRange.remove(attributeRange.indexOf("None"));
                attributeRange.remove("None");

                if (bhideAbstian) {
                    attributeDisplayRange.remove(attributeRange.indexOf("Abstain"));
                    attributeRange.remove("Abstain");
                }

                rangeActual.addAll(attributeRange);
                rangeDisplay.addAll(attributeDisplayRange);

                returnMap.put("field_choices", rangeActual);
                returnMap.put("field_display_choices", rangeDisplay);
            }
            return returnMap;

        } catch (Exception e) {
            throw new FrameworkException(e);
        }
    }

    // PCM TIGTK-9994: 29/09/2017 : KWagh : END

    // PCM TIGTK-10286: 3/10/2017 : KWagh : START
    /**
     * To get the quick task complete link
     * @param context
     * @param args
     * @return StringList
     * @throws Exception
     * @since V6R2015x
     */
    public StringList getQuickTaskCompleteLink(Context context, String[] args) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        MapList relBusObjPageList = (MapList) programMap.get("objectList");
        StringList slLinks = new StringList(relBusObjPageList.size());
        String sTaskLink = "";
        for (int i = 0; i < relBusObjPageList.size(); i++) {
            Map collMap = (Map) relBusObjPageList.get(i);
            String sTaskType = (String) collMap.get(DomainObject.SELECT_TYPE);
            String sTaskId = (String) collMap.get(DomainObject.SELECT_ID);
            String routeTaskAction = (String) collMap.get("attribute[" + DomainConstants.ATTRIBUTE_ROUTE_ACTION + "]");
            if (sTypeInboxTask.equals(sTaskType) && routeTaskAction.equalsIgnoreCase("Approve")) {
                sTaskLink = "javascript:emxTableColumnLinkClick('../components/emxTaskCompletePreProcess.jsp?action=Approve"
                        + "&amp;summaryPage=true&amp;calledFromTaskDashBoard=true&amp;emxSuiteDirectory=components&amp;suiteKey=Components&amp;objectId=" + sTaskId
                        + "', null, null, 'false', 'listHidden', '', null, 'true')";
            } else if (sTypeInboxTask.equals(sTaskType) && (routeTaskAction.equalsIgnoreCase("Comment") || routeTaskAction.equalsIgnoreCase("Notify Only"))) {
                sTaskLink = "javascript:emxTableColumnLinkClick('../components/emxTaskCompletePreProcess.jsp?action=Complete&amp;summaryPage=true&amp;emxSuiteDirectory=components&amp;suiteKey=Components&amp;objectId="
                        + sTaskId + "', null, null, 'false', 'listHidden', '', null, 'true')";
            } else {
                sTaskLink = "javascript:emxTableColumnLinkClick('../components/emxUserTasksSummaryLinksProcess.jsp?fromPage=Complete&amp;emxSuiteDirectory=components&amp;suiteKey=Components&amp;emxTableRowId="
                        + sTaskId + "', null, null, 'false', 'listHidden', '', null, 'true')";
            }
            sTaskLink = "<a href=\"" + sTaskLink + "\">" + "<img src=\"" + "../common/images/buttonDialogDone.gif" + "\" width=\"16px\" height=\"16px\"/>" + "</a>";
            slLinks.add(sTaskLink);
        }

        return slLinks;

    }
    // PCM TIGTK-10286: 3/10/2017 : KWagh : End
}
