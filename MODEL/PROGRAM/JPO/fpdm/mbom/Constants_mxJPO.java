package fpdm.mbom;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.PropertyUtil;

import matrix.db.Context;
import matrix.util.MatrixException;
import matrix.util.StringList;

public class Constants_mxJPO implements DomainConstants {

    // CONSTANTS
    public static final String VAULT_PRODUCTION = "eService Production";

    // the attribute PSS_ManufacturingItemExt.PSS_SAPPartNumber ranges values
    public static final String STR_CUSTOMER_PART_NUMBER = "Customer Part Number";

    public static final Pattern patTigerMaterial = Pattern.compile("(M[0-9]{6})");

    public static final Pattern patTigerPart = Pattern.compile("([0-9]{7})");

    public static final Pattern patLegacyPartWithX = Pattern.compile("([0-9]{7}X)");

    public static final Pattern patLegacyFASMaterail = Pattern.compile("MAT-([0-9]{6})");

    public static final String STR_SUPPLIER_PART_NUMBER = "Supplier Part Number";

    public static final String STR_FAURECIA_PART_NUMBER = "Faurecia Part Number";

    public static final String STR_COLOR_NOT_ASSIGNED = "N/A";

    public static final String STR_COLOR_IGNORE = "Ignore";

    public static final String STR_COLOR_BLANK = "";

    public static final String STR_QUALITY_CHECK_PASS = "PASS";

    public static final String STR_QUALITY_CHECK_FAIL = "FAIL";

    public static final String SELECT_PHYSICALID = "physicalid";

    public static final String SELECT_LOGICALID = "logicalid";

    public static final String SELECT_FROM_PHYSICALID = "from.physicalid";

    public static final StringList MBOM150_BUS_SELECT = new StringList(
            new String[] { SELECT_LEVEL, SELECT_PHYSICALID, SELECT_LOGICALID, SELECT_TYPE, SELECT_NAME, SELECT_REVISION, SELECT_ID, "attribute.value" });

    public static final StringList MBOM150_REL_SELECT = new StringList(new String[] { "physicalid[connection]", "logicalid[connection]", SELECT_FROM_PHYSICALID, "attribute.value" });

    public static final String STR_HarmonyAssociationRelId = "HarmonyAssociationRelId";

    public static final String STR_HarmonyId = "HarmonyId";

    public static final String STR_PSS_ColorPID = "PSS_ColorPID";

    public static final String STR_PSS_ProductConfigurationPID = "PSS_ProductConfigurationPID";

    public static final String STR_PSS_CustomerPartNumber = "PSS_CustomerPartNumber";

    public static final String STR_PSS_OldMaterialNumber = "PSS_OldMaterialNumber";

    public static final String STR_PSS_VariantAssemblyPID = "PSS_VariantAssemblyPID";

    public static final String STR_Quantity = "Quantity";

    public static final String STR_HarmonyAssociationRelInfos = "HarmonyAssociationRelInfos";

    public static final String SELECT_PRODUCT_CONFIGURATION_NAME = SELECT_NAME;

    public static final String SELECT_PRODUCT_CONFIGURATION_DESCRIPTION = SELECT_DESCRIPTION;

    // SYMBOILC NAME
    // TYPES
    public static final String SYMBOLIC_type_FPDM_MBOMPart = "type_FPDM_MBOMPart";

    public static final String SYMBOLIC_type_FPDM_MBOMMaterial = "type_FPDM_MBOMMaterial";

    public static final String SYMBOLIC_type_Plant = "type_Plant";

    public static final String SYMBOLIC_type_PSS_Harmony = "type_PSS_Harmony";

    public static final String SYMBOLIC_type_PSS_ChangeNotice = "type_PSS_ChangeNotice";

    public static final String SYMBOLIC_type_PSS_MBOMVariantAssembly = "type_PSS_MBOMVariantAssembly";

    public static final String SYMBOLIC_type_DELFmiFunctionPPRContinuousReference = "type_DELFmiFunctionPPRContinuousReference";

    public static final String SYMBOLIC_type_PSS_Operation = "type_PSS_Operation";

    public static final String SYMBOLIC_type_PSS_LineData = "type_PSS_LineData";

    public static final String SYMBOLIC_type_MfgProductionPlanning = "type_MfgProductionPlanning";

    // RELATIONSHIPS
    public static final String SYMBOLIC_relationship_FPDM_ScopeLink = "relationship_FPDM_ScopeLink";

