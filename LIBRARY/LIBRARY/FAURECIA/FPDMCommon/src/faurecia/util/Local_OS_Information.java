/*
 * Creation Date : 8 july 2004
 *
 * Pour changer le modele de ce fichier genere, allez a :
 * Fenetre&gt;Preferences&gt;Java&gt;Generation de code&gt;Code et commentaires
 */
package faurecia.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Date;

/**
 * @author fcolin
 *
 * Pour changer le modele de ce commentaire de type genere, allez a :
 * Fenetre&gt;Preferences&gt;Java&gt;Generation de code&gt;Code et commentaires
 */
public class Local_OS_Information {

	//FPDM ADD Start JFA 09/06/2006 RFC 4625, RFC 4326
	private final static String CLASSNAME = "Local_OS_Information";

	public static String VISVIEW_UNIX = "visviewpro";
	public static String VVLAUNCH_UNIX = "vvprolaunch";

	public static String VISVIEW_WIN = "VisView.exe";
	public static String VVLAUNCH_WIN = "VVLaunch.exe";

	public static String PRODUCTS_DIRECTORY_WIN = "Products";
	public static String PROGRAM_DIRECTORY_WIN = "Program";

	public static String FILE_EXTENSION_PLT = ".plt";
	public static String FILE_EXTENSION_TIF = ".tif";
	public static String FILE_EXTENSION_TIFF = ".tiff";

    //FPDM ADD Start JFA 28/08/2006 RFC 2528
    public static String FILE_EXTENSION = FILE_EXTENSION_TIF;
    //FPDM ADD End JFA 28/08/2006 RFC 2528

	public static String getCommandToExecute(boolean isWin, String sCommand) throws Exception {
		String command = "";
		if (isWin){
			if (sCommand.indexOf(VISVIEW_WIN) > 0 ){
				command = sCommand.substring(0, sCommand.lastIndexOf(" "));
				int index = command.indexOf(PRODUCTS_DIRECTORY_WIN);
				String sTempCommand = command.substring(0, index);
				DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/getCommandToExecute : For Windows -- Test if vvlaunch program exists...");
				sTempCommand = sTempCommand.concat(PROGRAM_DIRECTORY_WIN + "\\" + VVLAUNCH_WIN);
				DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/getCommandToExecute : sCommandToTest : " + sTempCommand);
				boolean existsPrg = (new File(sTempCommand)).exists();
			    if (existsPrg) {
			    	command = sTempCommand;
			    	DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/getCommandToExecute : For Windows -- command : " + command);
			    }
			}
			else{
				if (sCommand.indexOf(" ") > -1){
					command = sCommand.substring(0, sCommand.lastIndexOf(" "));
				}
				else{
					command = sCommand;
				}
				DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/getCommandToExecute : For Windows -- command : " + command);
			}
		}
		else{
			if (sCommand.indexOf(VISVIEW_UNIX) > 0 ){
        		String sTempCommand = sCommand.substring(0, sCommand.lastIndexOf(VISVIEW_UNIX));
				DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/getCommandToExecute : For Unix -- Test if vvlaunch program exists...");

				sTempCommand = sTempCommand.concat(VVLAUNCH_UNIX);
				DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/getCommandToExecute : sCommandToTest : " + sTempCommand);

				boolean existsPrg = (new File(sTempCommand)).exists();
			    if (existsPrg) {
			    	command = StringUtil.replaceFirst(sCommand, VISVIEW_UNIX, VVLAUNCH_UNIX);
					DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/getCommandToExecute : For Unix -- command modified: " + command);
			    } else {
                    command = sCommand;
                    DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/getCommandToExecute : For Unix -- command : " + command);
                }
        	}
			else{
				command = sCommand;
				DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/getCommandToExecute : For Unix -- command : " + command);
			}
		}
		return command;
	}
	//FPDM ADD End JFA 09/06/2006 RFC 4625, RFC 4326

