package fpdm.uls;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.matrixone.apps.common.Person;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.framework.ui.UIUtil;
import com.matrixone.vplm.posbusinessmodel.Project;
import com.matrixone.vplm.posbusinessmodel.SecurityContext;

import matrix.db.Attribute;
import matrix.db.AttributeList;
import matrix.db.AttributeType;
import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.ExpansionIterator;
import matrix.db.JPO;
import matrix.db.RelationshipType;
import matrix.util.StringList;
import pss.constants.TigerConstants;

public class ProgramProject_mxJPO {

    private static Logger logger = LoggerFactory.getLogger("ProgramProject");

    public String getCSOptions(Context context, String[] args) throws Exception {
        StringBuilder sb = new StringBuilder();
        
        // TIGTK-12983 - ssamel : START
        // For displaying only Active option to JV user
        Person pLoggedPerson = new Person(PersonUtil.getPersonObjectID(context));
        String sUserType = pLoggedPerson.getInfo(context, "attribute[" + TigerConstants.ATTRIBUTE_PSS_USERTYPE + "]");
        
        if(TigerConstants.ATTRIBUTE_PSS_USERTYPE_RANGE_JV.equals(sUserType))
        {
            sb.append("<input type=\"radio\" name=\"CSOption\"  id=\"CSOption\" value=\"Active\" checked = \"checked\"></input> ");
            sb.append(EnoviaResourceBundle.getFrameworkStringResourceProperty(context, "emxFramework.ProgProj.ActiveCS", context.getLocale()));
        }
        else
        {
            sb.append("<input type=\"radio\" name=\"CSOption\"  id=\"CSOption\" value=\"Active\" ></input> ");
            sb.append(EnoviaResourceBundle.getFrameworkStringResourceProperty(context, "emxFramework.ProgProj.ActiveCS", context.getLocale()));
            sb.append(" <br>");
            sb.append("<input type=\"radio\" name=\"CSOption\"  id=\"CSOption\" value=\"Other\"  checked = \"checked\"></input> ");
            sb.append(EnoviaResourceBundle.getFrameworkStringResourceProperty(context, "emxFramework.ProgProj.OtherCS", context.getLocale()));
            sb.append(" <input type=\"text\"  size=\"\" name=\"CSName\"  id=\"CSName\" value=\"\" onchange=\"changeToUpperCase(this);\"></input>");
            sb.append("</br>");
            sb.append("<script language='JavaScript'>myValidationRoutines1.push(['assignValidateMethod', 'CSName', 'checkOtherCS']);</script>");
        }
        // TIGTK-12983 - ssamel : END
        
        return sb.toString();
    }

