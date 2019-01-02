package fpdm.pno;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;

import matrix.db.Context;
import matrix.util.StringList;

public class Product_mxJPO {
    private static Logger logger = LoggerFactory.getLogger("fpdm.pno.Product");

    private static final String KEY_PRODUCTS_NOT_AUTOMATICALLY_ASSIGNABLE = "PRODUCTS_NOT_AUTOMATICALLY_ASSIGNABLE";

    private static final String ROLE_PRODUCTS_PAGE_NAME = "FPDM_RoleProductsPage.properties";

    private static final String ROLE_GLOBAL_ROLES_PAGE_NAME = "FPDM_RoleGlobalRolesPage.properties";

    /**
     * Add missing global roles and Products associated the new Security Context (Role) added to the person
     * @plm.usage Trigger: RelationshipAssignedSecurityContextCreateAction Revision: AddNecessaryUserLicenses
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            Person and Security Context IDs
     * @throws Exception
     */
    public void addNecessaryUserLicenses(Context context, String[] args) throws Exception {
        try {
            String sPersonID = args[0];
            String sSecurityContextID = args[1];
            logger.debug("addNecessaryUserLicenses() - sPersonID = <" + sPersonID + "> sSecurityContextID = <" + sSecurityContextID + ">");

            String RELATIONSHIP_SECURITYCONTEXT_ROLE = PropertyUtil.getSchemaProperty(context, "relationship_SecurityContextRole");
            String SELECT_RELATIONSHIP_SECURITY_CONTEXT_ROLE_TO_NAME = "from[" + RELATIONSHIP_SECURITYCONTEXT_ROLE + "].to.name";

            DomainObject doObject = DomainObject.newInstance(context);
            // get Person name
            doObject.setId(sPersonID);
            String sPersonName = doObject.getInfo(context, DomainConstants.SELECT_NAME);
            logger.debug("addNecessaryUserLicenses() - sPersonName = <" + sPersonName + ">");
            // get All already assigned licenses
            ArrayList<String> alAllAssignedProducts = getAssignedProductsToUser(context, sPersonName);
            // get Role name
            doObject.setId(sSecurityContextID);
            String sRoleName = doObject.getInfo(context, SELECT_RELATIONSHIP_SECURITY_CONTEXT_ROLE_TO_NAME);
            logger.debug("addNecessaryUserLicenses() - sRoleName = <" + sRoleName + ">");

            // read the Page FPDM_RoleProductsPage
            fpdm.utils.Page_mxJPO _page = new fpdm.utils.Page_mxJPO(context, ROLE_PRODUCTS_PAGE_NAME);
            // Get Products list needed for the connected role
            ArrayList<String> alProductsToAssign = _page.getPropertyValues(context, sRoleName, ",");
            logger.debug("addNecessaryUserLicenses() - alProductsToAssign = <" + alProductsToAssign + ">");
            // Get Products list which must not be assigned automatically (PRODUCTS_NOT_AUTOMATICALLY_ASSIGNABLE=DEN,DEP,PDM)
            ArrayList<String> alProductsNotAutomaticallyAssignable = _page.getPropertyValues(context, KEY_PRODUCTS_NOT_AUTOMATICALLY_ASSIGNABLE, ",");
            logger.debug("addNecessaryUserLicenses() - alProductsNotAutomaticallyAssignable = <" + alProductsNotAutomaticallyAssignable + ">");

            // get Products list not assigned yet to the user
            HashSet<String> hsProductsNotYetAssigned = getMissedProducts(alAllAssignedProducts, alProductsToAssign);
            // Exclude this list
            hsProductsNotYetAssigned.removeAll(alProductsNotAutomaticallyAssignable);

            // assign the missed Products
            assignNewProductsToUser(context, sPersonName, hsProductsNotYetAssigned);

            // assign the global roles to user
            assignRoleGlobalRolesToUser(context, sPersonName, sRoleName);

        } catch (Exception e) {
            logger.error("Error in addNecessaryUserLicenses()\n", e);
            e.printStackTrace();
            throw e;
        }

    }

