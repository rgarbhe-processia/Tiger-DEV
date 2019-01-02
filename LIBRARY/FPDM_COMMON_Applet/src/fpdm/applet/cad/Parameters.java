package fpdm.applet.cad;

import java.util.Vector;

/**
 * @author steria
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class Parameters {
    public static final String FAURECIA_eMCS_SERVLET_NAME = "Faurecia_eMCSFileServlet";
    public static final String HOME_VAR_LOCALFILE_PATH  = "/tmp/home_var";
    public static final String DEFAULT_FORMAT = "generic";

	// FPDM STERIA ADD Start : MCE 02JAN2006: RFC2099: Add the checkin of session files
    public static final String TYPE_CAD_DEFINITION      = "CAD Definition";
	// FPDM STERIA ADD End   : MCE 02JAN2006: RFC2099
    public static final String TYPE_FPDM_CAD_COMPONENT  = "FPDM CAD Component";
    public static final String TYPE_FPDM_CAD_DRAWING    = "FPDM CAD Drawing";
    public static final String TYPE_2DVIEWABLE          = "2DViewable";

    public static final String LOCAL_REPERTORY          = "localRepertory";
	public static final String SERVER_REPERTORY			= "serverRepertory";
	public static final String IN_OUT 					= "inOut";
	public static final String OBJECT_ID				= "objectId";
	public static final String SCRIPT 					= "script";
	public static final String FORMAT					= "format";
	public static final String LOCK 					= "lock";
	public static final String APPEND 					= "append";
	public static final String PARAMETER_FILE 			= "parameterFile";
	public static final String SESSION_ID				= "sessionId";
	public static final String REL_PAT					= "relPat";
	public static final String TYPE_PAT					= "typePat";
	public static final String REC_LEVEL				= "recLevel";
	public static final String TYPE_LIST				= "typeList";
	public static final String DEBUG					= "debug";
	public static final String LISTOFFILES				= "ListOfFiles";
	public static final String LANGUAGE					= "Language";
	public static final String NB_COPY 					= "nbcopy";
	public static final String SCALE 					= "scale";
	public static final String COLOR 					= "color";
	public static final String FORMATDRAWING			= "formatdrawing";
	public static final String TITLE_BLOCK_ONLY 		= "titleBlockOnly";
	public static final String HISTORIC 				= "historic";
	public static final String BON_PROD 				= "bonProd";
	public static final String NISSAN_RENAULT           = "sNissanOrRenault";
	public static final String INFOS 					= "infos";
	public static final String NB_FOLIOS 				= "nbFolios";
	public static final String MSBG_CHECKOUT 			= "pMSBGCheckOut";
	public static final String CHECKOUTDIR   			= "Checkoutdir";
	public static final String CHECKIN2D   			    = "checkin2D";
	public static final String CAD_OBJECTS_TNR          = "sCadObjectNames";
	public static final String DOWNLOAD_TIF_COMMAND     = "sDownloadTIFCommand";
    public static String COMMANDE_SYSGEN                = "commande_sysgen";
    public static String CAD_FORMAT                     = "cad_format";
    public static String CAD_ENVIRONMENT                = "cad_environment";
    public static String VERSION_SYSTEM_GENERATEUR      = "version_system_generateur";
    public static String SYSTEM_GENERATEUR              = "system_generateur";
    public static String STYPEORI                       = "sTypeOri";
    public static String SCURRENTORI                    = "sCurrentOri";
    public static final String PRE_INTEGRATION          = "PRE_INTEGRATION";
    public static final String LIST_CAD_SOFTWARE        = "cad_softwares";
    public static final String LIST_SCRIPT              = "script_values";
    public static final String ORGANIZATION_NAME        = "organizationName";
    
    // FPDM Add Start - JFA 2005/12/23 RFC 1731
    public static final String PREFERED_PRINTER         = "preferencedPrinters";
	// FPDM Add End - JFA 2005/12/23

    // FPDM Add Start - JFA 2005/12/23 RFC 3783
    public static final String LISTHPGLPROPERTY         = "ListOfHPGLProperty";
    // FPDM Add End - JFA 2005/12/23 RFC 3783

	// FPDM Add Start - JFA 2006/01/17 Enhancement
	public static final String DEBUG_LEVEL         = "debug";
	// FPDM Add End

// FPDM Add Start - CRO 20050729 - CAD : TIFF banner - ESBG
	public static final String CURRENT_CADDEF     = "currentCADDef";	
    public static final String PART_STATE         = "PartState";
	public static final String USER_NAME          = "UserName";
	public static final String DATE               = "Date";
// FPDM Add End - CRO 20050729
    
    public static final String MAJOR_REVISION       = "MajorRevision";

	public static final String COLLECTION_NAME = "collectionName";
	public static final String COLLECTION_MESSAGE = "collectionMessage";

	//Define the size of the buffer used during the stream transmission operation
	public static final short BUFFER_SIZE = 1024;
	public static final String DELIM = "|";

    public static final Vector<String> POSSIBLE_VALUES_OF_INOUT_PARAMETER = new Vector<String>();
    static {
        POSSIBLE_VALUES_OF_INOUT_PARAMETER.addElement("checkin");
        POSSIBLE_VALUES_OF_INOUT_PARAMETER.addElement("checkout");
        POSSIBLE_VALUES_OF_INOUT_PARAMETER.addElement("viewAsTIF");
        POSSIBLE_VALUES_OF_INOUT_PARAMETER.addElement("viewAsTIFWindows");
        POSSIBLE_VALUES_OF_INOUT_PARAMETER.addElement("print");
        POSSIBLE_VALUES_OF_INOUT_PARAMETER.addElement("view");
        POSSIBLE_VALUES_OF_INOUT_PARAMETER.addElement("printWindows");
        POSSIBLE_VALUES_OF_INOUT_PARAMETER.addElement("viewWindows");
// FPDM Add Start - CRO 20050801 - CAD : TIFF banner - ESBG
        POSSIBLE_VALUES_OF_INOUT_PARAMETER.addElement("viewTIFFWithFileWindows");
        POSSIBLE_VALUES_OF_INOUT_PARAMETER.addElement("viewTIFFWithFile");
// FPDM Add End - CRO 20050801
//FPDM ADD Start : PRI 12/01/2006 RFC 2034
        POSSIBLE_VALUES_OF_INOUT_PARAMETER.addElement("checkoutMultipleTIFF");
        POSSIBLE_VALUES_OF_INOUT_PARAMETER.addElement("checkoutMultipleTIFFWindows");
//FPDM ADD End : PRI 12/01/2006 RFC 2034
        POSSIBLE_VALUES_OF_INOUT_PARAMETER.addElement("downloadMultipleTIFF");
        POSSIBLE_VALUES_OF_INOUT_PARAMETER.addElement("downloadMultipleTIFFWindows");
    }

	public static String debugging = "OFF";

    public static CadAppletUI getAppletUI() {
        String[] uiClasses = {
            "faurecia.applet.awt.CadAppletAwtUI",
            "faurecia.MCADIntegration.CadAppletIefUI"
        };
        if(cadAppletUI == null) {
            try {
                for(int i=0; i<uiClasses.length;i++) {
                    try {
                        Class c = Class.forName(uiClasses[i]);
                        if(c!=null) {
                            cadAppletUI = (CadAppletUI)c.newInstance();
                            break;
                        }
                    }
                    catch(ClassNotFoundException e) {
                    }
                }
            }
            catch(Exception e) {
                e.printStackTrace();
                // this should never happen, it is a systematic error
                return null;
            }
        }
        return cadAppletUI;
    }

    public static String getMainServerBaseURL() {
        return MAIN_SERVER_BASE_URL;
    }

    public static void setMainServerBaseURL(String string) {
        MAIN_SERVER_BASE_URL = string;
    }

    private static CadAppletUI cadAppletUI = null;
    private static String MAIN_SERVER_BASE_URL = null;
}
