import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Pattern;

import com.dassault_systemes.iPLMDictionaryPublicItf.IPLMDictionaryPublicClassItf;
import com.dassault_systemes.iPLMDictionaryPublicItf.IPLMDictionaryPublicFactory;
import com.dassault_systemes.iPLMDictionaryPublicItf.IPLMDictionaryPublicItf;
import com.dassault_systemes.vplm.fctProcessAdvAuth.interfaces.IVPLMFctProcessAdvAuth;
import com.dassault_systemes.vplm.fctProcessAuthoring.interfaces.IVPLMFctProcessImplementLinkAuthoring;
import com.dassault_systemes.vplm.fctProcessNav.utility.MBOMILImpactDiagnosis;
import com.dassault_systemes.vplm.fctProcessNav.utility.MBOMSLOutputInfo;
import com.dassault_systemes.vplm.interfaces.access.IPLMxCoreAccess;
import com.dassault_systemes.vplm.modeler.PLMCoreModelerSession;
import com.dassault_systemes.vplm.modeler.entity.PLMxEntityPath;
import com.dassault_systemes.vplm.modeler.entity.PLMxReferenceEntity;
import com.matrixone.apps.configuration.ConfigurationConstants;
import com.matrixone.apps.configuration.ConfigurationUtil;
import com.matrixone.apps.configuration.ProductConfiguration;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.mxType;
import com.matrixone.apps.effectivity.EffectivityFramework;
import com.matrixone.apps.framework.ui.UIUtil;
import com.matrixone.plmql.cmd.PLMID;
import com.mbom.modeler.utility.FRCEffExpr;
import com.mbom.modeler.utility.FRCMBOMModelerUtility;
import com.mbom.modeler.utility.FRCSettings;

import matrix.db.Context;
import matrix.util.LicenseUtil;
import matrix.util.StringList;
import pss.constants.TigerConstants;

/**
 * This method is used to return allowed 5 type list
 * @param context
 * @param args
 * @throws Exception
 */
