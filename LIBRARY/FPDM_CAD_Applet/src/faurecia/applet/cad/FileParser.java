package faurecia.applet.cad;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

import faurecia.util.BusObject;
import faurecia.util.DebugUtil;
import faurecia.util.Local_OS_Information;
import fpdm.applet.cad.Parameters;


/**
 *
 * This class is used to parse the parameter file and store<br>
 * the result is a list of BusObject.<br>
 * <br>
 * The parameter file should be on this format:<br>
 * OBJECT:MCAD Component|FJ01|A 	File:File A<br>
 * OBJECT:MCAD Component|FJ02|A 	File:File B<br>
 * @author rinero
 *
 */

public class FileParser {
    private Vector<BusObject> array = new Vector<BusObject>();

    private final String sIdent             = "[IDENT]";
    private final String sPlan              = "[PLAN]";
    private final String sFolioTrace        = "[FOLIO_TRACE]";
    private final String sFolioVisu         = "[FOLIO_VISU]";
    private final String sCde               = "[COMMANDES]";
    // FPDM STERIA ADD Start: MCE 02JAN2006: RFC2099: Add the checkin of session files
    private final String sSession			= "[SESSION]";
    // FPDM STERIA ADD And  : MCE 02JAN2006: RFC2099
    private final String sFin               = "[FIN]";
    //private final String sNum               = "numero";
    //private final String sIndice            = "indice";
    private final String sNom               = "nom";
    private final String sNom2D             = "nom2D";
    //private final String sSysgen            = "sysgen";
    //private final String sFormatCAO         = "format_cao";
    //private final String sCdeSysgen         = "commande_sysgen";
    private final String sFormatTrace       = "format_trace";
    private final String sFormatVisu        = "format_visu";
    //private final String sResolution        = "resolution";
    private final String sFormatPapier      = "format_papier";
    private final String sNbrFolio          = "nbr_folio";
    //private final String sEchelle           = "echelle";
    //private final String sTypeVue           = "type_vue";
    //private final String sDimensioning1     = "dimensioning1";
    //private final String sDimensioning2     = "dimensioning2";
    //private final String sDimensioning3     = "dimensioning3";
    //private final String sDimensioning4     = "dimensioning4";
    //private final String sPtsSecu           = "pts_securite";
    //private final String sPtsReglm          = "pts_reglement";
    //private final String sRayNonCote        = "rayon_non_cote";
    //private final String sTolAngl           = "tol_angulaire";
    //private final String sTolLin            = "tol_lineaire";
    private final String sNomFich           = "nom_fichier";
    private final String sTIFF              = "TIFF";
    private final String sHPGL              = "HPGL";
    private final String sPRE_INTEGRATION   = "preint";


	//Delimiter that separate the type, name and revision in the parameter file
	private final static String DELIM = "|";
    private final static String sDelim = "=";
    
    public boolean bIsTIFFOnly = true;

    /**
     * Parse the file given in parameter and<br>
     * keep the result as a list of BusObject.<br>
     * <br>
     * This object can be found thanks to the<br>
     * method enumeration() and getBusObjects();<br>
     * <br>
     * @param f : File to parse
     * @throws Exception
     */

