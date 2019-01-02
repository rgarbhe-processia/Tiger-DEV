import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import pss.constants.TigerConstants;

import com.matrixone.apps.common.Route;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;
import com.matrixone.apps.domain.util.ContextUtil;
import matrix.db.Context;
import matrix.db.Access;

/****
 * @author rekatpure This JPO used fot Issue Notification
 */
public class PSS_IssueManagementNotification_mxJPO {

    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PSS_IssueManagementNotification_mxJPO.class);

    // TIGTK-6418:Rutuja Ekatpure:10/8/2017:Start
    /****
     * This method called from Reject Issue command
     * @param context
     * @param args
     * @throws Exception
     */
    public static void rejectIssueAndNotifyUsers(Context context, String[] args) throws Exception {
        logger.debug("PSS_IssueManagementNotification_mxJPO:rejectIssueAndNotifyUsers:START ");
        // TIGTK-10692 Suchit Gangurde: 27/10/2017: START
        boolean bIsContextPushed = false;
        // TIGTK-10692 Suchit Gangurde: 27/10/2017: END
        try {
            String contextUser = (String) context.getUser();
            Map<?, ?> programMap = (Map<?, ?>) JPO.unpackArgs(args);
            Map<?, ?> requestMap = (Map<?, ?>) programMap.get("requestMap");
            String strIssueObjId = (String) requestMap.get("objectId");
            DomainObject domIssue = DomainObject.newInstance(context, strIssueObjId);

            Map<String, String> payload = new HashMap<String, String>();
            StringList objectSelect = new StringList();
            objectSelect.add(DomainConstants.SELECT_ID);
            objectSelect.add(DomainConstants.ATTRIBUTE_ROUTE_STATUS);

            MapList mlConnectedRoute = domIssue.getRelatedObjects(context, DomainConstants.RELATIONSHIP_OBJECT_ROUTE, DomainConstants.TYPE_ROUTE, objectSelect, null, false, true, (short) 1,
                    "attribute[" + DomainConstants.ATTRIBUTE_ROUTE_STATUS + "] ==Started", null, 0);

            Iterator<Map<?, ?>> itrRoutes = mlConnectedRoute.iterator();
            String strRouteID = DomainConstants.EMPTY_STRING;
            while (itrRoutes.hasNext()) {
                Map<?, ?> mpRoute = itrRoutes.next();
                strRouteID = (String) mpRoute.get(DomainConstants.SELECT_ID);
                Route route = new Route(strRouteID);
                route.setAttributeValue(context, DomainConstants.ATTRIBUTE_ROUTE_STATUS, "Stopped");
                route.setState(context, "Complete");

            }
            // TIGTK-10692 Suchit Gangurde: 27/10/2017: START
            Access access = domIssue.getAccessMask(context);
            Boolean bAccessPromote = access.hasPromoteAccess();

            if (!bAccessPromote) {
                MqlUtil.mqlCommand(context, "history off", true, false);
                ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                bIsContextPushed = true;
            }

            domIssue.setAttributeValue(context, TigerConstants.ATTRIBUTE_BRANCH_TO, "Rejected");
            domIssue.promote(context);
            if (bIsContextPushed) {
                bIsContextPushed = false;
                ContextUtil.popContext(context);
                MqlUtil.mqlCommand(context, "history on", true, false);
                String strMqlHistory = "modify bus $1 add history $2";
                MqlUtil.mqlCommand(context, strMqlHistory, strIssueObjId, "promote");
            }
            // TIGTK-10692 Suchit Gangurde: 27/10/2017: END
            // TIGTK-6418:Rutuja Ekatpure:18/8/2017:Start
            // TIGTK-10692 :Sayali D : Start
            String strIssueRejectComments = (String) requestMap.get("IssueRejectComments");
            payload.put("IssueRejectComments", strIssueRejectComments);
            payload.put("fromList", contextUser);
            // TIGTK-10692 : Sayali D : End
            emxNotificationUtil_mxJPO.objectNotification(context, strIssueObjId, "PSS_IssueRejectNotification", payload);

            // TIGTK-6418:Rutuja Ekatpure:18/8/2017:End

            logger.debug("PSS_IssueManagementNotification_mxJPO:rejectIssueAndNotifyUsers:END ");
        } catch (Exception ex) {
            logger.error("Error in PSS_IssueManagementNotification_mxJPO:rejectIssueAndNotifyUsers:ERROR " + ex);
            throw ex;
        }
        // TIGTK-10692 Suchit Gangurde: 27/10/2017: START
        finally {
            if (bIsContextPushed) {
                ContextUtil.popContext(context);
            }
        }
        // TIGTK-10692 Suchit Gangurde: 27/10/2017: END
    }

    // TIGTK-10692 Suchit Gangurde: 27/10/2017: START
    /****
     * This method is called from rejectIssueAndNotifyUsers method to set from List for rejected Issue
     * @param context
     * @param args
     * @throws Exception
     */
    public String getIssueGenericPromoteFromList(Context context, String[] args) throws Exception {
        String strIssueGenericFrom = "";
        try {

            Map programMap = (Map) JPO.unpackArgs(args);
            Map payload = (Map) programMap.get("payload");

            if (!payload.isEmpty() && payload.containsKey("fromList")) {
                strIssueGenericFrom = (String) payload.get("fromList");
            }

        } catch (Exception ex) {

            logger.error("Error in getIssueGenericPromoteFromList: ", ex);
        }
        return strIssueGenericFrom;
    }
    // TIGTK-10692 Suchit Gangurde: 27/10/2017: END

    // TIGTK-6418:Rutuja Ekatpure:10/8/2017:End
    // TS-184 Create and Edit Issue From Global Actions -- TIGTK -6309 : 08/08/2017 : KWagh
    /**
     * @param context
     * @param args
     * @throws Exception
     * @author KWagh This method is called as post process method on Issue creation,to connect Issue and Program Project object
     */
    public void updateAttributeonCreation(Context context, String[] args) throws Exception {

        try {
            Map programMap = (Map) JPO.unpackArgs(args);

            Map paramMap = (Map) programMap.get("paramMap");
            Map requestMap = (Map) programMap.get("requestMap");
            String strIssueObjId = (String) paramMap.get("objectId");

            String strfromCommand = (String) requestMap.get("fromCommand");
            String strProjectID = (String) requestMap.get("ProjectCodeOID");

            DomainObject domIssue = DomainObject.newInstance(context, strIssueObjId);
            DomainObject domProject = DomainObject.newInstance(context);
            if (UIUtil.isNotNullAndNotEmpty(strProjectID)) {

                domProject.setId(strProjectID);
            }

            if ("SLC".equalsIgnoreCase(strfromCommand)) {

                // Get the Parent Id which is object id of program-project in whose context issue is getting created.

                String strParentObjectId = (String) requestMap.get("parentOID");

                domProject.setId(strParentObjectId);
            }

            // Connect Issue and Program Project object
            // TIGTK-11424 : Issue creation was blocked for Raw Material Engineer : Resolved by push-pop context : START
            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, TigerConstants.PERSON_USER_AGENT), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            DomainRelationship.connect(context, domProject, TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA, domIssue);
            ContextUtil.popContext(context);
            // TIGTK-11424 : Issue creation was blocked for Raw Material Engineer : Resolved by push-pop context : END
            // Send Mail Notification for Issue creation to Co-Owner of Issue
            Map payLoad = new HashMap();
            payLoad.put("objectId", strIssueObjId);
            emxNotificationUtil_mxJPO.objectNotification(context, strIssueObjId, "PSS_IssueCreateNotification", payLoad);
        } catch (Exception e) {
            logger.error("Error in updateAttributeonCreation: ", e);
            throw e;
        }

    }
    // TS-184 Create and Edit Issue From Global Actions -- TIGTK -6309 : 08/08/2017 : KWagh

    /**
     * @param context
     * @param args
     * @throws Exception
     * @author KWagh This method is called send notification to Issue COOwner during Cloning
     */
    public void sendNotificationToCoOwner(Context context, String[] arCloneIds) throws Exception {
        try {

            for (int i = 0; i < arCloneIds.length; i++) {
                String strIssueObjId = arCloneIds[i];
                Map payLoad = new HashMap();
                payLoad.put("objectId", strIssueObjId);
                emxNotificationUtil_mxJPO.objectNotification(context, strIssueObjId, "PSS_IssueCreateNotification", payLoad);

            }

        } catch (Exception e) {
            logger.error("Error in sendNotificationToCoOwner: ", e);
            throw e;
        }
    }

}