	//FPDM MODIFY Start JFA 09/06/2006 RFC 4625, RFC 4326
    public static String getUnixEnv(String env) throws Exception {
        String type = System.getProperty("os.name");
        String sRetrievedCommand;
        boolean isWin = false;
        if(type.indexOf("Windows")<0) {
            sRetrievedCommand = Local_OS_Information.executeCommand(new String[]{"ksh", "-c", "echo $" + env }, 2000, true);
        }
        else {
        	isWin = true;
            sRetrievedCommand = Local_OS_Information.executeCommand("cmd /c set " + env, 2000, true);
        }
        if(sRetrievedCommand.indexOf('\n')>=0) {
            sRetrievedCommand = sRetrievedCommand.substring(0, sRetrievedCommand.indexOf('\n'));
        }

        String sCommand = sRetrievedCommand.substring(sRetrievedCommand.indexOf("=") + 1).trim();

        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/getUnixEnv Command to launch : " + sCommand);

        return getCommandToExecute(isWin, sCommand);
    }
    //  FPDM MODIFY End JFA 09/06/2006 RFC 4625, RFC 4326

    public static String executeCommand(String sCmd, long maxDelayInMs, boolean destroyAfterDelay) throws Exception {
        DebugUtil.debug(DebugUtil.INFO, CLASSNAME, "/executeCommand Command executed : " + sCmd);

        final String sCmdFinal = sCmd;
        final boolean destroyAfterDelayFinal = destroyAfterDelay;
        final long maxDelayInMsFinal = maxDelayInMs;

        try {
        String sResult = (String) AccessController.doPrivileged(new PrivilegedExceptionAction() {
            public Object run()throws Exception {
                Runtime run = Runtime.getRuntime();
                Process process = run.exec(sCmdFinal);
                String sOutput = executeCommand(process, sCmdFinal, maxDelayInMsFinal, destroyAfterDelayFinal);
                return sOutput;
            }
        });

        return sResult;
    }
        catch(PrivilegedActionException pae) {
            throw pae.getException();
        }
    }

    public static String executeCommand(String[] sCmd, long maxDelayInMs, boolean destroyAfterDelay) throws Exception {
        DebugUtil.debug(DebugUtil.INFO, CLASSNAME, "/executeCommand Command executed : " + sCmd);

        final String[] sCmdFinal = sCmd;
        final boolean destroyAfterDelayFinal = destroyAfterDelay;
        final long maxDelayInMsFinal = maxDelayInMs;

        try {
        String sResult = (String) AccessController.doPrivileged(new PrivilegedExceptionAction() {
            public Object run()throws Exception {
                Runtime run = Runtime.getRuntime();
                Process process = run.exec(sCmdFinal);
                String sCmdLine = sCmdFinal[0];
                for(int i=1; i<sCmdFinal.length; i++) {
                    sCmdLine += " " + sCmdFinal[i];
                }
                String sOutput = executeCommand(process, sCmdLine, maxDelayInMsFinal, destroyAfterDelayFinal);
                return sOutput;
            }
        });

        return sResult;
    }
        catch(PrivilegedActionException pae) {
            throw pae.getException();
        }
    }

