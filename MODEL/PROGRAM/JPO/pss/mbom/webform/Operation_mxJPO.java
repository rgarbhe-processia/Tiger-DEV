package pss.mbom.webform;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.Pattern;
import matrix.util.StringList;
import pss.constants.TigerConstants;

public class Operation_mxJPO {

    // TIGTK-5405 - 11-04-2017 - VB - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Operation_mxJPO.class);
    // TIGTK-5405 - 11-04-2017 - VB - END

    public Operation_mxJPO() {
        super();
    }

    /**
     * this method is used to autofill the MBOM title on the form,while creating Harmony request object
     * @param context
     * @param args
     * @return List of Harmony Request objects
     * @throws Exception
     *             Exception appears, if error occurred
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public HashMap getVariantNameForMBOM(Context context, String[] args) throws Exception {
        HashMap returnMap = new HashMap();
        try {
            StringList includeList = new StringList();
            includeList.add(DomainConstants.SELECT_ID);
            includeList.add(TigerConstants.SELECT_ATTRIBUTE_PSS_VARIANTID);
            includeList.add(TigerConstants.SELECT_PHYSICALID);
            StringList relSelect = new StringList(DomainConstants.SELECT_RELATIONSHIP_ID);
            StringList strChoicesDisp = new StringList();
            StringList strChoices = new StringList();
            strChoices.add((TigerConstants.ALL));
            strChoicesDisp.add((TigerConstants.ALL));

            Map programMap = JPO.unpackArgs(args);
            Map rMap = (Map) programMap.get("requestMap");
            String strMBOMObjectId = (String) rMap.get("objectId");
            String parentOID = (String) rMap.get("parentOID");
            DomainObject domMbom = null;
            DomainObject domObject = DomainObject.newInstance(context, strMBOMObjectId);
            String strType = domObject.getInfo(context, DomainConstants.SELECT_TYPE);
            if (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_OPERATION) && UIUtil.isNotNullAndNotEmpty(parentOID)) {
                domMbom = DomainObject.newInstance(context, parentOID);
            } else {
                domMbom = DomainObject.newInstance(context, strMBOMObjectId);
            }

            MapList mlVariantAssembly = domMbom.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_PARTVARIANTASSEMBLY, // relationship pattern
                    TigerConstants.TYPE_PSS_MBOM_VARIANT_ASSEMBLY, // object pattern
                    includeList, // object selects
                    relSelect, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 0, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    null, // Postpattern
                    null, null, null);
            if (!mlVariantAssembly.isEmpty()) {
                for (int i = 0; i < mlVariantAssembly.size(); i++) {
                    Map infoMap = (Map) mlVariantAssembly.get(i);
                    strChoices.add((String) infoMap.get(TigerConstants.SELECT_PHYSICALID));
                    strChoicesDisp.add((String) infoMap.get(TigerConstants.SELECT_ATTRIBUTE_PSS_VARIANTID));
                }
            }
            returnMap.put("field_choices", strChoices);
            returnMap.put("field_display_choices", strChoicesDisp);

        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getVariantNameForMBOM: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
        return returnMap;

    }

    /**
     * this method is used to filter the Harmony Request Object for the search
     * @param context
     * @param args
     * @return List of Harmony Request objects
     * @throws Exception
     *             Exception appears, if error occurred
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public HashMap getHarmonyForMBOM(Context context, String[] args) throws Exception {
        HashMap returnMap = new HashMap();
        try {
            StringList objSelect = new StringList();
            objSelect.add(DomainConstants.SELECT_ID);
            objSelect.add(DomainConstants.SELECT_NAME);
            objSelect.add(TigerConstants.SELECT_PHYSICALID);
            objSelect.add(TigerConstants.SELECT_ATTRIBUTE_DISPLAYNAME);
            StringList relSelect = new StringList(DomainConstants.SELECT_RELATIONSHIP_ID);
            StringList slChoicesDisp = new StringList();
            StringList slChoices = new StringList();
            StringList slFinalChoicesDisp = new StringList();
            StringList slFinalChoices = new StringList();

            slChoices.add((TigerConstants.ALL));
            slChoicesDisp.add((TigerConstants.ALL));

            Map programMap = JPO.unpackArgs(args);
            Map rMap = (Map) programMap.get("requestMap");
            String relId = (String) rMap.get("relId");
            String strMBOMObjectId = (String) rMap.get("objectId");
            String parentOID = (String) rMap.get("parentOID");

            if (UIUtil.isNullOrEmpty(relId)) {
                DomainObject domObject = DomainObject.newInstance(context, strMBOMObjectId);
                MapList mlHarmony = domObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_HARMONYASSOCIATION, // relationship pattern
                        TigerConstants.TYPE_PSS_HARMONY, // object pattern
                        objSelect, // object selects
                        relSelect, // relationship selects
                        false, // to direction
                        true, // from direction
                        (short) 0, // recursion level
                        null, // object where clause
                        null, (short) 0, false, // checkHidden
                        true, // preventDuplicates
                        (short) 1000, // pageSize
                        null, // Postpattern
                        null, null, null);

                for (int i = 0; i < mlHarmony.size(); i++) {
                    Map harmonymap = (Map) mlHarmony.get(i);
                    StringBuffer sbUniqueHarmonyIds = new StringBuffer();
                    StringBuffer sbUniqueHarmonyNames = new StringBuffer();
                    String strHarmonyName = (String) harmonymap.get(TigerConstants.SELECT_ATTRIBUTE_DISPLAYNAME);
                    String strHarmonyIds = (String) harmonymap.get(TigerConstants.SELECT_PHYSICALID);
                    String strHarmonyAssocitionId = (String) harmonymap.get(DomainRelationship.SELECT_ID);
                    DomainRelationship domRel = DomainRelationship.newInstance(context, strHarmonyAssocitionId);
                    String strProductValue = domRel.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PRODUCT_CONFIGURATION_PID);
                    String strColorId = domRel.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_COLORPID);

                    if (UIUtil.isNotNullAndNotEmpty(strProductValue) && UIUtil.isNotNullAndNotEmpty(strColorId) && !strColorId.equalsIgnoreCase("Ignore") && !strColorId.equalsIgnoreCase("N/A")) {
                        DomainObject domProductConfig = DomainObject.newInstance(context, strProductValue);
                        String strProductConfigName = domProductConfig.getAttributeValue(context, TigerConstants.ATTRIBUTE_MARKETINGNAME);

                        sbUniqueHarmonyIds.append(strHarmonyIds);
                        sbUniqueHarmonyIds.append("|");
                        sbUniqueHarmonyIds.append(strProductValue);
                        sbUniqueHarmonyNames.append(strHarmonyName);
                        sbUniqueHarmonyNames.append("(");
                        sbUniqueHarmonyNames.append(strProductConfigName);
                        sbUniqueHarmonyNames.append(")");

                        slChoices.add(sbUniqueHarmonyIds.toString());
                        slChoicesDisp.add(sbUniqueHarmonyNames.toString());

                    } else if (UIUtil.isNotNullAndNotEmpty(strColorId) && !strColorId.equalsIgnoreCase("Ignore") && !strColorId.equalsIgnoreCase("N/A")) {
                        sbUniqueHarmonyIds.append(strHarmonyIds);
                        sbUniqueHarmonyNames.append(strHarmonyName);

                        slChoices.add(sbUniqueHarmonyIds.toString());
                        slChoicesDisp.add(sbUniqueHarmonyNames.toString());
                    }
                }

            } else {
                String strHarmonyName = "";
                String strHarmonyPhysicalId = "";
                String strHarmonyId = MqlUtil.mqlCommand(context, "print connection $1 select $2 dump", relId, "frommid.to.id");
                String strAssociationId = MqlUtil.mqlCommand(context, "print connection $1 select $2 dump", relId, "frommid.id");
                StringList slectedIds = FrameworkUtil.split(strAssociationId, ",");
                StringList slObjectIds = FrameworkUtil.split(strHarmonyId, ",");

                for (int i = 0, j = 0; i < slectedIds.size() && j < slObjectIds.size(); j++, i++) {

                    String strHarmonyIds = (String) slObjectIds.get(j);
                    DomainObject dom = DomainObject.newInstance(context, strHarmonyIds);
                    strHarmonyPhysicalId = dom.getInfo(context, TigerConstants.SELECT_PHYSICALID);
                    strHarmonyName = dom.getInfo(context, TigerConstants.SELECT_ATTRIBUTE_DISPLAYNAME);
                    String strHarmonyAssocitionId = (String) slectedIds.get(i);
                    StringBuffer sbUniqueHarmonyIds = new StringBuffer();
                    StringBuffer sbUniqueHarmonyNames = new StringBuffer();
                    DomainRelationship domRel = DomainRelationship.newInstance(context, strHarmonyAssocitionId);
                    String strProductValue = domRel.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PRODUCT_CONFIGURATION_PID);
                    String strColorId = domRel.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_COLORPID);

                    if (UIUtil.isNotNullAndNotEmpty(strProductValue) && UIUtil.isNotNullAndNotEmpty(strColorId) && !strColorId.equalsIgnoreCase("Ignore") && !strColorId.equalsIgnoreCase("N/A")) {
                        DomainObject domProductConfig = DomainObject.newInstance(context, strProductValue);
                        String strProductConfigName = domProductConfig.getAttributeValue(context, TigerConstants.ATTRIBUTE_MARKETINGNAME);

                        sbUniqueHarmonyIds.append(strHarmonyPhysicalId);
                        sbUniqueHarmonyIds.append("|");
                        sbUniqueHarmonyIds.append(strProductValue);
                        sbUniqueHarmonyNames.append(strHarmonyName);
                        sbUniqueHarmonyNames.append("(");

                        sbUniqueHarmonyNames.append(strProductConfigName);
                        sbUniqueHarmonyNames.append(")");

                        slChoices.add(sbUniqueHarmonyIds.toString());
                        slChoicesDisp.add(sbUniqueHarmonyNames.toString());

                    } else if (UIUtil.isNotNullAndNotEmpty(strColorId) && !strColorId.equalsIgnoreCase("Ignore") && !strColorId.equalsIgnoreCase("N/A")) {
                        sbUniqueHarmonyIds.append(strHarmonyPhysicalId);
                        sbUniqueHarmonyNames.append(strHarmonyName);

                        slChoices.add(sbUniqueHarmonyIds.toString());
                        slChoicesDisp.add(sbUniqueHarmonyNames.toString());
                    }
                }

            }

            StringList slInitialHarmonies = getRelatedHarmonyData(context, parentOID);
            String strCheckHarmony = DomainObject.EMPTY_STRING;
            if (!slInitialHarmonies.isEmpty()) {
                for (int i = 0; i < slChoices.size(); i++) {
                    String strGetChoices = (String) slChoices.get(i);
                    if (strGetChoices.contains("|")) {
                        String[] strGetChoicesArray = strGetChoices.split("\\|");
                        strCheckHarmony = strGetChoicesArray[0];

                    } else {
                        strCheckHarmony = strGetChoices;
                    }

                    if (slInitialHarmonies.contains(strCheckHarmony)) {

                        if (!slFinalChoices.contains(strGetChoices)) {
                            slFinalChoices.add(strGetChoices);
                            slFinalChoicesDisp.add(slChoicesDisp.get(i));
                        }
                    }
                }
            }

            returnMap.put("field_choices", slFinalChoices);
            returnMap.put("field_display_choices", slFinalChoicesDisp);

        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getHarmonyForMBOM: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
        return returnMap;

    }

    @SuppressWarnings("rawtypes")
    public StringList getRelatedHarmonyData(Context context, String ObjectId) throws Exception {
        try {
            StringList slInitialHarmonies = new StringList();
            slInitialHarmonies.add((TigerConstants.ALL));
            if (UIUtil.isNotNullAndNotEmpty(ObjectId)) {
                DomainObject domObject = DomainObject.newInstance(context, ObjectId);
                StringList objSelect = new StringList();
                objSelect.add(DomainConstants.SELECT_ID);
                objSelect.add(DomainConstants.SELECT_NAME);
                objSelect.add(TigerConstants.SELECT_PHYSICALID);
                StringList relSelect = new StringList(DomainConstants.SELECT_RELATIONSHIP_ID);
                MapList mlHarmonylist = domObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSSMBOM_HARMONIES, // relationship pattern
                        TigerConstants.TYPE_PSS_HARMONY, // object pattern
                        objSelect, // object selects
                        relSelect, // relationship selects
                        false, // to direction
                        true, // from direction
                        (short) 0, // recursion level
                        null, // object where clause
                        null, (short) 0, false, // checkHidden
                        true, // preventDuplicates
                        (short) 1000, // pageSize
                        null, // Postpattern
                        null, null, null);
                if (!mlHarmonylist.isEmpty()) {
                    for (int i = 0; i < mlHarmonylist.size(); i++) {
                        Map Harmonymap = (Map) mlHarmonylist.get(i);
                        slInitialHarmonies.add((String) Harmonymap.get(TigerConstants.SELECT_PHYSICALID));
                        // slInitialHarmonies.add((String) Harmonymap.get(DomainConstants.SELECT_NAME));
                    }
                }
            }

            return slInitialHarmonies;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getRelatedHarmonyData: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public HashMap getCustomerPartNumberForMBOM(Context context, String[] args) throws Exception {
        HashMap returnMap = new HashMap();
        try {
            StringList strChoicesDisp = new StringList();
            StringList strChoices = new StringList();
            strChoices.add((TigerConstants.ALL));
            strChoicesDisp.add((TigerConstants.ALL));

            Map programMap = JPO.unpackArgs(args);

            Map rMap = (Map) programMap.get("requestMap");
            String strMBOMObjectId = (String) rMap.get("objectId");
            String parentOID = (String) rMap.get("parentOID");
            String relId = (String) rMap.get("relId");
            String rootId = (String) rMap.get("rootId");

            DomainObject domObject = DomainObject.newInstance(context, strMBOMObjectId);
            String strHarmonyFinalId = DomainObject.EMPTY_STRING;
            String strSelectedProductId = DomainObject.EMPTY_STRING;
            StringList slIntermediateList = new StringList();
            String strType = domObject.getInfo(context, DomainConstants.SELECT_TYPE);
            if (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_OPERATION) && UIUtil.isNotNullAndNotEmpty(parentOID)) {

                domObject = DomainObject.newInstance(context, strMBOMObjectId);
                String harmonyPID = domObject.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_OPERATION_PSS_HARMONY);

                if (harmonyPID.contains("|")) {
                    String[] strGetChoicesArray = harmonyPID.split("\\|");
                    strHarmonyFinalId = strGetChoicesArray[0];
                    strSelectedProductId = strGetChoicesArray[1];
                } else {
                    strHarmonyFinalId = harmonyPID;
                }

                if (UIUtil.isNotNullAndNotEmpty(strHarmonyFinalId) && !strHarmonyFinalId.equalsIgnoreCase("All")) {
                    slIntermediateList.add(strHarmonyFinalId);
                } else {
                    slIntermediateList = getRelatedHarmonyData(context, rootId);
                }

                StringList associationIdList = new StringList();
                StringList intermediateAssociationIdList = new StringList();
                if (UIUtil.isNullOrEmpty(relId)) {
                    if (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_OPERATION) && UIUtil.isNotNullAndNotEmpty(parentOID)) {
                        domObject = DomainObject.newInstance(context, parentOID);
                    }
                    StringList slObjSelectStmts = new StringList(1);
                    slObjSelectStmts.addElement(DomainConstants.SELECT_ID);
                    StringList slSelectRelStmts = new StringList(1);
                    slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
                    slSelectRelStmts.add(TigerConstants.SELECT_ATTRIBUTE_PSS_COLOR_PID);

                    Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_HARMONYASSOCIATION);
                    Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_HARMONY);

                    MapList mlRelatedHarmony = domObject.getRelatedObjects(context, relationshipPattern.getPattern(), typePattern.getPattern(), // object pattern
                            slObjSelectStmts, // object selects
                            slSelectRelStmts, // relationship selects
                            false, // to direction
                            true, // from direction
                            (short) 0, // recursion level
                            null, // object where clause
                            null, // relationship where clause
                            (short) 0, false, // checkHidden
                            true, // preventDuplicates
                            (short) 1000, // pageSize
                            null, null, null, null);
                    Iterator itrReletedHarmony = mlRelatedHarmony.iterator();
                    while (itrReletedHarmony.hasNext()) {
                        Map mHermonyMap = (Map) itrReletedHarmony.next();
                        intermediateAssociationIdList.add((String) mHermonyMap.get(DomainConstants.SELECT_RELATIONSHIP_ID));
                    }
                    StringList slList = (StringList) domObject.getInfoList(context, "from[PSS_MBOMHarmonies].to.physicalid");
                    if (!intermediateAssociationIdList.isEmpty()) {
                        for (int i = 0; i < intermediateAssociationIdList.size(); i++) {
                            String strHarmonyRelIDs = (String) intermediateAssociationIdList.get(i);
                            if (UIUtil.isNotNullAndNotEmpty(strHarmonyRelIDs)) {
                                String strHarmonyIDs = MqlUtil.mqlCommand(context, "print connection $1 select $2 dump", strHarmonyRelIDs, "to.physicalid");
                                if (slList.contains(strHarmonyIDs)) {
                                    associationIdList.add(strHarmonyRelIDs);
                                }
                            }
                        }

                    }

                } else {
                    String strGetRelId = DomainObject.EMPTY_STRING;
                    if (!strHarmonyFinalId.equalsIgnoreCase("All") && !strHarmonyFinalId.isEmpty()) {
                        strGetRelId = MqlUtil.mqlCommand(context, "print connection $1 select $2 dump", relId,
                                "frommid[" + TigerConstants.RELATIONSHIP_PSS_HARMONYASSOCIATION + "|to.physicalid=='" + strHarmonyFinalId + "'].id");
                    } else {
                        strGetRelId = MqlUtil.mqlCommand(context, "print connection $1 select $2 dump", relId, "frommid.id");
                    }
                    StringList slectedIds = FrameworkUtil.split(strGetRelId, ",");
                    for (int i = 0; i < slectedIds.size(); i++) {
                        associationIdList.add((String) slectedIds.get(i));
                    }
                }

                int slSize = associationIdList.size();
                if (!slIntermediateList.isEmpty() && !slIntermediateList.contains("All")) {
                    for (int j = 0; j < slSize; j++) {
                        String associationId = (String) associationIdList.get(j);
                        if (UIUtil.isNotNullAndNotEmpty(associationId)) {
                            DomainRelationship domRel = new DomainRelationship(associationId);
                            String strPCId = domRel.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PRODUCT_CONFIGURATION_PID);
                            String strGetHarmonyPID = MqlUtil.mqlCommand(context, "print connection $1 select $2 dump", associationId, "to.physicalid");

                            // selected product with harmony
                            if (UIUtil.isNotNullAndNotEmpty(strPCId) && UIUtil.isNotNullAndNotEmpty(strSelectedProductId)) {
                                if (strPCId.equalsIgnoreCase(strSelectedProductId)) {
                                    if (slIntermediateList.contains(strGetHarmonyPID)) {
                                        String strCustomerPartNumber = domRel.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CUSTOMERPARTNUMBER);
                                        if (UIUtil.isNotNullAndNotEmpty(strCustomerPartNumber)) {
                                            strChoices.add(strCustomerPartNumber);
                                            strChoicesDisp.add(strCustomerPartNumber);
                                            strChoices.remove("All");
                                            strChoicesDisp.remove("All");
                                        }
                                    }
                                }
                            }
                            // Seelcted only Harmony
                            else if (UIUtil.isNullOrEmpty(strSelectedProductId) && UIUtil.isNullOrEmpty(strPCId)) {
                                if (strPCId.equalsIgnoreCase(strSelectedProductId)) {
                                    if (slIntermediateList.contains(strGetHarmonyPID)) {
                                        String strCustomerPartNumber = domRel.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CUSTOMERPARTNUMBER);
                                        if (UIUtil.isNotNullAndNotEmpty(strCustomerPartNumber)) {
                                            strChoices.add(strCustomerPartNumber);
                                            strChoicesDisp.add(strCustomerPartNumber);
                                            strChoices.remove("All");
                                            strChoicesDisp.remove("All");
                                        }
                                    }
                                }
                            }

                        }
                    }
                }
                // Selected All
                else {
                    DomainObject domParentMBOM = DomainObject.newInstance(context, parentOID);
                    StringList slList = domParentMBOM.getInfoList(context, "attribute[PSS_ManufacturingItemExt.PSS_CustomerPartNumber].value");
                    if (!slList.isEmpty()) {
                        for (int i = 0; i < slList.size(); i++) {
                            String strCustoNumber = (String) slList.get(i);
                            if (UIUtil.isNotNullAndNotEmpty(strCustoNumber)) {
                                strChoices.add(strCustoNumber);
                                strChoicesDisp.add(strCustoNumber);
                            }
                        }
                    }

                }

            } else {

                StringList slList = domObject.getInfoList(context, "attribute[PSS_ManufacturingItemExt.PSS_CustomerPartNumber].value");
                if (!slList.isEmpty()) {
                    for (int i = 0; i < slList.size(); i++) {
                        String strCustoNumber = (String) slList.get(i);
                        if (UIUtil.isNotNullAndNotEmpty(strCustoNumber)) {
                            strChoices.add(strCustoNumber);
                            strChoicesDisp.add(strCustoNumber);
                        }
                    }
                }
            }

            returnMap.put("field_choices", strChoices);
            returnMap.put("field_display_choices", strChoicesDisp);
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getCustomerPartNumberForMBOM: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
        return returnMap;

    }

    @SuppressWarnings("rawtypes")
    public String getTitleForMBOMOperation(Context context, String[] args) throws Exception {
        String title = DomainObject.EMPTY_STRING;

        try {
            Map programMap = JPO.unpackArgs(args);
            Map rMap = (Map) programMap.get("requestMap");
            String strOperationId = (String) rMap.get("objectId");
            StringList select = new StringList(2);
            select.add(TigerConstants.SELECT_ATTRIBUTE_PSS_OPERATION_NUMBER);
            select.add(TigerConstants.SELECT_ATTRIBUTE_PSS_OPERATION_HARMONY);

            if (UIUtil.isNotNullAndNotEmpty(strOperationId)) {
                DomainObject domOperation = DomainObject.newInstance(context, strOperationId);
                Map slCust = domOperation.getInfo(context, select);

                if (((String) slCust.get(TigerConstants.SELECT_ATTRIBUTE_PSS_OPERATION_HARMONY)).equalsIgnoreCase(TigerConstants.ALL)) {
                    title = (String) slCust.get(TigerConstants.SELECT_ATTRIBUTE_PSS_OPERATION_NUMBER);
                } else {
                    title = (String) slCust.get(TigerConstants.SELECT_ATTRIBUTE_PSS_OPERATION_NUMBER) + (String) slCust.get(TigerConstants.SELECT_ATTRIBUTE_PSS_OPERATION_HARMONY);
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getTitleForMBOMOperation: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
        return title;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public HashMap getColorsForMBOM(Context context, String[] args) throws Exception {
        HashMap returnMap = new HashMap();
        try {
            StringList strChoicesDisp = new StringList();
            StringList strChoices = new StringList();
            strChoices.add((TigerConstants.ALL));
            strChoicesDisp.add((TigerConstants.ALL));

            Map programMap = JPO.unpackArgs(args);
            Map rMap = (Map) programMap.get("requestMap");
            String relId = (String) rMap.get("relId");
            String strMBOMObjectId = (String) rMap.get("objectId");
            String parentOID = (String) rMap.get("parentOID");
            String rootId = (String) rMap.get("rootId");
            if (UIUtil.isNullOrEmpty(rootId)) {
                rootId = parentOID;
            }
            DomainObject domObject = DomainObject.newInstance(context, strMBOMObjectId);
            String strType = domObject.getInfo(context, DomainConstants.SELECT_TYPE);
            StringList colorList = new StringList();
            StringList slIntermediateList = new StringList();
            String strHarmonyFinalId = DomainObject.EMPTY_STRING;
            String strSelectedProductId = DomainObject.EMPTY_STRING;
            if (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_OPERATION)) {
                domObject = DomainObject.newInstance(context, strMBOMObjectId);
                String harmonyPID = domObject.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_OPERATION_PSS_HARMONY);

                if (harmonyPID.contains("|")) {
                    String[] strGetChoicesArray = harmonyPID.split("\\|");
                    strHarmonyFinalId = strGetChoicesArray[0];
                    strSelectedProductId = strGetChoicesArray[1];
                } else {
                    strHarmonyFinalId = harmonyPID;
                }

                if (UIUtil.isNotNullAndNotEmpty(strHarmonyFinalId) && !strHarmonyFinalId.equalsIgnoreCase("All")) {
                    slIntermediateList.add(strHarmonyFinalId);
                } else {
                    slIntermediateList = getRelatedHarmonyData(context, rootId);
                }

                StringList associationIdList = new StringList();
                StringList intermediateAssociationIdList = new StringList();

                if (UIUtil.isNullOrEmpty(relId)) {
                    if (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_OPERATION) && UIUtil.isNotNullAndNotEmpty(parentOID)) {
                        domObject = DomainObject.newInstance(context, parentOID);
                    }
                    StringList slObjSelectStmts = new StringList(1);
                    slObjSelectStmts.addElement(DomainConstants.SELECT_ID);
                    StringList slSelectRelStmts = new StringList(1);
                    slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
                    slSelectRelStmts.add(TigerConstants.SELECT_ATTRIBUTE_PSS_COLOR_PID);

                    Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_HARMONYASSOCIATION);
                    Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_HARMONY);

                    MapList mlRelatedHarmony = domObject.getRelatedObjects(context, relationshipPattern.getPattern(), typePattern.getPattern(), // object pattern
                            slObjSelectStmts, // object selects
                            slSelectRelStmts, // relationship selects
                            false, // to direction
                            true, // from direction
                            (short) 0, // recursion level
                            null, // object where clause
                            null, // relationship where clause
                            (short) 0, false, // checkHidden
                            true, // preventDuplicates
                            (short) 1000, // pageSize
                            null, null, null, null);
                    Iterator itrReletedHarmony = mlRelatedHarmony.iterator();
                    while (itrReletedHarmony.hasNext()) {
                        Map mHermonyMap = (Map) itrReletedHarmony.next();
                        intermediateAssociationIdList.add((String) mHermonyMap.get(DomainConstants.SELECT_RELATIONSHIP_ID));
                    }
                    StringList slList = (StringList) domObject.getInfoList(context, "from[PSS_MBOMHarmonies].to.physicalid");
                    if (!intermediateAssociationIdList.isEmpty()) {
                        for (int i = 0; i < intermediateAssociationIdList.size(); i++) {
                            String strHarmonyRelIDs = (String) intermediateAssociationIdList.get(i);
                            if (UIUtil.isNotNullAndNotEmpty(strHarmonyRelIDs)) {
                                String strHarmonyIDs = MqlUtil.mqlCommand(context, "print connection $1 select $2 dump", strHarmonyRelIDs, "to.physicalid");
                                if (slList.contains(strHarmonyIDs)) {
                                    associationIdList.add(strHarmonyRelIDs);
                                }
                            }
                        }

                    }

                } else {
                    String strGetRelId = DomainObject.EMPTY_STRING;
                    if (!strHarmonyFinalId.equalsIgnoreCase("All") && !strHarmonyFinalId.isEmpty()) {
                        strGetRelId = MqlUtil.mqlCommand(context, "print connection $1 select $2 dump", relId,
                                "frommid[" + TigerConstants.RELATIONSHIP_PSS_HARMONYASSOCIATION + "|to.physicalid=='" + strHarmonyFinalId + "'].id");
                    } else {
                        strGetRelId = MqlUtil.mqlCommand(context, "print connection $1 select $2 dump", relId, "frommid.id");
                    }
                    StringList slectedIds = FrameworkUtil.split(strGetRelId, ",");
                    for (int i = 0; i < slectedIds.size(); i++) {
                        associationIdList.add((String) slectedIds.get(i));
                    }
                }
                int slSize = associationIdList.size();

                if (!slIntermediateList.isEmpty() && !slIntermediateList.contains("All")) {
                    for (int j = 0; j < slSize; j++) {
                        String associationId = (String) associationIdList.get(j);
                        if (UIUtil.isNotNullAndNotEmpty(associationId)) {
                            DomainRelationship domRel = new DomainRelationship(associationId);
                            String strPCId = domRel.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PRODUCT_CONFIGURATION_PID);
                            String strGetHarmonyPID = MqlUtil.mqlCommand(context, "print connection $1 select $2 dump", associationId, "to.physicalid");

                            // selected product with harmony
                            if (UIUtil.isNotNullAndNotEmpty(strPCId) && UIUtil.isNotNullAndNotEmpty(strSelectedProductId)) {
                                if (strPCId.equalsIgnoreCase(strSelectedProductId)) {
                                    if (slIntermediateList.contains(strGetHarmonyPID)) {
                                        String strColorPID = domRel.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_COLORPID);
                                        if (UIUtil.isNotNullAndNotEmpty(strColorPID) && !colorList.contains(strColorPID) && !strColorPID.equals("Ignore") && !strColorPID.equals("N/A")) {
                                            colorList.add(strColorPID);
                                            strChoices.add(strColorPID);
                                            DomainObject domColorOption = DomainObject.newInstance(context, strColorPID);
                                            String strColorCode = domColorOption.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_COLORCODE);
                                            String strDisplayName = domColorOption.getAttributeValue(context, TigerConstants.ATTRIBUTE_DISPLAYNAME);
                                            StringBuilder colorDisplayText = new StringBuilder(strColorCode);
                                            colorDisplayText.append("(");
                                            colorDisplayText.append(strDisplayName);
                                            colorDisplayText.append(")");
                                            strChoicesDisp.add(colorDisplayText.toString());
                                            strChoices.remove("All");
                                            strChoicesDisp.remove("All");
                                        }
                                    }
                                }
                            }
                            // Seelcted only Harmony
                            else if (UIUtil.isNullOrEmpty(strSelectedProductId) && UIUtil.isNullOrEmpty(strPCId)) {
                                if (strPCId.equalsIgnoreCase(strSelectedProductId)) {
                                    if (slIntermediateList.contains(strGetHarmonyPID)) {
                                        String strColorPID = domRel.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_COLORPID);
                                        if (UIUtil.isNotNullAndNotEmpty(strColorPID) && !colorList.contains(strColorPID) && !strColorPID.equals("Ignore") && !strColorPID.equals("N/A")) {
                                            colorList.add(strColorPID);
                                            strChoices.add(strColorPID);
                                            DomainObject domColorOption = DomainObject.newInstance(context, strColorPID);
                                            String strColorCode = domColorOption.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_COLORCODE);
                                            String strDisplayName = domColorOption.getAttributeValue(context, TigerConstants.ATTRIBUTE_DISPLAYNAME);
                                            StringBuilder colorDisplayText = new StringBuilder(strColorCode);
                                            colorDisplayText.append("(");
                                            colorDisplayText.append(strDisplayName);
                                            colorDisplayText.append(")");
                                            strChoicesDisp.add(colorDisplayText.toString());
                                            strChoices.remove("All");
                                            strChoicesDisp.remove("All");
                                        }
                                    }
                                }

                            }

                        }
                    }
                }
                // Selected All
                else {
                    if (UIUtil.isNotNullAndNotEmpty(parentOID)) {
                        DomainObject domParentMBOM = DomainObject.newInstance(context, parentOID);
                        StringList slList = domParentMBOM.getInfoList(context, "from[PSS_ColorList].to.physicalid");
                        if (!slList.isEmpty()) {
                            for (int i = 0; i < slList.size(); i++) {
                                String strColorId = (String) slList.get(i);
                                DomainObject domColorOption = DomainObject.newInstance(context, strColorId);
                                strChoices.add(strColorId);
                                String strColorCode = domColorOption.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_COLORCODE);
                                String strDisplayName = domColorOption.getAttributeValue(context, TigerConstants.ATTRIBUTE_DISPLAYNAME);
                                StringBuilder colorDisplayText = new StringBuilder(strColorCode);
                                colorDisplayText.append("(");
                                colorDisplayText.append(strDisplayName);
                                colorDisplayText.append(")");
                                strChoicesDisp.add(colorDisplayText.toString());
                            }
                        }
                    }
                }
            } else {
                StringList slList = domObject.getInfoList(context, "from[PSS_ColorList].to.physicalid");
                if (!slList.isEmpty()) {
                    for (int i = 0; i < slList.size(); i++) {
                        String strColorId = (String) slList.get(i);
                        DomainObject domColorOption = DomainObject.newInstance(context, strColorId);
                        strChoices.add(strColorId);
                        String strColorCode = domColorOption.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_COLORCODE);
                        String strDisplayName = domColorOption.getAttributeValue(context, TigerConstants.ATTRIBUTE_DISPLAYNAME);
                        StringBuilder colorDisplayText = new StringBuilder(strColorCode);
                        colorDisplayText.append("(");
                        colorDisplayText.append(strDisplayName);
                        colorDisplayText.append(")");
                        strChoicesDisp.add(colorDisplayText.toString());
                    }
                }
            }

            returnMap.put("field_choices", strChoices);
            returnMap.put("field_display_choices", strChoicesDisp);
        } catch (

        Exception e)

        {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getColorsForMBOM: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
        return returnMap;

    }

    public void setOperationLineDataTitle(Context context, String[] args) throws Exception {
        try {
            String ObjectId = args[0];
            DomainObject domObject = DomainObject.newInstance(context, ObjectId);
            String strType = (String) domObject.getInfo(context, DomainConstants.SELECT_TYPE);
            if (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_OPERATION)) {
                String strOperationNumber = domObject.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_OPERATION_NUMBER);
                String strHarmony = domObject.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_OPERATION_PSS_HARMONY);
                if (strHarmony.equalsIgnoreCase(TigerConstants.ALL)) {
                    domObject.setAttributeValue(context, TigerConstants.ATTRIBUTE_V_NAME, strOperationNumber);
                } else {
                    String strHarmonyFinalId = DomainConstants.EMPTY_STRING;
                    String strSelectedProductId = DomainConstants.EMPTY_STRING;
                    if (UIUtil.isNotNullAndNotEmpty(strHarmony)) {
                        if (strHarmony.contains("|")) {
                            String[] strGetChoicesArray = strHarmony.split("\\|");
                            strHarmonyFinalId = strGetChoicesArray[0];
                            strSelectedProductId = strGetChoicesArray[1];
                        } else {
                            strHarmonyFinalId = strHarmony;
                        }
                    }
                    if (UIUtil.isNotNullAndNotEmpty(strHarmonyFinalId)) {
                        StringBuilder sb = new StringBuilder("");
                        DomainObject domHarmony = DomainObject.newInstance(context, strHarmonyFinalId);
                        String strHarmonyName = domHarmony.getInfo(context, DomainConstants.SELECT_NAME);
                        if (UIUtil.isNotNullAndNotEmpty(strSelectedProductId)) {
                            DomainObject domProductConfig = DomainObject.newInstance(context, strSelectedProductId);
                            String strPCName = domProductConfig.getAttributeValue(context, TigerConstants.ATTRIBUTE_MARKETINGNAME);
                            sb.append(strOperationNumber);
                            sb.append(strHarmonyName);
                            sb.append("(");
                            sb.append(strPCName);
                            sb.append(")");
                        } else {
                            sb.append(strOperationNumber);
                            sb.append(strHarmonyName);
                        }
                        domObject.setAttributeValue(context, TigerConstants.ATTRIBUTE_V_NAME, sb.toString());
                    }
                }
            } else if (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_LINEDATA)) {
                String strTitle = domObject.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_LINEDATA_NUMBER);
                domObject.setAttributeValue(context, TigerConstants.ATTRIBUTE_V_NAME, strTitle);
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in setOperationLineDataTitle: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public HashMap getTechnologyForOperation(Context context, String[] args) throws Exception {
        HashMap returnMap = new HashMap();
        try {
            StringList slChoicesDisp = new StringList();
            StringList slChoices = new StringList();
            String strConstantDash = "-";
            final String PROPERTIES_KEY = "PSS_FRCMBOMCentral.CreateMBOM.BG.";

            Map programMap = JPO.unpackArgs(args);
            Map rMap = (Map) programMap.get("requestMap");
            String ObjectID = (String) rMap.get("objectId");
            DomainObject dom = DomainObject.newInstance(context, ObjectID);
            String strBG = dom.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_OPERATION_BG);
            if (!strBG.equals(strConstantDash) && UIUtil.isNotNullAndNotEmpty(strBG)) {
                String query = PROPERTIES_KEY + strBG;
                String strTechnologyList = EnoviaResourceBundle.getProperty(context, TigerConstants.PROPERTIES_EMXFRCMBOMCENTRAL, context.getLocale(), query);
                StringList slTechnologyList = FrameworkUtil.split(strTechnologyList, "|");

                for (int i = 0; i < slTechnologyList.size(); i++) {
                    String strGetValues = (String) slTechnologyList.get(i);
                    slChoices.add(strGetValues);
                    slChoicesDisp.add(strGetValues);
                }
            } else {
                slChoices.add(strConstantDash);
                slChoicesDisp.add(strConstantDash);
            }
            returnMap.put("field_choices", slChoices);
            returnMap.put("field_display_choices", slChoicesDisp);
            return returnMap;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getTechnologyForOperation: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public HashMap getSpecificTechnologyForOperation(Context context, String[] args) throws Exception {
        HashMap returnMap = new HashMap();
        try {
            StringList slChoicesDisp = new StringList();
            StringList slChoices = new StringList();
            String strDot = ".";
            String strConstantDash = "-";
            final String PROPERTIES_KEY = "PSS_FRCMBOMCentral.CreateMBOM.BG.";

            Map programMap = JPO.unpackArgs(args);
            Map rMap = (Map) programMap.get("requestMap");
            String ObjectID = (String) rMap.get("objectId");
            DomainObject dom = DomainObject.newInstance(context, ObjectID);
            String strTechnology = dom.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_OPERATION_TECHNOLOGY);
            String strBG = dom.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_OPERATION_BG);
            String strTechnologyName = strTechnology.replaceAll(" ", "_");
            if (UIUtil.isNotNullAndNotEmpty(strTechnologyName) && !strTechnologyName.equals(strConstantDash)) {
                String query = PROPERTIES_KEY + strBG + strDot + strTechnologyName;
                String strTechnologyList = EnoviaResourceBundle.getProperty(context, TigerConstants.PROPERTIES_EMXFRCMBOMCENTRAL, context.getLocale(), query);
                StringList slTechnologyList = FrameworkUtil.split(strTechnologyList, "|");
                for (int i = 0; i < slTechnologyList.size(); i++) {
                    String strGetValues = (String) slTechnologyList.get(i);
                    slChoices.add(strGetValues);
                    slChoicesDisp.add(strGetValues);
                }
            } else {
                String strGetValues = DomainObject.EMPTY_STRING;
                slChoices.add(strGetValues);
                slChoicesDisp.add(strGetValues);
            }
            returnMap.put("field_choices", slChoices);
            returnMap.put("field_display_choices", slChoicesDisp);
            return returnMap;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getSpecificTechnologyForOperation: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    @SuppressWarnings("rawtypes")
    public String getDisplayValue(Context context, String[] args) throws Exception {
        try {
            String displayText = DomainConstants.EMPTY_STRING;
            Map programMap = JPO.unpackArgs(args);
            Map mParamMap = (Map) programMap.get("paramMap");
            Map fieldMap = (Map) programMap.get("fieldMap");
            Map SettingMap = (Map) fieldMap.get("settings");
            String strOperationId = (String) mParamMap.get("objectId");
            String admin_Type = (String) SettingMap.get("Admin Type");
            String strHarmonyFinalId = DomainObject.EMPTY_STRING;
            String strSelectedProductId = DomainObject.EMPTY_STRING;
            StringBuffer sb = new StringBuffer();
            if (UIUtil.isNotNullAndNotEmpty(strOperationId)) {
                DomainObject domOperation = DomainObject.newInstance(context, strOperationId);
                String attrName = PropertyUtil.getSchemaProperty(context, admin_Type);
                String attrValue = domOperation.getAttributeValue(context, attrName);

                if (attrValue.contains("|")) {
                    String[] strGetChoicesArray = attrValue.split("\\|");
                    strHarmonyFinalId = strGetChoicesArray[0];
                    strSelectedProductId = strGetChoicesArray[1];
                } else {
                    strHarmonyFinalId = attrValue;
                }
                if (UIUtil.isNotNullAndNotEmpty(strHarmonyFinalId) && strHarmonyFinalId.length() == 32) {
                    DomainObject dObj = DomainObject.newInstance(context, strHarmonyFinalId);
                    if (admin_Type.equalsIgnoreCase("attribute_PSS_Operation.PSS_Harmony")) {

                        // TIGTK-10500 :Start
                        String strHarmony = dObj.getInfo(context, TigerConstants.SELECT_ATTRIBUTE_DISPLAYNAME);
                        // TIGTK-10500 :End
                        if (UIUtil.isNotNullAndNotEmpty(strSelectedProductId)) {
                            DomainObject domProductConfig = DomainObject.newInstance(context, strSelectedProductId);
                            String strPCName = domProductConfig.getAttributeValue(context, TigerConstants.ATTRIBUTE_MARKETINGNAME);
                            sb.append(strHarmony);
                            sb.append("(");
                            sb.append(strPCName);
                            sb.append(")");
                            displayText = sb.toString();
                        } else {
                            displayText = strHarmony;

                        }

                    } else if (admin_Type.equalsIgnoreCase("attribute_PSS_Operation.PSS_Color")) {
                        String strColorCode = dObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_COLORCODE);
                        String strDisplayName = dObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_DISPLAYNAME);
                        StringBuilder colorDisplayText = new StringBuilder(strColorCode);
                        colorDisplayText.append("(");
                        colorDisplayText.append(strDisplayName);
                        colorDisplayText.append(")");
                        displayText = colorDisplayText.toString();
                    } else if (admin_Type.equalsIgnoreCase("attribute_PSS_Operation.PSS_VariantName")) {
                        displayText = dObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_VARIANTID);
                    }
                } else {
                    displayText = attrValue;
                }
            }
            return displayText;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getDisplayValue: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    @SuppressWarnings("rawtypes")
    public String getHarmonyOnChangeValues(Context context, String[] args) throws Exception {

        StringBuffer sb = new StringBuffer();
        try {
            final String ATTRIBUTE_COLORPID = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ColorPID");
            final String ATTRIBUTE_CUSTOMERPARTNUMBER = PropertyUtil.getSchemaProperty(context, "attribute_PSS_CustomerPartNumber");
            final String ATTRIBUTE_PRODUCTCONFIGURATIONPID = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ProductConfigurationPID");
            final String ATTRIBUTE_QUANTITY = PropertyUtil.getSchemaProperty(context, "attribute_Quantity");
            String strColorPID = DomainObject.EMPTY_STRING;
            String strCustomerPartNumber = DomainObject.EMPTY_STRING;
            String strQunatity = DomainObject.EMPTY_STRING;
            String strPCId = DomainObject.EMPTY_STRING;

            Map programMap = JPO.unpackArgs(args);
            String strGetRelId = (String) programMap.get("strGetRelId");
            String strSelectedProductId = (String) programMap.get("strSelectedProductId");

            StringList slectedIds = FrameworkUtil.split(strGetRelId, ",");
            for (int i = 0; i < slectedIds.size(); i++) {
                String relId = (String) slectedIds.get(i);

                DomainRelationship domRel = new DomainRelationship(relId);
                strPCId = domRel.getAttributeValue(context, ATTRIBUTE_PRODUCTCONFIGURATIONPID);
                strQunatity = domRel.getAttributeValue(context, ATTRIBUTE_QUANTITY);
                strCustomerPartNumber = domRel.getAttributeValue(context, ATTRIBUTE_CUSTOMERPARTNUMBER);
                if (UIUtil.isNotNullAndNotEmpty(strPCId) && UIUtil.isNotNullAndNotEmpty(strSelectedProductId)) {
                    if (strPCId.equalsIgnoreCase(strSelectedProductId)) {
                        strColorPID = domRel.getAttributeValue(context, ATTRIBUTE_COLORPID);
                        if (UIUtil.isNotNullAndNotEmpty(strColorPID)) {
                            DomainObject domColorOption = DomainObject.newInstance(context, strColorPID);
                            String strColorCode = domColorOption.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_COLORCODE);
                            String strDisplayName = domColorOption.getAttributeValue(context, TigerConstants.ATTRIBUTE_DISPLAYNAME);

                            StringBuilder colorDisplayText = new StringBuilder(strColorCode);
                            colorDisplayText.append("(");
                            colorDisplayText.append(strDisplayName);
                            colorDisplayText.append(")");

                            sb.append(strColorPID);
                            sb.append(",");
                            sb.append(colorDisplayText.toString());
                            sb.append(",");
                            sb.append(strQunatity);
                            sb.append(",");
                            sb.append(strCustomerPartNumber);
                            sb.append(";");
                        }
                    }
                } else if (UIUtil.isNullOrEmpty(strSelectedProductId) && UIUtil.isNullOrEmpty(strPCId)) {

                    strColorPID = domRel.getAttributeValue(context, ATTRIBUTE_COLORPID);
                    if (UIUtil.isNotNullAndNotEmpty(strColorPID)) {
                        DomainObject domColorOption = DomainObject.newInstance(context, strColorPID);
                        String strColorCode = domColorOption.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_COLORCODE);
                        String strDisplayName = domColorOption.getAttributeValue(context, TigerConstants.ATTRIBUTE_DISPLAYNAME);

                        StringBuilder colorDisplayText = new StringBuilder(strColorCode);
                        colorDisplayText.append("(");
                        colorDisplayText.append(strDisplayName);
                        colorDisplayText.append(")");

                        sb.append(strColorPID);
                        sb.append(",");
                        sb.append(colorDisplayText.toString());
                        sb.append(",");
                        sb.append(strQunatity);
                        sb.append(",");
                        sb.append(strCustomerPartNumber);
                        sb.append(";");
                    }

                }

            }

        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getDisplayValue: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
        return (sb.toString());
    }

}