
/*
 ** MCADSessionStatusCheck
 **
 ** Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 **
 ** To check the Server Connection status by the CSEs like NX/PRO integration.
 **
 */
import matrix.db.*;

public class MCADSessionStatusCheck_mxJPO {

    /**
     * Constructor.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @throws Exception
     *             if the operation fails
     */
    public MCADSessionStatusCheck_mxJPO(Context context, String[] args) throws Exception {

    }

    public int mxMain(Context context, String[] args) throws Exception {
        return 0;
    }

    /**
     * function: checkServerConnectionStatus No arguments are expected to pass to this function. This function is executed by the CSE (NX/PRO) through DEC's 'executeJPO' protocol to check if the
     * Server connection is active or not. If this function is executed properly, the value 'true' is returned indicating that the connection is active. Otherwise, DEC will return an error code that
     * indicated the session is timedout.
     */
    public String checkServerConnectionStatus(Context context, String[] args) throws Exception {
        String result = "true";
        // System.out.println("MCADSessionStatusCheck.checkServerConnectionStatus entered...");

        // System.out.println("MCADSessionStatusCheck.checkServerConnectionStatus returned...");
        return result;
    }

}
