package fpdm.mbom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.MapList;
import com.mbom.modeler.utility.FRCMBOMModelerUtility;

import fpdm.mbom.Comparators_mxJPO.MapComparator;
import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.MatrixException;
import matrix.util.StringList;

public class MBOM100Management_mxJPO extends fpdm.mbom.Constants_mxJPO {
    private static Logger logger = LoggerFactory.getLogger("fpdm.mbom.MBOM100Management");

    private static final StringList HARMONY_BUS_SELECT = new StringList(new String[] { SELECT_PHYSICALID, SELECT_ID, SELECT_TYPE, SELECT_NAME, SELECT_REVISION, "attribute.value" });

    private static final StringList PLANT_BUS_SELECT = new StringList(new String[] { SELECT_PHYSICALID, SELECT_TYPE, SELECT_NAME, SELECT_REVISION, SELECT_ID, "attribute.value" });

    private static final StringList COLOR_BUS_SELECT = new StringList(new String[] { SELECT_ID, SELECT_TYPE, SELECT_NAME, SELECT_REVISION, SELECT_DESCRIPTION, "attribute.value" });

    private static final StringList PRODUCT_CONFIGURATION_BUS_SELECT = new StringList(new String[] { SELECT_ID, SELECT_TYPE, SELECT_NAME, SELECT_REVISION, SELECT_DESCRIPTION, "attribute.value" });

    // MBOM 150 % and linked Plant
    private Map<String, List<String>> mMBOMReferencePlants = null;

    // Colors informations
    private Map<String, Map<String, Object>> mColorInfos = null;

    private String sLanguage = "";

    private String sOrganizationName = "";

    /**
     * Constructor
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     * @throws Exception
     */
    public MBOM100Management_mxJPO(Context context, String[] args) throws Exception {
        sLanguage = context.getSession().getLanguage();
        sOrganizationName = fpdm.utils.Person_mxJPO.getOrganizationName(context, context.getUser());
    }

    /**
     * Check if user can have access to launch the command MBOM 100% Management. Only accessible when the current object is at the state (In Work, Review or Release)
     * @plm.usage Command: FPDM_MBOM100Management
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     * @return
     * @throws Exception
     */
    public Boolean hasAccessToMBOM100ManagementCommand(Context context, String[] args) throws Exception {
        boolean isShowCommand = false;

        try {
            HashMap<?, ?> paramMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            String sObjectId = (String) paramMap.get("objectId");
            logger.debug("hasAccessToMBOM100ManagementCommand() - sObjectId = <" + sObjectId + ">");

            // Get current MBOM150% state
            DomainObject doObject = DomainObject.newInstance(context, sObjectId);
            String sCurrent = doObject.getInfo(context, DomainConstants.SELECT_CURRENT);
            logger.debug("hasAccessToMBOM100ManagementCommand() - sCurrent = <" + sCurrent + ">");

            if ("In Work".equals(sCurrent) || "Review".equals(sCurrent) || "Released".equals(sCurrent)) {
                isShowCommand = true;
            }
        } catch (Exception e) {
            logger.error("Error in hasAccessToMBOM100ManagementCommand()\n", e);
            throw e;
        }

        return isShowCommand;
    }

    /**
     * Check if the selected object is the top level assembly or has a scope link
     * @plm.usage JSP: FPDM_GenerateMBOM100PreProcess.jsp
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     * @return
     * @throws Exception
     */
    public String checkTopLevelManufacturingAssembly(Context context, String[] args) throws Exception { // Called by FRCCreateNewScopePreProcess.jsp
        String returnValue = "";

        try {
            String sObjectId = args[0];
            logger.debug("checkTopLevelManufacturingAssembly() - sObjectId = <" + sObjectId + ">");

            // relationship DELFmiFunctionIdentifiedInstance
            String sRelation = getSchemaProperty(context, SYMBOLIC_relationship_DELFmiFunctionIdentifiedInstance);

            // Get the Implemented Items connected.
            DomainObject doObject = DomainObject.newInstance(context, sObjectId);
            if (!doObject.isKindOf(context, getSchemaProperty(context, "type_CreateAssembly")) || (doObject.hasRelatedObjects(context, sRelation, false) && !hasScopeLink(context, sObjectId))) {
                returnValue = EnoviaResourceBundle.getProperty(context, "FRCMBOMCentral", "FPDM_MBOM.message.error.MBOM100Generation.SubAssemblySelected", sLanguage);
            }
        } catch (Exception e) {
            logger.error("Error in checkTopLevelManufacturingAssembly()\n", e);
            throw e;
        }

        return returnValue;
    }

