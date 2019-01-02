
/*
 * ${CLASSNAME}.java.
 *
 * Copyright (c) 1992-2011 Dassault Systemes.
 *
 * All Rights Reserved.
 *
 */
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.AttributeType;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.Context;
import matrix.db.MatrixWriter;
import matrix.util.MatrixException;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADConfigObjectLoader;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADUtil;
import com.matrixone.apps.domain.DomainConstants;

public abstract class SLWMigrator_mxJPO {
    protected static final String SLW_GCO_TYPE = "SolidWorksInteg-GlobalConfig";

    protected static final String SLW_GCO_NAME = "SWNewArch";

    protected static final String DRAWING_LIKE_CADTYPES = "drawing"; // Enter comma-separated list here if custom drawing types are used.

    protected final boolean simulationMode = false;

    // Do we need to update this number?
    protected static final String BUILD_NUMBER = "Wk22_05.29";

    protected BufferedWriter writer = null;

    protected PrintWriter iefLog = null;

    private PrintWriter failedIDFile = null;

    protected long migrationStartTime = 0L;

    protected MCADMxUtil _mxUtil = null;

    protected String documentDirectory = "";

    protected String range = "";

    protected MCADServerGeneralUtil _serverGeneralUtil = null;

    protected MCADGlobalConfigObject _gco = null;

    protected MCADServerResourceBundle _res = null;

    protected IEFGlobalCache _cache = null;

    protected AttributeType cadType_AttributeType = null;

    protected AttributeType title_AttributeType = null;

    protected AttributeType forceNewVersion_AttributeType = null;

    protected AttributeType ebomExposition_AttributeType = null;

    protected Set<String> cadTypesToSkip_Cache = null;

    protected Map<String, List<AttributeType>> commonTypeAttributes_Cache = null;

    protected Map<String, List<String>> typeAttributeSelectsMap_Cache = null;

    protected Map<String, String> attributeNameAttributeSelectsMap_Cache = null;

    protected Map<String, String> attributeNameDefaultValueMap_Cache = null;

    protected Set<String> attrCopyExclusionList = null;

    protected static final String COMPONENT_INSTANCE_CADTYPE = "componentInstance";

    protected static final String ASSEMBLY_INSTANCE_CADTYPE = "assemblyInstance";

    protected static final String COMPONENT_FAMILY_CADTYPE = "componentFamily";

    protected static final String ASSEMBLY_FAMILY_CADTYPE = "assemblyFamily";

    protected String componentInstanceEnoviaType = null;

    protected String assemblyInstanceEnoviaType = null;

    protected String componentFamilyEnoviaType = null;

    protected String assemblyFamilyEnoviaType = null;

    protected String MIG_PARAM_MIGRATION_MESSAGE = "";

    protected String MIG_PARAM_ID_FILE_PREFIX = ""; // ID_FILE_PREFIX or ID_FILE_TITLE_PREFIX

    protected String MIG_PARAM_LOG_FILE_NAME = "";

    protected String MIG_PARAM_FAILED_ID_FILE_PREFIX = "";

    protected static final String SELECT_EXPR_ID = "id";

    protected static final String SELECT_EXPR_REVISIONS = "revisions";

    protected static final String SELECT_EXPR_REVISION_LOCKED = "revisions.locked";

    protected static final String SELECT_EXPR_REVISION_CURRENT = "revisions.current";

    protected static final String SELECT_EXPR_REVISION_NAME = "revisions.name";

    protected static final String SELECT_EXPR_REVISION_STATES = "revisions.state";

    protected static final String SELECT_EXPR_REVISION_ID = "revisions.id";

    protected static final String SELECT_EXPR_REVISION_VAULT = "revisions.vault";

    protected static final String SELECT_EXPR_REVISION_TYPE = "revisions.type";

    protected static final String SELECT_EXPR_REVISION_POLICY = "revisions.policy";

    protected static final String SELECT_EXPR_REVISION_OWNER = "revisions.owner";

    protected static final String SELECT_EXPR_REVISION_DESCRIPTION = "revisions.description";

    protected static String SELECT_EXPR_ON_MAJOR = "";

    protected static String SELECT_EXPR_ON_ACTIVE_VERSION = "";

    protected static String SELECT_EXPR_ON_FINALIZED_VERSION = "";

    protected static String SELECT_EXPR_ACTIVE_VERSION_ID = "";