    /**
     * Remove unnecessary global roles and Products when the the person lost a Security Context (Role)
     * @plm.usage Trigger: RelationshipAssignedSecurityContextCreateAction Revision: RemoveUnnecessaryUserLicenses
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            Person and Security Context IDs
     * @throws Exception
     */
    public void removeUnnecessaryUserLicenses(Context context, String[] args) throws Exception {
        try {
            String sPersonID = args[0];
            String sSecurityContextID = args[1];
            logger.debug("removeUnnecessaryUserLicenses() - sPersonID = <" + sPersonID + "> sSecurityContextID = <" + sSecurityContextID + ">");

            DomainObject doPerson = DomainObject.newInstance(context);
            // get Person name
            doPerson.setId(sPersonID);
            String sPersonName = doPerson.getInfo(context, DomainConstants.SELECT_NAME);
            logger.debug("removeUnnecessaryUserLicenses() - sPersonName = <" + sPersonName + ">");

            // get Already assigned Licenses
            ArrayList<String> alAllAssignedProducts = getAssignedProductsToUser(context, sPersonName);
            // get necessary assigned Licenses depending on the linked Security Context objects
            HashSet<String> hsAllNecessaryProducts = getNecessaryProductsToUser(context, doPerson);
            // get difference between the two lists to the Licenses to remove
            HashSet<String> hsUnNecessaryProducts = getUnnecessaryProducts(alAllAssignedProducts, hsAllNecessaryProducts);

            // Remove unnecessary Products
            removeUnnecessaryProductsToUser(context, sPersonName, hsUnNecessaryProducts);

            // Remove unnecessary global roles
            removeRoleGlobalRolesToUser(context, doPerson, sPersonName, sSecurityContextID);

        } catch (Exception e) {
            logger.error("Error in removeUnnecessaryUserLicenses()\n", e);
            e.printStackTrace();
            throw e;
        }

    }

    /**
     * Return the Product list assigned to the person
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sPersonName
     *            User name
     * @return
     * @throws FrameworkException
     */
    private ArrayList<String> getAssignedProductsToUser(Context context, String sPersonName) throws FrameworkException {
        ArrayList<String> alAssignedProducts = new ArrayList<String>();

        String sResult = MqlUtil.mqlCommand(context, "print person $1 select product dump", sPersonName);
        StringList slProducts = FrameworkUtil.split(sResult, ",");
        for (Object object : slProducts) {
            alAssignedProducts.add((String) object);
        }
        logger.debug("getAssignedProductsToUser() - sPersonName = <" + sPersonName + "> alAssignedProducts = <" + alAssignedProducts + ">");
        return alAssignedProducts;
    }

    /**
     * Return the Product list assigned to the role
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sPersonName
     *            Role name
     * @return
     * @throws FrameworkException
     */
    private HashSet<String> getNecessaryProductsToUser(Context context, DomainObject doPerson) throws Exception {
        HashSet<String> hsNecessaryProducts = new HashSet<String>();

        String RELATIONSHIP_SECURITYCONTEXT_ROLE = PropertyUtil.getSchemaProperty(context, "relationship_SecurityContextRole");
        String RELATIONSHIP_SECURITYCONTEXT_CONTEXT = PropertyUtil.getSchemaProperty(context, "relationship_AssignedSecurityContext");
        String SELECT_ASSIGNED_ROLE_TO_NAME = "from[" + RELATIONSHIP_SECURITYCONTEXT_CONTEXT + "].to.from[" + RELATIONSHIP_SECURITYCONTEXT_ROLE + "].to.name";

        StringList slRolesStillAssigned = doPerson.getInfoList(context, SELECT_ASSIGNED_ROLE_TO_NAME);
        logger.debug("getNecessaryProductsToUser() - slRolesStillAssigned = <" + slRolesStillAssigned + ">");

        Properties _propertyResource = fpdm.utils.Page_mxJPO.getPropertiesFromPage(context, ROLE_PRODUCTS_PAGE_NAME);
        StringList slTreatedRoles = new StringList();
        String sRoleProducts = null;
        StringList slValues = null;
        for (Object oRoleName : slRolesStillAssigned) {
            if (!slTreatedRoles.contains(oRoleName)) {
                slTreatedRoles.add(oRoleName);
                sRoleProducts = _propertyResource.getProperty((String) oRoleName);
                slValues = FrameworkUtil.split(sRoleProducts, ",");
                for (Object oValue : slValues) {
                    hsNecessaryProducts.add((String) oValue);
                }
            }
        }
        logger.debug("getNecessaryProductsToUser() - hsNecessaryProducts = <" + hsNecessaryProducts + ">");

        return hsNecessaryProducts;
    }

    /**
     * Return the Products list not assigned yet
     * @param alProductsAlreadyAssigned
     *            List of the Products already assigned
     * @param alProductsToAssign
     *            List of the Products assigned to the new Security Context
     * @return
     * @throws Exception
     */
    private HashSet<String> getMissedProducts(ArrayList<String> alProductsAlreadyAssigned, ArrayList<String> alProductsToAssign) throws Exception {
        HashSet<String> hsProductsNotYetAssigned = new HashSet<String>();

        for (String sProductName : alProductsToAssign) {
            logger.debug("getMissedProducts() - sProductName = <" + sProductName + ">");
            if (!alProductsAlreadyAssigned.contains(sProductName)) {
                hsProductsNotYetAssigned.add(sProductName);
            }
        }
        logger.debug("getMissedProducts() - hsProductsNotYetAssigned = <" + hsProductsNotYetAssigned + ">");
        return hsProductsNotYetAssigned;
    }

