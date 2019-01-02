package pss.jv.common;
import java.util.Map;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.Context;
import matrix.util.StringList;
import pss.constants.TigerConstants;


public class PSS_JVUtil_mxJPO   {   

	protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PSS_JVUtil_mxJPO.class);

	public PSS_JVUtil_mxJPO() throws Exception {

	}

	/****
	 * This method will return StringList of JV Roles
	 * @param context
	 * @throws Exception
	 */
	public StringList getJVRelatedRoles(Context context)
	{
		StringList slRoleList = new StringList();
		try
		{
			String strJVRoles = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSS.EnterpriseChangeMgt.ProgramProject.JVRoles");
			if(UIUtil.isNotNullAndNotEmpty(strJVRoles))
			{
				slRoleList = FrameworkUtil.split(strJVRoles, ",");
			}
		}
		catch(Exception ex)
		{
			logger.error("Error in PSS_JVUtil_mxJPO:getJVRelatedRoles:ERROR " + ex);
		}

		return slRoleList;    	
	}

	/****
	 * This method will return true if it is related to JV ProgramProject
	 * @param context
	 * @param strObjectID
	 * @throws Exception
	 */
	public boolean isJVProgramProject(Context context, String strObjectID)
	{
		boolean isJVProgramProject = false;
		try
		{
			if(UIUtil.isNotNullAndNotEmpty(strObjectID))
			{
				DomainObject dmObj = new DomainObject(strObjectID);
				String strType = dmObj.getInfo(context , DomainConstants.SELECT_TYPE);

				// If type is PSS_ChangeRequest or PSS_Issue
				if(UIUtil.isNotNullAndNotEmpty(strType) && (TigerConstants.TYPE_PSS_CHANGEREQUEST.equals(strType) || TigerConstants.TYPE_PSS_ISSUE.equals(strType) ))
				{
					String strOwnership = dmObj.getInfo(context, "to["+TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA+"].from.attribute["+TigerConstants.ATTRIBUTE_PSS_OWNERSHIP+"]"); 
					if(UIUtil.isNotNullAndNotEmpty(strOwnership) && TigerConstants.ATTRIBUTE_PSS_USERTYPE_RANGE_JV.equalsIgnoreCase(strOwnership) )
					{
						isJVProgramProject = true;
					}
				} // If type is PSS_ProgramProject
				else if(UIUtil.isNotNullAndNotEmpty(strType) && TigerConstants.TYPE_PSS_PROGRAMPROJECT.contentEquals(strType))
				{
					String strOwnership = dmObj.getInfo(context, "attribute["+TigerConstants.ATTRIBUTE_PSS_OWNERSHIP+"]");
					if(UIUtil.isNotNullAndNotEmpty(strOwnership) && TigerConstants.ATTRIBUTE_PSS_USERTYPE_RANGE_JV.equalsIgnoreCase(strOwnership) )
					{
						isJVProgramProject = true;
					}
				}
			}
		}
		catch(Exception ex)
		{
			logger.error("Error in PSS_JVUtil_mxJPO:isJVProgramProject:ERROR " + ex);
		}

		return isJVProgramProject;
	}

	/****
	 * This method will return StringList of BusinessUnit related to JV
	 * @param context
	 * @param args
	 * @throws Exception
	 */
	public StringList getListOfBusinessUnit(Context context,String[] args)
	{
		StringList slBU = new StringList();
		try
		{
			String strWhere = "attribute["+TigerConstants.ATTRIBUTE_PSS_BGTYPE+"] == "+TigerConstants.ATTRIBUTE_PSS_USERTYPE_RANGE_JV;
			StringList objectSelect = new StringList();
			objectSelect.addElement(DomainConstants.SELECT_NAME);
			MapList resultsList = DomainObject.findObjects(context, // eMatrix context
					TigerConstants.TYPE_BUSINESSUNIT, // type pattern
					DomainConstants.QUERY_WILDCARD, // name pattern
					DomainConstants.QUERY_WILDCARD, // revision pattern
					DomainConstants.QUERY_WILDCARD, // owner pattern
					TigerConstants.VAULT_ESERVICEPRODUCTION, // vault pattern
					strWhere, // where expression
					true, // Expand Type
					objectSelect); // object selects
			if(null != resultsList && !resultsList.isEmpty())
			{
				int iSize = resultsList.size();
				Map mpData = null;
				String strBUName = null;
				for(int i=0;i<iSize;i++)
				{
					mpData = (Map) resultsList.get(i);
					if(null != mpData && !mpData.isEmpty())
					{
						strBUName = (String) mpData.get(DomainConstants.SELECT_NAME);
						if(UIUtil.isNotNullAndNotEmpty(strBUName))
						{
							slBU.addElement(strBUName);
						}
					}
				}
			}

		}
		catch(Exception ex)
		{
			logger.error("Error in PSS_JVUtil_mxJPO:getListOfBusinessUnit:ERROR " + ex);
		}
		return slBU;
	}

	public StringList getFauresiaUsersConnectedToJV(Context context, String strProgramProjectId)
	{
		StringList slUsers = new StringList();
		try
		{
			if(UIUtil.isNotNullAndNotEmpty(strProgramProjectId))
			{
				DomainObject dmObj = new DomainObject(strProgramProjectId);
				// Get Org and Ownership
				StringList slSelect = new StringList(2);
				slSelect.addElement(DomainConstants.SELECT_ORGANIZATION);
				slSelect.addElement("attribute["+TigerConstants.ATTRIBUTE_PSS_OWNERSHIP+"]");
				Map mpPPData = dmObj.getInfo(context, slSelect);
				if(null != mpPPData && !mpPPData.isEmpty())
				{
					String strOrg = (String)mpPPData.get(DomainConstants.SELECT_ORGANIZATION);
					String strOwnership = (String)mpPPData.get("attribute["+TigerConstants.ATTRIBUTE_PSS_OWNERSHIP+"]");
					if(UIUtil.isNotNullAndNotEmpty(strOwnership) && TigerConstants.ATTRIBUTE_PSS_USERTYPE_RANGE_JV.equalsIgnoreCase(strOwnership) && UIUtil.isNotNullAndNotEmpty(strOrg) )
				{
					// Get BusinessUnitType from BU 
						StringList objectSelect = new StringList(1);
						//objectSelect.addElement("attribute["+TigerConstants.ATTRIBUTE_PSS_BGTYPE+"]");
					objectSelect.addElement(DomainConstants.SELECT_ID);
					MapList resultsList = DomainObject.findObjects(context, // eMatrix context
							TigerConstants.TYPE_BUSINESSUNIT, // type pattern
							strOrg, // name pattern
							DomainConstants.QUERY_WILDCARD, // revision pattern
							DomainConstants.QUERY_WILDCARD, // owner pattern
							TigerConstants.VAULT_ESERVICEPRODUCTION, // vault pattern
							null, // where expression
							true, // Expand Type
							objectSelect); // object selects
					if(null != resultsList && !resultsList.isEmpty())
					{
						Map mpBU = (Map)resultsList.get(0);
						if(null != mpBU && !mpBU.isEmpty())
						{
								//String strBUType = (String) mpBU.get("attribute["+TigerConstants.ATTRIBUTE_PSS_BGTYPE+"]");
							String strBUId = (String) mpBU.get(DomainConstants.SELECT_ID);
								//if(UIUtil.isNotNullAndNotEmpty(strBUType) && TigerConstants.ATTRIBUTE_PSS_USERTYPE_RANGE_JV.equalsIgnoreCase(strBUType) )
								//{
								if(UIUtil.isNotNullAndNotEmpty(strBUId))
								{
									// Get User connected with Member relation which are ACTIVE and has UserType Faurecia
									StringList slObjectSelects = new StringList(2);
									slObjectSelects.addElement(DomainConstants.SELECT_TYPE);
									slObjectSelects.addElement(DomainConstants.SELECT_ID);
									String strWhere = "attribute["+TigerConstants.ATTRIBUTE_PSS_USERTYPE+"]== \""+TigerConstants.ATTRIBUTE_PSS_USERTYPE_RANGE_FAURECIA+"\" && "+DomainConstants.SELECT_CURRENT+" == "+DomainConstants.STATE_PERSON_ACTIVE;
									DomainObject dmBU = new DomainObject(strBUId);
									MapList mlRelatedPersonObjects = dmBU.getRelatedObjects(context, DomainConstants.RELATIONSHIP_MEMBER, DomainConstants.TYPE_PERSON, slObjectSelects, null, false, true,
											(short) 0, strWhere, null, 0);
									if(null != mlRelatedPersonObjects && !mlRelatedPersonObjects.isEmpty())
									{
										int iSize = mlRelatedPersonObjects.size();
										Map mpPerson = null;
										String strPersonId = null;
										for(int i=0;i<iSize;i++)
										{
											mpPerson = (Map) mlRelatedPersonObjects.get(i);
											if(null != mpPerson && !mpPerson.isEmpty())
											{
												strPersonId = (String) mpPerson.get(DomainConstants.SELECT_ID);
												if(UIUtil.isNotNullAndNotEmpty(strPersonId))
												{
													slUsers.add(strPersonId);
												}
											}
										}
									}
								}
								//}
							}
						}
					}

				}
			}
		}
		catch(Exception ex)
		{
			logger.error("Error in PSS_JVUtil_mxJPO:getFauresiaUsersConnectedToJV:ERROR " + ex);
		}
		return slUsers;
	}

}