    public static final String SYMBOLIC_relationship_FPDM_GeneratedMBOM = "relationship_FPDM_GeneratedMBOM";

    public static final String SYMBOLIC_relationship_FPDM_MBOMRel = "relationship_FPDM_MBOMRel";

    public static final String SYMBOLIC_relationship_DELFmiFunctionIdentifiedInstance = "relationship_DELFmiFunctionIdentifiedInstance";

    public static final String SYMBOLIC_relationship_PSS_HarmonyAssociation = "relationship_PSS_HarmonyAssociation";

    public static final String SYMBOLIC_relationship_FPDM_CNAffectedItems = "relationship_FPDM_CNAffectedItems";

    public static final String SYMBOLIC_relationship_PSS_PartVariantAssembly = "relationship_PSS_PartVariantAssembly";

    public static final String SYMBOLIC_relationship_ProcessInstanceContinuous = "relationship_ProcessInstanceContinuous";

    public static final String SYMBOLIC_relationship_VPLMrelPLMConnectionV_Owner = "relationship_VPLMrel@PLMConnection@V_Owner";

    // ATTRIBUTES
    public static final String SYMBOLIC_attribute_FPDM_FCSReference = "attribute_FPDM_FCSReference";

    public static final String SYMBOLIC_attribute_FPDM_EffectivityDate = "attribute_FPDM_EffectivityDate";

    public static final String SYMBOLIC_attribute_FPDM_ProductConfigurationId = "attribute_FPDM_ProductConfigurationId";

    public static final String SYMBOLIC_attribute_FPDM_Harmony = "attribute_FPDM_Harmony";

    public static final String SYMBOLIC_attribute_FPDM_Synchronized = "attribute_FPDM_Synchronized";

    public static final String SYMBOLIC_attribute_FPDM_CustomerPartNumber = "attribute_FPDM_CustomerPartNumber";

    public static final String SYMBOLIC_attribute_FPDM_OldMaterialNumber = "attribute_FPDM_OldMaterialNumber";

    public static final String SYMBOLIC_attribute_FPDM_GenratedFrom = "attribute_FPDM_GenratedFrom";

    public static final String SYMBOLIC_attribute_PSS_ColorCode = "attribute_PSS_ColorCode";

    public static final String SYMBOLIC_attribute_PLMEntity_V_Name = "attribute_PLMEntity.V_Name";

    public static final String SYMBOLIC_attribute_PLMReference_V_ApplicabilityDate = "attribute_PLMReference.V_ApplicabilityDate";

    public static final String SYMBOLIC_attribute_PSS_ManufacturingInstanceExt_PSS_PhantomLevel = "attribute_PSS_ManufacturingInstanceExt.PSS_PhantomLevel";

    public static final String SYMBOLIC_attribute_PSS_ManufacturingInstanceExt_PSS_PhantomPart = "attribute_PSS_ManufacturingInstanceExt.PSS_PhantomPart";

    public static final String SYMBOLIC_attribute_PSS_ManufacturingItemExt_PSS_QualityCheck = "attribute_PSS_ManufacturingItemExt.PSS_QualityCheck";

    public static final String SYMBOLIC_attribute_PSS_ManufacturingItemExt_PSS_SAPPartNumber = "attribute_PSS_ManufacturingItemExt.PSS_SAPPartNumber";

    public static final String SYMBOLIC_attribute_PSS_ManufacturingItemExt_PSS_CustomerPartNumber = "attribute_PSS_ManufacturingItemExt.PSS_CustomerPartNumber";

    public static final String SYMBOLIC_attribute_PSS_ManufacturingItemExt_PSS_SupplierPartNumber = "attribute_PSS_ManufacturingItemExt.PSS_CustomerPartNumber";

    public static final String SYMBOLIC_attribute_PSS_ManufacturingItemExt_PSS_FCSIndex = "attribute_PSS_ManufacturingItemExt.PSS_FCSIndex";

    public static final String SYMBOLIC_attribute_PSS_PublishedEBOM_PSS_EBOMFindNumber = "attribute_PSS_PublishedEBOM.PSS_EBOMFindNumber";

    public static final String SYMBOLIC_attribute_PSS_PublishedEBOM_PSS_EBOMQuantity = "attribute_PSS_PublishedEBOM.PSS_EBOMQuantity";

    public static final String SYMBOLIC_attribute_PSS_PublishedPart_PSS_PartUnitofMeasure = "attribute_PSS_PublishedPart.PSS_PartUnitofMeasure";

    public static final String SYMBOLIC_attribute_FPDM_Quantity = "attribute_FPDM_Quantity";

