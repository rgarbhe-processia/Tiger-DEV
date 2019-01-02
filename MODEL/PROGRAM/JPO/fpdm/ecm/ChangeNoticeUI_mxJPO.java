package fpdm.ecm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.XSSUtil;
import com.mbom.modeler.utility.FRCMBOMModelerUtility;

import matrix.db.Access;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;
import pss.constants.TigerConstants;

public class ChangeNoticeUI_mxJPO extends fpdm.ecm.Constants_mxJPO {

    private static Logger logger = LoggerFactory.getLogger("fpdm.ecm.ChangeNoticeUI");

    /**
     * This Method is used on the Item to Transfer Tab to fetch the contents Table. Displays all linked MBOM 100
     * @plm.usage Table: ECMCOAffectedItemsTable Command: FPDM_CNItemsToTransferMBOMPart
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            Change Notice object ID
     * @return MapList
     * @throws Exception
     */
    public MapList getMBOMPartItemsToTransfer(Context context, String[] args) throws Exception {

        MapList mlRelatedMBOMParts = null;

        try {
            Map<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            String sCNId = (String) programMap.get("objectId");
            logger.debug("getMBOMPartItemsToTransfer() - sCNId = <" + sCNId + ">");

            StringList objSelects = new StringList();
            objSelects.addElement(SELECT_ID);
            objSelects.addElement(SELECT_TYPE);
            objSelects.addElement(SELECT_NAME);
            objSelects.addElement(SELECT_REVISION);
            objSelects.addElement(SELECT_OWNER);
            objSelects.addElement(SELECT_POLICY);
            objSelects.addElement(SELECT_CURRENT);

            String RELATIONSHIP_CN_AFFECTED_ITEMS = getSchemaProperty(context, SYMBOLIC_relationship_FPDM_CNAffectedItems);
            String TYPE_FPDMMBOMPART = getSchemaProperty(context, SYMBOLIC_type_FPDM_MBOMPart);

            DomainObject doChangeNotice = DomainObject.newInstance(context, sCNId);

            mlRelatedMBOMParts = doChangeNotice.getRelatedObjects(context, RELATIONSHIP_CN_AFFECTED_ITEMS, TYPE_FPDMMBOMPART, objSelects, new StringList(SELECT_RELATIONSHIP_ID), false, true,
                    (short) 1, null, null, 0);

        } catch (Exception e) {
            logger.error("Error in getMBOMPartItemsToTransfer()\n", e);
            throw e;
        }
        return mlRelatedMBOMParts;

    }

    /**
     * This Method is used on the Item to Transfer Tab to fetch the contents Table. Displays all linked MBOM Tools
     * @plm.usage Table: ECMCOAffectedItemsTable Command: FPDM_CNItemsToTransferTools
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            Change Notice object ID
     * @return MapList
     * @throws Exception
     */
    public MapList getToolsItemsToTransfer(Context context, String[] args) throws Exception {

        MapList mlRelatedTools = null;

        try {
            Map<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            String sCNId = (String) programMap.get("objectId");
            logger.debug("getToolsItemsToTransfer() - sCNId = <" + sCNId + ">");

            StringList objSelects = new StringList();
            objSelects.addElement(SELECT_ID);
            objSelects.addElement(SELECT_TYPE);
            objSelects.addElement(SELECT_NAME);
            objSelects.addElement(SELECT_REVISION);
            objSelects.addElement(SELECT_OWNER);
            objSelects.addElement(SELECT_POLICY);
            objSelects.addElement(SELECT_CURRENT);

            String RELATIONSHIP_CN_AFFECTED_ITEMS = getSchemaProperty(context, SYMBOLIC_relationship_FPDM_CNAffectedItems);
            String TYPE_VPM_REFERENCE = getSchemaProperty(context, SYMBOLIC_type_VPMReference);

            DomainObject doChangeNotice = DomainObject.newInstance(context, sCNId);

            mlRelatedTools = doChangeNotice.getRelatedObjects(context, RELATIONSHIP_CN_AFFECTED_ITEMS, TYPE_VPM_REFERENCE, objSelects, new StringList(DomainConstants.SELECT_RELATIONSHIP_ID), false,
                    true, (short) 1, null, null, 0);

        } catch (Exception e) {
            logger.error("Error in getToolsItemsToTransfer()\n", e);
            throw e;
        }
        return mlRelatedTools;

    }

