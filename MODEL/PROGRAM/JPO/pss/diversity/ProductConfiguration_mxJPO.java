package pss.diversity;

import java.util.HashMap;
import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.RelationshipType;
import pss.constants.TigerConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.framework.ui.UIUtil;

public class ProductConfiguration_mxJPO {

    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ProductConfiguration_mxJPO.class);

    /**
     * Function to clone the Product Configuration
     * @param context
     * @param objectID
     * @throws Exception
     */

    // TIGTK-6806 - 19-06-2017 - TS - START
    public void cloneProductConfigurations(Context context, String[] args) throws java.lang.Exception {
        try {

            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);

            String[] strArrSelectedProductConfigs = (String[]) programMap.get("SelectedProductConfigurations");
            String strProductID = (String) programMap.get("ProductId");
            String strObjectGeneratorName = PropertyUtil.getAliasForAdmin(context, "Type", TigerConstants.TYPE_PRODUCTCONFIGURATION, false);
            if (UIUtil.isNotNullAndNotEmpty(strObjectGeneratorName)) {

                DomainObject domProduct = DomainObject.newInstance(context, strProductID);
                String[] clonnedProductConfigs = new String[strArrSelectedProductConfigs.length];

                for (int i = 0; i < strArrSelectedProductConfigs.length; i++) {
                    String strProductConfigId = strArrSelectedProductConfigs[i];
                    DomainObject domProductConfig = DomainObject.newInstance(context, strProductConfigId);
                    String strAutoName = DomainObject.getAutoGeneratedName(context, strObjectGeneratorName, null);
                    BusinessObject busClonedProductConfig = domProductConfig.cloneObject(context, strAutoName, null, null, true);
                    if (busClonedProductConfig.exists(context)) {
                        String strObjectId = busClonedProductConfig.getObjectId();
                        clonnedProductConfigs[i] = strObjectId;
                    }

                }

                domProduct.addRelatedObjects(context, new RelationshipType(TigerConstants.RELATIONSHIP_PRODUCT_CONFIGURATION), true, clonnedProductConfigs);

            }
        } catch (Exception ex) {

            logger.error("Exception in ProductConfiguration : cloneProductConfigurations()", ex);

        }
    }

}
// TIGTK-6806 - 19-06-2017 - TS - END
