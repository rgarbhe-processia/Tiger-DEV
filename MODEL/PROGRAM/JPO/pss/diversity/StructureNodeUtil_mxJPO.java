package pss.diversity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;

import com.matrixone.apps.configuration.ProductConfiguration;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.XSSUtil;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.ExpansionIterator;
import matrix.db.JPO;
import matrix.db.Policy;
import matrix.db.RelationshipType;
import matrix.util.MatrixException;
import matrix.util.Pattern;
import matrix.util.StringList;
import pss.constants.TigerConstants;
// DIVERSITY:PHASE2.0 : TIGTK-9119 & TIGTK-9146 : PSE : 09-08-2017 : END

public class StructureNodeUtil_mxJPO {

    // TIGTK-5405 - 11-04-2017 - VB - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(StructureNodeUtil_mxJPO.class);
    // TIGTK-5405 - 11-04-2017 - VB - END

    public int mxMain(Context context, String[] args) throws Exception {
        try {

        } catch (Exception e) {
            // TIGTK-5405 - 21-04-2017 - PTE - START
            logger.error("Error in mxMain : ", e);
            // TIGTK-5405 - 21-04-2017 - PTE - END
        }

        return 0;
    }

    // TIGTK-2968 : 09-09-2016

    /**
     * @param context
     * @param args
     * @throws Exception
     * @Modified By : Chintan DADHANIA
     * @Modified On : 05-08-2016
     * @Modification Reason : JIRA Ticket TIGTK-2682
     */
    public void setResetStructureNodeFlag(Context context, String[] args) throws Exception {

        try {

            String strNewValueOfAttribute = args[0];

            String strFromObjectId = args[1];
            String strToObjectId = args[2];
            String strEvent = args[3];

            DomainObject domFromPartObject = new DomainObject(strFromObjectId);
            boolean checkForSetStructureNodeFlag = false;
            boolean checkForResetStructureNodeFlag = false;

            if (!strNewValueOfAttribute.isEmpty() && strEvent.equalsIgnoreCase("Modify")) {
                checkForSetStructureNodeFlag = true;
            } else if (strNewValueOfAttribute.isEmpty() && strEvent.equalsIgnoreCase("Modify")) {
                checkForResetStructureNodeFlag = true;

            }

            if (strEvent.equalsIgnoreCase("Create")) {
                DomainObject domToPartObject = new DomainObject(strToObjectId);
                String strStrucuteNode = domToPartObject.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_STRUCTURENODE);

                if (strStrucuteNode.equalsIgnoreCase("Yes")) {
                    checkForSetStructureNodeFlag = true;

                }

            }

            if (strEvent.equalsIgnoreCase("Delete")) {
                // Diversity: TIGTK-3708 | 02/12/16 : GC : Start
                // if (strStrucuteNode.equalsIgnoreCase("Yes")) {
                checkForResetStructureNodeFlag = true;

                // }
                // Diversity: TIGTK-3708 | 02/12/16 : GC : End

            }
            StringList objectSelects = new StringList();
            objectSelects.add(DomainConstants.SELECT_ID);
            objectSelects.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_STRUCTURENODE + "]");
            String objectWhere = "attribute[PSS_StructureNode] != Yes";

            if (checkForSetStructureNodeFlag) {
                String strStrucuteNode = domFromPartObject.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_STRUCTURENODE);