    /**
     * Include OID Program for Add Existing Parts on the Items to Transfer Tab of the Change Notice
     * @plm.usage Command: FPDM_CNItemsToTransferAddMBOMPart
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            Change Notice object ID
     * @return StringList
     * @throws Exception
     */
    public StringList getSelectableMBOMParts(Context context, String[] args) throws Exception {
        StringList sl100MBOM = new StringList();
        try {
            String RELATIONSHIP_RELATEDCN = getSchemaProperty(context, SYMBOLIC_relationship_PSS_RelatedCN);
            String RELATIONSHIP_FPDMGENERATEDMBOM = getSchemaProperty(context, SYMBOLIC_relationship_FPDM_GeneratedMBOM);
            String RELATIONSHIP_CN_AFFECTED_ITEMS = getSchemaProperty(context, SYMBOLIC_relationship_FPDM_CNAffectedItems);
            String RELATIONSHIP_FPDM_SCOPELINK = getSchemaProperty(context, SYMBOLIC_relationship_FPDM_ScopeLink);
            String RELATIONSHIP_PSS_MFGRELATEDPLANT = getSchemaProperty(context, SYMBOLIC_relationship_PSS_MfgRelatedPlant);

            Map<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            String sCNID = (String) programMap.get("objectId");
            logger.debug("getSelectableMBOMParts() - sCNID = <" + sCNID + ">");

            DomainObject doCN = DomainObject.newInstance(context, sCNID);
            String sMCOId = doCN.getInfo(context, "to[" + RELATIONSHIP_RELATEDCN + "].from.id");
            logger.debug("getSelectableMBOMParts() - sMCOId = <" + sMCOId + ">");

            DomainObject domMCO = DomainObject.newInstance(context, sMCOId);
            String sMCOPlantId = domMCO.getInfo(context, "from[" + RELATIONSHIP_PSS_MFGRELATEDPLANT + "].to.id");
            logger.debug("getSelectableMBOMParts() - sMCOPlantId = <" + sMCOPlantId + ">");

            String[] saMBOM150 = getMCOLinkedReleasedMBOM150(context, sMCOId);
            logger.debug("getSelectableMBOMParts() - saMBOM150=<" + Arrays.toString(saMBOM150) + ">");

            if (saMBOM150 != null) {
                logger.debug("getSelectableMBOMParts() - saMBOM150=<" + Arrays.toString(saMBOM150) + ">");
                StringList objSelects = new StringList();
                objSelects.add(SELECT_ID);
                objSelects.add("from[" + RELATIONSHIP_FPDMGENERATEDMBOM + "].to.id");
                objSelects.add("from[" + RELATIONSHIP_FPDM_SCOPELINK + "].to.id");

                // get MBOM 100 object info
                Map<String, Map<String, Object>> mMBOM150Info = fpdm.utils.SelectData_mxJPO.getSelectBusinessObjectData(context, saMBOM150, objSelects);
                logger.debug("getSelectableMBOMParts() - mMBOM150Info = <" + mMBOM150Info + ">");

                if (mMBOM150Info.size() > 0) {
                    // get already linked MBOM 100 object info
                    StringList slAlreadyLinkedMBOM100 = doCN.getInfoList(context, "from[" + RELATIONSHIP_CN_AFFECTED_ITEMS + "].to.id");

                    DomainObject doMBOM100 = DomainObject.newInstance(context);
                    for (Map<String, Object> mMBOM150 : mMBOM150Info.values()) {
                        ArrayList<String> alLinked100 = fpdm.utils.SelectData_mxJPO.getListOfValues(mMBOM150.get("from[" + RELATIONSHIP_FPDMGENERATEDMBOM + "].to.id"));
                        for (Iterator<String> iterator = alLinked100.iterator(); iterator.hasNext();) {
                            String sMBOM100ID = iterator.next();
                            if (!slAlreadyLinkedMBOM100.contains(sMBOM100ID)) {
                                doMBOM100.setId(sMBOM100ID);
                                String sLinkedPlant = doMBOM100.getInfo(context, "from[" + RELATIONSHIP_FPDM_SCOPELINK + "].to.id");
                                logger.debug("getSelectableMBOMParts() - sMBOM100ID = <" + sMBOM100ID + "> sLinkedPlant = <" + sLinkedPlant + "> sMCOPlantId = <" + sMCOPlantId + ">");
                                if (sLinkedPlant != null && sLinkedPlant.equals(sMCOPlantId)) {
                                    sl100MBOM.add(sMBOM100ID);
                                }
                            }
                        }
                    }

                }
            }
            if (sl100MBOM.isEmpty()) {
                // The includeOIDprogram must return an element (null or " ") when there no object found to avoid an IndexOutOfBoundsException .
                sl100MBOM.add(null);
            }
            logger.debug("getSelectableMBOMParts() - sl100MBOM=<" + sl100MBOM + ">");

        } catch (Exception e) {
            logger.error("Error in getSelectableMBOMParts()\n", e);
            throw e;
        }
        return sl100MBOM;
    }

