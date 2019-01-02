
/*
 * * PSS_emxCommonCompleteTask was cloned from emxCommonCompleteTaskBase.java** All Rights Reserved. This program contains proprietary and trade secret information of MatrixOne, Inc. Copyright notice
 * is precautionary only and does not evidence any actual or intended publication of such program*
 */

import pss.constants.TigerConstants;
import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.Person;
import matrix.db.Signature;
import matrix.db.SignatureList;
import matrix.util.Pattern;
import matrix.util.SelectList;
import matrix.util.StringList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import com.matrixone.apps.framework.ui.UIUtil;
import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeConstants;
import com.matrixone.apps.common.util.ComponentsUtil;
import com.matrixone.apps.framework.ui.UIMenu;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MailUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;

public class PSS_emxCommonCompleteTask_mxJPO {

    // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PSS_emxCommonCompleteTask_mxJPO.class);

    // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - END

    private static final String sAttCurrentRouteNode = PropertyUtil.getSchemaProperty("attribute_CurrentRouteNode");

    private static final String sAttRouteCompletionAction = PropertyUtil.getSchemaProperty("attribute_RouteCompletionAction");

    private static final String sAttParallelNodeProcessionRule = PropertyUtil.getSchemaProperty("attribute_ParallelNodeProcessionRule");

    private static final String sRouteDelegationGrantor = PropertyUtil.getSchemaProperty("person_RouteDelegationGrantor");

    private static final String sAttAutoStopOnRejection = PropertyUtil.getSchemaProperty("attribute_AutoStopOnRejection");

    private static final String RELASHIONSHIP_CHANGE_COORDINATOR = PropertyUtil.getSchemaProperty("relationship_ChangeCoordinator");

    // Modified for Find bug issue by PTE

    private static emxMailUtil_mxJPO mailUtil = null;

    protected static emxSubscriptionManager_mxJPO subscriptionManager = null;

    // Modified for Find bug issue by PTE

    private static emxCommonInitiateRoute_mxJPO initiateRoute = null;

    // Modified for Find bug issue by PTE

    private static emxcommonPushPopShadowAgentBase_mxJPO shadowAgent = null;

    /**
     * Constructor.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @throws Exception
     *             if the operation fails
     * @since AEF 9.5.0.0
     * @grade 0
     */
    public PSS_emxCommonCompleteTask_mxJPO(Context context, String[] args) throws Exception {
    }

    /**
     * This method is executed if a specific method is not specified.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @returns nothing
     * @throws Exception
     *             if the operation fails
     * @since AEF 9.5.0.0
     */
    public int mxMain(Context context, String[] args) throws Exception {
        if (true) {
            throw new Exception(ComponentsUtil.i18nStringNow("emxComponents.CommonCompleteTask.SpecifyMethodOnServiceCommonCompleteTaskInvocation", context.getLocale().getLanguage()));
        }
        return 0;
    }

    /**
     * emxServicecommonCompleteTask method to remove the proicess from the tcl The method is supposed to be invoked on completion of any task of route. The high level operations performed by this
     * method are:- If the task is rejected, and the attribute Auto Stop On Rejection is Immediate, then the route will be stops. If the task is rejected, and the attribute Auto Stop On Rejection is
     * Deferred, then the route will be stopped on completion of all the task on this level. If the task is completed, and there are no more tasks on the level, then decides if the route is to be
     * stopped or finished. It is stopped if one of the tasks on this level is rejected and Auto Stop On Rejection was set to Deferred. If all the tasks on this level is completed, none of the task is
     * rejected, and there are more tasks in the route, then next level tasks are activated.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds the following input arguments:
     * @returns booelan
     * @throws Exception
     *             if the operation fails
     * @since AEF 10 minor 1
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static int completeTask(Context context, String args[]) throws Exception {
        try {
            // TGPSS_PCM-TS152 Change Request Notifications_V2.2 | 07/03/2017 |Harika Varanasi : Starts
            String strContextUser = context.getUser();
            // TGPSS_PCM-TS152 Change Request Notifications_V2.2 | 07/03/2017 |Harika Varanasi : Ends
            PropertyUtil.setGlobalRPEValue(context, "PSS_CAD_PROMOTE_USERNAME", context.getUser());

            shadowAgent = new emxcommonPushPopShadowAgentBase_mxJPO(context, null);
            shadowAgent.pushContext(context, null);

            String message = "";
            String strObjectType = "";

            String sObjectsNotSatisfied = "";
            String sRoutesInProcess = "";
            String sPromotedObjects = "";
            StringBuffer sBufObjectsNotSatisfied = new StringBuffer();
            StringBuffer sBufRoutesInProcess = new StringBuffer();
            StringBuffer sBufPromotedObjects = new StringBuffer();
            StringBuffer sBufInCompleteRoutes = new StringBuffer();
            String sInCompleteRoutes = "";
            // Get absolute names from symbolic names
            String sDate = MqlUtil.mqlCommand(context, "get env TIMESTAMP");

            // Initializing the jpos to be used
            mailUtil = new emxMailUtil_mxJPO(context, null);

            initiateRoute = new emxCommonInitiateRoute_mxJPO(context, null);

            // Getting the type name rev from teh argument's passed
            String sType = args[0];
            String sName = args[1];
            String sRev = args[2];

            // the below line is commented for the bug 319223
            // String bConsiderAdhocRoutes ="FALSE";
            // Get setting from emxSystem.properties file to
            // check if Ad Hoc routes should be considered or not

            // Bug 293332
            String arguments[] = new String[4];
            /*
             * arguments[0]= "emxFramework.AdHocRoutesBlockLifecycle"; arguments[1]= "0"; arguments[2] = ""; arguments[3]= "emxSystem"; String bConsiderAdhocRoutes
             * =mailUtil.getMessage(context,arguments); // set default to false if property doesn't exists bConsiderAdhocRoutes = bConsiderAdhocRoutes.toUpperCase(); if
             * (!bConsiderAdhocRoutes.equals("TRUE")) { bConsiderAdhocRoutes = "FALSE"; }
             */

            // Set Actual Completion Date attribute in Inbox Task
            BusinessObject bObject = new BusinessObject(sType, sName, sRev, "");
            bObject.open(context);
            String ObjectId = bObject.getObjectId();
            bObject.close(context);

            DomainObject inboxTask = new DomainObject(ObjectId);
            inboxTask.setAttributeValue(context, DomainConstants.ATTRIBUTE_ACTUAL_COMPLETION_DATE, sDate);

            // Copy 'Approval Status', 'Actual Completion Date', 'Comments' to Route Node relationship
            SelectList objectSelects = new SelectList();
            objectSelects.addElement("attribute[" + DomainConstants.ATTRIBUTE_ROUTE_NODE_ID + "]");
            objectSelects.addElement("attribute[" + DomainConstants.ATTRIBUTE_APPROVAL_STATUS + "]");
            objectSelects.addElement("attribute[" + DomainConstants.ATTRIBUTE_ACTUAL_COMPLETION_DATE + "]");
            objectSelects.addElement("attribute[" + DomainConstants.ATTRIBUTE_COMMENTS + "]");
            objectSelects.addElement("attribute[" + DomainConstants.ATTRIBUTE_ROUTE_ACTION + "]");
            // Added by Pooja Mantri for TS026 for PCM RFC 033
            objectSelects.addElement("from[Project Task].to.name");

            Map objectMap = inboxTask.getInfo(context, objectSelects);

            String sRouteNodeIDOnIB = (String) objectMap.get("attribute[" + DomainConstants.ATTRIBUTE_ROUTE_NODE_ID + "]");
            String sApprovalStatus = (String) objectMap.get("attribute[" + DomainConstants.ATTRIBUTE_APPROVAL_STATUS + "]");
            String sActualCompletionDate = (String) objectMap.get("attribute[" + DomainConstants.ATTRIBUTE_ACTUAL_COMPLETION_DATE + "]");
            String sComments = (String) objectMap.get("attribute[" + DomainConstants.ATTRIBUTE_COMMENTS + "]");
            String sRouteActionOfTask = (String) objectMap.get("attribute[" + DomainConstants.ATTRIBUTE_ROUTE_ACTION + "]");
            String sInboxTaskAssigneeName = (String) objectMap.get("from[Project Task].to.name");

            StringList lRouteNodeId = inboxTask.getInfoList(context, "from[" + DomainConstants.RELATIONSHIP_ROUTE_TASK + "].businessobject.from[" + DomainConstants.RELATIONSHIP_ROUTE_NODE + "].id");
            StringList lRouteNodeIdAttr = inboxTask.getInfoList(context, "from[" + DomainConstants.RELATIONSHIP_ROUTE_TASK + "].businessobject.from[" + DomainConstants.RELATIONSHIP_ROUTE_NODE
                    + "].attribute[" + DomainConstants.ATTRIBUTE_ROUTE_NODE_ID + "]");

            String sRouteNodeId = "";
            String sRouteNodeIdAttr = "";

            // Get matching relationship id
            int bRouteNodeIdFound = 0;
            // need to update this for loop

            for (int i = 0; i < lRouteNodeId.size(); i++) {

                sRouteNodeId = (String) lRouteNodeId.elementAt(i);

                sRouteNodeIdAttr = (String) lRouteNodeIdAttr.elementAt(i);
                if (sRouteNodeIDOnIB.equals(sRouteNodeIdAttr)) {
                    bRouteNodeIdFound = 1;
                    break;
                }
            }

            // If Route Node Id not found then
            // Error out
            if (bRouteNodeIdFound == 0) {
                String arguments1[] = new String[13];
                arguments1[0] = "emxFramework.ProgramObject.eServicecommonCompleteTask.InvalidRouteNodeId";
                arguments1[1] = "3";
                arguments1[2] = "type";
                arguments1[3] = sType;
                arguments1[4] = "name";
                arguments1[5] = sName;
                arguments1[6] = "rev";
                arguments1[7] = sRev;
                arguments1[8] = "";
                message = mailUtil.getMessage(context, arguments1);
                MqlUtil.mqlCommand(context, "notice " + message + "");
                return 1;
            }
            Map map = new HashMap();
            map.put(DomainConstants.ATTRIBUTE_APPROVAL_STATUS, sApprovalStatus);
            map.put(DomainConstants.ATTRIBUTE_ACTUAL_COMPLETION_DATE, sActualCompletionDate);
            map.put(DomainConstants.ATTRIBUTE_COMMENTS, sComments);
            DomainRelationship.setAttributeValues(context, sRouteNodeId, map);
            String relationshipIds[] = new String[1];
            relationshipIds[0] = sRouteNodeId;
            SelectList RelSelects = new SelectList();
            RelSelects.addElement("from.id");
            RelSelects.addElement("from.owner");
            RelSelects.addElement("from.type");
            RelSelects.addElement("from.name");
            RelSelects.addElement("from.revision");
            RelSelects.addElement("from.attribute[" + sAttCurrentRouteNode + "]");
            RelSelects.addElement("from.attribute[" + DomainConstants.ATTRIBUTE_ROUTE_STATUS + "]");
            RelSelects.addElement("from.attribute[" + sAttRouteCompletionAction + "]");
            RelSelects.addElement("to.name");
            RelSelects.addElement("to.attribute[" + DomainConstants.ATTRIBUTE_FIRST_NAME + "]");
            RelSelects.addElement("to.attribute[" + DomainConstants.ATTRIBUTE_LAST_NAME + "]");
            RelSelects.addElement("attribute[" + sAttParallelNodeProcessionRule + "]");
            RelSelects.addElement("from.to[" + DomainConstants.RELATIONSHIP_OBJECT_ROUTE + "].from.id");
            RelSelects.addElement("from.to[" + DomainConstants.RELATIONSHIP_OBJECT_ROUTE + "].from.current.satisfied");
            RelSelects.addElement("from.to[" + DomainConstants.RELATIONSHIP_OBJECT_ROUTE + "].from.type");
            RelSelects.addElement("from.to[" + DomainConstants.RELATIONSHIP_OBJECT_ROUTE + "].from.name");
            RelSelects.addElement("from.to[" + DomainConstants.RELATIONSHIP_OBJECT_ROUTE + "].from.revision");
            RelSelects.addElement("from.to[" + DomainConstants.RELATIONSHIP_OBJECT_ROUTE + "].from.current");
            RelSelects.addElement("from.to[" + DomainConstants.RELATIONSHIP_OBJECT_ROUTE + "].from.policy");

            MapList relMapList = DomainRelationship.getInfo(context, relationshipIds, RelSelects);
            // Get information on attached route

