package fpdm.nemo;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import matrix.db.AttributeList;
import matrix.db.BusinessObject;
import matrix.db.BusinessObjectProxy;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.Program;
import matrix.db.ProgramList;
import matrix.db.Query;
import matrix.db.QueryIterator;
import matrix.util.MatrixException;
import matrix.util.Pattern;
import matrix.util.SelectList;
import matrix.util.StringList;

import org.apache.axis.encoding.Base64;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.framework.ui.UINavigatorUtil;
import com.matrixone.jdom.Element;
import com.matrixone.jdom.Namespace;

@SuppressWarnings({ "unchecked", "rawtypes" })
public final class NEMOUtils_mxJPO {

    private static Logger logger = LoggerFactory.getLogger("fpdm.nemo.NEMOUtils");

    private static String PROGRAM_NAME = "FPDMNEMOUtils.properties";

    public final static String RELATIONSHIP_NEMO_CONVERSION_QUEUE = "FPDM_TreatmentQueueToDoNemoConversion"; // TODO Add to properties

    public final static String ATTRIBUTE_NEMO_CONVERSION_QUEUE_JOB_PARAMS = "FPDM_QueueJobParams"; // TODO Add to properties

    public final static String ATTRIBUTE_NEMO_CONVERSION_OPTIONS = "FPDM_NemoConversionOptions";

    public final static String ATTRIBUTE_NEMO_ACTION = "FPDM_QueueJobNemoAction"; // TODO Add to properties

    public final static String RELATIONSHIP_VIEWABLE = "Viewable"; // TODO Add to properties

    public final static String ATTRIBUTE_CONVERSION_REQUESTER = "FPDM_QueueJobRequester"; // TODO Add to properties

    public final static String ATTRIBUTE_QUEUE_JOB_MESSAGE = "FPDM_QueueJobMessage"; // TODO Add to properties

    public final static String ATTRIBUTE_QUEUE_JOB_ITERATION = "FPDM_QueueJobIteration"; // TODO Add to properties

    public final static String TYPE_NEMO_CONVERSION_CONFIGURATION = "FPDM_NemoConversionConfiguration"; // TODO Add to properties

    public final static String RELATIONSHIP_ACTIVE_VERSION = PropertyUtil.getSchemaProperty("relationship_ActiveVersion"); // TODO Add to properties

    public final static String TYPE_QC_VIEWABLE = ""; // TODO

    public final static String TYPE_VIEWABLE = ""; // TODO

    public final static String TYPE_UG_ASSEMBLY = ""; // TODO

    public final static String RELATIONSHIP_CAD_SUBCOMPONENT = "CAD SubComponent"; // TODO

    public final static String TYPE_CATPRODUCT = ""; // TODO

    public final static String TYPE_CATPART = ""; // TODO

    public final static String RELATIONSHIP_ASSOCIATED_DRAWING = ""; // TODO

    public final static String TYPE_FPDM_CAD_DRAWING = ""; // TODO

    public final static String TYPE_FPDM_2D_NEUTRAL_VIEWABLE = ""; // TODO

    public final static String POLICY_VIEWABLE_POLICY = ""; // TODO

    public final static String TYPE_FPDM_3D_NEUTRAL_VIEWABLE = ""; // TODO

    public final static String POLICY_3DVIEWABLE = ""; // TODO

    public final static String TYPE_UG_MODEL = ""; // TODO

    public final static String TYPE_CATIA_V4_MODEL = ""; // TODO

    public final static String TYPE_CATIA_CGR = ""; // TODO

    public final static String ATTRIBUTE_FPDM_NEMO_CHAIN = ""; // TODO !!!

    public final static String ATTRIBUTE_CONVERSION_RESULT = ""; // TODO !!!

    public final static String ATTRIBUTE_CONVERSION_IDENTIFIANT = "FPDM_QueueJobId"; // TODO !!!

    public final static String TYPE_FPDM_CAD_COMPONENT = ""; // TODO !!!

    public final static String POLICY_DESIGN_POLICY = ""; // TODO !!!

    public final static String TYPE_CATDRAWING = ""; // TODO !!!

    public final static String TYPE_UG_DRAWING = ""; // TODO !!!

    public final static String ATTRIBUTE_QC_CURRENT_ACTION = ""; // TODO !!!

    public final static String ATTRIBUTE_QC_RESULT = ""; // TODO !!!

    public final static String ATTRIBUTE_CONVERSION_STATUS = "FPDM_QueueJobProcessStatus"; // TODO !!!

    public final static String ATTRIBUTE_REL_CONVERSION_STATUS = ""; // TODO !!!

    public final static String VAULT_PRODUCTION = PropertyUtil.getSchemaProperty("vault_eServiceProduction"); // TODO Add to properties

    public final static String VAULT_ADMINISTRATION = PropertyUtil.getSchemaProperty("vault_eServiceAdministration"); // TODO Add to properties

    public final static String VALUE_STATUS_QUEUED = "QUEUED";

    private static String sCADStatePreliminary = null;

    private final static String SELECT_NEMO_ACTION_FROM_CAD_OBJECT = "to[" + RELATIONSHIP_NEMO_CONVERSION_QUEUE + "].attribute[" + ATTRIBUTE_NEMO_ACTION + "]";

    private final static String SELECT_VIEWABLE_ID = "from[" + RELATIONSHIP_VIEWABLE + "].to.id";

    private static final String OPTIONS_SEPARATOR = "|";

    private static final String OPTION_VALUE_SEPARATOR = "=";

    private static final String VALUES_SEPARATOR = ";";

    private static final String OPTIONS_SEPARATOR_TOSPLIT = "[|]";

    public static final String XML_TYPE = "type";

    public static final String XML_LABEL = "label";

    public static final String XML_VALUE = "value";

    public static final String XML_NAME = "name";

    public static final String XML_ID = "id";

    public static final String XML_OPTION = "option";

    public static final String XML_OUTPUT = "output";

    public static final String XML_SERVICE = "service";

    public static final String XML_RESULT = "result";

    public static final String XML_ERROR = "error";

    public static final String XML_TITLE = "title";

    public static final String XML_MESSAGE = "message";

    public static final String XML_CHAIN_LABEL = "chain_label";

    public static final String VAL_HIDDEN = "hidden";

    public static final String VAL_BOOLEAN = "boolean";

    public static final String VAL_RADIO = "radio";

    public static final String VAL_LIST = "list";

    public static final String VAL_TEXT = "text";

    public static final String SELECT_NEMO_ACTION = "attribute[" + ATTRIBUTE_NEMO_ACTION + "]";

    private static final String HISTORY_ACTION = "action";

    private static final String HISTORY_USER = "user";

    private static final String HISTORY_CHECKIN = "checkin";

    private static final String OPEN_PDM_LOCAL_USERS_START = "OPENPDM_FOR_";

    private static FPDMProgramProperties programProperties = null;

    static final String TYPE_NEMO_CONVERSION_QUEUE = "FPDM_TreatmentQueueNemoConversion"; // TODO Add to properties

    static final String NEMO_CONVERSION_QUEUE_OBJECT_NAME = "FPDM_TreatmentQueueNemo"; // TODO Add to properties

    static final String NEMO_CONVERSION_QUEUE_OBJECT_REVISION = "-"; // TODO Add to properties

    static final String PERSON_USER_AGENT = "person_UserAgent"; // TODO Add to properties

    public static final String ATTRIBUTE_FPDM_LIMIT_CANCEL_TRIALS = "FPDM_NemoLimitOfCancelTrials";

    public static final String ATTRIBUTE_QC_IDENTIFIANT = PropertyUtil.getSchemaProperty("attribute_FPDMQCIdentifiant");

    public static final String ATTRIBUTE_QC_PROFILE = PropertyUtil.getSchemaProperty("attribute_FPDMQCProfile");

    public static final String ATTRIBUTE_QC_ENVIRONMENT = PropertyUtil.getSchemaProperty("attribute_FPDMQCEnvironment");

    public static final String ATTRIBUTE_QC_DATE_TIME = PropertyUtil.getSchemaProperty("attribute_FPDMQCDateTime");

    public static final String ATTRIBUTE_PROCESS_IN_USE = "FPDM_QueueJobProcessStatus"; // TODO Add to properties

    public static final String ATTRIBUTE_NEMO_MAXIMUM_NUMBER_OF_REQUEST = "FPDM_NemoMaximumNumberOfCancelRequests"; // TODO Add to properties

    public static final String ATTRIBUTE_FOLIO_NUMBER = PropertyUtil.getSchemaProperty("attribute_Folio_number");

    public static final String ATTRIBUTE_NEMO_MAXIMUM_NUMBER_OF_CANCEL = "FPDM_NemoMaximumNumberOfCancelRequests";

    public static final String ATTRIBUTE_FPDM_MAJOR_REV = PropertyUtil.getSchemaProperty("attribute_FPDMMajorRev");

    public static final String ATTRIBUTE_NEMO_LIMIT_OBJECTS_ALERT = "FPDM_TreatmentQueueLimitObjectsAlert"; // TODO Add to properties

    public static final String ATTRIBUTE_NEMO_LAST_NOTIFICATION_TIME = ""; // TODO Add to properties -- ???

    public static final String ATTRIBUTE_NEMO_LIMIT_TIME_CONVERSION_REQUESTED = "FPDM_TreatmentQueueLimitRequestAge"; // TODO Add to properties

