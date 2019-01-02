/*
 * Creation Date : 27 juil. 04
 *
 */
package faurecia.applet.cad;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import faurecia.applet.efcs.Checkiner;
import faurecia.util.AppletServletCommunication;
import faurecia.util.BusObject;
import faurecia.util.DebugUtil;
import faurecia.util.Local_OS_Information;
import fpdm.applet.cad.Parameters;

/**
 * @author fcolin
 * 
 */
public class CheckinCADFile {

    public static void main(String[] args) {
    }

    private static ArrayList<File> alLSTFiles;

    private static ArrayList<FileParser> alFileParsers;

    private static ArrayList<BusObject> alBusObjects;

    private static boolean isCheckedIn = false;

    private static String sOrganizationName;

    public static Object[] checkinAllFilesFromLSTFiles(String sLocalTempDirectory, String[] sLSTFiles, boolean bAppend, boolean bUnlock, String sMainServerBaseURL, String scheckin2D, String objectId,
            String listcheckinout, boolean allcheckin, String _sOrganizationName) throws Exception {

        return checkinAllFilesFromLSTFiles(sLocalTempDirectory, sLSTFiles, bAppend, bUnlock, sMainServerBaseURL, scheckin2D, objectId, listcheckinout, true, allcheckin, _sOrganizationName);
    }

