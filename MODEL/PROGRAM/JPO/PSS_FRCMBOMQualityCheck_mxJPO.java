import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dassault_systemes.vplm.modeler.PLMCoreModelerSession;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.framework.ui.UIUtil;
import com.matrixone.apps.productline.ProductLineConstants;
import com.mbom.modeler.utility.FRCMBOMModelerUtility;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;
import pss.constants.TigerConstants;

public class PSS_FRCMBOMQualityCheck_mxJPO {

    private static final String IS_UNIT_OF_MEASURE_PC = "PC";

    private static final String SELECT_RELATIONSHIP_HARMONY_ASSOCIATION_TOID = "frommid[" + TigerConstants.RELATIONSHIP_PSS_HARMONYASSOCIATION + "].to.id";

    private static final String SELECT_RELATIONSHIP_HARMONY_ASSOCIATION_ID = "frommid[" + TigerConstants.RELATIONSHIP_PSS_HARMONYASSOCIATION + "].id";

    private static final String SELECT_RELATIONSHIP_DELFMIFUNCTIONIDENTIFIEDINSTANCE_TOID = "from[" + TigerConstants.RELATIONSHIP_DELFMIFUNCTIONIDENTIFIEDINSTANCE + "].to.id";

    private static final String SELECT_RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS_TOID = "from[" + TigerConstants.RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS + "].to.id";

    private static final String SELECT_RELATIONSHIP_PSS_COLOR_LIST_TOID = "from[" + TigerConstants.RELATIONSHIP_PSS_COLORLIST + "].to.id";

    private static final String SELECT_RELATIONSHIP_MAINPRODUCT_TOID = "from[" + TigerConstants.RELATIONSHIP_MAINPRODUCT + "].to.id";

    private static final String SELECT_ATTRIBUTE_FAURECIA_FULL_LENGTH_DESC = "attribute[" + TigerConstants.ATTRIBUTE_FAURECIA_FULL_LENGTH_DESC + "].value";

    private static final String SELECT_ATTRIBUTE_FAURECIA_SHORT_LENGTH_DESC = "attribute[" + TigerConstants.ATTRIBUTE_FAURECIA_SHORT_LENGTH_DESC + "].value";

    private static final String SELECT_ATTRIBUTE_CUSTOMER_DESCRIPTION = "attribute[" + TigerConstants.ATTRIBUTE_CUSTOMER_DESCRIPTION + "].value";

    private static final String SELECT_ATTRIBUTE_CUSTOMER_PARTNUMBER = "attribute[" + TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXT_PSS_CUSTOMER_PART_NUMBER + "].value";

    private static final String SELECT_ATTRIBUTE_ALTERNATE_DESCRIPTION = "attribute[" + TigerConstants.ATTRIBUTE_ALTERNATE_DESCRIPTION + "].value";

    private static final String SELECT_ATTRIBUTE_SUPPLIERS_PART_NUMBER = "attribute[" + TigerConstants.ATTRIBUTE_SUPPLIERS_PART_NUMBER + "].value";

    private static final String SELECT_ATTRIBUTE_TYPE_OF_PART = "attribute[" + TigerConstants.ATTRIBUTE_TYPE_OF_PART + "].value";

    private static final String SELECT_ATTRIBUTE_UNITOF_MEASURE = "attribute[" + TigerConstants.ATTRIBUTE_PSS_UNIT_OF_MEASURE + "].value";

    private static final String SELECT_ATTRIBUTE_PSS_UNIT_OF_MEASURE_CATEGORY = "attribute[" + TigerConstants.ATTRIBUTE_PSS_UNIT_OF_MEASURE_CATEGORY + "].value";

    private static final String SELECT_ATTRIBUTE_PSS_PDMCLASS = "attribute[" + TigerConstants.ATTRIBUTE_PDM_CLASS + "].value";

    private static final String SELECT_ATTRIBUTE_PSS_UNIT_OF_MEASURE = "attribute[" + TigerConstants.ATTRIBUTE_PSS_UNIT_OF_MEASURE + "].value";

    private static final String SELECT_SELECTED_CONFIGURATION_OPTIONS = "from[" + ProductLineConstants.RELATIONSHIP_SELECTED_OPTIONS + "].torel[" + TigerConstants.RELATIONSHIP_CONFIGURATION_OPTION
            + "].id";

    private static final String SELECT_ATTRIBUTE_PLMINSTANCE_V_TREEORDER = "attribute[" + TigerConstants.ATTRIBUTE_PLMINSTANCE_V_TREEORDER + "].value";

    private static final StringList EXPD_BUS_SELECT = new StringList(new String[] { "physicalid", "logicalid" });

    private static final StringList EXPD_REL_SELECT = new StringList(new String[] { "physicalid[connection]", "logicalid[connection]", "from.physicalid", SELECT_ATTRIBUTE_PLMINSTANCE_V_TREEORDER });

    // TIGTK-5405 - 06-04-2017 - VB - START
    protected static final Logger logger = LoggerFactory.getLogger(PSS_FRCMBOMQualityCheck_mxJPO.class);

    // TIGTK-5405 - 06-04-2017 - VB - END

    public PSS_FRCMBOMQualityCheck_mxJPO() {
        super();
    }

    @SuppressWarnings("deprecation")
    private static void flushAndCloseSession(com.dassault_systemes.vplm.modeler.PLMCoreModelerSession plmSession) {
        try {
            plmSession.flushSession();
        } catch (Exception e) {
        }

        try {
            plmSession.closeSession(true);
        } catch (Exception e) {
        }
    }