    public static final String SYMBOLIC_attribute_FPDM_Unit = "attribute_FPDM_Unit";

    public static final String SYMBOLIC_attribute_FPDM_FindNumber = "attribute_FPDM_FindNumber";

    public static final String SYMBOLIC_attribute_FPDM_PhantomPart = "attribute_FPDM_PhantomPart";

    public static final String SYMBOLIC_attribute_PSS_ProductConfigurationPID = "attribute_PSS_ProductConfigurationPID";

    public static final String SYMBOLIC_PSS_ManufacturingInstanceExt_PSS_Harmonies = "attribute_PSS_ManufacturingInstanceExt.PSS_Harmonies";

    public static final String SYMBOLIC_attribute_FPDM_CustomerDescription = "attribute_FPDM_CustomerDescription";

    public static final String SYMBOLIC_attribute_FPDM_PDMClass = "attribute_FPDM_PDMClass";

    public static final String SYMBOLIC_attribute_FPDM_GrossWeight = "attribute_FPDM_GrossWeight";

    public static final String SYMBOLIC_attribute_FPDM_NetWeight = "attribute_FPDM_NetWeight";

    public static final String SYMBOLIC_attribute_FPDM_FaureciaShortLengthDescriptionFCS = "attribute_FPDM_FaureciaShortLengthDescriptionFCS";

    public static final String SYMBOLIC_attribute_FPDM_AlternativeDescription = "attribute_FPDM_AlternativeDescription";

    public static final String SYMBOLIC_attribute_FPDM_TypeOfPart = "attribute_FPDM_TypeOfPart";

    public static final String SYMBOLIC_attribute_PSS_ManufacturingItemExt_PSS_CustomerDescription = "attribute_PSS_ManufacturingItemExt.PSS_CustomerDescription";

    public static final String SYMBOLIC_attribute_PSS_ManufacturingItemExt_PSS_PDMClass = "attribute_PSS_ManufacturingItemExt.PSS_PDMClass";

    public static final String SYMBOLIC_attribute_PSS_ManufacturingItemExt_PSS_FCSClassCategory = "attribute_PSS_ManufacturingItemExt.PSS_FCSClassCategory";

    public static final String SYMBOLIC_attribute_PSS_ManufacturingItemExt_PSS_GrossWeight = "attribute_PSS_ManufacturingItemExt.PSS_GrossWeight";

    public static final String SYMBOLIC_attribute_PSS_ManufacturingItemExt_PSS_NetWeight = "attribute_PSS_ManufacturingItemExt.PSS_NetWeight";

    public static final String SYMBOLIC_attribute_PSS_ManufacturingItemExt_PSS_FaureciaShortLengthDescriptionFCS = "attribute_PSS_ManufacturingItemExt.PSS_FaureciaShortLengthDescriptionFCS";

    public static final String SYMBOLIC_attribute_PSS_ManufacturingItemExt_PSS_AlternativeDescription = "attribute_PSS_ManufacturingItemExt.PSS_AlternativeDescription";

    public static final String SYMBOLIC_attribute_PSS_ManufacturingInstanceExt_PSS_TypeOfPart = "attribute_PSS_ManufacturingInstanceExt.PSS_TypeOfPart";

    public static final String SYMBOLIC_attribute_PLMEntity_V_description = "attribute_PLMEntity.V_description";

    public static final String SYMBOLIC_attribute_V_Name = "attribute_V_Name";

    public static final String SYMBOLIC_attribute_PSS_ColorPID = "attribute_PSS_ColorPID";

    public static final String SYMBOLIC_attribute_PSS_ManufacturingItemExt_PSS_Harmonies = "attribute_PSS_ManufacturingItemExt.PSS_Harmonies";

    public static final String SYMBOLIC_attribute_PSS_ManufacturingUoMExt_PSS_UnitOfMeasure = "attribute_PSS_ManufacturingUoMExt.PSS_UnitOfMeasure";

    public static final String SYMBOLIC_attribute_PLMInstance_V_TreeOrder = "attribute_PLMInstance.V_TreeOrder";

    public static final String SYMBOLIC_attribute_PSS_VariantID = "attribute_PSS_VariantID";

    public static final String SYMBOLIC_attribute_DisplayName = "attribute_DisplayName";

    public static final String SYMBOLIC_attribute_PSS_NetWeight = "attribute_PSS_NetWeight";

    public static final String SYMBOLIC_attribute_PSS_GrossWeight = "attribute_PSS_GrossWeight";

