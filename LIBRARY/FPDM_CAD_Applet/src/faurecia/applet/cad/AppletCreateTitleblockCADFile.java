package faurecia.applet.cad;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import faurecia.applet.FaureciaApplet;
import faurecia.util.AbstractFile;
import faurecia.util.AppletServletCommunication;
import faurecia.util.BusObject;
import faurecia.util.DebugUtil;
import faurecia.util.StringUtil;
import faurecia.util.TranslationUtil;
import faurecia.util.cad.CADTitleBlock;
import fpdm.applet.util.FPDMAppletUtils;
import fpdm.applet.cad.Parameters;

/**
 * 
 * Applet that should be able to checkout TitleBlock cad file <br>
 * <br>
 * This class just retrieve the parameter given to the applet, <br>
 * and call createTitleBlockCadFile to create the .cad file<br>
 * <br>
 * 
 * @author mmiraoui
 */
public class AppletCreateTitleblockCADFile extends FaureciaApplet {

    protected String sCloseWindowURL = "engineeringcentral/closeWindow.jsp";

    private final static String CLASSNAME = "AppletCreateTitleblockCADFile";

    public static String sObjectId;

    // private String sLocalTempDirectory;

    private String sServerTempDirectory;

    private String integrationName;

    private String sCADObjectIds;

    private String sCADObjectTypes;

    private String sCADObjectNames;

    private String sCADObjectRevisions;

    private String sCADDefinitionIds;

    private String sCADDefinitionTypes;

    private String sCADDefinitionNames;

    private String sCADDefinitionRevisions;

    private String sCADDefinitionListOfFiles;

    private String sHistoricLevel;

    private String sBonProd;

    private String sNissanOrRenault;

    private String sAdditionalInfo;

    private String sLanguage;

    private String debugMode;

    private String mcsBaseURL;

