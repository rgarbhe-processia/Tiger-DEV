
/*
 ** ${CLASS:PSS_emxPart} Cloned from emxPart JPO
 */
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeOrder;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.engineering.Part;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;
import matrix.util.StringUtils;
import pss.constants.TigerConstants;

public class PSS_emxECO_mxJPO extends emxECOBase_mxJPO {

    // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PSS_emxECO_mxJPO.class);

    // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - END
    public PSS_emxECO_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
        // TODO Auto-generated constructor stub
    }

    /**
     * Method to initiate the BOM Go To Production on a Part/Assembly and to connect the production Parts to an ECO
     * @param context
     * @param args
     * @return Strnig
     * @throws Exception
     */
    @com.matrixone.apps.framework.ui.PostProcessCallable
    public String bomGotoProduction(Context context, String[] args) throws Exception {
        try {

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap paramMap = (HashMap) programMap.get("paramMap");
            HashMap requestMap = (HashMap) programMap.get("requestMap");

            String strCOId = programMap.containsKey("changeId") ? (String) programMap.get("changeId") : (String) paramMap.get("objectId");
            // TIGTK-13602 : When Change object is not selected, set it as blank
            if (UIUtil.isNullOrEmpty(strCOId))
                strCOId = "";

            String strObjId = programMap.containsKey("selectedPartId") ? (String) programMap.get("selectedPartId") : (String) requestMap.get("selectedPartId");
            if (strObjId == null) {
                return "Create CO from Global Action";
            }

            bomGotoProduction(context, strCOId, strObjId);
            return "";
        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in bomGotoProduction: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
    }

    /**
     * Method to initiate the BOM Go To Production on a Part/Assembly and to connect the production Parts to an ECO
     * @param context
     * @param args
     * @return Strnig
     * @throws Exception
     */
    public void bomGotoProduction(Context context, String strChangeId, String strRootPartId) throws Exception {

        ContextUtil.startTransaction(context, true);

        context.setCustomData("BOM_GO_TO_PRODUCTION", "TRUE");

        try {
            StringBuffer sbSelectedParts = new StringBuffer();
            StringList objectSelects = new StringList(DomainConstants.SELECT_POLICY);
            objectSelects.addElement(DomainConstants.SELECT_ID);
            objectSelects.addElement(DomainConstants.SELECT_VAULT);
            objectSelects.add(DomainConstants.SELECT_LAST_ID);
            objectSelects.add("last.policy");

            DomainObject rootPartDomainObject = new DomainObject(strRootPartId);

            MapList mlEBOM = rootPartDomainObject.getRelatedObjects(context, RELATIONSHIP_EBOM, TYPE_PART, objectSelects, null, false, true, (short) 0, null, null, 0);
            Part part = new Part();
            String strNewPartID = "";
            part.setId(strRootPartId);
            String strVault = part.getInfo(context, DomainConstants.SELECT_VAULT);
            String strRevision = part.getNextSequence(context);
            PropertyUtil.setGlobalRPEValue(context, "PSS_REVISE_FROM_GOTOPROD" + strRootPartId, "REVISE_FROM_GOTOPROD");
            String strTargetRootPart = part.triggerPolicyDevelopmentPartStateReviewPromoteAction(context, TigerConstants.POLICY_PSS_ECPART, strRevision, strVault);
            PropertyUtil.setGlobalRPEValue(context, "PSS_REVISE_FROM_GOTOPROD" + strRootPartId, "");
            part.floatEBOMrelationship(context, strRootPartId, strTargetRootPart);
            sbSelectedParts.append(strTargetRootPart);

            StringList lstTargetParts1 = new StringList();
            Map lstTargetParts = new HashMap();
            lstTargetParts1.add(strTargetRootPart);
            Iterator iterator = mlEBOM.iterator();
            StringList lstPartId = new StringList();
            StringList newPartList = new StringList();

            while (iterator.hasNext()) {
                Map localMap = (Map) iterator.next();
                String strPolicy = (String) localMap.get(DomainConstants.SELECT_POLICY);
                String strLastID = (String) localMap.get(DomainConstants.SELECT_LAST_ID);
                if (TigerConstants.POLICY_PSS_DEVELOPMENTPART.equalsIgnoreCase(strPolicy)) {
                    String strObjectId = (String) localMap.get(DomainConstants.SELECT_ID);
                    String strLastPolicy = (String) localMap.get("last.policy");
                    if (!lstPartId.contains(strObjectId)) {
                        part.setId(strObjectId);
                        strRevision = part.getNextSequence(context);
                        strVault = (String) localMap.get(DomainConstants.SELECT_VAULT);
                        if (!strLastPolicy.equalsIgnoreCase(TigerConstants.POLICY_PSS_ECPART)) {
                            Part objPart = new Part(strLastID);
                            PropertyUtil.setGlobalRPEValue(context, "PSS_REVISE_FROM_GOTOPROD" + strLastID, "REVISE_FROM_GOTOPROD");
                            strNewPartID = objPart.triggerPolicyDevelopmentPartStateReviewPromoteAction(context, TigerConstants.POLICY_PSS_ECPART, strRevision, strVault);
                            PropertyUtil.setGlobalRPEValue(context, "PSS_REVISE_FROM_GOTOPROD" + strLastID, "");
                            // localObject1 = new Part(strLastID).triggerPolicyDevelopmentPartStateReviewPromoteAction(context, POLICY_PSS_EC_PART, strRevision, strVault);
                            sbSelectedParts.append("," + (String) strNewPartID);
                            lstTargetParts1.add((String) strNewPartID);
                        } else {
                            strNewPartID = strLastID;
                        }
                        lstPartId.addElement(strObjectId);
                        newPartList.addElement((String) strNewPartID);
                    }
                }
            }

            for (int i = 0; i < lstPartId.size(); i++) {
                part.floatEBOMrelationship(context, (String) lstPartId.get(i), (String) newPartList.get(i));
            }
            HashMap paramHashMap = new HashMap();
            paramHashMap.put("selectedItems", StringUtils.split(sbSelectedParts.toString(), ","));
            paramHashMap.put("changeId", strChangeId);
            paramHashMap.put("relType", "relationship_AffectedItem");

            if (UIUtil.isNotNullAndNotEmpty(strChangeId)) {
                DomainObject changeObject = DomainObject.newInstance(context, strChangeId);
                lstTargetParts.put("busObjId", (String) strChangeId);
                // TIGTK-4668 : 24-02-2017 : START
                lstTargetParts.put("selectedItemsList", lstTargetParts1);
                // lstTargetParts.put("strSelectedItemsWithPartsSymmetricalAndCAD", lstTargetParts1);
                // TIGTK-4668 : 24-02-2017 : END
                String[] args = JPO.packArgs(lstTargetParts);
                PSS_enoECMChangeUtil_mxJPO enoECM = new PSS_enoECMChangeUtil_mxJPO(context, args);
                if (changeObject.isKindOf(context, TigerConstants.TYPE_PSS_CHANGEREQUEST)) {

                    // Added for TIGTK-3124:Start

                    enoECM.connectCRToAffectedItem(context, args);
                    // Added for TIGTK-3124:End
                    // TIGTK-13602 : START
                } else if (changeObject.isKindOf(context, TigerConstants.TYPE_PSS_CHANGEORDER)) {
                    // Get List of valid affected Items
                    PSS_enoECMChangeOrder_mxJPO enoECMChangeOrderJPO = new PSS_enoECMChangeOrder_mxJPO(context, args);
                    StringList returnList = enoECMChangeOrderJPO.processCOAffectedItems(context, args);

                    // Connect Affected Items to CO
                    ChangeOrder changeOrder = new ChangeOrder(strChangeId);
                    changeOrder.connectAffectedItems(context, returnList);

                    // Connect Route Template to CA
                    String[] strArgs = new String[2];
                    strArgs[0] = strChangeId;
                    strArgs[1] = null;
                    PSS_enoECMChangeRequest_mxJPO enoECMCRJPO = new PSS_enoECMChangeRequest_mxJPO(context, args);
                    enoECMCRJPO.connectRouteTemplateToChangeAction(context, strArgs);

                    // Set CA Type
                    Map argMap = new HashMap();
                    argMap.put("strCOObjectID", (String) strChangeId);
                    String[] argsArr = JPO.packArgs(argMap);
                    enoECM.setCATypeonNewlyCreatedCA(context, argsArr);

                    // Set PSS_TRANSFERFROMCRFLAG to indicate affected items are directly added on CO and not transfer from CR.
                    Map mpParam = new HashMap();
                    mpParam.put("strCOObjectID", strChangeId);
                    PSS_enoECMChangeOrder_mxJPO enoECMCOJPO = new PSS_enoECMChangeOrder_mxJPO(context, args);
                    enoECMCOJPO.setFlagForMoveToExistingCO(context, JPO.packArgs(mpParam));
                }
                // TIGTK-13602 : END
            }
            ContextUtil.commitTransaction(context);
        } catch (Exception ex) {
            ContextUtil.abortTransaction(context);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            ex.printStackTrace();
            logger.error("Error in bomGotoProduction: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw ex;
        } finally {
            context.removeFromCustomData("BOM_GO_TO_PRODUCTION");
        }
        // return (String)(String)getId();
    }

    /**
     * This method is called from form:BOMGoToProduction field:CreateOrSelectECO to get html output.
     * @param context
     *            ematrix context
     * @param args
     *            packed arguments
     * @return String which is used as form field href.
     * @throws Exception
     *             if any operation fails.
     */
    public String getChangeFieldForBomGoToProduction(Context context, String[] args) throws Exception {
        String AddExisting = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(), "emxFramework.Command.AddExisting");
        // String createNewCO = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(), "emxFramework.Command.CreateNewCO");
        String createNewCR = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(), "emxFramework.Command.CreateNewCR");

        StringBuffer sbHTMLTag = new StringBuffer(800);

        /*
         * sbHTMLTag.append("<input type=radio name='ecoOptions' value='createNewCO' onclick='javascript:hideSearchOption()' checked='checked'>").append(createNewCO).append("<br>") .append(
         * "<input type=radio name='ecoOptions' value='createNewCR' onclick='javascript:hideSearchOption()'>").append(createNewCR).append("<br>") .append(
         * "<input type=radio name='ecoOptions' value='AddExisting' onclick='javascript:showSearchOption()'>").append(AddExisting);
         */
        // Findbug Issue correction start
        // Date: 22/03/2017
        // By: Asha G.
        // TIGTK-13602 : 02-04-2018 : START
        sbHTMLTag.append("<input type=radio name='ecoOptions' value='createNewCO' onclick='javascript:hideSearchOption()'>");
        sbHTMLTag.append("Create Change Order");
        sbHTMLTag.append("<br>");
        // TIGTK-13602 : 02-04-2018 : END

        sbHTMLTag.append("<input type=radio name='ecoOptions' value='createNewCR' onclick='javascript:hideSearchOption()'>");
        sbHTMLTag.append(createNewCR);
        sbHTMLTag.append("<br>");
        sbHTMLTag.append("<input type=radio name='ecoOptions' value='AddExisting' onclick='javascript:showSearchOption()'>");
        sbHTMLTag.append(AddExisting);
        // Findbug Issue correction end
        sbHTMLTag.append("<script language=javascript>");
        // TIGTK-10420 : TS : 11/10/2017: Start
        String strUserName = context.getUser();
        String strLoggedUserSecurityContext = PersonUtil.getDefaultSecurityContext(context, strUserName);
        String assignedRoles = (strLoggedUserSecurityContext.split("[.]")[0]);
        if (assignedRoles.equalsIgnoreCase(TigerConstants.ROLE_PSS_GLOBAL_ADMINISTRATOR) || assignedRoles.equalsIgnoreCase(TigerConstants.ROLE_PSS_PROGRAM_MANAGER)
                || assignedRoles.equalsIgnoreCase(TigerConstants.ROLE_PSS_CHANGE_COORDINATOR) || assignedRoles.equalsIgnoreCase(TigerConstants.ROLE_PSS_PLM_SUPPORT_TEAM)) {
            sbHTMLTag.append("function hideSearchOption() {");
            sbHTMLTag.append("document.editDataForm.btnCOToReleaseSubmit.disabled = true;");
            sbHTMLTag.append("document.editDataForm.COToReleaseSubmitDisplay.disabled = true;");
            sbHTMLTag.append("basicClear('COToReleaseSubmit');");
            sbHTMLTag.append('}');
            sbHTMLTag.append("function showSearchOption() {");
            sbHTMLTag.append("document.editDataForm.btnCOToReleaseSubmit.disabled = false;");
            sbHTMLTag.append("document.editDataForm.COToReleaseSubmitDisplay.disabled = false;");
            sbHTMLTag.append('}');

        } else {

            sbHTMLTag.append("function hideSearchOption() {");
            sbHTMLTag.append("document.editDataForm.btnCOToRelease.disabled = true;");
            sbHTMLTag.append("document.editDataForm.COToReleaseDisplay.disabled = true;");
            sbHTMLTag.append("basicClear('COToRelease');");
            sbHTMLTag.append('}');
            sbHTMLTag.append("function showSearchOption() {");
            sbHTMLTag.append("document.editDataForm.btnCOToRelease.disabled = false;");
            sbHTMLTag.append("document.editDataForm.COToReleaseDisplay.disabled = false;");
            sbHTMLTag.append('}');
        }
        // TIGTK-10420 : TS : 11/10/2017: End

        sbHTMLTag.append("</script>");

        return sbHTMLTag.toString();
    }

}
