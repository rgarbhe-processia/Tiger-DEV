package fpdm.cad;

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

    // SYMBOILC NAME
    // TYPES
    public static final String SYMBOLIC_type_VIEWABLE = "type_Viewable";

    public static final String SYMBOLIC_type_PSS_CATPart = "type_PSS_CATPart";

    public static final String SYMBOLIC_type_PSS_UGModel = "type_PSS_UGModel";

    public static final String SYMBOLIC_type_CATIACGR = "type_CATIACGR";

    public static final String SYMBOLIC_type_CATIAV4Model = "type_CATIAV4Model";

    // RELATIONSHIPS
    public static final String SYMBOLIC_relationship_CADSubComponent = "relationship_CADSubComponent";

    public static final String SYMBOLIC_relationship_Viewable = "relationship_Viewable";

    // ATTRIBUTES
    public static final String SYMBOLIC_attribute_SpatialLocation = "attribute_SpatialLocation";

    // FORMATS
    public static final String SYMBOLIC_format_JT = "format_JT";

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
