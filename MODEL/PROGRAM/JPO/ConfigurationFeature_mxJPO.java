
/*
 ** ${CLASS:MarketingFeature}
 **
 ** Copyright (c) 1993-2015 Dassault Systemes. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes. Copyright notice is precautionary only and does
 * not evidence any actual or intended publication of such program
 */

import matrix.db.Context;

import com.matrixone.apps.domain.util.EnoviaResourceBundle;

// DIVERSITY:PHASE2.0 : TIGTK-9119 & TIGTK-9146 : PSE : 09-08-2017 : START
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.configuration.ConfigurationUtil;
import com.matrixone.apps.configuration.ConfigurationFeature;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.configuration.ConfigurationConstants;
import com.matrixone.apps.framework.ui.UIUtil;
import pss.constants.TigerConstants;
// DIVERSITY:PHASE2.0 : TIGTK-9119 & TIGTK-9146 : PSE : 09-08-2017 : END

/**
 * This JPO class has some methods pertaining to MarketingFeature Extension.
 * @author XOG
 * @version R210 - Copyright (c) 1993-2015 Dassault Systemes.
 */
public class ConfigurationFeature_mxJPO extends ConfigurationFeatureBase_mxJPO {
    /**
     * Create a new ${CLASS:MarketingFeature} object from a given id.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments.
     * @throws Exception
     *             if the operation fails
     * @author XOG
     * @since R210
     */

    public ConfigurationFeature_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
    }

    /**
     * Main entry point.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @return an integer status code (0 = success)
     * @throws Exception
     *             if the operation fails
     * @author XOG
     * @since R210
     */
    public int mxMain(Context context, String[] args) throws Exception {
        if (!context.isConnected()) {
            String sContentLabel = EnoviaResourceBundle.getProperty(context, "Configuration", "emxProduct.Error.UnsupportedClient", context.getSession().getLanguage());
            throw new Exception(sContentLabel);
        }
        return 0;
    }

    // DIVERSITY:PHASE2.0 : TIGTK-9119 & TIGTK-9146 : PSE : 09-08-2017 : START
    /**
     * Wrapper method which will call method on CONFIGURATION STRUCTURES rel delete to restrict removal of Product if any of the Product's Configuration Feature/Option structure is used in FO
     * Effectivity Expression.
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public int checkIfCFStructureUsedInFOEffectivity(Context context, String args[]) throws Exception {
        String _sRelTypeToDisconnect = args[0];
        String _sToSideObjectId = args[1];
        String _sFromSideObjectId = args[2];
        DomainObject domFromSideObject = DomainObject.newInstance(context, _sFromSideObjectId);
        String strFromObjectType = domFromSideObject.getInfo(context, DomainConstants.SELECT_TYPE);
        int iReturn = 0;
        try {
            if (!strFromObjectType.equals(TigerConstants.TYPE_CONFIGURATIONFEATURE)) {
                if (UIUtil.isNotNullAndNotEmpty(_sRelTypeToDisconnect)
                        && ConfigurationUtil.isOfParentRel(context, _sRelTypeToDisconnect, ConfigurationConstants.RELATIONSHIP_CONFIGURATION_STRUCTURES)) {
                    ConfigurationFeature cfBean = new ConfigurationFeature();
                    iReturn = cfBean.isChildStructureUsedInFOEffectivity(context, _sRelTypeToDisconnect, _sToSideObjectId, _sFromSideObjectId);
                    if (iReturn == 1) {
                        String strLanguage = context.getSession().getLanguage();
                        String errorMessage = (EnoviaResourceBundle.getProperty(context, "Effectivity", "Effectivity.Error.EffectivityUsageCannotDelete", strLanguage)).trim();
                        emxContextUtil_mxJPO.mqlError(context, errorMessage);
                    }
                }
            }
        } catch (FrameworkException fe) {
            iReturn = 1;
        } catch (Exception e) {
            iReturn = 1;
        }
        return iReturn;
    }
    // DIVERSITY:PHASE2.0 : TIGTK-9119 & TIGTK-9146 : PSE : 09-08-2017 : END
}
