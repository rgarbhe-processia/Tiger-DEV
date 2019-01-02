
/*
 * ${CLASSNAME}.java.
 *
 * Copyright (c) 1992-2011 Dassault Systemes
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
import java.util.logging.LogManager;

import matrix.db.Attribute;
import matrix.db.AttributeList;
import matrix.db.AttributeType;
import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.ClientTask;
import matrix.db.ClientTaskItr;
import matrix.db.ClientTaskList;
import matrix.db.Context;
import matrix.db.MatrixWriter;
import matrix.util.MatrixException;
import matrix.util.StringList;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.MCADIntegration.server.beans.MCADConfigObjectLoader;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADFileUtils;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADLogger;
import com.matrixone.MCADIntegration.utils.MCADUtil;

public class SLWDataModelTitleMigrator_mxJPO extends SLWMigrator_mxJPO {
    private StringList instanceSelectsForTitle_Cache = null;

    private Set<String> processedInstanceIds_Cache = null;

    public SLWDataModelTitleMigrator_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);

        processedInstanceIds_Cache = new HashSet<String>(1000);
    }

    protected void setMigrationParams() {
        MIG_PARAM_MIGRATION_MESSAGE = "SLW Data Model Title Migration";
        MIG_PARAM_ID_FILE_PREFIX = "SLWDataModelMigrationTitle_IDs_";
        MIG_PARAM_LOG_FILE_NAME = "SLWDataModelTitleMigration";
        MIG_PARAM_FAILED_ID_FILE_PREFIX = "SLWDataModelMigrationTitle_FailedIDs_";
    }

    protected int doMigration(Context context, List<String> idsList, StringList selects, String idFileName) throws Exception {
        String[] ids = new String[idsList.size()];
        idsList.toArray(ids);
        BusinessObjectWithSelectList busWithSelectList = null;
        int numFailedIds = 0;
        try {
            _mxUtil.startReadOnlyTransaction(context);

            long queryStartTime = logTimeForEvent("Start query for file:" + idFileName, -1);

            busWithSelectList = BusinessObject.getSelectBusinessObjectData(context, ids, selects);

            logTimeForEvent("End query for file:" + idFileName, queryStartTime);
        } catch (Exception ex) {
            numFailedIds = idsList.size();
            writeAllIdsToFailedFile(idFileName);
            MCADException.createException("Failed in querying ids from file:" + idFileName, null);
        } finally {
            _mxUtil.commitReadOnlyTransaction(context);
        }

        String failedIDsFileName = null;
        try {
            for (int i = 0; i < busWithSelectList.size(); i++) {
                String id = null;

                try {
                    _mxUtil.startTransaction(context);

                    BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect) busWithSelectList.elementAt(i);
                    id = busObjectWithSelect.getSelectData(DomainConstants.SELECT_ID);
                    boolean isDrawing = Boolean.valueOf(busObjectWithSelect.getSelectData("type.kindof[" + TYPE_CADDRAWING + "]"));
                    StringList revisions = busObjectWithSelect.getSelectDataList(SELECT_EXPR_REVISIONS);

                    log(new StringBuilder("Starting title migration for revision stream: ").append(id).toString());

                    StringBuilder revSelectBuf = new StringBuilder();
                    for (int j = 0; j < revisions.size(); ++j) {
                        revSelectBuf.setLength(0);
                        String revSelect = revSelectBuf.append("revisions[").append(revisions.elementAt(j)).append("].").toString();
                        String majorId = (String) busObjectWithSelect.getSelectData(new StringBuilder(revSelect).append(DomainConstants.SELECT_ID).toString());
                        String majorName = (String) busObjectWithSelect.getSelectData(new StringBuilder(revSelect).append(DomainConstants.SELECT_NAME).toString());
                        String activeMinorId = (String) busObjectWithSelect
                                .getSelectData(new StringBuilder(revSelect).append(SELECT_EXPR_ON_ACTIVE_VERSION).append(".").append(DomainConstants.SELECT_ID).toString());
                        String finalizedMinorId = (String) busObjectWithSelect
                                .getSelectData(new StringBuilder(revSelect).append(SELECT_EXPR_ON_FINALIZED_VERSION).append(".").append(DomainConstants.SELECT_ID).toString());
                        String majorType = (String) busObjectWithSelect.getSelectData(new StringBuilder(revSelect).append(DomainConstants.SELECT_TYPE).toString());
                        String majorPolicy = (String) busObjectWithSelect.getSelectData(new StringBuilder(revSelect).append(DomainConstants.SELECT_POLICY).toString());
                        String majorCADType = (String) busObjectWithSelect.getSelectData(new StringBuilder(revSelect).append(SELECT_EXPR_ATTR_CADTYPE).toString());
                        String majorCurrentTitle = (String) busObjectWithSelect.getSelectData(new StringBuilder(revSelect).append(SELECT_EXPR_ATTR_TITLE).toString());

                        if (_gco.isTypeOfClass(majorCADType, "TYPE_FAMILY_LIKE") || isDrawing) {
                            String mappedFormat = _gco.getFormatsForType(majorType, majorCADType);
                            String majorTitle = (String) busObjectWithSelect.getSelectData(new StringBuilder(revSelect).append("format[").append(mappedFormat).append("].file.name").toString());
                            // log( "majorFileName = " + majorTitle );
                            String finalizationState = _gco.getFinalizationState(majorPolicy);

                            boolean isFinalized = isFinalized(context, majorPolicy, finalizationState, busObjectWithSelect, revSelect);

                            StringList minorRevisions = (StringList) busObjectWithSelect.getSelectDataList(new StringBuilder(revSelect).append(SELECT_EXPR_MINOR_REVISIONS).toString());

                            if (minorRevisions != null && minorRevisions.size() > 0) {
                                StringBuilder minorRevSelectBuf = new StringBuilder();
                                for (int k = 0; k < minorRevisions.size(); ++k) {
                                    minorRevSelectBuf.setLength(0);
                                    String minorRevSelect = minorRevSelectBuf.append(revSelect).append(SELECT_EXPR_ON_ACTIVE_VERSION).append(".revisions[").append(minorRevisions.elementAt(k))
                                            .append("].").toString();
                                    String minorId = (String) busObjectWithSelect.getSelectData(new StringBuilder(minorRevSelect).append(DomainConstants.SELECT_ID).toString());
                                    String minorType = (String) busObjectWithSelect.getSelectData(new StringBuilder(minorRevSelect).append(DomainConstants.SELECT_TYPE).toString());
                                    String minorName = (String) busObjectWithSelect.getSelectData(new StringBuilder(minorRevSelect).append(DomainConstants.SELECT_NAME).toString());
                                    String minorCADType = (String) busObjectWithSelect.getSelectData(new StringBuilder(minorRevSelect).append(SELECT_EXPR_ATTR_CADTYPE).toString());
                                    String minorCurrentTitle = (String) busObjectWithSelect.getSelectData(new StringBuilder(minorRevSelect).append(SELECT_EXPR_ATTR_TITLE).toString());

                                    // writeMessageToConsole("processing id" + minorId, true);
                                    String minorTitle = null;
                                    if (_gco.isTypeOfClass(minorCADType, "TYPE_FAMILY_LIKE") || isDrawing) {
                                        String format = _gco.getFormatsForType(_mxUtil.getCorrespondingType(context, minorType), minorCADType);
                                        minorTitle = (isFinalized && (minorId.equals(finalizedMinorId) || minorRevisions.size() == 1) && !majorTitle.equals("")) ? majorTitle
                                                : (String) busObjectWithSelect.getSelectData(new StringBuilder(minorRevSelect).append("format[").append(format).append("].file.name").toString());
                                        // writeMessageToConsole("minorTitle" + minorTitle, true);
                                        if (minorTitle.equals("")) {
                                            minorTitle = minorName;
                                        }

                                        if (!minorCurrentTitle.equals(minorTitle)) {
                                            updateTitleAttribute(context, minorId, minorTitle);
                                        }

                                        if (!isDrawing) {
                                            processInstances(context, minorRevSelect, minorName, busObjectWithSelect);
                                        }
                                    } else {
                                        continue; // skip this minor
                                    }

                                    if (minorId.equals(activeMinorId) && majorTitle.equals("")) {
                                        majorTitle = minorTitle;
                                    }
                                }
                            }

                            if (majorTitle.equals("")) {
                                majorTitle = majorName;
                            }

                            if (!majorCurrentTitle.equals(majorTitle)) {
                                updateTitleAttribute(context, majorId, majorTitle);
                            }

                            if (!isDrawing) {
                                processInstances(context, revSelect, majorName, busObjectWithSelect);
                            }
                        } else {
                            continue; // skip this object - or should we still look at its minors?
                        }
                    }

                    if (simulationMode) {
                        context.abort();
                    } else {
                        _mxUtil.commitTransaction(context);
                    }

                    log(new StringBuilder("Successful migration of rev stream: ").append(id).toString());
                } catch (Exception exception) {
                    context.abort();

                    if (failedIDsFileName == null) {
                        failedIDsFileName = getFailedIDFileName(idFileName);
                        initFailedIDFile(failedIDsFileName);
                    }
                    ++numFailedIds;
                    writeFailedID(id);
                    log("Failed migration of rev stream: " + id + " error: " + exception.getMessage());
                    exception.printStackTrace();
                    exception.printStackTrace(iefLog);
                }
            }
        } finally {
            closeFailedIDFile();
        }

        return numFailedIds;
    }

    private void processInstances(Context context, String familyRevSelect, String familyName, BusinessObjectWithSelect busObjSelectData) throws Exception {
        for (int i = 0; i < instanceSelectsForTitle_Cache.size(); ++i) {
            String select = (String) instanceSelectsForTitle_Cache.elementAt(i);

            StringList instanceIds = (StringList) busObjSelectData.getSelectDataList(new StringBuilder(familyRevSelect).append(select).append(".").append(DomainObject.SELECT_ID).toString());
            StringList instanceNames = (StringList) busObjSelectData.getSelectDataList(new StringBuilder(familyRevSelect).append(select).append(".").append(DomainObject.SELECT_NAME).toString());
            updateTitleForInstances(context, instanceIds, instanceNames, familyName);

            StringList majorInstanceIds = (StringList) busObjSelectData
                    .getSelectDataList(new StringBuilder(familyRevSelect).append(select).append(".").append(SELECT_EXPR_ON_MAJOR).append(".").append(DomainObject.SELECT_ID).toString());
            StringList majorInstanceNames = (StringList) busObjSelectData
                    .getSelectDataList(new StringBuilder(familyRevSelect).append(select).append(".").append(SELECT_EXPR_ON_MAJOR).append(".").append(DomainObject.SELECT_NAME).toString());
            updateTitleForInstances(context, majorInstanceIds, majorInstanceNames, familyName);

            StringList finalizedMinorInstanceIds = (StringList) busObjSelectData
                    .getSelectDataList(new StringBuilder(familyRevSelect).append(select).append(".").append(SELECT_EXPR_ON_ACTIVE_VERSION).append(".").append(DomainObject.SELECT_ID).toString());
            StringList finalizedMinorInstanceNames = (StringList) busObjSelectData
                    .getSelectDataList(new StringBuilder(familyRevSelect).append(select).append(".").append(SELECT_EXPR_ON_ACTIVE_VERSION).append(".").append(DomainObject.SELECT_NAME).toString());

            // writeMessageToConsole("finalizedMinorInstanceIds:" + finalizedMinorInstanceIds, true);
            updateTitleForInstances(context, finalizedMinorInstanceIds, finalizedMinorInstanceNames, familyName);
        }
    }

    private void updateTitleForInstances(Context context, StringList instanceIds, StringList instanceNames, String familyName) throws Exception {
        if (instanceIds != null && !instanceIds.isEmpty()) {
            for (int i = 0; i < instanceIds.size(); ++i) {
                String instanceId = (String) instanceIds.elementAt(i);
                String instanceName = (String) instanceNames.elementAt(i);

                if (instanceId != null && instanceName != null && !processedInstanceIds_Cache.contains(instanceId)) {
                    String instanceTitle = new StringBuilder(_serverGeneralUtil.getIndivisualInstanceName(instanceName)).append("(").append(familyName).append(")").toString();

                    updateTitleAttribute(context, instanceId, instanceTitle);

                    processedInstanceIds_Cache.add(instanceId);
                }
            }
        }
    }

    private void updateTitleAttribute(Context context, String id, String title) throws Exception {
        // writeMessageToConsole(mqlCmd.toString(), true);
        String Args[] = new String[3];
        Args[0] = id;
        Args[1] = ATTR_TITLE;
        Args[2] = title;

        String mqlCmd = "mod bus $1 $2 $3";

        String result = _mxUtil.executeMQL(context, mqlCmd, Args);

        // log("mqlCmd:" + mqlCmd.toString());
        if (result.startsWith("false")) {
            MCADException.createException("Failed to update title for object:" + id + " Error: " + result, null);
        }
    }

    private StringList getFormatSelectsForTitleQuery(Context context) throws Exception {
        StringList formatSelectsForMappedTypes = new StringList();

        Vector familyLikeCADTypes = _gco.getTypeListForClass(MCADAppletServletProtocol.TYPE_FAMILY_LIKE);
        Vector instanceLikeCADTypes = _gco.getTypeListForClass(MCADAppletServletProtocol.TYPE_INSTANCE_LIKE);
        Vector drawingCADTypes = MCADUtil.getVectorFromString(DRAWING_LIKE_CADTYPES, ",");

        Vector relevantCADTypes = new Vector();
        Set<String> relevantENOVIATypes = new HashSet<String>();
        relevantCADTypes.addAll(familyLikeCADTypes);
        relevantCADTypes.addAll(instanceLikeCADTypes);
        relevantCADTypes.addAll(drawingCADTypes);

        for (int i = 0; i < relevantCADTypes.size(); i++) {
            String cadType = (String) relevantCADTypes.elementAt(i);
            Vector enoviaTypes = getMappedBusTypesForCADType(cadType);
            relevantENOVIATypes.addAll(enoviaTypes);

            Vector formats = _gco.getFormatForCADType(cadType);
            // Vector formats = getFormatForCADType(cadType);

            StringBuilder formatSelectBuf = new StringBuilder();
            for (int j = 0; j < formats.size(); ++j) {
                formatSelectBuf.setLength(0);
                String formatSelect = formatSelectBuf.append("format[").append((String) formats.elementAt(j)).append("].file.name").toString();

                if (!formatSelectsForMappedTypes.contains(formatSelect)) {
                    formatSelectsForMappedTypes.add(new StringBuilder(SELECT_EXPR_REVISIONS).append(".").append(formatSelect).toString());
                    formatSelectsForMappedTypes.add(new StringBuilder(SELECT_EXPR_REVISIONS).append(".").append(SELECT_EXPR_MINOR_REVISIONS).append(".").append(formatSelect).toString());

                    formatSelectsForMappedTypes.add(formatSelect);
                }
            }
        }

        return formatSelectsForMappedTypes;
    }

    protected StringList getSelectListForQuery(Context context) throws Exception {
        StringList selectList = new StringList();

        selectList.add(DomainConstants.SELECT_ID);
        selectList.add(DomainConstants.SELECT_NAME);
        selectList.add("type.kindof[" + TYPE_CADDRAWING + "]");
        selectList.add(SELECT_EXPR_ACTIVE_VERSION_ID);
        selectList.add(SELECT_EXPR_REVISIONS);
        // selectList.add(SELECT_EXPR_REVISION_VAULT);
        selectList.add(SELECT_EXPR_REVISION_ID);
        // selectList.add(SELECT_EXPR_REVISION_LOCKED);
        selectList.add(SELECT_EXPR_REVISION_TYPE);
        selectList.add(SELECT_EXPR_REVISION_NAME);
        // selectList.add(SELECT_EXPR_REVISION_DESCRIPTION);
        selectList.add(SELECT_EXPR_REVISIONS_ATTR_CADTYPE);
        selectList.add(SELECT_EXPR_REVISION_POLICY);
        // selectList.add(SELECT_EXPR_REVISION_OWNER);
        selectList.add(SELECT_EXPR_REVISION_CURRENT);
        selectList.add(SELECT_EXPR_REVISIONS_ATTR_TITLE);
        selectList.add(SELECT_EXPR_REVISION_STATES);
        selectList.add(SELECT_EXPR_FINALIZED_VERSION_ID);
        selectList.add(SELECT_EXPR_REVISIONS + "." + SELECT_EXPR_MINOR_REVISIONS);
        selectList.add(SELECT_EXPR_REVISIONS + "." + SELECT_EXPR_MINOR_IDS); // to[VersionOf].from.id?
        selectList.add(SELECT_EXPR_REVISIONS + "." + SELECT_EXPR_MINOR_POLICY);
        // selectList.add(SELECT_EXPR_REVISIONS + "." + SELECT_EXPR_MINOR_OWNER);
        selectList.add(SELECT_EXPR_REVISIONS + "." + SELECT_EXPR_MINOR_CADTYPE);
        selectList.add(SELECT_EXPR_REVISIONS + "." + SELECT_EXPR_MINOR_TITLE);
        selectList.add(SELECT_EXPR_REVISIONS + "." + SELECT_EXPR_MINOR_TYPE);
        // selectList.add(SELECT_EXPR_REVISIONS + "." + SELECT_EXPR_MINOR_VAULT);
        selectList.add(SELECT_EXPR_REVISIONS + "." + SELECT_EXPR_MINOR_NAME);
        // selectList.add(SELECT_EXPR_REVISIONS + "." + SELECT_EXPR_MINOR_DESCRIPTION);

        selectList.addAll(getFormatSelectsForTitleQuery(context));

        StringList instanceSelects = getInstanceSelectsForTitleQuery();

        for (int i = 0; i < instanceSelects.size(); ++i) {
            String instanceRelSelect = (String) instanceSelects.elementAt(i);

            // from major of family
            selectList.add(SELECT_EXPR_REVISIONS + "." + instanceRelSelect + "." + DomainConstants.SELECT_ID);// Instance major id
            selectList.add(SELECT_EXPR_REVISIONS + "." + instanceRelSelect + "." + DomainConstants.SELECT_NAME);// Instance major name
            selectList.add(SELECT_EXPR_REVISIONS + "." + instanceRelSelect + "." + SELECT_EXPR_ON_ACTIVE_VERSION + "." + DomainConstants.SELECT_ID); // Instance minor id
            selectList.add(SELECT_EXPR_REVISIONS + "." + instanceRelSelect + "." + SELECT_EXPR_ON_ACTIVE_VERSION + "." + DomainConstants.SELECT_NAME); // Instance minor name

            // from minor of family
            selectList.add(SELECT_EXPR_REVISIONS + "." + SELECT_EXPR_MINOR_REVISIONS + "." + instanceRelSelect + "." + DomainConstants.SELECT_ID);// Instance minor id
            selectList.add(SELECT_EXPR_REVISIONS + "." + SELECT_EXPR_MINOR_REVISIONS + "." + instanceRelSelect + "." + DomainConstants.SELECT_NAME);// Instance minor name
            selectList.add(SELECT_EXPR_REVISIONS + "." + SELECT_EXPR_MINOR_REVISIONS + "." + instanceRelSelect + "." + SELECT_EXPR_ON_MAJOR + "." + DomainConstants.SELECT_ID); // Instance major id
            selectList.add(SELECT_EXPR_REVISIONS + "." + SELECT_EXPR_MINOR_REVISIONS + "." + instanceRelSelect + "." + SELECT_EXPR_ON_MAJOR + "." + DomainConstants.SELECT_NAME); // Instance major name
            selectList.add(SELECT_EXPR_REVISIONS + "." + SELECT_EXPR_MINOR_REVISIONS + "." + instanceRelSelect + "." + SELECT_EXPR_ON_ACTIVE_VERSION + "." + DomainConstants.SELECT_ID); // Instance
                                                                                                                                                                                         // minor id
            selectList.add(SELECT_EXPR_REVISIONS + "." + SELECT_EXPR_MINOR_REVISIONS + "." + instanceRelSelect + "." + SELECT_EXPR_ON_ACTIVE_VERSION + "." + DomainConstants.SELECT_NAME); // Instance
                                                                                                                                                                                           // minor name

        }

        return selectList;
    }

    private StringList getInstanceSelectsForTitleQuery() throws Exception {
        if (instanceSelectsForTitle_Cache == null) {
            instanceSelectsForTitle_Cache = new StringList();

            Hashtable familyLikeRelsAndEnds = _gco.getRelationshipsOfClass(MCADServerSettings.FAMILY_LIKE);

            Set keys = familyLikeRelsAndEnds.keySet();
            Iterator iterator = keys.iterator();

            while (iterator.hasNext()) {
                String rel = (String) iterator.next();
                String end = (String) familyLikeRelsAndEnds.get(rel);

                String parentEnd = (end.equals("to")) ? "from" : "to";
                instanceSelectsForTitle_Cache.add(new StringBuilder(parentEnd).append("[").append(rel).append("].").append(end).toString());
            }
        }

        return instanceSelectsForTitle_Cache;
    }
}