    private static String executeCommand(Process process, String sCmd, long maxDelayInMs, boolean destroyAfterDelay) throws Exception {
        long start = (new Date()).getTime();
        int iExitValue = -1;
        boolean bFinishedRegulary = false;
        while (((new Date()).getTime() - start) < maxDelayInMs) {
            try {
                iExitValue = process.exitValue();
                bFinishedRegulary = true;
                DebugUtil.debug(DebugUtil.INFO, CLASSNAME, "/executeCommand : Process is finished ("+iExitValue+").");
                break;
            }
            catch (IllegalThreadStateException e) {
                // Process is not ended or can not be trapped
                // => Wait 200 ms and loop
                // NB: JDK 1.1.5 (standard in Netscape cannot reach external process)
                //     JDK 1.3   looks OK
                bFinishedRegulary = true;
				Thread.sleep(200);
				break;				
            }
        }
        if (!bFinishedRegulary) {
            DebugUtil.debug(DebugUtil.ERROR, CLASSNAME, "/executeCommand : Process has took longer than " + maxDelayInMs + ". Timeout reached.");        	
        }
        if (iExitValue < 0) {
            DebugUtil.debug(DebugUtil.ERROR, CLASSNAME, "/executeCommand : Internal JVM is not able to determine if process is finished.");
        }

        OutputStream out    = process.getOutputStream();
        InputStream in      = process.getInputStream();
        InputStream err     = process.getErrorStream();

        BufferedReader outputReader
                = new BufferedReader( new InputStreamReader(in));
        BufferedReader errorReader
                = new BufferedReader( new InputStreamReader(err));

        String sOutput      = "";
        String sError       = "";

        while (outputReader.ready()) {
            sOutput += outputReader.readLine() + "\n";
        }
        sOutput = sOutput.trim();

        while (errorReader.ready()) {
            sError += errorReader.readLine() + "\n";
        }
        sError = sError.trim();

        errorReader.close();
        outputReader.close();
        out.close();
        in.close();
        err.close();

        sOutput = sOutput.trim();
        sError = sError.trim();

        DebugUtil.debug(DebugUtil.INFO, CLASSNAME, "/executeCommand: standard output stream returned : " + sOutput);
        DebugUtil.debug(DebugUtil.INFO, CLASSNAME, "/executeCommand: error output stream returned    : " + sError);

        if (destroyAfterDelay || iExitValue >= 0) {
        	DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/executeCommand: Destroying process...");
            process.destroy();
        }

        if (!"".equals(sError)) {
            throw new Exception ("Command \'" + sCmd + "\' has launched following error : \n" + sError);
        }

        return sOutput;
    }