    /**
     * Return the Products already assigned to a person and that are no more necessary
     * @param alAllAssignedProducts
     *            List of Products already assigned to the user
     * @param hsAllNecessaryProducts
     *            List of necessary Products to the user depending on the Security Context that he have
     * @return
     */
    private HashSet<String> getUnnecessaryProducts(ArrayList<String> alAllAssignedProducts, HashSet<String> hsAllNecessaryProducts) {
        HashSet<String> hsUnNecessaryProducts = new HashSet<String>();

        for (String sProductName : alAllAssignedProducts) {
            if (!hsAllNecessaryProducts.contains(sProductName)) {
                hsUnNecessaryProducts.add(sProductName);
            }
        }
        logger.debug("getUnnecessaryProducts() - hsUnNecessaryProducts = <" + hsUnNecessaryProducts + ">");
        return hsUnNecessaryProducts;
    }

    /**
     * Assign new Products list to a person
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sPersonName
     *            Person name
     * @param hsProductsNotYetAssigned
     *            List of Products to assign
     * @throws FrameworkException
     */
    private void assignNewProductsToUser(Context context, String sPersonName, HashSet<String> hsProductsNotYetAssigned) throws FrameworkException {
        logger.debug("assignNewProductsToUser() - sPersonName = <" + sPersonName + "> hsProductsNotYetAssigned = <" + hsProductsNotYetAssigned + ">");
        for (String sProductName : hsProductsNotYetAssigned) {
            MqlUtil.mqlCommand(context, "modify product $1 add person $2", true, sProductName, sPersonName);
        }
    }

    /**
     * Remove unnecessary Products to person
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sPersonName
     *            Person name
     * @param hsUnNecessaryProducts
     *            List of Products to remove
     * @throws FrameworkException
     */
    private void removeUnnecessaryProductsToUser(Context context, String sPersonName, HashSet<String> hsUnNecessaryProducts) throws FrameworkException {
        logger.debug("removeUnnecessaryProductsToUser() - sPersonName = <" + sPersonName + "> hsUnNecessaryProducts = <" + hsUnNecessaryProducts + ">");
        for (String sProductName : hsUnNecessaryProducts) {
            MqlUtil.mqlCommand(context, "modify product $1 remove person $2", true, sProductName, sPersonName);
        }
    }

    /**
     * Get the Products list assignable only manually
     * @plm.usage : Called from the program emxLicenseBase and the JSP emxPLMOnlineAdminXHRLicenseGet.jsp
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            null
     * @return
     */
    public static ArrayList<String> getAssignableProductsManually(Context context, String[] args) {
        ArrayList<String> alProducts = new ArrayList<String>();

        try {
            Properties _propertyResource = fpdm.utils.Page_mxJPO.getPropertiesFromPage(context, ROLE_PRODUCTS_PAGE_NAME);
            String sPropertyValue = _propertyResource.getProperty(KEY_PRODUCTS_NOT_AUTOMATICALLY_ASSIGNABLE);
            StringList slProducts = FrameworkUtil.split(sPropertyValue, ",");
            for (Object object : slProducts) {
                alProducts.add((String) object);
            }
        } catch (Exception e) {
            // do nothing
        }

        return alProducts;
    }

    /**
     * Assign to the person the related global roles corresponding the role
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sPersonName
     *            User name
     * @param sRoleName
     *            New Role assigned to the user
     * @throws Exception
     */
    private void assignRoleGlobalRolesToUser(Context context, String sPersonName, String sRoleName) throws Exception {
        // read the Page FPDM_RoleProductsPage
        fpdm.utils.Page_mxJPO _page = new fpdm.utils.Page_mxJPO(context, ROLE_GLOBAL_ROLES_PAGE_NAME);
        // Get Products list needed for the connected role
        ArrayList<String> alRoleGlobalRoles = _page.getPropertyValues(context, sRoleName, ",");
        logger.debug("assignRoleGlobalRoles() - sRoleName = <" + sRoleName + "> alRoleGlobalRoles = <" + alRoleGlobalRoles + ">");

        for (String sRole : alRoleGlobalRoles) {
            logger.debug("assignRoleGlobalRoles() - sRole = <" + sRole + ">");
            // assign role the user
            MqlUtil.mqlCommand(context, "modify person $1 assign role $2", true, sPersonName, sRole.trim());
            logger.debug("removeRoleGlobalRolesToUser() - assign role <" + sRole + "> to the user <" + sPersonName + "> done.");
        }
    }