    /**
     * Get MCO Related 150% Parts
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sMCOId
     *            Manufacturing Change Order object ID
     * @return
     * @throws Exception
     */
    private String[] getMCOLinkedReleasedMBOM150(Context context, String sMCOId) throws Exception {

        String[] saAll150Objects = null;
        ArrayList<String> alAllLinkedMBOM150 = new ArrayList<String>();
        String RELATIONSHIP_MFGCHANGEACTION = getSchemaProperty(context, SYMBOLIC_relationship_PSS_MfgChangeAction);
        String RELATIONSHIP_MFGCHANGEAFFECTEDITEM = getSchemaProperty(context, SYMBOLIC_relationship_PSS_MfgChangeAffectedItem);
        String RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS = TigerConstants.RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS;

        // get linked MCA
        Object oLinkedMCA = fpdm.utils.SelectData_mxJPO.getSelectBusinessObjectData(context, sMCOId, "from[" + RELATIONSHIP_MFGCHANGEACTION + "].to.id");
        ArrayList<String> alLinkedMCA = fpdm.utils.SelectData_mxJPO.getListOfValues(oLinkedMCA);
        logger.debug("getMCOLinkedReleasedMBOM150() - alLinkedMCA = <" + alLinkedMCA + ">");

        if (alLinkedMCA.size() > 0) {
            // get linked MBOM 150 connected to found MCAs
            StringList slSelect = new StringList();
            slSelect.addElement("from[" + RELATIONSHIP_MFGCHANGEAFFECTEDITEM + "].to.id");
            slSelect.addElement("from[" + RELATIONSHIP_MFGCHANGEAFFECTEDITEM + "].to.current");
            String[] saMCA = alLinkedMCA.toArray(new String[alLinkedMCA.size()]);
            Map<String, Map<String, Object>> mLinkedMBOM150 = fpdm.utils.SelectData_mxJPO.getSelectBusinessObjectData(context, saMCA, slSelect);
            logger.debug("getMCOLinkedReleasedMBOM150() - mLinkedMBOM150 = <" + mLinkedMBOM150 + ">");

            if (mLinkedMBOM150.size() > 0) {
                ArrayList<String> alMBOMIds = null;
                ArrayList<String> alMBOMStates = null;
                for (Map<String, Object> mMBOM150 : mLinkedMBOM150.values()) {
                    alMBOMIds = fpdm.utils.SelectData_mxJPO.getListOfValues(mMBOM150.get("from[" + RELATIONSHIP_MFGCHANGEAFFECTEDITEM + "].to.id"));
                    alMBOMStates = fpdm.utils.SelectData_mxJPO.getListOfValues(mMBOM150.get("from[" + RELATIONSHIP_MFGCHANGEAFFECTEDITEM + "].to.current"));

                    Iterator<String> itMBOMID = alMBOMIds.iterator();
                    Iterator<String> itMBOMState = alMBOMStates.iterator();

                    while (itMBOMState.hasNext()) {
                        String sCurrent = (String) itMBOMState.next();
                        String sID = (String) itMBOMID.next();

                        if ("Released".equals(sCurrent) && !alAllLinkedMBOM150.contains(sID)) {
                            alAllLinkedMBOM150.add(sID);
                        }

                    }
                }
            }
        }
        logger.debug("getMCOLinkedReleasedMBOM150() - All released linked MBOM 150 = <" + alAllLinkedMBOM150 + ">");

        if (alAllLinkedMBOM150.size() > 0) {
            saAll150Objects = alAllLinkedMBOM150.toArray(new String[alAllLinkedMBOM150.size()]);

            // get Material 150 connected to found MBOM 150
            StringList slSelect = new StringList();
            slSelect.addElement("from[" + RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS + "].to.id");
            slSelect.addElement("from[" + RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS + "].to.current");
            slSelect.addElement("from[" + RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS + "].to.policy");

            Map<String, Map<String, Object>> mLinkedMaterial150 = fpdm.utils.SelectData_mxJPO.getSelectBusinessObjectData(context, saAll150Objects, slSelect);
            logger.debug("getMCOLinkedReleasedMBOM150() - mLinkedMaterial150 = <" + mLinkedMaterial150 + ">");

            if (mLinkedMaterial150.size() > 0) {
                ArrayList<String> alMaterialIds = null;
                ArrayList<String> alMaterialStates = null;
                ArrayList<String> alMaterialPolicies = null;
                for (Map<String, Object> mMBOM150 : mLinkedMaterial150.values()) {
                    alMaterialIds = fpdm.utils.SelectData_mxJPO.getListOfValues(mMBOM150.get("from[" + RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS + "].to.id"));
                    alMaterialStates = fpdm.utils.SelectData_mxJPO.getListOfValues(mMBOM150.get("from[" + RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS + "].to.current"));
                    alMaterialPolicies = fpdm.utils.SelectData_mxJPO.getListOfValues(mMBOM150.get("from[" + RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS + "].to.policy"));

                    Iterator<String> itMaterialID = alMaterialIds.iterator();
                    Iterator<String> itMaterialState = alMaterialStates.iterator();
                    Iterator<String> itMaterialPolicy = alMaterialPolicies.iterator();

                    while (itMaterialState.hasNext()) {
                        String sCurrent = (String) itMaterialState.next();
                        String sID = (String) itMaterialID.next();
                        String sPolicy = (String) itMaterialPolicy.next();

                        if (("PSS_MBOM".equals(sPolicy) && "Released".equals(sCurrent) || ("PSS_Material".equals(sPolicy) && "Approved".equals(sCurrent))) && !alAllLinkedMBOM150.contains(sID)) {
                            alAllLinkedMBOM150.add(sID);
                        }

                    }
                }

                saAll150Objects = alAllLinkedMBOM150.toArray(new String[alAllLinkedMBOM150.size()]);
                logger.debug("getMCOLinkedReleasedMBOM150() - All released linked MBOM 150 and Material 150 = <" + alAllLinkedMBOM150 + ">");
            }
        }

        return saAll150Objects;
    }

