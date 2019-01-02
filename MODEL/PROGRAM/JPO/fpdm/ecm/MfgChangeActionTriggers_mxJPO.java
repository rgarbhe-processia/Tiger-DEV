package fpdm.ecm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.PropertyUtil;

import matrix.db.Context;
import matrix.util.MatrixException;
import matrix.util.StringList;

public class MfgChangeActionTriggers_mxJPO extends fpdm.ecm.Constants_mxJPO {
    private static Logger logger = LoggerFactory.getLogger("fpdm.ecm.MfgChangeActionTriggers");

    /**
     * Check if related MCO can be promoted and promote it from Complete to Implemented state
     * @plm.usage Trigger: FPDM_PolicyPSSMfgChangeActionStateInReviewPromoteAction Rev: checkAndPromoteAutomaticallyMCOToImplementedState
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     * @return
     * @throws Exception
     */
    public int checkAndPromoteAutomaticallyMCOToImplementedState(Context context, String[] args) throws MatrixException {
        try {
            logger.debug("checkAndPromoteAutomaticallyMCOToImplementedState() - Arrays.toString(args) = <" + Arrays.toString(args) + ">");
            String sMCAId = args[0];

            StringList slSelectOnRelatedObjects = new StringList();
            slSelectOnRelatedObjects.add(SELECT_ID);
            slSelectOnRelatedObjects.add(SELECT_CURRENT);

            // get linked MCO
            ArrayList<Map<String, Object>> alRelatedMCOs = fpdm.utils.SelectData_mxJPO.getRelatedInfos(context, sMCAId,
                    "to[" + getSchemaProperty(context, SYMBOLIC_relationship_PSS_MfgChangeAction) + "].from.id", slSelectOnRelatedObjects);
            logger.debug("checkAndPromoteAutomaticallyMCOToImplementedState() - alRelatedMCOs = <" + alRelatedMCOs + ">");

            if (alRelatedMCOs.size() > 0) {
                Map<String, Object> mRelatedMCO = alRelatedMCOs.get(0);
                String sMCOCurrent = (String) mRelatedMCO.get(SELECT_CURRENT);

                if ("Complete".equals(sMCOCurrent)) {
                    String sMCOID = (String) mRelatedMCO.get(SELECT_ID);

                    // get linked MCA
                    ArrayList<Map<String, Object>> alRelatedMCAs = fpdm.utils.SelectData_mxJPO.getRelatedInfos(context, sMCAId,
                            "from[" + getSchemaProperty(context, SYMBOLIC_relationship_PSS_MfgChangeAction) + "].to.id", slSelectOnRelatedObjects);
                    logger.debug("checkAndPromoteAutomaticallyMCOToImplementedState() - alRelatedMCAs = <" + alRelatedMCAs + ">");

                    boolean bAllRelatedMCAAtCompleteState = true;
                    for (Map<String, Object> mMCA : alRelatedMCAs) {
                        if (!"Complete".equals((String) mMCA.get(SELECT_CURRENT))) {
                            bAllRelatedMCAAtCompleteState = false;
                            break;
                        }
                    }
                    logger.debug("checkAndPromoteAutomaticallyMCOToImplementedState() - bAllRelatedMCAAtCompleteState = <" + bAllRelatedMCAAtCompleteState + ">");

                    if (bAllRelatedMCAAtCompleteState) {
                        DomainObject doMCO = DomainObject.newInstance(context, sMCOID);

                        // get MCO attribute value
                        String sTransferToSAPExpected = doMCO.getInfo(context, "attribute[" + getSchemaProperty(context, SYMBOLIC_attribute_PSS_Transfer_To_SAP_Expected) + "].value");
                        logger.debug("checkBeforePromoteMCOToImplementedState() - sMCOID = <" + sMCOID + "> sTransferToSAPExpected = <" + sTransferToSAPExpected + ">");

                        // get linked CN
                        ArrayList<Map<String, Object>> alRelatedCNs = fpdm.utils.SelectData_mxJPO.getRelatedInfos(context, sMCOID,
                                "from[" + getSchemaProperty(context, SYMBOLIC_relationship_PSS_RelatedCN) + "].to.id", slSelectOnRelatedObjects);
                        logger.debug("checkBeforePromoteMCOToImplementedState() - alRelatedCNs = <" + alRelatedCNs + ">");

                        // check no CN is at a different state than Not Fully Integrated, Fully Integrated or Cancelled
                        boolean bAllCNareAtValidState = false;
                        String sCNCurrent = null;
                        for (Map<String, Object> mapCN : alRelatedCNs) {
                            bAllCNareAtValidState = true;
                            sCNCurrent = (String) mapCN.get(SELECT_CURRENT);
                            if (!"Not Fully Integrated".equals(sCNCurrent) && !"Fully Integrated".equals(sCNCurrent) && !"Cancelled".equals(sCNCurrent)) {
                                bAllCNareAtValidState = false;
                                break;
                            }
                        }

                        boolean bCanPromoteMCO = false;
                        if (ATTRIBUTE_PSS_TRANSFER_TO_SAP_EXPECTED_RANGE_NO.equals(sTransferToSAPExpected) && alRelatedCNs.size() == 0) {
                            bCanPromoteMCO = true;
                        }
                        if (ATTRIBUTE_PSS_TRANSFER_TO_SAP_EXPECTED_RNAGE_YES.equals(sTransferToSAPExpected) && bAllCNareAtValidState) {
                            bCanPromoteMCO = true;
                        }

                        logger.debug("checkBeforePromoteMCOToImplementedState() - sMCOID = <" + sMCOID + "> bCanPromoteMCO = <" + bCanPromoteMCO + ">");
                        if (bCanPromoteMCO) {
                            String POLICY_PSS_MFG_CHANGE_ORDER = getSchemaProperty(context, SYMBOLIC_policy_PSS_MfgChangeOrder);
                            String STATE_IMPLEMENTED = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_MFG_CHANGE_ORDER, "state_Implemented");

                            // promote MCO to Implemented state
                            doMCO.setState(context, STATE_IMPLEMENTED);
                        }

                    }

                }
            }

        } catch (MatrixException e) {
            logger.error("Error in checkAndPromoteAutomaticallyMCOToImplementedState()\n", e);
            throw e;
        }
        return 0;
    }
}