    /**
     * Check that the quantity of parts measured by "PC" must be an integer
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unused")
    private static int checkQuantityOfParts(Context context, Map<?, ?> programMap) throws Exception {

        int result = 0;

        String strObjectId = (String) programMap.get(DomainConstants.SELECT_ID);
        String strRelId = (String) programMap.get(DomainConstants.SELECT_RELATIONSHIP_ID);

        DomainObject domMBOM = DomainObject.newInstance(context, strObjectId);
        StringList slSelects = new StringList();
        slSelects.add(SELECT_RELATIONSHIP_HARMONY_ASSOCIATION_TOID);
        slSelects.add(SELECT_RELATIONSHIP_HARMONY_ASSOCIATION_ID);

        StringBuilder stringBuilder = new StringBuilder("attribute[" + TigerConstants.ATTRIBUTE_PSS_COLORPID + "].value != \'\' && ");
        stringBuilder.append("attribute[" + TigerConstants.ATTRIBUTE_PSS_COLORPID + "].value != 'Ignore'");

        String strWhereExp = stringBuilder.toString();
        if (UIUtil.isNullOrEmpty(strRelId)) {
            MapList slHarmonyList = domMBOM.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_HARMONYASSOCIATION, // Relationship
                    // Pattern
                    TigerConstants.TYPE_PSS_HARMONY, // Object Pattern
                    new StringList(DomainConstants.SELECT_ID), // Object Selects
                    new StringList(DomainConstants.SELECT_RELATIONSHIP_ID), // Relationship Selects
                    false, // to direction
                    true, // from direction
                    (short) 0, // recursion level
                    null, // object where clause
                    strWhereExp, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    null, // Post Type Pattern
                    null, null, null);
            if (slHarmonyList != null && !slHarmonyList.isEmpty()) {
                for (int i = 0; i < slHarmonyList.size(); i++) {
                    Map<?, ?> mTempMap = (Map<?, ?>) slHarmonyList.get(i);
                    DomainRelationship domRel = DomainRelationship.newInstance(context, (String) mTempMap.get(DomainConstants.SELECT_RELATIONSHIP_ID));
                    String strValue = (String) programMap.get(SELECT_ATTRIBUTE_UNITOF_MEASURE);

                    if (IS_UNIT_OF_MEASURE_PC.equalsIgnoreCase(strValue)) {
                        String strQuantity = domRel.getAttributeValue(context, TigerConstants.ATTRIBUTE_QUANTITY);
                        StringList slTempQuantity = FrameworkUtil.split(strQuantity, ".");
                        int sQuantity = Integer.parseInt((String) slTempQuantity.get(1));
                        if (sQuantity != 0) {
                            result = 1;
                            break;
                        }

                    }
                }

            }
        } else {
            MapList slConnectedHarmonyList = DomainRelationship.getInfo(context, new String[] { strRelId }, slSelects);
            if (slConnectedHarmonyList != null && !slConnectedHarmonyList.isEmpty()) {
                for (int i = 0; i < slConnectedHarmonyList.size(); i++) {
                    Map<?, ?> mTempMap = (Map<?, ?>) slConnectedHarmonyList.get(i);
                    if (mTempMap.containsKey(SELECT_RELATIONSHIP_HARMONY_ASSOCIATION_ID)) {
                        StringList slList = new StringList();
                        String strTempObject = (mTempMap.get(SELECT_RELATIONSHIP_HARMONY_ASSOCIATION_ID)).toString();

                        if (strTempObject.contains("[")) {
                            strTempObject = strTempObject.substring(strTempObject.indexOf("[") + 1, strTempObject.indexOf("]"));
                        }

                        StringList slTempList = FrameworkUtil.split(strTempObject, ",");
                        for (int m = 0; m < slTempList.size(); m++) {
                            slList.add(((String) slTempList.get(m)).trim());
                        }

                        if (slList != null && !slList.isEmpty()) {
                            for (int j = 0; j < slList.size(); j++) {
                                DomainRelationship domRel = DomainRelationship.newInstance(context, (String) slList.get(j));
                                String strValue = (String) programMap.get(SELECT_ATTRIBUTE_UNITOF_MEASURE);

                                if (IS_UNIT_OF_MEASURE_PC.equalsIgnoreCase(strValue)) {
                                    String strQuantity = domRel.getAttributeValue(context, TigerConstants.ATTRIBUTE_QUANTITY);
                                    String strColorPID = domRel.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_COLORPID);
                                    if (UIUtil.isNotNullAndNotEmpty(strColorPID) && !strColorPID.equalsIgnoreCase("Ignore")) {
                                        StringList slTempQuantity = FrameworkUtil.split(strQuantity, ".");
                                        int sQuantity = Integer.parseInt((String) slTempQuantity.get(1));
                                        if (sQuantity != 0) {
                                            result = 1;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * Quality Check
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unused")
    private static int checkUOMOfParts(Context context, Map<?, ?> programMap) throws Exception {

        int result = 0;
        String strObjectId = (String) programMap.get(DomainConstants.SELECT_ID);
        String strRelId = (String) programMap.get(DomainConstants.SELECT_RELATIONSHIP_ID);

        DomainObject domMBOM = DomainObject.newInstance(context, strObjectId);
        StringList slSelects = new StringList();
        slSelects.add(SELECT_RELATIONSHIP_HARMONY_ASSOCIATION_TOID);
        slSelects.add(SELECT_RELATIONSHIP_HARMONY_ASSOCIATION_ID);

        StringBuilder stringBuilder = new StringBuilder("attribute[" + TigerConstants.ATTRIBUTE_PSS_COLORPID + "].value != \'\' && ");
        stringBuilder.append("attribute[" + TigerConstants.ATTRIBUTE_PSS_COLORPID + "].value != 'Ignore'");

        String strWhereExp = stringBuilder.toString();

        if (UIUtil.isNullOrEmpty(strRelId)) {
            MapList slHarmonyList = domMBOM.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_HARMONYASSOCIATION, // Relationship
                    // Pattern
                    TigerConstants.TYPE_PSS_HARMONY, // Object Pattern
                    new StringList(DomainConstants.SELECT_ID), // Object Selects
                    new StringList(DomainConstants.SELECT_RELATIONSHIP_ID), // Relationship Selects
                    false, // to direction
                    true, // from direction
                    (short) 0, // recursion level
                    null, // object where clause
                    strWhereExp, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    null, // Post Type Pattern
                    null, null, null);
            if (slHarmonyList != null && !slHarmonyList.isEmpty()) {
                for (int i = 0; i < slHarmonyList.size(); i++) {
                    Map<?, ?> mTempMap = (Map<?, ?>) slHarmonyList.get(i);

                    DomainRelationship domRel = DomainRelationship.newInstance(context, (String) mTempMap.get(DomainConstants.SELECT_RELATIONSHIP_ID));

                    String strValue = (String) programMap.get(SELECT_ATTRIBUTE_UNITOF_MEASURE);

                    if (!(strValue.equalsIgnoreCase(IS_UNIT_OF_MEASURE_PC))) {
                        String strQuantity = domRel.getAttributeValue(context, TigerConstants.ATTRIBUTE_QUANTITY);

                        double sQuantity = Double.parseDouble(strQuantity);

                        if (sQuantity == 1.0) {
                            result = 1;
                            break;
                        }

                    }

                }

            }
        } else {
            MapList slConnectedHarmonyList = DomainRelationship.getInfo(context, new String[] { strRelId }, slSelects);

            if (slConnectedHarmonyList != null && !slConnectedHarmonyList.isEmpty()) {
                for (int i = 0; i < slConnectedHarmonyList.size(); i++) {
                    Map<?, ?> mTempMap = (Map<?, ?>) slConnectedHarmonyList.get(i);

                    if (mTempMap.containsKey(SELECT_RELATIONSHIP_HARMONY_ASSOCIATION_ID)) {
                        StringList slList = new StringList();
                        String strTempObject = (mTempMap.get(SELECT_RELATIONSHIP_HARMONY_ASSOCIATION_ID)).toString();

                        if (strTempObject.contains("[")) {
                            strTempObject = strTempObject.substring(strTempObject.indexOf("[") + 1, strTempObject.indexOf("]"));
                        }

                        StringList slTempList = FrameworkUtil.split(strTempObject, ",");
                        for (int m = 0; m < slTempList.size(); m++) {
                            slList.add(((String) slTempList.get(m)).trim());
                        }

                        if (slList != null && !slList.isEmpty()) {
                            for (int j = 0; j < slList.size(); j++) {
                                DomainRelationship domRel = DomainRelationship.newInstance(context, (String) slList.get(j));
                                String strValue = (String) programMap.get(SELECT_ATTRIBUTE_UNITOF_MEASURE);
                                if (!(strValue.equalsIgnoreCase(IS_UNIT_OF_MEASURE_PC))) {
                                    String strQuantity = domRel.getAttributeValue(context, TigerConstants.ATTRIBUTE_QUANTITY);
                                    String strColorPID = domRel.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_COLORPID);
                                    if (UIUtil.isNotNullAndNotEmpty(strColorPID) && !strColorPID.equalsIgnoreCase("Ignore")) {
                                        double sQuantity = Double.parseDouble(strQuantity);
                                        if (sQuantity == 1.0) {
                                            result = 1;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * Check if the unit of measure of a Material is not PC
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unused")
    private static int checkUOMOfMaterial(Context context, Map<?, ?> programMap) throws Exception {

        int result = 0;
        String strValue = (String) programMap.get(SELECT_ATTRIBUTE_UNITOF_MEASURE);
        String strMBOMType = (String) programMap.get(DomainConstants.SELECT_TYPE);

        if (strValue.equalsIgnoreCase(IS_UNIT_OF_MEASURE_PC)) {
            if (TigerConstants.LIST_TYPE_MATERIALS.contains(strMBOMType)) {
                result = 1;
            }
        }

        return result;

    }

    /**
     * Check that the related engineering Part is not obsolete
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings({ "unused", "deprecation" })
    private static int checkParts(Context context, Map<?, ?> programMap) throws Exception {
        com.dassault_systemes.vplm.modeler.PLMCoreModelerSession plmSession = null;
        int result = 0;
        List<String> slIdList = new ArrayList<String>();
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = com.dassault_systemes.vplm.modeler.PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            String strObjectId = (String) programMap.get(DomainConstants.SELECT_ID);

            slIdList.add(strObjectId);
            if (!slIdList.isEmpty()) {
                List<String> psRefPIDList = FRCMBOMModelerUtility.getScopedPSReferencePIDFromList(context, plmSession, slIdList);

                if (psRefPIDList != null && !(psRefPIDList.isEmpty())) {
                    String sObjectID = null;
                    for (int i = 0; i < psRefPIDList.size(); i++) {
                        sObjectID = (String) psRefPIDList.get(i);
                        if (UIUtil.isNotNullAndNotEmpty(sObjectID) && UIUtil.isNotNullAndNotEmpty(strObjectId)) {
                            DomainObject domMBOM = DomainObject.newInstance(context, strObjectId);
                            String strMBOMPolicy = domMBOM.getInfo(context, DomainConstants.SELECT_POLICY);
                            String strMBOMCurrent = domMBOM.getInfo(context, DomainConstants.SELECT_CURRENT);
                            String strMBOMCancelled = PropertyUtil.getSchemaProperty(context, "policy", strMBOMPolicy, "state_Cancelled");
                            String strMBOMObsolete = PropertyUtil.getSchemaProperty(context, "policy", strMBOMPolicy, "state_Obsolete");
                            DomainObject domPS = DomainObject.newInstance(context, sObjectID);
                            String strPSCurrent = domPS.getInfo(context, DomainConstants.SELECT_CURRENT);
                            String strPolicy = domPS.getInfo(context, DomainConstants.SELECT_POLICY);
                            String strPSObsolete = PropertyUtil.getSchemaProperty(context, "policy", strPolicy, "state_OBSOLETE");
                            if (strMBOMCurrent.equalsIgnoreCase(strMBOMCancelled) || strMBOMCurrent.equalsIgnoreCase(strMBOMObsolete)) {
                                result = 1;
                                break;
                            } else if (strPSCurrent.equalsIgnoreCase(strPSObsolete)) {
                                result = 2;
                                break;
                            }
                        } else {
                            DomainObject domMBOM = DomainObject.newInstance(context, strObjectId);
                            String strMBOMPolicy = domMBOM.getInfo(context, DomainConstants.SELECT_POLICY);
                            String strMBOMCurrent = domMBOM.getInfo(context, DomainConstants.SELECT_CURRENT);
                            String strMBOMCancelled = PropertyUtil.getSchemaProperty(context, "policy", strMBOMPolicy, "state_Cancelled");
                            String strMBOMObsolete = PropertyUtil.getSchemaProperty(context, "policy", strMBOMPolicy, "state_Obsolete");
                            if (strMBOMCurrent.equalsIgnoreCase(strMBOMCancelled) || strMBOMCurrent.equalsIgnoreCase(strMBOMObsolete)) {
                                result = 1;
                                break;
                            }
                        }
                    }

                }
            }
            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);
        } catch (Exception e) {
            flushAndCloseSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw e;
        }
        return result;
    }

    /**
     * Check that a part which has no child should not be set to "MAKE"
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unused")
    private static int checkTypeOfPartForMake(Context context, Map<?, ?> programMap) throws Exception {
        int result = 0;

        // TIGTK-10176:Rutuja Ekatpure:27/9/2017:start
        String strObjId = (String) programMap.get(DomainConstants.SELECT_ID);
        String strTypeofPart = (String) programMap.get(SELECT_ATTRIBUTE_TYPE_OF_PART);
        DomainObject domObj = DomainObject.newInstance(context, strObjId);
        if (domObj.isKindOf(context, TigerConstants.TYPE_PROCESS_CONTINUOUS_CREATE_MATERIAL) || domObj.isKindOf(context, TigerConstants.TYPE_PROCESSCONTINUOUSPROVIDE)) {
            strTypeofPart = domObj.getInfo(context, "attribute[" + TigerConstants.ATTRIBUTE_PSS_MANUFACTURING_ITEMEXT_PSS_MAKEORBUYMATERIAL + "]");
        }
        if (UIUtil.isNotNullAndNotEmpty(strTypeofPart) && "MAKE".equalsIgnoreCase(strTypeofPart)) {
            // TIGTK-11293:Start
            StringList slConnectedObj = domObj.getInfoList(context, SELECT_RELATIONSHIP_DELFMIFUNCTIONIDENTIFIEDINSTANCE_TOID);
            StringList slConnectedObjects = generateStringList(slConnectedObj);
            StringList slConnectedMaterialObj = domObj.getInfoList(context, SELECT_RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS_TOID);
            // TIGTK-11293:End
            StringList slConnectedMaterialList = generateStringList(slConnectedMaterialObj);

            if (slConnectedObjects.isEmpty() && slConnectedMaterialList.isEmpty()) {
                result = 1;
            }
        }
        // TIGTK-10176:Rutuja Ekatpure:27/9/2017:End
        return result;

    }

    /**
     * Check that a BUY part should not have a Supplier Part Number empty
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unused")
    private static int checkTypeOfPartForBuy(Context context, Map<?, ?> programMap) throws Exception {

        int result = 0;
        String strRelId = (String) programMap.get(DomainConstants.SELECT_RELATIONSHIP_ID);

        if (UIUtil.isNotNullAndNotEmpty(strRelId)) {
            String strTypeofPart = (String) programMap.get(SELECT_ATTRIBUTE_TYPE_OF_PART);
            if ("BUY".equalsIgnoreCase(strTypeofPart)) {
                String strSupplierPartNumber = (String) programMap.get(SELECT_ATTRIBUTE_SUPPLIERS_PART_NUMBER);
                if (UIUtil.isNullOrEmpty(strSupplierPartNumber)) {
                    result = 2;// For Warning
                }
            }
        }

        return result;
    }

    /**
     * Quality Check
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unused")
    private static int checkCustomerPartNumberHarmonies(Context context, Map<?, ?> programMap) throws Exception {

        int result = 0;

        StringList slSelects = new StringList();
        slSelects.add(SELECT_RELATIONSHIP_HARMONY_ASSOCIATION_TOID);
        slSelects.add(SELECT_RELATIONSHIP_HARMONY_ASSOCIATION_ID);

        String strObjectId = (String) programMap.get(DomainConstants.SELECT_ID);
        DomainObject domMBOM = DomainObject.newInstance(context, strObjectId);
        String strRelId = (String) programMap.get(DomainConstants.SELECT_RELATIONSHIP_ID);
        StringBuilder stringBuilder = new StringBuilder("attribute[" + TigerConstants.ATTRIBUTE_PSS_COLORPID + "].value != \'\' && ");
        stringBuilder.append("attribute[" + TigerConstants.ATTRIBUTE_PSS_COLORPID + "].value != 'Ignore'");

        String strWhereExp = stringBuilder.toString();

        if (UIUtil.isNullOrEmpty(strRelId)) {
            StringList slCustomerPartNoList = new StringList();
            MapList slHarmonyList = domMBOM.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_HARMONYASSOCIATION, // Relationship Pattern
                    TigerConstants.TYPE_PSS_HARMONY, // Object Pattern
                    new StringList(DomainConstants.SELECT_ID), // Object Selects
                    new StringList(DomainConstants.SELECT_RELATIONSHIP_ID), // Relationship Selects
                    false, // to direction
                    true, // from direction
                    (short) 0, // recursion level
                    null, // object where clause
                    strWhereExp, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    null, // Post Type Pattern
                    null, null, null);
            if (slHarmonyList != null && !slHarmonyList.isEmpty()) {
                for (int i = 0; i < slHarmonyList.size(); i++) {
                    Map<?, ?> mTempMap = (Map<?, ?>) slHarmonyList.get(i);

                    DomainRelationship domRel = DomainRelationship.newInstance(context, (String) mTempMap.get(DomainConstants.SELECT_RELATIONSHIP_ID));
                    String strCustomerPartNumber = domRel.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CUSTOMERPARTNUMBER);
                    if (UIUtil.isNotNullAndNotEmpty(strCustomerPartNumber)) {
                        if (slCustomerPartNoList.contains(strCustomerPartNumber)) {
                            result = 1;
                            break;
                        } else {
                            slCustomerPartNoList.add(strCustomerPartNumber);
                        }
                    }
                }
            }
        } else {
            StringList slCustomerPartNoList = new StringList();
            MapList slConnectedHarmonyList = DomainRelationship.getInfo(context, new String[] { strRelId }, slSelects);
            if (slConnectedHarmonyList != null && !slConnectedHarmonyList.isEmpty()) {
                for (int i = 0; i < slConnectedHarmonyList.size(); i++) {
                    Map<?, ?> mTempMap = (Map<?, ?>) slConnectedHarmonyList.get(i);
                    if (mTempMap.containsKey(SELECT_RELATIONSHIP_HARMONY_ASSOCIATION_ID)) {
                        StringList slList = new StringList();
                        String strTempObject = (mTempMap.get(SELECT_RELATIONSHIP_HARMONY_ASSOCIATION_ID)).toString();
                        if (strTempObject.contains("[")) {
                            strTempObject = strTempObject.substring(strTempObject.indexOf("[") + 1, strTempObject.indexOf("]"));
                        }

                        StringList slTempList = FrameworkUtil.split(strTempObject, ",");
                        for (int m = 0; m < slTempList.size(); m++) {
                            slList.add(((String) slTempList.get(m)).trim());
                        }

                        if (slList != null && !slList.isEmpty()) {
                            for (int j = 0; j < slList.size(); j++) {
                                DomainRelationship domRel = DomainRelationship.newInstance(context, (String) slList.get(j));
                                String strCustomerPartNumber = domRel.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CUSTOMERPARTNUMBER);
                                String strColorPID = domRel.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_COLORPID);
                                if (UIUtil.isNotNullAndNotEmpty(strColorPID) && !strColorPID.equalsIgnoreCase("Ignore")) {
                                    if (UIUtil.isNotNullAndNotEmpty(strCustomerPartNumber)) {
                                        if (slCustomerPartNoList.contains(strCustomerPartNumber)) {
                                            result = 1;
                                            break;
                                        } else {
                                            slCustomerPartNoList.add(strCustomerPartNumber);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Quality Check
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unused")
    private static int checkAssignedColorForParent(Context context, Map<?, ?> programMap) throws Exception {

        int result = 0;
        String strRelId = (String) programMap.get(DomainConstants.SELECT_RELATIONSHIP_ID);
        if (UIUtil.isNotNullAndNotEmpty(strRelId)) {
            // TIGTK-11293:Start
            String strObjectId = (String) programMap.get("objectId");
            DomainObject domRootMBOM = DomainObject.newInstance(context, strObjectId);
            StringList slColorList = domRootMBOM.getInfoList(context, SELECT_RELATIONSHIP_PSS_COLOR_LIST_TOID);
            // StringList slColorList = generateStringList(slRootColorList);
            // TIGTK-11293:End
            if (slColorList != null && !(slColorList.isEmpty()) && !slColorList.contains(null)) {
                Object objParentList = programMap.get("from.id");
                StringList slParentList = generateStringList(objParentList);
                if (slParentList != null && !(slParentList.isEmpty()) && !slParentList.contains(null)) {
                    for (int i = 0; i < slParentList.size(); i++) {
                        String strParentObj = (String) slParentList.get(i);
                        DomainObject domParentMBOM = DomainObject.newInstance(context, strParentObj);
                        StringList slColorforParent = domParentMBOM.getInfoList(context, SELECT_RELATIONSHIP_PSS_COLOR_LIST_TOID);
                        if (slColorforParent == null || slColorforParent.isEmpty()) {
                            result = 1;
                            break;
                        }
                    }
                }
            }
        }

        return result;

    }

    /**
     * Quality Check
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unused")
    private static int validateDescription(Context context, Map<?, ?> programMap) throws Exception {

        String pattern = "^[a-zA-Z0-9\".,:;_\\s\\/-]*$";
        int result = 0;
        StringList slAttributeList = new StringList();
        slAttributeList.add(programMap.get(SELECT_ATTRIBUTE_FAURECIA_FULL_LENGTH_DESC));
        slAttributeList.add(programMap.get(SELECT_ATTRIBUTE_FAURECIA_SHORT_LENGTH_DESC));
        slAttributeList.add(programMap.get(SELECT_ATTRIBUTE_CUSTOMER_DESCRIPTION));
        slAttributeList.add(programMap.get(SELECT_ATTRIBUTE_ALTERNATE_DESCRIPTION));
        slAttributeList.add(programMap.get(SELECT_ATTRIBUTE_CUSTOMER_PARTNUMBER));
        slAttributeList.add(programMap.get(SELECT_ATTRIBUTE_SUPPLIERS_PART_NUMBER));

        if (slAttributeList != null && !(slAttributeList.isEmpty())) {
            for (int i = 0; i < slAttributeList.size(); i++) {
                String strContent = (String) slAttributeList.get(i);

                strContent = strContent.replaceAll("[\\[\\]\\\u0007]", "");
                if (UIUtil.isNotNullAndNotEmpty(strContent)) {
                    if (!(strContent.matches(pattern))) {
                        result = 1;
                        break;
                    }
                }
            }

        }

        return result;
    }

    /**
     * Quality Check
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unused")
    private static int checkPDMClassRAW(Context context, Map<?, ?> programMap) throws Exception {

        int result = 0;
        String strValue = (String) programMap.get(SELECT_ATTRIBUTE_PSS_PDMCLASS);

        String strType = (String) programMap.get(DomainConstants.SELECT_TYPE);
        if ("RAW".equals(strValue) && !TigerConstants.LIST_TYPE_MATERIALS.contains(strType)) {
            result = 1;
        }

        return result;
    }

    @SuppressWarnings({ "unused", "rawtypes", "deprecation" })
    private static int checkProductConfigurationDefination(Context context, Map map) throws Exception {
        com.dassault_systemes.vplm.modeler.PLMCoreModelerSession plmSession = null;
        int result = 0;

        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = com.dassault_systemes.vplm.modeler.PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();
            String strRootObjectId = (String) map.get("RootId");
            String[] strModelId = (String[]) FRCMBOMModelerUtility.getListOfContextModelPIDs(context, strRootObjectId);
            StringList slObjectSelect = new StringList();
            slObjectSelect.add(DomainConstants.SELECT_ID);
            slObjectSelect.add(DomainConstants.SELECT_NAME);
            for (int i = 0; i < strModelId.length; i++) {
                DomainObject dObjModel = new DomainObject(strModelId[i]);
                String strProductId = dObjModel.getInfo(context, SELECT_RELATIONSHIP_MAINPRODUCT_TOID);
                String strProdConfgObjId = DomainObject.EMPTY_STRING;
                DomainObject domProduct = new DomainObject(strProductId);
                MapList mlProductConfigurations = domProduct.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PRODUCT_CONFIGURATION, TigerConstants.TYPE_PRODUCTCONFIGURATION, false, true, 1,
                        slObjectSelect, DomainConstants.EMPTY_STRINGLIST, "", "", 0, "", "", null);

                ArrayList<Map<String, Object>> mlConfigurationOption = new ArrayList<Map<String, Object>>();
                Map<String, Object> mConfigurationOption = new HashMap<String, Object>();

                if (mlProductConfigurations.size() > 0) {
                    for (int j = 0; j < mlProductConfigurations.size(); j++) {
                        Map mObj = (Map) mlProductConfigurations.get(j);
                        strProdConfgObjId = (String) mObj.get(DomainConstants.SELECT_ID);
                        DomainObject dObjProductConfig = new DomainObject(strProdConfgObjId);
                        StringList slConfigurationOptionRelationships = dObjProductConfig.getInfoList(context, SELECT_SELECTED_CONFIGURATION_OPTIONS);
                        if (!slConfigurationOptionRelationships.isEmpty() && slConfigurationOptionRelationships.size() > 0) {
                            if (j == 0) {
                                // Add relationships to the MapList for the first product configuration
                                mConfigurationOption.put("SelectedValue", slConfigurationOptionRelationships);
                                mlConfigurationOption.add(mConfigurationOption);
                            } else {
                                for (int l = 0; l < mlConfigurationOption.size(); l++) {

                                    Map mCheckConfigurationOption = (Map) mlConfigurationOption.get(l);
                                    StringList slCheckConfigureOption = (StringList) mCheckConfigurationOption.get("SelectedValue");
                                    if (slCheckConfigureOption.equals(slConfigurationOptionRelationships)) {
                                        result = 1;
                                    }

                                }
                                Map<String, Object> mNewConfigurationOption = new HashMap<String, Object>();
                                mNewConfigurationOption.put("SelectedValue", slConfigurationOptionRelationships);
                                mlConfigurationOption.add(mNewConfigurationOption);
                            }
                        }
                    }
                }
            }

            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);

        } catch (Exception e) {
            flushAndCloseSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw e;
        }
        return result;
    }

    /**
     * Intermediate Method
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    private static StringList generateStringList(Object obj) {
        StringList strListFromObject = new StringList();
        if (obj != null && obj instanceof StringList) {
            strListFromObject = (StringList) obj;
        } else if (obj != null) {
            strListFromObject.add((String) obj);
        }
        return strListFromObject;
    }

    /**
     * Quality Check method to display Quality Check Description with results
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Vector getRulesWithResultHTML(Context context, String[] args) throws Exception {

        Vector vResult = new Vector();
        String strMethod;
        int result = 0;

        try {
            Map ProgramMap = JPO.unpackArgs(args);
            MapList mlObjectList = (MapList) ProgramMap.get("objectList");

            Map paramMap = (HashMap) ProgramMap.get("paramList");
            // TIGTK-8292 :Rutuja Ekatpure :6/6/2017:Start
            boolean isexport = false;
            String export = (String) paramMap.get("exportFormat");
            if (export != null) {
                isexport = true;
            }
            // TIGTK-8292 :Rutuja Ekatpure :6/6/2017:End
            String strRootObjectId = (String) paramMap.get("objectId");
            if (mlObjectList != null && !mlObjectList.isEmpty()) {
                for (int i = 0; i < mlObjectList.size(); i++) {
                    StringBuilder sb = new StringBuilder();
                    StringBuilder sbForExport = new StringBuilder();
                    Map mTempMap = (Map) mlObjectList.get(i);

                    String strRootNode = (String) mTempMap.get("Root Node");
                    String strObjectId = (String) mTempMap.get(DomainConstants.SELECT_ID);
                    DomainObject domMBOM = DomainObject.newInstance(context, strObjectId);
                    Map mParamMap = mTempMap;
                    mParamMap.put("RootId", strRootObjectId);
                    mParamMap.put("objectId", strObjectId);
                    if (strRootObjectId.equalsIgnoreCase(strObjectId)) {

                        String strUnitofMesure = domMBOM.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_UNIT_OF_MEASURE);
                        String strPDMClass = domMBOM.getAttributeValue(context, TigerConstants.ATTRIBUTE_PDM_CLASS);
                        String strUnitOfMeasureCategory = domMBOM.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_UNIT_OF_MEASURE_CATEGORY);
                        mParamMap.put(SELECT_ATTRIBUTE_UNITOF_MEASURE, strUnitofMesure);
                        mParamMap.put(SELECT_ATTRIBUTE_PSS_UNIT_OF_MEASURE_CATEGORY, strUnitOfMeasureCategory);
                        mParamMap.put(SELECT_ATTRIBUTE_PSS_PDMCLASS, strPDMClass);
                        mParamMap.put(SELECT_ATTRIBUTE_FAURECIA_FULL_LENGTH_DESC, domMBOM.getAttributeValue(context, TigerConstants.ATTRIBUTE_FAURECIA_FULL_LENGTH_DESC));
                        mParamMap.put(SELECT_ATTRIBUTE_FAURECIA_SHORT_LENGTH_DESC, domMBOM.getAttributeValue(context, TigerConstants.ATTRIBUTE_FAURECIA_SHORT_LENGTH_DESC));
                        mParamMap.put(SELECT_ATTRIBUTE_CUSTOMER_DESCRIPTION, domMBOM.getAttributeValue(context, TigerConstants.ATTRIBUTE_CUSTOMER_DESCRIPTION));
                        mParamMap.put(SELECT_ATTRIBUTE_ALTERNATE_DESCRIPTION, domMBOM.getAttributeValue(context, TigerConstants.ATTRIBUTE_ALTERNATE_DESCRIPTION));
                        mParamMap.put(SELECT_ATTRIBUTE_CUSTOMER_PARTNUMBER, domMBOM.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXT_PSS_CUSTOMER_PART_NUMBER));
                        mParamMap.put(SELECT_ATTRIBUTE_SUPPLIERS_PART_NUMBER, domMBOM.getAttributeValue(context, TigerConstants.ATTRIBUTE_SUPPLIERS_PART_NUMBER));
                    }
                    String strRules = EnoviaResourceBundle.getProperty(context, "PSS_FRCMBOMCentral.QualityCheck.Rules");

                    StringList slRulesList = FrameworkUtil.split(strRules, ",");

                    if (slRulesList != null && !slRulesList.isEmpty()) {

                        for (int j = 0; j < slRulesList.size(); j++) {
                            String strQualityRule = EnoviaResourceBundle.getProperty(context, "PSS_FRCMBOMCentral.QualityCheck.Rules." + ((String) slRulesList.get(j)).trim());
                            StringList slQualityCheckDescription = FrameworkUtil.split(strQualityRule, "|");

                            strMethod = ((String) slQualityCheckDescription.get(1)).trim();

                            Class[] paramsMap = new Class[2];
                            paramsMap[0] = Context.class;
                            paramsMap[1] = Map.class;
                            Object obj = Class.forName(PSS_FRCMBOMQualityCheck_mxJPO.class.getName()).newInstance();

                            Method m = Class.forName(PSS_FRCMBOMQualityCheck_mxJPO.class.getName()).getDeclaredMethod(strMethod, paramsMap);

                            result = (Integer) m.invoke(obj, context, mParamMap);
                            // TIGTK-8292 :Rutuja Ekatpure :6/6/2017:Start
                            if (result == 0) {
                                sb.append("<font color=\"36CE31\" style=\"font-weight: bold\"  >PASS</font><span style=\"padding-right:50px\"/>");
                                sbForExport.append("PASS");
                            } else if (result == 2) {
                                sb.append("<font color=\"EE7600\" style=\"font-weight: bold;\">WARNING</font><span style=\"padding-right:17px\"/>");
                                sbForExport.append("WARNING");
                            } else {
                                sb.append("<font color=\"FF0000\" style=\"font-weight: bold;\">FAIL</font><span style=\"padding-right:57px\"/>");
                                sbForExport.append("FAIL");
                            }

                            sb.append("\t");
                            sb.append(slQualityCheckDescription.get(0));
                            sb.append("<br/>");
                            sbForExport.append("  ");
                            sbForExport.append(slQualityCheckDescription.get(0));
                            sbForExport.append("\n");
                        }
                        if (UIUtil.isNotNullAndNotEmpty(strRootNode) && UIUtil.isNotNullAndNotEmpty(strRootObjectId)) {
                            String strRuleForRootNodeDescription = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentral", context.getLocale(),
                                    "PSS_FRCMBOMCentral.QualityCheck.Rules.RootNodeHarmonyRule");
                            result = checkColorOnHarmoniesForAssembly(context, strRootObjectId);

                            if (result == 0) {
                                sb.append("<font color=\"36CE31\" style=\"font-weight: bold\"  >PASS</font><span style=\"padding-right:50px\"/>");
                                sbForExport.append("PASS");
                            } else if (result == 2) {
                                sb.append("<font color=\"EE7600\" style=\"font-weight: bold;\">WARNING</font><span style=\"padding-right:17px\"/>");
                                sbForExport.append("WARNING");
                            } else if (result == 3) {
                                sb.append("<font color=\"36CE31\" style=\"font-weight: bold\"  >PASS</font><span style=\"padding-right:50px\"/>");
                                sbForExport.append("PASS");
                            } else {
                                sb.append("<font color=\"FF0000\" style=\"font-weight: bold;\">FAIL</font><span style=\"padding-right:57px\"/>");
                                sbForExport.append("FAIL");
                            }

                            sb.append("\t");
                            sb.append(strRuleForRootNodeDescription);
                            sb.append("<br/>");
                            sbForExport.append("  ");
                            sbForExport.append(strRuleForRootNodeDescription);
                            sbForExport.append("\n");

                            if (result == 3) {
                                String strWarningMsg = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentral", context.getLocale(), "PSS_FRCMBOMCentral.QualityCheck.Rules.NoHarmonyConfigured");
                                sb.append("<font color=\"FF0000\" style=\"font-weight: bold;\">" + strWarningMsg + "</font><span style=\"padding-right:57px\"/>");
                                sbForExport.append(strWarningMsg);
                            }
                        }
                        if (isexport) {
                            vResult.add(sbForExport.toString());
                        } else {
                            vResult.add(sb.toString());
                        }
                        // TIGTK-8292 :Rutuja Ekatpure :6/6/2017:End
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error in getRulesWithResultHTML()\n", e);
            throw e;
        }

        return vResult;
    }

    /**
     * Quality Check
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unused")
    private static int checkPDMClassINTorSMF(Context context, Map<?, ?> programMap) throws Exception {
        int result = 0;
        int flag = 0;
        short level = 0;
        try {
            String strObjectId = (String) programMap.get(DomainConstants.SELECT_ID);
            String strValue = (String) programMap.get(SELECT_ATTRIBUTE_PSS_PDMCLASS);

            if ("INT".equals(strValue)) {

                MapList mlMBOMStructureList = PSS_FRCMBOMProg_mxJPO.getExpandMBOM(context, strObjectId, 0, null, null, null, EXPD_REL_SELECT, EXPD_BUS_SELECT);

                // Added : 07/11/2016
                if (mlMBOMStructureList != null && !(mlMBOMStructureList.isEmpty())) {
                    Iterator<?> itr = mlMBOMStructureList.iterator();
                    // Added : 07/11/2016
                    while (itr.hasNext()) {
                        Map<?, ?> mTempMap = (Map<?, ?>) itr.next();

                        String strType = (String) mTempMap.get(DomainConstants.SELECT_TYPE);

                        if (!TigerConstants.LIST_TYPE_MATERIALS.contains(strType)) {
                            result = 1;

                        } else {
                            flag = 1;
                        }

                    }
                } else {
                    result = 1;
                }
            }
            if (flag == 1) {
                result = 0;
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in checkPDMClassINTorSMF: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
        return result;
    }

    /**
     * Quality Check
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unused")
    private static int checkPDMClassSMF(Context context, Map<?, ?> programMap) throws Exception {

        int result = 0;
        int flag = 0;
        short level = 0;
        try {
            String strObjectId = (String) programMap.get(DomainConstants.SELECT_ID);
            String strValue = (String) programMap.get(SELECT_ATTRIBUTE_PSS_PDMCLASS);

            if ("SMF".equals(strValue)) {

                MapList mlMBOMStructureList = PSS_FRCMBOMProg_mxJPO.getExpandMBOM(context, strObjectId, 0, null, null, null, EXPD_REL_SELECT, EXPD_BUS_SELECT);
                // Added : 07/11/2016
                if (mlMBOMStructureList != null && !(mlMBOMStructureList.isEmpty())) {
                    Iterator<?> itr = mlMBOMStructureList.iterator();
                    // Added : 07/11/2016
                    while (itr.hasNext()) {
                        Map<?, ?> mTempMap = (Map<?, ?>) itr.next();

                        String strType = (String) mTempMap.get(DomainConstants.SELECT_TYPE);
                        // TIGTK-8290:Rutuja Ekatpure:5/6/2017:start
                        if (TigerConstants.LIST_TYPE_MATERIALS.contains(strType)) {
                            result = 1;
                        } else {
                            flag = 1;
                        }
                        // TIGTK-8290:Rutuja Ekatpure:5/6/2017:End
                    }
                } else {
                    result = 1;
                }
            }
            if (flag == 1) {
                result = 0;
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in checkPDMClassSMF: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
        return result;
    }

    /**
     * Quality Check
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings({ "unused", "deprecation" })
    private static int checkProductConfigurationEffectivity(Context context, Map<?, ?> programMap) throws Exception {

        com.dassault_systemes.vplm.modeler.PLMCoreModelerSession plmSession = null;
        boolean activeTransaction = false;
        try {
            ContextUtil.startTransaction(context, true);
            activeTransaction = true;
            short level = (short) 1;

            context.setApplication("VPLM");
            plmSession = com.dassault_systemes.vplm.modeler.PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            String strObjectId = (String) programMap.get(DomainConstants.SELECT_ID);
            String strRelId = (String) programMap.get(DomainConstants.SELECT_RELATIONSHIP_ID);
            String strRootObjectId = (String) programMap.get("RootId");

            if (UIUtil.isNotNullAndNotEmpty(strRelId)) {

                String parentObject = (String) programMap.get("from.id");

                String[] strModelId = (String[]) FRCMBOMModelerUtility.getListOfContextModelPIDs(context, strRootObjectId);

                StringList slObjectSelect = new StringList();
                slObjectSelect.add(DomainConstants.SELECT_ID);
                slObjectSelect.add(DomainConstants.SELECT_NAME);
                if (strModelId.length > 0) {
                    for (int i = 0; i < strModelId.length; i++) {
                        DomainObject dObjModel = new DomainObject(strModelId[i]);
                        String strProductId = dObjModel.getInfo(context, SELECT_RELATIONSHIP_MAINPRODUCT_TOID);

                        String strProdConfgObjId = "";

                        DomainObject domProduct = new DomainObject(strProductId);
                        MapList mlProductConfigurations = domProduct.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PRODUCT_CONFIGURATION, TigerConstants.TYPE_PRODUCTCONFIGURATION, false,
                                true, 1, slObjectSelect, DomainConstants.EMPTY_STRINGLIST, "", "", 0, "", "", null);
                        StringList busSelect = new StringList();
                        busSelect.add("physicalid");

                        StringList relSelect = new StringList();
                        relSelect.add("physicalid[connection]");
                        relSelect.add("attribute[PLMInstance.V_TreeOrder].value");
                        relSelect.add("attribute[PSS_PublishedEBOM.PSS_InstanceName]");
                        if (mlProductConfigurations.size() > 0) {
                            for (int j = 0; j < mlProductConfigurations.size(); j++) {

                                Map<?, ?> mObj = (Map<?, ?>) mlProductConfigurations.get(j);
                                strProdConfgObjId = (String) mObj.get(DomainConstants.SELECT_ID);
                                // DomainObject dObjProductConfig = new DomainObject(strProdConfgObjId);

                                MapList psInstList = PSS_FRCMBOMModelerUtility_mxJPO.getVPMStructureMQL(context, parentObject, busSelect, relSelect, (short) 1, strProdConfgObjId);
                                for (int k = 0; k < psInstList.size(); k++) {
                                    Map<?, ?> mPhysicalId = (Map<?, ?>) psInstList.get(k);
                                    String strPhysicalId = (String) mPhysicalId.get("physicalid");
                                    if (strPhysicalId.equals(strObjectId)) {
                                        return 0;
                                    }
                                }

                            }
                        } else {
                            return 0;
                        }
                    }
                } else {
                    return 0;
                }

            } else {
                return 0;
            }
            flushAndCloseSession(plmSession);
            if (activeTransaction) {
                ContextUtil.commitTransaction(context);
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in checkProductConfigurationEffectivity: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            flushAndCloseSession(plmSession);
            if (activeTransaction) {
                ContextUtil.abortTransaction(context);
            }
        }
        return 1;
    }

    /**
     * Quality Check
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public static MapList getExpandMBOMForQualityCheck(Context context, String[] args) throws Exception {
        try {
            Map<?, ?> paramMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            MapList mlExpandedList = getExpandMBOMForQualityCheck(context, paramMap);
            return mlExpandedList;
        } catch (Exception e) {
            logger.error("Error in getExpandMBOMForQualityCheck()\n", e);
            throw e;
        }

    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static MapList getExpandMBOMForQualityCheck(Context context, Map<?, ?> paramMap) throws Exception {

        // long startTime;
        MapList mlExpandedList = new MapList();
        ContextUtil.startTransaction(context, false);
        String objectId = null;
        try {
            final String TYPE_OPERATION = PropertyUtil.getSchemaProperty(context, "type_PSS_Operation");
            final String TYPE_LINE_DATA = PropertyUtil.getSchemaProperty(context, "type_PSS_LineData");

            objectId = (String) paramMap.get("objectId");
            String expandLevel = (String) paramMap.get("expandLevel");
            if (UIUtil.isNullOrEmpty(expandLevel)) {
                expandLevel = (String) paramMap.get("compareLevel");
            }

            /*
             * // Add configuration filter String filterExpression = (String) paramMap.get("FRCExpressionFilterInput_OID"); String filterValue = (String)
             * paramMap.get("FRCExpressionFilterInput_actualValue"); String filterInput = (String) paramMap.get("FRCExpressionFilterInput");
             */