    protected static String SELECT_EXPR_REVISIONS_ATTR_CADTYPE = "";

    protected static String SELECT_EXPR_ATTR_CADTYPE = "";

    protected static String SELECT_EXPR_MINOR_REVISIONS = "";

    protected static String SELECT_EXPR_MINOR_IDS = "";

    protected static String SELECT_EXPR_MINOR_POLICY = "";

    protected static String SELECT_EXPR_MINOR_OWNER = "";

    protected static String SELECT_EXPR_MINOR_TYPE = "";

    protected static String SELECT_EXPR_MINOR_CADTYPE = "";

    protected static String SELECT_EXPR_MINOR_VAULT = "";

    protected static String SELECT_EXPR_MINOR_NAME = "";

    protected static String SELECT_EXPR_MINOR_DESCRIPTION = "";

    protected static String ATTR_CADTYPE = "";

    protected static String ATTR_FORCE_NEW_VERSION = "";

    protected static String RELATIONSHIP_INSTANCE_OF = "";

    protected static String RELATIONSHIP_VERSION_OF = "";

    protected static String RELATIONSHIP_ACTIVE_VERSION = "";

    protected static String RELATIONSHIP_LATEST_VERSION = "";

    protected static String RELATIONSHIP_FINALIZED = "";

    protected static String TYPE_CADDRAWING = "";

    protected static String SELECT_EXPR_ATTR_TITLE = "";

    protected static String ATTR_TITLE = "";

    protected static String ATTR_ISFINALIZED = "";

    protected static String SELECT_EXPR_FINALIZED_VERSION_ID = "";

    // protected static String SELECT_EXPR_IS_ATTR_BASED_FINALIZED_VERSION = "";
    protected static String SELECT_EXPR_REVISIONS_ATTR_TITLE = "";

    protected static String SELECT_EXPR_MINOR_TITLE = "";

    protected static String ATTR_IEF_EBOMEXPOSITION_MODE = "";