    /**
     * Generate the MBOM 100% objects
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     * @return
     * @throws Exception
     */
    public Object generateMBOM100(Context context, String[] args) throws Exception { // Called by FRCCreateNewScopePreProcess.jsp
        ArrayList<Map<String, String>> mlResults = null;
        StringBuilder sbResult = null;

        try {
            Map<?, ?> paramMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            logger.debug("generateMBOM100() - paramMap = <" + paramMap + ">");
            String sObjectId = (String) paramMap.get("objectId");
            String sAction = (String) paramMap.get("action");
            logger.debug("generateMBOM100() - sObjectId = <" + sObjectId + "> sAction = <" + sAction + ">");
            Set<String> alSelectedHarmonies = getSelectedHarmonies(paramMap);
            logger.debug("generateMBOM100() - alSelectedHarmonies = <" + alSelectedHarmonies + ">");

            // In case there is Technical diversity(Variant ID available for MBOM150% part), if the user select the neutral configuration, the system must not generate of update MBOM 100%
            if (alSelectedHarmonies.size() == 0 && hasTechnicalDiversity(context, sObjectId)) {
                sbResult = new StringBuilder(EnoviaResourceBundle.getProperty(context, "FRCMBOMCentral", "FPDM_MBOM.message.error.MBOM100Generation.MBOM150HasTechnicalDiversity", sLanguage));
                return sbResult;
            }

            // Get linked Plant
            String sPlantPhysicalId = getFirstLinkedPlant(context, sObjectId);
            logger.debug("generateMBOM100() - sPlantPhysicalId = <" + sPlantPhysicalId + ">");
            if (sPlantPhysicalId == null || "".equals(sPlantPhysicalId)) {
                sbResult = new StringBuilder(EnoviaResourceBundle.getProperty(context, "FRCMBOMCentral", "FPDM_MBOM.message.error.MBOM100Generation.NoPlant", sLanguage));
                return sbResult;
            }

            StringList slSelectMBOM150 = new StringList(MBOM150_BUS_SELECT);
            slSelectMBOM150.addElement("from[" + getSchemaProperty(context, SYMBOLIC_relationship_ProcessInstanceContinuous) + "].to.id");
            // Call Expand
            MapList mlMBOM150 = getAllMBOM150Assembly(context, sObjectId, slSelectMBOM150, MBOM150_REL_SELECT);

            // get head Part associated Harmonies
            Map<String, Map<String, Object>> mAssociatedHarmonies = getAssociatedHormonies(context, sObjectId);

            // get Plant informations
            Map<String, Object> mPlantInfo = fpdm.utils.SelectData_mxJPO.getSelectBusinessObjectData(context, sPlantPhysicalId, PLANT_BUS_SELECT);
            logger.debug("generateMBOM100() - mPlantInfo = <" + mPlantInfo + ">");

            Collection<fpdm.mbom.MBOM100_mxJPO> listMBOM100 = null;
            if (alSelectedHarmonies.size() > 0) {
                listMBOM100 = new ArrayList<fpdm.mbom.MBOM100_mxJPO>();
                for (String sSelectedHarmonyId : alSelectedHarmonies) {
                    listMBOM100.addAll(constructMBOM100Tree(context, mlMBOM150, mPlantInfo, sSelectedHarmonyId, mAssociatedHarmonies));
                }

            } else {
                listMBOM100 = constructMBOM100Tree(context, mlMBOM150, mPlantInfo, null, mAssociatedHarmonies);
            }
            logger.debug("generateMBOM100() - listMBOM100.size() = <" + listMBOM100.size() + ">");

            if (listMBOM100.size() > 0) {
                fpdm.mbom.MBOM100_mxJPO headMBOM100 = listMBOM100.iterator().next();

                if ("generate".equals(sAction)) {
                    if (!headMBOM100.checkObjectExist()) {
                        Object oResult = createNewMBOM100(context, listMBOM100);
                        return oResult;
                    } else {
                        sbResult = new StringBuilder(EnoviaResourceBundle.getProperty(context, "FRCMBOMCentral", "FPDM_MBOM.message.error.MBOM100Generation.AlreadyExist", sLanguage));
                        return sbResult;
                    }
                } else if ("update".equals(sAction)) {
                    if (headMBOM100.checkObjectExist()) {
                        mlResults = updateExistingMBOM100(context, listMBOM100);
                    } else {
                        sbResult = new StringBuilder(EnoviaResourceBundle.getProperty(context, "FRCMBOMCentral", "FPDM_MBOM.message.error.MBOM100Update.NotExist", sLanguage));
                        return sbResult;
                    }
                } else if ("revise".equals(sAction)) {
                    if (headMBOM100.checkObjectExist()) {
                        mlResults = reviseExistingMBOM100(context, listMBOM100);
                    } else {
                        sbResult = new StringBuilder(EnoviaResourceBundle.getProperty(context, "FRCMBOMCentral", "FPDM_MBOM.message.error.MBOM100Revision.NotExist", sLanguage));
                        return sbResult;
                    }
                } else {
                    throw new Exception("Unreconized Action <" + sAction + ">.");
                }
            }

        } catch (RuntimeException e) {
            logger.error("Error in generateMBOM100()\n", e);
            throw e;
        } catch (Exception e) {
            logger.error("Error in generateMBOM100()\n", e);
            throw e;
        }

        return mlResults;
    }