    public static final String ATTRIBUTE_FPDM_NEMO_CONVERSION_NAME = PropertyUtil.getSchemaProperty("attribute_FPDM_NemoConversionName");

    public static final String ATTRIBUTE_FPDM_NEMO_CONVERSION_PDM_TYPE = PropertyUtil.getSchemaProperty("attribute_FPDM_NemoConversionPdmType");

    public static final String ATTRIBUTE_NEMO_CONVERSION_FOR_ASSEMBLY = PropertyUtil.getSchemaProperty("attribute_FPDM_NemoConversionForAssembly");

    /*
     * ************************************************************************ R A N G E S
     ************************************************************************/
    public final static String RANGE_FOR_REVISE = "For Revise";

    public final static String RANGE_FOR_RELEASE = "For Release";

    public final static String RANGE_FOR_OBSOLETE = "For Obsolescence";

    public final static String RANGE_FOR_OBSOLETESCENCE = "For Obsolescence";

    public final static String RANGE_NONE = "None";

    public final static String RANGE_PROCESS_IN_USE_FALSE = "FALSE";

    public final static String RANGE_PROCESS_IN_USE_TRUE = "TRUE";

    public final static String RANGE_PROCESS_IN_USE_LOCKED = "LOCKED";

    public final static String RANGE_QC_PART_RESULT_SUCCESS = "SUCCESS";

    public final static String RANGE_QC_PART_RESULT_FAILED = "FAILED";

    public final static String RANGE_QC_PART_RESULT_PARTIALLY = "PARTIALLY";

    public final static String RANGE_QC_PART_RESULT_NOTAPPLICABLE = "NOT APPLICABLE";

    public final static String RANGE_QC_PART_RESULT_EMPTY = "UNASSIGNED";

    public final static String RANGE_QC_RESULT_SUCCESS = "SUCCESS";

    public final static String RANGE_QC_RESULT_FAILED = "FAILED";

    public final static String RANGE_QC_RESULT_EMPTY = "UNASSIGNED";

    public final static String RANGE_QC_RESULT_ERROR = "ERROR";

    public final static String RANGE_NEMO_ACTION_QC = "QC";

    public final static String RANGE_NEMO_ACTION_CANCEL = "cancel";

    private static final String RELATIONSHIP_VERSIONOF = null;

    public static final String PROPERTY_NEMO_LOGIN = null;

    public static final String PROPERTY_NEMO_PASSWORD = null;

    public static final String PROPERTY_NEMO_DOMAIN = null;

    public final static String RANGE_COMPLETION_STATUS_SUCCEEDED = "Succeeded";

    public final static String RANGE_COMPLETION_STATUS_FAILED = "Failed";

    public final static String RANGE_COMPLETION_STATUS_WARNING = "Warning";

    public final static String RANGE_COMPLETION_STATUS_ABORTED = "Aborted";

    public final static String RANGE_COMPLETION_STATUS_NONE = "None";

    public final static String RANGE_CONVERSION_STATUS_SUCCEED = "SUCCEED";

    public final static String RANGE_CONVERSION_STATUS_FAILED = "FAILED";

    public final static String RANGE_CONVERSION_STATUS_DONE = "DONE";

    public final static String RANGE_CONVERSION_STATUS_QUEUED = "QUEUED";

    public final static String RANGE_CONVERSION_STATUS_FINISHING = "FINISHING";

    public final static String RANGE_CONVERSION_STATUS_KILLED = "KILLED";

    public final static String RANGE_CONVERSION_STATUS_CANCELLED = "CANCELLED";

    public final static String RANGE_CONVERSION_STATUS_CHECKIN_REQUEST = "CHECKIN REQUEST";

    public final static String RANGE_CONVERSION_STATUS_RUNNING = "RUNNING";

    public final static String RANGE_CONVERSION_STATUS_WAITING_TREATMENT_START = "WAITING-TREATMENT-START";

    public final static String RANGE_CONVERSION_STATUS_PDM_CHECKOUT = "PDM-CHECKOUT";

    public final static String RANGE_CONVERSION_STATUS_PDM_CHECKIN_WAIT = "PDM-CHECKIN-WAIT";

    public final static String RANGE_CONVERSION_STATUS_CHECKIN_FAILED = "PDM-CHECKIN-FAILED";

    public final static String RANGE_MDE_TRANSFER_TYPE_BOMS = "BOMS";

    public final static String RANGE_MDE_TRANSFER_TYPE_PARTS = "PARTS";

    public final static String RANGE_MDE_TRANSFER_TYPE_TOOL = "TOOL";

    /**
     * All constants used only for NEMO.
     */
    public final static class NEMOConstants {

        public final static String OPT_CAD_OBJECT_ID = "OPT_CAD_OBJECT_ID";

        public final static String OPT_FILE_NAME = "OPT_FILE_NAME";

        public final static String OPT_FILE_FORMAT = "OPT_FILE_FORMAT";

        public final static String OPT_PARAM1 = "OPT_PARAM1";

        public final static String OPT_PARAM2 = "OPT_PARAM2";

        public final static String OPT_PARAM3 = "OPT_PARAM3";

        public final static String OPT_PARAM4 = "OPT_PARAM4";

        public final static String OPT_PARAM5 = "OPT_PARAM5";

        public final static String OPT_PARAM6 = "OPT_PARAM6";

        public final static String OPT_PARAM7 = "OPT_PARAM7";

        public final static String OPT_PARAM8 = "OPT_PARAM8";

        public final static String OPT_NEMO_ID = "OPT_NEMO_ID";

        public final static String OPT_NEMO_STATUS = "OPT_NEMO_STATUS";

        public final static String OPT_NEMO_MESSAGE = "OPT_NEMO_MESSAGE";

        public final static String OPT_PDM_URL = "OPT_PDM_URL";

        public final static String OPT_INPUT_FILE_SIZE = "OPT_INPUT_FILE_SIZE";

        public final static String OPT_CHECKOUT_TICKET = "OPT_CHECKOUT_TICKET";

        public final static String OPT_CHECKOUT_ACTION = "OPT_CHECKOUT_ACTION";

        public final static String OPT_CHECKIN_TICKET = "OPT_CHECKIN_TICKET";

        public final static String OPT_CHECKIN_ACTION = "OPT_CHECKIN_ACTION";

        public final static String OPT_SPOOLER = "OPT_SPOOLER";

        public static final String OPT_TREATMENT = "OPT_treatment_id";

        public static final String OPTION_PROGRAM = "NemoOptions.xml";

        private NEMOConstants() {
        }
    }

    /**
     * Helper class to load properties from a MQL program.
     */
    public final static class FPDMProgramProperties extends Properties {

        /**
         * the serialUID
         */
        private static final long serialVersionUID = -8413799723350782129L;