    public SLWMigrator_mxJPO(Context context, String[] args) throws Exception {
        cadTypesToSkip_Cache = new HashSet<String>();
        typeAttributeSelectsMap_Cache = new HashMap<String, List<String>>();
        commonTypeAttributes_Cache = new HashMap<String, List<AttributeType>>();
        attributeNameAttributeSelectsMap_Cache = new HashMap<String, String>();
        attributeNameDefaultValueMap_Cache = new HashMap<String, String>();

        writer = new BufferedWriter(new MatrixWriter(context));
        _res = new MCADServerResourceBundle("en-US");
        _cache = new IEFGlobalCache();
        _mxUtil = new MCADMxUtil(context, _res, _cache);

        getGlobalConfigObject(context);

        _serverGeneralUtil = new MCADServerGeneralUtil(context, _gco, _res, _cache);

        componentInstanceEnoviaType = (String) _gco.getMappedBusTypes(COMPONENT_INSTANCE_CADTYPE).elementAt(0);
        assemblyInstanceEnoviaType = (String) _gco.getMappedBusTypes(ASSEMBLY_INSTANCE_CADTYPE).elementAt(0);
        componentFamilyEnoviaType = (String) _gco.getMappedBusTypes(COMPONENT_FAMILY_CADTYPE).elementAt(0);
        assemblyFamilyEnoviaType = (String) _gco.getMappedBusTypes(ASSEMBLY_FAMILY_CADTYPE).elementAt(0);

        setMigrationParams();

        ATTR_CADTYPE = MCADMxUtil.getActualNameForAEFData(context, "attribute_CADType");
        ATTR_ISFINALIZED = MCADMxUtil.getActualNameForAEFData(context, "attribute_IsFinalized");
        ATTR_FORCE_NEW_VERSION = MCADMxUtil.getActualNameForAEFData(context, "attribute_ModifiedinMatrix");
        ATTR_TITLE = MCADMxUtil.getActualNameForAEFData(context, "attribute_Title");
        RELATIONSHIP_INSTANCE_OF = MCADMxUtil.getActualNameForAEFData(context, "relationship_InstanceOf");
        RELATIONSHIP_VERSION_OF = MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");
        RELATIONSHIP_ACTIVE_VERSION = MCADMxUtil.getActualNameForAEFData(context, "relationship_ActiveVersion");
        RELATIONSHIP_LATEST_VERSION = MCADMxUtil.getActualNameForAEFData(context, "relationship_LatestVersion");
        RELATIONSHIP_FINALIZED = MCADMxUtil.getActualNameForAEFData(context, "relationship_Finalized");
        RELATIONSHIP_ACTIVE_VERSION = MCADMxUtil.getActualNameForAEFData(context, "relationship_ActiveVersion");
        TYPE_CADDRAWING = MCADMxUtil.getActualNameForAEFData(context, "type_CADDrawing");
        ATTR_IEF_EBOMEXPOSITION_MODE = MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-EBOMExpositionMode");

        SELECT_EXPR_ON_MAJOR = "to[" + RELATIONSHIP_ACTIVE_VERSION + "].from";
        SELECT_EXPR_ON_ACTIVE_VERSION = "from[" + RELATIONSHIP_ACTIVE_VERSION + "].to";
        SELECT_EXPR_ON_FINALIZED_VERSION = "to[" + RELATIONSHIP_FINALIZED + "].from";
        SELECT_EXPR_ACTIVE_VERSION_ID = "revisions." + SELECT_EXPR_ON_ACTIVE_VERSION + "." + SELECT_EXPR_ID;
        SELECT_EXPR_ATTR_CADTYPE = "attribute[" + ATTR_CADTYPE + "]";
        SELECT_EXPR_REVISIONS_ATTR_CADTYPE = "revisions." + SELECT_EXPR_ATTR_CADTYPE;
        SELECT_EXPR_ATTR_TITLE = "attribute[" + ATTR_TITLE + "]";
        SELECT_EXPR_REVISIONS_ATTR_CADTYPE = "revisions." + SELECT_EXPR_ATTR_CADTYPE;
        SELECT_EXPR_FINALIZED_VERSION_ID = "revisions.to[" + RELATIONSHIP_FINALIZED + "].from.id";
        // SELECT_EXPR_IS_ATTR_BASED_FINALIZED_VERSION = "revisions.from[" + RELATIONSHIP_VERSION_OF + "].attribute[" + ATTR_ISFINALIZED + "]";
        SELECT_EXPR_REVISIONS_ATTR_TITLE = "revisions." + SELECT_EXPR_ATTR_TITLE;

        SELECT_EXPR_MINOR_REVISIONS = SELECT_EXPR_ON_ACTIVE_VERSION + ".revisions";
        SELECT_EXPR_MINOR_IDS = SELECT_EXPR_MINOR_REVISIONS + "." + DomainConstants.SELECT_ID;
        SELECT_EXPR_MINOR_POLICY = SELECT_EXPR_MINOR_REVISIONS + "." + DomainConstants.SELECT_POLICY;
        SELECT_EXPR_MINOR_OWNER = SELECT_EXPR_MINOR_REVISIONS + "." + DomainConstants.SELECT_OWNER;
        SELECT_EXPR_MINOR_TYPE = SELECT_EXPR_MINOR_REVISIONS + "." + DomainConstants.SELECT_TYPE;
        SELECT_EXPR_MINOR_CADTYPE = SELECT_EXPR_MINOR_REVISIONS + "." + SELECT_EXPR_ATTR_CADTYPE;
        SELECT_EXPR_MINOR_VAULT = SELECT_EXPR_MINOR_REVISIONS + "." + DomainConstants.SELECT_VAULT;
        SELECT_EXPR_MINOR_NAME = SELECT_EXPR_MINOR_REVISIONS + "." + DomainConstants.SELECT_NAME;
        SELECT_EXPR_MINOR_DESCRIPTION = SELECT_EXPR_MINOR_REVISIONS + "." + DomainConstants.SELECT_DESCRIPTION;
        SELECT_EXPR_MINOR_TITLE = SELECT_EXPR_MINOR_REVISIONS + "." + SELECT_EXPR_ATTR_TITLE;

        cadType_AttributeType = new AttributeType(ATTR_CADTYPE);
        title_AttributeType = new AttributeType(ATTR_TITLE);
        forceNewVersion_AttributeType = new AttributeType(ATTR_FORCE_NEW_VERSION);

        if (ATTR_IEF_EBOMEXPOSITION_MODE != null && !ATTR_IEF_EBOMEXPOSITION_MODE.equals("") && doesAttributeExist(context, ATTR_IEF_EBOMEXPOSITION_MODE)) {
            ebomExposition_AttributeType = new AttributeType(ATTR_IEF_EBOMEXPOSITION_MODE);
        }

        initAttrCopyExclusionList();
    }

