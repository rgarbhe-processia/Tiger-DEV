package fpdm.pno;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.matrixone.apps.common.Company;
import com.matrixone.apps.common.Person;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.ExpansionIterator;
import matrix.db.JPO;
import matrix.db.RelationshipType;
import matrix.db.RelationshipWithSelect;
import matrix.util.MatrixException;
import matrix.util.StringList;

public class ManagePerson_mxJPO {

    private static Logger logger = LoggerFactory.getLogger("ManagePerson");

    private static String RENAME_PROGRAM_PROPERTIES = "emxComponentsStringResource";

    public ManagePerson_mxJPO(Context context, String[] args) throws Exception {
    }

    public void setAdminPropertyAndPrivileges(Context context, String[] args) throws Exception {

        String sRoleGlobalAdministrator = PropertyUtil.getSchemaProperty(context, "role_PSS_Global_Administrator");
        String sRolePLMSupportTeam = PropertyUtil.getSchemaProperty(context, "role_PSS_PLM_Support_Team");
        String sRoleProgramManager = PropertyUtil.getSchemaProperty(context, "role_PSS_Program_Manager");

        if (args.length == 1 || args[1].startsWith(sRoleGlobalAdministrator) || args[1].startsWith(sRolePLMSupportTeam) || args[1].startsWith(sRoleProgramManager)) {
            String sPerson = DomainConstants.EMPTY_STRING;
            try {
                Person pPerson = new Person(args[0]);
                sPerson = pPerson.getInfo(context, DomainConstants.SELECT_NAME);

                MqlUtil.mqlCommand(context, false, "mod person " + sPerson + " property preference_Solution value VPLM", true);

                if ((args.length == 1 || args[1].startsWith(sRoleGlobalAdministrator) || args[1].startsWith(sRolePLMSupportTeam)) && !Company.isHostRep(context, pPerson)) {
                    pPerson.addFromObject(context, new RelationshipType(DomainConstants.RELATIONSHIP_COMPANY_REPRESENTATIVE), Company.getHostCompany(context));
                }
            } catch (Exception e) {
                logger.error("Error in MqlCommand in method setAdminPropertyAndPrivileges()\n", e);
            }

        }

    }

    public void removeAdminPropertyAndPrivileges(Context context, String[] args) throws Exception {

        String sRoleGlobalAdministrator = PropertyUtil.getSchemaProperty(context, "role_PSS_Global_Administrator");
        String sRolePLMSupportTeam = PropertyUtil.getSchemaProperty(context, "role_PSS_PLM_Support_Team");
        String sRoleProgramManager = PropertyUtil.getSchemaProperty(context, "role_PSS_Program_Manager");

        if (args.length == 1 || args[1].startsWith(sRoleGlobalAdministrator) || args[1].startsWith(sRolePLMSupportTeam) || args[1].startsWith(sRoleProgramManager)) {
            try {
                try {
                    ContextUtil.startTransaction(context, true);
                    Person pPerson = new Person(args[0]);
                    String sPerson = pPerson.getInfo(context, DomainConstants.SELECT_NAME);
                    String sWhereClause = "name smatchlist \"" + sRoleGlobalAdministrator + ".*," + sRolePLMSupportTeam + ".*," + sRoleProgramManager + ".*\" \",\"";
                    StringList slBus = new StringList(DomainConstants.SELECT_NAME);
                    ExpansionIterator eiSecurityContext = pPerson.getExpansionIterator(context, DomainConstants.RELATIONSHIP_ASSIGNED_SECURITY_CONTEXT, DomainConstants.TYPE_SECURITYCONTEXT, slBus,
                            DomainConstants.EMPTY_STRINGLIST, false, true, (short) 1, sWhereClause, DomainConstants.EMPTY_STRING, (short) 0, true, false, (short) 500);
                    StringList slRoles = pPerson.getRoleAssignments(context);

                    if (!eiSecurityContext.hasNext() && !slRoles.contains("role_PSS_Global_Administrator") && !slRoles.contains("role_PSS_PLM_Support_Team")) {
                        try {
                            MqlUtil.mqlCommand(context, false, "mod person " + sPerson + " remove property preference_Solution", true);

                            if (Company.isHostRep(context, pPerson)) {
                                pPerson.disconnect(context, new RelationshipType(DomainConstants.RELATIONSHIP_COMPANY_REPRESENTATIVE), false, Company.getCompanyForRep(context, pPerson));
                            }
                        } catch (MatrixException e) {
                            logger.error("Error in MqlCommand in method removeAdminPropertyAndPrivileges()\n", e);
                        }
                    }
                    eiSecurityContext.close();
                } finally {
                    ContextUtil.commitTransaction(context);

                }
            } catch (Exception e) {
                logger.error("Error in method removeAdminPropertyAndPrivileges()\n", e);
            }

        }
    }

