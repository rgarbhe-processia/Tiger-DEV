// Bug Fix - TIGTK 1962 - Starts
package pss.diversity;

// Bug Fix - TIGTK 1962 - Ends
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import pss.constants.TigerConstants;

import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.effectivity.EffectivityFramework;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

public class emxEffectivityFramework_mxJPO {
    /**
     * Updates the Simpler Effectivity Expression for a relationship. Called as the update method from structure browser edit.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds a packed hashmap with the following arguments paramMap HashMap of parameter values- relId, New Value
     * @return Object - boolean true if the operation is successful
     * @throws Exception
     *             if the operation fails
     * @since CFF R209
     * @author Gautami Chaudhari
     * @date 23-03-2016
     * @Modified By : Priyanka Salunke on Date : 19/10/2016
     */

    // Function - getSimplifiedEffectivityExpression Ends
    public Object updateRelExpression(Context context, String[] args) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap paramMap = (HashMap) programMap.get("paramMap");
        // String objectId = (String) paramMap.get("objectId");
        String customExpression = (String) paramMap.get("New Value");
        // Added for TIGTK-3431 By Priyanka Salunke: 19-10-2016 : START
        String newExpression = customExpression.replace("||", "OR");
        paramMap.put("New Value", newExpression);
        // Added for TIGTK-3431 By Priyanka Salunke: 19-10-2016 : END

        String relId = (String) paramMap.get("relId");

        String strSimplifiedExpression = "";
        if (newExpression.length() > 0) {
            strSimplifiedExpression = getSimplifiedEffectivityExpression(context, newExpression);
        }

        EffectivityFramework ef = new EffectivityFramework();

        // Find Bug Issue : TIGTK-3953 : Priyanka Salunke : 25-Jan-2017 : START

        if (UIUtil.isNullOrEmpty(relId)) {
            // Find Bug Issue : TIGTK-3953 : Priyanka Salunke : 25-Jan-2017 : END
            String errorMessage = EnoviaResourceBundle.getProperty(context, "Effectivity", "Effectivity.Alert.rootNodeStructureEffectivity", context.getSession().getLanguage());
            throw new FrameworkException(errorMessage);
        }
        DomainRelationship domRel = new DomainRelationship(relId);
        domRel.open(context);
        String relType = domRel.getTypeName();
        String relAlias = FrameworkUtil.getAliasForAdmin(context, "relationship", relType, true);
        MapList relEffTypes = ef.getRelEffectivity(context, relAlias);
        EffectivityFramework.setRelEffectivityTypes(context, relId, relEffTypes);
        domRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_SIMPLEREFFECTIVITYEXPRESSION, strSimplifiedExpression);
        domRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CUSTOMEFFECTIVITYEXPRESSION, customExpression); // Custom Expression used to store for render purpose.
        return ef.updateRelExpression(context, JPO.packArgs(programMap));
    } // Function - getSimplifiedEffectivityExpression Ends

    /**
     * Gets the Simpler Effectivity Expression from the Original Expression for a relationship. Called as the update method from structure browser edit.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param holds
     *            the context and original Effectivity Expression
     * @return String - Simplified Effectivity expression
     * @throws Exception
     *             if the operation fails
     * @since CFF R209
     * @author Gautami Chaudhari
     * @date 22-03-2016
     * @Modified By : Priyanka Salunke on Date : 19/10/2016
     */

    // Function - getSimplifiedEffectivityExpression Starts
    public String getSimplifiedEffectivityExpression(Context context, String strExpression) throws Exception {
        StringBuilder sbSimpleExpression = new StringBuilder();
        // TIGTK-12718 : Added null or empty check to display empty effectivity when no effectivity set
        if (UIUtil.isNotNullAndNotEmpty(strExpression)) {
            strExpression = strExpression.replaceAll("@EF_FO\\(PHY@EF:", "@@@");
            strExpression = strExpression.replaceAll("\\(", "( ");
            strExpression = strExpression.replaceAll("\\)", " )");
            String[] strExpressionElem = strExpression.trim().split("\\s+");

            // TIGTK-3029 : 08-09-2016 : START
            sbSimpleExpression.append("(");
            // TIGTK-3029 : 08-09-2016 : END
            boolean trueElement = false;
            for (int itr = 0; itr < strExpressionElem.length; itr++) {
                String elementStr = strExpressionElem[itr].trim();
                if (UIUtil.isNotNullAndNotEmpty(elementStr)) {
                    if (elementStr.equals("AND")) {
                        // TIGTK-3029 : 08-09-2016 : START
                        sbSimpleExpression.append("&");
                        // TIGTK-3029 : 08-09-2016 : END
                    } else if (trueElement && elementStr.equals(")")) {
                        trueElement = false;
                        continue;
                    } else if (elementStr.equals("OR") || elementStr.equals("NOT") || elementStr.equals("(") || elementStr.equals(")")) {
                        sbSimpleExpression.append(strExpressionElem[itr]);
                    } else {
                        String[] strCORelPhyID = (elementStr.substring(elementStr.indexOf("@@@") + "@@@".length(), elementStr.lastIndexOf("~"))).split(",");

                        StringList strSelect = new StringList();
                        strSelect.add("to.attribute[Display Name]");
                        strSelect.add("from.attribute[Display Name]");

                        MapList select = DomainRelationship.getInfo(context, strCORelPhyID, strSelect);

                        Iterator it = select.iterator();
                        while (it.hasNext()) {
                            Map mp = (Map) it.next();
                            sbSimpleExpression.append("[");
                            sbSimpleExpression.append((String) mp.get("from.attribute[Display Name]"));
                            sbSimpleExpression.append("{");
                            sbSimpleExpression.append((String) mp.get("to.attribute[Display Name]"));
                            sbSimpleExpression.append("}");
                            sbSimpleExpression.append("]");
                        }
                        trueElement = true;
                    }
                }
            }
            // TIGTK-3029 : 08-09-2016 : START
            sbSimpleExpression.append(")");
            // TIGTK-3029 : 08-09-2016 : END
        }
        return (sbSimpleExpression.toString());

    } // Function - getSimplifiedEffectivityExpression Ends

    public String getSimplifiedEffectivityExpression(Context context, String[] args) throws Exception {
        return getSimplifiedEffectivityExpression(context, args[0]);
    }

}// Class Ends