    protected abstract int doMigration(Context context, List<String> idsList, StringList selects, String idFileName) throws Exception;

    protected abstract void setMigrationParams();

    protected abstract StringList getSelectListForQuery(Context context) throws Exception;

    private MCADGlobalConfigObject getGlobalConfigObject(Context context) throws Exception {
        if (_gco == null) {
            MCADConfigObjectLoader configLoader = new MCADConfigObjectLoader();
            _gco = configLoader.createGlobalConfigObject(context, _mxUtil, SLW_GCO_TYPE, SLW_GCO_NAME);
        }

        return _gco;
    }

    private void initAttrCopyExclusionList() {
        attrCopyExclusionList = new HashSet<String>();
        attrCopyExclusionList.add(ATTR_CADTYPE);
        attrCopyExclusionList.add(ATTR_TITLE);
    }

    protected boolean isFinalized(Context context, String policy, String finalizationState, BusinessObjectWithSelect busObjSelectData, String revSelect) {
        boolean isFinalized = false;

        StringList states = (StringList) busObjSelectData.getSelectDataList(new StringBuilder(revSelect).append(DomainConstants.SELECT_STATES).toString());
        String currentState = (String) busObjSelectData.getSelectData(new StringBuilder(revSelect).append(DomainConstants.SELECT_CURRENT).toString());

        if (states.lastIndexOf(currentState) >= states.lastIndexOf(finalizationState)) {
            isFinalized = true;
        }

        return isFinalized;
    }

    protected Vector getMappedBusTypesForCADType(String cadType) throws MCADException {
        Vector rawMappedTypes = _gco.getMappedBusTypes(cadType);

        if (rawMappedTypes == null) {
            String errorMessage = _res.getString("mcadIntegration.Server.Message.ProblemsWithGlobalConfigObject");
            MCADServerException.createException(errorMessage, null);
        }

        Vector mappedBusTypes = MCADUtil.getListOfActualTypes(rawMappedTypes);

        return mappedBusTypes;
    }

    protected boolean doesAttributeExist(Context context, String attrName) {
        String Args[] = new String[1];
        Args[0] = attrName;
        String mqlCommand = "list attribute $1";
        String result = _mxUtil.executeMQL(context, mqlCommand, Args);

        if (result.startsWith("true|")) {
            result = result.substring(5);

            if (result.equals(attrName)) {
                return true;
            }
        }

        return false;
    }

    public int mxMain(Context context, String[] args) throws Exception {
        try {
            int totalNumFailedIds = 0;

            readInputArguments(args);

            migrationStartTime = logTimeForEvent(MIG_PARAM_MIGRATION_MESSAGE + " STARTED.", -1);

            startIEFLog();

            _mxUtil.executeMQL(context, "trigger off");

            File[] idFiles = getIDFiles();

            StringList selectList = getSelectListForQuery(context);

            for (int i = 0; i < idFiles.length; ++i) {
                List<String> ids = readDataFromIDFile(idFiles[i]);
                String idFileName = idFiles[i].getName();

                if (ids != null) {
                    long queryStartTime = logTimeForEvent("Start migration for file: " + idFileName, -1);

                    totalNumFailedIds += doMigration(context, ids, selectList, idFileName);

                    logTimeForEvent("End migration for file: " + idFileName, queryStartTime);
                } else {
                    writeAllIdsToFailedFile(idFileName);
                    writeErrorToConsole("Failed to read id file: " + idFileName);
                }
            }

            logTimeForEvent(MIG_PARAM_MIGRATION_MESSAGE + " COMPLETE.", migrationStartTime);
            log("Total number of failed Ids: " + String.valueOf(totalNumFailedIds));
            writeMessageToConsole("Please review log file: " + documentDirectory + MIG_PARAM_LOG_FILE_NAME + "_" + BUILD_NUMBER + ".log", true);
        } finally {
            _mxUtil.executeMQL(context, "trigger on");
            endIEFLog();
            writer.flush();
            writer.close();
        }

        return 0;
    }

