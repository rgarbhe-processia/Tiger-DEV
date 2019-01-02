package pss.mbom;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import com.dassault_systemes.vplm.modeler.PLMCoreModelerSession;
import com.matrixone.apps.common.Route;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.DomainSymbolicConstants;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MailUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.UOMUtil;
import com.matrixone.apps.engineering.EngineeringUtil;
import com.matrixone.apps.framework.ui.UIUtil;
import com.mbom.modeler.utility.FRCMBOMModelerUtility;

import matrix.db.BusinessInterface;
import matrix.db.BusinessInterfaceList;
import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.RelationshipType;
import matrix.db.User;
import matrix.util.Pattern;
import matrix.util.StringList;
import pss.constants.TigerConstants;

public class Triggers_mxJPO {

    // TIGTK-5405 - 11-04-2017 - VB - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Triggers_mxJPO.class);
    // TIGTK-5405 - 11-04-2017 - VB - END

    public static final String PLANTOWNERSHIP = "PLANT_OWNERSHIP";

    public static final String PLANTFROMENOVIA = "PSS_PLANT_CALLING_FROM_ENOVIA";

    public static final String MODIFYPLANT = "MODIFYPLANT";

    public static final String PROPOGRATEHARMONYASSOCIATION = "PROPOGRATEHARMONYASSOCIATION";