            Map relMap = (Map) relMapList.get(0);
            String sRouteId = (String) relMap.get("from.id");
            String sOwner = (String) relMap.get("from.owner");
            String sRouteType = (String) relMap.get("from.type");
            String sRouteName = (String) relMap.get("from.name");
            String sRouteRev = (String) relMap.get("from.revision");
            String sRouteStatus = (String) relMap.get("from.attribute[" + DomainConstants.ATTRIBUTE_ROUTE_STATUS + "]");
            String sRouteCompletionAction = (String) relMap.get("from.attribute[" + sAttRouteCompletionAction + "]");
            String sPerson = (String) relMap.get("to.name");
            // lvc
            String sFirstName = (String) relMap.get("to.attribute[" + DomainConstants.ATTRIBUTE_FIRST_NAME + "]");
            String sLastName = (String) relMap.get("to.attribute[" + DomainConstants.ATTRIBUTE_LAST_NAME + "]");
            String sProcessionRule = (String) relMap.get("attribute[" + sAttParallelNodeProcessionRule + "]");
            int sCurrentRouteNode = Integer.parseInt((String) relMap.get("from.attribute[" + sAttCurrentRouteNode + "]"));
            final String SELECT_TASK_ASSIGNEE_ID = "from[" + DomainObject.RELATIONSHIP_PROJECT_TASK + "].to.id";

            DomainObject Route = new DomainObject(sRouteId);
            // Addition for Tiger - PCM stream by SGS starts
            // Get The context Object type.

            final String RELATIONSHIP_PSS_CONNECTEDPCMDATA = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ConnectedPCMData");
            String strChangeObjectId = Route.getInfo(context, "to[" + DomainConstants.RELATIONSHIP_OBJECT_ROUTE + "].from.id");

            // PCM TIGTK-6772 | 18/04/17 :Pooja Mantri : Start
            Map mapChangeRequestInfo = null;
            DomainObject domChangeRequestObject = null;
            DomainObject domChangeActionObject = null;
            String strChangeCurrent = DomainConstants.EMPTY_STRING;

