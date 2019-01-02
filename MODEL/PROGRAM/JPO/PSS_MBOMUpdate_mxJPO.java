
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dassault_systemes.vplm.modeler.PLMCoreModelerSession;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.framework.ui.UIUtil;
import com.matrixone.plmql.cmd.PLMID;
import com.mbom.modeler.utility.FRCMBOMModelerUtility;

import matrix.db.Context;
import matrix.util.Pattern;
import matrix.util.StringList;
import pss.constants.TigerConstants;

public class PSS_MBOMUpdate_mxJPO extends FRCMBOMModelerUtility {

    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PSS_MBOMUpdate_mxJPO.class);

    public final static String PSS_SPARE_PART = "Spare Part";

    public PSS_MBOMUpdate_mxJPO() {
        // TODO Auto-generated constructor stub
    }

    public static void flushAndCloseSession(PLMCoreModelerSession plmSession) {
        try {
            plmSession.flushSession();
        } catch (Exception e) {
        }

        try {
            plmSession.closeSession(true);
        } catch (Exception e) {
        }
    }

    public static void updateMBOMEffectivities(Context context, String psLeafInstPID, String objPIDConnection) throws Exception {
        PLMCoreModelerSession plmSession = null;
        boolean isTransactionActive = false;
        try {
            ContextUtil.startTransaction(context, true);
            isTransactionActive = true;

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            DomainRelationship domRel = DomainRelationship.newInstance(context, psLeafInstPID);
            String hasEffectivity = domRel.getAttributeValue(context, "PLMInstance.V_hasConfigEffectivity");

            if (UIUtil.isNotNullAndNotEmpty(hasEffectivity) && hasEffectivity.equalsIgnoreCase("true")) {
                PSS_FRCMBOMModelerUtility_mxJPO.deleteImplementLink(context, plmSession, objPIDConnection, false);
            } else {
                PSS_FRCMBOMModelerUtility_mxJPO.deleteImplementLink(context, plmSession, objPIDConnection, true);
            }

            List<String> mbomLeafInstancePIDList = new ArrayList<String>();
            mbomLeafInstancePIDList.add(objPIDConnection);
            List<String> trimmedPSPathList = new ArrayList<String>();
            trimmedPSPathList.add(psLeafInstPID);

            PSS_FRCMBOMModelerUtility_mxJPO.setImplementLinkBulk(context, plmSession, mbomLeafInstancePIDList, trimmedPSPathList, false);
            FRCMBOMModelerUtility.updateImplementLinkFromCandidate(context, plmSession, objPIDConnection);

            flushAndCloseSession(plmSession);
            if (isTransactionActive) {
                ContextUtil.commitTransaction(context);
            }

        } catch (Exception ex) {
            logger.error("Error in updateMBOMEffectivities : ", ex);
            flushAndCloseSession(plmSession);
            if (isTransactionActive) {
                ContextUtil.abortTransaction(context);
            }
            throw ex;
        }
    }

    public static void updateMBOMColorOptions(Context context, String psPhysicalId, String mbomPhysicalId) throws Exception {
        try {
            DomainObject domPhysicalObject = DomainObject.newInstance(context, psPhysicalId);
            String strColorList = domPhysicalObject.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PUBLISHEDPART_PSS_COLORLIST);
            StringList slPublishedColorList = new StringList();
            StringList slMBOMColorList = new StringList();
            if (UIUtil.isNotNullAndNotEmpty(strColorList)) {
                if (strColorList.contains("|")) {
                    String[] strColorArray = strColorList.split("\\|");
                    if (strColorArray.length > 1) {
                        for (int i = 0; i < strColorArray.length; i++) {
                            slPublishedColorList.add(strColorArray[i]);
                        }
                    }
                } else {
                    slPublishedColorList.add(strColorList);
                }
            }
            DomainObject domMBOMObject = DomainObject.newInstance(context, mbomPhysicalId);

            MapList mlMBOMColorOptions = domMBOMObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_COLORLIST, // relationship pattern
                    TigerConstants.TYPE_PSS_COLOROPTION, // object pattern
                    new StringList("physicalid"), // object selects
                    new StringList(DomainConstants.SELECT_RELATIONSHIP_ID), // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 0, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    null, // postPattern
                    null, null, null);

            if (!mlMBOMColorOptions.isEmpty()) {
                Iterator itr = mlMBOMColorOptions.iterator();
                while (itr.hasNext()) {
                    Map mObjectMap = (Map) itr.next();
                    String strMBOMColorOption = (String) mObjectMap.get("physicalid");
                    slMBOMColorList.add(strMBOMColorOption);
                    String strMBOMColorOptionRelId = (String) mObjectMap.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                    if (!slPublishedColorList.contains(strMBOMColorOption)) {
                        DomainRelationship.disconnect(context, strMBOMColorOptionRelId);
                    }
                }
            }

            if (!slPublishedColorList.isEmpty()) {
                int slSize = slPublishedColorList.size();
                for (int j = 0; j < slSize; j++) {

                    if (!slMBOMColorList.contains(slPublishedColorList.get(j))) {
                        DomainObject domColorObject = DomainObject.newInstance(context, (String) slPublishedColorList.get(j));
                        DomainRelationship.connect(context, domMBOMObject, TigerConstants.RELATIONSHIP_PSS_COLORLIST, domColorObject);
                    }
                }
            }
        } catch (RuntimeException ex) {
            logger.error("Error in updateMBOMColorList : ", ex);
            throw ex;
        } catch (Exception ex) {
            logger.error("Error in updateMBOMColorList : ", ex);
            throw ex;
        }

    }

    /**
     * This method is for update the attribute values in MBOM based on EBOM
     * @param context
     * @param psPhysicalId
     * @param mbomPhysicalId
     * @param strPSRel
     * @param strMBOMRel
     * @throws Exception
     */
    public static void updateMBOMAttributes(Context context, String psPhysicalId, String mbomPhysicalId, String strPSRel, String strMBOMRel) throws Exception {

        try {
            DomainObject dPSObj = DomainObject.newInstance(context, psPhysicalId);
            DomainObject dMBOMObj = DomainObject.newInstance(context, mbomPhysicalId);
            String strSparePS = dPSObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PUBLISHEDPART_PSS_PARTSPAREPART);
            String strSpareMBOM = dMBOMObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXT_PSS_SPARE);
            String strNameRefPS = dPSObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_V_NAME);
            String strNameRefMBOM = dMBOMObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_V_NAME);
            String strDescriptionRefPS = dPSObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_PLMENTITY_V_DESCRIPTION);
            String strDescriptionRefMBOM = dMBOMObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_PLMENTITY_V_DESCRIPTION);
            // TIGTK-9215:START
            String strNetWeight = dMBOMObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXTPSS_NETWEIGHT);
            String strGrossWeight = dMBOMObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXTPSS_GROSSWEIGHT);
            String strCADMass = dPSObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PUBLISHEDPARTPSS_PP_CADMASS);

            String strEBOMMass1 = dPSObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_EBOM_Mass1);
            String strEBOMMass2 = dPSObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_EBOM_Mass2);
            String strEBOMMass3 = dPSObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_EBOM_Mass3);

            if (UIUtil.isNullOrEmpty(strNetWeight) || ("0.0").equalsIgnoreCase(strNetWeight)) {
                if (UIUtil.isNotNullAndNotEmpty(strEBOMMass1)) {
                    dMBOMObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXTPSS_NETWEIGHT, strEBOMMass1);
                } else if (UIUtil.isNotNullAndNotEmpty(strEBOMMass2)) {
                    dMBOMObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXTPSS_NETWEIGHT, strEBOMMass2);
                } else if (UIUtil.isNotNullAndNotEmpty(strEBOMMass3)) {
                    dMBOMObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXTPSS_NETWEIGHT, strEBOMMass3);
                } else {
                    dMBOMObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXTPSS_NETWEIGHT, strCADMass);
                }
            }

            if (UIUtil.isNullOrEmpty(strGrossWeight) || ("0.0").equalsIgnoreCase(strGrossWeight)) {

                if (UIUtil.isNotNullAndNotEmpty(strEBOMMass1)) {
                    dMBOMObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXTPSS_GROSSWEIGHT, strEBOMMass1);
                } else if (UIUtil.isNotNullAndNotEmpty(strEBOMMass2)) {
                    dMBOMObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXTPSS_GROSSWEIGHT, strEBOMMass2);
                } else if (UIUtil.isNotNullAndNotEmpty(strEBOMMass3)) {
                    dMBOMObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXTPSS_GROSSWEIGHT, strEBOMMass3);
                } else {
                    dMBOMObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXTPSS_GROSSWEIGHT, strCADMass);
                }
            }
            // TIGTK-9215:END
            if (!strSparePS.equalsIgnoreCase(strSpareMBOM)) {
                dMBOMObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXT_PSS_SPARE, strSparePS);
            }
            if (!strNameRefPS.equalsIgnoreCase(strNameRefMBOM)) {
                dMBOMObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_V_NAME, strNameRefPS);
            }

            if (!strDescriptionRefPS.equalsIgnoreCase(strDescriptionRefMBOM)) {
                dMBOMObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PLMENTITY_V_DESCRIPTION, strDescriptionRefPS);
            }

            if (UIUtil.isNotNullAndNotEmpty(strPSRel) && UIUtil.isNotNullAndNotEmpty(strMBOMRel)) {
                DomainRelationship dFromPSRel = DomainRelationship.newInstance(context, strPSRel);
                DomainRelationship dFromMBOMRel = DomainRelationship.newInstance(context, strMBOMRel);
                Map psInstAttributes = dFromPSRel.getAttributeMap(context);
                Map mbomInstAttributes = dFromMBOMRel.getAttributeMap(context);
                String strNameInstancePS = (String) psInstAttributes.get(TigerConstants.ATTRIBUTE_PLMINSTANCE_V_NAME);
                String strDescriptionInstancePS = (String) psInstAttributes.get(TigerConstants.ATTRIBUTE_PLMINSTANCE_V_DESCRIPTION);

                if (!strNameInstancePS.equalsIgnoreCase((String) mbomInstAttributes.get(TigerConstants.ATTRIBUTE_PLMINSTANCE_V_NAME))) {
                    dFromMBOMRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_PLMINSTANCE_V_NAME, strNameInstancePS);
                }

                if (!strDescriptionInstancePS.equalsIgnoreCase((String) mbomInstAttributes.get(TigerConstants.ATTRIBUTE_PLMINSTANCE_V_DESCRIPTION))) {
                    dFromMBOMRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_PLMINSTANCE_V_DESCRIPTION, strDescriptionInstancePS);
                }

            }

        } catch (Exception ex) {
            logger.error("Error in updateMBOMAttributes : ", ex);
            throw ex;
        }

    }

    /**
     * This method is for update the Material values in MBOM based on EBOM
     * @param context
     * @param psPhysicalId
     * @param mbomPhysicalId
     * @param strFromPSRel
     * @param strFromMBOMRel
     * @return
     * @throws Exception
     */
    public static boolean updateMBOMMaterial(Context context, String psPhysicalId, String mbomPhysicalId) throws Exception {

        boolean booleanReturn = false;
        String GENERIC = "Generic";
        try {
            DomainObject domPhysicalObject = DomainObject.newInstance(context, psPhysicalId);
            String strPSMaterial = domPhysicalObject.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PUBLISHEDPART_PSS_MATERIALLIST);
            StringList slPublishedMaterialList = new StringList();
            StringList slMBOMMaterialList = new StringList();
            StringList slMBOMMateriaTypelList = new StringList();
            if (UIUtil.isNotNullAndNotEmpty(strPSMaterial)) {
                if (strPSMaterial.contains("|")) {
                    String[] strMaterialArray = strPSMaterial.split("\\|");
                    if (strMaterialArray.length > 1) {
                        for (int i = 0; i < strMaterialArray.length; i++) {
                            DomainObject domMaterial = DomainObject.newInstance(context, strMaterialArray[i]);
                            String strCheckMaterialType = domMaterial.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PROCESSCONTINUOUSPROVIDE_PSS_MATERIALTYPE);
                            if (strCheckMaterialType.equalsIgnoreCase(GENERIC))
                                slPublishedMaterialList.add(strMaterialArray[i]);
                        }
                    }
                } else {
                    DomainObject domMaterial = DomainObject.newInstance(context, strPSMaterial);
                    String strCheckMaterialType = domMaterial.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PROCESSCONTINUOUSPROVIDE_PSS_MATERIALTYPE);
                    if (strCheckMaterialType.equalsIgnoreCase(GENERIC))
                        slPublishedMaterialList.add(strPSMaterial);
                }
            }
            DomainObject domMBOMObject = DomainObject.newInstance(context, mbomPhysicalId);
            Pattern typePattern = new Pattern(TigerConstants.TYPE_PROCESS_CONTINUOUS_CREATE_MATERIAL);
            typePattern.addPattern(TigerConstants.TYPE_PROCESSCONTINUOUSPROVIDE);

            String strWhere = "attribute[" + TigerConstants.ATTRIBUTE_PSS_PROCESSCONTINUOUSPROVIDE_PSS_MATERIALTYPE + "]==" + GENERIC;

            MapList mlMBOMMaterialList = domMBOMObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS, // relationship pattern
                    typePattern.getPattern(), // object pattern
                    new StringList("physicalid"), // object selects
                    new StringList(DomainConstants.SELECT_RELATIONSHIP_ID), // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 0, // recursion level
                    strWhere, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    null, // postPattern
                    null, null, null);

            if (!mlMBOMMaterialList.isEmpty()) {
                Iterator itr = mlMBOMMaterialList.iterator();
                while (itr.hasNext()) {
                    Map mObjectMap = (Map) itr.next();
                    String strMBOMMaterialId = (String) mObjectMap.get("physicalid");
                    slMBOMMaterialList.add(strMBOMMaterialId);
                    String strMBOMMaterialRelId = (String) mObjectMap.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                    DomainObject dMaterialMBOMObj = DomainObject.newInstance(context, strMBOMMaterialId);
                    String strMaterialTypeMBOM = dMaterialMBOMObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PROCESSCONTINUOUSPROVIDE_PSS_MATERIALTYPE);
                    if (!slPublishedMaterialList.contains(strMBOMMaterialId) && (strMaterialTypeMBOM.equalsIgnoreCase("Generic"))) {
                        DomainRelationship.disconnect(context, strMBOMMaterialRelId);
                    }
                }
            }

            if (!slPublishedMaterialList.isEmpty()) {
                int slSize = slPublishedMaterialList.size();
                for (int j = 0; j < slSize; j++) {
                    DomainObject domMaterialObject = DomainObject.newInstance(context, (String) slPublishedMaterialList.get(j));
                    if (!slMBOMMaterialList.contains(slPublishedMaterialList.get(j))) {
                        DomainRelationship.connect(context, domMBOMObject, TigerConstants.RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS, domMaterialObject);
                    }
                }
            }

            StringList slMaterialList = domMBOMObject.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS + "].to.physicalid");
            if (!slMaterialList.isEmpty()) {
                for (int j = 0; j < slMaterialList.size(); j++) {
                    DomainObject domMaterialObject = DomainObject.newInstance(context, (String) slMaterialList.get(j));
                    String strMaterialTypeMBOM = domMaterialObject.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PROCESSCONTINUOUSPROVIDE_PSS_MATERIALTYPE);
                    slMBOMMateriaTypelList.add(strMaterialTypeMBOM);
                }
                if (slMBOMMateriaTypelList.contains("Generic") && slMBOMMateriaTypelList.contains("Specific")) {
                    booleanReturn = true;
                }
            }

        } catch (RuntimeException ex) {
            logger.error("Error in updateMBOMMaterial : ", ex);
            throw ex;
        } catch (Exception ex) {
            logger.error("Error in updateMBOMMaterial : ", ex);
            throw ex;
        }
        return booleanReturn;
    }

    public static String checkColorOptionsUpdate(Context context, String psPhysicalId, String mbomPhysicalId) throws Exception {
        String strUpdateOfColorOptions = DomainConstants.EMPTY_STRING;
        try {

            DomainObject domPS = DomainObject.newInstance(context, psPhysicalId);
            DomainObject domMBOM = DomainObject.newInstance(context, mbomPhysicalId);

            StringList slPublishedColorList = new StringList();
            StringList slMBOMColorList = new StringList();
            boolean checkFlag = false;
            String strPSColorOptions = domPS.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PUBLISHEDPART_PSS_COLORLIST);
            if (UIUtil.isNotNullAndNotEmpty(strPSColorOptions)) {
                if (strPSColorOptions.contains("|")) {
                    String[] strColorArray = strPSColorOptions.split("\\|");
                    if (strColorArray.length > 1) {
                        for (int i = 0; i < strColorArray.length; i++) {
                            slPublishedColorList.add(strColorArray[i]);
                        }
                    }
                } else {
                    slPublishedColorList.add(strPSColorOptions);
                }
            }
            MapList mlMBOMColorOptions = domMBOM.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_COLORLIST, // relationship pattern
                    TigerConstants.TYPE_PSS_COLOROPTION, // object pattern
                    new StringList("physicalid"), // object selects
                    new StringList(DomainConstants.SELECT_RELATIONSHIP_ID), // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 0, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    null, // postPattern
                    null, null, null);
            String strMBOMColorOption = DomainConstants.EMPTY_STRING;
            if (!mlMBOMColorOptions.isEmpty()) {
                Iterator itr = mlMBOMColorOptions.iterator();
                while (itr.hasNext()) {
                    Map mObjectMap = (Map) itr.next();
                    strMBOMColorOption = (String) mObjectMap.get("physicalid");
                    slMBOMColorList.add(strMBOMColorOption);
                }
            }

            if (slPublishedColorList.size() != slMBOMColorList.size()) {
                checkFlag = true;
            } else if (!slPublishedColorList.isEmpty() && !slMBOMColorList.isEmpty()) {
                for (int i = 0; i < slPublishedColorList.size(); i++) {
                    String strPSColorOption = (String) slPublishedColorList.get(i);
                    if (!slMBOMColorList.contains(strPSColorOption)) {
                        checkFlag = true;
                    }
                }
            }
            if (checkFlag == true)
                strUpdateOfColorOptions = "Update of Color Options is available";

        } catch (Exception ex) {
            logger.error("Error in checkColorOptionsUpdate : ", ex);
            throw ex;
        }
        return strUpdateOfColorOptions;
    }

    public static String checkEffectivitiesUpdate(Context context, String psPhysicalId, String mbomPhysicalId, String strFromPSRel, String strFromMBOMRel) throws Exception {
        String strUpdateEffectvity = DomainConstants.EMPTY_STRING;
        try {

            if (UIUtil.isNotNullAndNotEmpty(strFromPSRel) && UIUtil.isNotNullAndNotEmpty(strFromMBOMRel)) {
                DomainRelationship domPSRel = DomainRelationship.newInstance(context, strFromPSRel);
                DomainRelationship domMBOMRel = DomainRelationship.newInstance(context, strFromMBOMRel);
                String strPSEffectivity = domPSRel.getAttributeValue(context, TigerConstants.ATTRIBUTE_V_EFFECTIVITYCOMPILEDFORM);
                String strMBOMEffectivity = domMBOMRel.getAttributeValue(context, TigerConstants.ATTRIBUTE_V_EFFECTIVITYCOMPILEDFORM);
                if (!strPSEffectivity.equalsIgnoreCase(strMBOMEffectivity)) {
                    strUpdateEffectvity = "Update of Technical Diversity available";
                }
            }

        } catch (Exception ex) {
            logger.error("Error in checkEffectivitiesUpdate : ", ex);
            throw ex;
        }
        return strUpdateEffectvity;
    }

    public static String checkMaterialUpdate(Context context, String psPhysicalId, String mbomPhysicalId) throws Exception {
        String strUpdateOfMaterial = DomainConstants.EMPTY_STRING;
        String GENERIC = "Generic";
        try {
            DomainObject domPS = DomainObject.newInstance(context, psPhysicalId);
            DomainObject domMBOM = DomainObject.newInstance(context, mbomPhysicalId);

            StringList slPublishedMaterialList = new StringList();
            StringList slMBOMMaterialList = new StringList();

            String strPSMaterial = domPS.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PUBLISHEDPART_PSS_MATERIALLIST);
            if (UIUtil.isNotNullAndNotEmpty(strPSMaterial)) {
                if (strPSMaterial.contains("|")) {
                    String[] strMaterialArray = strPSMaterial.split("\\|");
                    if (strMaterialArray.length > 1) {
                        for (int i = 0; i < strMaterialArray.length; i++) {
                            DomainObject domMaterial = DomainObject.newInstance(context, strMaterialArray[i]);
                            String strCheckMaterialType = domMaterial.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PROCESSCONTINUOUSPROVIDE_PSS_MATERIALTYPE);
                            if (strCheckMaterialType.equalsIgnoreCase(GENERIC))
                                slPublishedMaterialList.add(strMaterialArray[i]);
                        }
                    }
                } else {
                    DomainObject domMaterial = DomainObject.newInstance(context, strPSMaterial);
                    String strCheckMaterialType = domMaterial.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PROCESSCONTINUOUSPROVIDE_PSS_MATERIALTYPE);
                    if (strCheckMaterialType.equalsIgnoreCase(GENERIC))
                        slPublishedMaterialList.add(strPSMaterial);
                }
            }

            Pattern typePattern = new Pattern(TigerConstants.TYPE_PROCESS_CONTINUOUS_CREATE_MATERIAL);
            typePattern.addPattern(TigerConstants.TYPE_PROCESSCONTINUOUSPROVIDE);

            String strWhere = "attribute[" + TigerConstants.ATTRIBUTE_PSS_PROCESSCONTINUOUSPROVIDE_PSS_MATERIALTYPE + "]==" + GENERIC;

            MapList mlMBOMMaterialListConnectedWithMBOM = domMBOM.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS, // relationship pattern
                    typePattern.getPattern(), // object pattern
                    new StringList("physicalid"), // object selects
                    new StringList(DomainConstants.SELECT_RELATIONSHIP_ID), // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 0, // recursion level
                    strWhere, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    null, // postPattern
                    null, null, null);
            boolean checkFlag = false;
            String strMBOMMaterial = DomainConstants.EMPTY_STRING;
            if (!mlMBOMMaterialListConnectedWithMBOM.isEmpty()) {
                Iterator itr = mlMBOMMaterialListConnectedWithMBOM.iterator();
                while (itr.hasNext()) {
                    Map mObjectMap = (Map) itr.next();
                    strMBOMMaterial = (String) mObjectMap.get("physicalid");
                    slMBOMMaterialList.add(strMBOMMaterial);
                }
            }
            if (slMBOMMaterialList.size() != slPublishedMaterialList.size())
                checkFlag = true;

            else if (!slPublishedMaterialList.isEmpty() && !slMBOMMaterialList.isEmpty()) {
                for (int i = 0; i < slPublishedMaterialList.size(); i++) {
                    String strPSMaterialObject = (String) slPublishedMaterialList.get(i);
                    if (!slMBOMMaterialList.contains(strPSMaterialObject)) {
                        checkFlag = true;
                    }
                }
            }

            if (checkFlag == true)
                strUpdateOfMaterial = "Update of Material is available";
        }

        catch (Exception ex) {
            logger.error("Error in checkMaterialUpdate : ", ex);
            throw ex;
        }
        return strUpdateOfMaterial;
    }

    public static String checkSpareUpdate(Context context, String psPhysicalId, String mbomPhysicalId) throws Exception {
        String strUpdateOfSpare = DomainConstants.EMPTY_STRING;
        try {
            DomainObject domPS = DomainObject.newInstance(context, psPhysicalId);
            DomainObject domMBOM = DomainObject.newInstance(context, mbomPhysicalId);

            StringList slObjSelectStmts = new StringList(1);
            slObjSelectStmts.addElement("physicalid");
            slObjSelectStmts.addElement(DomainConstants.SELECT_NAME);
            slObjSelectStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_V_NAME + "]");
            StringList slRelSelectStmts = new StringList(1);
            slRelSelectStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            String strRelWhereClause = "attribute[PSS_PublishedEBOM.PSS_InstanceName] ==\"" + PSS_SPARE_PART + "\"";
            MapList mlPSSparePart = domPS.getRelatedObjects(context, "VPMInstance", // Relationship Pattern
                    TigerConstants.TYPE_VPMREFERENCE, // Object Pattern
                    slObjSelectStmts, // Object Selects
                    slRelSelectStmts, // Relationship Selects
                    false, // to direction
                    true, // from direction
                    (short) 1, // recursion level
                    null, // object where clause
                    strRelWhereClause, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    null, // Post Type Pattern
                    null, null, null);

            Pattern typePattern = new Pattern(TigerConstants.TYPE_CREATEASSEMBLY);
            typePattern.addPattern(TigerConstants.TYPE_CREATEKIT);
            typePattern.addPattern(TigerConstants.TYPE_CREATEMATERIAL);
            typePattern.addPattern(TigerConstants.TYPE_PROCESS_CONTINUOUS_CREATE_MATERIAL);
            typePattern.addPattern(TigerConstants.TYPE_PROCESSCONTINUOUSPROVIDE);

            MapList mlMBOMSparePart = domMBOM.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_SPAREMANUFACTURINGITEM, // Relationship Pattern
                    typePattern.getPattern(), // Object Pattern
                    slObjSelectStmts, // Object Selects
                    slRelSelectStmts, // Relationship Selects
                    false, // to direction
                    true, // from direction
                    (short) 0, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    null, // Post Type Pattern
                    null, null, null);

            if (mlMBOMSparePart.size() != mlPSSparePart.size())
                strUpdateOfSpare = "Update of Spare is available";
            else {
                if (!mlMBOMSparePart.isEmpty() && !mlPSSparePart.isEmpty()) {
                    for (int i = 0; i < mlPSSparePart.size() && i < mlMBOMSparePart.size(); i++) {
                        Map mMBOMSpare = (Map) mlMBOMSparePart.get(i);
                        Map mPSSpare = (Map) mlPSSparePart.get(i);
                        String strPSMaterialObject = (String) mPSSpare.get("attribute[" + TigerConstants.ATTRIBUTE_V_NAME + "]");
                        String strMBOMMaterialObject = (String) mMBOMSpare.get("attribute[" + TigerConstants.ATTRIBUTE_V_NAME + "]");
                        if (!strPSMaterialObject.equalsIgnoreCase(strMBOMMaterialObject)) {
                            strUpdateOfSpare = "Update of Spare is available";
                        }
                    }
                }
            }

        } catch (Exception ex) {
            logger.error("Error in checkSpareUpdate : ", ex);
            throw ex;
        }
        return strUpdateOfSpare;

    }

    public static String checkAttributeUpdate(Context context, String psPhysicalId, String mbomPhysicalId, String strFromPSRel, String strFromMBOMRel) throws Exception {
        String strUpdateOfAttribute = DomainConstants.EMPTY_STRING;
        String strNameInstancePS = DomainConstants.EMPTY_STRING;
        String strDescriptionInstancePS = DomainConstants.EMPTY_STRING;
        String strNameInstanceMBOM = DomainConstants.EMPTY_STRING;
        String strDescriptionInstanceMBOM = DomainConstants.EMPTY_STRING;
        try {
            DomainObject domPS = DomainObject.newInstance(context, psPhysicalId);
            DomainObject domMBOM = DomainObject.newInstance(context, mbomPhysicalId);

            String strPSSpare = domPS.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PUBLISHEDPART_PSS_PARTSPAREPART);
            String strMBOMSpare = domMBOM.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXT_PSS_SPARE);

            String strNameRefPS = domPS.getAttributeValue(context, TigerConstants.ATTRIBUTE_V_NAME);
            String strDescriptionRefPS = domPS.getAttributeValue(context, TigerConstants.ATTRIBUTE_PLMENTITY_V_DESCRIPTION);

            String strNameRefMBOM = domMBOM.getAttributeValue(context, TigerConstants.ATTRIBUTE_V_NAME);
            String strDescriptionRefMBOM = domMBOM.getAttributeValue(context, TigerConstants.ATTRIBUTE_PLMENTITY_V_DESCRIPTION);

            if (UIUtil.isNotNullAndNotEmpty(strFromPSRel) && UIUtil.isNotNullAndNotEmpty(strFromMBOMRel)) {
                DomainRelationship dFromPSRel = DomainRelationship.newInstance(context, strFromPSRel);
                DomainRelationship dFromMBOMRel = DomainRelationship.newInstance(context, strFromMBOMRel);

                strNameInstancePS = dFromPSRel.getAttributeValue(context, TigerConstants.ATTRIBUTE_PLMINSTANCE_V_NAME);
                strDescriptionInstancePS = dFromPSRel.getAttributeValue(context, TigerConstants.ATTRIBUTE_PLMINSTANCE_V_DESCRIPTION);
                strNameInstanceMBOM = dFromMBOMRel.getAttributeValue(context, TigerConstants.ATTRIBUTE_PLMINSTANCE_V_NAME);
                strDescriptionInstanceMBOM = dFromPSRel.getAttributeValue(context, TigerConstants.ATTRIBUTE_PLMINSTANCE_V_DESCRIPTION);
            }
            if (!strPSSpare.equalsIgnoreCase(strMBOMSpare) || !strNameRefPS.equalsIgnoreCase(strNameRefMBOM) || !strDescriptionRefPS.equalsIgnoreCase(strDescriptionRefMBOM)
                    || !strNameInstancePS.equalsIgnoreCase(strNameInstanceMBOM) || !strDescriptionInstancePS.equalsIgnoreCase(strDescriptionInstanceMBOM))
                strUpdateOfAttribute = "Update of Attributes is available";

        } catch (Exception ex) {
            logger.error("Error in checkAttributeUpdate : ", ex);
            throw ex;
        }
        return strUpdateOfAttribute;
    }

    public static String checkAlternateUpdate(Context context, String psPhysicalId, String mbomPhysicalId) throws Exception {

        PLMCoreModelerSession plmSession = null;
        boolean isTransactionActive = false;
        String strUpdateOfAltenate = DomainConstants.EMPTY_STRING;
        try {
            ContextUtil.startTransaction(context, true);
            isTransactionActive = true;
            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();
            boolean checkFlag = false;
            DomainObject domPS = DomainObject.newInstance(context, psPhysicalId);
            DomainObject domMBOM = DomainObject.newInstance(context, mbomPhysicalId);

            String strPSName = domPS.getAttributeValue(context, TigerConstants.ATTRIBUTE_V_NAME);
            String strMBOMName = domMBOM.getAttributeValue(context, TigerConstants.ATTRIBUTE_V_NAME);

            if (!strPSName.equalsIgnoreCase(strMBOMName)) {
                checkFlag = true;
            }
            boolean[] iComputeStatus = new boolean[1];
            iComputeStatus[0] = true;
            MapList mbomList = new MapList();
            mbomList.add(mbomPhysicalId);
            Map<String, List<String>> mpMBOMAlternatePartsAvailable = FRCMBOMModelerUtility.getAlternateReferences(context, plmSession, mbomList, iComputeStatus);
            StringList slMBOMAlternateList = new StringList();
            if (mpMBOMAlternatePartsAvailable.size() > 0) {
                List<String> slMBOMAlternate = mpMBOMAlternatePartsAvailable.get(mbomPhysicalId);
                for (String alternateMBOMId : slMBOMAlternate) {
                    slMBOMAlternateList.add(alternateMBOMId);
                }
            }

            StringList slObjSelectStmts = new StringList(1);
            slObjSelectStmts.addElement("physicalid");
            slObjSelectStmts.addElement(DomainConstants.SELECT_NAME);
            StringList slRelSelectStmts = new StringList(1);
            slRelSelectStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            String strRelWhereClause = "attribute[PSS_PublishedEBOM.PSS_InstanceName] == Alternate";
            MapList mlPSAlternateParts = domPS.getRelatedObjects(context, "VPMInstance", // Relationship Pattern
                    TigerConstants.TYPE_VPMREFERENCE, // Object Pattern
                    slObjSelectStmts, // Object Selects
                    slRelSelectStmts, // Relationship Selects
                    false, // to direction
                    true, // from direction
                    (short) 1, // recursion level
                    null, // object where clause
                    strRelWhereClause, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    null, // Post Type Pattern
                    null, null, null);

            StringList slPSAlternateList = new StringList();
            if (!mlPSAlternateParts.isEmpty()) {
                int slAlternateSize = mlPSAlternateParts.size();
                for (int i = 0; i < slAlternateSize; i++) {
                    Map mAlternateMap = (Map) mlPSAlternateParts.get(i);
                    slPSAlternateList.add((String) mAlternateMap.get("physicalid"));
                }
            }

            if (slPSAlternateList.size() != slMBOMAlternateList.size())
                checkFlag = true;
            else if (!slPSAlternateList.isEmpty() && !slMBOMAlternateList.isEmpty()) {
                for (int i = 0; i < slPSAlternateList.size(); i++) {
                    DomainObject domPhysicalObject = DomainObject.newInstance(context, (String) slPSAlternateList.get(i));
                    DomainObject domMBOMObject = DomainObject.newInstance(context, (String) slMBOMAlternateList.get(i));
                    String strPSVName = domPhysicalObject.getAttributeValue(context, TigerConstants.ATTRIBUTE_V_NAME);
                    String strMBOMVName = domMBOMObject.getAttributeValue(context, TigerConstants.ATTRIBUTE_V_NAME);
                    if (!strPSVName.equalsIgnoreCase(strMBOMVName))
                        checkFlag = true;
                }
            }

            if (checkFlag == true)
                strUpdateOfAltenate = "Update of Alternate is available";

            flushAndCloseSession(plmSession);
            if (isTransactionActive) {
                ContextUtil.commitTransaction(context);
            }

        } catch (Exception ex) {
            logger.error("Error in checkAlternateUpdate : ", ex);
            flushAndCloseSession(plmSession);
            if (isTransactionActive) {
                ContextUtil.abortTransaction(context);
            }
            throw ex;
        }
        return strUpdateOfAltenate;
    }

    public static String checkToolingUpdate(Context context, String psPhysicalId, String mbomPhysicalId) throws Exception {
        String strUpdateOfTooling = DomainConstants.EMPTY_STRING;
        PLMCoreModelerSession plmSession = null;
        try {
            DomainObject domPS = DomainObject.newInstance(context, psPhysicalId);
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            StringList slPublishedToolingList = new StringList();
            StringList slMBOMToolingList = new StringList();

            String strPSTooling = domPS.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PUBLISHEDPART_PSS_TOOLINGLIST);
            if (UIUtil.isNotNullAndNotEmpty(strPSTooling)) {
                if (strPSTooling.contains("|")) {
                    String[] strToolingArray = strPSTooling.split("\\|");
                    if (strToolingArray.length > 1) {
                        for (int i = 0; i < strToolingArray.length; i++) {
                            slPublishedToolingList.add(strToolingArray[i]);
                        }
                    }
                } else {
                    slPublishedToolingList.add(strPSTooling);
                }
            }

            List mlToolingList = PSS_FRCMBOMModelerUtility_mxJPO.getResourcesAttachedToMBOMReference(context, plmSession, mbomPhysicalId);
            boolean checkFlag = false;
            if (!mlToolingList.isEmpty()) {
                for (int i = 0; i < mlToolingList.size(); i++) {
                    String strMBOMTooling = (String) mlToolingList.get(i);
                    slMBOMToolingList.add(strMBOMTooling);
                }
            }
            if (slPublishedToolingList.size() != slMBOMToolingList.size())
                checkFlag = true;

            else if (!slPublishedToolingList.isEmpty() && !slMBOMToolingList.isEmpty()) {
                for (int i = 0; i < slPublishedToolingList.size(); i++) {
                    String strPSMaterialObject = (String) slPublishedToolingList.get(i);
                    if (!slMBOMToolingList.contains(strPSMaterialObject)) {
                        checkFlag = true;
                    }
                }
            }

            if (checkFlag == true)
                strUpdateOfTooling = "Update of Tooling is available";

        } catch (Exception ex) {
            logger.error("Error in checkToolingUpdate : ", ex);
            throw ex;
        }
        return strUpdateOfTooling;
    }

    /**
     * This method update the Alternate part as per EBOM to MBOM
     * @param context
     * @param psPhysicalId
     * @param mbomPhysicalId
     * @param strFromPSRel
     * @param strFromMBOMRel
     * @throws Exception
     */
    public static void updateMBOMAlternate(Context context, String psPhysicalId, String mbomPhysicalId, String strFromPSRel, String strFromMBOMRel) throws Exception {

        PLMCoreModelerSession plmSession = null;
        boolean isTransactionActive = false;
        try {
            System.out.println("\n \n \n updateMBOMAlternate CALLED");
            ContextUtil.startTransaction(context, true);
            isTransactionActive = true;
            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();
            StringList mbomList = new StringList();
            String strMBOMAlternatePart = DomainConstants.EMPTY_STRING;
            DomainObject dPSObj = DomainObject.newInstance(context, psPhysicalId);
            DomainObject dMBOMObj = DomainObject.newInstance(context, mbomPhysicalId);
            String strNameRefPS = dPSObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_V_NAME);
            String strNameRefMBOM = dMBOMObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_V_NAME);

            boolean[] iComputeStatus = new boolean[1];
            iComputeStatus[0] = true;
            if (!strNameRefPS.equalsIgnoreCase(strNameRefMBOM)) {
                StringList slMBOMAlternateItemList = new StringList();
                slMBOMAlternateItemList.add(mbomPhysicalId);
                Map<String, List<String>> mpMBOMAlternateParts = FRCMBOMModelerUtility.getAlternateReferences(context, plmSession, slMBOMAlternateItemList, iComputeStatus);
                System.out.println("\n \n \n mpMBOMAlternateParts :: " + mpMBOMAlternateParts);
                if (mpMBOMAlternateParts.size() > 0) {
                    List<String> slMBOMAlternate = mpMBOMAlternateParts.get(mbomPhysicalId);
                    for (int i = 0; i < slMBOMAlternate.size(); i++) {
                        strMBOMAlternatePart = slMBOMAlternate.get(i);
                        DomainObject domAlterMBOM = DomainObject.newInstance(context, strMBOMAlternatePart);
                        String strNameAlterMBOM = domAlterMBOM.getAttributeValue(context, TigerConstants.ATTRIBUTE_V_NAME);

                        if (strNameRefPS.equalsIgnoreCase(strNameAlterMBOM)) {
                            String[] args = new String[4];
                            args[0] = strFromMBOMRel;
                            args[1] = strMBOMAlternatePart;
                            args[2] = "true";
                            args[3] = mbomPhysicalId;

                            PSS_FRCMBOMProg_mxJPO.replaceByAlternateMBOM(context, args);
                            mbomPhysicalId = strMBOMAlternatePart;
                            break;

                        }
                    }
                }
            }

            mbomList.add(mbomPhysicalId);
            Map<String, List<String>> mpMBOMAlternatePartsAvailable = FRCMBOMModelerUtility.getAlternateReferences(context, plmSession, mbomList, iComputeStatus);
            System.out.println("\n \n \n mpMBOMAlternatePartsAvailable :: " + mpMBOMAlternatePartsAvailable);
            StringList slMBOMAlternateList = new StringList();
            if (mpMBOMAlternatePartsAvailable.size() > 0) {
                List<String> slMBOMAlternate = mpMBOMAlternatePartsAvailable.get(mbomPhysicalId);
                for (String alternateMBOMId : slMBOMAlternate) {
                    slMBOMAlternateList.add(alternateMBOMId);
                }
            }
            StringList slObjSelectStmts = new StringList(1);
            slObjSelectStmts.addElement("physicalid");
            slObjSelectStmts.addElement(DomainConstants.SELECT_NAME);
            StringList slRelSelectStmts = new StringList(1);
            slRelSelectStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            String strRelWhereClause = "attribute[PSS_PublishedEBOM.PSS_InstanceName] == Alternate";
            MapList mlPSAlternateParts = dPSObj.getRelatedObjects(context, "VPMInstance", // Relationship Pattern
                    TigerConstants.TYPE_VPMREFERENCE, // Object Pattern
                    slObjSelectStmts, // Object Selects
                    slRelSelectStmts, // Relationship Selects
                    false, // to direction
                    true, // from direction
                    (short) 1, // recursion level
                    null, // object where clause
                    strRelWhereClause, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    null, // Post Type Pattern
                    null, null, null);

            StringList slPSAlternateList = new StringList();
            StringList slPSAlternateNameList = new StringList();
            StringList slMBOMAlternateNameList = new StringList();
            if (!mlPSAlternateParts.isEmpty()) {
                int slPSAlternateSize = mlPSAlternateParts.size();
                for (int i = 0; i < slPSAlternateSize; i++) {
                    Map mPSAlternate = (Map) mlPSAlternateParts.get(i);
                    slPSAlternateList.add((String) mPSAlternate.get("physicalid"));
                    slPSAlternateNameList.add((String) mPSAlternate.get(DomainConstants.SELECT_NAME));
                }
            }

            if (!slMBOMAlternateList.isEmpty()) {
                for (int j = 0; j < slMBOMAlternateList.size(); j++) {
                    String strMBOMAlternateId = (String) slMBOMAlternateList.get(j);
                    DomainObject domAlternate = DomainObject.newInstance(context, strMBOMAlternateId);
                    String strMBOMAlternateName = domAlternate.getAttributeValue(context, TigerConstants.ATTRIBUTE_V_NAME);
                    slMBOMAlternateNameList.add(strMBOMAlternateName);
                    if (!slPSAlternateNameList.contains(strMBOMAlternateName)) {
                        FRCMBOMModelerUtility.deleteMfgProcessAlternate(context, plmSession, mbomPhysicalId, strMBOMAlternateId);
                    }
                }
            }
            String mbomType = dMBOMObj.getInfo(context, DomainConstants.SELECT_TYPE);
            String plantPID = pss.mbom.MBOMUtil_mxJPO.getMasterPlant(context, mbomPhysicalId);
            if (!slPSAlternateList.isEmpty()) {
                // TIGTK-13154 : START
                Map<String, List<String>> workingInfo = new HashMap<String, List<String>>();
                workingInfo.put("instanceToCreate_parentRefPLMID", new ArrayList<String>());
                workingInfo.put("instanceToCreate_childRefPLMID", new ArrayList<String>());
                workingInfo.put("mbomLeafInstancePIDList", new ArrayList<String>());
                workingInfo.put("psPathList", new ArrayList<String>());
                workingInfo.put("newRefPIDList", new ArrayList<String>());
                workingInfo.put("newScopesToCreate_MBOMRefPIDs", new ArrayList<String>());
                workingInfo.put("newScopesToCreate_MBOMRefPLMIDs", new ArrayList<String>());
                workingInfo.put("newScopesToCreate_PSRefPIDs", new ArrayList<String>());
                workingInfo.put("newScopeObjectList", new ArrayList<String>());

                List<Map<String, String>> workingInfo_instanceAttributes = new ArrayList<Map<String, String>>();
                List<Integer> workingInfo_indexInstancesForImplement = new ArrayList<Integer>();
                Set<String> workingInfo_lModelListOnStructure = new HashSet<String>();
                Map<String, String> workingInfo_AppDateToValuate = new HashMap<String, String>();
                // TIGTK-13154 : END
                for (int j = 0; j < slPSAlternateList.size(); j++) {
                    String strPSAlternateId = (String) slPSAlternateList.get(j);
                    DomainObject domPSAlternate = DomainObject.newInstance(context, strPSAlternateId);
                    String strPSAlternateName = domPSAlternate.getAttributeValue(context, TigerConstants.ATTRIBUTE_V_NAME);
                    if (!slMBOMAlternateNameList.contains(strPSAlternateName)) {
                        // TIGTK-13154 : START
                        // Recursively process the PS root node and create the MBOM references
                        String alternateMBOMRefPLMID = PSS_FRCMBOMProg_mxJPO.createMBOMFromEBOMLikePSRecursive_new(context, plmSession, null, null, strPSAlternateId, null, workingInfo,
                                workingInfo_lModelListOnStructure, workingInfo_instanceAttributes, workingInfo_indexInstancesForImplement, workingInfo_AppDateToValuate, plantPID, mbomType);

                        String alternateMBOMRefPID = PLMID.buildFromString(alternateMBOMRefPLMID).getPid();
                        // TIGTK-13154 : END
                        FRCMBOMModelerUtility.createMfgProcessAlternate(context, plmSession, mbomPhysicalId, alternateMBOMRefPID);
                    }
                }
            }
            flushAndCloseSession(plmSession);
            if (isTransactionActive) {
                ContextUtil.commitTransaction(context);
            }

        } catch (Exception ex) {
            logger.error("Error in updateMBOMAlternate : ", ex);
            flushAndCloseSession(plmSession);
            if (isTransactionActive) {
                ContextUtil.abortTransaction(context);
            }
            throw ex;
        }

    }

    public static void updateMBOMTooling(Context context, String psPhysicalId, String mbomPhysicalId) throws Exception {

        PLMCoreModelerSession plmSession = null;
        boolean isTransactionActive = false;
        try {
            ContextUtil.startTransaction(context, true);
            isTransactionActive = true;
            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();
            DomainObject domPhysicalObject = DomainObject.newInstance(context, psPhysicalId);
            String strToolingList = domPhysicalObject.getAttributeValue(context, "PSS_PublishedPart.PSS_ToolingList");
            StringList slPublishedToolList = new StringList();
            StringList slMBOMToolList = new StringList();
            if (UIUtil.isNotNullAndNotEmpty(strToolingList)) {
                if (strToolingList.contains("|")) {
                    String[] strToolArray = strToolingList.split("\\|");
                    if (strToolArray.length > 1) {
                        for (int i = 0; i < strToolArray.length; i++) {
                            slPublishedToolList.add(strToolArray[i]);
                        }
                    }
                } else {
                    slPublishedToolList.add(strToolingList);
                }
            }

            List lToolingList = PSS_FRCMBOMModelerUtility_mxJPO.getResourcesAttachedToMBOMReference(context, plmSession, mbomPhysicalId);
            if (!lToolingList.isEmpty()) {
                for (int i = 0; i < lToolingList.size(); i++) {
                    String strMBOMTool = (String) lToolingList.get(i);
                    slMBOMToolList.add(strMBOMTool);
                    if (!slPublishedToolList.contains(strMBOMTool)) {
                        FRCMBOMModelerUtility.detachResourceFromMBOMReference(context, plmSession, mbomPhysicalId, strMBOMTool);
                    }
                }
            }

            if (!slPublishedToolList.isEmpty()) {
                int slSize = slPublishedToolList.size();
                for (int j = 0; j < slSize; j++) {
                    if (!slMBOMToolList.contains(slPublishedToolList.get(j))) {
                        PSS_FRCMBOMModelerUtility_mxJPO.attachResourceToMBOMReference(context, plmSession, mbomPhysicalId, (String) slPublishedToolList.get(j));
                    }
                }
            }
            flushAndCloseSession(plmSession);
            if (isTransactionActive) {
                ContextUtil.commitTransaction(context);
            }

        } catch (Exception ex) {
            logger.error("Error in updateMBOMTooling : ", ex);
            flushAndCloseSession(plmSession);
            if (isTransactionActive) {
                ContextUtil.abortTransaction(context);
            }
            throw ex;
        }

    }

    public static void updateMBOMSpare(Context context, String psPhysicalId, String mbomPhysicalId) throws Exception {

        PLMCoreModelerSession plmSession = null;
        boolean isTransactionActive = false;
        try {
            ContextUtil.startTransaction(context, true);
            isTransactionActive = true;
            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();
            StringList slMBOMNameList = new StringList();
            String strMBOMAlternatePart = DomainConstants.EMPTY_STRING;
            DomainObject dPSObj = DomainObject.newInstance(context, psPhysicalId);
            DomainObject dMBOMObj = DomainObject.newInstance(context, mbomPhysicalId);

            StringList slObjSelectStmts = new StringList(1);
            slObjSelectStmts.addElement("physicalid");
            slObjSelectStmts.addElement(DomainConstants.SELECT_NAME);
            StringList slRelSelectStmts = new StringList(1);
            slRelSelectStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            String strRelWhereClause = "attribute[PSS_PublishedEBOM.PSS_InstanceName] ==\"" + PSS_SPARE_PART + "\"";
            MapList mlPSSparePart = dPSObj.getRelatedObjects(context, "VPMInstance", // Relationship Pattern
                    TigerConstants.TYPE_VPMREFERENCE, // Object Pattern
                    slObjSelectStmts, // Object Selects
                    slRelSelectStmts, // Relationship Selects
                    false, // to direction
                    true, // from direction
                    (short) 1, // recursion level
                    null, // object where clause
                    strRelWhereClause, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    null, // Post Type Pattern
                    null, null, null);

            StringList slPSSpareList = new StringList();
            StringList slPSSpareNameList = new StringList();

            if (!mlPSSparePart.isEmpty()) {

                for (int i = 0; i < mlPSSparePart.size(); i++) {
                    Map mPSSpare = (Map) mlPSSparePart.get(i);
                    String strSparePSId = (String) mPSSpare.get("physicalid");
                    String strSparePSName = (String) mPSSpare.get(DomainConstants.SELECT_NAME);
                    slPSSpareList.add(strSparePSId);
                    slPSSpareNameList.add(strSparePSName);
                }

            }

            Pattern typePattern = new Pattern(TigerConstants.TYPE_CREATEASSEMBLY);
            typePattern.addPattern(TigerConstants.TYPE_CREATEKIT);
            typePattern.addPattern(TigerConstants.TYPE_CREATEMATERIAL);
            typePattern.addPattern(TigerConstants.TYPE_PROCESS_CONTINUOUS_CREATE_MATERIAL);
            typePattern.addPattern(TigerConstants.TYPE_PROCESSCONTINUOUSPROVIDE);

            MapList mlMBOMSparePart = dMBOMObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_SPAREMANUFACTURINGITEM, // Relationship Pattern
                    typePattern.getPattern(), // Object Pattern
                    slObjSelectStmts, // Object Selects
                    slRelSelectStmts, // Relationship Selects
                    false, // to direction
                    true, // from direction
                    (short) 0, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    null, // Post Type Pattern
                    null, null, null);

            if (!mlMBOMSparePart.isEmpty()) {
                for (int i = 0; i < mlMBOMSparePart.size(); i++) {
                    Map mMBOMSpare = (Map) mlMBOMSparePart.get(i);
                    String strSpareMBOMId = (String) mMBOMSpare.get("physicalid");
                    String strSpareMBOMRelID = (String) mMBOMSpare.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                    DomainObject domMBOMSpare = DomainObject.newInstance(context, strSpareMBOMId);
                    String strMBOMSpareName = domMBOMSpare.getAttributeValue(context, TigerConstants.ATTRIBUTE_V_NAME);
                    slMBOMNameList.add(strMBOMSpareName);
                    if (!slPSSpareNameList.contains(strMBOMSpareName)) {
                        DomainRelationship.disconnect(context, strSpareMBOMRelID);
                    }
                }
            }

            String plantPID = pss.mbom.MBOMUtil_mxJPO.getMasterPlant(context, mbomPhysicalId);
            if (!slPSSpareList.isEmpty()) {

                // TIGTK-13154 : START
                Map<String, List<String>> workingInfo = new HashMap<String, List<String>>();
                workingInfo.put("instanceToCreate_parentRefPLMID", new ArrayList<String>());
                workingInfo.put("instanceToCreate_childRefPLMID", new ArrayList<String>());
                workingInfo.put("mbomLeafInstancePIDList", new ArrayList<String>());
                workingInfo.put("psPathList", new ArrayList<String>());
                workingInfo.put("newRefPIDList", new ArrayList<String>());
                workingInfo.put("newScopesToCreate_MBOMRefPIDs", new ArrayList<String>());
                workingInfo.put("newScopesToCreate_MBOMRefPLMIDs", new ArrayList<String>());
                workingInfo.put("newScopesToCreate_PSRefPIDs", new ArrayList<String>());
                workingInfo.put("newScopeObjectList", new ArrayList<String>());

                List<Map<String, String>> workingInfo_instanceAttributes = new ArrayList<Map<String, String>>();
                List<Integer> workingInfo_indexInstancesForImplement = new ArrayList<Integer>();
                Set<String> workingInfo_lModelListOnStructure = new HashSet<String>();
                Map<String, String> workingInfo_AppDateToValuate = new HashMap<String, String>();
                // TIGTK-13154 : END

                for (int j = 0; j < slPSSpareList.size(); j++) {
                    String strPSSpareId = (String) slPSSpareList.get(j);
                    DomainObject domPSSpare = DomainObject.newInstance(context, strPSSpareId);
                    String strPSSpareName = domPSSpare.getAttributeValue(context, TigerConstants.ATTRIBUTE_V_NAME);
                    if (!slMBOMNameList.contains(strPSSpareName)) {

                        String mbomType = dMBOMObj.getInfo(context, DomainConstants.SELECT_TYPE);
                        // TIGTK-13154 : START
                        String spareMBOMRefPLMID = PSS_FRCMBOMProg_mxJPO.createMBOMFromEBOMLikePSRecursive_new(context, plmSession, null, null, strPSSpareId, null, workingInfo,
                                workingInfo_lModelListOnStructure, workingInfo_instanceAttributes, workingInfo_indexInstancesForImplement, workingInfo_AppDateToValuate, plantPID, mbomType);
                        String spareMBOMRefPID = PLMID.buildFromString(spareMBOMRefPLMID).getPid();
                        // TIGTK-13154 : END
                        DomainRelationship.connect(context, dMBOMObj, TigerConstants.RELATIONSHIP_PSS_SPAREMANUFACTURINGITEM, DomainObject.newInstance(context, spareMBOMRefPID));

                    }
                }
            }

            flushAndCloseSession(plmSession);
            if (isTransactionActive) {
                ContextUtil.commitTransaction(context);
            }

        } catch (Exception ex) {
            logger.error("Error in updateMBOMSpare : ", ex);
            flushAndCloseSession(plmSession);
            if (isTransactionActive) {
                ContextUtil.abortTransaction(context);
            }
            throw ex;
        }

    }

    // TIGER 2.0 Update MBOM:VB:14/7/2017:End

}