    /**
     * This method execute an external command and take account of the bad behaviour
     * of class java.lang.UnixProcess (no control is possible on JDK 1.1.5)
     *
     * The workaround which is taken is to wait for a key return value (OK or ERROR:)
     * This key return value tell us that the process is finished and if it succeeded or failed.
     * In case of failure, the complete ERROR: line is send as an Exception
     *
     * @param sCmd         : Command line to execute
     * @param timeOutInMs  : Max delay before exiting with an error
     * @return             : The std output of the script
     * @throws IOException : if the script returns an error in the error output stream
     *         Exception   : if the script returns "ERROR..." in the std output stream
     *
     */
    public static String executeCommandAndWaitReturn(String sCmd, long timeOutInMs) throws Exception {
        DebugUtil.debug(DebugUtil.INFO, CLASSNAME, "/executeCommandAndWaitReturn EXECUTION OF : " + sCmd + "  -- timeout is fixed to " + timeOutInMs + " ms.");
        // Keys expected for the end of the process
        String sLabelOK         = "OK";
        String sLabelERROR      = "ERROR:";

        // ------------------------------------------------
        // LAUNCH PROCESS
        // ------------------------------------------------
        Runtime run = Runtime.getRuntime();
        Process process = run.exec(sCmd);

        // ------------------------------------------------
        // RETRIEVE STREAMS FOR COMMUNICATION
        // ------------------------------------------------
        OutputStream out    = process.getOutputStream();
        InputStream in      = process.getInputStream();
        InputStream err     = process.getErrorStream();

        BufferedReader outputReader
                = new BufferedReader( new InputStreamReader(in));
        BufferedReader errorReader
                = new BufferedReader( new InputStreamReader(err));

        String sOutput          = "";
        String sError           = "";
        String sErrorFromOutput = "";

        long start = (new Date()).getTime();
        int iExitValue = -1;

        while (iExitValue == -1) {
            // ------------------------------------------------
            // TRY TO KNOW IF PROCESS IS TERMINATED
            // exitValue return a correct value only when :
            //             - process is terminated
            //     AND     - JVM > 1.2
            // ------------------------------------------------
            try {
                iExitValue = process.exitValue();
                DebugUtil.debug(DebugUtil.INFO, CLASSNAME, "/executeCommandAndWaitReturn: Process is finished ("+iExitValue+").");
                break;
            }
            catch (IllegalThreadStateException e) {
                // Process is not ended or can not be trapped
                // => Wait 200 ms and loop
                // NB: JDK 1.1.5 (standard in Netscape cannot reach external process)
                //     JDK 1.3   looks OK
                Thread.sleep(200);
            }

            // ------------------------------------------------
            // READ OUTPUT STREAM
            // If a line begins with "OK" then process is successfully terminated
            // If a line begins with "ERROR:" then process is failed
            // ------------------------------------------------
            while (outputReader.ready()) {
                String sLine = outputReader.readLine();
                DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/executeCommandAndWaitReturn: SCRIPT OUTPUT " + sLine);
                sOutput += sLine + "\n";

                if (sLine.startsWith(sLabelOK) || sLine.startsWith(sLabelERROR) ) {
                    iExitValue = 100;
                    if (sLine.startsWith(sLabelERROR) ) {
                        sErrorFromOutput = sLine;
                    }
                }
            }

            // ------------------------------------------------
            // READ ERROR OUTPUT STREAM
            // ------------------------------------------------
            while (errorReader.ready()) {
                String sLine = errorReader.readLine();
                DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/executeCommandAndWaitReturn: SCRIPT ERROR OUTPUT " + sLine);
                sError += sLine + "\n";
            }

            // ------------------------------------------------
            // CHECK TIMEOUT IS NOT OVERFLOWED
            // ------------------------------------------------
            if (((new Date()).getTime() - start) > timeOutInMs) {
                sError += "Timeout has been reached.\n";
                DebugUtil.debug(DebugUtil.ERROR, CLASSNAME, "/executeCommandAndWaitReturn: ======  TIME OUT  ======");
                break;
            }

        }

        if (iExitValue < 0 || iExitValue == 100) {
        	DebugUtil.debug(DebugUtil.INFO, CLASSNAME, "/executeCommandAndWaitReturn: Internal JVM is not able to determine if process is finished.");

        }

        // ------------------------------------------------
        // READ END OF THE OUTPUT STREAM
        // ------------------------------------------------
        while (outputReader.ready()) {
            String sLine = outputReader.readLine();
            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/executeCommandAndWaitReturn: SCRIPT OUTPUT " + sLine);
            sOutput += sLine + "\n";
            if (sLine.startsWith(sLabelERROR) ) {
                sErrorFromOutput = sLine.substring(sLabelERROR.length());
            }
        }
        sOutput = sOutput.trim();

        // ------------------------------------------------
        // READ END OF THE ERROR OUTPUT STREAM
        // ------------------------------------------------
        while (errorReader.ready()) {
            sError += errorReader.readLine() + "\n";
        }
        sError = sError.trim();

        // ------------------------------------------------
        // CLOSE ALL STREAMS AND DESTROY PROCESS
        // ------------------------------------------------
        errorReader.close();
        outputReader.close();
        out.close();
        in.close();
        err.close();

        if (iExitValue >= 0) {
        	 DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/executeCommandAndWaitReturn: Destroying process...");
            process.destroy();
        }

        // ------------------------------------------------
        // LAUNCH EXCEPTION IF ERROR RETURNED BY SCRIPT
        // ------------------------------------------------
        if (!"".equals(sErrorFromOutput)) {
            throw new Exception (sErrorFromOutput);
        }

        // ------------------------------------------------
        // LAUNCH IO-EXCEPTION IF SCRIPT HAS FAILED
        // ------------------------------------------------
        if (!"".equals(sError)) {
            throw new IOException ("Command \'" + sCmd + "\' has launched following error : \n" + sError);
        }

        return sOutput;
    }

    // WARNING : This method may not work
    //    eg: the class "netscape.security.UserDialogHelper" may have been installed on some PC-plugins
    public static boolean isNetscape() {
        boolean isNetscape = false;
        try {
            Class.forName("netscape.security.UserDialogHelper");
            isNetscape = true;
        }
        catch(ClassNotFoundException classnotfoundexception) { }
        catch(NoClassDefFoundError noclassdeffounderror) { }
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/isNetscape : " + isNetscape);
        return isNetscape;
    }

    public static boolean deleteDirectory(File path) throws Exception {
        String s_files[] = path.list();
        File file = null;
        boolean b_deleted = true;
        for(int i = 0; i < s_files.length; i++) {
            file = new File(path.getAbsolutePath() + "/" + s_files[i]);
            if(file.isDirectory()) {
                b_deleted = b_deleted && deleteDirectory(file);
            } else {
                b_deleted = b_deleted && file.delete();
            }
        }
        b_deleted = b_deleted && path.delete();
        return b_deleted;
    }
    
