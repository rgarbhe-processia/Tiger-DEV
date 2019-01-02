package pss.mbom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.Vector;

import org.omg.Messaging.SyncScopeHelper;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.RelationshipType;
import matrix.util.Pattern;
import matrix.util.StringList;
import pss.constants.TigerConstants;

import com.dassault_systemes.PLMJCoreData.data.interfaces.PLMJExpandNodeIF;
import com.dassault_systemes.vplm.config.interfaces.IPublicConfigurationServices;
import com.dassault_systemes.vplm.config.interfaces.IPublicConfiguredEntityNav;
import com.dassault_systemes.vplm.data.PLMxJExpandSet;
import com.dassault_systemes.vplm.data.interfaces.IPLMExpandFilter;
import com.dassault_systemes.vplm.data.interfaces.IPLMxExpandNode;
import com.dassault_systemes.vplm.interfaces.access.IPLMxCoreAccess;
import com.dassault_systemes.vplm.modeler.PLMCoreModelerSession;
import com.dassault_systemes.vplm.modeler.interfaces.IPLMCoreExpand;
import com.matrixone.apps.configuration.ConfigurationConstants;
import com.matrixone.apps.configuration.ConfigurationUtil;
import com.matrixone.apps.configuration.ProductConfiguration;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.XSSUtil;
import com.matrixone.apps.domain.util.mxType;
import com.matrixone.apps.effectivity.EffectivityExpression;
import com.matrixone.apps.effectivity.EffectivityFramework;
import com.matrixone.apps.framework.ui.UIUtil;
import com.matrixone.plmql.cmd.PLMID;
import com.mbom.modeler.utility.FRCEffExpr;
import com.mbom.modeler.utility.FRCMBOMModelerUtility;
import com.mbom.modeler.utility.FRCSettings;

@SuppressWarnings("deprecation")
public class StructureNodeUtil_mxJPO {

    public static final String MBOMREVISEACTION = "MBOMREVISEACTION";

    public static final StringList EXPD_BUS_SELECT = new StringList(new String[] { "physicalid", "logicalid" });

    public static final StringList EXPD_REL_SELECT = new StringList(
            new String[] { "physicalid[connection]", "logicalid[connection]", "from.physicalid", "attribute[PLMInstance.V_TreeOrder].value", "attribute[PSS_PublishedEBOM.PSS_InstanceName]" });

    // TIGTK-5405 - 11-04-2017 - VB - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(StructureNodeUtil_mxJPO.class);
    // TIGTK-5405 - 11-04-2017 - VB - END

    public int mxMain(Context context, String[] args) throws Exception {

        return 0;
    }

    /**
     * @param context
     * @param args
     * @throws Exception
     * @author Asha Gholve
     */
    @SuppressWarnings("rawtypes")
    public void setResetStructureNodeFlag(Context context, String[] args) throws Exception {

        try {
            String strEvent = args[0];
            String strFromObjectId = args[1];
            String strToObjectId = args[2];
            String strNewValueOfAttribute = args[3];
            StringList relationshipSelects = new StringList(1);
            relationshipSelects.add("physicalid");
            StringBuilder sbwhereClause = new StringBuilder();
            sbwhereClause.append("id==");
            sbwhereClause.append("strToObjectId");
            DomainObject domFromPartObject = new DomainObject(strFromObjectId);
            MapList mapids = domFromPartObject.getRelatedObjects(context, TigerConstants.PATTERN_MBOM_INSTANCE, DomainConstants.QUERY_WILDCARD, DomainConstants.EMPTY_STRINGLIST, relationshipSelects,
                    false, true, (short) 0, sbwhereClause.toString(), "", 0);

            String physicalid = DomainObject.EMPTY_STRING;
            if (mapids != null && !mapids.isEmpty())
                physicalid = (String) ((Map) mapids.get(0)).get("physicalid");

            boolean checkForSetStructureNodeFlag = false;
            boolean checkForResetStructureNodeFlag = false;
            DomainObject domToPartObject = new DomainObject(strToObjectId);
            String strType = (String) domToPartObject.getInfo(context, DomainConstants.SELECT_TYPE);
            if (!strEvent.equalsIgnoreCase("Delete") && (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_LINEDATA) || strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_OPERATION))) {
                return;
            }
            if (strEvent.equalsIgnoreCase("Modify")) {
                if (UIUtil.isNotNullAndNotEmpty(strNewValueOfAttribute)) {
                    checkForSetStructureNodeFlag = true;

                } else {
                    checkForResetStructureNodeFlag = true;
                }

            } else if (strEvent.equalsIgnoreCase("Create")) {

                String strStrucuteNode = domToPartObject.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MBOM_STRUCTURENODE);

