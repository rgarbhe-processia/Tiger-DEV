package faurecia.applet.cad;

import java.awt.Button;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import faurecia.applet.FaureciaApplet;
import faurecia.applet.awt.AppletWindowListener;
import faurecia.applet.awt.LstChooser;
import faurecia.applet.efcs.Checkouter;
import faurecia.util.AbstractFile;
import faurecia.util.AppletServletCommunication;
import faurecia.util.BusObject;
import faurecia.util.DebugUtil;
import faurecia.util.Local_OS_Information;
import faurecia.util.StringUtil;
import faurecia.util.cad.CADTitleBlock;
import faurecia.util.cad.HPGLMerger;
import fpdm.applet.cad.Parameters;

/**
 * 
 * Applet that should be able to make checkin and checkout by <br>
 * discussing with a servlet. The original servlet was StServletCheckInOut.<br>
 * <br>
 * This class just retrieve the parameter given to the applet, <br>
 * and call CheckFactory to have an abstract object Check whose type will depend<br>
 * on the value of the parameters.<br>
 * <br>
 * Then it call the abstract method doProcess that will do a checkin or a checkout.<br>
 * <br>
 * 
 * @author rinero
 */
public class AppletCheckInOut extends FaureciaApplet {

    // FDPM ADD Start JFA 30/01/05 Message Log management
    private final static String CLASSNAME = "AppletCheckInOut";

    // FDPM ADD End JFA 30/01/05 Message Log management

    public static String sPrinterSelected = "";

    public static String[] sFiletoCheckin;

    public static String listcheckinout = "";

    public static String scheckin2D = "";

    public static String MCS_SERVLET_URL = "";

    public static String sObjectId;

    private String sCadObjectsTNR;

    private String sDownloadTifCommand;

    private String sLocalTempDirectory;

    private String sServerTempDirectory;

    private String sFormat;

    private String relPat;

    private String recLevel;

    private String typeList;

    private String commande_sysgen;

    private String cad_format;

    private String cad_environment;

    private String version_system_generateur;

    private String system_generateur;
    private String sListCADSoftwares;

    private String sTypeOri;

    private String sCurrentOri;

    private boolean bTitleBlockOnly;

    private boolean bPreIntegration;

    private ArrayList<Boolean> bListHPGL2 = new ArrayList<Boolean>();

    private String listobjid;

    // Especially for checkout
    private String sScript;

    private String sListScript;

    private String pMSBGCheckOut;

    private String sCheckoutdir;

    // Especially for printing with Title Block
    private String sLanguage;

    private String listOfFiles;

    private String sHistoricLevel;

    private String sBonProd;
    private String sNissanOrRenault;
    private String sNbOfFolios;

    private String sAdditionalInfo;

    private String sColor;

    private String sScale;

    private String sListHPGLProperty;

    // Format the information passed in parameter
    // Especially for checkin
    private String sParameterFile;

    private boolean allcheckin;

    private boolean bAppend;

    private boolean bLock;

    private String sNbCopy;

    // FPDM Add Start - CRO 20050729 - CAD : TIFF banner - ESBG
    // Especially for checkout for ESBG
    private String sUserName;

    private String sCurrentState;

    private String sDate;

    // FPDM Add Start - JFA 20051223
    // Especially for printer selection
    public static String preferedPrinters = "";

    // FPDM Add End - JFA 20051223

    private static String TIF_EXTENSION = ".tif";

    private static String PLT_EXTENSION = ".plt";

    private String debugMode;

    // Especially for creating a collection with the checkined CAD Definition
    private String sCollectionName;

