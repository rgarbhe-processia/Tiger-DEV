package fpdm.ecm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.matrixone.apps.domain.util.EnoviaResourceBundle;

import matrix.db.Context;
import matrix.util.StringList;

public class MfgChangeOrderTriggers_mxJPO extends fpdm.ecm.Constants_mxJPO {
    private static Logger logger = LoggerFactory.getLogger("fpdm.ecm.MfgChangeOrderTriggers");

    /**
     * Check if MCO can be promoted from Complete to Implemented state
     * @plm.usage Trigger: FPDM_PolicyPSSMfgChangeOrderStateCompletePromoteCheck Rev: checkBeforePromoteMCOToImplementedState
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     * @return
     * @throws Exception
     */
    public int checkBeforePromoteMCOToImplementedState(Context context, String[] args) throws Exception {
        try {
            logger.debug("checkBeforePromoteMCOToImplementedState() - Arrays.toString(args) = <" + Arrays.toString(args) + ">");
            String sMCOId = args[0];

            String RELATIONSHIP_RELATEDCN = getSchemaProperty(context, SYMBOLIC_relationship_PSS_RelatedCN);
            String ATTRIBUTE_TRANSFER_TO_SAP_EXPECTED = getSchemaProperty(context, SYMBOLIC_attribute_PSS_Transfer_To_SAP_Expected);
            String RELATIONSHIP_MFGCHANGEACTION = getSchemaProperty(context, SYMBOLIC_relationship_PSS_MfgChangeAction);

            ArrayList<String> alAuthorizedCNStates = new ArrayList<>();
            alAuthorizedCNStates.add("Not Fully Integrated");

            StringList slSelects = new StringList();
            slSelects.add("attribute[" + ATTRIBUTE_TRANSFER_TO_SAP_EXPECTED + "].value");

            // get MCO attribute value
            Map<String, Object> mMCO = fpdm.utils.SelectData_mxJPO.getSelectBusinessObjectData(context, sMCOId, slSelects);
            String sTransferToSAPExpected = (String) mMCO.get("attribute[" + ATTRIBUTE_TRANSFER_TO_SAP_EXPECTED + "].value");
            logger.debug("checkBeforePromoteMCOToImplementedState() - sMCOId = <" + sMCOId + "> sTransferToSAPExpected = <" + sTransferToSAPExpected + ">");

            StringList slSelectOnRelatedObjects = new StringList();
            slSelectOnRelatedObjects.add(SELECT_CURRENT);

            // get linked MCA
            ArrayList<Map<String, Object>> alRelatedMCAs = fpdm.utils.SelectData_mxJPO.getRelatedInfos(context, sMCOId, "from[" + RELATIONSHIP_MFGCHANGEACTION + "].to.id", slSelectOnRelatedObjects);
            logger.debug("checkBeforePromoteMCOToImplementedState() - alRelatedMCAs = <" + alRelatedMCAs + ">");

            // check all linked MCA are at the Complete state
            for (Map<String, Object> mapMCA : alRelatedMCAs) {
                logger.debug("checkBeforePromoteMCOToImplementedState() - mapMCA = <" + mapMCA + ">");
                if (!"Complete".equals((String) mapMCA.get(SELECT_CURRENT))) {
                    String sErrorMsg = EnoviaResourceBundle.getProperty(context, "EnterpriseChangeMgt", "FPDM_EnterpriseChangeMgt.MCO.PromoteToImplemented.MCAs.Error",
                            context.getSession().getLanguage());
                    logger.debug("checkBeforePromoteMCOToImplementedState() - sErrorMsg = <" + sErrorMsg + ">");

                    fpdm.utils.Notice_mxJPO.mqlNotice(context, sErrorMsg);
                    return 1;
                }
            }

            // get linked CN
            ArrayList<Map<String, Object>> alRelatedCNs = fpdm.utils.SelectData_mxJPO.getRelatedInfos(context, sMCOId, "from[" + RELATIONSHIP_RELATEDCN + "].to.id", slSelectOnRelatedObjects);
            logger.debug("checkBeforePromoteMCOToImplementedState() - alRelatedCNs = <" + alRelatedCNs + ">");

            // check no CN is at a different state than Not Fully Integrated, Fully Integrated or Cancelled
            String sCNCurrent = null;
            for (Map<String, Object> mapCN : alRelatedCNs) {
                sCNCurrent = (String) mapCN.get(SELECT_CURRENT);
                if (!"Not Fully Integrated".equals(sCNCurrent) && !"Fully Integrated".equals(sCNCurrent) && !"Cancelled".equals(sCNCurrent)) {
                    String sErrorMsg = EnoviaResourceBundle.getProperty(context, "EnterpriseChangeMgt", "FPDM_EnterpriseChangeMgt.MCO.PromoteToImplemented.CNs.Error",
                            context.getSession().getLanguage());
                    fpdm.utils.Notice_mxJPO.mqlNotice(context, sErrorMsg);
                    return 1;
                }
            }

        } catch (Exception e) {
            logger.error("Error in checkBeforePromoteMCOToImplementedState()\n", e);
            throw e;
        }
        return 0;
    }

