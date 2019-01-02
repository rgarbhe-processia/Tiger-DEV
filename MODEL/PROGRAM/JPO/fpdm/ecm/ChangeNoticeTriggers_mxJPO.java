package fpdm.ecm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.matrixone.apps.domain.DomainObject;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.MatrixException;
import matrix.util.StringList;

public class ChangeNoticeTriggers_mxJPO extends fpdm.ecm.Constants_mxJPO {
    private static Logger logger = LoggerFactory.getLogger("fpdm.ecm.ChangeNoticeTriggers");

    /**
     * Set Attribute Effectivity Date of CN to Affected Item
     * @plm.usage Trigger: FPDM_PolicyPSSChangeNoticeStateInReviewPromoteAction Revision: UpdateEffectivityDate
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            CN ID
     * @return
     * @throws Exception
     */
    private void populatesEffectivityDate(Context context, String sCNID) throws Exception {
        DomainObject doChangeNotice = DomainObject.newInstance(context, sCNID);
        String sCNEffectivityDate = doChangeNotice.getAttributeValue(context, getSchemaProperty(context, SYMBOLIC_attribute_PSS_Effectivity_Date));
        logger.debug("populatesEffectivityDate() - sCNID = <" + sCNID + "> sCNEffectivityDate = <" + sCNEffectivityDate + ">");

        StringList slSelectOnRelatedObjects = new StringList();
        slSelectOnRelatedObjects.add(SELECT_ID);

        // get linked MBOMPart and Tools
        ArrayList<Map<String, Object>> alRelatedObjects = fpdm.utils.SelectData_mxJPO.getRelatedInfos(context, sCNID,
                "from[" + getSchemaProperty(context, SYMBOLIC_relationship_FPDM_CNAffectedItems) + "].to.id", slSelectOnRelatedObjects);
        logger.debug("populatesEffectivityDate() - alRelatedObjects() = <" + alRelatedObjects + ">");

        String TYPE_FPDM_MBOM_PART = getSchemaProperty(context, SYMBOLIC_type_FPDM_MBOMPart);
        String ATTRIBUTE_FPDM_EFFECTIVITY_DATE = getSchemaProperty(context, SYMBOLIC_attribute_FPDM_EffectivityDate);
        String ATTRIBUTE_PLMREFRERENCE_V_APPLICABILITY_DATE = getSchemaProperty(context, SYMBOLIC_attribute_PLMReference_V_ApplicabilityDate);
        Map<String, Object> mapObjectInfo = null;
        DomainObject doObject = DomainObject.newInstance(context);
        for (Iterator<Map<String, Object>> iterator = alRelatedObjects.iterator(); iterator.hasNext();) {
            mapObjectInfo = iterator.next();

            doObject.setId((String) mapObjectInfo.get(SELECT_ID));

            if (doObject.isKindOf(context, TYPE_FPDM_MBOM_PART)) {
                doObject.setAttributeValue(context, ATTRIBUTE_FPDM_EFFECTIVITY_DATE, sCNEffectivityDate);
            } else {
                // VPMReference (Tool)
                doObject.setAttributeValue(context, ATTRIBUTE_PLMREFRERENCE_V_APPLICABILITY_DATE, sCNEffectivityDate);
            }
        }

    }

    /**
     * Send related MBOM 100 % to SAP. A message is send to the EAI
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            CN ID
     * @return
     * @throws Exception
     */
    public void sendToFCSProcess(Context context, String[] args) throws Exception {
        try {
            logger.debug("sendToFCSProcess() - Arrays.toString(args) = <" + Arrays.toString(args) + ">");
            String sCNID = args[0];

            StringList slSelectOnRelatedObjects = new StringList();
            slSelectOnRelatedObjects.add(SELECT_ID);
            slSelectOnRelatedObjects.add(SELECT_TYPE);

            // get linked MBOMPart and Tools
            ArrayList<Map<String, Object>> alRelatedMBOMParts = fpdm.utils.SelectData_mxJPO.getRelatedInfos(context, sCNID,
                    "from[" + getSchemaProperty(context, SYMBOLIC_relationship_FPDM_CNAffectedItems) + "].to.id", slSelectOnRelatedObjects);
            logger.debug("sendToFCSProcess() - alRelatedMBOMParts.size() = <" + alRelatedMBOMParts.size() + ">");

            if (alRelatedMBOMParts.size() > 0) {
                // send a message
                sendToFCSProcess(context, sCNID);

            } else {
                logger.debug("sendToFCSProcess() - No related MBOMPart found.");
            }

        } catch (Exception e) {
            logger.error("Error in sendToFCSProcess()\n", e);
            throw e;
        }
    }

