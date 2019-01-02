package fpdm.nemo;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;
import javax.xml.rpc.holders.StringHolder;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.util.MatrixException;
import matrix.util.SelectList;
import matrix.util.StringList;

import org.apache.axis.AxisFault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enginframe.webservices.client.cad.Authority;
import com.enginframe.webservices.client.cad.Credentials;
import com.enginframe.webservices.client.cad.EnginFrameWSServiceLocator;
import com.enginframe.webservices.client.cad.EnginFrameWSSoapBindingStub;
import com.enginframe.webservices.client.cad.Flow;
import com.enginframe.webservices.client.cad.OptionValue;
import com.enginframe.webservices.client.cad.Session;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkProperties;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.eMatrixDateFormat;
import com.matrixone.jdom.Document;
import com.matrixone.jdom.Element;
import com.matrixone.jdom.JDOMException;
import com.matrixone.jdom.Namespace;
import com.matrixone.jdom.input.SAXBuilder;
import com.matrixone.jdom.output.XMLOutputter;

/**
 * Class used for NEMO Conversion. Used to request NEMO conversions.
 */
public class NEMOInterface_mxJPO {

    private static final String FORBIDDEN_CHAR = "[+ &()<>$%+;&@\\]\\[\\\\]";

    // private FPDMJobAdministration_mxJPO jobAdministration = null;

    // Prevent the process to be called twice.
    private static boolean retrievedProperties = false;

    private static Logger logger = LoggerFactory.getLogger("fpdm.nemo.NEMOInterface");

    // This prevent NEMO Master server congestion
    // private static int PAUSE_AFTER_SEND_JOB = 800;

    //
    // ALL NEMO SETTINGS AND CONSTANTS
    //

    private static String DEFAULT_USER_SITE = "";

    private static String NEMO_SDF_URL = "";

    private static String NEMO_WS_ID_LOGIN = "";

    private static String NEMO_WS_ID_CHANGE_SITE = "";

    private static String NEMO_WS_ID_PDM_START_TREATMENT = "";

    private static String NEMO_WS_ACTION_SHOW_FORM = "";

    private static String NEMO_WS_ID_PDM_CHECK_IN = "";

    private static String NEMO_WS_ID_LIST_SITES_PDM = "";

    private static String NEMO_WS_ID_LIST_QCHECKER_PROFILES_PDM = "";

    private static String NEMO_WS_ID_JOB_CANCEL = "";

    private static String PDM_HOST = "";

    private static String CONVERSION_REQUESTER_PASSWORD = "";

    private static String SEND_JOB_ERROR_MSG = "";

    private static String SEND_JOB_FAIL_MSG = "";

    private static String NO_FILE_ERROR_MSG = "";

    private static String ERRONEOUS_ENVIRONMENT_PROFILE_ERROR_MSG = "";

    private static String OBJECT_TYPE_NOT_RECOGNIZED_ERROR_MSG = "";

    private static String NO_CONVERSION_REQUESTER_ERROR_MSG = "";

    private static String SITE_NOT_MAPPED_ERROR_MSG = "";

    private static String PDM_USER_PASSWORD_NOT_EMPTY_ERROR_MSG = "";

    private static String SUPPORTED_FILE_FORMAT = "";

    private static String STATE_CADOBJECT_PRELIMINARY = "Preliminary";

    private static HashMap<String, ArrayList<String[]>> hmNeutralForms = null;

    private static HashMap<String, HashMap<String, Object>> hmAllNeutralInfos = null;

    private static ArrayList<String> alToHideOptions = null;

    private static final String[] saToHideOptions = new String[] { "OPT_same_ftp", "OPT_input", "OPT_queue", "OPT_notify_also", "OPT_PDM_URL", "OPT_JARFILE" };

    private static final String PROGRAM_NEMO_USERS = "FPDMNEMO.properties";

    private static final String ATTRIBUTE_FPDM_NEMO_CHAIN = "FPDM_NemoChains"; // TODO Properties

    private static String FPDM_CAD_CONVERSION_USERNAME_TEMPLATE = "";

    private static String PDM_CAD_CONVERSION_USER_NOT_EXIST_ERROR_MSG;

    private static String SELECT_CAD_CONVERSION_USER_NAME = "CAD_CONVERSION_USER_NAME";

    //
    // INSTANCE VARIABLES
    //

    private Map<String, Map<String, String>> mNemoConfigurationByName;

    private ArrayList<String> alSupportedFileFormat;

    private HashMap<String, String> hmSiteMap;

    private EnginFrameWSSoapBindingStub enginFrameWS = null;

    private Session session = null;

    private boolean bJobError = false;

    private boolean bJobNotif = false;

    private String sProcessStatus = null;

    private String sPDMUserSite = "";

    private String sUserSite = "";

    private String sNemoConnectedSite = null;

    // hmResultSummary key for this HashMap is the concatenation of the NemoAction ("QC", ...) and the objectId
    private HashMap<String, Map<String, String>> hmResultSummary = new HashMap<String, Map<String, String>>();

    private static HashMap<String, ArrayList<String>> hmQCProfiles = null;

    private static long lLastProfilesRetrieval = 0;

    private Map<String, String> hmSiteByPerson = new HashMap<String, String>();

    private static final String SELECT_ATTRIBUTE_CHAIN = "attribute[" + ATTRIBUTE_FPDM_NEMO_CHAIN + "]";

    private static final String ATTRIBUTE_FPDM_NEMO_OPTIONS = "FPDM_NemoOptions"; // TODO properties

    private static final String SELECT_ATTRIBUTE_OPTIONS = "attribute[" + ATTRIBUTE_FPDM_NEMO_OPTIONS + "]";