            if (UIUtil.isNotNullAndNotEmpty(strChangeObjectId)) {
                domChangeRequestObject = new DomainObject(strChangeObjectId);
                StringList lstselectList = new StringList();
                lstselectList.add(DomainConstants.SELECT_TYPE);
                lstselectList.add("from[" + RELASHIONSHIP_CHANGE_COORDINATOR + "].to.id");
                lstselectList.add(DomainConstants.SELECT_DESCRIPTION);
                lstselectList.add(DomainConstants.SELECT_NAME);
                lstselectList.add("to[" + RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.name");
                // Added for JIRA TIGTK-2599 -- Pooja Mantri
                lstselectList.add("to[" + RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
                lstselectList.add(DomainConstants.SELECT_CURRENT);
                // Added for JIRA TIGTK-2599 -- Pooja Mantri

                mapChangeRequestInfo = domChangeRequestObject.getInfo(context, lstselectList);

                // strObjectType = (String) domChangeRequestObject.getInfo(context, domChangeRequestObject.SELECT_TYPE);
                strObjectType = (String) mapChangeRequestInfo.get(DomainConstants.SELECT_TYPE);
                strChangeCurrent = (String) mapChangeRequestInfo.get(DomainConstants.SELECT_CURRENT);

                // Added for RFC-033 -- Reject Change Action Approval Task -- by SGS Starts
                // Creating "Change Action" object" Instance
                domChangeActionObject = new DomainObject(strChangeObjectId);

                // Get the type of "Change Action" object
                strObjectType = (String) domChangeActionObject.getInfo(context, DomainConstants.SELECT_TYPE);
                // Added for RFC-033 -- Reject Change Action Approval Task -- by SGS Ends
            }
            // PCM TIGTK-6772 | 18/04/17 :Pooja Mantri : End
            // Addition for Tiger - PCM stream by SGS ends
            // modified for the 327641 12/28/2006-- Begin
            objectSelects = new SelectList();
            objectSelects.addElement(DomainConstants.SELECT_ID);
            objectSelects.addElement("current");
            objectSelects.addElement("attribute[" + DomainConstants.ATTRIBUTE_ROUTE_NODE_ID + "]");
            objectSelects.addElement("owner");
            objectSelects.addElement(DomainConstants.SELECT_TYPE);
            objectSelects.addElement(DomainConstants.SELECT_NAME);
            objectSelects.addElement(DomainConstants.SELECT_REVISION);
            objectSelects.addElement(SELECT_TASK_ASSIGNEE_ID);

            SelectList relSelects = new SelectList();

            MapList ObjectsList = Route.getRelatedObjects(context, DomainConstants.RELATIONSHIP_ROUTE_TASK, "*", objectSelects, relSelects, true, false, (short) 0, null, null);
            // modified for the 327641 12/28/2006-- Ends

            // Start: Resume Process
            // Due to Resume Process algorithm, there can be some tasks which are connected to route but are not connected to person
            // These tasks should be removed from the ObjectsList.
            MapList mlFilteredObjectsList = new MapList();
            Map mapCurrentObjectInfo = null;
            for (Iterator itrObjectsList = ObjectsList.iterator(); itrObjectsList.hasNext();) {
                mapCurrentObjectInfo = (Map) itrObjectsList.next();

                if (mapCurrentObjectInfo.get(SELECT_TASK_ASSIGNEE_ID) != null) {
                    mlFilteredObjectsList.add(mapCurrentObjectInfo);
                }
            }
            ObjectsList = mlFilteredObjectsList;
            // End: Resume Process

            // Start: Auto-Stop
            // If this task is rejected then send the rejection notice

            // Addition for Tiger - PCM stream by SGS Starts
            String strLanguage = context.getSession().getLanguage();
            String strMsgTigerKey = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.Tiger");
            String strMsgRejectedTaskOnKey = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.hasRejectedTaskOn");
            String strMsgAbstainTaskOnKey = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "PSS_emxFramework.Message.AbstainTaskOn");
            String strMsgForStringKey = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.ForString");
            // Added by Pooja Mantri - TIGTK-2846
            String strMsgCRNumberKey = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.CRNumber");
            String strMsgUserKey = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.UserKey");
            String strMsgTaskCommentsKey = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.TaskCommentsKey");
            // Added by Pooja Mantri - TIGTK-2846
            // Added by Pooja Mantri - TIGTK-2599
            String strMsgProgramProjectNumberKey = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.ProgramProject");
            // Added by Pooja Mantri - TIGTK-2599
            // Addition for Tiger - PCM stream by SGS Ends

            if ("Approve".equals(sRouteActionOfTask)) {

                String strChangeManagerNameId = "";
                String strCRName = "";
                String strChangeManagerName = "";
                String strProgramProjectName = "";
                // Added for JIRA TIGTK-2599 -- Pooja Mantri
                String strProgramProjectId = "";
                // Added for JIRA TIGTK-2599 -- Pooja Mantri

                if (strObjectType.equals(TigerConstants.TYPE_PSS_CHANGEREQUEST)) {

                    // Get ChangeRequest Details
                    /*
                     * strChangeManagerNameId = domChangeRequestObject.getInfo(context, "from[" + RELASHIONSHIP_CHANGE_COORDINATOR + "].to.id"); strCRDescription = (String)
                     * domChangeRequestObject.getInfo(context, domChangeRequestObject.SELECT_DESCRIPTION); strCRName = (String) domChangeRequestObject.getInfo(context,
                     * domChangeRequestObject.SELECT_NAME);
                     */

                    strChangeManagerNameId = (String) mapChangeRequestInfo.get("from[" + RELASHIONSHIP_CHANGE_COORDINATOR + "].to.id");
                    strCRName = (String) mapChangeRequestInfo.get(DomainConstants.SELECT_NAME);
                    strProgramProjectName = (String) mapChangeRequestInfo.get("to[" + RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.name");
                    // Added for JIRA TIGTK-2599 -- Pooja Mantri
                    strProgramProjectId = (String) mapChangeRequestInfo.get("to[" + RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
                    // Added for JIRA TIGTK-2599 -- Pooja Mantri
                    if (strChangeManagerNameId != null) {
                        DomainObject domChangeManagerObject = new DomainObject(strChangeManagerNameId);
                        strChangeManagerName = (String) domChangeManagerObject.getInfo(context, domChangeManagerObject.SELECT_NAME);
                    }
                }
                // TIGTK-7102 : TS : 12/07/2017 : START
                else if (strObjectType.equals(TigerConstants.TYPE_PSS_MATERIAL_REQUEST)) {
                    Map payload = new HashMap();
                    payload.put("subject", "emxFramework.MaterialRequest.eServicecommonCompleteTask.SubjectApprove");
                    payload.put("message", "emxFramework.MaterialRequest.eServicecommonCompleteTask.MessageApprove");

                    String sBaseURL = emxMailUtil_mxJPO.getBaseURL(context, null);
                    int position = sBaseURL.lastIndexOf("/");
                    String strBaseURLSubstring = sBaseURL.substring(0, position);
                    String strCRLink = strBaseURLSubstring + "/emxNavigator.jsp?&objectId=" + strChangeObjectId;
                    String[] messageKeys_ = new String[2];
                    messageKeys_[0] = "description";
                    messageKeys_[1] = "link";
                    String[] messageValues_ = new String[2];
                    messageValues_[0] = (String) domChangeActionObject.getInfo(context, DomainConstants.SELECT_DESCRIPTION);
                    messageValues_[1] = strCRLink;
                    payload.put("messageKeys", messageKeys_);
                    payload.put("messageValues", messageValues_);
                    new emxNotificationUtilBase_mxJPO(context, null).objectNotification(context, ObjectId, "APPObjectRouteTaskApprovedEvent", payload);

                }
                // TIGTK-7102 : TS : 12/07/2017 : END

                if ("Reject".equals(sApprovalStatus) && "FALSE".equals(inboxTask.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CHECKAPPROVEORABSTAIN))) {
                    /*
                     * // Send Iconmail to Route owner arguments = new String[20]; arguments[0] = sOwner; arguments[1] = "emxFramework.ProgramObject.eServicecommonCompleteTask.SubjectReject";
                     * arguments[2] = "0"; arguments[3] = "emxFramework.ProgramObject.eServicecommonCompleteTask.MessageReject"; arguments[4] = "7"; arguments[5] = "name"; arguments[6] =
                     * sLastName+","+sFirstName+""+"("+sPerson+")"; arguments[7] = "IBType"; arguments[8] = sType; arguments[9] = "IBName"; arguments[10] = sName; arguments[11] = "IBRev";
                     * arguments[12] = sRev; arguments[13] = "RType"; arguments[14] = sRouteType; arguments[15] = "RName"; arguments[16] = sRouteName; arguments[17] = "RRev"; arguments[18] =
                     * sRouteRev; arguments[19] = ObjectId;
                     * 
                     * ${CLASS:emxMailUtilBase}.sendNotificationToUser(context,arguments);
                     */

                    // If The context Object Type is PSS_ChangeRequest then Send nitification to Change manager of change Request
                    if (strObjectType.equals(TigerConstants.TYPE_PSS_CHANGEREQUEST) && strChangeManagerName != null) {
                        // TGPSS_PCM-TS152 Change Request Notifications_V2.2 | 07/03/2017 |Harika Varanasi : Starts

                        // PCM : TIGTK-10909 :Sayali D : 13-Nov-2017 :START
                        boolean bLastTaskRejected = false;
                        String ApprovalStatus_REJECT = "Reject";
                        String ApprovalStatus_APPROVE = "Approve";
                        StringList selectStmts = new StringList(4);
                        selectStmts.addElement(DomainConstants.SELECT_ID);
                        selectStmts.addElement(DomainConstants.SELECT_POLICY);

                        StringList relStmts = new StringList(4);
                        relStmts.addElement("id[connection]");

                        MapList mlObjRoute = inboxTask.getRelatedObjects(context, DomainConstants.RELATIONSHIP_ROUTE_TASK, // relationship pattern
                                DomainConstants.TYPE_ROUTE, // object pattern
                                selectStmts, // object selects
                                relStmts, // relationship selects
                                false, // to direction
                                true, // from direction
                                (short) 1, // recursion level
                                null, // object where clause
                                null, // relationship where clause
                                0);
                        if (mlObjRoute.size() > 0) {
                            Map<?, ?> objRouteMap = (Map<?, ?>) mlObjRoute.get(0);
                            String sOIDRoute = (String) objRouteMap.get(DomainConstants.SELECT_ID);
                            DomainObject domRoutebject = new DomainObject(sOIDRoute);
                            StringList slTaskList = domRoutebject.getInfoList(context, "to[" + DomainConstants.RELATIONSHIP_ROUTE_TASK + "].from.id");
                            StringList slList = new StringList();
                            Iterator<String> itr = slTaskList.iterator();
                            while (itr.hasNext()) {
                                String sTaskId = (String) itr.next();

                                DomainObject domTaskObject = new DomainObject(sTaskId);
                                String strStatus = domTaskObject.getAttributeValue(context, DomainConstants.ATTRIBUTE_APPROVAL_STATUS);
                                if (strStatus.equals(ApprovalStatus_REJECT) || strStatus.equals(ApprovalStatus_APPROVE)) {
                                    slList.add(sTaskId);
                                }
                            }

                            if (slList.size() == (slTaskList.size())) {
                                bLastTaskRejected = true;
                            }
                        }
                        Map payload = new HashMap();
                        payload.put("fromList", strContextUser);
                        // TIGTK-11455 :START
                        payload.put("RejectedTask_Comment", sComments);
                        // TIGTK-11455 :END
                        if (bLastTaskRejected) {
                            payload.put("ChangeStateTo", "Rejected");
                        }
                        // PCM : TIGTK-10909 :Sayali D : 13-Nov-2017 : END
                        // PCM : TIGTK-6815 : 24/04/17 : AB : START
                        emxNotificationUtil_mxJPO.objectNotification(context, strChangeObjectId, "PSS_CRTaskRejectNotification", payload);
                        // PCM : TIGTK-6815 : 24/04/17 : AB : END
                        /*
                         * StringList toList = new StringList(); toList.add(strChangeManagerName); // Creating ccList StringList ccList = new StringList(); ccList.add(sOwner);
                         * 
                         * // "SubjectKey" StringBuffer StringBuffer subjectKey = new StringBuffer(); subjectKey.append(strMsgTigerKey + " "); subjectKey.append(sInboxTaskAssigneeName + " ");
                         * subjectKey.append(strMsgRejectedTaskOnKey + " "); subjectKey.append(strCRName + " "); subjectKey.append(strMsgForStringKey + " "); subjectKey.append(strProgramProjectName);
                         * 
                         * // String comment = "Task is Rejected"; StringBuffer strBufferMessage = new StringBuffer();
                         * 
                         * strBufferMessage.append(strMsgUserKey + " "); strBufferMessage.append(sInboxTaskAssigneeName); strBufferMessage.append("\n"); strBufferMessage.append(strMsgTaskCommentsKey +
                         * " "); strBufferMessage.append(sComments); // Added by Pooja Mantri - TIGTK-2846 strBufferMessage.append("\n"); strBufferMessage.append(strMsgCRNumberKey + " "); String
                         * strCRLink = DomainConstants.EMPTY_STRING; String strBaseURL = MailUtil.getBaseURL(context); String strBaseURLSubstring = DomainConstants.EMPTY_STRING; if
                         * (UIUtil.isNotNullAndNotEmpty(strBaseURL)) { int position = strBaseURL.lastIndexOf("/"); strBaseURLSubstring = strBaseURL.substring(0, position); strCRLink =
                         * strBaseURLSubstring + "/emxTree.jsp?mode=edit&objectId=" + strChangeObjectId; } strBufferMessage.append(strCRLink); // Added by Pooja Mantri - TIGTK-2846
                         * 
                         * // Added by Pooja Mantri - TIGTK-2599 String strProgramProjectLink = DomainConstants.EMPTY_STRING; strProgramProjectLink = strBaseURLSubstring +
                         * "/emxTree.jsp?mode=edit&objectId=" + strProgramProjectId; strBufferMessage.append("\n"); strBufferMessage.append(strMsgProgramProjectNumberKey + " ");
                         * strBufferMessage.append(strProgramProjectLink); // Added by Pooja Mantri - TIGTK-2599
                         * 
                         * // Added for JIRA TIGTK-2874 shadowAgent.popContext(context, null); // Added for JIRA TIGTK-2874 // Send Notification to Change Manager of CR for reject Task.
                         * MailUtil.sendNotification(context, toList, // toList ccList, // ccList null, // bccList subjectKey.toString(), // subjectKey null, // subjectKeys null, // subjectValues
                         * strBufferMessage.toString(), // messageKey null, // messageKeys null, // messageValues new StringList(ObjectId), // objectIdList null); // companyName
                         */
                        // TGPSS_PCM-TS152 Change Request Notifications_V2.2 | 07/03/2017 |Harika Varanasi : Ends

                    } else if (strObjectType.equals(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION)) {
                        String strMfgChangeActionId = Route.getInfo(context, "to[" + DomainConstants.RELATIONSHIP_OBJECT_ROUTE + "].from.id");
                        DomainObject domMfgChangeActionId = new DomainObject(strMfgChangeActionId);
                        String strMCAState = domMfgChangeActionId.getInfo(context, DomainConstants.SELECT_CURRENT);

                        String sInboxTaskLink = emxNotificationUtil_mxJPO.getObjectLinkHTML(context, sName, ObjectId);
                        String sRouteLink = emxNotificationUtil_mxJPO.getObjectLinkHTML(context, sRouteName, sRouteId);

                        // Demote Manufacturing Change Action to In Work State if Current state is 'In Review'
                        if (strMCAState.equals("In Review")) {
                            domMfgChangeActionId.setState(context, "In Work");
                        }

                        // Get connected Manufacturing ChangeOrder objects with ChangeAction
                        StringList objectSel = new StringList(1);
                        objectSel.addElement(DomainConstants.SELECT_ID);
                        objectSel.addElement(DomainConstants.SELECT_CURRENT);
                        StringList relSel = new StringList(1);
                        relSel.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

                        MapList mfgchangeOrderList = domMfgChangeActionId.getRelatedObjects(context, // context
                                TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION, // relationship pattern
                                TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER, // object pattern
                                objectSel, // object selects
                                relSel, // relationship selects
                                true, // to direction
                                false, // from direction
                                (short) 1, // recursion level
                                "", // object where clause
                                "", // relationship where clause
                                (short) 0);

                        // Check Current State of Manufacturing ChangeOrder
                        if (mfgchangeOrderList.size() != 0) {
                            for (int m = 0; m < mfgchangeOrderList.size(); m++) {
                                @SuppressWarnings("unchecked")
                                Map<String, String> mMCOObj = (Map<String, String>) mfgchangeOrderList.get(m);
                                String strMCOId = (String) mMCOObj.get(DomainConstants.SELECT_ID);
                                String strMCOState = (String) mMCOObj.get(DomainConstants.SELECT_CURRENT);
                                DomainObject domMCO = new DomainObject(strMCOId);
                                if (strMCOState.equals("In Review")) {
                                    domMCO.setState(context, "In Work");
                                }
                            }
                        }
                        // TGPSS_PCM-TS156 Manufacturing Change Action Notifications V_2.2 | 20/03/2017 |Harika Varanasi : Starts
                        // Send Mail Notification
                        /*
                         * Map<String, Object> payload = new HashMap<String, Object>(); payload.put("subject", "emxFramework.ProgramObject.eServicecommonCompleteTask.MCASubjectReject"); // PCM
                         * TIGTK-3628 : 03/12/2016 : AB : Start payload.put("message", "emxFramework.ProgramObject.eServicecommonCompleteTask.MessageReject"); payload.put("tableHeader",
                         * "emxFramework.ProgramObject.eServicecommonCompleteTask.TableHeader"); payload.put("tableRow", "emxFramework.ProgramObject.eServicecommonCompleteTask.TableData"); String[]
                         * messageKeys = { "name", "IBType", "IBName", "IBRev", "RType", "RName", "RRev" }; String[] messageValues = { sLastName + "," + sFirstName + "" + " (" + sPerson + ")", sType,
                         * sName, sRev, sRouteType, sRouteName, sRouteRev }; String[] tableKeys = { "TaskType", "RouteName", "TaskName", "TaskOwner", "CompletionDate", "Comments" }; String[]
                         * tableValues = { sRouteActionOfTask, sRouteLink, sInboxTaskLink, sPerson, sActualCompletionDate, sComments }; payload.put("messageKeys", messageKeys);
                         * payload.put("messageValues", messageValues); payload.put("tableRowKeys", tableKeys); payload.put("tableRowValues", tableValues);
                         * emxNotificationUtil_mxJPO.objectNotification(context, ObjectId, "PSSObjectRouteTaskRejectedForManufacturingChangeActionEvent", payload);
                         */
                        Map payLoad = new HashMap();
                        payLoad.put("fromList", strContextUser);
                        // TIGTK-11455 :START
                        payLoad.put("RejectedTask_Comment", sComments);
                        // TIGTK-11455 :END
                        emxNotificationUtil_mxJPO.objectNotification(context, strChangeObjectId, "PSS_MCATaskRejectionNotification", payLoad);
                        // TGPSS_PCM-TS156 Manufacturing Change Action Notifications V_2.2 | 20/03/2017 |Harika Varanasi : Ends
                        // PCM TIGTK-3628 : 03/12/2016 : AB : END
                    }
                    // TIGTK-7102 : TS : 12/07/2017 : START
                    else if (strObjectType.equals(TigerConstants.TYPE_PSS_MATERIAL_REQUEST)) {
                        Map payload = new HashMap();
                        payload.put("subject", "emxFramework.MaterialRequest.eServicecommonCompleteTask.SubjectReject");
                        payload.put("message", "emxFramework.MaterialRequest.eServicecommonCompleteTask.MessageReject");
                        String sBaseURL = emxMailUtil_mxJPO.getBaseURL(context, null);
                        int position = sBaseURL.lastIndexOf("/");
                        String strBaseURLSubstring = sBaseURL.substring(0, position);
                        String strCRLink = strBaseURLSubstring + "/emxNavigator.jsp?&objectId=" + strChangeObjectId;
                        String[] messageKeys_ = new String[2];
                        messageKeys_[0] = "description";
                        messageKeys_[1] = "link";

                        String[] messageValues_ = new String[2];
                        messageValues_[0] = (String) domChangeActionObject.getInfo(context, DomainConstants.SELECT_DESCRIPTION);
                        messageValues_[1] = strCRLink;

                        payload.put("messageKeys", messageKeys_);
                        payload.put("messageValues", messageValues_);
                        new emxNotificationUtilBase_mxJPO(context, null).objectNotification(context, ObjectId, "APPObjectRouteTaskRejectedEvent", payload);

                    } // TIGTK-7102 : TS : 12/07/2017 : END
                      // TIGTK-6418:Rutuja Ekatpure:8/8/2017:Start
                    else if (strObjectType.equals(TigerConstants.TYPE_PSS_ISSUE)) {
                        String strISSUEId = Route.getInfo(context, "to[" + DomainConstants.RELATIONSHIP_OBJECT_ROUTE + "].from.id");
                        DomainObject domIssue = new DomainObject(strISSUEId);
                        String strIssueState = domIssue.getInfo(context, DomainConstants.SELECT_CURRENT);

                        // promote connected Issue to Rejected state
                        if (strIssueState.equals(TigerConstants.STATE_PSS_ISSUE_REVIEW)) {
                            domIssue.setAttributeValue(context, DomainConstants.ATTRIBUTE_COMMENTS, sComments);
                            domIssue.setAttributeValue(context, TigerConstants.ATTRIBUTE_BRANCH_TO, "Rejected");
                            domIssue.promote(context);

                            Map payLoad = new HashMap();
                            try {
                                ContextUtil.pushContext(context, strContextUser, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                                payLoad.put("fromList", strContextUser);
                                payLoad.put("IssueRejectComments", sComments);
                                emxNotificationUtil_mxJPO.objectNotification(context, strChangeObjectId, "PSS_IssueReviewTaskRejectNotification", payLoad);
                            } catch (Exception e) {
                            } finally {
                                ContextUtil.popContext(context);
                            }
                        }
                    } // TIGTK-6418:Rutuja Ekatpure:8/8/2017:End
                    else {
                        // Added for JIRA TIGTK-2874
                        shadowAgent.pushContext(context, null);
                        // Added for JIRA TIGTK-2874
                        String notificationObjectName = "";
                        String sInboxTaskLink = emxNotificationUtil_mxJPO.getObjectLinkHTML(context, sName, ObjectId);
                        String sRouteLink = emxNotificationUtil_mxJPO.getObjectLinkHTML(context, sRouteName, sRouteId);
                        String strChangeAction;
                        Map payload = new HashMap();
                        payload.put("subject", "emxFramework.ProgramObject.eServicecommonCompleteTask.SubjectReject");
                        payload.put("message", "emxFramework.ProgramObject.eServicecommonCompleteTask.MessageReject");
                        payload.put("tableHeader", "emxFramework.ProgramObject.eServicecommonCompleteTask.TableHeader");
                        payload.put("tableRow", "emxFramework.ProgramObject.eServicecommonCompleteTask.TableData");
                        String[] messageKeys = { "name", "IBType", "IBName", "IBRev", "RType", "RName", "RRev" };
                        String[] messageValues = { sLastName + "," + sFirstName + "" + " (" + sPerson + ")", sType, sName, sRev, sRouteType, sRouteName, sRouteRev };
                        String[] tableKeys = { "TaskType", "RouteName", "TaskName", "TaskOwner", "CompletionDate", "Comments" };
                        String[] tableValues = { sRouteActionOfTask, sRouteLink, sInboxTaskLink, sPerson, sActualCompletionDate, sComments };
                        payload.put("messageKeys", messageKeys);
                        payload.put("messageValues", messageValues);
                        payload.put("tableRowKeys", tableKeys);
                        payload.put("tableRowValues", tableValues);
                        // ${CLASS:emxNotificationUtil}.objectNotification(context, ObjectId, "APPObjectRouteTaskRejectedEvent", payload);
                        strChangeAction = PropertyUtil.getSchemaProperty(context, "type_ChangeAction");

                        // Added for RFC-033 -- Reject Change Action Approval Task -- by SGS Starts
                        if (strObjectType.equalsIgnoreCase(strChangeAction)) {
                            final String PSS_CHANGEORDER_STATE_INWORK = "In Work";

                            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                            domChangeActionObject.setState(context, PSS_CHANGEORDER_STATE_INWORK);
                            ContextUtil.popContext(context);
                            // TGPSS_PCM-TS153 Change Actions Notifications_V2.1 | 17/03/2017 |Harika Varanasi : Starts
                            notificationObjectName = "";
                            // TIGTK-11455 :START
                            payload.put("RejectedTask_Comment", sComments);
                            // TIGTK-11455 :END
                            emxNotificationUtil_mxJPO.objectNotification(context, strChangeObjectId, "PSS_CATaskRejectNotification", payload);
                            // TGPSS_PCM-TS153 Change Actions Notifications_V2.1 | 17/03/2017 |Harika Varanasi : Ends
                        }
                        // Added for RFC-033 -- Reject Change Action Approval Task -- by SGS Ends

                        /*
                         * Added for PSS_ChangeNotice : TS108
                         */
                        String strChangeNotice = PropertyUtil.getSchemaProperty(context, "type_PSS_ChangeNotice");
                        if (strObjectType.equalsIgnoreCase(strChangeNotice)) {

                            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                            domChangeActionObject.setState(context, TigerConstants.STATE_PREPARE_CN);
                            ContextUtil.popContext(context);

                            notificationObjectName = strObjectType.equals(strChangeNotice) ? "PSSObjectRouteTaskRejectedForChangeNoticeEvent" : "APPObjectRouteTaskRejectedEvent";
                            // TIGTK-11455 :START
                            if (notificationObjectName.equals("PSSObjectRouteTaskRejectedForChangeNoticeEvent")) {
                                payload.put("RejectedTask_Comment", sComments);
                                payload.put("id", strChangeObjectId);
                            }
                            // TIGTK-11455 :END
                        }
                        // TGPSS_PCM-TS153 Change Actions Notifications_V2.1 | 17/03/2017 |Harika Varanasi : Starts
                        if (UIUtil.isNotNullAndNotEmpty(notificationObjectName)) {
                            emxNotificationUtil_mxJPO.objectNotification(context, ObjectId, notificationObjectName, payload);
                        }
                        // TGPSS_PCM-TS153 Change Actions Notifications_V2.1 | 17/03/2017 |Harika Varanasi : Ends
                    }
                }
                // Modification for Tiger - PCM stream by SGS starts
                else if ("Approve".equals(sApprovalStatus) && "FALSE".equalsIgnoreCase(inboxTask.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CHECKAPPROVEORABSTAIN))) {

                    String sInboxTaskLink = emxNotificationUtil_mxJPO.getObjectLinkHTML(context, sName, ObjectId);
                    String sRouteLink = emxNotificationUtil_mxJPO.getObjectLinkHTML(context, sRouteName, sRouteId);

                    Map payload = new HashMap();
                    payload.put("subject", "emxFramework.ProgramObject.eServicecommonCompleteTask.SubjectApprove");
                    payload.put("message", "emxFramework.ProgramObject.eServicecommonCompleteTask.MessageApprove");
                    payload.put("tableHeader", "emxFramework.ProgramObject.eServicecommonCompleteTask.TableHeader");
                    payload.put("tableRow", "emxFramework.ProgramObject.eServicecommonCompleteTask.TableData");
                    String[] messageKeys = { "name", "IBType", "IBName", "IBRev", "RType", "RName", "RRev" };
                    String[] messageValues = { sLastName + "," + sFirstName + "" + " (" + sPerson + ")", sType, sName, sRev, sRouteType, sRouteName, sRouteRev };
                    payload.put("messageKeys", messageKeys);
                    payload.put("messageValues", messageValues);
                    // Modification for TIGTK-3068
                    String strChangeNotice = PropertyUtil.getSchemaProperty(context, "type_PSS_ChangeNotice");

                    // TIGTK-7745 : TaskApproval CR, CA, MCA and CN : AB : 21/08/2017 : START
                    // TIGTK-10791:TS: added consition for ISSUE:8/11/2017:Start
                    if (!(TigerConstants.TYPE_PSS_CHANGENOTICE.equals(strObjectType) || TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION.equals(strObjectType)
                            || TigerConstants.TYPE_PSS_CHANGEREQUEST.equals(strObjectType) || ChangeConstants.TYPE_CHANGE_ACTION.equalsIgnoreCase(strObjectType)
                            || TigerConstants.TYPE_PSS_ISSUE.equals(strObjectType))) {
                        // TIGTK-10791:TS: added consition for ISSUE:8/11/2017:End
                        // TIGTK-7745 : TaskApproval CA : AB : 21/08/2017 : END
                        // For all other Type's Object send notification as per OOTB behaviour.
                        String[] tableKeys = { "TaskType", "RouteName", "TaskName", "TaskOwner", "CompletionDate", "Comments" };
                        String[] tableValues = { sRouteActionOfTask, sRouteLink, sInboxTaskLink, sPerson, sActualCompletionDate, sComments };

                        payload.put("tableRowKeys", tableKeys);
                        payload.put("tableRowValues", tableValues);

                        new emxNotificationUtilBase_mxJPO(context, null).objectNotification(context, ObjectId, "APPObjectRouteTaskApprovedEvent", payload);
                    }
                } else if (("TRUE".equalsIgnoreCase(inboxTask.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CHECKAPPROVEORABSTAIN))) || ("Abstain".equalsIgnoreCase(sApprovalStatus))) {

                    if (strObjectType.equals(TigerConstants.TYPE_PSS_CHANGEREQUEST) && strChangeManagerName != null) {

                        StringList toList = new StringList();
                        toList.add(strChangeManagerName);
                        StringList ccList = new StringList();
                        ccList.add(sOwner);

                        // Creating SubjectKey Buffer
                        StringBuffer subjectKey = new StringBuffer();
                        subjectKey.append(strMsgTigerKey + " ");
                        subjectKey.append(strMsgUserKey + " ");
                        subjectKey.append(sInboxTaskAssigneeName + " ");
                        subjectKey.append(strMsgAbstainTaskOnKey + " ");
                        subjectKey.append(strCRName + " ");
                        subjectKey.append(strMsgForStringKey + " ");
                        subjectKey.append(strProgramProjectName);

                        /* String comment = "Task is Abstained"; */
                        StringBuffer strBufferMessage = new StringBuffer();
                        /*
                         * strBufferMessage.append(comment).append("\n"); strBufferMessage.append(" Task Number :").append(sName).append("\n"); strBufferMessage.append(" Change Request Number : "
                         * ).append(strCRName).append("\n"); strBufferMessage.append(" Description : ").append(strCRDescription).append("\n");
                         */

                        strBufferMessage.append(strMsgUserKey + " ");
                        strBufferMessage.append(sInboxTaskAssigneeName);
                        strBufferMessage.append("\n");
                        strBufferMessage.append(strMsgTaskCommentsKey + " ");
                        strBufferMessage.append(sComments);
                        // Added by Pooja Mantri - TIGTK-2846
                        strBufferMessage.append("\n");
                        strBufferMessage.append(strMsgCRNumberKey + " ");
                        String strCRLink = DomainConstants.EMPTY_STRING;
                        String strBaseURL = MailUtil.getBaseURL(context);
                        String strBaseURLSubstring = DomainConstants.EMPTY_STRING;
                        if (UIUtil.isNotNullAndNotEmpty(strBaseURL)) {
                            int position = strBaseURL.lastIndexOf("/");
                            strBaseURLSubstring = strBaseURL.substring(0, position);
                            strCRLink = strBaseURLSubstring + "/emxTree.jsp?mode=edit&objectId=" + strChangeObjectId;
                        }
                        strBufferMessage.append(strCRLink);
                        // Added by Pooja Mantri - TIGTK-2846

                        // Added by Pooja Mantri - TIGTK-2599
                        String strProgramProjectLink = DomainConstants.EMPTY_STRING;
                        strProgramProjectLink = strBaseURLSubstring + "/emxTree.jsp?mode=edit&objectId=" + strProgramProjectId;
                        strBufferMessage.append("\n");
                        strBufferMessage.append(strMsgProgramProjectNumberKey + " ");
                        strBufferMessage.append(strProgramProjectLink);
                        // Added by Pooja Mantri - TIGTK-2599

                        // Added for JIRA TIGTK-2874
                        shadowAgent.popContext(context, null);
                        // Added for JIRA TIGTK-2874

                        // PCM : TIGTK-3974 : 18/04/17 : AB : START
                        if (!strChangeCurrent.equalsIgnoreCase(ChangeConstants.STATE_CHANGEREQUEST_EVALUATE)) {
                            // Send Notification to ChangeManaher of CR for reject Task.
                            MailUtil.sendNotification(context, toList, // toList
                                    ccList, // ccList
                                    null, // bccList
                                    subjectKey.toString(), // subjectKey
                                    null, // subjectKeys
                                    null, // subjectValues
                                    strBufferMessage.toString(), // messageKey
                                    null, // messageKeys
                                    null, // messageValues
                                    new StringList(ObjectId), // objectIdList
                                    null); // companyName
                        }
                        // PCM : TIGTK-3974 : 18/04/17 : AB : END
                        inboxTask.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CHECKAPPROVEORABSTAIN, "false");
                    } else {

                    }
                }

            }
            // Modification for Tiger - PCM stream by SGS ends