    /**
     * Send related MBOM 100 % to SAP. A message is send to the EAI
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            CN ID
     * @return
     * @throws Exception
     */
    private boolean sendToFCSProcess(Context context, String sCNID) throws Exception {
        boolean bResult = false;
        logger.debug("sendToFCSProcess() - sCNID = <" + sCNID + ">");

        try {
            // send a message
            logger.debug("sendToFCSProcess() - Ask EAI a transfer to FCS.");
            fpdm.eai.utils.AMQManager_mxJPO jpo = fpdm.eai.utils.AMQManager_mxJPO.getInstance(context);
            HashMap<String, String> map = new HashMap<String, String>();
            map.put("objectId", sCNID);
            jpo.addSAPTransferMessage(map);

            bResult = true;
        } catch (Exception e) {
            logger.error("Error in sendToFCSProcess()\n", e);

        }
        return bResult;
    }

    /**
     * Send related MBOM 100 % to SAP. A message is send to the EAI and promote CN to In Transfer
     * @plm.usage Trigger: PolicyCNStateReviewPromoteOverride Revision: promoteToInTransferAndSendToFCSProcess
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            CN ID
     * @return
     * @throws Exception
     */
    public int promoteToInTransferAndSendToFCSProcess(Context context, String[] args) throws Exception {
        try {
            logger.debug("promoteToInTransferAndSendToFCSProcess() - Arrays.toString(args) = <" + Arrays.toString(args) + ">");
            String sCNID = args[0];

            StringList slSelectOnRelatedObjects = new StringList();
            slSelectOnRelatedObjects.add(SELECT_ID);

            // get linked MBOMPart and Tools
            ArrayList<Map<String, Object>> alRelatedMBOMParts = fpdm.utils.SelectData_mxJPO.getRelatedInfos(context, sCNID,
                    "from[" + getSchemaProperty(context, SYMBOLIC_relationship_FPDM_CNAffectedItems) + "].to.id", slSelectOnRelatedObjects);
            logger.debug("promoteToInTransferAndSendToFCSProcess() - alRelatedMBOMParts.size() = <" + alRelatedMBOMParts.size() + ">");

            if (alRelatedMBOMParts.size() > 0) {
                // populate CN Effectivity date to all linked objects
                populatesEffectivityDate(context, sCNID);
                // send a message
                boolean bTransfer = sendToFCSProcess(context, sCNID);

                if (bTransfer) {
                    DomainObject doChangeNotice = DomainObject.newInstance(context, sCNID);
                    String POLICY_PSS_CHANGE_NOTICE = getSchemaProperty(context, SYMBOLIC_policy_PSS_ChangeNotice);
                    String STATE_IN_TRANSFER = getSchemaProperty(context, "policy", POLICY_PSS_CHANGE_NOTICE, "state_InTransfer");
                    doChangeNotice.setState(context, STATE_IN_TRANSFER);

                    String sCurrentDate = fpdm.utils.DateUtil_mxJPO.getCurrentDateTimeMatrixFormat();
                    doChangeNotice.setAttributeValue(context, getSchemaProperty(context, SYMBOLIC_attribute_PSS_CN_Transfer_Date), sCurrentDate);

                } else {
                    return 1;
                }

            } else {
                logger.debug("promoteToInTransferAndSendToFCSProcess() - No related MBOMPart found.");
            }

        } catch (Exception e) {
            logger.error("Error in promoteToInTransferAndSendToFCSProcess()\n", e);
            throw e;
        }

        return 0;
    }