    public static final String SYMBOLIC_attribute_Quantity = "attribute_Quantity";

    public static final String SYMBOLIC_attribute_PSS_VariantAssemblyPID = "attribute_PSS_VariantAssemblyPID";

    public static final String SYMBOLIC_attribute_PSS_OldMaterialNumber = "attribute_PSS_OldMaterialNumber";

    public static final String SYMBOLIC_attribute_PSS_CustomerPartNumber = "attribute_PSS_CustomerPartNumber";

    // POLICIES
    public static final String SYMBOLIC_policy_FPDM_MBOM100 = "policy_FPDM_MBOM100";

    public static final String sRegExpHarmonyAssociationRelId = String.format("%s:(.*?),", STR_HarmonyAssociationRelId);

    public static final String sRegExpHarmonyID = String.format("%s:(.*?),", STR_HarmonyId);

    public static final String sRegExpColorPID = String.format("%s:(.*?),", STR_PSS_ColorPID);

    public static final String sRegExpProductConfigurationPID = String.format("%s:(.*?),", STR_PSS_ProductConfigurationPID);

    public static final String sRegExpVariantAssemblyPID = String.format("%s:(.*?),", STR_PSS_VariantAssemblyPID);

    public static final String sRegExpCustomerPartNumber = String.format("%s:(.*?),", STR_PSS_CustomerPartNumber);

    public static final String sRegExpOldMaterialNumber = String.format("%s:(.*?),", STR_PSS_OldMaterialNumber);

    public static final String sRegExpQuantity = String.format("%s:(.*?),", STR_Quantity);

    // Schema properties values
    private static Map<String, String> mSchemaProperty = null;

    /**
     * get instance
     * @return
     */
    public static synchronized Map<String, String> getInstance() {
        return (mSchemaProperty != null ? mSchemaProperty : (mSchemaProperty = new HashMap<String, String>()));
    }

    /**
     * Return the schema value of a property.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sPropertyName
     *            property name (example: type_Part, relationship_EBOM, attribute_Originator)
     * @return
     * @throws FrameworkException
     */
    public static String getSchemaProperty(Context context, String sPropertyName) throws FrameworkException {
        Map<String, String> mSchemaProperty = getInstance();
        String sSchema = mSchemaProperty.get(sPropertyName);
        if (sSchema == null || "".equals(sSchema)) {
            sSchema = PropertyUtil.getSchemaProperty(context, sPropertyName);
            mSchemaProperty.put(sPropertyName, sSchema);
        }
        return sSchema;
    }

    /**
     * Get property name from a symbolic name. Check first if the property already exist on the static variable
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sType
     *            the business administrative type; e.g. policy, type, etc.
     * @param sName
     *            the business administrative name
     * @param sProperty
     *            the property alias name to retrieve (example: state_Preliminary)
     * @return
     * @throws MatrixException
     */
    public static String getSchemaProperty(Context context, String sType, String sName, String sProperty) throws MatrixException {
        Map<String, String> mSchemaProperty = getInstance();
        StringBuilder sbKey = new StringBuilder();
        sbKey.append(sType);
        sbKey.append("|");
        sbKey.append(sName);
        sbKey.append("|");
        sbKey.append(sProperty);
        String sSchema = mSchemaProperty.get(sbKey.toString());
        if (sSchema == null || "".equals(sSchema)) {
            sSchema = PropertyUtil.getSchemaProperty(context, sType, sName, sProperty);
            mSchemaProperty.put(sbKey.toString(), sSchema);
        }
        return sSchema;

    }

    /**
     * Extract Attribute value from a map
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sSymbloicAttrName
     *            Symbolic Attribute name
     * @param mInfo
     *            Object informations
     * @return
     * @throws FrameworkException
     */
    public static String getAttributeValueFromMap(Context context, String sSymbolicAttrName, Map<?, ?> mInfo) throws FrameworkException {
        String sAttrName = getSchemaProperty(context, sSymbolicAttrName);
        return (String) mInfo.get("attribute[" + sAttrName + "].value");
    }

    /**
     * Extract Basic Attribute value from a map. Example of basic attribute: type, name, current,...
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sBasicAttrName
     *            Basic Attribute name
     * @param mInfo
     *            Object informations
     * @return
     * @throws FrameworkException
     */
    public static String getBasicAttributeValueFromMap(Context context, String sBasicAttrName, Map<?, ?> mInfo) throws FrameworkException {
        return (String) mInfo.get(sBasicAttrName);
    }

}
