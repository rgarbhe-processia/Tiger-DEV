package faurecia.applet.jt;
import java.io.IOException;
import java.net.URL;
import java.util.StringTokenizer;
   

import netscape.security.PrivilegeManager;

import com.ms.security.PermissionID;
import com.ms.security.PolicyEngine;     
      

import faurecia.applet.FaureciaApplet;
import faurecia.applet.efcs.Checkouter;
import faurecia.util.AbstractFile;
import faurecia.util.Local_OS_Information;
import faurecia.util.StringUtil;

/**
 * Used for view jt/plmxml files
 * It allows to checkout jt files and launch default program associated to jt on the operating system
 * @author FAURECIA
 *
 */
public class AppletJTAssembly extends FaureciaApplet {
    private static final long serialVersionUID = 1L;

	private static final String PARAM_OBJECT_ID = "objectId";
	private static final String PARAM_LIST_ID_TO_CHECKOUT = "list3DViewableIds";
	private static final String PARAM_LIST_FILE_TO_CHECKOUT = "listViewableFiles";
	private static final String PARAM_LOCAL_TEMP_DIRECTORY = "localTempDir";
	private static final String PARAM_FILE_NAME = "fileName";
	private static final String PARAM_ERRORMSG = "errorMsg";
	
	
	private static final String FORMAT_3DVIEWABLE = "JT";
	private static final String TYPE_3DVIEWABLE = "3DViewable";
	private static final String FILE_EXTENSION = ".plmxml";
	
	
	
    /**
     * method start of the applet. This method will be called by the navigator after the init method.<br>
     * In this applet, the init method is not defined. Thus this method will be the first called.<br>
     *
     * First, this method set the privilege to be able to access to the local disc, <br>
     * then retrieve parameters, make some treatment to set them to null or to<br>
     * retrieve boolean value, and finally call the CheckFactory and the Check.doProcess();<br>
     *
     */
    public void init() {
        super.init();
    }