            short expLvl = 0;// Default to Expand All = 0
            if (!"All".equals(expandLevel))
                expLvl = Short.parseShort(expandLevel);

            // Call Expand
            // TIGTK-11293:Start
            StringList EXPD_BUS_SELECT = new StringList(new String[] { "physicalid", "logicalid", SELECT_ATTRIBUTE_SUPPLIERS_PART_NUMBER, SELECT_ATTRIBUTE_FAURECIA_FULL_LENGTH_DESC,
                    SELECT_ATTRIBUTE_FAURECIA_SHORT_LENGTH_DESC, SELECT_ATTRIBUTE_CUSTOMER_DESCRIPTION, SELECT_ATTRIBUTE_ALTERNATE_DESCRIPTION, SELECT_ATTRIBUTE_CUSTOMER_PARTNUMBER,
                    SELECT_ATTRIBUTE_PSS_PDMCLASS, SELECT_ATTRIBUTE_UNITOF_MEASURE, SELECT_ATTRIBUTE_PSS_UNIT_OF_MEASURE_CATEGORY });

            StringList EXPD_REL_SELECT = new StringList(
                    new String[] { "physicalid[connection]", "logicalid[connection]", "from.physicalid", SELECT_ATTRIBUTE_PLMINSTANCE_V_TREEORDER, SELECT_ATTRIBUTE_TYPE_OF_PART, "from.id" });

