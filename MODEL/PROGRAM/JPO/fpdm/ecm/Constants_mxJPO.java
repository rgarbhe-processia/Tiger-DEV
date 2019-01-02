package fpdm.ecm;

import java.util.HashMap;
import java.util.Map;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.PropertyUtil;

import matrix.db.Context;
import matrix.util.MatrixException;

public class Constants_mxJPO implements DomainConstants {

    // CONSTANTS
    public static final String VAULT_PRODUCTION = "eService Production";

    public static final String SELECT_PHYSICALID = "physicalid";

    public static final String ATTRIBUTE_PSS_CN_TYPE_RANGE_PARTS_AND_BOM = "Parts and BOM";

    public static final String ATTRIBUTE_PSS_CN_TYPE_RANGE_TOOL = "Tools";

    public static final String ATTRIBUTE_PSS_CN_TYPE_RANGE_PARTS = "Parts";

    public static final String ATTRIBUTE_PSS_TRANSFER_TO_SAP_EXPECTED_RNAGE_YES = "Yes";

    public static final String ATTRIBUTE_PSS_TRANSFER_TO_SAP_EXPECTED_RANGE_NO = "No";

    // SYMBOILC NAME
    // TYPES
    public static final String SYMBOLIC_type_FPDM_MBOMPart = "type_FPDM_MBOMPart";

    public static final String SYMBOLIC_type_VPMReference = "type_VPMReference";

    public static final String SYMBOLIC_type_PSS_ChangeNotice = "type_PSS_ChangeNotice";

    // RELATIONSHIPS
    public static final String SYMBOLIC_relationship_FPDM_CNAffectedItems = "relationship_FPDM_CNAffectedItems";

    public static final String SYMBOLIC_relationship_PSS_RelatedCN = "relationship_PSS_RelatedCN";

    public static final String SYMBOLIC_relationship_FPDM_GeneratedMBOM = "relationship_FPDM_GeneratedMBOM";

    public static final String SYMBOLIC_relationship_PSS_MfgChangeAction = "relationship_PSS_ManufacturingChangeAction";

    public static final String SYMBOLIC_relationship_PSS_MfgChangeAffectedItem = "relationship_PSS_ManufacturingChangeAffectedItem";

    public static final String SYMBOLIC_relationship_PSS_Related150MBOM = "relationship_PSS_Related150MBOM";

    public static final String SYMBOLIC_relationship_PSS_ConnectedPCMData = "relationship_PSS_ConnectedPCMData";

    public static final String SYMBOLIC_relationship_PSS_MfgRelatedPlant = "relationship_PSS_ManufacturingRelatedPlant";

    public static final String SYMBOLIC_relationship_FPDM_ScopeLink = "relationship_FPDM_ScopeLink";

    public static final String SYMBOLIC_relationship_FPDM_MBOMRel = "relationship_FPDM_MBOMRel";

    // ATTRIBUTES
    public static final String SYMBOLIC_attribute_PSS_CN_Type = "attribute_PSS_CN_Type";

    public static final String SYMBOLIC_attribute_PSS_Transfer_To_SAP_Expected = "attribute_PSS_Transfer_To_SAP_Expected";

    public static final String SYMBOLIC_attribute_PSS_Effectivity_Date = "attribute_PSS_Effectivity_Date";

    public static final String SYMBOLIC_attribute_FPDM_EffectivityDate = "attribute_FPDM_EffectivityDate";

    public static final String SYMBOLIC_attribute_PLMReference_V_ApplicabilityDate = "attribute_PLMReference.V_ApplicabilityDate";

    public static final String SYMBOLIC_attribute_PSS_CN_Transfer_Date = "attribute_PSS_CN_Transfer_Date";

    // POLICIES
    public static final String SYMBOLIC_policy_PSS_MfgChangeOrder = "policy_PSS_ManufacturingChangeOrder";

    public static final String SYMBOLIC_policy_PSS_Tool = "policy_PSS_Tool";

    public static final String SYMBOLIC_policy_PSS_ChangeNotice = "policy_PSS_ChangeNotice";

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
}