    public int setPersonDefaultRoles(Context context, String[] args) throws Exception {
        try {
            String sDefaultRoles = "";
            try {
                sDefaultRoles = EnoviaResourceBundle.getProperty(context, "emxFramework.roles.default");
            } catch (FrameworkException fe) {
                logger.warn("Property emxFramework.roles.default not found", fe);
            }
            if (!"".equals(sDefaultRoles)) {
                Person person = new Person(args[0]);
                for (String sDefaultRole : sDefaultRoles.split(",")) {
                    person.addToRole(context, PropertyUtil.getSchemaProperty(context, sDefaultRole));
                }
            }
        } catch (Exception e) {
            logger.error("Error in method setPersonDefaultRoles\n", e);
            throw e;
        }
        return 0;
    }

    public int renamePerson(Context context, String[] args) throws Exception {
        HashMap<?, ?> paramMap = (HashMap<?, ?>) JPO.unpackArgs(args);
        String sPersonId = (String) paramMap.get("objectId");
        String sCNName = (String) paramMap.get("cnName");

        if (sPersonId == null || "".equals(sPersonId)) {
            // parameter error
            return 1;
        }

        // check if the new username is valid : [a-zA-Z0-9]+
        if (!sCNName.matches("^[a-zA-Z0-9]+$")) {
            return 7;
        }

        // check if the target username is different than the current username
        DomainObject doUserToRename = DomainObject.newInstance(context, sPersonId);
        String sOldUserName = doUserToRename.getInfo(context, "name");

        if (logger.isDebugEnabled()) {
            logger.debug("renamePerson() - trying to rename user " + sPersonId + " <" + sOldUserName + "> to username <" + sCNName + ">");
        }

        if (sOldUserName == null) {
            // an username could not be found for the id provided
            return 2;
        }

        if (sOldUserName.equals(sCNName)) {
            // the two usernames are the same
            return 3;
        }

        int intReturnValue = 0;
        if (!searchInLDAP(context, sCNName)) {
            return 4;
        } else {
            // user exists, proceed with the renaming ; check if the target user
            // is not in the database
            Boolean bUserHasBeenFound = true;
            try {
                BusinessObject boUser = new BusinessObject(DomainConstants.TYPE_PERSON, sCNName, "-", PropertyUtil.getSchemaProperty(context, "vault_eServiceProduction"));
                DomainObject doUserExistsInEnovia = DomainObject.newInstance(context, boUser);
                String sIdUserExistsInEnovia = doUserExistsInEnovia.getId(context);

                if ("".equals(sIdUserExistsInEnovia) || sIdUserExistsInEnovia == null) {
                    bUserHasBeenFound = false;
                }

            } catch (MatrixException e) {
                bUserHasBeenFound = false;
            }

            if (bUserHasBeenFound) {
                // user already exists in the system
                return 5;
            }

            // the username is different, and the target user is not in the
            // database : rename the user
            try {
                doUserToRename.setName(context, sCNName);
                changeCoOwnerName(context, sPersonId, sOldUserName, sCNName);
                changeRoleName(context, sOldUserName, sCNName);
            } catch (Exception e) {
                logger.error("Error in method renamePerson()", e);

                intReturnValue = 6;
            }
        }

        return intReturnValue;
    }

    private static DirContext getInitDirContextForLDAPQuery(Context context) throws Exception {

        String sAdminDN = EnoviaResourceBundle.getProperty(context, RENAME_PROGRAM_PROPERTIES, context.getLocale(), "LDAP.AdminDN");
        String ldapURL = EnoviaResourceBundle.getProperty(context, RENAME_PROGRAM_PROPERTIES, context.getLocale(), "LDAP.Url");
        String sAdminPwd = EnoviaResourceBundle.getProperty(context, RENAME_PROGRAM_PROPERTIES, context.getLocale(), "LDAP.AdminPwd");
        String ldapFactory = EnoviaResourceBundle.getProperty(context, RENAME_PROGRAM_PROPERTIES, context.getLocale(), "LDAP.Factory");
        String ldapVersion = EnoviaResourceBundle.getProperty(context, RENAME_PROGRAM_PROPERTIES, context.getLocale(), "LDAP.Version");

        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, ldapFactory);
        env.put(javax.naming.Context.PROVIDER_URL, ldapURL);
        env.put("java.naming.ldap.version", ldapVersion);
        env.put(javax.naming.Context.SECURITY_AUTHENTICATION, "simple");
        env.put(javax.naming.Context.SECURITY_PRINCIPAL, sAdminDN);
        env.put(javax.naming.Context.SECURITY_CREDENTIALS, sAdminPwd);