    /**
     * Include OID Program for Add Existing Tools on the Items to Transfer Tab of the Change Notice
     * @plm.usage Command: FPDM_CNItemsToTransferAddTools
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            Change Notice object ID
     * @return StringList
     * @throws Exception
     */
    @SuppressWarnings("deprecation")
    public StringList getSelectableMBOMTools(Context context, String[] args) throws Exception {
        StringList slTools = new StringList();
        com.dassault_systemes.vplm.modeler.PLMCoreModelerSession plmSession = null;
        try {
            String RELATIONSHIP_RELATEDCN = getSchemaProperty(context, SYMBOLIC_relationship_PSS_RelatedCN);
            String POLICY_PSS_TOOL = getSchemaProperty(context, SYMBOLIC_policy_PSS_Tool);
            Map<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            String sCNID = (String) programMap.get("objectId");
            logger.debug("getSelectableMBOMTools() - sCNID = <" + sCNID + ">");

            DomainObject doCN = DomainObject.newInstance(context, sCNID);
            String sMCOId = doCN.getInfo(context, "to[" + RELATIONSHIP_RELATEDCN + "].from.id");
            logger.debug("getSelectableMBOMParts() - sMCOId = <" + sMCOId + ">");

            String[] saMBOM150 = getMCOLinkedReleasedMBOM150(context, sMCOId);
            logger.debug("getSelectableMBOMTools() - saMBOM150 = <" + Arrays.toString(saMBOM150) + ">");

            if (saMBOM150 != null) {
                for (int i = 0; i < saMBOM150.length; i++) {

                    String strAssemblyId = saMBOM150[i];
                    if (strAssemblyId != null && !"".equalsIgnoreCase(strAssemblyId)) {
                        DomainObject domAssembly = new DomainObject(strAssemblyId);
                        String strAssemblyPhyId = domAssembly.getInfo(context, SELECT_PHYSICALID);
                        try {
                            ContextUtil.startTransaction(context, false);

                            context.setApplication("VPLM");
                            plmSession = com.dassault_systemes.vplm.modeler.PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
                            plmSession.openSession();
                            List<?> lConnectedTools = FRCMBOMModelerUtility.getResourcesAttachedToMBOMReference(context, plmSession, strAssemblyPhyId);
                            logger.debug("getSelectableMBOMTools() - lConnectedTools = <" + lConnectedTools + ">");
                            for (int itr = 0; itr < lConnectedTools.size(); itr++) {
                                String strToolPhyId = (String) lConnectedTools.get(itr);
                                DomainObject domTool = DomainObject.newInstance(context, strToolPhyId);
                                String strPolicy = domTool.getInfo(context, SELECT_POLICY);
                                String strToolId = domTool.getInfo(context, SELECT_ID);
                                logger.debug("getSelectableMBOMTools() - strToolId = <" + strToolId + "> strPolicy = <" + strPolicy + "> POLICY_PSS_TOOL = <" + POLICY_PSS_TOOL + ">");

                                if (strPolicy.equalsIgnoreCase(POLICY_PSS_TOOL)) {
                                    slTools.add(strToolId);
                                }

                            }
                            // closeSession(plmSession);
                            plmSession.closeSession(true);
                            ContextUtil.commitTransaction(context);
                        } catch (Exception e) {
                            e.printStackTrace();
                            // closeSession(plmSession);
                            plmSession.closeSession(true);
                            ContextUtil.abortTransaction(context);
                        }
                    }
                }
            }
            if (slTools.isEmpty()) {
                // The includeOIDprogram must return an element (null or " ") when there no object found to avoid an IndexOutOfBoundsException .
                slTools.add(null);
            }
            logger.debug("getSelectableMBOMTools() - slTools=<" + slTools + ">");

        } catch (Exception e) {
            logger.error("Error in getSelectableMBOMTools()\n", e);
            throw e;
        }
        return slTools;
    }