    /*
     * sLSTFiles : List of LST file names without extension (ex: 4400312A00)
     */
    private static Object[] checkinAllFilesFromLSTFiles(String sLocalTempDirectory, String[] sLSTFiles, boolean bAppend, boolean bUnlock, String sMainServerBaseURL, String scheckin2D,
            String objectId, String listcheckinout, boolean bDeleteFiles, boolean allcheckin, String _sOrganizationName) throws Exception {
        sOrganizationName = _sOrganizationName;
        alLSTFiles = new ArrayList<File>();
        alFileParsers = new ArrayList<FileParser>();
        alBusObjects = new ArrayList<BusObject>();

        String[] results = new String[sLSTFiles.length];

        // ====================================================================
        // FIRST STEP : CHECK OBJECTS CAN BE CHECKED IN
        // NB: An object is checkinable if a CAD Def ID is returned
        // ====================================================================
        // Build a list of objects
        Parameters.getAppletUI().displayProgressMessage("Retrieve object information ...");
        BusObject[] busObjects = new BusObject[sLSTFiles.length];

        for (int j = 0; j < sLSTFiles.length; j++) {
            DebugUtil.debug(DebugUtil.DEBUG, "Applet  : LST File : " + sLSTFiles[j]);
            busObjects[j] = getBusObjectFromLSTFile(sLocalTempDirectory, sLSTFiles[j]);
        }

        // Complete the definition of the busObjects by calling the servlet
        DebugUtil.debug(DebugUtil.DEBUG, "Applet  : scheckin2D " + scheckin2D);
        DebugUtil.debug(DebugUtil.DEBUG, "Applet  : objectId " + objectId);
        if (!"true".equals(scheckin2D)) {
            if (!allcheckin) {
                busObjects = BusObject.retrieveBusInfoFromServlet(busObjects, sMainServerBaseURL + Checkiner.FAURECIA_eMCS_SERVLET_NAME);
            } else {
                busObjects = BusObject.retrieveBusInfoFromServletForCheckinAll(busObjects, sMainServerBaseURL + Checkiner.FAURECIA_eMCS_SERVLET_NAME);
            }
        } else
            busObjects[0].setId(objectId);

        // ====================================================================
        // SECOND STEP : PARSE LST FILE TO RETRIEVE LIST OF FILES TO BE CHECKED IN
        // ====================================================================
        for (int jj = 0; jj < busObjects.length; jj++) {

            String sObjectId = busObjects[jj].getId();
            // Are there any CAD Document linked to the CAD Definition
            // Will be used for error treatment
            boolean bAreThereAlreadyCADDocuments = busObjects[jj].isBAppend();

            DebugUtil.debug(DebugUtil.INFO, "Applet  : O " + sObjectId + " TIFF only=" + busObjects[jj].isTIFFOnly());

            if ("NOPO".equalsIgnoreCase(sObjectId)) {
                results[jj] = busObjects[jj].getFileName() + " not authorized ";
                Parameters.getAppletUI().displayProgressMessage("Check-in of " + busObjects[jj].getName() + " " + busObjects[jj].getRevision() + " is not authorized.");
                if (sLSTFiles.length == 1) {
                    throw new Exception("Check-in of " + busObjects[jj].getName() + " " + busObjects[jj].getRevision() + " is not authorized.");
                }
                continue;
            } else if ("NOID".equalsIgnoreCase(sObjectId)) {
                results[jj] = busObjects[jj].getFileName() + " object does not exist ";
                Parameters.getAppletUI().displayProgressMessage("No " + busObjects[jj].getType() + " " + busObjects[jj].getName() + " " + busObjects[jj].getRevision() + " is registered in database.");
                if (sLSTFiles.length == 1) {
                    throw new Exception("No " + busObjects[jj].getType() + " " + busObjects[jj].getName() + " " + busObjects[jj].getRevision() + " is registered in database.");
                }
                continue;
            } else if ("NOIW".equalsIgnoreCase(sObjectId)) {
                results[jj] = busObjects[jj].getFileName() + " Not possible ";
                Parameters.getAppletUI().displayProgressMessage("Check-in of " + busObjects[jj].getName() + " " + busObjects[jj].getRevision() + " is not authorized.");
                if (sLSTFiles.length == 1) {
                    throw new Exception("Check-in of " + busObjects[jj].getName() + " " + busObjects[jj].getRevision() + " is not authorized.");
                }
                continue;
            } else if ("NOLK".equalsIgnoreCase(sObjectId)) {
                results[jj] = busObjects[jj].getFileName() + " Not possible ";
                Parameters.getAppletUI().displayProgressMessage("Check-in of " + busObjects[jj].getName() + " " + busObjects[jj].getRevision() + " is not authorized.");
                if (sLSTFiles.length == 1) {
                    throw new Exception(busObjects[jj].getName() + " " + busObjects[jj].getRevision() + " ALL CAD objects are not locked. To do this, you must lock existing objects before.");
                }
                continue;
            } else if ("NOAU".equalsIgnoreCase(sObjectId)) {
                results[jj] = busObjects[jj].getFileName() + " Not possible ";
                Parameters.getAppletUI().displayProgressMessage("Check-in of " + busObjects[jj].getName() + " " + busObjects[jj].getRevision() + " is not authorized.");
                if (sLSTFiles.length == 1) {
                    throw new Exception(busObjects[jj].getName() + " " + busObjects[jj].getRevision() + " A TIFF ONLY CAD object already exist. Delete it before.");
                }
                continue;
            }

            // --------------------------------------------
            // PARSE THE .LST FILE TO KNOW WHICH FILES
            // ARE TO BE SENT TO THE SERVER
            // --------------------------------------------
            String sLSTFileName = busObjects[jj].getFileName() + ".lst";
            Parameters.getAppletUI().displayProgressMessage("Parsing file " + sLSTFileName + " ...");

            File fLSTFile = new File(sLocalTempDirectory, sLSTFileName);
            if (!fLSTFile.exists()) {
                // FPDM MODIFY Start JFA RFC #3564 29/03/2006
                // throw new Exception("This LST file is missing : " + fLSTFile.getAbsolutePath());
                throw new Exception("This LST file is missing : " + fLSTFile.getAbsolutePath() + " (CATIA V5 users, use only the check in from CATIA).");
                // FPDM MODIFY End JFA RFC #3564 29/03/2006
            }
            FileParser fileParser = new FileParser(fLSTFile);

            // FPDM Add Start : JGO 2006/04/05 RFC 1520
            // Retrieve the map containing the name of the files on the business object before checkin
            Hashtable hmBusObjectInfo = busObjects[jj].getInfoMap();
            String sCADSoftware = (String) hmBusObjectInfo.get("sCADSoftware");

            Vector<String> vSuppressedTifs = new Vector<String>();
            Vector<String> vSuppressedPlts = new Vector<String>();

            int answer = JOptionPane.YES_OPTION;
            if (sCADSoftware != null && sCADSoftware.indexOf("CATIA") != -1 && sCADSoftware.indexOf("V5") == -1) {
                Enumeration e = fileParser.enumeration();

                Vector vModelsCheckedOut = (Vector) hmBusObjectInfo.get("model");
                Vector vPltsCheckedOut = (Vector) hmBusObjectInfo.get("plt");
                Vector vTifsCheckedOut = (Vector) hmBusObjectInfo.get("tif");
                Vector vSessionsCheckedOut = (Vector) hmBusObjectInfo.get("session");

                // Get the name of the files to be checked in
                Vector<String> vModels = new Vector<String>();
                Vector<String> vTifs = new Vector<String>();
                Vector<String> vPlts = new Vector<String>();
                Vector<String> vSessions = new Vector<String>();

                String sTemp = null;

                while (e.hasMoreElements()) {
                    BusObject bo = (BusObject) e.nextElement();
                    String sFileName = bo.getFileName();
                    if (sFileName.indexOf("model") != -1) {
                        vModels.add(sFileName.substring(0, sFileName.indexOf(".model") + 6));
                    } else if (sFileName.indexOf("tif") != -1) {
                        sTemp = sFileName.substring(0, sFileName.indexOf(".tif") + 4);
                        vTifs.add(sTemp);
                    } else if (sFileName.indexOf("plt") != -1) {
                        sTemp = sFileName.substring(0, sFileName.indexOf(".plt") + 4);
                        vPlts.add(sTemp);
                    } else if (sFileName.indexOf("session") != -1) {
                        vSessions.add(sFileName.substring(0, sFileName.indexOf(".session") + 8));
                    }
                }

                Iterator Ite = vTifsCheckedOut.iterator();
                while (Ite != null && Ite.hasNext()) {
                    String sFileName = (String) Ite.next();
                    if (!vTifs.contains(sFileName)) {
                        vSuppressedTifs.add(sFileName);
                    }
                }
                Ite = vPltsCheckedOut.iterator();
                while (Ite != null && Ite.hasNext()) {
                    String sFileName = (String) Ite.next();
                    if (!vPlts.contains(sFileName)) {
                        vSuppressedPlts.add(sFileName);
                    }
                }

                String sWarning = "";

                if (vModels.size() < vModelsCheckedOut.size()) {
                    sWarning += "The number of .model files to be checked-in is less than the previous one\n";
                }
                if (vPlts.size() < vPltsCheckedOut.size()) {
                    sWarning += "The number of .plt files to be checked-in is less than the previous one\n";
                }
                if (vTifs.size() < vTifsCheckedOut.size()) {
                    sWarning += "The number of .tif files to be checked-in is less than the previous one\n";
                }
                if (vSessions.size() < vSessionsCheckedOut.size()) {
                    sWarning += "The number of .session files to be checked-in is less than the previous one\n";
                }
                if (!sWarning.equals("")) {
                    sWarning += "After checkout there was:\n";
                    sWarning += (vModelsCheckedOut.size() > 0 ? vModelsCheckedOut.toString().replace(',', '\n') + "\n" : "")
                            + (vPltsCheckedOut.size() > 0 ? vPltsCheckedOut.toString().replace(',', '\n') + "\n" : "")
                            + (vTifsCheckedOut.size() > 0 ? vTifsCheckedOut.toString().replace(',', '\n') + "\n" : "")
                            + (vSessionsCheckedOut.size() > 0 ? vSessionsCheckedOut.toString().replace(',', '\n') : "") + "\n\n";
                    sWarning += "You are checking in:\n";
                    sWarning += (vModels.size() > 0 ? vModels.toString().replace(',', '\n') + "\n" : "") + (vPlts.size() > 0 ? vPlts.toString().replace(',', '\n') + "\n" : "")
                            + (vTifs.size() > 0 ? vTifs.toString().replace(',', '\n') + "\n" : "") + (vSessions.size() > 0 ? vSessions.toString().replace(',', '\n') : "");
                    sWarning += "\n\nDo you want to continue?";

                    JTextArea area = new JTextArea(sWarning.toString());
                    area.setRows(20);
                    area.setColumns(50);
                    area.setLineWrap(true);
                    JScrollPane pane = new JScrollPane(area);

                    answer = JOptionPane.showConfirmDialog(null, pane, "Warning", JOptionPane.YES_NO_OPTION);
                }
            }
            // FPDM Add End : JGO 2006/04/05 RFC 1520
            Enumeration e = fileParser.enumeration();

            try {
                if (answer != JOptionPane.YES_OPTION) {
                    throw new Exception("The operation has been cancelled.");
                }
                // --------------------------------------------
                // CHECKIN ALL REQUIRED FILES
                // --------------------------------------------
                Vector<String> v_ObjectsCheckedIn = new Vector<String>();

                while (e.hasMoreElements()) {
                    BusObject bo = (BusObject) e.nextElement();

                    DebugUtil.debug(DebugUtil.INFO, "BO retrieved by fileParser : " + bo.toString() + " TIFF only=" + bo.isTIFFOnly());

                    String sTypetoCheckin = null;
                    if (e.hasMoreElements()) {

                        String sFormat = "generic";
                        if (bo.getFileName().indexOf("model") != -1) {
                            sFormat = "CATIA";
                        } else if (bo.getFileName().indexOf("prt") != -1) {
                            sFormat = "UG";
                        } else if (bo.getFileName().indexOf("tif") != -1) {
                            sFormat = "TIF";
                        } else if (bo.getFileName().indexOf("plt") != -1) {
                            sFormat = "HPGL";
                        }
                        bo.setFormat(sFormat);

                        boolean isObjectFirstCheckIn = true;
                        String sKEY = bo.getType() + "|" + bo.getName() + "|" + bo.getRevision() + "|" + sFormat;
                        if (v_ObjectsCheckedIn.contains(sKEY)) {
                            isObjectFirstCheckIn = false;

                        } else {
                            v_ObjectsCheckedIn.addElement(sKEY);
                        }

                        Parameters.getAppletUI().displayProgressMessage("Check-in of \'" + bo.getFileName() + "\' ...");

                        // Append this file in the object only if requested by JSP page
                        // or if other files has just had been checked-in
                        boolean bAppendNow = (bAppend || !isObjectFirstCheckIn);
                        sTypetoCheckin = bo.getType();
                        if ("true".equals(scheckin2D) && Parameters.TYPE_FPDM_CAD_COMPONENT.equals(sTypetoCheckin))
                            sTypetoCheckin = Parameters.TYPE_FPDM_CAD_DRAWING;
                        Parameters.getAppletUI().displayProgressMessage("Check-in of \'" + sTypetoCheckin + "\' ...");
                        Checkiner.checkinFile(sLocalTempDirectory, sTypetoCheckin, bo.getName(), bo.getRevision(), bo.getFileName(), sFormat, bAppendNow, bUnlock, bo.isTIFFOnly(), sObjectId,
                                Parameters.getMainServerBaseURL());
                    }
                    if (Parameters.TYPE_FPDM_CAD_DRAWING.equals(sTypetoCheckin) || Parameters.TYPE_FPDM_CAD_COMPONENT.equals(sTypetoCheckin) || Parameters.TYPE_2DVIEWABLE.equals(sTypetoCheckin)) {
                        Parameters.getAppletUI().displayProgressMessage("Update of " + bo.getType() + " " + bo.getName() + " " + bo.getRevision() + " ...");

                        BusObject bo2 = new BusObject(sTypetoCheckin, bo.getName(), bo.getRevision(), bo.getFileName(), bAppend, bUnlock, bo.isTIFFOnly(), sObjectId);
                        bo2.setLstFileContent(fLSTFile);
                        BusObject[] a_bo = new BusObject[1];
                        a_bo[0] = bo2;
                        String eMCSServletPath = Parameters.getMainServerBaseURL() + Checkiner.FAURECIA_eMCS_SERVLET_NAME + "/updateObjectsFromLSTFile";
                        eMCSServletPath += "?debug=ON";
                        if ("true".equals(scheckin2D))
                            eMCSServletPath += "&checkin2D=true";
                        else
                            eMCSServletPath += "&checkin2D=false";
                        eMCSServletPath += "&time=" + new Date().getTime();

                        DebugUtil.debug(DebugUtil.DEBUG, "Applet : Connection to the server to get ticket for checkin");
                        DebugUtil.debug(DebugUtil.DEBUG, "Applet : url used: " + eMCSServletPath);

                        // --------------------------------------
                        // Connection to the MCS server & send BusObject & read return
                        AppletServletCommunication.requestServerTask(eMCSServletPath, a_bo);
                    }
                }
                if ((vSuppressedTifs.size() > 0) || (vSuppressedPlts.size() > 0)) {
                    HashMap<String, Object> hViewableToDelete = new HashMap<String, Object>();

                    hViewableToDelete.put("BusObject", busObjects[jj]);
                    hViewableToDelete.put("SuppressedTifs", vSuppressedTifs);
                    hViewableToDelete.put("SuppressedPlts", vSuppressedPlts);
                    Object[] viewableToSuppress = new Object[1];
                    viewableToSuppress[0] = hViewableToDelete;
                    AppletServletCommunication.requestServerTask(Parameters.getMainServerBaseURL() + Checkiner.FAURECIA_eMCS_SERVLET_NAME + "/suppress2DViewableV4", viewableToSuppress);
                }

            } catch (Exception ex) {
                // If there was no CAD Document linked to the CAD Definition at the beginning
                // Then we can destroy any CAD Document now it has failed
                if (!bAreThereAlreadyCADDocuments && !"true".equals(scheckin2D)) {
                    try {
                        Parameters.getAppletUI().displayProgressMessage("Error occured. Abort transaction ...");

                        BusObject bo2 = new BusObject(sObjectId, "", "", "", "");
                        BusObject[] a_bo = new BusObject[1];
                        a_bo[0] = bo2;

                        String eMCSServletPath = Parameters.getMainServerBaseURL() + Checkiner.FAURECIA_eMCS_SERVLET_NAME + "/removeAllLinkedCADDocuments";
                        eMCSServletPath += "?debug=ON";
                        eMCSServletPath += "&time=" + new Date().getTime();

                        DebugUtil.debug(DebugUtil.DEBUG, "Applet : url used: " + eMCSServletPath);

                        // --------------------------------------
                        // Connection to the MCS server & send BusObject & read return
                        AppletServletCommunication.requestServerTask(eMCSServletPath, a_bo);

                    } catch (Exception ex2) {
                        DebugUtil.debug(DebugUtil.ERROR, "Error has occured while rolling back transaction : " + ex2.getMessage());
                    }

                }
                throw ex;
            }
            // --------------------------------------------
            // EXECUTE COMMANDS OF THE LST FILE
            // --------------------------------------------
            DebugUtil.debug(DebugUtil.WARNING, "Applet : bDeleteFiles: " + bDeleteFiles);
            if (bDeleteFiles) {
                results[jj] = deleteFiles(fLSTFile, sLocalTempDirectory, fileParser, busObjects[jj], listcheckinout);
                isCheckedIn = false;
            } else {
                alLSTFiles.add(fLSTFile);
                alFileParsers.add(fileParser);
                alBusObjects.add(busObjects[jj]);
                isCheckedIn = true;
            }
        }
        Object[] oResult = new Object[] { results, busObjects };

        return oResult;
    }

