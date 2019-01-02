package fpdm.utils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import javax.activation.DataHandler;
import javax.mail.BodyPart;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PersonUtil;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.Page;
import matrix.util.StringList;
import pss.constants.TigerConstants;

public class Page_mxJPO {

    private final static Logger logger = LoggerFactory.getLogger("fpdm.utils.Page");

    private final static String COLUMN_NAME_KEY = "name";

    private final static String COLUMN_EDITLINK_KEY = "editlink";

    Properties _properties = null;

    /**
     * Constructor.
     * @param context
     *            the eMatrix Context object
     * @param args
     *            holds no arguments
     * @throws Exception
     */
    public Page_mxJPO(Context context, String[] args) throws Exception {
        // constructor
    }

    /**
     * Constructor
     * @param context
     * @param args
     * @throws Exception
     */
    public Page_mxJPO(Context context, String sPageName) throws Exception {
        _properties = getPropertiesFromPage(context, sPageName);
    }

    /***
     * Get properties from a Page object
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sPageName
     *            Page object name
     * @return
     * @throws Exception
     */
    public static Properties getPropertiesFromPage(Context context, String sPageName) throws Exception {
        Properties properties = new Properties();

        try {
            logger.debug("getPropertiesFromPage() - sPageName = <" + sPageName + ">");
            Page page = new Page(sPageName);
            page.open(context);

            InputStream input = page.getContentsAsStream(context);
            if (input == null) {
                throw new Exception("Page Object <" + sPageName + "> doesn't exist. Please check and update Page Object with Type information");
            }
            properties.load(input);

            page.close(context);

        } catch (Exception e) {
            logger.error("Error in getPropertiesFromPage()\n", e);
            throw e;
        }

        return properties;
    }

    /**
     * Get property value
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sPropertyKey
     *            Property key name
     * @return String
     * @throws Exception
     */
    public String getPropertyValue(Context context, String sPropertyKey) throws Exception {
        String sPropertyValue = _properties.getProperty(sPropertyKey);
        logger.debug("getPropertyValue() - sPropertyKey = <" + sPropertyKey + "> sPropertyValue = <" + sPropertyValue + ">");

        return sPropertyValue;
    }

    /**
     * Get property value after splitting
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sPropertyKey
     *            Property key name
     * @param sSplitChar
     *            The characters used to split property value
     * @return String
     * @throws Exception
     */
    public ArrayList<String> getPropertyValues(Context context, String sPropertyKey, String sSplitChar) throws Exception {
        ArrayList<String> alPropertylValues = new ArrayList<String>();
        String sPropertyValue = _properties.getProperty(sPropertyKey);
        logger.debug("getPropertyValue() - sPropertyKey = <" + sPropertyKey + "> sPropertyValue = <" + sPropertyValue + ">");
        StringList slValues = FrameworkUtil.split(sPropertyValue, sSplitChar);
        for (Object object : slValues) {
            alPropertylValues.add((String) object);
        }
        logger.debug("getPropertyValueFromPage() - sPropertyValue = <" + sPropertyValue + ">");
        return alPropertylValues;
    }

