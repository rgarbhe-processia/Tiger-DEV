
/**
 * ${CLASSNAME} 2nd April 2018 - created Copyright (c) 2009/2010 TranscenData, An ITI Business. All Rights Reserved. This program contains proprietary and trade secret information of TranscenData Inc.
 * Copyright notice is precautionary only and does not evidence any actual or intended publication of such program Revision History
 * --------------------------------------------------------------------------------------------- arabinda 4/2/2018 - DRAFT arabinda 5/22/2018 - Added logger statements
 */

import java.util.Iterator;
import java.util.Map;

import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PersonUtil;

import matrix.util.MatrixException;
import matrix.util.StringList;
import com.matrixone.apps.domain.util.ContextUtil;

import javax.servlet.http.HttpSession;
import com.matrixone.apps.domain.util.i18nNow;
import java.util.StringTokenizer;

import matrix.db.*;
import java.util.Date;

/**
 * The <code>ITISecurityContextUtil</code> Utility class for security context functionality Copyright (c) 2009/2010 TranscenData, An ITI Business.
 */
public class ITISecurityContextUtil_mxJPO {
    private Context context = null;

    private MatrixLogWriter mtxLogWriter = null;

    /**
     * Constructor.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @throws Exception
     *             if the operation fails
     * @since AEF 10.0.SP4
     * @grade 0
     */
    public ITISecurityContextUtil_mxJPO() {
    }

    /**
     * Constructor.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @throws Exception
     *             if the operation fails
     * @since AEF 10.0.SP4
     * @grade 0
     */

