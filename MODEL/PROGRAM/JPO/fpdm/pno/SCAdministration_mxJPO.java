package fpdm.pno;

import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeConstants;
import com.matrixone.apps.common.Person;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.StringUtil;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.vplm.posbusinessmodel.SecurityContext;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.Context;
import matrix.db.ExpansionIterator;
import matrix.db.JPO;
import matrix.db.Query;
import matrix.db.QueryIterator;
import matrix.db.RelationshipWithSelect;
import matrix.util.MatrixException;
import matrix.util.StringList;

public class SCAdministration_mxJPO {

    private static Logger logger = LoggerFactory.getLogger("SCAdministration");

    public SCAdministration_mxJPO(Context context, String[] args) {
    }

    public HashMap<String, StringList> getListOfRoles(Context context, String[] args) {
        HashMap<String, StringList> hmListRolesWithTranslations = new HashMap<String, StringList>();
        StringList slRoleTranslations = new StringList();
        StringList slPSSRoles = new StringList();
        try {
            try {
                ContextUtil.startTransaction(context, true);
                Query query = new Query();
                query.setBusinessObjectType(PropertyUtil.getSchemaProperty(context, "type_BusinessRole"));
                query.setBusinessObjectName("PSS_*");
                query.setVaultPattern(PropertyUtil.getSchemaProperty(context, "vault_eServiceProduction"));
                StringList slRoles = new StringList(DomainConstants.SELECT_NAME);
                QueryIterator queryItr = query.getIterator(context, slRoles, (short) 500);
                String language = context.getSession().getLanguage();
                if (queryItr.hasNext()) {
                    while (queryItr.hasNext()) {
                        BusinessObjectWithSelect bowsItr = queryItr.next();
                        slPSSRoles.addElement(bowsItr.getSelectData(DomainConstants.SELECT_NAME));
                        slRoleTranslations.addElement(i18nNow.getRoleI18NString(bowsItr.getSelectData(DomainConstants.SELECT_NAME), language));
                    }
                    slPSSRoles.addElement("");
                    slRoleTranslations.addElement("");

                    hmListRolesWithTranslations.put("field_choices", slPSSRoles);
                    hmListRolesWithTranslations.put("field_display_choices", slRoleTranslations);
                } else {
                    logger.error("No PSS business roles found in getListOfRoles method\n");
                }
                queryItr.close();
                query.close(context);
            } finally {
                ContextUtil.abortTransaction(context);
            }
        } catch (MatrixException e) {
            logger.error("error in retrieving PSS Business roles in getListOfRoles method\n", e);
        }
        return hmListRolesWithTranslations;
    }

    public HashMap<String, StringList> getListOfOrg(Context context, String args[]) {

        HashMap<String, StringList> hmListOfOrg = new HashMap<String, StringList>();
        try {
            StringList slNames = new StringList();
            SecurityContext sc = SecurityContext.getSecurityContext(context, PersonUtil.getDefaultSecurityContext(context));
            try {
                ContextUtil.startTransaction(context, true);

                StringList slBus = new StringList(DomainConstants.SELECT_NAME);
                String sRelPattern = DomainConstants.RELATIONSHIP_SECURITYCONTEXT_ORGANIZATION + "," + DomainConstants.RELATIONSHIP_DIVISION + "," + DomainConstants.RELATIONSHIP_COMPANY_DEPARTMENT;
                String sTypePattern = DomainConstants.TYPE_COMPANY + "," + DomainConstants.TYPE_BUSINESS_UNIT + "," + DomainConstants.TYPE_DEPARTMENT;
                ExpansionIterator eiOrganizations = sc.getExpansionIterator(context, sRelPattern, sTypePattern, slBus, DomainConstants.EMPTY_STRINGLIST, false, true, (short) 0,
                        DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, (short) 0, true, true, (short) 500);

                RelationshipWithSelect rwsOrg;
                while (eiOrganizations.hasNext()) {
                    rwsOrg = eiOrganizations.next();
                    slNames.add(rwsOrg.getTargetSelectData(DomainConstants.SELECT_NAME));
                }
                eiOrganizations.close();
            } finally {
                ContextUtil.abortTransaction(context);
            }
            hmListOfOrg.put("field_choices", slNames);
            hmListOfOrg.put("field_display_choices", slNames);
        } catch (MatrixException e) {
            logger.error("Error in getListOfOrg method\n", e);
        }
        return hmListOfOrg;
    }

