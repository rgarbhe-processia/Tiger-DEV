import pss.constants.TigerConstants;
import pss.document.policy.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.servlet.http.HttpSession;
import com.matrixone.apps.common.Workspace;
import com.matrixone.apps.domain.DomainAccess;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkProperties;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.apps.framework.ui.UINavigatorUtil;
import com.matrixone.apps.framework.ui.UIUtil;
import com.matrixone.apps.team.TeamUtil;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.RelationshipType;
import matrix.util.MatrixException;
import matrix.util.Pattern;
import matrix.util.StringList;

public class PSS_emxWorkspace_mxJPO extends emxWorkspaceBase_mxJPO {

    // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PSS_emxWorkspace_mxJPO.class);

    // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - END
    // TIGTK-7962, 8109 : VP : 2017-07-28 : START - Modified for Findbug
    private static final long serialVersionUID = 1L;

    // TIGTK-7962, 8109 : VP : 2017-07-28 : END
    public PSS_emxWorkspace_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
    }

    private static final HttpSession HttpServletRequest = null;

    @com.matrixone.apps.framework.ui.CreateProcessCallable
    /**
     * Added this function to Create Workspace from the Workspace Template.
     * @param context
     * @param args
     * @return Map
     * @throws Exception
     */
    public Map<?, ?> createWorkspaceProcess(Context context, String[] args) throws Exception {

        final String RELATIONSHIP_DATA_VAULT = PropertyUtil.getSchemaProperty(context, "relationship_ProjectVaults");
        final String RELATIONSHIP_SUB_VAULT = PropertyUtil.getSchemaProperty(context, "relationship_SubVaults");
        final String RELATIONSHIP_VAULTED_OBJECTS = PropertyUtil.getSchemaProperty(context, "relationship_VaultedDocuments");
        final String TYPE_PSS_DOCUMENTS = PropertyUtil.getSchemaProperty(context, "type_PSS_Document");
        final String TYPE_WORKSPACE_VAULTS = PropertyUtil.getSchemaProperty(context, "type_ProjectVault");
        // final String ATTRIBUTE_PSS_TEMPLATE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Template");
        RelationshipType rtVaultedObjects = new RelationshipType(RELATIONSHIP_VAULTED_OBJECTS);
        HashMap retMap = new HashMap();

        try {
            Map<?, ?> programMap = (Map<?, ?>) JPO.unpackArgs(args);
            // HashMap<?, ?> paramMap = (HashMap<?, ?>) programMap.get("paramMap");
            String strWSName = (String) programMap.get("Name");
            String strWSDes = (String) programMap.get("Description");
            String sTemplateId = (String) programMap.get("TemplateOID");
            String strBuyerDeskId = (String) programMap.get("txtBuyerDeskId");
            String strCompanyVault = context.getVault().getName();
            // String typeWorkspace = PropertyUtil.getSchemaProperty(context, "type_Project");
            Map<String, String> mAssociatedDocumentTemplates = new HashMap<>();

            Workspace WorkspaceObj = (Workspace) DomainObject.newInstance(context, DomainConstants.TYPE_WORKSPACE, DomainConstants.TEAM);

            String strProjectType = WorkspaceObj.TYPE_PROJECT;
            // String strUser = context.getUser();
            String MAX_LENGTH = FrameworkProperties.getProperty(context, "emxComponents.MAX_FIELD_LENGTH");
            // String newSecurityModel = "true";
            String objectId = "";
            String langStr = context.getLocale().getLanguage();

            if (strWSName.length() > (Integer.parseInt(MAX_LENGTH))) {
                String strLengthMessage = UINavigatorUtil.getI18nString("emxTeamCentral.NameLength.Message", "emxTeamCentralStringResource", langStr);
                String strChars = UINavigatorUtil.getI18nString("emxTeamCentral.NameLength.NumChars", "emxTeamCentralStringResource", langStr);
                retMap.put("ErrorMessage", strLengthMessage + MAX_LENGTH + " " + strChars);
                return retMap;
            }

            ContextUtil.pushContext(context);

            boolean isWorkspaceExists = (boolean) Workspace.isWorkspaceExists(context, FrameworkUtil.getVaultNames(context, false, true).toString(), strWSName);

            ContextUtil.popContext(context);
            if (!isWorkspaceExists) {
                objectId = TeamUtil.autoRevision(context, HttpServletRequest, strProjectType, strWSName, WorkspaceObj.POLICY_PROJECT, strCompanyVault);
                WorkspaceObj.setId(objectId);
                WorkspaceObj.open(context);
                WorkspaceObj.setDescription(context, strWSDes);
                WorkspaceObj.update(context);
                DomainAccess.createObjectOwnership(context, objectId, com.matrixone.apps.domain.util.PersonUtil.getPersonObjectID(context), "Full", DomainAccess.COMMENT_MULTIPLE_OWNERSHIP);
                if (!UIUtil.isNullOrEmpty(sTemplateId)) {
                    if (sTemplateId.startsWith("B")) {
                        sTemplateId = sTemplateId.substring(1);
                    }
                    DomainObject workspaceTemplateObj = DomainObject.newInstance(context, sTemplateId, DomainConstants.TEAM);
                    WorkspaceObj.connectWorkspaceTemplate(context, workspaceTemplateObj, true, true, strCompanyVault);

                    Pattern relPattern = new Pattern(RELATIONSHIP_DATA_VAULT);
                    relPattern.addPattern(RELATIONSHIP_SUB_VAULT);
                    relPattern.addPattern(RELATIONSHIP_VAULTED_OBJECTS);

                    Pattern typPattern = new Pattern(TYPE_PSS_DOCUMENTS);
                    typPattern.addPattern(TYPE_WORKSPACE_VAULTS);

                    StringList slObjSelect = new StringList();
                    slObjSelect.add(DomainConstants.SELECT_CURRENT);
                    slObjSelect.add(DomainConstants.SELECT_ID);
                    slObjSelect.add(DomainConstants.SELECT_NAME);
                    slObjSelect.addElement("to[" + RELATIONSHIP_SUB_VAULT + "].from.name");

                    StringList slRelSelect = new StringList();
                    slRelSelect.add(DomainRelationship.SELECT_FROM_ID);

                    MapList mpWorkspaceTemplate = workspaceTemplateObj.getRelatedObjects(context, relPattern.getPattern(), typPattern.getPattern(), slObjSelect, slRelSelect, false, true, (short) 0,
                            null, null, (short) 0);

                    for (int i = 0; i < mpWorkspaceTemplate.size(); i++) {
                        Map<?, ?> mWorkspaceTemplateObj = (Map<?, ?>) mpWorkspaceTemplate.get(i);
                        String strType = (String) mWorkspaceTemplateObj.get(DomainConstants.SELECT_TYPE);

                        if (!strType.equals(TYPE_PSS_DOCUMENTS)) {
                            String strFolderLevel = (String) mWorkspaceTemplateObj.get(DomainConstants.SELECT_LEVEL);
                            String strFolderName = (String) mWorkspaceTemplateObj.get(DomainConstants.SELECT_NAME);
                            String strParentFolderName = (String) mWorkspaceTemplateObj.get("to[" + RELATIONSHIP_SUB_VAULT + "].from.name");
                            String strMKey = strParentFolderName + strFolderName + strFolderLevel;
                            String strFolderId = (String) mWorkspaceTemplateObj.get(DomainConstants.SELECT_ID);
                            StringBuffer sbMpValue = new StringBuffer();
                            for (int j = 0; j < mpWorkspaceTemplate.size(); j++) {
                                Map<?, ?> mDocObj = (Map<?, ?>) mpWorkspaceTemplate.get(j);
                                String strDocType = (String) mDocObj.get(DomainConstants.SELECT_TYPE);
                                String strFromId = (String) mDocObj.get(DomainRelationship.SELECT_FROM_ID);
                                String strDocId = (String) mDocObj.get(DomainConstants.SELECT_ID);

                                if (strDocType.equals(TYPE_PSS_DOCUMENTS) && strFromId.equals(strFolderId)) {
                                    // Findbug Issue correction start
                                    // Date: 22/03/2017
                                    // By: Asha G.
                                    sbMpValue.append(strDocId);
                                    sbMpValue.append("|");
                                    // Findbug Issue correction End
                                }
                            }
                            String strMValue = sbMpValue.toString();
                            mAssociatedDocumentTemplates.put(strMKey, strMValue);
                        }
                    }

                    if (mAssociatedDocumentTemplates.size() != 0) {
                        MapList mpWorkspace = WorkspaceObj.getRelatedObjects(context, relPattern.getPattern(), typPattern.getPattern(), slObjSelect, slRelSelect, false, true, (short) 0, null, null,
                                (short) 0);

                        for (int i = 0; i < mpWorkspace.size(); i++) {
                            Map<?, ?> mWorkspaceObj = (Map<?, ?>) mpWorkspace.get(i);
                            String strTempParentFolderName = (String) mWorkspaceObj.get("to[" + RELATIONSHIP_SUB_VAULT + "].from.name");
                            String strTempFolderLevel = (String) mWorkspaceObj.get(DomainConstants.SELECT_LEVEL);
                            String strTempFolderName = (String) mWorkspaceObj.get(DomainConstants.SELECT_NAME);
                            String strWSTemplateFolderId = (String) mWorkspaceObj.get(DomainConstants.SELECT_ID);
                            DomainObject domFolder = DomainObject.newInstance(context, strWSTemplateFolderId);
                            String strTemplateKey = strTempParentFolderName + strTempFolderName + strTempFolderLevel;
                            if (mAssociatedDocumentTemplates.containsKey(strTemplateKey) && mAssociatedDocumentTemplates.get(strTemplateKey) != null
                                    && !(mAssociatedDocumentTemplates.get(strTemplateKey).equals(""))) {
                                String strDocumentTemplateIds = mAssociatedDocumentTemplates.get(strTemplateKey);
                                String[] strSplit = strDocumentTemplateIds.split("\\|");

                                for (int j = 0; j < strSplit.length; j++) {
                                    pss.document.policy.DocumentUtil_mxJPO obj = new pss.document.policy.DocumentUtil_mxJPO();
                                    String[] strClonedDocArr = { strSplit[j] };
                                    String strClonedDocId = obj.documentClone(context, strClonedDocArr);
                                    domFolder.addRelatedObject(context, rtVaultedObjects, false, strClonedDocId);
                                }
                            }
                        }
                    }
                }
                if (strBuyerDeskId != null && !"".equals(strBuyerDeskId)) {
                    WorkspaceObj.addBuyerDesk(context, strBuyerDeskId);
                    WorkspaceObj.addBuyerDeskPersons(context, strCompanyVault, strBuyerDeskId);
                }

            } else {
                retMap.put("ErrorMessage", EnoviaResourceBundle.getProperty(context, "emxTeamCentralStringResource", context.getLocale(), "emxTeamCentral.Common.Workspace") + " " + strWSName + " "
                        + EnoviaResourceBundle.getProperty(context, "emxTeamCentralStringResource", context.getLocale(), "emxTeamCentral.Common.AlreadyExists"));
                return retMap;
            }
            retMap.put("id", objectId);
            return retMap;
        } catch (Exception e) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in createWorkspaceProcess: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
    }

    public boolean showEditAndSubscriptionCommand(matrix.db.Context context, String[] args) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String folderId = (String) programMap.get("objectId");
        String workspaceId = (String) programMap.get("workspaceId");

        // Bug fix-330176 . Added below condition to get workspace id.
        if (workspaceId == null || "".equals(workspaceId) || "null".equals(workspaceId)) {
            emxWorkspaceFolderBase_mxJPO emxWorkspaceFolderBase = new emxWorkspaceFolderBase_mxJPO(context, null);
            workspaceId = emxWorkspaceFolderBase.getProjectId(context, folderId);
        }

        BusinessObject workspaceFolder = new BusinessObject(folderId);
        BusinessObject workspace = new BusinessObject(workspaceId);
        String loggedInPerson = context.getUser().toString();
        if (loggedInPerson.equals(workspace.getOwner(context).toString()) || loggedInPerson.equals(workspaceFolder.getOwner(context).toString())) {
            return false;
        } else {
            return true;
        }
    }
	
	// TIGTK-12983 : mkakade : start
	public void addVersionObjects(Context context, String[] args) throws Exception
	{
		try
		{
			if(null != args && args.length>1)
			{
				String strFromId = args[0];
				String strToId = args[1];
				if(UIUtil.isNotNullAndNotEmpty(strFromId) && UIUtil.isNotNullAndNotEmpty(strToId))
				{
					DomainObject dmWorkspaceVault = new DomainObject(strToId);
					ArrayList<String> alConnectList = new ArrayList<String>(10);

					StringList slObjSelect = new StringList(2);
					slObjSelect.add(DomainConstants.SELECT_TYPE);
					slObjSelect.add(DomainConstants.SELECT_ID);

					StringList slSelectData = new StringList(2);
					slSelectData.addElement(DomainConstants.SELECT_POLICY);
					slSelectData.addElement(DomainConstants.SELECT_TYPE);

					DomainObject dmFromObj = new DomainObject(strFromId);
					Map mpDataObj = dmFromObj.getInfo(context, slSelectData);
					if(null != mpDataObj && !mpDataObj.isEmpty())
					{
						String strPolicy = (String)mpDataObj.get(DomainConstants.SELECT_POLICY);
						String strType = (String)mpDataObj.get(DomainConstants.SELECT_TYPE);
						// Check for CADObjects
						if(UIUtil.isNotNullAndNotEmpty(strPolicy) && strPolicy.equals(TigerConstants.POLICY_PSS_CADOBJECT))
						{
							/*
							StringList slMinorObjects = dmFromObj.getInfoList(context, "to["+TigerConstants.RELATIONSHIP_VERSIONOF+"].from.id");
							StringList slDerivedObjects = dmFromObj.getInfoList(context, "from["+TigerConstants.RELATIONSHIP_DERIVEDOUTPUT+"].to.id");
							 */
							Pattern relPattern = new Pattern(TigerConstants.RELATIONSHIP_VERSIONOF);
							relPattern.addPattern(TigerConstants.RELATIONSHIP_DERIVEDOUTPUT);			

							// Get all versionOf and derived output objects 
							MapList mlData = dmFromObj.getRelatedObjects(context, relPattern.getPattern(), "*", slObjSelect, null, true, true, (short) 1,
									null, null, (short) 0);
							if(null != mlData && !mlData.isEmpty())
							{
								int iSize = mlData.size();
								Map mpData = null;
								String strId = null;
								for(int i=0;i<iSize;i++)
								{
									mpData = (Map) mlData.get(i);
									if(null != mpData && !mpData.isEmpty())
									{
										strId = (String) mpData.get(DomainConstants.SELECT_ID);
										if(UIUtil.isNotNullAndNotEmpty(strId))
										{
											alConnectList.add(strId);
										}
									}
								}
							}

						}
						else if(UIUtil.isNotNullAndNotEmpty(strType) && strType.equals(TigerConstants.TYPE_PSS_DOCUMENT))
						{
							Pattern relPattern = new Pattern(TigerConstants.RELATIONSHIP_ACTIVEVERSION);
							relPattern.addPattern(TigerConstants.RELATIONSHIP_LATESTVERSION);
							MapList mlData = dmFromObj.getRelatedObjects(context, relPattern.getPattern(), "*", slObjSelect, null, false, true, (short) 1,
									null, null, (short) 0);
							if(null != mlData && !mlData.isEmpty())
							{
								int iSize = mlData.size();
								Map mpData = null;
								String strId = null;
								for(int i=0;i<iSize;i++)
								{
									mpData = (Map) mlData.get(i);
									if(null != mpData && !mpData.isEmpty())
									{
										strId = (String) mpData.get(DomainConstants.SELECT_ID);
										if(UIUtil.isNotNullAndNotEmpty(strId) && !alConnectList.contains(strId))
										{
											alConnectList.add(strId);
										}
									}
								}
							}
						}

						if(null != alConnectList && !alConnectList.isEmpty())
						{
							int iListSize = alConnectList.size();
							String strObjId = null;
							ArrayList<String> alFinalList = new ArrayList<String>();
							for(int j=0;j<iListSize;j++)
							{
								strObjId = alConnectList.get(j);
								DomainObject dmObject = new DomainObject(strObjId);
								StringList slConnectedData = dmObject.getInfoList(context, "to["+DomainConstants.RELATIONSHIP_VAULTED_OBJECTS+"].from.id");
								// Check object is already connected or not 
								if(null != slConnectedData && !slConnectedData.contains(strToId))
								{
									alFinalList.add(strObjId);
								}
							}
							String[] saList = alFinalList.toArray(new String[alFinalList.size()]);
							// Connect objects to Workspace Vault
							DomainRelationship.connect(context, dmWorkspaceVault, DomainConstants.RELATIONSHIP_VAULTED_OBJECTS, true, saList); 
						}
					}

				}
			}
		}
		catch(Exception ex)
		{
			logger.error("Error in PSS_emxWorkspace_mxJPO:addVersionObjects: ", ex);
		}
	}
	// TIGTK-12983 : mkakade : end

}