    /**
     * this is a legacy method to provide the old (deprecated) method to retrive the view command
     * here the timeout is fixed to 1000 ms. It should not be used any more since the time out is 
     * hard coded and has prooved to be too short for some older client machines.
     * @return
     * @throws Exception
     * @deprecated
     */
    public static String getViewCommand() throws Exception  {
    	// call with fixed 30000 ms timeout.
    	return getViewCommand(30000);
    }

    //  FPDM MODIFY Start JFA 09/06/2006 RFC 4625, RFC 4326

    /**
     * returns the absolute path the viewer for a given file type.
     * The filetype is given in the system property "faurecia.file.extension".
     * @param iDuration: Timeout in ms to get a response from the underlying OS which viewer to use. 1000ms was the standard, bus is too short for older hardware.
     * @throws Exception
     */
    public static String getViewCommand(int iDuration) throws Exception {
    	boolean isWin = false;
        String sMapping = "";
        String sCommandToExecute = "";
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/getViewCommand");
        // ====================================================================
        // Retrieve application which is used for viewing plt
        try {
            //FPDM ADD Start JFA 28/08/2006 RFC 2528
            String strFileExtension = System.getProperty("faurecia.file.extension");
            if (strFileExtension.length() != 0){
                FILE_EXTENSION = strFileExtension;
            }
            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/getViewCommand --> FILE_EXTENSION : " + FILE_EXTENSION);
            //FPDM ADD End JFA 28/08/2006 RFC 2528
            String sCmd1 = "cmd /c  assoc " + FILE_EXTENSION ;
            String line = executeCommand(sCmd1, iDuration, true);
            if (line != null) {
                sMapping = line.trim();
            }
        }
        catch (Exception e) {
            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/getViewCommand : Exception catched :" + e.toString());
        }

        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/getViewCommand : sMapping " + sMapping);

        //FPDM ADD Start JFA 28/08/2006 RFC 2528
        if (sMapping.indexOf("=") == -1){
            throw new Exception("No viewer available or timed out. See java console for details.");
        }
        //FPDM ADD End JFA 28/08/2006 RFC 2528

        // ====================================================================
        // Retrieve execution command for this application
        int index = sMapping.lastIndexOf(FILE_EXTENSION);
        if (index >= 0) {
            sMapping = sMapping.substring(FILE_EXTENSION.length() + 1);
            String sCmd2 = "cmd /c ftype ".concat(sMapping) ;

            try {
                String line = executeCommand(sCmd2, iDuration, true);
                if (line != null) {
                    sCommandToExecute = line.trim();
                }
            }
            catch (Exception e) {
                DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/getViewCommand : Exception catched " + e.toString());
            }

            index = sCommandToExecute.lastIndexOf(sMapping);

            if (index >= 0) {

                sCommandToExecute = sCommandToExecute.substring(sMapping.length()+1);
                DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/getViewCommand : Potential command to execute: " + sCommandToExecute);

                String type = System.getProperty("os.name");
                if(type.indexOf("Windows")>=0) {
                	isWin = true;
                }
                sCommandToExecute = getCommandToExecute(isWin, sCommandToExecute);
            }
            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "Execution command for this application: " + sCommandToExecute);
        }
		