    /**
     * Promote MCO from Complete to Implemented state
     * @plm.usage Trigger: FPDM_PolicyPSSMfgChangeOrderStateCompletePromoteOverride Rev: checkAndPromoteAutomaticallyMCOToImplementedState
     * @plm.usage Trigger: PolicyPSSMfgChangeOrdeStateInReviewPromoteAction Rev: checkAndPromoteAutomaticallyMCOToImplementedState
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     * @return
     * @throws Exception
     */
    public int checkAndPromoteAutomaticallyMCOToImplementedState(Context context, String[] args) throws Exception {
        try {
            logger.debug("checkAndPromoteAutomaticallyMCOToImplementedState() - Arrays.toString(args) = <" + Arrays.toString(args) + ">");
            String sMCOId = args[0];

            ArrayList<String> alAuthorizedCNStates = new ArrayList<>();
            alAuthorizedCNStates.add("Not Fully Integrated");

            StringList slSelects = new StringList();
            slSelects.add("attribute[" + getSchemaProperty(context, SYMBOLIC_attribute_PSS_Transfer_To_SAP_Expected) + "].value");

            // get MCO attribute value
            Map<String, Object> mMCO = fpdm.utils.SelectData_mxJPO.getSelectBusinessObjectData(context, sMCOId, slSelects);
            String sTransferToSAPExpected = (String) mMCO.get("attribute[" + getSchemaProperty(context, SYMBOLIC_attribute_PSS_Transfer_To_SAP_Expected) + "].value");
            logger.debug("checkAndPromoteAutomaticallyMCOToImplementedState() - sMCOId = <" + sMCOId + "> sTransferToSAPExpected = <" + sTransferToSAPExpected + ">");

            // get linked CN
            ArrayList<Map<String, Object>> alRelatedCNs = fpdm.utils.SelectData_mxJPO.getRelatedInfos(context, sMCOId,
                    "from[" + getSchemaProperty(context, SYMBOLIC_relationship_PSS_RelatedCN) + "].to.id", new StringList(SELECT_ID));
            logger.debug("checkAndPromoteAutomaticallyMCOToImplementedState() - alRelatedCNs = <" + alRelatedCNs + ">");

            if (ATTRIBUTE_PSS_TRANSFER_TO_SAP_EXPECTED_RNAGE_YES.equals(sTransferToSAPExpected) && alRelatedCNs.size() == 0) {
                // No CN related to the MCO and SAP Transfer is required
                logger.debug("checkAndPromoteAutomaticallyMCOToImplementedState() - No CN related to the MCO and SAP Transfer is required.");

                StringBuilder processStr = new StringBuilder();

                processStr.append("JSP:postProcess");
                processStr.append("|");
                processStr.append("commandName=");
                processStr.append("FPDM_PromoteMCOToImplementedCommand");
                processStr.append("|");
                processStr.append("objectId=");
                processStr.append(sMCOId);

                fpdm.utils.Notice_mxJPO.mqlNotice(context, processStr.toString());
                return 1;
            }

        } catch (Exception e) {
            logger.error("Error in checkAndPromoteAutomaticallyMCOToImplementedState()\n", e);
            throw e;
        }
        return 0;
    }

}
