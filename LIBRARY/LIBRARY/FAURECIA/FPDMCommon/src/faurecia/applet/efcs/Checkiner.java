package faurecia.applet.efcs;

import java.io.File;
import java.util.Date;

import faurecia.util.AbstractFile;
import faurecia.util.AppletServletCommunication;
import faurecia.util.BusObject;
import faurecia.util.DebugUtil;

/**
 * @author steria
 * 
 *         To change this generated comment edit the template variable "typecomment": Window>Preferences>Java>Templates. To enable and disable the creation of type comments go to
 *         Window>Preferences>Java>Code Generation.
 */
public class Checkiner {
    // TODO : give debug level to the servlets

    public static final String sShortURLForMxFCSCheckinServlet = "/servlet/fcs/checkin";

    public static final String FAURECIA_eFCS_SERVLET_NAME = "Faurecia_eFCSFileServlet";

    public static final String FAURECIA_eMCS_SERVLET_NAME = "Faurecia_eMCSFileServlet";

    /**
     * Method to Check-in a file using eFCS If last is true, then no file is sent, it is just an update of the CAD Definition
     * 
     * @param sLocalDirectoryPath
     * @param boType
     * @param boName
     * @param boRev
     * @param fileName
     * @param sFormat
     * @param bAppend
     * @param bUnlock
     * @param bTIFFOnly
     * @param toConnectId
     * @param sMainServerBaseUrl
     * @throws Exception
     */
    public static void checkinFile(String sLocalDirectoryPath, String boType, String boName, String boRev, String fileName, String sFormat, boolean bAppend, boolean bUnlock, boolean bTIFFOnly,
            String toConnectId, String sMainServerBaseUrl) throws Exception {
        DebugUtil.debug(DebugUtil.INFO, "Applet : entry in Checkiner.checkinFile");

        // Construct BusObject
        BusObject bo = new BusObject(boType, boName, boRev, fileName, bAppend, bUnlock, bTIFFOnly, toConnectId);
        bo.setFormat(sFormat);

        BusObject[] a_bo = new BusObject[1];
        a_bo[0] = bo;

        // Retrieve JobTicket from the MCS server
        a_bo = (BusObject[]) getTicketsForCheckin(a_bo, false, sMainServerBaseUrl);

        String[] ListJobReceipt = storeFilesInFCS(sLocalDirectoryPath, a_bo);
        checkinEnd(ListJobReceipt, sMainServerBaseUrl);
        DebugUtil.debug(DebugUtil.DEBUG, "Applet : after Checkiner.checkinEnd");

        return;
    }

    /**
     * Method to Check-in a file using eFCS If last is true, then no file is sent, it is just an update of the CAD Definition
     * @param sLocalDirectoryPath 
     * @param boType 
     * @param boName 
     * @param boRev 
     * @param fileName 
     * @param sFormat 
     * @param bAppend 
     * @param bTIFFOnly 
     * @param sMainServerBaseUrl 
     * @throws Exception 
     */
    public static void checkinAndUnlockFile(String sLocalDirectoryPath, String boType, String boName, String boRev, String fileName, String sFormat, boolean bAppend, boolean bTIFFOnly,
            String sMainServerBaseUrl) throws Exception {

        DebugUtil.debug(DebugUtil.INFO, "Applet : entry in Checkiner.checkinFile");

        // Construct BusObject
        BusObject bo = new BusObject(boType, boName, boRev, fileName, bAppend, false, bTIFFOnly, "");
        // STERIA BEGIN #556
        // ADD
        bo.setFormat(sFormat);
        // STERIA BEGIN #556

        BusObject[] a_bo = new BusObject[1];
        a_bo[0] = bo;

        // Retrieve JobTicket from the MCS server
        a_bo = (BusObject[]) getTicketsForCheckin(a_bo, false, sMainServerBaseUrl);

        String[] ListJobReceipt = storeFilesInFCS(sLocalDirectoryPath, a_bo);
        checkinEnd(ListJobReceipt, sMainServerBaseUrl);
        unlockFiles(a_bo, sMainServerBaseUrl);
        DebugUtil.debug(DebugUtil.DEBUG, "Applet : after Checkiner.checkinEnd");

        return;
    }

    public static BusObject[] getTicketsForCheckin(BusObject[] a_bo, boolean isLast, String sMainServerBaseUrl) throws Exception {

        // --------------------------------------
        // URL to connect to the MCS server
        String eMCSServletPath = sMainServerBaseUrl + FAURECIA_eMCS_SERVLET_NAME + "/getCheckinTicket";
        eMCSServletPath += "?debug=ON";
        eMCSServletPath += "&time=" + new Date().getTime();

        DebugUtil.debug(DebugUtil.DEBUG, "Applet : Connection to the server to get ticket for checkin");
        DebugUtil.debug(DebugUtil.DEBUG, "Applet : url used: " + eMCSServletPath);

        // --------------------------------------
        // Connection to the MCS server & send BusObject & read return
        Object[] a_TempObj = AppletServletCommunication.requestServerTask(eMCSServletPath, a_bo);
        a_bo = new BusObject[a_TempObj.length];
        for (int i = 0; i < a_TempObj.length; i++) {
            a_bo[i] = (BusObject) a_TempObj[i];
            DebugUtil.debug(DebugUtil.DEBUG, "Checkiner:getTicketsForCheckin", "returned BusObject is : " + a_bo[i].toString());
            DebugUtil.debug(DebugUtil.DEBUG, "Checkiner:getTicketsForCheckin", "returned JobTicket is : " + a_bo[i].getJobTicket());
        }

        return a_bo;
    }

