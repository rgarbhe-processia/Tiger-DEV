/*
 * Creation Date : 27 juil. 04
 *
 * Pour changer le mod�le de ce fichier g�n�r�, allez � :
 * Fen�tre&gt;Pr�f�rences&gt;Java&gt;G�n�ration de code&gt;Code et commentaires
 */
package faurecia.util.cad;

import java.io.File;
import java.io.Serializable;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Vector;

import faurecia.util.AbstractFile;
import faurecia.util.AppletServletCommunication;
import faurecia.util.DebugUtil;
import faurecia.util.StringUtil;


/**
 * @author fcolin
 */

public class CADTitleBlock implements Serializable {
	
	  // FDPM ADD Start JFA 30/01/05 Message Log management
	  private final static String CLASSNAME          		= "CADTitleBlock";
	  // FDPM ADD End JFA 30/01/05 Message Log management

    public static final String FAURECIA_TITLEBLOCK_SERVLET_NAME = "Faurecia_eMCSFileServlet/retrieveTitleBlock";

    /** Object Id of the CAD Definition - Filled by Applet */
    private String CAD_DEFINITION_OBJECTID  = "";

    /** Object Id of the CAD Component (or Basis Definition) - Filled by Applet */
    private String CAD_VIEWABLE_OBJECTID    = "";

    /** Format of the CAD File (eg : A4, A3, ...) - Filled by Applet */
    private String CAD_FORMAT               = "";

    /** Language of the title block - Filled by Applet */
    private String TITLE_BLOCK_LANGUAGE     = "";

    /** Date of the title block - Filled at creation */
    private String TITLE_BLOCK_DATE         = "";

    /** Input parameter of the title block - Filled by Applet */
    private String BON_PROD                 = "";

    /** Input parameter of the title block - Filled by Applet */
    private boolean NISSAN_RENAULT          = false;

    /** Input parameter of the title block - Filled by Applet */
    private String HISTORIC_LEVEL           = "";

    /** Input parameter of the title block - Filled by Applet */
    private String NB_OF_FOLIOS             = "";

    /** Input parameter of the title block - Filled by Applet */
    private String ADDITIONAL_INFO          = "";
    
    /** Input parameter of the title block - Filled by Applet 
     *   This parameter is :
     *              -   empty when user ask for printing 
     *              -   "checkout" when user asks for checkout for modification
     *              -   "copy" when user asks for checkout for copy
     * */
    private String TXT_ONLY                 = "";
    
    /** Lines (in HPGL) of the Title Block - Filled by Servlet */
    private AbstractFile sContent = new AbstractFile();

    /** Horizontal offset for merging with CAD File - Filled by Servlet */
    private String sOffset_x                = "";

    /** Vertical offset for merging with CAD File - Filled by Servlet */
    private String sOffset_y                = "";
    
    /**
     * @param sCADDefinitionId
     * @param sViewableId
     * @param sFormat
     */
    public CADTitleBlock (  String sCADDefinitionId, 
                            String sViewableId, 
                            String sFormat,
                            String sLanguage,
                            String sBonProd,
                            String sHistoricLevel,
                            String sNbOfFolios,
                            String sAdditionalInfo,
                            String sTxtOnly,
                            String sNissanRenault
                            ) {
        
        this.CAD_VIEWABLE_OBJECTID      = sViewableId;
        this.CAD_DEFINITION_OBJECTID    = sCADDefinitionId;        
        this.CAD_FORMAT                 = sFormat;
        this.TITLE_BLOCK_LANGUAGE       = sLanguage;
        if ("O".equalsIgnoreCase(sBonProd)) {
            this.BON_PROD               = "O";
        } else {
            this.BON_PROD               = "N";
        }
        if (sNissanRenault != null && "O".equalsIgnoreCase(sNissanRenault)) {
            this.NISSAN_RENAULT         = true;
        }
        this.HISTORIC_LEVEL             = sHistoricLevel;
        this.NB_OF_FOLIOS               = sNbOfFolios;
        this.ADDITIONAL_INFO            =sAdditionalInfo;
        
        this.TITLE_BLOCK_DATE           = String.valueOf(new Date().getTime());
        this.TXT_ONLY                   = StringUtil.returnNullIfEmpty(sTxtOnly);
    }

    public File createTitleBlockFile (  String sTempLocalDirectory, 
                                        String sFileName)
            throws Exception 
    {
        // Construct & fill the file
        
        File file = sContent.createFileOnSystemDrive(sTempLocalDirectory, sFileName);
        return file;
    }


