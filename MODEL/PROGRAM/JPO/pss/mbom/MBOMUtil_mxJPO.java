package pss.mbom;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.dassault_systemes.vplm.modeler.PLMCoreModelerSession;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.UOMUtil;
import com.matrixone.apps.domain.util.eMatrixDateFormat;
import com.matrixone.apps.domain.util.mxType;
import com.matrixone.apps.engineering.EngineeringConstants;
import com.matrixone.apps.framework.ui.UIUtil;
import com.mbom.modeler.utility.FRCMBOMModelerUtility;

import matrix.db.AttributeType;
import matrix.db.AttributeTypeList;
import matrix.db.BusinessInterface;
import matrix.db.BusinessInterfaceList;
import matrix.db.Context;
import matrix.db.Vault;
import matrix.util.Pattern;
import matrix.util.StringList;
import pss.constants.TigerConstants;
import com.matrixone.apps.domain.util.PersonUtil;

@SuppressWarnings("deprecation")
public class MBOMUtil_mxJPO {

    // TIGTK-5405 - 11-04-2017 - VB - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MBOMUtil_mxJPO.class);
    // TIGTK-5405 - 11-04-2017 - VB - END

    public static final String MODIFYPLANT = "MODIFYPLANT";

    public MBOMUtil_mxJPO() {
        super();
        // TODO Auto-generated constructor stub
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static MapList getProgramFromMBOM(Context context, String physicalId) throws Exception {

        MapList mlist = new MapList();
        PLMCoreModelerSession plmSession = null;
        boolean transactionActive = false;
        try {

            Pattern relpattern = new Pattern(TigerConstants.RELATIONSHIP_GBOM);
            relpattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPRODUCT);
            // TIGTK-9295:VB:Start
            relpattern.addPattern(TigerConstants.RELATIONSHIP_EBOM);
            // TIGTK-9295:VB:Start

            // TIGTK-8196:PKH:Start
            // relpattern.addPattern(TigerConstants.RELATIONSHIP_PSS_SUBPROGRAMPROJECT);
            // TIGTK-8196:PKH:Start
            Pattern typepattern = new Pattern(DomainConstants.TYPE_PART);
            typepattern.addPattern(TigerConstants.TYPE_PLMCORE_REFERENCE);
            typepattern.addPattern(TigerConstants.TYPE_VPMREFERENCE);
            typepattern.addPattern(TigerConstants.TYPE_PRODUCTS);
            typepattern.addPattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);
            Pattern finalPattern = new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);
            StringList slselectObjStmts = new StringList(DomainConstants.SELECT_ID);
            slselectObjStmts.addElement(DomainConstants.SELECT_NAME);
            slselectObjStmts.addElement(TigerConstants.ATTRIBUTE_PSS_PROGRAMPROJECT);
            List<String> objectIdList = new ArrayList();
            objectIdList.add(physicalId);