    public static void deleteFiles(String sLocalTempDirectory, String listcheckinout, boolean deleteLST) throws Exception {
        if (alLSTFiles != null && isCheckedIn) {
            isCheckedIn = false;
            for (int i = 0; i < alLSTFiles.size(); i++) {
                File fLSTFile = (File) alLSTFiles.get(i);
                FileParser fileParser = (FileParser) alFileParsers.get(i);
                BusObject busObject = (BusObject) alBusObjects.get(i);
                deleteFiles(fLSTFile, sLocalTempDirectory, fileParser, busObject, listcheckinout, deleteLST);
            }
        }
    }

    private static String deleteFiles(File fLSTFile, String sLocalTempDirectory, FileParser fileParser, BusObject busObject, String listcheckinout) throws Exception {
        String result = deleteFiles(fLSTFile, sLocalTempDirectory, fileParser, busObject, listcheckinout, false);
        if (result.endsWith(" OK ")) {
            Vector<String> listCadFiles = new Vector<String>();
            Vector<String> listSessionFiles = new Vector<String>();
            int count2DFiles = 0;
            Enumeration eDelete = fileParser.enumeration();
            while (eDelete.hasMoreElements()) {
                BusObject bo = (BusObject) eDelete.nextElement();
                String filename = bo.getFileName();
                if (filename.endsWith(".session")) {
                    listSessionFiles.addElement(filename.substring(0, filename.lastIndexOf('.')));
                } else if (filename.endsWith(".plt") || filename.endsWith(".tif")) {
                    count2DFiles++;
                } else if (filename.endsWith(".lst")) {
                    // do nothing
                } else {
                    listCadFiles.add(filename.substring(0, filename.lastIndexOf('.')));
                }
            }
            String message = "";
            String separator = "";
            if (listSessionFiles.size() > 0) {
                String sMessage = "{0} CAD Sessions have been successfully checked in :";
                message += separator + MessageFormat.format(sMessage, new Object[] { String.valueOf(listSessionFiles.size()) });
                separator = "\n";
                Enumeration en = listSessionFiles.elements();
                while (en.hasMoreElements()) {
                    message += separator + "- " + (String) en.nextElement();
                }
            }
            if (listCadFiles.size() > 0) {
                String sMessage = "{0} CAD Files have been successfully checked in :";
                message += separator + MessageFormat.format(sMessage, new Object[] { String.valueOf(listCadFiles.size()) });
                separator = "\n ";
                Enumeration en = listCadFiles.elements();
                while (en.hasMoreElements()) {
                    message += separator + "- " + (String) en.nextElement();
                }
            }
            String sMessage = "{0} tiff/plot have been successfully checked-in.";
            message += separator + MessageFormat.format(sMessage, new Object[] { String.valueOf(count2DFiles) });

            DebugUtil.debug(DebugUtil.DEBUG, "CheckinCADFile:", message);

            if ("".equals(listcheckinout)) {
                // FPDM Replace Start : JGO 2006/03/22 - manage "carriage returns" in the message box
                // FPDM Replace Start : NLE 2006/04/04 - repair IEF applet
                // Parameters.getAppletUI().displayMessage("Information", message);
                Parameters.getAppletUI().displayResultMessage("Information", message, true);
                // FPDM Replace End : JGO 2006/03/22
            }
        }
        return result;
    }