	public FileParser(File f)
        throws Exception
    {

        String object = "";
		int rubriq=0;
        boolean bIsPreIntegrationCheckin = false;

		Vector<String> arrayIdent = new Vector<String>();
		Vector<String> arrayFolioTrace = new Vector<String>();
		Vector<String> arrayFolioVisu = new Vector<String>();
		Vector<String> arrayCmd = new Vector<String>();
	    // FPDM STERIA ADD Start: MCE 02JAN2006: RFC2099: Add the checkin of session files
		Vector<String> arraySession = new Vector<String>();
	    // FPDM STERIA ADD End  : MCE 02JAN2006: RFC2099
		Vector<String> arrayPlan = new Vector<String>();
		FileReader fileStr = new FileReader(f);
		BufferedReader fileBuf = new BufferedReader(fileStr);
		String line = fileBuf.readLine();
		String localDir = f.getParent();
		DebugUtil.debug(DebugUtil.DEBUG, "FileParser:parse", "localDir ==: " + localDir);
        boolean troisD = false;
        boolean bNeedToUpdateLSTFile = false;

		String attribName;
		//Construction des tableaux avec les valeurs du fichier parameter
	    // FPDM STERIA ADD Start: MCE 02JAN2006: RFC2099: Add the checkin of session files
		// rubriq 6 is now for session and rubriq 7 is now for FIN
		while  (line != null) {
			line = line.trim();
			if (sIdent.equals(line)) {
				rubriq = 1;
			}else if (sPlan.equals(line)) {
				rubriq = 2;
			}else if (sFolioTrace.equals(line)) {
				rubriq = 3;
			}else if (sFolioVisu.equals(line)) {
				rubriq = 4;
			}else if (sCde.equals(line)) {
				rubriq = 5;
			// FPDM STERIA ADD Start: MCE 02JAN2006: RFC2099: Add the checkin of session files
			}else if (sSession.equals(line)) {
				rubriq = 6;
			// FPDM STERIA ADD End  : MCE 02JAN2006: RFC2099
			}else if (sFin.equals(line)) {
				rubriq = 7;
			}else {
				if  (line != null && !line.equals("")) {
					switch (rubriq) {
						case 1:
							arrayIdent.addElement(line);
							break;
						case 2:
							arrayPlan.addElement(line);
							attribName = line.substring(0, line.lastIndexOf("="));
						    if (attribName.equals(sNom)) {
								troisD = true;
							}
							break;
						case 3:
							arrayFolioTrace.addElement(line);
							break;
						case 4:
							arrayFolioVisu.addElement(line);
							break;
						case 5:
							arrayCmd.addElement(line);
							break;
						case 6:
							// FPDM STERIA ADD Start : MCE 02JAN2006: RFC2099: Add the checkin of session files
							arraySession.addElement(line);
							break;
						case 7:
							// FPDM STERIA ADD End   : MCE 02JAN2006: RFC2099
							DebugUtil.debug(DebugUtil.DEBUG, "FileParser:parse", "Fin de fichier ...!!: " + line);
							break;
						default:
							DebugUtil.debug(DebugUtil.DEBUG, "FileParser:parse", "cas impossible ...!!: " + rubriq);
							DebugUtil.debug(DebugUtil.DEBUG, "FileParser:parse", "line==: " + line);
							break;
					}
				}

			}

			line = fileBuf.readLine();
		}

        if (arrayIdent.size() <= 0) {
            throw new Exception ("LST file is not correct. Object identifier is missing.");
        }
        //Get the name and the revision of the parent object
		String identNum = ((String)arrayIdent.elementAt(0)).trim();
		String identIndice = ((String)arrayIdent.elementAt(1)).trim();

        if (identNum.indexOf(sDelim) < 0 || identIndice.indexOf(sDelim) < 0) {
            throw new Exception ("LST file is not correct. Object identifier is incorrect.");
        }

        String sParentName = identNum.substring(identNum.indexOf(sDelim) + 1);
		String sParentRev = identIndice.substring(identIndice.indexOf(sDelim) + 1);
		DebugUtil.debug(DebugUtil.DEBUG, "FileParser:parse", "sParentName ==: " + sParentName);
		DebugUtil.debug(DebugUtil.DEBUG, "FileParser:parse", "sParentRev ==: " + sParentRev);

		// ======================================================
		// Loop thru the lines in "PLAN" category of the LST File
        // ======================================================
        DebugUtil.debug(DebugUtil.INFO, "FileParser:parse", "-- RETRIEVE PLAN FILES --");
        // store the name of the first file (will be used to retrieve viewables if none is in lst file)
        String firstPlanFileName = "";
        for (int i=0; i<arrayPlan.size(); i++) {

            String sLine = (String)arrayPlan.elementAt(i);

            int indexDelim = sLine.indexOf(sDelim);
            if (indexDelim < 0) {
                continue;
            }

            String sLabel = sLine.substring(0, indexDelim).trim();
            String sValue = "";
            if (sLine.length() > indexDelim) {
                sValue = sLine.substring(indexDelim + 1).trim();
            }

            if (sPRE_INTEGRATION.equalsIgnoreCase(sLabel) && "YES".equalsIgnoreCase(sValue)) {
                bIsPreIntegrationCheckin = true;
            }

            if (!sNom.equals(sLabel) && !sNom2D.equals(sLabel)) {
                continue;
            }

            // Retrieve file name
            int indexFileName = sValue.lastIndexOf("/");
            if(indexFileName<0) {
                indexFileName =  sValue.lastIndexOf("\\");
            }
            String fileName = sValue.substring(indexFileName+1, sValue.length());
            if ("".equals(firstPlanFileName)) {
                firstPlanFileName = fileName;
            }

            // Check existence of the file
            File file = new File(localDir, fileName);
            if (!file.exists()) {
                throw new Exception("file " + file.getAbsolutePath() + " does not exists.");
            }

            // ------------------------------------------------------
            //    CASE OF A 3D FILE
            //       => THE FILE WILL BE STORED IN A CAD COMPONENT
            // ------------------------------------------------------
			if (sNom.equals(sLabel)) {
			    bIsTIFFOnly = false;
				object = Parameters.TYPE_FPDM_CAD_COMPONENT + "|".concat(sParentName).concat("|").concat(sParentRev);
                DebugUtil.debug(DebugUtil.INFO, "FileParser:parse",
                    "   3D Plan \'" + fileName + "\' will be stored in the " + Parameters.TYPE_FPDM_CAD_COMPONENT + ".");

            // ------------------------------------------------------
            //    CASE OF A 2D FILE AND AT LEAST ONE 3D FILE WILL BE CHECKED-IN
            //       => THE FILE WILL BE STORED IN A CAD DRAWING
            // ------------------------------------------------------
			} else if (sNom2D.equals(sLabel) && troisD) {
			    bIsTIFFOnly = false;
                object = Parameters.TYPE_FPDM_CAD_DRAWING + "|".concat(sParentName).concat("|").concat(sParentRev);
                DebugUtil.debug(DebugUtil.INFO, "FileParser:parse",
                    "   2D Plan \'" + fileName + "\' will be stored in the " + Parameters.TYPE_FPDM_CAD_DRAWING + ".");

            // ------------------------------------------------------
            //    CASE OF A 2D FILE AND NO 3D FILE WILL BE CHECKED-IN
            //       => THE FILE WILL BE STORED IN A CAD COMPONENT
            // ------------------------------------------------------
            } else if (sNom2D.equals(sLabel)) {
                bIsTIFFOnly = false;
                object = Parameters.TYPE_FPDM_CAD_COMPONENT + "|".concat(sParentName).concat("|").concat(sParentRev);
                DebugUtil.debug(DebugUtil.INFO, "FileParser:parse",
                    "   2D Plan \'" + fileName + "\' will be stored in the " + Parameters.TYPE_FPDM_CAD_COMPONENT + ".");
			}

            // compress file and add a BusObject to the vector "array"
            this.addBusObject(object, new File (localDir, fileName));
		}

        // ======================================================
        // Loop thru the lines in "FOLIO_TRACE" category of the LST File
        // NB : files here are ".plt"
        // ======================================================
        boolean bIsAFolioTrace = false;

        DebugUtil.debug(DebugUtil.INFO, "FileParser:parse", "-- RETRIEVE FOLIO_TRACE FILES --");
		for (int i=0; i<arrayFolioTrace.size();i++) {
            String FolioTraceElmt = (String)arrayFolioTrace.elementAt(i);
			//Tester si un plt existe
            String sLabel = FolioTraceElmt.substring(0, FolioTraceElmt.indexOf(sDelim)-1);
            String sLabel10 = FolioTraceElmt.substring(0, FolioTraceElmt.indexOf(sDelim)-2);
			if (sNomFich.equals(sLabel) || sNomFich.equals(sLabel10)) {
				//Retrieve file name
                int indexFolio = FolioTraceElmt.lastIndexOf("/");
                if(indexFolio<0) {
                    indexFolio = FolioTraceElmt.lastIndexOf("\\");
                }
				String fileName = FolioTraceElmt.substring(indexFolio+1, FolioTraceElmt.length());

                // Check existence of the file
                File file = new File(localDir, fileName);
                if (!file.exists()) {
                    throw new Exception("file " + file.getAbsolutePath() + " does not exists.");
                }

				//Recuperer l'indice du fichier
                // eg: "_1"
				String sIndiceFich = fileName.substring(fileName.lastIndexOf("_"), fileName.lastIndexOf("."));

				//Construire la ligne object pour qu'elle soit traiter par la fct addBusObject
				object = Parameters.TYPE_2DVIEWABLE + "|".concat(sParentName).concat(sIndiceFich).concat("|").concat(sParentRev);

				//appel de la methode addBusObject pour le cas de 2D Viewable
                DebugUtil.debug(DebugUtil.INFO, "FileParser:parse",
                        "   FOLIO_TRACE file \'" + fileName + "\' will be stored in the " + Parameters.TYPE_2DVIEWABLE + ".");
				addBusObject(object, new File (localDir, fileName));
                bIsAFolioTrace = true;
			}
		}
        if (!bIsAFolioTrace) {
            DebugUtil.debug(DebugUtil.INFO, "FileParser:parse", "NO FOLIO_TRACE FILE FOUND IN LST FILE");
        }

        // ======================================================
        // Loop thru the lines in "FOLIO_VISU" category of the LST File
        // NB : files here are ".tif"
        // ======================================================
        boolean bIsAFolioVisu = false;

        DebugUtil.debug(DebugUtil.INFO, "FileParser:parse", "-- RETRIEVE FOLIO_VISU FILES --");
		for (int i=0; i<arrayFolioVisu.size();i++) {
            String FolioVisuElmt = (String)arrayFolioVisu.elementAt(i);
            String sLabel = FolioVisuElmt.substring(0, FolioVisuElmt.indexOf(sDelim)-1);
            String sLabel10 = FolioVisuElmt.substring(0, FolioVisuElmt.indexOf(sDelim)-2);
			//Tester si un tiff existe
			if (sNomFich.equals(sLabel) || sNomFich.equals(sLabel10)) {
                //Retrieve file name
                int indexVisuElmt = FolioVisuElmt.lastIndexOf("/");
                if(indexVisuElmt<0) {
                    indexVisuElmt = FolioVisuElmt.lastIndexOf("\\");
                }
				String fileName = FolioVisuElmt.substring(indexVisuElmt+1, FolioVisuElmt.length());

                // Check existence of the file
                File file = new File(localDir, fileName);
                if (!file.exists()) {
                    throw new Exception("file " + file.getAbsolutePath() + " does not exists.");
                }

				//Recuperer l'indice du fichier
                // eg: "_1"
				String sIndiceFich = fileName.substring(fileName.lastIndexOf("_"), fileName.lastIndexOf("."));

                //Construire la ligne object pour qu'elle soit traiter par la fct addBusObject
				object = Parameters.TYPE_2DVIEWABLE + "|".concat(sParentName).concat(sIndiceFich).concat("|").concat(sParentRev);

                //appel de la methode addBusObject pour le cas de 2D Viewable
				addBusObject(object, new File (localDir, fileName));
                bIsAFolioVisu = true;
			}
		}
        if (!bIsAFolioVisu) {
            DebugUtil.debug(DebugUtil.INFO, "FileParser:parse", "NO FOLIO_VISU FILE FOUND IN LST FILE");
        }

        // ======================================================
        // IF NO "FOLIO_VISU" NOR "FOLIO_TRACE" FILES HAVE BEEN FOUND IN LST FILE
        //  THEN : RETRIEVING ALL .tif AND .plt FILES THAT MATCH NAME OF THE FIRST PLAN FILE NAME
        // ======================================================
        if (bIsPreIntegrationCheckin && !bIsAFolioTrace && !bIsAFolioVisu) {
            DebugUtil.debug(DebugUtil.INFO, "FileParser:parse", "-- RETRIEVE FOLIO_TRACE FILES IN LOCAL DIRECTORY --");
            File fHomeDirectory = new File(localDir);

            String[] fileNames = fHomeDirectory.list();
            String firstPlanFileNameWithoutExtension = firstPlanFileName.substring(0, firstPlanFileName.lastIndexOf("."));
            DebugUtil.debug(DebugUtil.DEBUG, "FileParser:parse", "firstPlanFileNameWithoutExtension : " + firstPlanFileNameWithoutExtension);
            Vector<String> vListOfTraceFiles = new Vector<String>();
            Vector<String> vListOfVisuFiles = new Vector<String>();
			String sStartofName = "";
			String sPDMAppli = Local_OS_Information.getUnixEnv("MX_ENV");
            if ("OCTOPUS".equals(sPDMAppli)) {
                sStartofName = sParentName.concat(sParentRev);
               DebugUtil.debug(DebugUtil.DEBUG, "FileParser:parse", "sStartofName for OCTOPUS : " + sStartofName);
            } else if ("CHEOPS".equals(sPDMAppli)) {
                sStartofName = firstPlanFileNameWithoutExtension.substring(0, firstPlanFileName.indexOf("."));
               DebugUtil.debug(DebugUtil.DEBUG, "FileParser:parse", "sStartofName for No CHEOPS : " + sStartofName);
            } else {
                sStartofName = firstPlanFileNameWithoutExtension;
               DebugUtil.debug(DebugUtil.DEBUG, "FileParser:parse", "sStartofName for No OCTOPUS : " + sStartofName);
            }
            for (int i = 0; i < fileNames.length; i++) {
                String tempFileName = fileNames[i];
                if ( tempFileName.startsWith(sStartofName)) {
                    if (tempFileName.endsWith(".plt")) {
                        vListOfTraceFiles.addElement(tempFileName);
                    }else if (tempFileName.endsWith(".tif")) {
                        vListOfVisuFiles.addElement(tempFileName);
                    }
                }
            }

            if (vListOfTraceFiles.size() > 0 && vListOfVisuFiles.size() > 0) {
                // --------------------------------------------
                // Add all trace files to the list of folio to check-in
                // --------------------------------------------
                Enumeration eEnum = vListOfTraceFiles.elements();
                while (eEnum.hasMoreElements()) {
                    String sPLTFileName = (String)eEnum.nextElement();
                    String sTIFFileName = sPLTFileName.substring(0, (sPLTFileName.length() - 4)) + ".tif";
                    if (!vListOfVisuFiles.contains(sTIFFileName)) {
                        DebugUtil.debug(DebugUtil.INFO, "FileParser:parse", "  FOLIO_TRACE \'"+sPLTFileName+"\' HAS NO EQUIVALENT TIF FILE, IT WILL NOT BE CHECKED-IN.");
                        continue;
                    }
                    DebugUtil.debug(DebugUtil.INFO, "FileParser:parse", "  FOLIO_TRACE \'"+sPLTFileName+"\' WILL BE CHECKED-IN.");

                    //Recuperer l'indice du fichier
                    // eg: "1"
                    String sIndiceFich = sPLTFileName.substring(sPLTFileName.lastIndexOf("_") + 1, sPLTFileName.lastIndexOf("."));

                    //Construire la ligne object pour qu'elle soit traiter par la fct addBusObject
                    object = Parameters.TYPE_2DVIEWABLE + "|" + sParentName + "_" + sIndiceFich + "|" + sParentRev;

                    //appel de la methode addBusObject pour le cas de 2D Viewable
                    DebugUtil.debug(DebugUtil.INFO, "FileParser:parse",
                            "   FOLIO_TRACE file \'" + sPLTFileName + "\' will be stored in the " + Parameters.TYPE_2DVIEWABLE + ".");
                    addBusObject(object, new File (localDir, sPLTFileName));

                    DebugUtil.debug(DebugUtil.INFO, "FileParser:parse",
                            "   FOLIO_VISU file \'" + sTIFFileName + "\' will be stored in the " + Parameters.TYPE_2DVIEWABLE + ".");
                    addBusObject(object, new File (localDir, sTIFFileName));

                    arrayFolioTrace.addElement(sNomFich + sIndiceFich + "=" + (new File(localDir, sPLTFileName)).getAbsolutePath());
                    arrayFolioVisu.addElement(sNomFich + sIndiceFich + "=" + (new File(localDir, sTIFFileName)).getAbsolutePath());

                    boolean bHasFormatTrace = false;
                    boolean bHasFormatPapier = false;
                    boolean bHasNbrFolio = false;
                    for (Enumeration enum2 = arrayPlan.elements(); enum2.hasMoreElements();) {
                        String sPlanLine = (String)enum2.nextElement();
                        if (sPlanLine.startsWith(sFormatTrace)) {
                            bHasFormatTrace = true;
                        } else if (sPlanLine.startsWith(sFormatVisu)) {
                        } else if (sPlanLine.startsWith(sFormatPapier)) {
                            bHasFormatPapier = true;
                        } else if (sPlanLine.startsWith(sNbrFolio)) {
                            bHasNbrFolio = true;
                        }
                    }
                    if (!bHasFormatTrace) {
                        arrayPlan.addElement(sFormatTrace + "=" + sHPGL);
                    }
                    if (!bHasFormatTrace) {
                        arrayPlan.addElement(sFormatVisu + "=" + sTIFF);
                    }
                    if (!bHasFormatPapier) {
                        arrayPlan.addElement(sFormatPapier + "=A0H");
                    }
                    if (!bHasNbrFolio) {
                        arrayPlan.addElement(sNbrFolio + "=" + arrayFolioTrace.size());
                    }


                    bNeedToUpdateLSTFile = true;
                }

            }
        }

		// FPDM STERIA ADD Start : MCE 02JAN2006: RFC2099: Add the checkin of session files
        DebugUtil.debug(DebugUtil.INFO, "FileParser:parse", "-- RETRIEVE SESSION FILES --");
        // store the name of the first file (will be used to retrieve viewable if none is in lst file)
        for (int i=0; i<arraySession.size(); i++) {

            String sLine = (String)arraySession.elementAt(i);
//            String sLabel = sLine.substring(0, sLine.indexOf(sDelim)-1);
            String sValue = sLine.substring(sLine.indexOf(sDelim)+1,sLine.length());

            // Retrieve file name
            int indexFileName = sValue.lastIndexOf("/");
            if(indexFileName<0) {
                indexFileName =  sValue.lastIndexOf("\\");
            }
            String fileName = sValue.substring(indexFileName+1, sValue.length());

            // Check existence of the file
            File file = new File(localDir, fileName);
            if (!file.exists()) {
                throw new Exception("file " + file.getAbsolutePath() + " does not exists.");
            }

			object = Parameters.TYPE_CAD_DEFINITION + "|".concat(sParentName).concat("|").concat(sParentRev);
            DebugUtil.debug(DebugUtil.INFO, "FileParser:parse",
                "  Session \'" + fileName + "\' will be stored in the " + Parameters.TYPE_CAD_DEFINITION + ".");

            // compress file and add a BusObject to the vector "array"
            this.addBusObject(object, new File (localDir, fileName));
		}
		// FPDM STERIA ADD End   : MCE 02JAN2006: RFC2099

        // --------------------------------------------
        // REBUILD LST FILE IF REQUIRED
        // --------------------------------------------
        if (bNeedToUpdateLSTFile) {
            DebugUtil.debug(DebugUtil.INFO, "FileParser:parse", "   REBUILD LST FILE.");
            FileWriter fw = new FileWriter(f.getAbsolutePath(), false);
            // Write [IDENT] CATEGORY
            fw.write(sIdent + "\n");
            DebugUtil.debug(DebugUtil.DEBUG, "FileParser:parse", sIdent);
            for (Enumeration eEnum = arrayIdent.elements(); eEnum.hasMoreElements();) {
                String sLine = (String)eEnum.nextElement();
                fw.write(sLine + "\n");
                DebugUtil.debug(DebugUtil.DEBUG, "FileParser:parse", sLine);
            }
            fw.write("\n");


            // Write [PLAN] CATEGORY
            fw.write(sPlan + "\n");
            DebugUtil.debug(DebugUtil.DEBUG, "FileParser:parse", sPlan);
            for (Enumeration eEnum = arrayPlan.elements(); eEnum.hasMoreElements();) {
                String sLine = (String)eEnum.nextElement();
                fw.write(sLine + "\n");
                DebugUtil.debug(DebugUtil.DEBUG, "FileParser:parse", sLine);
            }
            fw.write("\n");

            // Write [FOLIO_TRACE] CATEGORY
            fw.write(sFolioTrace + "\n");
            DebugUtil.debug(DebugUtil.DEBUG, "FileParser:parse", sFolioTrace);
            for (Enumeration eEnum = arrayFolioTrace.elements(); eEnum.hasMoreElements();) {
                String sLine = (String)eEnum.nextElement();
                fw.write(sLine + "\n");
                DebugUtil.debug(DebugUtil.DEBUG, "FileParser:parse", sLine);
            }
            fw.write("\n");

            // Write [FOLIO_VISU] CATEGORY
            fw.write(sFolioVisu + "\n");
            DebugUtil.debug(DebugUtil.DEBUG, "FileParser:parse", sFolioVisu);
            for (Enumeration eEnum = arrayFolioVisu.elements(); eEnum.hasMoreElements();) {
                String sLine = (String)eEnum.nextElement();
                fw.write(sLine + "\n");
                DebugUtil.debug(DebugUtil.DEBUG, "FileParser:parse", sLine);
            }
            fw.write("\n");

            // Write [CMD] CATEGORY
            fw.write(sCde + "\n");
            DebugUtil.debug(DebugUtil.DEBUG, "FileParser:parse", sCde);
            for (Enumeration eEnum = arrayCmd.elements(); eEnum.hasMoreElements();) {
                String sLine = (String)eEnum.nextElement();
                fw.write(sLine + "\n");
                DebugUtil.debug(DebugUtil.DEBUG, "FileParser:parse", sLine);
            }
            fw.write("\n");

			// FPDM STERIA ADD Start : MCE 02JAN2006: RFC2099: Add the checkin of session files
            fw.write(sSession + "\n");
            DebugUtil.debug(DebugUtil.DEBUG, "FileParser:parse", sSession);
            for (Enumeration eEnum = arraySession.elements(); eEnum.hasMoreElements();) {
                String sLine = (String)eEnum.nextElement();
                fw.write(sLine + "\n");
                DebugUtil.debug(DebugUtil.DEBUG, "FileParser:parse", sLine);
            }
            fw.write("\n");
			// FPDM STERIA ADD End   : MCE 02JAN2006: RFC2099

            // Write [END] CATEGORY
            fw.write(sFin + "\n");
            DebugUtil.debug(DebugUtil.DEBUG, "FileParser:parse", sFin);

            fw.flush();
            fw.close();
        }

        // ======================================================
        // LST FILE WILL BE SEND TO THE SERVER TO UPDATE ALL OBJECTS IN DATABASE
        // ======================================================
        DebugUtil.debug(DebugUtil.INFO, "FileParser:parse", "   LST FILE PARSING IS ENDED.");
        object = "DATA|".concat(" ").concat("|").concat(" ");
        DebugUtil.debug(DebugUtil.DEBUG, "FileParser:parse", "object DATA ==: " + object);
        DebugUtil.debug(DebugUtil.DEBUG, "FileParser:parse", "file DATA ==: " + f.getName());
        addBusObject(object, f);

	}