            // Find if all tasks are completed on this level and Find at least one task is rejected on this level
            final String SELECT_CURRENT_ROUTE_NODE = "attribute[" + sAttCurrentRouteNode + "]";
            final String SELECT_ROUTE_NODE_ID = "attribute[" + DomainObject.ATTRIBUTE_ROUTE_NODE_ID + "]";
            final String SELECT_AUTO_STOP_ON_REJECTION = "attribute[" + sAttAutoStopOnRejection + "]";
            final String SELECT_APPROVAL_STATUS = "attribute[" + DomainConstants.ATTRIBUTE_APPROVAL_STATUS + "]";
            final boolean GET_TO = true;
            final boolean GET_FROM = true;

            StringList slBusSelect = new StringList();
            slBusSelect.add(com.matrixone.apps.common.Route.SELECT_ROUTE_STATUS);
            slBusSelect.add(SELECT_CURRENT_ROUTE_NODE);
            slBusSelect.add(SELECT_AUTO_STOP_ON_REJECTION);

            Map mapInfo = Route.getInfo(context, slBusSelect);

            String strCurrentRouteLevel = (String) mapInfo.get(SELECT_CURRENT_ROUTE_NODE);
            String sAutoStopOnRejection = (String) mapInfo.get(SELECT_AUTO_STOP_ON_REJECTION);

            // Expand route and get 'Route Node ID' on relationship 'Route Node' for 'Route Sequence' = 'Current Route Node'
            slBusSelect = new StringList();
            StringList slRelSelect = new StringList();
            slRelSelect.add(SELECT_ROUTE_NODE_ID);

            String strRelPattern = DomainObject.RELATIONSHIP_ROUTE_NODE;
            String strTypePattern = "*";
            String strBusWhere = "";
            String strRelWhere = "attribute[" + DomainObject.ATTRIBUTE_ROUTE_SEQUENCE + "]==" + strCurrentRouteLevel;
            short nRecurseLevel = (short) 1;

            MapList mlRouteNodes = Route.getRelatedObjects(context, strRelPattern, strTypePattern, slBusSelect, slRelSelect, !GET_TO, GET_FROM, nRecurseLevel, strBusWhere, strRelWhere);