    /**
     * Remove to the person the related global roles corresponding the role
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param doPerson
     *            Person Business Object
     * @param sPersonName
     *            User name
     * @param sSecurityContextID
     *            Security Context ID
     * @throws Exception
     */
    private void removeRoleGlobalRolesToUser(Context context, DomainObject doPerson, String sPersonName, String sSecurityContextID) throws Exception {
        String RELATIONSHIP_SECURITYCONTEXT_ROLE = PropertyUtil.getSchemaProperty(context, "relationship_SecurityContextRole");
        String SELECT_RELATIONSHIP_SECURITY_CONTEXT_ROLE_TO_NAME = "from[" + RELATIONSHIP_SECURITYCONTEXT_ROLE + "].to.name";

        DomainObject doObject = DomainObject.newInstance(context, sSecurityContextID);
        String sRoleName = doObject.getInfo(context, SELECT_RELATIONSHIP_SECURITY_CONTEXT_ROLE_TO_NAME);
        logger.debug("removeRoleGlobalRolesToUser() - sRoleName = <" + sRoleName + ">");

        // read the Page FPDM_RoleProductsPage
        fpdm.utils.Page_mxJPO _page = new fpdm.utils.Page_mxJPO(context, ROLE_GLOBAL_ROLES_PAGE_NAME);
        // Get global roles corresponding to this role
        ArrayList<String> alRoleGlobalRoles = _page.getPropertyValues(context, sRoleName, ",");
        logger.debug("removeRoleGlobalRolesToUser() - sRoleName = <" + sRoleName + "> alRoleGlobalRoles = <" + alRoleGlobalRoles + ">");

        // get necessary assigned Licenses depending on the linked Security Context objects
        HashSet<String> hsAllNecessaryGlobalRoles = getNecessaryGlobalRolesToUser(context, doPerson);
        logger.debug("removeRoleGlobalRolesToUser() - hsAllNecessaryGlobalRoles = <" + hsAllNecessaryGlobalRoles + ">");

        // Remove unnecessary global roles
        for (String sRole : alRoleGlobalRoles) {
            logger.debug("removeRoleGlobalRolesToUser() - sRole = <" + sRole + ">");
            if (!hsAllNecessaryGlobalRoles.contains(sRole)) {
                // remove assign role to the user
                MqlUtil.mqlCommand(context, "modify person $1 remove assign role $2", true, sPersonName, sRole.trim());
                logger.debug("removeRoleGlobalRolesToUser() - remove assign role <" + sRole + "> to the user <" + sPersonName + "> the done.");
            }
        }

    }

    /**
     * Return the all global roles corresponding the roles still assigned to the user
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param doPerson
     *            User business object
     * @return
     * @throws FrameworkException
     */
    private HashSet<String> getNecessaryGlobalRolesToUser(Context context, DomainObject doPerson) throws Exception {
        HashSet<String> hsNecessaryGlobalRoles = new HashSet<String>();

        String RELATIONSHIP_SECURITYCONTEXT_ROLE = PropertyUtil.getSchemaProperty(context, "relationship_SecurityContextRole");
        String RELATIONSHIP_SECURITYCONTEXT_CONTEXT = PropertyUtil.getSchemaProperty(context, "relationship_AssignedSecurityContext");
        String SELECT_ASSIGNED_ROLE_TO_NAME = "from[" + RELATIONSHIP_SECURITYCONTEXT_CONTEXT + "].to.from[" + RELATIONSHIP_SECURITYCONTEXT_ROLE + "].to.name";

        StringList slRolesStillAssigned = doPerson.getInfoList(context, SELECT_ASSIGNED_ROLE_TO_NAME);
        logger.debug("getNecessaryGlobalRolesToUser() - slRolesStillAssigned = <" + slRolesStillAssigned + ">");

        Properties _propertyResource = fpdm.utils.Page_mxJPO.getPropertiesFromPage(context, ROLE_GLOBAL_ROLES_PAGE_NAME);
        StringList slTreatedRoles = new StringList();
        String sRoleProducts = null;
        StringList slValues = null;
        for (Object oRoleName : slRolesStillAssigned) {
            if (!slTreatedRoles.contains(oRoleName)) {
                slTreatedRoles.add(oRoleName);
                sRoleProducts = _propertyResource.getProperty((String) oRoleName);
                slValues = FrameworkUtil.split(sRoleProducts, ",");
                for (Object oValue : slValues) {
                    hsNecessaryGlobalRoles.add((String) oValue);
                }
            }
        }
        logger.debug("getNecessaryGlobalRolesToUser() - hsNecessaryGlobalRoles = <" + hsNecessaryGlobalRoles + ">");

        return hsNecessaryGlobalRoles;
    }

}
