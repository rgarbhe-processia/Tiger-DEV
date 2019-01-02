
/*
 ** IEFEBOMSyncFindMatchingPartBase
 **
 ** Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 **
 */

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.IEFEBOMConfigObject;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADUtil;
import com.matrixone.apps.common.Part;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.UOMUtil;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.Attribute;
import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.Policy;
import matrix.util.StringList;
import pss.constants.TigerConstants;

public class IEFEBOMSyncFindMatchingPartBase_mxJPO {
    protected String MATCHING_PART_RULE = "LATEST_REV";

    protected String PART_RELEASE_STATE = "Complete";

    protected final String MATCH_CADMODEL_REV = "MATCH_CADMODEL_REV";

    protected boolean confAttrFailAtMissingPart = false;

    String sVersionOfRelName = "";

    String SELECT_ON_MAJOR = "";

    MCADMxUtil mxUtil = null;

    MCADServerResourceBundle serverResourceBundle = null;

    IEFGlobalCache cache = null;

    protected String ATTR_PART_TYPE = "";

    protected String LOCAL_CONFIG_TYPE = "";

    IEFEBOMConfigObject ebomConfObject = null;

    Hashtable cadAttrTable = null;

    HashMap policySequenceMap = new HashMap();

    // Constants defined for "PSS_Geometry Type" attribute check
    public final String ATTR_RANGE_PSSGEOMETRYTYPE = "MG";

    // End of Constants Definition

    public IEFEBOMSyncFindMatchingPartBase_mxJPO() {
    }

