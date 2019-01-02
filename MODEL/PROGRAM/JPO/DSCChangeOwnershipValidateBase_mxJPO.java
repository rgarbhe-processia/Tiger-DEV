
/**
 * DSCChangeOwnershipValidateBase.java Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its
 * subsidiaries, Copyright notice is precautionary only and does not evidence any actual or intended publication of such program
 */

import java.util.HashMap;
import java.util.Vector;
import java.util.StringTokenizer;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Locale;
import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;
import java.util.Iterator;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;

import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.util.DSCOwnershipUtil;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.server.beans.IEFSimpleConfigObject;
import com.matrixone.MCADIntegration.server.beans.MCADConfigObjectLoader;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;

import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.FrameworkUtil;

import com.matrixone.MCADIntegration.utils.MCADUtil;

import com.matrixone.MCADIntegration.server.MCADServerException;

import matrix.util.StringList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.RelationshipWithSelect;
import matrix.util.MatrixException;
import matrix.util.StringList;
import com.matrixone.apps.domain.DomainRelationship;

public class DSCChangeOwnershipValidateBase_mxJPO {
    DSCOwnershipUtil changeOwner = null;

    protected MCADServerResourceBundle _serverResourceBundle = null;

    public DSCChangeOwnershipValidateBase_mxJPO() {

    }

    public DSCChangeOwnershipValidateBase_mxJPO(Context context, String[] args) throws Exception {
    }

    public int mxMain(Context context, String[] args) throws Exception {
        return 0;
    }

    private boolean isMajorObject(Context context, String objectId, String language) throws Exception {
        boolean isMajor = true;

        MCADMxUtil mxUtil = new MCADMxUtil(context, new MCADServerResourceBundle(language), new IEFGlobalCache());

        BusinessObject busObject = new BusinessObject(objectId);
        busObject.open(context);
        BusinessObject majorBusObject = mxUtil.getMajorObject(context, busObject);
        busObject.close(context);

        if (majorBusObject != null)
            isMajor = false;

        return isMajor;
    }

    public Boolean inValidateForMinor(Context context, String[] args) throws Exception {
        boolean result = false;

        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        String objectId = (String) paramMap.get("objectId");
        String language = (String) paramMap.get("languageStr");

        changeOwner = new DSCOwnershipUtil(context, language);

        if (objectId != null && isMajorObject(context, objectId, language))
            return new Boolean(result);

        if (!changeOwner.validateUserForChangeOwner(context, objectId, language))
            result = true;

        return new Boolean(result);
    }

    public Boolean validateForMinor(Context context, String[] args) throws Exception {
        boolean result = false;

        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        String objectId = (String) paramMap.get("objectId");
        String language = (String) paramMap.get("languageStr");

        changeOwner = new DSCOwnershipUtil(context, language);

        if (objectId != null && isMajorObject(context, objectId, language))
            return new Boolean(result);

        if (changeOwner.validateUserForChangeOwner(context, objectId, language))
            result = true;

        return new Boolean(result);
    }

    public Boolean inValidate(Context context, String[] args) throws Exception {
        boolean result = false;
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        String objectId = (String) paramMap.get("objectId");
        String language = (String) paramMap.get("languageStr");

        changeOwner = new DSCOwnershipUtil(context, language);

        if (objectId != null && !isMajorObject(context, objectId, language))
            return new Boolean(result);

        if (!changeOwner.validateUserForChangeOwner(context, objectId, language))
            result = true;

        return new Boolean(result);
    }

    public Boolean validate(Context context, String[] args) throws Exception {
        boolean result = false;

        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        String objectId = (String) paramMap.get("objectId");
        String language = (String) paramMap.get("languageStr");

        changeOwner = new DSCOwnershipUtil(context, language);

        if (objectId != null && !isMajorObject(context, objectId, language))
            return new Boolean(result);

        if (changeOwner.validateUserForChangeOwner(context, objectId, language))
            result = true;

        return new Boolean(result);
    }

