
/*
 * SLWDataModelMigratorFindObjects.java.
 *
 * Copyright (c) 1992-2011 Dassault Systemes
 *
 * All Rights Reserved. This program contains proprietary and trade secret information of MatrixOne, Inc. Copyright notice is precautionary only and does not evidence any actual or intended
 * publication of such program.
 *
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.BusinessObjectWithSelect;
import matrix.db.Context;
import matrix.db.MatrixWriter;
import matrix.db.Query;
import matrix.db.QueryIterator;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADConfigObjectLoader;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADUtil;
import com.matrixone.apps.domain.DomainConstants;

public class SLWDataModelMigratorFindObjects_mxJPO {
    private static final String SLW_GCO_TYPE = "SolidWorksInteg-GlobalConfig";

    private static final String SLW_GCO_NAME = "SWNewArch";

    private static final String DRAWING_LIKE_CADTYPES = "drawing"; // Enter comma-separated list here if custom drawing types are used.

    private BufferedWriter writer = null;

    private PrintWriter iefLog = null;

    private long startTime = 0L;

    private boolean isDebug = false;

    private MCADMxUtil _mxUtil = null;

    private String documentDirectory = "";

    private int chunkSize = 0;

    private int fileID = 0;

    private int idCounter = 0;

    private MCADServerGeneralUtil _serverGeneralUtil = null;

    private MCADGlobalConfigObject _gco = null;

    private MCADServerResourceBundle _res = null;

    private IEFGlobalCache _cache = null;

    public static final String ID = "id";

    public static final String TYPE = "type";

    public static final String INFO_DELIM = "|";

    private static final String MIGRATION_IDENTIFIER = "SLW Data Model Migration Find Objects";

    private static final String ID_FILE_PREFIX = "SLWDataModelMigration_IDs_";

    private static final String ID_FILE_TITLE_PREFIX = "SLWDataModelMigrationTitle_IDs_";

    private static final int PRIMARY_QUERY = 1;

    private static final int TYPE_CHANGE_QUERY = 2;

    private static String TYPE_CADDRAWING = "";

    public SLWDataModelMigratorFindObjects_mxJPO(Context context, String[] args) throws Exception {
        writer = new BufferedWriter(new MatrixWriter(context));
        _res = new MCADServerResourceBundle("en-US");
        _cache = new IEFGlobalCache();
        _mxUtil = new MCADMxUtil(context, _res, _cache);

        TYPE_CADDRAWING = MCADMxUtil.getActualNameForAEFData(context, "type_CADDrawing");
    }

    public int mxMain(Context context, String[] args) throws Exception {
        try {
            readInputArguments(args);

            startIEFLog();
            startTime = logTimeForEvent(MIGRATION_IDENTIFIER + " STARTED", -1);

            _mxUtil.startReadOnlyTransaction(context);

            getGlobalConfigObject(context);

            // Run the update title query
            fileID = 0;
            idCounter = 0;
            Set<String> writtenIds = new HashSet<String>();

            long updateQueryStart = logTimeForEvent("Starting update title query", startTime);

            String enoviaTypesForTitleQuery = getRelevantENOVIATypesForTitleQuery(context);
            writeMessageToConsole("Types For Title Query = " + enoviaTypesForTitleQuery);
            processQuery(context, PRIMARY_QUERY, enoviaTypesForTitleQuery, ID_FILE_TITLE_PREFIX, writtenIds);
            processQuery(context, TYPE_CHANGE_QUERY, enoviaTypesForTitleQuery, ID_FILE_TITLE_PREFIX, writtenIds);

            writeMessageToConsole("Number of items found: " + String.valueOf(idCounter));

            // Run the convert type query
            fileID = 0;
            idCounter = 0;
            writtenIds.clear();

            long convertQueryStart = logTimeForEvent("Starting convert type query", startTime);

            String enoviaTypesForQuery = getRelevantENOVIATypesForQuery(context);
            writeMessageToConsole("Types For Query = " + enoviaTypesForQuery);
            processQuery(context, PRIMARY_QUERY, enoviaTypesForQuery, ID_FILE_PREFIX, writtenIds);
            processQuery(context, TYPE_CHANGE_QUERY, enoviaTypesForQuery, ID_FILE_PREFIX, writtenIds);

            writeMessageToConsole("Number of items found: " + String.valueOf(idCounter));

            _mxUtil.commitReadOnlyTransaction(context);

            logTimeForEvent(MIGRATION_IDENTIFIER + " COMPLETE", startTime);
            writeSuccessToConsole();
        } catch (Exception iExp) {
            writeErrorToConsole(iExp.getMessage());
            _mxUtil.abortReadOnlyTransaction(context);
        } finally {
            endIEFLog();
            writer.flush();
            writer.close();
        }

        return 0;
    }

    // getQueryAndSelectionList method
    private void getQueryAndSelectionList(String typesForQuery, int queryType, Query inQuery, StringList selectList) throws Exception {
        try {
            // Initialize the input query object
            inQuery.setBusinessObjectType(typesForQuery);
            inQuery.setExpandType(false);
            inQuery.setBusinessObjectName("*");
            inQuery.setBusinessObjectRevision("*");
            if (queryType == PRIMARY_QUERY)
                inQuery.setWhereExpression("revision==last");
            else if (queryType == TYPE_CHANGE_QUERY)
                inQuery.setWhereExpression("revisions.type!=revisions.next.type");

            // Initialize the selection list
            if (queryType == PRIMARY_QUERY)
                selectList.add(ID);
            else if (queryType == TYPE_CHANGE_QUERY)
                selectList.add(DomainConstants.SELECT_LAST_ID);
        } catch (Exception iExp) {
            writeErrorToConsole(iExp.getMessage());
        }
    }

    // processQuery method
    // Parameters
    // queryType - PRIMARY_QUERY or TYPE_CHANGE_QUERY
    // typesForQuery - Retrieved by getRelevantENOVIATypesForTitleQuery or getRelevantENOVIATypesForQuery
    // filePrefix - ID_FILE_TITLE_PREFIX or ID_FILE_PREFIX
    // writtenIds - Used to avoid duplicate results between the 2 query types.
    private void processQuery(Context context, int queryType, String typesForQuery, String filePrefix, Set<String> writtenIds) throws Exception {
        try {
            Query query = new Query();
            StringList selectList = new StringList();
            getQueryAndSelectionList(typesForQuery, queryType, query, selectList);
            Set<String> idsToWrite = new HashSet<String>();
            // Set<String> writtenIds = new HashSet<String>();
            int counter = idCounter; // Initialize the file ID counter
            QueryIterator queryIterator = query.getIterator(context, selectList, (short) 1000);
            try {
                // Populate the idsToWrite set
                while (queryIterator.hasNext()) {
                    BusinessObjectWithSelect busWithSelect = queryIterator.next();
                    String lastMajorRevId = "";
                    if (queryType == PRIMARY_QUERY)
                        lastMajorRevId = (String) busWithSelect.getSelectData(ID);
                    else if (queryType == TYPE_CHANGE_QUERY)
                        lastMajorRevId = (String) busWithSelect.getSelectData(DomainConstants.SELECT_LAST_ID);

                    if (!writtenIds.contains(lastMajorRevId))
                        idsToWrite.add(lastMajorRevId);
                }
            } catch (Exception iExp) {
                writeErrorToConsole(iExp.getMessage());
            } finally {
                queryIterator.close();
            }
            // Write all IDs to files
            Iterator<String> iterator = idsToWrite.iterator();
            Set<String> idsForFile = new HashSet<String>();
            while (iterator.hasNext()) {
                ++counter;
                idsForFile.add(iterator.next());

                if (counter % chunkSize == 0) {
                    writeIDFile(idsForFile, filePrefix);
                    writtenIds.addAll(idsForFile);
                    idsForFile.clear();
                }
            }
            // Write the last chunk of the IDs to file
            if (!idsForFile.isEmpty()) {
                writeIDFile(idsForFile, filePrefix);
                idsForFile.clear();
                writtenIds.addAll(idsForFile);
            }
            idCounter = counter;
        } catch (Exception iException) {
            writeErrorToConsole(iException.getMessage());
        }
    }

    // fileNamePrefix: ID_FILE_PREFIX - id files for migration, or
    // ID_FILE_TITLE_PREFIX - id files for title update

    private void writeIDFile(Set<String> idsToWrite, String fileNamePrefix) throws Exception {
        PrintWriter idFileWriter = null;

        try {
            ++fileID;

            idFileWriter = new PrintWriter(new BufferedWriter(new FileWriter(documentDirectory + fileNamePrefix + String.valueOf(fileID) + ".txt")));
            Iterator<String> iter = idsToWrite.iterator();

            while (iter.hasNext()) {
                idFileWriter.println((String) iter.next());
            }
        } finally {
            idFileWriter.close();
        }
    }

    private MCADGlobalConfigObject getGlobalConfigObject(Context context) throws Exception {
        if (_gco == null) {
            MCADConfigObjectLoader configLoader = new MCADConfigObjectLoader();
            _gco = configLoader.createGlobalConfigObject(context, _mxUtil, SLW_GCO_TYPE, SLW_GCO_NAME);
        }

        return _gco;
    }

    private StringList getSelectListForQuery() {
        StringList selectList = new StringList();

        selectList.add(ID);
        selectList.add(TYPE);
        // selectList.add("attribute[CAD Type]");

        return selectList;
    }

    private String getRelevantENOVIATypesForQuery(Context context) throws MCADException {
        Vector componentLikeCADTypes = _gco.getTypeListForClass(MCADAppletServletProtocol.TYPE_COMPONENT_LIKE);
        Vector assemblyLikeCADTypes = _gco.getTypeListForClass(MCADAppletServletProtocol.TYPE_ASSEMBLY_LIKE);
        Vector instanceLikeCADTypes = _gco.getTypeListForClass(MCADAppletServletProtocol.TYPE_INSTANCE_LIKE);

        Vector relevantCADTypes = new Vector();
        HashSet<String> relevantENOVIATypes = new HashSet<String>();
        relevantCADTypes.addAll(componentLikeCADTypes);
        relevantCADTypes.addAll(assemblyLikeCADTypes);

        for (int i = 0; i < relevantCADTypes.size(); i++) {
            String cadType = (String) relevantCADTypes.elementAt(i);
            if (!instanceLikeCADTypes.contains(cadType)) {
                Vector enoviaTypes = getMappedBusTypesForCADType(cadType);
                relevantENOVIATypes.addAll(enoviaTypes);
            }
        }

        StringBuffer relevantENOVIATypesStr = new StringBuffer();
        Iterator<String> iter = relevantENOVIATypes.iterator();
        while (iter.hasNext()) {
            String enoviaType = iter.next();
            relevantENOVIATypesStr.append(enoviaType);

            if (iter.hasNext()) {
                relevantENOVIATypesStr.append(",");
            }
        }

        return relevantENOVIATypesStr.toString();
    }

    private String getRelevantENOVIATypesForTitleQuery(Context context) throws MCADException {
        // Get all family CAD types (TYPE_FAMILY_LIKE)
        // TYPE_INSTANCE_LIKE will be handled later in the SLWDataMigrator JPO
        Vector familyLikeCADTypes = _gco.getTypeListForClass(MCADAppletServletProtocol.TYPE_FAMILY_LIKE);

        Vector relevantCADTypes = new Vector();
        HashSet<String> relevantENOVIATypes = new HashSet<String>();
        relevantCADTypes.addAll(familyLikeCADTypes);

        // Get BusTypes from CADTypes
        for (int i = 0; i < relevantCADTypes.size(); i++) {
            String cadType = (String) relevantCADTypes.elementAt(i);
            Vector enoviaTypes = getMappedBusTypesForCADType(cadType);
            relevantENOVIATypes.addAll(enoviaTypes);
        }

        Vector drawingCADTypes = MCADUtil.getVectorFromString(DRAWING_LIKE_CADTYPES, ",");
        for (int i = 0; i < drawingCADTypes.size(); ++i) {
            String drawingCADType = (String) drawingCADTypes.elementAt(i);
            Vector drawingEnoviaTypes = getMappedBusTypesForCADType(drawingCADType);
            relevantENOVIATypes.addAll(drawingEnoviaTypes);
        }

        // Create the output ENOVIA types string
        StringBuffer relevantENOVIATypesStr = new StringBuffer();
        Iterator<String> iter = relevantENOVIATypes.iterator();
        while (iter.hasNext()) {
            String enoviaType = iter.next();
            relevantENOVIATypesStr.append(enoviaType);
            if (iter.hasNext()) {
                relevantENOVIATypesStr.append(",");
            }
        }

        return relevantENOVIATypesStr.toString();
    }

    private Vector getMappedBusTypesForCADType(String cadType) throws MCADException {
        Vector rawMappedTypes = _gco.getMappedBusTypes(cadType);

        if (rawMappedTypes == null) {
            String errorMessage = _res.getString("mcadIntegration.Server.Message.ProblemsWithGlobalConfigObject");
            MCADServerException.createException(errorMessage, null);
        }

        Vector mappedBusTypes = MCADUtil.getListOfActualTypes(rawMappedTypes);

        return mappedBusTypes;
    }

    private void readInputArguments(String[] args) throws Exception {
        if (args.length < 2)
            throw new IllegalArgumentException("Wrong number of arguments");

        documentDirectory = args[0];
        chunkSize = Integer.parseInt(args[1]);

        String fileSeparator = java.io.File.separator;
        if (documentDirectory != null && !documentDirectory.endsWith(fileSeparator))
            documentDirectory = documentDirectory + fileSeparator;
    }

    private void writeMessageToConsole(String message) throws Exception {
        writer.write(message + "\n");
    }

    private void writeSuccessToConsole() throws Exception {
        writeLineToConsole();
        writeMessageToConsole("                " + MIGRATION_IDENTIFIER + " is COMPLETE");
        writeMessageToConsole("                Time : " + (System.currentTimeMillis() - startTime) + "ms ");
        writeMessageToConsole("                " + MIGRATION_IDENTIFIER + " is     : SUCCESS");
        writeLineToConsole();
        writer.flush();
    }

    private void writeErrorToConsole(String message) throws Exception {
        writeLineToConsole();
        writeMessageToConsole(message);
        writeMessageToConsole(MIGRATION_IDENTIFIER + "     : FAILED");
        writeLineToConsole();
        writer.flush();
    }

    private void writeLineToConsole() throws Exception {
        writeMessageToConsole("=======================================================");
    }

    private void startIEFLog() throws Exception {
        try {
            iefLog = new PrintWriter(new BufferedWriter(new FileWriter(documentDirectory + "SLWDataModelMigration_FindObjects.log")));
        } catch (Exception e) {
            writeMessageToConsole("ERROR: Can not create log file. " + e.getMessage());
        }
    }

    private void endIEFLog() {
        try {
            iefLog.println();
            iefLog.println();
            iefLog.close();
        } catch (Exception e) {
        }
    }

    private void log(String message) {
        try {
            iefLog.println(message);
        } catch (Exception e) {
        }
    }

    private long logTimeForEvent(String event, long startTime) throws Exception {
        long time = System.currentTimeMillis();
        StringBuilder message = new StringBuilder(event).append(" Time: ").append(time);

        if (startTime != -1) {
            long elapsedTime = time - startTime;
            message.append(" Elapsed time: ").append(String.valueOf(elapsedTime)).append("ms");
        }

        log(message.toString());

        writeMessageToConsole(message.toString());

        return time;
    }

}