    public IEFEBOMSyncFindMatchingPartBase_mxJPO(Context context, String[] args) throws Exception {
        if (!context.isConnected())
            MCADServerException.createException("not supported no desktop client", null);

        if (args.length == 4) {
            String ebomRegType = args[0];
            String ebomRegName = args[1];
            String ebomRegRev = args[2];
            String language = args[3];

            ebomConfObject = new IEFEBOMConfigObject(context, ebomRegType, ebomRegName, ebomRegRev);
            serverResourceBundle = new MCADServerResourceBundle(language);
            cache = new IEFGlobalCache();
            mxUtil = new MCADMxUtil(context, serverResourceBundle, cache);
            sVersionOfRelName = MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");
            SELECT_ON_MAJOR = "from[" + sVersionOfRelName + "].to.";

            confAttrFailAtMissingPart = "true".equalsIgnoreCase(ebomConfObject.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_FAIL_ON_NOT_FINDING_PART));
        }
    }

    public int mxMain(Context context, String[] args) throws Exception {
        return 0;
    }

    public BusinessObject findMatchingPart(Context context, String[] args) throws Exception {
        String cadObjectId = args[0];
        String partObjName = args[1];
        String instanceName = args[2];
        String ebomRegType = args[3];
        String ebomRegName = args[4];
        String ebomRegRev = args[5];
        String language = args[6];
        // /String isMinorType = args[7]; //[NDM]
        String famID = args[7];

        ebomConfObject = new IEFEBOMConfigObject(context, ebomRegType, ebomRegName, ebomRegRev);
        serverResourceBundle = new MCADServerResourceBundle(language);
        cache = new IEFGlobalCache();
        mxUtil = new MCADMxUtil(context, serverResourceBundle, cache);

        sVersionOfRelName = MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");
        SELECT_ON_MAJOR = "from[" + sVersionOfRelName + "].to.";

        MATCHING_PART_RULE = ebomConfObject.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_PART_MATCHING_RULE);
        PART_RELEASE_STATE = ebomConfObject.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_RELEASE_PART_STATE);

        ATTR_PART_TYPE = MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-EBOMSync-PartTypeAttribute");
        confAttrFailAtMissingPart = "true".equalsIgnoreCase(ebomConfObject.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_FAIL_ON_NOT_FINDING_PART));

        if (null != famID && !"".equals(famID)) {
            String ebomExpositionMode = mxUtil.getAttributeForBO(context, famID, MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-EBOMExpositionMode"));
            if (null != ebomExpositionMode && "single".equalsIgnoreCase(ebomExpositionMode)) {
                cadObjectId = famID;
            }
        }

        StringList busSelects = new StringList();
        busSelects.add("type");
        busSelects.add("name");
        busSelects.add("revision");
        busSelects.add("attribute[" + ATTR_PART_TYPE + "]");
        busSelects.add("policy");
        busSelects.add("policy.revision");

        // [NDM]
        /*
         * busSelects.add(SELECT_ON_MAJOR + "revision"); busSelects.add(SELECT_ON_MAJOR + "policy"); busSelects.add(SELECT_ON_MAJOR + "policy.revision");
         */
        // [NDM]
        String[] cadids = new String[1];
        cadids[0] = cadObjectId;

        BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, cadids, busSelects);

        BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect) buslWithSelectionList.elementAt(0);

        BusinessObject cadObject = new BusinessObject(cadObjectId);
        cadObject.open(context);

        String cadType = busObjectWithSelect.getSelectData("type");
        String cadModelRevision = busObjectWithSelect.getSelectData("revision");
        String cadPolicyName = busObjectWithSelect.getSelectData("policy");
        String cadModelPolicyRevSeq = busObjectWithSelect.getSelectData("policy.revision");
        String partType = busObjectWithSelect.getSelectData("attribute[" + ATTR_PART_TYPE + "]");
        // [NDM]
        /*
         * if(MATCHING_PART_RULE.equals(MATCH_CADMODEL_REV) && isMinorType.equalsIgnoreCase("true")) { cadModelRevision = busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "revision"); cadPolicyName
         * = busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "policy"); cadModelPolicyRevSeq = busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "policy.revision"); }
         */
        // [NDM]
        if (partType.equals(""))
            partType = ebomConfObject.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_DEFAULT_NEW_PART_TYPE);

        if (partType == null || partType.equals(""))
            MCADServerException.createException(serverResourceBundle.getString("mcadIntegration.Server.Message.partTypeNotSpecified"), null);

        BusinessObject partObject = getPartRevisionForRule(context, partType, partObjName, MATCHING_PART_RULE, cadPolicyName, cadModelPolicyRevSeq, cadModelRevision, cadType);

        if (partObject != null && !isPartReleased(context, partObject.getObjectId(context))) {
            copyAttribsFromCadObjToPart(context, partObject, cadObject, instanceName, cadType, partType);
        }

        cadObject.close(context);

        return partObject;
    }

    public void transferCadAttribsToPart(Context context, String[] args) throws Exception {
        String cadObjectId = args[0];
        String partObjId = args[1];
        String instanceName = args[2];

        BusinessObject cadObject = new BusinessObject(cadObjectId);
        cadObject.open(context);
        String cadType = cadObject.getTypeName();

        if (args.length == 5) {
            MCADServerGeneralUtil serverGeneralUtil = null;
            MCADGlobalConfigObject globalConfigObject = null;
            BusinessObject activeMinorObject = null;
            String famID = null;
            String ebomExpositionMode = null;
            String cadObjectType = mxUtil.getCADTypeForBO(context, cadObject);

            String[] packedGCO = new String[2];

            packedGCO[0] = args[3];
            packedGCO[1] = args[4];

            globalConfigObject = (MCADGlobalConfigObject) JPO.unpackArgs(packedGCO);

            serverGeneralUtil = new MCADServerGeneralUtil(context, globalConfigObject, serverResourceBundle, cache);

            if (globalConfigObject != null && globalConfigObject.isTypeOfClass(cadObjectType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE)) {
                // [NDM] H68
                // if(serverGeneralUtil.isBusObjectFinalized(context, cadObject))
                // {
                famID = serverGeneralUtil.getTopLevelFamilyObjectForInstance(context, cadObject.getObjectId());
                // }
                // else
                // {
                // activeMinorObject = mxUtil.getActiveMinor(context, cadObject);
                // famID = serverGeneralUtil.getTopLevelFamilyObjectForInstance(context,activeMinorObject.getObjectId());
                // }

                if (null != famID) {
                    ebomExpositionMode = mxUtil.getAttributeForBO(context, famID, MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-EBOMExpositionMode"));

                    if ("single".equalsIgnoreCase(ebomExpositionMode)) {
                        cadObject.close(context);
                        cadObject = new BusinessObject(famID);
                        cadObject.open(context);
                        cadType = cadObject.getTypeName();
                    }
                }
            }
        }

        BusinessObject partObject = new BusinessObject(partObjId);
        partObject.open(context);
        String partType = partObject.getTypeName();
        partObject.close(context);

        copyAttribsFromCadObjToPart(context, partObject, cadObject, instanceName, cadType, partType);

        cadObject.close(context);
    }

    public void copyAttribsFromCadObjToPart(Context context, BusinessObject partObject, BusinessObject cadObject, String instanceName, String cadType, String partType) throws Exception {
        Hashtable cadAttrTable = getAttributeMap(context, ebomConfObject, cadObject, instanceName, cadType, partType, partObject);
        Part partObj = new Part(partObject);
        String strCADObjectId = cadObject.getObjectId(context);
        String strMassUnit = UOMUtil.getInputunit(context, strCADObjectId, TigerConstants.ATTRIBUTE_PSS_CADMass);
        String strCADMass = UOMUtil.getInputValue(context, strCADObjectId, TigerConstants.ATTRIBUTE_PSS_CADMass);
        String strNewMassUnitValue = strCADMass + " " + strMassUnit;

        if (cadAttrTable.containsKey("PSS_EBOM_CADMass")) {
            cadAttrTable.put("PSS_EBOM_CADMass", strNewMassUnitValue);
        }
        if (cadAttrTable.containsKey("PSS_EBOM_Mass2")) {
            cadAttrTable.put("PSS_EBOM_Mass2", strNewMassUnitValue);
        }
        partObj.openObject(context);
        cadAttrTable = setSystemAttributeValues(context, partObj, cadAttrTable);
        partObj.setAttributeValues(context, cadAttrTable);
        partObj.closeObject(context, true);
    }

    protected BusinessObject getPartRevisionForRule(Context context, String partTypeName, String partName, String matchingRule, String cadPolicyName, String cadModelPolicyRevSeq,
            String cadModelRevision, String cadType) throws MCADException {
        String Args[] = new String[5];
        Args[0] = partTypeName;
        Args[1] = partName;
        Args[2] = "*";
        Args[3] = "revisions";
        Args[4] = "|";

        BusinessObject retBus = null;

        try {
            String sResult = mxUtil.executeMQL(context, "temp query bus $1 $2 $3 select $4 dump $5", Args);
            if (sResult.startsWith("true")) {
                sResult = sResult.substring(5);
                StringTokenizer strtok1 = new StringTokenizer(sResult, "\n");
                if (strtok1.hasMoreTokens()) {
                    StringTokenizer strtok2 = new StringTokenizer(strtok1.nextToken(), "|");
                    String sType = strtok2.nextToken();
                    String sName = strtok2.nextToken();
                    String sRev = "";

                    Vector revList = new Vector();
                    while (strtok2.hasMoreTokens()) {
                        revList.addElement(strtok2.nextToken());
                    }

                    String latestRevision = "";
                    for (int i = revList.size() - 1; i > 0; i--) {
                        String currRev = (String) revList.elementAt(i);
                        Args = new String[6];
                        Args[0] = sType;
                        Args[1] = sName;
                        Args[2] = currRev;
                        Args[3] = "current";
                        Args[4] = "policy.property[PolicyClassification].value";
                        Args[5] = "|";
                        String indvlResult = mxUtil.executeMQL(context, "print bus $1 $2 $3 select $4 $5 dump $6", Args);

                        if (indvlResult.startsWith("true")) {
                            StringTokenizer indvlTok = new StringTokenizer(indvlResult.substring(5), "|");
                            String classification = "";
                            String state = "";
                            if (indvlTok.hasMoreTokens())
                                state = indvlTok.nextToken();
                            if (indvlTok.hasMoreTokens())
                                classification = indvlTok.nextToken();

                            if (!"Equivalent".equals(classification)) {
                                if (latestRevision.equals(""))
                                    latestRevision = currRev;

                                if (matchingRule.equals("LATEST_REV")) {
                                    sRev = currRev;
                                    break;
                                } else if (matchingRule.equals(MATCH_CADMODEL_REV) && currRev.equals(cadModelRevision)) {
                                    sRev = currRev;
                                    break;
                                }
                            }

                        }
                    }

                    if (!"".equals(sRev)) {
                        retBus = new BusinessObject(sType, sName, sRev, "");
                        retBus.open(context);
                        Policy partPolicyObject = retBus.getPolicy(context);
                        partPolicyObject.open(context);
                        String partPolicyName = partPolicyObject.getName();
                        String sPolicyConfiguredPart = MCADMxUtil.getActualNameForAEFData(context, "policy_ConfiguredPart");

                        if (sPolicyConfiguredPart != null && sPolicyConfiguredPart.equals(partPolicyName)) {
                            partPolicyObject.close(context);
                            retBus.close(context);

                            Hashtable messageDetails = new Hashtable(4);
                            messageDetails.put("TYPE", sType);
                            messageDetails.put("NAME", sName);
                            messageDetails.put("REV", sRev);
                            messageDetails.put("PARTPOLICY", partPolicyName);
                            String message = serverResourceBundle.getString("mcadIntegration.Server.Message.EBOMSyncNotSupportedForPolicy", messageDetails);

                            MCADServerException.createException(message, null);
                        }

                        if (matchingRule.equals(MATCH_CADMODEL_REV)) {
                            String partRevSequence = "";
                            if (policySequenceMap.containsKey(partPolicyName)) {
                                partRevSequence = (String) policySequenceMap.get(partPolicyName);
                            } else {
                                partRevSequence = partPolicyObject.getSequence();
                                policySequenceMap.put(partPolicyName, partRevSequence);
                            }

                            partPolicyObject.close(context);

                            if (!cadModelPolicyRevSeq.equals(partRevSequence)) {
                                Hashtable messageDetails = new Hashtable(3);
                                messageDetails.put("CADPOLICY", cadPolicyName);
                                messageDetails.put("PARTPOLICY", partPolicyName);

                                String message = serverResourceBundle.getString("mcadIntegration.Server.Message.RevisionSequenceCADandDevMisMatch", messageDetails);

                                MCADServerException.createException(message, null);
                            }

                            if (isPartReleased(context, retBus.getObjectId(context))) {
                                Hashtable messageDetails = new Hashtable(3);
                                messageDetails.put("NAME", sName);
                                messageDetails.put("CADMODELTYPE", sType);

                                String message = serverResourceBundle.getString("mcadIntegration.Server.Message.partWithMatchRevAlreadyReleased", messageDetails);

                                MCADServerException.createException(message, null);
                            }

                        }
                        retBus.close(context);
                    } else if (matchingRule.equals(MATCH_CADMODEL_REV) && !"".equals(latestRevision)) {
                        BusinessObject partObject = new BusinessObject(sType, sName, latestRevision, "");
                        // move logic for throwing error outside this method
                        retBus = getPartRevisionIfReleased(context, partObject.getObjectId(context), cadPolicyName, cadModelPolicyRevSeq, cadModelRevision, cadType);
                    }
                }
            }
        } catch (Exception ex) {
            MCADServerException.createException(ex.getMessage(), ex);
        }

        return retBus;
    }

    protected BusinessObject getPartRevisionIfReleased(Context context, String partObjectId, String cadPolicyName, String cadModelPolicyRevSeq, String targetRevision, String sType) throws Exception {
        String revisedPartId = partObjectId;
        BusinessObject partObject = new BusinessObject(partObjectId);
        partObject.open(context);

        String sName = partObject.getName();
        if (isPartReleased(context, partObjectId)) {
            BusinessObject revisedPartObject = partObject.revise(context, targetRevision, partObject.getVault());
            revisedPartId = revisedPartObject.getObjectId(context);

            partObject.close(context);
            return new BusinessObject(revisedPartId);
        } else if (confAttrFailAtMissingPart) {
            Policy partPolicyObject = partObject.getPolicy(context);
            partPolicyObject.open(context);
            String partPolicyName = partPolicyObject.getName();
            String partRevSequence = "";

            if (policySequenceMap.containsKey(partPolicyName)) {
                partRevSequence = (String) policySequenceMap.get(partPolicyName);
            } else {
                partRevSequence = partPolicyObject.getSequence();
                policySequenceMap.put(partPolicyName, partRevSequence);
            }

            partPolicyObject.close(context);

            if (!cadModelPolicyRevSeq.equals(partRevSequence)) {
                Hashtable messageDetails = new Hashtable(3);
                messageDetails.put("CADPOLICY", cadPolicyName);
                messageDetails.put("PARTPOLICY", partPolicyName);

                String message = serverResourceBundle.getString("mcadIntegration.Server.Message.RevisionSequenceCADandDevMisMatch", messageDetails);

                MCADServerException.createException(message, null);
            }

            // Latest revision without matching revision
            Hashtable messageDetails = new Hashtable(3);
            messageDetails.put("NAME", sName);
            messageDetails.put("CADMODELTYPE", sType);

            String message = serverResourceBundle.getString("mcadIntegration.Server.Message.partRevAndCADModelRevMismatch", messageDetails);

            MCADServerException.createException(message, null);

            partObject.close(context);
            return new BusinessObject(revisedPartId);
        } else {
            partObject.close(context);
            return null;
        }
    }

    protected boolean isPartReleased(Context context, String partObjectId) {
        boolean bReleased = false;
        String Args[] = new String[3];
        Args[0] = partObjectId;
        Args[1] = "current";
        Args[2] = "|";
        String mqlCmdResult = mxUtil.executeMQL(context, "print bus $1 select $2 dump $3", Args);

        if (mqlCmdResult.startsWith("true")) {
            String currentState = mqlCmdResult.substring(5);

            String PART_RELEASE_STATE = ebomConfObject.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_RELEASE_PART_STATE);

            // If part is released, then revise the part
            if (PART_RELEASE_STATE.equals(currentState)) {
                bReleased = true;
            }
        }

        return bReleased;
    }

    /**
     * This method is used to get all the Attributes for Part(Attributes that get applied applied to Part based on "Part Family")
     * @param context
     * @param args
     * @param args0
     *            -- Instance of IEFEBOMConfigObject
     * @param args1
     *            -- "CAD Object" instance
     * @param args2
     *            -- Instance name
     * @param args3
     *            -- Type of CAD object
     * @param args3
     *            -- Type of Part object
     * @param args4
     *            -- Part Object instance
     * @return -- Hashtble -- Returns Hashtable instance "attrValueHash" which contains the "partAttrName" as key and "partAttrValue" as its value
     * @throws Exception
     */
    protected Hashtable getAttributeMap(Context context, IEFEBOMConfigObject ebomConfObj, BusinessObject cadObject, String instanceName, String cadType, String partType, BusinessObject Part)
            throws Exception {
        // Constants defined for "PSS_Geometry Type" attribute check
        final String ATTRIBUTE_PSS_GEOMETRYTYPE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_GeometryType");
        final String POLICY_EC_PART = PropertyUtil.getSchemaProperty(context, "policy_PSS_EC_Part");
        final String POLICY_DEVELOPMENT_PART = PropertyUtil.getSchemaProperty(context, "policy_PSS_Development_Part");
        final String STATE_PRELIMINARY = PropertyUtil.getSchemaProperty(context, "policy", POLICY_EC_PART, "state_Preliminary");
        final String STATE_CREATE = PropertyUtil.getSchemaProperty(context, "policy", POLICY_DEVELOPMENT_PART, "state_Create");
        final String SELECT_ATTRIBUTE_PSS_GEOMETRYTYPE = "attribute[" + ATTRIBUTE_PSS_GEOMETRYTYPE + "]";
        // End of Constants Definition

        Hashtable attrValueHash = new Hashtable();
        Hashtable mandAttrNamehash = new Hashtable();
        DomainObject domPartObject = DomainObject.newInstance(context, Part);
        Vector PartAttributeList = getPartAttributeList(context, domPartObject);

        // For Mandatory Object Attribute
        mandAttrNamehash = getMandTypeAttributeMapping(context, ebomConfObj, cadType, partType);
        String strMandAttrValue = ebomConfObj.getConfigAttributeValue(ebomConfObj.ATTR_MAND_OBJECT_ATTR_MAPPING);

        getAttrHashtable(context, attrValueHash, cadObject, mandAttrNamehash, partType, PartAttributeList, true);
        LOCAL_CONFIG_TYPE = MCADMxUtil.getActualNameForAEFData(context, "type_MCADInteg-LocalConfig");

        // Taking Pref Value From LCO
        String userName = context.getUser();
        String prefColElement = "";

        Hashtable localhash = new Hashtable();
        Hashtable localattrHashName = new Hashtable();

        String objectId = cadObject.getObjectId();
        String localConfigObjRev = MCADMxUtil.getConfigObjectRevision(context);
        BusinessObject localObj = new BusinessObject(LOCAL_CONFIG_TYPE, userName, localConfigObjRev, "");
        Attribute prefObjectValue = localObj.getAttributeValues(context, IEFEBOMConfigObject.ATTR_OBJECT_ATTR_MAPPING);
        String prefObjectMapping = prefObjectValue.getValue();

        DomainObject domCADObject = DomainObject.newInstance(context, objectId);
        StringList lstCADSelects = new StringList();
        lstCADSelects.add(DomainConstants.SELECT_NAME);
        lstCADSelects.add(SELECT_ATTRIBUTE_PSS_GEOMETRYTYPE);
        lstCADSelects.add(DomainConstants.SELECT_ID);
        Map CADMap = domCADObject.getInfo(context, lstCADSelects);
        String strCADName = (String) CADMap.get(DomainConstants.SELECT_NAME);

        // Code added to check whether Part is in "InWork" state and "CAD
        // Object" is of "MG" Geometry Type
        String cadObjectGeometryType = (String) CADMap.get(SELECT_ATTRIBUTE_PSS_GEOMETRYTYPE);

        StringList lstPartSelects = new StringList();
        lstPartSelects.add(DomainConstants.SELECT_CURRENT);
        lstPartSelects.add(DomainConstants.SELECT_NAME);

        Map PartMap = domPartObject.getInfo(context, lstPartSelects);
        String strPartName = (String) PartMap.get(DomainConstants.SELECT_NAME);
        String strPartCurrentState = (String) PartMap.get(DomainConstants.SELECT_CURRENT);

        /*
         * if (!(strCADName.equals(strPartName))) { return new Hashtable(); }
         */
        if (!cadObjectGeometryType.equals(ATTR_RANGE_PSSGEOMETRYTYPE) && !(strPartCurrentState.equals(STATE_PRELIMINARY) || strPartCurrentState.equals(STATE_CREATE))) {
            return new Hashtable();
        }
        // End of addition of code to check whether Part is in "InWork" state
        // and "CAD Object" is of "MG" Geometry Type

        String integrationName = mxUtil.getIntegrationName(context, objectId);
        StringTokenizer prefObjectMappingToken = new StringTokenizer(prefObjectMapping, "\n");
        while (prefObjectMappingToken.hasMoreElements()) {
            prefColElement = (String) prefObjectMappingToken.nextElement();
            int firstIndex = prefColElement.indexOf("|");
            String integName = prefColElement.substring(0, firstIndex);
            String prefValue = prefColElement.substring(firstIndex + 1, prefColElement.length());
            if (integName.equals(integrationName)) {
                if (prefValue != null && !prefValue.trim().equals("")) {
                    Enumeration objectStringValue1 = MCADUtil.getTokensFromString(prefValue.trim(), "@");
                    while (objectStringValue1.hasMoreElements()) {
                        String obj1 = (String) objectStringValue1.nextElement();
                        localhash.put(obj1, obj1);
                    }
                }
            }
        }
        // Formating Value for the EBOM SYNC
        String cadTypeName = "";
        String cadAttrName = "";
        String partTypeName = "";
        String partAttrName = "";
        Enumeration localEnum = localhash.keys();
        while (localEnum.hasMoreElements()) {
            String name = (String) localEnum.nextElement();
            String value = (String) localhash.get(name);
            StringTokenizer token = new StringTokenizer(value, "|");
            cadTypeName = (String) token.nextElement();
            cadAttrName = (String) token.nextElement();
            partTypeName = (String) token.nextElement();
            partAttrName = (String) token.nextElement();

            if (cadTypeName.equals(cadType))
                localattrHashName.put(cadAttrName, partAttrName);
        }

        // For Attribute Mapping From Local object
        // getAttrHashtable(context, attrValueHash, cadObject, localattrHashName, partType, PartAttributeList, false);
        getAttrHashtable(context, attrValueHash, cadObject, localattrHashName, partType, PartAttributeList, true);

        // CABBOM : TIGTK-10103 : 10/09/2017 : PTE : START
        Map<Integer, String> map = attrValueHash;
        Iterator<Map.Entry<Integer, String>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, String> entry = it.next();
            String strValue = entry.getValue();

            try {

                // Remove entry if value is null or equals 0.
                if (strValue == null || strValue.equals(DomainConstants.EMPTY_STRING) || strValue.length() == 0) {
                    it.remove();
                } else {
                    double dAttributeValue = Double.parseDouble(strValue);
                    double dAttributeFloorValue = Math.floor(dAttributeValue);
                    double dAttributeCeilValue = Math.ceil(dAttributeValue);

                    if (dAttributeFloorValue == 0.0 && dAttributeCeilValue == 0.0)
                        it.remove();
                }
            } catch (Exception ex) {

            }
        }
        // CABBOM : TIGTK-10103 : 10/09/2017 : PTE : END

        return attrValueHash;
    }

    /**
     * This method is used to get all the Attributes for Part(Attributes that get applied applied to Part based on "Part Family")
     * @param context
     * @param args
     * @param args0
     *            -- Hashtable instance "attrValueHash" which contains the "partAttrName" as key and "partAttrValue" as its value
     * @param args1
     *            -- "CAD Object" instance
     * @param args2
     *            -- Hashtable instance "testHash" which contains the Mandatory attribute mapping between "CAD Object" and Part
     * @param args3
     *            -- Type of Part object
     * @param args4
     *            -- "Attributes" list of Part object
     * @param args5
     *            -- Boolean parameter that checks if attributes exists on Part or not.
     * @return -- void -- Returns nothing
     * @throws Exception
     */
    private void getAttrHashtable(Context context, Hashtable attrValueHash, BusinessObject cadObject, Hashtable testHash, String partType, Vector partAttributeList,
            boolean checkIfAttributeExistsOnPart) throws Exception {
        Hashtable sysAttrValueHash = new Hashtable();
        boolean addAttributetoAttrMap = true;
        // TIGTK-8696 - START
        String partAttrName = DomainConstants.EMPTY_STRING;
        String attrValue = DomainConstants.EMPTY_STRING;
        // TIGTK-8696 - END
        // Vector attributeList = mxUtil.getAllAttributeNamesOnType(context,
        // partType);
        // TIGTK-8696 - START
        String partAttributeCalculation = EnoviaResourceBundle.getProperty(context, "emxIEFDesignCenter.mcadIntegration.EBOMSync.PartAttribute.calculation");
        // TIGTK-8696 - END
        StringList lstCalcAttrributeList = FrameworkUtil.split(partAttributeCalculation, "|");
        Iterator AttrCalculationListItr = lstCalcAttrributeList.iterator();
        HashMap CalculationMap = new HashMap();
        try {
            while (AttrCalculationListItr.hasNext()) {
                String strCurrentAttrCalcVal = (String) AttrCalculationListItr.next();
                StringList lstPartAttrributesListWithMultiplier = FrameworkUtil.split(strCurrentAttrCalcVal, ";");
                CalculationMap.put((String) lstPartAttrributesListWithMultiplier.get(0), (String) lstPartAttrributesListWithMultiplier.get(1));
            }
        } catch (Exception e) {
        }

        Vector attributeList = partAttributeList;
        Enumeration cadAttrEnum = testHash.keys();
        while (cadAttrEnum.hasMoreElements()) {
            try {
                String cadAttrName = (String) cadAttrEnum.nextElement();
                partAttrName = (String) testHash.get(cadAttrName);

                if (cadAttrName.startsWith("$$"))
                    attrValue = getSystemAttributeValues(context, cadObject, cadAttrName);
                else {
                    Attribute attrib = cadObject.getAttributeValues(context, cadAttrName);

                    if (attrib != null) {
                        attrValue = attrib.getValue();
                    }
                }

                // TIGTK-8696 - START
                if (partAttrName.startsWith("$$"))
                    sysAttrValueHash.put(partAttrName, attrValue);
                else {
                    StringList PartAttributeSyncList = new StringList(partAttrName);
                    if (partAttrName.indexOf(",") != -1) {
                        PartAttributeSyncList = new StringList(partAttrName);
                    }
                    Iterator PartAttrItr = PartAttributeSyncList.iterator();
                    while (PartAttrItr.hasNext()) {
                        String strPartAttrName = (String) PartAttrItr.next();
                        // TIGTK-8696 - START
                        StringList slSplitPartAttrNameList = FrameworkUtil.split(strPartAttrName, ",");
                        Iterator itrSplitPartName = slSplitPartAttrNameList.iterator();
                        while (itrSplitPartName.hasNext()) {
                            String strSplitPartAttrName = (String) itrSplitPartName.next();
                            // TIGTK-8696 - END
                            if (checkIfAttributeExistsOnPart) {
                                // TIGTK-8696 - START
                                if (attributeList.contains(strSplitPartAttrName)) {
                                    attrValue = getCalculatedAttributeValue(context, cadObject, strSplitPartAttrName, attrValue, CalculationMap);
                                    attrValueHash.put(strSplitPartAttrName, attrValue);
                                    // TIGTK-8696 - END
                                }
                            }
                            // TIGTK-8696 - START
                        }
                        // TIGTK-8696 - END
                    }

                }
                // TIGTK-8696 - END

            } catch (Exception e) {
                // MCADServerException.createException(e.getMessage(), e);
            }
        }

        if (sysAttrValueHash != null && sysAttrValueHash.size() > 0) {
            attrValueHash.put("System Attributes", sysAttrValueHash);
        }
    }

    /**
     * This method is used to get all the Attributes for Part(Attributes that get applied applied to Part based on "Part Family")
     * @param context
     * @param args
     * @param args0
     *            -- Domain Object Instance of Part
     * @return -- Vector -- Returns the Vector containing the attribute names of Part
     * @throws Exception
     */
    private Vector getPartAttributeList(Context context, DomainObject domPartObject) {
        // TODO Auto-generated method stub
        Vector attributeVector = new Vector();
        try {
            Map attrm = domPartObject.getAttributeMap(context);
            Set en = attrm.keySet();
            Iterator itr = en.iterator();
            while (itr.hasNext()) {
                String attrname = (String) itr.next();
                attributeVector.add(attrname);
            }
        } catch (FrameworkException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return attributeVector;
    }

    protected String getSystemAttributeValues(Context context, BusinessObject cadObj, String attrName) throws Exception {
        if ("$$Owner$$".equals(attrName))
            return cadObj.getOwner(context).getName();
        else if ("$$Description$$".equals(attrName))
            return cadObj.getDescription(context);
        else
            return "";
    }

    protected Hashtable setSystemAttributeValues(Context context, Part partObj, Hashtable attrMap) throws Exception {
        Hashtable sysAttr = (Hashtable) attrMap.get("System Attributes");
        attrMap.remove("System Attributes");

        if (sysAttr != null && sysAttr.containsKey("$$Description$$")) {
            String descAttr = (String) sysAttr.get("$$Description$$");
            if (descAttr != null && descAttr.trim().length() > 0) {
                partObj.setDescription(context, descAttr);
                partObj.update(context);
            }
        }

        if (sysAttr != null && sysAttr.containsKey("$$Owner$$")) {
            String ownerAttr = (String) sysAttr.get("$$Owner$$");
            if (ownerAttr != null && ownerAttr.trim().length() > 0) {
                partObj.setOwner(context, ownerAttr);
                partObj.update(context);
            }
        }
        return attrMap;
    }

    /**
     * This method is called to calculate some specific "Part" attributes values based on "CAD Object" attribute values.
     * @param context
     * @param args
     * @param args0
     *            -- CAD Object
     * @param args1
     *            -- Part Attribute name whose values is to be calculated
     * @param args2
     *            -- Attribute Value corresponding to partAttrName
     * @param args3
     *            -- HashMap containing the attributes required for Calculation
     * @return -- String -- Return Attribute Value to be updated on Part
     * @throws Exception
     */
    protected String getCalculatedAttributeValue(Context context, BusinessObject cadObject, String partAttrName, String attrValue, HashMap CalculationMap) throws Exception {
        // TIGTK-8696 - START
        double result = 0;
        // TIGTK-8696 - END
        // Check if the "CalculationMap" contains the partAttrName whose value is to be computed
        if (attrValue.contains("mm")) {
            attrValue = attrValue.replace("mm", "");
            attrValue = attrValue.trim();
        }

        if (CalculationMap.containsKey(partAttrName)) {
            // TIGTK-8696 - START

            if (UIUtil.isNotNullAndNotEmpty(attrValue) && !attrValue.equalsIgnoreCase("0")) {
                result = Double.parseDouble(attrValue);
                // TIGTK-8696 - START
                if (result != 0 || result != 0.0) {
                    // TIGTK-8696 - END
                    return attrValue;
                }
            } else {
                // TIGTK-8696 - END
                String strCalculation = (String) CalculationMap.get(partAttrName);
                int position = strCalculation.indexOf("[");
                String strCaseString = strCalculation.substring(0, position);
                DomainObject domCADObject = DomainObject.newInstance(context, cadObject.getObjectId());
                switch (strCaseString) {
                case "Multiply":
                    String strLengthWidthString = strCalculation.substring(9, strCalculation.length() - 1);
                    StringList lstCADObjectAttrributesList = FrameworkUtil.split(strLengthWidthString, ",");
                    Iterator strCADObjectAttributesListItr = lstCADObjectAttrributesList.iterator();
                    StringList lstbusSelect = new StringList();
                    while (strCADObjectAttributesListItr.hasNext()) {
                        String strCADObjectAttribName = (String) strCADObjectAttributesListItr.next();

                        lstbusSelect.add("attribute[" + strCADObjectAttribName + "]");
                    }
                    Map mapCADObjectDetails = domCADObject.getInfo(context, lstbusSelect);

                    result = 1;
                    strCADObjectAttributesListItr = lstCADObjectAttrributesList.iterator();
                    // TIGTK-139 START
                    if (lstCADObjectAttrributesList.size() == 2 && lstCADObjectAttrributesList.contains(TigerConstants.ATTRIBUTE_PSS_HEIGHT)
                            && lstCADObjectAttrributesList.contains(TigerConstants.ATTRIBUTE_PSS_WIDTH)) {
                        String strHeightAttr = (String) strCADObjectAttributesListItr.next();
                        String strWidthAttr = (String) strCADObjectAttributesListItr.next();

                        String strHeightAttrValue = (String) mapCADObjectDetails.get("attribute[" + strHeightAttr + "]");
                        String strWidthAttrValue = (String) mapCADObjectDetails.get("attribute[" + strWidthAttr + "]");

                        if (strHeightAttrValue.contains("mm")) {
                            strHeightAttrValue = strHeightAttrValue.replace("mm", "").trim();
                        }
                        if (strWidthAttrValue.contains("mm")) {
                            strWidthAttrValue = strWidthAttrValue.replace("mm", "").trim();
                        }
                        StringList slAttrList = new StringList(strHeightAttrValue);
                        slAttrList.add(strWidthAttrValue);
                        Boolean bIsNull = false;
                        Iterator strCADObjectAttributesItr = slAttrList.iterator();
                        while (strCADObjectAttributesItr.hasNext()) {
                            String strCADObjectAttrValue = (String) strCADObjectAttributesItr.next();
                            bIsNull = removeEntryIfValueIsNull(context, strCADObjectAttrValue);
                            if (bIsNull) {
                                return null;
                            }

                        }
                        if (!bIsNull) {
                            // TIGTK-16037:19-07-2018:STARTS
                            attrValue = strWidthAttrValue + "x" + strHeightAttrValue;
                            // TIGTK-16037:19-07-2018:ENDS
                            return attrValue;
                        }
                        // TIGTK-139 END
                    } else {
                        while (strCADObjectAttributesListItr.hasNext()) {
                            String strCADObjectAttribName = (String) strCADObjectAttributesListItr.next();
                            String strAttrValue = (String) mapCADObjectDetails.get("attribute[" + strCADObjectAttribName + "]");
                            if (strAttrValue.contains("mm")) {
                                strAttrValue = strAttrValue.replace("mm", "");
                                strAttrValue = strAttrValue.trim();
                            }
                            double dblAttributeValue = Double.parseDouble(strAttrValue);
                            result = result * dblAttributeValue;
                        }
                    }
                    break;
                default:
                    break;

                }
                return Double.toString(result);
            }
        }
        return attrValue;
    }

    /**
     * This method is used to get the Mandatory Attribute Mapping between "CAD Object" and Part
     * @param context
     * @param args
     * @param args0
     *            -- Instance of IEFEBOMConfigObject
     * @param args1
     *            -- The type of "CAD Object"
     * @param args2
     *            -- The type of Part
     * @return -- Hashtable -- Contains the Mandatory attribute mapping of "CAD Object" attribute and its corresponding Part attribute
     * @throws Exception
     */
    private Hashtable getMandTypeAttributeMapping(Context context, IEFEBOMConfigObject ebomConfObj, String cadType, String partType) {
        // TODO Auto-generated method stub
        Hashtable localHashtable = new Hashtable();
        String strMandAttr = ebomConfObj.getConfigAttributeValue(ebomConfObj.ATTR_MAND_OBJECT_ATTR_MAPPING);
        if ((strMandAttr != null) && (strMandAttr.length() > 0)) {
            StringTokenizer localStringTokenizer1 = new StringTokenizer(strMandAttr, "\n");
            while (localStringTokenizer1.hasMoreElements()) {
                String str2 = (String) localStringTokenizer1.nextElement();
                StringTokenizer localStringTokenizer2 = new StringTokenizer(str2, "|");
                if (localStringTokenizer2.countTokens() == 4) {
                    int i = str2.indexOf("|");
                    String str3 = str2.substring(0, i);
                    String str4 = str2.substring(i + 1);

                    Vector localVector = (Vector) localHashtable.get(str3);
                    if (localVector == null) {
                        localVector = new Vector();
                    }
                    localVector.addElement(str4);
                    localHashtable.put(str3, localVector);
                } else {
                    System.out.println("[IEFEBOMConfigObject:readAttributeMapping] Wrong Attribute mapping : " + str2);
                }
            }
        }
        Vector localVector1 = getBaseTypes(context, cadType);
        Vector localVector2 = getBaseTypes(context, partType);
        return getAttributeMapping(localHashtable, localVector1, localVector2);
        // return localHashtable;
    }

    /**
     * This method is used to get the "Parent Types" for the specified type.
     * @param context
     * @param args
     * @param args0
     *            -- Type for which for "Parent Types" are to be computed
     * @return -- Vector -- Contains "Parent Type" for given type
     * @throws Exception
     */
    private Vector getBaseTypes(Context paramContext, String paramString) {
        Vector localVector = new Vector();
        try {
            String str = paramString;
            if ((str != null) && (!str.trim().equals(""))) {
                localVector.addElement(str);
                /*
                 * BusinessType localBusinessType = new BusinessType(str, new Vault("")); StringList strTypeList = localBusinessType.getParents(paramContext); Iterator strTypeItr =
                 * strTypeList.iterator(); while(strTypeItr.hasNext()){ String strCurrentType = (String)strTypeItr.next(); localVector.addElement(strCurrentType); }
                 */
            }
        } catch (Exception localException) {
        }
        return localVector;
    }

    /**
     * This method is used to get the Attribute Mapping between "CAD Object" and Part
     * @param context
     * @param args
     * @param args0
     *            -- Vector containing the "CAD Object" attributes
     * @param args1
     *            -- Vector containing the "Part" attributes
     * @return -- Hashtable -- Contains the mapping of "CAD Object" attribute and its corresponding Part attribute
     * @throws Exception
     */
    private Hashtable getAttributeMapping(Hashtable paramHashtable, Vector paramVector1, Vector paramVector2) {
        Hashtable localHashtable = new Hashtable();
        try {
            Vector localVector = new Vector();
            String str1;
            Object localObject;
            for (int i = 0; i < paramVector1.size(); i++) {
                str1 = (String) paramVector1.elementAt(i);
                localObject = (Vector) paramHashtable.get(str1);
                if (localObject != null) {
                    localVector.addAll((Collection) localObject);
                }
            }
            if (localVector != null) {
                for (int i = 0; i < localVector.size(); i++) {
                    str1 = (String) localVector.elementAt(i);
                    if (!isTypeIncluded(paramVector2, str1))
                        continue;
                    localObject = new StringTokenizer(str1, "|");
                    String str2 = (String) ((StringTokenizer) localObject).nextElement();
                    String str3 = (String) ((StringTokenizer) localObject).nextElement();
                    String str4 = (String) ((StringTokenizer) localObject).nextElement();
                    if (localHashtable.containsKey(str2)) {
                        String strKeyValue = (String) localHashtable.get(str2);
                        strKeyValue = strKeyValue + "," + str4;
                        localHashtable.put(str2, strKeyValue);
                    } else {
                        localHashtable.put(str2, str4);
                    }
                }
            }

        } catch (Exception localException) {
            System.out.println("[IEFEBOMConfigObject:getTypeAttributeMapping] Exception - " + localException.getMessage());
        }
        return (Hashtable) localHashtable;
    }

    /**
     * This method is called to check if the type is included or not
     * @param context
     * @param args
     * @param args0
     *            -- Given Type
     * @return -- boolean -- Returns the stataus whether type is included or not
     * @throws Exception
     */
    private boolean isTypeIncluded(Vector paramVector, String paramString) {
        for (int i = 0; i < paramVector.size(); i++) {
            String str = (String) paramVector.elementAt(i);
            if (paramString.indexOf("|" + str + "|") > 0)
                return true;
        }
        return false;
    }

    /**
     * TIGTK- 13926 Method to avoid EBOM synch if Height or width attribute value is null or zero for FCM view.
     * @param context
     * @param args0
     *            CAD Attribute value
     * @return void
     * @throws Exception
     */
    public boolean removeEntryIfValueIsNull(Context context, String strCADObjectAttrValue) throws Exception {

        boolean isNotNull = false;

        try {

            // Remove entry if value is null or equals 0.
            if (strCADObjectAttrValue == null || strCADObjectAttrValue.equals(DomainConstants.EMPTY_STRING) || strCADObjectAttrValue.length() == 0) {
                isNotNull = true;
            } else {
                double dAttributeValue = Double.parseDouble(strCADObjectAttrValue);
                double dAttributeFloorValue = Math.floor(dAttributeValue);
                double dAttributeCeilValue = Math.ceil(dAttributeValue);

                if (dAttributeFloorValue == 0.0 && dAttributeCeilValue == 0.0) {
                    isNotNull = true;
                }
            }

        } catch (Exception ex) {
        }
        return isNotNull;

    }
}