    public HashMap updateOwnerForVersion(Context context, String[] args) throws Exception {
        HashMap resultMap = null;
        updateOwner(context, args);
        return resultMap;
    }

    private void validateNewOwner(Context context, String language, String newOwner, String strObjectId) throws Exception {
        if (MCADMxUtil.isSolutionBasedEnvironment(context)) {
            boolean isSuccess = false;
            String errorMsg = "";
            String vplmViewer = MCADMxUtil.getActualNameForAEFData(context, "role_VPLMViewer");
            String vplmExperimenter = MCADMxUtil.getActualNameForAEFData(context, "role_VPLMExperimenter");
            String vplmProjectAdmin = MCADMxUtil.getActualNameForAEFData(context, "role_VPLMProjectAdministrator");

            MCADServerResourceBundle _serverResourceBundle = new MCADServerResourceBundle(language);

            String sCommandStatementProject = "print bus $1 select $2 $3 dump";
            String projectAndOrg = MqlUtil.mqlCommand(context, sCommandStatementProject, strObjectId, "organization", "project");
            Vector prjAndOrg = MCADUtil.getVectorFromString(projectAndOrg, ",");
            String prj = (String) prjAndOrg.get(1);
            String org = (String) prjAndOrg.get(0);

            String sCommandStatementUserId = "print person $1 select $2 dump";
            String sUserID = MqlUtil.mqlCommand(context, sCommandStatementUserId, newOwner, "name");

            String sCommandStatementRole = "print role $1 select $2 dump";
            String userList = MqlUtil.mqlCommand(context, sCommandStatementRole, prj, "person");
            StringList sUserList = FrameworkUtil.split(userList, ",");

            if (!sUserList.contains(sUserID)) {
                errorMsg = _serverResourceBundle.getString("mcadIntegration.Server.Message.ProjectofSelectedUserAndObjectNotSame");
                MCADServerException.createException(errorMsg, null);
            }

            String sCommandStatement1 = "print person $1 select  assignment dump";
            String newOwnerRolesString = MqlUtil.mqlCommand(context, sCommandStatement1, newOwner);
            StringTokenizer ownersRoles = new StringTokenizer(newOwnerRolesString, ",");

            while (ownersRoles.hasMoreTokens()) {

                String ownerRole = ownersRoles.nextToken();
                if (ownerRole.startsWith("ctx::")) {

                    ownerRole = ownerRole.substring(5);
                    Vector result = MCADUtil.getVectorFromString(ownerRole, ".");
                    String roleName = (String) result.get(0);
                    String OrganizationName = (String) result.get(1);
                    String projectName = (String) result.get(2);

                    if (((!roleName.equals(vplmViewer)) && (!roleName.equals(vplmExperimenter)) && (!roleName.equals(vplmProjectAdmin))) && prj.equals(projectName)) {
                        isSuccess = true;
                        break;
                    }
                }

            }
            if (!isSuccess) {
                errorMsg = _serverResourceBundle.getString("mcadIntegration.Server.Message.UserNotHaveAuthourityToAuthor");
                MCADServerException.createException(errorMsg, null);
            }
        }
    }

