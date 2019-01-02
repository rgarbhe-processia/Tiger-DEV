
/**
 * IEFEBOMProxy Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice
 * is precautionary only and does not evidence any actual or intended publication of such program
 */

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.StringTokenizer;

import matrix.db.Context;
import matrix.db.JPO;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.MCADIntegration.server.beans.IEFEBOMConfigObject;
import com.matrixone.MCADIntegration.server.beans.MCADEBOMSynchPageHelper;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;

public class IEFEBOMProxy_mxJPO {
    private MCADMxUtil _util = null;

    private MCADServerGeneralUtil _generalUtil = null;

    private MCADServerResourceBundle _resourceBundle = null;

    private IEFGlobalCache _cache = null;

    private MCADGlobalConfigObject _globalConfigObject = null;

    private Hashtable famIDInstanceTable = new Hashtable();

    private IEFEBOMConfigObject ebomConfigObject = null;

    private String confAttrAssignPartToMajor = null;

    private Hashtable _argumentsTable = null;

    private String jpoName = null;

    public IEFEBOMProxy_mxJPO() {
    }

    public IEFEBOMProxy_mxJPO(Context context, String[] args) throws Exception {
        if (!context.isConnected())
            MCADServerException.createException("not supported no desktop client", null);

        initialize(context, args);
    }

    public int mxMain(Context context, String[] args) throws Exception {
        return 0;
    }

    /**
     * This method initializes all the class members useful in the JPO operations
     */
    public void initialize(Context context, String[] args) throws MCADException {
        try {
            _argumentsTable = (Hashtable) JPO.unpackArgs(args);
            _globalConfigObject = (MCADGlobalConfigObject) _argumentsTable.get(MCADServerSettings.GCO_OBJECT);
            String languageName = (String) _argumentsTable.get(MCADServerSettings.LANGUAGE_NAME);
            _resourceBundle = new MCADServerResourceBundle(languageName);
            _cache = new IEFGlobalCache();
            _util = new MCADMxUtil(context, _resourceBundle, _cache);
            _generalUtil = new MCADServerGeneralUtil(context, _globalConfigObject, _resourceBundle, _cache);

            String commandName = MCADGlobalConfigObject.FEATURE_EBOMSYNCHRONIZE;
            this.jpoName = _globalConfigObject.getFeatureJPO(commandName);

            String sEBOMRegistryTNR = _globalConfigObject.getEBOMRegistryTNR();
            StringTokenizer token = new StringTokenizer(sEBOMRegistryTNR, "|");
            if (token.countTokens() >= 3) {
                String sEBOMRConfigObjType = (String) token.nextElement();
                String sEBOMRConfigObjName = (String) token.nextElement();
                String sEBOMRConfigObjRev = (String) token.nextElement();

                ebomConfigObject = new IEFEBOMConfigObject(context, sEBOMRConfigObjType, sEBOMRConfigObjName, sEBOMRConfigObjRev);
            }

            confAttrAssignPartToMajor = "true";// ebomConfigObject.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_ASSIGN_PART_TO_MAJOR);

        } catch (Exception e) {
            System.out.println("[initialize]: Exception while initializating JPO" + e.getMessage());
            MCADServerException.createException(e.getMessage(), e);
        }
    }

    /*
     * Entry Point. This method is responsible for creating EBOM Syncroinzation for all selected root node objects and call the EBOM Synch JPO which is mapped in GCO feature JPOs for each root node
     * objects. This method will receive the all root bus object ids as '|' seperated value in busId content in arguments table.
     */
    public Hashtable createEBOMSynchronization(Context context, String[] args) throws MCADException {
        Hashtable resultDataTable = new Hashtable();
        StringBuffer resultMessage = new StringBuffer();

        try {
            String busIds = args[0];

            String busId = "";
            String jpoMethod = "execute";
            String[] jpoArgs = new String[] {};
            String[] init = new String[] {};

            StringTokenizer tokens = new StringTokenizer(busIds, "|");

            while (tokens.hasMoreElements()) {
                busId = (String) tokens.nextElement();

                // check for child strucutres same if instance and mode single
                isEBOMRestricted(context, busId);

                _argumentsTable.put(MCADServerSettings.OBJECT_ID, busId);
                jpoArgs = JPO.packArgs(_argumentsTable);
                resultDataTable = (Hashtable) JPO.invoke(context, jpoName, init, jpoMethod, jpoArgs, Hashtable.class);

                if (isOperationSuccessful(resultDataTable)) {
                    String resultMsg = (String) resultDataTable.get(MCADServerSettings.JPO_STATUS_MESSAGE);
                    resultMessage.append("\n");
                    resultMessage.append(resultMsg);
                } else {
                    String error = (String) resultDataTable.get(MCADServerSettings.JPO_STATUS_MESSAGE);
                    MCADServerException.createException(error, null);
                }
            }

            resultDataTable.put(MCADServerSettings.JPO_STATUS_MESSAGE, resultMessage.toString());
        } catch (Exception e) {
            resultDataTable.put(MCADServerSettings.JPO_EXECUTION_STATUS, "false");
            resultDataTable.put(MCADServerSettings.JPO_STATUS_MESSAGE, e.getMessage());
        }

        return resultDataTable;
    }