    /**
     * Return Configurations and Harmonies linked to the MBOM 150 top level assembly
     * @plm.usage JSP: FPDM_GenerateMBOM100SelectDiversity.jsp
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     * @return
     * @throws FrameworkException
     */
    @SuppressWarnings("unchecked")
    public Map<String, Map<String, Object>> getAllConfigurations(Context context, String[] args) throws FrameworkException, MatrixException {
        Map<String, Map<String, Object>> mConfigurations = new TreeMap<String, Map<String, Object>>();

        try {
            String sObjectId = args[0];
            logger.debug("getAllConfigurations() - sObjectId = <" + sObjectId + ">");

            // get associated Harmonies
            Map<String, Map<String, Object>> mLinkedHarmonies = getAssociatedHormonies(context, sObjectId);

            if (mLinkedHarmonies.size() > 0) {
                Map<?, ?> mHarmony = null;
                Map<?, ?> mConfiguration = null;
                Map<String, Object> mConfig = null;
                String sConfigurationID = null;
                String sConfigurationName = "";
                String sConfigurationDesc = "";
                String sColorPID = null;
                String sVariantAssemblyPID = null;
                ArrayList<Map<?, ?>> mlHarmonies = null;
                MapComparator mComparator = new MapComparator(SELECT_NAME, "ascending", "string");
                for (Object object : mLinkedHarmonies.values()) {
                    mHarmony = (Map<?, ?>) object;
                    sColorPID = (String) mHarmony.get(STR_PSS_ColorPID);
                    sConfigurationID = (String) mHarmony.get(STR_PSS_ProductConfigurationPID);
                    logger.debug("getAllConfigurations() - sColorPID = <" + sColorPID + "> sConfigurationID = <" + sConfigurationID + ">");
                    if (!"".equals(sConfigurationID)) {
                        mConfiguration = (Map<?, ?>) mHarmony.get(sConfigurationID);
                        sConfigurationName = (String) mConfiguration.get(SELECT_NAME);
                        sConfigurationDesc = (String) mConfiguration.get(SELECT_DESCRIPTION);
                    } else {
                        sConfigurationName = "";
                    }
                    logger.debug("getAllConfigurations() - sConfigurationName = <" + sConfigurationName + "> sColorPID = <" + sColorPID + ">");
                    if (!STR_COLOR_IGNORE.equals(sColorPID) && !STR_COLOR_BLANK.equals(sColorPID)) {
                        if (mConfigurations.containsKey(sConfigurationName)) {
                            mConfig = mConfigurations.get(sConfigurationName);
                            mlHarmonies = (ArrayList<Map<?, ?>>) mConfig.get("harmonies");
                            mlHarmonies.add(mHarmony);
                            Collections.sort(mlHarmonies, mComparator);
                        } else {
                            mConfig = new HashMap<String, Object>();
                            mConfigurations.put(sConfigurationName, mConfig);
                            mConfig.put(SELECT_NAME, sConfigurationName);
                            mConfig.put(SELECT_ID, sConfigurationID);
                            mConfig.put(SELECT_DESCRIPTION, sConfigurationDesc);
                            mlHarmonies = new ArrayList<Map<?, ?>>();
                            mlHarmonies.add(mHarmony);

                            mConfig.put("harmonies", mlHarmonies);
                        }

                        // check that VariantAssemblyPID exist on the database
                        Map<String, Object> mHarmonyAssociationRelInfos = (Map<String, Object>) mHarmony.get(STR_HarmonyAssociationRelInfos);
                        logger.debug("getAllConfigurations() - mHarmonyAssociationRelInfos = <" + mHarmonyAssociationRelInfos + ">");
                        if (mHarmonyAssociationRelInfos != null) {
                            sVariantAssemblyPID = getBasicAttributeValueFromMap(context, getSchemaProperty(context, SYMBOLIC_attribute_PSS_VariantAssemblyPID), mHarmonyAssociationRelInfos);
                            if (sVariantAssemblyPID != null && !"".equals(sVariantAssemblyPID)) {
                                BusinessObject boVariantAssembly = new BusinessObject(sVariantAssemblyPID);
                                if (!boVariantAssembly.exists(context)) {
                                    mConfig.put("NewHarmonyAssociationRequested", "true");
                                }
                            }
                        }

                    }
                }
            }

            logger.debug("getAllConfigurations() - mConfigurations = <" + mConfigurations + ">");

        } catch (RuntimeException e) {
            logger.error("Error in getAllConfigurations()\n", e);
            throw e;
        } catch (Exception e) {
            logger.error("Error in getAllConfigurations()\n", e);
            throw e;
        }

        return mConfigurations;
    }