        /**
         * Load from program.
         * @param programName
         *            the program name
         * @throws MatrixException
         */
        public void loadFromProgram(Context context, String programName) throws MatrixException {
            if (programName != null) {
                ProgramList programList = Program.getPrograms(context);
                Iterator<?> iterator = programList.iterator();
                Program foundProgram = null;
                while (iterator.hasNext() && foundProgram == null) {
                    Program tmpProgram = (Program) iterator.next();
                    if (programName.equalsIgnoreCase(tmpProgram.getName())) {
                        foundProgram = tmpProgram;
                    }
                }
                if (foundProgram != null) {
                    foundProgram.open(context);
                    InputStream stream = null;
                    try {
                        stream = IOUtils.toInputStream(foundProgram.getCode(context));
                        load(stream);
                    } catch (IOException e) {
                        throw new MatrixException(e);
                    } finally {
                        foundProgram.close(context);
                        if (stream != null) {
                            try {
                                stream.close();
                            } catch (IOException e) {
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Prevent new instance.
     */
    private NEMOUtils_mxJPO() {
        System.out.println("NEMOUtils init...");
    }

    private static void initProperties(Context context) throws MatrixException {
        if (programProperties == null) {
            FPDMProgramProperties pPropTemp = new FPDMProgramProperties();
            try {
                pPropTemp.loadFromProgram(context, PROGRAM_NAME);
            } catch (MatrixException me) {
                logger.error("An error occurs while loading properties ", me);
                throw me;
            }
            // do not re-update if already updated by another script (static variable)
            if (programProperties == null) {
                programProperties = pPropTemp;
            }
            if (logger.isDebugEnabled()) {
                logger.debug("InitProperties - programProperties <" + programProperties + ">");
            }
        }
    }

    public static String[] getChains(Context context, String sCADObjectType) throws MatrixException {
        return getChains(context, sCADObjectType, "");
    }

    public static String[] getChains(Context context, String sCADObjectType, String sOrganizationName) throws MatrixException {
        System.out.println("getChains() - sCADObjectType<" + sCADObjectType + "> sOrganizationName<" + sOrganizationName + ">");

        initProperties(context);

        StringBuilder key = new StringBuilder();
        if (sOrganizationName != null) {
            key.append(sOrganizationName.toUpperCase());
            key.append("_");
        }
        if (sCADObjectType != null) {
            key.append(sCADObjectType.toLowerCase().replaceAll("\\s", ""));
            key.append("_");
        }
        key.append("chains");

        String value = programProperties.getProperty(key.toString(), "");
        // String[] aValue = ("".equals(value)) ? (new String[0]) : (value.split("[,]"));
        String[] aValue = { "catia5_dwg" };// TODO What is that ?
        System.out.println("getChains() - result <" + Arrays.toString(aValue) + ">");

        return aValue;
    }

    /**
     * Return the NEMO Conversion Queue domain object
     * @param context
     *            the eMatrix <code>Context</code> object
     * @return
     * @throws MatrixException
     */
    public static DomainObject getNemoConversionQueueObject(Context context) throws MatrixException {
        BusinessObject boNConversion = new BusinessObject(TYPE_NEMO_CONVERSION_QUEUE, NEMO_CONVERSION_QUEUE_OBJECT_NAME, NEMO_CONVERSION_QUEUE_OBJECT_REVISION, "");
        DomainObject dobNemoConversion = DomainObject.newInstance(context, boNConversion);
        return dobNemoConversion;

    }

    public static void updateOrInsertOptionsProgram(Context context, String sOptions) throws FrameworkException {
        if (sOptions != null && sOptions.length() > 0) {
            try {
                ContextUtil.pushContext(context, PERSON_USER_AGENT, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                String sEncoded = Base64.encode(sOptions.getBytes());
                if (sEncoded.contains("\"")) {
                    logger.error("Base64 encoded string contains the \" char : " + sEncoded);
                }
                String sMqlListCommand = new StringBuilder("list program \"").append(NEMOConstants.OPTION_PROGRAM).append("\";").toString();
                String sPrograms = MqlUtil.mqlCommand(context, sMqlListCommand);
                if (sPrograms == null || sPrograms.length() == 0) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("try to add Options in DB");
                    }
                    String sMqlAddCommand = new StringBuilder("add program \"").append(NEMOConstants.OPTION_PROGRAM).append("\" code \"").append(sEncoded).append("\";").toString();
                    MqlUtil.mqlCommand(context, sMqlAddCommand);
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("try to update Options in DB");
                    }
                    String sMqlUpdateCommand = new StringBuilder("modify program \"").append(NEMOConstants.OPTION_PROGRAM).append("\" code \"").append(sEncoded).append("\";").toString();
                    MqlUtil.mqlCommand(context, sMqlUpdateCommand);
                }
            } finally {
                ContextUtil.popContext(context);
            }
        }
    }

    public static String getOptionsFromDB(Context context) throws FrameworkException {
        try {
            ContextUtil.pushContext(context, PERSON_USER_AGENT, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            String sMqlListCommand = new StringBuilder("list program \"").append(NEMOConstants.OPTION_PROGRAM).append("\" select code dump;").toString();
            String sOptions = MqlUtil.mqlCommand(context, sMqlListCommand);
            if (sOptions != null && sOptions.length() > 0) {
                sOptions = new String(Base64.decode(sOptions));
            }
            if (logger.isDebugEnabled()) {
                logger.debug("getOptionFromDB : " + sOptions);
            }
            return sOptions;
        } finally {
            ContextUtil.popContext(context);
        }
    }

    public static void retrieveAllOptions(ArrayList<Element> alAllOptions, Element elCurrent, Namespace nsef) {
        List<Element> lAllChilds = elCurrent.getChildren();
        List<Element> lOptions = elCurrent.getChildren(XML_OPTION, nsef);
        alAllOptions.addAll(lOptions);
        lAllChilds.removeAll(lOptions);
        for (Element elChild : lAllChilds) {
            retrieveAllOptions(alAllOptions, elChild, nsef);
        }
    }

    public static boolean isObjectConnectedToNEMOWithAction(Context context, DomainObject doCADObject, String sNemoAction) throws FrameworkException {
        StringList slConnectedToNemo = doCADObject.getInfoList(context, SELECT_NEMO_ACTION_FROM_CAD_OBJECT);
        if (logger.isDebugEnabled()) {
            logger.debug("isObjectConnectedToNEMOWithAction() --> slConnectedToNemo:<" + slConnectedToNemo + ">");
        }
        if (slConnectedToNemo.contains(sNemoAction)) { // check if any Viewable object exist
            if (logger.isDebugEnabled()) {
                logger.debug("isObjectConnectedToNEMOWithAction() - CAD object already connected to NEMO Queue.");
            }
            return true;
        }
        return false;
    }

    public static ArrayList<String> getViewablesForChain(Context context, DomainObject doCADObject, String sChain) throws MatrixException {
        StringBuilder sbSelectViewableForChain = new StringBuilder("from[").append(RELATIONSHIP_VIEWABLE).append("|to.attribute[").append(ATTRIBUTE_FPDM_NEMO_CHAIN).append("]=='").append(sChain)
                .append("'].to.id");
        Map<String, Map<String, Object>> mViewableInfos = NEMOUtils_mxJPO.getSelectBusinessObjectData(context, new String[] { doCADObject.getObjectId() },
                new StringList(sbSelectViewableForChain.toString()));
        Map<?, ?> mViewableInfo = mViewableInfos.get(doCADObject.getObjectId());
        ArrayList<String> alViewableIds = null;
        if (mViewableInfo != null) {
            alViewableIds = NEMOUtils_mxJPO.getListOfValues(mViewableInfo.get(SELECT_VIEWABLE_ID));
        }
        if (alViewableIds == null) {
            alViewableIds = new ArrayList<String>();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("hasViewableForChain() --> slViewableIds:<" + alViewableIds + ">");
        }
        return alViewableIds;
    }

    public static void updateViewableAttributes(Context context, DomainObject doViewable, AttributeList alAttributes) throws MatrixException {
        doViewable.setAttributeValues(context, alAttributes);
    }

    public static DomainObject createViewable(Context context, DomainObject doCADObject, String sChain, String sViewableType, String sViewablePolicy) throws FrameworkException {
        SelectList slSelectStmts = new SelectList();
        slSelectStmts.addElement(DomainConstants.SELECT_TYPE);
        slSelectStmts.addElement(DomainConstants.SELECT_NAME);
        slSelectStmts.addElement(DomainConstants.SELECT_REVISION);

        Map<?, ?> mCADObjectInfo = doCADObject.getInfo(context, slSelectStmts);
        String sCADObjectType = (String) mCADObjectInfo.get(DomainConstants.SELECT_TYPE);
        String sCADObjectName = (String) mCADObjectInfo.get(DomainConstants.SELECT_NAME);
        String sCADObjectRevision = (String) mCADObjectInfo.get(DomainConstants.SELECT_REVISION);

        // construct Viewable name and revision
        StringBuilder sbViewableName = new StringBuilder(sCADObjectType).append("_").append(sCADObjectName).append("_").append(sCADObjectRevision).append("_").append(sChain);
        String sViewableName = sbViewableName.toString().replaceAll(" ", "");
        String sViewableRevision = NEMOUtils_mxJPO.getNumFromLetter(sCADObjectRevision);
        if (logger.isDebugEnabled()) {
            logger.debug("createViewable() - sViewableName = <" + sViewableName + "> sViewableRevision = <" + sViewableRevision + ">");
        }
        // create new Viewable
        DomainObject dobRelatedViewable = new DomainObject();
        ContextUtil.pushContext(context, PERSON_USER_AGENT, null, null);
        try {
            dobRelatedViewable.createAndConnect(context, sViewableType, sViewableName, sViewableRevision, sViewablePolicy, null, RELATIONSHIP_VIEWABLE, doCADObject, true);
        } finally {
            ContextUtil.popContext(context);
        }
        return dobRelatedViewable;
    }

    public static void connectToNemo(Context context, DomainObject doToConvert, DomainObject doNemoQueue, String sChain) throws FrameworkException {
        DomainRelationship doRel = DomainRelationship.connect(context, doNemoQueue, RELATIONSHIP_NEMO_CONVERSION_QUEUE, doToConvert);
        String converter = context.getUser();
        doRel.setAttributeValue(context, ATTRIBUTE_CONVERSION_REQUESTER, converter);
        doRel.setAttributeValue(context, ATTRIBUTE_NEMO_ACTION, sChain);
    }

    public static String getRealOpenPDMUser(Context context, DomainObject doToConvert) {
        String sRealOpenPDMUser = null;
        try {
            HashMap<?, ?> hmHistory = UINavigatorUtil.getHistoryData(context, doToConvert.getObjectId());
            ArrayList<String> alActions = NEMOUtils_mxJPO.getListOfValues(hmHistory.get(HISTORY_ACTION));
            ArrayList<String> alUsers = NEMOUtils_mxJPO.getListOfValues(hmHistory.get(HISTORY_USER));
            if (logger.isDebugEnabled()) {
                logger.debug("alActions : " + alActions);
                logger.debug("alUsers : " + alUsers);
            }
            int i = 0;
            for (String sAction : alActions) {
                if (HISTORY_CHECKIN.equals(sAction)) {
                    if (alUsers.size() > i) {
                        String sTempUser = alUsers.get(i);
                        if (sTempUser != null && sTempUser.contains(OPEN_PDM_LOCAL_USERS_START)) {
                            int iIndex = sTempUser.indexOf(OPEN_PDM_LOCAL_USERS_START);
                            if (iIndex != 0) {
                                sTempUser = sTempUser.substring(iIndex);
                            }
                            sRealOpenPDMUser = sTempUser;
                        }
                    }
                }
                i++;
            }
        } catch (MatrixException e) {
            logger.error("Unable to retrieve Real Open PDM user", e);
        }
        return sRealOpenPDMUser;
    }

    /**
     * Fetch all nemo configurations attributes. Sorted by Nemo Configuration Name
     * @param context
     *            ematrix context
     * @param attributes
     *            Attributes to fetch. If null, return all non standard attributes. It empty, return empty Map. If non empty, return only needed informations specified in the select list
     * @return Map with key the name of the nemo configuration and a map with configuration informations
     * @throws Exception
     *             if error occurs
     */
    public static Map<String, Map<String, String>> getNemoConfigurationAttributes(Context context, SelectList sSelect) throws Exception {
        logger.debug("getNemoConfigurationAttributes() - Start");
        Map<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();
        SelectList selectList = new SelectList();
        selectList.addId();
        selectList.addName();
        if (sSelect != null) {
            selectList.addAll(sSelect);
        }
        String sName;
        MapList mlNemoConfigurations = NEMOUtils_mxJPO.findObjects(context, TYPE_NEMO_CONVERSION_CONFIGURATION, "*", "*", selectList, "", (short) 0);
        for (Iterator<?> iterator = mlNemoConfigurations.iterator(); iterator.hasNext();) {
            Map mNemoConfiguration = (Map) iterator.next();
            sName = (String) mNemoConfiguration.get(DomainConstants.SELECT_NAME);
            Map attributes = DomainObject.newInstance(context, (String) mNemoConfiguration.get(DomainConstants.SELECT_ID)).getAttributeMap(context, false);
            Map tmp = new HashMap(attributes);
            if (sSelect != null) {
                tmp.keySet().retainAll(sSelect);
            }
            mNemoConfiguration.putAll(tmp);
            map.put(sName, mNemoConfiguration);
        }
        return map;
    }

    /**
     * Fetch all nemo configurations
     * @param context
     *            ematrix context
     * @return Map with key the name of the nemo configuration and a map with configuration informations
     * @throws Exception
     *             if error occurs
     */
    public static Map<String, Map<String, String>> getNemoConfigurations(Context context) throws Exception {
        SelectList selectList = new SelectList();
        selectList.addId();
        selectList.addName();
        selectList.addType();
        selectList.addRevision();
        return getNemoConfigurationAttributes(context, selectList);
    }

    /**
     * Check that the object with the given id exists. Does not check type.
     * @param context
     *            the ematrix context
     * @param mMethodArgs
     *            method arguments
     * @return <code>true</code> if the object exists, <code>false</code> otherwise
     * @throws Exception
     *             if error occurs
     */
    public static boolean checkExistence(Context context, String sObjectId) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("checkExistence() - sObjectId = <" + sObjectId + ">");
        }
        DomainObject doCADObject = new DomainObject(sObjectId);
        boolean exists = doCADObject.exists(context);
        if (logger.isDebugEnabled()) {
            logger.debug("checkExistence() - exists = <" + exists + ">");
        }
        return exists;
    }

    /**
     * Construct the content of the mail sent when there is an alert on Nemo Conversion Queue. It will sumup how many connection we have from each type
     * @param hmQC
     *            : HashMap<String,Integer>, contains Type of connection as key and Number of this type of connection as value for QC
     * @param hmNemo
     *            : HashMap<String,Integer>, contains Type of connection as key and Number of this type of connection as value for Nemo
     * @return
     */
    public static String constructAlertMessage(HashMap<String, Object> hmQC, HashMap<String, Object> hmNemo, HashMap<String, Object> hmChoosen, HashMap<String, Object> hmCancel, String sText) {
        StringBuilder sbMailMessage = new StringBuilder();
        sbMailMessage.append(sText);
        sbMailMessage.append("======= Quality Checker =======\n");
        Iterator<Entry<String, Object>> hmItr = hmQC.entrySet().iterator();
        while (hmItr.hasNext()) {
            Entry<String, Object> tmpEntry = hmItr.next();
            sbMailMessage.append(tmpEntry.getKey());
            sbMailMessage.append(": ");
            sbMailMessage.append(tmpEntry.getValue());
            sbMailMessage.append("\n");
        }
        sbMailMessage.append("\n======= Nemo Conversion =======\n");
        hmItr = hmNemo.entrySet().iterator();
        while (hmItr.hasNext()) {
            Entry<String, Object> tmpEntry = hmItr.next();
            sbMailMessage.append(tmpEntry.getKey());
            sbMailMessage.append(": ");
            sbMailMessage.append(tmpEntry.getValue());
            sbMailMessage.append("\n");
        }
        sbMailMessage.append("\n======= Choosen Conversion =======\n");
        hmItr = hmChoosen.entrySet().iterator();
        while (hmItr.hasNext()) {
            Entry<String, Object> tmpEntry = hmItr.next();
            sbMailMessage.append(tmpEntry.getKey());
            sbMailMessage.append(": ");
            sbMailMessage.append(tmpEntry.getValue());
            sbMailMessage.append("\n");
        }
        sbMailMessage.append("\n======= Cancelled Conversion =======\n");
        hmItr = hmCancel.entrySet().iterator();
        while (hmItr.hasNext()) {
            Entry<String, Object> tmpEntry = hmItr.next();
            sbMailMessage.append(tmpEntry.getKey());
            sbMailMessage.append(": ");
            sbMailMessage.append(tmpEntry.getValue());
            sbMailMessage.append("\n");
        }
        return sbMailMessage.toString();
    }

    /**
     * This method will fill the message in mail.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param hmResultSummary
     *            contain information to write in mail
     * @param sJobErrorMessage
     *            Error message
     * @param sProgramName
     *            the program name
     * @param sStartDate
     *            the formatted string of start date
     * @param sCompletionStatus
     *            the completion status
     * @return String: the message send by mail
     * @throws Exception
     */
    public static String constructMailMessage(Context context, Map<String, Map<String, String>> hmResultSummary, String sJobErrorMessage, String sProgramName, String sStartDate,
            String sCompletionStatus) throws Exception {
        StringBuilder sbReturnMessage = new StringBuilder("");
        sbReturnMessage.append(sProgramName).append(" started on ").append(sStartDate).append(" has been finished with the result: ").append(sCompletionStatus).append("\n\n");
        for (Entry<String, Map<String, String>> eEntry : hmResultSummary.entrySet()) {
            Map<String, String> hmObject = eEntry.getValue();

            boolean bProcessSucceeded = "true".equals(hmObject.get("Process Succeeded")) ? true : false;
            sbReturnMessage.append("Object : (").append(hmObject.get("Object Type")).append(") (").append(hmObject.get("Object Name")).append(") (").append(hmObject.get("Object Revision"))
                    .append(")");
            sbReturnMessage.append(" **** ").append((bProcessSucceeded ? "Success\n" : "FAILED !\n"));

            for (Entry<String, String> eObjectEntry : hmObject.entrySet()) {
                String sKey = eObjectEntry.getKey();
                String sValue = eObjectEntry.getValue();

                sbReturnMessage.append("\t").append(sKey).append(" : ").append(sValue).append("\n");
            }
        }
        if (!"".equals(sJobErrorMessage))
            sbReturnMessage.append("Error message:").append(sJobErrorMessage).append("\n");
        return sbReturnMessage.toString();
    }

    /**
     * This method will be used by all JPO for batches. This will construct the mail subject.
     * @param context
     *            Matrix user context
     * @param sTitle
     *            String for job title
     * @param myJob
     *            Job used for process
     * @return String for mail subject
     * @throws Exception
     */
    public static String constructMailSubject(Context context, String sTitle, String sState) throws Exception { // TODO !!!
        /*
         * String sEnvironment = FPDMOrganizationName_mxJPO.getEnvironment(context); String sReturnSubject = "NOTIF: " + sEnvironment + "_" + FPDMOrganizationName_mxJPO.getName(context) + "-" +
         * InetAddress.getLocalHost().getHostName() + ": " + sTitle + " " + sState; return sReturnSubject;
         */
        return "";
    }

    /**
     * get the name of the status attribute regarding the king of process
     * @param sNemoAction
     *            the code of the action : "QC" for Quality Check
     * @throws Exception
     */
    public static String getStatusAttribute(String sNemoAction) {
        return getLabelByNemoAction(sNemoAction, ATTRIBUTE_QC_CURRENT_ACTION, ATTRIBUTE_CONVERSION_STATUS, ATTRIBUTE_REL_CONVERSION_STATUS);
    }

    /**
     * get the Status Label regarding the king of process
     * @param sNemoAction
     *            the code of the action : "QC" for Quality Check
     * @throws Exception
     */
    public static String getStatusLabel(String sNemoAction) {
        return getLabelByNemoAction(sNemoAction, "Object Quality Checker Status", "Object Conversion Status", "Object Conversion Status");
    }

    /**
     * get the Identifier label regarding the king of process
     * @param sNemoAction
     *            the code of the action : "QC" for Quality Check
     * @throws Exception
     */
    public static String getIdentifierLabel(String sNemoAction) {
        return getLabelByNemoAction(sNemoAction, "Object Quality Checker Identifier", "Object Conversion Identifier", "Object Conversion Identifier");
    }

    /**
     * get the process label regarding the king of process
     * @param sNemoAction
     *            the code of the action : "QC" for Quality Check
     * @throws Exception
     */
    public static String getProcessLabel(String sNemoAction) {
        return getLabelByNemoAction(sNemoAction, "SEND QUALITY CHECK", "SEND CONVERSION", "SEND CONVERSION");
    }

    /**
     * Return the first label if the given nemo action is equals to QC, otherwise the second label
     * @param sNemoAction
     *            the nemo action
     * @param ifQC
     *            the first label
     * @param ifNotQC
     *            the second label
     * @return the label to use
     */
    private static String getLabelByNemoAction(String sNemoAction, String ifQC, String ifAuto, String sIfOnDemand) {
        if (sNemoAction != null && sNemoAction.equals(RANGE_NEMO_ACTION_QC)) {
            return ifQC;
        } else if (sNemoAction != null && sNemoAction.length() > 0) {
            return sIfOnDemand;
        }
        return ifAuto;
    }

    /**
     * Check if there is at least one allowed file on the CAD Object
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sObjectId
     *            CAD Object id
     * @param alSupportedFileFormat
     *            allowed format files
     * @return boolean (true/false)
     * @throws Exception
     *             if error occurs
     */
    public static boolean hasSupportedFiles(Context context, String sObjectId, Collection<String> alSupportedFileFormat) throws Exception {
        boolean bHasFile = false;
        if (logger.isDebugEnabled()) {
            logger.debug("hasSupportedFiles() - sObjectId <" + sObjectId + "> alSupportedFileFormat <" + alSupportedFileFormat + ">");
        }
        try {
            DomainObject dobCADObject = DomainObject.newInstance(context, sObjectId);
            String sCurrent = dobCADObject.getInfo(context, DomainConstants.SELECT_CURRENT);
            String sObjectType = dobCADObject.getInfo(context, DomainConstants.SELECT_TYPE);
            String sCADStatePreliminary = getCADStatePreliminary(context);
            // If the state of the CAD Object is Preliminary, we switch to the Versioned CAD Object
            if (!TYPE_FPDM_CAD_COMPONENT.equals(sObjectType)) {
                if (sCADStatePreliminary.equals(sCurrent)) {
                    String sVersionedId = dobCADObject.getInfo(context, "from[" + RELATIONSHIP_ACTIVE_VERSION + "].to." + DomainConstants.SELECT_ID);
                    dobCADObject.setId(sVersionedId);
                }
            }
            StringList slFileNames = NEMOUtils_mxJPO.toStringList(dobCADObject.getInfoList(context, DomainConstants.SELECT_FILE_NAME));
            StringList slFileFormats = NEMOUtils_mxJPO.toStringList(dobCADObject.getInfoList(context, DomainConstants.SELECT_FILE_FORMAT));
            Iterator<?> itNames = slFileNames.iterator();
            Iterator<?> itFormats = slFileFormats.iterator();
            while (!bHasFile && itFormats.hasNext()) {
                String sFileFormat = (String) itFormats.next();
                if (logger.isDebugEnabled()) {
                    String sFileName = (String) itNames.next();
                    logger.debug("hasSupportedFiles() - sFileFormat = <" + sFileFormat + ">");
                    logger.debug("hasSupportedFiles() - sFileName = <" + sFileName + ">");
                }
                bHasFile = alSupportedFileFormat.contains(sFileFormat);
            }
        } catch (Exception e) {
            logger.error("Error in hasSupportedFiles", e);
            throw e;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("hasSupportedFiles() - returns " + bHasFile);
        }
        return bHasFile;
    }

    /**
     * Returns the total file size of the given cad object
     * @param context
     *            ematrix context
     * @param sObjectId
     *            the cad object id
     * @param alSupportedFileFormat
     *            wanted format size
     * @return the total file size
     * @throws Exception
     *             if error occurs
     */
    public static long getFileSizeFromCADObject(Context context, String sObjectId, Collection<String> alSupportedFileFormat) throws Exception {
        DomainObject dobCADObject = DomainObject.newInstance(context, sObjectId);
        return getFileSizeFromCADObject(context, dobCADObject, alSupportedFileFormat);
    }

    /**
     * Returns the total file size of the given cad object
     * @param context
     *            ematrix context
     * @param dobCADObject
     *            the cad object
     * @param alSupportedFileFormat
     *            wanted format size
     * @return the total file size
     * @throws Exception
     *             if error occurs
     */
    public static long getFileSizeFromCADObject(Context context, DomainObject dobCADObject, Collection<String> alSupportedFileFormat) throws Exception {
        StringList slFileFormats = NEMOUtils_mxJPO.toStringList(dobCADObject.getInfoList(context, DomainConstants.SELECT_FILE_FORMAT));
        ArrayList<String> alPerformedFormat = new ArrayList<String>();
        long lTotalFileSize = 0;
        for (Iterator<?> iterator = slFileFormats.iterator(); iterator.hasNext();) {
            String sFormat = (String) iterator.next();
            if (alSupportedFileFormat.contains(sFormat)) {
                // alPerformedFormat prevents duplication of format[sFormat].file.size in case you have two file checked in with same format.
                // Hence the getInfoList to retrieve the sFileSizeList.
                if (!alPerformedFormat.contains(sFormat)) {
                    StringList slFileSizeList = dobCADObject.getInfoList(context, "format[" + sFormat + "].file.size");
                    for (Iterator<?> itr = slFileSizeList.iterator(); itr.hasNext();) {
                        try {
                            long lFileSize = Long.parseLong((String) itr.next());
                            lTotalFileSize += lFileSize;
                        } catch (NumberFormatException e) {
                            // Shouldn't happen as we don't have non numeric value with format.file.size in database.
                            logger.error(e.getMessage(), e);
                        }
                    }
                    alPerformedFormat.add(sFormat);
                }
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("getFilesSizeFromCADObject() - Object = <" + dobCADObject.toString() + "> size = <" + lTotalFileSize + ">");
        }
        return lTotalFileSize;
    }

    /**
     * Return the CAD Object file name
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sObjectId
     *            CAD Object id
     * @param alSupportedFileFormat
     *            supported file formats
     * @return : String (filename)
     * @throws Exception
     */
    public static String getCADObjectFileName(Context context, String sObjectId, Collection<String> alSupportedFileFormat) throws Exception {
        String sCADObjectFileName = "";
        try {

            DomainObject dobCADObject = DomainObject.newInstance(context, sObjectId);

            String sCurrent = dobCADObject.getInfo(context, DomainConstants.SELECT_CURRENT);
            String sCADStatePreliminary = getCADStatePreliminary(context);
            // If the state of the CAD Object is Preliminary, we switch to the Versioned CAD Object
            if (sCADStatePreliminary.equals(sCurrent)) {
                String sVersionedId = dobCADObject.getInfo(context, "from[" + RELATIONSHIP_ACTIVE_VERSION + "].to." + DomainConstants.SELECT_ID);
                if (sVersionedId != null && !"".equals(sVersionedId)) {
                    dobCADObject.setId(sVersionedId);
                }
            }

            StringList slFileNames = NEMOUtils_mxJPO.toStringList(dobCADObject.getInfoList(context, DomainConstants.SELECT_FILE_NAME));
            StringList slFileFormats = NEMOUtils_mxJPO.toStringList(dobCADObject.getInfoList(context, DomainConstants.SELECT_FILE_FORMAT));
            Iterator<?> itNames = slFileNames.iterator();
            Iterator<?> itFormats = slFileFormats.iterator();
            while (itFormats.hasNext()) {
                String sFileName = (String) itNames.next();
                String sFileFormat = (String) itFormats.next();
                if (alSupportedFileFormat.contains(sFileFormat)) {
                    sCADObjectFileName = sFileName;
                }
            }

        } catch (Exception e) {
            logger.error("Error in getCADObjectFileName", e);
            throw e;
        }

        return sCADObjectFileName;
    }

    /**
     * Returns the preliminary state used for CAD Object
     * @param context
     *            ematrix context
     * @return the preliminary state
     */
    public static String getCADStatePreliminary(Context context) {
        if (sCADStatePreliminary == null) {
            sCADStatePreliminary = PropertyUtil.getSchemaProperty(context, "policy", POLICY_DESIGN_POLICY, "state_Preliminary");
        }
        return sCADStatePreliminary;
    }

    /**
     * Specify if the given object type can be assigned as 2D
     * @param objectType
     * @return <code>true</code> if is a CATDrawing or UGDrawing, <code>false</code> otherwise.
     */
    public static boolean is2d(String objectType) {
        return (TYPE_CATDRAWING.equals(objectType) || TYPE_UG_DRAWING.equals(objectType));
    }

    public static Collection<BusinessObjectProxy> getFileFromCADObject(Context context, DomainObject dobCADObject, Collection<String> alSupportedFileFormat) throws Exception {
        ArrayList<BusinessObjectProxy> alFiles = new ArrayList<BusinessObjectProxy>();
        try {
            String sObjectId = dobCADObject.getInfo(context, DomainConstants.SELECT_ID);
            StringList slFileNames = NEMOUtils_mxJPO.toStringList(dobCADObject.getInfoList(context, DomainConstants.SELECT_FILE_NAME));
            StringList slFileFormats = NEMOUtils_mxJPO.toStringList(dobCADObject.getInfoList(context, DomainConstants.SELECT_FILE_FORMAT));
            Iterator<?> itFiles = slFileNames.iterator();
            Iterator<?> itFormats = slFileFormats.iterator();
            String sFileName = "";
            String sFileFormat = "";
            for (; itFiles.hasNext();) {
                sFileName = (String) itFiles.next();
                sFileFormat = (String) itFormats.next();
                if (alSupportedFileFormat.contains(sFileFormat)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("getFileFromCADObject() - sFileName = " + sFileName + " :sFileFormat = " + sFileFormat);
                    }
                    BusinessObjectProxy bop = new BusinessObjectProxy(sObjectId, sFileFormat, sFileName);
                    if (!alFiles.contains(bop)) {
                        alFiles.add(bop);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error in getFileFromCADObject", e);
            throw e;
        }
        return alFiles;
    }

    /**
     * Initialize parameters to reset the NEMO check-in for the selected CAD Object (already converted but the check-in failed)<br>
     * CAD Objects with the Conversion Status value contains "CHECKIN REQUEST"
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sCADObjectId
     *            CAD object Id
     * @param sRelationshipId
     *            Relationship id between CAD object and NEMO conversion queue object
     * @throws Exception
     */
    public static void relaunchNemoCheckinForSelectedCADObjects(Context context, String sCADObjectId, String sRelationshipId) throws Exception {

        DomainObject dobCADObject = DomainObject.newInstance(context);
        dobCADObject.setId(sCADObjectId);
        if (!sRelationshipId.equals("")) {
            DomainRelationship sRelObject = new DomainRelationship(sRelationshipId);

            String sConversionResult = sRelObject.getAttributeValue(context, ATTRIBUTE_CONVERSION_RESULT);
            if (sConversionResult != null && sConversionResult.startsWith("Checkin Failed")) {
                sRelObject.setAttributeValue(context, ATTRIBUTE_CONVERSION_RESULT, "");
            }

            String sNemoAction = sRelObject.getAttributeValue(context, ATTRIBUTE_NEMO_ACTION);
            if (isOnDemandConversion(sNemoAction)) {
                String sConversionStatus = sRelObject.getAttributeValue(context, ATTRIBUTE_REL_CONVERSION_STATUS);
                if (RANGE_CONVERSION_STATUS_CHECKIN_REQUEST.equals(sConversionStatus)) {
                    sRelObject.setAttributeValue(context, getStatusAttribute(sNemoAction), RANGE_CONVERSION_STATUS_DONE);
                }
            } else {
                String sConversionStatus = dobCADObject.getAttributeValue(context, getStatusAttribute(sNemoAction));
                if (RANGE_CONVERSION_STATUS_CHECKIN_REQUEST.equals(sConversionStatus)) {
                    dobCADObject.setAttributeValue(context, getStatusAttribute(sNemoAction), RANGE_CONVERSION_STATUS_DONE);
                }
            }
        }
    }

    public static boolean isOnDemandConversion(String sNemoAction) {
        if (sNemoAction.length() > 0 && !sNemoAction.equals(RANGE_NEMO_ACTION_QC)) {
            return true;
        }
        return false;
    }

    /**
     * Save the conversion status at the ATTRIBUTE_CONVERSION_STATUS attribute of the CAD Object and the CAD Definition
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sCADDef
     *            CAD Definition name + revision
     * @param sNemoObjectId
     *            Nemo Object id
     * @param sCADObjectId
     *            CAD Object id
     * @param sConversionStatus
     *            Conversion status
     * @throws Exception
     */
    public static void saveConversionStatus(Context context, String sNemoObjectId, String sCADObjectId, String sConversionStatus, String sConversionMessage) throws Exception {
        boolean bContextPushed = false;
        try {

            // We modify the attribute in User Agent since it's a process that change them
            /*
             * FPDMJobAdministration_mxJPO jobadmin = new FPDMJobAdministration_mxJPO(context, null); jobadmin.pushContextForJob(context); bContextPushed = true;
             */

            // Set CAD object conversion status
            DomainObject dobCADObj = new DomainObject(sCADObjectId);
            String sConversionIdentifiant = null;
            String sRelId = null;
            String sNemoAction = null;

            StringList slObjectSelect = new StringList();

            StringList slRelSelect = new StringList();
            slRelSelect.addElement(DomainRelationship.SELECT_ID);
            slRelSelect.addElement(SELECT_NEMO_ACTION);
            slRelSelect.addElement("attribute[" + ATTRIBUTE_CONVERSION_IDENTIFIANT + "]");
            slRelSelect.addElement("attribute[" + ATTRIBUTE_CONVERSION_RESULT + "]");

            String sRelPattern = RELATIONSHIP_NEMO_CONVERSION_QUEUE;

            MapList mlRelations = dobCADObj.getRelatedObjects(context, sRelPattern, "*", slObjectSelect, slRelSelect, true, false, (short) 1, "", "", 0);

            Map<?, ?> mRelation = null;
            for (Iterator<?> ite = mlRelations.iterator(); ite.hasNext();) {
                // abort nemo conversion in progress
                mRelation = (Map<?, ?>) ite.next();
                sConversionIdentifiant = (String) mRelation.get("attribute[" + ATTRIBUTE_CONVERSION_IDENTIFIANT + "]");
                sNemoAction = (String) mRelation.get("attribute[" + ATTRIBUTE_NEMO_ACTION + "]");
                sRelId = (String) mRelation.get(DomainRelationship.SELECT_ID);
                if (logger.isDebugEnabled()) {
                    logger.debug("sConversionIdentifiant = <" + sConversionIdentifiant + ">");
                    logger.debug("sNemoObjectId = <" + sNemoObjectId + ">");
                    logger.debug("sConversionStatus received = <" + sConversionStatus + ">");
                }
                if (!RANGE_NEMO_ACTION_CANCEL.equals(sNemoAction) && sConversionIdentifiant != null && sConversionIdentifiant.equals(sNemoObjectId)) {
                    DomainRelationship sRelObject = new DomainRelationship(sRelId);
                    if (sConversionMessage == null || sConversionMessage.equals("")) {
                        // if (sConversionStatus.contains("Checkout error")) {
                        // sRelObject.setAttributeValue(context, FPDMConstants.ATTRIBUTE_CONVERSION_IDENTIFIANT, "");
                        // }
                        if (RANGE_CONVERSION_STATUS_FAILED.equals(sConversionStatus) && RANGE_NEMO_ACTION_QC.equals(sNemoAction)) {
                            Map<String, String> mUpdate = new HashMap<String, String>();
                            mUpdate.put(ATTRIBUTE_QC_RESULT, RANGE_QC_RESULT_ERROR);
                            NEMOUtils_mxJPO.updateViewableInfos(context, sCADObjectId, mUpdate, null, sNemoAction);
                        }
                        if (!isOnDemandConversion(sNemoAction)) {
                            dobCADObj.setAttributeValue(context, getStatusAttribute(sNemoAction), sConversionStatus);
                        } else {
                            sRelObject.setAttributeValue(context, ATTRIBUTE_REL_CONVERSION_STATUS, sConversionStatus);
                        }
                        if (logger.isDebugEnabled()) {
                            logger.debug("CAD Object status set to <" + sConversionStatus + ">");
                        }
                    } else {
                        sRelObject.setAttributeValue(context, ATTRIBUTE_CONVERSION_RESULT, sConversionMessage);
                        if (logger.isDebugEnabled()) {
                            logger.debug("CAD Object result set to <" + sConversionMessage + ">");
                        }
                    }
                }
            }

        } catch (RuntimeException e) {
            logger.error("RuntimeException in saveConversionStatus()\n", e);
            throw e;
        } catch (Exception e) {
            logger.error("Error in saveConversionStatus()\n", e);
            throw e;
        } finally {
            if (bContextPushed) {
                ContextUtil.popContext(context);
            }
        }

    }

    public static String getStringOptions(HashMap<String, Object> hmChainOptions) {
        StringBuilder sbToReturn = new StringBuilder("");
        int i = 1;
        Set<Entry<String, Object>> sEntries = hmChainOptions.entrySet();
        for (Entry<String, Object> eEntry : sEntries) {
            String sOptionName = eEntry.getKey();
            checkOptionContent(sOptionName);
            sbToReturn.append(sOptionName).append(OPTION_VALUE_SEPARATOR);
            Object oValue = eEntry.getValue();
            if (oValue instanceof String) {
                String sValue = (String) oValue;
                checkOptionContent(sValue);
                sbToReturn.append(sValue);
            } else if (oValue instanceof String[]) {
                int j = 1;
                String[] saValues = (String[]) oValue;
                for (String sValue : saValues) {
                    checkOptionContent(sValue);
                    sbToReturn.append(sValue);
                    if (j < saValues.length) {
                        sbToReturn.append(VALUES_SEPARATOR);
                    }
                    j++;
                }
            }
            if (i < sEntries.size()) {
                sbToReturn.append(OPTIONS_SEPARATOR);
            }
            i++;
        }
        return sbToReturn.toString();
    }

    private static void checkOptionContent(String sOption) {
        if (logger.isDebugEnabled()) {
            if (sOption.contains(VALUES_SEPARATOR) || sOption.contains(OPTIONS_SEPARATOR) || sOption.contains(OPTION_VALUE_SEPARATOR)) {
                logger.error(" XXXXXXXXXXXXXXXXXX A separator is contained in " + sOption);
            }
        }
    }

    public static HashMap<String, Object> getConvOptionsFromString(String sOptions) {
        HashMap<String, Object> hmSelectedOptions = new HashMap<String, Object>();
        String[] saOptions = sOptions.split(OPTIONS_SEPARATOR_TOSPLIT);
        for (String sOption : saOptions) {
            if (sOption.length() > 0) {
                String[] saOptNameValue = sOption.split(OPTION_VALUE_SEPARATOR, -1);
                if (saOptNameValue != null && saOptNameValue.length == 2) {
                    String sOptName = saOptNameValue[0];
                    String sOptValues = saOptNameValue[1];
                    String[] saValues = sOptValues.split(VALUES_SEPARATOR, -1);
                    if (saValues.length == 1) {
                        hmSelectedOptions.put(sOptName, saValues[0]);
                    } else {
                        hmSelectedOptions.put(sOptName, saValues);
                    }
                } else {
                    logger.error("Option has not the expected length : " + saOptNameValue.length + " on String option : " + sOptions);
                }
            }
        }
        return hmSelectedOptions;
    }

    /**
     * Return the CAD Object file format
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sObjectId
     *            CAD Object id
     * @param sCADObjectFileName
     *            file name
     * @return : String (filename)
     * @throws Exception
     */
    public static String getCADObjectFileFormat(Context context, String sObjectId, Collection<String> alSupportedFileFormat) throws Exception {
        String sCADObjectFileFormat = "";
        try {

            DomainObject dobCADObject = DomainObject.newInstance(context, sObjectId);

            String sCurrent = dobCADObject.getInfo(context, DomainConstants.SELECT_CURRENT);
            String sCADStatePreliminary = getCADStatePreliminary(context);
            // If the state of the CAD Object is Preliminary, we switch to the Versioned CAD Object
            if (sCADStatePreliminary.equals(sCurrent)) {
                String sVersionedId = dobCADObject.getInfo(context, "from[" + RELATIONSHIP_ACTIVE_VERSION + "].to." + DomainConstants.SELECT_ID);
                if (sVersionedId != null && !"".equals(sVersionedId)) {
                    dobCADObject.setId(sVersionedId);
                }
            }

            StringList slFileFormats = NEMOUtils_mxJPO.toStringList(dobCADObject.getInfoList(context, DomainConstants.SELECT_FILE_FORMAT));
            String sFileFormat = null;
            Iterator<?> itFormats = slFileFormats.iterator();
            while (itFormats.hasNext()) {
                sFileFormat = (String) itFormats.next();
                if (alSupportedFileFormat.contains(sFileFormat)) {
                    sCADObjectFileFormat = sFileFormat;
                }
            }

        } catch (Exception e) {
            logger.error("Error in getCADObjectFileFormat", e);
            throw e;
        }

        return sCADObjectFileFormat;
    }

    public static MapList findObjects(Context context, String sType, String sName, String sRev, StringList resultSelects, String sWhereExp, short limit) throws Exception {
        MapList mlResult = new MapList();

        ContextUtil.startTransaction(context, true);
        try {
            Query query = new Query();
            query.setBusinessObjectType(sType);
            query.setBusinessObjectName(sName);
            query.setBusinessObjectRevision(sRev);
            query.setVaultPattern(VAULT_PRODUCTION);

            if (sWhereExp != null && !"".equals(sWhereExp)) {
                query.setWhereExpression(sWhereExp);
            }
            if (resultSelects == null) {
                resultSelects = DomainConstants.EMPTY_STRINGLIST;
            }

            QueryIterator queryIterator = query.getIterator(context, resultSelects, limit);

            while (queryIterator.hasNext()) {
                BusinessObjectWithSelect busWithSelect = queryIterator.next();

                Map<String, Object> mObj = new HashMap<String, Object>();

                for (Iterator iterator = resultSelects.iterator(); iterator.hasNext();) {
                    String sKey = (String) iterator.next();
                    mObj.put(sKey, busWithSelect.getSelectData(sKey));
                }

                mlResult.add(mObj);
            }

            queryIterator.close();
            ContextUtil.commitTransaction(context);
        } catch (Exception e) {
            logger.error("Error in getRelatedObjects()\n", e);
            ContextUtil.abortTransaction(context);
            throw e;
        }

        return mlResult;
    }

    /**
     * Transform an Object (String or StringList) into a string list
     * @param obj
     *            Object to transform
     * @return StringList : The string list
     * @throws Exception
     */
    public static StringList toStringList(Object obj) throws Exception {
        StringList slObj = new StringList();
        try {
            if (obj != null) {
                if (obj instanceof String) {
                    if (((String) obj).length() > 0) {
                        slObj.addElement((String) obj);
                    }
                } else {
                    slObj = (StringList) obj;
                }
            }
        } catch (Exception e) {
            logger.error("Error in toStringList() - obj=<" + obj + ">\n", e);
            throw e;
        }
        return slObj;
    }

    /**
     * To retrieve multiple information on multiple object. No need to instantiate BusinessObject or DomainObject
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param saIds
     *            The list of id
     * @param slSelect
     *            The information to select
     * @return a Map, where keys are the ids and values are a map. In the value Maps, keys are the names of selected information and values are values of selected information (values are Objects, it
     *         can be String or ArrayList<String>) ex : Map<"12345.12345.12345.12345", Map<"current", "Preliminary">> ex : Map<"12345.12345.12345.12345", Map<"state",
     *         {"Preliminary","Review","Release","Canceled"}>>
     * @throws MatrixException
     */
    public static Map<String, Map<String, Object>> getSelectBusinessObjectData(Context context, String[] saIds, StringList slSelect) throws MatrixException {
        Map<String, Map<String, Object>> mObjectsInformation = new HashMap<String, Map<String, Object>>();
        if (saIds != null && saIds.length > 0) {
            BusinessObjectWithSelectList bowsList = BusinessObject.getSelectBusinessObjectData(context, saIds, slSelect);
            if (bowsList != null && saIds.length == bowsList.size()) {
                int k = 0;
                for (Object oBusSelect : bowsList) {
                    if (oBusSelect instanceof BusinessObjectWithSelect) {
                        String sId = saIds[k];
                        Map<String, Object> mValue = new HashMap<String, Object>();
                        BusinessObjectWithSelect bowSelect = (BusinessObjectWithSelect) oBusSelect;
                        populateReturnMap(mValue, bowSelect.getSelectKeys(), bowSelect.getSelectValues());
                        mObjectsInformation.put(sId, mValue);
                    }
                    k++;
                }
            }
        }
        return mObjectsInformation;
    }

    /**
     * Parse keys and values to populate the Map of values
     * @param mReturn
     * @param vKeys
     * @param vValues
     */
    private static void populateReturnMap(Map<String, Object> mReturn, Vector<?> vKeys, Vector<?> vValues) {
        int index = 0;
        if (vKeys.size() > vValues.size()) {
            System.err.println("there is more keys : " + vKeys.size() + " than values : " + vValues.size());
        }
        for (Object oSelect : vKeys) {
            if (oSelect instanceof String) {
                String sData = (String) vValues.get(index);
                Object oData = getDataStringOrList(sData);
                Object oExisting = mReturn.get((String) oSelect);
                if (oExisting != null) {
                    ArrayList<String> alExistingValues = null;
                    if (oExisting instanceof String) {
                        alExistingValues = new ArrayList<String>();
                        alExistingValues.add((String) oExisting);
                    } else if (oExisting instanceof ArrayList) {
                        alExistingValues = (ArrayList<String>) oExisting;
                    }
                    if (alExistingValues != null) {
                        if (oData instanceof String) {
                            alExistingValues.add((String) oData);
                        } else if (oData instanceof ArrayList) {
                            alExistingValues.addAll((ArrayList<String>) oData);
                        }
                    }
                    mReturn.put((String) oSelect, alExistingValues);
                } else {
                    mReturn.put((String) oSelect, oData);
                }
            }
            index++;
        }
    }

    /**
     * Standard process to retrieve a single value or a list of values
     * @param sData
     *            The data directly returned by getSelectBusinessObjectData
     * @return A String or an ArrayList<String>
     */
    private static Object getDataStringOrList(String sData) {
        if (sData != null && sData.indexOf("\7") >= 0) {
            // return it as an ArrayList
            ArrayList<String> alValues = new ArrayList<String>();
            int prev = 0;
            int curr;
            while ((curr = sData.indexOf("\7", prev)) >= 0) {
                alValues.add(sData.substring(prev, curr));
                prev = curr + 1;
            }
            alValues.add((prev > 0) ? sData.substring(prev) : sData);
            return alValues;
        } else {
            // return it as a String
            return sData;
        }
    }

    /**
     * Transform an object to an ArrayList<String> if object is one of String, ArrayList<String> or StringList
     * @param oValue
     * @return an ArrayList<String>
     */
    public static ArrayList<String> getListOfValues(Object oValue) {
        if (oValue instanceof ArrayList) {
            return (ArrayList<String>) oValue;
        } else if (oValue instanceof String) {
            ArrayList<String> alValues = new ArrayList<String>();
            alValues.add((String) oValue);
            return alValues;
        } else if (oValue instanceof StringList) {
            ArrayList<String> alValues = new ArrayList<String>();
            for (Iterator<?> ite = ((StringList) oValue).iterator(); ite.hasNext();) {
                String sValue = (String) ite.next();
                alValues.add(sValue);
            }
            return alValues;
        }
        return new ArrayList<String>();
    }

    /**
     * Convert character on two digit number<br>
     * (A --> 0, B --> 1, ...)
     * @param letter
     * @return number
     */
    public static String getNumFromLetter(String letter) {
        if (letter.matches("[0-9]*")) {
            return letter;
        }
        return String.format("%d", getNum(letter));
    }

    /**
     * Convert String to integer (A --> 0, B --> 1, ... , AA --> 26, AB --> 27, ... , ZZ --> 701, ...)
     * @param letters
     * @return
     */
    public static int getNum(String letters) {
        if (letters.length() == 1) {
            return Integer.valueOf(letters.toUpperCase().charAt(0)) - 65;
        }
        int charValue = Integer.valueOf(letters.toUpperCase().charAt(0)) - 65;
        int weightedValue = (int) Math.pow(26, letters.length() - 1);
        String newLetters = letters.substring(1);
        int newValue = (charValue + 1) * weightedValue;
        return newValue + getNum(newLetters);

    }

    /**
     * update attributes on the Viewable
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sCADObjectId
     *            the id of the CADObject. Can be empty if the sViewableId is filled.
     * @param mInfos
     *            the map containing the information to update
     * @param sViewableId
     *            the id of the Viewable to update. Can be empty if sCADObjectId and sNemoAction are filled.
     * @param sNemoAction
     *            the code of the action : "QC" for Quality Check. Can be empty if the sViewableId is filled.
     * @return
     * @throws Exception
     */
    public static void updateViewableInfos(Context context, String sCADObjectId, Map mInfos, String sViewableId, String sNemoAction) throws Exception {
        try {
            String sTypePattern = null;
            if (sViewableId == null || sViewableId.equals("")) {
                if (sCADObjectId != null && !sCADObjectId.equals("")) {
                    if (sNemoAction != null && sNemoAction.equals("QC")) {
                        sTypePattern = NEMOUtils_mxJPO.TYPE_QC_VIEWABLE;
                    } else {
                        sTypePattern = NEMOUtils_mxJPO.TYPE_VIEWABLE;
                    }
                    String sCommand = "print bus \"" + sCADObjectId + "\" select from[" + RELATIONSHIP_VIEWABLE + "].to[" + sTypePattern + "].id dump";
                    sViewableId = MqlUtil.mqlCommand(context, sCommand);
                } else {
                    logger.error("sCADObjectId or sViewableId must be filled");
                }
            }
            if (sViewableId == null || sViewableId.equals("")) {
                throw new Exception("No viewable to update has been found");
            }
            if (logger.isDebugEnabled()) {
                logger.debug("sViewableId : " + sViewableId);
                logger.debug("mInfos : " + mInfos);
            }
            DomainObject doViewable = new DomainObject(sViewableId);
            doViewable.setAttributeValues(context, mInfos);

        } catch (Exception e) {
            logger.error("Error in updateViewableInfos()\n", e);
            throw e;
        }
    }

    /**
     * getStringFromMap : Retrieve a String value from a Map, if the Object is not an instance of String, the value is returned to null
     * @param mInfos
     *            : the Map to find the information
     * @param sKey
     *            : The key to retrieve
     * @return : null if no value found or if the value was not a String.
     */
    public static String getStringFromMap(Map<?, ?> mInfos, String sKey) {
        if (mInfos != null && sKey != null) {
            Object oTemp = mInfos.get(sKey);
            if (oTemp instanceof String) {
                return (String) oTemp;
            }
        }
        return null;
    }

    /**
     * Transform or cast an object to a String. Object can be String, ArrayList<String> or StringList<String> else method will an empty String. If multiple values, retrieves the first value.
     * @param oValue
     * @return a String
     */
    public static String getSingleValue(Object oValue) {
        if (oValue instanceof String) {
            return (String) oValue;
        } else if (oValue instanceof ArrayList) {
            ArrayList<?> alValues = (ArrayList<?>) oValue;
            if (alValues != null && alValues.size() > 0) {
                Object oSingleValue = alValues.get(0);
                if (oSingleValue instanceof String) {
                    return (String) oSingleValue;
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } else if (oValue instanceof StringList) {
            Iterator<?> ite = ((StringList) oValue).iterator();
            if (ite.hasNext()) {
                Object oSingleValue = ite.next();
                if (oSingleValue instanceof String) {
                    return (String) oSingleValue;
                }
            }
            return null;
        }
        return null;
    }

    /**
     * To retrieve single information (with possibly multiple values) on a single object. No need to instantiate BusinessObject or DomainObject ONLY ONE KEY IS EXPECTED IN RETURN. Ex : Select
     * from[*].to.id may return multiple keys (from[EBOM].to.id=XXX, from[EBOM history].to.id==XXX), in that case, the method will only return the values of the first key. Ex : Select revisions.id may
     * return multiple keys (revisions[01].id=XXX, revisions[02].id==XXX ...), in that case, the method will only return the id of the revision 01.
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param sId
     *            The id
     * @param sSelect
     *            The information to select
     * @return an Object (a String or an ArrayList of String)
     * @throws MatrixException
     */
    public static Object getSelectBusinessObjectData(Context context, String sId, String sSelect) throws MatrixException {
        if (sId != null && sId.length() > 0) {
            BusinessObjectWithSelectList bowsList = BusinessObject.getSelectBusinessObjectData(context, new String[] { sId }, new StringList(sSelect));
            if (bowsList != null && bowsList.size() == 1) {
                for (Object oBusSelect : bowsList) {
                    if (oBusSelect instanceof BusinessObjectWithSelect) {
                        BusinessObjectWithSelect bowSelect = (BusinessObjectWithSelect) oBusSelect;
                        Vector<?> vValues = bowSelect.getSelectValues();
                        if (vValues.size() > 0) {
                            String sData = (String) vValues.get(0);
                            return getDataStringOrList(sData);
                        }
                    }
                }
            }
        }
        return null;
    }

    public static boolean isNullOrEmpty(String var) {
        return isNullOrEmpty(var, true);
    }

    public static boolean isNullOrEmpty(String var, boolean trim) {
        if (trim) {
            if (var == null || var.trim().length() == 0) {
                return true;
            }
            return false;
        } else {
            if (var == null || var.length() == 0) {
                return true;
            }
            return false;
        }
    }

    /**
     * get attributes values from the related Viewable object
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param doCADObject
     *            CAD object
     * @param sTypePattern
     * @param slSelect
     *            Select list
     * @param sNemoAction
     *            the code of the action : "QC" for Quality Check
     * @return
     * @throws Exception
     */
    public static Map getViewableInfos(Context context, DomainObject doCADObject, String sTypePattern, StringList slSelect) throws Exception {
        Map<String, Object> mReturn = null;

        try {
            String sRelPattern = RELATIONSHIP_VIEWABLE;
            sRelPattern += "," + RELATIONSHIP_VERSIONOF;
            sRelPattern += "," + RELATIONSHIP_ACTIVE_VERSION;
            SelectList sl = new SelectList();
            sl.addAll(slSelect);
            sl.addElement(DomainConstants.SELECT_TYPE);
            MapList mlViewableObject = doCADObject.getRelatedObjects(context, sRelPattern, "*", sl, null, false, true, (short) 2, "", "", 0, new Pattern(sTypePattern), null, null);
            if (logger.isDebugEnabled()) {
                logger.debug("getViewableInfos() - mlViewableObject = <" + mlViewableObject + ">");
            }

            if (mlViewableObject.size() > 0) {
                mReturn = new HashMap<String, Object>();

                Set<Map.Entry<String, Object>> mySet = ((Map<String, Object>) mlViewableObject.get(0)).entrySet();
                String sKey = "";
                for (Map.Entry<String, Object> me : mySet) {
                    sKey = me.getKey();
                    if (slSelect.contains(sKey)) {
                        mReturn.put(sKey, me.getValue());
                    }
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("getViewableInfos() - mReturn = <" + mReturn + ">");
                }
            }
        } catch (FrameworkException e) {
            logger.error("Error in getViewableInfos()\n", e);
            throw e;
        }
        return mReturn;
    }

    /**
     * get attributes values from the related Viewable object
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param doCADObject
     *            CAD object
     * @param sTypePattern
     * @param slSelect
     *            Select list
     * @param sNemoAction
     *            the code of the action : "QC" for Quality Check
     * @return
     * @throws Exception
     */
    public static MapList getViewablesInfos(Context context, DomainObject doCADObject, String sTypePattern, StringList slSelect) throws Exception {
        MapList mlViewableObject = new MapList();
        try {
            String sRelPattern = RELATIONSHIP_VIEWABLE;
            sRelPattern += "," + RELATIONSHIP_ACTIVE_VERSION;
            SelectList sl = new SelectList();
            sl.addAll(slSelect);
            sl.addElement(DomainConstants.SELECT_TYPE);
            sl.addElement(DomainConstants.SELECT_NAME);

            MapList mlObjects = doCADObject.getRelatedObjects(context, sRelPattern, "*", sl, null, false, true, (short) 2, "", "", 0, new Pattern(sTypePattern), null, null);
            if (logger.isDebugEnabled()) {
                logger.debug("getViewablesInfos() - mlViewableObject = <" + mlViewableObject + ">");
            }

            StringList slRelatedViewableObjects = new StringList();
            String sViewableType = null;
            String sViewableName = null;
            for (Iterator iterator = mlObjects.iterator(); iterator.hasNext();) {
                Map mViewableInfo = (Map) iterator.next();

                sViewableType = (String) mViewableInfo.get(DomainConstants.SELECT_TYPE);
                sViewableName = (String) mViewableInfo.get(DomainConstants.SELECT_NAME);
                if (logger.isDebugEnabled()) {
                    logger.debug("getViewablesInfos() - sViewableType = <" + sViewableType + "> sViewableName = <" + sViewableName + ">");
                }
                if (!slRelatedViewableObjects.contains(sViewableType + "_" + sViewableName)) { // keep only one revision of Viewable
                    slRelatedViewableObjects.addElement(sViewableType + "_" + sViewableName);

                    mlViewableObject.add(mViewableInfo);
                }
            }
            if (logger.isDebugEnabled()) {
                logger.debug("getViewablesInfos() - mlViewableObject = <" + mlViewableObject + ">");
            }
        } catch (FrameworkException e) {
            logger.error("Error in getViewablesInfos()\n", e);
            throw e;
        }
        return mlViewableObject;

    }
}