    public AppletCreateTitleblockCADFile() {
        super();
        mcsBaseURL = "";
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
    public void init() {
        // super.init();
        mcsBaseURL = getParameter("VIRTUALPATH");
        mcsBaseURL = mcsBaseURL + "/";
        System.out.println("Main server base URL: " + mcsBaseURL);
        Parameters.getAppletUI().setApplet(this);
        Parameters.setMainServerBaseURL(mcsBaseURL);
        TranslationUtil.init(this);
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
        AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {

                DebugUtil.debug(DebugUtil.INFO, CLASSNAME, "/start : Start Applet AppletCreateTitleblockCADFile");

                Parameters.getAppletUI().setApplet(AppletCreateTitleblockCADFile.this);

                String inOutStr = AppletCreateTitleblockCADFile.this.getParameter("inOut");
                sObjectId = StringUtil.returnNullIfEmpty(AppletCreateTitleblockCADFile.this.getParameter(Parameters.OBJECT_ID));
                // sLocalTempDirectory = StringUtil.returnNullIfEmpty(AppletCreateTitleblockCADFile.this.getParameter(Parameters.LOCAL_REPERTORY));
                sServerTempDirectory = StringUtil.returnNullIfEmpty(AppletCreateTitleblockCADFile.this.getParameter(Parameters.SERVER_REPERTORY));

                sLanguage = AppletCreateTitleblockCADFile.this.getParameter("ACCEPTLANGUAGE");
                integrationName = AppletCreateTitleblockCADFile.this.getParameter("integrationName");
                sCADObjectIds = AppletCreateTitleblockCADFile.this.getParameter("sCADObjectIds");
                sCADObjectTypes = AppletCreateTitleblockCADFile.this.getParameter("sCADObjectTypes");
                sCADObjectNames = AppletCreateTitleblockCADFile.this.getParameter("sCADObjectNames");
                sCADObjectRevisions = AppletCreateTitleblockCADFile.this.getParameter("sCADObjectRevisions");
                sCADDefinitionIds = AppletCreateTitleblockCADFile.this.getParameter("sCADDefinitionIds");
                sCADDefinitionTypes = AppletCreateTitleblockCADFile.this.getParameter("sCADDefinitionTypes");
                sCADDefinitionNames = AppletCreateTitleblockCADFile.this.getParameter("sCADDefinitionNames");
                sCADDefinitionRevisions = AppletCreateTitleblockCADFile.this.getParameter("sCADDefinitionRevisions");
                sCADDefinitionListOfFiles = AppletCreateTitleblockCADFile.this.getParameter("sCADDefinitionListOfFiles");

                sHistoricLevel = AppletCreateTitleblockCADFile.this.getParameter("sHistoricLevel");
                sBonProd = AppletCreateTitleblockCADFile.this.getParameter("sBonProd");
                sAdditionalInfo = "|" + AppletCreateTitleblockCADFile.this.getParameter("sAdditionalInfo");
                sNissanOrRenault = AppletCreateTitleblockCADFile.this.getParameter("sNissanOrRenault");

                debugMode = AppletCreateTitleblockCADFile.this.getParameter(Parameters.DEBUG_LEVEL);

                if (debugMode.equals("ON")) {
                    DebugUtil.initDebugger(true);
                    System.setProperty("faurecia.debugging", "ON");
                } else {
                    DebugUtil.initDebugger(false);
                    System.setProperty("faurecia.debugging", "OFF");
                }

                try {
                    DebugUtil.debug(DebugUtil.INFO, CLASSNAME, "/start : Call method: " + inOutStr);

                    createTitleBlockCadFile(integrationName, sServerTempDirectory, sCADObjectIds, sCADObjectTypes, sCADObjectNames, sCADObjectRevisions, sCADDefinitionIds, sCADDefinitionTypes,
                            sCADDefinitionNames, sCADDefinitionRevisions, sCADDefinitionListOfFiles, sHistoricLevel, sBonProd, sAdditionalInfo, sNissanOrRenault);

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

        });
    }

    public void closeAppletWindow() {

        DebugUtil.debug(DebugUtil.INFO, CLASSNAME, "in method: closeAppletWindow");

        try {
            URL closeWindow = new URL(mcsBaseURL + sCloseWindowURL);
            // DebugUtil.debug(DebugUtil.INFO, "PAGE REDIRECTION", closeWindow.toString());
            this.getAppletContext().showDocument(closeWindow);
        } catch (MalformedURLException e) {
        }
    }

    private String createTitleBlockCadFile(String integrationName, String serverTempDirectory, String cadId, String cadType, String cadName, String cadRevision, String cadDefId, String cadDefType,
            String cadDefName, String cadDefRevision, String listOfFiles, String sHistoricLevel, String sBonProd, String sAdditionalInfo, String sNissanOrRenault) {
        try {
            Hashtable mxInfos = buildMxInfoCheckout(cadId, cadType, cadName, cadRevision, cadDefId, cadDefType, cadDefName, cadDefRevision, listOfFiles);
            for (Iterator iterator = mxInfos.values().iterator(); iterator.hasNext();) {
                Hashtable mxInfo = (Hashtable) iterator.next();
                DebugUtil.debug(DebugUtil.DEBUG, "AppletCreateTitleblockCADFile:createTitleBlockCadFile", "mxInfo=" + mxInfo);
                checkoutTitleBlock(integrationName, mxInfo, serverTempDirectory, sHistoricLevel, sBonProd, sAdditionalInfo, sNissanOrRenault);
            }
            this.closeAppletWindow();
            return "TRUE";
        } catch (Exception e) {
            e.printStackTrace();
            String sMessage = e.getMessage();
            return sMessage;
        }
    }

    private void checkoutTitleBlock(final String integrationName, final Hashtable mxInfo, final String sServerTempDirectory, final String sHistoricLevel, final String sBonProd,
            final String sAdditionalInfo, final String sNissanOrRenault) throws Exception {
        try {
            String sCADDefinitionId = (String) mxInfo.get("id");
            String sCADComponentId = (String) mxInfo.get("cadid");
            String sCADDefinitionName = (String) mxInfo.get("name");
            String sCADDefinitionRev = (String) mxInfo.get("revision");
            String sCADComponentName = (String) mxInfo.get("cadname");
            String sCADComponentRev = (String) mxInfo.get("cadrevision");

            String listOfFiles = (String) mxInfo.get("listOfFiles");
            String[] ListOfFiles = StringUtil.split(listOfFiles, Parameters.DELIM);
            String sNbOfFolios = String.valueOf(ListOfFiles.length);

            // ===================================
            // == BUILD LST FILE ==
            // ===================================
            String sLSTFileName = sCADComponentName + sCADComponentRev + ".lst";
            DebugUtil.debug(DebugUtil.DEBUG, "AppletCreateTitleblockCADFile:checkoutTitleBlock", "Name of the LST file : " + sLSTFileName);

            BusObject boCAD = new BusObject(sCADComponentId, "CAD Component", sCADDefinitionName, sCADDefinitionRev, sLSTFileName);
            boCAD.setToConnectId(sCADDefinitionId);

            // ===================================
            // ASK THE SERVER FOR A NEW LST FILE
            // ===================================
            String sURL2 = Parameters.getMainServerBaseURL() + Parameters.FAURECIA_eMCS_SERVLET_NAME + "/retrieveLSTFile";
            sURL2 += "?debug=ON";
            sURL2 += "&time=" + System.currentTimeMillis();

            BusObject[] a_BusObjects = new BusObject[1];
            a_BusObjects[0] = boCAD;
            Object[] retObjects = AppletServletCommunication.requestServerTask(sURL2, a_BusObjects);
            String inputDir = "";
            // System.out.println("integrationName 4" +integrationName ) ;
            if (!"MxUG".equals(integrationName)) {
                inputDir = FPDMAppletUtils.getCatiaV5TbProperty("INPUTDIR");
            } else {
                inputDir = FPDMAppletUtils.getUGTbProperty("INPUTDIR");
            }
            ((AbstractFile) retObjects[0]).createFileOnSystemDrive(inputDir);

            // ===================================
            // ASK THE SERVER FOR A NEW TITLE BLOCK FILE
            // ===================================
            String sDebugTrace = "== BUILD TITLE BLOCK FILES ==";
            DebugUtil.debug(DebugUtil.INFO, sDebugTrace);
            /*
             * FPDM REPLACE START : MCE 2005/09/06 : The title block for Catia V5 is always like checkout for Copy String sTxtOnly; if (bLock) { sTxtOnly = "checkout"; } else { sTxtOnly = "copy"; } BY
             */
            String sTxtOnly = "copy";
            // FPDM REPLACE END : MCE 2005/09/06

            // ----------------------------------------------------
            // Define Title block input parameters
            DebugUtil.debug(DebugUtil.DEBUG, "ListOfFiles : ", ListOfFiles);
            for (int iCpt = 0; iCpt < (ListOfFiles.length - 2); iCpt = (iCpt + 3)) {
                DebugUtil.debug(DebugUtil.DEBUG, "AppletCreateTitleblockCADFile:checkoutTitleBlock", "sObjectId : " + sObjectId);
                String sViewId = ListOfFiles[iCpt];
                DebugUtil.debug(DebugUtil.DEBUG, "AppletCreateTitleblockCADFile:checkoutTitleBlock", "sViewId : " + sViewId);
                String sFormat = ListOfFiles[iCpt + 1];
                DebugUtil.debug(DebugUtil.DEBUG, "AppletCreateTitleblockCADFile:checkoutTitleBlock", "sFormat : " + sFormat);
                String sFileName = ListOfFiles[iCpt + 2];
                DebugUtil.debug(DebugUtil.DEBUG, "AppletCreateTitleblockCADFile:checkoutTitleBlock", "sFileName : " + sFileName);
                showStatus("Construct title block for file \'" + sFileName + "\'...");

                String type = (String) mxInfo.get("type");
                DebugUtil.debug(DebugUtil.DEBUG, "AppletCreateTitleblockCADFile:checkoutTitleBlock", "type : " + type);

                String sEndFileName = sFileName.substring(sFileName.lastIndexOf("_"), sFileName.lastIndexOf("."));
                sEndFileName = sEndFileName.replaceAll("SHEET", "");
                sFileName = "CAD_" + sCADComponentName + sEndFileName + ".cad";
                DebugUtil.debug(DebugUtil.DEBUG, "AppletCreateTitleblockCADFile:checkoutTitleBlock", "sFileName : " + sFileName);

                CADTitleBlock[] a_TitleBlocks = new CADTitleBlock[1];
                a_TitleBlocks[0] = new CADTitleBlock(sObjectId, sViewId, sFormat, sLanguage, sBonProd, sHistoricLevel, sNbOfFolios, sAdditionalInfo, sTxtOnly, sNissanOrRenault);
                a_TitleBlocks = CADTitleBlock.retrieveTitleBlockFromServer(a_TitleBlocks, sServerTempDirectory, Parameters.getMainServerBaseURL());
                for (int iCpt2 = 0; iCpt2 < a_TitleBlocks.length; iCpt2++) {
                    a_TitleBlocks[iCpt2].createTitleBlockFile(inputDir, sFileName);
                    sDebugTrace = "Title Block generated : " + sFileName;
                    DebugUtil.debug(DebugUtil.DEBUG, "AppletCreateTitleblockCADFile:checkoutTitleBlock", sDebugTrace);

                }
            }

        } catch (PrivilegedActionException pae) {
            throw pae.getException();
        }
    }

    private Hashtable buildMxInfoCheckout(String cadId, String cadType, String cadName, String cadRevision, String cadDefId, String cadDefType, String cadDefName, String cadDefRevision,
            String listOfFiles) {
        Hashtable<String, Object> result = new Hashtable<String, Object>();
        Vector<String> cadids = FPDMAppletUtils.parseStringList(cadId, '#');
        Vector<String> cadtypes = FPDMAppletUtils.parseStringList(cadType, '#');
        Vector<String> cadnames = FPDMAppletUtils.parseStringList(cadName, '#');
        Vector<String> cadrevisions = FPDMAppletUtils.parseStringList(cadRevision, '#');
        Vector<String> ids = FPDMAppletUtils.parseStringList(cadDefId, '#');
        Vector<String> types = FPDMAppletUtils.parseStringList(cadDefType, '#');
        Vector<String> names = FPDMAppletUtils.parseStringList(cadDefName, '#');
        Vector<String> revisions = FPDMAppletUtils.parseStringList(cadDefRevision, '#');
        Vector<String> listsOfFiles = FPDMAppletUtils.parseStringList(listOfFiles, '#');
        int len = cadids.size();
        for (int i = 0; i < len; i++) {
            Hashtable<String, String> data = new Hashtable<String, String>();
            data.put("id", ids.elementAt(i));
            data.put("type", types.elementAt(i));
            data.put("name", names.elementAt(i));
            data.put("revision", revisions.elementAt(i));
            data.put("cadid", cadids.elementAt(i));
            data.put("cadtype", cadtypes.elementAt(i));
            data.put("cadname", cadnames.elementAt(i));
            data.put("cadrevision", cadrevisions.elementAt(i));
            data.put("listOfFiles", listsOfFiles.elementAt(i));
            result.put(cadids.elementAt(i), data);
        }
        System.out.println("mxInfo:" + result);
        return result;
    }

    public void destroy() {
        DebugUtil.debug(DebugUtil.INFO, "AppletCreateTitleblockCADFile:destroy", "Destroying applet");
    }

}