    /**
     * Construct MBOM 100% tree
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param mlMBOM150
     *            the MBOM 150% assembly
     * @param sPlantId
     *            the Plant ID
     * @return
     * @throws Exception
     */
    private Collection<fpdm.mbom.MBOM100_mxJPO> constructMBOM100Tree(Context context, MapList mlMBOM150, Map<String, Object> mPlantInfo, String sSelectedHarmonyKey,
            Map<String, Map<String, Object>> mAssociatedHarmonies) throws Exception {

        Map<String, fpdm.mbom.MBOM100_mxJPO> mResolvedMBOM100 = new LinkedHashMap<String, fpdm.mbom.MBOM100_mxJPO>();
        Map<String, fpdm.mbom.MBOM100_mxJPO> mIgnoredMBOM100 = new LinkedHashMap<String, fpdm.mbom.MBOM100_mxJPO>();
        Map<String, fpdm.mbom.MBOM100_mxJPO> mAllMBOM100 = new LinkedHashMap<String, fpdm.mbom.MBOM100_mxJPO>();
        Map<String, Integer> mMBOM100ParentFindNumber = new LinkedHashMap<String, Integer>();
        Map<?, ?> mMBOM150Info = null;
        String sFromPhysicalId = null;
        String sPhysicalId = null;
        String sPhantomLevel = null;
        String sLevel = null;
        String sKeyAllMBOM100 = null;
        String sKeyMBOM100Parent = null;
        String sType = null;
        String sKeyResolvedMBOM100 = null;
        String sHarmonies = null;
        String sColorID = null;
        String sConfigurationID = null;
        String sSelectedHarmonyID = null;
        Map<String, Object> mColorInfo = null;
        Map<String, Object> mSelectedHarmonyInfo = null;
        String sRegExpHarmonyConfiguration = null;
        if (sSelectedHarmonyKey != null && mAssociatedHarmonies.size() > 0) {
            mSelectedHarmonyInfo = (Map<String, Object>) mAssociatedHarmonies.get(sSelectedHarmonyKey);
            sSelectedHarmonyID = (String) mSelectedHarmonyInfo.get(SELECT_ID);
            sConfigurationID = (String) mSelectedHarmonyInfo.get(STR_PSS_ProductConfigurationPID);
            sRegExpHarmonyConfiguration = String.format("%s:\\s*%s[^| ]*%s:\\s*%s", STR_HarmonyId, sSelectedHarmonyID, STR_PSS_ProductConfigurationPID, sConfigurationID);
        }
        logger.debug("constructMBOM100Tree() - sSelectedHarmonyID = <" + sSelectedHarmonyID + "> sConfigurationID = <" + sConfigurationID + ">");

        String sPlantPhysicalId = (String) mPlantInfo.get(SELECT_PHYSICALID);
        logger.debug("constructMBOM100Tree() - sPlantPhysicalId = <" + sPlantPhysicalId + ">");
        String TYPE_PSS_OPERATION = getSchemaProperty(context, SYMBOLIC_type_PSS_Operation);
        String TYPE_PSS_LINEDATA = getSchemaProperty(context, SYMBOLIC_type_PSS_LineData);
        List<String> lPlants = null;
        fpdm.mbom.MBOM100_mxJPO MBOM100Parent = null;
        fpdm.mbom.MBOM100_mxJPO oMBOM100 = null;
        fpdm.mbom.MBOM100_mxJPO oTemporaryMBOM100 = null;
        int iLevel = 0;
        int iFindNumber;
        for (Iterator<?> iterator = mlMBOM150.iterator(); iterator.hasNext();) {
            mMBOM150Info = (Map<?, ?>) iterator.next();
            logger.debug("constructMBOM100Tree() - mMBOM150Info = <" + mMBOM150Info + ">");
            sType = getBasicAttributeValueFromMap(context, SELECT_TYPE, mMBOM150Info);
            // Ignore PSS_LineData and PSS_Operation
            if (sType.equals(TYPE_PSS_OPERATION) || sType.equals(TYPE_PSS_LINEDATA)) {
                continue;
            }
            sLevel = getBasicAttributeValueFromMap(context, SELECT_LEVEL, mMBOM150Info);
            iLevel = Integer.parseInt(sLevel);
            sPhysicalId = getBasicAttributeValueFromMap(context, SELECT_PHYSICALID, mMBOM150Info);
            sFromPhysicalId = getBasicAttributeValueFromMap(context, SELECT_FROM_PHYSICALID, mMBOM150Info);
            sPhantomLevel = getAttributeValueFromMap(context, SYMBOLIC_attribute_PSS_ManufacturingInstanceExt_PSS_PhantomLevel, mMBOM150Info);
            if (sFromPhysicalId != null && !"".equals(sFromPhysicalId)) {
                // child part get harmonies value on relationship
                sHarmonies = getAttributeValueFromMap(context, SYMBOLIC_PSS_ManufacturingInstanceExt_PSS_Harmonies, mMBOM150Info);
            } else {
                // head part get harmonies value on object
                sHarmonies = getAttributeValueFromMap(context, SYMBOLIC_attribute_PSS_ManufacturingItemExt_PSS_Harmonies, mMBOM150Info);
            }
            logger.debug("constructMBOM100Tree() - sHarmonies = <" + sHarmonies + ">");
            mColorInfo = null;
            if (sHarmonies != null && sSelectedHarmonyID != null) {
                sColorID = getColorID(sHarmonies, sRegExpHarmonyConfiguration);
                logger.debug("constructMBOM100Tree() - sColorID = <" + sColorID + ">");
                if (sColorID == null) {
                    // the current Part is not defined for current Harmony and Configuration
                    continue;
                } else if (!STR_COLOR_BLANK.equals(sColorID) && !STR_COLOR_NOT_ASSIGNED.equals(sColorID) && !STR_COLOR_IGNORE.equals(sColorID)) {
                    mColorInfo = getColorInfo(context, sColorID);
                }
            }
            lPlants = getLinkedPlants(context, sPhysicalId);
            logger.debug("constructMBOM100Tree() - sPhysicalId = <" + sPhysicalId + "> sFromPhysicalId = <" + sFromPhysicalId + "> lPlants = <" + lPlants + ">");

            String sRelationshipName = getBasicAttributeValueFromMap(context, "relationship", mMBOM150Info);
            boolean isMaterial = "ProcessInstanceContinuous".equals(sRelationshipName);
            logger.debug("constructMBOM100Tree() - isMaterial = <" + isMaterial + "> sRelationshipName = <" + sRelationshipName + ">");

            if (lPlants != null && lPlants.contains(sPlantPhysicalId) || isMaterial) {

                // Initialize MBOM100 object
                oMBOM100 = new fpdm.mbom.MBOM100_mxJPO(context, mMBOM150Info, mPlantInfo, mSelectedHarmonyInfo, mColorInfo, sHarmonies, isMaterial, sOrganizationName);

                // Initialize MBOM100 parent
                MBOM100Parent = null;
                if (sFromPhysicalId != null && !"".equals(sFromPhysicalId)) {
                    sKeyMBOM100Parent = sFromPhysicalId + "_" + (iLevel - 1);
                    MBOM100Parent = mAllMBOM100.get(sKeyMBOM100Parent);
                    if (MBOM100Parent == null) {
                        if (mIgnoredMBOM100.containsKey(sKeyMBOM100Parent)) {
                            MBOM100Parent = mIgnoredMBOM100.get(sKeyMBOM100Parent).getParent();
                            if (MBOM100Parent != null) {
                                sKeyMBOM100Parent = MBOM100Parent.getMBOM150Identifier() + "_" + (iLevel - 2);
                            }
                        }
                    }
                    logger.debug("constructMBOM100Tree() - MBOM100Parent = <" + MBOM100Parent + ">");
                    oMBOM100.setParent(MBOM100Parent);
                    // resolve Find Number value
                    iFindNumber = mMBOM100ParentFindNumber.containsKey(sKeyMBOM100Parent) ? (mMBOM100ParentFindNumber.get(sKeyMBOM100Parent).intValue() + 10) : 10;
                    mMBOM100ParentFindNumber.put(sKeyMBOM100Parent, Integer.valueOf(iFindNumber));
                    oMBOM100.setFindNumber(iFindNumber);
                }

                if ("Yes".equals(sPhantomLevel) || STR_COLOR_IGNORE.equals(sColorID) || STR_COLOR_BLANK.equals(sColorID) || (isMaterial && MBOM100Parent == null)) {
                    // Phantom level and when Color Code is Ignore, the object will not be generated in the MBOM 100%
                    sKeyAllMBOM100 = sPhysicalId + "_" + sLevel;
                    mIgnoredMBOM100.put(sKeyAllMBOM100, oMBOM100);
                } else {
                    sKeyAllMBOM100 = sPhysicalId + "_" + sLevel;
                    sKeyResolvedMBOM100 = sPhysicalId + "_" + sFromPhysicalId + "_" + sLevel;
                    // case MBOM 100% in quantity and not in instance (If we have 3 parts at the same level, we will have one link and quantity set to 3 parts)
                    if (mResolvedMBOM100.containsKey(sKeyResolvedMBOM100) && oMBOM100.hasTheSameParent(mResolvedMBOM100.get(sKeyResolvedMBOM100))) {
                        oTemporaryMBOM100 = mResolvedMBOM100.get(sKeyResolvedMBOM100);
                        oTemporaryMBOM100.addQuantity(oMBOM100.getQuantity());
                    } else {
                        mResolvedMBOM100.put(sKeyResolvedMBOM100, oMBOM100);
                        mAllMBOM100.put(sKeyAllMBOM100, oMBOM100);
                    }
                }

            }
        }

        return mResolvedMBOM100.values();

    }