    /**
     * Demote CN to In Transfer and Send related MBOM 100 % to SAP.
     * @plm.usage Trigger: PolicyCNStateNotFullyIntegratedDemoteOverride Revision: demoteToInTransferAndSendToFCSProcess
     * @plm.usage Trigger: PolicyCNStateTransferErrorDemoteOverride Revision: demoteToInTransferAndSendToFCSProcess
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public int demoteToInTransferAndSendToFCSProcess(Context context, String[] args) throws Exception {
        try {
            logger.debug("demoteToInTransferAndSendToFCSProcess() - Arrays.toString(args) = <" + Arrays.toString(args) + ">");
            String sCNID = args[0];

            StringList slSelectOnRelatedObjects = new StringList();
            slSelectOnRelatedObjects.add(SELECT_ID);

            // get linked MBOMPart and Tools
            ArrayList<Map<String, Object>> alRelatedMBOMParts = fpdm.utils.SelectData_mxJPO.getRelatedInfos(context, sCNID,
                    "from[" + getSchemaProperty(context, SYMBOLIC_relationship_FPDM_CNAffectedItems) + "].to.id", slSelectOnRelatedObjects);
            logger.debug("promoteToInTransferAndSendToFCSProcess() - alRelatedMBOMParts.size() = <" + alRelatedMBOMParts.size() + ">");

            if (alRelatedMBOMParts.size() > 0) {
                // send a message
                boolean bTransfer = sendToFCSProcess(context, sCNID);

                if (bTransfer) {
                    DomainObject doChangeNotice = DomainObject.newInstance(context, sCNID);
                    String POLICY_PSS_CHANGE_NOTICE = getSchemaProperty(context, SYMBOLIC_policy_PSS_ChangeNotice);
                    String STATE_IN_TRANSFER = getSchemaProperty(context, "policy", POLICY_PSS_CHANGE_NOTICE, "state_InTransfer");
                    doChangeNotice.setState(context, STATE_IN_TRANSFER);
                } else {
                    return 1;
                }

            } else {
                logger.debug("promoteToInTransferAndSendToFCSProcess() - No related MBOMPart found.");
            }

        } catch (Exception e) {
            logger.error("Error in demoteToInTransferAndSendToFCSProcess()\n", e);
            throw e;
        }

        return 0;
    }

    /**
     * Promote related MCO to implemented when all CNs are in the states (Fully Integrated, Not Fully Integrated and Cancelled)
     * @plm.usage Trigger: PolicyPSS_ChangeNoticeStateInTransferPromoteAction Rev: checkAndPromoteRelatedMCOToImplemented
     * @plm.usage Trigger: PolicyPSS_ChangeNoticeStatePreparePromoteAction Rev: checkAndPromoteRelatedMCOToImplemented
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            CN ID
     * @return
     * @throws Exception
     */
    public void checkAndPromoteRelatedMCOToImplemented(Context context, String[] args) throws MatrixException {
        try {
            logger.debug("checkAndPromoteRelatedMCOToImplemented() - Current CN = <" + Arrays.toString(args) + ">");

            String RELATIONSHIP_RELATEDCN = getSchemaProperty(context, SYMBOLIC_relationship_PSS_RelatedCN);
            String POLICY_PSS_CHANGE_NOTICE = getSchemaProperty(context, SYMBOLIC_policy_PSS_ChangeNotice);
            String STATE_FULLY_INTEGRATED = getSchemaProperty(context, "policy", POLICY_PSS_CHANGE_NOTICE, "state_FullyIntegrated");
            String STATE_NOT_FULLY_INTEGRATED = getSchemaProperty(context, "policy", POLICY_PSS_CHANGE_NOTICE, "state_NotFullyIntegrated");
            String STATE_CANCELLED = getSchemaProperty(context, "policy", POLICY_PSS_CHANGE_NOTICE, "state_Cancelled");
            String[] saTargetStates = new String[] { STATE_FULLY_INTEGRATED, STATE_NOT_FULLY_INTEGRATED, STATE_CANCELLED };
            StringList alTargetStates = new StringList(saTargetStates);

            String sCNCurrentState = args[1];
            if (alTargetStates.contains(sCNCurrentState)) {
                StringList slSelectOnRelatedObjects = new StringList();
                slSelectOnRelatedObjects.add(SELECT_ID);
                slSelectOnRelatedObjects.add(SELECT_CURRENT);

                // get linked MCO
                DomainObject doCN = DomainObject.newInstance(context, args[0]);
                // get related MCO
                String sMCOId = doCN.getInfo(context, "to[" + RELATIONSHIP_RELATEDCN + "].from.id");
                logger.debug("checkAndPromoteRelatedMCOToImplemented() - sMCOId = <" + sMCOId + ">");
                // get all CNs linked to the current MCO
                ArrayList<Map<String, Object>> alMCORelatedCNs = fpdm.utils.SelectData_mxJPO.getRelatedInfos(context, sMCOId, "from[" + RELATIONSHIP_RELATEDCN + "].to.id", slSelectOnRelatedObjects);
                logger.debug("checkAndPromoteRelatedMCOToImplemented() - alMCORelatedCNs = <" + alMCORelatedCNs + ">");

                boolean bPromoteMCO = true;
                String sCNCurrent = null;
                for (Map<String, Object> map : alMCORelatedCNs) {
                    sCNCurrent = (String) map.get(SELECT_CURRENT);
                    if (!alTargetStates.contains(sCNCurrent)) {
                        bPromoteMCO = false;
                    }
                }
                if (bPromoteMCO) {
                    String POLICY_PSS_MFG_CHANGE_ORDER = getSchemaProperty(context, SYMBOLIC_policy_PSS_MfgChangeOrder);
                    String STATE_IMPLEMENTED = getSchemaProperty(context, "policy", POLICY_PSS_MFG_CHANGE_ORDER, "state_Implemented");
                    String STATE_COMPLETE = getSchemaProperty(context, "policy", POLICY_PSS_MFG_CHANGE_ORDER, "state_Complete");

                    DomainObject domMCO = DomainObject.newInstance(context, sMCOId);
                    String sCurrent = domMCO.getInfo(context, SELECT_CURRENT);
                    logger.debug("checkAndPromoteRelatedMCOToImplemented() - current state = <" + sCurrent + "> target state = <" + STATE_COMPLETE + ">");
                    if (STATE_COMPLETE.equals(sCurrent)) {
                        // promote MCO to Implemented state
                        domMCO.setState(context, STATE_IMPLEMENTED);
                        logger.debug("checkAndPromoteRelatedMCOToImplemented() - MCO promoted to implmented state.");
                    } else {
                        logger.debug("checkAndPromoteRelatedMCOToImplemented() - MCO cannot be promote because it's not at the correct state.");
                    }
                } else {
                    logger.debug("checkAndPromoteRelatedMCOToImplemented() - MCO can't be promoted because all related CNs are not at the requested state <" + alTargetStates + ">.");
                }
            } else {
                logger.debug("checkAndPromoteRelatedMCOToImplemented() - CN is not on the requested state <" + alTargetStates + ">.");
            }
        } catch (MatrixException e) {
            logger.error("Error in checkAndPromoteRelatedMCOToImplemented()\n", e);
            throw e;
        }
    }