        return sCommandToExecute;
    }
    //  FPDM MODIFY End JFA 09/06/2006 RFC 4625, RFC 4326

    // --------------------------------------------------------------------
    // Retrieve local HOME directory
    // --------------------------------------------------------------------
    public static String getLocalHomeDirectory(String sHomeVarFilePath)
        throws Exception
    {
        String sHomeDirectoryPath = Local_OS_Information.getUnixEnv("HOME");
	    DebugUtil.debug(DebugUtil.INFO, CLASSNAME, "/getLocalHomeDirectory: OS variable HOME is " + sHomeDirectoryPath);

        if (sHomeDirectoryPath == null || "".equals(sHomeDirectoryPath)) {
	        DebugUtil.debug(DebugUtil.INFO, CLASSNAME, "/getLocalHomeDirectory: Looking in file \"" + sHomeVarFilePath + "\" for HOME_DIRECTORY.");
            try {
                File home_file = new File(sHomeVarFilePath);
                FileReader fileStr = new FileReader(home_file);
                BufferedReader fileBuf = new BufferedReader(fileStr);
                sHomeDirectoryPath = fileBuf.readLine();
                fileBuf.close();
	            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/getLocalHomeDirectory: HOME = " + sHomeDirectoryPath);
            }
            catch (Exception e) {
	            String sErrorMessage = "/getLocalHomeDirectory: HOME can not be resolved : " + e.getMessage();
                DebugUtil.debug(DebugUtil.WARNING, sErrorMessage);
                throw new Exception (sErrorMessage);
            }
        }
        return sHomeDirectoryPath;
    }

    public static String[] retrieveFilesList(String repertory)
            throws Exception
    {
        String [] Chemin = null;
        File repertoire = new File(repertory);
        if(repertoire.exists() && repertoire.isDirectory()) {
            Chemin = repertoire.list();
        } else {
            throw new Exception("Local temp directory '" + repertoire.getAbsolutePath() + "' is empty.");
        }
        return Chemin;
    }


    /**
     * NOT TESTED ON UNIX (as not used in this platform)
     * @
     * @return true if application has been opened.
     *
     * @throws Exception
     */
    public static boolean openApplication(String sLocalDirectory, String sFileName)
        throws Exception
    {
        if(System.getProperty("os.name").indexOf("Windows")>=0) {
            boolean bApplicationOpened = true;
	        DebugUtil.debug (DebugUtil.DEBUG, CLASSNAME, "/openApplication : START");
            String sFilePath = (new File (sLocalDirectory, sFileName)).getAbsolutePath();

            try {
                executeCommand (new String[]{"cmd", "/c", sFilePath}, 1000, false);
            }
            catch (Exception e ) {
	            DebugUtil.debug (DebugUtil.ERROR, CLASSNAME, "/openApplication : Failed Opening Application " + e.toString() );
                bApplicationOpened = false;
            }
	        DebugUtil.debug (DebugUtil.DEBUG, CLASSNAME, "/openApplication : END");

            return bApplicationOpened;
        }
        else {
            return true;
        }
    }



    public static File Compressfile(File fileToCompress, boolean compress) throws Exception {
        String type = System.getProperty("os.name");
        if(type.indexOf("Windows")<0) {

        String sCmd = "compressefile";
          sCmd += (compress ? " c " : " d ") ;
          sCmd += fileToCompress.getAbsolutePath();

        executeCommandAndWaitReturn(sCmd, 600000);

        File returnedFile;
        if (compress) {
            returnedFile = new File(fileToCompress.getAbsolutePath() + ".Z");
        } else {
            String sTempFileName = fileToCompress.getAbsolutePath();
            returnedFile = new File(sTempFileName.substring(0, sTempFileName.lastIndexOf(".Z")));
        }
        if (!returnedFile.exists()) {
        	 DebugUtil.debug(DebugUtil.ERROR, CLASSNAME, "/Compressfile : resulted file does not exists.");
            throw new Exception ("resulted file does not exists.");
        }

        return returnedFile;
    }
        else {
            return fileToCompress;
        }
    }

    public static File DirectUncompressFile(File fileToUncompress)
        throws Exception
    {
        String sFileName = fileToUncompress.getAbsolutePath();
        File fileUncompressed = fileToUncompress;
        if (sFileName.endsWith(".Z") && System.getProperty("os.name").indexOf("Windows")<0) {
            String sCmd = "compress -df " + sFileName;
            String line = executeCommand(sCmd, 60000000, true);
            DebugUtil.debug (DebugUtil.DEBUG, CLASSNAME, "/DirectUncompressFile: Script return " + line);
            fileUncompressed = new File (sFileName.substring(0, sFileName.length()-2));
        }
        return fileUncompressed;
    }

    public static String buildPrintCommand(String commandBeginning, String filePath, String nbcopy, String scale, String color, String formatdrawing, boolean bTitleBlockOnly) {
        String result = commandBeginning;
        result += " -f " + filePath;
        result += " -n " + nbcopy;
        result += " -e " + scale;
        result += " -c " + color;
        if(!bTitleBlockOnly) {
        	result += " -t " + formatdrawing;
		}
        return result;

    }



}
