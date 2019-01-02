import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import com.matrixone.apps.common.BuyerDesk;
import com.matrixone.apps.common.Company;
import com.matrixone.apps.common.Person;
import com.matrixone.apps.common.Workspace;
import com.matrixone.apps.common.WorkspaceVault;
import com.matrixone.apps.domain.DomainAccess;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.AccessUtil;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkProperties;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.XSSUtil;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.apps.framework.ui.UIComponent;
import com.matrixone.apps.framework.ui.UIMenu;
import com.matrixone.apps.framework.ui.UINavigatorUtil;
import com.matrixone.apps.framework.ui.UIRTEUtil;
import com.matrixone.apps.framework.ui.UIUtil;
import com.matrixone.apps.team.WorkspaceTemplate;

import matrix.db.Attribute;
import matrix.db.AttributeList;
import matrix.db.AttributeType;
import matrix.db.BusinessObject;
import matrix.db.BusinessObjectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.RelationshipType;
import matrix.util.Pattern;
import matrix.util.StringList;
import pss.constants.TigerConstants;

public class PSS_emxWorkspaceTemplate_mxJPO extends emxWorkspaceTemplateBase_mxJPO {

    private static final long serialVersionUID = 1L;

    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PSS_emxWorkspaceTemplate_mxJPO.class);

    /**
     * displayTemplateData - method to display the template data like folders and members
     * @param context
     *            the eMatrix <code>Context</code> object
     * @return String
     * @throws Exception
     *             if the operation fails
     * @since R210
     * @grade 0
     */
    public PSS_emxWorkspaceTemplate_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
    }

    // TIGTK-7962, 8109 : VP : 2017-07-28 : START
    public static String displayTemplateData(Context context, String args[]) throws Exception {
        StringBuffer strTemplateData = new StringBuffer();
        try {
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            Map<?, ?> paramMap = (HashMap<?, ?>) programMap.get("paramMap");
            String language = (String) paramMap.get("languageStr");
            language = language == null ? (String) ((HashMap<?, ?>) programMap.get("requestMap")).get("languageStr") : language;
            String strMembers = i18nNow.getI18nString("emxTeamCentral.WorkspaceTemplateSaveDialog.Members", "emxTeamCentralStringResource", language);
            String strFolderStructure = i18nNow.getI18nString("emxTeamCentral.WorkspaceTemplateSaveDialog.FolderStructure", "emxTeamCentralStringResource", language);
            String strDocumentTemplate = i18nNow.getI18nString("emxTeamCentral.WorkspaceTemplateSaveDialog.PSS_DocumentTemplate", "emxTeamCentralStringResource", language);

            strTemplateData.append("<input type=checkbox name=\"addMembers\" id=\"addMembers\" value=\"\" checked onclick=\"javascript:templateDataSelected(this)\">"
                    + XSSUtil.encodeForHTML(context, strMembers) + "</br>");
            strTemplateData.append("<input type=checkbox name=\"addFolders\" id=\"addFolders\" value=\"\" checked onclick=\"javascript:templateDataSelected(this)\">"
                    + XSSUtil.encodeForHTML(context, strFolderStructure) + "</br>");
            strTemplateData.append("<input type=checkbox name=\"addDocumentTemplates\" id=\"addDocumentTemplates\" value=\"\" checked onclick=\"javascript:templateDataSelected(this)\">"
                    + XSSUtil.encodeForHTML(context, strDocumentTemplate));
            strTemplateData.append("<input type=hidden name=\"TemplateData\" id=\"TemplateData\" value=\"true|true|true\" >");

        } catch (Exception ex) {
            logger.error("Error from JPO - PSS_emxWorkspaceTemplate : displayTemplateData" + ex);
            throw ex;
        }
        return strTemplateData.toString();
    }

    /**
     * saveAsTemplateProcess - method to save the selected workspace as a workspace template
     * @param context
     *            the eMatrix <code>Context</code> object
     * @return void
     * @throws Exception
     *             if the operation fails
     * @since R210
     * @grade 0
     */
    @com.matrixone.apps.framework.ui.PostProcessCallable
    public HashMap saveAsTemplateProcess(Context context, String[] args) throws Exception {
        final String RELATIONSHIP_DATA_VAULT = PropertyUtil.getSchemaProperty(context, "relationship_ProjectVaults");
        final String RELATIONSHIP_SUB_VAULT = PropertyUtil.getSchemaProperty(context, "relationship_SubVaults");
        final String RELATIONSHIP_VAULTED_OBJECTS = PropertyUtil.getSchemaProperty(context, "relationship_VaultedDocuments");
        final String TYPE_PSS_DOCUMENTS = PropertyUtil.getSchemaProperty(context, "type_PSS_Document");
        final String TYPE_WORKSPACE_VAULTS = PropertyUtil.getSchemaProperty(context, "type_ProjectVault");
        final String ATTRIBUTE_PSS_TEMPLATE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Template");
        String strStateReleased = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", context.getLocale(), "emxFramework.State.Released");
        String strYes = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", context.getLocale(), "emxFramework.Range.PSS_Template.Yes");
        RelationshipType rtVaultedObjects = new RelationshipType(RELATIONSHIP_VAULTED_OBJECTS);

        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);

            HashMap requestMap = (HashMap) programMap.get("requestMap");
            String language = (String) requestMap.get("languageStr");
            language = language == null ? context.getLocale().getLanguage() : language;

            String strTemplateName = (String) requestMap.get("TemplateName");
            String strTemplateDesc = (String) requestMap.get("TemplateDescription");
            String strScope = (String) requestMap.get("Availability");
            String strOption = (String) requestMap.get("SaveOptions");
            String WorkspaceId = (String) requestMap.get("objectId");
            String templateData = (String) requestMap.get("TemplateData");
            String workspaceTemplateId = (String) requestMap.get("AvailableWorkspaceTemplates");
            String strAddMembers = "";
            String strAddFolders = "";
            String strAddDocumentTemplates = "";
            HashMap resultMap = new HashMap();
            boolean addMembers = false;
            boolean addFolders = false;
            boolean addDocumentTemplates = false;
            boolean bRevise = false;
            boolean bError = false;
            String strAlreadyExists = i18nNow.getI18nString("emxTeamCentral.WorkspaceTemplateSaveDialog.AlreadyExists", "emxTeamCentralStringResource", language);
            String strLargeValue = i18nNow.getI18nString("emxTeamCentral.WorkspaceTemplateSaveDialog.NameLength", "emxTeamCentralStringResource", language);

            WorkspaceTemplate workspaceTemplate = (WorkspaceTemplate) DomainObject.newInstance(context, DomainObject.TYPE_WORKSPACE_TEMPLATE, DomainObject.TEAM);
            StringList strTempData = FrameworkUtil.split(templateData, "|");
            strAddMembers = (String) strTempData.get(0);
            strAddFolders = (String) strTempData.get(1);
            strAddDocumentTemplates = (String) strTempData.get(2);
            if ("true".equals(strAddMembers)) {
                addMembers = true;
            }
            if ("true".equals(strAddFolders)) {
                addFolders = true;
            }
            if ("true".equals(strAddDocumentTemplates)) {
                addDocumentTemplates = true;
            }

            if (strOption != null && strOption.equals("yes")) {
                bRevise = false;
            } else {
                bRevise = true;
            }
            boolean addBuyerDeskMember = false;

            String addBuyerDeskMemberStr = FrameworkProperties.getProperty(context, "emxTeamCentral.ConvertBuyerDeskMembersToWorkspaceMembers");
            if (addBuyerDeskMemberStr == null) {
                addBuyerDeskMember = false;
            } else if ("true".equalsIgnoreCase(addBuyerDeskMemberStr.trim())) {
                addBuyerDeskMember = true;
            }
            if (!bRevise) {
                try {
                    boolean exists = WorkspaceTemplate.isWorkspaceTemplateExists(context, strTemplateName);
                    if (exists) {
                        throw new Exception("not unique");
                    }

                    workspaceTemplateId = createWorkspaceTemplate(context, WorkspaceId, strTemplateName, addMembers, addBuyerDeskMember, addFolders, strScope);
                } catch (Exception ex) {
                    bError = true;
                    String errorMessage = ex.getMessage();
                    if (errorMessage.indexOf("not unique") != -1) {
                        resultMap.put("Message", strAlreadyExists);
                    } else if (errorMessage.indexOf("inserted value too large for column") != -1) {
                        emxContextUtil_mxJPO.mqlNotice(context, strLargeValue);
                    } else {
                        emxContextUtil_mxJPO.mqlNotice(context, errorMessage);
                    }

                }

            } else {
                workspaceTemplateId = reviseWorkspaceTemplate(context, workspaceTemplateId, WorkspaceId, strTemplateName, addMembers, addBuyerDeskMember, addFolders, strScope);
            }

            if (!bError) {

                if (workspaceTemplateId != null && !"".equals(workspaceTemplateId) && !"null".equals(workspaceTemplateId)) {
                    workspaceTemplate.setId(workspaceTemplateId);
                    workspaceTemplate.open(context);
                    AttributeList templateAttrList = new AttributeList();
                    if (UIRTEUtil.isRTEEnabled(context, "Workspace Template", "description")) {
                        AttributeType attrType = new AttributeType("description_RTE");
                        Attribute attr = new Attribute(attrType, strTemplateDesc);
                        templateAttrList.add(attr);
                    } else {
                        workspaceTemplate.setDescription(strTemplateDesc);
                    }
                    workspaceTemplate.setAttributes(context, templateAttrList);
                    workspaceTemplate.setVault(context, context.getVault());
                    workspaceTemplate.update(context);
                    workspaceTemplate.close(context);
                }
            }

            Map<String, String> mReleasedDocumentTemplates = new HashMap<>();
            if (addDocumentTemplates) {
                DomainObject domWorkspace = DomainObject.newInstance(context, WorkspaceId);
                DomainObject domWorkspaceTemplate = DomainObject.newInstance(context, workspaceTemplateId);

                Pattern relPattern = new Pattern(RELATIONSHIP_DATA_VAULT);
                relPattern.addPattern(RELATIONSHIP_SUB_VAULT);
                relPattern.addPattern(RELATIONSHIP_VAULTED_OBJECTS);

                Pattern typPattern = new Pattern(TYPE_PSS_DOCUMENTS);
                typPattern.addPattern(TYPE_WORKSPACE_VAULTS);

                StringList slObjSelect = new StringList();
                slObjSelect.add(DomainConstants.SELECT_CURRENT);
                slObjSelect.add(DomainConstants.SELECT_ID);
                slObjSelect.add(DomainConstants.SELECT_NAME);
                slObjSelect.add("attribute[" + ATTRIBUTE_PSS_TEMPLATE + "]");
                slObjSelect.addElement("to[" + RELATIONSHIP_SUB_VAULT + "].from.name");

                StringList slRelSelect = new StringList();
                slRelSelect.add(DomainRelationship.SELECT_FROM_ID);

                MapList mpWorkspace = domWorkspace.getRelatedObjects(context, relPattern.getPattern(), typPattern.getPattern(), slObjSelect, slRelSelect, false, true, (short) 0, null, null,
                        (short) 0);

                for (int i = 0; i < mpWorkspace.size(); i++) {
                    Map<?, ?> mWorkspaceObj = (Map<?, ?>) mpWorkspace.get(i);
                    String strType = (String) mWorkspaceObj.get(DomainConstants.SELECT_TYPE);

                    if (!strType.equals(TYPE_PSS_DOCUMENTS)) {
                        String strFolderLevel = (String) mWorkspaceObj.get(DomainConstants.SELECT_LEVEL);
                        String strFolderName = (String) mWorkspaceObj.get(DomainConstants.SELECT_NAME);
                        String strParentFolderName = (String) mWorkspaceObj.get("to[" + RELATIONSHIP_SUB_VAULT + "].from.name");

                        String strMKey = strParentFolderName + strFolderName + strFolderLevel;
                        String strFolderId = (String) mWorkspaceObj.get(DomainConstants.SELECT_ID);
                        StringBuffer sbMpValue = new StringBuffer();
                        for (int j = 0; j < mpWorkspace.size(); j++) {
                            Map<?, ?> mDocObj = (Map<?, ?>) mpWorkspace.get(j);
                            String strDocType = (String) mDocObj.get(DomainConstants.SELECT_TYPE);
                            String strFromId = (String) mDocObj.get(DomainRelationship.SELECT_FROM_ID);
                            String strDocId = (String) mDocObj.get(DomainConstants.SELECT_ID);
                            String strDocState = (String) mDocObj.get(DomainConstants.SELECT_CURRENT);
                            String strIsTemplate = (String) mDocObj.get("attribute[" + ATTRIBUTE_PSS_TEMPLATE + "]");

                            if (strDocType.equals(TYPE_PSS_DOCUMENTS) && strFromId.equals(strFolderId) && strDocState.equals(strStateReleased) && strIsTemplate.equals(strYes)) {
                                // Findbug Issue correction start
                                // Date: 22/03/2017
                                // By: Asha G.
                                sbMpValue.append(strDocId);
                                sbMpValue.append("|");
                                // Findbug Issue correction end
                            }
                        }
                        String strMValue = sbMpValue.toString();
                        mReleasedDocumentTemplates.put(strMKey, strMValue);
                    }

                }

                if (mReleasedDocumentTemplates.size() != 0) {
                    MapList mpWorkspaceTemplate = domWorkspaceTemplate.getRelatedObjects(context, relPattern.getPattern(), typPattern.getPattern(), slObjSelect, slRelSelect, false, true, (short) 0,
                            null, null, (short) 0);

                    for (int i = 0; i < mpWorkspaceTemplate.size(); i++) {
                        Map<?, ?> mWorkspaceTemplateObj = (Map<?, ?>) mpWorkspaceTemplate.get(i);

                        String strTempParentFolderName = (String) mWorkspaceTemplateObj.get("to[" + RELATIONSHIP_SUB_VAULT + "].from.name");
                        String strTempFolderLevel = (String) mWorkspaceTemplateObj.get(DomainConstants.SELECT_LEVEL);
                        String strTempFolderName = (String) mWorkspaceTemplateObj.get(DomainConstants.SELECT_NAME);
                        String strWSTemplateFolderId = (String) mWorkspaceTemplateObj.get(DomainConstants.SELECT_ID);
                        DomainObject domFolder = DomainObject.newInstance(context, strWSTemplateFolderId);
                        String strTemplateKey = strTempParentFolderName + strTempFolderName + strTempFolderLevel;
                        if (mReleasedDocumentTemplates.containsKey(strTemplateKey) && mReleasedDocumentTemplates.get(strTemplateKey) != null
                                && !(mReleasedDocumentTemplates.get(strTemplateKey).equals(""))) {
                            String strDocumentTemplateIds = mReleasedDocumentTemplates.get(strTemplateKey);
                            String[] strSplit = strDocumentTemplateIds.split("\\|");
                            domFolder.addRelatedObjects(context, rtVaultedObjects, true, strSplit);
                        }
                    }
                }
            }

            return resultMap;
        } catch (Exception ex) {
            logger.error("Error from JPO - PSS_emxWorkspaceTemplate : saveAsTemplateProcess" + ex);
            throw ex;
        }
    }
    // TIGTK-7962, 8109 : VP : 2017-07-28 : END

    // TIGTK-7962, 8109 : VP : 2017-07-28 : START

    public String reviseWorkspaceTemplate(Context paramContext, String workspaceTemplateId, String WorkspaceId, String strTemplateName, boolean addMembers, boolean addBuyerDeskMember,
            boolean addFolders, String strScope) throws Exception {

        try {
            String strWorkspaceTemplateNewRevisionId = null;

            Workspace localWorkspace = null;
            DomainObject domWorkspaceTemplate = null;
            try {
                localWorkspace = (Workspace) DomainObject.newInstance(paramContext, TYPE_WORKSPACE, "Team");
                domWorkspaceTemplate = DomainObject.newInstance(paramContext, workspaceTemplateId, "Team");
            } catch (Exception localException2) {
                logger.error("Error from JPO - PSS_emxWorkspaceTemplate : reviseWorkspaceTemplate" + localException2);
                throw localException2;
            }
            localWorkspace.setId(WorkspaceId);

            Map localMap = domWorkspaceTemplate.createRevision(paramContext);
            String strWorkspaceTemplateRevisionID = (String) localMap.get("id");

            DomainObject domWorkspaceTemplateRevision = DomainObject.newInstance(paramContext, strWorkspaceTemplateRevisionID, "Team");

            String strMQLResult = MqlUtil.mqlCommand(paramContext, "expand bus $1 relationship $2", workspaceTemplateId, DomainConstants.RELATIONSHIP_WORKSPACE_TEMPLATE);

            if (UIUtil.isNotNullAndNotEmpty(strMQLResult)) {
                domWorkspaceTemplate.disconnect(paramContext, new RelationshipType(DomainConstants.RELATIONSHIP_WORKSPACE_TEMPLATE), true, localWorkspace);
                domWorkspaceTemplateRevision.connectTo(paramContext, DomainConstants.RELATIONSHIP_WORKSPACE_TEMPLATE, localWorkspace);
            } else {
                domWorkspaceTemplate.disconnect(paramContext, new RelationshipType(TigerConstants.RELATIONSHIP_PSS_DERIVED_WORKSPACE_TEMPLATE), false, localWorkspace);
                domWorkspaceTemplateRevision.connectFrom(paramContext, TigerConstants.RELATIONSHIP_PSS_DERIVED_WORKSPACE_TEMPLATE, localWorkspace);
            }
            if (addMembers) {
                try {
                    addMembersToTemplate(paramContext, domWorkspaceTemplateRevision, localWorkspace, addBuyerDeskMember);
                } catch (Exception Ex) {
                    logger.error("Error from JPO - PSS_emxWorkspaceTemplate : reviseWorkspaceTemplate" + Ex);
                    throw Ex;
                }
            } else {
                // TIGTK-13216 : Remove Ownership if addMember is false
                DomainAccess.clearMultipleOwnership(paramContext, strWorkspaceTemplateRevisionID);
            }

            if (addFolders) {
                addFoldersToTemplate(paramContext, domWorkspaceTemplateRevision, localWorkspace, addMembers);
                setFolderAccess(paramContext, domWorkspaceTemplateRevision.getObjectId(), WorkspaceId, strScope);
            }

            Person localPerson = Person.getPerson(paramContext);
            Company localCompany = localPerson.getCompany(paramContext);

            setAvailability(paramContext, strScope, localCompany, domWorkspaceTemplateRevision);

            strWorkspaceTemplateNewRevisionId = domWorkspaceTemplateRevision.getObjectId();
            return strWorkspaceTemplateNewRevisionId;
        } catch (Exception localException1) {
            logger.error("Error from JPO - PSS_emxWorkspaceTemplate : reviseWorkspaceTemplate" + localException1);
            throw localException1;
        }
    }

    public String createWorkspaceTemplate(Context paramContext, String paramString1, String paramString2, boolean paramBoolean1, boolean paramBoolean2, boolean paramBoolean3, String paramString3)
            throws Exception {
        String str1 = null;
        try {
            String str2 = "1";

            Workspace localWorkspace = (Workspace) DomainObject.newInstance(paramContext, TYPE_WORKSPACE, "Team");
            DomainObject localDomainObject1 = DomainObject.newInstance(paramContext);

            localDomainObject1.createObject(paramContext, TYPE_WORKSPACE_TEMPLATE, paramString2, str2, POLICY_WORKSPACE_TEMPLATE, getVault());
            localWorkspace.setId(paramString1);

            try {
                localDomainObject1.connectFrom(paramContext, TigerConstants.RELATIONSHIP_PSS_DERIVED_WORKSPACE_TEMPLATE, localWorkspace);
            } catch (Exception localException3) {
                logger.error("Error from JPO - PSS_emxWorkspaceTemplate : createWorkspaceTemplate" + localException3);
                throw localException3;
            }

            if (paramBoolean1) {
                try {
                    addMembersToTemplate(paramContext, localDomainObject1, localWorkspace, paramBoolean2);
                } catch (Exception localException4) {
                    logger.error("Error from JPO - PSS_emxWorkspaceTemplate : createWorkspaceTemplate" + localException4);
                    throw localException4;
                }

            }

            if (paramBoolean3) {
                addFoldersToTemplate(paramContext, localDomainObject1, localWorkspace, paramBoolean1);
                setFolderAccess(paramContext, localDomainObject1.getObjectId(), paramString1, paramString3);
            }

            Person localPerson = Person.getPerson(paramContext);
            Company localCompany = localPerson.getCompany(paramContext);
            setAvailability(paramContext, paramString3, localCompany, localDomainObject1);

            str1 = localDomainObject1.getObjectId();
        } catch (Exception localException1) {
            logger.error("Error from JPO - PSS_emxWorkspaceTemplate : createWorkspaceTemplate" + localException1);
            throw localException1;
        }
        return str1;
    }

    protected void addFoldersToTemplate(Context paramContext, DomainObject paramDomainObject1, DomainObject paramDomainObject2) throws Exception {
        addFoldersToTemplate(paramContext, paramDomainObject1, paramDomainObject2, false);
    }

    protected void addFoldersToTemplate(Context paramContext, DomainObject paramDomainObject1, DomainObject paramDomainObject2, boolean paramBoolean) throws Exception {
        try {
            // DomainObject localDomainObject = DomainObject.newInstance(paramContext);

            StringList localStringList1 = new StringList();

            localStringList1.add("name");
            localStringList1.add("id");

            String str1 = RELATIONSHIP_WORKSPACE_VAULTS;
            String str2 = TYPE_PROJECT_VAULT;

            StringList localStringList2 = new StringList();
            localStringList2.add("id");

            MapList localMapList = paramDomainObject2.getRelatedObjects(paramContext, str1, str2, localStringList1, null, false, true, (short) 1, "", "", null, null, null);

            // String str3 = paramDomainObject2.getVault();
            Iterator localIterator = localMapList.iterator();

            while (localIterator.hasNext()) {
                Hashtable localHashtable = (Hashtable) localIterator.next();
                String str4 = (String) localHashtable.get("id");
                String str5 = (String) localHashtable.get("name");

                WorkspaceVault localWorkspaceVault = (WorkspaceVault) DomainObject.newInstance(paramContext, TYPE_WORKSPACE_VAULT, "Team");

                localWorkspaceVault.setId(str4);
                try {
                    localWorkspaceVault.cloneFolderStructure(paramContext, str5, paramDomainObject1, RELATIONSHIP_WORKSPACE_VAULTS, paramBoolean);
                } catch (Exception localException2) {
                    logger.error("Error from JPO - PSS_emxWorkspaceTemplate : addFoldersToTemplate::" + localException2);
                    throw localException2;
                }
            }
        } catch (Exception localException1) {
            logger.error("Error from JPO - PSS_emxWorkspaceTemplate : addFoldersToTemplate::" + localException1);
            throw localException1;
        }
    }

    private void setFolderAccess(Context paramContext, String paramString1, String paramString2, String paramString3) throws Exception {
        String str = "true";
        try {
            str = EnoviaResourceBundle.getProperty(paramContext, "emxComponents.NewSecurityModel");
        } catch (Exception localException) {
            logger.error("Error from JPO - PSS_emxWorkspaceTemplate : setFolderAccess::" + localException);
        }
        if ("true".equalsIgnoreCase(str)) {
            setFolderAccess_new(paramContext, paramString1, paramString2);
        } else {
            setFolderAccess_old(paramContext, paramString1, paramString3);
        }
    }

    private void setFolderAccess_old(Context paramContext, String paramString1, String paramString2) throws Exception {
        try {
            DomainObject localDomainObject1 = DomainObject.newInstance(paramContext, paramString1, "Team");

            StringList localStringList1 = new StringList();
            localStringList1.add("id");

            Pattern localPattern1 = new Pattern(TYPE_PROJECT_VAULT);
            Pattern localPattern2 = new Pattern(RELATIONSHIP_WORKSPACE_VAULTS);
            localPattern2.addPattern(RELATIONSHIP_SUB_VAULTS);

            MapList localMapList = localDomainObject1.getRelatedObjects(paramContext, localPattern2.getPattern(), localPattern1.getPattern(), localStringList1, null, false, true, (short) 0, "", "",
                    null, null, null);

            Iterator localIterator1 = localMapList.iterator();

            while (localIterator1.hasNext()) {
                StringList localStringList2 = new StringList();
                BusinessObjectList localBusinessObjectList = new BusinessObjectList();

                Map localMap = (Map) localIterator1.next();
                String str1 = (String) localMap.get("id");
                if (UIUtil.isNotNullAndNotEmpty(str1)) {
                    BusinessObject localDomainObject2 = new BusinessObject(str1);
                    localBusinessObjectList.addElement(localDomainObject2);

                    Iterator localIterator2 = localDomainObject2.getGrantees(paramContext).iterator();
                    while (localIterator2.hasNext()) {
                        String str2 = (String) localIterator2.next();
                        if (!(localStringList2.contains(str2))) {
                            localStringList2.addElement(str2);
                        }
                    }

                    AccessUtil.revokeAccess(paramContext, localBusinessObjectList, localStringList2);
                }
            }
        } catch (RuntimeException e) {
            logger.error("Error from JPO - PSS_emxWorkspaceTemplate : setFolderAccess_old::" + e);
            throw e;
        } catch (Exception localException1) {
            logger.error("Error from JPO - PSS_emxWorkspaceTemplate : setFolderAccess_old::" + localException1);
            throw localException1;
        }
    }

    private void setFolderAccess_new(Context paramContext, String paramString1, String paramString2) throws Exception {
        try {
            DomainObject localDomainObject = DomainObject.newInstance(paramContext, paramString1, "Team");

            StringList localStringList = new StringList();
            localStringList.add("id");

            Pattern localPattern1 = new Pattern(TYPE_PROJECT_VAULT);
            Pattern localPattern2 = new Pattern(RELATIONSHIP_WORKSPACE_VAULTS);
            localPattern2.addPattern(RELATIONSHIP_SUB_VAULTS);

            MapList localMapList = localDomainObject.getRelatedObjects(paramContext, localPattern2.getPattern(), localPattern1.getPattern(), localStringList, null, false, true, (short) 0, "", "",
                    null, null, null);

            Iterator localIterator = localMapList.iterator();
            while (localIterator.hasNext()) {
                Map localMap = (Map) localIterator.next();
                String str = (String) localMap.get("id");
                DomainAccess.deleteObjectOwnership(paramContext, str, paramString2, null);
            }

        } catch (Exception localException) {
            logger.error("Error from JPO - PSS_emxWorkspaceTemplate : setFolderAccess_new::" + localException);
            throw localException;
        }
    }

    public void setAvailability(Context paramContext, String paramString, Company paramCompany, DomainObject paramDomainObject) throws Exception {
        try {
            paramDomainObject.open(paramContext);
            String str = paramDomainObject.getInfo(paramContext, "to[" + RELATIONSHIP_ORGANIZATION_TEMPLATE + "].id");
            if ("Enterprise".equals(paramString)) {
                if (str == null) {
                    paramCompany.connectTo(paramContext, RELATIONSHIP_ORGANIZATION_TEMPLATE, paramDomainObject);
                }
            } else if (str != null) {
                DomainRelationship.disconnect(paramContext, str);
            }

            paramDomainObject.close(paramContext);
        } catch (RuntimeException e) {
            logger.error("Error from JPO - PSS_emxWorkspaceTemplate : setAvailability::" + e);
            throw e;
        } catch (Exception localException) {
            logger.error("Error from JPO - PSS_emxWorkspaceTemplate : setAvailability::" + localException);
            throw localException;
        }
    }

    protected void addMembersToTemplate(Context paramContext, DomainObject paramDomainObject1, DomainObject paramDomainObject2, boolean paramBoolean) throws Exception {
        String str = "true";
        try {
            str = EnoviaResourceBundle.getProperty(paramContext, "emxComponents.NewSecurityModel");
        } catch (Exception localException) {
            logger.error("Error from JPO - PSS_emxWorkspaceTemplate : addMembersToTemplate::" + localException);
        }
        if ("true".equalsIgnoreCase(str)) {
            addMembersToTemplate_new(paramContext, paramDomainObject1, paramDomainObject2, paramBoolean);
        } else
            addMembersToTemplate_old(paramContext, paramDomainObject1, paramDomainObject2, paramBoolean);
    }

    protected void addMembersToTemplate_new(Context paramContext, DomainObject paramDomainObject1, DomainObject paramDomainObject2, boolean paramBoolean) throws Exception {
        try {
            ContextUtil.pushContext(paramContext);
            String str1 = paramDomainObject2.getObjectId(paramContext);
            MapList localMapList1 = DomainAccess.getAccessSummaryList(paramContext, str1);
            Iterator localIterator1 = localMapList1.iterator();
            String str2 = "";
            String str3 = paramDomainObject1.getObjectId(paramContext);
            while (localIterator1.hasNext()) {
                Map localObject1 = (Map) localIterator1.next();
                String localObject2 = (String) ((Map) localObject1).get("org");
                String localObject3 = (String) ((Map) localObject1).get("name");
                String localObject4 = (String) ((Map) localObject1).get("project");
                if (!(((String) localObject3).contains((CharSequence) localObject4))) {
                    localObject4 = localObject3;
                }

                if (!(((String) localObject4).equals(MqlUtil.mqlCommand(paramContext, "list role $1", new String[] { localObject4 })))) {
                    localObject4 = localObject3;
                }

                String localObject5 = (String) ((Map) localObject1).get("access");
                str2 = (String) ((Map) localObject1).get("comment");

                DomainAccess.createObjectOwnership(paramContext, str3, (String) localObject2, (String) localObject4, (String) localObject5, str2);
            }

            Object localObject1 = new HashSet();

            Object localObject2 = new Pattern(TYPE_PERSON);
            Object localObject3 = new Pattern(PropertyUtil.getSchemaProperty(paramContext, "relationship_WorkspaceMember"));

            Object localObject4 = new StringList();
            ((StringList) localObject4).addElement("id");

            Object localObject5 = paramDomainObject2.getRelatedObjects(paramContext, ((Pattern) localObject3).getPattern(), ((Pattern) localObject2).getPattern(), (StringList) localObject4, null,
                    false, true, (short) 1, "", "", null, null, null);

            Iterator localIterator2 = ((MapList) localObject5).iterator();
            while (localIterator2.hasNext()) {
                Map localObject6 = (Map) localIterator2.next();
                ((HashSet) localObject1).add((String) ((Map) localObject6).get("id"));
            }
            String str4;
            Object localObject7;
            if (paramBoolean) {
                String localObject6 = "from[" + RELATIONSHIP_WORKSPACE_BUYER_DESK + "].to.id";
                str4 = "";
                try {
                    str4 = paramDomainObject2.getInfo(paramContext, (String) localObject6);
                } catch (Exception localException2) {
                    logger.error("Error from JPO - PSS_emxWorkspaceTemplate : addMembersToTemplate_new::" + localException2);
                }
                if (str4 != null) {
                    localObject7 = (BuyerDesk) DomainObject.newInstance(paramContext, str4);
                    StringList localStringList = new StringList(1);
                    localStringList.addElement("id");
                    localStringList.addElement("name");
                    Pattern localPattern1 = new Pattern(RELATIONSHIP_ASSIGNED_BUYER);
                    Pattern localPattern2 = new Pattern(TYPE_PERSON);

                    MapList localMapList2 = ((BuyerDesk) localObject7).getRelatedObjects(paramContext, localPattern1.getPattern(), localPattern2.getPattern(), localStringList, null, true, false,
                            (short) 1, "", "", null, null, null);

                    Iterator localIterator3 = localMapList2.iterator();
                    while (localIterator3.hasNext()) {
                        Hashtable localHashtable = (Hashtable) localIterator3.next();
                        String str5 = (String) localHashtable.get("id");
                        ((HashSet) localObject1).add(str5);
                        DomainAccess.createObjectOwnership(paramContext, str3, str5, "Basic", str2);
                    }
                }
            }
            Object localObject6 = ((HashSet) localObject1).iterator();

            while (((Iterator) localObject6).hasNext()) {
                str4 = (String) ((Iterator) localObject6).next();
                localObject7 = (Person) DomainObject.newInstance(paramContext, str4);

                paramDomainObject1.connectTo(paramContext, RELATIONSHIP_WORKSPACE_TEMPLATE_MEMBER, (DomainObject) localObject7);
            }

        } catch (Exception localException1) {
            logger.error("Error from JPO - PSS_emxWorkspaceTemplate : addMembersToTemplate_new::" + localException1);
        } finally {
            ContextUtil.popContext(paramContext);
        }
    }

    protected void addMembersToTemplate_old(Context paramContext, DomainObject paramDomainObject1, DomainObject paramDomainObject2, boolean paramBoolean) throws Exception {
        try {
            // String str1 = paramDomainObject2.getName();

            Pattern localPattern1 = new Pattern(TYPE_PROJECT_MEMBER);
            Pattern localPattern2 = new Pattern(RELATIONSHIP_PROJECT_MEMBERS);

            StringList localStringList = new StringList();

            localStringList.add("id");
            localStringList.add("attribute[" + ATTRIBUTE_PROJECT_ACCESS + "]");
            localStringList.add("attribute[" + ATTRIBUTE_CREATE_ROUTE + "]");
            localStringList.add("to[" + RELATIONSHIP_PROJECT_MEMBERSHIP + "].from.id");

            MapList localMapList = paramDomainObject2.getRelatedObjects(paramContext, localPattern2.getPattern(), localPattern1.getPattern(), localStringList, null, false, true, (short) 1, "", "",
                    null, null, null);

            ArrayList localArrayList = new ArrayList();
            String str2 = "from[" + RELATIONSHIP_WORKSPACE_BUYER_DESK + "].to.id";
            String str3 = "";
            try {
                str3 = paramDomainObject2.getInfo(paramContext, str2);
            } catch (Exception localException2) {
                logger.error("Error from JPO - PSS_emxWorkspaceTemplate : addMembersToTemplate_old::" + localException2);
            }
            Object localObject3;
            Object localObject4;
            Object localObject5;
            Object localObject6;
            Object localObject7;
            Object localObject8;
            if (str3 != null) {
                BuyerDesk localObject1 = (BuyerDesk) DomainObject.newInstance(paramContext, str3);
                StringList localObject2 = new StringList(1);
                ((StringList) localObject2).add("id");
                localObject3 = new Pattern(RELATIONSHIP_ASSIGNED_BUYER);
                localObject4 = new Pattern(TYPE_PERSON);

                localObject5 = ((BuyerDesk) localObject1).getRelatedObjects(paramContext, ((Pattern) localObject3).getPattern(), ((Pattern) localObject4).getPattern(), (StringList) localObject2, null,
                        true, false, (short) 1, "", "", null, null, null);

                localObject6 = ((MapList) localObject5).iterator();
                while (((Iterator) localObject6).hasNext()) {
                    localObject7 = (Hashtable) ((Iterator) localObject6).next();
                    localObject8 = (String) ((Hashtable) localObject7).get("id");
                    localArrayList.add(localObject8);
                }

            }

            Object localObject1 = new ArrayList();

            Object localObject2 = localMapList.iterator();

            while (((Iterator) localObject2).hasNext()) {
                localObject3 = (Hashtable) ((Iterator) localObject2).next();

                localObject4 = (String) ((Hashtable) localObject3).get("to[" + RELATIONSHIP_PROJECT_MEMBERSHIP + "].from.id");
                localObject5 = (String) ((Hashtable) localObject3).get("attribute[" + ATTRIBUTE_CREATE_ROUTE + "]");
                localObject6 = (String) ((Hashtable) localObject3).get("attribute[" + ATTRIBUTE_PROJECT_ACCESS + "]");
                if (!(localArrayList.contains(localObject4))) {
                    localObject7 = (Person) DomainObject.newInstance(paramContext, (String) localObject4);

                    localObject8 = paramDomainObject1.connectTo(paramContext, RELATIONSHIP_WORKSPACE_TEMPLATE_MEMBER, (DomainObject) localObject7);
                    ((ArrayList) localObject1).add(localObject4);

                    if (localObject8 != null) {
                        AttributeList localAttributeList = new AttributeList();
                        Attribute localAttribute1 = new Attribute(new AttributeType(ATTRIBUTE_CREATE_ROUTE), (String) localObject5);
                        Attribute localAttribute2 = new Attribute(new AttributeType(ATTRIBUTE_PROJECT_ACCESS), (String) localObject6);
                        localAttributeList.addElement(localAttribute1);
                        localAttributeList.addElement(localAttribute2);
                        ((DomainRelationship) localObject8).setAttributes(paramContext, localAttributeList);
                    }
                }
            }

            if (paramBoolean) {
                for (int i = 0; i < localArrayList.size(); ++i) {
                    localObject4 = (String) localArrayList.get(i);
                    if (((ArrayList) localObject1).contains(localObject4))
                        continue;
                    localObject5 = (Person) DomainObject.newInstance(paramContext, (String) localObject4);

                    paramDomainObject1.connectTo(paramContext, RELATIONSHIP_WORKSPACE_TEMPLATE_MEMBER, (DomainObject) localObject5);
                }

            }

        } catch (RuntimeException e) {
            logger.error("Error from JPO - PSS_emxWorkspaceTemplate : addMembersToTemplate_old::" + e);
            throw e;
        } catch (Exception localException1) {
            logger.error("Error from JPO - PSS_emxWorkspaceTemplate : addMembersToTemplate_old::" + localException1);
            throw localException1;
        }
    }

    /**
     * displaySaveOptions - method to display the save options for workspace template - User or Enterprise
     * @param context
     *            the eMatrix <code>Context</code> object
     * @return String
     * @throws Exception
     *             if the operation fails
     * @since R210
     * @grade 0
     */
    public static String displaySaveOptions(Context context, String args[]) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        Map paramMap = (HashMap) programMap.get("paramMap");
        String language = (String) paramMap.get("languageStr");
        language = language == null ? (String) ((HashMap) programMap.get("requestMap")).get("languageStr") : language;
        // String relOrgTemplate = DomainObject.RELATIONSHIP_ORGANIZATION_TEMPLATE;
        HashMap requestMap = (HashMap) programMap.get("requestMap");
        String objectId = (String) requestMap.get("objectId");
        DomainObject dob = DomainObject.newInstance(context, objectId);
        StringList slWorkplaceTemplateLst = (StringList) dob.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_DERIVED_WORKSPACE_TEMPLATE + "].to.id");

        String sworkspaceTemplateId = dob.getInfo(context, "to[" + DomainConstants.RELATIONSHIP_WORKSPACE_TEMPLATE + "].from.id");

        if (UIUtil.isNotNullAndNotEmpty(sworkspaceTemplateId))
            slWorkplaceTemplateLst.add(sworkspaceTemplateId);

        // String mode = (String) requestMap.get("mode");
        String strSaveTemplate = i18nNow.getI18nString("emxTeamCentral.SaveTemplateDialog.SavenewTemp", "emxTeamCentralStringResource", language);
        String reviseTemp = i18nNow.getI18nString("emxTeamCentral.SaveTemplateDialog.ReviseTemp", "emxTeamCentralStringResource", language);
        StringBuffer strOptions = new StringBuffer();
        // com.matrixone.apps.common.Person person = com.matrixone.apps.common.Person.getPerson(context);
        // Company company = person.getCompany(context);
        // String strCompanyId = company.getObjectId();
        String strWorkspaceTempName = "";
        String strWorkspaceTempDesc = "";
        String strUserChecked = "";
        String strEnterpriseChecked = "";
        String workspaceTemplateId = "";

        // String strScope = (String) requestMap.get("rb");
        boolean bRevise = false;
        String reviseChecked = "";
        String newChecked = "checked";
        DomainObject boTemplate = null;
        String disable = "";
        StringBuilder sbAvailableTemplates = new StringBuilder();

        if (slWorkplaceTemplateLst != null && slWorkplaceTemplateLst.size() > 0) {

            for (int i = 0; i < slWorkplaceTemplateLst.size(); i++) {
                workspaceTemplateId = (String) slWorkplaceTemplateLst.getElement(i);
                boTemplate = DomainObject.newInstance(context, workspaceTemplateId, DomainConstants.TEAM);

                boTemplate.open(context);
                strWorkspaceTempName = boTemplate.getName(context);
                strWorkspaceTempDesc = boTemplate.getInfo(context, boTemplate.SELECT_DESCRIPTION);
                String companyId = boTemplate.getInfo(context, "to[" + boTemplate.RELATIONSHIP_ORGANIZATION_TEMPLATE + "].from.id");
                if (companyId == null) {
                    // strScope = "User";
                    strUserChecked = "checked";
                } else {
                    strEnterpriseChecked = "checked";
                    strUserChecked = "";
                }

                String hasReviseAccess = boTemplate.getInfo(context, "current.access[revise]");
                if (hasReviseAccess.equals("TRUE")) {
                    if (!bRevise) {
                        sbAvailableTemplates.append("<script language=\"javascript\" src=\"../components/emxComponentsJSFunctions.js\"></script>");
                        sbAvailableTemplates.append("<select name='AvailableWorkspaceTemplates' onchange=\"setNameAndDescriptionOfWorkplaceTemplate()\">");
                    }

                    sbAvailableTemplates.append("<option ");
                    sbAvailableTemplates.append(" id='");
                    sbAvailableTemplates.append(XSSUtil.encodeForHTMLAttribute(context, workspaceTemplateId));
                    sbAvailableTemplates.append("'");
                    sbAvailableTemplates.append(" description='");
                    sbAvailableTemplates.append(XSSUtil.encodeForHTMLAttribute(context, strWorkspaceTempDesc));
                    sbAvailableTemplates.append("'");
                    sbAvailableTemplates.append(" name='");
                    sbAvailableTemplates.append(XSSUtil.encodeForHTMLAttribute(context, strWorkspaceTempName));
                    sbAvailableTemplates.append("'");
                    sbAvailableTemplates.append(" value='");
                    sbAvailableTemplates.append(XSSUtil.encodeForHTMLAttribute(context, workspaceTemplateId));
                    sbAvailableTemplates.append("'>");
                    sbAvailableTemplates.append(XSSUtil.encodeForHTMLAttribute(context, strWorkspaceTempName));
                    sbAvailableTemplates.append("</option>");

                    bRevise = true;

                }
            }

            if (bRevise) {
                sbAvailableTemplates.append("</select>");
                newChecked = "";
                reviseChecked = "checked";
                strOptions.append("<input type = \"radio\" name = \"SaveOptions\" id = \"SaveOptions\" value = \"yes\"" + XSSUtil.encodeForHTMLAttribute(context, newChecked) + ">"
                        + XSSUtil.encodeForHTML(context, strSaveTemplate) + "</br>");
                strOptions.append("<input type = \"radio\" name = \"SaveOptions\" id = \"SaveOptions\" value = \"no\"" + XSSUtil.encodeForHTMLAttribute(context, disable) + ""
                        + XSSUtil.encodeForHTML(context, reviseChecked) + ">");
                strOptions.append(reviseTemp);

                strOptions.append(sbAvailableTemplates);
            } else {
                strOptions.append("<input type = \"radio\" name = \"SaveOptions\" id = \"SaveOptions\" value = \"yes\"" + newChecked + ">" + strSaveTemplate + "</br>");
            }
        } else {

            if (!bRevise)
                disable = "disabled";
            strOptions.append("<input type = \"radio\" name = \"SaveOptions\" id = \"SaveOptions\" value = \"yes\"" + XSSUtil.encodeForHTMLAttribute(context, newChecked) + ">"
                    + XSSUtil.encodeForHTML(context, strSaveTemplate) + "</br>");
        }
        return strOptions.toString();
    }

    /**
     * getTemplateName - method to return the workspace template name if already connected to workspace
     * @param context
     *            the eMatrix <code>Context</code> object
     * @return String
     * @throws Exception
     *             if the operation fails
     * @since R210
     * @grade 0
     */
    public String getTemplatename(Context context, String[] args) throws Exception {
        StringBuffer templateNames = new StringBuffer();
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap paramMap = (HashMap) programMap.get("paramMap");
        String objectId = (String) paramMap.get("objectId");

        DomainObject dob = DomainObject.newInstance(context, objectId);
        StringList slWorkplaceTemplateLst = (StringList) dob.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_DERIVED_WORKSPACE_TEMPLATE + "].to.id");

        String sworkspaceTemplateId = dob.getInfo(context, "to[" + DomainConstants.RELATIONSHIP_WORKSPACE_TEMPLATE + "].from.id");
        if (UIUtil.isNotNullAndNotEmpty(sworkspaceTemplateId))
            slWorkplaceTemplateLst.add(sworkspaceTemplateId);

        if (slWorkplaceTemplateLst != null && slWorkplaceTemplateLst.size() > 0) {
            String workspaceTemplateId = (String) slWorkplaceTemplateLst.getElement(0);
            DomainObject boTemplate = DomainObject.newInstance(context, workspaceTemplateId, DomainConstants.TEAM);
            boTemplate.open(context);
            String strWorkspaceTempName = boTemplate.getName(context);
            templateNames.append("<input type=\"text\" name=\"TemplateName\" value=\"" + XSSUtil.encodeForHTMLAttribute(context, strWorkspaceTempName) + "\">");
        } else {
            templateNames.append("<input type=\"text\" name=\"TemplateName\" value=\"\">");
        }
        return templateNames.toString();
    }

    /**
     * getTemplateDescription - method to return the workspace template description if already connected to workspace
     * @param context
     *            the eMatrix <code>Context</code> object
     * @return String
     * @throws Exception
     *             if the operation fails
     * @since R210
     * @grade 0
     */
    public String getTemplateDescription(Context context, String[] args) throws Exception {
        StringBuffer templateDesc = new StringBuffer();
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap paramMap = (HashMap) programMap.get("paramMap");
        String objectId = (String) paramMap.get("objectId");
        DomainObject dob = DomainObject.newInstance(context, objectId);

        boolean enableRichTextEditor = UIRTEUtil.isRTEEnabled(context, "Workspace Template", "description");
        String rteClass = enableRichTextEditor ? "class=\"rte\"" : "";

        StringList slWorkplaceTemplateLst = (StringList) dob.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_DERIVED_WORKSPACE_TEMPLATE + "].to.id");

        String sworkspaceTemplateId = dob.getInfo(context, "to[" + DomainConstants.RELATIONSHIP_WORKSPACE_TEMPLATE + "].from.id");

        if (UIUtil.isNotNullAndNotEmpty(sworkspaceTemplateId))
            slWorkplaceTemplateLst.add(sworkspaceTemplateId);

        if (slWorkplaceTemplateLst != null && slWorkplaceTemplateLst.size() > 0) {
            String workspaceTemplateId = (String) slWorkplaceTemplateLst.getElement(0);
            DomainObject boTemplate = DomainObject.newInstance(context, workspaceTemplateId, DomainConstants.TEAM);
            boTemplate.open(context);
            String strWorkspaceTempDesc = boTemplate.getInfo(context, boTemplate.SELECT_DESCRIPTION);
            templateDesc.append("<textarea ");
            templateDesc.append(rteClass);
            templateDesc.append(" name=\"TemplateDescription\" rows = \"5\" cols = \"25\" value=\"");
            templateDesc.append(XSSUtil.encodeForHTMLAttribute(context, strWorkspaceTempDesc));
            templateDesc.append("\">");
            templateDesc.append(XSSUtil.encodeForHTML(context, strWorkspaceTempDesc));
            templateDesc.append("</textarea>");
        } else {
            templateDesc.append("<textarea ");
            templateDesc.append(rteClass);
            templateDesc.append(" name=\"TemplateDescription\" rows = \"5\" cols = \"25\" value=\"\"></textarea>");
        }
        return templateDesc.toString();
    }

    public String showRelatedObjectsCountAndHyperlink(Context context, String[] args) throws Exception {
        String strFieldValue = DomainObject.EMPTY_STRING;
        try {
            HashMap<?, ?> mpProgramMap = (HashMap) JPO.unpackArgs(args);

            HashMap<?, ?> requestMap = (HashMap<?, ?>) mpProgramMap.get("requestMap");
            String strObjectId = (String) requestMap.get("objectId");
            HashMap<?, ?> mpFieldMap = (HashMap<?, ?>) mpProgramMap.get("fieldMap");
            Map mpSettingsMap = (Map) mpFieldMap.get("settings");
            String strTableName = (String) mpSettingsMap.get("PSS_TableName");
            String relToTraverse = PropertyUtil.getSchemaProperty(context, (String) mpSettingsMap.get("PSS_Relationship"));
            String direction = (String) mpSettingsMap.get("PSS_Direction");
            String typeToTraverse = (String) mpSettingsMap.get("PSS_Type");
            StringList slTypeToTraverse = FrameworkUtil.split(typeToTraverse, ",");
            int iTypeToTraveseSize = slTypeToTraverse.size();
            Pattern typePattern = null;
            for (int i = 0; i < iTypeToTraveseSize; i++) {
                String strTypeToTraverse = (String) slTypeToTraverse.get(i);
                String strType = PropertyUtil.getSchemaProperty(context, strTypeToTraverse);
                // type Pattern
                if (i == 0) {
                    typePattern = new Pattern(strType);
                } else {
                    typePattern.addPattern(strType);
                }
            }
            boolean getFrom;
            boolean getTo;
            if (direction.equalsIgnoreCase("from")) {
                getFrom = true;
                getTo = false;
            } else {
                getFrom = false;
                getTo = true;
            }

            if (UIUtil.isNotNullAndNotEmpty(strObjectId)) {
                DomainObject domObject = DomainObject.newInstance(context, strObjectId);
                // Object Selects
                StringList slObjectSelect = new StringList();
                slObjectSelect.add(DomainConstants.SELECT_ID);
                slObjectSelect.add(DomainConstants.SELECT_NAME);
                // Relationship Selects
                StringList slRelSelect = new StringList();
                slRelSelect.add(DomainConstants.SELECT_RELATIONSHIP_ID);
                // Relationship Pattern
                Pattern relationshipPattern = new Pattern(relToTraverse);
                // Get Related Objects of Part
                MapList mlRelatedObjectsList = domObject.getRelatedObjects(context, // context
                        relationshipPattern.getPattern(), // relationship
                        // pattern
                        typePattern.getPattern(), // object pattern
                        slObjectSelect, // object selects
                        slRelSelect, // relationship selects
                        getTo, // to direction
                        getFrom, // from direction
                        (short) 0, // recursion level
                        null, // object where clause
                        null, // relationship where clause
                        (short) 0, false, // checkHidden
                        true, // preventDuplicates
                        (short) 1000, // pageSize
                        null, null, null, null);

                if (mlRelatedObjectsList != null && !mlRelatedObjectsList.isEmpty()) {
                    int intSize = mlRelatedObjectsList.size();
                    StringBuffer sbFinalHref = new StringBuffer("../common/emxIndentedTable.jsp?");

                    StringBuffer sbBuffer = new StringBuffer();
                    sbBuffer.append("<a href=\"JavaScript:emxFormLinkClick('");
                    sbBuffer.append(sbFinalHref.toString().replace("&", "&amp;"));
                    sbBuffer.append("&amp;objectId=");
                    sbBuffer.append(XSSUtil.encodeForHTMLAttribute(context, strObjectId));
                    sbBuffer.append("&amp;table=");
                    sbBuffer.append(strTableName);
                    sbBuffer.append("&amp;relationship=");
                    sbBuffer.append(relationshipPattern.getPattern());
                    sbBuffer.append("&amp;direction=");
                    sbBuffer.append(direction);
                    sbBuffer.append("','popup', '', '', '')\">");
                    sbBuffer.append(XSSUtil.encodeForHTML(context, String.valueOf(intSize)));
                    sbBuffer.append("</a>");
                    strFieldValue = sbBuffer.toString();

                } else {
                    strFieldValue = EMPTY_STRING;
                }
            }

        } catch (Exception e) {
            logger.error("Error from JPO - PSS_emxWorkspaceTemplate : showRelatedObjectsCountAndHyperlink:: " + e);
        }
        return strFieldValue;
    }
    // TIGTK-7962, 8109 : VP : 2017-07-28 : END
}