    /**
     * Send a notification when the CN is promoted from In Transfer state (SAP feed back)
     * @plm.usage Trigger: PolicyPSS_ChangeNoticeStateInTransferPromoteAction Rev: sendCNNotification
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            CN ID
     * @throws Exception
     */
    public void sendCNNotification(Context context, String[] args) throws Exception {
        try {
            logger.debug("sendCNNotification() - Arrays.toString(args) = <" + Arrays.toString(args) + ">");
            String sCNID = args[0];

            DomainObject doCN = DomainObject.newInstance(context, sCNID);
            String sCNCurrent = doCN.getInfo(context, SELECT_CURRENT);
            logger.debug("sendCNNotification() - sCNCurrent = <" + sCNCurrent + ">");

            String POLICY_PSS_CHANGE_NOTICE = getSchemaProperty(context, SYMBOLIC_policy_PSS_ChangeNotice);
            String STATE_FULLY_INTEGRATED = getSchemaProperty(context, "policy", POLICY_PSS_CHANGE_NOTICE, "state_FullyIntegrated");
            String STATE_NOT_FULLY_INTEGRATED = getSchemaProperty(context, "policy", POLICY_PSS_CHANGE_NOTICE, "state_NotFullyIntegrated");
            String STATE_TRANSFER_ERROR = getSchemaProperty(context, "policy", POLICY_PSS_CHANGE_NOTICE, "state_TransferError");

            String sNotifcationName = null;
            if (STATE_FULLY_INTEGRATED.equals(sCNCurrent)) {
                sNotifcationName = "FPDM_CNPromoteFullyIntegrationNotification";
            } else if (STATE_NOT_FULLY_INTEGRATED.equals(sCNCurrent)) {
                sNotifcationName = "FPDM_CNPromoteNotFullyIntegrationNotification";
            } else if (STATE_TRANSFER_ERROR.equals(sCNCurrent)) {
                sNotifcationName = "FPDM_CNPromoteTransferErrorNotification";
            }
            logger.debug("sendCNNotification() - sNotifcationName = <" + sNotifcationName + ">");

            // send notification
            Map<String, Object> programMap = new HashMap<String, Object>();
            programMap.put("objectId", sCNID);
            programMap.put("notificationName", sNotifcationName);
            programMap.put("payload", null);

            JPO.invoke(context, "emxNotificationUtil", null, "objectNotificationFromMap", JPO.packArgs(programMap));

        } catch (Exception e) {
            logger.error("Error in sendCNNotification()\n", e);
            throw e;
        }

    }

}