    /**
     * method start of the applet. This method will be called by the navigator after the init method.<br>
     * In this applet, the init method is not defined. Thus this method will be the first called.<br>
     *
     * First, this method set the privilege to be able to access to the local disc, <br>
     * then retrieve parameters, make some treatment to set them to null or to<br>
     * retrieve boolean value, and finally call the CheckFactory and the Check.doProcess();<br>
     *
     */
    public void start() {
    	// indicate in status bar that we are starting the work
        try {
	    	agGUI.startAction();
	        displayProgressMessage("Enable privileges ...");
	        System.out.println("AppletJTAssembly.startEnter.");
	
	        // --------------------------------------------------------------------
	        // Give to the applet all File Accesses in the local OS
	        if (Local_OS_Information.isNetscape()) {
	            System.out.println("AppletJTAssembly:enableAllPrivilegeFor netscape");
	            PrivilegeManager.enablePrivilege("UniversalPropertyRead");
	            PrivilegeManager.enablePrivilege("UniversalExecAccess");
	            PrivilegeManager.enablePrivilege("UniversalFileAccess");
	            PrivilegeManager.enablePrivilege("UniversalConnect");
	        } else {
	            System.out.println("AppletJTAssembly:enableAllPrivilegeFor IE");
	            PolicyEngine.assertPermission(PermissionID.FILEIO);
	            PolicyEngine.assertPermission(PermissionID.NETIO);
	            PolicyEngine.assertPermission(PermissionID.EXEC);
	        }
	
	        System.out.println("AppletJTAssembly.startAll privilege enabled.");
	        // --------------------------------------------------------------------
	        // Retrieve parameters
			String sObjectId                    = StringUtil.returnNullIfEmpty(this.getParameter(PARAM_OBJECT_ID));
			String s3DViewableIds           = StringUtil.returnDefaultIfEmpty(this.getParameter(PARAM_LIST_ID_TO_CHECKOUT),"");
			String s3DViewableFiles         = StringUtil.returnDefaultIfEmpty(this.getParameter(PARAM_LIST_FILE_TO_CHECKOUT),"");
	        String sLocalTempDirectory          = StringUtil.returnDefaultIfEmpty(this.getParameter(PARAM_LOCAL_TEMP_DIRECTORY), "c:/Temp");
	        //String sBomType						= StringUtil.returnDefaultIfEmpty(this.getParameter(PARAM_BOM_TYPE),"EBOM");
	        String sFileName				= StringUtil.returnDefaultIfEmpty(this.getParameter(PARAM_FILE_NAME),sObjectId);
	        String serrormsg				= StringUtil.returnDefaultIfEmpty(this.getParameter(PARAM_ERRORMSG),"");

        	displayProgressMessage("Get the command to view jt files from the operating system...");
			if(!serrormsg.equals(""))
			{
				displayProgressMessage(serrormsg);
			}
			
        	String sViewJtCommand=getViewCommand();
        	
        	System.out.println("AppletJTAssembly:startsObjectId " + sObjectId);
			System.out.println("AppletJTAssembly:startsObjectIds " + s3DViewableIds);
			System.out.println("AppletJTAssembly:startsObjectFiles " + s3DViewableFiles);
			System.out.println("AppletJTAssembly:startsViewJtCommand " + sViewJtCommand);
			//System.out.println("AppletJTAssembly:startsBomType " + sBomType);

            if (sObjectId == null) {
                throw new Exception ("Object Id has not been sent.");
            }
            
			StringTokenizer tokenIds = new StringTokenizer(s3DViewableIds, "|");
			StringTokenizer tokenFiles = new StringTokenizer(s3DViewableFiles, "|");
			// if some files are missing, we display a warning to the user, but we continue.
			// this may happen when e.g. some 3DViewable objects do not contain files.
			// Since the user sees this in the VisView (red X in the file tree), it is better 
			// to continue than to make a full stop. 
			// But here it is too late, since we do not have the information which files are missing. 
			// This inconsistency should be dealt within the page FPDMViewJTAssemblyByApplet.jsp line 56, where the file for each object is searched. 
            if (tokenIds.countTokens() != tokenFiles.countTokens()) {
    			System.out.println("AppletJTAssembly:startnumber of Ids is *NOT* equal to number of files to check out.");
                throw new Exception ("Number of selected Objects is NOT equal to number of files to check out.\n Some JT files are missing! Cannot proceed.");
    			// agGUI.displayResultMessageAndContinue("WARNING", "WARNING: On some of the selected objects the files are missing (no JT). Press Continue.", "WARNING");
            }

            // ========================================================
            // Retrieve XML file from server
            // ========================================================
            displayProgressMessage("Build plmxml file for VisView ...");

            URL url = new URL(this.getCodeBase()+"FPDM_JTAssemblyManagementPLMXML.jsp?objectId="+sObjectId);
			System.out.println("AppletJTAssembly:starturl "+url.toString());
            AbstractFile absXMLFile = new AbstractFile();
            absXMLFile.setContent(url.openStream());
            absXMLFile.createFileOnSystemDrive(sLocalTempDirectory, sFileName+FILE_EXTENSION);
            String sJtFileName = " \"" + sLocalTempDirectory + java.io.File.separatorChar + sFileName+FILE_EXTENSION  + "\"";
			System.out.println("AppletJTAssembly:startsCommandToExecute " + sViewJtCommand + sJtFileName);
            System.out.println("/AppletJTAssembly : b4 =======================================spilt " );
					//String[] exacturl = sViewJtCommand.split("=");
					//System.out.println("/AppletJTAssembly : after =======================================spilt "+exacturl );
					//sViewJtCommand = exacturl[1];
					//System.out.println("/AppletJTAssembly : after =======================================spilt "+sViewJtCommand );
            long longMaxMs = 1000;
			boolean bDestroy = false;

			while (tokenIds.hasMoreTokens()) {
                String sSelectedObjectId = tokenIds.nextToken();
                String sObjectFiles = tokenFiles.nextToken(); //case multiple JT files
                StringTokenizer tokenObjectFiles = new StringTokenizer(sObjectFiles, ";");
            
                while (tokenObjectFiles.hasMoreTokens()) {
	                String sSelectedObjectFile = tokenObjectFiles.nextToken();

            			System.out.println("AppletJTAssembly:startsSelectedObjectId "+sSelectedObjectId);
            			System.out.println("AppletJTAssembly:startsSelectedObjectFile "+sSelectedObjectFile);
            			System.out.println("AppletJTAssembly:MAIN_SERVER_BASE_URL "+MAIN_SERVER_BASE_URL);
        
            			displayProgressMessage("Checkout JT Files ..." + sSelectedObjectFile);
            			Checkouter.checkoutFile(sLocalTempDirectory,sSelectedObjectId,sSelectedObjectFile,FORMAT_3DVIEWABLE,false,"",TYPE_3DVIEWABLE, MAIN_SERVER_BASE_URL);
            		}
			}

			displayProgressMessage("Execution of VisView ...");
			try {
			    Local_OS_Information.executeCommand(sViewJtCommand + sJtFileName,longMaxMs,bDestroy);
			} catch (IOException e) {
			    if (!sViewJtCommand.startsWith("\"")) {
			        sViewJtCommand = "\"" + sViewJtCommand + "\"";
			        System.out.println("AppletJTAssembly:startsCommandToExecute " + sViewJtCommand + sJtFileName);
			        Local_OS_Information.executeCommand(sViewJtCommand + sJtFileName,longMaxMs,bDestroy);
			    }
				 this.closeAppletWindow();
			}

            // ========================================================
            // Close window and Refresh opener page
            // ========================================================
			bRefreshOpenerPage = false;
            this.closeAppletWindow();

        } catch (Throwable e) {
            handleError(e);
			  this.closeAppletWindow();
        } 
    }
    
    private String getViewCommand() throws Exception {
        String sOSName = System.getProperty("os.name").toUpperCase();
        if(sOSName.indexOf("WINDOWS")!= -1 ) {
        	System.setProperty("faurecia.file.extension",".jt");
        	System.out.println("System.getProperty(faurecia.file.extension) ");
			// since on some machines 1000 ms is too short, wait maximum 30000 ms.

            return Local_OS_Information.getViewCommand(30000);
        }
        else {
            return Local_OS_Information.getUnixEnv("FPDM_3DVIEWER");
        }
    }

}