    public static String[] storeFilesInFCS(String sLocalDirectoryPath, BusObject[] a_bo) throws Exception {
        String[] ListJobReceipt = new String[a_bo.length];
        DebugUtil.debug(DebugUtil.DEBUG, "Applet : Entry in storeFilesInFCS");

        String sFaureciaFCSServletURL = "";
        // TODO : Sort by FCS URL and treat more than one object at a time
        for (int iCpt = 0; iCpt < a_bo.length; iCpt++) {
            String sMatrixFCSServletURL = a_bo[iCpt].getFCSServletURL();
            int iIndex = sMatrixFCSServletURL.indexOf(sShortURLForMxFCSCheckinServlet);
            if (iIndex > 0)
                sFaureciaFCSServletURL = sMatrixFCSServletURL.substring(0, iIndex);
            else
                sFaureciaFCSServletURL = sMatrixFCSServletURL;
            if (!sFaureciaFCSServletURL.endsWith("/")) {
                sFaureciaFCSServletURL += "/";
            }
            sFaureciaFCSServletURL += FAURECIA_eFCS_SERVLET_NAME + "/checkin";

            DebugUtil.debug(DebugUtil.DEBUG, "Applet : Complete Faurecia FCS URL " + sFaureciaFCSServletURL);

            String sFileName = a_bo[iCpt].getFileName();
            String sJobTicket = a_bo[iCpt].getJobTicket();

            AbstractFile abstractFile = new AbstractFile(sFileName, sJobTicket);
            abstractFile.setContent(new File(sLocalDirectoryPath, sFileName));
            AbstractFile[] a_AbstractFile = new AbstractFile[1];
            a_AbstractFile[0] = abstractFile;

            // --------------------------------------
            // Connection to the MCS server to retrieve JobReceipts
            Object[] a_TempObj = AppletServletCommunication.requestServerTask(sFaureciaFCSServletURL, a_AbstractFile);
            String[] TempListJobReceipt = new String[a_TempObj.length];
            for (int i = 0; i < a_TempObj.length; i++) {
                TempListJobReceipt[i] = (String) a_TempObj[i];
            }

            ListJobReceipt[iCpt] = TempListJobReceipt[0];
        }
        return ListJobReceipt;
    }

    public static void checkinEnd(String[] ListJobReceipt, String sMainServerBaseUrl) throws Exception {
        DebugUtil.debug(DebugUtil.DEBUG, "Applet : Entry in checkinEnd");
        String eMCSServletPath = sMainServerBaseUrl + FAURECIA_eMCS_SERVLET_NAME + "/endCheckin";

        DebugUtil.debug(DebugUtil.DEBUG, "Applet : eMCSServletPath : " + eMCSServletPath);

        // --------------------------------------
        // Connection to the MCS server
        AppletServletCommunication.requestServerTask(eMCSServletPath, ListJobReceipt);

        return;
    }

    public static void unlockFiles(BusObject[] a_bo, String sMainServerBaseUrl) throws Exception {
        DebugUtil.debug(DebugUtil.DEBUG, "Applet : Entry in checkinEnd");
        String eMCSServletPath = sMainServerBaseUrl + FAURECIA_eMCS_SERVLET_NAME + "/UnlockFiles";

        DebugUtil.debug(DebugUtil.DEBUG, "Applet : eMCSServletPath : " + eMCSServletPath);

        // --------------------------------------
        // Connection to the MCS server
        AppletServletCommunication.requestServerTask(eMCSServletPath, a_bo);

        return;
    }

    public static void removeFiles(String sLocalDirectoryPath, String boId, String boType, String boName, String boRev, String fileName, String sFormat, boolean bAppend, String sMainServerBaseUrl)
            throws Exception {

        // Construct BusObject
        BusObject bo = new BusObject(boType, boName, boRev, fileName, bAppend, false, false, "");
        bo.setId(boId);
        bo.setFormat(sFormat);

        BusObject[] a_bo = new BusObject[1];
        a_bo[0] = bo;
        DebugUtil.debug(DebugUtil.DEBUG, "Applet removeFiles : Entry in checkinEnd");
        String eMCSServletPath = sMainServerBaseUrl + FAURECIA_eMCS_SERVLET_NAME + "/RemoveFiles";

        DebugUtil.debug(DebugUtil.DEBUG, "Applet removeFiles : eMCSServletPath : " + eMCSServletPath);

        // --------------------------------------
        // Connection to the MCS server
        AppletServletCommunication.requestServerTask(eMCSServletPath, a_bo);

        return;
    }
}
