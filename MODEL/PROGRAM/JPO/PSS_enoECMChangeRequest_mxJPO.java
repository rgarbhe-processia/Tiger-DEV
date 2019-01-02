
/*
 ** ${CLASS:PSS_enoECMChangeRequest}
 **
 ** Copyright (c) 1993-2015 Dassault Systemes. All Rights Reserved.
 */

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeConstants;
import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeOrder;
import com.dassault_systemes.enovia.enterprisechangemgt.util.ChangeUtil;
import com.matrixone.apps.common.Route;
import com.matrixone.apps.common.util.ComponentsUtil;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MailUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MessageUtil;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.eMatrixDateFormat;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.Access;
import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.MQLCommand;
import matrix.db.RelationshipType;
import matrix.db.User;
import matrix.util.Pattern;
import matrix.util.StringList;
import pss.constants.TigerConstants;

/**
 * The <code>enoECMChangeOrder</code> class contains code for the "Change Order" business type.
 * @version ECM R215 - # Copyright (c) 1992-2015 Dassault Systemes.
 */
public class PSS_enoECMChangeRequest_mxJPO extends enoECMChangeRequestBase_mxJPO {
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(enoECMChangeRequestBase_mxJPO.class);

    private ChangeOrder changeOrder = null;

    protected static emxRouteBase_mxJPO routeBase = null;

    /**
     * Constructor.
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param args
     *            holds no arguments.
     * @throws Exception
     *             if the operation fails.
     * @since ECM R215.
     */

    public PSS_enoECMChangeRequest_mxJPO(Context context, String[] args) throws Exception {

        super(context, args);
        changeOrder = new ChangeOrder();
    }

    // Addition for Tiger - PCM stream by Processia starts

    public void connectProgramOrProject(Context context, String[] args) throws Exception {

        HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
        HashMap<?, ?> paramMap = (HashMap<?, ?>) programMap.get("paramMap");
        String strChangeRequestOID = (String) paramMap.get("objectId");
        DomainObject domChangeRequestObject = DomainObject.newInstance(context, strChangeRequestOID);
        String strProgramProjectOID = (String) paramMap.get("New OID");
        // Added for getting program-project on WebForm field of create form
        int result = 0;
        if (strProgramProjectOID == null || strProgramProjectOID.isEmpty()) {
            HashMap<?, ?> requestMap = (HashMap<?, ?>) programMap.get("requestMap");
            String[] strProgProjOId = (String[]) requestMap.get("ProjectCodeId");
            if (strProgProjOId.length != 0) {
                strProgramProjectOID = strProgProjOId[0];
            }

        } else {
            if (domChangeRequestObject.isKindOf(context, TigerConstants.TYPE_CHANGEORDER)) {
                result = checkLinkedData(context, domChangeRequestObject);
            }

        }
        if (UIUtil.isNotNullAndNotEmpty(strProgramProjectOID) && result == 0) {

            // PCM : TIGTK_2483:on edit form of CR update Program Project related data by disconnecting already connected Prog. Project :08/09/2016:Rutuja Ekatpure:Start
            // get connected Program Project id
            String strCRProgProjRelId = domChangeRequestObject.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].id");
            // disconnect already connected program project from CR

            if (UIUtil.isNotNullAndNotEmpty(strCRProgProjRelId)) {
                DomainRelationship.disconnect(context, strCRProgProjRelId);
            }
            // PCM : TIGTK_2483: on edit form of CR update Program Project related data by disconnecting already connected Prog. Project :08/09/2016:Rutuja Ekatpure:End
            String strType = (String) domChangeRequestObject.getInfo(context, DomainConstants.SELECT_TYPE);
            DomainObject domProgramProjectObject = DomainObject.newInstance(context, strProgramProjectOID);
            String strProgramProjectCurrent = (String) domProgramProjectObject.getInfo(context, DomainConstants.SELECT_CURRENT);
            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            if (UIUtil.isNotNullAndNotEmpty(strProgramProjectCurrent) && strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_CHANGEREQUEST)) {
                domChangeRequestObject.setAttributeValue(context, "PSS_Project_Phase_at_Creation", strProgramProjectCurrent);
            }

            DomainRelationship.connect(context, domProgramProjectObject, TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA, domChangeRequestObject);
            ContextUtil.popContext(context);
        }
    }

    /**
     * @throws Exception
     */

    public void connectIssue(Context context, String[] args) throws Exception {

        HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
        HashMap<?, ?> paramMap = (HashMap<?, ?>) programMap.get("paramMap");
        String strChangeRequestOID = (String) paramMap.get("objectId");
        String strRelatedIssueOID = (String) paramMap.get("New OID");
        if (UIUtil.isNotNullAndNotEmpty(strRelatedIssueOID)) {
            DomainObject domChangeRequestObject = DomainObject.newInstance(context, strChangeRequestOID);
            DomainObject domIssueObject = DomainObject.newInstance(context, strRelatedIssueOID);
            DomainRelationship.connect(context, domChangeRequestObject, TigerConstants.RELATIONSHIP_ISSUE, domIssueObject);
        }

    }

    /**
     * Description : This method is used for Update Originator & Title of CR after creation of CR
     * @author abhalani
     * @args
     * @Date Jul 8, 2016
     */

    public void updateOriginator(Context context, String[] args) throws Exception {

        HashMap<?, ?> params = (HashMap<?, ?>) JPO.unpackArgs(args);
        HashMap<?, ?> paramMap = (HashMap<?, ?>) params.get("paramMap");
        String newObjectId = (String) paramMap.get("newObjectId");

        DomainObject domCRObj = new DomainObject(newObjectId);
        String strOwner = domCRObj.getOwner(context).getName();
        domCRObj.setAttributeValue(context, DomainConstants.ATTRIBUTE_ORIGINATOR, strOwner);
        String strCRTitle = domCRObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CRTITLE);
        String strCRName = domCRObj.getInfo(context, DomainConstants.SELECT_NAME);

        // Check If CR Title is empty than Set CR name as Title of CR
        if (UIUtil.isNullOrEmpty(strCRTitle)) {
            domCRObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CRTITLE, strCRName);
        }
    }

    // Addition for Tiger - PCM stream by Processia starts

    /**
     * Description : This method is used for Transfer Change Initiator of Change Request (TIGTK-2612)
     * @author abhalani
     * @args
     * @Date Aug 10, 2016
     */

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void sendToChangeInitiator(Context context, String[] args) throws Exception {
        try {

            HashMap<String, Object> param = (HashMap<String, Object>) JPO.unpackArgs(args);
            HashMap<String, Object> paramMap = (HashMap<String, Object>) param.get("paramMap");

            // Get The object id of Change Request & Reason for Re-transfer Initiator
            String strCRId = (String) paramMap.get("objectId");
            String strTransferReason = (String) paramMap.get("TransferReason");
            // Findbug Issue correction start
            // Date: 21/03/2017
            // By: Asha G.
            BusinessObject domCR = new BusinessObject(strCRId);
            // Findbug Issue correction End
            String strContextUser = context.getUser();
            // TGPSS_PCM-TS152 Change Request Notifications_V2.2 | 07/03/2017 |Harika Varanasi : Ends

            // PCM TIGTK-3128 & 3213 : 19/09/16 : AB : START
            // Transfer Change Initiator of Change Request
            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            domCR.demote(context);
            // TIGTK-3389:transfer to initaiator only change owner not change manager connected to CR object so below line commented and appropriate JAR code called:12/10/2016:start
            ChangeOrder changeOrder = new ChangeOrder(strCRId);
            // TIGTK-7128:Rutuja Ekatpure: On CR change owner two notification send ,one from change owner trigger on CR and
            // one from post process of Send to change initiator command,notification from change owner trigger on CR not required
            // so adding below code:28/4/2017:start
            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            MqlUtil.mqlCommand(context, false, "trigger off", false);
            changeOrder.transferBackToInitiator(context);
            MqlUtil.mqlCommand(context, false, "trigger on", false);
            ContextUtil.popContext(context);
            // TIGTK-7128:Rutuja Ekatpure:28/4/2017:End
            // TIGTK-3389:transfer to initaiator only change owner not change manager connected to CR object so below line commented and appropriate JAR code called:12/10/2016:End
            ContextUtil.popContext(context);
            // PCM TIGTK-3128 & 3213 : 19/09/16 : AB : END

            // TGPSS_PCM-TS152 Change Request Notifications_V2.2 | 07/03/2017 |Harika Varanasi : Starts
            Map<String, Object> payload = new HashMap<String, Object>();
            payload.put("fromList", new StringList(strContextUser));
            // PCM : TIGTK-8212 : 09/06/2017 : AB : START
            payload.put("ReasonForRework", strTransferReason);
            // PCM : TIGTK-8212 : 09/06/2017 : AB : END
            emxNotificationUtil_mxJPO.objectNotification(context, strCRId, "PSS_CRCreateDemoteNotification", payload);

            // TGPSS_PCM-TS152 Change Request Notifications_V2.2 | 07/03/2017 |Harika Varanasi : Ends

            // Added for TIGTK-6321 : Priyanka Salunke : 13-May-2017 : START
            DomainObject domChangeRequest = DomainObject.newInstance(context, strCRId);
            String strCRRequestedAssesmentEndDateValue = domChangeRequest.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CRREQUESTEDASSESSMENTENDDATE); // ARJUN SEE HERE

            if (UIUtil.isNotNullAndNotEmpty(strCRRequestedAssesmentEndDateValue)) {
                domChangeRequest.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CRREQUESTEDASSESSMENTENDDATE, DomainConstants.EMPTY_STRING); // ARJUN SEE SHERE
            }
            // Added for TIGTK-6321 : Priyanka Salunke : 13-May-2017 : END

        } catch (Exception e) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in sendToChangeInitiator: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
    }

    /**
     * Description : This method is called on "Rejection of CR" in Submit state. Modified for JIRA TIGTK-2370
     * @author abhalani
     * @args
     * @Date Aug 10, 2016
     */
    @com.matrixone.apps.framework.ui.PostProcessCallable
    public void rejectChangeRequest(Context context, String[] args) throws Exception {
        // TIGTK-12784 : 17-01-2018 : START
        String strContextUser = context.getUser();
        // TIGTK-12784 : 17-01-2018 : END
        boolean isContextPushed = false;
        try {
            String strRouteID = "";
            StringList objectSelect = new StringList();
            StringList relSelect = new StringList(1);
            String sUser = context.getUser();
            Map<?, ?> programMap = (Map<?, ?>) JPO.unpackArgs(args);
            Map<?, ?> paramMap = (Map<?, ?>) programMap.get("paramMap");
            String strCRID = (String) paramMap.get("objectId");
            HashMap<?, ?> requestMap = (HashMap<?, ?>) programMap.get(ChangeConstants.REQUEST_MAP);

            DomainObject domCR = new DomainObject(strCRID);
            objectSelect.add(DomainConstants.SELECT_ID);
            // Get Connected Route Of Change Request
            relSelect.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);
            MapList mlCRConnectedRoute = domCR.getRelatedObjects(context, DomainConstants.RELATIONSHIP_OBJECT_ROUTE, DomainConstants.TYPE_ROUTE, objectSelect, relSelect, false, true, (short) 1, null,
                    null, 0);
            for (Object objRoutInfo : mlCRConnectedRoute) {
                Map<?, ?> mapRoute = (Map<?, ?>) objRoutInfo;
                strRouteID = (String) mapRoute.get(DomainConstants.SELECT_ID);
                DomainObject domRoute = new DomainObject(strRouteID);
                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, TigerConstants.PERSON_USER_AGENT), "", "");
                isContextPushed = true;
                domRoute.setOwner(context, sUser);
                if (isContextPushed) {
                    ContextUtil.popContext(context);
                    isContextPushed = false;
                }
                domRoute.setAttributeValue(context, DomainConstants.ATTRIBUTE_ROUTE_STATUS, "Stopped");
            }
            // TIGTK-6888 : PKH : Phase-2.0 : Start
            Map<String, String> payload = new HashMap<String, String>();
            String sRejectionCommentValue = (String) requestMap.get("Reason");
            payload.put("rejectCRCommentValue ", sRejectionCommentValue);
            // TIGTK-6888 : PKH : Phase-2.0 : END
            // PCM2.0 Spr4:TIGTK-7589:31/8/2017:START
            // TIGTK-12784 : 17-01-2018 : START
            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, TigerConstants.PERSON_USER_AGENT), "", "");
            isContextPushed = true;
            // TIGTK-12784 : 17-01-2018 : END
            cancelImpactAnalysisStructure(context, domCR);
            // PCM2.0 Spr4:TIGTK-7589:31/8/2017:END
            domCR.setAttributeValue(context, TigerConstants.ATTRIBUTE_BRANCH_TO, "Rejected");
            String strSystemDate = getSystemDate(context, args);
            domCR.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_DECISION_DATE, strSystemDate);
            domCR.promote(context);

            // TIGTK-6888 : PKH : Phase-2.0 : Start

            if (domCR.hasRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_PREREQUISITECR, false)) {
                emxNotificationUtil_mxJPO.objectNotification(context, strCRID, "PSS_RejectSlaveCRNotification", payload);
            }

            else if (domCR.hasRelatedObjects(context, "PSS_PrerequisiteCR", true)) {
                emxNotificationUtil_mxJPO.objectNotification(context, strCRID, "PSS_RejectMasterCRNotification", payload);
            }
            // TIGTK-10757 Suchit Gangurde: 01/11/2017: START
            // else {
            // TIGTK-10757 Suchit Gangurde: 01/11/2017: END
            payload.put("fromList", strContextUser);
            emxNotificationUtil_mxJPO.objectNotification(context, strCRID, "PSS_CRRejectNotification", payload);
            // TIGTK-10757 Suchit Gangurde: 01/11/2017: START
            // }
            // TIGTK-10757 Suchit Gangurde: 01/11/2017: END
            // TIGTK-6888 : PKH : Phase-2.0 : END
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (isContextPushed) {
                ContextUtil.popContext(context);
                isContextPushed = false;
            }
        }
    }

    public void demoteCRToSubmit(Context context, String[] args) throws Exception {

        HashMap<?, ?> param = (HashMap<?, ?>) JPO.unpackArgs(args);

        HashMap<?, ?> paramMap = (HashMap<?, ?>) param.get("paramMap");
        String objectId = (String) paramMap.get("objectId");

        DomainObject objCR = new DomainObject(objectId);
        objCR.setState(context, "Submit");
    }

    /**
     * This Method is used for Connect ChangeManager with the Change Request Modified by PCM : TIGTK-4417 : 09/02/2017 : AB
     * @param context
     * @param args
     * @throws Exception
     */

    public void connectChangeManager(Context context, String[] args) throws Exception {
        try {
            // unpacking the Arguments from variable args
            Boolean isCMChnaged = false;
            String strRelationshipId = DomainConstants.EMPTY_STRING;
            String strCRCurrent = DomainConstants.EMPTY_STRING;
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            HashMap<?, ?> paramMap = (HashMap<?, ?>) programMap.get("paramMap");
            String strChangeRequestOID = (String) paramMap.get("objectId");
            String strChangeManagerOID = (String) paramMap.get("New OID");
            String strNewChangeManagerName = (String) paramMap.get("New Value");
            String strAlreadyConnectedChangeManagerName = (String) paramMap.get("Old value");

            DomainObject domChangeRequestObject = DomainObject.newInstance(context, strChangeRequestOID);
            DomainObject domPersonObject = DomainObject.newInstance(context, strChangeManagerOID);

            // Check it is updating Change Manger or not
            if (UIUtil.isNotNullAndNotEmpty(strAlreadyConnectedChangeManagerName)) {
                StringList slBusSelect = new StringList(DomainConstants.SELECT_CURRENT);
                slBusSelect.add("from[" + ChangeConstants.RELATIONSHIP_CHANGE_COORDINATOR + "].id");

                Map<String, Object> mapCRInfo = (Map<String, Object>) domChangeRequestObject.getInfo(context, slBusSelect);
                if (!mapCRInfo.isEmpty()) {
                    strCRCurrent = (String) mapCRInfo.get(DomainConstants.SELECT_CURRENT);
                    strRelationshipId = (String) mapCRInfo.get("from[" + ChangeConstants.RELATIONSHIP_CHANGE_COORDINATOR + "].id");
                }

                // Disconnect relationship with old Change Manager
                DomainRelationship.disconnect(context, strRelationshipId);
                isCMChnaged = true;
            }

            // Connect Change Request with Change Manager
            DomainRelationship.connect(context, domChangeRequestObject, ChangeConstants.RELATIONSHIP_CHANGE_COORDINATOR, domPersonObject);
            // TIGTK-10226 :START 12-Oct-2017
            if (TigerConstants.STATE_SUBMIT_CR.equalsIgnoreCase(strCRCurrent)) {
                Map<String, Object> payload = new HashMap<String, Object>();
                payload.put("fromList", new StringList(context.getUser()));
                if (UIUtil.isNotNullAndNotEmpty((String) paramMap.get("Comments"))) {
                    payload.put("Comments", (String) paramMap.get("Comments"));
                } else {
                    payload.put("Comments", "CR ownership has been transfered to you.");
                }
                if (isCMChnaged) {
                    emxNotificationUtil_mxJPO.objectNotification(context, strChangeRequestOID, "PSS_CRSubmitPromoteNotification", payload);
                }
                // TIGTK-10226 :END 12-Oct-2017
                // PCM : TIGTK-8504 : 15/06/2017 : AB : START
                // Set New Change-Manager to Owner of CR if CR is in Submit state

                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                MqlUtil.mqlCommand(context, false, "trigger off", false);
                domChangeRequestObject.setOwner(context, strNewChangeManagerName);
                MqlUtil.mqlCommand(context, false, "trigger on", false);
                ContextUtil.popContext(context);
            }
            // PCM : TIGTK-8504 : 15/06/2017 : AB : END

            // TIGTK-8665 - 17-06-2017 - AM - START
            // Set date of CR last transfer to Change Manager to current date
            String strCurrentSystemdate = getSystemDate(context, args);
            domChangeRequestObject.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CRDATEOFLASTTRANSFERTOCHANGEMANAGER, strCurrentSystemdate);
            // TIGTK-8665 - 17-06-2017 - AM - END
        }

        catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in connectChangeManager: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }

    }

    private DomainRelationship connect(Context context, HashMap<?, ?> paramMap, String targetRelName) throws Exception {

        try {
            String objectId = (String) paramMap.get(ChangeConstants.OBJECT_ID);
            changeOrder.setId(objectId);
            return changeOrder.connect(context, paramMap, targetRelName, true);
        }

        catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in connect: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
    }

    /**
     * This method is used to send notification to "Change Manager" on promotion of CR from Create to Submit state.
     * @param context
     * @param args
     * @author -- Pooja Mantri -- Added for Iteration 11 Issue -- TS 011
     * @return void - Returns Nothing
     * @throws Exception
     */
    public void sendToChangeManager(Context context, String[] args) throws Exception {
        logger.debug("PSS_enoECMChangeRequest:sendToChangeManager:START");
        boolean isContextPushed = false;
        try {
            Map<?, ?> programMap = (Map<?, ?>) JPO.unpackArgs(args);
            Map<?, ?> requestMap = (Map<?, ?>) programMap.get(ChangeConstants.REQUEST_MAP);

            String strCRobjectId = (String) requestMap.get("objectId");
            String strChangeManagerName = (String) requestMap.get("Change Manager");

            DomainObject domCRObject = DomainObject.newInstance(context, strCRobjectId);

            StringList busSelect = new StringList();
            busSelect.add(DomainConstants.SELECT_DESCRIPTION);
            busSelect.add(DomainConstants.SELECT_NAME);
            busSelect.add("from[" + TigerConstants.RELATIONSHIP_CHANGECOORDINATOR + "].id");
            busSelect.add("from[" + TigerConstants.RELATIONSHIP_CHANGECOORDINATOR + "].to.name");

            Map<?, ?> mapChangeConnectedDetails = domCRObject.getInfo(context, busSelect);

            String strChangeManagerNameBefore = (String) mapChangeConnectedDetails.get("from[" + TigerConstants.RELATIONSHIP_CHANGECOORDINATOR + "].to.name");
            String strRelOfCRAndCM = (String) mapChangeConnectedDetails.get("from[" + TigerConstants.RELATIONSHIP_CHANGECOORDINATOR + "].id");

            if (!strChangeManagerNameBefore.equalsIgnoreCase(strChangeManagerName)) {
                DomainRelationship.disconnect(context, strRelOfCRAndCM);
                BusinessObject boPerson = new BusinessObject(DomainConstants.TYPE_PERSON, strChangeManagerName, "-", null);
                DomainRelationship.connect(context, domCRObject, TigerConstants.RELATIONSHIP_CHANGECOORDINATOR, new DomainObject(boPerson));
            }

            updateInterchangeabilityStatusOnCR(context, domCRObject, programMap);
            // TIGTK-13056 : 30-01-2018 : START
            String strCurrentContextUser = context.getUser();
            // TIGTK-13056 : 30-01-2018 : END
            // Promote CR to Next State (i.e Submit)
            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            domCRObject.promote(context);
            isContextPushed = true;
            String strSystemDate = getSystemDate(context, args);
            // Set value to CR Attribute (Date of Last Transfer to Change Manager)
            domCRObject.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CRDATEOFLASTTRANSFERTOCHANGEMANAGER, strSystemDate);
            // To Skip Change owner Notification Set GlobalRPEValue "SubmitChange" to "YES"
            PropertyUtil.setGlobalRPEValue(context, "SubmitChange", "YES");
            domCRObject.setOwner(context, strChangeManagerName);
            // Send Notification to Change Manager
            Map<String, Object> payload = new HashMap<String, Object>();
            // TIGTK-13056 : 30-01-2018 : START
            payload.put("fromList", new StringList(strCurrentContextUser));
            // TIGTK-13056 : 30-01-2018 : END
            payload.put("Comments", (String) requestMap.get("Comments"));
            emxNotificationUtil_mxJPO.objectNotification(context, strCRobjectId, "PSS_CRSubmitPromoteNotification", payload);
            logger.debug("PSS_enoECMChangeRequest:sendToChangeManager:END");
        } catch (Exception ex) {
            logger.error("PSS_enoECMChangeRequest:sendToChangeManager:ERROR", ex);
            throw ex;
        } finally {
            if (isContextPushed) {
                ContextUtil.popContext(context);
            }
        }

    }

    public void checkRouteOnChangeRequestPromote(Context context, String[] args) throws Exception {
        try {
            String strObjectId = args[0];
            String strCurrentState = args[1];
            String contextUser = context.getUser();
            boolean contextPush = false;

            DomainObject domChangeRequest = DomainObject.newInstance(context, strObjectId);
            if ("Evaluate".equals(strCurrentState)) {
                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);

                domChangeRequest.demote(context);
                try {
                    String tempContextUser = context.getCustomData("contextUser");
                    if (UIUtil.isNullOrEmpty(tempContextUser)) {
                        context.setCustomData("contextUser", contextUser);
                    }
                    contextPush = true;
                    domChangeRequest.promote(context);
                } catch (Exception e) {
                    // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
                    logger.error("Error in checkRouteOnChangeRequestPromote: ", e);
                    // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
                    throw e;
                } finally {
                    if (contextPush) {
                        ContextUtil.popContext(context);
                    }
                }

            } else {

                StringList slObjectSle = new StringList(1);
                slObjectSle.addElement(DomainConstants.SELECT_ID);
                slObjectSle.addElement(DomainConstants.SELECT_NAME);

                StringList slRelSle = new StringList(1);
                slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);
                MapList mlObjConnected = domChangeRequest.getRelatedObjects(context, DomainConstants.RELATIONSHIP_OBJECT_ROUTE, DomainConstants.TYPE_ROUTE, slObjectSle, slRelSle, false, true,
                        (short) 1, null, null);

                for (int i = 0; i < mlObjConnected.size(); i++) {
                    Map<?, ?> mRouteObj = (Map<?, ?>) mlObjConnected.get(i);
                    String strRouteID = (String) mRouteObj.get(DomainConstants.SELECT_ID);
                    DomainObject objRoute = new DomainObject(strRouteID);

                    objRoute.deleteObject(context);
                }
            }

        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkRouteOnChangeRequestPromote: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
    }

    // This method is created for create new Route.
    public void createNewRouteOnChangeRequestPromote(Context context, String[] args) throws Exception {
        boolean isContextPushed = false;
        try {

            String contextUser = (String) context.getCustomData("contextUser");

            if (contextUser != null && !"".equals(contextUser)) {
                ContextUtil.pushContext(context, contextUser, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                isContextPushed = true;
                context.setCustomData("contextUser", "");
            }
            // Get the Object Id of the context Change Request object and Attribute value for Route.
            String strObjectId = args[0];
            String strRouteBaseState = args[1];
            String strRouteBasePurpose = args[2];
            String strRouteScope = args[3];
            String strRouteCompletionAction = args[4];
            String strRouteAutoStopOnRejection = args[5];
            String strRouteStateCondition = args[7];
            // PCM TIGTK-3000 | 08/09/16 : NAME : Kwagh Start

            DomainObject domChangeRequest = new DomainObject(strObjectId);

            // PCM : TIGTK-8087 : 25/05/2017 : AB : START
            String strSymbolicBaseState = FrameworkUtil.reverseLookupStateName(context, TigerConstants.POLICY_PSS_CHANGEREQUEST, strRouteBaseState);

            StringList objectSelects = new StringList();
            objectSelects.add(DomainConstants.SELECT_OWNER);
            objectSelects.add("from[" + ChangeConstants.RELATIONSHIP_CHANGE_COORDINATOR + "].to.name");
            objectSelects.add("from[" + DomainConstants.RELATIONSHIP_OBJECT_ROUTE + "|attribute[" + DomainConstants.ATTRIBUTE_ROUTE_BASE_STATE + "]==" + strSymbolicBaseState + "].to.id");

            // Check that ChangeRequest has already connected with Route or not
            Map<String, Object> mapCRInfo = domChangeRequest.getInfo(context, objectSelects);
            String strCRConnectedRouteID = (String) mapCRInfo.get("from[" + DomainConstants.RELATIONSHIP_OBJECT_ROUTE + "].to.id");

            String strCRRouteId = strCRConnectedRouteID;
            if (UIUtil.isNullOrEmpty(strCRConnectedRouteID)) {

                String strCROwner = (String) mapCRInfo.get(DomainConstants.SELECT_OWNER);
                if (UIUtil.isNotNullAndNotEmpty(strCROwner)) {
                    ContextUtil.pushContext(context, strCROwner, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                    isContextPushed = true;
                }

                // PCM : TIGTK-8087 : 25/05/2017 : AB : END
                // PCM TIGTK-3474 | 26/10/16 : Pooja Mantri : Start
                String strRouteTemplateDetails = getConnectedRouteObjectToProgProjForChange(context, strObjectId, strRouteBaseState);
                StringList slRouteTemplateDetails = FrameworkUtil.split(strRouteTemplateDetails, "|");
                String strRouteTemplateName = (String) slRouteTemplateDetails.get(0);
                String strRouteTemplateRevision = (String) slRouteTemplateDetails.get(1);
                BusinessObject boRouteTemplate = new BusinessObject(DomainConstants.TYPE_ROUTE_TEMPLATE, strRouteTemplateName, strRouteTemplateRevision, TigerConstants.VAULT_ESERVICEPRODUCTION);
                // PCM TIGTK-3474 | 26/10/16 : Pooja Mantri : End
                DomainObject domRouteTemplate = DomainObject.newInstance(context, boRouteTemplate);
                String strRouteTemplateId = domRouteTemplate.getInfo(context, DomainConstants.SELECT_ID);

                Route routeObject = (Route) DomainObject.newInstance(context, DomainConstants.TYPE_ROUTE); // Create new Route Object.

                // Create the new Route Object and connect it to the CR.
                Map<String, String> mpRelAttributeMap = new Hashtable<String, String>();
                mpRelAttributeMap.put("routeBasePurpose", strRouteBasePurpose);
                mpRelAttributeMap.put("Scope", strRouteScope);
                mpRelAttributeMap.put("State Condition", strRouteStateCondition);
                mpRelAttributeMap.put("Route Base State", strRouteBaseState);
                mpRelAttributeMap.put(strObjectId, strRouteBaseState);
                String strRouteId = "";

                Map<?, ?> routeMap = null;

                // try{

                routeMap = Route.createRouteWithScope(context, strObjectId, null, null, true, (Hashtable<String, String>) mpRelAttributeMap);
                strRouteId = (String) routeMap.get("routeId");
                strCRRouteId = strRouteId;
                routeObject.setId(strRouteId);
                DomainObject domObjRoute = new DomainObject(strRouteId);
                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                domObjRoute.setOwner(context, strCROwner);
                ContextUtil.popContext(context);

                // Set Attribute value of Route
                Map<String, String> mpRelNewAttributeMap = new Hashtable<String, String>();
                mpRelNewAttributeMap.put("Route Completion Action", strRouteCompletionAction);
                mpRelNewAttributeMap.put("Auto Stop On Rejection", strRouteAutoStopOnRejection);
                mpRelNewAttributeMap.put("Route Base Purpose", strRouteBasePurpose);

                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                routeObject.setAttributeValues(context, mpRelNewAttributeMap);
                ContextUtil.popContext(context);

                // If the route template id is not null then connect the route to the route template
                if (strRouteTemplateId != null && !strRouteTemplateId.equalsIgnoreCase("null") && !strRouteTemplateId.equalsIgnoreCase(DomainConstants.EMPTY_STRING)) {
                    routeObject.connectTemplate(context, strRouteTemplateId);
                    routeObject.addMembersFromTemplate(context, strRouteTemplateId);
                }

                // Set attribute of Route Node relationship,
                final String SELECT_ATTRIBUTE_SCHEDULED_COMPLETION_DATE = "attribute[" + DomainObject.ATTRIBUTE_SCHEDULED_COMPLETION_DATE + "]";
                String strRelId = "";

                // PCM TIGTK-3102 | 15/09/16 :Start
                Pattern typePattern = new Pattern(DomainConstants.TYPE_ROUTE_TASK_USER);
                typePattern.addPattern(DomainConstants.TYPE_PERSON);

                StringList slBusSelect = new StringList();

                StringList slRelSelect = new StringList(1);
                slRelSelect.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);
                slRelSelect.add(SELECT_ATTRIBUTE_SCHEDULED_COMPLETION_DATE);

                MapList mapListRouteNodeRel = domObjRoute.getRelatedObjects(context, DomainObject.RELATIONSHIP_ROUTE_NODE, typePattern.getPattern(), slBusSelect, slRelSelect, false, true, (short) 1,
                        null, null);

                // PCM TIGTK-3102 | 15/09/16 :End

                if (mapListRouteNodeRel != null && !mapListRouteNodeRel.isEmpty()) {
                    for (int i = 0; i < mapListRouteNodeRel.size(); i++) {

                        Map<?, ?> connectIdMap = (Map<?, ?>) mapListRouteNodeRel.get(i);
                        strRelId = (String) connectIdMap.get(DomainConstants.SELECT_RELATIONSHIP_ID);

                        // Creating relationship object.
                        DomainRelationship domRelRouteNode = DomainRelationship.newInstance(context, strRelId);

                        // setting attibute (scheduled completion date) values for that relationship.
                        domRelRouteNode.setAttributeValue(context, DomainConstants.ATTRIBUTE_DUEDATE_OFFSET, "7");
                        domRelRouteNode.setAttributeValue(context, DomainConstants.ATTRIBUTE_DATE_OFFSET_FROM, "Route Start Date");
                        domRelRouteNode.setAttributeValue(context, DomainConstants.ATTRIBUTE_ASSIGNEE_SET_DUEDATE, "No");
                    }
                }
            }
            if (UIUtil.isNotNullAndNotEmpty(strCRRouteId)) {
                String strCRChangeManager = (String) mapCRInfo.get("from[" + ChangeConstants.RELATIONSHIP_CHANGE_COORDINATOR + "].to.name");
                DomainObject domObjRoute = DomainObject.newInstance(context, strCRRouteId);
                boolean isContxtPushed = false;

                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, TigerConstants.PERSON_USER_AGENT), "", "");
                isContxtPushed = true;
                domObjRoute.setOwner(context, strCRChangeManager);
                if (isContxtPushed) {
                    ContextUtil.popContext(context);
                    isContxtPushed = false;
                }

            }
        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in createNewRouteOnChangeRequestPromote: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        } finally {
            if (isContextPushed) {
                ContextUtil.popContext(context);
            }
        }

    }

    // Custom Check trigger method to check when the Change Request is promoted from Submit to Evaluate state
    public int checkForParallelTrackComment(Context context, String args[]) throws Exception {
        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }
        String strMessage = "";
        int retValue = 0;
        try {
            final String RANGE_PSSCRTYPE_ENGINEERING_CR = "Engineering CR";
            final String RANGE_PSSPARALLELTRACK_YES = "Yes";
            final String RANGE_PSSPARALLELTRACK_NO = "No";
            String objectId = args[0];
            DomainObject objCR = new DomainObject(objectId);
            Map<?, ?> mapAttrDetails = objCR.getAttributeMap(context);
            String strAttrPSSCRParallelTrackValue = (String) mapAttrDetails.get(TigerConstants.ATTRIBUTE_PSS_PARALLELTRACK);
            String strAttrPSSCRParallelTrackCommentValue = (String) mapAttrDetails.get(TigerConstants.ATTRIBUTE_PSS_PARALLELTRACKCOMMENT);
            String strAttrPSSCRTypeValue = (String) mapAttrDetails.get(TigerConstants.ATTRIBUTE_PSS_CRTYPE);

            if (strAttrPSSCRParallelTrackValue.equals(RANGE_PSSPARALLELTRACK_NO)) {
                return retValue;
            }
            if (!strAttrPSSCRTypeValue.equalsIgnoreCase(RANGE_PSSCRTYPE_ENGINEERING_CR)) {
                return retValue;
            }
            if (strAttrPSSCRParallelTrackValue.equals(RANGE_PSSPARALLELTRACK_YES) && strAttrPSSCRTypeValue.equals(RANGE_PSSCRTYPE_ENGINEERING_CR)) {
                if (UIUtil.isNotNullAndNotEmpty(strAttrPSSCRParallelTrackCommentValue)) {
                    return retValue;
                } else {
                    strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSSEnterpriseChangeMgt.Alert.ReloadSuccessful");
                    MqlUtil.mqlCommand(context, "notice $1", strMessage);

                    return 1;
                }
            }

        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkForParallelTrackComment: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }

        return retValue;
    }

    // Custom Action trigger method to create CO object while promoting from Submit to Evaluate state
    // PCM TIGTK-3761 | 26/12/2016 : Ketaki Wagh : Start
    public void createCOFromCR(Context context, String args[]) throws Exception {

        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }

        StringList slPartType = new StringList();
        StringList slCADType = new StringList();
        StringList slStandardType = new StringList();
        StringList slCAComplete = new StringList();
        slCAComplete.add("Complete");
        HashSet<String> hsOnlyCompleteCA = new HashSet<String>();
        Map<String, String> mPartToRC = new HashMap<String, String>();
        PSS_enoECMChangeUtil_mxJPO ChangeUtilJPO = new PSS_enoECMChangeUtil_mxJPO(context, args);
        PSS_enoECMChangeOrder_mxJPO changeOrder = new PSS_enoECMChangeOrder_mxJPO(context, null);
        String sChangeCoordinatorId = "";
        String sReportedAgainstId = "";
        String sResponsibleOrganizationId = "";
        String sOwner = "";
        String sDesc = "";
        String sOriginator = "";
        String estimatedCompletionDate = "";
        String severity = "";
        String sCategoryOfChange = "";
        String sProgramProjectOIDValue = "";
        // Modified by KWagh -- TIGTK-2592
        String sPSSCOPurposeofRelease = DomainConstants.EMPTY_STRING;
        // Modified by KWagh -- TIGTK-259
        Map<String, String> sAttritubeMap = new HashMap<String, String>();
        String PROJECT = "project";

        try {
            String strCRObjID = args[0];
            DomainObject domCR = new DomainObject(strCRObjID);
            // PCM TIGTK-4048 | 27/01/17 :Pooja Mantri : Start
            StringList slAttributeSelect = new StringList();
            slAttributeSelect.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_PARALLELTRACK + "]");
            slAttributeSelect.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE + "]");
            slAttributeSelect.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRTARGETCHANGEIMPLEMENTATIONDATE + "]");
            Map<String, Object> mapCRAttributeDetails = domCR.getInfo(context, slAttributeSelect);
            String strattrPSSCRParallelTrack = (String) mapCRAttributeDetails.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_PARALLELTRACK + "]");
            sPSSCOPurposeofRelease = (String) mapCRAttributeDetails.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE + "]");
            String strAttrPSSCRTragetChangeImplDate = (String) mapCRAttributeDetails.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRTARGETCHANGEIMPLEMENTATIONDATE + "]");
            // PCM TIGTK-4048 | 27/01/17 :Pooja Mantri : End

            if (strattrPSSCRParallelTrack.equalsIgnoreCase("Yes")) {
                // create CO
                String sChangeCoordinator = "from[" + ChangeConstants.RELATIONSHIP_CHANGE_COORDINATOR + "].to.id";
                String sReportedAgainst = "from[" + ChangeConstants.RELATIONSHIP_REPORTED_AGAINST_CHANGE + "].to.id";
                String sProgramProjectCodeOID = "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id";
                String sResponsibleOrganization = SELECT_ORGANIZATION;

                StringList selectable = new StringList();
                selectable.addElement(sChangeCoordinator);
                selectable.addElement(sReportedAgainst);
                selectable.addElement(sResponsibleOrganization);
                selectable.addElement(SELECT_OWNER);
                selectable.addElement(SELECT_ORIGINATOR);
                selectable.addElement(SELECT_DESCRIPTION);
                selectable.addElement(ChangeConstants.SELECT_ATTRIBUTE_ESTIMATED_COMPLETION_DATE);
                selectable.addElement(ChangeConstants.SELECT_ATTRIBUTE_SEVERITY);
                selectable.addElement(ChangeConstants.SELECT_ATTRIBUTE_CATEGORY_OF_CHANGE);
                selectable.addElement(SELECT_ORGANIZATION);
                selectable.addElement(sProgramProjectCodeOID);
                selectable.addElement(PROJECT);

                Map<String, Object> resultList = domCR.getInfo(context, selectable);
                sChangeCoordinatorId = (String) resultList.get(sChangeCoordinator);
                sReportedAgainstId = (String) resultList.get(sReportedAgainst);
                sResponsibleOrganizationId = (String) resultList.get(sResponsibleOrganization);
                sOwner = (String) resultList.get(SELECT_OWNER);
                sDesc = (String) resultList.get(SELECT_DESCRIPTION);
                sOriginator = (String) resultList.get(SELECT_ORIGINATOR);
                estimatedCompletionDate = (String) resultList.get(ChangeConstants.SELECT_ATTRIBUTE_ESTIMATED_COMPLETION_DATE);
                severity = (String) resultList.get(ChangeConstants.SELECT_ATTRIBUTE_SEVERITY);
                sCategoryOfChange = (String) resultList.get(ChangeConstants.SELECT_ATTRIBUTE_CATEGORY_OF_CHANGE);
                sProgramProjectOIDValue = (String) resultList.get(sProgramProjectCodeOID);

                String sOrganization = (String) resultList.get(SELECT_ORGANIZATION);
                String sProject = (String) resultList.get(PROJECT);

                sAttritubeMap.put(ATTRIBUTE_ORIGINATOR, sOriginator);
                sAttritubeMap.put(ChangeConstants.ATTRIBUTE_ESTIMATED_COMPLETION_DATE, estimatedCompletionDate);
                sAttritubeMap.put(ATTRIBUTE_SEVERITY, severity);
                sAttritubeMap.put(ATTRIBUTE_CATEGORY_OF_CHANGE, sCategoryOfChange);
                // PCM TIGTK-4048 | 27/01/17 :Pooja Mantri : Start
                sAttritubeMap.put(TigerConstants.ATTRIBUTE_PSS_COVIRTUALIMPLEMENTATIONDATE, strAttrPSSCRTragetChangeImplDate);
                // PCM TIGTK-4048 | 27/01/17 :Pooja Mantri : End
                if (!ChangeUtil.isNullOrEmpty(strCRObjID)) {
                    StringList objectSelects = new StringList(1);
                    objectSelects.addElement(DomainConstants.SELECT_ID);
                    objectSelects.addElement(DomainConstants.SELECT_POLICY);
                    objectSelects.addElement(DomainConstants.SELECT_TYPE);
                    objectSelects.addElement(DomainConstants.SELECT_CURRENT);
                    objectSelects.add("to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "]");
                    objectSelects.add("to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].from.current");

                    StringList relSelects = new StringList(1);
                    relSelects.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);
                    relSelects.add(ChangeConstants.SELECT_ATTRIBUTE_REQUESTED_CHANGE);
                    // Modified for PCM by Ketaki Wagh-- For "Empty CO Creation"

                    String where = "policy != PSS_Development_Part && policy != PSS_MBOM && (current==Released || current==Release || current == 'In Work' || current == Preliminary || current==Review)";

                    // Get CR Related Affected Items
                    MapList mlCRAffectedItems = domCR.getRelatedObjects(context, // context
                            TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM, // relationship pattern
                            DomainConstants.QUERY_WILDCARD, // object pattern
                            objectSelects, // object selects
                            relSelects, // relationship selects
                            false, // to direction
                            true, // from direction
                            (short) 0, // recursion level
                            where, // object where clause
                            null, // relationship where clause
                            (short) 0, false, // checkHidden
                            true, // preventDuplicates
                            (short) 0, // pageSize
                            null, null, null, null);
                    Map mapAICurrentState = new HashMap();
                    // Filter CR affected item on based of type of affected Item
                    for (int k = 0; k < mlCRAffectedItems.size(); k++) {
                        Map<?, ?> mTemp = (Map<?, ?>) mlCRAffectedItems.get(k);
                        // TIGTK-10960 :Start
                        String strConnectedTOCA = (String) mTemp.get("to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "]");

                        PSS_enoECMChangeUtil_mxJPO changeUtil = new PSS_enoECMChangeUtil_mxJPO(context, args);
                        StringList slConnectedTOCAState = changeUtil.getStringListFromMap(context, mTemp, "to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].from.current");
                        hsOnlyCompleteCA.addAll(slConnectedTOCAState);
                        int nCAStateSetSize = hsOnlyCompleteCA.size();
                        String strPolicy = (String) mTemp.get(DomainObject.SELECT_POLICY);
                        String strCRRequesedChangeValue = (String) mTemp.get(ChangeConstants.SELECT_ATTRIBUTE_REQUESTED_CHANGE);
                        String strAIID = (String) mTemp.get(DomainObject.SELECT_ID);
                        String strPartState = (String) mTemp.get(DomainObject.SELECT_CURRENT);
                        mapAICurrentState.put(strAIID, strPartState);
                        // PCM TIGTK-10830 : 2/11/17 : START
                        DomainObject domAI = DomainObject.newInstance(context, strAIID);

                        String strGoveringPrjId = domAI.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT + "].from.id");
                        if (strGoveringPrjId == null) {
                            strGoveringPrjId = DomainConstants.EMPTY_STRING;
                        }

                        if ((UIUtil.isNullOrEmpty(strGoveringPrjId)) || (strGoveringPrjId.equalsIgnoreCase(sProgramProjectOIDValue))) {

                            // PCM TIGTK-10830 : 2/11/17 : END
                            if ((TigerConstants.POLICY_PSS_ECPART.equals(strPolicy))
                                    && (("False".equalsIgnoreCase(strConnectedTOCA) || (nCAStateSetSize == 1 && hsOnlyCompleteCA.contains(ChangeConstants.STATE_COMPLETE))))) {
                                slPartType.add(strAIID);

                                mPartToRC.put(strAIID, strCRRequesedChangeValue);
                            }
                            // Added for Standard Part
                            else if (TigerConstants.POLICY_STANDARDPART.equals(strPolicy)
                                    && (("False".equalsIgnoreCase(strConnectedTOCA) || (nCAStateSetSize == 1 && hsOnlyCompleteCA.contains(ChangeConstants.STATE_COMPLETE))))) {
                                slStandardType.add(strAIID);
                                mPartToRC.put(strAIID, strCRRequesedChangeValue);
                            }

                            // PCM : TIGTK-8595 19/06/2017 : AB : START
                            else if ((TigerConstants.POLICY_PSS_CADOBJECT.equals(strPolicy))
                                    && (("False".equalsIgnoreCase(strConnectedTOCA) || (nCAStateSetSize == 1 && hsOnlyCompleteCA.contains(ChangeConstants.STATE_COMPLETE))))) {

                                // PCM : TIGTK-9060 : 28/07/2017 : AB : START
                                Map<String, DomainObject> mapObjects = new HashMap<String, DomainObject>();
                                DomainObject domAffectedItem = DomainObject.newInstance(context, strAIID);
                                mapObjects.put("domCADItem", domAffectedItem);
                                boolean bolAllowConnection = changeOrder.checkCADValidationForAddIntoCO(context, JPO.packArgs(mapObjects));

                                if (bolAllowConnection) {
                                    // PCM : TIGTK-8595 19/06/2017 : AB : END
                                    slCADType.add(strAIID);
                                    mPartToRC.put(strAIID, strCRRequesedChangeValue);
                                }
                            }
                        }
                    }
                    StringList slValidCADId = ChangeUtilJPO.checkStateOfCADForTransferAffectedItemsFromCRToCO(context, slPartType, slStandardType, slCADType, mapAICurrentState);
                    slCADType = slValidCADId;
                }
                // TIGTK-10960 :End
                if ((!slPartType.isEmpty()) || (!slStandardType.isEmpty()) || (!slCADType.isEmpty())) {
                    // Create New CO
                    final String newCOId = FrameworkUtil.autoName(context, "type_PSS_ChangeOrder", "policy_PSS_ChangeOrder", true);

                    DomainObject objCOobj = new DomainObject(newCOId);
                    // Modified by KWagh -- TIGTK-2592
                    objCOobj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE, sPSSCOPurposeofRelease);
                    // Modified by KWagh -- TIGTK-2592
                    // TIGTK-4463 - 21-03-2017 - VP - START
                    if (sPSSCOPurposeofRelease.equals("Other")) {
                        objCOobj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_OTHER_COMMENTS, "Parallel Track CO");
                    }
                    // TIGTK-4463 - 21-03-2017 - VP - END
                    objCOobj.setDescription(context, sDesc);
                    objCOobj.setOwner(context, sOwner);

                    // Connects CR and CO while promoting CR
                    ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                    objCOobj.connect(context, ChangeConstants.RELATIONSHIP_CHANGE_ORDER, domCR, true);
                    ContextUtil.popContext(context);

                    // Copy ResponsibleOrganization from CR to CO
                    if (!ChangeUtil.isNullOrEmpty(sResponsibleOrganizationId)) {
                        objCOobj.setPrimaryOwnership(context, sProject, sOrganization);
                    }

                    // Copy ChangeCoordinator from CR to CO
                    if (!ChangeUtil.isNullOrEmpty(sChangeCoordinatorId)) {
                        DomainRelationship.connect(context, objCOobj, ChangeConstants.RELATIONSHIP_CHANGE_COORDINATOR, DomainObject.newInstance(context, sChangeCoordinatorId));
                    }
                    // Copy ReportedAgainst from CR to CO
                    if (!ChangeUtil.isNullOrEmpty(sReportedAgainstId)) {
                        DomainRelationship.connect(context, objCOobj, ChangeConstants.RELATIONSHIP_REPORTED_AGAINST_CHANGE, DomainObject.newInstance(context, sReportedAgainstId));
                    }
                    // Coping Estimated Completion Date, Severity, Originator and Category of Change to CO while generating CO from CR.
                    objCOobj.setAttributeValues(context, sAttritubeMap);

                    // Modified for PCM RFC033 by Pooja Mantri -- For Connecting "Parallel Track CO" with "Program Project"
                    // Connect "PSS_ChangeOrder" object with "PSS Program Project" Object
                    // Creating Domain Object instance of "Program Project"
                    DomainObject domProgramProject = DomainObject.newInstance(context, sProgramProjectOIDValue);
                    ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                    DomainRelationship.connect(context, domProgramProject, new RelationshipType(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA), objCOobj);
                    ContextUtil.popContext(context);
                    // Modified for PCM RFC033 by Pooja Mantri

                    // Added for displaying Effectivity field on CO Properties page
                    if (ChangeUtil.isCFFInstalled(context)) {

                        String changeName = (String) resultList.get(DomainConstants.SELECT_NAME);
                        String changeVault = (String) resultList.get(DomainConstants.SELECT_VAULT);

                        DomainObject neObj = DomainObject.newInstance(context);
                        neObj.createObject(context, TigerConstants.TYPE_NAMED_EFFECTIVITY, changeName, "", TigerConstants.POLICY_NAMED_EFFECTIVITY, changeVault);
                        String neObjectId = neObj.getObjectId(context);
                        RelationshipType relType = new RelationshipType();
                        relType.setName(TigerConstants.RELATIONSHIP_NAMED_EFFECTIVITY);
                        objCOobj.addToObject(context, relType, neObjectId);
                    }

                    DomainObject domNewCA = new DomainObject();

                    if (slCADType.size() > 0) {
                        // ceate new CA and attach all AI of CAD
                        String CAId = ChangeUtilJPO.createNewCA(context, newCOId);
                        domNewCA = DomainObject.newInstance(context, CAId);
                        domNewCA.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CATYPE, "CAD");
                        Map<?, ?> mCATOPartMap = DomainRelationship.connect(context, domNewCA, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, true,
                                ((String) FrameworkUtil.join(slCADType, ",")).split(","));
                        ChangeUtilJPO.setRequestedChange(context, mCATOPartMap, mPartToRC);
                    }

                    if (slPartType.size() > 0) {
                        // ceate new CA and attach all AI of Part
                        String CAId = ChangeUtilJPO.createNewCA(context, newCOId);
                        domNewCA = DomainObject.newInstance(context, CAId);
                        domNewCA.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CATYPE, "Part");
                        Map<?, ?> mCATOPartMap = DomainRelationship.connect(context, domNewCA, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, true,
                                ((String) FrameworkUtil.join(slPartType, ",")).split(","));
                        ChangeUtilJPO.setRequestedChange(context, mCATOPartMap, mPartToRC);
                    }

                    if (slStandardType.size() > 0) {
                        // ceate new CA and attach all AI of Part
                        String CAId = ChangeUtilJPO.createNewCA(context, newCOId);
                        domNewCA = DomainObject.newInstance(context, CAId);
                        domNewCA.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CATYPE, "Standard");
                        Map<?, ?> mCATOStandardMap = DomainRelationship.connect(context, domNewCA, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, true,
                                ((String) FrameworkUtil.join(slStandardType, ",")).split(","));
                        ChangeUtilJPO.setRequestedChange(context, mCATOStandardMap, mPartToRC);
                    }

                }

            }

            // Modified for PCM by Ketaki Wagh-- For "Empty CO Creation"
        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in createCOFromCR: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
    }

    // PCM TIGTK-3761 | 26/12/2016 : Ketaki Wagh : End
    /**
     * Description : Custom action trigger method which starts Impact Analysis Route while promoting CR from Submit to Evaluate state Modified for PCM : TIGTK-4248 : 21/02/2017 : AB
     * @author abhalani
     * @args
     * @Date Aug 11, 2016
     */

    public void startImpactAnalysisRoute(Context context, String args[]) throws Exception {

        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }

        try {
            String strRouteID = "";
            String strCRRequestdAssesmentEndDate = DomainConstants.EMPTY_STRING;
            StringList objectSelect = new StringList();
            StringList slConnectedPersonOfRoute = new StringList();

            // Get the Object id of Change Request
            String strCRID = args[0];
            String strRouteDueDateTime = getRouteDueDate(context, args);
            DomainObject domCR = new DomainObject(strCRID);

            // Get CR connected Program Project Information & value of CR's attribute
            objectSelect.addElement(DomainConstants.SELECT_DESCRIPTION);
            objectSelect.addElement(DomainConstants.SELECT_NAME);
            objectSelect.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
            objectSelect.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.name");
            objectSelect.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRREASONFORCHANGE + "]");
            objectSelect.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE + "]");
            objectSelect.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRREQUESTEDASSESSMENTENDDATE + "]");
            objectSelect.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_PARALLELTRACK + "]");
            objectSelect.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_PARALLELTRACKCOMMENT + "]");

            @SuppressWarnings("unchecked")
            Map<String, Object> mapCRInfo = (Map<String, Object>) domCR.getInfo(context, objectSelect);
            strCRRequestdAssesmentEndDate = (String) mapCRInfo.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRREQUESTEDASSESSMENTENDDATE + "]");

            // Get The connected Route of Change Request
            objectSelect.addElement(DomainConstants.SELECT_ID);
            objectSelect.addElement(DomainConstants.SELECT_NAME);
            objectSelect.addElement("attribute[" + DomainConstants.ATTRIBUTE_ROUTE_STATUS + "]");
            StringList relSelect = new StringList(1);
            relSelect.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

            MapList mlCRConnectedRoute = domCR.getRelatedObjects(context, DomainConstants.RELATIONSHIP_OBJECT_ROUTE, DomainConstants.TYPE_ROUTE, objectSelect, relSelect, false, true, (short) 1, null,
                    null, 0);
            if (mlCRConnectedRoute.size() > 0) {
                for (int i = 0; i < mlCRConnectedRoute.size(); i++) {
                    Map<?, ?> mapRoute = (Map<?, ?>) mlCRConnectedRoute.get(i);
                    strRouteID = (String) mapRoute.get(DomainConstants.SELECT_ID);
                    String strRouteStatus = (String) mapRoute.get("attribute[" + DomainConstants.ATTRIBUTE_ROUTE_STATUS + "]");
                    DomainObject domRoute = new DomainObject(strRouteID);
                    String strRouteBaseState = domRoute.getInfo(context,
                            "to[" + DomainConstants.RELATIONSHIP_OBJECT_ROUTE + "].attribute[" + PropertyUtil.getSchemaProperty(context, "attribute_RouteBaseState") + "]");

                    if ("state_Evaluate".equals(strRouteBaseState)) {

                        String strRelId = "";
                        String strRelPattern = DomainObject.RELATIONSHIP_ROUTE_NODE;

                        // PCM TIGTK-3102 | 15/09/16 :Start

                        // Rutuja Ekatpure : 14/09/2016 :Start
                        Pattern typeSelectPattern = new Pattern(DomainConstants.TYPE_ROUTE_TASK_USER);
                        typeSelectPattern.addPattern(DomainConstants.TYPE_PERSON);

                        typeSelectPattern.addPattern("Person");
                        StringList slBusSelect = new StringList();
                        slBusSelect.add(DomainConstants.SELECT_NAME);
                        StringList slRelSelect = new StringList(1);
                        slRelSelect.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);
                        slRelSelect.add("attribute[" + DomainObject.ATTRIBUTE_SCHEDULED_COMPLETION_DATE + "]");
                        MapList mlRouteNodeRel = domRoute.getRelatedObjects(context, strRelPattern, typeSelectPattern.getPattern(), slBusSelect, slRelSelect, false, true, (short) 1, null, null, 0);
                        // Rutuja Ekatpure : 14/09/2016 :End
                        // PCM TIGTK-3102 | 15/09/16 :End

                        try {

                            if (mlRouteNodeRel != null && !mlRouteNodeRel.isEmpty()) {
                                for (int j = 0; j < mlRouteNodeRel.size(); j++) {
                                    Map<?, ?> mapRouteNodeRel = (Map<?, ?>) mlRouteNodeRel.get(j);
                                    strRelId = (String) mapRouteNodeRel.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                                    // PCM TIGTK-3733 : 03/12/2016 : AB : Start
                                    String slPersonName = (String) mapRouteNodeRel.get(DomainConstants.SELECT_NAME);

                                    BusinessObject boPerson = new BusinessObject(DomainConstants.TYPE_PERSON, slPersonName, "-", null);
                                    if (boPerson.exists(context)) {
                                        slConnectedPersonOfRoute.add(slPersonName);
                                    }
                                    // PCM TIGTK-3733 : 03/12/2016 : AB : END
                                    DomainRelationship domRelRouteNode = DomainRelationship.newInstance(context, strRelId);
                                    // Added by Pooja Mantri - TIGTK-2741
                                    ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);

                                    if (UIUtil.isNotNullAndNotEmpty(strCRRequestdAssesmentEndDate)) {
                                        domRelRouteNode.setAttributeValue(context, DomainObject.ATTRIBUTE_SCHEDULED_COMPLETION_DATE, strCRRequestdAssesmentEndDate);
                                    } else {
                                        domRelRouteNode.setAttributeValue(context, DomainObject.ATTRIBUTE_SCHEDULED_COMPLETION_DATE, strRouteDueDateTime);
                                        domCR.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CRREQUESTEDASSESSMENTENDDATE, strRouteDueDateTime);
                                    }

                                }
                            }

                            if (strRouteStatus.equalsIgnoreCase("Stopped") || strRouteStatus.equalsIgnoreCase("Finished")) {
                                // Restarting the already connected Route
                                Route route = new Route(strRouteID);
                                route.resume(context);
                                this.setDueDateOnCRRoute(context, strRouteID, strCRRequestdAssesmentEndDate);
                            } else {
                                domRoute.setAttributeValue(context, DomainConstants.ATTRIBUTE_ROUTE_STATUS, "Started");
                                domRoute.setState(context, "In Process");
                            }

                        } catch (Exception e) {
                            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
                            logger.error("Error in startImpactAnalysisRoute: ", e);
                            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
                            throw e;
                        } finally {
                            ContextUtil.popContext(context);
                            // Added by Pooja Mantri - TIGTK-2741
                        }
                    }
                }
            }

        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in startImpactAnalysisRoute: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }

    }

    // Custom action trigger method which starts Evaluation Reviewers List Route while promoting CR from Evaluate to In Review state
    public void startEvaluationReviewersListRoute(Context context, String args[]) throws Exception {

        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }
        boolean isContextPushed = false;
        try {
            String contextUser = (String) context.getCustomData("PSS_CRcontextUser");

            if (contextUser != null && !"".equals(contextUser)) {
                ContextUtil.pushContext(context, contextUser, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                isContextPushed = true;
            }

            String strCRObjID = args[0];
            String strRouteDueDateTime = getRouteDueDate(context, args);
            DomainObject objCR = new DomainObject(strCRObjID);

            String relpattern = PropertyUtil.getSchemaProperty(context, "relationship_ObjectRoute");
            String typePattern = PropertyUtil.getSchemaProperty(context, "type_Route");

            StringList slObjectSle = new StringList(1);
            slObjectSle.addElement(DomainConstants.SELECT_ID);
            slObjectSle.addElement(DomainConstants.SELECT_NAME);
            slObjectSle.addElement("attribute[" + DomainConstants.ATTRIBUTE_ROUTE_STATUS + "]");

            StringList slRelSle = new StringList(1);
            slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

            // Connected Route Objects
            String strRouteID;

            MapList mlObjConnected = objCR.getRelatedObjects(context, relpattern, typePattern, slObjectSle, slRelSle, false, true, (short) 1, null, null);
            if (mlObjConnected.size() > 0) {
                for (int i = 0; i < mlObjConnected.size(); i++) {
                    Map<?, ?> mRouteObj = (Map<?, ?>) mlObjConnected.get(i);
                    strRouteID = (String) mRouteObj.get(DomainConstants.SELECT_ID);
                    String strRouteStatus = (String) mRouteObj.get("attribute[" + DomainConstants.ATTRIBUTE_ROUTE_STATUS + "]");
                    DomainObject objRoute = new DomainObject(strRouteID);
                    String sRouteBaseState = objRoute.getInfo(context,
                            "to[" + DomainConstants.RELATIONSHIP_OBJECT_ROUTE + "].attribute[" + PropertyUtil.getSchemaProperty(context, "attribute_RouteBaseState") + "]");

                    if ("state_InReview".equals(sRouteBaseState)) {
                        final String SELECT_ATTRIBUTE_SCHEDULED_COMPLETION_DATE = "attribute[" + DomainObject.ATTRIBUTE_SCHEDULED_COMPLETION_DATE + "]";
                        String strRelId = "";

                        // PCM TIGTK-3102 | 15/09/16 :Start

                        // Rutuja Ekatpure : 14/09/2016 :Start
                        Pattern typeSelectPattern = new Pattern(DomainConstants.TYPE_ROUTE_TASK_USER);
                        typeSelectPattern.addPattern(DomainConstants.TYPE_PERSON);

                        StringList slBusSelect = new StringList();
                        StringList slRelSelect = new StringList(1);
                        slRelSelect.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);
                        slRelSelect.add(SELECT_ATTRIBUTE_SCHEDULED_COMPLETION_DATE);
                        MapList mapListRouteNodeRel = objRoute.getRelatedObjects(context, DomainObject.RELATIONSHIP_ROUTE_NODE, typeSelectPattern.getPattern(), slBusSelect, slRelSelect, false, true,
                                (short) 1, null, null);

                        // Rutuja Ekatpure : 14/09/2016
                        // PCM TIGTK-3102 | 15/09/16 :End

                        try {
                            if (mapListRouteNodeRel != null && !mapListRouteNodeRel.isEmpty()) {
                                for (int j = 0; j < mapListRouteNodeRel.size(); j++) {
                                    Map<?, ?> connectIdMap = (Map<?, ?>) mapListRouteNodeRel.get(j);
                                    strRelId = (String) connectIdMap.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                                    DomainRelationship domRelRouteNode = DomainRelationship.newInstance(context, strRelId);
                                    // Added by Pooja Mantri - TIGTK-2741
                                    ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                                    domRelRouteNode.setAttributeValue(context, DomainObject.ATTRIBUTE_SCHEDULED_COMPLETION_DATE, strRouteDueDateTime);
                                }
                            }

                            // PCM : TIGTK-6301 : 12/04/2017 : AB : START
                            if ("Stopped".equalsIgnoreCase(strRouteStatus) || "Finished".equalsIgnoreCase(strRouteStatus)) {
                                // Restarting the already connected Route
                                Route route = new Route(strRouteID);
                                route.resume(context);
                            } else {
                                objRoute.setAttributeValue(context, DomainConstants.ATTRIBUTE_ROUTE_STATUS, "Started");
                                objRoute.setState(context, "In Process");
                            }
                            // PCM : TIGTK-6301 : 12/04/2017 : AB : END

                        } catch (Exception e) {
                            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
                            logger.error("Error in startEvaluationReviewersListRoute: ", e);
                            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
                            throw e;
                        } finally {
                            ContextUtil.popContext(context);
                            // Added by Pooja Mantri - TIGTK-2741
                        }
                    }

                }
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in startEvaluationReviewersListRoute: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        } finally {
            if (isContextPushed) {
                ContextUtil.popContext(context);
            }
        }
    }

    /**
     * @param context
     * @param args
     * @throws Exception
     */

    public void demoteCRWithCommentstoSubmitState(Context context, String args[]) throws Exception {
        boolean isContextPushed = false;
        try {
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            HashMap<?, ?> requestMap = (HashMap<?, ?>) programMap.get(ChangeConstants.REQUEST_MAP);

            // Get the Change Request ID and demote to Submit state
            String strCRobjectId = (String) requestMap.get("objectId");
            DomainObject domCRObj = new DomainObject(strCRobjectId);

            String contextUser = context.getUser();
            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);

            String tempContextUser = context.getCustomData("contextUser");

            if (UIUtil.isNullOrEmpty(tempContextUser)) {
                context.setCustomData("contextUser", contextUser);
            }

            try {
                MqlUtil.mqlCommand(context, false, "trigger off", false);
                domCRObj.setState(context, "Evaluate");
            } catch (Exception e) {
                // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
                logger.error("Error in demoteCRWithCommentstoSubmitState: ", e);
                // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
                throw e;
            } finally {
                MqlUtil.mqlCommand(context, false, "trigger on", false);
            }

            ContextUtil.popContext(context);
            domCRObj.setState(context, "Submit");
            // TIGTK-7595 : TS : 20/9/2017:start
            pss.ecm.impactanalysis.ImpactAnalysisUtil_mxJPO.reviseImpactAnalysisStructure(context, strCRobjectId);
            // TIGTK-7595 : TS : 20/9/2017:end

            // TIGTK-5707 - 05-07-2018 - START
            domCRObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CRREQUESTEDASSESSMENTENDDATE, DomainConstants.EMPTY_STRING);
            // TIGTK-5707 - 05-07-2018 - END

            // TIGTK-10048 : TS : 21/9/2017:start
            String relpattern = PropertyUtil.getSchemaProperty(context, "relationship_ObjectRoute");
            String typePattern = PropertyUtil.getSchemaProperty(context, "type_Route");

            StringList slObjectSle = new StringList(1);
            slObjectSle.addElement(DomainConstants.SELECT_ID);
            slObjectSle.addElement(DomainConstants.SELECT_NAME);
            slObjectSle.addElement(DomainConstants.SELECT_CURRENT);

            StringList slRelSle = new StringList(1);
            slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);
            MapList mlConnectedRoute = domCRObj.getRelatedObjects(context, relpattern, typePattern, slObjectSle, slRelSle, false, true, (short) 1, null, null, 0);
            Iterator<Map<?, ?>> itrRoutes = mlConnectedRoute.iterator();
            String strRouteID = DomainConstants.EMPTY_STRING;
            while (itrRoutes.hasNext()) {
                Map<?, ?> mpRoute = itrRoutes.next();
                strRouteID = (String) mpRoute.get(DomainConstants.SELECT_ID);
                String sUser = context.getUser();
                Route route = new Route(strRouteID);
                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, TigerConstants.PERSON_USER_AGENT), "", "");
                isContextPushed = true;
                route.setOwner(context, sUser);
                if (isContextPushed) {
                    ContextUtil.popContext(context);
                    isContextPushed = false;
                }
                route.setAttributeValue(context, DomainConstants.ATTRIBUTE_ROUTE_STATUS, "Stopped");
            }
            // TIGTK-10048 : TS : 21/9/2017:end
            // BR002:Rutuja Ekatpure:15/9/2017:start
            Map<String, String> payload = new HashMap<String, String>();
            emxNotificationUtil_mxJPO.objectNotification(context, strCRobjectId, "PSS_CRDemoteNotificationToCM", payload);
            emxNotificationUtil_mxJPO.objectNotification(context, strCRobjectId, "PSS_CRDemoteNotificationToProgProjMember", payload);
            // BR002:Rutuja Ekatpure:15/9/2017:End
        } catch (Exception progress) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in demoteCRWithCommentstoSubmitState: ", progress);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw progress;
        } finally {
            if (isContextPushed) {
                ContextUtil.popContext(context);
                isContextPushed = false;
            }
        }
    }

    public int demoteCRFromInReview(Context context, String args[]) throws Exception {

        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }
        try {
            String objectId = args[0];
            StringBuffer processStr = new StringBuffer();

            processStr.append("JSP:postProcess");
            processStr.append("|");
            processStr.append("commandName=");
            processStr.append("PSS_DemoteCRfromJSP");
            processStr.append("|");
            processStr.append("objectId=");
            processStr.append(objectId);

            MqlUtil.mqlCommand(context, "notice $1", processStr.toString());

            return 1;

        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in demoteCRFromInReview: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
    }

    // Custom Check trigger method to check when the Change Request is promoted
    // from In Review to In Process state
    public int checkRejectedImpactAnalysisRoute(Context context, String args[]) throws Exception {
        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }
        String strMessage = "";
        String strRouteID;
        String strCurrent;
        String strTaskID;
        String strattrRouteStatus;
        String strAttrApprovalStatus;
        int retValue = 0;

        try {
            String objectId = args[0];
            DomainObject objCR = new DomainObject(objectId);
            String relpattern = PropertyUtil.getSchemaProperty(context, "relationship_ObjectRoute");
            String typePattern = PropertyUtil.getSchemaProperty(context, "type_Route");

            StringList slObjectSle = new StringList(1);
            slObjectSle.addElement(DomainConstants.SELECT_ID);
            slObjectSle.addElement(DomainConstants.SELECT_NAME);
            slObjectSle.addElement(DomainConstants.SELECT_CURRENT);

            StringList slRelSle = new StringList(1);
            slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

            // Connected Route Objects
            // TIGTK-3351 -- KWagh - Start
            MapList mlObjConnected = objCR.getRelatedObjects(context, relpattern, typePattern, slObjectSle, slRelSle, false, true, (short) 1, null, null, 0);
            if (mlObjConnected.size() > 0) {
                for (int i = 0; i < mlObjConnected.size(); i++) {
                    Map<?, ?> mRouteObj = (Map<?, ?>) mlObjConnected.get(i);
                    strRouteID = (String) mRouteObj.get(DomainConstants.SELECT_ID);
                    DomainObject objRoute = new DomainObject(strRouteID);
                    strattrRouteStatus = objRoute.getAttributeValue(context, DomainConstants.ATTRIBUTE_ROUTE_STATUS);

                    String sRouteBaseState = objRoute.getInfo(context,
                            "to[" + DomainConstants.RELATIONSHIP_OBJECT_ROUTE + "].attribute[" + PropertyUtil.getSchemaProperty(context, "attribute_RouteBaseState") + "]");

                    // Check Attributes "Route Base State" and "Route Status"
                    if (("state_Evaluate".equals(sRouteBaseState)) && (strattrRouteStatus.equals("Stopped"))) {

                        String srelpattern = PropertyUtil.getSchemaProperty(context, "relationship_RouteTask");
                        String stypePattern = PropertyUtil.getSchemaProperty(context, "type_InboxTask");

                        // Connected Inbox Task Objects
                        MapList mConnectedTasks = objRoute.getRelatedObjects(context, srelpattern, stypePattern, slObjectSle, slRelSle, true, false, (short) 1, null, null, 0);

                        if (mConnectedTasks.size() > 0) {
                            for (int j = 0; j < mConnectedTasks.size(); j++) {
                                Map<?, ?> mTaskObj = (Map<?, ?>) mConnectedTasks.get(j);
                                strTaskID = (String) mTaskObj.get(DomainConstants.SELECT_ID);
                                DomainObject domObjTask = new DomainObject(strTaskID);
                                strAttrApprovalStatus = domObjTask.getAttributeValue(context, "Approval Status");
                                strCurrent = (String) mTaskObj.get(DomainConstants.SELECT_CURRENT);

                                // Check Attribute " Approval Status" and Tasks current state
                                // Task is rejected then block promotion
                                if (("Complete".equals(strCurrent)) && (strAttrApprovalStatus.equals("Reject"))) {

                                    // Display alert and Block Prpmotion
                                    strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                                            "PSS_EnterpriseChangeMgt.Alert.RejectImpactedAnalysisRouteConnected");

                                    MqlUtil.mqlCommand(context, "notice $1", strMessage);

                                    // PCM TIGTK-3795 | 14/12/16 :Pooja Mantri : Start
                                    retValue = 1;
                                    // PCM TIGTK-3795 | 14/12/16 :Pooja Mantri : End

                                }
                                // TIGTK-3351 -- KWagh - End
                            }

                        }
                    }
                }
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkRejectedImpactAnalysisRoute: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }

        return retValue;
    }

    /**
     * Description : This method is used get the calendar date for Route Due Date Modified for TIGTK-3766
     * @author abhalani
     * @args
     * @Date December 14, 2016
     */

    public String getRouteDueDate(Context context, String[] args) throws Exception {
        SimpleDateFormat _mxDateFormat = new SimpleDateFormat(eMatrixDateFormat.getEMatrixDateFormat(), Locale.US);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, -1);
        cal.add(Calendar.DATE, 7);
        Date date = cal.getTime();
        String strDate = _mxDateFormat.format(date);
        return strDate;
    }

    public String getSystemDate(Context context, String[] args) throws Exception {
        SimpleDateFormat _mxDateFormat = new SimpleDateFormat(eMatrixDateFormat.getEMatrixDateFormat(), Locale.US);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, -1);
        Date date = cal.getTime();
        String strDate = _mxDateFormat.format(date);
        return strDate;
    }

    /**
     * Description : This is Access Function on Change Request Form for show the Parallel Track and Parallel Track comment Field
     * @Date : Modified by PCM : TIGTK-4108 : 04/04/2017 : AB
     * @param context
     * @param args
     *            Object ID of CR and mode of Form
     * @return boolean value
     * @throws Exception
     */
    public boolean showParallelTrackFieldForEdit(Context context, String[] args) throws Exception {
        boolean showParallelTrackField = false;
        HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
        String strCRId = (String) programMap.get("objectId");
        // Get the mode of Change Request Form
        String strCRFormMode = (String) programMap.get("mode");
        try {
            showParallelTrackField = false;
            String strLoggedInUser = context.getUser();
            String strLoggedUserSecurityContext = PersonUtil.getDefaultSecurityContext(context, strLoggedInUser);

            if (UIUtil.isNotNullAndNotEmpty(strLoggedUserSecurityContext)) {
                String strLoggerUserRole = (strLoggedUserSecurityContext.split("[.]")[0]);
                DomainObject domChangeRequest = new DomainObject(strCRId);
                String strCurent = domChangeRequest.getInfo(context, DomainObject.SELECT_CURRENT);
                StringList slRole = new StringList();
                slRole.add(TigerConstants.ROLE_PSS_PROGRAM_MANAGER);
                slRole.add(TigerConstants.ROLE_PSS_CHANGE_COORDINATOR);
                pss.ecm.ui.CommonUtil_mxJPO objCommoUtil = new pss.ecm.ui.CommonUtil_mxJPO(context, args);
                StringList slProjectMember = objCommoUtil.getProgramProjectTeamMembersForChange(context, strCRId, slRole, false);

                // Check conditions for Show the Parallel Track and Parallel Track Comment field
                if (strCRFormMode.equalsIgnoreCase("view") && !TigerConstants.STATE_PSS_CR_CREATE.equalsIgnoreCase(strCurent)) {
                    showParallelTrackField = true;
                } else if (strCRFormMode.equalsIgnoreCase("edit") && slProjectMember.contains(strLoggedInUser) && slRole.contains(strLoggerUserRole) && "Submit".equals(strCurent)) {
                    showParallelTrackField = true;
                }
            }

        } catch (Exception e) {
            // TIGTK-8051 - 29-05-2017 - Rutuja Ekatpure - START
            if (UIUtil.isNullOrEmpty(strCRFormMode)) {
                showParallelTrackField = true;
            }
            // TIGTK-8051 - 29-05-2017 - Rutuja Ekatpure - End
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in showParallelTrackFieldForEdit: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
        }

        return showParallelTrackField;
    }

    public String getChangeManagerForProgramProject(Context context, String args[]) throws Exception {
        String strResult = EMPTY_STRING;
        try {
            final String SELECT_ATTRIBUTE_PSS_ROLE = "attribute[" + TigerConstants.ATTRIBUTE_PSS_ROLE + "]";
            final String SELECT_ATTRIBUTE_PSS_POSITION = "attribute[" + TigerConstants.ATTRIBUTE_PSS_POSITION + "]";

            final String POSITION_LEAD = "Lead";
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            String strProgramProjectOID = (String) programMap.get("programProjectObjectId");
            StringList objSelect = new StringList();
            objSelect.add(DomainConstants.SELECT_ID);
            objSelect.add(DomainConstants.SELECT_NAME);
            StringList relSelect = new StringList();
            relSelect.add(DomainConstants.SELECT_RELATIONSHIP_ID);
            relSelect.add(SELECT_ATTRIBUTE_PSS_POSITION);
            relSelect.add(SELECT_ATTRIBUTE_PSS_ROLE);
            if (UIUtil.isNotNullAndNotEmpty(strProgramProjectOID)) {
                DomainObject domProgramProjectObject = DomainObject.newInstance(context, strProgramProjectOID);
                // TIGTK-12983 : JV : START
                String programProjectType=domProgramProjectObject.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_OWNERSHIP);
                String roleToCheck = TigerConstants.ROLE_PSS_CHANGE_COORDINATOR;
                if(TigerConstants.ATTRIBUTE_PSS_USERTYPE_RANGE_JV.equalsIgnoreCase(programProjectType)) {
                    roleToCheck = TigerConstants.ROLE_PSS_CHANGE_COORDINATOR_JV;
                }
                // TIGTK-12983 : JV : END
                MapList mlConnectedMembersList = domProgramProjectObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS, TYPE_PERSON, objSelect, relSelect, false, true,
                        (short) 0, null, null, 0);
                if (mlConnectedMembersList.size() > 0) {
                    for (int i = 0; i < mlConnectedMembersList.size(); i++) {
                        Map<?, ?> mapConnectedMemberDetails = (Map<?, ?>) mlConnectedMembersList.get(i);
                        String strPositionValue = (String) mapConnectedMemberDetails.get(SELECT_ATTRIBUTE_PSS_POSITION);
                        String strRoleValue = (String) mapConnectedMemberDetails.get(SELECT_ATTRIBUTE_PSS_ROLE);
                        // TIGTK-5890 : PTE : 4/7/2017 modification done in Role OOTB to custom
                        if (strRoleValue.equals(roleToCheck) && strPositionValue.equals(POSITION_LEAD)) {
                            String strPersonOID = (String) mapConnectedMemberDetails.get(DomainConstants.SELECT_ID);
                            String strPersonName = (String) mapConnectedMemberDetails.get(DomainConstants.SELECT_NAME);
                            strResult = strPersonOID + "|" + strPersonName;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("In method getChangeManagerForProgramProject: ", e);
        }
        return strResult;
    }

    // PCM TIGTK-4255:21/3/2017:Rutuja Ekatpure:start
    @com.matrixone.apps.framework.ui.IncludeOIDProgramCallable
    public StringList getPromotedChildProgProForChange(Context context, String[] args) throws Exception {
        StringList lstProgramProjectObjects = new StringList();

        try {
            // from context user get collaborative space
            String strProject = PersonUtil.getDefaultProject(context, context.getUser());
            // PCM TIGTK-6573| 14/04/17 :KWagh : Start
            // where clause
            String strwhere = "current!= " + TigerConstants.STATE_ACTIVE + " && current!= " + TigerConstants.STATE_OBSOLETE + " && current!= \"" + TigerConstants.STATE_NONAWARDED
                    + "\" && project == \"" + strProject + "\" && from[" + TigerConstants.RELATIONSHIP_PSS_SUBPROGRAMPROJECT + "]==False";
            // PCM TIGTK-6573| 14/04/17 :KWagh : Start
            StringList objSelect = new StringList();
            objSelect.add(DomainConstants.SELECT_ID);
            // find program project
            MapList mlProgramProjectObjectsList = DomainObject.findObjects(context, TigerConstants.TYPE_PSS_PROGRAMPROJECT, TigerConstants.VAULT_ESERVICEPRODUCTION, strwhere, objSelect);
            int iSize = mlProgramProjectObjectsList.size();
            // sort maplist by date modified
            mlProgramProjectObjectsList.sort(DomainConstants.SELECT_ORIGINATED, "descending", "date");

            for (int i = 0; i < iSize; i++) {
                HashMap<?, ?> mapProgramProjectObject = (HashMap<?, ?>) mlProgramProjectObjectsList.get(i);
                DomainObject domProgProj = DomainObject.newInstance(context, (String) mapProgramProjectObject.get(DomainConstants.SELECT_ID));
                // get list of members connected to program project
                StringList strProgramProjectMembers = domProgProj.getInfoList(context, "from[PSS_ConnectedMembers].to.name");

                if (strProgramProjectMembers.contains(context.getUser())) {
                    lstProgramProjectObjects.add(mapProgramProjectObject.get(DomainConstants.SELECT_ID));
                }
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getPromotedChildProgProForChange: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }

        // TS-185 Issue-Create From SLC and Change Management -- TIGTK -6312 : 09/08/2017 : KWagh
        if (lstProgramProjectObjects.isEmpty()) {

            lstProgramProjectObjects.add(DomainConstants.EMPTY_STRING);
        }

        // TS-185 Issue-Create From SLC and Change Management -- TIGTK -6312 : 09/08/2017 : KWagh
        return lstProgramProjectObjects;
    }
    // PCM TIGTK-4255:21/3/2017:Rutuja Ekatpure:End

    @com.matrixone.apps.framework.ui.IncludeOIDProgramCallable
    public StringList getConnectedChangeCoordToProgProjForChange(Context context, String[] args) throws Exception {
        StringList lstReturn = new StringList();
        try {

            final String SELECT_ATTRIBUTE_PSS_ROLE = "attribute[" + TigerConstants.ATTRIBUTE_PSS_ROLE + "]";
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            String strProgramProjectOID = (String) programMap.get("programProjectCode");
            // TIGTK-7452 : Monika Naruni : 5/5/2017 : START
            if (strProgramProjectOID != null) {
                // TIGTK-7452 : Monika Naruni : 5/5/2017 : END
                DomainObject domProgramProjectObject = DomainObject.newInstance(context, strProgramProjectOID);
                StringList objSelect = new StringList();
                objSelect.add(DomainConstants.SELECT_ID);
                objSelect.add(DomainConstants.SELECT_NAME);
                StringList relSelect = new StringList();
                relSelect.add(DomainConstants.SELECT_RELATIONSHIP_ID);
                relSelect.add(SELECT_ATTRIBUTE_PSS_ROLE);
                // TIGTK-5890 : PTE : 4/7/2017 : START
                // TIGTK-12983 : JV : START
                String strRelWhereclause = DomainConstants.EMPTY_STRING;
                String typeProgramProject=domProgramProjectObject.getInfo(context, "attribute["+TigerConstants.ATTRIBUTE_PSS_OWNERSHIP+"]");
                if(TigerConstants.ATTRIBUTE_PSS_USERTYPE_RANGE_JV.equalsIgnoreCase(typeProgramProject)) {                
                    strRelWhereclause = " attribute[" + TigerConstants.ATTRIBUTE_PSS_ROLE + "] == \"" + TigerConstants.ROLE_PSS_CHANGE_COORDINATOR_JV + "\"";                
                } else {
                    strRelWhereclause = " attribute[" + TigerConstants.ATTRIBUTE_PSS_ROLE + "] == \"" + TigerConstants.ROLE_PSS_CHANGE_COORDINATOR + "\"";
                }
                // TIGTK-12983 : JV : END
                // TIGTK-5890 : PTE : 4/7/2017 : END
                MapList mlConnectedChangeCoordMembersList = domProgramProjectObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS, TYPE_PERSON, objSelect, relSelect,
                        false, true, (short) 0, null, strRelWhereclause, 0);
                // TIGTK-7452 : Monika Naruni : 5/5/2017 : START
                // TIGTK-7741 Find Bug correction
                if (!mlConnectedChangeCoordMembersList.isEmpty()) {
                    // TIGTK-7452 : Monika Naruni : 5/5/2017 : END
                    for (int i = 0; i < mlConnectedChangeCoordMembersList.size(); i++) {
                        Map<?, ?> mapConnectedChangeCoordMember = (Map<?, ?>) mlConnectedChangeCoordMembersList.get(i);
                        String strChangeCoordOID = (String) mapConnectedChangeCoordMember.get(DomainConstants.SELECT_ID);
                        lstReturn.add(strChangeCoordOID);
                    }

                }
            }
            // TIGTK-7452 : Monika Naruni : 5/5/2017 : START
            if (lstReturn.isEmpty())
                lstReturn.add("");
            // TIGTK-7452 : Monika Naruni : 5/5/2017 : END

        } catch (Exception ex) {
            logger.error("in method getConnectedChangeCoordToProgProjForChange: ", ex);
        }
        return lstReturn;
    }

    @com.matrixone.apps.framework.ui.IncludeOIDProgramCallable
    public StringList getIncludeOIDForChangeManagerSearch(Context context, String[] args) throws Exception {
        StringList lstReturn = new StringList();
        try {

            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            String strChangeObjectId = (String) programMap.get("objectId");
            DomainObject domChangeObject = DomainObject.newInstance(context, strChangeObjectId);
            StringList busSelect = new StringList();
            busSelect.add("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
            busSelect.add("from[" + ChangeConstants.RELATIONSHIP_CHANGE_COORDINATOR + "].to.id");
            Map<String, Object> mapConnectedObjects = domChangeObject.getInfo(context, busSelect);
            String strProgramProjectOID = (String) mapConnectedObjects.get("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
            String strChangeCoordinatorOID = (String) mapConnectedObjects.get("from[" + ChangeConstants.RELATIONSHIP_CHANGE_COORDINATOR + "].to.id");
            DomainObject domProgramProjectObject = DomainObject.newInstance(context, strProgramProjectOID);
            // TIGTK-5890 : PTE : 4/7/2017 : START
            String strRelWhereclause = " attribute[" + TigerConstants.ATTRIBUTE_PSS_ROLE + "] == \"" + TigerConstants.ROLE_PSS_CHANGE_COORDINATOR + "\"";
            // TIGTK-5890 : PTE : 4/7/2017 : END
            StringList objSelect = new StringList();
            objSelect.add(DomainConstants.SELECT_ID);
            MapList mlConnectedChangeCoordMembersList = domProgramProjectObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS, TYPE_PERSON, objSelect, null, false, true,
                    (short) 0, null, strRelWhereclause, 0);
            for (int i = 0; i < mlConnectedChangeCoordMembersList.size(); i++) {
                Map<?, ?> mapConnectedChangeCoordMember = (Map<?, ?>) mlConnectedChangeCoordMembersList.get(i);
                String strChangeCoordOID = (String) mapConnectedChangeCoordMember.get(DomainConstants.SELECT_ID);
                if (!strChangeCoordinatorOID.equals(strChangeCoordOID)) {
                    lstReturn.add(strChangeCoordOID);
                }
            }

        } catch (Exception ex) {
            logger.error("Runtime Exception in method getIncludeOIDForChangeManagerSearch: ", ex);
        }
        return lstReturn;
    }

    /**
     * Modified by PCM : TIGTK-3971 : 19/01/2017 : AB
     * @param context
     * @param args
     * @throws Exception
     */
    public void connectChangeOrder(Context context, String args[]) throws Exception {
        String sChangeCoordinatorId = "";
        String sReportedAgainstId = "";
        String sResponsibleOrganizationId = "";
        String PROJECT = "project";
        String sProject = "";
        String sOrganization = "";

        try {
            PSS_enoECMChangeOrder_mxJPO changeOrder = new PSS_enoECMChangeOrder_mxJPO(context, null);
            HashMap<?, ?> params = (HashMap<?, ?>) JPO.unpackArgs(args);
            HashMap<?, ?> requestMap = (HashMap<?, ?>) params.get("requestMap");
            String strCRId = (String) requestMap.get("objectId");

            HashMap<?, ?> paramMap = (HashMap<?, ?>) params.get("paramMap");
            String strCOObjId = (String) paramMap.get("newObjectId");

            // create CR Object
            DomainObject domObjCR = new DomainObject(strCRId);
            DomainObject objCOobj = new DomainObject(strCOObjId);

            StringList slobjSelects = new StringList();
            slobjSelects.add(DomainConstants.SELECT_ID);
            slobjSelects.add(DomainConstants.SELECT_TYPE);

            String sChangeCoordinator = "from[" + ChangeConstants.RELATIONSHIP_CHANGE_COORDINATOR + "].to.id";
            String sReportedAgainst = "from[" + ChangeConstants.RELATIONSHIP_REPORTED_AGAINST_CHANGE + "].to.id";
            String strCOconnectedProgramid = objCOobj.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
            String sResponsibleOrganization = SELECT_ORGANIZATION;

            StringList selectable = new StringList();
            selectable.addElement(sChangeCoordinator);
            selectable.addElement(sReportedAgainst);
            selectable.addElement(sResponsibleOrganization);
            selectable.addElement(SELECT_OWNER);
            selectable.addElement(PROJECT);
            selectable.addElement(SELECT_ORGANIZATION);

            Map<String, Object> resultList = domObjCR.getInfo(context, selectable);
            sChangeCoordinatorId = (String) resultList.get(sChangeCoordinator);
            sReportedAgainstId = (String) resultList.get(sReportedAgainst);
            sResponsibleOrganizationId = (String) resultList.get(sResponsibleOrganization);
            sProject = (String) resultList.get(PROJECT);
            sOrganization = (String) resultList.get(SELECT_ORGANIZATION);
            // TIGTK-14861 - rgarbhe
            String strCOCollabSpace = objCOobj.getInfo(context, "project");
            // Connects CR and CO
            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            objCOobj.connect(context, ChangeConstants.RELATIONSHIP_CHANGE_ORDER, domObjCR, true);
            ContextUtil.popContext(context);
            // Copy ResponsibleOrganization from CR to CO
            if (!ChangeUtil.isNullOrEmpty(sResponsibleOrganizationId)) {
                objCOobj.setPrimaryOwnership(context, sProject, sOrganization);
            }

            // Copy ChangeCoordinator from CR to CO
            if (!ChangeUtil.isNullOrEmpty(sChangeCoordinatorId)) {
                DomainRelationship.connect(context, objCOobj, ChangeConstants.RELATIONSHIP_CHANGE_COORDINATOR, DomainObject.newInstance(context, sChangeCoordinatorId));
            }
            // Copy ReportedAgainst from CR to CO
            if (!ChangeUtil.isNullOrEmpty(sReportedAgainstId)) {
                DomainRelationship.connect(context, objCOobj, ChangeConstants.RELATIONSHIP_REPORTED_AGAINST_CHANGE, DomainObject.newInstance(context, sReportedAgainstId));
            }

            // Added for displaying Effectivity field on CO Properties page
            if (ChangeUtil.isCFFInstalled(context)) {

                String changeName = (String) resultList.get(DomainConstants.SELECT_NAME);
                String changeVault = (String) resultList.get(DomainConstants.SELECT_VAULT);

                DomainObject neObj = DomainObject.newInstance(context);
                neObj.createObject(context, TigerConstants.TYPE_NAMED_EFFECTIVITY, changeName, "", TigerConstants.POLICY_NAMED_EFFECTIVITY, changeVault);
                String neObjectId = neObj.getObjectId(context);
                RelationshipType relType = new RelationshipType();
                relType.setName(TigerConstants.RELATIONSHIP_NAMED_EFFECTIVITY);
                objCOobj.addToObject(context, relType, neObjectId);
            }
            // PCM TIGTK-3101 : 19/09/16 : AB : START
            if (!ChangeUtil.isNullOrEmpty(strCRId)) {
                StringList slStandardPartList = new StringList();

                StringList objectSelects = new StringList(1);
                objectSelects.addElement(DomainConstants.SELECT_ID);
                objectSelects.addElement(DomainConstants.SELECT_POLICY);
                objectSelects.addElement(DomainConstants.SELECT_TYPE);
                objectSelects.add("to[" + ChangeConstants.RELATIONSHIP_PART_SPECIFICATION + "].from.policy");
                objectSelects.add("to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "]");
                objectSelects.add("to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].from.current");
                objectSelects.add("to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].from.to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from.current");
                objectSelects.add(DomainConstants.SELECT_CURRENT);
                objectSelects.add("project");// TIGTK-14861 - rgarbhe

                StringList relSelects = new StringList(1);
                relSelects.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);
                relSelects.add(ChangeConstants.SELECT_ATTRIBUTE_REQUESTED_CHANGE);

                StringList slReleaseItem = new StringList();
                slReleaseItem.add("Released");
                slReleaseItem.add("Release");

                // Get CR Related Affected Items
                Map<String, String> mPartToRC = new HashMap<String, String>();
                PSS_enoECMChangeUtil_mxJPO changeUtil = new PSS_enoECMChangeUtil_mxJPO(context, null);
                // PCM TIGTK-3243 : 23/09/16 : AB : START
                String where = "policy != PSS_Development_Part && policy != PSS_MBOM && (current==Released || current==Release || current == 'In Work' || current == Preliminary || current==Review)";
                MapList mlCRAffectedItems = domObjCR.getRelatedObjects(context, // context
                        TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM, // relationship pattern
                        DomainConstants.QUERY_WILDCARD, // object pattern
                        objectSelects, // object selects
                        relSelects, // relationship selects
                        false, // to direction
                        true, // from direction
                        (short) 0, // recursion level
                        where, // object where clause
                        null, // relationship where clause
                        (short) 0, false, // checkHidden
                        true, // preventDuplicates
                        (short) 0, // pageSize
                        null, null, null, null);
                // PCM TIGTK-3243 : 23/09/16 : AB : END
                Map mapAICurrentState = new HashMap();
                if (!mlCRAffectedItems.isEmpty() && mlCRAffectedItems.size() > 0) {
                    StringList slPartType = new StringList();
                    StringList slCADType = new StringList();
                    StringList slActiveCOState = new StringList();
                    slActiveCOState.add(TigerConstants.STATE_PSS_CHANGEORDER_PREPARE);
                    slActiveCOState.add(TigerConstants.STATE_PSS_CHANGEORDER_INWORK);
                    slActiveCOState.add(TigerConstants.STATE_PSS_CHANGEORDER_INAPPROVAL);
                    // TIGTK-8229 :Modiofied by SIE :Start
                    StringList slActiveCAState = new StringList();
                    slActiveCAState.add(TigerConstants.STATE_CHANGEACTION_PENDING);
                    slActiveCAState.add(TigerConstants.STATE_CHANGEACTION_INWORK);
                    slActiveCAState.add(TigerConstants.STATE_CHANGEACTION_INAPPROVAL);
                    // TIGTK-8229 :Modiofied by SIE :End
                    for (int k = 0; k < mlCRAffectedItems.size(); k++) {
                        Map<String, Object> mTemp = (Map<String, Object>) mlCRAffectedItems.get(k);
                        // TIGTK-14861 - rgarbhe - Start
                        // For Collabs space matching
                        String strAffectedItemCollabSpace = (String) mTemp.get("project");
                        if (strCOCollabSpace.equals(strAffectedItemCollabSpace)) {
                            String strConnectedTOCA = (String) mTemp.get("to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "]");
                            StringList slConnectedCOCurrent = changeUtil.getStringListFromMap(context, mTemp,
                                    "to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].from.to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from.current");
                            StringList slSpecsConnectedPartPolicy = changeUtil.getStringListFromMap(context, mTemp, "to[" + ChangeConstants.RELATIONSHIP_PART_SPECIFICATION + "].from.policy");
                            String strPolicy = (String) mTemp.get(DomainObject.SELECT_POLICY);
                            String strPartState = (String) mTemp.get(DomainObject.SELECT_CURRENT);
                            String strCRRequesedChangeValue = (String) mTemp.get(ChangeConstants.SELECT_ATTRIBUTE_REQUESTED_CHANGE);
                            String strAIId = (String) mTemp.get(DomainObject.SELECT_ID);
                            mapAICurrentState.put(strAIId, strPartState);
                            // TIGTK-6870 : Modified by Hiren : Start
                            DomainObject domAI = DomainObject.newInstance(context, strAIId);
                            String strGoveringPrjId = domAI.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT + "].from.id");
                            if (strGoveringPrjId == null) {
                                strGoveringPrjId = DomainConstants.EMPTY_STRING;
                            }
                            if ((UIUtil.isNullOrEmpty(strGoveringPrjId)) || !slReleaseItem.contains(strPartState)
                                    || (slReleaseItem.contains(strPartState) && strGoveringPrjId.equalsIgnoreCase(strCOconnectedProgramid))) {
                                boolean flag = changeOrder.isAffectedItemHasNoActiveCAConnected(slActiveCOState, slConnectedCOCurrent);
                                // TIGTK-8229 :Modiofied by SIE :Start
                                StringList slConnectedCACurrent = changeUtil.getStringListFromMap(context, mTemp, "to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].from.current");
                                boolean flagforCA = changeOrder.isAffectedItemHasNoActiveCAConnected(slActiveCAState, slConnectedCACurrent);

                                // PCM: TIGTK-7779 : 17/05/2017: TS: START

                                /*
                                 * if (!(ChangeConstants.FOR_RELEASE.equalsIgnoreCase(strCRRequesedChangeValue) && (TigerConstants.STATE_RELEASED_CAD_OBJECT.equalsIgnoreCase(strPartState) ||
                                 * TigerConstants.STATE_PART_RELEASE.equalsIgnoreCase(strPartState)))) {
                                 */
                                if ((ChangeConstants.FOR_RELEASE.equalsIgnoreCase(strCRRequesedChangeValue))
                                        && (TigerConstants.STATE_RELEASED_CAD_OBJECT.equalsIgnoreCase(strPartState) || TigerConstants.STATE_PART_RELEASE.equalsIgnoreCase(strPartState))) {
                                    strCRRequesedChangeValue = ChangeConstants.FOR_REVISE;
                                }
                                if ((TigerConstants.POLICY_PSS_ECPART.equals(strPolicy)) && (flagforCA || slReleaseItem.contains(strPartState)) && flag) {
                                    slPartType.add(strAIId);
                                    mPartToRC.put(strAIId, strCRRequesedChangeValue);
                                } else if (((TigerConstants.POLICY_PSS_CADOBJECT.equals(strPolicy)) || (TigerConstants.POLICY_PSS_Legacy_CAD.equals(strPolicy)))
                                        && (flagforCA || slReleaseItem.contains(strPartState)) && flag) {

                                    // PCM : TIGTK-9060 : 28/07/2017 : AB : START
                                    Map<String, DomainObject> mapObjects = new HashMap<String, DomainObject>();
                                    DomainObject domAffectedItem = DomainObject.newInstance(context, strAIId);
                                    mapObjects.put("domCADItem", domAffectedItem);
                                    boolean bolAllowConnection = changeOrder.checkCADValidationForAddIntoCO(context, JPO.packArgs(mapObjects));

                                    if (bolAllowConnection) {
                                        slCADType.add(strAIId);
                                        mPartToRC.put(strAIId, strCRRequesedChangeValue);
                                    }

                                    // PCM : TIGTK-9060 : 28/07/2017 : AB : END
                                } else if ((TigerConstants.POLICY_STANDARDPART.equals(strPolicy)) && (flagforCA || slReleaseItem.contains(strPartState)) && flag) {
                                    slStandardPartList.add(strAIId);
                                    mPartToRC.put(strAIId, strCRRequesedChangeValue);
                                } // TIGTK-8229 :Modiofied by SIE :END
                                  // }
                            }
                            // TIGTK-6870 : Modified by Hiren : End
                        } // TIGTK-14861 - rgarbhe - End
                    }
                    StringList slValidCADId = changeUtil.checkStateOfCADForTransferAffectedItemsFromCRToCO(context, slPartType, slStandardPartList, slCADType, mapAICurrentState);
                    slCADType = slValidCADId;
                    // PCM: TIGTK-7779 : 17/05/2017: TS: END

                    if (slCADType.size() > 0) {
                        // create new CA and attach all AI of CAD
                        String CAId = changeUtil.createNewCA(context, strCOObjId);
                        DomainObject domNewCA = DomainObject.newInstance(context, CAId);
                        domNewCA.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CATYPE, "CAD");
                        Map<?, ?> mCATOPartMap = DomainRelationship.connect(context, domNewCA, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, true,
                                ((String) FrameworkUtil.join(slCADType, ",")).split(","));
                        changeUtil.setRequestedChange(context, mCATOPartMap, mPartToRC);
                    }

                    if (slPartType.size() > 0) {
                        // create new CA and attach all AI of Part
                        String CAId = changeUtil.createNewCA(context, strCOObjId);
                        DomainObject domNewCA = DomainObject.newInstance(context, CAId);
                        domNewCA.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CATYPE, "Part");
                        Map<?, ?> mCATOPartMap = DomainRelationship.connect(context, domNewCA, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, true,
                                ((String) FrameworkUtil.join(slPartType, ",")).split(","));
                        changeUtil.setRequestedChange(context, mCATOPartMap, mPartToRC);
                    }

                    if (slStandardPartList.size() > 0) {
                        // TIGTK-9287| 30/08/17 : Start
                        Map mCATOStdPartMap = new HashMap<>();
                        // create new CA and attach all AI of Part
                        String CAId = changeUtil.createNewCA(context, strCOObjId);
                        DomainObject domNewCA = DomainObject.newInstance(context, CAId);
                        domNewCA.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CATYPE, "Standard");

                        Iterator itrstrSTDPartId = slStandardPartList.iterator();

                        while (itrstrSTDPartId.hasNext()) {
                            String strSTDPartId = (String) itrstrSTDPartId.next();
                            DomainObject domSTDPart = DomainObject.newInstance(context, strSTDPartId);

                            Access access = domSTDPart.getAccessMask(context);
                            boolean bAccessToConnect = access.hasToConnectAccess();
                            boolean bIsPushed = false;
                            try {

                                if (!bAccessToConnect) {
                                    ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                                    bIsPushed = true;
                                }
                                DomainRelationship domRelId = DomainRelationship.connect(context, domNewCA, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, domSTDPart);
                                String sdomRelId = domRelId.toString();
                                mCATOStdPartMap.put(strSTDPartId, sdomRelId);
                            } catch (Exception e) {
                                logger.error("Error in STD part connection: ", e);
                            } finally {
                                if (bIsPushed)
                                    ContextUtil.popContext(context);
                            }

                        }

                        changeUtil.setRequestedChange(context, mCATOStdPartMap, mPartToRC);
                    }
                }

            }
            // PCM TIGTK-6325 : 4/14/17 : PTE : END
            String[] strArgs = new String[2];
            strArgs[0] = strCOObjId;
            this.connectRouteTemplateToChangeAction(context, strArgs);
            // PCM TIGTK-6325 : 4/14/17 : PTE : START
            // PCM TIGTK-3101 : 19/09/16 : AB : END
        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in connectChangeOrder: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }

    }

    public int calledFromCheckDuringPromoteCRToComplete(Context context, String args[]) throws Exception {
        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }
        try {
            String strChangeRequestId = args[0];
            boolean bolPromoteCR = true;
            int flagCOCurrent = 0;
            StringList objSelect = new StringList();
            objSelect.add(DomainConstants.SELECT_ID);
            objSelect.add(DomainConstants.SELECT_CURRENT);
            DomainObject domChangeRequestObject = DomainObject.newInstance(context, strChangeRequestId);
            MapList listConnectedChangeOrder = domChangeRequestObject.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_ORDER, TigerConstants.TYPE_PSS_CHANGEORDER, objSelect, null, false,
                    true, (short) 1, null, null);
            // To get the system date
            if (listConnectedChangeOrder.size() > 0) {
                for (int i = 0; i < listConnectedChangeOrder.size(); i++) {
                    Map<?, ?> mapConnectedChangeOrder = (Map<?, ?>) listConnectedChangeOrder.get(i);
                    String strChangeOrderCurrentState = (String) mapConnectedChangeOrder.get(DomainConstants.SELECT_CURRENT);
                    if (strChangeOrderCurrentState.equalsIgnoreCase(TigerConstants.STATE_PSS_CHANGEORDER_CANCELLED)
                            || strChangeOrderCurrentState.equalsIgnoreCase(TigerConstants.STATE_CHANGEORDER_IMPLEMENTED)) {
                    } else {
                        bolPromoteCR = false;
                        flagCOCurrent++;
                    }
                }
            }
            if (bolPromoteCR == false && flagCOCurrent > 1) {
                String strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                        "PSS_EnterpriseChangeMgt.Alert.COsNotInCompleteToCloseChangeRequest");
                MqlUtil.mqlCommand(context, "notice $1", strMessage);
                return 1;
            } else {
                return 0;
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in calledFromCheckDuringPromoteCRToComplete: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }

    }

    public int calledFromOverrideDuringPromoteCRToComplete(Context context, String args[]) throws Exception {
        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }
        try {
            String strChangeRequestId = args[0];
            StringBuffer processStr = new StringBuffer();
            processStr.append("JSP:postProcess");
            processStr.append("|");
            processStr.append("commandName=");
            processStr.append("PSS_PromoteCRToCompleteCommand");
            processStr.append("|");
            processStr.append("objectId=");
            processStr.append(strChangeRequestId);
            MqlUtil.mqlCommand(context, "notice $1", processStr.toString());
        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in calledFromOverrideDuringPromoteCRToComplete: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
        return 1;
    }

    public int promoteCRToComplete(Context context, String args[]) throws Exception {
        try {
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            HashMap<?, ?> requestMap = (HashMap<?, ?>) programMap.get("requestMap");
            String strCRId = (String) requestMap.get("objectId");
            DomainObject domCRID = new DomainObject(strCRId);
            try {
                MqlUtil.mqlCommand(context, false, "trigger off", false);
                domCRID.setState(context, TigerConstants.STATE_COMPLETE_CR);
            } catch (Exception e) {
                // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
                logger.error("Error in promoteCRToComplete: ", e);
                // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
                throw e;
            } finally {
                MqlUtil.mqlCommand(context, false, "trigger on", false);
            }
            return 0;
        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in promoteCRToComplete: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
    }

    public static String getConnectedRouteObjectToProgProjForChange(Context context, String strCRObjectId, String strRouteBaseState) throws Exception {

        String strRouteTemplateName = DomainConstants.EMPTY_STRING;
        //
        String strRouteTemplateRevision = DomainConstants.EMPTY_STRING;
        StringBuffer sbRouteTemplateInfo = new StringBuffer();

        try {

            final String RANGE_DEFAULT_IMPACT_ANALYSIS_ROUTETEMPLATEFORCR = "Default Impact Analysis Route Template for CR";
            final String RANGE_DEFAULT_REVIEW_ROUTETEMPLATEFORCR = "Default Evaluation Review Route Template for CR";
            final String STATE_EVALUATE = "Evaluate";
            final String STATE_IN_REVIEW = "In Review";
            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            DomainObject domCRObject = DomainObject.newInstance(context, strCRObjectId);
            String strProgramProjectId = domCRObject.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");

            DomainObject domProgramProjectObject = DomainObject.newInstance(context, strProgramProjectId);

            StringList busSelect = new StringList();
            Map<String, Object> mapRouteTemplateDetails = null;
            // PCM TIGTK-3474 | 26/10/16 : Pooja Mantri : Start
            if (strRouteBaseState.equals(STATE_IN_REVIEW)) {
                busSelect.add("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "|attribute[" + TigerConstants.ATTRIBUTE_PSS_ROUTETEMPLATETYPE + "]=='"
                        + RANGE_DEFAULT_REVIEW_ROUTETEMPLATEFORCR + "'].to.name");
                busSelect.add("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "|attribute[" + TigerConstants.ATTRIBUTE_PSS_ROUTETEMPLATETYPE + "]=='"
                        + RANGE_DEFAULT_REVIEW_ROUTETEMPLATEFORCR + "'].to.revision");
            }
            if (strRouteBaseState.equals(STATE_EVALUATE)) {
                busSelect.add("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "|attribute[" + TigerConstants.ATTRIBUTE_PSS_ROUTETEMPLATETYPE + "]=='"
                        + RANGE_DEFAULT_IMPACT_ANALYSIS_ROUTETEMPLATEFORCR + "'].to.name");
                busSelect.add("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "|attribute[" + TigerConstants.ATTRIBUTE_PSS_ROUTETEMPLATETYPE + "]=='"
                        + RANGE_DEFAULT_IMPACT_ANALYSIS_ROUTETEMPLATEFORCR + "'].to.revision");

            }

            mapRouteTemplateDetails = domProgramProjectObject.getInfo(context, busSelect);
            strRouteTemplateName = (String) mapRouteTemplateDetails.get("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "].to.name");
            strRouteTemplateRevision = (String) mapRouteTemplateDetails.get("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "].to.revision");
            sbRouteTemplateInfo.append(strRouteTemplateName);
            sbRouteTemplateInfo.append("|");
            sbRouteTemplateInfo.append(strRouteTemplateRevision);
            // PCM TIGTK-3474 | 26/10/16 : Pooja Mantri : Start
            ContextUtil.popContext(context);
        }
        // FindBug Exception is caught when Exception is not thrown: TIGTK-6238 | 18/04/2017 |Harika Varanasi :Starts
        catch (RuntimeException re) {
            throw re;
        }
        // FindBug Exception is caught when Exception is not thrown: TIGTK-6238 | 18/04/2017 |Harika Varanasi :Ends
        catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getConnectedRouteObjectToProgProjForChange: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
        return sbRouteTemplateInfo.toString();
    }

    @com.matrixone.apps.framework.ui.IncludeOIDProgramCallable
    public StringList getContextProgramProjectChangeManager(Context context, String[] args) throws Exception {
        StringList lstReturn = new StringList();
        try {
            final String SELECT_ATTRIBUTE_PSS_ROLE = "attribute[" + TigerConstants.ATTRIBUTE_PSS_ROLE + "]";
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            String strChangeObjectId = (String) programMap.get("objectId");
            DomainObject domChangeObject = DomainObject.newInstance(context, strChangeObjectId);
            StringList busSelect = new StringList();
            busSelect.add("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
            busSelect.add("from[" + ChangeConstants.RELATIONSHIP_CHANGE_COORDINATOR + "].to.id");
            Map<String, Object> mapConnectedObjects = domChangeObject.getInfo(context, busSelect);
            String strProgramProjectOID = (String) mapConnectedObjects.get("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
            DomainObject domProgramProjectObject = DomainObject.newInstance(context, strProgramProjectOID);
            StringList objSelect = new StringList();
            objSelect.add(DomainConstants.SELECT_ID);
            objSelect.add(DomainConstants.SELECT_NAME);
            objSelect.add(DomainConstants.SELECT_CURRENT);
            StringList relSelect = new StringList();
            relSelect.add(DomainConstants.SELECT_RELATIONSHIP_ID);
            relSelect.add(SELECT_ATTRIBUTE_PSS_ROLE);
            // TIGTK-5890 : PTE : 4/7/2017 : START
            
            // TIGTK-12983 : JV : START
            String programProjectType=domProgramProjectObject.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_OWNERSHIP);
            String roleToCheck = TigerConstants.ROLE_PSS_CHANGE_COORDINATOR;
            if(TigerConstants.ATTRIBUTE_PSS_USERTYPE_RANGE_JV.equalsIgnoreCase(programProjectType)) {
                roleToCheck = TigerConstants.ROLE_PSS_CHANGE_COORDINATOR_JV;
            }
            // TIGTK-12983 : JV : END
            
            String strRelWhereclause = " attribute[" + TigerConstants.ATTRIBUTE_PSS_ROLE + "] == \"" + roleToCheck + "\"";
            // TIGTK-5890 : PTE : 4/7/2017 : END
            MapList mlConnectedChangeCoordMembersList = domProgramProjectObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS, TYPE_PERSON, objSelect, relSelect, false,
                    true, (short) 0, null, strRelWhereclause, 0);
            for (int i = 0; i < mlConnectedChangeCoordMembersList.size(); i++) {
                Map<?, ?> mapConnectedChangeCoordMember = (Map<?, ?>) mlConnectedChangeCoordMembersList.get(i);
                String strChangeCoordOID = (String) mapConnectedChangeCoordMember.get(DomainConstants.SELECT_ID);
                // Modified By KWagh - TIGTK-2901 - Start
                String strChangeCoOrdCurrent = (String) mapConnectedChangeCoordMember.get(DomainConstants.SELECT_CURRENT);
                if (strChangeCoOrdCurrent.equalsIgnoreCase("Active")) {
                    lstReturn.add(strChangeCoordOID);
                }
                // Modified By KWagh - TIGTK-2901 - End
            }
        } catch (Exception ex) {
            logger.error("In method getContextProgramProjectChangeManager: ", ex);
        }
        return lstReturn;
    }

    @com.matrixone.apps.framework.ui.ExcludeOIDProgramCallable
    public StringList excludeCurrentOwner(Context context, String[] args) throws Exception {
        StringList lstReturn = new StringList();
        try {
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            String strChangeObjectId = (String) programMap.get("objectId");
            BusinessObject domChangeObject = new BusinessObject(strChangeObjectId);
            /*
             * StringList busSelect = new StringList(); busSelect.add("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id"); busSelect.add("from[" +
             * ChangeConstants.RELATIONSHIP_CHANGE_COORDINATOR + "].to.id");
             * 
             * Map<String, Object> mapConnectedObjects = domChangeObject.getInfo(context, busSelect); String strProgramProjectOID = (String) mapConnectedObjects.get("to[" +
             * TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id"); String strChangeCoordinatorOID = (String) mapConnectedObjects.get("from[" +
             * ChangeConstants.RELATIONSHIP_CHANGE_COORDINATOR + "].to.id"); DomainObject domProgramProjectObject = DomainObject.newInstance(context, strProgramProjectOID); // TIGTK-5890 : PTE :
             * 4/7/2017 : START String strRelWhereclause = " attribute[" + TigerConstants.ATTRIBUTE_PSS_ROLE + "] == \"" + TigerConstants.ROLE_PSS_CHANGE_COORDINATOR + "\""; // TIGTK-5890 : PTE :
             * 4/7/2017 : END StringList objSelect = new StringList(); objSelect.add(DomainConstants.SELECT_ID);
             * 
             * MapList mlConnectedChangeCoordMembersList = domProgramProjectObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS, TYPE_PERSON, objSelect, null, false,
             * true, (short) 0, null, strRelWhereclause, 0); for (int i = 0; i < mlConnectedChangeCoordMembersList.size(); i++) { Map<?, ?> mapConnectedChangeCoordMember = (Map<?, ?>)
             * mlConnectedChangeCoordMembersList.get(i); String strChangeCoordOID = (String) mapConnectedChangeCoordMember.get(DomainConstants.SELECT_ID); // PCM : TIGTK-8258 : PSE : 19-07-2017 :
             * START if (UIUtil.isNotNullAndNotEmpty(strChangeCoordinatorOID) && UIUtil.isNotNullAndNotEmpty(strChangeCoordOID) && strChangeCoordinatorOID.equals(strChangeCoordOID)) {
             * lstReturn.addElement(strChangeCoordOID); // PCM : TIGTK-8258 : PSE : 19-07-2017 : END } }
             */

            // PCM : TIGTK-8258 : PSE : 19-07-2017 : START
            String strChangeObjectOwnerId = PersonUtil.getPersonObjectID(context, domChangeObject.getOwner(context).getName());
            if (!(lstReturn.contains(strChangeObjectOwnerId))) {
                lstReturn.addElement(strChangeObjectOwnerId);
            }
            // PCM : TIGTK-8258 : PSE : 19-07-2017 : END
        } catch (Exception ex) {
            logger.error("In method excludeCurrentOwner: ", ex);
        }
        return lstReturn;
    }

    // RFC-033-AB-IT12
    /**
     * This method is used for Disconnect Affected Item from CR
     * @param context
     * @param args
     *            - Selected Item for Remove Affected Item in CR which is comes from PSS_ECMFullSearchPostProcess.jsp
     * @return StringList - Contains the Object Id of the Part which is Passed in method argument
     * @throws Exception
     */

    public boolean disconnectAffectedItemFromCR(Context context, String[] args) throws Exception {

        boolean retStatus = true;
        try {
            DomainObject domCR = new DomainObject();
            // PCM TIGTK-3797 | 23/12/16 :Pooja Mantri : Start
            // PCM : TIGTK-7054 : 25/04/2017 : AB : START
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            HashMap<?, ?> mapObjIdRelId = (HashMap<?, ?>) programMap.get("mapObjIdRelId");
            String strCRID = (String) programMap.get("strCRID");
            if (UIUtil.isNotNullAndNotEmpty(strCRID)) {
                domCR = DomainObject.newInstance(context, strCRID);
            }
            StringList slRelIdList = (StringList) mapObjIdRelId.get("RelId");
            StringList slObjIdList = (StringList) mapObjIdRelId.get("ObjId");
            StringList slObjectSelects = new StringList();
            slObjectSelects.add(DomainConstants.SELECT_POLICY);
            slObjectSelects.add(DomainConstants.SELECT_CURRENT);

            // Get the RelID and ObjectID which is selected for Disconnect item from CR
            if (!slRelIdList.isEmpty() && !slObjIdList.isEmpty()) {
                int intSize = slObjIdList.size();
                for (int i = 0; i < intSize; i++) {
                    // Get the affected Item's info like policy and current state
                    String strItemID = (String) slObjIdList.get(i);
                    DomainObject domItem = DomainObject.newInstance(context, strItemID);
                    DomainObject domObject = DomainObject.newInstance(context, strItemID);
                    Map<String, Object> mapItemInfo = (Map<String, Object>) domObject.getInfo(context, slObjectSelects);
                    String strItemPolicy = (String) mapItemInfo.get(DomainConstants.SELECT_POLICY);
                    String strItemState = (String) mapItemInfo.get(DomainConstants.SELECT_CURRENT);
                    String strRelID = (String) slRelIdList.get(i);

                    // Pushcontext for remove item if Item is Released MBOM
                    // PCM : TIGTK-9132 : 26/07/2017 : AB : START
                    PSS_enoECMChangeRequest_mxJPO changeRequest = new PSS_enoECMChangeRequest_mxJPO(context, null);
                    boolean bolAllowPushPop = changeRequest.isAllowConnctionOfStandardPartAndChangeRequest(context, domItem, domCR);
                    if (TigerConstants.POLICY_PSS_MBOM.equalsIgnoreCase(strItemPolicy) && TigerConstants.STATE_MBOM_RELEASED.equalsIgnoreCase(strItemState) || bolAllowPushPop) {
                        // PCM : TIGTK-9132 : 26/07/2017 : AB : END
                        try {
                            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                            DomainRelationship.disconnect(context, strRelID);
                        } finally {
                            ContextUtil.popContext(context);
                        }
                    } else {
                        DomainRelationship.disconnect(context, strRelID);
                    }
                }
            }
            // PCM : TIGTK-7054 : 25/04/2017 : AB : END
            ContextUtil.commitTransaction(context);
            // PCM TIGTK-3797 | 23/12/16 :Pooja Mantri : End

        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in disconnectAffectedItemFromCR: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
        }
        return retStatus;
    }

    // RFC-033-AB-IT12

    public boolean noshowParallelTrackFieldForEdit(Context context, String[] args) throws Exception {
        HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
        String strCRId = (String) programMap.get("objectId");
        boolean showParallelTrackField = true;
        String strLoggedInUser = context.getUser();
        DomainObject domChangeRequest = new DomainObject(strCRId);
        String strChangeManager = domChangeRequest.getInfo(context, "from[" + ChangeConstants.RELATIONSHIP_CHANGE_COORDINATOR + "].to.name");
        String strCurent = domChangeRequest.getInfo(context, DomainObject.SELECT_CURRENT);
        if (strLoggedInUser.equalsIgnoreCase(strChangeManager) && TigerConstants.STATE_SUBMIT_CR.equals(strCurent)) {
            showParallelTrackField = false;
        }
        return showParallelTrackField;
    }

    public boolean noshowParallelTrackFieldForEditStateCreate(Context context, String[] args) throws Exception {
        HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);

        String strCRId = (String) programMap.get("objectId");
        String strMode = (String) programMap.get("mode");
        boolean showParallelTrackField = true;
        String strLoggedInUser = context.getUser();
        DomainObject domChangeRequest = new DomainObject(strCRId);
        String strChangeManager = domChangeRequest.getInfo(context, "from[" + ChangeConstants.RELATIONSHIP_CHANGE_COORDINATOR + "].to.name");
        String strCurent = domChangeRequest.getInfo(context, DomainObject.SELECT_CURRENT);
        if (strLoggedInUser.equalsIgnoreCase(strChangeManager) && TigerConstants.STATE_SUBMIT_CR.equals(strCurent)) {
            showParallelTrackField = false;
        }
        if (strMode.equals("edit")) {
            showParallelTrackField = false;
        }
        return showParallelTrackField;
    }

    // Addition for Tiger - PCM stream by SGS ends

    public int updateAttributeValue(Context context, String[] args) throws Exception {
        int result = 0;
        // Added by PCM, Change Corrdinator does not have modify access on CR in Evaluate state-Start
        boolean isContextPushed = false;
        // Added by PCM, Change Corrdinator does not have modify access on CR in Evaluate state-End
        try {
            String strObjectId = args[0];
            String strCurrentState = args[1];
            DomainObject domChangeRequest = DomainObject.newInstance(context, strObjectId);
            if ("Evaluate".equals(strCurrentState)) {
                String strValue = domChangeRequest.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CRREQUESTEDASSESSMENTENDDATE); // ARJUN SEE HERE

                if ((strValue == null || strValue.trim().isEmpty())) {
                    SimpleDateFormat _mxDateFormat = new SimpleDateFormat(eMatrixDateFormat.getEMatrixDateFormat(), Locale.US);
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.SECOND, -1);
                    // Modified for TIGTK-6321 : Priyanka Salunke : 13-04-2017 : START
                    cal.add(Calendar.DATE, 7);
                    // Modified for TIGTK-6321 : Priyanka Salunke : 13-04-2017 : END
                    Date date = cal.getTime();
                    String strNewValue = _mxDateFormat.format(date);
                    // Added by PCM, Change Corrdinator does not have modify access on CR in Evaluate state-Start
                    ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                    isContextPushed = true;
                    // Added by PCM, Change Corrdinator does not have modify access on CR in Evaluate state-End
                    domChangeRequest.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CRREQUESTEDASSESSMENTENDDATE, strNewValue); // ARJUN SEE SHERE
                    strValue = strNewValue;
                }

                StringList objectSelects = new StringList();
                objectSelects.add(DomainConstants.SELECT_ID);

                String strObjWhereClause = "revision==last";
                MapList mlIA = domChangeRequest.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS, TigerConstants.TYPE_PSS_IMPACTANALYSIS, objectSelects, null, false, // getTo
                        // false
                        true, // getFrom true
                        (short) 1, // 1
                        strObjWhereClause, null, 0); // limit 0

                if (mlIA.size() > 0) {
                    Map<?, ?> mapIA = (Map<?, ?>) mlIA.get(0);
                    String strIAId = (String) mapIA.get(DomainConstants.SELECT_ID);
                    DomainObject domIA = DomainObject.newInstance(context, strIAId);

                    StringList relSelects = new StringList();
                    relSelects.add(DomainConstants.SELECT_RELATIONSHIP_ID);

                    MapList mlRA = domIA.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT, TigerConstants.TYPE_PSS_ROLEASSESSMENT, objectSelects, relSelects, false, // getTo
                            // false
                            true, // getFrom true
                            (short) 1, // 1
                            null, null, 0); // limit 0

                    int nRASize = mlRA.size();

                    for (int i = 0; i < nRASize; i++) {
                        Map<?, ?> mapRA = (Map<?, ?>) mlRA.get(i);
                        String strRelId = (String) mapRA.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                        DomainRelationship domRel = new DomainRelationship(strRelId);
                        domRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_DUEDATE, strValue);
                    }
                }
            }
        } catch (RuntimeException ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in updateAttributeValue: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
        // Added by PCM, Change Corrdinator does not have modify access on CR in Evaluate state-Start
        finally {
            if (isContextPushed) {
                ContextUtil.popContext(context);
            }
        }
        // Added by PCM, Change Corrdinator does not have modify access on CR in Evaluate state-End
        return result;
    }

    /**
     * This method is called from "PSS_ECMFullSearchPreProcess.jsp". It exclude the already connected "Sketch" or "Markup" OIDs ---- TS137 -- Attach Reference Documents to CN -- For RFC033.
     * @param context
     * @param args
     * @return -- StringList -- List of OIDs to be excluded in Search
     * @author -- Pooja Mantri
     * @throws Exception
     */
    public StringList excludeConnectSupportingDocOIDs(Context context, String[] args) throws Exception {
        // Findbug Issue correction start
        // Date: 21/03/2017
        // By: Asha G.
        StringList lstExcludeConnectSupportingDocOIDs = null;
        // Findbug Issue correction End
        try {

            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            String strObjectId = (String) programMap.get("objectId");

            // Creating Domain Object Instance for Context Object
            DomainObject domContextObject = DomainObject.newInstance(context, strObjectId);

            // Get Connected Sketch or Markup Objects
            lstExcludeConnectSupportingDocOIDs = domContextObject.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_SUPPORTINGDOCUMENT + "].to.id");

        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in excludeConnectSupportingDocOIDs: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
        return lstExcludeConnectSupportingDocOIDs;
    }

    // RFC-033-AB-IT12
    /**
     * This method is called from Check trigger on Creation of Relationship 'PSS_AffectedItem' for Check whether Part or CAD which is not in Released state that is connected to any other CR or not
     * @param context
     * @param args
     *            - CR Id & Affected Item ID
     * @return
     * @throws Exception
     */

    public int checkAffectedItemConnectedToAnyOtherCR(Context context, String[] args) throws Exception {
        int intReturn = 0;
        try {
            String strAffectedItemId = args[1];
            DomainObject domAffectedItem = new DomainObject(strAffectedItemId);
            String strAffectedItemState = (String) domAffectedItem.getInfo(context, DomainConstants.SELECT_CURRENT);
            String strAffectedItemName = (String) domAffectedItem.getInfo(context, DomainConstants.SELECT_NAME);

            // Check State of Affected Item if it is Released than it can be connected to other CR
            if (strAffectedItemState.equalsIgnoreCase("Released") || strAffectedItemState.equalsIgnoreCase("Release") || strAffectedItemState.equalsIgnoreCase("Create")
                    || strAffectedItemState.equalsIgnoreCase("Peer Review")) {
                intReturn = 0;
            } else {
                // Get Connected CR with affected Item
                String strConnectedCRId = domAffectedItem.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM + "].from.name");
                if (UIUtil.isNotNullAndNotEmpty(strConnectedCRId)) {
                    StringBuffer msg = new StringBuffer();
                    // Findbug Issue correction start
                    // Date: 21/03/2017
                    // By: Asha G.
                    msg.append("Affected Item ");
                    msg.append(strAffectedItemName);
                    msg.append(" is already connected with ");
                    msg.append(strConnectedCRId);
                    msg.append(" which is not Released.");
                    // Findbug Issue correction End
                    MqlUtil.mqlCommand(context, "notice $1", msg.toString());
                    intReturn = 1;
                }
            }

        } catch (Exception e) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkAffectedItemConnectedToAnyOtherCR: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
        return intReturn;
    }

    /**
     * This method is used for Move Affetced Item from One CR to another CR
     * @param context
     * @param args
     *            - Selected Item for Move Affected Item in CR & Old and new CRID & RelId of affected Item
     * @return
     * @throws Exception
     */

    public int moveAffectedItemsToAnotherCR(Context context, String[] args) throws Exception {
        try {
            PSS_enoECMChangeRequest_mxJPO changeRequest = new PSS_enoECMChangeRequest_mxJPO(context, null);
            DomainObject domOldCR = new DomainObject();
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            StringList slAffectedItemList = (StringList) programMap.get("strAffectedItemList");
            StringList slAffectedItemRelList = (StringList) programMap.get("strAffectedItemRelList");
            String[] arrNewCRIdsToBeConnected = (String[]) programMap.get("listnewCRIdsToBeConnected");
            String strOldCRID = (String) programMap.get("strOldCRID");

            if (UIUtil.isNotNullAndNotEmpty(strOldCRID)) {
                domOldCR = DomainObject.newInstance(context, strOldCRID);
            }

            Map<String, String> mRelToRequestedChange = new HashMap<String, String>();
            if (!slAffectedItemRelList.isEmpty()) {

                for (int d = 0; d < slAffectedItemRelList.size(); d++) {
                    // PCM : TIGTK-9132 : 26/07/2017 : AB : START
                    // If current Affected Item is Selected Item than Disconnect Relation between CR And Item
                    String strRelID = (String) slAffectedItemRelList.get(d);
                    String strItemID = (String) slAffectedItemList.get(d);
                    DomainObject domItem = DomainObject.newInstance(context, strItemID);
                    DomainRelationship domainRelationship = new DomainRelationship(strRelID);
                    String requestedChange = domainRelationship.getAttributeValue(context, ChangeConstants.ATTRIBUTE_REQUESTED_CHANGE);
                    mRelToRequestedChange.put(strItemID, requestedChange);

                    boolean bolAllowPushPop = changeRequest.isAllowConnctionOfStandardPartAndChangeRequest(context, domItem, domOldCR);
                    boolean isPushedContext = false;
                    if (bolAllowPushPop) {
                        ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                        isPushedContext = true;
                    }

                    try {
                        DomainRelationship.disconnect(context, strRelID);
                    } finally {
                        if (isPushedContext) {
                            ContextUtil.popContext(context);
                        }
                    }
                }
            }

            // Connect the Affected Items to selected ChangeRequest
            for (int index = 0; index < arrNewCRIdsToBeConnected.length; index++) {
                String sCRID = (String) arrNewCRIdsToBeConnected[index];
                DomainObject domNewCR = new DomainObject(sCRID);
                int nCount = slAffectedItemList.size();
                for (int j = 0; j < nCount; j++) {

                    String strItemID = (String) slAffectedItemList.get(j);
                    DomainObject domItem = DomainObject.newInstance(context, strItemID);
                    boolean bolAllowPushPop = changeRequest.isAllowConnctionOfStandardPartAndChangeRequest(context, domItem, domNewCR);
                    boolean isPushedContext = false;
                    if (bolAllowPushPop) {
                        ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                        isPushedContext = true;
                    }

                    try {
                        DomainRelationship domRel = DomainRelationship.connect(context, domNewCR, TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM, domItem);
                        domRel.setAttributeValue(context, ChangeConstants.ATTRIBUTE_REQUESTED_CHANGE, (String) mRelToRequestedChange.get(strItemID));

                    } finally {
                        if (isPushedContext) {
                            ContextUtil.popContext(context);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in moveAffectedItemsToAnotherCR: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw e;
        }

        return 0;
    }

    /**
     * This method is used to set the Requested Change Values on the Affected Item when moved to new CR. Modified for Perfomance issue TIGTK-3951 : 02/02/2017 : AB
     * @param context
     * @param args
     * @return
     * @throws Exception
     */

    public void processMapForRequestedChange(Context context, Map mRelMap, Map mRelToRequestedChange) throws Exception {
        try {
            if (!mRelMap.isEmpty() && mRelMap.size() > 0) {
                Set<Entry> entrySet = mRelMap.entrySet();
                for (Entry entry : entrySet) {
                    String strObjectID = (String) entry.getKey();
                    String relId = (String) entry.getValue();
                    String strAIReqChange = (String) mRelToRequestedChange.get(strObjectID);
                    if (UIUtil.isNotNullAndNotEmpty(strAIReqChange)) {
                        DomainRelationship domRel = new DomainRelationship(relId);
                        domRel.setAttributeValue(context, ChangeConstants.ATTRIBUTE_REQUESTED_CHANGE, strAIReqChange);
                    }
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in processMapForRequestedChange: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
    }

    // RFC-033-AB-IT12
    /**
     * This method is used for updating the value for attribute "PSS_CRTitle".It is called from CR Creation and Properties Page
     * @param context
     * @param args
     * @author -- Pooja Mantri -- Added for Iteration 11 Issue
     * @return void - Returns Nothing
     * @throws Exception
     */
    public void updateCRTitle(Context context, String[] args) throws Exception {
        try {

            HashMap<?, ?> programMap = JPO.unpackArgs(args);
            HashMap<?, ?> paramMap = (HashMap<?, ?>) programMap.get("paramMap");

            String strCRObjectId = (String) paramMap.get("objectId");
            String strCRTitleValue = (String) paramMap.get("New Value");

            if (UIUtil.isNotNullAndNotEmpty(strCRObjectId)) {
                // Creating DomainObject Instance of "PSS_ChangeRequest" Object
                DomainObject domCRObject = DomainObject.newInstance(context, strCRObjectId);
                // PCM TIGTK - 3762 | 08/12/16 : GC : Start
                if (UIUtil.isNotNullAndNotEmpty(strCRTitleValue)) {
                    domCRObject.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CRTITLE, strCRTitleValue);
                } else {
                    String strCRName = domCRObject.getInfo(context, DomainConstants.SELECT_NAME);
                    domCRObject.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CRTITLE, strCRName);
                }
                // PCM TIGTK - 3762 | 08/12/16 : GC : End
            }

        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in updateCRTitle: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
    }

    /**
     * Description : This method is used for Update Created Date for Change Request
     * @author abhalani
     * @args
     * @Date Jul 14, 2016
     */

    public void updateCreatedOnForChangeRequest(Context context, String[] args) throws Exception {

        HashMap<?, ?> params = (HashMap<?, ?>) JPO.unpackArgs(args);
        HashMap<?, ?> paramMap = (HashMap<?, ?>) params.get("paramMap");
        String strObjectId = (String) paramMap.get("objectId");
        String strCreatedDate = (String) paramMap.get("New Value");
        Date date = new Date();
        strCreatedDate = (strCreatedDate == null) ? date.toString() : strCreatedDate;

        SimpleDateFormat _mxDateFormat = new SimpleDateFormat(eMatrixDateFormat.getEMatrixDateFormat(), Locale.US);
        String strDate = _mxDateFormat.format(new Date(strCreatedDate));

        // Update Creation Date of CR
        DomainObject domCRObj = new DomainObject(strObjectId);
        domCRObj.setAttributeValue(context, "Created On", strDate);
    }

    /**
     * This method is called on "PSS_ChangeRequest" create form. It populates the current Collaborative Space "Program Project" on CR.
     * @author -- Pooja Mantri -- JIRA -- TIG - 237
     * @param context
     * @param args
     * @return String -- Returns Program Project Code
     * @throws Exception
     */

    public String getContextCSProgramProject(Context context, String[] args) throws Exception {

        StringBuffer strBuf = new StringBuffer();
        try {
            MapList mlProgramProjectList;
            StringList objectSelect = new StringList();
            objectSelect.add(DomainObject.SELECT_ID);
            objectSelect.add(DomainObject.SELECT_NAME);
            objectSelect.add(DomainObject.SELECT_DESCRIPTION);
            String strProgProjName = "";
            String strProgProjId = "";

            String strSecurityContext = PersonUtil.getDefaultSecurityContext(context, context.getUser());
            String strCollaborativeSpace = (strSecurityContext.split("[.]")[2]);
            String queryLimit = "0";
            mlProgramProjectList = DomainObject.findObjects(context, TigerConstants.TYPE_PSS_PROGRAMPROJECT, // type keyed in or selected from type chooser
                    "*", "*", "*", TigerConstants.VAULT_ESERVICEPRODUCTION,
                    "project==" + strCollaborativeSpace + " && current!=Active && from[" + TigerConstants.RELATIONSHIP_PSS_SUBPROGRAMPROJECT + "]==False", "", // save to the .finder later
                    false, objectSelect, Short.parseShort(queryLimit), "*", "");
            if (mlProgramProjectList.size() > 0) {
                Map<?, ?> mPrj = (Map<?, ?>) mlProgramProjectList.get(0);
                strProgProjName = (String) mPrj.get(DomainObject.SELECT_NAME);
                strProgProjId = (String) mPrj.get(DomainObject.SELECT_ID);
            }
            strBuf.append("<input type=\"textbox\" id=\"Project Code\" value=\"" + strProgProjName + "\" name=\"Project Code\" readOnly='true' ");
            strBuf.append("/>");
            strBuf.append("<input type=\"hidden\" id=\"Project CodeOID\" value=\"" + strProgProjId + "\" name=\"Project CodeOID\" ");
            strBuf.append("/>");
        }
        // FindBug Exception is caught when Exception is not thrown: TIGTK-6238 | 18/04/2017 |Harika Varanasi :Starts
        catch (RuntimeException re) {
            throw re;
        }
        // FindBug Exception is caught when Exception is not thrown: TIGTK-6238 | 18/04/2017 |Harika Varanasi :Ends
        catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getContextCSProgramProject: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
        return strBuf.toString();
    }

    /**
     * Description : This method is Called from action trigger of promotion of CR from In Review to In Process state Send Mail Notification to Lead change manger of each affected Item
     * @author abhalani
     * @args
     * @Date Jul 22, 2016
     */
    public void sendMailNotificationToLeadchangeMangerOfAffectedItem(Context context, String[] args) throws Exception {
        // TODO Auto-generated method stub
        try {

            String strObjectId = args[0];
            DomainObject domCRID = new DomainObject(strObjectId);

            StringList objSelects = new StringList();
            objSelects.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
            objSelects.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.description");
            objSelects.addElement(DomainConstants.SELECT_NAME);
            objSelects.addElement(DomainConstants.SELECT_DESCRIPTION);
            objSelects.addElement(DomainConstants.SELECT_CURRENT);
            objSelects.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRTITLE + "]");
            objSelects.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRTYPE + "]");
            objSelects.addElement("attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");

            Map<String, Object> mpCRObjInfo = domCRID.getInfo(context, objSelects);

            String strCRProjectId = (String) mpCRObjInfo.get("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
            String strCRProjectDescription = (String) mpCRObjInfo.get("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.description");
            String strCRName = (String) mpCRObjInfo.get(DomainConstants.SELECT_NAME);
            String strCRDescription = (String) mpCRObjInfo.get(DomainConstants.SELECT_DESCRIPTION);
            String strCRState = (String) mpCRObjInfo.get(DomainConstants.SELECT_CURRENT);
            String strCRTitle = (String) mpCRObjInfo.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRTITLE + "]");
            String strCRType = (String) mpCRObjInfo.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRTYPE + "]");
            String strCRInitiator = (String) mpCRObjInfo.get("attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");
            String strCRInitiatorEmail = PersonUtil.getEmail(context, strCRInitiator);

            String strCRLink = DomainConstants.EMPTY_STRING;
            String strCRProjectLink = DomainConstants.EMPTY_STRING;
            String strBaseURL = MailUtil.getBaseURL(context);
            String strBaseURLSubstring = DomainConstants.EMPTY_STRING;
            if (UIUtil.isNotNullAndNotEmpty(strBaseURL)) {
                int position = strBaseURL.lastIndexOf("/");
                strBaseURLSubstring = strBaseURL.substring(0, position);
                strCRLink = strBaseURLSubstring + "/emxTree.jsp?mode=edit&objectId=" + strObjectId;
                strCRProjectLink = strBaseURLSubstring + "/emxTree.jsp?mode=edit&objectId=" + strCRProjectId;
            }

            PSS_enoECMChangeUtil_mxJPO changeUtil = new PSS_enoECMChangeUtil_mxJPO(context, null);

            StringList slAffectedItemSelects = new StringList();
            slAffectedItemSelects.addElement(DomainConstants.SELECT_ID);

            // Get Affected Item of Change request
            MapList mlRelatedCRAffectedItems = domCRID.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM, // relationship pattern
                    "*", // object pattern
                    slAffectedItemSelects, // object selects
                    null, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 1, // recursion level
                    null, // object where clause
                    null, 0);

            StringList toListPerson = new StringList();

            for (int m = 0; m < mlRelatedCRAffectedItems.size(); m++) {
                Map<?, ?> mapCRAffectedItems = (Map<?, ?>) mlRelatedCRAffectedItems.get(m);
                String strCRAffectedItemId = (String) mapCRAffectedItems.get(DomainConstants.SELECT_ID);
                MapList allProjects = (MapList) changeUtil.getProjectListFromEBOM(context, strCRAffectedItemId);

                HashMap<String, MapList> paramMap = new HashMap<String, MapList>();
                paramMap.put("objectList", allProjects);
                paramMap.put("paramList", null);

                Vector<?> vProjectCountList = (Vector<?>) changeUtil.getProjectLeadChangeManager(context, JPO.packArgs(paramMap));
                for (int i = 0; i < vProjectCountList.size(); i++) {
                    String strPerson = (String) vProjectCountList.get(i);
                    String arrPerson[] = strPerson.split("\\,");
                    for (int j = 0; j < arrPerson.length; j++) {
                        toListPerson.add(arrPerson[j]);
                    }
                }
            }

            String strLanguage = context.getSession().getLanguage();
            String strMsgTigerKey = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.Tiger");
            String strMsgForStringKey = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.ForLeadChangeManager");

            // Create Message For Mail Notification
            StringBuffer subjectKey = new StringBuffer();
            subjectKey.append(strMsgTigerKey + " ");
            subjectKey.append(strCRName + " ");
            subjectKey.append(strMsgForStringKey + " ");

            // Create Message Body for the Mail Notification
            StringBuffer strBufferMessage = new StringBuffer();
            // Findbug Issue correction start
            // Date: 21/03/2017
            // By: Asha G.
            strBufferMessage.append("Project : ");
            strBufferMessage.append(strCRProjectLink);
            strBufferMessage.append("\n");
            strBufferMessage.append("Project Description : ");
            strBufferMessage.append(strCRProjectDescription);
            strBufferMessage.append("\n");
            strBufferMessage.append("CR ID : ");
            strBufferMessage.append(strCRLink);
            strBufferMessage.append("\n");
            strBufferMessage.append("CR Description : ");
            strBufferMessage.append(strCRDescription);
            strBufferMessage.append("\n");
            strBufferMessage.append("State of CR : ");
            strBufferMessage.append(strCRState);
            strBufferMessage.append("\n");
            strBufferMessage.append("CR Title : ");
            strBufferMessage.append(strCRTitle);
            strBufferMessage.append("\n");
            strBufferMessage.append("CR Type : ");
            strBufferMessage.append(strCRType);
            strBufferMessage.append("\n");
            strBufferMessage.append("Change Initiator : ");
            strBufferMessage.append(strCRInitiator);
            strBufferMessage.append("\n");
            strBufferMessage.append("Change Initiator e-mail : ");
            strBufferMessage.append(strCRInitiatorEmail);
            strBufferMessage.append("\n");
            // Findbug Issue correction End
            // Send Notification to Lead Change Manager
            MailUtil.sendNotification(context, toListPerson, // toList
                    null, // ccList
                    null, // bccList
                    subjectKey.toString(), // subjectKey
                    null, // subjectKeys
                    null, // subjectValues
                    strBufferMessage.toString(), // messageKey
                    null, // messageKeys
                    null, // messageValues
                    null, // objectIdList
                    null); // companyName
        } catch (Exception e) {

            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in sendMailNotificationToLeadchangeMangerOfAffectedItem: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
    }

    /**
     * Description :This method is used for Connect Route template With change Action on Changes of 'Purpose of Release' on CO
     * @author abhalani
     * @args
     * @Date Jul 25, 2016
     */
    public void connectRouteTemplateToChangeAction(Context context, String[] args) throws Exception {

        try {
            String strObjectId = args[0]; // Object Id OF Change Order
            // String strNewValuePurposeOfRelease = args[1];
            String strPSSRouteTemplateTypeValue = "";
            String strAffectedItemType = "";
            String strAffectedItemPolicy = "";
            String strConnectedRouteTemplateOfCA = "";
            if (UIUtil.isNotNullAndNotEmpty(strObjectId)) {
                final String RANGE_APPROVAL_LIST_FORCOMMERCIALUPDATEONCO = "Approval List for Commercial update on CO";
                final String RANGE_APPROVAL_LIST_FORPROTOTYPEONCO = "Approval List for Prototype on CO";
                final String RANGE_APPROVAL_LIST_FORSERIALLAUNCHONCO = "Approval List for Serial Launch on CO";
                final String RANGE_APPROVAL_LIST_FORDESIGNSTUDYONCO = "Approval List for Design study on CO";
                final String RANGE_APPROVAL_LIST_FOROTHERPARTSONCO = "Approval List for Other Parts on CO";
                final String RANGE_APPROVAL_LIST_FORCADONCO = "Approval List for CAD on CO";
                final String RANGE_APPROVAL_LIST_FORSTANDARDPARTSONCO = "Approval List for Standard Parts on CO";

                // Range Constants for attribute "PSS_Purpose_Of_Release"
                final String RANGE_OTHER = "Other";
                final String RANGE_DESIGN_STUDY = "Design study";
                final String RANGE_SERIAL_TOOL_LAUNCH_MODIFICATION = "Serial Tool Launch/Modification";
                final String RANGE_PROTOTYPE_TOOL_LAUNCH_MODIFICATION = "Prototype Tool Launch/Modification";
                final String RANGE_COMMERCIAL_UPDATE = "Commercial Update";

                Map<Object, Object> programProjectMap = new HashMap<>();
                programProjectMap.put(RANGE_COMMERCIAL_UPDATE, RANGE_APPROVAL_LIST_FORCOMMERCIALUPDATEONCO);
                programProjectMap.put(RANGE_PROTOTYPE_TOOL_LAUNCH_MODIFICATION, RANGE_APPROVAL_LIST_FORPROTOTYPEONCO);
                programProjectMap.put(RANGE_SERIAL_TOOL_LAUNCH_MODIFICATION, RANGE_APPROVAL_LIST_FORSERIALLAUNCHONCO);
                programProjectMap.put(RANGE_DESIGN_STUDY, RANGE_APPROVAL_LIST_FORDESIGNSTUDYONCO);
                programProjectMap.put(RANGE_OTHER, RANGE_APPROVAL_LIST_FOROTHERPARTSONCO);

                DomainObject domObject = new DomainObject(strObjectId);
                String strTypeObject = domObject.getInfo(context, DomainConstants.SELECT_TYPE);
                String strProgramProjectOID = domObject.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");

                if (strTypeObject.equalsIgnoreCase(TigerConstants.TYPE_PSS_CHANGEORDER) && UIUtil.isNotNullAndNotEmpty(strObjectId) && UIUtil.isNotNullAndNotEmpty(strProgramProjectOID)) {
                    String strPurposeOfreleaseOfCO = domObject.getInfo(context, "attribute[" + TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE + "]");

                    // Creating "PSS Program Project" Object Instance
                    DomainObject domProgramProjectObject = DomainObject.newInstance(context, strProgramProjectOID);

                    StringList objectSelects = new StringList(1);
                    objectSelects.addElement(SELECT_ID);
                    objectSelects.addElement(DomainConstants.SELECT_NAME);
                    objectSelects.addElement(DomainConstants.SELECT_TYPE);
                    objectSelects.addElement(DomainConstants.SELECT_POLICY);
                    objectSelects.addElement("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "].to.name");

                    StringList relSelects = new StringList(1);

                    // get Connected Change Action Object of Change Order
                    MapList changeActionList = domObject.getRelatedObjects(context, // context
                            ChangeConstants.RELATIONSHIP_CHANGE_ACTION, // relationship pattern
                            ChangeConstants.TYPE_CHANGE_ACTION, // object pattern
                            objectSelects, // object selects
                            relSelects, // relationship selects
                            false, // to direction
                            true, // from direction
                            (short) 1, // recursion level
                            null, // object where clause
                            null, // relationship where clause
                            (short) 0);

                    for (int i = 0; i < changeActionList.size(); i++) {
                        StringList busSelect = new StringList();
                        Map<?, ?> mCAObj = (Map<?, ?>) changeActionList.get(i);
                        String strCAId = (String) mCAObj.get(DomainConstants.SELECT_ID);
                        DomainObject domCAID = new DomainObject(strCAId);
                        strConnectedRouteTemplateOfCA = (String) mCAObj.get("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "].to.name");

                        // Get Connected Affected Item of CO
                        MapList listCOConnectedToAffectedItem = domCAID.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, DomainConstants.QUERY_WILDCARD, objectSelects,
                                null, false, true, (short) 1, "", "", (short) 0);

                        for (int j = 0; j < listCOConnectedToAffectedItem.size(); j++) {
                            Map<?, ?> mCOAffectedItemObj = (Map<?, ?>) listCOConnectedToAffectedItem.get(j);
                            strAffectedItemType = (String) mCOAffectedItemObj.get(DomainConstants.SELECT_TYPE);
                            strAffectedItemPolicy = (String) mCOAffectedItemObj.get(DomainConstants.SELECT_POLICY);
                        }

                        // Select Route Template Type on Basic of Affected item Type
                        if (strAffectedItemType.equals(TYPE_PART)) {
                            if (strPurposeOfreleaseOfCO.equals(RANGE_DESIGN_STUDY)) {
                                strPSSRouteTemplateTypeValue = (String) programProjectMap.get(RANGE_DESIGN_STUDY);
                            } else if (strPurposeOfreleaseOfCO.equals(RANGE_SERIAL_TOOL_LAUNCH_MODIFICATION)) {
                                strPSSRouteTemplateTypeValue = (String) programProjectMap.get(RANGE_SERIAL_TOOL_LAUNCH_MODIFICATION);
                            } else if (strPurposeOfreleaseOfCO.equals(RANGE_COMMERCIAL_UPDATE)) {
                                strPSSRouteTemplateTypeValue = (String) programProjectMap.get(RANGE_COMMERCIAL_UPDATE);
                            } else if (strPurposeOfreleaseOfCO.equals(RANGE_PROTOTYPE_TOOL_LAUNCH_MODIFICATION)) {
                                strPSSRouteTemplateTypeValue = (String) programProjectMap.get(RANGE_PROTOTYPE_TOOL_LAUNCH_MODIFICATION);
                            } else if (strPurposeOfreleaseOfCO.equals(RANGE_OTHER)) {
                                strPSSRouteTemplateTypeValue = (String) programProjectMap.get(RANGE_OTHER);
                            }
                        } else if (strAffectedItemPolicy.equals(TigerConstants.POLICY_PSS_CADOBJECT)) {
                            busSelect.add("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "|attribute[" + TigerConstants.ATTRIBUTE_PSS_ROUTETEMPLATETYPE + "]=='"
                                    + RANGE_APPROVAL_LIST_FORCADONCO + "'].to.name");
                            busSelect.add("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "|attribute[" + TigerConstants.ATTRIBUTE_PSS_ROUTETEMPLATETYPE + "]=='"
                                    + RANGE_APPROVAL_LIST_FORCADONCO + "'].to.id");
                        } else if (strAffectedItemPolicy.equals(TigerConstants.POLICY_STANDARDPART)) {
                            busSelect.add("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "|attribute[" + TigerConstants.ATTRIBUTE_PSS_ROUTETEMPLATETYPE + "]=='"
                                    + RANGE_APPROVAL_LIST_FORSTANDARDPARTSONCO + "'].to.name");
                            busSelect.add("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "|attribute[" + TigerConstants.ATTRIBUTE_PSS_ROUTETEMPLATETYPE + "]=='"
                                    + RANGE_APPROVAL_LIST_FORSTANDARDPARTSONCO + "'].to.id");
                        }

                        if (strAffectedItemType.equals(TYPE_PART)) {
                            busSelect.add("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "|attribute[" + TigerConstants.ATTRIBUTE_PSS_ROUTETEMPLATETYPE + "]=='"
                                    + strPSSRouteTemplateTypeValue + "'].to.name");
                            busSelect.add("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "|attribute[" + TigerConstants.ATTRIBUTE_PSS_ROUTETEMPLATETYPE + "]=='"
                                    + strPSSRouteTemplateTypeValue + "'].to.id");
                        }

                        // Get Route Template From ProgramProject
                        Map<String, Object> mapRouteTemplateDetails = domProgramProjectObject.getInfo(context, busSelect);
                        String strRouteTemplateFromProgramProject = (String) mapRouteTemplateDetails.get("from[PSS_ConnectedRouteTemplates].to.name");
                        String strRouteTemplateFromProgramProjectID = (String) mapRouteTemplateDetails.get("from[PSS_ConnectedRouteTemplates].to.id");

                        // PCM TIGTK-3116 | 15/09/16 : Ketaki Wagh : Start
                        ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                        if (UIUtil.isNotNullAndNotEmpty(strRouteTemplateFromProgramProjectID)) {
                            if (UIUtil.isNullOrEmpty(strConnectedRouteTemplateOfCA)) {
                                domCAID.addToObject(context, new RelationshipType(TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES), strRouteTemplateFromProgramProjectID);
                                ContextUtil.popContext(context);
                                // PCM TIGTK-3116 | 15/09/16 : Ketaki Wagh : End
                            } else if (!strConnectedRouteTemplateOfCA.equalsIgnoreCase(strRouteTemplateFromProgramProject)) {
                                String strOldRelID = (String) domCAID.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "].id");
                                DomainRelationship.disconnect(context, strOldRelID);
                                domCAID.addToObject(context, new RelationshipType(TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES), strRouteTemplateFromProgramProjectID);
                            }
                        }

                    }
                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in connectRouteTemplateToChangeAction: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
    }

    /**
     * Description : This method is used for get the Route Template name Connected with Change Action
     * @author abhalani
     * @args
     * @Date Jul 26, 2016
     */
    public Vector<String> getApprovalListForChangeAction(Context context, String[] args) throws Exception {
        // Create result vector
        Vector<String> vecResult = new Vector<String>();
        Map<?, ?> mapObjectInfo = null;
        String sApprovalListName = "";

        // Get object list information from packed arguments
        HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
        MapList objectList = (MapList) programMap.get("objectList");
        for (Iterator<?> itrObjects = objectList.iterator(); itrObjects.hasNext();) {
            mapObjectInfo = (Map<?, ?>) itrObjects.next();
            String strCAId = (String) mapObjectInfo.get("id");
            DomainObject domCAID = new DomainObject(strCAId);

            // Get Connected Route template which is connected with Change Action
            sApprovalListName = (String) domCAID.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "].to.name");
            vecResult.add(sApprovalListName);
        }
        return vecResult;
    }

    /**
     * Owner: Ketaki Wagh This method is used to check required route template is available or not on PSS_ProgramProject.
     * @param context
     * @param args
     *            -- args0 -- "PSS_ProgramProject" Object Id -- args1 -- Change Object Id
     * @return
     * @throws Exception
     */
    public int checkForRouteTemplate(Context context, String args[]) throws Exception {
        String strMessage = DomainConstants.EMPTY_STRING;
        ;
        int nResult = 1;
        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }
        try {

            String str_PSS_Purpose_Of_Release = DomainConstants.EMPTY_STRING;

            String strPSS_ProgramProjectId = args[0];
            String strChangeObjectId = args[1];

            // Domain Object of Change
            DomainObject domChangeObject = DomainObject.newInstance(context, strChangeObjectId);

            String strChangeType = domChangeObject.getInfo(context, DomainConstants.SELECT_TYPE);

            // TS-184 Create and Edit Issue From Global Actions -- TIGTK -6309 : 02/08/2017 : KWagh
            if (!TigerConstants.TYPE_PSS_ISSUE.equalsIgnoreCase(strChangeType)) {
                // TS-184 Create and Edit Issue From Global Actions -- TIGTK -6309 : 02/08/2017 : KWagh
                if (strChangeType.equalsIgnoreCase(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER)) {
                    str_PSS_Purpose_Of_Release = domChangeObject.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE);
                } else if (strChangeType.equalsIgnoreCase(TigerConstants.TYPE_PSS_CHANGEORDER)) {

                    str_PSS_Purpose_Of_Release = domChangeObject.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE);
                }

                // Domain Object of Program-Project
                DomainObject domProgramProjectObject = DomainObject.newInstance(context, strPSS_ProgramProjectId);
                String strProgramProjectName = domProgramProjectObject.getName(context);

                String strPSS_ROUTETEMPLATETYPEAttributeValue = "attribute[" + TigerConstants.ATTRIBUTE_PSS_ROUTETEMPLATETYPE + "].value";

                StringList ObjectSelect = new StringList();
                ObjectSelect.add(DomainConstants.SELECT_NAME);
                ObjectSelect.add(DomainConstants.SELECT_ID);

                StringList relSelects = new StringList();
                relSelects.add(strPSS_ROUTETEMPLATETYPEAttributeValue);

                // Get value from Map
                // TIGTK-11675 : 27/11/17 : TS : START
                String strRouteTemplateName = (String) getRouteTemplateTypeValueForChange(str_PSS_Purpose_Of_Release);
                // TIGTK-11675 : 27/11/17 : TS : END
                if (strChangeType.equalsIgnoreCase(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER)) {
                    strRouteTemplateName = strRouteTemplateName + " on MCO";

                } else if (strChangeType.equalsIgnoreCase(TigerConstants.TYPE_PSS_CHANGEORDER)) {
                    strRouteTemplateName = strRouteTemplateName + " on CO";
                }

                StringList RouteTemplateTypeList = new StringList();

                MapList mlRelatedRoutes = domProgramProjectObject.getRelatedObjects(context, // context
                        TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES, // relationship pattern
                        "*", // object pattern
                        ObjectSelect, // object selects
                        relSelects, // relationship selects
                        false, // to direction
                        true, // from direction
                        (short) 1, // recursion level
                        null, // object where clause
                        null, // relationship where clause
                        (short) 0);
                int nCnt = mlRelatedRoutes.size();

                for (int n = 0; n < nCnt; n++) {
                    Map<?, ?> mRouteObj = (Map<?, ?>) mlRelatedRoutes.get(n);
                    // Get value of attribute Route template type
                    String strPSS_ROUTETEMPLATETYPE = (String) mRouteObj.get(strPSS_ROUTETEMPLATETYPEAttributeValue);

                    if (UIUtil.isNotNullAndNotEmpty(strPSS_ROUTETEMPLATETYPE)) {
                        RouteTemplateTypeList.add(strPSS_ROUTETEMPLATETYPE);
                    }

                }

                if (RouteTemplateTypeList.isEmpty()) {
                    // No route is connected to Project
                    // Added for JIRA TIGTK-2155
                    strMessage = "Program-Project '" + strProgramProjectName + "' has no Route Template Connected to it";
                    // Added for JIRA TIGTK-2155
                } else {

                    if (strChangeType.equalsIgnoreCase(TigerConstants.TYPE_PSS_CHANGEREQUEST)) {
                        if ((RouteTemplateTypeList.contains("Default Evaluation Review Route Template for CR"))) {
                            nResult = 0;
                        } else {
                            strMessage = "Program-Project '" + strProgramProjectName
                                    + "' selected for current object creation does not have either/both 'Default Evaluation Review Route Template for CR' ";
                        }

                    } else if (strChangeType.equalsIgnoreCase(TigerConstants.TYPE_PSS_CHANGENOTICE)) {
                        if ((RouteTemplateTypeList.contains("Default CN Reviewer on CN"))) {
                            nResult = 0;
                        } else {
                            strMessage = "Program-Project '" + strProgramProjectName + "' selected for current object creation does not have 'Default CN Reviewer on CN' connected";
                        }
                    } else {
                        int nCount = RouteTemplateTypeList.size();
                        for (int i = 0; i < nCount; i++) {

                            String strRouteType = (String) RouteTemplateTypeList.get(i);

                            if (strRouteType.equalsIgnoreCase(strRouteTemplateName)) {
                                nResult = 0;
                            } else {
                                strMessage = "Program-Project '" + strProgramProjectName + "'selected for current object creation does not have Route template attached whose purpose is to '"
                                        + str_PSS_Purpose_Of_Release + "'.";
                            }
                        }
                    }
                }
                // TS-184 Create and Edit Issue From Global Actions -- TIGTK -6309 : 02/08/2017 : KWagh
            } else {
                nResult = 0;
            }
            // TS-184 Create and Edit Issue From Global Actions -- TIGTK -6309 : 02/08/2017 : KWagh
        } // FindBug Exception is caught when Exception is not thrown: TIGTK-6238 | 18/04/2017 |Harika Varanasi :Starts
        catch (RuntimeException re) {
            throw re;
        }
        // FindBug Exception is caught when Exception is not thrown: TIGTK-6238 | 18/04/2017 |Harika Varanasi :Ends
        catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkForRouteTemplate: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
        if (nResult == 1) {
            MqlUtil.mqlCommand(context, "notice $1", strMessage);
        }
        return nResult;
    }

    /**
     * @author kWagh
     * @param context
     * @param args
     * @return
     * @throws Exception
     *             Method to list Program Project Members
     */
    public StringList getContextProgramProjectMembers(Context context, String[] args) throws Exception {
        // StringList slProgramProjectMembers = new StringList();
        StringList slPersonList = new StringList();
        try {
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            String strChangeObjectId = (String) programMap.get("objectId");
            // PCM : TIGTK-8442 : PSE : 08-06-2017 : START
            // Change Object domain object
            DomainObject domChangeObject = DomainObject.newInstance(context, strChangeObjectId);
            StringList slSelectables = new StringList();
            slSelectables.add("to[" + TigerConstants.RELATIONSHIP_PSS_RELATEDCN + "]." + DomainConstants.SELECT_FROM_ID);
            slSelectables.add(DomainConstants.SELECT_TYPE);
            slSelectables.add(DomainConstants.SELECT_CURRENT);
            Map<String, Object> mpCNInfoList = domChangeObject.getInfo(context, slSelectables);
            String strMCOId = (String) mpCNInfoList.get("to[" + TigerConstants.RELATIONSHIP_PSS_RELATEDCN + "]." + DomainConstants.SELECT_FROM_ID);
            String strChangeType = (String) mpCNInfoList.get(DomainConstants.SELECT_TYPE);
            String strChangeState = (String) mpCNInfoList.get(DomainConstants.SELECT_CURRENT);
            // If chnage object type is change notice then get program project memebrs from connected Mfg Change Order Object
            if (strChangeType.equalsIgnoreCase(TigerConstants.TYPE_PSS_CHANGENOTICE)) {
                domChangeObject.setId(strMCOId);
            }
            // PCM : TIGTK-8442 : PSE : 08-06-2017 : END

            // Get related Program-Project Object
            // Get all members of Program-Project
            // get the Allowed Roles for Transfer Ownership from Properties file.
            String strSelectProjectTeamMembers = DomainConstants.EMPTY_STRING;
            StringList slProgramProjectMembers;
            String strAllowedRoles = DomainConstants.EMPTY_STRING;
            if (TigerConstants.TYPE_PSS_CHANGEORDER.equalsIgnoreCase(strChangeType)) {
                strAllowedRoles = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                        "PSS_EnterpriseChangeMgt.Label.COTransferOwnershipAllowedRoles");
            } else if (TigerConstants.TYPE_PSS_CHANGEREQUEST.equalsIgnoreCase(strChangeType)) {
                strAllowedRoles = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                        "PSS_EnterpriseChangeMgt.Label.CRTransferOwnershipAllowedRoles");
            }
            if (TigerConstants.TYPE_PSS_CHANGEORDER.equalsIgnoreCase(strChangeType) || TigerConstants.TYPE_PSS_CHANGEREQUEST.equalsIgnoreCase(strChangeType)) {
                StringList slAllowedRolesForTransferOwnership = FrameworkUtil.split(strAllowedRoles, ",");

                // Get the Program-Project member who have role assigned from given list in where clause.
                // TIGTK-17803:Start
                String strRolesWhere = DomainConstants.EMPTY_STRING;
                if (!(TigerConstants.TYPE_PSS_CHANGEREQUEST.equalsIgnoreCase(strChangeType) && ChangeConstants.STATE_CHANGEREQUEST_CREATE.equalsIgnoreCase(strChangeState))) {
                    strRolesWhere = new StringBuilder("|.(attribute[").append(TigerConstants.ATTRIBUTE_PSS_ROLE).append("] matchlist \"")
                            .append(FrameworkUtil.join(slAllowedRolesForTransferOwnership, ",")).append("\" \",\"").append(")").toString();
                }
                // TIGTK-17803:End
                strSelectProjectTeamMembers = new StringBuilder("to[").append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA).append("].from.from[")
                        .append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS).append(strRolesWhere).append("].to.id").toString();

                String strMapSelectProjectTeamMembers = new StringBuilder("to[").append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA).append("].from.from[")
                        .append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS).append("].to.id").toString();
                DomainObject.MULTI_VALUE_LIST.add(strMapSelectProjectTeamMembers);

                StringList slSelects = new StringList(strSelectProjectTeamMembers);
                Map objectMap = domChangeObject.getInfo(context, slSelects);

                PSS_enoECMChangeUtil_mxJPO changeOrder = new PSS_enoECMChangeUtil_mxJPO(context, null);
                ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                slProgramProjectMembers = changeOrder.getStringListFromMap(context, objectMap, strMapSelectProjectTeamMembers);
                ContextUtil.popContext(context);
            } else {
                strSelectProjectTeamMembers = new StringBuilder("to[").append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA).append("].from.from[")
                        .append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS).append("].to.id").toString();
                ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                slProgramProjectMembers = domChangeObject.getInfoList(context, strSelectProjectTeamMembers);
                ContextUtil.popContext(context);
            }

            // Modified By KWagh - TIGTK-2901 - Start

            // PCM : TIGTK-8442 : PSE : 08-06-2017 : START
            if (!slProgramProjectMembers.isEmpty()) {
                // PCM : TIGTK-8442 : PSE : 08-06-2017 : END
                int nCount = slProgramProjectMembers.size();
                for (int i = 0; i < nCount; i++) {
                    String strPersonID = (String) slProgramProjectMembers.get(i);
                    DomainObject domPersonObject = new DomainObject(strPersonID);
                    String strPersonCurrent = (String) domPersonObject.getInfo(context, DomainConstants.SELECT_CURRENT);
                    if (strPersonCurrent.equalsIgnoreCase("Active")) {
                        slPersonList.add(strPersonID);
                    }
                }
                // PCM : TIGTK-8442 : PSE : 08-06-2017 : START
            } else {
                slPersonList.add(DomainConstants.EMPTY_STRING);
            }
            // PCM : TIGTK-8442 : PSE : 08-06-2017 : END
        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getContextProgramProjectMembers: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
        return slPersonList;
        // Modified By KWagh - TIGTK-2901 - End
    }

    /**
     * Description : This method is called for Change Owner action of 'Change Request' Modified For TIGTK-3707
     * @author abhalani
     * @args
     * @Date Aug 10, 2016
     */
    public int notifyOwnerForTransferOwnership(Context context, String args[]) throws Exception {
        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }
        String sSubmitChange = PropertyUtil.getGlobalRPEValue(context, "SubmitChange");

        try {
            // Skip Change Owner Notification in case of 'SendToChangeManger' case.
            if ("YES".equals(sSubmitChange)) {
                PropertyUtil.setGlobalRPEValue(context, "SubmitChange", "NO");
                return 0;
            }
            String strObjectId = args[0]; // Object Id of Change Request
            String strCRLink = "";
            String strProgramProjectLink = "";
            String strCRName = "";
            String strCRDescription = "";
            String strCRReasonForChange = "";
            String strCRPurposeOfRelease = "";
            String strCRTargetChangeImplementationDate = "";
            StringList objectSelect = new StringList();
            StringList objectIdList = new StringList();
            StringBuffer stbSubject = new StringBuffer();
            StringBuffer stbMessage = new StringBuffer();

            String strLanguage = context.getSession().getLanguage();
            String strMsgTigerKey = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.Tiger");
            String strMsgForStringKey = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.For");
            String strMsgNewChangeRequest = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.NewChangeRequest");
            String strMsghasBeenAssignedFor = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.hasBeenAssignedFor");
            String strMsgCRDescriptionStringKey = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.CRDescription");
            String strMsgCRReasonForChangeStringKey = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.ReasonForChange");
            String strMsgCRPurposeOfReleaseStringKey = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.PurposeOfRelease");
            String strMsgCRTargetChangeImplDateStringKey = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage),
                    "emxFramework.Message.TargetChangeImplDate");

            // Get CR connected Program Project Information
            DomainObject domCR = newInstance(context, strObjectId);
            objectSelect.add(DomainConstants.SELECT_ID);
            objectSelect.addElement(DomainConstants.SELECT_NAME);
            objectSelect.addElement(DomainConstants.SELECT_DESCRIPTION);
            objectSelect.add(DomainConstants.SELECT_OWNER);
            objectSelect.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
            objectSelect.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.name");
            objectSelect.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRREASONFORCHANGE + "]");
            objectSelect.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE + "]");
            objectSelect.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRTARGETCHANGEIMPLEMENTATIONDATE + "]");

            @SuppressWarnings("unchecked")
            Map<String, Object> mpCRObjectInfo = (Map<String, Object>) domCR.getInfo(context, objectSelect);
            String strCROwner = (String) domCR.getInfo(context, DomainConstants.SELECT_OWNER);
            String strProgramProjectId = (String) mpCRObjectInfo.get("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
            String strProgramProjectName = (String) mpCRObjectInfo.get("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.name");
            strCRName = (String) mpCRObjectInfo.get(DomainConstants.SELECT_NAME);
            strCRDescription = (String) mpCRObjectInfo.get(DomainConstants.SELECT_DESCRIPTION);
            strCRReasonForChange = (String) mpCRObjectInfo.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRREASONFORCHANGE + "]");
            strCRPurposeOfRelease = (String) mpCRObjectInfo.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE + "]");
            strCRTargetChangeImplementationDate = (String) mpCRObjectInfo.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRTARGETCHANGEIMPLEMENTATIONDATE + "]");

            // Do not send a message if the current owner is person_UserAgent
            if (ChangeConstants.USER_AGENT.equalsIgnoreCase(strCROwner))
                return 0;

            // Create Subject of the Mail Notification
            stbSubject.append(strMsgTigerKey + " ");
            stbSubject.append(strMsgNewChangeRequest + " ");
            stbSubject.append(strCRName + " ");
            stbSubject.append(strMsghasBeenAssignedFor + " ");
            stbSubject.append(strMsgForStringKey + " ");
            stbSubject.append(strProgramProjectName + " ");

            // Create Link for Change Request & Program Project
            String strBaseURLSubstring = "";
            String strBaseURL = MailUtil.getBaseURL(context);

            if (UIUtil.isNotNullAndNotEmpty(strBaseURL)) {
                int position = strBaseURL.lastIndexOf("/");
                strBaseURLSubstring = strBaseURL.substring(0, position);
                strCRLink = strBaseURLSubstring + "/emxTree.jsp?mode=edit&objectId=" + strObjectId;
                strProgramProjectLink = strBaseURLSubstring + "/emxTree.jsp?mode=edit&objectId=" + strProgramProjectId;
            }

            // Create Message Body of the Mail Notification
            // Findbug Issue correction start
            // Date: 21/03/2017
            // By: Asha G.
            stbMessage.append("Change Request  ");
            stbMessage.append(strCRLink);
            stbMessage.append("\n");
            stbMessage.append("Program Project  ");
            stbMessage.append(strProgramProjectLink);
            stbMessage.append("\n");
            stbMessage.append(strMsgCRDescriptionStringKey);
            stbMessage.append(" ");
            stbMessage.append(strCRDescription);
            stbMessage.append("\n");
            stbMessage.append(strMsgCRReasonForChangeStringKey);
            stbMessage.append(" ");
            stbMessage.append(strCRReasonForChange);
            stbMessage.append("\n");
            stbMessage.append(strMsgCRPurposeOfReleaseStringKey);
            stbMessage.append(" ");
            stbMessage.append(strCRPurposeOfRelease);
            stbMessage.append("\n");
            stbMessage.append(strMsgCRTargetChangeImplDateStringKey);
            stbMessage.append(" ");
            stbMessage.append(strCRTargetChangeImplementationDate);
            stbMessage.append("\n");

            // Findbug Issue correction End

            // Send Mail Notification
            MailUtil.sendNotification(context, new StringList(strCROwner), // toList
                    null, // ccList
                    null, // bccList
                    stbSubject.toString(), // subjectKey
                    null, // subjectKeys
                    null, // subjectValues
                    stbMessage.toString(), // messageKey
                    null, // messageKeys
                    null, // messageValues
                    objectIdList, // objectIdList
                    null); // companyName

        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in notifyOwnerForTransferOwnership: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
        return 0;
    }

    /**
     * Method to Move Affected Items from current CR to a new CR
     * @param context
     * @param args
     * @throws Exception
     */
    public void addAffectedItemToChangeRequest(Context context, String[] args) throws Exception {
        try {
            PSS_enoECMChangeRequest_mxJPO changeRequest = new PSS_enoECMChangeRequest_mxJPO(context, null);
            DomainObject domNewCR = new DomainObject();
            DomainObject domOldCR = new DomainObject();
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            HashMap<?, ?> paramMap = (HashMap<?, ?>) programMap.get("paramMap");
            HashMap<?, ?> requestMap = (HashMap<?, ?>) programMap.get("requestMap");

            StringList slAffectedItems = (StringList) FrameworkUtil.split((String) requestMap.get("affectedItemList"), ",");
            StringList slRelationshipIds = (StringList) FrameworkUtil.split((String) requestMap.get("relationshipIds"), ",");

            String strNewCRID = (String) paramMap.get("newObjectId");
            String strOldCRID = (String) requestMap.get("objectId");
            if (UIUtil.isNotNullAndNotEmpty(strNewCRID)) {
                domNewCR = DomainObject.newInstance(context, strNewCRID);
            }

            if (UIUtil.isNotNullAndNotEmpty(strOldCRID)) {
                domOldCR = DomainObject.newInstance(context, strOldCRID);
            }

            Map<String, String> mRelToRequestedChange = new HashMap<String, String>();

            int nRelCount = slRelationshipIds.size();
            for (int cnt = 0; cnt < nRelCount; cnt++) {
                String strRelID = (String) slRelationshipIds.get(cnt);
                String strItemID = (String) slAffectedItems.get(cnt);
                DomainObject domItem = DomainObject.newInstance(context, strItemID);
                DomainRelationship domainRelationship = new DomainRelationship(strRelID);
                String requestedChange = domainRelationship.getAttributeValue(context, ChangeConstants.ATTRIBUTE_REQUESTED_CHANGE);
                mRelToRequestedChange.put(strItemID, requestedChange);

                // PCM : TIGTK-9132 : 26/07/2017 : AB : START
                boolean bolAllowPushPop = changeRequest.isAllowConnctionOfStandardPartAndChangeRequest(context, domItem, domOldCR);
                boolean isPushedContext = false;
                if (bolAllowPushPop) {
                    ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                    isPushedContext = true;
                }

                try {
                    DomainRelationship.disconnect(context, strRelID);
                } finally {
                    if (isPushedContext) {
                        ContextUtil.popContext(context);
                    }
                }
            }

            int nCount = slAffectedItems.size();
            if (nCount > 0) {
                for (int j = 0; j < nCount; j++) {

                    String strItemID = (String) slAffectedItems.get(j);
                    DomainObject domItem = DomainObject.newInstance(context, strItemID);
                    boolean bolAllowPushPop = changeRequest.isAllowConnctionOfStandardPartAndChangeRequest(context, domItem, domNewCR);
                    boolean isPushedContext = false;
                    if (bolAllowPushPop) {
                        ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                        isPushedContext = true;
                    }

                    try {
                        DomainRelationship domRel = DomainRelationship.connect(context, domNewCR, TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM, domItem);
                        domRel.setAttributeValue(context, ChangeConstants.ATTRIBUTE_REQUESTED_CHANGE, (String) mRelToRequestedChange.get(strItemID));

                    } finally {
                        if (isPushedContext) {
                            ContextUtil.popContext(context);
                        }
                    }
                }
                // PCM : TIGTK-9132 : 26/07/2017 : AB : END
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in addAffectedItemToChangeRequest: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
    }

    // Kwagh - TIGTK-2770 - Start
    /**
     * This method is called to send mail to new Assignee.
     * @author Kwagh
     * @param context
     * @param args
     * @throws Exception
     */
    public void sendNotificationToCAAssignee(Context context, String args[]) throws Exception {
        try {
            // PCM TIGTK-3897: 10/01/2017 : KWagh : START
            String strAssigneeID = args[0];
            String strChangeOID = args[1];
            Map<String, Object> mInfo;
            // PCM TIGTK-3985 | 27/01/17 :Harika Varanasi : Start
            // PCM TIGTK-10768 | 15/11/17 : TS : Start
            StringList slBasics = new StringList();
            slBasics.add(DomainConstants.SELECT_NAME);
            slBasics.add(DomainConstants.SELECT_CURRENT);
            slBasics.add(DomainConstants.SELECT_TYPE);
            slBasics.add("from[" + ChangeConstants.RELATIONSHIP_TECHNICAL_ASSIGNEE + "].to.name");

            // Get information of related Change Action Object
            DomainObject domChangeObject = DomainObject.newInstance(context, strChangeOID);
            mInfo = domChangeObject.getInfo(context, slBasics);
            String strChangeName = (String) mInfo.get(DomainConstants.SELECT_NAME);
            String strChangeState = (String) mInfo.get(DomainConstants.SELECT_CURRENT);
            String strTechnicalAssignee = (String) mInfo.get("from[" + ChangeConstants.RELATIONSHIP_TECHNICAL_ASSIGNEE + "].to.name");

            String strChangeObjType = (String) mInfo.get(DomainConstants.SELECT_TYPE);

            // PCM : TIGTK-9180 : 17/08/2017 : AB : START
            if (!(ChangeConstants.STATE_CHANGE_ACTION_PENDING.equalsIgnoreCase(strChangeState) || TigerConstants.STATE_PSS_MCA_PREPARE.equalsIgnoreCase(strChangeState))) {

                // PCM : TIGTK-9180 : 17/08/2017 : AB : END
                Map payload = new HashMap();
                payload.put("ChangeObjName", strChangeName);
                payload.put("toList", strTechnicalAssignee);
                payload.put("ChangeObjType", strChangeObjType);

                if (TigerConstants.TYPE_CHANGEACTION.equalsIgnoreCase(strChangeObjType)) {
                    emxNotificationUtil_mxJPO.objectNotification(context, strChangeOID, "PSS_CAAssigneeReassignNotification", payload);
                } else if (TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION.equalsIgnoreCase(strChangeObjType)) {
                    emxNotificationUtil_mxJPO.objectNotification(context, strChangeOID, "PSS_MCAAssigneeReassignNotification", payload);
                }
                // PCM TIGTK-10768 | 15/11/17 : TS : End
            }
        } catch (Exception e) {

            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in sendNotificationToCAAssignee: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
    }

    // Kwagh - TIGTK-2770 - End
    // Modification for SLC starts
    public void setDecisionDateOnCR(Context context, String[] args) throws Exception {

        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }

        try {
            String strCRId = args[0];
            DomainObject domChangeRequestObject = DomainObject.newInstance(context, strCRId);
            String strSystemDate = getSystemDate(context, args);
            domChangeRequestObject.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_DECISION_DATE, strSystemDate);
        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in setDecisionDateOnCR: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
    }

    // Modification for SLC ends

    // TIGTK-3187 : Rutuja Ekatpure :22/09/2016:start
    /**
     * This method is on include OID program on Project code field
     * @author RutujaE
     * @param context
     * @param args
     * @throws Exception
     */
    public StringList getProgramProjRelatedToPlant(Context context, String[] args) throws Exception {
        // PCM TIGTK-4461: 16/02/2017 : KWagh : START
        HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
        String strPlantId = (String) programMap.get("objectId");
        DomainObject domPlantObject = DomainObject.newInstance(context, strPlantId);
        // PCM TIGTK-6942 | 4/28/17 : PTE : START
        StringList slProgramProjectList = this.getPromotedChildProgProForChange(context, args);
        // PCM TIGTK-6942 | 4/28/17 : PTE : ENDs
        StringList slFinalProjectList = new StringList();
        StringList slStates = new StringList();
        slStates.add(TigerConstants.STATE_ACTIVE);
        slStates.add(TigerConstants.STATE_OBSOLETE);
        slStates.add(TigerConstants.STATE_NONAWARDED);

        // get connected program project
        StringList slProgProjIds = domPlantObject.getInfoList(context, "to[" + TigerConstants.RELATIONSHIP_PSS_PRODUCTIONENTITY + "].from.id");
        int nProgramCnt = slProgProjIds.size();
        for (int cnt = 0; cnt < nProgramCnt; cnt++) {
            String strProgramID = (String) slProgProjIds.get(cnt);
            DomainObject domProject = new DomainObject(strProgramID);
            String strCurrent = domProject.getInfo(context, DomainConstants.SELECT_CURRENT);
            if (!slStates.contains(strCurrent)) {
                // PCM TIGTK-6942 | 4/28/17 : PTE : START
                if (slProgramProjectList.contains(strProgramID)) {
                    slFinalProjectList.add(strProgramID);
                }
                // PCM TIGTK-6942 | 4/28/17 : PTE : Ends
            }
        }
        return slFinalProjectList;
    }

    // PCM TIGTK-4461: 16/02/2017 : KWagh : End
    // TIGTK-3187 : Rutuja Ekatpure :22/09/2016:End

    // TIGTK-3351 -- KWagh - Start
    public String displayImpactAnalysisTaskStatus(Context context, String[] args) throws Exception {

        Map<?, ?> programMap = (Map<?, ?>) JPO.unpackArgs(args);

        String strRouteID = DomainConstants.EMPTY_STRING;
        String strattrRouteStatus = DomainConstants.EMPTY_STRING;
        String strTaskID = DomainConstants.EMPTY_STRING;

        String strCurrent = DomainConstants.EMPTY_STRING;
        String strAttrApprovalStatus = DomainConstants.EMPTY_STRING;
        String strDisplayImage = DomainConstants.EMPTY_STRING;
        Map<?, ?> requestMap = (Map<?, ?>) programMap.get("requestMap");

        String strCROID = (String) requestMap.get("objectId");
        DomainObject domObjCR = new DomainObject(strCROID);

        String relpattern = DomainConstants.RELATIONSHIP_OBJECT_ROUTE;
        String typePattern = PropertyUtil.getSchemaProperty(context, "type_Route");

        StringList slObjectSle = new StringList(1);
        slObjectSle.addElement(DomainConstants.SELECT_ID);
        slObjectSle.addElement(DomainConstants.SELECT_NAME);
        slObjectSle.addElement(DomainConstants.SELECT_CURRENT);

        StringList slRelSle = new StringList(1);
        slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

        // Get connected Route Objects

        MapList mlConnectedRoutes = domObjCR.getRelatedObjects(context, relpattern, typePattern, slObjectSle, slRelSle, false, true, (short) 1, null, null, 0);
        if (mlConnectedRoutes.size() > 0) {
            for (int i = 0; i < mlConnectedRoutes.size(); i++) {
                Map<?, ?> mRouteObj = (Map<?, ?>) mlConnectedRoutes.get(i);
                strRouteID = (String) mRouteObj.get(DomainConstants.SELECT_ID);
                DomainObject objRoute = new DomainObject(strRouteID);
                strattrRouteStatus = objRoute.getAttributeValue(context, DomainConstants.ATTRIBUTE_ROUTE_STATUS);

                String sRouteBaseState = objRoute.getInfo(context,
                        "to[" + DomainConstants.RELATIONSHIP_OBJECT_ROUTE + "].attribute[" + PropertyUtil.getSchemaProperty(context, "attribute_RouteBaseState") + "]");

                // Check Attributes "Route Base State" and "Route Status"
                if (("state_Evaluate".equals(sRouteBaseState))) {

                    if ((strattrRouteStatus.equals("Stopped"))) {

                        String srelpattern = DomainConstants.RELATIONSHIP_ROUTE_TASK;
                        String stypePattern = PropertyUtil.getSchemaProperty(context, "type_InboxTask");

                        // Connected Inbox Task Objects
                        MapList mConnectedTasks = objRoute.getRelatedObjects(context, srelpattern, stypePattern, slObjectSle, slRelSle, true, false, (short) 1, null, null, 0);

                        if (mConnectedTasks.size() > 0) {
                            for (int j = 0; j < mConnectedTasks.size(); j++) {
                                Map<?, ?> mTaskObj = (Map<?, ?>) mConnectedTasks.get(j);
                                strTaskID = (String) mTaskObj.get(DomainConstants.SELECT_ID);
                                DomainObject domObjTask = new DomainObject(strTaskID);
                                strAttrApprovalStatus = domObjTask.getAttributeValue(context, "Approval Status");
                                strCurrent = (String) mTaskObj.get(DomainConstants.SELECT_CURRENT);

                                // Check Attribute " Approval Status" and Tasks current state
                                // Task is rejected then block promotion
                                if (("Complete".equals(strCurrent)) && (strAttrApprovalStatus.equals("Reject"))) {
                                    strDisplayImage = "<img src=\"../common/images/buttonDialogCancel.gif\" border=\"0\"/>";
                                }

                            }
                        }

                    } else {

                        strDisplayImage = "<img src=\"../common/images/buttonDialogDone.gif\" border=\"0\"/>";
                    }

                }
            }

        }
        return strDisplayImage;
    }

    // TIGTK-3351 -- KWagh - End

    /**
     * Description : This method is used for get person in Change assignee of Task related to Change Object TIGTK-3370
     * @author abhalani
     * @args
     * @Date Oct 5, 2016
     */
    public StringList getTaskAssigneeInclusionIDs(Context context, String[] args) throws Exception {
        StringList slProgramProjectMembers = new StringList();
        try {
            String strProgProject;
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            // get the CHange Object ID
            String strChangeObjectId = (String) programMap.get("objectId");
            DomainObject domChangeObject = DomainObject.newInstance(context, strChangeObjectId);

            // Check the type of Chane Object and get connected program project from Change Object
            String strChangeType = (String) domChangeObject.getInfo(context, DomainConstants.SELECT_TYPE);

            if (strChangeType.equalsIgnoreCase(ChangeConstants.TYPE_CHANGE_ACTION)) {
                String strRelatedCOID = domChangeObject.getInfo(context, "to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from.id");
                DomainObject domCO = DomainObject.newInstance(context, strRelatedCOID);
                strProgProject = domCO.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
            } else if (strChangeType.equalsIgnoreCase(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION)) {
                String strRelatedMCOID = domChangeObject.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].from.id");
                DomainObject domMCO = DomainObject.newInstance(context, strRelatedMCOID);
                strProgProject = domMCO.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
            } else {
                strProgProject = domChangeObject.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
            }

            // get connected program project team Members from Change Object
            DomainObject domProgramProject = new DomainObject(strProgProject);
            slProgramProjectMembers = domProgramProject.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS + "].to.id");

            //TIGTK-12983 :mkakade : START
            pss.jv.common.PSS_JVUtil_mxJPO objJVUtil = new pss.jv.common.PSS_JVUtil_mxJPO();
            StringList slFaureciaUserList = objJVUtil.getFauresiaUsersConnectedToJV(context, strProgProject);
            if(null != slFaureciaUserList && slProgramProjectMembers!=null)
            {
            	// Add New user to existing list and remove duplicate
            	slProgramProjectMembers.addAll(slFaureciaUserList);
            	Set<String> setUserList = new LinkedHashSet<>(); 
            	setUserList.addAll(slProgramProjectMembers);
            	slProgramProjectMembers.clear();
            	slProgramProjectMembers.addAll(setUserList);
            }
            //TIGTK-12983 :mkakade : END
        } catch (Exception e) {

            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getTaskAssigneeInclusionIDs: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
        }
        return slProgramProjectMembers;
    }

    // PCM TIGTK-3281 | 07/10/16 : Pooja Mantri : Start
    /**
     * This method is on called to check if "Change Request" object is connected to Inactive "Route Template".
     * @author Pooja Mantri
     * @param context
     * @param args
     *            -- args0 -- Change Object Id
     * @return -- int -- Status of check Trigger
     * @throws Exception
     */
    public int checkForActiveRouteTemplate(Context context, String[] args) throws Exception {
        int intRetStatus = 0;
        try {
            if (args == null || args.length < 1) {
                throw (new IllegalArgumentException());
            }
            String strObjectId = args[0];
            DomainObject domChangeObject = DomainObject.newInstance(context, strObjectId);
            StringList slConnectedProgramProjectList = (StringList) domChangeObject.getInfoList(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
            String strProgramProjectId = (String) slConnectedProgramProjectList.get(0);
            DomainObject domProgramProject = DomainObject.newInstance(context, strProgramProjectId);
            String strPSS_ROUTETEMPLATETYPEAttributeValue = "attribute[" + TigerConstants.ATTRIBUTE_PSS_ROUTETEMPLATETYPE + "].value";

            StringList ObjectSelect = new StringList();
            ObjectSelect.add(DomainConstants.SELECT_NAME);
            ObjectSelect.add(DomainConstants.SELECT_ID);
            ObjectSelect.add(DomainConstants.SELECT_CURRENT);

            StringList relSelects = new StringList();
            relSelects.add(strPSS_ROUTETEMPLATETYPEAttributeValue);

            MapList mlRelatedRoutes = domProgramProject.getRelatedObjects(context, // context
                    TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES, // relationship
                    // pattern
                    "*", // object pattern
                    ObjectSelect, // object selects
                    relSelects, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 1, // recursion level
                    null, // object where clause
                    null, // relationship where clause
                    (short) 0);
            int nCnt = mlRelatedRoutes.size();
            StringList RouteTemplateCurrentStateList = new StringList();
            StringList RouteTemplateIdList = new StringList();
            for (int n = 0; n < nCnt; n++) {
                Map<?, ?> mRouteObj = (Map<?, ?>) mlRelatedRoutes.get(n);
                // Get value of attribute Route template type
                String strPSS_ROUTETEMPLATETYPE = (String) mRouteObj.get(strPSS_ROUTETEMPLATETYPEAttributeValue);
                String strRouteTemplateId = (String) mRouteObj.get(DomainConstants.SELECT_ID);
                String strRouteTemplateCurrentState = (String) mRouteObj.get(DomainConstants.SELECT_CURRENT);
                if (UIUtil.isNotNullAndNotEmpty(strPSS_ROUTETEMPLATETYPE)) {
                    if (strPSS_ROUTETEMPLATETYPE.equalsIgnoreCase(TigerConstants.DEFAULT_EVALUATION_REVIEW_ROUTE_TEMPLATE_FOR_CR)
                            || strPSS_ROUTETEMPLATETYPE.equalsIgnoreCase(TigerConstants.DEFAULT_IMPACT_ANALYSIS_ROUTE_TEMPLATE_FOR_CR)) {
                        RouteTemplateIdList.add(strRouteTemplateId);
                        RouteTemplateCurrentStateList.add(strRouteTemplateCurrentState);
                    }

                }
            }

            if (RouteTemplateCurrentStateList.contains("Inactive")) {
                intRetStatus = 1;
            }

            if (intRetStatus == 1) {
                emxContextUtilBase_mxJPO.mqlNotice(context, "The Program Project has Inactive Route Template associated to it.\n Please modify the Program Project for Active Route Template");
            }
        } // Fix for FindBugs issue RuntimeException capture: Suchit Gangurde: 28 Feb 2017: START
        catch (RuntimeException e) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkForActiveRouteTemplate: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw e;
        } // Fix for FindBugs issue RuntimeException capture: Suchit Gangurde: 28 Feb 2017: END
        catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkForActiveRouteTemplate: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - Endx
            throw ex;
        }
        return intRetStatus;
    }

    // PCM TIGTK-3281 | 07/10/16 : Pooja Mantri : End

    // PCM TIGTK-3678 : 28/11/2016 : KWagh : Start
    /**
     * This method is called when the Change Request is promoted from Submit to Evaluate state to check value of Change Implementation Date Modified by PCM : TIGTK-4148 : 02/02/2017 : AB
     * @author kWagh
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public int checkForCRTargetChangeImplementationDate(Context context, String args[]) throws Exception {
        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }
        String strMessage = "";
        int retValue = 0;
        try {
            String objectId = args[0];
            DomainObject domCR = new DomainObject(objectId);

            StringList slSelactables = new StringList();
            slSelactables.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRTARGETCHANGEIMPLEMENTATIONDATE + "]");
            slSelactables.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRTYPE + "]");

            // Get the CR's attribute(CRType and TargetChangeImplementationDate)
            Map<String, Object> mapCRInfo = (Map<String, Object>) domCR.getInfo(context, slSelactables);

            String strCRTargetChangeImplementationDate = (String) mapCRInfo.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRTARGETCHANGEIMPLEMENTATIONDATE + "]");

            // If the CRType is ProgramCR and TargetChangeImplementationDate is blank than successfully promorte CR to Evaluate state
            // TIGTK-10678 - 25-10-2017 - TS - START
            if (UIUtil.isNullOrEmpty(strCRTargetChangeImplementationDate)) {
                // TIGTK-10678 - 25-10-2017 - TS - END
                strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSSEnterpriseChangeMgt.Alert.CRTargetChangeImplementationDate");
                MqlUtil.mqlCommand(context, "notice $1", strMessage);
                retValue = 1;
            }

        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkForCRTargetChangeImplementationDate: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }

        return retValue;
    }

    // PCM TIGTK-3678 : 28/11/2016 : KWagh : End
    // TIGTK-3660 :reject Change Request :Rutuja Ekatpure :25/11/2016:start
    // TIGTK-3660 :Reject Change Request :modified by:KWagh :21/12/2016:start
    /***
     * check Connected Change Oredr state for visibility of of Change Request
     * @param context
     * @param args
     * @throws Exception
     */

    public boolean checkRejectCRVisibility(Context context, String args[]) throws Exception {
        boolean flag = false;

        HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);

        String strObjID = (String) programMap.get("objectId");
        // create domain object for current CR
        DomainObject domCRObj = new DomainObject(strObjID);

        StringList slCRInfo = new StringList(3);
        slCRInfo.add(DomainConstants.SELECT_CURRENT);
        slCRInfo.add(DomainConstants.SELECT_OWNER);
        // take information about CR
        Map<String, Object> mCRinfo = domCRObj.getInfo(context, slCRInfo);
        String strCRCurrent = (String) mCRinfo.get(DomainConstants.SELECT_CURRENT);

        // take connected COO state
        StringList slConnectedCOCurrent = domCRObj.getInfoList(context, "from[" + ChangeConstants.RELATIONSHIP_CHANGE_ORDER + "].to.current");
        // TIGTK-6888:Phase-2.0:START
        String strUserName = context.getUser();
        String strLoggedUserSecurityContext = PersonUtil.getDefaultSecurityContext(context, strUserName);

        if (UIUtil.isNotNullAndNotEmpty(strLoggedUserSecurityContext)) {
            String strLoggerUserRole = (strLoggedUserSecurityContext.split("[.]")[0]);

            StringList slRole = new StringList();
            slRole.add(TigerConstants.ROLE_PSS_PROGRAM_MANAGER);
            slRole.add(TigerConstants.ROLE_PSS_CHANGE_COORDINATOR);
            pss.ecm.ui.CommonUtil_mxJPO objCommoUtil = new pss.ecm.ui.CommonUtil_mxJPO(context, args);
            StringList strProjectMember = objCommoUtil.getProgramProjectTeamMembersForChange(context, strObjID, slRole, false);

            // check state of CR if submit then show command
            if (TigerConstants.STATE_SUBMIT_CR.equals(strCRCurrent) && (strProjectMember.contains(strUserName) && slRole.contains(strLoggerUserRole))) {
                flag = true;
            }
            // check state of CR if In Process or Evaluate then check Connected CO state
            else if ((TigerConstants.STATE_PSS_CR_INPROCESS.equals(strCRCurrent) || TigerConstants.STATE_EVALUATE.equals(strCRCurrent) || TigerConstants.STATE_INREVIEW_CR.equals(strCRCurrent))
                    && (strProjectMember.contains(strUserName) && slRole.contains(strLoggerUserRole))) {
                if (!slConnectedCOCurrent.isEmpty() && slConnectedCOCurrent.size() > 0 && !slConnectedCOCurrent.contains(null)) {
                    for (int i = 0; i < slConnectedCOCurrent.size(); i++) {
                        // if connected CO state is other than Cancelled then hide command
                        if (!TigerConstants.STATE_PSS_CHANGEORDER_CANCELLED.equals(slConnectedCOCurrent.get(i))) {
                            flag = false;
                            break;
                        } else {
                            flag = true;
                        }
                    }
                } else {
                    flag = true;
                }

            }
        }
        return flag;
    }

    // TIGTK-6888:Phase-2.0:END
    // TIGTK-3660 :reject Change Request :Rutuja Ekatpure :25/11/2016:End
    // TIGTK-3660 :Reject Change Request :modified by:KWagh :21/12/2016:End
    // PCM TIGTK-3718 : 2/12/2016 : KWagh : Start
    /**
     * This method is called when the Change Request is promoted from create to Submit state to check For AffectedItems
     * @author kWagh
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public int checkForAffectedItems(Context context, String args[]) throws Exception {
        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }
        String strMessage = "";
        int retValue = 0;
        try {

            String objectId = args[0];
            DomainObject objCR = new DomainObject(objectId);

            String strSelectable = "from[" + TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM + "].to.policy";

            StringList slObjectSle = new StringList(1);
            slObjectSle.addElement(DomainConstants.SELECT_ID);

            String strAttrCRType = objCR.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CRTYPE);

            if (TigerConstants.ENGINEERING_CR.equalsIgnoreCase(strAttrCRType)) {

                StringList slCRAIPolicy = objCR.getInfoList(context, strSelectable);
                // PCM : TIGTK-4044 & TIGTK-4045 : 27/01/2017 : AB : START
                if (!slCRAIPolicy.isEmpty() && slCRAIPolicy.size() > 0) {

                    if (slCRAIPolicy.contains(TigerConstants.POLICY_PSS_ECPART) || slCRAIPolicy.contains(TigerConstants.POLICY_PSS_CADOBJECT)
                            || slCRAIPolicy.contains(TigerConstants.POLICY_PSS_Legacy_CAD) || slCRAIPolicy.contains(TigerConstants.POLICY_STANDARDPART)
                            || slCRAIPolicy.contains(TigerConstants.POLICY_PSS_DEVELOPMENTPART)) {

                        retValue = 0;
                    } else {
                        retValue = 1;
                    }

                } else {

                    retValue = 1;
                }

            } else if (TigerConstants.MANUFACTURING_CR.equalsIgnoreCase(strAttrCRType)) {
                StringList slCRAIPolicy = objCR.getInfoList(context, strSelectable);
                if (!slCRAIPolicy.contains(TigerConstants.POLICY_PSS_MBOM)) {
                    retValue = 1;
                }
            }
            // PCM : TIGTK-4044 & TIGTK-4045 : 27/01/2017 : AB : END
            if (retValue == 1) {
                strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSSEnterpriseChangeMgt.Alert.CRCreatePromoteMessage");
                MqlUtil.mqlCommand(context, "notice $1", strMessage);
            }

        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkForAffectedItems: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }

        return retValue;
    }

    // PCM TIGTK-3718 : 2/12/2016 : KWagh : End
    /**
     * This method is used for Change collobrative space Date: 12/12/2016
     * @author abhalani
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public void changeCollobrativeSpaceOfChangeObject(Context context, String args[]) throws Exception {
        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }

        try {
            // Get the Id of Change Object
            String objectId = args[0];
            DomainObject domChangeObject = new DomainObject(objectId);
            String strObjectProject = (String) domChangeObject.getInfo(context, "project");
            // TIGTK-14064 : 19-04-2018 : START
            String strContextUser = context.getUser();
            if (strContextUser.equalsIgnoreCase(TigerConstants.PERSON_USER_AGENT)) {
                strContextUser = PropertyUtil.getGlobalRPEValue(context, ContextUtil.MX_LOGGED_IN_USER_NAME);
            }
            // Get the Collabrativespace of Logedin User
            String strSecurityContext = PersonUtil.getDefaultSecurityContext(context, strContextUser);
            // TIGTK-14064 : 19-04-2018 : END
            String strCollaborativeSpace = (strSecurityContext.split("[.]")[2]);
            // Change Collabrativespace if it was not same with courrent LogedIn user's Collabrativespace
            if (UIUtil.isNotNullAndNotEmpty(strObjectProject) && UIUtil.isNotNullAndNotEmpty(strCollaborativeSpace)) {
                if (!strObjectProject.equalsIgnoreCase(strCollaborativeSpace)) {

                    // PCM TIGTK-6573| 18/04/17 :KWagh : Start
                    String strMQLQuery = "mod bus " + objectId + " project \"" + strCollaborativeSpace + "\"";
                    // PCM TIGTK-6573| 18/04/17 :KWagh : End
                    MqlUtil.mqlCommand(context, true, strMQLQuery, true);
                }
            }

        } catch (Exception e) {
            logger.error("Error in changeCollobrativeSpaceOfChangeObject: ", e);
            throw e;
        }
    }

    /**
     * This method is used for Change Perallel track's value acordingly CR Type of ChangeRequest Date: 29/12/2016 TIGTK-3853
     * @author abhalani
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public void checkParallelTrackOfChangeRequest(Context context, String args[]) throws Exception {

        try {
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            HashMap<?, ?> requestMap = (HashMap<?, ?>) programMap.get("requestMap");

            // Get the objectId of Change Request
            String strObjectID = (String) requestMap.get("objectId");
            DomainObject domCR = new DomainObject(strObjectID);

            StringList slSelectable = new StringList();
            slSelectable.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRTYPE + "]");
            slSelectable.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_PARALLELTRACK + "]");
            // Check the value of Parallel Track
            Map<String, Object> mapCRAttributes = (Map<String, Object>) domCR.getInfo(context, slSelectable);
            String strCRType = (String) mapCRAttributes.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRTYPE + "]");
            String strCRParallelTrack = (String) mapCRAttributes.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_PARALLELTRACK + "]");

            // If CR Type is "Program CR" or "Manufacturing CR" then change value of Parallel Track to "No"
            if ("Yes".equalsIgnoreCase(strCRParallelTrack) && !"Engineering CR".equalsIgnoreCase(strCRType)) {
                domCR.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PARALLELTRACK, "No");
            }
        } catch (Exception e) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkParallelTrackOfChangeRequest: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw e;
        }

    }

    /**
     * Description : This method is used set the Due date for Task of ImpactRoute on CR Created for PCM : TIGTK-4248 : 21/02/2017 : AB
     * @author abhalani
     * @throws Exception
     * @args
     * @Date February 21, 2017
     */

    public void setDueDateOnCRRoute(Context context, String strRouteID, String strCRTargetChangeImplementationDate) throws Exception {
        try {
            String strRouteId = strRouteID;
            DomainObject domRoute = new DomainObject(strRouteId);

            // Get the ObjectID of Inbox Task from Route
            StringList slInboxTask = domRoute.getInfoList(context, "to[" + DomainConstants.RELATIONSHIP_ROUTE_TASK + "].from.id");

            if (slInboxTask != null && !slInboxTask.isEmpty()) {
                for (int i = 0; i < slInboxTask.size(); i++) {

                    String strInboxtask = (String) slInboxTask.get(i);
                    DomainObject domInboxTask = new DomainObject(strInboxtask);

                    // setting attribute (scheduled completion date) values on the Inbox Task
                    if (UIUtil.isNotNullAndNotEmpty(strCRTargetChangeImplementationDate)) {
                        domInboxTask.setAttributeValue(context, DomainObject.ATTRIBUTE_SCHEDULED_COMPLETION_DATE, strCRTargetChangeImplementationDate);
                    } else {
                        domInboxTask.setAttributeValue(context, DomainObject.ATTRIBUTE_SCHEDULED_COMPLETION_DATE, this.getRouteDueDate(context, null));
                    }

                }
            }

        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in setDueDateOnCRRoute: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
    }

    /**
     * This Method is used to get connected related Change Request from a Change request TIGTK 4879
     * @param context
     * @param args
     * @return MapList
     * @throws Exception
     */
    public MapList getRelatedCRs(Context context, String[] args) throws Exception {
        // Findbug Issue correction start
        // Date: 21/03/2017
        // By: Asha G.
        MapList mlCRList = null;
        // Findbug Issue correction End
        try {

            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);

            String strChangeRequestObjectId = (String) programMap.get("objectId");
            DomainObject domChangeRequest = new DomainObject(strChangeRequestObjectId);

            StringList slObjectSelect = new StringList(1);
            slObjectSelect.addElement(DomainConstants.SELECT_ID);

            StringList slRelSelect = new StringList(1);
            slRelSelect.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

            mlCRList = domChangeRequest.getRelatedObjects(context, // context
                    TigerConstants.RELATIONSHIP_PSS_RELATED_CR, // Relationship Pattern
                    TigerConstants.TYPE_PSS_CHANGEREQUEST, // Type Pattern
                    slObjectSelect, // Object Select
                    slRelSelect, // Relationship Select
                    true, // To Side
                    true, // from Side
                    (short) 1, // Recursion Level
                    "", // Object Where clause
                    "", // Relationship Where clause
                    0, // Limit
                    null, // Post Relationship Patten
                    null, // Post Type Pattern
                    null); // Post Patterns

        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getRelatedCRs: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
        return mlCRList;
    }

    /**
     * This Method is used to connect the related Change Request to a context Change request TIGTK 4879
     * @param context
     * @param args
     * @return MapList
     * @throws Exception
     */
    public Map<?, ?> connectSelectedObjects(Context context, String[] args) throws Exception {
        Map<?, ?> mapConnectionResult = new HashMap<Object, Object>();
        try {

            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);

            String strContextObjectID = (String) programMap.get("objectId");
            String strRelationshipName = (String) programMap.get("relationshipName");
            StringList slSelectedObjectIDs = (StringList) programMap.get("selectedObjects");

            if (UIUtil.isNotNullAndNotEmpty(strContextObjectID) && UIUtil.isNotNullAndNotEmpty(strRelationshipName) && slSelectedObjectIDs != null && !slSelectedObjectIDs.isEmpty()) {
                DomainObject domContextObject = DomainObject.newInstance(context, strContextObjectID);

                // TIGTK-9626 : START
                StringList slObjectSelects = new StringList(3);
                slObjectSelects.add(DomainConstants.SELECT_TYPE);
                slObjectSelects.add(DomainConstants.SELECT_NAME);
                slObjectSelects.add(DomainConstants.SELECT_REVISION);

                Map mFromSideCRInfo = domContextObject.getInfo(context, slObjectSelects);

                StringBuffer sbFromCRInfo = new StringBuffer(3);
                sbFromCRInfo.append((String) mFromSideCRInfo.get(DomainConstants.SELECT_TYPE));
                sbFromCRInfo.append(" ");
                sbFromCRInfo.append((String) mFromSideCRInfo.get(DomainConstants.SELECT_NAME));
                sbFromCRInfo.append(" ");
                sbFromCRInfo.append((String) mFromSideCRInfo.get(DomainConstants.SELECT_REVISION));

                for (int i = 0; i < slSelectedObjectIDs.size(); i++) {
                    String strToObjectId = (String) slSelectedObjectIDs.get(i);
                    DomainObject domSelectedObject = DomainObject.newInstance(context, strToObjectId);

                    boolean bAllowPushContext = isPushPopContextRequired(context, strContextObjectID, strToObjectId);

                    if (bAllowPushContext) {
                        MqlUtil.mqlCommand(context, "history off", true, false);
                        boolean bIsConextPushed = false;
                        ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                        bIsConextPushed = true;
                        DomainRelationship.connect(context, domContextObject, strRelationshipName, domSelectedObject);
                        if (bIsConextPushed) {
                            ContextUtil.popContext(context);
                        }
                        MqlUtil.mqlCommand(context, "history on", true, false);

                        String strMqlHistory = "modify bus $1 add history $2 comment $3";

                        Map mToCRInfo = domSelectedObject.getInfo(context, slObjectSelects);
                        StringBuffer sbToCRInfo = new StringBuffer(3);
                        sbToCRInfo.append((String) mToCRInfo.get(DomainConstants.SELECT_TYPE));
                        sbToCRInfo.append(" ");
                        sbToCRInfo.append((String) mToCRInfo.get(DomainConstants.SELECT_NAME));
                        sbToCRInfo.append(" ");
                        sbToCRInfo.append((String) mToCRInfo.get(DomainConstants.SELECT_REVISION));

                        // Update from side object history
                        MqlUtil.mqlCommand(context, strMqlHistory, strContextObjectID, "connect", strRelationshipName + "  to " + sbToCRInfo.toString());

                        // Update to side object history
                        MqlUtil.mqlCommand(context, strMqlHistory, strToObjectId, "connect", strRelationshipName + "  from " + sbFromCRInfo.toString());
                    } else {
                        DomainRelationship.connect(context, domContextObject, strRelationshipName, domSelectedObject);
                    }
                }
                // TIGTK-9626 : END
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in connectSelectedObjects: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
        return mapConnectionResult;
    }

    /**
     * This Method is used to get list of the Change requests to be excluded for the connection TIGTK 4879
     * @param context
     * @param args
     * @return MapList
     * @throws Exception
     */
    public StringList excludeConnectedCR(Context context, String args[]) throws Exception {

        StringList excludeList = new StringList();
        try {
            HashMap<String, String> paramMap = (HashMap<String, String>) JPO.unpackArgs(args);

            String strChangeRequestId = (String) paramMap.get(ChangeConstants.OBJECT_ID);
            excludeList.add(strChangeRequestId);

            DomainObject domChangeRequest = newInstance(context, strChangeRequestId);
            StringList objectSle = new StringList(DomainConstants.SELECT_ID);

            // TIGTK-8491 - 18-06-2017 - AM - START
            MapList mList = domChangeRequest.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_RELATED_CR, TigerConstants.TYPE_PSS_CHANGEREQUEST, objectSle, null, true, true, (short) 1, null,
                    null, (short) 0);
            // TIGTK-8491 - 18-06-2017 - AM - END
            for (int i = 0; i < mList.size(); i++) {
                Map<String, String> tempMap = (Map<String, String>) mList.get(i);
                excludeList.add(tempMap.get(DomainConstants.SELECT_ID));
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in excludeConnectedCR: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }

        return excludeList;

    }

    // TIGTK-5403 | Harika Varanasi |22/03/2017 : Start
    /**
     * checkBeforeRemovingPartCRs
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    public Map<String, Object> checkBeforeRemovingPartCRs(Context context, String args[]) throws Exception {
        Map<String, Object> resMap = new HashMap<String, Object>();
        StringList resList = new StringList();
        StringBuffer sbNonRemoveCRs = new StringBuffer();
        try {
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            StringList slAffectedItemList = (StringList) programMap.get("relIds");
            StringList objSelects = new StringList();
            objSelects.addElement(DomainConstants.SELECT_NAME);
            objSelects.addElement(DomainConstants.SELECT_CURRENT);
            Map<String, Object> objMap = null;
            for (int i = 0; i < slAffectedItemList.size(); i++) {
                String strRelId = (String) slAffectedItemList.get(i);
                MQLCommand mqlCommand = new MQLCommand();
                boolean mqlResult = mqlCommand.executeCommand(context, "print connection $1 select $2", strRelId, "from.id");
                String strCRId = null;
                if (mqlResult) {
                    strCRId = mqlCommand.getResult();
                    if (UIUtil.isNotNullAndNotEmpty(strCRId) && strCRId.endsWith("\n")) {
                        strCRId = strCRId.substring(0, (strCRId.lastIndexOf("\n")));
                        strCRId = strCRId.substring((strCRId.lastIndexOf("=") + 2), strCRId.length());
                    }
                }

                if (UIUtil.isNotNullAndNotEmpty(strCRId)) {

                    DomainObject domCR = DomainObject.newInstance(context, strCRId);
                    objMap = domCR.getInfo(context, objSelects);
                    String StrState = (String) objMap.get(DomainConstants.SELECT_CURRENT);

                    if (UIUtil.isNotNullAndNotEmpty(StrState) && ("Create".equalsIgnoreCase(StrState) || "Submit".equalsIgnoreCase(StrState))) {
                        resList.addElement(strRelId);
                    } else {
                        String StrName = (String) objMap.get(DomainConstants.SELECT_NAME);
                        if (sbNonRemoveCRs.toString().length() > 0) {
                            sbNonRemoveCRs.append(" , ");
                        }
                        sbNonRemoveCRs.append(StrName);
                    }
                }

            }
            resMap.put("removableCRs", resList);
            if (sbNonRemoveCRs.toString().length() > 0) {
                resMap.put("nonRemovableCRs", MessageUtil.getMessage(context, null, "EnterpriseChangeMg.Message.NonRemovableCRs", new String[] { "name" }, new String[] { sbNonRemoveCRs.toString() },
                        null, context.getLocale(), "emxEnterpriseChangeMgtStringResource"));
            } else {
                resMap.put("nonRemovableCRs", "");
            }

        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkBeforeRemovingPartCRs: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }

        return resMap;

    }
    // TIGTK-5403 | Harika Varanasi |22/03/2017 : End

    /**
     * This method updates the Interchangeability status on CR.
     * @param context
     * @param args
     * @throws Exception
     * @author Suchit Gangurde
     */
    public void updateInterchangeabilityStatusOnCR(Context context, DomainObject domCR, Map<?, ?> programMap) throws Exception {
        HashMap<?, ?> formMap = (HashMap<?, ?>) programMap.get("formMap");
        MapList fields = (MapList) formMap.get("fields");
        for (Object mfieldMap : fields) {
            Map<?, ?> mpField = (Map<?, ?>) mfieldMap;
            if ("Interchangeability Status".equals(mpField.get("name"))) {
                StringList slVal = (StringList) mpField.get("field_value");
                if (slVal != null && slVal.size() > 0 && slVal.get(0).toString().startsWith("<img")) {
                    domCR.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_INTERCHANGEABILITYPROPAGATIONSTATUS, "Complete");
                }
            }
        }
    }

    // PCM TIGTK-4978 | 23/03/17 :Pooja Mantri : Start
    /**
     * This Method is used as Access Function on Create and Add Existing MCO Command on CR and CO
     * @param context
     * @param args
     * @return Boolean -- > The output whether command to be visible or no
     * @throws Exception
     */
    public boolean showCreateAndAddExistingMCOOnCRCO(Context context, String args[]) throws Exception {
        boolean showMCOCmd = false;
        try {
            HashMap<?, ?> programMap = JPO.unpackArgs(args);

            String strObjectId = (String) programMap.get("objectId");
            DomainObject domChangeObject = DomainObject.newInstance(context, strObjectId);

            StringList slSelectStmts = new StringList();
            slSelectStmts.add(DomainConstants.SELECT_CURRENT);
            slSelectStmts.add(DomainConstants.SELECT_TYPE);

            Map<String, Object> mapChangeObjectDetails = domChangeObject.getInfo(context, slSelectStmts);
            String strChangeObjectType = (String) mapChangeObjectDetails.get(DomainConstants.SELECT_TYPE);
            String strChangeCurrentState = (String) mapChangeObjectDetails.get(DomainConstants.SELECT_CURRENT);

            if (TigerConstants.TYPE_PSS_CHANGEREQUEST.equals(strChangeObjectType)) {
                if (TigerConstants.STATE_COMPLETE_CR.equals(strChangeCurrentState) || TigerConstants.STATE_REJECTED_CR.equals(strChangeCurrentState)) {
                    showMCOCmd = false;
                } else {
                    showMCOCmd = true;
                }
            } else if (TigerConstants.TYPE_PSS_CHANGEORDER.equals(strChangeObjectType)) {
                if (TigerConstants.STATE_PSS_CHANGEORDER_INWORK.equals(strChangeCurrentState) || TigerConstants.STATE_PSS_CHANGEORDER_INAPPROVAL.equals(strChangeCurrentState)
                        || TigerConstants.STATE_PSS_CHANGEORDER_COMPLETE.equals(strChangeCurrentState)) {
                    showMCOCmd = true;
                }

            }

        } catch (Exception ex) {

            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in showCreateAndAddExistingMCOOnCRCO: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
        }
        return showMCOCmd;
    }

    // PCM TIGTK-4978 | 23/03/17 :Pooja Mantri : End
    // PCM TIGTK-5821 : 27/03/2017 : Harika Varanasi - Starts
    /**
     * @author Harika varanasi
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public int checkForCREvaluateMandatoryAttributes(Context context, String args[]) throws Exception {
        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }
        String strMessage = "";
        int retValue = 0;
        try {
            String objectId = args[0];

            if (UIUtil.isNotNullAndNotEmpty(objectId)) {
                StringList objectSelects = new StringList();

                objectSelects.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRCUSTOMERAGREEMENTDATE + "]");
                objectSelects.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRREQUESTEDASSESSMENTENDDATE + "]");

                DomainObject domCRObj = DomainObject.newInstance(context, objectId);
                Map<String, Object> objCRMap = domCRObj.getInfo(context, objectSelects);

                String strCRRequestAssesDate = (String) objCRMap.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRREQUESTEDASSESSMENTENDDATE + "]");
                String strCustAgreementDate = (String) objCRMap.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRCUSTOMERAGREEMENTDATE + "]");

                StringBuffer sbMandatoryAttrs = new StringBuffer();
                if (UIUtil.isNullOrEmpty(strCRRequestAssesDate)) {
                    sbMandatoryAttrs.append("Requested Assessment End Date");
                }

                if (UIUtil.isNullOrEmpty(strCustAgreementDate)) {
                    if (sbMandatoryAttrs.toString().length() > 0) {
                        sbMandatoryAttrs.append(" , ");
                    }
                    sbMandatoryAttrs.append("Customer Agreement Date");
                }

                if (sbMandatoryAttrs.toString().length() > 0) {
                    strMessage = MessageUtil.getMessage(context, null, "PSSEnterpriseChangeMgt.Alert.CREvaluateMandatoryAttrCheck", new String[] { "attributes" },
                            new String[] { sbMandatoryAttrs.toString() }, null, context.getLocale(), "emxEnterpriseChangeMgtStringResource");
                    MqlUtil.mqlCommand(context, "notice $1", strMessage);
                    retValue = 1;
                }

            }
        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkForCREvaluateMandatoryAttributes: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }

        return retValue;
    }
    // PCM TIGTK-5821 : 27/03/2017 : Harika Varanasi - Ends

    // PCM TIGTK-5920 | 29/03/17 :Pooja Mantri : Start
    /**
     * Description : This method is used to check the value of Parallel Track for CR, if all Dev Parts are connected as Affected Item to CR
     * @author Pooja Mantri
     * @args
     * @return - Integer
     */

    public int checkDevPartsConnectedToCR(Context context, String[] args) throws Exception {

        int intReturn = 0;
        try {
            String strCRObjectID = args[0];
            // Domain Object instance for CR
            DomainObject domCRObject = new DomainObject(strCRObjectID);
            // PCM TIGTK-6330 | 4/12/17 :PTE: Starts
            String strNewAttrValue = args[1];
            if (UIUtil.isNotNullAndNotEmpty(strNewAttrValue) && strNewAttrValue.equalsIgnoreCase("Yes")) {
                StringList slCRAffectedItemsPolicy = domCRObject.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM + "].to.policy");
                // PCM : TIGTK-8595 04/07/2017 : AB : START
                if (!slCRAffectedItemsPolicy.isEmpty()) {
                    if (!((slCRAffectedItemsPolicy.contains(TigerConstants.POLICY_PSS_ECPART)) || slCRAffectedItemsPolicy.contains(TigerConstants.POLICY_STANDARDPART)
                            || slCRAffectedItemsPolicy.contains(TigerConstants.POLICY_PSS_CADOBJECT) || slCRAffectedItemsPolicy.contains(TigerConstants.POLICY_PSS_Legacy_CAD))) {
                        // PCM : TIGTK-8595 04/07/2017 : AB : END
                        // PCM TIGTK-6330 | 4/12/17 :PTE: Ends
                        String strLanguage = context.getSession().getLanguage();
                        String strErrorMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", new Locale(strLanguage),
                                "PSS_EnterpriseChangeMgt.Alert.CRParallelTrackModifyAlert");
                        MqlUtil.mqlCommand(context, "notice $1", strErrorMessage);
                        intReturn = 1;
                    }
                }
            }

        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkDevPartsConnectedToCR: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }

        return intReturn;
    }
    // PCM TIGTK-5920 | 29/03/17 :Pooja Mantri : End

    // PCM TIGTK-6452/ ALM 3382 | 14/04/17 :KWagh : Start

    /**
     * @auther Kwagh
     * @param context
     * @param args
     * @return
     * @throws Exception
     *             This method is used to show "Close CR" command when : current is In Process owner is context.user CR is billable
     */
    public boolean showCloseCRCommand(Context context, String[] args) throws Exception {

        boolean flag = false;
        try {

            HashMap<?, ?> programMap = JPO.unpackArgs(args);

            String objectId = (String) programMap.get("objectId");
            DomainObject domCR = DomainObject.newInstance(context, objectId);

            StringList slSelectStmts = new StringList();
            slSelectStmts.add(DomainConstants.SELECT_CURRENT);
            slSelectStmts.add(DomainConstants.SELECT_OWNER);
            slSelectStmts.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRBILLABLE + "]");
            slSelectStmts.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRTYPE + "]");
            StringList slConnectedCO = domCR.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_CHANGEORDER + "].to.id");

            Map<String, Object> mapChangeObjectDetails = domCR.getInfo(context, slSelectStmts);
            String owner = (String) mapChangeObjectDetails.get(DomainConstants.SELECT_OWNER);
            String current = (String) mapChangeObjectDetails.get(DomainConstants.SELECT_CURRENT);
            String billable = (String) mapChangeObjectDetails.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRBILLABLE + "]");
            String CRtype = (String) mapChangeObjectDetails.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRTYPE + "]");
            String contextUser = context.getUser();

            if ("Program CR".equals(CRtype) && "In Process".equals(current) && contextUser.equals(owner) && slConnectedCO.isEmpty()) {
                flag = true;
            } else if ("In Process".equals(current) && contextUser.equals(owner) && "Yes".equals(billable)) {
                flag = true;
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in showCloseCRCommand: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
        return flag;
    }

    // PCM TIGTK-6452/ ALM 3382 | 14/04/17 :KWagh : Start

    // PCM TIGTK-9179|RFC 136 | 23/08/17 :KWagh : Start
    // PCM TIGTK-9179 RFC 136 | 3-Aug-17 :Sayali Deshpande : Start
    /**
     * @auther Sayali Deshpande
     * @param context
     * @param args
     * @return
     * @throws Exception
     *             This method is used to check "Attr[Final Payment Agreement Confirmed]" should be Y/N for Billable CR : current is In Process owner is context.user
     */
    public int checkBillableCRForPaymentConfirmation(Context context, String[] args) throws Exception {

        int intReturn = 1;

        try {
            if (args == null || args.length < 1) {
                throw (new IllegalArgumentException());
            }
            String strCRObjId = args[0];

            DomainObject domCR = DomainObject.newInstance(context, strCRObjId);

            StringList slSelectStmts = new StringList();
            slSelectStmts.add(DomainConstants.SELECT_CURRENT);
            slSelectStmts.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRBILLABLE + "]");
            slSelectStmts.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_FINALPAYMENTAGREEMENTCONFIRMED + "]");
            Map<String, Object> mapChangeObjectDetails = domCR.getInfo(context, slSelectStmts);
            String strCRCurrents = (String) mapChangeObjectDetails.get(DomainConstants.SELECT_CURRENT);
            String strBillableValue = (String) mapChangeObjectDetails.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRBILLABLE + "]");
            String strFinalPaymentAgreementConfirmationValue = (String) mapChangeObjectDetails.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_FINALPAYMENTAGREEMENTCONFIRMED + "]");

            if (TigerConstants.STATE_PSS_CR_INPROCESS.equals(strCRCurrents) && (strBillableValue.equalsIgnoreCase("Yes")) && UIUtil.isNotNullAndNotEmpty(strFinalPaymentAgreementConfirmationValue)) {

                intReturn = 0;

            } else if ((TigerConstants.STATE_PSS_CR_INPROCESS.equals(strCRCurrents) && (strBillableValue.equalsIgnoreCase("No")))) {

                intReturn = 0;
            }

        } catch (RuntimeException ex) {
            logger.debug("pss_enoECMChangeRequest:checkBillableCRForPaymentConfirmation:ERROR", ex);
            throw ex;
        } catch (Exception ex) {
            logger.debug("pss_enoECMChangeRequest:checkBillableCRForPaymentConfirmation:ERROR", ex);
            throw ex;
        }
        String strAlertMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                "PSS_EnterpriseChangeMgt.Alert.SelectValueForFinalPaymentAgreementConfirmed");
        if (intReturn == 1) {
            emxContextUtilBase_mxJPO.mqlNotice(context, strAlertMessage);
        }
        return intReturn;
    }
    // PCM TIGTK-9179|RFC 136 | 23/08/17 :KWagh : End
    // PCM TIGTK-9179 RFC 136 | 3-Aug-17 :Sayali Deshpande : END

    /**
     * This method is used to check that context person is Member of Program_Project which is connected to From side (Change) Object. This method is called from Check trigger of Relatioship
     * PSS_AffectedItem creation. PCM : TIGTK-7009 : 17/04/2017 : AB
     * @param context
     * @param args
     * @throws Exception
     */
    public int checkContextPersonIsProgramProjectMemberOrNot(Context context, String args[]) throws Exception {
        int intReturn = 0;
        // TIGTK-6866:Phase-2.0:PKH:Start

        String strCRStatus = PropertyUtil.getGlobalRPEValue(context, "PSS_AutoCRCreation");
        // TIGTK-13602-03-04-2018 : START
        String strIsGoToProduction = context.getCustomData("BOM_GO_TO_PRODUCTION");
        if ((UIUtil.isNotNullAndNotEmpty(strCRStatus) && strCRStatus.equals("True")) || (UIUtil.isNotNullAndNotEmpty(strIsGoToProduction) && strIsGoToProduction.equals("TRUE"))) {
            // TIGTK-13602-03-04-2018 : END
            // PropertyUtil.setGlobalRPEValue(context, "PSS_AutoCRCreation", "False");
            return 0;
        }
        // TIGTK-6866:Phase-2.0:PKH:END
        try {
            String strFromObjectID = args[0];
            String strFromObjectType = args[2];

            String strQuery = DomainConstants.EMPTY_STRING;
            String strContextUser = context.getUser();
            String loggedinUserName = PropertyUtil.getGlobalRPEValue(context, ContextUtil.MX_LOGGED_IN_USER_NAME);
            // PCM:PHASE1.1 : TIGTK-9293 : PSE : 30-08-2017 : START
            String strToObjectId = args[1];
            DomainObject domAffectedItemObj = DomainObject.newInstance(context, strToObjectId);
            StringList slObjectSelects = new StringList(2);
            slObjectSelects.addElement(DomainConstants.SELECT_POLICY);
            slObjectSelects.addElement(DomainConstants.SELECT_CURRENT);
            Map mAffectedItemInfoMap = domAffectedItemObj.getInfo(context, slObjectSelects);
            String strAffectedItemPolicy = (String) mAffectedItemInfoMap.get(DomainConstants.SELECT_POLICY);
            String strAffectedItemState = (String) mAffectedItemInfoMap.get(DomainConstants.SELECT_CURRENT);
            if (!strAffectedItemPolicy.equalsIgnoreCase(TigerConstants.POLICY_STANDARDPART) && !strAffectedItemState.equalsIgnoreCase(TigerConstants.STATE_STANDARDPART_RELEASE)) {
                // PCM:PHASE1.1 : TIGTK-9293 : PSE : 30-08-2017 : END
                if (strContextUser.equalsIgnoreCase(TigerConstants.PERSON_USER_AGENT)) {
                    strContextUser = loggedinUserName;
                }
                // PCM:PHASE1.1 : TIGTK-9293 : PSE : 30-08-2017 : START
            }
            // PCM:PHASE1.1 : TIGTK-9293 : PSE : 30-08-2017 : END
            StringList slProgramProjectMembers = null;

            // get the connected Program-Project Members of Change Object
            DomainObject domFromObjectID = DomainObject.newInstance(context, strFromObjectID);

            if (strFromObjectType.equalsIgnoreCase(TigerConstants.TYPE_PSS_CHANGEREQUEST)) {
                strQuery = "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS + "].to.name";
            } else {
                strQuery = "to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.from["
                        + TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS + "].to.name";
            }
            slProgramProjectMembers = domFromObjectID.getInfoList(context, strQuery);
            // Check the context user is Program_Project member or not
            // PCM:PHASE1.1 : TIGTK-9293 : PSE : 30-08-2017 : START
            if (!slProgramProjectMembers.contains(strContextUser) && !strContextUser.equalsIgnoreCase(TigerConstants.PERSON_USER_AGENT)) {
                // PCM:PHASE1.1 : TIGTK-9293 : PSE : 30-08-2017 : END
                String strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                        "PSS_EnterpriseChangeMgt.Notice.ContextUserNotMemberOfProgramProject");
                emxContextUtilBase_mxJPO.mqlNotice(context, strMessage);
                // TIGTK-7040:Modified on 27/04/2017 by SIE :Start
                PropertyUtil.setRPEValue(context, "disconnectAffectedItemFromCR", "true", true);
                // TIGTK-7040:Modified on 27/04/2017 by SIE :END
                intReturn = 1;
            }

        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in checkCotextPersonIsProgramProjectMemberOrNot: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End
        }
        return intReturn;
    }

    /**
     * This method is called for Program-Function on "Requested Assesment End Date" field of CR. PCM : TIGTK-6361 : 17/05/17 : AB
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public String getCRRequestedAssesmentEndDateValue(Context context, String args[]) throws Exception {
        try {
            String strReturn = DomainConstants.EMPTY_STRING;
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            HashMap<?, ?> requestMap = (HashMap<?, ?>) programMap.get("requestMap");

            // Get the object id of Change Request and get the attribute "CR Requested Assesment End Date" Value.
            String strChangeObjectId = (String) requestMap.get("objectId");
            DomainObject domChangeObject = DomainObject.newInstance(context, strChangeObjectId);
            String strCRRequestdAssesmentEndDate = (String) domChangeObject.getInfo(context, "attribute[" + TigerConstants.ATTRIBUTE_PSS_CRREQUESTEDASSESSMENTENDDATE + "]");

            // If attribute "CR Requested Assesment End Date" is blank then show current date + 7 days in "Requested Assessment End Date" field.
            if (UIUtil.isNullOrEmpty(strCRRequestdAssesmentEndDate)) {
                strReturn = this.getRouteDueDate(context, args);
            } else {
                strReturn = strCRRequestdAssesmentEndDate;
            }
            return strReturn;
        } catch (Exception e) {
            logger.error("Error in getCRRequestedAssesmentEndDateValue: ", e);
            e.printStackTrace();
            throw e;
        }

    }

    /**
     * Description : This method is called for Update Program- Update Function on "Requested Assesment End Date" field of CR.PCM : TIGTK-6361 : 17/05/17 : AB
     * @param context
     * @param args
     * @throws Exception
     */
    public void setCRRequestedAssesmentEndDateValue(Context context, String args[]) throws Exception {
        try {
            // Get the object id of Change request and new value of Attribute
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            HashMap<?, ?> paramMap = (HashMap<?, ?>) programMap.get("paramMap");
            String objectId = (String) paramMap.get("objectId");
            String newValue = (String) paramMap.get("New Value");

            // set the new Value of attribute "CR Requested Assesment End Date" .
            if (UIUtil.isNotNullAndNotEmpty(objectId) && UIUtil.isNotNullAndNotEmpty(newValue)) {
                DomainObject domObject = DomainObject.newInstance(context, objectId);
                domObject.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CRREQUESTEDASSESSMENTENDDATE, newValue);
            }
        } catch (Exception e) {
            logger.error("Error in setCRRequestedAssesmentEndDateValue: ", e);
            throw e;
        }

    }

    /**
     * PCM : TIGTK-8219 : 30/05/2017 : AB This method is used for Access function on TransferOwnership command on CR, CO, MCO, and CN.
     * @param context
     * @param args
     *            Object ID of ChangeObject
     * @return boolean TRUE or FALSE
     */
    public boolean checkAccessForTransferOwnership(Context context, String args[]) throws Exception {
        boolean flag = false;
        try {
            HashMap<?, ?> programMap = JPO.unpackArgs(args);
            String objectId = (String) programMap.get("objectId");
            DomainObject domChange = DomainObject.newInstance(context, objectId);

            // Get the Owner and current state of Change Object
            StringList slObjectSelects = new StringList();
            slObjectSelects.add(DomainConstants.SELECT_CURRENT);
            slObjectSelects.add(DomainConstants.SELECT_OWNER);

            Map<String, Object> mapChangeObjectDetails = domChange.getInfo(context, slObjectSelects);
            String strOwner = (String) mapChangeObjectDetails.get(DomainConstants.SELECT_OWNER);
            String strCurrent = (String) mapChangeObjectDetails.get(DomainConstants.SELECT_CURRENT);

            // If current user is Global user or PLM SupportTeam then allow access & If change object is in Create/Prepare state then check if loggedin user is owner or not.
            String strLoggedUser = context.getUser();
            String strLoggedUserSecurityContext = PersonUtil.getDefaultSecurityContext(context, strLoggedUser);

            if (UIUtil.isNotNullAndNotEmpty(strLoggedUserSecurityContext)) {
                String strLoggerUserRole = (strLoggedUserSecurityContext.split("[.]")[0]);

                if (strLoggerUserRole.equalsIgnoreCase(TigerConstants.ROLE_PSS_GLOBAL_ADMINISTRATOR) || strLoggerUserRole.equalsIgnoreCase(TigerConstants.ROLE_PSS_PLM_SUPPORT_TEAM)) {
                    flag = true;
                } else if ((strCurrent.equalsIgnoreCase(TigerConstants.STATE_PSS_CR_CREATE) || strCurrent.equalsIgnoreCase(TigerConstants.STATE_PSS_CHANGEORDER_PREPARE)
                        || strCurrent.equalsIgnoreCase(TigerConstants.STATE_PREPARE_CN)) && strOwner.equalsIgnoreCase(strLoggedUser)) {
                    flag = true;
                }
            }

        } catch (Exception ex) {
            logger.error("Error in checkAccessForTransferOwnership: ", ex);
            throw ex;
        }
        return flag;

    }

    // TIGTK-6843:Phase-2.0:PKH:Start
    /**
     * Method invoked from the Delete Trigger of rel "Change Affected Item". This method is Used to remove the Implemented Object while removing affected item.
     * @param context
     * @param args
     *            CA Object and Affected Item Object
     * @return integer
     * @throws Exception
     */
    public int deleteImplementationItemWhileDeletingAffectedItem(Context context, String args[]) throws Exception {
        logger.debug("PSS_enoECMChangeRequest:deleteImplementationItemWhileDeletingAffectedItem:START");
        try {

            String strCancelStatus = PropertyUtil.getGlobalRPEValue(context, "PSS_COCancel");
            if (UIUtil.isNotNullAndNotEmpty(strCancelStatus) && strCancelStatus.equals("True")) {
                return 0;
            }
            String strCAObjectId = args[0];
            String strAffectedItemId = args[1];

            String SELECT_LOGICALID = "logicalid";
            String strLogicalId = "";
            String impItemRelId = "";

            DomainObject doObj = DomainObject.newInstance(context, strAffectedItemId);
            strLogicalId = doObj.getInfo(context, SELECT_LOGICALID);
            String objSelect = "relationship[" + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM + "|to.logicalid==\"" + strLogicalId + "\"].id";
            impItemRelId = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", strCAObjectId, objSelect);

            if (UIUtil.isNotNullAndNotEmpty(impItemRelId)) {
                DomainRelationship.disconnect(context, impItemRelId);
            }
            logger.debug("PSS_enoECMChangeRequest:deleteImplementationItemWhileDeletingAffectedItem:END");
        } catch (Exception Ex) {
            logger.error("PSS_enoECMChangeRequest:deleteImplementationItemWhileDeletingAffectedItem:ERROR", Ex);
            Ex.printStackTrace();
            throw Ex;
        }
        return 0;
    }
    // TIGTK-6843:Phase-2.0:PKH:End

    // PCM TIGTK-9163 | 01/08/17 : PTE : START
    /**
     * Description: Method to get Program Project from Current Collaborative Space where logged in user is not a member
     * @Date Created by PCM : TIGTK-9163
     * @param context
     * @param args
     * @return StringList
     * @throws Exception
     */
    public StringList getAllProgramProjectFromCS(Context context, String[] args) throws Exception {
        StringList slAllProgProjectCS = new StringList();
        try {
            String strProject = PersonUtil.getDefaultProject(context, context.getUser());
            String strwhere = "current!= " + TigerConstants.STATE_ACTIVE + " && current!= " + TigerConstants.STATE_OBSOLETE + " && current!= \"" + TigerConstants.STATE_NONAWARDED
                    + "\" && project == \"" + strProject + "\" && from[" + TigerConstants.RELATIONSHIP_PSS_SUBPROGRAMPROJECT + "]==False";

            StringList objSelect = new StringList(DomainConstants.SELECT_ID);

            MapList mlProgramProjectObjectsList = DomainObject.findObjects(context, TigerConstants.TYPE_PSS_PROGRAMPROJECT, TigerConstants.VAULT_ESERVICEPRODUCTION, strwhere, objSelect);
            if (!mlProgramProjectObjectsList.isEmpty()) {
                int iSize = mlProgramProjectObjectsList.size();

                mlProgramProjectObjectsList.sort(DomainConstants.SELECT_ORIGINATED, "descending", "date");
                for (int i = 0; i < iSize; i++) {
                    HashMap<?, ?> mapProgramProjectObject = (HashMap<?, ?>) mlProgramProjectObjectsList.get(i);
                    DomainObject domProgProj = DomainObject.newInstance(context, (String) mapProgramProjectObject.get(DomainConstants.SELECT_ID));
                    // get list of members connected to program project
                    StringList slProgramProjectMembers = domProgProj.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS + "].to.name");
                    if (!slProgramProjectMembers.contains(context.getUser())) {
                        slAllProgProjectCS.add(mapProgramProjectObject.get(DomainConstants.SELECT_ID));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error in getAllProgramProjectFromCS: ", e);
        }
        return slAllProgProjectCS;
    }

    // TIGTK-6315:Phase-2.0:Hiren:Start
    /**
     * Description : This method is used to get Issues list connected to ChangeRequest.
     * @param context
     * @param args
     * @return MapList
     * @throws Exception
     */
    public MapList getConnectedIssueToCR(Context context, String[] args) throws Exception {
        MapList mlIssueList = new MapList();
        try {

            HashMap<?, ?> programMap = JPO.unpackArgs(args);
            String strCRID = (String) programMap.get("objectId");
            StringList slObjectSle = new StringList(1);
            slObjectSle.addElement(DomainConstants.SELECT_ID);
            StringList slRelSle = new StringList(1);
            slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);
            DomainObject domCRObj = new DomainObject(strCRID);
            mlIssueList = domCRObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_GOVERNINGISSUE, TigerConstants.TYPE_PSS_ISSUE, slObjectSle, slRelSle, true, false, (short) 1, null, null,
                    0);

        } catch (Exception Ex) {
            logger.error("PSS_enoECMChangeRequest:getConnectedIssueToCR:ERROR", Ex);
        }
        return mlIssueList;
    }
    // TIGTK-6315:Phase-2.0:Hiren:End

    // TIGTK-6415 : TS : 10/08/2017 : START
    @SuppressWarnings({ "rawtypes" })
    public void promoteCRConnectedToIssue(Context context, String[] args) throws Exception {
        logger.debug("PSS_enoECMChangeRequest:promoteCRConnectedToIssue:START");
        int intReturn = 0;
        boolean isContextPushed = false;
        try {
            String contextUser = (String) context.getCustomData("PSS_CRcontextUser");

            if (contextUser != null && !"".equals(contextUser)) {
                ContextUtil.pushContext(context, contextUser, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                isContextPushed = true;
            }

            String strCRObjId = args[0];
            DomainObject domCR = DomainObject.newInstance(context, strCRObjId);
            StringList slObjectSelects = new StringList();
            slObjectSelects.add(DomainConstants.SELECT_ID);
            slObjectSelects.add(DomainConstants.SELECT_CURRENT);
            StringList slAllowedCRStateList = new StringList();
            slAllowedCRStateList.add(TigerConstants.STATE_COMPLETE_CR);
            slAllowedCRStateList.add(TigerConstants.STATE_REJECTED_CR);
            MapList mlRelatedIssueObjects = domCR.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_GOVERNINGISSUE, TigerConstants.TYPE_PSS_ISSUE, slObjectSelects, null, true, false,
                    (short) 0, null, null, 0);
            for (int i = 0; i < mlRelatedIssueObjects.size(); i++) {
                Map<?, ?> mObjectMap = (Map<?, ?>) mlRelatedIssueObjects.get(i);
                String strIssueID = (String) mObjectMap.get(DomainConstants.SELECT_ID);
                DomainObject domIssue = DomainObject.newInstance(context, strIssueID);

                MapList mlRelatedCRObjects = domIssue.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_GOVERNINGISSUE, TigerConstants.TYPE_PSS_CHANGEREQUEST, slObjectSelects, null, false,
                        true, (short) 0, null, null, 0);
                for (int j = 0; j < mlRelatedCRObjects.size(); j++) {
                    if (!mlRelatedCRObjects.isEmpty()) {
                        Map<?, ?> mObjectMapCR = (Map<?, ?>) mlRelatedCRObjects.get(j);
                        String strCRCurrent = (String) mObjectMapCR.get(DomainConstants.SELECT_CURRENT);
                        String strCRID = (String) mObjectMapCR.get(DomainConstants.SELECT_ID);
                        if (slAllowedCRStateList.contains(strCRCurrent)) {
                            intReturn = 0;
                        } else {
                            intReturn = 1;
                            break;
                        }
                    }
                }
                if (intReturn == 0) {
                    domIssue.promote(context);
                }
            }
            logger.debug("PSS_enoECMChangeRequest:promoteCRConnectedToIssue:END");
        } catch (RuntimeException e) {
            logger.error("Error in repeatSelectedPartInEBOM: ", e);
            throw e;
        } catch (Exception e) {
            logger.error("PSS_enoECMChangeRequest:promoteCRConnectedToIssue:ERROR", e);
            throw e;
        } finally {
            if (isContextPushed) {
                ContextUtil.popContext(context);
            }
        }
    }
    // TIGTK-6415 : TS : 10/08/2017 : END

    /**
     * PCM : TIGTK-9132 : 27/07/2017 : AB. This Method is used to check that Affected item is Released Standard Part and try to Connect CR which is other than Part's collobrative space or not.
     * @param context
     * @param domAffetcedItem
     * @param domChangeRequest
     * @return
     * @throws Exception
     */
    public boolean isAllowConnctionOfStandardPartAndChangeRequest(Context context, DomainObject domAffetcedItem, DomainObject domChangeRequest) throws Exception {
        boolean bolAllowConnection = false;
        try {
            StringList slObjectSelects = new StringList();
            slObjectSelects.add(DomainConstants.SELECT_POLICY);
            slObjectSelects.add(DomainConstants.SELECT_CURRENT);
            slObjectSelects.add("project");
            // get Info of Part
            Map<String, Object> mapItemInfo = (Map<String, Object>) domAffetcedItem.getInfo(context, slObjectSelects);
            String strItemCurrent = (String) mapItemInfo.get(DomainConstants.SELECT_CURRENT);
            String strItemPolicy = (String) mapItemInfo.get(DomainConstants.SELECT_POLICY);
            String strItemProject = (String) mapItemInfo.get("project");

            if (TigerConstants.POLICY_STANDARDPART.equalsIgnoreCase(strItemPolicy) && TigerConstants.STATE_PART_RELEASE.equalsIgnoreCase(strItemCurrent)) {
                // Get the Change Request's Collabrative Space
                String strCRCollabSpace = (String) domChangeRequest.getInfo(context, "project");
                // Check if Released Standard Part and CR's collabrative Space are differenet then allow Connection with Push-Pop context
                // PCM:PHASE1.1: TIGTK-9293 : PSE : 30-08-2017 : START
                matrix.db.Access mAccess = domAffetcedItem.getAccessMask(context);
                String strLoggedInPersonCollabSpace = PersonUtil.getDefaultProject(context, context.getUser());
                if (!strCRCollabSpace.equalsIgnoreCase(strItemProject)
                        || ((!mAccess.hasToConnectAccess()) && (!strCRCollabSpace.equalsIgnoreCase(strLoggedInPersonCollabSpace) || !strItemProject.equalsIgnoreCase(strLoggedInPersonCollabSpace)))) {
                    // PCM:PHASE1.1: TIGTK-9293 : PSE : 30-08-2017 : END
                    bolAllowConnection = true;
                }
            }
            return bolAllowConnection;
        } catch (Exception Ex) {
            logger.error("Error in isAllowConnctionOfStandardPartAndChangeRequest: ", Ex);
            throw Ex;
        }
    }

    /**
     * PCM : TIGTK-9132 : 27/07/2017 : AB. This Function is Wrapper function of isAllowConnctionOfStandardPartAndChangeRequest() Function.It is called from PSS_ECMUtil.jsp
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public boolean isAllowConnctionOfStandardPartAndChangeRequestWrapper(Context context, String args[]) throws Exception {
        boolean bolAllowConnection = false;
        try {
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            DomainObject domAffetcedItem = (DomainObject) programMap.get("domAffectedItem");
            DomainObject domChangeRequest = (DomainObject) programMap.get("domCR");

            bolAllowConnection = this.isAllowConnctionOfStandardPartAndChangeRequest(context, domAffetcedItem, domChangeRequest);
        } catch (Exception Ex) {
            logger.error("Error in isAllowConnctionOfStandardPartAndChangeRequestWrapper: ", Ex);
            throw Ex;
        }
        return bolAllowConnection;
    }

    /**
     * Description : This method is called on "CO Cancellation Request" .
     * @author PTE
     * @args
     * @Date Aug 16, 2017
     */
    @com.matrixone.apps.framework.ui.PostProcessCallable
    public void cancellationRequestToCO(Context context, String[] args) throws Exception {
        logger.debug("PSS_enoECMChangeRequest : cancellationRequestToCO : START");
        try {
            HashMap<String, Object> programMap = (HashMap<String, Object>) JPO.unpackArgs(args);
            HashMap<String, Object> paramMap = (HashMap<String, Object>) programMap.get("paramMap");
            HashMap<?, ?> requestMap = (HashMap<?, ?>) programMap.get(ChangeConstants.REQUEST_MAP);
            // Get Object Id of Change Request & Reason for Rejection of Change Request
            String strCROrbjectId = (String) paramMap.get("objectId");
            DomainObject domCRObject = DomainObject.newInstance(context, strCROrbjectId);
            Map<String, Object> payload = new HashMap<String, Object>();
            StringList slConnectedCOOwner = domCRObject.getInfoList(context, "from[" + ChangeConstants.RELATIONSHIP_CHANGE_ORDER + "].to.owner");
            if (!slConnectedCOOwner.isEmpty()) {
                payload.put("toList", slConnectedCOOwner);
                payload.put("ReasonForCancellationRequest", (String) requestMap.get("ReasonForCancellationRequest"));
                emxNotificationUtil_mxJPO.objectNotification(context, strCROrbjectId, "PSS_COCancellationRequestNotification", payload);
            }
            logger.debug("PSS_enoECMChangeRequest : cancellationRequestToCO : END");
        } catch (Exception ex) {
            logger.error("PSS_enoECMChangeRequest : cancellationRequestToCO : ERROR ", ex);
            throw ex;
        }
    }

    // TIGTK-7589:31/8/2017:START
    /**
     * @param context
     * @param domCR
     * @throws Exception
     */
    public void cancelImpactAnalysisStructure(Context context, DomainObject domCR) throws Exception {
        logger.debug("PSS_enoECMChangeRequest : cancelImpactAnalysisStructure : START");

        try {

            StringList slObjectSelects = new StringList();
            slObjectSelects.addElement(DomainConstants.SELECT_ID);

            String strwhere = "revision == last";
            MapList mlImpactAnalysis = domCR.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS, TigerConstants.TYPE_PSS_IMPACTANALYSIS, slObjectSelects,
                    DomainConstants.EMPTY_STRINGLIST, false, true, (short) 1, strwhere, null, 0);

            if (!mlImpactAnalysis.isEmpty()) {
                Map<?, ?> mapImpactAnalysis = (Map<?, ?>) mlImpactAnalysis.get(0);
                String strIAId = (String) mapImpactAnalysis.get(DomainConstants.SELECT_ID);
                DomainObject domIA = DomainObject.newInstance(context, strIAId);
                domIA.setState(context, TigerConstants.STATE_IMPACTANALYSIS_CANCELLED);
                MapList mlRoleAssement = domIA.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT, TigerConstants.TYPE_PSS_ROLEASSESSMENT, slObjectSelects,
                        DomainConstants.EMPTY_STRINGLIST, false, true, (short) 1, strwhere, null, 0);
                for (Object objRAInfo : mlRoleAssement) {
                    Map<?, ?> mapRAInfo = (Map<?, ?>) objRAInfo;
                    String strRAId = (String) mapRAInfo.get(DomainConstants.SELECT_ID);
                    DomainObject domRA = DomainObject.newInstance(context, strRAId);
                    domRA.setState(context, TigerConstants.STATE_ROLEASSESSMENT_CANCELLED);
                }
            }

            logger.debug("PSS_enoECMChangeRequest : cancelImpactAnalysisStructure : END");
        } catch (Exception e) {
            logger.error("PSS_enoECMChangeRequest : cancelImpactAnalysisStructure : ERROR ", e);
            throw e;
        }
    }

    // TIGTK-7589:31/8/2017:END
    // TIGTK-7587:9/6/2017:START
    /**
     * @param context
     * @param args
     * @throws Exception
     */
    public void promoteCRToInReviewState(Context context, String[] args) throws Exception {
        logger.debug("PSS_enoECMChangeRequest : promoteCRToInReviewState : START");
        try {

            String contextUser = context.getUser();
            context.setCustomData("PSS_CRcontextUser", contextUser);

            String strIAObjId = args[0];
            DomainObject domIA = DomainObject.newInstance(context, strIAObjId);
            String strRelWhere = "attribute[" + TigerConstants.ATTRIBUTE_PSS_DECISION + "] == \"" + TigerConstants.ATTRIBUTE_PSS_DECISION_RANGE_NODECISION + "\"";

            StringList slBusSelect = new StringList(DomainConstants.SELECT_NAME);
            slBusSelect.add("to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.id");
            slBusSelect.add(DomainConstants.SELECT_TYPE);
            slBusSelect.add(DomainConstants.SELECT_REVISION);
            slBusSelect.add(DomainConstants.SELECT_CURRENT);

            String strIAName = DomainConstants.EMPTY_STRING;
            String strIAType = DomainConstants.EMPTY_STRING;
            String strIARevision = DomainConstants.EMPTY_STRING;

            String strCRName = DomainConstants.EMPTY_STRING;
            String strCRType = DomainConstants.EMPTY_STRING;
            String strCRRevision = DomainConstants.EMPTY_STRING;
            String strCRCurrentState = DomainConstants.EMPTY_STRING;

            MapList mlRAObjs = domIA.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT, TigerConstants.TYPE_PSS_ROLEASSESSMENT, DomainConstants.EMPTY_STRINGLIST,
                    DomainConstants.EMPTY_STRINGLIST, false, true, (short) 0, null, strRelWhere, 0);

            if (mlRAObjs.isEmpty()) {
                String strCRId = DomainConstants.EMPTY_STRING;

                Map<String, Object> mapIAInfo = (Map<String, Object>) domIA.getInfo(context, slBusSelect);
                if (!mapIAInfo.isEmpty()) {
                    strIAName = (String) mapIAInfo.get(DomainConstants.SELECT_NAME);
                    strCRId = (String) mapIAInfo.get("to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.id");
                    strIAType = (String) mapIAInfo.get(DomainConstants.SELECT_TYPE);
                    strIARevision = (String) mapIAInfo.get(DomainConstants.SELECT_REVISION);
                }

                DomainObject domCR = DomainObject.newInstance(context, strCRId);

                Map<String, Object> mapCRInfo = (Map<String, Object>) domCR.getInfo(context, slBusSelect);
                if (!mapCRInfo.isEmpty()) {
                    strCRName = (String) mapCRInfo.get(DomainConstants.SELECT_NAME);
                    strCRCurrentState = (String) mapCRInfo.get(DomainConstants.SELECT_CURRENT);
                    strCRType = (String) mapCRInfo.get(DomainConstants.SELECT_TYPE);
                    strCRRevision = (String) mapCRInfo.get(DomainConstants.SELECT_REVISION);
                }

                if (TigerConstants.STATE_EVALUATE.equals(strCRCurrentState)) {
                    MqlUtil.mqlCommand(context, "history off", true, false);
                    ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                    // Promote CR to "In review" state
                    domCR.promote(context);
                    // TIGTK-13618 : START

                    // Promote IA to "Complete" state
                    domIA.promote(context);
                    ContextUtil.popContext(context);

                    MqlUtil.mqlCommand(context, "history on", true, false);
                    String strMqlHistory = "modify bus $1 add history $2 comment $3";

                    StringBuffer sbHistoryAction = new StringBuffer();
                    sbHistoryAction.delete(0, sbHistoryAction.length());
                    sbHistoryAction.append(" Add history for Promote ");
                    sbHistoryAction.append(strIAName);
                    sbHistoryAction.append(" ");
                    sbHistoryAction.append(strIAType);
                    sbHistoryAction.append(" ");
                    sbHistoryAction.append(strIARevision);

                    StringBuffer sbCRHistoryAction = new StringBuffer();
                    sbCRHistoryAction.delete(0, sbHistoryAction.length());
                    sbCRHistoryAction.append(" Add history for Promote ");
                    sbCRHistoryAction.append(strCRName);
                    sbCRHistoryAction.append(" ");
                    sbCRHistoryAction.append(strCRType);
                    sbCRHistoryAction.append(" ");
                    sbCRHistoryAction.append(strCRRevision);

                    // Update Impact Analysis object history
                    MqlUtil.mqlCommand(context, strMqlHistory, strIAObjId, "Promote", sbHistoryAction.toString());

                    // Update Change Request object history
                    MqlUtil.mqlCommand(context, strMqlHistory, strCRId, "Promote", sbCRHistoryAction.toString());
                    // TIGTK-13618 : END
                }
            }
        } catch (RuntimeException e) {
            logger.error("PSS_enoECMChangeRequest : promoteCRToInReviewState : ERROR ", e);
            throw e;
        } finally {
            context.setCustomData("PSS_CRcontextUser", "");
        }
    }

    // TIGTK-7587:9/6/2017:END
    // TIGTK-9020:Rutuja Ekatpure:5/9/2017:Start
    /***
     * This method is used to call notification object for Impact Analysis Rejection. This method is called from modify trigger of attribute "PSS_Decision".
     * @param context
     * @param args
     * @throws Exception
     */
    public void callImpactAnalysisRejectionNotification(Context context, String args[]) throws Exception {
        logger.debug("PSS_enoECMChangeRequest : callImpactAnalysisRejectionNotification : Start");
        String strNewValue = (String) args[0];
        String strIAObjectId = (String) args[1];

        if (TigerConstants.ATTRIBUTE_PSS_DECISION_RANGE_NOGO.equalsIgnoreCase(strNewValue)) {
            emxNotificationUtil_mxJPO.objectNotification(context, strIAObjectId, "PSSImpactAnalysisRejectionNotification", null);
        }
        logger.debug("PSS_enoECMChangeRequest : callImpactAnalysisRejectionNotification : End");
    }

    /***
     * This method is used to call notification object for Impact Analysis reassignment. This method is called from Change Owner trigger on RA object.
     * @param context
     * @param args
     * @throws Exception
     */
    public void callImpactAnalysisChangeOwnerNotification(Context context, String args[]) throws Exception {
        logger.debug("PSS_enoECMChangeRequest : callImpactAnalysisChangeOwnerNotification : Start");
        String strIACreatoin = PropertyUtil.getGlobalRPEValue(context, "PSS_CREATE_IA");
        String strCRPromotion = PropertyUtil.getRPEValue(context, "PSS_Delegate", false);
        if ((UIUtil.isNotNullAndNotEmpty(strIACreatoin) && strIACreatoin.equals("True")) || (UIUtil.isNotNullAndNotEmpty(strCRPromotion) && strCRPromotion.equalsIgnoreCase("True"))) {
            return;
        }
        String strRAObjectId = (String) args[0];
        DomainObject domRAObj = DomainObject.newInstance(context, strRAObjectId);
        String strCRID = domRAObj.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.id");
        String strRAOwner = domRAObj.getInfo(context, DomainConstants.SELECT_OWNER);
        updateRAEOwners(context, strRAObjectId, strRAOwner);
        if (UIUtil.isNotNullAndNotEmpty(strCRID)) {
            DomainObject domCRObj = DomainObject.newInstance(context, strCRID);
            String strCRCurrent = domCRObj.getInfo(context, DomainConstants.SELECT_CURRENT);
            // TIGTK-10792: TS : 08/11/2017 : Start
            Map<String, String> payload = new HashMap<String, String>();
            String strContextUser = context.getUser();
            payload.put("fromList", strContextUser);
            if (TigerConstants.STATE_EVALUATE.equals(strCRCurrent)) {
                emxNotificationUtil_mxJPO.objectNotification(context, strRAObjectId, "PSSRoleAssessmentReassignmentNotification", payload);
                // TIGTK-10792: TS : 08/11/2017 : End
            }
        }

        logger.debug("PSS_enoECMChangeRequest : callImpactAnalysisChangeOwnerNotification : End");
        PropertyUtil.setRPEValue(context, "PSS_Delegate", "false", false);
    }
    // TIGTK-9020:Rutuja Ekatpure:5/9/2017:End

    /**
     * This method is used to Reject CR when last Task is Rejected
     * @author sirale
     * @param context
     *            RFC-135
     * @throws Exception
     */
    public int promoteChangeRequest(Context context, String[] args) throws Exception {
        try {
            String ApprovalStatus_REJECT = "Reject";
            String ApprovalStatus_STOPPED = "Stopped";
            String ApprovalStatus_APPROVE = "Approve";
            String strobjectId = args[0];
            StringList selectStmts = new StringList(4);
            selectStmts.addElement(DomainConstants.SELECT_ID);
            selectStmts.addElement(DomainConstants.SELECT_POLICY);

            StringList relStmts = new StringList(4);
            relStmts.addElement("id[connection]");
            DomainObject domInboxTaskObject = DomainObject.newInstance(context, strobjectId);

            MapList mlObjRoute = domInboxTaskObject.getRelatedObjects(context, DomainConstants.RELATIONSHIP_ROUTE_TASK, // relationship pattern
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
                    String strattrRouteStatus = domRoutebject.getAttributeValue(context, DomainConstants.ATTRIBUTE_ROUTE_STATUS);
                    StringList busSelects = new StringList();
                    busSelects.add(DomainConstants.SELECT_NAME);
                    busSelects.add(DomainConstants.SELECT_REVISION);
                    MapList mlConnectedObject = domRoutebject.getRelatedObjects(context, DomainConstants.RELATIONSHIP_OBJECT_ROUTE, // relationship pattern
                            TigerConstants.TYPE_PSS_CHANGEREQUEST, // object pattern
                            selectStmts, // object selects
                            relStmts, // relationship selects
                            true, // to direction
                            false, // from direction
                            (short) 1, // recursion level
                            null, // object where clause
                            null, 0);
                    if (mlConnectedObject.size() > 0) {
                        Map<?, ?> connectedObjMap = (Map<?, ?>) mlConnectedObject.get(0);

                        String sObjectID = (String) connectedObjMap.get(DomainConstants.SELECT_ID);
                        DomainObject domCRObj = DomainObject.newInstance(context, sObjectID);
                        String current = domCRObj.getInfo(context, DomainConstants.SELECT_CURRENT);
                        if (domCRObj.isKindOf(context, TigerConstants.TYPE_PSS_CHANGEREQUEST) && current.equals(TigerConstants.STATE_INREVIEW_CR)) {
                            String strTaskApprovalStatus = domInboxTaskObject.getAttributeValue(context, DomainConstants.ATTRIBUTE_APPROVAL_STATUS);
                            if (ApprovalStatus_REJECT.equals(strTaskApprovalStatus) && (strattrRouteStatus.equals(ApprovalStatus_STOPPED))) // If Rejected
                            {
                                String strCOId = domCRObj.getInfo(context, ("from[" + TigerConstants.RELATIONSHIP_CHANGEORDER + "].to.id"));

                                if (UIUtil.isNotNullAndNotEmpty(strCOId)) {
                                    DomainObject domCOObj = DomainObject.newInstance(context, strCOId);
                                    String currentState = domCOObj.getInfo(context, DomainConstants.SELECT_CURRENT);
                                    if (currentState.equals(TigerConstants.STATE_PSS_CHANGEORDER_COMPLETE) || currentState.equals(TigerConstants.STATE_CHANGEORDER_IMPLEMENTED)
                                            || currentState.equals(TigerConstants.STATE_PSS_CHANGEORDER_ONHOLD)) {
                                        String strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                                                "PSSEnterpriseChangeMgt.Notice.NoCompleteCOtObeConnectedtoCR");
                                        throw new Exception(strMessage);
                                    } else if (!TigerConstants.STATE_PSS_CHANGEORDER_CANCELLED.equals(currentState)) {
                                        domCOObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_BRANCH_TO, TigerConstants.STATE_PSS_CHANGEORDER_CANCELLED);
                                        domCOObj.promote(context);
                                    }
                                }
                                domCRObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_BRANCH_TO, TigerConstants.STATE_REJECTED_CR);
                                domCRObj.promote(context);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {

            logger.error("Error in promoteChangeRequest: ", ex);
            throw ex;
        }
        return 0;
    }

    /****
     * This method is used to abstain Impact Analysis.
     * @param context
     * @param args
     * @return MapList
     * @throws Exception
     */
    public void abstainImpactAnalysis(Context context, String[] args) throws Exception {
        logger.debug("abstainImpactAnalysis : START");
        boolean isContextPushed = false;
        try {
            Map<?, ?> programMap = (Map<?, ?>) JPO.unpackArgs(args);
            String strObjectId = (String) programMap.get("objectId");
            DomainObject domObjCR = DomainObject.newInstance(context, strObjectId);
            ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            isContextPushed = true;
            domObjCR.setAttributeValue(context, TigerConstants.ATTRIBUTE_BRANCH_TO, TigerConstants.STATE_COMPLETE_CR);
            domObjCR.promote(context);
            if (isContextPushed) {
                ContextUtil.popContext(context);
                isContextPushed = false;
            }
            Map<String, String> payload = new HashMap<String, String>();
            String strContextUser = context.getUser();
            payload.put("fromList", strContextUser);
            emxNotificationUtil_mxJPO.objectNotification(context, strObjectId, "PSS_AbstainImpactAnalysisNotification", payload);
            logger.debug("abstainImpactAnalysis : END");
        } catch (Exception ex) {
            logger.error("abstainImpactAnalysis : ERROR ", ex);
            throw ex;
        } finally {
            if (isContextPushed) {
                ContextUtil.popContext(context);
                isContextPushed = false;
            }
        }
    }

    /****
     * This Method is used to allow connection of the related Change Request to a different context Change request
     * @param context
     * @param To
     *            and From object ids
     * @return boolean
     * @throws Exception
     * @since 07-09-2017
     * @author psalunke - TIGTK-9626
     */
    public boolean isPushPopContextRequired(Context context, String strFromSideObjectId, String strToSideObjectId) throws Exception {
        logger.debug("PSS_enoECMChangeRequest : isPushPopContextRequired : START");
        try {
            boolean bAllowPushContext = false;
            BusinessObject busToSideObject = new BusinessObject(strToSideObjectId);
            matrix.db.Access mToSideObjectAccess = busToSideObject.getAccessMask(context);
            if (mToSideObjectAccess.hasReadAccess() && mToSideObjectAccess.hasShowAccess()) {
                BusinessObject busFromSideObject = new BusinessObject(strFromSideObjectId);
                matrix.db.Access mFromSideObjectAccess = busFromSideObject.getAccessMask(context);
                if ((!mFromSideObjectAccess.hasToConnectAccess()) || (!mToSideObjectAccess.hasFromConnectAccess())) {
                    bAllowPushContext = true;
                }
            }
            logger.debug("PSS_enoECMChangeRequest : isPushPopContextRequired : END");
            return bAllowPushContext;
        } catch (Exception ex) {
            logger.error("PSS_enoECMChangeRequest : isPushPopContextRequired : ERROR : ", ex);
            throw ex;
        }
    }

    /**
     * This Method is used to allow disconnection of the related Change Request to a different context Change request
     * @param context
     * @param args
     * @return void
     * @throws Exception
     * @since 07-09-2017
     * @author psalunke - TIGTK-9626
     */
    public void disconnectSelectedObjects(Context context, String[] args) throws Exception {
        logger.debug("PSS_enoECMChangeRequest : disconnectSelectedObjects : START");
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strParentCRObjectId = (String) programMap.get("parentCRId");
            StringList slObjectIds = (StringList) programMap.get("selectedCRIds");
            StringList slRelIds = (StringList) programMap.get("selectedCRRelIds");
            if (slObjectIds != null && !slObjectIds.isEmpty()) {
                StringList slObjectSelects = new StringList(3);
                slObjectSelects.add(DomainConstants.SELECT_TYPE);
                slObjectSelects.add(DomainConstants.SELECT_NAME);
                slObjectSelects.add(DomainConstants.SELECT_REVISION);
                DomainObject domParentCRObject = DomainObject.newInstance(context, strParentCRObjectId);
                Map mpParentCRInfo = domParentCRObject.getInfo(context, slObjectSelects);

                StringBuffer sbParentCRInfo = new StringBuffer(3);
                sbParentCRInfo.append((String) mpParentCRInfo.get(DomainConstants.SELECT_TYPE));
                sbParentCRInfo.append(" ");
                sbParentCRInfo.append((String) mpParentCRInfo.get(DomainConstants.SELECT_NAME));
                sbParentCRInfo.append(" ");
                sbParentCRInfo.append((String) mpParentCRInfo.get(DomainConstants.SELECT_REVISION));

                for (int i = 0; i < slObjectIds.size(); i++) {
                    String strToObjectId = (String) slObjectIds.get(i);
                    String strRelId = (String) slRelIds.get(i);

                    boolean bAllowPushContext = isPushPopContextRequired(context, strParentCRObjectId, strToObjectId);

                    if (bAllowPushContext) {
                        MqlUtil.mqlCommand(context, "history off", true, false);
                        boolean bIsConextPushed = false;
                        ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                        bIsConextPushed = true;
                        DomainRelationship.disconnect(context, strRelId);
                        if (bIsConextPushed) {
                            ContextUtil.popContext(context);
                        }
                        MqlUtil.mqlCommand(context, "history on", true, false);

                        String strMqlHistory = "modify bus $1 add history $2 comment $3";
                        DomainObject domSelectedObject = DomainObject.newInstance(context, strToObjectId);
                        Map mToCRInfo = domSelectedObject.getInfo(context, slObjectSelects);
                        StringBuffer sbToCRInfo = new StringBuffer(3);
                        sbToCRInfo.append((String) mToCRInfo.get(DomainConstants.SELECT_TYPE));
                        sbToCRInfo.append(" ");
                        sbToCRInfo.append((String) mToCRInfo.get(DomainConstants.SELECT_NAME));
                        sbToCRInfo.append(" ");
                        sbToCRInfo.append((String) mToCRInfo.get(DomainConstants.SELECT_REVISION));

                        // Update disconnect history on from side object
                        MqlUtil.mqlCommand(context, strMqlHistory, strParentCRObjectId, "disconnect", TigerConstants.RELATIONSHIP_PSS_RELATED_CR + "  to " + sbToCRInfo.toString());

                        // Update disconnect history on to side object
                        MqlUtil.mqlCommand(context, strMqlHistory, strToObjectId, "disconnect", TigerConstants.RELATIONSHIP_PSS_RELATED_CR + "  from " + sbParentCRInfo.toString());
                    } else {
                        DomainRelationship.disconnect(context, strRelId);
                    }
                }
            }
            logger.debug("PSS_enoECMChangeRequest : disconnectSelectedObjects : END");
        } catch (Exception ex) {
            logger.error("PSS_enoECMChangeRequest : disconnectSelectedObjects : ERROR : ", ex);
            throw ex;
        }
    }

    // BR002:Rutuja Ekatpure:15/9/2017:start
    /***
     * this method used for notification to list on CR demote from Review to Submit.
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public StringList getAllProgramProjMembers(Context context, String[] args) throws Exception {
        logger.debug("getAllProgramProjMembers : Start");
        Map<?, ?> programMap = (Map<?, ?>) JPO.unpackArgs(args);
        String strObjectId = (String) programMap.get("id");
        pss.ecm.ui.CommonUtil_mxJPO commonObj = new pss.ecm.ui.CommonUtil_mxJPO(context, args);
        StringList slMemberList = commonObj.getProgramProjectTeamMembersForChange(context, strObjectId, DomainConstants.EMPTY_STRINGLIST, false);
        slMemberList.remove(context.getUser());
        logger.debug("getAllProgramProjMembers : End");
        return slMemberList;
    }

    /***
     * this method used for notification to list on CR demote from Review to Submit.
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public StringList getLeadChangeManagerOfProgramProj(Context context, String[] args) throws Exception {
        logger.debug("getLeadChangeManagerOfProgramProj : Start");
        Map<?, ?> programMap = (Map<?, ?>) JPO.unpackArgs(args);
        String strObjectId = (String) programMap.get("id");
        StringList slRole = new StringList();
        slRole.add(TigerConstants.ROLE_PSS_CHANGE_COORDINATOR);
        pss.ecm.ui.CommonUtil_mxJPO commonObj = new pss.ecm.ui.CommonUtil_mxJPO(context, args);
        StringList slMemberList = commonObj.getProgramProjectTeamMembersForChange(context, strObjectId, slRole, false);
        slMemberList.remove(context.getUser());
        logger.debug("getLeadChangeManagerOfProgramProj : End");
        return slMemberList;
    }
    // BR002:Rutuja Ekatpure:15/9/2017:End

    // TIGTK-10113:Rutuja Ekatpure:25/9/2017:start
    /****
     * this method used for CO Cancellation Command Access
     * @param context
     * @param args
     * @return boolean
     * @throws Exception
     */
    public boolean showCoCancellationCommand(Context context, String[] args) throws Exception {
        boolean isReturn = false;
        logger.debug("showCoCancellationCommand : Start");
        Map<?, ?> programMap = (Map<?, ?>) JPO.unpackArgs(args);
        String strObjectId = (String) programMap.get("objectId");
        if (UIUtil.isNotNullAndNotEmpty(strObjectId)) {
            DomainObject domCRObj = DomainObject.newInstance(context, strObjectId);
            String strCurrent = domCRObj.getInfo(context, DomainConstants.SELECT_CURRENT);
            StringList slRole = new StringList();
            slRole.add(TigerConstants.ROLE_PSS_CHANGE_COORDINATOR);
            pss.ecm.ui.CommonUtil_mxJPO commonObj = new pss.ecm.ui.CommonUtil_mxJPO(context, args);
            StringList slCMUser = commonObj.getProgramProjectTeamMembersForChange(context, strObjectId, slRole, false);
            if (slCMUser.contains(context.getUser()) && TigerConstants.STATE_PSS_CR_INPROCESS.equals(strCurrent)) {
                isReturn = true;
            }
        }
        logger.debug("showCoCancellationCommand : End");
        return isReturn;
    }

    /****
     * this method used for CR Edit Command Access
     * @param context
     * @param args
     * @return boolean
     * @throws Exception
     */
    public boolean checkEditCRCommand(Context context, String[] args) throws Exception {
        logger.debug("checkEditCRCommand : START");
        boolean isAccess = false;
        try {
            Map<?, ?> programMap = (Map<?, ?>) JPO.unpackArgs(args);
            String strObjectId = (String) programMap.get("objectId");
            if (UIUtil.isNotNullAndNotEmpty(strObjectId)) {
                DomainObject domCRObj = DomainObject.newInstance(context, strObjectId);
                StringList slCRInfo = new StringList(3);
                slCRInfo.add(DomainConstants.SELECT_CURRENT);
                slCRInfo.add(DomainConstants.SELECT_OWNER);
                // take information about CR
                Map<String, Object> mCRinfo = domCRObj.getInfo(context, slCRInfo);
                String strCRCurrent = (String) mCRinfo.get(DomainConstants.SELECT_CURRENT);
                String strCROwner = (String) mCRinfo.get(DomainConstants.SELECT_OWNER);
                String strUserName = context.getUser();
                String strLoggedUserSecurityContext = PersonUtil.getDefaultSecurityContext(context, strUserName);

                if (UIUtil.isNotNullAndNotEmpty(strLoggedUserSecurityContext)) {
                    String strLoggerUserRole = (strLoggedUserSecurityContext.split("[.]")[0]);

                    StringList slState = new StringList();
                    slState.add(TigerConstants.STATE_PSS_CR_CREATE);
                    slState.add(TigerConstants.STATE_SUBMIT_CR);
                    StringList slRole = new StringList();
                    slRole.add(TigerConstants.ROLE_PSS_CHANGE_COORDINATOR);
                    slRole.add(TigerConstants.ROLE_PSS_PROGRAM_MANAGER);
                    pss.ecm.ui.CommonUtil_mxJPO commonObj = new pss.ecm.ui.CommonUtil_mxJPO(context, args);
                    StringList slCMUser = commonObj.getProgramProjectTeamMembersForChange(context, strObjectId, slRole, false);
                    if (slState.contains(strCRCurrent) && (strUserName.equals(strCROwner) || (slCMUser.contains(strUserName) && slRole.contains(strLoggerUserRole)))) {
                        isAccess = true;
                    }
                }
            }

        } catch (Exception ex) {
            logger.error("PSS_enoECMChangeRequest : checkEditCRCommand : ERROR : ", ex);
            throw ex;
        }
        logger.debug("checkEditCRCommand : END");
        return isAccess;
    }
    // TIGTK-10113:Rutuja Ekatpure:25/9/2017:End

    /**
     * This Method is used to check if the Program Project on CR and CO is same or not.If not, it blocks the further promotion of CR CO Objects.
     * @param context
     * @param args
     * @return void
     * @throws Exception
     * @since 10-04-2017
     * @author pmantri - TIGTK-10261
     */
    public int checkForSameProjectForCRCO(Context context, String args[]) throws Exception {
        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }
        String strMessage = DomainConstants.EMPTY_STRING;
        int retValue = 0;
        logger.debug("PSS_enoECMChangeRequest : checkForSameProjectForCRCO : START");
        try {
            String objectId = args[0];
            String objectType = args[1];
            DomainObject domChange = new DomainObject(objectId);
            StringList slSelactables = new StringList();
            slSelactables.add("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
            String strChangeProgramProjectId = (String) domChange.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
            String strSelect = DomainConstants.EMPTY_STRING;
            if (objectType.equalsIgnoreCase(TigerConstants.TYPE_PSS_CHANGEREQUEST)) {
                strSelect = "from[" + ChangeConstants.RELATIONSHIP_CHANGE_ORDER + "].to.id";
            } else {
                strSelect = "to[" + ChangeConstants.RELATIONSHIP_CHANGE_ORDER + "].from.id";
            }
            StringList slConnectedChangeObjects = domChange.getInfoList(context, strSelect);

            if (!slConnectedChangeObjects.isEmpty()) {
                Iterator itrChange = slConnectedChangeObjects.iterator();
                while (itrChange.hasNext()) {
                    String strConnectedChangeID = (String) itrChange.next();
                    DomainObject domConnectedChangeObject = DomainObject.newInstance(context, strConnectedChangeID);
                    String strConnectedChangeProgramProjectId = (String) domConnectedChangeObject.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
                    if (!strChangeProgramProjectId.equalsIgnoreCase(strConnectedChangeProgramProjectId)) {
                        retValue = 1;
                    }
                }

            }

            if (retValue == 1) {
                strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSS_EnterpriseChangeMgt.Alert.CRCOMismatch");
                MqlUtil.mqlCommand(context, "notice $1", strMessage);
            }
            logger.debug("PSS_enoECMChangeRequest : checkForSameProjectForCRCO : END");
        } catch (Exception ex) {
            logger.error("Error in checkForSameProjectForCRCO: ", ex);
            throw ex;
        }
        return retValue;
    }

    // TIGTK-10420 : TS : 11/10/2017: Start
    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author TS This method will be used to define access on web-form field (COToRelease)
     */
    public boolean checkAccessToCOToReleaseFieldCreate(Context context, String[] args) throws Exception {
        boolean bShow = true;
        try {
            String strUserName = context.getUser();
            String strLoggedUserSecurityContext = PersonUtil.getDefaultSecurityContext(context, strUserName);
            String assignedRoles = (strLoggedUserSecurityContext.split("[.]")[0]);
            if (assignedRoles.equalsIgnoreCase(TigerConstants.ROLE_PSS_GLOBAL_ADMINISTRATOR) || assignedRoles.equalsIgnoreCase(TigerConstants.ROLE_PSS_PROGRAM_MANAGER)
                    || assignedRoles.equalsIgnoreCase(TigerConstants.ROLE_PSS_CHANGE_COORDINATOR) || assignedRoles.equalsIgnoreCase(TigerConstants.ROLE_PSS_PLM_SUPPORT_TEAM)) {
                bShow = false;
            }

        } catch (Exception e) {
            logger.error("Error in checkAccessToCOToReleaseFieldCreate: ", e);
            throw e;
        }
        return bShow;
    }

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author TS This method will be used to define access on web-form field (COToRelease)
     */
    public boolean checkAccessToCOToReleaseFieldSubmit(Context context, String[] args) throws Exception {
        boolean bShow = false;
        try {
            String strUserName = context.getUser();
            String strLoggedUserSecurityContext = PersonUtil.getDefaultSecurityContext(context, strUserName);
            String assignedRoles = (strLoggedUserSecurityContext.split("[.]")[0]);

            if (assignedRoles.equalsIgnoreCase(TigerConstants.ROLE_PSS_GLOBAL_ADMINISTRATOR) || assignedRoles.equalsIgnoreCase(TigerConstants.ROLE_PSS_PROGRAM_MANAGER)
                    || assignedRoles.equalsIgnoreCase(TigerConstants.ROLE_PSS_CHANGE_COORDINATOR) || assignedRoles.equalsIgnoreCase(TigerConstants.ROLE_PSS_PLM_SUPPORT_TEAM)) {
                bShow = true;
            }

        } catch (Exception e) {
            logger.error("Error in checkAccessToCOToReleaseFieldSubmit: ", e);
            throw e;
        }
        return bShow;
    }
    // TIGTK-10420 : TS : 11/10/2017: End

    // PCM TIGTK-11326 :15/11/2017 :End
    public boolean isUserMemberOfProgProject(Context context, String[] args) throws Exception {
        boolean isPPMember = false;
        try {

            Map<?, ?> programMap = (Map<?, ?>) JPO.unpackArgs(args);
            String strProgramProjectId = (String) programMap.get("objectId");

            DomainObject domProgProj = DomainObject.newInstance(context, strProgramProjectId);
            // get list of members connected to program project
            StringList strProgramProjectMembers = domProgProj.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS + "].to.name");

            if (strProgramProjectMembers.contains(context.getUser())) {
                isPPMember = true;
            }

        } catch (Exception ex) {
            logger.error("Error in isUserMemberOfProgProject: ", ex);
            throw ex;
        }
        return isPPMember;
    }
    // PCM TIGTK-11326 :15/11/2017 :End

    // TIGTK-10768 : TS : 16/11/2017 : Start
    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author TS This method will be used to define access on CA Edit command
     */

    public boolean checkEditAccessForCA(Context context, String[] args) throws Exception {
        boolean bShow = false;
        logger.debug("PSS_enoECMChangeRequest : checkEditAccessForCA : START");
        try {
            HashMap<?, ?> programMap = JPO.unpackArgs(args);
            String objectId = (String) programMap.get("objectId");
            DomainObject domChange = DomainObject.newInstance(context, objectId);

            // TIGTK-15588:29-06-2018:STARTS
            StringList slObjectSelects = new StringList();
            slObjectSelects.addElement(DomainConstants.SELECT_OWNER);
            slObjectSelects.addElement(DomainConstants.SELECT_CURRENT);
            slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_CHANGEACTION + "]." + DomainConstants.SELECT_FROM_ID);
            slObjectSelects.addElement("from[" + TigerConstants.RELATIONSHIP_TECHNICALASSIGNEE + "]." + DomainConstants.SELECT_TO_NAME);

            Map mpCAInfoMap = domChange.getInfo(context, slObjectSelects);

            String strOwner = (String) mpCAInfoMap.get(DomainConstants.SELECT_OWNER);
            String strCurrent = (String) mpCAInfoMap.get(DomainConstants.SELECT_CURRENT);
            String strCOobjId = (String) mpCAInfoMap.get("to[" + TigerConstants.RELATIONSHIP_CHANGEACTION + "]." + DomainConstants.SELECT_FROM_ID);
            String strCATechnicalAssignee = (String) mpCAInfoMap.get("from[" + TigerConstants.RELATIONSHIP_TECHNICALASSIGNEE + "]." + DomainConstants.SELECT_TO_NAME);
            // TIGTK-15588:29-06-2018:ENDS

            String strLoggedUser = context.getUser();

            StringList slRolesList = new StringList();
            slRolesList.addElement(TigerConstants.ROLE_PSS_PRODUCT_DEVELOPMENT_LEAD);

            pss.ecm.ui.CommonUtil_mxJPO commonObj = new pss.ecm.ui.CommonUtil_mxJPO(context, args);
            StringList slToCAImplementedList = commonObj.getProgramProjectTeamMembersForChange(context, strCOobjId, slRolesList, true);
            // TIGTK-11600 : TS : 24/11/2017: Start

            // TIGTK-15588:29-06-2018:STARTS
            StringList slLoggedInUserAssignedRoles = PersonUtil.getSCUserRoles(context);
            if (!slLoggedInUserAssignedRoles.contains(TigerConstants.ROLE_PSS_GLOBAL_ADMINISTRATOR) && !slLoggedInUserAssignedRoles.contains(TigerConstants.ROLE_PSS_PLM_SUPPORT_TEAM)) {
                // TIGTK-15588:29-06-2018:ENDS
                if ((TigerConstants.STATE_CHANGEACTION_INWORK.equalsIgnoreCase(strCurrent) && slToCAImplementedList.contains(strLoggedUser))) {
                    bShow = true;
                }
                if (!bShow) {
                    if ((TigerConstants.STATE_CHANGEACTION_PENDING.equalsIgnoreCase(strCurrent) || TigerConstants.STATE_CHANGEACTION_INWORK.equalsIgnoreCase(strCurrent)
                            || TigerConstants.STATE_CHANGEACTION_ONHOLD.equalsIgnoreCase(strCurrent))
                            && (strLoggedUser.equalsIgnoreCase(strOwner) || strLoggedUser.equalsIgnoreCase(strCATechnicalAssignee))) {
                        bShow = true;
                    }
                }
                // TIGTK-15588:29-06-2018:STARTS
            }
            // TIGTK-15588:29-06-2018:ENDS
            // TIGTK-11600 : TS : 24/11/2017: End
            logger.debug("PSS_enoECMChangeRequest : checkEditAccessForCA : END");
        }

        catch (Exception e) {
            logger.error("Error in checkAccessToEditCA: ", e);
            throw e;
        }
        return bShow;
    }
    // TIGTK-10768 : TS : 16/11/2017: End

    // TIGTK-11675 : 27/11/17 : TS : START
    public String getRouteTemplateTypeValueForChange(String strPurposeOfRelease) throws Exception {
        String strAttrValueToSet = DomainObject.EMPTY_STRING;
        if (strPurposeOfRelease.equals("Acquisition")) {
            strAttrValueToSet = "Approval List for Acquisition";
        } else if (strPurposeOfRelease.equals("Commercial Update")) {
            strAttrValueToSet = "Approval List for Commercial update";
        } else if (strPurposeOfRelease.equals("Prototype Tool Launch/Modification")) {
            strAttrValueToSet = "Approval List for Prototype";
        } else if (strPurposeOfRelease.equals("Serial Tool Launch/Modification")) {
            strAttrValueToSet = "Approval List for Serial Launch";
        } else if (strPurposeOfRelease.equals("Design study")) {
            strAttrValueToSet = "Approval List for Design study";
        } else if (strPurposeOfRelease.equals("Other")) {
            strAttrValueToSet = "Approval List for Other Parts";
        }
        return strAttrValueToSet;
    }
    // TIGTK-11675 : 27/11/17 : TS : END

    /**
     * @param context
     * @param args
     * @return TIGTK-12036
     * @throws Exception
     * @author TS This method will be used on Purpose of Release Attribute to check LA is present or not
     */

    public int checkForCRPurposeOfRealese(Context context, String[] args) throws Exception {
        logger.debug("PSS_enoECMChangeRequest:checkForCRPurposeOfRealese:START");
        int intResult = 0;
        int flag = 0;
        try {
            String strCRObjId = args[0]; // Change Request Object Id
            String strPurposeOfRelease = args[1]; // Change Object Id

            if (UIUtil.isNotNullAndNotEmpty(strCRObjId)) {
                DomainObject domCRObject = DomainObject.newInstance(context, strCRObjId);
                String strChangeObjType = domCRObject.getInfo(context, DomainConstants.SELECT_TYPE);
                if (!TigerConstants.TYPE_PSS_CHANGEREQUEST.equalsIgnoreCase(strChangeObjType)) {
                    flag = 1;
                }

                if (flag == 0) {
                    // Get Program-Project Id
                    String strProgProjId = domCRObject.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
                    if (UIUtil.isNotNullAndNotEmpty(strProgProjId)) {
                        DomainObject domProgProj = DomainObject.newInstance(context, strProgProjId);
                        String strProgProjName = domCRObject.getInfo(context, DomainConstants.SELECT_NAME);
                        // Define Object Select
                        StringList objSelect = new StringList();
                        objSelect.add(DomainConstants.SELECT_ID);
                        // Define Relationship Select
                        StringList relSelects = new StringList();
                        relSelects.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE + "]");

                        String relWhereClause = "attribute[" + TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE + "]== \"" + strPurposeOfRelease + "\"";

                        MapList mlListOfAssessor = domProgProj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_CONNECTEDASSESSORS, // relationship pattern
                                TigerConstants.TYPE_PSS_LISTOFASSESSORS, // object pattern
                                new StringList(DomainObject.SELECT_ID), // object selects
                                relSelects, // relationship selects
                                false, // to direction
                                true, // from direction;
                                (short) 1, // recursion level
                                null, // object where clause
                                relWhereClause, (short) 0, false, // checkHidden
                                true, // preventDuplicates
                                (short) 1000, // pageSize
                                null, null, null, null, null);

                        if (mlListOfAssessor.isEmpty()) {
                            intResult = 1;
                            String strAlertMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                                    "PSS_EnterpriseChangeMgt.Alert.ListOfAssessorsNotFoundForPurposeOfRelease");
                            strAlertMessage = strAlertMessage.replace("{ProgramProjectName}", strProgProjName);
                            strAlertMessage = strAlertMessage.replace("{PurposeOfRelease}", strPurposeOfRelease);
                            MqlUtil.mqlCommand(context, "notice $1", strAlertMessage);
                        }
                    }
                }
            }
            logger.debug("PSS_enoECMChangeRequest:checkForCRPurposeOfRealese:END");
        } catch (RuntimeException ex) {
            logger.error("Error in PSS_enoECMChangeRequest:checkForCRPurposeOfRealese:ERROR ", ex);
            throw ex;
        } catch (Exception ex) {
            logger.error("Error in PSS_enoECMChangeRequest:checkForCRPurposeOfRealese:END:ERROR ", ex);
            throw ex;
        }
        return intResult;
    }

    /**
     * @param This
     *            method is used to set IA owner on Promotion of CR from Submit to Evaluate.
     * @param args
     * @return int
     * @throws Exception
     */
    public int setIAOwner(Context context, String[] args) throws Exception {
        logger.debug("PSS_enoECMChangeRequest:setIAOwner:Start");
        int intResult = 0;
        try {
            String strCRObjId = args[0]; // Change Request Object Id
            if (UIUtil.isNotNullAndNotEmpty(strCRObjId)) {
                DomainObject domCR = DomainObject.newInstance(context, strCRObjId);
                String strContextUser = context.getUser();
                StringList slObjectSle = new StringList();
                slObjectSle.addElement(DomainConstants.SELECT_ID);
                String strwhere = "revision == last";

                MapList mlIA = domCR.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS, TigerConstants.TYPE_PSS_IMPACTANALYSIS, slObjectSle, DomainConstants.EMPTY_STRINGLIST,
                        false, true, (short) 1, strwhere, null, 0);
                if (!mlIA.isEmpty()) {
                    Map<?, ?> mIA = (Map<?, ?>) mlIA.get(0);
                    String sImpactAnalysisID = (String) mIA.get(DomainConstants.SELECT_ID);
                    DomainObject domIAObject = new DomainObject(sImpactAnalysisID);
                    domIAObject.setOwner(context, strContextUser);
                }
            }

        } catch (Exception ex) {
            logger.error("Error in PSS_enoECMChangeRequest:setIAOwner:ERROR ", ex);
            throw ex;
        }
        return intResult;
    }

    /**
     * This method is used for post process for Editing Change Request.
     * @param context
     * @param args
     * @throws Exception
     */
    public void postProcessForChangeRequestEdit(Context context, String args[]) throws Exception {
        logger.debug("PSS_enoECMChangeRequest:postProcessForChangeRequestEdit:Start");
        try {
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            HashMap<?, ?> requestMap = (HashMap<?, ?>) programMap.get("requestMap");

            checkParallelTrackOfChangeRequest(context, args);

            // Get the objectId of Change Request
            String strObjectID = (String) requestMap.get("objectId");
            DomainObject domCR = new DomainObject(strObjectID);
            String strCRCurrent = domCR.getInfo(context, DomainConstants.SELECT_CURRENT);
            String strOldCMVal = (String) requestMap.get("ChangeManagerfieldValue");
            String strNewCMVal = (String) requestMap.get("ChangeManager");

            if (UIUtil.isNotNullAndNotEmpty(strOldCMVal) && UIUtil.isNotNullAndNotEmpty(strNewCMVal) && TigerConstants.STATE_SUBMIT_CR.equalsIgnoreCase(strCRCurrent)) {
                if (!strOldCMVal.equals(strNewCMVal)) {
                    StringList slObjectSle = new StringList();
                    slObjectSle.addElement(DomainConstants.SELECT_ID);
                    String strwhere = "revision == last";

                    MapList mlIA = domCR.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS, TigerConstants.TYPE_PSS_IMPACTANALYSIS, slObjectSle,
                            DomainConstants.EMPTY_STRINGLIST, false, true, (short) 1, strwhere, null, 0);
                    if (!mlIA.isEmpty()) {
                        Map<?, ?> mIA = (Map<?, ?>) mlIA.get(0);
                        String sImpactAnalysisID = (String) mIA.get(DomainConstants.SELECT_ID);
                        DomainObject domIAObject = new DomainObject(sImpactAnalysisID);
                        domIAObject.setOwner(context, strNewCMVal);
                    }
                    String strCRConnectedRouteID = (String) domCR.getInfo(context, "from[" + DomainConstants.RELATIONSHIP_OBJECT_ROUTE + "].to.id");
                    DomainObject domObjRoute = DomainObject.newInstance(context, strCRConnectedRouteID);
                    boolean isContextPushed = false;
                    ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, TigerConstants.PERSON_USER_AGENT), "", "");
                    isContextPushed = true;
                    domObjRoute.setOwner(context, strNewCMVal);
                    if (isContextPushed) {
                        ContextUtil.popContext(context);
                        isContextPushed = false;
                    }

                }
            }
            // TIGTK-13112 : PSE : START
            String strProgProjId = (String) requestMap.get("ProjectCodeOID");
            if (UIUtil.isNotNullAndNotEmpty(strProgProjId)) {
                updateCSOfChangeObject(context, strProgProjId, strObjectID);
            }
            // TIGTK-13112 : PSE : END

            logger.debug("PSS_enoECMChangeRequest:postProcessForChangeRequestEdit:END");
        } catch (Exception ex) {
            logger.error("Error in PSS_enoECMChangeRequest:postProcessForChangeRequestEdit:ERROR", ex);
            throw ex;
        }
    }

    /**
     * This method is used to update Deligate Assignee on promotion of CR from Submit to Evaluate TIGTK-12403
     * @param context
     * @param args
     * @throws Exception
     */
    public void updateDeligatAssignee(Context context, String[] args) throws Exception {
        logger.debug("PSS_enoECMChangeRequest:updateDeligatAssignee:Start");
        boolean flag = false;
        try {
            String strCRObjId = args[0]; // Change Request Object Id
            if (UIUtil.isNotNullAndNotEmpty(strCRObjId)) {
                DomainObject domCR = DomainObject.newInstance(context, strCRObjId);
                StringList slObjectSle = new StringList();
                slObjectSle.addElement(DomainConstants.SELECT_ID);
                String strwhere = "revision == last";

                MapList mlIA = domCR.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS, TigerConstants.TYPE_PSS_IMPACTANALYSIS, slObjectSle, DomainConstants.EMPTY_STRINGLIST,
                        false, true, (short) 1, strwhere, null, 0);
                Iterator<Map<?, ?>> itrRA = mlIA.iterator();
                while (itrRA.hasNext()) {
                    Map<?, ?> mIA = itrRA.next();
                    String sImpactAnalysisID = (String) mIA.get(DomainConstants.SELECT_ID);
                    DomainObject domIAObject = new DomainObject(sImpactAnalysisID);

                    StringList slRAobjSelect = new StringList();
                    slRAobjSelect.add(DomainConstants.SELECT_ID);

                    MapList mplRAObjs = domIAObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT, TigerConstants.TYPE_PSS_ROLEASSESSMENT, slRAobjSelect, null, false, true,
                            (short) 1, null, null, 0);
                    for (Object objRA : mplRAObjs) {
                        Map<?, ?> mpRA = (Map<?, ?>) objRA;
                        String strRAID = (String) mpRA.get(DomainConstants.SELECT_ID);
                        DomainObject domRA = DomainObject.newInstance(context, strRAID);

                        String strRADueDate = DomainConstants.EMPTY_STRING;
                        String strRAOwner = DomainConstants.EMPTY_STRING;
                        String strRARole = DomainConstants.EMPTY_STRING;

                        StringList slSelectList = new StringList();
                        slSelectList.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT + "].attribute[" + TigerConstants.ATTRIBUTE_PSS_DUEDATE + "]");
                        slSelectList.addElement(DomainConstants.SELECT_OWNER);
                        slSelectList.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_RAROLE + "].value");

                        Map<?, ?> tempmap = domRA.getInfo(context, slSelectList);

                        if (!tempmap.isEmpty()) {
                            strRADueDate = (String) tempmap.get("to[" + TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT + "].attribute[" + TigerConstants.ATTRIBUTE_PSS_DUEDATE + "]");
                            strRAOwner = (String) tempmap.get(DomainConstants.SELECT_OWNER);
                            strRARole = (String) tempmap.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_RAROLE + "].value");
                        }

                        String strMQLCommand = "print bus Person " + strRAOwner + " - select id dump";
                        String strRAOwnerId = MqlUtil.mqlCommand(context, true, strMQLCommand, true);

                        StringList slRolesList = new StringList();
                        slRolesList.addElement(strRARole);

                        pss.ecm.ui.CommonUtil_mxJPO commonObj = new pss.ecm.ui.CommonUtil_mxJPO(context, args);
                        StringList slUserList = commonObj.getProgramProjectTeamMembersForChange(context, strCRObjId, slRolesList, false);

                        // Call this method to get the delegated user for RA Owner
                        String strAbsenceDelegate = getDelegatedAssigneeForRAOwner(context, strRAOwnerId);

                        String strDelegate = DomainConstants.EMPTY_STRING;
                        if (UIUtil.isNotNullAndNotEmpty(strAbsenceDelegate)) {
                            if (slUserList.contains(strAbsenceDelegate)) {
                                flag = true;
                                strDelegate = strAbsenceDelegate;
                            } else {
                                StringList slPMRoleList = new StringList();
                                slPMRoleList.add(TigerConstants.ROLE_PSS_PROGRAM_MANAGER);
                                StringList slPMList = commonObj.getProgramProjectTeamMembersForChange(context, strCRObjId, slPMRoleList, true);

                                String strPM = (String) slPMList.get(0);
                                strMQLCommand = "print bus Person " + strPM + " - select id dump";
                                String strPMId = MqlUtil.mqlCommand(context, true, strMQLCommand, true);
                                // Call this method to get the delegated user for RA Owner
                                String strPMAbsenceDelegate = getDelegatedAssigneeForRAOwner(context, strPMId);

                                if (UIUtil.isNotNullAndNotEmpty(strPMAbsenceDelegate)) {
                                    flag = true;
                                    strDelegate = strPMAbsenceDelegate;
                                } else {
                                    flag = true;
                                    strDelegate = strPM;
                                }
                            }
                        }

                        if (flag == true) {
                            PropertyUtil.setRPEValue(context, "PSS_Delegate", "true", false);
                            domRA.setOwner(context, strDelegate);
                            // Call the method to update RAE Owners.
                            updateRAEOwners(context, strRAID, strDelegate);
                            // Mail goes new RA Owner
                            generateRADelegationMail(context, strCRObjId, sImpactAnalysisID, strRAID, strRADueDate, strRAOwner, strDelegate);
                        }
                    }
                }
            }
            logger.debug("PSS_enoECMChangeRequest:updateDeligatAssignee:End");
        } catch (Exception ex) {
            logger.error("Error in PSS_enoECMChangeRequest:updateDeligatAssignee:ERROR ", ex);
            throw ex;
        }
    }

    /**
     * This method is used to update Deligate Assignee on promotion of CR from Submit to Evaluate TIGTK-12403
     * @param context
     * @param args
     * @throws Exception
     */
    public String getDelegatedAssigneeForRAOwner(Context context, String strRAOwnerId) throws Exception {
        logger.debug("PSS_enoECMChangeRequest:checkDelegate:Start");
        String strDelegate = DomainConstants.EMPTY_STRING;
        try {
            final String SELECT_ATTRIBUTE_ABSENCE_DELEGATE = "attribute[" + TigerConstants.ATTRIBUTE_ABSENCEDELEGATE + "]";
            final String SELECT_ATTRIBUTE_ABSENCE_END_DATE = "attribute[" + TigerConstants.ATTRIBUTE_ABSENCEENDDATE + "]";
            final String SELECT_ATTRIBUTE_ABSENCE_START_DATE = "attribute[" + TigerConstants.ATTRIBUTE_ABSENCESTARTDATE + "]";

            SimpleDateFormat dateFormat = new SimpleDateFormat(eMatrixDateFormat.getInputDateFormat(), context.getLocale());
            StringList slBusSelect = new StringList();
            slBusSelect.addElement(SELECT_ATTRIBUTE_ABSENCE_DELEGATE);
            slBusSelect.addElement(SELECT_ATTRIBUTE_ABSENCE_START_DATE);
            slBusSelect.addElement(SELECT_ATTRIBUTE_ABSENCE_END_DATE);

            Vector<String> vecAlreadyVisitedNewAssignees = new Vector();

            while (!vecAlreadyVisitedNewAssignees.contains(strRAOwnerId)) {
                DomainObject domNewAssignee = DomainObject.newInstance(context, strRAOwnerId);

                Map mapNewAssigneeInfo = domNewAssignee.getInfo(context, slBusSelect);

                String strAbsenceDelegate = (String) mapNewAssigneeInfo.get(SELECT_ATTRIBUTE_ABSENCE_DELEGATE);
                String strAbsenceStartDate = (String) mapNewAssigneeInfo.get(SELECT_ATTRIBUTE_ABSENCE_START_DATE);
                String strAbsenceEndDate = (String) mapNewAssigneeInfo.get(SELECT_ATTRIBUTE_ABSENCE_END_DATE);

                if (UIUtil.isNotNullAndNotEmpty(strAbsenceDelegate) && UIUtil.isNotNullAndNotEmpty(strAbsenceStartDate) && UIUtil.isNotNullAndNotEmpty(strAbsenceEndDate)) {
                    // Is the new user absent?
                    Date dtAbsenceStart = dateFormat.parse(strAbsenceStartDate);
                    Date dtAbsenceEnd = dateFormat.parse(strAbsenceEndDate);
                    Date dtToday = new Date();

                    if (dtToday.after(dtAbsenceStart) && dtToday.before(dtAbsenceEnd)) {
                        vecAlreadyVisitedNewAssignees.add(strRAOwnerId);
                        strRAOwnerId = PersonUtil.getPersonObjectID(context, strAbsenceDelegate);
                        strDelegate = strAbsenceDelegate;
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }

            if (vecAlreadyVisitedNewAssignees.contains(strRAOwnerId)) {
                throw new Exception(ComponentsUtil.i18nStringNow("emxComponents.LifeCycle.CircularReferenceFound", context.getLocale().getLanguage()));
            }
            vecAlreadyVisitedNewAssignees.clear();

            logger.debug("PSS_enoECMChangeRequest:checkDelegate:End");
        } catch (Exception e) {
            logger.error("Error in PSS_enoECMChangeRequest:checkDelegate:ERROR ", e);
            throw e;
        }
        return strDelegate;
    }

    // updatedDelegatedRAOwner
    /**
     * This method is used to update Deligate Assignee when we modify owner TIGTK-12403
     * @param context
     * @param args
     * @throws Exception
     */
    public void updateRADeligatAssignee(Context context, String[] args) throws Exception {
        logger.debug("PSS_enoECMChangeRequest:updateRADeligatAssignee:Start");
        boolean flag = false;
        try {
            String strRAObjId = args[0]; // Role Assessment Object Id
            if (UIUtil.isNotNullAndNotEmpty(strRAObjId)) {
                String strDelegate = DomainConstants.EMPTY_STRING;
                DomainObject domRA = DomainObject.newInstance(context, strRAObjId);

                // Get RA Details
                String strIAId = DomainConstants.EMPTY_STRING;
                String strRADueDate = DomainConstants.EMPTY_STRING;
                String strRAOwner = DomainConstants.EMPTY_STRING;
                String strRARole = DomainConstants.EMPTY_STRING;

                StringList slSelectList = new StringList();
                slSelectList.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT + "].attribute[" + TigerConstants.ATTRIBUTE_PSS_DUEDATE + "]");
                slSelectList.addElement(DomainConstants.SELECT_OWNER);
                slSelectList.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_RAROLE + "].value");
                slSelectList.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT + "].from.id");

                Map<?, ?> tempmap = domRA.getInfo(context, slSelectList);

                if (!tempmap.isEmpty()) {
                    strRADueDate = (String) tempmap.get("to[" + TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT + "].attribute[" + TigerConstants.ATTRIBUTE_PSS_DUEDATE + "]");
                    strRAOwner = (String) tempmap.get(DomainConstants.SELECT_OWNER);
                    strRARole = (String) tempmap.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_RAROLE + "].value");
                    strIAId = (String) tempmap.get("to[" + TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT + "].from.id");
                }
                if (UIUtil.isNotNullAndNotEmpty(strIAId)) {

                    DomainObject domIA = DomainObject.newInstance(context, strIAId);
                    String strCRObjId = domIA.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.id");
                    DomainObject domCR = DomainObject.newInstance(context, strCRObjId);
                    String strCRState = domCR.getInfo(context, DomainConstants.SELECT_CURRENT);

                    if (TigerConstants.STATE_EVALUATE.equals(strCRState)) {

                        String strMQLCommand = "print bus Person " + strRAOwner + " - select id dump";
                        String strRAOwnerId = MqlUtil.mqlCommand(context, true, strMQLCommand, true);

                        StringList slRolesList = new StringList();
                        slRolesList.addElement(strRARole);

                        // Check Member present in Program-Project
                        pss.ecm.ui.CommonUtil_mxJPO commonObj = new pss.ecm.ui.CommonUtil_mxJPO(context, args);
                        StringList slUserList = commonObj.getProgramProjectTeamMembersForChange(context, strCRObjId, slRolesList, false);

                        // Get Absence Delegate
                        String strAbsenceDelegate = getDelegatedAssigneeForRAOwner(context, strRAOwnerId);

                        if (UIUtil.isNotNullAndNotEmpty(strAbsenceDelegate)) {
                            if (slUserList.contains(strAbsenceDelegate)) {
                                flag = true;
                                strDelegate = strAbsenceDelegate;
                            } else {
                                StringList slPMRoleList = new StringList();
                                slPMRoleList.add(TigerConstants.ROLE_PSS_PROGRAM_MANAGER);
                                StringList slPMList = commonObj.getProgramProjectTeamMembersForChange(context, strCRObjId, slPMRoleList, true);
                                String strPM = (String) slPMList.get(0);
                                strMQLCommand = "print bus Person " + strPM + " - select id dump";
                                String strPMId = MqlUtil.mqlCommand(context, true, strMQLCommand, true);
                                String strPMAbsenceDelegate = getDelegatedAssigneeForRAOwner(context, strPMId);

                                if (UIUtil.isNotNullAndNotEmpty(strPMAbsenceDelegate)) {
                                    flag = true;
                                    strDelegate = strPMAbsenceDelegate;
                                } else {
                                    flag = true;
                                    strDelegate = strPM;
                                }
                            }
                        }
                        // If Absent Delegate present then set RA owner
                        if (flag == true) {
                            domRA.setOwner(context, strDelegate);
                            // Call the method to update RAE Owners.
                            updateRAEOwners(context, strRAObjId, strDelegate);
                            // Mail goes new RA Owner
                            generateRADelegationMail(context, strCRObjId, strIAId, strRAObjId, strRADueDate, strRAOwner, strDelegate);
                        }
                    }
                }
            }
            logger.debug("PSS_enoECMChangeRequest:updateRADeligatAssignee:End");
        } catch (Exception ex) {
            logger.error("Error in PSS_enoECMChangeRequest:updateRADeligatAssignee:ERROR ", ex);
            throw ex;
        }
    }

    /**
     * This method is used to send mail to the new RA owner TIGTK-12403
     * @param context
     * @param args
     * @throws Exception
     */
    public void generateRADelegationMail(Context context, String strCRObjId, String strIAObjectId, String strRAObjectId, String strRADueDate, String strOriginalAssigneeName,
            String strDelegatedAssigneeName) throws Exception {
        logger.debug("PSS_enoECMChangeRequest:generateRADelegationMail:Start");
        try {
            String strIALink = DomainConstants.EMPTY_STRING;
            String strRALink = DomainConstants.EMPTY_STRING;
            String strCRLink = DomainConstants.EMPTY_STRING;

            StringBuffer sbMessage = new StringBuffer();

            // Read the Properties Keys for Msg Body
            String strLanguage = context.getSession().getLanguage();
            String strMsgRoleAssessmentKey = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", new Locale(strLanguage), "PSS_EnterpriseChangeMgt.Command.RelatedRA");
            String strMsgImpactAnalysisKey = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", new Locale(strLanguage),
                    "PSS_emxEnterpriseChangeMgt.Label.ImpactAnalysisKey");
            String strMsgDelegationKey = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", new Locale(strLanguage), "PSS_emxEnterpriseChangeMgt.Label.DelegationKey");
            String strMsgOriginalAssigneeKey = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", new Locale(strLanguage),
                    "PSS_emxEnterpriseChangeMgt.Label.OriginalAssigneeKey");
            String strMsgAbsentKey = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", new Locale(strLanguage), "PSS_emxEnterpriseChangeMgt.Label.AbsentKey");
            String strMsgAnalyzeImpactKey = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", new Locale(strLanguage),
                    "PSS_emxEnterpriseChangeMgt.Label.AnalyseImpactKey");
            String strMsgRAInstructionsKey = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", new Locale(strLanguage),
                    "PSS_emxEnterpriseChangeMgt.Label.RAInstructions");
            String strMsgRADueDateInformationKey = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", new Locale(strLanguage),
                    "PSS_emxEnterpriseChangeMgt.Label.RADueDateInformation");
            String strMsgRALinkInformationKey = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", new Locale(strLanguage),
                    "PSS_emxEnterpriseChangeMgt.Label.RALinkInformation");
            String strMsgIALinkInformationKey = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", new Locale(strLanguage),
                    "PSS_emxEnterpriseChangeMgt.Label.IALinkInformation");

            // Get RA Information
            DomainObject domRA = DomainObject.newInstance(context, strRAObjectId);
            String strRAName = domRA.getInfo(context, DomainConstants.SELECT_NAME);

            // Get IA Information
            DomainObject domIA = DomainObject.newInstance(context, strIAObjectId);
            String strIAName = domIA.getInfo(context, DomainConstants.SELECT_NAME);

            // Read Subject of the RA Delegation Mail Notification
            String strSubject = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", new Locale(strLanguage), "PSS_emxEnterpriseChangeMgt.Subject.RADelegation");

            // Create Link for Impact Analysis & Role Assessment Object
            String strBaseURLSubstring = DomainConstants.EMPTY_STRING;
            String strBaseURL = MailUtil.getBaseURL(context);

            if (UIUtil.isNotNullAndNotEmpty(strBaseURL)) {
                int position = strBaseURL.lastIndexOf("/");
                strBaseURLSubstring = strBaseURL.substring(0, position);
                strIALink = strBaseURLSubstring + "/emxTree.jsp?mode=edit&objectId=" + strIAObjectId;
                strRALink = strBaseURLSubstring + "/emxTree.jsp?mode=edit&objectId=" + strRAObjectId;
                strCRLink = strBaseURLSubstring + "/emxTree.jsp?mode=edit&objectId=" + strCRObjId;
            }

            // Create Message Body of the Mail Notification
            sbMessage.append(strMsgRoleAssessmentKey + " ");
            sbMessage.append(strRAName + " ");
            sbMessage.append(strMsgImpactAnalysisKey + " ");
            sbMessage.append(strIAName + " ");
            sbMessage.append(strMsgDelegationKey + " ");
            sbMessage.append(strDelegatedAssigneeName + " ");
            sbMessage.append(strMsgOriginalAssigneeKey + " ");
            sbMessage.append(strOriginalAssigneeName + " ");
            sbMessage.append(strMsgAbsentKey);
            sbMessage.append(strMsgAnalyzeImpactKey + " ");
            sbMessage.append(strCRLink);
            sbMessage.append(System.getProperty("line.separator"));
            sbMessage.append(System.getProperty("line.separator"));
            sbMessage.append(strMsgRAInstructionsKey);
            sbMessage.append(System.getProperty("line.separator"));
            sbMessage.append(System.getProperty("line.separator"));
            sbMessage.append(strMsgRADueDateInformationKey + " ");
            sbMessage.append(strRADueDate);
            sbMessage.append(System.getProperty("line.separator"));
            sbMessage.append(System.getProperty("line.separator"));
            sbMessage.append(strMsgRALinkInformationKey + " ");
            sbMessage.append(strRALink);
            sbMessage.append(System.getProperty("line.separator"));
            sbMessage.append(System.getProperty("line.separator"));
            sbMessage.append(strMsgIALinkInformationKey + " ");
            sbMessage.append(strIALink);

            // Send Mail Notification
            if (UIUtil.isNotNullAndNotEmpty(strDelegatedAssigneeName)) {
                MailUtil.sendNotification(context, new StringList(strDelegatedAssigneeName), // toList
                        null, // ccList
                        null, // bccList
                        strSubject, // subjectKey
                        null, // subjectKeys
                        null, // subjectValues
                        sbMessage.toString(), // messageKey
                        null, // messageKeys
                        null, // messageValues
                        null, // objectIdList
                        null); // companyName
            }

            logger.debug("PSS_enoECMChangeRequest:generateRADelegationMail:End");
        } catch (Exception ex) {
            logger.error("Error in PSS_enoECMChangeRequest:generateRADelegationMail:ERROR ", ex);
            throw ex;
        }
    }

    /**
     * This method is used to update RAE Owner TIGTK-12403
     * @param context
     * @param args
     * @throws Exception
     */
    public void updateRAEOwners(Context context, String strRAId, String strDelegate) throws Exception {
        logger.debug("PSS_enoECMChangeRequest:updateRAEOwners:Start");
        try {
            DomainObject domRA = DomainObject.newInstance(context, strRAId);
            StringList slRAEobjSelect = new StringList();
            slRAEobjSelect.add(DomainConstants.SELECT_ID);

            MapList mplRAEvalObjs = domRA.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT_EVALUATION, TigerConstants.TYPE_PSS_ROLEASSESSMENT_EVALUATION, slRAEobjSelect, null,
                    false, true, (short) 1, null, null, 0);
            if (!mplRAEvalObjs.isEmpty()) {
                for (Object objRAEval : mplRAEvalObjs) {
                    Map<String, String> mpRAE = (Map<String, String>) objRAEval;
                    String strRAEId = mpRAE.get(DomainConstants.SELECT_ID);
                    DomainObject domRAE = DomainObject.newInstance(context, strRAEId);
                    domRAE.setOwner(context, strDelegate);
                }
            }

        } catch (Exception ex) {
            logger.error("Error in PSS_enoECMChangeRequest:updateRAEOwners:ERROR ", ex);
            throw ex;
        }
    }

    /**
     * This method is used to check Route Template Connected To Program Project or not on property page of MCO and CO
     * @param context
     * @param args
     * @throws Exception
     */
    public int checkRouteTemplateConnectedToProject(Context context, String args[]) throws Exception {
        String strMessage = DomainConstants.EMPTY_STRING;
        int nResult = 0;

        try {
            String strChangeObjectId = args[0];
            String str_PSS_Purpose_Of_Release = args[1];

            // Domain Object of Change
            if (UIUtil.isNotNullAndNotEmpty(strChangeObjectId)) {
                DomainObject domChangeObject = DomainObject.newInstance(context, strChangeObjectId);

                String strChangeType = domChangeObject.getInfo(context, DomainConstants.SELECT_TYPE);

                if (TigerConstants.TYPE_PSS_CHANGEORDER.equalsIgnoreCase(strChangeType) || TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER.equalsIgnoreCase(strChangeType)) {
                    // Domain Object of Program-Project
                    String strProgramProjectOID = domChangeObject.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
                    if (UIUtil.isNotNullAndNotEmpty(strProgramProjectOID)) {
                        DomainObject domProgramProjectObject = DomainObject.newInstance(context, strProgramProjectOID);
                        String strProgramProjectName = domProgramProjectObject.getInfo(context, DomainConstants.SELECT_NAME);

                        StringList ObjectSelect = new StringList();
                        ObjectSelect.add(DomainConstants.SELECT_NAME);
                        ObjectSelect.add(DomainConstants.SELECT_ID);

                        String strRouteTemplateName = (String) getRouteTemplateTypeValueForChange(str_PSS_Purpose_Of_Release);

                        if (strChangeType.equalsIgnoreCase(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER)) {
                            strRouteTemplateName = strRouteTemplateName + " on MCO";

                        } else if (strChangeType.equalsIgnoreCase(TigerConstants.TYPE_PSS_CHANGEORDER)) {
                            strRouteTemplateName = strRouteTemplateName + " on CO";
                        }

                        String strWhere = "attribute[" + TigerConstants.ATTRIBUTE_PSS_ROUTETEMPLATETYPE + "] == " + "\"" + strRouteTemplateName + "\"";

                        MapList mlRelatedRoutesTemplates = domProgramProjectObject.getRelatedObjects(context, // context
                                TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES, // relationship pattern
                                DomainConstants.TYPE_ROUTE_TEMPLATE, // object pattern
                                ObjectSelect, // object selects
                                DomainConstants.EMPTY_STRINGLIST, // relationship selects
                                false, // to direction
                                true, // from direction
                                (short) 1, // recursion level
                                null, // object where clause
                                strWhere, // relationship where clause
                                (short) 0);

                        if (mlRelatedRoutesTemplates.isEmpty()) {
                            String strError = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                                    "PSS_EnterpriseChangeMgt.Alert.RouteTemplateNotPresentOnProgramProjectOnEdit");
                            strMessage = "Program-Project '" + strProgramProjectName + strError + str_PSS_Purpose_Of_Release + "'.";

                            nResult = 1;
                        }
                    }
                }
            }
        }

        catch (Exception ex) {

            logger.error("Error in checkRouteTemplateConnectedToProject: ", ex);

            throw ex;
        }
        if (nResult == 1) {

            MqlUtil.mqlCommand(context, "notice $1", strMessage);
        }
        return nResult;
    }

    public int checkLinkedData(Context context, DomainObject domCO) throws Exception {
        StringList slObjSel = new StringList(1);
        slObjSel.addElement(DomainConstants.SELECT_ID);
        StringList slRelSle = new StringList(1);
        slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

        Pattern relPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER);
        relPattern.addPattern(TigerConstants.RELATIONSHIP_CHANGEACTION);

        MapList mlCAMCOs = domCO.getRelatedObjects(context, relPattern.getPattern(), DomainConstants.QUERY_WILDCARD, slObjSel, slRelSle, false, true, (short) 1, null, null, 0);
        if (mlCAMCOs != null && !mlCAMCOs.isEmpty()) {

            String strAlertMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                    "PSS_EnterpriseChangeMgt.Alert.NotPossibleToEditProgramProject");

            MqlUtil.mqlCommand(context, "notice $1", strAlertMessage);
            return 1;
        }

        return 0;
    }

    /**
     * This method is used to update the Project(CS) of Change Object as per selected Program Projects Project(CS)
     * @param context
     * @param args,Program
     *            Project Object Id , Change Object Id
     * @throws Exception
     * @Since 07-02-2018 : TIGTK-13112
     * @author psalunke
     */
    public void updateCSOfChangeObject(Context context, String strProgProjId, String strChangeObjectID) throws Exception {
        logger.debug("PSS_enoECMChangeRequest : updateCSOfChangeObject : START");
        try {
            if (UIUtil.isNotNullAndNotEmpty(strProgProjId) && UIUtil.isNotNullAndNotEmpty(strChangeObjectID)) {
                BusinessObject busProgProjObj = new BusinessObject(strProgProjId);
                User usrProgProjProjectName = busProgProjObj.getProjectOwner(context);
                String strProgProjProjectName = usrProgProjProjectName.toString();
                BusinessObject busChangeObj = new BusinessObject(strChangeObjectID);
                User usrChangeProjectName = busChangeObj.getProjectOwner(context);
                String strChangeObjectProjectName = usrChangeProjectName.toString();
                if (!strProgProjProjectName.equalsIgnoreCase(strChangeObjectProjectName)) {
                    MqlUtil.mqlCommand(context, "history off", true, false);
                    boolean isContextPushed = false;
                    ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, TigerConstants.PERSON_USER_AGENT), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                    isContextPushed = true;
                    busChangeObj.open(context);
                    busChangeObj.setProjectOwner(context, strProgProjProjectName);
                    busChangeObj.update(context);
                    busChangeObj.close(context);
                    if (isContextPushed) {
                        ContextUtil.popContext(context);
                        isContextPushed = false;
                    }
                    MqlUtil.mqlCommand(context, "history on", true, false);

                    String strMqlHistory = "modify bus $1 add history $2 comment $3";

                    StringBuffer sbInfo = new StringBuffer();
                    sbInfo.append("project: ");
                    sbInfo.append(strProgProjProjectName);

                    MqlUtil.mqlCommand(context, strMqlHistory, strChangeObjectID, "change", sbInfo.toString() + " was " + strChangeObjectProjectName);

                }
            }
            logger.debug("PSS_enoECMChangeRequest : updateCSOfChangeObject : END");
        } catch (Exception ex) {
            logger.error("Error in PSS_enoECMChangeRequest : updateCSOfChangeObject : ERROR ", ex);
            throw ex;
        }
    }

    /**
     * This method is used to modify the History of the object.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param strObjectId
     *            - The object id.
     * @return strAction - The action to be added in the history.
     * @return strComment - The comment to be added in the history
     * @throws Exception
     *             if the operation fails
     */
    protected void modifyHistory(Context context, String strObjectId, String strAction, String strComment) throws Exception {
        logger.debug("modifyHistory : START");
        StringBuffer sbMqlCommand = new StringBuffer("modify bus ");
        sbMqlCommand.append(strObjectId);
        sbMqlCommand.append(" add history \"");
        sbMqlCommand.append("disconnect ");
        sbMqlCommand.append("\" comment \"");
        sbMqlCommand.append(strAction);

        MqlUtil.mqlCommand(context, sbMqlCommand.toString(), true);
        logger.debug("modifyHistory : END");
    }

    /****
     * this method used for CR Creation Command Access from SLC
     * @param context
     * @param args
     * @return boolean
     * @throws Exception
     */
    public boolean showCRCreationCommandFromSLC(Context context, String[] args) throws Exception {

        try {
            boolean isReturn = false;
            logger.debug("showCRCreationCommandFromSLC : Start");
            Map<?, ?> programMap = (Map<?, ?>) JPO.unpackArgs(args);

            String strObjectId = (String) programMap.get("objectId");
            if (UIUtil.isNotNullAndNotEmpty(strObjectId)) {
                StringList slObjectSelects = new StringList(DomainConstants.SELECT_CURRENT);
                slObjectSelects.addElement("project");

                StringList slStateList = new StringList(TigerConstants.STATE_ACTIVE);
                slStateList.addElement(TigerConstants.STATE_OBSOLETE);
                slStateList.addElement(TigerConstants.STATE_NONAWARDED);

                DomainObject domProject = DomainObject.newInstance(context, strObjectId);

                boolean bSubProject = domProject.hasRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_SUBPROGRAMPROJECT, true);
                Map mPPInfo = domProject.getInfo(context, slObjectSelects);
                String strState = DomainObject.EMPTY_STRING;
                String strProject = DomainObject.EMPTY_STRING;

                if (!mPPInfo.isEmpty()) {
                    strState = (String) mPPInfo.get(DomainConstants.SELECT_CURRENT);
                    strProject = (String) mPPInfo.get("project");
                }

                String strSecurityContext = PersonUtil.getDefaultSecurityContext(context, context.getUser());
                String strCollaborativeSpace = (strSecurityContext.split("[.]")[2]);
                if (strProject.equalsIgnoreCase(strCollaborativeSpace) && !slStateList.contains(strState) && bSubProject == false) {
                    isReturn = true;
                }
            }
            logger.debug("showCRCreationCommandFromSLC : End");
            return isReturn;

        } catch (Exception ex) {
            logger.error("Error in PSS_enoECMChangeRequest : showCRCreationCommandFromSLC : ERROR ", ex);
            throw ex;
        }
    }

    // TIGTK-15588:29-06-2018:STARTS
    /**
     * This method is used to give edit access to Admin roles based on states
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author psalunke
     * @since 29-06-2018
     */

    public boolean checkAdminEditAccessForCA(Context context, String[] args) throws Exception {
        boolean bShow = false;
        logger.debug("\n PSS_enoECMChangeRequest : checkAdminEditAccessForCA : START");
        try {
            HashMap<?, ?> programMap = JPO.unpackArgs(args);
            String strCAObjectId = (String) programMap.get("objectId");
            DomainObject domChangeAction = DomainObject.newInstance(context, strCAObjectId);

            String strCACurrent = (String) domChangeAction.getInfo(context, DomainConstants.SELECT_CURRENT);

            StringList slLoggedInUserAssignedRoles = PersonUtil.getSCUserRoles(context);
            if (slLoggedInUserAssignedRoles.contains(TigerConstants.ROLE_PSS_GLOBAL_ADMINISTRATOR) || slLoggedInUserAssignedRoles.contains(TigerConstants.ROLE_PSS_PLM_SUPPORT_TEAM)) {
                if ((TigerConstants.STATE_CHANGEACTION_PENDING.equalsIgnoreCase(strCACurrent) || TigerConstants.STATE_CHANGEACTION_INWORK.equalsIgnoreCase(strCACurrent)
                        || TigerConstants.STATE_CHANGEACTION_ONHOLD.equalsIgnoreCase(strCACurrent))) {
                    bShow = true;
                }
            }
            logger.debug("\n PSS_enoECMChangeRequest : checkAdminEditAccessForCA : END");
        } catch (Exception e) {
            logger.error("\n Error in PSS_enoECMChangeRequest : checkAdminEditAccessForCA : ", e);
            throw e;
        }
        return bShow;
    }
    // TIGTK-15588:29-06-2018:ENDS

    /**
     * This method for visibilty of Clone CR command
     * @param context
     * @param args
     * @throws Exception
     */
    public Boolean showCloneCRCommandIfContextPersonIsProgProjMember(Context context, String args[]) throws Exception {
        boolean result = false;
        try {
            String currentUser = context.getUser();
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strCRobjectId = (String) programMap.get("objectId");

            DomainObject domChangeRequestObject = new DomainObject(strCRobjectId);
            String strCRProgProjId = domChangeRequestObject.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");

            DomainObject domProgProj = new DomainObject(strCRProgProjId);
            StringList slMember = domProgProj.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS + "].to.name");

            if (slMember.contains(currentUser)) {
                result = true;

            }

        } catch (Exception e) {
            logger.error("Error in showCloneCRCommandIfContextPersonIsProgProjMember: ", e);
        }
        return result;
    }// end of method

}