    private static final String SELECT_ATTRIBUTE_NEMO_ACTION = "attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_NEMO_ACTION + "]";

    private static final String SELECT_ATTRIBUTE_CONVERSION_STATUS = "attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_CONVERSION_STATUS + "]";

    private static final String SELECT_ATTRIBUTE_QC_CONVERSION_STATUS = "attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_QC_CURRENT_ACTION + "]";

    private static final String SELECT_ATTRIBUTE_REL_CONVERSION_STATUS = "attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_REL_CONVERSION_STATUS + "]";

    private static final String SELECT_ATTRIBUTE_CONVERSION_IDENTIFIANT = "attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_CONVERSION_IDENTIFIANT + "]";

    public static final Namespace nsFaurecia = Namespace.getNamespace("faurecia", "http://www.faurecia.com/xmlns/faurecia");

    public static final Namespace nsef = Namespace.getNamespace("ef", "http://www.enginframe.com/2000/EnginFrame");

    private static final String PROPERTY_NEMO_LOGIN = "Nemo_Login";

    private static final String PROPERTY_NEMO_PASSWORD = "Nemo_Password";

    private static final String PROPERTY_NEMO_DOMAIN = "Nemo_Domain";

    private static final String NEMO_CONVERSION_QUEUE_OBJECT_REVISION = null;

    private static String sOpenPDMUser = "";

    private static String PROPERTY_NEMO_conv2D = "";

    private static String PROPERTY_NEMO_conv3D = "";

    private static String PROPERTY_NEMO_convAssembly = "";

    /**
     * specify if we need to perform a change site.
     */
    private boolean performChangeSiteRequestNeeded = true;

    private static class SiteNotMappedException extends Exception {
        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        public SiteNotMappedException(String msg) {
            super(msg);
        }
    }

    private static class SendJobException extends Exception {
        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        public SendJobException(String msg) {
            super(msg);
        }
    }

    private static class ConversionUserNotExistException extends Exception {
        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        public ConversionUserNotExistException(String msg) {
            super(msg);
        }
    }

    /**
     * Constructor
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            Not used
     * @throws Exception
     */

    public NEMOInterface_mxJPO(Context context, String[] args) throws Exception {
        // jobAdministration = new FPDMJobAdministration_mxJPO(context, args);
        mNemoConfigurationByName = fpdm.nemo.NEMOUtils_mxJPO.getNemoConfigurationAttributes(context, null);
        // Remove empty attributes
        for (Entry<String, Map<String, String>> eEntryByName : mNemoConfigurationByName.entrySet()) {
            String sNemoConfigurationName = eEntryByName.getKey();
            Map<String, String> mNemoConfiguration = eEntryByName.getValue();
            Map<String, String> mNemoConfigurationWithoutEmptyAttributes = new HashMap<String, String>(mNemoConfiguration);
            for (Entry<String, String> eEntry : mNemoConfiguration.entrySet()) {
                String key = (String) eEntry.getKey();
                String value = (String) eEntry.getValue();
                if (value == null || "".equals(value)) {
                    mNemoConfigurationWithoutEmptyAttributes.remove(key);
                }
            }
            mNemoConfigurationByName.put(sNemoConfigurationName, mNemoConfigurationWithoutEmptyAttributes);
        }
        /*
         * if (logger.isDebugEnabled()) { logger.debug("FPDMNEMOInterface - mNemoConfigurationByName = <" + mNemoConfigurationByName + ">"); }
         */
    }

    /**
     * Read properties and init global variables
     * @param context
     *            the eMatrix <code>Context</code> object
     * @throws Exception
     */
    private void init(Context context) throws Exception {
        try {
            if (!retrievedProperties) {
                initProperties(context);
            }
            NEMO_SDF_URL = FrameworkProperties.getProperty("FPDM.NEMOInterface.Application.Server.SdfUrl", "");
            alSupportedFileFormat = getList(SUPPORTED_FILE_FORMAT);
        } catch (Exception e) {
            logger.error("Error in FPDMNEMOInterface_mxJPO init", e);
            throw e;
        }
    }

    /**
     * Read properties and init global variables
     * @param context
     *            the eMatrix <code>Context</code> object
     * @throws Exception
     */
    private void initProperties(Context context) throws Exception {
        try {
            PDM_HOST = FrameworkProperties.getProperty("FPDM.Application.Server.HostName", "");
            NEMO_WS_ID_CHANGE_SITE = FrameworkProperties.getProperty("FPDM.NEMOInterface.Service.Change_Site", "");// "//faurecia/change.site";
            NEMO_WS_ID_LOGIN = FrameworkProperties.getProperty("FPDM.NEMOInterface.Service.Login", "");// "//faurecia/change.site";
            NEMO_WS_ID_JOB_CANCEL = FrameworkProperties.getProperty("FPDM.NEMOInterface.Service.job.Cancel", "");
            NEMO_WS_ID_PDM_START_TREATMENT = FrameworkProperties.getProperty("FPDM.NEMOInterface.Service.PDM_Start_Treatment", "");// "//faurecia/login";
            // NEMO_WS_ACTION_SHOW_FORM = "show.form";// FrameworkProperties.getProperty("FPDM.NEMOInterface.Service.PDM_Action.show.form", "");// "//faurecia/login";
            NEMO_WS_ACTION_SHOW_FORM = FrameworkProperties.getProperty("FPDM.NEMOInterface.Service.PDM_Action.show.form", "");// "//faurecia/login";
            NEMO_WS_ID_PDM_CHECK_IN = FrameworkProperties.getProperty("FPDM.NEMOInterface.Service.PDM_Check_In", "");// "//faurecia/pdm.check.in";
            NEMO_WS_ID_LIST_SITES_PDM = FrameworkProperties.getProperty("FPDM.NEMOInterface.Service.list_sites_pdm", "");// "//faurecia/list.sites.pdm";
            NEMO_WS_ID_LIST_QCHECKER_PROFILES_PDM = FrameworkProperties.getProperty("FPDM.NEMOInterface.Service.list_qchecker_profiles_pdm", "");// "//faurecia/pdm.qchecker.list";

            try {
                SEND_JOB_ERROR_MSG = FrameworkProperties.getProperty("FPDM.NEMOInterface.Error_msg.Send_Job_Error", "");
                NO_FILE_ERROR_MSG = FrameworkProperties.getProperty("FPDM.NEMOInterface.Error_msg.No_File_To_Convert", "");
                ERRONEOUS_ENVIRONMENT_PROFILE_ERROR_MSG = FrameworkProperties.getProperty("FPDM.NEMOInterface.Error_msg.Erroneous_Environment_Profile", "");
                NO_CONVERSION_REQUESTER_ERROR_MSG = FrameworkProperties.getProperty("FPDM.NEMOInterface.Error_msg.No_Conversion_Requester", "");
                OBJECT_TYPE_NOT_RECOGNIZED_ERROR_MSG = FrameworkProperties.getProperty("FPDM.NEMOInterface.Error_msg.Object_Type_Not_Recognized", "");
                SITE_NOT_MAPPED_ERROR_MSG = FrameworkProperties.getProperty("FPDM.NEMOInterface.Error_msg.Site_not_mapped", "");
                PDM_USER_PASSWORD_NOT_EMPTY_ERROR_MSG = FrameworkProperties.getProperty("FPDM.NEMOInterface.Error_msg.Pdm_Password_Not_Empty", "");
                SEND_JOB_FAIL_MSG = FrameworkProperties.getProperty("FPDM.NEMOInterface.Error_msg.NemoCallFail", "");
                PDM_CAD_CONVERSION_USER_NOT_EXIST_ERROR_MSG = FrameworkProperties.getProperty("FPDM.NEMOInterface.Error_msg.Pdm_CAD_Conversion_User_Not_Exist", "");
            } catch (Exception e) {
                logger.error("Missing properties in init method", e);
            }

            SUPPORTED_FILE_FORMAT = FrameworkProperties.getProperty("FPDM.NEMOInterface.Supported_File_Format", "");
            DEFAULT_USER_SITE = FrameworkProperties.getProperty("FPDM.NEMOInterface.Default_User_Site", "");

            FPDM_CAD_CONVERSION_USERNAME_TEMPLATE = FrameworkProperties.getProperty("FPDM.cadConversion.userNameTemplate", "");

            sOpenPDMUser = FrameworkProperties.getProperty(context, "FPDM.OpenPDM.Username");

            PROPERTY_NEMO_conv2D = EnoviaResourceBundle.getProperty(context, "EngineeringCentral", "FPDM.NEMO.conv2D", context.getSession().getLanguage());
            PROPERTY_NEMO_conv3D = EnoviaResourceBundle.getProperty(context, "EngineeringCentral", "FPDM.NEMO.conv3D", context.getSession().getLanguage());
            PROPERTY_NEMO_convAssembly = EnoviaResourceBundle.getProperty(context, "EngineeringCentral", "FPDM.NEMO.convAssembly", context.getSession().getLanguage());

            retrievedProperties = true;

        } catch (Exception e) {
            logger.error("Error in FPDMNEMOInterface_mxJPO initProperties", e);
            throw e;
        }
    }

    /**
     * Send an email to Support level 3 in case of too many objects linked to Nemo Conversion Queue. The support level 3 will have to check if everything seems to be ok.
     * @param context
     * @throws MatrixException
     */
    private void checkConversionQueue(Context context) throws MatrixException {
        BusinessObject boNConversion = new BusinessObject(fpdm.nemo.NEMOUtils_mxJPO.TYPE_NEMO_CONVERSION_QUEUE, fpdm.nemo.NEMOUtils_mxJPO.NEMO_CONVERSION_QUEUE_OBJECT_NAME,
                NEMO_CONVERSION_QUEUE_OBJECT_REVISION, fpdm.nemo.NEMOUtils_mxJPO.VAULT_PRODUCTION);
        DomainObject dobNemoConversion = DomainObject.newInstance(context, boNConversion);
        int iLimitObjectsAlert = Integer.valueOf(dobNemoConversion.getAttributeValue(context, fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_NEMO_LIMIT_OBJECTS_ALERT));
        long lLastNotification = Long.valueOf(dobNemoConversion.getAttributeValue(context, fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_NEMO_LAST_NOTIFICATION_TIME));
        long lLimitTimeInConversion = Long.valueOf(dobNemoConversion.getAttributeValue(context, fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_NEMO_LIMIT_TIME_CONVERSION_REQUESTED));
        long lCurrentTime = System.currentTimeMillis();
        // 2 Hours ==> 7200 seconds ==> 7 200 000 ms
        long l2HoursMs = 7200000;
        // Check last notification date
        if ((lCurrentTime - lLastNotification) > l2HoursMs) {
            // Get linked objects to the NEMO queue
            String sNemoObjectWhere = SELECT_ATTRIBUTE_CONVERSION_STATUS + "!=" + fpdm.nemo.NEMOUtils_mxJPO.RANGE_CONVERSION_STATUS_FAILED;
            String sNemoRelWhere = SELECT_ATTRIBUTE_NEMO_ACTION + "==''";

            String sQCObjectWhere = SELECT_ATTRIBUTE_QC_CONVERSION_STATUS + "!=" + fpdm.nemo.NEMOUtils_mxJPO.RANGE_CONVERSION_STATUS_FAILED;
            String sQCRelWhere = SELECT_ATTRIBUTE_NEMO_ACTION + "=='" + fpdm.nemo.NEMOUtils_mxJPO.RANGE_NEMO_ACTION_QC + "'";

            String sCustomRelWhere = SELECT_ATTRIBUTE_NEMO_ACTION + "!='" + fpdm.nemo.NEMOUtils_mxJPO.RANGE_NEMO_ACTION_QC + "' && " + SELECT_ATTRIBUTE_NEMO_ACTION + "!='"
                    + fpdm.nemo.NEMOUtils_mxJPO.RANGE_NEMO_ACTION_CANCEL + "' && " + SELECT_ATTRIBUTE_NEMO_ACTION + "!='' && " + SELECT_ATTRIBUTE_REL_CONVERSION_STATUS + "!="
                    + fpdm.nemo.NEMOUtils_mxJPO.RANGE_CONVERSION_STATUS_FAILED;

            String sCancelRelWhere = SELECT_ATTRIBUTE_NEMO_ACTION + "=='" + fpdm.nemo.NEMOUtils_mxJPO.RANGE_NEMO_ACTION_CANCEL + "'";

            StringList slObjectInfo = new StringList("attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_CONVERSION_STATUS + "]");
            slObjectInfo.addElement("attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_QC_CURRENT_ACTION + "]");
            slObjectInfo.addElement(DomainConstants.SELECT_TYPE);
            slObjectInfo.addElement(DomainConstants.SELECT_NAME);
            slObjectInfo.addElement(DomainConstants.SELECT_REVISION);

            StringList slRelInfo = new StringList(SELECT_ATTRIBUTE_NEMO_ACTION);
            slRelInfo.addElement(DomainRelationship.SELECT_ORIGINATED);
            slRelInfo.addElement(SELECT_ATTRIBUTE_REL_CONVERSION_STATUS);

            MapList mlNemoLinked = dobNemoConversion.getRelatedObjects(context, fpdm.nemo.NEMOUtils_mxJPO.RELATIONSHIP_NEMO_CONVERSION_QUEUE, "*", slObjectInfo, slRelInfo, false, true, (short) 1,
                    sNemoObjectWhere, sNemoRelWhere, 0);
            MapList mlQCLinked = dobNemoConversion.getRelatedObjects(context, fpdm.nemo.NEMOUtils_mxJPO.RELATIONSHIP_NEMO_CONVERSION_QUEUE, "*", slObjectInfo, slRelInfo, false, true, (short) 1,
                    sQCObjectWhere, sQCRelWhere, 0);
            MapList mlCustomLinked = dobNemoConversion.getRelatedObjects(context, fpdm.nemo.NEMOUtils_mxJPO.RELATIONSHIP_NEMO_CONVERSION_QUEUE, "*", slObjectInfo, slRelInfo, false, true, (short) 1,
                    "", sCustomRelWhere, 0);
            MapList mlCancelLinked = dobNemoConversion.getRelatedObjects(context, fpdm.nemo.NEMOUtils_mxJPO.RELATIONSHIP_NEMO_CONVERSION_QUEUE, "*", slObjectInfo, slRelInfo, false, true, (short) 1,
                    "", sCancelRelWhere, 0);

            mlNemoLinked.addAll(mlQCLinked);
            mlNemoLinked.addAll(mlCustomLinked);
            mlNemoLinked.addAll(mlCancelLinked);
            if (logger.isDebugEnabled()) {
                logger.debug("checkConversionQueue() - mlObjToLinked.size() = <" + mlNemoLinked.size() + ">");
            }

            boolean bNemoLimitObjectAlertSent = false;
            // Check number of linked objects
            if (mlNemoLinked.size() >= iLimitObjectsAlert) {
                // Create mail content with objects linked to Nemo Conversion Queue
                bNemoLimitObjectAlertSent = sendNemoLimitObjectAlert(context, mlNemoLinked);
            }
            // check requested date of each object and send an email to support 3 if necessary
            boolean bNemoLimitTimeInConversionAlertSent = sendNemoLimitTimeConversionRequestedAlert(context, mlNemoLinked, lCurrentTime, lLimitTimeInConversion);

            if (bNemoLimitObjectAlertSent || bNemoLimitTimeInConversionAlertSent) {
                // set the last notification time
                dobNemoConversion.setAttributeValue(context, fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_NEMO_LAST_NOTIFICATION_TIME, "" + System.currentTimeMillis());
            }

        }
        return;
    }

    /**
     * Reset to an empty status any conversion without Nemo job id
     * @param context
     */
    private void cleanConversionQueue(Context context) {
        try {
            ContextUtil.startTransaction(context, true);
            if (fpdm.nemo.NEMOUtils_mxJPO.RANGE_PROCESS_IN_USE_FALSE.equals(getQueueStatus(context))) {
                setQueueStatus(context, fpdm.nemo.NEMOUtils_mxJPO.RANGE_PROCESS_IN_USE_TRUE);
                try {
                    BusinessObject boNConversion = new BusinessObject(fpdm.nemo.NEMOUtils_mxJPO.TYPE_NEMO_CONVERSION_QUEUE, fpdm.nemo.NEMOUtils_mxJPO.NEMO_CONVERSION_QUEUE_OBJECT_NAME,
                            NEMO_CONVERSION_QUEUE_OBJECT_REVISION, fpdm.nemo.NEMOUtils_mxJPO.VAULT_PRODUCTION);
                    DomainObject dobNemoConversion = DomainObject.newInstance(context, boNConversion);

                    StringList slObjectInfo = new StringList("attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_CONVERSION_STATUS + "]");
                    // if ("ASBG".equals(FPDMOrganizationName_mxJPO.getName(context))) {
                    slObjectInfo.addElement("attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_QC_CURRENT_ACTION + "]");
                    // }
                    slObjectInfo.addElement(DomainConstants.SELECT_ID);
                    slObjectInfo.addElement(DomainConstants.SELECT_TYPE);
                    slObjectInfo.addElement(DomainConstants.SELECT_NAME);
                    slObjectInfo.addElement(DomainConstants.SELECT_REVISION);

                    StringList slRelInfo = new StringList(SELECT_ATTRIBUTE_NEMO_ACTION);
                    slRelInfo.addElement(SELECT_ATTRIBUTE_REL_CONVERSION_STATUS);
                    slRelInfo.addElement(DomainRelationship.SELECT_ID);

                    String sNemoRelWhere = SELECT_ATTRIBUTE_CONVERSION_IDENTIFIANT + "==''";
                    MapList mlConversions = dobNemoConversion.getRelatedObjects(context, fpdm.nemo.NEMOUtils_mxJPO.RELATIONSHIP_NEMO_CONVERSION_QUEUE, "*", slObjectInfo, slRelInfo, false, true,
                            (short) 1, "", sNemoRelWhere, 0);

                    if (logger.isTraceEnabled()) {
                        logger.trace("cleanConversionQueue - mlConversions : " + mlConversions);
                    }
                    for (Object oConversion : mlConversions) {
                        if (oConversion != null && oConversion instanceof Map) {
                            Map<?, ?> mConversion = (Map<?, ?>) oConversion;
                            String sNemoAction = fpdm.nemo.NEMOUtils_mxJPO.getSingleValue(mConversion.get(SELECT_ATTRIBUTE_NEMO_ACTION));
                            String sAttributeStatusName = fpdm.nemo.NEMOUtils_mxJPO.getStatusAttribute(sNemoAction);
                            String sConversionStatusValue = fpdm.nemo.NEMOUtils_mxJPO.getSingleValue(mConversion.get("attribute[" + sAttributeStatusName + "]"));
                            if (logger.isDebugEnabled()) {
                                logger.debug("sNemoAction : " + sNemoAction);
                                logger.debug("sConversionStatusValue : " + sConversionStatusValue);
                            }
                            if (fpdm.nemo.NEMOUtils_mxJPO.RANGE_CONVERSION_STATUS_QUEUED.equals(sConversionStatusValue)
                                    || fpdm.nemo.NEMOUtils_mxJPO.RANGE_CONVERSION_STATUS_PDM_CHECKOUT.equals(sConversionStatusValue)
                                    || fpdm.nemo.NEMOUtils_mxJPO.RANGE_CONVERSION_STATUS_WAITING_TREATMENT_START.equals(sConversionStatusValue)
                                    || fpdm.nemo.NEMOUtils_mxJPO.RANGE_CONVERSION_STATUS_RUNNING.equals(sConversionStatusValue)
                                    || fpdm.nemo.NEMOUtils_mxJPO.RANGE_CONVERSION_STATUS_FINISHING.equals(sConversionStatusValue)
                                    || fpdm.nemo.NEMOUtils_mxJPO.RANGE_CONVERSION_STATUS_DONE.equals(sConversionStatusValue)
                                    || fpdm.nemo.NEMOUtils_mxJPO.RANGE_CONVERSION_STATUS_CHECKIN_REQUEST.equals(sConversionStatusValue)
                                    || fpdm.nemo.NEMOUtils_mxJPO.RANGE_CONVERSION_STATUS_PDM_CHECKIN_WAIT.equals(sConversionStatusValue)) {
                                String sRelId = fpdm.nemo.NEMOUtils_mxJPO.getSingleValue(mConversion.get(DomainRelationship.SELECT_ID));
                                if (logger.isDebugEnabled()) {
                                    logger.debug("sRelId : " + sRelId);
                                }
                                DomainRelationship sRelObject = DomainRelationship.newInstance(context, sRelId);
                                if (fpdm.nemo.NEMOUtils_mxJPO.isOnDemandConversion(sNemoAction)) {
                                    sRelObject.setAttributeValue(context, fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_REL_CONVERSION_STATUS, "");
                                    sRelObject.setAttributeValue(context, fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_CONVERSION_RESULT, "");
                                } else {
                                    String sBusId = fpdm.nemo.NEMOUtils_mxJPO.getSingleValue(mConversion.get(DomainConstants.SELECT_ID));
                                    DomainObject dobCADObject = DomainObject.newInstance(context, sBusId);
                                    sRelObject.setAttributeValue(context, fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_CONVERSION_RESULT, "");
                                    dobCADObject.setAttributeValue(context, fpdm.nemo.NEMOUtils_mxJPO.getStatusAttribute(sNemoAction), "");
                                }
                            }
                        }
                    }

                } finally {
                    if (!fpdm.nemo.NEMOUtils_mxJPO.RANGE_PROCESS_IN_USE_LOCKED.equals(getQueueStatus(context))) {
                        setQueueStatus(context, fpdm.nemo.NEMOUtils_mxJPO.RANGE_PROCESS_IN_USE_FALSE);
                    }
                }
            }
            ContextUtil.commitTransaction(context);
        } catch (RuntimeException rt) {
            throw rt;
        } catch (Exception e) {
            ContextUtil.abortTransaction(context);
            logger.error("cleanConversionQueue - Error happened while trying to clean the conversion queue : ", e);
        }
    }

    /**
     * This method will be executed by the web service. Perform directly the request to NEMO.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            Not used
     * @return
     * @throws Exception
     */
    public String FPDMNEMOInterface(Context context, String[] args) throws Exception {
        String sMessageReturn = "";
        boolean bContextPushed = false;
        boolean bProcessFailed = false;
        String sExceptionMessage = "";

        try {

            /*
             * if (jobAdministration.contextHasToBePush(context.getUser())) { jobAdministration.pushContextForJob(context); bContextPushed = true; }
             */

            cleanConversionQueue(context);

            if (logger.isDebugEnabled()) {
                logger.debug("FPDMNEMOInterface() - args = <" + Arrays.toString(args) + ">");
                logger.debug("FPDMNEMOInterface() - context.getUser() = <" + context.getUser() + ">");
            }

            if (fpdm.nemo.NEMOUtils_mxJPO.RANGE_PROCESS_IN_USE_FALSE.equals(getQueueStatus(context))) {
                perform(context, args);
            }

        } catch (Exception e) {
            logger.error("Error:", e);
            sExceptionMessage = e.toString();
            bProcessFailed = true;
            sMessageReturn = "Error during creation of Job:" + e.toString();
        } finally {

            if (bJobError || bJobNotif) {
                SimpleDateFormat dateMXFormat = new SimpleDateFormat(eMatrixDateFormat.strEMatrixDateFormat);
                String sFormattedDate = dateMXFormat.format(new Date());
                try {
                    String sMailSubject = fpdm.nemo.NEMOUtils_mxJPO.constructMailSubject(context, "FPDMNEMOInterface JPO", sProcessStatus);
                    String sMailMessage = fpdm.nemo.NEMOUtils_mxJPO.constructMailMessage(context, hmResultSummary, sExceptionMessage, "WS-FPDMNEMOInterface", sFormattedDate, sProcessStatus);
                    boolean hasError = bProcessFailed | bJobError;
                    /*
                     * if (jobAdministration.mailHasToBeSent(context, "FPDMNEMOInterface", hmResultSummary, hasError)) { if (logger.isDebugEnabled()) { logger.debug(
                     * "FPDMNEMOInterface() - Send notification  bProcessFailed<" + bProcessFailed + "> bJobError<" + bJobError + "> hasError<" + hasError + ">"); }
                     * FPDMNotification_mxJPO.sendMailToSupport3(context, sMailMessage, sMailSubject); }
                     */
                } catch (Exception e) {
                    logger.error("error:", e);
                }
            }
            try {
                checkConversionQueue(context);
            } finally {
                if (bContextPushed) {
                    ContextUtil.popContext(context);
                }
            }
        }

        return sMessageReturn;
    }

    /**
     * Set the queue status to the given value
     * @param context
     * @param value
     */
    private void setQueueStatus(Context context, String value) {
        boolean bContextPushed = false;
        try {
            String sUserName = PropertyUtil.getSchemaProperty(context, "person_UserAgent");
            ContextUtil.pushContext(context, sUserName, null, null);
            bContextPushed = true;
            BusinessObject boNConversion = new BusinessObject(fpdm.nemo.NEMOUtils_mxJPO.TYPE_NEMO_CONVERSION_QUEUE, fpdm.nemo.NEMOUtils_mxJPO.NEMO_CONVERSION_QUEUE_OBJECT_NAME,
                    fpdm.nemo.NEMOUtils_mxJPO.NEMO_CONVERSION_QUEUE_OBJECT_REVISION, fpdm.nemo.NEMOUtils_mxJPO.VAULT_PRODUCTION);
            DomainObject dobNemoConversion = DomainObject.newInstance(context, boNConversion);
            boolean bHistoryOff = false;
            try {
                // Disable history
                bHistoryOff = true;
                MqlUtil.mqlCommand(context, "history off");
                dobNemoConversion.setAttributeValue(context, fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_PROCESS_IN_USE, value);
            } finally {
                // Enable history
                if (bHistoryOff) {
                    MqlUtil.mqlCommand(context, "history on");
                }
            }
        } catch (MatrixException e) {
            logger.error("Error in setQueueStatus", e);
        } finally {
            if (bContextPushed) {
                try {
                    ContextUtil.popContext(context);
                    bContextPushed = false;
                } catch (FrameworkException e2) {
                    logger.error("Error in setQueueStatus", e2);
                }
            }
        }
    }

    /**
     * Get the queue status
     * @param context
     * @return
     */
    private String getQueueStatus(Context context) {
        String sReturn = null;
        boolean bContextPushed = false;
        try {
            String sUserName = PropertyUtil.getSchemaProperty(context, "person_UserAgent");
            ContextUtil.pushContext(context, sUserName, null, null);
            bContextPushed = true;

            BusinessObject boNConversion = new BusinessObject(fpdm.nemo.NEMOUtils_mxJPO.TYPE_NEMO_CONVERSION_QUEUE, fpdm.nemo.NEMOUtils_mxJPO.NEMO_CONVERSION_QUEUE_OBJECT_NAME,
                    fpdm.nemo.NEMOUtils_mxJPO.NEMO_CONVERSION_QUEUE_OBJECT_REVISION, fpdm.nemo.NEMOUtils_mxJPO.VAULT_PRODUCTION);
            DomainObject dobNemoConversion = DomainObject.newInstance(context, boNConversion);
            sReturn = dobNemoConversion.getAttributeValue(context, fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_PROCESS_IN_USE);
        } catch (MatrixException e) {
            logger.error("Error in setQueueStatus", e);
        } finally {
            if (bContextPushed) {
                try {
                    ContextUtil.popContext(context);
                    bContextPushed = false;
                } catch (FrameworkException e2) {
                    logger.error("Error in setQueueStatus", e2);
                }
            }
        }
        return sReturn;
    }

    /**
     * This method will return objects to treat during the Nemo process. It will merge objects for Conversion and Quality Checker
     * @param context
     *            : Matrix user context
     * @param dobNemoConversion
     *            : DomainObject corresponding to Nemo Conversion Queue
     * @return MapList that contains objects to treat
     * @throws Exception
     */
    private MapList getObjectsToConvert(Context context, DomainObject dobNemoConversion) throws Exception {
        MapList mlToReturn = new MapList();

        // Limit the object in order to not charge Nemo server
        int iLimitOfObjects = Integer.valueOf(dobNemoConversion.getAttributeValue(context, fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_NEMO_MAXIMUM_NUMBER_OF_REQUEST));
        if (logger.isDebugEnabled()) {
            logger.debug("[getObjectsToConvert]<iLimitOfObjects>:" + iLimitOfObjects);
        }
        // Where clauses for Nemo, get only Objects connected with nemo action different from QC and status equals to Empty or Done
        String sNemoObjectWhere = "attribute[" + fpdm.nemo.NEMOUtils_mxJPO.getStatusAttribute("") + "]=='" + fpdm.nemo.NEMOUtils_mxJPO.RANGE_CONVERSION_STATUS_DONE + "' || attribute["
                + fpdm.nemo.NEMOUtils_mxJPO.getStatusAttribute("") + "]==''";
        String sNemoRelWhere = SELECT_ATTRIBUTE_NEMO_ACTION + "==''";
        // Where clauses for QC, get only Objects connected with nemo action set to QC and status equals to Empty or Done
        String sQCObjectWhere = "attribute[" + fpdm.nemo.NEMOUtils_mxJPO.getStatusAttribute(fpdm.nemo.NEMOUtils_mxJPO.RANGE_NEMO_ACTION_QC) + "]=='"
                + fpdm.nemo.NEMOUtils_mxJPO.RANGE_CONVERSION_STATUS_DONE + "' || attribute[" + fpdm.nemo.NEMOUtils_mxJPO.getStatusAttribute(fpdm.nemo.NEMOUtils_mxJPO.RANGE_NEMO_ACTION_QC) + "]==''";
        String sQCRelWhere = SELECT_ATTRIBUTE_NEMO_ACTION + "=='" + fpdm.nemo.NEMOUtils_mxJPO.RANGE_NEMO_ACTION_QC + "'";
        // Where clauses for On Demand conversions, get only Objects connected with nemo action set to not QC, not empty and status equals to Empty or Done
        String sOtherRelWhere = SELECT_ATTRIBUTE_NEMO_ACTION + "!='" + fpdm.nemo.NEMOUtils_mxJPO.RANGE_NEMO_ACTION_QC + "' && " + SELECT_ATTRIBUTE_NEMO_ACTION + "!= '"
                + fpdm.nemo.NEMOUtils_mxJPO.RANGE_NEMO_ACTION_CANCEL + "' && " + SELECT_ATTRIBUTE_NEMO_ACTION + "!='' && (attribute[" + fpdm.nemo.NEMOUtils_mxJPO.getStatusAttribute("Other") + "]=='"
                + fpdm.nemo.NEMOUtils_mxJPO.RANGE_CONVERSION_STATUS_DONE + "' || attribute[" + fpdm.nemo.NEMOUtils_mxJPO.getStatusAttribute("Other") + "]=='')";
        if (logger.isDebugEnabled()) {
            logger.debug("[getObjectsToConvert]<sNemoWhere>:" + sNemoObjectWhere);
            logger.debug("[getObjectsToConvert]<sQCWhere>:" + sQCObjectWhere);
            logger.debug("[getObjectsToConvert]<sOtherRelWhere>:" + sOtherRelWhere);
        }

        StringList slObjectSelect = new StringList();
        slObjectSelect.addElement(DomainConstants.SELECT_ID);
        slObjectSelect.addElement(DomainConstants.SELECT_TYPE);
        slObjectSelect.addElement(DomainConstants.SELECT_NAME);
        slObjectSelect.addElement(DomainConstants.SELECT_REVISION);
        slObjectSelect.addElement(DomainConstants.SELECT_CURRENT);
        slObjectSelect.addElement("attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_FOLIO_NUMBER + "]");
        slObjectSelect.addElement("attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_CONVERSION_STATUS + "]");
        slObjectSelect.addElement("attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_QC_CURRENT_ACTION + "]");

        StringList slRelSelect = new StringList();
        slRelSelect.addElement(DomainRelationship.SELECT_ID);
        slRelSelect.addElement("attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_CONVERSION_REQUESTER + "]");
        slRelSelect.addElement("attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_NEMO_ACTION + "]");
        slRelSelect.addElement("attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_CONVERSION_IDENTIFIANT + "]");
        slRelSelect.addElement("attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_CONVERSION_RESULT + "]");
        slRelSelect.addElement("attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_REL_CONVERSION_STATUS + "]");

        String sRelPattern = fpdm.nemo.NEMOUtils_mxJPO.RELATIONSHIP_NEMO_CONVERSION_QUEUE;
        MapList mlNemoObjects = dobNemoConversion.getRelatedObjects(context, sRelPattern, "*", slObjectSelect, slRelSelect, false, true, (short) 1, sNemoObjectWhere, sNemoRelWhere, iLimitOfObjects);
        MapList mlQCObjects = dobNemoConversion.getRelatedObjects(context, sRelPattern, "*", slObjectSelect, slRelSelect, false, true, (short) 1, sQCObjectWhere, sQCRelWhere, iLimitOfObjects);
        MapList mlOtherObjects = dobNemoConversion.getRelatedObjects(context, sRelPattern, "*", slObjectSelect, slRelSelect, false, true, (short) 1, null, sOtherRelWhere, iLimitOfObjects);
        if (logger.isDebugEnabled()) {
            logger.debug("[getObjectsToConvert]<mlNemoObjects size>" + mlNemoObjects.size());
            logger.debug("[getObjectsToConvert]<mlQCObjects size>" + mlQCObjects.size());
            logger.debug("[getObjectsToConvert]<mlOtherObjects size>" + mlOtherObjects.size());
        }
        mlToReturn.addAll(mlNemoObjects);
        mlToReturn.addAll(mlQCObjects);
        mlToReturn.addAll(mlOtherObjects);
        return mlToReturn;
    }

    /**
     * This method will return objects to cancel during the Nemo process.
     * @param context
     *            : Matrix user context
     * @param dobNemoConversion
     *            : DomainObject corresponding to Nemo Conversion Queue
     * @return MapList that contains objects to cancel
     * @throws Exception
     */
    private MapList getObjectsToCancel(Context context, DomainObject dobNemoConversion) throws Exception {

        // Limit the object in order to not charge Nemo server
        int iLimitOfObjects = Integer.valueOf(dobNemoConversion.getAttributeValue(context, fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_NEMO_MAXIMUM_NUMBER_OF_CANCEL));
        if (logger.isDebugEnabled()) {
            logger.debug("[getObjectsToCancel]<iLimitOfObjects>:" + iLimitOfObjects);
        }
        // Where clauses for Cancels, get only Objects connected with nemo action equals "cancel"
        String sCancelRelWhere = SELECT_ATTRIBUTE_NEMO_ACTION + "== '" + fpdm.nemo.NEMOUtils_mxJPO.RANGE_NEMO_ACTION_CANCEL + "'";
        if (logger.isDebugEnabled()) {
            logger.debug("[getObjectsToCancel]<sCancelRelWhere>:" + sCancelRelWhere);
        }

        StringList slObjectSelect = new StringList();
        slObjectSelect.addElement(DomainConstants.SELECT_ID);

        StringList slRelSelect = new StringList();
        slRelSelect.addElement(DomainRelationship.SELECT_ID);
        slRelSelect.addElement(SELECT_ATTRIBUTE_NEMO_ACTION);
        slRelSelect.addElement(SELECT_ATTRIBUTE_CONVERSION_IDENTIFIANT);
        slRelSelect.addElement(SELECT_ATTRIBUTE_REL_CONVERSION_STATUS);

        String sRelPattern = fpdm.nemo.NEMOUtils_mxJPO.RELATIONSHIP_NEMO_CONVERSION_QUEUE;
        MapList mlCancelObjects = dobNemoConversion.getRelatedObjects(context, sRelPattern, "*", slObjectSelect, slRelSelect, false, true, (short) 1, null, sCancelRelWhere, iLimitOfObjects);
        if (logger.isDebugEnabled()) {
            logger.debug("[getObjectsToCancel]<mlCancelObjects size>" + mlCancelObjects.size());
        }
        return mlCancelObjects;
    }

    /**
     * Treat all conversions if the queue is not locked and if there are several objects
     * @param context
     *            the ematrix context
     * @param args
     *            arguments
     * @return 0 if no error occurs, -1 otherwise
     * @throws Exception
     *             if error occurs
     */
    private int perform(Context context, String[] args) throws Exception {
        int iResult = 0;
        boolean bNEMOConnected = false;

        try {
            if (logger.isDebugEnabled()) {
                logger.debug("**************************************************");
                logger.debug("***** Starting NEMO Conversion Queue Process *****");
                logger.debug("**************************************************");
                logger.debug("[perform] : context.getUser() = <" + context.getUser() + ">");
            }
            // init properties
            init(context);

            /*
             * if ("ISBG".equals(FPDMOrganizationName_mxJPO.getName(context))) { CONVERSION_REQUESTER_PASSWORD = FrameworkProperties.getProperty(context, "FPDM.OpenPDM.Password"); }
             */

            BusinessObject boNConversion = new BusinessObject(fpdm.nemo.NEMOUtils_mxJPO.TYPE_NEMO_CONVERSION_QUEUE, fpdm.nemo.NEMOUtils_mxJPO.NEMO_CONVERSION_QUEUE_OBJECT_NAME,
                    NEMO_CONVERSION_QUEUE_OBJECT_REVISION, fpdm.nemo.NEMOUtils_mxJPO.VAULT_PRODUCTION);
            DomainObject dobNemoConversion = DomainObject.newInstance(context, boNConversion);
            MapList mlObjToConvert = getObjectsToConvert(context, dobNemoConversion);
            if (logger.isDebugEnabled()) {
                logger.debug("[perform] : Nb Object Found = " + mlObjToConvert.size());
            }
            if (mlObjToConvert.size() > 0) {
                // Connect to NEMO Web-service
                WSConnect(context, NEMO_WS_ID_LOGIN, true);
                bNEMOConnected = true;
                hmSiteMap = getSiteMap();
                Collections.sort(mlObjToConvert, new RequestorSiteComparator(context));
                String sObjectType = null;
                String sObjectName = null;
                String sObjectRev = null;
                String sObjectId = null;
                String sRelationshipId = null;
                String sConversionRequester = null;
                String sNemoAction = null;
                String sConversionIdentifier = null;
                String sConversionResult = null;
                String sConversionStatus = null;
                String sConversionUserName = null;
                String sNemoSite = null;
                processSuccess();
                boolean bProcessConversionFail = false;
                DomainObject dobCADObject = null;
                DomainRelationship doRelation = null;

                for (Iterator<Map<String, Object>> itr = mlObjToConvert.iterator(); itr.hasNext();) {
                    bProcessConversionFail = false;
                    ContextUtil.startTransaction(context, true);
                    try {
                        if (fpdm.nemo.NEMOUtils_mxJPO.RANGE_PROCESS_IN_USE_FALSE.equals(getQueueStatus(context))) {
                            setQueueStatus(context, fpdm.nemo.NEMOUtils_mxJPO.RANGE_PROCESS_IN_USE_TRUE);
                            try {
                                Map<String, Object> mCADObject = itr.next();

                                sObjectType = (String) mCADObject.get(DomainConstants.SELECT_TYPE);
                                sObjectName = (String) mCADObject.get(DomainConstants.SELECT_NAME);
                                sObjectRev = (String) mCADObject.get(DomainConstants.SELECT_REVISION);
                                sObjectId = (String) mCADObject.get(DomainConstants.SELECT_ID);
                                sRelationshipId = (String) mCADObject.get(DomainRelationship.SELECT_ID);
                                sConversionRequester = (String) mCADObject.get("attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_CONVERSION_REQUESTER + "]");
                                // get conversion user name from requester site value
                                sConversionUserName = getConversionUserName(context, sConversionRequester);
                                if (logger.isDebugEnabled()) {
                                    logger.debug("[perform] sConversionUserName = <" + sConversionUserName + "> sConversionRequester = <" + sConversionRequester + "> ");
                                }
                                mCADObject.put(SELECT_CAD_CONVERSION_USER_NAME, sConversionUserName);
                                sNemoAction = (String) mCADObject.get("attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_NEMO_ACTION + "]");
                                sConversionStatus = (String) mCADObject.get("attribute[" + fpdm.nemo.NEMOUtils_mxJPO.getStatusAttribute(sNemoAction) + "]");
                                dobCADObject = DomainObject.newInstance(context, sObjectId);
                                doRelation = DomainRelationship.newInstance(context, sRelationshipId);
                                if (!"".equals(sConversionUserName) && sConversionUserName != null) {
                                    try {
                                        sNemoSite = getMappedUserSite(context, sConversionUserName);
                                    } catch (SiteNotMappedException snme) {
                                        // ignore this exception in this loop.
                                        logger.warn("[perform] - User <" + sConversionUserName + "> has a site non mapped with Nemo");
                                        processWarn();
                                        HashMap<String, String> hmObjectSummary = new HashMap<String, String>();
                                        hmObjectSummary.put("Error message", String.format(SITE_NOT_MAPPED_ERROR_MSG, sPDMUserSite));
                                        hmResultSummary.put(sNemoAction + sObjectId, hmObjectSummary);
                                        sNemoSite = DEFAULT_USER_SITE;
                                    } catch (ConversionUserNotExistException snme) {
                                        // user doesn't exist
                                        logger.warn("[perform] - Error on Object : (" + sObjectId + ") \"" + sObjectType + "\" \"" + sObjectName + "\" \"" + sObjectRev + "\"");
                                        String sFailureMessage = String.format(PDM_CAD_CONVERSION_USER_NOT_EXIST_ERROR_MSG, sConversionUserName);
                                        processConversionFail(context, dobCADObject, doRelation, sFailureMessage, "", sConversionUserName);
                                        if (logger.isDebugEnabled()) {
                                            logger.debug("[perform] - dobCADObject.getInfo(attribute[Conversion Status]) = <" + dobCADObject.getInfo(context, "attribute[Conversion Status]") + ">");
                                        }
                                        bProcessConversionFail = true;
                                    }
                                    if (!bProcessConversionFail) {
                                        performChangeSiteRequestNeeded = ((sNemoConnectedSite == null || (!sNemoConnectedSite.equals(sNemoSite))));
                                        if (logger.isDebugEnabled()) {
                                            logger.debug("[perform] performChangeSiteRequestNeeded = <" + performChangeSiteRequestNeeded + "> ");
                                            logger.debug("[perform] sNemoConnectedSite = <" + sNemoConnectedSite + "> sNemoSite = <" + sNemoSite + "> ");
                                        }
                                        sConversionIdentifier = (String) mCADObject.get("attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_CONVERSION_IDENTIFIANT + "]");
                                        sConversionResult = (String) mCADObject.get("attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_CONVERSION_RESULT + "]");
                                        String sPassStatus = MqlUtil.mqlCommand(context, "print person " + sConversionUserName + " select password dump", true);
                                        if (logger.isDebugEnabled()) {
                                            logger.debug("[perform] sPassStatus = <" + sPassStatus + ">");
                                        }

                                        if ("<NONE>".equals(sPassStatus) || sOpenPDMUser.equals(sConversionUserName)) {

                                            sConversionStatus = getCadObjectConversionStatus(context, sNemoAction, sRelationshipId, sObjectId);
                                            if (logger.isDebugEnabled()) {
                                                logger.debug("[perform] : Object <" + sObjectId + "> <" + sObjectType + "> <" + sObjectName + "> <" + sObjectRev + ">");
                                                logger.debug("[perform] : sNemoAction = <" + sNemoAction + ">");
                                                logger.debug("[perform] : sConversionIdentifier = <" + sConversionIdentifier + ">");
                                                logger.debug("[perform] : getStatusAttribute(sNemoAction) = <" + fpdm.nemo.NEMOUtils_mxJPO.getStatusAttribute(sNemoAction) + ">");
                                                logger.debug("[perform] : sQCCurrentStatus = <" + sConversionStatus + ">");
                                            }
                                            if (!"".equals(sConversionIdentifier)) {
                                                // Check-in request if status DONE sent
                                                if (fpdm.nemo.NEMOUtils_mxJPO.RANGE_CONVERSION_STATUS_DONE.equals(sConversionStatus) && "".equals(sConversionResult)) {
                                                    performSendCheckinRequest(context, mCADObject);
                                                }
                                            } else {
                                                if ("".equals(sConversionStatus)) {
                                                    performSendConversionRequest(context, mCADObject);
                                                }
                                            }
                                        } else {
                                            // check if the error message has been set before setting it again
                                            if ("".equals(sConversionStatus)) {
                                                logger.error("Error on Object : (" + sObjectId + ") \"" + sObjectType + "\" \"" + sObjectName + "\" \"" + sObjectRev + "\"");
                                                String sFailureMessage = String.format(PDM_USER_PASSWORD_NOT_EMPTY_ERROR_MSG, sConversionUserName);
                                                processConversionFail(context, dobCADObject, doRelation, sFailureMessage, "", sConversionUserName);
                                            }
                                        }
                                    }
                                } else {
                                    logger.error("Error on Object : (" + sObjectId + ") \"" + sObjectType + "\" \"" + sObjectName + "\" \"" + sObjectRev + "\"");
                                    logger.error(NO_CONVERSION_REQUESTER_ERROR_MSG);
                                    processFail();
                                    Map<String, String> hmObjectSummary = buildObjectConversionSummaryMap(context, dobCADObject, "SEND NEMO REQUEST", false, sConversionUserName);
                                    hmObjectSummary.put("Object Nemo Identifier", "");
                                    hmObjectSummary.put("Object Nemo Request Status", NO_CONVERSION_REQUESTER_ERROR_MSG);
                                    hmResultSummary.put(sNemoAction + sObjectId, hmObjectSummary);
                                }
                            } finally {
                                if (!fpdm.nemo.NEMOUtils_mxJPO.RANGE_PROCESS_IN_USE_LOCKED.equals(getQueueStatus(context))) {
                                    setQueueStatus(context, fpdm.nemo.NEMOUtils_mxJPO.RANGE_PROCESS_IN_USE_FALSE);
                                }
                            }
                        }
                        ContextUtil.commitTransaction(context);

                    } catch (RuntimeException e) {
                        ContextUtil.abortTransaction(context);
                        if (logger.isDebugEnabled()) {
                            logger.debug("perform() - Trans aborted.");
                        }
                        throw e;
                    } catch (Exception e) {
                        ContextUtil.abortTransaction(context);
                        if (logger.isDebugEnabled()) {
                            logger.debug("perform() - Trans aborted.");
                        }
                        throw e;
                    }
                }
            }
            MapList mlObjToCancel = getObjectsToCancel(context, dobNemoConversion);
            if (logger.isDebugEnabled()) {
                logger.debug("[perform] : mlObjToCancel = " + mlObjToCancel);
            }
            if (mlObjToCancel.size() > 0) {
                // Connect to NEMO Web-service
                if (!bNEMOConnected) {
                    WSConnect(context, NEMO_WS_ID_LOGIN, true);
                    bNEMOConnected = true;
                }
                String sMaxCancelTrials = dobNemoConversion.getInfo(context, "attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_FPDM_LIMIT_CANCEL_TRIALS + "]");

                int iMaxCancelTrials = 2;
                try {
                    iMaxCancelTrials = new Integer(sMaxCancelTrials);
                } catch (NumberFormatException nfe) {
                    logger.error("Could not transform maximum of cancellation trials to an int : " + sMaxCancelTrials);
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("sMaxCancelTrials : " + sMaxCancelTrials);
                    logger.debug("iMaxCancelTrials : " + iMaxCancelTrials);
                }

                for (Iterator<Map<String, Object>> itr = mlObjToCancel.iterator(); itr.hasNext();) {
                    ContextUtil.startTransaction(context, true);
                    try {
                        if (fpdm.nemo.NEMOUtils_mxJPO.RANGE_PROCESS_IN_USE_FALSE.equals(getQueueStatus(context))) {
                            setQueueStatus(context, fpdm.nemo.NEMOUtils_mxJPO.RANGE_PROCESS_IN_USE_TRUE);
                            try {
                                Map<String, Object> mCADObject = itr.next();
                                performSendCancelRequest(context, iMaxCancelTrials, mCADObject);
                            } finally {
                                if (!fpdm.nemo.NEMOUtils_mxJPO.RANGE_PROCESS_IN_USE_LOCKED.equals(getQueueStatus(context))) {
                                    setQueueStatus(context, fpdm.nemo.NEMOUtils_mxJPO.RANGE_PROCESS_IN_USE_FALSE);
                                }
                            }
                        }
                        ContextUtil.commitTransaction(context);
                    } catch (FrameworkException e) {
                        ContextUtil.abortTransaction(context);
                        logger.error("An error happened while trying to cancel a job", e);
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Error in perform", e);
            iResult = -1;
            processFail();
            throw e;
        } finally {
            if (bNEMOConnected) {
                WSDisconnect();
                bNEMOConnected = false;
            }
            if (logger.isDebugEnabled()) {
                logger.debug("************************************************");
                logger.debug("***** Ending NEMO Conversion Queue Process *****");
                logger.debug("************************************************");
            }
        }

        return iResult;
    }

    private static String getCadObjectConversionStatus(Context context, String sNemoAction, String sRelationshipId, String sCADObjectId) {
        if (sRelationshipId == null || sRelationshipId.equals("")) {
            logger.error("Relationship id is null or empty");
            return "NOT_QUEUED";
        }
        try {
            DomainRelationship drNemoRel = DomainRelationship.newInstance(context, sRelationshipId);
            boolean bIsRelOpened = false;
            try {
                drNemoRel.open(context);
                bIsRelOpened = true;
                if ("".equals(sNemoAction)) {
                    return fpdm.nemo.NEMOUtils_mxJPO.getSingleValue(fpdm.nemo.NEMOUtils_mxJPO.getSelectBusinessObjectData(context, sCADObjectId, SELECT_ATTRIBUTE_CONVERSION_STATUS));
                } else if (fpdm.nemo.NEMOUtils_mxJPO.RANGE_NEMO_ACTION_QC.equals(sNemoAction)) {
                    return fpdm.nemo.NEMOUtils_mxJPO.getSingleValue(fpdm.nemo.NEMOUtils_mxJPO.getSelectBusinessObjectData(context, sCADObjectId, SELECT_ATTRIBUTE_QC_CONVERSION_STATUS));
                } else {
                    return drNemoRel.getAttributeValue(context, fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_REL_CONVERSION_STATUS);
                }
            } catch (MatrixException e) {
                logger.error("Error happened while trying to open relationship : " + sRelationshipId, e);
                return "NOT_QUEUED";
            } finally {
                if (bIsRelOpened) {
                    try {
                        drNemoRel.close(context);
                    } catch (MatrixException e) {
                        logger.error("Error happened while trying to close relationship : " + sRelationshipId, e);
                    }
                }
            }
        } catch (FrameworkException e) {
            logger.error("Error happened while trying to instanciate relationship : " + sRelationshipId, e);
            return "NOT_QUEUED";
        }

    }

    /**
     * Build a Map having Object id, type, name and revision, the nemo process name, the requester name and the process status. This map will be used for request NEMO conversion
     * @param context
     *            ematrix context
     * @param domainObject
     *            the domain object
     * @param sProcessName
     *            the nemo process name
     * @param bProcessSucceeded
     *            the process status
     * @param sConversionRequester
     *            the requestor name
     * @return map containing informations needed for nemo
     * @throws Exception
     *             if error occurs
     */
    private Map<String, String> buildObjectConversionSummaryMap(Context context, DomainObject domainObject, String sProcessName, boolean bProcessSucceeded, String sConversionRequester)
            throws Exception {
        HashMap<String, String> hmObjectSummary = new HashMap<String, String>();
        if (domainObject != null) {
            String sObjectId;
            String sObjectType;
            String sObjectName;
            String sObjectRev;
            boolean closeNeeded = false;
            if (closeNeeded = (!domainObject.isOpen())) {
                domainObject.open(context);
            }
            sObjectId = domainObject.getObjectId();
            sObjectType = domainObject.getTypeName();
            sObjectName = domainObject.getName();
            sObjectRev = domainObject.getRevision();
            if (closeNeeded) {
                domainObject.close(context);
            }
            hmObjectSummary.put("Process", sProcessName);
            hmObjectSummary.put("Object Id", sObjectId);
            hmObjectSummary.put("Object Type", sObjectType);
            hmObjectSummary.put("Object Name", sObjectName);
            hmObjectSummary.put("Object Revision", sObjectRev);
            hmObjectSummary.put("Process Succeeded", String.valueOf(bProcessSucceeded));
            hmObjectSummary.put("Requester", sConversionRequester);
        } else {
            throw new Exception("Cannot build the Map for nemo with a null domain object");
        }
        return hmObjectSummary;
    }

    /**
     * Perform the change site request
     * @param context
     *            ematrix context
     * @param mCADObject
     *            map of CAD Object informations
     * @throws Exception
     *             if error occurs
     */
    private void performSendChangeSiteRequest(Context context, Map<String, Object> mCADObject) throws Exception {
        boolean changeSite = true;
        String sObjectType = (String) mCADObject.get(DomainConstants.SELECT_TYPE);
        String sObjectName = (String) mCADObject.get(DomainConstants.SELECT_NAME);
        String sObjectRev = (String) mCADObject.get(DomainConstants.SELECT_REVISION);
        String sObjectId = (String) mCADObject.get(DomainConstants.SELECT_ID);
        String sNemoAction = (String) mCADObject.get("attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_NEMO_ACTION + "]");

        try {
            sUserSite = getUserSite(context, sObjectId, sNemoAction);
        } catch (SiteNotMappedException e) {
            logger.error("Send Job Error on Object : (" + sObjectId + ") \"" + sObjectType + "\" \"" + sObjectName + "\" \"" + sObjectRev + "\"");
            sUserSite = DEFAULT_USER_SITE;
        } catch (Exception e) {
            logger.error("Error in performSendChangeSiteRequest()", e);
            changeSite = false;
            throw new Exception("NEMO Web service Change site failed : Access problem with login : " + fpdm.nemo.NEMOUtils_mxJPO.PROPERTY_NEMO_LOGIN);
        } finally {
            // Change the NEMO Site
            if (logger.isDebugEnabled()) {
                logger.debug("[performSendChangeSiteRequest] : sUserSite = <" + sUserSite + ">");
            }
            if (changeSite) {
                HashMap<String, String> hmParam = new HashMap<String, String>();
                hmParam.put("OPT_site", sUserSite.toLowerCase());
                try {
                    sendJob(hmParam, NEMO_WS_ID_CHANGE_SITE);
                    sNemoConnectedSite = sUserSite;
                } catch (Exception e) {
                    if (sNemoConnectedSite == null) {
                        throw new Exception("Failled to change NEMO Site to " + sUserSite + ". With error " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Perform the checkin request
     * @param context
     *            ematrix context
     * @param mCADObject
     *            map of CAD Object informations
     * @throws Exception
     *             if error occurs
     */
    private void performSendCheckinRequest(Context context, Map<String, Object> mCADObject) throws Exception {
        // Check-in request if status DONE sent
        if (logger.isDebugEnabled()) {
            logger.debug("performSendCheckinRequest() - mCADObject = <" + mCADObject + "> ");
        }
        String sObjectType = (String) mCADObject.get(DomainConstants.SELECT_TYPE);
        boolean bIs2d = fpdm.nemo.NEMOUtils_mxJPO.is2d(sObjectType);
        String sObjectId = (String) mCADObject.get(DomainConstants.SELECT_ID);
        String sConversionIdentifier = (String) mCADObject.get("attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_CONVERSION_IDENTIFIANT + "]");
        String sNemoAction = (String) mCADObject.get("attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_NEMO_ACTION + "]");
        String sConversionStatus = (String) mCADObject.get("attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_CONVERSION_STATUS + "]");
        String sConversionUserName = (String) mCADObject.get(SELECT_CAD_CONVERSION_USER_NAME);

        String sViewableFormat = "";
        String sStatusAttributeName = fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_CONVERSION_STATUS;
        if (sNemoAction != null && sNemoAction.equals(fpdm.nemo.NEMOUtils_mxJPO.RANGE_NEMO_ACTION_QC)) {
            sViewableFormat = fpdm.nemo.NEMOUtils_mxJPO.RANGE_NEMO_ACTION_QC;
            sStatusAttributeName = fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_QC_CURRENT_ACTION;
        } else if (sNemoAction != null && sNemoAction.length() == 0) {
            if (bIs2d) {
                sViewableFormat = "2D";
            } else {
                sViewableFormat = "3D";
            }
        } else {
            sViewableFormat = "other";
        }
        // perform change site
        if (performChangeSiteRequestNeeded) {
            performSendChangeSiteRequest(context, mCADObject);
        }

        Map<String, String> hmObjectSummary = null;
        if (hmResultSummary.containsKey(sNemoAction + sObjectId)) {
            hmObjectSummary = hmResultSummary.get(sNemoAction + sObjectId);
        } else {
            hmObjectSummary = new HashMap<String, String>();
        }
        try {
            sendCheckinRequest(context, sObjectId, sConversionIdentifier, sViewableFormat, sNemoAction);
        } catch (SendJobException e) {
            logger.error("Send Job Error on Object : " + sObjectId, e);
            handleCheckinDemandFail(context, (String) mCADObject.get(DomainRelationship.SELECT_ID), e.getMessage());
            return;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Send Job Error on Object : " + sObjectId, e);
            handleCheckinDemandFail(context, (String) mCADObject.get(DomainRelationship.SELECT_ID), "Technical error : " + e.getMessage());
            return;
        }
        DomainObject dobCADObject = DomainObject.newInstance(context, sObjectId);
        if (sNemoAction != null && sNemoAction.length() > 0 && !sNemoAction.equals(fpdm.nemo.NEMOUtils_mxJPO.RANGE_NEMO_ACTION_QC)) {
            String sRelId = (String) mCADObject.get(DomainRelationship.SELECT_ID);
            DomainRelationship drRelObject = new DomainRelationship(sRelId);
            drRelObject.setAttributeValue(context, fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_REL_CONVERSION_STATUS, fpdm.nemo.NEMOUtils_mxJPO.RANGE_CONVERSION_STATUS_CHECKIN_REQUEST);
        } else {
            dobCADObject.setAttributeValue(context, sStatusAttributeName, fpdm.nemo.NEMOUtils_mxJPO.RANGE_CONVERSION_STATUS_CHECKIN_REQUEST);
        }
        bJobNotif = false;
        hmObjectSummary.putAll(buildObjectConversionSummaryMap(context, dobCADObject, "CHECKIN", true, sConversionUserName));
        hmObjectSummary.put("Object Conversion Identifier", sConversionIdentifier);
        hmObjectSummary.put("Object Conversion Status", sConversionStatus);
        hmResultSummary.put(sNemoAction + sObjectId, hmObjectSummary);
    }

    private void handleCheckinDemandFail(Context context, String sRelId, String sErrorMessage) {
        String sUserErrorMessage = "Checkin Failed : " + sErrorMessage;
        DomainRelationship drRelObject = new DomainRelationship(sRelId);
        try {
            drRelObject.setAttributeValue(context, fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_CONVERSION_RESULT, sUserErrorMessage);
        } catch (FrameworkException fe) {
            // unable to update relationship => Object will be re-processed next time
            logger.error("Unable to set the checkin error message : ", fe);
        }
    }

    /**
     * Perform the conversion cancellation
     * @param context
     *            ematrix context
     * @param iMaxCancelTrials
     *            maximum number of cancellation trials
     * @param mCADObject
     *            map of CAD Object informations
     * @throws Exception
     *             if error occurs
     */
    private void performSendCancelRequest(Context context, int iMaxCancelTrials, Map<String, Object> mCADObject) {
        String sObjectId = (String) mCADObject.get(DomainConstants.SELECT_ID);
        String sRelId = (String) mCADObject.get(DomainRelationship.SELECT_ID);
        if (logger.isDebugEnabled()) {
            logger.debug("in performSendCancelRequest");
        }
        boolean bCancellationOk = false;
        try {
            String sFormattedKey = (String) mCADObject.get(SELECT_ATTRIBUTE_CONVERSION_IDENTIFIANT);
            HashMap<String, String> hmParam = new HashMap<String, String>();
            if (sFormattedKey != null && sFormattedKey.indexOf("spooler://") == 0) {
                sFormattedKey = sFormattedKey.substring(10);

            }
            hmParam.put(fpdm.nemo.NEMOUtils_mxJPO.NEMOConstants.OPT_SPOOLER, sFormattedKey);
            sendJobAction(hmParam, NEMO_WS_ID_JOB_CANCEL, "submit");
            if (logger.isDebugEnabled()) {
                logger.debug("Cancelling job : " + sFormattedKey);
            }
            bCancellationOk = true;
        } catch (Exception e) {
            logger.error("Error happened while trying to cancel JOB on NEMO for object : " + sObjectId, e);
        }

        try {
            if (bCancellationOk) {
                DomainRelationship.disconnect(context, sRelId);
                if (logger.isDebugEnabled()) {
                    logger.debug("Cancellation has been successfully processed on object : " + sObjectId);
                }
            } else {
                DomainRelationship drRelObject = new DomainRelationship(sRelId);
                String sCancelTrials = drRelObject.getAttributeValue(context, fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_REL_CONVERSION_STATUS);
                int iCancelTrials = 0;
                try {
                    iCancelTrials = new Integer(sCancelTrials);
                } catch (NumberFormatException nfe) {
                    logger.error("Number of cancellation trials is not a number on object : " + sObjectId + ". Attribute value is : " + sCancelTrials);
                }
                iCancelTrials++;
                if (iCancelTrials >= iMaxCancelTrials) {
                    DomainRelationship.disconnect(context, sRelId);
                    logger.error("Maximum number of cancellation trials has been attained for object : " + sObjectId);
                    logger.error("Cancellation has been aborted => relationship between object and Nemo queue with cancel action has been diconnected");
                } else {
                    drRelObject.setAttributeValue(context, fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_REL_CONVERSION_STATUS, Integer.toString(iCancelTrials));
                }
            }
        } catch (FrameworkException e) {
            logger.error("An error happened while handeling the result of the Nemo JOB cancellation process on object : " + sObjectId);
        }
    }

    /**
     * Perform the conversion request
     * @param context
     *            ematrix context
     * @param mCADObject
     *            map of CAD Object informations
     * @throws Exception
     *             if error occurs
     */
    private void performSendConversionRequest(Context context, Map<String, Object> mCADObject) throws Exception {
        boolean bHasProcess;
        String sObjectType = (String) mCADObject.get(DomainConstants.SELECT_TYPE);
        String sObjectName = (String) mCADObject.get(DomainConstants.SELECT_NAME);
        String sObjectRev = (String) mCADObject.get(DomainConstants.SELECT_REVISION);
        String sObjectId = (String) mCADObject.get(DomainConstants.SELECT_ID);
        String sNemoAction = (String) mCADObject.get("attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_NEMO_ACTION + "]");
        String sRelId = (String) mCADObject.get(DomainRelationship.SELECT_ID);
        DomainRelationship drRelObject = new DomainRelationship(sRelId);
        if (logger.isDebugEnabled()) {
            logger.debug("in performSendConversionRequest");
        }
        String sConversionUserName = (String) mCADObject.get(SELECT_CAD_CONVERSION_USER_NAME);
        DomainObject dobConnectedCADObject = DomainObject.newInstance(context, sObjectId);
        if (fpdm.nemo.NEMOUtils_mxJPO.TYPE_CATPART.equals(sObjectType)
                || (sNemoAction.length() == 0 && (fpdm.nemo.NEMOUtils_mxJPO.TYPE_UG_MODEL.equals(sObjectType) || fpdm.nemo.NEMOUtils_mxJPO.TYPE_UG_DRAWING.equals(sObjectType)
                        || fpdm.nemo.NEMOUtils_mxJPO.TYPE_CATDRAWING.equals(sObjectType) || fpdm.nemo.NEMOUtils_mxJPO.TYPE_FPDM_CAD_COMPONENT.equals(sObjectType)
                        || fpdm.nemo.NEMOUtils_mxJPO.TYPE_CATIA_V4_MODEL.equals(sObjectType) || fpdm.nemo.NEMOUtils_mxJPO.TYPE_CATIA_CGR.equals(sObjectType)))
                || (sNemoAction.equals(fpdm.nemo.NEMOUtils_mxJPO.RANGE_NEMO_ACTION_QC)
                        && (fpdm.nemo.NEMOUtils_mxJPO.TYPE_CATPRODUCT.equals(sObjectType) || fpdm.nemo.NEMOUtils_mxJPO.TYPE_CATDRAWING.equals(sObjectType)))
                || ((sNemoAction.length() > 0 && !sNemoAction.equals(fpdm.nemo.NEMOUtils_mxJPO.RANGE_NEMO_ACTION_QC)) && (fpdm.nemo.NEMOUtils_mxJPO.TYPE_UG_MODEL.equals(sObjectType)
                        || fpdm.nemo.NEMOUtils_mxJPO.TYPE_UG_ASSEMBLY.equals(sObjectType) || fpdm.nemo.NEMOUtils_mxJPO.TYPE_UG_DRAWING.equals(sObjectType)
                        || fpdm.nemo.NEMOUtils_mxJPO.TYPE_CATDRAWING.equals(sObjectType) || fpdm.nemo.NEMOUtils_mxJPO.TYPE_CATPRODUCT.equals(sObjectType)
                        || fpdm.nemo.NEMOUtils_mxJPO.TYPE_FPDM_CAD_COMPONENT.equals(sObjectType) || fpdm.nemo.NEMOUtils_mxJPO.TYPE_FPDM_CAD_DRAWING.equals(sObjectType)
                        || fpdm.nemo.NEMOUtils_mxJPO.TYPE_CATIA_V4_MODEL.equals(sObjectType) || fpdm.nemo.NEMOUtils_mxJPO.TYPE_CATIA_CGR.equals(sObjectType)))) {

            if (fpdm.nemo.NEMOUtils_mxJPO.hasSupportedFiles(context, sObjectId, alSupportedFileFormat)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Object has file");
                }

                // perform change site
                if (performChangeSiteRequestNeeded) {
                    performSendChangeSiteRequest(context, mCADObject);
                }

                try {
                    String sCADObjectFileName = fpdm.nemo.NEMOUtils_mxJPO.getCADObjectFileName(context, sObjectId, alSupportedFileFormat);
                    HashMap<String, String> hmParam = new HashMap<String, String>();
                    hmParam.put(fpdm.nemo.NEMOUtils_mxJPO.NEMOConstants.OPT_PDM_URL, PDM_HOST);
                    hmParam.put(fpdm.nemo.NEMOUtils_mxJPO.NEMOConstants.OPT_CAD_OBJECT_ID, sObjectId);
                    hmParam.put(fpdm.nemo.NEMOUtils_mxJPO.NEMOConstants.OPT_CHECKOUT_TICKET, "");
                    hmParam.put(fpdm.nemo.NEMOUtils_mxJPO.NEMOConstants.OPT_CHECKOUT_ACTION, "");
                    hmParam.put(fpdm.nemo.NEMOUtils_mxJPO.NEMOConstants.OPT_NEMO_ID, "");
                    hmParam.put("OPT_same_ftp", "NO");
                    hmParam.put("OPT_queue", "normal");// low normal priority
                    hmParam.put(fpdm.nemo.NEMOUtils_mxJPO.NEMOConstants.OPT_PARAM1, sConversionUserName);
                    if (sNemoAction.equals(fpdm.nemo.NEMOUtils_mxJPO.RANGE_NEMO_ACTION_QC)) {
                        bHasProcess = populateParametersForQChecker(hmParam, sObjectType, sCADObjectFileName, null);
                        if (bHasProcess) {
                            // Specific settings
                            hmParam.put(fpdm.nemo.NEMOUtils_mxJPO.NEMOConstants.OPT_PARAM5, fpdm.nemo.NEMOUtils_mxJPO.RANGE_NEMO_ACTION_QC);
                            StringList slSelect = new StringList();
                            slSelect.addElement("attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_QC_PROFILE + "]");
                            slSelect.addElement("attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_QC_ENVIRONMENT + "]");
                            Map<?, ?> mViewableInfos = fpdm.nemo.NEMOUtils_mxJPO.getViewableInfos(context, dobConnectedCADObject, fpdm.nemo.NEMOUtils_mxJPO.TYPE_QC_VIEWABLE, slSelect);
                            String sQCProfile = (String) mViewableInfos.get("attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_QC_PROFILE + "]");
                            String sQCEnvironment = (String) mViewableInfos.get("attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_QC_ENVIRONMENT + "]");
                            Map<String, ArrayList<String>> mProfiles = getQualityCheckerProfiles(context, new String[] { "CONNECTED" });
                            ArrayList<String> alValue = mProfiles.get(sQCEnvironment);
                            if (alValue == null || !alValue.contains(sQCProfile)) {
                                logger.error("Object : (" + sObjectId + ") \"" + sObjectType + "\" \"" + sObjectName + "\" \"" + sObjectRev + "\"");
                                logger.error("Erroneous Profile Environment");
                                // If the Send Job Process Failed, we update the QC Current Action to Failed
                                processConversionFail(context, dobConnectedCADObject, drRelObject, ERRONEOUS_ENVIRONMENT_PROFILE_ERROR_MSG, sNemoAction, sConversionUserName);
                                return;
                            }
                            hmParam.put("OPT_EXTRA_environment", sQCEnvironment); // depends of CAD Object
                            hmParam.put("OPT_EXTRA_profile", sQCProfile); // override stored configuration because depends of CAD Object
                        }
                    } else if (sNemoAction.length() == 0) {
                        String sCADObjectFileFormat = null;
                        if (fpdm.nemo.NEMOUtils_mxJPO.TYPE_FPDM_CAD_COMPONENT.equals(sObjectType)) {
                            sCADObjectFileFormat = fpdm.nemo.NEMOUtils_mxJPO.getCADObjectFileFormat(context, sObjectId, alSupportedFileFormat);
                        }
                        bHasProcess = populateParametersForConversions(hmParam, sObjectType, sCADObjectFileName, sCADObjectFileFormat);
                        if (bHasProcess) {
                            // Specific settings
                            hmParam.put(fpdm.nemo.NEMOUtils_mxJPO.NEMOConstants.OPT_PARAM3, ("OpenPDM".equals(sConversionUserName)) ? (CONVERSION_REQUESTER_PASSWORD) : (""));
                            String sNbFolio = (String) mCADObject.get("attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_FOLIO_NUMBER + "]");
                            if (fpdm.nemo.NEMOUtils_mxJPO.is2d(sObjectType)) {
                                hmParam.put(fpdm.nemo.NEMOUtils_mxJPO.NEMOConstants.OPT_PARAM2, sNbFolio);
                            }
                        }
                    } else {
                        bHasProcess = populateConversionOptions(context, hmParam, dobConnectedCADObject, sObjectType, sCADObjectFileName, sNemoAction);
                    }

                    if (bHasProcess) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("bHasProcess = <" + bHasProcess + ">");
                        }

                        // File size process
                        long lTotalFileSize = getInputFileSize(context, dobConnectedCADObject, sNemoAction);
                        hmParam.put(fpdm.nemo.NEMOUtils_mxJPO.NEMOConstants.OPT_INPUT_FILE_SIZE, Long.toString(lTotalFileSize));

                        // DEBUG
                        // FPDMCommon_mxJPO.logMap(logger, hmParam);

                        String sFlowId = "";
                        try {
                            sFlowId = sendJobAction(context, hmParam, NEMO_WS_ID_PDM_START_TREATMENT, "submit.service");
                        } catch (SendJobException e) {
                            logger.error("Send Job Error on Object : (" + sObjectId + ") \"" + sObjectType + "\" \"" + sObjectName + "\" \"" + sObjectRev + "\"", e);
                            processConversionFail(context, dobConnectedCADObject, drRelObject, SEND_JOB_FAIL_MSG + e.getMessage(), sNemoAction, sConversionUserName);
                            return;
                        } catch (RuntimeException e) {
                            throw e;
                        } catch (Exception e) {
                            logger.error("Send Job Error on Object : (" + sObjectId + ") \"" + sObjectType + "\" \"" + sObjectName + "\" \"" + sObjectRev + "\"", e);
                            processConversionFail(context, dobConnectedCADObject, drRelObject, SEND_JOB_FAIL_MSG + "Technical Error : " + e.getMessage(), sNemoAction, sConversionUserName);
                            return;
                        }
                        // Thread.sleep(PAUSE_AFTER_SEND_JOB);
                        if (logger.isDebugEnabled()) {
                            logger.debug("sFlowId = <" + sFlowId + ">");
                        }

                        bJobNotif = false;

                        // If we send a conversion request, we update the
                        // Conversion Identifiant attribute of the CAD
                        // Object.
                        drRelObject.setAttributeValue(context, fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_CONVERSION_IDENTIFIANT, sFlowId);
                        drRelObject.setAttributeValue(context, fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_CONVERSION_RESULT, "");
                        if (fpdm.nemo.NEMOUtils_mxJPO.isOnDemandConversion(sNemoAction)) {
                            drRelObject.setAttributeValue(context, fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_REL_CONVERSION_STATUS, fpdm.nemo.NEMOUtils_mxJPO.RANGE_CONVERSION_STATUS_QUEUED);
                        } else {
                            dobConnectedCADObject.setAttributeValue(context, fpdm.nemo.NEMOUtils_mxJPO.getStatusAttribute(sNemoAction), fpdm.nemo.NEMOUtils_mxJPO.RANGE_CONVERSION_STATUS_QUEUED);
                        }

                        try {
                            Map<String, String> hmObjectSummary;
                            if (hmResultSummary.containsKey(sNemoAction + sObjectId)) {
                                hmObjectSummary = hmResultSummary.get(sNemoAction + sObjectId);
                            } else {
                                hmObjectSummary = new HashMap<String, String>();
                            }
                            hmObjectSummary.putAll(buildObjectConversionSummaryMap(context, dobConnectedCADObject, fpdm.nemo.NEMOUtils_mxJPO.getProcessLabel(sNemoAction), true, sConversionUserName));
                            hmObjectSummary.put(fpdm.nemo.NEMOUtils_mxJPO.getIdentifierLabel(sNemoAction), sFlowId);
                            hmObjectSummary.put(fpdm.nemo.NEMOUtils_mxJPO.getStatusLabel(sNemoAction), fpdm.nemo.NEMOUtils_mxJPO.RANGE_CONVERSION_STATUS_QUEUED);
                            hmResultSummary.put(sNemoAction + sObjectId, hmObjectSummary);
                        } catch (Exception e) {
                            logger.error("An error happened while trying to construct the summary => no need to set the conversion to Failed!", e);
                        }
                    }

                } catch (Exception e) {
                    logger.error("Send Job Error on Object : (" + sObjectId + ") \"" + sObjectType + "\" \"" + sObjectName + "\" \"" + sObjectRev + "\"", e);
                    // If the Send Job Process Failed, we update the
                    // Conversion Status to SEND_JOB_ERROR_MSG
                    processConversionFail(context, dobConnectedCADObject, drRelObject, SEND_JOB_ERROR_MSG, sNemoAction, sConversionUserName);

                }
            } else {

                logger.error("Object : (" + sObjectId + ") \"" + sObjectType + "\" \"" + sObjectName + "\" \"" + sObjectRev + "\"");
                logger.error("This object has no supported file to convert");
                // If the Send Job Process Failed, we update the Conversion
                // Status to SEND_JOB_ERROR_MSG
                processConversionFail(context, dobConnectedCADObject, drRelObject, NO_FILE_ERROR_MSG, sNemoAction, sConversionUserName);

            }
        } else {
            logger.error("Type <" + sObjectType + "> has no process, view JPO FPDMNEMOInterface (id : <" + sObjectId + "> name : <" + sObjectName + "> rev : <" + sObjectRev + ">)");
            processConversionFail(context, dobConnectedCADObject, drRelObject, OBJECT_TYPE_NOT_RECOGNIZED_ERROR_MSG, sNemoAction, sConversionUserName);
        }
    }

    private boolean populateConversionOptions(Context context, Map<String, String> hmParam, DomainObject dobConnectedCADObject, String sObjectType, String sCADObjectFileName, String sNemoAction)
            throws Exception {
        boolean bHasProcess = true;
        StringList slSelect = new StringList();
        slSelect.addElement(SELECT_ATTRIBUTE_CHAIN);
        slSelect.addElement(SELECT_ATTRIBUTE_OPTIONS);
        String sTypePattern = fpdm.nemo.NEMOUtils_mxJPO.TYPE_FPDM_2D_NEUTRAL_VIEWABLE + "," + fpdm.nemo.NEMOUtils_mxJPO.TYPE_FPDM_3D_NEUTRAL_VIEWABLE;
        MapList mlViewableInfos = fpdm.nemo.NEMOUtils_mxJPO.getViewablesInfos(context, dobConnectedCADObject, sTypePattern, slSelect);
        for (Object oViewableInfos : mlViewableInfos) {
            if (oViewableInfos instanceof Map) {
                Map<?, ?> mViewableInfos = (Map<?, ?>) oViewableInfos;
                String sChain = (String) mViewableInfos.get(SELECT_ATTRIBUTE_CHAIN);
                if (sNemoAction.equals(sChain)) {
                    String sOptions = (String) mViewableInfos.get(SELECT_ATTRIBUTE_OPTIONS);
                    HashMap<String, Object> hmSelectedOptions = fpdm.nemo.NEMOUtils_mxJPO.getConvOptionsFromString(sOptions);
                    if (logger.isDebugEnabled()) {
                        logger.debug("sOptions : " + sOptions);
                        logger.debug("hmSelectedOptions : " + hmSelectedOptions);
                    }
                    HashMap<String, Object> hmDefaultOptions = retrieveExtraHiddenOptions(context, sChain);
                    for (Entry<String, Object> eEntry : hmDefaultOptions.entrySet()) {
                        String sOptionName = eEntry.getKey();
                        Object oOption = eEntry.getValue();
                        if (oOption instanceof Map<?, ?>) {
                            HashMap<?, ?> hmOption = (HashMap<?, ?>) eEntry.getValue();
                            String sType = (String) hmOption.get(fpdm.nemo.NEMOUtils_mxJPO.XML_TYPE);
                            if (fpdm.nemo.NEMOUtils_mxJPO.VAL_HIDDEN.equals(sType)) {
                                hmParam.put(sOptionName, (String) hmOption.get(fpdm.nemo.NEMOUtils_mxJPO.XML_VALUE));
                            }
                        }
                    }
                    for (Entry<String, Object> eEntry : hmSelectedOptions.entrySet()) {
                        String sOptName = eEntry.getKey();
                        Object oOptValue = eEntry.getValue();
                        if (oOptValue instanceof String) {
                            hmParam.put(sOptName, (String) oOptValue);
                        }
                    }
                }
            }
        }
        hmParam.put(fpdm.nemo.NEMOUtils_mxJPO.NEMOConstants.OPT_TREATMENT, sNemoAction);
        if (fpdm.nemo.NEMOUtils_mxJPO.TYPE_CATPRODUCT.equals(sObjectType) || fpdm.nemo.NEMOUtils_mxJPO.TYPE_UG_ASSEMBLY.equals(sObjectType)) {
            sCADObjectFileName = getFileNameWithoutExtension(sCADObjectFileName) + ".zip";
        }
        hmParam.put(fpdm.nemo.NEMOUtils_mxJPO.NEMOConstants.OPT_FILE_NAME, sCADObjectFileName);// Mandatory parameter
        hmParam.put(fpdm.nemo.NEMOUtils_mxJPO.NEMOConstants.OPT_PARAM5, sNemoAction);// Parameter to recognize the treatment chain
        return bHasProcess;
    }

    /**
     * Search the file size of the given CAD Object.
     * @param context
     *            ematrix context
     * @param sObjectType
     *            the CAD object type
     * @param sObjectCurrent
     *            the CAD Object current
     * @param sObjectId
     *            the CAD Object id
     * @param dobConnectedCADObject
     *            the domain object instance
     * @return its size
     * @throws FrameworkException
     *             if error occurs during attribute search
     * @throws Exception
     *             if error occurs while during file size search
     */
    private long getInputFileSize(Context context, DomainObject dobConnectedCADObject, String sNemoAction) throws FrameworkException, Exception {
        long lTotalFileSize = 0;
        Collection<String> alAddedObjectId = new HashSet<String>();

        Calendar startTime = new GregorianCalendar();
        Calendar endTime = new GregorianCalendar();
        // set current time
        startTime.setTime(new Date());

        SelectList slBus = new SelectList();
        slBus.addElement(DomainConstants.SELECT_ID);
        slBus.addElement(DomainConstants.SELECT_TYPE);
        slBus.addElement(DomainConstants.SELECT_CURRENT);

        Map<?, ?> mBusInfo = dobConnectedCADObject.getInfo(context, slBus);

        String sObjectType = (String) mBusInfo.get(DomainConstants.SELECT_TYPE);
        String sObjectCurrent = (String) mBusInfo.get(DomainConstants.SELECT_CURRENT);
        String sObjectId = (String) mBusInfo.get(DomainConstants.SELECT_ID);
        if (logger.isDebugEnabled()) {
            logger.warn("getInputFileSize() - sObjectId = <" + sObjectId + "> sObjectType = <" + sObjectType + "> sObjectCurrent = <" + sObjectCurrent + ">");
        }

        if (fpdm.nemo.NEMOUtils_mxJPO.TYPE_UG_DRAWING.equals(sObjectType) || fpdm.nemo.NEMOUtils_mxJPO.TYPE_CATPRODUCT.equals(sObjectType)
                || (fpdm.nemo.NEMOUtils_mxJPO.TYPE_CATDRAWING.equals(sObjectType) && fpdm.nemo.NEMOUtils_mxJPO.RANGE_NEMO_ACTION_QC.equals(sNemoAction))) {
            DomainObject dobCADObject = dobConnectedCADObject;
            if (STATE_CADOBJECT_PRELIMINARY.equals(sObjectCurrent)) {
                String sVersionedId = dobCADObject.getInfo(context, "from[" + fpdm.nemo.NEMOUtils_mxJPO.RELATIONSHIP_ACTIVE_VERSION + "].to." + DomainConstants.SELECT_ID);
                dobCADObject = DomainObject.newInstance(context, sVersionedId);
                lTotalFileSize += searchFilesize(context, sVersionedId, alAddedObjectId);
            } else {
                lTotalFileSize += searchFilesize(context, sObjectId, alAddedObjectId);
            }
            StringList slSelectable = new StringList();
            slSelectable.addElement(DomainConstants.SELECT_ID);
            String sHeadObjectId = null;
            String sSubCADObjectId = dobCADObject.getInfo(context, "to[" + fpdm.nemo.NEMOUtils_mxJPO.RELATIONSHIP_ASSOCIATED_DRAWING + "].from." + DomainConstants.SELECT_ID);
            if (!fpdm.nemo.NEMOUtils_mxJPO.isNullOrEmpty(sSubCADObjectId)) {
                lTotalFileSize += searchFilesize(context, sSubCADObjectId, alAddedObjectId);
                sHeadObjectId = sSubCADObjectId;
            } else if (fpdm.nemo.NEMOUtils_mxJPO.TYPE_CATPRODUCT.equals(sObjectType)) {
                sHeadObjectId = sObjectId;
            }
            if (sHeadObjectId != null) {
                dobCADObject = DomainObject.newInstance(context, sHeadObjectId);
                MapList mlObjects = dobCADObject.getRelatedObjects(context, fpdm.nemo.NEMOUtils_mxJPO.RELATIONSHIP_CAD_SUBCOMPONENT, "*", slSelectable, null, false, true, (short) 0, "", "", 0);
                if (logger.isDebugEnabled()) {
                    logger.debug("getInputFileSize() - mlObjects = <" + mlObjects + ">");
                }
                for (Iterator<?> iter = mlObjects.iterator(); iter.hasNext();) {
                    Hashtable<?, ?> htObject = (Hashtable<?, ?>) iter.next();
                    String sSubObjectId = (String) htObject.get(DomainConstants.SELECT_ID);
                    lTotalFileSize += searchFilesize(context, sSubObjectId, alAddedObjectId);
                }
            }
        } else {
            if (STATE_CADOBJECT_PRELIMINARY.equals(sObjectCurrent)) {
                String sCADObjectId = dobConnectedCADObject.getInfo(context, "from[" + fpdm.nemo.NEMOUtils_mxJPO.RELATIONSHIP_ACTIVE_VERSION + "].to." + DomainConstants.SELECT_ID);
                if (sCADObjectId != null && sCADObjectId.length() > 0) {
                    sObjectId = sCADObjectId;
                }
            }
            lTotalFileSize = searchFilesize(context, sObjectId, alAddedObjectId);
        }
        if (logger.isDebugEnabled()) {
            endTime.setTime(new Date());
            if (logger.isDebugEnabled()) {
                logger.debug("Call getInputFileSize() duration : " + (endTime.getTimeInMillis() - startTime.getTimeInMillis()) + " ms");
                logger.debug("getInputFileSize() Object <" + dobConnectedCADObject.toString() + ">lTotalFileSize : <" + lTotalFileSize + ">");
            }
        }

        return lTotalFileSize;
    }

    /**
     * Search file size only if the given object id is not present inside the given collection
     * @param context
     *            ematrix context
     * @param sObjectId
     *            the Object id
     * @param alAddedObjectId
     *            the already treated object Ids
     * @return 0 if the Object is already treated, its size otherwise
     * @throws Exception
     */
    private long searchFilesize(Context context, String sObjectId, Collection<String> alAddedObjectId) throws Exception {
        long lFileSize = 0;
        if (!alAddedObjectId.contains(sObjectId)) {
            try {
                lFileSize = fpdm.nemo.NEMOUtils_mxJPO.getFileSizeFromCADObject(context, sObjectId, alSupportedFileFormat);
            } catch (NumberFormatException e) {
                logger.error(e.getMessage());
            }
            alAddedObjectId.add(sObjectId);
        }
        return lFileSize;
    }

    /**
     * Extract the soap response from the given StringHolder
     * @param so
     *            the StringHolder to parse
     * @return Map containing informations returned by the web service
     * @throws Exception
     *             if error occurs
     */
    private HashMap<String, String> extractSOAPResponse(StringHolder so) throws Exception {
        HashMap<String, String> hmReturnMap = new HashMap<String, String>();
        SAXBuilder sxb = new SAXBuilder();
        Document document = null;
        try {
            if (so == null || so.value == null || so.value.length() == 0) {
                logger.error("Soap response empty or null");
                return hmReturnMap;
            }
            if (logger.isDebugEnabled()) {
                logger.debug(" so.value set to : " + so.value);
            }
            document = sxb.build(new StringReader(so.value));
            Element el = document.getRootElement().getChild("output", nsef);
            String[] sa = el.getTextTrim().split("\\s+", -1);
            for (String s : sa) {
                String[] saElement = s.split(":", -1);
                if (saElement[0].trim().length() > 0) {
                    hmReturnMap.put(saElement[0], saElement[1]);
                }
            }
        } catch (Exception e) {
            logger.error("Error in extractSOAPResponse", e);
            throw e;
        }
        return hmReturnMap;
    }

    /**
     * Return the file name without the extension <br/>
     * @FIXME Use {@link org.apache.commons.io.FilenameUtils#getBaseName(String)} instead.
     * @param fileName
     *            File name with extension
     * @return String
     */
    private String getFileNameWithoutExtension(String fileName) {
        int whereDot = fileName.lastIndexOf('.');
        if (0 < whereDot && whereDot <= fileName.length() - 2) {
            return fileName.substring(0, whereDot);
            // extension = filename.substring(whereDot+1);
        }
        return fileName;
    }

    /**
     * Extract nemo spooler name from the given string
     * @param sFlowId
     *            the string
     * @return the nemp spooler name
     * @throws Exception
     *             if error occurs
     */
    private String extractNemoSpooler(String sFlowId) throws Exception {
        String ResultString = null;
        try {
            Pattern regex = Pattern.compile("spooler://(.*)");
            Matcher regexMatcher = regex.matcher(sFlowId);
            if (regexMatcher.find()) {
                ResultString = regexMatcher.group(1);
            }
        } catch (Exception e) {
            logger.error("Error in extractNemoId", e);
            throw e;
        }
        return ResultString;
    }

    /**
     * Connect to the web service
     * @param context
     *            ematrix context
     * @param faureciaWSid
     *            Webservice id
     * @param bRetryLogin
     *            If true, process has to retry a connection on NEMO (if Login/Password have been updated)
     * @throws Exception
     *             if error occurs
     */
    private void WSConnect(Context context, String faureciaWSid, boolean bRetryLogin) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("in Connect");
        }

        if (session == null) {
            String sNemoUser = fpdm.nemo.NEMOUtils_mxJPO.PROPERTY_NEMO_LOGIN;
            String sNemoPwd = fpdm.nemo.NEMOUtils_mxJPO.PROPERTY_NEMO_PASSWORD;
            String sNemoDomain = fpdm.nemo.NEMOUtils_mxJPO.PROPERTY_NEMO_DOMAIN;
            try {
                URL efURL = findEndPoint(NEMO_SDF_URL);

                EnginFrameWSServiceLocator efWSServLocator = new EnginFrameWSServiceLocator();
                enginFrameWS = (EnginFrameWSSoapBindingStub) efWSServLocator.getEnginFrameWS(efURL);

                Authority authority = enginFrameWS.getAuthority(NEMO_SDF_URL, null);
                Credentials[] credentials = authority.getCredentials();
                for (Credentials cred : credentials) {
                    if ("_username".equals(cred.getName())) {
                        cred.setValue(sNemoUser);
                    }
                    if ("_password".equals(cred.getName())) {
                        cred.setValue(sNemoPwd);
                    }
                    if ("_domain".equals(cred.getName())) {
                        cred.setValue(sNemoDomain);
                    }
                }

                session = enginFrameWS.initSession(NEMO_SDF_URL, faureciaWSid, credentials);
                if (logger.isDebugEnabled()) {
                    logger.debug("token used");
                }
                enginFrameWS.authenticate(session.getId(), NEMO_SDF_URL, faureciaWSid, credentials);
            } catch (Exception e) {
                if (bRetryLogin) {
                    String sLogBefore = sNemoUser;
                    String sPassBefore = sNemoPwd;
                    String sDomainBefore = sNemoDomain;

                    String sNemoUserNew = fpdm.nemo.NEMOUtils_mxJPO.PROPERTY_NEMO_LOGIN;
                    String sNemoPwdNew = fpdm.nemo.NEMOUtils_mxJPO.PROPERTY_NEMO_PASSWORD;
                    String sNemoDomainNew = fpdm.nemo.NEMOUtils_mxJPO.PROPERTY_NEMO_DOMAIN;

                    if ((sNemoUserNew != null && !sNemoUserNew.equals(sLogBefore)) || (sNemoPwdNew != null && !sNemoPwdNew.equals(sPassBefore))
                            || (sNemoDomainNew != null && !sNemoDomainNew.equals(sDomainBefore))) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Retry to log on nemo with new User/password");
                        }
                        WSConnect(context, faureciaWSid, false);
                    } else {
                        bRetryLogin = false;
                    }
                } else {
                    logger.error("Error in Connect", e);
                    throw new Exception("NEMO Web service Authentication failed : Problem of credential with login : " + sNemoUser);
                }
            }
        } else {
            Exception e = new Exception("Already connected");
            logger.error("Error in Connect", e);
            throw e;
        }
    }

    /**
     * Disconnect from the web service
     * @throws Exception
     */
    private void WSDisconnect() throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("in Disconnect");
        }
        if (session != null) {
            try {
                enginFrameWS.logout(session.getId());
                session = null;
                if (logger.isDebugEnabled()) {
                    logger.debug("token released");
                }
            } catch (Exception e) {
                logger.error("Error in Disconnect", e);
                throw e;
            }
        } else {
            Exception e = new Exception("Already disconnected");
            logger.error("Error in Disconnect", e);
            throw e;
        }
    }

    /**
     * Perform a "send job" request
     * @param hmParam
     *            all parameters
     * @param sWSid
     * @return the response
     * @throws Exception
     *             if error occurs
     */
    private String sendJob(HashMap<String, String> hmParam, String sWSid) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("in sendJob");
        }
        String sFlowId = null;

        if ((enginFrameWS != null) && (session != null)) {
            StringHolder so = new StringHolder();
            try {

                OptionValue[] optVal = new OptionValue[hmParam.size()];
                int i = 0;
                if (logger.isDebugEnabled()) {
                    logger.debug("hmParam : " + hmParam);
                    logger.debug("sWSid : " + sWSid);
                    logger.debug("so : " + so);
                    logger.debug("NEMO_SDF_URL : " + NEMO_SDF_URL);
                    logger.debug("session.getId() : " + session.getId());
                }
                for (Entry<String, String> eEntry : hmParam.entrySet()) {
                    String sKey = eEntry.getKey();
                    String sValue = eEntry.getValue();
                    optVal[i] = new OptionValue(true, sKey, new String[] { sValue });
                    i++;
                }

                Flow flow = enginFrameWS.runService(session.getId(), NEMO_SDF_URL, sWSid, optVal, so);

                analyzeNemoOutput(so);
                if (flow != null) {
                    sFlowId = flow.getId();
                }
            } catch (AxisFault af) {
                logger.error("AxisFault in sendJob", af);
                QName qn = af.getFaultCode();
                if (qn != null) {
                    throw new Exception("NEMO : " + qn.getLocalPart());
                } else {
                    throw af;
                }
            } catch (Exception e) {

                logger.error("Error in sendJob", e);
                logger.error("Returns : \n" + extractSOAPResponse(so));
                throw e;
            }
        } else {
            Exception e = new Exception("You must be connected before sending a job (use Connect)");
            logger.error("Error in sendJob", e);
            throw e;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("end sendJob : sFlowId = <" + sFlowId + ">");
        }
        return sFlowId;
    }

    private String sendJobAction(HashMap<String, String> hmParam, String sWSid, String sActionId) throws Exception {
        return sendJobAction(null, hmParam, sWSid, sActionId, new StringHolder());
    }

    private String sendJobAction(HashMap<String, String> hmParam, String sWSid, String sActionId, StringHolder so) throws Exception {
        return sendJobAction(null, hmParam, sWSid, sActionId, so);
    }

    private String sendJobAction(Context context, HashMap<String, String> hmParam, String sWSid, String sActionId) throws Exception {
        return sendJobAction(context, hmParam, sWSid, sActionId, new StringHolder());
    }

    private String sendJobAction(Context context, HashMap<String, String> hmParam, String sWSid, String sActionId, StringHolder so) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("in sendJobAction");
        }
        String sFlowId = null;

        if ((enginFrameWS != null) && (session != null)) {
            try {
                OptionValue[] optVal = new OptionValue[hmParam.size()];
                int i = 0;
                for (Entry<String, String> eEntry : hmParam.entrySet()) {
                    String sKey = eEntry.getKey();
                    String sValue = eEntry.getValue();
                    optVal[i] = new OptionValue(true, sKey, new String[] { sValue });
                    i++;
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("hmParam : " + hmParam);
                    logger.debug("sWSid : " + sWSid);
                    logger.debug("so : " + so);
                    logger.debug("NEMO_SDF_URL : " + NEMO_SDF_URL);
                    logger.debug("session.getId() : " + session.getId());
                }

                // Check existence of CAD Objects before launching the request to prevent useless request.
                String sCADObjectId = hmParam.get(fpdm.nemo.NEMOUtils_mxJPO.NEMOConstants.OPT_CAD_OBJECT_ID);
                // Can be null for several method.
                boolean exists = (sCADObjectId != null) ? (fpdm.nemo.NEMOUtils_mxJPO.checkExistence(context, sCADObjectId)) : (true);

                if (!exists) {
                    logger.error("sendJobAction - Trying to send a job action for a deleted CAD Object. sCADObjectId <" + sCADObjectId + ">");
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("sendJobAction - Trying to send a job action sWSid <" + sWSid + "> hmParam <" + hmParam + ">");
                    }
                    Flow flow = enginFrameWS.runServiceAction(session.getId(), NEMO_SDF_URL, sWSid, sActionId, optVal, so);
                    if (flow != null) {
                        sFlowId = flow.getId();
                    }
                    analyzeNemoOutput(so);
                }
            } catch (AxisFault af) {
                logger.error("AxisFault in sendJob", af);
                QName qn = af.getFaultCode();
                if (qn != null) {
                    throw new Exception("NEMO : " + qn.getLocalPart());
                } else {
                    throw af;
                }
            } catch (Exception e) {
                logger.error("Error in sendJobAction", e);
                logger.error("Returns : \n" + extractSOAPResponse(so));
                throw e;
            }
        } else {
            Exception e = new Exception("You must be connected before sending a job (use Connect)");
            logger.error("Error in sendJobAction", e);
            throw e;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("end sendJobAction : sFlowId = <" + sFlowId + ">");
        }
        return sFlowId;
    }

    private URL findEndPoint(String sdfURL) throws Exception {
        URL efURLElement = null;
        URL url;
        try {
            url = new URL(sdfURL + "?efws");
            SAXBuilder sxb = new SAXBuilder();
            Document document;
            document = sxb.build(url);
            Element racine = document.getRootElement();
            Namespace ns = racine.getNamespace();

            String sEfURLElement = racine.getChildText("end-point", ns);
            efURLElement = new URL(sEfURLElement);
        } catch (Exception e) {
            logger.error("Error in findEndPoint", e);
            throw e;
        }
        return efURLElement;
    }

    /**
     * Return the site of the current user
     * @param context
     *            the eMatrix <code>Context</code> object
     * @return : site name
     * @throws Exception
     */
    private HashMap<String, String> getSiteMap() throws Exception {
        HashMap<String, String> hmSiteMap = null;
        try {
            HashMap<String, String> hmParam = new HashMap<String, String>();

            hmParam.clear();
            hmParam.put("Submit", "submit");
            StringHolder so = new StringHolder();
            try {
                sendJobAction(hmParam, NEMO_WS_ID_LIST_SITES_PDM, "submit", so);
            } catch (Exception e) {
                logger.error("Nemo fail to return the site mapping => processes will used default site");
                return new HashMap<String, String>();
            }
            hmSiteMap = extractSOAPResponse(so);
            if (logger.isDebugEnabled()) {
                logger.debug("[getSiteMap] hmSiteMap = <" + hmSiteMap + ">");
            }

        } catch (Exception e) {
            logger.error("Error in getSiteMap", e);
            throw e;
        }
        return hmSiteMap;
    }

    /**
     * Return the site of the current user
     * @param context
     *            the eMatrix <code>Context</code> object
     * @return : site name
     * @throws Exception
     */
    private String getUserSite(Context context, String sObjectId, String sNemoAction) throws Exception {
        String sUserSiteResult = null;
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("getUserSite() - sObjectId = <" + sObjectId + ">");
                logger.debug("getUserSite() - sNemoAction = <" + sNemoAction + ">");
            }
            DomainObject dobCADObject = DomainObject.newInstance(context, sObjectId);
            String sSelectRequester = "to[" + fpdm.nemo.NEMOUtils_mxJPO.RELATIONSHIP_NEMO_CONVERSION_QUEUE + "|attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_NEMO_ACTION + "]=='" + sNemoAction
                    + "'].attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_CONVERSION_REQUESTER + "]";
            if (logger.isDebugEnabled()) {
                logger.debug("getUserSite() - sSelectRequester = <" + sSelectRequester + ">");
            }
            String sConversionRequester = fpdm.nemo.NEMOUtils_mxJPO.getSingleValue(fpdm.nemo.NEMOUtils_mxJPO.getSelectBusinessObjectData(context, sObjectId, sSelectRequester));
            if (logger.isDebugEnabled()) {
                logger.debug("getUserSite() - sConversionRequester = <" + sConversionRequester + ">");
            }
            if (sConversionRequester != null && sConversionRequester.length() > 0) {
                // get conversion user name from requester site value
                String sConversionUserName = getConversionUserName(context, sConversionRequester);
                if (logger.isDebugEnabled()) {
                    logger.debug("getUserSite() - sConversionUserName = <" + sConversionUserName + "> sConversionRequester = <" + sConversionRequester + "> ");
                }

                if (!"".equals(sConversionUserName)) {
                    sUserSiteResult = getMappedUserSite(context, sConversionUserName);
                }
            }
            if (sUserSiteResult == null || sUserSiteResult.length() == 0) {
                String sObjectType = (String) dobCADObject.getInfo(context, DomainConstants.SELECT_TYPE);
                String sObjectName = (String) dobCADObject.getInfo(context, DomainConstants.SELECT_NAME);
                String sObjectRev = (String) dobCADObject.getInfo(context, DomainConstants.SELECT_REVISION);
                String relId = (String) dobCADObject.getInfo(context, DomainRelationship.SELECT_ID);
                sUserSiteResult = DEFAULT_USER_SITE;
                logger.error("Object (" + sObjectId + ") \"" + sObjectType + "\" \"" + sObjectName + "\" \"" + sObjectRev + "\" has no Conversion Requester relationship attribute value (relId : "
                        + relId + ")");
                logger.error("using default site : " + DEFAULT_USER_SITE);
            }
        } catch (SiteNotMappedException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error in getUserSite", e);
            throw e;
        }
        return sUserSiteResult;
    }

    /**
     * Returns the mapped NEMO site for the given user
     * @param context
     *            ematrix context
     * @param sConversionRequester
     *            the user
     * @param hmSiteMap
     *            Map between Nemo site and PDM Site
     * @return the NEMO site to used for the given user
     * @throws FrameworkException
     *             if we are unable to find the PDM Site
     * @throws SiteNotMappedException
     *             if the PDM Site is not mapped.
     */
    private String getMappedUserSite(Context context, String sConversionRequester) throws ConversionUserNotExistException, SiteNotMappedException {
        String sUserSiteResult = null;
        if (hmSiteByPerson.containsKey(sConversionRequester)) {
            sPDMUserSite = hmSiteByPerson.get(sConversionRequester);
        } else {
            try {
                if (!checkUserExist(context, sConversionRequester)) {
                    throw new ConversionUserNotExistException("User <" + sConversionRequester + "> doesn't exist.");
                }
                sPDMUserSite = MqlUtil.mqlCommand(context, "print Person \"" + sConversionRequester + "\" select site dump");
            } catch (FrameworkException e) {
                throw new ConversionUserNotExistException("User <" + sConversionRequester + "> doesn't exist.");
            }
            hmSiteByPerson.put(sConversionRequester, sPDMUserSite);
        }
        if (!hmSiteMap.containsKey(sPDMUserSite)) {
            throw new SiteNotMappedException("Site <" + sPDMUserSite + "> for user <" + sConversionRequester + "> not mapped");
        } else {
            sUserSiteResult = hmSiteMap.get(sPDMUserSite);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("getMappedUserSite() - sUserSiteResult = <" + sUserSiteResult + ">");
        }

        return sUserSiteResult;
    }

    /**
     * Transform a String of values separated by a pipe to an ArrayList
     * @param sMap
     *            sMap is a String matching this pattern "value1|value2|..."
     * @return Return a new ArrayList with value(n) from the pattern
     * @throws Exception
     */
    private ArrayList<String> getList(String sMap) throws Exception {
        ArrayList<String> alOut = null;
        try {
            String[] saMap = sMap.split("[|]", -1);
            alOut = new ArrayList<String>(Arrays.asList(saMap));
        } catch (Exception e) {
            logger.error("Error in getList", e);
            throw e;
        }
        return alOut;
    }

    /**
     * Send a checkin request to NEMO web service
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sCADObjectId
     *            CAD Object id
     * @param sNemoObjectId
     *            NEMO Object id
     * @param sViewableFormat
     *            Viewable format (2D or 3D)
     * @throws Exception
     */
    private void sendCheckinRequest(Context context, String sCADObjectId, String sNemoObjectId, String sViewableFormat, String sNemoAction) throws Exception {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("In sendCheckinRequest");
            }
            DomainObject dobCADObject = DomainObject.newInstance(context, sCADObjectId);
            StringList slSelect = new StringList();
            slSelect.addElement(DomainConstants.SELECT_CURRENT);
            slSelect.addElement(DomainConstants.SELECT_TYPE);
            slSelect.addElement(DomainConstants.SELECT_REVISION);
            slSelect.addElement("format[prt].file.name");
            slSelect.addElement("format[drw].file.name");
            slSelect.addElement("format[asm].file.name");
            slSelect.addElement("from[" + fpdm.nemo.NEMOUtils_mxJPO.RELATIONSHIP_ACTIVE_VERSION + "].to.format[asm].file.name");
            slSelect.addElement("from[" + fpdm.nemo.NEMOUtils_mxJPO.RELATIONSHIP_ACTIVE_VERSION + "].to.format[prt].file.name");
            slSelect.addElement("from[" + fpdm.nemo.NEMOUtils_mxJPO.RELATIONSHIP_ACTIVE_VERSION + "].to.format[drw].file.name");
            slSelect.addElement("to[" + fpdm.nemo.NEMOUtils_mxJPO.RELATIONSHIP_NEMO_CONVERSION_QUEUE + "|attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_NEMO_ACTION + "]==\"" + sNemoAction
                    + "\"].attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_CONVERSION_REQUESTER + "]");
            slSelect.addElement("to[" + fpdm.nemo.NEMOUtils_mxJPO.RELATIONSHIP_CAD_SUBCOMPONENT + "].from.attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_FPDM_MAJOR_REV + "]");
            slSelect.addElement("to[" + fpdm.nemo.NEMOUtils_mxJPO.RELATIONSHIP_CAD_SUBCOMPONENT + "].from.revision");
            Map<?, ?> mInfos = dobCADObject.getInfo(context, slSelect);
            if (logger.isDebugEnabled()) {
                logger.debug("mInfos : " + mInfos);
            }
            String sCurrent = (String) mInfos.get(DomainConstants.SELECT_CURRENT);
            String sObjectType = (String) mInfos.get(DomainConstants.SELECT_TYPE);
            String sConversionRequester = (String) mInfos
                    .get("to[" + fpdm.nemo.NEMOUtils_mxJPO.RELATIONSHIP_NEMO_CONVERSION_QUEUE + "].attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_CONVERSION_REQUESTER + "]");
            // get conversion user name from requester site value
            String sConversionUserName = getConversionUserName(context, sConversionRequester);
            if (logger.isDebugEnabled()) {
                logger.debug("sendCheckinRequest() -  sConversionUserName = <" + sConversionUserName + "> sConversionRequester = <" + sConversionRequester + "> ");
            }
            String sFileName = null;

            // use CAD Object revision (RFC18084)
            String sCADOobjectRev = (String) mInfos.get(DomainConstants.SELECT_REVISION);
            if (logger.isDebugEnabled()) {
                logger.debug("sendCheckinRequest() - sCADOobjectRev = <" + sCADOobjectRev + ">");
            }

            // Parameter OPT_PARAM4 will be used to rename converted file by adding CAD Def major rev and minor revision
            String sFileNameSuffix = sCADOobjectRev;

            if (fpdm.nemo.NEMOUtils_mxJPO.TYPE_FPDM_CAD_COMPONENT.equals(sObjectType)) {
                sFileNameSuffix = "";
            }
            String sFileExtension = "";
            String sNbFolio = "";
            String sFileFormat = "";
            if ("2D".equals(sViewableFormat)) {
                sFileFormat = "ZIP";
                sFileExtension = "zip";
                sNbFolio = dobCADObject.getInfo(context, "attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_FOLIO_NUMBER + "]");
            } else if ("3D".equals(sViewableFormat)) {
                sFileFormat = "JT";
                sFileExtension = "jt";
                sNbFolio = "";
            } else if (fpdm.nemo.NEMOUtils_mxJPO.RANGE_NEMO_ACTION_QC.equals(sViewableFormat)) {
                sFileFormat = "ZIP";
                sFileExtension = "zip";
                sFileNameSuffix = "";
            } else if ("other".equals(sViewableFormat)) {
                StringBuilder sbFileNameSuffix = new StringBuilder();
                sbFileNameSuffix.append("_").append(sCADOobjectRev);
                if (fpdm.nemo.NEMOUtils_mxJPO.TYPE_CATDRAWING.equals(sObjectType) || fpdm.nemo.NEMOUtils_mxJPO.TYPE_UG_DRAWING.equals(sObjectType)
                        || fpdm.nemo.NEMOUtils_mxJPO.TYPE_FPDM_CAD_DRAWING.equals(sObjectType)) {
                    sbFileNameSuffix.append("_2D");
                } else {
                    sbFileNameSuffix.append("_3D");
                }
                sFileNameSuffix = sbFileNameSuffix.toString();
                sFileFormat = "other";
                sFileExtension = "zip";
            } else {
                Exception e = new Exception("Error in sendCheckinRequest: Wrong viewable format");
                logger.error("Error in sendCheckinRequest", e);
                throw e;
            }

            String sCADObjectFileNameWithoutExtension = getFileNameWithoutExtension(fpdm.nemo.NEMOUtils_mxJPO.getCADObjectFileName(context, sCADObjectId, alSupportedFileFormat));
            sCADObjectFileNameWithoutExtension = sCADObjectFileNameWithoutExtension.replaceAll(FORBIDDEN_CHAR, "_");

            String sCheckinFileName = sCADObjectFileNameWithoutExtension + "." + sFileExtension;

            if (logger.isDebugEnabled()) {
                logger.debug("sendCheckinRequest() - sCurrent = <" + sCurrent + ">");
            }

            String sNemoObjectSpooler = extractNemoSpooler(sNemoObjectId);

            HashMap<String, String> hmParam = new HashMap<String, String>();

            hmParam.put(fpdm.nemo.NEMOUtils_mxJPO.NEMOConstants.OPT_PDM_URL, PDM_HOST);
            hmParam.put(fpdm.nemo.NEMOUtils_mxJPO.NEMOConstants.OPT_FILE_FORMAT, sFileFormat);
            hmParam.put(fpdm.nemo.NEMOUtils_mxJPO.NEMOConstants.OPT_FILE_NAME, sCheckinFileName);
            hmParam.put(fpdm.nemo.NEMOUtils_mxJPO.NEMOConstants.OPT_CAD_OBJECT_ID, sCADObjectId);
            hmParam.put(fpdm.nemo.NEMOUtils_mxJPO.NEMOConstants.OPT_CHECKOUT_TICKET, "");
            hmParam.put(fpdm.nemo.NEMOUtils_mxJPO.NEMOConstants.OPT_CHECKOUT_ACTION, "");
            hmParam.put(fpdm.nemo.NEMOUtils_mxJPO.NEMOConstants.OPT_CHECKIN_TICKET, "");
            hmParam.put(fpdm.nemo.NEMOUtils_mxJPO.NEMOConstants.OPT_CHECKIN_ACTION, "");
            // NEMO WS is case sensitive. It does not allow to use fpdm.nemo.NEMOUtils_mxJPO.NEMOConstants.OPT_SPOOLER
            hmParam.put("OPT_spooler", sNemoObjectSpooler);
            hmParam.put(fpdm.nemo.NEMOUtils_mxJPO.NEMOConstants.OPT_NEMO_ID, sNemoObjectId);
            //
            hmParam.put(fpdm.nemo.NEMOUtils_mxJPO.NEMOConstants.OPT_PARAM1, sConversionUserName);
            hmParam.put(fpdm.nemo.NEMOUtils_mxJPO.NEMOConstants.OPT_PARAM2, sNbFolio);
            // Parameter OPT_PARAM3 will contains requester passwd (for OpenPDM)
            if ("OpenPDM".equals(sConversionUserName)) {
                hmParam.put(fpdm.nemo.NEMOUtils_mxJPO.NEMOConstants.OPT_PARAM3, CONVERSION_REQUESTER_PASSWORD);
            } else {
                hmParam.put(fpdm.nemo.NEMOUtils_mxJPO.NEMOConstants.OPT_PARAM3, "");
            }

            if (logger.isDebugEnabled()) {
                logger.debug("sendCheckinRequest() - sFileNameSuffix = <" + sFileNameSuffix + ">");
            }
            hmParam.put(fpdm.nemo.NEMOUtils_mxJPO.NEMOConstants.OPT_PARAM4, sFileNameSuffix);

            if (fpdm.nemo.NEMOUtils_mxJPO.RANGE_NEMO_ACTION_QC.equals(sViewableFormat)) {
                hmParam.put(fpdm.nemo.NEMOUtils_mxJPO.NEMOConstants.OPT_PARAM5, fpdm.nemo.NEMOUtils_mxJPO.RANGE_NEMO_ACTION_QC);
                if (sObjectType != null && sObjectType.equals(fpdm.nemo.NEMOUtils_mxJPO.TYPE_CATDRAWING)) {
                    sFileName = (String) mInfos.get("format[drw].file.name");
                    if (sFileName == null || sFileName.equals("")) {
                        sFileName = (String) mInfos.get("from[" + fpdm.nemo.NEMOUtils_mxJPO.RELATIONSHIP_ACTIVE_VERSION + "].to.format[drw].file.name");
                    }
                } else if (sObjectType != null && sObjectType.equals(fpdm.nemo.NEMOUtils_mxJPO.TYPE_CATPART)) {
                    sFileName = (String) mInfos.get("format[prt].file.name");
                    if (sFileName == null || sFileName.equals("")) {
                        sFileName = (String) mInfos.get("from[" + fpdm.nemo.NEMOUtils_mxJPO.RELATIONSHIP_ACTIVE_VERSION + "].to.format[prt].file.name");
                    }
                } else {
                    sFileName = (String) mInfos.get("format[asm].file.name");
                    if (sFileName == null || sFileName.equals("")) {
                        sFileName = (String) mInfos.get("from[" + fpdm.nemo.NEMOUtils_mxJPO.RELATIONSHIP_ACTIVE_VERSION + "].to.format[asm].file.name");
                    }
                }
                hmParam.put(fpdm.nemo.NEMOUtils_mxJPO.NEMOConstants.OPT_PARAM6, sFileName);
            } else if ("other".equals(sViewableFormat)) {
                hmParam.put(fpdm.nemo.NEMOUtils_mxJPO.NEMOConstants.OPT_PARAM5, sNemoAction);
            }

            if (logger.isDebugEnabled()) {
                // logHashMap(hmParam);
            }

            if (logger.isDebugEnabled()) {
                logger.debug("sendCheckinRequest() - Sending checkin request for <" + sCheckinFileName + ">");
            }
            String sFlowId = sendJobAction(context, hmParam, NEMO_WS_ID_PDM_CHECK_IN, "checkin");
            if (logger.isDebugEnabled()) {
                logger.debug("sendCheckinRequest() - sFlowId = " + sFlowId);
            }

        } catch (Exception e) {
            logger.error("Error in sendCheckinRequest", e);
            throw e;
        }
    }

    /**
     * Return the Profiles with Environments list existing on NEMO
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            no arguments
     * @return
     * @throws Exception
     */
    public HashMap<String, ArrayList<String>> getQualityCheckerProfiles(Context context, String[] args) throws Exception {
        boolean bNEMOConnected = false;
        Date dCurrent = new Date();
        long lCurrentTime = dCurrent.getTime();
        long lDifTime = lCurrentTime - lLastProfilesRetrieval;
        // retrieve the values from QChecker each 15 minutes
        if (hmQCProfiles == null || lDifTime > 900000) {
            try {
                // init properties
                init(context);
                // Connect to NEMO Web-service
                if (args == null || args.length < 1 || args[0] == null || !args[0].equals("CONNECTED")) {
                    WSConnect(context, NEMO_WS_ID_LOGIN, true);
                    bNEMOConnected = true;
                }
                try {
                    HashMap<String, ArrayList<String>> hmQc = getProfileMap();
                    lLastProfilesRetrieval = lCurrentTime;
                    hmQCProfiles = hmQc;
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    logger.error("Unable to retrieve QC profiles from nemo", e);
                    if (hmQCProfiles == null) {
                        throw new Exception("Unable to retrieve QC profiles from nemo");
                    } else {
                        logger.error("Use existing profiles");
                    }
                }
            } finally {
                if (bNEMOConnected) {
                    WSDisconnect();
                    bNEMOConnected = false;
                }
            }
        }
        return hmQCProfiles;
    }

    /**
     * Return the Profiles with Environments list existing on NEMO
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            no arguments
     * @return
     * @throws Exception
     */
    public String retrieveStringOptions(Context context, String[] args) throws Exception {
        boolean bNEMOConnected = false;
        if (args != null) {
            try {
                // init properties
                init(context);
                if (logger.isDebugEnabled()) {
                    logger.debug("Not connected to Nemo, process will connect to NEMO.");
                }
                WSConnect(context, NEMO_WS_ID_LOGIN, true);
                bNEMOConnected = true;
                if (hmSiteMap == null) {
                    hmSiteMap = getSiteMap();
                }
                String sSite;
                try {
                    sSite = getMappedUserSite(context, context.getUser());
                } catch (SiteNotMappedException snme) {
                    logger.warn("[perform] - Could not retrieve mapped, use the default one");
                    sSite = DEFAULT_USER_SITE;
                }
                try {
                    performChangeSite(sSite);
                } catch (Exception e) {
                    if (DEFAULT_USER_SITE != null && !DEFAULT_USER_SITE.equals(sSite)) {
                        logger.error("Change site failled for site of user, try to change to default site");
                        try {
                            performChangeSite(DEFAULT_USER_SITE);
                        } catch (Exception ne) {
                            logger.error("Impossible to change site", ne);
                            throw new Exception("Failled on NEMO Change site with error " + e.getMessage());
                        }
                    } else {
                        logger.error("Impossible to change site", e);
                        throw new Exception("Failled on NEMO Change site with error " + e.getMessage());
                    }
                }

                try {
                    return getStringOption(context, args);
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    logger.error("ERROR", e);
                    throw new Exception("An error happened while trying to get options from Nemo : " + e.getMessage());
                }
            } finally {
                if (bNEMOConnected) {
                    WSDisconnect();
                    bNEMOConnected = false;
                }
            }
        }
        return null;
    }

    /**
     * Return the Profiles with Environments list existing on NEMO<br>
     * This function will call a NEMO service and extract result from the the SOAP response
     * @return
     * @throws Exception
     */
    private HashMap<String, ArrayList<String>> getProfileMap() throws Exception {
        HashMap<String, ArrayList<String>> hmProfileMap = new HashMap<String, ArrayList<String>>();
        try {
            HashMap<String, String> hmParam = new HashMap<String, String>();
            hmParam.put("Submit", "submit");
            StringHolder so = new StringHolder();
            try {
                sendJobAction(hmParam, NEMO_WS_ID_LIST_QCHECKER_PROFILES_PDM, "submit", so);
            } catch (Exception e) {
                logger.error("Error in getProfileMap()\n", e);
                throw e;
            }

            SAXBuilder sxb = new SAXBuilder();
            Document document = sxb.build(new StringReader(so.value));
            Element el = document.getRootElement().getChild("output", nsef);
            String[] sa = el.getTextTrim().split("\\s+", -1);
            if (logger.isDebugEnabled()) {
                logger.debug("getProfileMap() - sa=<" + Arrays.toString(sa) + ">");
            }

            for (String s : sa) {
                String[] saElement = s.split(";", -1);
                if (saElement[0].trim().length() > 0) {
                    if (hmProfileMap.containsKey(saElement[0])) {
                        ArrayList<String> alValue = hmProfileMap.get(saElement[0]);
                        alValue.add(saElement[1]);
                    } else {
                        ArrayList<String> alValue = new ArrayList<String>();
                        alValue.add(saElement[1]);
                        hmProfileMap.put(saElement[0], alValue);
                    }
                }
            }

            if (logger.isDebugEnabled()) {
                logger.debug("getProfileMap() - hmProfileMap = <" + hmProfileMap + ">");
            }

        } catch (Exception e) {
            logger.error("Error in getProfileMap()\n", e);
            throw e;
        }
        return hmProfileMap;
    }

    /**
     * Return the Profiles with Environments list existing on NEMO<br>
     * This function will call a NEMO service and extract result from the the SOAP response
     * @return
     * @throws Exception
     */
    private String getStringOption(Context context, String[] saTreatmentIds) throws Exception {
        int i = 0;
        Document dToReturn = null;
        Element elRoot = null;
        for (String sTreatmentId : saTreatmentIds) {
            try {
                HashMap<String, String> hmParam = new HashMap<String, String>();
                hmParam.put(fpdm.nemo.NEMOUtils_mxJPO.NEMOConstants.OPT_TREATMENT, sTreatmentId);
                StringHolder so = new StringHolder();
                try {
                    getServiceOptions(context, hmParam, NEMO_WS_ID_PDM_START_TREATMENT, NEMO_WS_ACTION_SHOW_FORM, so);
                } catch (Exception e) {
                    logger.error("Error in getOptions()\n", e);
                    throw e;
                }
                SAXBuilder sxb = new SAXBuilder();
                Document document = sxb.build(new StringReader(so.value));
                if (i == 0) {
                    i++;
                    dToReturn = document;
                    elRoot = dToReturn.getRootElement();
                    checkNemoReturn(elRoot.getChild("output", nsef));
                } else {
                    Element elOutput = document.getRootElement().getChild("output", nsef);
                    if (elOutput != null) {
                        elOutput.detach();
                        checkNemoReturn(elRoot.getChild("output", nsef));
                        elRoot.addContent(elOutput);
                    }
                }
            } catch (Exception e) {
                logger.error("Error in getOptions()\n", e);
                throw e;
            }
        }
        XMLOutputter xmlOutputter = new XMLOutputter();
        String sToReturn = xmlOutputter.outputString(dToReturn);
        if (logger.isTraceEnabled()) {
            logger.trace("sToReturn : " + sToReturn);
        }
        return sToReturn;
    }

    private static void checkNemoReturn(Element elOutput) throws Exception {
        if (elOutput == null) {
            throw new Exception("No output has been found in Nemo return");
        }
        Element elError = elOutput.getChild(fpdm.nemo.NEMOUtils_mxJPO.XML_ERROR, nsef);
        if (elError != null) {
            String sErrorTitle = elError.getChildText(fpdm.nemo.NEMOUtils_mxJPO.XML_TITLE, nsef);
            String sErrorMessage = elError.getChildText(fpdm.nemo.NEMOUtils_mxJPO.XML_MESSAGE, nsef);
            String sError = "Error happened on Nemo side : " + sErrorTitle + " with message " + sErrorMessage;
            logger.error(sError);
            throw new SendJobException(sError);
        }
        Element elResult = elOutput.getChild(fpdm.nemo.NEMOUtils_mxJPO.XML_RESULT, nsFaurecia);
        String sExitCode = getFaureciaExitCode(elResult);
        if (!"0".equals(sExitCode)) {
            if (elResult != null) {
                String sNemoMessage = elResult.getChildText(fpdm.nemo.NEMOUtils_mxJPO.XML_MESSAGE, nsFaurecia);
                if (sNemoMessage != null && sNemoMessage.length() > 0) {
                    sExitCode += " " + sNemoMessage;
                }
            }
            logger.error("Exit code is not equal to 0 : " + sExitCode);
            throw new SendJobException("Nemo has returned an unexpected response code : " + sExitCode);
        }
    }

    private static String getFaureciaExitCode(Element elResult) {
        if (elResult != null) {
            String sExitCode = elResult.getAttributeValue("exit-code");
            if (sExitCode != null) {
                return sExitCode;
            }
            return "No Exit Code";
        }
        // no faurecia result
        return "0";
    }

    private void performChangeSite(String sSite) throws Exception {
        HashMap<String, String> hmParam = new HashMap<String, String>();
        hmParam.put("OPT_site", sSite.toLowerCase());
        sendJob(hmParam, NEMO_WS_ID_CHANGE_SITE);
    }

    private String getServiceOptions(Context context, HashMap<String, String> hmParam, String sWSid, String sActionId, StringHolder so) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("in getService");
        }
        String sFlowId = null;

        if ((enginFrameWS != null) && (session != null)) {
            try {
                OptionValue[] optVal = new OptionValue[hmParam.size()];
                int i = 0;
                for (Entry<String, String> eEntry : hmParam.entrySet()) {
                    String sKey = eEntry.getKey();
                    String sValue = eEntry.getValue();
                    optVal[i] = new OptionValue(true, sKey, new String[] { sValue });
                    i++;
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("hmParam : " + hmParam);
                    logger.debug("sWSid : " + sWSid);
                    logger.debug("sActionId : " + sActionId);
                    logger.debug("so : " + so);
                    logger.debug("NEMO_SDF_URL : " + NEMO_SDF_URL);
                    logger.debug("session.getId() : " + session.getId());
                    logger.debug("getService - Trying to getService sWSid <" + sWSid + "> hmParam <" + hmParam + ">");
                }

                Flow flow = enginFrameWS.runServiceAction(session.getId(), NEMO_SDF_URL, sWSid, sActionId, optVal, so);
                if (flow != null) {
                    sFlowId = flow.getId();
                }

                analyzeNemoOutput(so);

            } catch (Exception e) {

                logger.error("Error in getServiceOptions", e);
                logger.error("Returns : \n" + extractSOAPResponse(so));
                throw e;
            }
        } else {
            Exception e = new Exception("You must be connected before sending a job (use Connect)");
            logger.error("Error in getService", e);
            throw e;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("end getService : sFlowId = <" + sFlowId + ">");
        }
        return sFlowId;
    }

    /**
     * trigger called when disconnecting a CAD Object <br>
     * of the Nemo Conversion Queue Object
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sKey
     *            contains id of the job to cancel
     * @throws Exception
     */
    public void cancelJob(Context context, String sKey) throws Exception {
        boolean bNEMOConnected = false;
        try {
            // init properties
            this.init(context);
            // Connect to NEMO Web-service
            WSConnect(context, NEMO_WS_ID_LOGIN, true);
            bNEMOConnected = true;
            String sFormattedKey = sKey;
            HashMap<String, String> hmParam = new HashMap<String, String>();
            if (sFormattedKey != null && sFormattedKey.indexOf("spooler://") == 0) {
                sFormattedKey = sFormattedKey.substring(10);

            }
            hmParam.put(fpdm.nemo.NEMOUtils_mxJPO.NEMOConstants.OPT_SPOOLER, sFormattedKey);
            try {
                sendJobAction(hmParam, NEMO_WS_ID_JOB_CANCEL, "submit");
                if (logger.isDebugEnabled()) { // TODO We can delete this
                    logger.debug("Cancelling job : " + sFormattedKey);
                }
            } catch (Exception e) {
                logger.error("Error happend while trying to cancel job", e);
                e.printStackTrace();
            }
        } finally {
            if (bNEMOConnected) {
                WSDisconnect();
                bNEMOConnected = false;
            }
        }
    }

    /**
     * Populate the given map with parameters for conversion or Quality check
     * @param hmParam
     *            the map to fill
     * @param sObjectType
     *            the CAD Object type
     * @param sCADObjectFileName
     *            the CAD Object file name
     * @param sCADObjectFileFormat
     *            the CAD Object file format
     * @param sNemoAction
     *            the code of the action : "QC" for Quality Check, empty for a conversion
     * @throws Exception
     *             if error occurs
     * @return <code>true</code> if a configuration has been found, <code>false</code> otherwise
     */
    private boolean populateParameters(Map<String, String> hmParam, String sObjectType, String sCADObjectFileName, String sCADObjectFileFormat, String sNemoAction) throws Exception {
        boolean bHasProcess = false;
        String sTempFileName = sCADObjectFileName;

        String sMappedNemoConfiguration = null;
        if (fpdm.nemo.NEMOUtils_mxJPO.RANGE_NEMO_ACTION_QC.equals(sNemoAction)) {
            sMappedNemoConfiguration = "CATIA5_QCHECKER";
            if (fpdm.nemo.NEMOUtils_mxJPO.TYPE_CATPRODUCT.equals(sObjectType) || fpdm.nemo.NEMOUtils_mxJPO.TYPE_CATDRAWING.equals(sObjectType)) {
                sTempFileName = getFileNameWithoutExtension(sCADObjectFileName) + ".zip";
            }
        } else if (fpdm.nemo.NEMOUtils_mxJPO.TYPE_CATPART.equals(sObjectType) || fpdm.nemo.NEMOUtils_mxJPO.TYPE_CATIA_CGR.equals(sObjectType)) {
            sMappedNemoConfiguration = "CATIA5_JT";
        } else if (fpdm.nemo.NEMOUtils_mxJPO.TYPE_CATIA_V4_MODEL.equals(sObjectType)) {
            sMappedNemoConfiguration = "CATIA4_JT";
        } else if (fpdm.nemo.NEMOUtils_mxJPO.TYPE_FPDM_CAD_COMPONENT.equals(sObjectType)) {
            if ("UG".equals(sCADObjectFileFormat)) {
                sMappedNemoConfiguration = "NX_JT";
            } else {
                sMappedNemoConfiguration = "CATIA4_JT";
            }
        } else if (fpdm.nemo.NEMOUtils_mxJPO.TYPE_CATDRAWING.equals(sObjectType)) {
            sMappedNemoConfiguration = "CATIA5_HPGLTIFF";
        } else if (fpdm.nemo.NEMOUtils_mxJPO.TYPE_UG_MODEL.equals(sObjectType)) {
            sMappedNemoConfiguration = "NX_JT";
        } else if (fpdm.nemo.NEMOUtils_mxJPO.TYPE_UG_DRAWING.equals(sObjectType)) {
            sMappedNemoConfiguration = "NX_HPGLTIFF";
            sTempFileName = getFileNameWithoutExtension(sCADObjectFileName) + ".zip";
        }

        if (sMappedNemoConfiguration == null || !mNemoConfigurationByName.containsKey(sMappedNemoConfiguration)) {
            if (logger.isDebugEnabled()) {
                logger.debug("populateParameters() - Does not contains nemo configuration for <" + sObjectType + ">. Nemo Configuration <" + sMappedNemoConfiguration + ">");
            }
            bHasProcess = false;
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("populateParameters() -  Processing Type <" + sObjectType + "> Nemo Configuration <" + sMappedNemoConfiguration + ">");
            }
            Map tmp = new HashMap(mNemoConfigurationByName.get(sMappedNemoConfiguration));
            tmp.keySet().removeAll(hmParam.keySet());
            tmp.remove(DomainConstants.SELECT_ID); // remove useless informations
            tmp.remove(DomainConstants.SELECT_NAME); //
            hmParam.putAll(tmp);
            bHasProcess = true;
        }
        hmParam.put(fpdm.nemo.NEMOUtils_mxJPO.NEMOConstants.OPT_FILE_NAME, sTempFileName);// Mandatory parameter
        if (logger.isDebugEnabled()) {
            logger.debug("populateParameters() - hmParam = <" + hmParam + ">");
        }
        return bHasProcess;
    }

    /**
     * Populate the given map with needed informations for a QC action
     * @param hmParam
     *            the map to fill
     * @param sObjectType
     *            the object type
     * @param sCADObjectFileName
     *            the object file name
     * @param sCADObjectFileFormat
     *            the object file format
     * @return true if a process is needed to be done
     * @throws Exception
     *             if error occurs
     */
    private boolean populateParametersForQChecker(Map<String, String> hmParam, String sObjectType, String sCADObjectFileName, String sCADObjectFileFormat) throws Exception {
        return populateParameters(hmParam, sObjectType, sCADObjectFileName, sCADObjectFileFormat, fpdm.nemo.NEMOUtils_mxJPO.RANGE_NEMO_ACTION_QC);
    }

    /**
     * Populate the given map with needed informations for a conversion
     * @param hmParam
     *            the map to fill
     * @param sObjectType
     *            the object type
     * @param sCADObjectFileName
     *            the object file name
     * @param sCADObjectFileFormat
     *            the object file format
     * @return true if a process is needed to be done
     * @throws Exception
     *             if error occurs
     */
    private boolean populateParametersForConversions(Map<String, String> hmParam, String sObjectType, String sCADObjectFileName, String sCADObjectFileFormat) throws Exception {
        return populateParameters(hmParam, sObjectType, sCADObjectFileName, sCADObjectFileFormat, "");
    }

    /**
     * Set the current process to WARNING level if is set to SUCCEEDED
     */
    private void processWarn() {
        if (!bJobError) {
            bJobError = true;
            sProcessStatus = fpdm.nemo.NEMOUtils_mxJPO.RANGE_COMPLETION_STATUS_WARNING;
        }
    }

    /**
     * Set the current process to FAILED level if is set to SUCCEEDED or WARNING
     */
    private void processFail() {
        bJobError = true;
        sProcessStatus = fpdm.nemo.NEMOUtils_mxJPO.RANGE_COMPLETION_STATUS_FAILED;
    }

    /**
     * Helper method for failures. Performs theses operations :
     * <ul>
     * <li>set the current process to FAILED</li>
     * <li>update the attribute Conversion Status of the given domain object to FAILED</li>
     * <li>set the given failure message into the attribute Conversion Result of the given domain relationship</li>
     * <li>create a conversion summary and put it inside global result summary</li>
     * <li>log the error with ERROR level</li>
     * </ul>
     * @param context
     *            ematrix context
     * @param doObject
     *            the domain object to update
     * @param drObject
     *            the domain relationship to update
     * @param sFailureMessage
     *            the failure message
     * @param sNemoAction
     *            the current nemo action
     * @param sConversionRequester
     *            the conversion requester
     * @throws Exception
     *             if error occurs during attribute update
     */
    private void processConversionFail(Context context, DomainObject doObject, DomainRelationship drObject, String sFailureMessage, String sNemoAction, String sConversionRequester) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("processConversionFail() - doObject <" + doObject + ">");
            logger.debug("processConversionFail() - drObject <" + drObject + ">");
            logger.debug("processConversionFail() - sFailureMessage <" + sFailureMessage + ">");
            logger.debug("processConversionFail() - sNemoAction <" + sNemoAction + ">");
            logger.debug("processConversionFail() - sConversionRequester <" + sConversionRequester + ">");
        }
        processFail();
        Map<String, String> hmObjectSummary = buildObjectConversionSummaryMap(context, doObject, fpdm.nemo.NEMOUtils_mxJPO.getProcessLabel(sNemoAction), false, sConversionRequester);
        if (fpdm.nemo.NEMOUtils_mxJPO.isOnDemandConversion(sNemoAction)) {
            drObject.setAttributeValue(context, fpdm.nemo.NEMOUtils_mxJPO.getStatusAttribute(sNemoAction), fpdm.nemo.NEMOUtils_mxJPO.RANGE_CONVERSION_STATUS_FAILED);
        } else {
            doObject.setAttributeValue(context, fpdm.nemo.NEMOUtils_mxJPO.getStatusAttribute(sNemoAction), fpdm.nemo.NEMOUtils_mxJPO.RANGE_CONVERSION_STATUS_FAILED);
        }
        if (drObject != null) {
            drObject.setAttributeValue(context, fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_CONVERSION_RESULT, sFailureMessage);
        }
        hmObjectSummary.put(fpdm.nemo.NEMOUtils_mxJPO.getIdentifierLabel(sNemoAction), "");
        hmObjectSummary.put(fpdm.nemo.NEMOUtils_mxJPO.getStatusLabel(sNemoAction), sFailureMessage);
        boolean closeNeeded = false;
        if (closeNeeded = !doObject.isOpen()) {
            doObject.open(context);
        }
        logger.error(sFailureMessage + " : (" + doObject.getObjectId() + ") \"" + doObject.getTypeName() + "\" \"" + doObject.getName() + "\" \"" + doObject.getRevision() + "\"\n");
        hmResultSummary.put(sNemoAction + doObject.getObjectId(), hmObjectSummary);
        if (closeNeeded) {
            doObject.close(context);
        }
    }

    /**
     * Set the current process statut to SUCCEEDED.
     */
    private void processSuccess() {
        sProcessStatus = fpdm.nemo.NEMOUtils_mxJPO.RANGE_COMPLETION_STATUS_SUCCEEDED;
    }

    /**
     * Send an email to support level 3 to alert them that the limit of the number of objects to convert is reached
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param mlObjInQueue
     *            objects list linked to the NEMO conversion queue
     * @throws Exception
     */
    private boolean sendNemoLimitObjectAlert(Context context, MapList mlObjInQueue) {
        HashMap<String, Object> hmNemoSum = new HashMap<String, Object>();
        HashMap<String, Object> hmQCSum = new HashMap<String, Object>();
        HashMap<String, Object> hmCancelSum = new HashMap<String, Object>();
        HashMap<String, Object> hmChoosenSum = new HashMap<String, Object>();
        Iterator<?> mlItr = mlObjInQueue.iterator();
        while (mlItr.hasNext()) {
            Map<?, ?> mTempMap = (Map<?, ?>) mlItr.next();
            String sNemoAction = (String) mTempMap.get(SELECT_ATTRIBUTE_NEMO_ACTION);
            if (fpdm.nemo.NEMOUtils_mxJPO.RANGE_NEMO_ACTION_QC.equalsIgnoreCase(sNemoAction)) {
                String sKey = "".equals((String) mTempMap.get(SELECT_ATTRIBUTE_QC_CONVERSION_STATUS)) ? "EMPTY" : (String) mTempMap.get(SELECT_ATTRIBUTE_QC_CONVERSION_STATUS);
                Integer currentValue = (Integer) hmQCSum.get(sKey);
                if (currentValue == null) {
                    hmQCSum.put(sKey, Integer.valueOf(1));
                } else {
                    int iTempValue = currentValue.intValue() + 1;
                    hmQCSum.put(sKey, Integer.valueOf(iTempValue));
                }
            } else if (sNemoAction.length() == 0) {
                String sKey = "".equals((String) mTempMap.get(SELECT_ATTRIBUTE_CONVERSION_STATUS)) ? "EMPTY" : (String) mTempMap.get(SELECT_ATTRIBUTE_CONVERSION_STATUS);
                Integer currentValue = (Integer) hmNemoSum.get(sKey);
                if (currentValue == null) {
                    hmNemoSum.put(sKey, Integer.valueOf(1));
                } else {
                    int iTempValue = currentValue.intValue() + 1;
                    hmNemoSum.put(sKey, Integer.valueOf(iTempValue));
                }
            } else if (sNemoAction.equals(fpdm.nemo.NEMOUtils_mxJPO.RANGE_NEMO_ACTION_CANCEL)) {
                String sKey = "".equals((String) mTempMap.get(SELECT_ATTRIBUTE_REL_CONVERSION_STATUS)) ? "EMPTY" : (String) mTempMap.get(SELECT_ATTRIBUTE_REL_CONVERSION_STATUS);
                if (sKey.equals("0")) {
                    sKey = "Waiting for Cancel ";
                } else {
                    sKey = "Cancel not successfull at least once ";
                }
                Integer currentValue = (Integer) hmCancelSum.get(sKey);
                if (currentValue == null) {
                    hmCancelSum.put(sKey, Integer.valueOf(1));
                } else {
                    int iTempValue = currentValue.intValue() + 1;
                    hmCancelSum.put(sKey, Integer.valueOf(iTempValue));
                }
            } else {
                String sKey = "".equals((String) mTempMap.get(SELECT_ATTRIBUTE_REL_CONVERSION_STATUS)) ? "EMPTY" : (String) mTempMap.get(SELECT_ATTRIBUTE_REL_CONVERSION_STATUS);
                Integer currentValue = (Integer) hmChoosenSum.get(sKey);
                if (currentValue == null) {
                    hmChoosenSum.put(sKey, Integer.valueOf(1));
                } else {
                    int iTempValue = currentValue.intValue() + 1;
                    hmChoosenSum.put(sKey, Integer.valueOf(iTempValue));
                }
            }
        }
        /*
         * try { String sSubject = "!ALERT NEMO CONVERSION QUEUE! " + FPDMOrganizationName_mxJPO.getEnvironment(context) + "-" + FPDMOrganizationName_mxJPO.getName(context);
         * FPDMNotification_mxJPO.sendMailToSupport3(context, fpdm.nemo.NEMOUtils_mxJPO.constructAlertMessage(hmQCSum, hmNemoSum, hmChoosenSum, hmCancelSum, ""), sSubject); } catch (Exception e) {
         * logger.error("Unable to send email"); logger.error("Error with NEMO Conversion queue"); logger.error("hmNemoSum:" + hmNemoSum); logger.error("hmChoosenSum:" + hmChoosenSum);
         * logger.error("hmQCSum:" + hmQCSum); logger.error("hmCancelSum:" + hmCancelSum); // do not block the process if this email cannot be sent logger.error(e.toString(), e); }
         */

        return true;
    }

    /**
     * Send an email to support level 3 to alert them that some objects are in queue for a long time<br>
     * Only objects with conversion status is empty or equals PDM-CHECKIN-WAIT
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param mlObjInQueue
     *            objects list linked to the NEMO conversion queue
     * @throws Exception
     */
    @SuppressWarnings("deprecation")
    private boolean sendNemoLimitTimeConversionRequestedAlert(Context context, MapList mlObjInQueue, long lCurrentTime, long lLimitTimeInConversion) {
        boolean bAlertSent = false;
        HashMap<String, Object> hmConversionsList = new HashMap<String, Object>();
        HashMap<String, Object> hmQCList = new HashMap<String, Object>();
        HashMap<String, Object> hmChoosenList = new HashMap<String, Object>();
        HashMap<String, Object> hmCancelList = new HashMap<String, Object>();

        Map<?, ?> mTempMap = null;
        String sObjectLinkedDate = null;
        String sObjectTNR = null;
        String sObjectStatus = null;
        String sNemoAction = null;
        Date date = null;
        Iterator<?> mlItr = mlObjInQueue.iterator();
        while (mlItr.hasNext()) {
            mTempMap = (Map<?, ?>) mlItr.next();

            sObjectLinkedDate = (String) mTempMap.get(DomainRelationship.SELECT_ORIGINATED);
            if (logger.isDebugEnabled()) {
                logger.debug("sendLimitTimeConversionRequestedAlert() - sObjectLinkedDate = <" + sObjectLinkedDate + ">");
            }
            date = new Date(sObjectLinkedDate);
            long lLinkedObjectTime = date.getTime(); // in mill
            if (logger.isDebugEnabled()) {
                logger.debug("sendLimitTimeConversionRequestedAlert() - lLinkedObjectTime = <" + lLinkedObjectTime + "> lCurrentTime = <" + lCurrentTime + "> in milliseconds");
            }
            long lDiff = (lCurrentTime - lLinkedObjectTime) / 3600000; // diff in hours
            if (logger.isDebugEnabled()) {
                logger.debug("sendLimitTimeConversionRequestedAlert() - lDiff = <" + lDiff + "> hours");
            }

            if (lDiff >= lLimitTimeInConversion) {
                sObjectTNR = (String) mTempMap.get(DomainConstants.SELECT_TYPE) + " " + (String) mTempMap.get(DomainConstants.SELECT_NAME) + " "
                        + (String) mTempMap.get(DomainConstants.SELECT_REVISION);
                sNemoAction = (String) mTempMap.get("attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_NEMO_ACTION + "]");
                if (fpdm.nemo.NEMOUtils_mxJPO.RANGE_NEMO_ACTION_QC.equalsIgnoreCase(sNemoAction)) {
                    sObjectStatus = (String) mTempMap.get(SELECT_ATTRIBUTE_QC_CONVERSION_STATUS);
                    if ("".equals(sObjectStatus) || fpdm.nemo.NEMOUtils_mxJPO.RANGE_CONVERSION_STATUS_DONE.equals(sObjectStatus)) {
                        if ("".equals(sObjectStatus)) {
                            sObjectStatus = "(no status)";
                        }
                        hmQCList.put(sObjectTNR, sObjectStatus);
                    }
                } else if ("".equalsIgnoreCase(sNemoAction)) {
                    sObjectStatus = (String) mTempMap.get(SELECT_ATTRIBUTE_CONVERSION_STATUS);
                    if ("".equals(sObjectStatus) || fpdm.nemo.NEMOUtils_mxJPO.RANGE_CONVERSION_STATUS_DONE.equals(sObjectStatus)) {
                        if ("".equals(sObjectStatus)) {
                            sObjectStatus = "(no status)";
                        }
                        hmConversionsList.put(sObjectTNR, sObjectStatus);
                    }
                } else if (fpdm.nemo.NEMOUtils_mxJPO.RANGE_NEMO_ACTION_CANCEL.equalsIgnoreCase(sNemoAction)) {
                    sObjectStatus = (String) mTempMap.get(SELECT_ATTRIBUTE_REL_CONVERSION_STATUS);
                    if ("".equals(sObjectStatus)) {
                        sObjectStatus = "(no status)";
                    }
                    hmCancelList.put(sObjectTNR, sObjectStatus);
                } else {
                    sObjectStatus = (String) mTempMap.get(SELECT_ATTRIBUTE_REL_CONVERSION_STATUS);
                    if ("".equals(sObjectStatus) || fpdm.nemo.NEMOUtils_mxJPO.RANGE_CONVERSION_STATUS_DONE.equals(sObjectStatus)) {
                        if ("".equals(sObjectStatus)) {
                            sObjectStatus = "(no status)";
                        }
                        hmChoosenList.put(sObjectTNR, sObjectStatus);
                    }
                }
            }
        }
        try {
            /*
             * if (logger.isDebugEnabled()) { logger.debug("sendLimitTimeConversionRequestedAlert() - hmQCList.size() = <" + hmQCList.size() + "> hmConversionsList.size() = <" +
             * hmConversionsList.size() + "> hmChoosenList.size() = <" + hmChoosenList.size() + "> hmCancelList.size() = <" + hmCancelList.size() + ">"); } if (hmQCList.size() > 0 ||
             * hmConversionsList.size() > 0 || hmChoosenList.size() > 0 || hmCancelList.size() > 0) { //String sSubject = "!ALERT NEMO CONVERSION QUEUE! " +
             * FPDMOrganizationName_mxJPO.getEnvironment(context) + "-" + FPDMOrganizationName_mxJPO.getName(context); String sSubject = ""; // TODO String sText =
             * "Some NEMO conversions were launched since more than " + lLimitTimeInConversion + " hours. It should be a problem, please check them.\nHere is the list:\n";
             * FPDMNotification_mxJPO.sendMailToSupport3(context, fpdm.nemo.NEMOUtils_mxJPO.constructAlertMessage(hmQCList, hmConversionsList, hmChoosenList, hmCancelList, sText), sSubject);
             * 
             * bAlertSent = true; }
             */
        } catch (Exception e) {
            logger.error("Unable to send email");
            logger.error("Error with NEMO Conversion queue");
            logger.error("hmNemoSum:" + hmConversionsList);
            logger.error("hmQCSum:" + hmQCList);
            logger.error("hmChoosenList:" + hmChoosenList);
            logger.error("hmCancelList:" + hmCancelList);
            // do not block the process if this email cannot be sent
            logger.error(e.toString(), e);
        }

        return bAlertSent;
    }

    /**
     * Inner class used to sort all CAD Objects present in Conversion queue by the requestor site. In that case, we do not perform a lot of change site request for NEMO
     */
    private final class RequestorSiteComparator implements Comparator, Serializable {

        /**
         * the Serial UID
         */
        private static final long serialVersionUID = 1L;

        private Context context;

        public RequestorSiteComparator(Context context) {
            this.context = context;
        }

        /**
         * Compare 2 Map having the <code>"attribute[" + ATTRIBUTE_CONVERSION_REQUESTER + "]"</code>. <br/>
         * At first time, compare site of user having name equals to value of <code>"attribute[" + ATTRIBUTE_CONVERSION_REQUESTER + "]"</code> key. If they are equals, compare names. <br/>
         * If error occurs while fetching users site, we consider user site equals to <code>DEFAULT_USER_SITE</code>. <br/>
         * We do not throw exception because it is more important to treat the object, even if it is not placed at the good place, than kill the whole process. <br/>
         * This method is only used inside {@link FPDMNEMOInterface_mxJPO#perform(Context, String[])} method before the treatment.
         */
        public int compare(Object o1, Object o2) {
            int compare = 0;
            Map first = (Map) o1;
            Map second = (Map) o2;
            String sConversionRequester = "attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_CONVERSION_REQUESTER + "]";
            String sFirstRequester = (String) first.get(sConversionRequester);
            String sSecondRequester = (String) second.get(sConversionRequester);
            String sFirstUserSite = DEFAULT_USER_SITE;
            String sSecondUserSite = DEFAULT_USER_SITE;

            try {
                sFirstUserSite = getMappedUserSite(context, sFirstRequester);
            } catch (SiteNotMappedException e) {
                // Ignore exception for Site not mapped exception.
                logger.warn("The site of <" + sFirstRequester + "> is not mapped in Nemo. Using default one");
            } catch (ConversionUserNotExistException fe) {
                // Ignore exception for FrameworkException.
                logger.warn("An error occurs while fetching the site of <" + sFirstRequester + ">. Using default site one", fe);
            }
            try {
                sSecondUserSite = getMappedUserSite(context, sSecondRequester);
            } catch (SiteNotMappedException e) {
                // Ignore exception for Site not mapped exception.
                logger.warn("The site of <" + sSecondRequester + "> is not mapped in Nemo. Using default one");
            } catch (ConversionUserNotExistException fe) {
                // Ignore exception for FrameworkException.
                logger.warn("An error occurs while fetching the site of <" + sSecondRequester + ">. Using default site one", fe);
            }

            compare = sFirstUserSite.compareTo(sSecondUserSite);
            if (compare == 0) {
                compare = sFirstRequester.compareTo(sSecondRequester);
            }
            return compare;
        }
    }

    private static void analyzeNemoOutput(StringHolder so) throws Exception {
        if (so == null || so.value == null) {
            throw new Exception("StringHolder is empty");
        }
        if (logger.isTraceEnabled()) {
            logger.trace("StringHolder : " + so.value);
        }
        try {
            SAXBuilder sxb = new SAXBuilder();
            Document document = sxb.build(new StringReader(so.value));
            Element elRoot = document.getRootElement();
            checkNemoReturn(elRoot.getChild("output", nsef));
            if (logger.isDebugEnabled()) {
                logger.debug("No error has been found in Nemo output");
            }
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            logger.error("Error detected in Nemo return (StringHolder) : " + so.value);
            throw e;
        }
    }

    public static HashMap<String, ArrayList<String[]>> retrieveAllOptions(Context context) throws Exception {
        logger.info("retrieveAllOptions");
        if (hmNeutralForms == null) {
            String sOptions = fpdm.nemo.NEMOUtils_mxJPO.getOptionsFromDB(context);
            if (sOptions == null || sOptions.length() == 0) {
                sOptions = getOptionsFromNemo(context);
                try {
                    fpdm.nemo.NEMOUtils_mxJPO.updateOrInsertOptionsProgram(context, sOptions);
                } catch (Exception e) {
                    logger.error("Could not update option program in database", e);
                }
            }
            HashMap<String, ArrayList<String[]>> hmTempForms = parseNemoOptions(sOptions);
            if (hmNeutralForms == null) {
                hmNeutralForms = hmTempForms;
            }
        }
        return hmNeutralForms;
    }

    private static String getOptionsFromNemo(Context context) throws Exception {
        // get Options from Nemo
        try {
            fpdm.nemo.NEMOInterface_mxJPO nemoInterface = new fpdm.nemo.NEMOInterface_mxJPO(context, new String[0]);
            return nemoInterface.retrieveStringOptions(context, fpdm.nemo.NEMOUtils_mxJPO.getChains(context, null));
            // return nemoInterface.retrieveStringOptions(context, saChains);
        } catch (Exception e) {
            logger.error("Error happened while trying to get Options from nemo", e);
            throw e;
        }
    }

    public static HashMap<String, Object> retrieveExtraHiddenOptions(Context context, String sChain) throws Exception {
        if (hmNeutralForms == null || hmAllNeutralInfos == null) {
            retrieveAllOptions(context);
        }
        if (hmAllNeutralInfos != null) {
            return hmAllNeutralInfos.get(sChain);
        }
        return null;
    }

    public static HashMap<String, ArrayList<String[]>> parseNemoOptions(String sOptions) throws JDOMException, IOException {
        HashMap<String, ArrayList<String[]>> hmToReturn = new HashMap<String, ArrayList<String[]>>();
        HashMap<String, HashMap<String, Object>> hmAllInfos = new HashMap<String, HashMap<String, Object>>();
        if (alToHideOptions == null) {
            ArrayList<String> alHideOptions = new ArrayList<String>();
            for (String sToHide : saToHideOptions) {
                alHideOptions.add(sToHide);
            }
            // do not re-update if already updated by another script (static variable)
            if (alToHideOptions == null) {
                alToHideOptions = alHideOptions;
            }
        }
        SAXBuilder sxb = new SAXBuilder();
        Document document = sxb.build(new StringReader(sOptions));
        Element elRoot = document.getRootElement();
        Namespace nsef = elRoot.getNamespace();
        List<Element> lOutput = elRoot.getChildren(fpdm.nemo.NEMOUtils_mxJPO.XML_OUTPUT, nsef);
        if (logger.isDebugEnabled()) {
            logger.debug("lOutput : " + lOutput);
        }
        int i = 0;
        if (lOutput != null) {
            for (Element elOutput : lOutput) {
                List<Element> lServices = elOutput.getChildren(fpdm.nemo.NEMOUtils_mxJPO.XML_SERVICE, nsef);
                if (logger.isDebugEnabled()) {
                    logger.debug("lServices : " + lServices);
                }
                if (lServices != null) {
                    for (Element elService : lServices) {
                        Element elName = elService.getChild(fpdm.nemo.NEMOUtils_mxJPO.XML_NAME, nsef);
                        if (elName != null) {
                            HashMap<String, Object> hmChainInfos = new HashMap<String, Object>();
                            ArrayList<String[]> alToReturn = new ArrayList<String[]>();
                            String sTreatmentId = null;
                            ArrayList<Element> alAllOptions = new ArrayList<Element>();
                            fpdm.nemo.NEMOUtils_mxJPO.retrieveAllOptions(alAllOptions, elService, nsef);
                            if (alAllOptions != null) {
                                for (Element elOption : alAllOptions) {
                                    // logger.debug("elOption : " + elOption);
                                    if (elOption != null) {
                                        String sName = elOption.getAttributeValue(fpdm.nemo.NEMOUtils_mxJPO.XML_ID);
                                        if (sName != null && sName.length() > 0) {
                                            HashMap<String, String> hmOptionInfos = new HashMap<String, String>();
                                            String sValue = elOption.getText();
                                            if (sName.equals(fpdm.nemo.NEMOUtils_mxJPO.NEMOConstants.OPT_TREATMENT)) {
                                                sTreatmentId = sValue;
                                            }
                                            String sType = elOption.getAttributeValue(fpdm.nemo.NEMOUtils_mxJPO.XML_TYPE);
                                            String sLabel = elOption.getAttributeValue(fpdm.nemo.NEMOUtils_mxJPO.XML_LABEL);
                                            hmOptionInfos.put(fpdm.nemo.NEMOUtils_mxJPO.XML_TYPE, sType);
                                            hmOptionInfos.put(fpdm.nemo.NEMOUtils_mxJPO.XML_LABEL, sLabel);
                                            hmOptionInfos.put(fpdm.nemo.NEMOUtils_mxJPO.XML_ID, sName);
                                            hmOptionInfos.put(fpdm.nemo.NEMOUtils_mxJPO.XML_VALUE, sValue);
                                            if (!alToHideOptions.contains(sName)) {
                                                if (fpdm.nemo.NEMOUtils_mxJPO.VAL_HIDDEN.equalsIgnoreCase(sType)) {
                                                    // String sValue = elOption.getText();
                                                    // sbFormForTreatment.append("<input type=\"hidden\" name=\"").append(sTreatmentId).append(sName).append("\"
                                                    // id=\"").append(sTreatmentId).append(sName).append("\" value=\"").append(sValue).append("\">\n");
                                                } else if (fpdm.nemo.NEMOUtils_mxJPO.VAL_RADIO.equalsIgnoreCase(sType)) {
                                                    List<Element> elChoices = elOption.getChildren(fpdm.nemo.NEMOUtils_mxJPO.XML_OPTION, nsef);
                                                    if (elChoices != null) {
                                                        StringBuilder sbOption = new StringBuilder("");
                                                        for (Element eChoice : elChoices) {
                                                            String sChoiceId = eChoice.getAttributeValue(fpdm.nemo.NEMOUtils_mxJPO.XML_ID);
                                                            String sChoiceSelected = eChoice.getAttributeValue("selected");
                                                            String sChoiceLabel = eChoice.getText();
                                                            String sToolTip = eChoice.getAttributeValue("tooltip");
                                                            sbOption.append("<input type=\"radio\"  name=\"").append(sTreatmentId).append(sName).append("\" id=\"").append(sTreatmentId).append(sName)
                                                                    .append("\" ");
                                                            if ("selected".equalsIgnoreCase(sChoiceSelected)) {
                                                                sbOption.append("checked=\"checked\" ");
                                                            }
                                                            sbOption.append("value=\"").append(sChoiceId).append("\"> ");
                                                            if (sToolTip != null && sToolTip.length() > 0) {
                                                                StringBuilder sbToolTip = new StringBuilder("");
                                                                sbToolTip.append("<label onMouseOut=\"hideTooltip(").append(sTreatmentId).append(i).append(")\" onMouseOver=\"showTooltip(")
                                                                        .append(sTreatmentId).append(i).append(", '").append(sToolTip).append("')\">").append(sChoiceLabel)
                                                                        .append("</label></input><div style=\"display:none\" id=\"").append(sTreatmentId).append(i).append("\"></div>");
                                                                i++;
                                                                sChoiceLabel = sbToolTip.toString();
                                                                HashMap<String, String> hmRadioOptionInfos = new HashMap<String, String>();
                                                                hmRadioOptionInfos.put(fpdm.nemo.NEMOUtils_mxJPO.XML_LABEL, sChoiceLabel);
                                                                hmChainInfos.put(sChoiceId, hmRadioOptionInfos);
                                                            }
                                                            sbOption.append(sChoiceLabel).append("<br/>\n");
                                                        }
                                                        alToReturn.add(new String[] { sLabel, sbOption.toString() });
                                                    }
                                                } else if (fpdm.nemo.NEMOUtils_mxJPO.VAL_LIST.equalsIgnoreCase(sType)) {
                                                    List<Element> elChoices = elOption.getChildren(fpdm.nemo.NEMOUtils_mxJPO.XML_OPTION, nsef);
                                                    if (elChoices != null) {
                                                        StringBuilder sbOption = new StringBuilder("");
                                                        String sToolTip = elOption.getAttributeValue("tooltip");
                                                        if (sToolTip != null && sToolTip.length() > 0) {
                                                            StringBuilder sbToolTip = new StringBuilder("");
                                                            sbToolTip.append("<label onMouseOut=\"hideTooltip(").append(sTreatmentId).append(i).append(")\" onMouseOver=\"showTooltip(")
                                                                    .append(sTreatmentId).append(i).append(", '").append(sToolTip).append("')\">").append(sLabel)
                                                                    .append("</label></input><div style=\"display:none\" id=\"").append(sTreatmentId).append(i).append("\"></div>");
                                                            i++;
                                                            sLabel = sbToolTip.toString();
                                                        }
                                                        sbOption.append("<select ");
                                                        if ("true".equalsIgnoreCase(elOption.getAttributeValue("multi"))) {
                                                            sbOption.append("multiple=\"true\" size=\"").append(elChoices.size()).append("\" ");
                                                        }
                                                        sbOption.append("name=\"").append(sTreatmentId).append(sName).append("\" id=\"").append(sTreatmentId).append(sName).append("\" >");
                                                        for (Element eChoice : elChoices) {
                                                            String sChoiceId = eChoice.getAttributeValue("id");
                                                            String sChoiceSelected = eChoice.getAttributeValue("selected");
                                                            String sChoiceLabel = eChoice.getText();
                                                            sbOption.append("<option value=\"").append(sChoiceId).append("\" ");
                                                            if ("selected".equalsIgnoreCase(sChoiceSelected)) {
                                                                sbOption.append("checked=\"checked\" ");
                                                            }
                                                            sbOption.append("> ").append(sChoiceLabel).append("</option>");
                                                        }
                                                        sbOption.append("</select>");
                                                        alToReturn.add(new String[] { sLabel, sbOption.toString() });
                                                    }
                                                } else if (fpdm.nemo.NEMOUtils_mxJPO.VAL_BOOLEAN.equalsIgnoreCase(sType)) {
                                                    String sToolTip = elOption.getAttributeValue("tooltip");
                                                    StringBuilder sbOption = new StringBuilder("");
                                                    sbOption.append("<input type=\"checkbox\"  value=\"").append(sValue).append("\" name=\"").append(sTreatmentId).append(sName).append("\"  id=\"")
                                                            .append(sTreatmentId).append(sName).append("\" ");
                                                    String sChoiceSelected = elOption.getAttributeValue("selected");
                                                    if ("selected".equalsIgnoreCase(sChoiceSelected)) {
                                                        sbOption.append("checked=\"checked\" ");
                                                    }
                                                    sbOption.append(">");
                                                    if (sToolTip != null && sToolTip.length() > 0) {
                                                        StringBuilder sbToolTip = new StringBuilder("");
                                                        sbToolTip.append("<label onMouseOut=\"hideTooltip(").append(sTreatmentId).append(i).append(")\" onMouseOver=\"showTooltip(")
                                                                .append(sTreatmentId).append(i).append(", '").append(sToolTip).append("')\">").append(sLabel)
                                                                .append("</label></input><div style=\"display:none\" id=\"").append(sTreatmentId).append(i).append("\"></div>");
                                                        i++;
                                                        sLabel = sbToolTip.toString();
                                                    }
                                                    sbOption.append("</input>");
                                                    alToReturn.add(new String[] { sLabel, sbOption.toString() });
                                                } else if (fpdm.nemo.NEMOUtils_mxJPO.VAL_TEXT.equalsIgnoreCase(sType)) {
                                                    StringBuilder sbOption = new StringBuilder("");
                                                    String sToolTip = elOption.getAttributeValue("tooltip");
                                                    if (sToolTip != null && sToolTip.length() > 0) {
                                                        StringBuilder sbToolTip = new StringBuilder("");
                                                        sbToolTip.append("<label onMouseOut=\"hideTooltip(").append(sTreatmentId).append(i).append(")\" onMouseOver=\"showTooltip(")
                                                                .append(sTreatmentId).append(i).append(", '").append(sToolTip).append("')\">").append(sLabel)
                                                                .append("</label></input><div style=\"display:none\" id=\"").append(sTreatmentId).append(i).append("\"></div>");
                                                        i++;
                                                        sLabel = sbToolTip.toString();
                                                    }
                                                    sbOption.append("<input type=\"text\" value=\"\" name=\"").append(sTreatmentId).append(sName).append("\"  id=\"").append(sTreatmentId).append(sName)
                                                            .append("\" />");
                                                    alToReturn.add(new String[] { sLabel, sbOption.toString() });
                                                } else {
                                                    if (logger.isDebugEnabled()) {
                                                        logger.debug("UNKNOWN FIELD TYPE : " + sType + " for treatment " + sTreatmentId);
                                                    }
                                                }
                                            }
                                            hmChainInfos.put(sName, hmOptionInfos);
                                        }
                                    }
                                }
                            }
                            String sTreatmentName = elName.getText();
                            StringBuilder sbTreatment = new StringBuilder("<input type=\"checkbox\" value=\"YES\" name=\"").append(sTreatmentId).append("\"  id=\"").append(sTreatmentId).append("\" ");
                            sbTreatment.append(">").append(sTreatmentName).append("</input>");
                            alToReturn.add(0, new String[] { sbTreatment.toString() });
                            hmToReturn.put(sTreatmentId, alToReturn);
                            hmChainInfos.put(fpdm.nemo.NEMOUtils_mxJPO.XML_CHAIN_LABEL, sTreatmentName);
                            hmAllInfos.put(sTreatmentId, hmChainInfos);
                        }
                    }
                }
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("hmToReturn : " + hmToReturn);
        }
        hmAllNeutralInfos = hmAllInfos;
        return hmToReturn;
    }

    public static boolean is2D(Context context, String objectId) {
        boolean response = false;
        try {
            DomainObject doToConvert = new DomainObject(objectId);
            response = doToConvert.isKindOf(context, "MCAD Drawing");
        } catch (Exception e) {
            logger.info("Can't open object with id <" + objectId + ">");
            e.printStackTrace();
        }
        return response;
    }

    public static boolean isAssembly(Context context, String objectId) {
        boolean response = false;
        try {
            DomainObject doToConvert = new DomainObject(objectId);
            response = doToConvert.isKindOf(context, "MCAD Assembly");
        } catch (Exception e) {
            logger.info("Can't open object with id <" + objectId + ">");
            e.printStackTrace();
        }
        return response;
    }

    public static boolean isModel(Context context, String objectId) {
        boolean response = false;
        try {
            DomainObject doToConvert = new DomainObject(objectId);
            response = doToConvert.isKindOf(context, "MCAD Model");
        } catch (Exception e) {
            logger.info("Can't open object with id <" + objectId + ">");
            e.printStackTrace();
        }
        return response;
    }

    public static boolean is2D(Context context, String[] objectId) {
        boolean response = false;
        try {
            response = fpdm.nemo.NEMOInterface_mxJPO.is2D(context, objectId[0]);
        } catch (Exception e) {
            logger.info("Can't open object with id <" + objectId + ">");
            e.printStackTrace();
        }
        return response;
    }

    public static boolean isAssembly(Context context, String[] objectId) {
        boolean response = false;
        logger.debug("IsAssembly ? " + objectId[0]);
        try {
            response = fpdm.nemo.NEMOInterface_mxJPO.isAssembly(context, objectId[0]);
        } catch (Exception e) {
            logger.info("Can't open object with id <" + objectId + ">");
            e.printStackTrace();
        }
        return response;
    }

    public static boolean isModel(Context context, String[] objectId) {
        boolean response = false;
        try {
            response = fpdm.nemo.NEMOInterface_mxJPO.isModel(context, objectId[0]);
        } catch (Exception e) {
            logger.info("Can't open object with id <" + objectId[0] + ">");
            e.printStackTrace();
        }
        return response;
    }

    public static HashMap<String, String[]> retrieveAllConversionsTypes(Context context, String[] objectId) throws FrameworkException {
        PROPERTY_NEMO_conv2D = EnoviaResourceBundle.getProperty(context, "EngineeringCentral", "FPDM.NEMO.conv2D", context.getSession().getLanguage());
        PROPERTY_NEMO_conv3D = EnoviaResourceBundle.getProperty(context, "EngineeringCentral", "FPDM.NEMO.conv3D", context.getSession().getLanguage());
        PROPERTY_NEMO_convAssembly = EnoviaResourceBundle.getProperty(context, "EngineeringCentral", "FPDM.NEMO.convAssembly", context.getSession().getLanguage());

        logger.debug(PROPERTY_NEMO_conv2D + " / " + PROPERTY_NEMO_conv3D + " / " + PROPERTY_NEMO_convAssembly);

        logger.debug("2D : " + fpdm.nemo.NEMOInterface_mxJPO.PROPERTY_NEMO_conv2D);
        logger.debug("Model : " + fpdm.nemo.NEMOInterface_mxJPO.PROPERTY_NEMO_conv3D);
        logger.debug("Assembly : " + fpdm.nemo.NEMOInterface_mxJPO.PROPERTY_NEMO_convAssembly);

        HashMap<String, String[]> hmResult = new HashMap<String, String[]>();
        hmResult.put("2D", fpdm.nemo.NEMOInterface_mxJPO.PROPERTY_NEMO_conv2D.split(","));
        hmResult.put("Model", fpdm.nemo.NEMOInterface_mxJPO.PROPERTY_NEMO_conv3D.split(","));
        hmResult.put("Assembly", fpdm.nemo.NEMOInterface_mxJPO.PROPERTY_NEMO_convAssembly.split(","));

        return hmResult;
    }

    public static boolean isToConvert(Context context, String objectId, String nemoChain, String nemoOptions) {
        return true;
    }

    /**
     * Returns the mapped CAD Coversion user name for the given user
     * @param context
     *            ematrix context
     * @param sConversionRequester
     *            the user
     * @return the CAD Coversion user name to used for the given user
     * @throws FrameworkException
     *             if we are unable to find the PDM Site
     * @throws SiteNotMappedException
     *             if the PDM Site is not mapped.
     */
    private String getConversionUserName(Context context, String sConversionRequester) throws ConversionUserNotExistException, FrameworkException {
        if (hmSiteByPerson.containsKey(sConversionRequester)) {
            sPDMUserSite = hmSiteByPerson.get(sConversionRequester);
        } else {
            if (!checkUserExist(context, sConversionRequester)) {
                throw new ConversionUserNotExistException("User <" + sConversionRequester + "> doesn't exist.");
            }
            sPDMUserSite = MqlUtil.mqlCommand(context, "print Person \"" + sConversionRequester + "\" select site dump");
            hmSiteByPerson.put(sConversionRequester, sPDMUserSite);
        }
        if (sPDMUserSite != null && !"".equals(sPDMUserSite)) {
            return FPDM_CAD_CONVERSION_USERNAME_TEMPLATE.replaceAll("<SITE>", sPDMUserSite);
        } else {
            return FPDM_CAD_CONVERSION_USERNAME_TEMPLATE.replaceAll("<SITE>", "DEFAULT");
        }
    }

    private boolean checkUserExist(Context context, String sConversionRequester) throws FrameworkException {
        String sResult = MqlUtil.mqlCommand(context, "list Person \"" + sConversionRequester + "\"");
        if (logger.isDebugEnabled()) {
            logger.debug("checkUserExist() - User = <" + sConversionRequester + "> sResult <" + sResult + ">");
        }
        return sConversionRequester.equals(sResult);
    }
}
