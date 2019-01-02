package pss.cad2d3d;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.Attribute;
import matrix.db.AttributeItr;
import matrix.db.AttributeList;
import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.Page;
import matrix.util.MatrixException;
import matrix.util.StringList;
import pss.common.utils.TigerEnums;
import pss.common.utils.TigerEnums.FileFormat;
import pss.constants.TigerConstants;

/**
 * Class in charge of the CAD Title-block generation </br>
 * - Instantiated by the eMCS Servlet </br>
 * - The main method "createTitleBlock" called in order to generate the Titleblock
 */
public class TitleBlockCADFileGenerator_mxJPO {
    private final static Logger logger = LoggerFactory.getLogger(pss.cad2d3d.TitleBlockCADFileGenerator_mxJPO.class);

    public static final String TITLEBLOCK_ECO_BLOCK_PAGE = "FPDM_Titleblock_ECO_Block.txt";

    public static final String TITLEBLOCK_ECO_BLOCK_PAGE_UG = "FPDM_Titleblock_ECO_Block_For_UG.txt";

    public static final String TITLEBLOCK_BASIS_DEFINITION_BLOCK_PAGE = "FPDM_Titleblock_Basis_Definition_Block.txt";

    public static final String TITLEBLOCK_BASIS_DEFINITION_BLOCK_PAGE_UG = "FPDM_Titleblock_Basis_Definition_Block_For_UG.txt";

    public static final String TITLEBLOCK_CAD_INFO_BLOCK_PAGE = "FPDM_Titleblock_Info_Block.txt";

    public static final String TITLEBLOCK_CAD_PLAN_BLOCK_PAGE = "FPDM_Titleblock_Plan_Block.txt";

    public static final String TITLEBLOCK_CAD_PLAN_BLOCK_PAGE_UG = "FPDM_Titleblock_Plan_Block_For_UG.txt";

    public static final String TITLEBLOCK_RELATED_BASIS_DEFINITION_BLOCK_PAGE = "FPDM_Titleblock_Basis_Info_Block.txt";

    public static final String TITLEBLOCK_RELATED_BASIS_DEFINITION_BLOCK_PAGE_UG = "FPDM_Titleblock_Basis_Info_Block_For_UG.txt";

    public static final String TITLEBLOCK_PART_INFO_BLOCK_PAGE = "FPDM_Titleblock_Part_Info_Block.txt";

    public static final String TITLEBLOCK_PART_INFO_BLOCK_PAGE_UG = "FPDM_Titleblock_Part_Info_Block_For_UG.txt";

    public static final String TITLEBLOCK_TRANSLATION_PAGE = "FPDM_Titleblock_Translation.txt";

    public static final String TITLEBLOCK_NISSANRENAULT_PAGE = "FPDM_Titleblock_Nissan_Renault.txt";

    public static final String TITLEBLOCK_CAD_PLAN_BLOCK_CHECKOUT_PAGE = "FPDM_Titleblock_Plan_Block_Checkout.txt";

    public static final String TITLEBLOCK_CAD_PLAN_BLOCK_CHECKOUT_PAGE_UG = "FPDM_Titleblock_Plan_Block_Checkout_For_UG.txt";

    private boolean isCheckout = false;

    private boolean isCopy = true;

    private BufferedWriter cartouche_fd;

    // Current vertical position on the title-block (Y coordinate)
    private float ycart;

    // Margin coordinates
    private float cart_bep_hauteur_max_y;

    // character thickness
    private float cart_bep_epaisseur;

    // spacing between characters
    private float cart_bep_espacement;

    // characters height
    private float cart_bep_hauteur;

    // characters width
    private float cart_bep_largeur;

    private boolean isUG = false;

    private String sLanguageUsed = "US";

    private String sUser = "";

    // Contains the structure of the last read model file: keys are sections, content is a Vector of Lines for this section
    private Hashtable<String, Vector<String>> htInstructions = new Hashtable<String, Vector<String>>();

    // Contains translations label for the title-block
    private Hashtable<String, String> htTranslations = new Hashtable<String, String>();

    /**
     * Constructor
     * @throws Exception
     */
    public TitleBlockCADFileGenerator_mxJPO() throws Exception {
    }

    /**
     * Construct and generate lines to draw a section of the title-block
     * @param section
     *            : Section name
     * @param vArgs
     *            : values to insert in this section
     * @throws Exception
     */
    private void generateElementarySection(String section, Vector<String> vArgs) throws Exception {
        try {
            if (this.htInstructions.containsKey(section)) {
                Vector<String> vLines = this.htInstructions.get(section);
                for (int iCpt = 0; iCpt < vLines.size(); iCpt++) {
                    writeLine(vLines.get(iCpt), vArgs);
                }
            }
        } catch (Exception e) {
            logger.error("Error in generateElementarySection()\n", e);
            throw e;
        }
        return;
    }

