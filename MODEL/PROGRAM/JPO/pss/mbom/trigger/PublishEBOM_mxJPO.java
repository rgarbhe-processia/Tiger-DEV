package pss.mbom.trigger;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.BusinessInterface;
import matrix.db.BusinessInterfaceList;
import matrix.db.Context;
import matrix.db.Vault;
import matrix.util.StringList;
import pss.constants.TigerConstants;
import pss.mbom.webform.Equipment_mxJPO;

public class PublishEBOM_mxJPO {

    // TIGTK-5405 - 11-04-2017 - VB - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PublishEBOM_mxJPO.class);
    // TIGTK-5405 - 11-04-2017 - VB - END

    /**
     * Called from trigger on PSS_PublishedEBOM.PSS_InstanceName modify action
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public int convertPSToToolingResource(Context context, String[] args) throws Exception {
        if (args == null || args.length < 2) {
            throw new IllegalArgumentException();
        }
        ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), "", "");
        try {
            String attributeValue = args[0];
            String toObjectId = args[1];

            if (UIUtil.isNotNullAndNotEmpty(toObjectId) && UIUtil.isNotNullAndNotEmpty(attributeValue) && attributeValue.equals("PSS_PartTool")) {
                DomainObject dObj = DomainObject.newInstance(context, toObjectId);
                String current = dObj.getInfo(context, DomainConstants.SELECT_CURRENT);
                dObj.setPolicy(context, TigerConstants.POLICY_PSS_TOOL);
                if (current.equals("REMOVED")) {
                    current = "OBSOLETE";
                }
                dObj.setState(context, current);

                // TIGTK-3296
                BusinessInterfaceList interfaceList = dObj.getBusinessInterfaces(context, true);
                StringList classificationList = new StringList(TigerConstants.INTERFACE_PSS_TOOLING);
                boolean addInterface = true;
                for (int i = 0; i < interfaceList.size(); i++) {
                    BusinessInterface bInterface = interfaceList.getElement(i);
                    if (classificationList.contains(bInterface.getName())) {
                        addInterface = false;
                        break;
                    }
                }
                if (addInterface) {
                    dObj.addBusinessInterface(context, new BusinessInterface(TigerConstants.INTERFACE_PSS_TOOLING, new Vault(dObj.getVault())));
                }
            }
            return 0;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in convertPSToToolingResource: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        } finally {
            ContextUtil.popContext(context);
        }
    }

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public int updateClassifications(Context context, String[] args) throws Exception {
        if (args == null || args.length < 2) {
            throw new IllegalArgumentException();
        }
        pss.mbom.MBOMUtil_mxJPO.addORUpdateClassification(context, args[1], args[0]);
        return 0;
    }
}
