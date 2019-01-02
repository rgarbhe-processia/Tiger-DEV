
/*
 ** MxUGCustomJPO
 **
 ** Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 **
 ** Program to use as trigger on Revise event of Major Objects
 **
 ** This program is enhanced to implement support for Update PLM Parameters during Save from NX integration.
 **
 */

import java.io.*;
import java.util.*;
import matrix.db.*;
import matrix.db.Context;
import matrix.db.MatrixWriter;
import matrix.util.MatrixException;
import matrix.util.StringList;
import matrix.db.MatrixLogWriter;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.ContextUtil;

public class MxUGCustomJPO_mxJPO {
    // Define this variable to True for working in Customer Environment.
    private boolean bCustomerEnvironment = false;

    // Attribute for reading the User initials.
    private String ATTR_CADSIGNATURE = "CAD Signature";

    // Policy name definition for reading Initial Revision and State names.
    private String POLICY_NX_DESIGN = "PSS_CAD_Object";

    private static final boolean DEBUG = true;

    private static final boolean WRITE_DEBUG_TO_RMI_LOGS = true;

    private MatrixLogWriter matrixLogger = null;

    /**
     * The no-argument constructor.
     */
    public MxUGCustomJPO_mxJPO() {
    }

    /**
     * Constructor which accepts the Matrix context and an array of String arguments.
     */
    public MxUGCustomJPO_mxJPO(Context context, String[] args) throws Exception {
    }

    public int mxMain(Context context, String[] args) throws Exception {
        return 0;
    }

    public String logUnSavedTNRDetails(Context context, String[] args) throws Exception {
        String sResult = "failed";
        try {

            System.out.println("[MxUGCustomJPO.logUnSavedTNRDetails]... entered ");
            String errorName = args[0];
            String userName = args[1];
            String topLevelAssemblyName = args[2];
            String unSavedFileNames = args[3];

            System.out.println("[MxUGCustomJPO.logUnSavedTNRDetails]... errorName: " + errorName + " + userName: " + userName + " topLevelAssemblyName: " + topLevelAssemblyName + " unSavedFileNames: "
                    + unSavedFileNames);

            String output = "errorName: " + errorName + " userName: " + userName + " topLevelAssemblyName: " + topLevelAssemblyName + " unSavedFileNames: " + unSavedFileNames;
            write("MxUGCustomJPO_logUnSavedTNRDetails.log", output);
            System.out.println("Sucessfull writing of log MxUGCustomJPO_logUnSavedTNRDetails.log");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("[MxUGCustomJPO.logUnSavedTNRDetails]... Error logging TNR...");
        }

        return sResult;
    }

    private void write(String fileName, String output) throws IOException {
        FileWriter aWriter = new FileWriter(fileName, true);
        aWriter.write(output + "\n");
        aWriter.flush();
        aWriter.close();
    }

    /**
     * getCommonPLMAttributes returns Common PLM attributes to be updated in the Design file. e.g., DESIGNED_BY, MODIFIED_BY,INITIALREVISION and INITIALSTATE. In addition, any other parameters can
     * also be returned as key-value pairs with the following syntax. PARAM1|param1Value@PARAM2|param2Value@...
     */
    public String getCommonPLMAttributes(Context context, String[] args) throws Exception {
        String sResult = "";

        logMessage("getCommonPLMAttributes", "Entered...");
        try {
            // Read User Initials from attribute 'CAD Signature' on the Person object of the Context User.
            String sUserCredentials = getUserIntials(context);
            String sDesignedBy = "DESIGNED_BY|" + sUserCredentials;
            String sModifiedBy = "MODIFIED_BY|" + sUserCredentials;

            // Read Initial Revision and State name from the Policy.
            String sPolicyName = POLICY_NX_DESIGN;
            String sInitialRevAndState = getPolicyInitialRevAndInitialState(context, sPolicyName);

            sResult = sDesignedBy + "@" + sModifiedBy + "@" + sInitialRevAndState;
            logMessage("getCommonPLMAttributes", "Returning sResult: " + sResult);
        } catch (Exception e) {
            System.out.println("getCommonPLMAttributes: Exception occurred while retriveing common PLM attributes.");
        }

        return sResult;
    }