    /**
     * Create new MBOM 100% assembly
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param listMBOM100
     * @return
     * @throws Exception
     */
    private Object createNewMBOM100(Context context, Collection<fpdm.mbom.MBOM100_mxJPO> listMBOM100) throws Exception {

        ArrayList<Map<String, String>> mlResults = new ArrayList<Map<String, String>>();

        // No check of the table mapping on 2.0.5
        // check Legacy Part
        // ArrayList<Map<String, String>> alLegaciesToCheck = getLegacies(listMBOM100);
        // if (alLegaciesToCheck.size() > 0) {
        // StringBuilder sbResult = checkLegacies(context, alLegaciesToCheck);
        // if (sbResult != null) {
        // return sbResult;
        // }
        // }

        try {
            ContextUtil.startTransaction(context, true);
            // create objects references
            for (fpdm.mbom.MBOM100_mxJPO oMBOM100 : listMBOM100) {
                oMBOM100.createObject();
                logger.debug("createNewMBOM100() - oMBOM100 = <" + oMBOM100.toString() + "> created.");

                if (oMBOM100.getParent() == null && !oMBOM100.isMaterial()) {
                    // head MBOM
                    Map<String, String> mResult = new HashMap<String, String>();
                    mResult.put("TopAssemblyName", oMBOM100.getTopAssemblyName());
                    mResult.put("TopAssemblyProductConfiguration", oMBOM100.getProductConfigurationDescription());
                    mResult.put("TopAssemblyHarmony", oMBOM100.getHarmonyName());
                    mResult.put("TopAssemblySynchronized", oMBOM100.getSynchronized());

                    mlResults.add(mResult);
                }
            }
            if (context.isTransactionActive())
                ContextUtil.commitTransaction(context);
        } catch (MatrixException e) {
            ContextUtil.abortTransaction(context);
            throw e;
        }

        return mlResults;
    }

    /**
     * Update existing MBOM 100% assembly
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param listMBOM100
     * @return
     * @throws Exception
     */
    private ArrayList<Map<String, String>> updateExistingMBOM100(Context context, Collection<fpdm.mbom.MBOM100_mxJPO> listMBOM100) throws Exception {
        ArrayList<Map<String, String>> mlResults = new ArrayList<Map<String, String>>();

        for (fpdm.mbom.MBOM100_mxJPO oMBOM100 : listMBOM100) {
            // update object reference
            if (oMBOM100.checkObjectExist()) {
                oMBOM100.updateObject();
            } else {
                oMBOM100.createObject();
            }

            if (oMBOM100.getParent() == null) {
                // head MBOM
                Map<String, String> mResult = new HashMap<String, String>();
                mResult.put("TopAssemblyName", oMBOM100.getTopAssemblyName());
                mResult.put("TopAssemblyProductConfiguration", oMBOM100.getProductConfigurationDescription());
                mResult.put("TopAssemblyHarmony", oMBOM100.getHarmonyName());
                mResult.put("TopAssemblySynchronized", oMBOM100.getSynchronized());

                mlResults.add(mResult);
            } else {
                // add current MBOM100 object ID to the children list of its parent
                oMBOM100.getParent().addNewChildObjectId(oMBOM100.getObjectId());
            }
        }

        // remove existing relationships removed by the update
        for (fpdm.mbom.MBOM100_mxJPO oMBOM100 : listMBOM100) {
            ArrayList<String> alChildrenObjectsIds = oMBOM100.getChildrenObjectsIds();
            Map<String, String> mChildrenInDB = oMBOM100.getChildren();
            logger.debug("updateExistingMBOM100() - alChildrenObjectsIds = <" + alChildrenObjectsIds + "> mChildrenInDB = <" + mChildrenInDB + ">");

            for (Map.Entry<String, String> entry : mChildrenInDB.entrySet()) {
                String sChildID = entry.getKey();
                String sRelationshipID = entry.getValue();
                if (!alChildrenObjectsIds.contains(sChildID)) {
                    DomainRelationship.disconnect(context, sRelationshipID);
                    logger.debug("updateExistingMBOM100() - DomainRelationship.disconnect = <" + sRelationshipID + ">");
                }
            }

        }

        return mlResults;
    }

    /**
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param listMBOM100
     * @return
     * @throws Exception
     */
    private ArrayList<Map<String, String>> reviseExistingMBOM100(Context context, Collection<fpdm.mbom.MBOM100_mxJPO> listMBOM100) throws MatrixException {
        ArrayList<Map<String, String>> mlResults = new ArrayList<Map<String, String>>();

        // do revision process
        try {
            ContextUtil.startTransaction(context, true);
            // create objects references
            for (fpdm.mbom.MBOM100_mxJPO oMBOM100 : listMBOM100) {
                oMBOM100.createObject();

                if (oMBOM100.getParent() == null) {
                    // head MBOM
                    Map<String, String> mResult = new HashMap<String, String>();
                    mResult.put("TopAssemblyName", oMBOM100.getTopAssemblyName());
                    mResult.put("TopAssemblyProductConfiguration", oMBOM100.getProductConfigurationDescription());
                    mResult.put("TopAssemblyHarmony", oMBOM100.getHarmonyName());
                    mResult.put("TopAssemblySynchronized", oMBOM100.getSynchronized());

                    mlResults.add(mResult);
                }
            }
            if (context.isTransactionActive())
                ContextUtil.commitTransaction(context);
        } catch (MatrixException e) {
            logger.error("Error in reviseExistingMBOM100()\n", e);
            ContextUtil.abortTransaction(context);
            throw e;
        }

        return mlResults;
    }