	private void addBusObject(String object, File file) throws IOException, InterruptedException {
		StringTokenizer token = new StringTokenizer(object, DELIM);
		String type = token.nextToken().trim();
		String name = token.nextToken().trim();
		String revision = token.nextToken().trim();
		if ( type.equals("CAD Component") || type.equals("CAD Drawing")) {
            try {
                file = Local_OS_Information.Compressfile(file, true);
			}
            catch (Exception ex) {
                DebugUtil.debug(DebugUtil.ERROR, "-- Unable to compress file : " + file.getAbsolutePath());
            }
        }
		DebugUtil.debug(DebugUtil.ERROR, "-- bIsTIFFOnly : " + bIsTIFFOnly);
		BusObject bo = new BusObject(null, type, name, revision, file.getName(), bIsTIFFOnly);
		this.array.addElement(bo);
	}


/**
 * Number of BusObject contained in this object.<br>
 * Should be 0 before<br>
 * the parse() method is called.<br>
 *
 * @return
 */
	public int size() {
		return this.array.size();
	}

/**
 * Give an enumeration of the BusObject contained in this object.<br>
 * @return
 */
	public Enumeration enumeration() {
		return this.array.elements();
	}


	/**
	 * Give an array of the BusObject contained in this object.<br>
	 * @return
	 */
	public BusObject[] getBusObjects() {
//      return (BusObject[]) this.array.toArray(new BusObject[0]);
        BusObject[] boRet = new BusObject[this.array.size()];
        int iCpt = 0;
        for (Enumeration eEnum = this.array.elements(); eEnum.hasMoreElements(); iCpt++) {
            boRet[iCpt] = (BusObject)eEnum.nextElement();
        }
        return boRet;
	}

}