    public ITISecurityContextUtil_mxJPO(Context context, String[] args) throws Exception {
        try {
            this.context = context;
            mtxLogWriter = new MatrixLogWriter(context);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    /**
     * This method is executed if a specific method is not specified.
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param args
     *            holds no arguments.
     * @return int.
     * @throws Exception
     *             if the operation fails.
     * @since AEF 9.5.0.0.
     */

    public int mxMain(Context context, String[] args) throws Exception {

        String scVal = getSecurityContextRoleInfo(context, args);

        if (true) {
            // throw new Exception("must specify method on MxPLinkMassRevise invocation");
        }
        return 0;
    }

    /**
     * This method is used to retrieve the current security context assigned to the user
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param args
     *            holds no arguments.
     * @return String.
     * @throws Exception
     *             if the operation fails.
     */

    public String getSecurityContext(Context context, String[] args) throws Exception {

        String returnVal = "failure";
        String securityContextVal = "";

        try {
            if (null == mtxLogWriter)
                mtxLogWriter = new MatrixLogWriter(context);
            printDebug("Calling ITISecurityContextUtil:getSecurityContext() .....starts.........");

            securityContextVal = PersonUtil.getDefaultSecurityContext(context);
            printDebug("securityContextVal =  " + securityContextVal);

            returnVal = securityContextVal;
        } catch (Exception ex) {
            ex.printStackTrace();
            returnVal = "failure|" + ex.toString();
            printDebug("Error retrieving assigned security context information = " + ex.toString());
        }

        printDebug("Calling ITISecurityContextUtil:getSecurityContext() .....ends.........");

        return returnVal;
    }

    /**
     * This method is used to retrieve all security contexts assigned to the user
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param args
     *            holds no arguments.
     * @return String.
     * @throws Exception
     *             if the operation fails.
     */
    public String getAssignedSecurityContexts(Context context, String[] args) throws Exception {
        String returnVal = "failure";
        String loginUser = "";
        MapList securityContextDetails = null;

        StringBuffer scValBfr = new StringBuffer("");
        try {
            if (null == mtxLogWriter)
                mtxLogWriter = new MatrixLogWriter(context);
            printDebug("Calling ITISecurityContextUtil:getAssignedSecurityContexts() .....starts.........");

            loginUser = context.getUser();
            printDebug("loginUser = " + loginUser);

            StringList selects = new StringList();
            selects.addElement(DomainObject.SELECT_NAME);

            securityContextDetails = PersonUtil.getSecurityContexts(context, loginUser, selects);
            printDebug("securityContextDetails = " + securityContextDetails);

            if (null != securityContextDetails && securityContextDetails.size() > 0) {
                Iterator itr = securityContextDetails.iterator();
                Map scMap = null;
                String scName = "";
                int count = 0;

                while (itr.hasNext()) {
                    scMap = (Map) itr.next();
                    scName = (String) scMap.get(DomainObject.SELECT_NAME);

                    if (count == 0) {
                        scValBfr.append(scName);
                    } else {
                        scValBfr.append("|").append(scName);
                    }

                    count++;

                }

                returnVal = scValBfr.toString();
            } else {
                returnVal = "failure|No security context assigned to user";
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            returnVal = "failure|" + ex.toString();
            printDebug("Error retrieving assigned security context information = " + ex.toString());
        }

        printDebug("returnVal = " + returnVal);
        printDebug("Calling ITISecurityContextUtil:ITISecurityContextUtil() .....ends.........");

        return returnVal;

    }

    /**
     * This method is used to update the security context assigned to user
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param args
     *            holds no arguments.
     * @return String.
     * @throws Exception
     *             if the operation fails.
     */
    public String updateSecurityContext(Context context, String[] args) throws MatrixException {

        String scOrganization = "";
        String scProject = "";
        String scRole = "";
        String newSecurityCtxRole = "";

        String status = "";

        try {

            if (null == mtxLogWriter)
                mtxLogWriter = new MatrixLogWriter(context);
            printDebug("Calling ITISecurityContextUtil:updateSecurityContext() .....starts.........");

            String currentRole = context.getRole();
            printDebug("currentRole = " + currentRole);

            scOrganization = args[0];
            scProject = args[1];
            scRole = args[2];

            printDebug("scOrganization = " + scOrganization);
            printDebug("scProject = " + scProject);
            printDebug("scRole = " + scRole);

            newSecurityCtxRole = "ctx::" + scRole + "." + scOrganization + "." + scProject;

            // context.resetContext(context.getUser(), context.getPassword(), context.getVault().getName());
            context.resetRole(newSecurityCtxRole);
            printDebug("Security context updated successfully...");

            String scVal = PersonUtil.getSecurityContext(context, scOrganization, scProject, scRole);
            PersonUtil.setDefaultSecurityContext(context, scVal);
            printDebug("Default security context profile updated successfully...");

            status = "Success|" + "Security context updated successfully";

        } catch (Exception ex) {
            ex.printStackTrace();
            printDebug("Error in updating security context = " + ex.toString());
            status = "Failure|" + ex.toString();

        }

        printDebug("Return val = " + status);
        printDebug("Calling ITISecurityContextUtil:updateSecurityContext() .....ends.........");
        return status;
    }

    /**
     * This method is used to retrieve all security contexts role info
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param args
     *            holds no arguments.
     * @return String.
     * @throws Exception
     *             if the operation fails.
     */
    public String getSecurityContextRoleInfo(Context context, String[] args) throws Exception {

        String returnVal = "failure";
        String loginUser = "";
        MapList securityContextDetails = null;

        i18nNow bundle = new i18nNow();
        StringBuffer scValBfr = new StringBuffer("");

        try {
            if (null == mtxLogWriter)
                mtxLogWriter = new MatrixLogWriter(context);
            printDebug("Calling ITISecurityContextUtil:getSecurityContextRoleInfo() .....starts.........");

            loginUser = context.getUser();
            printDebug("loginUser = " + loginUser);

            StringList selects = new StringList();
            selects.addElement(DomainObject.SELECT_NAME);

            securityContextDetails = PersonUtil.getSecurityContexts(context, loginUser, selects);
            printDebug("securityContextDetails = " + securityContextDetails);

            if (null != securityContextDetails && securityContextDetails.size() > 0) {
                Iterator itr = securityContextDetails.iterator();
                Map scMap = null;
                String scName = "";
                int count = 0;
                String roleName = "";
                String roleDispName = "";

                while (itr.hasNext()) {
                    scMap = (Map) itr.next();
                    scName = (String) scMap.get(DomainObject.SELECT_NAME);

                    StringTokenizer scTokens = new StringTokenizer(scName, ".");
                    if (null != scTokens) {
                        if (scTokens.hasMoreTokens()) {
                            roleName = scTokens.nextToken();
                            printDebug("roleName = " + roleName);
                            roleDispName = bundle.GetString("emxFrameworkStringResource", "en", "emxFramework.Role." + roleName);
                            printDebug("roleDispName = " + roleDispName);
                            if (null != roleDispName && !"".equals(roleDispName.trim())) {
                                // do nothing
                            } else {
                                roleDispName = roleName;
                            }

                            scName = roleName + "," + roleDispName;

                            if (count == 0) {
                                scValBfr.append(scName);
                            } else {
                                scValBfr.append("|").append(scName);
                            }

                            count++;
                        }
                    }

                }

                returnVal = scValBfr.toString();
            } else {
                returnVal = "failure|No security context assigned to user";
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            returnVal = "failure|" + ex.toString();
        }

        printDebug("Return val = " + returnVal);
        printDebug("Calling ITISecurityContextUtil:getSecurityContextRoleInfo() .....ends.........");

        return returnVal;

    }

    /**
     * Print message into the log file and also console.
     * @param msg
     */
    private void printDebug(String msg) {
        try {
            if (msg != null) {
                msg = new Date() + "::: " + msg + "\n";
                if (mtxLogWriter != null) {
                    mtxLogWriter.write(msg);
                    mtxLogWriter.flush();
                }
            }
        } catch (Exception e) {
            printDebug("Error occured while logging messages..." + e.getMessage());
        }
    }
}