    /**
     * Expand MBOM 150% and return all assembly
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sObjectId
     * @param expandLevel
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private MapList getAllMBOM150Assembly(Context context, String sObjectId, StringList slBusSelect, StringList slRelSelect) throws Exception {
        MapList mlMBOM150 = null;

        ContextUtil.startTransaction(context, false);
        try {

            short expLvl = 0;// Default to Expand All = 0

            DomainObject doHeadObject = DomainObject.newInstance(context, sObjectId);
            // get head Part info
            Map<String, Object> mHeadObjectInfo = doHeadObject.getInfo(context, slBusSelect);

            // Call Expand
            StringBuilder sbRelationPattern = new StringBuilder(getSchemaProperty(context, SYMBOLIC_relationship_DELFmiFunctionIdentifiedInstance));
            sbRelationPattern.append(",").append(getSchemaProperty(context, SYMBOLIC_relationship_ProcessInstanceContinuous));
            // String sRelationPattern = getSchemaProperty(context, SYMBOLIC_relationship_DELFmiFunctionIdentifiedInstance);
            mlMBOM150 = doHeadObject.getRelatedObjects(context, sbRelationPattern.toString(), "*", slBusSelect, slRelSelect, false, true, expLvl, null, null, 0);

            // sort MBOM 150% by TreeOrder
            mlMBOM150.sortStructure("attribute[" + getSchemaProperty(context, SYMBOLIC_attribute_PLMInstance_V_TreeOrder) + "].value", "ascending", "String");

            // add head Part first
            mHeadObjectInfo.put(SELECT_LEVEL, "0");
            mlMBOM150.add(0, mHeadObjectInfo);

            ContextUtil.commitTransaction(context);
        } catch (Exception e) {
            ContextUtil.abortTransaction(context);
            logger.error("Error in getAllMBOM150Assembly()\n", e);
            throw e;
        }
        return mlMBOM150;
    }

    /**
     * Calls the EAI web service which will call KMP241 WebService and returns a list of unmapped material master names.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param alLegaciesToCheck
     *            Parts to check
     * @return
     * @throws Exception
     */
    private StringBuilder checkLegacies(Context context, ArrayList<Map<String, String>> alLegaciesToCheck) throws Exception {

        // send legacies to EAI which will interrogate PI
        fpdm.mbom.FCSCheckLegacy_mxJPO jpo = new fpdm.mbom.FCSCheckLegacy_mxJPO(context, null);
        Vector<String> alUnmappedLegacies = jpo.checkLegaciesMappedInSAP(alLegaciesToCheck);

        if (alUnmappedLegacies.size() > 0) {
            // display a message to the user
            String sErrorMessage1 = EnoviaResourceBundle.getProperty(context, "FRCMBOMCentral", "FPDM_MBOM.message.error.MBOM100Generation.UnmapppedLegacy1", sLanguage);
            String sErrorMessage2 = EnoviaResourceBundle.getProperty(context, "FRCMBOMCentral", "FPDM_MBOM.message.error.MBOM100Generation.UnmapppedLegacy2", sLanguage);

            StringBuilder sbErrorLog = new StringBuilder(sErrorMessage1);
            sbErrorLog.append(" ");

            // Parts
            for (String sLegacy : alUnmappedLegacies) {
                sbErrorLog.append(sLegacy);
                sbErrorLog.append(System.getProperty("line.separator"));
            }
            sbErrorLog.append(sErrorMessage2);

            return sbErrorLog;
        }

        return null;

    }

    /**
     * Get list of MBOMPart to check theirs names on the mapping table.
     * @param aLegacyData
     * @return
     * @throws Exception
     */
    private ArrayList<Map<String, String>> getLegacies(Collection<fpdm.mbom.MBOM100_mxJPO> listMBOM100) throws Exception {

        ArrayList<Map<String, String>> alLegacies = new ArrayList<Map<String, String>>();

        Map<String, String> mLegacy = null;
        for (fpdm.mbom.MBOM100_mxJPO oMBOM100 : listMBOM100) {
            logger.debug("getLegacies() - oMBOM100.isMaterial() = <" + oMBOM100.isMaterial() + "> oMBOM100.getPartName() = <" + oMBOM100.getPartName() + ">");

            if (!useTigerCodification(oMBOM100.getPartName())) {
                oMBOM100.setLegacy(true);
                if (!useLegacyCodificationWithX(oMBOM100.getPartName())) {
                    // We will check on legacy mapping only Parts without 7 digit+X
                    mLegacy = new HashMap<String, String>();
                    mLegacy.put("matrx_matnr", oMBOM100.getFCSReference());
                    mLegacy.put("werks", oMBOM100.getPlantName());

                    alLegacies.add(mLegacy);
                }
            }

        }

        return alLegacies;

    }

    /**
     * This method checks if the Part Name uses the tiger Codification (ex: 3000000)
     * @param sPartName
     * @return
     * @throws Exception
     *             if the operation fails
     */
    private boolean useTigerCodification(String sPartName) throws Exception {
        boolean ok = false;

        Matcher mMaterialCodification = patTigerMaterial.matcher(sPartName);
        Matcher mPartCodification = patTigerPart.matcher(sPartName);

        if (mMaterialCodification.find()) {
            // case of Material start with the character M
            ok = true;
        } else if (mPartCodification.find()) {
            // Check the size of the Part name is 7 (tiger Part codification)
            try {
                int iPartNumber = Integer.parseInt(sPartName);

                // Check if the number is greater than 3000000
                if (iPartNumber >= 3000000) {
                    ok = true;
                }
            } catch (Exception ex) {
                ok = false;
            }
        }

        return ok;
    }

    /**
     * This method checks if the Part Name uses the Legacy Part Codification with X (ex: 1000281X)
     * @param sPartName
     * @return
     * @throws Exception
     *             if the operation fails
     */
    private boolean useLegacyCodificationWithX(String sPartName) throws Exception {
        boolean ok = false;
        Matcher mMaterialCodification = patLegacyFASMaterail.matcher(sPartName);
        Matcher mPartCodification = patLegacyPartWithX.matcher(sPartName);

        if (mMaterialCodification.find()) {
            // case of Material start with the character MAT-
            ok = true;
        } else if (mPartCodification.find()) {
            // Check the size of the Part name is 8 (After 2008 Part codification and before tiger codification)
            // Check if the 7 first characters are digits
            String sPartNumber = sPartName.substring(0, 7);

            try {
                int iPartNumber = Integer.parseInt(sPartNumber);

                // Check if the number is greater than 1000000
                if (iPartNumber > 1000000) {
                    ok = true;
                }
            } catch (Exception ex) {
                ok = false;
            }
        }

        return ok;
    }