    protected List<AttributeType> getCommonAttributes(Context context, String type1, String type2) throws Exception {
        String[] typeKeys = new String[] { new StringBuilder(type1).append("|").append(type2).toString(), new StringBuilder(type2).append("|").append(type1).toString() };

        List<AttributeType> commonAttributeTypes = new ArrayList<AttributeType>();
        if (commonTypeAttributes_Cache.get(typeKeys[0]) == null && commonTypeAttributes_Cache.get(typeKeys[1]) == null) {
            List type1AttributeNames = (typeAttributeSelectsMap_Cache.get(type1) == null) ? getTypeAttributes(context, type1, null) : typeAttributeSelectsMap_Cache.get(type1);
            List type2AttributeNames = (typeAttributeSelectsMap_Cache.get(type2) == null) ? getTypeAttributes(context, type2, null) : typeAttributeSelectsMap_Cache.get(type2);

            Iterator<String> iter = type1AttributeNames.iterator();

            for (int i = 0; i < type1AttributeNames.size(); ++i) {
                String type1AttributeName = (String) type1AttributeNames.get(i);

                if (type2AttributeNames.contains(type1AttributeName)) {
                    commonAttributeTypes.add(new AttributeType(type1AttributeName));
                }
            }

            commonTypeAttributes_Cache.put(typeKeys[0], commonAttributeTypes);
            commonTypeAttributes_Cache.put(typeKeys[1], commonAttributeTypes);
        } else {
            commonAttributeTypes = (commonTypeAttributes_Cache.get(typeKeys[0]) != null) ? commonTypeAttributes_Cache.get(typeKeys[0]) : commonTypeAttributes_Cache.get(typeKeys[1]);
        }

        return commonAttributeTypes;
    }

    protected String getDefaultAttrValue(Context context, AttributeType attrType) throws MatrixException {
        String attrName = attrType.getDefaultValue(context);
        String defaultAttrValue = attributeNameDefaultValueMap_Cache.get(attrName);

        if (defaultAttrValue == null) {
            defaultAttrValue = attrType.getDefaultValue(context);
            attributeNameDefaultValueMap_Cache.put(attrName, defaultAttrValue);
        }

        return defaultAttrValue;
    }

    private List<String> getTypeAttributes(Context context, String enoviaType, StringList attributeSelectsForMappedTypes) throws Exception {
        List<String> attributeNames = new ArrayList<String>();

        if (typeAttributeSelectsMap_Cache.get(enoviaType) == null) {
            String Args[] = new String[5];
            Args[0] = enoviaType;
            Args[1] = " select ";
            Args[2] = " attribute ";
            Args[3] = "dump";
            Args[4] = " |";
            String mqlCmd = "print type $1 $2 $3 $4 $5";
            String result = _mxUtil.executeMQL(context, mqlCmd, Args);

            if (result.startsWith("true|")) {
                result = result.substring(5);
            } else {
                MCADException.createException("Failed to get attributes for type:" + enoviaType, null);
            }

            attributeNames.addAll(MCADUtil.getVectorFromString(result, "|"));

            Iterator<String> iter = attributeNames.iterator();
            while (iter.hasNext()) {
                String attributeName = iter.next();
                String attributeSelect = getAttributeSelect(attributeName);

                /*
                 * attributeSelects.add(attributeSelect); attributeSelects.add(new StringBuilder(SELECT_EXPR_REVISIONS).append(".").append(attributeSelect).toString());
                 */

                if (attributeSelectsForMappedTypes != null && !attributeSelectsForMappedTypes.contains(attributeSelect)) {
                    attributeSelectsForMappedTypes.add(attributeSelect);
                }
            }

            typeAttributeSelectsMap_Cache.put(enoviaType, attributeNames);
        }

        return attributeNames;
    }

    protected String getAttributeSelect(String attributeName) {
        String attributeSelect = attributeNameAttributeSelectsMap_Cache.get(attributeName);

        if (attributeSelect == null) {
            attributeSelect = new StringBuilder("attribute[").append(attributeName).append("]").toString();
            attributeNameAttributeSelectsMap_Cache.put(attributeName, attributeSelect);
        }

        return attributeSelect;
    }

    protected boolean isInstanceorFamilyLikeCADType(String cadType) {
        boolean skip = false;

        if (cadTypesToSkip_Cache.contains(cadType) || _gco.isTypeOfClass(cadType, "TYPE_INSTANCE_LIKE") || _gco.isTypeOfClass(cadType, "TYPE_FAMILY_LIKE")) {
            skip = true;
        }

        if (skip && !cadTypesToSkip_Cache.contains(cadType)) {
            cadTypesToSkip_Cache.add(cadType);
        }

        return skip;
    }