    private static String deleteFiles(File fLSTFile, String sLocalTempDirectory, FileParser fileParser, BusObject busObject, String listcheckinout, boolean deleteLST) throws Exception {
        String sObjectId = busObject.getId();
        Parameters.getAppletUI().displayProgressMessage("Delete local files ...");
        DebugUtil.debug(DebugUtil.WARNING, "deleteFiles:busObject=", busObject.toString());
        boolean deleteFilesInJava = true;
        FileReader fileStr = new FileReader(fLSTFile);
        BufferedReader fileBuf = new BufferedReader(fileStr);
        String ligne = fileBuf.readLine();
        String sCommand = "[COMMANDES]";
        String sDelCde = "";
        byte tampon[] = new byte[1024];
        String sFileExecCMD = sLocalTempDirectory + "/" + "command.tmp";
        File file = new File(sLocalTempDirectory, "command.tmp");
        FileOutputStream fileOut = new FileOutputStream(file);
        // Execution des commandes specifiees dans la fichier .lst
        while (ligne != null) {
            if (sCommand.equals(ligne)) {
                ligne = fileBuf.readLine();
                while (!ligne.equals("")) {
                    deleteFilesInJava = false;
                    sDelCde = ligne;
                    tampon = ligne.getBytes();
                    fileOut.write(tampon);
                    fileOut.write("\n".getBytes());
                    ligne = fileBuf.readLine();
                }
                break;
            }
            ligne = fileBuf.readLine();
        }
        fileBuf.close();
        fileStr.close();
        fileOut.flush();
        fileOut.close();
        if (deleteFilesInJava) {
            Parameters.getAppletUI().displayProgressMessage("No command found ! Proceeding directly from Java");
            Enumeration eDelete = fileParser.enumeration();
            while (eDelete.hasMoreElements()) {
                BusObject bo = (BusObject) eDelete.nextElement();
                File f = new File(sLocalTempDirectory, bo.getFileName());
                f.delete();
            }
        } else {
            sDelCde = "ksh " + sFileExecCMD;
            try {
                Local_OS_Information.executeCommand(sDelCde, 2000, false);
            } catch (Exception ex) {
                Parameters.getAppletUI().displayProgressMessage(ex.getMessage());
                Parameters.getAppletUI().displayMessage("Warning", ex.getMessage());
            }

        }
        DebugUtil.debug(DebugUtil.DEBUG, "CheckinCADFile:", "command checkin ==: " + sDelCde);
        if ("all".equals(listcheckinout)) {
            listcheckinout = sObjectId + "|";
        } else {
            listcheckinout += sObjectId + "|";
        }

        String result = busObject.getFileName() + " OK ";

        // ===================================
        // UNLOCK CAD DEFINITION OBJECT
        // ===================================
        String sURL = Parameters.getMainServerBaseURL() + Checkiner.FAURECIA_eMCS_SERVLET_NAME + "/unlock";
        sURL += "?debug=ON";
        sURL += "&time=" + new Date().getTime();

        BusObject[] a_BusObject = new BusObject[1];
        a_BusObject[0] = new BusObject(sObjectId, "", "", "", "");
        AppletServletCommunication.requestServerTask(sURL, a_BusObject);
        if (deleteLST) {
            boolean isDeleted = fLSTFile.delete();
            if (!isDeleted) {
                System.out.println("WARNING: The following file is not deleted:" + fLSTFile.getAbsolutePath());
            }

        }
        return result;
    }