            StringBuffer sbufMatchList = new StringBuffer(64);
            Map mapRouteNode = null;
            for (Iterator itrRouteNodes = mlRouteNodes.iterator(); itrRouteNodes.hasNext();) {
                mapRouteNode = (Map) itrRouteNodes.next();

                if (sbufMatchList.length() > 0) {
                    sbufMatchList.append(",");
                }
                sbufMatchList.append(mapRouteNode.get(SELECT_ROUTE_NODE_ID));
            }

            // Expand route and get id for tasks with 'Route Node ID' = 'Route Node ID' just found.
            slBusSelect = new StringList(DomainObject.SELECT_ID);
            slBusSelect.add(DomainObject.SELECT_CURRENT);
            slBusSelect.add(SELECT_APPROVAL_STATUS);

            slRelSelect = new StringList();
            strRelPattern = DomainObject.RELATIONSHIP_ROUTE_TASK;
            strTypePattern = DomainObject.TYPE_INBOX_TASK;
            strBusWhere = "(attribute[" + DomainObject.ATTRIBUTE_ROUTE_NODE_ID + "] matchlist \"" + sbufMatchList.toString() + "\" \",\")";
            strRelWhere = "";
            nRecurseLevel = (short) 1;

            MapList mlTasks = Route.getRelatedObjects(context, strRelPattern, strTypePattern, slBusSelect, slRelSelect, GET_TO, !GET_FROM, nRecurseLevel, strBusWhere, strRelWhere);

            // Find if at least one task is incomplete on this level
            Map mapTaskInfo = null;
            String strTaskState = "";
            String strApprovalStatus = "";
            boolean isLevelCompleted = true;
            boolean isTaskRejectedOnThisLevel = false;

            for (Iterator itrRouteNodes = mlTasks.iterator(); itrRouteNodes.hasNext();) {
                mapTaskInfo = (Map) itrRouteNodes.next();

                strTaskState = (String) mapTaskInfo.get(DomainObject.SELECT_CURRENT);
                strApprovalStatus = (String) mapTaskInfo.get(SELECT_APPROVAL_STATUS);

                if (!strTaskState.equals(DomainConstants.STATE_INBOX_TASK_COMPLETE)) {
                    isLevelCompleted = false;
                }

                if ("Reject".equals(strApprovalStatus)) {
                    isTaskRejectedOnThisLevel = true;
                }
            }

            boolean isRouteToBeStopped = false;
            if ("Approve".equals(sRouteActionOfTask)) {
                if ("Reject".equals(sApprovalStatus)) {
                    isRouteToBeStopped = true;
                }
            }
            // Bug 346841 : Removed following if from another else part to also consider Approve kind of tasks.
            if (isTaskRejectedOnThisLevel) {
                isRouteToBeStopped = true;
            }

            if (!isLevelCompleted) {
                if ("any".equalsIgnoreCase(sProcessionRule)) {
                    isLevelCompleted = true;
                }
            }

            if (isRouteToBeStopped) {
                if ("Immediate".equals(sAutoStopOnRejection) || isLevelCompleted) {

                    // Addition for Tiger - PCM stream by SGS starts

                    // below code is executed if the object is PSS_ChangeRequest object
                    if (strObjectType.equals(TigerConstants.TYPE_PSS_CHANGEREQUEST)) {
                        String sRouteBaseState = Route.getInfo(context,
                                "to[" + DomainConstants.RELATIONSHIP_OBJECT_ROUTE + "].attribute[" + PropertyUtil.getSchemaProperty(context, "attribute_RouteBaseState") + "]");
                        // if
                        if ("Deferred".equals(sAutoStopOnRejection) && "state_Evaluate".equals(sRouteBaseState)) {
                            try {
                                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                                Route.setAttributeValue(context, DomainConstants.ATTRIBUTE_ROUTE_STATUS, "Finished");
                            } finally {
                                ContextUtil.popContext(context);
                            }
                            domChangeRequestObject.promote(context);
                        }
                        try {
                            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                            Route.setAttributeValue(context, DomainConstants.ATTRIBUTE_ROUTE_STATUS, "Stopped");
                        } finally {
                            ContextUtil.popContext(context);
                        }
                    } else {
                        // Set Route Status attribute to Stopped
                        try {
                            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                            Route.setAttributeValue(context, DomainConstants.ATTRIBUTE_ROUTE_STATUS, "Stopped");
                        } finally {
                            ContextUtil.popContext(context);
                        }
                    }

                    // Addition for Tiger - PCM stream by SGS Ends

                    Map objectDetails = null;
                    String sState = null;
                    String sRouteNodeID = null;

                    for (int i = 0; i < ObjectsList.size(); i++) {
                        objectDetails = (Map) ObjectsList.get(i);
                        sState = (String) objectDetails.get("current");
                        sRouteNodeID = (String) objectDetails.get("attribute[" + DomainConstants.ATTRIBUTE_ROUTE_NODE_ID + "]");

                        if (sState.equals(DomainConstants.STATE_INBOX_TASK_ASSIGNED) || sState.equals(DomainConstants.STATE_INBOX_TASK_REVIEW)) {
                            if ((sProcessionRule.toLowerCase()).equals("any")) {
                                DomainRelationship.disconnect(context, sRouteNodeID);

                                // Delete unsigned/non-completed tasks
                                DomainObject.deleteObjects(context, new String[] { (String) objectDetails.get(DomainConstants.SELECT_ID) });
                            }
                        }
                    } // for

                    return 0;
                }
            } // if (isRouteToBeStopped)
              // End: Auto-Stop

            // If Approval Status == Reject

