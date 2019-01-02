import java.util.HashMap;
import java.util.Map;

import pss.constants.TigerConstants;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.framework.ui.UIUtil;
import com.matrixone.apps.productline.DerivationUtil;
import com.matrixone.apps.productline.Product;
import com.matrixone.apps.productline.ProductLineCommon;
import com.matrixone.apps.productline.ProductLineConstants;
import com.matrixone.apps.productline.ProductLineUtil;
import com.matrixone.apps.domain.util.MqlUtil;

import matrix.db.*;
import matrix.util.StringList;

public class PSS_emxProduct_mxJPO extends emxProduct_mxJPO {

    public PSS_emxProduct_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
    }

    /**
     * TIGTK-11452:RE:27/11/2017:This method copied from OOTB JPO emxProduct_mxJPO Used as the CreateJPO parameter of the Create New Derivation action, this method creates the new Product Derivation.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            - Holds the following arguments 0 - HashMap containing the following arguments
     * @return Map containing the ID of the new Product.
     * @throws Exception
     */
    @com.matrixone.apps.framework.ui.CreateProcessCallable
    public Map<String, String> createProductDerivation(Context context, String[] args) throws FrameworkException {
        HashMap<String, String> returnMap = new HashMap<String, String>();
        try {
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            String strDerivedFromID = (String) programMap.get("copyObjectId");
            String strType0 = (String) programMap.get("TypeActual");
            String strType1 = (String) programMap.get("Type1");
            String strName = (String) programMap.get("Name");
            String strType = (strType0 == null ? strType1 : strType0);
            String strRevision = (String) programMap.get("Revision");
            String strMarketingName = (String) programMap.get("MarketingName");
            String strDerivationType = (String) programMap.get("DerivationType");
            // TIGTK-13872 : 29-03-2018 : START
            String strDerivationLevel = (String) programMap.get("DerivationLevel");
            // TIGTK-13872 : 29-03-2018 : END
            String strDescription = (String) programMap.get("Description");
            String strMarketingText = (String) programMap.get("MarketingText");
            String strPolicy = (String) programMap.get("Policy");
            String strVault = (String) programMap.get("Vault");
            String strOwner = (String) programMap.get("Owner");
            // Add the attributes we will need.
            HashMap<String, String> objAttributeMap = new HashMap<String, String>();
            objAttributeMap.put(ProductLineConstants.ATTRIBUTE_MARKETING_NAME, strMarketingName);
            objAttributeMap.put(ProductLineConstants.ATTRIBUTE_MARKETING_TEXT, strMarketingText);
            // TIGTK-13872 : 29-03-2018 : START
            // If Product Evolutions are not enabled, we do not have a Derivation Level, so let's make sure to set that.
            if (Product.isProductEvolutionsDisabled(context) || UIUtil.isNullOrEmpty(strDerivationLevel)) {
                strDerivationLevel = DerivationUtil.DERIVATION_LEVEL0;
            }
            if (UIUtil.isNotNullAndNotEmpty(strDerivedFromID))
                strDerivedFromID = "";
            // Create the attributes to be sent to create by calling the Create Derived Node function. This will fill the
            // Map with the attributes we will need for the new nodes.
            HashMap nodeAttrs = DerivationUtil.createDerivedNode(context, null, strDerivationLevel, strType);
            if (nodeAttrs != null && nodeAttrs.size() > 0) {
                objAttributeMap.putAll(nodeAttrs);
            }
            // TIGTK-13872 : 29-03-2018 : END
            // Create the new Product Derivation.
            Product productBean = new Product();

            String strProductId = "";
            // TIGTK-11452:RE:27/11/2017:Start
            if (UIUtil.isNotNullAndNotEmpty(strDerivedFromID)) {
                DomainObject domProduct = DomainObject.newInstance(context, strDerivedFromID);
                PropertyUtil.setRPEValue(context, "PSS_PRODUCT_REVISED", "true", true);
                BusinessObject lastRevObj = domProduct.getLastRevision(context);
                strRevision = lastRevObj.getNextSequence(context);
            }
            strProductId = productBean.createProductDerivation(context, strType, strName, strRevision, strPolicy, strVault, strOwner, strDescription, objAttributeMap, strDerivationType,
                    strDerivedFromID);

            // TIGTK-11452:RE:27/11/2017:End
            returnMap.put("id", strProductId);
        } catch (Exception e) {
            e.printStackTrace();
            throw new FrameworkException(e);
        }
        return returnMap;
    }

    /**
     * TIGTK-11452:RE:27/11/2017:This method copied from OOTB JPO emxProduct_mxJPO Connect the Company to the Product on create of the Product
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            - Has the packed Hashmap having information of the object in context.
     * @return int - Returns 0 in case of the updation process is successful
     * @throws Exception
     *             if the operation fails
     */
    public int connectCompanyProduct(Context context, String[] args) throws Exception {
        // HashMap is defined to retrieve the arguments sent by the form after unpacking.
        HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
        // HashMap is defined to retrieve another HashMap in the unpacked list, that has the object information
        HashMap<?, ?> paramMap = (HashMap<?, ?>) programMap.get("paramMap");
        // Object Id of the context object is obtained from the Map.
        String strObjectId = (String) paramMap.get("objectId");
        // The new object id of the company that has to be used to connect with the product in context is obtained
        String strNewValue = (String) paramMap.get("New Value");
        // TIGTK-11452:RE:27/11/2017:Start
        String isRPESet = PropertyUtil.getRPEValue(context, "PSS_PRODUCT_REVISED", true);

        // The connection between Product and Company is updated with the new value.
        if (!"true".equalsIgnoreCase(isRPESet)) {
            // TIGTK-11452:RE:27/11/2017:End
            if (strNewValue != null && !"null".equalsIgnoreCase(strNewValue) && !"".equals(strNewValue)) {

                boolean companyExists = FrameworkUtil.isObjectId(context, strNewValue);
                if (!companyExists) {
                    // if the field was pre-populated with persons company, then we only have the name, need to get the id
                    com.matrixone.apps.common.Person person = com.matrixone.apps.common.Person.getPerson(context);
                    if (strNewValue.equals(person.getCompany(context).getName())) {
                        strNewValue = person.getCompanyId(context);
                    }

                }
                setId(strNewValue);
                DomainObject domainObjectToType = newInstance(context, strObjectId);

                // Added for RDO Fix
                // Changing the context to super user
                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), "", "");

                DomainRelationship.connect(context, this, ProductLineConstants.RELATIONSHIP_COMPANY_PRODUCT, domainObjectToType);

                // Added for RDO Fix
                // Changing the context back to the context user
                ContextUtil.popContext(context);
            }
        }
        // TIGTK-11452:RE:27/11/2017:Start
        if ("true".equalsIgnoreCase(isRPESet))
            PropertyUtil.setRPEValue(context, "PSS_PRODUCT_REVISED", "false", true);

        // TIGTK-11452:RE:27/11/2017:End
        return 0;
    }
}