    public static CADTitleBlock[] retrieveTitleBlockFromServer (CADTitleBlock[] a_TitleBlocks, 
                                        String sServerTempDirectory,
                                        String sMainServerBaseURL)
            throws Exception 
    {
        // Build URL for the request to the servlet
        String sURL = sMainServerBaseURL + FAURECIA_TITLEBLOCK_SERVLET_NAME;
        sURL += "?TempDirectory="     + URLEncoder.encode(sServerTempDirectory);
        sURL += "&debug="         + (DebugUtil.iDebugLevel >= 4 ? "ON" : "");

        DebugUtil.debug(DebugUtil.INFO, CLASSNAME, "/retrieveTitleBlockFromServer: Connection to the MCS server to retrieve the title block : " + sURL);

//      -- BUG WORKAROUND Nb1 -----------------------
//         DOES NOT WORK PROPERLY (not Serialized object exception is launch by the applet)
//     WARNING : ONLY ONE TITLE BLOCK CAN NOW BE SEND !
//      -- WAS : -----
//        Object[] a_ReturnedObjects = AppletServletCommunication.requestServerTask (sURL, a_TitleBlocks);
//        for (int iCpt = 0; iCpt < a_ReturnedObjects.length ; iCpt++) {
//          a_TitleBlocks[iCpt] = (CADTitleBlock)a_ReturnedObjects[iCpt];
//          DebugUtil.debug(DebugUtil.INFO, "TitleBlock:getTitleBlockFromServer", "a_TitleBlocks[iCpt] : " + a_TitleBlocks[iCpt]);
//        }
//      -- IS REPLACED BY : -----
/*        Object[] a_ReturnedObjects = AppletServletCommunication.requestServerTask (sURL, a_TitleBlocks);
        for (int iCpt = 0; iCpt < a_ReturnedObjects.length ; iCpt++) {
            Vector vRet = (Vector)a_ReturnedObjects[iCpt];
            if (vRet != null && vRet.size() > 2) {
                a_TitleBlocks[iCpt].setXOffset((String)vRet.elementAt(0));
                a_TitleBlocks[iCpt].setYOffset((String)vRet.elementAt(1));
                vRet.removeElementAt(0);
                vRet.removeElementAt(0);
                a_TitleBlocks[iCpt].setTitleBlockLines(vRet);
            }
            DebugUtil.debug(DebugUtil.INFO, "TitleBlock:getTitleBlockFromServer", "a_TitleBlocks[iCpt] : " + a_TitleBlocks[iCpt]);
        }
*/
        Object[] a_ReturnedObjects = AppletServletCommunication.requestServerTask (sURL, a_TitleBlocks);
        for (int iCpt = 0; iCpt < a_ReturnedObjects.length ; iCpt++) {
          a_TitleBlocks[iCpt] = (CADTitleBlock)a_ReturnedObjects[iCpt];
          DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/retrieveTitleBlockFromServer: a_TitleBlocks[iCpt] : " + a_TitleBlocks[iCpt]);
        }
        
//      -- END OF BUG WORKAROUND Nb1 -----------------------

        return a_TitleBlocks;

    }

    public String getAdditionalInfo() {
        return ADDITIONAL_INFO;
    }

    public String getBonProd() {
        return BON_PROD;
    }

    public boolean getNissanRenault() {
        return NISSAN_RENAULT;
    }

    public String getCadDefinitionObjectId() {
        return CAD_DEFINITION_OBJECTID;
    }

    public String getCadViewableObjectId() {
        return CAD_VIEWABLE_OBJECTID;
    }

    public String getHistoricLevel() {
        return HISTORIC_LEVEL;
    }

    public String getNbOfFolios() {
        return NB_OF_FOLIOS;
    }

    public String getTITLE_BLOCK_DATE() {
        return TITLE_BLOCK_DATE;
    }

    public String getTitleBlockLanguage() {
        return TITLE_BLOCK_LANGUAGE;
    }

    public void setXOffset(String string) {
    	DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/setXOffset: New sOffset_x = " + string);
        this.sOffset_x = string;
    }

    public void setYOffset(String string) {
    	DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/setYOffset: New sOffset_y = " + string);
        sOffset_y = string;
    }

    public String getXOffset() {
        return sOffset_x;
    }

    public String getYOffset() {
        return sOffset_y ;
    }

    public void setContent (AbstractFile abstractFile) {
        this.sContent = abstractFile;
    }

    public AbstractFile getContent () {
        return this.sContent;
    }

    public void setTitleBlockLines(Vector vector) throws Exception {
    	DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/setTitleBlockLines: New Vector = ");
        sContent = new AbstractFile();
        sContent.setContent(vector);
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/setTitleBlockLines: End of vector");
    }
    public String getTxtOnly() {
        return this.TXT_ONLY;
    }

}
