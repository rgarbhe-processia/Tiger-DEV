
/*
 * JPO is defined for Override the OOTB lifecycle process and Clone of emxLifecycle_mxJPO MOdified : TIGTK-5775 : Vishalb
 *
 *
 */
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.State;
import matrix.db.StateList;
import matrix.util.StringList;
import pss.constants.TigerConstants;

import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeConstants;
import com.matrixone.apps.common.Lifecycle;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.XSSUtil;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.apps.framework.lifecycle.LifeCyclePolicyDetails;
import com.matrixone.apps.framework.ui.UIUtil;

/**
 * This JPO includes the code related to the Lifecycle Mass Approval functionality
 */
public class PSS_emxLifecycle_mxJPO extends emxLifecycle_mxJPO {

    // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PSS_emxLifecycle_mxJPO.class);

    // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - END
    private static final String SELECT_ATTRIBUTE_ACTUAL_COMPLETION_DATE = "attribute[" + DomainObject.ATTRIBUTE_ACTUAL_COMPLETION_DATE + "]";

    private static final String SELECT_ATTRIBUTE_SCHEDULED_COMPLETION_DATE = "attribute[" + DomainObject.ATTRIBUTE_SCHEDULED_COMPLETION_DATE + "]";

    private static final String SELECT_ATTRIBUTE_TITLE = "attribute[" + DomainObject.ATTRIBUTE_TITLE + "]";

    private static final String SELECT_ATTRIBUTE_COMMENTS = "attribute[" + DomainObject.ATTRIBUTE_COMMENTS + "]";

    private static final String SELECT_ATTRIBUTE_ROUTE_INSTRUCTIONS = "attribute[" + DomainObject.ATTRIBUTE_ROUTE_INSTRUCTIONS + "]";

    private static final String SELECT_ATTRIBUTE_APPROVAL_STATUS = "attribute[" + DomainObject.ATTRIBUTE_APPROVAL_STATUS + "]";

    private static final String SELECT_ATTRIBUTE_ROUTE_NODE_ID = "attribute[" + DomainObject.ATTRIBUTE_ROUTE_NODE_ID + "]";

    private static final String SELECT_ATTRIBUTE_ROUTE_TASK_USER = "attribute[" + DomainObject.ATTRIBUTE_ROUTE_TASK_USER + "]";

    private static final String SELECT_ROUTE_TASK_ASSIGNEE_ID = "from[" + DomainObject.RELATIONSHIP_PROJECT_TASK + "].to.id";

    private static final String SELECT_ROUTE_TASK_ASSIGNEE_TYPE = "from[" + DomainObject.RELATIONSHIP_PROJECT_TASK + "].to.type";

    private static final String SELECT_ROUTE_TASK_ASSIGNEE_NAME = "from[" + DomainObject.RELATIONSHIP_PROJECT_TASK + "].to.name";

    private static final String SELECT_REL_ATTRIBUTE_ROUTE_NODE_ID = DomainRelationship.getAttributeSelect(DomainObject.ATTRIBUTE_ROUTE_NODE_ID);

    private static final String SELECT_REL_ATTRIBUTE_APPROVAL_STATUS = DomainRelationship.getAttributeSelect(DomainObject.ATTRIBUTE_APPROVAL_STATUS);

    private static final String SELECT_REL_ATTRIBUTE_TITLE = DomainRelationship.getAttributeSelect(DomainObject.ATTRIBUTE_TITLE);

    private static final String SELECT_REL_ATTRIBUTE_ROUTE_INSTRUCTIONS = DomainRelationship.getAttributeSelect(DomainObject.ATTRIBUTE_ROUTE_INSTRUCTIONS);

    private static final String SELECT_REL_ATTRIBUTE_COMMENTS = DomainRelationship.getAttributeSelect(DomainObject.ATTRIBUTE_COMMENTS);

    private static final String SELECT_REL_ATTRIBUTE_SCHEDULED_COMPLETION_DATE = DomainRelationship.getAttributeSelect(DomainObject.ATTRIBUTE_SCHEDULED_COMPLETION_DATE);

    private static final String SELECT_REL_ATTRIBUTE_ROUTE_TASK_USER = DomainRelationship.getAttributeSelect(DomainObject.ATTRIBUTE_ROUTE_TASK_USER);

    private static final String SELECT_ATTRIBUTE_ROUTE_STATUS = "attribute[" + DomainObject.ATTRIBUTE_ROUTE_STATUS + "]";

    private static final String SELECT_ATTRIBUTE_ROUTE_SEQUENCE = "attribute[" + DomainObject.ATTRIBUTE_ROUTE_SEQUENCE + "]";

    private static final String INFO_TYPE_ACTIVATED_TASK = "activatedTask";

    private static final String INFO_TYPE_DEFINED_TASK = "definedTask";

    private static final String INFO_TYPE_SIGNATURE = "signature";

    // added for IR - 043921V6R2011
    public static final String SELECT_OWNING_ORG_ID = "from[" + DomainConstants.RELATIONSHIP_INITIATING_ROUTE_TEMPLATE + "].to.to[" + DomainConstants.RELATIONSHIP_OWNING_ORGANIZATION + "].from.id";