    public MapList getRootCS(Context context, String[] args) {

        MapList mlPnoProjects = new MapList();
        try {
            try {
                ContextUtil.startTransaction(context, true);
                Query query = new Query();
                query.setBusinessObjectType(DomainConstants.TYPE_PNOPROJECT);
                query.setVaultPattern(PropertyUtil.getSchemaProperty(context, "vault_eServiceProduction"));
                String sActiveOrg = PersonUtil.getDefaultOrganization(context, context.getUser());
                String sRelSubProject = PropertyUtil.getSchemaProperty(context, "relationship_SubProject");
                query.setWhereExpression("ownership.organization.ancestor==\"" + sActiveOrg + "\" && (to[" + sRelSubProject + "]==False || !to[" + sRelSubProject
                        + "].from.ownership.organization.ancestor==\"" + sActiveOrg + "\")");

                StringList slCS = new StringList(2);
                slCS.add(DomainConstants.SELECT_ID);
                slCS.add(DomainConstants.SELECT_NAME);
                QueryIterator queryItr = query.getIterator(context, slCS, (short) 500);
                boolean bIsAdministrator = false;
                StringList slCSActualUser = new StringList(); // stringList
                                                              // which
                                                              // contains only
                                                              // Collaborative
                                                              // Spaces where
                                                              // the actual
                                                              // program
                                                              // manager user
                                                              // is defined
                if (context.isAssigned(PropertyUtil.getSchemaProperty(context, "role_PSS_Global_Administrator"))
                        || context.isAssigned(PropertyUtil.getSchemaProperty(context, "role_PSS_PLM_Support_Team"))) {
                    bIsAdministrator = true;
                } else {
                    DomainObject doActualUser = PersonUtil.getPersonObject(context);
                    String sWhereClause = "name ~~ \"" + PropertyUtil.getSchemaProperty(context, "role_PSS_Program_Manager") + ".*" + "\"";
                    ExpansionIterator eiSecurityContext = doActualUser.getExpansionIterator(context, DomainConstants.RELATIONSHIP_ASSIGNED_SECURITY_CONTEXT, DomainConstants.TYPE_SECURITYCONTEXT,
                            new StringList(DomainConstants.SELECT_NAME), new StringList(), false, true, (short) 1, sWhereClause, DomainConstants.EMPTY_STRING, (short) 0, true, false, (short) 500);
                    while (eiSecurityContext.hasNext()) {
                        RelationshipWithSelect rwsSC = eiSecurityContext.next();
                        String sSCName = rwsSC.getTargetSelectData(DomainConstants.SELECT_NAME);
                        slCSActualUser.add(StringUtil.split(sSCName, ".").elementAt(2));
                    }
                    eiSecurityContext.close();
                }

                while (queryItr.hasNext()) {
                    BusinessObjectWithSelect bowsItr = queryItr.next();
                    String sActualCS = bowsItr.getSelectData(DomainConstants.SELECT_ID);
                    Map<String, String> map = new HashMap<String, String>();
                    map.put(DomainConstants.SELECT_ID, sActualCS);
                    if (!bIsAdministrator && !slCSActualUser.contains(bowsItr.getSelectData(DomainConstants.SELECT_NAME))) {
                        map.put("disableSelection", "true");
                    }
                    mlPnoProjects.add(map);
                }
                queryItr.close();
                query.close(context);
            } finally {
                ContextUtil.abortTransaction(context);
            }
        } catch (MatrixException e) {
            logger.error("Error in getRootCS method\n", e);
        }
        return mlPnoProjects;
    }