    protected StringList getFormatSelectsForQuery(List<String[]> inputData) throws Exception {
        String[] ids = inputData.get(0);
        String[] enoviaTypes = inputData.get(1);
        String[] cadTypes = inputData.get(2);

        StringList formatNamesForQuery = new StringList();
        Set<String> processedFormatNames = new HashSet<String>();

        for (int i = 0; i < ids.length; ++i) {
            String format = _gco.getFormatsForType(enoviaTypes[i], cadTypes[i]);

            if (!processedFormatNames.contains(format)) {
                formatNamesForQuery.add("format[" + format + "].file.name");
                processedFormatNames.add(format);
            }
        }

        return formatNamesForQuery;
    }

    protected void connectObjects(Context context, String fromObjId, String toObjId, String relationship, String failureMsg) throws MCADException {
        String cmd = new StringBuilder("connect bus ").append(fromObjId).append(" relationship \"").append(relationship).append("\" preserve to ").append(toObjId).toString();
        String result = _mxUtil.executeMQL(context, cmd);

        if (result.startsWith("false")) {
            MCADException.createException(failureMsg, null);
        }
    }

    protected void readInputArguments(String[] args) throws Exception {
        if (args.length < 2)
            throw new IllegalArgumentException("Wrong number of arguments");

        documentDirectory = args[0];
        if (!new File(documentDirectory).exists()) {
            throw new FileNotFoundException("Input directory does not exist: " + documentDirectory);
        }
        String fileSeparator = java.io.File.separator;
        if (documentDirectory != null && !documentDirectory.endsWith(fileSeparator))
            documentDirectory = documentDirectory + fileSeparator;

        range = args[1];
    }

    private File[] getIDFiles() throws Exception {
        StringTokenizer tokenizer = new StringTokenizer(range, ",");

        int minRange = Integer.parseInt(tokenizer.nextToken().trim());
        String lastToken = tokenizer.nextToken().trim();

        FilenameFilter filter = new IDFileNameFilter();
        File dir = new File(documentDirectory);

        List<String> fileNames = Arrays.asList(dir.list(filter));

        Comparator comparator = new IDFileNameComparator();

        Collections.sort(fileNames, comparator);

        int startIndex = fileNames.indexOf(MIG_PARAM_ID_FILE_PREFIX + minRange + ".txt");
        int endIndex = (!lastToken.equalsIgnoreCase("n")) ? Integer.parseInt(lastToken) : fileNames.size();

        File[] idFiles = new File[endIndex - startIndex];
        int idx = 0;
        for (int i = startIndex; i < endIndex; ++i) {
            idFiles[idx] = new File(new StringBuilder(documentDirectory).append(fileNames.get(i)).toString());
            idx++;
        }

        return idFiles;
    }

    /*
     * private List<String[]> readDataFromIDFile(File idFile) throws IOException { List<String> idsList = new ArrayList<String>(); List<String> typesList = new ArrayList<String>(); List<String>
     * cadTypesList = new ArrayList<String>();
     * 
     * BufferedReader br = null; try { String info = null; br = new BufferedReader(new FileReader(idFile)); while ((info = br.readLine()) != null) { StringTokenizer tokenizer = new
     * StringTokenizer(info, "|");
     * 
     * idsList.add(tokenizer.nextToken()); typesList.add(tokenizer.nextToken()); cadTypesList.add(tokenizer.nextToken()); } } catch(Exception ex) { ex.printStackTrace(); idsList = typesList =
     * cadTypesList = null; } finally { br.close(); }
     * 
     * List<String[]> data = null; if((idsList != null && typesList != null && cadTypesList != null )) { String[] ids = new String[idsList.size()]; idsList.toArray(ids);
     * 
     * String[] types = new String[typesList.size()]; typesList.toArray(types);
     * 
     * String[] cadTypes = new String[cadTypesList.size()]; cadTypesList.toArray(cadTypes);
     * 
     * data = new ArrayList<String[]>(); data.add(ids); data.add(types); data.add(cadTypes); }
     * 
     * return data; }
     */