    /**
     * Get MBOM 150 % linked Plant
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sMBOM150Id
     *            The Manufacturing 150% ID
     * @return
     * @throws Exception
     */
    private List<String> getLinkedPlants(Context context, String sMBOM150Id) throws Exception {
        List<String> lPlants = null;

        if (mMBOMReferencePlants == null) {
            mMBOMReferencePlants = new HashMap<String, List<String>>();
        }
        if (mMBOMReferencePlants.containsKey(sMBOM150Id)) {
            lPlants = mMBOMReferencePlants.get(sMBOM150Id);
            logger.debug("getLinkedPlants() - mMBOMReferencePlants.get(" + sMBOM150Id + ") = <" + lPlants + ">");

        } else {
            lPlants = FRCMBOMModelerUtility.getPlantsAttachedToMBOMReference(context, null, sMBOM150Id);
            logger.debug("getLinkedPlants() - lPlants=<" + lPlants + ">");
            mMBOMReferencePlants.put(sMBOM150Id, lPlants);
        }

        return lPlants;
    }

    /**
     * Return the first linked Plant (Master Plant)
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sMBOM150Id
     *            MBOM 150% object ID
     * @return
     * @throws Exception
     */
    private String getFirstLinkedPlant(Context context, String sMBOM150Id) throws Exception {
        String sPlantID = pss.mbom.MBOMUtil_mxJPO.getMasterPlant(context, sMBOM150Id);
        logger.debug("getFirstLinkedPlant() - sPlantID = <" + sPlantID + ">");

        return sPlantID;
    }

    /**
     * Get selected Harmonies IDs
     * @param paramMap
     * @return
     */
    private Set<String> getSelectedHarmonies(Map<?, ?> paramMap) {
        Set<String> alHarmonies = new HashSet<String>();

        String sKey = null;
        for (Object oKey : paramMap.keySet()) {
            sKey = (String) oKey;
            if ((sKey.contains("_harmony"))) {
                alHarmonies.add((String) paramMap.get(sKey));
            }
        }

        return alHarmonies;
    }

    /**
     * Get related Harmonies associated to MBOM 150% top level assembly
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sHeadObjectId
     *            MBOM 150% top level assembly ID
     * @return
     * @throws FrameworkException
     */
    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Object>> getAssociatedHormonies(Context context, String sHeadObjectId) throws FrameworkException {
        Map<String, Map<String, Object>> mAssociatedHarmonies = new HashMap<String, Map<String, Object>>();

        DomainObject doObject = DomainObject.newInstance(context);
        DomainRelationship doRel = DomainRelationship.newInstance(context);

        String SELECT_ATTRIBUTE_HARMONIES = "attribute[" + getSchemaProperty(context, SYMBOLIC_attribute_PSS_ManufacturingItemExt_PSS_Harmonies) + "].value";
        String SELECT_ATTRIBUTE_DISPLAY_NAME = "attribute[" + getSchemaProperty(context, SYMBOLIC_attribute_DisplayName) + "].value";

        // get Harmonies attribute value
        doObject.setId(sHeadObjectId);
        String sHarmoniesAttributeValue = doObject.getInfo(context, SELECT_ATTRIBUTE_HARMONIES);
        logger.debug("getAssociatedHormonies() - sHarmoniesAttributeValue = <" + sHarmoniesAttributeValue + ">");

        String[] savalues = sHarmoniesAttributeValue.split("\\|");
        Matcher m = null;
        String sHarmonyID = null;
        String sHarmonyAssociationRelId = null;
        String sColorID = null;
        String sProductConfigurationID = null;
        String sVariantAssemblyID = null;
        Map<String, Object> mHarmonyInfo = null;
        Map<String, Object> mProductConfigurationInfo = null;
        StringBuilder sbKey = null;
        String sDisplayName = null;
        for (int i = 0; i < savalues.length; i++) {
            m = Pattern.compile(sRegExpHarmonyAssociationRelId).matcher(savalues[i]);
            if (m.find()) {
                sHarmonyAssociationRelId = m.group(1);
                // get Harmony id
                sHarmonyID = null;
                m = Pattern.compile(sRegExpHarmonyID).matcher(savalues[i]);
                if (m.find()) {
                    sHarmonyID = m.group(1);
                }
                // get color id
                sColorID = null;
                m = Pattern.compile(sRegExpColorPID).matcher(savalues[i]);
                if (m.find()) {
                    sColorID = m.group(1);
                }
                // get Product Configuration id
                sProductConfigurationID = null;
                m = Pattern.compile(sRegExpProductConfigurationPID).matcher(savalues[i]);
                if (m.find()) {
                    sProductConfigurationID = m.group(1);
                }
                // get Variant Assembly id
                sVariantAssemblyID = null;
                m = Pattern.compile(sRegExpVariantAssemblyPID).matcher(savalues[i]);
                if (m.find()) {
                    sVariantAssemblyID = m.group(1);
                }
                logger.debug("getAssociatedHormonies() - sHarmonyAssociationRelId = <" + sHarmonyAssociationRelId + "> sHarmonyID = <" + sHarmonyID + "> sColorID = <" + sColorID
                        + "> sVariantAssemblyID = <" + sVariantAssemblyID + ">");

                if ((sHarmonyID != null && !"".equals(sHarmonyID)) && !STR_COLOR_IGNORE.equals(sColorID) && !STR_COLOR_BLANK.equals(sColorID)) {
                    sbKey = new StringBuilder();
                    sbKey.append(sProductConfigurationID != null ? sProductConfigurationID : "").append("_").append(sHarmonyID);

                    doObject.setId(sHarmonyID);
                    mHarmonyInfo = doObject.getInfo(context, HARMONY_BUS_SELECT);
                    sDisplayName = (String) mHarmonyInfo.get(SELECT_ATTRIBUTE_DISPLAY_NAME);

                    mHarmonyInfo.put(STR_HarmonyAssociationRelId, sHarmonyAssociationRelId);
                    mHarmonyInfo.put(STR_PSS_ColorPID, sColorID);
                    mHarmonyInfo.put(STR_PSS_ProductConfigurationPID, sProductConfigurationID);
                    mHarmonyInfo.put(STR_PSS_VariantAssemblyPID, sVariantAssemblyID);

                    if (!"".equals(sProductConfigurationID)) {
                        doObject.setId(sProductConfigurationID);
                        mProductConfigurationInfo = doObject.getInfo(context, PRODUCT_CONFIGURATION_BUS_SELECT);
                        mHarmonyInfo.put(sProductConfigurationID, mProductConfigurationInfo);
                    }
                    mHarmonyInfo.put("HARMONYKEY", sbKey.toString());
                    mHarmonyInfo.put("display_name", sDisplayName);

                    // get Harmony association relationship attribute
                    if (sHarmonyAssociationRelId != null && !"".equals(sHarmonyAssociationRelId)) {
                        doRel.setName(sHarmonyAssociationRelId);
                        Map<String, Object> mRelAttributes = doRel.getAttributeMap(context);
                        logger.debug("getAssociatedHormonies() - mRelAttributes = <" + mRelAttributes + ">");
                        mHarmonyInfo.put(STR_HarmonyAssociationRelInfos, mRelAttributes);
                    }

                    mAssociatedHarmonies.put(sbKey.toString(), mHarmonyInfo);
                }
            } else {
                logger.debug("getAssociatedHormonies() - No Hamony expression found on the reference attribute line(" + i + ") = <" + savalues[i] + ">");
            }
        }
        logger.debug("getAssociatedHormonies() - mAssociatedHarmonies = <" + mAssociatedHarmonies + ">");

        return mAssociatedHarmonies;
    }