            if (!"Reject".equals(sApprovalStatus)) {
                // Expand route and get the current state of all Inbox Tasks associated with it

                // commented for the BugNo 327641 12/28/2006-- Ends
                int bFound = 0;
                // Added Boolean variable to check if there are any tasks having status != Complete for the bug no 340260
                boolean isNonCompleteTasksThere = false;
                // Logic to check if there any tasks that are not in Complete State
                // Till here
                for (int i = 0; i < ObjectsList.size(); i++) {
                    Map objectDetails = (Map) ObjectsList.get(i);
                    String sState = (String) objectDetails.get("current");
                    // Added for bug no 340260
                    if (!sState.equals("Complete"))
                        isNonCompleteTasksThere = true;
                }
                for (int i = 0; i < ObjectsList.size(); i++) {
                    Map objectDetails = (Map) ObjectsList.get(i);
                    String sState = (String) objectDetails.get("current");
                    // till here

                    String sRouteNodeID = (String) objectDetails.get("attribute[" + DomainConstants.ATTRIBUTE_ROUTE_NODE_ID + "]");
                    if (sState.equals(DomainConstants.STATE_INBOX_TASK_ASSIGNED) || sState.equals(DomainConstants.STATE_INBOX_TASK_REVIEW)) {
                        if ((sProcessionRule.toLowerCase()).equals("any")) {

                            // Added "If Approval Status is Abstain" and "there are still tasks which are not completed", then don't promote the Route for bug no 340260
                            if (sApprovalStatus.equals("Abstain") && isNonCompleteTasksThere) {
                                bFound = 1;
                                break;
                            }

                            // DomainRelationship.disconnect(context, sRouteNodeId);
                            // Added code for the Bug NO:330220
                            com.matrixone.apps.common.Route route = (com.matrixone.apps.common.Route) DomainObject.newInstance(context, sRouteId);
                            String orgRelId = route.getRouteNodeRelId(context, sRouteNodeID);
                            DomainRelationship.disconnect(context, orgRelId);

                            // Added code for the Bug NO:330220
                            // Delete unsigned/non-completed tasks
                            String sTaskId = (String) objectDetails.get(DomainConstants.SELECT_ID);
                            java.lang.String[] objectIds = new String[1];
                            objectIds[0] = sTaskId;
                            DomainObject.deleteObjects(context, objectIds);

                            // Send mail to owner of task about deletion
                            String sTaskOwner = (String) objectDetails.get("owner");
                            String sDelTaskType = (String) objectDetails.get(DomainConstants.SELECT_TYPE);
                            String sDelTaskName = (String) objectDetails.get(DomainConstants.SELECT_NAME);
                            String sDelTaskRev = (String) objectDetails.get(DomainConstants.SELECT_REVISION);

                            arguments = new String[19];
                            arguments[0] = sTaskOwner;
                            arguments[1] = "emxFramework.ProgramObject.eServicecommonCompleteTask.SubjectDeleteTask";
                            arguments[2] = "0";
                            arguments[3] = "emxFramework.ProgramObject.eServicecommonCompleteTask.MessageDeletionTask";
                            arguments[4] = "6";
                            arguments[5] = "IBType";
                            arguments[6] = sDelTaskType;
                            arguments[7] = "IBName";
                            arguments[8] = sDelTaskName;
                            arguments[9] = "IBRev";
                            arguments[10] = sDelTaskRev;
                            arguments[11] = "IBType2";
                            arguments[12] = sType;
                            arguments[13] = "IBName2";
                            arguments[14] = sName;
                            arguments[15] = "IBRev2";
                            arguments[16] = sRev;
                            arguments[17] = "";
                            arguments[18] = "";

                            mailUtil.sendNotificationToUser(context, arguments);
                        } else {
                            bFound = 1;
                            break;
                        }
                    }
                } // for

                // If None of the Inbox Task objects are returned and Route Status == Started
                if (bFound == 0 && sRouteStatus.equals("Started")) {

                    // Increment Current Route Node attribute on attached Route object
                    // PCM TIGTK-4112: 22/02/2017 : KWagh : START
                    String strContextuser = context.getUser();
                    boolean isPushContext = false;
                    try {
                        if (!strContextuser.equalsIgnoreCase("User Agent")) {

                            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                            isPushContext = true;
                        }
                        sCurrentRouteNode++;
                        Route.setAttributeValue(context, sAttCurrentRouteNode, "" + sCurrentRouteNode);
                    } catch (Exception e) {
                        // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
                        logger.error("Error in completeTask: ", e);
                        // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
                        throw e;
                    } finally {
                        if (isPushContext) {
                            ContextUtil.popContext(context);
                        }
                    }
                    // PCM TIGTK-4112: 22/02/2017 : KWagh : End
                    // Expand Route Node relationship and get all Relationship Ids whose
                    // Route Sequence == Current Route Node value
                    arguments = new String[5];
                    arguments[0] = sRouteType;
                    arguments[1] = sRouteName;
                    arguments[2] = sRouteRev;
                    arguments[3] = "" + sCurrentRouteNode;
                    arguments[4] = "0";

                    int outStr1 = initiateRoute.InitiateRoute(context, arguments);

                    // Return 0 if no more tasks for route
                    if (outStr1 == 0) {
                        // PCM TIGTK-4112: 22/02/2017 : KWagh : START
                        try {
                            if (!strContextuser.equalsIgnoreCase("User Agent")) {

                                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                                isPushContext = true;
                            }
                            MqlUtil.mqlCommand(context, "override bus " + sRouteId + "");
                            Route.promote(context);

                            Route.setAttributeValue(context, DomainConstants.ATTRIBUTE_ROUTE_STATUS, "Finished");
                        } catch (Exception e) {
                            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
                            logger.error("Error in completeTask: ", e);
                            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
                            throw e;
                        } finally {
                            if (isPushContext) {
                                ContextUtil.popContext(context);
                            }
                        }
                        // PCM TIGTK-4112: 22/02/2017 : KWagh : End
                        // Expand Object Route relationship to get routed items
                        MapList objectList = Route.getRelatedObjects(context, DomainConstants.RELATIONSHIP_OBJECT_ROUTE, "*", objectSelects, relSelects, true, false, (short) 0, null, null);

                        if (objectList.size() > 0) {
                            for (int i1 = 0; i1 < objectList.size(); i1++) {
                                Map objectmap = (Map) objectList.get(i1);
                                String sObjectId = (String) objectmap.get(DomainConstants.SELECT_ID);

                                String out11 = MqlUtil.mqlCommand(context, "print bus " + sObjectId + " select grantor[" + sRouteDelegationGrantor + "] dump");

                                if (!out11.equals("FALSE")) {
                                    // modified for the bug 316518
                                    // MqlUtil.mqlCommand(context,"modify bus "+sObjectId+" revoke grantor "+sRouteDelegationGrantor+"");
                                    MqlUtil.mqlCommand(context, "modify bus " + sObjectId + " revoke grantor '" + sRouteDelegationGrantor + "'");
                                    // modified for the bug 316518
                                }
                            }
                        }

                        if (sRouteCompletionAction.equals("Promote Connected Object")) {
                            objectSelects = new SelectList();
                            objectSelects.addElement(DomainConstants.SELECT_ID);
                            objectSelects.addElement(DomainConstants.SELECT_TYPE);
                            objectSelects.addElement(DomainConstants.SELECT_NAME);
                            objectSelects.addElement(DomainConstants.SELECT_REVISION);
                            objectSelects.addElement("current.satisfied");
                            objectSelects.addElement("current");
                            objectSelects.addElement("policy");
                            objectSelects.addElement("state");
                            relSelects = new SelectList();
                            relSelects.addElement("attribute[" + DomainConstants.ATTRIBUTE_ROUTE_BASE_STATE + "].value");
                            relSelects.addElement("attribute[" + DomainConstants.ATTRIBUTE_ROUTE_BASE_POLICY + "].value");

                            // Get all the Route content information
                            MapList objectlist = Route.getRelatedObjects(context, DomainConstants.RELATIONSHIP_OBJECT_ROUTE, "*", objectSelects, relSelects, true, false, (short) 0, null, null);

                            for (int count = 0; count < objectlist.size(); count++) {

                                Map object = (Map) objectlist.get(count);
                                String sObjType = (String) object.get(DomainConstants.SELECT_TYPE);
                                String sObjName = (String) object.get(DomainConstants.SELECT_NAME);
                                String sObjRev = (String) object.get(DomainConstants.SELECT_REVISION);
                                String sObjId = (String) object.get(DomainConstants.SELECT_ID);
                                String sIsObjSatisfied = (String) object.get("current.satisfied");
                                String sObjCurrent = (String) object.get("current");
                                String sObjPolicy = (String) object.get("policy");

                                StringList lObjState = new StringList();
                                if (object.get("state") instanceof StringList) {
                                    lObjState = (StringList) object.get("state");
                                }

                                // String lObjState = (String) object.get("state");
                                String sObjBaseState = (String) object.get("attribute[" + DomainConstants.ATTRIBUTE_ROUTE_BASE_STATE + "].value");
                                String sObjBasePolicy = (String) object.get("attribute[" + DomainConstants.ATTRIBUTE_ROUTE_BASE_POLICY + "].value");
                                int bPromoteObject = 1;

                                // Check if object state and policy maches with base state and policy
                                if (!sObjBaseState.equals("Ad Hoc")) {
                                    sObjBasePolicy = PropertyUtil.getSchemaProperty(context, sObjBasePolicy);
                                    // sObjBaseState = FrameworkUtil.lookupStateName(context, sObjBasePolicy, sObjBaseState);
                                    sObjBaseState = PropertyUtil.getSchemaProperty(context, "policy", sObjBasePolicy, sObjBaseState);

                                    // Get names from properties
                                    String sTempStore = sObjBaseState;
                                    if (sObjBaseState.equals("")) {
                                        arguments = new String[13];
                                        arguments[0] = "emxFramework.ProgramObject.eServicecommonCompleteTask.InvalidPolicy";
                                        arguments[1] = "5";
                                        arguments[2] = "State";
                                        arguments[3] = sTempStore;
                                        arguments[4] = "Type";
                                        arguments[5] = sRouteType;
                                        arguments[6] = "OType";
                                        arguments[7] = sObjType;
                                        arguments[8] = "OName";
                                        arguments[9] = sObjName;
                                        arguments[10] = "ORev";
                                        arguments[11] = sObjRev;
                                        arguments[12] = "";

                                        message = mailUtil.getMessage(context, arguments);
                                        MqlUtil.mqlCommand(context, "notice " + message + "");

                                        return 1;
                                    }

                                    sTempStore = sObjBasePolicy;

                                    if (sObjBasePolicy.equals("")) {

                                        arguments = new String[13];
                                        arguments[0] = "emxFramework.ProgramObject.eServicecommonCompleteTask.InvalidState";
                                        arguments[1] = "5";
                                        arguments[2] = "Policy";
                                        arguments[3] = sTempStore;
                                        arguments[4] = "Type";
                                        arguments[5] = sRouteType;
                                        arguments[6] = "OType";
                                        arguments[7] = sObjType;
                                        arguments[8] = "OName";
                                        arguments[9] = sObjName;
                                        arguments[10] = "ORev";
                                        arguments[11] = sObjRev;
                                        arguments[12] = "";
                                        message = mailUtil.getMessage(context, arguments);
                                        MqlUtil.mqlCommand(context, "notice " + message + "");

                                        return 1;
                                    }
                                }

                                // the below else block is commented for the bug 319223 -- this functionality regarding this bug
                                /*
                                 * else { if (bConsiderAdhocRoutes.equals("FALSE")) { continue; } }
                                 */

                                if (sObjBaseState.equals("Ad Hoc") && (!sObjBaseState.equals(sObjCurrent) || !sObjBasePolicy.equals(sObjPolicy))) {
                                    continue;
                                }

                                // Check if object is in the last state
                                /*
                                 * if ([lsearch $lObjState "$sObjCurrent"] == [expr [llength $lObjState] - 1]) { continue; }
                                 */

                                // Modified for Bug No: 293332 and Bug no: 293506
                                if (lObjState.indexOf(sObjCurrent) == (lObjState.size() - 1)) {
                                    continue;
                                }

                                objectSelects = new SelectList();
                                objectSelects.addElement("current");
                                objectSelects.addElement(DomainConstants.SELECT_TYPE);
                                objectSelects.addElement(DomainConstants.SELECT_NAME);
                                objectSelects.addElement(DomainConstants.SELECT_REVISION);
                                relSelects = new SelectList();
                                relSelects.addElement("attribute[" + DomainConstants.ATTRIBUTE_ROUTE_BASE_STATE + "].value");
                                relSelects.addElement("attribute[" + DomainConstants.ATTRIBUTE_ROUTE_BASE_POLICY + "].value");
                                DomainObject dObject = new DomainObject(sObjId);

                                // should retrieve only Route objects
                                // Include Route sub_types if applicable, use addPattern()
                                Pattern typePattern = new Pattern(DomainObject.TYPE_ROUTE);

                                // Modified for Bug No: 293332 and Bug no: 293506
                                MapList ObjectList = dObject.getRelatedObjects(context, DomainConstants.RELATIONSHIP_OBJECT_ROUTE, typePattern.getPattern(), // "*",
                                        objectSelects, relSelects, false, true, (short) 1, "", "");

                                // Check for each object if there is any route which is not complete

                                for (int i = 0; i < ObjectList.size(); i++) {
                                    Map objectsMap = (Map) ObjectList.get(i);
                                    String sObjRouteBaseState = (String) objectsMap.get("attribute[" + DomainConstants.ATTRIBUTE_ROUTE_BASE_STATE + "].value");
                                    String sObjRouteBasePolicy = (String) objectsMap.get("attribute[" + DomainConstants.ATTRIBUTE_ROUTE_BASE_POLICY + "].value");
                                    String sObjRouteType = (String) objectsMap.get(DomainConstants.SELECT_TYPE);
                                    String sObjRouteName = (String) objectsMap.get(DomainConstants.SELECT_NAME);
                                    String sObjRouteRev = (String) objectsMap.get(DomainConstants.SELECT_REVISION);
                                    String sObjRouteCurrent = (String) objectsMap.get("current");

                                    if (sObjRouteBaseState.equals("")) {
                                        sObjRouteBaseState = "Ad Hoc";
                                    }

                                    if (!sObjRouteBaseState.equals("Ad Hoc")) {
                                        // Get names from properties

                                        // Bug 293332
                                        String sTempStore = sObjRouteBasePolicy;
                                        sObjRouteBasePolicy = PropertyUtil.getSchemaProperty(context, sObjRouteBasePolicy);

                                        if (sObjRouteBasePolicy.equals("")) {
                                            arguments = new String[13];
                                            arguments[0] = "emxFramework.ProgramObject.eServicecommonCompleteTask.InvalidPolicy";
                                            arguments[1] = "5";
                                            arguments[2] = "State";
                                            arguments[3] = sTempStore;
                                            arguments[4] = "Type";
                                            arguments[5] = sRouteType;
                                            arguments[6] = "OType";
                                            arguments[7] = sObjType;
                                            arguments[8] = "OName";
                                            arguments[9] = sObjName;
                                            arguments[10] = "ORev";
                                            arguments[11] = sObjRev;
                                            arguments[12] = "";

                                            message = mailUtil.getMessage(context, arguments);
                                            MqlUtil.mqlCommand(context, "notice " + message + "");

                                            return 1;
                                        }
                                        // Bug 293332

                                        sTempStore = sObjRouteBaseState;
                                        // sObjRouteBaseState = FrameworkUtil.lookupStateName(context, sObjRouteBasePolicy, sObjRouteBaseState);
                                        sObjRouteBaseState = PropertyUtil.getSchemaProperty(context, "policy", sObjRouteBasePolicy, sObjRouteBaseState);
                                        if (sObjRouteBaseState.equals("")) {
                                            arguments = new String[13];
                                            arguments[0] = "emxFramework.ProgramObject.eServicecommonCompleteTask.InvalidState";
                                            arguments[1] = "5";
                                            arguments[2] = "Policy";
                                            arguments[3] = sTempStore;
                                            arguments[4] = "Type";
                                            arguments[5] = sRouteType;
                                            arguments[6] = "OType";
                                            arguments[7] = sObjType;
                                            arguments[8] = "OName";
                                            arguments[9] = sObjName;
                                            arguments[10] = "ORev";
                                            arguments[11] = sObjRev;
                                            arguments[12] = "";

                                            message = mailUtil.getMessage(context, arguments);
                                            MqlUtil.mqlCommand(context, "notice " + message + "");

                                            return 1;
                                        }
                                    }

                                    // If Route Base State is Ad Hoc or Route Base State and Policy are
                                    // same as object state and policy
                                    if ((sObjRouteBaseState.equals("Ad Hoc")) || (sObjRouteBaseState.equals(sObjCurrent) && sObjRouteBasePolicy.equals(sObjPolicy))) {
                                        // Set flag if Route still in work
                                        if (!sObjRouteCurrent.equals(DomainConstants.STATE_INBOX_TASK_COMPLETE)) {
                                            sBufInCompleteRoutes.append(sObjRouteType + " ");
                                            sBufInCompleteRoutes.append(sObjRouteName + " ");
                                            sBufInCompleteRoutes.append(sObjRouteRev + ",");
                                            // Bug 293332
                                            bPromoteObject = 0;
                                        }
                                    }
                                } // for

                                if (!sInCompleteRoutes.equals("")) {
                                    sBufRoutesInProcess.append(sObjType + " ");
                                    sBufRoutesInProcess.append(sObjName + " ");
                                    sBufRoutesInProcess.append(sObjRev + " : ");
                                    sBufRoutesInProcess.append(sInCompleteRoutes + "\n");
                                }

                                // Check if all the signatures are approved
                                if (sIsObjSatisfied.equals("FALSE")) {
                                    sBufObjectsNotSatisfied.append(sObjType + " ");
                                    sBufObjectsNotSatisfied.append(sObjName + " ");
                                    sBufObjectsNotSatisfied.append(sObjRev + "\n");
                                    // Bug 293332
                                    bPromoteObject = 0;
                                }

                                if (bPromoteObject == 1) {
                                    // TIGTK-17751 : Start
                                    // MqlUtil.mqlCommand(context, "promote bus " + sObjId + "");
                                    // DomainObject domainObject=DomainObject.newInstance(context, sObjId);
                                    BusinessObject boCADObject = new BusinessObject(sObjId);
                                    try {
                                        // domainObject.promote(context);
                                        boCADObject.promote(context);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    // TIGTK-17751 : End
                                    sBufPromotedObjects.append(sObjType + " ");
                                    sBufPromotedObjects.append(sObjName + " ");
                                    sBufPromotedObjects.append(sObjRev + "\n");
                                }
                            } // for

                            sInCompleteRoutes = sBufInCompleteRoutes.toString();
                            sRoutesInProcess = sBufRoutesInProcess.toString();
                            sObjectsNotSatisfied = sBufObjectsNotSatisfied.toString();
                            sPromotedObjects = sBufPromotedObjects.toString();

                            if (!(sObjectsNotSatisfied.equals("") && sRoutesInProcess.equals(""))) {
                                if (sRoutesInProcess.equals("")) {
                                    arguments = new String[3];
                                    arguments[0] = "emxFramework.ProgramObject.eServicecommonCompleteTask.None";
                                    arguments[1] = "0";
                                    arguments[2] = "";

                                    sRoutesInProcess = mailUtil.getMessage(context, arguments);
                                }
                                if (sObjectsNotSatisfied.equals("")) {
                                    arguments = new String[3];
                                    arguments[0] = "emxFramework.ProgramObject.eServicecommonCompleteTask.None";
                                    arguments[1] = "0";
                                    arguments[2] = "";
                                    sObjectsNotSatisfied = mailUtil.getMessage(context, arguments);
                                }

                                arguments = new String[19];
                                arguments[0] = sOwner;
                                arguments[1] = "emxFramework.ProgramObject.eServicecommonCompleteTask.SubjectNotComplete";
                                arguments[2] = "0";
                                arguments[3] = "emxFramework.ProgramObject.eServicecommonCompleteTask.MessageNotComplete";
                                arguments[4] = "6";
                                arguments[5] = "RType";
                                arguments[6] = sRouteType;
                                arguments[7] = "RName";
                                arguments[8] = sRouteName;
                                arguments[9] = "RRev";
                                arguments[10] = sRouteRev;
                                arguments[11] = "PromotedObj";
                                arguments[12] = sPromotedObjects;
                                arguments[13] = "RInProcess";
                                arguments[14] = sRoutesInProcess;
                                arguments[15] = "ONotApproved";
                                arguments[16] = sObjectsNotSatisfied;
                                arguments[17] = "";
                                arguments[18] = "";

                                mailUtil.sendNotificationToUser(context, arguments);
                            }
                            // TIGTK-7745 : Route Completion Notification CR, CA, MCA and CN : AB : 22/08/2017 : START
                        } else if (!(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION.equalsIgnoreCase(strObjectType) || TigerConstants.TYPE_PSS_CHANGEREQUEST.equalsIgnoreCase(strObjectType)
                                || ChangeConstants.TYPE_CHANGE_ACTION.equalsIgnoreCase(strObjectType) || TigerConstants.TYPE_PSS_CHANGENOTICE.equalsIgnoreCase(strObjectType))) {
                            // TIGTK-7745 : Route Completion Notification CR, CA, MCA and CN : AB : 22/08/2017 : END
                            arguments = new String[13];
                            arguments[0] = sOwner;
                            arguments[1] = "emxFramework.ProgramObject.eServicecommonCompleteTask.SubjectRouteComplete";
                            arguments[2] = "0";
                            arguments[3] = "emxFramework.ProgramObject.eServicecommonCompleteTask.MessageRouteComplete";
                            arguments[4] = "3";
                            arguments[5] = "RType";
                            arguments[6] = sRouteType;
                            arguments[7] = "RName";
                            arguments[8] = sRouteName;
                            arguments[9] = "RRev";
                            arguments[10] = sRouteRev;
                            arguments[11] = sRouteId;
                            arguments[12] = "";
                            String routeSymbolicName = FrameworkUtil.getAliasForAdmin(context, "type", Route.getType(context), true);
                            String mappedTreeName = UIMenu.getTypeToTreeNameMapping(routeSymbolicName);
                            String[] treeMenu = { mappedTreeName };
                            mailUtil.setTreeMenuName(context, treeMenu);
                            // Added for the bug no 335211 - Begin
                            String oldagentName = mailUtil.getAgentName(context, args);
                            String user = context.getUser();
                            mailUtil.setAgentName(context, new String[] { user });
                            mailUtil.sendNotificationToUser(context, arguments);
                            mailUtil.setAgentName(context, new String[] { oldagentName });
                            // Added for the bug no 335211 - Ends
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in completeTask: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw e;
        } finally {
            // Added for JIRA TIGTK-2874
            // ShadowAgent.popContext(context, null);
            // Added for JIRA TIGTK-2874
        }

        return 0;
    }// eof method

    public StringList getCAApprovalRouteTaskRejectedToList(Context context, String[] args) throws Exception {

        StringList routeOwnerList = new StringList();

        HashSet<String> personSet = new HashSet<>();

        try {
            PSS_enoECMChangeUtil_mxJPO changeUtil = new PSS_enoECMChangeUtil_mxJPO(context, args);
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String objectId = (String) programMap.get(DomainConstants.SELECT_ID);

            DomainObject domInboxTask = DomainObject.newInstance(context, objectId);
            String strRouteID = domInboxTask.getInfo(context, "from[" + DomainConstants.RELATIONSHIP_ROUTE_TASK + "].to.id");

            String strCAAssignee;
            DomainObject domRoute = new DomainObject(strRouteID);

            String strChangeActionID = domRoute.getInfo(context, "to[" + DomainConstants.RELATIONSHIP_OBJECT_ROUTE + "].from.id");

            DomainObject domChangeAction = new DomainObject(strChangeActionID);

            String strCAAssigneeID = domChangeAction.getInfo(context, "from[" + ChangeConstants.RELATIONSHIP_TECHNICAL_ASSIGNEE + "].to.id");

            if (strCAAssigneeID != null) {

                DomainObject domAssignee = new DomainObject(strCAAssigneeID);
                strCAAssignee = domAssignee.getName(context);

                personSet.add(strCAAssignee);

            }

            String strApprovalListMember = "from[" + DomainConstants.RELATIONSHIP_ROUTE_NODE + "].to.name";

            StringList slObjectSle = new StringList();
            slObjectSle.addElement(DomainConstants.SELECT_ID);
            slObjectSle.addElement(DomainConstants.SELECT_TYPE);
            slObjectSle.addElement(DomainConstants.SELECT_ORIGINATOR);
            slObjectSle.addElement(strApprovalListMember);
            slObjectSle.addElement(DomainConstants.ATTRIBUTE_ROUTE_BASE_PURPOSE);

            StringList slRelSle = new StringList(1);
            slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

            // For Change Order

            String sattrOriginator;
            Map mCOObj;

            MapList mlConnectedCO = domChangeAction.getRelatedObjects(context, TigerConstants.RELATIONSHIP_CHANGEACTION, TigerConstants.TYPE_CHANGEORDER, slObjectSle, slRelSle, true, false, (short) 1,
                    null, null);

            if (!mlConnectedCO.isEmpty()) {
                for (int k = 0; k < mlConnectedCO.size(); k++) {
                    mCOObj = (Map) mlConnectedCO.get(k);
                    sattrOriginator = (String) mCOObj.get(DomainConstants.SELECT_ORIGINATOR);

                    personSet.add(sattrOriginator);

                }

            }

            // For Route Object
            Map mRouteObj;
            String strRouteBasePurpose;
            String sstrApprovalListMemberName;
            MapList mlConnectedRoutes = domChangeAction.getRelatedObjects(context, DomainConstants.RELATIONSHIP_OBJECT_ROUTE, DomainConstants.TYPE_ROUTE, slObjectSle, slRelSle, false, true, (short) 1,
                    null, null);

            if (!mlConnectedRoutes.isEmpty()) {
                // StringList sApprovalListMemberNameList = new StringList();
                for (int k = 0; k < mlConnectedRoutes.size(); k++) {
                    mRouteObj = (Map) mlConnectedRoutes.get(k);
                    String sRouteID = (String) mRouteObj.get(DomainConstants.SELECT_ID);
                    DomainObject dRoute = new DomainObject(sRouteID);
                    strRouteBasePurpose = (String) dRoute.getAttributeValue(context, DomainConstants.ATTRIBUTE_ROUTE_BASE_PURPOSE);

                    if (strRouteBasePurpose.equalsIgnoreCase("Approval")) {
                        // Find Bug Issue : Priyanka Salunke : 27-Feb-2017
                        StringList sApprovalListMemberNameList = (StringList) changeUtil.getStringListFromMap(context, mRouteObj, strApprovalListMember);
                        for (int v = 0; v < sApprovalListMemberNameList.size(); v++) {
                            sstrApprovalListMemberName = (String) sApprovalListMemberNameList.get(v);
                            // PCM : TIGTK-3604 : 23/11/16 : AB : START
                            BusinessObject boPerson = new BusinessObject(DomainConstants.TYPE_PERSON, sstrApprovalListMemberName, "-", null);

                            if (boPerson.exists(context)) {
                                personSet.add(sstrApprovalListMemberName);
                            }
                            // PCM : TIGTK-3604 : 23/11/16 : AB : END
                        }

                    }

                }

            }

            routeOwnerList.addAll(personSet);

        } catch (Exception e) {
            throw e;
        }

        return (routeOwnerList);
    }

    /**
     * This method is
     * @param context
     * @param holds
     *            no arguments
     * @returns StringList ofToList for Mail Notification of MCA task Rejection
     * @throws Exception
     *             if the operation fails
     */

    public StringList getMCAApprovalRouteTaskRejectedToList(Context context, String[] args) throws Exception {

        StringList mailToList = new StringList();
        HashSet<String> personSet = new HashSet<>();

        try {
            PSS_enoECMChangeUtil_mxJPO changeUtil = new PSS_enoECMChangeUtil_mxJPO(context, args);
            // Get the Inbox Task Object Id
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String objectId = (String) programMap.get(DomainConstants.SELECT_ID);
            DomainObject domInboxTask = DomainObject.newInstance(context, objectId);
            // get the Route id
            String strRouteID = domInboxTask.getInfo(context, "from[" + DomainConstants.RELATIONSHIP_ROUTE_TASK + "].to.id");
            DomainObject domRoute = new DomainObject(strRouteID);

            String strChangeActionID = domRoute.getInfo(context, "to[" + DomainConstants.RELATIONSHIP_OBJECT_ROUTE + "].from.id");
            DomainObject domMfgChangeAction = new DomainObject(strChangeActionID);

            // Get Assignee of Manufacturing Change Action
            String strMCAAssigneeID = domMfgChangeAction.getInfo(context, "from[" + ChangeConstants.RELATIONSHIP_TECHNICAL_ASSIGNEE + "].to.name");

            if (strMCAAssigneeID != null) {
                personSet.add(strMCAAssigneeID);
            }

            // Get person which is connected with Route Node Relationship from Manufacturing Change Action
            String strApprovalListMember = "from[" + DomainConstants.RELATIONSHIP_ROUTE_NODE + "].to.name";

            StringList slObjectSle = new StringList();
            slObjectSle.addElement(DomainConstants.SELECT_ID);
            slObjectSle.addElement(DomainConstants.SELECT_TYPE);
            slObjectSle.addElement(DomainConstants.SELECT_ORIGINATOR);
            slObjectSle.addElement(strApprovalListMember);
            slObjectSle.addElement(DomainConstants.ATTRIBUTE_ROUTE_BASE_PURPOSE);

            StringList slRelSle = new StringList(1);
            slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

            // For Route Object get all Person which is connected with Route
            String sstrApprovalListMemberName;
            // StringList sApprovalListMemberNameList = new StringList();
            MapList mlConnectedRoutes = domMfgChangeAction.getRelatedObjects(context, DomainConstants.RELATIONSHIP_OBJECT_ROUTE, DomainConstants.TYPE_ROUTE, slObjectSle, slRelSle, false, true,
                    (short) 1, null, null);

            if (!mlConnectedRoutes.isEmpty()) {
                for (int k = 0; k < mlConnectedRoutes.size(); k++) {
                    Map mRouteObj = (Map) mlConnectedRoutes.get(k);
                    String sRouteID = (String) mRouteObj.get(DomainConstants.SELECT_ID);
                    DomainObject dRoute = new DomainObject(sRouteID);
                    String strRouteBasePurpose = (String) dRoute.getAttributeValue(context, DomainConstants.ATTRIBUTE_ROUTE_BASE_PURPOSE);

                    if (strRouteBasePurpose.equalsIgnoreCase("Approval")) {
                        // Find Bug Issue : Priyanka Salunke : 27-Feb-2017
                        StringList sApprovalListMemberNameList = (StringList) changeUtil.getStringListFromMap(context, mRouteObj, strApprovalListMember);

                        for (int v = 0; v < sApprovalListMemberNameList.size(); v++) {
                            sstrApprovalListMemberName = (String) sApprovalListMemberNameList.get(v);

                            // PCM : TIGTK-3628 : 1/12/16 : AB : START
                            BusinessObject boPerson = new BusinessObject(DomainConstants.TYPE_PERSON, sstrApprovalListMemberName, "-", null);

                            if (boPerson.exists(context)) {
                                personSet.add(sstrApprovalListMemberName);
                            }
                            // PCM : TIGTK-3628 : 1/12/16 : AB : END

                        }
                    }
                }
            }

            mailToList.addAll(personSet);
        } catch (Exception e) {
            throw e;
        }

        return (mailToList);
    }

    /*
     * This Method for get the PsersonList and Creator of CN into toLsit when approval task is rejected Then Notification is send them
     * 
     * @param context
     * 
     * @param args
     * 
     * @throws Exception Owner : Vishal B
     */

    public StringList getCNApprovalRouteTaskRejectedToList(Context context, String[] args) throws Exception {

        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        StringList returlToList = new StringList();
        String taskObjectId = (String) programMap.get("id");

        DomainObject domTask = new DomainObject(taskObjectId);
        String RouteObjectId = domTask.getInfo(context, "from[" + DomainConstants.RELATIONSHIP_ROUTE_TASK + "].to.id");

        DomainObject domRoute = new DomainObject(RouteObjectId);
        String cnObjectId = domRoute.getInfo(context, "to[" + DomainConstants.RELATIONSHIP_OBJECT_ROUTE + "].from.id");
        DomainObject CNDOM = new DomainObject(cnObjectId);

        // MapList mlPersonList = new MapList();
        Pattern typePattern = new Pattern(DomainConstants.TYPE_PERSON);
        Pattern relPattern = new Pattern(DomainConstants.RELATIONSHIP_ROUTE_NODE);
        StringList objSelects = new StringList();
        objSelects.addElement(DomainConstants.SELECT_ID);

        MapList mlPersonList = domRoute.getRelatedObjects(context, relPattern.getPattern(), // relationship pattern
                typePattern.getPattern(), // object pattern
                objSelects, // object selects
                null, // relationship selects
                false, // to direction
                true, // from direction
                (short) 1, // recursion level
                null, // object where clause
                null, 0);

        HashSet<String> allPersonList = new HashSet<String>();
        Iterator itr = mlPersonList.iterator();
        while (itr.hasNext()) {
            Map checkMap = (Map) itr.next();
            String personId = (String) checkMap.get("id");
            DomainObject domPerson = new DomainObject(personId);
            String personName = domPerson.getInfo(context, DomainConstants.SELECT_NAME);
            BusinessObject boPerson = new BusinessObject(DomainConstants.TYPE_PERSON, personName, "-", null);

            if (boPerson.exists(context)) {
                allPersonList.add(personName);
            }

        }

        String getCNCreator = CNDOM.getInfo(context, DomainConstants.SELECT_OWNER);
        allPersonList.add(getCNCreator);
        returlToList.addAll(allPersonList);

        return returlToList;

    }

    /**
     * This method is used for check current task is related to ChangeManger then check status of all task related Evaluation Route of CR Date: 15/12/2016 : ADDED For TIGTK-3813
     * @author abhalani
     * @param context
     * @param args
     *            ID of InboxTask Object
     * @return
     * @throws Exception
     */

    public int checkStatusOfAllTaskForCREvaluationRoute(Context context, String[] args) throws Exception {
        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }

        int intReturnValue;
        try {
            intReturnValue = 0;
            StringList slNotCompletedTask = new StringList();
            String strObjectID = args[0];
            DomainObject domInboxTask = new DomainObject(strObjectID);

            SelectList objectSelects = new SelectList();
            objectSelects.addElement("attribute[" + DomainConstants.ATTRIBUTE_ROUTE_NODE_ID + "]");
            objectSelects.addElement("from[Project Task].to.name");

            SelectList relSelects = new SelectList();
            relSelects.addElement("from.to[" + DomainConstants.RELATIONSHIP_OBJECT_ROUTE + "].from.id");
            relSelects.addElement("from.to[" + DomainConstants.RELATIONSHIP_OBJECT_ROUTE + "].from.current");
            relSelects.addElement("from.to[" + DomainConstants.RELATIONSHIP_OBJECT_ROUTE + "].from.type");
            relSelects.addElement("from.id");

            // Get related Route of InboxTask
            Map objectMap = domInboxTask.getInfo(context, objectSelects);

            String sRouteNodeIDOnIB = (String) objectMap.get("attribute[" + DomainConstants.ATTRIBUTE_ROUTE_NODE_ID + "]");

            String relationshipIds[] = new String[1];
            relationshipIds[0] = sRouteNodeIDOnIB;
            MapList relMapList = DomainRelationship.getInfo(context, relationshipIds, relSelects);

            Map relMap = (Map) relMapList.get(0);
            String sRouteId = (String) relMap.get("from.id");
            String strChangeCurrent = (String) relMap.get("from.to[" + DomainConstants.RELATIONSHIP_OBJECT_ROUTE + "].from.current");
            String strChangeObjectType = (String) relMap.get("from.to[" + DomainConstants.RELATIONSHIP_OBJECT_ROUTE + "].from.type");
            String strChangeObjectID = (String) relMap.get("from.to[" + DomainConstants.RELATIONSHIP_OBJECT_ROUTE + "].from.id");

            // Check If Route connected with ChangeRequest object and it is between "In Review" state and "In Process" state then check further for connected Task
            if (TigerConstants.TYPE_PSS_CHANGEREQUEST.equalsIgnoreCase(strChangeObjectType) && ChangeConstants.STATE_CHANGEREQUEST_INREVIEW.equalsIgnoreCase(strChangeCurrent)) {

                boolean hasChangeManagerRole = PersonUtil.hasAssignment(context, TigerConstants.ROLE_PSS_CHANGE_COORDINATOR);
                if (!hasChangeManagerRole) {
                    intReturnValue = 0;
                } else {
                    DomainObject domRoute = new DomainObject(sRouteId);

                    // Get the All connected RouteNode relationship of Route on 'In Review' state of Change Request
                    // objectSelects.removeAll(objectSelects);
                    // relSelects.removeAll(relSelects);

                    // Mofified for Find Bug Issue : START : PS
                    objectSelects.clear();
                    relSelects.clear();
                    // Mofified for Find Bug Issue : END
                    objectSelects.add(DomainObject.SELECT_ID);
                    objectSelects.add(DomainObject.SELECT_NAME);
                    objectSelects.add(DomainObject.SELECT_CURRENT);
                    objectSelects.addElement("from[" + DomainConstants.RELATIONSHIP_PROJECT_TASK + "].to.name");
                    objectSelects.add("attribute[" + DomainConstants.ATTRIBUTE_ROUTE_TASK_USER + "]");

                    // Get Related Route Node Relationship
                    MapList mlRouteNodeRel = domRoute.getRelatedObjects(context, // context // here
                            DomainConstants.RELATIONSHIP_ROUTE_TASK, // relationship pattern
                            DomainConstants.TYPE_INBOX_TASK, // object pattern
                            objectSelects, // object selects
                            relSelects, // relationship selects
                            false, // to direction
                            true, // from direction
                            (short) 0, // recursion level
                            null, // object where clause
                            null, // relationship where clause
                            (short) 0, false, // checkHidden
                            true, // preventDuplicates
                            (short) 0, // pageSize
                            null, null, null, null);

                    if (mlRouteNodeRel.size() != 0) {
                        int intSizeOfList = mlRouteNodeRel.size();
                        for (int j = 0; j < intSizeOfList; j++) {
                            Map mapITObject = (Map) mlRouteNodeRel.get(j);
                            String strTaskID = (String) mapITObject.get(DomainConstants.SELECT_ID);
                            String strITAssignneName = (String) mapITObject.get("from[" + DomainConstants.RELATIONSHIP_PROJECT_TASK + "].to.name");
                            String strRouteTaskUser = (String) mapITObject.get("attribute[" + DomainConstants.ATTRIBUTE_ROUTE_TASK_USER + "]");
                            String strITState = (String) mapITObject.get(DomainConstants.SELECT_CURRENT);

                            BusinessObject boPerson = new BusinessObject(DomainConstants.TYPE_PERSON, strITAssignneName, "-", null);
                            boolean bCMRole = false;

                            // Check whether task is assigned to ChangeManager Role or Person which has assigned this role
                            if (boPerson.exists(context)) {
                                Person person = new Person(strITAssignneName);
                                person.open(context);
                                bCMRole = person.isAssigned(context, TigerConstants.ROLE_PSS_CHANGE_COORDINATOR);
                            } else if (TigerConstants.ROLE_PSS_CHANGE_COORDINATOR.equalsIgnoreCase(PropertyUtil.getSchemaProperty(context, strRouteTaskUser))) {
                                bCMRole = true;
                            }

                            // Filter the task which is not Completed and assigned to ChangeManager
                            if (!bCMRole && !strITState.equalsIgnoreCase(DomainConstants.STATE_INBOX_TASK_COMPLETE) && !strObjectID.equalsIgnoreCase(strTaskID)) {
                                slNotCompletedTask.add(strTaskID);
                            }
                        }
                    }

                    // Give alert if any task is pending for action which is not assigned to Change Manager
                    if (slNotCompletedTask.size() > 0) {
                        String strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSS_EnterpriseChangeMgt.Alert.ApproveNonCMTask");

                        MqlUtil.mqlCommand(context, "notice $1", strMessage);
                        intReturnValue = 1;
                    }

                }
                // PCM : TIGTK-4434 : 02/03/2017 : AB : START
            } else if (TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION.equalsIgnoreCase(strChangeObjectType)) {
                DomainObject domMCA = new DomainObject(strChangeObjectID);
                // TIGTK-10708 : START
                // Get the Connected plant of MCO
                String strPlantName = domMCA.getInfo(context,
                        "to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].from.from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGRELATEDPLANT + "].to.name");

                // Get the Connected plant members of MCO
                StringList strMCOPlantMembers = domMCA.getInfoList(context, "to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].from.from["
                        + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGRELATEDPLANT + "].to.from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS + "].to.name");

                // Get Plant Members connected To Program-Project of MCO
                String strPlantMembersConnectedToProjectSelectable = "to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA
                        + "].from.from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPLANTMEMBERTOPROGPROJ + "|attribute[" + TigerConstants.ATTRIBUTE_PSS_PLANTNAME + "]=='" + strPlantName
                        + "'].to.name";

                String strMember = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", strChangeObjectID, strPlantMembersConnectedToProjectSelectable);

                String[] sMembers = strMember.split(",");
                String strContextUser = context.getUser();
                boolean flag = false;
                for (int j = 0; j < sMembers.length; j++) {
                    String strPlantMember = sMembers[j];
                    if (strPlantMember.equalsIgnoreCase(strContextUser)) {
                        flag = true;
                        break;
                    }
                }

                // If current user is Member of connected Plant and also member from PlantMembers connected To Program-Project of MCO
                if (!strMCOPlantMembers.contains(strContextUser) || flag == false) {
                    String strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSS_EnterpriseChangeMgt.Alert.ApproveMCATask");
                    MqlUtil.mqlCommand(context, "notice $1", strMessage);
                    intReturnValue = 1;
                }
                // TIGTK-10708 : END
            }
            // PCM : TIGTK-4434 : 02/03/2017 : AB : END
            // PCM : TIGTK-10708 : 28/10/2017 : AB : START
            else if (TigerConstants.TYPE_PSS_CHANGENOTICE.equalsIgnoreCase(strChangeObjectType)) {
                DomainObject domCN = new DomainObject(strChangeObjectID);
                // TIGTK-10708 : START
                // Get the Connected plant of connected MCO to ChangeNotice
                String strPlantName = domCN.getInfo(context,
                        "to[" + TigerConstants.RELATIONSHIP_PSS_RELATEDCN + "].from.from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGRELATEDPLANT + "].to.name");

                // Get the Connected plant members of connected MCO to ChangeNotice
                StringList strMCOPlantMembers = domCN.getInfoList(context, "to[" + TigerConstants.RELATIONSHIP_PSS_RELATEDCN + "].from.from["
                        + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGRELATEDPLANT + "].to.from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS + "].to.name");

                // Get Plant Members connected To Program-Project of connected MCO to ChangeNotice
                String strPlantMembersConnectedToProjectSelectable = "to[" + TigerConstants.RELATIONSHIP_PSS_RELATEDCN + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA
                        + "].from.from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPLANTMEMBERTOPROGPROJ + "|attribute[" + TigerConstants.ATTRIBUTE_PSS_PLANTNAME + "]=='" + strPlantName
                        + "'].to.name";

                String strMember = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", strChangeObjectID, strPlantMembersConnectedToProjectSelectable);

                String[] sMembers = strMember.split(",");
                String strContextUser = context.getUser();
                boolean flag = false;
                for (int j = 0; j < sMembers.length; j++) {
                    String strPlantMember = sMembers[j];
                    if (strPlantMember.equalsIgnoreCase(strContextUser)) {
                        flag = true;
                        break;
                    }
                }
                // If current user is Member of connected Plant and also member from PlantMembers connected To Program-Project of MCO
                if (!strMCOPlantMembers.contains(strContextUser) || flag == false) {
                    String strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSS_EnterpriseChangeMgt.Alert.ApproveMCATask");
                    MqlUtil.mqlCommand(context, "notice $1", strMessage);
                    intReturnValue = 1;
                }
                // TIGTK-10708 : END
            }
            // PCM : TIGTK-10708 : 28/10/2017 : AB : END
        } catch (Exception e) {
            // TODO Auto-generated catch block
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkStatusOfAllTaskForCREvaluationRoute: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw e;
        }

        return intReturnValue;

    }

}// eof class