                if (strStrucuteNode.equalsIgnoreCase("Yes")) {
                    checkForSetStructureNodeFlag = true;

                }

            } else if (strEvent.equalsIgnoreCase("Delete")) {
                checkForResetStructureNodeFlag = true;

            }
            StringList objectSelects = new StringList();
            objectSelects.add(DomainConstants.SELECT_ID);
            objectSelects.add(TigerConstants.SELECT_ATTRIBUTE_PSS_MBOM_STRUCTURENODE);

            if (checkForSetStructureNodeFlag) {
                String strStrucuteNode = domFromPartObject.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MBOM_STRUCTURENODE);

                if (strStrucuteNode.equalsIgnoreCase("Yes")) {
                    return;
                }
                domFromPartObject.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MBOM_STRUCTURENODE, "Yes");
                MapList mapParentPartObjects = domFromPartObject.getRelatedObjects(context, TigerConstants.PATTERN_MBOM_INSTANCE, DomainConstants.QUERY_WILDCARD, objectSelects,
                        DomainConstants.EMPTY_STRINGLIST, true, false, (short) 0, "", DomainConstants.EMPTY_STRING, 0);

                if (mapParentPartObjects != null && !mapParentPartObjects.isEmpty()) {
                    for (int i = 0; i < mapParentPartObjects.size(); i++) {
                        Map mapParts = (Map) mapParentPartObjects.get(i);
                        DomainObject domChildPart = new DomainObject((String) mapParts.get(DomainConstants.SELECT_ID));
                        domChildPart.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MBOM_STRUCTURENODE, "Yes");

                    }

                }

            }

            if (checkForResetStructureNodeFlag) {
                String strStrucuteNode = domFromPartObject.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MBOM_STRUCTURENODE);

                if (!strStrucuteNode.equalsIgnoreCase("Yes")) {
                    return;
                }
                // TIGTK-3690 : START
                StringList objSelects = new StringList();
                objSelects.add(DomainConstants.SELECT_ID);
                objSelects.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_MBOM_STRUCTURENODE + "]");

                // get all child objects
                MapList mapChildMBOMObjects = domFromPartObject.getRelatedObjects(context, TigerConstants.PATTERN_MBOM_INSTANCE, DomainConstants.QUERY_WILDCARD, objSelects,
                        DomainConstants.EMPTY_STRINGLIST, false, true, (short) 1, "", DomainConstants.EMPTY_STRING, 0);

                if (mapChildMBOMObjects != null && !mapChildMBOMObjects.isEmpty()) {
                    for (int i = 0; i < mapChildMBOMObjects.size(); i++) {
                        Map mapParts = (Map) mapChildMBOMObjects.get(i);
                        // TIGTK-8373:Rutuja Ekatpure:12/6/2017:Start
                        // check if child is same as the node from there relationship we removed effectivity.If yes then just check it is structure node or not.
                        // if it is structure node then no need to reset S/N value just return from fuction
                        strStrucuteNode = (String) mapParts.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_MBOM_STRUCTURENODE + "]");
                        if (strToObjectId.equalsIgnoreCase((String) mapParts.get(DomainConstants.SELECT_ID))) {
                            if ("Yes".equalsIgnoreCase(strStrucuteNode))
                                return;
                        } else {
                            int nIsEffectivityFound = checkForEffectivityOnRelatedStructure(context, (String) mapParts.get(DomainConstants.SELECT_ID), physicalid);

                            // check if child having effectivity set or structure node value is yes or not
                            if (nIsEffectivityFound == 1 || "Yes".equalsIgnoreCase(strStrucuteNode)) {
                                return;
                            }
                        }
                        // TIGTK-8373:Rutuja Ekatpure:12/6/2017:End
                    }
                }
                // if no child with effectivity set then change structure node value 'No'
                domFromPartObject.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MBOM_STRUCTURENODE, "No");
                objectSelects = new StringList();
                objectSelects.add(TigerConstants.SELECT_PHYSICALID);

                StringList strListRelationshipSelect = new StringList();
                strListRelationshipSelect.add(DomainConstants.SELECT_RELATIONSHIP_ID);

                MapList mlConnectedVariantAssembly = domFromPartObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_PARTVARIANTASSEMBLY, TigerConstants.TYPE_PSS_MBOM_VARIANT_ASSEMBLY,
                        objectSelects, strListRelationshipSelect, false, true, (short) 0, null, DomainConstants.EMPTY_STRING, 0);
                for (int i = 0; i < mlConnectedVariantAssembly.size(); i++) {
                    Map eachMap = (Map) mlConnectedVariantAssembly.get(i);
                    String vObjectId = (String) eachMap.get(TigerConstants.SELECT_PHYSICALID);
                    String vConnId = (String) eachMap.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                    disconnectVariantAssemblyWithHarmonyAssociationCleanUp(context, domFromPartObject, vObjectId, vConnId);
                }
                // TIGTK-3690 : END
                resetStructureNodeOnParent(context, strFromObjectId, physicalid);

            }

        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in setResetStructureNodeFlag: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    /**
     * @param context
     * @param strCurrentMBOMObjectId
     * @return
     * @throws Exception
     * @author Asha Gholve
     */
    @SuppressWarnings({ "unchecked" })
    public int checkForEffectivityOnRelatedStructure(Context context, String strCurrentMBOMObjectId, String physicalid) throws Exception {

        DomainObject domFromPartObject = new DomainObject(strCurrentMBOMObjectId);
        String typeStr = domFromPartObject.getInfo(context, DomainConstants.SELECT_TYPE);
        // TIGTK-3690 : START
        StringList conids = (StringList) domFromPartObject.getInfoList(context, "to[" + getMBOMRelationshipPattern(typeStr) + "].physicalid");
        // TIGTK-3690 : END
        if (conids == null || conids.size() == 0) {
            return 0;
        }

        PLMCoreModelerSession plmSession = null;

        ContextUtil.startTransaction(context, false);

        context.setApplication("VPLM");
        plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
        plmSession.openSession();
        Map<String, String> effMap = FRCMBOMModelerUtility.getEffectivityOnInstanceList(context, plmSession, conids, true);
        closeSession(plmSession);
        context.removeApplication("VPLM");
        ContextUtil.commitTransaction(context);
        boolean effect = false;

        for (Map.Entry<String, String> entry : effMap.entrySet()) {

            if (UIUtil.isNotNullAndNotEmpty(entry.getValue()) && !physicalid.equalsIgnoreCase(entry.getKey())) {
                effect = true;
            }
        }

        if (effect) {
            return 1;
        }

        return 0;

    }

    /**
     * @param context
     * @param strCurrentMBOMObjectId
     * @throws Exception
     * @author Asha Gholve
     */
    @SuppressWarnings("rawtypes")
    public void resetStructureNodeOnParent(Context context, String strCurrentMBOMObjectId, String physicalid) throws Exception {
        DomainObject domFromPartObject = new DomainObject(strCurrentMBOMObjectId);
        StringList objectSelects = new StringList();
        // TIGTK-3690 : START
        objectSelects.add(DomainConstants.SELECT_ID);
        objectSelects.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_MBOM_STRUCTURENODE + "]");
        String objectWhere = "attribute[" + TigerConstants.ATTRIBUTE_PSS_MBOM_STRUCTURENODE + "] == 'Yes'";
        // get all parent objects having structure node value 'Yes'
        MapList mapParentMBOMObjects = domFromPartObject.getRelatedObjects(context, TigerConstants.PATTERN_MBOM_INSTANCE, "*", objectSelects, DomainConstants.EMPTY_STRINGLIST, true, false, (short) 1,
                objectWhere, DomainConstants.EMPTY_STRING, 0);

        if (mapParentMBOMObjects != null && !mapParentMBOMObjects.isEmpty()) {
            for (int i = 0; i < mapParentMBOMObjects.size(); i++) {
                Map mapParts = (Map) mapParentMBOMObjects.get(i);
                String strCurrentMBOMObjectId_ = (String) mapParts.get(DomainConstants.SELECT_ID);
                DomainObject domParentPart = new DomainObject(strCurrentMBOMObjectId_);
                // get all child objects
                MapList mapChildMBOMObjects = domParentPart.getRelatedObjects(context, TigerConstants.PATTERN_MBOM_INSTANCE, "*", objectSelects, DomainConstants.EMPTY_STRINGLIST, false, true,
                        (short) 1, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, 0);

                for (int j = 0; j < mapChildMBOMObjects.size(); j++) {
                    Map mapPart = (Map) mapChildMBOMObjects.get(j);

                    String strStructNode = (String) mapPart.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_MBOM_STRUCTURENODE + "]");
                    int nIsEffectivityFound = checkForEffectivityOnRelatedStructure(context, (String) mapPart.get(DomainConstants.SELECT_ID), physicalid);
                    // if child having structure node value "Yes" or effectivity set then return with no change
                    if (nIsEffectivityFound == 1 || "Yes".equalsIgnoreCase(strStructNode)) {
                        return;
                    }
                }
                // if child does not have structure node value "Yes" or effectivity set then make parent structure node value "No"
                domParentPart.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MBOM_STRUCTURENODE, "No");
                resetStructureNodeOnParent(context, strCurrentMBOMObjectId_, physicalid);
                // TIGTK-3690 : END
            }
        }

    }

    public void generateVariantAssemblyCaller(Context context, String[] args) {
        String strpartID = args[0];
        String strConfigurationid = args[2];
        String strXMLStructure = args[3];
        String xml = XSSUtil.decodeFromURL(strXMLStructure);

        try {
            generateVariantAssembly(context, strpartID, strConfigurationid, xml, null);
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in generateVariantAssemblyCaller: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }

    }

    /**
     * @param context
     * @param strMBOMObjectId
     * @param strProductConfigurationId
     * @param strXMLStructure
     * @return
     * @throws Exception
     * @author Asha Gholve
     */
    @SuppressWarnings({ "rawtypes" })
    public int generateVariantAssembly(Context context, String strMBOMObjectId, String strProductConfigurationId, String strXMLStructure, String psRefPID) throws Exception {

        int returnValue = 0;
        boolean transactionStarted = false;
        try {
            PLMCoreModelerSession plmSession = null;
            List<String> mbomRefPIDList = new ArrayList<String>();
            mbomRefPIDList.add(strMBOMObjectId);
            // TIGTK-6366 :Migration - Variant Assembly Revision Format: By AG: Starts
            String StrRevision = "01";
            // TIGTK-6366 :Migration - Variant Assembly Revision Format: By AG: Ends
            String strName = DomainObject.getAutoGeneratedName(context, "type_PSS_MBOMVariantAssembly", null);
            List<String> lstPD = new ArrayList<String>();
            if (UIUtil.isNotNullAndNotEmpty(psRefPID)) {
                lstPD = new ArrayList<String>();
                lstPD.add(psRefPID);
            } else {
                ContextUtil.startTransaction(context, false);
                transactionStarted = true;
                context.setApplication("VPLM");
                plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
                plmSession.openSession();
                lstPD = FRCMBOMModelerUtility.getScopedPSReferencePIDFromList(context, plmSession, mbomRefPIDList);
                closeSession(plmSession);
                if (transactionStarted) {
                    ContextUtil.commitTransaction(context);
                    transactionStarted = false;
                }
                context.removeApplication("VPLM");
            }

            if (lstPD == null || lstPD.isEmpty() || UIUtil.isNullOrEmpty(lstPD.get(0))) {
                DomainObject domMBOM = DomainObject.newInstance(context, strMBOMObjectId);
                String strVname = domMBOM.getAttributeValue(context, TigerConstants.ATTRIBUTE_V_NAME);
                String strFCSIndex = domMBOM.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MBOM_FCSINDEX);
                String strRevison = strFCSIndex.concat(".1");
                StringList busSelects = new StringList(DomainConstants.SELECT_ID);
                MapList mlPSList = DomainObject.findObjects(context, TigerConstants.TYPE_VPMREFERENCE, // Type Pattern
                        strVname, // Name Pattern
                        strRevison, // Rev Pattern
                        null, // Owner Pattern
                        TigerConstants.VAULT_VPLM, // Vault Pattern
                        null, // Where Expression
                        false, // Expand Type
                        busSelects); // Object Pattern

                if (mlPSList != null && mlPSList.size() > 0) {
                    lstPD = new ArrayList<String>();
                    lstPD.add((String) ((Map) mlPSList.get(0)).get(DomainConstants.SELECT_ID));
                }
            }
            String strVARName = DomainObject.EMPTY_STRING;
            if (lstPD != null && UIUtil.isNotNullAndNotEmpty(lstPD.get(0))) {
                DomainObject domPSDom = new DomainObject(lstPD.get(0));
                String strVarinatIDS = domPSDom.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PUBLISHEDPART_PSS_VARIANTASSEMBLYLIST);
                if (UIUtil.isNotNullAndNotEmpty(strVarinatIDS)) {
                    StringBuilder sbObjectWhere = new StringBuilder();
                    sbObjectWhere.append("revision == 'last' && (");
                    String[] varnames = strVarinatIDS.split("\\|");

                    for (int i = 0; i < varnames.length; i++) {
                        DomainObject temp = new DomainObject(varnames[i]);
                        if (i < varnames.length - 1) {

                            sbObjectWhere.append("name == ");
                            sbObjectWhere.append((String) temp.getInfo(context, DomainConstants.SELECT_NAME));
                            sbObjectWhere.append(" || ");
                        } else {
                            sbObjectWhere.append("name == ");
                            sbObjectWhere.append((String) temp.getInfo(context, DomainConstants.SELECT_NAME));
                        }
                    }
                    sbObjectWhere.append(")");

                    DomainObject PCDOm = new DomainObject(strProductConfigurationId);
                    StringList objectSelects = new StringList();
                    objectSelects.add(DomainConstants.SELECT_NAME);

                    MapList mapVariantAssemblies = PCDOm.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_VARIANT_ASSEMBLY_PRODUCT_CONFIGURATION, TigerConstants.TYPE_PSS_VARIANTASSEMBLY,
                            objectSelects, DomainConstants.EMPTY_STRINGLIST, true, false, (short) 1, sbObjectWhere.toString(), DomainConstants.EMPTY_STRING, 0);

                    if (mapVariantAssemblies != null && !mapVariantAssemblies.isEmpty()) {
                        int intVarCount = mapVariantAssemblies.size();
                        for (int i = 0; i < intVarCount; i++) {
                            Map tempMap = (Map) mapVariantAssemblies.get(i);
                            strVARName = (String) tempMap.get("name");
                        }
                    }
                }
            }
            if (!UIUtil.isNotNullAndNotEmpty(strVARName)) {
                strVARName = DomainObject.getAutoGeneratedName(context, "type_PSS_VariantAssembly", null);
            }

            BusinessObject busObj = new BusinessObject(TigerConstants.TYPE_PSS_MBOM_VARIANT_ASSEMBLY, strName, StrRevision, TigerConstants.VAULT_VPLM);
            busObj.create(context, TigerConstants.POLICY_PSS_VARIANTASSEMBLY);
            String strVariantAssemblyId = busObj.getObjectId(context);
            DomainObject dObj = DomainObject.newInstance(context, strVariantAssemblyId);
            dObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_XMLSTRUCTURE, strXMLStructure);
            dObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_VARIANTID, strVARName);
            connectVariantAssembly(context, strMBOMObjectId, strProductConfigurationId, strVariantAssemblyId);

        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in generateVariantAssembly: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            if (transactionStarted) {
                ContextUtil.abortTransaction(context);
            }
            returnValue = 1;
        }

        return returnValue;
    }

    public static void closeSession(PLMCoreModelerSession plmSession) {
        try {
            plmSession.closeSession(true);
        } catch (Exception e) {

        }
    }

    public void connectVariantAssemblyCaller(Context context, String[] args) {
        try {
            String strpartID = args[0];
            String strConfigurationid = args[2];
            String strVariantAssemblyId = args[1];
            // TIGTK-6366 :Migration - Variant Assembly Revision Format: By AG: Starts
            if (args.length > 3)
                updateVariantAssemblyXMLStructure(context, args);
            // TIGTK-6366 :Migration - Variant Assembly Revision Format: By AG: ENds

            connectVariantAssembly(context, strpartID, strConfigurationid, strVariantAssemblyId);
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in connectVariantAssemblyCaller: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }

    }

    /**
     * @param context
     * @param strMBOMObjectId
     * @param strProductConfigurationId
     * @param strVariantAssemblyId
     * @throws Exception
     * @author Asha Gholve
     */
    @SuppressWarnings("rawtypes")
    public void connectVariantAssembly(Context context, String strMBOMObjectId, String strProductConfigurationId, String strVariantAssemblyId) throws Exception {
        try {
            DomainObject domMBOMObject = DomainObject.newInstance(context, strMBOMObjectId);

            String objectWhere = "from[" + TigerConstants.RELATIONSHIP_PSS_VARIANT_ASSEMBLY_PRODUCT_CONFIGURATION + "].to.id ==" + strProductConfigurationId;

            StringList objectSelects = new StringList();
            objectSelects.add(TigerConstants.SELECT_PHYSICALID);

            StringList strListRelationshipSelect = new StringList();
            strListRelationshipSelect.add(DomainConstants.SELECT_RELATIONSHIP_ID);

            MapList mlConnectedVariantAssembly = domMBOMObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_PARTVARIANTASSEMBLY, TigerConstants.TYPE_PSS_MBOM_VARIANT_ASSEMBLY,
                    objectSelects, strListRelationshipSelect, false, true, (short) 0, objectWhere, DomainConstants.EMPTY_STRING, 0);
            if (mlConnectedVariantAssembly != null && !mlConnectedVariantAssembly.isEmpty()) {
                int intVarCount = mlConnectedVariantAssembly.size();

                for (int i = 0; i < intVarCount; i++) {
                    Map tempMap = (Map) mlConnectedVariantAssembly.get(i);
                    String relId = (String) tempMap.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                    String vObjectId = (String) tempMap.get(TigerConstants.SELECT_PHYSICALID);
                    disconnectVariantAssemblyWithHarmonyAssociationCleanUp(context, domMBOMObject, vObjectId, relId);
                }

            }

            String strConnectionIDs = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump $3", strMBOMObjectId,
                    "from[" + TigerConstants.RELATIONSHIP_PSS_PARTVARIANTASSEMBLY + "|to.id=='" + strVariantAssemblyId + "'].id", "|");
            boolean isPushcontext = false;
            if (strConnectionIDs.length() <= 0) {

                try {
                    ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                    isPushcontext = true;
                    DomainRelationship.connect(context, domMBOMObject, TigerConstants.RELATIONSHIP_PSS_PARTVARIANTASSEMBLY, DomainObject.newInstance(context, strVariantAssemblyId));
                } catch (Exception e) {
                    logger.error("Error in connectVariantAssembly: ", e);
                } finally {
                    if (isPushcontext)
                        ContextUtil.popContext(context);
                }

            }

            String[] strParamArray = new String[3];
            strParamArray[0] = strMBOMObjectId;
            strParamArray[1] = strProductConfigurationId;
            String strPCRelatedOptions = getPCRelatedOptionsForPart(context, strParamArray);

            // Connect Relationship between VariantAssembly to ProductConfiguration type
            DomainObject domVarAssembly = DomainObject.newInstance(context, strVariantAssemblyId);
            DomainRelationship domRel = DomainRelationship.connect(context, domVarAssembly, TigerConstants.RELATIONSHIP_PSS_VARIANT_ASSEMBLY_PRODUCT_CONFIGURATION,
                    DomainObject.newInstance(context, strProductConfigurationId));
            DomainRelationship.setAttributeValue(context, domRel.toString(), TigerConstants.ATTRIBUTE_PSS_VARIANTOPTIONS, strPCRelatedOptions);

        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in connectVariantAssembly: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    /**
     * This method is used to get Product Configuration Related options for Structure Node.
     * @param context
     * @param ObjectId
     * @param ProductConfigurationId
     * @throws Exception
     * @author Chintan DADHANIA
     * @modifiedOn 08-08-2016
     * @modifiedBy Chintan DADHANIA
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public String getPCRelatedOptionsForPart(Context context, String args[]) throws Exception {

        // get Root Part and Product Configuration Id from args
        String strRootPartId = args[0];
        String strProductConfigurationId = args[1];
        String SELECT_CONFIGURATION_FEATURE_DISPLAY_NAME = "from." + TigerConstants.SELECT_ATTRIBUTE_DISPLAYNAME;

        // Declare Variables of common use
        HashSet<String> hsFeatureList = new HashSet<String>();
        MapList relBusObjPageList = ProductConfiguration.getSelectedOptions(context, strProductConfigurationId, true, true);

        // Relationship select list

        // Make DomainObject of Root Part and get Effectivity Values form all EBOM Connection
        StringList objectSelect = new StringList(new String[] { "physicalid", "logicalid" });
        objectSelect.add(DomainConstants.SELECT_ID);

        StringList relationshipselect = new StringList(new String[] { "physicalid[connection]", "logicalid[connection]", "from.physicalid", "attribute[PLMInstance.V_TreeOrder].value" });
        relationshipselect.add(DomainConstants.SELECT_RELATIONSHIP_ID);

        MapList mlFilteredParts = getVPMStructureMQL(context, strRootPartId, objectSelect, relationshipselect, (short) 0, null, null);
        StringList conids = new StringList();
        for (int i = 0; i < mlFilteredParts.size(); i++) {
            conids.addElement((String) ((Map) mlFilteredParts.get(i)).get("physicalid[connection]"));
        }

        PLMCoreModelerSession plmSession = null;
        ContextUtil.startTransaction(context, false);
        context.setApplication("VPLM");
        plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
        plmSession.openSession();
        Map<String, String> effMap = FRCMBOMModelerUtility.getEffectivityOnInstanceList(context, plmSession, conids, false);

        closeSession(plmSession);
        context.removeApplication("VPLM");
        ContextUtil.commitTransaction(context);
        // Iterate Result MapList and prepare Unique Feature List.
        for (Map.Entry<String, String> entry : effMap.entrySet()) {
            String eachValue = entry.getValue();
            if (UIUtil.isNotNullAndNotEmpty(eachValue)) {
                EffectivityExpression myExp = new EffectivityExpression(context, eachValue, null, null);
                eachValue = myExp.getActualExpr(context);
                String[] effPartArr = eachValue.split("OR|AND");
                for (int i = 0; i < effPartArr.length; i++) {
                    String[] fullStructureArr = effPartArr[i].trim().split(":");
                    if (fullStructureArr.length > 1) {
                        String[] actualArr = fullStructureArr[1].split("~");

                        if (UIUtil.isNotNullAndNotEmpty(actualArr[0])) {
                            if (!hsFeatureList.contains(actualArr[0])) {
                                hsFeatureList.add(actualArr[0]);
                            }
                        }
                    }
                }
            }
        }
        String[] sFeature = new String[hsFeatureList.size()];
        int iCount = 0;
        for (String strFeatureRelId : hsFeatureList) {
            sFeature[iCount] = strFeatureRelId;
            iCount++;
        }
        StringList slObjectSelect = new StringList();
        slObjectSelect.add("from.physicalid");
        MapList mlFeatureIds = DomainRelationship.getInfo(context, sFeature, slObjectSelect);
        int intFeatureCOunt = mlFeatureIds.size();
        HashSet<String> hsFeatureIds = new HashSet<String>();
        for (int i = 0; i < intFeatureCOunt; i++) {
            hsFeatureIds.add(((Map) mlFeatureIds.get(i)).get("from.physicalid").toString());
        }
        StringList slConfigOptions = new StringList();
        for (int i = 0; i < relBusObjPageList.size(); i++) {
            Map objMap = (Map) relBusObjPageList.get(i);
            if (hsFeatureIds.contains((String) objMap.get("from.physicalid"))) {
                slConfigOptions.add((String) objMap.get("id[connection]"));
            }
        }
        StringList slConfigOptionsRelSelect = new StringList();
        slConfigOptionsRelSelect.add("from." + DomainConstants.SELECT_NAME);
        slConfigOptionsRelSelect.add("from." + DomainConstants.SELECT_ID);
        slConfigOptionsRelSelect.add("to." + DomainConstants.SELECT_ID);
        slConfigOptionsRelSelect.add("to." + DomainConstants.SELECT_NAME);
        slConfigOptionsRelSelect.add("to." + TigerConstants.SELECT_ATTRIBUTE_DISPLAYNAME);
        slConfigOptionsRelSelect.add("from." + TigerConstants.SELECT_ATTRIBUTE_DISPLAYNAME);

        slConfigOptionsRelSelect.add(SELECT_CONFIGURATION_FEATURE_DISPLAY_NAME);

        MapList mlConfigOptionsList = DomainRelationship.getInfo(context, (String[]) slConfigOptions.toArray(new String[slConfigOptions.size()]), slConfigOptionsRelSelect);
        mlConfigOptionsList.sort(TigerConstants.SELECT_ATTRIBUTE_DISPLAYNAME, "ascending", "integer");
        mlConfigOptionsList.sort(SELECT_CONFIGURATION_FEATURE_DISPLAY_NAME, "ascending", "integer");

        StringBuilder sbPCRelatedOptions = new StringBuilder();
        int intPCRelatedOptionsCount = mlConfigOptionsList.size();
        for (int i = 0; i < intPCRelatedOptionsCount; i++) {
            Map objMap = (Map) mlConfigOptionsList.get(i);
            sbPCRelatedOptions.append(objMap.get("from." + TigerConstants.SELECT_ATTRIBUTE_DISPLAYNAME));
            sbPCRelatedOptions.append("{").append(objMap.get("to." + TigerConstants.SELECT_ATTRIBUTE_DISPLAYNAME));
            sbPCRelatedOptions.append("}");
            // If feature is not last then append +
            if (i + 1 < intPCRelatedOptionsCount) {
                sbPCRelatedOptions.append("+");
            }
        }
        return sbPCRelatedOptions.toString();
    }

    @SuppressWarnings("rawtypes")
    public StringList displayPCRelatedOptionsForPart(Context context, String[] args) throws Exception {

        // declare common used variable
        Map tempMap = null;
        String strStructureNodeValue = DomainObject.EMPTY_STRING, strVariantOptions = DomainObject.EMPTY_STRING, strObjectId = DomainObject.EMPTY_STRING;

        // return StringList
        StringList slReturnVariantOptions = new StringList();

        // get ObjectList and Product Configuration Id
        Map programMap = (Map) JPO.unpackArgs(args);

        Map paramList = (Map) programMap.get("paramList");
        MapList mlObjectList = (MapList) programMap.get("objectList");

        String strParentOID = (String) paramList.get("parentOID");

        String[] ObjectIdsArray = new String[mlObjectList.size()];
        ObjectIdsArray[0] = strParentOID;
        for (int i = 0; i < mlObjectList.size(); i++) {
            // convert StringList to String array to pass it in getInfo.
            ObjectIdsArray[i] = (String) ((Map) mlObjectList.get(i)).get("id");
        }

        String strProductConfiguration = (String) paramList.get("FRCExpressionFilterInput_OID");
        for (int i = 0; i < mlObjectList.size(); i++) {
            // convert StringList to String array to pass it in getInfo.
            ObjectIdsArray[i] = (String) ((Map) mlObjectList.get(i)).get("id");

            if (UIUtil.isNullOrEmpty(strProductConfiguration) || strProductConfiguration.equalsIgnoreCase("-none-")) {
                slReturnVariantOptions.add(DomainConstants.EMPTY_STRING);
            }
        }
        if (UIUtil.isNullOrEmpty(strProductConfiguration) || strProductConfiguration.equalsIgnoreCase("-none-")) {
            return slReturnVariantOptions;
        }

        // BusinessObject bom = new BusinessObject(TigerConstants.TYPE_PRODUCTCONFIGURATION, strProductConfiguration, "-", TigerConstants.VAULT_ESERVICEPRODUCTION);
        // String strProductConfigurationId = bom.getObjectId(context);

        // get Actual Name of Attribute,Relationship from Symbolic name

        // Object Select List
        StringList slObjectSelect = new StringList();
        slObjectSelect.add(TigerConstants.SELECT_ATTRIBUTE_PSS_MBOM_STRUCTURENODE);

        // Call getInfo to get StructureNode Attribute value for all parts.
        MapList mlStructureNode = DomainObject.getInfo(context, ObjectIdsArray, slObjectSelect);
        int intStructureNodeCount = ObjectIdsArray.length;

        // Object Select List
        slObjectSelect = new StringList();
        slObjectSelect.add(DomainConstants.SELECT_NAME);
        slObjectSelect.add("id");

        String[] strParamArray = new String[2];
        strParamArray[1] = strProductConfiguration;
        // Iterate Structure Node.
        for (int i = 0; i < intStructureNodeCount; i++) {

            // get Current Map
            tempMap = (Map) mlStructureNode.get(i);

            // Get PSS_StructureNode attribute value.
            strStructureNodeValue = (String) tempMap.get(TigerConstants.SELECT_ATTRIBUTE_PSS_MBOM_STRUCTURENODE);

            if ("Yes".equalsIgnoreCase(strStructureNodeValue)) {
                strObjectId = (String) ObjectIdsArray[i];
                strParamArray[0] = strObjectId;

                strVariantOptions = getVariantOptionsForPart(context, strParamArray);

                slReturnVariantOptions.add(strVariantOptions);
            } else {
                slReturnVariantOptions.add("");
            }

        }

        return slReturnVariantOptions;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Map compareAndGenerateVariantAssembly(Context context, String[] args) throws Exception {

        Map<String, Object> mapVariantAssemblyComparisonData = new HashMap<String, Object>();
        PLMCoreModelerSession plmSession = null;
        try {

            String strMBOMObjectId = args[0];
            String strProductConfiguration = args[1];
            String psRefPID = null;
            if (args.length > 3) {
                psRefPID = args[3];
            }
            if (strProductConfiguration.contains("PC")) {
                BusinessObject bom = new BusinessObject(TigerConstants.TYPE_PRODUCTCONFIGURATION, strProductConfiguration, "-", "eService Production");
                strProductConfiguration = bom.getObjectId(context);
            }

            Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_VARIANTASSEMBLY);
            typePattern.addPattern(TigerConstants.TYPE_PART);

            MapList mlVariantAssemblyForDoubles = new MapList();
            MapList mlVariantAssemblyForUpdate = new MapList();
            DomainObject domMBOMObj = new DomainObject(strMBOMObjectId);
            String currentstate = domMBOMObj.getInfo(context, DomainConstants.SELECT_CURRENT);
            String STATE_INWORK = PropertyUtil.getSchemaProperty(context, "policy", TigerConstants.POLICY_PSS_MBOM, "state_InWork");
            String STATE_MBOM_CANCELLED = PropertyUtil.getSchemaProperty(context, "policy", TigerConstants.POLICY_PSS_MBOM, "state_Cancelled");
            String STATE_OBSOLETE = PropertyUtil.getSchemaProperty(context, "policy", TigerConstants.POLICY_PSS_MBOM, "state_Obsolete");
            String STATE_CANCELLED = PropertyUtil.getSchemaProperty(context, "policy", TigerConstants.POLICY_PSS_MATERIAL, "state_Cancelled");
            String STATE_EXIST = PropertyUtil.getSchemaProperty(context, "policy", TigerConstants.POLICY_OPERATIONLINE_DATA, "state_Exists");
            if (!currentstate.equalsIgnoreCase(STATE_INWORK)) {
                String notice = EnoviaResourceBundle.getProperty(context, TigerConstants.PROPERTIES_EMXFRCMBOMCENTRALSTRINGRESOURCE, context.getLocale(),
                        "PSS_FRCMBOMCentral.AlertMessage.StructureNodeIsNotInWork");
                MqlUtil.mqlCommand(context, "notice $1", notice);
                throw new Exception(notice);
            }

            StringList objectSelect = new StringList(new String[] { "physicalid", "logicalid" });
            objectSelect.add(TigerConstants.SELECT_ATTRIBUTE_PSS_MBOM_STRUCTURENODE);
            objectSelect.add("attribute[" + TigerConstants.ATTRIBUTE_V_NAME + "]");
            objectSelect.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_XMLSTRUCTURE + "]");

            objectSelect.add(DomainConstants.SELECT_ID);
            objectSelect.add(DomainConstants.SELECT_DESCRIPTION);
            objectSelect.add(DomainConstants.SELECT_NAME);
            objectSelect.add(DomainConstants.SELECT_CURRENT);
            objectSelect.add(DomainConstants.SELECT_REVISION);
            objectSelect.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_VARIANTID + "]");

            Map mapParentdata = domMBOMObj.getInfo(context, objectSelect);
            StringList relationshipselect = new StringList(new String[] { "physicalid[connection]", "logicalid[connection]", "from.physicalid", "attribute[PLMInstance.V_TreeOrder].value" });
            relationshipselect.add(DomainConstants.SELECT_RELATIONSHIP_ID);
            relationshipselect.add("attribute[" + TigerConstants.ATTRIBUTE_V_EFFECTIVITYCOMPILEDFORM + "]");
            relationshipselect.add("attribute[" + TigerConstants.ATTRIBUTE_V_HASCONFIGURATIONEFFECTIVITY + "]");

            // TIGTK-13029 :START
            StringBuilder whereClauseObj = new StringBuilder();
            whereClauseObj.append("(current != '");
            whereClauseObj.append(STATE_MBOM_CANCELLED);
            whereClauseObj.append("' && current != '");
            whereClauseObj.append(STATE_OBSOLETE);
            whereClauseObj.append("' && current != '");
            whereClauseObj.append(STATE_CANCELLED);
            whereClauseObj.append("' && current != '");
            whereClauseObj.append(STATE_EXIST);
            whereClauseObj.append("')");
            // TIGTK-13029 :END
            String objectId = (String) mapParentdata.get("physicalid");
            MapList mlFilteredParts = getVPMStructureMQL(context, objectId, objectSelect, relationshipselect, (short) 0, strProductConfiguration, whereClauseObj.toString());

            if (mlFilteredParts != null && !mlFilteredParts.isEmpty()) {
                mlFilteredParts.add(0, mapParentdata);

                for (int i = 0; i < mlFilteredParts.size(); i++) {
                    Map mapCurrentFilteredPart = (Map) mlFilteredParts.get(i);
                    String isStructureNode = (String) mapCurrentFilteredPart.get(TigerConstants.SELECT_ATTRIBUTE_PSS_MBOM_STRUCTURENODE);

                    if (isStructureNode.equalsIgnoreCase("Yes")) {
                        String strPartId = (String) mapCurrentFilteredPart.get("physicalid");
                        DomainObject domCurPartObj = new DomainObject(strPartId);

                        MapList mlStructureNode = getVPMStructureMQL(context, strPartId, objectSelect, relationshipselect, (short) 0, strProductConfiguration, whereClauseObj.toString());
                        String strXMLStructure = prepareXMLStructure(context, strPartId, mlStructureNode);

                        String objectWhere1 = "from[" + TigerConstants.RELATIONSHIP_PSS_VARIANT_ASSEMBLY_PRODUCT_CONFIGURATION + "].to.id == '" + strProductConfiguration + "' && revision == 'last'";
                        MapList mlVariantAssembly = domCurPartObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_PARTVARIANTASSEMBLY, TigerConstants.TYPE_PSS_MBOM_VARIANT_ASSEMBLY,
                                objectSelect, relationshipselect, false, true, (short) 1, objectWhere1, DomainConstants.EMPTY_STRING, 0);

                        // Get the scope on the old reference
                        List<String> mbomRefPIDList = new ArrayList<String>();
                        mbomRefPIDList.add(strPartId);
                        ContextUtil.startTransaction(context, false);
                        context.setApplication("VPLM");
                        plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
                        plmSession.openSession();

                        List<String> scopePSRefPID = FRCMBOMModelerUtility.getScopedPSReferencePIDFromList(context, plmSession, mbomRefPIDList);
                        flushAndCloseSession(plmSession);
                        ContextUtil.commitTransaction(context);

                        boolean deleteVAR = false;
                        if (args.length > 3)
                            deleteVAR = true;
                        /*
                         * if (deleteVAR) {
                         * 
                         * if (mlVariantAssembly != null && !mlVariantAssembly.isEmpty()) { for (int j = 0; j < mlVariantAssembly.size(); j++) { Map mapCurrentVariantAssembly = (Map)
                         * mlVariantAssembly.get(j); String ConID = (String) mapCurrentVariantAssembly.get(DomainConstants.SELECT_RELATIONSHIP_ID); String varID = (String)
                         * mapCurrentVariantAssembly.get(DomainConstants.SELECT_ID); DomainRelationship.disconnect(context, ConID); DomainObject varDom = new DomainObject(varID);
                         * varDom.deleteObject(context, true); } } }
                         */
                        String rootPsRefPID = "";
                        String strVariantIds = "";
                        if (scopePSRefPID.size() > 0 && UIUtil.isNotNullAndNotEmpty(scopePSRefPID.get(0))) {
                            rootPsRefPID = (String) scopePSRefPID.get(0);
                            DomainObject dPSObj = new DomainObject(rootPsRefPID);
                            strVariantIds = dPSObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PUBLISHEDPART_PSS_VARIANTASSEMBLYLIST);
                        }

                        if (deleteVAR && UIUtil.isNotNullAndNotEmpty(rootPsRefPID)) {
                            // EBOM-Modification Start
                            if (UIUtil.isNotNullAndNotEmpty(strVariantIds)) {
                                String[] varNames = strVariantIds.split("\\|");
                                String strPSVarId = "";
                                List<String> lsPSVariantNames = new ArrayList<String>();
                                for (int z = 0; z < varNames.length; z++) {
                                    DomainObject temp = new DomainObject(varNames[z]);
                                    strPSVarId = temp.getInfo(context, DomainConstants.SELECT_NAME);
                                    lsPSVariantNames.add(strPSVarId);
                                }
                                if (mlVariantAssembly != null && !mlVariantAssembly.isEmpty()) {
                                    for (int j = 0; j < mlVariantAssembly.size(); j++) {
                                        Map mapCurrentVariantAssembly = (Map) mlVariantAssembly.get(j);
                                        String conID = (String) mapCurrentVariantAssembly.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                                        String varIDMBOM = (String) mapCurrentVariantAssembly.get("physicalid");

                                        DomainObject varDom = new DomainObject(varIDMBOM);
                                        StringList slVarinantObject = new StringList();
                                        slVarinantObject.add("to[" + TigerConstants.RELATIONSHIP_PSS_VARIANT_ASSEMBLY_PRODUCT_CONFIGURATION + "]");
                                        String strMBOMVarId = (String) mapCurrentVariantAssembly.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_VARIANTID + "]");

                                        if (!lsPSVariantNames.contains(strMBOMVarId)
                                                && varDom.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_PSS_VARIANT_ASSEMBLY_PRODUCT_CONFIGURATION + "]").equalsIgnoreCase("false")) {
                                            disconnectVariantAssemblyWithHarmonyAssociationCleanUp(context, domCurPartObj, varIDMBOM, conID);
                                            varDom.deleteObject(context, true);
                                        }
                                    }
                                }
                            }
                        } // EBOM-Modification End

                        boolean boolThreeandFourCaseTrue = false;
                        // if (mlVariantAssembly == null || mlVariantAssembly.size() == 0 || deleteVAR) {
                        if (mlVariantAssembly == null || mlVariantAssembly.size() == 0) {
                            MapList mlVariantAssembly1 = domCurPartObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_PARTVARIANTASSEMBLY, TigerConstants.TYPE_PSS_MBOM_VARIANT_ASSEMBLY,
                                    objectSelect, relationshipselect, false, true, (short) 1, "revision == 'last'", DomainConstants.EMPTY_STRING, 0);
                            if (mlVariantAssembly1 == null || mlVariantAssembly1.isEmpty()) {
                                boolThreeandFourCaseTrue = true;
                            } else if (mlVariantAssembly1.size() > 0) {
                                boolean boolIsSimilarVariantAssemblyFound = false;
                                for (int j = 0; j < mlVariantAssembly1.size(); j++) {
                                    Map mapCurrentVariantAssembly = (Map) mlVariantAssembly1.get(j);
                                    int nComparisonResult = compareVariantAssembly(context, mapCurrentVariantAssembly, strXMLStructure);

                                    if (nComparisonResult == 0) {
                                        boolIsSimilarVariantAssemblyFound = true;
                                        String strVariantAssemblyId = (String) mapCurrentVariantAssembly.get(DomainConstants.SELECT_ID);
                                        String strVariantAssemblyName = (String) mapCurrentVariantAssembly.get(DomainConstants.SELECT_NAME);
                                        DomainObject DomVariantObject = new DomainObject(strVariantAssemblyId);

                                        StringList lirelPRoduct = new StringList(1);
                                        lirelPRoduct.add(TigerConstants.SELECT_ATTRIBUTE_PSS_VARIANTOPTIONS);
                                        MapList mlProductConfigurations = DomVariantObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_VARIANT_ASSEMBLY_PRODUCT_CONFIGURATION,
                                                TigerConstants.TYPE_PRODUCTCONFIGURATION, objectSelect, lirelPRoduct, false, true, (short) 1, DomainConstants.EMPTY_STRING,
                                                DomainConstants.EMPTY_STRING, 0);

                                        Map<String, Object> temp = new HashMap<String, Object>();
                                        temp.put(DomainConstants.SELECT_ID, (String) mapCurrentFilteredPart.get(DomainConstants.SELECT_ID));
                                        temp.put(DomainConstants.SELECT_DESCRIPTION, (String) mapCurrentFilteredPart.get(DomainConstants.SELECT_DESCRIPTION));
                                        temp.put(DomainConstants.SELECT_NAME, (String) mapCurrentFilteredPart.get(TigerConstants.SELECT_ATTRIBUTE_V_NAME));
                                        temp.put(DomainConstants.SELECT_CURRENT, (String) mapCurrentFilteredPart.get(DomainConstants.SELECT_CURRENT));
                                        temp.put(DomainConstants.SELECT_REVISION, (String) mapCurrentFilteredPart.get(DomainConstants.SELECT_REVISION));
                                        temp.put("strVariantAssemblyId", strVariantAssemblyId);
                                        temp.put("strVariantAssemblyName", strVariantAssemblyName);
                                        temp.put("strProductConfiguration", strProductConfiguration);
                                        temp.put("mlProductConfigurations", mlProductConfigurations);

                                        connectVariantAssembly(context, (String) temp.get(DomainConstants.SELECT_ID), strProductConfiguration, strVariantAssemblyId);

                                        mlVariantAssemblyForDoubles.add(temp);

                                    }
                                }

                                if (!boolIsSimilarVariantAssemblyFound) {
                                    generateVariantAssembly(context, strPartId, strProductConfiguration, strXMLStructure, psRefPID);
                                }
                            }

                            if ((mlVariantAssembly1 == null || mlVariantAssembly1.size() == 0) && boolThreeandFourCaseTrue == true) {
                                generateVariantAssembly(context, strPartId, strProductConfiguration, strXMLStructure, psRefPID);
                            }

                        } else if (mlVariantAssembly.size() == 1) {
                            Map mapCurrentVariantAssembly = (Map) mlVariantAssembly.get(0);
                            // DomainObject domVariantAssembly = new DomainObject((String) mapCurrentVariantAssembly.get(DomainConstants.SELECT_ID));
                            // String objectWhere = "id == '" + strProductConfigurationId + "'";
                            // MapList mapProductConfiguration = domVariantAssembly.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_VARIANT_ASSEMBLY_PRODUCT_CONFIGURATION,
                            // TigerConstants.TYPE_PRODUCTCONFIGURATION, objectSelect,
                            // DomainConstants.EMPTY_STRINGLIST, false, true, (short) 1, objectWhere, DomainConstants.EMPTY_STRING, 0);
                            // if (mapProductConfiguration == null || mapProductConfiguration.isEmpty()) {
                            // boolThreeandFourCaseTrue = true;
                            // } else {

                            int nComparisonResult = compareVariantAssembly(context, mapCurrentVariantAssembly, strXMLStructure);
                            String strVariantAssemblyId = (String) mapCurrentVariantAssembly.get(DomainConstants.SELECT_ID);
                            String strVariantAssemblyName = (String) mapCurrentVariantAssembly.get(DomainConstants.SELECT_NAME);
                            if (nComparisonResult == 1) {

                                if (UIUtil.isNotNullAndNotEmpty((String) mapCurrentVariantAssembly.get(TigerConstants.ATTRIBUTE_PSS_VARIANTID)))
                                    strVariantAssemblyName = (String) mapCurrentVariantAssembly.get(TigerConstants.ATTRIBUTE_PSS_VARIANTID);
                                DomainObject DomVariantObject = new DomainObject(strVariantAssemblyId);

                                StringList relationshipSelect = new StringList();
                                relationshipSelect.add(DomainConstants.SELECT_RELATIONSHIP_ID);
                                relationshipSelect.add(TigerConstants.SELECT_ATTRIBUTE_PSS_VARIANTOPTIONS);

                                MapList mlProductConfigurations = DomVariantObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_VARIANT_ASSEMBLY_PRODUCT_CONFIGURATION,
                                        TigerConstants.TYPE_PRODUCTCONFIGURATION, objectSelect, relationshipSelect, false, true, (short) 1, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING,
                                        0);
                                if (mlProductConfigurations.size() == 1) {

                                    Map mapProductConfi = (Map) mlProductConfigurations.get(0);
                                    Map<String, String> mapInputdata = new HashMap<String, String>();
                                    mapInputdata.put("PartId", (String) mapCurrentFilteredPart.get(DomainConstants.SELECT_ID));
                                    mapInputdata.put("ProductConfigurationId", (String) mapProductConfi.get(DomainConstants.SELECT_ID));
                                    mapInputdata.put("PSS_VariantAssemblyProductConfigurationId", (String) mapProductConfi.get(DomainConstants.SELECT_RELATIONSHIP_ID));
                                    mapInputdata.put("VariantAssemblyId", strVariantAssemblyId);
                                    mapInputdata.put("XMLStructure", strXMLStructure);
                                    updateVariantAssembly(context, mapInputdata);

                                } else if (mlProductConfigurations.size() > 1) {
                                    Map<String, Object> temp = new HashMap<String, Object>();
                                    temp.put(DomainConstants.SELECT_ID, (String) mapCurrentFilteredPart.get(DomainConstants.SELECT_ID));
                                    temp.put(DomainConstants.SELECT_DESCRIPTION, (String) mapCurrentFilteredPart.get(DomainConstants.SELECT_DESCRIPTION));
                                    temp.put(DomainConstants.SELECT_NAME, (String) mapCurrentFilteredPart.get(TigerConstants.SELECT_ATTRIBUTE_V_NAME));
                                    temp.put(DomainConstants.SELECT_POLICY, (String) mapCurrentFilteredPart.get(DomainConstants.SELECT_POLICY));
                                    temp.put(DomainConstants.SELECT_CURRENT, (String) mapCurrentFilteredPart.get(DomainConstants.SELECT_CURRENT));
                                    temp.put(DomainConstants.SELECT_REVISION, (String) mapCurrentFilteredPart.get(DomainConstants.SELECT_REVISION));
                                    temp.put("strVariantAssemblyId", strVariantAssemblyId);
                                    temp.put("strVariantAssemblyName", strVariantAssemblyName);
                                    temp.put("strXMLStructure", strXMLStructure);
                                    temp.put("strProductConfiguration", strProductConfiguration);
                                    temp.put("mlProductConfigurations", mlProductConfigurations);
                                    mlVariantAssemblyForUpdate.add(temp);
                                }

                            }
                            // Scenario-12d
                            if (UIUtil.isNotNullAndNotEmpty(strVariantIds)) {
                                String[] varNamesPS = strVariantIds.split("\\|");
                                for (int y = 0; y < varNamesPS.length; y++) {
                                    DomainObject dPSVariantObj = new DomainObject(varNamesPS[y]);
                                    String strVariantNamePS = dPSVariantObj.getInfo(context, DomainConstants.SELECT_NAME);
                                    MapList mlProductConfigurationsConnectedWithEBOMVA = dPSVariantObj.getRelatedObjects(context,
                                            TigerConstants.RELATIONSHIP_PSS_VARIANT_ASSEMBLY_PRODUCT_CONFIGURATION, TigerConstants.TYPE_PRODUCTCONFIGURATION, objectSelect, relationshipselect, false,
                                            true, (short) 1, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, 0);
                                    Map mapProductConfigConnectedWithEBOMVA = (Map) mlProductConfigurationsConnectedWithEBOMVA.get(0);
                                    String strPCConnectedToPSVariantAssembly = (String) mapProductConfigConnectedWithEBOMVA.get(DomainConstants.SELECT_ID);
                                    if (strPCConnectedToPSVariantAssembly.equals(strProductConfiguration)) {
                                        DomainObject dVariantMBOMObj = new DomainObject(strVariantAssemblyId);
                                        String strMBOMVariantId = dVariantMBOMObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_VARIANTID);
                                        if (!strVariantNamePS.equalsIgnoreCase(strMBOMVariantId)) {
                                            dVariantMBOMObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_VARIANTID, strVariantNamePS);
                                        }
                                    }
                                }
                            }
                        }

                    }
                    mapVariantAssemblyComparisonData.put("mapForVariantAssemblyUpdate", mlVariantAssemblyForUpdate);

                    mapVariantAssemblyComparisonData.put("mapForVariantAssemblyDoubles", mlVariantAssemblyForDoubles);
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in compareAndGenerateVariantAssembly: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
        return mapVariantAssemblyComparisonData;

    }

    public static MapList getVPMStructureMQL(Context context, String rootPID, StringList busSelect, StringList relSelect, short expLvl, String pcId, String whereClauseObj) throws Exception {

        String pcGlobalFilterId = null;
        String whereClauseRel = DomainObject.EMPTY_STRING;
        if (UIUtil.isNotNullAndNotEmpty(pcId)) {
            DomainObject dObj = DomainObject.newInstance(context, pcId);
            String pcGlobalCompExpr = dObj.getAttributeValue(context, "Filter Compiled Form");

            if ((pcGlobalCompExpr != null) && (!("".equals(pcGlobalCompExpr))) && (!("null".equals(pcGlobalCompExpr)))) {
                pcGlobalFilterId = MqlUtil.mqlCommand(context, false, "exec prog ConfigFiltering_InitFilter " + pcGlobalCompExpr, false);
            }

            if ((pcGlobalFilterId != null) && (!("".equals(pcGlobalFilterId)))) {
                whereClauseRel = "escape  ( IF (!(attribute[PLMInstance.V_hasConfigEffectivity] == TRUE)) THEN 1 ELSE (execute[ConfigFiltering_ApplyFilter " + pcGlobalFilterId + "  attribute\\["
                        + "PLMInstance.V_EffectivityCompiledForm" + "\\] ]) )";
            }
        }
        DomainObject objectDOM = new DomainObject(rootPID);

        MapList res = objectDOM.getRelatedObjects(context, "PLMCoreInstance", "PLMCoreReference", busSelect, relSelect, false, true, expLvl, whereClauseObj, whereClauseRel, 0);

        if ((pcGlobalFilterId != null) && (!("".equals(pcGlobalFilterId)))) {
            MqlUtil.mqlCommand(context, false, "exec prog ConfigFiltering_ReleaseFilter " + pcGlobalFilterId, false);
        }

        return res;
    }

    @SuppressWarnings("rawtypes")
    public String prepareXMLStructure(Context context, String strObjectId, MapList mlStructureNode) throws Exception {

        StringBuffer strXML = new StringBuffer("");

        try {
            // Create Domain Object of Main Part
            // TIGTK-13072 :START
            DomainObject domObject = DomainObject.newInstance(context, strObjectId);
            String strName = domObject.getInfo(context, DomainConstants.SELECT_NAME);
            // TIGTK-13072 :END

            int iPartCount = mlStructureNode.size();
            Map tempMap = null;

            // creating stack
            Stack<Integer> stack = new Stack<>();

            int strCurrentStackLevel = 0;

            // TIGTK-13072 :START
            strXML.append("<MBOM identifier=\"" + strName + "\">");
            // TIGTK-13072 :END
            // Iterate over Structured Node
            for (int i = 0; i < iPartCount; i++) {
                tempMap = (Map) mlStructureNode.get(i);
                int strCurrentLevel = Integer.parseInt((String) tempMap.get("level"));
                // TIGTK-13072 :START
                String strPartAttributeDetails = "identifier=\"" + (String) tempMap.get(DomainConstants.SELECT_NAME) + "\"";
                // TIGTK-13072 :END

                String strEBOMAttributeDetails = "level=\"" + strCurrentLevel + "\"";
                String attributeValue = (String) tempMap.get("attribute[" + TigerConstants.ATTRIBUTE_V_EFFECTIVITYCOMPILEDFORM + "]");
                if (UIUtil.isNullOrEmpty(attributeValue)) {
                    attributeValue = "";
                }
                strEBOMAttributeDetails = strEBOMAttributeDetails + " " + TigerConstants.ATTRIBUTE_V_EFFECTIVITYCOMPILEDFORM.replaceAll("\\s", "_") + "=\"" + attributeValue + "\"";

                if (strCurrentLevel == strCurrentStackLevel) {
                    strXML.append("</MBOM></" + TigerConstants.RELATIONSHIP_DELFMIFUNCTIONIDENTIFIEDINSTANCE + ">");
                } else if (strCurrentLevel < strCurrentStackLevel) {
                    int strDiffInLevel = strCurrentStackLevel - strCurrentLevel;

                    for (int p = 0; p < strDiffInLevel; p++) {
                        strXML.append("</MBOM></" + TigerConstants.RELATIONSHIP_DELFMIFUNCTIONIDENTIFIEDINSTANCE + ">");
                        stack.pop();
                    }
                    strXML.append("</MBOM></" + TigerConstants.RELATIONSHIP_DELFMIFUNCTIONIDENTIFIEDINSTANCE + ">");
                    strCurrentStackLevel = stack.peek();
                } else {
                    stack.add(strCurrentLevel);
                    strCurrentStackLevel = strCurrentLevel;
                }
                strXML.append("<" + TigerConstants.RELATIONSHIP_DELFMIFUNCTIONIDENTIFIEDINSTANCE + " ");
                strXML.append(strEBOMAttributeDetails);
                strXML.append("><MBOM ");
                strXML.append(strPartAttributeDetails);
                strXML.append(">");

            }
            int stackSize1 = stack.size();

            for (int j = 0; j < stackSize1; j++) {
                strXML.append("</MBOM></" + TigerConstants.RELATIONSHIP_DELFMIFUNCTIONIDENTIFIEDINSTANCE + ">");
                stack.pop();
            }
            strXML.append("</MBOM>");
        } catch (RuntimeException re) {
            logger.error("Error in getChangeOrderInformation: ", re);
            throw re;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in prepareXMLStructure: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }

        return strXML.toString();
    }

    @SuppressWarnings("rawtypes")
    public int compareVariantAssembly(Context context, Map mapCurrentVariantAssembly, String strXMLStructure) throws Exception {

        int nComparisonResult = 1;

        String strVariantAssemblyXMLStructure = (String) mapCurrentVariantAssembly.get(TigerConstants.SELECT_ATTRIBUTE_PSS_XMLSTRUCTURE);
        if (strVariantAssemblyXMLStructure.equalsIgnoreCase(strXMLStructure)) {

            nComparisonResult = 0;
        }
        return nComparisonResult;
    }

    /**
     * @param context
     * @param args
     * @author SGS
     * @throws Exception
     * @description this method will connvert args to proper arguments and pass it to updateVariantAssembly method
     */
    public void updateVariantAssemblyCaller(Context context, String[] args) throws Exception {
        try {
            String strpartID = args[0];
            String strConfigurationid = args[2];
            String strVariantAssemblyId = args[1];
            String strXMLStructure = args[3];
            // TIGTK-13040 :START
            strXMLStructure = java.net.URLDecoder.decode(strXMLStructure, "UTF-8");
            // TIGTK-13040 :END

            Map<String, String> mapInputdata = new HashMap<String, String>();

            mapInputdata.put("PartId", strpartID);
            mapInputdata.put("ProductConfigurationId", strConfigurationid);
            mapInputdata.put("VariantAssemblyId", strVariantAssemblyId);
            mapInputdata.put("XMLStructure", strXMLStructure);

            updateVariantAssembly(context, mapInputdata);
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in updateVariantAssemblyCaller: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }

    }

    @SuppressWarnings("rawtypes")
    public void updateVariantAssembly(Context context, Map mapArgs) throws Exception {
        try {

            String strVariantAssemblyId = (String) mapArgs.get("VariantAssemblyId");
            String strXMLStructure = (String) mapArgs.get("XMLStructure");

            DomainObject doObjVariantAssemblyId = DomainObject.newInstance(context, strVariantAssemblyId);

            doObjVariantAssemblyId.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_XMLSTRUCTURE, strXMLStructure);

        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in updateVariantAssembly: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }

    }

    @SuppressWarnings("rawtypes")
    public String getVariantAssemblyComparisonTableForDoubles(Context context, String[] args) throws Exception {

        StringBuffer sbComparisonResult = new StringBuffer("");

        try {
            MapList mlComparisonTableData = (MapList) JPO.unpackArgs(args);

            int nComparisonTableDataCount = mlComparisonTableData.size();
            Map<String, String[]> mapPartdata = new HashMap<String, String[]>();
            Map<String, String> mapProductName = new HashMap<String, String>();

            for (int i = 0; i < nComparisonTableDataCount; i++) {
                Map mapStrctureNodeDetail = (Map) mlComparisonTableData.get(i);
                String strStructureNodeId = (String) mapStrctureNodeDetail.get(DomainConstants.SELECT_ID);
                String strVariantAssemblyId = (String) mapStrctureNodeDetail.get("strVariantAssemblyId");
                String strVariantAssemblyName = (String) mapStrctureNodeDetail.get("strVariantAssemblyName");
                String[] partrelteddata = new String[5];
                boolean isChecked = false;
                if (!mapPartdata.containsKey(strStructureNodeId)) {
                    String strStructureNodeName = (String) mapStrctureNodeDetail.get(DomainConstants.SELECT_NAME);
                    String strStructureNodeRevision = (String) mapStrctureNodeDetail.get(DomainConstants.SELECT_REVISION);
                    String strStructureNodeCurrent = (String) mapStrctureNodeDetail.get(DomainConstants.SELECT_CURRENT);
                    String strStructureNodeDescription = (String) mapStrctureNodeDetail.get(DomainConstants.SELECT_DESCRIPTION);
                    StringBuffer abinitialrow = new StringBuffer("");
                    abinitialrow.append("<tr>");
                    abinitialrow.append("<td>");
                    abinitialrow.append(strStructureNodeName);
                    abinitialrow.append("</td>");
                    abinitialrow.append("<td>");
                    abinitialrow.append(strStructureNodeRevision);
                    abinitialrow.append("</td>");
                    abinitialrow.append("<td>");
                    abinitialrow.append(strStructureNodeCurrent);
                    abinitialrow.append("</td>");
                    abinitialrow.append("<td>");
                    abinitialrow.append(strStructureNodeDescription);
                    abinitialrow.append("</td>");

                    partrelteddata[0] = abinitialrow.toString();
                    partrelteddata[1] = "";
                    partrelteddata[2] = "";
                    partrelteddata[3] = "";
                    partrelteddata[4] = "";
                    isChecked = true;

                } else {

                    partrelteddata = mapPartdata.get(strStructureNodeId);
                }

                String strProductConfiguration = (String) mapStrctureNodeDetail.get("strProductConfiguration");

                MapList mlProductConfigurations = (MapList) mapStrctureNodeDetail.get("mlProductConfigurations");
                int ProductConfigurtionsCount = mlProductConfigurations.size();
                String strclassName = strStructureNodeId + "|" + strVariantAssemblyId + "|" + strProductConfiguration;

                String checkedval = isChecked ? "checked" : "";
                StringBuffer sbVariantAssembly = new StringBuffer();
                sbVariantAssembly.append("<input type='radio' name='");
                sbVariantAssembly.append(strStructureNodeId);
                sbVariantAssembly.append("' value='");
                sbVariantAssembly.append(strclassName);
                sbVariantAssembly.append("' ");
                sbVariantAssembly.append(checkedval);
                sbVariantAssembly.append(" />");
                sbVariantAssembly.append(strVariantAssemblyName);
                sbVariantAssembly.append("</br>");
                partrelteddata[1] += sbVariantAssembly.toString();
                StringBuffer sbProductMarketingName = new StringBuffer();

                StringBuffer sbProductConfig = new StringBuffer();
                StringBuffer sbVariantOptions = new StringBuffer();

                for (int j = 0; j < ProductConfigurtionsCount; j++) {
                    sbProductMarketingName.append("Product Marketing Name :");
                    String strProductID = (String) ((Map) mlProductConfigurations.get(j)).get(DomainConstants.SELECT_ID);
                    if (mapProductName.containsKey(strProductID)) {
                        sbProductMarketingName.append(mapProductName.get(strProductID));
                        sbProductMarketingName.append("</br>");
                    } else {
                        DomainObject domProductConfig = new DomainObject(strProductID);
                        StringList attributes = new StringList(1);
                        attributes.add(TigerConstants.SELECT_ATTRIBUTE_MARKETINGNAME);
                        Map product = domProductConfig.getRelatedObject(context, TigerConstants.RELATIONSHIP_FEATUREPRODUCTCONFIGURATION, false, attributes, DomainConstants.EMPTY_STRINGLIST);
                        if (!product.isEmpty()) {
                            sbProductMarketingName
                                    .append((String) product.get(TigerConstants.SELECT_ATTRIBUTE_MARKETINGNAME) == null ? "" : (String) product.get(TigerConstants.SELECT_ATTRIBUTE_MARKETINGNAME));
                            sbProductMarketingName.append("</br>");
                            mapProductName.put(strProductID,
                                    (String) product.get(TigerConstants.SELECT_ATTRIBUTE_MARKETINGNAME) == null ? "" : (String) product.get(TigerConstants.SELECT_ATTRIBUTE_MARKETINGNAME));
                        }

                    }
                    sbProductConfig.append(((Map) mlProductConfigurations.get(j)).get(DomainConstants.SELECT_NAME));
                    sbProductConfig.append(" : ");
                    sbProductConfig.append((String) ((Map) mlProductConfigurations.get(j)).get(TigerConstants.SELECT_ATTRIBUTE_MARKETINGNAME) == null ? ""
                            : ((Map) mlProductConfigurations.get(j)).get(TigerConstants.SELECT_ATTRIBUTE_MARKETINGNAME));
                    sbProductConfig.append("</br>");
                    sbVariantOptions.append((String) ((Map) mlProductConfigurations.get(j)).get(TigerConstants.SELECT_ATTRIBUTE_PSS_VARIANTOPTIONS) == null ? ""
                            : ((Map) mlProductConfigurations.get(j)).get(TigerConstants.SELECT_ATTRIBUTE_PSS_VARIANTOPTIONS));
                    sbVariantOptions.append("</br>");

                }
                partrelteddata[2] += sbProductMarketingName.toString();
                partrelteddata[3] += sbProductConfig.toString();
                partrelteddata[4] += sbVariantOptions.toString();
                mapPartdata.put(strStructureNodeId, partrelteddata);

            }
            for (Map.Entry<String, String[]> entry : mapPartdata.entrySet()) {

                String[] data = entry.getValue();
                sbComparisonResult.append(data[0]);
                sbComparisonResult.append("<td>");
                sbComparisonResult.append(data[1]);
                sbComparisonResult.append("</td>");
                sbComparisonResult.append("<td>");
                sbComparisonResult.append(data[4]);
                sbComparisonResult.append("</td>");
                sbComparisonResult.append("<td>");
                sbComparisonResult.append(data[2]);
                sbComparisonResult.append("</td>");
                sbComparisonResult.append("<td>");
                sbComparisonResult.append(data[3]);
                sbComparisonResult.append("</td>");
                sbComparisonResult.append("</tr>");
            }

        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getVariantAssemblyComparisonTableForDoubles: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END

        }
        return sbComparisonResult.toString();

    }

    /**
     * This method is used to get Variant Assembly Id Column value
     * @param context
     * @param objectList
     * @throws Exception
     * @author Asha Gholve
     */
    @SuppressWarnings("rawtypes")
    public StringList displayVariantAssemblyID(Context context, String[] args) throws Exception {
        // declare common used variable

        StringList slReturnVariantIds = new StringList();
        try {
            Map tempMap = null;
            String strStructureNodeValue = DomainObject.EMPTY_STRING, strVariantId = DomainObject.EMPTY_STRING, strObjectId = DomainObject.EMPTY_STRING;

            // get ObjectList and Product Configuration Id
            Map programMap = (Map) JPO.unpackArgs(args);

            Map paramList = (Map) programMap.get("paramList");
            MapList mlObjectList = (MapList) programMap.get("objectList");
            String[] ObjectIdsArray = new String[mlObjectList.size()];
            String strProductConfigurationId = (String) paramList.get("FRCExpressionFilterInput_OID");
            for (int i = 0; i < mlObjectList.size(); i++) {
                // convert StringList to String array to pass it in getInfo.
                ObjectIdsArray[i] = (String) ((Map) mlObjectList.get(i)).get("id");
                if (UIUtil.isNullOrEmpty(strProductConfigurationId) || strProductConfigurationId.equalsIgnoreCase("-none-")) {
                    slReturnVariantIds.add(DomainConstants.EMPTY_STRING);
                }
            }
            if (UIUtil.isNullOrEmpty(strProductConfigurationId) || strProductConfigurationId.equalsIgnoreCase("-none-")) {
                return slReturnVariantIds;
            }

            // BusinessObject bom = new BusinessObject("Product Configuration", strProductConfiguration, "-", "eService Production");
            // String strProductConfigurationId = bom.getObjectId(context);

            // Object Select List
            StringList slObjectSelect = new StringList();
            slObjectSelect.add(TigerConstants.SELECT_ATTRIBUTE_PSS_MBOM_STRUCTURENODE);

            // Call getInfo to get StructureNode Attribute value for all parts.
            MapList mlStructureNode = DomainObject.getInfo(context, ObjectIdsArray, slObjectSelect);

            int intStructureNodeCount = ObjectIdsArray.length;

            String[] strParamArray = new String[2];
            strParamArray[1] = strProductConfigurationId;

            // Iterate Structure Node.
            for (int i = 0; i < intStructureNodeCount; i++) {

                // get Current Map
                tempMap = (Map) mlStructureNode.get(i);

                // Get PSS_StructureNode attribute value.
                strStructureNodeValue = (String) tempMap.get(TigerConstants.SELECT_ATTRIBUTE_PSS_MBOM_STRUCTURENODE);

                if ("Yes".equalsIgnoreCase(strStructureNodeValue)) {
                    strObjectId = (String) ObjectIdsArray[i];
                    // set current Part Object Id into Array
                    strParamArray[0] = strObjectId;
                    strVariantId = getVariantAssemblyID(context, strParamArray);
                    slReturnVariantIds.add(strVariantId);
                } else {
                    slReturnVariantIds.add("");
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in displayVariantAssemblyID: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
        return slReturnVariantIds;
    }

    /**
     * This method is used to get Variant Assembly Id Column value
     * @param context
     * @param objectList
     * @throws Exception
     * @author Asha Gholve
     */
    @SuppressWarnings("rawtypes")
    public StringList displayStuctureNode(Context context, String[] args) throws Exception {
        // declare common used variable
        StringList slReturnVariantIds = new StringList();
        try {
            Map tempMap = null;
            String strStructureNodeValue = DomainObject.EMPTY_STRING;

            // get ObjectList and Product Configuration Id
            Map programMap = (Map) JPO.unpackArgs(args);
            MapList mlObjectList = (MapList) programMap.get("objectList");

            String[] ObjectIdsArray = new String[mlObjectList.size()];

            for (int i = 0; i < mlObjectList.size(); i++) {
                // convert StringList to String array to pass it in getInfo.
                ObjectIdsArray[i] = (String) ((Map) mlObjectList.get(i)).get("id");
            }

            // Object Select List
            StringList slObjectSelect = new StringList();
            slObjectSelect.add(TigerConstants.SELECT_ATTRIBUTE_PSS_MBOM_STRUCTURENODE);
            slObjectSelect.add(DomainConstants.SELECT_TYPE);

            // Call getInfo to get StructureNode Attribute value for all parts.
            MapList mlStructureNode = DomainObject.getInfo(context, ObjectIdsArray, slObjectSelect);

            int intStructureNodeCount = ObjectIdsArray.length;

            // Iterate Structure Node.
            for (int i = 0; i < intStructureNodeCount; i++) {

                // get Current Map
                tempMap = (Map) mlStructureNode.get(i);

                // Get PSS_StructureNode attribute value.
                strStructureNodeValue = (String) tempMap.get(TigerConstants.SELECT_ATTRIBUTE_PSS_MBOM_STRUCTURENODE);

                // if ("Yes".equalsIgnoreCase(strStructureNodeValue) && //TYPE_CREATEASSEMBLY.equalsIgnoreCase((String) tempMap.get(DomainConstants.SELECT_TYPE))) {
                if ("Yes".equalsIgnoreCase(strStructureNodeValue)) {
                    slReturnVariantIds.add(strStructureNodeValue);
                } else {
                    slReturnVariantIds.add("No");
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in displayStuctureNode: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }

        return slReturnVariantIds;
    }

    /**
     * This method is used to get Variant Assembly Id for Root Part.
     * @param context
     * @param ObjectId
     * @param ProductConfigurationId
     * @throws Exception
     * @author Asha Gholve
     */

    @SuppressWarnings("rawtypes")
    public String getVariantAssemblyID(Context context, String args[]) throws Exception {
        try {
            String strObjectId = args[0];
            String strProductConfigurationId = args[1];

            // If Object Id or ProductConfigId is empty then return EMPTY.
            if ("".equalsIgnoreCase(strObjectId) || "".equalsIgnoreCase(strProductConfigurationId)) {
                return "";
            }

            // Create Domain Object of Root Part.
            DomainObject domPartObject = DomainObject.newInstance(context, strObjectId);

            // Object Select List
            StringList slObjectSelect = new StringList();
            slObjectSelect.add(DomainConstants.SELECT_NAME);
            slObjectSelect.add(TigerConstants.SELECT_ATTRIBUTE_PSS_VARIANTID);

            // Relationship Select List
            StringList slRelSelect = new StringList();
            slRelSelect.add(DomainConstants.SELECT_ID);

            // get Actual name from Symbolic name

            // Object Where clause
            String strObjectWhere = "from[" + TigerConstants.RELATIONSHIP_PSS_VARIANT_ASSEMBLY_PRODUCT_CONFIGURATION + "].to.id == '" + strProductConfigurationId + "'";

            // Get Variant Assembly Id which is connected with Part and Product
            // Configuration Id
            MapList mlVariantAssembly = domPartObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_PARTVARIANTASSEMBLY, TigerConstants.TYPE_PSS_MBOM_VARIANT_ASSEMBLY, slObjectSelect,
                    slRelSelect, false, true, (short) 0, strObjectWhere, "", 0);

            String strVariantId = DomainObject.EMPTY_STRING;

            if (mlVariantAssembly.size() == 1) {
                strVariantId = (String) ((Map) mlVariantAssembly.get(0)).get(TigerConstants.SELECT_ATTRIBUTE_PSS_VARIANTID);
            } else {
                strVariantId = "";
            }

            return strVariantId;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getVariantAssemblyID: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }

    }

    /**
     * This method is used to get Variant Assembly Id for Root Part.
     * @param context
     * @param ObjectId
     * @param ProductConfigurationId
     * @throws Exception
     * @author Chintan DADHANIA
     */

    @SuppressWarnings("rawtypes")
    public String getVariantOptionsForPart(Context context, String args[]) throws Exception {
        String strObjectId = args[0];
        String strProductConfigurationId = args[1];
        // If Object Id or ProductConfigId is empty then return EMPTY.
        if ("".equalsIgnoreCase(strObjectId) || "".equalsIgnoreCase(strProductConfigurationId)) {
            return "";
        }

        // Create Domain Object of Root Part.
        DomainObject domPartObject = DomainObject.newInstance(context, strObjectId);

        // Object Select List
        StringList slObjectSelect = new StringList();
        slObjectSelect.add(DomainConstants.SELECT_NAME);
        slObjectSelect.add(DomainConstants.SELECT_ID);

        // Relationship Select List
        StringList slRelSelect = new StringList();
        slRelSelect.add(DomainConstants.SELECT_ID);

        // get Actual Name of Attribute,Relationship from Symbolic name
        slRelSelect.add(TigerConstants.SELECT_ATTRIBUTE_PSS_VARIANTOPTIONS);
        String strVariantOptions = DomainObject.EMPTY_STRING;
        // Object Where clause
        String strObjectWhere = "from[" + TigerConstants.RELATIONSHIP_PSS_VARIANT_ASSEMBLY_PRODUCT_CONFIGURATION + "].to.id==" + strProductConfigurationId;

        // Get Variant Assembly Id which is connected with Part and Product Configuration Id

        MapList mlVariantAssembly = domPartObject.getRelatedObjects(context, // Context
                TigerConstants.RELATIONSHIP_PSS_PARTVARIANTASSEMBLY, // Relationship Pattern
                TigerConstants.TYPE_PSS_MBOM_VARIANT_ASSEMBLY, // Type Pattern
                slObjectSelect, // Object Select
                DomainConstants.EMPTY_STRINGLIST, // Relationship Select
                false, // To Side
                true, // from Side
                (short) 0, // Recursion Level
                strObjectWhere, // Object Where clause
                "", // Relationship Where clause
                0, // Limit
                null, // Post Relationship Patten
                null, // Post Type Pattern
                null); // Post Patterns
        if (mlVariantAssembly.size() > 0) {
            String strVariantAssemblyID = (String) ((Map) mlVariantAssembly.get(0)).get(DomainConstants.SELECT_ID);
            DomainObject domProd = new DomainObject(strVariantAssemblyID);
            strObjectWhere = "id==" + strProductConfigurationId;

            MapList mlProduct = domProd.getRelatedObjects(context, // Context
                    TigerConstants.RELATIONSHIP_PSS_VARIANT_ASSEMBLY_PRODUCT_CONFIGURATION, // Relationship Pattern
                    TigerConstants.TYPE_PRODUCTCONFIGURATION, // Type Pattern
                    DomainConstants.EMPTY_STRINGLIST, // Object Select
                    slRelSelect, // Relationship Select
                    false, // To Side
                    true, // from Side
                    (short) 0, // Recursion Level
                    strObjectWhere, // Object Where clause
                    "", // Relationship Where clause
                    0, // Limit
                    null, // Post Relationship Patten
                    null, // Post Type Pattern
                    null); // Post Patterns

            // if size is greator than 0 then get VariantId from first Map and add it to Return StringList.
            if (mlProduct.size() > 0) {
                strVariantOptions = (String) ((Map) mlProduct.get(0)).get(TigerConstants.SELECT_ATTRIBUTE_PSS_VARIANTOPTIONS);
            } else {
                strVariantOptions = "";
            }

        }

        return strVariantOptions;
    }

    public void disconnectUnusedVariantAssembly(Context context, String[] args) throws Exception {
        try {
            String strVariantAssemblyId = args[0];
            DomainObject domVariantAssembly = DomainObject.newInstance(context, strVariantAssemblyId);
            StringList slObjSelects = new StringList();
            StringList slRelSelects = new StringList();
            slRelSelects.add(DomainRelationship.SELECT_ID);

            MapList mlProductConfigurations = domVariantAssembly.getRelatedObjects(context, // context
                    TigerConstants.RELATIONSHIP_PSS_VARIANT_ASSEMBLY_PRODUCT_CONFIGURATION, // relationshipPattern
                    TigerConstants.TYPE_PRODUCTCONFIGURATION, // typePattern
                    slObjSelects, // objectSelects
                    slRelSelects, // relationshipSelects
                    false, // getTo
                    true, // getFrom
                    (short) 0, // recurseToLevel
                    null, // objectWhere
                    null, // relationshipWhere
                    (short) 0, // limit
                    null, // includeType
                    null, // includeRelationship
                    null); // includeMap

            if (mlProductConfigurations.isEmpty() && mlProductConfigurations.size() == 0) {

                MapList mlParts = domVariantAssembly.getRelatedObjects(context, // context
                        TigerConstants.RELATIONSHIP_PSS_PARTVARIANTASSEMBLY, // relationshipPattern
                        DomainConstants.TYPE_PART, // typePattern
                        slObjSelects, // objectSelects
                        slRelSelects, // relationshipSelects
                        true, // getTo
                        false, // getFrom
                        (short) 0, // recurseToLevel
                        null, // objectWhere
                        null, // relationshipWhere
                        (short) 0, // limit
                        null, // includeType
                        null, // includeRelationship
                        null); // includeMap
                for (int itr = 0; itr < mlParts.size(); itr++) {
                    Map<?, ?> mPart = (Map<?, ?>) mlParts.get(itr);
                    String strConnectionId = (String) mPart.get(DomainRelationship.SELECT_ID);
                    DomainRelationship.disconnect(context, strConnectionId);
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in disconnectUnusedVariantAssembly: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }

    }

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public String getVariantAssemblyComparisonTableForUpdate(Context context, String[] args) throws Exception {

        StringBuffer sbComparisonResult = new StringBuffer("");

        try {
            MapList mlComparisonTableData = (MapList) JPO.unpackArgs(args);
            int nComparisonTableDataCount = mlComparisonTableData.size();

            for (int i = 0; i < nComparisonTableDataCount; i++) {
                Map mapStrctureNodeDetail = (Map) mlComparisonTableData.get(i);
                String strStructureNodeId = (String) mapStrctureNodeDetail.get(DomainConstants.SELECT_ID);
                String strStructureNodeName = (String) mapStrctureNodeDetail.get(DomainConstants.SELECT_NAME);
                String strStructureNodeRevision = (String) mapStrctureNodeDetail.get(DomainConstants.SELECT_REVISION);
                String strStructureNodeCurrent = (String) mapStrctureNodeDetail.get(DomainConstants.SELECT_CURRENT);
                String strStructureNodeDescription = (String) mapStrctureNodeDetail.get(DomainConstants.SELECT_DESCRIPTION);
                String strVariantAssemblyId = (String) mapStrctureNodeDetail.get("strVariantAssemblyId");
                String strProductConfiguration = (String) mapStrctureNodeDetail.get("strProductConfiguration");
                String strVariantAssemblyName = (String) mapStrctureNodeDetail.get("strVariantAssemblyName");
                String strXMLStructure = (String) mapStrctureNodeDetail.get("strXMLStructure");
                String encoded = XSSUtil.encodeForURL(context, strXMLStructure);
                MapList mlProductConfigurations = (MapList) mapStrctureNodeDetail.get("mlProductConfigurations");
                int ProductConfigurtionsCount = mlProductConfigurations.size();

                sbComparisonResult.append("<tr>");
                sbComparisonResult.append("<td>");
                sbComparisonResult.append(strStructureNodeName);
                sbComparisonResult.append("</td>");
                sbComparisonResult.append("<td>");
                sbComparisonResult.append(strStructureNodeRevision);
                sbComparisonResult.append("</td>");
                sbComparisonResult.append("<td>");
                sbComparisonResult.append(strStructureNodeCurrent);
                sbComparisonResult.append("</td>");
                sbComparisonResult.append("<td>");
                sbComparisonResult.append(strStructureNodeDescription);
                sbComparisonResult.append("</td>");
                sbComparisonResult.append("<td>");
                sbComparisonResult.append(strVariantAssemblyName);
                sbComparisonResult.append("</td>");

                StringBuffer sbProductMarketingName = new StringBuffer();
                StringBuffer sbProductConfig = new StringBuffer();
                StringBuffer sbVariantOptions = new StringBuffer();
                String strclassName = strStructureNodeId + "|" + strVariantAssemblyId + "|" + strProductConfiguration + "|" + encoded;
                for (int j = 0; j < ProductConfigurtionsCount; j++) {
                    sbProductMarketingName.append("Product Marketing Name :");
                    sbVariantOptions.append(((Map) mlProductConfigurations.get(j)).get(TigerConstants.SELECT_ATTRIBUTE_PSS_VARIANTOPTIONS) == null ? ""
                            : ((Map) mlProductConfigurations.get(j)).get(TigerConstants.SELECT_ATTRIBUTE_PSS_VARIANTOPTIONS));
                    sbVariantOptions.append("</br>");
                    String strProductID = (String) ((Map) mlProductConfigurations.get(j)).get(DomainConstants.SELECT_ID);
                    DomainObject domProductConfig = new DomainObject(strProductID);
                    StringList attributes = new StringList(1);
                    attributes.add(TigerConstants.SELECT_ATTRIBUTE_MARKETINGNAME);
                    Map product = domProductConfig.getRelatedObject(context, TigerConstants.RELATIONSHIP_FEATUREPRODUCTCONFIGURATION, false, attributes, DomainConstants.EMPTY_STRINGLIST);
                    if (!product.isEmpty()) {
                        sbProductMarketingName
                                .append((String) product.get(TigerConstants.SELECT_ATTRIBUTE_MARKETINGNAME) == null ? "" : (String) product.get(TigerConstants.SELECT_ATTRIBUTE_MARKETINGNAME));
                        sbProductMarketingName.append("</br>");
                    }

                    sbProductConfig.append(((Map) mlProductConfigurations.get(j)).get(DomainConstants.SELECT_NAME));
                    sbProductConfig.append(" : ");
                    sbProductConfig.append(((Map) mlProductConfigurations.get(j)).get(TigerConstants.SELECT_ATTRIBUTE_MARKETINGNAME) == null ? ""
                            : ((Map) mlProductConfigurations.get(j)).get(TigerConstants.SELECT_ATTRIBUTE_MARKETINGNAME));
                    sbProductConfig.append("</br>");

                }
                sbComparisonResult.append("<td>");
                sbComparisonResult.append(sbVariantOptions.toString());
                sbComparisonResult.append("</td>");
                sbComparisonResult.append("<td>");
                sbComparisonResult.append(sbProductMarketingName.toString());
                sbComparisonResult.append("</td>");
                sbComparisonResult.append("<td>");
                sbComparisonResult.append(sbProductConfig.toString());
                sbComparisonResult.append("</td>");
                // TIGTK-3306 : START
                String fieldNameStr = "CreateNewUpdate" + i;
                sbComparisonResult.append("<td>");
                sbComparisonResult.append("<input type='radio' name='");
                sbComparisonResult.append(fieldNameStr);
                sbComparisonResult.append("' value='Update'/>");
                sbComparisonResult.append("Update");
                sbComparisonResult.append("<input type='hidden' name='");
                sbComparisonResult.append(fieldNameStr + "Update_value");
                sbComparisonResult.append("' value='");
                sbComparisonResult.append(strclassName);
                sbComparisonResult.append("'/>");
                sbComparisonResult.append("</br>");
                // fieldNameStr = "CreateNew"+i;
                sbComparisonResult.append("<input type='radio' name='");
                sbComparisonResult.append(fieldNameStr);
                sbComparisonResult.append("' value='Create New'/>");
                sbComparisonResult.append("Create New");
                sbComparisonResult.append("<input type='hidden' name='");
                sbComparisonResult.append(fieldNameStr + "CreateNew_value");
                sbComparisonResult.append("' value='");
                sbComparisonResult.append(strclassName);
                sbComparisonResult.append("'/>");
                sbComparisonResult.append("</td>");
                sbComparisonResult.append("</tr>");
                // TIGTK-3306 : END
            }

        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getVariantAssemblyComparisonTableForUpdate: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END

        }

        return sbComparisonResult.toString();

    }

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author Asha Gholve
     */
    @SuppressWarnings("rawtypes")
    public int generateMassVariantAssemblies(Context context, String args[]) throws Exception {

        int iReturn = 0;

        String strMBOMId = args[0];

        StringList slObjectSelect = new StringList();
        slObjectSelect.add(DomainConstants.SELECT_ID);
        slObjectSelect.add(DomainConstants.SELECT_NAME);
        slObjectSelect.add(DomainConstants.SELECT_TYPE);

        StringList slRelSelect = new StringList();
        slRelSelect.add(DomainConstants.SELECT_RELATIONSHIP_ID);

        try {
            boolean skipExpandVA = false;
            if (args.length > 2) {
                skipExpandVA = true;
            }
            DomainObject domObjectMBOM = DomainObject.newInstance(context, strMBOMId);
            String[] ModelIds = (String[]) FRCMBOMModelerUtility.getListOfContextModelPIDs(context, strMBOMId);

            if (!skipExpandVA) {
                // Added for MBOM Stream Issue : TIGTK-4422 : Priyanka Salunke : 17-Feb-2017 : START
                StringBuilder sbVariantAssemblyExist = new StringBuilder("from[");
                sbVariantAssemblyExist.append(TigerConstants.RELATIONSHIP_PSS_PARTVARIANTASSEMBLY);
                sbVariantAssemblyExist.append("]");
                String selectVariantAssemblExist = sbVariantAssemblyExist.toString();
                StringList slObjectSelectLocal = new StringList(selectVariantAssemblExist);

                Pattern typePattern = new Pattern(TigerConstants.TYPE_CREATEASSEMBLY);
                typePattern.addPattern(TigerConstants.TYPE_CREATEKIT);
                typePattern.addPattern(TigerConstants.TYPE_CREATEMATERIAL);
                typePattern.addPattern(TigerConstants.TYPE_PROCESS_CONTINUOUS_CREATE_MATERIAL);
                typePattern.addPattern(TigerConstants.TYPE_PROCESSCONTINUOUSPROVIDE);

                Pattern relPattern = new Pattern(TigerConstants.RELATIONSHIP_DELFMIFUNCTIONIDENTIFIEDINSTANCE);
                relPattern.addPattern(TigerConstants.RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS);

                StringBuilder sbObjectWhereExpression = new StringBuilder("attribute[");
                sbObjectWhereExpression.append(TigerConstants.ATTRIBUTE_PSS_MBOM_STRUCTURENODE);
                sbObjectWhereExpression.append("] == Yes");
                MapList mlAssoaciatedVariantAssemblies = domObjectMBOM.getRelatedObjects(context, relPattern.getPattern(), // relPattern PSS_PartVariantAssembly
                        typePattern.getPattern(), // typePattern PSS_VariantAssembly
                        false, // getTo
                        true, // getFrom
                        0, // recursionLevel
                        slObjectSelectLocal, // objectSelects
                        DomainConstants.EMPTY_STRINGLIST, // relationshipSelects
                        sbObjectWhereExpression.toString(), // busWhereClause
                        DomainConstants.EMPTY_STRING, // relWhereClause
                        0, // limit
                        DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, null // postPatterns
                );

                if (mlAssoaciatedVariantAssemblies != null && !mlAssoaciatedVariantAssemblies.isEmpty()) {
                    int iCount = 0;
                    int iAssoaciatedVariantAssembliesSize = mlAssoaciatedVariantAssemblies.size();
                    for (int iSN = 0; iSN < iAssoaciatedVariantAssembliesSize; iSN++) {
                        Map mAssociatedVariantAssembliesMap = (Map) mlAssoaciatedVariantAssemblies.get(iSN);
                        String strVariantAssemblyIsConnected = (String) mAssociatedVariantAssembliesMap.get(selectVariantAssemblExist);
                        // If found MBOM object with S/N value Yes and do not have Connected Variant Assembly
                        if (strVariantAssemblyIsConnected.equalsIgnoreCase("False")) {
                            iCount++;
                        }
                    }

                    if (iCount == 0) {
                        return 2;
                    }
                } else {
                    String strRootStrutureNode = domObjectMBOM.getInfo(context, selectVariantAssemblExist);
                    if (strRootStrutureNode.equalsIgnoreCase("True")) {
                        return 2;
                    }
                }
                // Added for MBOM Stream Issue : TIGTK-4422 : Priyanka Salunke : 17-Feb-2017 : END
            }

            for (int i = 0; i < ModelIds.length; i++) {
                DomainObject model = new DomainObject(ModelIds[i]);
                String Product_id = model.getInfo(context, TigerConstants.SELECT_RELATIONSHIP_MAINPRODUCT);

                String strProdConfgObjName = DomainObject.EMPTY_STRING;

                DomainObject domproduct = new DomainObject(Product_id);
                MapList mlProductConfigurations = domproduct.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PRODUCT_CONFIGURATION, TigerConstants.TYPE_PRODUCTCONFIGURATION, false, true, 1,
                        slObjectSelect, DomainConstants.EMPTY_STRINGLIST, "", "", 0, "", "", null);

                if (mlProductConfigurations.size() > 0) {
                    for (int j = 0; j < mlProductConfigurations.size(); j++) {
                        Map mpObj = (Map) mlProductConfigurations.get(j);
                        strProdConfgObjName = (String) mpObj.get(DomainConstants.SELECT_NAME);

                        String[] args1 = new String[2];
                        if (args.length > 2) {
                            args1 = new String[4];
                            args1[2] = "true";
                            args1[3] = args[2];
                        }
                        args1[0] = strMBOMId;
                        args1[1] = strProdConfgObjName;

                        compareAndGenerateVariantAssembly(context, args1);
                    }
                } else {
                    String notice = EnoviaResourceBundle.getProperty(context, "emxConfigurationStringResource", context.getLocale(), "PSS_emxConfiguration.AlertMessage.ProductConfigurationNotFound");
                    MqlUtil.mqlCommand(context, "notice $1", notice);
                    throw new Exception(notice);
                }
            }
        } catch (Exception e) {
            iReturn = 1;
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in generateMassVariantAssemblies: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }

        return iReturn;
    }

    /**
     * @param context
     * @param args
     * @throws Exception
     * @author Asha Gholve
     */
    @SuppressWarnings("rawtypes")
    public void reviseVariantAssemblies(Context context, String args[]) throws Exception {

        if (args.length < 2) {
            throw new IllegalArgumentException("At least two arguments required. ObjectId and NewObjectId");
        }

        String strPartObjectId = args[0];
        String strNewPartObjectId = args[1];
        StringList slObjectSelect = new StringList();
        slObjectSelect.add(DomainConstants.SELECT_ID);
        slObjectSelect.add(DomainConstants.SELECT_NAME);
        StringList relSelects = new StringList();
        relSelects.add(DomainRelationship.SELECT_ID);
        relSelects.add("attribute[" + TigerConstants.ATTRIBUTE_V_EFFECTIVITYCOMPILEDFORM + "]");
        relSelects.add("attribute[" + TigerConstants.ATTRIBUTE_V_HASCONFIGURATIONEFFECTIVITY + "]");
        try {
            DomainObject domMBOMObject = DomainObject.newInstance(context, strPartObjectId);
            DomainObject domNewMBOMObject = DomainObject.newInstance(context, strNewPartObjectId);
            MapList mlVariantAssemblies = domMBOMObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_PARTVARIANTASSEMBLY, TigerConstants.TYPE_PSS_MBOM_VARIANT_ASSEMBLY, slObjectSelect,
                    null, false, true, (short) 1, "", "", 0);
            String strVarAsmblyObjId = DomainObject.EMPTY_STRING;
            String[] strRevisedVariantAssemblyArray = new String[mlVariantAssemblies.size()];
            RelationshipType relType = new RelationshipType(TigerConstants.RELATIONSHIP_PSS_PARTVARIANTASSEMBLY);
            String strCurrentUser = PropertyUtil.getRPEValue(context, MBOMREVISEACTION, false);
            if (mlVariantAssemblies.size() > 0) {
                for (int i = 0; i < mlVariantAssemblies.size(); i++) {
                    Map mpObj = (Map) mlVariantAssemblies.get(i);
                    strVarAsmblyObjId = (String) mpObj.get(DomainConstants.SELECT_ID);
                    String strRevisedObjectId = DomainObject.EMPTY_STRING;
                    DomainObject domObjectVarAsmbly = DomainObject.newInstance(context, strVarAsmblyObjId);
                    matrix.db.BusinessObject busRevisedObject = domObjectVarAsmbly.reviseObject(context, true);
                    strRevisedObjectId = busRevisedObject.getObjectId(context);

                    if (UIUtil.isNotNullAndNotEmpty(strCurrentUser)) {
                        if (UIUtil.isNotNullAndNotEmpty(strRevisedObjectId)) {
                            DomainObject dObj = DomainObject.newInstance(context, strRevisedObjectId);
                            dObj.setOwner(context, strCurrentUser);
                        }
                    }

                    strRevisedVariantAssemblyArray[i] = strRevisedObjectId;
                    // TIGTK-6366 :Migration - Variant Assembly Revision Format: By AG: Starts
                    MapList relatedPCS = domObjectVarAsmbly.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_VARIANT_ASSEMBLY_PRODUCT_CONFIGURATION, "*", slObjectSelect, relSelects, false,
                            true, (short) 0, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, 0);

                    if (relatedPCS != null)
                        for (int j = 0; j < relatedPCS.size(); j++) {
                            Map PCObject = (Map) relatedPCS.get(j);
                            String[] strParamArray = new String[2];
                            strParamArray[0] = strNewPartObjectId;
                            strParamArray[1] = (String) PCObject.get(DomainConstants.SELECT_ID);

                            String strPCRelatedOptions = getPCRelatedOptionsForPart(context, strParamArray);

                            DomainRelationship.setAttributeValue(context, (String) PCObject.get(DomainRelationship.SELECT_ID), TigerConstants.ATTRIBUTE_PSS_VARIANTOPTIONS, strPCRelatedOptions);

                            MapList mlStructureNode = getVPMStructureMQL(context, strNewPartObjectId, slObjectSelect, relSelects, (short) 0, (String) PCObject.get(DomainConstants.SELECT_ID),
                                    DomainConstants.EMPTY_STRING);
                            String strXMLStructure = prepareXMLStructure(context, strNewPartObjectId, mlStructureNode);
                            busRevisedObject.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_XMLSTRUCTURE, strXMLStructure);
                        }
                    // TIGTK-6366 :Migration - Variant Assembly Revision Format: By AG: END
                }
                domNewMBOMObject.addRelatedObjects(context, relType, true, strRevisedVariantAssemblyArray);
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in reviseVariantAssemblies: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    // TIGTK-6366 :Migration - Variant Assembly Revision Format: By AG: Starts
    public void updateVariantAssemblyXMLStructure(Context context, String[] args) throws Exception {

        StringList slObjectSelect = new StringList();
        slObjectSelect.add(DomainConstants.SELECT_ID);
        StringList relSelects = new StringList();
        relSelects.add(DomainRelationship.SELECT_ID);
        relSelects.add("attribute[" + TigerConstants.ATTRIBUTE_V_EFFECTIVITYCOMPILEDFORM + "]");
        relSelects.add("attribute[" + TigerConstants.ATTRIBUTE_V_HASCONFIGURATIONEFFECTIVITY + "]");
        String strpartID = args[0];
        // DomainObject domRevisedPartObject = new DomainObject(strpartID);
        String strConfigurationid = args[2];
        String strVariantAssemblyId = args[1];
        DomainObject domVariantObj = new DomainObject(strVariantAssemblyId);

        MapList mlStructureNode = getVPMStructureMQL(context, strpartID, slObjectSelect, relSelects, (short) 0, strConfigurationid, DomainConstants.EMPTY_STRING);
        String strXMLStructure = prepareXMLStructure(context, strpartID, mlStructureNode);
        domVariantObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_XMLSTRUCTURE, strXMLStructure);

    }

    public static String getMBOMRelationshipPattern(String typeStr) {
        String relationshipPattern = TigerConstants.RELATIONSHIP_DELFMIFUNCTIONIDENTIFIEDINSTANCE;
        if (TigerConstants.LIST_TYPE_MATERIALS.contains(typeStr)) {
            relationshipPattern = TigerConstants.RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS;
        }
        return relationshipPattern;
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
     * ALM-4190
     * @param context
     * @param domCurPartObj
     * @param strProductConfigurationId
     * @param varIDMBOM
     * @param conID
     * @throws Exception
     */
    public static void disconnectVariantAssemblyWithHarmonyAssociationCleanUp(Context context, DomainObject domCurPartObj, String varIDMBOM, String conID) throws Exception {
        DomainRelationship dVariantRelId = DomainRelationship.newInstance(context, conID);
        if (domCurPartObj == null) {
            BusinessObject bObj = dVariantRelId.getFrom();
            String strMBOMObjId = bObj.getObjectId(context);
            domCurPartObj = DomainObject.newInstance(context, strMBOMObjId);
        }
        if (UIUtil.isNullOrEmpty(varIDMBOM)) {
            BusinessObject bObj = dVariantRelId.getTo();
            String strVariantObjId = bObj.getObjectId(context);
            DomainObject dVariantObj = DomainObject.newInstance(context, strVariantObjId);
            varIDMBOM = dVariantObj.getInfo(context, TigerConstants.SELECT_PHYSICALID);
        }
        if (UIUtil.isNotNullAndNotEmpty(conID)) {
            DomainRelationship.disconnect(context, conID);

            StringList lsAttributeList = new StringList();
            lsAttributeList.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_PRODUCT_CONFIGURATION_PID + "]");
            lsAttributeList.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_VARIANTASSEMBLY_PID + "]");
            StringList strHarmonyAssociationRelId = domCurPartObj.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_HARMONYASSOCIATION + "].id");
            for (int q = 0; q < strHarmonyAssociationRelId.size(); q++) {
                String strHarmonyRelId = (String) strHarmonyAssociationRelId.get(q);
                DomainRelationship dHarmonyRelId = new DomainRelationship(strHarmonyRelId);
                MapList mlRelInfo = DomainRelationship.getInfo(context, new String[] { strHarmonyRelId }, lsAttributeList);
                Map mHarmonyAssociationRelInfo = (Map) mlRelInfo.get(0);
                String strVariantPIDOnHarmonyAssociation = (String) mHarmonyAssociationRelInfo.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_VARIANTASSEMBLY_PID + "]");
                if (strVariantPIDOnHarmonyAssociation.equalsIgnoreCase(varIDMBOM)) {
                    dHarmonyRelId.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_VARIANTASSEMBLY_PID, DomainConstants.EMPTY_STRING);
                }
            }
            String strMBOMRelId = DomainConstants.EMPTY_STRING;
            if (domCurPartObj.isKindOf(context, TigerConstants.TYPE_PROCESS_CONTINUOUS_CREATE_MATERIAL) || domCurPartObj.isKindOf(context, TigerConstants.TYPE_PROCESSCONTINUOUSPROVIDE)) {
                strMBOMRelId = (String) domCurPartObj.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS + "].id");
            } else {
                strMBOMRelId = (String) domCurPartObj.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_DELFMIFUNCTIONIDENTIFIEDINSTANCE + "].id");
            }
            if (UIUtil.isNotNullAndNotEmpty(strMBOMRelId)) {
                StringList slSelects = new StringList();
                slSelects.add(TigerConstants.SELECT_RELATIONSHIP_HARMONY_ASSOCIATION_ID);
                MapList slConnectedHarmonyList = DomainRelationship.getInfo(context, new String[] { strMBOMRelId }, slSelects);
                if (slConnectedHarmonyList != null && !slConnectedHarmonyList.isEmpty()) {
                    for (int y = 0; y < slConnectedHarmonyList.size(); y++) {
                        Map mTempMap = (Map) slConnectedHarmonyList.get(y);
                        StringList slAssociationId = pss.mbom.MBOMUtil_mxJPO.getStringListValue(mTempMap.get("frommid[PSS_HarmonyAssociation].id"));
                        if (slAssociationId != null && slAssociationId.size() > 0) {
                            for (int b = 0; b < slAssociationId.size(); b++) {
                                String strAssocitaionId = (String) slAssociationId.get(b);
                                DomainRelationship dHarmonyRelId = new DomainRelationship(strAssocitaionId);
                                MapList mlRelInfo = DomainRelationship.getInfo(context, new String[] { strAssocitaionId }, lsAttributeList);
                                Map mHarmonyAssociationRelInfo = (Map) mlRelInfo.get(0);
                                String strVariantPIDOnHarmonyAssociation = (String) mHarmonyAssociationRelInfo.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_VARIANTASSEMBLY_PID + "]");
                                if (strVariantPIDOnHarmonyAssociation.equalsIgnoreCase(varIDMBOM)) {
                                    dHarmonyRelId.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_VARIANTASSEMBLY_PID, DomainConstants.EMPTY_STRING);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * This method is used to get Variant Assembly Id Column value for Product Configuration Filtering.
     * @param context
     *            , args
     * @throws Exception
     * @since 12-10-2017
     */
    public Vector displayVariantIDForPCFiltering(Context context, String[] args) throws Exception {
        logger.error("pss.MBOM.StructureNodeUtil : displayVariantIDForPCFiltering() : START ");
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            Map paramList = (Map) programMap.get("paramList");
            MapList mlProductConfigurationList = (MapList) programMap.get("objectList");
            Vector vVariantAssemblyIds = new Vector(mlProductConfigurationList.size());

            // Get Parent Part object id
            String strParentPartId = (String) paramList.get("parentOID");
            if (UIUtil.isNotNullAndNotEmpty(strParentPartId)) {
                DomainObject domParentPart = DomainObject.newInstance(context, strParentPartId);
                String strParentStructureNodeValue = domParentPart.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MBOM_STRUCTURENODE);
                if (strParentStructureNodeValue.equalsIgnoreCase("Yes")) {
                    if (!mlProductConfigurationList.isEmpty()) {
                        // Argument array
                        String[] strParamArray = new String[3];
                        // Root Part Id
                        strParamArray[0] = strParentPartId;
                        strParamArray[2] = DomainConstants.SELECT_NAME;

                        // Iterate Structure Node.
                        for (int i = 0; i < mlProductConfigurationList.size(); i++) {
                            Map mPCInfoMap = (Map) mlProductConfigurationList.get(i);
                            String strProductConfigurationObjectId = (String) mPCInfoMap.get(DomainConstants.SELECT_ID);
                            // Product Configuration Id
                            strParamArray[1] = strProductConfigurationObjectId;

                            String strVariantId = getVariantAssemblyID(context, strParamArray);

                            if (UIUtil.isNotNullAndNotEmpty(strVariantId)) {
                                vVariantAssemblyIds.add(strVariantId);
                            } else {
                                vVariantAssemblyIds.add("");
                            }

                        } // for end

                    } // PC map list empty check if end

                } // Parent Part S/N value check if end

            } // Part id null check if end
            logger.error("pss.mbom.StructureNodeUtil : displayVariantIDForPCFiltering() : END ");
            return vVariantAssemblyIds;
        } catch (Exception e) {
            logger.error("pss.mbom.StructureNodeUtil : displayVariantIDForPCFiltering() : ERROR ", e.toString());
            throw e;
        }
    }

    /**
     * This method is used on the Table Column that displays already applied Product Configuration with marked X.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @returns StringList of X
     * @throws Exception
     *             if the operation fails
     */

    // Method displayAppliedProductConfiguration - Ends
    public StringList displayAppliedProductConfiguration(Context context, String[] args) throws Exception {
        StringList slProductConfigurationresult = new StringList();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap paramMap = (HashMap) programMap.get("paramList");
            String strAppliedProductConfigurationId = (String) paramMap.get("FRCExpressionFilterInput_OID");
            MapList mlObjList = (MapList) programMap.get("objectList");
            for (int i = 0; i < mlObjList.size(); i++) {
                Map mPcObj = (Map) mlObjList.get(i);
                String strPcId = (String) mPcObj.get(DomainConstants.SELECT_ID);
                if (strPcId.equals(strAppliedProductConfigurationId)) {
                    slProductConfigurationresult.add("X");
                } else {
                    slProductConfigurationresult.add("");
                }
            }

        } catch (Exception e) {
            // TIGTK-5405 - 03-04-2017 - VP - START
            logger.error("Error in displayAppliedProductConfiguration: ", e);
            // TIGTK-5405 - 03-04-2017 - VP - END
            throw e;
        }
        return slProductConfigurationresult;
    }

}