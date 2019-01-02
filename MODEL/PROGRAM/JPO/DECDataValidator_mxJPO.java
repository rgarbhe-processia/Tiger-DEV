
/*
 ** ${CLASSNAME}
 **
 ** Copyright (c) 1993-2016 Dassault Systemes. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes. Copyright notice is precautionary only and does
 * not evidence any actual or intended publication of such program
 */
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;
import matrix.db.Context;
import matrix.db.MatrixWriter;
import matrix.util.StringList;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADConfigObjectLoader;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADUtil;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.MCADIntegration.DataValidation.*;
import com.matrixone.MCADIntegration.DataValidation.Rules.RuleValidator;
import com.matrixone.MCADIntegration.DataValidation.util.DataValidationUtil;
import com.matrixone.MCADIntegration.DataValidation.util.PageObjectUtil;

public class DECDataValidator_mxJPO {
    BufferedWriter _consoleWriter = null;

    BufferedWriter _logWriter = null;

    BufferedWriter _objIdWriter = null;

    PrintStream _errorStream = null;

    MCADGlobalConfigObject _gcoObject = null;

    private ProcessUserInputs _objProcessUsrInputs = null;

    private UserInputs _userInputs = null;

    public static int _counter = 0;

    public static int _sequence = 1;

    public static int _chunk = 1000;

    public static StringList _objectidList = null;

    public static BufferedWriter _fileWriter = null;

    public static java.io.File _oidsFile = null;

    public static String _inputDirectory = "";

    public static String _pageObjectName = "";

    public static final String _fileSeparator = java.io.File.separator;

    String _logDirectory = "";

    String _pageObjName = "";

    String _timeStamp = "";

    int _chunkSize = 1000;

    public DECDataValidator_mxJPO(Context context, String[] args) throws Exception {
        _objProcessUsrInputs = new ProcessUserInputs();
        _timeStamp = DataValidationUtil.get_timeStamp();
    }

    public int mxMain(Context context, String[] args) throws Exception {
        try {
            _userInputs = _objProcessUsrInputs.GetAndProcessUserInputs(args);

            if (_userInputs._error == true)
                return 1;

            System.out.println("\n DATA VALIDATOR launched ...\n");

            if (!context.isConnected()) {
                throw new Exception("not supported on desktop client");
            }

            long startTime = System.currentTimeMillis();

            initialize(context);

            if (_userInputs._objIdFileDir.isEmpty()) {
                writeMessageToConsole("====================================================================================");
                writeMessageToConsole(" Querying for " + _pageObjName + " Objects...\n");
                writeMessageToConsole(" (" + _chunkSize + ") Objects per File");
                writeMessageToConsole(" Writing files to: " + _logDirectory);
                writeMessageToConsole("====================================================================================\n");

                boolean bFromGCO = true;
                String typesToSearch = getTypeListFromPage(context, bFromGCO);
                getObjectIds(context, typesToSearch);

                writeMessageToConsole("====================================================================================");
                writeMessageToConsole(" Querying for Objects COMPLETED.");
                if (DECDataValidator_mxJPO._counter <= 0)
                    writeMessageToConsole("\n No objects found !!.\n [HINT: Check if the vault is valid.]");

                writeMessageToConsole("\n Time: " + (System.currentTimeMillis() - startTime) + "ms");
                if (DECDataValidator_mxJPO._counter > 0) {
                    writeMessageToConsole(" Objectid log files written to: " + DECDataValidator_mxJPO._inputDirectory);
                    writeMessageToConsole(" Step 1 of finding objects:  SUCCESS ");
                }

                writeMessageToConsole("====================================================================================\n");
            } else
                DECDataValidator_mxJPO._inputDirectory = _userInputs._objIdFileDir;

            if (_userInputs._bContinueValidate == false) {
                System.out.println("Aborting validation as you have not chosen for continuation.\n");
                return 0;
            }

            _gcoObject = DataValidationUtil.getGCOObject(context, _userInputs._gco);

            RuleValidator ruleValidator = new RuleValidator(context, _userInputs._logFileDir, _gcoObject, _logWriter, _errorStream);

            Properties properties = PageObjectUtil.readPageObject(context, "DECDataValidationConfig");

            String strRules = null;
            String strReports = null;
            if (properties.keySet() != null) {
                strRules = properties.getProperty("Validation.Rule");
                strReports = properties.getProperty("Validation.Report");
            }
            String[] ruleArray = strRules.split(",");
            String[] reportArray = strReports.split(",");

            StringList ruleList = new StringList(ruleArray);
            StringList reportList = new StringList(reportArray);

            ruleValidator.setRules(ruleList);
            ruleValidator.setReports(reportList);

            ProcessedObjects processedObjects = new ProcessedObjects();

            ValidationObjectList objectList = new ValidationObjectList(context, DECDataValidator_mxJPO._inputDirectory);
            Iterator<ValidationObject> itr = objectList.iterator();
            while (itr.hasNext()) {
                ValidationObject object = itr.next();
                if (null != object) {

                    boolean isOK = ruleValidator.validateObject(object);
                    processedObjects.setObject(object, isOK);
                }
            }

            System.out.println(" Generating reports ...");

            processedObjects.printReport(_userInputs._objIdFileDir, _userInputs._logFileDir);

            if (DECDataValidator_mxJPO._counter > 0)
                System.out.println("\nData Validation completed successfully.\n");
            processedObjects.printSummaryToConsole();

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Main exception: " + e.getMessage());
            e.printStackTrace(_errorStream);
        } finally {
            closeLogStream();
        }
        return 0;
    }