            // MapList res = PSS_FRCMBOMProg_mxJPO.getExpandMBOM(context, objectId, expLvl, filterExpression, filterValue, filterInput, EXPD_REL_SELECT, EXPD_BUS_SELECT);
            // startTime = System.currentTimeMillis();

            String strPCId = (String) paramMap.get("FRCExpressionFilterInput_OID");
            MapList res = PSS_FRCMBOMProg_mxJPO.getExpandMBOMonPC(context, objectId, expLvl, strPCId, EXPD_REL_SELECT, EXPD_BUS_SELECT);
            // TIGTK-11293:End

            // START UM5c06 : Build Paths and save theses in the return maps
            HashMap<String, String> mapPaths = new HashMap<String, String>();// Store
            // path in a Map to be able to manage unsorted return MapList

            DomainObject domObj = new DomainObject(objectId);
            String rootPID = domObj.getInfo(context, "physicalid");
            mapPaths.put(rootPID, rootPID);

            // Declare variable before to improve prefs
            Map<String, Object> mapObj;
            String objPID, objFromPID, objPIDConnection;
            String newPath = "";
            for (int i = 0; i < res.size(); i++) {
                mapObj = (Map) res.get(i);
                objPID = (String) mapObj.get("physicalid");
                objPIDConnection = (String) mapObj.get("physicalid[connection]");
                objFromPID = (String) mapObj.get("from.physicalid");
                newPath = mapPaths.get(objFromPID);
                if (newPath != null && !newPath.isEmpty()) {
                    newPath = newPath + "/" + objPIDConnection;
                } else {
                    newPath = objPID;
                }
                mapPaths.put(objPID, newPath);
                mapObj.put("pathPID", newPath);

                // set ids to use physicalids
                mapObj.put("id", objPID);
                mapObj.put("id[connection]", objPIDConnection);

                // Add hasChildren info here
                if ((i + 1) < res.size()) {
                    Map<?, ?> nextMapObj = (Map<?, ?>) res.get(i + 1);
                    String strLevelNow = (String) mapObj.get("level");
                    String strLevelNext = (String) nextMapObj.get("level");
                    int levelNow = Integer.parseInt(strLevelNow);
                    int levelNext = Integer.parseInt(strLevelNext);
                    if (levelNext > levelNow) {
                        mapObj.put("hasChildren", "true");
                    } else {
                        mapObj.put("hasChildren", "false");
                    }
                } else {
                    // Last object so no childrens
                    mapObj.put("hasChildren", "false");
                }
            }
            // END UM5c06 : Build Paths and save theses in the return maps