    private boolean isOperationSuccessful(Hashtable resultDataTable) throws MCADException {
        boolean returnStatus = false;
        String result = (String) resultDataTable.get(MCADServerSettings.JPO_EXECUTION_STATUS);
        if (result.equalsIgnoreCase("true")) {
            returnStatus = true;
        }

        return returnStatus;
    }

    private void isEBOMRestricted(Context context, String busID) throws Exception {
        String cadTypeAttrActualName = MCADMxUtil.getActualNameForAEFData(context, "attribute_CADType");
        String isEBOMRestrictedOnDesign = "false";
        String cadType = _util.getAttributeForBO(context, busID, cadTypeAttrActualName);
        // validate first level structures if mode is single take care of exclude case for structure validation
        if (_globalConfigObject.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE)) {
            String famID = _generalUtil.getTopLevelFamilyObjectForInstance(context, busID);
            String ebomExpositionMode = _util.getAttributeForBO(context, famID, MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-EBOMExpositionMode"));

            if (null != ebomExpositionMode && "single".equalsIgnoreCase(ebomExpositionMode)) {
                ArrayList instanceList = new ArrayList();
                if (!famIDInstanceTable.containsKey(famID)) {
                    ArrayList allInstanceList = _generalUtil.getFamilyStructureRecursively(context, new String[] { famID }, new Hashtable(), null);
                    famIDInstanceTable.put(famID, allInstanceList);

                    instanceList = _generalUtil.getAlreadyEBOMSynchedInstanceList(context, allInstanceList, confAttrAssignPartToMajor);
                    instanceList.add(busID);
                } else {
                    ArrayList allInstanceList = (ArrayList) famIDInstanceTable.get(famID);
                    instanceList = _generalUtil.getAlreadyEBOMSynchedInstanceList(context, allInstanceList, confAttrAssignPartToMajor);

                    // One Transaction Scenario multiple SAI.NO EBOM hence no existing part.
                    // validate with all structures
                    if (instanceList.size() == 0)
                        instanceList.addAll(allInstanceList);
                    else
                        instanceList.add(busID);
                }

                if (instanceList.size() > 1)
                    isEBOMRestrictedOnDesign = _generalUtil.isEBOMRestrictedOnDesign(context, instanceList);
            }
        }

        if (isEBOMRestrictedOnDesign.startsWith("true|")) {
            isEBOMRestrictedOnDesign = isEBOMRestrictedOnDesign.substring(5);
            StringTokenizer objDetails = new StringTokenizer(isEBOMRestrictedOnDesign, "|");

            if (objDetails.hasMoreTokens() && objDetails.countTokens() == 6) {
                Hashtable messageDetails = new Hashtable();
                messageDetails.put("TYPE1", objDetails.nextToken());
                messageDetails.put("NAME1", objDetails.nextToken());
                messageDetails.put("REV1", objDetails.nextToken());

                messageDetails.put("TYPE2", objDetails.nextToken());
                messageDetails.put("NAME2", objDetails.nextToken());
                messageDetails.put("REV2", objDetails.nextToken());

                MCADServerException.createException(_resourceBundle.getString("mcadIntegration.Server.Message.CantEBOMOnObjectHavingStructureDiffWithStructureDetails", messageDetails), null);
            }
        }

        // return isEBOMRestrictedOnDesign;
    }
}