    /**
     * Generate a plot line and write it on the text CAD file
     * @param line
     *            : instruction line to draw
     * @param vArgs
     *            : values to insert in this line
     * @throws Exception
     */
    private void writeLine(String line, Vector<String> vArgs) throws Exception {
        int deltax = 1;
        String sLineToWrite = "";

        try {
            StringTokenizer token = new StringTokenizer(line, "(),");
            String sInstruction = token.nextToken().trim();
            if ("POLL".equals(sInstruction)) {
                // l'ordre de passage obligatoire des parametres de l'instruction POLL, est :
                // E,t epaisseur, type. -> aucune action a faire...
                String sEpaisseur = (String) token.nextToken();
                String sType = (String) token.nextToken();
                sLineToWrite = sInstruction + " " + sEpaisseur + " " + sType;
            } else if ("LINE".equals(sInstruction)) {
                // l'ordre de passage obligatoire des parametres de l'instruction LINE, est :
                // x,y,x',y' coordonnees. -> recalcul y et y'
                String s_X = (String) token.nextToken();
                String s_Y = (String) token.nextToken();
                String s_X2 = (String) token.nextToken();
                String s_Y2 = (String) token.nextToken();
                float y = Float.parseFloat(s_Y);
                float y2 = Float.parseFloat(s_Y2);
                s_Y = String.valueOf(y + this.ycart);
                s_Y2 = String.valueOf(y2 + this.ycart);
                sLineToWrite = sInstruction + " " + s_X + " " + s_Y + " " + s_X2 + " " + s_Y2;
            } else if ("XLINE".equals(sInstruction)) {
                // l'ordre de passage obligatoire des parametres de l'instruction LINE, est :
                // x,y,x',y' coordonnees. -> recalcul y et y'
                String s_X = (String) token.nextToken();
                String s_Y = (String) token.nextToken();
                String s_X2 = (String) token.nextToken();
                String s_Y2 = (String) token.nextToken();

                float y = Float.parseFloat(s_Y);
                float y2 = Float.parseFloat(s_Y2);
                s_Y = String.valueOf(y - (y2 * 0.5) + this.ycart);
                s_Y2 = String.valueOf(y2 - (y * 0.5) + this.ycart);
                sLineToWrite = "LINE " + s_X + " " + s_Y + " " + s_X2 + " " + s_Y2;
            } else if (sInstruction.equals("CIRC")) {
                // l'ordre de passage obligatoire des parametres de l'instruction CIRC,est :
                // x,y coordonnees; r rayon. -> recalcul y
                String s_X = (String) token.nextToken();
                String s_Y = (String) token.nextToken();
                String s_R = (String) token.nextToken();
                float y = Float.parseFloat(s_Y);
                s_Y = String.valueOf(y + this.ycart);
                sLineToWrite = sInstruction + " " + s_X + " " + s_Y + " " + s_R;
            } else if ("DITO".equals(sInstruction)) {
                // l'ordre de passage obligatoire des parametres de l'instruction DITO,est :
                // x,y coordonnees; a action; n nom du logo. -> recalcul y
                String s_X = (String) token.nextToken();
                String s_Y = (String) token.nextToken();
                String s_Action = (String) token.nextToken();
                String s_Logo = (String) token.nextToken();
                float y = Float.parseFloat(s_Y);
                s_Y = String.valueOf(y + this.ycart);
                if ("site".equals(s_Action)) {
                    s_Logo = s_Logo + "_001";
                    s_Logo = this.translateValue("CARTOUCHE", s_Logo);
                }
                sLineToWrite = sInstruction + " " + s_X + " " + s_Y + " " + s_Logo;
            } else if ("POLT".equals(sInstruction)) {
                // l'ordre de passage obligatoire des parametres de l'instruction POLT, est :
                // l,h,e largeur hauteur espacement des caracteres; E epaisseur
                // on sauvegarde les valeurs pour calcul de positionnement du texte...
                this.cart_bep_largeur = Float.parseFloat((String) token.nextToken());
                this.cart_bep_hauteur = Float.parseFloat((String) token.nextToken());
                this.cart_bep_espacement = Float.parseFloat((String) token.nextToken());
                this.cart_bep_epaisseur = Float.parseFloat((String) token.nextToken());
                sLineToWrite = sInstruction + " " + String.valueOf(this.cart_bep_largeur) + " " + String.valueOf(this.cart_bep_hauteur);
                sLineToWrite += " " + String.valueOf(this.cart_bep_espacement) + " " + String.valueOf(this.cart_bep_epaisseur);
            } else if ("TEXT".equals(sInstruction)) {
                // l'ordre de passage obligatoire des parametres de l'instruction TEX, est : x,y coordonnees; j justification
                // et a code action (trad: traduction, date: conversion, addr: adresse, ... , none aucune
                // et eventuellement t taille maxi -> tronquage eventuelle de la chaine...
                // les coordonnees retournees doivent correspondre au coin inferieur gauche, donc
                // ils doivent etre recalcules pour une justification au centre ou a droite.
                String s_X = (String) token.nextToken();
                String s_Y = (String) token.nextToken();
                String s_Justif = (String) token.nextToken();
                String s_Action = (String) token.nextToken();
                String s_Text = (String) token.nextToken();
                s_Text = s_Text.trim();
                int i_MaxNbChars = -1;
                if (token.hasMoreTokens()) {
                    i_MaxNbChars = Integer.parseInt((String) token.nextToken());
                }
                float x = Float.parseFloat(s_X);
                float y = Float.parseFloat(s_Y);
                y = y + this.ycart;

                // recherche de valeur a traduire
                if (s_Text.indexOf('<') != -1) {
                    s_Text = s_Text.substring(s_Text.lastIndexOf('<') + 1, s_Text.lastIndexOf('>'));
                    if (s_Text.length() <= 0) {
                        throw new Exception("SYNTAX_ERROR_IN_GRAPHIC_TEXT_INSTRUCTION");
                    }
                    if ("INFORMATION".equals(s_Text)) {
                        Date dDate = new Date();
                        s_Text = this.translateValue("CARTOUCHE", s_Text);
                        StringTokenizer tokenInfo = new StringTokenizer(s_Text, "%");
                        s_Text = "";
                        if (tokenInfo.hasMoreTokens())
                            s_Text += " " + tokenInfo.nextToken();
                        String format = "";
                        if ((this.sLanguageUsed.equals("US"))) {
                            format = "yyyy/MM/dd";
                        } else {
                            format = "dd/MM/yyyy";
                        }
                        SimpleDateFormat sampleDataFormat = new SimpleDateFormat(format);
                        s_Text += " " + sampleDataFormat.format(dDate);
                        if (tokenInfo.hasMoreTokens())
                            s_Text += " " + tokenInfo.nextToken();
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm");
                        s_Text += " " + simpleDateFormat.format(dDate);
                        if (tokenInfo.hasMoreTokens())
                            s_Text += " " + tokenInfo.nextToken();

                        s_Text += " " + this.sUser;
                        String s_infoenplus = (String) vArgs.get(0);
                        if (!"".equals(s_infoenplus)) {
                            s_Text += ": ";
                            s_Text += s_infoenplus;
                        }
                    } else {
                        s_Text = this.translateValue("CARTOUCHE", s_Text);
                    }
                } else if (s_Text.indexOf('[') != -1) {
                    // Retrieve the value to show from the input vector
                    s_Text = s_Text.substring(s_Text.lastIndexOf('[') + 1, s_Text.lastIndexOf(']'));
                    int iIndex = Integer.parseInt(s_Text);
                    if (iIndex >= 0 && vArgs.size() > iIndex)
                        s_Text = (String) vArgs.get(iIndex);

                    // Loop thru the possible actions
                    if (s_Action.equals("none")) {
                        // Rectype ...
                    } else if (s_Action.equals("trad")) {
                        s_Text = this.translateValue("CARTOUCHE", s_Text);
                        // Convert date
                    } else if (s_Action.equals("date") && s_Text != null && !"".equals(s_Text)) {
                        if (!"#DENIED!".equals(s_Text)) {
                            try {
                                SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a");
                                Date originatedDate = formatter.parse(s_Text);
                                formatter = new SimpleDateFormat("ddMMMyy");
                                String sParsedDate = formatter.format(originatedDate);
                                s_Text = sParsedDate;

                            } catch (java.text.ParseException e) {
                            }
                        }
                        // Tol_angulaire ...
                    } else if (s_Action.equals("deg+")) {
                        if (s_Text != null && !"".equals(s_Text)) {
                            // Ecriture du + - pour les angles
                            float xint = (x - (s_Text.length() * (this.cart_bep_largeur + this.cart_bep_espacement) - this.cart_bep_espacement) / 2 - this.cart_bep_espacement * 2);
                            String Dint = "LINE " + String.valueOf(xint - deltax) + " " + String.valueOf(y) + " " + String.valueOf(xint) + " " + String.valueOf(y) + "\n";
                            cartouche_fd.write(Dint);
                            Dint = "LINE " + String.valueOf(xint - deltax) + " " + String.valueOf(y + deltax) + " " + String.valueOf(xint) + " " + String.valueOf(y + deltax) + "\n";
                            cartouche_fd.write(Dint);
                            Dint = "LINE " + String.valueOf(xint - deltax * 0.5) + " " + String.valueOf(y + deltax * 0.5) + " " + String.valueOf(xint - deltax * 0.5) + " "
                                    + String.valueOf(y + deltax * 1.5) + "\n";
                            cartouche_fd.write(Dint);
                            xint = (x + (s_Text.length() * (this.cart_bep_largeur + this.cart_bep_espacement) - this.cart_bep_espacement) / 2 + this.cart_bep_espacement * 2);
                            Dint = "CIRC " + String.valueOf(xint) + " " + String.valueOf(y + deltax * 2.5) + " " + String.valueOf(deltax * 0.25) + "\n";
                            cartouche_fd.write(Dint);
                        }
                        // Tol_lineaire ...
                    } else if (s_Action.equals("mm+")) {
                        if (s_Text != null && !"".equals(s_Text)) {
                            float fLenText = s_Text.length() * (this.cart_bep_largeur + this.cart_bep_espacement) - this.cart_bep_espacement;
                            float xint = x - (fLenText) / 2 - this.cart_bep_espacement * 2;
                            String Dint = "LINE " + String.valueOf(xint - deltax) + " " + String.valueOf(y) + " " + String.valueOf(xint) + " " + String.valueOf(y) + "\n";
                            cartouche_fd.write(Dint);
                            Dint = "LINE " + String.valueOf(xint - deltax) + " " + String.valueOf(y + deltax) + " " + String.valueOf(xint) + " " + String.valueOf(y + deltax) + "\n";
                            cartouche_fd.write(Dint);
                            Dint = "LINE " + String.valueOf(xint - deltax * 0.5) + " " + String.valueOf(y + deltax * 0.5) + " " + String.valueOf(xint - deltax * 0.5) + " "
                                    + String.valueOf(y + deltax * 1.5) + "\n";
                            cartouche_fd.write(Dint);
                            float xint2 = x + (fLenText) / 2 + this.cart_bep_espacement * 2;
                            Dint = "TEXT " + String.valueOf(xint2) + " " + String.valueOf(y) + " " + String.valueOf("mm".length()) + " mm" + "\n";
                            cartouche_fd.write(Dint);
                        }
                        // N'affiche pas les zeros
                    } else if ("zero-".equals(s_Action)) {
                        if ("0".equals(s_Text))
                            s_Text = "";
                        // Pts de reglementation et securite afficher ?
                    } else if ("pts".equals(s_Action)) {
                        if ("O".equals(s_Text))
                            s_Text = "X";
                        else
                            s_Text = "";
                    }
                    // troncage a faire ?
                    if (i_MaxNbChars >= 0 && s_Text != null && (s_Text.length() > i_MaxNbChars)) {
                        s_Text = s_Text.substring(0, i_MaxNbChars);
                    }
                } else {
                    throw new MatrixException("ERROR_IN_GRAPHIC_TEXT_INSTRUCTION");
                }
                if (s_Text != null && s_Justif.equals("center")) {
                    x = (x - (((this.cart_bep_largeur + this.cart_bep_espacement) * s_Text.length()) - this.cart_bep_espacement) / 2);
                } else if (s_Text != null && s_Justif.equals("right")) {
                    x = (x - (((this.cart_bep_largeur + this.cart_bep_espacement) * s_Text.length()) - this.cart_bep_espacement));
                } else if (s_Text != null && s_Justif.equals("center2")) {
                    x = (x - (((this.cart_bep_largeur + this.cart_bep_espacement) * s_Text.length())) / 4);
                }
                if (s_Text != null && s_Text.length() > 0) {
                    sLineToWrite = sInstruction + " " + String.valueOf(x) + " " + String.valueOf(y) + " ";
                    sLineToWrite += String.valueOf(s_Text.length()) + " " + s_Text;
                }
            } else {
                throw new MatrixException("INVALID_GRAPHIC_FUNCTION");
            }
            if (!"".equals(sLineToWrite)) {
                // on recopie dans le cartouche
                cartouche_fd.write(sLineToWrite + "\n");
            }
        } catch (Exception e) {
            logger.error("Error in writeLine()\n", e);
            throw e;
        }
        return;
    }

    /**
     * Generate instructions to draw the history block - ECO block
     * @param vHistoryInfos
     *            : Basis Definitions informations
     * @param isECOBlockPage
     *            : Page file containing instructions
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private void generateHistoricPart(Vector<Hashtable<String, Object>> vHistoryInfos, InputStream isECOBlockPage) throws Exception {
        try {
            logger.debug("generateHistoricPart() - vHistoryInfos = <" + vHistoryInfos + ">");
            if (vHistoryInfos.size() == 0) {
                logger.debug("/generateHistoricPart --> No history to show");
                return;
            }

            CART_BEP_sauvegarde_section(isECOBlockPage);

            // HEADER
            Vector<String> vChars = new Vector<String>();
            generateElementarySection("HEADER", vChars);
            /* Update pixel values */
            float height = Float.parseFloat(CART_BEP_valeur_s_init("height"));
            this.ycart = this.ycart + height;
            float delta = Float.parseFloat(CART_BEP_valeur_s_init("delta"));