    /**
     * Return the Page list on database
     * @plm.usage: command: FPDM_PagesManagementCmd
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public MapList getPagesList(Context context, String[] args) throws Exception {

        MapList mlPages = new MapList();
        String sPagesList = MqlUtil.mqlCommand(context, "list page $1", "*");
        String[] saPages = sPagesList.split("\n");
        Arrays.sort(saPages);
        for (int i = 0; i < saPages.length; i++) {
            HashMap<String, String> hmPageInfo = new HashMap<String, String>();
            hmPageInfo.put(COLUMN_NAME_KEY, saPages[i]);
            StringBuilder sbEditLink = new StringBuilder();
            sbEditLink.append("<a ");
            sbEditLink.append("href=\"javascript:emxTableColumnLinkClick('../FPDM_Utils/FPDM_PageEditionFormFS.jsp?PageName=");
            sbEditLink.append(saPages[i]);
            sbEditLink.append("', '850', '700', 'false', 'popup', '','images/iconActionEdit.gif','false')\"");
            sbEditLink.append(">");
            sbEditLink.append("<img src=\"./images/iconActionEdit.gif\" style=\"border:none;\"/>");
            sbEditLink.append("</a>");
            hmPageInfo.put(COLUMN_EDITLINK_KEY, sbEditLink.toString());
            mlPages.add(hmPageInfo);
        }

        return mlPages;
    }

    /**
     * Return Page name column
     * @plm.usage: table: FPDM_PagesListTable - column: Name
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     * @return
     * @returns : Object of type Vector
     * @throws Exception
     */
    public static Vector<String> getPageName(Context context, String[] args) throws Exception {
        Vector<String> vResult = new Vector<String>();
        try {
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);

            MapList objectList = (MapList) programMap.get("objectList");
            Iterator<?> objectListItr = objectList.iterator();
            while (objectListItr.hasNext()) {
                Map<?, ?> objectMap = (Map<?, ?>) objectListItr.next();
                vResult.add((String) objectMap.get(COLUMN_NAME_KEY));
            }
        } catch (Exception ex) {
            logger.error("Error in getPageName()\n", ex);
            throw ex;
        }
        return vResult;
    }

    /**
     * Return Link to Page column
     * @plm.usage: table: FPDM_PagesListTable - column: editWindow
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     * @return
     * @returns : Object of type Vector
     * @throws Exception
     */
    public static Vector<String> getPageEditLink(Context context, String[] args) throws Exception {
        Vector<String> vResult = new Vector<String>();
        try {
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");
            Iterator<?> objectListItr = objectList.iterator();
            while (objectListItr.hasNext()) {
                Map<?, ?> objectMap = (Map<?, ?>) objectListItr.next();
                vResult.add((String) objectMap.get(COLUMN_EDITLINK_KEY));
            }
        } catch (Exception ex) {
            logger.error("Error in getPageEditLink()\n", ex);
            throw ex;
        }
        return vResult;
    }

    /**
     * Update the Page content and send a mail to notify for this change reason
     * @plm.usage: table: FPDM_PagesListTable - column: editWindow
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     * @return
     * @throws Exception
     */
    public Map<String, String> updatePageContent(Context context, String[] args) throws Exception {
        HashMap<String, String> mReturn = new HashMap<String, String>();
        Map<?, ?> paramMap = (Map<?, ?>) JPO.unpackArgs(args);
        String sPageName = (String) paramMap.get("PageName");
        String sReasonForChange = (String) paramMap.get("ReasonForChange");
        String sUserName = PersonUtil.getFullName(context, context.getUser());

        MapList mlAllPages = this.getPagesList(context, null);
        boolean bVerifInjection = false;
        for (Object hmPageInfo : mlAllPages) {
            Map<?, ?> map = (Map<?, ?>) hmPageInfo;
            if (map.get("name").equals(sPageName)) {
                bVerifInjection = true;
                break;
            }
        }

        if (bVerifInjection) {
            String sContent = (String) paramMap.get("Content");

            boolean bContextPushed = false;

            try {

                ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                bContextPushed = true;

                ContextUtil.startTransaction(context, true);

                try {
                    MqlUtil.mqlCommand(context, "modify page $1 content $2", sPageName, sContent);
                } catch (Exception e) {
                    ContextUtil.abortTransaction(context);
                    logger.error("An error has occured when trying to insert in database the new program properties code for " + sPageName, e);
                    mReturn.put("code", "Error");
                    mReturn.put("msg", "Error when trying to insert to insert in database the new program properties code for " + sPageName);
                }

                ContextUtil.commitTransaction(context);

                try {
                    this.sendMail(context, sPageName, sReasonForChange, sUserName);
                } catch (Exception e) {
                    ContextUtil.abortTransaction(context);
                    logger.error("An error has occured when trying to send email for update of " + sPageName, e);
                    // code will be success to reset cache even if an error has occured when sending mail, to be able to correct if an error is on the sender or recipient list
                    mReturn.put("msg", "Error when trying to send notification mail, please verify " + sPageName + " or smtp configuration (" + matrix.db.Environment.getValue(context, "MX_SMTP_HOST")
                            + ") and retry");
                }

                mReturn.put("code", "Success");

                return mReturn;

            } catch (FrameworkException e) {
                ContextUtil.abortTransaction(context);
                logger.error("An error has occured when trying to push context to User Agent", e);
                mReturn.put("code", "Error");
                mReturn.put("msg", "Error whith push context");
                return mReturn;
            } catch (Exception e) {
                ContextUtil.abortTransaction(context);
                logger.error("An error has occured when trying to update program properties code " + sPageName, e);
                mReturn.put("code", "Error");
                mReturn.put("msg", "An error has occured when trying to update program properties code");
                return mReturn;
            } finally {
                if (bContextPushed) {
                    ContextUtil.popContext(context);
                }
            }
        } else {
            logger.error("Modification of " + sPageName + " not allowed");
            mReturn.put("code", "Error");
            mReturn.put("msg", "Modification of " + sPageName + " not allowed");
            return mReturn;
        }
    }

    /**
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sPageName
     *            the page name
     * @param sReasonForChange
     *            the reason for the content page change
     * @throws Exception
     */
    private void sendMail(Context context, String sPageName, String sReasonForChange, String sUserName) throws Exception {
        // Get Recipients users from the current Page
        Properties pageProperties = getPropertiesFromPage(context, sPageName);
        String recipientsProperty = pageProperties.getProperty("Recipients");
        logger.debug("sendMail() - recipientsProperty = <" + recipientsProperty + ">");
        List<String> toList = new ArrayList<String>();
        if (recipientsProperty != null) {
            for (String sRecipient : recipientsProperty.split(",")) {
                toList.add(sRecipient);
            }
        }
        // Get Sender email
        String from = pageProperties.getProperty("Recipients");// PersonUtil.getEmail(context);
        logger.debug("sendMail() - from = <" + from + ">");

        if (from != null && toList.size() > 0) {
            // Subject
            StringBuilder sbSubject = new StringBuilder();
            sbSubject.append("Page ");
            sbSubject.append(sPageName);
            sbSubject.append(" has been modified - ");
            sbSubject.append(sReasonForChange);
            // Message
            StringBuilder sbMessage = new StringBuilder();
            sbMessage.append("The page <b>").append(sPageName);
            sbMessage.append("</b> has been modified the user: <b>");
            sbMessage.append(sUserName);
            sbMessage.append("</b> according to <b>");
            sbMessage.append(sReasonForChange);
            sbMessage.append("</b>.<br><br>Don't forget to update ClearCase.");

            try {
                // Get SMTP server
                String sSMTPServer = matrix.db.Environment.getValue(context, "MX_SMTP_HOST");
                Properties properties = System.getProperties();
                properties.put("mail.smtp.host", sSMTPServer);
                // Create Mime message
                Session currentSession = Session.getDefaultInstance(properties, null);
                MimeMessage mimeMessage = new MimeMessage(currentSession);
                // Set From and To fields
                mimeMessage.setFrom(new InternetAddress(from));
                for (String recipient : toList) {
                    mimeMessage.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(recipient));
                }
                // Set subject
                mimeMessage.setSubject(sbSubject.toString());
                // Set text
                mimeMessage.setContent(sbMessage.toString(), "text/html; charset=utf-8");

                javax.mail.Transport.send(mimeMessage);
            } catch (javax.mail.MessagingException me) {
                String sErrorMsg = "javax.mail.MessagingException: \n" + me.toString();
                logger.error(sErrorMsg, me);
                throw me;
            }

        }
        return;
    }

    /**
     * Get property value from a Page
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            Contains Page name, Property key
     * @return String
     * @throws Exception
     */
    public String getPropertyValueFromPage(Context context, String[] args) throws Exception {
        String sPropertyValue = null;
        try {
            Map<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            String sPageName = (String) programMap.get("pageName");
            String sPropertyKey = (String) programMap.get("propertyKey");

            sPropertyValue = getPropertyValueFromPage(context, sPageName, sPropertyKey);

        } catch (Exception e) {
            logger.error("Error in getPropertyValueFromPage()\n", e);
            throw e;
        }
        return sPropertyValue;
    }

    /**
     * Get property value from a Page
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            Contains Page name, Property key and the characters used to split property value
     * @return ArrayList
     * @throws Exception
     */
    public ArrayList<String> getPropertyValuesFromPage(Context context, String[] args) throws Exception {
        ArrayList<String> alPropertylValues = null;
        try {
            Map<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            String sPageName = (String) programMap.get("pageName");
            String sPropertyKey = (String) programMap.get("propertyKey");
            String sSplitChar = (String) programMap.get("splitChar");

            alPropertylValues = getPropertyValuesFromPage(context, sPageName, sPropertyKey, sSplitChar);

        } catch (Exception e) {
            logger.error("Error in getPropertyValueFromPage()\n", e);
            throw e;
        }
        return alPropertylValues;
    }

    /**
     * Get property value from a Page
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sPageName
     *            Page name
     * @param sPropertyKey
     *            Property key name
     * @param sSplitChar
     *            The characters used to split property value
     * @return
     * @throws Exception
     */
    public static ArrayList<String> getPropertyValuesFromPage(Context context, String sPageName, String sPropertyKey, String sSplitChar) throws Exception {
        ArrayList<String> alPropertylValues = new ArrayList<String>();
        try {
            String sPropertyValue = getPropertyValueFromPage(context, sPageName, sPropertyKey);

            StringList slValues = FrameworkUtil.split(sPropertyValue, sSplitChar);
            for (Object object : slValues) {
                alPropertylValues.add((String) object);
            }
            logger.debug(
                    "getPropertyValuesFromPage() - sPageName = <" + sPageName + "> sPropertyKey = <" + sPropertyKey + "> sSplitChar = <" + sSplitChar + "> sPropertyValue = <" + sPropertyValue + ">");

        } catch (Exception e) {
            logger.error("Error in getPropertyValuesFromPage()\n", e);
            throw e;
        }
        return alPropertylValues;
    }

    /**
     * Get property value from a Page
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sPageName
     *            Page name
     * @param sPropertyKey
     *            Property key name
     * @return
     * @throws Exception
     */
    private static String getPropertyValueFromPage(Context context, String sPageName, String sPropertyKey) throws Exception {
        Properties _propertyResource = getPropertiesFromPage(context, sPageName);
        String sPropertyValue = _propertyResource.getProperty(sPropertyKey);
        logger.debug("getPropertyValueFromPage() - sPageName = <" + sPageName + "> sPropertyKey = <" + sPropertyKey + "> sPropertyValue = <" + sPropertyValue + ">");

        return sPropertyValue;
    }

}