    public Boolean updateOwner(Context context, String[] args) throws Exception {
        boolean returnValue = true;
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap paramMap = (HashMap) programMap.get("paramMap");
            String language = (String) paramMap.get("languageStr");
            boolean validateOwner = MCADUtil.getBoolean((String) paramMap.get("validateNewOwner"), true);

            MCADMxUtil util = new MCADMxUtil(context, null, new IEFGlobalCache());

            String strObjectId = (String) paramMap.get("objectId");
            String newOwnerValue = (String) paramMap.get("New Value");

            MCADGlobalConfigObject globalConfigObj = getGlobalConfigObject(context, strObjectId, util);

            MCADServerGeneralUtil serverUtil = new MCADServerGeneralUtil(context, globalConfigObj, new MCADServerResourceBundle(language), new IEFGlobalCache());

            BusinessObject busObj = new BusinessObject(strObjectId);
            String cad_type = util.getCADTypeForBO(context, busObj);

            busObj.open(context);
            String busType = busObj.getTypeName();
            String revisionMode = "";
            HashSet inputInstanceIDS = new HashSet();

            if (null != globalConfigObj.getDefaultFamRevMode() && !"".equals(globalConfigObj.getDefaultFamRevMode())) {
                if (globalConfigObj.isTypeOfClass(cad_type, MCADAppletServletProtocol.TYPE_FAMILY_LIKE)) {
                    revisionMode = busObj.getAttributeValues(context, MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-ObjectBasedConfigurationRevisionMode")).getValue();
                } else if (globalConfigObj.isTypeOfClass(cad_type, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE)) {
                    BusinessObject famObj = null;
                    inputInstanceIDS.add(strObjectId);
                    Hashtable validFamilyIdTable = serverUtil.getValidObjectIdToGetParent(context, inputInstanceIDS);
                    String validInstId = (String) validFamilyIdTable.get(strObjectId);
                    BusinessObject validInsBusObj = new BusinessObject(validInstId);

                    famObj = serverUtil.getFamilyObjectForInstance(context, validInsBusObj);
                    revisionMode = famObj.getAttributeValues(context, MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-ObjectBasedConfigurationRevisionMode")).getValue();
                }
            } else
                revisionMode = "individual";

            // [NDM] removed finalization check as DEC is not having any logic on Finalization
            // if(!serverUtil.isBusObjectFinalized(context,busObj) && util.isMajorObject(context, strObjectId) && revisionMode.equalsIgnoreCase("together")) //globalConfigObj.isMajorType(busType) //
            // [NDM] OP6
            // {
            // strObjectId = util.getActiveMinor(context,busObj).getObjectId();
            // }

            busObj.close(context);

            String instanceOfRelName = MCADMxUtil.getActualNameForAEFData(context, "relationship_InstanceOf");

            StringBuffer Sb_to_instance_of_from = new StringBuffer("to[");
            Sb_to_instance_of_from.append(instanceOfRelName);
            Sb_to_instance_of_from.append("].from.");

            StringBuffer Sb_from_instance_of_to = new StringBuffer("from[");
            Sb_from_instance_of_to.append(instanceOfRelName);
            Sb_from_instance_of_to.append("].to.");

            String to_instance_of_from = Sb_to_instance_of_from.toString();

            String from_instance_of_to = Sb_from_instance_of_to.toString();

            StringList selectStmts = new StringList();
            selectStmts.addElement(to_instance_of_from + "id");
            selectStmts.addElement(to_instance_of_from + from_instance_of_to + "id");
            selectStmts.addElement(from_instance_of_to + "id");

            BusinessObjectWithSelectList busWithSelectList = BusinessObject.getSelectBusinessObjectData(context, new String[] { strObjectId }, selectStmts);

            BusinessObjectWithSelect busWithSelect = (BusinessObjectWithSelect) busWithSelectList.elementAt(0);

            Vector connectedInstanceList = new Vector();

            if (globalConfigObj.isTypeOfClass(cad_type, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE) && revisionMode.equalsIgnoreCase("together")) {
                StringList familyIdList = busWithSelect.getSelectDataList(to_instance_of_from + "id");
                StringList instanceList = busWithSelect.getSelectDataList(to_instance_of_from + from_instance_of_to + "id");
                connectedInstanceList.addAll(familyIdList);
                connectedInstanceList.addAll(instanceList);
            } else if (globalConfigObj.isTypeOfClass(cad_type, MCADAppletServletProtocol.TYPE_FAMILY_LIKE) && revisionMode.equalsIgnoreCase("together")) {
                StringList instanceList = busWithSelect.getSelectDataList(from_instance_of_to + "id");
                connectedInstanceList.add(strObjectId);
                connectedInstanceList.addAll(instanceList);
            } else
                connectedInstanceList.add(strObjectId);

            Iterator iterator = connectedInstanceList.iterator();

            while (iterator.hasNext()) {
                String objectId = (String) iterator.next();
                updateOwnerForBO(context, objectId, newOwnerValue, language, validateOwner);
            }

        } catch (Exception e) {
            returnValue = false;
            throw e;
        }

        return new Boolean(returnValue);
    }

    private void updateOwnerForBO(Context context, String strObjectId, String newOwnerValue, String language, Boolean validateOwner) throws Exception {
        Boolean returnValue = false;

        MCADServerResourceBundle _serverResourceBundle = new MCADServerResourceBundle(language);
        IEFGlobalCache _cache = new IEFGlobalCache();
        MCADMxUtil _util = new MCADMxUtil(context, _serverResourceBundle, _cache);

        boolean isSolutionBasedEnvironment = MCADMxUtil.isSolutionBasedEnvironment(context);
        BusinessObject busObj = new BusinessObject(strObjectId);

        if (isSolutionBasedEnvironment) {
            Boolean isMajor = isMajorObject(context, strObjectId, language);
            String locker = busObj.getLocker(context).getName();
            if (!isMajor) {
                BusinessObject majorBusObject = _util.getMajorObject(context, busObj);
                locker = majorBusObject.getLocker(context).getName();
            }

            if (!locker.isEmpty()) {
                Hashtable messageTokens = new Hashtable();
                messageTokens.put("LOCKERNAME", locker);
                String errorMsg = _serverResourceBundle.getString("mcadIntegration.Server.Message.LockedByAnotherUser", messageTokens);
                MCADServerException.createException(errorMsg, null);
            }
        }

        if (validateOwner)
            validateNewOwner(context, language, newOwnerValue, strObjectId);

        changeOwner = new DSCOwnershipUtil(context, language);
        returnValue = changeOwner.updateOwner(context, strObjectId, newOwnerValue);
    }

    private String getGlobalConfigObjectName(Context context, String busId) throws Exception {
        // Get the IntegrationName
        IEFGuessIntegrationContext_mxJPO guessIntegration = new IEFGuessIntegrationContext_mxJPO(context, null);
        String jpoArgs[] = new String[1];
        jpoArgs[0] = busId;
        String integrationName = guessIntegration.getIntegrationName(context, jpoArgs);

        // Get the relevant GCO Name

        String gcoName = null;

        IEFSimpleConfigObject simpleLCO = IEFSimpleConfigObject.getSimpleLCO(context);

        String gcoType = MCADMxUtil.getActualNameForAEFData(context, "type_MCADInteg-GlobalConfig");
        String attrName = MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-IntegrationToGCOMapping");
        Hashtable integNameGcoMapping = null;
        ;
        if (simpleLCO.isObjectExists())
            integNameGcoMapping = simpleLCO.getAttributeAsHashtable(attrName, "\n", "|");

        if (integNameGcoMapping != null && integNameGcoMapping.size() > 0) {
            gcoName = (String) integNameGcoMapping.get(integrationName);
        } else {
            IEFGetRegistrationDetails_mxJPO registrationDetailsReader = new IEFGetRegistrationDetails_mxJPO(context, null);
            String args[] = new String[1];
            args[0] = integrationName;
            String registrationDetails = registrationDetailsReader.getRegistrationDetails(context, args);
            gcoName = registrationDetails.substring(registrationDetails.lastIndexOf("|") + 1);
        }

        return gcoName;
    }

    protected MCADGlobalConfigObject getGlobalConfigObject(Context context, String objectId, MCADMxUtil mxUtil) throws Exception {
        String typeGlobalConfig = MCADMxUtil.getActualNameForAEFData(context, "type_MCADInteg-GlobalConfig");
        String gcoName = this.getGlobalConfigObjectName(context, objectId);

        MCADGlobalConfigObject gcoObject = null;

        if (gcoName != null && gcoName.length() > 0) {
            MCADConfigObjectLoader configLoader = new MCADConfigObjectLoader(null);
            gcoObject = configLoader.createGlobalConfigObject(context, mxUtil, typeGlobalConfig, gcoName);
        }
        return gcoObject;
    }

    public int checkTransferOwnershipRules(matrix.db.Context context, String[] args) throws Exception {

        MCADMxUtil util = new MCADMxUtil(context, null, new IEFGlobalCache());
        if (util.isSolutionBasedEnvironment(context)) {
            String Args[] = new String[1];
            Args[0] = "DECIgnoreChangeOwnerCheck";
            String result = util.executeMQL(context, "get env $1", Args);

            if (result.startsWith("true")) {

                result = result.substring(5);
                if (result.startsWith("true"))
                    return 0;
            }
            String event = args[0];
            String objectId = args[1];
            String sNewOwner = args[2];
            String sNewOrg = args[3];
            String sNewProject = args[4];
            String sKindOfChange = args[5];
            String sCurrentOrg = args[6];
            String sCurrentProj = args[7];
            String s4 = args[8];
            String s5 = args[9];
            /*
             * System.out.println("pre--EVENT>>"+event); System.out.println("--OBJECTID>>"+objectId); System.out.println("--NEWOWNER>>"+sNewOwner); System.out.println("--NEWALTOWNER1>>"+sNewOrg);
             * System.out.println("--NEWALTOWNER2>>"+sNewProject);
             * System.out.println("--KINDOFOWNER>>"+sKindOfChange+"<<ALTOWNER1>>"+sCurrentOrg+"<<ALTOWNER2>>"+sCurrentProj+"<<NEWOWNERSHIPTYPE>>"+s4+"<<NEWPARENT>>"+s5);
             */

            String sContextUser = context.getUser();
            String sContextRole = context.getRole();
            /*
             * System.out.println("--sContextUser>>"+sContextUser); System.out.println("--sContextRole>>"+sContextRole);
             */
            String LANG_RESOURCE_FILE = "iefStringResource";

            String language = context.getLocale().getLanguage();

            // System.out.println("========language==="+language);

            String sCannotChangeOrg = EnoviaResourceBundle.getProperty(context, LANG_RESOURCE_FILE, new Locale(language), "mcadIntegration.Server.Message.ContextRoleCannotChangeOrg");
            String sCannotChangeProject = EnoviaResourceBundle.getProperty(context, LANG_RESOURCE_FILE, new Locale(language), "mcadIntegration.Server.Message.ContextRoleCannotChangeProject");
            String sNoSecctxtAssigned = EnoviaResourceBundle.getProperty(context, LANG_RESOURCE_FILE, new Locale(language), "mcadIntegration.Server.Message.TargetUserNoSecctxtAssigned");
            String sNoRightsOrNoValidAssignment = EnoviaResourceBundle.getProperty(context, LANG_RESOURCE_FILE, new Locale(language),
                    "mcadIntegration.Server.Message.TargetUserNoRightsOrNoValidAssignment");
            String sError = EnoviaResourceBundle.getProperty(context, LANG_RESOURCE_FILE, new Locale(language), "mcadIntegration.Server.Message.ErrorInChangeownerCheck");

            // Locked by check
            MCADServerResourceBundle _serverResourceBundle = new MCADServerResourceBundle(language);
            BusinessObject busObj = new BusinessObject(objectId);
            // Boolean isMajor = isMajorObject(context, objectId, language);
            String locker = busObj.getLocker(context).getName();
            /*
             * if(!isMajor) { BusinessObject majorBusObject = util.getMajorObject(context,busObj); locker = majorBusObject.getLocker(context).getName(); }
             */

            if (!locker.isEmpty()) {
                Hashtable messageTokens = new Hashtable();

                busObj.open(context);
                String name = busObj.getName();
                busObj.close(context);
                messageTokens.put("NAME", name);
                messageTokens.put("LOCKER", locker);
                String errorMsg = _serverResourceBundle.getString("mcadIntegration.Server.Message.ObjectAlreadyLocked", messageTokens);
                MqlUtil.mqlCommand(context, "notice $1", errorMsg);
                return 1;
            }
            // TODO if called from checkin [collab space support]; this check trigger need to be ignored; To fix using RPE variable

            // Exit if current user is "User Agent"
            /*
             * if (sContextUser.equalsIgnoreCase("User Agent")) { return 0; }
             */
            // Control on Organization Change
            if (("organization").equalsIgnoreCase(sKindOfChange)) {
                if (!(sNewOrg.equals(sCurrentOrg))) {
                    // It is not allowed to change organization except for VPLMAdmin, VPLMProjectAdministrator and VPLMProjectLeader roles
                    if ((sContextRole.indexOf("VPLMAdmin") == -1) && (sContextRole.indexOf("VPLMProjectAdministrator") == -1) && (sContextRole.indexOf("VPLMProjectLeader") == -1)) {
                        // throw error message
                        // SendNotice(context, ObjID, "OOTBParameterization.TransferOwnership.Notice.NewOrganization");
                        String sErr1 = sCannotChangeOrg;
                        MqlUtil.mqlCommand(context, "notice $1", sErr1);
                        return 1;
                    } else
                        return 0;
                } else
                    return 0;
            }

            // Control on Project Change
            if (("project").equalsIgnoreCase(sKindOfChange)) {
                if (!(sNewProject.equals(sCurrentProj))) {
                    // It is not allowed to change project except for VPLMAdmin role
                    // if ((context.getRole()).indexOf("VPLMAdmin") == -1)
                    // It is not allowed to change project except for VPLMAdmin roles
                    // project change to be done by roles - VPLMAdmin
                    // if ((sContextRole.indexOf("VPLMAdmin") == -1) && (sContextRole.indexOf("VPLMProjectAdministrator") == -1) && (sContextRole.indexOf("VPLMProjectLeader") == -1))
                    if ((sContextRole.indexOf("VPLMAdmin") == -1)) {
                        // throw error message
                        // SendNotice(context, ObjID, "OOTBParameterization.TransferOwnership.Notice.NewProject");
                        String sErr1 = sCannotChangeProject;
                        MqlUtil.mqlCommand(context, "notice $1", sErr1);
                        return 1;
                    } else
                        return 0;
                } else
                    return 0;
            }

            matrix.db.MQLCommand CmdPrint = new matrix.db.MQLCommand();
            String AssGmt = "assignment";
            String dump = "|";
            String StPrint = "print person $1 select $2 dump $3";

            if (!CmdPrint.executeCommand(context, StPrint, sNewOwner, AssGmt, dump)) {
                String sErr1 = sError;
                MqlUtil.mqlCommand(context, "notice $1", sErr1);
                return 1;
            }

            String PrintRes = CmdPrint.getResult().trim();
            // System.out.println("--MQL output>>"+PrintRes);
            if (PrintRes.equals("")) {
                String sErr1 = sNoSecctxtAssigned;
                MqlUtil.mqlCommand(context, "notice $1", sErr1);
                return 1;
            }

            String vplmProjectAdminRoleName = MCADMxUtil.getActualNameForAEFData(context, "role_VPLMProjectAdministrator");
            String vplmViewerRoleName = MCADMxUtil.getActualNameForAEFData(context, "role_VPLMViewer");
            String vplmExpRoleName = MCADMxUtil.getActualNameForAEFData(context, "role_VPLMExperimenter");

            StringList rolesThatCannotAuthor = new StringList();
            rolesThatCannotAuthor.add(vplmViewerRoleName);
            rolesThatCannotAuthor.add(vplmProjectAdminRoleName);
            rolesThatCannotAuthor.add(vplmExpRoleName);

            boolean CanAuthor = false;
            int fromIdx = 0;
            int toIdx = PrintRes.indexOf("|", fromIdx);
            while (fromIdx != -1) {
                String SubPrintRes = "";
                if (toIdx != -1)
                    SubPrintRes = PrintRes.substring(fromIdx, toIdx);
                else
                    SubPrintRes = PrintRes.substring(fromIdx);

                // Extract organization & project
                int OrgIdx = SubPrintRes.indexOf(".");
                if (OrgIdx != -1) {
                    int PrjIdx = SubPrintRes.indexOf(".", OrgIdx + 1);
                    String CtxOrg = SubPrintRes.substring(OrgIdx + 1, PrjIdx);
                    String CtxPrj = SubPrintRes.substring(PrjIdx + 1);

                    //
                    // Check if the new user can author the data on the project / org
                    //
                    if (CtxPrj.equals(sCurrentProj) || CtxPrj.equals(sNewProject)) {
                        int idx = SubPrintRes.indexOf(".");
                        String oRole = SubPrintRes.substring(5, idx);
                        // role has to be one of the authoring role - Creator and above
                        if (!rolesThatCannotAuthor.contains(oRole)) {
                            CanAuthor = true;
                            break;
                        }
                    }
                }
                if (toIdx != -1) {
                    fromIdx = toIdx + 1;
                    toIdx = PrintRes.indexOf("|", fromIdx);
                } else
                    fromIdx = toIdx;
            }

            if (CanAuthor)
                return 0;
            else {
                String sErr1 = sNoRightsOrNoValidAssignment;
                MqlUtil.mqlCommand(context, "notice $1", sErr1);
                return 1;
            }
        } else
            return 0;
    }

    public boolean checkTransferOwnershipAccessRules(matrix.db.Context context, String[] args) throws Exception {
        // System.out.println("In change owner trasnfer ownershp ..access function ....");
        boolean bRet = false;
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strObjId = (String) programMap.get("objectId");
            // System.out.println("programMap-->"+programMap);
            // System.out.println("strObjId-->"+strObjId);
            String language = (String) programMap.get("languageStr");
            MCADMxUtil util = new MCADMxUtil(context, null, new IEFGlobalCache());
            MCADGlobalConfigObject globalConfigObj = getGlobalConfigObject(context, strObjId, util);

            StringList slSel = new StringList(3);
            slSel.addElement(DomainObject.SELECT_ID);
            slSel.addElement(DomainObject.SELECT_TYPE);
            slSel.addElement("locked");
            DomainObject domTemp = DomainObject.newInstance(context, strObjId);
            Map mpObjInfo = (Map) domTemp.getInfo(context, slSel);
            // System.out.println("mpObjInfo...."+mpObjInfo);

            String busType = (String) mpObjInfo.get(DomainObject.SELECT_TYPE);
            String sLocked = (String) mpObjInfo.get("locked");
            boolean isLocked = Boolean.parseBoolean(sLocked);
            /*
             * BusinessObject boTemp = new BusinessObject(strObjId); boolean isLocked = boTemp.isLocked(context); String busType = boTemp.getTypeName();
             */
            // System.out.println("busType...."+busType);

            String cad_type = util.getCADTypeForBO(context, domTemp);

            boolean isMajorType = globalConfigObj.isMajorType(busType);
            boolean isFamilyLike = globalConfigObj.isTypeOfClass(cad_type, MCADAppletServletProtocol.TYPE_FAMILY_LIKE);
            boolean isInstLike = globalConfigObj.isTypeOfClass(cad_type, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE);
            // System.out.println("isMajorType...."+isMajorType);
            // System.out.println("isFamilyLike...."+isFamilyLike);
            // System.out.println("isInstLike...."+isInstLike);
            if (!isLocked) {
                if (isFamilyLike) {
                    if (isMajorType) {
                        bRet = true;
                    }
                } else if (isInstLike) {
                    bRet = false;
                } else {
                    if (isMajorType) {
                        bRet = true;
                    }
                }
            }
            // System.out.println("returing...."+bRet);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        return bRet;
    }

    public void propagateChangeOwnership(matrix.db.Context context, String[] args) throws Exception {
        return;
    }

}