                if (strStrucuteNode.equalsIgnoreCase("Yes")) {
                    return;
                }
                domFromPartObject.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_STRUCTURENODE, "Yes");
                MapList mapParentPartObjects = domFromPartObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_EBOM, TigerConstants.TYPE_PART, objectSelects, DomainConstants.EMPTY_STRINGLIST,
                        true, false, (short) 0, objectWhere, DomainConstants.EMPTY_STRING, 0);

                if (mapParentPartObjects != null && !mapParentPartObjects.isEmpty()) {
                    for (int i = 0; i < mapParentPartObjects.size(); i++) {
                        Map mapParts = (Map) mapParentPartObjects.get(i);
                        DomainObject domChildPart = new DomainObject((String) mapParts.get(DomainConstants.SELECT_ID));
                        domChildPart.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_STRUCTURENODE, "Yes");

                    }

                }

            }

            if (checkForResetStructureNodeFlag) {
                String strStrucuteNode = domFromPartObject.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_STRUCTURENODE);
                if (!strStrucuteNode.equalsIgnoreCase("Yes")) {
                    return;
                }
                // TIGTK-2881 : 29-08-2016 : START
                // String relWhere = "to[EBOM].attribute[PSS_StructureNode] == \"Yes\" || attribute[" + TigerConstants.ATTRIBUTE_PSS_SIMPLEREFFECTIVITYEXPRESSION + "] !=\"\"";

                // Added for TIGTK-3606 by Priyanka Salunke on date : 18/11/2016 : START
                // Get the related parts with attribute Structure Node and Simplere Effectivity Expression
                StringList strRelSelectes = new StringList();
                strRelSelectes.add(DomainRelationship.SELECT_ID);
                strRelSelectes.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_SIMPLEREFFECTIVITYEXPRESSION + "]");
                strRelSelectes.add("to." + TigerConstants.ATTRIBUTE_PSS_STRUCTURENODE);

                // TIGTK-2682 : 05-08-2016 : START
                MapList mlTemp = domFromPartObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_EBOM, TigerConstants.TYPE_PART, objectSelects, strRelSelectes, false, true, (short) 0,
                        DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, 0);
                // TIGTK-2682 : 05-08-2016 : END
                // TIGTK-2881 : 29-08-2016 : END
                if (mlTemp != null && !mlTemp.isEmpty()) {
                    for (int i = 0; i < mlTemp.size(); i++) {
                        Map mEBOMObjectMap = (Map) mlTemp.get(i);
                        String strStructureNodeAttr = (String) mEBOMObjectMap.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_STRUCTURENODE + "]");
                        String strSimplerEffectivityAttr = (String) mEBOMObjectMap.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_SIMPLEREFFECTIVITYEXPRESSION + "]");
                        // If part have child with effectivity and structure node=yes then return
                        if (strStructureNodeAttr.equalsIgnoreCase("Yes") || UIUtil.isNotNullAndNotEmpty(strSimplerEffectivityAttr)) {
                            return;
                        }
                    }
                }
                // Added for TIGTK-3606 by Priyanka Salunke on date : 18/11/2016 : END
                domFromPartObject.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_STRUCTURENODE, "No");
                iterateandsetvalue(context, domFromPartObject);
            }

        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in setResetStructureNodeFlag: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END

        }
    }

    public void iterateandsetvalue(Context context, DomainObject domFromPartObject) {
        try {
            String objectWhere = "attribute[" + TigerConstants.ATTRIBUTE_PSS_STRUCTURENODE + "] == 'Yes'";
            StringList objectSelects = new StringList();
            objectSelects.add(DomainConstants.SELECT_ID);
            objectSelects.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_STRUCTURENODE + "]");
            // Diversity: TIGTK-3708 | 02/12/16 : GC : Start
            StringList slRelSelectes = new StringList();
            slRelSelectes.add(DomainRelationship.SELECT_ID);
            slRelSelectes.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_SIMPLEREFFECTIVITYEXPRESSION + "]");
            // Diversity: TIGTK-3708 | 02/12/16 : GC : End
            MapList mapParentPartObjects = domFromPartObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_EBOM, TigerConstants.TYPE_PART, objectSelects, DomainConstants.EMPTY_STRINGLIST,
                    true, false, (short) 1, objectWhere, DomainConstants.EMPTY_STRING, 0);

            if (mapParentPartObjects != null && !mapParentPartObjects.isEmpty()) {
                // TIGTK-11725 : START
                boolean boolResetFlag = false;
                // TIGTK-11725 : END
                for (int i = 0; i < mapParentPartObjects.size(); i++) {
                    boolResetFlag = true;
                    Map mapParts = (Map) mapParentPartObjects.get(i);
                    DomainObject domChildPart = new DomainObject((String) mapParts.get(DomainConstants.SELECT_ID));

                    // objectWhere = ""; //"attribute[PSS_StructureNode] == 'Yes'";
                    // relwhere = "from[EBOM].attribute[" + TigerConstants.ATTRIBUTE_PSS_SIMPLEREFFECTIVITYEXPRESSION + "] !=''";
                    // TIGTK-2682 : 05-08-2016 : START
                    MapList mlTemp = domChildPart.getRelatedObjects(context, TigerConstants.RELATIONSHIP_EBOM, TigerConstants.TYPE_PART, objectSelects, slRelSelectes, false, true, (short) 0,
                            DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, 0);

                    // Diversity: TIGTK-3708 | 02/12/16 : GC : Start
                    if (mlTemp != null && !mlTemp.isEmpty()) {
                        for (int itr = 0; itr < mlTemp.size(); itr++) {
                            Map mEBOMObjectMap = (Map) mlTemp.get(itr);
                            String strStructureNodeAttr = (String) mEBOMObjectMap.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_STRUCTURENODE + "]");
                            String strSimplerEffectivityAttr = (String) mEBOMObjectMap.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_SIMPLEREFFECTIVITYEXPRESSION + "]");
                            // If part have child with effectivity and structure node=yes then return
                            if (strStructureNodeAttr.equalsIgnoreCase("Yes") || UIUtil.isNotNullAndNotEmpty(strSimplerEffectivityAttr)) {
                                boolResetFlag = false;
                                break;
                            }
                        }
                    }
                    // Diversity: TIGTK-3708 | 02/12/16 : GC : End

                    // Diversity: TIGTK-3708 | 02/12/16 : GC : Start
                    if (boolResetFlag) {
                        // Diversity: TIGTK-3708 | 02/12/16 : GC : End
                        domChildPart.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_STRUCTURENODE, "No");
                        iterateandsetvalue(context, domChildPart);
                    }
                    // TIGTK-2682 : 05-08-2016 : END
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in iterateandsetvalue: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }

    }

    /**
     * @param context
     * @param args
     * @throws Exception
     * @author Asha Gholve
     */
    public Map compareAndGenerateVariantAssembly(Context context, String[] args) throws Exception {
        try {

            String strPartObjectId = args[0];
            String strProductConfiguration = args[1];
            String strIsMassCreate = args[2];

            // create domain object of part
            DomainObject domPart = new DomainObject(strPartObjectId);
            DomainObject domProductConfig = new DomainObject(strProductConfiguration);
            String currentstate = domPart.getInfo(context, DomainConstants.SELECT_CURRENT);
            // TIGTK-2553 : Diversity : 29-07-2016 : START

            String STR_ALLOWED_STATE = EnoviaResourceBundle.getProperty(context, "emxConfigurationStringResource", context.getLocale(),
                    "PSS_emxConfiguration.Part.VariantAssembly.Create.Condition.State");
            if (!STR_ALLOWED_STATE.contains(currentstate)) {
                String notice = EnoviaResourceBundle.getProperty(context, "emxConfigurationStringResource", context.getLocale(), "PSS_emxConfiguration.Errormsg.NotINWORKStateError");
                MqlUtil.mqlCommand(context, "notice $1", notice);
                return new HashMap();
            }
            // TIGTK-2553 : Diversity : 29-07-2016 : END
            String filterExpression = domProductConfig.getAttributeValue(context, TigerConstants.ATTRIBUTE_FILTERCOMPILEDFORM);

            Map<String, Object> mapVariantAssemblyComparisonData = new HashMap<String, Object>();
            MapList mlVariantAssemblyForDoubles = new MapList();
            MapList mlVariantAssemblyForUpdate = new MapList();

            StringList objectSelect = new StringList();
            objectSelect.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_STRUCTURENODE + "]");
            objectSelect.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_XMLSTRUCTURE + "]");
            objectSelect.add("attribute[" + TigerConstants.ATTRIBUTE_MARKETINGNAME + "]");
            objectSelect.add(DomainConstants.SELECT_ID);
            objectSelect.add(DomainConstants.SELECT_DESCRIPTION);
            objectSelect.add(DomainConstants.SELECT_NAME);
            objectSelect.add(DomainConstants.SELECT_CURRENT);
            objectSelect.add(DomainConstants.SELECT_REVISION);
            // TIGTK-11360 : START
            objectSelect.add("project");
            // TIGTK-11360 : END
            // TIGTK-2639 : 03-08-2016 : START
            objectSelect.add(DomainConstants.SELECT_POLICY);
            // TIGTK-2639 : 03-08-2016 : END

            // TIGTK-2968 : 09-09-2016 : START
            StringList relSelect = new StringList();
            // TIGTK-12177 & TIGTK-10841 : START
            relSelect.add("attribute[" + TigerConstants.ATTRIBUTE_REFERENCEDESIGNATOR + "]");
            // TIGTK-12177 & TIGTK-10841 : END

            // TIGTK-2968 : 09-09-2016 : END
            Map mapParentdata = domPart.getInfo(context, objectSelect);
            StringList relationshipselect = new StringList();
            relationshipselect.add("attribute[" + TigerConstants.ATTRIBUTE_EFFECTIVITYORDEREDIMPACTINGCRITERIA + "]");

            relationshipselect.add("attribute[" + TigerConstants.ATTRIBUTE_EFFECTIVITYORDEREDCRITERIADICTIONARY + "]");

            relationshipselect.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_SIMPLEREFFECTIVITYEXPRESSION + "]");

            // TIGTK-12177 & TIGTK-10841 : START
            relationshipselect.add("attribute[" + TigerConstants.ATTRIBUTE_REFERENCEDESIGNATOR + "]");
            // TIGTK-12177 & TIGTK-10841 : END

            relationshipselect.add("attribute[" + TigerConstants.ATTRIBUTE_EFFECTIVITY_VARIABLE_INDEXES + "]");

            relationshipselect.add("attribute[" + TigerConstants.ATTRIBUTE_EFFECTIVITYEXPRESSIONBINARY + "]");

            relationshipselect.add("attribute[" + TigerConstants.ATTRIBUTE_EFFECTIVITYPROPOSEDEXPRESSION + "]");

            relationshipselect.add("attribute[" + TigerConstants.ATTRIBUTE_EFFECTIVITYORDEREDCRITERIA + "]");

            relationshipselect.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_SIMPLEREFFECTIVITYEXPRESSION + "]");

            relationshipselect.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_CUSTOMEFFECTIVITYEXPRESSION + "]");

            MapList mlFilteredParts = domPart.getRelatedObjects(context, TigerConstants.RELATIONSHIP_EBOM, TigerConstants.TYPE_PART, objectSelect, relationshipselect, false, true, (short) 0, null,
                    null, (short) 0, false, false, DomainObject.PAGE_SIZE, null, null, null, null, filterExpression, DomainObject.FILTER_STRUCTURE);

            if (mlFilteredParts != null && !mlFilteredParts.isEmpty()) {

                mlFilteredParts.add(mapParentdata);
                for (int i = 0; i < mlFilteredParts.size(); i++) {
                    Map mapCurrentFilteredPart = (Map) mlFilteredParts.get(i);
                    String isStructureNode = (String) mapCurrentFilteredPart.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_STRUCTURENODE + "]");

                    if (isStructureNode.equalsIgnoreCase("Yes")) {
                        String strPartId = (String) mapCurrentFilteredPart.get(DomainConstants.SELECT_ID);

                        DomainObject domCurPartObj = new DomainObject(strPartId);
                        // TIGTK-2968 : 09-09-2016 : START
                        MapList mlStructureNode = domCurPartObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_EBOM, TigerConstants.TYPE_PART, objectSelect, relSelect, false, true, (short) 0,
                                null, null, (short) 0, false, false, DomainObject.PAGE_SIZE, null, null, null, null, filterExpression, DomainObject.FILTER_STRUCTURE);
                        // TIGTK-2968 : 09-09-2016 : END
                        String strXMLStructure = prepareXMLStructure(context, strPartId, mlStructureNode);
                        String objectWhere = "from[" + TigerConstants.RELATIONSHIP_PSS_VARIANT_ASSEMBLY_PRODUCT_CONFIGURATION + "].to.id == '" + strProductConfiguration + "' && revision == 'last'";
                        MapList mlVariantAssembly = domCurPartObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_PARTVARIANTASSEMBLY, TigerConstants.TYPE_PSS_VARIANTASSEMBLY, objectSelect,
                                DomainConstants.EMPTY_STRINGLIST, false, true, (short) 1, objectWhere, DomainConstants.EMPTY_STRING, 0);

                        boolean isUseCase1Applicable = false;

                        if (mlVariantAssembly == null || mlVariantAssembly.isEmpty()) {

                            MapList mlVariantAssembly1 = domCurPartObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_PARTVARIANTASSEMBLY, TigerConstants.TYPE_PSS_VARIANTASSEMBLY,
                                    objectSelect, DomainConstants.EMPTY_STRINGLIST, false, true, (short) 1, "revision == 'last'", DomainConstants.EMPTY_STRING, 0);

                            if (mlVariantAssembly1 == null || mlVariantAssembly1.isEmpty()) {
                                isUseCase1Applicable = true; // Usecase 1
                            } else if (mlVariantAssembly1.size() > 0) {// Usecase 3 and 4
                                boolean boolIsSimilarVariantAssemblyFound = false;
                                for (int j = 0; j < mlVariantAssembly1.size(); j++) {

                                    Map mapCurrentVariantAssembly = (Map) mlVariantAssembly1.get(j);
                                    int nComparisonResult = compareVariantAssembly(context, mapCurrentVariantAssembly, strXMLStructure);

                                    if (nComparisonResult == 0) {// Equal Found - Usecase 3
                                        boolIsSimilarVariantAssemblyFound = true;
                                        String strVariantAssemblyId = (String) mapCurrentVariantAssembly.get(DomainConstants.SELECT_ID);
                                        String strVariantAssemblyName = (String) mapCurrentVariantAssembly.get(DomainConstants.SELECT_NAME);
                                        DomainObject DomVariantObject = new DomainObject(strVariantAssemblyId);

                                        StringList lirelPRoduct = new StringList(1);
                                        lirelPRoduct.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_VARIANTOPTIONS + "]");
                                        MapList mlProductConfigurations = DomVariantObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_VARIANT_ASSEMBLY_PRODUCT_CONFIGURATION,
                                                TigerConstants.TYPE_PRODUCTCONFIGURATION, objectSelect, lirelPRoduct, false, true, (short) 1, DomainConstants.EMPTY_STRING,
                                                DomainConstants.EMPTY_STRING, 0);

                                        Map<String, Object> temp = new HashMap<String, Object>();
                                        temp.put(DomainConstants.SELECT_ID, (String) mapCurrentFilteredPart.get(DomainConstants.SELECT_ID));
                                        temp.put(DomainConstants.SELECT_DESCRIPTION, (String) mapCurrentFilteredPart.get(DomainConstants.SELECT_DESCRIPTION));
                                        temp.put(DomainConstants.SELECT_NAME, (String) mapCurrentFilteredPart.get(DomainConstants.SELECT_NAME));
                                        temp.put(DomainConstants.SELECT_CURRENT, (String) mapCurrentFilteredPart.get(DomainConstants.SELECT_CURRENT));
                                        temp.put(DomainConstants.SELECT_REVISION, (String) mapCurrentFilteredPart.get(DomainConstants.SELECT_REVISION));
                                        // TIGTK-2639 : 03-08-2016 : START
                                        temp.put(DomainConstants.SELECT_POLICY, (String) mapCurrentFilteredPart.get(DomainConstants.SELECT_POLICY));
                                        // TIGTK-2639 : 03-08-2016 : END
                                        temp.put("strVariantAssemblyId", strVariantAssemblyId);

                                        temp.put("strVariantAssemblyName", strVariantAssemblyName);
                                        temp.put("strProductConfiguration", strProductConfiguration);
                                        temp.put("mlProductConfigurations", mlProductConfigurations);
                                        // TIGTK-3004 : 09-09-2016 : START
                                        if (strIsMassCreate.equals("true")) {
                                            connectVariantAssembly(context, (String) temp.get(DomainConstants.SELECT_ID), strProductConfiguration, strVariantAssemblyId);
                                        } else {
                                            mlVariantAssemblyForDoubles.add(temp); // show dialog of the doubles - Reuse
                                        }
                                        // TIGTK-3004 : 09-09-2016 : END
                                    }

                                }

                                if (!boolIsSimilarVariantAssemblyFound) {// Usecase 4 - Create new
                                    generateVariantAssembly(context, strPartId, strProductConfiguration, strXMLStructure);
                                }

                            }

                            if ((mlVariantAssembly1 == null || mlVariantAssembly1.size() == 0) && isUseCase1Applicable == true) {

                                generateVariantAssembly(context, strPartId, strProductConfiguration, strXMLStructure); // Usecase 1 - Create Variant Assembly

                            }

                        } else if (mlVariantAssembly.size() == 1) {// Usecase 2
                            Map mapCurrentVariantAssembly = (Map) mlVariantAssembly.get(0);

                            int nComparisonResult = compareVariantAssembly(context, mapCurrentVariantAssembly, strXMLStructure);

                            if (nComparisonResult == 1) { // Not Equal Found
                                String strVariantAssemblyId = (String) mapCurrentVariantAssembly.get(DomainConstants.SELECT_ID);
                                String strVariantAssemblyName = (String) mapCurrentVariantAssembly.get(DomainConstants.SELECT_NAME);
                                DomainObject DomVariantObject = new DomainObject(strVariantAssemblyId);

                                StringList relationshipSelects = new StringList();
                                relationshipSelects.add("id[connection]");
                                relationshipSelects.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_VARIANTOPTIONS + "]");

                                MapList mlProductConfigurations = DomVariantObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_VARIANT_ASSEMBLY_PRODUCT_CONFIGURATION,
                                        TigerConstants.TYPE_PRODUCTCONFIGURATION, objectSelect, relationshipSelects, false, true, (short) 1, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING,
                                        0);

                                // TIGTK-3979 : 25-01-2017 : START
                                // TIGTK-4530 : 25-01-2017 : START
                                boolean boolIsSimilarVariantAssemblyFound = false;
                                MapList mlVariantAssembly1 = domCurPartObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_PARTVARIANTASSEMBLY, TigerConstants.TYPE_PSS_VARIANTASSEMBLY,
                                        objectSelect, DomainConstants.EMPTY_STRINGLIST, false, true, (short) 1, "revision == 'last'", DomainConstants.EMPTY_STRING, 0);
                                for (int j = 0; j < mlVariantAssembly1.size(); j++) {

                                    mapCurrentVariantAssembly = (Map) mlVariantAssembly1.get(j);
                                    nComparisonResult = compareVariantAssembly(context, mapCurrentVariantAssembly, strXMLStructure);

                                    if (nComparisonResult == 0) {
                                        boolIsSimilarVariantAssemblyFound = true;
                                        strVariantAssemblyId = (String) mapCurrentVariantAssembly.get(DomainConstants.SELECT_ID);
                                        strVariantAssemblyName = (String) mapCurrentVariantAssembly.get(DomainConstants.SELECT_NAME);
                                        DomVariantObject = new DomainObject(strVariantAssemblyId);

                                        StringList lirelPRoduct = new StringList(1);
                                        lirelPRoduct.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_VARIANTOPTIONS + "]");
                                        mlProductConfigurations = DomVariantObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_VARIANT_ASSEMBLY_PRODUCT_CONFIGURATION,
                                                TigerConstants.TYPE_PRODUCTCONFIGURATION, objectSelect, lirelPRoduct, false, true, (short) 1, DomainConstants.EMPTY_STRING,
                                                DomainConstants.EMPTY_STRING, 0);

                                        Map<String, Object> temp = new HashMap<String, Object>();
                                        temp.put(DomainConstants.SELECT_ID, (String) mapCurrentFilteredPart.get(DomainConstants.SELECT_ID));
                                        temp.put(DomainConstants.SELECT_DESCRIPTION, (String) mapCurrentFilteredPart.get(DomainConstants.SELECT_DESCRIPTION));
                                        temp.put(DomainConstants.SELECT_NAME, (String) mapCurrentFilteredPart.get(DomainConstants.SELECT_NAME));
                                        temp.put(DomainConstants.SELECT_CURRENT, (String) mapCurrentFilteredPart.get(DomainConstants.SELECT_CURRENT));
                                        temp.put(DomainConstants.SELECT_REVISION, (String) mapCurrentFilteredPart.get(DomainConstants.SELECT_REVISION));
                                        // TIGTK-2639 : 03-08-2016 : START
                                        temp.put(DomainConstants.SELECT_POLICY, (String) mapCurrentFilteredPart.get(DomainConstants.SELECT_POLICY));
                                        // TIGTK-2639 : 03-08-2016 : END
                                        temp.put("strVariantAssemblyId", strVariantAssemblyId);

                                        temp.put("strVariantAssemblyName", strVariantAssemblyName);
                                        temp.put("strProductConfiguration", strProductConfiguration);
                                        temp.put("mlProductConfigurations", mlProductConfigurations);
                                        // TIGTK-3004 : 09-09-2016 : START
                                        if (strIsMassCreate.equals("true")) {
                                            connectVariantAssembly(context, (String) temp.get(DomainConstants.SELECT_ID), strProductConfiguration, strVariantAssemblyId);
                                        } else {
                                            mlVariantAssemblyForDoubles.add(temp);
                                        }
                                    }
                                }
                                // TIGTK-3979 : 25-01-2017 : END
                                // TIGTK-4530 : 25-01-2017 : END

                                if (mlProductConfigurations.size() == 1) {

                                    if (!boolIsSimilarVariantAssemblyFound) {
                                        Map mapProductConfi = (Map) mlProductConfigurations.get(0);
                                        String abc = DomainConstants.SELECT_RELATIONSHIP_ID;
                                        Map<String, String> mapInputdata = new HashMap<String, String>();
                                        mapInputdata.put("PartId", (String) mapCurrentFilteredPart.get(DomainConstants.SELECT_ID));
                                        mapInputdata.put("ProductConfigurationId", (String) mapProductConfi.get(DomainConstants.SELECT_ID));
                                        mapInputdata.put("PSS_VariantAssemblyProductConfigurationId", (String) mapProductConfi.get(DomainRelationship.SELECT_ID));
                                        mapInputdata.put("VariantAssemblyId", strVariantAssemblyId);
                                        mapInputdata.put("XMLStructure", strXMLStructure);
                                        updateVariantAssembly(context, mapInputdata);
                                        // updateVariantAssembly(context, strVariantAssemblyId, strXMLStructure);
                                    }
                                } else if (mlProductConfigurations.size() > 1) {
                                    if (!boolIsSimilarVariantAssemblyFound) {
                                        Map<String, Object> temp = new HashMap<String, Object>();
                                        temp.put(DomainConstants.SELECT_ID, (String) mapCurrentFilteredPart.get(DomainConstants.SELECT_ID));
                                        temp.put(DomainConstants.SELECT_DESCRIPTION, (String) mapCurrentFilteredPart.get(DomainConstants.SELECT_DESCRIPTION));
                                        temp.put(DomainConstants.SELECT_NAME, (String) mapCurrentFilteredPart.get(DomainConstants.SELECT_NAME));
                                        temp.put(DomainConstants.SELECT_CURRENT, (String) mapCurrentFilteredPart.get(DomainConstants.SELECT_CURRENT));
                                        temp.put(DomainConstants.SELECT_REVISION, (String) mapCurrentFilteredPart.get(DomainConstants.SELECT_REVISION));
                                        // Added for TIGTK-3062 by Priyanka Salunke on Date : 28-10-2016 : START
                                        // To show display name of state in comparision Update pop up
                                        temp.put(DomainConstants.SELECT_POLICY, (String) mapCurrentFilteredPart.get(DomainConstants.SELECT_POLICY));
                                        // Added for TIGTK-3062 by Priyanka Salunke on Date : 28-10-2016 : END
                                        temp.put("strVariantAssemblyId", strVariantAssemblyId);
                                        temp.put("strVariantAssemblyName", strVariantAssemblyName);
                                        temp.put("strXMLStructure", strXMLStructure);
                                        temp.put("strProductConfiguration", strProductConfiguration);
                                        temp.put("mlProductConfigurations", mlProductConfigurations);
                                        mlVariantAssemblyForUpdate.add(temp);

                                    }
                                }
                            }
                        }
                    }
                }

                mapVariantAssemblyComparisonData.put("mapForVariantAssemblyUpdate", mlVariantAssemblyForUpdate);

                mapVariantAssemblyComparisonData.put("mapForVariantAssemblyDoubles", mlVariantAssemblyForDoubles);
                return mapVariantAssemblyComparisonData;
            }

        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in compareAndGenerateVariantAssembly: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw (e);

        }
        // Modofied for TIGTK-3641 by Priyanka Salunke On Date : 21/11/2016 : START
        return new HashMap();
        // return null;
        // Modofied for TIGTK-3641 by Priyanka Salunke On Date : 21/11/2016 : END

    }

    /**
     * @param context
     * @param args
     * @author SGS
     * @throws Exception
     * @description this method will connvert args to proper arguments and pass it to updateVariantAssembly method
     */
    public void updateVariantAssemblyCaller(Context context, String[] args) throws Exception {
        try {
            String strpartID = args[0];
            String strConfigurationid = args[2];
            String strVariantAssemblyId = args[1];
            String strXMLStructure = args[3];
            // Bug Fixing (Decode XML Structure String): 09-09-2016 : START
            strXMLStructure = java.net.URLDecoder.decode(strXMLStructure, "UTF-8");
            // Bug Fixing : 09-09-2016 : END
            Map<String, String> mapInputdata = new HashMap<String, String>();

            mapInputdata.put("PartId", strpartID);
            mapInputdata.put("ProductConfigurationId", strConfigurationid);
            mapInputdata.put("VariantAssemblyId", strVariantAssemblyId);
            mapInputdata.put("XMLStructure", strXMLStructure);
            String strConnectionID = DomainObject.EMPTY_STRING;

            try {
                strConnectionID = MqlUtil.mqlCommand(context, "print connection bus $1 to $2 rel $3 select $4 dump $5",
                        new String[] { strVariantAssemblyId, strConfigurationid, TigerConstants.RELATIONSHIP_PSS_VARIANT_ASSEMBLY_PRODUCT_CONFIGURATION, "id", "|" });
            } catch (Exception mqlException1) {
                // TIGTK-5405 - 5-11-2017 - PTE - START
                logger.error("Error in updateVariantAssemblyCaller: ", mqlException1);
                // TIGTK-5405 - 5-11-2017 - PTE - END

            }
            // Find Bug issue : TIGTK-3953 : Priyanka Salunke : 25-Jan-2017 : END

            mapInputdata.put("PSS_VariantAssemblyProductConfigurationId", strConnectionID);

            updateVariantAssembly(context, mapInputdata);

        } catch (MatrixException e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in updateVariantAssemblyCaller: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }

    }

    /**
     * @param context
     * @param mapArgs
     * @throws Exception
     */
    private void updateVariantAssembly(Context context, Map mapArgs) throws Exception {

        try {
            String strVariantAssemblyId = (String) mapArgs.get("VariantAssemblyId");
            String strXMLStructure = (String) mapArgs.get("XMLStructure");

            DomainObject doObjVariantAssemblyId = DomainObject.newInstance(context, strVariantAssemblyId);

            doObjVariantAssemblyId.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_XMLSTRUCTURE, strXMLStructure);
            String[] strParamArray = new String[2];
            strParamArray[0] = (String) mapArgs.get("PartId");
            strParamArray[1] = (String) mapArgs.get("ProductConfigurationId");

            String strPCRelatedOptions = getPCRelatedOptionsForPart(context, strParamArray);
            if ((String) mapArgs.get("PSS_VariantAssemblyProductConfigurationId") != null) {
                DomainRelationship.setAttributeValue(context, (String) mapArgs.get("PSS_VariantAssemblyProductConfigurationId"), TigerConstants.ATTRIBUTE_PSS_VARIANTOPTIONS, strPCRelatedOptions);
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in updateVariantAssembly: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    private int compareVariantAssembly(Context context, Map mapCurrentVariantAssembly, String strXMLStructure) {

        int nComparisonResult = 1;

        String strVariantAssemblyXMLStructure = (String) mapCurrentVariantAssembly.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_XMLSTRUCTURE + "]");
        if (strVariantAssemblyXMLStructure.equalsIgnoreCase(strXMLStructure)) {

            nComparisonResult = 0;
        }
        return nComparisonResult;
    }

    public void generateVariantAssemblyCaller(Context context, String[] args) {
        String strpartID = args[0];
        String strConfigurationid = args[2];
        String strXMLStructure = args[3];
        String xml = XSSUtil.decodeFromURL(strXMLStructure);
        generateVariantAssembly(context, strpartID, strConfigurationid, xml);

    }

    private void generateVariantAssembly(Context context, String strPartID, String strProductConfiguration, String strXMLStructure) {

        try {
            // TIGTK-2702 : 05-08-2016 : Start
            DomainObject domPartObject = DomainObject.newInstance(context, strPartID);
            String strPartStatus = domPartObject.getInfo(context, DomainConstants.SELECT_CURRENT);
            String STR_ALLOWED_STATE = EnoviaResourceBundle.getProperty(context, "emxConfigurationStringResource", context.getLocale(),
                    "PSS_emxConfiguration.Part.VariantAssembly.Create.Condition.State");
            if (STR_ALLOWED_STATE.contains(strPartStatus)) {

                String StrType = "PSS_VariantAssembly";
                String StrPolicy = "PSS_VariantAssembly";
                String StrRevision = "";
                // TIGTK-2553 : Diversity : 28-07-2016 : START
                try {
                    Policy policyObj = new Policy(StrPolicy);
                    StrRevision = policyObj.getFirstInSequence(context);
                } catch (Exception e) {
                    // TIGTK-6366 :Migration - Variant Assembly Revision Format: By AG: Starts
                    StrRevision = "01";
                    // TIGTK-6366 :Migration - Variant Assembly Revision Format: By AG: Ends
                    // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
                    logger.error("Error in generateVariantAssembly: ", e);
                    // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - END
                }
                // TIGTK-2553 : Diversity : 28-07-2016 : END
                String StrVault = "eService Production";

                String StrName = DomainObject.getAutoGeneratedName(context, "type_PSS_VariantAssembly", null);

                DomainObject dObj = new DomainObject();
                dObj.createObject(context, TigerConstants.TYPE_PSS_VARIANTASSEMBLY, StrName, StrRevision, StrPolicy, StrVault);

                dObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_XMLSTRUCTURE, strXMLStructure);

                String strVariantAssemblyId = dObj.getId(context);

                connectVariantAssembly(context, strPartID, strProductConfiguration, strVariantAssemblyId);
            }
            // TIGTK-2702 : 05-08-2016 : End
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in generateVariantAssembly: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
        return;
    }

    public void connectVariantAssemblyCaller(Context context, String[] args) {
        try {
            String strpartID = args[0];
            String strConfigurationid = args[2];
            String strVariantAssemblyId = args[1];
            // TIGTK-6366 :Migration - Variant Assembly Revision Format: By AG: Starts
            if (args.length > 3)
                updateVariantAssemblyXMLStructure(context, args);
            // TIGTK-6366 :Migration - Variant Assembly Revision Format: By AG: END

            connectVariantAssembly(context, strpartID, strConfigurationid, strVariantAssemblyId);
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in connectVariantAssemblyCaller: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }

    }

    /*
     * Created By : Chintan DADHANIA Created On : 06-Aug-2016 Purpose : Disconnect Variant Assembly from Product Configuration.
     */
    public void disconnectVariantAssemblyFromPCCaller(Context context, String[] args) {
        try {
            String strpartID = args[0];
            String strConfigurationid = args[2];
            String strCommand = "temp query bus PSS_VariantAssembly * * where 'to[" + TigerConstants.RELATIONSHIP_PSS_PARTVARIANTASSEMBLY + "].from.id == \"" + strpartID + "\" and from["
                    + TigerConstants.RELATIONSHIP_PSS_VARIANT_ASSEMBLY_PRODUCT_CONFIGURATION + "].to.id == \"" + strConfigurationid + "\"' select id dump |;";

            // String result = MqlUtil.mqlCommand(context, strCommand);
            String result = MqlUtil.mqlCommand(context, strCommand);

            StringTokenizer tokens = new StringTokenizer(result, "\n", false);
            while (tokens.hasMoreTokens()) {
                String token = tokens.nextToken();
                StringList tempList = FrameworkUtil.split(token, "|");
                for (int i = 3; i < tempList.size(); i++) {
                    String strVAObjectId = (String) tempList.get(i);
                    strCommand = "disconnect bus " + strVAObjectId + " to " + strConfigurationid + " relationship " + TigerConstants.RELATIONSHIP_PSS_VARIANT_ASSEMBLY_PRODUCT_CONFIGURATION;
                    // MqlUtil.mqlCommand(context, strCommand);
                    MqlUtil.mqlCommand(context, strCommand);

                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in disconnectVariantAssemblyFromPCCaller: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
    }

    @SuppressWarnings({ "rawtypes", "deprecation", "unchecked" })
    public void connectVariantAssembly(Context context, String strPartId, String strProductConfigurationId, String strVariantAssemblyId) throws Exception {
        try {

            String strFromObjId_Part = strPartId;

            String strToObjId_VariantAssembly = strVariantAssemblyId;

            String strFromObjId_VariantAssembly = strVariantAssemblyId;

            String strToObjId_ProductConfiguration = strProductConfigurationId;

            Boolean bPreserve = false;

            // Connect Relationship between Part to VariantAssembly type
            // String mql = "print connection bus " + strFromObjId_Part + " to " + strToObjId_VariantAssembly + " select id dump |;";
            String strConnectionIDs = DomainObject.EMPTY_STRING;

            // Find Bug issue : TIGTK-3953 : Priyanka Salunke : 25-Jan-2017 : START
            try {
                // Modify for TIGTK-5462:PK:Start
                // strConnectionIDs = MqlUtil.mqlCommand(context, "print connection bus $1 to $2 rel $3 select $4 dump $5",
                StringBuilder strBuilder = new StringBuilder("query connection type ");
                strBuilder.append(TigerConstants.RELATIONSHIP_PSS_PARTVARIANTASSEMBLY);
                strBuilder.append(" where 'from.id==");
                strBuilder.append(strFromObjId_Part);
                strBuilder.append(" AND to.id==");
                strBuilder.append(strToObjId_VariantAssembly);
                strBuilder.append("' select ");
                strBuilder.append(DomainConstants.SELECT_ID);
                strBuilder.append(" dump |");
                strConnectionIDs = MqlUtil.mqlCommand(context, strBuilder.toString(), false, false);
            } catch (Exception mqlException1) {
                // TIGTK-5405 - 5-11-2017 - PTE - START
                logger.error("Error in connectVariantAssembly: ", mqlException1);
                // TIGTK-5405 - 5-11-2017 - PTE - END
            }
            //
            //
            // Find Bug issue : TIGTK-3953 : Priyanka Salunke : 25-Jan-2017 : END

            if (strConnectionIDs.length() <= 0)
                DomainRelationship.connect(context, strFromObjId_Part, TigerConstants.RELATIONSHIP_PSS_PARTVARIANTASSEMBLY, strToObjId_VariantAssembly, bPreserve);

            // Connect Relationship between VariantAssembly to ProductConfiguration type
            DomainObject domVarAssembly = new DomainObject(strFromObjId_VariantAssembly);
            String[] relatedIds = new String[1];
            relatedIds[0] = strToObjId_ProductConfiguration;

            Map conids = DomainRelationship.connect(context, domVarAssembly, TigerConstants.RELATIONSHIP_PSS_VARIANT_ASSEMBLY_PRODUCT_CONFIGURATION, true, relatedIds);

            String[] strParamArray = new String[2];
            strParamArray[0] = strPartId;
            strParamArray[1] = strProductConfigurationId;

            String strPCRelatedOptions = getPCRelatedOptionsForPart(context, strParamArray);

            DomainRelationship.setAttributeValue(context, (String) conids.get(strToObjId_ProductConfiguration), TigerConstants.ATTRIBUTE_PSS_VARIANTOPTIONS, strPCRelatedOptions);

        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in connectVariantAssembly: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }

    }

    private String prepareXMLStructure(Context context, String strObjectId, MapList mlStructureNode) {
        StringBuffer strXML = new StringBuffer("");

        // Get Attributes List for Part mentioned in Properties File
        String strPartAttributes = EnoviaResourceBundle.getProperty(context, "emxConfigurationStringResource", context.getLocale(), "PSS_emxConfiguration.VariantAssemblyXML.PartAttributes");
        String[] SelectedPartAttributes = strPartAttributes.split("\\,");

        // Get Attributes List for EBOM mentioned in Properties File
        String strEBOMAttributes = EnoviaResourceBundle.getProperty(context, "emxConfigurationStringResource", context.getLocale(), "PSS_emxConfiguration.VariantAssemblyXML.EBOMAttributes");
        String[] SelectedEBOMAttributes = strEBOMAttributes.split("\\,");

        try {
            // Create Domain Object of Main Part
            DomainObject domObject = DomainObject.newInstance(context, strObjectId);
            String PartAttributes = "";
            // Iterate over List of Attributes of Part and create a String with Attribute Name and Value
            for (String attr : SelectedPartAttributes) {
                String strAttribute = domObject.getInfo(context, attr.trim());
                PartAttributes = PartAttributes + attr.replaceAll("\\s", "_") + "=\"" + strAttribute + "\"";
            }

            int iPartCount = mlStructureNode.size();
            Map tempMap = null;

            // creating stack
            Stack<Integer> stack = new Stack<>();

            int strCurrentStackLevel = 0;

            strXML.append("<Part " + PartAttributes + ">");
            // Iterate over Structured Node
            for (int i = 0; i < iPartCount; i++) {
                tempMap = (Map) mlStructureNode.get(i);
                int strCurrentLevel = Integer.parseInt((String) tempMap.get("level"));

                // String strPartAttributeDetails = "";
                StringBuffer sbAttriDetails = new StringBuffer();
                // Iterate over List of Attributes of Part and create a String with Attribute Name and Value
                for (String attr : SelectedPartAttributes) {
                    // start: FindBugs Issue : Sneha
                    String strPartAttribute = (String) tempMap.get(attr.trim());
                    sbAttriDetails.append(attr);
                    sbAttriDetails.append("=\"");
                    sbAttriDetails.append(strPartAttribute);
                    sbAttriDetails.append("\"");
                    // end
                }

                String strEBOMAttributeDetails = "";
                // Iterate over List of Attributes of EBOM and create a String with Attribute Name and Value

                StringBuffer sbEBOMAttributeDetails = new StringBuffer();

                for (String attr : SelectedEBOMAttributes) {
                    String strAttrName = PropertyUtil.getSchemaProperty(context, attr.trim());
                    String strEBOMAttribute = (String) tempMap.get("attribute[" + strAttrName + "]");

                    // Find Bug issue TIGTK-3953 : Priyanka Salunke : 24-Jan-2017 START

                    sbEBOMAttributeDetails.append(strEBOMAttributeDetails);
                    sbEBOMAttributeDetails.append(" ");
                    sbEBOMAttributeDetails.append(strAttrName.replaceAll("\\s", "_"));
                    sbEBOMAttributeDetails.append("=\"");
                    sbEBOMAttributeDetails.append(strEBOMAttribute);
                    sbEBOMAttributeDetails.append("\"");
                }
                strEBOMAttributeDetails = sbEBOMAttributeDetails.toString();

                // Find Bug issue TIGTK-3953 : Priyanka Salunke : 24-Jan-2017 END
                if (strCurrentLevel == strCurrentStackLevel) {
                    strXML.append("</Part></EBOM>");
                } else if (strCurrentLevel < strCurrentStackLevel) {
                    int strDiffInLevel = strCurrentStackLevel - strCurrentLevel;

                    for (int p = 0; p < strDiffInLevel; p++) {
                        strXML.append("</Part></EBOM>");
                        stack.pop();
                    }
                    strXML.append("</Part>");
                    strCurrentStackLevel = stack.peek();
                } else {
                    stack.add(strCurrentLevel);
                    strCurrentStackLevel = strCurrentLevel;
                }
                strXML.append("<EBOM ");
                strXML.append(strEBOMAttributeDetails);
                strXML.append("><Part ");
                strXML.append(sbAttriDetails.toString());
                strXML.append(">");

            }
            int stackSize1 = stack.size();

            for (int j = 0; j < stackSize1; j++) {
                strXML.append("</Part></EBOM>");
                stack.pop();
            }
            strXML.append("</Part>");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in prepareXMLStructure: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }

        return strXML.toString();
    }

    public String getVariantAssemblyComparisonTableForDoubles(Context context, String[] args) throws Exception {

        StringBuffer sbComparisonResult = new StringBuffer("");

        // TIGTK-2639 : 03-08-2016 : START
        String strLocale = context.getSession().getLanguage();
        // TIGTK-2639 : 03-08-2016 : END
        try {
            MapList mlComparisonTableData = (MapList) JPO.unpackArgs(args);

            int nComparisonTableDataCount = mlComparisonTableData.size();
            Map<String, String[]> mapPartdata = new HashMap<String, String[]>();
            Map<String, String> mapProductName = new HashMap<String, String>();
            // TIGTK-6808 | Harika Varanasi | 16/06/2017 : Starts
            int nRowCnt = 0;

            for (int i = 0; i < nComparisonTableDataCount; i++) {
                String strTRClass = "even";
                if (nRowCnt % 2 != 0) {
                    strTRClass = "odd";
                }
                Map mapStrctureNodeDetail = (Map) mlComparisonTableData.get(i);
                String strStructureNodeId = (String) mapStrctureNodeDetail.get(DomainConstants.SELECT_ID);
                String strVariantAssemblyId = (String) mapStrctureNodeDetail.get("strVariantAssemblyId");
                String strVariantAssemblyName = (String) mapStrctureNodeDetail.get("strVariantAssemblyName");
                // TIGTK-11360 : START
                String[] partrelteddata = new String[6];
                // TIGTK-11360 : END
                boolean isChecked = false;
                if (!mapPartdata.containsKey(strStructureNodeId)) {

                    String strStructureNodeName = (String) mapStrctureNodeDetail.get(DomainConstants.SELECT_NAME);
                    String strStructureNodeRevision = (String) mapStrctureNodeDetail.get(DomainConstants.SELECT_REVISION);
                    // TIGTK-2639 : Diversity : 03-08-2016 :START
                    String strStructureNodeState = (String) mapStrctureNodeDetail.get(DomainConstants.SELECT_CURRENT);
                    String strStructureNodePolicy = (String) mapStrctureNodeDetail.get(DomainConstants.SELECT_POLICY);
                    String strStructureNodeCurrent = i18nNow.getStateI18NString(strStructureNodePolicy, strStructureNodeState, strLocale);
                    // TIGTK-2639 : 03-08-2016 : END

                    String strStructureNodeDescription = (String) mapStrctureNodeDetail.get(DomainConstants.SELECT_DESCRIPTION);
                    StringBuffer abinitialrow = new StringBuffer("");
                    // Findbug Issue correction start
                    // Date: 22/03/2017
                    // By: Asha G.
                    abinitialrow.append("<tr class='");
                    abinitialrow.append(strTRClass);
                    abinitialrow.append("'>");
                    abinitialrow.append("<td>");
                    abinitialrow.append(strStructureNodeName);
                    abinitialrow.append("</td>");
                    abinitialrow.append("<td>");
                    abinitialrow.append(strStructureNodeRevision);
                    abinitialrow.append("</td>");
                    abinitialrow.append("<td>");
                    abinitialrow.append(strStructureNodeCurrent);
                    abinitialrow.append("</td>");
                    abinitialrow.append("<td>");
                    abinitialrow.append(strStructureNodeDescription);
                    abinitialrow.append("</td>");
                    // Findbug Issue correction end
                    // abinitialrow.append("<td>").append(strVariantAssemblyId).append("</td>");
                    partrelteddata[0] = abinitialrow.toString();
                    // TIGTK-11360 : START
                    partrelteddata[1] = DomainConstants.EMPTY_STRING;
                    partrelteddata[2] = DomainConstants.EMPTY_STRING;
                    partrelteddata[3] = DomainConstants.EMPTY_STRING;
                    partrelteddata[4] = DomainConstants.EMPTY_STRING;
                    partrelteddata[5] = DomainConstants.EMPTY_STRING;
                    // TIGTK-11360 : END
                    isChecked = true;

                    nRowCnt++;
                    // TIGTK-6808 | Harika Varanasi | 16/06/2017 : Ends
                } else {

                    partrelteddata = mapPartdata.get(strStructureNodeId);
                }

                String strProductConfiguration = (String) mapStrctureNodeDetail.get("strProductConfiguration");

                MapList mlProductConfigurations = (MapList) mapStrctureNodeDetail.get("mlProductConfigurations");
                int ProductConfigurtionsCount = mlProductConfigurations.size();
                String strclassName = strStructureNodeId + "|" + strVariantAssemblyId + "|" + strProductConfiguration;

                String checkedval = isChecked ? "checked" : "";
                StringBuffer sbVariantAssembly = new StringBuffer();
                sbVariantAssembly.append("<input type='radio' name='");
                sbVariantAssembly.append(strStructureNodeId);
                sbVariantAssembly.append("' value='");
                sbVariantAssembly.append(strclassName);
                sbVariantAssembly.append("' ");
                sbVariantAssembly.append(checkedval);
                sbVariantAssembly.append(" />");
                sbVariantAssembly.append(strVariantAssemblyName);
                sbVariantAssembly.append("</br>");
                partrelteddata[1] += sbVariantAssembly.toString();
                StringBuffer sbProductMarketingName = new StringBuffer();

                StringBuffer sbProductConfig = new StringBuffer();
                StringBuffer sbVariantOptions = new StringBuffer();
                // TIGTK-11360 : START
                StringBuffer sbCollabSpaceofPC = new StringBuffer();
                // TIGTK-11360 : END
                for (int j = 0; j < ProductConfigurtionsCount; j++) {
                    // TIGTK-11360 : START
                    sbCollabSpaceofPC.append((String) ((Map) mlProductConfigurations.get(j)).get("project"));
                    // TIGTK-12489 : START
                    sbCollabSpaceofPC.append("</br>");
                    // TIGTK-12489 : END
                    // TIGTK-11360 : END
                    sbProductMarketingName.append("Product Marketing Name :");
                    String strProductID = (String) ((Map) mlProductConfigurations.get(j)).get(DomainConstants.SELECT_ID);
                    if (mapProductName.containsKey(strProductID)) {
                        sbProductMarketingName.append(mapProductName.get(strProductID));
                        sbProductMarketingName.append("</br>");
                    } else {
                        DomainObject domProductConfig = new DomainObject(strProductID);
                        StringList attributes = new StringList(1);
                        attributes.add("attribute[" + TigerConstants.ATTRIBUTE_MARKETINGNAME + "]");
                        Map product = domProductConfig.getRelatedObject(context, TigerConstants.RELATIONSHIP_FEATUREPRODUCTCONFIGURATION, false, attributes, DomainConstants.EMPTY_STRINGLIST);
                        if (!product.isEmpty()) {
                            sbProductMarketingName.append((String) product.get("attribute[" + TigerConstants.ATTRIBUTE_MARKETINGNAME + "]") == null ? ""
                                    : (String) product.get("attribute[" + TigerConstants.ATTRIBUTE_MARKETINGNAME + "]"));
                            sbProductMarketingName.append("</br>");
                            mapProductName.put(strProductID, (String) product.get("attribute[" + TigerConstants.ATTRIBUTE_MARKETINGNAME + "]") == null ? ""
                                    : (String) product.get("attribute[" + TigerConstants.ATTRIBUTE_MARKETINGNAME + "]"));
                        }

                    }
                    sbProductConfig.append(((Map) mlProductConfigurations.get(j)).get(DomainConstants.SELECT_NAME));
                    sbProductConfig.append(" : ");
                    sbProductConfig.append((String) ((Map) mlProductConfigurations.get(j)).get("attribute[" + TigerConstants.ATTRIBUTE_MARKETINGNAME + "]") == null ? ""
                            : ((Map) mlProductConfigurations.get(j)).get("attribute[" + TigerConstants.ATTRIBUTE_MARKETINGNAME + "]"));
                    sbProductConfig.append("</br>");
                    sbVariantOptions.append((String) ((Map) mlProductConfigurations.get(j)).get("attribute[" + TigerConstants.ATTRIBUTE_PSS_VARIANTOPTIONS + "]") == null ? ""
                            : ((Map) mlProductConfigurations.get(j)).get("attribute[" + TigerConstants.ATTRIBUTE_PSS_VARIANTOPTIONS + "]"));
                    sbVariantOptions.append("</br>");

                }
                partrelteddata[2] += sbProductMarketingName.toString();
                partrelteddata[3] += sbProductConfig.toString();
                partrelteddata[4] += sbVariantOptions.toString();
                // TIGTK-11360 : START
                partrelteddata[5] += sbCollabSpaceofPC.toString();
                // TIGTK-11360 : END
                mapPartdata.put(strStructureNodeId, partrelteddata);

            }

            // Find Bug issue : TIGTK-3953 : Priyanka Salunke : 24-Jan-2017 : START
            for (java.util.Map.Entry<String, String[]> entrySet : mapPartdata.entrySet()) {
                // Modification done by PTE for Find bug issue
                String[] data = entrySet.getValue();
                sbComparisonResult.append(data[0]);
                sbComparisonResult.append("<td>");
                sbComparisonResult.append(data[1]);
                sbComparisonResult.append("</td>");
                sbComparisonResult.append("<td>");
                sbComparisonResult.append(data[4]);
                sbComparisonResult.append("</td>");
                sbComparisonResult.append("<td>");
                sbComparisonResult.append(data[2]);
                sbComparisonResult.append("</td>");
                sbComparisonResult.append("<td>");
                sbComparisonResult.append(data[3]);
                sbComparisonResult.append("</td>");
                // TIGTK-11360 : START
                sbComparisonResult.append("<td>");
                sbComparisonResult.append(data[5]);
                sbComparisonResult.append("</td>");
                // TIGTK-11360 : END

                sbComparisonResult.append("</tr>");

            }
            // Find Bug issue : TIGTK-3953 : Priyanka Salunke : 24-Jan-2017 : END
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getVariantAssemblyComparisonTableForDoubles: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END

        }
        return sbComparisonResult.toString();

    }

    public String getUpdateVariantAssemblyComparisonTable(Context context, String[] args) throws Exception {

        StringBuffer sbComparisonResult = new StringBuffer("");
        // Diversity: TIGTK 3755: 07-12-16: GC: Start
        StringList slStructureNodes = new StringList();
        // Diversity: TIGTK 3755: 07-12-16: GC: End
        // TIGTK-2639 : Diversity : 03-08-2016 : START
        String strLocale = context.getSession().getLanguage();
        // TIGTK-2639 : Diversity : 03-08-2016 : END
        try {
            MapList mlComparisonTableData = (MapList) JPO.unpackArgs(args);
            int nComparisonTableDataCount = mlComparisonTableData.size();
            // TIGTK-6808 | Harika Varanasi | 16/06/2017 : Starts
            int nRowCnt = 0;

            for (int i = 0; i < nComparisonTableDataCount; i++) {
                String strTRClass = "even";
                if (nRowCnt % 2 != 0) {
                    strTRClass = "odd";
                }
                Map mapStrctureNodeDetail = (Map) mlComparisonTableData.get(i);
                String strStructureNodeId = (String) mapStrctureNodeDetail.get(DomainConstants.SELECT_ID);
                // Diversity: TIGTK 3755: 07-12-16: GC: Start
                if (!slStructureNodes.contains(strStructureNodeId)) {
                    slStructureNodes.add(strStructureNodeId);
                    // Diversity: TIGTK 3755: 07-12-16: GC: End
                    String strStructureNodeName = (String) mapStrctureNodeDetail.get(DomainConstants.SELECT_NAME);
                    String strStructureNodeRevision = (String) mapStrctureNodeDetail.get(DomainConstants.SELECT_REVISION);
                    // TIGTK-2639 : Diversity : 03-08-2016 : START
                    String strStructureNodeState = (String) mapStrctureNodeDetail.get(DomainConstants.SELECT_CURRENT);
                    String strStructureNodePolicy = (String) mapStrctureNodeDetail.get(DomainConstants.SELECT_POLICY);
                    String strStructureNodeCurrent = i18nNow.getStateI18NString(strStructureNodePolicy, strStructureNodeState, strLocale);
                    // TIGTK-2639 : Diversity : 03-08-2016 : END
                    String strStructureNodeDescription = (String) mapStrctureNodeDetail.get(DomainConstants.SELECT_DESCRIPTION);
                    String strVariantAssemblyId = (String) mapStrctureNodeDetail.get("strVariantAssemblyId");
                    String strProductConfiguration = (String) mapStrctureNodeDetail.get("strProductConfiguration");
                    String strVariantAssemblyName = (String) mapStrctureNodeDetail.get("strVariantAssemblyName");
                    String strXMLStructure = (String) mapStrctureNodeDetail.get("strXMLStructure");
                    String encoded = XSSUtil.encodeForURL(context, strXMLStructure);
                    MapList mlProductConfigurations = (MapList) mapStrctureNodeDetail.get("mlProductConfigurations");
                    int ProductConfigurtionsCount = mlProductConfigurations.size();
                    // Modification Done by PTE for FindBug Issue
                    sbComparisonResult.append("<tr class='");
                    sbComparisonResult.append(strTRClass);
                    sbComparisonResult.append("'>");
                    sbComparisonResult.append("<td>");
                    sbComparisonResult.append(strStructureNodeName);
                    sbComparisonResult.append("</td>");
                    sbComparisonResult.append("<td>");
                    sbComparisonResult.append(strStructureNodeRevision);
                    sbComparisonResult.append("</td>");
                    sbComparisonResult.append("<td>");
                    sbComparisonResult.append(strStructureNodeCurrent);
                    sbComparisonResult.append("</td>");
                    sbComparisonResult.append("<td>");
                    sbComparisonResult.append(strStructureNodeDescription);
                    sbComparisonResult.append("</td>");
                    sbComparisonResult.append("<td>");
                    sbComparisonResult.append(strVariantAssemblyName);
                    sbComparisonResult.append("</td>");

                    StringBuffer sbProductMarketingName = new StringBuffer();
                    StringBuffer sbProductConfig = new StringBuffer();
                    StringBuffer sbVariantOptions = new StringBuffer();
                    // TIGTK-11360 : START
                    StringBuffer sbCollabSpaceofPC = new StringBuffer();
                    // TIGTK-11360 : END
                    String strclassName = strStructureNodeId + "|" + strVariantAssemblyId + "|" + strProductConfiguration + "|" + encoded;
                    for (int j = 0; j < ProductConfigurtionsCount; j++) {
                        // TIGTK-11360 : START
                        sbCollabSpaceofPC.append((String) ((Map) mlProductConfigurations.get(j)).get("project"));
                        sbCollabSpaceofPC.append("</br>");
                        // TIGTK-11360 : END
                        sbProductMarketingName.append("Product Marketing Name :");
                        sbVariantOptions.append(((Map) mlProductConfigurations.get(j)).get("attribute[" + TigerConstants.ATTRIBUTE_PSS_VARIANTOPTIONS + "]") == null ? ""
                                : ((Map) mlProductConfigurations.get(j)).get("attribute[" + TigerConstants.ATTRIBUTE_PSS_VARIANTOPTIONS + "]"));
                        // Findbug Issue correction start
                        // Date: 22/03/2017
                        // By: Asha G.
                        sbVariantOptions.append("</br>");
                        // Findbug Issue correction end
                        String strProductID = (String) ((Map) mlProductConfigurations.get(j)).get(DomainConstants.SELECT_ID);
                        DomainObject domProductConfig = new DomainObject(strProductID);
                        StringList attributes = new StringList(1);
                        attributes.add("attribute[" + TigerConstants.ATTRIBUTE_MARKETINGNAME + "]");
                        Map product = domProductConfig.getRelatedObject(context, TigerConstants.RELATIONSHIP_FEATUREPRODUCTCONFIGURATION, false, attributes, DomainConstants.EMPTY_STRINGLIST);
                        if (!product.isEmpty()) {

                            sbProductMarketingName.append((String) product.get("attribute[" + TigerConstants.ATTRIBUTE_MARKETINGNAME + "]") == null ? ""
                                    : (String) product.get("attribute[" + TigerConstants.ATTRIBUTE_MARKETINGNAME + "]"));
                            sbProductMarketingName.append("</br>");

                        }

                        // sbProductMarketingName.append(((Map) mlProductConfigurations.get(j)).get("strProductMarketingName")).append("</br>");

                        sbProductConfig.append(((Map) mlProductConfigurations.get(j)).get(DomainConstants.SELECT_NAME));
                        sbProductConfig.append(" : ");
                        sbProductConfig.append(((Map) mlProductConfigurations.get(j)).get("attribute[" + TigerConstants.ATTRIBUTE_MARKETINGNAME + "]") == null ? ""
                                : ((Map) mlProductConfigurations.get(j)).get("attribute[" + TigerConstants.ATTRIBUTE_MARKETINGNAME + "]"));
                        sbProductConfig.append("</br>");

                    }

                    sbComparisonResult.append("<td>");
                    sbComparisonResult.append(sbVariantOptions.toString());
                    sbComparisonResult.append("</td>");
                    sbComparisonResult.append("<td>");
                    sbComparisonResult.append(sbProductMarketingName.toString());
                    sbComparisonResult.append("</td>");
                    sbComparisonResult.append("<td>");
                    sbComparisonResult.append(sbProductConfig.toString());
                    sbComparisonResult.append("</td>");
                    // TIGTK-11360 : START
                    sbComparisonResult.append("<td>");
                    sbComparisonResult.append(sbCollabSpaceofPC.toString());
                    sbComparisonResult.append("</td>");
                    // TIGTK-11360 : END
                    // TIGTK-3306 : START
                    // Modified for TIGTK-3748 by GC and PS Date:06-Dec-2016 : START
                    String fieldNameStr = "CreateNewUpdate" + i;
                    sbComparisonResult.append("<td>");
                    sbComparisonResult.append("<input type='radio' name='");
                    sbComparisonResult.append(fieldNameStr);
                    // TIGTK-8038 : PSE : 11-10-2017 : START
                    sbComparisonResult.append("' value='Update'/>");
                    sbComparisonResult.append("Keep Exsting Variant Id");
                    // TIGTK-8038 : PSE : 11-10-2017 : END
                    sbComparisonResult.append("<input type='hidden' name='");
                    sbComparisonResult.append(fieldNameStr);
                    sbComparisonResult.append("Update_value");
                    sbComparisonResult.append("' value='");
                    sbComparisonResult.append(strclassName);
                    sbComparisonResult.append("'/>");
                    sbComparisonResult.append("</br>");
                    // fieldNameStr = "CreateNew"+i;
                    sbComparisonResult.append("<input type='radio' name='");
                    sbComparisonResult.append(fieldNameStr);
                    // TIGTK-8038 : PSE : 11-10-2017 : START
                    sbComparisonResult.append("' value='Create New'/>");
                    sbComparisonResult.append("Create New Variant Id");
                    // TIGTK-8038 : PSE : 11-10-2017 : END
                    sbComparisonResult.append("<input type='hidden' name='");
                    sbComparisonResult.append(fieldNameStr);
                    sbComparisonResult.append("CreateNew_value");
                    sbComparisonResult.append("' value='");
                    sbComparisonResult.append(strclassName);
                    sbComparisonResult.append("'/>");
                    sbComparisonResult.append("</td>");
                    // Find Bug : Dodgy Code : PS : 21-March-2017 : END
                    sbComparisonResult.append("</tr>");
                    // Modified for TIGTK-3748 by GC and PS Date:06-Dec-2016 : END
                    // TIGTK-3306 : END
                    // Diversity: TIGTK 3755: 07-12-16: GC: Start

                    nRowCnt++;
                }
                // Diversity: TIGTK 3755: 07-12-16: GC: End
            }

        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getUpdateVariantAssemblyComparisonTable: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END

        }

        return sbComparisonResult.toString();

    }

    public void disconnectUnusedVariantAssembly(Context context, String[] args) throws Exception {
        try {
            String strVariantAssemblyId = args[0];

            DomainObject domVariantAssembly = DomainObject.newInstance(context, strVariantAssemblyId);
            StringList slObjSelects = new StringList();
            StringList slRelSelects = new StringList();
            slRelSelects.add(DomainRelationship.SELECT_ID);

            MapList mlProductConfigurations = domVariantAssembly.getRelatedObjects(context, // context
                    TigerConstants.RELATIONSHIP_PSS_VARIANT_ASSEMBLY_PRODUCT_CONFIGURATION, // relationshipPattern
                    TigerConstants.TYPE_PRODUCTCONFIGURATION, // typePattern
                    slObjSelects, // objectSelects
                    slRelSelects, // relationshipSelects
                    false, // getTo
                    true, // getFrom
                    (short) 0, // recurseToLevel
                    null, // objectWhere
                    null, // relationshipWhere
                    (short) 0, // limit
                    null, // includeType
                    null, // includeRelationship
                    null); // includeMap

            if (mlProductConfigurations.isEmpty() && mlProductConfigurations.size() == 0) {

                MapList mlParts = domVariantAssembly.getRelatedObjects(context, // context
                        TigerConstants.RELATIONSHIP_PSS_PARTVARIANTASSEMBLY, // relationshipPattern
                        DomainConstants.TYPE_PART, // typePattern
                        slObjSelects, // objectSelects
                        slRelSelects, // relationshipSelects
                        true, // getTo
                        false, // getFrom
                        (short) 0, // recurseToLevel
                        null, // objectWhere
                        null, // relationshipWhere
                        (short) 0, // limit
                        null, // includeType
                        null, // includeRelationship
                        null); // includeMap
                for (int itr = 0; itr < mlParts.size(); itr++) {
                    Map<?, ?> mPart = (Map<?, ?>) mlParts.get(itr);
                    String strConnectionId = (String) mPart.get(DomainRelationship.SELECT_ID);
                    String strVariantAssemblyType = (String) domVariantAssembly.getInfo(context, DomainConstants.SELECT_TYPE);
                    if (strVariantAssemblyType.equalsIgnoreCase(TigerConstants.TYPE_PSS_MBOM_VARIANT_ASSEMBLY)) {
                        pss.mbom.StructureNodeUtil_mxJPO.disconnectVariantAssemblyWithHarmonyAssociationCleanUp(context, null, strVariantAssemblyId, strConnectionId);
                    }
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in disconnectUnusedVariantAssembly: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }

    }

    /**
     * This method is used to generate mass variant assemblies.
     * @param context,
     *            args
     * @throws Exception
     * @return int
     * @modified by psalunke :13-11-2017 : TIGTK-11329
     */

    public int generateMassVariantAssembly(Context context, String args[]) throws Exception {
        logger.error("pss.diversity.StructureNodeUtil : generateMassVariantAssembly() : START ");
        int iReturn = 0;
        String strPartObjectId = args[0];

        try {
            if (UIUtil.isNotNullAndNotEmpty(strPartObjectId)) {
                DomainObject domObjectPart = DomainObject.newInstance(context, strPartObjectId);
                // TIGTK-11329 : PSE : START

                StringList slObjectSelect = new StringList();
                slObjectSelect.add(DomainConstants.SELECT_ID);
                slObjectSelect.add(DomainConstants.SELECT_NAME);
                slObjectSelect.add(DomainConstants.SELECT_TYPE);

                // Rel pattern
                Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_GBOM);
                relationshipPattern.addPattern(DomainConstants.RELATIONSHIP_EBOM);
                // Type pattern
                Pattern includeTypePattern = new Pattern(TigerConstants.TYPE_PART);
                includeTypePattern.addPattern(TigerConstants.TYPE_PRODUCTS);

                // Rel Post pattern
                Pattern postRelRelationship = new Pattern(TigerConstants.RELATIONSHIP_GBOM);

                MapList mlProducts = (MapList) domObjectPart.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship pattern
                        includeTypePattern.getPattern(), // type pattern
                        new StringList(DomainConstants.SELECT_ID), // object select
                        DomainConstants.EMPTY_STRINGLIST, // rel selects
                        true, // to direction
                        false, // from direction
                        (short) 0, // recursion level
                        DomainConstants.EMPTY_STRING, // object where clause
                        DomainConstants.EMPTY_STRING, // relationship where clause
                        (short) 0, // limit
                        false, // checkHidden
                        true, // preventDuplicates
                        (short) 0, // pageSize
                        null, // post type pattern
                        postRelRelationship, // post rel pattern
                        null, null, null);

                String strProdId = "";
                String strProdConfgObjId = "";
                if (mlProducts.size() > 0) {
                    StringList slUniqueProducts = new StringList();
                    for (int i = 0; i < mlProducts.size(); i++) {
                        Map mpObjparent = (Map) mlProducts.get(i);
                        strProdId = (String) mpObjparent.get(DomainConstants.SELECT_ID);
                        if (!slUniqueProducts.contains(strProdId)) {
                            slUniqueProducts.add(strProdId);
                        }
                    }

                    if (slUniqueProducts.size() > 0) {
                        for (int i = 0; i < slUniqueProducts.size(); i++) {
                            strProdId = (String) slUniqueProducts.get(i);
                            // TIGTK-11329 : PSE : END
                            DomainObject domproduct = new DomainObject(strProdId);
                            MapList mlProductConfigurations = domproduct.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PRODUCT_CONFIGURATION, TigerConstants.TYPE_PRODUCTCONFIGURATION, false,
                                    true, 1, slObjectSelect, DomainConstants.EMPTY_STRINGLIST, "", "", 0, "", "", null);
                            if (mlProductConfigurations.size() > 0) {
                                for (int j = 0; j < mlProductConfigurations.size(); j++) {
                                    Map mpObj = (Map) mlProductConfigurations.get(j);
                                    strProdConfgObjId = (String) mpObj.get(DomainConstants.SELECT_ID);

                                    String[] args1 = new String[3];
                                    args1[0] = strPartObjectId;
                                    args1[1] = strProdConfgObjId;
                                    args1[2] = "true";

                                    compareAndGenerateVariantAssembly(context, args1);
                                }
                            }
                        }
                    }
                }
            }
            logger.error("pss.diversity.StructureNodeUtil : generateMassVariantAssembly() : END ");
        } catch (Exception e) {
            iReturn = 1;
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in generateMassVariantAssembly: ", e);
            e.printStackTrace();
            throw e;

            // TIGTK-5405 - 11-04-2017 - VB - END
        }

        return iReturn;
    }

    public void reviseVariantAssembly(Context context, String args[]) throws Exception {
        String strPartObjectId = args[0];

        StringList slObjectSelect = new StringList();
        slObjectSelect.add(DomainConstants.SELECT_ID);
        slObjectSelect.add(DomainConstants.SELECT_NAME);
        // TIGTK-6366 :Migration - Variant Assembly Revision Format: By AG: Starts
        StringList relSelects = new StringList();
        relSelects.add(DomainRelationship.SELECT_ID);
        // TIGTK-12177 & TIGTK-10841 : START
        relSelects.add("attribute[" + TigerConstants.ATTRIBUTE_REFERENCEDESIGNATOR + "]");
        // TIGTK-12177 & TIGTK-10841 : END

        try {
            DomainObject domObjectPart = DomainObject.newInstance(context, strPartObjectId);

            MapList mlVariantAssemblies = domObjectPart.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_PARTVARIANTASSEMBLY, TigerConstants.TYPE_PSS_VARIANTASSEMBLY, slObjectSelect, null,
                    false, true, (short) 1, "", "", 0);
            BusinessObject busLastRevision = domObjectPart.getLastRevision(context);

            DomainObject domRevisedPartObject = DomainObject.newInstance(context, busLastRevision);
            // Modification done by PTE for Find bug issue
            if (mlVariantAssemblies != null) {
                String strVarAsmblyObjId = "";

                String[] strRevisedVariantAssemblyArray = new String[mlVariantAssemblies.size()];
                // Modification done by PTE for Find bug issue
                // if (mlVariantAssemblies != null) {

                int nVariantAssembliesCnt = mlVariantAssemblies.size();
                for (int i = 0; i < nVariantAssembliesCnt; i++) {

                    Map mpObj = (Map) mlVariantAssemblies.get(i);
                    strVarAsmblyObjId = (String) mpObj.get(DomainConstants.SELECT_ID);

                    // Modification Done by PTE For Find bug issue of category Dead store to local variable

                    // DomainObject domObjectVarAsmbly = new DomainObject();
                    DomainObject domObjectVarAsmbly = DomainObject.newInstance(context, strVarAsmblyObjId);

                    BusinessObject busRevisedObject = domObjectVarAsmbly.reviseObject(context, true);

                    String strRevisedObjectId = busRevisedObject.getObjectId(context);

                    strRevisedVariantAssemblyArray[i] = strRevisedObjectId;

                    MapList relatedPCS = domObjectVarAsmbly.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_VARIANT_ASSEMBLY_PRODUCT_CONFIGURATION, "*", slObjectSelect, relSelects, false,
                            true, (short) 0, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, 0);

                    if (relatedPCS != null)
                        for (int j = 0; j < relatedPCS.size(); j++) {
                            Map PCObject = (Map) relatedPCS.get(j);
                            String[] strParamArray = new String[2];
                            strParamArray[0] = domRevisedPartObject.getId(context);
                            strParamArray[1] = (String) PCObject.get(DomainConstants.SELECT_ID);

                            String strPCRelatedOptions = getPCRelatedOptionsForPart(context, strParamArray);

                            DomainRelationship.setAttributeValue(context, (String) PCObject.get(DomainRelationship.SELECT_ID), TigerConstants.ATTRIBUTE_PSS_VARIANTOPTIONS, strPCRelatedOptions);

                            DomainObject domProductConfig = new DomainObject((String) PCObject.get(DomainConstants.SELECT_ID));
                            String filterExpression = domProductConfig.getAttributeValue(context, TigerConstants.ATTRIBUTE_FILTERCOMPILEDFORM);
                            MapList mlStructureNode = domRevisedPartObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_EBOM, TigerConstants.TYPE_PART, slObjectSelect, relSelects, false,
                                    true, (short) 0, null, null, (short) 0, false, false, DomainObject.PAGE_SIZE, null, null, null, null, filterExpression, DomainObject.FILTER_STRUCTURE);
                            String strXMLStructure = prepareXMLStructure(context, domRevisedPartObject.getId(context), mlStructureNode);
                            busRevisedObject.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_XMLSTRUCTURE, strXMLStructure);
                        }
                }
                // TIGTK-6366 :Migration - Variant Assembly Revision Format: By AG: End

                RelationshipType relType = new RelationshipType(TigerConstants.RELATIONSHIP_PSS_PARTVARIANTASSEMBLY);
                // Modification Done by PTE For Find bug issue of category Dead store to local variable
                // Map mlRevised = domRevisedPartObject.addRelatedObjects(context, relType, true, strRevisedVariantAssemblyArray);

                domRevisedPartObject.addRelatedObjects(context, relType, true, strRevisedVariantAssemblyArray);
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in reviseVariantAssembly: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
    }

    //// TIGTK-6366 :Migration - Variant Assembly Revision Format: By AG: Starts
    public void updateVariantAssemblyXMLStructure(Context context, String[] args) throws Exception {

        try {
            StringList slObjectSelect = new StringList();
            slObjectSelect.add(DomainConstants.SELECT_ID);
            slObjectSelect.add(DomainConstants.SELECT_NAME);

            StringList relSelects = new StringList();
            relSelects.add(DomainRelationship.SELECT_ID);
            // TIGTK-12177 & TIGTK-10841 : START
            relSelects.add("attribute[" + TigerConstants.ATTRIBUTE_REFERENCEDESIGNATOR + "]");
            // TIGTK-12177 & TIGTK-10841 : END
            relSelects.add(DomainRelationship.SELECT_NAME);

            String strpartID = args[0];

            DomainObject domRevisedPartObject = new DomainObject(strpartID);

            String strConfigurationid = args[2];

            String strVariantAssemblyId = args[1];

            DomainObject busRevisedObject = new DomainObject(strVariantAssemblyId);
            DomainObject domProductConfig = new DomainObject(strConfigurationid);
            String filterExpression = domProductConfig.getInfo(context, "attribute[" + TigerConstants.ATTRIBUTE_FILTERCOMPILEDFORM + "]");
            ;// domProductConfig.getAttributeValue(context, TigerConstants.ATTRIBUTE_FILTERCOMPILEDFORM);

            MapList filterEBOMList = domRevisedPartObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_EBOM, // relationshipPattern
                    TigerConstants.TYPE_PART, // typePattern
                    slObjectSelect, // objectSelects
                    relSelects, // relationshipSelects
                    false, // getTo
                    true, // getFrom
                    (short) 0, // recurseToLevel
                    "", // objectWhereClause
                    "", // relationshipWhereClause
                    (short) 0, // limit
                    true, // checkHidden
                    false, // preventDuplicates
                    (short) 0, // pageSize
                    null, // includeType
                    null, // includeRelationship
                    null, // includeMap
                    "", // relKeyPrefix
                    filterExpression, // filterExpression
                    (short) 0); // filterFlag

            String strXMLStructure = prepareXMLStructure(context, domRevisedPartObject.getId(context), filterEBOMList);

            busRevisedObject.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_XMLSTRUCTURE, strXMLStructure);

        } catch (Exception e) {
            // TIGTK-5405 - 21-04-2017 - PTE - START
            logger.error("Error in reviseVariantAssembly: ", e);
            // TIGTK-5405 - 21-04-2017 - PTE - END
        }
    }

    /**
     * This method is used to get Variant Assembly Id Column value for EBOM and EBOM In Quantity.
     * @param context
     * @param objectList
     * @throws Exception
     * @author Chintan DADHANIA
     */
    public StringList displayVariantAssemblyID(Context context, String[] args) throws Exception {
        // declare common used variable
        Map tempMap = null;
        String strStructureNodeValue = "", strVariantId = "", strObjectId = "";

        // return StringList
        StringList slReturnVariantIds = new StringList();

        // get ObjectList and Product Configuration Id
        Map programMap = (Map) JPO.unpackArgs(args);
        Map paramList = (Map) programMap.get("paramList");
        Map requestMap = (Map) programMap.get("requestMap");
        MapList mlObjectList = (MapList) programMap.get("objectList");

        String[] ObjectIdsArray = new String[mlObjectList.size()];

        for (int i = 0; i < mlObjectList.size(); i++) {
            // convert StringList to String array to pass it in getInfo.
            ObjectIdsArray[i] = (String) ((Map) mlObjectList.get(i)).get(DomainConstants.SELECT_ID);
        }
        String strProductConfigurationId = DomainConstants.EMPTY_STRING;
        if (paramList != null) {
            strProductConfigurationId = (String) paramList.get("PSS_ProductConfigurationFilter_OID");

        } else
            strProductConfigurationId = (String) requestMap.get("PSS_ProductConfigurationFilter_OID");

        // get Actual Name of Attribute,Relationship from Symbolic name

        // Object Select List
        StringList slObjectSelect = new StringList();
        slObjectSelect.add(TigerConstants.SELECT_ATTRIBUTE_PSS_STRUCTURENODE);

        // Call getInfo to get StructureNode Attribute value for all parts.
        MapList mlStructureNode = DomainObject.getInfo(context, ObjectIdsArray, slObjectSelect);

        int intStructureNodeCount = ObjectIdsArray.length;

        // Modified by Suchit G. for TIGTK-4487 on 22/03/2017: START
        String[] strParamArray = new String[3];
        // Modified by Suchit G. for TIGTK-4487 on 22/03/2017: END
        strParamArray[1] = strProductConfigurationId;

        // Added by Suchit G. for TIGTK-4487 on 22/03/2017: START
        strParamArray[2] = DomainConstants.SELECT_NAME;
        // Added by Suchit G. for TIGTK-4487 on 22/03/2017: END

        // Iterate Structure Node.
        for (int i = 0; i < intStructureNodeCount; i++) {

            // get Current Map
            tempMap = (Map) mlStructureNode.get(i);

            // Get PSS_StructureNode attribute value.
            strStructureNodeValue = (String) tempMap.get(TigerConstants.SELECT_ATTRIBUTE_PSS_STRUCTURENODE);

            if ("Yes".equalsIgnoreCase(strStructureNodeValue)) {
                strObjectId = (String) ObjectIdsArray[i];

                // set current Part Object Id into Array
                strParamArray[0] = strObjectId;

                // Modified by Suchit G. for TIGTK-4487 on 22/03/2017: START
                strVariantId = getVariantAssemblyDetails(context, strParamArray);
                // Modified by Suchit G. for TIGTK-4487 on 22/03/2017: END

                slReturnVariantIds.add(strVariantId);

            } else {
                slReturnVariantIds.add("");
            }

        }

        return slReturnVariantIds;
    }

    /**
     * This method is used to get Variant Assembly Id for Root Part.
     * @param context
     * @param ObjectId
     * @param ProductConfigurationId
     * @throws Exception
     * @author Chintan DADHANIA
     * @Modified By psalunke : TIGTK-9897
     */

    public String getVariantAssemblyDetails(Context context, String args[]) throws Exception {
        logger.error("pss.diversity.StructureNodeUtil : getVariantAssemblyDetails() : START ");
        String strVariantId = DomainConstants.EMPTY_STRING;
        try {
            String strObjectId = args[0];
            String strProductConfigurationId = args[1];
            String strExpression = args[2];

            if (UIUtil.isNotNullAndNotEmpty(strObjectId) && UIUtil.isNotNullAndNotEmpty(strProductConfigurationId)) {
                // Create Domain Object of Root Part.
                DomainObject domPartObject = DomainObject.newInstance(context, strObjectId);

                String strParentStructureNodeValue = domPartObject.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_STRUCTURENODE);
                if (strParentStructureNodeValue.equalsIgnoreCase("Yes")) {
                    // Object Select List
                    StringList slObjectSelect = new StringList();
                    slObjectSelect.add(DomainConstants.SELECT_ID);
                    slObjectSelect.add(strExpression);

                    // Get Variant Assembly Id which is connected with Part and Applied Product Configuration
                    MapList mlVariantAssembly = domPartObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_PARTVARIANTASSEMBLY, // relationship
                            TigerConstants.TYPE_PSS_VARIANTASSEMBLY, // object pattern
                            slObjectSelect, // object selects
                            new StringList(DomainConstants.SELECT_RELATIONSHIP_ID), // relationship selects
                            false, // to direction
                            true, // from direction
                            (short) 1, // recursion level
                            DomainConstants.EMPTY_STRING, // object where clause
                            DomainConstants.EMPTY_STRING, // relationship where clause
                            0);
                    // TIGTK-11725 : END
                    if (mlVariantAssembly != null && !mlVariantAssembly.isEmpty()) {
                        // TIGTK-11725 : START
                        Iterator iteratorVariantAssemblies = mlVariantAssembly.iterator();
                        while (iteratorVariantAssemblies.hasNext()) {
                            Map mVariantAssemblyMap = (Map) iteratorVariantAssemblies.next();
                            String strVariantAssemblyId = (String) mVariantAssemblyMap.get(DomainConstants.SELECT_ID);
                            DomainObject domVariantAssembly = DomainObject.newInstance(context, strVariantAssemblyId);
                            // Object Where clause
                            StringBuffer sbObjectWhere = new StringBuffer();
                            sbObjectWhere.append(DomainConstants.SELECT_ID);
                            sbObjectWhere.append(" == '");
                            sbObjectWhere.append(strProductConfigurationId);
                            sbObjectWhere.append("'");
                            MapList mlVariantAssemblyPC = domVariantAssembly.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_VARIANT_ASSEMBLY_PRODUCT_CONFIGURATION, // relationship
                                    TigerConstants.TYPE_PRODUCTCONFIGURATION, // object pattern
                                    slObjectSelect, // object selects
                                    DomainConstants.EMPTY_STRINGLIST, // relationship selects
                                    false, // to direction
                                    true, // from direction
                                    (short) 1, // recursion level
                                    sbObjectWhere.toString(), // object where clause
                                    DomainConstants.EMPTY_STRING, // relationship where clause
                                    0);
                            if (mlVariantAssemblyPC != null && !mlVariantAssemblyPC.isEmpty()) {
                                strVariantId = (String) mVariantAssemblyMap.get(strExpression);
                                break;
                            } else {
                                continue;
                            }
                        }
                        // TIGTK-11725 : END
                    } else {
                        strVariantId = DomainConstants.EMPTY_STRING;
                    }
                }
            }
            logger.error("pss.diversity.StructureNodeUtil : getVariantAssemblyDetails() : END ");
        } catch (RuntimeException e) {
            logger.error("pss.diversity.StructureNodeUtil : getVariantAssemblyDetails() : ERROR ", e);
            throw e;
        } catch (Exception e) {
            logger.error("pss.diversity.StructureNodeUtil : getVariantAssemblyDetails() : ERROR ", e.toString());
            throw e;
        }
        return strVariantId;

    }

    /**
     * This method is used to get Product Configuration Related options for Structure Node.
     * @param context
     * @param ObjectId
     * @param ProductConfigurationId
     * @throws Exception
     * @author Chintan DADHANIA
     * @modifiedOn 08-08-2016
     * @modifiedBy Chintan DADHANIA
     */
    public String getPCRelatedOptionsForPart(Context context, String args[]) throws Exception {

        // get Root Part and Product Configuration Id from args
        String strRootPartId = args[0];
        String strProductConfigurationId = args[1];

        // Declare Variables of common use
        Map tempMap;
        HashSet<String> hsFeatureList = new HashSet<String>();
        String[] effectivityFeaturesArray;
        String strEffectivityIndexs = "", strFeatureLogicalId = "", strOptionPhyId = "", strOptionWhereClause = "", strFeatureName = "";

        // Added for Diversity Stream : TIGTK-3181 : 07-Dec-2016 : Priyanka Salunke : END
        MapList relBusObjPageList = ProductConfiguration.getSelectedOptions(context, strProductConfigurationId, true, true);
        String SELECT_CONFIGURATION_FEATURE_DISPLAY_NAME = "from." + TigerConstants.SELECT_ATTRIBUTE_DISPLAYNAME;
        // Relationship select list
        StringList slRelSelect = new StringList();
        slRelSelect.add("id[connection]");
        slRelSelect.add(TigerConstants.SELECT_ATTRIBUTE_EFFECTIVITYVARIABLEINDEXES);
        slRelSelect.add(SELECT_CONFIGURATION_FEATURE_DISPLAY_NAME);

        // Make DomainObject of Root Part and get Effectivity Values form all EBOM Connection
        DomainObject domPartObject = DomainObject.newInstance(context, strRootPartId);

        MapList mlEBOMObjects = domPartObject.getRelatedObjects(context, // Context,
                TigerConstants.RELATIONSHIP_EBOM, // Relationship EBOM
                TigerConstants.TYPE_PART, // Type
                null, // Object Select
                slRelSelect, // Relationship Select
                false, // To Side
                true, // From Side
                (short) 0, // Recurse Level
                null, // Object Where
                null, // Relationship Where
                (short) 0, // Limit
                true, // Check for Hiddens
                true, // Prevent duplicates
                DomainObject.PAGE_SIZE, // Page size
                null, // Type Include Patterns
                null, // Relationship Incude Patterns
                null, // Include Map
                null, // Relationship Key
                DomainConstants.EMPTY_STRING, (short) 0);

        // Iterate Result MapList and prepare Unique Feature List.
        for (int i = 0; i < mlEBOMObjects.size(); i++) {
            tempMap = (Map) mlEBOMObjects.get(i);
            strEffectivityIndexs = (String) tempMap.get(TigerConstants.SELECT_ATTRIBUTE_EFFECTIVITYVARIABLEINDEXES);

            if (strEffectivityIndexs.length() > 0) {
                strEffectivityIndexs = strEffectivityIndexs.replaceAll(":", ",");
                effectivityFeaturesArray = strEffectivityIndexs.split("\\,");
                for (int j = 0; j < effectivityFeaturesArray.length; j++) {
                    if (!"".equals(effectivityFeaturesArray[j]) && effectivityFeaturesArray[j].length() > 5)
                        hsFeatureList.add(effectivityFeaturesArray[j]);
                }
            }
        }
        // Added for TIGTK-3181 by Priyanka Salunke : on Date : 16 Nov 2016 : START
        String[] sFeature = new String[hsFeatureList.size()];
        int iCount = 0;
        for (String strFeatureRelId : hsFeatureList) {
            sFeature[iCount] = strFeatureRelId;
            iCount++;
        }
        StringList slObjectSelect = new StringList();
        slObjectSelect.add("from.physicalid");
        MapList mlFeatureIds = DomainRelationship.getInfo(context, sFeature, slObjectSelect);
        // Iterate Maplist and create stringlist

        // Find Bug Issue : TIGTK-3953 : Priyanka Salunke : 25-Jan-2017 : START
        if (mlFeatureIds != null && mlFeatureIds.size() > 0) {
            int intFeatureCOunt = mlFeatureIds.size();
            // Find Bug Issue : TIGTK-3953 : Priyanka Salunke : 25-Jan-2017 : END
            HashSet<String> hsFeatureIds = new HashSet<String>();
            for (int i = 0; i < intFeatureCOunt; i++) {
                hsFeatureIds.add(((Map) mlFeatureIds.get(i)).get("from.physicalid").toString());
            }
            // Added for TIGTK-3181 by Priyanka Salunke : on Date : 16 Nov 2016 : END
            StringList slConfigOptions = new StringList();
            for (int i = 0; i < relBusObjPageList.size(); i++) {
                Map objMap = (Map) relBusObjPageList.get(i);
                if (hsFeatureIds.contains((String) objMap.get("from.physicalid"))) {
                    slConfigOptions.add((String) objMap.get("id[connection]"));
                }
            }
            StringList slConfigOptionsRelSelect = new StringList();
            slConfigOptionsRelSelect.add("from." + DomainConstants.SELECT_NAME);
            slConfigOptionsRelSelect.add("from." + DomainConstants.SELECT_ID);
            slConfigOptionsRelSelect.add("to." + DomainConstants.SELECT_ID);
            slConfigOptionsRelSelect.add("to." + DomainConstants.SELECT_NAME);
            slConfigOptionsRelSelect.add("to." + TigerConstants.SELECT_ATTRIBUTE_DISPLAYNAME);
            slConfigOptionsRelSelect.add("from." + TigerConstants.SELECT_ATTRIBUTE_DISPLAYNAME);

            // Added for TIGTK-3181 by Priyanka Salunke : on Date : 16 Nov 2016 : START
            slConfigOptionsRelSelect.add(SELECT_CONFIGURATION_FEATURE_DISPLAY_NAME);

            MapList mlConfigOptionsList = DomainRelationship.getInfo(context, (String[]) slConfigOptions.toArray(new String[slConfigOptions.size()]), slConfigOptionsRelSelect);
            mlConfigOptionsList.sort(TigerConstants.SELECT_ATTRIBUTE_DISPLAYNAME, "ascending", "integer");
            mlConfigOptionsList.sort(SELECT_CONFIGURATION_FEATURE_DISPLAY_NAME, "ascending", "integer");
            // Added for TIGTK-3181 by Priyanka Salunke : on Date : 16 Nov 2016 : END

            StringBuilder sbPCRelatedOptions = new StringBuilder();
            int intPCRelatedOptionsCount = mlConfigOptionsList.size();
            for (int i = 0; i < intPCRelatedOptionsCount; i++) {
                Map objMap = (Map) mlConfigOptionsList.get(i);
                sbPCRelatedOptions.append(objMap.get("from." + TigerConstants.SELECT_ATTRIBUTE_DISPLAYNAME));
                sbPCRelatedOptions.append("{").append(objMap.get("to." + TigerConstants.SELECT_ATTRIBUTE_DISPLAYNAME));
                sbPCRelatedOptions.append("}");
                // If feature is not last then append +
                if (i + 1 < intPCRelatedOptionsCount) {
                    sbPCRelatedOptions.append("+");
                }
            }
            return sbPCRelatedOptions.toString();
            // Find Bug Issue : TIGTK-3953 : Priyanka Salunke : 25-Jan-2017 : START
        } else {
            return "";
        }
        // Find Bug Issue : TIGTK-3953 : Priyanka Salunke : 25-Jan-2017 : END
    }

    public StringList displayPCRelatedOptionsForPart(Context context, String[] args) throws Exception {

        // declare common used variable
        Map tempMap = null;
        String strStructureNodeValue = "", strVariantOptions = "", strObjectId = "";
        DomainObject domPartObject;

        // return StringList
        StringList slReturnVariantOptions = new StringList();

        // get ObjectList and Product Configuration Id
        Map programMap = (Map) JPO.unpackArgs(args);

        Map paramList = (Map) programMap.get("paramList");
        MapList mlObjectList = (MapList) programMap.get("objectList");

        String strParentOID = (String) paramList.get("parentOID");

        String[] ObjectIdsArray = new String[mlObjectList.size()];
        ObjectIdsArray[0] = strParentOID;
        for (int i = 0; i < mlObjectList.size(); i++) {
            // convert StringList to String array to pass it in getInfo.
            ObjectIdsArray[i] = (String) ((Map) mlObjectList.get(i)).get("id");
        }

        String strProductConfigurationId = (String) paramList.get("PSS_ProductConfigurationFilter_OID");
        // Object Select List
        StringList slObjectSelect = new StringList();
        slObjectSelect.add(TigerConstants.SELECT_ATTRIBUTE_PSS_STRUCTURENODE);

        // Call getInfo to get StructureNode Attribute value for all parts.
        MapList mlStructureNode = DomainObject.getInfo(context, ObjectIdsArray, slObjectSelect);

        int intStructureNodeCount = ObjectIdsArray.length;

        // Object Select List
        slObjectSelect = new StringList();
        slObjectSelect.add(DomainConstants.SELECT_NAME);
        slObjectSelect.add("id");

        // Relationship Select List
        StringList slRelSelect = new StringList();
        slRelSelect.add(DomainConstants.SELECT_ID);
        slRelSelect.add(TigerConstants.SELECT_ATTRIBUTE_PSS_VARIANTOPTIONS);

        String[] strParamArray = new String[2];
        strParamArray[1] = strProductConfigurationId;
        // Iterate Structure Node.
        for (int i = 0; i < intStructureNodeCount; i++) {

            // get Current Map
            tempMap = (Map) mlStructureNode.get(i);

            // Get PSS_StructureNode attribute value.
            strStructureNodeValue = (String) tempMap.get(TigerConstants.SELECT_ATTRIBUTE_PSS_STRUCTURENODE);

            if ("Yes".equalsIgnoreCase(strStructureNodeValue)) {
                strObjectId = (String) ObjectIdsArray[i];
                strParamArray[0] = strObjectId;

                strVariantOptions = getVariantOptionsForPart(context, strParamArray);
                slReturnVariantOptions.add(strVariantOptions);
            } else {
                slReturnVariantOptions.add("");
            }

        }

        return slReturnVariantOptions;
    }

    /**
     * This method is used to get Variant Assembly Id for Root Part.
     * @param context
     * @param ObjectId
     * @param ProductConfigurationId
     * @throws Exception
     * @author Chintan DADHANIA
     * @Modified By psalunke : TIGTK-9897
     */

    public String getVariantOptionsForPart(Context context, String args[]) throws Exception {
        logger.error("pss.diversity.StructureNodeUtil : getVariantOptionsForPart() : START ");
        String strVariantOptions = DomainConstants.EMPTY_STRING;
        try {
            String strObjectId = args[0];
            String strProductConfigurationId = args[1];

            if (UIUtil.isNotNullAndNotEmpty(strObjectId) && UIUtil.isNotNullAndNotEmpty(strProductConfigurationId)) {
                // Create Domain Object of Root Part.
                DomainObject domPartObject = DomainObject.newInstance(context, strObjectId);
                String strStructureNodValue = domPartObject.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_STRUCTURENODE);
                if (strStructureNodValue.equalsIgnoreCase("Yes")) {
                    // Object Select List
                    StringList slObjectSelect = new StringList(2);
                    slObjectSelect.add(DomainConstants.SELECT_NAME);
                    slObjectSelect.add(DomainConstants.SELECT_ID);

                    // Relationship Select List
                    StringList slRelSelect = new StringList(2);
                    slRelSelect.add(DomainConstants.SELECT_ID);
                    slRelSelect.add(TigerConstants.SELECT_ATTRIBUTE_PSS_VARIANTOPTIONS);

                    // Object Where clause
                    StringBuffer sbObjectWhere = new StringBuffer();
                    sbObjectWhere.append("from[");
                    sbObjectWhere.append(TigerConstants.RELATIONSHIP_PSS_VARIANT_ASSEMBLY_PRODUCT_CONFIGURATION);
                    sbObjectWhere.append("].to.id== '");
                    sbObjectWhere.append(strProductConfigurationId);
                    sbObjectWhere.append("'");
                    // Get Variant Assembly Id which is connected with Part and Product Configuration Id

                    MapList mlVariantAssembly = domPartObject.getRelatedObjects(context, // Context
                            TigerConstants.RELATIONSHIP_PSS_PARTVARIANTASSEMBLY, // Relationship Pattern
                            TigerConstants.TYPE_PSS_VARIANTASSEMBLY, // Type Pattern
                            slObjectSelect, // Object Select
                            DomainConstants.EMPTY_STRINGLIST, // Relationship Select
                            false, // To Side
                            true, // from Side
                            (short) 0, // Recursion Level
                            sbObjectWhere.toString(), // Object Where clause
                            DomainConstants.EMPTY_STRING, // Relationship Where clause
                            0, // Limit
                            null, // Post Relationship Patten
                            null, // Post Type Pattern
                            null); // Post Patterns

                    if (mlVariantAssembly.size() > 0) {
                        String strVariantAssemblyID = (String) ((Map) mlVariantAssembly.get(0)).get(DomainConstants.SELECT_ID);
                        DomainObject domProd = new DomainObject(strVariantAssemblyID);
                        sbObjectWhere = new StringBuffer();
                        sbObjectWhere.append("id==");
                        sbObjectWhere.append(strProductConfigurationId);

                        MapList mlProduct = domProd.getRelatedObjects(context, // Context
                                TigerConstants.RELATIONSHIP_PSS_VARIANT_ASSEMBLY_PRODUCT_CONFIGURATION, // Relationship Pattern
                                TigerConstants.TYPE_PRODUCTCONFIGURATION, // Type Pattern
                                DomainConstants.EMPTY_STRINGLIST, // Object Select
                                slRelSelect, // Relationship Select
                                false, // To Side
                                true, // from Side
                                (short) 0, // Recursion Level
                                sbObjectWhere.toString(), // Object Where clause
                                DomainConstants.EMPTY_STRING, // Relationship Where clause
                                0, // Limit
                                null, // Post Relationship Patten
                                null, // Post Type Pattern
                                null); // Post Patterns

                        // Result MapList size will 0 or 1.
                        // if size is greator than 0 then get Variant Option from first Map and add it to Return StringList.

                        if (mlProduct.size() > 0) {
                            strVariantOptions = (String) ((Map) mlProduct.get(0)).get(TigerConstants.SELECT_ATTRIBUTE_PSS_VARIANTOPTIONS);
                        } else {
                            strVariantOptions = DomainConstants.EMPTY_STRING;
                        }
                    }
                } // Root Part is Structure Node if check end
            } // PC and Root Part id if check
            logger.error("pss.diversity.StructureNodeUtil : getVariantOptionsForPart() : END ");
        } catch (Exception e) {
            logger.error("pss.diversity.StructureNodeUtil : getVariantOptionsForPart() : ERROR ", e.toString());
        }
        return strVariantOptions;
    }

    /**
     * This method displays the Variant Description.
     * @param context
     * @param args
     * @throws Exception
     * @author Suchit Gangurde
     */
    public StringList displayVariantDescription(Context context, String[] args) throws Exception {
        // declare common used variable
        Map tempMap = null;
        String strStructureNodeValue = "", strVariantDescription = "", strObjectId = "";

        // return StringList
        StringList slReturnVariantDescriptions = new StringList();

        // get ObjectList and Product Configuration Id
        Map programMap = (Map) JPO.unpackArgs(args);
        Map paramList = (Map) programMap.get("paramList");

        MapList mlObjectList = (MapList) programMap.get("objectList");

        String[] ObjectIdsArray = new String[mlObjectList.size()];

        for (int i = 0; i < mlObjectList.size(); i++) {
            // convert StringList to String array to pass it in getInfo.
            ObjectIdsArray[i] = (String) ((Map) mlObjectList.get(i)).get(DomainConstants.SELECT_ID);
        }

        String strProductConfigurationId = (String) paramList.get("PSS_ProductConfigurationFilter_OID");

        // get Actual Name of Attribute,Relationship from Symbolic name

        // Object Select List
        StringList slObjectSelect = new StringList();
        slObjectSelect.add(TigerConstants.SELECT_ATTRIBUTE_PSS_STRUCTURENODE);

        // Call getInfo to get StructureNode Attribute value for all parts.
        MapList mlStructureNode = DomainObject.getInfo(context, ObjectIdsArray, slObjectSelect);

        int intStructureNodeCount = ObjectIdsArray.length;

        String[] strParamArray = new String[3];

        strParamArray[1] = strProductConfigurationId;

        strParamArray[2] = DomainConstants.SELECT_DESCRIPTION;

        // Iterate Structure Node.
        for (int i = 0; i < intStructureNodeCount; i++) {

            // get Current Map
            tempMap = (Map) mlStructureNode.get(i);

            // Get PSS_StructureNode attribute value.
            strStructureNodeValue = (String) tempMap.get(TigerConstants.SELECT_ATTRIBUTE_PSS_STRUCTURENODE);

            if ("Yes".equalsIgnoreCase(strStructureNodeValue)) {
                strObjectId = (String) ObjectIdsArray[i];

                // set current Part Object Id into Array
                strParamArray[0] = strObjectId;

                strVariantDescription = getVariantAssemblyDetails(context, strParamArray);
                slReturnVariantDescriptions.add(strVariantDescription);

            } else {
                slReturnVariantDescriptions.add("");
            }

        }
        return slReturnVariantDescriptions;
    }

    /**
     * This method updates the Variant Description.
     * @param context
     * @param args
     * @throws Exception
     * @author Suchit Gangurde
     */
    public void updateVariantDescription(Context context, String[] args) throws Exception {

        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap RequestMap = (HashMap) programMap.get("requestMap");
            HashMap paramMap = (HashMap) programMap.get("paramMap");

            // TIGTK-6122 - 03-04-2017 - VP - START
            String strProductConfigId = (String) RequestMap.get("CFFExpressionFilterInput_OID");
            // TIGTK-6122 - 03-04-2017 - VP - END

            String strPartId = (String) paramMap.get("objectId");

            String strNewDescriptionValue = (String) paramMap.get("New Value");

            DomainObject domPartObject = DomainObject.newInstance(context, strPartId);
            StringList slObjectSelect = new StringList();
            slObjectSelect.add(DomainConstants.SELECT_ID);

            // TIGTK-5914 - 30-03-2017 - VP - START
            String objectWhere = "from[" + TigerConstants.RELATIONSHIP_PSS_VARIANT_ASSEMBLY_PRODUCT_CONFIGURATION + "].to.id == '" + strProductConfigId + "' && revision == 'last'";
            MapList mlVariantAssemblyObject = domPartObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_PARTVARIANTASSEMBLY, TigerConstants.TYPE_PSS_VARIANTASSEMBLY, slObjectSelect,
                    DomainConstants.EMPTY_STRINGLIST, false, true, (short) 1, objectWhere, DomainConstants.EMPTY_STRING, 0);
            // TIGTK-5914 - 30-03-2017 - VP - END

            String sVariantAssemblyId = "";
            if (mlVariantAssemblyObject.size() > 0) {
                sVariantAssemblyId = (String) ((Map) mlVariantAssemblyObject.get(0)).get(DomainConstants.SELECT_ID);
            }

            if (UIUtil.isNotNullAndNotEmpty(sVariantAssemblyId)) {

                DomainObject domVariantAssyObj = DomainObject.newInstance(context, sVariantAssemblyId);
                domVariantAssyObj.setDescription(context, strNewDescriptionValue);
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in updateVariantDescription: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
    }

    /**
     * This method gives the cell level edit access for Variant Description.
     * @param context
     * @param args
     * @throws Exception
     * @author Suchit Gangurde
     */
    public StringList getCellLevelEditAccessForVariantDescription(Context context, String[] args) throws Exception {

        StringList slReturnVariantEditable = new StringList();
        try {
            StringList lstVariantIDs = displayVariantAssemblyID(context, args);

            for (int i = 0; i < lstVariantIDs.size(); i++) {
                String strVariantID = (String) lstVariantIDs.get(i);
                if (UIUtil.isNullOrEmpty(strVariantID)) {
                    slReturnVariantEditable.add(false);
                } else
                    slReturnVariantEditable.add(true);
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getCellLevelEditAccessForVariantDescription: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
        return slReturnVariantEditable;
    }

    // DIVERSITY:PHASE2.0 : TIGTK-9119 & TIGTK-9146 : PSE : 09-08-2017 : START
    /**
     * This method is used to restrict removal of Configuration Feature if any of the Product's Configuration Feature/Option structure is used in Effectivity or Product Configuration
     * @name checkIfCFStructureUsedInEffectivityOrProductConfiguration
     * @param context
     *            the Matrix Context
     * @throws Exception
     *             if the operation fails
     * @Created By : psalunke
     * @Created on : 09-Aug-2017
     */
    public int checkIfCFStructureUsedInEffectivityOrProductConfiguration(Context context, String args[]) throws Exception {

        int iReturn = 0;
        try {
            int iEffectivityReturn = 0;
            int iProductConfigurationReturn = 0;
            String strToSideObjectId = args[1];
            String strFromSideObjectId = args[2];
            DomainObject domToSideObject = DomainObject.newInstance(context, strToSideObjectId);
            String strToSideObjectType = domToSideObject.getInfo(context, DomainConstants.SELECT_TYPE);
            // if to side type is HARDWARE_PRODUCT then do action
            if (strToSideObjectType.equals(TigerConstants.TYPE_HARDWARE_PRODUCT)) {
                StringList slSelectStmts = new StringList(); // object selects
                slSelectStmts.addElement(DomainConstants.SELECT_ID);

                StringList slRelSelects = new StringList(); // relationship selects
                slRelSelects.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
                slRelSelects.addElement(TigerConstants.SELECT_CONFIGURATIONFEATURE_ID_FROM_EFFECTIVITY);

                // Rel Pattern
                Pattern relPattern = new Pattern(TigerConstants.RELATIONSHIP_GBOM);
                relPattern.addPattern(TigerConstants.RELATIONSHIP_EBOM);

                // Get EBOM Structure with Configuration Features of Hardware Product

                ExpansionIterator expandIter = domToSideObject.getExpansionIterator(context, relPattern.getPattern(), // relationship pattern
                        TigerConstants.TYPE_PART, // type pattern
                        slSelectStmts, // object select
                        slRelSelects, // rel select
                        false, // to direction
                        true, // from direction
                        (short) 0, // recusrion level
                        DomainConstants.EMPTY_STRING, // object where clause
                        DomainConstants.EMPTY_STRING, // relationship where clause
                        (short) 0, false, true, (short) 1, false);

                MapList mlEBOMList = FrameworkUtil.toMapList(expandIter, (short) 0, null, null, null, null);

                if (mlEBOMList != null && !mlEBOMList.isEmpty()) {
                    Iterator iteratorEBOM = mlEBOMList.iterator();
                    while (iteratorEBOM.hasNext()) {
                        Map mEBOMMap = (Map) iteratorEBOM.next();

                        Object objConfiguratinFeatures = (Object) mEBOMMap.get(TigerConstants.SELECT_CONFIGURATIONFEATURE_ID_FROM_EFFECTIVITY);

                        StringList slConfigurationFeatureIds = new StringList();
                        if (objConfiguratinFeatures != null) {
                            if (objConfiguratinFeatures instanceof StringList) {
                                slConfigurationFeatureIds = (StringList) objConfiguratinFeatures;
                            } else {
                                slConfigurationFeatureIds.addElement((String) objConfiguratinFeatures);
                            }
                        }

                        if (!slConfigurationFeatureIds.isEmpty() && slConfigurationFeatureIds.contains(strFromSideObjectId)) {
                            iEffectivityReturn = 1;
                            break;
                        }
                    }
                }

                // If Configuration Feature is not used in any effectivity then check for Product Configuration expression
                if (iEffectivityReturn == 0) {
                    // Get Product Configurations with expression
                    StringList slObjectSelects = new StringList();
                    slObjectSelects.addElement(DomainConstants.SELECT_ID);
                    slObjectSelects.addElement(TigerConstants.SELECT_CONFIGURATIONFEATURE_ID_FROM_PRODUCTCONFIGURATION);

                    MapList mlProductConfigurationList = domToSideObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_FEATUREPRODUCTCONFIGURATION, // relationship
                            TigerConstants.TYPE_PRODUCTCONFIGURATION, // types to fetch from other end
                            false, // getTO
                            true, // getFrom
                            1, // recursionTo level
                            slObjectSelects, // object selects
                            new StringList(DomainConstants.SELECT_RELATIONSHIP_ID), // relationship selects
                            DomainConstants.EMPTY_STRING, // object where
                            DomainConstants.EMPTY_STRING, // relationship where
                            0, // limit
                            DomainConstants.EMPTY_STRING, // post rel pattern
                            DomainConstants.EMPTY_STRING, // post type pattern
                            null); // post patterns
                    if (mlProductConfigurationList != null && !mlProductConfigurationList.isEmpty()) {
                        Iterator iteratorProductConfiguration = mlProductConfigurationList.iterator();
                        while (iteratorProductConfiguration.hasNext()) {
                            Map mProductConfigurationMap = (Map) iteratorProductConfiguration.next();
                            Object objConfiguratinFeatures = (Object) mProductConfigurationMap.get(TigerConstants.SELECT_CONFIGURATIONFEATURE_ID_FROM_PRODUCTCONFIGURATION);

                            StringList slConfiguratinFeatures = new StringList();
                            if (objConfiguratinFeatures != null) {
                                if (objConfiguratinFeatures instanceof StringList) {
                                    slConfiguratinFeatures = (StringList) objConfiguratinFeatures;
                                } else {
                                    slConfiguratinFeatures.addElement((String) objConfiguratinFeatures);
                                }
                            }

                            if (!slConfiguratinFeatures.isEmpty()) {
                                if (slConfiguratinFeatures.contains(strFromSideObjectId)) {
                                    iProductConfigurationReturn = 1;
                                    break;
                                }
                            }

                        }
                    }
                }
                if (iEffectivityReturn == 1 || iProductConfigurationReturn == 1) {
                    iReturn = 1;
                    String errorMessage = EnoviaResourceBundle.getProperty(context, "emxConfigurationStringResource", context.getLocale(),
                            "emxConfigurationStringResource.Error.EffectivityUsageCannotDelete");
                    MqlUtil.mqlCommand(context, "notice $1", errorMessage);
                }
            }
        } catch (RuntimeException e) {
            iReturn = 1;
            logger.error("Error in pss.diversity.StructureNodeUtil : checkIfCFStructureUsedInEffectivityOrProductConfiguration(): ", e);
            throw e;
        } catch (Exception e) {
            iReturn = 1;
            logger.error("Error in pss.diversity.StructureNodeUtil : checkIfCFStructureUsedInEffectivityOrProductConfiguration(): ", e);
            throw e;
        }
        return iReturn;
    }
    // DIVERSITY:PHASE2.0 : TIGTK-9119 & TIGTK-9146 : PSE : 09-08-2017 : END

    /**
     * This method is used to get Variant Assembly Id Column value for Product Configuration Filtering.
     * @param context
     *            , args
     * @throws Exception
     * @since 20-09-2017
     * @author Priyanka SALUNKE : TIGTK-9897
     */
    public Vector displayVariantIDForPCFiltering(Context context, String[] args) throws Exception {
        logger.error("pss.diversity.StructureNodeUtil : displayVariantIDForPCFiltering() : START ");
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            Map paramList = (Map) programMap.get("paramList");
            MapList mlProductConfigurationList = (MapList) programMap.get("objectList");
            Vector vVariantAssemblyIds = new Vector(mlProductConfigurationList.size());

            // Get Parent Part object id
            String strParentPartId = (String) paramList.get("parentOID");
            if (UIUtil.isNotNullAndNotEmpty(strParentPartId)) {
                DomainObject domParentPart = DomainObject.newInstance(context, strParentPartId);
                String strParentStructureNodeValue = domParentPart.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_STRUCTURENODE);
                if (strParentStructureNodeValue.equalsIgnoreCase("Yes")) {
                    if (!mlProductConfigurationList.isEmpty()) {
                        // Argument array
                        String[] strParamArray = new String[3];
                        // Root Part Id
                        strParamArray[0] = strParentPartId;
                        strParamArray[2] = DomainConstants.SELECT_NAME;

                        // Iterate Structure Node.
                        for (int i = 0; i < mlProductConfigurationList.size(); i++) {
                            Map mPCInfoMap = (Map) mlProductConfigurationList.get(i);
                            String strProductConfigurationObjectId = (String) mPCInfoMap.get(DomainConstants.SELECT_ID);
                            // Product Configuration Id
                            strParamArray[1] = strProductConfigurationObjectId;

                            String strVariantId = getVariantAssemblyDetails(context, strParamArray);

                            if (UIUtil.isNotNullAndNotEmpty(strVariantId)) {
                                vVariantAssemblyIds.add(strVariantId);
                            } else {
                                vVariantAssemblyIds.add("");
                            }

                        } // for end

                    } // PC map list empty check if end

                } // Parent Part S/N value check if end

            } // Part id null check if end
            logger.error("pss.diversity.StructureNodeUtil : displayVariantIDForPCFiltering() : END ");
            return vVariantAssemblyIds;
        } catch (Exception e) {
            logger.error("pss.diversity.StructureNodeUtil : displayVariantIDForPCFiltering() : ERROR ", e.toString());
            throw e;
        }
    }
}