    private String getObjectIds(Context context, String typesToSearch) throws Exception {
        writeMessageToConsole("\n\n\tSearching objects for " + _pageObjName + "........\n\n");

        String vaultName = "*";

        if (null != _userInputs._vault && !_userInputs._vault.isEmpty())
            vaultName = _userInputs._vault;

        String command = "temp query bus $1 $2 $3 vault $4 where $5";
        String result = "";

        DECDataValidator_mxJPO._counter = 0;
        DECDataValidator_mxJPO._sequence = 1;
        DECDataValidator_mxJPO._pageObjectName = _pageObjName;
        DECDataValidator_mxJPO._oidsFile = null;
        DECDataValidator_mxJPO._fileWriter = null;
        DECDataValidator_mxJPO._objectidList = null;

        if (DECDataValidator_mxJPO._fileWriter == null) {
            try {
                DECDataValidator_mxJPO._inputDirectory = _logDirectory + "ObjectIds" + DECDataValidator_mxJPO._fileSeparator;
                DECDataValidator_mxJPO._oidsFile = new java.io.File(DECDataValidator_mxJPO._inputDirectory + _pageObjName + "_Objectids_1.log");
                DECDataValidator_mxJPO._oidsFile.getParentFile().mkdirs();
                DECDataValidator_mxJPO._fileWriter = new BufferedWriter(new FileWriter(DECDataValidator_mxJPO._oidsFile));
                DECDataValidator_mxJPO._chunk = _chunkSize;
                DECDataValidator_mxJPO._objectidList = new StringList(_chunkSize);
            } catch (FileNotFoundException eee) {
                throw eee;
            }
        }

        try {
            String args[] = new String[5];
            args[0] = typesToSearch;
            args[1] = "*";
            args[2] = "*";
            args[3] = vaultName;
            args[4] = "(program[DECDataValidator -method writeOID ${OBJECTID} \"${TYPE}\" \"${NAME}\" \"${REVISION}\"] == true)";

            result = MqlUtil.mqlCommand(context, command, args);
        } catch (Exception me) {
            writeMessageToConsole("[DECNewDataModelFindObjects:getObjectIds] Exception while fetching object id list : " + me.getMessage());
            writeErrorToFile("[DECNewDataModelFindObjects:getObjectIds] Exception while fetching object id list : " + me.getMessage());
            me.printStackTrace(_errorStream);
            throw me;
        }

        // Call cleanup to write the left over oids to a file
        DECDataValidator_mxJPO.cleanup();

        return result;
    }

