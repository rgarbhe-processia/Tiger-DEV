/**
 * @issueNotificationUtil_mxJPO
 * @Developed for PCM Phase 1
 * @author Harika Varanasi : SteepGraph
 */
package pss.ecm.notification;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import com.matrixone.apps.common.Issue;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;
import pss.constants.TigerConstants;

public class IssueNotificationUtil_mxJPO {
    // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(IssueNotificationUtil_mxJPO.class);

    // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - END

    public LinkedHashMap<String, String> lhmIssueSelectionStore = new LinkedHashMap<String, String>();

    public LinkedHashMap<String, String> lhmIssueFinalStore = new LinkedHashMap<String, String>();

    /**
     * Constructor.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @throws Exception
     *             if the operation fails
     * @author Harika Varanasi : SteepGraph
     */
    public IssueNotificationUtil_mxJPO(Context context, String[] args) throws Exception {

    }

    /**
     * initializeIssueLinkedHashMap
     * @throws Exception
     *             if the operation fails
     * @author Harika Varanasi : SteepGraph
     */
    public void initializeIssueLinkedHashMap(LinkedHashMap<String, String> lhmIssueSelectionStore, boolean isIssueReject) throws Exception {
        try {

            if (lhmIssueSelectionStore != null && (lhmIssueSelectionStore.isEmpty())) {
                // TS-184 Create and Edit Issue From Global Actions -- TIGTK -6309 : KWagh
                lhmIssueSelectionStore.put("Title", "SectionHeader");
                lhmIssueSelectionStore.put("Subject", "SectionSubject");
                lhmIssueSelectionStore.put("Main_Information", "SectionHeader");
                lhmIssueSelectionStore.put("Project_Code", "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.name");
                lhmIssueSelectionStore.put("Project_Description", "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.description");
                lhmIssueSelectionStore.put("Issue", "name");
                lhmIssueSelectionStore.put("Issue_Description", "attribute[" + TigerConstants.ATTRIBUTE_PSS_ISSUE_DESCRIPTION + "]");
                lhmIssueSelectionStore.put("State", "current");
                lhmIssueSelectionStore.put("PSS_ProgramRiskLevel", "attribute[PSS_ProgramRiskLevel]");
                lhmIssueSelectionStore.put("Scheduled_End_Date", "attribute[Estimated End Date]");
                lhmIssueSelectionStore.put("Issue_Originator", "attribute[Originator]");
                // TIGTK-6418:Rutuja Ekatpure:18/8/2017:Start
                if (isIssueReject) {
                    lhmIssueSelectionStore.put("IssueRejectComments", "IssueRejectComments");
                }
                // TIGTK-6418:Rutuja Ekatpure:18/8/2017:End
                // TIGTK-7580 : START
                lhmIssueSelectionStore.put("Comment", "TransferComments");
                // TIGTK-7580 : END
                lhmIssueSelectionStore.put("Useful_Links", "SectionHeader");
                lhmIssueSelectionStore.put("Reported_Against", "from[Issue].to.name");
                lhmIssueSelectionStore.put("Resolved_To", "from[" + DomainConstants.RELATIONSHIP_RESOLVED_TO + "].to.name");
                lhmIssueSelectionStore.put("Related_CR_Content", "from[" + TigerConstants.RELATIONSHIP_PSS_GOVERNINGISSUE + "].to.id");
                // TS-184 Create and Edit Issue From Global Actions -- TIGTK -6309 : KWagh

            }
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in initializeIssueLinkedHashMap: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
    }

    /**
     * getIssueNotificationBodyHTML method is used to Issue messageHTML in Notification Object Payload
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public String getIssueNotificationBodyHTML(Context context, String[] args) throws Exception {
        String messageHTML = "";
        // TIGTK-10709 -- START
        String strSectionSub = DomainConstants.EMPTY_STRING;
        // TIGTK-10709 -- END
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String strIssueObjId = (String) programMap.get("id");
            String strBaseURL = (String) programMap.get("baseURL");
            String notificationObjName = (String) programMap.get("notificationName");
            StringList busSelects = new StringList("attribute[Subject Text]");

            MapList mpNotificationList = DomainObject.findObjects(context, "Notification", notificationObjName, "*", "*", TigerConstants.VAULT_ESERVICEADMINISTRATION, "", null, true, busSelects,
                    (short) 0);

            String strSubjectKey = "";

            if (mpNotificationList != null && (!mpNotificationList.isEmpty())) {
                strSubjectKey = (String) ((Map) mpNotificationList.get(0)).get("attribute[Subject Text]");

            }

            Map issueMap = (Map) getIssueInformation(context, strIssueObjId);

            if (UIUtil.isNotNullAndNotEmpty(strSubjectKey)) {
                issueMap.put("SectionSubject", EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), strSubjectKey));
            } else {
                issueMap.put("SectionSubject", "");
            }

            // TIGTK-6418:Rutuja Ekatpure:18/8/2017:Start
            Map<String, String> payload = (Map<String, String>) programMap.get("payload");
            if (payload != null) {
                if (payload.containsKey("IssueRejectComments")
                        && ("PSS_IssueRejectNotification".equalsIgnoreCase(notificationObjName) || "PSS_IssueReviewTaskRejectNotification".equalsIgnoreCase(notificationObjName))) {
                    String strIssueRejectComments = (String) payload.get("IssueRejectComments");
                    issueMap.put("IssueRejectComments", strIssueRejectComments);
                } // TIGTK-7580 : START
                if (payload.containsKey("TransferComments") && "PSS_TransferOwnershipNotification".equalsIgnoreCase(notificationObjName)) {
                    String strComments = (String) payload.get("TransferComments");
                    issueMap.put("TransferComments", strComments);
                    pss.ecm.enoECMChange_mxJPO enoECMChange = new pss.ecm.enoECMChange_mxJPO();
                    strSubjectKey = enoECMChange.getTranferOwnershipSubject(context, args);
                    issueMap.put("SectionSubject", strSubjectKey);
                } else {
                    issueMap.put("TransferComments", "");
                } // TIGTK-7580 : END
                  // TIGTK-10709 -- START
                if (UIUtil.isNotNullAndNotEmpty(notificationObjName) && "PSS_IssueTaskReassignNotification".equalsIgnoreCase(notificationObjName)) {
                    pss.ecm.enoECMChange_mxJPO enoECMObj = new pss.ecm.enoECMChange_mxJPO();
                    strSectionSub = enoECMObj.getTaskReassignmentSubject(context, args);
                    issueMap.put("SectionSubject", strSectionSub);
                }
                // TIGTK-10709 -- END
            }
            // TIGTK-6418:Rutuja Ekatpure:18/8/2017:End
            MapList mlInfoList = transformIssueMapToHTMLList(context, issueMap, strBaseURL);

            pss.ecm.notification.CommonNotificationUtil_mxJPO commonObj = new pss.ecm.notification.CommonNotificationUtil_mxJPO(context);
            String strStyleSheet = commonObj.getFormStyleSheet(context, "PSS_NotificationStyleSheet");
            messageHTML = commonObj.getHTMLTwoColumnTable(context, mlInfoList, strStyleSheet);

        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getIssueNotificationBodyHTML: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }

        return messageHTML;
    }

    /**
     * getIssueInformation method is used to get all information about Issue
     * @param context
     * @param strIssueId
     * @return Map
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    @SuppressWarnings({ "rawtypes" })
    public Map getIssueInformation(Context context, String strIssueId) throws Exception {
        Map issueMap = new HashMap();
        try {
            String strAttribEstEndDate = PropertyUtil.getSchemaProperty(context, Issue.SYMBOLIC_attribute_EstimatedEndDate);
            String strAttribPriority = PropertyUtil.getSchemaProperty(context, Issue.SYMBOLIC_attribute_Priority);
            if (UIUtil.isNotNullAndNotEmpty(strIssueId)) {
                DomainObject domIssueObj = DomainObject.newInstance(context);
                domIssueObj.setId(strIssueId);
                StringList slObjectSelects = new StringList(13);
                slObjectSelects.addElement(DomainConstants.SELECT_NAME);
                slObjectSelects.addElement(DomainConstants.SELECT_ID);
                slObjectSelects.addElement(DomainConstants.SELECT_CURRENT);
                slObjectSelects.addElement(DomainConstants.SELECT_DESCRIPTION);
                slObjectSelects.addElement(DomainConstants.SELECT_OWNER);
                slObjectSelects.addElement("attribute[" + strAttribEstEndDate + "]");
                slObjectSelects.addElement("attribute[" + strAttribPriority + "]");
                slObjectSelects.addElement("attribute[" + DomainConstants.ATTRIBUTE_CO_OWNER + "]");
                slObjectSelects.addElement("attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.name");
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.description");
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_ISSUE + "|from.type==PSS_ChangeRequest].from.id");
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_ISSUE + "|from.type==PSS_ChangeRequest].from.name");
                slObjectSelects.addElement("from[" + TigerConstants.RELATIONSHIP_ISSUE + "].to.id");
                slObjectSelects.addElement("from[" + TigerConstants.RELATIONSHIP_ISSUE + "].to.name");
                slObjectSelects.addElement("to[" + Issue.SYMBOLIC_relationship_AssignedIssue + "].from.name");
                // TS-184 Create and Edit Issue From Global Actions -- TIGTK -6309 : 02/08/2017 : KWagh
                slObjectSelects.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_PROGRAMRISKLEVEL + "]");
                slObjectSelects.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_ISSUE_DESCRIPTION + "]");
                slObjectSelects.addElement("from[" + DomainConstants.RELATIONSHIP_RESOLVED_TO + "].to.id");
                slObjectSelects.addElement("from[" + DomainConstants.RELATIONSHIP_RESOLVED_TO + "].to.name");
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_ISSUE + "].from.id");
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_ISSUE + "].from.name");
                slObjectSelects.addElement("from[" + TigerConstants.RELATIONSHIP_PSS_GOVERNINGISSUE + "].to.id");
                slObjectSelects.addElement("from[" + TigerConstants.RELATIONSHIP_PSS_GOVERNINGISSUE + "].to.name");
                slObjectSelects.addElement("from[" + DomainConstants.RELATIONSHIP_OBJECT_ROUTE + "].to.from[" + DomainConstants.RELATIONSHIP_ROUTE_NODE + "].to.name");
                // TS-184 Create and Edit Issue From Global Actions -- TIGTK -6309 : 02/08/2017 : KWagh

                // PCM : TIGTK-9942 : 27/09/2017 : AB : START
                DomainConstants.MULTI_VALUE_LIST.add("from[" + TigerConstants.RELATIONSHIP_ISSUE + "].to.id");
                DomainConstants.MULTI_VALUE_LIST.add("from[" + TigerConstants.RELATIONSHIP_ISSUE + "].to.name");
                DomainConstants.MULTI_VALUE_LIST.add("from[" + TigerConstants.RELATIONSHIP_PSS_GOVERNINGISSUE + "].to.id");
                DomainConstants.MULTI_VALUE_LIST.add("from[" + TigerConstants.RELATIONSHIP_PSS_GOVERNINGISSUE + "].to.name");
                DomainConstants.MULTI_VALUE_LIST.add("from[" + DomainConstants.RELATIONSHIP_RESOLVED_TO + "].to.id");
                DomainConstants.MULTI_VALUE_LIST.add("from[" + DomainConstants.RELATIONSHIP_RESOLVED_TO + "].to.name");
                // PCM : TIGTK-9942 : 27/09/2017 : AB : END

                issueMap = domIssueObj.getInfo(context, slObjectSelects);

            }

        }
        // Fix for FindBugs issue RuntimeException capture: Harika Varanasi : 21 March 2017: START
        catch (RuntimeException re) {
            throw re;
        }
        // Fix for FindBugs issue RuntimeException capture: Harika Varanasi : 21 March 2017: End
        catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getIssueInformation: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        } finally {
            // PCM : TIGTK-9942 : 27/09/2017 : AB : START
            DomainConstants.MULTI_VALUE_LIST.remove("from[" + TigerConstants.RELATIONSHIP_ISSUE + "].to.id");
            DomainConstants.MULTI_VALUE_LIST.remove("from[" + TigerConstants.RELATIONSHIP_ISSUE + "].to.name");
            DomainConstants.MULTI_VALUE_LIST.remove("from[" + TigerConstants.RELATIONSHIP_PSS_GOVERNINGISSUE + "].to.id");
            DomainConstants.MULTI_VALUE_LIST.remove("from[" + TigerConstants.RELATIONSHIP_PSS_GOVERNINGISSUE + "].to.name");
            DomainConstants.MULTI_VALUE_LIST.remove("from[" + DomainConstants.RELATIONSHIP_RESOLVED_TO + "].to.id");
            DomainConstants.MULTI_VALUE_LIST.remove("from[" + DomainConstants.RELATIONSHIP_RESOLVED_TO + "].to.name");
            // PCM : TIGTK-9942 : 27/09/2017 : AB : END
        }

        return issueMap;
    }

    /**
     * transformIssueMapToHTMLList
     * @param context
     * @param objectMap
     * @return
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    @SuppressWarnings({ "rawtypes" })
    public MapList transformIssueMapToHTMLList(Context context, Map objectMap, String strBaseURL) throws Exception {
        MapList mlInfoList = new MapList();
        try {

            pss.ecm.notification.CommonNotificationUtil_mxJPO commonObj = new pss.ecm.notification.CommonNotificationUtil_mxJPO(context);

            StringList slHyperLinkLabelKey = FrameworkUtil
                    .split(EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "EnterpriseChangeMgt.IssueNotification.HyperLinkLabelKey"), ",");
            StringList slHyperLinkLabelKeyIds = FrameworkUtil
                    .split(EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "EnterpriseChangeMgt.IssueNotification.HyperLinkLabelKeyIds"), ",");

            // TIGTK-6418:Rutuja Ekatpure:18/8/2017:Start
            boolean isIssueReject = false;

            if (objectMap.containsKey("IssueRejectComments")) {
                isIssueReject = true;
            }
            initializeIssueLinkedHashMap(lhmIssueSelectionStore, isIssueReject);

            lhmIssueFinalStore = lhmIssueSelectionStore;
            // TIGTK-6418:Rutuja Ekatpure:18/8/2017:End
            mlInfoList = commonObj.transformGenericMapToHTMLList(context, objectMap, strBaseURL, lhmIssueFinalStore, slHyperLinkLabelKey, slHyperLinkLabelKeyIds);

        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in transformIssueMapToHTMLList: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

        return mlInfoList;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    /**
     * getIssueRouteAssignee
     * @param context
     * @param args
     * @return StringList
     * @throws Exception
     * @author Harika Varanasi : SteepGraphs
     */
    public StringList getIssueRouteAssignee(Context context, String[] args) throws Exception {
        StringList slRoteAssigneeList = new StringList();
        String strContextUser = context.getUser();
        // TIGTK-13528 : 05-03-2018 : START
        Map programMap = (Map) JPO.unpackArgs(args);
        // TIGTK-13528 : 05-03-2018 : END
        try {
            String strIssueObjId = (String) programMap.get("id");

            if (UIUtil.isNotNullAndNotEmpty(strIssueObjId)) {
                DomainObject domIssueObj = DomainObject.newInstance(context);
                domIssueObj.setId(strIssueObjId);

                String strAttrRouteTaskUser = "attribute[" + PropertyUtil.getSchemaProperty(context, "attribute_RouteTaskUser") + "]";
                String selectRoutePersonName = new StringBuilder("from[").append(TigerConstants.RELATIONSHIP_OBJECT_ROUTE).append("].to.from[").append(DomainConstants.RELATIONSHIP_ROUTE_NODE)
                        .append("|to.type==\"Person\"].to.name").toString();
                String selectRouteGroupOrRoleName = new StringBuilder("from[").append(TigerConstants.RELATIONSHIP_OBJECT_ROUTE).append("].to.from[").append(DomainConstants.RELATIONSHIP_ROUTE_NODE)
                        .append("|to.type!=\"Person\"].").append(strAttrRouteTaskUser).toString();

                // TIGTK-12480 : Start : VB
                String strUserQuery = "print bus " + strIssueObjId + " select " + selectRoutePersonName + " dump |";
                String strRouteConnectedUser = MqlUtil.mqlCommand(context, strUserQuery, false, false);

                String strGroupOrRoleQuery = "print bus " + strIssueObjId + " select " + selectRouteGroupOrRoleName + " dump |";
                String strRouteConnectedGroupOrRole = MqlUtil.mqlCommand(context, strGroupOrRoleQuery, false, false);
                StringList slUserList = new StringList();

                // TIGTK-12770 :Start
                if (UIUtil.isNotNullAndNotEmpty(strRouteConnectedUser)) {
                    if (strRouteConnectedUser.contains("|")) {
                        String strSplitArray[] = strRouteConnectedUser.split("\\|");
                        for (int i = 0; i < strSplitArray.length; i++) {
                            slUserList.add(strSplitArray[i]);
                        }
                    } else {
                        slUserList.add(strRouteConnectedUser);
                    }
                }

                if (UIUtil.isNotNullAndNotEmpty(strRouteConnectedGroupOrRole)) {
                    if (strRouteConnectedGroupOrRole.contains("|")) {
                        String strSplitArray[] = strRouteConnectedGroupOrRole.split("\\|");
                        for (int i = 0; i < strSplitArray.length; i++) {
                            String strRole = strSplitArray[i].substring(5, strSplitArray[i].length());
                            slUserList.add(strRole);
                        }
                    } else {
                        slUserList.add(strRouteConnectedGroupOrRole);
                    }
                }
                // TIGTK-12770 :End
                slRoteAssigneeList.addAll(slUserList);
                // TIGTK-12480 : Start : END
            }

        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getIssueRouteAssignee: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }
        // TIGTK-11576 : START
        // TIGTK-13528 && TIGTK-13538 : 05-03-2018 : START
        String strCalledFrom = (String) programMap.get("calledFrom");
        if (!(UIUtil.isNotNullAndNotEmpty(strCalledFrom) && (strCalledFrom.equalsIgnoreCase("showIssueReferenceDocumentsCommands") || strCalledFrom.equalsIgnoreCase("checkIssueDemoteAccess")))) {
            // TIGTK-13528 && TIGTK-13538 : 05-03-2018 : END
            while (slRoteAssigneeList.contains(strContextUser)) {
                slRoteAssigneeList.remove(strContextUser);
            }
        }
        // TIGTK-11576 : END
        return slRoteAssigneeList;
    }

    // TIGTK-6418:Rutuja Ekatpure:10/8/2017:Start
    /****
     * This method called from emxNotificationUtil:objectNotification JPO method to get Notification Recipient list
     * @param context
     * @param args
     * @return StringList : List of Notification Recipient
     */
    public StringList getIssueRejectNotificationRecipients(Context context, String[] args) throws Exception {

        StringList slSenderIds = new StringList();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);

            String strIssueObjId = (String) programMap.get(DomainConstants.SELECT_ID);
            String strNotificationName = (String) programMap.get("notificationName");
            boolean isRejectNotification = true;
            // PCM : TIGKT-9942 : 27/09/2017 : AB : START
            boolean isRejectNotificationBecauseOfTaskRejection = false;

            if ("PSS_IssueReviewTaskRejectNotification".equalsIgnoreCase(strNotificationName)) {
                isRejectNotificationBecauseOfTaskRejection = true;
            }
            slSenderIds = getIssueNotificationRecipients(context, strIssueObjId, isRejectNotification, isRejectNotificationBecauseOfTaskRejection);

            // PCM : TIGKT-9942 : 27/09/2017 : AB : END
        } catch (Exception e) {
            logger.error("Error in getIssueRejectNotificationRecipients: ", e);
        }
        return slSenderIds;
    }

    /****
     * This method gives Notification Recipient list ,called from getIssueRejectNotificationRecipients method.
     * @param context
     * @param strIssueObjId
     *            : Issue object id
     * @param isRejectNotification
     *            : boolean value True or False
     * @return StringList : List of Notification Recipient
     */
    public StringList getIssueNotificationRecipients(Context context, String strIssueObjId, boolean isRejectNotification, boolean isRejectNotificationBecauseOfTaskRejection) throws Exception {
        logger.debug("pss.ecm.notification.IssueNotificationUtil_mxJPO:getIssueNotificationRecipients:START ");
        HashSet setMailRecipients = new HashSet();
        StringList slMailRecipients = new StringList();
        try {
            String strContextUser = context.getUser();
            StringList slSelects = new StringList();
            slSelects.add("attribute[" + DomainConstants.ATTRIBUTE_CO_OWNER + "]");
            slSelects.add("attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");
            slSelects.add(DomainConstants.SELECT_CURRENT);
            DomainObject domIssue = DomainObject.newInstance(context, strIssueObjId);
            Map mpIssueInfo = domIssue.getInfo(context, slSelects);

            String strCoOwner = (String) mpIssueInfo.get("attribute[" + DomainConstants.ATTRIBUTE_CO_OWNER + "]");
            String strOriginator = (String) mpIssueInfo.get("attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");
            setMailRecipients.add(strOriginator);
            // PCM : TIGTK-9942 : 27/09/2017 : AB : START
            setMailRecipients.add(strCoOwner);

            StringList slTemp = domIssue.getInfoList(context, "to[" + TigerConstants.RELATIONSHIP_ASSIGNEDISSUE + "].from.name");
            setMailRecipients.addAll(slTemp);

            if (isRejectNotificationBecauseOfTaskRejection) {
                Map<String, String> progMap = new HashMap<String, String>();
                progMap.put(DomainConstants.SELECT_ID, strIssueObjId);
                String[] args = JPO.packArgs(progMap);

                StringList slRouteAssignee = getIssueRouteAssignee(context, args);
                slRouteAssignee.remove(strContextUser);
                setMailRecipients.addAll(slRouteAssignee);
            }

            while (setMailRecipients.contains(strContextUser)) {
                setMailRecipients.remove(strContextUser);
            }

            slMailRecipients.addAll(setMailRecipients);
            // PCM : TIGTK-9942 : 27/09/2017 : AB : END
            logger.debug("pss.ecm.notification.IssueNotificationUtil_mxJPO:getIssueNotificationRecipients:END ");
        } catch (Exception e) {
            logger.error("Error in pss.ecm.notification.IssueNotificationUtil_mxJPO:getIssueNotificationRecipients:ERROR ", e);
            throw e;
        }
        return slMailRecipients;
    }

    // TIGTK-6418:Rutuja Ekatpure:10/8/2017:End
    // TIGTK-6415 : TS : 10/08/2017 : START
    @SuppressWarnings({ "rawtypes" })
    /**
     * This method called from emxNotificationUtil:objectNotification JPO method to get Notification Sender list
     * @param context
     * @param args
     * @return StringList
     * @throws Exception
     */
    public StringList getIssueCloseNotifiers(Context context, String[] args) throws Exception {
        logger.debug("pss.ecm.notification.IssueNotificationUtil:getIssueCloseNotifiers:START");
        StringList slSenderIds = new StringList();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strIssueObjId = (String) programMap.get(DomainConstants.SELECT_ID);
            boolean isRejectNotification = false;
            slSenderIds = getIssueNotificationRecipients(context, strIssueObjId, isRejectNotification, false);
            logger.debug("pss.ecm.notification:getIssueCloseNotifiers:END");
        } catch (Exception e) {
            logger.error("pss.ecm.notification.IssueNotificationUtil:getIssueCloseNotifiers:ERROR ", e);
        }
        return slSenderIds;
    }
    // TIGTK-6415 : TS : 10/08/2017 : END

    // TIGTK-6409:Pranjali Tupe :9/8/2017:START
    /****
     * This method is used for getting the users to be notified when Issue is demoted
     * @param args
     * @return StringList
     * @throws Exception
     */
    public StringList getIssueNotificationUsersForDemotion(Context context, String[] args) throws Exception {

        logger.debug("pss.ecm.notification.IssueNotificationUtil:getIssueNotificationUsersForDemotion:START");

        Map<?, ?> programMap = (Map<?, ?>) JPO.unpackArgs(args);
        String strIssueObjId = (String) programMap.get("id");
        StringList slSenders = new StringList();

        try {
            StringList slSelects = new StringList();
            slSelects.add("attribute[" + DomainConstants.ATTRIBUTE_CO_OWNER + "]");
            slSelects.add("attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");
            slSelects.add(DomainConstants.SELECT_CURRENT);
            DomainObject domIssue = DomainObject.newInstance(context, strIssueObjId);
            Map<?, ?> mpIssueInfo = domIssue.getInfo(context, slSelects);

            String sIssueState = (String) mpIssueInfo.get(DomainConstants.SELECT_CURRENT);
            String strCoOwner = (String) mpIssueInfo.get("attribute[" + DomainConstants.ATTRIBUTE_CO_OWNER + "]");
            String strOriginator = (String) mpIssueInfo.get("attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");

            StringList slBusSelect = new StringList();
            slBusSelect.add(DomainConstants.SELECT_NAME);
            slBusSelect.add(DomainConstants.SELECT_ID);

            slSenders.add(strOriginator);
            slSenders.add(strCoOwner);
            if (TigerConstants.STATE_PSS_ISSUE_ACTIVE.equals(sIssueState)) {
                StringList slRelSelect = new StringList(1);
                slRelSelect.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

                MapList mlIssueAssignee = domIssue.getRelatedObjects(context, TigerConstants.RELATIONSHIP_ASSIGNEDISSUE, DomainConstants.TYPE_PERSON, slBusSelect, slRelSelect, true, false, (short) 1,
                        null, null, 0);

                for (Object objAssignee : mlIssueAssignee) {
                    Map<?, ?> mAssignee = (Map<?, ?>) objAssignee;
                    String sAssigneeName = (String) mAssignee.get(DomainConstants.SELECT_NAME);
                    slSenders.add(sAssigneeName);
                }

                StringList slRoteAssigneeList = getIssueRouteAssignee(context, args);
                slSenders.addAll(slRoteAssigneeList);
            }

            String strContextUser = context.getUser();
            // TIGTK-9940 -- START
            Set<String> hsSenders = new HashSet<String>();
            hsSenders.addAll(slSenders);
            slSenders.clear();
            slSenders.addAll(hsSenders);
            if (slSenders.contains(strContextUser)) {
                slSenders.remove(strContextUser);
            }
            // TIGTK-9940 -- END

            logger.debug("pss.ecm.notification.IssueNotificationUtil:getIssueNotificationUsersForDemotion:END");
        } catch (Exception ex) {
            logger.error("pss.ecm.notification.IssueNotificationUtil:getIssueNotificationUsersForDemotion:ERROR ", ex);
            throw ex;
        }
        return slSenders;
    }
    // TIGTK-6409:Pranjali Tupe :9/8/2017:END

    // TIGTK-6412:Pranjali Tupe :9/8/2017:START
    /****
     * This method is used for getting the users to be notified when Issue is promoted to Accepted state
     * @param args
     * @return StringList
     * @throws Exception
     */
    public StringList getIssueNotificationRecipientsForAcceptedState(Context context, String[] args) throws Exception {

        StringList slSenderIds = new StringList();
        try {
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);

            String strIssueObjId = (String) programMap.get(DomainConstants.SELECT_ID);
            boolean isRejectNotification = false;
            slSenderIds = getIssueNotificationRecipients(context, strIssueObjId, isRejectNotification, false);

        } catch (Exception ex) {
            logger.error("Error in getIssueNotificationRecipientsForAcceptedState: ", ex);
        }
        return slSenderIds;
    }
    // TIGTK-6412:Pranjali Tupe :9/8/2017:END

    /****
     * This method is used for getting the users to be notified when Issue is connected to Assignee
     * @param args
     * @return StringList
     * @throws Exception
     */
    public StringList getToListOfIssueAssignee(Context context, String[] args) throws Exception {
        logger.debug("getToListOfIssueAssignee : START");

        StringList slGenericToList = new StringList();
        String strContextUser = context.getUser();
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            Map payLoadMap = (Map) programMap.get("payload");
            String strAssigneeName = (String) payLoadMap.get("args_2");
            String strId = (String) payLoadMap.get("args_0");
            DomainObject domObj = DomainObject.newInstance(context, strId);
            String strCurrentState = domObj.getInfo(context, DomainConstants.SELECT_CURRENT);
            if (TigerConstants.STATE_PSS_ISSUE_ASSIGN.equals(strCurrentState) || TigerConstants.STATE_PSS_ISSUE_ACTIVE.equals(strCurrentState)) {
                slGenericToList.add(strAssigneeName);

                if (!slGenericToList.contains(strAssigneeName))
                    slGenericToList.add(strAssigneeName);

                if (slGenericToList.contains(strContextUser)) {
                    while (slGenericToList.contains(strContextUser)) {
                        slGenericToList.remove(strContextUser);
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("Error in getToListOfIssueAssignee: ", ex);
        }
        return slGenericToList;
    }
}