            // Sort by TreeOrder "attribute[PLMInstance.V_TreeOrder].value"
            res.sortStructure("attribute[PLMInstance.V_TreeOrder].value", "ascending", "real");

            for (int k = 0; k < res.size(); k++) {
                Map<?, ?> mStructureMap = (Map<?, ?>) res.get(k);
                String strType = (String) mStructureMap.get(DomainConstants.SELECT_TYPE);
                if (!(strType.equalsIgnoreCase(TYPE_OPERATION)) && !(strType.equalsIgnoreCase(TYPE_LINE_DATA))) {
                    mlExpandedList.add(mStructureMap);
                }
            }

            ContextUtil.commitTransaction(context);
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getExpandMBOMForQualityCheck()\n", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            ContextUtil.abortTransaction(context);
            throw e;
        }
        // long endTime = System.currentTimeMillis();
        return mlExpandedList;
    }

    /**
     * Check that Faurecia Short Length Description is filled
     * @param context
     * @param map
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unused")
    private static int checkFaureciaShortLengthDescription(Context context, Map<?, ?> map) throws Exception {
        String strAttributeValue = (String) map.get(SELECT_ATTRIBUTE_FAURECIA_SHORT_LENGTH_DESC);
        if (UIUtil.isNullOrEmpty(strAttributeValue))
            return 1;
        return 0;
    }

    /**
     * Check that Unit of Measure is empty or Not
     * @param context
     * @param map
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unused")
    private static int checkUnitOfMeasure(Context context, Map<?, ?> map) throws Exception {
        String strAttributeValue = (String) map.get(SELECT_ATTRIBUTE_PSS_UNIT_OF_MEASURE);
        if (UIUtil.isNullOrEmpty(strAttributeValue) || strAttributeValue.equalsIgnoreCase("-"))
            return 1;
        return 0;
    }

    /**
     * Check if the Quality Checker Pass or Failed
     * @plm.usage JSP: FPDM_GenerateMBOM100PreProcess.jsp
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            contains MBOM object ID
     * @return true or false
     * @throws Exception
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Boolean checkQualityChecker(Context context, String[] args) throws Exception {
        try {
            String strRootObjectId = args[0];
            logger.debug("checkQualityChecker() - strRootObjectId = <" + strRootObjectId + ">");

            // get MBOM structure
            Map paramMap = new HashMap();
            paramMap.put(DomainConstants.SELECT_ID, strRootObjectId);
            paramMap.put(DomainConstants.SELECT_LEVEL, "0");
            paramMap.put("objectId", strRootObjectId);
            paramMap.put("expandLevel", "All");

            MapList mlObjectList = getExpandMBOMForQualityCheck(context, paramMap);
            logger.debug("checkQualityChecker() - mlObjectList = <" + mlObjectList + ">");

            // add Root Node
            mlObjectList.add(0, paramMap);

            if (mlObjectList != null && !mlObjectList.isEmpty()) {
                String strRules = EnoviaResourceBundle.getProperty(context, "PSS_FRCMBOMCentral.QualityCheck.Rules");
                logger.debug("checkQualityChecker() - strRules = <" + strRules + ">");
                StringList slRulesList = FrameworkUtil.split(strRules, ",");

                DomainObject domMBOM = DomainObject.newInstance(context);
                String strObjectId = null;
                Map mMBOM = null;
                for (int i = 0; i < mlObjectList.size(); i++) {
                    mMBOM = (Map<?, ?>) mlObjectList.get(i);
                    strObjectId = (String) mMBOM.get(DomainConstants.SELECT_ID);
                    domMBOM.setId(strObjectId);
                    mMBOM.put("RootId", strRootObjectId);
                    mMBOM.put("objectId", strObjectId);
                    if (strRootObjectId.equalsIgnoreCase(strObjectId)) {

                        String strUnitofMesure = domMBOM.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_UNIT_OF_MEASURE);
                        String strPDMClass = domMBOM.getAttributeValue(context, TigerConstants.ATTRIBUTE_PDM_CLASS);
                        String strUnitOfMeasureCategory = domMBOM.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_UNIT_OF_MEASURE_CATEGORY);
                        mMBOM.put(SELECT_ATTRIBUTE_UNITOF_MEASURE, strUnitofMesure);
                        mMBOM.put(SELECT_ATTRIBUTE_PSS_UNIT_OF_MEASURE_CATEGORY, strUnitOfMeasureCategory);
                        mMBOM.put(SELECT_ATTRIBUTE_PSS_PDMCLASS, strPDMClass);
                        mMBOM.put(SELECT_ATTRIBUTE_FAURECIA_FULL_LENGTH_DESC, domMBOM.getAttributeValue(context, TigerConstants.ATTRIBUTE_FAURECIA_FULL_LENGTH_DESC));
                        mMBOM.put(SELECT_ATTRIBUTE_FAURECIA_SHORT_LENGTH_DESC, domMBOM.getAttributeValue(context, TigerConstants.ATTRIBUTE_FAURECIA_SHORT_LENGTH_DESC));
                        mMBOM.put(SELECT_ATTRIBUTE_CUSTOMER_DESCRIPTION, domMBOM.getAttributeValue(context, TigerConstants.ATTRIBUTE_CUSTOMER_DESCRIPTION));
                        mMBOM.put(SELECT_ATTRIBUTE_ALTERNATE_DESCRIPTION, domMBOM.getAttributeValue(context, TigerConstants.ATTRIBUTE_ALTERNATE_DESCRIPTION));
                        mMBOM.put(SELECT_ATTRIBUTE_CUSTOMER_PARTNUMBER, domMBOM.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXT_PSS_CUSTOMER_PART_NUMBER));
                        mMBOM.put(SELECT_ATTRIBUTE_SUPPLIERS_PART_NUMBER, domMBOM.getAttributeValue(context, TigerConstants.ATTRIBUTE_SUPPLIERS_PART_NUMBER));
                    }
                    if (slRulesList != null && !slRulesList.isEmpty()) {
                        String strQualityRule = null;
                        StringList slQualityCheckDescription = null;
                        String strMethod = null;
                        Class[] paramsMap = null;
                        Object obj = null;
                        Method m = null;
                        int result = 1;
                        for (int j = 0; j < slRulesList.size(); j++) {
                            strQualityRule = EnoviaResourceBundle.getProperty(context, "PSS_FRCMBOMCentral.QualityCheck.Rules." + ((String) slRulesList.get(j)).trim());
                            slQualityCheckDescription = FrameworkUtil.split(strQualityRule, "|");
                            strMethod = ((String) slQualityCheckDescription.get(1)).trim();

                            paramsMap = new Class[2];
                            paramsMap[0] = Context.class;
                            paramsMap[1] = Map.class;
                            obj = Class.forName(PSS_FRCMBOMQualityCheck_mxJPO.class.getName()).newInstance();

                            m = Class.forName(PSS_FRCMBOMQualityCheck_mxJPO.class.getName()).getDeclaredMethod(strMethod, paramsMap);
                            result = (Integer) m.invoke(obj, context, mMBOM);
                            logger.debug("checkQualityChecker() - strMethod = <" + strMethod + "> result = <" + result + ">");

                            if (result == 1) {
                                return false;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error in checkQualityChecker()\n", e);
            throw e;
        }
        return true;
    }

    /**
     * @param context
     * @param strRootPID
     * @return
     * @throws Exception
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static int checkColorOnHarmoniesForAssembly(Context context, String strRootPID) throws Exception {
        int result = 0;
        try {
            int count = 0;
            boolean flag = false;
            boolean flagToCheckBasedOnPC = false;
            String strNoHarmonyName = "NoHarmony";
            List<String> lsProductConfiguration = new ArrayList<String>();
            lsProductConfiguration.add(DomainConstants.EMPTY_STRING);

            String[] strModelId = (String[]) FRCMBOMModelerUtility.getListOfContextModelPIDs(context, strRootPID);
            StringList slObjectSelect = new StringList();
            slObjectSelect.add(DomainConstants.SELECT_ID);
            slObjectSelect.add("physicalid");
            slObjectSelect.add(DomainConstants.SELECT_NAME);

            for (int i = 0; i < strModelId.length; i++) {
                DomainObject dObjModel = new DomainObject(strModelId[i]);
                String strProductId = dObjModel.getInfo(context, SELECT_RELATIONSHIP_MAINPRODUCT_TOID);
                String strProdConfgObjId = DomainConstants.EMPTY_STRING;
                DomainObject domProduct = new DomainObject(strProductId);
                MapList mlProductConfigurations = domProduct.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PRODUCT_CONFIGURATION, TigerConstants.TYPE_PRODUCTCONFIGURATION, false, true, 1,
                        slObjectSelect, DomainConstants.EMPTY_STRINGLIST, "", "", 0, "", "", null);

                if (mlProductConfigurations.size() > 0) {
                    for (int j = 0; j < mlProductConfigurations.size(); j++) {
                        Map mObj = (Map) mlProductConfigurations.get(j);
                        strProdConfgObjId = (String) mObj.get("physicalid");
                        lsProductConfiguration.add(strProdConfgObjId);
                    }
                }
            }

            DomainObject domMBOM = DomainObject.newInstance(context, strRootPID);

            StringList busSelects = new StringList();
            busSelects.add(DomainConstants.SELECT_NAME);
            busSelects.add(DomainConstants.SELECT_ID);
            MapList mlNoHarmony = DomainObject.findObjects(context, TigerConstants.TYPE_PSS_HARMONY, // Type Pattern
                    strNoHarmonyName, // Name Pattern
                    "Default", // Rev Pattern
                    null, // Owner Pattern
                    TigerConstants.VAULT_ESERVICEPRODUCTION, // Vault Pattern
                    null, // Where Expression
                    false, // Expand Type
                    busSelects); // Object Pattern
            Map mHarmonyMapNoHarmony = null;

            MapList mlHarmonyList = domMBOM.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSSMBOM_HARMONIES, // relationship pattern
                    TigerConstants.TYPE_PSS_HARMONY, // object pattern
                    new StringList(DomainConstants.SELECT_ID), // object selects
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
            if (mlNoHarmony != null && !(mlNoHarmony.isEmpty())) {
                mHarmonyMapNoHarmony = (Map) mlNoHarmony.get(0);
                mlHarmonyList.add(0, mHarmonyMapNoHarmony);
            }
            MapList finalMBOMList = new MapList();
            StringList slPlantAttatchedtoRoot = new StringList();
            String strAttachedPlant = pss.mbom.MBOMUtil_mxJPO.getMasterPlant(context, strRootPID);
            if (UIUtil.isNotNullAndNotEmpty(strAttachedPlant)) {
                slPlantAttatchedtoRoot.add(strAttachedPlant);
            }
            StringList busSelect = new StringList();
            busSelect.add("physicalid");
            StringList relSelect = new StringList();
            relSelect.add("physicalid[connection]");
            // MapList mlExpandedMBOM = new MapList();
            for (int z = 0; z < lsProductConfiguration.size(); z++) {
                String strPCId = lsProductConfiguration.get(z);
                MapList mlExpandedMBOM = PSS_FRCMBOMModelerUtility_mxJPO.getVPMStructureMQL(context, strRootPID, busSelect, relSelect, (short) 0, strPCId);
                // mlExpandedMBOM = PSS_FRCMBOMProg_mxJPO.getExpandMBOM(context, strRootPID, 0, null, null, null, relSelect, busSelect);
                // MapList mlExpandedMBOM = PSS_FRCMBOMModelerUtility_mxJPO.getVPMStructureMQL(context, strMbomId, slObjSelect, slRelSelect, (short) 0,
                // strProductConfiguration);//PSS_FRCMBOMProg_mxJPO.getExpandMBOM(context, strRootPID, 0, null, null, null, relSelect, busSelect);
                if (mlExpandedMBOM != null && !(mlExpandedMBOM.isEmpty())) {
                    int skipedLevel = 0;
                    for (int i = 0; i < mlExpandedMBOM.size(); i++) {
                        Map mStructureMap = (Map) mlExpandedMBOM.get(i);

                        String sPlantPID = pss.mbom.MBOMUtil_mxJPO.getMasterPlant(context, (String) mStructureMap.get("physicalid"));
                        String level = (String) mStructureMap.get("level");
                        int levelInt = Integer.parseInt(level);
                        String strType = (String) mStructureMap.get(DomainConstants.SELECT_TYPE);

                        if (skipedLevel != 0 && skipedLevel < levelInt) {
                            continue;
                        } else if (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_OPERATION) || strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_LINEDATA)) {
                            skipedLevel = levelInt;
                        } else if (!slPlantAttatchedtoRoot.contains(sPlantPID)) {
                            skipedLevel = levelInt;
                            finalMBOMList.add(mStructureMap);
                        } else {
                            skipedLevel = 0;
                            finalMBOMList.add(mStructureMap);
                        }
                    }
                }
                List<String> lConnectionIds = new ArrayList<String>();
                for (int i = 0; i < finalMBOMList.size(); i++) {
                    Map mapObj = (Map) finalMBOMList.get(i);
                    String objPIDConnection = (String) mapObj.get("physicalid[connection]");
                    if (!lConnectionIds.contains(objPIDConnection))
                        lConnectionIds.add(objPIDConnection);
                }
                lConnectionIds.add(strRootPID);

                for (int j = 0; j < mlHarmonyList.size(); j++) {
                    count = 0;
                    Map mapHarmonyObj = (Map) mlHarmonyList.get(j);
                    String strHarmonyId = (String) mapHarmonyObj.get(DomainConstants.SELECT_ID);
                    String strMBOMConnectionId = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump $3", strHarmonyId, "to[" + TigerConstants.RELATIONSHIP_PSS_HARMONYASSOCIATION
                            + " | attribute[PSS_ColorPID]!='' && attribute[PSS_ProductConfigurationPID]=='" + strPCId + "'].fromrel.physicalid", "|");
                    String strRootMBOMId = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump $3", strHarmonyId,
                            "to[" + TigerConstants.RELATIONSHIP_PSS_HARMONYASSOCIATION + " | attribute[PSS_ColorPID]!='' && attribute[PSS_ProductConfigurationPID]=='" + strPCId + "'].from.physicalid",
                            "|");
                    if (UIUtil.isNotNullAndNotEmpty(strMBOMConnectionId)) {
                        String[] mbomConnectionIds = strMBOMConnectionId.split("\\|");
                        for (int k = 0; k < mbomConnectionIds.length; k++) {
                            if (lConnectionIds.contains(mbomConnectionIds[k])) {
                                count++;
                            }
                        }
                    }
                    if (UIUtil.isNotNullAndNotEmpty(strRootMBOMId)) {
                        String[] rootMBOMConnectionIds = strRootMBOMId.split("\\|");
                        for (int l = 0; l < rootMBOMConnectionIds.length; l++) {
                            if (lConnectionIds.contains(rootMBOMConnectionIds[l])) {
                                count++;
                            }
                        }
                    }
                    if (count > 0 && count < lConnectionIds.size()) {
                        flag = true;
                        break;
                    } else if (count == lConnectionIds.size()) {
                        flagToCheckBasedOnPC = true;
                        result = 0;
                    }
                    // count=0;

                    if (count == 0 && !flagToCheckBasedOnPC) {
                        result = 3;
                    }

                }
                finalMBOMList.clear();
            }
            if (flag) {
                result = 1;
            }
        } catch (Exception e) {
            logger.error("Error in checkColorOnHarmoniesForAssembly()\n", e);
            throw e;
        }
        return result;
    }

    // TIGTK-10578:MBOM-UE:Start
    /**
     * Check that Unit of Measure value is correct or not.
     * @param context
     * @param map
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unused")
    private static int checkUnitOfMeasureValue(Context context, Map<?, ?> map) throws Exception {
        final String PROPERTIES_KEY = "PSS_FRCMBOMCentral.UnitOfMeasure.Categorty.";
        try {
            String strAttributeValue = (String) map.get(SELECT_ATTRIBUTE_PSS_UNIT_OF_MEASURE);
            // TIGTK-11662:RE:30/11/2017:Start
            String strUnitOfMeasureCategory = (String) map.get(SELECT_ATTRIBUTE_PSS_UNIT_OF_MEASURE_CATEGORY);
            // TIGTK-11662:RE:30/11/2017:End
            String query = PROPERTIES_KEY + strUnitOfMeasureCategory;
            String strUnitOfMeasures = EnoviaResourceBundle.getProperty(context, TigerConstants.PROPERTIES_EMXFRCMBOMCENTRAL, context.getLocale(), query);
            StringList slUnitOfMeasuresList = FrameworkUtil.split(strUnitOfMeasures, "|");
            if (slUnitOfMeasuresList.contains(strAttributeValue) || strAttributeValue.equals("-")) {
                return 0;
            }
        } catch (Exception e) {
            logger.error("Error in checkUnitOfMeasureValue()\n", e);
            throw e;
        }
        return 1;
    }

    // TIGTK-10578:MBOM-UE:End
    // TIGTK-10661:MBOM-UE:Start
    /**
     * Verify that Variant ID on MBOM are consistant with its related Published EBOM variant IDs
     * @param context
     * @param map
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unused")
    private static int checkVariantIdConsistancy(Context context, Map<?, ?> map) throws Exception {
        int iResult = 0;
        List<String> slIdList = new ArrayList<String>();
        StringList slVIDsofMBOM = new StringList();
        StringList slVIDsofEBOM = new StringList();
        PLMCoreModelerSession plmSession = null;
        try {
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();
            String strObjectId = (String) map.get("objectId");

            DomainObject domMBOM = DomainObject.newInstance(context, strObjectId);
            StringList slVariantIdofMBOM = domMBOM.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_PARTVARIANTASSEMBLY + "].to.id");

            for (int i = 0; i < slVariantIdofMBOM.size(); i++) {
                String strVIDMBOM = (String) slVariantIdofMBOM.get(i);
                DomainObject domVIdMBOM = DomainObject.newInstance(context, strVIDMBOM);
                String strAttributeValueonMBOM = domVIdMBOM.getInfo(context, "attribute[" + TigerConstants.ATTRIBUTE_PSS_VARIANTID + "].value");
                slVIDsofMBOM.add(strAttributeValueonMBOM);
            }

            slIdList.add(strObjectId);
            List<String> psRefPIDList = FRCMBOMModelerUtility.getScopedPSReferencePIDFromList(context, plmSession, slIdList);
            if (!psRefPIDList.isEmpty()) {
                String strPublishEBOM = psRefPIDList.get(0);
                if (UIUtil.isNotNullAndNotEmpty(strPublishEBOM)) {
                    DomainObject domPublishEBOM = DomainObject.newInstance(context, strPublishEBOM);
                    StringList slVariantIds = domPublishEBOM.getInfoList(context, "attribute[" + TigerConstants.ATTRIBUTE_PSS_PUBLISHEDPART_PSS_VARIANTASSEMBLYLIST + "].value");
                    String strVariantIdsList = (String) slVariantIds.get(0);
                    StringList slVariantIdsList = FrameworkUtil.split(strVariantIdsList, "|");
                    for (int j = 0; j < slVariantIdsList.size(); j++) {
                        String strVIDEBOM = (String) slVariantIdsList.get(j);
                        DomainObject domVIdEOM = DomainObject.newInstance(context, strVIDEBOM);
                        String strVIdName = domVIdEOM.getInfo(context, DomainConstants.SELECT_NAME);
                        slVIDsofEBOM.add(strVIdName);
                    }

                    if (!slVIDsofMBOM.isEmpty()) {
                        if (slVIDsofMBOM.size() == slVIDsofEBOM.size()) {
                            for (int k = 0; k < slVIDsofMBOM.size(); k++) {
                                String strName = (String) slVIDsofMBOM.get(k);
                                if (!slVIDsofEBOM.contains(strName)) {
                                    iResult = 1;
                                    break;
                                }
                            }
                        } else {
                            iResult = 1;
                        }
                    } else {
                        if (!slVIDsofEBOM.isEmpty()) {
                            iResult = 1;

                        }
                    }

                }
            }

            flushAndCloseSession(plmSession);
        } catch (Exception e) {
            flushAndCloseSession(plmSession);
            logger.error("Error in checkVariantIdConsistancy()\n", e);
            throw e;
        }

        return iResult;
    }

    // TIGTK-10661:MBOM-UE:End
    // TIGTK-10660:Rutuja Ekatpure:Start
    /***
     * used to check variant id generated if Effectivity set on MBOM
     * @param context
     * @param programMap
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unused")
    private static int checkVariantIdOnMBOM(Context context, Map<?, ?> programMap) throws Exception {
        logger.debug("checkVariantIdOnMBOM:Start");
        int result = 1;
        try {
            String strObjectId = (String) programMap.get("objectId");
            String strHasChildren = (String) programMap.get("hasChildren");
            MapList mlChildrentList = new MapList();
            if (programMap.containsKey("children")) {
                mlChildrentList = (MapList) programMap.get("children");
            }
            if (mlChildrentList.size() > 0) {
                Map mChild = (Map) mlChildrentList.get(0);
                String strConnectionId = (String) mChild.get("id[connection]");
                if (UIUtil.isNotNullAndNotEmpty(strConnectionId)) {
                    String currentEffXML = FRCMBOMModelerUtility.getEffectivityXML(context, strConnectionId, true);
                    String displayStr = FRCMBOMModelerUtility.getEffectivityOrderedStringFromXML(context, currentEffXML);
                    if (UIUtil.isNotNullAndNotEmpty(displayStr)) {
                        DomainObject domMBOM = DomainObject.newInstance(context, strObjectId);
                        StringList slVariantIdofMBOM = domMBOM.getInfoList(context, "from[PSS_PartVariantAssembly].to.id");
                        if (!slVariantIdofMBOM.isEmpty()) {
                            result = 0;
                        }
                    } else {
                        result = 0;
                    }
                }
            } else if ("false".equalsIgnoreCase(strHasChildren) || mlChildrentList.size() == 0) {
                result = 0;
            }
        } catch (Exception e) {
            logger.error("Error in checkVariantIdOnMBOM()\n", e);
        }
        logger.debug("checkVariantIdOnMBOM:End");
        return result;
    }
    // TIGTK-10660:Rutuja Ekatpure:End

}