            ContextUtil.startTransaction(context, false);
            transactionActive = true;
            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            List<String> strPhysicalStructureId = FRCMBOMModelerUtility.getScopedPSReferencePIDFromList(context, plmSession, objectIdList);
            if (strPhysicalStructureId != null && strPhysicalStructureId.size() > 0) {
                MapList mapLIst = getPartFromVPMReference(context, strPhysicalStructureId.get(0));
                if (mapLIst != null && mapLIst.size() > 0) {
                    Map objMap = (Map) mapLIst.get(0);
                    DomainObject domPart = DomainObject.newInstance(context, (String) objMap.get(DomainConstants.SELECT_ID));

                    mlist = domPart.getRelatedObjects(context, relpattern.getPattern(), // relationship pattern
                            typepattern.getPattern(), // object pattern
                            slselectObjStmts, // object selects
                            null, // relationship selects
                            true, // to direction
                            false, // from direction
                            (short) 0, // recursion level
                            null, // object where clause
                            null, (short) 0, false, // checkHidden
                            true, // preventDuplicates
                            (short) 1000, // pageSize
                            finalPattern, // Postpattern
                            null, null, null);
                }
            }
            closeSession(plmSession);
            if (transactionActive) {
                ContextUtil.commitTransaction(context);
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getProgramFromMBOM: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            closeSession(plmSession);
            if (transactionActive) {
                ContextUtil.abortTransaction(context);
            }
            throw e;
        }
        return mlist;

    }

    public static void closeSession(PLMCoreModelerSession plmSession) {
        try {
            plmSession.closeSession(true);
        } catch (Exception e) {
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @com.matrixone.apps.framework.ui.ProgramCallable
    public static MapList getPartFromVPMReference(Context context, String sPorductId) throws Exception {
        MapList mlRet = new MapList();
        try {
            if (UIUtil.isNotNullAndNotEmpty(sPorductId)) {
                DomainObject domObj = new DomainObject(sPorductId);
                StringList objSelects = new StringList(4);
                objSelects.addElement(DomainConstants.SELECT_ID);
                objSelects.addElement("physicalid");
                objSelects.addElement(DomainConstants.SELECT_TYPE);
                objSelects.addElement(DomainConstants.SELECT_NAME);
                objSelects.addElement(DomainConstants.SELECT_REVISION);
                String sObjTypes = "*";
                String sRelTypes = EngineeringConstants.RELATIONSHIP_PART_SPECIFICATION;
                // Case 1 : Get Part through Part Spec relationship
                MapList mlRelated = domObj.getRelatedObjects(context, sRelTypes, sObjTypes, objSelects, null, true, false, (short) 1, null, null, 0);
                if (null != mlRelated) {
                    for (int i = 0; i < mlRelated.size(); i++) {
                        Map mObj = (Map) mlRelated.get(i);
                        String sType = (String) mObj.get(DomainConstants.SELECT_TYPE);
                        if (mxType.isOfParentType(context, sType, "Part")) {
                            mlRet.add(mObj);
                        }
                    }
                }

                // Case 2 : Get Part through a query based on TNR
                if (0 == mlRet.size()) {
                    StringList productSelects = new StringList(3);
                    productSelects.addElement(DomainConstants.SELECT_TYPE);
                    productSelects.addElement(DomainConstants.SELECT_NAME);
                    productSelects.addElement("majororder");

                    Map mPart = domObj.getInfo(context, productSelects);

                    /*
                     * String sType = (String)mPart.get(DomainConstants.SELECT_TYPE); String psTypeToSearch = ebomToPSTypeMapping.get(sType); if (psTypeToSearch == null || "".equals(psTypeToSearch))
                     * throw new Exception ("EBOM type " + sType + " has no mapping defined for equivalent Product Structure type.");
                     */

                    String sName = (String) mPart.get(DomainConstants.SELECT_NAME);
                    String sRevIndex = (String) mPart.get("majororder");
                    String sWhere = "minororder == \"" + sRevIndex + "\"";
                    mlRet = DomainObject.findObjects(context, DomainConstants.TYPE_PART, sName, DomainConstants.QUERY_WILDCARD, DomainConstants.QUERY_WILDCARD, TigerConstants.VAULT_ESERVICEPRODUCTION,
                            sWhere, true, objSelects);
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getPartFromVPMReference: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
        return mlRet;
    }

    /**
     * Function to update or add new classification
     * @param context
     * @param objectId
     * @param classificationStr
     * @throws Exception
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void addORUpdateClassification(Context context, String objectId, String classificationStr) throws Exception {
        ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, TigerConstants.PERSON_USER_AGENT), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
        try {
            if (UIUtil.isNotNullAndNotEmpty(objectId) && UIUtil.isNotNullAndNotEmpty(classificationStr)) {
                StringList classificationList = FrameworkUtil.split(classificationStr, "|");
                DomainObject dObj = DomainObject.newInstance(context, objectId);
                BusinessInterfaceList interfaceList = dObj.getBusinessInterfaces(context, true);
                BusinessInterfaceList classificationToUpdated = new BusinessInterfaceList();
                for (int i = 0; i < interfaceList.size(); i++) {
                    BusinessInterface bInterface = interfaceList.getElement(i);
                    if (classificationList.contains(bInterface.getName())) {
                        classificationToUpdated.addElement(bInterface);
                        classificationList.remove(bInterface.getName());
                    }
                }

                MapList mapList = pss.mbom.MBOMUtil_mxJPO.getPartFromVPMReference(context, objectId);
                String partId = null;
                if (mapList != null && mapList.size() > 0) {
                    partId = (String) ((Map) mapList.get(0)).get(DomainConstants.SELECT_ID);
                }
                if (UIUtil.isNotNullAndNotEmpty(partId)) {
                    DomainObject partObj = DomainObject.newInstance(context, partId);
                    Map attrMap = partObj.getAttributeMap(context, true);
                    Map attrMapPS = new HashMap();
                    // TIGTK-16166:20-07-2018:START
                    StringList slEBOMMassAttributes = new StringList(3);
                    slEBOMMassAttributes.addElement(TigerConstants.ATTRIBUTE_PSS_EBOM_Mass1);
                    slEBOMMassAttributes.addElement(TigerConstants.ATTRIBUTE_PSS_EBOM_Mass2);
                    slEBOMMassAttributes.addElement(TigerConstants.ATTRIBUTE_PSS_EBOM_Mass3);
                    // TIGTK-16166:20-07-2018:END

                    for (int i = 0; i < classificationList.size(); i++) {
                        String interfaceName = (String) classificationList.get(i);
                        DomainRelationship.connect(context, DomainObject.newInstance(context, interfaceName), DomainConstants.RELATIONSHIP_CLASSIFIED_ITEM, dObj);
                        BusinessInterface interfaceObj = new BusinessInterface(interfaceName, new Vault(dObj.getVault()));
                        AttributeTypeList attributeTypeList = interfaceObj.getAttributeTypes(context);
                        Iterator itrAttr = attributeTypeList.iterator();
                        while (itrAttr.hasNext()) {
                            String attrName = ((AttributeType) itrAttr.next()).getName();
                            String attrValue = (String) attrMap.get(attrName);
                            if (attrValue == null) {
                                attrValue = "";
                            }
                            // TIGTK-16166:20-07-2018:START
                            if (slEBOMMassAttributes.contains(attrName)) {
                                String strMassUnit = UOMUtil.getInputunit(context, partId, attrName);
                                String strMass = UOMUtil.getInputValue(context, partId, attrName);
                                String strNewMassUnitValue = strMass + " " + strMassUnit;
                                attrMapPS.put(attrName, strNewMassUnitValue);
                            } else {
                                // TIGTK-16166:20-07-2018:END
                                attrMapPS.put(attrName, attrValue);
                            }
                        }
                    }

                    for (int i = 0; i < classificationToUpdated.size(); i++) {
                        BusinessInterface bInterface = classificationToUpdated.getElement(i);
                        AttributeTypeList attributeTypeList = bInterface.getAttributeTypes(context);
                        Iterator itrAttr = attributeTypeList.iterator();
                        while (itrAttr.hasNext()) {
                            String attrName = ((AttributeType) itrAttr.next()).getName();
                            String attrValue = (String) attrMap.get(attrName);
                            if (attrValue == null) {
                                attrValue = "";
                            }
                            // TIGTK-16166:20-07-2018:START
                            if (slEBOMMassAttributes.contains(attrName)) {
                                String strMassUnit = UOMUtil.getInputunit(context, partId, attrName);
                                String strMass = UOMUtil.getInputValue(context, partId, attrName);
                                String strNewMassUnitValue = strMass + " " + strMassUnit;
                                attrMapPS.put(attrName, strNewMassUnitValue);
                            } else {
                                // TIGTK-16166:20-07-2018:END
                                attrMapPS.put(attrName, attrValue);
                            }
                        }
                    }
                    dObj.setAttributeValues(context, attrMapPS);
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in addORUpdateClassification: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        } finally {
            ContextUtil.popContext(context);
        }
    }

    /**
     * @param context
     * @param mbomRefPID
     * @param pcPID
     * @return
     * @throws Exception
     */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public static String getVariantAssemblyPIDForMBOM(Context context, String mbomRefPID, String pcPID) throws Exception {
        String strVarintID = DomainObject.EMPTY_STRING;
        try {
            if (UIUtil.isNotNullAndNotEmpty(pcPID) && UIUtil.isNotNullAndNotEmpty(mbomRefPID)) {
                DomainObject domMBOM = DomainObject.newInstance(context, mbomRefPID);
                String strObjectWhere = "from[" + TigerConstants.RELATIONSHIP_PSS_VARIANT_ASSEMBLY_PRODUCT_CONFIGURATION + "].to.physicalid == '" + pcPID + "'";
                MapList mlVariantAssembly = domMBOM.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_PARTVARIANTASSEMBLY, // relationship pattern
                        TigerConstants.TYPE_PSS_MBOM_VARIANT_ASSEMBLY, // object pattern
                        new StringList("physicalid"), // object selects
                        null, // relationship selects
                        false, // to direction
                        true, // from direction
                        (short) 1, // recursion level
                        strObjectWhere, // object where clause
                        null, (short) 0, false, // checkHidden
                        true, // preventDuplicates
                        (short) 1000, // pageSize
                        null, // Postpattern
                        null, null, null);
                if (mlVariantAssembly != null && !(mlVariantAssembly.isEmpty())) {
                    for (int k = 0; k < mlVariantAssembly.size(); k++) {
                        Map mVariantAssembly = (Map) mlVariantAssembly.get(k);
                        strVarintID = (String) mVariantAssembly.get("physicalid");
                    }
                } else {
                    strVarintID = "";
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getVariantAssemblyPIDForMBOM: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
        return strVarintID;
    }

    public static StringList getStringListValue(Object valueObj) throws Exception {
        if (valueObj instanceof String) {
            return new StringList((String) valueObj);
        } else {
            return (StringList) valueObj;
        }
    }

    public static String getMasterPlant(Context context, String strMBOMPID) throws Exception {
        String strMfgProductionConnectedMasterPlant = DomainConstants.EMPTY_STRING;
        String sAssemblyPID = DomainConstants.EMPTY_STRING;

        try {
            if (UIUtil.isNotNullAndNotEmpty(strMBOMPID)) {
                sAssemblyPID = MqlUtil.mqlCommand(context, "print bus " + strMBOMPID + " select physicalid dump |", false, false);

                String strquery = "query path type SemanticRelation containing " + sAssemblyPID + " where owner.type=='MfgProductionPlanning' select owner.physicalid dump |";
                String listPathIds = MqlUtil.mqlCommand(context, strquery, false, false);
                if (UIUtil.isNotNullAndNotEmpty(listPathIds)) {
                    StringList listMfgProuctionPlanningPID = new StringList();
                    String[] strOwnerArray = listPathIds.split("\n");

                    for (int i = 0; i < strOwnerArray.length; i++) {
                        String strPhysicalId = strOwnerArray[i];
                        listMfgProuctionPlanningPID.add(strPhysicalId);

                    }
                    if (!listMfgProuctionPlanningPID.isEmpty()) {
                        int slListSize = listMfgProuctionPlanningPID.size();
                        for (int i = 0; i < slListSize; i++) {
                            String strMfgProuctionDomainObjectPlanningPID = (String) listMfgProuctionPlanningPID.get(i);
                            String strMfgPlanningId = strMfgProuctionDomainObjectPlanningPID.split("\\|")[1];
                            DomainObject dMfgProductionPlannigObj = DomainObject.newInstance(context, strMfgPlanningId);
                            String strPlantOwnership = dMfgProductionPlannigObj.getAttributeValue(context, "PSS_ManufacturingPlantExt.PSS_Ownership");
                            if (strPlantOwnership.equalsIgnoreCase("Master")) {

                                Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_PLANT);
                                StringList slObjSelectStmts = new StringList(1);
                                slObjSelectStmts.addElement("physicalid");
                                StringList slRelSelectStmts = new StringList(1);
                                slRelSelectStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

                                MapList mlMfgProductionConnectedPlant = dMfgProductionPlannigObj.getRelatedObjects(context, "VPLMrel/PLMConnection/V_Owner", // Relationship
                                        // Pattern
                                        typePattern.getPattern(), // Object Pattern
                                        slObjSelectStmts, // Object Selects
                                        slRelSelectStmts, // Relationship Selects
                                        true, // to direction
                                        false, // from direction
                                        (short) 0, // recursion level
                                        null, // object where clause
                                        null, (short) 0, false, // checkHidden
                                        true, // preventDuplicates
                                        (short) 1000, // pageSize
                                        null, // Post Type Pattern
                                        null, null, null);

                                if (!mlMfgProductionConnectedPlant.isEmpty()) {
                                    Iterator itr = mlMfgProductionConnectedPlant.iterator();
                                    while (itr.hasNext()) {
                                        Map mConnectedPlant = (Map) itr.next();
                                        strMfgProductionConnectedMasterPlant = (String) mConnectedPlant.get("physicalid");
                                    }
                                }

                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Error in getMasterPlant: ", e);
        }
        return strMfgProductionConnectedMasterPlant;
    }

    // TIGTK-7259: TS :17/7/2017:START
    public static void createPublishControlObject(Context context, String psRefPID, String mbomRefPID) throws Exception {
        try {

            DomainObject dObj = DomainObject.newInstance(context, mbomRefPID);
            String strPCOName = DomainObject.getAutoGeneratedName(context, "type_PSS_PublishControlObject", "");
            String strRevision = "01.1";
            DomainObject dNewPublishControlObj = DomainObject.newInstance(context);
            dNewPublishControlObj.createObject(context, TigerConstants.TYPE_PSS_PUBLISHCONTROLOBJECT, strPCOName, strRevision, TigerConstants.POLICY_PSS_PUBLISHCONTROLOBJECT,
                    TigerConstants.VAULT_VPLM);
            DomainRelationship.connect(context, dObj, TigerConstants.RELATIONSHIP_PSS_PUBLISHCONTROLOBJECT, dNewPublishControlObj);
            SimpleDateFormat MATRIX_DATE_FORMAT = new SimpleDateFormat(eMatrixDateFormat.getEMatrixDateFormat(), Locale.US);
            String strTimeStamp = MATRIX_DATE_FORMAT.format(new Date());
            dNewPublishControlObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_UPDATETIMESTAMP, strTimeStamp);
            DomainObject dObjPhysicalProduct = DomainObject.newInstance(context, psRefPID);
            DomainRelationship.connect(context, dObjPhysicalProduct, TigerConstants.RELATIONSHIP_PSS_PUBLISHCONTROLOBJECT, dNewPublishControlObj);
        } catch (Exception e) {

            logger.error("Error in createPublishControlObject: ", e);
        }
    }
    // TIGTK-7259: TS :17/7/2017:END

    /**
     * This method get the MfgProductionPlanning object which is connected to master plant. FRC-139
     * @param context
     * @param strMBOMPID
     * @return
     * @throws Exception
     */
    public static String getMasterMfgProductionPlanning(Context context, String strMBOMPID) throws Exception {
        try {
            String strMfgProuctionPlanningPID = DomainConstants.EMPTY_STRING;
            if (UIUtil.isNotNullAndNotEmpty(strMBOMPID)) {
                String sAssemblyPID = MqlUtil.mqlCommand(context, "print bus " + strMBOMPID + " select physicalid dump |", false, false);
                String strquery = "query path type SemanticRelation containing " + sAssemblyPID + " where owner.type=='MfgProductionPlanning' select owner.physicalid dump |";
                String listPathIds = MqlUtil.mqlCommand(context, strquery, false, false);
                if (UIUtil.isNotNullAndNotEmpty(listPathIds)) {
                    StringList listMfgProuctionPlanningPID = new StringList();
                    String[] strOwnerArray = listPathIds.split("\n");

                    for (int i = 0; i < strOwnerArray.length; i++) {
                        String strPhysicalId = strOwnerArray[i];
                        listMfgProuctionPlanningPID.add(strPhysicalId);
                    }

                    if (!listMfgProuctionPlanningPID.isEmpty()) {
                        int slListSize = listMfgProuctionPlanningPID.size();
                        for (int i = 0; i < slListSize; i++) {
                            String strMfgProuctionDomainObjectPlanningPID = (String) listMfgProuctionPlanningPID.get(i);
                            String strMfgPlanningId = strMfgProuctionDomainObjectPlanningPID.split("\\|")[1];
                            DomainObject dMfgProductionPlannigObj = DomainObject.newInstance(context, strMfgPlanningId);
                            String strPlantOwnership = dMfgProductionPlannigObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGPLANTEXTPSS_OWNERSHIP);

                            if (strPlantOwnership.equalsIgnoreCase("Master"))
                                strMfgProuctionPlanningPID = strMfgPlanningId;
                        }
                    }
                }
            }
            return strMfgProuctionPlanningPID;
        } catch (Exception e) {
            logger.error("Error in getMasterMfgProductionPlanning: ", e);
            throw e;
        }
    }

    /**
     * This method return the parent element of dropped MBOM object during Implement Link
     * @param context
     * @param strObjectid
     * @param slParentList
     * @throws Exception
     */
    public static void getParentVPMStructure(Context context, String strObjectid, List<String> slParentList) throws Exception {

        try {
            if (UIUtil.isNotNullAndNotEmpty(strObjectid)) {
                DomainObject objectDOM = DomainObject.newInstance(context, strObjectid);
                StringList busSelect = new StringList(TigerConstants.SELECT_PHYSICALID);

                MapList mlObjectList = objectDOM.getRelatedObjects(context, "PLMCoreInstance", "PLMCoreReference", busSelect, null, true, false, (short) 0, DomainConstants.EMPTY_STRING,
                        DomainConstants.EMPTY_STRING, 0);
                if (!mlObjectList.isEmpty()) {
                    Iterator itr = mlObjectList.iterator();
                    while (itr.hasNext()) {
                        Map mConnectedObjects = (Map) itr.next();
                        slParentList.add((String) mConnectedObjects.get("physicalid"));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error in getParentVPMStructure: ", e);
            throw e;
        }

    }

    public static StringList getMfgProductionPlanning(Context context, String strMBOMPID) throws Exception {
        StringList listMfgProuctionPlanningID = new StringList();
        try {

            if (UIUtil.isNotNullAndNotEmpty(strMBOMPID)) {
                String sAssemblyPID = MqlUtil.mqlCommand(context, "print bus " + strMBOMPID + " select physicalid dump |", false, false);

                String strquery = "query path type SemanticRelation containing " + sAssemblyPID + " where owner.type=='MfgProductionPlanning' select owner.physicalid dump |";
                String listPathIds = MqlUtil.mqlCommand(context, strquery, false, false);
                if (UIUtil.isNotNullAndNotEmpty(listPathIds)) {
                    StringList listMfgProuctionPlanningPID = new StringList();
                    String[] strOwnerArray = listPathIds.split("\n");

                    for (int i = 0; i < strOwnerArray.length; i++) {
                        String strPhysicalId = strOwnerArray[i];
                        listMfgProuctionPlanningPID.add(strPhysicalId);

                    }
                    if (!listMfgProuctionPlanningPID.isEmpty()) {
                        int slListSize = listMfgProuctionPlanningPID.size();
                        for (int i = 0; i < slListSize; i++) {
                            String strMfgProuctionDomainObjectPlanningPID = (String) listMfgProuctionPlanningPID.get(i);
                            String strMfgPlanningId = strMfgProuctionDomainObjectPlanningPID.split("\\|")[1];
                            listMfgProuctionPlanningID.add(strMfgPlanningId);
                        }
                    }
                }
            }
            return listMfgProuctionPlanningID;
        } catch (Exception e) {
            logger.error("Error in getMfgProductionPlanning: ", e);
            throw e;
        }

    }

    /**
     * Method to perform additional/custom action after creating Delmia connection.
     * @param context
     * @param instancePID
     * @throws Exception
     */
    public static void postCreateInstance(Context context, String instancePID) throws Exception {
        // TIGTK-8444:Rutuja Ekatpure :If Master for parent and child are different then make chils type as MakeBuy: 31/7/2017:Start
        logger.debug("postCreateInstance:::Start");
        try {
            String strConnectionCommand = "print connection " + instancePID + " select from.id to.id dump |";
            String strConnectionResult = MqlUtil.mqlCommand(context, strConnectionCommand, false, false);
            String[] slFromToConnection = strConnectionResult.split("\\|");
            String strFromMBOMMasterPlant = pss.mbom.MBOMUtil_mxJPO.getMasterPlant(context, slFromToConnection[0]);
            String strToMBOMMasterPlant = pss.mbom.MBOMUtil_mxJPO.getMasterPlant(context, slFromToConnection[1]);
            DomainObject domTo = new DomainObject(slFromToConnection[1]);
            // TIGTK-10100:Rutuja Ekatpure:Start
            String strToObjPolicy = domTo.getInfo(context, DomainConstants.SELECT_POLICY);
            DomainRelationship domRel = new DomainRelationship(instancePID);
            if (TigerConstants.POLICY_PSS_STANDARDMBOM.equals(strToObjPolicy)) {
                domRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_TYPE_OF_PART, "Buy");
                logger.debug("postCreateInstance:::Set Type Of Part as Buy on instanceId ::" + instancePID);
            } else {
                if (!strFromMBOMMasterPlant.equalsIgnoreCase(strToMBOMMasterPlant)
                        && !(domTo.isKindOf(context, TigerConstants.TYPE_PROCESSCONTINUOUSPROVIDE) || domTo.isKindOf(context, TigerConstants.TYPE_PROCESS_CONTINUOUS_CREATE_MATERIAL))) {
                    domRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_TYPE_OF_PART, "MakeBuy");
                    logger.debug("postCreateInstance:::Set Type Of Part as MakeBuy on instanceId ::" + instancePID);
                }
            }
            // TIGTK-10100:Rutuja Ekatpure:End
        } catch (Exception e) {
            logger.error("Error in postCreateInstance", e);
            throw e;
        }
        logger.debug("postCreateInstance:::End");
        // TIGTK-8444:Rutuja Ekatpure : 31/7/2017:End
    }

    // TIGTK-10100:Rutuja Ekatpure:Start
    /***
     * this method used to get Consumer plant from Standard MBOM
     * @param context
     * @param strStandardMBOMPID
     * @return
     * @throws Exception
     */
    public static StringList getConsumerPlantOnStandardMBOM(Context context, String strStandardMBOMPID) throws Exception {
        logger.debug("getConsumerPlantOnStandardMBOM: Start ");
        StringList slMfgProductionConnectedConsumerPlant = new StringList();
        try {
            StringList slMfgProdPlanId = getMfgProductionPlanning(context, strStandardMBOMPID);

            if (!slMfgProdPlanId.isEmpty()) {
                int slListSize = slMfgProdPlanId.size();
                for (int i = 0; i < slListSize; i++) {
                    String strMfgPlanningId = (String) slMfgProdPlanId.get(i);
                    DomainObject dMfgProductionPlannigObj = DomainObject.newInstance(context, strMfgPlanningId);
                    String strPlantOwnership = dMfgProductionPlannigObj.getAttributeValue(context, "PSS_ManufacturingPlantExt.PSS_Ownership");
                    if (strPlantOwnership.equalsIgnoreCase("Consumer")) {

                        Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_PLANT);
                        StringList slObjSelectStmts = new StringList(1);
                        slObjSelectStmts.addElement("physicalid");
                        StringList slRelSelectStmts = new StringList(1);
                        slRelSelectStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

                        MapList mlMfgProductionConnectedPlant = dMfgProductionPlannigObj.getRelatedObjects(context, "VPLMrel/PLMConnection/V_Owner", // Relationship
                                // Pattern
                                typePattern.getPattern(), // Object Pattern
                                slObjSelectStmts, // Object Selects
                                slRelSelectStmts, // Relationship Selects
                                true, // to direction
                                false, // from direction
                                (short) 0, // recursion level
                                null, // object where clause
                                null, (short) 0, false, // checkHidden
                                true, // preventDuplicates
                                (short) 1000, // pageSize
                                null, // Post Type Pattern
                                null, null, null);

                        if (!mlMfgProductionConnectedPlant.isEmpty()) {
                            Iterator itr = mlMfgProductionConnectedPlant.iterator();
                            while (itr.hasNext()) {
                                Map mConnectedPlant = (Map) itr.next();
                                slMfgProductionConnectedConsumerPlant.add(mConnectedPlant.get("physicalid"));
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Error in getConsumerPlantOnStandardMBOM: ", e);
        }
        logger.debug("getConsumerPlantOnStandardMBOM: Consumer Plant Id::" + slMfgProductionConnectedConsumerPlant);
        logger.debug("getConsumerPlantOnStandardMBOM: End ");
        return slMfgProductionConnectedConsumerPlant;
    }
    // TIGTK-10100:Rutuja Ekatpure:End

    // TIGTK-10565:Rutuja Ekatpure:Start
    /***
     * this method used to check Master plant of parent child are matching or not,if not then pass true,else false.
     * @param context
     * @param ChildId
     *            , ParentId
     * @return boolean
     * @throws Exception
     */
    public boolean checkMasterPlantOnMBOM(Context context, String[] args) throws Exception {
        logger.debug("checkMasterPlantOnMBOM: Start ");
        boolean returnValue = true;
        String strChildId = args[0];
        String strParentId = args[1];
        String strChildMasterPlant = getMasterPlant(context, strChildId);
        if (UIUtil.isNullOrEmpty(strChildMasterPlant))
            returnValue = false;
        else if (UIUtil.isNotNullAndNotEmpty(strParentId)) {
            String strParentMasterPlant = getMasterPlant(context, strParentId);
            if (UIUtil.isNotNullAndNotEmpty(strParentMasterPlant)) {
                if (strParentMasterPlant.equals(strChildMasterPlant))
                    returnValue = false;
            }
        }
        logger.debug("checkMasterPlantOnMBOM: returnValue  : " + returnValue);
        logger.debug("checkMasterPlantOnMBOM: End ");
        return returnValue;
    }

    /***
     * this method used to check Selected MBOM is Standard MBOM or not,if yes then user is GTS Engineer.if both case true then return true for revise else false.
     * @param context
     * @param ChildId
     *            , ParentId
     * @return boolean
     * @throws Exception
     */
    public boolean checkForStdMBOMRevision(Context context, String[] args) throws Exception {
        logger.debug("checkForStdMBOMRevision: Start ");
        boolean returnValue = false;

        String userName = context.getUser();
        String strLoggedUserSecurityContext = PersonUtil.getDefaultSecurityContext(context, userName);
        String assignedRoles = (strLoggedUserSecurityContext.split("[.]")[0]);
        if (assignedRoles.equals(TigerConstants.ROLE_PSS_GTS_ENGINEER)) {
            returnValue = true;
        }
        logger.debug("checkForStdMBOMRevision: returnValue  :" + returnValue);
        logger.debug("checkForStdMBOMRevision: End ");
        return returnValue;
    }
    // TIGTK-10565:Rutuja Ekatpure:End

    /**
     * This method is call when plant is removed from MBOM to Check whether it has MCO or CN.
     * @param context
     * @param strMBOMObjectId
     * @return
     * @throws Exception
     *             VB : TIGTK-10700 : 07/12/2017
     */

    public static int checkMBOMIsConnectedToChangeManagment(Context context, String strMBOMObjectId) throws Exception {
        int intReturn = 0;
        try {

            String strRPEValueId = PropertyUtil.getGlobalRPEValue(context, MODIFYPLANT);
            if (UIUtil.isNullOrEmpty(strRPEValueId) && !strRPEValueId.equalsIgnoreCase("FALSE")) {

                if (UIUtil.isNotNullAndNotEmpty(strMBOMObjectId)) {
                    DomainObject domMBOM = DomainObject.newInstance(context, strMBOMObjectId);

                    StringList slObjectSelect = new StringList(1);
                    slObjectSelect.add(DomainConstants.SELECT_ID);

                    String strObjectWhereMCA = "current == '" + TigerConstants.STATE_PSS_MCA_PREPARE + "'||current == '" + TigerConstants.STATE_PSS_MCA_INWORK + "'|| current == '"
                            + TigerConstants.STATE_PSS_MCA_INREVIEW + "'";

                    // Get connected MCAs of MBOM
                    MapList mlConnectedActiveMCA = domMBOM.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEAFFECTEDITEM,
                            TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION, slObjectSelect, DomainConstants.EMPTY_STRINGLIST, true, false, (short) 1, strObjectWhereMCA, null, (short) 0);

                    String strObjectWhereMCO = "current == '" + TigerConstants.STATE_PSS_MCO_PREPARE + "'||current == '" + TigerConstants.STATE_PSS_MCO_INWORK + "'|| current == '"
                            + TigerConstants.STATE_PSS_MCO_INREVIEW + "'";

                    MapList mlConnectedActiveCN = getConnectedActiveCN(context, strMBOMObjectId);

                    // Get MCO connected with relationship Related150MBOM
                    MapList mlConnectedActive150MCO = domMBOM.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_RELATED150MBOM, TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER,
                            slObjectSelect, DomainConstants.EMPTY_STRINGLIST, true, false, (short) 1, strObjectWhereMCO, null, (short) 0);

                    if (!mlConnectedActiveMCA.isEmpty() || !mlConnectedActive150MCO.isEmpty() || !mlConnectedActiveCN.isEmpty()) {
                        intReturn = 1;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error in checkMBOMIsConnectedToChangeManagment: ", e);
            throw e;
        } finally {
            PropertyUtil.setGlobalRPEValue(context, MODIFYPLANT, "");
        }
        return intReturn;
    }

    /**
     * This method is return the Active CN connected to MCO
     * @param context
     * @param strMBOMObjectId
     * @return
     * @throws Exception
     *             VB : TIGTK-10700 : 07/12/2017
     */
    public static MapList getConnectedActiveCN(Context context, String strMBOMObjectId) throws Exception {

        try {
            MapList mlConnectedCN = new MapList();
            StringList slObjectSelect = new StringList(1);
            slObjectSelect.add(DomainConstants.SELECT_ID);

            Pattern relPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION);
            relPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEAFFECTEDITEM);

            Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION);
            typePattern.addPattern(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER);

            Pattern postPattern = new Pattern(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER);

            DomainObject domMBOM = DomainObject.newInstance(context, strMBOMObjectId);
            MapList mlConnectedActive150MCO = domMBOM.getRelatedObjects(context, relPattern.getPattern(), // relationship pattern
                    typePattern.getPattern(), // object pattern
                    slObjectSelect, // object selects
                    null, // relationship selects
                    true, // to direction
                    false, // from direction
                    (short) 0, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    postPattern, // Postpattern
                    null, null, null);

            if (!mlConnectedActive150MCO.isEmpty()) {
                for (int i = 0; i < mlConnectedActive150MCO.size(); i++) {
                    Map mConnectedMCO = (Map) mlConnectedActive150MCO.get(i);
                    String strMCOId = (String) mConnectedMCO.get(DomainConstants.SELECT_ID);
                    DomainObject domMCO = DomainObject.newInstance(context, strMCOId);

                    String strObjectWhereCN = "current == '" + TigerConstants.STATE_TRANSFERERROR + "'||current == '" + TigerConstants.STATE_INREVIEW_CN + "'|| current == '"
                            + TigerConstants.STATE_INTRANSFER + "' || current == '" + TigerConstants.STATE_PREPARE_CN + "'";

                    mlConnectedCN = domMCO.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_RELATEDCN, TigerConstants.TYPE_PSS_CHANGENOTICE, slObjectSelect, DomainConstants.EMPTY_STRINGLIST,
                            false, true, (short) 0, strObjectWhereCN, null, (short) 0);

                }
            }

            return mlConnectedCN;
        } catch (Exception e) {
            logger.error("Error in getConnectedActiveCN: ", e);
            throw e;
        }
    }

}