    private List<String> readDataFromIDFile(File idFile) throws IOException {
        List<String> idsList = new ArrayList<String>();

        BufferedReader br = null;
        try {
            String id = null;
            br = new BufferedReader(new FileReader(idFile));
            while ((id = br.readLine()) != null) {
                // writeMessageToConsole("next id = " + id, true);
                idsList.add(id);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            idsList = null;
        } finally {
            br.close();
        }

        return idsList;
    }

    class IDFileNameComparator implements Comparator {

        public int compare(Object o1, Object o2) {
            String fileName1 = (String) o1;
            String fileName2 = (String) o2;

            int num1 = Integer.parseInt(String.valueOf(fileName1.charAt(MIG_PARAM_ID_FILE_PREFIX.length())));
            int num2 = Integer.parseInt(String.valueOf(fileName2.charAt(MIG_PARAM_ID_FILE_PREFIX.length())));

            if (num1 < num2) {
                return -1;
            } else if (num1 > num2) {
                return 1;
            }

            return 0;
        }

    }

    class IDFileNameFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            if (name.startsWith(MIG_PARAM_ID_FILE_PREFIX)) {
                return true;
            }

            return false;
        }
    }

    private void startIEFLog() throws Exception {
        try {
            iefLog = new PrintWriter(new BufferedWriter(new FileWriter(documentDirectory + MIG_PARAM_LOG_FILE_NAME + "_" + BUILD_NUMBER + ".log")));
            log("Simulation Mode: " + simulationMode);
            log("Build Number: " + BUILD_NUMBER);
        } catch (Exception e) {
            writeMessageToConsole("ERROR: Can not create log file. " + e.getMessage(), true);
        }
    }

    protected void writeFailedID(String failedId) throws Exception {
        try {
            if (failedIDFile != null) {
                failedIDFile.println(failedId);
                failedIDFile.flush();
            }
        } catch (Exception e) {
            writeMessageToConsole("ERROR: Failed to write to ID file for ID: " + failedId, true);
        }
    }

    protected void initFailedIDFile(String fileName) throws Exception {
        try {
            failedIDFile = new PrintWriter(new BufferedWriter(new FileWriter(documentDirectory + fileName)));
        } catch (Exception e) {
            writeMessageToConsole("ERROR: Can not create failed IDs file. " + e.getMessage(), true);
        }
    }

    protected void closeFailedIDFile() {
        try {
            if (failedIDFile != null) {
                failedIDFile.flush();
                failedIDFile.close();
                failedIDFile = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void writeAllIdsToFailedFile(String sourceFile) {
        try {
            String failedFileName = getFailedIDFileName(sourceFile);
            initFailedIDFile(failedFileName);
            MCADUtil.copyFile(documentDirectory + sourceFile, documentDirectory + failedFileName);
        } catch (Exception e) {

        } finally {
            closeFailedIDFile();
        }
    }

    private void endIEFLog() {
        try {
            if (iefLog != null) {
                iefLog.println();
                iefLog.println();
                iefLog.flush();
                iefLog.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void writeMessageToConsole(String message, boolean flush) throws Exception {
        writer.write(message + "\n");

        if (flush) {
            writer.flush();
        }
    }

    private void writeSuccessToConsole() throws Exception {
        writeLineToConsole();
        writeMessageToConsole("                " + MIG_PARAM_MIGRATION_MESSAGE + " is COMPLETE", false);
        writeMessageToConsole("                Time : " + (System.currentTimeMillis() - migrationStartTime) + "ms ", false);
        writeMessageToConsole("                " + MIG_PARAM_MIGRATION_MESSAGE + " is SUCCESSFUL", false);
        writeLineToConsole();
        writer.flush();
    }

    private void writeErrorToConsole(String errorMessage) throws Exception {
        writeLineToConsole();
        writeMessageToConsole(errorMessage, false);
        writeMessageToConsole(MIG_PARAM_MIGRATION_MESSAGE + "     : FAILED", false);
        writeLineToConsole();
        writer.flush();

        log(errorMessage);
    }

    private void writeLineToConsole() throws Exception {
        writeMessageToConsole("=======================================================", false);
    }

    protected void log(String message) {
        try {
            iefLog.println(message);
            iefLog.flush();
        } catch (Exception e) {
        }
    }

    protected String getFailedIDFileName(String idFileName) {
        return idFileName.replaceAll(MIG_PARAM_ID_FILE_PREFIX, MIG_PARAM_FAILED_ID_FILE_PREFIX);
    }

    protected long logTimeForEvent(String event, long startTime) throws Exception {
        long time = System.currentTimeMillis();
        StringBuilder message = new StringBuilder(event).append(" Time: ").append(time);

        if (startTime != -1) {
            long elapsedTime = time - startTime;
            message.append(" Elapsed time: ").append(String.valueOf(elapsedTime));
        }

        log(message.toString());

        writeMessageToConsole(message.toString(), true);

        return time;
    }
}