    /**
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @throws Exception
     *             if the operation fails
     * @grade 0
     */
    public PSS_emxLifecycle_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
    }

    /**
     * Method is used to populate the data for Tasks/Signature table in advance lifecycle page
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            The packed argument for JPO, containing the program map. This program map will have request parameter information, objectId and information about the UI table object.
     * @return MapList of data
     * @throws Exception
     */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getCurrentTaskSignaturesOnObject(Context context, String[] args) throws Exception {
        try {
            // To hold the table data
            MapList mlTableData = new MapList();

            // Get object list information from packed arguments
            Map programMap = (Map) JPO.unpackArgs(args);

            // Get object id
            String strObjectId = (String) programMap.get("objectId");

            DomainObject dmoObject = null;
            DomainObject dmoRoute = null;

            String strCurrentObjectId = null;
            String strObjectType = null;
            String strObjectStateName = null;
            String strObjectPolicyName = null;
            String strObjectSymbolicStateName = null;
            String strSymbolicPolicyName = null;
            String strRelPattern = null;
            String strTypePattern = null;
            String strObjectWhere = null;
            String strRelWhere = null;
            String strRouteId = null;
            String strRouteNodeId = null;
            String strRouteStatus = null;
            StringList slRelSelect = null;
            StringList slBusSelect = null;
            short nRecurseToLevel = (short) 1;
            final boolean GET_TO = true;
            final boolean GET_FROM = true;
            boolean isActivated = false;
            Map mapRouteInfo = null;
            Map mapObjectInfo = null;
            Map mapTemp = null;
            Map mapTemp2 = null;
            Map mapConfigurableParameters = null;
            MapList mlDefinedTasksOnRoute = null;
            MapList mlActivatedTasksOnRoute = null;
            MapList mlTemp = null;
            MapList mlRoutes = null;

            // For i18nNow string formation
            i18nNow loc = new i18nNow();
            String strLanguage = (String) programMap.get("languageStr");
            final String RESOURCE_BUNDLE = "emxFrameworkStringResource";
            final String STRING_PENDING = (String) loc.GetString(RESOURCE_BUNDLE, strLanguage, "emxFramework.LifecycleTasks.Pending");
            final String STRING_AWAITING_APPROVAL = (String) loc.GetString(RESOURCE_BUNDLE, strLanguage, "emxFramework.LifecycleTasks.AwaitingApproval");

            // Create maplist to hold related objects information
            MapList mlAllObjectsInfo = new MapList();

            // Find the configurable parameters
            dmoObject = new DomainObject(strObjectId);
            strObjectType = dmoObject.getInfo(context, DomainObject.SELECT_TYPE);
            Lifecycle lifecycle = new Lifecycle();
            mapConfigurableParameters = lifecycle.getConfigurableParameters(context, strObjectType);

            // If configurable parameters are passed, expand the object to get all those related objects' id
            if (mapConfigurableParameters != null) {
                strRelPattern = (String) mapConfigurableParameters.get("Relationships");
                strTypePattern = "*";
                slRelSelect = new StringList();
                nRecurseToLevel = (short) 1;
                slBusSelect = new StringList();
                slBusSelect.add(DomainObject.SELECT_ID);
                slBusSelect.add(DomainObject.SELECT_NAME);
                slBusSelect.add(DomainObject.SELECT_TYPE);
                slBusSelect.add(DomainObject.SELECT_CURRENT);
                slBusSelect.add(DomainObject.SELECT_POLICY);

                // Decide direction of expand
                boolean isGetTo = false;
                boolean isGetFrom = false;
                if ("to".equals((String) mapConfigurableParameters.get("Direction"))) {
                    isGetTo = false;
                    isGetFrom = true;
                } else if ("from".equals((String) mapConfigurableParameters.get("Direction"))) {
                    isGetTo = true;
                    isGetFrom = false;
                } else if ("both".equals((String) mapConfigurableParameters.get("Direction"))) {
                    isGetTo = true;
                    isGetFrom = true;
                }

                MapList mlRelatedObjects = dmoObject.getRelatedObjects(context, strRelPattern, strTypePattern, slBusSelect, slRelSelect, isGetTo, isGetFrom, nRecurseToLevel, null, null, 0);
                // Set marker inside the maps to identify this is related object
                Map mapRelatedObjectInfo = null;
                for (Iterator itrRelatedObjects = mlRelatedObjects.iterator(); itrRelatedObjects.hasNext();) {
                    mapRelatedObjectInfo = (Map) itrRelatedObjects.next();
                    mapRelatedObjectInfo.put("isRelatedObject", "true");
                }

                mlAllObjectsInfo.addAll(mlRelatedObjects);

            }

            // Get context object information and add it to the related objects map marking it is not related object
            dmoObject = new DomainObject(strObjectId);
            slBusSelect = new StringList();
            slBusSelect.add(DomainObject.SELECT_NAME);
            slBusSelect.add(DomainObject.SELECT_TYPE);
            slBusSelect.add(DomainObject.SELECT_CURRENT);
            slBusSelect.add(DomainObject.SELECT_POLICY);
            mapObjectInfo = dmoObject.getInfo(context, slBusSelect);
            mapObjectInfo.put("isRelatedObject", "false");
            mapObjectInfo.put(DomainObject.SELECT_ID, strObjectId);

            // Add this object into the all objects list for processing hence forward
            mlAllObjectsInfo.add(mapObjectInfo);
            mapObjectInfo = null;

            // Process each object so found
            // For each object find the route tasks for the state-based routes for current state of the object
            for (Iterator itrAllObjects = mlAllObjectsInfo.iterator(); itrAllObjects.hasNext();) {
                mapObjectInfo = (Map) itrAllObjects.next();

                // Get object information
                strCurrentObjectId = (String) mapObjectInfo.get(DomainObject.SELECT_ID);
                strObjectStateName = (String) mapObjectInfo.get(DomainObject.SELECT_CURRENT);
                strObjectPolicyName = (String) mapObjectInfo.get(DomainObject.SELECT_POLICY);

                // Get symbolic names of object state and policy
                strObjectSymbolicStateName = FrameworkUtil.reverseLookupStateName(context, strObjectPolicyName, strObjectStateName);
                strSymbolicPolicyName = FrameworkUtil.getAliasForAdmin(context, "Policy", strObjectPolicyName, false);

                dmoObject = new DomainObject(strCurrentObjectId);

                // Get state based route objects for this object
                strRelPattern = DomainObject.RELATIONSHIP_OBJECT_ROUTE;
                strTypePattern = DomainObject.TYPE_ROUTE;
                slRelSelect = new StringList();
                nRecurseToLevel = (short) 1;
                slBusSelect = new StringList();
                slBusSelect.add(DomainObject.SELECT_ID);
                slBusSelect.add(DomainObject.SELECT_NAME);
                slBusSelect.add(SELECT_ATTRIBUTE_ROUTE_STATUS);
                strRelWhere = "attribute[" + DomainObject.ATTRIBUTE_ROUTE_BASE_POLICY + "]=='" + strSymbolicPolicyName + "' && attribute[" + DomainObject.ATTRIBUTE_ROUTE_BASE_STATE + "]=='"
                        + strObjectSymbolicStateName + "'";

                mlRoutes = dmoObject.getRelatedObjects(context, strRelPattern, strTypePattern, slBusSelect, slRelSelect, !GET_TO, GET_FROM, nRecurseToLevel, strObjectWhere, strRelWhere);

                // Find all the tasks on each route
                for (Iterator itrRoutes = mlRoutes.iterator(); itrRoutes.hasNext();) {
                    // Find the route object
                    mapRouteInfo = (Map) itrRoutes.next();
                    strRouteId = (String) mapRouteInfo.get(DomainObject.SELECT_ID);
                    strRouteStatus = (String) mapRouteInfo.get(SELECT_ATTRIBUTE_ROUTE_STATUS);
                    dmoRoute = new DomainObject(strRouteId);

                    // Find the tasks defined on the route object, using Route Node relationship
                    strRelPattern = DomainObject.RELATIONSHIP_ROUTE_NODE;
                    strTypePattern = DomainObject.TYPE_PERSON + "," + DomainObject.TYPE_ROUTE_TASK_USER;
                    slBusSelect = new StringList();
                    slBusSelect.add(DomainObject.SELECT_ID);
                    slBusSelect.add(DomainObject.SELECT_TYPE);
                    slBusSelect.add(DomainObject.SELECT_NAME);
                    slRelSelect = new StringList();
                    slRelSelect.add(SELECT_REL_ATTRIBUTE_TITLE);
                    slRelSelect.add(SELECT_REL_ATTRIBUTE_APPROVAL_STATUS);
                    slRelSelect.add(SELECT_REL_ATTRIBUTE_ROUTE_NODE_ID);
                    slRelSelect.add(SELECT_REL_ATTRIBUTE_SCHEDULED_COMPLETION_DATE);
                    slRelSelect.add(SELECT_REL_ATTRIBUTE_ROUTE_INSTRUCTIONS);
                    slRelSelect.add(SELECT_REL_ATTRIBUTE_COMMENTS);
                    slRelSelect.add(SELECT_REL_ATTRIBUTE_ROUTE_TASK_USER);
                    nRecurseToLevel = (short) 1;
                    strObjectWhere = "";
                    strRelWhere = "";
                    mlDefinedTasksOnRoute = dmoRoute.getRelatedObjects(context, strRelPattern, strTypePattern, slBusSelect, slRelSelect, !GET_TO, GET_FROM, nRecurseToLevel, strObjectWhere,
                            strRelWhere);

                    // Find the tasks connected to route object, using Route Task relationship
                    strRelPattern = DomainObject.RELATIONSHIP_ROUTE_TASK;
                    strTypePattern = DomainObject.TYPE_INBOX_TASK;
                    slBusSelect = new StringList();
                    slBusSelect.add(DomainObject.SELECT_ID);
                    slBusSelect.add(DomainObject.SELECT_NAME);
                    slBusSelect.add(DomainObject.SELECT_TYPE);
                    slBusSelect.add(DomainObject.SELECT_OWNER);
                    slBusSelect.add(DomainObject.SELECT_CURRENT);
                    slBusSelect.add(SELECT_ATTRIBUTE_TITLE);
                    slBusSelect.add(SELECT_ATTRIBUTE_APPROVAL_STATUS);
                    slBusSelect.add(SELECT_ATTRIBUTE_ROUTE_NODE_ID);
                    slBusSelect.add(SELECT_ATTRIBUTE_SCHEDULED_COMPLETION_DATE);
                    slBusSelect.add(SELECT_ATTRIBUTE_ACTUAL_COMPLETION_DATE);
                    slBusSelect.add(SELECT_ATTRIBUTE_COMMENTS);
                    slBusSelect.add(SELECT_ATTRIBUTE_ROUTE_INSTRUCTIONS);
                    slBusSelect.add(SELECT_ROUTE_TASK_ASSIGNEE_ID); // To find if it person is connected
                    slBusSelect.add(SELECT_ROUTE_TASK_ASSIGNEE_TYPE);
                    slBusSelect.add(SELECT_ROUTE_TASK_ASSIGNEE_NAME);
                    slBusSelect.add(SELECT_ATTRIBUTE_ROUTE_TASK_USER);
                    slRelSelect = new StringList();
                    nRecurseToLevel = (short) 1;
                    strObjectWhere = "";
                    strRelWhere = "";
                    mlActivatedTasksOnRoute = dmoRoute.getRelatedObjects(context, strRelPattern, strTypePattern, slBusSelect, slRelSelect, GET_TO, !GET_FROM, nRecurseToLevel, strObjectWhere,
                            strRelWhere);

                    // Filter the partial tasks created due to Resume Process
                    mlTemp = new MapList();
                    for (Iterator itrActiveTasks = mlActivatedTasksOnRoute.iterator(); itrActiveTasks.hasNext();) {
                        mapTemp = (Map) itrActiveTasks.next();

                        // If person is connected then it is not partial task
                        if (mapTemp.get(SELECT_ROUTE_TASK_ASSIGNEE_ID) != null) {
                            mlTemp.add(mapTemp);
                        }
                    }
                    mlActivatedTasksOnRoute = mlTemp;

                    // Filter out the defined tasks which are already active
                    mlTemp = new MapList();
                    for (Iterator itrDefinedTasks = mlDefinedTasksOnRoute.iterator(); itrDefinedTasks.hasNext();) {
                        mapTemp = (Map) itrDefinedTasks.next();

                        // Find the id of this relationship
                        strRouteNodeId = (String) mapTemp.get(SELECT_REL_ATTRIBUTE_ROUTE_NODE_ID);

                        // Check if the task with this id is already activated
                        isActivated = false;
                        for (Iterator itrActiveTasks = mlActivatedTasksOnRoute.iterator(); itrActiveTasks.hasNext();) {
                            mapTemp2 = (Map) itrActiveTasks.next();

                            if (strRouteNodeId != null && strRouteNodeId.equals((String) mapTemp2.get(SELECT_ATTRIBUTE_ROUTE_NODE_ID))) {
                                isActivated = true;
                                break;
                            }
                        }

                        if (!isActivated) {
                            mlTemp.add(mapTemp);
                        }
                    }
                    mlDefinedTasksOnRoute = mlTemp;

                    // Form the final task list of the route object, we want the information to be present in final list with the same key in map

                    // Add all the active tasks on to the final list
                    for (Iterator itrActiveTasks = mlActivatedTasksOnRoute.iterator(); itrActiveTasks.hasNext();) {
                        mapTemp = (Map) itrActiveTasks.next();

                        mapTemp2 = new HashMap();
                        mapTemp2.put("name", mapTemp.get(DomainObject.SELECT_NAME));
                        mapTemp2.put("title", mapTemp.get(SELECT_ATTRIBUTE_TITLE));
                        mapTemp2.put("approvalStatus", mapTemp.get(SELECT_ATTRIBUTE_APPROVAL_STATUS));
                        mapTemp2.put("routeNodeId", mapTemp.get(SELECT_ATTRIBUTE_ROUTE_NODE_ID));
                        mapTemp2.put("taskId", mapTemp.get(DomainObject.SELECT_ID));
                        mapTemp2.put("routeId", strRouteId);
                        mapTemp2.put("routeStatus", strRouteStatus);
                        mapTemp2.put("infoType", INFO_TYPE_ACTIVATED_TASK);
                        mapTemp2.put("currentState", mapTemp.get(DomainObject.SELECT_CURRENT));
                        mapTemp2.put("dueDate", mapTemp.get(SELECT_ATTRIBUTE_SCHEDULED_COMPLETION_DATE));
                        mapTemp2.put("completionDate", mapTemp.get(SELECT_ATTRIBUTE_ACTUAL_COMPLETION_DATE));
                        mapTemp2.put("comments", mapTemp.get(SELECT_ATTRIBUTE_COMMENTS));
                        mapTemp2.put("instructions", mapTemp.get(SELECT_ATTRIBUTE_ROUTE_INSTRUCTIONS));
                        mapTemp2.put("assigneeId", mapTemp.get(SELECT_ROUTE_TASK_ASSIGNEE_ID));
                        mapTemp2.put("assigneeType", mapTemp.get(SELECT_ROUTE_TASK_ASSIGNEE_TYPE));
                        mapTemp2.put("assigneeName", mapTemp.get(SELECT_ROUTE_TASK_ASSIGNEE_NAME));
                        mapTemp2.put("routeTaskUser", mapTemp.get(SELECT_ATTRIBUTE_ROUTE_TASK_USER));
                        mapTemp2.put("owner", mapTemp.get(DomainObject.SELECT_OWNER));

                        // Set parent object information
                        mapTemp2.put("parentObjectId", mapObjectInfo.get(DomainObject.SELECT_ID));
                        mapTemp2.put("parentObjectName", mapObjectInfo.get(DomainObject.SELECT_NAME));
                        mapTemp2.put("parentObjectType", mapObjectInfo.get(DomainObject.SELECT_TYPE));
                        mapTemp2.put("parentObjectState", mapObjectInfo.get(DomainObject.SELECT_CURRENT));
                        mapTemp2.put("parentObjectPolicy", mapObjectInfo.get(DomainObject.SELECT_POLICY));
                        mapTemp2.put("isFromRelatedObject", mapObjectInfo.get("isRelatedObject"));
                        mapTemp2.put("relationship", mapObjectInfo.get("relationship"));

                        // Add the tasks due to route objects in the table data list
                        mlTableData.add(mapTemp2);
                    }

                    // Add all the defined tasks on to the final list
                    for (Iterator itrDefinedTasks = mlDefinedTasksOnRoute.iterator(); itrDefinedTasks.hasNext();) {
                        mapTemp = (Map) itrDefinedTasks.next();

                        mapTemp2 = new HashMap();
                        mapTemp2.put("name", mapTemp.get(SELECT_REL_ATTRIBUTE_TITLE));
                        mapTemp2.put("title", mapTemp.get(SELECT_REL_ATTRIBUTE_TITLE));
                        mapTemp2.put("approvalStatus", mapTemp.get(SELECT_REL_ATTRIBUTE_APPROVAL_STATUS));
                        mapTemp2.put("routeNodeId", mapTemp.get(SELECT_REL_ATTRIBUTE_ROUTE_NODE_ID));
                        mapTemp2.put("routeId", strRouteId);
                        mapTemp2.put("routeStatus", strRouteStatus);
                        mapTemp2.put("infoType", INFO_TYPE_DEFINED_TASK);
                        mapTemp2.put("dueDate", mapTemp.get(SELECT_REL_ATTRIBUTE_SCHEDULED_COMPLETION_DATE));
                        mapTemp2.put("comments", mapTemp.get(SELECT_REL_ATTRIBUTE_COMMENTS));
                        mapTemp2.put("instructions", mapTemp.get(SELECT_REL_ATTRIBUTE_ROUTE_INSTRUCTIONS));
                        mapTemp2.put("assigneeId", mapTemp.get(DomainObject.SELECT_ID));
                        mapTemp2.put("assigneeType", mapTemp.get(DomainObject.SELECT_TYPE));
                        mapTemp2.put("assigneeName", mapTemp.get(DomainObject.SELECT_NAME));
                        mapTemp2.put("routeTaskUser", mapTemp.get(SELECT_REL_ATTRIBUTE_ROUTE_TASK_USER));

                        // Set parent object information
                        mapTemp2.put("parentObjectId", mapObjectInfo.get(DomainObject.SELECT_ID));
                        mapTemp2.put("parentObjectName", mapObjectInfo.get(DomainObject.SELECT_NAME));
                        mapTemp2.put("parentObjectType", mapObjectInfo.get(DomainObject.SELECT_TYPE));
                        mapTemp2.put("parentObjectState", mapObjectInfo.get(DomainObject.SELECT_CURRENT));
                        mapTemp2.put("parentObjectPolicy", mapObjectInfo.get(DomainObject.SELECT_POLICY));
                        mapTemp2.put("isFromRelatedObject", mapObjectInfo.get("isRelatedObject"));
                        mapTemp2.put("relationship", mapObjectInfo.get("relationship"));

                        // Add the tasks due to route objects in the table data list
                        mlTableData.add(mapTemp2);
                    }
                } // for each route object

                //
                // Find signatures' details
                //

                // Find the states of the object
                StateList stateList = LifeCyclePolicyDetails.getStateList(context, dmoObject, strObjectPolicyName);

                State fromState = null;
                State toState = null;
                State state = null;

                // Find the current and the next state's State object
                for (Iterator itrStates = stateList.iterator(); itrStates.hasNext();) {
                    // Get a state
                    state = (State) itrStates.next();

                    // If form state is not yet found then check if the current state name is this state
                    if (fromState == null) {
                        if (state.getName().equals(strObjectStateName)) {
                            fromState = state;
                        }
                    }
                }

                // If we get the from and to state then only there are signatures to be found out
                if (fromState != null) {
                    MapList mlSignatureDetails = null;
                    Map mapSignatureInfo = null;
                    Map mapSignatureDetails = null;
                    String strSignatureName = null;
                    String strResult = null;
                    StringList slSignatureApprovers = null;
                    Vector vecAllBranches = new Vector();

                    // ///////////////////////////////////////////////////////////////////////////
                    //
                    // Detect if there are branches
                    //
                    for (Iterator itrAllStates = stateList.iterator(); itrAllStates.hasNext();) {
                        toState = (State) itrAllStates.next();

                        // Skip if this is from state
                        if (toState.getName().equals(fromState.getName())) {
                            continue;
                        }

                        mlSignatureDetails = dmoObject.getSignaturesDetails(context, fromState, toState);

                        if (mlSignatureDetails != null && mlSignatureDetails.size() > 0) {
                            //
                            // We need to find out the due date for the signatures.
                            //
                            for (Iterator itrSignatureDetails = mlSignatureDetails.iterator(); itrSignatureDetails.hasNext();) {
                                mapSignatureDetails = (Map) itrSignatureDetails.next();
                                // The due date for the signature will be the scheduled date for the next state
                                mapSignatureDetails.put("dueDate", toState.getScheduledDate());
                            }

                            vecAllBranches.add(mlSignatureDetails);
                        }
                    } // for

                    //
                    // If there are multiple branches then show the signtures for all of the branches
                    //
                    mlSignatureDetails = new MapList();

                    //
                    // When there are multiple branches, only show the signatures which are signed,
                    // so filter out the signatures which are signed
                    //
                    for (Iterator itrAllBranches = vecAllBranches.iterator(); itrAllBranches.hasNext();) {
                        MapList mlCurrentBranch = (MapList) itrAllBranches.next();
                        for (Iterator itrCurrentBranchSignatures = mlCurrentBranch.iterator(); itrCurrentBranchSignatures.hasNext();) {
                            Map mapCurrentSignature = (Map) itrCurrentBranchSignatures.next();
                            mlSignatureDetails.add(mapCurrentSignature);
                        }
                    }
                    //
                    //
                    // /////////////////////////////////////////////////////////////////////////////

                    String strMQL = null;
                    String strSelectStateTemplate = "state[" + strObjectStateName + "].signature[${SIGNATURE_NAME}]";
                    StringBuffer strMQLFindApproversTemplate = new StringBuffer(64);
                    String strHasApprove = null;
                    String strHasReject = null;
                    String strHasIgnore = null;
                    String strApproveLink = null;

                    // Form the hyperlink template to approve the signature
                    String strMQLApproveLinkTemplate = "<a href=\"javascript:emxTableColumnLinkClick('emxLifecycleApprovalDialogFS.jsp?objectId=" + strCurrentObjectId
                            + "&signatureName=${SIGNATURE_NAME}&toState=" + toState.getName() + "&fromState=" + fromState.getName()
                            + "&isInCurrentState=true&sHasApprove=${HAS_APPROVE}&sHasReject=${HAS_REJECT}&sHasIgnore=${HAS_IGNORE}','500','400','false','popup','')\"><img border='0' src='../common/images/iconActionApprove.png' />"
                            + STRING_AWAITING_APPROVAL + "</a>";

                    // Form the MQL template to find the approver for signature
                    strMQLFindApproversTemplate.append("print policy \"");
                    strMQLFindApproversTemplate.append(strObjectPolicyName);
                    strMQLFindApproversTemplate.append("\" select ");
                    strMQLFindApproversTemplate.append(strSelectStateTemplate);
                    strMQLFindApproversTemplate.append(".approve ");
                    strMQLFindApproversTemplate.append(strSelectStateTemplate);
                    strMQLFindApproversTemplate.append(".reject ");
                    strMQLFindApproversTemplate.append(strSelectStateTemplate);
                    strMQLFindApproversTemplate.append(".ignore dump ,");

                    // Do processing for each signature details so found
                    for (Iterator itrSignatureDetails = mlSignatureDetails.iterator(); itrSignatureDetails.hasNext();) {
                        mapSignatureInfo = (Map) itrSignatureDetails.next();

                        // Add information in map
                        mapSignatureInfo.put("infoType", INFO_TYPE_SIGNATURE);

                        // Get if user has access
                        strHasApprove = (String) mapSignatureInfo.get("hasapprove");
                        strHasReject = (String) mapSignatureInfo.get("hasreject");
                        strHasIgnore = (String) mapSignatureInfo.get("hasignore");

                        // If this singnature is not signed then find out if this user has access to approve it
                        if (!"true".equalsIgnoreCase((String) mapSignatureInfo.get("signed"))) {
                            strSignatureName = (String) mapSignatureInfo.get("name");

                            // Find out the role/group/person assigned for Approve/Reject/Ignore action for the signature
                            strMQL = FrameworkUtil.findAndReplace(strMQLFindApproversTemplate.toString(), "${SIGNATURE_NAME}", strSignatureName);
                            strResult = MqlUtil.mqlCommand(context, strMQL, true);
                            // slSignatureApprovers = FrameworkUtil.split(strResult, ",");

                            // If user has access to work on the signature then show the hyperlink for the signature
                            if ("TRUE".equals(strHasApprove) || "TRUE".equals(strHasReject) || "TRUE".equals(strHasIgnore)) {
                                strApproveLink = FrameworkUtil.findAndReplace(strMQLApproveLinkTemplate, "${SIGNATURE_NAME}", strSignatureName);
                                strApproveLink = FrameworkUtil.findAndReplace(strApproveLink, "${HAS_APPROVE}", strHasApprove);
                                strApproveLink = FrameworkUtil.findAndReplace(strApproveLink, "${HAS_REJECT}", strHasReject);
                                strApproveLink = FrameworkUtil.findAndReplace(strApproveLink, "${HAS_IGNORE}", strHasIgnore);

                                mapSignatureInfo.put("approvalStatus", strApproveLink);
                            } else {
                                mapSignatureInfo.put("approvalStatus", STRING_PENDING);
                            }

                            mapSignatureInfo.put("approver", strResult);
                        } else {
                            mapSignatureInfo.put("approver", mapSignatureInfo.get("signer"));
                        }

                        // Set parent object information
                        mapSignatureInfo.put("parentObjectId", mapObjectInfo.get(DomainObject.SELECT_ID));
                        mapSignatureInfo.put("parentObjectName", mapObjectInfo.get(DomainObject.SELECT_NAME));
                        mapSignatureInfo.put("parentObjectType", mapObjectInfo.get(DomainObject.SELECT_TYPE));
                        mapSignatureInfo.put("parentObjectState", mapObjectInfo.get(DomainObject.SELECT_CURRENT));
                        mapSignatureInfo.put("parentObjectPolicy", mapObjectInfo.get(DomainObject.SELECT_POLICY));
                        mapSignatureInfo.put("isFromRelatedObject", mapObjectInfo.get("isRelatedObject"));
                        mapSignatureInfo.put("relationship", mapObjectInfo.get("relationship"));

                        // Add signature information into table data maplist
                        mlTableData.add(mapSignatureInfo);
                    }
                } // if for signatures

            } // for for all objects

            // PCM TIGTK-3102 | 16/09/16 : Ketaki Wagh : Start
            MapList mlValidTaskList = getValidTaskListInCaseOfRole(context, mlTableData, strObjectId);
            // PCM : TIGTK-4434 : 02/03/2017 : AB : START
            // PCM : TIGTK-10708 : 28/10/2017 : AB : START
            if (TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION.equalsIgnoreCase(strObjectType) || TigerConstants.TYPE_PSS_CHANGENOTICE.equalsIgnoreCase(strObjectType)) {
                mlValidTaskList = getValidTaskListInCaseOfRoleForMCA(context, mlTableData, strObjectId);
            }
            // PCM : TIGTK-10708 : 28/10/2017 : AB : END
            // PCM : TIGTK-4434 : 02/03/2017 : AB : END

            return mlValidTaskList;
            // PCM TIGTK-3102 | 16/09/16 : Ketaki Wagh : End
        } catch (Exception exp) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getCurrentTaskSignaturesOnObject: ", exp);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw exp;
        } finally {
        }

    }

    // PCM TIGTK-3102 | 16/09/16 : Ketaki Wagh : Start
    /**
     * @author Kwagh
     * @param context
     * @param objectId
     * @return
     * @throws Exception
     */
    public MapList getValidTaskListInCaseOfRole(Context context, MapList mlTableData, String objectId) throws Exception {

        MapList retunrList = new MapList();
        if (mlTableData.size() > 0) {

            int tableDataSize = mlTableData.size();
            for (int i = 0; i < tableDataSize; i++) {

                Map mTemp = (Map) mlTableData.get(i);
                String strRouteTaskUser = (String) mTemp.get("routeTaskUser");
                // PCM TIGTK-6391 | 4/25/2017 : PTE : START
                String strtaskId = (String) mTemp.get("taskId");
                mTemp.put(DomainObject.SELECT_ID, strtaskId);
                // PCM TIGTK-6391 | 4/25/2017 : PTE : END
                // PCM TIGTK-4576 | 20/02/2017 : KWagh : Start
                String strTaskOwner = (String) mTemp.get("owner");
                if (UIUtil.isNotNullAndNotEmpty(strRouteTaskUser) && UIUtil.isNotNullAndNotEmpty(strTaskOwner) && strRouteTaskUser.startsWith("role_") && strRouteTaskUser.contains(strTaskOwner)) {
                    String strContextUser = context.getUser();
                    String strMQL = "print person  '" + strContextUser + "' select isassigned[" + PropertyUtil.getSchemaProperty(context, strRouteTaskUser) + "] dump";
                    boolean isToBeAccepted = "true".equalsIgnoreCase(MqlUtil.mqlCommand(context, strMQL, true));

                    // PCM : TIGTK-4560 : 28/06/2017 : AB : START
                    // check whether context user is delegated person or not. If it is delegated user for current task add current task to return list.
                    if (!isToBeAccepted) {
                        boolean isAbsenceDelegate = isAbsenceDelegate(context, strContextUser, strtaskId);
                        isToBeAccepted = isAbsenceDelegate;
                    }
                    // PCM : TIGTK-4560 : 28/06/2017 : AB : END

                    if (isToBeAccepted) {
                        boolean isPersonProgamMember = isPersonProgramMember(context, strContextUser, objectId);
                        if (isPersonProgamMember)
                            retunrList.add(mTemp);
                    }
                } else {
                    retunrList.add(mTemp);
                }
                // PCM TIGTK-4576 | 20/02/2017 : KWagh : End
            }

        }

        return retunrList;
    }

    // PCM TIGTK-3102 | 16/09/16 : Ketaki Wagh : End

    // PCM TIGTK-3102 | 16/09/16 : Ketaki Wagh : Start
    /**
     * @author kwagh
     * @param context
     * @param userName
     * @param objectId
     * @return
     * @throws Exception
     */
    public boolean isPersonProgramMember(Context context, String userName, String objectId) throws Exception {

        StringList slProgramprojectMemberList = new StringList();
        boolean isvalid = false;

        String strSelectableforCR = "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS + "].to.name";
        String strSelectableforCA = "to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.from["
                + TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS + "].to.name";
        String strSelectableforMCA = "to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.from["
                + TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS + "].to.name";

        DomainObject domChangeObject = new DomainObject(objectId);

        StringList slRequestedInfo = new StringList();
        slRequestedInfo.add(DomainObject.SELECT_TYPE);

        Map requestMap = domChangeObject.getInfo(context, slRequestedInfo);

        // Get Type of Change Object
        String strObjType = (String) requestMap.get(DomainConstants.SELECT_TYPE);

        if (strObjType.equalsIgnoreCase(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION) || strObjType.equalsIgnoreCase(TigerConstants.TYPE_CHANGEACTION)
                || strObjType.equalsIgnoreCase(TigerConstants.TYPE_PSS_CHANGEREQUEST)) {

            // Get List of Program-Project members for MCA
            if (strObjType.equalsIgnoreCase(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION)) {

                slProgramprojectMemberList = domChangeObject.getInfoList(context, strSelectableforMCA);

            } // Get List of Program-Project members for CA
            else if (strObjType.equalsIgnoreCase(TigerConstants.TYPE_CHANGEACTION)) {

                slProgramprojectMemberList = domChangeObject.getInfoList(context, strSelectableforCA);

            } // Get List of Program-Project members for CR
            else if (strObjType.equalsIgnoreCase(TigerConstants.TYPE_PSS_CHANGEREQUEST)) {

                slProgramprojectMemberList = domChangeObject.getInfoList(context, strSelectableforCR);
            }

            if (slProgramprojectMemberList.contains(userName)) {

                isvalid = true;
            }
        } else
            isvalid = true;

        return isvalid;
    }

    /**
     * This Method is used to Filter task list of MCA's Route. PCM : TIGTK-4434 : 02/03/2017 : AB
     * @param context
     * @param mlTableData
     * @param objectId
     * @return
     * @throws Exception
     */
    public MapList getValidTaskListInCaseOfRoleForMCA(Context context, MapList mlTableData, String objectId) throws Exception {
        try {
            MapList retunrList = new MapList();
            if (mlTableData.size() > 0) {

                int tableDataSize = mlTableData.size();
                for (int i = 0; i < tableDataSize; i++) {

                    Map mTemp = (Map) mlTableData.get(i);
                    String strRouteTaskUser = (String) mTemp.get("routeTaskUser");

                    String strTaskOwner = (String) mTemp.get("owner");
                    if (UIUtil.isNotNullAndNotEmpty(strRouteTaskUser) && strRouteTaskUser.startsWith("role_") && strRouteTaskUser.contains(strTaskOwner)) {
                        DomainObject domChange = DomainObject.newInstance(context, objectId);
                        String strChangeType = domChange.getInfo(context, DomainConstants.SELECT_TYPE);
                        // TIGTK-10708 :START
                        // PCM : TIGTK-10708 : 28/10/2017 : AB : START
                        String strMCOPlantMemberSelectable = DomainConstants.EMPTY_STRING;
                        String strPlantMembersConnectedToProjectSelectable = DomainConstants.EMPTY_STRING;
                        if (TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION.equalsIgnoreCase(strChangeType)) {

                            String strPlantName = domChange.getInfo(context,
                                    "to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].from.from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGRELATEDPLANT + "].to.name");

                            strMCOPlantMemberSelectable = "to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].from.from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGRELATEDPLANT
                                    + "].to.from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS + "].to.name";

                            strPlantMembersConnectedToProjectSelectable = "to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].from.to["
                                    + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPLANTMEMBERTOPROGPROJ + "|attribute["
                                    + TigerConstants.ATTRIBUTE_PSS_PLANTNAME + "]=='" + strPlantName + "'].to.name";
                        } else if (TigerConstants.TYPE_PSS_CHANGENOTICE.equalsIgnoreCase(strChangeType)) {
                            String strPlantName = domChange.getInfo(context,
                                    "to[" + TigerConstants.RELATIONSHIP_PSS_RELATEDCN + "].from.from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGRELATEDPLANT + "].to.name");

                            strMCOPlantMemberSelectable = "to[" + TigerConstants.RELATIONSHIP_PSS_RELATEDCN + "].from.from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGRELATEDPLANT + "].to.from["
                                    + TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS + "].to.name";

                            strPlantMembersConnectedToProjectSelectable = "to[" + TigerConstants.RELATIONSHIP_PSS_RELATEDCN + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA
                                    + "].from.from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPLANTMEMBERTOPROGPROJ + "|attribute[" + TigerConstants.ATTRIBUTE_PSS_PLANTNAME + "]=='" + strPlantName
                                    + "'].to.name";

                        }
                        // PCM : TIGTK-10708 : 28/10/2017 : AB : END
                        // Get the Connected plant members of MCO
                        StringList strMCOPlantMembers = domChange.getInfoList(context, strMCOPlantMemberSelectable);
                        // Get Plant Members connected To Program-Project of MCO

                        String strMember = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", objectId, strPlantMembersConnectedToProjectSelectable);

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
                        if (strMCOPlantMembers.contains(strContextUser) && flag == true) {
                            retunrList.add(mTemp);
                        }
                        // TIGTK-10708 :END
                    } else {
                        retunrList.add(mTemp);
                    }
                }
            }
            return retunrList;
        } catch (RuntimeException ex) {
            logger.error("Error in getValidTaskListInCaseOfRoleForMCA: ", ex);
            throw ex;
        } catch (Exception e) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getValidTaskListInCaseOfRoleForMCA: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
    }

    // PCM TIGTK-3102 | 16/09/16 : Ketaki Wagh : End

    // PCM TIGTK-9994: 29/09/2017 : KWagh : START
    /**
     * Gets Approval Status For Mass Tasks Approval Table
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            Packed arguments having table data
     * @return The vector containing values for this column
     * @throws Exception
     */
    public Vector getApprovalStatusForMassTasksApproval(Context context, String[] args) throws Exception {

        try {
            // Create result vector
            Vector vecResult = new Vector();

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");
            Map paramList = (Map) programMap.get("paramList");
            String languageStr = (String) paramList.get("languageStr");
            i18nNow loc = new i18nNow();

            Boolean bShowAbstian = false;
            String strChangeObjectId = (String) paramList.get("parentOID");
            if (UIUtil.isNotNullAndNotEmpty(strChangeObjectId)) {
                DomainObject domChange = DomainObject.newInstance(context, strChangeObjectId);
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
            }

            String isTaskApprovalCommentsOn = EnoviaResourceBundle.getProperty(context, "emxComponents.Routes.ShowCommentsForTaskApproval");
            final String STRING_NO_ACTION = (String) loc.GetString("emxFrameworkStringResource", languageStr, "emxFramework.LifecycleTasks.NoAction");
            final String STRING_REJECT = (String) loc.GetString("emxFrameworkStringResource", languageStr, "emxFramework.LifecycleTasks.Reject");
            final String STRING_APPROVE = (String) loc.GetString("emxFrameworkStringResource", languageStr, "emxFramework.LifecycleTasks.Approve");
            final String STRING_ACCEPT = (String) loc.GetString("emxFrameworkStringResource", languageStr, "emxFramework.LifecycleTasks.Accept");
            final String STRING_COMPLETE = (String) loc.GetString("emxFrameworkStringResource", languageStr, "emxFramework.LifecycleTasks.Complete");
            final String STRING_IGNORE = (String) loc.GetString("emxFrameworkStringResource", languageStr, "emxFramework.LifecycleTasks.Ignore");

            final String OPTION_NO_ACTION = "<option value=\"No Action\">" + STRING_NO_ACTION + "</option>";
            final String OPTION_REJECT = "<option value=\"Reject\">" + STRING_REJECT + "</option>";
            final String OPTION_APPROVE = "<option value=\"Approve\">" + STRING_APPROVE + "</option>";
            final String OPTION_ACCEPT = "<option value=\"Accept\">" + STRING_ACCEPT + "</option>";
            final String OPTION_COMPLETE = "<option value=\"Complete\">" + STRING_COMPLETE + "</option>";
            String OPTION_ABSTAIN = null;
            final String OPTION_IGNORE = "<option value=\"Ignore\">" + STRING_IGNORE + "</option>";

            boolean showAbstain = "true".equalsIgnoreCase(EnoviaResourceBundle.getProperty(context, "emxComponents.Routes.ShowAbstainForTaskApproval"));
            if (bShowAbstian) {
                showAbstain = false;
            }
            if (showAbstain) {
                OPTION_ABSTAIN = "<option value=\"Abstain\">" + loc.GetString("emxFrameworkStringResource", languageStr, "emxFramework.LifecycleTasks.Abstain") + "</option>";
            }

            Map mapObjectInfo = null;
            String strListBoxName = null;
            String strListBoxId = null;
            StringBuffer sbufHTML = null;
            String strValidApprovalStatusAction = null;
            String strSerialNumber = null;

            for (Iterator itrObjects = objectList.iterator(); itrObjects.hasNext();) {
                mapObjectInfo = (Map) itrObjects.next();

                strValidApprovalStatusAction = (String) mapObjectInfo.get("ValidApprovalStatusAction");
                strSerialNumber = (String) mapObjectInfo.get("serialNumber");

                // Form id & name of the list box
                strListBoxName = "Approval Status" + XSSUtil.encodeForHTMLAttribute(context, strSerialNumber);
                strListBoxId = strListBoxName + "Id";

                // Form the HTML code
                sbufHTML = new StringBuffer(64);
                sbufHTML.append("<select id=\"").append(strListBoxId).append("\" name=\"").append(strListBoxName);
                if (isTaskApprovalCommentsOn.equals("false")) {
                    sbufHTML.append("\" onchange=\"clearApprovalAndAbstainTaskComments('");
                    sbufHTML.append(strListBoxId).append("'").append(",'").append(strSerialNumber).append("')");
                }
                sbufHTML.append("\">");
                sbufHTML.append(OPTION_NO_ACTION);

                if ("IT-Accept".equals(strValidApprovalStatusAction)) {
                    sbufHTML.append(OPTION_ACCEPT);
                } else if ("IT-Approve".equals(strValidApprovalStatusAction)) {
                    sbufHTML.append(OPTION_APPROVE);
                    sbufHTML.append(OPTION_REJECT);
                    if (showAbstain)
                        sbufHTML.append(OPTION_ABSTAIN);
                } else if ("IT-Complete".equals(strValidApprovalStatusAction)) {
                    sbufHTML.append(OPTION_COMPLETE);
                } else if ("Sign-Approve".equals(strValidApprovalStatusAction)) {
                    // Begin : Bug 348243 : code modification
                    if ("true".equalsIgnoreCase((String) mapObjectInfo.get("hasapprove"))) {
                        sbufHTML.append(OPTION_APPROVE);
                    }
                    if ("true".equalsIgnoreCase((String) mapObjectInfo.get("hasreject"))) {
                        sbufHTML.append(OPTION_REJECT);
                    }
                    if ("true".equalsIgnoreCase((String) mapObjectInfo.get("hasignore"))) {
                        sbufHTML.append(OPTION_IGNORE);
                    }
                    // End : Bug 348243 : code modification
                } else if ("WFT-Accept".equals(strValidApprovalStatusAction)) {
                    sbufHTML.append(OPTION_ACCEPT);
                } else if ("WFT-Complete".equals(strValidApprovalStatusAction)) {
                    sbufHTML.append(OPTION_COMPLETE);
                } else if ("WBS-Complete".equals(strValidApprovalStatusAction)) {
                    sbufHTML.append(OPTION_COMPLETE);
                }
                sbufHTML.append("</select>");

                // Add to result vector
                vecResult.add(sbufHTML.toString());
            }

            return vecResult;
        } catch (Exception exp) {
            exp.printStackTrace();
            throw exp;
        } finally {
        }
    }
    // PCM TIGTK-9994: 29/09/2017 : KWagh : End
}