@SuppressWarnings({ "deprecation", "rawtypes", "unchecked" })
public class PSS_FRCMBOMModelerUtility_mxJPO extends FRCMBOMModelerUtility {

    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PSS_FRCMBOMModelerUtility_mxJPO.class);

    public static final String INTERFACE_PSS_MANUFACTURING_ITEM_EXT = PropertyUtil.getSchemaProperty("interface_PSS_ManufacturingItemExt");

    public static final String INTERFACE_PSS_MANUFACTURING_PART_EXT = PropertyUtil.getSchemaProperty("interface_PSS_ManufacturingPartExt");

    public static final String INTERFACE_PSS_MANUFACTURING_UOM_EXT = PropertyUtil.getSchemaProperty("interface_PSS_ManufacturingUoMExt");

    public static final String INTERFACE_PSS_MANUFACTURING_INSTANCE_EXT = PropertyUtil.getSchemaProperty("interface_PSS_ManufacturingInstanceExt");

    public static final String ATTRIBUTE_PSS_MANUFACTURING_ITEM_EXT_PSS_FCS_INDEX = PropertyUtil.getSchemaProperty("attribute_PSS_ManufacturingItemExt.PSS_FCSIndex");

    public static final String PLANTFROMENOVIA = "PSS_PLANT_CALLING_FROM_ENOVIA";

    public static final String MODIFYPLANT = "MODIFYPLANT";

    public static List<String> getAuthorizedChildMBOMReferenceTypes(Context context, PLMCoreModelerSession plmSession, String typeName) throws Exception {
        final String TYPE_OPERATION = PropertyUtil.getSchemaProperty(context, "type_PSS_Operation");
        final String TYPE_LINE_DATA = PropertyUtil.getSchemaProperty(context, "type_PSS_LineData");

        String[] validTypes = { "CreateAssembly", "CreateKit", "CreateMaterial", "ProcessContinuousProvide", "ProcessContinuousCreateMaterial" };
        List<String> typesToBeRetained = Arrays.asList(validTypes);

        IVPLMFctProcessAdvAuth fProcessAdvAuthModeler = (IVPLMFctProcessAdvAuth) plmSession.getModeler("com.dassault_systemes.vplm.fctProcessAdvAuth.implementation.VPLMFctProcessAdvAuth");
        List returnList = fProcessAdvAuthModeler.getAggregationRules(typeName);

        List finalList = new ArrayList();
        if (typeName.equalsIgnoreCase(TYPE_OPERATION) || typeName.equalsIgnoreCase(TYPE_LINE_DATA)) {
            finalList.add("");
        }

        else if (returnList != null) {
            for (String type : typesToBeRetained) {
                if (returnList.contains(type)) {
                    finalList.add(type);
                }
            }
        }
        return finalList;
    }

    /**
     * @PolicyChange
     * @param context
     * @param plmSession
     * @param typeName
     * @param policy
     * @param attributes
     * @return
     * @throws Exception
     */
    public static String createMBOMDiscreteReference(Context context, PLMCoreModelerSession plmSession, String typeName, String policy, Map<String, String> attributes) throws Exception {
        // REFACTORING : to replace
        // return createNewManufItemReference2(context, typeName, policy, null);
        Properties p = plmSession.getContext().getProperties();
        p.put("PPR_Autoname_Faurecia", "true");
        plmSession.getContext().setProperties(p);

        IVPLMFctProcessAdvAuth fProcessAdvAuthModeler = (IVPLMFctProcessAdvAuth) plmSession.getModeler("com.dassault_systemes.vplm.fctProcessAdvAuth.implementation.VPLMFctProcessAdvAuth");

        IPLMDictionaryPublicFactory ipldmFactory = new IPLMDictionaryPublicFactory();
        IPLMDictionaryPublicItf dico = ipldmFactory.getDictionary();
        IPLMDictionaryPublicClassItf iFuncType = dico.getClass(context, typeName);

        Hashtable attributesHT = new Hashtable();
        for (Entry<String, String> attName : attributes.entrySet()) {
            String attBareName = attName.getKey();

            int index = attBareName.indexOf(".");
            if (index >= 0) {
                attBareName = attBareName.substring(index + 1);
            }
            attributesHT.put(attBareName, attName.getValue());
        }
        PLMxReferenceEntity newRef = fProcessAdvAuthModeler.createFctProcessReferenceFromType(iFuncType, attributesHT);

        if (newRef == null) {
            throw new Exception("Create MBOM discrete reference failed : returned null.");
        }
        String refPLMIDStr = newRef.getPLMIdentifier();
        PLMID refPLMID = PLMID.buildFromString(refPLMIDStr);
        return refPLMID.getPid();
    }

    /**
     * @PolicyChange
     * @param context
     * @param plmSession
     * @param typeName
     * @param policy
     * @param magnitudeType
     * @param attributes
     * @return
     * @throws Exception
     */
    public static String createMBOMContinuousReference(Context context, PLMCoreModelerSession plmSession, String typeName, String policy, String magnitudeType, Map<String, String> attributes)
            throws Exception {
        // REFACTORING : to replace
        // return createNewManufItemReference2(context, typeName, policy, magnitudeType);
        Properties p = plmSession.getContext().getProperties();
        p.put("PPR_Autoname_Faurecia", "true");
        plmSession.getContext().setProperties(p);

        IVPLMFctProcessAdvAuth fProcessAdvAuthModeler = (IVPLMFctProcessAdvAuth) plmSession.getModeler("com.dassault_systemes.vplm.fctProcessAdvAuth.implementation.VPLMFctProcessAdvAuth");

        IPLMDictionaryPublicFactory ipldmFactory = new IPLMDictionaryPublicFactory();
        IPLMDictionaryPublicItf dico = ipldmFactory.getDictionary();
        IPLMDictionaryPublicClassItf iFuncType = dico.getClass(context, typeName);

        Hashtable attributesHT = new Hashtable();
        for (Entry<String, String> attName : attributes.entrySet()) {
            String attBareName = attName.getKey();

            int index = attBareName.indexOf(".");
            if (index >= 0) {
                attBareName = attBareName.substring(index + 1);
            }
            attributesHT.put(attBareName, attName.getValue());
        }

        IVPLMFctProcessAdvAuth.continuousMagnitude magType = IVPLMFctProcessAdvAuth.continuousMagnitude.Length;
        if ("Area".equalsIgnoreCase(magnitudeType))
            magType = IVPLMFctProcessAdvAuth.continuousMagnitude.Area;
        else if ("Mass".equalsIgnoreCase(magnitudeType))
            magType = IVPLMFctProcessAdvAuth.continuousMagnitude.Mass;
        else if ("Volume".equalsIgnoreCase(magnitudeType))
            magType = IVPLMFctProcessAdvAuth.continuousMagnitude.Volume;
        else if ("Length".equalsIgnoreCase(magnitudeType)) {
            magType = IVPLMFctProcessAdvAuth.continuousMagnitude.Length;
        }
        PLMxReferenceEntity newRef = fProcessAdvAuthModeler.createFctProcessContinuousReferenceFromType(iFuncType, magType, 1.0D, attributesHT);

        if (newRef == null) {
            throw new Exception("Create MBOM discrete reference failed : returned null.");
        }
        String refPLMIDStr = newRef.getPLMIdentifier();
        PLMID refPLMID = PLMID.buildFromString(refPLMIDStr);

        return refPLMID.getPid();
    }

    /**
     * @PolicyChange
     * @param context
     * @param plmSession
     * @param refPolicy
     * @param mBOMRefPIDList
     * @return
     * @throws Exception
     */
    public static List<String> partialDuplicateMBOMStructure(Context context, PLMCoreModelerSession plmSession, String refPolicy, List<String> mBOMRefPIDList) throws Exception {
        TreeMap<String, String> alreadyProcessedPIDs = new TreeMap();
        createMBOMFromEBOMFromTemplateRecursive2(context, refPolicy, (String) mBOMRefPIDList.get(0), alreadyProcessedPIDs);

        List<String> returnList = new ArrayList();
        for (String templateRefPID : mBOMRefPIDList) {
            String newRefPID = (String) alreadyProcessedPIDs.get(templateRefPID);
            returnList.add(newRefPID);
        }
        return returnList;
    }

    /**
     * @UniqueMBOMPerPlant
     * @PolicyChange
     * @param context
     * @param plmSession
     * @param refPolicy
     * @param mBOMRefPIDList
     * @return
     * @throws Exception
     */
    public static List<String> partialDuplicateMBOMStructure(Context context, PLMCoreModelerSession plmSession, String refPolicy, List<String> mBOMRefPIDList, StringList psRefPIDList, String plantId)
            throws Exception {
        TreeMap<String, String> alreadyProcessedPIDs = new TreeMap();
        createMBOMFromEBOMFromTemplateRecursive2(context, plmSession, refPolicy, (String) mBOMRefPIDList.get(0), alreadyProcessedPIDs, psRefPIDList, plantId);

        List<String> returnList = new ArrayList();
        for (String templateRefPID : mBOMRefPIDList) {
            String newRefPID = (String) alreadyProcessedPIDs.get(templateRefPID);
            returnList.add(newRefPID);
        }
        return returnList;
    }

    /**
     * @PolicyChange
     * @param context
     * @param refPolicy
     * @param templateRefID
     * @param alreadyProcessedPIDs
     * @return
     * @throws Exception
     */
    public static String createMBOMFromEBOMFromTemplateRecursive2(Context context, String refPolicy, String templateRefID, TreeMap<String, String> alreadyProcessedPIDs) throws Exception {
        String newRefPID = null;

        // String refPolicy = "VPLM_SMB_Definition";

        String refType = MqlUtil.mqlCommand(context, "print bus " + templateRefID + " select type dump |", false, false);

        String[] refInterfaceList = null;
        String refInterfaceListStr = MqlUtil.mqlCommand(context, "print bus " + templateRefID + " select interface dump |", false, false);
        if ("".equals(refInterfaceListStr)) {
            refInterfaceList = new String[0];
        } else {
            refInterfaceList = refInterfaceListStr.replace("DELAsmUnitRefRequired", "").replace("DELAsmLotRefRequired", "").split("\\|");
        }

        DomainObject refObj = new DomainObject(templateRefID);
        Map refAttributes = refObj.getAttributeMap(context, true);

        newRefPID = (String) alreadyProcessedPIDs.get(templateRefID);
        if (newRefPID == null) {
            newRefPID = createManufItem(context, refType, refPolicy, refInterfaceList);
            alreadyProcessedPIDs.put(templateRefID, newRefPID);
        }

        DomainObject newRefObj = new DomainObject(newRefPID);
        newRefObj.setAttributeValues(context, refAttributes);

        StringList busSelect = new StringList();
        busSelect.add("physicalid");
        StringList relSelect = new StringList();
        relSelect.add("physicalid[connection]");

        List<Map> instList = getVPMStructureMQL(context, templateRefID, busSelect, relSelect, (short) 1, null);

        for (Map instInfo : instList) {
            DomainRelationship instObj = new DomainRelationship((String) instInfo.get("physicalid[connection]"));
            Map instAttributes = instObj.getAttributeMap(context, true);

            String newChildRefPID = createMBOMFromEBOMFromTemplateRecursive2(context, refPolicy, (String) instInfo.get("physicalid"), alreadyProcessedPIDs);

            String newInstPID = createManufItemInstance(context, newRefPID, newChildRefPID);

            DomainRelationship newInstObj = new DomainRelationship(newInstPID);
            newInstObj.setAttributeValues(context, instAttributes);
        }

        return newRefPID;
    }

    /**
     * @UniqueMBOMPerPlant
     * @PolicyChange
     * @param context
     * @param refPolicy
     * @param templateRefID
     * @param alreadyProcessedPIDs
     * @return
     * @throws Exception
     */
    public static String createMBOMFromEBOMFromTemplateRecursive2(Context context, PLMCoreModelerSession plmSession, String refPolicy, String templateRefID,
            TreeMap<String, String> alreadyProcessedPIDs, StringList psRefPIDList, String plantId) throws Exception {
        String newRefPID = null;
        HashMap<String, String> attributes = new HashMap<String, String>();
        StringList slList = new StringList();
        slList.addElement(TigerConstants.TYPE_PROCESS_CONTINUOUS_CREATE_MATERIAL);
        slList.addElement(TigerConstants.TYPE_PROCESSCONTINUOUSPROVIDE);
        slList.addElement(TigerConstants.TYPE_PSS_PAINTLACK);
        slList.addElement(TigerConstants.TYPE_PSS_PAINTSYSTEM);
        slList.addElement(TigerConstants.TYPE_PSS_MATERIALMIXTURE);
        slList.addElement(TigerConstants.TYPE_PSS_MATERIAL);
        slList.addElement(TigerConstants.TYPE_PSS_COLORMASTERBATCH);

        // String refPolicy = "VPLM_SMB_Definition";

        // PSS ALM4253 fix START
        PropertyUtil.setRPEValue(context, PLANTFROMENOVIA, "true", false);
        // PSS ALM4253 fix END

        String refType = MqlUtil.mqlCommand(context, "print bus " + templateRefID + " select type dump |", false, false);

        /*
         * String[] refInterfaceList = null; String refInterfaceListStr = MqlUtil.mqlCommand(context, "print bus " + templateRefID + " select interface dump |", false, false); if
         * ("".equals(refInterfaceListStr)) { refInterfaceList = new String[0]; } else { refInterfaceList = refInterfaceListStr.replace("DELAsmUnitRefRequired", "").replace("DELAsmLotRefRequired",
         * "").split("\\|"); }
         */
        DomainObject refObj = new DomainObject(templateRefID);
        Map refAttributes = refObj.getAttributeMap(context, true);

        newRefPID = (String) alreadyProcessedPIDs.get(templateRefID);
        if (newRefPID == null) {
            ArrayList<String> pidList = new ArrayList<String>();
            pidList.add(templateRefID);
            List<String> psRefPIDs = getScopedPSReferencePIDFromList(context, plmSession, pidList);
            boolean createNew = true;
            for (String psRefPID : psRefPIDs) {
                if (psRefPIDList.contains(psRefPID)) {
                    String strAttachedPlant = pss.mbom.MBOMUtil_mxJPO.getMasterPlant(context, templateRefID);
                    if (UIUtil.isNotNullAndNotEmpty(strAttachedPlant)) {
                        if (strAttachedPlant.equalsIgnoreCase(plantId)) {
                            newRefPID = templateRefID;
                            createNew = false;
                            break;
                        }
                    }
                }
            }
            if (createNew) {
                // Sneha :Start
                // newRefPID = createManufItem(context, refType, refPolicy, refInterfaceList);

                if (!slList.contains(refType)) {
                    newRefPID = PSS_FRCMBOMProg_mxJPO.createMBOMReference(context, plmSession, refType, null, attributes);
                    PSS_FRCMBOMModelerUtility_mxJPO.attachPlantToMBOMReference(context, plmSession, newRefPID, plantId);
                    PSS_FRCMBOMProg_mxJPO.copyMBOMItemsToMBOMReference(context, plmSession, newRefPID, templateRefID);
                    DomainObject dMBOMObj = DomainObject.newInstance(context, newRefPID);
                    if (UIUtil.isNotNullAndNotEmpty(psRefPIDs.get(0)) && (dMBOMObj.isKindOf(context, TigerConstants.TYPE_CREATEASSEMBLY)
                            || dMBOMObj.isKindOf(context, TigerConstants.TYPE_CREATEMATERIAL) || dMBOMObj.isKindOf(context, TigerConstants.TYPE_CREATEKIT))) {
                        PSS_FRCMBOMModelerUtility_mxJPO.createScopeLinkAndReduceChildImplementLinks(context, plmSession, newRefPID, psRefPIDs.get(0), false);
                    }
                } else {
                    newRefPID = templateRefID;
                }
                // Sneha :End
            }
            alreadyProcessedPIDs.put(templateRefID, newRefPID);
        }
        if (!slList.contains(refType)) {
            DomainObject newRefObj = new DomainObject(newRefPID);
            newRefObj.setAttributeValues(context, refAttributes);
            // TIGTK-8913:Rutuja Ekatpure:10/7/2017:Start
            newRefObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PLM_EXTERNALID, newRefObj.getInfo(context, DomainConstants.SELECT_NAME));
        }
        StringList busSelect = new StringList();
        busSelect.add("physicalid");
        StringList relSelect = new StringList();
        relSelect.add("physicalid[connection]");

        List<Map> instList = getVPMStructureMQL(context, templateRefID, busSelect, relSelect, (short) 1, null);

        for (Map instInfo : instList) {
            String strOldInstId = (String) instInfo.get("physicalid[connection]");
            DomainRelationship instObj = new DomainRelationship(strOldInstId);
            Map instAttributes = instObj.getAttributeMap(context, true);

            String newChildRefPID = createMBOMFromEBOMFromTemplateRecursive2(context, plmSession, refPolicy, (String) instInfo.get("physicalid"), alreadyProcessedPIDs, psRefPIDList, plantId);
            String psTreeOrder = (String) instAttributes.get("PLMInstance.V_TreeOrder");
            String psVName = (String) instAttributes.get("PLMInstance.V_Name");
            String psExternalID = (String) instAttributes.get("PLMInstance.PLM_ExternalID");

            String newInstPID = PSS_FRCMBOMProg_mxJPO.getInstanceToReuse(context, newRefPID, newChildRefPID, psTreeOrder, psExternalID, psVName);
            if (UIUtil.isNullOrEmpty(newInstPID))
                newInstPID = PSS_FRCMBOMProg_mxJPO.createInstance(context, plmSession, newRefPID, newChildRefPID);
            // TIGTK-8913:Rutuja Ekatpure:10/7/2017:End
            DomainRelationship newInstObj = new DomainRelationship(newInstPID);
            newInstObj.setAttributeValues(context, instAttributes);

            List<String> implementLink = FRCMBOMModelerUtility.getImplementLinkInfoSimple(context, plmSession, strOldInstId);
            if (!implementLink.isEmpty()) {
                if (implementLink.size() > 0) {
                    StringBuffer implementPIDPathSB = new StringBuffer();
                    for (int i = 0; i < implementLink.size(); i++) {
                        if (implementPIDPathSB.length() > 0)
                            implementPIDPathSB.append("/");

                        implementPIDPathSB.append(implementLink.get(i));
                    }

                    List<String> mbomLeafInstancePIDList = new ArrayList();
                    List<String> trimmedPSPathList = new ArrayList();
                    mbomLeafInstancePIDList.add(newInstPID);
                    trimmedPSPathList.add(implementPIDPathSB.toString());
                    setImplementLinkBulk(context, plmSession, mbomLeafInstancePIDList, trimmedPSPathList, true);

                }

            }
        }

        /*
         * String args_temp[] = new String[2]; args_temp[0] = strOldInstId; args_temp[1] = newInstPID; PSS_FRCMBOMProg_mxJPO.setEffectivity(context, plmSession, args_temp, newRefPID);
         */
        return newRefPID;
    }

    public static void flushSession(PLMCoreModelerSession plmSession) {
        try {
            plmSession.flushSession();
        } catch (Exception e) {
        }
    }

    /**
     * @PolicyChange
     * @param context
     * @param type
     * @param policy
     * @param magnitudeType
     * @return
     * @throws Exception
     */
    public static String createNewManufItemReference2(Context context, String type, String policy, String magnitudeType) throws Exception {
        // String policy = "VPLM_SMB_Definition";

        String interfaceName = "";
        if ("Length".equals(magnitudeType)) {
            interfaceName = "DELFmiContQuantity_Length";
        } else if ("Mass".equals(magnitudeType)) {
            interfaceName = "DELFmiContQuantity_Mass";
        } else if ("Area".equals(magnitudeType)) {
            interfaceName = "DELFmiContQuantity_Area";
        } else if ("Volume".equals(magnitudeType)) {
            interfaceName = "DELFmiContQuantity_Volume";
        }
        String newObjID = createManufItem(context, type, policy, new String[] { interfaceName });

        return newObjID;
    }

    /**
     * @WorkAround
     * @param context
     * @param plmSession
     * @param rootPID
     * @param busSelect
     * @param relSelect
     * @param expLvl
     * @param pcGlobalFilterCompExpr
     * @param pcGlobalFilterXMLValue
     * @return
     * @throws Exception
     */
    public static MapList getVPMStructureMQL(Context context, String rootPID, StringList busSelect, StringList relSelect, short expLvl, String pcGlobalCompExpr, String whereClauseRel)
            throws Exception {
        long startTime = System.currentTimeMillis();

        String pcGlobalFilterId = null;
        if ((pcGlobalCompExpr != null) && (!("".equals(pcGlobalCompExpr))) && (!("null".equals(pcGlobalCompExpr)))) {
            pcGlobalFilterId = MqlUtil.mqlCommand(context, false, "exec prog ConfigFiltering_InitFilter " + pcGlobalCompExpr, false);
        }

        if (UIUtil.isNullOrEmpty(whereClauseRel)) {
            whereClauseRel = "";
        }

        if ((pcGlobalFilterId != null) && (!("".equals(pcGlobalFilterId)))) {
            whereClauseRel = " AND (escape  ( IF (!(attribute[PLMInstance.V_hasConfigEffectivity] == TRUE)) THEN 1 ELSE (execute[ConfigFiltering_ApplyFilter " + pcGlobalFilterId + "  attribute\\["
                    + "PLMInstance.V_EffectivityCompiledForm" + "\\] ]) ))";
        }

        DomainObject objectDOM = new DomainObject(rootPID);

        MapList res = objectDOM.getRelatedObjects(context, "PLMCoreInstance", "PLMCoreReference", busSelect, relSelect, false, true, expLvl, "", whereClauseRel, 0);

        if ((pcGlobalFilterId != null) && (!("".equals(pcGlobalFilterId)))) {
            MqlUtil.mqlCommand(context, false, "exec prog ConfigFiltering_ReleaseFilter " + pcGlobalFilterId, false);
        }

        long endTime = System.currentTimeMillis();
        logger.info("FRC PERFOS : getVPMStructure :  ", (endTime - startTime));
        return res;
    }

    /**
     * @UniqueMBOMPerPlant
     * @AddExtension
     * @param context
     * @param parentRefPID
     * @param childRefPID
     * @return
     * @throws Exception
     */
    public static String createManufItemInstance(Context context, String parentRefPID, String childRefPID) throws Exception {
        String childRefType = MqlUtil.mqlCommand(context, "print bus " + childRefPID + " select type dump |", false, false);

        String continuousRefTypesListStr = MqlUtil.mqlCommand(context, "print type DELFmiFunctionPPRContinuousReference select derivative dump |", false, false);
        String[] continuousRefTypesListArray = continuousRefTypesListStr.split("\\|");
        List continuousRefTypesList = Arrays.asList(continuousRefTypesListArray);

        String instanceType = "DELFmiFunctionIdentifiedInstance";
        if (continuousRefTypesList.contains(childRefType)) {
            instanceType = "ProcessInstanceContinuous";
        }

        String childRefTitle = MqlUtil.mqlCommand(context, "print bus " + childRefPID + " select attribute[PLMEntity.V_Name].value dump |", false, false);

        String refTitleListStr = MqlUtil.mqlCommand(context, "print bus " + parentRefPID + " select from[PLMInstance].to.attribute[PLMEntity.V_Name].value dump |", false, false);
        int newOccNbr = refTitleListStr.split(Pattern.quote(childRefTitle), -1).length;
        String instanceTitle = childRefTitle + "." + newOccNbr;

        String newTreeOrder = "1.0";

        String instanceTreeOrderListStr = MqlUtil.mqlCommand(context, "print bus " + parentRefPID + " select from[PLMInstance].attribute[PLMInstance.V_TreeOrder].value dump |", false, false);
        if (!("".equals(instanceTreeOrderListStr))) {
            String[] instanceTreeOrderList = instanceTreeOrderListStr.split("\\|");

            double treeOrderMax = 4.9E-324D;
            for (String instTreeOrderStr : instanceTreeOrderList)
                try {
                    treeOrderMax = Math.max(treeOrderMax, Double.parseDouble(instTreeOrderStr));
                } catch (Exception e) {
                }
            newTreeOrder = Double.toString(treeOrderMax + 1000.0D);
        }

        String instancePID = MqlUtil.mqlCommand(context, "add connection '" + instanceType + "' from '" + parentRefPID + "' to '" + childRefPID + "' select physicalid dump |", false, false);

        String instanceDiscipline = instanceType;

        MqlUtil.mqlCommand(context, "mod connection " + instancePID + " PLMInstance.C_updatestamp -1 PLMInstance.V_sec_level 0 PLMInstance.V_discipline '" + instanceDiscipline
                + "' PLMInstance.PLM_ExternalID '" + instanceTitle + "'" + " PLMInstance.V_TreeOrder '" + newTreeOrder + "'", false, false);

        return instancePID;
    }

    /**
     * @UniqueMBOMPerPlant
     * @AddExtension
     * @param context
     * @param type
     * @param policy
     * @param listInterfacesToAdd
     * @return
     * @throws Exception
     */
    public static String createManufItem(Context context, String type, String policy, String[] listInterfacesToAdd) throws Exception {

        // PSS ALM2107 fix START
        PropertyUtil.setRPEValue(context, "PSS_IS_CALLING_FROM_ENOVIA", "true", false);
        // PSS ALM2107 fix END

        String newRev = MqlUtil.mqlCommand(context, "print policy " + policy + " select majorsequence[0] minorsequence[0] dump '.'", false, false);

        String newName = getAutoNameForVPM(context, type);

        String newPID = MqlUtil.mqlCommand(context, "add bus '" + type + "' '" + newName + "' '" + newRev + "' policy '" + policy + "' vault vplm select physicalid dump |", false, false);

        String discipline = "";
        if ("ElementaryEndItem".equals(type))
            discipline = "DELAsmElementaryEndItem";
        else {
            discipline = type;
        }
        MqlUtil.mqlCommand(context, "mod bus " + newPID + " PLMEntity.V_sec_level 0 PLMEntity.C_updatestamp -1 PLMEntity.PLM_ExternalID '" + newName + "' PLMEntity.V_discipline '" + discipline
                + "' PLMReference.V_order 1 PLMReference.V_VersionID " + newPID, false, false);

        StringBuffer interfaceCommand = new StringBuffer();

        interfaceCommand.append(" add interface DELAsmUnitRefRequired add interface DELAsmLotRefRequired");

        for (String interfaceName : listInterfacesToAdd) {
            if (!(interfaceName.startsWith("PSS_"))) {
                // Added : 07/11/2016
                // if ((interfaceName != null) && (!("".equals(interfaceName)))) {
                if ((UIUtil.isNotNullAndNotEmpty(interfaceName))) {
                    // Added : 07/11/2016
                    interfaceCommand.append(" add interface '");
                    interfaceCommand.append(interfaceName);
                    interfaceCommand.append("'");
                }
            }
        }

        MqlUtil.mqlCommand(context, "mod bus " + newPID + interfaceCommand, false, false);
        // PSS : Customization : START
        // Add Interface PSS_ManufacturingItemExt on newly created object of
        // type CretaeMaterial
        final String TYPE_CREATE_ASSEMBLY = PropertyUtil.getSchemaProperty(context, "type_CreateAssembly");
        final String TYPE_CREATE_KIT = PropertyUtil.getSchemaProperty(context, "type_CreateKit");
        final String TYPE_CREATE_MATERIAL = PropertyUtil.getSchemaProperty(context, "type_CreateMaterial");

        DomainObject domObj = new DomainObject(newPID);
        String strType = domObj.getInfo(context, DomainConstants.SELECT_TYPE);
        /*
         * String policy = domObj.getInfo(context, DomainConstants.SELECT_POLICY); if (UIUtil.isNotNullAndNotEmpty(policy) && !policy.equalsIgnoreCase(POLICY_PSS_MBOM)) { domObj.setPolicy(context,
         * POLICY_PSS_MBOM); }
         */

        // String strCommand = "modify bus " + newPID + " add interface " + INTERFACE_PSS_MANUFACTURING_UOM_EXT;
        // MqlUtil.mqlCommand(context, strCommand, false, false);
        // String strCommand = "";
        // if (strType.equals(TYPE_CREATE_MATERIAL)) {
        // strCommand = "modify bus " + newPID + " add interface " + INTERFACE_PSS_MANUFACTURING_PART_EXT;
        // MqlUtil.mqlCommand(context, strCommand, false, false);
        // }
        if (strType.equals(TYPE_CREATE_ASSEMBLY) || strType.equals(TYPE_CREATE_KIT) || strType.equals(TYPE_CREATE_MATERIAL)) {
            // strCommand = "modify bus " + newPID + " add interface " + INTERFACE_PSS_MANUFACTURING_ITEM_EXT;
            // MqlUtil.mqlCommand(context, strCommand, false, false);
            String symbolicTypeName = FrameworkUtil.getAliasForAdmin(context, DomainConstants.SELECT_TYPE, strType, true);
            String strAutoName = DomainObject.getAutoGeneratedName(context, symbolicTypeName, "-");
            domObj.setName(context, strAutoName);
            domObj.setAttributeValue(context, "PLMEntity.PLM_ExternalID", strAutoName);
            // MqlUtil.mqlCommand(context, "mod bus "+newObjPID+" name "+ strAutoName, false, false);
        }
        // PSS : Customization : END

        return newPID;
    }

    /**
     * @FCSIndex
     * @param context
     * @param plmSession
     * @param mbomRefPID
     * @param psRefPID
     * @param reduceChildImplementLinks
     * @return
     * @throws Exception
     */
    public static List<String> createScopeLinkAndReduceChildImplementLinks(Context context, PLMCoreModelerSession plmSession, String mbomRefPID, String psRefPID, boolean reduceChildImplementLinks)
            throws Exception {
        String[] listValidLicence = LicenseUtil.validateProducts(context, new String[] { "PPV" });
        if (!(listValidLicence[0].equals("true"))) {
            throw new Exception("Need PPV License to use this function");
        }

        IPLMxCoreAccess coreAccess = plmSession.getVPLMAccess();

        String[] plmIds = coreAccess.convertM1IDinPLMID(new String[] { mbomRefPID, psRefPID });

        IVPLMFctProcessImplementLinkAuthoring fProcessImplementLinkAuthoringModeler = (IVPLMFctProcessImplementLinkAuthoring) plmSession
                .getModeler("com.dassault_systemes.vplm.fctProcessAuthoring.implementation.VPLMFctProcessImplementLinkAuthoring");

        MBOMSLOutputInfo oMBOMSLOutputInfo = fProcessImplementLinkAuthoringModeler.createScopeLink(plmIds[0], plmIds[1], reduceChildImplementLinks, false);

        if (oMBOMSLOutputInfo == null) {
            throw new Exception("Create Scope Link failed : returned null.");
        }
        List oMBOMILImpactDiagnosisInfo = oMBOMSLOutputInfo.getImpactDiagnosisList();

        List modifiedInstancePIDList = new ArrayList();

        for (Object obj : oMBOMILImpactDiagnosisInfo) {
            MBOMILImpactDiagnosis mobj = (MBOMILImpactDiagnosis) obj;
            modifiedInstancePIDList.add(mobj.getImplementingInstancePLMID());
        }

        // PSS: START
        // TIGTK-7259: TS :17/7/2017:START
        pss.mbom.MBOMUtil_mxJPO.createPublishControlObject(context, psRefPID, mbomRefPID);
        // TIGTK-7259: TS :17/7/2017:END
        setFCSIndexOnMBOM(context, psRefPID, mbomRefPID);
        // PSS_FRCMBOMProg_mxJPO.disconnectVaraintAssemblyFromMBOMAndCreateNew(context, mbomRefPID, psRefPID);
        // PSS: END
        return modifiedInstancePIDList;
    }

    /**
     * @FCSIndex
     * @param revIndex
     * @return
     * @throws Exception
     */
    public static void setFCSIndexOnMBOM(Context context, String psRefPID, String mbomRefPID) throws Exception {
        DomainObject obj = DomainObject.newInstance(context, psRefPID);
        String majorRevisionIndex = obj.getInfo(context, "majororder");
        obj = DomainObject.newInstance(context, mbomRefPID);
        obj.setAttributeValue(context, ATTRIBUTE_PSS_MANUFACTURING_ITEM_EXT_PSS_FCS_INDEX, formatRevisionIndex(majorRevisionIndex));
    }

    /**
     * @FCSIndex
     * @param revIndex
     * @return
     * @throws Exception
     */
    public static String formatRevisionIndex(String revIndex) throws Exception {
        int revInt = 1;
        if (UIUtil.isNotNullAndNotEmpty(revIndex)) {
            revInt = Integer.parseInt(revIndex) + 1;
        }
        DecimalFormat decimalFormat = new DecimalFormat("00");
        String strRevInd = decimalFormat.format(revInt);
        return strRevInd;
        // return new DecimalFormat("00").format(revInt);
    }

    /**
     * @WorkAround
     * @param context
     * @param plmSession
     * @param rootPID
     * @param busSelect
     * @param relSelect
     * @param expLvl
     * @param pcGlobalFilterCompExpr
     * @param pcGlobalFilterXMLValue
     * @return
     * @throws Exception
     */
    public static MapList getVPMStructureMQL(Context context, String rootPID, StringList busSelect, StringList relSelect, short expLvl, String pcId) throws Exception {
        long startTime = System.currentTimeMillis();

        String pcGlobalCompExpr = null;
        if (UIUtil.isNotNullAndNotEmpty(pcId)) {
            DomainObject dObj = DomainObject.newInstance(context, pcId);
            pcGlobalCompExpr = dObj.getAttributeValue(context, "Filter Compiled Form");
        }
        String pcGlobalFilterId = null;
        if ((pcGlobalCompExpr != null) && (!("".equals(pcGlobalCompExpr))) && (!("null".equals(pcGlobalCompExpr)))) {
            pcGlobalFilterId = MqlUtil.mqlCommand(context, false, "exec prog ConfigFiltering_InitFilter " + pcGlobalCompExpr, false);
        }
        String whereClauseRel = "";
        if ((pcGlobalFilterId != null) && (!("".equals(pcGlobalFilterId)))) {
            whereClauseRel = "escape  ( IF (!(attribute[PLMInstance.V_hasConfigEffectivity] == TRUE)) THEN 1 ELSE (execute[ConfigFiltering_ApplyFilter " + pcGlobalFilterId + "  attribute\\["
                    + "PLMInstance.V_EffectivityCompiledForm" + "\\] ]) )";
        }

        DomainObject objectDOM = new DomainObject(rootPID);

        MapList res = objectDOM.getRelatedObjects(context, "PLMCoreInstance", "PLMCoreReference", busSelect, relSelect, false, true, expLvl, "", whereClauseRel, 0);

        if ((pcGlobalFilterId != null) && (!("".equals(pcGlobalFilterId)))) {
            MqlUtil.mqlCommand(context, false, "exec prog ConfigFiltering_ReleaseFilter " + pcGlobalFilterId, false);
        }

        long endTime = System.currentTimeMillis();
        logger.info("FRC PERFOS : getVPMStructure : ", (endTime - startTime));
        return res;
    }

    public static String setImplementLinkBulk(Context context, PLMCoreModelerSession plmSession, List<String> mbomInstancePIDList, List<String> psPathWithoutRootList, boolean manageEff)
            throws Exception {
        try {
            IPLMxCoreAccess coreAccess = plmSession.getVPLMAccess();
            String[] mbomInstancePLMIDList = coreAccess.convertM1IDinPLMID((String[]) mbomInstancePIDList.toArray(new String[0]));
            ArrayList psPathObjWithoutRootList = new ArrayList();

            for (String psPathWithoutRoot : psPathWithoutRootList) {
                PLMxEntityPath path = new PLMxEntityPath(coreAccess.convertM1IDinPLMID(psPathWithoutRoot.split("/")));
                psPathObjWithoutRootList.add(path);
            }
            IVPLMFctProcessImplementLinkAuthoring fProcessAuthoringModeler1 = (IVPLMFctProcessImplementLinkAuthoring) plmSession
                    .getModeler("com.dassault_systemes.vplm.fctProcessAuthoring.implementation.VPLMFctProcessImplementLinkAuthoring");
            List errorInfoList = fProcessAuthoringModeler1.createImplementLinks(Arrays.asList(mbomInstancePLMIDList), psPathObjWithoutRootList, manageEff, manageEff);

            String strMBOMPhysicalIdResult = DomainConstants.EMPTY_STRING;
            String strPSPhysicaldResult = DomainConstants.EMPTY_STRING;

            if (!mbomInstancePIDList.isEmpty()) {
                int intArraySize = mbomInstancePIDList.size();
                for (int i = 0; i < intArraySize; i++) {
                    String mbomInstancePLMID = (String) mbomInstancePIDList.get(i);
                    if (mbomInstancePLMID.contains("/")) {
                        String[] mbomPathPIDList = mbomInstancePLMID.split("/");
                        int mbomPathIndex = mbomPathPIDList.length - 1;
                        while (mbomPathIndex > 0) {
                            mbomInstancePLMID = mbomPathPIDList[mbomPathIndex];
                        }
                    }
                    String strMBOMPhysicalIdAndFlagAttribute = "print connection " + mbomInstancePLMID + " select to.physicalid dump |";
                    strMBOMPhysicalIdResult = MqlUtil.mqlCommand(context, strMBOMPhysicalIdAndFlagAttribute, true, false);
                }
            }

            if (!psPathWithoutRootList.isEmpty()) {
                int intArraySize = psPathWithoutRootList.size();
                for (int i = 0; i < intArraySize; i++) {
                    String psInstancePLMID = (String) psPathWithoutRootList.get(i);

                    if (psInstancePLMID.contains("/")) {
                        String[] psPathPIDList = psInstancePLMID.split("/");
                        int psPathIndex = psPathPIDList.length - 1;
                        while (psPathIndex > 0) {
                            psInstancePLMID = psPathPIDList[psPathIndex];
                        }
                    }
                    String strPSPhysicalId = "print connection " + psInstancePLMID + " select to.physicalid dump |";
                    strPSPhysicaldResult = MqlUtil.mqlCommand(context, strPSPhysicalId, true, false);
                }
            }

            // RFC-139 : Update the Master Plant Name on Scope Link
            String strRPEValueId = PropertyUtil.getGlobalRPEValue(context, MODIFYPLANT);
            if (UIUtil.isNullOrEmpty(strRPEValueId) && !strRPEValueId.equalsIgnoreCase("FALSE")) {
                String strMasterMfgProductionPlanning = pss.mbom.MBOMUtil_mxJPO.getMasterMfgProductionPlanning(context, strMBOMPhysicalIdResult);
                if (UIUtil.isNotNullAndNotEmpty(strMasterMfgProductionPlanning)) {
                    DomainObject dMfgProductionPlanningObj = DomainObject.newInstance(context, strMasterMfgProductionPlanning);
                    String strPlantName = dMfgProductionPlanningObj.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_VOWNER + "].from.name");
                    DomainObject dMBOMObj = DomainObject.newInstance(context, strMBOMPhysicalIdResult);
                    dMBOMObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGITEMEXTPSS_MATERPLANTNAME, strPlantName);
                }
            }
            flushSession(plmSession);
            String[] args = new String[] { strPSPhysicaldResult, strMBOMPhysicalIdResult };
            PSS_FRCMBOMProg_mxJPO.checkUpdatesAvailableOnPS(context, args);

            return getErrorMessageInfo(errorInfoList);
        } catch (Exception e) {
            logger.error("Error in setImplementLinkBulk: ", e);
            throw e;
        } finally {
            PropertyUtil.setGlobalRPEValue(context, "MODIFYPLANT", "");
        }
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

    /*
     * TIGTK-3583 /**
     * 
     * @param context
     * 
     * @param pcObjectId
     * 
     * @return
     * 
     * @throws Exception
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Map getExpressionsForPC(Context context, String pcObjectId) throws Exception {

        PLMCoreModelerSession plmSession = null;
        try {

            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            MapList expressionList = new MapList();
            EffectivityFramework eff = new EffectivityFramework();

            MapList mapPCSelectedOptions = ProductConfiguration.getSelectedOptions(context, pcObjectId, false, true);
            MapList selectedOptionsMap = new MapList();
            // If PC is created for a part then Model Id is not required
            if (!mapPCSelectedOptions.isEmpty()) {
                for (int i = 0; i < mapPCSelectedOptions.size(); i++) {
                    Map temp = (Map) mapPCSelectedOptions.get(i);
                    if (!mxType.isOfParentType(context, temp.get("from.type").toString(), ConfigurationConstants.TYPE_PRODUCTS)) {
                        selectedOptionsMap.add(temp);
                    }
                }
            }
            Object objModelID = ProductConfiguration.getModelPhysicalIdBasedOnProductConfiguration(context, pcObjectId);
            StringList slModelID = ConfigurationUtil.convertObjToStringList(context, objModelID);
            StringBuffer strActualExpression = new StringBuffer();
            StringBuffer strDisplayExpression = new StringBuffer();

            MapList dataList = selectedOptionsMap;

            for (int i = 0; i < slModelID.size(); i++) {
                String contextId = (String) slModelID.get(i);

                StringList strRelSelect = new StringList();
                strRelSelect.add("physicalid");

                String contextPhyId = (String) (DomainObject.newInstance(context, contextId)).getInfo(context, "physicalid");
                String strRelIds[] = new String[dataList.size()];
                for (int ii = 0; ii < dataList.size(); ii++) {
                    Map mapFOs = (Map) dataList.get(ii);
                    strRelIds[ii] = (String) mapFOs.get(DomainRelationship.SELECT_ID);
                }

                StringList strRelSelects = new StringList();
                strRelSelects.addElement("physicalid");
                strRelSelects.addElement("from.physicalid");
                strRelSelects.addElement("to.physicalid");

                MapList mapPhysicalIds = DomainRelationship.getInfo(context, strRelIds, strRelSelects);
                boolean addOperator = false;
                for (int ii = 0; ii < mapPhysicalIds.size(); ii++) {
                    Map mapFOs = (Map) mapPhysicalIds.get(ii);
                    com.matrixone.json.JSONObject effObj = new com.matrixone.json.JSONObject();
                    effObj.put("contextId", contextPhyId); // physicalid of the
                                                           // Model context
                    effObj.put("parentId", (String) mapFOs.get("from.physicalid")); // physicalid of the CF
                    effObj.put("objId", (String) mapFOs.get("to.physicalid")); // physicalid of the CO
                    effObj.put("relId", (String) mapFOs.get("physicalid")); // physicalid of the CO rel
                    effObj.put("insertAsRange", false);

                    String jsonString = effObj.toString();
                    Map formatedExpr = eff.formatExpression(context, "FeatureOption", jsonString);

                    String actualFormatedExpr = (String) formatedExpr.get(EffectivityFramework.ACTUAL_VALUE);
                    if (actualFormatedExpr != null && !actualFormatedExpr.isEmpty()) {
                        if ((i + 1) != dataList.size() && addOperator) {
                            strActualExpression.append(" " + "AND" + " ");
                            strDisplayExpression.append(" " + "AND" + " ");
                            addOperator = false;
                        }
                        strActualExpression.append(actualFormatedExpr);
                        strDisplayExpression.append((String) formatedExpr.get(EffectivityFramework.DISPLAY_VALUE));
                        addOperator = true;
                    }
                }
            }
            String currentEffExprActual = strActualExpression.toString();
            if (strActualExpression != null && strActualExpression.length() > 0) {
                Map cxtsMap = EffectivityFramework.getEffectivityByContext(context, currentEffExprActual);
                if (cxtsMap != null && cxtsMap.size() > 0) {

                    for (Iterator cxtItr = cxtsMap.entrySet().iterator(); cxtItr.hasNext();) {
                        Map.Entry inputEntry = (Map.Entry) cxtItr.next();
                        String cxtPhysicalId = (String) inputEntry.getKey();
                        Map map = new HashMap();

                        if ((cxtPhysicalId == null || "null".equalsIgnoreCase(cxtPhysicalId)) || "Global".equalsIgnoreCase(cxtPhysicalId)) {
                            map.put("contextId", "Global");
                        } else {
                            map.put("contextId", cxtPhysicalId);
                        }

                        map.put("validated", "true");

                        List listValue = (List) ((Map) cxtsMap.get(cxtPhysicalId)).get(EffectivityFramework.LIST_VALUE);
                        List listValueActual = (List) ((Map) cxtsMap.get(cxtPhysicalId)).get(EffectivityFramework.LIST_VALUE_ACTUAL);
                        StringBuffer sbListValue = new StringBuffer();
                        for (int i = 0; i < listValue.size(); i++) {
                            sbListValue.append(listValue.get(i));
                            sbListValue.append("@delimitter@");
                        }

                        String strListValue = sbListValue.toString();
                        sbListValue.delete(0, sbListValue.length());
                        for (int i = 0; i < listValueActual.size(); i++) {
                            sbListValue.append(listValueActual.get(i));
                            sbListValue.append("@delimitter@");
                        }

                        String strListValueAc = sbListValue.toString();
                        map.put(EffectivityFramework.DISPLAY_VALUE, ((Map) cxtsMap.get(cxtPhysicalId)).get(EffectivityFramework.DISPLAY_VALUE));
                        map.put(EffectivityFramework.ACTUAL_VALUE, ((Map) cxtsMap.get(cxtPhysicalId)).get(EffectivityFramework.ACTUAL_VALUE));
                        map.put("contextRuleTextList", strListValue.substring(0, strListValue.length()));
                        map.put("contextRuleActualList", strListValueAc.substring(0, strListValueAc.length()));
                        Map binaryMap = eff.getFilterCompiledBinary(context, currentEffExprActual, null);
                        map.put(EffectivityFramework.COMPILED_BINARY_EXPR, (String) binaryMap.get(EffectivityFramework.COMPILED_BINARY_EXPR));
                        expressionList.add(map);
                    }
                }
            }
            String filterExpr = null;
            String filterValue = null;
            String filterInput = null;
            if (expressionList != null && expressionList.size() > 0) {
                Map expressionMap = (Map) expressionList.get(0);
                filterExpr = (String) expressionMap.get(EffectivityFramework.COMPILED_BINARY_EXPR);
                filterValue = (String) expressionMap.get(EffectivityFramework.ACTUAL_VALUE);
                filterInput = (String) expressionMap.get(EffectivityFramework.DISPLAY_VALUE);
            }
            String pcGlobalFilterCompExpr = null;
            String pcGlobalFilterXMLValue = null;
            if (UIUtil.isNotNullAndNotEmpty(filterExpr))
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

            Map returnMap = new HashMap();
            returnMap.put("pcGlobalFilterCompExpr", pcGlobalFilterCompExpr);
            returnMap.put("pcGlobalFilterXMLValue", pcGlobalFilterXMLValue);
            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);

            return returnMap;
        } catch (Exception exp) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getExpressionsForPC: ", exp);
            // TIGTK-5405 - 11-04-2017 - VB - END
            flushAndCloseSession(plmSession);
            if (context.isTransactionActive()) {
                ContextUtil.abortTransaction(context);
            }
            throw exp;
        }
    }

    /**
     * TIGTK-3583
     * @param context
     * @param sEffExpr
     * @return
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public static String formatEffExpToNeutral(Context context, String sEffExpr) throws Exception {
        // Replace tilde by #
        // Tilde is not accepted when used into an emxIndentedTable cell, so it may have been already replaced into the expression
        // To be consistent and compatible with all possible formats (with or without tilde) we are applying the same transformation here
        String sSeparator = "#";
        String sOrSeparator = ",";
        if (true == sEffExpr.contains("~"))
            sEffExpr = sEffExpr.replaceAll("~", "#");
        StringBuffer sbExp = new StringBuffer();
        String[] sValues = sEffExpr.split("@EF_FO");
        for (int i = 0; i < sValues.length; i++) {
            String[] orValues = null;
            orValues = sValues[i].split(sOrSeparator);
            String sSavedFeat = "";

            for (int j = 0; j < orValues.length; j++) {
                int stPos = -1, midPos = -1, endPos = -1;

                stPos = orValues[j].indexOf("PHY@EF:");
                midPos = orValues[j].indexOf(sSeparator);
                endPos = midPos + 33;

                if (-1 != stPos && -1 != midPos && -1 != endPos) {
                    String sRel = orValues[j].substring(stPos + 7, midPos);
                    String sModel = orValues[j].substring(midPos + 1, endPos);

                    String sFeat = "", sOpt = "";
                    String[] aIds = new String[1];
                    aIds[0] = sRel;
                    StringList lSelects = new StringList(2);
                    lSelects.add(DomainRelationship.SELECT_FROM_NAME);
                    lSelects.add(DomainRelationship.SELECT_TO_NAME);
                    MapList mlRel = DomainRelationship.getInfo(context, aIds, lSelects);
                    if (null != mlRel && 0 < mlRel.size()) {
                        Map mObj = (Map) mlRel.get(0);
                        sFeat = (String) mObj.get(DomainRelationship.SELECT_FROM_NAME);
                        sOpt = (String) mObj.get(DomainRelationship.SELECT_TO_NAME);
                    }

                    String sNew = "";
                    String sStart = "", sEnd = "";
                    if (0 == j) {
                        sStart = "(";
                        sNew = sFeat;
                        sSavedFeat = sFeat;
                    }
                    // Check we are still on the same conf feature
                    else {
                        if (!sFeat.equals(sSavedFeat))
                            throw new Exception("Error while building neutral expression - multiple features into OR expression");
                    }
                    if (orValues.length - 1 == j)
                        sEnd = ")";
                    String sInitial = sStart + "PHY@EF:" + sRel + sSeparator + sModel + sEnd;
                    sNew += "." + sOpt;
                    String sNewExp = orValues[j].replace(sInitial, sNew);
                    sbExp.append(sNewExp);
                } else {
                    sbExp.append(orValues[j]);
                }
            }
        }

        return sbExp.toString();
    }

    /**
     * TIGTK-3583
     * @param context
     * @param sNeutralEff
     * @param sModelId
     * @param sMode
     * @return
     * @throws Exception
     */
    public static String convertEffNeutralToXML(Context context, String sNeutralEff, String sModelId, String sMode) throws Exception {
        FRCSettings sett = new FRCSettings();
        sett.AndOperator = " AND ";
        sett.OrOperator = " OR ";
        sett.NotOperator = " NOT ";

        FRCEffExpr effFactory = new FRCEffExpr(sett, false, false, false);
        FRCEffExpr curEff = effFactory.parseStringToEffExpr(sett, sNeutralEff);
        String sXMLEff = curEff.convertToXML(sMode);

        DomainObject domModel = new DomainObject(sModelId);
        String sModelName = domModel.getName(context);
        String sXMLExpr = addXMLHeaderToEffExpr(sXMLEff, sModelName, sMode);

        return sXMLExpr;
    }

    /**
     * TIGTK-3583
     * @param sXMLEff
     * @param sModelName
     * @param sMode
     * @return
     * @throws Exception
     */
    public static String addXMLHeaderToEffExpr(String sXMLEff, String sModelName, String sMode) throws Exception {
        // FRC: Fixed Bug#556:EBOM and MBOM Refinement function doesn't work (WP2-07 WP2-08 WP-01 and WP3-02)
        String sXMLExpr = "";
        if ("filter".equals(sMode)) {
            String sHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><CfgFilterExpression xmlns=\"urn:com:dassault_systemes:config\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"urn:com:dassault_systemes:config CfgFilterExpression.xsd\"><FilterSelection SelectionMode=\"Strict\">";
            String sContext = "<Context HolderType=\"Model\" HolderName=\"" + sModelName + "\">";
            String sSuffix = "</Context></FilterSelection></CfgFilterExpression>";

            sXMLExpr = sHeader + sContext + sXMLEff + sSuffix;
        } else if ("set".equals(sMode)) {
            String sHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><CfgEffectivityExpression xmlns=\"urn:com:dassault_systemes:config\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"urn:com:dassault_systemes:config CfgEffectivityExpression.xsd\"><Expression>";
            String sContext = "<Context HolderType=\"Model\" HolderName=\"" + sModelName + "\">";
            String sSuffix = "</Context></Expression></CfgEffectivityExpression>";

            sXMLExpr = sHeader + sContext + sXMLEff + sSuffix;
        }
        return sXMLExpr;
    }

}