    /**
     * Extract Color ID from the PSS_Harmonies attribute value corresponding the selected Harmony ID
     * @param sHarmonies
     *            MBOM 150% attribute PSS_Harmonies value
     * @param sSelectedHarmonyID
     *            Selected Harmony ID
     * @return
     */
    private String getColorID(String sHarmonies, String sRegExpHarmonyConfiguration) {
        String sColorID = null;
        String[] savalues = sHarmonies.split("\\|");

        Matcher mColor = null;
        Matcher mHarmonyConfiguration = null;
        logger.debug("getColorID() - sRegExpHarmonyConfiguration = <" + sRegExpHarmonyConfiguration + ">");

        for (int i = 0; i < savalues.length; i++) {
            logger.debug("getColorID() - savalues[" + i + "] = <" + savalues[i] + ">");
            mHarmonyConfiguration = Pattern.compile(sRegExpHarmonyConfiguration).matcher(savalues[i]);
            if (mHarmonyConfiguration.find()) {
                mColor = Pattern.compile(sRegExpColorPID).matcher(savalues[i]);
                if (mColor.find()) {
                    sColorID = mColor.group(1);
                }
            }
        }
        return sColorID;
    }

    /**
     * Get Color informations
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sColorID
     *            Color object ID
     * @return
     * @throws FrameworkException
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getColorInfo(Context context, String sColorID) throws FrameworkException {
        Map<String, Object> mColor = null;
        if (mColorInfos == null) {
            mColorInfos = new HashMap<String, Map<String, Object>>();
        }
        if (mColorInfos.containsKey(sColorID)) {
            mColor = mColorInfos.get(sColorID);
        } else {
            DomainObject doColor = DomainObject.newInstance(context, sColorID);
            mColor = doColor.getInfo(context, COLOR_BUS_SELECT);
            logger.debug("getColorInfo() - mColor=<" + mColor + ">");
            mColorInfos.put(sColorID, mColor);
        }
        return mColor;
    }

    /**
     * Check if there a scope link between the Manufacturing Part and a Product Structure
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sMBOMPhysicalID
     *            MBOM 150% assembly ID
     * @return
     */
    @SuppressWarnings("deprecation")
    private boolean hasScopeLink(Context context, String sMBOMPhysicalID) throws Exception {
        boolean bHasScope = false;
        com.dassault_systemes.vplm.modeler.PLMCoreModelerSession plmSession = null;

        try {
            context.setApplication("VPLM");
            plmSession = com.dassault_systemes.vplm.modeler.PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();
            List<String> inputListForGetScope = new ArrayList<String>();
            inputListForGetScope.add(sMBOMPhysicalID);
            List<String> psScopePID = FRCMBOMModelerUtility.getScopedPSReferencePIDFromList(context, plmSession, inputListForGetScope);
            logger.debug("hasScopeLink() - sMBOMPhysicalID=<" + sMBOMPhysicalID + "> psScopePID=<" + psScopePID + "> psScopePID.size()=<" + psScopePID.size() + ">");
            if (psScopePID.size() > 0) {
                bHasScope = !"".equals(psScopePID.get(0)) ? true : false;
            }
            plmSession.closeSession(true);
        } catch (Exception e) {
            logger.error("Error in hasScopeLink()\n", e);
            throw e;
        } finally {
            try {
                if (plmSession != null) {
                    plmSession.closeSession(true);
                }
            } catch (com.dassault_systemes.vplm.modeler.exception.PLMxModelerException e) {
            }
        }
        return bHasScope;
    }

    /**
     * Check if the current MBOM150% is linked a to a Varaint Assembly
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sObjectId
     *            MBOM 150% assembly ID
     * @return
     * @throws Exception
     */
    private boolean hasTechnicalDiversity(Context context, String sObjectId) throws Exception {
        boolean bResult = false;

        // relationship PSS_PartVariantAssembly
        String sRelation = getSchemaProperty(context, SYMBOLIC_relationship_PSS_PartVariantAssembly);

        // Get the Implemented Items connected.
        DomainObject doObject = DomainObject.newInstance(context, sObjectId);
        if (doObject.hasRelatedObjects(context, sRelation, true)) {
            logger.debug("hasTechnicalDiversity() - MBOM 150 = <" + sObjectId + "> has a technical diversity");
            bResult = true;
        } else {
            logger.debug("hasTechnicalDiversity() - MBOM 150 = <" + sObjectId + "> has no technical diversity");
        }

        return bResult;
    }

}