    /**
     * getPolicyInitialRevAndInitialState To Read Initial Revision and State Name from the Policy.
     **/
    private String getPolicyInitialRevAndInitialState(Context context, String policyName) throws Exception {
        String sResult = "";

        logMessage("getPolicyInitialRevAndInitialState", "Entered...");

        // Query 'INITIAL REVISION' in the revision sequence of the Policy.
        String sInitalRevision = "";
        String sInitialState = "";
        matrix.db.Policy policyObj = new matrix.db.Policy(policyName);
        policyObj.open(context);
        String sFirstRev = policyObj.getFirstInSequence();
        String sFirstRevision = policyObj.getFirstInMinorSequence();
        // System.out.println("firstRev: " + sFirstRev);
        // System.out.println("firstRevision: " + sFirstRevision);
        policyObj.close(context);

        sInitalRevision = "INITIALREVISION|" + sFirstRevision;
        logMessage("getPolicyInitialRevAndInitialState", "Initial Revision defined in Revision Sequence of the Policy, sInitalRevision: " + sInitalRevision);

        // Query 'name' of the First State in the Lifecycle.
        String sStatesQuery = "print policy '" + policyName + "' select state dump |";
        String sStateNamesResult = executeMQLCommand(context, sStatesQuery);
        if (sStateNamesResult.startsWith("true|")) {
            sStateNamesResult = sStateNamesResult.substring(5);
            int pipeIdx = sStateNamesResult.indexOf("|");
            if (pipeIdx > -1) {
                sInitialState = sStateNamesResult.substring(0, pipeIdx);
                sInitialState = "INITIALSTATE|" + sInitialState;
                logMessage("getPolicyInitialRevAndInitialState", "Initial State defined in Life Cycle of the Policy, sInitalState: " + sInitialState);
            }
        }

        sResult = sInitalRevision + "@" + sInitialState;
        logMessage("getPolicyInitialRevAndInitialState", "returning value: sResult: " + sResult);

        return sResult;
    }

    /**
     * getUserIntials returns the value of CAD Signature attribute from Person Object.
     **/
    private String getUserIntials(Context context) throws Exception {
        String sCADSignature = "";

        logMessage("getUserIntials", "Entered...");
        try {
            if (!bCustomerEnvironment) {
                // For OOTB, the 'CAD Signature' attribute won't be present.
                // So, return User Name.
                String sContextUserName = context.getUser();
                sCADSignature = sContextUserName;
            } else {
                //
                // TODO - In customer environment, if the CAD Signature is to be read from an attribute on the Person
                // Object, implement the code to read it from the Person Object and return it from here.
                // e.g., sCADSignature = "Test User"
                //
                sCADSignature = "";
            }

            logMessage("getUserIntials", "sCADSignature: " + sCADSignature);
        } catch (Exception e) {
        }

        return sCADSignature;
    }

    /**
     * Method to execute the MQL command.
     * @param context
     * @param mqlCmd
     * @return
     */
    private String executeMQLCommand(Context context, String mqlCmd) throws Exception {
        String mqlResult = "";
        logMessage("executeCommand", "Entered...");
        try {
            if (context != null) {
                MQLCommand mqlc = new MQLCommand();

                logMessage("executeCommand", "mqlCmd: " + mqlCmd);
                logMessage("executeCommand", "context user: " + context.getUser());
                boolean bRet = mqlc.executeCommand(context, mqlCmd);
                if (bRet) {
                    mqlResult = mqlc.getResult();
                    logMessage("executeCommand", "mqlResult: " + mqlResult);
                    if (mqlResult != null && mqlResult.length() > 0) {
                        mqlResult = "true|" + mqlResult;
                    } else if (mqlResult.length() == 0) {
                        mqlResult = "false|" + mqlResult;
                    }
                } else {
                    mqlResult = mqlc.getError();
                    mqlResult = "false|" + mqlResult;
                }

                logMessage("executeCommand", "mqlResult: " + mqlResult.length());
                if (mqlResult.endsWith("\n")) {
                    mqlResult = mqlResult.substring(0, (mqlResult.lastIndexOf("\n")));
                }
            } else {
                mqlResult = "false|" + "Invalid Context is passed for executing MQL command...";
            }
        } catch (Exception me) {
            mqlResult = "false|" + me.getMessage();
            throw new Exception("[executeMQLCommand]... Error occured: " + mqlResult);
        }

        logMessage("executeCommand", "returning mqlResult: " + mqlResult);
        return mqlResult;
    }

    private void logMessage(String method, String message) {
        if (DEBUG) {
            String logMessage = new Date() + " ::: [ITIMxMCAD_ENOVIANewCustomJPO." + method + "] :::  " + message;
            if (WRITE_DEBUG_TO_RMI_LOGS && matrixLogger != null) {
                try {
                    matrixLogger.write(logMessage);
                    matrixLogger.flush();
                } catch (Exception e) {
                    System.out.println(logMessage);
                }
            } else {
                System.out.println(logMessage);
            }
        }
    }
}