    /**
     * Access Function for the Add Existing Parts on Change Notice in Item to Transfer Tab
     * @plm.usage Command: FPDM_CNItemsToTransferAddMBOMParts
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            Change Notice object ID
     * @return Boolean
     * @throws Exception
     */
    public boolean checkAddPartsAccess(Context context, String[] args) throws Exception {
        boolean bHasAccess = false;
        try {
            Map<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            String sCNID = (String) programMap.get("objectId");
            String currentUser = context.getUser();
            DomainObject domChangeNotice = new DomainObject(sCNID);
            String strObjOwner = domChangeNotice.getInfo(context, SELECT_OWNER);

            if (strObjOwner.equalsIgnoreCase(currentUser)) {
                String strCNType = domChangeNotice.getInfo(context, "attribute[" + getSchemaProperty(context, SYMBOLIC_attribute_PSS_CN_Type) + "]");
                if (!ATTRIBUTE_PSS_CN_TYPE_RANGE_TOOL.equalsIgnoreCase(strCNType)) {
                    bHasAccess = true;
                }
            }
            logger.debug("checkAddPartsAccess() - bHasAccess = <" + bHasAccess + ">");
        } catch (Exception e) {
            logger.error("Error in checkAddPartsAccess()\n", e);
            throw e;
        }
        return bHasAccess;
    }

    /**
     * Access Function for the Add Existing Tools on Change Notice in Item to Transfer Tab
     * @plm.usage Comnand: FPDM_CNItemsToTransferAddMBOMTools
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            Change Notice object ID
     * @return Boolean
     * @throws Exception
     */
    public boolean checkAddToolsAccess(Context context, String[] args) throws Exception {
        boolean bHasAccess = false;
        try {
            Map<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            String sCNID = (String) programMap.get("objectId");
            String currentUser = context.getUser();
            DomainObject domChangeNotice = new DomainObject(sCNID);
            String strObjOwner = domChangeNotice.getInfo(context, SELECT_OWNER);

            if (strObjOwner.equalsIgnoreCase(currentUser)) {
                String strCNType = domChangeNotice.getInfo(context, "attribute[" + getSchemaProperty(context, SYMBOLIC_attribute_PSS_CN_Type) + "]");
                if (ATTRIBUTE_PSS_CN_TYPE_RANGE_TOOL.equalsIgnoreCase(strCNType)) {
                    bHasAccess = true;
                }
            }
            logger.debug("checkAddToolsAccess() - bHasAccess = <" + bHasAccess + ">");
        } catch (Exception e) {
            logger.error("Error in checkAddToolsAccess()\n", e);
            throw e;
        }
        return bHasAccess;
    }