    /*
     * Method Used for the Creation of Program-project and Collaborative space if not already exit.
     */
    public void createPPPostProcess(Context context, String[] args) throws Exception {
        try {
            HashMap programMap = null;
            programMap = (HashMap) JPO.unpackArgs(args);
            Map requestMap = (Map) programMap.get("requestMap");
            Map paramMap = (Map) programMap.get("paramMap");

            Person person = new Person(PersonUtil.getPersonObjectID(context));
            String CSOption = (String) requestMap.get("CSOption");

            if ("Other".equals(CSOption)) {
                String CSName = (String) requestMap.get("CSName");
                context.setApplication("VPLM");
                if (!Project.doesProjectExists(context, CSName)) {
                    String ParentOID = (String) requestMap.get("parentOID");
                    DomainObject doParentObject = DomainObject.newInstance(context, ParentOID); // Domain Object instantiated with the ParentOID
                    String parentProj = doParentObject.getInfo(context, "project");
                    BusinessObject parentBoCS = new BusinessObject(DomainConstants.TYPE_PNOPROJECT, parentProj, "-", context.getVault().getName());

                    AttributeList paramAttributeList = new AttributeList();
                    Attribute attPnOSolution = new Attribute(new AttributeType(Project.ATTRIBUTE_SOLUTION), "VPM");
                    paramAttributeList.addElement(attPnOSolution);
                    Attribute attPnOVibility = parentBoCS.getAttributeValues(context, Project.ATTRIBUTE_VISIBILITY);
                    if (attPnOVibility != null)
                        paramAttributeList.addElement(attPnOVibility);

                    BusinessObject boCS = new Project();
                    ((Project) boCS).create(context, CSName, Project.POLICY_PNO_PROJECT, context.getVault().getName(), null);
                    boCS.setAttributes(context, paramAttributeList);

                    DomainObject donForParent = DomainObject.newInstance(context, parentBoCS);
                    donForParent.addToObject(context, new RelationshipType("Sub Project"), boCS.getObjectId());

                }

                String newObjectId = (String) paramMap.get("newObjectId");
                DomainObject domObj = DomainObject.newInstance(context, newObjectId);
                domObj.setProjectOwner(context, CSName);
                domObj.update(context);

                String sSecurityContext = SecurityContext.getPreferredSecurityContext(context, context.getUser());
                sSecurityContext = sSecurityContext.substring(0, sSecurityContext.lastIndexOf('.') + 1);

                StringBuffer saSecurityContext = new StringBuffer(sSecurityContext);
                saSecurityContext.append(CSName);

                String[] saSecurityCtx = (saSecurityContext.toString()).split("\\.");
                String sRole = (String) saSecurityCtx[0];
                String sOrganization = (String) saSecurityCtx[1];
                StringList slSecurityContext = new StringList();

                boolean bExist = SecurityContext.doesSecurityContextExists(context, saSecurityContext.toString());

                if (!bExist) {
                    SecurityContext scSecurityContext = new SecurityContext();
                    scSecurityContext.create(context, sRole, sOrganization, CSName, DomainConstants.TYPE_SECURITYCONTEXT, PropertyUtil.getSchemaProperty(context, context.getVault().getName()),
                            new HashMap());
                }
                slSecurityContext.addElement(saSecurityContext.toString());
                SecurityContext.addSecurityContexts(context, person, slSecurityContext);
            }
            
            // TIGTK-12983 - ssamel : START
            // For setting Ownership attribute on Program-Project
            String sPPId = (String) paramMap.get("newObjectId");
            DomainObject doPPObject = DomainObject.newInstance(context, sPPId); // Domain Object instantiated with the ParentOID
            String sPPOrganization = doPPObject.getInfo(context, DomainConstants.SELECT_ORGANIZATION);

            if(UIUtil.isNotNullAndNotEmpty(sPPOrganization))
            {
                StringList slSelect = new StringList(2);
                slSelect.addElement(DomainConstants.SELECT_ID);
                slSelect.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_BGTYPE + "].value");
                MapList mlOrganizations = DomainObject.findObjects(context, DomainConstants.TYPE_ORGANIZATION, sPPOrganization, 
                        DomainConstants.QUERY_WILDCARD, DomainConstants.QUERY_WILDCARD, TigerConstants.VAULT_ESERVICEPRODUCTION, 
                        null, "", true, (StringList) slSelect, (short) 0);          

                if(null!=mlOrganizations && !mlOrganizations.isEmpty() && mlOrganizations.size()==1){
                    String sBGType = (String)((Map) mlOrganizations.get(0)).get("attribute[" + TigerConstants.ATTRIBUTE_PSS_BGTYPE + "].value");
                    if(UIUtil.isNotNullAndNotEmpty(sBGType) && TigerConstants.ATTRIBUTE_PSS_USERTYPE_RANGE_JV.equals(sBGType))
                    {
                        doPPObject.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_OWNERSHIP, sBGType);
                    }
                }
            }
            // TIGTK-12983 - ssamel : END
            
        } catch (Exception e) {
            logger.error("Error in createPPPostProcess method\n", e);
            String msg = EnoviaResourceBundle.getFrameworkStringResourceProperty(context, "emxFramework.CreateProgramProject.ErrorMsg", context.getLocale());
            MqlUtil.mqlCommand(context, "notice $1", msg);
            throw e;
        }
    }

    public int assignSCFromMemberRole(Context context, String args[]) throws Exception {
        try {
            String relationship = args[0];
            String role = args[1];
            String ppId = args[2];
            String personId = args[3];

            if ("PSS_ConnectedMembers".equals(relationship) && !"".equals(role)) {
                StringList slPPInfo = new StringList(2);
                slPPInfo.add(DomainConstants.SELECT_ORGANIZATION);
                slPPInfo.add("project");

                DomainObject doPP = DomainObject.newInstance(context, ppId);
                Map mPPInfo = doPP.getInfo(context, slPPInfo);
                String sPPOrg = (String) mPPInfo.get(DomainConstants.SELECT_ORGANIZATION);
                String sPPCS = (String) mPPInfo.get("project");
                String sSC = role + "." + sPPOrg + "." + sPPCS;

                if (!SecurityContext.doesSecurityContextExists(context, sSC)) {
                    SecurityContext scSecurityContext = new SecurityContext();
                    scSecurityContext.create(context, role, sPPOrg, sPPCS, DomainConstants.TYPE_SECURITYCONTEXT, TigerConstants.VAULT_ESERVICEPRODUCTION, new HashMap());
                }

                Context vplmContext = new Context(context.getSession());
                vplmContext.setApplication("VPLM");
                vplmContext.setVault("vplm");

                Person person = new Person(personId);
                StringList slSecurityContext = new StringList();
                slSecurityContext.addElement(sSC);
                SecurityContext.addSecurityContexts(vplmContext, person, slSecurityContext);
            }

            return 0;
        } catch (Exception e) {
            logger.error("Error in assignSCFromMemberRole method\n", e);
            throw e;
        }

    }

    public int removeSCOnMemberDisconnection(Context context, String args[]) throws Exception {
        try {
            String relId = args[0];
            String ppId = args[1];
            String personId = args[2];

            Person person = new Person(personId);

            StringList slPPInfo = new StringList(2);
            slPPInfo.add(DomainConstants.SELECT_ORGANIZATION);
            slPPInfo.add("project");

            DomainObject doPP = DomainObject.newInstance(context, ppId);
            Map mPPInfo = doPP.getInfo(context, slPPInfo);
            String sPPOrg = (String) mPPInfo.get(DomainConstants.SELECT_ORGANIZATION);
            String sPPCS = (String) mPPInfo.get("project");

            String role = DomainRelationship.getAttributeValue(context, relId, TigerConstants.ATTRIBUTE_PSS_ROLE);
            if (!"".equals(role)) {
                // The Security Context can be removed only if the user has not multiple PSS_ConnectedMembers links on PP belonging to the PP CS
                int noOfConnectedMemberLinks = 0;
                try {
                    ContextUtil.startTransaction(context, true);
                    String sBusWhereClause = "project == \"" + sPPCS + "\"";
                    String sRelWhereClause = "attribute[" + TigerConstants.ATTRIBUTE_PSS_ROLE + "].value == \"" + role + "\"";
                    ExpansionIterator eiPP = person.getExpansionIterator(context, TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS, TigerConstants.TYPE_PSS_PROGRAMPROJECT,
                            new StringList(DomainConstants.SELECT_ID), DomainConstants.EMPTY_STRINGLIST, true, false, (short) 1, sBusWhereClause, sRelWhereClause, (short) 0, false, true, (short) 500);
                    while (eiPP.hasNext()) {
                        eiPP.next();
                        noOfConnectedMemberLinks++;
                    }
                    eiPP.close();
                } finally {
                    ContextUtil.abortTransaction(context);
                }

                if (noOfConnectedMemberLinks <= 1) {
                    String sSC = role + "." + sPPOrg + "." + sPPCS;

                    Context vplmContext = new Context(context.getSession());
                    vplmContext.setApplication("VPLM");
                    vplmContext.setVault("vplm");

                    StringList slSecurityContext = new StringList();
                    slSecurityContext.addElement(sSC);
                    SecurityContext.removeSecurityContexts(vplmContext, person, slSecurityContext);
                }
            }
            return 0;
        } catch (Exception e) {
            logger.error("Error in removeSCOnMemberDisconnection method\n", e);
            throw e;
        }
    }

}