    public Triggers_mxJPO() {
        super();
        // TODO Auto-generated constructor stub
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public int promoteHarmonyRequest(Context context, String[] args) throws Exception {

        try {
            String strInboxTaskId = args[0];
            DomainObject domInboxTask = DomainObject.newInstance(context, strInboxTaskId);

            StringList objectSelects = new StringList(DomainConstants.SELECT_ID);

            MapList mList = domInboxTask.getRelatedObjects(context, DomainConstants.RELATIONSHIP_ROUTE_TASK, // relationship pattern
                    DomainConstants.TYPE_ROUTE, // object pattern
                    objectSelects, // object selects
                    null, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 1, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    null, // postPattern
                    null, null, null);
            if (mList.size() > 0) {
                Map<String, String> map = (Map<String, String>) mList.get(0);

                String strRouteId = (String) map.get(DomainConstants.SELECT_ID);
                DomainObject domRoute = DomainObject.newInstance(context, strRouteId);
                MapList mHarmonyList = domRoute.getRelatedObjects(context, TigerConstants.RELATIONSHIP_OBJECT_ROUTE, // relationship pattern
                        TigerConstants.TYPE_PSS_HARMONY_REQUEST, // object pattern
                        objectSelects, // object selects
                        null, // relationship selects
                        true, // to direction
                        false, // from direction
                        (short) 1, // recursion level
                        null, null, 0);

                if (mHarmonyList.size() > 0) {
                    Map mHarmonyMap = (Map) mHarmonyList.get(0);
                    String strHarmonyRequestId = (String) mHarmonyMap.get(DomainConstants.SELECT_ID);

                    DomainObject domHarmonyRequest = DomainObject.newInstance(context, strHarmonyRequestId);
                    String strAttrApprovalStatus = domInboxTask.getAttributeValue(context, TigerConstants.ATTRIBUTE_APPROVAL_STATUS);

                    Object strCurrent = domInboxTask.getInfo(context, DomainConstants.SELECT_CURRENT);

                    if ((DomainConstants.STATE_INBOX_TASK_COMPLETE.equals(strCurrent)) && (strAttrApprovalStatus.equals(TigerConstants.APPROVAL_STATUS))) {
                        domHarmonyRequest.setAttributeValue(context, TigerConstants.ATTRIBUTE_BRANCH_TO, TigerConstants.STATE_PSS_HARMONYREQUEST_CANCELLED);

                    }

                    domHarmonyRequest.promote(context);
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in promoteHarmonyRequest: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
        return 0;

    }

    @SuppressWarnings("rawtypes")
    public int addRouteToHarmonyRequest(Context context, String[] args) throws Exception {
        try {
            if (args == null || args.length < 1) {
                throw (new IllegalArgumentException());
            }

            String defaultRDOName = EngineeringUtil.getDefaultOrganization(context);
            final String POLICY_PSS_HARMONYREQUEST = "policy_PSS_HarmonyRequest";
            String ROUTE_TEMPLATE_NAME = "Validate Harmony Request_" + defaultRDOName;
            String strHarmonyRequestId = args[0];
            DomainObject domHarmonyRequest = DomainObject.newInstance(context, strHarmonyRequestId);
            StringList busSelects = new StringList();
            busSelects.add(DomainConstants.SELECT_NAME);
            busSelects.add(DomainConstants.SELECT_ID);
            busSelects.add(DomainConstants.SELECT_REVISION);
            busSelects.add(DomainConstants.SELECT_DESCRIPTION);

            MapList mlRouteTemplates = DomainObject.findObjects(context, DomainConstants.TYPE_ROUTE_TEMPLATE, // Type Pattern
                    ROUTE_TEMPLATE_NAME, // Name Pattern
                    DomainConstants.QUERY_WILDCARD, // Rev Pattern
                    DomainConstants.QUERY_WILDCARD, // Owner Pattern
                    TigerConstants.VAULT_ESERVICEPRODUCTION, // Vault Pattern
                    null, // Where Expression
                    true, // Expand Type
                    busSelects); // Object Pattern

            if (mlRouteTemplates.size() > 0) {

                Map mRouteTemplate = (Map) mlRouteTemplates.get(0);

                String strRouteTemplateId = (String) mRouteTemplate.get(DomainConstants.SELECT_ID);

                String strDescriptionTemplate = (String) mRouteTemplate.get(DomainConstants.SELECT_DESCRIPTION);

                String strName = DomainObject.getAutoGeneratedName(context, DomainSymbolicConstants.SYMBOLIC_type_Route, "");

                Route newRoute = new Route();
                newRoute.createObject(context, DomainConstants.TYPE_ROUTE, strName, TigerConstants.ROUTE_REVISION, DomainConstants.POLICY_ROUTE, context.getVault().getName());
                String strRouteId = newRoute.getInfo(context, DomainConstants.SELECT_ID);

                newRoute.setId(strRouteId); // addMembersFromTemplate was setting the route id as route task user id

                newRoute.setDescription(context, strDescriptionTemplate);

                // Create new Route using Route Template
                newRoute.setAttributeValue(context, DomainConstants.ATTRIBUTE_ROUTE_BASE_PURPOSE, TigerConstants.ROUTE_BASE_PURPOSE_APPROVAL);

                newRoute.connectTemplate(context, strRouteTemplateId);
                newRoute.addMembersFromTemplate(context, strRouteTemplateId);
                // Add Relationship
                DomainRelationship dRel = domHarmonyRequest.addRelatedObject(context, new RelationshipType(DomainConstants.RELATIONSHIP_OBJECT_ROUTE), false, strRouteId);

                try {
                    // Add Attribute to Relationship
                    Map<String, String> mapAttribute = new HashMap<String, String>();
                    mapAttribute.put(DomainConstants.ATTRIBUTE_ROUTE_BASE_POLICY, POLICY_PSS_HARMONYREQUEST);
                    // TIGTK-11710:RE:28/11/2017:Start
                    mapAttribute.put(DomainConstants.ATTRIBUTE_ROUTE_BASE_STATE, "state_Create");
                    // TIGTK-11710:RE:28/11/2017:End
                    mapAttribute.put(DomainConstants.ATTRIBUTE_ROUTE_BASE_PURPOSE, TigerConstants.ROUTE_BASE_PURPOSE_APPROVAL);
                    dRel.setAttributeValues(context, mapAttribute);

                } catch (Exception e) {
                    // TIGTK-5405 - 11-04-2017 - VB - START
                    logger.error("Error in addRouteToHarmonyRequest: ", e);
                    // TIGTK-5405 - 11-04-2017 - VB - END
                }
            }

            return 0;
        } catch (Exception ex) {
            // TIGTK-5405 - 5-11-2017 - PTE - START
            logger.error("Error in addRouteToHarmonyRequest: ", ex);
            // TIGTK-5405 - 5-11-2017 - PTE - END
            throw ex;
        }
    }

    @SuppressWarnings("rawtypes")
    public int checkRouteStarted(Context context, String[] args) throws Exception {

        try {

            String strObjectId = args[0];
            DomainObject domHarmonyRequest = DomainObject.newInstance(context, strObjectId);
            StringList busselects = new StringList(DomainConstants.SELECT_ID);
            Map map = domHarmonyRequest.getRelatedObject(context, TigerConstants.RELATIONSHIP_OBJECT_ROUTE, true, busselects, null);
            String strRouteId = (String) map.get(DomainConstants.SELECT_ID);
            DomainObject domRoue = DomainObject.newInstance(context, strRouteId);
            domRoue.setAttributeValue(context, TigerConstants.ATTRIBUTE_ROUTE_STATUS, "Started");

        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in checkRouteStarted: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
        return 0;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public int promoteEquipmentRequest(Context context, String[] args) throws Exception {

        try {
            String strInboxTaskId = args[0];
            DomainObject domInboxTask = DomainObject.newInstance(context, strInboxTaskId);
            StringList objectSelects = new StringList(DomainConstants.SELECT_ID);
            MapList mList = domInboxTask.getRelatedObjects(context, DomainConstants.RELATIONSHIP_ROUTE_TASK, // relationship pattern
                    DomainConstants.TYPE_ROUTE, // object pattern
                    objectSelects, // object selects
                    null, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 1, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    null, // postPattern
                    null, null, null);
            if (mList.size() > 0) {
                Map<String, String> map = (Map<String, String>) mList.get(0);

                String strRouteId = (String) map.get(DomainConstants.SELECT_ID);
                DomainObject domRoute = DomainObject.newInstance(context, strRouteId);
                MapList mEquipmentList = domRoute.getRelatedObjects(context, TigerConstants.RELATIONSHIP_OBJECT_ROUTE, // relationship pattern
                        TigerConstants.TYPE_PSS_EQUIPMENT_REQUEST, // object pattern
                        objectSelects, // object selects
                        null, // relationship selects
                        true, // to direction
                        false, // from direction
                        (short) 1, // recursion level
                        null, null, 0);

                if (mEquipmentList.size() > 0) {
                    Map mEquipmentMap = (Map) mEquipmentList.get(0);
                    String strEquipmentRequestId = (String) mEquipmentMap.get(DomainConstants.SELECT_ID);

                    DomainObject domEquipmentRequest = DomainObject.newInstance(context, strEquipmentRequestId);
                    String strAttrApprovalStatus = domInboxTask.getAttributeValue(context, TigerConstants.ATTRIBUTE_APPROVAL_STATUS);

                    Object strCurrent = domInboxTask.getInfo(context, DomainConstants.SELECT_CURRENT);

                    if ((DomainConstants.STATE_INBOX_TASK_COMPLETE.equals(strCurrent)) && (strAttrApprovalStatus.equals(TigerConstants.APPROVAL_STATUS))) {
                        domEquipmentRequest.setAttributeValue(context, TigerConstants.ATTRIBUTE_BRANCH_TO, TigerConstants.STATE_PSS_EQUIPMENTREQUEST_CANCELLED);

                    }

                    domEquipmentRequest.promote(context);
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in promoteEquipmentRequest: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
        return 0;
    }

    @SuppressWarnings("rawtypes")
    public int addRouteToEquipmentRequest(Context context, String[] args) throws Exception {
        try {

            try {
                if (args == null || args.length < 1) {
                    throw (new IllegalArgumentException());
                }
                String defaultRDOName = EngineeringUtil.getDefaultOrganization(context);
                final String POLICY_PSS_EQUPMENTREQUEST = "policy_PSS_EquipmentRequest";
                String ROUTE_TEMPLATE_NAME = "Validate Equipment Request_" + defaultRDOName;
                String strEquipmentRequestId = args[0];
                DomainObject domEquipment = DomainObject.newInstance(context, strEquipmentRequestId);
                StringList busSelects = new StringList();
                busSelects.add(DomainConstants.SELECT_NAME);
                busSelects.add(DomainConstants.SELECT_ID);
                busSelects.add(DomainConstants.SELECT_REVISION);
                busSelects.add(DomainConstants.SELECT_DESCRIPTION);

                MapList mlRouteTemplates = DomainObject.findObjects(context, DomainConstants.TYPE_ROUTE_TEMPLATE, // Type Pattern
                        ROUTE_TEMPLATE_NAME, // Name Pattern
                        DomainConstants.QUERY_WILDCARD, // Rev Pattern
                        DomainConstants.QUERY_WILDCARD, // Owner Pattern
                        TigerConstants.VAULT_ESERVICEPRODUCTION, // Vault Pattern
                        null, // Where Expression
                        true, // Expand Type
                        busSelects); // Object Pattern

                if (mlRouteTemplates.size() > 0) {

                    Map mRouteTemplate = (Map) mlRouteTemplates.get(0);

                    String strRouteTemplateId = (String) mRouteTemplate.get(DomainConstants.SELECT_ID);

                    String strDescriptionTemplate = (String) mRouteTemplate.get(DomainConstants.SELECT_DESCRIPTION);

                    String strName = DomainObject.getAutoGeneratedName(context, DomainSymbolicConstants.SYMBOLIC_type_Route, "");

                    Route newRoute = new Route();
                    newRoute.createObject(context, DomainConstants.TYPE_ROUTE, strName, TigerConstants.ROUTE_REVISION, DomainConstants.POLICY_ROUTE, context.getVault().getName());
                    String strRouteId = newRoute.getInfo(context, DomainConstants.SELECT_ID);

                    newRoute.setId(strRouteId); // addMembersFromTemplate was setting the route id as route task user id

                    newRoute.setDescription(context, strDescriptionTemplate);

                    // Create new Route using Route Template
                    newRoute.setAttributeValue(context, DomainConstants.ATTRIBUTE_ROUTE_BASE_PURPOSE, TigerConstants.ROUTE_BASE_PURPOSE_APPROVAL);

                    newRoute.connectTemplate(context, strRouteTemplateId);
                    newRoute.addMembersFromTemplate(context, strRouteTemplateId);
                    // Add Relationship
                    DomainRelationship dRel = domEquipment.addRelatedObject(context, new RelationshipType(DomainConstants.RELATIONSHIP_OBJECT_ROUTE), false, strRouteId);

                    try {
                        // Add Attribute to Relationship
                        Map<String, String> mapAttribute = new HashMap<String, String>();
                        mapAttribute.put(DomainConstants.ATTRIBUTE_ROUTE_BASE_POLICY, POLICY_PSS_EQUPMENTREQUEST);
                        // TIGTK-11710:RE:28/11/2017:Start
                        mapAttribute.put(DomainConstants.ATTRIBUTE_ROUTE_BASE_STATE, "state_Create");
                        // TIGTK-11710:RE:28/11/2017:End
                        mapAttribute.put(DomainConstants.ATTRIBUTE_ROUTE_BASE_PURPOSE, TigerConstants.ROUTE_BASE_PURPOSE_APPROVAL);
                        dRel.setAttributeValues(context, mapAttribute);

                    } catch (Exception e) {
                        // TIGTK-5405 - 19-04-2017 - PTE - START
                        logger.error("Error in addRouteToEquipmentRequest: ", e);
                        // TIGTK-5405 - 19-04-2017 - PTE - End

                    }
                }

                return 0;
            } catch (Exception ex) {
                // TIGTK-5405 - 11-04-2017 - VB - START
                logger.error("Error in addRouteToEquipmentRequest: ", ex);
                // TIGTK-5405 - 11-04-2017 - VB - END
                throw ex;
            }

        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in addRouteToEquipmentRequest: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
        return 0;

    }

    public int populateClassificationAttributes(Context context, String[] args) throws Exception {

        try {
            String strObjectId = args[0];
            DomainObject domMBOM = DomainObject.newInstance(context, strObjectId);
            String selectedPDMValue = domMBOM.getAttributeValue(context, TigerConstants.ATTRIBUTE_PDM_CLASS);
            String strPDMValue = EnoviaResourceBundle.getProperty(context, "PSS_FRCMBOMCentral.PSS_PDMClass.SelectedValue." + selectedPDMValue);
            StringList attributeList = FrameworkUtil.split(strPDMValue, "|");
            Map<String, String> attributeMap = new HashMap<String, String>();
            for (int i = 0; i < attributeList.size(); i++) {
                String[] attributeValueList = ((String) attributeList.get(i)).split("~");
                if (attributeValueList != null && attributeValueList.length == 2) {
                    String attrName = attributeValueList[0];
                    String attrValue = attributeValueList[1];
                    if (UIUtil.isNullOrEmpty(attrValue) || attrValue.equals("<BLANK>")) {
                        attrValue = "";
                    }
                    attributeMap.put(attrName, attrValue);
                }
            }
            domMBOM.setAttributeValues(context, attributeMap);
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in populateClassificationAttributes: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }

        return 0;
    }

    /**
     * This mehod is used to set the Information of PSS_HarmonyAssociation Relationship on PSS_Harmonies attribute of Reference and Instance
     * @param context
     * @param args
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void digitalContinuityOnInstance(Context context, String[] args) throws Exception {
        String skipTriggerCheck = PropertyUtil.getRPEValue(context, PROPOGRATEHARMONYASSOCIATION, false);
        if (UIUtil.isNullOrEmpty(skipTriggerCheck)) {

            StringList strHarmonyIdList = new StringList();
            StringList strHarmonyRelIdList = new StringList();
            String strFromRelId = DomainObject.EMPTY_STRING;
            String strFromObjectId = DomainObject.EMPTY_STRING;
            DomainObject domMBOM = null;
            String strHarmonyId = DomainObject.EMPTY_STRING;
            String strHarmonyAssociationId = DomainObject.EMPTY_STRING;
            String strColorPID = DomainObject.EMPTY_STRING;
            String strCustomerPartNumber = DomainObject.EMPTY_STRING;
            String strOldMaterialNumber = DomainObject.EMPTY_STRING;
            String strProductConfigurationPID = DomainObject.EMPTY_STRING;
            String strVariantAssemblyPID = DomainObject.EMPTY_STRING;
            String strQuantity = DomainObject.EMPTY_STRING;
            String strUnitofMeasure = DomainObject.EMPTY_STRING;
            String strUnitofMeasureCategory = DomainObject.EMPTY_STRING;
            String HarmonyAssociationRelId = "HarmonyAssociationRelId:";
            String HarmonyId = ",HarmonyId:";
            // Added For TIGTK-7113:PKH:Phase-2.0:Start
            String strEffectiveRatio = DomainObject.EMPTY_STRING;
            String strReferenceRatio = DomainObject.EMPTY_STRING;
            String strAllowTolerance = DomainObject.EMPTY_STRING;
            String strRatioTolerance = DomainObject.EMPTY_STRING;
            // Added For TIGTK-7113:PKH:Phase-2.0:End

            int flag = 1;
            try {
                MapList mlHarmonyList = new MapList();
                StringBuilder sbDigitalContinuity = new StringBuilder();
                String strRelId = args[2];
                // DomainRelationship domRel = DomainRelationship.newInstance(context, strRelId);
                StringList relSelects = new StringList(DomainRelationship.SELECT_FROM_ID);
                relSelects.add(DomainRelationship.SELECT_FROM_TYPE);
                relSelects.add(DomainRelationship.SELECT_FROM);
                relSelects.add("fromrel.id");
                MapList slConnectedHarmonyList = DomainRelationship.getInfo(context, new String[] { strRelId }, relSelects);

                if (slConnectedHarmonyList != null && !(slConnectedHarmonyList.isEmpty())) {
                    for (int i = 0; i < slConnectedHarmonyList.size(); i++) {
                        Map<String, String> mTempMap = (Map<String, String>) slConnectedHarmonyList.get(i);

                        if (mTempMap.containsKey("from.id[connection]")) {
                            strFromObjectId = (String) mTempMap.get(DomainConstants.SELECT_FROM_ID);
                            strFromObjectId = (String) mTempMap.get("from.id[connection]");
                            domMBOM = DomainObject.newInstance(context, strFromObjectId);
                            strUnitofMeasure = domMBOM.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_UNIT_OF_MEASURE);
                            strUnitofMeasureCategory = domMBOM.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_UNIT_OF_MEASURE_CATEGORY);
                            mlHarmonyList = domMBOM.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_HARMONYASSOCIATION, // relationship pattern
                                    TigerConstants.TYPE_PSS_HARMONY, // object pattern
                                    new StringList(DomainConstants.SELECT_ID), // object selects
                                    new StringList(DomainConstants.SELECT_RELATIONSHIP_ID), // relationship selects
                                    false, // to direction
                                    true, // from direction
                                    (short) 1, // recursion level
                                    null, // object where clause
                                    null, (short) 0, false, // checkHidden
                                    true, // preventDuplicates
                                    (short) 1000, // pageSize
                                    null, // postPattern
                                    null, null, null);
                        } else {
                            StringList slRelSelects = new StringList(TigerConstants.SELECT_RELATIONSHIP_HARMONY_ASSOCIATION);
                            slRelSelects.add(DomainConstants.SELECT_RELATIONSHIP_ID);
                            slRelSelects.add("frommid.id");
                            slRelSelects.add("to.id");
                            strFromRelId = (String) mTempMap.get("fromrel.id");
                            MapList mlTempListforHarmony = DomainRelationship.getInfo(context, new String[] { strFromRelId }, slRelSelects);
                            Map<Object, Object> mTempMapforHarmony = (Map<Object, Object>) mlTempListforHarmony.get(0);
                            String strHarmonyIds = (mTempMapforHarmony.get("frommid[" + TigerConstants.RELATIONSHIP_PSS_HARMONYASSOCIATION + "].to.id")).toString();
                            String strHarmonyRelIds = (mTempMapforHarmony.get("frommid[" + TigerConstants.RELATIONSHIP_PSS_HARMONYASSOCIATION + "].id")).toString();
                            String strToMBOMId = (String) mTempMapforHarmony.get("to.id");
                            DomainObject domToMBOMObject = DomainObject.newInstance(context, strToMBOMId);
                            strUnitofMeasure = domToMBOMObject.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_UNIT_OF_MEASURE);
                            strUnitofMeasureCategory = domToMBOMObject.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_UNIT_OF_MEASURE_CATEGORY);
                            if (!(UIUtil.isNullOrEmpty(strHarmonyIds))) {
                                if (strHarmonyIds.contains("[")) {
                                    strHarmonyIds = strHarmonyIds.substring(strHarmonyIds.indexOf("[") + 1, strHarmonyIds.indexOf("]"));
                                    strHarmonyIdList = FrameworkUtil.split(strHarmonyIds, ",");
                                } else {
                                    strHarmonyIdList.add(strHarmonyIds);
                                }
                                if (strHarmonyRelIds.contains("[")) {
                                    strHarmonyRelIds = strHarmonyRelIds.substring(strHarmonyRelIds.indexOf("[") + 1, strHarmonyRelIds.indexOf("]"));
                                    strHarmonyRelIdList = FrameworkUtil.split(strHarmonyRelIds, ",");
                                } else {
                                    strHarmonyRelIdList.add(strHarmonyRelIds);
                                }
                                for (int k = 0; k < strHarmonyIdList.size(); k++) {
                                    String strTempHarmonyId = (String) strHarmonyIdList.get(k);
                                    String strTempHarmonyRelId = (String) strHarmonyRelIdList.get(k);
                                    Map<String, String> mTempMapforProcess = new HashMap<String, String>();
                                    mTempMapforProcess.put("frommid[" + TigerConstants.RELATIONSHIP_PSS_HARMONYASSOCIATION + "].to.id", strTempHarmonyId);
                                    mTempMapforProcess.put("frommid[" + TigerConstants.RELATIONSHIP_PSS_HARMONYASSOCIATION + "].id", strTempHarmonyRelId);
                                    mlHarmonyList.add(mTempMapforProcess);
                                }
                            }
                        }

                        if (mlHarmonyList != null && !(mlHarmonyList.isEmpty())) {
                            for (int j = 0; j < mlHarmonyList.size(); j++) {
                                Map<String, String> mHarmonyMap = (Map<String, String>) mlHarmonyList.get(j);
                                if (flag != 1) {
                                    sbDigitalContinuity.append("|");
                                }
                                if (mHarmonyMap.containsKey("frommid[" + TigerConstants.RELATIONSHIP_PSS_HARMONYASSOCIATION + "].to.id")) {
                                    strHarmonyId = (String) mHarmonyMap.get("frommid[" + TigerConstants.RELATIONSHIP_PSS_HARMONYASSOCIATION + "].to.id");
                                    strHarmonyAssociationId = ((String) mHarmonyMap.get("frommid[" + TigerConstants.RELATIONSHIP_PSS_HARMONYASSOCIATION + "].id")).trim();
                                    DomainRelationship domRelHarmony = DomainRelationship.newInstance(context, strHarmonyAssociationId);
                                    strColorPID = domRelHarmony.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_COLORPID);
                                    strCustomerPartNumber = domRelHarmony.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CUSTOMERPARTNUMBER);
                                    strOldMaterialNumber = domRelHarmony.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_OLD_MATERIAL_NUMBER);
                                    strProductConfigurationPID = domRelHarmony.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PRODUCT_CONFIGURATION_PID);
                                    strVariantAssemblyPID = domRelHarmony.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_VARIANTASSEMBLY_PID);
                                    strQuantity = domRelHarmony.getAttributeValue(context, TigerConstants.ATTRIBUTE_QUANTITY);
                                    // Added for TIGTK-7113:PKH:Phase-2.0:Start
                                    strEffectiveRatio = domRelHarmony.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGINSTANCEEXTPSS_EFFECTIVERATIO);
                                    StringList slRelSelects = new StringList();
                                    slRelSelects.addElement("fromrel[" + TigerConstants.RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS + "].attribute["
                                            + TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGINSTANCEEXTPSS_REFERENCERATIO + "]");
                                    slRelSelects.addElement("fromrel[" + TigerConstants.RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS + "].attribute["
                                            + TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGINSTANCEEXTPSS_RATIOTOLERANCE + "]");
                                    slRelSelects.addElement("fromrel[" + TigerConstants.RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS + "].attribute["
                                            + TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGINSTANCEEXTPSS_ALLOWTOLERANCE + "]");

                                    Hashtable htRelHarmonyData = domRelHarmony.getRelationshipData(context, slRelSelects);
                                    StringList slPCIReferenceRatio = (StringList) htRelHarmonyData.get("fromrel[" + TigerConstants.RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS + "].attribute["
                                            + TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGINSTANCEEXTPSS_REFERENCERATIO + "]");
                                    StringList slRatioTolerance = (StringList) htRelHarmonyData.get("fromrel[" + TigerConstants.RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS + "].attribute["
                                            + TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGINSTANCEEXTPSS_RATIOTOLERANCE + "]");
                                    StringList slAllowTolerance = (StringList) htRelHarmonyData.get("fromrel[" + TigerConstants.RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS + "].attribute["
                                            + TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGINSTANCEEXTPSS_ALLOWTOLERANCE + "]");

                                    strReferenceRatio = FrameworkUtil.join(slPCIReferenceRatio, ",");
                                    strAllowTolerance = FrameworkUtil.join(slRatioTolerance, ",");
                                    strRatioTolerance = FrameworkUtil.join(slAllowTolerance, ",");
                                    // Added for TIGTK-7113:PKH:Phase-2.0:End
                                } else {
                                    strHarmonyId = (String) mHarmonyMap.get(DomainConstants.SELECT_ID);
                                    strHarmonyAssociationId = (String) mHarmonyMap.get("id[connection]");
                                    DomainRelationship domRelHarmony = DomainRelationship.newInstance(context, strHarmonyAssociationId);
                                    strColorPID = domRelHarmony.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_COLORPID);
                                    strCustomerPartNumber = domRelHarmony.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CUSTOMERPARTNUMBER);
                                    strOldMaterialNumber = domRelHarmony.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_OLD_MATERIAL_NUMBER);
                                    strProductConfigurationPID = domRelHarmony.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PRODUCT_CONFIGURATION_PID);
                                    strVariantAssemblyPID = domRelHarmony.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_VARIANTASSEMBLY_PID);
                                    strQuantity = domRelHarmony.getAttributeValue(context, TigerConstants.ATTRIBUTE_QUANTITY);
                                    // TIGTK-7113 :PKH:Phase-2.0: Starts
                                    StringList slRelSelects = new StringList();
                                    slRelSelects.addElement("fromrel[" + TigerConstants.RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS + "].attribute["
                                            + TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGINSTANCEEXTPSS_REFERENCERATIO + "]");
                                    slRelSelects.addElement("fromrel[" + TigerConstants.RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS + "].attribute["
                                            + TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGINSTANCEEXTPSS_RATIOTOLERANCE + "]");
                                    slRelSelects.addElement("fromrel[" + TigerConstants.RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS + "].attribute["
                                            + TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGINSTANCEEXTPSS_ALLOWTOLERANCE + "]");

                                    Hashtable htRelHarmonyData = domRelHarmony.getRelationshipData(context, slRelSelects);
                                    StringList slPCIReferenceRatio = (StringList) htRelHarmonyData.get("fromrel[" + TigerConstants.RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS + "].attribute["
                                            + TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGINSTANCEEXTPSS_ALLOWTOLERANCE + "]");
                                    StringList slRatioTolerance = (StringList) htRelHarmonyData.get("fromrel[" + TigerConstants.RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS + "].attribute["
                                            + TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGINSTANCEEXTPSS_RATIOTOLERANCE + "]");
                                    StringList slAllowTolerance = (StringList) htRelHarmonyData.get("fromrel[" + TigerConstants.RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS + "].attribute["
                                            + TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGINSTANCEEXTPSS_ALLOWTOLERANCE + "]");

                                    strReferenceRatio = FrameworkUtil.join(slPCIReferenceRatio, ",");
                                    strAllowTolerance = FrameworkUtil.join(slRatioTolerance, ",");
                                    strRatioTolerance = FrameworkUtil.join(slAllowTolerance, ",");
                                    // TIGTK-7113 :PKH:Phase-2.0: End
                                }
                                sbDigitalContinuity.append(HarmonyAssociationRelId);
                                sbDigitalContinuity.append(strHarmonyAssociationId);
                                sbDigitalContinuity.append(HarmonyId);
                                sbDigitalContinuity.append(strHarmonyId);
                                sbDigitalContinuity.append("," + TigerConstants.ATTRIBUTE_PSS_COLORPID);
                                sbDigitalContinuity.append(":" + strColorPID);
                                sbDigitalContinuity.append("," + TigerConstants.ATTRIBUTE_PSS_CUSTOMERPARTNUMBER);
                                sbDigitalContinuity.append(":" + strCustomerPartNumber);
                                sbDigitalContinuity.append("," + TigerConstants.ATTRIBUTE_PSS_OLD_MATERIAL_NUMBER);
                                sbDigitalContinuity.append(":" + strOldMaterialNumber);
                                sbDigitalContinuity.append("," + TigerConstants.ATTRIBUTE_PSS_PRODUCT_CONFIGURATION_PID);
                                sbDigitalContinuity.append(":" + strProductConfigurationPID);
                                sbDigitalContinuity.append("," + TigerConstants.ATTRIBUTE_PSS_VARIANTASSEMBLY_PID);
                                sbDigitalContinuity.append(":" + strVariantAssemblyPID);
                                sbDigitalContinuity.append("," + TigerConstants.ATTRIBUTE_QUANTITY);
                                sbDigitalContinuity.append(":" + strQuantity);
                                sbDigitalContinuity.append("," + TigerConstants.ATTRIBUTE_PSS_UNIT_OF_MEASURE);
                                sbDigitalContinuity.append(":" + strUnitofMeasure);
                                sbDigitalContinuity.append("," + TigerConstants.ATTRIBUTE_PSS_UNIT_OF_MEASURE_CATEGORY);
                                sbDigitalContinuity.append(":" + strUnitofMeasureCategory);
                                // TIGTK-7113 :PKH :Phase-2.0:Starts
                                sbDigitalContinuity.append("," + TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGINSTANCEEXTPSS_EFFECTIVERATIO);
                                sbDigitalContinuity.append(":" + strEffectiveRatio);
                                sbDigitalContinuity.append("," + TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGINSTANCEEXTPSS_REFERENCERATIO);
                                sbDigitalContinuity.append(":" + strReferenceRatio);
                                sbDigitalContinuity.append("," + TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGINSTANCEEXTPSS_RATIOTOLERANCE);
                                sbDigitalContinuity.append(":" + strRatioTolerance);
                                sbDigitalContinuity.append("," + TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGINSTANCEEXTPSS_ALLOWTOLERANCE);
                                sbDigitalContinuity.append(":" + strAllowTolerance);
                                // TIGTK-7113 :PKH :Phase-2.0:End

                                flag = 0;
                            }
                        }
                        if (UIUtil.isNullOrEmpty(strFromRelId)) {
                            domMBOM.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_HARMONIES_REFERENCE, sbDigitalContinuity.toString());
                        } else {

                            DomainRelationship domFromRel = new DomainRelationship(strFromRelId);
                            domFromRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_HARMONIES_INSTANCE, sbDigitalContinuity.toString());

                        }

                    }
                }

            } catch (Exception e) {
                // TIGTK-5405 - 11-04-2017 - VB - START
                logger.error("Error in digitalContinuityOnInstance: ", e);
                // TIGTK-5405 - 11-04-2017 - VB - END
            } finally {
                PropertyUtil.setRPEValue(context, PROPOGRATEHARMONYASSOCIATION, DomainConstants.EMPTY_STRING, false);
            }
        }

    }

    /**
     * method to add interface on MBOM Objects
     * @param context
     * @param args
     * @throws Exception
     */
    public void checkForExtension(Context context, String[] args) throws Exception {

        StringList strInterfaceList = new StringList();
        try {
            String objectId = args[0];
            // PSS ALM2107 fix START
            String skipTriggerCheck = PropertyUtil.getRPEValue(context, "PSS_IS_CALLING_FROM_ENOVIA", false);
            if (!"true".equalsIgnoreCase(skipTriggerCheck)) {
                return;
            }
            // PSS ALM2107 fix END
            DomainObject domMBOM = DomainObject.newInstance(context, objectId);
            String strType = domMBOM.getInfo(context, DomainConstants.SELECT_TYPE);
            BusinessInterfaceList interfaceList = domMBOM.getBusinessInterfaces(context, true);

            if (interfaceList != null && !interfaceList.isEmpty()) {
                for (int i = 0; i < interfaceList.size(); i++) {

                    BusinessInterface bInterface = interfaceList.getElement(i);
                    strInterfaceList.add(bInterface.getName());

                }
            }

            if (strType.equals(TigerConstants.TYPE_CREATEASSEMBLY) || strType.equals(TigerConstants.TYPE_CREATEKIT) || strType.equals(TigerConstants.TYPE_CREATEMATERIAL)
                    || TigerConstants.LIST_TYPE_MATERIALS.contains(strType)) {

                if (interfaceList != null && !interfaceList.isEmpty()) {
                    if (!(strInterfaceList.contains(TigerConstants.INTERFACE_PSS_MANUFACTURING_ITEMEXT))) {
                        MqlUtil.mqlCommand(context, "mod bus " + objectId + " add interface " + TigerConstants.INTERFACE_PSS_MANUFACTURING_ITEMEXT, false, false);
                    }
                    if (!(strInterfaceList.contains(TigerConstants.INTERFACE_PSS_MANUFACTURING_UOMEXT))) {
                        MqlUtil.mqlCommand(context, "mod bus " + objectId + " add interface " + TigerConstants.INTERFACE_PSS_MANUFACTURING_UOMEXT, false, false);
                    }
                    if ((!(strInterfaceList.contains(TigerConstants.INTERFACE_PSS_MANUFACTURING_PART_EXT))) && strType.equalsIgnoreCase(TigerConstants.TYPE_CREATEMATERIAL)) {
                        MqlUtil.mqlCommand(context, "mod bus " + objectId + " add interface " + TigerConstants.INTERFACE_PSS_MANUFACTURING_PART_EXT, false, false);
                    }
                    if ((!(strInterfaceList.contains(TigerConstants.INTERFACE_PSS_PROCESSCONTINUOUSPROVIDE)))
                            && (strType.equalsIgnoreCase(TigerConstants.TYPE_PROCESSCONTINUOUSPROVIDE) || strType.equalsIgnoreCase(TigerConstants.TYPE_PROCESS_CONTINUOUS_CREATE_MATERIAL))) {
                        MqlUtil.mqlCommand(context, "mod bus " + objectId + " add interface " + TigerConstants.INTERFACE_PSS_PROCESSCONTINUOUSPROVIDE, false, false);
                    }

                } else {
                    // TIGTK-9895:Rutuja Ekatpure :for MBOM creation error we removed this code:22/9/2017:start
                    MqlUtil.mqlCommand(context, "mod bus " + objectId + " add interface " + TigerConstants.INTERFACE_PSS_MANUFACTURING_ITEMEXT, false, false);
                    MqlUtil.mqlCommand(context, "mod bus " + objectId + " add interface " + TigerConstants.INTERFACE_PSS_MANUFACTURING_UOMEXT, false, false);
                    // TIGTK-9895:Rutuja Ekatpure :for MBOM creation error we removed this code:22/9/2017:End
                    if (strType.equalsIgnoreCase(TigerConstants.TYPE_CREATEMATERIAL)) {
                        MqlUtil.mqlCommand(context, "mod bus " + objectId + " add interface " + TigerConstants.INTERFACE_PSS_MANUFACTURING_PART_EXT, false, false);
                    }
                    if (TigerConstants.LIST_TYPE_MATERIALS.contains(strType)) {
                        MqlUtil.mqlCommand(context, "mod bus " + objectId + " add interface " + TigerConstants.INTERFACE_PSS_PROCESSCONTINUOUSPROVIDE, false, false);
                    }
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in checkForExtension: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        } finally {
            PropertyUtil.setRPEValue(context, "PSS_IS_CALLING_FROM_ENOVIA", "false", false);
        }
    }

    /**
     * RFC-089
     * @param context
     * @param args
     * @return
     */
    public int notifyGlobalAdminForTaskAvailability(Context context, String args[]) throws Exception {
        try {

            String sObjectId = args[0];
            if (UIUtil.isNotNullAndNotEmpty(sObjectId)) {
                String strObjectType = "";
                String strObjectOrganization = "";
                String strObjectProject = "";
                String strObjectId = "";
                String strObjectName = "";

                DomainObject dObjRoute = DomainObject.newInstance(context, sObjectId);

                StringList busSelects = new StringList();
                busSelects.add(DomainConstants.SELECT_TYPE);
                busSelects.add(DomainConstants.SELECT_ID);
                busSelects.add(DomainConstants.SELECT_ORGANIZATION);
                busSelects.add("project");
                busSelects.add(DomainConstants.SELECT_DESCRIPTION);
                busSelects.add(DomainConstants.SELECT_ORIGINATOR);
                busSelects.add(DomainConstants.SELECT_NAME);

                Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_HARMONY_REQUEST);
                typePattern.addPattern(TigerConstants.TYPE_PSS_EQUIPMENT_REQUEST);

                MapList mList = dObjRoute.getRelatedObjects(context, DomainConstants.RELATIONSHIP_OBJECT_ROUTE, // relationship pattern
                        typePattern.getPattern(), // object pattern
                        busSelects, // object selects
                        null, // relationship selects
                        true, // to direction
                        false, // from direction
                        (short) 1, // recursion level
                        null, // object where clause
                        null, (short) 0, false, // checkHidden
                        true, // preventDuplicates
                        (short) 1000, // pageSize
                        null, // postPattern
                        null, null, null);
                if (mList.size() > 0) {
                    Map mObjectInfo = (Map) mList.get(0);
                    strObjectType = (String) mObjectInfo.get(DomainConstants.SELECT_TYPE);
                    strObjectOrganization = (String) mObjectInfo.get(DomainConstants.SELECT_ORGANIZATION);
                    strObjectProject = (String) mObjectInfo.get("project");
                    strObjectId = (String) mObjectInfo.get(DomainConstants.SELECT_ID);

                    strObjectName = (String) mObjectInfo.get(DomainConstants.SELECT_NAME);
                }

                if (UIUtil.isNotNullAndNotEmpty(strObjectId) && (strObjectType.equals(TigerConstants.TYPE_PSS_HARMONY_REQUEST) || strObjectType.equals(TigerConstants.TYPE_PSS_EQUIPMENT_REQUEST))) {

                    String strRouteTaskUser = dObjRoute.getInfo(context, "from[" + DomainConstants.RELATIONSHIP_ROUTE_NODE + "].attribute[" + DomainConstants.ATTRIBUTE_ROUTE_TASK_USER + "]");

                    if (UIUtil.isNotNullAndNotEmpty(strRouteTaskUser) && strRouteTaskUser.startsWith("role_")) {
                        String taskName = "";
                        String taskObjectId = "";

                        busSelects = new StringList();
                        busSelects.add(DomainConstants.SELECT_NAME);
                        busSelects.add(DomainConstants.SELECT_ID);

                        mList = dObjRoute.getRelatedObjects(context, DomainConstants.RELATIONSHIP_ROUTE_TASK, // relationship pattern
                                DomainConstants.TYPE_INBOX_TASK, // object pattern
                                busSelects, // object selects
                                null, // relationship selects
                                true, // to direction
                                false, // from direction
                                (short) 1, // recursion level
                                null, // object where clause
                                null, (short) 1, false, // checkHidden
                                true, // preventDuplicates
                                (short) 1000, // pageSize
                                null, // postPattern
                                null, null, null);
                        if (mList.size() > 0) {
                            Map mObjectInfo = (Map) mList.get(0);
                            taskObjectId = (String) mObjectInfo.get(DomainConstants.SELECT_ID);
                            taskName = (String) mObjectInfo.get(DomainConstants.SELECT_NAME);
                        }

                        String strUINameOfRouteTaskUser = PropertyUtil.getSchemaProperty(context, strRouteTaskUser);
                        StringBuilder sbCtxOfUser = new StringBuilder("ctx::");
                        sbCtxOfUser.append(strUINameOfRouteTaskUser);
                        sbCtxOfUser.append(".");
                        sbCtxOfUser.append(strObjectOrganization);
                        sbCtxOfUser.append(".");
                        sbCtxOfUser.append(strObjectProject);
                        StringList slPersonsForMailNotification = PersonUtil.getPersonFromRole(context, sbCtxOfUser.toString());

                        String messageTypeStr = "Harmony ";
                        if (strObjectType.equals(TigerConstants.TYPE_PSS_EQUIPMENT_REQUEST)) {
                            messageTypeStr = "Equipment ";
                        }

                        String messageSubject = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(),
                                "PSS_FRCMBOMCentral.Message.Subject.TaskAvailability");
                        String messageBody = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.Message.Body.TaskAvailability");
                        messageSubject = MessageFormat.format(messageSubject, messageTypeStr, strObjectName, taskName);
                        messageBody = MessageFormat.format(messageBody, messageTypeStr);

                        StringList objectIdList = new StringList();
                        objectIdList.add(strObjectId);
                        objectIdList.add(taskObjectId);
                        MailUtil.sendNotification(context, slPersonsForMailNotification, // toList
                                null, // ccList
                                null, // bccList
                                messageSubject, // subjectKey
                                null, // subjectKeys
                                null, // subjectValues
                                messageBody, // messageKey
                                null, // messageKeys
                                null, // messageValues
                                objectIdList, // objectIdList
                                null);
                    }
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in notifyGlobalAdminForTaskAvailability: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
        return 0;
    }

    /**
     * RFC-089
     * @param context
     * @param args
     * @return
     */
    public int changeContentOwner(Context context, String args[]) throws Exception {
        try {

            String sObjectId = args[0];

            if (UIUtil.isNotNullAndNotEmpty(sObjectId)) {
                String strObjectId = "";

                DomainObject dObjTask = DomainObject.newInstance(context, sObjectId);

                StringList busSelects = new StringList();
                busSelects.add(DomainConstants.SELECT_TYPE);
                busSelects.add(DomainConstants.SELECT_ID);
                busSelects.add(DomainConstants.SELECT_ORGANIZATION);
                busSelects.add("project");
                busSelects.add(DomainConstants.SELECT_DESCRIPTION);
                busSelects.add(DomainConstants.SELECT_ORIGINATOR);
                busSelects.add(DomainConstants.SELECT_NAME);

                Pattern relPattern = new Pattern(DomainConstants.RELATIONSHIP_OBJECT_ROUTE);
                relPattern.addPattern(DomainConstants.RELATIONSHIP_ROUTE_TASK);

                Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_HARMONY_REQUEST);
                typePattern.addPattern(TigerConstants.TYPE_PSS_EQUIPMENT_REQUEST);
                typePattern.addPattern(DomainConstants.TYPE_INBOX_TASK);
                typePattern.addPattern(DomainConstants.TYPE_ROUTE);

                Pattern finalPattern = new Pattern(TigerConstants.TYPE_PSS_HARMONY_REQUEST);
                finalPattern.addPattern(TigerConstants.TYPE_PSS_EQUIPMENT_REQUEST);

                MapList mList = dObjTask.getRelatedObjects(context, relPattern.getPattern(), // relationship pattern
                        typePattern.getPattern(), // object pattern
                        busSelects, // object selects
                        null, // relationship selects
                        true, // to direction
                        true, // from direction
                        (short) 2, // recursion level
                        null, // object where clause
                        null, (short) 0, false, // checkHidden
                        true, // preventDuplicates
                        (short) 1000, // pageSize
                        finalPattern, // postPattern
                        null, null, null);

                if (mList.size() > 0) {
                    Map mObjectInfo = (Map) mList.get(0);
                    strObjectId = (String) mObjectInfo.get(DomainConstants.SELECT_ID);
                    DomainObject dObj = DomainObject.newInstance(context, strObjectId);
                    User sOwner = dObjTask.getOwner(context);
                    dObj.setOwner(context, sOwner.getName());
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in changeContentOwner: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
        return 0;
    }

    public static void setInterfaceOnMfgProductionPlanning(Context context, String[] args) throws Exception {
        try {
            String strMfgProductionPlanning = args[0];
            String skipTriggerCheck = PropertyUtil.getRPEValue(context, PLANTFROMENOVIA, false);
            if (!"true".equalsIgnoreCase(skipTriggerCheck)) {
                return;
            }

            if (UIUtil.isNotNullAndNotEmpty(strMfgProductionPlanning)) {
                StringList slInterfaceList = new StringList();
                BusinessObject domObject = new BusinessObject(strMfgProductionPlanning);
                BusinessInterfaceList interfaceList = domObject.getBusinessInterfaces(context, true);
                if (interfaceList != null && !interfaceList.isEmpty()) {
                    for (int i = 0; i < interfaceList.size(); i++) {

                        BusinessInterface bInterface = interfaceList.getElement(i);
                        slInterfaceList.add(bInterface.getName());
                    }
                }
                if (!slInterfaceList.contains("PSS_ManufacturingPlantExt")) {
                    String query = "mod bus " + strMfgProductionPlanning + " add interface PSS_ManufacturingPlantExt";
                    MqlUtil.mqlCommand(context, query, false, false);
                }
            }
        } catch (Exception e) {
            logger.error("Error in setInterfaceOnMfgProductionPlanning: ", e);
            throw e;
        } finally {
            PropertyUtil.setRPEValue(context, PLANTFROMENOVIA, "false", false);
        }
    }

    public static void attachPlantAsConsumer(Context context, String[] args) throws Exception {
        boolean transactionActive = false;
        PLMCoreModelerSession plmSession = null;
        try {

            ContextUtil.startTransaction(context, true);
            transactionActive = true;
            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);

            // PSS ALM4253 fix START
            PropertyUtil.setRPEValue(context, PLANTFROMENOVIA, "true", false);
            // PSS ALM4253 fix END

            String strPlantOwnership = PropertyUtil.getRPEValue(context, PLANTOWNERSHIP, true);
            if (!strPlantOwnership.equalsIgnoreCase("False") || UIUtil.isNullOrEmpty(strPlantOwnership)) {
                String strFromObjectId = args[0];
                String strToObjectId = args[1];
                String strToPhysicalId = DomainConstants.EMPTY_STRING;
                DomainObject domObject = null;
                if (UIUtil.isNotNullAndNotEmpty(strToObjectId)) {
                    strToPhysicalId = MqlUtil.mqlCommand(context, "print bus " + strToObjectId + " select physicalid dump |", false, false);
                    domObject = DomainObject.newInstance(context, strToPhysicalId);

                    List<String> listPlant = FRCMBOMModelerUtility.getPlantsAttachedToMBOMReference(context, plmSession, strToPhysicalId);
                    String strFromMBOMMasterPlant = pss.mbom.MBOMUtil_mxJPO.getMasterPlant(context, strFromObjectId);
                    if (!domObject.isKindOf(context, TigerConstants.TYPE_PSS_OPERATION) && !domObject.isKindOf(context, TigerConstants.TYPE_PSS_LINEDATA)) {
                        if (!listPlant.contains(strFromMBOMMasterPlant)) {
                            FRCMBOMModelerUtility.attachPlantToMBOMReference(context, plmSession, strToPhysicalId, strFromMBOMMasterPlant);

                            String strquery = "query path type SemanticRelation containing " + strToPhysicalId + " where owner.type=='MfgProductionPlanning' select owner.physicalid dump |";
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
                                        String strMBOMPlant = (String) dMfgProductionPlannigObj.getInfo(context, "to[VPLMrel/PLMConnection/V_Owner].from.physicalid");
                                        if (UIUtil.isNotNullAndNotEmpty(strMBOMPlant)) {
                                            if (strMBOMPlant.equalsIgnoreCase(strFromMBOMMasterPlant)) {
                                                // RFC-139 : Update the Master Plant Name on MBOM
                                                PropertyUtil.setGlobalRPEValue(context, MODIFYPLANT, "false");
                                                dMfgProductionPlannigObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGPLANTEXTPSS_OWNERSHIP, "Consumer");
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            flushAndCloseSession(plmSession);
            if (transactionActive)
                ContextUtil.commitTransaction(context);

        } catch (Exception e) {
            logger.error("Error in attachPlantAsConsumer: ", e);
            if (transactionActive)
                ContextUtil.abortTransaction(context);
            throw e;
        }

    }

    /**
     * This method update the Dedicated System attribute after Insert existing from Database.
     * @param context
     * @param args
     * @throws Exception
     * @TIGTK -10214 modified for push context user.
     */
    public static void updateDedicatedSystem(Context context, String[] args) throws Exception {
        boolean isPushcontext = false;

        try {
            String strTosideMBOM = args[0];
            String strNewTypeOfPart = args[1];
            if (strNewTypeOfPart.equalsIgnoreCase("Buy") || strNewTypeOfPart.equalsIgnoreCase("MakeBuy")) {
                DomainObject domToSideMBOM = DomainObject.newInstance(context, strTosideMBOM);
                String strType = (String) domToSideMBOM.getInfo(context, DomainConstants.SELECT_TYPE);
                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                isPushcontext = true;
                if (strType.equalsIgnoreCase(TigerConstants.TYPE_CREATEASSEMBLY))
                    domToSideMBOM.setAttributeValue(context, "CreateAssembly.V_NeedDedicatedSystem", "2");
                else if (strType.equalsIgnoreCase(TigerConstants.TYPE_CREATEMATERIAL))
                    domToSideMBOM.setAttributeValue(context, "CreateMaterial.V_NeedDedicatedSystem", "2");
            }

        } catch (Exception e) {
            logger.error("Error in updateDedicatedSystem: ", e);
            throw e;
        } finally {
            if (isPushcontext)
                ContextUtil.popContext(context);
        }

    }

    /**
     * TIGTK-9512 This method fill the short length description by Description in MBOM when promoted In_work to Review
     * @param context
     * @param args
     * @throws Exception
     */
    public static void fillShortLengthDescription(Context context, String[] args) throws Exception {
        try {
            String strObjectId = args[0];
            DomainObject domMBOM = DomainObject.newInstance(context, strObjectId);
            String strDescriptionValue = domMBOM.getAttributeValue(context, TigerConstants.ATTRIBUTE_PLMENTITY_V_DESCRIPTION);
            String strShortLengthDescription = domMBOM.getAttributeValue(context, TigerConstants.ATTRIBUTE_FAURECIA_SHORT_LENGTH_DESC);
            if (UIUtil.isNullOrEmpty(strShortLengthDescription)) {
                if (domMBOM.isKindOf(context, TigerConstants.TYPE_CREATEASSEMBLY) || domMBOM.isKindOf(context, TigerConstants.TYPE_CREATEMATERIAL)
                        || domMBOM.isKindOf(context, TigerConstants.TYPE_CREATEKIT)) {
                    ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                    if (UIUtil.isNotNullAndNotEmpty(strDescriptionValue)) {
                        if (strDescriptionValue.length() >= 41) {
                            String strNCharacterValue = strDescriptionValue.substring(0, 40);
                            domMBOM.setAttributeValue(context, TigerConstants.ATTRIBUTE_FAURECIA_SHORT_LENGTH_DESC, strNCharacterValue);
                        } else
                            domMBOM.setAttributeValue(context, TigerConstants.ATTRIBUTE_FAURECIA_SHORT_LENGTH_DESC, strDescriptionValue);
                    } else {
                        domMBOM.setAttributeValue(context, TigerConstants.ATTRIBUTE_FAURECIA_SHORT_LENGTH_DESC, DomainConstants.EMPTY_STRING);
                    }
                    ContextUtil.popContext(context);
                }
            }

        } catch (Exception e) {
            logger.error("Error in fillShortLengthDescription: ", e);
            throw e;
        }
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

    // Added for TIGTK-7105:Phase-2.0:PKH:Start
    /**
     * This method Populate attributes
     * @param context
     * @param args
     * @return -- '0'if success...'1' for failure with error message
     * @throws Exception
     */
    public int populatePDMClassificationAttributes(Context context, String[] args) throws Exception {
        try {
            String strObjectId = args[0];
            DomainObject domMBOM = DomainObject.newInstance(context, strObjectId);
            String selectedPDMValue = domMBOM.getAttributeValue(context, "PSS_PDMClass.PSS_PDMClass");
            String strPDMValue = EnoviaResourceBundle.getProperty(context, "PSS_FRCMBOMCentral.PSS_PDMClass.SelectedValue." + selectedPDMValue);
            StringList attributeList = FrameworkUtil.split(strPDMValue, "|");
            Map<String, String> attributeMap = new HashMap<String, String>();
            for (int i = 0; i < attributeList.size(); i++) {
                String[] attributeValueList = ((String) attributeList.get(i)).split("~");
                if (attributeValueList != null && attributeValueList.length == 2) {
                    String attrName = attributeValueList[0];
                    attrName = attrName.replace("PSS_ManufacturingItemExt", "PSS_PDMClass");
                    String attrValue = attributeValueList[1];
                    if (UIUtil.isNullOrEmpty(attrValue) || attrValue.equals("<BLANK>")) {
                        attrValue = "";
                    }
                    attributeMap.put(attrName, attrValue);
                }
            }
            domMBOM.setAttributeValues(context, attributeMap);
        } catch (Exception e) {

            logger.error("Error in populatePDMClassificationAttributes: ", e);

        }

        return 0;
    }

    // Added for TIGTK-7105:Phase-2.0:PKH:End
    // Added for TIGTK-7113:Phase-2.0:PKH:Start
    /**
     * This method digitalContinuityOnPCIInstance
     * @param context
     * @param args
     * @return --
     * @throws Exception
     */
    public void digitalContinuityOnPCIInstance(Context context, String[] args) throws Exception {

        String strAttrName = args[0];
        String strNewAttrValue = args[1];
        String strRelId = args[2];
        String strDigitalContinuityAttrs = args[3];
        String strHarmonyAssociationRelId = "HarmonyAssociationRelId:";
        String strHarmonyId = "HarmonyId:";
        StringList slDigitalContinuityAttrs = FrameworkUtil.split(strDigitalContinuityAttrs, "|");
        try {
            if (UIUtil.isNotNullAndNotEmpty(strAttrName) && slDigitalContinuityAttrs != null && slDigitalContinuityAttrs.contains(strAttrName)) {
                if (UIUtil.isNotNullAndNotEmpty(strRelId)) {
                    DomainRelationship domRelPCI = new DomainRelationship(strRelId);
                    String strAttrPSSHarmonies = domRelPCI.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_HARMONIES_INSTANCE);
                    List attrPSSHarmoniesList = FrameworkUtil.split(strAttrPSSHarmonies, ",", "|");

                    if (attrPSSHarmoniesList != null && attrPSSHarmoniesList.size() > 0) {
                        int slPrevSize = attrPSSHarmoniesList.size();
                        StringList slHarmonyObjectAttrDefinition = new StringList();

                        for (int i = 0; i < slPrevSize; i++) {
                            slHarmonyObjectAttrDefinition = (StringList) attrPSSHarmoniesList.get(i);
                            int slHarmonySize = slHarmonyObjectAttrDefinition.size();
                            String strAttrDefinition = "";
                            boolean bIsNotExistAttr = true;
                            for (int j = 0; j < slHarmonySize; j++) {
                                strAttrDefinition = (String) slHarmonyObjectAttrDefinition.get(j);
                                if (strAttrDefinition.startsWith(strAttrName)) {
                                    slHarmonyObjectAttrDefinition.set(j, strAttrName + ":" + strNewAttrValue);
                                    bIsNotExistAttr = false;
                                }
                            }
                            if (bIsNotExistAttr) {
                                slHarmonyObjectAttrDefinition.addElement(strAttrName + ":" + strNewAttrValue);
                            }
                            slHarmonyObjectAttrDefinition = new StringList();
                        }

                    } else {
                        StringList slRelSelects = new StringList();
                        slRelSelects.addElement("to.to[" + TigerConstants.RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS + "|.id==" + strRelId + "].frommid["
                                + TigerConstants.RELATIONSHIP_PSS_HARMONYASSOCIATION + "].to.id");
                        slRelSelects.addElement(
                                "to.to[" + TigerConstants.RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS + "|.id==" + strRelId + "].frommid[" + TigerConstants.RELATIONSHIP_PSS_HARMONYASSOCIATION + "].id");

                        Hashtable htRelHarmonyData = domRelPCI.getRelationshipData(context, slRelSelects);
                        StringList slRelPSSHarmonyAssociation = (StringList) htRelHarmonyData.get(
                                "to.to[" + TigerConstants.RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS + "|.id==" + strRelId + "].frommid[" + TigerConstants.RELATIONSHIP_PSS_HARMONYASSOCIATION + "].id");
                        StringList slHarmonyObjects = (StringList) htRelHarmonyData.get("to.to[" + TigerConstants.RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS + "|.id==" + strRelId + "].frommid["
                                + TigerConstants.RELATIONSHIP_PSS_HARMONYASSOCIATION + "].to.id");

                        if (slRelPSSHarmonyAssociation != null && !slRelPSSHarmonyAssociation.isEmpty()) {
                            int slHarmonySize = slRelPSSHarmonyAssociation.size();
                            StringList slSubList = new StringList();
                            attrPSSHarmoniesList = new ArrayList<StringList>();
                            for (int i = 0; i < slHarmonySize; i++) {
                                slSubList.addElement(strHarmonyAssociationRelId + ":" + slRelPSSHarmonyAssociation.get(i));
                                slSubList.addElement(strHarmonyId + ":" + slHarmonyObjects.get(i));
                                slSubList.addElement(strAttrName + ":" + strNewAttrValue);
                                attrPSSHarmoniesList.add(slSubList);
                                slSubList = new StringList();
                            }

                        }
                    }

                    domRelPCI.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_HARMONIES_INSTANCE, FrameworkUtil.join(attrPSSHarmoniesList, ",", "|"));
                }
            }
        } catch (Exception e) {

            logger.error("Error in digitalContinuityOnPCIInstance: ", e);

        }

    }
    // Added for TIGTK-7113:Phase-2.0:PKH:End

    /**
     * This method is for updating the Mater Plant name on related MBOM objects RFC-139
     * @param context
     * @param args
     * @throws Exception
     */
    public static void updateMasterPlantOwnerOnMBOM(Context context, String args[]) throws Exception {
        try {
            String strMfgProductionPlanningObjId = args[0];
            String strEvent = args[1];
            String strNewAttributeValue = args[2];
            String strRPEValueId = PropertyUtil.getGlobalRPEValue(context, MODIFYPLANT);
            if (UIUtil.isNullOrEmpty(strRPEValueId) && !strRPEValueId.equalsIgnoreCase("FALSE")) {
                String strQuery = "print bus " + strMfgProductionPlanningObjId + " select paths.path.element[0].physicalid dump |;";
                String strMqlResult = MqlUtil.mqlCommand(context, strQuery, false, false);
                String strSplitArray[] = strMqlResult.split("\\|");
                if (strSplitArray.length > 0) {
                    String strPlantId = strSplitArray[0];
                    String strMBOMObjectId = strSplitArray[1];
                    if (UIUtil.isNotNullAndNotEmpty(strMBOMObjectId)) {
                        DomainObject dMBOMObj = DomainObject.newInstance(context, strMBOMObjectId);

                        if (dMBOMObj.isKindOf(context, TigerConstants.TYPE_CREATEASSEMBLY) || dMBOMObj.isKindOf(context, TigerConstants.TYPE_CREATEMATERIAL)
                                || dMBOMObj.isKindOf(context, TigerConstants.TYPE_CREATEKIT)) {
                            if (strEvent.equalsIgnoreCase("modify")) {
                                if (!strNewAttributeValue.equalsIgnoreCase("Consumer")) {
                                    DomainObject domPlant = DomainObject.newInstance(context, strPlantId);
                                    String strPlantName = domPlant.getInfo(context, DomainConstants.SELECT_NAME);
                                    dMBOMObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXTPSS_MATERPLANTNAME, strPlantName);
                                } else
                                    dMBOMObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXTPSS_MATERPLANTNAME, DomainConstants.EMPTY_STRING);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error in updateMasterPlantOwnerOnMBOM: ", e);
        } finally {
            PropertyUtil.setGlobalRPEValue(context, "MODIFYPLANT", "");
        }
    }

    /**
     * TIGTK-9214 : Update Harmony Information on Revision of MBOM
     * @param context
     * @param args
     * @throws Exception
     */
    public void updateHarmonyAssociationOnRevision(Context context, String[] args) throws Exception {

        try {
            String skipTriggerCheck = PropertyUtil.getRPEValue(context, PROPOGRATEHARMONYASSOCIATION, false);
            if (UIUtil.isNotNullAndNotEmpty(skipTriggerCheck) && skipTriggerCheck.equalsIgnoreCase("True")) {
                String strRelId = args[0];
                String strNewAttributeValue = args[1];
                if (UIUtil.isNotNullAndNotEmpty(strNewAttributeValue) && UIUtil.isNotNullAndNotEmpty(strRelId)) {
                    strRelId = MqlUtil.mqlCommand(context, "print connection " + strRelId + " select physicalid dump |", false, false);
                    String strSplitArray[] = strNewAttributeValue.split("\\|");
                    int slSpiltArrayLength = strSplitArray.length;
                    for (int i = 0; i < slSpiltArrayLength; i++) {
                        String strAttributeValue = strSplitArray[i];
                        String strSplitSecondArray[] = strAttributeValue.split(",");
                        int slSpiltSecondArrayLength = strSplitSecondArray.length;

                        HashMap returnMap = new HashMap();
                        for (int j = 0; j < slSpiltSecondArrayLength - 2; j++) {
                            String strSecondAttributeValue = strSplitSecondArray[j + 1];

                            String strSplitThirdArray[] = strSecondAttributeValue.split(":");
                            int slSpiltThirdArrayLength = strSplitThirdArray.length;
                            String strFinalKey = strSplitThirdArray[0];
                            String strFinalValue = DomainConstants.EMPTY_STRING;
                            if (slSpiltThirdArrayLength > 1) {
                                strFinalValue = strSplitThirdArray[1];
                            }
                            returnMap.put(strFinalKey, strFinalValue);

                        }
                        String associationRelId = MqlUtil.mqlCommand(context, "add connection $1 fromrel $2 to $3 select $4 dump", TigerConstants.RELATIONSHIP_PSS_HARMONYASSOCIATION, strRelId,
                                (String) returnMap.get("HarmonyId"), "id");
                        DomainRelationship domRel = DomainRelationship.newInstance(context, associationRelId);
                        domRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PRODUCT_CONFIGURATION_PID, (String) returnMap.get("PSS_ProductConfigurationPID"));
                        domRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_VARIANTASSEMBLY_PID, (String) returnMap.get("PSS_VariantAssemblyPID"));
                        domRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_OLD_MATERIAL_NUMBER, (String) returnMap.get("PSS_OldMaterialNumber"));
                        domRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CUSTOMERPARTNUMBER, (String) returnMap.get("PSS_CustomerPartNumber"));
                        domRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_COLORPID, (String) returnMap.get("PSS_ColorPID"));
                        domRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_QUANTITY, (String) returnMap.get("Quantity"));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error in updateHarmonyAssociationOnRevision: ", e);
            throw e;
        } finally {
            PropertyUtil.setRPEValue(context, PROPOGRATEHARMONYASSOCIATION, DomainConstants.EMPTY_STRING, false);
        }

    }

    /**
     * This method check the Type of Part Attribute Access to edit or not for particular user
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public boolean isMaterialMakeOrBuyEditable(Context context, String args[]) throws Exception {
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strEditCheck = (String) programMap.get("mode");
            if (UIUtil.isNotNullAndNotEmpty(strEditCheck) && strEditCheck.equalsIgnoreCase("edit")) {
                String strLoggedUser = context.getUser();
                String strLoggedUserSecurityContext = PersonUtil.getDefaultSecurityContext(context, strLoggedUser);
                if (UIUtil.isNotNullAndNotEmpty(strLoggedUserSecurityContext)) {
                    String strLoggerUserRole = (strLoggedUserSecurityContext.split("[.]")[0]);
                    if (strLoggerUserRole.equalsIgnoreCase(TigerConstants.ROLE_PSS_GLOBAL_ADMINISTRATOR) || strLoggerUserRole.equalsIgnoreCase(TigerConstants.ROLE_PSS_PLM_SUPPORT_TEAM)
                            || strLoggerUserRole.equalsIgnoreCase(TigerConstants.ROLE_PSS_RAW_MATERIAL_ENGINEER)) {
                        return true;
                    }
                }
                return false;
            } else {
                return true;
            }
        } catch (Exception e) {
            logger.error("Exception in isMaterialMakeOrBuyEditable", e);
            throw e;
        }
    }

    // KETAKI WAGH -TIGTK-10700 -Start
    public int checkMBOMIsConnectedToMasterPlant(Context context, String args[]) throws Exception {
        int intReturn = 0;
        try {
            String strMfgProductionPlanningObjId = args[0];

            String strNewAttributeValue = args[1];
            String strRPEValueId = PropertyUtil.getGlobalRPEValue(context, MODIFYPLANT);
            if (UIUtil.isNullOrEmpty(strRPEValueId) && !strRPEValueId.equalsIgnoreCase("FALSE")) {
                if (strNewAttributeValue.equalsIgnoreCase("Consumer")) {

                    String strQuery = "print bus " + strMfgProductionPlanningObjId + " select paths.path.element[0].physicalid dump |;";
                    String strMqlResult = MqlUtil.mqlCommand(context, strQuery, false, false);
                    String strSplitArray[] = strMqlResult.split("\\|");
                    if (strSplitArray.length > 0) {

                        String strMBOMObjectId = strSplitArray[1];

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
                }
                if (intReturn == 1) {
                    String strAlertMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSS_EnterpriseChangeMgt.Alert.ModifyOwnership");
                    MqlUtil.mqlCommand(context, "notice $1", strAlertMessage);
                }
            }
        } catch (Exception e) {
            logger.error("Error in updateMasterPlantOwnerOnMBOM: ", e);
            throw e;
        } finally {
            PropertyUtil.setGlobalRPEValue(context, "MODIFYPLANT", "");
        }

        return intReturn;
    }
    // KETAKI WAGH -TIGTK-10700 -END

    /**
     * This method is return the Active CN connected to MCO
     * @param context
     * @param strMBOMObjectId
     * @return
     * @throws Exception
     */
    public MapList getConnectedActiveCN(Context context, String strMBOMObjectId) throws Exception {

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

    /**
     * Method will update the Classification attribute values in Publish EBOM
     * @param context
     * @param args
     * @throws Exception
     */
    public void updatePublishDetails(Context context, String args[]) throws Exception {
        try {
            String psRefPID = args[0];
            String strClassificationFromPart = DomainConstants.EMPTY_STRING;
            DomainObject domPS = DomainObject.newInstance(context, psRefPID);
            String strClassificationFromPS = domPS.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PUBLISHEDPART_PSS_CLASSIFICATIONLIST);
            MapList mlPartObject = pss.mbom.MBOMUtil_mxJPO.getPartFromVPMReference(context, psRefPID);
            if (!mlPartObject.isEmpty()) {
                for (int i = 0; i < mlPartObject.size(); i++) {
                    Map objectMap = (Map) mlPartObject.get(i);
                    String strPartId = (String) objectMap.get(DomainConstants.SELECT_ID);
                    DomainObject domPart = DomainObject.newInstance(context, strPartId);
                    strClassificationFromPart = domPart.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_CLASSIFIEDITEM + "].from.physicalid");

                    // TIGTK-16166 : 24-08-2018 : START
                    String strCADMassUnit = UOMUtil.getInputunit(context, strPartId, TigerConstants.ATTRIBUTE_PSS_EBOM_CADMass);
                    String strCADMassValue = UOMUtil.getInputValue(context, strPartId, TigerConstants.ATTRIBUTE_PSS_EBOM_CADMass);
                    String strCADMassValueWithUnit = strCADMassValue + " " + strCADMassUnit;
                    if (UIUtil.isNotNullAndNotEmpty(strCADMassValueWithUnit)) {
                        domPS.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PUBLISHEDPARTPSS_PP_CADMASS, strCADMassValueWithUnit);
                    }
                    // TIGTK-16166 : 24-08-2018 : END
                }

                if (UIUtil.isNotNullAndNotEmpty(strClassificationFromPS) && strClassificationFromPart.equalsIgnoreCase(strClassificationFromPS))
                    pss.mbom.MBOMUtil_mxJPO.addORUpdateClassification(context, psRefPID, strClassificationFromPS);
            }

        } catch (Exception e) {
            logger.error("Error in updatePublishDetails: ", e);
            throw e;
        }
    }

}