    /**
     * This Method is used on the Properties Form of the Change Notice to display the CN Related Program Project
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            Change Notice object ID
     * @return String
     * @throws Exception
     */
    public String getCNMCOProject(Context context, String[] args) throws Exception {
        String showRelObjectName = "";
        try {
            final String RELATIONSHIP_PSS_CONNECTEDPCMDATA = getSchemaProperty(context, SYMBOLIC_relationship_PSS_ConnectedPCMData);
            String RELATIONSHIP_RELATEDCN = getSchemaProperty(context, SYMBOLIC_relationship_PSS_RelatedCN);
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            HashMap<?, ?> requestMap = (HashMap<?, ?>) programMap.get("requestMap");
            String sCNID = (String) requestMap.get("objectId");

            DomainObject domChangeNotice = DomainObject.newInstance(context, sCNID);
            String strMCOId = domChangeNotice.getInfo(context, "to[" + RELATIONSHIP_RELATEDCN + "].from.id");
            DomainObject domMCO = DomainObject.newInstance(context, strMCOId);
            String strProgProjName = domMCO.getInfo(context, "to[" + RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.name");
            String strProgProjId = domMCO.getInfo(context, "to[" + RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");

            if (strProgProjName == null || "".equalsIgnoreCase(strProgProjName)) {
                strProgProjName = "";
            }
            String sRelatedObjectUrl = "./emxTree.jsp?objectId=" + XSSUtil.encodeForJavaScript(context, strProgProjId);
            showRelObjectName = "<a href=\"" + sRelatedObjectUrl + "\">" + XSSUtil.encodeForHTML(context, strProgProjName) + "</a>&#160;";
        } catch (Exception e) {
            logger.error("Error in getCNMCOProject()\n", e);
            throw e;
        }
        return showRelObjectName;
    }

    /**
     * This Method is used on the Properties Form of the Change Notice to display the CN Related Plant
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            Change Notice object ID
     * @return String
     * @throws Exception
     */
    public String getCNMCORelatedPlant(Context context, String[] args) throws Exception {
        String showRelObjectName = "";
        try {
            final String RELATIONSHIP_PSS_MFGRELATEDPLANT = getSchemaProperty(context, SYMBOLIC_relationship_PSS_MfgRelatedPlant);
            String RELATIONSHIP_RELATEDCN = getSchemaProperty(context, SYMBOLIC_relationship_PSS_RelatedCN);
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            HashMap<?, ?> requestMap = (HashMap<?, ?>) programMap.get("requestMap");
            String sCNID = (String) requestMap.get("objectId");

            DomainObject domChangeNotice = new DomainObject(sCNID);
            String strMCOId = domChangeNotice.getInfo(context, "to[" + RELATIONSHIP_RELATEDCN + "].from.id");

            DomainObject domMCO = DomainObject.newInstance(context, strMCOId);
            String strPlantName = domMCO.getInfo(context, "from[" + RELATIONSHIP_PSS_MFGRELATEDPLANT + "].to.name");
            String strPlantId = domMCO.getInfo(context, "from[" + RELATIONSHIP_PSS_MFGRELATEDPLANT + "].to.id");

            if (strPlantName == null || "".equalsIgnoreCase(strPlantName)) {
                strPlantName = "";
            }
            String sRelatedObjectUrl = "./emxTree.jsp?objectId=" + XSSUtil.encodeForJavaScript(context, strPlantId);
            showRelObjectName = "<a href=\"" + sRelatedObjectUrl + "\">" + XSSUtil.encodeForHTML(context, strPlantName) + "</a>&#160;";
        } catch (Exception e) {
            logger.error("Error in getCNMCORelatedPlant()\n", e);
            throw e;
        }

        return showRelObjectName;
    }

    /**
     * This Method is used on the Properties Form of the Change Notice to display the CN Related 150% MBOM
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            Change Notice object ID
     * @return String
     * @throws Exception
     */
    public String getCNMCO150MBOM(Context context, String[] args) throws Exception {
        String showRelObjectName = "";
        try {
            String RELATIONSHIP_RELATED150MBOM = getSchemaProperty(context, SYMBOLIC_relationship_PSS_Related150MBOM);
            String RELATIONSHIP_RELATEDCN = getSchemaProperty(context, SYMBOLIC_relationship_PSS_RelatedCN);
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            HashMap<?, ?> requestMap = (HashMap<?, ?>) programMap.get("requestMap");
            String sCNID = (String) requestMap.get("objectId");

            DomainObject domChangeNotice = new DomainObject(sCNID);
            String strMCOId = domChangeNotice.getInfo(context, "to[" + RELATIONSHIP_RELATEDCN + "].from.id");
            DomainObject domMCO = DomainObject.newInstance(context, strMCOId);
            String str150MBOMName = domMCO.getInfo(context, "from[" + RELATIONSHIP_RELATED150MBOM + "].to.name");
            String str150MBOMId = domMCO.getInfo(context, "from[" + RELATIONSHIP_RELATED150MBOM + "].to.id");
            if (str150MBOMName == null || "".equalsIgnoreCase(str150MBOMName)) {
                str150MBOMName = "";
            }

            String sRelatedObjectUrl = "./emxTree.jsp?objectId=" + XSSUtil.encodeForJavaScript(context, str150MBOMId);
            showRelObjectName = "<a href=\"" + sRelatedObjectUrl + "\">" + XSSUtil.encodeForHTML(context, str150MBOMName) + "</a>&#160;";
        } catch (Exception e) {
            logger.error("Error in getCNMCO150MBOM()\n", e);
            throw e;
        }

        return showRelObjectName;
    }

    /**
     * Access Function for the CN modification of Effectivity date
     * @plm.usage Comnand: FPDM_CNModifyEffectivityDate
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            Change Notice object ID
     * @return Boolean
     * @throws Exception
     */
    public boolean hasAccessToModifyEffectivityDate(Context context, String[] args) throws Exception {
        boolean bHasAccess = false;
        try {
            Map<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            String sCNID = (String) programMap.get("objectId");
            logger.debug("hasAccessToModifyEffectivityDate() - sCNID = <" + sCNID + ">");

            DomainObject doChangeNotice = DomainObject.newInstance(context, sCNID);

            // CN must be at Fully Integrated state
            String sCurrent = doChangeNotice.getInfo(context, SELECT_CURRENT);
            logger.debug("hasAccessToModifyEffectivityDate() - sCurrent = <" + sCurrent + ">");
            if ("Fully Integrated".equals(sCurrent)) {
                Access acAccessMask = doChangeNotice.getAccessMask(context);

                // Test if Business Object has the Create right on one RDO
                bHasAccess = acAccessMask.hasCreateAccess();
            }
            logger.debug("hasAccessToModifyEffectivityDate() - hasAccessToModifyEffectivityDate = <" + bHasAccess + ">");

        } catch (Exception e) {
            logger.error("Error in hasAccessToModifyEffectivityDate()\n", e);
            throw e;
        }
        return bHasAccess;
    }

    /**
     * Update Effectivity date value on CN and its related MBOMPart
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            Change Notice object ID
     * @throws Exception
     */
    public void modifyEffectivityDate(Context context, String[] args) throws Exception {
        try {
            Map<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            HashMap<?, ?> requestMap = (HashMap<?, ?>) programMap.get("requestMap");
            String sCNID = (String) requestMap.get("objectId");
            logger.debug("modifyEffectivityDate() - sCNID = <" + sCNID + ">");
            String sEffectivityDateOldValue = (String) requestMap.get("Effectivity DatefieldValue");
            logger.debug("modifyEffectivityDate() - sEffectivityDateOldValue = <" + sEffectivityDateOldValue + ">");

            DomainObject doChangeNotice = DomainObject.newInstance(context, sCNID);
            String sEffectivityDateNewValue = doChangeNotice.getInfo(context, "attribute[" + getSchemaProperty(context, SYMBOLIC_attribute_PSS_Effectivity_Date) + "].value");
            logger.debug("modifyEffectivityDate() - sEffectivityDateNewValue = <" + sEffectivityDateNewValue + ">");

            if (sEffectivityDateNewValue != null && !sEffectivityDateNewValue.equals(sEffectivityDateOldValue)) {
                // get already linked MBOM 100 object info
                StringList slAlreadyLinkedMBOM100 = doChangeNotice.getInfoList(context, "from[" + getSchemaProperty(context, SYMBOLIC_relationship_FPDM_CNAffectedItems) + "].to.id");

                String ATTRIBUTE_EFFECTIVITY_DATE = getSchemaProperty(context, SYMBOLIC_attribute_FPDM_EffectivityDate);
                DomainObject doMBOM100 = DomainObject.newInstance(context);
                for (Object object : slAlreadyLinkedMBOM100) {
                    doMBOM100.setId((String) object);
                    doMBOM100.setAttributeValue(context, ATTRIBUTE_EFFECTIVITY_DATE, sEffectivityDateNewValue);
                }
            }

        } catch (Exception e) {
            logger.error("Error in modifyEffectivityDate()\n", e);
            throw e;
        }

    }

    /**
     * Connect the selected MBOM 100 % and all their released children which are connected to the same Plant
     * @plm.usage JSP: FPDM_ConnectSelectedMBOMPartsToCN.jsp
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            contains CN ID and the selected MBOM 100 % IDs
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void connectSelectedMBOMParts(Context context, String[] args) throws Exception {
        try {
            String RELATIONSHIP_RELATEDCN = getSchemaProperty(context, SYMBOLIC_relationship_PSS_RelatedCN);
            String RELATIONSHIP_CN_AFFECTED_ITEMS = getSchemaProperty(context, SYMBOLIC_relationship_FPDM_CNAffectedItems);
            String RELATIONSHIP_PSS_MFGRELATEDPLANT = getSchemaProperty(context, SYMBOLIC_relationship_PSS_MfgRelatedPlant);

            String SELECT_CN_RELATED_PLANT_ID = "to[" + RELATIONSHIP_RELATEDCN + "].from.from[" + RELATIONSHIP_PSS_MFGRELATEDPLANT + "].to.id";
            String SELECT_CN_AFFECTED_ITEMS_ID = "from[" + RELATIONSHIP_CN_AFFECTED_ITEMS + "].to.id";

            StringList slSelectCN = new StringList();
            slSelectCN.addElement(SELECT_CN_RELATED_PLANT_ID);
            slSelectCN.addElement(SELECT_CN_AFFECTED_ITEMS_ID);

            Map<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            logger.debug("connectSelectedMBOMParts() - programMap = <" + programMap + ">");

            String sChangeNoticeId = (String) programMap.get("objectId");
            ArrayList<String> alSelectedMBOMParts = (ArrayList<String>) programMap.get("selectedMBOMParts");

            // get Plant related to the CN
            Map<String, Object> mCNInfos = fpdm.utils.SelectData_mxJPO.getSelectBusinessObjectData(context, sChangeNoticeId, slSelectCN);
            logger.debug("connectSelectedMBOMParts() - mCNInfos = <" + mCNInfos + ">");

            String sCNPlantId = (String) mCNInfos.get(SELECT_CN_RELATED_PLANT_ID);
            logger.debug("connectSelectedMBOMParts() - sCNPlantId = <" + sCNPlantId + ">");

            ArrayList<String> alAlreadyLinkedMBOM100 = fpdm.utils.SelectData_mxJPO.getListOfValues(mCNInfos.get(SELECT_CN_AFFECTED_ITEMS_ID));
            logger.debug("connectSelectedMBOMParts() - alAlreadyLinkedMBOM100 = <" + alAlreadyLinkedMBOM100 + ">");

            // get all selected MBOMPart children
            ArrayList<String> alConnectableMBOMPats = getConnectableMBOMPartsChildren(context, alSelectedMBOMParts, sCNPlantId);
            logger.debug("connectSelectedMBOMParts() - alConnectableMBOMPats = <" + alConnectableMBOMPats + ">");

            for (String sSelectedMBOMId : alConnectableMBOMPats) {
                if (!alAlreadyLinkedMBOM100.contains(sSelectedMBOMId)) {
                    DomainRelationship.connect(context, sChangeNoticeId, RELATIONSHIP_CN_AFFECTED_ITEMS, sSelectedMBOMId, false);
                    alAlreadyLinkedMBOM100.add(sSelectedMBOMId);
                }
            }

        } catch (Exception e) {
            logger.error("Error in connectSelectedMBOMParts()\n", e);
            throw e;
        }

    }

    /**
     * Expand select MBOM 100% to get all children. Return only objects at release state and connected to the same Plant
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param alSelectedMBOMParts
     *            Selected MBOM 100 % to expand
     * @param sCNPlantId
     *            The plant related to the CN (via the MCO)
     * @return
     * @throws Exception
     */
    private ArrayList<String> getConnectableMBOMPartsChildren(Context context, ArrayList<String> alSelectedMBOMParts, String sCNPlantId) throws Exception {
        ArrayList<String> alConnectableMBOMPats = new ArrayList<String>();

        String RELATIONSHIP_FPDMGENERATEDMBOM = getSchemaProperty(context, SYMBOLIC_relationship_FPDM_GeneratedMBOM);
        String RELATIONSHIP_FPDM_SCOPELINK = getSchemaProperty(context, SYMBOLIC_relationship_FPDM_ScopeLink);
        String SELECT_MBOMPart_RELATED_PLANT_ID = "from[" + RELATIONSHIP_FPDM_SCOPELINK + "].to.id";
        String SELECT_MBOMPart_RELATED_MBOM150_ID = "to[" + RELATIONSHIP_FPDMGENERATEDMBOM + "].from.id";
        String SELECT_MBOMPart_RELATED_MBOM150_CURRENT = "to[" + RELATIONSHIP_FPDMGENERATEDMBOM + "].from.current";

        StringList slSelectMBOMPart = new StringList();
        slSelectMBOMPart.addElement(SELECT_ID);
        slSelectMBOMPart.addElement(SELECT_MBOMPart_RELATED_PLANT_ID);
        slSelectMBOMPart.addElement(SELECT_MBOMPart_RELATED_MBOM150_ID);
        slSelectMBOMPart.addElement(SELECT_MBOMPart_RELATED_MBOM150_CURRENT);

        alConnectableMBOMPats.addAll(alSelectedMBOMParts);
        String sRelationPattern = getSchemaProperty(context, SYMBOLIC_relationship_FPDM_MBOMRel);
        String sTypePattern = getSchemaProperty(context, SYMBOLIC_type_FPDM_MBOMPart);
        DomainObject doMBOM100 = DomainObject.newInstance(context);

        MapList mlMBOM100 = null;
        Map<?, ?> mMBOM100Info = null;
        String sMBOMPartId = null;
        String sRelatedPlantId = null;
        String sRelatedMBOM150Current = null;
        for (String sSelectedMBOMId : alSelectedMBOMParts) {
            doMBOM100.setId(sSelectedMBOMId);
            // Call Expand
            mlMBOM100 = doMBOM100.getRelatedObjects(context, sRelationPattern, sTypePattern, slSelectMBOMPart, null, false, true, (short) 1, null, null, 0);

            for (Object oMBOM100 : mlMBOM100) {
                mMBOM100Info = (Map<?, ?>) oMBOM100;
                sMBOMPartId = (String) mMBOM100Info.get(SELECT_ID);
                sRelatedPlantId = (String) mMBOM100Info.get(SELECT_MBOMPart_RELATED_PLANT_ID);
                sRelatedMBOM150Current = (String) mMBOM100Info.get(SELECT_MBOMPart_RELATED_MBOM150_CURRENT);

                if (!alConnectableMBOMPats.contains(sMBOMPartId) && sCNPlantId.equals(sRelatedPlantId) && "Released".equals(sRelatedMBOM150Current)) {
                    alConnectableMBOMPats.add(sMBOMPartId);
                }
            }
        }

        return alConnectableMBOMPats;
    }

}
