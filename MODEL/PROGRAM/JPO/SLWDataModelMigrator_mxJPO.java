
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

import matrix.db.Access;
import matrix.db.AccessList;
import matrix.db.Attribute;
import matrix.db.AttributeList;
import matrix.db.AttributeType;
import matrix.db.BusinessObject;
import matrix.db.BusinessObjectList;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.ClientTask;
import matrix.db.ClientTaskItr;
import matrix.db.ClientTaskList;
import matrix.db.Context;
import matrix.db.MatrixWriter;
import matrix.db.StateItr;
import matrix.util.MatrixException;
import matrix.util.StringList;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.util.AccessUtil;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
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

public class SLWDataModelMigrator_mxJPO extends SLWMigrator_mxJPO {
    private String upgradedPrefix = "";

    private static final String DEFAULT_UPGRADED_PREFIX = "Default";

    private static final String DEFAULT_EBOM_EXPOSITION_MODE = "single";

    public SLWDataModelMigrator_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
    }

    protected void setMigrationParams() {
        MIG_PARAM_MIGRATION_MESSAGE = "SLW Data Model Migration";
        MIG_PARAM_ID_FILE_PREFIX = "SLWDataModelMigration_IDs_";
        MIG_PARAM_LOG_FILE_NAME = "SLWDataModelMigration";
        MIG_PARAM_FAILED_ID_FILE_PREFIX = "SLWDataModelMigration_FailedIDs_";
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
                String[] convertedInstanceInfo = null;

                try {
                    _mxUtil.startTransaction(context);

                    BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect) busWithSelectList.elementAt(i);
                    id = busObjectWithSelect.getSelectData(DomainConstants.SELECT_ID);
                    StringList revisions = busObjectWithSelect.getSelectDataList(SELECT_EXPR_REVISIONS);
                    BusinessObject createdMajorFamily = null;
                    log(new StringBuilder("Starting migration for revision stream: ").append(id).toString());

                    StringBuilder revSelectBuf = new StringBuilder();
                    for (int j = 0; j < revisions.size(); ++j) {
                        String activeMinorFileName = null;

                        revSelectBuf.setLength(0);
                        String revSelect = revSelectBuf.append("revisions[").append(revisions.elementAt(j)).append("].").toString();
                        String majorId = (String) busObjectWithSelect.getSelectData(new StringBuilder(revSelect).append(DomainConstants.SELECT_ID).toString());
                        String majorName = (String) busObjectWithSelect.getSelectData(new StringBuilder(revSelect).append(DomainConstants.SELECT_NAME).toString());
                        String majorLockStatus = (String) busObjectWithSelect.getSelectData(new StringBuilder(revSelect).append(DomainConstants.SELECT_LOCKED).toString());
                        String activeMinorId = (String) busObjectWithSelect
                                .getSelectData(new StringBuilder(revSelect).append(SELECT_EXPR_ON_ACTIVE_VERSION).append(".").append(DomainConstants.SELECT_ID).toString());
                        String finalizedMinorId = (String) busObjectWithSelect
                                .getSelectData(new StringBuilder(revSelect).append(SELECT_EXPR_ON_FINALIZED_VERSION).append(".").append(DomainConstants.SELECT_ID).toString());
                        String majorType = (String) busObjectWithSelect.getSelectData(new StringBuilder(revSelect).append(DomainConstants.SELECT_TYPE).toString());
                        String majorPolicy = (String) busObjectWithSelect.getSelectData(new StringBuilder(revSelect).append(DomainConstants.SELECT_POLICY).toString());
                        String majorCADType = (String) busObjectWithSelect.getSelectData(new StringBuilder(revSelect).append(SELECT_EXPR_ATTR_CADTYPE).toString());
                        String currentState = (String) busObjectWithSelect.getSelectData(new StringBuilder(revSelect).append(DomainConstants.SELECT_CURRENT).toString());
                        String grant = (String) busObjectWithSelect.getSelectData(new StringBuilder(revSelect).append("grant").toString());
                        // String isAttrBasedFinalizedVersion = (String)busObjectWithSelect.getSelectData(new
                        // StringBuilder(revSelect).append("from[").append(RELATIONSHIP_VERSION_OF).append("].attribute[").append(ATTR_ISFINALIZED).append("]").toString());

                        // majorCADType = majorType.indexOf("SW Component") != -1 ? "component": "assembly"; //TODO: remove this!
                        if (!isInstanceorFamilyLikeCADType(majorCADType)) {
                            String mappedFormat = _gco.getFormatsForType(majorType, majorCADType);
                            String majorFileName = (String) busObjectWithSelect.getSelectData(new StringBuilder(revSelect).append("format[").append(mappedFormat).append("].file.name").toString());

                            String finalizationState = _gco.getFinalizationState(majorPolicy);

                            boolean isFinalized = isFinalized(context, majorPolicy, finalizationState, busObjectWithSelect, revSelect);
                            String finalizedMinorFamilyId = null;
                            String activeMinorFamilyId = null;

                            StringList minorRevisions = (StringList) busObjectWithSelect.getSelectDataList(new StringBuilder(revSelect).append(SELECT_EXPR_MINOR_REVISIONS).toString());
                            String[] createdMinorFamilyIds = null;
                            if (minorRevisions != null && minorRevisions.size() > 0) {
                                createdMinorFamilyIds = new String[minorRevisions.size()];
                                BusinessObject createdMinorFamily = null;
                                StringBuilder minorRevSelectBuf = new StringBuilder();
                                for (int k = 0; k < minorRevisions.size(); ++k) {
                                    minorRevSelectBuf.setLength(0);
                                    String minorRevSelect = minorRevSelectBuf.append(revSelect).append(SELECT_EXPR_ON_ACTIVE_VERSION).append(".revisions[").append(minorRevisions.elementAt(k))
                                            .append("].").toString();
                                    String minorId = (String) busObjectWithSelect.getSelectData(new StringBuilder(minorRevSelect).append(DomainConstants.SELECT_ID).toString());
                                    String minorType = (String) busObjectWithSelect.getSelectData(new StringBuilder(minorRevSelect).append(DomainConstants.SELECT_TYPE).toString());
                                    String minorName = (String) busObjectWithSelect.getSelectData(new StringBuilder(minorRevSelect).append(DomainConstants.SELECT_NAME).toString());
                                    String minorCADType = (String) busObjectWithSelect.getSelectData(new StringBuilder(minorRevSelect).append(SELECT_EXPR_ATTR_CADTYPE).toString());

                                    if (minorCADType == null || minorCADType.equals("")) // Here the source data is improper...//TODO:log this
                                    {
                                        minorCADType = majorCADType;
                                    }
                                    // minorCADType = (minorType.indexOf("SW Versioned Component")) != -1 ? "component" : "assembly"; //TODO: remove this!

                                    if (!isInstanceorFamilyLikeCADType(minorCADType)) {
                                        String format = _gco.getFormatsForType(_mxUtil.getCorrespondingType(context, minorType), minorCADType);
                                        String minorFileName = (String) busObjectWithSelect
                                                .getSelectData(new StringBuilder(minorRevSelect).append("format[").append(format).append("].file.name").toString());

                                        boolean isActiveMinor = false;
                                        if (minorId.equals(activeMinorId)) {
                                            activeMinorFileName = minorFileName;
                                            isActiveMinor = true;
                                        }

                                        boolean isFinalizedMinor = false;
                                        if (isFinalized && minorId.equals(finalizedMinorId)) {
                                            isFinalizedMinor = true;
                                        }

                                        if (minorFileName == null || minorFileName.equals("")) {
                                            minorFileName = majorFileName != null ? majorFileName : "";
                                        }

                                        convertedInstanceInfo = convertToInstance(context, minorId, minorCADType, minorType, minorFileName, minorName, busObjectWithSelect);

                                        createdMinorFamily = createOrReviseFamilyObject(context, convertedInstanceInfo, (String) minorRevisions.elementAt(k), busObjectWithSelect, createdMinorFamily,
                                                minorFileName, minorRevSelect);

                                        String createdMinorFamilyId = createdMinorFamily.getObjectId(context);
                                        createdMinorFamilyIds[k] = createdMinorFamilyId;

                                        if (isActiveMinor) {
                                            activeMinorFamilyId = createdMinorFamilyId;
                                        }

                                        if (isFinalizedMinor) {
                                            finalizedMinorFamilyId = createdMinorFamilyId;
                                        }

                                        moveFilesToFamily(context, minorId, createdMinorFamilyId, format);

                                        if (!isFinalized || !isFinalizedMinor) {
                                            connectInstanceToFamily(context, createdMinorFamilyId, minorId, null);
                                        }
                                    }
                                }
                            }

                            if (isFinalized && finalizedMinorFamilyId == null) {
                                finalizedMinorFamilyId = activeMinorFamilyId;
                            }

                            if (!isInstanceorFamilyLikeCADType(majorCADType)) {
                                if (majorFileName == null || majorFileName.equals("")) {
                                    majorFileName = activeMinorFileName;
                                }

                                convertedInstanceInfo = convertToInstance(context, majorId, majorCADType, majorType, majorFileName, majorName, busObjectWithSelect);

                                createdMajorFamily = createOrReviseFamilyObject(context, convertedInstanceInfo, (String) revisions.elementAt(j), busObjectWithSelect, createdMajorFamily, majorFileName,
                                        revSelect);
                                String createdMajorFamilyId = createdMajorFamily.getObjectId(context);

                                if (!grant.equals("")) {
                                    BusinessObject majorInstance = new BusinessObject(majorId);
                                    copyGrants(context, majorInstance, createdMajorFamily);
                                }

                                moveFilesToFamily(context, majorId, createdMajorFamilyId, mappedFormat);

                                promoteFamilyObject(context, createdMajorFamily, currentState);

                                if (isFinalized) {
                                    connectInstanceToFamily(context, createdMajorFamilyId, majorId, finalizedMinorFamilyId);
                                }

                                connectFamilyMinors(context, createdMajorFamilyId, createdMinorFamilyIds, activeMinorFamilyId, finalizedMinorFamilyId);

                                if (majorLockStatus.equalsIgnoreCase("true")) {
                                    unlockInstance(context, majorId);
                                }
                            }
                        }
                    }

                    if (simulationMode) {
                        if (_mxUtil.isTransactionAborting(context)) {
                            throw new Exception("Transaction abort error");
                        }
                        context.abort();
                    } else {
                        _mxUtil.commitTransaction(context);
                    }

                    log(new StringBuilder("Successful migration of rev stream: ").append(id).toString());
                } catch (Exception exception) {
                    if (context.isTransactionActive()) {
                        context.abort();
                    }

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

    private void copyGrants(Context context, BusinessObject source, BusinessObject destination) throws MatrixException {
        source.open(context);
        destination.open(context);

        AccessList grants = source.getAccessAll(context);
        BusinessObjectList destObjects = new BusinessObjectList(1);
        destObjects.add(destination);

        Iterator accessItr = grants.iterator();

        while (accessItr.hasNext()) {
            Access access = (Access) accessItr.next();
            String grantor = access.getGrantor();

            if (grantor != null && !grantor.equals("")) {
                AccessList accessList = new AccessList();
                accessList.addElement(access);
                // BusinessObject.grantAccessRights(context, destObjects, grants); //This results in "grantor" becoming the migration context user (e.g. creator)
                AccessUtil.grantAccess(context, destObjects, accessList, grantor);
            }
        }

        source.close(context);
        destination.close(context);
    }

    private void unlockInstance(Context context, String majorId) throws Exception {
        String Args[] = new String[1];
        Args[0] = majorId;

        String unlockCmd = "unlock bus $1";
        String result = _mxUtil.executeMQL(context, unlockCmd, Args);

        if (result.startsWith("false")) {
            MCADException.createException("Failed to unlock object:" + majorId + " error: " + result, null);
        }
    }

    private void connectInstanceToFamily(Context context, String familyId, String instanceId, String finalizedMinorFamilyId) throws MCADException {
        String Args[] = new String[5];
        Args[0] = familyId;
        Args[1] = " relationship ";
        Args[2] = RELATIONSHIP_INSTANCE_OF;
        Args[3] = " preserve to ";
        Args[4] = instanceId;

        String instanceOfConnectCmd = "connect bus $1 $2 $3 $4 $5";
        String result = _mxUtil.executeMQL(context, instanceOfConnectCmd, Args);

        if (result.startsWith("false")) {
            MCADException.createException("Failed while connecting instance: " + instanceId + " to family", null);
        }

        if (finalizedMinorFamilyId != null && !finalizedMinorFamilyId.equals("")) {
            String Args2[] = new String[5];
            Args2[0] = finalizedMinorFamilyId;
            Args2[1] = " relationship ";
            Args2[2] = RELATIONSHIP_INSTANCE_OF;
            Args2[3] = " preserve to ";
            Args2[4] = instanceId;

            instanceOfConnectCmd = "connect bus $1 $2 $3 $4 $5";
            result = _mxUtil.executeMQL(context, instanceOfConnectCmd, Args2);

            if (result.startsWith("false")) {
                MCADException.createException("Failed while connecting instance: " + instanceId + " to finalized family version", null);
            }
        }
    }

    private void connectFamilyMinors(Context context, String familyMajorId, String[] familyMinorIds, String activeFamilyMinorId, String finalizedMinorId) throws Exception {

        String result = null;
        String errorMsg = "Failed while connecting family minors";

        String Args[] = new String[5];
        Args[0] = familyMajorId;
        Args[1] = " relationship ";
        Args[2] = RELATIONSHIP_VERSION_OF;
        Args[3] = " preserve from ";

        if (familyMinorIds != null) {
            for (int i = 0; i < familyMinorIds.length; ++i) {
                String createdMinorFamilyId = familyMinorIds[i];

                if (createdMinorFamilyId != null) {
                    Args[4] = createdMinorFamilyId;
                    String connectCmd = "connect bus $1 $2 $3 $4 $5";
                    // writeMessageToConsole(connectCmd.toString(), true);
                    result = _mxUtil.executeMQL(context, connectCmd, Args);

                    if (result.startsWith("false")) {
                        MCADException.createException(errorMsg + " Error:" + result, null);
                    }
                }
            }

            String latestFamilyMinorId = familyMinorIds[familyMinorIds.length - 1];

            connectObjects(context, familyMajorId, latestFamilyMinorId, RELATIONSHIP_LATEST_VERSION, errorMsg);
            connectObjects(context, familyMajorId, activeFamilyMinorId, RELATIONSHIP_ACTIVE_VERSION, errorMsg);

            if (finalizedMinorId != null && !finalizedMinorId.equals("")) {
                connectObjects(context, finalizedMinorId, familyMajorId, RELATIONSHIP_FINALIZED, errorMsg);
            }
        }
    }

    private void moveFilesToFamily(Context context, String fromBusId, String toBusId, String format) throws Exception {
        String Args[] = new String[5];
        Args[0] = toBusId;
        Args[1] = " move from ";
        Args[2] = fromBusId;
        Args[3] = " format ";
        Args[4] = format;

        String moveCmd = "modify bus $1 $2 $3 $4 $5";

        String result = _mxUtil.executeMQL(context, moveCmd, Args);

        if (!result.startsWith("true")) {
            MCADException.createException("Failed to move files from object: " + fromBusId + " error: " + result, null);
        }
    }

    private BusinessObject createOrReviseFamilyObject(Context context, String[] convertedInstanceInfo, String rev, BusinessObjectWithSelect busObjectWithSelect, BusinessObject previousFamilyRev,
            String fileName, String revSelect) throws Exception {
        BusinessObject busObject = null;

        String name = (String) busObjectWithSelect.getSelectData(new StringBuilder(revSelect).append(DomainConstants.SELECT_NAME).toString());
        String vault = (String) busObjectWithSelect.getSelectData(new StringBuilder(revSelect).append(DomainConstants.SELECT_VAULT).toString());
        String policy = (String) busObjectWithSelect.getSelectData(new StringBuilder(revSelect).append(DomainConstants.SELECT_POLICY).toString());
        String owner = (String) busObjectWithSelect.getSelectData(new StringBuilder(revSelect).append(DomainConstants.SELECT_OWNER).toString());
        String description = (String) busObjectWithSelect.getSelectData(new StringBuilder(revSelect).append(DomainConstants.SELECT_DESCRIPTION).toString());

        String familyEnoviaType = null;
        String familyCADType = null;

        String convertedInstanceCADType = convertedInstanceInfo[0];
        String convertedInstanceEnoviaType = convertedInstanceInfo[1];
        // String convertedInstanceEnoviaName = convertedInstanceInfo[2];

        if (_gco.isTypeOfClass(convertedInstanceCADType, "TYPE_COMPONENT_LIKE")) {
            familyEnoviaType = _gco.isMajorType(convertedInstanceEnoviaType) ? componentFamilyEnoviaType : _mxUtil.getCorrespondingType(context, componentFamilyEnoviaType);
            familyCADType = "componentFamily";
        } else if (_gco.isTypeOfClass(convertedInstanceCADType, "TYPE_ASSEMBLY_LIKE")) {
            familyEnoviaType = _gco.isMajorType(convertedInstanceEnoviaType) ? assemblyFamilyEnoviaType : _mxUtil.getCorrespondingType(context, assemblyFamilyEnoviaType);
            familyCADType = "assemblyFamily";
        }

        if (previousFamilyRev == null) {
            AttributeList attrList = getAttributeListForFamily(context, convertedInstanceEnoviaType, familyEnoviaType, familyCADType, name, fileName, busObjectWithSelect);

            busObject = new BusinessObject(familyEnoviaType, name, rev, vault);
            busObject.create(context, policy, null, attrList);

            // writeMessageToConsole("Created object:" + familyEnoviaType + "|" + name + "|" + rev, true);
        } else {
            AttributeList attrList = getAttributeListForFamily(context, convertedInstanceEnoviaType, previousFamilyRev.getTypeName(), familyCADType, name, fileName, busObjectWithSelect);

            busObject = previousFamilyRev.revise(context, null, rev, vault, false);
            busObject.setAttributes(context, attrList);
        }

        busObject.setOwner(context, owner);
        busObject.setDescription(context, description);
        busObject.update(context);

        return busObject;
    }

    private AttributeList getAttributeListForFamily(Context context, String sourceEnoviaType, String familyEnoviaType, String familyCADType, String busName, String fileName,
            BusinessObjectWithSelect sourceBusObjectWithSelect) throws Exception {
        AttributeList familyAttrList = new AttributeList();
        List<AttributeType> commonAttributes = getCommonAttributes(context, sourceEnoviaType, familyEnoviaType);

        for (int i = 0; i < commonAttributes.size(); ++i) {
            AttributeType commonAttrType = commonAttributes.get(i);
            String attrName = commonAttrType.getName();

            if (!attrCopyExclusionList.contains(attrName)) {
                String commonAttrValue = sourceBusObjectWithSelect.getSelectData(getAttributeSelect(attrName));

                String defaultAttrValue = getDefaultAttrValue(context, commonAttrType);

                if (commonAttrValue != null && defaultAttrValue != null && !defaultAttrValue.equals(commonAttrValue)) {
                    Attribute attribute = new Attribute(commonAttributes.get(i), commonAttrValue);
                    familyAttrList.add(attribute);
                }
            }
        }

        familyAttrList.add(new Attribute(cadType_AttributeType, familyCADType));

        String title = (fileName == null || fileName.equals("")) ? busName : fileName;

        familyAttrList.add(new Attribute(title_AttributeType, title));
        familyAttrList.add(new Attribute(forceNewVersion_AttributeType, "true"));

        if (ebomExposition_AttributeType != null) {
            familyAttrList.add(new Attribute(ebomExposition_AttributeType, DEFAULT_EBOM_EXPOSITION_MODE));
        }

        return familyAttrList;
    }

    private void promoteFamilyObject(Context context, BusinessObject majorFamily, String targetState) throws Exception {
        /*
         * writeMessageToConsole("am hereX"); writeMessageToConsole("am hereY"); StateItr itr = new StateItr(majorFamily.getStates(context)); while (itr.next()) { String stateName =
         * itr.obj().getName(); writeMessageToConsole("stateName:" + stateName, true); if (stateName.equalsIgnoreCase(targetState)) { break; }
         * 
         * majorFamily.overridePromotionRights(context); majorFamily.promote(context); }
         */

        majorFamily.current(context, targetState);

    }

    private String[] convertToInstance(Context context, String id, String cadType, String enoviaType, String fileName, String name, BusinessObjectWithSelect busObjectWithSelect) throws Exception {
        String targetType = null;
        String targetCADType = null;
        String[] convertedInstanceInfo = new String[3];

        String[] Args = new String[13];
        Args[0] = id;
        Args[1] = " type ";
        Args[2] = targetType;
        Args[3] = ATTR_CADTYPE;
        Args[4] = targetCADType;
        Args[5] = ATTR_TITLE;

        // newTitle;
        Args[6] = upgradedPrefix;
        Args[7] = "(" + name + ") ";

        Args[8] = " name ";

        // newName;
        Args[9] = upgradedPrefix;
        Args[10] = "(" + name + ") ";

        Args[11] = ATTR_FORCE_NEW_VERSION;
        Args[12] = "true";

        if (_gco.isTypeOfClass(cadType, "TYPE_COMPONENT_LIKE") || _gco.isTypeOfClass(cadType, "TYPE_COMPONENT_FAMILY_LIKE")) {
            targetType = _gco.isMajorType(enoviaType) ? componentInstanceEnoviaType : _mxUtil.getCorrespondingType(context, componentInstanceEnoviaType);
            targetCADType = "componentInstance";
        } else {
            targetType = _gco.isMajorType(enoviaType) ? assemblyInstanceEnoviaType : _mxUtil.getCorrespondingType(context, assemblyInstanceEnoviaType);
            targetCADType = "assemblyInstance";
        }

        String newTitle = new StringBuilder(upgradedPrefix).append("(").append(name).append(")").toString();

        // writeMessageToConsole(mqlCmd.toString(), true);
        String mqlCmd = "mod bus $1 $2 $3 $4 $5 $6 $7 $8 $9 $10 $11 $12 $13";
        String result = _mxUtil.executeMQL(context, mqlCmd, Args);

        if (result.startsWith("false")) {
            System.out.println("Failed cmd:" + mqlCmd);
            MCADException.createException("Failed while modifying object:" + id + " error:" + result, null);
        }

        /*
         * String updateCmd = new StringBuilder("updatestate bus ").append(id).toString(); writeMessageToConsole(updateCmd.toString(), true);
         * writeMessageToConsole(_mxUtil.executeMQL(context,updateCmd), true);
         */

        convertedInstanceInfo[0] = targetCADType;
        convertedInstanceInfo[1] = targetType;
        convertedInstanceInfo[2] = newTitle;

        return convertedInstanceInfo;
    }

    private StringList getAttributeAndFormatSelectsForQuery(Context context) throws Exception {
        StringList attributeAndFormatSelectsForMappedTypes = new StringList();

        Vector componentLikeCADTypes = _gco.getTypeListForClass(MCADAppletServletProtocol.TYPE_COMPONENT_LIKE);
        Vector assemblyLikeCADTypes = _gco.getTypeListForClass(MCADAppletServletProtocol.TYPE_ASSEMBLY_LIKE);
        Vector instanceLikeCADTypes = _gco.getTypeListForClass(MCADAppletServletProtocol.TYPE_INSTANCE_LIKE);

        Vector relevantCADTypes = new Vector();
        Set<String> relevantENOVIATypes = new HashSet<String>();
        relevantCADTypes.addAll(componentLikeCADTypes);
        relevantCADTypes.addAll(assemblyLikeCADTypes);

        for (int i = 0; i < relevantCADTypes.size(); i++) {
            String cadType = (String) relevantCADTypes.elementAt(i);
            if (!instanceLikeCADTypes.contains(cadType)) {
                Vector enoviaTypes = getMappedBusTypesForCADType(cadType);
                relevantENOVIATypes.addAll(enoviaTypes);

                Vector formats = _gco.getFormatForCADType(cadType);
                // Vector formats = getFormatForCADType(cadType);

                StringBuilder formatSelectBuf = new StringBuilder();
                for (int j = 0; j < formats.size(); ++j) {
                    formatSelectBuf.setLength(0);
                    String formatSelect = formatSelectBuf.append("format[").append((String) formats.elementAt(j)).append("].file.name").toString();

                    if (!attributeAndFormatSelectsForMappedTypes.contains(formatSelect)) {
                        attributeAndFormatSelectsForMappedTypes.add(new StringBuilder(SELECT_EXPR_REVISIONS).append(".").append(formatSelect).toString());
                        attributeAndFormatSelectsForMappedTypes
                                .add(new StringBuilder(SELECT_EXPR_REVISIONS).append(".").append(SELECT_EXPR_MINOR_REVISIONS).append(".").append(formatSelect).toString());

                        attributeAndFormatSelectsForMappedTypes.add(formatSelect);
                    }
                }
            }
        }

        Iterator<String> iter = relevantENOVIATypes.iterator();
        while (iter.hasNext()) {
            String enoviaType = iter.next();

            getTypeAttributes(context, enoviaType, attributeAndFormatSelectsForMappedTypes);

            String versionedType = _mxUtil.getCorrespondingType(context, enoviaType);

            getTypeAttributes(context, versionedType, attributeAndFormatSelectsForMappedTypes);
        }

        return attributeAndFormatSelectsForMappedTypes;
    }

    private List<String> getTypeAttributes(Context context, String enoviaType, StringList attributeSelectsForMappedTypes) throws Exception {
        List<String> attributeNames = new ArrayList<String>();

        if (typeAttributeSelectsMap_Cache.get(enoviaType) == null) {
            String Args[] = new String[4];
            Args[0] = enoviaType;
            Args[1] = "  attribute";
            Args[2] = " dump";
            Args[3] = " |";

            String mqlCmd = "print type $1 select $2 $3 $4";
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

    protected StringList getSelectListForQuery(Context context) throws Exception {
        StringList selectList = new StringList();

        selectList.add(DomainConstants.SELECT_ID);
        selectList.add(DomainConstants.SELECT_NAME);
        selectList.add(SELECT_EXPR_ACTIVE_VERSION_ID);
        selectList.add(SELECT_EXPR_REVISIONS);
        selectList.add(SELECT_EXPR_REVISION_VAULT);
        selectList.add(SELECT_EXPR_REVISION_ID);
        selectList.add(SELECT_EXPR_REVISION_LOCKED);
        selectList.add(SELECT_EXPR_REVISION_TYPE);
        selectList.add(SELECT_EXPR_REVISION_NAME);
        selectList.add(SELECT_EXPR_REVISION_DESCRIPTION);
        selectList.add(SELECT_EXPR_REVISIONS_ATTR_CADTYPE);
        selectList.add(SELECT_EXPR_REVISION_POLICY);
        selectList.add(SELECT_EXPR_REVISION_OWNER);
        selectList.add(SELECT_EXPR_REVISION_CURRENT);
        selectList.add(SELECT_EXPR_REVISION_STATES);
        selectList.add(SELECT_EXPR_FINALIZED_VERSION_ID);
        selectList.add(SELECT_EXPR_REVISIONS + "." + "grant");
        selectList.add(SELECT_EXPR_REVISIONS + "." + SELECT_EXPR_MINOR_REVISIONS);
        selectList.add(SELECT_EXPR_REVISIONS + "." + SELECT_EXPR_MINOR_IDS); // to[VersionOf].from.id?
        selectList.add(SELECT_EXPR_REVISIONS + "." + SELECT_EXPR_MINOR_POLICY);
        selectList.add(SELECT_EXPR_REVISIONS + "." + SELECT_EXPR_MINOR_OWNER);
        selectList.add(SELECT_EXPR_REVISIONS + "." + SELECT_EXPR_MINOR_CADTYPE);
        selectList.add(SELECT_EXPR_REVISIONS + "." + SELECT_EXPR_MINOR_TYPE);
        selectList.add(SELECT_EXPR_REVISIONS + "." + SELECT_EXPR_MINOR_VAULT);
        selectList.add(SELECT_EXPR_REVISIONS + "." + SELECT_EXPR_MINOR_NAME);
        selectList.add(SELECT_EXPR_REVISIONS + "." + SELECT_EXPR_MINOR_DESCRIPTION);

        StringList attributeAndFormatSelects = getAttributeAndFormatSelectsForQuery(context);
        selectList.addAll(attributeAndFormatSelects);

        return selectList;
    }

    protected void readInputArguments(String[] args) throws Exception {
        super.readInputArguments(args);
        upgradedPrefix = (args.length > 2) ? new MCADServerResourceBundle(args[2]).getString("mcadIntegration.Server.UpgradedPrefix") : DEFAULT_UPGRADED_PREFIX;
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

}