    public boolean writeOID(Context context, String[] args) throws Exception {
        try {
            StringBuffer busIDBuf = new StringBuffer(args[0]);
            String busID = busIDBuf.toString();
            if (DataValidationUtil.isMajorObject(context, busID) || !DataValidationUtil.hasAVerionOfRel_To(context, busID)) {
                DECDataValidator_mxJPO._objectidList.add(busID);
                DECDataValidator_mxJPO._counter++;

                if (DECDataValidator_mxJPO._counter == DECDataValidator_mxJPO._chunk) {
                    DECDataValidator_mxJPO._counter = 0;
                    DECDataValidator_mxJPO._sequence++;

                    // write oid from ${CLASSNAME}._objectidList
                    for (int s = 0; s < DECDataValidator_mxJPO._objectidList.size(); s++) {
                        DECDataValidator_mxJPO._fileWriter.write((String) DECDataValidator_mxJPO._objectidList.elementAt(s));
                        DECDataValidator_mxJPO._fileWriter.newLine();
                        DECDataValidator_mxJPO._fileWriter.flush();
                    }

                    DECDataValidator_mxJPO._objectidList = new StringList();
                    DECDataValidator_mxJPO._fileWriter.close();

                    // create new file
                    DECDataValidator_mxJPO._oidsFile = new java.io.File(
                            DECDataValidator_mxJPO._inputDirectory + DECDataValidator_mxJPO._pageObjectName + "_Objectids_" + DECDataValidator_mxJPO._sequence + ".log");
                    DECDataValidator_mxJPO._fileWriter = new BufferedWriter(new FileWriter(DECDataValidator_mxJPO._oidsFile));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            e.printStackTrace(_errorStream);
        }

        return false;
    }

    public static void cleanup() throws Exception {
        try {
            if (DECDataValidator_mxJPO._objectidList != null && DECDataValidator_mxJPO._objectidList.size() > 0) {
                for (int s = 0; s < DECDataValidator_mxJPO._objectidList.size(); s++) {
                    DECDataValidator_mxJPO._fileWriter.write((String) DECDataValidator_mxJPO._objectidList.elementAt(s));
                    DECDataValidator_mxJPO._fileWriter.newLine();
                }
                DECDataValidator_mxJPO._fileWriter.close();
            } else {
                // delete the empty file created
                DECDataValidator_mxJPO._fileWriter.close();
                DECDataValidator_mxJPO._oidsFile.delete();
            }
        } catch (Exception Exp) {
            throw Exp;
        }
    }

    private String getTypeListFromPage(Context context, boolean bFromGCO) throws Exception {
        String types = null;
        if (bFromGCO) {
            Vector typeList = _gcoObject.getBusTypesForClass("TYPE_CADMODEL_LIKE");

            Iterator itr = typeList.iterator();
            while (itr.hasNext())// String type:(String)typeList)
            {
                types += itr.next();
                if (itr.hasNext())
                    types += ",";
            }
        } else
            types = getTypeListFromPage(context);
        return types;
    }

    private String getTypeListFromPage(Context context) throws Exception {
        HashSet<String> typeListSet = new HashSet<String>();
        String MQLResult = "";

        try {
            // Get Types from Page object
            String args[] = new String[1];
            args[0] = _pageObjName;

            MQLResult = MqlUtil.mqlCommand(context, "print page $1 select content dump", args);

            if (MQLResult == null || MQLResult.length() == 0) {
                writeMessageToConsole("[DECNewDataModelFindObjects:getTypeListFromPage] Page Object is blank. Please update Page Object with Type information");
                throw new Exception("Page Object is blank. Please update Page Object with Type information");
            }
        } catch (Exception exception) {
            writeMessageToConsole("[DECNewDataModelFindObjects:getTypeListFromPage] Failure in getting type list from Page obejct : " + exception.getMessage());
            exception.printStackTrace(_errorStream);
            throw exception;
        }

        byte[] bytes = MQLResult.getBytes("UTF-8");
        InputStream input = new ByteArrayInputStream(bytes);

        Properties properties = new Properties();

        properties.load(input);

        if (properties.keySet() != null) {
            Iterator keyTypeSymbolicNames = properties.values().iterator();
            while (keyTypeSymbolicNames.hasNext()) {
                String keyType = (String) keyTypeSymbolicNames.next();
                keyType = MCADMxUtil.getActualNameForAEFData(context, keyType);
                if (!typeListSet.contains(keyType))
                    typeListSet.add(keyType);
            }
        }

        /*
         * typeListSet.addAll(_gcoObject.getAllMappedTypes());
         */
        StringBuffer typeList = new StringBuffer();
        Iterator iterator = typeListSet.iterator();

        while (iterator.hasNext()) {
            typeList.append((String) iterator.next());

            if (iterator.hasNext())
                typeList.append(",");
        }
        return typeList.toString();
    }

    private void initialize(Context context) throws Exception {
        try {
            _consoleWriter = new BufferedWriter(new MatrixWriter(context));
            _logDirectory = _userInputs._logFileDir;

            // documentDirectory does not ends with "/" add it
            if (_logDirectory != null && !_logDirectory.endsWith(DECDataValidator_mxJPO._fileSeparator)) {
                _logDirectory = _logDirectory + DECDataValidator_mxJPO._fileSeparator;
            }

            _logDirectory = _logDirectory + _timeStamp + DECDataValidator_mxJPO._fileSeparator;

            File debugLogFile = new File(_logDirectory + "FindObjects_DebugLog.log");
            File errorLogFile = new File(_logDirectory + "FindObjects_ErrorLog.log");

            // Create Directory Structure
            debugLogFile.getParentFile().mkdirs();

            _logWriter = new BufferedWriter(new FileWriter(debugLogFile));
            _errorStream = new PrintStream(new FileOutputStream(errorLogFile));

            _pageObjName = _userInputs._page;

            String gcoBusId = _userInputs._gco;

            // Get Types from GCO BusTypeMapping
            MCADConfigObjectLoader configLoader = new MCADConfigObjectLoader(null);
            MCADMxUtil util = new MCADMxUtil(context, new MCADServerResourceBundle(""), new IEFGlobalCache());

            _gcoObject = configLoader.createGlobalConfigObject(context, util, gcoBusId);
            _chunkSize = _userInputs._chunk;
        } catch (Exception iExp) {
            writeMessageToConsole("[DECNewDataModelFindObjects:initialize] Exception in initialization : " + iExp.getMessage());
            writeErrorToFile("[DECNewDataModelFindObjects:initialize] Exception in initialization : " + iExp.getMessage());

            iExp.printStackTrace(_errorStream);

            closeLogStream();
            return;
        }
    }

    private void closeLogStream() throws IOException {
        try {
            if (null != _consoleWriter)
                _consoleWriter.close();

            if (null != _logWriter)
                _logWriter.close();

            if (null != _errorStream)
                _errorStream.close();
        } catch (IOException e) {
            System.out.println("Exception while closing log stream " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void writeMessageToConsole(String message) throws Exception {
        _consoleWriter.write(message + "\n");
        _consoleWriter.flush();
        writeMessageToLogFile(message);
    }

    private void writeMessageToLogFile(String message) throws Exception {
        _logWriter.write(MCADUtil.getCurrentTimeForLog() + message + "\n");
    }

    private void writeErrorToFile(String message) throws Exception {
        _errorStream.write(message.getBytes("UTF-8"));
        _errorStream.write("\n".getBytes("UTF-8"));
    }
}