    /**
     * Get Part name from LST file name
     * 
     * @param sName
     *            LST file name without extension
     * @return
     */
    private static String extractPartName(String sName) {
        String sPartName = sName;
        Matcher regexMatcher = null;

        DebugUtil.debug(DebugUtil.DEBUG, "Applet : sOrganizationName=" + sOrganizationName);

        Pattern regex;
        if ("ESBG".equals(sOrganizationName)) {
            regex = Pattern
                    .compile("^((X?E[0-9]{7})|([0-9]{2}[A-Z][0-9]{7})|(C[0-9]{5,7}_[0-9]{2})|(C[0-9]{7})|([0-9]{6,7}_[0-9]{2})|([0-9]{10})|([0-9]{7}X?)|([0-9]{4,7})|([0-9]{3}[A-Z]{2}[0-9]{3}[A-Z]))(.*)");
        } else {
            regex = Pattern.compile("([0-9]+X?)(.*)");
        }
        regexMatcher = regex.matcher(sPartName);
        if (regexMatcher.find()) {
            sPartName = regexMatcher.group(1);
        }

        return sPartName;
    }

    private static BusObject getBusObjectFromLSTFile(String sLocalTempDirectory, String sLSTFileName) throws Exception {
        BusObject busObj = null;

        DebugUtil.debug(DebugUtil.INFO, "sLocalTempDirectory=" + sLocalTempDirectory + " , sLSTFileName=" + sLSTFileName);
        if (sLSTFileName == null || "".equals(sLSTFileName)) {
            throw new Exception("This LST file name is incorrect : sLSTFileName = <" + sLSTFileName + ">.");
        }
        String sFileNameWithExt = sLSTFileName + ".lst";
        File fLSTFile = new File(sLocalTempDirectory, sFileNameWithExt);
        if (!fLSTFile.exists()) {
            throw new Exception("This LST file is missing : " + fLSTFile.getAbsolutePath() + " (CATIA V5 users, use only the check in from CATIA).");
        }
        FileParser fileParser = new FileParser(fLSTFile);

        // remove minor revision
        String sObjectName = sLSTFileName.substring(0, sLSTFileName.length() - 2); // name with major revision
        String sObjectRevision = sLSTFileName.substring(sLSTFileName.length() - 2); // minor revision (00, 01, ...)

        String sName = sObjectName;
        String sRevision = sObjectRevision;
        if (fileParser.bIsTIFFOnly) {
            if (!"ESBG".equals(sOrganizationName) && sObjectName.indexOf("_") >= 0) {
                sObjectName = sObjectName.substring(0, sObjectName.indexOf("_"));
            }
            sName = extractPartName(sObjectName);
            sRevision = sObjectName.substring(sName.length(), sObjectName.length());
        }
        DebugUtil.debug(DebugUtil.DEBUG, "Applet : sName=" + sName + " sRevision=" + sRevision + " sLSTFileName=" + sLSTFileName);
        busObj = new BusObject("", "", sName, sRevision, sLSTFileName, fileParser.bIsTIFFOnly);
        return busObj;
    }

}