            /* SECTION OF ALL REVISIONS OF THE PART */
            // From the last revision to the first :
            // Show the part informations with ECO
            // Focus the last revision (DER_IND)
            // Focus the last released revision (DER_IND_BPF)
            // Show all revisions included in the input hashtable
            for (int iCpt = vHistoryInfos.size() - 1; iCpt >= 0; iCpt--) {
                Hashtable<String, Object> ht_ThisRevPartInfos = vHistoryInfos.get(iCpt);
                String sPartRev = (String) ht_ThisRevPartInfos.get("PART_REVISION");
                String s_ECOState = (String) ht_ThisRevPartInfos.get("CO_STATE");
                Vector<String> vCOCRImpacted = (Vector<String>) ht_ThisRevPartInfos.get("COCR_NUMBERS");
                String s_ECOOriginated = (String) ht_ThisRevPartInfos.get("CO_ORIGINATED");
                String s_ECOOriginator = (String) ht_ThisRevPartInfos.get("CO_ORIGINATOR");
                Vector<String> vPartsImpacted = (Vector<String>) ht_ThisRevPartInfos.get("PARTS_IMPACTED");
                Vector<String> vDescription = (Vector<String>) ht_ThisRevPartInfos.get("ECO_DESCRIPTION");
                int iMaxNbOfCharactersPerLine = pss.cad2d3d.TitleBlockUtil_mxJPO.getMaxNumberOfCharactersPerLine("DESCRIPTION", this.htInstructions);
                if (iMaxNbOfCharactersPerLine > 0)
                    vDescription = pss.cad2d3d.TitleBlockUtil_mxJPO.reformatLines(vDescription, iMaxNbOfCharactersPerLine);

                int nbLinesToWrite = 2;
                if (nbLinesToWrite < vPartsImpacted.size()) {
                    nbLinesToWrite = vPartsImpacted.size();
                }
                String s_ECOName = "";
                String s_ECRName = "";
                if (vCOCRImpacted != null) {
                    s_ECOName = (vCOCRImpacted.size() > 0) ? vCOCRImpacted.get(0) : "";
                    s_ECRName = (vCOCRImpacted.size() > 1) ? vCOCRImpacted.get(1) : "";
                    if (nbLinesToWrite < vCOCRImpacted.size()) {
                        nbLinesToWrite = vCOCRImpacted.size();
                    }
                }

                // Separateur entre lignes
                vChars.clear();
                generateElementarySection("SEPARATEUR", vChars);
                // Ecriture depuis la derniere ligne jusqu'a la premiere
                for (int iCount = nbLinesToWrite; iCount >= 1; iCount--) {
                    // Structure de ligne (vertical)
                    vChars.clear();
                    generateElementarySection("LINE", vChars);
                    if (iCount == 1) {
                        // Write CAD revision and CO status on the first line
                        vChars.clear();
                        vChars.addElement(sPartRev);
                        vChars.addElement(s_ECOState);
                        generateElementarySection("REV_AND_STATE", vChars);

                        // Write CO NUMBER and DATE OF CHANGE on the first line
                        vChars.clear();
                        vChars.addElement(s_ECOName);
                        vChars.addElement(s_ECOOriginated);
                        generateElementarySection("ECO_NB_AND_DATE", vChars);
                    } else if (iCount == 2) {
                        // Write CR NUMBER and DATE OF CHANGE on the second line
                        vChars.clear();
                        vChars.addElement(s_ECRName);
                        vChars.addElement(s_ECOOriginator);
                        generateElementarySection("ECR_NB_AND_AUTHOR", vChars);
                    } else if (iCount > 2) {
                        // Write the other CR NUMBER on the next line
                        vChars.clear();
                        vChars.addElement((vCOCRImpacted != null && vCOCRImpacted.size() >= iCount) ? vCOCRImpacted.get(iCount - 1) : "");
                        vChars.addElement("");
                        generateElementarySection("ECR_NB_AND_AUTHOR", vChars);
                    }

                    if (iCount <= vPartsImpacted.size()) {
                        // Write "PART TO CREATE"
                        vChars.clear();
                        vChars.addElement(vPartsImpacted.elementAt(iCount - 1));
                        vChars.addElement("");
                        generateElementarySection("PART_IMPACTED", vChars);
                    }

                    int iIndexDescLine = iCount - 1;
                    if (iIndexDescLine >= 0 && iIndexDescLine < vDescription.size()) {
                        // write "CHANGE DESCRIPTION"
                        vChars.clear();
                        vChars.addElement(vDescription.elementAt(iIndexDescLine));
                        generateElementarySection("DESCRIPTION", vChars);
                    }
                    this.ycart = this.ycart + delta;
                }
            }