    private String sCollectionMessage;
    private String sOrganizationName;

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
        MCS_SERVLET_URL = MAIN_SERVER_BASE_URL + FAURECIA_eMCS_SERVLET_NAME;
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
    @SuppressWarnings("unchecked")
    public synchronized void start() {
        // indicate to user that we start the action.
        agGUI.startAction();
        AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {

                DebugUtil.debug(DebugUtil.INFO, CLASSNAME, "/start : Start Applet");

                Parameters.getAppletUI().setApplet(AppletCheckInOut.this);

                String inOutStr = AppletCheckInOut.this.getParameter("inOut");
                sObjectId = AppletCheckInOut.this.getParameter(Parameters.OBJECT_ID);
                sCadObjectsTNR = AppletCheckInOut.this.getParameter(Parameters.CAD_OBJECTS_TNR);
                sDownloadTifCommand = StringUtil.returnNullIfEmpty(AppletCheckInOut.this.getParameter(Parameters.DOWNLOAD_TIF_COMMAND));
                sLocalTempDirectory = StringUtil.returnNullIfEmpty(AppletCheckInOut.this.getParameter(Parameters.LOCAL_REPERTORY));
                sServerTempDirectory = StringUtil.returnNullIfEmpty(AppletCheckInOut.this.getParameter(Parameters.SERVER_REPERTORY));
                sFormat = StringUtil.returnNullIfEmpty(AppletCheckInOut.this.getParameter(Parameters.FORMAT));
                relPat = StringUtil.returnDefaultIfEmpty(AppletCheckInOut.this.getParameter(Parameters.REL_PAT), "");
                recLevel = StringUtil.returnNullIfEmpty(AppletCheckInOut.this.getParameter(Parameters.REC_LEVEL));
                typeList = StringUtil.returnNullIfEmpty(AppletCheckInOut.this.getParameter(Parameters.TYPE_LIST));
                commande_sysgen = AppletCheckInOut.this.getParameter(Parameters.COMMANDE_SYSGEN);
                cad_format = AppletCheckInOut.this.getParameter(Parameters.CAD_FORMAT);
                cad_environment = AppletCheckInOut.this.getParameter(Parameters.CAD_ENVIRONMENT);
                version_system_generateur = AppletCheckInOut.this.getParameter(Parameters.VERSION_SYSTEM_GENERATEUR);
                system_generateur = AppletCheckInOut.this.getParameter(Parameters.SYSTEM_GENERATEUR);
                sListCADSoftwares = AppletCheckInOut.this.getParameter(Parameters.LIST_CAD_SOFTWARE);
                sTypeOri = AppletCheckInOut.this.getParameter(Parameters.STYPEORI);
                sCurrentOri = AppletCheckInOut.this.getParameter(Parameters.SCURRENTORI);
                bTitleBlockOnly = "TRUE".equalsIgnoreCase(AppletCheckInOut.this.getParameter(Parameters.TITLE_BLOCK_ONLY));
                bPreIntegration = "YES".equalsIgnoreCase(AppletCheckInOut.this.getParameter(Parameters.PRE_INTEGRATION));
                sOrganizationName = AppletCheckInOut.this.getParameter(Parameters.ORGANIZATION_NAME);

                listobjid = StringUtil.returnNullIfEmpty(AppletCheckInOut.this.getParameter("listobjid"));

                // FPDM Add Start - JFA 2005/12/23 RFC 1731
                // Especially for printer selection
                preferedPrinters = AppletCheckInOut.this.getParameter(Parameters.PREFERED_PRINTER);
                // FPDM Add End - JFA 2005/12/23

                // FPDM Add Start - JFA 2005/01/16 Enhancement
                debugMode = AppletCheckInOut.this.getParameter(Parameters.DEBUG_LEVEL);

                // FPDM Add Start - JFA 2005/12/23 RFC 3783
                sListHPGLProperty = AppletCheckInOut.this.getParameter(Parameters.LISTHPGLPROPERTY);
                // FPDM Add Start - JFA 2005/12/23 RFC 3783

                if (debugMode.equals("ON")) {
                    DebugUtil.initDebugger(true);
                    System.setProperty("faurecia.debugging", "ON");
                } else {
                    DebugUtil.initDebugger(false);
                    System.setProperty("faurecia.debugging", "OFF");
                }

                // FPDM Add End - JFA 2005/01/16

                // Especially for checkout
                sScript = StringUtil.returnNullIfEmpty(AppletCheckInOut.this.getParameter(Parameters.SCRIPT));
                sListScript = StringUtil.returnNullIfEmpty(AppletCheckInOut.this.getParameter(Parameters.LIST_SCRIPT));
                pMSBGCheckOut = StringUtil.returnNullIfEmpty(AppletCheckInOut.this.getParameter(Parameters.MSBG_CHECKOUT));
                sCheckoutdir = StringUtil.returnNullIfEmpty(AppletCheckInOut.this.getParameter(Parameters.CHECKOUTDIR));
                scheckin2D = StringUtil.returnNullIfEmpty(AppletCheckInOut.this.getParameter(Parameters.CHECKIN2D));
                // Especially for printing with Title Block
                sLanguage = StringUtil.returnDefaultIfEmpty(AppletCheckInOut.this.getParameter(Parameters.LANGUAGE), "FR");
                
                listOfFiles = AppletCheckInOut.this.getParameter(Parameters.LISTOFFILES);
                sHistoricLevel = AppletCheckInOut.this.getParameter(Parameters.HISTORIC);
                sBonProd = AppletCheckInOut.this.getParameter(Parameters.BON_PROD);
                sNissanOrRenault             = AppletCheckInOut.this.getParameter(Parameters.NISSAN_RENAULT);
                sNbOfFolios = AppletCheckInOut.this.getParameter(Parameters.NB_FOLIOS);
                sAdditionalInfo = AppletCheckInOut.this.getParameter(Parameters.INFOS);
                sColor = AppletCheckInOut.this.getParameter(Parameters.COLOR);
                sScale = AppletCheckInOut.this.getParameter(Parameters.SCALE);

                // Format the information passed in parameter
                // Especially for checkin
                sParameterFile = StringUtil.returnNullIfEmpty(AppletCheckInOut.this.getParameter(Parameters.PARAMETER_FILE));
                allcheckin = "all".equalsIgnoreCase(sParameterFile);
                bAppend = !"false".equalsIgnoreCase(AppletCheckInOut.this.getParameter(Parameters.APPEND));
                bLock = "true".equalsIgnoreCase(AppletCheckInOut.this.getParameter(Parameters.LOCK));
                sNbCopy = AppletCheckInOut.this.getParameter(Parameters.NB_COPY);

                // FPDM Add Start - CRO 20050729 - CAD : TIFF banner - ESBG
                sUserName = AppletCheckInOut.this.getParameter(Parameters.USER_NAME);
                sCurrentState = AppletCheckInOut.this.getParameter(Parameters.CURRENT_CADDEF);
                sDate = AppletCheckInOut.this.getParameter(Parameters.DATE);
                // FPDM Add End - CRO 20050729

                if (sListCADSoftwares != null && sListCADSoftwares.length() > 0) {
                    String[] aListCADSoftwares = StringUtil.split(sListCADSoftwares, Parameters.DELIM);
                    for (String sCADSoftware : aListCADSoftwares) {
                        if (sCADSoftware != null && !"null".equals(sCADSoftware) && (sCADSoftware.indexOf("V5") != -1 || sCADSoftware.indexOf("UG") != -1)) {
                            bListHPGL2.add(true);
                        } else {
                            bListHPGL2.add(false);
                        }                    }
                } else if (system_generateur != null && !"null".equals(system_generateur) && (system_generateur.indexOf("V5") != -1 || system_generateur.indexOf("UG") != -1)) {
                    bListHPGL2.add(true);
                } else {
                    bListHPGL2.add(false);
                }

                if ("yes".equalsIgnoreCase(sColor)) {
                    sColor = "o";
                }
                if ("no".equalsIgnoreCase(sColor)) {
                    sColor = "n";
                }

                // Manage the creation of the collection at the end
                sCollectionName = AppletCheckInOut.this.getParameter(Parameters.COLLECTION_NAME);
                sCollectionMessage = AppletCheckInOut.this.getParameter(Parameters.COLLECTION_MESSAGE);

                try {
                    DebugUtil.debug(DebugUtil.INFO, CLASSNAME, "/start : Call method: " + inOutStr);
                    // --------------------------------------------------------------------
                    // Dispatch treatment according inOutStr parameter
                    // --------------------------------------------------------------------
                    if (!Parameters.POSSIBLE_VALUES_OF_INOUT_PARAMETER.contains(inOutStr)) {
                        throw new Exception("Parameter inOut is not correct!");
                    } else if ("checkin".equalsIgnoreCase(inOutStr)) {
                        checkin();
                    } else if ("checkout".equalsIgnoreCase(inOutStr)) {
                        checkout();
                    } else if ("viewAsTIF".equalsIgnoreCase(inOutStr)) {
                        viewAsTIF(false);
                    } else if ("viewAsTIFWindows".equalsIgnoreCase(inOutStr)) {
                        viewAsTIF(true);
                    } else if ("print".equalsIgnoreCase(inOutStr)) {
                        print(false, true);
                    } else if ("view".equalsIgnoreCase(inOutStr)) {
                        print(false, false);
                    } else if ("printWindows".equalsIgnoreCase(inOutStr)) {
                        printWindows();
                    } else if ("viewWindows".equalsIgnoreCase(inOutStr)) {
                        print(true, false);
                    }
                    // FPDM Add Start - CRO 20050729 - CAD : TIFF banner - ESBG
                    else if ("viewTIFFWithFileWindows".equalsIgnoreCase(inOutStr)) {
                        viewTIFWithFile(true, false);
                    }
                    // FPDM Add End - CRO 20050729
                    else if ("viewTIFFWithFile".equalsIgnoreCase(inOutStr)) {
                        viewTIFWithFile(false, false);
                    }
                    // FPDM ADD Start : PRI 12/01/2006 RFC 2034
                    else if ("checkoutMultipleTIFF".equalsIgnoreCase(inOutStr)) {
                        checkoutMultipleTIFF(false);
                    } else if ("checkoutMultipleTIFFWindows".equalsIgnoreCase(inOutStr)) {
                        checkoutMultipleTIFF(true);
                    } else if ("downloadMultipleTIFF".equalsIgnoreCase(inOutStr)) {
                        downloadMultipleTIFF(false);
                    } else if ("downloadMultipleTIFFWindows".equalsIgnoreCase(inOutStr)) {
                        downloadMultipleTIFF(true);
                    }
                    // FPDM ADD End: PRI 12/01/2006 RFC 2034
                    else {
                        throw new Exception("No action parameter.");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    handleError(e);
                }
                return null;
            }

        });
    }

    private void viewTIFWithFile(boolean isWindows, boolean doPrint) throws Exception {
        displayProgressMessage("Retrieving local temporary directory ...");

        // --------------------------------------------------------------------
        // Retrieve local temp directory
        // --------------------------------------------------------------------

        sLocalTempDirectory = getPrintTempDirectory(isWindows);

        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/viewTIFWithFile : START");

        // ============================
        // Retrieve Print command line
        String sPrinterCmd = "";
        if (!isWindows && doPrint) {
            displayProgressMessage("Retrieve list of printers ...");
            sPrinterCmd = Printer.selectLocalPrinterAndBuildCommand();
            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/viewTIFWithFile: Printer command: " + sPrinterCmd);
        }

        // ============================
        // Title Block Construction

        // ----------------------------------------------------
        // Split inputted parameters
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/viewTIFWithFile: sObjectId " + sObjectId);
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/viewTIFWithFile: listOfFiles " + listOfFiles);
        String[] sCADDefIds = StringUtil.split(sObjectId, Parameters.DELIM);
        String[] ListOfFiles = StringUtil.split(listOfFiles, Parameters.DELIM);
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/viewTIFWithFile: ListOfFiles : ", ListOfFiles);

        String[] aListCurrentState = StringUtil.split(sCurrentState, Parameters.DELIM);
        // FPDM Add Start : JFA 2005/07/26 RFC 3783
        boolean bListPropertyExist = false;
        StringTokenizer tokenHPGL = null;

        if (sListHPGLProperty != null && !"".equals(sListHPGLProperty)) {
            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/viewTIFWithFile: sListHPGLProperty : " + sListHPGLProperty);
            tokenHPGL = new StringTokenizer(sListHPGLProperty, "|");
            bListPropertyExist = true;
        }
        String sHPGL = "";
        String sCurrentState = "";
        // FPDM Add Start : JFA 2005/07/26 RFC 3783

        for (int iCpt = 0; iCpt < (ListOfFiles.length / 5); iCpt++) {
            String sViewId = ListOfFiles[(5 * iCpt)];
            sFormat = ListOfFiles[(5 * iCpt) + 1];
            String sFileName = ListOfFiles[(5 * iCpt) + 2];
            String sPaperFormat = ListOfFiles[(5 * iCpt) + 3];
            // String sViewName = ListOfFiles[(5 * iCpt) + 4];

            // FPDM Add Start : JFA 2005/07/26 RFC 3783
            if (bListPropertyExist) {
                sHPGL = tokenHPGL.nextToken();
                DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/print: sHPGL : " + sHPGL);
            }
            // FPDM Add End : JFA 2005/07/26 RFC 3783
            // ----------------------------------------------------
            // Retrieve title blocks from servlet
            CADTitleBlock cadTitleBlock = buildTitleBlock(sCADDefIds, ListOfFiles, iCpt);
            if (aListCurrentState != null && aListCurrentState.length > iCpt) {
                sCurrentState = aListCurrentState[iCpt];
            } else {
                sCurrentState = aListCurrentState[0];
            }
            printTiffWithFile(isWindows, doPrint, sPrinterCmd, sViewId, sFormat, sFileName, sPaperFormat, cadTitleBlock, sHPGL, sCurrentState);

            // FPDM Add Start : JFA 2005/07/26 RFC 3783
            sHPGL = "";
            // FPDM Add End : JFA 2005/07/26 RFC 3783
        }
        if (doPrint) {
            // new DisplayMessage("Information", "Plot has been printed.", "Information", "OK");
            agGUI.displayResultMessageAndContinue("Information", "Plot has been printed.", "OK");
        }
        this.closeAppletWindow();
    }

    private void printTiffWithFile(boolean isWindows, boolean doPrint, String sPrinterCmd, String sViewId, String sFormat, String sFileName, String sPaperFormat, CADTitleBlock cadTitleBlock,
            String sHpgl, String sCADDefState) throws Exception {
        // --------------------------
        // CREATE TITLE BLOCK FILES ON LOCAL TEMP DIRECTORY
        File titleBlockFile = null;

        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/printTiffWithFile: sScript  " + sScript);
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/printTiffWithFile: bTitleBlockOnly  " + bTitleBlockOnly);

        if (!"Nouse".equalsIgnoreCase(sScript) && !"NoTitleBlock".equalsIgnoreCase(sScript)) {
            cadTitleBlock.createTitleBlockFile(sLocalTempDirectory, sFileName + "Car.plt");
            titleBlockFile = new File(sLocalTempDirectory, sFileName + "Car.plt");
        }

        // --------------------------
        // DO THE CHECKOUT OF FILES
        File checkedOutFile = null;

        if (!bTitleBlockOnly) {
            displayProgressMessage("Checkout file ..." + sFileName);

            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/printTiffWithFile: retrieving file  " + sFileName);
            bLock = false;
            checkedOutFile = Checkouter.checkoutTIFWithFile(sLocalTempDirectory, sViewId, sFileName, sFormat, bLock, sObjectId, "2DViewable", MAIN_SERVER_BASE_URL, sUserName, sCADDefState, sDate);

            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/printTiffWithFile: Exit of checkoutFiles " + sFileName);
            displayProgressMessage("Done.");
            // ============================
        }

        // FPDM ADD Start JFA 28/08/2006 RFC 2528
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/print: checkedOutFile " + checkedOutFile);
        if (checkedOutFile != null) {
            if (checkedOutFile.getAbsolutePath().indexOf(TIF_EXTENSION) == -1) {
                System.setProperty("faurecia.file.extension", PLT_EXTENSION);
            } else {
                System.setProperty("faurecia.file.extension", TIF_EXTENSION);
            }
        } else {
            System.setProperty("faurecia.file.extension", TIF_EXTENSION);
        }
        // FPDM ADD End JFA 28/08/2006 RFC 2528

        // Retrieve Command Line for printing
        String sPrinterCmdComplete = null;
        if ("Nouse".equalsIgnoreCase(sScript)) {
            if (bTitleBlockOnly) {
                throw new Exception("IMPOSSIBLE CASE (script=Nouse and TitleBlockOnly)");
            }
            // suppression .Z dans name of file
            int index1 = checkedOutFile.getAbsolutePath().lastIndexOf(".Z");
            String WithoutZ = checkedOutFile.getAbsolutePath();
            if (index1 >= 0) {
                DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/printTiffWithFile: Suppresion .Z " + checkedOutFile.getName());
                WithoutZ = WithoutZ.substring(0, index1);
                File source = new File(checkedOutFile.getAbsolutePath());
                File destination = new File(WithoutZ);
                source.renameTo(destination);
                DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/printTiffWithFile: New name after Suppresion .Z " + WithoutZ);
            }
            sPrinterCmdComplete = getViewCommand(isWindows).concat(" ").concat(WithoutZ);

        } else if (bTitleBlockOnly) {
            sPrinterCmdComplete = Local_OS_Information.buildPrintCommand(sPrinterCmd, titleBlockFile.getAbsolutePath(), sNbCopy, sScale, sColor, sPaperFormat, bTitleBlockOnly);

        } else if ("NoTitleBlock".equalsIgnoreCase(sScript)) {
            sPrinterCmdComplete = Local_OS_Information.buildPrintCommand(sPrinterCmd, checkedOutFile.getAbsolutePath(), sNbCopy, sScale, sColor, sPaperFormat, false);
        } else {
            displayProgressMessage("Merge CAD file with title block ...");
            // FPDM Add Start : JFA 2005/07/26 RFC 3783
            if (!"".equals(sHpgl)) {
                if (bListHPGL2.size() == 0) {
                    bListHPGL2.add(new Boolean(sHpgl).booleanValue());
                } else {
                    bListHPGL2.set(0, new Boolean(sHpgl).booleanValue());
                }
            }
            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/printTiffWithFile: bListHPGL2.get(0) " + bListHPGL2.get(0));
            // FPDM Add End : JFA 2005/07/26 RFC 3783
            File mergedFile = HPGLMerger.mergeHpgl(checkedOutFile.getAbsolutePath(), titleBlockFile.getAbsolutePath(), cadTitleBlock.getXOffset(), cadTitleBlock.getYOffset(), bListHPGL2.get(0));

            checkedOutFile.delete();
            titleBlockFile.delete();

            if (doPrint) {
                sPrinterCmdComplete = Local_OS_Information.buildPrintCommand(sPrinterCmd, mergedFile.getAbsolutePath(), sNbCopy, sScale, sColor, sPaperFormat, bTitleBlockOnly);
            } else {
                sPrinterCmdComplete = getViewCommand(isWindows) + " " + mergedFile.getAbsolutePath();
                DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/printTiffWithFile: sPrinterCmdComplete " + sPrinterCmdComplete);
            }
        }

        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/printTiffWithFile: Print " + sPrinterCmdComplete);

        if (sPrinterCmdComplete != null) {
            displayProgressMessage("Print the plot ...");
            try {
                Local_OS_Information.executeCommand(sPrinterCmdComplete, 2000, false);
            } catch (Exception e) {
                DebugUtil.debug(DebugUtil.ERROR, CLASSNAME, "/printTiffWithFile: Exception launched by printing command : " + e.getMessage());
            }

        } else {
            // new DisplayMessage("Information", "No Association for Plot file has been found.", "Information", "OK");
            agGUI.displayResultMessageAndContinue("Information", "No Association for Plot file has been found.", "OK");
        }
        // this.closeAppletWindow();
    }

    @SuppressWarnings({ "deprecation", "unused" })
    private void viewAsTIF(boolean isWindows) throws Exception {
        displayProgressMessage("Retrieving local temporary directory ...");
        // sLocalTempDirectory = getPrintTempDirectory(isWindows);

        // ----------------------------------------------------
        // Split inputed parameters
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/viewAsTIF: sObjectId " + sObjectId);
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/viewAsTIF: listOfFiles " + listOfFiles);
        String[] sCADDefIds = StringUtil.split(sObjectId, Parameters.DELIM);
        String[] ListOfFiles = StringUtil.split(listOfFiles, Parameters.DELIM);
        String[] ListOfScripts = StringUtil.split(sListScript, Parameters.DELIM);
        //String[] ListOfSoftwares = StringUtil.split(listOfFiles, Parameters.DELIM);
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/viewAsTIF: ListOfFiles : ", ListOfFiles);

        for (int iCpt = 0; iCpt < (ListOfFiles.length / 5); iCpt++) {
            String sViewId = ListOfFiles[(5 * iCpt)];
            sFormat = ListOfFiles[(5 * iCpt) + 1];
            String sFileName = ListOfFiles[(5 * iCpt) + 2];
            // String sPaperFormat = ListOfFiles[(5 * iCpt) + 3];
            String sViewName = ListOfFiles[(5 * iCpt) + 4];
            sScript = ListOfScripts[iCpt];
            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/viewAsTIF: sViewId : " + sViewId + " sFileName : " + sFileName + " sScript : " + sScript);
            CADTitleBlock cadTitleBlock = buildTitleBlock(sCADDefIds, ListOfFiles, iCpt);
            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/viewAsTIF: sLocalTempDirectory : " + sLocalTempDirectory);
            boolean bHPGL = true;
            if (bListHPGL2.size() > iCpt) {
                bHPGL = bListHPGL2.get(iCpt);
            } else {
                bHPGL = bListHPGL2.get(0);
            }
            File checkedOutFile = Checkouter.checkoutAsTIF(sLocalTempDirectory, sViewId, sViewName, sFileName, sFormat, bLock, sObjectId, "2DViewable", MAIN_SERVER_BASE_URL, cadTitleBlock, bHPGL);

        }

        // FPDM ADD Start : JFA 2006/04/21 RFC #2675
        // Parameters.getAppletUI().agGUI.displayResultMessage("Information", "Export done successfully", true);
        agGUI.displayResultMessage("Information", "Export done successfully", "OK");
        // FPDM ADD End : JFA 2006/04/21 RFC #2675

    }

    /*
     * Replaced by static BusObject[] BusObject.retrieveBusInfoFromServlet() protected String GetID(String ListName, String ListRevision, String sMCSServletName, String hostSite,String sessionId,
     * String sessionName, String sUser) throws Exception
     */
    private boolean fileExistsTravail(String fileInLst) throws FileNotFoundException {
        int index = fileInLst.lastIndexOf(".Z");
        if (index >= 0) {
            fileInLst = fileInLst.substring(0, index);
        }
        String fileDir = "";
        try {
            String Workrepertory = Local_OS_Information.getUnixEnv("TRAVAIL");
            fileDir = Workrepertory.concat("/").concat(fileInLst);
        } catch (Exception e) {
            // new DisplayMessage("Information", "ERROR  " + e.toString(), "Information", "OK");
            agGUI.displayResultMessageAndContinue("Information", "ERROR  " + e.toString(), "OK");
        }

        File f = new File(fileDir);

        if (f.exists()) {
            throw new FileNotFoundException("The file exist, name: " + fileDir);
        }

        return true;
    }

    public void execScriptCheckout(boolean is3D, String oldFileName, boolean bLock, String system_generateur, String sTypeOri, String sCurrentOri, String version_system_generateur,
            String commande_sysgen, String cad_environment, String cad_format, String sInfo, String newFileName, String sFileRenamingParam, boolean bPreIntegration) throws Exception {

        String scommand;
        String script_cao = "";
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/execScriptCheckout: system_generateur the script: " + system_generateur);
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/execScriptCheckout: sTypeOri the script: " + sTypeOri);
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/execScriptCheckout: sCurrentOri the script: " + sCurrentOri);
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/execScriptCheckout: version_system_generateur the script: " + version_system_generateur);
        String sstype = sTypeOri.substring(0, sTypeOri.indexOf("|"));
        if (sstype.startsWith("FPDM")) {
            sstype = sstype.replace("FPDM ", "");
            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/execScriptCheckout: sTypeOri : " + sTypeOri);
        }
        // PRI STERIA Change new name if the parameter newFileName is not null MODIFY START
        // String sNameRev = sTypeOri.substring(sTypeOri.indexOf("|")+1, sTypeOri.length());
        String sNameRev;
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/execScriptCheckout: newFileName: " + newFileName);
        if (!"".equals(newFileName)) {
            sNameRev = newFileName;
        } else {
            sNameRev = sTypeOri.substring(sTypeOri.indexOf("|") + 1, sTypeOri.length());
        }
        // PRI STERIA Change new name if the parameter newFileName is not null MODIFY END

        // suppression .Z dans name of file
        int index = oldFileName.lastIndexOf(".Z");
        if (index >= 0) {
            oldFileName = oldFileName.substring(0, index);
            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/execScriptCheckout: Suppression .Z " + oldFileName);
        }
        // end suppression Z
        index = oldFileName.lastIndexOf(".");
        if (index >= 0) {
            oldFileName = oldFileName.substring(0, index);
        }
        if (oldFileName.endsWith("_3D")) {
            oldFileName = oldFileName.substring(0, oldFileName.length() - 3);
        }
        String prefix = sNameRev.substring(0, sNameRev.length() - 2);
        String suffix = sNameRev.substring(sNameRev.length() - 2);
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/execScriptCheckout: prefix:" + prefix);
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/execScriptCheckout: suffix:" + suffix);
        if (oldFileName.equals(prefix) && "00".equals(suffix)) {
            oldFileName = sNameRev;
        }
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/execScriptCheckout: oldFileName: " + oldFileName);

        if (system_generateur.equals("UG") || system_generateur.equals("UNIGRAPHICS")) {
            try {
                script_cao = Local_OS_Information.getUnixEnv("MAGELLAN_UG");
                DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/execScriptCheckout: function getUnixEnv(\"MAGELLAN_UG\") returned : " + script_cao);
            } catch (Exception e) {
                DebugUtil.debug(DebugUtil.ERROR, CLASSNAME, "/execScriptCheckout: function getUnixEnv(\"MAGELLAN_UG\") launch exception : " + e.toString());
            }
            if (script_cao != null && !"null".equals(script_cao) && !"".equals(script_cao))
                script_cao = script_cao.concat("/");

            script_cao = script_cao.concat("magellan2UG -cs UG -d ");
        } else if (system_generateur.equals("CATIA")) {
            try {
                script_cao = Local_OS_Information.getUnixEnv("MAGELLAN_CATIA");
                DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/execScriptCheckout: function getUnixEnv(\"MAGELLAN_CATIA\") returned : " + script_cao);
            } catch (Exception e) {
                DebugUtil.debug(DebugUtil.ERROR, CLASSNAME, "/execScriptCheckout: function getUnixEnv(\"MAGELLAN_CATIA\") launch exception : " + e.toString());
            }
            if (script_cao != null && !"null".equals(script_cao) && !"".equals(script_cao))
                script_cao = script_cao.concat("/");

            script_cao = script_cao.concat("magellan2CATIA -cs CATIA -d ");
        }

        if (script_cao != null) {
            if (bLock)
                if (sCheckoutdir != null && sCheckoutdir.length() > 0 && !sCheckoutdir.equals("null"))
                    scommand = script_cao.concat(sCheckoutdir);
                else
                    scommand = script_cao.concat("TRAVAIL");
            else {
                if (sCheckoutdir != null && sCheckoutdir.length() > 0 && !sCheckoutdir.equals("null"))
                    scommand = script_cao.concat(sCheckoutdir);
                else
                    scommand = script_cao.concat("REFERENCE");
            }
            scommand += " -n " + oldFileName;
            scommand += " -T " + sstype;
            scommand += " -N " + sNameRev;

            // version_de_cartouche, 2 is type_vue != ""
            scommand += " -V 2";

            scommand += " -l " + sCurrentOri;

            if (version_system_generateur != null && !"null".equals(version_system_generateur) && !"".equals(version_system_generateur)) {
                scommand += " -cr " + version_system_generateur;
            }

            if (commande_sysgen != null && !"null".equals(commande_sysgen) && !"".equals(commande_sysgen)) {
                scommand += " -cc " + commande_sysgen;
            }
            if (cad_format != null && !"null".equals(cad_format) && !"".equals(cad_format)) {
                scommand += " -cf " + cad_format;
            }
            if (cad_environment != null && !"null".equals(cad_environment) && !"".equals(cad_environment)) {
                scommand += " -ce " + cad_environment;
            }

            // Parameter -D is to know if a 3D is to convert
            if (!("3DONLY".equals(sInfo))) {
                if (is3D && "".equals(sFileRenamingParam)) {
                    scommand += " -D TRUE";
                } else {
                    scommand += " -D FALSE";
                }
            } else {
                scommand += " -D 3DONLY";
            }

            // Parameter -pr is for preintegration
            if (bPreIntegration) {
                scommand += " -pr YES";
            }

            // Parameter -DMU is to know the list of files to rename and copy
            DebugUtil.debug(DebugUtil.INFO, CLASSNAME, "/execScriptCheckout: pMSBGCheckOut: " + pMSBGCheckOut);
            if (!"".equals(sFileRenamingParam)) {
                scommand += " -DMU " + sFileRenamingParam;
            } else if (pMSBGCheckOut != null && !"".equals(pMSBGCheckOut)) {
                scommand += " -DMU OnlyOne";
            }

            DebugUtil.debug(DebugUtil.INFO, CLASSNAME, "/execScriptCheckout: Script executed : " + scommand);
            try {
                // Execute the script waiting "OK" or "ERROR" in the std output stream
                // Max delay delay is over 10 hours
                // Catch IOException (those exception are on the error output stream of the script.
                // These may include some WARNING messages)
                // other Exception are not catched, mainly : the ERROR label in the std output stream.
                String sOutput = Local_OS_Information.executeCommandAndWaitReturn(scommand, 60000000);
                DebugUtil.debug(DebugUtil.INFO, CLASSNAME, "/execScriptCheckout: Output returned by the script : " + sOutput);
            } catch (IOException e) {
                DebugUtil.debug(DebugUtil.ERROR, CLASSNAME, "/execScriptCheckout: Error returned by the script : " + e.getMessage());
            }
        }

    }

    @SuppressWarnings("deprecation")
    public static String[] displayListCheckin(String title, String message, String[] ListFiles) {
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/displayListCheckin: entry in List Checkin");

        LstChooser frameChooser = new LstChooser(title);
        Dialog d = new Dialog(frameChooser, title, true);
        d.addWindowListener(new AppletWindowListener(d));
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraint = new GridBagConstraints();
        constraint.insets.top = 2;
        constraint.insets.bottom = 2;
        constraint.insets.left = 0;
        constraint.insets.right = 0;
        constraint.anchor = GridBagConstraints.CENTER;
        constraint.fill = GridBagConstraints.NONE;
        constraint.gridwidth = GridBagConstraints.REMAINDER;
        Label l = new Label(message);
        layout.setConstraints(l, constraint);

        d.setLayout(layout);
        d.add(l);
        d.pack();
        boolean bexist = false;
        int iwidth = 600;
        for (int j = 0; j < ListFiles.length; j++) {
            String sFile = ListFiles[j];
            if (sFile.endsWith(".lst") || (title.indexOf("Result") != -1)) {
                if (sFile.endsWith(".lst"))
                    sFile = sFile.substring(0, sFile.lastIndexOf(".lst"));
                int iLength = sFile.length() * 10;
                if (iwidth < iLength) {
                    iwidth = iLength;
                }
                frameChooser.lst.add(sFile);
                bexist = true;
            }
        }
        if (bexist == true) {
            int height = 40;
            frameChooser.lst.setSize(iwidth + 200, height);
            constraint.gridwidth = GridBagConstraints.REMAINDER;
            constraint.ipadx = 300;
            constraint.ipady = 5;
            layout.setConstraints(frameChooser.lst, constraint);
            d.add(frameChooser.lst);

            Button okButton = new Button("OK");
            okButton.setActionCommand("OK");
            okButton.addActionListener(frameChooser);

            constraint.gridwidth = GridBagConstraints.REMAINDER;
            constraint.ipadx = 50;
            constraint.ipady = 5;
            layout.setConstraints(okButton, constraint);
            d.add(okButton);

            Rectangle rec = d.getBounds();
            double width = rec.width;
            double heigth = rec.height;

            if (width < 500) {
                width = 500;
            }
            if (heigth < 300) {
                heigth = 300;
            }
            d.setBounds(500, 300, (int) width, (int) heigth);
            d.show();
        }
        return AppletCheckInOut.sFiletoCheckin;
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
    @SuppressWarnings({ "unused", "deprecation" })
    private void checkin() throws Exception {

        displayProgressMessage("Retrieving local temporary directory ...");

        // --------------------------------------------------------------------
        // Retrieve local temp directory
        // --------------------------------------------------------------------
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkin: Retrieve Local Temp Directory");
        String sHomeDirectoryPath = Local_OS_Information.getLocalHomeDirectory(Parameters.HOME_VAR_LOCALFILE_PATH);
        sLocalTempDirectory = sHomeDirectoryPath + "/temp";
        DebugUtil.debug(DebugUtil.INFO, CLASSNAME, "/checkin: Local Temp Directory = " + sLocalTempDirectory);

        // --------------------------------------------------------------------
        // For a single checkin
        // --------------------------------------------------------------------
        DebugUtil.debug(DebugUtil.INFO, CLASSNAME, "/checkin: checkin all: " + allcheckin);
        if (!allcheckin) {
            DebugUtil.debug(DebugUtil.INFO, CLASSNAME, "/checkin: Single checkin Component with LST file = " + sParameterFile);
            String[] ListFilesResult = new String[1];
            ListFilesResult[0] = sParameterFile;
            CheckinCADFile.checkinAllFilesFromLSTFiles(sLocalTempDirectory, ListFilesResult, bAppend, bLock, MAIN_SERVER_BASE_URL, scheckin2D, sObjectId, listcheckinout, allcheckin, sOrganizationName);

            // --------------------------------------------------------------------
            // For a grouped checkin
            // --------------------------------------------------------------------
        } else {
            DebugUtil.debug(DebugUtil.INFO, CLASSNAME, "/checkin: Multiple checkin Component");
            listcheckinout = "all";
            // ----------------------------------------------------------------
            // The user chooses which files are to be checked in
            // ----------------------------------------------------------------
            String[] ListFilesResult = null;
            // Retrieve all files in the local directory
            displayProgressMessage("Looking for LST files in temporary directory ...");
            String[] ListFiles = Local_OS_Information.retrieveFilesList(sLocalTempDirectory);
            if (ListFiles == null || ListFiles.length <= 0) {
                DebugUtil.debug(DebugUtil.INFO, CLASSNAME, "/checkin:  No files to ckeck in.");

            } else {

                // Display a listbox to enable the user to make its choice between LST files
                ListFilesResult = displayListCheckin("List Checkin", "List checkin : ", ListFiles);

                if (ListFilesResult != null && ListFilesResult.length > 0) {
                    DebugUtil.debug(DebugUtil.INFO, CLASSNAME, "/checkin: LIST OF LST FILES SELECTED  : ", ListFilesResult);
                } else {
                    DebugUtil.debug(DebugUtil.INFO, CLASSNAME, "/checkin: NO LST FILES  ");
                }
            }

            // ----------------------------------------------------------------
            // Foreach LST file, proceed the Checkin
            // ----------------------------------------------------------------
            if (ListFilesResult != null && ListFilesResult.length > 0) {
                Object[] oListFilesCheckedIn = CheckinCADFile.checkinAllFilesFromLSTFiles(sLocalTempDirectory, ListFilesResult, bAppend, bLock, MAIN_SERVER_BASE_URL, scheckin2D, sObjectId,
                        listcheckinout, allcheckin,sOrganizationName);
                String[] ListFilesCheckedIn = (String[]) oListFilesCheckedIn[0];

                // displayListCheckin("List Checkin Result", "List checkin Result: ", ListFilesCheckedIn);
                if (sCollectionName != null && !"".equals(sCollectionName)) {
                    displayProgressMessage("Creating the collection:" + sCollectionName);

                    Object[] aBusObject = (Object[]) oListFilesCheckedIn[1];
                    Object[] aResult = AppletServletCommunication.requestServerTask(Parameters.getMainServerBaseURL() + Parameters.FAURECIA_eMCS_SERVLET_NAME + "/createCollection?name="
                            + sCollectionName, aBusObject);
                    String sResultCollection = (String) aResult[0];
                    if ("OK".equals(sResultCollection)) {
                        String sMessage = MessageFormat.format(sCollectionMessage, new Object[] { sCollectionName });
                        // new DisplayMessage("Information", sMessage, "Information", "OK");
                        // agGUI.displayResultMessageAndContinue("Information", sMessage,"OK");
                        agGUI.displayResultMessage("Information", sMessage, "OK");
                    }
                }
            } else {
                // Fin du traitement si fichier a ingerer
                // new DisplayMessage("Information", "No Model ready for checkin/Pas de Modele pret a etre integre", "Information", "OK");
                agGUI.displayResultMessageAndContinue("Information", "No Model ready for checkin/Pas de Modele pret a etre integre", "OK");
            }
            // let the user close the window.
            agGUI.displayResultAndExit("Checkin done.");
        }

        // --------------------------------------------------------------------
        // REDIRECTION OF THE OPENER JSP PAGE
        System.out.println("before getDocumentBase()");
        if (!allcheckin) {
            // Refresh opener page
            bRefreshOpenerPage = true;
        }
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
    @SuppressWarnings("deprecation")
    private void checkout() throws Exception {

        String sDebugTrace = "";

        displayProgressMessage("Retrieving local temporary directory ...");

        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkout --> sAdditionalInfo : " + sAdditionalInfo);

        // --------------------------------------------------------------------
        // Retrieve local temp directory
        // --------------------------------------------------------------------
        DebugUtil.debug(DebugUtil.INFO, CLASSNAME, "/checkout: Retrieve Local Temp Directory");
        String sHomeDirectoryPath = Local_OS_Information.getLocalHomeDirectory(Parameters.HOME_VAR_LOCALFILE_PATH);
        sLocalTempDirectory = sHomeDirectoryPath + "/temp";
        DebugUtil.debug(DebugUtil.INFO, CLASSNAME, "/checkout: Local Temp Directory = " + sLocalTempDirectory);

        displayProgressMessage("Retrieve object information...");

        if (listobjid != null) {
            listcheckinout = "all";
        }
        // DebugUtil.debug(DebugUtil.INFO, CLASSNAME, "/checkout");

        // ===================================
        // == RETRIEVE LIST OF FILES TO CHECKOUT ==
        // ===================================
        sDebugTrace = "== RETRIEVE LIST OF FILES TO CHECKOUT ==";
        DebugUtil.debug(DebugUtil.INFO, CLASSNAME, "/checkout: " + sDebugTrace);

        Hashtable<String, String> ht_InputParameters = new Hashtable<String, String>();
        ht_InputParameters.put("objectId", sObjectId);
        ht_InputParameters.put("format", sFormat);
        ht_InputParameters.put("recurseLevel", recLevel);
        ht_InputParameters.put("relationPattern", relPat);
        ht_InputParameters.put("typePattern", typeList);

        Hashtable[] a_InputParameters = new Hashtable[1];
        a_InputParameters[0] = ht_InputParameters;

        String sURL = MCS_SERVLET_URL + "/getRelatedFiles";
        sURL += "?debug=" + System.getProperty("faurecia.debugging");
        sURL += "&time=" + new Date().getTime();

        Object[] a_TempObjects = AppletServletCommunication.requestServerTask(sURL, a_InputParameters);
        BusObject[] a_BusObject = new BusObject[a_TempObjects.length];
        for (int i = 0; i < a_TempObjects.length; i++) {
            a_BusObject[i] = (BusObject) a_TempObjects[i];
        }

        int nbOfFilesCheckedOut = 0;
        String sCADComponentName = "";
        String sCADComponentRev = "";
        String sCADComponentId = "";
        boolean is3D = false;
        Vector<BusObject> vCADComponentFiles = new Vector<BusObject>();

        for (int iCpt = 0; iCpt < a_BusObject.length; iCpt++) {
            DebugUtil.debug(DebugUtil.INFO, CLASSNAME, "/checkout: busObject[" + iCpt + "] = " + a_BusObject[iCpt].toString());
            String childType = a_BusObject[iCpt].getType();
            String childName = a_BusObject[iCpt].getName();
            String childRev = a_BusObject[iCpt].getRevision();
            String childId = a_BusObject[iCpt].getId();
            String childFileName = StringUtil.returnDefaultIfEmpty(a_BusObject[iCpt].getFileName(), "");
            String childFileFormat = StringUtil.returnDefaultIfEmpty(a_BusObject[iCpt].getFormat(), "");

            if (Parameters.TYPE_FPDM_CAD_COMPONENT.equals(childType)) {
                sCADComponentName = childName;
                sCADComponentRev = childRev;
                sCADComponentId = childId;
                if (!"".equals(childFileName)) {
                    vCADComponentFiles.addElement(a_BusObject[iCpt]);
                }
                if (bLock && !"".equals(childFileName)) {
                    fileExistsTravail(childFileName);
                }
            } else if (Parameters.TYPE_FPDM_CAD_DRAWING.equals(childType)) {
                is3D = true;
                if (bLock && !"".equals(childFileName)) {
                    fileExistsTravail(childFileName);
                }
                if (!"".equals(childFileName)) {
                    vCADComponentFiles.addElement(a_BusObject[iCpt]);
                }
            }

            if ("".equals(childFileName)) {
                continue;
            }
            if (!"2DViewable".equals(childType)) {
                // ===================================
                // == CHECKOUT FILE FROM eMATRIX ==
                // ===================================
                displayProgressMessage("Checkout file \'" + childFileName + "\'...");
                sDebugTrace = "== CHECKOUT FILE \'" + childFileName + "\' FROM eMATRIX ==";
                DebugUtil.debug(DebugUtil.INFO, CLASSNAME, "/checkout: " + sDebugTrace);
                DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkout: sLocalTempDirectory     : " + sLocalTempDirectory);
                DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkout: childId                 : " + childId);
                DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkout: childFileName           : " + childFileName);
                DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkout: sFormat                 : " + childFileFormat);
                DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkout: sObjectId               : " + sObjectId);
                DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkout: childType               : " + childType);
                DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkout: MAIN_SERVER_BASE_URL    : " + MAIN_SERVER_BASE_URL);

                Checkouter.checkoutFile(sLocalTempDirectory, childId, childFileName, childFileFormat, false, sObjectId, childType, MAIN_SERVER_BASE_URL);

                nbOfFilesCheckedOut++;
            }
        }

        sDebugTrace = nbOfFilesCheckedOut + " files has been checked out from eMatrix.";
        DebugUtil.debug(DebugUtil.INFO, CLASSNAME, "/checkout: " + sDebugTrace);

        // ===================================
        // == BUILD LST FILE ==
        // ===================================
        String sLSTFileName = sCADComponentName + sCADComponentRev + ".lst";
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkout: Name of the LST file : " + sLSTFileName);

        BusObject boCAD = new BusObject(sCADComponentId, "CAD Component", sCADComponentName, sCADComponentRev, sLSTFileName);
        boCAD.setToConnectId(sObjectId);

        // ===================================
        // ASK THE SERVER FOR A NEW LST FILE
        // ===================================
        if (!("3DONLY".equals(sAdditionalInfo)) && !("2DONLY".equals(sAdditionalInfo))) {
            displayProgressMessage("Build LST file...");
            sDebugTrace = "== BUILD LST FILE ==";
            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkout: " + sDebugTrace);

            String sURL2 = MCS_SERVLET_URL + "/retrieveLSTFile";
            sURL2 += "?debug=" + System.getProperty("faurecia.debugging");
            sURL2 += "&time=" + new Date().getTime();

            BusObject[] a_BusObjects = new BusObject[1];
            a_BusObjects[0] = boCAD;
            Object[] retObjects = AppletServletCommunication.requestServerTask(sURL2, a_BusObjects);
            ((AbstractFile) retObjects[0]).createFileOnSystemDrive(sLocalTempDirectory);
        }

        // ===================================
        // ASK THE SERVER FOR A NEW TITLE BLOCK FILE
        // ===================================
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkout: bPreIntegration : " + bPreIntegration);
        if (!("3DONLY".equals(sAdditionalInfo)) && !("2DONLY".equals(sAdditionalInfo)) && !bPreIntegration) {
            try {
                sDebugTrace = "== BUILD TITLE BLOCK FILES ==";
                DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkout: " + sDebugTrace);
                String sTxtOnly;
                if (bLock) {
                    sTxtOnly = "checkout";
                } else {
                    sTxtOnly = "copy";
                }

                String[] ListOfFiles = StringUtil.split(listOfFiles, Parameters.DELIM);

                // ----------------------------------------------------
                // Define Title block input parameters
                DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkout: ListOfFiles : ", ListOfFiles);
                for (int iCpt = 0; iCpt < (ListOfFiles.length - 2); iCpt = (iCpt + 3)) {
                    DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkout: sObjectId : " + sObjectId);
                    String sViewId = ListOfFiles[iCpt];
                    DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkout: sViewId : " + sViewId);
                    sFormat = ListOfFiles[iCpt + 1];
                    DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkout: sFormat : " + sFormat);
                    String sFileName = ListOfFiles[iCpt + 2];
                    DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkout: sFileName : " + sFileName);
                    displayProgressMessage("Construct title block for file \'" + sFileName + "\'...");

                    DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkout: sTypeOri : " + sTypeOri);
                    String type = sTypeOri.substring(0, sTypeOri.indexOf("|"));
                    DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkout: type : " + type);
//                    int prefixIndex = type.indexOf(" ");
//                    String prefix;
//                    if (prefixIndex < 0) {
//                        prefix = type;
//                    } else {
//                        prefix = type.substring(0, prefixIndex);
//                    }
//                    DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkout: prefix : " + prefix);

//                    sFileName = prefix + "_" + sFileName.substring(0, sFileName.lastIndexOf(".")) + ".cad";
                    sFileName = "CAD_" + sFileName.substring(0, sFileName.lastIndexOf(".")) + ".cad";
                    DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkout: sFileName : " + sFileName);

                    CADTitleBlock[] a_TitleBlocks = new CADTitleBlock[1];
                    a_TitleBlocks[0] = new CADTitleBlock(sObjectId, sViewId, sFormat, sLanguage, sBonProd, sHistoricLevel, sNbOfFolios, sAdditionalInfo, sTxtOnly, sNissanOrRenault);
	                a_TitleBlocks = CADTitleBlock.retrieveTitleBlockFromServer(a_TitleBlocks, sServerTempDirectory, MAIN_SERVER_BASE_URL);
                    for (int iCpt2 = 0; iCpt2 < a_TitleBlocks.length; iCpt2++) {
                        a_TitleBlocks[iCpt2].createTitleBlockFile(sLocalTempDirectory, sFileName);
                        sDebugTrace = "Title Block generated : " + sFileName;
                        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkout: sDebugTrace: " + sDebugTrace);

                    }
                }
            } catch (Exception e) {
                DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkout: error during generation of TitleBlock " + e);
            }
        }
        if ("UG".equals(system_generateur) || "UNIGRAPHICS".equals(system_generateur) || "CATIA".equals(system_generateur)) {

            sDebugTrace = "== CONVERTION FROM MAGELLAN TO CAD SYSTEM ==";
            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkout: " + sDebugTrace);

            // Si aucun fichier dans le CAD COMPONENT, alors lancement du script sur le fichier LST
            if (vCADComponentFiles.size() == 0) {
                vCADComponentFiles.addElement(boCAD);
            }

            // --------------------------------------------------
            // RETRIEVE THE FILE THAT WILL BE USED FOR UNIX SCRIPT 'MagellanToCatia'
            // (before executing this script, "_3D" will be removed from the name)
            // This file is the shortest file
            // (eg : in this list : (toto_2.model, toto_3.model, toto.model)
            // the script must be executed on toto.model)
            // --------------------------------------------------
            String fileName = extractFilename(vCADComponentFiles);

            String sNewFileName = "";
            String sFileRenamingParam = "";
            if (pMSBGCheckOut != null) {
                // Si DMU ou CUSTOMER checkout => pas d'option preintegration dans Catia.
                bPreIntegration = false;
                Hashtable htFiles = StringUtil.constructHashtable(pMSBGCheckOut, "|", "$");
                if (fileName.endsWith(".Z") && htFiles.containsKey(fileName.substring(0, fileName.length() - 2))) {
                    sNewFileName = (String) htFiles.get(fileName.substring(0, fileName.length() - 2));
                } else if (htFiles.containsKey(fileName)) {
                    sNewFileName = (String) htFiles.get(fileName);
                }

                for (Enumeration eEnum = htFiles.keys(); eEnum.hasMoreElements();) {
                    String sOldFileName2 = (String) eEnum.nextElement();
                    String sNewFileName2 = (String) htFiles.get(sOldFileName2);

                    int iIndex = sOldFileName2.indexOf(".");
                    if (iIndex > 0) {
                        sOldFileName2 = sOldFileName2.substring(0, iIndex);
                    }
                    iIndex = sNewFileName2.indexOf(".");
                    if (iIndex > 0) {
                        sNewFileName2 = sNewFileName2.substring(0, iIndex);
                    }

                    if (!sOldFileName2.equalsIgnoreCase(fileName.substring(0, fileName.indexOf(".")))) {
                        if (!"".equals(sFileRenamingParam)) {
                            sFileRenamingParam += "|";
                        }
                        sFileRenamingParam += sOldFileName2 + "|" + sNewFileName2;
                    }
                }
            }
            displayProgressMessage("Convert file to " + system_generateur + "...");

            // FPDM ADD Add JFA 13/11/2006 RFC 1201
            if (("2DONLY".equals(sAdditionalInfo))) {
                is3D = false;
            }
            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkout --> Checkout 2D only... is3D : " + is3D);
            // FPDM ADD End JFA 13/11/2006 RFC 1201

            execScriptCheckout(is3D, fileName, bLock, system_generateur, sTypeOri, sCurrentOri, version_system_generateur, commande_sysgen, cad_environment, cad_format, sAdditionalInfo, sNewFileName,
                    sFileRenamingParam, bPreIntegration);

        }

        // ===================================
        // LOCK CAD DEFINITION OBJECT
        // ===================================
        if (bLock) {
            displayProgressMessage("Lock CAD Definition ...");
            sURL = MCS_SERVLET_URL + "/lock";
            sURL += "?debug=" + System.getProperty("faurecia.debugging");
            sURL += "&time=" + new Date().getTime();

            a_BusObject = new BusObject[1];
            a_BusObject[0] = new BusObject(sObjectId, "", "", "", "");
            AppletServletCommunication.requestServerTask(sURL, a_BusObject);

        }

        if ("".equals(listcheckinout)) {
            String message = "";
            if (nbOfFilesCheckedOut > 0) {
                message = nbOfFilesCheckedOut + " files have been successfully checked-out!";
            } else {
                message = "No file has been checked-out! The object may be locked.";
            }
            displayProgressMessage(message);
            // new DisplayMessage("Information", message, "Information", "OK");
            agGUI.displayResultMessage("Information", message, "OK");
        }
        sDebugTrace = "== CHECK-OUT IS ENDED ==";
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkout: sDebugTrace: " + sDebugTrace);

        // --------------------------------------------------------------------
        // REDIRECTION OF THE OPENER JSP PAGE
        // System.out.println("before getDocumentBase()");
        // Refresh opener
        bRefreshOpenerPage = true;

        // Override default Close window if needed
        if (listobjid != null) {
            if (pMSBGCheckOut != null) {
                sCloseWindowURL = "engineeringcentral/stMSBGCheckOutCopyAll.jsp?listobjid=" + listobjid + "&selInformations=" + sAdditionalInfo + "&historicLevel=" + sHistoricLevel + "&bonProd="
                        + sBonProd;
            } else {
                // FPDM Add Start - JFA 2005/12/23 RFC 1897
                if (bLock) {
                    sCloseWindowURL = "engineeringcentral/stCheckOutModifyAll.jsp?listobjid=" + listobjid + "&selInformations=" + sAdditionalInfo + "&historicLevel=" + sHistoricLevel + "&bonProd="
                            + sBonProd + "&Checkoutdir=" + sCheckoutdir;
                    // FPDM REPLACE Start - JFA 2005/12/23 RFC 1201
                } else if (!"2DONLY".equals(sAdditionalInfo)) {
                    // FPDM Add End - JFA 2005/12/23 RFC 1897
                    sCloseWindowURL = "engineeringcentral/stCheckOutCopyAll.jsp?listobjid=" + listobjid + "&selInformations=" + sAdditionalInfo + "&historicLevel=" + sHistoricLevel + "&bonProd="
                            + sBonProd + "&Checkoutdir=" + sCheckoutdir;
                    // FPDM Add Start - JFA 2005/12/23 RFC 1897
                } else {
                    sCloseWindowURL = "engineeringcentral/stCheckOutCopyAll2D.jsp?listobjid=" + listobjid + "&selInformations=" + sAdditionalInfo + "&historicLevel=" + sHistoricLevel + "&bonProd="
                            + sBonProd + "&Checkoutdir=" + sCheckoutdir;
                }
                // FPDM Add End - JFA 2005/12/23 RFC 1897
                // FPDM REPLACE Start - JFA 2005/12/23 RFC 1201
            }
        } else {
            // All id has been treated, we need to close the window applet
            bRefreshOpenerPage = false;
        }
        // Closing applet windows
        AppletCheckInOut.this.closeAppletWindow();
    }

    @SuppressWarnings("deprecation")
    private void printWindows() throws Exception {

        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/printWindows- --> sScript : " + sScript);

        if (!"Nouse".equalsIgnoreCase(sScript)) {

            // --------------------------------------------------------------------
            // Retrieve local temp directory
            // --------------------------------------------------------------------

            sLocalTempDirectory = getPrintTempDirectory(true);

            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/printWindows: printing for Windows");

            // ========================================================
            // Split input parameters
            // ========================================================
            String[] sCADDefIds = StringUtil.split(sObjectId, Parameters.DELIM);
            String[] ListOfFiles = StringUtil.split(listOfFiles, Parameters.DELIM);

            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/printWindows: sObjectId " + sObjectId);
            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/printWindows: ListOfFiles : ", ListOfFiles);

            String sPrinterCmd = "";

            // FPDM Add Start : JFA 2005/07/26 RFC 3783
            boolean bListPropertyExist = false;
            StringTokenizer tokenHPGL = null;

            if (sListHPGLProperty != null && !"".equals(sListHPGLProperty)) {
                DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/printWindows: sListHPGLProperty : " + sListHPGLProperty);
                tokenHPGL = new StringTokenizer(sListHPGLProperty, "|");
                bListPropertyExist = true;
            }
            String sHPGL = "";
            // FPDM Add Start : JFA 2005/07/26 RFC 3783

            for (int iCpt = 0; iCpt < (ListOfFiles.length / 5); iCpt++) {
                String sViewId = ListOfFiles[(5 * iCpt)];
                sFormat = ListOfFiles[(5 * iCpt) + 1];
                String sFileName = ListOfFiles[(5 * iCpt) + 2];
                String sPaperFormat = ListOfFiles[(5 * iCpt) + 3];
                String sViewName = ListOfFiles[(5 * iCpt) + 4];

                // FPDM Add Start : JFA 2005/07/26 RFC 3783
                if (bListPropertyExist && tokenHPGL.hasMoreTokens()) {
                    sHPGL = tokenHPGL.nextToken();
                }
                // FPDM Add End : JFA 2005/07/26 RFC 3783

                // ========================================================
                // Retrieve title blocks from server
                // ========================================================

                DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/printWindows: sViewId " + sViewId);
                DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/printWindows: sFormat " + sFormat);
                DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/printWindows: sFileName " + sFileName);
                DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/printWindows: sPaperFormat " + sPaperFormat);
                DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/printWindows: sViewName " + sViewName);
                DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/printWindows: sHpgl " + sHPGL);

                CADTitleBlock cadTitleBlock = buildTitleBlock(sCADDefIds, ListOfFiles, iCpt);

                sPrinterCmd = printServer(sPrinterCmd, sViewId, sFormat, sFileName, sPaperFormat, cadTitleBlock, sHPGL);
                // FPDM Add Start : JFA 2005/07/26 RFC 3783
                sHPGL = "";
                // FPDM Add End : JFA 2005/07/26 RFC 3783
            }
        } else {
            print(true, true);
        }
        // new DisplayMessage("Information", "Plot has been printed.", "Information", "OK");
        agGUI.displayResultMessage("Information", "Plot has been printed.", "OK");

    }

    @SuppressWarnings("deprecation")
    private void print(boolean isWindows, boolean doPrint) throws Exception {

        displayProgressMessage("Retrieving local temporary directory ...");

        // --------------------------------------------------------------------
        // Retrieve local temp directory
        // --------------------------------------------------------------------

        sLocalTempDirectory = getPrintTempDirectory(isWindows);

        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/print: Start");
        // ============================
        // Retrieve Print command line
        String sPrinterCmd = "";
        if (!isWindows && doPrint) {
            displayProgressMessage("Retrieve list of printers ...");
            sPrinterCmd = Printer.selectLocalPrinterAndBuildCommand();
            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/print: sPrinterCmd: " + sPrinterCmd);
        }

        // ============================
        // Title Block Construction

        // ----------------------------------------------------
        // Split inputted parameters
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/print: sObjectId " + sObjectId);
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/print: listOfFiles " + listOfFiles);
        String[] sCADDefIds = StringUtil.split(sObjectId, Parameters.DELIM);
        String[] ListOfFiles = StringUtil.split(listOfFiles, Parameters.DELIM);
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/print: ListOfFiles : ", ListOfFiles);
        String[] ListOfScripts = StringUtil.split(sListScript, Parameters.DELIM);
        
        // FPDM Add Start : JFA 2005/07/26 RFC 3783
        boolean bListPropertyExist = false;
        StringTokenizer tokenHPGL = null;

        if (sListHPGLProperty != null && !"".equals(sListHPGLProperty)) {
            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/print: sListHPGLProperty : " + sListHPGLProperty);
            tokenHPGL = new StringTokenizer(sListHPGLProperty, "|");
            bListPropertyExist = true;
        }
        String sHPGL = "";
        // FPDM Add Start : JFA 2005/07/26 RFC 3783

        for (int iCpt = 0; iCpt < (ListOfFiles.length / 5); iCpt++) {
            String sViewId = ListOfFiles[(5 * iCpt)];
            sFormat = ListOfFiles[(5 * iCpt) + 1];
            String sFileName = ListOfFiles[(5 * iCpt) + 2];
            String sPaperFormat = ListOfFiles[(5 * iCpt) + 3];
            //String sViewName = ListOfFiles[(5 * iCpt) + 4];
            sScript = ListOfScripts[iCpt];

            // FPDM Add Start : JFA 2005/07/26 RFC 3783
            if (bListPropertyExist && tokenHPGL.hasMoreTokens()) {
                sHPGL = tokenHPGL.nextToken();
                DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/print: sHPGL : " + sHPGL);
            }
            // FPDM Add End : JFA 2005/07/26 RFC 3783
            // ----------------------------------------------------
            // Retrieve title blocks from servlet
            // Retrieve title blocks from servlet
            CADTitleBlock cadTitleBlock = buildTitleBlock(sCADDefIds, ListOfFiles, iCpt);
            print(isWindows, doPrint, sPrinterCmd, sViewId, sFormat, sFileName, sPaperFormat, cadTitleBlock, sHPGL);

            // FPDM Add Start : JFA 2005/07/26 RFC 3783
            sHPGL = "";
            // FPDM Add End : JFA 2005/07/26 RFC 3783
        }

        if (doPrint) {
            // fnew DisplayMessage("Information", "Plot has been printed.", "Information", "OK");
            agGUI.displayResultMessage("Information", "Plot has been printed.", "OK");
        } else {
            // display a success message and let the user close the window.
            agGUI.displayResultMessage("Information", "Plot has been openened in the viewer.", "OK");
            this.closeAppletWindow();
        }
    }

    private CADTitleBlock buildTitleBlock(String[] sCADDefIds, String[] ListOfFiles, int iCpt) throws Exception {
        // ----------------------------------------------------
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/buildTitleBlock: sCADDefIds : ", sCADDefIds);
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/buildTitleBlock: sScript : " + sScript);
        // Define Title block input parameters
        CADTitleBlock cadTitleBlock = null;
        if (!"Nouse".equalsIgnoreCase(sScript) && !"NoTitleBlock".equalsIgnoreCase(sScript)) {
            displayProgressMessage("Retrieve title block file #" + iCpt + " ...");
            String sCADId = sCADDefIds[Math.min(iCpt, sCADDefIds.length - 1)];
            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/buildTitleBlock: sCADId : " + sCADId);

            String sViewId = ListOfFiles[(5 * iCpt)];
            sFormat = ListOfFiles[(5 * iCpt) + 1];
            String sFileName = ListOfFiles[(5 * iCpt) + 2];
            String sPaperFormat = ListOfFiles[(5 * iCpt) + 3];
            String sViewName = ListOfFiles[(5 * iCpt) + 4];

            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/buildTitleBlock: sViewId " + sViewId);
            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/buildTitleBlock: sFormat " + sFormat);
            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/buildTitleBlock: sFileName " + sFileName);
            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/buildTitleBlock: sPaperFormat " + sPaperFormat);
            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/buildTitleBlock: sViewName " + sViewName);

            cadTitleBlock = new CADTitleBlock(sCADId, sViewId, sPaperFormat, sLanguage, sBonProd, sHistoricLevel, sNbOfFolios, sAdditionalInfo, null, sNissanOrRenault);

            // ----------------------------------------------------
            // Retrieve title blocks from servlet
            CADTitleBlock[] a_TitleBlocks = CADTitleBlock.retrieveTitleBlockFromServer(new CADTitleBlock[] {cadTitleBlock}, sServerTempDirectory, MAIN_SERVER_BASE_URL);
            cadTitleBlock = a_TitleBlocks[0];
        }
        return cadTitleBlock;
    }

    private void print(boolean isWindows, boolean doPrint, String sPrinterCmd, String sViewId, String sFormat, String sFileName, String sPaperFormat, CADTitleBlock cadTitleBlock, String sHpgl)
            throws Exception {

        // --------------------------
        // CREATE TITLE BLOCK FILES ON LOCAL TEMP DIRECTORY
        File titleBlockFile = null;
        if (!"Nouse".equalsIgnoreCase(sScript) && !"NoTitleBlock".equalsIgnoreCase(sScript)) {
            cadTitleBlock.createTitleBlockFile(sLocalTempDirectory, sFileName + "Car.plt");
            titleBlockFile = new File(sLocalTempDirectory, sFileName + "Car.plt");
        }

        // --------------------------
        // DO THE CHECKOUT OF FILES
        File checkedOutFile = null;
        if (!bTitleBlockOnly) {
            displayProgressMessage("Checkout file ..." + sFileName);

            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/print: retrieving file  " + sFileName);
            bLock = false;
            checkedOutFile = Checkouter.checkoutFile(sLocalTempDirectory, sViewId, sFileName, sFormat, bLock, sObjectId, "2DViewable", MAIN_SERVER_BASE_URL);

            // DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/print: Exit of checkoutFiles " + sFileName);

            // ============================
        }

        // FPDM ADD Start JFA 28/08/2006 RFC 2528
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/print: checkedOutFile " + checkedOutFile);
        if (checkedOutFile != null) {
            if (checkedOutFile.getAbsolutePath().indexOf(TIF_EXTENSION) == -1) {
                System.setProperty("faurecia.file.extension", PLT_EXTENSION);
            } else {
                System.setProperty("faurecia.file.extension", TIF_EXTENSION);
            }
        } else {
            System.setProperty("faurecia.file.extension", TIF_EXTENSION);
        }
        // FPDM ADD End JFA 28/08/2006 RFC 2528

        // Retrieve Command Line for printing
        String sPrinterCmdComplete = null;
        if ("Nouse".equalsIgnoreCase(sScript)) {
            if (bTitleBlockOnly) {
                throw new Exception("IMPOSSIBLE CASE (script=Nouse and TitleBlockOnly)");
            }
            // suppression .Z dans name of file
            int index1 = checkedOutFile.getAbsolutePath().lastIndexOf(".Z");
            String WithoutZ = checkedOutFile.getAbsolutePath();
            if (index1 >= 0) {
                // DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/print: Suppresion .Z " + checkedOutFile.getName());
                WithoutZ = WithoutZ.substring(0, index1);
                File source = new File(checkedOutFile.getAbsolutePath());
                File destination = new File(WithoutZ);
                source.renameTo(destination);
                // DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/print: New name after Suppresion .Z " + WithoutZ);
            }
            sPrinterCmdComplete = getViewCommand(isWindows).concat(" ").concat(WithoutZ);

        } else if (bTitleBlockOnly) {
            sPrinterCmdComplete = Local_OS_Information.buildPrintCommand(sPrinterCmd, titleBlockFile.getAbsolutePath(), sNbCopy, sScale, sColor, sPaperFormat, bTitleBlockOnly);

        } else if ("NoTitleBlock".equalsIgnoreCase(sScript)) {
            sPrinterCmdComplete = Local_OS_Information.buildPrintCommand(sPrinterCmd, checkedOutFile.getAbsolutePath(), sNbCopy, sScale, sColor, sPaperFormat, false);
        } else {
            displayProgressMessage("Merge CAD file with title block ...");
            // FPDM Add Start : JFA 2005/07/26 RFC 3783
            if (!"".equals(sHpgl)) {
                if (bListHPGL2.size() == 0) {
                    bListHPGL2.add(new Boolean(sHpgl).booleanValue());
                } else {
                    bListHPGL2.set(0, new Boolean(sHpgl).booleanValue());
                }
            }
            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/print: bListHPGL2.get(0) " + bListHPGL2.get(0));
            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/print: checked out file " + checkedOutFile.getAbsolutePath());
            // FPDM Add End : JFA 2005/07/26 RFC 3783
            File mergedFile = HPGLMerger.mergeHpgl(checkedOutFile.getAbsolutePath(), titleBlockFile.getAbsolutePath(), cadTitleBlock.getXOffset(), cadTitleBlock.getYOffset(), bListHPGL2.get(0));
            checkedOutFile.delete();
            titleBlockFile.delete();

            if (doPrint) {
                sPrinterCmdComplete = Local_OS_Information.buildPrintCommand(sPrinterCmd, mergedFile.getAbsolutePath(), sNbCopy, sScale, sColor, sPaperFormat, bTitleBlockOnly);
            } else {
                sPrinterCmdComplete = getViewCommand(isWindows) + " " + mergedFile.getAbsolutePath();
                DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/print: sPrinterCmdComplete " + sPrinterCmdComplete);
            }
        }
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/print: sPrinterCmdComplete: " + sPrinterCmdComplete);
        if (sPrinterCmdComplete != null) {
            displayProgressMessage("Print the plot ...");
            try {
                Local_OS_Information.executeCommand(sPrinterCmdComplete, 2000, false);
            } catch (Exception e) {
                DebugUtil.debug(DebugUtil.ERROR, CLASSNAME, "/print: Exception launched by printing command : " + e.getMessage());
            }
        } else {
            // new DisplayMessage("Information", "No Association for Plot file has been found.", "Information", "OK");
            agGUI.displayResultMessageAndContinue("Information", "No Association for Plot file has been found.", "OK");
            // --------------------------------------------------------------------
            // REDIRECTION OF THE OPENER JSP PAGE
            System.out.println("before getDocumentBase()");
        }

    }

    @SuppressWarnings("unchecked")
    private String printServer(String sPrinterCmd, String sViewId, String sFormat, String sFileName, String sPaperFormat, CADTitleBlock cadTitleBlock, String sHpgl) throws Exception {
        // ========================================================
        // Checkout all files from FCS and memorize FCS URL
        // ========================================================
        Hashtable<String, Object>[] a_TitleBlocksToSendToFCS = new Hashtable[1];

        // --------------------------
        // DO THE CHECKOUT OF FILES
        String sCheckedOutFilePath = "";
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/printServer: bTitleBlockOnly:  " + bTitleBlockOnly);

        if (!bTitleBlockOnly) {
            displayProgressMessage("Checkout file ..." + sFileName);

            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/printServer: retrieving file  " + sFileName);
            bLock = false;
            sCheckedOutFilePath = Checkouter.checkoutFileOnServer(sViewId, sFileName, sFormat, bLock, sObjectId, "2DViewable", MAIN_SERVER_BASE_URL);

            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/printServer: Exit of checkoutFilesInServer " + sCheckedOutFilePath);
        }
        // FPDM ADD Start JFA 25/01/2006 RFC 2903
        // retrieve the FCS if it has not been initialized
        else {
            if (Checkouter.urlOfLastFCSCall.equals("")) {
                BusObject bo = new BusObject(sViewId, "2DViewable", sFileName, sFormat, bLock, sObjectId);
                BusObject[] a_bo = new BusObject[1];
                a_bo[0] = bo;
                a_bo = (BusObject[]) Checkouter.getTicketsForCheckout(a_bo, MAIN_SERVER_BASE_URL);
                String sMatrixFCSServletURL = a_bo[0].getFCSServletURL();

                int iIndex = sMatrixFCSServletURL.indexOf(Checkouter.sShortURLForMxFCSCheckoutServlet);
                String sFaureciaFCSServletURL;
                if (iIndex > 0)
                    sFaureciaFCSServletURL = sMatrixFCSServletURL.substring(0, iIndex);
                else
                    sFaureciaFCSServletURL = sMatrixFCSServletURL;
                if (!sFaureciaFCSServletURL.endsWith("/")) {
                    sFaureciaFCSServletURL += "/";
                }
                sFaureciaFCSServletURL += Checkouter.FAURECIA_eFCS_SERVLET_NAME;
                Checkouter.urlOfLastFCSCall = sFaureciaFCSServletURL;
                DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/printServer: sFaureciaFCSServletURL: " + sFaureciaFCSServletURL);
            }
        }
        // FPDM ADD End JFA 25/01/2006 RFC 2903

        // ============================
        // Retrieve list of printers from localhost on Unix
        if (sPrinterCmd == null || sPrinterCmd.length() == 0) {
            displayProgressMessage("Retrieve list of printers ...");

            sPrinterCmd = Printer.selectServerPrinterAndBuildCommand(Checkouter.urlOfLastFCSCall);

            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/printServer: sPrinterCmd: " + sPrinterCmd);
        }

        // ============================
        // SEND TITLE BLOCKS TO THE FCS SERVER FOR MERGING AND PRINT
        a_TitleBlocksToSendToFCS[0] = new Hashtable<String, Object>();
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/printServer: sCheckedOutFilePath   " + sCheckedOutFilePath);
        a_TitleBlocksToSendToFCS[0].put("FILE_PATH", sCheckedOutFilePath);
        if (!"NoTitleBlock".equalsIgnoreCase(sScript)) {
            DebugUtil.debug(DebugUtil.DEBUG, "a_TitleBlocks[0] " + cadTitleBlock);
            a_TitleBlocksToSendToFCS[0].put("TITLE_BLOCK", cadTitleBlock);
        }
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/printServer: sPrinterCmd           " + sPrinterCmd);
        a_TitleBlocksToSendToFCS[0].put("PRINTER_COMMAND", sPrinterCmd);
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/printServer: sNbCopy               " + sNbCopy);
        a_TitleBlocksToSendToFCS[0].put("NB_COPY", sNbCopy);
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/printServer: sScale                " + sScale);
        a_TitleBlocksToSendToFCS[0].put("SCALE", sScale);
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/printServer: sColor                " + sColor);
        a_TitleBlocksToSendToFCS[0].put("COLOR", sColor);
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/printServer: sPaperFormat        " + sPaperFormat);
        a_TitleBlocksToSendToFCS[0].put("DRAWING_FORMAT", sPaperFormat);
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/printServer: Checkouter.urlOfLastFCSCall " + Checkouter.urlOfLastFCSCall);

        // FPDM Add Start : JFA 2005/07/26 RFC 3783
        if (!"".equals(sHpgl)) {
            if (bListHPGL2.size() == 0) {
                bListHPGL2.add(new Boolean(sHpgl).booleanValue());
            } else {
                bListHPGL2.set(0, new Boolean(sHpgl).booleanValue());
            }
        }
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/print: bListHPGL2.get(0) " + bListHPGL2.get(0));
        // FPDM Add End : JFA 2005/07/26 RFC 3783

        if (!"NoTitleBlock".equalsIgnoreCase(sScript)) {
            AppletServletCommunication.requestServerTask(Checkouter.urlOfLastFCSCall + "/mergeAndPrintOnServer?TitleBlockOnly=" + String.valueOf(bTitleBlockOnly) + "&bHPGL2=" + String.valueOf(bListHPGL2.get(0))
                    + "&debug=" + System.getProperty("faurecia.debugging"), a_TitleBlocksToSendToFCS);
        } else {
            AppletServletCommunication.requestServerTask(
                    Checkouter.urlOfLastFCSCall + "/PrintOnServer?TitleBlockOnly=" + String.valueOf(bTitleBlockOnly) + "&debug=" + System.getProperty("faurecia.debugging"), a_TitleBlocksToSendToFCS);
        }

        return sPrinterCmd;
    }

    private String getPrintTempDirectory(boolean isWindows) throws Exception {
        String sLocalTempDirectory = StringUtil.returnNullIfEmpty(this.getParameter(Parameters.LOCAL_REPERTORY));
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/getPrintTempDirectory: Retrieve Local Temp Directory.");
        if (isWindows) {
            sLocalTempDirectory = StringUtil.returnDefaultIfEmpty(sLocalTempDirectory, "c:/Temp");
        } else {
            sLocalTempDirectory = Local_OS_Information.getUnixEnv("MAGELLAN_TEMP");
            if (sLocalTempDirectory == null || "".equals(sLocalTempDirectory)) {
                sLocalTempDirectory = "/tmp";
            }
        }
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/getPrintTempDirectory: Local Temp Directory = " + sLocalTempDirectory);
        return sLocalTempDirectory;
    }

    private String getViewCommand(boolean isWindows) throws Exception {
        if (isWindows) {
            return Local_OS_Information.getViewCommand(30000);
        }
        return Local_OS_Information.getUnixEnv("MAGELLAN_VIEW");
    }

    private void downloadMultipleTIFF(boolean isWindows) throws Exception {
        displayProgressMessage("Retrieving local temporary directory ...");
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/downloadMultipleTIFF sDownloadTifCommand: " + sDownloadTifCommand);

        if (sDownloadTifCommand.equalsIgnoreCase("true")) {
            downloadAllTIFFs(isWindows, false);
            return;
        }
        // --------------------------------------------------------------------
        // Retrieve local temp directory
        // --------------------------------------------------------------------

        displayProgressMessage("The local directory is:" + sLocalTempDirectory);

        // ----------------------------------------------------
        // Split inputted parameters
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/downloadMultipleTIFF: sObjectId " + sObjectId);
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/downloadMultipleTIFF: listOfFiles " + listOfFiles);
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/downloadMultipleTIFF: sCadObjectsTNR " + sCadObjectsTNR);
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/downloadMultipleTIFF: sDownloadTifCommand " + sDownloadTifCommand);
        // String[] sCADDefIds = StringUtil.split(sObjectId, Parameters.DELIM);

        String[] ListOfFiles = StringUtil.split(listOfFiles, Parameters.DELIM);
        // String[] aListCurrentState = StringUtil.split(sCurrentState, Parameters.DELIM);
        // String[] aListCADObjectsTNR = StringUtil.split(sCadObjectsTNR, Parameters.DELIM);

        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/downloadMultipleTIFF: ListOfFiles : ", ListOfFiles);

        for (int iCpt = 0; iCpt < (ListOfFiles.length / 5); iCpt++) {
            // String sCADId = sCADDefIds[Math.min(iCpt, sCADDefIds.length - 1)];
            String sViewId = ListOfFiles[(5 * iCpt)];
            sFormat = ListOfFiles[(5 * iCpt) + 1];
            String sFileName = ListOfFiles[(5 * iCpt) + 2];
            // String sPaperFormat = ListOfFiles[(5 * iCpt) + 3];
            // String sViewName = ListOfFiles[(5 * iCpt) + 4];
            // ----------------------------------------------------
            // Retrieve title blocks from servlet
            // CADTitleBlock cadTitleBlock = buildTitleBlock(sCADDefIds, ListOfFiles, iCpt);
            Checkouter.checkoutFile(sLocalTempDirectory, sViewId, sFileName, sFormat, false, sObjectId, "2DViewable", MAIN_SERVER_BASE_URL);
        }
        agGUI.displayResultAndExit("Download finished.");

    }

    // FPDM ADD Start : PRI 12/01/2006 RFC 2034
    private void checkoutMultipleTIFF(boolean isWindows) throws Exception {
        displayProgressMessage("Retrieving local temporary directory ...");
        if (sDownloadTifCommand.equalsIgnoreCase("true")) {
            downloadAllTIFFs(isWindows, true);
            return;
        }

        // --------------------------------------------------------------------
        // Retrieve local temp directory
        // --------------------------------------------------------------------

        displayProgressMessage("The local directory is:" + sLocalTempDirectory);

        // ----------------------------------------------------
        // Split inputed parameters
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkoutMultipleTIFF: sObjectId " + sObjectId);
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkoutMultipleTIFF: listOfFiles " + listOfFiles);
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkoutMultipleTIFF: sCurrentState " + sCurrentState);
        String[] sCADDefIds = StringUtil.split(sObjectId, Parameters.DELIM);

        String[] ListOfFiles = StringUtil.split(listOfFiles, Parameters.DELIM);

        String[] aListCurrentState = StringUtil.split(sCurrentState, Parameters.DELIM);
        // String[] aListCADObjectsTNR = StringUtil.split(sCadObjectsTNR, Parameters.DELIM);

        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkoutMultipleTIFF: ListOfFiles : ", ListOfFiles);

        for (int iCpt = 0; iCpt < (ListOfFiles.length / 5); iCpt++) {
            // String sCADId = sCADDefIds[Math.min(iCpt, sCADDefIds.length - 1)];
            String sViewId = ListOfFiles[(5 * iCpt)];
            sFormat = ListOfFiles[(5 * iCpt) + 1];
            String sFileName = ListOfFiles[(5 * iCpt) + 2];
            // String sViewName = ListOfFiles[(5 * iCpt) + 4];
            // ----------------------------------------------------
            // Retrieve title blocks from servlet
            CADTitleBlock cadTitleBlock = buildTitleBlock(sCADDefIds, ListOfFiles, iCpt);

            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkoutMultipleTIFF: sCurrentState" + iCpt + ": " + aListCurrentState[iCpt]);

            checkoutTiffWithBanner(isWindows, sViewId, sFormat, sFileName, cadTitleBlock, aListCurrentState[iCpt]);
        }
        agGUI.displayResultAndExit("Checkout finished.");
    }

    @SuppressWarnings("unused")
    private void checkoutTiffWithBanner(boolean isWindows, String sViewId, String sFormat, String sFileName, CADTitleBlock cadTitleBlock, String sCurrentCADDefState) throws Exception {
        // --------------------------
        // DO THE CHECKOUT OF FILES
        File checkedOutFile = null;
        displayProgressMessage("Checkout file ..." + sFileName);

        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkoutTiffWithBanner: retrieving file  " + sFileName);
        bLock = false;
        checkedOutFile = Checkouter.checkoutTIFWithFile(sLocalTempDirectory, sViewId, sFileName, sFormat, bLock, sObjectId, "2DViewable", MAIN_SERVER_BASE_URL, sUserName, sCurrentCADDefState, sDate);

        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkoutTiffWithBanner: exit of checkoutFiles " + sFileName);
    }

    // FPDM ADD End : PRI 12/01/2006 RFC 2034

    // FPDM ADD Start : JFA 21/02/2006 Change
    private String extractFilename(Vector vCADCptFiles) throws Exception {

        String fileName = "";
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkout: call method extractFilename() vCADComponentFiles : " + vCADCptFiles);

        try {
            if (vCADCptFiles.size() == 1) {
                fileName = ((BusObject) vCADCptFiles.get(0)).getFileName();
            } else {
                String defaultFileName = "";
                String sTempFileName = "";
                Vector<String> vCADFileName = new Vector<String>();

                for (Enumeration eEnum = vCADCptFiles.elements(); eEnum.hasMoreElements();) {
                    sTempFileName = ((BusObject) eEnum.nextElement()).getFileName();
                    DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/extractFilename: sTempFileName: " + sTempFileName);

                    boolean tmpCADHasNumber = false;

                    for (int j = 0; j < 10; j++) {
                        String pattern = "_" + j;
                        if (sTempFileName.indexOf(pattern) != -1) {
                            tmpCADHasNumber = true;
                        }
                    }

                    if (!tmpCADHasNumber) {
                        vCADFileName.addElement(sTempFileName);
                        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/extractFilename:  Add file sTempFileName: " + sTempFileName + " -- vCADFileName.size(): " + vCADFileName.size());
                    }

                    // return the shortest filename in case there is no valid filename
                    if (sTempFileName.length() < defaultFileName.length() || defaultFileName.length() == 0) {
                        defaultFileName = sTempFileName;
                    }
                }

                if (vCADFileName.size() == 0) {
                    DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/extractFilename: No valid filename found, return the shortest filename");
                    fileName = defaultFileName;
                } else {
                    String tempStr = "";
                    for (Enumeration enumFinal = vCADFileName.elements(); enumFinal.hasMoreElements();) {
                        String sTempName = (String) enumFinal.nextElement();
                        if (sTempName.length() < tempStr.length() || tempStr.length() == 0) {
                            tempStr = sTempName;
                        }
                    }
                    DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/extractFilename: size = " + vCADFileName.size() + " -- valid filename: " + tempStr);
                    fileName = tempStr;
                }
            }
        } catch (Exception e) {
            DebugUtil.debug(DebugUtil.ERROR, CLASSNAME, "/extractFilename: An error occured when retrieving the filename... Cancel action");
            throw e;
        }

        DebugUtil.debug(DebugUtil.INFO, CLASSNAME, "/extractFilename: returned filename: " + fileName);

        return fileName;
    }

    // FPDM ADD End : JFA 21/02/2006 Change

    // FPDM ADD Start : PRI 12/01/2006 RFC 2034
    private void downloadAllTIFFs(boolean isWindows, boolean bIsBanner) throws Exception {
        displayProgressMessage("Retrieving local temporary directory ...");

        // --------------------------------------------------------------------
        // Retrieve local temp directory
        // --------------------------------------------------------------------

        displayProgressMessage("The local directory is:" + sLocalTempDirectory);

        // ----------------------------------------------------
        // Split inputed parameters
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/downloadAllTIFFs: sObjectId " + sObjectId);
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/downloadAllTIFFs: listOfFiles " + listOfFiles);
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/downloadAllTIFFs: sCurrentState " + sCurrentState);
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/downloadAllTIFFs: sCadObjectsTNR " + sCadObjectsTNR);
        if (sObjectId == null) {
            sObjectId = "";
        }
        // String[] sCADDefIds = StringUtil.split(sObjectId, Parameters.DELIM);

        String[] ListOfFiles = StringUtil.split(listOfFiles, Parameters.DELIM);

        String[] aListCurrentState = StringUtil.split(sCurrentState, Parameters.DELIM);
        String[] aListCADObjectsTNR = StringUtil.split(sCadObjectsTNR, Parameters.DELIM);

        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkoutMultipleTIFF: ListOfFiles : ", ListOfFiles);

        Checkouter.downloadAllTIFFs(sLocalTempDirectory, ListOfFiles, bLock, sObjectId, "2DViewable", MAIN_SERVER_BASE_URL, sUserName, aListCurrentState, sDate, aListCADObjectsTNR, bIsBanner);
        agGUI.displayResultAndExit("Checkout finished.");
    }

}
