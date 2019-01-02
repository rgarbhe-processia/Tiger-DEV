package fpdm.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;

import matrix.db.Context;
import matrix.db.MQLCommand;
import matrix.util.Pattern;
import matrix.util.StringList;
import pss.constants.TigerConstants;

public class Person_mxJPO {

    private final static Logger logger = LoggerFactory.getLogger("fpdm.utils.Person");

    private static Map<String, String> mOrganizationName = null;

    private static String STR_ORGANIZATION_FAS = "FAS";

    private static String STR_ORGANIZATION_FIS = "FIS";

    private static String STR_ORGANIZATION_FECT = "FECT";

    private static String STR_ORGANIZATION_FAE = "FAE";

    /**
     * Delete file of the Local Configuration Object
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param aPersonIds
     *            List of persons owner of the Local Configuration Objects
     * @return
     * @throws Exception
     */
    public Vector<String> deleteLocalConfigFiles(Context context, String[] aPersonIds) throws Exception {
        boolean bPushedContext = false;
        Vector<String> vResult = new Vector<String>();

        try {
            logger.debug("deleteLocalConfigFiles() - aPersonIds = <" + Arrays.toString(aPersonIds) + ">");
            String sTypeLocalConfig = PropertyUtil.getSchemaProperty(context, "type_MCADInteg-LocalConfig");
            String sVaultProduction = TigerConstants.VAULT_ESERVICEPRODUCTION;

            StringList slSelect = new StringList();
            slSelect.addElement(DomainConstants.SELECT_ID);
            slSelect.addElement(DomainConstants.SELECT_NAME);

            MQLCommand mql = new MQLCommand();
            mql.open(context);

            ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, null, null);
            bPushedContext = true;
            Pattern pNamePerson = new Pattern(null);

            DomainObject doPerson = DomainObject.newInstance(context);
            String sPersonID = null;
            for (int i = 0; i < aPersonIds.length; i++) {
                sPersonID = (String) FrameworkUtil.split(aPersonIds[i], "|").get(0);
                doPerson.setId(sPersonID);
                String sNamePerson = doPerson.getInfo(context, DomainConstants.SELECT_NAME);
                pNamePerson.addPattern(sNamePerson);
            }
            MapList mlListObject = DomainObject.findObjects(context, sTypeLocalConfig, pNamePerson.getPattern(), "*", "*", sVaultProduction, "last.id==id", false, slSelect);
            for (Iterator<?> it = mlListObject.iterator(); it.hasNext();) {
                Map<?, ?> mInfo = (Map<?, ?>) it.next();
                String sObjectId = (String) mInfo.get("id");
                if (mql.executeCommand(context, "delete bus $1 file all", sObjectId)) {
                    vResult.add((String) mInfo.get(DomainConstants.SELECT_NAME));
                }
            }
            logger.debug("deleteLocalConfigFiles() - deleted file only of found LCO objects for the users <" + vResult + ">");

        } catch (RuntimeException e) {
            logger.error("Error in deleteLocalConfigFiles ", e);
            throw e;
        } catch (Exception e) {
            logger.error("Error in deleteLocalConfigFiles ", e);
            throw e;
        } finally {
            if (bPushedContext) {
                ContextUtil.popContext(context);
            }
        }
        return vResult;
    }

    /**
     * Delete Local Configuration Object
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param aPersonIds
     *            List of persons owner of the Local Configuration Objects
     * @return
     * @throws Exception
     */
    public Vector<String> deleteLocalConfigObjects(Context context, String[] aPersonIds) throws Exception {
        boolean bPushedContext = false;
        Vector<String> vResult = new Vector<String>();

        try {
            logger.debug("deleteLocalConfigObjects() - aPersonIds = <" + Arrays.toString(aPersonIds) + ">");
            String sTypeLocalConfig = PropertyUtil.getSchemaProperty(context, "type_MCADInteg-LocalConfig");
            String sVaultProduction = TigerConstants.VAULT_ESERVICEPRODUCTION;

            StringList slSelect = new StringList();
            slSelect.addElement(DomainConstants.SELECT_ID);
            slSelect.addElement(DomainConstants.SELECT_NAME);

            ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, null, null);
            bPushedContext = true;
            DomainObject doObject = DomainObject.newInstance(context);

            Pattern pNamePerson = new Pattern(null);
            String sPersonID = null;
            for (int i = 0; i < aPersonIds.length; i++) {
                sPersonID = (String) FrameworkUtil.split(aPersonIds[i], "|").get(0);
                doObject.setId(sPersonID);
                String sNamePerson = doObject.getInfo(context, DomainConstants.SELECT_NAME);
                pNamePerson.addPattern(sNamePerson);
            }
            MapList mlListObject = DomainObject.findObjects(context, sTypeLocalConfig, pNamePerson.getPattern(), "*", "*", sVaultProduction, "last.id==id", false, slSelect);
            for (Iterator<?> it = mlListObject.iterator(); it.hasNext();) {
                Map<?, ?> mInfo = (Map<?, ?>) it.next();
                String sObjectId = (String) mInfo.get("id");
                doObject.setId(sObjectId);
                doObject.deleteObject(context);
                vResult.add((String) mInfo.get(DomainConstants.SELECT_NAME));
            }
            logger.debug("deleteLocalConfigObjects() - deleted LCO  objects for the users <" + vResult + ">");
        } catch (Exception e) {
            logger.error("Error in deleteLocalConfigObjects ", e);
            throw e;
        } finally {
            if (bPushedContext) {
                ContextUtil.popContext(context);
            }
        }
        return vResult;
    }

    /**
     * get instance
     * @return
     */
    public static synchronized Map<String, String> getOrganizationMapInstance() {
        return (mOrganizationName != null ? mOrganizationName : (mOrganizationName = new HashMap<String, String>()));
    }

    /**
     * Get person organization name from its context
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sPersonneName
     *            user name
     * @return
     * @throws FrameworkException
     */
    public static String getOrganizationName(Context context, String sPersonneName) throws FrameworkException {
        String sOrganizationName = null;
        try {
            Map<String, String> mOrganizationName = getOrganizationMapInstance();
            if (mOrganizationName.containsKey(sPersonneName)) {
                sOrganizationName = mOrganizationName.get(sPersonneName);
            } else {
                // get all ancestor organizations of the current user context
                String sAncestorOrgs = MqlUtil.mqlCommand(context, "print context select $1 dump", "role.org.ancestor");
                logger.debug("getOrganizationName() - sAncestorOrgs = <" + sAncestorOrgs + ">");

                if (sAncestorOrgs != null) {
                    if (sAncestorOrgs.contains(STR_ORGANIZATION_FAS)) {
                        sOrganizationName = STR_ORGANIZATION_FAS;
                    } else if (sAncestorOrgs.contains(STR_ORGANIZATION_FIS)) {
                        sOrganizationName = STR_ORGANIZATION_FIS;
                    } else if (sAncestorOrgs.contains(STR_ORGANIZATION_FECT)) {
                        sOrganizationName = STR_ORGANIZATION_FECT;
                    } else if (sAncestorOrgs.contains(STR_ORGANIZATION_FAE)) {
                        sOrganizationName = STR_ORGANIZATION_FAE;
                    }
                }

                if (sOrganizationName != null) {
                    mOrganizationName.put(sPersonneName, sOrganizationName);
                    logger.info("getOrganizationName() - sPersonneName = <" + sPersonneName + "> Organization = <" + mOrganizationName + ">");
                }
            }
            logger.debug("getOrganizationName() - sPersonneName = <" + sPersonneName + "> Organization = <" + mOrganizationName + ">");
        } catch (FrameworkException e) {
            logger.error("Error in getOrganizationName ", e);
            throw e;
        }

        return sOrganizationName;
    }

}