    public MapList getStructureOfCS(Context context, String[] args) {

        MapList mlPnoProjects = new MapList();
        try {
            Map<?, ?> requestMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            String sParentID = (String) requestMap.get(ChangeConstants.OBJECT_ID);
            DomainObject domainObject = DomainObject.newInstance(context, sParentID);

            String sRelationship = PropertyUtil.getSchemaProperty(context, "relationship_SubProject");
            String sType = DomainConstants.TYPE_PNOPROJECT;

            StringList slObjectSelected = new StringList(2);
            slObjectSelected.add(DomainConstants.SELECT_ID);
            slObjectSelected.add(DomainConstants.SELECT_NAME);
            short shRecursionLevel = 1;
            String sBusWhere = "ownership.organization.ancestor==\"" + PersonUtil.getDefaultOrganization(context, context.getUser()) + "\"";
            mlPnoProjects = domainObject.getRelatedObjects(context, sRelationship, sType, slObjectSelected, DomainConstants.EMPTY_STRINGLIST, false, true, shRecursionLevel, sBusWhere,
                    DomainConstants.EMPTY_STRING, 0);
            if (!context.isAssigned(PropertyUtil.getSchemaProperty(context, "role_PSS_Global_Administrator"))
                    && !context.isAssigned(PropertyUtil.getSchemaProperty(context, "role_PSS_PLM_Support_Team"))) {
                StringList slCSActualUser = new StringList(mlPnoProjects.size());
                DomainObject doActualUser = PersonUtil.getPersonObject(context);
                String sWhereClause = "name ~~ \"" + PropertyUtil.getSchemaProperty(context, "role_PSS_Program_Manager") + ".*" + "\"";
                ExpansionIterator eiSecurityContext = doActualUser.getExpansionIterator(context, DomainConstants.RELATIONSHIP_ASSIGNED_SECURITY_CONTEXT, DomainConstants.TYPE_SECURITYCONTEXT,
                        new StringList(DomainConstants.SELECT_NAME), DomainConstants.EMPTY_STRINGLIST, false, true, (short) 1, sWhereClause, DomainConstants.EMPTY_STRING, (short) 0, true, false,
                        (short) 500);
                while (eiSecurityContext.hasNext()) {
                    RelationshipWithSelect rwsSC = eiSecurityContext.next();
                    String sSCName = rwsSC.getTargetSelectData(DomainConstants.SELECT_NAME);
                    slCSActualUser.add(StringUtil.split(sSCName, ".").elementAt(2));
                }
                eiSecurityContext.close();

                for (Object map : mlPnoProjects) {
                    Map<String, Object> mPnoTemp = (Map<String, Object>) map;
                    String sName = (String) mPnoTemp.get(DomainConstants.SELECT_NAME);
                    if (!slCSActualUser.contains(sName)) {
                        mPnoTemp.put("disableSelection", "true");
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Error in Retrieving datas in getStructureOfCS method\n", e);
        }
        return mlPnoProjects;
    }

    public void assignPersonToNewSC(Context context, String[] args) {

        try {
            HashMap<?, ?> requestMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            HashMap<String, Object> paramMap = (HashMap<String, Object>) requestMap.get("paramMap");

            String sRole = (String) paramMap.get("Role");
            String sPersonID = (String) paramMap.get("objectId");
            String sOrganization = (String) paramMap.get("Organization");
            StringList slCollaborativeSpaces = StringUtil.split((String) paramMap.get("CollaborativeSpace"), "|");

            String sSecurityContext = DomainConstants.EMPTY_STRING;
            SecurityContext scSecurityContext;
            BusinessObject boPerson = new BusinessObject(sPersonID);
            boPerson.open(context);

            Context mainContext = new Context(context.getSession());
            mainContext.setApplication("VPLM");
            mainContext.setVault("vplm");
            Person person = new Person(boPerson);

            StringList slSecurityContext = new StringList();
            for (Object oCollaborativeSpace : slCollaborativeSpaces) {
                String sCurrentCS = (String) oCollaborativeSpace;
                sSecurityContext = sRole + "." + sOrganization + "." + sCurrentCS;
                boolean bExist = SecurityContext.doesSecurityContextExists(context, sSecurityContext);

                try {
                    if (!bExist) {
                        scSecurityContext = new SecurityContext();
                        scSecurityContext.create(context, sRole, sOrganization, sCurrentCS, DomainConstants.TYPE_SECURITYCONTEXT, PropertyUtil.getSchemaProperty(context, "vault_eServiceProduction"),
                                new HashMap());
                    }
                    slSecurityContext.addElement(sSecurityContext);
                } catch (Exception e) {
                    logger.error("Error in assignPersonToNewCS method\n", e);
                }
            }

            try {
                SecurityContext.addSecurityContexts(mainContext, person, slSecurityContext);
            } catch (Throwable e) {
                logger.error("Assigning Security Contexts to " + sPersonID + " has failed in assignPersonToNewSC method\n", e);
            }

            boPerson.close(context);
        } catch (Exception e) {
            logger.error("Error in assignPersonToNewSC method\n", e);
        }

    }

    public MapList getPersonsWithSCInSelectedOrgAndCS(Context context, String[] args) {

        MapList mlPersonIdAndRole = new MapList();
        try {
            HashMap<?, ?> hmProgram = (HashMap<?, ?>) JPO.unpackArgs(args);
            String sOrg = (String) hmProgram.get("FPDM_SCAdministrationOrgFilter");
            String sCS = (String) hmProgram.get("FPDM_SCAdministrationCSFilter");
            String sSCRole = (String) hmProgram.get("FPDM_SCAdministrationRoleFilter");

            if (!"".equals(sCS)) {
                if (sSCRole == null || "".equals(sSCRole)) {
                    sSCRole = "PSS*";
                }
                String sAddedPersons = (String) hmProgram.get("personIds");

                StringList slPersonsInCurrentCSAndOrg = new StringList();
                try {
                    ContextUtil.startTransaction(context, true);
                    Query querySC = new Query();
                    querySC.setBusinessObjectType(DomainConstants.TYPE_SECURITYCONTEXT);
                    querySC.setVaultPattern(PropertyUtil.getSchemaProperty(context, "vault_eServiceProduction"));
                    querySC.setBusinessObjectName(sSCRole + "." + sOrg + "." + sCS);

                    try {

                        try {

                            StringList slSecurityContext = new StringList(2);
                            slSecurityContext.add(DomainConstants.SELECT_ID);
                            slSecurityContext.add(DomainConstants.SELECT_NAME);

                            QueryIterator queryItr = querySC.getIterator(context, slSecurityContext, (short) 500, null, false);

                            MapList mlPersonsTmp = new MapList();
                            BusinessObjectWithSelect bows;
                            BusinessObject boSC;
                            StringList slBus = new StringList(DomainConstants.SELECT_ID);
                            StringList slRel = new StringList(DomainConstants.SELECT_RELATIONSHIP_ID);
                            String sRole = DomainConstants.EMPTY_STRING;
                            String sLanguage = context.getSession().getLanguage();

                            while (queryItr.hasNext()) {
                                // Retrieving related persons to each Security
                                // Context found
                                bows = queryItr.next();
                                bows.open(context);

                                boSC = new BusinessObject(bows.getObjectId());
                                boSC.open(context);

                                sRole = bows.getName();
                                sRole = sRole.substring(0, sRole.indexOf('.'));
                                bows.close(context);

                                String sTranslation = i18nNow.getRoleI18NString(sRole, sLanguage);

                                ExpansionIterator eiPersons = boSC.getExpansionIterator(context, DomainConstants.RELATIONSHIP_ASSIGNED_SECURITY_CONTEXT, DomainConstants.TYPE_PERSON, slBus, slRel,
                                        true, false, (short) 1, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, (short) 0, true, false, (short) 500);
                                while (eiPersons.hasNext()) {
                                    RelationshipWithSelect rwsPerson = eiPersons.next();
                                    HashMap<String, String> mPerson = new HashMap<String, String>();
                                    mPerson.put(DomainConstants.SELECT_ID, rwsPerson.getTargetSelectData(DomainConstants.SELECT_ID));
                                    mPerson.put("role", sTranslation);
                                    mlPersonsTmp.add(mPerson);
                                }
                                eiPersons.close();
                                boSC.close(context);

                            }
                            queryItr.close();
                            querySC.close(context);
                            Map<String, Object> mPerson = new HashMap<String, Object>();
                            Map<String, Object> mPersonTmp;
                            String sCurrentPersonId = DomainConstants.EMPTY_STRING;
                            String sPreviousPersonId = DomainConstants.EMPTY_STRING;

                            mlPersonsTmp.sort(DomainConstants.SELECT_ID, "ascending", "string");

                            for (int i = 0; i < mlPersonsTmp.size(); i++) {
                                mPersonTmp = (Map<String, Object>) mlPersonsTmp.get(i);

                                sCurrentPersonId = (String) mPersonTmp.get(DomainConstants.SELECT_ID);

                                if (!sCurrentPersonId.equals(sPreviousPersonId)) {
                                    mPerson = new HashMap<String, Object>();
                                    slPersonsInCurrentCSAndOrg.add(sCurrentPersonId);
                                    mPerson.put(DomainConstants.SELECT_ID, mPersonTmp.get(DomainConstants.SELECT_ID));
                                    mPerson.put("role", ((Map<String, Object>) mlPersonsTmp.get(i)).get("role"));
                                    mlPersonIdAndRole.add(mPerson);
                                } else {
                                    mPerson.put("role", mPerson.get("role") + "," + ((Map<String, Object>) mlPersonsTmp.get(i)).get("role"));
                                }

                                sPreviousPersonId = sCurrentPersonId;
                            }

                        } finally {
                            ContextUtil.commitTransaction(context);
                        }

                        StringList slAddedPersons = new StringList();
                        if (sAddedPersons != null) {
                            slAddedPersons = StringUtil.split(sAddedPersons, "|");
                        }
                        if (slAddedPersons.size() > 0) {
                            HashMap<String, Object> mPerson = new HashMap<String, Object>();
                            for (Object oPersonId : slAddedPersons) {
                                if (!slPersonsInCurrentCSAndOrg.contains(oPersonId)) {
                                    mPerson = new HashMap<String, Object>();
                                    mPerson.put(DomainConstants.SELECT_ID, oPersonId);
                                    mPerson.put("role", DomainConstants.EMPTY_STRING);
                                    mlPersonIdAndRole.add(mPerson);
                                }
                            }
                        }

                    } catch (MatrixException e) {
                        logger.error("Error in retrieving datas in getPersonsWithSCInSelectedOrgAndCS method\n", e);
                    }
                } catch (FrameworkException e) {
                    logger.error("Error in starting transaction in getPersonsWithSCInSelectedOrgAndCS method\n", e);
                }
            }
        } catch (Exception e) {
            logger.error("Error in getPersonsWithSCInSelectedOrgAndCS method\n", e);
        }
        return mlPersonIdAndRole;
    }

    public Vector<String> getUserRole(Context context, String args[]) {

        Vector<String> vRole = null;

        try {
            Map<String, Object> hmProgram = (Map<String, Object>) JPO.unpackArgs(args);
            MapList mlObjectList = (MapList) hmProgram.get(ChangeConstants.OBJECT_LIST);
            vRole = new Vector<String>(mlObjectList.size());
            ListIterator<Map<String, String>> lIterator = mlObjectList.listIterator();
            while (lIterator.hasNext()) {
                vRole.add(String.valueOf(lIterator.next().get("role")));
            }
        } catch (Exception e) {
            logger.error("Error in getUserRole method\n", e);
        }
        return vRole;
    }

    public StringList excludeExistingPersons(Context context, String args[]) {

        StringList slPersonIdsInCurrentOrgCS = new StringList();

        try {
            Map<?, ?> hmProgram = (Map<?, ?>) JPO.unpackArgs(args);

            String sOrg = (String) hmProgram.get("FPDM_SCAdministrationOrgFilter");
            String sCS = (String) hmProgram.get("FPDM_SCAdministrationCSFilter");
            try {
                ContextUtil.startTransaction(context, true);

                BusinessObjectWithSelect bows;
                BusinessObject boSC;
                StringList slBus = new StringList(DomainConstants.SELECT_ID);

                Query querySC = new Query();
                querySC.setBusinessObjectType(DomainConstants.TYPE_SECURITYCONTEXT);
                querySC.setBusinessObjectName("*." + sOrg + "." + sCS);
                querySC.setVaultPattern(PropertyUtil.getSchemaProperty(context, "vault_eServiceProduction"));
                QueryIterator queryItr = querySC.getIterator(context, new StringList(), (short) 200);
                StringList slPersonIdTmp = new StringList();
                while (queryItr.hasNext()) {
                    bows = queryItr.next();
                    bows.open(context);

                    boSC = new BusinessObject(bows.getObjectId());
                    bows.close(context);

                    ExpansionIterator eiPersons = boSC.getExpansionIterator(context, DomainConstants.RELATIONSHIP_ASSIGNED_SECURITY_CONTEXT, DomainConstants.TYPE_PERSON, slBus,
                            DomainConstants.EMPTY_STRINGLIST, true, false, (short) 1, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, (short) 0, true, false, (short) 500);

                    while (eiPersons.hasNext()) {
                        RelationshipWithSelect rwsPerson = eiPersons.next();
                        slPersonIdTmp.add(rwsPerson.getTargetSelectData(DomainConstants.SELECT_ID));
                    }
                    eiPersons.close();

                }
                queryItr.close();
                querySC.close(context);

                slPersonIdTmp.sort();
                String sCurrentPersonId = DomainConstants.EMPTY_STRING;
                String sPreviousPersonId = DomainConstants.EMPTY_STRING;
                for (Object oPersonId : slPersonIdTmp) {
                    sCurrentPersonId = (String) oPersonId;

                    if (!sCurrentPersonId.equals(sPreviousPersonId)) {
                        slPersonIdsInCurrentOrgCS.add(sCurrentPersonId);
                    }

                    sPreviousPersonId = sCurrentPersonId;
                }
            } finally {
                ContextUtil.commitTransaction(context);
            }
        } catch (Exception e) {
            logger.error("Error in excludeExistingPersons method\n", e);
        }
        return slPersonIdsInCurrentOrgCS;
    }

    public void updateUserSecurityContext(Context context, String[] args) throws Exception {

        try {
            Map<?, ?> hmProgram = (Map<?, ?>) JPO.unpackArgs(args);
            Map<String, Object> paramMap = (HashMap<String, Object>) hmProgram.get("paramMap");
            Map<String, Object> requestMap = (HashMap<String, Object>) hmProgram.get("requestMap");

            String sSelectedOrg = FrameworkUtil.decodeURL((String) requestMap.get("FPDM_SCAdministrationOrgFilter"), "UTF8");
            String sSelectedCS = FrameworkUtil.decodeURL((String) requestMap.get("FPDM_SCAdministrationCSFilter"), "UTF8");
            String sPersonId = (String) paramMap.get(ChangeConstants.OBJECT_ID);
            StringList slSelectedRoles = StringUtil.split((String) paramMap.get(ChangeConstants.NEW_VALUE), ",");

            SecurityContext scSecurityContext = new SecurityContext();
            String sSecurityContext = DomainConstants.EMPTY_STRING;
            String sRole = DomainConstants.EMPTY_STRING;
            StringList slOldSecurityContexts = new StringList();

            Person person = new Person(sPersonId);
            try {
                ContextUtil.startTransaction(context, true);
                try {
                    StringList slBus = new StringList(2);
                    slBus.add(DomainConstants.SELECT_ID);
                    slBus.add(DomainConstants.SELECT_NAME);

                    String sWhereClause = "name ~~ \"PSS*." + sSelectedOrg + "." + sSelectedCS + "\"";

                    ExpansionIterator eiSecurityContext = person.getExpansionIterator(context, DomainConstants.RELATIONSHIP_ASSIGNED_SECURITY_CONTEXT, DomainConstants.TYPE_SECURITYCONTEXT, slBus,
                            DomainConstants.EMPTY_STRINGLIST, false, true, (short) 1, sWhereClause, DomainConstants.EMPTY_STRING, (short) 0, true, false, (short) 500);

                    while (eiSecurityContext.hasNext()) {
                        RelationshipWithSelect rwsSC = eiSecurityContext.next();
                        sSecurityContext = rwsSC.getTargetSelectData(DomainConstants.SELECT_NAME);
                        sRole = sSecurityContext.substring(0, sSecurityContext.indexOf('.'));
                        if (slSelectedRoles.contains(sRole)) {
                            slSelectedRoles.remove(sRole);
                        } else {
                            slOldSecurityContexts.addElement(sSecurityContext);
                        }
                    }
                    eiSecurityContext.close();

                } catch (MatrixException e) {
                    logger.error("Error in query iterator loop in updateUserSecurityContext method\n", e);
                    throw e;
                }
            } finally {
                ContextUtil.commitTransaction(context);
            }

            Context mainContext = new Context(context.getSession());
            mainContext.setApplication("VPLM");
            mainContext.setVault("vplm");

            if (!slOldSecurityContexts.isEmpty()) {
                try {
                    SecurityContext.removeSecurityContexts(mainContext, person, slOldSecurityContexts);
                } catch (Throwable e) {
                    logger.error("Error while removing old Security Contexts to " + sPersonId + " in updateUserSecurityContext method\n", e);
                    throw e;
                }
            }

            StringList slNewSecurityContexts = new StringList();
            if (!slSelectedRoles.isEmpty()) {
                for (Object oRoleSelected : slSelectedRoles) {
                    sRole = (String) oRoleSelected;
                    if (!"".equals(sRole)) {
                        sSecurityContext = sRole + "." + sSelectedOrg + "." + sSelectedCS;
                        boolean bExist = SecurityContext.doesSecurityContextExists(context, sSecurityContext);
                        if (!bExist) {
                            scSecurityContext.create(context, sRole, sSelectedOrg, sSelectedCS, DomainConstants.TYPE_SECURITYCONTEXT,
                                    PropertyUtil.getSchemaProperty(context, "vault_eServiceProduction"), new HashMap());
                        }
                        slNewSecurityContexts.addElement(sSecurityContext);
                    }
                }
            }

            if (!slNewSecurityContexts.isEmpty()) {
                try {
                    SecurityContext.addSecurityContexts(mainContext, person, slNewSecurityContexts);
                } catch (Throwable e) {
                    logger.error("Error while assigning new Security Contexts to " + sPersonId + " in updateUserSecurityContext method\n", e);
                    throw e;
                }
            }

        } catch (Exception e) {
            logger.error("Error in updateUserSecurityContext method\n", e);
            throw e;
        }

    }

    public void addPersonsFromCS(Context context, String args[]) {

        try {
            try {
                try {
                    ContextUtil.startTransaction(context, true);
                    try {

                        Map<?, ?> requestMap = (HashMap<?, ?>) JPO.unpackArgs(args);
                        String[] saSelectedPersonsId = (String[]) requestMap.get("emxTableRowId");
                        String sFormRole = (String) requestMap.get("FPDM_SCAdministrationRoleFilter");
                        if (sFormRole == null || "".equals(sFormRole)) {
                            sFormRole = "PSS*";
                        }
                        String sFormOrg = (String) requestMap.get("FPDM_SCAdministrationOrgFilter");
                        String sFormPrj = (String) requestMap.get("FPDM_SCAdministrationCSFilter");
                        String sParentFilterOrg = (String) requestMap.get("FPDM_SCAdministrationParentOrgFilter");
                        String sParentFilterCS = (String) requestMap.get("FPDM_SCAdministrationParentCSFilter");
                        String sWhereClause = "name ~~ \"" + sFormRole + "." + sFormOrg + "." + sFormPrj + "\"";

                        Context mainContext = new Context(context.getSession());
                        mainContext.setApplication("VPLM");
                        mainContext.setVault("vplm");
                        for (Object oPersonId : saSelectedPersonsId) {

                            String sActualPersonId = (String) ((StringUtil.split((String) oPersonId, "|")).elementAt(0));
                            BusinessObject boPerson = new BusinessObject(sActualPersonId);
                            Person person = new Person(boPerson);
                            StringList slBus = new StringList(DomainConstants.SELECT_NAME);
                            ExpansionIterator eiSecurityContext = boPerson.getExpansionIterator(context, DomainConstants.RELATIONSHIP_ASSIGNED_SECURITY_CONTEXT, DomainConstants.TYPE_SECURITYCONTEXT,
                                    slBus, DomainConstants.EMPTY_STRINGLIST, false, true, (short) 1, sWhereClause, DomainConstants.EMPTY_STRING, (short) 0, true, false, (short) 500);

                            StringList slSecurityContext = new StringList();
                            String sSecurityContext = DomainConstants.EMPTY_STRING;

                            while (eiSecurityContext.hasNext()) {
                                RelationshipWithSelect rwsSC = eiSecurityContext.next();
                                String sSCName = rwsSC.getTargetSelectData(DomainConstants.SELECT_NAME);
                                String sCurrentRole = sSCName.substring(0, sSCName.indexOf('.'));
                                sSecurityContext = sCurrentRole + "." + sParentFilterOrg + "." + sParentFilterCS;
                                boolean bExist = SecurityContext.doesSecurityContextExists(context, sSecurityContext);
                                if (!bExist) {
                                    SecurityContext scSecurityContext = new SecurityContext();
                                    scSecurityContext.create(context, sCurrentRole, sParentFilterOrg, sParentFilterCS, DomainConstants.TYPE_SECURITYCONTEXT,
                                            PropertyUtil.getSchemaProperty(context, "vault_eServiceProduction"), new HashMap());
                                }
                                slSecurityContext.addElement(sSecurityContext);
                            }
                            try {
                                SecurityContext.addSecurityContexts(mainContext, person, slSecurityContext);
                            } catch (Throwable e) {
                                logger.error("Assigning " + sSecurityContext + " to " + sActualPersonId + " has failed in addPersonsFromCS method\n", e);
                            }
                            boPerson.close(context);
                            eiSecurityContext.close();
                        }

                    } catch (Exception e) {
                        logger.error("Error in retrieving datas in addPersonsFromCS method\n", e);
                    }

                } catch (MatrixException e) {
                    logger.error("Error in starting transaction in addPersonsFromCS method\n", e);
                }

            } finally {
                ContextUtil.commitTransaction(context);
            }

        } catch (FrameworkException e) {
            logger.error("Error in addPersonsFromCS method\n", e);
        }

    }

    public void unassignSCfromPersonRemoved(Context context, String[] args) {
        try {

            Map<?, ?> hmProgram = (Map<?, ?>) JPO.unpackArgs(args);

            String sSelectedOrg = FrameworkUtil.decodeURL((String) hmProgram.get("FPDM_SCAdministrationOrgFilter"), "UTF8");
            String sSelectedCS = FrameworkUtil.decodeURL((String) hmProgram.get("FPDM_SCAdministrationCSFilter"), "UTF8");
            StringList saSelectedPersonsId = StringUtil.split((String) hmProgram.get("sPersonIds"), ",");

            Context mainContext = new Context(context.getSession());
            mainContext.setApplication("VPLM");
            mainContext.setVault("vplm");

            String sSecurityContext = DomainConstants.EMPTY_STRING;

            for (Object oPersonId : saSelectedPersonsId) {
                String sActualPersonId = (String) ((StringUtil.split((String) oPersonId, ",")).elementAt(0));
                StringList slOldSecurityContexts = new StringList();
                Person person = new Person(sActualPersonId);
                try {

                    ContextUtil.startTransaction(context, true);
                    try {

                        StringList slBus = new StringList(2);
                        slBus.add(DomainConstants.SELECT_ID);
                        slBus.add(DomainConstants.SELECT_NAME);

                        String sWhereClause = "name ~~ \"PSS*." + sSelectedOrg + "." + sSelectedCS + "\"";

                        ExpansionIterator eiSecurityContext = person.getExpansionIterator(context, DomainConstants.RELATIONSHIP_ASSIGNED_SECURITY_CONTEXT, DomainConstants.TYPE_SECURITYCONTEXT, slBus,
                                DomainConstants.EMPTY_STRINGLIST, false, true, (short) 1, sWhereClause, DomainConstants.EMPTY_STRING, (short) 0, true, false, (short) 500);

                        while (eiSecurityContext.hasNext()) {
                            RelationshipWithSelect rwsSC = eiSecurityContext.next();
                            sSecurityContext = rwsSC.getTargetSelectData(DomainConstants.SELECT_NAME);
                            slOldSecurityContexts.addElement(sSecurityContext);
                        }
                        eiSecurityContext.close();

                    } catch (MatrixException e) {
                        logger.error("Error in query iterator loop in unassignSCfromPersonRemoved method\n", e);
                    }
                } finally {
                    ContextUtil.commitTransaction(context);
                }

                if (!slOldSecurityContexts.isEmpty()) {
                    try {
                        SecurityContext.removeSecurityContexts(mainContext, person, slOldSecurityContexts);

                    } catch (Throwable e) {
                        logger.error("Error while removing Security Contexts to " + sActualPersonId + " in unassignSCfromPersonRemoved method\n", e);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error in unassignSCfromPersonRemoved method\n", e);
        }
    }

}