            // End of the loop thru the revisions of the part
            vChars.clear();
            generateElementarySection("LAST_LINE", vChars);
        } catch (Exception e) {
            logger.error("Error in generateHistoricPart()\n", e);
            throw e;
        }
        return;
    }

    /**
     * Generate instructions to draw the Basis Definition block
     * @param vHistoryInfos
     *            : Basis Definitions informations
     * @param inBasisDefinitionBlock
     *            : Page file containing instructions
     * @throws Exception
     */
    private void generateHistoricBasisDefinition(Vector<Hashtable<String, String>> vHistoryInfos, InputStream inBasisDefinitionBlock) throws Exception {
        try {
            if (vHistoryInfos.size() == 0) {
                logger.debug("/generateHistoricBasisDefinition: No history to show");
                return;
            }
            Vector<String> vChars = new Vector<String>();
            // CHARGEMENT DU FICHIER MODELE
            CART_BEP_sauvegarde_section(inBasisDefinitionBlock);
            // HEADER
            generateElementarySection("HEADER", vChars);
            /* Update pixel values */
            float height = Float.parseFloat(CART_BEP_valeur_s_init("height"));
            this.ycart = this.ycart + height;
            float delta = Float.parseFloat(CART_BEP_valeur_s_init("delta"));
            for (int iCpt = 0; iCpt < vHistoryInfos.size(); iCpt++) {
                Hashtable<String, String> ht_ThisRevPartInfos = vHistoryInfos.get(iCpt);
                String sBasisDefRev = ht_ThisRevPartInfos.get("BASIS_DEF_REVISION");
                String sBasisDefState = ht_ThisRevPartInfos.get("BASIS_DEF_STATE");
                String sBasisDefOriginated = ht_ThisRevPartInfos.get("BASIS_DEF_ORIGINATED");
                String sBasisDefOriginator = ht_ThisRevPartInfos.get("BASIS_DEF_ORIGINATOR");
                String sBasisDefDescription = ht_ThisRevPartInfos.get("BASIS_DEF_DESCRIPTION");
                String[] arrBasisDefDesc = sBasisDefDescription.split("(<br\\/>)");
                Vector<String> vDescription = new Vector<String>(Arrays.asList(arrBasisDefDesc));
                // Rebuild description in splitting description
                int iMaxNbOfCharactersPerLine = pss.cad2d3d.TitleBlockUtil_mxJPO.getMaxNumberOfCharactersPerLine("DESCRIPTION", this.htInstructions);
                if (iMaxNbOfCharactersPerLine > 0)
                    vDescription = pss.cad2d3d.TitleBlockUtil_mxJPO.reformatLines(vDescription, iMaxNbOfCharactersPerLine);
                // Calculate the nb of lines to write for this part revision
                int nbLinesToWrite = 1;
                if (nbLinesToWrite < vDescription.size())
                    nbLinesToWrite = vDescription.size();
                // Separateur entre lignes
                vChars.clear();
                generateElementarySection("SEPARATEUR", vChars);
                // Ecriture depuis la derniere ligne jusqu'a la premiere
                for (int iCpt2 = nbLinesToWrite; iCpt2 >= 1; iCpt2--) {
                    // Structure de ligne (vertical)
                    vChars.clear();
                    generateElementarySection("LINE", vChars);
                    // Ecriture de REV et STATUS
                    if (iCpt2 == 1) {
                        vChars.clear();
                        vChars.addElement(sBasisDefRev);
                        generateElementarySection("REVISION", vChars);
                    }
                    if (iCpt2 == 1) {
                        vChars.clear();
                        vChars.addElement(sBasisDefState);
                        generateElementarySection("STATE", vChars);
                    }
                    // Ecriture de ECO NUMBER et DATE OF CHANGE sur la premiere ligne
                    if (iCpt2 == 1) {
                        vChars.clear();
                        vChars.addElement(sBasisDefOriginated);
                        generateElementarySection("ORIGINATED", vChars);
                        // Ecriture de ECR NUMBER et AUTHOR OF CHANGE sur la deuxieme ligne
                    }
                    if (iCpt2 == 1) {
                        vChars.clear();
                        vChars.addElement(sBasisDefOriginator);
                        generateElementarySection("ORIGINATOR", vChars);
                    }
                    // Ecriture du "CHANGE DESCRIPTION"
                    int iIndexDescLine = iCpt2 - 1;
                    if (iIndexDescLine >= 0 && iIndexDescLine < vDescription.size()) {
                        vChars.clear();
                        vChars.addElement(vDescription.elementAt(iIndexDescLine));
                        generateElementarySection("DESCRIPTION", vChars);
                    }
                    this.ycart = this.ycart + delta;
                }
            }
            // End of the loop through the revisions of the part
            vChars.clear();
            generateElementarySection("LAST_LINE", vChars);
        } catch (Exception e) {
            logger.error("Error in generateHistoricBasisDefinition()\n", e);
            throw e;
        }
        return;
    }

    /**
     * Fonction de lecture d'une variable du bloc d'initialisation ([INIT])
     * @param var
     */
    private String CART_BEP_valeur_s_init(String var) throws Exception {
        String valueint = "0";
        // recherche de la valeur de la variable...
        if (this.htInstructions.containsKey("INIT")) {
            Vector<String> vLines = this.htInstructions.get("INIT");
            for (int iCpt = 0; iCpt < vLines.size(); iCpt++) {
                String sLine = vLines.get(iCpt);
                if (sLine.indexOf(var) != -1) {
                    valueint = sLine.substring(sLine.lastIndexOf('=') + 1);
                    break;
                }
            }
        }
        return (valueint);
    }

    /**
     * Read the page file and save sections instructions on a hash table
     * @param isTitleblockBlockPage
     *            : Page file containing instructions
     * @throws Exception
     */
    private void CART_BEP_sauvegarde_section(InputStream isTitleblockBlockPage) throws IOException {
        BufferedReader buf = null;
        try {

            Reader readers = new InputStreamReader(isTitleblockBlockPage, StandardCharsets.UTF_8);
            buf = new BufferedReader(readers);
            // le fichier modele est ouvert en lecture,lecture du fichier pour en extraire les sections associee a la zone ...
            String lineRead = buf.readLine();
            String section;
            this.htInstructions = new Hashtable<String, Vector<String>>();
            while (lineRead != null) {
                lineRead = lineRead.trim();
                // lecture d'une ligne dans le fichier modele
                if (lineRead.indexOf('[') == 0) {
                    // La ligne lue est le debut d'une section a prendre en compte
                    Vector<String> vLines = new Vector<String>();
                    section = lineRead.trim();
                    section = section.substring(1, section.length() - 1);
                    // Lecture des instructions pour la section courante
                    lineRead = buf.readLine();
                    if (UIUtil.isNotNullAndNotEmpty(lineRead))
                        lineRead = lineRead.trim();

                    while (UIUtil.isNotNullAndNotEmpty(lineRead) && (lineRead.indexOf("[END]") == -1)) {
                        if (lineRead.indexOf(':') != 0) {
                            vLines.addElement(lineRead);
                        }
                        lineRead = buf.readLine();
                    }
                    this.htInstructions.put(section, vLines);
                }
                lineRead = buf.readLine();
            }

        } catch (IOException e) {
            logger.error("Error in CART_BEP_sauvegarde_section()\n", e);
            throw e;
        } finally {
            if (buf != null) {
                buf.close();
            }
        }
        return;
    }

    /**
     * Generate instructions to draw the Parts block
     * @param htTitleBlockInformations
     *            : CAD informations
     * @param isPlanModelBlock
     *            : Page file containing instructions
     * @param bULS
     *            : true/false
     * @throws Exception
     */
    private void CART_BEP_generation_zone_plan(Hashtable<String, Object> htTitleBlockInformations, InputStream isPlanModelBlock, boolean bULS) throws Exception {
        Vector<String> vChars = new Vector<String>();
        try {
            // CHARGEMENT DU FICHIER MODELE
            CART_BEP_sauvegarde_section(isPlanModelBlock);
            float delta = Float.parseFloat(CART_BEP_valeur_s_init("delta"));
            // Zone Principale
            // Formattage de la description
            String sCADModelDesc = "";
            sCADModelDesc = (String) htTitleBlockInformations.get("CADMODEL_DESCRIPTION");
            sCADModelDesc = pss.cad2d3d.TitleBlockUtil_mxJPO.formatDescription(sCADModelDesc);
            sCADModelDesc = sCADModelDesc.replaceAll("\\n", " ");
            String[] arrCADModelDesc = sCADModelDesc.split("(<br\\/>)");
            Vector<String> vDescription = new Vector<String>(Arrays.asList(arrCADModelDesc));
            int iMaxNbOfCharactersPerLine = pss.cad2d3d.TitleBlockUtil_mxJPO.getMaxNumberOfCharactersPerLine("MAIN", this.htInstructions);
            if (iMaxNbOfCharactersPerLine > 0) {
                vDescription = pss.cad2d3d.TitleBlockUtil_mxJPO.reformatLines(vDescription, iMaxNbOfCharactersPerLine);
            }
            vChars.clear();
            // Dimensioning Std (i= 0 to 3)
            for (int iCpt = 1; iCpt <= 4; iCpt++) {
                String sDimStds = (String) htTitleBlockInformations.get("CADMODEL_DIMENSIONSTD" + String.valueOf(iCpt));
                if (!("".equals(sDimStds)))
                    vChars.addElement(sDimStds);
            }
            while (vChars.size() < 4) {
                vChars.addElement("");
            }
            // Undim Radius (i=4)
            vChars.addElement((String) htTitleBlockInformations.get("CADMODEL_UNDIMRADIUS"));
            // Angular Tolerance (i=5)
            vChars.addElement((String) htTitleBlockInformations.get("CADMODEL_ANGULARTOL"));
            // Linear Tol (i=6)
            vChars.addElement((String) htTitleBlockInformations.get("CADMODEL_LINEARTOL"));
            // Scale (i=7)
            vChars.addElement((String) htTitleBlockInformations.get("CADMODEL_SCALE"));
            // Format papier (i=8)
            vChars.addElement((String) htTitleBlockInformations.get("VIEWABLE_FILEFORMAT"));
            // Nb Folio (i=9)
            String sFolioText = (String) htTitleBlockInformations.get("VIEWABLE_FOLIO");
            sFolioText += "/" + (String) htTitleBlockInformations.get("CADMODEL_NBOFFOLIOS");
            vChars.addElement(sFolioText);
            // Type du CAD Model (i=10)
            vChars.addElement((String) htTitleBlockInformations.get("CADMODEL_TYPE"));
            // No more used (i=11)
            vChars.addElement("");
            // Nom, Revision et description du CAD Model (i=12 to 13)
            vChars.addElement((String) htTitleBlockInformations.get("CADMODEL_DISPLAYNAME"));
            vChars.addElement((String) htTitleBlockInformations.get("CADMODEL_DISPLAYREVISION"));
            // modify for ULS start
            // Adresse du Departement de la part (i=14 to 17)
            // if(bULS)
            // {
            vChars.addElement((String) htTitleBlockInformations.get("DEPT_NAME"));
            vChars.addElement((String) htTitleBlockInformations.get("DEPT_STREET"));
            vChars.addElement((String) htTitleBlockInformations.get("DEPT_CITY"));
            vChars.addElement((String) htTitleBlockInformations.get("DEPT_PHONE"));
            int iLengthMaxForULS = pss.cad2d3d.TitleBlockUtil_mxJPO.getMaxNumberOfCharactersPerLine("MAIN", this.htInstructions);
            // Program (i=18)
            // Vehicle (i=19)
            vChars.addElement(getUlsLine(iLengthMaxForULS, (String) htTitleBlockInformations.get("PROGRAM")));
            vChars.addElement(getUlsLine(iLengthMaxForULS, (String) htTitleBlockInformations.get("VEHICLE")));

            // modify for ULS end
            // State and Date of last promotion (i=20 and 21)
            vChars.addElement((String) htTitleBlockInformations.get("CADMODEL_STATE"));
            vChars.addElement((String) htTitleBlockInformations.get("CADMODEL_DATE"));

            generateElementarySection("MAIN", vChars);
            // Zone "DRAWING CREATED IN CAD SYSTEM"
            vChars.clear();
            if ("Unknown".equalsIgnoreCase((String) htTitleBlockInformations.get("ATT_GENERATEUR"))) {
                generateElementarySection("MANUEL", vChars);
            } else {
                generateElementarySection("CAO", vChars);
            }
            // Zone plan US/EU (dessin du symbole du plan)
            vChars.clear();
            if ("US".equals((String) htTitleBlockInformations.get("CADMODEL_VIEWCONV")))
                generateElementarySection("PLAN_US", vChars);
            else
                generateElementarySection("PLAN_EU", vChars);

            // recherche du height pour la mise a jour de ycart
            this.ycart = Float.parseFloat(CART_BEP_valeur_s_init("height"));

            int iDescriptionLineNumber = vDescription.size();
            for (int iLine = iDescriptionLineNumber; iLine > 0; iLine--) {
                // write "DESCRIPTION"
                vChars.clear();
                vChars.addElement(vDescription.elementAt(iLine - 1));
                generateElementarySection("DESCRIPTION", vChars);
                this.ycart = this.ycart + (3 * delta / 2);
            }
            this.ycart = this.ycart + (delta / 2);

            // last line of the block
            vChars.clear();
            generateElementarySection("LAST_LINE", vChars);

        } catch (Exception e) {
            logger.error("Error in CART_BEP_generation_zone_plan()\n", e);
            throw e;
        }
        return;
    }

    /**
     * Generate instructions to draw the NISSAN-RENAULT block
     * @param isNissanRenaultBlock
     *            : Page file containing instructions
     * @throws Exception
     */
    private void CART_BEP_generation_zone_nissan_renault(InputStream isNissanRenaultBlock) throws Exception {
        Vector<String> vChars = new Vector<String>();
        try {
            CART_BEP_sauvegarde_section(isNissanRenaultBlock);
            // Zone Principale
            generateElementarySection("MAIN", vChars);
            this.ycart = this.ycart + Float.parseFloat(CART_BEP_valeur_s_init("height"));
        } catch (Exception e) {
            logger.error("Error in CART_BEP_generation_zone_nissan_renault()\n", e);
            throw e;
        }
        return;
    }

    private String getUlsLine(int iLengthMaxForULS, String vUlsInfo) {
        String sULS = "";
        StringBuffer sbULS = new StringBuffer();
        int iTmpLength = 0;
        sULS = vUlsInfo;
        if (sULS != null) {
            iTmpLength = sbULS.length();
            if (iTmpLength < iLengthMaxForULS) {
                sbULS.append(vUlsInfo);
            } else {
                sbULS.append(vUlsInfo.substring(iLengthMaxForULS - 3));
                sULS = sULS.concat("...");
            }
        }
        sULS = sbULS.toString();
        return sULS;
    }

    /**
     * Generate instructions to draw the LOGO block
     * @throws Exception
     */
    private void CART_BEP_generation_logo() throws Exception {
        Vector<String> vChars = new Vector<String>();
        try {
            // LOGO Faurecia
            generateElementarySection("LOGO", vChars);
        } catch (Exception e) {
            logger.error("Error in CART_BEP_generation_logo()\n", e);
            throw e;
        }
        return;
    }

    /**
     * Generate instructions to draw the Parts block
     * @param vListParts
     *            : Parts informations
     * @param isRelatedPartBlock
     *            : Page file containing instructions
     * @throws Exception
     */
    private void CART_BEP_generation_zone_articles(Vector<Hashtable<String, Object>> vListParts, InputStream isRelatedPartBlock) throws Exception {
        // CHARGEMENT DU FICHIER MODELE
        try {
            if (vListParts != null) {
                CART_BEP_sauvegarde_section(isRelatedPartBlock);
                // recherche du delta pour la partie multiplicative
                float delta = Float.parseFloat(CART_BEP_valeur_s_init("delta"));
                float height = Float.parseFloat(CART_BEP_valeur_s_init("height"));
                Vector<String> vChars = new Vector<String>();
                int iLinesNumber = 0;
                boolean bFirstPart = true;
                for (int iCpt = (vListParts.size() - 1); iCpt >= 0; iCpt--) {
                    if (!bFirstPart) {
                        this.ycart = this.ycart + delta;
                    } else {
                        bFirstPart = false;
                    }
                    Hashtable<String, Object> htPartLine = vListParts.get(iCpt);
                    String sState = (String) htPartLine.get("STATE");
                    sState = sState.replaceAll("\\n", "");
                    String[] arrState = sState.split("(<br\\/>)");
                    Vector<String> vState = new Vector<String>(Arrays.asList(arrState));
                    iLinesNumber = vState.size();
                    String sMaterial = (String) htPartLine.get("MATERIAL");
                    sMaterial = sMaterial.replaceAll("\\n", "");
                    String[] arrMaterials = sMaterial.split("(<br\\/>)");
                    Vector<String> vMaterials = new Vector<String>(Arrays.asList(arrMaterials));
                    iLinesNumber = (vMaterials.size() > iLinesNumber) ? vMaterials.size() : iLinesNumber;
                    logger.debug("CART_BEP_generation_zone_articles() - vMaterials = <" + vMaterials + ">");
                    String sMaterialNorm = (String) htPartLine.get("MATERIALNORM");
                    sMaterialNorm = sMaterialNorm.replaceAll("\\n", "");
                    String[] arrMaterialsNorm = sMaterialNorm.split("(<br\\/>)");
                    Vector<String> vMaterialsNorm = new Vector<String>(Arrays.asList(arrMaterialsNorm));
                    iLinesNumber = (vMaterialsNorm.size() > iLinesNumber) ? vMaterialsNorm.size() : iLinesNumber;
                    logger.debug("CART_BEP_generation_zone_articles() - vMaterialsNorm = <" + vMaterialsNorm + ">");
                    String sTreatement = (String) htPartLine.get("TREATMENT");
                    sTreatement = sTreatement.replaceAll("\\n", "");
                    String[] arrTreatement = sTreatement.split("(<br\\/>)");
                    Vector<String> vTreatement = new Vector<String>(Arrays.asList(arrTreatement));
                    iLinesNumber = (vTreatement.size() > iLinesNumber) ? vTreatement.size() : iLinesNumber;
                    logger.debug("CART_BEP_generation_zone_articles() - TREATMENT = <" + vTreatement + ">");

                    if (iLinesNumber > 1) {
                        for (int iLineNb = (iLinesNumber - 1); iLineNb >= 1; iLineNb--) {
                            sState = (vState.size() > iLineNb) ? vState.elementAt(iLineNb) : "";
                            sMaterial = (vMaterials.size() > iLineNb) ? vMaterials.elementAt(iLineNb) : "";
                            sMaterialNorm = (vMaterialsNorm.size() > iLineNb) ? vMaterialsNorm.elementAt(iLineNb) : "";
                            sTreatement = (vTreatement.size() > iLineNb) ? vTreatement.elementAt(iLineNb) : "";
                            vChars.clear();
                            vChars.addElement("");
                            vChars.addElement("");
                            vChars.addElement("");
                            vChars.addElement("");
                            vChars.addElement(sState);
                            vChars.addElement(sMaterial);
                            vChars.addElement(sMaterialNorm);
                            vChars.addElement(sTreatement);
                            generateElementarySection("MAIN", vChars);
                            this.ycart = this.ycart + delta;
                        }
                    }
                    sState = "";
                    if (vState.size() > 0) {
                        sState = vState.elementAt(0);
                    }
                    sMaterial = "";
                    sMaterialNorm = "";
                    sTreatement = "";
                    if (vMaterials.size() > 0) {
                        sMaterial = vMaterials.elementAt(0);
                        sMaterialNorm = vMaterialsNorm.elementAt(0);
                        sTreatement = vTreatement.elementAt(0);
                    }
                    vChars.clear();
                    // Part Name and Rev
                    if (!(htPartLine.get("NAME")).equals("")) {
                        vChars.addElement((String) htPartLine.get("NAME") + "-" + (String) htPartLine.get("REVISION"));
                    } else {
                        vChars.addElement((String) htPartLine.get("NAME") + "" + (String) htPartLine.get("REVISION"));
                    }
                    // Unused
                    vChars.addElement("");
                    // Mass
                    vChars.addElement((String) htPartLine.get("MASS"));
                    // Symetric
                    vChars.addElement((String) htPartLine.get("SYMETRIC") + "-" + (String) htPartLine.get("SYMREVISION"));
                    // State
                    vChars.addElement(sState);

                    // Material
                    vChars.addElement(sMaterial);
                    // Norm
                    vChars.addElement(sMaterialNorm);
                    // Treatment
                    vChars.addElement(sTreatement);
                    generateElementarySection("MAIN", vChars);

                    // Inpair Cross if necessary
                    String sValue = (String) htPartLine.get("INPAIR");
                    if ("Yes".equalsIgnoreCase(sValue)) {
                        vChars.clear();
                        vChars.addElement(sValue);
                        if (iLinesNumber == 1) {
                            generateElementarySection("INPAIR", vChars);
                        } else {
                            generateElementarySectionX("INPAIR", vChars);
                        }
                    }
                    sValue = (String) htPartLine.get("PARTSAFETYCLASS");
                    if ("1".equalsIgnoreCase(sValue)) {
                        vChars.clear();
                        vChars.addElement(sValue);
                        if (iLinesNumber == 1) {
                            generateElementarySection("PART_SAFETY_CLASS_1", vChars);
                        } else {
                            generateElementarySectionX("PART_SAFETY_CLASS_1", vChars);
                        }
                    }
                    if ("2".equalsIgnoreCase(sValue)) {
                        vChars.clear();
                        vChars.addElement(sValue);
                        if (iLinesNumber == 1) {
                            generateElementarySection("PART_SAFETY_CLASS_2", vChars);
                        } else {
                            generateElementarySectionX("PART_SAFETY_CLASS_2", vChars);
                        }
                    }
                    sValue = (String) htPartLine.get("MATERIALSAFETYCLASS");
                    if ("1".equalsIgnoreCase(sValue)) {
                        vChars.clear();
                        vChars.addElement(sValue);
                        if (iLinesNumber == 1) {
                            generateElementarySection("MATERIAL_SAFETY_CLASS_1", vChars);
                        } else {
                            generateElementarySectionX("MATERIAL_SAFETY_CLASS_1", vChars);
                        }
                    }
                    if ("2".equalsIgnoreCase(sValue)) {
                        vChars.clear();
                        vChars.addElement(sValue);
                        if (iLinesNumber == 1) {
                            generateElementarySection("MATERIAL_SAFETY_CLASS_2", vChars);
                        } else {
                            generateElementarySectionX("MATERIAL_SAFETY_CLASS_2", vChars);
                        }
                    }
                    sValue = (String) htPartLine.get("TREATSAFETYCLASS");
                    if ("1".equalsIgnoreCase(sValue)) {
                        vChars.clear();
                        vChars.addElement(sValue);
                        if (iLinesNumber == 1) {
                            generateElementarySection("TREATMENT_SAFETY_CLASS_1", vChars);
                        } else {
                            generateElementarySectionX("TREATMENT_SAFETY_CLASS_1", vChars);
                        }
                    }
                    if ("2".equalsIgnoreCase(sValue)) {
                        vChars.clear();
                        vChars.addElement(sValue);
                        if (iLinesNumber == 1) {
                            generateElementarySection("TREATMENT_SAFETY_CLASS_2", vChars);
                        } else {
                            generateElementarySectionX("TREATMENT_SAFETY_CLASS_2", vChars);
                        }
                    }
                    vChars.clear();
                    generateElementarySection("SEPARATEUR", vChars);
                    this.ycart = this.ycart + delta;

                }
                if (vListParts.size() > 0) {
                    vChars.clear();
                    generateElementarySection("HEADER", vChars);
                }
                this.ycart = this.ycart + height;
            }

        } catch (Exception e) {
            logger.error("Error in CART_BEP_generation_zone_articles()\n", e);
            throw e;
        }
        return;
    }

    /**
     * Generate instructions to draw the Basis Definition block
     * @param vBasisInfos
     *            : Basis Definition information
     * @param isRelatedBasisDefinitionBlock
     *            : Page file containing instructions
     * @throws Exception
     */
    private void generateBasisDefinitionInfo(Vector<Hashtable<String, Object>> vBasisInfos, InputStream isRelatedBasisDefinitionBlock) throws Exception {

        try {
            if (vBasisInfos != null && vBasisInfos.size() > 0) {
                // CHARGEMENT DU FICHIER MODELE
                CART_BEP_sauvegarde_section(isRelatedBasisDefinitionBlock);
                // recherche du delta pour la partie multiplicative
                float delta = Float.parseFloat(CART_BEP_valeur_s_init("delta"));
                Vector<String> vChars = new Vector<String>();
                Hashtable<String, Object> htBasisDef;
                for (int iCpt = 1; iCpt < vBasisInfos.size(); iCpt++) {
                    htBasisDef = vBasisInfos.get(iCpt);
                    String sBasisDefName = (String) htBasisDef.get("NAME");
                    String sBasisDefRev = (String) htBasisDef.get("REVISION");
                    String sFormattedName = getFormattedBasisDefinitionName(sBasisDefName, sBasisDefRev);

                    // Formattage de la description
                    String sBasisDefDescription = (String) htBasisDef.get("DESCRIPTION");
                    String[] arrBasisDefDesc = sBasisDefDescription.split("(<br\\/>)");
                    Vector<String> vDescription = new Vector<String>(Arrays.asList(arrBasisDefDesc));
                    // Rebuild description in splitting description
                    int iMaxNbOfCharactersPerLine = pss.cad2d3d.TitleBlockUtil_mxJPO.getMaxNumberOfCharactersPerLine("BASIS_DEF_DESCRIPTION", this.htInstructions);
                    if (iMaxNbOfCharactersPerLine > 0) {
                        vDescription = pss.cad2d3d.TitleBlockUtil_mxJPO.reformatLines(vDescription, iMaxNbOfCharactersPerLine);
                    }

                    int iLineNumber = vDescription.size();
                    for (int i = iLineNumber; i > 0; i--) {
                        String sDesc = vDescription.get(i - 1);

                        // Vertical line
                        vChars.clear();
                        generateElementarySection("LINE_VERTICAL", vChars);

                        if (i == 1) {
                            vChars.clear();
                            vChars.addElement(sFormattedName);
                            generateElementarySection("BASIS_DEF_NAME", vChars);

                            vChars.clear();
                            vChars.addElement((String) htBasisDef.get("STATE"));
                            generateElementarySection("BASIS_DEF_STATE", vChars);
                        }
                        vChars.clear();
                        vChars.addElement(sDesc);
                        generateElementarySection("BASIS_DEF_DESCRIPTION", vChars);

                        if (i == 1) {
                            // Structure de ligne (vertical)
                            vChars.clear();
                            generateElementarySection("LINE_HORIZONTAL", vChars);
                        }

                        this.ycart = this.ycart + delta;
                    }

                }

                // Generation du header
                vChars.clear();
                generateElementarySection("HEADER", vChars);
                float height = Float.parseFloat(CART_BEP_valeur_s_init("height"));
                this.ycart = this.ycart + height;
            }
        } catch (Exception e) {
            logger.error("Error in generateBasisDefinitionInfo()\n", e);
            throw e;
        }
    }

    /**
     * Generate instructions to draw and generate the CAD file
     * @param givenPath
     * @param htTitleBlockInformations
     * @param sInfo
     * @param sOutputFileName
     * @param bULS
     * @param bCAD
     * @param bNissanRenault
     */
    @SuppressWarnings("unchecked")
    private String generateTitleblock(Context context, String givenPath, Hashtable<String, Object> htTitleBlockInformations, String sInfo, String sOutputFileName, boolean bULS, boolean bCAD,
            boolean bNissanRenault) throws Exception {
        // logger.debug("start generateTitleblock");
        String sCartoucheName = givenPath + "/" + sOutputFileName;
        try {
            String sISUG = (String) htTitleBlockInformations.get("CADMODEL_ISUG");
            this.isUG = sISUG != null ? "true".equalsIgnoreCase(sISUG) : false;
            this.sUser = context.getUser();
            this.initTranslator(Page.getContentsAsStream(context, TITLEBLOCK_TRANSLATION_PAGE));
            // On recupere le FORMAT_PAPIER et on calcule les heights verticaux
            // On supprime systematiquement 15 mm (2x5+5) pour eviter que le cartouche ne deborde dans lacadre du plan...
            if (this.isCheckout) {
                this.cart_bep_hauteur_max_y = 1000;
            } else {
                try {
                    // tBlockTemplate.cart_bep_hauteur_max_y = Float.parseFloat(tBlockTemplate.translateValue("PLOT_HEIGHT_Y", sCADModelAttFormatPapier));
                } catch (NumberFormatException ex) {
                    throw new NumberFormatException("The format of the Viewable is not valid.");
                }
            }
            this.cart_bep_hauteur_max_y = this.cart_bep_hauteur_max_y - 15;
            // Creation du fichier txt decrivant le cartouche
            File fl = new File(sCartoucheName);
            FileOutputStream IS = new FileOutputStream(fl);
            OutputStreamWriter readers = new OutputStreamWriter(IS, StandardCharsets.UTF_8);
            cartouche_fd = new BufferedWriter(readers);
            // Generation de la partie Informative
            this.ycart = 0;
            if (!"NONE".equals(sInfo)) {
                // CHARGEMENT DU FICHIER MODELE
                CART_BEP_sauvegarde_section(Page.getContentsAsStream(context, TITLEBLOCK_CAD_INFO_BLOCK_PAGE));
                // generation du bandeau d'information...
                Vector<String> vChars = new Vector<String>();
                vChars.addElement(sInfo);
                generateElementarySection("INFO", vChars);
            }
            // Generation de la partie Principale
            this.ycart = 0;
            if (bCAD) {
                InputStream isPlanModelBlock = null;
                if (this.isCheckout && (!this.isCopy)) {
                    isPlanModelBlock = Page.getContentsAsStream(context, isUG ? TITLEBLOCK_CAD_PLAN_BLOCK_CHECKOUT_PAGE_UG : TITLEBLOCK_CAD_PLAN_BLOCK_CHECKOUT_PAGE);
                } else {
                    isPlanModelBlock = Page.getContentsAsStream(context, isUG ? TITLEBLOCK_CAD_PLAN_BLOCK_PAGE_UG : TITLEBLOCK_CAD_PLAN_BLOCK_PAGE);
                }
                CART_BEP_generation_zone_plan(htTitleBlockInformations, isPlanModelBlock, bULS);
                // LOGO Faurecia
                CART_BEP_generation_logo();
            }

            // Generation de du bloc Nissan-Renault (RFC21923)
            if (bNissanRenault) {
                CART_BEP_generation_zone_nissan_renault(Page.getContentsAsStream(context, TITLEBLOCK_NISSANRENAULT_PAGE));
            }

            // Generation de la partie Basis Definition
            Vector<Hashtable<String, Object>> vTemp = (Vector<Hashtable<String, Object>>) htTitleBlockInformations.get("BasisInformation");
            InputStream isRelatedBasisDefinitionBlock = Page.getContentsAsStream(context, isUG ? TITLEBLOCK_RELATED_BASIS_DEFINITION_BLOCK_PAGE_UG : TITLEBLOCK_RELATED_BASIS_DEFINITION_BLOCK_PAGE);
            generateBasisDefinitionInfo(vTemp, isRelatedBasisDefinitionBlock);
            // GENERATION DE LA PARTIE PART
            vTemp = (Vector<Hashtable<String, Object>>) htTitleBlockInformations.get("PartsLinked");
            if (vTemp != null) {
                InputStream isRelatedPartBlock = Page.getContentsAsStream(context, isUG ? TITLEBLOCK_PART_INFO_BLOCK_PAGE_UG : TITLEBLOCK_PART_INFO_BLOCK_PAGE);
                CART_BEP_generation_zone_articles(vTemp, isRelatedPartBlock);
            }

            // GENERATION DE LA PARTIE HISTORY
            if (htTitleBlockInformations.containsKey("HistoryInfos")) {
                Vector<Hashtable<String, Object>> vHistoryInfos = (Vector<Hashtable<String, Object>>) htTitleBlockInformations.get("HistoryInfos");
                InputStream isECOBlockPage = Page.getContentsAsStream(context, isUG ? TITLEBLOCK_ECO_BLOCK_PAGE_UG : TITLEBLOCK_ECO_BLOCK_PAGE);
                generateHistoricPart(vHistoryInfos, isECOBlockPage);
            }
            if (htTitleBlockInformations.containsKey("HistoryInfosForBasisDefinition")) {
                Vector<Hashtable<String, String>> v_HistoryInfos = (Vector<Hashtable<String, String>>) htTitleBlockInformations.get("HistoryInfosForBasisDefinition");
                InputStream inBasisDefinitionBlock = Page.getContentsAsStream(context, isUG ? TITLEBLOCK_BASIS_DEFINITION_BLOCK_PAGE_UG : TITLEBLOCK_BASIS_DEFINITION_BLOCK_PAGE);
                generateHistoricBasisDefinition(v_HistoryInfos, inBasisDefinitionBlock);
            }
            this.ycart = 0;
            // CART_BEP_generation_logo();
            cartouche_fd.flush();
            cartouche_fd.close();
        } catch (Exception e) {
            logger.error("Error in generateTitleblock()\n", e);
            throw e;
        }
        logger.debug("End generateTitleblock");
        return sCartoucheName;
    }

    /**
     * Get Basis Definition name to display on the title-block
     * @param sBasisDefName
     * @param sBasisDefRev
     * @return
     */
    private String getFormattedBasisDefinitionName(String sBasisDefName, String sBasisDefRev) {
        StringBuilder sbFormatedName = new StringBuilder();
        int iMaxSize = 40;
        int iNameSize = sBasisDefName.length();
        int iRevSize = sBasisDefRev.length();
        logger.debug("getFormattedBasisDefinitionName() - iMaxSize = <" + iMaxSize + "> iNameSize = <" + iNameSize + "> iRevSize = <" + iRevSize + ">");

        if ((iNameSize + iRevSize) < iMaxSize) {
            sbFormatedName.append(sBasisDefName).append(" ").append(sBasisDefRev);
        } else {
            int iSize = iMaxSize - (iRevSize + 1) - 1; // we remove only one character for the three points
            logger.debug("getFormattedBasisDefinitionName() - iSize = <" + iSize + ">");
            sbFormatedName.append(sBasisDefName.substring(0, iSize)).append("...");
            sbFormatedName.append(" ").append(sBasisDefRev);
            logger.debug("getFormattedBasisDefinitionName() - sbFormatedName = <" + sbFormatedName + ">");
        }

        return sbFormatedName.toString();
    }

    /***
     * Write line to design X section
     * @param section
     *            : Section name
     * @param vArgs
     *            : contains coordinates lines
     * @throws Exception
     */
    private void generateElementarySectionX(String section, Vector<String> vArgs) throws Exception {
        try {
            if (this.htInstructions.containsKey(section)) {
                Vector<String> vLines = this.htInstructions.get(section);
                String sLine = null;
                for (int iCpt = 0; iCpt < vLines.size(); iCpt++) {
                    sLine = (vLines.get(iCpt)).replace("LINE", "XLINE");
                    writeLine(sLine, vArgs);
                }
            }
        } catch (Exception e) {
            logger.error("Error in generateElementarySectionX()\n", e);
            throw e;
        }
        return;
    }

    /**
     * Get label translation
     * @param sSection
     *            : Section name
     * @param sKey
     *            : label
     * @return
     * @throws Exception
     */
    private String translateValue(String sSection, String sKey) throws Exception {
        String sValue = "";
        try {
            if (htTranslations.containsKey(sSection + " - " + sKey)) {
                sValue = htTranslations.get(sSection + " - " + sKey);
            } else {
                sValue = sKey;
            }
        } catch (Exception e) {
            logger.error("Error in translateValue()\n", e);
            throw e;
        }
        return sValue;
    }

    /**
     * Read translation page file and initialize hash table
     * @param isTranslationFile
     *            : translation page file
     * @throws Exception
     */
    private void initTranslator(InputStream isTranslationFile) throws Exception {
        logger.debug("initTranslator start");
        try {
            Reader readers = new InputStreamReader(isTranslationFile, StandardCharsets.UTF_8);
            BufferedReader buf = new BufferedReader(readers);

            String lineRead = buf.readLine();
            String section = "DEFAULT";
            while (lineRead != null) {
                lineRead = lineRead.trim();
                // lecture d'une ligne dans le fichier modele
                if ("".equals(lineRead) || "*".equals(lineRead)) {
                } else if ("[END]".equals(lineRead)) {
                    section = "DEFAULT";
                } else if (lineRead.indexOf("[") == 0) {
                    section = lineRead.substring(1, lineRead.length() - 1);
                } else {
                    StringTokenizer token = new StringTokenizer(lineRead, "\t");
                    String sLang = "";
                    String sKey = "";
                    String sValue = "";
                    if (token.countTokens() == 2) {
                        sKey = token.nextToken();
                        sValue = token.nextToken();
                    } else if (token.countTokens() == 3) {
                        sLang = token.nextToken();
                        sKey = token.nextToken();
                        sValue = token.nextToken();
                    }
                    if ("".equals(sLang) || this.sLanguageUsed.equals(sLang)) {
                        if ("NULL".equals(sValue))
                            sValue = "";
                        htTranslations.put(section + " - " + sKey, sValue);
                    }
                }
                lineRead = buf.readLine();
            }
            buf.close();
        } catch (RuntimeException e) {
            logger.error("Error in initTranslator: ", e);
            throw e;
        } catch (Exception e) {
            logger.error("Error in initTranslator: ", e);
            throw e;
        }
        logger.debug("htTranslations:" + htTranslations);
        logger.debug("initTranslator end");
        return;
    }

    /**
     * Generate the .cad file and check-in them on the derived output object. Also
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param doCADObject
     *            CAD Drawing object
     * @param strDrawingName
     *            CAD Drawing name
     * @param args
     *            Title Block options selected
     * @param strCheckoutDir
     *            the directory name where files are created
     * @throws Exception
     */
    public void createTitleBlockAndCheckinCAD(Context context, DomainObject doCADObject, String strDrawingName, String sDerivedOutputId, String[] args, String strCheckoutDir) throws Exception {
        try {
            logger.debug("createTitleBlockAndCheckinCAD() - sDerivedOutputId = <" + sDerivedOutputId + ">");
            String sCADModelId = args[0];
            String strPattern = args[1];
            String strDrawinghistory = args[2];
            String strNoRevision = args[3];
            String strLinkedParts = args[4];
            String strULS = args[5];

            boolean bNissanRenault = TigerEnums.TitleBlockPattern.RENAULTNISSAN.toString().equalsIgnoreCase(strPattern);
            // tBlockTemplate.cart_bep_nissan_renault = bNissanRenault;
            // String strTemplatePath = EnoviaResourceBundle.getProperty(context, SUITE_KEY_IEF_DESIGN, context.getLocale(), "emxIEFDesignCenter.Common.TemplatePath");
            // strTemplatePath = strTemplatePath + "/";
            // tBlockTemplate.sPathForTemplates = strTemplatePath;
            // tBlockTemplate.sLanguageUsed = "US";
            // tBlockTemplate.initTranslator("US");
            boolean bULS = TigerEnums.TitleBlockPattern.FASFAE.toString().equalsIgnoreCase(strPattern) && TigerConstants.STRING_YES.equalsIgnoreCase(strULS);
            boolean bCAD = !TigerEnums.TitleBlockPattern.FCM.toString().equalsIgnoreCase(strPattern);

            DomainObject doObject = DomainObject.newInstance(context, sCADModelId);
            Hashtable<String, Object> htTitleBlockInformations = getTitleblockInformations(context, sCADModelId, sDerivedOutputId, strCheckoutDir, strPattern, strDrawinghistory, strNoRevision,
                    strLinkedParts, 100, doObject.isKindOf(context, TigerConstants.TYPE_PSS_UGDRAWING));
            String sFolioNumber = (String) htTitleBlockInformations.get("CADMODEL_NBOFFOLIOS");

            if (UIUtil.isNotNullAndNotEmpty(sFolioNumber)) {
                logger.debug("createTitleBlockAndCheckinCAD() - sFolioNumber = <" + sFolioNumber + ">");
                int iFolioNumber = Integer.parseInt(sFolioNumber.trim());
                pss.cad2d3d.TitleBlockCADFileGenerator_mxJPO titleBlockGen = new pss.cad2d3d.TitleBlockCADFileGenerator_mxJPO();
                String sCADFileName = null;
                String sCartoucheFileName = null;
                BusinessObject domObjDerived = new BusinessObject(sDerivedOutputId);
                for (int i = 1; i <= iFolioNumber; i++) {
                    // CAD file name
                    sCADFileName = String.format("CAD_%s_%d.cad", strDrawingName, i);
                    // set sheet number (case of several sheets)
                    htTitleBlockInformations.put("VIEWABLE_FOLIO", String.valueOf(i));
                    sCartoucheFileName = titleBlockGen.generateTitleblock(context, strCheckoutDir, htTitleBlockInformations, "Title Block", sCADFileName, bULS, bCAD, bNissanRenault);
                    logger.debug("createTitleBlockAndCheckinCAD() - sCartoucheFileName = <" + sCartoucheFileName + ">");
                    // check-in CAD file in drawing object start
                    PropertyUtil.setRPEValue(context, ContextUtil.MX_LOGGED_IN_USER_NAME + "-" + sDerivedOutputId, "true", true);
                    domObjDerived.checkinFile(context, false, true, "", "generic", sCADFileName, strCheckoutDir);
                    // START :: fix for impact from TIGTK-17327 :: ALM-6162 :: Rename PDF
                    PropertyUtil.setRPEValue(context, ContextUtil.MX_LOGGED_IN_USER_NAME + "-" + sDerivedOutputId, "false", true);
                    // END :: fix for impact from TIGTK-17327 :: ALM-6162 :: Rename PDF
                }
            }
            // create LST file too
            createLSTFile(context, doCADObject, sDerivedOutputId, strCheckoutDir);
        } catch (Exception e) {
            logger.error("Error in createTitleBlockAndCheckinCAD()\n", e);
            throw e;
        }
    }

    /**
     * Create LST file
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param busObject
     *            CAD Drawing object
     * @param sDerivedOutputId
     *            the Derived Output object ID
     * @param strCheckoutDir
     *            the directory name where files are created
     * @throws Exception
     */
    private void createLSTFile(Context context, DomainObject doCADObject, String sDerivedOutputId, String strCheckoutDir) throws Exception {
        final String TYPE_UG_DRAWING = TigerConstants.TYPE_UGDRAWING;
        final String ATTRIBUTE_PSS_CAD_FILE_FORMAT = TigerConstants.ATTRIBUTE_PSS_CAD_FILE_FORMAT;
        final String ATTRIBUTE_PSS_DIMENSIONINGSTANDARDS1 = TigerConstants.ATTRIBUTE_PSS_DIMENSIONINGSTANDARDS1;
        final String ATTRIBUTE_PSS_DIMENSIONINGSTANDARDS2 = TigerConstants.ATTRIBUTE_PSS_DIMENSIONINGSTANDARDS2;
        final String ATTRIBUTE_PSS_DIMENSIONINGSTANDARDS3 = TigerConstants.ATTRIBUTE_PSS_DIMENSIONINGSTANDARDS3;
        final String ATTRIBUTE_PSS_DIMENSIONINGSTANDARDS4 = TigerConstants.ATTRIBUTE_PSS_DIMENSIONINGSTANDARDS4;
        final String ATTRIBUTE_PSS_UNDIMENSIONEDRADIUS = TigerConstants.ATTRIBUTE_PSS_UNDIMENSIONEDRADIUS;
        final String ATTRIBUTE_PSS_ANGULARTOLERANCE = TigerConstants.ATTRIBUTE_PSS_ANGULARTOLERANCE;
        final String ATTRIBUTE_PSS_LINEARTOLERANCE = TigerConstants.ATTRIBUTE_PSS_LINEARTOLERANCE;
        final String ATTRIBUTE_PSS_SCALE = TigerConstants.ATTRIBUTE_PSS_SCALE;
        final String ATTRIBUTE_PSS_DRAWINGFORMAT = TigerConstants.ATTRIBUTE_PSS_DRAWINGFORMAT;
        final String ATTRIBUTE_PSS_FOLIONUMBER = TigerConstants.ATTRIBUTE_PSS_FOLIONUMBER;
        final String ATTRIBUTE_PSS_DRAWINGVIEWCONVENTION = TigerConstants.ATTRIBUTE_PSS_DRAWINGVIEWCONVENTION;

        // build the list of the cad drawing attributes
        StringList slSelect = new StringList();
        slSelect.addElement(ATTRIBUTE_PSS_CAD_FILE_FORMAT);
        slSelect.addElement(ATTRIBUTE_PSS_SCALE);
        slSelect.addElement(ATTRIBUTE_PSS_DRAWINGVIEWCONVENTION);
        slSelect.addElement(ATTRIBUTE_PSS_ANGULARTOLERANCE);
        slSelect.addElement(ATTRIBUTE_PSS_LINEARTOLERANCE);
        slSelect.addElement(ATTRIBUTE_PSS_DIMENSIONINGSTANDARDS1);
        slSelect.addElement(ATTRIBUTE_PSS_DIMENSIONINGSTANDARDS2);
        slSelect.addElement(ATTRIBUTE_PSS_DIMENSIONINGSTANDARDS3);
        slSelect.addElement(ATTRIBUTE_PSS_DIMENSIONINGSTANDARDS4);
        slSelect.addElement(ATTRIBUTE_PSS_UNDIMENSIONEDRADIUS);
        slSelect.addElement(ATTRIBUTE_PSS_DRAWINGFORMAT);
        slSelect.addElement(ATTRIBUTE_PSS_FOLIONUMBER);

        // Get the attribute values
        AttributeList cadAttribListVal = doCADObject.getAttributeValues(context, slSelect, true);
        logger.debug("createLSTFile() - cadAttribListVal = <" + cadAttribListVal + ">");

        StringBuilder sbLSTContent = new StringBuilder();
        sbLSTContent.append("[PLAN]").append("\n");

        Attribute cadAttrib = null;
        String sAttributeName = null;
        String sAttributeValue = null;
        AttributeItr itr = new AttributeItr(cadAttribListVal);
        while (itr.next()) {
            cadAttrib = itr.obj();
            sAttributeName = cadAttrib.getName();
            sAttributeValue = cadAttrib.getValue();
            logger.debug("createLSTFile() - sAttributeName = <" + sAttributeName + "> sAttributeValue = <" + sAttributeValue + ">");
            if (ATTRIBUTE_PSS_CAD_FILE_FORMAT.equals(sAttributeName)) {
                sbLSTContent.append("cad_format=").append(sAttributeValue).append("\n");
            } else if (ATTRIBUTE_PSS_SCALE.equals(sAttributeName)) {
                sbLSTContent.append("echelle=").append(sAttributeValue).append("\n");
            } else if (ATTRIBUTE_PSS_DRAWINGVIEWCONVENTION.equals(sAttributeName)) {
                sbLSTContent.append("type_vue=").append(sAttributeValue).append("\n");
            } else if (ATTRIBUTE_PSS_DIMENSIONINGSTANDARDS1.equals(sAttributeValue)) {
                sbLSTContent.append("dimensioning1=").append(sAttributeValue).append("\n");
            } else if (ATTRIBUTE_PSS_DIMENSIONINGSTANDARDS2.equals(sAttributeName)) {
                sbLSTContent.append("dimensioning2=").append(sAttributeValue).append("\n");
            } else if (ATTRIBUTE_PSS_DIMENSIONINGSTANDARDS3.equals(sAttributeName)) {
                sbLSTContent.append("dimensioning3=").append(sAttributeValue).append("\n");
            } else if (ATTRIBUTE_PSS_DIMENSIONINGSTANDARDS4.equals(sAttributeName)) {
                sbLSTContent.append("dimensioning4=").append(sAttributeValue).append("\n");
            } else if (ATTRIBUTE_PSS_ANGULARTOLERANCE.equals(sAttributeName)) {
                sbLSTContent.append("tol_angulaire=").append(sAttributeValue).append("\n");
            } else if (ATTRIBUTE_PSS_LINEARTOLERANCE.equals(sAttributeName)) {
                sbLSTContent.append("tol_lineaire=").append(sAttributeValue).append("\n");
            } else if (ATTRIBUTE_PSS_UNDIMENSIONEDRADIUS.equals(sAttributeName)) {
                sbLSTContent.append("rayon_non_cote=").append(sAttributeValue).append("\n");
            } else if (ATTRIBUTE_PSS_DRAWINGFORMAT.equals(sAttributeName)) {
                sbLSTContent.append("format_papier=").append(sAttributeValue).append("\n");
            } else if (ATTRIBUTE_PSS_FOLIONUMBER.equals(sAttributeName)) {
                sbLSTContent.append("nbr_folio=").append(sAttributeValue).append("\n");
            }
        }

        sbLSTContent.append("[FIN]").append("\n");

        // construct the LST file name
        String sLSTFileName = "";
        if (doCADObject.isKindOf(context, TYPE_UG_DRAWING)) {
            sLSTFileName = String.format("%s.lst", doCADObject.getInfo(context, DomainConstants.SELECT_NAME));
        } else {
            sLSTFileName = String.format("%s%s.lst", doCADObject.getInfo(context, DomainConstants.SELECT_NAME), doCADObject.getInfo(context, DomainConstants.SELECT_REVISION));
        }
        logger.debug("createLSTFile() - sLSTFileName = <" + sLSTFileName + ">");
        // create the LST file

        Writer fstream = new OutputStreamWriter(new FileOutputStream(new StringBuilder(strCheckoutDir).append(java.io.File.separator).append(sLSTFileName).toString()), StandardCharsets.UTF_8);
        BufferedWriter brLSTFile = new BufferedWriter(fstream);
        brLSTFile.write(sbLSTContent.toString());
        brLSTFile.flush();
        brLSTFile.close();
        // check-in LS file in drawing object start
        BusinessObject domObjDerived = new BusinessObject(sDerivedOutputId);
        PropertyUtil.setRPEValue(context, ContextUtil.MX_LOGGED_IN_USER_NAME + "-" + sDerivedOutputId, "true", true);
        domObjDerived.checkinFile(context, false, true, DomainConstants.EMPTY_STRING, FileFormat.GENERIC(), sLSTFileName, strCheckoutDir);
        // START :: fix for impact from TIGTK-17327 :: ALM-6162 :: Rename PDF
        PropertyUtil.setRPEValue(context, ContextUtil.MX_LOGGED_IN_USER_NAME + "-" + sDerivedOutputId, "false", true);
        // START :: fix for impact from TIGTK-17327 :: ALM-6162 :: Rename PDF
    }

    /**
     * Get CAD Drawing Title Block informations
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sCADModelId
     *            the CAD Drawing object ID
     * @param sDerivedOutputId
     *            the Derived Output object ID
     * @param strCheckoutDir
     *            the directory name where files are created
     * @param strPattern
     *            Title Block Pattern selected
     * @param strDrawinghistory
     *            Drawing history (Yes or No)
     * @param strNoRevision
     *            Number of revisions of the history
     * @param strLinkedParts
     *            Linked Parts options (Yes or No)
     * @param iMaxNbPartsInHistory
     *            Parts number maximum used in history (100 by default)
     * @return
     * @throws Exception
     */
    private Hashtable<String, Object> getTitleblockInformations(Context context, String sCADModelId, String sDerivedOutputId, String strCheckoutDir, String strPattern, String strDrawinghistory,
            String strNoRevision, String strLinkedParts, int iMaxNbPartsInHistory, boolean isUG) throws Exception {
        HashMap<String, Object> hmParams = new HashMap<String, Object>();
        hmParams.put("objectId", sCADModelId);
        hmParams.put("viewableId", sDerivedOutputId);
        hmParams.put("strXMLFileName", pss.cad2d3d.GenerateTitleBlock_mxJPO.XML_FILE_NAME);
        hmParams.put("strCheckoutDir", strCheckoutDir);
        hmParams.put("strPattern", strPattern);
        hmParams.put("strDrawinghistory", strDrawinghistory);
        hmParams.put("strNoRevision", strNoRevision);
        hmParams.put("strLinkedParts", strLinkedParts);
        hmParams.put("maxPartsInHistory", String.valueOf(iMaxNbPartsInHistory));
        hmParams.put("IS_UG", String.valueOf(isUG));

        String[] methodargs = JPO.packArgs(hmParams);
        pss.cad2d3d.FPDMTitleBlockInformations_mxJPO FPDMtitleBlockInfo = new pss.cad2d3d.FPDMTitleBlockInformations_mxJPO();
        Hashtable<String, Object> htTitleBlockInformations = FPDMtitleBlockInfo.retrieveTitleblockInformation(context, methodargs);
        return htTitleBlockInformations;
    }

}
