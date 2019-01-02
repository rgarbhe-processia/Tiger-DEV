package pss.mbom.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.Pattern;
import matrix.util.StringList;
import pss.constants.TigerConstants;

public class Operation_mxJPO {

    // TIGTK-5405 - 06-04-2017 - VB - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Operation_mxJPO.class);

    // TIGTK-5405 - 06-04-2017 - VB - END

    public Operation_mxJPO() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * this method is used to connect Harmony request with program object ,which is related to the parent MBOM object
     * @param context
     * @param args
     * @return
     * @throws Exception
     *             Exception appears, if error occured
     */

    public static void setFlagForUpdateIcon(Context context, String[] args) throws Exception {

        HashMap param = (HashMap) JPO.unpackArgs(args);
        HashMap requestMap = (HashMap) param.get("requestMap");
        String objectId = (String) requestMap.get("objectId");
        String FALSE = "false";
        DomainObject domObj = DomainObject.newInstance(context, objectId);
        domObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_OPERATIONLINEDATA_EXT_PSS_DIRTY, FALSE);

    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public StringList updateOperationLineData(Context context, String[] args) throws Exception {
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        String strObjId = (String) paramMap.get("objectId");
        String strRowId = (String) paramMap.get("rowId");
        String strRelId = (String) paramMap.get("relId");
        // TIGTK-9254:Rutuja Ekatpure:23/8/2017:Start
        String strParentId = (String) paramMap.get("parentId");
        // TIGTK-9254:Rutuja Ekatpure:23/8/2017:End
        String strRels = TigerConstants.RELATIONSHIP_DELFMIFUNCTIONIDENTIFIEDINSTANCE + "," + TigerConstants.RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS;
        StringList objectSelects = new StringList();
        objectSelects.addElement(DomainConstants.SELECT_ID);
        objectSelects.addElement(DomainConstants.SELECT_NAME);
        objectSelects.addElement(DomainConstants.SELECT_TYPE);

        StringList selectRelStmts = new StringList();
        selectRelStmts.addElement(DomainRelationship.SELECT_ID);
        selectRelStmts.addElement(TigerConstants.SELECT_ATTRIBUTE_PSS_COLORID);
        selectRelStmts.addElement(TigerConstants.SELECT_ATTRIBUTE_QUANTITY);
        selectRelStmts.addElement(TigerConstants.SELECT_ATTRIBUTE_CUSTOMER_PARTNUMBER);

        StringList slselectAttribute = new StringList();
        slselectAttribute.addElement(TigerConstants.SELECT_ATTRIBUTE_PSS_OPERATION_PSS_HARMONY);
        slselectAttribute.addElement(TigerConstants.SELECT_ATTRIBUTE_PSS_OPERATION_PSS_QUANTITY);
        slselectAttribute.addElement(TigerConstants.SELECT_ATTRIBUTE_PSS_OPERATION_PSS_COLOR);
        slselectAttribute.addElement(TigerConstants.SELECT_ATTRIBUTE_PSS_OPERATION_PSS_OPNCUSTOMER_PART_NUMBER);

        StringList slSelectedItems = new StringList();

        try {
            MapList objectList = new MapList();
            StringList processedObjeList = new StringList();
            DomainObject domObj = DomainObject.newInstance(context, strObjId);
            String strType = domObj.getInfo(context, DomainConstants.SELECT_TYPE);

            if (strType.equalsIgnoreCase(TigerConstants.TYPE_CREATEASSEMBLY) || strType.equalsIgnoreCase(TigerConstants.TYPE_CREATEKIT) || strType.equalsIgnoreCase(TigerConstants.TYPE_CREATEMATERIAL)
                    || TigerConstants.LIST_TYPE_MATERIALS.contains(strType)) {

                Pattern patternType = new Pattern(TigerConstants.TYPE_PSS_LINEDATA);
                patternType.addPattern(TigerConstants.TYPE_PSS_OPERATION);
                patternType.addPattern(TigerConstants.TYPE_CREATEASSEMBLY);
                patternType.addPattern(TigerConstants.TYPE_CREATEKIT);
                patternType.addPattern(TigerConstants.TYPE_CREATEMATERIAL);
                patternType.addPattern(TigerConstants.TYPE_PROCESSCONTINUOUSPROVIDE);
                patternType.addPattern(TigerConstants.TYPE_PROCESS_CONTINUOUS_CREATE_MATERIAL);

                MapList mlConnectedObj = domObj.getRelatedObjects(context, strRels, // relationship pattern
                        patternType.getPattern(), // object pattern
                        objectSelects, // object selects
                        selectRelStmts, // relationship selects
                        false, // to direction
                        true, // from direction
                        (short) 0, // recursion level
                        null, // object where clause
                        null, (short) 0, false, // checkHidden
                        true, // preventDuplicates
                        (short) 1000, // pageSize
                        null, // Postpattern
                        null, null, null);

                if (mlConnectedObj != null) {
                    int listSize = mlConnectedObj.size();
                    List<Integer> levelIndexList = new ArrayList<Integer>();
                    levelIndexList.add(0, 0);
                    for (int i = 0; i < listSize; i++) {
                        Map mRelatedObjMap = (Map) mlConnectedObj.get(i);
                        String strId = (String) mRelatedObjMap.get(DomainConstants.SELECT_ID);
                        String strLevel = (String) mRelatedObjMap.get("level");
                        int levelNow = Integer.parseInt(strLevel);
                        levelIndexList.add(levelNow, i);
                        String strLocalType = (String) mRelatedObjMap.get(DomainConstants.SELECT_TYPE);
                        if (!processedObjeList.contains(strId) && UIUtil.isNotNullAndNotEmpty(strLocalType)
                                && (strLocalType.equals(TigerConstants.TYPE_PSS_LINEDATA) || strLocalType.equals(TigerConstants.TYPE_PSS_OPERATION))) {
                            String relId = null;
                            if (levelNow - 1 > 0) {
                                relId = (String) ((Map) mlConnectedObj.get(levelIndexList.get(levelNow - 1))).get(DomainConstants.SELECT_RELATIONSHIP_ID);
                            } else if (levelNow - 1 <= 0) {
                                relId = strRelId;
                            }
                            Map objMap = new HashMap();
                            objMap.put(DomainConstants.SELECT_ID, strId);
                            objMap.put(DomainConstants.SELECT_RELATIONSHIP_ID, relId);
                            objectList.add(objMap);
                            processedObjeList.addElement(strId);
                        }
                    }
                }
            } else if (!processedObjeList.contains(strObjId) && strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_LINEDATA) || strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_OPERATION)) {
                Map objMap = new HashMap();
                objMap.put(DomainConstants.SELECT_ID, strObjId);
                objMap.put(DomainConstants.SELECT_RELATIONSHIP_ID, strRelId);
                objectList.add(objMap);
                processedObjeList.addElement(strObjId);
                // TIGTK-9254:Rutuja Ekatpure:23/8/2017:Start
                // Check for Line data on same parent
                if (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_LINEDATA)) {
                    DomainObject domParent = new DomainObject(strParentId);
                    String strObjectWhere = "relationship[DELFmiFunctionIdentifiedInstance].to.type ==" + TigerConstants.TYPE_PSS_LINEDATA;

                    StringList slobjectSelects = new StringList();
                    slobjectSelects.addElement("physicalid");

                    StringList slselectRelStmts = new StringList();
                    slselectRelStmts.addElement(DomainRelationship.SELECT_ID);
                    MapList mlConnectedLineObj = domParent.getRelatedObjects(context, TigerConstants.RELATIONSHIP_DELFMIFUNCTIONIDENTIFIEDINSTANCE, // relationship pattern
                            TigerConstants.TYPE_PSS_LINEDATA, // object pattern
                            slobjectSelects, // object selects
                            slselectRelStmts, // relationship selects
                            false, // to direction
                            true, // from direction
                            (short) 1, // recursion level
                            strObjectWhere, // object where clause
                            null, (short) 0, false, // checkHidden
                            true, // preventDuplicates
                            (short) 1000, // pageSize
                            null, // Postpattern
                            null, null, null);
                    if (!mlConnectedLineObj.isEmpty()) {
                        Iterator itrLineObj = mlConnectedLineObj.iterator();
                        while (itrLineObj.hasNext()) {
                            Map mLineData = (Map) itrLineObj.next();

                            String strLineId = (String) mLineData.get("physicalid");
                            if (!processedObjeList.contains(strLineId)) {
                                Map lineObjMap = new HashMap();
                                lineObjMap.put(DomainConstants.SELECT_ID, strLineId);
                                lineObjMap.put(DomainConstants.SELECT_RELATIONSHIP_ID, mLineData.get(DomainConstants.SELECT_RELATIONSHIP_ID));
                                objectList.add(lineObjMap);
                                processedObjeList.addElement(strLineId);
                            }
                        }
                    }
                }
                // TIGTK-9254:Rutuja Ekatpure:23/8/2017:End
            }
            String strDefaultQuantity = "1.0";
            for (int j = 0; j < objectList.size(); j++) {
                boolean flag = false;
                Map objMap = (Map) objectList.get(j);
                String strLineOperationId = (String) objMap.get(DomainConstants.SELECT_ID);
                String strRelationshipId = (String) objMap.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                DomainObject domlineOperationObj = DomainObject.newInstance(context, strLineOperationId);
                String strMBOMId = domlineOperationObj.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_DELFMIFUNCTIONIDENTIFIEDINSTANCE + "].from.id");
                DomainObject domMBOMParentObj = DomainObject.newInstance(context, strMBOMId);

                String strLineOpType = domlineOperationObj.getInfo(context, DomainConstants.SELECT_TYPE);

                if (strLineOpType.equalsIgnoreCase(TigerConstants.TYPE_PSS_OPERATION)) {
                    Map mGetOpAttribute = domlineOperationObj.getInfo(context, slselectAttribute);

                    String strHarmony = (String) mGetOpAttribute.get(TigerConstants.SELECT_ATTRIBUTE_PSS_OPERATION_PSS_HARMONY);
                    String strOpQuantity = (String) mGetOpAttribute.get(TigerConstants.SELECT_ATTRIBUTE_PSS_OPERATION_PSS_QUANTITY);
                    String strOpColor = (String) mGetOpAttribute.get(TigerConstants.SELECT_ATTRIBUTE_PSS_OPERATION_PSS_COLOR);
                    String strOpCPN = (String) mGetOpAttribute.get(TigerConstants.SELECT_ATTRIBUTE_PSS_OPERATION_PSS_OPNCUSTOMER_PART_NUMBER);

                    if (!strHarmony.equalsIgnoreCase("All")) {
                        MapList mlHarmony = null;
                        if (UIUtil.isNotNullAndNotEmpty(strHarmony) && strHarmony.startsWith("HMY-")) {
                            strHarmony = MqlUtil.mqlCommand(context, "print bus $1 $2 $3 select $4 dump", TigerConstants.TYPE_PSS_HARMONY, strHarmony, "-", "physicalid");
                        }
                        String[] harmonyParts = strHarmony.split("\\|");
                        String pcPID = DomainConstants.EMPTY_STRING;
                        if (harmonyParts != null) {
                            if (harmonyParts.length > 0)
                                strHarmony = harmonyParts[0];
                            if (harmonyParts.length == 2)
                                pcPID = harmonyParts[1];
                        }

                        if (UIUtil.isNotNullAndNotEmpty(strRelationshipId)) {
                            // String strHarmonyIds = MqlUtil.mqlCommand(context, "print connection " + strRelationshipId + " select frommid.to.id dump", true, false);
                            String strAssociationIds = MqlUtil.mqlCommand(context, "print connection " + strRelationshipId + " select frommid.id dump", true, true);
                            // StringList slectedIds = FrameworkUtil.split(strHarmonyIds, ",");
                            StringList slectedIds1 = FrameworkUtil.split(strAssociationIds, ",");
                            int nSize = slectedIds1.size();
                            for (int y = 0; y < nSize; y++) {
                                String associationId = (String) slectedIds1.get(y);
                                DomainRelationship domRel = DomainRelationship.newInstance(context, associationId);
                                Map attributeMap = domRel.getAttributeMap(context, true);
                                String toHarmonyPhysicalId = MqlUtil.mqlCommand(context, "print connection " + associationId + " select to.physicalid dump", true, true);
                                if (pcPID.equals((String) attributeMap.get(TigerConstants.ATTRIBUTE_PSS_PRODUCT_CONFIGURATION_PID)) && strHarmony.equals(toHarmonyPhysicalId)) {
                                    String strRelColorPID = (String) attributeMap.get(TigerConstants.ATTRIBUTE_PSS_COLORPID);
                                    if (UIUtil.isNotNullAndNotEmpty(strRelColorPID) && !strRelColorPID.equalsIgnoreCase("Ignore") && !strRelColorPID.equalsIgnoreCase("N/A")
                                            && !strRelColorPID.equals(strOpColor)) {
                                        domlineOperationObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_OPERATION_PSS_COLOR, strRelColorPID);
                                        flag = true;
                                    } else if (!strOpColor.equals(strRelColorPID) && !strOpColor.equalsIgnoreCase("All")) {
                                        domlineOperationObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_OPERATION_PSS_COLOR, "");
                                        flag = true;
                                    }
                                    String strRelQuantity = (String) attributeMap.get(TigerConstants.ATTRIBUTE_QUANTITY);
                                    if (!strRelQuantity.equalsIgnoreCase(strOpQuantity)) {
                                        domlineOperationObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_OPERATION_PSS_QUANTITY, strRelQuantity);
                                        flag = true;
                                    }
                                    String strCustomerPartNumber = (String) attributeMap.get(TigerConstants.ATTRIBUTE_PSS_CUSTOMERPARTNUMBER);
                                    if (!strCustomerPartNumber.equalsIgnoreCase(strOpCPN) && !strOpCPN.equalsIgnoreCase("All")) {
                                        domlineOperationObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_OPERATION_PSS_OPNCUSTOMER_PART_NUMBER, strCustomerPartNumber);
                                        flag = true;
                                    }
                                }
                            }
                        } else {

                            String objectWhere = "physicalid==" + strHarmony;
                            StringBuilder relationshipWhere = new StringBuilder("attribute[");
                            relationshipWhere.append(TigerConstants.ATTRIBUTE_PSS_PRODUCT_CONFIGURATION_PID);
                            relationshipWhere.append("].value==\"");
                            relationshipWhere.append(pcPID);
                            relationshipWhere.append("\"");

                            mlHarmony = domMBOMParentObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_HARMONYASSOCIATION, // relationship pattern
                                    TigerConstants.TYPE_PSS_HARMONY, // object pattern
                                    objectSelects, // object selects
                                    selectRelStmts, // relationship selects
                                    false, // to direction
                                    true, // from direction
                                    (short) 1, // recursion level
                                    objectWhere, // object where clause
                                    relationshipWhere.toString(), 0);
                            if (mlHarmony.isEmpty()) {
                                domlineOperationObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_OPERATION_PSS_COLOR, "");
                                domlineOperationObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_OPERATION_PSS_QUANTITY, strDefaultQuantity);
                                domlineOperationObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_OPERATION_PSS_OPNCUSTOMER_PART_NUMBER, "");
                                flag = true;
                            } else {
                                Map mHarmonyMap = (Map) mlHarmony.get(0);
                                String strRelColorPID = (String) mHarmonyMap.get(TigerConstants.SELECT_ATTRIBUTE_PSS_COLORID);
                                String strRelQuantity = (String) mHarmonyMap.get(TigerConstants.SELECT_ATTRIBUTE_QUANTITY);
                                String strRelCPN = (String) mHarmonyMap.get(TigerConstants.SELECT_ATTRIBUTE_CUSTOMER_PARTNUMBER);

                                if (!UIUtil.isNullOrEmpty(strRelColorPID) && !strRelColorPID.equalsIgnoreCase("Ignore") && !strRelColorPID.equalsIgnoreCase("N/A")
                                        && !strRelColorPID.equals(strOpColor)) {
                                    domlineOperationObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_OPERATION_PSS_COLOR, strRelColorPID);
                                    flag = true;
                                } else if (!strOpColor.equals(strRelColorPID) && !strOpColor.equalsIgnoreCase("All")) {
                                    domlineOperationObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_OPERATION_PSS_COLOR, "");
                                    flag = true;
                                }

                                if (!strRelQuantity.equalsIgnoreCase(strOpQuantity)) {
                                    domlineOperationObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_OPERATION_PSS_QUANTITY, strRelQuantity);
                                    flag = true;
                                }

                                if (!strRelCPN.equalsIgnoreCase(strOpCPN) && !"All".equalsIgnoreCase(strOpCPN)) {
                                    domlineOperationObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_OPERATION_PSS_OPNCUSTOMER_PART_NUMBER, strRelCPN);
                                    flag = true;
                                }
                            }
                        }

                    }

                    String strVariantName = domlineOperationObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_OPERATION_PSS_VARIANTNAME);
                    if (UIUtil.isNotNullAndNotEmpty(strVariantName) && !strVariantName.equalsIgnoreCase("All")) {
                        String objWhere = "name == \"" + strVariantName + "\"";
                        if (strVariantName.length() == 32) {
                            objWhere = "physicalid == " + strVariantName;
                        }
                        MapList mlVariantName = domMBOMParentObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_PARTVARIANTASSEMBLY, // relationship pattern
                                TigerConstants.TYPE_PSS_MBOM_VARIANT_ASSEMBLY, // object pattern
                                objectSelects, // object selects
                                null, // relationship selects
                                false, // to direction
                                true, // from direction
                                (short) 1, // recursion level
                                objWhere, // object where clause
                                null, 0);

                        if (mlVariantName.isEmpty()) {
                            domlineOperationObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_OPERATION_PSS_VARIANTNAME, "");
                            flag = true;
                        }
                    }
                }

                if (strLineOpType.equalsIgnoreCase(TigerConstants.TYPE_PSS_LINEDATA)) {
                    MapList mlColorOptions = domMBOMParentObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_COLORLIST, // relationship pattern
                            TigerConstants.TYPE_PSS_COLOROPTION, // object pattern
                            objectSelects, // object selects
                            null, // relationship selects
                            false, // to direction
                            true, // from direction
                            (short) 1, // recursion level
                            null, // object where clause
                            null, (short) 0, false, // checkHidden
                            true, // preventDuplicates
                            (short) 1000, // pageSize
                            null, // Postpattern
                            null, null, null);

                    int nNoOfColorChnages = mlColorOptions.size() - 1;
                    String strColorChanges = Integer.toString(nNoOfColorChnages);

                    String strNumberOfColorChanges = domlineOperationObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_LINEDATA_PSS_NUMBEROFCOLORCHANGES);

                    if (!strNumberOfColorChanges.equals(strColorChanges)) {
                        domlineOperationObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_LINEDATA_PSS_NUMBEROFCOLORCHANGES, strColorChanges);
                        flag = true;
                    }
                }
                if (flag == true) {
                    String strTrue = "true";
                    domlineOperationObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_OPERATIONLINEDATA_EXT_PSS_DIRTY, strTrue);
                    slSelectedItems.add(strRowId);
                }
            }
        } // Fix for FindBugs issue RuntimeException capture: Suchit Gangurde: 28 Feb 2017: START
        catch (RuntimeException e) {
            // TIGTK-5405 - 06-04-2017 - VB - START
            logger.error("Error in updateOperationLineData: ", e);
            // TIGTK-5405 - 06-04-2017 - VB - END
            throw e;
        } // Fix for FindBugs issue RuntimeException capture: Suchit Gangurde: 28 Feb 2017: END
        catch (Exception e) {
            // TIGTK-5405 - 06-04-2017 - VB - START
            logger.error("Error in updateOperationLineData: ", e);
            // TIGTK-5405 - 06-04-2017 - VB - END
            throw e;
        }
        return slSelectedItems;

    }

}
