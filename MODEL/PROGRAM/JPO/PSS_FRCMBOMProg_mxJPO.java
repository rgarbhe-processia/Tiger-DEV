import java.io.File;
import java.io.FileOutputStream;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bouncycastle.jcajce.provider.digest.Tiger;

import com.dassault_systemes.vplm.interfaces.access.IPLMxCoreAccess;
import com.dassault_systemes.vplm.modeler.PLMCoreModelerSession;
import com.dassault_systemes.i3dx.appsmodel.matrix.Relationship;
import com.dassault_systemes.vplm.modeler.entity.PLMxSemanticRelation;
import com.dassault_systemes.vplm.modeler.entity.PLMxRefInstanceEntity;
import com.matrixone.apps.domain.DomainAccess;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.XSSUtil;
import com.matrixone.apps.domain.util.eMatrixDateFormat;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.apps.framework.ui.UIUtil;
import com.matrixone.apps.engineering.EngineeringUtil;
import com.matrixone.apps.productline.ProductLineConstants;
import com.matrixone.plmql.cmd.PLMID;
import com.mbom.modeler.utility.FRCMBOMModelerAPI;
import com.mbom.modeler.utility.FRCMBOMModelerUtility;

import matrix.db.BusinessInterface;
import matrix.db.BusinessInterfaceList;
import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.Context;
import matrix.db.ExpansionIterator;
import matrix.db.JPO;
import matrix.db.Query;
import matrix.db.QueryIterator;
import matrix.db.RelationshipType;
import matrix.util.Pattern;
import matrix.util.SelectList;
import matrix.util.StringList;
import pss.constants.TigerConstants;
import pss.cadbom.Material_mxJPO;

@SuppressWarnings("deprecation")
public class PSS_FRCMBOMProg_mxJPO extends FRCMBOMProg_mxJPO {

    private static boolean boolCreateMBOM = false;

    private static List<String> lModelListOnStructure = new ArrayList<String>();

    private static String valueEnvAttachModel;

    public static final StringList EXPD_BUS_SELECT = new StringList(new String[] { "physicalid", "logicalid" });

    public static final String MBOMMAJORREVISION = "MBOMMAJORREVISION";

    public static final String MBOMDONOTREUSE = "MBOMDONOTREUSE";

    public static final String MBOMREVISEACTION = "MBOMREVISEACTION";

    public static final String MODIFYPLANT = "MODIFYPLANT";

    public static final String DONOTEXPAND = "DONOTEXPAND";

    public static final String FROMDRAGNDROP = "FROMDRAGNDROP";

    public static final String TOOLREVISEACTION = "TOOLREVISEACTION";

    public static final String NOTGENERATEVARINTASSEMBLY = "NotGenerateVariantAssembly";

    public static final String PROPOGRATEHARMONYASSOCIATION = "PROPOGRATEHARMONYASSOCIATION";

    public static final String PLANTFROMENOVIA = "PSS_PLANT_CALLING_FROM_ENOVIA";

    public static final String ISCRATEUSINGDRAGNDROP = "IsCreateMBOMUsingDragAndDrop";

    public static final String PLANTOWNERSHIP = "PLANT_OWNERSHIP";

    public final static String PSS_SPARE_PART = "Spare Part";

    public final static String STANDARDROOTMBOM = "RootStandardMBOM";

    public final static String SKIPSTANDARDMBOM = "SkipStdMBOM";

    public static final StringList EXPD_REL_SELECT = new StringList(
            new String[] { "physicalid[connection]", "logicalid[connection]", "from.physicalid", "attribute[PLMInstance.V_TreeOrder].value", "attribute[PSS_PublishedEBOM.PSS_InstanceName]" });

    public static final String PUBLISHED_EBOM_INSTANCENAME_WHERE_CLAUSE = "(" + TigerConstants.SELECT_ATTRIBUTE_PSS_PUBLISHEDEBOM_INSTANCENAME + "==Alternate || "
            + TigerConstants.SELECT_ATTRIBUTE_PSS_PUBLISHEDEBOM_INSTANCENAME + "=='Spare Part' || " + TigerConstants.SELECT_ATTRIBUTE_PSS_PUBLISHEDEBOM_INSTANCENAME + "==PSS_PartTool)";

    // TIGTK-5405 - 06-04-2017 - VB - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PSS_FRCMBOMProg_mxJPO.class);

    // TIGTK-5405 - 06-04-2017 - VB - END

    public PSS_FRCMBOMProg_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
        // TODO Auto-generated constructor stub
    }

    public static void flushAndCloseSession(PLMCoreModelerSession plmSession) {
        try {
            plmSession.flushSession();
        } catch (Exception e) {
        }

        try {
            plmSession.closeSession(true);
        } catch (Exception e) {
        }
    }

    /**
     * This method is used to add plant to MBOM assembly structure table
     * @param context
     * @param args
     */
    @SuppressWarnings({ "rawtypes", "serial" })
    public String addPlant(Context context, String[] args) {

        PLMCoreModelerSession plmSession = null;
        boolean isTransactionActive = false;
        int flag = 0;
        try {

            // PSS ALM4253 fix START
            PropertyUtil.setRPEValue(context, PLANTFROMENOVIA, "true", false);
            // PSS ALM4253 fix END

            ContextUtil.startTransaction(context, true);
            isTransactionActive = true;

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            Map programMap = (Map) JPO.unpackArgs(args);
            final String objectId = (String) programMap.get("objectId");

            String[] aTableRowIds = (String[]) programMap.get("emxTableRowId");
            if (aTableRowIds != null && aTableRowIds.length > 0) {
                String selectedPlantId = aTableRowIds[0];
                String[] aRowId = selectedPlantId.split("\\|");
                String sNewPlant = "";
                if (null != aRowId && 1 < aRowId.length)
                    sNewPlant = aRowId[1];
                String sNewPID = "";
                if (null != sNewPlant && !"".equals(sNewPlant))
                    sNewPID = MqlUtil.mqlCommand(context, "print bus " + sNewPlant + " select physicalid dump |", false, false);

                List<String> psRefPIDList = PSS_FRCMBOMModelerUtility_mxJPO.getScopedPSReferencePIDFromList(context, plmSession, new ArrayList<String>() {
                    {
                        add(objectId);
                    }
                });

                DomainObject dObj = DomainObject.newInstance(context, objectId);
                String selectedObjMajorIds = dObj.getInfo(context, "majorids");
                String[] lastMajorIds = selectedObjMajorIds.split("\\|");
                String selectedObjLastPhysicalId = lastMajorIds[lastMajorIds.length - 1];
                for (String psRefPID : psRefPIDList) {
                    if (UIUtil.isNotNullAndNotEmpty(psRefPID)) {
                        List<String> mbomPIDList = PSS_FRCMBOMModelerUtility_mxJPO.getScopingReferencesFromList(context, plmSession, psRefPID);
                        String mbomRefPID = null;
                        if (mbomPIDList != null) {
                            for (String refPID : mbomPIDList) {
                                DomainObject eachObj = DomainObject.newInstance(context, refPID);
                                String eachObjMajorIds = eachObj.getInfo(context, "majorids");
                                String[] eachLastMajorIds = eachObjMajorIds.split("\\|");
                                String eachLastPhysicalId = eachLastMajorIds[eachLastMajorIds.length - 1];
                                String strAttachedPlant = pss.mbom.MBOMUtil_mxJPO.getMasterPlant(context, refPID);
                                if (UIUtil.isNotNullAndNotEmpty(strAttachedPlant)) {
                                    if ((strAttachedPlant.equalsIgnoreCase(sNewPID)) && !eachLastPhysicalId.equals(selectedObjLastPhysicalId)) {
                                        mbomRefPID = refPID;
                                        break;
                                    }
                                }
                            }
                        }
                        if (mbomRefPID != null) {
                            flag = 1;
                            throw new Exception(
                                    EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.Error.Message.PlantCanNotbeConnected"));

                        }
                    }
                }
                StringList tobeProcessedList = new StringList(objectId);
                MapList mlMBOMStructureList = getExpandMBOM(context, objectId, 0, null, null, null, EXPD_REL_SELECT, EXPD_BUS_SELECT);

                Iterator itr = mlMBOMStructureList.iterator();
                int lastSkippedLevel = 0;
                while (itr.hasNext()) {
                    Map mTempMap = (Map) itr.next();
                    String strId = (String) mTempMap.get("physicalid");
                    String strLevel = (String) mTempMap.get(DomainConstants.SELECT_LEVEL);
                    int intLevel = Integer.parseInt(strLevel);

                    if (UIUtil.isNotNullAndNotEmpty(strId)) {
                        if (lastSkippedLevel != 0 && lastSkippedLevel < intLevel) {
                            continue;
                        }

                        String strAttachedPlant = pss.mbom.MBOMUtil_mxJPO.getMasterPlant(context, strId);
                        if (UIUtil.isNotNullAndNotEmpty(strAttachedPlant)) {
                            lastSkippedLevel = intLevel;
                        } else {
                            DomainObject domObject = DomainObject.newInstance(context, strId);
                            String strType = domObject.getInfo(context, DomainConstants.SELECT_TYPE);
                            if (!TigerConstants.LIST_TYPE_MATERIALS.contains(strType) && !strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_OPERATION)
                                    && !strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_LINEDATA))
                                tobeProcessedList.add(strId);
                        }
                        // }
                    }
                }
                for (int j = 0; j < tobeProcessedList.size(); j++) {
                    PSS_FRCMBOMModelerUtility_mxJPO.attachPlantToMBOMReference(context, plmSession, (String) tobeProcessedList.get(j), sNewPID);
                    getAttachedPlantAsConsumer(context, (String) tobeProcessedList.get(j), sNewPID);
                }
            }
            flushAndCloseSession(plmSession);
            if (isTransactionActive) {
                ContextUtil.commitTransaction(context);
            }

        } catch (Exception exp) {
            try {
                if (flag == 1)
                    emxContextUtil_mxJPO.mqlNotice(context, exp.getMessage());
                else {
                    // TIGTK-5405 - 11-04-2017 - VB - START
                    logger.error("Error in addPlant: ", exp);
                    // TIGTK-5405 - 11-04-2017 - VB - END
                }
            } catch (Exception e) {
                // TIGTK-5405 - 11-04-2017 - VB - START
                logger.error("Error in addPlant: ", e);
                // TIGTK-5405 - 11-04-2017 - VB - END
            }

            flushAndCloseSession(plmSession);
            if (isTransactionActive) {
                ContextUtil.abortTransaction(context);
            }
        }
        return null;
    }

    public static void disconnectVaraintAssemblyFromMBOMAndCreateNew(Context context, String mbomRefPID, String psRefPID) throws Exception {

        try {
            if (UIUtil.isNotNullAndNotEmpty(mbomRefPID) && !boolCreateMBOM) {
                DomainObject domMBOM = DomainObject.newInstance(context, mbomRefPID);
                String typeName = domMBOM.getInfo(context, DomainConstants.SELECT_TYPE);
                // String strtypeCreateAssembly = PropertyUtil.getSchemaProperty(context, "type_CreateAssembly");
                // String strtypeCreateKit = PropertyUtil.getSchemaProperty(context, "type_CreateKit");
                if (!typeName.equalsIgnoreCase(TigerConstants.TYPE_CREATEKIT) && !typeName.equalsIgnoreCase(TigerConstants.TYPE_CREATEASSEMBLY))
                    return;

                String[] args = new String[3];
                args[0] = mbomRefPID;
                args[1] = "true";
                args[2] = psRefPID;

                pss.mbom.StructureNodeUtil_mxJPO refObject = new pss.mbom.StructureNodeUtil_mxJPO();
                refObject.generateMassVariantAssemblies(context, args);
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in disconnectVaraintAssemblyFromMBOMAndCreateNew: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
    }

    /**
     * This method is used to get column revision
     * @param context
     * @param args
     * @throws Exception
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Vector getRevisionColumn(Context context, String[] args) throws Exception {
        long startTime = System.currentTimeMillis();
        List<String> listIDs = null;
        String strMajorRevision = "";

        final String ATTRIBUTE_V_MANUFACTURING_DECISION = PropertyUtil.getSchemaProperty(context, "attribute_V_Manufacturing_Decision");

        try {
            ContextUtil.startTransaction(context, false);

            // Create result vector
            Vector vecResult = new Vector();

            // Get object list information from packed arguments
            HashMap programMap = (HashMap) JPO.unpackArgs(args);

            // FRC START - HE5 : Added the part of code to fix the issue #267
            Map paramMap = (HashMap) programMap.get("paramList");
            boolean isexport = false;
            String export = (String) paramMap.get("exportFormat");
            if (export != null) {
                isexport = true;
            }
            // FRC END - HE5 : Added the part of code to fix the issue #267

            MapList objectList = (MapList) programMap.get("objectList");

            listIDs = new ArrayList<String>();
            // Do for each object
            for (Iterator itrObjects = objectList.iterator(); itrObjects.hasNext();) {
                Map mapObjectInfo = (Map) itrObjects.next();

                String objectID = (String) mapObjectInfo.get("id");
                listIDs.add(objectID);
            }
            StringList busSelect = new StringList();
            busSelect.add("revision");
            busSelect.add("majorids");
            busSelect.add("majorid");
            busSelect.add("logicalid");
            MapList resultInfoML = DomainObject.getInfo(context, listIDs.toArray(new String[0]), busSelect);

            int index = 0;
            for (Iterator itrObjects = objectList.iterator(); itrObjects.hasNext();) {
                Map mapObjectInfo = (Map) itrObjects.next();
                String physicalID = (String) mapObjectInfo.get("id");

                String objectID = (String) mapObjectInfo.get("id");
                // Get value of attribute V_MANUFACTURING_DECISION
                DomainObject domObject = new DomainObject(objectID);
                // TODO : Need to evolve the code after correct datamodel deicision
                String strAttributeValue = (String) domObject.getAttributeValue(context, ATTRIBUTE_V_MANUFACTURING_DECISION);

                // Default value unassigned (0)
                if (UIUtil.isNullOrEmpty(strAttributeValue)) {
                    strAttributeValue = "0";
                }

                Map<String, String> resultInfoMap = (Map<String, String>) resultInfoML.get(index);
                index++;

                String majorID = resultInfoMap.get("majorid");
                String logicalid = resultInfoMap.get("logicalid");
                String objectRevision = resultInfoMap.get("revision");
                if (objectRevision.contains(".")) {
                    strMajorRevision = objectRevision.substring(0, 2);
                } else {
                    strMajorRevision = objectRevision;
                }
                String revisionsListStr = resultInfoMap.get("majorids");
                if (majorID == null)
                    majorID = "temp";

                // FRC START - HE5 : Added the part of code to fix the issue #267
                if (isexport) {
                    vecResult.add(strMajorRevision);
                } else {

                    // hidden input field with id/name ManufacturingDecisionElement
                    String strHiddenInput = " <input type='hidden' name='ManufacturingDecisionElement' id='ManufacturingDecisionElement' value='" + strAttributeValue + "'/>";
                    if (revisionsListStr.endsWith(physicalID) || revisionsListStr.endsWith(majorID) || revisionsListStr.endsWith(logicalid)) {
                        vecResult.add("<div>" + strMajorRevision + "</div>" + strHiddenInput);
                    } else {
                        vecResult.add("<div style=\"color:red\">" + strMajorRevision + "</div>" + strHiddenInput);
                    }
                }
                // FRC END - HE5 : Added the part of code to fix the issue #267
            }

            ContextUtil.commitTransaction(context);

            long endTime = System.currentTimeMillis();
            logger.info("FRC PERFOS : getRevisionColumn (" + listIDs.size() + " objects in list) : " + (endTime - startTime));

            if (perfoTraces != null) {
                perfoTraces.write("Time spent in getRevisionColumn() : " + (endTime - startTime) + " milliseconds");
                perfoTraces.newLine();
                perfoTraces.flush();
            }

            return vecResult;
        } catch (Exception exp) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getRevisionColumn: ", exp);
            // TIGTK-5405 - 11-04-2017 - VB - END
            if (context.isTransactionActive()) {
                ContextUtil.abortTransaction(context);
            }
            throw exp;
        }
    }

    /**
     * This method is used to change the part type value
     * @param context
     * @param args
     * @throws Exception
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Map createNewManufItem(Context context, String[] args) throws Exception { // Called from FRCInsertManufItemPreProcess.jsp
        final String[] listMagnitudeFieldKeys = { "FRCMBOMCentral.MBOMManufItemMagnitudeLength", "FRCMBOMCentral.MBOMManufItemMagnitudeMass", "FRCMBOMCentral.MBOMManufItemMagnitudeArea",
                "FRCMBOMCentral.MBOMManufItemMagnitudeVolume" };
        PLMCoreModelerSession plmSession = null;
        try {

            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            // Map returnMap = createNewManufItemReference(context, args);

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String type = (String) programMap.get("TypeActual");
            String strMode = (String) programMap.get("PSS_Mode");

            // Prepare the magnitude, if the object is Continuous
            Locale loc = context.getLocale();
            String magnitudeFieldValue = (String) programMap.get("MagnitudeCreate");
            if (magnitudeFieldValue == null || "".equals(magnitudeFieldValue))
                magnitudeFieldValue = (String) programMap.get("MagnitudeForm");

            String magnitudeType = null;
            if (magnitudeFieldValue != null && !"".equals(magnitudeFieldValue)) {
                String magnitudeFieldKey = "";

                for (String key : listMagnitudeFieldKeys) {
                    if (magnitudeFieldValue.equals(EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", loc, key)))
                        magnitudeFieldKey = key;
                }

                if ("FRCMBOMCentral.MBOMManufItemMagnitudeLength".equals(magnitudeFieldKey))
                    magnitudeType = "Length";
                else if ("FRCMBOMCentral.MBOMManufItemMagnitudeMass".equals(magnitudeFieldKey))
                    magnitudeType = "Mass";
                else if ("FRCMBOMCentral.MBOMManufItemMagnitudeArea".equals(magnitudeFieldKey))
                    magnitudeType = "Area";
                else if ("FRCMBOMCentral.MBOMManufItemMagnitudeVolume".equals(magnitudeFieldKey))
                    magnitudeType = "Volume";
            }

            HashMap<String, String> attributes = new HashMap<String, String>();
            if (UIUtil.isNotNullAndNotEmpty(strMode))
                attributes.put("mode", strMode);

            String newRefPID = createMBOMReference(context, plmSession, type, magnitudeType, attributes);

            String changeObjectName = (String) programMap.get("FRCMBOMGetChangeObject");
            // Modify AFN - Test if a value has been defined into the creation
            // web form
            String changeObjectFromForm = (String) programMap.get("ChangeObjectCreateForm");
            if (null != changeObjectFromForm && !"".equals(changeObjectFromForm))
                changeObjectName = changeObjectFromForm;
            attachObjectToChange(context, plmSession, changeObjectName, newRefPID);

            // TIGTK-3595 : Issue due to Faurecia Part Number field Removed: START

            if (UIUtil.isNotNullAndNotEmpty(newRefPID)) {
                DomainObject dObj = DomainObject.newInstance(context, newRefPID);
                String strAutoName = DomainObject.EMPTY_STRING;

                if (TigerConstants.LIST_TYPE_MATERIALS.contains(type)) {
                    String symbolicTypeName = FrameworkUtil.getAliasForAdmin(context, DomainConstants.SELECT_TYPE, type, true);
                    strAutoName = DomainObject.getAutoGeneratedName(context, symbolicTypeName, null);
                    dObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PLM_EXTERNALID, strAutoName);
                    dObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_V_NAME, strAutoName);
                    dObj.setName(context, strAutoName);
                } else {
                    strAutoName = DomainObject.getAutoGeneratedName(context, "type_CreateAssembly", "FAURECIA");
                    dObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_V_NAME, strAutoName);
                }
            }

            // TIGTK-3595 : Issue due to Faurecia Part Number field Removed : END

            Map returnMap = new HashMap();
            returnMap.put("id", newRefPID);
            returnMap.put("newObjectId", newRefPID);

            // Add Interface PSS_ManufacturingItemExt on newly created object of
            // type CretaeMaterial

            /*
             * DomainObject domObj = new DomainObject(newRefPID); String strType = domObj.getInfo(context, DomainConstants.SELECT_TYPE);
             * 
             * String strCommand = "modify bus " + newRefPID + " add interface PSS_ManufacturingItemExt"; MqlUtil.mqlCommand(context, strCommand, false, false);
             * 
             * if (strType.equals(TYPE_CREATEMATERIAL)) { strCommand = "modify bus " + newRefPID + " add interface PSS_ManufacturingPartExt"; MqlUtil.mqlCommand(context, strCommand, false, false); }
             */

            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);
            return returnMap;
        } catch (Exception exp) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in createNewManufItem: ", exp);
            // TIGTK-5405 - 11-04-2017 - VB - END
            flushAndCloseSession(plmSession);
            if (context.isTransactionActive()) {
                ContextUtil.abortTransaction(context);
            }
            throw exp;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @com.matrixone.apps.framework.ui.ProgramCallable
    public static MapList getExpandMBOM(Context context, String[] args) throws Exception {// Expand program called by the emxIndentedTable.jsp of the MBOM table
        long startTime = System.currentTimeMillis();

        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        ContextUtil.startTransaction(context, false);
        String objectId = null;
        try {
            objectId = (String) paramMap.get("objectId");
            String expandLevel = (String) paramMap.get("expandLevel");
            if (UIUtil.isNullOrEmpty(expandLevel)) {
                expandLevel = (String) paramMap.get("compareLevel");
            }

            // Add configuration filter
            String filterExpression = (String) paramMap.get("FRCExpressionFilterInput_OID");
            String filterValue = (String) paramMap.get("FRCExpressionFilterInput_actualValue");
            String filterInput = (String) paramMap.get("FRCExpressionFilterInput");

            short expLvl = 0;// Default to Expand All = 0

            // EPI : correction bug when applying a config filter : force to do an Expand All
            // if (!"All".equals(expandLevel))
            // expLvl = Short.parseShort(expandLevel);

            // Call Expand
            MapList res = getExpandMBOM(context, objectId, expLvl, filterExpression, filterValue, filterInput, EXPD_REL_SELECT, EXPD_BUS_SELECT);

            // START UM5c06 : Build Paths and save theses in the return maps
            HashMap<String, String> mapPaths = new HashMap<String, String>();// Store
            // path
            // in
            // a
            // Map
            // to
            // be
            // able
            // to
            // manage
            // unsorted
            // return
            // MapList

            DomainObject domObj = new DomainObject(objectId);
            String rootPID = domObj.getInfo(context, "physicalid");
            mapPaths.put(rootPID, rootPID);

            // Declare variable before to improve prefs
            Map<String, Object> mapObj;
            String objPID, objFromPID, objPIDConnection;
            String newPath = "";
            for (int i = 0; i < res.size(); i++) {
                mapObj = (Map) res.get(i);
                objPID = (String) mapObj.get("physicalid");
                objPIDConnection = (String) mapObj.get("physicalid[connection]");
                objFromPID = (String) mapObj.get("from.physicalid");
                newPath = mapPaths.get(objFromPID);
                if (newPath != null && !newPath.isEmpty()) {
                    newPath = newPath + "/" + objPIDConnection;
                } else {
                    newPath = objPID;
                }
                mapPaths.put(objPID, newPath);
                mapObj.put("pathPID", newPath);

                // set ids to use physicalids
                mapObj.put("id", objPID);
                mapObj.put("id[connection]", objPIDConnection);

                // Add hasChildren info here
                if ((i + 1) < res.size()) {
                    Map nextMapObj = (Map) res.get(i + 1);
                    String strLevelNow = (String) mapObj.get("level");
                    String strLevelNext = (String) nextMapObj.get("level");
                    int levelNow = Integer.parseInt(strLevelNow);
                    int levelNext = Integer.parseInt(strLevelNext);
                    if (levelNext > levelNow) {
                        mapObj.put("hasChildren", "true");
                    } else {
                        mapObj.put("hasChildren", "false");
                        mapObj.put("children", new MapList());
                    }
                } else {
                    // Last object so no childrens
                    mapObj.put("hasChildren", "false");
                    mapObj.put("children", new MapList());
                }
            }
            // END UM5c06 : Build Paths and save theses in the return maps

            // Sort by TreeOrder "attribute[PLMInstance.V_TreeOrder].value"
            res.sortStructure("attribute[PLMInstance.V_TreeOrder].value", "ascending", "real");

            ContextUtil.commitTransaction(context);
            long endTime = System.currentTimeMillis();
            System.out.println("FRC PERFOS : getExpandMBOM (without getVPMStructure) : " + (endTime - startTime));

            if (perfoTraces != null) {
                perfoTraces.write("Time spent in getExpandMBOM() : " + (endTime - startTime) + " milliseconds");
                perfoTraces.newLine();
                perfoTraces.flush();
            }

            return res;
        } catch (Exception ex) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getExpandMBOM: ", ex);
            // TIGTK-5405 - 11-04-2017 - VB - END
            if (context.isTransactionActive()) {
                ContextUtil.abortTransaction(context);
            }
            throw ex;
        }
    }

    public static String getListAuthorizedChildManufItemTypes(Context context, String[] args) throws Exception { // Called from FRCInsertDuplicatedPreProcess.jsp and FRCInsertDuplicatedPreProcess2.jsp
        // and FRCInsertExistingPreProcess.jsp and FRCInsertManufItemPreProcess.jsp
        StringBuffer returnSB = new StringBuffer("");

        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, false);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            String type = args[0];

            List<String> listTypes = PSS_FRCMBOMModelerUtility_mxJPO.getAuthorizedChildMBOMReferenceTypes(context, plmSession, type);

            boolean firstElem = true;
            for (String childType : listTypes) {
                if (firstElem)
                    firstElem = false;
                else
                    returnSB.append(",");
                returnSB.append(childType);
            }

            closeSession(plmSession);
            ContextUtil.commitTransaction(context);
        } catch (Exception exp) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getListAuthorizedChildManufItemTypes: ", exp);
            // TIGTK-5405 - 11-04-2017 - VB - END
            closeSession(plmSession);
            if (context.isTransactionActive()) {
                ContextUtil.abortTransaction(context);
            }
            throw exp;
        }

        return returnSB.toString();
    }

    /**
     * @PolicyChange
     * @param context
     * @param plmSession
     * @param type
     * @param magnitudeType
     * @param attributes
     * @return
     * @throws Exception
     */
    public static String createMBOMReference(Context context, PLMCoreModelerSession plmSession, String type, String magnitudeType, Map<String, String> attributes) throws Exception {

        // PSS ALM2107 fix START
        PropertyUtil.setRPEValue(context, "PSS_IS_CALLING_FROM_ENOVIA", "true", false);
        // PSS ALM2107 fix END
        // TIGTK-9215:START
        String strCADMassValue = (String) attributes.get("PSS_PublishedPart.PSS_PP_CADMass");
        String strEBOMMass1 = (String) attributes.get("PSS_EBOM_Mass1");
        String strEBOMMass2 = (String) attributes.get("PSS_EBOM_Mass2");
        String strEBOMMass3 = (String) attributes.get("PSS_EBOM_Mass3");
        // TIGTK-9215:END

        String isContinuous = MqlUtil.mqlCommand(context, "print type '" + type + "' select kindof[DELFmiContinuousFunctionReference] dump |", false, false);
        String newObjPID = null;
        String newObjPLMID = null;
        String strMode = DomainConstants.EMPTY_STRING;
        if (!attributes.isEmpty()) {
            strMode = attributes.get("mode");
            attributes = new HashMap<String, String>();
        }

        String strAutoName = "";
        if ("TRUE".equalsIgnoreCase(isContinuous)) {
            newObjPID = PSS_FRCMBOMModelerUtility_mxJPO.createMBOMContinuousReference(context, plmSession, type, TigerConstants.POLICY_PSS_MBOM, magnitudeType, attributes);
        } else {
            newObjPID = PSS_FRCMBOMModelerUtility_mxJPO.createMBOMDiscreteReference(context, plmSession, type, TigerConstants.POLICY_PSS_MBOM, attributes);
        }
        flushSession(plmSession);

        // PSS : Customization : START
        // Add Interface PSS_ManufacturingItemExt on newly created object of
        // type CretaeMaterial

        StringList slObjSelectStmts = new StringList();
        slObjSelectStmts.addElement(DomainConstants.SELECT_POLICY);
        slObjSelectStmts.addElement(DomainConstants.SELECT_NAME);
        slObjSelectStmts.addElement(DomainConstants.SELECT_TYPE);
        DomainObject domObj = new DomainObject(newObjPID);

        // TIGTK-::9215:START
        // TIGTK-12872 : 17-01-2018 : START
        if (UIUtil.isNotNullAndNotEmpty(strEBOMMass1) && !strEBOMMass1.equals("0.0")) {
            Double dblEBOMMass1 = Double.parseDouble(strEBOMMass1);
            dblEBOMMass1 = dblEBOMMass1 / 1000;
            strEBOMMass1 = String.valueOf(dblEBOMMass1);
            domObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXTPSS_NETWEIGHT, strEBOMMass1);
            domObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXTPSS_GROSSWEIGHT, strEBOMMass1);
        } else if (UIUtil.isNotNullAndNotEmpty(strEBOMMass2) && !strEBOMMass2.equals("0.0")) {
            Double dblEBOMMass2 = Double.parseDouble(strEBOMMass2);
            dblEBOMMass2 = dblEBOMMass2 / 1000;
            strEBOMMass2 = String.valueOf(dblEBOMMass2);
            domObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXTPSS_NETWEIGHT, strEBOMMass2);
            domObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXTPSS_GROSSWEIGHT, strEBOMMass2);
        } else if (UIUtil.isNotNullAndNotEmpty(strEBOMMass3) && !strEBOMMass3.equals("0.0")) {
            Double dblEBOMMass3 = Double.parseDouble(strEBOMMass3);
            dblEBOMMass3 = dblEBOMMass3 / 1000;
            strEBOMMass3 = String.valueOf(dblEBOMMass3);
            domObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXTPSS_NETWEIGHT, strEBOMMass3);
            domObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXTPSS_GROSSWEIGHT, strEBOMMass3);
        } else if (UIUtil.isNotNullAndNotEmpty(strCADMassValue) && !strCADMassValue.equals("0.0")) {
            Double dblCADMassValue = Double.parseDouble(strCADMassValue);
            dblCADMassValue = dblCADMassValue / 1000;
            strCADMassValue = String.valueOf(dblCADMassValue);
            domObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXTPSS_NETWEIGHT, strCADMassValue);
            domObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXTPSS_GROSSWEIGHT, strCADMassValue);
        }
        // TIGTK-12872 : 17-01-2018 : END
        // TIGTK-::9215:END

        Map objectInfoMap = domObj.getInfo(context, slObjSelectStmts);

        String policy = (String) objectInfoMap.get(DomainConstants.SELECT_POLICY);
        String strType = (String) objectInfoMap.get(DomainConstants.SELECT_TYPE);

        // OOTB attribute Modification : Start : TIGTK-13669
        if (strType.equalsIgnoreCase(TigerConstants.TYPE_CREATEMATERIAL))
            domObj.setAttributeValue(context, "CreateMaterial.V_NeedDedicatedSystem", "2");
        // OOTB attribute Modification : END

        // Sneha :Start
        if (UIUtil.isNotNullAndNotEmpty(policy) && !policy.equalsIgnoreCase(TigerConstants.POLICY_PSS_MBOM) && UIUtil.isNullOrEmpty(strMode)
                && (strType.equals(TigerConstants.TYPE_PSS_LINEDATA) || strType.equals(TigerConstants.TYPE_PSS_OPERATION))) {
            domObj.setPolicy(context, TigerConstants.POLICY_OPERATIONLINE_DATA);
            String strName = DomainConstants.EMPTY_STRING;
            if (strType.equals(TigerConstants.TYPE_PSS_LINEDATA)) {
                strName = DomainObject.getAutoGeneratedName(context, "type_" + TigerConstants.TYPE_PSS_LINEDATA, "");

            } else {
                strName = DomainObject.getAutoGeneratedName(context, "type_" + TigerConstants.TYPE_PSS_OPERATION, "");
            }
            domObj.setName(context, strName);
            String strRevision = "01.1";
            String strChangeString = "modify bus $1 revision $2 name $3;";
            MqlUtil.mqlCommand(context, strChangeString, newObjPID, strRevision, strName);
        } else if (UIUtil.isNotNullAndNotEmpty(policy) && !policy.equalsIgnoreCase(TigerConstants.POLICY_PSS_MBOM) && UIUtil.isNullOrEmpty(strMode)) {
            domObj.setPolicy(context, TigerConstants.POLICY_PSS_MBOM);
            /* updated Revision Changes : 09/09/16 START */
            String strName = (String) objectInfoMap.get(DomainConstants.SELECT_NAME);
            String strRevision = "01.1";
            String strChangeString = "modify bus $1 revision $2 name $3;";
            MqlUtil.mqlCommand(context, strChangeString, newObjPID, strRevision, strName);
            /* updated Revision Changes : 09/09/16 END */
        } else {
            if (strMode.equalsIgnoreCase("setNewPolicy")) {
                domObj.setPolicy(context, TigerConstants.POLICY_PSS_MATERIALASSEMBLY);
                String strName = domObj.getInfo(context, DomainConstants.SELECT_NAME);
                String strRevision = "01.1";
                String strChangeString = "modify bus $1 revision $2 name $3;";
                MqlUtil.mqlCommand(context, strChangeString, newObjPID, strRevision, strName);
            } else {
                domObj.setPolicy(context, TigerConstants.POLICY_PSS_STANDARDMBOM);
                String strName = domObj.getInfo(context, DomainConstants.SELECT_NAME);
                String strRevision = "01.1";
                String strChangeString = "modify bus $1 revision $2 name $3;";
                MqlUtil.mqlCommand(context, strChangeString, newObjPID, strRevision, strName);
            }
        }
        // Sneha :End
        String symbolicTypeName = FrameworkUtil.getAliasForAdmin(context, DomainConstants.SELECT_TYPE, strType, true);
        // TIGTK-3515
        if (!TigerConstants.LIST_TYPE_CLONEMBOM.contains(strType)) {
            strAutoName = DomainObject.getAutoGeneratedName(context, symbolicTypeName, "-");
            domObj.setName(context, strAutoName);
            domObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PLM_EXTERNALID, strAutoName);
        }
        // PSS : Customization : END
        return newObjPID;
    }

    /**
     * @InheritObjects
     * @param context
     * @param plmSession
     * @param type
     * @param magnitudeType
     * @param attributes
     * @return
     * @throws Exception
     */
    public static String createMBOMReferenceWithInherit(Context context, PLMCoreModelerSession plmSession, String type, String magnitudeType, Map<String, String> attributes, String psRefPID,
            String plantPID) throws Exception {
        // TIGTK-10100:Rutuja Ekatpure:start
        logger.debug("createMBOMReferenceWithInherit:::Start ");
        String newObjPID = DomainConstants.EMPTY_STRING;
        String result = MqlUtil.mqlCommand(context, "print bus " + psRefPID + " select interface dump |", false, false);
        DomainObject psRefObj = new DomainObject(psRefPID);
        Map psRefAttributes = psRefObj.getAttributeMap(context, true);
        // TIGTK-9215 : START
        String strCADMassValue = (String) psRefAttributes.get(TigerConstants.ATTRIBUTE_PSS_PUBLISHEDPARTPSS_PP_CADMASS);
        String strClassificationList = (String) psRefAttributes.get(TigerConstants.ATTRIBUTE_PSS_PUBLISHEDPART_PSS_CLASSIFICATIONLIST);

        String strEBOMMass1 = (String) psRefAttributes.get(TigerConstants.ATTRIBUTE_PSS_EBOM_Mass1);
        String strEBOMMass2 = (String) psRefAttributes.get(TigerConstants.ATTRIBUTE_PSS_EBOM_Mass2);
        String strEBOMMass3 = (String) psRefAttributes.get(TigerConstants.ATTRIBUTE_PSS_EBOM_Mass3);

        // TIGTK-9215 : END

        if (UIUtil.isNotNullAndNotEmpty(result) && result.contains("PSS_PublishedPart") && "Standard".equals(psRefAttributes.get("PSS_PublishedPart.PSS_StandardReference"))) {
            logger.debug("createMBOMReferenceWithInherit:::create StandardMBOM ");
            newObjPID = createStandardMBOMReference(context, plmSession, type, magnitudeType, attributes);
        } else {
            // TIGTK-11580:RE:1/12/2017:Start
            String strIsDragAndDrop = PropertyUtil.getRPEValue(context, ISCRATEUSINGDRAGNDROP, false);
            // TIGTK-9215 : START
            if (UIUtil.isNotNullAndNotEmpty(strClassificationList) && !"True".equalsIgnoreCase(strIsDragAndDrop)) {
                attributes.put("PSS_PublishedPart.PSS_PP_CADMass", strCADMassValue);
                attributes.put("PSS_EBOM_Mass1", strEBOMMass1);
                attributes.put("PSS_EBOM_Mass2", strEBOMMass2);
                attributes.put("PSS_EBOM_Mass3", strEBOMMass3);
            }
            // TIGTK-11580:RE:1/12/2017:End
            // TIGTK-9215 : END

            logger.debug("createMBOMReferenceWithInherit:::create MBOM ");
            newObjPID = createMBOMReference(context, plmSession, type, magnitudeType, attributes);
        }
        // TIGTK-10100:Rutuja Ekatpure:End
        inheritInfoFromPS(context, plmSession, newObjPID, psRefPID, plantPID);
        logger.debug("createMBOMReferenceWithInherit:::End ");
        return newObjPID;
    }

    /**
     * @CreateMBOMWithPlant
     * @param context
     * @param plmSession
     * @param type
     * @param magnitudeType
     * @param attributes
     * @return
     * @throws Exception
     */
    public static String createMBOMReferenceWithPlant(Context context, PLMCoreModelerSession plmSession, String type, String magnitudeType, Map<String, String> attributes, String psRefPID,
            String plantPID) throws Exception {
        // PSS ALM4253 fix START
        logger.debug("createMBOMReferenceWithPlant:::Start ");
        PropertyUtil.setRPEValue(context, PLANTFROMENOVIA, "true", false);
        // PSS ALM4253 fix END
        String newObjPID = createMBOMReferenceWithInherit(context, plmSession, type, magnitudeType, attributes, psRefPID, plantPID);
        PSS_FRCMBOMModelerUtility_mxJPO.attachPlantToMBOMReference(context, plmSession, (String) newObjPID, plantPID);

        // RFC-139 : Update the Master Plant Name on Implement Link
        if (UIUtil.isNotNullAndNotEmpty(newObjPID) && UIUtil.isNotNullAndNotEmpty(plantPID)) {
            DomainObject domObject = DomainObject.newInstance(context, newObjPID);

            if (!domObject.isKindOf(context, TigerConstants.TYPE_PROCESS_CONTINUOUS_CREATE_MATERIAL) && !domObject.isKindOf(context, TigerConstants.TYPE_PROCESSCONTINUOUSPROVIDE)) {
                DomainObject dPlantObj = DomainObject.newInstance(context, plantPID);
                String strPlantName = dPlantObj.getInfo(context, DomainConstants.SELECT_NAME);
                // TIGTK-10100:rutuja Ekatpure:Start
                String result = MqlUtil.mqlCommand(context, "print bus " + psRefPID + " select interface dump |", false, false);

                DomainObject psRefObj = new DomainObject(psRefPID);
                Map psRefAttributes = psRefObj.getAttributeMap(context, true);
                String strStandardReferenceVlue = (String) psRefAttributes.get("PSS_PublishedPart.PSS_StandardReference");
                if (UIUtil.isNotNullAndNotEmpty(result) && result.contains("PSS_PublishedPart") && ("Standard".equals(strStandardReferenceVlue))) {
                    String strquery = "query path type SemanticRelation containing " + newObjPID + " where owner.type=='MfgProductionPlanning' select owner.physicalid dump |";
                    String listPathIds = MqlUtil.mqlCommand(context, strquery, false, false);
                    if (UIUtil.isNotNullAndNotEmpty(listPathIds)) {
                        String strMfgPlanningId = listPathIds.split("\\|")[1];
                        PropertyUtil.setGlobalRPEValue(context, MODIFYPLANT, "false");
                        updateConsumerPlantOnMBOM(context, strMfgPlanningId);
                    }

                } else {
                    // RFC135 fix START
                    PropertyUtil.setGlobalRPEValue(context, MODIFYPLANT, "false");
                    // RFC135 fix END
                    domObject.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXTPSS_MATERPLANTNAME, strPlantName);
                }
                // TIGTK-10100:rutuja Ekatpure:End
            }
        }
        logger.debug("createMBOMReferenceWithPlant:::End ");
        return newObjPID;
    }

    @SuppressWarnings("rawtypes")
    public static void inheritInfoFromPS(Context context, PLMCoreModelerSession plmSession, String mbomRefPID, String psRefPID, String plantPID) throws Exception {
        // TODO : Inherit code
        logger.debug("inheritInfoFromPS:::Start ");
        DomainObject mbomObj = DomainObject.newInstance(context, mbomRefPID);
        DomainObject psObj = DomainObject.newInstance(context, psRefPID);
        Map attributeMap = psObj.getAttributeMap(context, true);
        mbomObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_V_NAME, (String) attributeMap.get(TigerConstants.ATTRIBUTE_V_NAME));
        mbomObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PLMENTITY_V_DESCRIPTION, (String) attributeMap.get(TigerConstants.ATTRIBUTE_PLMENTITY_V_DESCRIPTION));
        mbomObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXT_PSS_SPARE, (String) attributeMap.get(TigerConstants.ATTRIBUTE_PSS_PUBLISHEDPART_PSS_PARTSPAREPART));

        String colorList = (String) attributeMap.get(TigerConstants.ATTRIBUTE_PSS_PUBLISHEDPART_PSS_COLORLIST);
        if (UIUtil.isNotNullAndNotEmpty(colorList)) {
            DomainRelationship.connect(context, mbomObj, TigerConstants.RELATIONSHIP_PSS_COLORLIST, true, colorList.split("\\|"));
        }

        String materialList = (String) attributeMap.get(TigerConstants.ATTRIBUTE_PSS_PUBLISHEDPART_PSS_MATERIALLIST);
        if (UIUtil.isNotNullAndNotEmpty(materialList)) {
            StringList materialIdList = FrameworkUtil.split(materialList, "|");
            for (Object eachMaterialId : materialIdList) {
                createInstance(context, plmSession, mbomRefPID, (String) eachMaterialId);
            }
        }

        String classificationList = (String) attributeMap.get(TigerConstants.ATTRIBUTE_PSS_PUBLISHEDPART_PSS_CLASSIFICATIONLIST);
        if (UIUtil.isNotNullAndNotEmpty(classificationList)) {
            pss.mbom.MBOMUtil_mxJPO.addORUpdateClassification(context, mbomRefPID, classificationList);
        }
        // TIGTK-7712 : 7/4/2017 : PTE :Starts
        String strToolingList = (String) attributeMap.get(TigerConstants.ATTRIBUTE_PSS_PUBLISHEDPART_PSS_TOOLINGLIST);
        if (UIUtil.isNotNullAndNotEmpty(strToolingList)) {
            StringList slToolingList = FrameworkUtil.split(strToolingList, "|");
            for (Object objToolId : slToolingList) {
                FRCMBOMModelerUtility.attachResourceToMBOMReference(context, plmSession, mbomRefPID, (String) objToolId);
            }
        }
        // TIGTK-7712 : 7/4/2017 : PTE :END

        StringList busSelect = new StringList();
        busSelect.add("physicalid");
        StringList relSelect = new StringList();
        relSelect.add("physicalid[connection]");
        relSelect.add("attribute[PLMInstance.V_TreeOrder].value");
        relSelect.add(TigerConstants.SELECT_ATTRIBUTE_PSS_PUBLISHEDEBOM_INSTANCENAME);

        MapList psInstList = PSS_FRCMBOMModelerUtility_mxJPO.getVPMStructureMQL(context, psRefPID, busSelect, relSelect, (short) 1, null, PUBLISHED_EBOM_INSTANCENAME_WHERE_CLAUSE); // Expand
        // first
        // level
        // psInstList.sortStructure("attribute[PLMInstance.V_TreeOrder].value", "ascending", "real");

        List<Map<String, String>> workingInfo_instanceAttributes = new ArrayList<Map<String, String>>();
        List<Integer> workingInfo_indexInstancesForImplement = new ArrayList<Integer>();
        Set<String> workingInfo_lModelListOnStructure = new HashSet<String>();
        Map<String, String> workingInfo_AppDateToValuate = new HashMap<String, String>();
        Map<String, List<String>> workingInfo = new HashMap<String, List<String>>();
        workingInfo.put("instanceToCreate_parentRefPLMID", new ArrayList<String>());
        workingInfo.put("instanceToCreate_childRefPLMID", new ArrayList<String>());
        workingInfo.put("mbomLeafInstancePIDList", new ArrayList<String>());
        workingInfo.put("psPathList", new ArrayList<String>());
        workingInfo.put("newRefPIDList", new ArrayList<String>());
        workingInfo.put("newScopesToCreate_MBOMRefPIDs", new ArrayList<String>());
        workingInfo.put("newScopesToCreate_MBOMRefPLMIDs", new ArrayList<String>());
        workingInfo.put("newScopesToCreate_PSRefPIDs", new ArrayList<String>());
        workingInfo.put("newScopeObjectList", new ArrayList<String>());

        for (Object eachObj : psInstList) {
            Map eachMap = (Map) eachObj;
            String instanceName = (String) eachMap.get(TigerConstants.SELECT_ATTRIBUTE_PSS_PUBLISHEDEBOM_INSTANCENAME);
            String psPID = (String) eachMap.get("physicalid");
            if ("Alternate".equals(instanceName)) {
                List<String> scopingMBOMPIDList = PSS_FRCMBOMModelerUtility_mxJPO.getScopingReferencesFromList_PLMID(context, plmSession, psPID);
                String alternateMBOMRefPLMID = getMBOMReferenceToReuse(context, plmSession, scopingMBOMPIDList, plantPID);
                if (UIUtil.isNotNullAndNotEmpty(alternateMBOMRefPLMID)) {
                    DomainObject alternateObj = DomainObject.newInstance(context, alternateMBOMRefPLMID);
                    // String alernateLastPhysicalId = alternateObj.getInfo(context, "last.physicalid");
                    String alernateMajorId = alternateObj.getInfo(context, "majorids");
                    String[] alernateMajorIds = alernateMajorId.split("\\|");
                    String alernateLastPhysicalId = alernateMajorIds[alernateMajorIds.length - 1];
                    PSS_FRCMBOMModelerUtility_mxJPO.createMfgProcessAlternate(context, plmSession, mbomRefPID, alernateLastPhysicalId);
                } else {
                    String mbomType = mbomObj.getInfo(context, DomainConstants.SELECT_TYPE);

                    // alternateMBOMRefPID = createMBOMFromEBOMLikePSRecursive(context, plmSession, null, psPID, null, null, lModelsArray.toArray(new String[] {}), newRefPIDList, plantPID, mbomType);
                    // Updated with new API:MBO-164
                    alternateMBOMRefPLMID = createMBOMFromEBOMLikePSRecursive_new(context, plmSession, null, null, psPID, null, workingInfo, workingInfo_lModelListOnStructure,
                            workingInfo_instanceAttributes, workingInfo_indexInstancesForImplement, workingInfo_AppDateToValuate, plantPID, mbomType);
                    String alternateMBOMRefPID = PLMID.buildFromString(alternateMBOMRefPLMID).getPid();
                    FRCMBOMModelerUtility.createMfgProcessAlternate(context, plmSession, mbomRefPID, alternateMBOMRefPID);
                }
            } else if ("Spare Part".equals(instanceName)) {
                List<String> scopingMBOMPIDList = PSS_FRCMBOMModelerUtility_mxJPO.getScopingReferencesFromList_PLMID(context, plmSession, psPID);
                String spareMBOMRefPLMPID = getMBOMReferenceToReuse(context, plmSession, scopingMBOMPIDList, plantPID);
                if (UIUtil.isNotNullAndNotEmpty(spareMBOMRefPLMPID)) {
                    DomainObject spareObj = DomainObject.newInstance(context, spareMBOMRefPLMPID);
                    // String spareLastPhysicalId = spareObj.getInfo(context, "last.physicalid");
                    String spareLastMajorId = spareObj.getInfo(context, "majorids");
                    String[] spareLastMajorIds = spareLastMajorId.split("\\|");
                    String spareLastPhysicalId = spareLastMajorIds[spareLastMajorIds.length - 1];
                    DomainRelationship.connect(context, mbomObj, TigerConstants.RELATIONSHIP_PSS_SPAREMANUFACTURINGITEM, DomainObject.newInstance(context, spareLastPhysicalId));
                } else {
                    String mbomType = mbomObj.getInfo(context, DomainConstants.SELECT_TYPE);
                    // spareMBOMRefPID = createMBOMFromEBOMLikePSRecursive(context, plmSession, null, psPID, null, null, lModelsArray.toArray(new String[] {}), newRefPIDList, plantPID, mbomType);
                    // Updated with new API:MBO-164
                    spareMBOMRefPLMPID = createMBOMFromEBOMLikePSRecursive_new(context, plmSession, null, null, psPID, null, workingInfo, workingInfo_lModelListOnStructure,
                            workingInfo_instanceAttributes, workingInfo_indexInstancesForImplement, workingInfo_AppDateToValuate, plantPID, mbomType);
                    String spareMBOMRefPID = PLMID.buildFromString(spareMBOMRefPLMPID).getPid();
                    DomainRelationship.connect(context, mbomObj, TigerConstants.RELATIONSHIP_PSS_SPAREMANUFACTURINGITEM, DomainObject.newInstance(context, spareMBOMRefPID));
                }
            } else if ("PSS_PartTool".equals(instanceName)) {
                PSS_FRCMBOMModelerUtility_mxJPO.attachResourceToMBOMReference(context, plmSession, mbomRefPID, psPID);
            }
        }
        logger.debug("inheritInfoFromPS:::End ");
    }

    /**
     * @PolicyChange
     * @param context
     * @param args
     * @throws Exception
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void replaceByNewManufMaterial(Context context, String[] args) throws Exception { // Called from FRCReplaceByNewManufMaterialPostProcess.jsp
        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            String mbomPath = args[0];
            String changeObjectPID = args[1];

            String[] mbomPathList = mbomPath.split("/");

            String mbomRefPID = MqlUtil.mqlCommand(context, "print connection " + mbomPathList[mbomPathList.length - 1] + " select to.physicalid dump |", false, false);

            // Check the type of the initial reference
            String isProvideStr = MqlUtil.mqlCommand(context, "print bus " + mbomRefPID + " select type.kindof[Provide] dump |", false, false);
            String isContinuousProvideStr = MqlUtil.mqlCommand(context, "print bus " + mbomRefPID + " select type.kindof[ProcessContinuousProvide] dump |", false, false);

            if (!"TRUE".equalsIgnoreCase(isProvideStr) && !"TRUE".equalsIgnoreCase(isContinuousProvideStr))
                throw new Exception("Manufacturing Item must be of type Provide or ContinuousProvide");

            // Get the attribute values of the PS reference
            DomainObject mbomRefObj = new DomainObject(mbomRefPID);
            Map mbomRefAttributes = mbomRefObj.getAttributeMap(context, true);
            Map newMBOMRefAttributes = new HashMap();
            newMBOMRefAttributes.put("PLMEntity.V_Name", mbomRefAttributes.get("PLMEntity.V_Name"));
            newMBOMRefAttributes.put("PLMEntity.V_description", mbomRefAttributes.get("PLMEntity.V_description"));

            // Create a new MBOM reference
            HashMap<String, String> attributes = new HashMap<String, String>();

            String newMBOMRefPID = null;
            if ("TRUE".equalsIgnoreCase(isProvideStr)) {
                newMBOMRefPID = createMBOMReference(context, plmSession, "CreateMaterial", null, attributes);
            } else if ("TRUE".equalsIgnoreCase(isContinuousProvideStr)) {
                String magnitudeExtension = MqlUtil.mqlCommand(context, "print bus " + mbomRefPID + " select interface dump |", false, false);
                String magnitudeType = null;
                String quantity = "1.0";
                if (magnitudeExtension.contains("DELFmiContQuantity_Length")) {
                    magnitudeType = "Length";
                    quantity = MqlUtil.mqlCommand(context, "print bus " + mbomRefPID + " select attribute[DELFmiContQuantity_Length.V_ContQuantity].value dump |", false, false);
                    newMBOMRefAttributes.put("DELFmiContQuantity_Length.V_ContQuantity", quantity);
                } else if (magnitudeExtension.contains("DELFmiContQuantity_Mass")) {
                    magnitudeType = "Mass";
                    quantity = MqlUtil.mqlCommand(context, "print bus " + mbomRefPID + " select attribute[DELFmiContQuantity_Mass.V_ContQuantity].value dump |", false, false);
                    newMBOMRefAttributes.put("DELFmiContQuantity_Mass.V_ContQuantity", quantity);
                } else if (magnitudeExtension.contains("DELFmiContQuantity_Area")) {
                    magnitudeType = "Area";
                    quantity = MqlUtil.mqlCommand(context, "print bus " + mbomRefPID + " select attribute[DELFmiContQuantity_Area.V_ContQuantity].value dump |", false, false);
                    newMBOMRefAttributes.put("DELFmiContQuantity_Area.V_ContQuantity", quantity);
                } else if (magnitudeExtension.contains("DELFmiContQuantity_Volume")) {
                    magnitudeType = "Volume";
                    quantity = MqlUtil.mqlCommand(context, "print bus " + mbomRefPID + " select attribute[DELFmiContQuantity_Volume.V_ContQuantity].value dump |", false, false);
                    newMBOMRefAttributes.put("DELFmiContQuantity_Volume.V_ContQuantity", quantity);
                }

                newMBOMRefPID = createMBOMReference(context, plmSession, "ProcessContinuousCreateMaterial", magnitudeType, attributes);
            }

            // Attach the new reference to the change object
            attachObjectToChange(context, plmSession, changeObjectPID, newMBOMRefPID);

            // Replicate all the attributes values on the new MBOM reference
            DomainObject newMBOMRefObj = new DomainObject(newMBOMRefPID);
            newMBOMRefObj.setAttributeValues(context, newMBOMRefAttributes);

            // Get the scope on the old reference
            List<String> inputListForGetScope = new ArrayList<String>();
            inputListForGetScope.add(mbomRefPID);
            String scopePSRefPID = PSS_FRCMBOMModelerUtility_mxJPO.getScopedPSReferencePIDFromList(context, plmSession, inputListForGetScope).get(0);

            // Delete the scope on the old reference
            List<String> modifiedInstanceList = PSS_FRCMBOMModelerUtility_mxJPO.deleteScopeLinkAndDeleteChildImplementLinks(context, plmSession, mbomRefPID, false);
            logger.info("FRC : modified Instances : ", modifiedInstanceList);

            // Set the scope on the new reference
            if (scopePSRefPID != null && !"".equals(scopePSRefPID))
                PSS_FRCMBOMModelerUtility_mxJPO.createScopeLinkAndReduceChildImplementLinks(context, plmSession, newMBOMRefPID, scopePSRefPID, false);

            // Replace the instance
            PSS_FRCMBOMModelerUtility_mxJPO.replaceMBOMInstance(context, plmSession, mbomPathList[mbomPathList.length - 1], newMBOMRefPID);

            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);
            // ContextUtil.abortTransaction(context);
        } catch (Exception exp) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in replaceByNewManufMaterial: ", exp);
            // TIGTK-5405 - 11-04-2017 - VB - END
            flushAndCloseSession(plmSession);
            if (context.isTransactionActive()) {
                ContextUtil.abortTransaction(context);
            }
            throw exp;
        }
    }

    /**
     * @PolicyChange
     * @param context
     * @param plmSession
     * @param mbomParentRef
     * @param psRefID
     * @param mbomCompleteParentPath
     * @param psCompletePath
     * @param modelsToAttachToRoot
     * @param newRefPIDList
     * @return
     * @throws Exception
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static String createMBOMFromEBOMLikePSRecursive(Context context, PLMCoreModelerSession plmSession, String mbomParentRef, String psRefID, String mbomCompleteParentPath,
            String psCompletePath, String[] modelsToAttachToRoot, List<String> newRefPIDList, String plantPID, String parentType) throws Exception {
        String newMBOMRefPID = null;
        boolean checkRPE = false;

        // Get all the first level instances of the PS reference
        StringList busSelect = new StringList();
        busSelect.add("physicalid");
        StringList relSelect = new StringList();
        relSelect.add("physicalid[connection]");

        // Bug #231 - DCP - START
        relSelect.add("attribute[PLMInstance.V_TreeOrder].value");

        // MapList psInstList = getVPMStructure(context, psRefID, busSelect,
        // relSelect, (short) 1, null); // Expand first level
        // Rutuja Ekatpure:TIGTK-10100:2/10/2017:Start
        MapList psInstList = null;
        String result = MqlUtil.mqlCommand(context, "print bus " + psRefID + " select interface dump |", false, false);
        boolean isStandardMBOM = false;
        DomainObject psRefObj = new DomainObject(psRefID);
        Map psRefAttributes = psRefObj.getAttributeMap(context, true);
        if (UIUtil.isNotNullAndNotEmpty(result) && result.contains("PSS_PublishedPart") && "Standard".equals(psRefAttributes.get("PSS_PublishedPart.PSS_StandardReference"))) {
            isStandardMBOM = true;
        }
        // Rutuja Ekatpure:TIGTK-10100:2/10/2017:End
        if (UIUtil.isNotNullAndNotEmpty(result) || result.contains("PSS_PublishedPart")) {
            psInstList = PSS_FRCMBOMModelerUtility_mxJPO.getVPMStructureMQL(context, psRefID, busSelect, relSelect, (short) 1, null,
                    "(attribute[PSS_PublishedEBOM.PSS_InstanceName]==EBOM || attribute[PSS_PublishedEBOM.PSS_InstanceName]=='')"); // Expand
            // first
            // level
        } else {
            psInstList = PSS_FRCMBOMModelerUtility_mxJPO.getVPMStructure(context, plmSession, psRefID, busSelect, relSelect, (short) 1, null, null); // Expand first level
        }
        // MapList psInstList = FRCMBOMModelerUtility.getVPMStructure(context,
        // plmSession, psRefID, busSelect, relSelect, (short) 1, null, null); //
        // Expand first level

        if (mbomParentRef != null && !"".equals(mbomParentRef)) {
            DomainObject domParentMBOM = DomainObject.newInstance(context, mbomParentRef);
            String strParentMBOMPolicy = domParentMBOM.getInfo(context, DomainConstants.SELECT_POLICY);
            if (TigerConstants.POLICY_PSS_STANDARDMBOM.equalsIgnoreCase(strParentMBOMPolicy))
                PropertyUtil.setRPEValue(context, STANDARDROOTMBOM, "true", true);
            else
                PropertyUtil.setRPEValue(context, STANDARDROOTMBOM, "false", true);
        }
        // Bug #231 - DCP - END
        if ((psInstList == null || psInstList.size() == 0) && mbomParentRef != null && !"".equals(mbomParentRef)) {
            // This is a leaf node of the PS (and it is not the root) : create a
            // Provide under the MBOM path, with implement link and effectivity

            String[] argsForImplement = new String[4];
            argsForImplement[0] = mbomCompleteParentPath;
            argsForImplement[1] = psCompletePath;

            List<String> newRefPIDListFromLinkProcess = new ArrayList<String>();
            List<String> mbomRefPIDScopedWithPSRefList = PSS_FRCMBOMModelerUtility_mxJPO.getScopingReferencesFromList(context, plmSession, psRefID);

            String strRPEValueId = PropertyUtil.getRPEValue(context, MBOMDONOTREUSE, true);
            if (strRPEValueId.equalsIgnoreCase("true") && !mbomRefPIDScopedWithPSRefList.isEmpty() && UIUtil.isNotNullAndNotEmpty(mbomRefPIDScopedWithPSRefList.get(0))) {
                newMBOMRefPID = mbomRefPIDScopedWithPSRefList.get(0);
                if (isStandardMBOM) {
                    StringList slPlantConnected = pss.mbom.MBOMUtil_mxJPO.getConsumerPlantOnStandardMBOM(context, newMBOMRefPID);
                    if (!slPlantConnected.contains(plantPID)) {
                        PropertyUtil.setRPEValue(context, PLANTFROMENOVIA, "true", false);
                        PSS_FRCMBOMModelerUtility_mxJPO.attachPlantToMBOMReference(context, plmSession, (String) newMBOMRefPID, plantPID);
                        String strquery = "query path type SemanticRelation containing " + newMBOMRefPID + " where owner.type=='MfgProductionPlanning' select owner.physicalid dump |";
                        String listPathIds = MqlUtil.mqlCommand(context, strquery, false, false);
                        String[] slPathIds = listPathIds.split("\n");
                        if (UIUtil.isNotNullAndNotEmpty(listPathIds)) {
                            String strMfgPlanningId = (slPathIds[slPathIds.length - 1]).split("\\|")[1];
                            PropertyUtil.setGlobalRPEValue(context, MODIFYPLANT, "false");
                            updateConsumerPlantOnMBOM(context, strMfgPlanningId);
                        }
                    }
                }
                newRefPIDList.add(newMBOMRefPID);
            } else {
                setImplementLinkProcess(context, plmSession, argsForImplement, newRefPIDListFromLinkProcess, plantPID);
            }
        } else {
            if (psInstList != null)
                psInstList.sortStructure("attribute[PLMInstance.V_TreeOrder].value", "ascending", "real");

            // This is an intermediate node of the PS (and it is not the root) : insert a new CreateAssembly under the MBOM reference, and process recursively for each child instance

            // Get the attribute values of the PS reference
            // DomainObject psRefObj = new DomainObject(psRefID);
            // Map psRefAttributes = psRefObj.getAttributeMap(context, true);
            HashMap<String, String> mbomRefAttributes = new HashMap<String, String>();
            mbomRefAttributes.put("PLMEntity.V_Name", (String) psRefAttributes.get("PLMEntity.V_Name"));
            mbomRefAttributes.put("PLMEntity.V_description", (String) psRefAttributes.get("PLMEntity.V_description"));

            // Create a new MBOM reference
            // PSS : START
            // newMBOMRefPID = createMBOMReference(context, plmSession, "CreateAssembly", null, mbomRefAttributes);
            if (UIUtil.isNullOrEmpty(parentType)) {
                parentType = "CreateAssembly";
            }
            List<String> mbomRefPIDScopedWithPSRefList = PSS_FRCMBOMModelerUtility_mxJPO.getScopingReferencesFromList(context, plmSession, psRefID);

            String strRPEValueId = PropertyUtil.getRPEValue(context, MBOMDONOTREUSE, true);
            if (strRPEValueId.equalsIgnoreCase("true") && !mbomRefPIDScopedWithPSRefList.isEmpty() && UIUtil.isNotNullAndNotEmpty(mbomRefPIDScopedWithPSRefList.get(0))) {
                newMBOMRefPID = mbomRefPIDScopedWithPSRefList.get(0);
            } else {// Rutuja Ekatpure:TIGTK-10100:2/10/2017:Start
                if (isStandardMBOM)
                    newMBOMRefPID = getReleasedStandardMBOMReferenceToReuse(context, mbomRefPIDScopedWithPSRefList, plantPID);
                else
                    newMBOMRefPID = getMBOMReferenceToReuse(context, plmSession, mbomRefPIDScopedWithPSRefList, plantPID);
            } // Rutuja Ekatpure:TIGTK-10100:2/10/2017:End
            String newMBOMInstPID = null;
            String strSKIPSTANDARDMBOM = PropertyUtil.getRPEValue(context, SKIPSTANDARDMBOM, true);
            if (isStandardMBOM && "true".equalsIgnoreCase(strSKIPSTANDARDMBOM)) {
                PropertyUtil.setRPEValue(context, MBOMDONOTREUSE, "true", true);
                checkRPE = true;
            } else {
                if (UIUtil.isNullOrEmpty(newMBOMRefPID)) {// Rutuja Ekatpure:TIGTK-10100:2/10/2017:Start
                    newMBOMRefPID = createMBOMReferenceWithPlant(context, plmSession, parentType, null, mbomRefAttributes, psRefID, plantPID);
                    // Replicate all the attributes values on the new MBOM reference
                    DomainObject newMBOMRefObj = new DomainObject(newMBOMRefPID);
                    // FRC: Fixed Bug#497-MBOM "Description" attribute not filled automatically
                    mbomRefAttributes.put("PLMReference.V_ApplicabilityDate", (String) psRefAttributes.get("PLMReference.V_ApplicabilityDate"));
                    newMBOMRefObj.setAttributeValues(context, mbomRefAttributes);
                    PSS_FRCMBOMModelerUtility_mxJPO.createScopeLinkAndReduceChildImplementLinks(context, plmSession, newMBOMRefPID, psRefID, false);
                } else if (UIUtil.isNotNullAndNotEmpty(mbomParentRef)) {
                    PropertyUtil.setRPEValue(context, MBOMDONOTREUSE, "true", true);
                    checkRPE = true;
                }

                // PSS : END
                // lModelListOnStructure
                if (valueEnvAttachModel == null) {
                    List<String> lModels = FRCMBOMModelerUtility.getAttachedModelsOnReference(context, plmSession, psRefID);
                    if (null != lModels && 0 < lModels.size()) {
                        lModelListOnStructure.addAll(lModels);
                    }
                }

                newRefPIDList.add(newMBOMRefPID);

                if (mbomParentRef == null || "".equals(mbomParentRef)) { // This is the root node of the PS
                    // Set scope and attach model
                    // PSS_FRCMBOMModelerUtility_mxJPO.createScopeLinkAndReduceChildImplementLinks(context, plmSession, newMBOMRefPID, psRefID, false);

                    List<String> newMBOMRefPIDList = new ArrayList<String>();
                    newMBOMRefPIDList.add(newMBOMRefPID);

                    List<String> modelsToAttachToRootList = new ArrayList<String>();
                    for (String modelPID : modelsToAttachToRoot)
                        modelsToAttachToRootList.add(modelPID);

                    FRCMBOMModelerUtility.attachModelsOnReferencesAndSetOptionsCriteria(context, plmSession, newMBOMRefPIDList, modelsToAttachToRootList);

                } else {
                    // Get the attribute values of the instance
                    String[] psCompletePathList = psCompletePath.split("/");
                    String psInstPID = psCompletePathList[psCompletePathList.length - 1];
                    DomainRelationship psInstObj = new DomainRelationship(psInstPID);
                    Map psInstAttributes = psInstObj.getAttributeMap(context, true);
                    Map mbomInstAttributes = new HashMap();
                    // mbomInstAttributes.put("PLMInstance.PLM_ExternalID", psInstAttributes.get("PLMInstance.PLM_ExternalID"));
                    mbomInstAttributes.put("PLMInstance.V_description", psInstAttributes.get("PLMInstance.V_description"));
                    // Fixed Bug 231-Tree Ordering
                    String psTreeOrder = (String) psInstAttributes.get("PLMInstance.V_TreeOrder");
                    String psVName = (String) psInstAttributes.get("PLMInstance.V_Name");
                    String psExternalID = (String) psInstAttributes.get("PLMInstance.PLM_ExternalID");
                    mbomInstAttributes.put("PLMInstance.V_Name", psVName);
                    mbomInstAttributes.put("PLMInstance.PLM_ExternalID", psExternalID);
                    mbomInstAttributes.put("PLMInstance.V_TreeOrder", psTreeOrder);

                    // Create a new instance
                    // PSS: START
                    newMBOMInstPID = getInstanceToReuse(context, mbomParentRef, newMBOMRefPID, psTreeOrder, psExternalID, psVName);
                    if (UIUtil.isNullOrEmpty(newMBOMInstPID)) {
                        newMBOMInstPID = createInstance(context, plmSession, mbomParentRef, newMBOMRefPID);

                        String trimmedPSPath = trimPSPathToClosestScope(context, plmSession, mbomCompleteParentPath + "/" + newMBOMInstPID, psCompletePath);

                        if (trimmedPSPath == null || "".equals(trimmedPSPath)) {
                            throw new Exception("No scope exists.");
                        }

                        // Remove any existing implement link
                        PSS_FRCMBOMModelerUtility_mxJPO.deleteImplementLink(context, plmSession, newMBOMInstPID, true);

                        // Put a new implement link
                        List<String> mbomLeafInstancePIDList = new ArrayList();
                        mbomLeafInstancePIDList.add(newMBOMInstPID);
                        List<String> trimmedPSPathList = new ArrayList();
                        trimmedPSPathList.add(trimmedPSPath);
                        String retStr = PSS_FRCMBOMModelerUtility_mxJPO.setImplementLinkBulk(context, plmSession, mbomLeafInstancePIDList, trimmedPSPathList, true);
                        if (!"".equals(retStr))
                            throw new Exception(retStr);
                        // PSS: SART
                        PSS_FRCMBOMModelerUtility_mxJPO.setFCSIndexOnMBOM(context, psRefID, newMBOMRefPID);
                        // disconnectVaraintAssemblyFromMBOMAndCreateNew(context, newMBOMRefPID, psRefID);
                        // PSS: END
                    }

                    // Rutuja Ekatpure:TIGTK-10100:31/10/2017:start
                    if (isStandardMBOM) {
                        StringList slPlantConnected = pss.mbom.MBOMUtil_mxJPO.getConsumerPlantOnStandardMBOM(context, newMBOMRefPID);
                        if (!slPlantConnected.contains(plantPID)) {
                            PropertyUtil.setRPEValue(context, PLANTFROMENOVIA, "true", false);
                            PSS_FRCMBOMModelerUtility_mxJPO.attachPlantToMBOMReference(context, plmSession, (String) newMBOMRefPID, plantPID);
                            String strquery = "query path type SemanticRelation containing " + newMBOMRefPID + " where owner.type=='MfgProductionPlanning' select owner.physicalid dump |";
                            String listPathIds = MqlUtil.mqlCommand(context, strquery, false, false);
                            String[] slPathIds = listPathIds.split("\n");
                            if (UIUtil.isNotNullAndNotEmpty(listPathIds)) {
                                String strMfgPlanningId = (slPathIds[slPathIds.length - 1]).split("\\|")[1];
                                PropertyUtil.setGlobalRPEValue(context, MODIFYPLANT, "false");
                                updateConsumerPlantOnMBOM(context, strMfgPlanningId);
                            }
                        }
                    }
                    // Rutuja Ekatpure:TIGTK-10100:31/10/2017:End
                    // PSS: END
                    // Replicate all the attributes values on the new instance
                    DomainRelationship newMBOMInstObj = new DomainRelationship(newMBOMInstPID);
                    newMBOMInstObj.setAttributeValues(context, mbomInstAttributes);

                }

                // Bug #231 - DCP - START
                if (psInstList != null) {
                    for (int i = 0; i < psInstList.size(); i++) {
                        Map<String, String> psInstInfo = (Map<String, String>) psInstList.get(i);
                        if (mbomParentRef == null || "".equals(mbomParentRef)) { // This is the root node of the PS
                            createMBOMFromEBOMLikePSRecursive(context, plmSession, newMBOMRefPID, psInstInfo.get("physicalid"), newMBOMRefPID, psRefID + "/" + psInstInfo.get("physicalid[connection]"),
                                    modelsToAttachToRoot, newRefPIDList, plantPID, null);
                        } else {
                            createMBOMFromEBOMLikePSRecursive(context, plmSession, newMBOMRefPID, psInstInfo.get("physicalid"), mbomCompleteParentPath + "/" + newMBOMInstPID,
                                    psCompletePath + "/" + psInstInfo.get("physicalid[connection]"), modelsToAttachToRoot, newRefPIDList, plantPID, null);
                        }
                    }
                }
            }
            String strcheckRPE = PropertyUtil.getRPEValue(context, MBOMDONOTREUSE, true);
            if (strcheckRPE.equalsIgnoreCase("true")) {
                PropertyUtil.setRPEValue(context, MBOMDONOTREUSE, "false", true);
            }
            // Bug #231 - DCP - END
        } // TIGTK- 12165 : START
        String strcheckRPE = PropertyUtil.getRPEValue(context, MBOMDONOTREUSE, true);
        if (strcheckRPE.equalsIgnoreCase("true")) {
            PropertyUtil.setRPEValue(context, MBOMDONOTREUSE, "false", true);
        }
        // TIGTK- 12165 : END
        return newMBOMRefPID;
    }

    /**
     * @UniqueMBOMPerPlant
     * @PolicyChange
     * @param context
     * @param plmSession
     * @param psRefPID
     * @param mbomRefPID
     * @param newRefPIDList
     * @return
     * @throws Exception
     */
    public static String getSynchedScopeMBOMRefFromPSRef(Context context, PLMCoreModelerSession plmSession, String psRefPID, String mbomRefPID, List<String> newRefPIDList, String plantPID)
            throws Exception {
        String returnMBOMRefPID = null;
        HashMap<String, String> attributes = new HashMap<String, String>();
        // TIGTK-10100:Rutuja Ekatpure:Start
        // Check if PS reference has a scope
        List<String> mbomRefPIDScopedWithPSRefList = PSS_FRCMBOMModelerUtility_mxJPO.getScopingReferencesFromList(context, plmSession, psRefPID);
        // to check wheather MBOM is standardMBOM or not by using Standard reference attribute on VPLMReference
        boolean isStandardMBOM = false;
        String result = MqlUtil.mqlCommand(context, "print bus " + psRefPID + " select interface dump |", false, false);
        DomainObject psRefObj = new DomainObject(psRefPID);
        Map psRefAttributes = psRefObj.getAttributeMap(context, true);
        if (UIUtil.isNotNullAndNotEmpty(result) && result.contains("PSS_PublishedPart") && "Standard".equals(psRefAttributes.get("PSS_PublishedPart.PSS_StandardReference"))) {
            isStandardMBOM = true;
        }
        // TIGTK-10100:Rutuja Ekatpure:Start
        if (mbomRefPIDScopedWithPSRefList.size() > 1) { // PS reference has multiple MBOM scopes
            // If all the elements of the list are within the same revision family, return the latest revision
            boolean isSameMajorIds = true;
            String lastMajorIdsStr = null;

            for (String refPID : mbomRefPIDScopedWithPSRefList) {
                String majorIdsStr = MqlUtil.mqlCommand(context, "print bus " + refPID + " select majorids dump |", false, false);

                if (lastMajorIdsStr != null) {
                    if (!lastMajorIdsStr.equals(majorIdsStr))
                        isSameMajorIds = false;
                }

                lastMajorIdsStr = majorIdsStr;
            }

            if (isSameMajorIds) {
                String[] lastMajorIds = lastMajorIdsStr.split("\\|");

                // Return this MBOM scope
                returnMBOMRefPID = lastMajorIds[lastMajorIds.length - 1];

                // Check if it is a provide
                String returnMBOMRefType = MqlUtil.mqlCommand(context, "print bus " + returnMBOMRefPID + " select type dump |", false, false);

                boolean isDirect = false;

                for (String typeInList : baseTypesForMBOMLeafNodes) {
                    if (getDerivedTypes(context, typeInList).contains(returnMBOMRefType))
                        isDirect = true;
                }

                if (!isDirect)
                    throw new Exception("The Part (" + psRefPID + ") is already scoped with a Manufacturing Item (" + returnMBOMRefPID + ") which is not a Provide or a Create Material.");

                // List<String> lastMbomRefPIDScopedWithPSRefList = new ArrayList<String>();
                // lastMbomRefPIDScopedWithPSRefList.add(returnMBOMRefPID);
                // TIGTK-10100:Rutuja Ekatpure:Start
                // to reuse released Standard MBOM
                if (isStandardMBOM) {
                    returnMBOMRefPID = getReleasedStandardMBOMReferenceToReuse(context, mbomRefPIDScopedWithPSRefList, plantPID);
                } else {
                    returnMBOMRefPID = getMBOMReferenceToReuse(context, plmSession, mbomRefPIDScopedWithPSRefList, plantPID);
                }
                // TIGTK-10100:Rutuja Ekatpure:End
                if (UIUtil.isNullOrEmpty(returnMBOMRefPID) && !isStandardMBOM) {
                    returnMBOMRefPID = createMBOMReferenceWithPlant(context, plmSession, "CreateMaterial", null, attributes, psRefPID, plantPID);
                    // Create a scope link between PS reference and MBOM reference
                    PSS_FRCMBOMModelerUtility_mxJPO.createScopeLinkAndReduceChildImplementLinks(context, plmSession, returnMBOMRefPID, psRefPID, false);
                }
            } else {
                // PSS: START
                // TIGTK-10100:Rutuja Ekatpure:Start
                // to reuse released Standard MBOM
                if (isStandardMBOM) {
                    returnMBOMRefPID = getReleasedStandardMBOMReferenceToReuse(context, mbomRefPIDScopedWithPSRefList, plantPID);
                } else {
                    returnMBOMRefPID = getMBOMReferenceToReuse(context, plmSession, mbomRefPIDScopedWithPSRefList, plantPID);
                }

                if (UIUtil.isNullOrEmpty(returnMBOMRefPID) && !isStandardMBOM) {
                    returnMBOMRefPID = createMBOMReferenceWithPlant(context, plmSession, "CreateMaterial", null, attributes, psRefPID, plantPID);
                    // Create a scope link between PS reference and MBOM reference
                    PSS_FRCMBOMModelerUtility_mxJPO.createScopeLinkAndReduceChildImplementLinks(context, plmSession, returnMBOMRefPID, psRefPID, false);
                }
                // TIGTK-10100:Rutuja Ekatpure:End

            }
        } else if (mbomRefPIDScopedWithPSRefList.size() == 1) { // PS reference has already one MBOM scope
            // TIGTK-10100:Rutuja Ekatpure:Start
            if (isStandardMBOM) {
                returnMBOMRefPID = getReleasedStandardMBOMReferenceToReuse(context, mbomRefPIDScopedWithPSRefList, plantPID);
                if (returnMBOMRefPID == null) {
                    PropertyUtil.setRPEValue(context, MBOMDONOTREUSE, "true", true);
                }
            } else {
                returnMBOMRefPID = getMBOMReferenceToReuse(context, plmSession, mbomRefPIDScopedWithPSRefList, plantPID);

                if (returnMBOMRefPID == null) {
                    returnMBOMRefPID = createMBOMReferenceWithPlant(context, plmSession, "CreateMaterial", null, attributes, psRefPID, plantPID);
                    // FRC: Fixed Bug#497-MBOM "Description" attribute not filled automatically
                    String psRefDescription = MqlUtil.mqlCommand(context, "print bus " + psRefPID + " select attribute[PLMEntity.V_description].value dump |", false, false);
                    MqlUtil.mqlCommand(context, "mod bus " + returnMBOMRefPID + " PLMEntity.V_description '" + psRefDescription + "'", false, false);
                    String psRefApplicabilityDate = MqlUtil.mqlCommand(context, "print bus " + psRefPID + " select attribute[PLMReference.V_ApplicabilityDate].value dump |", false, false);
                    MqlUtil.mqlCommand(context, "mod bus " + returnMBOMRefPID + " PLMReference.V_ApplicabilityDate '" + psRefApplicabilityDate + "'", false, false);

                    // Create a scope link between PS reference and MBOM reference
                    PSS_FRCMBOMModelerUtility_mxJPO.createScopeLinkAndReduceChildImplementLinks(context, plmSession, returnMBOMRefPID, psRefPID, false);
                }
                /*
                 * for not to reuse if plant is not same : TIGTK-12165 : START PropertyUtil.setRPEValue(context, MBOMDONOTREUSE, "true", true); TIGTK-12165 : END
                 */
                // Check if it is a provide
                String returnMBOMRefType = MqlUtil.mqlCommand(context, "print bus " + returnMBOMRefPID + " select type dump |", false, false);
                boolean isDirect = false;
                for (String typeInList : baseTypesForMBOMLeafNodes) {
                    if (getDerivedTypes(context, typeInList).contains(returnMBOMRefType))
                        isDirect = true;
                }
                if (!isDirect)
                    throw new Exception("The Part (" + psRefPID + ") is already scoped with a Manufacturing Item (" + returnMBOMRefPID + ") which is not a Provide or a Create Material.");
            }

        } else { // PS reference does not already have an MBOM scope
            // Get the previous revision of the PS reference
            String previousRevPSRefPID = MqlUtil.mqlCommand(context, "print bus " + psRefPID + " select previous.physicalid dump |", false, false);

            if ("".equals(previousRevPSRefPID)) { // PS reference does not have a previous revision
                String strRPEValueId = PropertyUtil.getRPEValue(context, MBOMDONOTREUSE, true);
                if (mbomRefPID == null || strRPEValueId.equalsIgnoreCase("true")) { // MBOM reference is null
                    // Create a new Provide and return it
                    // HashMap programMap = new HashMap();
                    // programMap.put("TypeActual", "Provide");
                    // programMap.put("FRCMBOMGetChangeObject", changeObjectPID);

                    // PSS: START
                    // returnMBOMRefPID = createMBOMReference(context, plmSession, "Provide", null, attributes);
                    returnMBOMRefPID = getMBOMReferenceToReuse(context, plmSession, mbomRefPIDScopedWithPSRefList, plantPID);
                    if (returnMBOMRefPID == null) {
                        returnMBOMRefPID = createMBOMReferenceWithPlant(context, plmSession, "CreateMaterial", null, attributes, psRefPID, plantPID);
                        // FRC: Fixed Bug#497-MBOM "Description" attribute not filled automatically
                        String psRefDescription = MqlUtil.mqlCommand(context, "print bus " + psRefPID + " select attribute[PLMEntity.V_description].value dump |", false, false);
                        MqlUtil.mqlCommand(context, "mod bus " + returnMBOMRefPID + " PLMEntity.V_description '" + psRefDescription + "'", false, false);
                        String psRefApplicabilityDate = MqlUtil.mqlCommand(context, "print bus " + psRefPID + " select attribute[PLMReference.V_ApplicabilityDate].value dump |", false, false);
                        MqlUtil.mqlCommand(context, "mod bus " + returnMBOMRefPID + " PLMReference.V_ApplicabilityDate '" + psRefApplicabilityDate + "'", false, false);

                        // By default, the Title on the new Provide should be the same as the Title of the VPMReference linked to it
                        String psRefTitle = MqlUtil.mqlCommand(context, "print bus " + psRefPID + " select attribute[PLMEntity.V_Name].value dump |", false, false);
                        MqlUtil.mqlCommand(context, "mod bus " + returnMBOMRefPID + " PLMEntity.V_Name '" + psRefTitle + "'", false, false);
                    }
                    // PSS: END

                    newRefPIDList.add(returnMBOMRefPID);
                    // String[] createArgs = JPO.packArgs(programMap);
                    // Map<String, String> newProvideMap = createNewManufItem(context, createArgs);
                    // returnMBOMRefPID = newProvideMap.get("id");

                    // Create a scope link between PS reference and MBOM reference
                    PSS_FRCMBOMModelerUtility_mxJPO.createScopeLinkAndReduceChildImplementLinks(context, plmSession, returnMBOMRefPID, psRefPID, false);
                } else { // MBOM reference is not null
                    List<String> inputListForGetScope = new ArrayList<String>();
                    inputListForGetScope.add(mbomRefPID);
                    String psRefScopePID = PSS_FRCMBOMModelerUtility_mxJPO.getScopedPSReferencePIDFromList(context, plmSession, inputListForGetScope).get(0);

                    if (psRefScopePID != null && !"".equals(psRefScopePID)) { // MBOM reference has already a PS scope : throw a new exception
                        throw new Exception("This MBOM node already has a scope, and it is not the EBOM part you are providing !");
                    } else { // MBOM reference does not already have a scope
                        // Return the MBOM reference
                        returnMBOMRefPID = mbomRefPID;

                        // Create a scope link between PS reference and MBOM reference
                        PSS_FRCMBOMModelerUtility_mxJPO.createScopeLinkAndReduceChildImplementLinks(context, plmSession, returnMBOMRefPID, psRefPID, false);
                    }
                }
            } else { // PS reference has a previous revision
                // Recursive call on previous revision (with MBOM reference in parameter)
                String mbomRefPIDSynchedToPreviousPSRevision = getSynchedScopeMBOMRefFromPSRef(context, plmSession, previousRevPSRefPID, mbomRefPID, newRefPIDList, plantPID);

                // New revision on the MBOM reference returned by the recursive call and return this new MBOM reference
                returnMBOMRefPID = newRevisionMBOMReference(context, plmSession, mbomRefPIDSynchedToPreviousPSRevision);

                newRefPIDList.add(returnMBOMRefPID);

                // !! CAREFULL : remove the scope on the new revision of the MBOM reference (by default, the new revision duplicates the scope)
                List<String> modifiedInstanceList = PSS_FRCMBOMModelerUtility_mxJPO.deleteScopeLinkAndDeleteChildImplementLinks(context, plmSession, returnMBOMRefPID, false);
                logger.info("FRC : modified Instances : ", modifiedInstanceList);

                // Create a scope link between the PS reference and the new MBOM reference revision
                PSS_FRCMBOMModelerUtility_mxJPO.createScopeLinkAndReduceChildImplementLinks(context, plmSession, returnMBOMRefPID, psRefPID, false);
            }
        }

        return returnMBOMRefPID;
    }

    /**
     * @UniqueMBOMPerPlant
     * @param context
     * @param plmSession
     * @param psRefPID
     * @param mbomRefPID
     * @param newRefPIDList
     * @param plantPID
     * @return
     * @throws Exception
     */
    public static String getMBOMReferenceToReuse(Context context, PLMCoreModelerSession plmSession, List<String> scopedRefPIDList, String plantPID) throws Exception {
        try {
            if (scopedRefPIDList != null) {
                for (String refPLMID : scopedRefPIDList) {

                    String refPID = PLMID.buildFromString(refPLMID).getPid();
                    String strAttachedPlant = pss.mbom.MBOMUtil_mxJPO.getMasterPlant(context, refPID);
                    if (UIUtil.isNotNullAndNotEmpty(strAttachedPlant)) {
                        if (strAttachedPlant.equalsIgnoreCase(plantPID)) {
                            String query = "print bus " + refPID + " select majorids dump |;";
                            String strResult = MqlUtil.mqlCommand(context, query, false, false);
                            if (UIUtil.isNotNullAndNotEmpty(strResult)) {
                                if (strResult.contains("|")) {
                                    String[] strMajorIds = strResult.split("\\|");
                                    String strFinalRefId = strMajorIds[strMajorIds.length - 1];
                                    return strFinalRefId;
                                } else
                                    return strResult;
                            }
                        }
                    }
                }
            }

            return null;
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * @UniqueMBOMPerPlant
     * @param context
     * @param plmSession
     * @param psRefPID
     * @param mbomRefPID
     * @param newRefPIDList
     * @param plantPID
     * @return
     * @throws Exception
     */
    public static boolean checkForExistingMBOMWithPlant(Context context, PLMCoreModelerSession plmSession, String psRefPID, String plantPID) throws Exception {
        try {
            List<String> scopedMBOMRefIDs = PSS_FRCMBOMModelerUtility_mxJPO.getScopingReferencesFromList(context, plmSession, psRefPID);
            if (scopedMBOMRefIDs != null) {
                for (String refPID : scopedMBOMRefIDs) {
                    String strAttachedPlant = pss.mbom.MBOMUtil_mxJPO.getMasterPlant(context, refPID);
                    if (UIUtil.isNotNullAndNotEmpty(strAttachedPlant)) {
                        if (strAttachedPlant.equalsIgnoreCase(plantPID)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * @PolicyChange
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Map createMBOMFromEBOM(Context context, String[] args) throws Exception {// Called from command FRCCreateMBOM
        Map returnMap = new HashMap();
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String sCreationMode = (String) programMap.get("selmbomtype"); // Possible values : CopyMBOM - FromEBOM - NewAssembly
        String strObjectId = (String) programMap.get("objectId");
        String strPlantId = (String) programMap.get("PSS_PlantOID");

        PLMCoreModelerSession plmSession = null;
        boolCreateMBOM = true;
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();
            // TIGTK-10606:Rutuja Ekatpure:9/11/2017:Start
            DomainObject domPlant = DomainObject.newInstance(context, strPlantId);
            String strPlantPhyId = domPlant.getInfo(context, "physicalid");
            // This method check if MBOM created with same plant from any revisions of part.
            checkMBOMCreatedForOtherRevisionsOfPart(context, plmSession, strObjectId, strPlantPhyId);
            // TIGTK-10606:Rutuja Ekatpure:9/11/2017:End

            String newObjID = "";
            if ("NewAssembly".equals(sCreationMode)) {
                newObjID = createMBOMFromEBOMEmptyStructure(context, plmSession, args);
            } else if ("CopyMBOM".equals(sCreationMode)) {
                newObjID = createMBOMFromEBOMFromTemplate(context, plmSession, args);
            } else if ("FromEBOM".equals(sCreationMode)) {
                PropertyUtil.setRPEValue(context, PLANTOWNERSHIP, "False", true);
                newObjID = createMBOMFromEBOMLikePS_new(context, plmSession, args);
            }

            if (null != newObjID && !"".equals(newObjID))
                returnMap.put("id", newObjID);

            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);
        } catch (Exception e) {
            String strErrorMesssage = e.getMessage();
            getErrorMessage(context, strErrorMesssage);
            flushAndCloseSession(plmSession);
            if (context.isTransactionActive()) {
                ContextUtil.abortTransaction(context);
            }
            throw e;
        } finally {
            boolCreateMBOM = false;
        }

        return returnMap;
    }

    private static void getErrorMessage(Context context, String strErrorMesssage) throws Exception {

        String strMaterialMsg = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.ErrorMessage.MatrialNotFound");
        String strColorMsg = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.ErrorMessage.ColorNotToConnect");
        String strEBOMInaccessibleMsg = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.ErrorMessage.EBOMInaccessible");
        String strEBOMInaccessibleMsgforColor = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(),
                "PSS_FRCMBOMCentral.ErrorMessage.EBOMInaccessibleforColor");
        String strName = DomainObject.EMPTY_STRING;
        String strType = DomainObject.EMPTY_STRING;
        String strRevision = DomainObject.EMPTY_STRING;
        StringBuffer sbBuffer = new StringBuffer();
        boolean isContextPush = false;
        try {

            if (strErrorMesssage.contains(strMaterialMsg)) {
                String[] strSplit = strErrorMesssage.split(" does not exist");
                String strObjectId = DomainObject.EMPTY_STRING;
                if (strSplit.length > 0) {
                    strObjectId = strSplit[0].substring(strSplit[0].lastIndexOf(' ') + 1);
                }
                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), "", "");
                isContextPush = true;
                if (UIUtil.isNotNullAndNotEmpty(strObjectId)) {
                    DomainObject domObject = new DomainObject(strObjectId.trim());
                    strName = domObject.getInfo(context, DomainConstants.SELECT_NAME);
                    strType = domObject.getInfo(context, DomainConstants.SELECT_TYPE);
                    strRevision = domObject.getInfo(context, DomainConstants.SELECT_REVISION);
                }
                sbBuffer.append(strEBOMInaccessibleMsg);
                sbBuffer.append(" : ");
                sbBuffer.append(strType);
                sbBuffer.append(" ");
                sbBuffer.append(strName);
                sbBuffer.append(" ");
                sbBuffer.append(strRevision);
                throw new Exception(sbBuffer.toString());

            }
            if (strErrorMesssage.contains(strColorMsg)) {
                String[] strSplit = strErrorMesssage.split(" Severity:2 ErrorCode:1500028");
                String strObjectDetails = DomainObject.EMPTY_STRING;
                if (strSplit.length > 0) {
                    strObjectDetails = strSplit[0].substring(strSplit[0].indexOf("'") + 1);
                    strObjectDetails = strObjectDetails.substring(0, strObjectDetails.length() - 1);
                }
                sbBuffer.append(strEBOMInaccessibleMsgforColor);
                sbBuffer.append(" : ");
                sbBuffer.append(strObjectDetails);
                throw new Exception(sbBuffer.toString());
            }
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (isContextPush)
                ContextUtil.popContext(context);
        }

    }

    /**
     * @PolicyChange
     * @param context
     * @param plmSession
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public static String createMBOMFromEBOMEmptyStructure(Context context, PLMCoreModelerSession plmSession, String[] args) throws Exception {
        String sRet = "";

        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String sPartId = (String) programMap.get("objectId");
        // PSS: START
        String plantId = (String) programMap.get("PSS_PlantOID");
        DomainObject plantObj = DomainObject.newInstance(context, plantId);
        plantId = plantObj.getInfo(context, "physicalid");
        // PSS: END

        MapList mlPrd = getProductFromEBOM(context, sPartId);
        if (null != mlPrd) {
            if (1 < mlPrd.size())
                throw new Exception("Several VPM Products have been found for the given EBOM part");
            if (mlPrd.size() == 0)
                throw new Exception(EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.ErrorMessage.NoVPMProduct"));
            Map mPrd = (Map) mlPrd.get(0);
            String sPrdPhysId = (String) mPrd.get("physicalid");
            if (null != sPrdPhysId && !"".equals(sPrdPhysId)) {
                // Create new manuf item
                String type = (String) programMap.get("TypeActual");

                HashMap<String, String> attributes = new HashMap<String, String>();
                // attributes.put("PLM_ExternalID", "9995");

                // PSS : START
                // String newRefPID = createMBOMReference(context, plmSession, type, null, attributes);
                if (checkForExistingMBOMWithPlant(context, plmSession, sPrdPhysId, plantId)) {
                    throw new Exception(
                            EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.Error.Message.CanNotCreateNewItemWithPlant"));
                }
                String newRefPID = createMBOMReferenceWithPlant(context, plmSession, type, null, attributes, sPrdPhysId, plantId);
                // PSS : END

                String changeObjectName = (String) programMap.get("FRCMBOMGetChangeObject");
                // Modif AFN - Test if a value has been defined into the creation web form
                String changeObjectFromForm = (String) programMap.get("ChangeObjectCreateForm");
                if (null != changeObjectFromForm && !"".equals(changeObjectFromForm))
                    changeObjectName = changeObjectFromForm;
                attachObjectToChange(context, plmSession, changeObjectName, newRefPID);

                // Set scope and attach model
                PSS_FRCMBOMModelerUtility_mxJPO.createScopeLinkAndReduceChildImplementLinks(context, plmSession, newRefPID, sPrdPhysId, false);

                // Before attaching Models, the plmSession needs to be flushed, otherwise the new objects will not be seen...
                flushSession(plmSession);

                List<String> lModelsArray = FRCMBOMModelerUtility.getAttachedModelsOnReference(context, plmSession, sPrdPhysId);

                List<String> newRefPIDList = new ArrayList<String>();
                newRefPIDList.add(newRefPID);

                FRCMBOMModelerUtility.attachModelsOnReferencesAndSetOptionsCriteria(context, plmSession, newRefPIDList, lModelsArray);

                sRet = newRefPID;
            }
        } else {
            throw new Exception(EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.ErrorMessage.NoVPMProduct"));
        }

        return sRet;
    }

    /**
     * @UniqueMBOMPerPlant
     * @PolicyChange
     * @param context
     * @param plmSession
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public static String createMBOMFromEBOMFromTemplate(Context context, PLMCoreModelerSession plmSession, String[] args) throws Exception {
        String sRet = "";

        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String sPartId = (String) programMap.get("objectId");

        String templateRootRefID = (String) programMap.get("rootMbomOID");
        // PSS: START
        String plantId = (String) programMap.get("PSS_PlantOID");
        DomainObject plantObj = DomainObject.newInstance(context, plantId);
        plantId = plantObj.getInfo(context, "physicalid");
        // PSS: END

        // Get the change object
        String changeObjectName = (String) programMap.get("FRCMBOMGetChangeObject");
        // Modif AFN - Test if a value has been defined into the creation web form
        String changeObjectFromForm = (String) programMap.get("ChangeObjectCreateForm");
        if (null != changeObjectFromForm && !"".equals(changeObjectFromForm))
            changeObjectName = changeObjectFromForm;

        MapList mlPrd = getProductFromEBOM(context, sPartId);
        if (null != mlPrd) {
            if (1 < mlPrd.size())
                throw new Exception("Several VPM Products have been found for the given EBOM part");
            if (mlPrd.size() == 0)
                throw new Exception(EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.ErrorMessage.NoVPMProduct"));
            Map mPrd = (Map) mlPrd.get(0);
            String sPrdPhysId = (String) mPrd.get("physicalid");
            if (null != sPrdPhysId && !"".equals(sPrdPhysId)) {
                // PSS : START
                if (checkForExistingMBOMWithPlant(context, plmSession, sPrdPhysId, plantId)) {
                    throw new Exception(
                            EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.Error.Message.CanNotCreateNewItemWithPlant"));
                }
                StringList busSelect = new StringList();
                busSelect.add("physicalid");
                StringList relSelect = new StringList();
                relSelect.add("physicalid[connection]");

                // Bug #231 - DCP - START
                relSelect.add("attribute[PLMInstance.V_TreeOrder].value");

                // MapList psInstList = getVPMStructure(context, psRefID, busSelect, relSelect, (short) 1, null); // Expand first level
                // @WorkAround
                MapList psInstList = null;
                String result = MqlUtil.mqlCommand(context, "print bus " + sPrdPhysId + " select interface dump |", false, false);
                if (UIUtil.isNotNullAndNotEmpty(result) || result.contains("PSS_PublishedPart")) {
                    psInstList = PSS_FRCMBOMModelerUtility_mxJPO.getVPMStructureMQL(context, sPrdPhysId, busSelect, relSelect, (short) 0, null,
                            "(attribute[PSS_PublishedEBOM.PSS_InstanceName]==EBOM || attribute[PSS_PublishedEBOM.PSS_InstanceName]=='')"); // Expand first level
                } else {
                    psInstList = PSS_FRCMBOMModelerUtility_mxJPO.getVPMStructure(context, plmSession, sPrdPhysId, busSelect, relSelect, (short) 0, null, null); // Expand first level
                }
                // Recursively clone the template root node
                StringList psPIDList = new StringList();
                if (psInstList != null) {
                    for (int i = 0; i < psInstList.size(); i++) {
                        psPIDList.addElement((String) ((Map) psInstList.get(i)).get("physicalid"));
                    }
                }

                String sMbomId = duplicateMBOMStructure(context, plmSession, templateRootRefID, changeObjectName, psPIDList, plantId);

                if (UIUtil.isNotNullAndNotEmpty(sMbomId)) {
                    // Set scope and attach model
                    PSS_FRCMBOMModelerUtility_mxJPO.createScopeLinkAndReduceChildImplementLinks(context, plmSession, sMbomId, sPrdPhysId, false);

                    List<String> lModelsArray = FRCMBOMModelerUtility.getAttachedModelsOnReference(context, plmSession, sPrdPhysId);

                    List<String> sMbomIdList = new ArrayList<String>();
                    sMbomIdList.add(sMbomId);

                    FRCMBOMModelerUtility.attachModelsOnReferencesAndSetOptionsCriteria(context, plmSession, sMbomIdList, lModelsArray);

                    sRet = sMbomId;
                }
            }
        } else {
            throw new Exception(EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.ErrorMessage.NoVPMProduct"));
        }

        return sRet;
    }

    /**
     * @PolicyChange
     * @param context
     * @param plmSession
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static String createMBOMFromEBOMLikePS(Context context, PLMCoreModelerSession plmSession, String[] args) throws Exception {
        String sRet = "";
        PSS_FRCMBOMModelerUtility_mxJPO.checkValidScenario(context);
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String sPartId = (String) programMap.get("objectId");
        valueEnvAttachModel = System.getenv("DISABLE_ATTACH_MODEL_ON_SCOPE");

        // PSS: START
        String plantId = (String) programMap.get("PSS_PlantOID");
        DomainObject plantObj = DomainObject.newInstance(context, plantId);
        plantId = plantObj.getInfo(context, "physicalid");
        // PSS: END

        // Get the change object
        String changeObjectName = (String) programMap.get("FRCMBOMGetChangeObject");
        // Modif AFN - Test if a value has been defined into the creation web form
        String changeObjectFromForm = (String) programMap.get("ChangeObjectCreateForm");
        if (null != changeObjectFromForm && !"".equals(changeObjectFromForm))
            changeObjectName = changeObjectFromForm;

        MapList mlPrd = getProductFromEBOM(context, sPartId);
        if (null != mlPrd) {
            if (1 < mlPrd.size())
                throw new Exception("Several VPM Products have been found for the given EBOM part");
            if (mlPrd.size() == 0)
                throw new Exception(EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.ErrorMessage.NoVPMProduct"));
            Map mPrd = (Map) mlPrd.get(0);
            String sPrdPhysId = (String) mPrd.get("physicalid");
            if (null != sPrdPhysId && !"".equals(sPrdPhysId)) {
                // PSS : START
                if (checkForExistingMBOMWithPlant(context, plmSession, sPrdPhysId, plantId)) {
                    throw new Exception(
                            EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.Error.Message.CanNotCreateNewItemWithPlant"));
                }
                // PSS : END
                List<String> lModelsArray = FRCMBOMModelerUtility.getAttachedModelsOnReference(context, plmSession, sPrdPhysId);
                lModelListOnStructure = new ArrayList<String>();
                // Recursively process the PS root node
                List<String> newRefPIDList = new ArrayList<String>();
                sRet = createMBOMFromEBOMLikePSRecursive(context, plmSession, null, sPrdPhysId, null, null, lModelsArray.toArray(new String[] {}), newRefPIDList, plantId, null);
                if (lModelListOnStructure.size() > 0) {
                    // remove duplicateModel
                    Set<String> hs = new HashSet<String>();
                    hs.addAll(lModelListOnStructure);
                    lModelListOnStructure.clear();
                    lModelListOnStructure.addAll(hs);
                    // Attach All Model to the Root
                    List lBOMRef = new ArrayList<List>();
                    lBOMRef.add(sRet);
                    context.setApplication("VPLM");
                    plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
                    plmSession.openSession();
                    FRCMBOMModelerUtility.attachModelsOnReferencesAndSetOptionsCriteria(context, plmSession, lBOMRef, lModelListOnStructure);
                    // Attcah all the Model to the root.
                }
                // Attach all created references to change object.
                attachListObjectsToChange(context, plmSession, changeObjectName, newRefPIDList);
            }
        } else {
            throw new Exception(EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.ErrorMessage.NoVPMProduct"));
        }

        return sRet;
    }

    /**
     * @PolicyChange
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public static String updateImplementLink(Context context, String[] args) throws Exception { // Called from FRCUpdateImplementLink.jsp
        // Return value :
        // 0 = Nothing needs to be updated
        // 1 = The implement link is updated (and potentially the effectivity too)
        // 2 = The implement link is updated and the Manuf Item was replaced (case of a Provide)
        // 3 = Only the effectivity is updated
        // 4 = Link is broken
        // 5 = Intermediate scope is updated
        String returnValue = "0";

        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            String mbomCompletePath = args[0];
            String changeObjectPID = args[1];

            String[] mbomCompletePathList = mbomCompletePath.split("/");
            String mbomLeafInstancePID = mbomCompletePathList[mbomCompletePathList.length - 1];
            String mbomLeafRefPID = MqlUtil.mqlCommand(context, "print connection " + mbomLeafInstancePID + " select to.physicalid dump |", false, false);

            int implementLinkUpdateInfo = PSS_FRCMBOMModelerUtility_mxJPO.updateImplementLinkFromCandidate(context, plmSession, mbomLeafInstancePID);

            if (implementLinkUpdateInfo == 0) { // Implement link is, in fact, up to date.
                returnValue = "0";
            } else if (implementLinkUpdateInfo == 1) { // Implement link is rerouted.
                returnValue = "1";

                // Get the new implement link
                // MapList newImplementLink = getImplementPIDList(context, mbomLeafInstancePID);
                List<String> newImplementLinkPIDList = PSS_FRCMBOMModelerUtility_mxJPO.getImplementLinkInfoSimple(context, plmSession, mbomLeafInstancePID);

                // If the leaf MBOM reference is a Provide, get the synched MBOM from the leaf PS reference, and replace it in the MBOM structure
                String mbomLeafRefType = MqlUtil.mqlCommand(context, "print bus " + mbomLeafRefPID + " select type dump |", false, false);

                boolean isDirect = false;

                for (String typeInList : baseTypesForMBOMLeafNodes) {
                    if (getDerivedTypes(context, typeInList).contains(mbomLeafRefType))
                        isDirect = true;
                }

                String psLeafInstPID = newImplementLinkPIDList.get(newImplementLinkPIDList.size() - 1);
                String psLeafRefPID = MqlUtil.mqlCommand(context, "print connection " + psLeafInstPID + " select to.physicalid dump |", false, false);

                if (isDirect) {
                    // Get a new synched Provide with the leaf PS reference
                    List<String> newRefPIDList = new ArrayList<String>();
                    String newProvidePID = getSynchedScopeMBOMRefFromPSRefForUpdateImplentLink(context, plmSession, psLeafRefPID, null, newRefPIDList, null);

                    // Attach all created references to change object.
                    attachListObjectsToChange(context, plmSession, changeObjectPID, newRefPIDList);

                    if (!newProvidePID.equals(mbomLeafRefPID)) { // The leaf MBOM reference is not the one synched with the leaf PS reference. Normally because it is a different revision
                        // Replace the MBOM leaf instance with the new one
                        String newMBOMLeafInstancePID = PSS_FRCMBOMModelerUtility_mxJPO.replaceMBOMInstance(context, plmSession, mbomLeafInstancePID, newProvidePID);

                        mbomCompletePath = mbomCompletePath.replace(mbomLeafInstancePID, newMBOMLeafInstancePID);
                        // mbomLeafInstancePID = newMBOMLeafInstancePID;

                        returnValue = "2";
                    }
                } else { // Case of an intermediate scoped Create Assembly
                    // Implement link is now up to date, but not the scope : update it to the leaf ref of the implement link
                    List<String> modifiedInstanceList = PSS_FRCMBOMModelerUtility_mxJPO.reconnectScopeLinkAndUpdateChildImplementLinks(context, plmSession, mbomLeafRefPID, psLeafRefPID, true);
                    logger.info("FRC : modified Instances : ", modifiedInstanceList);

                    returnValue = "5";
                }
            } else if (implementLinkUpdateInfo == 4) { // Implement link is broken
                returnValue = "4";
            } else if (implementLinkUpdateInfo == 3) { // Only the effectivity was updated
                returnValue = "3";
            }

            flushSession(plmSession);
            ContextUtil.commitTransaction(context);
            // ContextUtil.abortTransaction(context);
        } catch (Exception exp) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in updateImplementLink: ", exp);
            // TIGTK-5405 - 11-04-2017 - VB - END
            flushSession(plmSession);
            if (context.isTransactionActive()) {
                ContextUtil.abortTransaction(context);
            }
            throw exp;
        }

        return returnValue;
    }

    /**
     * @PolicyChange
     * @param context
     * @param plmSession
     * @param args
     * @param newRefPIDList
     * @return
     * @throws Exception
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static String setImplementLinkProcess(Context context, PLMCoreModelerSession plmSession, String[] args, List<String> newRefPIDList, String plantPID) throws Exception {
        // Return value :
        // 0 = refresh row of the leaf MBOM instance
        // 1 = re-expand the row of the leaf MBOM instance
        // 2 = re-expand the row of the parent of the MBOM instance
        try {
            String returnValue = "0";

            String mbomCompletePath = args[0];
            String psCompletePath = args[1];
            String approvalStatus = args[3];
            String newMBOMRefPID = null;
            List<String> newRefPIDListFromSynch = new ArrayList<String>();
            String trimmedPSPath = null;

            String[] psCompletePathList = psCompletePath.split("/");
            // DomainRelationship psInstObj = new DomainRelationship(strRootPSID);
            // String strRootMBOMPolicy = psInstObj.getInfo(context, arg1, arg2)
            String psLeafInstancePID = psCompletePathList[psCompletePathList.length - 1];
            String psLeafRefPID = MqlUtil.mqlCommand(context, "print connection " + psLeafInstancePID + " select to.physicalid dump |", false, false);

            String[] mbomCompletePathList = mbomCompletePath.split("/");
            String mbomLeafInstancePID = null;
            String mbomLeafRefPID = null;
            if (mbomCompletePathList.length > 1) {
                mbomLeafInstancePID = mbomCompletePathList[mbomCompletePathList.length - 1];
                mbomLeafRefPID = MqlUtil.mqlCommand(context, "print connection " + mbomLeafInstancePID + " select to.physicalid dump |", false, false);
            } else {
                mbomLeafRefPID = mbomCompletePathList[mbomCompletePathList.length - 1];
            }

            if (UIUtil.isNotNullAndNotEmpty(approvalStatus)) {
                if (mbomLeafRefPID != null && !"".equals(mbomLeafRefPID)) {
                    DomainObject domParentMBOM = DomainObject.newInstance(context, mbomLeafRefPID);
                    String strParentMBOMPolicy = domParentMBOM.getInfo(context, DomainConstants.SELECT_POLICY);
                    if (TigerConstants.POLICY_PSS_STANDARDMBOM.equalsIgnoreCase(strParentMBOMPolicy))
                        PropertyUtil.setRPEValue(context, STANDARDROOTMBOM, "true", true);
                    else
                        PropertyUtil.setRPEValue(context, STANDARDROOTMBOM, "false", true);
                }
                // newMBOMRefPID = createMBOMFromDragAndDrop(context, plmSession, psLeafRefPID, mbomLeafRefPID, newRefPIDListFromSynch, plantPID, approvalStatus, mbomCompletePath, psCompletePath);

                if (UIUtil.isNullOrEmpty(newMBOMRefPID)) {
                    String strSkipStdMBOMRPEValue = PropertyUtil.getRPEValue(context, SKIPSTANDARDMBOM, true);
                    if ("true".equalsIgnoreCase(strSkipStdMBOMRPEValue))
                        return "0";
                }
                DomainRelationship psInstObj = new DomainRelationship(psCompletePathList[psCompletePathList.length - 1]);
                Map psInstAttributes = psInstObj.getAttributeMap(context, true);
                Map mbomInstAttributes = new HashMap();
                String psTreeOrder = (String) psInstAttributes.get("PLMInstance.V_TreeOrder");
                String psVName = (String) psInstAttributes.get("PLMInstance.V_Name");
                String psExternalID = (String) psInstAttributes.get("PLMInstance.PLM_ExternalID");
                mbomInstAttributes.put("PLMInstance.V_TreeOrder", psTreeOrder);
                mbomInstAttributes.put("PLMInstance.V_Name", psVName);
                mbomInstAttributes.put("PLMInstance.PLM_ExternalID", psExternalID);
                // Insert this Provide under it's parent
                mbomLeafInstancePID = getInstanceToReuse(context, mbomLeafRefPID, newMBOMRefPID, psTreeOrder, psExternalID, psVName);
                if (UIUtil.isNullOrEmpty(mbomLeafInstancePID)) {
                    mbomLeafInstancePID = createInstance(context, plmSession, mbomLeafRefPID, newMBOMRefPID);
                }
                // Replicate all the attributes values on the new instance
                DomainRelationship newMBOMInstObj = new DomainRelationship(mbomLeafInstancePID);
                newMBOMInstObj.setAttributeValues(context, mbomInstAttributes);

                mbomCompletePath += "/";
                mbomCompletePath += mbomLeafInstancePID;

                returnValue = "3";
                trimmedPSPath = trimPSPathToClosestScope(context, plmSession, mbomCompletePath, psCompletePath);
                if (trimmedPSPath == null || "".equals(trimmedPSPath)) {
                    throw new Exception("No scope exists.");
                }

            }

            else {

                String mbomLeafRefType = MqlUtil.mqlCommand(context, "print bus " + mbomLeafRefPID + " select type dump |", false, false);

                boolean isDirect = false;
                boolean isIndirect = false;

                for (String typeInList : baseTypesForMBOMLeafNodes) {
                    if (getDerivedTypes(context, typeInList).contains(mbomLeafRefType))
                        isDirect = true;
                }

                if (isDirect) {
                    // Get the synched ManufItem with the lead PS reference (new Provide or the existing leaf ManufItem)
                    newMBOMRefPID = getSynchedScopeMBOMRefFromPSRef(context, plmSession, psLeafRefPID, mbomLeafRefPID, newRefPIDListFromSynch, plantPID);

                    if (!newMBOMRefPID.equals(mbomLeafRefPID)) { // The leaf MBOM reference is not the one synched with the leaf PS reference. Normally because it is a different revision
                        // Replace the MBOM leaf instance with the new one
                        String newMBOMLeafInstancePID = PSS_FRCMBOMModelerUtility_mxJPO.replaceMBOMInstance(context, plmSession, mbomLeafInstancePID, newMBOMRefPID);

                        mbomCompletePath = mbomCompletePath.replace(mbomLeafInstancePID, newMBOMLeafInstancePID);
                        mbomLeafInstancePID = newMBOMLeafInstancePID;

                        returnValue = "2";
                    }
                } else {
                    for (String typeInList : baseTypesForMBOMAssemblyNodes) {
                        if (getDerivedTypes(context, typeInList).contains(mbomLeafRefType))
                            isIndirect = true;
                    }

                    if (isIndirect) {
                        // Get a new synched Provide with the leaf PS reference
                        if (mbomLeafRefPID != null && !"".equals(mbomLeafRefPID)) {
                            DomainObject domParentMBOM = DomainObject.newInstance(context, mbomLeafRefPID);
                            String strParentMBOMPolicy = domParentMBOM.getInfo(context, DomainConstants.SELECT_POLICY);
                            if (TigerConstants.POLICY_PSS_STANDARDMBOM.equalsIgnoreCase(strParentMBOMPolicy))
                                PropertyUtil.setRPEValue(context, STANDARDROOTMBOM, "true", true);
                            else
                                PropertyUtil.setRPEValue(context, STANDARDROOTMBOM, "false", true);
                        }

                        newMBOMRefPID = getSynchedScopeMBOMRefFromPSRef(context, plmSession, psLeafRefPID, null, newRefPIDListFromSynch, plantPID);
                        // TIGTK-10100:Rutuja Ekatpure:Start
                        // if no Standard MBOM object to reuse then return 0;
                        if (UIUtil.isNullOrEmpty(newMBOMRefPID)) {
                            String isSkipStdMBOM = PropertyUtil.getRPEValue(context, SKIPSTANDARDMBOM, true);
                            if ("true".equalsIgnoreCase(isSkipStdMBOM))
                                return "0";
                        }
                        // TIGTK-10100:Rutuja Ekatpure:End
                        DomainRelationship psInstObj = new DomainRelationship(psCompletePathList[psCompletePathList.length - 1]);
                        Map psInstAttributes = psInstObj.getAttributeMap(context, true);
                        Map mbomInstAttributes = new HashMap();
                        String psTreeOrder = (String) psInstAttributes.get("PLMInstance.V_TreeOrder");
                        String psVName = (String) psInstAttributes.get("PLMInstance.V_Name");
                        String psExternalID = (String) psInstAttributes.get("PLMInstance.PLM_ExternalID");
                        mbomInstAttributes.put("PLMInstance.V_TreeOrder", psTreeOrder);
                        mbomInstAttributes.put("PLMInstance.V_Name", psVName);
                        mbomInstAttributes.put("PLMInstance.PLM_ExternalID", psExternalID);

                        // Insert this Provide under it's parent
                        mbomLeafInstancePID = getInstanceToReuse(context, mbomLeafRefPID, newMBOMRefPID, psTreeOrder, psExternalID, psVName);
                        if (UIUtil.isNullOrEmpty(mbomLeafInstancePID)) {
                            mbomLeafInstancePID = createInstance(context, plmSession, mbomLeafRefPID, newMBOMRefPID);
                        }
                        // Replicate all the attributes values on the new instance
                        DomainRelationship newMBOMInstObj = new DomainRelationship(mbomLeafInstancePID);
                        newMBOMInstObj.setAttributeValues(context, mbomInstAttributes);
                        // Rutuja Ekatpure:TIGTK-10100:31/10/2017:start
                        DomainObject domMBOMObj = DomainObject.newInstance(context, newMBOMRefPID);
                        String strMBOMPolicy = domMBOMObj.getInfo(context, DomainConstants.SELECT_POLICY);
                        if (TigerConstants.POLICY_PSS_STANDARDMBOM.equals(strMBOMPolicy)) {
                            StringList slPlantConnected = pss.mbom.MBOMUtil_mxJPO.getConsumerPlantOnStandardMBOM(context, newMBOMRefPID);
                            if (!slPlantConnected.contains(plantPID)) {
                                PropertyUtil.setRPEValue(context, PLANTFROMENOVIA, "true", false);
                                PSS_FRCMBOMModelerUtility_mxJPO.attachPlantToMBOMReference(context, plmSession, (String) newMBOMRefPID, plantPID);
                                String strquery = "query path type SemanticRelation containing " + newMBOMRefPID + " where owner.type=='MfgProductionPlanning' select owner.physicalid dump |";
                                String listPathIds = MqlUtil.mqlCommand(context, strquery, false, false);
                                String[] slPathIds = listPathIds.split("\n");
                                if (UIUtil.isNotNullAndNotEmpty(listPathIds)) {
                                    String strMfgPlanningId = (slPathIds[slPathIds.length - 1]).split("\\|")[1];
                                    PropertyUtil.setGlobalRPEValue(context, MODIFYPLANT, "false");
                                    updateConsumerPlantOnMBOM(context, strMfgPlanningId);
                                }
                            }
                        }
                        // Rutuja Ekatpure:TIGTK-10100:31/10/2017:End
                        mbomCompletePath += "/";
                        mbomCompletePath += mbomLeafInstancePID;

                        returnValue = "1";
                    }
                }

                newRefPIDList.addAll(newRefPIDListFromSynch);

                if (isDirect || isIndirect) {
                    trimmedPSPath = trimPSPathToClosestScope(context, plmSession, mbomCompletePath, psCompletePath);

                    if (trimmedPSPath == null || "".equals(trimmedPSPath)) {
                        throw new Exception("No scope exists.");
                    }
                }
            }
            // Remove any existing implement link
            PSS_FRCMBOMModelerUtility_mxJPO.deleteImplementLink(context, plmSession, mbomLeafInstancePID, true);

            // Put a new implement link
            List<String> mbomLeafInstancePIDList = new ArrayList();
            mbomLeafInstancePIDList.add(mbomLeafInstancePID);
            List<String> trimmedPSPathList = new ArrayList();
            trimmedPSPathList.add(trimmedPSPath);
            String retStr = PSS_FRCMBOMModelerUtility_mxJPO.setImplementLinkBulk(context, plmSession, mbomLeafInstancePIDList, trimmedPSPathList, true);
            if (!"".equals(retStr))
                throw new Exception(retStr);
            // PSS: SART
            if (UIUtil.isNotNullAndNotEmpty(newMBOMRefPID)) {
                PSS_FRCMBOMModelerUtility_mxJPO.setFCSIndexOnMBOM(context, psLeafRefPID, newMBOMRefPID);
                /*
                 * String strNotGenerateVariantAssemmbly = PropertyUtil.getGlobalRPEValue(context, NOTGENERATEVARINTASSEMBLY); if (UIUtil.isNullOrEmpty(strNotGenerateVariantAssemmbly))
                 * disconnectVaraintAssemblyFromMBOMAndCreateNew(context, mbomLeafRefPID, psLeafRefPID); else if (!strNotGenerateVariantAssemmbly.equalsIgnoreCase("True"))
                 * disconnectVaraintAssemblyFromMBOMAndCreateNew(context, mbomLeafRefPID, psLeafRefPID);
                 */
            } else if (UIUtil.isNotNullAndNotEmpty(mbomLeafRefPID)) {
                PSS_FRCMBOMModelerUtility_mxJPO.setFCSIndexOnMBOM(context, psLeafRefPID, mbomLeafRefPID);
                // disconnectVaraintAssemblyFromMBOMAndCreateNew(context, mbomLeafRefPID, psLeafRefPID);
            }
            // PSS: END
            return returnValue;
        } catch (Exception exp) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in setImplementLinkProcess: ", exp);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw exp;
        } finally {
            PropertyUtil.setGlobalRPEValue(context, NOTGENERATEVARINTASSEMBLY, DomainConstants.EMPTY_STRING);
        }

    }

    /**
     * @PolicyChange
     * @param context
     * @param plmSession
     * @param rootMBOMRefPID
     * @param changeObject
     * @return
     * @throws Exception
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static String duplicateMBOMStructure(Context context, PLMCoreModelerSession plmSession, String rootMBOMRefPID, String changeObject) throws Exception {
        // Get all the reference PIDs in the structure to duplicate : expand all levels, no config filter, no bus or rel selects (we only want the ref physicalid)
        MapList resExpand = PSS_FRCMBOMModelerUtility_mxJPO.getVPMStructure(context, plmSession, rootMBOMRefPID, new StringList("physicalid"), new StringList(), (short) 0, null, null);

        // IPLMxCoreAccess coreAccess = plmSession.getVPLMAccess();
        // IVPLMProductionSystemAuthoring modeler = (IVPLMProductionSystemAuthoring)
        // plmSession.getModeler("com.dassault_systemes.vplm.ProductionSystemAuthoring.implementation.VPLMProductionSystemAuthoring");
        HashMap listCloneRefID = new HashMap();
        HashMap listCloneInstID = new HashMap();
        String refID;

        String instID;
        for (Object levelObj : resExpand) {
            Map levelMap = (Map) levelObj;
            refID = (String) levelMap.get("physicalid");
            if (refID != null) {
                listCloneRefID.put(refID, refID);
            }
            instID = (String) levelMap.get("physicalid[connection]");
            if (instID != null) {
                listCloneInstID.put(instID, instID);
            }
        }

        List<String> refAndInstPIList = new ArrayList<String>();
        refAndInstPIList.add(rootMBOMRefPID);
        List<String> refPIDList = new ArrayList<String>(listCloneRefID.values());
        List<String> instPIDList = new ArrayList<String>(listCloneInstID.values());
        refAndInstPIList.addAll(refPIDList);
        refAndInstPIList.addAll(instPIDList);

        List<String> newRefPIDList = PSS_FRCMBOMModelerUtility_mxJPO.partialDuplicateMBOMStructure(context, plmSession, TigerConstants.POLICY_PSS_MBOM, refAndInstPIList);

        // Attach all duplicated reference to change object.
        attachListObjectsToChange(context, plmSession, changeObject, newRefPIDList);

        return newRefPIDList.get(0); // Return the PID of the new root ref
    }

    /**
     * @UniqueMBOMPerPlant
     * @PolicyChange
     * @param context
     * @param plmSession
     * @param rootMBOMRefPID
     * @param changeObject
     * @return
     * @throws Exception
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static String duplicateMBOMStructure(Context context, PLMCoreModelerSession plmSession, String rootMBOMRefPID, String changeObject, StringList psRefPIDList, String plantId)
            throws Exception {
        // Get all the reference PIDs in the structure to duplicate : expand all levels, no config filter, no bus or rel selects (we only want the ref physicalid)
        MapList resExpand = PSS_FRCMBOMModelerUtility_mxJPO.getVPMStructure(context, plmSession, rootMBOMRefPID, new StringList("physicalid"), new StringList(), (short) 0, null, null);

        // IPLMxCoreAccess coreAccess = plmSession.getVPLMAccess();
        // LIVPLMProductionSystemAuthoring modeler = (IVPLMProductionSystemAuthoring)
        // plmSession.getModeler("com.dassault_systemes.vplm.ProductionSystemAuthoring.implementation.VPLMProductionSystemAuthoring");
        HashMap listCloneRefID = new HashMap();
        HashMap listCloneInstID = new HashMap();
        String refID;
        StringList slInstanceList = new StringList();
        String instID;
        for (Object levelObj : resExpand) {
            Map levelMap = (Map) levelObj;
            refID = (String) levelMap.get("physicalid");
            if (refID != null) {
                listCloneRefID.put(refID, refID);
            }
            instID = (String) levelMap.get("physicalid[connection]");
            slInstanceList.addElement(instID);
            if (instID != null) {
                listCloneInstID.put(instID, instID);
            }
        }

        List<String> refAndInstPIList = new ArrayList<String>();
        refAndInstPIList.add(rootMBOMRefPID);
        List<String> refPIDList = new ArrayList<String>(listCloneRefID.values());
        List<String> instPIDList = new ArrayList<String>(listCloneInstID.values());
        refAndInstPIList.addAll(refPIDList);
        refAndInstPIList.addAll(instPIDList);

        List<String> newRefPIDList = PSS_FRCMBOMModelerUtility_mxJPO.partialDuplicateMBOMStructure(context, plmSession, TigerConstants.POLICY_PSS_MBOM, refAndInstPIList, psRefPIDList, plantId);

        // Attach all duplicated reference to change object.
        attachListObjectsToChange(context, plmSession, changeObject, newRefPIDList);

        return newRefPIDList.get(0); // Return the PID of the new root ref
    }

    /**
     * This method is used to get Resource Objects
     * @name getToolingObjects
     * @param context
     *            the Matrix Context
     * @param args
     * @returns StringList
     * @throws Exception
     *             if the operation fails
     */
    @SuppressWarnings("rawtypes")
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getToolingObjects(Context context, String[] args) throws Exception {

        // final String TYPE_VPMREFERENCE = PropertyUtil.getSchemaProperty(context, "type_VPMReference");

        final String RELATIONSHIP_PSS_RELATEDRESOURCE = PropertyUtil.getSchemaProperty(context, "relationship_PSS_RelatedResource");
        String strErrorMessage = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.ErrorMessage");

        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String parentOID = (String) programMap.get("parentId");
        String strObjectId = (String) programMap.get("objectId");
        MapList slToolingObjectList = new MapList();

        try {
            StringList slObjSelectStmts = new StringList();
            slObjSelectStmts.addElement(DomainConstants.SELECT_ID);
            slObjSelectStmts.addElement(DomainConstants.SELECT_NAME);
            slObjSelectStmts.addElement(DomainConstants.SELECT_TYPE);
            slObjSelectStmts.addElement(DomainConstants.SELECT_CURRENT);

            StringList slRelSelectStmts = new StringList(1);
            slRelSelectStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

            String relationshipWhere = "";
            if (UIUtil.isNotNullAndNotEmpty(parentOID)) {
                if (parentOID.contains(".")) {
                    DomainObject dObj = DomainObject.newInstance(context, parentOID);
                    parentOID = dObj.getInfo(context, "physicalid");
                }
                relationshipWhere = "attribute[PSS_RelatedResource.PSS_MBOMContext]==" + parentOID;
            }
            if (UIUtil.isNotNullAndNotEmpty(strObjectId)) {
                DomainObject domToolingObject = DomainObject.newInstance(context, strObjectId);
                slToolingObjectList = domToolingObject.getRelatedObjects(context, RELATIONSHIP_PSS_RELATEDRESOURCE, // Relationship
                        // Pattern
                        TigerConstants.TYPE_VPMREFERENCE, // Object Pattern
                        slObjSelectStmts, // Object Selects
                        slRelSelectStmts, // Relationship Selects
                        false, // to direction
                        true, // from direction
                        (short) 0, // recursion level
                        null, // object where clause
                        relationshipWhere, (short) 0, false, // checkHidden
                        true, // preventDuplicates
                        (short) 1000, // pageSize
                        null, // Post Type Pattern
                        null, null, null);

            } else {
                String error = strErrorMessage;
                MqlUtil.mqlCommand(context, "notice $1", error);
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getToolingObjects: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
        return slToolingObjectList;
    }

    /**
     * This method is used to Display Equipment Report of MBOM Assembly Modified for TIGTK-3592
     * @name getEquipmentReportOfSelectedMBOMItems
     * @param context
     *            the Matrix Context
     * @param args
     * @returns StringList
     * @throws Exception
     *             if the operation fails
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public MapList getEquipmentReportOfSelectedMBOMItems(Context context, String[] args) throws Exception {
        MapList mEquipmentList = new MapList();
        StringList strTempMBOMList = new StringList();
        PLMCoreModelerSession plmSession = null;
        String strMBOMObjectId = "";
        int expLvl = 0;
        String filterExpr = null;
        String filterValue = null;
        String filterInput = null;
        try {
            Map strEquipmentList = new HashMap();
            ContextUtil.startTransaction(context, false);
            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();
            Map programMap = JPO.unpackArgs(args);
            String emxParentIds = (String) programMap.get("emxParentIds");
            StringList strObjectList = FrameworkUtil.split(emxParentIds, "~");

            if (strObjectList.size() > 1) {
                for (int i = 0; i < strObjectList.size(); i++) {
                    StringList strMBOMList = FrameworkUtil.split(((String) strObjectList.get(i)), "|");
                    strMBOMObjectId = (String) strMBOMList.get(1);
                    if (UIUtil.isNullOrEmpty(strMBOMObjectId)) {
                        strMBOMObjectId = (String) strMBOMList.get(0);
                    }

                    List lEquipmentList = PSS_FRCMBOMModelerUtility_mxJPO.getResourcesAttachedToMBOMReference(context, plmSession, strMBOMObjectId);

                    if (!(lEquipmentList.isEmpty())) {
                        for (int j = 0; j < lEquipmentList.size(); j++) {
                            if (isEquipment(context, (String) lEquipmentList.get(j))) {
                                Map mTempMap = new HashMap();
                                StringList strConnectedMBOMList = new StringList();
                                strConnectedMBOMList.add(strMBOMObjectId);
                                if (!strEquipmentList.containsKey(lEquipmentList.get(j))) {
                                    strEquipmentList.put(lEquipmentList.get(j), strConnectedMBOMList);
                                    mTempMap.put(DomainConstants.SELECT_ID, lEquipmentList.get(j));
                                    mTempMap.put("linkedItems", strConnectedMBOMList);

                                    mEquipmentList.add(mTempMap);
                                } else {
                                    StringList strConnectedMBOMListExisting = (StringList) strEquipmentList.get(lEquipmentList.get(j));
                                    strConnectedMBOMListExisting.add(strMBOMObjectId);
                                    strEquipmentList.put(lEquipmentList.get(j), strConnectedMBOMListExisting);
                                }
                            }
                        }
                    }
                }
            } else {
                StringList strMBOMList = FrameworkUtil.split(((String) strObjectList.get(0)), "|");
                strMBOMObjectId = (String) strMBOMList.get(1);
                if (UIUtil.isNullOrEmpty(strMBOMObjectId)) {
                    strMBOMObjectId = (String) strMBOMList.get(0);
                }

                MapList allMBOMItems = FRCMBOMProg_mxJPO.getExpandMBOM(context, strMBOMObjectId, expLvl, filterExpr, filterValue, filterInput, EXPD_REL_SELECT, EXPD_BUS_SELECT);

                strTempMBOMList.add(strMBOMObjectId);
                for (int i = 0; i < allMBOMItems.size(); i++) {
                    Map mMBOMMap = (Map) allMBOMItems.get(i);
                    strMBOMObjectId = (String) mMBOMMap.get("physicalid");
                    strTempMBOMList.add(strMBOMObjectId);
                }

                for (int j = 0; j < strTempMBOMList.size(); j++) {
                    List lEquipmentList = PSS_FRCMBOMModelerUtility_mxJPO.getResourcesAttachedToMBOMReference(context, plmSession, (String) strTempMBOMList.get(j));

                    if (!(lEquipmentList.isEmpty())) {
                        for (int k = 0; k < lEquipmentList.size(); k++) {
                            if (isEquipment(context, (String) lEquipmentList.get(k))) {
                                Map mTempMap = new HashMap();
                                StringList strConnectedMBOMList = new StringList();
                                strConnectedMBOMList.add((String) strTempMBOMList.get(j));

                                if (!strEquipmentList.containsKey(lEquipmentList.get(k))) {
                                    strEquipmentList.put(lEquipmentList.get(k), strConnectedMBOMList);
                                    mTempMap.put(DomainConstants.SELECT_ID, lEquipmentList.get(k));
                                    mTempMap.put("linkedItems", strConnectedMBOMList);

                                    mEquipmentList.add(mTempMap);
                                } else {
                                    StringList strConnectedMBOMListExisting = (StringList) strEquipmentList.get(lEquipmentList.get(k));
                                    strConnectedMBOMListExisting.add((String) strTempMBOMList.get(j));
                                    strEquipmentList.put(lEquipmentList.get(k), strConnectedMBOMListExisting);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getEquipmentReportOfSelectedMBOMItems: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        } finally {
            closeSession(plmSession);
        }

        return mEquipmentList;

    }

    /**
     * TIGTK-3592
     * @param context
     * @param objectId
     * @return
     * @throws Exception
     */
    public static boolean isEquipment(Context context, String objectId) throws Exception {
        // DomainObject dObj = DomainObject.newInstance(context, (String) objectId);
        BusinessObject busObj = new BusinessObject(objectId);
        BusinessInterfaceList interfaceList = busObj.getBusinessInterfaces(context, true);
        boolean isEquipment = false;
        for (int k = 0; k < interfaceList.size(); k++) {
            BusinessInterface bInterface = interfaceList.getElement(k);
            if (TigerConstants.INTERFACE_PSS_EQUIPMENT.equals(bInterface.getName())) {
                isEquipment = true;
                break;
            }
        }
        return isEquipment;
    }

    /**
     * This method is used to Display Tooling Report of MBOM Assembly Modified for TIGTK-3592
     * @name getToolingReportOfSelectedMBOMItems
     * @param context
     *            the Matrix Context
     * @param args
     * @returns StringList
     * @throws Exception
     *             if the operation fails
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public MapList getToolingReportOfSelectedMBOMItems(Context context, String[] args) throws Exception {

        MapList mToolingList = new MapList();
        StringList strTempMBOMList = new StringList();
        PLMCoreModelerSession plmSession = null;
        String strMBOMObjectId = "";
        int expLvl = 0;
        String filterExpr = null;
        String filterValue = null;
        String filterInput = null;
        try {
            Map strToolingtMap = new HashMap();
            ContextUtil.startTransaction(context, false);
            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();
            Map programMap = JPO.unpackArgs(args);
            String emxParentIds = (String) programMap.get("emxParentIds");
            StringList strObjectList = FrameworkUtil.split(emxParentIds, "~");

            if (strObjectList.size() > 1) {
                for (int i = 0; i < strObjectList.size(); i++) {
                    StringList strMBOMList = FrameworkUtil.split(((String) strObjectList.get(i)), "|");
                    strMBOMObjectId = (String) strMBOMList.get(1);

                    if (UIUtil.isNullOrEmpty(strMBOMObjectId)) {
                        strMBOMObjectId = (String) strMBOMList.get(0);
                    }
                    List lToolingList = PSS_FRCMBOMModelerUtility_mxJPO.getResourcesAttachedToMBOMReference(context, plmSession, strMBOMObjectId);

                    if (!(lToolingList.isEmpty())) {
                        for (int j = 0; j < lToolingList.size(); j++) {
                            if (isTooling(context, (String) lToolingList.get(j))) {
                                Map mTempMap = new HashMap();
                                StringList strConnectedMBOMList = new StringList();
                                strConnectedMBOMList.add(strMBOMObjectId);

                                if (!strToolingtMap.containsKey(lToolingList.get(j))) {
                                    strToolingtMap.put(lToolingList.get(j), strConnectedMBOMList);
                                    mTempMap.put(DomainConstants.SELECT_ID, lToolingList.get(j));
                                    mTempMap.put("linkedItems", strConnectedMBOMList);

                                    mToolingList.add(mTempMap);
                                } else {
                                    StringList strConnectedMBOMListExisting = (StringList) strToolingtMap.get(lToolingList.get(j));
                                    strConnectedMBOMListExisting.add(strMBOMObjectId);
                                    strToolingtMap.put(lToolingList.get(j), strConnectedMBOMListExisting);
                                }
                            }
                        }
                    }
                }
            } else {
                StringList strMBOMList = FrameworkUtil.split(((String) strObjectList.get(0)), "|");
                strMBOMObjectId = (String) strMBOMList.get(1);
                if (UIUtil.isNullOrEmpty(strMBOMObjectId)) {
                    strMBOMObjectId = (String) strMBOMList.get(0);
                }

                MapList allMBOMItems = FRCMBOMProg_mxJPO.getExpandMBOM(context, strMBOMObjectId, expLvl, filterExpr, filterValue, filterInput, EXPD_REL_SELECT, EXPD_BUS_SELECT);

                strTempMBOMList.add(strMBOMObjectId);
                for (int i = 0; i < allMBOMItems.size(); i++) {
                    Map mMBOMMap = (Map) allMBOMItems.get(i);
                    strMBOMObjectId = (String) mMBOMMap.get("physicalid");
                    strTempMBOMList.add(strMBOMObjectId);
                }

                for (int j = 0; j < strTempMBOMList.size(); j++) {
                    List lToolingList = PSS_FRCMBOMModelerUtility_mxJPO.getResourcesAttachedToMBOMReference(context, plmSession, (String) strTempMBOMList.get(j));

                    if (!(lToolingList.isEmpty())) {
                        for (int k = 0; k < lToolingList.size(); k++) {
                            if (isTooling(context, (String) lToolingList.get(k))) {
                                Map mTempMap = new HashMap();
                                StringList strConnectedMBOMList = new StringList();
                                strConnectedMBOMList.add((String) strTempMBOMList.get(j));

                                if (!strToolingtMap.containsKey(lToolingList.get(k))) {
                                    strToolingtMap.put(lToolingList.get(k), strConnectedMBOMList);
                                    mTempMap.put(DomainConstants.SELECT_ID, lToolingList.get(k));
                                    mTempMap.put("linkedItems", strConnectedMBOMList);

                                    mToolingList.add(mTempMap);

                                } else {
                                    StringList strConnectedMBOMListExisting = (StringList) strToolingtMap.get(lToolingList.get(k));
                                    strConnectedMBOMListExisting.add((String) strTempMBOMList.get(j));
                                    strToolingtMap.put(lToolingList.get(k), strConnectedMBOMListExisting);
                                }
                            }

                        }
                    }
                }
            }

        }

        catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getToolingReportOfSelectedMBOMItems: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        } finally {
            closeSession(plmSession);
        }

        return mToolingList;

    }

    /**
     * TIGTK-3592
     * @param context
     * @param objectId
     * @return
     * @throws Exception
     */
    public static boolean isTooling(Context context, String objectId) throws Exception {
        // DomainObject dObj = DomainObject.newInstance(context, (String) objectId);
        BusinessObject busObj = new BusinessObject(objectId);
        // BusinessInterfaceList interfaceList = dObj.getBusinessInterfaces(context, true);
        BusinessInterfaceList interfaceList = busObj.getBusinessInterfaces(context, true);
        boolean isTooling = false;
        for (int k = 0; k < interfaceList.size(); k++) {
            BusinessInterface bInterface = interfaceList.getElement(k);
            if (TigerConstants.INTERFACE_PSS_TOOLING.equals(bInterface.getName())) {
                isTooling = true;
                break;
            }
        }
        return isTooling;
    }

    /**
     * This method is used to display The Resource name
     * @name getResourceName
     * @param context
     *            the Matrix Context
     * @param args
     * @returns StringList
     * @throws Exception
     *             if the operation fails
     */
    @SuppressWarnings("rawtypes")
    public StringList getResourceName(Context context, String[] args) throws Exception {
        String strResourceName = "";
        StringList strResult = new StringList();

        try {
            Map ProgramMap = JPO.unpackArgs(args);
            MapList objectList = (MapList) ProgramMap.get("objectList");
            for (int i = 0; i < objectList.size(); i++) {
                Map mObjectMap = (Map) objectList.get(i);
                String strResourceId = (String) mObjectMap.get(DomainConstants.SELECT_ID);
                DomainObject domResourceObject = new DomainObject(strResourceId);
                strResourceName = domResourceObject.getInfo(context, DomainConstants.SELECT_NAME);
                strResult.add(strResourceName);

            }

        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getResourceName: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
        return strResult;

    }

    /**
     * This method is used to display expanded structure MBOM including Harmony and its information
     * @param context
     * @param args
     * @return maplist containing the Harmonies and MBOM
     * @throws Exception
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public MapList getExpandMBOMForHarmonyAssociation(Context context, String[] args) throws Exception {
        final StringList EXPD_BUS_SELECT_custom = new StringList(new String[] { "type", "physicalid", "logicalid", "id" });

        String CUSTOMER_PART_NUMBER = "PSS_CustomerPartNumberList";
        String UNIT_OF_MEASURE_CATEGORY = "PSS_UnitOfMeasureCategory";
        String UNIT_OF_MEASURE = "PSS_UnitOfMeasure";

        String strNoHarmonyName = "NoHarmony";
        // TIGTK-9215:START
        String strNetWeight = DomainConstants.EMPTY_STRING;
        String strGrossWeight = DomainConstants.EMPTY_STRING;
        // TIGTK-9215:END
        StringList slMBOMType = new StringList();
        slMBOMType.add(TigerConstants.TYPE_CREATEASSEMBLY);
        slMBOMType.add(TigerConstants.TYPE_CREATEKIT);
        slMBOMType.add(TigerConstants.TYPE_CREATEMATERIAL);

        StringList slBusSelect = new StringList(DomainConstants.SELECT_ID);
        slBusSelect.add("physicalid");

        boolean transactionActive = false;
        PLMCoreModelerSession plmSession = null;
        MapList finalMBOMList = new MapList();

        try {
            Map programMap = JPO.unpackArgs(args);
            String strMBOMObjectId = (String) programMap.get("objectId");
            String strRelationId = (String) programMap.get("relId");
            String strParentId = (String) programMap.get("parentOID");
            String strPCId = (String) programMap.get("FRCExpressionFilterInput_OID");

            String pcID = DomainConstants.EMPTY_STRING;
            if (UIUtil.isNotNullAndNotEmpty(strPCId) && !strPCId.equalsIgnoreCase("undefined")) {
                pcID = MqlUtil.mqlCommand(context, "print bus " + strPCId + " select physicalid dump |", false, false);
            }

            MapList mlExpandedMBOM = getExpandMBOMonPC(context, strMBOMObjectId, 0, pcID, EXPD_REL_SELECT, EXPD_BUS_SELECT_custom);
            mlExpandedMBOM.sortStructure("attribute[PLMInstance.V_TreeOrder].value", "ascending", "real");
            ContextUtil.startTransaction(context, true);
            transactionActive = true;
            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();
            StringList slPlantAttatchedtoRoot = new StringList();
            String strMasterPlantConnectedToRootObject = pss.mbom.MBOMUtil_mxJPO.getMasterPlant(context, strMBOMObjectId);
            // TIGTK-10265:Rutuja Ekatpure:29/11/2017:Start
            // This code added for Manual expand on child ,in this case if plant differ then we don't want to expand
            boolean isToExpandChalid = true;
            boolean isExpandChalid = false;
            if (UIUtil.isNotNullAndNotEmpty(strRelationId)) {
                isExpandChalid = true;
                String strParentOfSelectedMBOMId = MqlUtil.mqlCommand(context, "print connection " + strRelationId + " select from.physicalid dump |", false, false);
                String strMasterPlantonParentOfSelectedObject = pss.mbom.MBOMUtil_mxJPO.getMasterPlant(context, strParentOfSelectedMBOMId);
                if (!strMasterPlantonParentOfSelectedObject.equalsIgnoreCase(strMasterPlantConnectedToRootObject)) {
                    isToExpandChalid = false;
                }
            }
            // TIGTK-10265:Rutuja Ekatpure:29/11/2017:End
            if (UIUtil.isNotNullAndNotEmpty(strMasterPlantConnectedToRootObject)) {
                slPlantAttatchedtoRoot.add(strMasterPlantConnectedToRootObject);
            }

            if (UIUtil.isNotNullAndNotEmpty(strMBOMObjectId)) {
                DomainObject domMBOM = new DomainObject(strMBOMObjectId);
                if (mlExpandedMBOM != null && !(mlExpandedMBOM.isEmpty()) && isToExpandChalid) {
                    int skipedLevel = 0;
                    for (int i = 0; i < mlExpandedMBOM.size(); i++) {
                        Map mStructureMap = (Map) mlExpandedMBOM.get(i);

                        String strMasterPlantConnectedToChildObject = pss.mbom.MBOMUtil_mxJPO.getMasterPlant(context, (String) mStructureMap.get("physicalid"));
                        String level = (String) mStructureMap.get(DomainConstants.SELECT_LEVEL);
                        int levelInt = Integer.parseInt(level);
                        String strType = (String) mStructureMap.get(DomainConstants.SELECT_TYPE);

                        if (skipedLevel != 0 && skipedLevel < levelInt) {
                            continue;
                        } else if (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_OPERATION) || strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_LINEDATA)) {
                            skipedLevel = levelInt;
                        } else if (!slPlantAttatchedtoRoot.contains(strMasterPlantConnectedToChildObject)) {

                            if (TigerConstants.LIST_TYPE_MATERIALS.contains(strType)) {
                                skipedLevel = 0;
                                finalMBOMList.add(mStructureMap);
                            } else {
                                skipedLevel = levelInt;
                                finalMBOMList.add(mStructureMap);
                            }
                        } else {
                            skipedLevel = 0;
                            finalMBOMList.add(mStructureMap);
                        }
                    }

                }
                SelectList slSelectStmts = new SelectList();
                slSelectStmts.addElement(DomainConstants.SELECT_ID);
                slSelectStmts.addElement(DomainConstants.SELECT_TYPE);
                slSelectStmts.addElement(DomainConstants.SELECT_NAME);

                Query query = new Query();
                query.open(context);
                query.setBusinessObjectType(TigerConstants.TYPE_PSS_HARMONY);
                query.setBusinessObjectName(strNoHarmonyName);
                query.setBusinessObjectRevision("Default");
                query.setVaultPattern(TigerConstants.VAULT_ESERVICEPRODUCTION);
                query.setExpandType(false);
                QueryIterator queryIterator = query.getIterator(context, slSelectStmts, (short) 100);
                query.close(context);
                MapList mlNoHarmony = FrameworkUtil.toMapList(queryIterator, FrameworkUtil.MULTI_VALUE_LIST);
                queryIterator.close();

                Map mHarmonyMapNoHarmony = null;
                // TIGTK-10265:Rutuja Ekatpure:29/11/2017:Start
                String strWhere = "";
                if (UIUtil.isNotNullAndNotEmpty(strPCId) && !strPCId.equalsIgnoreCase("undefined")) {
                    strWhere = "to[" + TigerConstants.RELATIONSHIP_PSS_PCASSOCIATEDTOHARMONY + "].from.id==" + strPCId;
                }
                // This is added for getting harmony when you manualy expand on child level where you get child id instead of root on which PSS_MBOM_Harmony relationship exist
                String strRootObjectId = strMBOMObjectId;
                if (isExpandChalid) {
                    strRootObjectId = strParentId;
                }

                DomainObject domParentMBOM = new DomainObject(strRootObjectId);
                MapList mlHarmonyList = domParentMBOM.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSSMBOM_HARMONIES, // relationship pattern
                        TigerConstants.TYPE_PSS_HARMONY, // object pattern
                        new StringList(DomainConstants.SELECT_ID), // object selects
                        null, // relationship selects
                        false, // to direction
                        true, // from direction
                        (short) 1, // recursion level
                        strWhere, // object where clause
                        null, (short) 0, false, // checkHidden
                        true, // preventDuplicates
                        (short) 1000, // pageSize
                        null, // Postpattern
                        null, null, null);

                // TIGTK-10265:Rutuja Ekatpure:29/11/2017:End
                if (mlNoHarmony != null && !(mlNoHarmony.isEmpty())) {
                    mHarmonyMapNoHarmony = (Map) mlNoHarmony.get(0);
                    mlHarmonyList.add(0, mHarmonyMapNoHarmony);
                }
                // TIGTK-6667 :Rutuja Ekatpure:"finalMBOMList" list check is removed b'cos of which harmony association window does not show anything for root element in 1 level structure:17/4/2017
                if (mlHarmonyList != null && !(mlHarmonyList.isEmpty())) {
                    int count = 0;
                    MapList tempMapList = (MapList) finalMBOMList.clone();

                    String varintAssemblyPID = pss.mbom.MBOMUtil_mxJPO.getVariantAssemblyPIDForMBOM(context, strMBOMObjectId, pcID);

                    StringList slAttributeSelects = new StringList();

                    slAttributeSelects.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_UNIT_OF_MEASURE_CATEGORY + "]");
                    slAttributeSelects.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_UNIT_OF_MEASURE + "]");
                    slAttributeSelects.add(DomainConstants.SELECT_TYPE);
                    slAttributeSelects.add(DomainConstants.SELECT_NAME);
                    slAttributeSelects.add(TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXTPSS_GROSSWEIGHT);
                    slAttributeSelects.add(TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXTPSS_NETWEIGHT);
                    slAttributeSelects.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXT_PSS_CUSTOMER_PART_NUMBER + "].value");
                    slAttributeSelects.addElement("from[" + TigerConstants.RELATIONSHIP_PSS_COLORLIST + "].to.physicalid");
                    DomainObject.MULTI_VALUE_LIST.add("from[" + TigerConstants.RELATIONSHIP_PSS_COLORLIST + "].to.physicalid");
                    DomainObject.MULTI_VALUE_LIST.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXT_PSS_CUSTOMER_PART_NUMBER + "].value");

                    Map mpAttributeInfo = domMBOM.getInfo(context, slAttributeSelects);

                    String strUnitofMeasureCategory = (String) mpAttributeInfo.get(TigerConstants.ATTRIBUTE_PSS_UNIT_OF_MEASURE_CATEGORY);
                    String strUnitofMeasure = (String) mpAttributeInfo.get(TigerConstants.ATTRIBUTE_PSS_UNIT_OF_MEASURE);
                    String topType = (String) mpAttributeInfo.get(DomainConstants.SELECT_TYPE);

                    DomainObject.MULTI_VALUE_LIST.remove("from[" + TigerConstants.RELATIONSHIP_PSS_COLORLIST + "].to.physicalid");
                    DomainObject.MULTI_VALUE_LIST.remove("attribute[" + TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXT_PSS_CUSTOMER_PART_NUMBER + "].value");
                    StringList slColorList = (StringList) (mpAttributeInfo.get("from[" + TigerConstants.RELATIONSHIP_PSS_COLORLIST + "].to.physicalid"));
                    StringList strCustomerPartNumber = (StringList) (mpAttributeInfo.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXT_PSS_CUSTOMER_PART_NUMBER + "].value"));

                    for (int j = 0; j < mlHarmonyList.size(); j++) {
                        Map mHarmonyMap = (Map) mlHarmonyList.get(j);
                        String strHarmonyId = (String) mHarmonyMap.get(DomainConstants.SELECT_ID);
                        DomainObject domHarmony = new DomainObject(strHarmonyId);

                        String associationRelId = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", strHarmonyId, "to[" + TigerConstants.RELATIONSHIP_PSS_HARMONYASSOCIATION
                                + "|from.physicalid=='" + strMBOMObjectId + "' && attribute[" + TigerConstants.ATTRIBUTE_PSS_PRODUCT_CONFIGURATION_PID + "]=='" + pcID + "'].id");

                        if (UIUtil.isNullOrEmpty(associationRelId.trim())) {
                            // Connect Harmony Association
                            DomainRelationship harmonyAssociationRel = DomainRelationship.connect(context, domMBOM, TigerConstants.RELATIONSHIP_PSS_HARMONYASSOCIATION, domHarmony);
                            // TIGTK-9215:START
                            strGrossWeight = (String) mpAttributeInfo.get(TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXTPSS_GROSSWEIGHT);
                            strNetWeight = (String) mpAttributeInfo.get(TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXTPSS_NETWEIGHT);
                            // TIGTK-9215:END
                            associationRelId = harmonyAssociationRel.toString();
                            Map mlAttributeMap = new HashMap();
                            mlAttributeMap.put(TigerConstants.ATTRIBUTE_PSS_PRODUCT_CONFIGURATION_PID, pcID);
                            mlAttributeMap.put(TigerConstants.ATTRIBUTE_PSS_VARIANTASSEMBLY_PID, varintAssemblyPID);
                            mlAttributeMap.put(TigerConstants.ATTRIBUTE_PSS_GROSSWEIGHT, strGrossWeight);
                            mlAttributeMap.put(TigerConstants.ATTRIBUTE_PSS_NETWEIGHT, strNetWeight);
                            harmonyAssociationRel.setAttributeValues(context, mlAttributeMap);
                        }
                        if (UIUtil.isNotNullAndNotEmpty(associationRelId.trim())) {
                            // TIGTK-10503:Start
                            String strColorable = getColorableValue(context, strMBOMObjectId, plmSession);
                            // TIGTK-10503:End
                            Map tempMapforHarmony = new HashMap();
                            tempMapforHarmony.put(DomainConstants.SELECT_TYPE, (String) mHarmonyMap.get(DomainConstants.SELECT_TYPE));
                            tempMapforHarmony.put(DomainConstants.SELECT_RELATIONSHIP_ID, associationRelId);
                            tempMapforHarmony.put(DomainConstants.SELECT_ID, (String) mHarmonyMap.get(DomainConstants.SELECT_ID));
                            tempMapforHarmony.put(DomainConstants.SELECT_LEVEL, "1");
                            tempMapforHarmony.put(TigerConstants.RELATIONSHIP_PSS_COLORLIST, slColorList);
                            tempMapforHarmony.put(CUSTOMER_PART_NUMBER, strCustomerPartNumber);
                            tempMapforHarmony.put(UNIT_OF_MEASURE_CATEGORY, strUnitofMeasureCategory);
                            tempMapforHarmony.put(UNIT_OF_MEASURE, strUnitofMeasure);
                            tempMapforHarmony.put(TigerConstants.ATTRIBUTE_PSS_MANUFACTURING_INSTANCEEXT_PSS_PHANTOM_LEVEL, "N/A");
                            tempMapforHarmony.put(TigerConstants.ATTRIBUTE_PSS_MANUFACTURING_INSTANCEEXT_PSS_PHANTOM_PART, "N/A");
                            tempMapforHarmony.put("ParentType", topType);
                            // TIGTK-10503:Start
                            tempMapforHarmony.put("Colorable", strColorable);
                            // TIGTK-10503:End
                            finalMBOMList.add(count, tempMapforHarmony);
                            // ALM-4190 : START
                            DomainRelationship harmonyAssociationRel = DomainRelationship.newInstance(context, associationRelId);
                            // TIGTK-10265:RE:27/11/17:Start
                            String strCommonAssociationRelId = "";
                            if (UIUtil.isNullOrEmpty(pcID)) {
                                strCommonAssociationRelId = associationRelId;
                            } else {
                                strCommonAssociationRelId = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", strHarmonyId, "to[" + TigerConstants.RELATIONSHIP_PSS_HARMONYASSOCIATION
                                        + "|from.physicalid=='" + strMBOMObjectId + "' && attribute[" + TigerConstants.ATTRIBUTE_PSS_PRODUCT_CONFIGURATION_PID + "]==''].id");

                            }

                            if (UIUtil.isNotNullAndNotEmpty(strCommonAssociationRelId) && UIUtil.isNullOrEmpty(varintAssemblyPID)) {
                                DomainRelationship commonHarmonyAssociationRel = DomainRelationship.newInstance(context, strCommonAssociationRelId);
                                String Quantity = commonHarmonyAssociationRel.getAttributeValue(context, TigerConstants.ATTRIBUTE_QUANTITY);
                                harmonyAssociationRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_QUANTITY, Quantity);
                            }
                            // TIGTK-10265:RE:27/11/17:End
                            Map mlAttributeMapForRel1 = new HashMap();
                            mlAttributeMapForRel1.put(TigerConstants.ATTRIBUTE_PSS_PRODUCT_CONFIGURATION_PID, pcID);
                            mlAttributeMapForRel1.put(TigerConstants.ATTRIBUTE_PSS_VARIANTASSEMBLY_PID, varintAssemblyPID);
                            harmonyAssociationRel.setAttributeValues(context, mlAttributeMapForRel1);
                            // ALM-4190 : END
                            count++;
                        }
                    }

                    count++;

                    for (int i = 0; i < tempMapList.size(); i++) {
                        Map mStructureMap = (Map) tempMapList.get(i);
                        String level = (String) mStructureMap.get(DomainConstants.SELECT_LEVEL);
                        // TIGTK-10265:RE:27/11/17:Start
                        String strConnectionId = (String) mStructureMap.get("id[connection]");
                        // TIGTK-10265:RE:27/11/17:End
                        // TIGTK-9215:START
                        String strType = (String) mStructureMap.get(DomainConstants.SELECT_TYPE);
                        // TIGTK-9215:END
                        int levelInt = Integer.parseInt(level);
                        String strMBOMId = (String) mStructureMap.get(DomainConstants.SELECT_ID);
                        String strRelId = (String) mStructureMap.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                        String strPhantomLevel = "";
                        String strPhantomPart = "";

                        if (UIUtil.isNotNullAndNotEmpty(strRelId)) {
                            DomainRelationship domRelationship = new DomainRelationship(strRelId);
                            strPhantomLevel = domRelationship.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURING_INSTANCEEXT_PSS_PHANTOM_LEVEL);
                            strPhantomPart = domRelationship.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURING_INSTANCEEXT_PSS_PHANTOM_PART);
                            strRelId = MqlUtil.mqlCommand(context, "print connection $1 select $2 dump", strRelId, DomainConstants.SELECT_ID);
                        }

                        DomainObject domMBOMObjectFromMap = new DomainObject(strMBOMId);
                        StringList slSelectsChild = new StringList();

                        slSelectsChild.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_UNIT_OF_MEASURE_CATEGORY + "]");
                        slSelectsChild.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_UNIT_OF_MEASURE + "]");

                        slSelectsChild.add(TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXTPSS_GROSSWEIGHT);
                        slSelectsChild.add(TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXTPSS_NETWEIGHT);
                        slSelectsChild.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXT_PSS_CUSTOMER_PART_NUMBER + "].value");
                        slSelectsChild.addElement("from[" + TigerConstants.RELATIONSHIP_PSS_COLORLIST + "].to.physicalid");
                        DomainObject.MULTI_VALUE_LIST.add("from[" + TigerConstants.RELATIONSHIP_PSS_COLORLIST + "].to.physicalid");
                        DomainObject.MULTI_VALUE_LIST.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXT_PSS_CUSTOMER_PART_NUMBER + "].value");

                        Map mAttributeMap = domMBOMObjectFromMap.getInfo(context, slAttributeSelects);

                        DomainObject.MULTI_VALUE_LIST.remove("from[" + TigerConstants.RELATIONSHIP_PSS_COLORLIST + "].to.physicalid");
                        DomainObject.MULTI_VALUE_LIST.remove("attribute[" + TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXT_PSS_CUSTOMER_PART_NUMBER + "].value");
                        slColorList = (StringList) (mAttributeMap.get("from[" + TigerConstants.RELATIONSHIP_PSS_COLORLIST + "].to.physicalid"));
                        strCustomerPartNumber = (StringList) (mAttributeMap.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXT_PSS_CUSTOMER_PART_NUMBER + "].value"));
                        strUnitofMeasureCategory = (String) mAttributeMap.get(TigerConstants.ATTRIBUTE_PSS_UNIT_OF_MEASURE_CATEGORY);
                        strUnitofMeasure = (String) mAttributeMap.get(TigerConstants.ATTRIBUTE_PSS_UNIT_OF_MEASURE);
                        varintAssemblyPID = pss.mbom.MBOMUtil_mxJPO.getVariantAssemblyPIDForMBOM(context, strMBOMId, pcID);

                        for (int j = 0; j < mlHarmonyList.size(); j++) {
                            Map mHarmonyMap = (Map) mlHarmonyList.get(j);
                            String strHarmonyId = (String) mHarmonyMap.get("id");

                            String associationRelId = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", strHarmonyId, "to[" + TigerConstants.RELATIONSHIP_PSS_HARMONYASSOCIATION
                                    + "|fromrel.id==" + strRelId + "&& attribute[" + TigerConstants.ATTRIBUTE_PSS_PRODUCT_CONFIGURATION_PID + "]=='" + pcID + "'].id");

                            if (UIUtil.isNullOrEmpty(associationRelId.trim())) {
                                associationRelId = MqlUtil.mqlCommand(context, "add connection $1 fromrel $2 to $3 select $4 dump", TigerConstants.RELATIONSHIP_PSS_HARMONYASSOCIATION, strRelId,
                                        strHarmonyId, "id");
                                DomainRelationship domRel = DomainRelationship.newInstance(context, associationRelId);
                                // TIGTK-9215:START
                                if (slMBOMType.contains(strType)) {

                                    strGrossWeight = (String) mAttributeMap.get(TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXTPSS_GROSSWEIGHT);
                                    strNetWeight = (String) mAttributeMap.get(TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXTPSS_NETWEIGHT);

                                }
                                // TIGTK-9215:END

                                Map mlAttributeMapValues = new HashMap();
                                mlAttributeMapValues.put(TigerConstants.ATTRIBUTE_PSS_PRODUCT_CONFIGURATION_PID, pcID);
                                mlAttributeMapValues.put(TigerConstants.ATTRIBUTE_PSS_VARIANTASSEMBLY_PID, varintAssemblyPID);
                                mlAttributeMapValues.put(TigerConstants.ATTRIBUTE_PSS_GROSSWEIGHT, strGrossWeight);
                                mlAttributeMapValues.put(TigerConstants.ATTRIBUTE_PSS_NETWEIGHT, strNetWeight);
                                domRel.setAttributeValues(context, mlAttributeMapValues);

                            }
                            if (UIUtil.isNotNullAndNotEmpty(associationRelId.trim())) {
                                String strColorable = DomainConstants.EMPTY_STRING;
                                if (slMBOMType.contains(strType)) {
                                    strColorable = getColorableValue(context, strMBOMId, plmSession);
                                }

                                String parentType = (String) mStructureMap.get(DomainConstants.SELECT_TYPE);
                                Map tempMapforHarmony = new HashMap();
                                tempMapforHarmony.put(DomainConstants.SELECT_TYPE, (String) mHarmonyMap.get(DomainConstants.SELECT_TYPE));
                                tempMapforHarmony.put(DomainConstants.SELECT_RELATIONSHIP_ID, associationRelId);
                                tempMapforHarmony.put(DomainConstants.SELECT_ID, (String) mHarmonyMap.get(DomainConstants.SELECT_ID));
                                tempMapforHarmony.put(DomainConstants.SELECT_LEVEL, String.valueOf(levelInt + 1));
                                tempMapforHarmony.put(TigerConstants.RELATIONSHIP_PSS_COLORLIST, slColorList);
                                tempMapforHarmony.put(CUSTOMER_PART_NUMBER, strCustomerPartNumber);
                                tempMapforHarmony.put(UNIT_OF_MEASURE_CATEGORY, strUnitofMeasureCategory);
                                tempMapforHarmony.put(UNIT_OF_MEASURE, strUnitofMeasure);
                                tempMapforHarmony.put(TigerConstants.ATTRIBUTE_PSS_MANUFACTURING_INSTANCEEXT_PSS_PHANTOM_LEVEL, strPhantomLevel);
                                tempMapforHarmony.put(TigerConstants.ATTRIBUTE_PSS_MANUFACTURING_INSTANCEEXT_PSS_PHANTOM_PART, strPhantomPart);
                                tempMapforHarmony.put("ParentType", parentType);
                                // TIGTK-10503:Start
                                tempMapforHarmony.put("Colorable", strColorable);
                                // TIGTK-10503:End
                                if ((strPhantomLevel.equalsIgnoreCase("Yes") && !parentType.equals("CreateMaterial"))
                                        || (strPhantomPart.equalsIgnoreCase("Yes") && parentType.equals("CreateMaterial"))) {
                                    tempMapforHarmony.put("RowEditable", "readonly");
                                }
                                finalMBOMList.add(i + count, tempMapforHarmony);
                                // ALM-4190 : START
                                DomainRelationship harmonyAssociationRel = DomainRelationship.newInstance(context, associationRelId);
                                // TIGTK-10265:RE:27/11/17:Start
                                String strCommonAssociationRelId = "";

                                if (UIUtil.isNullOrEmpty(pcID)) {
                                    strCommonAssociationRelId = associationRelId;
                                } else {
                                    strCommonAssociationRelId = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", strHarmonyId, "to[" + TigerConstants.RELATIONSHIP_PSS_HARMONYASSOCIATION
                                            + "|fromrel.id==" + strRelId + "&& attribute[" + TigerConstants.ATTRIBUTE_PSS_PRODUCT_CONFIGURATION_PID + "]==''].id");
                                }

                                if (UIUtil.isNotNullAndNotEmpty(strConnectionId) && UIUtil.isNotNullAndNotEmpty(strCommonAssociationRelId)) {
                                    String currentEffXML = FRCMBOMModelerUtility.getEffectivityXML(context, strConnectionId, true);
                                    String strEffectivity = FRCMBOMModelerUtility.getEffectivityOrderedStringFromXML(context, currentEffXML);
                                    if (UIUtil.isNullOrEmpty(strEffectivity) && UIUtil.isNullOrEmpty(varintAssemblyPID)) {
                                        DomainRelationship CommonHarmonyAssociationRel = DomainRelationship.newInstance(context, strCommonAssociationRelId);
                                        String Quantity = CommonHarmonyAssociationRel.getAttributeValue(context, TigerConstants.ATTRIBUTE_QUANTITY);
                                        String strColorList = CommonHarmonyAssociationRel.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_COLORPID);
                                        Map mlAttributeMapForRelIntChild = new HashMap();
                                        mlAttributeMapForRelIntChild.put(TigerConstants.ATTRIBUTE_QUANTITY, Quantity);
                                        mlAttributeMapForRelIntChild.put(TigerConstants.ATTRIBUTE_PSS_COLORPID, strColorList);
                                        harmonyAssociationRel.setAttributeValues(context, mlAttributeMapForRelIntChild);
                                    }
                                }
                                // TIGTK-10265:RE:27/11/17:End
                                Map mlAttributeMapForRelChild = new HashMap();
                                mlAttributeMapForRelChild.put(TigerConstants.ATTRIBUTE_PSS_PRODUCT_CONFIGURATION_PID, pcID);
                                mlAttributeMapForRelChild.put(TigerConstants.ATTRIBUTE_PSS_VARIANTASSEMBLY_PID, varintAssemblyPID);
                                harmonyAssociationRel.setAttributeValues(context, mlAttributeMapForRelChild);
                                // ALM-4190 : END

                            }
                            count++;
                        }

                    }

                }

            }

            flushAndCloseSession(plmSession);
            if (transactionActive)
                ContextUtil.commitTransaction(context);
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getExpandMBOMForHarmonyAssociation: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            flushAndCloseSession(plmSession);
            if (transactionActive)
                ContextUtil.abortTransaction(context);
        }

        return finalMBOMList;

    }

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @date : 10/08/16
     * @author vbhosale
     */
    @SuppressWarnings("rawtypes")
    public static MapList getResourceStructure(Context context, String[] args) throws Exception {
        final String RELATIONSHIP_RELATED_RESOURCE = PropertyUtil.getSchemaProperty(context, "relationship_PSS_RelatedResource");

        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String ObjectId = (String) programMap.get("objectId");
        String expandLevel = (String) programMap.get("expandLevel");
        int getexpandLevel = Integer.parseInt(expandLevel);

        String TYPE_VPMREFERENCE = PropertyUtil.getSchemaProperty(context, "type_VPMReference");
        MapList mapList = new MapList();

        StringList busSelect = new StringList(1);
        busSelect.addElement(DomainConstants.SELECT_ID);

        StringList relSelect = new StringList(1);
        relSelect.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

        try {
            if (UIUtil.isNotNullAndNotEmpty(ObjectId)) {
                DomainObject domObject = new DomainObject(ObjectId);
                mapList = domObject.getRelatedObjects(context, RELATIONSHIP_RELATED_RESOURCE, // relationship pattern
                        TYPE_VPMREFERENCE, // object pattern
                        busSelect, // object selects
                        relSelect, // relationship selects
                        false, // to direction
                        true, // from direction
                        (short) getexpandLevel, // recursion level
                        null, // object where clause
                        null, 0);
            }

        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getResourceStructure: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END

        }
        return mapList;
    }

    /**
     * @InstanceExt
     * @param context
     * @param args
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public static void insertNewManufItem(Context context, String[] args) throws Exception { // Called from FRCInsertManufItemPreProcess.jsp
        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            HashMap programMap = (HashMap) JPO.unpackArgs(args);

            HashMap requestMap = (HashMap) programMap.get("requestMap");
            String parentPID = (String) requestMap.get("objectId");

            HashMap paramMap = (HashMap) programMap.get("paramMap");
            String newRefPID = (String) paramMap.get("newObjectId");

            createInstance(context, plmSession, parentPID, newRefPID);

            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);
        } catch (Exception exp) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in insertNewManufItem: ", exp);
            // TIGTK-5405 - 11-04-2017 - VB - END
            flushAndCloseSession(plmSession);
            if (context.isTransactionActive()) {
                ContextUtil.abortTransaction(context);
            }
            throw exp;
        }
    }

    /**
     * @InstanceExt
     * @param context
     * @param args
     * @throws Exception
     */
    public static void insertDuplicatedManufItem(Context context, String[] args) throws Exception { // Called from FRCInsertDuplicatedPostProcess.jsp
        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            String mbomPath = args[0];
            String templateRefPID = args[1];
            String changeObjectName = args[2];

            // Recursively clone the template root node
            // TIGTK-3601 : START
            String mbomRefPID = "";
            String strAttachedPlant = pss.mbom.MBOMUtil_mxJPO.getMasterPlant(context, templateRefPID);
            if (UIUtil.isNotNullAndNotEmpty(strAttachedPlant)) {
                mbomRefPID = duplicateMBOMStructure(context, plmSession, templateRefPID, changeObjectName, new StringList(), strAttachedPlant);
            } else {
                mbomRefPID = duplicateMBOMStructure(context, plmSession, templateRefPID, changeObjectName);
            }
            // TIGTK-3601 : END

            // Create a new instance
            String[] mbomPathList = mbomPath.split("/");
            String mbomParentRefPID = null;
            if (mbomPath.contains("/"))
                mbomParentRefPID = MqlUtil.mqlCommand(context, "print connection " + mbomPathList[mbomPathList.length - 1] + " select to.physicalid dump |", false, false);
            else
                mbomParentRefPID = MqlUtil.mqlCommand(context, "print bus " + mbomPath + " select physicalid dump |", false, false);

            createInstance(context, plmSession, mbomParentRefPID, mbomRefPID);

            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);
            // ContextUtil.abortTransaction(context);
        } catch (Exception exp) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in insertDuplicatedManufItem: ", exp);
            // TIGTK-5405 - 11-04-2017 - VB - END
            flushAndCloseSession(plmSession);
            if (context.isTransactionActive()) {
                ContextUtil.abortTransaction(context);
            }
            throw exp;
        }
    }

    /**
     * @InstanceExt
     * @param context
     * @param plmSession
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public static String restructureInstance(Context context, PLMCoreModelerSession plmSession, String[] args) throws Exception {
        String newInstPID = null;

        String oldInstancePID = args[0];
        String newParentRefPID = args[1];

        // Get the info on the old instance
        // String instanceInfoStr = MqlUtil.mqlCommand(context, "print connection " + oldInstancePID + " select type logicalid to.physicalid dump |", false, false);
        // String[] instanceInfo = instanceInfoStr.split("\\|", -2);

        // Get the child reference pointed by the instance
        String childRefPID = MqlUtil.mqlCommand(context, "print connection " + oldInstancePID + " select to.physicalid dump |", false, false);

        // Get the parent reference of the instance
        String parentRefPID = MqlUtil.mqlCommand(context, "print connection " + oldInstancePID + " select from.physicalid dump |", false, false);

        // Get the attribute values of the instance
        DomainRelationship instObj = new DomainRelationship(oldInstancePID);
        Map instAttributes = instObj.getAttributeMap(context, true);
        instAttributes.remove("PLMInstance.V_TreeOrder");

        // Get the effectivity of the instance
        List<String> oldInstancePIDList = new ArrayList<String>();
        oldInstancePIDList.add(oldInstancePID);

        Map<String, String> effMap = FRCMBOMModelerUtility.getEffectivityOnInstanceList(context, plmSession, oldInstancePIDList, false);

        String effXMLStr = effMap.get(oldInstancePID);

        // Get the implement link of the instance
        List<String> implementPath = FRCMBOMModelerUtility.getImplementLinkInfoSimple(context, plmSession, oldInstancePID);

        // Get the effectivity checksum of the instance
        String checksum = FRCMBOMModelerUtility.getEffectivityChecksumStoredOnInstance(context, oldInstancePID);

        // Delete the instance
        instObj.closeRelationship(context, true);
        DomainRelationship.disconnect(context, oldInstancePID);

        // Create an identical new instance
        newInstPID = createInstance(context, plmSession, newParentRefPID, childRefPID);
        // String newInstPID = MqlUtil.mqlCommand(context, "add connection '" + instanceInfo[0] + "' from " + newParentRefPID + " to " + instanceInfo[2] + " select physicalid dump |", false, false);

        // Replicate all the attributes values on the new instance
        DomainRelationship newInstObj = new DomainRelationship(newInstPID);
        newInstObj.setAttributeValues(context, instAttributes);

        // Replicate the effectivity on this new instance
        if (effXMLStr != null && !"".equals(effXMLStr)) {
            // Set the model on the parent reference of the instance
            List<String> parentModelPIDList = FRCMBOMModelerUtility.getAttachedModelsOnReference(context, plmSession, parentRefPID);

            List<String> newParentRefPIDList = new ArrayList<String>();
            newParentRefPIDList.add(newParentRefPID);

            FRCMBOMModelerUtility.attachModelsOnReferencesAndSetOptionsCriteria(context, plmSession, newParentRefPIDList, parentModelPIDList);

            FRCMBOMModelerUtility.setOrUpdateEffectivityOnInstance(context, plmSession, newInstPID, effXMLStr);
        }

        // Replicate the effectivity checksum on the new instance
        FRCMBOMModelerUtility.storeEffectivityChecksumOnInstance(context, newInstPID, checksum);

        // Replicate the implement link on this new instance, WITHOUT UPDATE OF THE EFFECTIVITY! We want to keep the status (broken or solved) of the link.
        if (implementPath.size() > 0) {
            StringBuffer implementPIDPathSB = new StringBuffer();
            for (int i = 0; i < implementPath.size(); i++) {
                if (implementPIDPathSB.length() > 0)
                    implementPIDPathSB.append("/");

                implementPIDPathSB.append(implementPath.get(i));
            }

            List<String> mbomLeafInstancePIDList = new ArrayList<String>();
            mbomLeafInstancePIDList.add(newInstPID);
            List<String> trimmedPSPathList = new ArrayList<String>();
            trimmedPSPathList.add(implementPIDPathSB.toString());
            String retStr = PSS_FRCMBOMModelerUtility_mxJPO.setImplementLinkBulk(context, plmSession, mbomLeafInstancePIDList, trimmedPSPathList, false);
            if (!"".equals(retStr))
                throw new Exception(retStr);

        }

        return newInstPID;
    }

    // FRC Start: Fixed Bug 671-Quantity value on Materials
    @SuppressWarnings("rawtypes")
    public static void insertNewManufItemAndUpdateQuantity(Context context, String[] args) throws Exception { // Called from FRCInsertManufItemPreProcess.jsp
        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            HashMap programMap = (HashMap) JPO.unpackArgs(args);

            HashMap requestMap = (HashMap) programMap.get("requestMap");
            String parentPID = (String) requestMap.get("objectId");
            String quantity = (String) requestMap.get("Quantity");
            if (UIUtil.isNullOrEmpty(quantity)) {
                quantity = "1.0";
            }

            HashMap paramMap = (HashMap) programMap.get("paramMap");
            String newRefPID = (String) paramMap.get("newObjectId");

            createInstance(context, plmSession, parentPID, newRefPID);

            // Update Quantity
            String childRefType = MqlUtil.mqlCommand(context, "print bus " + newRefPID + " select type dump |", false, false);

            String continuousRefTypesListStr = MqlUtil.mqlCommand(context, "print type DELFmiFunctionPPRContinuousReference select derivative dump |", false, false);
            String[] continuousRefTypesListArray = continuousRefTypesListStr.split("\\|");
            List<String> continuousRefTypesList = Arrays.asList(continuousRefTypesListArray);

            if (continuousRefTypesList.contains(childRefType)) {
                // Get Connection Id
                String connectionID = MqlUtil.mqlCommand(context, "print bus " + newRefPID + " select to.id dump |", false, false);

                String refQuantityValue = "";
                if (newRefPID == null || "".equals(newRefPID))
                    refQuantityValue = "1.0";
                else {
                    refQuantityValue = MqlUtil.mqlCommand(context, "print bus " + newRefPID + " select attribute[V_ContQuantity].value dump |", false, false);
                }

                double refQuantity = Double.parseDouble(refQuantityValue);
                double newVal = Double.parseDouble(quantity);
                double usageCoeff = newVal / refQuantity;
                String instanceUsgaeCoefficient = Double.toString(usageCoeff);

                if (connectionID != null && !"".equals(connectionID)) {
                    MqlUtil.mqlCommand(context, "mod connection " + connectionID + " " + "ProcessInstanceContinuous.V_UsageContCoeff" + " " + instanceUsgaeCoefficient, false, false);
                }
            }
            // RFC-139 : Update the Master Plant Name on insert new MBOM
            String strMasterMfgProductionPlanning = pss.mbom.MBOMUtil_mxJPO.getMasterMfgProductionPlanning(context, newRefPID);
            // TIGTK-12976 : START
            DomainObject dMBOMObj = DomainObject.newInstance(context, newRefPID);
            String strPolicy = dMBOMObj.getInfo(context, DomainConstants.SELECT_POLICY);
            String strType = dMBOMObj.getInfo(context, DomainConstants.SELECT_POLICY);

            // OOTB attribute Modification : Start : TIGTK-13669
            if (strType.equalsIgnoreCase(TigerConstants.TYPE_CREATEMATERIAL))
                dMBOMObj.setAttributeValue(context, "CreateMaterial.V_NeedDedicatedSystem", "2");
            // OOTB attribute Modification : END

            if (TigerConstants.POLICY_PSS_STANDARDMBOM.equals(strPolicy)) {
                String PSS_UNITOFMEASURE = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale("en"), "emxFramework.Range.PSS_ManufacturingUoMExt.PSS_UnitOfMeasure.PC");
                dMBOMObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_UNIT_OF_MEASURE, PSS_UNITOFMEASURE);
            }
            // TIGTK-12976 : END
            if (UIUtil.isNotNullAndNotEmpty(strMasterMfgProductionPlanning)) {
                DomainObject dMfgProductionPlanningObj = DomainObject.newInstance(context, strMasterMfgProductionPlanning);
                String strPlantName = dMfgProductionPlanningObj.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_VOWNER + "].from.name");

                dMBOMObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXTPSS_MATERPLANTNAME, strPlantName);
            }

            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);
        } catch (Exception exp) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in insertNewManufItemAndUpdateQuantity: ", exp);
            // TIGTK-5405 - 11-04-2017 - VB - END
            flushAndCloseSession(plmSession);
            if (context.isTransactionActive()) {
                ContextUtil.abortTransaction(context);
            }
            throw exp;
        }
    }

    /**
     * @InstanceExt
     * @param context
     * @param plmSession
     * @param parentRefPID
     * @param childRefPID
     * @return
     * @throws Exception
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static String createInstance(Context context, PLMCoreModelerSession plmSession, String parentRefPID, String childRefPID) throws Exception {
        // Compute the default instance title
        String childRefTitle = MqlUtil.mqlCommand(context, "print bus " + childRefPID + " select attribute[PLMEntity.V_Name].value dump |", false, false);

        String refTitleListStr = MqlUtil.mqlCommand(context, "print bus " + parentRefPID + " select from[PLMInstance].to.attribute[PLMEntity.V_Name].value dump |", false, false);
        int newOccNbr = refTitleListStr.split(java.util.regex.Pattern.quote(childRefTitle), -1).length;
        String instanceTitle = childRefTitle + "." + newOccNbr;

        Hashtable instanceAttributes = new Hashtable();
        instanceAttributes.put("PLM_ExternalID", instanceTitle);

        String relPID = PSS_FRCMBOMModelerUtility_mxJPO.createMBOMInstance(context, plmSession, parentRefPID, childRefPID, instanceAttributes);
        // MBO-164-MBOM performance issue:START-H65 15/11/2017
        flushSession(plmSession);
        if (UIUtil.isNotNullAndNotEmpty(relPID)) {
            String relationshipId = MqlUtil.mqlCommand(context, "print connection " + relPID + " select id dump", false, false);
            MqlUtil.mqlCommand(context, "mod connection " + relationshipId + " add interface FRCCustoExtension1 ", false, false);
            flushSession(plmSession);
        }
        // MBO-164-Below MBOM performance issue:END-H65 15/11/2017

        // PSS : START
        pss.mbom.MBOMUtil_mxJPO.postCreateInstance(context, relPID);
        // PSS : END

        return relPID;
    }

    /**
     * @InstanceExt
     * @param context
     * @param plmSession
     * @param parentRefPID
     * @param childRefPID
     * @param instanceAttributes
     * @return
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public static String createInstance(Context context, PLMCoreModelerSession plmSession, String parentRefPID, String childRefPID, Hashtable instanceAttributes) throws Exception {

        String relPID = PSS_FRCMBOMModelerUtility_mxJPO.createMBOMInstance(context, plmSession, parentRefPID, childRefPID, instanceAttributes);
        flushSession(plmSession);

        // PSS : START
        pss.mbom.MBOMUtil_mxJPO.postCreateInstance(context, relPID);
        // PSS : END

        return relPID;
    }

    /**
     * @InstanceExt Method to add interface on connection Insert reference under ref pass in paramter
     * @param Parent
     *            Ref PID
     * @param Reference
     *            to insert
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static String insertExistingManufItemFromDBWOLinks(Context context, String[] args) throws Exception {// Called from FRCInsertExistingPostProcess.jsp
        PLMCoreModelerSession plmSession = null;
        PLMxRefInstanceEntity instanceEntity = null;
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            String mbomParentRefPID = args[0];
            String mbomRefPID = args[1];

            // Create a new instance
            // TIGTK-10773:Start
            String newInstPID = createInstance(context, plmSession, mbomParentRefPID, mbomRefPID);
            StringList slSplitExtensionList = new StringList();
            String[] strArray = null;
            String strExtension = MqlUtil.mqlCommand(context, "print connection " + newInstPID + " select interface dump |", false, false);
            if (UIUtil.isNotNullAndNotEmpty(strExtension)) {
                if (strExtension.contains("|")) {
                    strArray = strExtension.split("\\|");
                    if (strArray.length > 0) {
                        for (int i = 0; i < strArray.length; i++)
                            slSplitExtensionList.add(strArray[i]);
                    }
                } else
                    slSplitExtensionList.add(strExtension);
            }
            if (!slSplitExtensionList.contains("FRCCustoExtension1"))
                MqlUtil.mqlCommand(context, "mod connection " + newInstPID + " add interface FRCCustoExtension1", false, false);

            // TIGTK-10773:End
            flushAndCloseSession(plmSession);

            ContextUtil.commitTransaction(context);
            return newInstPID;
            // ContextUtil.abortTransaction(context);
        } catch (Exception exp) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in insertExistingManufItemFromDBWOLinks: ", exp);
            // TIGTK-5405 - 11-04-2017 - VB - END
            flushAndCloseSession(plmSession);
            if (context.isTransactionActive()) {
                ContextUtil.abortTransaction(context);
            }
            throw exp;
        }
    }

    /**
     * @InstanceExt Method to add interface on connection
     * @param context
     * @param args
     * @throws Exception
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void insertExistingManufItem(Context context, String[] args) throws Exception {// Called from FRCInsertExistingPostProcess.jsp
        PLMCoreModelerSession plmSession = null;
        PLMxRefInstanceEntity instanceEntity = null;
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            String mbomPath = args[0];
            String mbomRefPID = args[1];
            String psPath = args[2];

            String[] mbomPathList = mbomPath.split("/");

            String mbomParentRefPID = null;

            if (mbomPathList.length == 1) {
                mbomParentRefPID = mbomPathList[0];
            } else {
                String mbomFinalInstPID = mbomPathList[mbomPathList.length - 1];
                mbomParentRefPID = MqlUtil.mqlCommand(context, "print connection " + mbomFinalInstPID + " select to.physicalid dump |", false, false);
            }

            // Create a new instance
            // TIGTK-10773:Start
            String newInstPID = createInstance(context, plmSession, mbomParentRefPID, mbomRefPID);
            // TIGTK-10773:End

            StringBuffer sbMBOMPath = new StringBuffer();
            // Create an implement link
            if (UIUtil.isNotNullAndNotEmpty(mbomPath)) {
                sbMBOMPath.append(mbomPath);
                sbMBOMPath.append("/");
                sbMBOMPath.append(newInstPID);
            }
            // Add the custom extension for the effectivity checksum
            StringList slSplitExtensionList = new StringList();
            String[] strArray = null;
            String strExtension = MqlUtil.mqlCommand(context, "print connection " + newInstPID + " select interface dump |", false, false);
            if (UIUtil.isNotNullAndNotEmpty(strExtension)) {
                if (strExtension.contains("|")) {
                    strArray = strExtension.split("\\|");
                    if (strArray.length > 0) {
                        for (int i = 0; i < strArray.length; i++)
                            slSplitExtensionList.add(strArray[i]);
                    }
                } else
                    slSplitExtensionList.add(strExtension);
            }
            if (!slSplitExtensionList.contains("FRCCustoExtension1"))
                MqlUtil.mqlCommand(context, "mod connection " + newInstPID + " add interface FRCCustoExtension1", false, false);

            String trimmedPSPath = trimPSPathToClosestScope(context, plmSession, sbMBOMPath.toString(), psPath);

            if (trimmedPSPath == null || "".equals(trimmedPSPath))
                throw new Exception("No parent scope exists.");

            // Put a new implement link
            List<String> mbomLeafInstancePIDList = new ArrayList();
            mbomLeafInstancePIDList.add(newInstPID);
            List<String> trimmedPSPathList = new ArrayList();
            trimmedPSPathList.add(trimmedPSPath);
            String retStr = FRCMBOMModelerUtility.setImplementLinkBulk(context, plmSession, mbomLeafInstancePIDList, trimmedPSPathList, true);
            if (!"".equals(retStr))
                throw new Exception(retStr);

            flushAndCloseSession(plmSession);

            ContextUtil.commitTransaction(context);
            // ContextUtil.abortTransaction(context);
        } catch (Exception exp) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in insertExistingManufItem: ", exp);
            // TIGTK-5405 - 11-04-2017 - VB - END
            flushAndCloseSession(plmSession);
            if (context.isTransactionActive()) {
                ContextUtil.abortTransaction(context);
            }
            throw exp;
        }
    }

    /**
     * @ReCustomizeDS
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public static String setImplementLink(Context context, String[] args) throws Exception {// Called by FRCCreateOrUpdateImplementLinkPostProcess.jsp
        // Return value :
        // 0 = refresh row of the leaf MBOM instance
        // 1 = re-expand the row of the leaf MBOM instance
        // 2 = re-expand the row of the parent of the MBOM instance
        String returnValue = "0";

        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            String mbomCompletePath = args[0];
            String psCompletePaths = args[1];// Multiple paths possible
            String changeObjectPID = args[2];
            String approvalStatus = args[3];

            String[] arrPsPaths = psCompletePaths.split("\\|", -2);
            int iCurrentRetVal = 0;
            String[] newArgs = new String[4];
            newArgs[0] = mbomCompletePath;
            newArgs[1] = "";
            newArgs[2] = changeObjectPID;
            newArgs[3] = approvalStatus;
            List<String> newRefPIDList = new ArrayList<String>();

            String[] mbomCompletePathList = mbomCompletePath.split("/");
            String mbomLeafInstancePID = null;
            String mbomLeafRefPID = null;
            if (mbomCompletePathList.length > 1) {
                mbomLeafInstancePID = mbomCompletePathList[mbomCompletePathList.length - 1];
                mbomLeafRefPID = MqlUtil.mqlCommand(context, "print connection " + mbomLeafInstancePID + " select to.physicalid dump |", false, false);
            } else {
                mbomLeafRefPID = mbomCompletePathList[mbomCompletePathList.length - 1];
            }

            String strAttachedPlant = null;
            if (UIUtil.isNotNullAndNotEmpty(mbomLeafRefPID)) {
                strAttachedPlant = pss.mbom.MBOMUtil_mxJPO.getMasterPlant(context, mbomLeafRefPID);
            }

            Map<String, List<String>> workingInfo = new HashMap<String, List<String>>();
            workingInfo.put("instanceToCreate_parentRefPLMID", new ArrayList<String>());
            workingInfo.put("instanceToCreate_childRefPLMID", new ArrayList<String>());
            workingInfo.put("mbomLeafInstancePIDList", new ArrayList<String>());
            workingInfo.put("psPathList", new ArrayList<String>());
            workingInfo.put("newRefPIDList", new ArrayList<String>());
            workingInfo.put("newScopesToCreate_MBOMRefPIDs", new ArrayList<String>());
            workingInfo.put("newScopesToCreate_MBOMRefPLMIDs", new ArrayList<String>());
            workingInfo.put("newScopesToCreate_PSRefPIDs", new ArrayList<String>());
            workingInfo.put("newScopeObjectList", new ArrayList<String>());

            List<Map<String, String>> workingInfo_instanceAttributes = new ArrayList<Map<String, String>>();
            List<Integer> workingInfo_indexInstancesForImplement = new ArrayList<Integer>();
            // Set<String> workingInfo_lModelListOnStructure = new HashSet<String>();
            Map<String, String> workingInfo_AppDateToValuate = new HashMap<String, String>();

            if (UIUtil.isNullOrEmpty(approvalStatus))
                PropertyUtil.setRPEValue(context, FROMDRAGNDROP, "true", true);

            for (String psPathHere : arrPsPaths) {
                newArgs[1] = psPathHere;
                // String returnValueHere = setImplementLinkProcess(context, plmSession, newArgs, newRefPIDList, strAttachedPlant);

                String returnValueHere = setImplementLinkProcess_new(context, plmSession, null, psPathHere, workingInfo, workingInfo_instanceAttributes, workingInfo_indexInstancesForImplement,
                        workingInfo_AppDateToValuate, strAttachedPlant, newArgs);

                int iRetVal = Integer.parseInt(returnValueHere);
                if (iRetVal > iCurrentRetVal)
                    iCurrentRetVal = iRetVal;
            }

            // Attach all new references created during the process to the change object

            attachListObjectsToChange(context, plmSession, changeObjectPID, newRefPIDList);

            // returnValue = setImplementLinkProcess(context, args);
            returnValue = Integer.toString(iCurrentRetVal);

            flushAndCloseSession(plmSession);
            // HE5 : Added fix for the error "Transaction not active" after calling the R&D API replaceMBOMInstance
            if (context.isTransactionActive())
                ContextUtil.commitTransaction(context);
            // ContextUtil.abortTransaction(context);
        } catch (Exception exp) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in setImplementLink: ", exp);
            // TIGTK-5405 - 11-04-2017 - VB - END
            flushAndCloseSession(plmSession);
            if (context.isTransactionActive()) {
                ContextUtil.abortTransaction(context);
            }
            throw exp;
        }

        return returnValue;
    }

    /**
     * @ReCustomizeDS
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static MapList getScopingMBOMReference(Context context, String[] args) throws Exception {// Called by FRCCreateOrUpdateImplementLinkPostProcess.jsp
        PLMCoreModelerSession plmSession = null;
        MapList listWithPlant = new MapList();
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            // String mbomCompletePath = args[0];
            String psCompletePaths = args[1];// Multiple paths possible

            String[] arrPsPaths = psCompletePaths.split("\\|", -2);
            // String[] newArgs = new String[3];
            // newArgs[0] = mbomCompletePath;
            // newArgs[1] = "";

            List<String> mbomRefPIDList = new ArrayList<String>();

            for (String psPathHere : arrPsPaths) {
                String psCompletePath = psPathHere;

                String[] psCompletePathList = psCompletePath.split("/");
                String psLeafInstancePID = psCompletePathList[psCompletePathList.length - 1];
                String psLeafRefPID = MqlUtil.mqlCommand(context, "print connection " + psLeafInstancePID + " select to.physicalid dump |", false, false);

                List<String> mbomRefPIDScopedWithPSRefList = PSS_FRCMBOMModelerUtility_mxJPO.getScopingReferencesFromList(context, plmSession, psLeafRefPID);
                mbomRefPIDList.addAll(mbomRefPIDScopedWithPSRefList);
            }

            for (String mbomRefId : mbomRefPIDList) {
                Map infoMap = new HashMap();
                infoMap.put("physicalid", mbomRefId);
                DomainObject dObj = DomainObject.newInstance(context, mbomRefId);
                infoMap.put("Name", dObj.getAttributeValue(context, "PLMEntity.V_Name"));
                infoMap.put("MajorRevision", dObj.getInfo(context, "majorrevision"));
                String plantId = "";
                String plantName = "";
                String strAttachedPlant = pss.mbom.MBOMUtil_mxJPO.getMasterPlant(context, mbomRefId);
                if (UIUtil.isNotNullAndNotEmpty(strAttachedPlant)) {
                    DomainObject plantObj = DomainObject.newInstance(context, strAttachedPlant);
                    plantId = plantObj.getInfo(context, DomainConstants.SELECT_ID);
                    plantName = plantObj.getInfo(context, DomainConstants.SELECT_NAME);
                }
                infoMap.put("PlantId", plantId);
                infoMap.put("PlantName", plantName);
                listWithPlant.add(infoMap);
            }
            flushAndCloseSession(plmSession);
            if (context.isTransactionActive())
                ContextUtil.commitTransaction(context);
        } catch (Exception exp) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getScopingMBOMReference: ", exp);
            // TIGTK-5405 - 11-04-2017 - VB - END
            flushAndCloseSession(plmSession);
            if (context.isTransactionActive()) {
                ContextUtil.abortTransaction(context);
            }
            throw exp;
        }
        return listWithPlant;
    }

    /**
     * @ReCustomizeDS
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public static String setImplementLinkWithGivenMBOMRef(Context context, String[] args) throws Exception {
        String returnValue = "0";

        PLMCoreModelerSession plmSession = null;
        try {

            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            String mbomCompletePath = args[0];
            String psCompletePaths = args[1];// Multiple paths possible
            // String changeObjectPID = args[2];
            String newMBOMRefPID = args[3];
            String plantPID = args[4];

            String[] arrPsPaths = psCompletePaths.split("\\|", -2);
            // String[] newArgs = new String[3];
            // newArgs[0] = mbomCompletePath;
            // newArgs[1] = "";

            String[] mbomCompletePathList = mbomCompletePath.split("/");
            String mbomLeafInstancePID = null;
            String mbomLeafRefPID = null;
            if (mbomCompletePathList.length > 1) {
                mbomLeafInstancePID = mbomCompletePathList[mbomCompletePathList.length - 1];
                mbomLeafRefPID = MqlUtil.mqlCommand(context, "print connection " + mbomLeafInstancePID + " select to.physicalid dump |", false, false);
            } else {
                mbomLeafRefPID = mbomCompletePathList[mbomCompletePathList.length - 1];
            }
            String strType = DomainConstants.EMPTY_STRING;
            if (UIUtil.isNotNullAndNotEmpty(newMBOMRefPID) && !newMBOMRefPID.equals("CreateNew")) {
                DomainObject domNewObject = DomainObject.newInstance(context, newMBOMRefPID);
                strType = domNewObject.getInfo(context, DomainConstants.SELECT_TYPE);
            }

            if (UIUtil.isNotNullAndNotEmpty(newMBOMRefPID) && !strType.equalsIgnoreCase(TigerConstants.TYPE_CREATEASSEMBLY)) {

                for (String psPathHere : arrPsPaths) {
                    String psCompletePath = psPathHere;

                    String[] psCompletePathList = psCompletePath.split("/");
                    String psLeafInstancePID = psCompletePathList[psCompletePathList.length - 1];
                    String psLeafRefPID = MqlUtil.mqlCommand(context, "print connection " + psLeafInstancePID + " select to.physicalid dump |", false, false);
                    if (newMBOMRefPID.equals("CreateNew")) {
                        HashMap<String, String> attributes = new HashMap<String, String>();
                        newMBOMRefPID = createMBOMReferenceWithPlant(context, plmSession, "CreateMaterial", null, attributes, psLeafRefPID, plantPID);
                        PSS_FRCMBOMModelerUtility_mxJPO.createScopeLinkAndReduceChildImplementLinks(context, plmSession, newMBOMRefPID, psLeafRefPID, false);
                    }
                    if (!newMBOMRefPID.equals(mbomLeafRefPID)) { // The leaf MBOM reference is not the one synched with the leaf PS reference. Normally because it is a different revision
                        // Replace the MBOM leaf instance with the new one
                        String newMBOMLeafInstancePID = PSS_FRCMBOMModelerUtility_mxJPO.replaceMBOMInstance(context, plmSession, mbomLeafInstancePID, newMBOMRefPID);

                        mbomCompletePath = mbomCompletePath.replace(mbomLeafInstancePID, newMBOMLeafInstancePID);
                        mbomLeafInstancePID = newMBOMLeafInstancePID;
                        // PSS : Start
                        mbomLeafRefPID = newMBOMRefPID;
                        // PSS :End
                        returnValue = "2";
                    }

                    String trimmedPSPath = trimPSPathToClosestScope(context, plmSession, mbomCompletePath, psCompletePath);

                    if (trimmedPSPath == null || "".equals(trimmedPSPath)) {
                        throw new Exception("No scope exists.");
                    }

                    // Remove any existing implement link
                    PSS_FRCMBOMModelerUtility_mxJPO.deleteImplementLink(context, plmSession, mbomLeafInstancePID, true);

                    // Put a new implement link
                    List<String> mbomLeafInstancePIDList = new ArrayList<String>();
                    mbomLeafInstancePIDList.add(mbomLeafInstancePID);
                    List<String> trimmedPSPathList = new ArrayList<String>();
                    trimmedPSPathList.add(trimmedPSPath);

                    String retStr = PSS_FRCMBOMModelerUtility_mxJPO.setImplementLinkBulk(context, plmSession, mbomLeafInstancePIDList, trimmedPSPathList, true);
                    if (!"".equals(retStr))
                        throw new Exception(retStr);
                    /*
                     * String strNotGenerateVariantAssemmbly = PropertyUtil.getGlobalRPEValue(context, NOTGENERATEVARINTASSEMBLY); if (UIUtil.isNullOrEmpty(strNotGenerateVariantAssemmbly))
                     * disconnectVaraintAssemblyFromMBOMAndCreateNew(context, mbomLeafRefPID, psLeafRefPID); else if (!strNotGenerateVariantAssemmbly.equalsIgnoreCase("True"))
                     * disconnectVaraintAssemblyFromMBOMAndCreateNew(context, mbomLeafRefPID, psLeafRefPID);
                     */

                    // PSS: END
                }
            } else
                throw new Exception("No scope exists.");
            flushAndCloseSession(plmSession);
            if (context.isTransactionActive())
                ContextUtil.commitTransaction(context);
        } catch (Exception exp) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in setImplementLinkWithGivenMBOMRef: ", exp);
            // TIGTK-5405 - 11-04-2017 - VB - END
            flushAndCloseSession(plmSession);
            if (context.isTransactionActive()) {
                ContextUtil.abortTransaction(context);
            }
            throw exp;
        }
        return returnValue;
    }

    /**
     * This method is used to display Faurecia Part Number on MBOM Creation form from global Action menu TIGTK-3515
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @date : 23/08/16
     */
    @SuppressWarnings("rawtypes")
    public static String getFaureciaPartNumber(Context context, String[] args) throws Exception {
        String strAutoName = "";
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            Map requestMap = (Map) programMap.get("requestMap");
            String typeStr = (String) requestMap.get("type");
            if (UIUtil.isNotNullAndNotEmpty(typeStr) && typeStr.contains("_selectedType:")) {
                StringList typeList = FrameworkUtil.split(typeStr, ",");
                for (int i = 0; i < typeList.size(); i++) {
                    String type = (String) typeList.get(0);
                    if (type.contains("_selectedType:")) {
                        typeStr = type.replace("_selectedType:", "");
                        break;
                    }
                }
                if (typeStr.equals(TigerConstants.TYPE_PROCESSCONTINUOUSPROVIDE) || typeStr.equals(TigerConstants.TYPE_PROCESS_CONTINUOUS_CREATE_MATERIAL)) {
                    String symbolicTypeName = FrameworkUtil.getAliasForAdmin(context, DomainConstants.SELECT_TYPE, typeStr, true);
                    strAutoName = DomainObject.getAutoGeneratedName(context, symbolicTypeName, null);
                } else {
                    strAutoName = DomainObject.getAutoGeneratedName(context, "type_CreateAssembly", "FAURECIA");
                }
            } else {
                strAutoName = DomainObject.getAutoGeneratedName(context, "type_CreateAssembly", "FAURECIA");
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getFaureciaPartNumber: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
        return strAutoName;
    }

    /**
     * This method is used to set Faurecia Part Number on MBOM Creation form from global Action menu TIGTK-3515
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @date : 23/08/16
     */
    @SuppressWarnings("rawtypes")
    public static void setFaureciaPartNumber(Context context, String[] args) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        Map paramMap = (Map) programMap.get("paramMap");
        Map fieldMap = (Map) programMap.get("fieldMap");
        String strFaureciaPartNumber = "";
        try {
            String strObjectId = (String) paramMap.get("objectId");

            StringList values = (StringList) fieldMap.get("field_value");
            if (values != null && values.size() == 1)
                strFaureciaPartNumber = (String) values.get(0);
            if (UIUtil.isNotNullAndNotEmpty(strFaureciaPartNumber) && UIUtil.isNotNullAndNotEmpty(strObjectId)) {
                DomainObject dObj = DomainObject.newInstance(context, strObjectId);
                if (strFaureciaPartNumber.startsWith("M")) {
                    dObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PLM_EXTERNALID, strFaureciaPartNumber);
                    dObj.setName(context, strFaureciaPartNumber);
                }
                dObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_V_NAME, strFaureciaPartNumber);
            }

        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in setFaureciaPartNumber: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
    }

    /**
     * This method is used connect MBOM with Plant from MBOM Creation form
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @date : 23/08/16
     */
    @SuppressWarnings("rawtypes")
    public void connectPlantToMBOM(Context context, String[] args) throws Exception {
        PLMCoreModelerSession plmSession = null;
        try {

            // PSS ALM4253 fix START
            PropertyUtil.setRPEValue(context, PLANTFROMENOVIA, "true", false);
            // PSS ALM4253 fix END

            ContextUtil.startTransaction(context, true);
            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();
            Map programMap = JPO.unpackArgs(args);
            Map paramMap = (Map) programMap.get("paramMap");
            String strMBOMId = (String) paramMap.get("objectId");
            String strPlantId = (String) paramMap.get("New OID");
            if (UIUtil.isNotNullAndNotEmpty(strPlantId)) {
                String strPlantPID = MqlUtil.mqlCommand(context, "print bus " + strPlantId + " select physicalid dump |", false, false);
                PSS_FRCMBOMModelerUtility_mxJPO.attachPlantToMBOMReference(context, plmSession, strMBOMId, strPlantPID);
            }
            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in connectPlantToMBOM: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            flushAndCloseSession(plmSession);
            if (context.isTransactionActive()) {
                ContextUtil.abortTransaction(context);
            }
            throw e;
        }
    }

    /**
     * This method is used to display Ranges of instance attributes on Edit Page of Properties
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public StringList getAttributeRanges(Context context, String[] args) throws Exception {
        StringList slRanges = new StringList();

        try {

            Map programMap = JPO.unpackArgs(args);
            Map mFieldMap = (Map) programMap.get("fieldMap");
            Map mSettings = (Map) mFieldMap.get("settings");
            // Map mParamMap = (Map) programMap.get("paramMap");
            // String strRelId = (String) mParamMap.get("relId");

            String strAdminType = (String) mSettings.get("Admin Type");

            if (UIUtil.isNotNullAndNotEmpty(strAdminType)) {
                String ATTRIBUTE_NAME = PropertyUtil.getSchemaProperty(context, strAdminType);
                slRanges = FrameworkUtil.getRanges(context, ATTRIBUTE_NAME);
            }

        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getAttributeRanges: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }

        return slRanges;
    }

    /**
     * This method is used to display Value of instance attributes on Property Page
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public String getAttributeValue(Context context, String[] args) throws Exception {
        String strAttributeValue = "";
        try {

            Map programMap = JPO.unpackArgs(args);
            Map mFieldMap = (Map) programMap.get("fieldMap");
            Map mSettings = (Map) mFieldMap.get("settings");
            String strAdminType = (String) mSettings.get("Admin Type");
            Map mParamMap = (Map) programMap.get("paramMap");
            String strRelId = (String) mParamMap.get("relId");

            if (UIUtil.isNotNullAndNotEmpty(strAdminType) && UIUtil.isNotNullAndNotEmpty(strRelId)) {
                String ATTRIBUTE_NAME = PropertyUtil.getSchemaProperty(context, strAdminType);
                strAttributeValue = DomainRelationship.getAttributeValue(context, strRelId, ATTRIBUTE_NAME);
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getAttributeValue: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }

        return strAttributeValue;
    }

    /**
     * This method is used to Update value of instance attributes
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public void updateAttributeValue(Context context, String[] args) throws Exception {
        try {
            Map programMap = JPO.unpackArgs(args);
            Map mParamMap = (Map) programMap.get("paramMap");
            String strSelectedValue = (String) mParamMap.get("New Value");
            Map mFieldMap = (Map) programMap.get("fieldMap");
            Map mSettings = (Map) mFieldMap.get("settings");
            String strAdminType = (String) mSettings.get("Admin Type");
            String strRelId = (String) mParamMap.get("relId");

            if (UIUtil.isNotNullAndNotEmpty(strAdminType) && UIUtil.isNotNullAndNotEmpty(strRelId)) {
                String ATTRIBUTE_NAME = PropertyUtil.getSchemaProperty(context, strAdminType);
                DomainRelationship domRelationship = new DomainRelationship(strRelId);
                domRelationship.setAttributeValue(context, ATTRIBUTE_NAME, strSelectedValue);
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in updateAttributeValue: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
    }

    /**
     * This method is used to display Faurecia Part Number on MBOM Creation form under Part MBOM tab
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @date : 27/08/16
     */
    @SuppressWarnings("rawtypes")
    public static String getFaureciaPartNumberForMBOM(Context context, String[] args) throws Exception {
        String strPartName = "";
        try {
            Map programMap = JPO.unpackArgs(args);
            // Map mParamMap = (Map) programMap.get("paramMap");
            Map mrequestMap = (Map) programMap.get("requestMap");

            String strPartId = (String) mrequestMap.get("objectId");
            if (UIUtil.isNotNullAndNotEmpty(strPartId)) {
                strPartName = MqlUtil.mqlCommand(context, "print bus " + strPartId + " select name dump |", false, false);
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getFaureciaPartNumberForMBOM: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
        return strPartName;
    }

    public static MapList getExpandPS(Context context, String objectId, short expLvl, String filterExpr, String filterValue, String filterInput, StringList relSelect, StringList busSelect)
            throws Exception, FrameworkException {
        // Common PS expand method for IndentedTable and GraphicalBrowser
        MapList res;

        String pcGlobalFilterCompExpr = null;
        String pcGlobalFilterXMLValue = null;

        PLMCoreModelerSession plmSession = null;
        try {
            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            // Prepare the filter expression if there is one
            if (filterExpr != null && !"".equals(filterExpr) && !"undefined".equals(filterExpr) && !"null".equals(filterExpr))
                pcGlobalFilterCompExpr = filterExpr;
            if (filterValue != null && !"".equals(filterValue) && !"undefined".equals(filterValue) && !"null".equals(filterValue)) {
                // Transform the expression into a neutral text string
                String sNeutralExpr = formatEffExpToNeutral(context, filterValue);

                // Get the model of the filter
                String modelName = filterInput.substring(0, filterInput.indexOf(":"));
                String modId = MqlUtil.mqlCommand(context, "print bus Model '" + modelName + "' '' select physicalid dump |", false, false);

                // Convert expression to XML
                pcGlobalFilterXMLValue = convertEffNeutralToXML(context, sNeutralExpr, modId, "filter");
            }

            res = PSS_FRCMBOMModelerUtility_mxJPO.getVPMStructure(context, plmSession, objectId, busSelect, relSelect, expLvl, pcGlobalFilterCompExpr, pcGlobalFilterXMLValue);
        } finally {
            closeSession(plmSession);
        }

        return res;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @com.matrixone.apps.framework.ui.ProgramCallable
    public static MapList getExpandProductStructure(Context context, String[] args) throws Exception {// Expand program called by the emxIndentedTable.jsp of the PS table
        long startTime;
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);

        // Add configuration filter
        String filterExpression = (String) paramMap.get("FRCExpressionFilterInput_OID");// Filter from AFN
        String filterValue = (String) paramMap.get("FRCExpressionFilterInput_actualValue");// Filter from AFN
        String filterInput = (String) paramMap.get("FRCExpressionFilterInput");

        String objectId = (String) paramMap.get("objectId");
        short stopLevel = 0;// If All then expand All so stop level=0

        MapList res;

        ContextUtil.startTransaction(context, false);
        try {
            final StringList EXPD_BUS_SELECT_MBOM = new StringList(new String[] { "physicalid", "logicalid", "from[VPMInstance]" });
            res = getExpandPS(context, objectId, stopLevel, filterExpression, filterValue, filterInput, EXPD_REL_SELECT, EXPD_BUS_SELECT_MBOM);
            // PSS: START
            int skipLevel = 0;
            MapList cloneRes = (MapList) res.clone();
            for (int i = 0; i < cloneRes.size(); i++) {
                Map mapObj = (Map) cloneRes.get(i);
                String instanceName = (String) mapObj.get("attribute[PSS_PublishedEBOM.PSS_InstanceName]");
                String strLevelNow = (String) mapObj.get("level");
                int levelNow = Integer.parseInt(strLevelNow);
                if (skipLevel > 0 && levelNow > skipLevel) {
                    res.remove(mapObj);
                } else {
                    skipLevel = 0;
                }

                if (UIUtil.isNotNullAndNotEmpty(instanceName) && !instanceName.equals("EBOM")) {
                    skipLevel = levelNow;
                    res.remove(mapObj);
                }
            }
            // PSS: END
            startTime = System.currentTimeMillis();

            // START UM5c06 : Build Paths and save theses in the return maps + add "hasChildren=false" where needed

            // Store path in a Map to be able to manage unsorted return MapList
            HashMap<String, String> mapPaths = new HashMap<String, String>();
            HashMap<String, String> mapPathsLogical = new HashMap<String, String>();

            DomainObject objectDOM = new DomainObject(objectId);

            String rootPID = objectDOM.getInfo(context, "physicalid");
            mapPaths.put(rootPID, rootPID);

            String rootLID = objectDOM.getInfo(context, "logicalid");
            mapPathsLogical.put(rootPID, rootLID);

            // Declare variable before to improve prefs
            String objPID, objFromPID, objPIDConnection, objLID, objLIDConnection;
            String newPath = "";
            String newPathLogical = "";
            for (int i = 0; i < res.size(); i++) {
                Map mapObj = (Map) res.get(i);
                objPID = (String) mapObj.get("physicalid");
                objPIDConnection = (String) mapObj.get("physicalid[connection]");
                objLID = (String) mapObj.get("logicalid");
                objLIDConnection = (String) mapObj.get("logicalid[connection]");
                objFromPID = (String) mapObj.get("from.physicalid");
                newPath = mapPaths.get(objFromPID);
                newPathLogical = mapPathsLogical.get(objFromPID);
                if (newPath != null && !newPath.isEmpty()) {
                    newPath = newPath + "/" + objPIDConnection;
                    newPathLogical = newPathLogical + "/" + objLIDConnection;
                } else {
                    newPath = objPID;
                    newPathLogical = objLID;
                }
                mapPaths.put(objPID, newPath);
                mapPathsLogical.put(objPID, newPathLogical);

                mapObj.put("pathPID", newPath);
                mapObj.put("pathLID", newPathLogical);
                // set ids to use physicalids
                mapObj.put("id", objPID);
                mapObj.put("id[connection]", objPIDConnection);

                // Add hasChildren info here
                // TIGTK-9704 : Phase-2.0 : START
                StringList mbomDerivativeList = new StringList();
                mbomDerivativeList.add("VPMInstance");
                mapObj.put("hasChildren", EngineeringUtil.getHasChildren(mapObj, mbomDerivativeList));
                // TIGTK-9704 : Phase-2.0 : END
            }

            // END UM5c06 : Build Paths and save theses in the return maps

            // Sort by TreeOrder "attribute[PLMInstance.V_TreeOrder].value"
            res.sortStructure("attribute[PLMInstance.V_TreeOrder].value", "ascending", "real");

            ContextUtil.commitTransaction(context);
        } catch (Exception ex) {
            if (context.isTransactionActive()) {
                ContextUtil.abortTransaction(context);
            }
            throw ex;
        }
        long endTime = System.currentTimeMillis();
        logger.info("FRC PERFOS : getExpandProductStructure (without getVPMStructure) : ", (endTime - startTime));

        if (perfoTraces != null) {
            perfoTraces.write("Time spent in getExpandProductStructure() : " + (endTime - startTime) + " milliseconds");
            perfoTraces.newLine();
            perfoTraces.flush();
        }
        return res;
    }

    /**
     * Webform field access function
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static boolean isKindOfForWebFormForProperty(Context context, String[] args) throws Exception {
        boolean propertyForm = isProperties(context, args);
        if (!propertyForm) {
            return false;
        }

        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String typeStr = (String) programMap.get("type");
        Map<String, String> settings = (Map<String, String>) programMap.get("SETTINGS");
        String listTypesToCheckStr = settings.get("FRCTypesToCheck");
        String[] listTypesToCheck = null;
        if (listTypesToCheckStr == null)
            return false;
        else {
            listTypesToCheck = listTypesToCheckStr.split(",");
        }

        String type = DomainObject.EMPTY_STRING;
        if (UIUtil.isNullOrEmpty(typeStr)) {
            String objectId = (String) programMap.get("objectId");
            type = MqlUtil.mqlCommand(context, "print bus " + objectId + " select type dump |", false, false);
        } else {
            int indexEnd = typeStr.indexOf(",");
            if (typeStr.startsWith("_selectedType:")) {
                if (indexEnd > 0)
                    type = typeStr.substring(14, indexEnd);
                else
                    type = typeStr.substring(14);
            } else {
                if (indexEnd > 0)
                    type = typeStr.substring(0, indexEnd);
                else
                    // type = typeStr.substring(0);
                    type = typeStr;
            }
        }

        boolean returnValue = false;

        for (String typeToCheck : listTypesToCheck) {
            String ret = MqlUtil.mqlCommand(context, "print type " + type + " select kindof[" + typeToCheck + "] dump |", false, false);
            if (ret.equalsIgnoreCase("TRUE"))
                returnValue = true;
        }

        return returnValue;
    }

    /**
     * Webform field access function
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static boolean isKindOfForWebFormForInstanceProperty(Context context, String[] args) throws Exception {
        boolean propertyForm = isInstanceProperty(context, args);
        if (!propertyForm) {
            return false;
        }

        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String typeStr = (String) programMap.get("type");
        Map<String, String> settings = (Map<String, String>) programMap.get("SETTINGS");
        String listTypesToCheckStr = settings.get("FRCTypesToCheck");
        String strPolicyCheck = settings.get("FRCPolicyToCheck");
        String strAdminType = settings.get("Admin Type");
        String SYMBOLICPHANTOMLEVEL = FrameworkUtil.getAliasForAdmin(context, "attribute", TigerConstants.ATTRIBUTE_PSS_MANUFACTURING_INSTANCEEXT_PSS_PHANTOM_LEVEL, true);
        String SYMBOLICPHANTOMPART = FrameworkUtil.getAliasForAdmin(context, "attribute", TigerConstants.ATTRIBUTE_PSS_MANUFACTURING_INSTANCEEXT_PSS_PHANTOM_PART, true);
        String[] listTypesToCheck = null;
        if (listTypesToCheckStr == null)
            return false;
        else {
            listTypesToCheck = listTypesToCheckStr.split(",");
        }
        String type = DomainObject.EMPTY_STRING;
        String policy = DomainObject.EMPTY_STRING;
        if (UIUtil.isNullOrEmpty(typeStr)) {
            String objectId = (String) programMap.get("objectId");
            type = MqlUtil.mqlCommand(context, "print bus " + objectId + " select type dump |", false, false);
            policy = MqlUtil.mqlCommand(context, "print bus " + objectId + " select policy dump |", false, false);
        } else {
            int indexEnd = typeStr.indexOf(",");
            if (typeStr.startsWith("_selectedType:")) {
                if (indexEnd > 0)
                    type = typeStr.substring(14, indexEnd);
                else
                    type = typeStr.substring(14);
            } else {
                if (indexEnd > 0)
                    type = typeStr.substring(0, indexEnd);
                else
                    // type = typeStr.substring(0);
                    type = typeStr;
            }
        }

        boolean returnValue = false;

        for (String typeToCheck : listTypesToCheck) {
            String ret = MqlUtil.mqlCommand(context, "print type " + type + " select kindof[" + typeToCheck + "] dump |", false, false);
            if (strAdminType.equalsIgnoreCase(SYMBOLICPHANTOMLEVEL)) {
                if (ret.equalsIgnoreCase("TRUE")) {
                    if (strPolicyCheck.equalsIgnoreCase(policy)) {
                        returnValue = false;
                    } else {
                        returnValue = true;
                    }
                }
            } else if (strAdminType.equalsIgnoreCase(SYMBOLICPHANTOMPART)) {
                if (strPolicyCheck.equalsIgnoreCase(policy)) {
                    returnValue = true;
                } else if (ret.equalsIgnoreCase("TRUE"))
                    returnValue = true;
            }

        }
        return returnValue;
    }

    /**
     * Webform field access function
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public static boolean isInstanceProperty(Context context, String[] args) throws Exception {

        Map programMap = JPO.unpackArgs(args);
        String relId = (String) programMap.get("relId");
        if (UIUtil.isNotNullAndNotEmpty(relId)) {
            boolean propertyForm = isProperties(context, args);
            if (!propertyForm) {
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

    @SuppressWarnings("rawtypes")
    public String removeRsc(Context context, String[] args) { // Called from FRCOthersTableActions.jsp
        // Disconnect a capable resource from a Create Assembly
        String sRet = "";

        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            if (null != programMap) {
                String sId = (String) programMap.get("objectId");

                String[] aTableRowId = (String[]) programMap.get("emxTableRowId");

                if (null != aTableRowId && null != sId && !"".equals(sId)) {
                    for (int i = 0; i < aTableRowId.length; i++) {
                        String sRsc = aTableRowId[i];
                        String[] sRscArray = sRsc.split("\\|");
                        if (null != sRscArray && sRscArray.length >= 4) {
                            String rowIdStr[] = sRscArray[3].split(",");
                            if (rowIdStr.length > 2) {
                                DomainRelationship.disconnect(context, sRscArray[0]);
                            } else {
                                FRCMBOMModelerUtility.detachResourceFromMBOMReference(context, plmSession, sId, sRscArray[1]);
                            }
                        }
                    }
                }
            }
            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);
        } catch (Exception exp) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in removeRsc: ", exp);
            // TIGTK-5405 - 11-04-2017 - VB - END
            flushAndCloseSession(plmSession);
            ContextUtil.abortTransaction(context);
        }

        return sRet;
    }

    /**
     * This method is for updating column field in the table
     * @param context
     * @param args
     * @return
     * @throws Exception
     */

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Vector getPlantColumn(Context context, String[] args) throws Exception {
        PLMCoreModelerSession plmSession = null;
        context.setApplication("VPLM");
        plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
        plmSession.openSession();

        try {
            Vector vecResult = new Vector();
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");
            HashMap paramList = (HashMap) programMap.get("paramList");
            String strglobalView = (String) paramList.get("globalView");

            Iterator itrObjects = objectList.iterator();

            // Do for each object
            while (itrObjects.hasNext()) {
                Map mapObjectInfo = (Map) itrObjects.next();
                String objectId = (String) mapObjectInfo.get("id");
                String plantName = "";
                // TIGTK-10260:Start
                if (UIUtil.isNotNullAndNotEmpty(objectId)) {
                    StringBuffer sb = new StringBuffer();
                    String strMasterPlant = pss.mbom.MBOMUtil_mxJPO.getMasterPlant(context, objectId);

                    if (UIUtil.isNotNullAndNotEmpty(strMasterPlant)) {
                        DomainObject domPlant = new DomainObject(strMasterPlant);
                        plantName = domPlant.getInfo(context, DomainConstants.SELECT_NAME);
                    }

                    if (UIUtil.isNotNullAndNotEmpty(strglobalView) && "true".equals(strglobalView)) {
                        if (UIUtil.isNotNullAndNotEmpty(strMasterPlant)) {
                            sb.append("<b>");
                            sb.append(plantName);
                            sb.append("</b>");

                        }
                        List<String> lPlants = FRCMBOMModelerUtility.getPlantsAttachedToMBOMReference(context, plmSession, objectId);

                        if (!lPlants.isEmpty() && lPlants.contains(strMasterPlant)) {
                            lPlants.remove(strMasterPlant);
                        }

                        if (!lPlants.isEmpty() && !lPlants.contains(strMasterPlant)) {
                            sb.append("(");
                            for (int i = 0; i < lPlants.size(); i++) {
                                String strConsumerPlant = (String) lPlants.get(i);
                                DomainObject domConsumerPlant = DomainObject.newInstance(context, strConsumerPlant);
                                plantName = domConsumerPlant.getInfo(context, DomainConstants.SELECT_NAME);
                                sb.append(plantName);
                                sb.append(",");
                            }
                            sb.append(")");
                            if (sb.length() > 2)
                                sb.deleteCharAt(sb.length() - 2);
                        }
                        plantName = sb.toString();
                    }
                }
                vecResult.add(plantName);
                // TIGTK-10260:End
            }

            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);
            return vecResult;
        } catch (Exception exp) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getPlantColumn: ", exp);
            // TIGTK-5405 - 11-04-2017 - VB - END
            flushAndCloseSession(plmSession);
            throw exp;
        }
    }

    /**
     * This method is used to connect resource with Context MBOM
     * @param context
     * @param args
     * @throws Exception
     * @author mnaruni
     */
    @SuppressWarnings("rawtypes")
    public void attachResourcetoMBOM(Context context, String[] args) throws Exception {
        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, true);
            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            Map programMap = JPO.unpackArgs(args);

            String strMBOMtOID = (String) programMap.get("parentOID");
            String strRscOId = (String) programMap.get("strChildId");
            DomainObject domResource = DomainObject.newInstance(context, strRscOId);
            String strPhysicalId = domResource.getInfo(context, "physicalid");
            List<String> lResourceMBOM = PSS_FRCMBOMModelerUtility_mxJPO.getResourcesAttachedToMBOMReference(context, plmSession, strMBOMtOID);

            if (!(lResourceMBOM.contains(strPhysicalId))) {
                FRCMBOMModelerUtility.attachResourceToMBOMReference(context, plmSession, strMBOMtOID, strRscOId);
            }

            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in attachResourcetoMBOM: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            flushAndCloseSession(plmSession);
            ContextUtil.abortTransaction(context);
        }

    }

    /**
     * this method excludes the resource that is already connected with current MBOM
     * @param context
     * @param args
     * @return
     */
    @SuppressWarnings("rawtypes")
    public StringList excludeConnectedResource(Context context, String[] args) {
        StringList slExcludeList = new StringList();
        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, false);
            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();
            Map programMap = JPO.unpackArgs(args);

            String strMBOMObjectId = (String) programMap.get("objectId");
            List<String> lResourceMBOM = PSS_FRCMBOMModelerUtility_mxJPO.getResourcesAttachedToMBOMReference(context, plmSession, strMBOMObjectId);

            if (lResourceMBOM != null && !(lResourceMBOM.isEmpty())) {
                for (int i = 0; i < lResourceMBOM.size(); i++) {
                    DomainObject domResource = DomainObject.newInstance(context, (String) lResourceMBOM.get(i));
                    String strObjectId = domResource.getInfo(context, DomainConstants.SELECT_ID);
                    slExcludeList.add(strObjectId);
                }
            }
            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);

        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in excludeConnectedResource: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            flushAndCloseSession(plmSession);
        }
        return slExcludeList;
    }

    /**
     * This method excludes the Resource laready connected with parent resource(Equipment/Tooling)
     * @param context
     * @param args
     * @return
     */
    @SuppressWarnings("rawtypes")
    public StringList excludeEquipmentorTooling(Context context, String[] args) {

        final String RELATIONSHIP_RELATED_RESOURCE = PropertyUtil.getSchemaProperty(context, "relationship_PSS_RelatedResource");
        StringList slExcludeList = new StringList();
        try {
            Map programMap = JPO.unpackArgs(args);

            String strParentObjectId = (String) programMap.get("objectId");
            DomainObject domParent = DomainObject.newInstance(context, strParentObjectId);
            slExcludeList = domParent.getInfoList(context, "from[" + RELATIONSHIP_RELATED_RESOURCE + "].to.id");

        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in excludeEquipmentorTooling: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }

        return slExcludeList;
    }

    /**
     * This method checks whether the Published EBOM exists for current MBOM
     * @param context
     * @param args
     * @return
     */
    @SuppressWarnings("rawtypes")
    public String isPublishEBOMExists(Context context, String[] args) throws Exception {
        PLMCoreModelerSession plmSession = null;
        String result = "";
        List<String> slIdList = new ArrayList<String>();
        try {

            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();
            Map ProgramMap = JPO.unpackArgs(args);

            final String strMBOMObjectid = (String) ProgramMap.get("objectId");
            slIdList.add(strMBOMObjectid);
            List<String> psRefPIDList = FRCMBOMModelerUtility.getScopedPSReferencePIDFromList(context, plmSession, slIdList);

            if (psRefPIDList.contains("")) {

                result = "false";
            } else {
                result = "true";
            }
            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in isPublishEBOMExists: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            flushAndCloseSession(plmSession);
            ContextUtil.abortTransaction(context);
        }
        return result;

    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Map createOperationLineData(Context context, String[] args) throws Exception {
        final String ATTRIBUTE_V_NAME = PropertyUtil.getSchemaProperty(context, "attribute_PLMEntity.V_Name");
        final String ATTRIBUTE_V_EXTERNALID = PropertyUtil.getSchemaProperty(context, "attribute_PLMEntity.PLM_ExternalID");
        final String ATTRIBUTE_PLMINSTANCE_V_EFFECTIVITYCOMPILED_FORM = PropertyUtil.getSchemaProperty(context, "attribute_PLMInstance.V_EffectivityCompiledForm");
        final String POLICY_PSS_OPERATIONLINEDATA = PropertyUtil.getSchemaProperty(context, "policy_PSS_OperationLineData");
        final String RELATIONSHIP_DELFMIFUNCTIONIDENTIFIEDINSTANCE = PropertyUtil.getSchemaProperty(context, "relationship_DELFmiFunctionIdentifiedInstance");
        String strRelAttribute = DomainObject.EMPTY_STRING;
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String type = (String) programMap.get("TypeActual");
        String strMbomId = (String) programMap.get("objectId");
        String strHarmony = (String) programMap.get("Harmony");
        String strRelId = (String) programMap.get("relId");
        String strOperationNumber = (String) programMap.get("OperationNumber");
        String strLineNumber = (String) programMap.get("Line Data Number");
        String strCommand = DomainObject.EMPTY_STRING;
        HashMap<String, String> attributes = new HashMap<String, String>();
        Map returnMap = new HashMap();
        String strHarmonyFinalId = DomainObject.EMPTY_STRING;
        String strSelectedProductId = DomainObject.EMPTY_STRING;
        StringBuffer sb = new StringBuffer();

        if (UIUtil.isNotNullAndNotEmpty(strHarmony)) {
            if (strHarmony.contains("|")) {
                String[] strGetChoicesArray = strHarmony.split("\\|");
                strHarmonyFinalId = strGetChoicesArray[0];
                strSelectedProductId = strGetChoicesArray[1];
            } else {
                strHarmonyFinalId = strHarmony;
            }
        }
        PLMCoreModelerSession plmSession = null;

        try {
            ContextUtil.startTransaction(context, true);
            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();
            if (UIUtil.isNotNullAndNotEmpty(strRelId)) {
                DomainRelationship domRel = DomainRelationship.newInstance(context, strRelId);
                strRelAttribute = domRel.getAttributeValue(context, ATTRIBUTE_PLMINSTANCE_V_EFFECTIVITYCOMPILED_FORM);
            }

            String newObjID = PSS_FRCMBOMModelerUtility_mxJPO.createMBOMDiscreteReference(context, plmSession, type, TigerConstants.POLICY_PSS_MBOM, attributes);
            flushSession(plmSession);

            DomainObject domObj = new DomainObject(newObjID);
            String policy = domObj.getInfo(context, DomainConstants.SELECT_POLICY);
            String strName = domObj.getInfo(context, DomainConstants.SELECT_NAME);
            String strType = domObj.getInfo(context, DomainConstants.SELECT_TYPE);

            if (UIUtil.isNotNullAndNotEmpty(policy)) {
                domObj.setPolicy(context, POLICY_PSS_OPERATIONLINEDATA);
                String strRevision = "01.1";
                String strChangeString = "modify bus $1 revision $2 name $3;";
                MqlUtil.mqlCommand(context, strChangeString, newObjID, strRevision, strName);
            }
            String strVName = domObj.getAttributeValue(context, ATTRIBUTE_V_NAME);
            String strExternalPid = domObj.getAttributeValue(context, ATTRIBUTE_V_EXTERNALID);
            if (UIUtil.isNullOrEmpty(strVName) || UIUtil.isNullOrEmpty(strExternalPid)) {
                if (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_OPERATION)) {
                    String strAutoName = DomainObject.getAutoGeneratedName(context, "type_PSS_Operation", "");
                    domObj.setName(context, strAutoName);
                    domObj.setAttributeValue(context, ATTRIBUTE_V_EXTERNALID, strAutoName);
                    if (UIUtil.isNotNullAndNotEmpty(strHarmonyFinalId) && strHarmonyFinalId.equalsIgnoreCase("All")) {
                        domObj.setAttributeValue(context, ATTRIBUTE_V_NAME, strOperationNumber);
                    } else {
                        if (UIUtil.isNotNullAndNotEmpty(strSelectedProductId)) {
                            sb.append(strHarmonyFinalId);
                            sb.append("|");
                            sb.append(strSelectedProductId);
                        } else {
                            sb.append(strHarmonyFinalId);
                        }
                        domObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_OPERATION_PSS_HARMONY, sb.toString());
                    }
                }
                if (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_LINEDATA)) {
                    String strAutoName = DomainObject.getAutoGeneratedName(context, "type_PSS_LineData", "");
                    domObj.setName(context, strAutoName);
                    domObj.setAttributeValue(context, ATTRIBUTE_V_NAME, strLineNumber);
                    domObj.setAttributeValue(context, ATTRIBUTE_V_EXTERNALID, strAutoName);
                }
            }
            // TIGTK-10076,10077:Rutuja Ekatpure:22/9/2017:Start
            // strCommand = "modify bus " + newObjID + " add interface " + TigerConstants.INTERFACE_PSS_OPERATIONLINEDATA_EXT;
            // MqlUtil.mqlCommand(context, strCommand, false, false);
            // TIGTK-10076,10077:Rutuja Ekatpure:22/9/2017:End
            if (UIUtil.isNotNullAndNotEmpty(strMbomId)) {
                DomainObject domMBOM = new DomainObject(strMbomId);
                RelationshipType rel = new RelationshipType(RELATIONSHIP_DELFMIFUNCTIONIDENTIFIEDINSTANCE);
                DomainRelationship domainRelationship = DomainRelationship.connect(context, domMBOM, rel, domObj);
                domainRelationship.setAttributeValue(context, ATTRIBUTE_PLMINSTANCE_V_EFFECTIVITYCOMPILED_FORM, strRelAttribute);

            }
            returnMap.put("id", newObjID);
            returnMap.put("newObjectId", newObjID);

            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);

        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in createOperationLineData: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            flushAndCloseSession(plmSession);
            ContextUtil.abortTransaction(context);
        }

        return returnMap;
    }

    @SuppressWarnings("rawtypes")
    public static void setValue(Context context, String[] args) throws Exception {
        try {
            Map programMap = JPO.unpackArgs(args);
            Map mParamMap = (Map) programMap.get("paramMap");
            Map fieldMap = (Map) programMap.get("fieldMap");
            Map SettingMap = (Map) fieldMap.get("settings");
            String strSelectedValue = (String) mParamMap.get("New Value");
            String strOperationId = (String) mParamMap.get("objectId");
            String admin_Type = (String) SettingMap.get("Admin Type");
            String ALL = "All";
            DomainObject domOperation = new DomainObject(strOperationId);

            if (UIUtil.isNotNullAndNotEmpty(strSelectedValue) && !strSelectedValue.equalsIgnoreCase("All")) {

                if (admin_Type.equalsIgnoreCase("attribute_PSS_Operation.PSS_Harmony")) {
                    domOperation.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_OPERATION_PSS_HARMONY, strSelectedValue);
                } else if (admin_Type.equalsIgnoreCase("attribute_PSS_Operation.PSS_Color")) {
                    domOperation.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_OPERATION_PSS_COLOR, strSelectedValue);
                } else if (admin_Type.equalsIgnoreCase("attribute_PSS_Operation.PSS_VariantName")) {
                    domOperation.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_OPERATION_PSS_VARIANTNAME, strSelectedValue);
                }
            } else {
                if (admin_Type.equalsIgnoreCase("attribute_PSS_Operation.PSS_Harmony")) {
                    domOperation.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_OPERATION_PSS_HARMONY, ALL);
                } else if (admin_Type.equalsIgnoreCase("attribute_PSS_Operation.PSS_Color")) {
                    domOperation.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_OPERATION_PSS_COLOR, ALL);
                } else if (admin_Type.equalsIgnoreCase("attribute_PSS_Operation.PSS_VariantName")) {
                    domOperation.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_OPERATION_PSS_VARIANTNAME, ALL);
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in setValue: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
    }

    // Added for RFC055
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Vector getMBOMNameHTML(Context context, String[] args) throws Exception { // Called
        // from
        // table
        // FRCMBOMTable
        // (column
        // FRCMBOMCentral.MBOMTableColumnTitle)

        long startTime = System.currentTimeMillis();
        try {
            ContextUtil.startTransaction(context, false);

            // Create result vector
            Vector vecResult = new Vector();

            // Get object list information from packed arguments
            HashMap programMap = (HashMap) JPO.unpackArgs(args);

            // FRC START - HE5 : Added the part of code to fix the issue #267
            Map paramMap = (HashMap) programMap.get("paramList");
            boolean isexport = false;
            String export = (String) paramMap.get("exportFormat");
            if (export != null) {
                isexport = true;
            }
            // FRC END - HE5 : Added the part of code to fix the issue #267

            MapList objectList = (MapList) programMap.get("objectList");

            List<String> listIDs = new ArrayList<String>();

            // Do for each object
            for (Iterator itrObjects = objectList.iterator(); itrObjects.hasNext();) {
                Map mapObjectInfo = (Map) itrObjects.next();

                String objectID = (String) mapObjectInfo.get("id");
                listIDs.add(objectID);
            }

            StringList busSelect = new StringList();
            busSelect.add("attribute[PLMEntity.V_Name].value");
            MapList resultInfoML = DomainObject.getInfo(context, listIDs.toArray(new String[0]), busSelect);

            int index = 0;
            for (Iterator itrObjects = objectList.iterator(); itrObjects.hasNext();) {
                Map mapObjectInfo = (Map) itrObjects.next();
                String objectID = (String) mapObjectInfo.get("id");
                DomainObject domObjectId = DomainObject.newInstance(context, objectID);
                String strType = domObjectId.getInfo(context, DomainConstants.SELECT_TYPE);

                Map<String, String> resultInfoMap = (Map<String, String>) resultInfoML.get(index);
                index++;

                String objectType = (String) mapObjectInfo.get("type");
                // String objectDisplayStr = MqlUtil.mqlCommand(context, "print
                // bus " + objectID + " select attribute[PLMEntity.V_Name].value
                // dump ' '", false, false);

                String objectDisplayStr = resultInfoMap.get("attribute[PLMEntity.V_Name].value");

                StringBuffer resultSB = new StringBuffer();

                resultSB.append(genObjHTML(context, objectID, objectType, objectDisplayStr, false, false));
                // start

                String StrNewResult = "";
                if (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_OPERATION) || strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_LINEDATA)) {
                    StringList slSelectAttribute = new StringList(1);
                    slSelectAttribute.add("attribute[PSS_OperationLineDataExt.PSS_Dirty]");
                    Map mGetAttribute = domObjectId.getInfo(context, slSelectAttribute);
                    String strDirty = (String) mGetAttribute.get("attribute[PSS_OperationLineDataExt.PSS_Dirty]");

                    if (strDirty.equalsIgnoreCase("true")) {
                        String strNewImg = "\"../common/images/iconActionRotate.png\"/";
                        String strImgMbom = "";
                        java.util.regex.Pattern p = java.util.regex.Pattern.compile("src=([^>]+)");
                        java.util.regex.Matcher m = p.matcher(resultSB.toString());
                        while (m.find()) {
                            strImgMbom = m.group(1);
                        }
                        StrNewResult = resultSB.toString().replaceAll(strImgMbom, strNewImg);
                    }

                }

                // FRC START - HE5 : Added the part of code to fix the issue
                // #267
                if (isexport) {
                    vecResult.add(objectDisplayStr);
                } else {
                    if (StrNewResult.isEmpty() && StrNewResult.equals("")) {
                        vecResult.add(resultSB.toString());
                    } else {
                        vecResult.add(StrNewResult);
                    }
                    // vecResult.add(resultSB.toString());
                }
                // FRC END - HE5 : Added the part of code to fix the issue #267
            }

            ContextUtil.commitTransaction(context);

            long endTime = System.currentTimeMillis();
            logger.info("FRC PERFOS : getMBOMNameHTML : ", (endTime - startTime));

            if (perfoTraces != null) {
                perfoTraces.write("Time spent in getMBOMNameHTML() : " + (endTime - startTime) + " milliseconds");
                perfoTraces.newLine();
                perfoTraces.flush();
            }

            return vecResult;
        } catch (Exception exp) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getMBOMNameHTML: ", exp);
            // TIGTK-5405 - 11-04-2017 - VB - END
            if (context.isTransactionActive()) {
                ContextUtil.abortTransaction(context);
            }
            throw exp;
        }

    }

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public static String getListAuthorizedTypes(Context context, String[] args) throws Exception {

        StringBuffer returnSB = new StringBuffer("");
        final String TYPE_OPERATION = PropertyUtil.getSchemaProperty(context, "type_PSS_Operation");
        final String TYPE_LINE_DATA = PropertyUtil.getSchemaProperty(context, "type_PSS_LineData");

        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, false);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();
            String type = args[0];
            List<String> finalList = new ArrayList<String>();
            if (type.equalsIgnoreCase(TYPE_OPERATION) || type.equalsIgnoreCase(TYPE_LINE_DATA)) {
                finalList.add("");
            } else {
                String[] validTypes = { "CreateAssembly", "CreateKit", "CreateMaterial", "ProcessContinuousProvide", "ProcessContinuousCreateMaterial" };
                finalList = Arrays.asList(validTypes);
            }

            boolean firstElem = true;
            for (String childType : finalList) {
                if (firstElem)
                    firstElem = false;
                else
                    returnSB.append(",");
                returnSB.append(childType);
            }

            closeSession(plmSession);
            ContextUtil.commitTransaction(context);
        } catch (Exception exp) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getListAuthorizedTypes: ", exp);
            // TIGTK-5405 - 11-04-2017 - VB - END
            closeSession(plmSession);
            if (context.isTransactionActive()) {
                ContextUtil.abortTransaction(context);
            }
            throw exp;
        }

        return returnSB.toString();
    }

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public String createCostingReport(Context context, String[] args) throws Exception {

        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        final String strMbomId = (String) programMap.get("strMbomId");
        final String PCId = (String) programMap.get("strProductConfigurationId");
        FileOutputStream fileOut = null;
        final String MBOM_COSTING_REPORT = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.Label.Heading.MBOMCostingReport");
        final String MBOMEBOM_ATTRIBUTES = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.Label.Heading.MBOMEBOMAttribute");
        final String OPERATION_ATTRIBUTES = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.Label.Heading.OperationAttribute");
        final String LINEDATA_ATTRIBUTES = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.Label.Heading.LineDataAttribute");
        final String OTHER_ATTRIBUTES = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.Label.Heading.OtherAttribute");
        StringList slMBOMCheckList = new StringList();
        StringList slObjSelect = new StringList();
        slObjSelect.add(DomainConstants.SELECT_ID);
        slObjSelect.add(DomainConstants.SELECT_NAME);
        slObjSelect.add("physicalid");
        StringList slRelSelect = new StringList();
        slRelSelect.add(DomainConstants.SELECT_RELATIONSHIP_ID);

        StringList selectRelStmts = new StringList();
        selectRelStmts.addElement(DomainRelationship.SELECT_ID);
        selectRelStmts.add("attribute[PSS_ColorPID]");
        selectRelStmts.add("attribute[PSS_ProductConfigurationPID]");

        StringList slSelects = new StringList();
        slSelects.add(TigerConstants.SELECT_RELATIONSHIP_HARMONY_ASSOCIATION_TOID);
        slSelects.add(TigerConstants.SELECT_RELATIONSHIP_HARMONY_ASSOCIATION_ID);
        slSelects.add("frommid[" + TigerConstants.RELATIONSHIP_PSS_HARMONYASSOCIATION + "].to.name");

        PLMCoreModelerSession plmSession = null;
        boolean isTransactionActive = false;
        String strExcelPath1 = "";
        try {
            ContextUtil.startTransaction(context, true);
            isTransactionActive = true;

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            // Create blank workbook
            XSSFWorkbook workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet(MBOM_COSTING_REPORT);
            Font font_Black = workbook.createFont();
            font_Black.setColor(IndexedColors.BLACK.getIndex());
            Font font_White = workbook.createFont();
            font_White.setColor(IndexedColors.WHITE.getIndex());
            // Define cell style
            CellStyle style_Grey = workbook.createCellStyle();
            setGreyStyle(font_Black, style_Grey);
            // Define cell style
            CellStyle style_HeadingGrey = workbook.createCellStyle();
            setHeadingGreyStyle(font_Black, style_HeadingGrey);
            // Define cell style
            CellStyle style_HeadingOrange = workbook.createCellStyle();
            setHeadingOrangeStyle(font_White, style_HeadingOrange);
            // Define cell style
            CellStyle style_HeadingBlue = workbook.createCellStyle();
            setHeadingBlueStyle(font_White, style_HeadingBlue);
            // Define cell style
            CellStyle style_Headingyellow = workbook.createCellStyle();
            setHeadingYellowStyle(font_White, style_Headingyellow);
            // Define cell style
            CellStyle style_Orange = workbook.createCellStyle();
            setOrangeStyle(font_Black, style_Orange);
            // Define cell style
            CellStyle style_Blue = workbook.createCellStyle();
            setBlueStyle(font_Black, style_Blue);
            // Define cell style
            CellStyle style_Yellow = workbook.createCellStyle();
            setYellowStyle(font_Black, style_Yellow);
            // Define cell style
            CellStyle style_LightOrange = workbook.createCellStyle();
            setLightOrangeStyle(font_Black, style_LightOrange);
            // Define cell style
            CellStyle style_Lightyellow = workbook.createCellStyle();
            setLightYellowStyle(font_Black, style_Lightyellow);
            // Define cell style
            CellStyle style_LightBlue = workbook.createCellStyle();
            setLightBlueStyle(font_Black, style_LightBlue);
            // Define cell style
            CellStyle style_Titleyellow = workbook.createCellStyle();
            setTitleyellowStyle(font_White, style_Titleyellow);
            // Define cell style
            CellStyle style_TitleOrange = workbook.createCellStyle();
            setTitleOrangeStyle(font_White, style_TitleOrange);
            // Define cell style
            CellStyle style_TitleBlue = workbook.createCellStyle();
            setTitleBlueStyle(font_White, style_TitleBlue);

            // MBOM Attributes after refactoring
            String strMBOMAttributes = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentral", context.getLocale(), "PSS_FRCMBOMCentral.CostingReport.MBOMAttributes");

            StringList slMBOMAttributes = FrameworkUtil.split(strMBOMAttributes, "|");

            int nMBOMAttributeslength = slMBOMAttributes.size();
            StringList objectSelects = getMBOMAttributesForCostingReport(context, nMBOMAttributeslength, slMBOMAttributes);
            // EBOM Attributes after code Refactoring
            String strEBOMAttributes = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentral", context.getLocale(), "PSS_FRCMBOMCentral.CostingReport.EBOMAttributes");
            StringList slEBOMAttributes = FrameworkUtil.split(strEBOMAttributes, "|");

            int nEBOMAttributeslength = slEBOMAttributes.size();

            StringList slEBOMSelects = getEBOMAttributesCostingReport(context, nEBOMAttributeslength, slEBOMAttributes);
            // Material Attributes
            String strMaterialAttributes = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentral", context.getLocale(), "PSS_FRCMBOMCentral.CostingReport.MaterialAttributes");
            StringList slMaterialSelects = new StringList();
            StringList slMaterialAttributes = FrameworkUtil.split(strMaterialAttributes, "|");

            int nMaterialAttributeslength = slMaterialAttributes.size();

            for (int v = 0; v < nMaterialAttributeslength; v++) {
                String strMaterialAttributeName = (String) slMaterialAttributes.get(v);
                slMaterialSelects.add("attribute[" + strMaterialAttributeName + "]");
            }
            // Operation Attributes
            String strOperationAttributes = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentral", context.getLocale(), "PSS_FRCMBOMCentral.CostingReport.OperationAttributes");
            StringList slOperationSelects = new StringList();
            StringList slOperationAttributes = FrameworkUtil.split(strOperationAttributes, "|");

            int nOperationAttributeslength = slOperationAttributes.size();

            for (int t = 0; t < nOperationAttributeslength; t++) {
                String strOperationAttributeName = (String) slOperationAttributes.get(t);
                slOperationSelects.add("attribute[" + strOperationAttributeName + "]");
            }
            // Line Data Attributes
            String strLineDataAttributes = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentral", context.getLocale(), "PSS_FRCMBOMCentral.CostingReport.LineDataAttributes");
            StringList lineDataSelects = new StringList();
            StringList slLineDataAttributes = FrameworkUtil.split(strLineDataAttributes, "|");

            int nLineDataAttributeslength = slLineDataAttributes.size();
            for (int r = 0; r < nLineDataAttributeslength; r++) {
                String strLineAttributeName = (String) slLineDataAttributes.get(r);
                lineDataSelects.add("attribute[" + strLineAttributeName + "]");
            }
            // EQUI Attributes
            String strEQUIAttributes = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentral", context.getLocale(), "PSS_FRCMBOMCentral.CostingReport.EquipmentAttributes");
            StringList slEQUIAttributes = FrameworkUtil.split(strEQUIAttributes, "|");

            int nEQUIAttributeslength = slEQUIAttributes.size();
            StringList slEQUISelects = getEquipmentAtrributesForCostingReport(context, nEQUIAttributeslength, slEQUIAttributes);
            // Added for Tool attribute Start
            String strToolAttributes = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentral", context.getLocale(), "PSS_FRCMBOMCentral.CostingReport.ToolAttributes");
            StringList slToolSelects = new StringList();
            StringList slToolAttributes = FrameworkUtil.split(strToolAttributes, "|");
            int nToolAttributeslength = slToolAttributes.size();
            for (int v = 0; v < nToolAttributeslength; v++) {
                String strToolAttributeName = (String) slToolAttributes.get(v);
                slToolSelects.add("attribute[" + strToolAttributeName + "]");

            }
            // Added for Tool attribute End

            int TotalMBOMAttributes = nMBOMAttributeslength + 5;
            int TotalEBOMAttributes = nEBOMAttributeslength + 1;
            int TotalMaterialsAttributes = nMaterialAttributeslength;
            int TotalOperationsAttributes = nOperationAttributeslength + 1;
            int OrangeSectionAttributes = TotalMBOMAttributes + TotalEBOMAttributes + TotalMaterialsAttributes;
            int TotalLineDataAttributes = nLineDataAttributeslength;
            int TotalEquipmentAttributes = nEQUIAttributeslength;
            int TotalToolAttributes = nToolAttributeslength;

            int rowindex = 1;
            int colindex = 1;
            Row row1 = sheet.createRow(rowindex);
            Cell cell1 = row1.createCell(colindex);
            cell1.setCellValue(MBOM_COSTING_REPORT);
            cell1.setCellStyle(style_HeadingGrey);
            int TotalAttributes = TotalMBOMAttributes + TotalEBOMAttributes + TotalMaterialsAttributes + TotalOperationsAttributes + TotalLineDataAttributes + TotalEquipmentAttributes
                    + TotalToolAttributes + 2 + 3;
            for (int f = 2; f < TotalAttributes; f++) {
                Cell cellgrey = row1.createCell(f);
                cellgrey.setCellStyle(style_HeadingGrey);
            }

            sheet.addMergedRegion(new CellRangeAddress(rowindex, rowindex, colindex, TotalAttributes - 1));
            rowindex++;

            Row row2 = sheet.createRow(rowindex);

            Cell blankGreycell = row2.createCell(colindex);
            blankGreycell.setCellStyle(style_Grey);

            Cell cell2 = row2.createCell(colindex + 1);
            cell2.setCellValue(MBOMEBOM_ATTRIBUTES);
            cell2.setCellStyle(style_HeadingOrange);
            colindex++;
            int limitorange = colindex + OrangeSectionAttributes + 1;
            sheet.addMergedRegion(new CellRangeAddress(rowindex, rowindex, colindex, limitorange - 2));
            for (int f = 3; f < limitorange; f++) {
                Cell cellorange = row2.createCell(f);
                cellorange.setCellStyle(style_HeadingOrange);
                colindex++;
            }

            Row getrow2 = sheet.getRow(rowindex);
            Cell cell3 = getrow2.createCell(colindex);
            cell3.setCellValue(OPERATION_ATTRIBUTES);
            cell3.setCellStyle(style_HeadingBlue);
            colindex++;

            int limitblue = colindex + TotalOperationsAttributes - 1;
            sheet.addMergedRegion(new CellRangeAddress(rowindex, rowindex, colindex - 1, limitblue - 1));
            for (int f = colindex; f < limitblue; f++) {
                Cell cellblueheading = getrow2.createCell(f);
                cellblueheading.setCellStyle(style_HeadingBlue);
                colindex++;
            }

            Cell celllineHeading = getrow2.createCell(colindex);
            celllineHeading.setCellValue(LINEDATA_ATTRIBUTES);
            celllineHeading.setCellStyle(style_Headingyellow);
            colindex++;
            int limityellow = colindex + TotalLineDataAttributes - 1;
            sheet.addMergedRegion(new CellRangeAddress(rowindex, rowindex, colindex - 1, limityellow - 1));
            for (int f = colindex; f < limityellow; f++) {
                Cell cellyellowheading = getrow2.createCell(f);
                cellyellowheading.setCellStyle(style_Headingyellow);
                colindex++;
            }
            Cell cellotherHeading = getrow2.createCell(colindex);
            cellotherHeading.setCellValue(OTHER_ATTRIBUTES);
            cellotherHeading.setCellStyle(style_Headingyellow);
            colindex++;
            int limityellow1 = colindex + TotalEquipmentAttributes + TotalToolAttributes + 2;

            sheet.addMergedRegion(new CellRangeAddress(rowindex, rowindex, colindex - 1, limityellow1 - 1));
            for (int f = colindex; f < limityellow1; f++) {
                Cell cellyellowheading1 = getrow2.createCell(f);
                cellyellowheading1.setCellStyle(style_Headingyellow);
                colindex++;

            }
            rowindex++;
            // Column Headings
            String strColumnHeadings = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentral", context.getLocale(), "PSS_FRCMBOMCentral.CostingReport.ColumnHeading");
            StringList slColumnHeadings = FrameworkUtil.split(strColumnHeadings, "|");
            int nlength = slColumnHeadings.size();
            Row row3 = sheet.createRow(rowindex);
            rowindex++;
            int col = 1;
            for (int m = 0; m < nlength; m++) {
                Cell cell4 = row3.createCell(col);
                cell4.setCellValue((String) slColumnHeadings.get(m));
                col++;
                int limit = nlength - (TotalLineDataAttributes + TotalEquipmentAttributes + TotalToolAttributes + 4);
                if (m == 0) {
                    cell4.setCellStyle(style_HeadingGrey);
                } else if (m > 0 && m <= OrangeSectionAttributes) {
                    cell4.setCellStyle(style_TitleOrange);
                } else if (m > OrangeSectionAttributes && m <= limit) {
                    cell4.setCellStyle(style_TitleBlue);
                } else if (m > limit && m <= nlength) {
                    cell4.setCellStyle(style_Titleyellow);
                }
            }
            String productConfigurationPID = null;
            if (UIUtil.isNotNullAndNotEmpty(PCId) && !PCId.equalsIgnoreCase("undefined")) {
                /*
                 * BusinessObject bom = new BusinessObject(TigerConstants.TYPE_PRODUCTCONFIGURATION, PCName, "-", TigerConstants.VAULT_ESERVICEPRODUCTION); strProductConfiguration =
                 * bom.getObjectId(context);
                 */
                productConfigurationPID = MqlUtil.mqlCommand(context, "print bus " + PCId + " select physicalid dump |", false, false);

            }

            MapList mlFilteredList = PSS_FRCMBOMModelerUtility_mxJPO.getVPMStructureMQL(context, strMbomId, slObjSelect, slRelSelect, (short) 0, productConfigurationPID);

            DomainObject domObj = DomainObject.newInstance(context, strMbomId);
            String strParentType = domObj.getInfo(context, DomainConstants.SELECT_TYPE);
            String strParentPartNumber = domObj.getInfo(context, "attribute[PLMEntity.V_Name]");

            Map mapTopLevelMBOM = new HashMap();
            mapTopLevelMBOM.put("level", "0");
            mapTopLevelMBOM.put("physicalid", strMbomId);
            mapTopLevelMBOM.put("type", strParentType);

            MapList mlFinal = new MapList();
            mlFilteredList.add(0, mapTopLevelMBOM);
            int nConnectedObjectsSize = mlFilteredList.size();

            for (int i = 0; i < nConnectedObjectsSize; i++) {
                Map objMap = (Map) mlFilteredList.get(i);
                String strType = (String) objMap.get(DomainConstants.SELECT_TYPE);

                if (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_LINEDATA) || strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_OPERATION)) {
                    objMap.put("grouptext", "A");
                } else {
                    objMap.put("grouptext", "B");
                }
            }
            mlFilteredList.sortStructure("grouptext", "ascending", "string");
            Map mapParents = new HashMap();
            for (int i = 0; i < nConnectedObjectsSize; i++) {
                Map objMap = (Map) mlFilteredList.get(i);
                String strType = (String) objMap.get(DomainConstants.SELECT_TYPE);

                String strlevel = (String) objMap.get("level");
                int nlevel = Integer.parseInt(strlevel);
                // TIGTK-9254:Rutuja Ekatpure:23/8/2017:Start
                // if (!strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_LINEDATA)) {
                mlFinal.add(objMap);
                mapParents.put(nlevel, objMap);
                // } else {
                // Map parentMap = (Map) mapParents.get(nlevel - 1);
                // parentMap.put("LineData", objMap);
                // }
                // TIGTK-9254:Rutuja Ekatpure:23/8/2017:End
            }
            int mlFinalSize = mlFinal.size();

            for (int k = 0; k < mlFinalSize; k++) {
                Map objMap = (Map) mlFinal.get(k);
                String strObjId = (String) objMap.get("physicalid");
                String strType = (String) objMap.get(DomainConstants.SELECT_TYPE);
                String strlevel = (String) objMap.get("level");
                String strOutput = "0";
                strlevel = strOutput + strlevel;

                String strRelId = (String) objMap.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                DomainObject domObject = DomainObject.newInstance(context, strObjId);

                Row row = sheet.createRow(rowindex);
                int columnindex = 1;
                Cell levelcell = row.createCell(columnindex);
                levelcell.setCellValue(strlevel);
                levelcell.setCellStyle(style_Grey);
                columnindex++;
                // TIGTK-9254:Rutuja Ekatpure:23/8/2017:Start
                if (!(strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_OPERATION) || strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_LINEDATA))) {
                    // TIGTK-9254:Rutuja Ekatpure:23/8/2017:End
                    Map MBOMMap = domObject.getInfo(context, objectSelects);
                    // Code Refactoring:Start
                    // String strMBOMPhysicalId = MqlUtil.mqlCommand(context, "print bus " + strObjId + " select physicalid dump |", false, false);
                    String strMBOMPhysicalId = (String) objMap.get("physicalid");
                    // Code Refactoring:End

                    // MBOM Attributes
                    for (int p = 0; p < nMBOMAttributeslength; p++) {
                        String attr = "attribute[" + (String) slMBOMAttributes.get(p) + "]";
                        String attrName = (String) slMBOMAttributes.get(p);
                        if (attrName.equalsIgnoreCase("revision")) {
                            attr = "majorrevision";
                        } else if (attrName.equalsIgnoreCase("description")) {
                            attr = DomainConstants.SELECT_DESCRIPTION;
                        }
                        Cell cell = row.createCell(columnindex);
                        cell.setCellValue((String) MBOMMap.get(attr));
                        cell.setCellStyle(style_LightOrange);
                        columnindex++;
                    }
                    // Harmony
                    StringBuilder sbAllHarmonyNames = new StringBuilder();
                    StringBuilder sbAllColorNames = new StringBuilder();
                    StringList slColorList = new StringList();
                    if (UIUtil.isNotNullAndNotEmpty(productConfigurationPID)) {
                        if (strlevel.equals("00")) {
                            MapList mlHarmony = getHarmonyForCostingReport(context, domObject, productConfigurationPID, slObjSelect, selectRelStmts);

                            int nHarmonySize = mlHarmony.size();
                            for (int q = 0; q < nHarmonySize; q++) {
                                Map mapHarmony = (Map) mlHarmony.get(q);
                                String strHarmony = (String) mapHarmony.get(DomainConstants.SELECT_NAME);
                                sbAllHarmonyNames.append(strHarmony);
                                sbAllHarmonyNames.append(",");

                                String strRelColorPID = (String) mapHarmony.get("attribute[PSS_ColorPID]");
                                if (!UIUtil.isNullOrEmpty(strRelColorPID) && !strRelColorPID.equals("N/A") && !strRelColorPID.equalsIgnoreCase("Ignore")) {
                                    String strColorNameMql = "print bus " + strRelColorPID + " select name dump;";
                                    String strColorName = MqlUtil.mqlCommand(context, strColorNameMql, true, false);
                                    if (!slColorList.contains(strColorName)) {
                                        sbAllColorNames.append(strColorName);
                                        sbAllColorNames.append(",");
                                        slColorList.add(strColorName);
                                    }
                                }
                            }
                        } else {
                            MapList slConnectedHarmonyList = DomainRelationship.getInfo(context, new String[] { strRelId }, slSelects);
                            if (slConnectedHarmonyList != null && !slConnectedHarmonyList.isEmpty()) {
                                for (int y = 0; y < slConnectedHarmonyList.size(); y++) {
                                    Map mTempMap = (Map) slConnectedHarmonyList.get(y);
                                    if (mTempMap.containsKey("frommid[PSS_HarmonyAssociation].to.name")) {
                                        StringList slHarmonyNames = pss.mbom.MBOMUtil_mxJPO.getStringListValue(mTempMap.get("frommid[PSS_HarmonyAssociation].to.name"));
                                        StringList slAssociationId = pss.mbom.MBOMUtil_mxJPO.getStringListValue(mTempMap.get("frommid[PSS_HarmonyAssociation].id"));
                                        for (int b = 0; b < slAssociationId.size(); b++) {
                                            String strAssocitaionId = (String) slAssociationId.get(b);
                                            String strAssociationMql = "print connection " + strAssocitaionId + " select attribute[PSS_ProductConfigurationPID] attribute[PSS_ColorPID] dump |";
                                            String strAssociationResult = MqlUtil.mqlCommand(context, strAssociationMql, true, false);
                                            String[] strAssociationSplit = strAssociationResult.split("\\|");
                                            if (strAssociationSplit.length > 0) {
                                                String ProductConfPId = strAssociationSplit[0];
                                                if (ProductConfPId.equals(productConfigurationPID)) {
                                                    String strharmonyName = (String) slHarmonyNames.get(b);
                                                    sbAllHarmonyNames.append(strharmonyName);
                                                    sbAllHarmonyNames.append(",");
                                                    if (strAssociationSplit.length > 1) {
                                                        String strColorPID = strAssociationSplit[1];
                                                        if (!UIUtil.isNullOrEmpty(strColorPID) && !strColorPID.equals("N/A") && !strColorPID.equalsIgnoreCase("Ignore")) {
                                                            String strColorNameMql = "print bus " + strColorPID + " select name dump;";
                                                            String strColorName = MqlUtil.mqlCommand(context, strColorNameMql, true, false);
                                                            if (!UIUtil.isNullOrEmpty(strColorName) && !slColorList.contains(strColorName)) {
                                                                sbAllColorNames.append(strColorName);
                                                                sbAllColorNames.append(",");
                                                                slColorList.add(strColorName);
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    String strGetAllHarmonies = sbAllHarmonyNames.toString();
                    if (strGetAllHarmonies.indexOf(",") != -1) {
                        strGetAllHarmonies = strGetAllHarmonies.substring(0, strGetAllHarmonies.length() - 1);
                    }
                    String strGetAllColors = sbAllColorNames.toString();
                    if (strGetAllColors.indexOf(",") != -1) {
                        strGetAllColors = strGetAllColors.substring(0, strGetAllColors.length() - 1);
                    }
                    Cell cellHarmony = row.createCell(columnindex);
                    cellHarmony.setCellValue(strGetAllHarmonies);
                    cellHarmony.setCellStyle(style_LightOrange);
                    columnindex++;
                    Cell cellColor = row.createCell(columnindex);
                    cellColor.setCellValue(strGetAllColors);
                    cellColor.setCellStyle(style_LightOrange);
                    columnindex++;

                    // Variant Name
                    String strAllVariantNames = getVariantName(context, domObject, slObjSelect, productConfigurationPID);
                    Cell cellVariant = row.createCell(columnindex);
                    cellVariant.setCellValue(strAllVariantNames);
                    cellVariant.setCellStyle(style_LightOrange);
                    columnindex++;

                    // Plant
                    List<String> lPlantList = new ArrayList<String>();
                    String strAttachedPlant = pss.mbom.MBOMUtil_mxJPO.getMasterPlant(context, strMBOMPhysicalId);
                    if (UIUtil.isNotNullAndNotEmpty(strAttachedPlant)) {
                        lPlantList.add(strAttachedPlant);
                    }

                    int nPlantSize = lPlantList.size();
                    String strPlantNames = DomainObject.EMPTY_STRING;
                    String strPlantLocations = DomainObject.EMPTY_STRING;
                    StringBuilder sbPlantNames = new StringBuilder();
                    StringBuilder sbPlantLocations = new StringBuilder();
                    for (int v = 0; v < nPlantSize; v++) {
                        String strPlant = lPlantList.get(v);
                        String strPlantMQLResult = MqlUtil.mqlCommand(context, "print bus " + strPlant + " select name attribute[PSS_Location] dump |", false, false);
                        String[] strPlantSplit = strPlantMQLResult.split("\\|");
                        String strPlantName = strPlantSplit[0];
                        String strPlantLocation = "";
                        if (strPlantSplit.length > 1) {
                            strPlantLocation = strPlantSplit[1];
                        }
                        sbPlantNames.append(strPlantName);
                        sbPlantNames.append(",");
                        if (!UIUtil.isNullOrEmpty(strPlantLocation)) {
                            sbPlantLocations.append(strPlantLocation);
                            sbPlantLocations.append(",");
                        }
                    }
                    strPlantNames = sbPlantNames.toString();
                    if (strPlantNames.indexOf(",") != -1) {
                        strPlantNames = strPlantNames.substring(0, strPlantNames.length() - 1);
                    }

                    Cell cellPlant = row.createCell(columnindex);
                    cellPlant.setCellValue(strPlantNames);
                    cellPlant.setCellStyle(style_LightOrange);
                    columnindex++;

                    // Plant Location
                    strPlantLocations = sbPlantLocations.toString();
                    if (strPlantLocations.indexOf(",") != -1) {
                        strPlantLocations = strPlantLocations.substring(0, strPlantLocations.length() - 1);
                    }
                    Cell cellPlantLocation = row.createCell(columnindex);
                    cellPlantLocation.setCellValue(strPlantLocations);
                    cellPlantLocation.setCellStyle(style_LightOrange);
                    columnindex++;

                    // EBOM
                    List<String> lMBOMPhysicalId = new ArrayList();
                    lMBOMPhysicalId.add(strMBOMPhysicalId);
                    List<String> lPSPhysicalId = FRCMBOMModelerUtility.getScopedPSReferencePIDFromList(context, plmSession, lMBOMPhysicalId);

                    if (lPSPhysicalId != null && lPSPhysicalId.size() > 0) {
                        String strPSPhysicalId = lPSPhysicalId.get(0);
                        if (!UIUtil.isNullOrEmpty(strPSPhysicalId)) {
                            // Carry Over EBOM
                            String strCarryOver = MqlUtil.mqlCommand(context, "print bus " + strPSPhysicalId + " select to[VPMInstance].attribute[PSS_ManufacturingInstanceExt.PSS_CarryOver] dump |",
                                    false, false);
                            Cell carryOverCell = row.createCell(columnindex);
                            carryOverCell.setCellValue(strCarryOver);
                            carryOverCell.setCellStyle(style_LightOrange);
                            columnindex++;

                            DomainObject domPSObj = DomainObject.newInstance(context, strPSPhysicalId);
                            Map PSMap = domPSObj.getInfo(context, slEBOMSelects);
                            // EBOM Attributes
                            for (int w = 0; w < nEBOMAttributeslength; w++) {
                                String attr = "";
                                String EBOMValue = "";
                                String attrName = (String) slEBOMAttributes.get(w);
                                if (attrName.contains("~")) {
                                    StringList slEBOMAttributes1 = FrameworkUtil.split(attrName, "~");
                                    for (int g = 0; g < slEBOMAttributes1.size(); g++) {
                                        attr = (String) slEBOMAttributes1.get(g);
                                        EBOMValue = (String) PSMap.get(attr);
                                        if (!UIUtil.isNullOrEmpty(EBOMValue)) {
                                            break;
                                        }
                                    }
                                } else {
                                    if (attrName.equalsIgnoreCase("Part Family")) {
                                        attr = "to[Classified Item].from.name";
                                    } else {
                                        attr = "attribute[" + attrName + "]";
                                    }

                                    EBOMValue = (String) PSMap.get(attr);
                                }
                                Cell EBOMcell = row.createCell(columnindex);
                                EBOMcell.setCellValue(EBOMValue);
                                EBOMcell.setCellStyle(style_LightOrange);
                                columnindex++;
                            }
                        } else {
                            for (int w = 0; w < TotalEBOMAttributes; w++) {
                                Cell EBOMCellBlank = row.createCell(columnindex);
                                EBOMCellBlank.setCellStyle(style_Grey);
                                columnindex++;
                            }
                        }
                    }
                    // Material Attributes
                    if (TigerConstants.LIST_TYPE_MATERIALS.contains(strType)) {
                        Map materialMap = domObject.getInfo(context, slMaterialSelects);
                        for (int s = 0; s < nMaterialAttributeslength; s++) {
                            String attr = "attribute[" + (String) slMaterialAttributes.get(s) + "]";
                            Cell Materialcell = row.createCell(columnindex);
                            Materialcell.setCellValue((String) materialMap.get(attr));
                            Materialcell.setCellStyle(style_LightOrange);
                            columnindex++;
                        }
                    } else {
                        for (int s = 0; s < TotalMaterialsAttributes; s++) {
                            Cell MaterialCellBlank = row.createCell(columnindex);
                            MaterialCellBlank.setCellStyle(style_Grey);
                            columnindex++;
                        }
                    }

                    // Skip operation attributes for MBOM row
                    int limit = columnindex + TotalOperationsAttributes;
                    for (int s = columnindex; s < limit; s++) {
                        Cell OperationsCellBlank = row.createCell(s);
                        OperationsCellBlank.setCellStyle(style_Grey);
                        columnindex++;
                    }
                    // TIGTK-9254:Rutuja Ekatpure:23/8/2017:Start
                    // Skip Line Data
                    for (int s = 0; s < TotalLineDataAttributes; s++) {
                        Cell LineCellBlank = row.createCell(columnindex);
                        LineCellBlank.setCellStyle(style_Grey);
                        columnindex++;
                    }
                    // TIGTK-9254:Rutuja Ekatpure:23/8/2017:End
                    // Skip equipment attributes for MBOM row
                    int eqlimit = columnindex + TotalEquipmentAttributes;
                    for (int s = columnindex; s < eqlimit; s++) {
                        Cell EquiCellBlank = row.createCell(s);
                        EquiCellBlank.setCellStyle(style_Grey);
                        columnindex++;
                    }

                    // Skip equipment attributes for MBOM row
                    int toollimit = columnindex + TotalToolAttributes;
                    for (int s = columnindex; s < toollimit; s++) {
                        Cell ToolCellBlank = row.createCell(s);
                        ToolCellBlank.setCellStyle(style_Grey);
                        columnindex++;
                    }

                    // Other Attributes Code
                    MapList mlProgramProjectList = pss.mbom.MBOMUtil_mxJPO.getProgramFromMBOM(context, strMBOMPhysicalId);

                    int nProgramProjectList = mlProgramProjectList.size();
                    String strPPNames = DomainObject.EMPTY_STRING;
                    String strVehicleNames = DomainObject.EMPTY_STRING;
                    StringBuilder sbVehicleNames = new StringBuilder();
                    StringBuilder sbPPNames = new StringBuilder();
                    for (int h = 0; h < nProgramProjectList; h++) {
                        Map mProgramProjectMap = (Map) mlProgramProjectList.get(h);
                        String strPPId = (String) mProgramProjectMap.get(DomainConstants.SELECT_ID);
                        String strPPName = (String) mProgramProjectMap.get(DomainConstants.SELECT_NAME);
                        sbPPNames.append(strPPName);
                        sbPPNames.append(",");

                        DomainObject domObjectPP = DomainObject.newInstance(context, strPPId);
                        sbVehicleNames = getVehicleNameFromProgramProject(context, domObjectPP, slObjSelect);
                    }
                    strPPNames = sbPPNames.toString();
                    if (strPPNames.indexOf(",") != -1) {
                        strPPNames = strPPNames.substring(0, strPPNames.length() - 1);
                    }
                    strVehicleNames = sbVehicleNames.toString();
                    if (strVehicleNames.indexOf(",") != -1) {
                        strVehicleNames = strVehicleNames.substring(0, strVehicleNames.length() - 1);
                    }
                    Cell ProgramProjectcell = row.createCell(columnindex);
                    ProgramProjectcell.setCellValue(strPPNames);
                    ProgramProjectcell.setCellStyle(style_Lightyellow);
                    columnindex++;

                    Cell Vehiclecell = row.createCell(columnindex);
                    Vehiclecell.setCellValue(strVehicleNames);
                    Vehiclecell.setCellStyle(style_Lightyellow);
                    columnindex++;

                    // CR Added
                    String strCRNames = DomainObject.EMPTY_STRING;
                    StringBuilder sbCRNames = new StringBuilder();
                    MapList mlList = getMCOFromMBOMForCostingReport(context, strMBOMPhysicalId);
                    if (!mlList.isEmpty()) {
                        int nconnectedCR = mlList.size();

                        for (int d = 0; d < nconnectedCR; d++) {
                            Map mCRMap = (Map) mlList.get(d);
                            String strCRName = (String) mCRMap.get(DomainConstants.SELECT_NAME);
                            sbCRNames.append(strCRName);
                            sbCRNames.append(",");
                        }
                    }
                    strCRNames = sbCRNames.toString();
                    if (strCRNames.indexOf(",") != -1) {
                        strCRNames = strCRNames.substring(0, strCRNames.length() - 1);
                    }
                    Cell CRcell = row.createCell(columnindex);
                    CRcell.setCellValue(strCRNames);
                    CRcell.setCellStyle(style_Lightyellow);
                    columnindex++;

                    // Operations
                    int nlimit = 2 + TotalMBOMAttributes + TotalEBOMAttributes + TotalMaterialsAttributes;
                    for (int z = 0; z < nOperationAttributeslength + 1; z++) {
                        Cell AllBlankcell = row.createCell(nlimit);
                        AllBlankcell.setCellStyle(style_Grey);
                        nlimit++;
                    }

                    columnindex = nlimit + TotalLineDataAttributes;
                    int checklimit1 = TotalEquipmentAttributes + TotalToolAttributes;
                    for (int z = 0; z < checklimit1; z++) {
                        Cell AllBlankcell = row.createCell(columnindex);
                        AllBlankcell.setCellStyle(style_Grey);
                        columnindex++;
                    }
                    // TIGTK-9254:Rutuja Ekatpure:23/8/2017:Start
                } else if (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_OPERATION)) {
                    // TIGTK-9254:Rutuja Ekatpure:23/8/2017:End
                    String strOperationid = (String) objMap.get(DomainConstants.SELECT_ID);
                    String strOperationName = (String) objMap.get(DomainConstants.SELECT_NAME);
                    DomainObject domOperation = DomainObject.newInstance(context, strOperationid);
                    String strOperationlevel = (String) objMap.get("level");
                    strOperationlevel = strOutput + strOperationlevel;
                    columnindex = 2;

                    Map OperationMap = domOperation.getInfo(context, slOperationSelects);
                    row = sheet.createRow(rowindex);
                    columnindex = columnindex + TotalMBOMAttributes + TotalEBOMAttributes + TotalMaterialsAttributes;
                    for (int z = 1; z < columnindex; z++) {
                        Cell AllBlankcell = row.createCell(z);
                        AllBlankcell.setCellStyle(style_Grey);
                    }

                    columnindex = 1;
                    Cell Operationlevelcell1 = row.createCell(columnindex);
                    Operationlevelcell1.setCellValue(strOperationlevel);
                    Operationlevelcell1.setCellStyle(style_Grey);
                    columnindex++;

                    columnindex = columnindex + TotalMBOMAttributes + TotalEBOMAttributes + TotalMaterialsAttributes;

                    Cell OperationNamecell = row.createCell(columnindex);
                    OperationNamecell.setCellValue(strOperationName);
                    OperationNamecell.setCellStyle(style_LightBlue);
                    columnindex++;
                    for (int u = 0; u < nOperationAttributeslength; u++) {
                        String attr = "attribute[" + (String) slOperationAttributes.get(u) + "]";
                        Cell Operationcell = row.createCell(columnindex);
                        Operationcell.setCellValue((String) OperationMap.get(attr));
                        Operationcell.setCellStyle(style_LightBlue);
                        columnindex++;
                    }

                    // skip Line data elements after Operation
                    int limit1 = columnindex + TotalLineDataAttributes;
                    for (int z = columnindex; z < limit1; z++) {
                        Cell AllBlankcell = row.createCell(z);
                        AllBlankcell.setCellStyle(style_Grey);
                    }

                    // skip Line Data cells till Equipments
                    columnindex = columnindex + TotalLineDataAttributes;
                    // Other Attributes Code
                    // Equipment Attributes
                    // Code Refactoring:START
                    // String strOperationPhysicalId = MqlUtil.mqlCommand(context, "print bus " + strOperationid + " select physicalid dump |", false, false);
                    String strOperationPhysicalId = (String) objMap.get("physicalid");
                    // Code Refactoring:END
                    String strMBOMIdFromOperation = DomainObject.EMPTY_STRING;

                    List lResourceList = new StringList();
                    if (UIUtil.isNotNullAndNotEmpty(strOperationPhysicalId)) {
                        DomainObject domObjectOperation = DomainObject.newInstance(context, strOperationPhysicalId);
                        strMBOMIdFromOperation = (String) domObjectOperation.getInfo(context, TigerConstants.SELECT_RELATIONSHIP_DELFMIFUNCTIONIDENTIFIEDINSTANCE_FROMID);

                    }
                    if (!slMBOMCheckList.contains(strMBOMIdFromOperation)) {
                        lResourceList = PSS_FRCMBOMModelerUtility_mxJPO.getResourcesAttachedToMBOMReference(context, plmSession, strMBOMIdFromOperation);
                        slMBOMCheckList.add(strMBOMIdFromOperation);
                    }

                    if (!(lResourceList.isEmpty())) {
                        for (int j = 0; j < lResourceList.size(); j++) {
                            String strObjectId = (String) lResourceList.get(j);
                            String strResourcePolicy = MqlUtil.mqlCommand(context, "print bus " + strObjectId + " select policy dump |", false, false);

                            if ((strResourcePolicy).equalsIgnoreCase(TigerConstants.POLICY_PSS_EQUIPMENT)) {
                                DomainObject domObjectEquipment = DomainObject.newInstance(context, strObjectId);
                                Map equipmentMap = domObjectEquipment.getInfo(context, slEQUISelects);
                                Row equipmentRow = null;
                                if (j > 0) {
                                    rowindex++;
                                    equipmentRow = sheet.createRow(rowindex);
                                    columnindex = 2 + TotalMBOMAttributes + TotalEBOMAttributes + TotalMaterialsAttributes + TotalOperationsAttributes + TotalLineDataAttributes;

                                    for (int c = columnindex - 1; c > 0; c--) {
                                        Cell EquiBlankcell = equipmentRow.createCell(c);
                                        EquiBlankcell.setCellStyle(style_Grey);
                                    }
                                }
                                for (int s = 0; s < nEQUIAttributeslength; s++) {
                                    String attrName = (String) slEQUIAttributes.get(s);
                                    String attr = "";
                                    if (attrName.equalsIgnoreCase("description")) {
                                        attr = DomainConstants.SELECT_DESCRIPTION;
                                    } else {
                                        attr = "attribute[" + attrName + "]";
                                    }
                                    Cell Equipmentcell;
                                    if (j > 0) {
                                        Equipmentcell = equipmentRow.createCell(columnindex);
                                    } else {
                                        Equipmentcell = row.createCell(columnindex);
                                    }
                                    Equipmentcell.setCellValue((String) equipmentMap.get(attr));
                                    Equipmentcell.setCellStyle(style_Lightyellow);
                                    columnindex++;
                                }

                                // skip ULS
                                if (j > 0) {
                                    int limit3 = columnindex + TotalToolAttributes + 3;
                                    for (int c = columnindex; c < limit3; c++) {
                                        Cell ulsBlankcell;
                                        if (j > 0) {
                                            ulsBlankcell = equipmentRow.createCell(c);
                                        } else {
                                            ulsBlankcell = row.createCell(c);
                                        }
                                        ulsBlankcell.setCellStyle(style_Grey);
                                    }
                                }
                                // Added Start
                                else {
                                    int limit3 = columnindex + TotalToolAttributes + 3;
                                    for (int c = columnindex; c < limit3; c++) {
                                        Cell ulsBlankcell;
                                        ulsBlankcell = row.createCell(c);
                                        ulsBlankcell.setCellStyle(style_Grey);
                                    }
                                }
                            }
                            // Added for Tool Start

                            else if ((strResourcePolicy).equalsIgnoreCase(TigerConstants.POLICY_PSS_TOOL)) {
                                DomainObject domObjectTooling = DomainObject.newInstance(context, strObjectId);
                                Map ToolMap = domObjectTooling.getInfo(context, slToolSelects);
                                Row ToolRow = null;
                                if (j > 0) {
                                    rowindex++;
                                    ToolRow = sheet.createRow(rowindex);
                                    columnindex = 2 + TotalMBOMAttributes + TotalEBOMAttributes + TotalMaterialsAttributes + TotalOperationsAttributes + TotalLineDataAttributes
                                            + TotalEquipmentAttributes;
                                    for (int c = columnindex - 1; c > 0; c--) {
                                        Cell ToolBlankcell = ToolRow.createCell(c);
                                        ToolBlankcell.setCellStyle(style_Grey);
                                    }
                                } else {
                                    for (int c = 0; c < TotalEquipmentAttributes; c++) {
                                        Cell ToolBlankcell = row.createCell(columnindex);
                                        ToolBlankcell.setCellStyle(style_Grey);
                                        columnindex++;
                                    }
                                }
                                for (int s = 0; s < nToolAttributeslength; s++) {
                                    String attrName = (String) slToolAttributes.get(s);
                                    String attr = "";
                                    attr = "attribute[" + attrName + "]";
                                    Cell ToolBlankcell;
                                    if (j > 0) {
                                        ToolBlankcell = ToolRow.createCell(columnindex);
                                    } else {
                                        ToolBlankcell = row.createCell(columnindex);
                                    }
                                    ToolBlankcell.setCellValue((String) ToolMap.get(attr));
                                    ToolBlankcell.setCellStyle(style_Lightyellow);
                                    columnindex++;
                                }

                                // skip ULS
                                if (j > 0) {
                                    int limit3 = columnindex + 3;
                                    for (int c = columnindex; c < limit3; c++) {
                                        Cell ulsBlankcell;
                                        if (j > 0) {
                                            ulsBlankcell = ToolRow.createCell(c);
                                        } else {
                                            ulsBlankcell = row.createCell(c);
                                        }
                                        ulsBlankcell.setCellStyle(style_Grey);
                                    }
                                } else {
                                    int limit3 = columnindex + 3;
                                    for (int c = columnindex; c < limit3; c++) {
                                        Cell ulsBlankcell;
                                        ulsBlankcell = row.createCell(c);
                                        ulsBlankcell.setCellStyle(style_Grey);
                                    }

                                }
                            }
                        }
                    } else {
                        int limit = columnindex + TotalToolAttributes + TotalEquipmentAttributes + 3;
                        for (int c = columnindex; c < limit; c++) {
                            Cell ulsBlankcell;
                            ulsBlankcell = row.createCell(c);
                            ulsBlankcell.setCellStyle(style_Grey);
                        }
                    }
                } // line data
                  // TIGTK-9254:Rutuja Ekatpure:23/8/2017:Start
                else if (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_LINEDATA)) {
                    columnindex = columnindex + TotalMBOMAttributes + TotalEBOMAttributes + TotalMaterialsAttributes + TotalOperationsAttributes;
                    for (int z = 1; z < columnindex; z++) {
                        Cell AllBlankcell = row.createCell(z);
                        AllBlankcell.setCellStyle(style_Grey);
                        if (z == 1)
                            AllBlankcell.setCellValue(strlevel);
                    }

                    DomainObject domObjectLineData = DomainObject.newInstance(context, strObjId);

                    Map LineDataMap = domObjectLineData.getInfo(context, lineDataSelects);
                    for (int s = 0; s < nLineDataAttributeslength; s++) {
                        String attr = "attribute[" + (String) slLineDataAttributes.get(s) + "]";
                        Cell LineDatacell = row.createCell(columnindex);
                        LineDatacell.setCellValue((String) LineDataMap.get(attr));
                        LineDatacell.setCellStyle(style_Lightyellow);
                        columnindex++;
                    }

                    int limit3 = columnindex + TotalEquipmentAttributes + TotalToolAttributes + 3;
                    for (int c = columnindex; c < limit3; c++) {
                        Cell ulsBlankcell;
                        ulsBlankcell = row.createCell(c);
                        ulsBlankcell.setCellStyle(style_Grey);
                    }
                }
                // TIGTK-9254:Rutuja Ekatpure:23/8/2017:End
                rowindex++;
            }

            String strPath = context.createWorkspace();
            String strExcelFileName = DomainObject.EMPTY_STRING;
            if (UIUtil.isNotNullAndNotEmpty(PCId) && !PCId.equalsIgnoreCase("undefined")) {
                // directory path for new Excel file
                DomainObject domPC = DomainObject.newInstance(context, PCId);
                String strPCName = domPC.getInfo(context, DomainConstants.SELECT_NAME);
                strExcelFileName = "Costing Extract Report - " + strParentPartNumber + " - " + strPCName + ".xlsx";
            } else {
                strExcelFileName = "Costing Extract Report - " + strParentPartNumber + ".xlsx";
            }
            strExcelPath1 = strPath + File.separator + strExcelFileName;

            fileOut = new FileOutputStream(strExcelPath1);
            workbook.write(fileOut);
            // closeSession(plmSession);
            if (isTransactionActive) {
                ContextUtil.commitTransaction(context);
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in createCostingReport: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            if (isTransactionActive) {
                ContextUtil.abortTransaction(context);
            }
        } finally {
            if (fileOut != null)
                fileOut.close();
        }
        return strExcelPath1;
    }

    @SuppressWarnings("rawtypes")
    public void getUpdatedRevision(Context context, String[] args) throws Exception {

        String strObjectId = DomainObject.EMPTY_STRING;
        StringList slObjSelectStmts = new StringList();
        slObjSelectStmts.addElement(DomainConstants.SELECT_ID);
        StringList slRelSelectStmts = new StringList(1);
        slRelSelectStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
        try {
            strObjectId = args[0];
            String strNewObjectId = args[1];
            MapList mlObjectList = getRelatedData(context, strObjectId);

            if (!mlObjectList.isEmpty()) {

                Iterator itr = mlObjectList.iterator();
                PropertyUtil.setRPEValue(context, MBOMMAJORREVISION, strNewObjectId, true);
                String strCurrentUser = PropertyUtil.getRPEValue(context, MBOMREVISEACTION, false);
                while (itr.hasNext()) {
                    Map mGetValuesMap = (Map) itr.next();
                    String strOperationId = (String) mGetValuesMap.get(DomainConstants.SELECT_ID);
                    String strInstanceId = (String) mGetValuesMap.get(DomainRelationship.SELECT_ID);
                    String changeObjectName = DomainObject.EMPTY_STRING;
                    String Arguments[] = new String[10];
                    Arguments[0] = strInstanceId;
                    Arguments[1] = strOperationId;
                    Arguments[2] = changeObjectName;
                    Arguments[3] = strNewObjectId;
                    String strNewOperationId = replaceChangeNewRevisionManufItem(context, Arguments);

                    if (UIUtil.isNotNullAndNotEmpty(strCurrentUser)) {
                        if (UIUtil.isNotNullAndNotEmpty(strNewOperationId)) {
                            DomainObject dObj = DomainObject.newInstance(context, strNewOperationId);
                            dObj.setOwner(context, strCurrentUser);
                        }
                    }

                }
            }
        } catch (Exception exp) {

            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getUpdatedRevision: ", exp);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw exp;
        }
    }

    public static String replaceChangeNewRevisionManufItem(Context context, String[] args) throws Exception { // Called from FRCReplaceNewRevisionPostProcess.jsp
        String returnValue = DomainObject.EMPTY_STRING;
        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, true);
            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();
            String childRefPID = args[1];
            String changeObjectName = args[2];
            String strObjectId = args[3];
            String newChildRefPID = newRevisionMBOMReference(context, plmSession, childRefPID);
            returnValue = newChildRefPID;
            attachObjectToChange(context, plmSession, changeObjectName, newChildRefPID);

            if (UIUtil.isNotNullAndNotEmpty(strObjectId)) {
                DomainObject domOperation = DomainObject.newInstance(context, newChildRefPID);
                DomainObject domMBOM = DomainObject.newInstance(context, strObjectId);
                RelationshipType rel = new RelationshipType(TigerConstants.RELATIONSHIP_DELFMIFUNCTIONIDENTIFIEDINSTANCE);
                DomainRelationship.connect(context, domMBOM, rel, domOperation);
            }

            flushAndCloseSession(plmSession);
            // HE5 : Added fix for the error "Transaction not active" after calling the R&D API replaceMBOMInstance
            if (context.isTransactionActive())
                ContextUtil.commitTransaction(context);
            // ContextUtil.abortTransaction(context);
        } catch (Exception exp) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in replaceChangeNewRevisionManufItem: ", exp);
            // TIGTK-5405 - 11-04-2017 - VB - END
            flushAndCloseSession(plmSession);
            if (ContextUtil.isTransactionActive(context)) {
                ContextUtil.abortTransaction(context);
            }
            throw exp;
        }
        return returnValue;
    }

    /**
     * TIGTK-3499
     * @param context
     * @param args
     * @throws Exception
     */
    public static void replaceByAlternateMBOM(Context context, String[] args) throws Exception {
        PLMCoreModelerSession plmSession = null;
        PLMxRefInstanceEntity instanceEntity = null;
        String isAlternate = DomainObject.EMPTY_STRING;
        String objectId = DomainObject.EMPTY_STRING;
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            String mbomPath = args[0];
            String newRefPID = args[1];
            isAlternate = args[2];
            objectId = args[3];

            if (isAlternate.equalsIgnoreCase("true")) {
                if (UIUtil.isNotNullAndNotEmpty(objectId) && objectId.contains(".")) {
                    DomainObject mbomObj = DomainObject.newInstance(context, objectId);
                    objectId = mbomObj.getInfo(context, "physicalid");
                }
                List<String> mbomList = new ArrayList<String>();
                mbomList.add(objectId);
                boolean[] iComputeStatus = new boolean[1];
                iComputeStatus[0] = true;
                Map<String, List<String>> alternateMap = FRCMBOMModelerUtility.getAlternateReferences(context, plmSession, mbomList, iComputeStatus);

                if (alternateMap.size() > 0) {
                    // Connect the alternate with each other

                    List<String> alternateMbomList = alternateMap.get(objectId);
                    mbomList = new ArrayList<String>();
                    mbomList.add(newRefPID);

                    Map<String, List<String>> alternateMapCheck = FRCMBOMModelerUtility.getAlternateReferences(context, plmSession, mbomList, iComputeStatus);
                    List<String> alternateMbomListCheck = alternateMapCheck.get(newRefPID);
                    if (alternateMbomListCheck != null && !alternateMbomListCheck.contains(objectId)) {
                        FRCMBOMModelerUtility.createMfgProcessAlternate(context, plmSession, newRefPID, objectId);
                    }

                    for (String alternateMBOMId : alternateMbomList) {
                        // Connect the alternate with others
                        if (!newRefPID.equals(alternateMBOMId)) {

                            // mbomList = new ArrayList<String>();
                            // mbomList.add(newRefPID);

                            if (alternateMbomListCheck != null && !alternateMbomListCheck.contains(alternateMBOMId)) {
                                FRCMBOMModelerUtility.createMfgProcessAlternate(context, plmSession, newRefPID, alternateMBOMId);
                            }
                        } else {
                            // Remove alternate from current object
                            FRCMBOMModelerUtility.deleteMfgProcessAlternate(context, plmSession, objectId, alternateMBOMId);
                        }

                    }
                }
            }
            // Swap the alternate

            if (mbomPath.contains("/")) {
                String[] mbomPathList = mbomPath.split("/");

                if (!isAlternate.equalsIgnoreCase("true")) {

                    String strInstanceId = mbomPathList[mbomPathList.length - 1];
                    if (UIUtil.isNotNullAndNotEmpty(strInstanceId)) {
                        String strConnectionCommand = "print connection " + strInstanceId + " select from.id dump |";
                        String strFromConnectionId = MqlUtil.mqlCommand(context, strConnectionCommand, false, false);
                        DomainRelationship.disconnect(context, strInstanceId);
                        String newInstPID = createInstance(context, plmSession, strFromConnectionId, newRefPID);
                        DomainRelationship domNewRel = DomainRelationship.newInstance(context, newInstPID);
                        String strFromMBOMMasterPlant = pss.mbom.MBOMUtil_mxJPO.getMasterPlant(context, strFromConnectionId);
                        String strToMBOMMasterPlant = pss.mbom.MBOMUtil_mxJPO.getMasterPlant(context, newRefPID);
                        if (!strFromMBOMMasterPlant.equalsIgnoreCase(strToMBOMMasterPlant))
                            domNewRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_TYPE_OF_PART, "MakeBuy");
                        else
                            domNewRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_TYPE_OF_PART, "Make");
                    }

                } else {
                    FRCMBOMModelerUtility.replaceMBOMInstance(context, plmSession, mbomPathList[mbomPathList.length - 1], newRefPID);
                }
            }
            flushAndCloseSession(plmSession);
            // HE5 : Added fix for the error "Transaction not active" after calling the R&D API replaceMBOMInstance
            if (context.isTransactionActive())
                ContextUtil.commitTransaction(context);
            // ContextUtil.abortTransaction(context);
        } catch (Exception exp) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in replaceByAlternateMBOM: ", exp);
            // TIGTK-5405 - 11-04-2017 - VB - END
            flushAndCloseSession(plmSession);
            if (context.isTransactionActive())
                ContextUtil.abortTransaction(context);
            throw exp;
        }
    }

    /**
     * @HideOOTBChangeObjets
     * @param context
     * @param plmSession
     * @param changePID
     * @param objectPID
     * @throws Exception
     */
    public static void attachObjectToChange(Context context, PLMCoreModelerSession plmSession, String changePID, String objectPID) throws Exception {
        if (UIUtil.isNotNullAndNotEmpty(changePID) && UIUtil.isNotNullAndNotEmpty(objectPID)) {
            List<String> objectPIDList = new ArrayList<String>();
            objectPIDList.add(objectPID);
            if (!"-none-".equals(changePID)) {
                attachListObjectsToChange(context, plmSession, changePID, objectPIDList);
            }
        }
    }

    /**
     * @param context
     * @param objectID
     * @throws Exception
     *             TIGTK-3732 : 05/12/16
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public HashMap getUnitOfMeasures(Context context, String[] args) throws Exception {
        HashMap returnMap = new HashMap();
        StringList slChoicesDisp = new StringList();
        StringList slChoices = new StringList();
        final String PROPERTIES_KEY = "PSS_FRCMBOMCentral.UnitOfMeasure.Categorty.";
        try {
            Map programMap = JPO.unpackArgs(args);
            Map rMap = (Map) programMap.get("requestMap");
            String ObjectID = (String) rMap.get("objectId");
            DomainObject dom = DomainObject.newInstance(context, ObjectID);
            String strUnitOfMeasureCategory = dom.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_UNIT_OF_MEASURE_CATEGORY);
            if (UIUtil.isNotNullAndNotEmpty(strUnitOfMeasureCategory)) {
                String query = PROPERTIES_KEY + strUnitOfMeasureCategory;
                String strUnitOfMeasures = EnoviaResourceBundle.getProperty(context, TigerConstants.PROPERTIES_EMXFRCMBOMCENTRAL, context.getLocale(), query);
                StringList slUnitOfMeasuresList = FrameworkUtil.split(strUnitOfMeasures, "|");
                for (int i = 0; i < slUnitOfMeasuresList.size(); i++) {
                    String strGetValues = (String) slUnitOfMeasuresList.get(i);
                    slChoices.add(strGetValues);
                    slChoicesDisp.add(strGetValues);
                }
            }
            returnMap.put("field_choices", slChoices);
            returnMap.put("field_display_choices", slChoicesDisp);
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getUnitOfMeasures: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
        return returnMap;

    }

    /**
     * TIGTK-3583
     * @param context
     * @param args
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public void generateMassMBOMVariantAssembly(Context context, String[] args) throws Exception {
        Map programMap = JPO.unpackArgs(args);
        Map paramMap = (Map) programMap.get("paramMap");
        String objectId = (String) paramMap.get("objectId");
        String[] args1 = new String[2];
        args1[0] = objectId;
        args1[1] = "true";

        pss.mbom.StructureNodeUtil_mxJPO refObject = new pss.mbom.StructureNodeUtil_mxJPO();
        refObject.generateMassVariantAssemblies(context, args1);
    }

    public void propogateRelatedObjects(Context context, String[] args) throws Exception {
        PLMCoreModelerSession plmSession = null;
        try {

            // PSS ALM4253 fix START
            PropertyUtil.setRPEValue(context, PLANTFROMENOVIA, "true", false);
            PropertyUtil.setRPEValue(context, PROPOGRATEHARMONYASSOCIATION, "true", false);
            // PSS ALM4253 fix END

            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();
            String strObjectId = args[0];
            String strNewObjectId = args[1];
            if (UIUtil.isNotNullAndNotEmpty(strObjectId) && UIUtil.isNotNullAndNotEmpty(strNewObjectId)) {
                String strAttachedPlant = pss.mbom.MBOMUtil_mxJPO.getMasterPlant(context, strNewObjectId);
                if (UIUtil.isNullOrEmpty(strAttachedPlant)) {
                    List<String> listPlantIDs = FRCMBOMModelerUtility.getPlantsAttachedToMBOMReference(context, plmSession, strObjectId);
                    String strOldAttachedMasterPlant = pss.mbom.MBOMUtil_mxJPO.getMasterPlant(context, strObjectId);
                    if (!listPlantIDs.isEmpty()) {
                        int listSize = listPlantIDs.size();
                        for (int i = 0; i < listSize; i++) {
                            String strPlantPhysicalId = listPlantIDs.get(i);
                            PSS_FRCMBOMModelerUtility_mxJPO.attachPlantToMBOMReference(context, plmSession, strNewObjectId, strPlantPhysicalId);

                        }
                    }

                    StringList slMfgList = pss.mbom.MBOMUtil_mxJPO.getMfgProductionPlanning(context, strNewObjectId);
                    if (!slMfgList.isEmpty()) {
                        int listSize = slMfgList.size();
                        for (int i = 0; i < listSize; i++) {
                            String strMfgPlanningId = (String) slMfgList.get(i);
                            DomainObject dMfgProductionPlannigObj = DomainObject.newInstance(context, strMfgPlanningId);
                            String strNewPlant = (String) dMfgProductionPlannigObj.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_VOWNER + "].from.physicalid");
                            if (!strOldAttachedMasterPlant.equalsIgnoreCase(strNewPlant)) {
                                dMfgProductionPlannigObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGPLANTEXTPSS_OWNERSHIP,
                                        TigerConstants.ATTR_RANGE_PSS_MANUFACTURINGPLANTEXTPSS_OWNERSHIP_CONSUMER);
                            }

                        }
                    }
                }
                DomainObject domOldMBOM = DomainObject.newInstance(context, strObjectId);
                DomainObject domNewMBOM = DomainObject.newInstance(context, strNewObjectId);
                StringList slColorList = (StringList) domOldMBOM.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_COLORLIST + "].to.id");
                RelationshipType relType = new RelationshipType(TigerConstants.RELATIONSHIP_PSS_COLORLIST);
                if (!slColorList.isEmpty()) {
                    int slListSize = slColorList.size();
                    String[] colorList = new String[slListSize];
                    for (int i = 0; i < slListSize; i++) {
                        String strColorId = (String) slColorList.get(i);
                        colorList[i] = strColorId;
                    }
                    domNewMBOM.addRelatedObjects(context, relType, true, colorList);
                }
                String strCurrentUser = PropertyUtil.getRPEValue(context, MBOMREVISEACTION, false);
                if (UIUtil.isNotNullAndNotEmpty(strCurrentUser)) {
                    if (UIUtil.isNotNullAndNotEmpty(strNewObjectId)) {
                        DomainObject dObj = DomainObject.newInstance(context, strNewObjectId);
                        dObj.setOwner(context, strCurrentUser);
                    }
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in PropogateRelatedObjects: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    @SuppressWarnings("rawtypes")
    public void disconnectPreviousRevisions(Context context, String[] args) throws Exception {
        String strObjectId = args[0];
        String strId = DomainObject.EMPTY_STRING;
        StringList strObjectIdList = new StringList();
        boolean resetRPE = false;
        try {
            String strRPEValueId = PropertyUtil.getRPEValue(context, MBOMMAJORREVISION, true);
            if (UIUtil.isNotNullAndNotEmpty(strObjectId) && UIUtil.isNotNullAndNotEmpty(strRPEValueId)) {
                MapList mlObjectList = getRelatedData(context, strObjectId);
                if (!mlObjectList.isEmpty()) {
                    Iterator itr = mlObjectList.iterator();
                    while (itr.hasNext()) {
                        Map mGetValuesMap = (Map) itr.next();
                        strId = (String) mGetValuesMap.get(DomainConstants.SELECT_ID);
                        strObjectIdList.add(strId);
                    }
                }

                if (!strObjectIdList.isEmpty()) {
                    MapList mlNewObjectList = getRelatedData(context, strRPEValueId);
                    if (!mlNewObjectList.isEmpty()) {
                        Iterator itr = mlNewObjectList.iterator();
                        while (itr.hasNext()) {
                            Map mGetValuesMap = (Map) itr.next();
                            strId = (String) mGetValuesMap.get(DomainConstants.SELECT_ID);
                            if (strObjectIdList.contains(strId)) {
                                String strRelId = (String) mGetValuesMap.get(DomainRelationship.SELECT_ID);
                                DomainRelationship.disconnect(context, strRelId);
                            }
                        }
                    }
                    resetRPE = true;
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in disconnectPreviousRevisions: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        } finally {
            if (resetRPE) {
                PropertyUtil.setRPEValue(context, MBOMMAJORREVISION, DomainObject.EMPTY_STRING, true);
            }
        }
    }

    public MapList getRelatedData(Context context, String strObjectId) throws Exception {
        try {
            DomainObject domNewMBOM = DomainObject.newInstance(context, strObjectId);
            Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_OPERATION);
            typePattern.addPattern(TigerConstants.TYPE_PSS_LINEDATA);
            StringList slObjSelectStmts = new StringList(1);
            slObjSelectStmts.addElement(DomainConstants.SELECT_ID);
            StringList slRelSelectStmts = new StringList(1);
            slRelSelectStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            MapList mlObjectList = domNewMBOM.getRelatedObjects(context, TigerConstants.RELATIONSHIP_DELFMIFUNCTIONIDENTIFIEDINSTANCE, // Relationship
                    // Pattern
                    typePattern.getPattern(), // Object Pattern
                    slObjSelectStmts, // Object Selects
                    slRelSelectStmts, // Relationship Selects
                    false, // to direction
                    true, // from direction
                    (short) 0, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    null, // Post Type Pattern
                    null, null, null);

            return mlObjectList;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getRelatedData: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    @SuppressWarnings("rawtypes")
    public static String reorderMBOM(Context context, String[] args) throws Exception {// Called by FRCReorder.jsp
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String sRet = "";

        String mode = (String) programMap.get("mode");
        String relIdSource = (String) programMap.get("relIdSource");
        String relIdTarget = (String) programMap.get("relIdTarget");

        // Code to reorder the structure
        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            // Get Rels Infos
            DomainRelationship drSrc = DomainRelationship.newInstance(context, relIdSource);
            DomainRelationship drTrg = DomainRelationship.newInstance(context, relIdTarget);

            StringList relSel = new StringList();
            relSel.add("from.id");
            relSel.add("to.id");
            relSel.add("id[connection]");
            relSel.add("name[connection]");
            relSel.add("attribute[PLMInstance.V_TreeOrder].value"); // Not needed ????

            Map mapRelSrc = drSrc.getRelationshipData(context, relSel);
            Map mapRelTrg = drTrg.getRelationshipData(context, relSel);

            String idFromSrc = (String) ((StringList) mapRelSrc.get("from.id")).get(0);
            // String idToSrc = (String) ((StringList)mapRelSrc.get("to.id")).get(0);

            String idFromTrg = (String) ((StringList) mapRelTrg.get("from.id")).get(0);
            String idToTrg = (String) ((StringList) mapRelTrg.get("to.id")).get(0);

            String m1IdRelTrg = (String) ((StringList) mapRelTrg.get("id[connection]")).get(0);// To be sure to have a matrix Id in case we have a physicalid in input

            DomainObject domParent = null;
            DomainRelationship domRel = null;
            String instToReorderID = relIdSource;
            if ((idFromTrg.equals(idFromSrc) && (mode.equals("DropAbove") || mode.equals("DropBellow"))) || (idToTrg.equals(idFromSrc) && mode.equals("DropAsChild"))) {
                sRet = "reorder";

                // Same parent do a simple modification of V_TreeOrder
                domParent = new DomainObject(idFromSrc);
                domRel = drSrc;
            } else {
                // Call specific function of FRC for restructuration
                sRet = "move";
                String[] params = new String[2];
                params[0] = relIdSource;
                if (mode.equals("DropAsChild")) {
                    params[1] = idToTrg;
                    domParent = new DomainObject(idToTrg);
                } else {
                    params[1] = idFromTrg;
                    domParent = new DomainObject(idFromTrg);
                }
                String sNewInstPID = restructureInstance(context, plmSession, params);
                if (null != sNewInstPID && !"".equals(sNewInstPID)) {
                    domRel = DomainRelationship.newInstance(context, sNewInstPID);
                    instToReorderID = sNewInstPID;
                }
            }

            // Then compute V_TreeOrder
            if (null != domParent && null != domRel) {
                StringList slType = new StringList(new String[] { "id", "type" });
                StringList slRel = new StringList(new String[] { "id[connection]", "name[connection]", "attribute[PLMInstance.V_TreeOrder].value", "physicalid" });

                // Get childs 1 level down only
                MapList res = domParent.getRelatedObjects(context, "PLMInstance", "*", slType, slRel, false, true, (short) 1, "", "", 0);

                res.sortStructure("attribute[PLMInstance.V_TreeOrder].value", "ascending", "real");

                double newTreeOrder = -1.0;

                List<String> reoderInstPIDList = new ArrayList<String>();

                if (mode.equals("DropAsChild")) {
                } else {
                    for (int i = 0; i < res.size(); i++) {
                        Map mapChild = (Map) res.get(i);
                        String relChildId = (String) mapChild.get("id[connection]");
                        if (relChildId.equals(m1IdRelTrg)) {
                            if (mode.equals("DropBellow")) {
                                reoderInstPIDList.add(relChildId);
                                reoderInstPIDList.add(instToReorderID);
                                if (i < res.size() - 1)
                                    reoderInstPIDList.add((String) ((Map) res.get(i + 1)).get("id[connection]"));
                            } else { // Drop Above
                                if (i > 0) {
                                    if (!((String) ((Map) res.get(0)).get("physicalid")).equals(instToReorderID)) {
                                        reoderInstPIDList.add((String) ((Map) res.get(0)).get("id[connection]"));
                                    }
                                }
                                reoderInstPIDList.add(instToReorderID);
                                reoderInstPIDList.add(relChildId);
                            }
                            break;
                        }
                    }
                }

                // Construct the full Chil Instance List in the expected order
                List<String> reoderInstPIDFullList = new ArrayList<String>();

                for (Iterator itr = res.iterator(); itr.hasNext();) {
                    Map curMap = (Map) itr.next();

                    if (instToReorderID.equals((String) curMap.get("physicalid"))) {
                        reoderInstPIDFullList.add((String) curMap.get("physicalid"));
                    } else {
                        reoderInstPIDFullList.add((String) curMap.get("id[connection]"));
                    }
                }

                int pos = reoderInstPIDList.indexOf(instToReorderID);
                int sizelist = reoderInstPIDList.size();
                if (pos == 0) {
                    reoderInstPIDFullList.remove(instToReorderID);
                    reoderInstPIDFullList.add(0, instToReorderID);
                } else if (pos == (sizelist - 1)) {
                    reoderInstPIDFullList.remove(instToReorderID);
                    reoderInstPIDFullList.add(instToReorderID);
                } else {
                    reoderInstPIDFullList.remove(instToReorderID);
                    int posInsert = reoderInstPIDFullList.indexOf(reoderInstPIDList.get(0));
                    reoderInstPIDFullList.add(posInsert + 1, instToReorderID);
                }

                logger.info("newTreeOrder=", Double.toString(newTreeOrder));
                if (reoderInstPIDList.size() > 0) {
                    FRCMBOMModelerUtility.reorderMBOMInstance(context, plmSession, instToReorderID, reoderInstPIDFullList);
                } else if ("reorder".equals(sRet))
                    sRet = ""; // In case of reorder, nothing has been changed into the structure
            }

            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);
        } catch (Exception e) {
            sRet = "";
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in reorderMBOM: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            flushAndCloseSession(plmSession);
            if (context.isTransactionActive()) {
                ContextUtil.abortTransaction(context);
            }
            throw e;
        }
        return sRet;
    }

    /**
     * TIGTK-3721
     * @param context
     * @param args
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    // tanya-start
    public void modifyPLMEntityDescriptionMethod(Context context, String[] args) throws Exception {
        try {
            String strObjectId = args[0];
            String strNewAttrValue = args[1];
            DomainObject domObj = DomainObject.newInstance(context, strObjectId);
            // String BasicDescription = objDomain.getAttributeValue(context, PropertyUtil.getSchemaProperty(context,"description"));
            String strPLMEntityDesc = domObj.getAttributeValue(context, "PLMEntity.V_description");

            if (!strPLMEntityDesc.equalsIgnoreCase(strNewAttrValue)) {
                domObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PLMENTITY_V_DESCRIPTION, strNewAttrValue);
            }
        } catch (Exception e) {
            throw e;
        }
    }

    // tanya-end
    public void correctUoMAttributesOnHarmonies(Context context, String[] args) throws Exception {

        try {
            String strObjectId = args[0];
            String strNewAttributeValue = args[1];
            String strAttributeName = args[2];
            DomainObject domMBOM = DomainObject.newInstance(context, strObjectId);

            Pattern typePattern = new Pattern(TigerConstants.TYPE_CREATEASSEMBLY);
            typePattern.addPattern(TigerConstants.TYPE_CREATEKIT);
            typePattern.addPattern(TigerConstants.TYPE_CREATEMATERIAL);
            typePattern.addPattern(TigerConstants.TYPE_PROCESS_CONTINUOUS_CREATE_MATERIAL);
            typePattern.addPattern(TigerConstants.TYPE_PROCESSCONTINUOUSPROVIDE);

            String strPSSManufacturingItemExt_PSSHarmonies = domMBOM.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_HARMONIES_REFERENCE);
            if (UIUtil.isNotNullAndNotEmpty(strPSSManufacturingItemExt_PSSHarmonies)) {
                String strNewAttributeValues = getCorrectedData(strPSSManufacturingItemExt_PSSHarmonies, strAttributeName, strNewAttributeValue);
                domMBOM.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_HARMONIES_REFERENCE, strNewAttributeValues);
            }
            StringBuilder stringBuilder = new StringBuilder("attribute[");
            stringBuilder.append(TigerConstants.ATTRIBUTE_PSS_HARMONIES_INSTANCE);
            stringBuilder.append("].value != \"\"");
            final String WHERE_REL_CLAUSE = stringBuilder.toString();
            MapList mlMBOMObjectList = domMBOM.getRelatedObjects(context, TigerConstants.PATTERN_MBOM_INSTANCE, // Relationship Pattern
                    typePattern.getPattern(), // Object Pattern
                    null, // Object Selects
                    new StringList("physicalid"), // Relationship Selects
                    true, // to direction
                    false, // from direction
                    (short) 0, // recursion level
                    null, // object where clause
                    WHERE_REL_CLAUSE, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    null, // Post Type Pattern
                    null, null, null);
            if (!mlMBOMObjectList.isEmpty()) {
                int mlSize = mlMBOMObjectList.size();
                for (int i = 0; i < mlSize; i++) {
                    Map mRelMap = (Map) mlMBOMObjectList.get(i);
                    String strRelID = (String) mRelMap.get("physicalid");
                    DomainRelationship domRel = new DomainRelationship(strRelID);
                    String strPSSManufacturingInstanceExt_PSSHarmonies = domRel.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_HARMONIES_INSTANCE);
                    String strNewAttributeValues = getCorrectedData(strPSSManufacturingInstanceExt_PSSHarmonies, strAttributeName, strNewAttributeValue);
                    domRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_HARMONIES_INSTANCE, strNewAttributeValues);
                }
            }

        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in correctUoMAttributesOnHarmonies: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    /**
     * TIGTK-3721
     * @param strSplitValue
     * @param strAttributeName
     * @param strNewAttributeValue
     * @param strPSS_Harmonies
     * @return
     */
    public String getCorrectedData(String strAttrValue, String strAttributeName, String strNewAttributeValue) {
        try {
            String[] strSplitValue = strAttrValue.split("\\|");
            int arraySize = strSplitValue.length;

            for (int i = 0; i < arraySize; i++) {
                String[] itemStrArr = strSplitValue[i].split(",");
                int itemArrLength = itemStrArr.length;
                for (int j = 0; j < itemArrLength; j++) {
                    String[] strArray = itemStrArr[j].split(":");
                    StringBuilder sbBuilder = new StringBuilder("");
                    StringBuilder sbCheckAttributeName = new StringBuilder();
                    sbCheckAttributeName.append("PSS_ManufacturingUoMExt");
                    sbCheckAttributeName.append(".");
                    sbCheckAttributeName.append(strAttributeName);

                    if (strArray[0].equalsIgnoreCase(sbCheckAttributeName.toString())) {
                        strArray[1] = strNewAttributeValue;
                        sbBuilder.append(strArray[0]);
                        sbBuilder.append(":");
                        sbBuilder.append(strArray[1]);
                        itemStrArr[j] = sbBuilder.toString();
                    }
                }
                strSplitValue[i] = (String) FrameworkUtil.join(itemStrArr, ",");
            }
            return (String) FrameworkUtil.join(strSplitValue, "|");
        } catch (Exception e) {
            throw e;
        }
    }

    @SuppressWarnings("rawtypes")
    public StringList excludeOperationLineData(Context context, String[] args) throws Exception {
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String emxTableRowId = (String) programMap.get("emxTableRowId");
            String[] strSpiltArray = emxTableRowId.split("\\|");
            String strObjectId = strSpiltArray[1];
            String strObejctID = DomainObject.EMPTY_STRING;
            Pattern typePattern = new Pattern(TigerConstants.TYPE_CREATEASSEMBLY);
            typePattern.addPattern(TigerConstants.TYPE_CREATEKIT);
            typePattern.addPattern(TigerConstants.TYPE_CREATEMATERIAL);
            typePattern.addPattern(TigerConstants.TYPE_PROCESS_CONTINUOUS_CREATE_MATERIAL);
            typePattern.addPattern(TigerConstants.TYPE_PROCESSCONTINUOUSPROVIDE);
            StringList slReturnList = new StringList();
            if (UIUtil.isNotNullAndNotEmpty(strObjectId)) {
                DomainObject domMBOM = DomainObject.newInstance(context, strObjectId);
                strObejctID = domMBOM.getInfo(context, DomainConstants.SELECT_ID);
                MapList mlMBOMObjectList = domMBOM.getRelatedObjects(context, TigerConstants.PATTERN_MBOM_INSTANCE, // Relationship Pattern
                        typePattern.getPattern(), // Object Pattern
                        new StringList(DomainConstants.SELECT_ID), // Object Selects
                        null, // Relationship Selects
                        true, // to direction
                        true, // from direction
                        (short) 1, // recursion level
                        null, // object where clause
                        null, (short) 0, false, // checkHidden
                        true, // preventDuplicates
                        (short) 1000, // pageSize
                        null, // Post Type Pattern
                        null, null, null);
                if (!mlMBOMObjectList.isEmpty()) {
                    int nObjectSize = mlMBOMObjectList.size();
                    for (int i = 0; i < nObjectSize; i++) {
                        Map mObjectMap = (Map) mlMBOMObjectList.get(i);
                        String strObjectIds = (String) mObjectMap.get(DomainConstants.SELECT_ID);
                        slReturnList.add(strObjectIds);

                    }
                }
            }
            slReturnList.add(strObejctID);
            return slReturnList;
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * TIGTK-4421
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public static String getDescriptionForMBOM(Context context, String[] args) throws Exception {
        String strPartDescription = "";
        try {
            Map programMap = JPO.unpackArgs(args);
            Map mrequestMap = (Map) programMap.get("requestMap");

            String strPartId = (String) mrequestMap.get("objectId");
            if (UIUtil.isNotNullAndNotEmpty(strPartId)) {
                DomainObject dObj = DomainObject.newInstance(context, strPartId);
                dObj.getInfo(context, DomainConstants.SELECT_DESCRIPTION);
                strPartDescription = MqlUtil.mqlCommand(context, "print bus " + strPartId + " select description dump |", false, false);
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getDescriptionForMBOM: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
        return strPartDescription;
    }

    /**
     * TIGTK-4421
     * @param context
     * @param args
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public static void setDescriptionForMBOM(Context context, String[] args) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        Map paramMap = (Map) programMap.get("paramMap");
        try {
            String strObjectId = (String) paramMap.get("objectId");
            if (UIUtil.isNotNullAndNotEmpty(strObjectId)) {
                String newValue = (String) paramMap.get("New Value");
                DomainObject dObj = DomainObject.newInstance(context, strObjectId);
                dObj.setDescription(context, newValue);
            }
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * TIGTK-4683
     * @param context
     * @param parentPID
     * @param childPID
     * @return
     * @throws Exception
     */
    public static String getInstanceToReuse(Context context, String parentPID, String childPID, String treeOrder, String externalID, String vName) throws Exception {

        Pattern typePattern = new Pattern(TigerConstants.TYPE_CREATEASSEMBLY);
        typePattern.addPattern(TigerConstants.TYPE_CREATEKIT);
        typePattern.addPattern(TigerConstants.TYPE_CREATEMATERIAL);
        typePattern.addPattern(TigerConstants.TYPE_PROCESS_CONTINUOUS_CREATE_MATERIAL);
        typePattern.addPattern(TigerConstants.TYPE_PROCESSCONTINUOUSPROVIDE);

        StringList slObjSelectStmts = new StringList();
        slObjSelectStmts.addElement(TigerConstants.SELECT_PHYSICALID);

        StringList slRelSelectStmts = new StringList(1);
        slRelSelectStmts.addElement("physicalid[connection]");

        DomainObject dParentObj = new DomainObject(parentPID);
        StringBuilder stringBuilder = new StringBuilder("to.physicalid==\"");
        stringBuilder.append(childPID);
        stringBuilder.append("\" AND attribute[PLMInstance.V_TreeOrder].value==\"");
        stringBuilder.append(treeOrder);
        stringBuilder.append("\" AND attribute[PLMInstance.V_Name].value==\"");
        stringBuilder.append(vName);
        stringBuilder.append("\" AND attribute[PLMInstance.PLM_ExternalID].value==\"");
        stringBuilder.append(externalID);
        stringBuilder.append("\"");

        String WHERE_REL_CLAUSE = stringBuilder.toString();
        MapList mlMBOMObjectList = dParentObj.getRelatedObjects(context, TigerConstants.PATTERN_MBOM_INSTANCE, // Relationship Pattern
                typePattern.getPattern(), // Object Pattern
                slObjSelectStmts, // Object Selects
                slRelSelectStmts, // Relationship Selects
                false, // to direction
                true, // from direction
                (short) 1, // recursion level
                null, // object where clause
                WHERE_REL_CLAUSE, (short) 0, false, // checkHidden
                true, // preventDuplicates
                (short) 1000, // pageSize
                null, // Post Type Pattern
                null, null, null);
        String strMBOMRelPID = null;
        if (mlMBOMObjectList.size() > 0) {
            Map mMBOMObjectMap = (Map) mlMBOMObjectList.get(0);
            strMBOMRelPID = (String) mMBOMObjectMap.get("physicalid[connection]");
            return strMBOMRelPID;
        }

        return null;
    }

    /**
     * TIGTK-5478
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public static String reviseManufacturingItem(Context context, String args[]) throws Exception {
        String strObjectId = DomainObject.EMPTY_STRING;
        try {
            String strCurrentUser = context.getUser();
            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), "", "");
            PropertyUtil.setRPEValue(context, MBOMREVISEACTION, strCurrentUser, false);
            strObjectId = replaceNewRevisionManufItem(context, args);
            if (UIUtil.isNotNullAndNotEmpty(strObjectId)) {
                DomainObject dObj = DomainObject.newInstance(context, strObjectId);
                dObj.setOwner(context, strCurrentUser);
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in reviseManufacturingItem: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        } finally {
            ContextUtil.popContext(context);
            PropertyUtil.setRPEValue(context, MBOMREVISEACTION, DomainObject.EMPTY_STRING, false);
        }
        return strObjectId;
    }

    public static StringList getEBOMAttributesCostingReport(Context context, int nEBOMAttributeslength, StringList slEBOMAttributes) throws Exception {
        // EBOM Attributes
        StringList slEBOMSelects = new StringList();
        for (int v = 0; v < nEBOMAttributeslength; v++) {
            String strEBOMAttributeName = (String) slEBOMAttributes.get(v);
            if (strEBOMAttributeName.contains("~")) {
                StringList slEBOMAttributes1 = FrameworkUtil.split(strEBOMAttributeName, "~");
                for (int g = 0; g < slEBOMAttributes1.size(); g++) {
                    slEBOMSelects.add("attribute[" + (String) slEBOMAttributes1.get(g) + "]");
                }
            } else {
                if (strEBOMAttributeName.equalsIgnoreCase("Part Family")) {
                    slEBOMSelects.add("to[Classified Item].from.name");
                } else {
                    slEBOMSelects.add("attribute[" + strEBOMAttributeName + "]");
                }
            }
        }
        return slEBOMSelects;
    }

    public static StringList getMBOMAttributesForCostingReport(Context context, int nMBOMAttributeslength, StringList slMBOMAttributes) throws Exception {
        StringList slMBOMSelects = new StringList();
        for (int n = 0; n < nMBOMAttributeslength; n++) {
            String strAttributeName = (String) slMBOMAttributes.get(n);
            if (strAttributeName.equalsIgnoreCase("revision")) {
                slMBOMSelects.add("majorrevision");
            } else if (strAttributeName.equalsIgnoreCase("description")) {
                slMBOMSelects.add(DomainConstants.SELECT_DESCRIPTION);
            } else {
                slMBOMSelects.add("attribute[" + strAttributeName + "]");
            }
        }
        return slMBOMSelects;
    }

    public static void setGreyStyle(Font font_Black, CellStyle style_Grey) throws Exception {

        style_Grey.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style_Grey.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style_Grey.setFont(font_Black);
        style_Grey.setBorderBottom(XSSFCellStyle.BORDER_THIN);
        style_Grey.setBorderRight(XSSFCellStyle.BORDER_THIN);

    }

    public static void setHeadingGreyStyle(Font font_Black, CellStyle style_HeadingGrey) throws Exception {

        style_HeadingGrey.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style_HeadingGrey.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style_HeadingGrey.setFont(font_Black);
        style_HeadingGrey.setBorderBottom(XSSFCellStyle.BORDER_THIN);
        style_HeadingGrey.setBorderRight(XSSFCellStyle.BORDER_THIN);
        style_HeadingGrey.setAlignment(CellStyle.ALIGN_CENTER);

    }

    public static void setHeadingOrangeStyle(Font font_White, CellStyle style_HeadingOrange) throws Exception {
        style_HeadingOrange.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
        style_HeadingOrange.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style_HeadingOrange.setFont(font_White);
        style_HeadingOrange.setBorderBottom(XSSFCellStyle.BORDER_THIN);
        style_HeadingOrange.setBorderRight(XSSFCellStyle.BORDER_THIN);
        style_HeadingOrange.setAlignment(CellStyle.ALIGN_CENTER);
    }

    public static void setHeadingBlueStyle(Font font_White, CellStyle style_HeadingBlue) throws Exception {
        style_HeadingBlue.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        style_HeadingBlue.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style_HeadingBlue.setFont(font_White);
        style_HeadingBlue.setBorderBottom(XSSFCellStyle.BORDER_THIN);
        style_HeadingBlue.setBorderRight(XSSFCellStyle.BORDER_THIN);
        style_HeadingBlue.setAlignment(CellStyle.ALIGN_CENTER);
    }

    public static void setHeadingYellowStyle(Font font_White, CellStyle style_Headingyellow) throws Exception {
        style_Headingyellow.setFillForegroundColor(IndexedColors.GOLD.getIndex());
        style_Headingyellow.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style_Headingyellow.setFont(font_White);
        style_Headingyellow.setBorderBottom(XSSFCellStyle.BORDER_THIN);
        style_Headingyellow.setBorderRight(XSSFCellStyle.BORDER_THIN);
        style_Headingyellow.setAlignment(CellStyle.ALIGN_CENTER);
    }

    public static void setOrangeStyle(Font font_Black, CellStyle style_Orange) throws Exception {
        style_Orange.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
        style_Orange.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style_Orange.setFont(font_Black);
        style_Orange.setBorderBottom(XSSFCellStyle.BORDER_THIN);
        style_Orange.setBorderRight(XSSFCellStyle.BORDER_THIN);
    }

    public static void setBlueStyle(Font font_Black, CellStyle style_Blue) throws Exception {
        style_Blue.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        style_Blue.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style_Blue.setFont(font_Black);
        style_Blue.setBorderBottom(XSSFCellStyle.BORDER_THIN);
        style_Blue.setBorderRight(XSSFCellStyle.BORDER_THIN);
    }

    public static void setYellowStyle(Font font_Black, CellStyle style_Yellow) throws Exception {
        style_Yellow.setFillForegroundColor(IndexedColors.GOLD.getIndex());
        style_Yellow.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style_Yellow.setFont(font_Black);
        style_Yellow.setBorderBottom(XSSFCellStyle.BORDER_THIN);
        style_Yellow.setBorderRight(XSSFCellStyle.BORDER_THIN);
    }

    public static void setLightOrangeStyle(Font font_Black, CellStyle style_LightOrange) throws Exception {
        style_LightOrange.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
        style_LightOrange.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style_LightOrange.setFont(font_Black);
        style_LightOrange.setBorderBottom(XSSFCellStyle.BORDER_THIN);
        style_LightOrange.setBorderRight(XSSFCellStyle.BORDER_THIN);

    }

    public static void setLightYellowStyle(Font font_Black, CellStyle style_Lightyellow) throws Exception {
        style_Lightyellow.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.getIndex());
        style_Lightyellow.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style_Lightyellow.setFont(font_Black);
        style_Lightyellow.setBorderBottom(XSSFCellStyle.BORDER_THIN);
        style_Lightyellow.setBorderRight(XSSFCellStyle.BORDER_THIN);
    }

    public static void setLightBlueStyle(Font font_Black, CellStyle style_LightBlue) throws Exception {
        style_LightBlue.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
        style_LightBlue.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style_LightBlue.setFont(font_Black);
        style_LightBlue.setBorderBottom(XSSFCellStyle.BORDER_THIN);
        style_LightBlue.setBorderRight(XSSFCellStyle.BORDER_THIN);
    }

    public static void setTitleyellowStyle(Font font_White, CellStyle style_Titleyellow) throws Exception {
        style_Titleyellow.setFillForegroundColor(IndexedColors.GOLD.getIndex());
        style_Titleyellow.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style_Titleyellow.setFont(font_White);
        style_Titleyellow.setBorderBottom(XSSFCellStyle.BORDER_THIN);
        style_Titleyellow.setBorderRight(XSSFCellStyle.BORDER_THIN);
    }

    public static void setTitleOrangeStyle(Font font_White, CellStyle style_TitleOrange) throws Exception {
        style_TitleOrange.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
        style_TitleOrange.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style_TitleOrange.setFont(font_White);
        style_TitleOrange.setBorderBottom(XSSFCellStyle.BORDER_THIN);
        style_TitleOrange.setBorderRight(XSSFCellStyle.BORDER_THIN);
    }

    public static void setTitleBlueStyle(Font font_White, CellStyle style_TitleBlue) throws Exception {
        style_TitleBlue.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        style_TitleBlue.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style_TitleBlue.setFont(font_White);
        style_TitleBlue.setBorderBottom(XSSFCellStyle.BORDER_THIN);
        style_TitleBlue.setBorderRight(XSSFCellStyle.BORDER_THIN);
    }

    @SuppressWarnings("rawtypes")
    public static StringBuilder getVehicleNameFromProgramProject(Context context, DomainObject domObjectPP, StringList slObjSelect) throws Exception {
        StringBuilder sbVehicleNames = new StringBuilder();
        MapList mlconnectedVehicles = domObjectPP.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_CONNECTEDVEHICLE, // relationship pattern
                TigerConstants.TYPE_PSS_VEHICLE, // object pattern
                slObjSelect, // object selects
                null, // relationship selects
                true, // to direction
                false, // from direction
                (short) 1, // recursion level
                null, // object where clause
                null, (short) 0, false, // checkHidden
                true, // preventDuplicates
                (short) 1000, // pageSize
                null, // Postpattern
                null, null, null);
        int nconnectedVehicles = mlconnectedVehicles.size();
        for (int d = 0; d < nconnectedVehicles; d++) {
            Map mVehicleMap = (Map) mlconnectedVehicles.get(d);
            String strVehicleName = (String) mVehicleMap.get(DomainConstants.SELECT_NAME);
            sbVehicleNames.append(strVehicleName);
            sbVehicleNames.append(",");
        }
        return sbVehicleNames;
    }

    @SuppressWarnings("rawtypes")
    public static String getVariantName(Context context, DomainObject domObject, StringList slObjSelect, String productConfigurationPID) throws Exception {
        StringBuilder sbAllVariantNames = new StringBuilder();
        if (UIUtil.isNotNullAndNotEmpty(productConfigurationPID)) {
            String objWhere = "from[PSS_VariantAssemblyProductConfiguration].to.physicalid=='" + productConfigurationPID + "'";
            MapList mlVariantName = domObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_PARTVARIANTASSEMBLY, // relationship pattern
                    TigerConstants.TYPE_PSS_MBOM_VARIANT_ASSEMBLY, // object pattern
                    slObjSelect, // object selects
                    null, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 0, // recursion level
                    objWhere, // object where clause
                    null, 0);

            int nVariantSize = mlVariantName.size();
            for (int l = 0; l < nVariantSize; l++) {
                Map mapVariant = (Map) mlVariantName.get(l);
                String strVariant = (String) mapVariant.get(DomainConstants.SELECT_NAME);
                sbAllVariantNames.append(strVariant);
                sbAllVariantNames.append(",");

            }
        }
        String strAllVariantNames = sbAllVariantNames.toString();
        if (strAllVariantNames.indexOf(",") != -1) {
            strAllVariantNames = strAllVariantNames.substring(0, strAllVariantNames.length() - 1);
        }
        return strAllVariantNames;
    }

    public static MapList getMCOFromMBOMForCostingReport(Context context, String strMBOMPhysicalId) throws Exception {
        StringList slObjSelectStmts = new StringList();
        slObjSelectStmts.addElement(DomainConstants.SELECT_NAME);
        slObjSelectStmts.addElement(DomainConstants.SELECT_TYPE);
        slObjSelectStmts.addElement(DomainConstants.SELECT_CURRENT);
        slObjSelectStmts.addElement(DomainConstants.SELECT_ID);

        StringList slRelSelectStmts = new StringList(1);
        slRelSelectStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

        Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_CHANGEREQUEST);
        typePattern.addPattern(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER);

        Pattern relPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_RELATED150MBOM);
        relPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER);

        Pattern postTypePattern = new Pattern(TigerConstants.TYPE_PSS_CHANGEREQUEST);

        DomainObject domMBOM = DomainObject.newInstance(context, strMBOMPhysicalId);

        MapList mlList = domMBOM.getRelatedObjects(context, relPattern.getPattern(), // Relationship
                // Pattern
                typePattern.getPattern(), // Object Pattern
                slObjSelectStmts, // Object Selects
                slRelSelectStmts, // Relationship Selects
                true, // to direction
                false, // from direction
                (short) 3, // recursion level
                null, // object where clause
                null, (short) 0, false, // checkHidden
                true, // preventDuplicates
                (short) 1000, // pageSize
                postTypePattern, // Post Type Pattern
                null, null, null);
        return mlList;
    }

    public static MapList getHarmonyForCostingReport(Context context, DomainObject domObject, String productConfigurationPID, StringList slObjSelect, StringList selectRelStmts) throws Exception {
        StringBuilder sbrelwhereclause = new StringBuilder();
        sbrelwhereclause.append("attribute[PSS_ProductConfigurationPID]");
        sbrelwhereclause.append("==");
        sbrelwhereclause.append(productConfigurationPID);
        MapList mlHarmony = domObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_HARMONYASSOCIATION, // relationship pattern
                TigerConstants.TYPE_PSS_HARMONY, // object pattern
                slObjSelect, // object selects
                selectRelStmts, // relationship selects
                false, // to direction
                true, // from direction
                (short) 1, // recursion level
                null, // object where clause
                sbrelwhereclause.toString(), (short) 0, false, // checkHidden
                true, // preventDuplicates
                (short) 1000, // pageSize
                null, // Postpattern
                null, null, null);
        return mlHarmony;
    }

    public static StringList getEquipmentAtrributesForCostingReport(Context context, int nEQUIAttributeslength, StringList slEQUIAttributes) throws Exception {
        StringList slEQUISelects = new StringList();
        for (int v = 0; v < nEQUIAttributeslength; v++) {
            String strEQUIAttributeName = (String) slEQUIAttributes.get(v);
            if (strEQUIAttributeName.equalsIgnoreCase("description")) {
                slEQUISelects.add(DomainConstants.SELECT_DESCRIPTION);
            } else {
                slEQUISelects.add("attribute[" + strEQUIAttributeName + "]");
            }
        }
        return slEQUISelects;
    }

    // PCM: TIGTK-7779 : 18/05/2017: TS: START
    public void updatePLMEntityDescription(Context context, String[] args) throws Exception {
        try {
            String strObjectId = args[0];
            DomainObject domObj = DomainObject.newInstance(context, strObjectId);
            String strBasicDescription = domObj.getInfo(context, "description");
            String strPLMEntityDesc = domObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_PLMENTITY_V_DESCRIPTION);

            if (!strPLMEntityDesc.equalsIgnoreCase(strBasicDescription)) {
                domObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PLMENTITY_V_DESCRIPTION, strBasicDescription);

            }
        } catch (Exception e) {
            throw e;
        }
    }

    // PCM: TIGTK-7779 : 18/05/2017: TS: END

    // PCM: TIGTK-8083 : 29/05/2017: PTE: START
    // TIGTK-6773 |03/07/2017 | Harika Varanasi : Starts
    // TIGTK-9527:Rutuja Ekatpure:24/8/2017:Start
    public String cloneTool(Context context, String args[]) throws Exception {
        String strClonedObjectId = "";
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strOriginalToolId = (String) programMap.get("objectId");
            DomainObject domSourceToolObj = DomainObject.newInstance(context, strOriginalToolId);
            // TIGTK-9583:30/08/2017 :PTE:START
            String strObjectGeneratorName = FrameworkUtil.getAliasForAdmin(context, DomainConstants.SELECT_TYPE, TigerConstants.TYPE_VPMREFERENCE, true);
            String strAutoName = DomainObject.getAutoGeneratedName(context, strObjectGeneratorName, "PSS_Tool");
            // TIGTK-9583:30/08/2017 :PTE:END
            DomainObject domClonedToolObj = new DomainObject(domSourceToolObj.cloneObject(context, strAutoName, "01.1", TigerConstants.VAULT_VPLM, true));
            strClonedObjectId = domClonedToolObj.getId(context);
            DomainRelationship domainRelationship = DomainRelationship.connect(context, domSourceToolObj, TigerConstants.RELATIONSHIP_DERIVED, domClonedToolObj);
            domainRelationship.setAttributeValue(context, TigerConstants.ATTRIBUTE_DERIVED_CONTEXT, "Clone");
            // set attribute value
            setAttributeOnCloneTool(context, strClonedObjectId);
        } catch (Exception e) {
            logger.error("Error in cloneTool: ", e);
        }
        return strClonedObjectId;

    }

    // TIGTK-9527:Rutuja Ekatpure:24/8/2017:End
    // TIGTK-6773 |03/07/2017 | Harika Varanasi : Ends

    // TIGTK-9527:Rutuja Ekatpure:24/8/2017:Start
    /****
     * this method set attribute value on cloned tool object
     * @param context
     * @param strClonedToolId
     * @throws Exception
     */
    public void setAttributeOnCloneTool(Context context, String strClonedToolId) throws Exception {

        if (UIUtil.isNotNullAndNotEmpty(strClonedToolId)) {
            DomainObject domClonedToolObj = DomainObject.newInstance(context, strClonedToolId);
            String strAutoName = domClonedToolObj.getInfo(context, DomainConstants.SELECT_NAME);
            String strCloneToolPhysId = domClonedToolObj.getInfo(context, "physicalid");
            domClonedToolObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_V_NAME, strAutoName);
            domClonedToolObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PLM_EXTERNALID, strAutoName);
            domClonedToolObj.setAttributeValue(context, "PLMReference.V_VersionID", strCloneToolPhysId);
        }
    }

    // TIGTK-9527:Rutuja Ekatpure:24/8/2017:End
    // PCM: TIGTK-8083 : 29/05/2017: PTE: END

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     *             MBOM: TIGTK-6909 : 31/05/2017: VB: Start
     */

    public int modifyPSSTypeOfPart(Context context, String args[]) throws Exception {
        try {
            String strFromObjectId = args[0];
            String strNewAttributeValue = args[1];
            String strAttributeName = args[2];
            String strToObjectId = args[3];
            StringBuffer sbBuffer = new StringBuffer("PSS_ManufacturingInstanceExt.");
            sbBuffer.append(strAttributeName);

            if (UIUtil.isNotNullAndNotEmpty(strFromObjectId)) {
                DomainObject domFromObject = DomainObject.newInstance(context, strFromObjectId);
                String strFromObjPolicy = domFromObject.getInfo(context, DomainConstants.SELECT_POLICY);
                String objectWhere = "id==" + strToObjectId;
                StringList slObjSelectStmts = new StringList();
                slObjSelectStmts.addElement(DomainConstants.SELECT_ID);

                StringList slRelSelectStmts = new StringList(TigerConstants.SELECT_PHYSICALID);

                Pattern typePattern = new Pattern(TigerConstants.TYPE_CREATEASSEMBLY);
                typePattern.addPattern(TigerConstants.TYPE_CREATEKIT);
                typePattern.addPattern(TigerConstants.TYPE_CREATEMATERIAL);

                MapList mlObejctList = domFromObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_DELFMIFUNCTIONIDENTIFIEDINSTANCE, // Relationship
                        // Pattern
                        typePattern.getPattern(), // Object Pattern
                        slObjSelectStmts, // Object Selects
                        slRelSelectStmts, // Relationship Selects
                        false, // to direction
                        true, // from direction
                        (short) 1, // recursion level
                        objectWhere, // object where clause
                        null, (short) 0, false, // checkHidden
                        false, // preventDuplicates
                        (short) 1000, // pageSize
                        null, // Post Type Pattern
                        null, null, null);
                if (!mlObejctList.isEmpty()) {
                    for (int i = 0; i < mlObejctList.size(); i++) {
                        Map mapObject = (Map) mlObejctList.get(i);
                        String strRelIds = (String) mapObject.get(TigerConstants.SELECT_PHYSICALID);
                        DomainRelationship domRel = DomainRelationship.newInstance(context, strRelIds);
                        if (TigerConstants.POLICY_PSS_STANDARDMBOM.equals(strFromObjPolicy)) {
                            domRel.setAttributeValue(context, sbBuffer.toString().trim(), "Buy");
                        } else {
                            domRel.setAttributeValue(context, sbBuffer.toString().trim(), strNewAttributeValue);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error in cloneTool: ", e);
            throw e;
        }

        return 0;
    }

    // TIGTK-8459:Rutuja Ekatpure :9/6/2017:start
    /****
     * this method is used for updating root scope
     * @param context
     * @param args
     * @throws Exception
     */
    public static String updateRootScope(Context context, String[] args) throws Exception { // Called from FRCUpdateScopePostProcess.jsp
        PLMCoreModelerSession plmSession = null;
        // IPLMxCoreAccess coreAccess = plmSession.getVPLMAccess();
        StringBuffer returnString = new StringBuffer();
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();
            IPLMxCoreAccess coreAccess = plmSession.getVPLMAccess();
            String mbomRefID = args[0];
            String psRefID = args[1];
            // String errorObjects = new String();
            StringBuffer strBuffer = new StringBuffer();
            DomainObject domMfgItem = new DomainObject(mbomRefID);
            DomainObject domPSItem = new DomainObject(psRefID);
            String rootObjectState = domMfgItem.getInfo(context, DomainConstants.SELECT_CURRENT);
            String rootObjectName = domMfgItem.getInfo(context, DomainConstants.SELECT_NAME);
            StringList objSelects = new StringList();
            objSelects.addElement(DomainConstants.SELECT_ID);
            objSelects.addElement(DomainConstants.SELECT_NAME);
            objSelects.addElement(DomainConstants.SELECT_CURRENT);
            objSelects.addElement("physicalid");

            StringList relSelects = new StringList();
            relSelects.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            relSelects.addElement("physicalid[connection]");
            matrix.util.Pattern relPattern = new matrix.util.Pattern(PropertyUtil.getSchemaProperty(context, "relationship_DELFmiFunctionIdentifiedInstance"));

            if ("IN_WORK".equalsIgnoreCase(rootObjectState) || TigerConstants.STATE_PSS_MBOM_INWORK.equalsIgnoreCase(rootObjectState)) {
                MapList mlToConnected = domMfgItem.getRelatedObjects(context, relPattern.getPattern(), // relationshipPattern
                        "*", // typePattern
                        objSelects, // objectSelects
                        relSelects, // relationshipSelects
                        false, // getTo
                        true, // getFrom
                        (short) 0, // recurseToLevel
                        null, // objectWhere,
                        null, // relationshipWhere
                        (int) 0); // limit

                String mbomRefPID = domMfgItem.getInfo(context, "physicalid"); // MqlUtil.mqlCommand(context, "print bus " + mbomRefID + " select physicalid dump |", false, false);
                String psRefPID = domPSItem.getInfo(context, "physicalid");// MqlUtil.mqlCommand(context, "print bus " + psRefID + " select physicalid dump |", false, false);

                FRCMBOMModelerUtility.reconnectScopeLinkAndUpdateChildImplementLinks(context, plmSession, mbomRefPID, psRefPID, true);
                // PSS: Sync FCS Index : START
                PSS_FRCMBOMModelerUtility_mxJPO.setFCSIndexOnMBOM(context, psRefPID, mbomRefPID);
                // PSS: Sync FCS Index : END
                flushSession(plmSession);

                for (int i = 0; i < mlToConnected.size(); i++) {
                    Map mapChild = (Map) mlToConnected.get(i);
                    String mbomObjId = (String) mapChild.get("id");
                    String strMBOMName = (String) mapChild.get("name");
                    String mbomObjPhysicalId = (String) mapChild.get("physicalid");

                    if (!mbomRefPID.equals(mbomObjPhysicalId)) {
                        String mbomObjConnectionId = (String) mapChild.get("id[connection]");
                        String mbomConnectionPhysicalId = (String) mapChild.get("physicalid[connection]");

                        String mbomObjState = (String) mapChild.get("current");
                        String mbomObjName = (String) mapChild.get("name");

                        // Check if the MBOM leaf reference already has a scope
                        List<String> inputListForGetScope = new ArrayList<String>();
                        inputListForGetScope.add(mbomObjPhysicalId);
                        String currentPSRefScopePID = FRCMBOMModelerUtility.getScopedPSReferencePIDFromList(context, plmSession, inputListForGetScope).get(0);
                        List<String> implementLink = FRCMBOMModelerUtility.getImplementLinkInfoSimple(context, plmSession, mbomConnectionPhysicalId);
                        if (currentPSRefScopePID != null && !"".equals(currentPSRefScopePID) && (implementLink.size() > 0)) {

                            if ("IN_WORK".equalsIgnoreCase(mbomObjState) || TigerConstants.STATE_PSS_MBOM_INWORK.equalsIgnoreCase(mbomObjState)) {

                                String implementLinkInstancePID = implementLink.get(implementLink.size() - 1);
                                String existingPSRefScopePID = MqlUtil.mqlCommand(context, "print connection " + implementLinkInstancePID + " select to.physicalid dump |", false, false);
                                List<String> modifiedInstanceList2 = FRCMBOMModelerUtility.reconnectScopeLinkAndUpdateChildImplementLinks(context, plmSession, mbomObjPhysicalId, existingPSRefScopePID,
                                        true);
                                // PSS: Sync FCS Index : START
                                PSS_FRCMBOMModelerUtility_mxJPO.setFCSIndexOnMBOM(context, existingPSRefScopePID, mbomObjPhysicalId);
                                // PSS: Sync FCS Index : END

                            } else {
                                strBuffer.append("\'");
                                strBuffer.append(strMBOMName);
                                strBuffer.append("\'");
                                strBuffer.append("\n");
                            }
                        }

                    }
                }
            } else {
                strBuffer.append("\'");
                strBuffer.append(rootObjectName);
                strBuffer.append("\'");
                strBuffer.append("\n");
            }

            if (UIUtil.isNotNullAndNotEmpty(strBuffer.toString())) {
                returnString.append("Reconnection not performed due to node(s) not at 'In Work' state:").append("\n").append(strBuffer.toString()).append("\n");

            }

            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);

            // }
        } catch (Exception exp) {
            // exp.printStackTrace();

            flushAndCloseSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw exp;
        }
        // FRC changes added for MBO-167 :H65:02/03/2017-END
        return returnString.toString();

    }

    // TIGTK-8459:Rutuja Ekatpure :9/6/2017:End

    public static String createMBOMFromDragAndDrop(Context context, PLMCoreModelerSession plmSession, String psRefPID, String mbomRefPID, String plantPID, String approvalStatus,
            String mbomCompletePath, String psCompletePath, Map<String, List<String>> workingInfo, List<Map<String, String>> workingInfo_instanceAttributes,
            List<Integer> workingInfo_indexInstancesForImplement, Map<String, String> workingInfo_AppDateToValuate) throws Exception {
        String newObjectId = DomainConstants.EMPTY_STRING;
        boolean isRPESet = false;
        try {

            if (UIUtil.isNotNullAndNotEmpty(psRefPID)) {
                String strPartId = DomainConstants.EMPTY_STRING;
                MapList mlPartObject = pss.mbom.MBOMUtil_mxJPO.getPartFromVPMReference(context, psRefPID);
                if (!mlPartObject.isEmpty()) {
                    for (int i = 0; i < mlPartObject.size(); i++) {
                        Map objectMap = (Map) mlPartObject.get(i);
                        strPartId = (String) objectMap.get(DomainConstants.SELECT_ID);
                    }
                }

                Map objectMap = new HashMap();
                objectMap.put("objectId", strPartId);
                objectMap.put("PSS_PlantOID", plantPID);
                objectMap.put("FRCMBOMGetChangeObject", null);
                objectMap.put("changeObjectFromForm", null);
                objectMap.put("TypeActual", "CreateAssembly");
                objectMap.put("mbomRefPID", mbomRefPID);
                objectMap.put("mbomCompletePath", mbomCompletePath);
                objectMap.put("psCompletePath", psCompletePath);

                DomainObject domPS = DomainObject.newInstance(context, psRefPID);
                String relationshipWhere = "attribute[PSS_PublishedEBOM.PSS_InstanceName].value == 'EBOM'";
                StringList slObjSelectStmts = new StringList();
                slObjSelectStmts.addElement("physicalid");

                StringList slRelSelectStmts = new StringList(1);
                slRelSelectStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

                MapList mlPSObjectsList = domPS.getRelatedObjects(context, "VPMInstance", // Relationship
                        // Pattern
                        TigerConstants.TYPE_VPMREFERENCE, // Object Pattern
                        slObjSelectStmts, // Object Selects
                        slRelSelectStmts, // Relationship Selects
                        false, // to direction
                        true, // from direction
                        (short) 1, // recursion level
                        null, // object where clause
                        relationshipWhere, (short) 0, false, // checkHidden
                        true, // preventDuplicates
                        (short) 1000, // pageSize
                        null, // Post Type Pattern
                        null, null, null);

                PropertyUtil.setGlobalRPEValue(context, NOTGENERATEVARINTASSEMBLY, "True");
                // TIGTK-11580:RE:1/12/2017:Start
                PropertyUtil.setRPEValue(context, ISCRATEUSINGDRAGNDROP, "True", false);
                // TIGTK-11580:RE:1/12/2017:End
                isRPESet = true;
                if (mlPSObjectsList.isEmpty()) {

                    objectMap.put("TypeActual", "CreateMaterial");
                    newObjectId = createNewTopManuAssembly(context, plmSession, JPO.packArgs(objectMap), workingInfo, workingInfo_instanceAttributes, workingInfo_indexInstancesForImplement,
                            workingInfo_AppDateToValuate);
                } else {
                    if (approvalStatus.equalsIgnoreCase("TopLevelAssembly")) {
                        // TIGTK-12366 : VB : Start :Do not expand child if top selection
                        PropertyUtil.setRPEValue(context, DONOTEXPAND, "True", false);
                        // TIGTK-12366 : VB : End
                        newObjectId = createNewTopManuAssembly(context, plmSession, JPO.packArgs(objectMap), workingInfo, workingInfo_instanceAttributes, workingInfo_indexInstancesForImplement,
                                workingInfo_AppDateToValuate);
                    } else {
                        newObjectId = createNewCompleteManuAssembly(context, plmSession, JPO.packArgs(objectMap), workingInfo, workingInfo_instanceAttributes, workingInfo_indexInstancesForImplement,
                                workingInfo_AppDateToValuate);
                    }

                }

            }

        } catch (FrameworkException e) {
            // TODO Auto-generated catch block
            logger.error("Error in createMBOMFromDragAndDrop: ", e);
            throw e;
        } finally {
            // TIGTK-11580:RE:1/12/2017:Start
            if (isRPESet)
                PropertyUtil.setRPEValue(context, ISCRATEUSINGDRAGNDROP, "False", false);
            // TIGTK-11580:RE:1/12/2017:End
        }
        return newObjectId;

    }

    public static String createNewCompleteManuAssembly(Context context, PLMCoreModelerSession plmSession, String[] args, Map<String, List<String>> workingInfo,
            List<Map<String, String>> workingInfo_instanceAttributes, List<Integer> workingInfo_indexInstancesForImplement, Map<String, String> workingInfo_AppDateToValuate) throws Exception {
        String sRet = "";
        String rootRefPID = "";
        try {
            PSS_FRCMBOMModelerUtility_mxJPO.checkValidScenario(context);
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String sPartId = (String) programMap.get("objectId");
            valueEnvAttachModel = System.getenv("DISABLE_ATTACH_MODEL_ON_SCOPE");

            // PSS: START
            String plantId = (String) programMap.get("PSS_PlantOID");
            DomainObject plantObj = DomainObject.newInstance(context, plantId);
            plantId = plantObj.getInfo(context, "physicalid");
            // PSS: END

            // Get the change object
            String changeObjectName = (String) programMap.get("FRCMBOMGetChangeObject");
            // Modif AFN - Test if a value has been defined into the creation web form
            String changeObjectFromForm = (String) programMap.get("ChangeObjectCreateForm");
            String MBOMId = (String) programMap.get("mbomRefPID");
            if (null != changeObjectFromForm && !"".equals(changeObjectFromForm))
                changeObjectName = changeObjectFromForm;

            MapList mlPrd = getProductFromEBOM(context, sPartId);

            if (null != mlPrd) {
                if (1 < mlPrd.size())
                    throw new Exception("Several VPM Products have been found for the given EBOM part");
                if (mlPrd.size() == 0)
                    throw new Exception(EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.ErrorMessage.NoVPMProduct"));
                Map mPrd = (Map) mlPrd.get(0);
                String sPrdPhysId = (String) mPrd.get("physicalid");
                if (null != sPrdPhysId && !"".equals(sPrdPhysId)) {
                    // PSS : START
                    /*
                     * if (checkForExistingMBOMWithPlant(context, plmSession, sPrdPhysId, plantId)) { throw new Exception( EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources",
                     * context.getLocale(), "PSS_FRCMBOMCentral.Error.Message.CanNotCreateNewItemWithPlant")); }
                     */
                    // PSS : END
                    // List<String> lModelsArray = FRCMBOMModelerUtility.getAttachedModelsOnReference(context, plmSession, sPrdPhysId);
                    lModelListOnStructure = new ArrayList<String>();
                    // Recursively process the PS root node
                    String mbomCompletePath = (String) programMap.get("mbomCompletePath");
                    String psCompletePath = (String) programMap.get("psCompletePath");
                    /*
                     * sRet = createMBOMFromEBOMLikePSRecursive(context, plmSession, null, sPrdPhysId, mbomCompletePath, psCompletePath, lModelsArray.toArray(new String[] {}), newRefPIDList, plantId,
                     * null);
                     */
                    Set<String> workingInfo_lModelListOnStructure = new HashSet<String>();

                    String returnMBOMRefPLMID = FRCMBOMModelerUtility.getPLMIDFromPID(context, plmSession, MBOMId);

                    sRet = createMBOMFromEBOMLikePSRecursive_new(context, plmSession, returnMBOMRefPLMID, mbomCompletePath, sPrdPhysId, psCompletePath, workingInfo, workingInfo_lModelListOnStructure,
                            workingInfo_instanceAttributes, workingInfo_indexInstancesForImplement, workingInfo_AppDateToValuate, plantId, null);// newRefPIDList, newScopesToCreate_MBOMRefPIDs,
                                                                                                                                                 // newScopesToCreate_PSRefPIDs);

                    flushSession(plmSession);

                    rootRefPID = PLMID.buildFromString(sRet).getPid();
                    // Valuate the V_ApplicabilityDate attributes
                    for (Entry<String, String> entrySet : workingInfo_AppDateToValuate.entrySet()) {
                        String refPID = entrySet.getKey();
                        MqlUtil.mqlCommand(context, "mod bus " + refPID + " PLMReference.V_ApplicabilityDate '" + workingInfo_AppDateToValuate.get(refPID) + "'", false, false);
                    }
                    // Create all the MBOM instances in one shot
                    List<String> allCreatedInstancesPIDList = new ArrayList<String>();
                    Map<String, Map<String, String>> validateAttributeMap = new HashMap<String, Map<String, String>>();
                    workingInfo.put("mbomLeafInstancePIDList",
                            createInstanceBulk(context, plmSession, workingInfo.get("instanceToCreate_parentRefPLMID"), workingInfo.get("instanceToCreate_childRefPLMID"),
                                    workingInfo_instanceAttributes, workingInfo_indexInstancesForImplement, allCreatedInstancesPIDList, validateAttributeMap));

                    flushSession(plmSession);

                    // MBO-164-MBOM performance issue:START-H65 15/11/2017
                    String[] strArray = null;
                    StringList slSplitExtensionList = new StringList();
                    for (String instancePID : allCreatedInstancesPIDList) {
                        String strExtension = MqlUtil.mqlCommand(context, "print connection " + instancePID + " select interface dump |", false, false);
                        if (UIUtil.isNotNullAndNotEmpty(strExtension)) {
                            if (strExtension.contains("|")) {
                                strArray = strExtension.split("\\|");
                                if (strArray.length > 0) {
                                    for (int i = 0; i < strArray.length; i++)
                                        slSplitExtensionList.add(strArray[i]);
                                }
                            } else
                                slSplitExtensionList.add(strExtension);
                        }
                        if (!slSplitExtensionList.contains("FRCCustoExtension1"))
                            MqlUtil.mqlCommand(context, "mod connection " + instancePID + " add interface FRCCustoExtension1", false, false);
                    }
                    // MBO-164-Below MBOM performance issue:END-H65 15/11/2017

                    // In the list of models to attach to the root, add the models attached to the root PS
                    workingInfo_lModelListOnStructure.addAll(FRCMBOMModelerUtility.getAttachedModelsOnReference(context, plmSession, sPrdPhysId));

                    // Attach all Models to the Root
                    if (workingInfo_lModelListOnStructure.size() > 0) {
                        List lBOMRef = new ArrayList<List>();
                        lBOMRef.add(rootRefPID);
                        lBOMRef.add(MBOMId);
                        pss.mbom.MBOMUtil_mxJPO.getParentVPMStructure(context, MBOMId, lBOMRef);
                        FRCMBOMModelerUtility.attachModelsOnReferencesAndSetOptionsCriteria(context, plmSession, lBOMRef, new ArrayList<String>(workingInfo_lModelListOnStructure));
                    }

                    // Create all the scope links in one shot
                    FRCMBOMModelerUtility.createScopeLinkBulk(context, plmSession, workingInfo.get("newScopesToCreate_MBOMRefPIDs"), workingInfo.get("newScopesToCreate_PSRefPIDs"));

                    flushSession(plmSession);
                    // Create all the implement links in one shot
                    if (!allCreatedInstancesPIDList.isEmpty()) {
                        PSS_FRCMBOMModelerUtility_mxJPO.setImplementLinkBulk(context, plmSession, allCreatedInstancesPIDList, workingInfo.get("psPathList"), true);

                        /*
                         * if (!"".equals(retStr)) throw new Exception("NO Scope");
                         */

                        flushSession(plmSession);
                        setInstAttributeValues(context, validateAttributeMap, allCreatedInstancesPIDList);
                    }
                    // Attach all created references to change object.
                    attachListObjectsToChange(context, plmSession, changeObjectName, workingInfo.get("newRefPIDList"));
                }
            } else {
                throw new Exception(EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.ErrorMessage.NoVPMProduct"));
            }

        } catch (Exception e) {
            logger.error("Error in createNewCompleteManuAssembly: ", e);
            if (context.isTransactionActive()) {
                ContextUtil.abortTransaction(context);
            }
            throw e;
        }
        return rootRefPID;
    }

    public static String createNewTopManuAssembly(Context context, PLMCoreModelerSession plmSession, String[] args, Map<String, List<String>> workingInfo,
            List<Map<String, String>> workingInfo_instanceAttributes, List<Integer> workingInfo_indexInstancesForImplement, Map<String, String> workingInfo_AppDateToValuate) throws Exception {
        String sRet = "";
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String sPartId = (String) programMap.get("objectId");
            // PSS: START
            String plantId = (String) programMap.get("PSS_PlantOID");
            String MBOMId = (String) programMap.get("mbomRefPID");
            String psCompletePath = (String) programMap.get("psCompletePath");
            String mbomCompletePath = (String) programMap.get("mbomCompletePath");
            String[] psCompletePathList = psCompletePath.split("/");

            if (UIUtil.isNotNullAndNotEmpty(plantId)) {
                DomainObject plantObj = DomainObject.newInstance(context, plantId);
                plantId = plantObj.getInfo(context, "physicalid");
                // PSS: END

                MapList mlPrd = getProductFromEBOM(context, sPartId);
                if (null != mlPrd) {
                    if (1 < mlPrd.size())
                        throw new Exception("Several VPM Products have been found for the given EBOM part");
                    if (mlPrd.size() == 0)
                        throw new Exception(EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.ErrorMessage.NoVPMProduct"));
                    Map mPrd = (Map) mlPrd.get(0);
                    String sPrdPhysId = (String) mPrd.get("physicalid");

                    MapList psInstList = null;
                    String result = MqlUtil.mqlCommand(context, "print bus " + sPrdPhysId + " select interface dump |", false, false);
                    boolean isStandardMBOM = false;
                    DomainObject psRefObj = new DomainObject(sPrdPhysId);
                    Map psRefAttributes = psRefObj.getAttributeMap(context, true);
                    if (UIUtil.isNotNullAndNotEmpty(result) && result.contains("PSS_PublishedPart") && "Standard".equals(psRefAttributes.get("PSS_PublishedPart.PSS_StandardReference"))) {
                        isStandardMBOM = true;
                    }

                    if (null != sPrdPhysId && !"".equals(sPrdPhysId)) {
                        // Create new manuf item
                        String type = (String) programMap.get("TypeActual");

                        HashMap<String, String> attributes = new HashMap<String, String>();
                        // attributes.put("PLM_ExternalID", "9995");

                        // PSS : START
                        // String newRefPID = createMBOMReference(context, plmSession, type, null, attributes);
                        /*
                         * if (checkForExistingMBOMWithPlant(context, plmSession, sPrdPhysId, plantId)) { throw new Exception( EnoviaResourceBundle.getProperty(context,
                         * "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.Error.Message.CanNotCreateNewItemWithPlant")); }
                         */

                        List<String> scopingMBOMPIDList = PSS_FRCMBOMModelerUtility_mxJPO.getScopingReferencesFromList_PLMID(context, plmSession, sPrdPhysId);

                        String newRefPID = DomainConstants.EMPTY_STRING;
                        if (isStandardMBOM)
                            newRefPID = getReleasedStandardMBOMReferenceToReuse(context, scopingMBOMPIDList, plantId);
                        else
                            newRefPID = getMBOMReferenceToReuse(context, plmSession, scopingMBOMPIDList, plantId);

                        String isSkipStandardMBOM = PropertyUtil.getRPEValue(context, SKIPSTANDARDMBOM, true);
                        if (!"true".equalsIgnoreCase(isSkipStandardMBOM)) {
                            if (UIUtil.isNullOrEmpty(newRefPID)) {
                                newRefPID = createMBOMReferenceWithPlant(context, plmSession, type, null, attributes, sPrdPhysId, plantId);
                            } // PSS : END

                            String changeObjectName = (String) programMap.get("FRCMBOMGetChangeObject");
                            // Modif AFN - Test if a value has been defined into the creation web form
                            String changeObjectFromForm = (String) programMap.get("ChangeObjectCreateForm");
                            if (null != changeObjectFromForm && !"".equals(changeObjectFromForm))
                                changeObjectName = changeObjectFromForm;
                            attachObjectToChange(context, plmSession, changeObjectName, newRefPID);

                            Set<String> workingInfo_lModelListOnStructure = new HashSet<String>();

                            // Set scope and attach model
                            /*
                             * PSS_FRCMBOMModelerUtility_mxJPO.createScopeLinkAndReduceChildImplementLinks(context, plmSession, newRefPID, sPrdPhysId, false);
                             * 
                             * // Before attaching Models, the plmSession needs to be flushed, otherwise the new objects will not be seen... flushSession(plmSession);
                             * 
                             * List<String> lModelsArray = FRCMBOMModelerUtility.getAttachedModelsOnReference(context, plmSession, sPrdPhysId);
                             * 
                             * List<String> newRefPIDList = new ArrayList<String>(); newRefPIDList.add(newRefPID); newRefPIDList.add(MBOMId); pss.mbom.MBOMUtil_mxJPO.getParentVPMStructure(context,
                             * MBOMId, newRefPIDList); FRCMBOMModelerUtility.attachModelsOnReferencesAndSetOptionsCriteria(context, plmSession, newRefPIDList, lModelsArray);
                             */

                            DomainRelationship psInstObj = new DomainRelationship(psCompletePathList[psCompletePathList.length - 1]);
                            Map psInstAttributes = psInstObj.getAttributeMap(context, true);
                            Map mbomInstAttributes = new HashMap();
                            String psTreeOrder = (String) psInstAttributes.get("PLMInstance.V_TreeOrder");
                            String psVName = (String) psInstAttributes.get("PLMInstance.V_Name");
                            String psExternalID = (String) psInstAttributes.get("PLMInstance.PLM_ExternalID");
                            mbomInstAttributes.put("PLMInstance.V_TreeOrder", psTreeOrder);
                            mbomInstAttributes.put("PLMInstance.V_Name", psVName);
                            mbomInstAttributes.put("PLMInstance.PLM_ExternalID", psExternalID);

                            workingInfo_instanceAttributes.add(mbomInstAttributes);

                            String strChildPLMID = FRCMBOMModelerUtility.getPLMIDFromPID(context, plmSession, newRefPID);

                            String strParentPLMID = FRCMBOMModelerUtility.getPLMIDFromPID(context, plmSession, MBOMId);

                            workingInfo.get("instanceToCreate_childRefPLMID").add(strChildPLMID);
                            workingInfo.get("instanceToCreate_parentRefPLMID").add(strParentPLMID);

                            for (Entry<String, String> entrySet : workingInfo_AppDateToValuate.entrySet()) {
                                String refPID = entrySet.getKey();
                                MqlUtil.mqlCommand(context, "mod bus " + refPID + " PLMReference.V_ApplicabilityDate '" + workingInfo_AppDateToValuate.get(refPID) + "'", false, false);
                            }
                            // Create all the MBOM instances in one shot
                            List<String> allCreatedInstancesPIDList = new ArrayList<String>();
                            Map<String, Map<String, String>> validateAttributeMap = new HashMap<String, Map<String, String>>();
                            workingInfo.put("mbomLeafInstancePIDList",
                                    createInstanceBulk(context, plmSession, workingInfo.get("instanceToCreate_parentRefPLMID"), workingInfo.get("instanceToCreate_childRefPLMID"),
                                            workingInfo_instanceAttributes, workingInfo_indexInstancesForImplement, allCreatedInstancesPIDList, validateAttributeMap));

                            flushSession(plmSession);

                            // MBO-164-MBOM performance issue:START-H65 15/11/2017
                            String[] strArray = null;
                            StringBuffer sb = new StringBuffer();
                            sb.append(mbomCompletePath);
                            StringList slSplitExtensionList = new StringList();
                            for (String instancePID : allCreatedInstancesPIDList) {
                                String strExtension = MqlUtil.mqlCommand(context, "print connection " + instancePID + " select interface dump |", false, false);
                                if (UIUtil.isNotNullAndNotEmpty(strExtension)) {
                                    if (strExtension.contains("|")) {
                                        strArray = strExtension.split("\\|");
                                        if (strArray.length > 0) {
                                            for (int i = 0; i < strArray.length; i++)
                                                slSplitExtensionList.add(strArray[i]);
                                        }
                                    } else
                                        slSplitExtensionList.add(strExtension);
                                }
                                if (!slSplitExtensionList.contains("FRCCustoExtension1"))
                                    MqlUtil.mqlCommand(context, "mod connection " + instancePID + " add interface FRCCustoExtension1", false, false);

                                sb.append("/");
                                sb.append(instancePID);

                            }
                            // MBO-164-Below MBOM performance issue:END-H65 15/11/2017

                            // In the list of models to attach to the root, add the models attached to the root PS
                            workingInfo_lModelListOnStructure.addAll(FRCMBOMModelerUtility.getAttachedModelsOnReference(context, plmSession, sPrdPhysId));

                            // Attach all Models to the Root
                            if (workingInfo_lModelListOnStructure.size() > 0) {
                                List lBOMRef = new ArrayList<List>();
                                lBOMRef.add(newRefPID);
                                lBOMRef.add(MBOMId);
                                pss.mbom.MBOMUtil_mxJPO.getParentVPMStructure(context, MBOMId, lBOMRef);
                                FRCMBOMModelerUtility.attachModelsOnReferencesAndSetOptionsCriteria(context, plmSession, lBOMRef, new ArrayList<String>(workingInfo_lModelListOnStructure));
                            }

                            PSS_FRCMBOMModelerUtility_mxJPO.createScopeLinkAndReduceChildImplementLinks(context, plmSession, newRefPID, sPrdPhysId, false);

                            flushSession(plmSession);

                            // Create all the implement links in one shot

                            String trimmedPSPath = trimPSPathToClosestScope(context, plmSession, sb.toString(), psCompletePath);
                            if (trimmedPSPath == null || "".equals(trimmedPSPath))
                                throw new Exception("No scope exists.");

                            List<String> slCheck = workingInfo.get("psPathList");
                            if (!slCheck.contains(trimmedPSPath))
                                workingInfo.get("psPathList").add(trimmedPSPath);
                            if (!allCreatedInstancesPIDList.isEmpty()) {
                                PSS_FRCMBOMModelerUtility_mxJPO.setImplementLinkBulk(context, plmSession, allCreatedInstancesPIDList, workingInfo.get("psPathList"), true);

                                /*
                                 * if (!"".equals(retStr)) throw new Exception(retStr);
                                 */

                                setInstAttributeValues(context, validateAttributeMap, allCreatedInstancesPIDList);
                            }
                        }
                        sRet = newRefPID;
                    }
                } else {
                    throw new Exception(EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.ErrorMessage.NoVPMProduct"));
                }
            }
        } catch (Exception e) {
            logger.error("Error in createNewTopManuAssembly: ", e);
            if (context.isTransactionActive()) {
                ContextUtil.abortTransaction(context);
            }
            throw e;
        }
        return sRet;
    }

    public void disconnectPlant(Context context, String[] args) throws Exception {
        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, true);
            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();
            boolean checkDelete = false;
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strAlertMessage = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.Alert.ModifyOwnership");
            if (null != programMap) {
                String sId = (String) programMap.get("objectId");
                String[] aTableRowId = (String[]) programMap.get("emxTableRowId");
                if (null != aTableRowId && UIUtil.isNotNullAndNotEmpty(sId)) {
                    DomainObject dMBOMObj = DomainObject.newInstance(context, sId);

                    for (int i = 0; i < aTableRowId.length; i++) {
                        String sPlant = aTableRowId[i];
                        String[] sArrayOfPlantId = sPlant.split("\\|");
                        if (sArrayOfPlantId.length > 0) {

                            String strMfgProductionPlanning = getRelatedMfgProductionPlanning(context, sId, sArrayOfPlantId[1]);
                            if (UIUtil.isNotNullAndNotEmpty(strMfgProductionPlanning)) {
                                DomainObject domMfgProductionPlanning = DomainObject.newInstance(context, strMfgProductionPlanning);
                                String strMasterPlant = domMfgProductionPlanning.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGPLANTEXTPSS_OWNERSHIP);
                                if (strMasterPlant.equalsIgnoreCase("Master")) {
                                    int iReturn = pss.mbom.MBOMUtil_mxJPO.checkMBOMIsConnectedToChangeManagment(context, sId);
                                    if (iReturn == 1)
                                        throw new Exception(strAlertMessage);
                                    else {
                                        FRCMBOMModelerUtility.detachPlantFromMBOMReference(context, plmSession, sId, sArrayOfPlantId[1]);
                                        checkDelete = true;
                                    }
                                } else {
                                    FRCMBOMModelerUtility.detachPlantFromMBOMReference(context, plmSession, sId, sArrayOfPlantId[1]);
                                }
                            }

                        }
                        // RFC-139 : Update the Master Plant Name after remove plant
                        if (checkDelete == true) {
                            String strMasterMfgProductionPlanning = pss.mbom.MBOMUtil_mxJPO.getMasterMfgProductionPlanning(context, sId);
                            if (UIUtil.isNotNullAndNotEmpty(strMasterMfgProductionPlanning)) {
                                DomainObject dMfgProductionPlanningObj = DomainObject.newInstance(context, strMasterMfgProductionPlanning);
                                String strPlantName = dMfgProductionPlanningObj.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_VOWNER + "].from.name");
                                dMBOMObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXTPSS_MATERPLANTNAME, strPlantName);
                            } else
                                dMBOMObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXTPSS_MATERPLANTNAME, "");
                        }
                    }

                }
            }
            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);
        } catch (Exception e) {
            logger.error("Error in disconnectPlant: ", e);
            flushAndCloseSession(plmSession);
            emxContextUtil_mxJPO.mqlNotice(context, e.getMessage());
            if (context.isTransactionActive()) {
                ContextUtil.abortTransaction(context);
            }
            throw e;
        }
    }

    /**
     * This is intermediate method to get the MfgProductionPlanning connected to MBOM with specific plant
     * @param context
     * @param strMBOMId
     * @param strPlantId
     * @return TIGTK-10700
     */
    public static String getRelatedMfgProductionPlanning(Context context, String strMBOMId, String strPlantId) {
        String strMfgProductionPlanningId = DomainConstants.EMPTY_STRING;

        try {
            StringList slConnectedMfgList = pss.mbom.MBOMUtil_mxJPO.getMfgProductionPlanning(context, strMBOMId);
            Pattern typePattern = new Pattern("MfgProductionPlanning");
            StringList slObjSelectStmts = new StringList(1);
            slObjSelectStmts.addElement("physicalid");
            StringList slRelSelectStmts = new StringList(1);
            slRelSelectStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

            DomainObject domPlant = DomainObject.newInstance(context, strPlantId);

            MapList mlMfgProductionConnectedPlant = domPlant.getRelatedObjects(context, "VPLMrel/PLMConnection/V_Owner", // Relationship
                    // Pattern
                    typePattern.getPattern(), // Object Pattern
                    slObjSelectStmts, // Object Selects
                    slRelSelectStmts, // Relationship Selects
                    false, // to direction
                    true, // from direction
                    (short) 0, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    null, // Post Type Pattern
                    null, null, null);
            if (!mlMfgProductionConnectedPlant.isEmpty()) {
                for (int i = 0; i < mlMfgProductionConnectedPlant.size(); i++) {
                    Map mConnectedMfg = (Map) mlMfgProductionConnectedPlant.get(i);
                    String strMfgId = (String) mConnectedMfg.get("physicalid");
                    if (slConnectedMfgList.contains(strMfgId))
                        strMfgProductionPlanningId = strMfgId;
                }
            }

        } catch (Exception e) {
            logger.error("Error in getRelatedMfgProductionPlanning: ", e);
        }

        return strMfgProductionPlanningId;
    }

    // TIGTK-6801:PKH:Phase-2.0:Start
    /***
     * this method get EBOM from MBOM.
     * @param context
     * @param args
     * @return MapList
     * @throws Exception
     * @author PKH
     */
    public MapList getEBOMFromMBOM(Context context, String[] args) throws Exception {
        MapList mlResultList = new MapList();
        String strPhysIdOfMBOM = DomainConstants.EMPTY_STRING;
        try {

            HashMap programMap = (HashMap) JPO.unpackArgs(args);

            if (null != programMap) {
                strPhysIdOfMBOM = (String) programMap.get("objectId");
            }
            FRCPSTree_mxJPO frcPSTree = new FRCPSTree_mxJPO(context, args);
            String strPhyIdOfPhysicalStructure = frcPSTree.getRootId(context, strPhysIdOfMBOM);
            DomainObject domObj = DomainObject.newInstance(context, strPhyIdOfPhysicalStructure);
            StringList productSelects = new StringList(2);
            productSelects.addElement(DomainConstants.SELECT_NAME);
            productSelects.addElement("majorrevision");
            Map mPart = domObj.getInfo(context, productSelects);
            String sVPMRefName = (String) mPart.get(DomainConstants.SELECT_NAME);
            String sVPMRefMajorRev = (String) mPart.get("majorrevision");
            BusinessObject boEngPart = new BusinessObject(DomainConstants.TYPE_PART, sVPMRefName, sVPMRefMajorRev, TigerConstants.VAULT_ESERVICEPRODUCTION);
            if (boEngPart.exists(context)) {
                Map tempMap = new HashMap();
                tempMap.put("id", boEngPart.getObjectId(context));
                mlResultList.add(tempMap);
            }
        } catch (Exception e) {
            logger.error("Error in getEBOMFromMBOM: ", e);
            throw e;
        }

        return mlResultList;

    }

    // TIGTK-6801:PKH:Phase-2.0:End

    // TIGTK-6812 | 20/06/2017 | Harika Varanasi : Starts
    /**
     * isPublishEBOMExistsBoolean method
     * @param context
     * @param args
     * @return boolean
     * @throws Exception
     * @author Harika Varanasi | 20/06/2017 | TIGTK-6812
     */

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public boolean isPublishEBOMExistsBoolean(Context context, String args[]) throws Exception {
        boolean bResult = false;
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strMBOMId = (String) programMap.get("objectId");
            Map ProgramMap = new HashMap();
            ProgramMap.put("objectId", strMBOMId);
            String strResult = isPublishEBOMExists(context, JPO.packArgs(ProgramMap));
            bResult = Boolean.parseBoolean(strResult);
        } catch (Exception ex) {
            logger.error("Error in getEBOMFromMBOM: ", ex);
        }
        return bResult;
    }

    // TIGTK-6812 | 20/06/2017 | Harika Varanasi : Ends

    public MapList getExpandMBOMWithMasterPlant(Context context, String[] args) throws Exception {

        long startTime;
        MapList finalMBOMList = new MapList();
        try {
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            String globalView = (String) paramMap.get("globalView");
            ContextUtil.startTransaction(context, false);
            String objectId = (String) paramMap.get("objectId");
            // String expandLevel = (String) paramMap.get("expandLevel");
            DomainObject domObj = DomainObject.newInstance(context, objectId);
            String strMasterPlantConnectedToIntermediateObject = pss.mbom.MBOMUtil_mxJPO.getMasterPlant(context, objectId);

            String strPCId = (String) paramMap.get("FRCExpressionFilterInput_OID");
            String strMasterPlantConnectedToRootObject = DomainConstants.EMPTY_STRING;
            String parentOID = (String) paramMap.get("parentOID");
            if (UIUtil.isNotNullAndNotEmpty(parentOID))
                strMasterPlantConnectedToRootObject = pss.mbom.MBOMUtil_mxJPO.getMasterPlant(context, parentOID);
            short expLvl = 0;// Default to Expand All = 0

            // Call Expand
            final StringList EXPD_BUS_SELECT_MBOM = new StringList(new String[] { "physicalid", "logicalid", "from[" + TigerConstants.RELATIONSHIP_DELFMIFUNCTIONIDENTIFIEDINSTANCE + "]",
                    "from[" + TigerConstants.RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS + "]" });
            // MapList res = getExpandMBOM(context, objectId, expLvl, filterExpression, filterValue, filterInput, EXPD_REL_SELECT, EXPD_BUS_SELECT_MBOM);

            MapList res = getExpandMBOMonPC(context, objectId, expLvl, strPCId, EXPD_REL_SELECT, EXPD_BUS_SELECT_MBOM);

            startTime = System.currentTimeMillis();

            // START UM5c06 : Build Paths and save theses in the return maps
            HashMap<String, String> mapPaths = new HashMap<String, String>();// Store path in a Map to be able to manage unsorted return MapList

            DomainObject domParentObj = new DomainObject(objectId);
            String rootPID = domParentObj.getInfo(context, "physicalid");
            mapPaths.put(rootPID, rootPID);

            // Declare variable before to improve prefs
            Map<String, Object> mapObj;
            String objPID, objFromPID, objPIDConnection;
            String newPath = "";
            // Modified for Findbug Issue : SIE
            if (res != null && !(res.isEmpty())) {
                for (int i = 0; i < res.size(); i++) {
                    mapObj = (Map) res.get(i);
                    objPID = (String) mapObj.get("physicalid");
                    objPIDConnection = (String) mapObj.get("physicalid[connection]");
                    objFromPID = (String) mapObj.get("from.physicalid");
                    newPath = mapPaths.get(objFromPID);
                    if (newPath != null && !newPath.isEmpty()) {
                        newPath = newPath + "/" + objPIDConnection;
                    } else {
                        newPath = objPID;
                    }
                    mapPaths.put(objPID, newPath);
                    mapObj.put("pathPID", newPath);

                    // set ids to use physicalids
                    mapObj.put("id", objPID);
                    mapObj.put("id[connection]", objPIDConnection);

                    // Add hasChildren info here
                    // TIGTK-9704 : Phase-2.0 : START
                    StringList mbomDerivativeList = new StringList();
                    mbomDerivativeList.add(TigerConstants.RELATIONSHIP_DELFMIFUNCTIONIDENTIFIEDINSTANCE);
                    mbomDerivativeList.add(TigerConstants.RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS);
                    String strValue = "False";

                    for (int j = 0; j < mbomDerivativeList.size(); j++) {
                        String strData = "from[" + mbomDerivativeList.get(j) + "]";
                        if (mapObj.containsKey(strData)) {
                            strValue = (String) mapObj.get(strData);
                            if (strValue.equalsIgnoreCase("True")) {
                                break;

                            }
                        }
                    }
                    mapObj.put("hasChildren", strValue);
                    // mapObj.put("hasChildren", EngineeringUtil.getHasChildren(mapObj, mbomDerivativeList));
                    // TIGTK-9704 : Phase-2.0 : END
                }
                // END UM5c06 : Build Paths and save theses in the return maps
                // Sort by TreeOrder "attribute[PLMInstance.V_TreeOrder].value"
                res.sortStructure("attribute[PLMInstance.V_TreeOrder].value", "ascending", "real");
            }
            // TIGTK-7887 : Expand Start
            if (res != null && !(res.isEmpty())) {
                int skipedLevel = 0;
                for (int i = 0; i < res.size(); i++) {
                    Map mStructureMap = (Map) res.get(i);

                    // TIGTK-10260:Start
                    if (UIUtil.isNotNullAndNotEmpty(globalView) && "true".equals(globalView)) {
                        finalMBOMList.add(mStructureMap);
                        // TIGTK-10260:End
                    } else {
                        if (strMasterPlantConnectedToRootObject.equalsIgnoreCase(strMasterPlantConnectedToIntermediateObject)) {
                            String strMasterPlantConnectedToChildObject = pss.mbom.MBOMUtil_mxJPO.getMasterPlant(context, (String) mStructureMap.get("physicalid"));
                            String level = (String) mStructureMap.get("level");
                            String strObjectId = (String) mStructureMap.get("physicalid");
                            DomainObject domObject = DomainObject.newInstance(context, strObjectId);
                            int levelInt = Integer.parseInt(level);

                            if (skipedLevel != 0 && skipedLevel < levelInt) {
                                continue;
                            } else if (!strMasterPlantConnectedToRootObject.equalsIgnoreCase(strMasterPlantConnectedToChildObject)) {

                                if (domObject.isKindOf(context, TigerConstants.TYPE_PROCESS_CONTINUOUS_CREATE_MATERIAL) || domObject.isKindOf(context, TigerConstants.TYPE_PROCESSCONTINUOUSPROVIDE)) {
                                    skipedLevel = 0;
                                    finalMBOMList.add(mStructureMap);
                                } else {
                                    skipedLevel = levelInt;
                                    finalMBOMList.add(mStructureMap);
                                }
                            } else {
                                skipedLevel = 0;
                                finalMBOMList.add(mStructureMap);
                            }
                        } else {
                            if (domObj.isKindOf(context, TigerConstants.TYPE_PROCESS_CONTINUOUS_CREATE_MATERIAL) || domObj.isKindOf(context, TigerConstants.TYPE_PROCESSCONTINUOUSPROVIDE)) {
                                skipedLevel = 0;
                                finalMBOMList.add(mStructureMap);
                            }
                            continue;
                        }
                    }
                }
            }
            // TIGTK-7887 : Expand End
            ContextUtil.commitTransaction(context);
        } catch (Exception ex) {
            logger.error("Error in getExpandMBOMWithMasterPlant: ", ex);
            if (context.isTransactionActive()) {
                ContextUtil.abortTransaction(context);
            }
            throw ex;
        }
        long endTime = System.currentTimeMillis();
        // TIGTK-7664 - 12-07-2017 - AM - START
        logger.info("FRC PERFOS : getExpandMBOM (without getVPMStructure) : ", (endTime - startTime));
        // TIGTK-7664 - 12-07-2017 - AM - End
        return finalMBOMList;
    }

    public StringList getExcludePlantList(Context context, String args[]) throws Exception {

        StringList listexcludePlantIDs = new StringList();
        PLMCoreModelerSession plmSession = null;
        boolean transactionActive = false;
        try {
            ContextUtil.startTransaction(context, true);
            transactionActive = true;
            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strPhysicalId = (String) programMap.get("objectId");

            List<String> listPlantIDs = FRCMBOMModelerUtility.getPlantsAttachedToMBOMReference(context, plmSession, strPhysicalId);
            if (!listPlantIDs.isEmpty()) {
                int listSize = listPlantIDs.size();
                for (int i = 0; i < listSize; i++) {
                    String strPlantPhysicalId = listPlantIDs.get(i);
                    String strPlantId = MqlUtil.mqlCommand(context, "print bus " + strPlantPhysicalId + " select id dump", false, false);
                    listexcludePlantIDs.add(strPlantId);
                }
            }
            flushAndCloseSession(plmSession);
            if (transactionActive)
                ContextUtil.commitTransaction(context);
        } catch (Exception ex) {
            logger.error("Error in getExcludePlantList: ", ex);
            flushAndCloseSession(plmSession);
            if (context.isTransactionActive()) {
                ContextUtil.abortTransaction(context);
            }
            throw ex;
        }
        return listexcludePlantIDs;
    }

    /**
     * This method is update the newly attached plant as Consumer
     * @param context
     * @param args
     * @throws Exception
     */

    public void getAttachedPlantAsConsumer(Context context, String mbomRefId, String plantId) throws Exception {
        PLMCoreModelerSession plmSession = null;
        boolean transactionActive = false;
        try {
            ContextUtil.startTransaction(context, true);
            transactionActive = true;
            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();
            String strlistPathIds = DomainConstants.EMPTY_STRING;
            StringList listMfgProuctionPlanningPID = new StringList();
            if (UIUtil.isNotNullAndNotEmpty(mbomRefId)) {
                String strquery = "query path type SemanticRelation containing " + mbomRefId + " where owner.type=='MfgProductionPlanning' select owner.physicalid dump |";
                strlistPathIds = MqlUtil.mqlCommand(context, strquery, false, false);
            }

            if (UIUtil.isNotNullAndNotEmpty(strlistPathIds)) {
                String[] strOwnerArray = strlistPathIds.split("\n");
                for (int i = 0; i < strOwnerArray.length; i++) {
                    String strPhysicalId = strOwnerArray[i];
                    listMfgProuctionPlanningPID.add(strPhysicalId);
                }
            }

            if (UIUtil.isNotNullAndNotEmpty(plantId)) {
                if (!listMfgProuctionPlanningPID.isEmpty()) {
                    for (int i = 0; i < listMfgProuctionPlanningPID.size(); i++) {
                        String strMfgProductionPlanning = (String) listMfgProuctionPlanningPID.get(i);
                        String strMfgPlanningId = strMfgProductionPlanning.split("\\|")[1];
                        DomainObject dMfgProductionPlanningObj = DomainObject.newInstance(context, strMfgPlanningId);
                        DomainObject dMBOMObj = DomainObject.newInstance(context, mbomRefId);
                        String strPlantFromMfgPlanning = dMfgProductionPlanningObj.getInfo(context, "to[VPLMrel/PLMConnection/V_Owner].from.physicalid");
                        String strPlantNameFromMfgPlanning = dMfgProductionPlanningObj.getInfo(context, "to[VPLMrel/PLMConnection/V_Owner].from.name");
                        if (plantId.equalsIgnoreCase(strPlantFromMfgPlanning)) {
                            List<String> slAttachedPlant = PSS_FRCMBOMModelerUtility_mxJPO.getPlantsAttachedToMBOMReference(context, plmSession, mbomRefId);
                            if (slAttachedPlant.size() > 1) {
                                // RFC-139 : Update the Master Plant Name on Add Plant
                                PropertyUtil.setGlobalRPEValue(context, MODIFYPLANT, "false");
                                dMfgProductionPlanningObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGPLANTEXTPSS_OWNERSHIP, "Consumer");
                            } else
                                dMBOMObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXTPSS_MATERPLANTNAME, strPlantNameFromMfgPlanning);
                        }
                    }
                }
            }
            flushAndCloseSession(plmSession);
            if (transactionActive)
                ContextUtil.commitTransaction(context);
        } catch (Exception ex) {
            logger.error("Error in getAttachedPlantAsConsumer: ", ex);
            flushAndCloseSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw ex;
        }
    }

    /**
     * Customized for WP3.12 MBO-155B and MBO-156
     * @param context
     * @param plmSession
     * @param psRefPID
     * @param mbomRefPID
     * @param newRefPIDList
     * @param plantPID
     * @return
     * @throws Exception
     */
    public static String getSynchedScopeMBOMRefFromPSRefForUpdateImplentLink(Context context, PLMCoreModelerSession plmSession, String psRefPID, String mbomRefPID, List<String> newRefPIDList,
            String plantPID) throws Exception {
        String returnMBOMRefPID = null;
        HashMap<String, String> attributes = new HashMap<String, String>();
        // Check if PS reference has a scope
        List<String> mbomRefPIDScopedWithPSRefList = PSS_FRCMBOMModelerUtility_mxJPO.getScopingReferencesFromList_PLMID(context, plmSession, psRefPID);

        if (mbomRefPIDScopedWithPSRefList.size() > 1) { // PS reference has multiple MBOM scopes
            // If all the elements of the list are within the same revision family, return the latest revision
            boolean isSameMajorIds = true;
            String lastMajorIdsStr = null;

            for (String refPLMID : mbomRefPIDScopedWithPSRefList) {
                String refPID = PLMID.buildFromString(refPLMID).getPid();
                String majorIdsStr = MqlUtil.mqlCommand(context, "print bus " + refPID + " select majorids dump |", false, false);

                if (lastMajorIdsStr != null) {
                    if (!lastMajorIdsStr.equals(majorIdsStr))
                        isSameMajorIds = false;
                }

                lastMajorIdsStr = majorIdsStr;
            }

            if (isSameMajorIds) {
                String[] lastMajorIds = lastMajorIdsStr.split("\\|");

                // Return this MBOM scope
                returnMBOMRefPID = lastMajorIds[lastMajorIds.length - 1];

                // Check if it is a provide
                String returnMBOMRefType = MqlUtil.mqlCommand(context, "print bus " + returnMBOMRefPID + " select type dump |", false, false);

                boolean isDirect = false;

                for (String typeInList : baseTypesForMBOMLeafNodes) {
                    if (getDerivedTypes(context, typeInList).contains(returnMBOMRefType))
                        isDirect = true;
                }

                if (!isDirect)
                    throw new Exception("The Part (" + psRefPID + ") is already scoped with a Manufacturing Item (" + returnMBOMRefPID + ") which is not a Provide or a Create Material.");

                // List<String> lastMbomRefPIDScopedWithPSRefList = new ArrayList<String>();
                // lastMbomRefPIDScopedWithPSRefList.add(returnMBOMRefPID);
                returnMBOMRefPID = getMBOMReferenceToReuse(context, plmSession, mbomRefPIDScopedWithPSRefList, plantPID);
                if (UIUtil.isNullOrEmpty(returnMBOMRefPID)) {
                    returnMBOMRefPID = createMBOMReferenceWithPlant(context, plmSession, "CreateMaterial", null, attributes, psRefPID, plantPID);
                    // Create a scope link between PS reference and MBOM reference
                    PSS_FRCMBOMModelerUtility_mxJPO.createScopeLinkAndReduceChildImplementLinks(context, plmSession, returnMBOMRefPID, psRefPID, false);
                }
            } else {
                // PSS: START
                returnMBOMRefPID = getMBOMReferenceToReuse(context, plmSession, mbomRefPIDScopedWithPSRefList, plantPID);
                // if (returnMBOMRefPID == null) {
                // throw new Exception("You cannot provide this part from the EBOM : it has multiple scopes !");
                // }
                if (UIUtil.isNullOrEmpty(returnMBOMRefPID)) {
                    returnMBOMRefPID = createMBOMReferenceWithPlant(context, plmSession, "CreateMaterial", null, attributes, psRefPID, plantPID);
                    // Create a scope link between PS reference and MBOM reference
                    PSS_FRCMBOMModelerUtility_mxJPO.createScopeLinkAndReduceChildImplementLinks(context, plmSession, returnMBOMRefPID, psRefPID, false);
                }
                // throw new Exception("You cannot provide this part from the EBOM : it has multiple scopes !");
                // PSS : END
            }
        } else if (mbomRefPIDScopedWithPSRefList.size() == 1) { // PS reference has already one MBOM scope

            // PSS: START
            // returnMBOMRefPID = createMBOMReference(context, plmSession, "Provide", null, attributes);
            returnMBOMRefPID = getMBOMReferenceToReuse(context, plmSession, mbomRefPIDScopedWithPSRefList, plantPID);
            if (returnMBOMRefPID == null) {
                returnMBOMRefPID = createMBOMReferenceWithPlant(context, plmSession, "CreateMaterial", null, attributes, psRefPID, plantPID);
                // FRC: Fixed Bug#497-MBOM "Description" attribute not filled automatically
                String psRefDescription = MqlUtil.mqlCommand(context, "print bus " + psRefPID + " select attribute[PLMEntity.V_description].value dump |", false, false);
                MqlUtil.mqlCommand(context, "mod bus " + returnMBOMRefPID + " PLMEntity.V_description '" + psRefDescription + "'", false, false);
                String psRefApplicabilityDate = MqlUtil.mqlCommand(context, "print bus " + psRefPID + " select attribute[PLMReference.V_ApplicabilityDate].value dump |", false, false);
                MqlUtil.mqlCommand(context, "mod bus " + returnMBOMRefPID + " PLMReference.V_ApplicabilityDate '" + psRefApplicabilityDate + "'", false, false);

                // Create a scope link between PS reference and MBOM reference
                PSS_FRCMBOMModelerUtility_mxJPO.createScopeLinkAndReduceChildImplementLinks(context, plmSession, returnMBOMRefPID, psRefPID, false);
            }
            // Return this MBOM scope
            // returnMBOMRefPID = mbomRefPIDScopedWithPSRefList.get(0);
            // PSS: END

            // Check if it is a provide
            String returnMBOMRefType = MqlUtil.mqlCommand(context, "print bus " + returnMBOMRefPID + " select type dump |", false, false);

            boolean isDirect = false;

            for (String typeInList : baseTypesForMBOMLeafNodes) {
                if (getDerivedTypes(context, typeInList).contains(returnMBOMRefType))
                    isDirect = true;
            }

            if (!isDirect)
                throw new Exception("The Part (" + psRefPID + ") is already scoped with a Manufacturing Item (" + returnMBOMRefPID + ") which is not a Provide or a Create Material.");
        } else { // PS reference does not already have an MBOM scope
            // Get the previous revision of the PS reference
            // String previousRevPSRefPID = MqlUtil.mqlCommand(context, "print bus " + psRefPID + " select previous.physicalid dump |", false, false);

            String previousRevPSRefPID = "";
            // Bypass for data coming from publish (which does not conatine all sequance info on revision

            StringList objSelectable = new StringList(4);
            objSelectable.addElement(DomainConstants.SELECT_TYPE);
            objSelectable.addElement(DomainConstants.SELECT_REVISION);
            objSelectable.addElement(DomainConstants.SELECT_NAME);
            objSelectable.addElement("attribute[PLMReference.V_order]");

            DomainObject dObj = new DomainObject(psRefPID);
            Map objMap = dObj.getInfo(context, objSelectable);
            String sType = (String) objMap.get(DomainConstants.SELECT_TYPE);
            String sName = (String) objMap.get(DomainConstants.SELECT_NAME);
            String V_Order = (String) objMap.get("attribute[PLMReference.V_order]");
            // V_Order is not dependent of the revision sequence of the policy

            int vorder = Integer.parseInt(V_Order);
            if (vorder > 1) {
                StringList objSelects = new StringList(1);

                objSelects.addElement("physicalid");
                String sWhere = "attribute[PLMReference.V_order] ==" + Integer.toString((vorder - 1));
                MapList mlList = DomainObject.findObjects(context, sType, sName, DomainConstants.QUERY_WILDCARD, DomainConstants.QUERY_WILDCARD, TigerConstants.VAULT_VPLM, sWhere, true, objSelects);
                if (null != mlList) {
                    for (int n = 0; n < mlList.size(); n++) {
                        Map tempObj = (Map) mlList.get(n);
                        previousRevPSRefPID = (String) tempObj.get("physicalid");
                    }
                }
            }

            if ("".equals(previousRevPSRefPID)) { // PS reference does not have a previous revision
                String strRPEValueId = PropertyUtil.getRPEValue(context, MBOMDONOTREUSE, true);
                if (mbomRefPID == null || strRPEValueId.equalsIgnoreCase("true")) { // MBOM reference is null
                    // Create a new Provide and return it
                    // HashMap programMap = new HashMap();
                    // programMap.put("TypeActual", "Provide");
                    // programMap.put("FRCMBOMGetChangeObject", changeObjectPID);

                    // PSS: START
                    // returnMBOMRefPID = createMBOMReference(context, plmSession, "Provide", null, attributes);
                    returnMBOMRefPID = getMBOMReferenceToReuse(context, plmSession, mbomRefPIDScopedWithPSRefList, plantPID);
                    if (returnMBOMRefPID == null) {
                        returnMBOMRefPID = createMBOMReferenceWithPlant(context, plmSession, "CreateMaterial", null, attributes, psRefPID, plantPID);
                        // FRC: Fixed Bug#497-MBOM "Description" attribute not filled automatically
                        String psRefDescription = MqlUtil.mqlCommand(context, "print bus " + psRefPID + " select attribute[PLMEntity.V_description].value dump |", false, false);
                        MqlUtil.mqlCommand(context, "mod bus " + returnMBOMRefPID + " PLMEntity.V_description '" + psRefDescription + "'", false, false);
                        String psRefApplicabilityDate = MqlUtil.mqlCommand(context, "print bus " + psRefPID + " select attribute[PLMReference.V_ApplicabilityDate].value dump |", false, false);
                        MqlUtil.mqlCommand(context, "mod bus " + returnMBOMRefPID + " PLMReference.V_ApplicabilityDate '" + psRefApplicabilityDate + "'", false, false);

                        // By default, the Title on the new Provide should be the same as the Title of the VPMReference linked to it
                        String psRefTitle = MqlUtil.mqlCommand(context, "print bus " + psRefPID + " select attribute[PLMEntity.V_Name].value dump |", false, false);
                        MqlUtil.mqlCommand(context, "mod bus " + returnMBOMRefPID + " PLMEntity.V_Name '" + psRefTitle + "'", false, false);
                    }
                    // PSS: END

                    newRefPIDList.add(returnMBOMRefPID);
                    // String[] createArgs = JPO.packArgs(programMap);
                    // Map<String, String> newProvideMap = createNewManufItem(context, createArgs);
                    // returnMBOMRefPID = newProvideMap.get("id");

                    // Create a scope link between PS reference and MBOM reference
                    PSS_FRCMBOMModelerUtility_mxJPO.createScopeLinkAndReduceChildImplementLinks(context, plmSession, returnMBOMRefPID, psRefPID, false);
                } else { // MBOM reference is not null
                    List<String> inputListForGetScope = new ArrayList<String>();
                    inputListForGetScope.add(mbomRefPID);
                    String psRefScopePID = PSS_FRCMBOMModelerUtility_mxJPO.getScopedPSReferencePIDFromList(context, plmSession, inputListForGetScope).get(0);

                    if (psRefScopePID != null && !"".equals(psRefScopePID)) { // MBOM reference has already a PS scope : throw a new exception
                        throw new Exception("This MBOM node already has a scope, and it is not the EBOM part you are providing !");
                    } else { // MBOM reference does not already have a scope
                        // Return the MBOM reference
                        returnMBOMRefPID = mbomRefPID;

                        // Create a scope link between PS reference and MBOM reference
                        PSS_FRCMBOMModelerUtility_mxJPO.createScopeLinkAndReduceChildImplementLinks(context, plmSession, returnMBOMRefPID, psRefPID, false);
                    }
                }
            } else { // PS reference has a previous revision
                // Recursive call on previous revision (with MBOM reference in parameter)
                String mbomRefPIDSynchedToPreviousPSRevision = getSynchedScopeMBOMRefFromPSRefForUpdateImplentLink(context, plmSession, previousRevPSRefPID, mbomRefPID, newRefPIDList, plantPID);

                // New revision on the MBOM reference returned by the recursive call and return this new MBOM reference
                returnMBOMRefPID = newRevisionMBOMReference(context, plmSession, mbomRefPIDSynchedToPreviousPSRevision);

                newRefPIDList.add(returnMBOMRefPID);
                try {
                    plmSession.flushSession();
                } catch (Exception e) {
                }

                // !! CAREFULL : remove the scope on the new revision of the MBOM reference (by default, the new revision duplicates the scope)
                List<String> modifiedInstanceList = PSS_FRCMBOMModelerUtility_mxJPO.deleteScopeLinkAndDeleteChildImplementLinks(context, plmSession, returnMBOMRefPID, false);
                logger.info("FRC : modified Instances : ", modifiedInstanceList);
                try {
                    plmSession.flushSession();
                } catch (Exception e) {
                }

                // Create a scope link between the PS reference and the new MBOM reference revision
                PSS_FRCMBOMModelerUtility_mxJPO.createScopeLinkAndReduceChildImplementLinks(context, plmSession, returnMBOMRefPID, psRefPID, false);
                // Map the attributes
                String psRefInfoStr = MqlUtil.mqlCommand(context,
                        "print bus " + psRefPID + " select attribute[PLMEntity.V_Name].value attribute[PLMEntity.V_description].value attribute[PLMReference.V_ApplicabilityDate].value dump |", false,
                        false);
                String[] psRefInfo = psRefInfoStr.split("\\|", -2);

                if ("".equals(psRefInfo[2]))
                    MqlUtil.mqlCommand(context, "mod bus " + returnMBOMRefPID + " PLMEntity.V_Name '" + psRefInfo[0] + "' PLMEntity.V_description '" + psRefInfo[1] + "'", false, false);
                else
                    MqlUtil.mqlCommand(context, "mod bus " + returnMBOMRefPID + " PLMEntity.V_Name '" + psRefInfo[0] + "' PLMEntity.V_description '" + psRefInfo[1]
                            + "' PLMReference.V_ApplicabilityDate '" + psRefInfo[2] + "'", false, false);
            }
        }

        return returnMBOMRefPID;
    }

    public MapList getPSInstanceList(Context context, String args[]) throws Exception {
        try {
            String psRefID = args[0];
            StringList busSelect = new StringList();
            busSelect.add("physicalid");
            StringList relSelect = new StringList();
            relSelect.add("physicalid[connection]");
            relSelect.add("attribute[PLMInstance.V_TreeOrder].value");
            MapList psInstList = PSS_FRCMBOMModelerUtility_mxJPO.getVPMStructureMQL(context, psRefID, busSelect, relSelect, (short) 1, null,
                    "(attribute[PSS_PublishedEBOM.PSS_InstanceName]==EBOM || attribute[PSS_PublishedEBOM.PSS_InstanceName]=='')");
            return psInstList;
        } catch (Exception ex) {
            logger.error("Error in getPSInstanceList : ", ex);
            throw ex;
        }
    }

    // TIGTK-6773 |03/07/2017 | Harika Varanasi : Starts

    /**
     * cancelTool method
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author Harika Varanasi | TIGTK-6773 | 03/07/2017
     */
    public void cancelTool(Context context, String args[]) throws Exception {
        boolean bContext = false;
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strOriginalToolId = (String) programMap.get("objectId");
            DomainObject domObject = DomainObject.newInstance(context, strOriginalToolId);
            String strCurrentState = domObject.getInfo(context, DomainConstants.SELECT_CURRENT);
            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            bContext = true;
            if ("InWork".equalsIgnoreCase(strCurrentState) || "Review".equalsIgnoreCase(strCurrentState)) {
                domObject.setAttributeValue(context, TigerConstants.ATTRIBUTE_BRANCH_TO, "Cancelled");
            }
            domObject.promote(context);

        } catch (Exception e) {
            logger.error("Error in cancelTool: ", e);
        } finally {
            if (bContext) {
                ContextUtil.popContext(context);
            }
        }
    }

    /**
     * connectReferenceDocumentsToTool method
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author Harika Varanasi | TIGTK-6773 | 03/07/2017
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void connectReferenceDocumentsToTool(Context context, String args[]) throws Exception {
        boolean bContext = false;
        try {
            // String strEvent = args[0];
            String strOriginalId = args[0];
            String strClonedIOrRevisedId = args[1];
            String strPolicy = args[2];
            if (UIUtil.isNotNullAndNotEmpty(strPolicy) && TigerConstants.POLICY_PSS_TOOL.equalsIgnoreCase(strPolicy)) {
                Map programMap = new HashMap();
                programMap.put("objectId", strOriginalId);
                String[] methodargs = JPO.packArgs(programMap);
                VPLMDocument_mxJPO VPLMDocument = new VPLMDocument_mxJPO(context, null);
                MapList mlDocuments = (MapList) VPLMDocument.getDocuments(context, methodargs);
                // TIGTK-9527:Rutuja Ekatpure:24/8/2017:Start
                if (mlDocuments != null && !mlDocuments.isEmpty()) {
                    // TIGTK-9527:Rutuja Ekatpure:24/8/2017:End
                    ContextUtil.startTransaction(context, true);
                    int mlSize = mlDocuments.size();
                    String[] strDocumentIds = new String[mlSize];
                    for (int i = 0; i < mlSize; i++) {
                        Map objMap = (Map) mlDocuments.get(i);
                        strDocumentIds[i] = (String) objMap.get(DomainConstants.SELECT_ID);
                    }
                    Map programAttachMap = new HashMap();
                    programAttachMap.put("objectId", strClonedIOrRevisedId);
                    programAttachMap.put("documentIds", strDocumentIds);
                    String[] methodAttachAgs = JPO.packArgs(programAttachMap);
                    VPLMDocument.attachDocuments(context, methodAttachAgs);
                    ContextUtil.commitTransaction(context);
                }
            }

        } catch (Exception e) {
            logger.error("Error in connectReferenceDocumentsToTool : ", e);
            ContextUtil.abortTransaction(context);
        } finally {
            if (bContext) {
                ContextUtil.popContext(context);
            }
        }
    }

    /**
     * getObjectMajorRevisions
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author Harika Varanasi | 03/07/2017 | Harika Varanasi
     */

    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getObjectMajorRevisions(Context context, String[] args) throws Exception {
        MapList revisionList = null;
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String objectId = (String) programMap.get("objectId");
            DomainObject domObj = DomainObject.newInstance(context, objectId);
            String strObjName = domObj.getInfo(context, DomainConstants.SELECT_NAME);
            StringList busSelects = new StringList(1);
            busSelects.add(DomainConstants.SELECT_ID);
            String strWhere = "minorrevision == 1";

            revisionList = DomainObject.findObjects(context, TigerConstants.TYPE_VPMREFERENCE, // Type Pattern
                    strObjName, // Name Pattern
                    DomainConstants.QUERY_WILDCARD, // Rev Pattern
                    null, // Owner Pattern
                    TigerConstants.VAULT_VPLM, // Vault Pattern
                    strWhere, // Where Expression
                    false, // Expand Type
                    busSelects); // Object Pattern
        } catch (Exception ex) {
            throw ex;
        }
        return revisionList;
    }

    /**
     * updateToolAttributeBranchTo method
     * @param context
     * @param args
     * @throws Exception
     * @author Harika Varanasi | 04/07/2017 | Harika Varanasi
     */
    public void updateToolAttributeBranchTo(Context context, String args[]) throws Exception {
        boolean bContext = false;
        try {

            String strToolId = args[0];
            if (UIUtil.isNotNullAndNotEmpty(strToolId)) {
                DomainObject domObject = DomainObject.newInstance(context, strToolId);
                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                bContext = true;
                domObject.setAttributeValue(context, TigerConstants.ATTRIBUTE_BRANCH_TO, "None");
            }

        } catch (Exception e) {
            logger.error("Error in updateToolAttributeBranchTo: ", e);
        } finally {
            if (bContext) {
                ContextUtil.popContext(context);
            }
        }
    }

    /**
     * updateToolOwnerValue method
     * @param context
     * @param args
     * @throws Exception
     * @author Harika Varanasi for TIGTK-6773
     */
    public void updateToolOwnerValue(Context context, String[] args) throws Exception {
        try {

            String strObjectId = args[0];
            String strNewObjectId = args[1];
            String strPolicy = args[2];
            if (UIUtil.isNotNullAndNotEmpty(strObjectId) && UIUtil.isNotNullAndNotEmpty(strNewObjectId) && UIUtil.isNotNullAndNotEmpty(strPolicy) && TigerConstants.POLICY_PSS_TOOL.equals(strPolicy)) {
                String strCurrentUser = PropertyUtil.getRPEValue(context, TOOLREVISEACTION, false);
                if (UIUtil.isNotNullAndNotEmpty(strCurrentUser)) {
                    if (UIUtil.isNotNullAndNotEmpty(strNewObjectId)) {
                        DomainObject dObj = DomainObject.newInstance(context, strNewObjectId);
                        dObj.setOwner(context, strCurrentUser);
                    }
                }

            }
        } catch (Exception e) {
            logger.error("Error in updateToolOwnerValue: ", e);
            throw e;
        }
    }

    /**
     * reviseTool method TIGTK-6773
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author Harika Varanasi for TIGTK-6773
     */
    public String reviseTool(Context context, String args[]) throws Exception {
        String strObjectId = DomainObject.EMPTY_STRING;
        String strCurrentUser = DomainConstants.EMPTY_STRING;
        try {
            strCurrentUser = context.getUser();
            PropertyUtil.setRPEValue(context, TOOLREVISEACTION, strCurrentUser, true);
            strObjectId = replaceNewRevisionTool(context, args);
            if (UIUtil.isNotNullAndNotEmpty(strObjectId)) {
                DomainObject dObj = DomainObject.newInstance(context, strObjectId);
                dObj.setOwner(context, strCurrentUser);
            }
        } catch (Exception e) {
            logger.error("Error in reviseTool: ", e);
        } finally {
            PropertyUtil.setRPEValue(context, TOOLREVISEACTION, strCurrentUser, true);
        }
        return strObjectId;
    }

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author Harika Varanasi for TIGTK-6773
     */
    public String replaceNewRevisionTool(Context context, String[] args) throws Exception { // Called from FRCReplaceNewRevisionPostProcess.jsp
        String returnValue = "";

        PLMCoreModelerSession plmSession = null;
        try {
            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            String childRefPID = args[0];
            String newChildRefPID = newRevisionToolReference(context, plmSession, childRefPID);

            returnValue = newChildRefPID;

            flushAndCloseSession(plmSession);

        } catch (Exception exp) {
            logger.error("Error in replaceNewRevisionTool : ", exp);
            flushAndCloseSession(plmSession);
            throw exp;
        }

        return returnValue;
    }

    /**
     * newRevisionToolReference
     * @param context
     * @param plmSession
     * @param refPID
     * @return
     * @throws Exception
     * @author Harika Varanasi for TIGTK-6773
     */
    public String newRevisionToolReference(Context context, PLMCoreModelerSession plmSession, String refPID) throws Exception {
        // Get the latest existing revision (the one given is not necessarily the latest one)
        String latestRevisionPIDsStr = MqlUtil.mqlCommand(context, "print bus " + refPID + " select majorids.lastmajorid dump |", false, false);
        String[] latestRevisionPIDs = latestRevisionPIDsStr.split("\\|");
        String latestRevisionPID = latestRevisionPIDs[0];

        String latestRevisionPID1 = FRCMBOMModelerAPI.newRevisionManufItem(context, latestRevisionPID);
        return latestRevisionPID1;
    }

    // TIGTK-6773 |03/07/2017 | Harika Varanasi : Ends
    // TIGTK-8562:Rutuja Ekatpure:11/7/2017:Start
    /***
     * this method used to get master plant present on parent for display as default plant on MBOM creation.
     * @param context
     * @param args
     *            parent object id
     * @return string containing plant id and plant name
     * @throws Exception
     */
    public static String getParentPlant(Context context, String[] args) throws Exception {
        String strResult = DomainConstants.EMPTY_STRING;
        try {
            // get master plant present on parent MBOM
            String strMasterPlant = (String) pss.mbom.MBOMUtil_mxJPO.getMasterPlant(context, args[0]);
            if (UIUtil.isNotNullAndNotEmpty(strMasterPlant)) {
                DomainObject domMasterPlant = new DomainObject(strMasterPlant);
                StringList slObjectSel = new StringList(2);
                slObjectSel.add(DomainObject.SELECT_ID);
                slObjectSel.add(DomainObject.SELECT_NAME);
                Map<?, ?> mapObjSelect = (Map<?, ?>) domMasterPlant.getInfo(context, slObjectSel);
                strResult = mapObjSelect.get(DomainObject.SELECT_NAME) + "=" + mapObjSelect.get(DomainObject.SELECT_ID);
            }
        } catch (Exception e) {
            logger.error("Error in getParentPlant: ", e);
        }
        return strResult;
    }

    // TIGTK-8562:Rutuja Ekatpure:11/7/2017:End

    // TIGER 2.0 Update MBOM:VB:14/7/2017:Start

    /**
     * This method update the MBOM structure on basis of EBOM structure
     * @param context
     * @param args
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public static void updateMBOMBasedOnPSUpdate(Context context, String[] args) throws Exception {
        PLMCoreModelerSession plmSession = null;
        boolean isTransactionActive = false;
        try {
            ContextUtil.startTransaction(context, true);
            isTransactionActive = true;

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            String psRootPID = args[0];
            String mbomRootPID = args[1];
            String mbomInstancePhysicalId = DomainConstants.EMPTY_STRING;
            String psInstancePhysicalId = DomainConstants.EMPTY_STRING;

            PSS_MBOMUpdate_mxJPO.updateMBOMColorOptions(context, psRootPID, mbomRootPID);
            PSS_MBOMUpdate_mxJPO.updateMBOMSpare(context, psRootPID, mbomRootPID);
            PSS_MBOMUpdate_mxJPO.updateMBOMTooling(context, psRootPID, mbomRootPID);
            PSS_MBOMUpdate_mxJPO.updateMBOMAttributes(context, psRootPID, mbomRootPID, psInstancePhysicalId, mbomInstancePhysicalId);
            PSS_MBOMUpdate_mxJPO.updateMBOMMaterial(context, psRootPID, mbomRootPID);

            DomainObject domMBOM = DomainObject.newInstance(context, mbomRootPID);
            domMBOM.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_AVAILABLE_UPDATEFLAG, "No");

            StringList busSelect = new StringList();
            busSelect.add("physicalid");
            StringList relSelect = new StringList();
            relSelect.add("physicalid[connection]");

            MapList mbomMapList = PSS_FRCMBOMModelerUtility_mxJPO.getVPMStructure(context, plmSession, mbomRootPID, busSelect, relSelect, (short) 0, null, null);

            for (int i = 0; i < mbomMapList.size(); i++) {
                Map mapObj = (Map) mbomMapList.get(i);
                String objPID = (String) mapObj.get("physicalid");
                String objPIDConnection = (String) mapObj.get("physicalid[connection]");
                DomainObject domMBOMObject = DomainObject.newInstance(context, objPID);
                String strType = (String) domMBOMObject.getInfo(context, DomainConstants.SELECT_TYPE);
                boolean checkOfMaterialTypeCombination = false;
                if (!domMBOMObject.isKindOf(context, TigerConstants.TYPE_PROCESSCONTINUOUSPROVIDE) && !domMBOMObject.isKindOf(context, TigerConstants.TYPE_PROCESS_CONTINUOUS_CREATE_MATERIAL)) {
                    DomainRelationship domMBOMRel = DomainRelationship.newInstance(context, objPIDConnection);
                    String strUpdateFlag = domMBOMRel.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_AVAILABLE_INSTANCEUPDATEFLAG);
                    List<String> newImplementLinkPIDList = PSS_FRCMBOMModelerUtility_mxJPO.getImplementLinkInfoSimple(context, plmSession, objPIDConnection);
                    if (newImplementLinkPIDList != null && newImplementLinkPIDList.size() > 0) {
                        String psLeafInstPID = newImplementLinkPIDList.get(newImplementLinkPIDList.size() - 1);
                        String psLeafRefPID = MqlUtil.mqlCommand(context, "print connection " + psLeafInstPID + " select to.physicalid dump |", false, false);
                        if (strUpdateFlag.equalsIgnoreCase("Yes")) {
                            PSS_MBOMUpdate_mxJPO.updateMBOMEffectivities(context, psLeafInstPID, objPIDConnection);
                            if (UIUtil.isNotNullAndNotEmpty(psLeafRefPID) && UIUtil.isNotNullAndNotEmpty(objPID)) {
                                PSS_MBOMUpdate_mxJPO.updateMBOMColorOptions(context, psLeafRefPID, objPID);
                                PSS_MBOMUpdate_mxJPO.updateMBOMAttributes(context, psLeafRefPID, objPID, psLeafInstPID, objPIDConnection);
                                PSS_MBOMUpdate_mxJPO.updateMBOMAlternate(context, psLeafRefPID, objPID, psLeafInstPID, objPIDConnection);
                                PSS_MBOMUpdate_mxJPO.updateMBOMTooling(context, psLeafRefPID, objPID);
                                PSS_MBOMUpdate_mxJPO.updateMBOMSpare(context, psLeafRefPID, objPID);
                                checkOfMaterialTypeCombination = PSS_MBOMUpdate_mxJPO.updateMBOMMaterial(context, psLeafRefPID, objPID);
                                if (checkOfMaterialTypeCombination == true) {
                                    domMBOMRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_AVAILABLE_INSTANCEUPDATE, "Material Update to be performed manually");
                                } else
                                    domMBOMRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_AVAILABLE_INSTANCEUPDATE, "");
                            }
                            domMBOMRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_AVAILABLE_INSTANCEUPDATEFLAG, "No");
                        } else {
                            checkOfMaterialTypeCombination = PSS_MBOMUpdate_mxJPO.updateMBOMMaterial(context, psLeafRefPID, objPID);
                            if (checkOfMaterialTypeCombination == true) {
                                domMBOMRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_AVAILABLE_INSTANCEUPDATE, "Material Update to be performed manually");
                            } else
                                domMBOMRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_AVAILABLE_INSTANCEUPDATE, "");
                        }
                    }
                }
            }

            flushAndCloseSession(plmSession);
            if (isTransactionActive) {
                ContextUtil.commitTransaction(context);
            }
        } catch (Exception ex) {
            logger.error("Error in updateMBOMBasedOnPSUpdate : ", ex);
            flushAndCloseSession(plmSession);
            if (isTransactionActive) {
                ContextUtil.abortTransaction(context);
            }
            throw ex;
        }
    }

    // TIGTK-7259: TS :17/7/2017:START
    public static void createNewRootScope(Context context, String[] args) throws Exception { // Called by FRCCreateNewScopePostProcess.jsp
        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            String mbomRefID = args[0];
            String psRefID = args[1];

            // String mbomRefPID = MqlUtil.mqlCommand(context, "print bus " + mbomRefID + " select physicalid dump |", false, false);
            String psRefPID = MqlUtil.mqlCommand(context, "print bus " + psRefID + " select physicalid dump |", false, false);

            // List<String> modifiedInstancePIDList = PSS_FRCMBOMModelerUtility_mxJPO.createScopeLinkAndReduceChildImplementLinks(context, plmSession, mbomRefPID, psRefPID, true);

            // On the MBOM node, set the same model as config context.
            List<String> listModelsPIDOnPSRef = FRCMBOMModelerUtility.getAttachedModelsOnReference(context, plmSession, psRefPID);

            if (listModelsPIDOnPSRef.size() > 1)
                throw new Exception("This part has multiple configuration context.");
            else if (listModelsPIDOnPSRef.size() == 1) {
                List<String> mbomRefIDList = new ArrayList<String>();
                mbomRefIDList.add(mbomRefID);

                FRCMBOMModelerUtility.attachModelsOnReferencesAndSetOptionsCriteria(context, plmSession, mbomRefIDList, listModelsPIDOnPSRef);
            }

            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);
            // ContextUtil.abortTransaction(context);
        } catch (Exception exp) {
            logger.error("Error in createNewRootScope : ", exp);
            flushAndCloseSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw exp;
        }
    }

    // TIGTK-7259: TS :17/7/2017:END

    public static Vector getImplementColumn(Context context, String[] args) throws Exception { // Called by table FRCMBOMTable (column FRCMBOMCentral.MBOMTableColumnImplement)
        long startTime = System.currentTimeMillis();

        String strMaterialNotMatch = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.Message.SpecificMaterialNotMatch");

        String strMaterialNotLink = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.Message.SpecificMaterialNotLink");

        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, false);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            // Create result vector
            Vector vecResult = new Vector();

            // Get object list information from packed arguments
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");
            // Do for each object
            List<String> listInstIDs = new ArrayList<String>();
            List<String> listRefIDs = new ArrayList<String>();
            for (Iterator itrObjects = objectList.iterator(); itrObjects.hasNext();) {
                Map mapObjectInfo = (Map) itrObjects.next();

                String manuItemInstID = (String) mapObjectInfo.get("id[connection]");
                String manuItemRefID = (String) mapObjectInfo.get("id");

                if (manuItemInstID != null && !"".equals(manuItemInstID)) {
                    listInstIDs.add(manuItemInstID);
                }

                listRefIDs.add(manuItemRefID);
            }

            List<String> resScope = FRCMBOMModelerUtility.getScopedPSReferencePIDFromList(context, plmSession, listRefIDs);

            List<Map<String, Object>> resImp = null;
            if (listInstIDs.size() > 0) {
                try {
                    resImp = FRCMBOMModelerUtility.getImplementLinkInfoWithEffStatus(context, plmSession, listInstIDs, true);
                } catch (Exception e) {
                    resImp = FRCMBOMModelerUtility.getImplementLinkInfoWithEffStatus(context, plmSession, listInstIDs, false);
                }
            }
            // Get all the instance names without looking twice for the same instance
            Set<String> psInstancePIDList = new HashSet<String>();
            if (resImp != null) {
                for (Map<String, Object> linkInfo : resImp) {
                    List<String> ilPathPIDList = (List<String>) linkInfo.get("PIDList");
                    if (ilPathPIDList != null) {
                        for (int j = 0; j < ilPathPIDList.size(); j++) {
                            String physicalId = ilPathPIDList.get(j);
                            psInstancePIDList.add(physicalId);
                        }
                    }
                }
            }

            Map<String, String> psInstanceNames = new HashMap<String, String>();

            StringList select = new StringList();
            select.add("attribute[PLMInstance.PLM_ExternalID].value");
            MapList resultInfoML = DomainRelationship.getInfo(context, psInstancePIDList.toArray(new String[0]), select);
            int index = 0;
            for (String pid : psInstancePIDList) {
                Map<String, String> resultInfoMap = (Map<String, String>) resultInfoML.get(index);
                index++;
                psInstanceNames.put(pid, resultInfoMap.get("attribute[PLMInstance.PLM_ExternalID].value"));
            }

            // Do for each object
            int resScopeIndex = 0;
            int resImpIndex = 0;
            for (Iterator itrObjects = objectList.iterator(); itrObjects.hasNext();) {
                StringBuffer cellContentSB = new StringBuffer("");

                Map mapObjectInfo = (Map) itrObjects.next();

                String productRefPID = resScope.get(resScopeIndex);
                String level = (String) mapObjectInfo.get("id[level]");
                if (level == null || "".equals(level))
                    level = (String) mapObjectInfo.get("level");

                String manuItemInstID = (String) mapObjectInfo.get("id[connection]");

                resScopeIndex++;

                if (resImp != null) {
                    Map<String, Object> linkInfo = null;
                    if (UIUtil.isNotNullAndNotEmpty(manuItemInstID)) {
                        linkInfo = resImp.get(resImpIndex);
                        resImpIndex++;
                    }

                    if (linkInfo != null) {
                        List<String> ilPathPIDList = (List<String>) linkInfo.get("PIDList");
                        List<String> ilPathLIDList = (List<String>) linkInfo.get("LIDList");
                        // Boolean ilPathEffSynch = (Boolean) linkInfo.get("EffSynch");

                        if (ilPathPIDList != null && ilPathLIDList != null) {
                            StringBuilder sbPathPID = new StringBuilder();
                            StringBuilder sbPathLID = new StringBuilder();

                            StringBuffer cellDisplaySB = new StringBuffer("");

                            String lastPID = "";

                            for (int j = 0; j < ilPathPIDList.size(); j++) {
                                // Map elemPath = (Map) mlElemsPathImplement.get(j);
                                // String physicalId = (String) elemPath.get("physicalid");
                                // String logicalId = (String) elemPath.get("logicalid");

                                String physicalId = ilPathPIDList.get(j);
                                String logicalId = ilPathLIDList.get(j);

                                String productInstName = psInstanceNames.get(physicalId);
                                if (productInstName == null || "".equals(productInstName))
                                    productInstName = "???";

                                /*
                                 * try { // Redmine Issue #221 : HE5 : Changed the original code to fix the error on broken implement link to physical product for EBOM : 16/02/2016 : START //
                                 * productInstName = MqlUtil.mqlCommand(context, "print connection " + physicalId + " select attribute[PLMInstance.PLM_ExternalID].value dump |", false, false);
                                 * //String tempStr = MqlUtil.mqlCommand(context, "query connection where physicalid==" + physicalId + " select attribute[PLMInstance.PLM_ExternalID].value dump |",
                                 * false, false); String tempStr = ""; try { //tempStr = MqlUtil.mqlCommand(context, "print connection " + physicalId +
                                 * " select attribute[PLMInstance.PLM_ExternalID].value dump |", false, false); DomainRelationship rel = DomainRelationship.newInstance(context, physicalId); StringList
                                 * relSel = new StringList(); relSel.add("attribute[PLMInstance.PLM_ExternalID].value"); Map mapRel = rel.getRelationshipData(context, relSel); productInstName =
                                 * (String) ((StringList) mapRel.get("attribute[PLMInstance.PLM_ExternalID].value")).get(0); } catch (Exception e) {}
                                 * 
                                 * //if (!"".equals(tempStr)) { //String[] tempStrA = tempStr.split("\\|"); // //productInstName = tempStrA[1]; //productInstName = tempStrA[0]; //} // Redmine Issue
                                 * #221 : HE5 : Changed the original code to fix the error on broken implement link to physical product for EBOM : 16/02/2016 : END } catch (RuntimeException ex) { }
                                 * catch (Exception ex) { // Broken link do nothing as it can happen in // normal process }
                                 */

                                if (j > 0) {
                                    cellDisplaySB.append(" => ");
                                }

                                cellDisplaySB.append(productInstName);

                                if (j > 0) {
                                    sbPathPID.append("/");
                                    sbPathLID.append("/");
                                }
                                sbPathPID.append(physicalId);
                                sbPathLID.append(logicalId);

                                lastPID = physicalId;
                            }

                            String strImplPathPIDS = sbPathPID.toString();
                            String strImplPathLIDS = sbPathLID.toString();
                            String displayStr = XSSUtil.encodeForHTML(context, cellDisplaySB.toString());

                            String manufItemPID = (String) mapObjectInfo.get("physicalid");
                            if (manufItemPID == null || manufItemPID.isEmpty()) {
                                String manuItemOID = (String) mapObjectInfo.get("id");
                                DomainObject domManuf = new DomainObject(manuItemOID);
                                manufItemPID = domManuf.getInfo(context, "physicalid");
                            }

                            /*
                             * Tiger 2.0
                             */
                            DomainRelationship domRel = DomainRelationship.newInstance(context, manuItemInstID);
                            String strAvailableUpdates = domRel.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_AVAILABLE_INSTANCEUPDATE);
                            String strUpdateFlag = domRel.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_AVAILABLE_INSTANCEUPDATEFLAG);

                            getImpelementlinkColorization(context, manufItemPID, level, cellContentSB, strUpdateFlag, strAvailableUpdates, productRefPID);

                            cellContentSB.append(manufItemPID);
                            cellContentSB.append("\" instPID=\"");
                            cellContentSB.append(manuItemInstID);
                            cellContentSB.append("\" pathPID=\"");
                            cellContentSB.append(strImplPathPIDS);
                            cellContentSB.append("\" pathLID=\"");
                            cellContentSB.append(strImplPathLIDS);
                            cellContentSB.append("\" ></div>");

                            cellContentSB.append("<span ");
                            // HE5 : Added the condition for null for getImplementedInstancesPathsFromList( ) R&D API
                            // if (!ilPathEffSynch) // If effectivity is not up to date, show the implement link text in red.

                            /*
                             * Tiger 2.0 if (ilPathEffSynch != null && !ilPathEffSynch) // If effectivity is not up to date, show the implement link text in red. cellContentSB.append(
                             * "style=\"color:red\" ");
                             */
                            // Changes added for MBO-184& MBO-183:H65:START
                            cellContentSB.append("title=\"" + manuItemInstID + "\"  id=\"completionMBOM_");
                            cellContentSB.append(manufItemPID);
                            cellContentSB.append("-span\">");
                            // cellContentSB.append(cellDisplaySB.toString());
                            // Changes added for MBO-184 &MBO-183:H65:END
                            cellContentSB.append("</span>");

                            cellContentSB.append("<div class='mBomImplementLink' style='display:none' manufItemInstID='");
                            cellContentSB.append(manuItemInstID);
                            cellContentSB.append("' implemPathPIDS='");
                            cellContentSB.append(strImplPathPIDS);
                            cellContentSB.append("' implemPathLIDS='");
                            cellContentSB.append(strImplPathLIDS);
                            cellContentSB.append("' productInstPID='");
                            cellContentSB.append(lastPID);
                            cellContentSB.append("'></div>");
                        }
                    }
                }
                // Add Scope infos
                String manufItemRefID = (String) mapObjectInfo.get("id");
                if (productRefPID != null && !"".equals(productRefPID)) {

                    boolean intermediateScope = false;
                    DomainObject domMBOM = new DomainObject(manufItemRefID);
                    for (String typeInList : baseTypesForMBOMAssemblyNodes) {
                        if (domMBOM.isKindOf(context, typeInList))
                            intermediateScope = true;
                    }
                    /*
                     * Tiger 2.0
                     */
                    String strUpdatesAvaiableReference = domMBOM.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_AVAILABLE_UPDATE);
                    String strUpdateFlagReference = domMBOM.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_AVAILABLE_UPDATEFLAG);

                    String strFromObjectList = domMBOM.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_DELFMIFUNCTIONIDENTIFIEDINSTANCE + "].from.id");

                    if (UIUtil.isNotNullAndNotEmpty(strFromObjectList)) {
                        cellContentSB.append("<div class='mBomScopeLink' style='display:none' manufItemRefID='");
                    } else {
                        getImpelementlinkColorization(context, manufItemRefID, null, cellContentSB, strUpdateFlagReference, strUpdatesAvaiableReference, productRefPID);
                    }

                    cellContentSB.append(manufItemRefID);
                    cellContentSB.append("' scopeId='");
                    cellContentSB.append(manufItemRefID);
                    cellContentSB.append("-");
                    cellContentSB.append(productRefPID);
                    cellContentSB.append("' productRefPID='");
                    cellContentSB.append(productRefPID);
                    cellContentSB.append("' intermediateScope='");
                    cellContentSB.append((intermediateScope ? "true" : "false"));
                    cellContentSB.append("'></div>");
                }

                // Matrial Status updated : 10/08/17
                // TIGTK-11550:Rutuja Ekatpure:23/11/2017:Start
                if (UIUtil.isNotNullAndNotEmpty(manuItemInstID)) {
                    DomainObject domMBOM = new DomainObject(manufItemRefID);
                    if (domMBOM.isKindOf(context, TigerConstants.TYPE_PROCESS_CONTINUOUS_CREATE_MATERIAL) || domMBOM.isKindOf(context, TigerConstants.TYPE_PROCESSCONTINUOUSPROVIDE)) {

                        String strFromId = MqlUtil.mqlCommand(context, "print connection " + manuItemInstID + " select from.physicalid dump |", false, false);
                        List<String> listMBOMRefIDs = new ArrayList<String>();
                        listMBOMRefIDs.add(strFromId);
                        List<String> psScope = FRCMBOMModelerUtility.getScopedPSReferencePIDFromList(context, plmSession, listMBOMRefIDs);
                        String strPSId = psScope.get(0);
                        if (UIUtil.isNotNullAndNotEmpty(strPSId)) {
                            DomainObject PSMBOM = new DomainObject(strPSId);
                            String strPSMaterial = PSMBOM.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PUBLISHEDPART_PSS_MATERIALLIST);
                            StringList slPublishedMaterialList = new StringList();
                            StringList slPublishedGenericMaterialList = new StringList();
                            if (UIUtil.isNotNullAndNotEmpty(strPSMaterial)) {
                                if (strPSMaterial.contains("|")) {
                                    String[] strMaterialArray = strPSMaterial.split("\\|");
                                    if (strMaterialArray.length > 1) {
                                        for (int i = 0; i < strMaterialArray.length; i++) {
                                            DomainObject domMaterial = DomainObject.newInstance(context, strMaterialArray[i]);
                                            String strCheckMaterialType = domMaterial.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PROCESSCONTINUOUSPROVIDE_PSS_MATERIALTYPE);
                                            if (strCheckMaterialType.equalsIgnoreCase("Specific"))
                                                slPublishedMaterialList.add(strMaterialArray[i]);
                                            else
                                                slPublishedGenericMaterialList.add(strMaterialArray[i]);
                                        }
                                    }
                                } else {
                                    DomainObject domMaterial = DomainObject.newInstance(context, strPSMaterial);
                                    String strCheckMaterialType = domMaterial.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PROCESSCONTINUOUSPROVIDE_PSS_MATERIALTYPE);
                                    if (strCheckMaterialType.equalsIgnoreCase("Specific"))
                                        slPublishedMaterialList.add(strPSMaterial);
                                    else
                                        slPublishedGenericMaterialList.add(strPSMaterial);
                                }
                            }

                            StringList slFromGenericSpecificMaterialList = getGenericMaterial(context, slPublishedGenericMaterialList);
                            String strMatTypeMBOM = domMBOM.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PROCESSCONTINUOUSPROVIDE_PSS_MATERIALTYPE);
                            if (strMatTypeMBOM.equalsIgnoreCase("Specific")) {
                                if (slPublishedMaterialList.contains(manufItemRefID)) {
                                    cellContentSB.append("<div onclick=\"FRCUpdateImplementLink('" + level
                                            + "')\" style=\"cursor:pointer;border:1px solid black;border-radius:8px;height:16px;width:16px;background-color:#8AFF4C;display:inline-block;\" class=\"completionMBOM\" title=\"\" manufItemRefID='");
                                } else {
                                    if (slFromGenericSpecificMaterialList.contains(manufItemRefID)) {
                                        cellContentSB.append("<div onclick=\"FRCUpdateImplementLink('" + level
                                                + "')\" style=\"cursor:pointer;border:1px solid black;border-radius:8px;height:16px;width:16px;background-color:#8AFF4C;display:inline-block;\" class=\"completionMBOM\" title=\"\" manufItemRefID='");
                                    } else {
                                        cellContentSB.append(
                                                "<div style=\"cursor:pointer;border:1px solid black;border-radius:8px;height:16px;width:16px;background-color:#EC3B1D;display:inline-block;\" title=\""
                                                        + strMaterialNotMatch + " | " + strMaterialNotLink + "\"  manufItemRefID='");
                                    }
                                }

                                cellContentSB.append(manufItemRefID);
                                cellContentSB.append("' scopeId='");
                                cellContentSB.append(manufItemRefID);
                                cellContentSB.append("-");
                                cellContentSB.append(strPSId);
                                cellContentSB.append("' productRefPID='");
                                cellContentSB.append(strPSId);
                                cellContentSB.append("'></div>");
                            }

                        }
                    }
                }
                // TIGTK-11550:Rutuja Ekatpure:23/11/2017:End
                // for update the PS color
                if (UIUtil.isNotNullAndNotEmpty(productRefPID) && UIUtil.isNotNullAndNotEmpty(manufItemRefID)) {
                    DomainObject dObjPhysicalStructure = DomainObject.newInstance(context, productRefPID);
                    DomainObject domMBOMObject = DomainObject.newInstance(context, manufItemRefID);
                    String strPSModifiedTimestamp = dObjPhysicalStructure.getInfo(context, DomainConstants.SELECT_MODIFIED);
                    List<String> mbomPIDList = PSS_FRCMBOMModelerUtility_mxJPO.getScopingReferencesFromList(context, plmSession, productRefPID);
                    if (!mbomPIDList.isEmpty()) {

                        StringList slObjSelectStmts = new StringList(1);
                        slObjSelectStmts.addElement("physicalid");
                        slObjSelectStmts.addElement(DomainConstants.SELECT_NAME);
                        StringList slRelSelectStmts = new StringList(1);
                        slRelSelectStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
                        MapList mlPublishControlObject = domMBOMObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_PUBLISHCONTROLOBJECT, // Relationship Pattern
                                TigerConstants.TYPE_PSS_PUBLISHCONTROLOBJECT, // Object Pattern
                                slObjSelectStmts, // Object Selects
                                slRelSelectStmts, // Relationship Selects
                                false, // to direction
                                true, // from direction
                                (short) 0, // recursion level
                                null, // object where clause
                                null, (short) 0, false, // checkHidden
                                true, // preventDuplicates
                                (short) 1000, // pageSize
                                null, // Post Type Pattern
                                null, null, null);

                        if (!mlPublishControlObject.isEmpty()) {
                            int slSize = mlPublishControlObject.size();
                            for (int j = 0; j < slSize; j++) {
                                Map mPCO = (Map) mlPublishControlObject.get(j);
                                DomainObject dPublishControlObj = DomainObject.newInstance(context, (String) mPCO.get("physicalid"));
                                String strPublishControlObjectTimeStamp = dPublishControlObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_UPDATETIMESTAMP);
                                Date dPSTimestamp = eMatrixDateFormat.getJavaDate(strPSModifiedTimestamp);
                                Date dPublishControlObjectTimeStamp = eMatrixDateFormat.getJavaDate(strPublishControlObjectTimeStamp);
                                if (dPSTimestamp.after(dPublishControlObjectTimeStamp)) {
                                    cellContentSB.append("<input type='hidden' class='ps-orange-color' value='" + productRefPID + "' />");
                                }

                            }
                        }
                    }
                }
                vecResult.add(cellContentSB.toString());
            }

            closeSession(plmSession);
            ContextUtil.commitTransaction(context);

            long endTime = System.currentTimeMillis();

            if (perfoTraces != null) {
                perfoTraces.write("Time spent in getImplementColumn() : " + (endTime - startTime) + " milliseconds");
                perfoTraces.newLine();
                perfoTraces.flush();
            }
            return vecResult;
        } catch (Exception exp) {
            logger.error("Error in getImplementColumn : ", exp);
            closeSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw exp;
        }
    }

    /**
     * This method return the generic material List connected to Material
     * @param context
     * @param slPublishedGenericMaterialList
     * @return
     * @throws FrameworkException
     */
    public static StringList getGenericMaterial(Context context, StringList slPublishedGenericMaterialList) throws FrameworkException {
        StringList slReturnList = new StringList();
        try {

            for (int i = 0; i < slPublishedGenericMaterialList.size(); i++) {
                String strGenericId = (String) slPublishedGenericMaterialList.get(i);
                DomainObject domMaterial = DomainObject.newInstance(context, strGenericId);

                Pattern typePattern = new Pattern(TigerConstants.TYPE_PROCESS_CONTINUOUS_CREATE_MATERIAL);
                typePattern.addPattern(TigerConstants.TYPE_PROCESSCONTINUOUSPROVIDE);

                StringList slObjSelectStmts = new StringList(1);
                slObjSelectStmts.addElement("physicalid");
                slObjSelectStmts.addElement(DomainConstants.SELECT_NAME);
                StringList slRelSelectStmts = new StringList(1);
                slRelSelectStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

                String objWhere = "attribute[PSS_ProcessContinuousProvide.PSS_MaterialType] == Specific";

                MapList mlMaterialList = domMaterial.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_RELATEDMATERIALS, // Relationship Pattern
                        typePattern.getPattern(), // Object Pattern
                        slObjSelectStmts, // Object Selects
                        slRelSelectStmts, // Relationship Selects
                        true, // to direction
                        true, // from direction
                        (short) 1, // recursion level
                        objWhere, // object where clause
                        null, (short) 0, false, // checkHidden
                        true, // preventDuplicates
                        (short) 1000, // pageSize
                        null, // Post Type Pattern
                        null, null, null);
                if (!mlMaterialList.isEmpty()) {
                    for (int j = 0; j < mlMaterialList.size(); j++) {
                        Map mMaterialMap = (Map) mlMaterialList.get(j);
                        String strMaterialObjectId = (String) mMaterialMap.get("physicalid");
                        slReturnList.add(strMaterialObjectId);
                    }
                }

            }
        } catch (Exception exp) {
            logger.error("Error in getGenericMaterial : ", exp);
            throw exp;
        }
        return slReturnList;
    }

    /**
     * This method Verified Update the MBOM structure on basis of EBOM structure
     * @param context
     * @param args
     * @throws Exception
     */
    public static void checkUpdatesAvailableOnPS(Context context, String[] args) throws Exception {
        PLMCoreModelerSession plmSession = null;
        boolean isTransactionActive = false;
        MapList mbomMapList = null;
        try {
            ContextUtil.startTransaction(context, true);
            isTransactionActive = true;

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            String psRootPID = args[0];
            String mbomRootPID = args[1];
            StringBuffer sbUpdatesAvailable = new StringBuffer();
            String strUpdatesAvailable = DomainConstants.EMPTY_STRING;
            String mbomInstancePhysicalId = DomainConstants.EMPTY_STRING;
            String psInstancePhysicalId = DomainConstants.EMPTY_STRING;

            if (UIUtil.isNotNullAndNotEmpty(psRootPID) && UIUtil.isNotNullAndNotEmpty(mbomRootPID)) {

                setPublishControlAttribute(context, mbomRootPID);
                String strCheckColorOptionsUpdate = PSS_MBOMUpdate_mxJPO.checkColorOptionsUpdate(context, psRootPID, mbomRootPID);
                String strCheckMaterialUpdate = PSS_MBOMUpdate_mxJPO.checkMaterialUpdate(context, psRootPID, mbomRootPID);
                String strCheckSpareUpdate = PSS_MBOMUpdate_mxJPO.checkSpareUpdate(context, psRootPID, mbomRootPID);
                String strCheckToolingUpdate = PSS_MBOMUpdate_mxJPO.checkToolingUpdate(context, psRootPID, mbomRootPID);
                String strCheckAttributeUpdate = PSS_MBOMUpdate_mxJPO.checkAttributeUpdate(context, psRootPID, mbomRootPID, psInstancePhysicalId, mbomInstancePhysicalId);

                if (UIUtil.isNotNullAndNotEmpty(strCheckColorOptionsUpdate) || UIUtil.isNotNullAndNotEmpty(strCheckMaterialUpdate) || UIUtil.isNotNullAndNotEmpty(strCheckSpareUpdate)
                        || UIUtil.isNotNullAndNotEmpty(strCheckToolingUpdate) || UIUtil.isNotNullAndNotEmpty(strCheckAttributeUpdate)) {
                    sbUpdatesAvailable.append(strCheckColorOptionsUpdate);
                    sbUpdatesAvailable.append("\n");
                    sbUpdatesAvailable.append(strCheckMaterialUpdate);
                    sbUpdatesAvailable.append("\n");
                    sbUpdatesAvailable.append(strCheckSpareUpdate);
                    sbUpdatesAvailable.append("\n");
                    sbUpdatesAvailable.append(strCheckToolingUpdate);
                    sbUpdatesAvailable.append("\n");
                    sbUpdatesAvailable.append(strCheckAttributeUpdate);
                    strUpdatesAvailable = sbUpdatesAvailable.toString();
                    checkValidString(strUpdatesAvailable);
                    setAttributeonUpdate(context, mbomRootPID, strUpdatesAvailable);
                }
            }
            StringList busSelect = new StringList();
            busSelect.add("physicalid");
            StringList relSelect = new StringList();
            relSelect.add("physicalid[connection]");
            // TIGTK-12366 : VB : Start :Do not expand child if top selection
            String isExpand = PropertyUtil.getRPEValue(context, DONOTEXPAND, false);
            if (UIUtil.isNotNullAndNotEmpty(isExpand) && isExpand.equalsIgnoreCase("true")) {
                mbomMapList = new MapList();
            } else {
                // mbomMapList = PSS_FRCMBOMModelerUtility_mxJPO.getVPMStructure(context, plmSession, mbomRootPID, busSelect, relSelect, (short) 0, null, null);
                mbomMapList = PSS_FRCMBOMModelerUtility_mxJPO.getVPMStructureMQL(context, mbomRootPID, busSelect, relSelect, (short) 1, null, null);
            }
            // TIGTK-12366 : VB : END

            for (int i = 0; i < mbomMapList.size(); i++) {
                Map mapObj = (Map) mbomMapList.get(i);
                String objPID = (String) mapObj.get("physicalid");
                String objPIDConnection = (String) mapObj.get("physicalid[connection]");
                StringBuffer sbChildUpdatesAvailable = new StringBuffer();

                List<String> newImplementLinkPIDList = PSS_FRCMBOMModelerUtility_mxJPO.getImplementLinkInfoSimple(context, plmSession, objPIDConnection);
                if (newImplementLinkPIDList != null && newImplementLinkPIDList.size() > 0) {
                    String psLeafInstPID = newImplementLinkPIDList.get(newImplementLinkPIDList.size() - 1);
                    String psLeafRefPID = MqlUtil.mqlCommand(context, "print connection " + psLeafInstPID + " select to.physicalid dump |", false, false);
                    if (UIUtil.isNotNullAndNotEmpty(objPID) && UIUtil.isNotNullAndNotEmpty(psLeafRefPID)) {
                        setPublishControlAttribute(context, objPID);
                        String strCheckChildColorOptionsUpdate = PSS_MBOMUpdate_mxJPO.checkColorOptionsUpdate(context, psLeafRefPID, objPID);
                        String strCheckAlternateUpdate = PSS_MBOMUpdate_mxJPO.checkAlternateUpdate(context, psLeafRefPID, objPID);
                        String strCheckChildMaterialUpdate = PSS_MBOMUpdate_mxJPO.checkMaterialUpdate(context, psLeafRefPID, objPID);
                        String strCheckChildSpareUpdate = PSS_MBOMUpdate_mxJPO.checkSpareUpdate(context, psLeafRefPID, objPID);
                        String strCheckEffectivitiesUpdate = PSS_MBOMUpdate_mxJPO.checkEffectivitiesUpdate(context, psLeafRefPID, objPID, psLeafInstPID, objPIDConnection);
                        String strCheckChildToolingUpdate = PSS_MBOMUpdate_mxJPO.checkToolingUpdate(context, psLeafRefPID, objPID);
                        String strCheckChildAttributeUpdate = PSS_MBOMUpdate_mxJPO.checkAttributeUpdate(context, psLeafRefPID, objPID, psLeafInstPID, objPIDConnection);
                        if (UIUtil.isNotNullAndNotEmpty(strCheckChildColorOptionsUpdate) || UIUtil.isNotNullAndNotEmpty(strCheckAlternateUpdate)
                                || UIUtil.isNotNullAndNotEmpty(strCheckEffectivitiesUpdate) || UIUtil.isNotNullAndNotEmpty(strCheckChildMaterialUpdate)
                                || UIUtil.isNotNullAndNotEmpty(strCheckChildSpareUpdate) || UIUtil.isNotNullAndNotEmpty(strCheckChildToolingUpdate)
                                || UIUtil.isNotNullAndNotEmpty(strCheckChildAttributeUpdate)) {
                            sbChildUpdatesAvailable.append(strCheckChildColorOptionsUpdate);
                            sbChildUpdatesAvailable.append("\n");
                            sbChildUpdatesAvailable.append(strCheckAlternateUpdate);
                            sbChildUpdatesAvailable.append("\n");
                            sbChildUpdatesAvailable.append(strCheckEffectivitiesUpdate);
                            sbChildUpdatesAvailable.append("\n");
                            sbChildUpdatesAvailable.append(strCheckChildMaterialUpdate);
                            sbChildUpdatesAvailable.append("\n");
                            sbChildUpdatesAvailable.append(strCheckChildSpareUpdate);
                            sbChildUpdatesAvailable.append("\n");
                            sbChildUpdatesAvailable.append(strCheckChildToolingUpdate);
                            sbChildUpdatesAvailable.append("\n");
                            sbChildUpdatesAvailable.append(strCheckChildAttributeUpdate);
                            sbChildUpdatesAvailable.append("\n");
                            strUpdatesAvailable = sbChildUpdatesAvailable.toString();
                            checkValidString(strUpdatesAvailable);
                            setAttributeonUpdate(context, objPID, strUpdatesAvailable);
                        }
                    }
                }
            }
            flushAndCloseSession(plmSession);
            if (isTransactionActive) {
                ContextUtil.commitTransaction(context);
            }

        } catch (Exception ex) {
            logger.error("Error in checkUpdatesAvailableOnPS : ", ex);
            flushAndCloseSession(plmSession);
            if (isTransactionActive) {
                ContextUtil.abortTransaction(context);
            }
            throw ex;
        }

    }

    public static void setAttributeonUpdate(Context context, String mbomPhysicalId, String strUpdatesAvailable) throws Exception {

        try {
            DomainObject dMBOMObj = DomainObject.newInstance(context, mbomPhysicalId);
            String strFromRel = (String) dMBOMObj.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_DELFMIFUNCTIONIDENTIFIEDINSTANCE + "].physicalid");
            if (UIUtil.isNotNullAndNotEmpty(strFromRel)) {
                DomainRelationship domFromRel = DomainRelationship.newInstance(context, strFromRel);
                domFromRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_AVAILABLE_INSTANCEUPDATE, strUpdatesAvailable);
                domFromRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_AVAILABLE_INSTANCEUPDATEFLAG, "Yes");
            } else {
                dMBOMObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_AVAILABLE_UPDATE, strUpdatesAvailable);
                dMBOMObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_AVAILABLE_UPDATEFLAG, "Yes");
            }

        } catch (Exception exp) {
            logger.error("Error in setAttributeonUpdate : ", exp);
            throw exp;
        }
    }

    // Added TIGTK-7113:PKH:Phase-2.0:Start
    /**
     * This method canEditEffectiveRatio
     * @param context
     * @param args
     * @return --
     * @throws Exception
     */
    public StringList canEditEffectiveRatio(Context context, String[] args) throws Exception {
        StringList slResultList = new StringList();

        try {

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList relBusObjPageList = (MapList) programMap.get("objectList");

            for (int i = 0; i < relBusObjPageList.size(); i++) {
                Map mapObjectInfo = (Map) relBusObjPageList.get(i);
                String strRelId = (String) mapObjectInfo.get("id[connection]");

                if (UIUtil.isNotNullAndNotEmpty(strRelId)) {
                    DomainRelationship domRelObj = new DomainRelationship(strRelId);
                    String strFromRelSelects = "fromrel[" + TigerConstants.RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS + "].attribute["
                            + TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGINSTANCEEXTPSS_ALLOWTOLERANCE + "]";
                    Hashtable relData = (Hashtable) domRelObj.getRelationshipData(context, new StringList(strFromRelSelects));
                    StringList slAttrAllowTolerance = (StringList) relData.get(strFromRelSelects);

                    if (slAttrAllowTolerance != null && slAttrAllowTolerance.contains("Yes")) {

                        slResultList.addElement("true");
                    } else {

                        slResultList.addElement("false");

                    }
                } else {

                    slResultList.addElement("false");
                }
            }

        } catch (Exception ex) {
            logger.error("Error in canEditEffectiveRatio: ", ex);
        }
        return slResultList;
    }

    // Added TIGTK-7113:PKH:Phase-2.0:End

    /**
     * format the stringbuffer and remove unwanted spaces
     */
    public static String checkValidString(String strCheck) {
        try {
            String splitted[] = strCheck.split("\n");
            StringBuffer sb = new StringBuffer();
            String retrieveData = DomainConstants.EMPTY_STRING;
            for (int i = 0; i < splitted.length; i++) {
                retrieveData = splitted[i];
                if ((retrieveData.trim()).length() > 0) {

                    if (i != 0) {
                        sb.append("\n");
                    }
                    sb.append(retrieveData);

                }
            }
            return (sb.toString());
        } catch (Exception ex) {
            logger.error("Error in checkValidString : ", ex);
            throw ex;
        }

    }

    /**
     * This method set the TimeStamp on PCO objects
     * @param context
     * @param mbomPhysicalId
     * @throws Exception
     */
    public static void setPublishControlAttribute(Context context, String mbomPhysicalId) throws Exception {
        try {
            DomainObject dMBOMObj = DomainObject.newInstance(context, mbomPhysicalId);
            StringList slObjSelectStmts = new StringList(1);
            slObjSelectStmts.addElement("physicalid");
            slObjSelectStmts.addElement(DomainConstants.SELECT_NAME);
            StringList slRelSelectStmts = new StringList(1);
            slRelSelectStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            MapList mlPublishControlObject = dMBOMObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_PUBLISHCONTROLOBJECT, TigerConstants.TYPE_PSS_PUBLISHCONTROLOBJECT, slObjSelectStmts,
                    slRelSelectStmts, false, true, (short) 1, null, null, (short) 0, false, true, (short) 1000, null, null, null, null);
            if (!mlPublishControlObject.isEmpty()) {

                int slSize = mlPublishControlObject.size();
                for (int i = 0; i < slSize; i++) {
                    Map mPCO = (Map) mlPublishControlObject.get(i);
                    DomainObject dPublishControlObj = DomainObject.newInstance(context, (String) mPCO.get("physicalid"));
                    SimpleDateFormat MATRIX_DATE_FORMAT = new SimpleDateFormat(eMatrixDateFormat.getEMatrixDateFormat(), Locale.US);
                    String strTimeStamp = MATRIX_DATE_FORMAT.format(new Date());
                    dPublishControlObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_UPDATETIMESTAMP, strTimeStamp);
                }
            }
        } catch (Exception exp) {
            logger.error("Error in setPublishControlAttribute : ", exp);
            throw exp;
        }

    }

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author agholve
     */
    public String replaceNewRevisionMaterial(Context context, String args[]) throws Exception {

        String strOriginalMaterialId = args[1];

        String strRevisedObjectId = "";
        PLMCoreModelerSession plmSession = null;
        // Modified for TIGTK-9399 :Start by SIE on 10/8/2017

        boolean bFlag = false;
        try {
            String strUserName = context.getUser();
            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            bFlag = true;
            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();
            ContextUtil.startTransaction(context, true);
            pss.cadbom.Material_mxJPO objMaterial = new pss.cadbom.Material_mxJPO();
            strRevisedObjectId = objMaterial.newRevisionMaterial(context, plmSession, strOriginalMaterialId);
            DomainObject domRevisedObj = DomainObject.newInstance(context, strRevisedObjectId);
            domRevisedObj.setOwner(context, strUserName);
            String instPID = args[0];
            if (instPID != null && !"".equals(instPID) && !instPID.equals(strOriginalMaterialId))
                FRCMBOMModelerUtility.replaceMBOMInstance(context, plmSession, instPID, strRevisedObjectId);

            flushAndCloseSession(plmSession);
            if (context.isTransactionActive()) {
                ContextUtil.commitTransaction(context);
            }

        } catch (Exception ex) {
            logger.error("Error in replaceNewRevisionMaterial", ex);
            flushAndCloseSession(plmSession);
            if (context.isTransactionActive()) {
                ContextUtil.abortTransaction(context);
            }

        } finally {
            if (bFlag)
                ContextUtil.popContext(context);
        }
        // Modified for TIGTK-9399 :End by SIE on 10/8/2017

        return strRevisedObjectId;

    }

    /**
     * Intermediate method return the Implement link column status
     * @param context
     * @param manufItemPID
     * @param level
     * @param cellContentSB
     * @param strUpdateFlag
     * @param strAvailableUpdates
     * @param productRefPID
     * @return
     */
    public static String getImpelementlinkColorization(Context context, String manufItemPID, String level, StringBuffer cellContentSB, String strUpdateFlag, String strAvailableUpdates,
            String productRefPID) {

        try {
            String strColorMessage = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.Message.ColorOptionNotImplemented");
            DomainObject domObject = DomainObject.newInstance(context, manufItemPID);
            StringList slMaterialColorList = new StringList();
            StringList slMaterialList = domObject.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS + "].to.physicalid");

            if (!slMaterialList.isEmpty()) {
                StringList slMBOMColorList = domObject.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_COLORLIST + "].to.physicalid");

                for (int i = 0; i < slMaterialList.size(); i++) {
                    String strMaterialId = (String) slMaterialList.get(i);
                    DomainObject domMaterialObject = DomainObject.newInstance(context, strMaterialId);
                    StringList slPColorList = domMaterialObject.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_COLORLIST + "].to.physicalid");
                    slMaterialColorList.addAll(slPColorList);
                }
                boolean checkFlag = false;

                if (slMBOMColorList.size() > slMaterialColorList.size()) {

                    checkFlag = true;

                } else {

                    for (int i = 0; i < slMBOMColorList.size(); i++) {
                        String strMaterialColorId = (String) slMBOMColorList.get(i);
                        if (!slMaterialColorList.contains(strMaterialColorId)) {
                            checkFlag = true;
                            break;
                        }
                    }
                }
                if (UIUtil.isNotNullAndNotEmpty(level)) {
                    if (checkFlag == false) {
                        if (UIUtil.isNotNullAndNotEmpty(strUpdateFlag) && strUpdateFlag.equalsIgnoreCase("Yes")) {
                            cellContentSB.append(
                                    "<div style=\"cursor:pointer;border:1px solid black;border-radius:8px;height:16px;width:16px;background-color:#e5de1d;display:inline-block;\" class=\"completionMBOM completionMBOMUpdateAvailable\" title=\""
                                            + strAvailableUpdates + "\" id=\"completionMBOM_");
                        } else if (UIUtil.isNotNullAndNotEmpty(strAvailableUpdates) && strAvailableUpdates.equalsIgnoreCase("Material Update to be performed manually")) {
                            cellContentSB.append("<div onclick=\"FRCUpdateImplementLink('" + level
                                    + "')\" style=\"cursor:pointer;border:1px solid black;border-radius:8px;height:16px;width:16px;background-color:#EC3B1D;display:inline-block;\" class=\"completionMBOM\" title=\""
                                    + strAvailableUpdates + "\" id=\"completionMBOM_");
                        } else
                            cellContentSB.append("<div onclick=\"FRCUpdateImplementLink('" + level
                                    + "')\" style=\"cursor:pointer;border:1px solid black;border-radius:8px;height:16px;width:16px;background-color:#EC3B1D;display:inline-block;\" class=\"completionMBOM\" title=\"\" id=\"completionMBOM_");
                    } else {

                        if (UIUtil.isNotNullAndNotEmpty(strUpdateFlag) && strUpdateFlag.equalsIgnoreCase("Yes")) {
                            cellContentSB.append(
                                    "<div style=\"cursor:pointer;border:1px solid black;border-radius:8px;height:16px;width:16px;background-color:#EC3B1D;display:inline-block;\" class=\"completionMBOM completionMBOMUpdateAvailable\" title=\""
                                            + strAvailableUpdates + " | " + strColorMessage + "\" id=\"completionMBOM_");
                        } else if (UIUtil.isNotNullAndNotEmpty(strAvailableUpdates) && strAvailableUpdates.equalsIgnoreCase("Material Update to be performed manually")) {
                            cellContentSB.append("<div onclick=\"FRCUpdateImplementLink('" + level
                                    + "')\" style=\"cursor:pointer;border:1px solid black;border-radius:8px;height:16px;width:16px;background-color:#EC3B1D;display:inline-block;\" class=\"completionMBOM Color\" title=\""
                                    + strAvailableUpdates + " | " + strColorMessage + "\" id=\"completionMBOM_");
                        } else {
                            cellContentSB.append("<div onclick=\"FRCUpdateImplementLink('" + level
                                    + "')\" style=\"cursor:pointer;border:1px solid black;border-radius:8px;height:16px;width:16px;background-color:#EC3B1D;display:inline-block;\" class=\"completionMBOM Color\" title=\""
                                    + strColorMessage + "\" id=\"completionMBOM_");
                        }
                    }
                } else {

                    if (checkFlag == false) {

                        if (UIUtil.isNotNullAndNotEmpty(strUpdateFlag) && strUpdateFlag.equalsIgnoreCase("Yes")) {
                            cellContentSB.append("<div class='mBomScopeLink' "
                                    + "style=\"cursor:pointer;border:1px solid black;border-radius:8px;height:16px;width:16px;background-color:#e5de1d;display:inline-block;\" title=\""
                                    + strAvailableUpdates + "\"  manufItemRefID='");
                        } else
                            cellContentSB.append("<div class='mBomScopeLink' style='display:none' manufItemRefID='");
                    } else {
                        if (UIUtil.isNotNullAndNotEmpty(strUpdateFlag) && strUpdateFlag.equalsIgnoreCase("Yes")) {
                            cellContentSB.append("<div class='mBomScopeLink' "
                                    + "style=\"cursor:pointer;border:1px solid black;border-radius:8px;height:16px;width:16px;background-color:#EC3B1D;display:inline-block;\" title=\""
                                    + strAvailableUpdates + " | " + strColorMessage + "\"  manufItemRefID='");
                        } else {
                            cellContentSB.append("<div class='mBomScopeLink' "
                                    + "style=\"cursor:pointer;border:1px solid black;border-radius:8px;height:16px;width:16px;background-color:#EC3B1D;display:inline-block;\" title=\""
                                    + strColorMessage + "\"  manufItemRefID='");
                        }
                    }
                }
            } else {

                if (UIUtil.isNotNullAndNotEmpty(level)) {
                    if (UIUtil.isNotNullAndNotEmpty(strUpdateFlag) && strUpdateFlag.equalsIgnoreCase("Yes")) {
                        cellContentSB.append("<div onclick=\"FRCUpdateImplementLink('" + level
                                + "')\" style=\"cursor:pointer;border:1px solid black;border-radius:8px;height:16px;width:16px;background-color:#e5de1d;display:inline-block;\" class=\"completionMBOM completionMBOMUpdateAvailable\" title=\""
                                + strAvailableUpdates + "\" id=\"completionMBOM_");
                    } else if (UIUtil.isNotNullAndNotEmpty(strAvailableUpdates) && strAvailableUpdates.equalsIgnoreCase("Material Update to be performed manually")) {
                        cellContentSB.append("<div onclick=\"FRCUpdateImplementLink('" + level
                                + "')\" style=\"cursor:pointer;border:1px solid black;border-radius:8px;height:16px;width:16px;background-color:#EC3B1D;display:inline-block;\" class=\"completionMBOM\" title=\""
                                + strAvailableUpdates + "\" id=\"completionMBOM_");
                    } else {
                        cellContentSB.append("<div onclick=\"FRCUpdateImplementLink('" + level
                                + "')\" style=\"cursor:pointer;border:1px solid black;border-radius:8px;height:16px;width:16px;background-color:#EC3B1D;display:inline-block;\" class=\"completionMBOM\" title=\"\" id=\"completionMBOM_");
                    }
                } else {
                    if (UIUtil.isNotNullAndNotEmpty(strUpdateFlag) && strUpdateFlag.equalsIgnoreCase("Yes")) {
                        cellContentSB.append("<div class='mBomScopeLink' "
                                + "style=\"cursor:pointer;border:1px solid black;border-radius:8px;height:16px;width:16px;background-color:#e5de1d;display:inline-block;\" title=\""
                                + strAvailableUpdates + "\"  manufItemRefID='");
                    } else
                        cellContentSB.append("<div class='mBomScopeLink' style='display:none' manufItemRefID='");
                }
            }

        } catch (RuntimeException ex) {
            logger.error("Error in getImpelementlinkColorization", ex);
        } catch (Exception ex) {
            logger.error("Error in getImpelementlinkColorization", ex);
        }
        return cellContentSB.toString();
    }

    /**
     * TIGTK-7250
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public StringList includeGenericMaterialWithLink(Context context, String[] args) throws Exception {
        StringList slMaterialList = new StringList();
        HashMap programMap = (HashMap) JPO.unpackArgs(args);

        try {
            String strObjectIds = (String) programMap.get("genericMaterialdetails");
            if (UIUtil.isNotNullAndNotEmpty(strObjectIds)) {
                String[] strObjectArray = strObjectIds.split("\\|");

                for (int i = 0; i < strObjectArray.length; i++) {
                    String strObjectId = strObjectArray[i];
                    DomainObject domId = DomainObject.newInstance(context, strObjectId);
                    StringList slObjSelectStmts = new StringList();
                    slObjSelectStmts.addElement(DomainConstants.SELECT_ID);
                    slObjSelectStmts.addElement(DomainConstants.SELECT_TYPE);

                    Pattern typePattern = new Pattern(TigerConstants.TYPE_PROCESS_CONTINUOUS_CREATE_MATERIAL);
                    typePattern.addPattern(TigerConstants.TYPE_PROCESSCONTINUOUSPROVIDE);

                    StringList slRelSelectStmts = new StringList();
                    slRelSelectStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

                    String ObjectWhere = " attribute[PSS_ProcessContinuousProvide.PSS_MaterialType]==" + "Specific";

                    MapList mlObjectList = domId.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_RELATEDMATERIALS, // Relationship
                            // Pattern
                            typePattern.getPattern(), // Object Pattern
                            slObjSelectStmts, // Object Selects
                            slRelSelectStmts, // Relationship Selects
                            true, // to direction
                            false, // from direction
                            (short) 0, // recursion level
                            ObjectWhere, // object where clause
                            null, (short) 0, false, // checkHidden
                            true, // preventDuplicates
                            (short) 1000, // pageSize
                            null, // Post Type Pattern
                            null, null, null);
                    for (int j = 0; j < mlObjectList.size(); j++) {
                        Map mMaterialMap = (Map) mlObjectList.get(j);
                        String strMaterialObjectId = (String) mMaterialMap.get("id");
                        slMaterialList.add(strMaterialObjectId);
                    }
                }
            }
            // Rutuja Ekatpure:int can not be casr to Map Error:22/8/2017:start
            if (slMaterialList.size() == 0) {
                slMaterialList.add(" ");
            }
            // Rutuja Ekatpure:int can not be casr to Map Error:22/8/2017:End
        } catch (Exception ex) {
            logger.error("Error in includeGenericMaterialWithLink : ", ex);
            throw ex;
        }
        return slMaterialList;
    }

    /**
     * TIGTK-7250
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public void postProcessForReplaceMaterial(Context context, String[] args) throws Exception {
        PLMCoreModelerSession plmSession = null;
        boolean isTransactionActive = false;
        try {
            ContextUtil.startTransaction(context, true);
            isTransactionActive = true;
            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            Map programMap = (Map) JPO.unpackArgs(args);
            String strRelIds = (String) programMap.get("strRelIds");
            if (UIUtil.isNotNullAndNotEmpty(strRelIds)) {
                String[] relIds = strRelIds.split("\\|");
                String[] emxTableRowId = (String[]) programMap.get("emxTableRowId");
                if (emxTableRowId != null && emxTableRowId.length > 0) {
                    String selectedMatrialId = emxTableRowId[0];
                    String[] strSpiltArray = selectedMatrialId.split("\\|");
                    String strObjectId = strSpiltArray[1];
                    strObjectId = DomainObject.newInstance(context, strObjectId).getInfo(context, "physicalid");

                    for (int i = 0; i < relIds.length; i++) {
                        String strInstanceId = relIds[i];
                        PSS_FRCMBOMModelerUtility_mxJPO.replaceMBOMInstance(context, plmSession, strInstanceId, strObjectId);
                    }
                }
            }
            flushAndCloseSession(plmSession);
            if (isTransactionActive) {
                ContextUtil.commitTransaction(context);
            }

        } catch (Exception ex) {
            logger.error("Error in postProcessForReplaceMaterial : ", ex);
            flushAndCloseSession(plmSession);
            if (isTransactionActive) {
                ContextUtil.abortTransaction(context);
            }
            throw ex;
        }
    }

    /**
     * This method add the Plant name on MBOM objects after creation. RFC-139
     * @param context
     * @param args
     * @throws Exception
     */
    public void insertNewManufItemAndUpdatePlantName(Context context, String[] args) throws Exception {
        PLMCoreModelerSession plmSession = null;
        boolean isTransactionActive = false;
        try {
            ContextUtil.startTransaction(context, true);
            isTransactionActive = true;
            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();
            Map programMap = (Map) JPO.unpackArgs(args);
            Map paramMap = (Map) programMap.get("paramMap");
            Map requestMap = (Map) programMap.get("requestMap");
            String strObjectId = (String) paramMap.get("objectId");
            String strMode = (String) requestMap.get("PSS_Mode");
            String strMasterMfgProductionPlanning = pss.mbom.MBOMUtil_mxJPO.getMasterMfgProductionPlanning(context, strObjectId);
            // TIGTK-12976 : START
            DomainObject dMBOMObj = DomainObject.newInstance(context, strObjectId);
            String strPolicy = dMBOMObj.getInfo(context, DomainConstants.SELECT_POLICY);
            String strType = dMBOMObj.getInfo(context, DomainConstants.SELECT_POLICY);

            // OOTB attribute Modification : Start : TIGTK-13669
            if (strType.equalsIgnoreCase(TigerConstants.TYPE_CREATEMATERIAL))
                dMBOMObj.setAttributeValue(context, "CreateMaterial.V_NeedDedicatedSystem", "2");
            // OOTB attribute Modification : END

            if (TigerConstants.POLICY_PSS_STANDARDMBOM.equals(strPolicy)) {
                String PSS_UNITOFMEASURE = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale("en"), "emxFramework.Range.PSS_ManufacturingUoMExt.PSS_UnitOfMeasure.PC");
                dMBOMObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_UNIT_OF_MEASURE, PSS_UNITOFMEASURE);
            }
            // TIGTK-12976 : END

            if (UIUtil.isNotNullAndNotEmpty(strMasterMfgProductionPlanning)) {
                DomainObject dMfgProductionPlanningObj = DomainObject.newInstance(context, strMasterMfgProductionPlanning);
                String strPlantName = dMfgProductionPlanningObj.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_VOWNER + "].from.name");
                dMBOMObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXTPSS_MATERPLANTNAME, strPlantName);

                if (UIUtil.isNotNullAndNotEmpty(strMode) && strMode.equals("standardMBOM")) {
                    String strquery = "query path type SemanticRelation containing " + strObjectId + " where owner.type=='MfgProductionPlanning' select owner.physicalid dump |";
                    String strlistPathIds = MqlUtil.mqlCommand(context, strquery, false, false);
                    if (UIUtil.isNotNullAndNotEmpty(strlistPathIds)) {
                        String[] strOwnerArray = strlistPathIds.split("\n");
                        for (int i = 0; i < strOwnerArray.length; i++) {
                            String strPhysicalId = strOwnerArray[i];
                            String strMfgPlanningId = strPhysicalId.split("\\|")[1];
                            DomainObject dMfgProductionPlanning = DomainObject.newInstance(context, strMfgPlanningId);
                            dMfgProductionPlanning.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGPLANTEXTPSS_OWNERSHIP, "Consumer");
                        }
                    }
                }
            }
            flushAndCloseSession(plmSession);
            if (isTransactionActive) {
                ContextUtil.commitTransaction(context);
            }

        } catch (Exception ex) {
            logger.error("Error in insertNewManufItemAndUpdatePlantName : ", ex);
            flushAndCloseSession(plmSession);
            if (isTransactionActive) {
                ContextUtil.abortTransaction(context);
            }
            throw ex;
        }

    }

    /**
     * This Method done Functionality of Replace By New MBOM Object.
     * @param context
     * @param args
     * @throws Exception
     */
    public void replaceNewExisting(Context context, String[] args) throws Exception {
        PLMCoreModelerSession plmSession = null;
        boolean isTransactionActive = false;
        try {
            ContextUtil.startTransaction(context, true);
            isTransactionActive = true;
            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();
            Map programMap = (Map) JPO.unpackArgs(args);
            Map paramMap = (Map) programMap.get("paramMap");
            Map requestMap = (Map) programMap.get("requestMap");
            String strObjectId = (String) paramMap.get("newObjectId");
            String strInstanceID = (String) requestMap.get("instanceID");
            if (UIUtil.isNotNullAndNotEmpty(strInstanceID) && UIUtil.isNotNullAndNotEmpty(strObjectId)) {
                DomainRelationship domRel = DomainRelationship.newInstance(context, strInstanceID);
                domRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_HARMONIES_INSTANCE, DomainConstants.EMPTY_STRING);
                // MakeBye if Plant is Different
                String strConnectionCommand = "print connection " + strInstanceID + " select from.id dump |";
                String strFromConnectionId = MqlUtil.mqlCommand(context, strConnectionCommand, false, false);

                DomainRelationship.disconnect(context, strInstanceID);
                String newInstPID = createInstance(context, plmSession, strFromConnectionId, strObjectId);
                DomainRelationship domNewRel = DomainRelationship.newInstance(context, newInstPID);
                String strFromMBOMMasterPlant = pss.mbom.MBOMUtil_mxJPO.getMasterPlant(context, strFromConnectionId);
                String strToMBOMMasterPlant = pss.mbom.MBOMUtil_mxJPO.getMasterPlant(context, strObjectId);
                if (!strFromMBOMMasterPlant.equalsIgnoreCase(strToMBOMMasterPlant))
                    domNewRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_TYPE_OF_PART, "MakeBuy");
                else
                    domNewRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_TYPE_OF_PART, "Make");
            }
            flushAndCloseSession(plmSession);
            if (isTransactionActive) {
                ContextUtil.commitTransaction(context);
            }

        } catch (Exception ex) {
            logger.error("Error in replaceNewExisting : ", ex);
            flushAndCloseSession(plmSession);
            if (isTransactionActive) {
                ContextUtil.abortTransaction(context);
            }
            throw ex;
        }
    }

    // Rutuja Ekatpure:TIGTK-10100:Start
    /**
     * this method used to create standard MBOM
     * @PolicyChange
     * @param context
     * @param plmSession
     * @param type
     * @param magnitudeType
     * @param attributes
     * @return
     * @throws Exception
     */
    public static String createStandardMBOMReference(Context context, PLMCoreModelerSession plmSession, String type, String magnitudeType, Map<String, String> attributes) throws Exception {
        logger.debug("createStandardMBOMReference:::Start ");
        String newObjPID = null;
        try {
            PropertyUtil.setRPEValue(context, "PSS_IS_CALLING_FROM_ENOVIA", "true", false);
            newObjPID = PSS_FRCMBOMModelerUtility_mxJPO.createMBOMDiscreteReference(context, plmSession, type, TigerConstants.POLICY_PSS_STANDARDMBOM, attributes);

            flushSession(plmSession);
            DomainObject domObj = new DomainObject(newObjPID);
            String strType = domObj.getInfo(context, DomainConstants.SELECT_TYPE);
            domObj.setPolicy(context, TigerConstants.POLICY_PSS_STANDARDMBOM);
            String policy = domObj.getInfo(context, DomainConstants.SELECT_POLICY);

            if (UIUtil.isNotNullAndNotEmpty(policy) && !policy.equalsIgnoreCase(TigerConstants.POLICY_PSS_MBOM)) {
                String strName = domObj.getInfo(context, DomainConstants.SELECT_NAME);
                String strRevision = "01.1";
                String strChangeString = "modify bus $1 revision $2 name $3;";
                MqlUtil.mqlCommand(context, strChangeString, newObjPID, strRevision, strName);
            }

            String symbolicTypeName = FrameworkUtil.getAliasForAdmin(context, DomainConstants.SELECT_TYPE, strType, true);
            if (!strType.equalsIgnoreCase(TigerConstants.TYPE_PROCESSCONTINUOUSPROVIDE) && !strType.equalsIgnoreCase(TigerConstants.TYPE_PROCESS_CONTINUOUS_CREATE_MATERIAL)) {
                String strAutoName = DomainObject.getAutoGeneratedName(context, symbolicTypeName, "-");
                domObj.setName(context, strAutoName);
                domObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PLM_EXTERNALID, strAutoName);
            }
        } catch (Exception e) {
            logger.error("Error in createStandardMBOMReference::: " + e);
        }
        logger.debug("createStandardMBOMReference:::newMBOMID:: " + newObjPID);
        logger.debug("createStandardMBOMReference:::End ");
        return newObjPID;
    }

    /**
     * This method is for setting the consumer Plant name on MBOM objects
     * @param context
     * @param args
     * @throws Exception
     */
    public static void updateConsumerPlantOnMBOM(Context context, String strMfgProductionPlanningObjId) throws Exception {
        logger.debug("updateConsumerPlantOnMBOM:::Start ");
        try {
            String strQuery = "print bus " + strMfgProductionPlanningObjId + " select paths.path.element[0].physicalid dump |;";
            String strMqlResult = MqlUtil.mqlCommand(context, strQuery, false, false);
            String strSplitArray[] = strMqlResult.split("\\|");
            if (strSplitArray.length > 0) {
                // String strPlantId = strSplitArray[0];
                String strMBOMObjectId = strSplitArray[1];
                if (UIUtil.isNotNullAndNotEmpty(strMBOMObjectId)) {
                    DomainObject dMBOMObj = DomainObject.newInstance(context, strMBOMObjectId);
                    DomainObject dMfgProductionPlanningObj = DomainObject.newInstance(context, strMfgProductionPlanningObjId);
                    if (dMBOMObj.isKindOf(context, TigerConstants.TYPE_CREATEASSEMBLY) || dMBOMObj.isKindOf(context, TigerConstants.TYPE_CREATEMATERIAL)
                            || dMBOMObj.isKindOf(context, TigerConstants.TYPE_CREATEKIT)) {
                        dMfgProductionPlanningObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGPLANTEXTPSS_OWNERSHIP,
                                TigerConstants.ATTR_RANGE_PSS_MANUFACTURINGPLANTEXTPSS_OWNERSHIP_CONSUMER);
                        logger.debug("updateConsumerPlantOnMBOM:::  MBOMID:: " + strMBOMObjectId);
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Error in updateConsumerPlantOnMBOM: ", e);
        }
        logger.debug("updateConsumerPlantOnMBOM:::End ");
    }

    /****
     * this method used to reuse relaesed StandardMBOM
     * @param context
     * @param scopedRefPIDList
     * @return
     * @throws Exception
     */
    public static String getReleasedStandardMBOMReferenceToReuse(Context context, List<String> scopedRefPIDList, String plantPID) throws Exception {
        logger.debug("getReleasedStandardMBOMReferenceToReuse:::Start ");
        String strReusableStandardMBOMId = null;
        String isRootStandardMBOM = PropertyUtil.getRPEValue(context, STANDARDROOTMBOM, true);
        String isSkipStandardMBOM = PropertyUtil.getRPEValue(context, SKIPSTANDARDMBOM, true);
        String refPID = null;
        boolean isRPESet = false;
        try {
            if (scopedRefPIDList != null && !scopedRefPIDList.isEmpty()) {
                for (String refID : scopedRefPIDList) {
                    refPID = PLMID.buildFromString(refID).getPid();
                    // StringList slAttachedConsumerPlant = pss.mbom.MBOMUtil_mxJPO.getConsumerPlantOnStandardMBOM(context, refPID);
                    DomainObject domStdMBOMObj = DomainObject.newInstance(context, refPID);
                    String strCurrent = domStdMBOMObj.getInfo(context, DomainConstants.SELECT_CURRENT);
                    if ("true".equalsIgnoreCase(isRootStandardMBOM)) {
                        isRPESet = true;
                        String query = "print bus " + refPID + " select majorids dump |;";
                        String strResult = MqlUtil.mqlCommand(context, query, false, false);
                        if (UIUtil.isNotNullAndNotEmpty(strResult)) {
                            if (strResult.contains("|")) {
                                String[] strMajorIds = strResult.split("\\|");
                                String strFinalRefId = strMajorIds[strMajorIds.length - 1];
                                strReusableStandardMBOMId = strFinalRefId;
                            } else
                                strReusableStandardMBOMId = strResult;
                        }
                    } else if (TigerConstants.STATE_PSS_STANDARD_MBOM_RELEASE.equals(strCurrent)) {
                        String query = "print bus " + refPID + " select majorids dump |;";
                        String strResult = MqlUtil.mqlCommand(context, query, false, false);
                        if (UIUtil.isNotNullAndNotEmpty(strResult)) {
                            if (strResult.contains("|")) {
                                String[] strMajorIds = strResult.split("\\|");
                                String strFinalRefId = strMajorIds[strMajorIds.length - 1];
                                strReusableStandardMBOMId = strFinalRefId;
                            } else
                                strReusableStandardMBOMId = strResult;
                        }
                    } else
                        strReusableStandardMBOMId = refPID;
                }
                if (UIUtil.isNullOrEmpty(strReusableStandardMBOMId) && "false".equalsIgnoreCase(isRootStandardMBOM) && UIUtil.isNullOrEmpty(isSkipStandardMBOM)) {
                    PropertyUtil.setRPEValue(context, SKIPSTANDARDMBOM, "true", true);
                }
            } else if (UIUtil.isNullOrEmpty(strReusableStandardMBOMId) && "false".equalsIgnoreCase(isRootStandardMBOM)) {
                PropertyUtil.setRPEValue(context, SKIPSTANDARDMBOM, "true", true);
            }
        } catch (Exception e) {
            logger.error("Error in getReleasedStandardMBOMReferenceToReuse::: ", e);
        } finally {
            if (isRPESet) {
                PropertyUtil.setRPEValue(context, STANDARDROOTMBOM, "false", true);
            }
        }
        logger.debug("getReleasedStandardMBOMReferenceToReuse:::ReusableStandardMBOMId:::: " + strReusableStandardMBOMId);
        logger.debug("getReleasedStandardMBOMReferenceToReuse:::End ");
        return strReusableStandardMBOMId;
    }

    /**
     * this function used on Create MBOM command used for Standard MBOM creation
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public boolean showCreateMBOM(Context context, String args[]) throws Exception {
        logger.debug("showCreateMBOM:::Start ");
        boolean isReturn = true;
        try {
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            String strID = (String) paramMap.get("objectId");
            DomainObject domObject = DomainObject.newInstance(context, strID);
            String strPolicy = domObject.getInfo(context, DomainConstants.SELECT_POLICY);
            String userName = context.getUser();
            String strLoggedUserSecurityContext = PersonUtil.getDefaultSecurityContext(context, userName);
            String assignedRoles = (strLoggedUserSecurityContext.split("[.]")[0]);
            if (assignedRoles.equals(TigerConstants.ROLE_PSS_GTS_ENGINEER) && strPolicy.equals(TigerConstants.POLICY_STANDARDPART)) {
                isReturn = false;
            }
        } catch (Exception ex) {

            throw ex;
        }
        logger.debug("showCreateMBOM:::End ");
        return isReturn;
    }

    // Rutuja Ekatpure:TIGTK-10100:End

    /**
     * This method to get Product configuration
     */
    public MapList getVariantsProductConfigurationForMBOM(Context context, String args[]) throws Exception {
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        String strMBOMId = (String) paramMap.get("objectId");
        StringList selectStmts = new StringList(2);
        selectStmts.addElement(DomainConstants.SELECT_ID);
        StringList relSelectStmts = new StringList(2);
        relSelectStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
        DomainObject domMBOMObject = DomainObject.newInstance(context, strMBOMId);
        Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_PARTVARIANTASSEMBLY);
        relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_VARIANT_ASSEMBLY_PRODUCT_CONFIGURATION);

        Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_MBOM_VARIANT_ASSEMBLY);
        typePattern.addPattern(TigerConstants.TYPE_PRODUCTCONFIGURATION);
        Pattern typePostPattern = new Pattern(TigerConstants.TYPE_PRODUCTCONFIGURATION);

        MapList mlProductConfigurations = domMBOMObject.getRelatedObjects(context, relationshipPattern.getPattern() // String relationshipPattern
                , typePattern.getPattern() // String typePattern
                , selectStmts // StringList objectSelects
                , relSelectStmts // StringList relationshipSelects
                , false // boolean getTo
                , true // boolean getFrom
                , (short) 2 // short recurseToLevel
                , null // String objectWhere
                , null, (short) 0, false // checkHidden
                , true // preventDuplicates
                , (short) 1000 // pageSize
                , typePostPattern, null, null, null, null);

        if (mlProductConfigurations.size() == 0) {
            String strLanguage = context.getSession().getLanguage();
            String strAlertMessage = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", new Locale(strLanguage), "PSS_FRCMBOMCentral.Alert.NoVariantAssembly");
            strAlertMessage = MessageFormat.format(strAlertMessage, domMBOMObject.getInfo(context, DomainConstants.SELECT_NAME));
            emxContextUtil_mxJPO.mqlNotice(context, strAlertMessage);
        }
        return mlProductConfigurations;
    }

    /**
     * This Function will return a list of Product Configurations associated with Products which are related to that particular Part.
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param args
     *            holds objectList.
     * @throws Exception
     *             If the operation fails.
     */

    public MapList getProductConfigurationsListForMBOM(Context context, String[] args) throws Exception {

        PLMCoreModelerSession plmSession = null;
        boolean isTransactionActive = false;
        StringList slUniqueIds = new StringList();
        MapList mpResultList = new MapList();
        MapList mpConnectedProductList = new MapList();
        try {
            ContextUtil.startTransaction(context, true);
            isTransactionActive = true;
            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String sPrdPhysId = (String) programMap.get("objectId");

            DomainObject domMBOM = DomainObject.newInstance(context, sPrdPhysId);

            List<String> lModelsArray = FRCMBOMModelerUtility.getAttachedModelsOnReference(context, plmSession, sPrdPhysId);
            if (!lModelsArray.isEmpty()) {
                DomainObject domModel = DomainObject.newInstance(context, lModelsArray.get(0));
                String strRel = "Main Product";
                String strModelId = domModel.getInfo(context, "from[" + strRel + "].to.id");
                mpConnectedProductList.add(strModelId);
            }

            if (mpConnectedProductList.isEmpty()) {

                String test = "There is no Product Configuration to be displayed because the top levelpart" + " " + domMBOM.getInfo(context, DomainConstants.SELECT_NAME) + " "
                        + "is not connected to a Product";

                emxContextUtil_mxJPO.mqlNotice(context, test);
            } else {
                int intCount = mpConnectedProductList.size();
                StringList slUniqueProductIds = new StringList();
                for (int itr = 0; itr < intCount; itr++) {
                    if (!slUniqueProductIds.contains((String) mpConnectedProductList.get(itr))) {
                        slUniqueProductIds.add((String) mpConnectedProductList.get(itr));
                    }
                }

                int intUniqueIdListSize = slUniqueProductIds.size();
                String strObjects[] = new String[intUniqueIdListSize];
                for (int i = 0; i < intUniqueIdListSize; i++) {

                    strObjects[i] = (String) slUniqueProductIds.get(i);
                }

                DomainConstants.MULTI_VALUE_LIST.add("from[" + TigerConstants.RELATIONSHIP_PRODUCT_CONFIGURATION + "].to.id");
                DomainConstants.MULTI_VALUE_LIST.add("from[" + TigerConstants.RELATIONSHIP_PRODUCT_CONFIGURATION + "].id");
                DomainConstants.MULTI_VALUE_LIST.add("from[" + TigerConstants.RELATIONSHIP_PRODUCT_CONFIGURATION + "].to.type");
                DomainConstants.MULTI_VALUE_LIST.add("from[" + TigerConstants.RELATIONSHIP_PRODUCT_CONFIGURATION + "].name");

                StringList slSelect = new StringList();
                slSelect.add("from[" + TigerConstants.RELATIONSHIP_PRODUCT_CONFIGURATION + "].to.id");
                slSelect.add("from[" + TigerConstants.RELATIONSHIP_PRODUCT_CONFIGURATION + "].id");
                slSelect.add("from[" + TigerConstants.RELATIONSHIP_PRODUCT_CONFIGURATION + "].to.type");
                slSelect.add("from[" + TigerConstants.RELATIONSHIP_PRODUCT_CONFIGURATION + "].name");
                mpConnectedProductList = DomainObject.getInfo(context, strObjects, slSelect);

                DomainConstants.MULTI_VALUE_LIST.remove("from[" + TigerConstants.RELATIONSHIP_PRODUCT_CONFIGURATION + "].to.id");
                DomainConstants.MULTI_VALUE_LIST.remove("from[" + TigerConstants.RELATIONSHIP_PRODUCT_CONFIGURATION + "].id");
                DomainConstants.MULTI_VALUE_LIST.remove("from[" + TigerConstants.RELATIONSHIP_PRODUCT_CONFIGURATION + "].to.type");
                DomainConstants.MULTI_VALUE_LIST.remove("from[" + TigerConstants.RELATIONSHIP_PRODUCT_CONFIGURATION + "].name");
                intCount = mpConnectedProductList.size();

                int intPCCount = 0;
                HashMap tempMap = null;
                for (int i = 0; i < intCount; i++) {
                    StringList tempIdSl = (StringList) ((Map) mpConnectedProductList.get(i)).get("from[" + TigerConstants.RELATIONSHIP_PRODUCT_CONFIGURATION + "].to.id");
                    StringList tempRelIdSl = (StringList) ((Map) mpConnectedProductList.get(i)).get("from[" + TigerConstants.RELATIONSHIP_PRODUCT_CONFIGURATION + "].id");
                    StringList tempRelNameSl = (StringList) ((Map) mpConnectedProductList.get(i)).get("from[" + TigerConstants.RELATIONSHIP_PRODUCT_CONFIGURATION + "].name");
                    StringList tempTypeSl = (StringList) ((Map) mpConnectedProductList.get(i)).get("from[" + TigerConstants.RELATIONSHIP_PRODUCT_CONFIGURATION + "].to.type");
                    intPCCount = tempIdSl.size();
                    for (int j = 0; j < intPCCount; j++) {
                        String objId = (String) tempIdSl.get(j);
                        if (!slUniqueIds.contains(objId)) {
                            tempMap = new HashMap();
                            tempMap.put(DomainConstants.SELECT_ID, (String) tempIdSl.get(j));
                            tempMap.put(DomainConstants.SELECT_RELATIONSHIP_ID, (String) tempRelIdSl.get(j));
                            tempMap.put(DomainConstants.SELECT_RELATIONSHIP_NAME, (String) tempRelNameSl.get(j));
                            tempMap.put(DomainConstants.SELECT_TYPE, (String) tempTypeSl.get(j));
                            mpResultList.add(tempMap);
                        } else {
                            slUniqueIds.add(objId);
                        }
                    }
                }
                if (mpResultList.size() == 0) {
                    String strAbsenceOfPC = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.Alert.AbsenceOfPC");
                    emxContextUtil_mxJPO.mqlNotice(context, strAbsenceOfPC);
                }
            }

            flushAndCloseSession(plmSession);
            if (isTransactionActive) {
                ContextUtil.commitTransaction(context);
            }

        } catch (Exception ex) {
            logger.error("Error in getProductConfigurationsListForMBOM : ", ex);
            flushAndCloseSession(plmSession);
            if (isTransactionActive) {
                ContextUtil.abortTransaction(context);
            }
            throw ex;
        }
        return mpResultList;
    }

    public static MapList getExpandMBOMonPC(Context context, String objectId, int expLvl, String PCId, StringList relSelect, StringList busSelect) throws Exception, FrameworkException {
        // Common MBOM expand method for IndentedTable and GraphicalBrowser
        MapList res = null;

        String pcGlobalFilterCompExpr = null;
        String pcGlobalFilterXMLValue = null;

        PLMCoreModelerSession plmSession = null;
        try {
            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            // Prepare the filter expression if there is one
            if (UIUtil.isNotNullAndNotEmpty(PCId) && !PCId.equalsIgnoreCase("undefined")) {
                Map mReturnMap = PSS_FRCMBOMModelerUtility_mxJPO.getExpressionsForPC(context, PCId);
                if (!mReturnMap.isEmpty()) {
                    pcGlobalFilterCompExpr = (String) mReturnMap.get("pcGlobalFilterCompExpr");
                    pcGlobalFilterXMLValue = (String) mReturnMap.get("pcGlobalFilterXMLValue");
                }
            }
            res = FRCMBOMModelerUtility.getVPMStructure(context, plmSession, objectId, busSelect, relSelect, (short) expLvl, pcGlobalFilterCompExpr, pcGlobalFilterXMLValue);

        } finally {
            closeSession(plmSession);
        }

        return res;
    }

    /**
     * This method is used to Hide the visibility of Edit commands for Global View of MBOM
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @TIGTK-10260
     */

    public boolean showAccessForGlobalView(Context context, String args[]) throws Exception {
        logger.debug("showAccessForGlobalView: Start ");
        boolean boolReturn = true;
        try {
            HashMap<?, ?> param = (HashMap<?, ?>) JPO.unpackArgs(args);
            String strView = (String) param.get("globalView");
            if (UIUtil.isNotNullAndNotEmpty(strView) && "true".equals(strView)) {
                boolReturn = false;
            }

        } catch (Exception ex) {
            logger.error("showAccessForGlobalView: error is  : " + ex);
            throw ex;
        }
        logger.debug("showAccessForGlobalView: End ");
        return boolReturn;

    }

    /**
     * This method is used Clone the MBOM related Items
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @TIGTK-10072
     */
    // Sneha:Start
    public static void copyMBOMItemsToMBOMReference(Context context, PLMCoreModelerSession plmSession, String mbomRefPID, String templateRefID) throws Exception {
        logger.debug("copyMBOMItemsToMBOMReference: Start ");
        try {
            DomainObject domMBOM = DomainObject.newInstance(context, templateRefID);
            DomainObject domNewMBOMId = DomainObject.newInstance(context, mbomRefPID);

            if (domMBOM.isKindOf(context, TigerConstants.TYPE_CREATEASSEMBLY) || domMBOM.isKindOf(context, TigerConstants.TYPE_CREATEMATERIAL)) {

                Pattern typePattern = new Pattern(TigerConstants.TYPE_CREATEASSEMBLY);
                typePattern.addPattern(TigerConstants.TYPE_PSS_COLOROPTION);
                typePattern.addPattern(TigerConstants.TYPE_PSS_HARMONY);
                typePattern.addPattern(TigerConstants.TYPE_PSS_HARMONY_REQUEST);
                typePattern.addPattern(TigerConstants.TYPE_PSS_MBOM_VARIANT_ASSEMBLY);
                typePattern.addPattern(TigerConstants.TYPE_PSS_DOCUMENT);
                typePattern.addPattern(TigerConstants.TYPE_CREATEMATERIAL);

                Pattern RelPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_SPAREMANUFACTURINGITEM);
                RelPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_HARMONY_REQUEST);
                RelPattern.addPattern(TigerConstants.RELATIONSHIP_PSSMBOM_HARMONIES);
                RelPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_PARTVARIANTASSEMBLY);
                RelPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_COLORLIST);
                RelPattern.addPattern(DomainConstants.RELATIONSHIP_REFERENCE_DOCUMENT);
                RelPattern.addPattern("MfgProcessAlternate");

                StringList slObjSelectStmts = new StringList();
                slObjSelectStmts.addElement(DomainConstants.SELECT_ID);
                slObjSelectStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
                slObjSelectStmts.addElement(DomainConstants.SELECT_NAME);

                MapList mlMBOMObjectList = domMBOM.getRelatedObjects(context, RelPattern.getPattern(), // Relationship Pattern
                        typePattern.getPattern(), // Object Pattern
                        null, // Object Selects
                        slObjSelectStmts, // Relationship Selects
                        false, // to direction
                        true, // from direction
                        (short) 0, // recursion level
                        null, // object where clause
                        null, (short) 0, false, // checkHidden
                        true, // preventDuplicates
                        (short) 1000, // pageSize
                        null, // Post Type Pattern
                        null, null, null);

                for (int i = 0; i < mlMBOMObjectList.size(); i++) {
                    Map mMBOMMap = (Map) mlMBOMObjectList.get(i);
                    String strRelId = (String) mMBOMMap.get("id[connection]");

                    String strObjectId = MqlUtil.mqlCommand(context, "print connection " + strRelId + " select to.id dump |", false, false);
                    DomainObject domInstance = DomainObject.newInstance(context, strObjectId);

                    String strRel = (String) mMBOMMap.get("relationship");
                    DomainRelationship instObj = new DomainRelationship(strRelId);
                    Map instAttributes = instObj.getAttributeMap(context, true);
                    DomainRelationship domainRelationship = DomainRelationship.connect(context, domNewMBOMId, strRel, domInstance);

                    if (!instAttributes.isEmpty()) {
                        domainRelationship.setAttributeValues(context, instAttributes);
                    }
                }

            }
        } catch (RuntimeException ex) {
        } catch (Exception ex) {
            logger.error("copyMBOMItemsToMBOMReference: error is  : " + ex);
            throw ex;
        }
        logger.debug("copyMBOMItemsToMBOMReference: End ");
    }

    // Sneha:End
    /**
     * This method is used Copy the Effectivity while clonning the MBOM
     * @param context
     * @param args
     * @author sirale
     * @throws Exception
     * @TIGTK-10072
     */
    public static void setEffectivity(Context context, PLMCoreModelerSession plmSession, String[] args, String newRefPID) throws Exception {
        logger.debug("setEffectivity: Start ");
        try {
            String oldInstancePID = args[0];
            String newInstancePID = args[1];

            // Get the attribute values of the instance
            DomainRelationship instObj = new DomainRelationship(oldInstancePID);
            Map instAttributes = instObj.getAttributeMap(context, true);
            instAttributes.remove("PLMInstance.V_TreeOrder");

            // Get the effectivity of the instance
            List<String> oldInstancePIDList = new ArrayList<String>();
            oldInstancePIDList.add(oldInstancePID);

            // Get the parent reference of the instance
            String parentRefPID = MqlUtil.mqlCommand(context, "print connection " + oldInstancePID + " select from.physicalid dump |", false, false);

            Map<String, String> effMap = FRCMBOMModelerUtility.getEffectivityOnInstanceList(context, plmSession, oldInstancePIDList, false);

            String effXMLStr = effMap.get(oldInstancePID);

            // Get the effectivity checksum of the instance
            String checksum = FRCMBOMModelerUtility.getEffectivityChecksumStoredOnInstance(context, oldInstancePID);

            // Replicate all the attributes values on the new instance
            DomainRelationship newInstObj = new DomainRelationship(newInstancePID);
            newInstObj.setAttributeValues(context, instAttributes);

            // Replicate the effectivity on this new instance
            if (effXMLStr != null && !"".equals(effXMLStr)) {
                // Set the model on the parent reference of the instance
                List<String> parentModelPIDList = FRCMBOMModelerUtility.getAttachedModelsOnReference(context, plmSession, parentRefPID);

                List<String> newParentRefPIDList = new ArrayList<String>();
                newParentRefPIDList.add(newRefPID);

                FRCMBOMModelerUtility.attachModelsOnReferencesAndSetOptionsCriteria(context, plmSession, newParentRefPIDList, parentModelPIDList);
                FRCMBOMModelerUtility.setOrUpdateEffectivityOnInstance(context, plmSession, newInstancePID, effXMLStr);
            }
            // Replicate the effectivity checksum on the new instance
            FRCMBOMModelerUtility.storeEffectivityChecksumOnInstance(context, newInstancePID, checksum);

        } catch (Exception ex) {
            logger.error("setEffectivity: error is  : " + ex);
            throw ex;
        }
        logger.debug("setEffectivity: End ");
    }

    // TIGTK-10606:Rutuja Ekatpure:Start
    /***
     * this method used for checking MBOM created for same plant by different revision of part
     * @param context
     * @param plmSession
     * @param strObjectId
     * @param strPlantId
     * @throws Exception
     */
    public static void checkMBOMCreatedForOtherRevisionsOfPart(Context context, PLMCoreModelerSession plmSession, String strObjectId, String strPlantId) throws Exception {
        logger.debug("checkMBOMCreatedForOtherRevisionsOfPart: Start ");
        try {
            DomainObject domEBOMObj = DomainObject.newInstance(context, strObjectId);
            DomainObject domPlantObj = DomainObject.newInstance(context, strPlantId);
            String strPlantName = domPlantObj.getInfo(context, DomainConstants.SELECT_NAME);
            StringList singleValueSelects = new StringList(DomainObject.SELECT_ID);
            StringList multiValueSelects = new StringList();
            // get all Revision of part
            MapList mlEBOMRevs = domEBOMObj.getRevisionsInfo(context, singleValueSelects, multiValueSelects);
            Iterator itr = mlEBOMRevs.iterator();
            // Iterate for all revision
            while (itr.hasNext()) {
                Map mRev = (Map) itr.next();
                String strEBOMRevId = (String) mRev.get(DomainObject.SELECT_ID);
                // get VPMReference for part
                MapList mlProduct = getProductFromEBOM(context, strEBOMRevId);
                if (!mlProduct.isEmpty()) {
                    Map mPrd = (Map) mlProduct.get(0);
                    String sPrdPhysId = (String) mPrd.get("physicalid");
                    if (!"".equals(sPrdPhysId)) {
                        // check existing MBOM for same plant
                        if (checkForExistingMBOMWithPlant(context, plmSession, sPrdPhysId, strPlantId)) {
                            // if same revision then give message "Cannot create new item for this Plant, as it already exists. Please re-use the existing item."
                            if (strEBOMRevId.equalsIgnoreCase(strObjectId)) {
                                throw new Exception(EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(),
                                        "PSS_FRCMBOMCentral.Error.Message.CanNotCreateNewItemWithPlant"));
                            } // if different revision then give message "MBOM already exists for Plant XXX, and is available for updates."
                            else {
                                String strErrorMSg = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(),
                                        "PSS_FRCMBOMCentral.Error.Message.CanNotCreateNewItemWithPlantAsCreatedWithOtherRev");
                                throw new Exception(strErrorMSg.replace("$<name>", strPlantName));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("checkMBOMCreatedForOtherRevisionsOfPart: error is  : " + e);
            throw e;
        }

        logger.debug("checkMBOMCreatedForOtherRevisionsOfPart: End ");
    }

    // TIGTK-10606:Rutuja Ekatpure:End

    /***
     * this method used for Getting Colorable value for Particular MBOM
     * @param context
     * @param plmSession
     * @param strMBOMId
     *            TIGTK-10503
     * @throws Exception
     */
    public static String getColorableValue(Context context, String strMBOMId, PLMCoreModelerSession plmSession) throws Exception {
        logger.debug("getColorableValue: Start ");
        String strUpdateOfColorOptions = DomainConstants.EMPTY_STRING;
        String strColorable = DomainConstants.EMPTY_STRING;
        try {
            List<String> lMBOMPhysicalId = new ArrayList();
            lMBOMPhysicalId.add(strMBOMId);
            List<String> psRefPIDList = FRCMBOMModelerUtility.getScopedPSReferencePIDFromList(context, plmSession, lMBOMPhysicalId);
            String strPSPhysicalId = DomainConstants.EMPTY_STRING;
            if (psRefPIDList != null && psRefPIDList.size() > 0) {
                strPSPhysicalId = psRefPIDList.get(0);
            }
            if (UIUtil.isNotNullAndNotEmpty(strPSPhysicalId)) {
                DomainObject domObjectPP = DomainObject.newInstance(context, strPSPhysicalId);
                strColorable = domObjectPP.getInfo(context, "attribute[" + TigerConstants.ATTRIBUTE_PSS_PUBLISHEDPART_PSS_PARTCOLORABLE + "]");
            }
        } catch (Exception ex) {
            logger.error("getColorableValue: error is  : " + ex);
            throw ex;
        }
        logger.debug("getColorableValue: End ");
        return strColorable;
    }

    /**
     * FCIndex Sync after MBO-167
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public static String updateIntermediateScope(Context context, String[] args) throws Exception { // Called from FRCUpdateScopePostProcess.jsp
        PLMCoreModelerSession plmSession = null;
        StringBuffer errorBuffer = new StringBuffer();
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();
            IPLMxCoreAccess coreAccess = plmSession.getVPLMAccess();

            String mbomPath = args[0];
            String psPath = args[1];

            String[] mbomPathList = mbomPath.split("/");
            if (mbomPathList.length < 2)
                throw new Exception("You cannot select the root Manufacturing Item for an intermediate scope update.");

            String mbomLeafInstPID = mbomPathList[mbomPathList.length - 1];
            String mbomLeafRefPID = MqlUtil.mqlCommand(context, "print connection " + mbomLeafInstPID + " select to.physicalid dump |", false, false);
            String mbomLeafObjId = MqlUtil.mqlCommand(context, "print connection " + mbomLeafInstPID + " select to.id dump |", false, false);

            DomainObject domMfgItem = new DomainObject(mbomLeafObjId);
            String mbomObjName = domMfgItem.getInfo(context, DomainConstants.SELECT_NAME);

            // Check that the MBOM leaf reference is not of type Provide (or similar)
            String mbomLeafRefType = MqlUtil.mqlCommand(context, "print bus " + mbomLeafRefPID + " select type dump |", false, false);
            boolean isIndirect = false;
            for (String typeInList : baseTypesForMBOMAssemblyNodes) {
                if (getDerivedTypes(context, typeInList).contains(mbomLeafRefType))
                    isIndirect = true;
            }
            if (isIndirect) {
                // throw new Exception("This type of Manufacturing Item is not valid for a sub-scope.");
                // Check if the MBOM leaf reference already has a scope

                List<String> inputListForGetScope = new ArrayList<String>();
                inputListForGetScope.add(mbomLeafRefPID);
                String currentPSRefScopePID = FRCMBOMModelerUtility.getScopedPSReferencePIDFromList(context, plmSession, inputListForGetScope).get(0);

                if (UIUtil.isNotNullAndNotEmpty(currentPSRefScopePID)) {
                    // throw new Exception("The Manufacturing Item you have selected does not have any scope.");

                    // Check if the scope and implement link of the MBOM is already up-to-date
                    List<String> implementLink = FRCMBOMModelerUtility.getImplementLinkInfoSimple(context, plmSession, mbomLeafInstPID);
                    if (implementLink.size() > 0) {
                        // throw new Exception("The Manufacturing Item you have selected does not have any implement link.");

                        boolean implementLinkIsUpToDate = false;
                        String implementLinkInstancePID = null;

                        if (psPath != null && !"".equals(psPath)) {
                            implementLinkIsUpToDate = true;

                            for (int n = 0; n < implementLink.size(); n++) {
                                implementLinkInstancePID = implementLink.get(n);
                                if (!psPath.contains(implementLinkInstancePID))
                                    implementLinkIsUpToDate = false;
                            }
                        }

                        if (implementLinkIsUpToDate) {
                            // Check if the scope is up to date
                            String implementLinkLeafRefPID = MqlUtil.mqlCommand(context, "print connection " + implementLinkInstancePID + " select to.physicalid dump |", false, false);

                            List<String> modifiedInstanceList = FRCMBOMModelerUtility.reconnectScopeLinkAndUpdateChildImplementLinks(context, plmSession, mbomLeafRefPID, implementLinkLeafRefPID,
                                    true);
                            // PSS: Sync FCS Index : START
                            PSS_FRCMBOMModelerUtility_mxJPO.setFCSIndexOnMBOM(context, implementLinkLeafRefPID, mbomLeafRefPID);
                            // PSS: Sync FCS Index : END
                            if (implementLinkLeafRefPID.equals(currentPSRefScopePID)) {
                                // throw new Exception("The scope is already up to date.");

                                errorBuffer.append("\"");
                                errorBuffer.append(mbomObjName);
                                errorBuffer.append("\"");
                                errorBuffer.append(":");
                                errorBuffer.append("The scope is already up to date");
                                errorBuffer.append("\n");
                            }
                        } else { // Implement link is not up to date : first, try to update it based on logicalids
                            String implementLinkLeafInstPID = null;
                            String implementLinkLeafRefPID = null;
                            int implementLinkUpdateInfo = FRCMBOMModelerUtility.updateImplementLinkFromCandidate(context, plmSession, mbomLeafInstPID);

                            if (implementLinkUpdateInfo == 0) { // Implement link is, in fact, up to date
                                // Get the implement link
                                implementLink = FRCMBOMModelerUtility.getImplementLinkInfoSimple(context, plmSession, mbomLeafInstPID);
                                implementLinkLeafInstPID = implementLink.get(implementLink.size() - 1);
                                implementLinkLeafRefPID = MqlUtil.mqlCommand(context, "print connection " + implementLinkLeafInstPID + " select to.physicalid dump |", false, false);

                                FRCMBOMModelerUtility.reconnectScopeLinkAndUpdateChildImplementLinks(context, plmSession, mbomLeafRefPID, implementLinkLeafRefPID, true);
                                // PSS: Sync FCS Index : START
                                PSS_FRCMBOMModelerUtility_mxJPO.setFCSIndexOnMBOM(context, implementLinkLeafRefPID, mbomLeafRefPID);
                                // PSS: Sync FCS Index : END
                                if (implementLinkLeafRefPID.equals(currentPSRefScopePID)) {

                                    errorBuffer.append("\"").append(mbomObjName).append("\"").append(":").append("The scope is already up to date.").append("\n");
                                }
                            } else if (implementLinkUpdateInfo == 3) { // Only the effectivity has been updated
                                // Get the implement link
                                implementLink = FRCMBOMModelerUtility.getImplementLinkInfoSimple(context, plmSession, mbomLeafInstPID);
                                implementLinkLeafInstPID = implementLink.get(implementLink.size() - 1);
                                implementLinkLeafRefPID = MqlUtil.mqlCommand(context, "print connection " + implementLinkLeafInstPID + " select to.physicalid dump |", false, false);

                                FRCMBOMModelerUtility.reconnectScopeLinkAndUpdateChildImplementLinks(context, plmSession, mbomLeafRefPID, implementLinkLeafRefPID, true);
                                // PSS: Sync FCS Index : START
                                PSS_FRCMBOMModelerUtility_mxJPO.setFCSIndexOnMBOM(context, implementLinkLeafRefPID, mbomLeafRefPID);
                                // PSS: Sync FCS Index : END

                                if (implementLinkLeafRefPID.equals(currentPSRefScopePID)) {
                                    // throw new Exception("The scope is already up to date.");
                                    errorBuffer.append("\"");
                                    errorBuffer.append(mbomObjName);
                                    errorBuffer.append("\"");
                                    errorBuffer.append(":");
                                    errorBuffer.append("The scope is already up to date");
                                    errorBuffer.append("\n");
                                }
                            } else if (implementLinkUpdateInfo == 1) { // Implement link is rerouted
                                // Get the new implement link
                                implementLink = FRCMBOMModelerUtility.getImplementLinkInfoSimple(context, plmSession, mbomLeafInstPID);
                                implementLinkLeafInstPID = implementLink.get(implementLink.size() - 1);
                                implementLinkLeafRefPID = MqlUtil.mqlCommand(context, "print connection " + implementLinkLeafInstPID + " select to.physicalid dump |", false, false);
                            } else if (implementLinkUpdateInfo == 4) { // Implement link is broken : update it to the psPath
                                // if (psPath == null || "".equals(psPath))
                                if (UIUtil.isNotNullAndNotEmpty(psPath)) {
                                    // throw new Exception("The implement link of this Manufacturing Item is broken : you must also select a Part in the EBOM to update it.");

                                    String[] psPathList = psPath.split("/");
                                    if (psPathList.length < 2)
                                        throw new Exception("You cannot select the root Part for an intermediate scope update.");

                                    String trimmedPSPath = trimPSPathToClosestScope(context, plmSession, mbomPath, psPath);

                                    if (trimmedPSPath == null || "".equals(trimmedPSPath))
                                        throw new Exception("No parent scope exists.");

                                    // Remove any existing implement link
                                    FRCMBOMModelerUtility.deleteImplementLink(context, plmSession, mbomLeafInstPID, true);

                                    // Put a new implement link and update the effectivity
                                    List<String> mbomLeafInstancePIDList = new ArrayList();
                                    mbomLeafInstancePIDList.add(mbomLeafInstPID);
                                    List<String> trimmedPSPathList = new ArrayList();
                                    trimmedPSPathList.add(trimmedPSPath);
                                    String retStr = FRCMBOMModelerUtility.setImplementLinkBulk(context, plmSession, mbomLeafInstancePIDList, trimmedPSPathList, true);
                                    if (!"".equals(retStr))
                                        throw new Exception(retStr);

                                    String[] trimmedPSPathPIDList = trimmedPSPath.split("/");
                                    implementLinkLeafInstPID = trimmedPSPathPIDList[trimmedPSPathPIDList.length - 1];
                                    implementLinkLeafRefPID = MqlUtil.mqlCommand(context, "print connection " + implementLinkLeafInstPID + " select to.physicalid dump |", false, false);

                                    // Reconnection is done
                                    // Implement link is now up to date, but not the scope : update it to the leaf ref of the implement link
                                    FRCMBOMModelerUtility.reconnectScopeLinkAndUpdateChildImplementLinks(context, plmSession, mbomLeafRefPID, implementLinkLeafRefPID, true);
                                    // PSS: Sync FCS Index : START
                                    PSS_FRCMBOMModelerUtility_mxJPO.setFCSIndexOnMBOM(context, implementLinkLeafRefPID, mbomLeafRefPID);
                                    // PSS: Sync FCS Index : END

                                } else {
                                    errorBuffer.append("\"");
                                    errorBuffer.append(mbomObjName);
                                    errorBuffer.append("\"");
                                    errorBuffer.append(":");
                                    errorBuffer.append("The implement link of this Manufacturing Item is broken : you must also select a Part in the EBOM to update it.");
                                    errorBuffer.append("\n");
                                }
                            }
                        }
                    } else {

                        errorBuffer.append("\"");
                        errorBuffer.append(mbomObjName);
                        errorBuffer.append("\"");
                        errorBuffer.append(":");
                        errorBuffer.append("Manufacturing Item you have selected does not have any implement link");
                        errorBuffer.append("\n");
                    }
                } else {
                    errorBuffer.append("\"");
                    errorBuffer.append(mbomObjName);
                    errorBuffer.append("\"");
                    errorBuffer.append(":");
                    errorBuffer.append("Manufacturing Item you have selected does not have any scope");
                    errorBuffer.append("\n");
                }
            } else {
                errorBuffer.append("\"");
                errorBuffer.append(mbomObjName);
                errorBuffer.append("\"");
                errorBuffer.append(":");
                errorBuffer.append("This type of Manufacturing Item is not valid for a sub-scope");
                errorBuffer.append("\n");
            }

            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);
            // ContextUtil.abortTransaction(context);
        } catch (Exception exp) {
            exp.printStackTrace();
            flushAndCloseSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw exp;
        }
        // FRC changes added for MBO-167 :H65:09/10/2017-END
        return errorBuffer.toString();
    }

    // ==== START : Manage Plants
    /***
     * This method used for Getting Plants of MBOM object
     * @param context
     * @param plmSession
     * @param args
     * @return MapList of Plant objects
     * @author psalunke : TIGTK-12875
     * @since 18-01-2017
     * @throws Exception
     */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getPlantsFromCreateAssembly(Context context, String[] args) { // Called from command FRCMBOMPlants
        MapList retList = new MapList();
        boolean bTrans = false;

        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, false);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            bTrans = true;
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            if (null != programMap) {
                String sId = (String) programMap.get("objectId");
                if (null != sId && !"".equals(sId)) {
                    List<String> lPlants = FRCMBOMModelerUtility.getPlantsAttachedToMBOMReference(context, plmSession, sId);
                    for (int i = 0; i < lPlants.size(); i++) {
                        String sPlantPID = (String) lPlants.get(i);
                        HashMap mTmp = new HashMap();
                        // TIGTK-12875 : 18-01-2018 : START
                        DomainObject domPlant = DomainObject.newInstance(context, sPlantPID);
                        String strPlantType = domPlant.getInfo(context, DomainConstants.SELECT_TYPE);
                        if (TigerConstants.TYPE_PSS_PLANT.equalsIgnoreCase(strPlantType)) {
                            mTmp.put(DomainConstants.SELECT_ID, sPlantPID);
                            retList.add(mTmp);
                        }
                        // TIGTK-12875 : 18-01-2018 : END
                    }
                }
            }
            if (bTrans)
                closeSession(plmSession);
            ContextUtil.commitTransaction(context);
        } catch (Exception e) {
            e.printStackTrace();
            if (bTrans)
                closeSession(plmSession);
            ContextUtil.abortTransaction(context);
        }
        return retList;
    }

    /**
     * MBO-164.Performance improvement
     * @param context
     * @param plmSession
     * @param args
     * @return
     * @throws Exception
     */
    public static String createMBOMFromEBOMLikePS_new(Context context, PLMCoreModelerSession plmSession, String[] args) throws Exception {
        long startTime = System.currentTimeMillis();

        String rootRefPID = "";

        FRCMBOMModelerUtility.checkValidScenario(context);
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String sPartId = (String) programMap.get("objectId");

        // PSS: START
        String plantId = (String) programMap.get("PSS_PlantOID");
        DomainObject plantObj = DomainObject.newInstance(context, plantId);
        plantId = plantObj.getInfo(context, "physicalid");
        // PSS: END

        valueEnvAttachModel = System.getenv("DISABLE_ATTACH_MODEL_ON_SCOPE");

        // Get the change object
        String changeObjectName = (String) programMap.get("FRCMBOMGetChangeObject");
        // Modif AFN - Test if a value has been defined into the creation web form
        String changeObjectFromForm = (String) programMap.get("ChangeObjectCreateForm");
        if (null != changeObjectFromForm && !"".equals(changeObjectFromForm))
            changeObjectName = changeObjectFromForm;

        MapList mlPrd = getProductFromEBOM(context, sPartId);
        if (null != mlPrd) {
            if (1 < mlPrd.size())
                throw new Exception("Several VPM Products have been found for the given EBOM part");
            if (mlPrd.size() == 0)
                throw new Exception("No VPM Products have been found for the given EBOM part. Please do a \"Collaborate with Physical\".");
            Map mPrd = (Map) mlPrd.get(0);
            String sPrdPhysId = (String) mPrd.get("physicalid");
            if (null != sPrdPhysId && !"".equals(sPrdPhysId)) {
                Map<String, List<String>> workingInfo = new HashMap<String, List<String>>();
                workingInfo.put("instanceToCreate_parentRefPLMID", new ArrayList<String>());
                workingInfo.put("instanceToCreate_childRefPLMID", new ArrayList<String>());
                workingInfo.put("mbomLeafInstancePIDList", new ArrayList<String>());
                workingInfo.put("psPathList", new ArrayList<String>());
                workingInfo.put("newRefPIDList", new ArrayList<String>());
                workingInfo.put("newScopesToCreate_MBOMRefPIDs", new ArrayList<String>());
                workingInfo.put("newScopesToCreate_MBOMRefPLMIDs", new ArrayList<String>());
                workingInfo.put("newScopesToCreate_PSRefPIDs", new ArrayList<String>());
                workingInfo.put("newScopeObjectList", new ArrayList<String>());

                List<Map<String, String>> workingInfo_instanceAttributes = new ArrayList<Map<String, String>>();
                List<Integer> workingInfo_indexInstancesForImplement = new ArrayList<Integer>();
                Set<String> workingInfo_lModelListOnStructure = new HashSet<String>();
                Map<String, String> workingInfo_AppDateToValuate = new HashMap<String, String>();

                // Recursively process the PS root node and create the MBOM references
                String sRet = createMBOMFromEBOMLikePSRecursive_new(context, plmSession, null, null, sPrdPhysId, null, workingInfo, workingInfo_lModelListOnStructure, workingInfo_instanceAttributes,
                        workingInfo_indexInstancesForImplement, workingInfo_AppDateToValuate, plantId, null);// newRefPIDList, newScopesToCreate_MBOMRefPIDs, newScopesToCreate_PSRefPIDs);

                flushSession(plmSession);

                rootRefPID = PLMID.buildFromString(sRet).getPid();
                // Valuate the V_ApplicabilityDate attributes
                for (Entry<String, String> entrySet : workingInfo_AppDateToValuate.entrySet()) {
                    String refPID = entrySet.getKey();
                    MqlUtil.mqlCommand(context, "mod bus " + refPID + " PLMReference.V_ApplicabilityDate '" + workingInfo_AppDateToValuate.get(refPID) + "'", false, false);
                }
                // Create all the MBOM instances in one shot
                List<String> allCreatedInstancesPIDList = new ArrayList<String>();
                Map<String, Map<String, String>> validateAttributeMap = new HashMap<String, Map<String, String>>();
                workingInfo.put("mbomLeafInstancePIDList", createInstanceBulk(context, plmSession, workingInfo.get("instanceToCreate_parentRefPLMID"),
                        workingInfo.get("instanceToCreate_childRefPLMID"), workingInfo_instanceAttributes, workingInfo_indexInstancesForImplement, allCreatedInstancesPIDList, validateAttributeMap));

                flushSession(plmSession);

                // MBO-164-MBOM performance issue:START-H65 15/11/2017
                String[] strArray = null;
                StringList slSplitExtensionList = new StringList();
                for (String instancePID : allCreatedInstancesPIDList) {
                    String strExtension = MqlUtil.mqlCommand(context, "print connection " + instancePID + " select interface dump |", false, false);
                    if (UIUtil.isNotNullAndNotEmpty(strExtension)) {
                        if (strExtension.contains("|")) {
                            strArray = strExtension.split("\\|");
                            if (strArray.length > 0) {
                                for (int i = 0; i < strArray.length; i++)
                                    slSplitExtensionList.add(strArray[i]);
                            }
                        } else
                            slSplitExtensionList.add(strExtension);
                    }
                    if (!slSplitExtensionList.contains("FRCCustoExtension1"))
                        MqlUtil.mqlCommand(context, "mod connection " + instancePID + " add interface FRCCustoExtension1", false, false);

                    // MqlUtil.mqlCommand(context, "mod connection " + instancePID + " add interface FRCCustoExtension1", false, false);
                }
                // MBO-164-Below MBOM performance issue:END-H65 15/11/2017

                // In the list of models to attach to the root, add the models attached to the root PS
                workingInfo_lModelListOnStructure.addAll(FRCMBOMModelerUtility.getAttachedModelsOnReference(context, plmSession, sPrdPhysId));

                // Attach all Models to the Root
                if (workingInfo_lModelListOnStructure.size() > 0) {
                    List lBOMRef = new ArrayList<List>();
                    lBOMRef.add(rootRefPID);
                    FRCMBOMModelerUtility.attachModelsOnReferencesAndSetOptionsCriteria(context, plmSession, lBOMRef, new ArrayList<String>(workingInfo_lModelListOnStructure));
                }

                // Create all the scope links in one shot
                FRCMBOMModelerUtility.createScopeLinkBulk(context, plmSession, workingInfo.get("newScopesToCreate_MBOMRefPIDs"), workingInfo.get("newScopesToCreate_PSRefPIDs"));

                flushSession(plmSession);

                // List<String> mbomInstancePIDList = new ArrayList<String>();
                /*
                 * if (workingInfo.get("instanceToCreate_childRefPLMID").size() > 0) { for (int z = 0; z < workingInfo.get("instanceToCreate_childRefPLMID").size(); z++) { List<String>
                 * strChildInstanceForImplementLink = workingInfo.get(z);
                 * 
                 * } }
                 */

                // Create all the implement links in one shot
                if (!allCreatedInstancesPIDList.isEmpty()) {
                    PSS_FRCMBOMModelerUtility_mxJPO.setImplementLinkBulk(context, plmSession, allCreatedInstancesPIDList, workingInfo.get("psPathList"), true);

                    flushSession(plmSession);

                    setInstAttributeValues(context, validateAttributeMap, allCreatedInstancesPIDList);
                }
                //
                // Attach all created references to change object.
                attachListObjectsToChange(context, plmSession, changeObjectName, workingInfo.get("newRefPIDList"));

            }
        } else {
            throw new Exception("No VPM Product found for the given EBOM part");
        }

        long endTime = System.currentTimeMillis();
        System.out.println("EPI PERFOS : createMBOMFromEBOMLikePS_new = " + (endTime - startTime));

        return rootRefPID;
    }

    public static String createMBOMFromEBOMLikePSRecursive_new(Context context, PLMCoreModelerSession plmSession, String mbomParentRefPLMID, String mbomCompleteParentPath, String psRefID,
            String psCompletePath, Map<String, List<String>> workingInfo, Set<String> workingInfo_lModelListOnStructure, List<Map<String, String>> workingInfo_instanceAttributes,
            List<Integer> workingInfo_indexInstancesForImplement, Map<String, String> workingInfo_AppDateToValuate, String plantPID, String parentType) throws Exception {
        String newMBOMRefPLMID = null;
        String newMBOMRefPID = null;

        boolean checkRPE = false;

        // Get all the first level instances of the PS reference
        StringList busSelect = new StringList();
        busSelect.add("physicalid");
        StringList relSelect = new StringList();
        relSelect.add("physicalid[connection]");

        // Bug #231 - DCP - START
        relSelect.add("attribute[PLMInstance.V_TreeOrder].value");

        // Rutuja Ekatpure:TIGTK-10100:2/10/2017:Start
        MapList psInstList = null;
        String result = MqlUtil.mqlCommand(context, "print bus " + psRefID + " select interface dump |", false, false);
        boolean isStandardMBOM = false;
        DomainObject psRefObj = new DomainObject(psRefID);
        Map psRefAttributes = psRefObj.getAttributeMap(context, true);
        if (UIUtil.isNotNullAndNotEmpty(result) && result.contains("PSS_PublishedPart") && "Standard".equals(psRefAttributes.get("PSS_PublishedPart.PSS_StandardReference"))) {
            isStandardMBOM = true;
        }
        // Rutuja Ekatpure:TIGTK-10100:2/10/2017:End
        if (UIUtil.isNotNullAndNotEmpty(result) || result.contains("PSS_PublishedPart")) {
            psInstList = PSS_FRCMBOMModelerUtility_mxJPO.getVPMStructureMQL(context, psRefID, busSelect, relSelect, (short) 1, null,
                    "(attribute[PSS_PublishedEBOM.PSS_InstanceName]==EBOM || attribute[PSS_PublishedEBOM.PSS_InstanceName]=='')"); // Expand
            // first
            // level
        } else {
            psInstList = PSS_FRCMBOMModelerUtility_mxJPO.getVPMStructure(context, plmSession, psRefID, busSelect, relSelect, (short) 1, null, null); // Expand first level
        }

        // Expand first level
        String mbomParentRef = DomainConstants.EMPTY_STRING;
        if (UIUtil.isNotNullAndNotEmpty(mbomParentRefPLMID)) {
            mbomParentRef = PLMID.buildFromString(mbomParentRefPLMID).getPid();
        }

        if (mbomParentRef != null && !"".equals(mbomParentRef)) {
            DomainObject domParentMBOM = DomainObject.newInstance(context, mbomParentRef);
            String strParentMBOMPolicy = domParentMBOM.getInfo(context, DomainConstants.SELECT_POLICY);
            if (TigerConstants.POLICY_PSS_STANDARDMBOM.equalsIgnoreCase(strParentMBOMPolicy))
                PropertyUtil.setRPEValue(context, STANDARDROOTMBOM, "true", true);
            else
                PropertyUtil.setRPEValue(context, STANDARDROOTMBOM, "false", true);
        }

        // psInstList.sortStructure("attribute[PLMInstance.V_TreeOrder].value", "ascending", "real");
        // Bug #231 - DCP - END

        if (psInstList.size() == 0 && mbomParentRefPLMID != null && !"".equals(mbomParentRefPLMID)) {
            // This is a leaf node of the PS (and it is not the root) : create a Provide under the MBOM path, with implement link and effectivity
            String[] argsForImplement = new String[4];
            argsForImplement[0] = mbomCompleteParentPath;
            argsForImplement[1] = psCompletePath;

            List<String> mbomRefPIDScopedWithPSRefList = PSS_FRCMBOMModelerUtility_mxJPO.getScopingReferencesFromList_PLMID(context, plmSession, psRefID);

            newMBOMRefPID = getMBOMReferenceToReuse(context, plmSession, mbomRefPIDScopedWithPSRefList, plantPID);
            if (UIUtil.isNullOrEmpty(newMBOMRefPID))
                PropertyUtil.setRPEValue(context, MBOMDONOTREUSE, "false", true);

            String strRPEValueId = PropertyUtil.getRPEValue(context, MBOMDONOTREUSE, true);
            if (strRPEValueId.equalsIgnoreCase("true") && !mbomRefPIDScopedWithPSRefList.isEmpty() && UIUtil.isNotNullAndNotEmpty(mbomRefPIDScopedWithPSRefList.get(0))) {

                String newMBOMRefPLMID1 = mbomRefPIDScopedWithPSRefList.get(0);
                newMBOMRefPID = PLMID.buildFromString(newMBOMRefPLMID1).getPid();
                if (isStandardMBOM) {
                    StringList slPlantConnected = pss.mbom.MBOMUtil_mxJPO.getConsumerPlantOnStandardMBOM(context, newMBOMRefPID);
                    if (!slPlantConnected.contains(plantPID)) {
                        PropertyUtil.setRPEValue(context, PLANTFROMENOVIA, "true", false);
                        PSS_FRCMBOMModelerUtility_mxJPO.attachPlantToMBOMReference(context, plmSession, (String) newMBOMRefPID, plantPID);
                        String strquery = "query path type SemanticRelation containing " + newMBOMRefPID + " where owner.type=='MfgProductionPlanning' select owner.physicalid dump |";
                        String listPathIds = MqlUtil.mqlCommand(context, strquery, false, false);
                        String[] slPathIds = listPathIds.split("\n");
                        if (UIUtil.isNotNullAndNotEmpty(listPathIds)) {
                            String strMfgPlanningId = (slPathIds[slPathIds.length - 1]).split("\\|")[1];
                            PropertyUtil.setGlobalRPEValue(context, MODIFYPLANT, "false");
                            updateConsumerPlantOnMBOM(context, strMfgPlanningId);
                        }
                    }
                }

                workingInfo.get("newRefPIDList").add(newMBOMRefPID);
            } else {

                setImplementLinkProcess_new(context, plmSession, mbomParentRefPLMID, psCompletePath, workingInfo, workingInfo_instanceAttributes, workingInfo_indexInstancesForImplement,
                        workingInfo_AppDateToValuate, plantPID, argsForImplement);
            }
        } else {
            // This is an intermediate node of the PS (and it is not the root) : insert a new CreateAssembly under the MBOM reference, and process recursively for each child instance
            if (!psInstList.isEmpty())
                psInstList.sortStructure("attribute[PLMInstance.V_TreeOrder].value", "ascending", "real");

            HashMap<String, String> mbomRefAttributes = new HashMap<String, String>();
            mbomRefAttributes.put("PLMEntity.V_Name", (String) psRefAttributes.get("PLMEntity.V_Name"));
            mbomRefAttributes.put("PLMEntity.V_description", (String) psRefAttributes.get("PLMEntity.V_description"));

            // Create a new MBOM reference

            // Create a new MBOM reference
            // PSS : START
            if (UIUtil.isNullOrEmpty(parentType)) {
                parentType = "CreateAssembly";
            }
            List<String> mbomRefPIDScopedWithPSRefList = PSS_FRCMBOMModelerUtility_mxJPO.getScopingReferencesFromList_PLMID(context, plmSession, psRefID);

            // String strRPEValueId = PropertyUtil.getRPEValue(context, MBOMDONOTREUSE, true);
            // if (strRPEValueId.equalsIgnoreCase("true") && !mbomRefPIDScopedWithPSRefList.isEmpty() && UIUtil.isNotNullAndNotEmpty(mbomRefPIDScopedWithPSRefList.get(0))) {
            // String newMBOMRefPLMID1 = mbomRefPIDScopedWithPSRefList.get(0);
            // newMBOMRefPID = PLMID.buildFromString(newMBOMRefPLMID1).getPid();
            // } else {// Rutuja Ekatpure:TIGTK-10100:2/10/2017:Start
            if (isStandardMBOM)
                newMBOMRefPID = getReleasedStandardMBOMReferenceToReuse(context, mbomRefPIDScopedWithPSRefList, plantPID);
            else
                newMBOMRefPID = getMBOMReferenceToReuse(context, plmSession, mbomRefPIDScopedWithPSRefList, plantPID);
            // } // Rutuja Ekatpure:TIGTK-10100:2/10/2017:End

            String strSKIPSTANDARDMBOM = PropertyUtil.getRPEValue(context, SKIPSTANDARDMBOM, true);
            if (isStandardMBOM && "true".equalsIgnoreCase(strSKIPSTANDARDMBOM)) {
                PropertyUtil.setRPEValue(context, MBOMDONOTREUSE, "true", true);
                checkRPE = true;
            } else {

                if (UIUtil.isNullOrEmpty(newMBOMRefPID)) {
                    // Rutuja Ekatpure:TIGTK-10100:2/10/2017:Start
                    PropertyUtil.setRPEValue(context, MBOMDONOTREUSE, "false", true);
                    StringBuffer sb = new StringBuffer();
                    sb.append(psRefID);
                    sb.append("|");
                    sb.append(plantPID);
                    List<String> list_MBOMObjectsReuse = workingInfo.get("newScopeObjectList");

                    if (list_MBOMObjectsReuse != null && list_MBOMObjectsReuse.size() > 0) {
                        boolean checkFlag = false;
                        for (int j = 0; j < list_MBOMObjectsReuse.size(); j++) {
                            String strMBOMObject = list_MBOMObjectsReuse.get(j);
                            String strSplitArray[] = strMBOMObject.split("\\|");
                            if (strSplitArray.length > 0) {
                                String strExistingPSRefId = strSplitArray[0];
                                String strExistingPlantId = strSplitArray[1];

                                if (plantPID.equalsIgnoreCase(strExistingPlantId) && strExistingPSRefId.equalsIgnoreCase(psRefID)) {
                                    newMBOMRefPID = strSplitArray[2];
                                    checkFlag = true;
                                }
                            }

                        }
                        if (checkFlag == false) {
                            newMBOMRefPID = createMBOMReferenceWithPlant(context, plmSession, parentType, null, mbomRefAttributes, psRefID, plantPID);

                            sb.append("|");
                            sb.append(newMBOMRefPID);
                            workingInfo.get("newScopeObjectList").add(sb.toString());
                        }
                    } else {
                        newMBOMRefPID = createMBOMReferenceWithPlant(context, plmSession, parentType, null, mbomRefAttributes, psRefID, plantPID);
                        sb.append("|");
                        sb.append(newMBOMRefPID);
                        workingInfo.get("newScopeObjectList").add(sb.toString());
                    }

                    // Replicate all the attributes values on the new MBOM reference

                    DomainObject newMBOMRefObj = new DomainObject(newMBOMRefPID);

                    PSS_FRCMBOMModelerUtility_mxJPO.setFCSIndexOnMBOM(context, psRefID, newMBOMRefPID);
                    // Added for check Current transaction already Having Same Object or not

                    // FRC: Fixed Bug#497-MBOM "Description" attribute not filled automatically
                    mbomRefAttributes.put("PLMReference.V_ApplicabilityDate", (String) psRefAttributes.get("PLMReference.V_ApplicabilityDate"));
                    newMBOMRefObj.setAttributeValues(context, mbomRefAttributes);
                } else if (UIUtil.isNotNullAndNotEmpty(mbomParentRef)) {
                    PropertyUtil.setRPEValue(context, MBOMDONOTREUSE, "true", true);
                    checkRPE = true;
                }

                // PSS : END
                // lModelListOnStructure

                if (valueEnvAttachModel == null) {
                    List<String> lModels = FRCMBOMModelerUtility.getAttachedModelsOnReference(context, plmSession, psRefID);

                    if (null != lModels && 0 < lModels.size()) {
                        workingInfo_lModelListOnStructure.addAll(lModels);
                    }
                }

                workingInfo.get("newRefPIDList").add(newMBOMRefPID);
                newMBOMRefPLMID = FRCMBOMModelerUtility.getPLMIDFromPID(context, plmSession, newMBOMRefPID);
                if (mbomParentRefPLMID == null || "".equals(mbomParentRefPLMID)) { // This is the root node of the PS
                    // Set scope and attach model (later)
                    workingInfo.get("newScopesToCreate_MBOMRefPIDs").add(newMBOMRefPID);
                    workingInfo.get("newScopesToCreate_MBOMRefPLMIDs").add(newMBOMRefPLMID);
                    workingInfo.get("newScopesToCreate_PSRefPIDs").add(psRefID);
                } else {
                    // Get the attribute values of the instance
                    String[] psCompletePathList = psCompletePath.split("/");
                    DomainRelationship psInstObj = new DomainRelationship(psCompletePathList[psCompletePathList.length - 1]);
                    Map psInstAttributes = psInstObj.getAttributeMap(context, true);
                    Map mbomInstAttributes = new HashMap();
                    mbomInstAttributes.put("PLMInstance.PLM_ExternalID", psInstAttributes.get("PLMInstance.PLM_ExternalID"));
                    mbomInstAttributes.put("PLMInstance.V_description", psInstAttributes.get("PLMInstance.V_description"));
                    mbomInstAttributes.put("PLMInstance.V_Name", psInstAttributes.get("PLMInstance.V_Name"));
                    // Fixed Bug 231-Tree Ordering
                    mbomInstAttributes.put("PLMInstance.V_TreeOrder", psInstAttributes.get("PLMInstance.V_TreeOrder"));

                    // Create a new instance (later)
                    workingInfo.get("instanceToCreate_parentRefPLMID").add(mbomParentRefPLMID);
                    workingInfo.get("instanceToCreate_childRefPLMID").add(newMBOMRefPLMID);
                    workingInfo.get("newScopesToCreate_MBOMRefPIDs").add(newMBOMRefPID);
                    workingInfo.get("newScopesToCreate_MBOMRefPLMIDs").add(newMBOMRefPLMID);
                    workingInfo.get("newScopesToCreate_PSRefPIDs").add(psRefID);

                    workingInfo_instanceAttributes.add(mbomInstAttributes);

                    String trimmedPSPath = psCompletePath.substring(psCompletePath.lastIndexOf("/") + 1);

                    List slCheck = workingInfo.get("psPathList");
                    if (!slCheck.contains(trimmedPSPath))
                        workingInfo.get("psPathList").add(trimmedPSPath);
                }

                // Rutuja Ekatpure:TIGTK-10100:31/10/2017:start
                if (isStandardMBOM) {
                    StringList slPlantConnected = pss.mbom.MBOMUtil_mxJPO.getConsumerPlantOnStandardMBOM(context, newMBOMRefPID);
                    if (!slPlantConnected.contains(plantPID)) {
                        PropertyUtil.setRPEValue(context, PLANTFROMENOVIA, "true", false);
                        PSS_FRCMBOMModelerUtility_mxJPO.attachPlantToMBOMReference(context, plmSession, (String) newMBOMRefPID, plantPID);
                        String strquery = "query path type SemanticRelation containing " + newMBOMRefPID + " where owner.type=='MfgProductionPlanning' select owner.physicalid dump |";
                        String listPathIds = MqlUtil.mqlCommand(context, strquery, false, false);
                        String[] slPathIds = listPathIds.split("\n");
                        if (UIUtil.isNotNullAndNotEmpty(listPathIds)) {
                            String strMfgPlanningId = (slPathIds[slPathIds.length - 1]).split("\\|")[1];
                            PropertyUtil.setGlobalRPEValue(context, MODIFYPLANT, "false");
                            updateConsumerPlantOnMBOM(context, strMfgPlanningId);
                        }
                    }
                }
                // Rutuja Ekatpure:TIGTK-10100:31/10/2017:End

                // Bug #231 - DCP - START
                for (int i = 0; i < psInstList.size(); i++) {
                    Map<String, String> psInstInfo = (Map<String, String>) psInstList.get(i);
                    if (mbomParentRefPLMID == null || "".equals(mbomParentRefPLMID)) { // This is the root node of the PS
                        createMBOMFromEBOMLikePSRecursive_new(context, plmSession, newMBOMRefPLMID, null, psInstInfo.get("physicalid"), psRefID + "/" + psInstInfo.get("physicalid[connection]"),
                                workingInfo, workingInfo_lModelListOnStructure, workingInfo_instanceAttributes, workingInfo_indexInstancesForImplement, workingInfo_AppDateToValuate, plantPID, null);
                    } else {
                        createMBOMFromEBOMLikePSRecursive_new(context, plmSession, newMBOMRefPLMID, null, psInstInfo.get("physicalid"), psCompletePath + "/" + psInstInfo.get("physicalid[connection]"),
                                workingInfo, workingInfo_lModelListOnStructure, workingInfo_instanceAttributes, workingInfo_indexInstancesForImplement, workingInfo_AppDateToValuate, plantPID, null);
                    }
                }
                // Bug #231 - DCP - END
            }
        }
        return newMBOMRefPLMID;
    }

    public static String setImplementLinkProcess_new(Context context, PLMCoreModelerSession plmSession, String mbomParentRefPLMID, String psCompletePath, Map<String, List<String>> workingInfo,
            List<Map<String, String>> workingInfo_instanceAttributes, List<Integer> workingInfo_indexInstancesForImplement, Map<String, String> workingInfo_AppDateToValuate, String plantPID,
            String[] args) throws Exception {
        // Get the PID of the PS leaf reference

        // Return value :
        // 0 = refresh row of the leaf MBOM instance
        // 1 = re-expand the row of the leaf MBOM instance
        // 2 = re-expand the row of the parent of the MBOM instance
        try {
            String returnValue = "0";

            String mbomCompletePath = args[0];
            // String psCompletePath = args[1];
            String approvalStatus = args[3];

            String newMBOMRefPID = null;
            String trimmedPSPath = null;

            String[] psCompletePathList = psCompletePath.split("/");
            String psLeafInstancePID = psCompletePathList[psCompletePathList.length - 1];
            String psLeafRefPID = MqlUtil.mqlCommand(context, "print connection " + psLeafInstancePID + " select to.physicalid dump |", false, false);

            String mbomLeafInstancePID = null;
            String mbomLeafRefPID = null;

            if (UIUtil.isNotNullAndNotEmpty(mbomCompletePath)) {
                String[] mbomCompletePathList = mbomCompletePath.split("/");

                if (mbomCompletePathList.length > 1) {
                    mbomLeafInstancePID = mbomCompletePathList[mbomCompletePathList.length - 1];
                    mbomLeafRefPID = MqlUtil.mqlCommand(context, "print connection " + mbomLeafInstancePID + " select to.physicalid dump |", false, false);
                } else {
                    mbomLeafRefPID = mbomCompletePathList[mbomCompletePathList.length - 1];
                }
            }

            // For Drag and drop

            if (UIUtil.isNotNullAndNotEmpty(approvalStatus)) {
                if (mbomLeafRefPID != null && !"".equals(mbomLeafRefPID)) {
                    DomainObject domParentMBOM = DomainObject.newInstance(context, mbomLeafRefPID);
                    String strParentMBOMPolicy = domParentMBOM.getInfo(context, DomainConstants.SELECT_POLICY);
                    if (TigerConstants.POLICY_PSS_STANDARDMBOM.equalsIgnoreCase(strParentMBOMPolicy))
                        PropertyUtil.setRPEValue(context, STANDARDROOTMBOM, "true", true);
                    else
                        PropertyUtil.setRPEValue(context, STANDARDROOTMBOM, "false", true);
                }

                newMBOMRefPID = createMBOMFromDragAndDrop(context, plmSession, psLeafRefPID, mbomLeafRefPID, plantPID, approvalStatus, mbomCompletePath, psCompletePath, workingInfo,
                        workingInfo_instanceAttributes, workingInfo_indexInstancesForImplement, workingInfo_AppDateToValuate);

                if (UIUtil.isNullOrEmpty(newMBOMRefPID)) {
                    String strSkipStdMBOMRPEValue = PropertyUtil.getRPEValue(context, SKIPSTANDARDMBOM, true);
                    if ("true".equalsIgnoreCase(strSkipStdMBOMRPEValue))
                        return "0";
                }

                returnValue = "3";

            } else {

                String strDragnDrop = PropertyUtil.getRPEValue(context, FROMDRAGNDROP, true);
                if (UIUtil.isNotNullAndNotEmpty(strDragnDrop) && strDragnDrop.equalsIgnoreCase("true")) {

                    String mbomLeafRefType = MqlUtil.mqlCommand(context, "print bus " + mbomLeafRefPID + " select type dump |", false, false);

                    boolean isDirect = false;
                    boolean isIndirect = false;

                    for (String typeInList : baseTypesForMBOMLeafNodes) {
                        if (getDerivedTypes(context, typeInList).contains(mbomLeafRefType))
                            isDirect = true;
                    }
                    if (isDirect) {
                        // Get the synched ManufItem with the lead PS reference (new Provide or the existing leaf ManufItem)
                        String newMBOMRefPLMID = getSynchedScopeMBOMRefFromPSRef_new(context, plmSession, psLeafRefPID, mbomLeafRefPID, workingInfo, workingInfo_AppDateToValuate, plantPID);
                        newMBOMRefPID = PLMID.buildFromString(newMBOMRefPLMID).getPid();

                        if (!newMBOMRefPID.equals(mbomLeafRefPID)) { // The leaf MBOM reference is not the one synched with the leaf PS reference. Normally because it is a different revision
                            // Replace the MBOM leaf instance with the new one
                            String newMBOMLeafInstancePID = PSS_FRCMBOMModelerUtility_mxJPO.replaceMBOMInstance(context, plmSession, mbomLeafInstancePID, newMBOMRefPID);

                            mbomCompletePath = mbomCompletePath.replace(mbomLeafInstancePID, newMBOMLeafInstancePID);
                            mbomLeafInstancePID = newMBOMLeafInstancePID;

                            returnValue = "2";
                        }
                    } else {
                        for (String typeInList : baseTypesForMBOMAssemblyNodes) {
                            if (getDerivedTypes(context, typeInList).contains(mbomLeafRefType))
                                isIndirect = true;
                        }

                        if (isIndirect) {
                            // Get a new synched Provide with the leaf PS reference
                            if (mbomLeafRefPID != null && !"".equals(mbomLeafRefPID)) {
                                DomainObject domParentMBOM = DomainObject.newInstance(context, mbomLeafRefPID);
                                String strParentMBOMPolicy = domParentMBOM.getInfo(context, DomainConstants.SELECT_POLICY);
                                if (TigerConstants.POLICY_PSS_STANDARDMBOM.equalsIgnoreCase(strParentMBOMPolicy))
                                    PropertyUtil.setRPEValue(context, STANDARDROOTMBOM, "true", true);
                                else
                                    PropertyUtil.setRPEValue(context, STANDARDROOTMBOM, "false", true);
                            }

                            String newMBOMRefPLMID = getSynchedScopeMBOMRefFromPSRef_new(context, plmSession, psLeafRefPID, null, workingInfo, workingInfo_AppDateToValuate, plantPID);
                            newMBOMRefPID = PLMID.buildFromString(newMBOMRefPLMID).getPid();

                            // TIGTK-10100:Rutuja Ekatpure:Start

                            // if no Standard MBOM object to reuse then return 0;
                            if (UIUtil.isNullOrEmpty(newMBOMRefPID)) {
                                String isSkipStdMBOM = PropertyUtil.getRPEValue(context, SKIPSTANDARDMBOM, true);
                                if ("true".equalsIgnoreCase(isSkipStdMBOM))
                                    return "0";
                            }
                            // TIGTK-10100:Rutuja Ekatpure:End
                            DomainRelationship psInstObj = new DomainRelationship(psCompletePathList[psCompletePathList.length - 1]);
                            Map psInstAttributes = psInstObj.getAttributeMap(context, true);
                            Map mbomInstAttributes = new HashMap();
                            String psTreeOrder = (String) psInstAttributes.get("PLMInstance.V_TreeOrder");
                            String psVName = (String) psInstAttributes.get("PLMInstance.V_Name");
                            String psExternalID = (String) psInstAttributes.get("PLMInstance.PLM_ExternalID");
                            mbomInstAttributes.put("PLMInstance.V_TreeOrder", psTreeOrder);
                            mbomInstAttributes.put("PLMInstance.V_Name", psVName);
                            mbomInstAttributes.put("PLMInstance.PLM_ExternalID", psExternalID);

                            // Insert this Provide under it's parent
                            mbomLeafInstancePID = getInstanceToReuse(context, mbomLeafRefPID, newMBOMRefPID, psTreeOrder, psExternalID, psVName);

                            if (UIUtil.isNullOrEmpty(mbomLeafInstancePID)) {
                                mbomLeafInstancePID = createInstance(context, plmSession, mbomLeafRefPID, newMBOMRefPID);
                            }
                            // Replicate all the attributes values on the new instance
                            DomainRelationship newMBOMInstObj = new DomainRelationship(mbomLeafInstancePID);
                            newMBOMInstObj.setAttributeValues(context, mbomInstAttributes);
                            // Rutuja Ekatpure:TIGTK-10100:31/10/2017:start
                            DomainObject domMBOMObj = DomainObject.newInstance(context, newMBOMRefPID);
                            String strMBOMPolicy = domMBOMObj.getInfo(context, DomainConstants.SELECT_POLICY);
                            if (TigerConstants.POLICY_PSS_STANDARDMBOM.equals(strMBOMPolicy)) {
                                StringList slPlantConnected = pss.mbom.MBOMUtil_mxJPO.getConsumerPlantOnStandardMBOM(context, newMBOMRefPID);
                                if (!slPlantConnected.contains(plantPID)) {
                                    PropertyUtil.setRPEValue(context, PLANTFROMENOVIA, "true", false);
                                    PSS_FRCMBOMModelerUtility_mxJPO.attachPlantToMBOMReference(context, plmSession, (String) newMBOMRefPID, plantPID);
                                    String strquery = "query path type SemanticRelation containing " + newMBOMRefPID + " where owner.type=='MfgProductionPlanning' select owner.physicalid dump |";
                                    String listPathIds = MqlUtil.mqlCommand(context, strquery, false, false);
                                    String[] slPathIds = listPathIds.split("\n");
                                    if (UIUtil.isNotNullAndNotEmpty(listPathIds)) {
                                        String strMfgPlanningId = (slPathIds[slPathIds.length - 1]).split("\\|")[1];
                                        PropertyUtil.setGlobalRPEValue(context, MODIFYPLANT, "false");
                                        updateConsumerPlantOnMBOM(context, strMfgPlanningId);
                                    }
                                }
                            }
                            // Rutuja Ekatpure:TIGTK-10100:31/10/2017:End
                            mbomCompletePath += "/";
                            mbomCompletePath += mbomLeafInstancePID;

                            returnValue = "1";
                        }
                    }

                    if (isDirect || isIndirect) {
                        trimmedPSPath = trimPSPathToClosestScope(context, plmSession, mbomCompletePath, psCompletePath);

                        if (trimmedPSPath == null || "".equals(trimmedPSPath)) {
                            throw new Exception("No scope exists.");
                        }
                    }
                    // Put a new implement link
                    List<String> mbomLeafInstancePIDList = new ArrayList();
                    mbomLeafInstancePIDList.add(mbomLeafInstancePID);
                    List<String> trimmedPSPathList = new ArrayList();
                    trimmedPSPathList.add(trimmedPSPath);
                    PSS_FRCMBOMModelerUtility_mxJPO.setImplementLinkBulk(context, plmSession, mbomLeafInstancePIDList, trimmedPSPathList, true);

                } else {
                    // Get existing Provide (or create one if there is none) synched with the leaf PS reference (with a scope link)
                    String newMBOMRefPLMID = getSynchedScopeMBOMRefFromPSRef_new(context, plmSession, psLeafRefPID, mbomLeafRefPID, workingInfo, workingInfo_AppDateToValuate, plantPID);
                    newMBOMRefPID = PLMID.buildFromString(newMBOMRefPLMID).getPid();

                    // Get the PID of the MBOM leaf reference

                    // Instantiate this Provide under it's parent and replicate all the attributes values on the new instance
                    DomainRelationship psInstObj = new DomainRelationship(psCompletePathList[psCompletePathList.length - 1]);
                    Map psInstAttributes = psInstObj.getAttributeMap(context, true);
                    Map mbomInstAttributes = new HashMap();

                    // Create a new instance (later)
                    mbomInstAttributes.put("PLMInstance.V_TreeOrder", psInstAttributes.get("PLMInstance.V_TreeOrder"));
                    mbomInstAttributes.put("PLMInstance.V_description", psInstAttributes.get("PLMInstance.V_description"));
                    mbomInstAttributes.put("PLMInstance.PLM_ExternalID", psInstAttributes.get("PLMInstance.PLM_ExternalID"));
                    mbomInstAttributes.put("PLMInstance.V_Name", psInstAttributes.get("PLMInstance.V_Name"));
                    workingInfo.get("instanceToCreate_parentRefPLMID").add(mbomParentRefPLMID);
                    workingInfo.get("instanceToCreate_childRefPLMID").add(newMBOMRefPLMID);
                    workingInfo.get("newScopesToCreate_MBOMRefPIDs").add(newMBOMRefPID);
                    workingInfo.get("newScopesToCreate_MBOMRefPLMIDs").add(newMBOMRefPLMID);
                    workingInfo.get("newScopesToCreate_PSRefPIDs").add(psLeafRefPID);
                    workingInfo_instanceAttributes.add(mbomInstAttributes);

                    trimmedPSPath = psCompletePath.substring(psCompletePath.lastIndexOf("/") + 1);
                    if (UIUtil.isNullOrEmpty(trimmedPSPath)) {
                        throw new Exception("No scope exists.");
                    }

                    // workingInfo.get("mbomLeafInstancePIDList").add(mbomLeafInstancePID);
                    workingInfo_indexInstancesForImplement.add(workingInfo.get("instanceToCreate_parentRefPLMID").size() - 1);
                    List slCheck = workingInfo.get("psPathList");
                    if (!slCheck.contains(trimmedPSPath))
                        workingInfo.get("psPathList").add(trimmedPSPath);
                    returnValue = "1";
                }
            }
            if (UIUtil.isNotNullAndNotEmpty(newMBOMRefPID)) {
                PSS_FRCMBOMModelerUtility_mxJPO.setFCSIndexOnMBOM(context, psLeafRefPID, newMBOMRefPID);
            } else if (UIUtil.isNotNullAndNotEmpty(mbomLeafRefPID)) {
                PSS_FRCMBOMModelerUtility_mxJPO.setFCSIndexOnMBOM(context, psLeafRefPID, mbomLeafRefPID);
            }

            return returnValue;
        } catch (Exception exp) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in setImplementLinkProcess_new: ", exp);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw exp;
        } finally {
            PropertyUtil.setGlobalRPEValue(context, NOTGENERATEVARINTASSEMBLY, DomainConstants.EMPTY_STRING);
            PropertyUtil.setRPEValue(context, FROMDRAGNDROP, "false", true);
        }
    }

    public static String getSynchedScopeMBOMRefFromPSRef_new(Context context, PLMCoreModelerSession plmSession, String psRefPID, String mbomRefPLMID, Map<String, List<String>> workingInfo,
            Map<String, String> workingInfo_AppDateToValuate, String plantPID) throws Exception {
        String returnMBOMRefPLMID = null;
        String returnMBOMRefPID = null;
        HashMap<String, String> attributes = new HashMap<String, String>();

        // Check if PS reference has a scope
        // !!! FOR DEBUG : force the creation of new Provides !!!
        List<String> mbomRefPLMIDScopedWithPSRefList = FRCMBOMModelerUtility.getScopingReferencesFromList_PLMID(context, plmSession, psRefPID);

        // to check wheather MBOM is standardMBOM or not by using Standard reference attribute on VPLMReference
        boolean isStandardMBOM = false;
        String result = MqlUtil.mqlCommand(context, "print bus " + psRefPID + " select interface dump |", false, false);
        DomainObject psRefObj = new DomainObject(psRefPID);
        Map psRefAttributes = psRefObj.getAttributeMap(context, true);
        if (UIUtil.isNotNullAndNotEmpty(result) && result.contains("PSS_PublishedPart") && "Standard".equals(psRefAttributes.get("PSS_PublishedPart.PSS_StandardReference"))) {
            isStandardMBOM = true;
        }

        // If no scopes found, check if there are scopes that are pending for creation.
        if (mbomRefPLMIDScopedWithPSRefList.size() == 0) {
            int index = workingInfo.get("newScopesToCreate_PSRefPIDs").indexOf(psRefPID);
            if (index >= 0) {
                returnMBOMRefPID = workingInfo.get("newScopesToCreate_MBOMRefPIDs").get(index);
                String scopedMBOMRefPLMID = workingInfo.get("newScopesToCreate_MBOMRefPLMIDs").get(index);
                mbomRefPLMIDScopedWithPSRefList.add(scopedMBOMRefPLMID);
            }
        }

        if (mbomRefPLMIDScopedWithPSRefList.size() > 1) { // PS reference has multiple MBOM scopes
            // If all the elements of the list are within the same revision family, return the latest revision
            boolean isSameMajorIds = true;
            String lastMajorIdsStr = null;

            for (String refPLMID : mbomRefPLMIDScopedWithPSRefList) {
                String refPID = PLMID.buildFromString(refPLMID).getPid();
                String majorIdsStr = MqlUtil.mqlCommand(context, "print bus " + refPID + " select majorids dump |", false, false);

                if (lastMajorIdsStr != null) {
                    if (!lastMajorIdsStr.equals(majorIdsStr))
                        isSameMajorIds = false;
                }

                lastMajorIdsStr = majorIdsStr;
            }

            if (isSameMajorIds) {
                String[] lastMajorIds = lastMajorIdsStr.split("\\|");

                // Return this MBOM scope
                returnMBOMRefPID = lastMajorIds[lastMajorIds.length - 1];

                // Check if it is a provide
                String returnMBOMRefType = MqlUtil.mqlCommand(context, "print bus " + returnMBOMRefPID + " select type dump |", false, false);

                boolean isDirect = false;

                for (String typeInList : baseTypesForMBOMLeafNodes) {
                    if (getDerivedTypes(context, typeInList).contains(returnMBOMRefType))
                        isDirect = true;
                }

                if (!isDirect)
                    throw new Exception("The Part (" + psRefPID + ") is already scoped with a Manufacturing Item (" + returnMBOMRefPID + ") which is not a Provide or a Create Material.");

                // TIGTK-10100:Rutuja Ekatpure:Start
                // to reuse released Standard MBOM
                if (isStandardMBOM) {
                    returnMBOMRefPID = getReleasedStandardMBOMReferenceToReuse(context, mbomRefPLMIDScopedWithPSRefList, plantPID);
                } else {
                    returnMBOMRefPID = getMBOMReferenceToReuse(context, plmSession, mbomRefPLMIDScopedWithPSRefList, plantPID);
                }
                // TIGTK-10100:Rutuja Ekatpure:End

                // Need to confirm below code

                if (UIUtil.isNullOrEmpty(returnMBOMRefPID) && !isStandardMBOM) {
                    returnMBOMRefPID = getIntermediateMBOMObject(context, plmSession, "CreateMaterial", null, attributes, psRefPID, plantPID, workingInfo);
                }

                returnMBOMRefPLMID = FRCMBOMModelerUtility.getPLMIDFromPID(context, plmSession, returnMBOMRefPID);

            } else {

                // To be confirm below code
                // PSS: START
                // TIGTK-10100:Rutuja Ekatpure:Start
                // to reuse released Standard MBOM
                if (isStandardMBOM) {
                    returnMBOMRefPID = getReleasedStandardMBOMReferenceToReuse(context, mbomRefPLMIDScopedWithPSRefList, plantPID);
                } else {
                    returnMBOMRefPID = getMBOMReferenceToReuse(context, plmSession, mbomRefPLMIDScopedWithPSRefList, plantPID);
                }

                if (UIUtil.isNullOrEmpty(returnMBOMRefPID) && !isStandardMBOM) {
                    returnMBOMRefPID = getIntermediateMBOMObject(context, plmSession, "CreateMaterial", null, attributes, psRefPID, plantPID, workingInfo);
                }
                // TIGTK-10100:Rutuja Ekatpure:End
                returnMBOMRefPLMID = FRCMBOMModelerUtility.getPLMIDFromPID(context, plmSession, returnMBOMRefPID);

                // throw new Exception("You cannot provide this part from the EBOM : it has multiple scopes !");
            }
        } else if (mbomRefPLMIDScopedWithPSRefList.size() == 1) { // PS reference has already one MBOM scope
            // Return this MBOM scope
            returnMBOMRefPID = getMBOMReferenceToReuse(context, plmSession, mbomRefPLMIDScopedWithPSRefList, plantPID);

            if (returnMBOMRefPID == null) { // Else, it means that it is a Provide that was created and scope previously in the process, so nothing more to do.

                returnMBOMRefPID = getIntermediateMBOMObject(context, plmSession, "CreateMaterial", null, attributes, psRefPID, plantPID, workingInfo);

                // FRC: Fixed Bug#497-MBOM "Description" attribute not filled automatically
                String psRefDescription = MqlUtil.mqlCommand(context, "print bus " + psRefPID + " select attribute[PLMEntity.V_description].value dump |", false, false);
                MqlUtil.mqlCommand(context, "mod bus " + returnMBOMRefPID + " PLMEntity.V_description '" + psRefDescription + "'", false, false);
                String psRefApplicabilityDate = MqlUtil.mqlCommand(context, "print bus " + psRefPID + " select attribute[PLMReference.V_ApplicabilityDate].value dump |", false, false);
                MqlUtil.mqlCommand(context, "mod bus " + returnMBOMRefPID + " PLMReference.V_ApplicabilityDate '" + psRefApplicabilityDate + "'", false, false);

            }
            // Check if it is a provide
            String returnMBOMRefType = MqlUtil.mqlCommand(context, "print bus " + returnMBOMRefPID + " select type dump |", false, false);
            boolean isDirect = false;

            for (String typeInList : baseTypesForMBOMLeafNodes) {
                if (getDerivedTypes(context, typeInList).contains(returnMBOMRefType))
                    isDirect = true;
            }
            returnMBOMRefPLMID = FRCMBOMModelerUtility.getPLMIDFromPID(context, plmSession, returnMBOMRefPID);
            if (!isDirect)
                throw new Exception("The Part (" + psRefPID + ") is already scoped with a Manufacturing Item (" + returnMBOMRefPID + ") which is not a Provide or a Create Material.");
        } else { // PS reference does not already have an MBOM scope
            // Get the previous revision of the PS reference
            String previousRevPSRefPID = MqlUtil.mqlCommand(context, "print bus " + psRefPID + " select previous.physicalid dump |", false, false);

            if ("".equals(previousRevPSRefPID)) { // PS reference does not have a previous revision
                // String strRPEValueId = PropertyUtil.getRPEValue(context, MBOMDONOTREUSE, true);
                if (mbomRefPLMID == null) { // MBOM reference is null
                    // Create a new Provide and return it
                    returnMBOMRefPID = getMBOMReferenceToReuse(context, plmSession, mbomRefPLMIDScopedWithPSRefList, plantPID);
                    if (returnMBOMRefPID == null) {

                        returnMBOMRefPID = getIntermediateMBOMObject(context, plmSession, "CreateMaterial", null, attributes, psRefPID, plantPID, workingInfo);

                        String psRefInfoStr = MqlUtil.mqlCommand(context,
                                "print bus " + psRefPID + " select attribute[PLMEntity.V_Name].value attribute[PLMEntity.V_description].value attribute[PLMReference.V_ApplicabilityDate].value dump |",
                                false, false);
                        String[] psRefInfo = psRefInfoStr.split("\\|", -2);

                        String psRefTitle = psRefInfo[0];
                        attributes.put("PLMEntity.V_Name", psRefTitle);

                        MqlUtil.mqlCommand(context, "mod bus " + returnMBOMRefPID + " PLMEntity.V_Name '" + psRefTitle + "'", false, false);

                        String psRefDescription = psRefInfo[1];
                        attributes.put("PLMEntity.V_description", psRefDescription);
                        MqlUtil.mqlCommand(context, "mod bus " + returnMBOMRefPID + " PLMEntity.V_description '" + psRefDescription + "'", false, false);
                        String psRefAppDate = psRefInfo[2];
                        MqlUtil.mqlCommand(context, "mod bus " + returnMBOMRefPID + " PLMReference.V_ApplicabilityDate '" + psRefAppDate + "'", false, false);
                        if (!"".equals(psRefAppDate))
                            workingInfo_AppDateToValuate.put(returnMBOMRefPID, psRefAppDate);
                    }

                    returnMBOMRefPLMID = FRCMBOMModelerUtility.getPLMIDFromPID(context, plmSession, returnMBOMRefPID);

                    workingInfo.get("newRefPIDList").add(returnMBOMRefPID);

                    // Create a scope link between PS reference and MBOM reference
                    workingInfo.get("newScopesToCreate_MBOMRefPIDs").add(returnMBOMRefPID);
                    workingInfo.get("newScopesToCreate_MBOMRefPLMIDs").add(returnMBOMRefPLMID);
                    workingInfo.get("newScopesToCreate_PSRefPIDs").add(psRefPID);

                } else { // MBOM reference is not null
                    List<String> inputListForGetScope = new ArrayList<String>();
                    inputListForGetScope.add(PLMID.buildFromString(mbomRefPLMID).getPid());
                    String psRefScopePID = FRCMBOMModelerUtility.getScopedPSReferencePIDFromList(context, plmSession, inputListForGetScope).get(0);

                    if (psRefScopePID != null && !"".equals(psRefScopePID)) { // MBOM reference has already a PS scope : throw a new exception
                        throw new Exception("This MBOM node already has a scope, and it is not the EBOM part you are providing !");
                    } else {
                        // Return the MBOM reference
                        returnMBOMRefPLMID = mbomRefPLMID;

                        // Create a scope link between PS reference and MBOM reference (later)
                        workingInfo.get("newScopesToCreate_MBOMRefPIDs").add(returnMBOMRefPID);
                        workingInfo.get("newScopesToCreate_MBOMRefPLMIDs").add(returnMBOMRefPLMID);
                        workingInfo.get("newScopesToCreate_PSRefPIDs").add(psRefPID);

                    }
                }
            } else { // PS reference has a previous revision
                // Recursive call on previous revision (with MBOM reference in parameter)
                String mbomRefPLMIDSynchedToPreviousPSRevision = getSynchedScopeMBOMRefFromPSRef_new(context, plmSession, previousRevPSRefPID, mbomRefPLMID, workingInfo, workingInfo_AppDateToValuate,
                        plantPID);
                String mbomRefPIDSynchedToPreviousPSRevision = PLMID.buildFromString(mbomRefPLMIDSynchedToPreviousPSRevision).getPid();

                // New revision on the MBOM reference returned by the recursive call and return this new MBOM reference
                // First, check if the session needs to be flushed, because in this particular MBOM creation process, the flushSession() is done at the end.
                try {
                    DomainObject obj = DomainObject.newInstance(context, mbomRefPIDSynchedToPreviousPSRevision);
                } catch (Exception e) {
                    // This object has just been created, and has not yet been flushed down to the M1 transaction.
                    // Do the flush now, so that the new revision will be able to perform...
                    flushSession(plmSession);
                }

                returnMBOMRefPID = newRevisionMBOMReference(context, plmSession, mbomRefPIDSynchedToPreviousPSRevision);
                returnMBOMRefPLMID = FRCMBOMModelerUtility.getPLMIDFromPID(context, plmSession, returnMBOMRefPID);
                workingInfo.get("newRefPIDList").add(returnMBOMRefPID);

                // !! CAREFULL : remove the scope on the new revision of the MBOM reference (by default, the new revision duplicates the scope)
                // List<String> modifiedInstanceList = FRCMBOMModelerUtility.deleteScopeLinkAndDeleteChildImplementLinks(context, plmSession, returnMBOMRefPID, false);

                // Create a scope link between the PS reference and the new MBOM reference revision (later)
                workingInfo.get("newScopesToCreate_MBOMRefPIDs").add(returnMBOMRefPID);
                workingInfo.get("newScopesToCreate_MBOMRefPLMIDs").add(returnMBOMRefPLMID);
                workingInfo.get("newScopesToCreate_PSRefPIDs").add(psRefPID);

                // Map the attributes
                String psRefInfoStr = MqlUtil.mqlCommand(context,
                        "print bus " + psRefPID + " select attribute[PLMEntity.V_Name].value attribute[PLMEntity.V_description].value attribute[PLMReference.V_ApplicabilityDate].value dump |", false,
                        false);
                String[] psRefInfo = psRefInfoStr.split("\\|", -2);

                if ("".equals(psRefInfo[2]))
                    MqlUtil.mqlCommand(context, "mod bus " + returnMBOMRefPID + " PLMEntity.V_Name '" + psRefInfo[0] + "' PLMEntity.V_description '" + psRefInfo[1] + "'", false, false);
                else
                    MqlUtil.mqlCommand(context, "mod bus " + returnMBOMRefPID + " PLMEntity.V_Name '" + psRefInfo[0] + "' PLMEntity.V_description '" + psRefInfo[1]
                            + "' PLMReference.V_ApplicabilityDate '" + psRefInfo[2] + "'", false, false);
            }
        }
        return returnMBOMRefPLMID;
    }

    public static List<String> createInstanceBulk(Context context, PLMCoreModelerSession plmSession, List<String> list_parentRefPLMID, List<String> list_childRefPLMID,
            List<Map<String, String>> list_instanceAttributes, List<Integer> workingInfo_indexInstancesForImplement, List<String> allCreatedInstancesPIDList,
            Map<String, Map<String, String>> validateAttributeMap) throws Exception {
        List<String> returnList = new ArrayList<String>();
        try {
            int index = 0;
            String strTreeOrder = null;
            String strExternalId = null;
            String strVName = null;

            Map<String, List<Map<String, String>>> workingInfo1 = new HashMap<String, List<Map<String, String>>>();
            workingInfo1.put("instanceToCreate", new ArrayList<Map<String, String>>());

            /*
             * if (index < workingInfo_indexInstancesForImplement.size()) nextIndex = workingInfo_indexInstancesForImplement.get(index);
             */

            for (int i = 0; i < list_parentRefPLMID.size(); i++) {

                Hashtable att = new Hashtable();
                att.putAll(list_instanceAttributes.get(i));

                Map<String, String> mInstanceAttributes = list_instanceAttributes.get(i);

                strTreeOrder = mInstanceAttributes.get("PLMInstance.V_TreeOrder");
                strExternalId = mInstanceAttributes.get("PLMInstance.PLM_ExternalID");
                strVName = mInstanceAttributes.get("PLMInstance.V_Name");

                String strParentId = PLMID.buildFromString(list_parentRefPLMID.get(i)).getPid();
                String strChildId = PLMID.buildFromString(list_childRefPLMID.get(i)).getPid();

                StringBuffer sb = new StringBuffer();
                sb.append(strTreeOrder);
                sb.append("|");
                sb.append(strExternalId);
                sb.append("|");
                sb.append(strVName);
                sb.append("|");
                sb.append(strParentId);
                sb.append("|");
                sb.append(strChildId);

                String newMBOMInstPID = getInstanceToReuse(context, strParentId, strChildId, strTreeOrder, strExternalId, strVName);
                if (UIUtil.isNullOrEmpty(newMBOMInstPID)) {

                    Map<String, String> mInstanceMap = new HashMap<String, String>();
                    List<Map<String, String>> list_MBOMInstanceReuse = workingInfo1.get("instanceToCreate");

                    if (list_MBOMInstanceReuse.size() > 0) {
                        boolean checkFlag = false;
                        for (int k = 0; k < list_MBOMInstanceReuse.size(); k++) {
                            Map<String, String> mInstanceCheckMap = list_MBOMInstanceReuse.get(k);
                            String strId = mInstanceCheckMap.get(sb.toString());
                            if (UIUtil.isNotNullAndNotEmpty(strId)) {
                                newMBOMInstPID = strId;
                                checkFlag = true;
                                break;
                            }
                        }
                        if (checkFlag == false) {
                            newMBOMInstPID = FRCMBOMModelerUtility.createMBOMInstanceWithPLMID(context, plmSession, list_parentRefPLMID.get(i), list_childRefPLMID.get(i), att);
                            mInstanceMap.put(sb.toString(), newMBOMInstPID);
                            workingInfo1.get("instanceToCreate").add(mInstanceMap);
                            validateAttributeMap.put(newMBOMInstPID, mInstanceAttributes);

                        }
                    } else {
                        newMBOMInstPID = FRCMBOMModelerUtility.createMBOMInstanceWithPLMID(context, plmSession, list_parentRefPLMID.get(i), list_childRefPLMID.get(i), att);
                        mInstanceMap.put(sb.toString(), newMBOMInstPID);
                        workingInfo1.get("instanceToCreate").add(mInstanceMap);
                        validateAttributeMap.put(newMBOMInstPID, mInstanceAttributes);
                    }
                }

                if (!returnList.contains(newMBOMInstPID) && !allCreatedInstancesPIDList.contains(newMBOMInstPID)) {
                    returnList.add(newMBOMInstPID);
                    allCreatedInstancesPIDList.add(newMBOMInstPID);
                }
            }
        } catch (Exception e) {
            logger.error("Error in createInstanceBulk: ", e);
            throw e;

        }

        return returnList;
    }

    public static String getIntermediateMBOMObject(Context context, PLMCoreModelerSession plmSession, String type, String magnitudeType, Map<String, String> attributes, String psRefPID,
            String plantPID, Map<String, List<String>> workingInfo) throws Exception {
        String returnMBOMRefPID = DomainConstants.EMPTY_STRING;
        try {
            PropertyUtil.setRPEValue(context, MBOMDONOTREUSE, "false", true);
            List<String> list_MBOMObjectsReuse = workingInfo.get("newScopeObjectList");
            StringBuffer sb = new StringBuffer();
            sb.append(psRefPID);
            sb.append("|");
            sb.append(plantPID);
            if (list_MBOMObjectsReuse.size() > 0) {
                boolean checkFlag = false;
                for (int j = 0; j < list_MBOMObjectsReuse.size(); j++) {
                    String strMBOMObject = list_MBOMObjectsReuse.get(j);

                    String strSplitArray[] = strMBOMObject.split("\\|");
                    if (strSplitArray.length > 2) {
                        String strExistingPSRefId = strSplitArray[0];
                        String strExistingPlantId = strSplitArray[1];
                        if (plantPID.equalsIgnoreCase(strExistingPlantId) && strExistingPSRefId.equalsIgnoreCase(psRefPID)) {
                            returnMBOMRefPID = strSplitArray[2];
                            checkFlag = true;
                        }
                    }
                }
                if (checkFlag == false) {
                    returnMBOMRefPID = createMBOMReferenceWithPlant(context, plmSession, "CreateMaterial", null, attributes, psRefPID, plantPID);
                    sb.append("|");
                    sb.append(returnMBOMRefPID);
                    workingInfo.get("newScopeObjectList").add(sb.toString());
                    String strDragNDrop = PropertyUtil.getRPEValue(context, FROMDRAGNDROP, true);
                    if (strDragNDrop.equalsIgnoreCase("true"))
                        PSS_FRCMBOMModelerUtility_mxJPO.createScopeLinkAndReduceChildImplementLinks(context, plmSession, returnMBOMRefPID, psRefPID, false);
                }
            } else {
                returnMBOMRefPID = createMBOMReferenceWithPlant(context, plmSession, "CreateMaterial", null, attributes, psRefPID, plantPID);
                sb.append("|");
                sb.append(returnMBOMRefPID);
                workingInfo.get("newScopeObjectList").add(sb.toString());
                String strDragNDrop = PropertyUtil.getRPEValue(context, FROMDRAGNDROP, true);
                if (strDragNDrop.equalsIgnoreCase("true"))
                    PSS_FRCMBOMModelerUtility_mxJPO.createScopeLinkAndReduceChildImplementLinks(context, plmSession, returnMBOMRefPID, psRefPID, false);
            }
        } catch (Exception e) {
            logger.error("Error in createInstanceBulk: ", e);
            throw e;
        }
        return returnMBOMRefPID;
    }

    public static String replaceNewRevisionManufItem(Context context, String[] args) throws Exception { // Called from FRCReplaceNewRevisionPostProcess.jsp
        String returnValue = "";
        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            String instPID = args[0];
            String childRefPID = args[1];
            String changeObjectName = args[2];

            String newChildRefPID = newRevisionMBOMReference(context, plmSession, childRefPID);

            returnValue = newChildRefPID;

            attachObjectToChange(context, plmSession, changeObjectName, newChildRefPID);

            flushAndCloseSession(plmSession);
            // HE5 : Added fix for the error "Transaction not active" after calling the R&D API replaceMBOMInstance
            if (context.isTransactionActive())
                ContextUtil.commitTransaction(context);
            // ContextUtil.abortTransaction(context);
        } catch (Exception exp) {
            logger.error("Error in replaceNewRevisionManufItem: ", exp);
            flushAndCloseSession(plmSession);
            if (ContextUtil.isTransactionActive(context)) {
                ContextUtil.abortTransaction(context);
            }
            throw exp;
        }

        return returnValue;
    }

    public static void setInstAttributeValues(Context context, Map<String, Map<String, String>> mInstanceAttributes, List<String> allCreatedInstancesPIDList) throws FrameworkException {
        try {
            for (int i = 0; i < allCreatedInstancesPIDList.size(); i++) {
                String newMBOMInstPID = allCreatedInstancesPIDList.get(i);
                Map mInstanceInfo = mInstanceAttributes.get(newMBOMInstPID);
                if (mInstanceInfo != null) {
                    DomainRelationship newMBOMInstObj = DomainRelationship.newInstance(context, newMBOMInstPID);
                    newMBOMInstObj.setAttributeValues(context, mInstanceInfo);
                }
            }
        } catch (Exception exp) {
            logger.error("Error in setInstAttributeValues: ", exp);
            throw exp;
        }

    }

    /**
     * This is Check Trigger Method used to Check Fourecia Short Length Desc. While promoting Standared MBOM
     * @param context
     * @param args
     * @return
     * @throws Exception
     *             TIGTK-12976
     */

    public int checkForFaureciaShortLengthDes(Context context, String args[]) throws Exception {
        logger.debug("PSS_FRCMBOMProg : checkForFaureciaShortLengthDes : START");
        int iReturn = 0;
        try {
            String strObjectId = args[0];
            DomainObject domStdMBOMObj = DomainObject.newInstance(context, strObjectId);
            String strAttributeVal = domStdMBOMObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_FAURECIA_SHORT_LENGTH_DESC);

            if (UIUtil.isNullOrEmpty(strAttributeVal)) {
                String strMaterialMsg = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.ErrorMessage.AttributeValNotAvailable");
                MqlUtil.mqlCommand(context, "notice $1", strMaterialMsg);
                iReturn = 1;
            }
            logger.debug("PSS_FRCMBOMProg : checkForFaureciaShortLengthDes : START");
        } catch (Exception exp) {
            logger.error("Error in checkForFaureciaShortLengthDes: ", exp);
            throw exp;
        }
        return iReturn;
    }

    // TIGTK-12976:Start
    public StringList getExcludeOIDList(Context context, String[] args) throws Exception {
        StringList excludeList = new StringList();

        try {
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            String selectedchildID = (String) paramMap.get("targetMBOMid");

            String strcurrentObjId = MqlUtil.mqlCommand(context, "print bus " + selectedchildID + " select id dump", false, false);
            if (!excludeList.contains(strcurrentObjId))
                excludeList.add(strcurrentObjId);

            DomainObject domMfgItem = new DomainObject(selectedchildID);
            StringList objSelects = new StringList();
            objSelects.addElement(DomainConstants.SELECT_ID);
            objSelects.addElement(DomainConstants.SELECT_NAME);
            matrix.util.Pattern relPattern = new matrix.util.Pattern(PropertyUtil.getSchemaProperty(context, "relationship_DELFmiFunctionIdentifiedInstance"));

            StringBuffer whereExpression = new StringBuffer();
            whereExpression.append('(');
            whereExpression.append("(policy==");
            whereExpression.append(TigerConstants.POLICY_PSS_STANDARDMBOM);
            whereExpression.append(" && ");
            whereExpression.append("(current!='");
            whereExpression.append(TigerConstants.STATE_PSS_STANDARD_MBOM_RELEASE);
            whereExpression.append("'))");
            whereExpression.append(" || ");
            whereExpression.append("(policy==");
            whereExpression.append(TigerConstants.POLICY_PSS_MATERIALASSEMBLY);
            whereExpression.append(" && ");
            whereExpression.append("(current!='");
            whereExpression.append(TigerConstants.STATE_PSS_MATERIALASSEMBLY_INWORK);
            whereExpression.append("'))");
            whereExpression.append(" || ");
            whereExpression.append("(policy==");
            whereExpression.append(TigerConstants.POLICY_PSS_MBOM);
            whereExpression.append(" && ");
            whereExpression.append("(current=='");
            whereExpression.append(TigerConstants.STATE_MBOM_OBSOLETE);
            whereExpression.append("'))");
            whereExpression.append(")");

            Pattern typePattern = new Pattern(TigerConstants.TYPE_CREATEASSEMBLY);
            typePattern.addPattern(TigerConstants.TYPE_CREATEKIT);
            typePattern.addPattern(TigerConstants.TYPE_CREATEMATERIAL);

            MapList childObjects = DomainObject.findObjects(context, typePattern.getPattern(), TigerConstants.VAULT_VPLM, whereExpression.toString(), objSelects);

            if (childObjects.size() > 0) {

                for (int i = 0; i < childObjects.size(); i++) {
                    Map mapChild = (Map) childObjects.get(i);
                    String mbomObjId = (String) mapChild.get("id");
                    if (!excludeList.contains(mbomObjId))
                        excludeList.add(mbomObjId);

                }

            }

            MapList mlToConnected = domMfgItem.getRelatedObjects(context, relPattern.getPattern(), // relationshipPattern
                    DomainConstants.QUERY_WILDCARD, // typePattern
                    objSelects, // objectSelects
                    null, // relationshipSelects
                    true, // getTo
                    false, // getFrom
                    (short) 0, // recurseToLevel
                    null, // objectWhere,
                    null, // relationshipWhere
                    (int) 0); // limit

            if (mlToConnected.size() > 0) {

                for (int i = 0; i < mlToConnected.size(); i++) {
                    Map mapChild = (Map) mlToConnected.get(i);
                    String mbomObjId = (String) mapChild.get("id");
                    if (!excludeList.contains(mbomObjId))
                        excludeList.add(mbomObjId);

                }

            }

        } catch (Exception err) {
            logger.error("Error in getExcludeOIDList: ", err);
            throw err;
        }
        return excludeList;
    }

    // TIGTK-12976:End
    /**
     * TIGTK-13507 This is Copy method from OOTB for Creation of MBOM from PS structure.
     */

    public String mbomCreationMethodSelect(Context context, String[] args) throws Exception { // Called from form FRCCreateMBOM (field FRCMBOMManufItemCreateAndPropertiesForm)
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        Map requestMap = (Map) programMap.get("requestMap");
        String strLanguage = (String) requestMap.get("languageStr");
        String sPartId = (String) requestMap.get("objectId");
        StringBuffer sb = new StringBuffer();
        Locale lLang = new Locale(strLanguage);
        String sSelectMBOM = EnoviaResourceBundle.getProperty(context, "FRCMBOMCentral", "FRCMBOMCentral.CreateMBOM.SelectMBOM", lLang);

        sb.append("<input type=\"radio\" name=\"selmbomtype\" value=\"CopyMBOM\" ></input>");
        sb.append("<input type=\"text\" readonly=\"readonly\" name=\"txtMBOM\" value=\"");
        sb.append(XSSUtil.encodeForXML(context, sSelectMBOM));
        sb.append("\"></input>");
        sb.append("<input type=\"button\" name=\"btnMBOM\" value=\"...\" onclick= \"showCreateAssemblyChooser('" + sPartId + "')\"></input>");
        sb.append("<input type=\"hidden\" name=\"rootMbom\" value=\"\" ></input>");
        sb.append("<input type=\"hidden\" name=\"rootMbomOID\" value=\"\" ></input>");
        sb.append("<br></br>");

        sb.append("<input type=\"radio\" name=\"selmbomtype\" value=\"FromEBOM\"  ></input>");
        String sFromEBOM = EnoviaResourceBundle.getProperty(context, "FRCMBOMCentral", "FRCMBOMCentral.CreateMBOM.FromEBOM", lLang);
        sb.append(sFromEBOM);
        sb.append("<br></br>");

        sb.append("<input type=\"radio\" name=\"selmbomtype\" value=\"NewAssembly\" checked=\"checked\"> </input>");
        String sNewAssembly = EnoviaResourceBundle.getProperty(context, "FRCMBOMCentral", "FRCMBOMCentral.CreateMBOM.NewAssembly", lLang);
        sb.append(sNewAssembly);
        sb.append("<br></br>");

        // Add validation method that will be launch on form submit button
        sb.append("<script language=\"javascript\">");
        sb.append("assignValidateMethod('rootMbomOID', 'validateCreationMethod');");

        // Initialize Title with V6 product Title
        MapList mlPrd = getProductFromEBOM(context, sPartId);
        String sTitle = "";
        if (null != mlPrd && 0 < mlPrd.size()) {
            Map mObj = (Map) mlPrd.get(0);
            sTitle = (String) mObj.get("attribute[PLMEntity.V_Name]");
        }
        if (!"".equals(sTitle)) {
            sb.append("document.forms[0].Title.value='");
            sb.append(sTitle);
            sb.append("'");
        }
        sb.append("</script>");

        return sb.toString();
    }

    /**
     * This method get the Scoped MBOM from PS Structure.
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public StringList getScopedMBOMFromPS(Context context, String[] args) throws Exception {
        StringList includeList = new StringList();
        PLMCoreModelerSession plmSession = null;
        boolean isTransactionActive = false;
        try {

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            ContextUtil.startTransaction(context, true);
            isTransactionActive = true;

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strparentPartId = (String) programMap.get("objectId");
            MapList mlPrd = getProductFromEBOM(context, strparentPartId);
            Map mPrd = (Map) mlPrd.get(0);
            String sPrdPhysId = (String) mPrd.get("physicalid");
            if (UIUtil.isNotNullAndNotEmpty(sPrdPhysId)) {
                List<String> scopedMBOMRefIDs = PSS_FRCMBOMModelerUtility_mxJPO.getScopingReferencesFromList(context, plmSession, sPrdPhysId);
                if (!scopedMBOMRefIDs.isEmpty())
                    for (String refPID : scopedMBOMRefIDs) {
                        DomainObject domObj = DomainObject.newInstance(context, refPID);
                        String strId = domObj.getInfo(context, DomainConstants.SELECT_ID);
                        includeList.add(strId);
                    }
            }
            flushAndCloseSession(plmSession);
            if (isTransactionActive)
                ContextUtil.commitTransaction(context);
        } catch (Exception err) {
            logger.error("Error in getScopedMBOMFromPS: ", err);
            flushAndCloseSession(plmSession);
            if (isTransactionActive) {
                ContextUtil.abortTransaction(context);
            }
            throw err;
        }

        return includeList;
    }

    // TIGTK-14370 START
    /**
     * This method is used to set attribute value of PSS_FCSMaterialType
     * @param context
     * @param args
     * @throws Exception
     */
    public void updateFCSMaterialType(Context context, String args[]) throws Exception {
        System.out.println("in the updateFCSMaterialType.....................");
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap requestMap = (HashMap) programMap.get("requestMap");
            String objectId = (String) requestMap.get("objectId");
            DomainObject domObj = DomainObject.newInstance(context, objectId);
            String strFCSMaterialType = (String) requestMap.get("calc_PSS_FCSMaterialType");
            if (UIUtil.isNotNullAndNotEmpty(strFCSMaterialType) && strFCSMaterialType.equalsIgnoreCase("Val1")) {
                domObj.setAttributeValue(context, "PSS_ManufacturingItemExt.PSS_FCSMaterialType", "NOVA");
            } else if (UIUtil.isNotNullAndNotEmpty(strFCSMaterialType) && strFCSMaterialType.equalsIgnoreCase("Val2")) {
                domObj.setAttributeValue(context, "PSS_ManufacturingItemExt.PSS_FCSMaterialType", "PACK");
            }
        } catch (Exception e) {
            throw e;
        }

    }

    // TIGTK-14370 END
    // TIGTK - 14360 START
    public String getPhysicalProductfromEBOM(Context context, String args[]) throws Exception {
        System.out.println("in the getPhysicalProductfromEBOM.....................");
        String sPrdPhysId = DomainConstants.EMPTY_STRING;
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strPartId = (String) programMap.get("objectId");
            MapList mlPrd = getProductFromEBOM(context, strPartId);
            if (null != mlPrd && !mlPrd.isEmpty()) {
                if (1 < mlPrd.size())
                    throw new Exception("Several VPM Products have been found for the given EBOM part");
                if (mlPrd.size() == 0)
                    throw new Exception(EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.ErrorMessage.NoVPMProduct"));
                Map mPrd = (Map) mlPrd.get(0);
                sPrdPhysId = (String) mPrd.get("physicalid");
            }

        } catch (Exception e) {
            throw e;
        }
        return sPrdPhysId;
    }
    // TIGTK - 14360 END
}