        return new InitialDirContext(env);
    }

    private static NamingEnumeration<SearchResult> getPeopleInGroupLDAP(Context context, String sInitFilter, String sInternalExternal, String[] attrIDs) throws Exception {

        String ldapBase = EnoviaResourceBundle.getProperty(context, RENAME_PROGRAM_PROPERTIES, context.getLocale(), "LDAP.Base");
        String sReplaceInternal = "";
        String sLDAPGroup = EnoviaResourceBundle.getProperty(context, RENAME_PROGRAM_PROPERTIES, context.getLocale(), "LDAP.Group");
        String sSearchLDAP = "(!(FauITDisabled=True))(isMemberOf=" + sLDAPGroup + ")";

        if (sInternalExternal.equals("internal")) {
            sReplaceInternal = "(mail=*faurecia*.com)";
        }

        String sFilter = "(&(objectclass=*)" + sInitFilter + sSearchLDAP + sReplaceInternal + ")";

        DirContext ctx = getInitDirContextForLDAPQuery(context);
        SearchControls constraints = new SearchControls();

        if (attrIDs != null) {
            constraints.setReturningAttributes(attrIDs);
        }

        constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);

        return ctx.search(ldapBase, sFilter, constraints);
    }

    private boolean searchInLDAP(Context context, String sCnName) throws Exception {

        boolean bRet = false;

        try {
            String filter = "(FauITUnifLog=" + sCnName + ")";
            String[] attrIDs = { "cn", "FauITUnifLog" };
            NamingEnumeration<SearchResult> resultsGroup = getPeopleInGroupLDAP(context, filter, "internal", attrIDs);

            if (resultsGroup != null && resultsGroup.hasMore()) {
                SearchResult siGroup = (SearchResult) resultsGroup.next();
                Attributes attrs = siGroup.getAttributes();

                if (attrs.get("fauituniflog") != null) {
                    bRet = sCnName.equals((String) attrs.get("fauituniflog").get()) ? true : false;
                } else {

                    while (resultsGroup.hasMore() && !bRet) {
                        searchInLDAP(context, sCnName);
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Error into searchInLDAP:", e);
            throw e;
        }

        return bRet;
    }

    private void changeCoOwnerName(Context context, String sObjectId, String sCnNameOld, String sCnNameNew) throws Exception {

        try {
            List<RelationshipWithSelect> lRoutes;
            String sCoOwnerName = PropertyUtil.getSchemaProperty(context, "attribute_CoOwner");
            String sAttributeCoOwner = "attribute[" + PropertyUtil.getSchemaProperty(context, "attribute_CoOwner") + "].value";
            try {

                ContextUtil.startTransaction(context, true);
                StringList slBus = new StringList(3);
                slBus.addElement(DomainConstants.SELECT_TYPE);
                slBus.addElement(DomainConstants.SELECT_ID);
                slBus.addElement(sAttributeCoOwner);
                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), "", "");
                DomainObject doPerson = new DomainObject(sObjectId);
                ExpansionIterator eiRoutes = doPerson.getExpansionIterator(context,
                        DomainConstants.RELATIONSHIP_PROJECT_ROUTE + "," + DomainConstants.RELATIONSHIP_INITIATING_ROUTE_TEMPLATE + ","
                                + PropertyUtil.getSchemaProperty(context, "relationship_PSS_ConnectedMembers") + "," + PropertyUtil.getSchemaProperty(context, "relationship_PSS_ConnectedPCMData"),
                        DomainConstants.TYPE_ROUTE + "," + DomainConstants.TYPE_ROUTE_TEMPLATE + "," + PropertyUtil.getSchemaProperty(context, "type_PSS_ProgramProject") + ","
                                + PropertyUtil.getSchemaProperty(context, "type_PSS_Issue"),
                        slBus, DomainConstants.EMPTY_STRINGLIST, true, true, (short) 0, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, (short) 0, false, true, (short) 500, false);

                lRoutes = new ArrayList<RelationshipWithSelect>();
                while (eiRoutes.hasNext()) {
                    RelationshipWithSelect rwsRoute = eiRoutes.next();
                    if (!rwsRoute.getTargetSelectData(DomainConstants.SELECT_TYPE).equals(PropertyUtil.getSchemaProperty(context, "type_PSS_ProgramProject"))) {
                        String sCoOwnerValue = rwsRoute.getTargetSelectData(sAttributeCoOwner);
                        if (!"".equals(sCoOwnerValue) && sCoOwnerValue.contains(sCnNameOld)) {
                            lRoutes.add(rwsRoute);
                        }
                    }
                }
                eiRoutes.close();
            } finally {
                ContextUtil.abortTransaction(context);
                ContextUtil.popContext(context);
            }
            for (RelationshipWithSelect relRoute : lRoutes) {
                List<String> listOwners = new ArrayList<String>(Arrays.asList(relRoute.getTargetSelectData(sAttributeCoOwner).split("[|]")));
                listOwners.remove(sCnNameOld);
                listOwners.add(sCnNameNew);
                MqlUtil.mqlCommand(context, false, "mod bus " + relRoute.getTargetSelectData(DomainConstants.SELECT_ID) + " " + sCoOwnerName + " " + String.join("|", listOwners), true);
            }

        } catch (MatrixException e) {
            logger.error("Error in changeCoOwnerName method\n", e);
        }
    }

    private void changeRoleName(Context context, String sCnNameOld, String sCnNameNew) {

        try {
            MqlUtil.mqlCommand(context, false, "mod role " + sCnNameOld + "_PRJ name " + sCnNameNew + "_PRJ", true);
        } catch (FrameworkException e) {
            logger.error("Error in changeRoleName method\n", e);
        }
    }

}
