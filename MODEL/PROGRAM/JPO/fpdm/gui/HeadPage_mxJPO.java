package fpdm.gui;

import java.text.MessageFormat;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.Context;
import matrix.util.StringList;

public class HeadPage_mxJPO {

    private static Logger logger = LoggerFactory.getLogger("fpdm.gui.HeadPage");

    private static final String USER_NAME = "<User Name>";

    private static final String LAST_NAME = "<Last Name>";

    private static final String FIRST_NAME = "<First Name>";

    /**
     * Constructor.
     * @param context
     *            the eMatrix Context object
     * @param args
     *            holds no arguments
     * @throws Exception
     */
    public HeadPage_mxJPO(Context context, String[] args) throws Exception {
        // constructor
    }

    /**
     * @param context
     * @param args
     * @return boolean
     * @throws Exception
     */
    public static String getWelcomeMessage(Context context, String[] args) throws FrameworkException {

        StringBuilder sbWelcomeMessage = new StringBuilder();
        try {

            DomainObject personObject = PersonUtil.getPersonObject(context, context.getUser());
            StringList objectSelects = new StringList();
            objectSelects.addElement(DomainConstants.SELECT_ATTRIBUTE_FIRSTNAME);
            objectSelects.addElement(DomainConstants.SELECT_ATTRIBUTE_LASTNAME);
            Map<?, ?> personInfo = personObject.getInfo(context, objectSelects);
            String firstName = (String) personInfo.get(DomainConstants.SELECT_ATTRIBUTE_FIRSTNAME);
            String lastName = (String) personInfo.get(DomainConstants.SELECT_ATTRIBUTE_LASTNAME);

            String strPersonName = PersonUtil.getFullName(context, context.getUser());

            if (!(UIUtil.isNullOrEmpty(firstName) || UIUtil.isNullOrEmpty(lastName))) {

                String strFullNameFormat = EnoviaResourceBundle.getProperty(context, "emxFramework.FullName.WelcomeFormat");
                if (strFullNameFormat != null) {
                    String strPattern = FrameworkUtil.findAndReplace(strFullNameFormat, USER_NAME, "{0}");
                    strPattern = FrameworkUtil.findAndReplace(strPattern, FIRST_NAME, "{1}");
                    strPattern = FrameworkUtil.findAndReplace(strPattern, LAST_NAME, "{2}");
                    Object arrKeyValues[] = new Object[] { strPersonName, firstName, lastName };
                    strPersonName = MessageFormat.format(strPattern, arrKeyValues);
                }
            }
            String sDefaultPolicy = PersonUtil.getDefaultSecurityContext(context);
            if (logger.isDebugEnabled()) {
                logger.debug("getWelcomeMessage() - sDefaultPolicy = <" + sDefaultPolicy + ">");
            }

            sbWelcomeMessage.append("<span><strong>");
            sbWelcomeMessage.append(strPersonName);
            sbWelcomeMessage.append("</strong><br>");
            sbWelcomeMessage.append(sDefaultPolicy);
            sbWelcomeMessage.append("</span>");
            if (logger.isDebugEnabled()) {
                logger.debug("getWelcomeMessage() - sbWelcomeMessage = <" + sbWelcomeMessage + ">");
            }
        } catch (FrameworkException e) {
            logger.error("Error in getBrandLogoHolder()\n", e);
            throw e;
        }
        return sbWelcomeMessage.toString();
    }

    /**
     * @param context
     * @param args
     * @return boolean
     * @throws Exception
     */
    public static String getBrandLogoHolder(Context context, String[] args) throws FrameworkException {
        StringBuilder sbApplicationName = new StringBuilder();

        try {
            String sApplicationNameProperty = EnoviaResourceBundle.getProperty(context, "Framework", "FPDMFramework.Application.Name", context.getSession().getLanguage());
            if (logger.isDebugEnabled()) {
                logger.debug("getBrandLogoHolder() - sApplicationNameProperty = <" + sApplicationNameProperty + ">");
            }

            if (sApplicationNameProperty != null) {
                sbApplicationName.append(sApplicationNameProperty);
            } else {
                sbApplicationName.append("<span><strong>TIGER</strong></span>");
            }
            String sEnvironment = fpdm.gui.SystemInfo_mxJPO.getEnvironment(context);
            if (!"PROD".equals(sEnvironment)) {
                sbApplicationName.append(" - ");
                sbApplicationName.append(sEnvironment);
            }

            if (logger.isDebugEnabled()) {
                logger.debug("getBrandLogoHolder() - sbApplicationName = <" + sbApplicationName + ">");
            }
        } catch (FrameworkException e) {
            logger.error("Error in getBrandLogoHolder()\n", e);
            throw e;
        }

        return sbApplicationName.toString();
    }

}