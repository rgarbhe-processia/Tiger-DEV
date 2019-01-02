
/*
 ** PSS_MCADRenameAsPart
 **
 ** Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 **
 ** Program to rename the MCAD objects.
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADNameValidationUtil;
import com.matrixone.MCADIntegration.utils.MCADUtil;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectItr;
import matrix.db.BusinessObjectList;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.ExpansionIterator;
import matrix.db.FileItr;
import matrix.db.FileList;
import matrix.db.JPO;
import matrix.db.Relationship;
import matrix.util.StringList;
import pss.common.utils.TigerEnums.FileFormat;
import pss.common.utils.TigerUtils;
import pss.constants.TigerConstants;

public class PSS_MCADRenameAsPart_mxJPO extends IEFCommonUIActions_mxJPO {

    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PSS_MCADRenameAsPart_mxJPO.class);

    public static final String SUITE_KEY = "emxIEFDesignCenterStringResource";

    protected String _errStr = "";

    protected String _integName = "";

    // java.util.Vector renamedObjNames = null;

    private Vector alreadyRenamedObjList = null;

    protected boolean isSystemCaseSensitive = true;

    public PSS_MCADRenameAsPart_mxJPO() {

    }

    public PSS_MCADRenameAsPart_mxJPO(Context context, String[] args) throws Exception {
        if (!context.isConnected())
            MCADServerException.createException("not supported no desktop client", null);

    }

    public int mxMain(Context context, String[] args) throws Exception {
        return 0;
    }

    // Functionamlity for Rename CAD objects start
    /**
     * This method is copy of OOTB method of JPO MCADRenameBase.
     * @param context
     * @param args
     * @throws Exception
     * @author Steepgraph Systems
     */
    public HashMap executeRenameOperation(Context _context, String[] args) throws Exception {
        HashMap returnMap = new HashMap();
        try {
            HashMap skippedMap = new HashMap();
            HashMap ProcessedMap = new HashMap();
            HashMap ErrorMap = new HashMap();
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap argMap = (HashMap) programMap.get("ArgumentMap");
            HashMap inputMap = (HashMap) programMap.get("inputMap");

            // Set inputSet = inputMap.keySet();
            Set inputSet = inputMap.entrySet();
            Iterator inputItr = inputSet.iterator();
            String strObjectId = "";
            String strNewName = "";
            String strOldName = "";
            while (inputItr.hasNext()) {
                Map.Entry inputEntry = (Map.Entry) inputItr.next();
                strObjectId = (String) inputEntry.getKey();
                strNewName = ((String) inputEntry.getValue()).trim();
                if (strNewName.equalsIgnoreCase("null")) {
                    strNewName = "";
                }
                DomainObject obj = DomainObject.newInstance(_context, strObjectId);
                strOldName = (String) obj.getInfo(_context, DomainConstants.SELECT_NAME);
                if (strNewName.length() == 0 || strNewName.equals("")) {
                    skippedMap.put(strOldName, "No new Name defined for renaming");
                } else if (strNewName.equalsIgnoreCase(strOldName)) {
                    skippedMap.put(strOldName, "Old Name and new name is same");
                } else {
                    try {
                        InitializeArg(_context, argMap, strObjectId);
                        executeCustom(_context, strNewName, strOldName);
                        ProcessedMap.put(strOldName, "Object " + strOldName + " succesfully renamed to " + strNewName);
                    } catch (Exception ex) {
                        // TIGTK-5405 - 18-04-2017 - PTE - START
                        logger.error("Error in executeRenameOperation: ", ex);
                        // TIGTK-5405 - 18-04-2017 - PTE - End

                        ErrorMap.put(strOldName, ex.getMessage());
                    }
                }
            }

            returnMap.put("skippedMap", skippedMap);
            returnMap.put("ProcessedMap", ProcessedMap);
            returnMap.put("ErrorMap", ErrorMap);
        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in executeRenameOperation: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End
        }
        return returnMap;
    }// END of method executeRenameOperation

    /**
     * This method is copy of OOTB method of JPO MCADRenameBase.
     * @throws Exception
     * @author Steepgraph Systems
     */
    private void InitializeArg(Context _context, HashMap argMap, String strBusId) throws Exception {
        // TODO Auto-generated method stub
        try {
            _globalConfig = (MCADGlobalConfigObject) argMap.get("_globalConfig");
            String languageName = (String) argMap.get("strLanguage");
            _serverResourceBundle = new MCADServerResourceBundle(languageName);
            _cache = new IEFGlobalCache();
            _busObjectID = strBusId;
            _busObject = new BusinessObject(_busObjectID);
            _util = new MCADMxUtil(_context, _serverResourceBundle, _cache);
            _generalUtil = new MCADServerGeneralUtil(_context, _globalConfig, _serverResourceBundle, _cache);
            // _integName = (String)argMap.get("integrationName");
            _integName = _util.getIntegrationName(_context, _busObjectID);
        } catch (Exception e) {
            MCADServerException.createException(e.getMessage(), e);
        }
    }// END of method InitializeArg

    /**
     * This method is copy of OOTB method of JPO MCADRenameBase.
     * @throws MCADException
     * @author Steepgraph Systems
     */
    public void executeCustom(Context _context, String strNewName, String strOldName) throws MCADException {
        String attrFileSouceName = MCADMxUtil.getActualNameForAEFData(_context, "attribute_IEF-FileSource");
        BusinessObject renamedObj = null;
        boolean isObjectAndFilenameDifferent = _globalConfig.isObjectAndFileNameDifferent();
        // String newName = (String) _argumentsTable.get(MCADServerSettings.NEW_NAME);
        String newName = strNewName;

        // String instanceName = (String) _argumentsTable.get(MCADServerSettings.INSTANCE_NAME);
        String instanceName = "";
        // String priority = (String) _argumentsTable.get(MCADServerSettings.MESSAGE_PRIORITY);
        // isSystemCaseSensitive = (Boolean.valueOf((String) _argumentsTable.get(MCADServerSettings.CASE_SENSITIVE_FLAG))).booleanValue();
        String paramsToReturn = "";
        alreadyRenamedObjList = new Vector();

        try {
            // _integName = (String) _argumentsTable.get(MCADAppletServletProtocol.INTEGRATION_NAME);

            _busObject.open(_context);

            String busType = _busObject.getTypeName();

            // String oldName = (String) _argumentsTable.get(MCADServerSettings.OLD_NAME);
            String oldName = (String) strOldName;

            if (oldName == null || oldName.equals(""))
                oldName = _busObject.getName();

            String cadType = _util.getCADTypeForBO(_context, _busObject);

            _busObject.close(_context);

            // resultAndStatusTable.put(MCADServerSettings.NEW_NAME, newName);

            StringBuffer renamedObjIds = new StringBuffer();
            // Renaming a ProE Object
            if (_integName.equalsIgnoreCase("MxPro")) {
                if (_globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE)) {
                    String oldProeInstanceName = _generalUtil.getIndivisualInstanceName(oldName);
                    BusinessObject familyObject = null;

                    if (_util.isMajorObject(_context, _busObjectID))// _globalConfig.isMajorType(busType)) [NDM]
                    {
                        BusinessObjectList minorObjects = _util.getMinorObjects(_context, _busObject);
                        BusinessObject latestMinor = (BusinessObject) minorObjects.lastElement();

                        latestMinor.open(_context);

                        String latestMinorID = latestMinor.getObjectId(_context);

                        latestMinor.close(_context);

                        latestMinorID = _util.getLatestRevisionID(_context, latestMinorID);
                        latestMinor = new BusinessObject(latestMinorID);

                        familyObject = _generalUtil.getFamilyObjectForInstance(_context, latestMinor);
                    } else {
                        familyObject = _generalUtil.getFamilyObjectForInstance(_context, _busObject);
                    }

                    familyObject.open(_context);
                    String familyName = familyObject.getName();

                    if (oldProeInstanceName.equalsIgnoreCase(familyName)) {
                        _busObject = familyObject;
                        oldName = familyName;
                        cadType = _util.getCADTypeForBO(_context, familyObject);
                    }

                    familyObject.close(_context);
                }
            }

            boolean instanceRename = false;

            if (_globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
                instanceRename = true;

            // to get all list of all objects to process
            BusinessObjectList objectsList = new BusinessObjectList();
            Vector objIDList = new Vector();

            getAllObjectsAcrossStreamsToRename(_context, _busObject, objectsList, objIDList, instanceRename);

            String attrTitle = MCADMxUtil.getActualNameForAEFData(_context, "attribute_Title");
            String oldTitle = _busObject.getAttributeValues(_context, attrTitle).getValue();
            String newTitle = "";

            if (instanceRename) {
                if (isObjectAndFilenameDifferent) {
                    String familyNameFromTitle = _generalUtil.getFamilyNameFromObjectName(oldTitle, true);
                    newTitle = MCADUtil.getDisplayNameForInstance(familyNameFromTitle, newName);
                } else {
                    String familyNameFromTitle = _generalUtil.getFamilyNameFromObjectName(oldTitle);

                    newTitle = MCADUtil.getNameForInstance(_globalConfig, familyNameFromTitle, newName);
                }
            }

            // Assumption : For instanceRename, 'objectsList' will have only instance revisions stream.
            BusinessObjectItr objItr = new BusinessObjectItr(objectsList);

            while (objItr.next()) {
                BusinessObject busObject = null;

                busObject = objItr.obj();

                if (!busObject.isOpen())
                    busObject.open(_context);

                boolean isRenamed = false;
                String currentBusOldName = busObject.getName();
                String currentCadType = _util.getCADTypeForBO(_context, busObject);
                String busID = busObject.getObjectId();
                busObject.close(_context);

                // BusinessObject renamedObj = busObject;
                renamedObj = busObject;

                if (instanceRename) {
                    Vector fileNames = new Vector();

                    try {
                        String cadType1 = _util.getCADTypeForBO(_context, busObject);
                        String formatName = _generalUtil.getFormatsForType(_context, busObject.getTypeName(), cadType1);
                        FileList fileList = busObject.getFiles(_context, formatName);
                        FileItr itr = new FileItr(fileList);
                        while (itr.next()) {
                            String fName = itr.obj().getName();
                            fileNames.addElement(fName);
                        }

                        if (fileNames.size() == 0) {
                            fileNames.addElement("");
                        }

                        if (!fileNames.isEmpty()) {
                            paramsToReturn = fileNames.toString();
                        }
                    } catch (Exception ex) {
                        // TIGTK-5405 - 19-04-2017 - PTE - START
                        logger.error("Error in executeCustom: ", ex);
                        throw ex;
                        // TIGTK-5405 - 19-04-2017 - PTE - End
                    }

                    String actualNewName = getNewNameForInstanceRename(newName, currentBusOldName);
                    String actualOldBusName = _generalUtil.getIndivisualInstanceName(currentBusOldName);

                    String newNameForFileOperation = actualNewName;
                    checkForTitleUniqueness(_context, newTitle, cadType, busObject, newName);

                    if (isObjectAndFilenameDifferent) {
                        String instanceNameFromTitle = MCADUtil.getIndivisualInstanceName(oldTitle, true);

                        String indivisualInstanceName = _generalUtil.getIndivisualInstanceName(currentBusOldName);

                        actualNewName = indivisualInstanceName.replace(instanceNameFromTitle, newName);

                        actualNewName = getNewNameForInstanceRename(actualNewName, currentBusOldName);

                        actualOldBusName = instanceNameFromTitle;

                        String familyTitle = _generalUtil.getFamilyNameFromObjectName(oldTitle, true);

                        newNameForFileOperation = MCADUtil.getDisplayNameForInstance(familyTitle, newName);
                    }

                    if (!newName.equals(actualOldBusName)) {
                        isRenamed = true;
                        // get name changed object
                        if (!actualNewName.equals(currentBusOldName)) {
                            // do Name Validation
                            validateBusObjectName(actualNewName, currentCadType);
                            if (!alreadyRenamedObjList.contains(busObject.getObjectId())) {
                                renamedObj = _util.renameObject(_context, busObject, actualNewName);
                                alreadyRenamedObjList.add(busObject.getObjectId());
                            }

                        }

                        // rename dependent docs if any
                        renameDependentDocs(_context, renamedObj, currentBusOldName, actualNewName, oldName, newNameForFileOperation, instanceName.trim(), renamedObjIds);

                        // set Parent Instance attribute on Instance Of relationship for nested instance if parent instance is renamed
                        String renamedObjID = renamedObj.getObjectId(_context);
                        String instanceOf = MCADMxUtil.getActualNameForAEFData(_context, "relationship_InstanceOf");
                        String parentInstance = MCADMxUtil.getActualNameForAEFData(_context, "attribute_ParentInstance");
                        String[] oids = new String[1];
                        oids[0] = renamedObjID;
                        StringList busSelectList = new StringList(5);

                        String selectOnInst = "to[" + instanceOf + "].from.from[" + instanceOf + "].";

                        busSelectList.add(selectOnInst + "id");
                        busSelectList.add(selectOnInst + "attribute[" + parentInstance + "]");

                        BusinessObjectWithSelectList busWithSelectList = BusinessObjectWithSelect.getSelectBusinessObjectData(_context, oids, busSelectList);
                        BusinessObjectWithSelect busWithSelect = busWithSelectList.getElement(0);
                        StringList relIdList = (StringList) busWithSelect.getSelectDataList(selectOnInst + "id");
                        StringList attrList = (StringList) busWithSelect.getSelectDataList(selectOnInst + "attribute[" + parentInstance + "]");

                        if (attrList != null) {
                            for (int b = 0; b < attrList.size(); b++) {
                                String attrParentInst = (String) attrList.elementAt(b);
                                if (attrParentInst.equals(actualOldBusName)) {
                                    String relId = (String) relIdList.elementAt(b);
                                    Relationship nestedInstRel = new Relationship(relId);
                                    String indivInstName = _generalUtil.getIndivisualInstanceName(actualNewName);

                                    if (isObjectAndFilenameDifferent)
                                        indivInstName = newName;

                                    _util.setRelationshipAttributeValue(_context, nestedInstRel, parentInstance, indivInstName);
                                }
                            }
                        }
                    }

                    // get actual server side operations for rename done
                    // Set file source attribute for Rename operation
                    _util.setAttributeValue(_context, renamedObj, attrFileSouceName, MCADAppletServletProtocol.FILESOURCE_RENAME);

                    boolean renamedFromRqdOnFamilyLike = true;

                    _generalUtil.doActualRename(_context, renamedObj, actualNewName, currentBusOldName, newNameForFileOperation, actualOldBusName, renamedFromRqdOnFamilyLike, true);
                } else {
                    String actualNewName = "";

                    if (isObjectAndFilenameDifferent && oldTitle.contains(".") && newName.contains(".") && oldTitle.substring(0, oldTitle.lastIndexOf(".")).equals(currentBusOldName))
                        actualNewName = newName.substring(0, newName.lastIndexOf("."));
                    else
                        actualNewName = _util.replace(currentBusOldName, oldName, newName);

                    String actualOldBusName = currentBusOldName;

                    boolean renamedFromRqdOnFamilyLike = true;

                    if (_globalConfig.isTypeOfClass(currentCadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE)) {
                        String oldFamilyNameFromInstanceName = _generalUtil.getFamilyNameFromObjectName(currentBusOldName);

                        String newFamilyBusName = "";
                        if (isObjectAndFilenameDifferent && oldTitle.contains(".") && newName.contains(".") && oldTitle.substring(0, oldTitle.lastIndexOf(".")).equals(oldFamilyNameFromInstanceName))
                            newFamilyBusName = newName.substring(0, newName.lastIndexOf("."));
                        else
                            newFamilyBusName = _util.replace(oldFamilyNameFromInstanceName, oldName, newName);

                        boolean isProeGenericInstance = false;
                        renamedFromRqdOnFamilyLike = false;

                        if (_integName.equalsIgnoreCase("MxPro")) {
                            String proeInstanceName = _generalUtil.getIndivisualInstanceName(currentBusOldName);
                            if (proeInstanceName.equalsIgnoreCase(oldName)) {
                                isProeGenericInstance = true;
                                renamedFromRqdOnFamilyLike = true;
                            }
                        }

                        actualNewName = getNewNameForFamilyRename(newFamilyBusName, currentBusOldName, isProeGenericInstance);

                        isRenamed = false;
                        // get name changed object
                        if (!actualNewName.equals(currentBusOldName)) {
                            // do Name Validation
                            validateBusObjectName(actualNewName, currentCadType);

                            if (!alreadyRenamedObjList.contains(busObject.getObjectId())) {
                                renamedObj = _util.renameObject(_context, busObject, actualNewName);
                                alreadyRenamedObjList.add(busObject.getObjectId());
                            }

                        }

                        // rename dependent docs if any
                        renameDependentDocs(_context, renamedObj, currentBusOldName, actualNewName, oldName, newName, instanceName.trim(), renamedObjIds);
                    } else if (!oldName.equals(newName)) {
                        isRenamed = true;

                        // get name changed object
                        if (!actualNewName.equals(currentBusOldName)) {
                            // do Name Validation
                            validateBusObjectName(actualNewName, currentCadType);

                            if (!alreadyRenamedObjList.contains(busObject.getObjectId())) {
                                renamedObj = _util.renameObject(_context, busObject, actualNewName);
                                alreadyRenamedObjList.add(busObject.getObjectId());
                            }

                        }

                        // rename dependent docs if any
                        renameDependentDocs(_context, renamedObj, currentBusOldName, actualNewName, oldName, newName, instanceName.trim(), renamedObjIds);

                        _util.setAttributeValue(_context, renamedObj, attrFileSouceName, MCADAppletServletProtocol.FILESOURCE_RENAME);
                    } else {
                        _util.setAttributeValue(_context, renamedObj, attrFileSouceName, MCADAppletServletProtocol.FILESOURCE_RENAME);
                    }

                    // get actual server side operations for rename done
                    // Set file source attribute for Rename operation
                    _generalUtil.doActualRename(_context, renamedObj, actualNewName, actualOldBusName, newName, oldName, renamedFromRqdOnFamilyLike, true);
                }

                if (isRenamed) {
                    renamedObjIds.append(busID);
                    renamedObjIds.append(MCADAppletServletProtocol.IEF_SEPERATOR_ONE);
                    renamedObjIds.append(oldName);
                    renamedObjIds.append(MCADAppletServletProtocol.IEF_SEPERATOR_TWO);
                }
            }
            _util.setAttributeValue(_context, renamedObj, attrTitle, newName);

        } catch (Exception e) {
            String error = e.getMessage();
            MCADServerException.createException(error, e);
        }
    }// END of method executeCustom

    /**
     * This method is copy of OOTB method of JPO MCADRenameBase.
     * @param context
     * @param String
     * @throws Exception
     * @author Steepgraph Systems
     */
    private void validateBusObjectName(String newName, String cadType) throws MCADServerException {
        if (_globalConfig.getNonSupportedCharacters() == null || _globalConfig.getNonSupportedCharacters().equals("")) {
            boolean isValidFileName = MCADNameValidationUtil.isValidNameForCADType(newName, cadType, _globalConfig);
            if (!isValidFileName) {
                MCADServerException.createManagedException("IEF0292300325", _serverResourceBundle.getString("mcadIntegration.Server.Message.IEF0292300325"), null);
            }
        }
    }// END of method validateBusObjectName

    /**
     * This method is copy of OOTB method of JPO MCADRenameBase.
     * @throws Exception
     * @author Steepgraph Systems
     */
    protected String getNewNameForInstanceRename(String newName, String oldInstanceName) {
        String familyName = _generalUtil.getFamilyNameFromObjectName(oldInstanceName);
        String newInstanceName = _generalUtil.getNameForInstance(familyName, newName);

        return newInstanceName;
    }// END of method getNewNameForInstanceRename

    /**
     * This method is copy of OOTB method of JPO MCADRenameBase.
     * @throws Exception
     * @author Steepgraph Systems
     */
    protected String getNewNameForFamilyRename(String newFamilyBusName, String oldInstanceBusName, boolean isProeGenericInstance) {
        String individualInstName = _generalUtil.getIndivisualInstanceName(oldInstanceBusName);

        String newNameForInstance = _generalUtil.getNameForInstance(newFamilyBusName, individualInstName);

        if (isProeGenericInstance) {
            newNameForInstance = _generalUtil.getNameForInstance(newFamilyBusName, newFamilyBusName);
        }

        return newNameForInstance;
    }// END of method getNewNameForFamilyRename

    /**
     * This method is copy of OOTB method of JPO MCADRenameBase.
     * @throws Exception
     * @author Steepgraph Systems
     */
    protected void getAllObjectsAcrossStreamsToRename(Context _context, BusinessObject busObj, BusinessObjectList ObjectsList, Vector objIDList, boolean instanceRename) {
        // String busType = busObj.getTypeName();
        boolean isMinorType = !_util.isMajorObject(_context, busObj.getObjectId());// _globalConfig.isMajorType(busType); [NDM]

        try {
            BusinessObjectList newObjList = _util.getRevisionBOsOfAllStreams(_context, busObj, isMinorType);
            BusinessObjectItr objItr = new BusinessObjectItr(newObjList);
            while (objItr.next()) {
                BusinessObject BusObject = objItr.obj();
                BusObject.open(_context);
                String busid = BusObject.getObjectId();
                String CadType = _util.getCADTypeForBO(_context, BusObject);
                BusObject.close(_context);
                if (!instanceRename
                        && (_globalConfig.isTypeOfClass(CadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE) || _globalConfig.isTypeOfClass(CadType, MCADAppletServletProtocol.TYPE_FAMILY_LIKE))) {
                    Vector preInstanceList = _generalUtil.getInstanceListForFamilyObject(_context, BusObject.getObjectId(_context));
                    Enumeration keys = preInstanceList.elements();
                    while (keys.hasMoreElements()) {
                        BusinessObject instObj = (BusinessObject) keys.nextElement();
                        instObj.open(_context);
                        String busObjType = instObj.getTypeName();
                        instObj.close(_context);

                        boolean bIsMinorType = !_util.isMajorObject(_context, busObj.getObjectId());// _globalConfig.isMajorType(busObjType); [NDM]
                        BusinessObjectList intObjList = _util.getRevisionBOsOfAllStreams(_context, instObj, bIsMinorType);

                        BusinessObjectItr instObjItr = new BusinessObjectItr(intObjList);
                        while (instObjItr.next()) {
                            BusinessObject instObjFinal = instObjItr.obj();
                            String instObjFinalID = instObjFinal.getObjectId(_context);

                            if (!objIDList.contains(instObjFinalID)) {
                                objIDList.addElement(instObjFinalID);
                                getAllObjectsAcrossStreamsToRename(_context, instObjFinal, ObjectsList, objIDList, false);
                                ObjectsList.addElement(instObjFinal);
                            }
                        }
                    }
                }

                if (!objIDList.contains(busid)) {
                    objIDList.addElement(busid);
                    ObjectsList.addElement(BusObject);
                }
            }
        } // Fix for FindBugs issue RuntimeException capture: Suchit Gangurde: 28 Feb 2017: START
        catch (RuntimeException ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in getAllObjectsAcrossStreamsToRename: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End

        } // Fix for FindBugs issue RuntimeException capture: Suchit Gangurde: 28 Feb 2017: END
        catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in getAllObjectsAcrossStreamsToRename: ", ex);
            // TIGTK-5405 - 18-04-2017 - PTE - End

        }
    }// END of method getAllObjectsAcrossStreamsToRename

    /**
     * This method renames dependent docs. If oldInstanceName (arg 4) has length>0 it means that only instance is to be renamed. else, family is renamed, leading to renaming of family dependent doc as
     * well as instance dependent doc.
     */
    protected void renameDependentDocs(Context _context, BusinessObject bo, String oldBusName, String newBusName, String oldNameForFileOperation, String newNameForFileOperation,
            String oldInstanceName, StringBuffer renamedObjIds) throws Exception {
        try {
            String attribName = MCADMxUtil.getActualNameForAEFData(_context, "attribute_CADObjectName");
            Hashtable relationsList = _generalUtil.getAllWheareUsedRelationships(_context, bo, true, MCADServerSettings.DERIVEDOUTPUT_LIKE);
            Enumeration allRels = relationsList.keys();
            String familyName = "";
            boolean bRenameOnlyInstance = false;

            if (oldInstanceName.length() > 0) {
                bRenameOnlyInstance = true;
                bo.open(_context);
                familyName = bo.getName();
                bo.close(_context);
            }

            while (allRels.hasMoreElements()) {
                Relationship rel = (Relationship) allRels.nextElement();
                String end = (String) relationsList.get(rel);

                BusinessObject ddBO = null;
                rel.open(_context);
                // The other object is at the other "end"
                if (end.equals("from")) {
                    ddBO = rel.getTo();
                } else {
                    ddBO = rel.getFrom();
                }

                ddBO.open(_context);
                String boName = ddBO.getName();
                if (boName.contains(newNameForFileOperation)) {
                    int indexOfDot = boName.indexOf(".");

                    if (indexOfDot != -1) {

                        if (newNameForFileOperation.equals(boName.substring(0, indexOfDot))) {
                            continue;
                        }
                    }

                }
                String cadType = _util.getCADTypeForBO(_context, ddBO);
                rel.close(_context);

                if (bRenameOnlyInstance) {
                    // String tmpOldInstanceName = _generalUtil.getGeneratedInstanceName(familyName, oldInstanceName);
                    String tmpOldInstanceName = familyName + "-" + oldInstanceName;
                    if (!_integName.equalsIgnoreCase("MxPro") && MCADUtil.areStringsEqual(boName, tmpOldInstanceName, isSystemCaseSensitive)) {
                        // String changedName = _generalUtil.getGeneratedInstanceName(familyName, newName);
                        String changedName = familyName + "-" + newBusName;

                        _util.setRelationshipAttributeValue(_context, rel, attribName, changedName);

                        validateBusObjectName(changedName, cadType);

                        if (!alreadyRenamedObjList.contains(ddBO.getObjectId())) {
                            _util.renameObject(_context, ddBO, changedName);
                            alreadyRenamedObjList.add(ddBO.getObjectId());
                        }

                    } else if (_integName.equalsIgnoreCase("MxPro") && !MCADUtil.areStringsEqual(boName, tmpOldInstanceName, isSystemCaseSensitive)) {
                        _util.setRelationshipAttributeValue(_context, rel, attribName, newBusName);

                        if (!alreadyRenamedObjList.contains(ddBO.getObjectId())) {
                            _util.renameObject(_context, ddBO, newBusName);
                            alreadyRenamedObjList.add(ddBO.getObjectId());
                        }
                    }
                } else {
                    String changedName = _util.replace(boName, oldNameForFileOperation, newNameForFileOperation);

                    if (changedName.equals(boName))
                        changedName = MCADMxUtil.getUniqueObjectName(_integName);

                    if (MCADUtil.areStringsEqual(boName, oldBusName, isSystemCaseSensitive))
                        _util.setRelationshipAttributeValue(_context, rel, attribName, changedName);
                    else if (boName.startsWith(oldBusName + "."))
                        _util.setRelationshipAttributeValue(_context, rel, attribName, newBusName);

                    validateBusObjectName(changedName, cadType);
                    if (!alreadyRenamedObjList.contains(ddBO.getObjectId())) {
                        _util.renameObject(_context, ddBO, changedName);
                        alreadyRenamedObjList.add(ddBO.getObjectId());
                    }

                }

                ddBO.close(_context);
            }
        } // Fix for FindBugs issue RuntimeException capture: Suchit Gangurde: 28 Feb 2017: START
        catch (RuntimeException ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in renameDependentDocs: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End
        } // Fix for FindBugs issue RuntimeException capture: Suchit Gangurde: 28 Feb 2017: END
        catch (Exception e) {
            MCADServerException.createException(e.getMessage(), e);
        }
    }// END of method renameDependentDocs

    /**
     * This method is copy of OOTB method of JPO MCADRenameBase.
     * @throws Exception
     * @author Steepgraph Systems
     */
    protected void checkForTitleUniqueness(Context _context, String newTitle, String cadType, BusinessObject busObject, String newName) throws Exception {
        ArrayList alreadyProcessedFamily = new ArrayList();
        String attrTitle = MCADMxUtil.getActualNameForAEFData(_context, "attribute_Title");

        if (_globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE)) {
            String objectIds[] = new String[1];
            objectIds[0] = busObject.getObjectId();

            String relInstanceOf = MCADMxUtil.getActualNameForAEFData(_context, "relationship_InstanceOf");
            String familyIds = "to[" + relInstanceOf + "].from.id";

            StringList busSelectList1 = new StringList();
            busSelectList1.add(familyIds);

            BusinessObjectWithSelectList busWithSelectList1 = BusinessObjectWithSelect.getSelectBusinessObjectData(_context, objectIds, busSelectList1);

            BusinessObjectWithSelect busWithSelect1 = busWithSelectList1.getElement(0);

            StringList familyID = busWithSelect1.getSelectDataList(familyIds);

            if (familyID != null && familyID.size() > 0) {
                for (int j = 0; j < familyID.size(); j++) {
                    String familyId = (String) familyID.get(j);

                    if (!alreadyProcessedFamily.contains(familyId)) {
                        alreadyProcessedFamily.add(familyId);

                        String[] arrfamilyId = new String[1];
                        arrfamilyId[0] = familyId;

                        ArrayList instanceList = _generalUtil.getFamilyStructureRecursively(_context, arrfamilyId, new Hashtable(), null);
                        String[] oids = new String[instanceList.size()];

                        instanceList.toArray(oids);

                        StringList busSelectList = new StringList();
                        busSelectList.add("attribute[" + attrTitle + "]");

                        BusinessObjectWithSelectList busWithSelectList = BusinessObjectWithSelect.getSelectBusinessObjectData(_context, oids, busSelectList);

                        for (int k = 0; k < busWithSelectList.size(); k++) {
                            BusinessObjectWithSelect busWithSelect = busWithSelectList.getElement(k);

                            String instancetitle = (String) busWithSelect.getSelectData("attribute[" + attrTitle + "]");

                            if (instancetitle.equals(newTitle)) {
                                BusinessObject familyObject = new BusinessObject(familyId);
                                familyObject.open(_context);
                                Hashtable messageDetails = new Hashtable();
                                messageDetails.put("NAME", newName);
                                messageDetails.put("FAMILYNAME", familyObject.getName());
                                familyObject.close(_context);
                                MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.InstanceNameNotUniqueForFamily", messageDetails), null);
                            }
                        }
                    }
                }
            }
        }
    }// END of method checkForTitleUniqueness
     // Functionamlity for Rename CAD objects end

    /**
     * This method will create data for program for table PSS_RenameCADStructure.
     * @param context
     * @param args
     * @throws Exception
     * @author Steepgraph Systems
     */
    public MapList getMainObjectForRename(Context context, String[] args) throws Exception {

        MapList mpReturnList = new MapList();
        try {
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            String strCADObjId = (String) paramMap.get("objectId");
            Map mObjId = new HashMap();
            mObjId.put("id", "");
            mpReturnList.add(mObjId);
            mObjId = new HashMap();
            mObjId.put("id", strCADObjId);

            // final String ATTR_PSS_GEOMETRY_TYPE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_GeometryType");
            StringList selectStmts = new StringList(2);
            selectStmts.addElement(DomainConstants.SELECT_ID);
            selectStmts.addElement(DomainConstants.SELECT_NAME);
            selectStmts.addElement(DomainConstants.SELECT_CURRENT);
            StringList selectRelStmts = new StringList(1);
            selectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

            DomainObject domObjExpand = DomainObject.newInstance(context, strCADObjId);
            Map strAttrGeometry = domObjExpand.getInfo(context, selectStmts);
            String strCurrent = (String) strAttrGeometry.get(DomainConstants.SELECT_CURRENT);
            ExpansionIterator expandIter = domObjExpand.getExpansionIterator(context, DomainConstants.RELATIONSHIP_PART_SPECIFICATION, DomainConstants.TYPE_PART, selectStmts, selectRelStmts, true,
                    false, (short) 1, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, (short) 0, false, true, (short) 1, false);

            MapList mExpandList = FrameworkUtil.toMapList(expandIter, (short) 0, null, null, null, null);
            if (mExpandList.size() <= 1 && TigerConstants.STATE_INWORK_CAD_OBJECT.equals(strCurrent)) {
                mObjId.put("disableRow", "false");
            } else {
                mObjId.put("disableRow", "true");
            }
            mpReturnList.add(mObjId);
        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in getMainObjectForRename: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End
        }

        return mpReturnList;
    }// END of method getMainObjectForRename

    /**
     * This method will create data for expandProgram for table PSS_RenameCADStructure.
     * @param context
     * @param args
     * @throws Exception
     * @author Steepgraph Systems
     */
    public MapList getCADStructureForRename(Context context, String[] args) throws Exception {
        // final String TYPE_PSS_CATPRODUCT = PropertyUtil.getSchemaProperty(context, "type_PSS_CATProduct");
        final String REALTIONSHIP_CAD_SUBCOMPONENT = PropertyUtil.getSchemaProperty(context, "relationship_CADSubComponent");

        MapList mpReturnList = new MapList();
        // MapList mpListExpand = new MapList();
        try {
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            String strCADObjId = (String) paramMap.get("objectId");
            String strExpandLevels = (String) paramMap.get("emxExpandFilter");
            int expandLevel = "All".equals(strExpandLevels) ? 0 : Integer.parseInt(strExpandLevels);
            StringList selectStmts = new StringList(2);
            selectStmts.addElement(DomainConstants.SELECT_ID);
            selectStmts.addElement(DomainConstants.SELECT_NAME);
            selectStmts.addElement(DomainConstants.SELECT_CURRENT);
            StringList selectRelStmts = new StringList(1);
            selectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

            String strRelPattern = REALTIONSHIP_CAD_SUBCOMPONENT;
            boolean bisTo = false;
            boolean bisFrom = true;

            BusinessObject busObj = new BusinessObject(strCADObjId);

            DomainObject domObj = DomainObject.newInstance(context, strCADObjId);

            if (domObj.isKindOf(context, TigerConstants.TYPE_MCADDRAWING)) {
                strRelPattern = TigerConstants.RELATIONSHIP_ASSOCIATEDDRAWING;
                bisTo = true;
                bisFrom = false;
            }

            ExpansionIterator iter = busObj.getExpansionIterator(context, strRelPattern, "*", selectStmts, selectRelStmts, bisTo, bisFrom, (short) expandLevel, DomainConstants.EMPTY_STRING,

                    DomainConstants.EMPTY_STRING, (short) 0, false, true, (short) 1, false);

            MapList mpListExpand = FrameworkUtil.toMapList(iter, (short) 0, null, null, null, null);
            iter.close();

            for (int i = 0; i < mpListExpand.size(); i++) {
                Map expandedStructure = (Map) mpListExpand.get(i);
                String strObjIdExpand = (String) expandedStructure.get(DomainConstants.SELECT_ID);
                String strCurrentExpand = (String) expandedStructure.get(DomainConstants.SELECT_CURRENT);
                BusinessObject boObjExpand = new BusinessObject(strObjIdExpand);
                ExpansionIterator expandIter = boObjExpand.getExpansionIterator(context, DomainConstants.RELATIONSHIP_PART_SPECIFICATION, DomainConstants.TYPE_PART, selectStmts, selectRelStmts, true,
                        false, (short) 1, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, (short) 0, false, true, (short) 1, false);

                MapList mExpandList = FrameworkUtil.toMapList(expandIter, (short) 0, null, null, null, null);
                if (mExpandList.size() <= 1 && TigerConstants.STATE_INWORK_CAD_OBJECT.equals(strCurrentExpand)) {
                    expandedStructure.put("disableRow", "false");
                } else {
                    expandedStructure.put("disableRow", "true");
                }
                mpReturnList.add(i, expandedStructure);
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in getCADStructureForRename: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End

        }
        return mpReturnList;
    }// END of method getCADStructureForRename

    /**
     * This method will create program HTML data for column Current Name of table PSS_RenameCADStructure.
     * @param context
     * @param args
     * @throws Exception
     * @author Steepgraph Systems
     */
    public StringList getCurrentNameColumn(Context context, String[] args) throws Exception {
        StringList columnValues = new StringList();
        try {

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList relBusObjPageList = (MapList) programMap.get("objectList");
            Map mlColumnMap = (Map) programMap.get("columnMap");
            String strName = (String) mlColumnMap.get(DomainConstants.SELECT_NAME);
            for (int i = 0; i < relBusObjPageList.size(); i++) {
                Map mapObjList = (Map) relBusObjPageList.get(i);
                String strobjectId = (String) mapObjList.get(DomainConstants.SELECT_ID);
                if (!UIUtil.isNotNullAndNotEmpty(strobjectId)) {
                    columnValues.add("<div> <p>Apply to all</p> </div>");
                } else {
                    DomainObject domObj = new DomainObject(strobjectId);
                    String strObjectName = domObj.getName(context);
                    columnValues.add("<div> <a  class=\"" + strName + strobjectId + "\">" + strObjectName + " </a> </div>");
                }
            }
            // HashMap paramMap = (HashMap) programMap.get("paramList");

        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in getCurrentNameColumn: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End
        }
        return columnValues;
    }// END of method getCurrentNameColumn

    /**
     * This method will create program HTML data for column NewName of table PSS_RenameCADStructure.
     * @param context
     * @param args
     * @throws Exception
     * @author Steepgraph Systems
     */
    public StringList getNewNameColumn(Context context, String[] args) throws Exception {
        StringList columnValues = new StringList();
        String strPartName = "";
        try {

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList relBusObjPageList = (MapList) programMap.get("objectList");
            Map mlColumnMap = (Map) programMap.get("columnMap");
            String strName = (String) mlColumnMap.get(DomainConstants.SELECT_NAME);

            for (int i = 0; i < relBusObjPageList.size(); i++) {
                Map mapObjList = (Map) relBusObjPageList.get(i);
                String strobjectId = (String) mapObjList.get(DomainConstants.SELECT_ID);
                String strDisableRow = (String) mapObjList.get("disableRow");
                // Add for TIGTK-4047(CAD-BOM):start
                if (UIUtil.isNotNullAndNotEmpty(strobjectId)) {
                    DomainObject domOriginalCAD = DomainObject.newInstance(context, strobjectId);
                    StringList slConnectedPart = domOriginalCAD.getInfoList(context, "to[" + DomainConstants.RELATIONSHIP_PART_SPECIFICATION + "].from.id");
                    for (Iterator iterator = slConnectedPart.iterator(); iterator.hasNext();) {
                        String partId = (String) iterator.next();
                        DomainObject doSynchronizedPart = DomainObject.newInstance(context, partId);
                        strPartName = doSynchronizedPart.getInfo(context, DomainConstants.SELECT_NAME);

                    }
                }
                // Add for TIGTK-4047(CAD-BOM):END
                if ("".equals(strobjectId)) {
                    columnValues.add("");
                } else {
                    if ("true".equals(strDisableRow)) {
                        // CAD_BOM - TIGTK-8588 : PSE : 03-07-2017 : START
                        columnValues.add("<input type=\"text\" name=\"" + strName + strobjectId + "\" id =\"" + strName + strobjectId + i + "\"  class=\"" + strName + strobjectId
                                + "\"  onchange=\"javascript:getNewNameOnChange('" + strobjectId + "', this)\" disabled=\"disabled\"/> <br/>");
                        // CAD_BOM - TIGTK-8588 : PSE : 03-07-2017 : END
                    }

                    else {
                        // Add for TIGTK-4047(CAD-BOM):start
                        /*
                         * columnValues.add("<input type=\"text\" name=\"" + strName + "\" id =\"" + strName + i + "\"  class=\"" + strName + strobjectId + "\" value=\"" + strPartName +
                         * "\" onchange=\"javascript:getNewNameOnChange('" + strobjectId + "', this)\" disabled=\"disabled\"/> <br/>");
                         */
                        // CAD_BOM - TIGTK-8588 : PSE : 03-07-2017 : START
                        columnValues.add("<input type=\"text\" name=\"" + strName + strobjectId + "\" id =\"" + strName + strobjectId + i + "\"  class=\"" + strName + strobjectId + "\" value=\""
                                + strPartName + "\" onchange=\"javascript:getNewNameOnChange('" + strobjectId + "', this)\" disabled=\"disabled\"/> <br/>");
                        // CAD_BOM - TIGTK-8588 : PSE : 03-07-2017 : END
                        // Add for TIGTK-4047(CAD-BOM):END
                    }
                }
            }
            // HashMap paramMap = (HashMap) programMap.get("paramList");

        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in getNewNameColumn: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End
        }
        return columnValues;
    }// END of method getNewNameColumn

    /**
     * This method will create program HTML data for column RenameAsCheckbox of table PSS_RenameCADStructure.
     * @param context
     * @param args
     * @throws Exception
     * @author Steepgraph Systems
     */
    public StringList getRenameAsPartCheckBox(Context context, String[] args) throws Exception {
        StringList columnValues = new StringList();
        try {

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList relBusObjPageList = (MapList) programMap.get("objectList");
            Map mlColumnMap = (Map) programMap.get("columnMap");
            String strName = (String) mlColumnMap.get(DomainConstants.SELECT_NAME);
            for (int i = 0; i < relBusObjPageList.size(); i++) {
                Map mapObjList = (Map) relBusObjPageList.get(i);
                String strobjectId = (String) mapObjList.get(DomainConstants.SELECT_ID);
                String strDisableRow = (String) mapObjList.get("disableRow");
                String strClassName = "";
                if ("".equals(strobjectId)) {
                    strClassName = "RenameCheckBoxClass";
                    columnValues.add("<input type=\"checkbox\"  name=\"RenameCheckBox\" id =\"RenameCheckBox0\" class=\"" + strClassName + "\"  onchange=\"javascript:renameAsPartCheckOnChange('"
                            + strobjectId + "', this, '" + strClassName + "')\"  checked=\"checked\"/> <br/>");
                } else {
                    if ("true".equals(strDisableRow)) {
                        strClassName = strName + strobjectId;
                        // columnValues.add( "<input type=\"checkbox\" name=\""+strName+"\" id =\""+strName+i+"\" onchange=\"javascript:renameAsPartCheckOnChange("+strobjectId+")\"/> <br/>");
                        // CAD_BOM - TIGTK-8588 : PSE : 03-07-2017 : START
                        columnValues.add("<input type=\"checkbox\"   name=\"" + strName + strobjectId + "\" id =\"" + strName + strobjectId + i + "\" class=\"" + strClassName
                                + "\"   onchange=\"javascript:renameAsPartCheckOnChange('" + strobjectId + "', this, '" + strClassName + "')\" disabled=\"disabled\"/> <br/>");
                        // CAD_BOM - TIGTK-8588 : PSE : 03-07-2017 : END
                    } else {
                        strClassName = strName + strobjectId;
                        // columnValues.add( "<input type=\"checkbox\" name=\""+strName+"\" id =\""+strName+i+"\" onchange=\"javascript:renameAsPartCheckOnChange("+strobjectId+")\"/> <br/>");
                        // CAD_BOM - TIGTK-8588 : PSE : 03-07-2017 : START
                        columnValues.add("<input type=\"checkbox\"   name=\"" + strName + strobjectId + "\" id =\"" + strName + strobjectId + i + "\" class=\"" + strClassName
                                + "\"   onchange=\"javascript:renameAsPartCheckOnChange('" + strobjectId + "', this, '" + strClassName + "')\" checked=\"checked\"/> <br/>");
                        // CAD_BOM - TIGTK-8588 : PSE : 03-07-2017 : END
                    }
                }
            }
            // HashMap paramMap = (HashMap) programMap.get("paramList");

        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in getRenameAsPartCheckBox: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End

        }
        return columnValues;
    }// END of method getRenameAsPartCheckBox

    /**
     * This method will create program HTML data for column PartName of table PSS_RenameCADStructure.
     * @param context
     * @param args
     * @throws Exception
     * @author Steepgraph Systems
     */
    public StringList getConnectedPartNameColumn(Context context, String[] args) throws Exception {
        StringList columnValues = new StringList();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            Map mlColumnMap = (Map) programMap.get("columnMap");
            String strName = (String) mlColumnMap.get(DomainConstants.SELECT_NAME);
            MapList relBusObjPageList = (MapList) programMap.get("objectList");

            StringList selectStmts = new StringList(2);
            selectStmts.addElement(DomainConstants.SELECT_ID);
            selectStmts.addElement(DomainConstants.SELECT_NAME);
            StringList selectRelStmts = new StringList(1);
            selectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            String strRelPattern = DomainConstants.RELATIONSHIP_PART_SPECIFICATION;
            for (int i = 0; i < relBusObjPageList.size(); i++) {
                Map mapObjList = (Map) relBusObjPageList.get(i);
                String strobjectId = (String) mapObjList.get(DomainConstants.SELECT_ID);
                // Added : 07/11/2016
                if (UIUtil.isNotNullAndNotEmpty(strobjectId)) {
                    // Added : 07/11/2016
                    BusinessObject busObj = new BusinessObject(strobjectId);
                    ExpansionIterator iter = busObj.getExpansionIterator(context, strRelPattern, DomainConstants.TYPE_PART, selectStmts, selectRelStmts, true, false, (short) 1,
                            DomainConstants.EMPTY_STRING,

                            DomainConstants.EMPTY_STRING, (short) 0, false, true, (short) 1, false);

                    MapList mpReturnList = FrameworkUtil.toMapList(iter, (short) 0, null, null, null, null);

                    StringBuffer strBuf = new StringBuffer();
                    if (mpReturnList.size() > 0) {
                        Map mapnameFirst = (Map) mpReturnList.get(0);
                        strBuf.append((String) mapnameFirst.get(DomainConstants.SELECT_NAME));
                        if (mpReturnList.size() > 1) {
                            for (int j = 1; j < mpReturnList.size(); j++) {
                                Map mapname = (Map) mpReturnList.get(j);
                                strBuf.append(",");
                                strBuf.append((String) mapname.get(DomainConstants.SELECT_NAME));
                            }
                        }
                    }

                    iter.close();
                    // CAD_BOM - TIGTK-8588 : PSE : 03-07-2017 : START
                    columnValues.add("<input type=\"text\" name=\"" + strName + strobjectId + "\" id =\"" + strName + strobjectId + i + "\" value=\"" + strBuf.toString() + "\" class=\"" + strName
                            + strobjectId + "\" readonly=\"readonly\"/> <br/>");
                    // CAD_BOM - TIGTK-8588 : PSE : 03-07-2017 : END
                    // strPartName = "";
                } else {
                    columnValues.add("");
                }

            }
        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in getConnectedPartNameColumn: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End
        }

        return columnValues;
    }// END of method getConnectedPartNameColumn

    /**
     * This method will create program HTML data for column Prefix of table PSS_RenameCADStructure.
     * @param context
     * @param args
     * @throws Exception
     * @author Steepgraph Systems
     */
    public StringList getPrefixColumn(Context context, String[] args) throws Exception {
        StringList columnValues = new StringList();
        try {

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            Map mlColumnMap = (Map) programMap.get("columnMap");
            String strName = (String) mlColumnMap.get(DomainConstants.SELECT_NAME);
            MapList relBusObjPageList = (MapList) programMap.get("objectList");

            StringBuffer sbReturnCheck = new StringBuffer();
            StringBuffer sbReturnText = new StringBuffer();
            StringBuffer sbReturn = new StringBuffer();
            for (int i = 0; i < relBusObjPageList.size(); i++) {
                Map mapObjList = (Map) relBusObjPageList.get(i);
                String strobjectId = (String) mapObjList.get(DomainConstants.SELECT_ID);
                String strDisableRow = (String) mapObjList.get("disableRow");
                String strClassName = "";
                if ("".equals(strobjectId)) {
                    strClassName = "PrefixCheckboxClass";

                    sbReturnCheck.append("<input type=\"checkbox\" class=\"" + strClassName + "\"  name=\"PrefixCheckbox\" id =\"PrefixCheckbox0\" onchange=\"javascript:getCheckPrefixColumnOnChange('"
                            + strobjectId + "', this,'" + strClassName + "')\"/> ");
                    // CAD_BOM - TIGTK-8588 : PSE : 03-07-2017 : START
                    // START :: TIGTK-17427 :: ALM-6128
                    sbReturnText.append("<input type=\"text\" name=\"" + strName + strobjectId + "\" id =\"" + strName + strobjectId + i
                            + "\" class=\"PrefixTextClass\" onchange=\"javascript:rebuildView()\" oninput=\"javascript:getTextPrefixColumnOnChange('" + strobjectId
                            + "', this,'PrefixTextClass')\" disabled=\"disabled\"/>");
                    // END :: TIGTK-17427 :: ALM-6128
                    // CAD_BOM - TIGTK-8588 : PSE : 03-07-2017 : END
                    // Findbug Issue correction start
                    // Date: 22/03/2017
                    // By: Asha G.
                    sbReturn.append(sbReturnCheck.toString());
                    sbReturn.append(sbReturnText);
                    // Findbug Issue correction end
                    columnValues.add(sbReturn.toString());
                } else {
                    strClassName = strName + strobjectId;
                    if ("true".equals(strDisableRow)) {
                        // CAD_BOM - TIGTK-8588 : PSE : 03-07-2017 : START
                        sbReturnCheck.append("<input type=\"checkbox\"  name=\"check" + strName + strobjectId + "\" id =\"" + strName + strobjectId + i + "\" class=\"check" + strClassName
                                + "\" onchange=\"javascript:getCheckPrefixColumnOnChange('" + strobjectId + "', this,'check" + strClassName + "')\"  disabled=\"disabled\"/> ");
                        // START :: TIGTK-17427 :: ALM-6128
                        sbReturnText.append("<input type=\"text\" name=\"text" + strName + strobjectId + "\" id =\"" + strName + strobjectId + i + "\"  class=\"text" + strClassName
                                + "\" onchange=\"javascript:rebuildView()\" oninput=\"javascript:getTextPrefixColumnOnChange('" + strobjectId + "', this,'text" + strClassName
                                + "')\"  disabled=\"disabled\"/>");
                        // END :: TIGTK-17427 :: ALM-6128
                    } else {
                        sbReturnCheck.append("<input type=\"checkbox\"  name=\"check" + strName + strobjectId + "\" id =\"" + strName + strobjectId + i + "\" class=\"check" + strClassName
                                + "\" onchange=\"javascript:getCheckPrefixColumnOnChange('" + strobjectId + "', this,'check" + strClassName + "')\" /> ");
                        // START :: TIGTK-17427 :: ALM-6128
                        sbReturnText.append("<input type=\"text\" name=\"text" + strName + strobjectId + "\" id =\"" + strName + strobjectId + i + "\"  class=\"text" + strClassName
                                + "\" onchange=\"javascript:rebuildView()\" oninput=\"javascript:getTextPrefixColumnOnChange('" + strobjectId + "', this,'text" + strClassName
                                + "')\" disabled=\"disabled\"/>");
                        // END :: TIGTK-17427 :: ALM-6128
                        // CAD_BOM - TIGTK-8588 : PSE : 03-07-2017 : END
                    }
                    // Findbug Issue correction start
                    // Date: 22/03/2017
                    // By: Asha G.
                    sbReturn.append(sbReturnCheck.toString());
                    sbReturn.append(sbReturnText.toString());
                    // Findbug Issue correctionend
                    columnValues.add(sbReturn.toString());
                }

                sbReturnCheck.setLength(0);
                sbReturnText.setLength(0);
                sbReturn.setLength(0);
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in getPrefixColumn: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End
        }
        return columnValues;
    }// END of method getPrefixColumn

    /**
     * This method will create program HTML data for column Suffix of table PSS_RenameCADStructure.
     * @param context
     * @param args
     * @throws Exception
     * @author Steepgraph Systems
     */
    public StringList getSuffixColumn(Context context, String[] args) throws Exception {
        StringList columnValues = new StringList();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            Map mlColumnMap = (Map) programMap.get("columnMap");
            String strName = (String) mlColumnMap.get(DomainConstants.SELECT_NAME);
            MapList relBusObjPageList = (MapList) programMap.get("objectList");
            String strSuffixValues = EnoviaResourceBundle.getProperty(context, SUITE_KEY, context.getLocale(), "emxIEFDesignCenter.SuffixRange.PSS_RenameCADPart");
            String[] arrSuffixValues = strSuffixValues.split(",");

            StringBuffer sbReturnCombo = new StringBuffer();
            StringBuffer sbReturnText = new StringBuffer();
            StringBuffer sbReturn = new StringBuffer();
            sbReturnCombo.append("<option value=\"\"> </option>");
            for (int i = 0; i < arrSuffixValues.length; i++) {
                sbReturnCombo.append("<option value=\"" + arrSuffixValues[i].trim() + "\">" + arrSuffixValues[i].trim() + "</option>");
            }
            sbReturnCombo.append("</select>");
            for (int i = 0; i < relBusObjPageList.size(); i++) {
                Map mapObjList = (Map) relBusObjPageList.get(i);
                String strobjectId = (String) mapObjList.get(DomainConstants.SELECT_ID);
                String strDisableRow = (String) mapObjList.get("disableRow");

                if ("".equals(strobjectId)) {
                    // 03-07-2017
                    // START :: TIGTK-17427 :: ALM-6128
                    sbReturnText.append("<input type=\"text\" name=\"textHeadSuffixName\" id =\"" + strName + i
                            + "\" class=\"textHeadSuffixClass\" onchange=\"javascript:rebuildView()\" oninput=\"javascript:getSuffixTextColumnOnChange('" + strobjectId
                            + "', this,'textHeadSuffixClass')\"  />");
                    // END :: TIGTK-17427 :: ALM-6128
                    // Findbug Issue correction start
                    // Date: 22/03/2017
                    // By: Asha G.
                    sbReturn.append(sbReturnText.toString());
                    sbReturn.append(" <span> or </span>  ");
                    sbReturn.append("<select name=\"comboHeadSuffixName\" style=\"width:70px;\"  class=\"comboHeadSuffixClass\" onchange=\"javascript:getSuffixComboColumnOnChange('" + strobjectId
                            + "', this,'comboHeadSuffixClass')\"  >");
                    // Findbug Issue correction
                    sbReturn.append(sbReturnCombo.toString());
                    columnValues.add(sbReturn.toString());
                } else {
                    if ("true".equals(strDisableRow)) {
                        // CAD_BOM - TIGTK-8588 : PSE : 03-07-2017 : START
                        // START :: TIGTK-17427 :: ALM-6128
                        sbReturnText.append("<input type=\"text\" name=\"" + strName + strobjectId + "\" id =\"" + strName + strobjectId + i + "\" class=\"text" + strName + strobjectId
                                + "\" onchange=\"javascript:rebuildView()\" oninput=\"javascript:getSuffixTextColumnOnChange('" + strobjectId + "', this, 'text" + strName + strobjectId
                                + "')\"  disabled=\"disabled\"/>");
                        // END :: TIGTK-17427 :: ALM-6128
                        // CAD_BOM - TIGTK-8588 : PSE : 03-07-2017 : END
                        // Findbug Issue correction start
                        // Date: 22/03/2017
                        // By: Asha G.
                        sbReturn.append(sbReturnText.toString());
                        sbReturn.append(" <span> or </span> ");
                        // CAD_BOM - TIGTK-8588 : PSE : 03-07-2017 : START
                        sbReturn.append("<select name=\"combo" + strName + strobjectId + "\" style=\"width:70px;\"  class=\"combo" + strName + strobjectId
                                + "\" onchange=\"javascript:getSuffixComboColumnOnChange('" + strobjectId + "', this,'combo" + strName + strobjectId + "')\"  disabled=\"disabled\">");
                        // CAD_BOM - TIGTK-8588 : PSE : 03-07-2017 : END
                        sbReturn.append(sbReturnCombo.toString());

                    } else {
                        // CAD_BOM - TIGTK-8588 : PSE : 03-07-2017 : START
                        // START :: TIGTK-17427 :: ALM-6128
                        sbReturnText.append("<input type=\"text\" name=\"text" + strName + strobjectId + "\" id =\"" + strName + strobjectId + i + "\" class=\"text" + strName + strobjectId
                                + "\" onchange=\"javascript:rebuildView()\" oninput=\"javascript:getSuffixTextColumnOnChange('" + strobjectId + "', this,'text" + strName + strobjectId + "')\"  />");
                        // END :: TIGTK-17427 :: ALM-6128
                        sbReturn.append(sbReturnText.toString());
                        sbReturn.append(" <span> or </span>  ");
                        sbReturn.append("<select name=\"combo" + strName + strobjectId + "\" style=\"width:70px;\"  class=\"combo" + strName + strobjectId
                                + "\" onchange=\"javascript:getSuffixComboColumnOnChange('" + strobjectId + "', this,'combo" + strName + strobjectId + "')\"  >");
                        // CAD_BOM - TIGTK-8588 : PSE : 03-07-2017 : END
                        sbReturn.append(sbReturnCombo.toString());
                        // Findbug Issue correction
                    }
                    columnValues.add(sbReturn.toString());
                }

                sbReturnText.setLength(0);
                sbReturn.setLength(0);
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in getSuffixColumn: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End
        }
        return columnValues;
    }// END of method getPrefixColumn

    // Entry point
    public void executeCustom(Context _context, Hashtable resultAndStatusTable) throws MCADException {
        String attrFileSouceName = MCADMxUtil.getActualNameForAEFData(_context, "attribute_IEF-FileSource");
        boolean isObjectAndFilenameDifferent = _globalConfig.isObjectAndFileNameDifferent();

        String newName = (String) _argumentsTable.get(MCADServerSettings.NEW_NAME);
        String instanceName = (String) _argumentsTable.get(MCADServerSettings.INSTANCE_NAME);
        String priority = (String) _argumentsTable.get(MCADServerSettings.MESSAGE_PRIORITY);
        isSystemCaseSensitive = (Boolean.valueOf((String) _argumentsTable.get(MCADServerSettings.CASE_SENSITIVE_FLAG))).booleanValue();
        String paramsToReturn = "";
        alreadyRenamedObjList = new Vector();

        try {
            _integName = (String) _argumentsTable.get(MCADAppletServletProtocol.INTEGRATION_NAME);

            _busObject.open(_context);

            String busType = _busObject.getTypeName();

            String oldName = (String) _argumentsTable.get(MCADServerSettings.OLD_NAME);

            if (oldName == null || oldName.equals(""))
                oldName = _busObject.getName();

            String cadType = _util.getCADTypeForBO(_context, _busObject);

            _busObject.close(_context);

            resultAndStatusTable.put(MCADServerSettings.NEW_NAME, newName);

            StringBuffer renamedObjIds = new StringBuffer();
            // Renaming a ProE Object
            if (_integName.equalsIgnoreCase("MxPro")) {
                if (_globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE)) {
                    String oldProeInstanceName = _generalUtil.getIndivisualInstanceName(oldName);
                    BusinessObject familyObject = null;

                    if (_util.isMajorObject(_context, _busObjectID))// _globalConfig.isMajorType(busType)) [NDM]
                    {
                        BusinessObjectList minorObjects = _util.getMinorObjects(_context, _busObject);
                        BusinessObject latestMinor = (BusinessObject) minorObjects.lastElement();

                        latestMinor.open(_context);

                        String latestMinorID = latestMinor.getObjectId(_context);

                        latestMinor.close(_context);

                        latestMinorID = _util.getLatestRevisionID(_context, latestMinorID);
                        latestMinor = new BusinessObject(latestMinorID);

                        familyObject = _generalUtil.getFamilyObjectForInstance(_context, latestMinor);
                    } else {
                        familyObject = _generalUtil.getFamilyObjectForInstance(_context, _busObject);
                    }

                    familyObject.open(_context);
                    String familyName = familyObject.getName();

                    if (oldProeInstanceName.equalsIgnoreCase(familyName)) {
                        _busObject = familyObject;
                        oldName = familyName;
                        cadType = _util.getCADTypeForBO(_context, familyObject);
                    }

                    familyObject.close(_context);
                }
            }

            boolean instanceRename = false;

            if (_globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
                instanceRename = true;

            // to get all list of all objects to process
            BusinessObjectList objectsList = new BusinessObjectList();
            Vector objIDList = new Vector();

            getAllObjectsAcrossStreamsToRename(_context, _busObject, objectsList, objIDList, instanceRename);

            String attrTitle = MCADMxUtil.getActualNameForAEFData(_context, "attribute_Title");
            String oldTitle = _busObject.getAttributeValues(_context, attrTitle).getValue();
            String newTitle = "";

            if (instanceRename) {
                if (isObjectAndFilenameDifferent) {
                    String familyNameFromTitle = _generalUtil.getFamilyNameFromObjectName(oldTitle, true);
                    newTitle = MCADUtil.getDisplayNameForInstance(familyNameFromTitle, newName);
                } else {
                    String familyNameFromTitle = _generalUtil.getFamilyNameFromObjectName(oldTitle);

                    newTitle = MCADUtil.getNameForInstance(_globalConfig, familyNameFromTitle, newName);
                }
            }

            // Assumption : For instanceRename, 'objectsList' will have only instance revisions stream.
            BusinessObjectItr objItr = new BusinessObjectItr(objectsList);

            while (objItr.next()) {
                BusinessObject busObject = null;

                busObject = objItr.obj();

                if (!busObject.isOpen())
                    busObject.open(_context);

                boolean isRenamed = false;
                String currentBusOldName = busObject.getName();
                String currentCadType = _util.getCADTypeForBO(_context, busObject);
                String busID = busObject.getObjectId();
                busObject.close(_context);

                BusinessObject renamedObj = busObject;

                if (instanceRename) {
                    Vector fileNames = new Vector();

                    try {
                        String cadType1 = _util.getCADTypeForBO(_context, busObject);
                        String formatName = _generalUtil.getFormatsForType(_context, busObject.getTypeName(), cadType1);
                        FileList fileList = busObject.getFiles(_context, formatName);
                        FileItr itr = new FileItr(fileList);
                        while (itr.next()) {
                            String fName = itr.obj().getName();
                            fileNames.addElement(fName);
                        }

                        if (fileNames.size() == 0) {
                            fileNames.addElement("");
                        }

                        if (!fileNames.isEmpty()) {
                            paramsToReturn = fileNames.toString();
                        }
                    } catch (Exception ex) {
                        // TIGTK-5405 - 19-04-2017 - PTE - START
                        logger.error("Error in executeCustom: ", ex);
                        throw ex;
                        // TIGTK-5405 - 19-04-2017 - PTE - End
                    }

                    String actualNewName = getNewNameForInstanceRename(newName, currentBusOldName);
                    String actualOldBusName = _generalUtil.getIndivisualInstanceName(currentBusOldName);

                    String newNameForFileOperation = actualNewName;

                    checkForTitleUniqueness(_context, newTitle, cadType, busObject, newName);

                    if (isObjectAndFilenameDifferent) {
                        String instanceNameFromTitle = MCADUtil.getIndivisualInstanceName(oldTitle, true);

                        String indivisualInstanceName = _generalUtil.getIndivisualInstanceName(currentBusOldName);

                        actualNewName = indivisualInstanceName.replace(instanceNameFromTitle, newName);

                        actualNewName = getNewNameForInstanceRename(actualNewName, currentBusOldName);

                        actualOldBusName = instanceNameFromTitle;

                        String familyTitle = _generalUtil.getFamilyNameFromObjectName(oldTitle, true);

                        newNameForFileOperation = MCADUtil.getDisplayNameForInstance(familyTitle, newName);
                    }

                    if (!newName.equals(actualOldBusName)) {
                        isRenamed = true;
                        // get name changed object
                        if (!actualNewName.equals(currentBusOldName)) {
                            // do Name Validation
                            validateBusObjectName(actualNewName, currentCadType);
                            if (!alreadyRenamedObjList.contains(busObject.getObjectId())) {
                                renamedObj = _util.renameObject(_context, busObject, actualNewName);
                                alreadyRenamedObjList.add(busObject.getObjectId());
                            }

                        }

                        // rename dependent docs if any
                        renameDependentDocs(_context, renamedObj, currentBusOldName, actualNewName, oldName, newNameForFileOperation, instanceName.trim(), renamedObjIds);

                        // set Parent Instance attribute on Instance Of relationship for nested instance if parent instance is renamed
                        String renamedObjID = renamedObj.getObjectId(_context);
                        String instanceOf = MCADMxUtil.getActualNameForAEFData(_context, "relationship_InstanceOf");
                        String parentInstance = MCADMxUtil.getActualNameForAEFData(_context, "attribute_ParentInstance");
                        String[] oids = new String[1];
                        oids[0] = renamedObjID;
                        StringList busSelectList = new StringList(5);

                        String selectOnInst = "to[" + instanceOf + "].from.from[" + instanceOf + "].";

                        busSelectList.add(selectOnInst + "id");
                        busSelectList.add(selectOnInst + "attribute[" + parentInstance + "]");

                        BusinessObjectWithSelectList busWithSelectList = BusinessObjectWithSelect.getSelectBusinessObjectData(_context, oids, busSelectList);
                        BusinessObjectWithSelect busWithSelect = busWithSelectList.getElement(0);
                        StringList relIdList = (StringList) busWithSelect.getSelectDataList(selectOnInst + "id");
                        StringList attrList = (StringList) busWithSelect.getSelectDataList(selectOnInst + "attribute[" + parentInstance + "]");

                        if (attrList != null) {
                            for (int b = 0; b < attrList.size(); b++) {
                                String attrParentInst = (String) attrList.elementAt(b);
                                if (attrParentInst.equals(actualOldBusName)) {
                                    String relId = (String) relIdList.elementAt(b);
                                    Relationship nestedInstRel = new Relationship(relId);
                                    String indivInstName = _generalUtil.getIndivisualInstanceName(actualNewName);

                                    if (isObjectAndFilenameDifferent)
                                        indivInstName = newName;

                                    _util.setRelationshipAttributeValue(_context, nestedInstRel, parentInstance, indivInstName);
                                }
                            }
                        }
                    }

                    // get actual server side operations for rename done
                    // Set file source attribute for Rename operation
                    _util.setAttributeValue(_context, renamedObj, attrFileSouceName, MCADAppletServletProtocol.FILESOURCE_RENAME);

                    boolean renamedFromRqdOnFamilyLike = true;

                    _generalUtil.doActualRename(_context, renamedObj, actualNewName, currentBusOldName, newNameForFileOperation, actualOldBusName, renamedFromRqdOnFamilyLike, true);
                } else {
                    String actualNewName = "";

                    if (isObjectAndFilenameDifferent && oldTitle.contains(".") && newName.contains(".") && oldTitle.substring(0, oldTitle.lastIndexOf(".")).equals(currentBusOldName))
                        actualNewName = newName.substring(0, newName.lastIndexOf("."));
                    else
                        actualNewName = _util.replace(currentBusOldName, oldName, newName);

                    String actualOldBusName = currentBusOldName;

                    boolean renamedFromRqdOnFamilyLike = true;

                    if (_globalConfig.isTypeOfClass(currentCadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE)) {
                        String oldFamilyNameFromInstanceName = _generalUtil.getFamilyNameFromObjectName(currentBusOldName);

                        String newFamilyBusName = "";
                        if (isObjectAndFilenameDifferent && oldTitle.contains(".") && newName.contains(".") && oldTitle.substring(0, oldTitle.lastIndexOf(".")).equals(oldFamilyNameFromInstanceName))
                            newFamilyBusName = newName.substring(0, newName.lastIndexOf("."));
                        else
                            newFamilyBusName = _util.replace(oldFamilyNameFromInstanceName, oldName, newName);

                        boolean isProeGenericInstance = false;
                        renamedFromRqdOnFamilyLike = false;

                        if (_integName.equalsIgnoreCase("MxPro")) {
                            String proeInstanceName = _generalUtil.getIndivisualInstanceName(currentBusOldName);
                            if (proeInstanceName.equalsIgnoreCase(oldName)) {
                                isProeGenericInstance = true;
                                renamedFromRqdOnFamilyLike = true;
                            }
                        }

                        actualNewName = getNewNameForFamilyRename(newFamilyBusName, currentBusOldName, isProeGenericInstance);

                        isRenamed = false;
                        // get name changed object
                        if (!actualNewName.equals(currentBusOldName)) {
                            // do Name Validation
                            validateBusObjectName(actualNewName, currentCadType);

                            if (!alreadyRenamedObjList.contains(busObject.getObjectId())) {
                                renamedObj = _util.renameObject(_context, busObject, actualNewName);
                                alreadyRenamedObjList.add(busObject.getObjectId());
                            }

                        }

                        // rename dependent docs if any
                        renameDependentDocs(_context, renamedObj, currentBusOldName, actualNewName, oldName, newName, instanceName.trim(), renamedObjIds);
                    } else if (!oldName.equals(newName)) {
                        isRenamed = true;

                        // get name changed object
                        if (!actualNewName.equals(currentBusOldName)) {
                            // do Name Validation
                            validateBusObjectName(actualNewName, currentCadType);

                            if (!alreadyRenamedObjList.contains(busObject.getObjectId())) {
                                renamedObj = _util.renameObject(_context, busObject, actualNewName);
                                alreadyRenamedObjList.add(busObject.getObjectId());
                            }

                        }

                        // rename dependent docs if any
                        renameDependentDocs(_context, renamedObj, currentBusOldName, actualNewName, oldName, newName, instanceName.trim(), renamedObjIds);

                        _util.setAttributeValue(_context, renamedObj, attrFileSouceName, MCADAppletServletProtocol.FILESOURCE_RENAME);
                    } else {
                        _util.setAttributeValue(_context, renamedObj, attrFileSouceName, MCADAppletServletProtocol.FILESOURCE_RENAME);
                    }

                    // get actual server side operations for rename done
                    // Set file source attribute for Rename operation
                    _generalUtil.doActualRename(_context, renamedObj, actualNewName, actualOldBusName, newName, oldName, renamedFromRqdOnFamilyLike, true);
                }

                if (isRenamed) {
                    renamedObjIds.append(busID);
                    renamedObjIds.append(MCADAppletServletProtocol.IEF_SEPERATOR_ONE);
                    renamedObjIds.append(oldName);
                    renamedObjIds.append(MCADAppletServletProtocol.IEF_SEPERATOR_TWO);
                }
            }

            resultAndStatusTable.put(MCADServerSettings.JPO_EXECUTION_RESULT, paramsToReturn + MCADAppletServletProtocol.IEF_SEPERATOR_TWO + renamedObjIds);
            resultAndStatusTable.put(MCADServerSettings.OBJECT_ID_LIST, objIDList);

            if (_globalConfig.isBatchProcessorForRenameEnabled()) {
                resultAndStatusTable.put(MCADServerSettings.OBJECT_ID, _busObject.getObjectId(_context));
                resultAndStatusTable.put(MCADServerSettings.MESSAGE_PRIORITY, priority);
                resultAndStatusTable.put(MCADServerSettings.SELECTED_OBJECTID_LIST, objectsList);
            }
        } catch (Exception e) {
            String error = e.getMessage();
            logger.error("[MCADRename.executeCustom] Exception occured- ", error);
            MCADServerException.createException(error, e);
        }
    }// END of executeCustom method

    /**
     * Method to rename PDF files when rename CADRawing TIGTK-17327 :: ALM-6162
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public int renameFilesofDOForTitleblock(Context context, String args[]) throws Exception {
        logger.debug("::::::: ENTER :: renameFilesofDOForTitleblock ::::::::::");
        int iReturn = 0;
        try {
            String strDOid = args[0];
            String strOldName = args[1];
            String strNewName = args[2];
            if (!strOldName.equals(strNewName)) {
                DomainObject doDerivedOutput = DomainObject.newInstance(context, strDOid);
                StringList slCADObjects = doDerivedOutput.getInfoList(context, "to[" + TigerConstants.RELATIONSHIP_DERIVEDOUTPUT + "].from.id");
                if (slCADObjects == null || slCADObjects.isEmpty())
                    return 0;
                MCADMxUtil mxUtil = new MCADMxUtil(context, new MCADServerResourceBundle("en-us"), new IEFGlobalCache());
                String strMajorCADOID = (String) slCADObjects.get(0);
                strMajorCADOID = mxUtil.isMajorObject(context, strMajorCADOID) ? strMajorCADOID : (String) slCADObjects.get(1);
                StringList slObjSelectables = new StringList(2);
                slObjSelectables.add(TigerConstants.SELECT_FROM_PSS_PDF_ARCHIVE_TO_ID);
                slObjSelectables.add("from[" + TigerConstants.RELATIONSHIP_VIEWABLE + "].to.id");
                BusinessObject boMajor = new BusinessObject(strMajorCADOID);
                BusinessObjectWithSelect bows = boMajor.select(context, slObjSelectables);
                String strPDFArchiveOID = bows.getSelectData(TigerConstants.SELECT_FROM_PSS_PDF_ARCHIVE_TO_ID);

                if (UIUtil.isNotNullAndNotEmpty(strPDFArchiveOID)) {
                    // PDF Archive rename
                    StringList slPDFArchiveFiles = new StringList();
                    String strWorkspace = TigerUtils.getWorkspaceForBus(context, context.createWorkspace(), strPDFArchiveOID);
                    BusinessObject boPdfArchive = new BusinessObject(strPDFArchiveOID);
                    // checkout files
                    FileList flFiles = TigerUtils.getValidPDFSheets(context, boPdfArchive);
                    FileItr fileItr = new FileItr(flFiles);
                    matrix.db.File file = null;
                    while (fileItr.next()) {
                        file = fileItr.obj();
                        slPDFArchiveFiles.add(file.getName());
                    }
                    if (!flFiles.isEmpty()) {
                        boPdfArchive.checkoutFiles(context, false, FileFormat.PDF(), flFiles, strWorkspace);
                    }
                    // delete files with old name
                    TigerUtils.deleteFiles(context, strPDFArchiveOID, FileFormat.PDF(), slPDFArchiveFiles);
                    // Rename and checkin files with newname
                    String strNewModifiedName = DomainConstants.EMPTY_STRING;
                    for (Object objPDF : slPDFArchiveFiles) {
                        String strName = (String) objPDF;
                        File pdfFile = new File(strWorkspace + java.io.File.separator + (String) strName);
                        strNewModifiedName = strName.replaceAll(strOldName, strNewName);
                        File fNew = new File(strWorkspace, strNewModifiedName);
                        Files.move(pdfFile.toPath(), fNew.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        boPdfArchive.checkinFile(context, true, true, DomainConstants.EMPTY_STRING, FileFormat.PDF(), strNewModifiedName, strWorkspace);
                    }
                    boPdfArchive.open(context);
                    String strType = boPdfArchive.getTypeName();
                    String strRevision = boPdfArchive.getRevision();
                    String strVault = boPdfArchive.getVault();
                    String strPolicy = boPdfArchive.getPolicy(context).getName();
                    boPdfArchive.close(context);
                    try {
                        TigerUtils.pushContextToSuperUser(context);
                        boPdfArchive.change(context, strType, strNewName, strRevision, strVault, strPolicy);
                    } finally {
                        ContextUtil.popContext(context);
                    }
                    TigerUtils.cleanUp(new File(strWorkspace));
                    // END - PDF Archihve

                    // Viewable Thumbnails
                    String strThumbnailOID = bows.getSelectData("from[" + TigerConstants.RELATIONSHIP_VIEWABLE + "].to.id");
                    strWorkspace = TigerUtils.getWorkspaceForBus(context, context.createWorkspace(), strThumbnailOID);
                    StringList slThumbnailFiles = new StringList();
                    StringList slPNGFiles = new StringList();
                    BusinessObject boThumbnail = new BusinessObject(strThumbnailOID);
                    FileList flThumbnails = boThumbnail.getFiles(context, FileFormat.THUMBNAIL()); // Thumbnail JPG files
                    flFiles = boThumbnail.getFiles(context, FileFormat.PNG()); // PNG files
                    if (!flThumbnails.isEmpty()) {
                        boThumbnail.checkoutFiles(context, false, FileFormat.THUMBNAIL(), flThumbnails, strWorkspace);
                    }
                    if (!flFiles.isEmpty()) {
                        boThumbnail.checkoutFiles(context, false, FileFormat.PNG(), flFiles, strWorkspace);
                    }
                    // checkout files
                    fileItr = new FileItr(flFiles); // PNG files
                    while (fileItr.next()) {
                        file = fileItr.obj();
                        slPNGFiles.add(file.getName());
                    }
                    fileItr = new FileItr(flThumbnails); // THUMBNAIL files
                    while (fileItr.next()) {
                        file = fileItr.obj();
                        slThumbnailFiles.add(file.getName());
                    }
                    // delete files with old name
                    TigerUtils.deleteFiles(context, strThumbnailOID, FileFormat.THUMBNAIL(), slThumbnailFiles);
                    TigerUtils.deleteFiles(context, strThumbnailOID, FileFormat.PNG(), slPNGFiles);
                    // Rename and checkin files with newname
                    for (Object objJPG : slThumbnailFiles) {
                        String strName = (String) objJPG;
                        File jpgFile = new File(strWorkspace + java.io.File.separator + (String) strName);
                        strNewModifiedName = strName.replaceAll(strOldName, strNewName);
                        File fNew = new File(strWorkspace, strNewModifiedName);
                        Files.move(jpgFile.toPath(), fNew.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        boThumbnail.checkinFile(context, true, true, DomainConstants.EMPTY_STRING, FileFormat.THUMBNAIL(), strNewModifiedName, strWorkspace);
                    }
                    for (Object objPNG : slPNGFiles) {
                        String strName = (String) objPNG;
                        File pngFile = new File(strWorkspace + java.io.File.separator + (String) strName);
                        strNewModifiedName = strName.replaceAll(strOldName, strNewName);
                        File fNew = new File(strWorkspace, strNewModifiedName);
                        Files.move(pngFile.toPath(), fNew.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        boThumbnail.checkinFile(context, true, true, DomainConstants.EMPTY_STRING, FileFormat.PNG(), strNewModifiedName, strWorkspace);
                    }
                    TigerUtils.cleanUp(new File(strWorkspace));
                    // END - Viewable Thumbnails

                    // Derived Output files rename
                    strWorkspace = TigerUtils.getWorkspaceForBus(context, context.createWorkspace(), strDOid);
                    StringList slGenericFiles = new StringList();
                    StringList slPDFFiles = new StringList();
                    BusinessObject boDerivedOutput = new BusinessObject(strDOid);
                    FileList flPDFs = boDerivedOutput.getFiles(context, FileFormat.PDF()); // pdf files
                    flFiles = boDerivedOutput.getFiles(context, FileFormat.GENERIC()); // generic files
                    if (!flPDFs.isEmpty()) {
                        boDerivedOutput.checkoutFiles(context, false, FileFormat.PDF(), flPDFs, strWorkspace);
                    }
                    if (!flFiles.isEmpty()) {
                        boDerivedOutput.checkoutFiles(context, false, FileFormat.GENERIC(), flFiles, strWorkspace);
                    }

                    // checkout files
                    fileItr = new FileItr(flFiles); // generic files
                    while (fileItr.next()) {
                        file = fileItr.obj();
                        slGenericFiles.add(file.getName());
                    }
                    fileItr = new FileItr(flPDFs); // pdf files
                    while (fileItr.next()) {
                        file = fileItr.obj();
                        slPDFFiles.add(file.getName());
                    }
                    // delete files with old name
                    TigerUtils.deleteFiles(context, strDOid, FileFormat.GENERIC(), slGenericFiles);
                    TigerUtils.deleteFiles(context, strDOid, FileFormat.PDF(), slPDFFiles);
                    // Rename and checkin files with newname
                    for (Object objGenericFile : slGenericFiles) {
                        String strName = (String) objGenericFile;
                        if (strName.endsWith(".cad") || strName.endsWith(".lst")) {
                            continue;
                        }
                        File genericFile = new File(strWorkspace + java.io.File.separator + (String) strName);
                        strNewModifiedName = strName.replaceAll(strOldName, strNewName);
                        File fNew = new File(strWorkspace, strNewModifiedName);
                        Files.move(genericFile.toPath(), fNew.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        boDerivedOutput.checkinFile(context, true, true, DomainConstants.EMPTY_STRING, FileFormat.GENERIC(), strNewModifiedName, strWorkspace);
                    }
                    for (Object objPDF : slPDFFiles) {
                        String strName = (String) objPDF;
                        File pdfFile = new File(strWorkspace + java.io.File.separator + (String) strName);
                        strNewModifiedName = strName.replaceAll(strOldName, strNewName);
                        File fNew = new File(strWorkspace, strNewModifiedName);
                        Files.move(pdfFile.toPath(), fNew.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        boDerivedOutput.checkinFile(context, true, true, DomainConstants.EMPTY_STRING, FileFormat.PDF(), strNewModifiedName, strWorkspace);
                    }
                    TigerUtils.cleanUp(new File(strWorkspace));
                    // END - Derived Output
                }
            }
        } catch (Exception e) {
            iReturn = 1;
            logger.error(e.getLocalizedMessage(), e);
        }
        logger.debug("::::::: END :: renameFilesofDOForTitleblock ::::::::::");
        return iReturn;
    }
}
