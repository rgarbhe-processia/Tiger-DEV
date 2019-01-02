package faurecia.applet.efcs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Date;
import java.util.Vector;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import faurecia.util.AbstractFile;
import faurecia.util.AppletServletCommunication;
import faurecia.util.BusObject;
import faurecia.util.DebugUtil;
import faurecia.util.Local_OS_Information;

/**
 * @author steria
 * 
 *         To change this generated comment edit the template variable "typecomment": Window>Preferences>Java>Templates. To enable and disable the creation of type comments go to
 *         Window>Preferences>Java>Code Generation.
 */

public class Checkouter {

    // FDPM ADD Start JFA 30/01/05 Message Log management
    private final static String CLASSNAME = "Checkouter";

    // FDPM ADD End JFA 30/01/05 Message Log management

    public static final String sShortURLForMxFCSCheckoutServlet = "/servlet/fcs/checkout";

    public static final String FAURECIA_eFCS_SERVLET_NAME = "Faurecia_eFCSFileServlet";

    public static final String FAURECIA_eMCS_SERVLET_NAME = "Faurecia_eMCSFileServlet";

    public static String urlOfLastFCSCall = "";

    public static File checkoutFile(String localRepertory, String boId, String fileName, String format, boolean bLock, String toConnectId, String type, String sMainServerBaseUrl) throws Exception {

        BusObject bo = new BusObject(boId, type, fileName, format, bLock, toConnectId);

        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkoutFile --> bo : " + bo.toString());
        BusObject[] a_bo = new BusObject[1];
        a_bo[0] = bo;

        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkoutFile --> a_bo.length : " + a_bo.length);
        // Retrieve JobTicket from the MCS server
        a_bo = (BusObject[]) getTicketsForCheckout(a_bo, sMainServerBaseUrl);

        // Retrieve JobTicket from the MCS server
        File[] files = retrieveFilesFromFCS(localRepertory, a_bo);
        File file = files[0];

        if (file.getName().endsWith(".Z") && !"2DViewable".equals(type)) {
            try {
                file = Local_OS_Information.Compressfile(file, false);
            } catch (Exception e) {
                DebugUtil.debug(DebugUtil.ERROR, CLASSNAME, "/checkoutFile --> Error occur during uncompression : " + e.toString());
                throw new Exception("Error occur during uncompression : " + e.toString());
            }
        }
        return file;
    }

    public static File checkoutTIFWithFile(String localRepertory, String boId, String fileName, String format, boolean bLock, String toConnectId, String type, String sMainServerBaseUrl,
            String sUserName, String sCurrentState, String sDate) throws Exception {

        BusObject bo = new BusObject(boId, type, fileName, format, bLock, toConnectId);

        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkoutTIFWithFile --> bo : " + bo.toString());
        BusObject[] a_bo = new BusObject[1];
        a_bo[0] = bo;

        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkoutTIFWithFile --> a_bo.length : " + a_bo.length);
        // Retrieve JobTicket from the MCS server
        a_bo = (BusObject[]) getTicketsForCheckout(a_bo, sMainServerBaseUrl);

        // Retrieve JobTicket from the MCS server
        File[] files = retrieveFilesFromFCSWithTxtFile(localRepertory, a_bo, sUserName, sCurrentState, sDate);
        File file = files[0];

        if (file.getName().endsWith(".Z") && !"2DViewable".equals(type)) {
            try {
                file = Local_OS_Information.Compressfile(file, false);
            } catch (Exception e) {
                DebugUtil.debug(DebugUtil.ERROR, CLASSNAME, "/checkoutTIFWithFile --> Error occur during uncompression : " + e.toString());
                throw new Exception("Error occur during uncompression : " + e.toString());
            }
        }
        return file;
    }

    public static File checkoutAsTIF(String localDirectory, String boId, String boName, String fileName, String format, boolean bLock, String toConnectId, String type, String sMainServerBaseUrl,
            Object titleBlock, boolean bHPGL2) throws Exception {
        // get the Checkout Ticket from MCS
        BusObject[] a_bo = { new BusObject(boId, type, fileName, format, bLock, toConnectId) };
        a_bo = (BusObject[]) getTicketsForCheckout(a_bo, sMainServerBaseUrl);
        // build the appropriate FCS URL
        String sFileName = a_bo[0].getFileName();
        String sJobTicket = a_bo[0].getJobTicket();
        if ("".equals(sJobTicket)) {
            throw new Exception("no file found on server");
        }
        String sMatrixFCSServletURL = a_bo[0].getFCSServletURL();
        int iIndex = sMatrixFCSServletURL.indexOf(sShortURLForMxFCSCheckoutServlet);
        String sFaureciaFCSServletURL;
        if (iIndex > 0)
            sFaureciaFCSServletURL = sMatrixFCSServletURL.substring(0, iIndex);
        else
            sFaureciaFCSServletURL = sMatrixFCSServletURL;
        if (!sFaureciaFCSServletURL.endsWith("/")) {
            sFaureciaFCSServletURL += "/";
        }
        sFaureciaFCSServletURL += FAURECIA_eFCS_SERVLET_NAME;
        urlOfLastFCSCall = sFaureciaFCSServletURL;
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkoutAsTIF --> bHPGL2=" + bHPGL2 + " titleBlock=" + titleBlock);

        if (titleBlock == null) {
            sFaureciaFCSServletURL += "/checkout";
        } else if (bHPGL2) {
            sFaureciaFCSServletURL += "/checkoutAsTIFHPGL2";
        } else {
            sFaureciaFCSServletURL += "/checkoutAsTIF";
        }

        // FDPM ADD Start JFA 30/01/05 Message Log management
        sFaureciaFCSServletURL += "?debug=" + System.getProperty("faurecia.debugging");
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkoutAsTIF --> Complete Faurecia FCS URL : " + sFaureciaFCSServletURL);
        // FDPM ADD End JFA 30/01/05 Message Log management

        // retrieve the file
        Object[] a_params = null;
        if (titleBlock == null) {
            a_params = new Object[] { new AbstractFile(sFileName, sJobTicket) };
        } else {
            a_params = new Object[] { new AbstractFile(sFileName, sJobTicket), titleBlock, sMainServerBaseUrl };
        }
        Object[] a_AbstractFiles = AppletServletCommunication.requestServerTask(sFaureciaFCSServletURL, a_params);
        AbstractFile abstractFile = (AbstractFile) a_AbstractFiles[0];

        String strFileName = abstractFile.getFileName();
        if (boName != null && !"".equals(boName)) {
            strFileName = boName + ".tif";
        }
        int index = strFileName.indexOf(".plt.tif");
        if (index != -1) {
            strFileName = strFileName.substring(0, index) + ".tif";
            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkoutAsTIF --> strFileName : " + strFileName);
        }
        File file = abstractFile.createFileOnSystemDrive(localDirectory, strFileName);
        // FPDM ADD End JFA 29/08/2006 RFC 3441
        return file;
    }

    public static String checkoutFileOnServer(String boId, String fileName, String format, boolean bLock, String toConnectId, String type, String sMainServerBaseUrl) throws Exception {

        BusObject bo = new BusObject(boId, type, fileName, format, bLock, toConnectId);

        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkoutFileOnServer --> bo : " + bo.toString());
        BusObject[] a_bo = new BusObject[1];
        a_bo[0] = bo;

        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkoutFileOnServer --> a_bo.length : " + a_bo.length);
        // Retrieve JobTicket from the MCS server
        a_bo = (BusObject[]) getTicketsForCheckout(a_bo, sMainServerBaseUrl);

        // Retrieve JobTicket from the MCS server
        String[] fileNames = retrieveFilesOnFCSServer(a_bo);

        return fileNames[0];
    }

    public static File checkoutAndLockFile(String localRepertory, String boId, String fileName, String format, String type, String sMainServerBaseUrl) throws Exception {

        // Lock of object must be made afterwards, as it is not possible to lock an object already locked by a previous checkout of another file of the same object
        BusObject bo = new BusObject(boId, type, fileName, format, false, "");

        // DebugUtil.debug(DebugUtil.DEBUG, "Checkouter.checkoutFile", "bo : " + bo.toString());
        BusObject[] a_bo = new BusObject[1];
        a_bo[0] = bo;

        // DebugUtil.debug(DebugUtil.DEBUG, "Checkouter.checkoutFile", "a_bo.length : " + a_bo.length);
        // Retrieve JobTicket from the MCS server
        a_bo = (BusObject[]) getTicketsForCheckout(a_bo, sMainServerBaseUrl);

        // Retrieve JobTicket from the MCS server
        lockFiles(a_bo, sMainServerBaseUrl);
        File file = null;
        try {
            File[] files = retrieveFilesFromFCS(localRepertory, a_bo);
            file = files[0];
        } catch (Exception e) {
            unlockFiles(a_bo, sMainServerBaseUrl);
            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkoutAndLockFile --> Error occur during retrieveFilesFromFCS : " + e.toString());
            throw new Exception("Error occur during retrieveFilesFromFCS : " + e.toString());
        }
        
        if (file != null && file.getName().endsWith(".Z") && !"2DViewable".equals(type)) {
            try {
                file = Local_OS_Information.Compressfile(file, false);
            } catch (Exception e) {
                DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkoutAndLockFile --> Error occur during uncompression : " + e.toString());
                throw new Exception("Error occur during uncompression : " + e.toString());
            }
        }
        return file;
    }

    public static BusObject[] getTicketsForCheckout(BusObject[] a_bo, String sMainServerBaseUrl) throws Exception {
        // --------------------------------------
        // URL to connect to the MCS server
        String eMCSServletPath = sMainServerBaseUrl + FAURECIA_eMCS_SERVLET_NAME + "/getCheckoutTicket";
        // TODO : Pass the debug level
        eMCSServletPath += "?debug=true" ;
        // FDPM ADD Start JFA 30/01/05 Message Log management
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/getTicketsForCheckout --> URL used : " + eMCSServletPath);
        // FDPM ADD Start JFA 30/01/05 Message Log management
        eMCSServletPath += "&time=" + new Date().getTime();

        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/getTicketsForCheckout --> Connection to the server to get ticket for checkout");

        // --------------------------------------
        // Connection to the MCS server & send BusObject & read return
        Object[] a_Obj = AppletServletCommunication.requestServerTask(eMCSServletPath, a_bo);
        a_bo = new BusObject[a_Obj.length];
        for (int i = 0; i < a_Obj.length; i++) {
            a_bo[i] = (BusObject) a_Obj[i];
        }

        return a_bo;
    }

    public static String[] retrieveFilesOnFCSServer(BusObject[] a_bo) throws Exception {
        String sFaureciaFCSServletURL = "";
        String[] listOfFileNames = new String[a_bo.length];

        // TODO : Sort by FCS URL and treat more than one object at a time
        for (int iCpt = 0; iCpt < a_bo.length; iCpt++) {
            String sFileName = a_bo[iCpt].getFileName();
            String sJobTicket = a_bo[iCpt].getJobTicket();
            if ("".equals(sJobTicket)) {
                continue;
            }
            String sMatrixFCSServletURL = a_bo[iCpt].getFCSServletURL();
            int iIndex = sMatrixFCSServletURL.indexOf(sShortURLForMxFCSCheckoutServlet);
            if (iIndex > 0)
                sFaureciaFCSServletURL = sMatrixFCSServletURL.substring(0, iIndex);
            else
                sFaureciaFCSServletURL = sMatrixFCSServletURL;
            if (!sFaureciaFCSServletURL.endsWith("/")) {
                sFaureciaFCSServletURL += "/";
            }
            sFaureciaFCSServletURL += FAURECIA_eFCS_SERVLET_NAME;
            urlOfLastFCSCall = sFaureciaFCSServletURL;
            sFaureciaFCSServletURL += "/checkoutOnServer";

            // FDPM ADD Start JFA 30/01/05 Message Log management
            sFaureciaFCSServletURL += "?debug=" + System.getProperty("faurecia.debugging");
            
            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/retrieveFilesOnFCSServer --> Complete Faurecia FCS URL : " + sFaureciaFCSServletURL);
            // FDPM ADD End JFA 30/01/05 Message Log management

            AbstractFile abstractFiles = new AbstractFile(sFileName, sJobTicket);
            AbstractFile[] a_AbstractFiles = new AbstractFile[1];
            a_AbstractFiles[0] = abstractFiles;

            // --------------------------------------
            // Connection to the MCS server to retrieve JobReceipts
            Object[] a_Obj = AppletServletCommunication.requestServerTask(sFaureciaFCSServletURL, a_AbstractFiles);
            listOfFileNames[iCpt] = (String) a_Obj[0];
        }
        return listOfFileNames;

    }

    public static File[] retrieveFilesFromFCS(String sLocalDirectoryPath, BusObject[] a_bo) throws Exception {

        String sFaureciaFCSServletURL = "";
        File[] files = new File[a_bo.length];
        // TODO : Sort by FCS URL and treat more than one object at a time
        for (int iCpt = 0; iCpt < a_bo.length; iCpt++) {
            String sFileName = a_bo[iCpt].getFileName();
            String sJobTicket = a_bo[iCpt].getJobTicket();
            if ("".equals(sJobTicket)) {
                continue;
            }
            String sMatrixFCSServletURL = a_bo[iCpt].getFCSServletURL();
            int iIndex = sMatrixFCSServletURL.indexOf(sShortURLForMxFCSCheckoutServlet);
            if (iIndex > 0)
                sFaureciaFCSServletURL = sMatrixFCSServletURL.substring(0, iIndex);
            else
                sFaureciaFCSServletURL = sMatrixFCSServletURL;
            if (!sFaureciaFCSServletURL.endsWith("/")) {
                sFaureciaFCSServletURL += "/";
            }
            sFaureciaFCSServletURL += FAURECIA_eFCS_SERVLET_NAME;

            urlOfLastFCSCall = sFaureciaFCSServletURL;
            sFaureciaFCSServletURL += "/checkout";

            // FDPM ADD Start JFA 30/01/05 Message Log management
            sFaureciaFCSServletURL += "?debug=" + System.getProperty("faurecia.debugging");
            
            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/retrieveFilesFromFCS --> Complete Faurecia FCS URL : " + sFaureciaFCSServletURL);
            // FDPM ADD End JFA 30/01/05 Message Log management

            AbstractFile abstractFiles = new AbstractFile(sFileName, sJobTicket);
            AbstractFile[] a_AbstractFiles = new AbstractFile[1];
            a_AbstractFiles[0] = abstractFiles;

            // --------------------------------------
            // Connection to the MCS server to retrieve JobReceipts
            Object[] a_Obj = AppletServletCommunication.requestServerTask(sFaureciaFCSServletURL, a_AbstractFiles);
			
			DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/retrieveFilesFromFCS -->======================== after requestServerTask" );
            a_AbstractFiles = new AbstractFile[a_Obj.length];
            for (int i = 0; i < a_Obj.length; i++) {
                a_AbstractFiles[i] = (AbstractFile) a_Obj[i];
            }
			DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/retrieveFilesFromFCS -->======================== b4 createFileOnSystemDrive" );
            files[iCpt] = a_AbstractFiles[0].createFileOnSystemDrive(sLocalDirectoryPath, sFileName);
			DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/retrieveFilesFromFCS -->======================== after createFileOnSystemDrive"+ files);

        }
        return files;
    }

    public static File[] retrieveFilesFromFCSWithTxtFile(String sLocalDirectoryPath, BusObject[] a_bo, String sUserName, String sCurrentState, String sDate) throws Exception {
        String sFaureciaFCSServletURL = "";
        File[] files = new File[a_bo.length];
        // TODO : Sort by FCS URL and treat more than one object at a time
        for (int iCpt = 0; iCpt < a_bo.length; iCpt++) {
            String sFileName = a_bo[iCpt].getFileName();
            String sJobTicket = a_bo[iCpt].getJobTicket();
            if ("".equals(sJobTicket)) {
                continue;
            }
            String sMatrixFCSServletURL = a_bo[iCpt].getFCSServletURL();
            int iIndex = sMatrixFCSServletURL.indexOf(sShortURLForMxFCSCheckoutServlet);
            if (iIndex > 0)
                sFaureciaFCSServletURL = sMatrixFCSServletURL.substring(0, iIndex);
            else
                sFaureciaFCSServletURL = sMatrixFCSServletURL;
            if (!sFaureciaFCSServletURL.endsWith("/")) {
                sFaureciaFCSServletURL += "/";
            }

            sFaureciaFCSServletURL += FAURECIA_eFCS_SERVLET_NAME;
            urlOfLastFCSCall = sFaureciaFCSServletURL;
            sFaureciaFCSServletURL += "/checkoutTIFWithFile";

            // FDPM ADD Start JFA 30/01/05 Message Log management
            sFaureciaFCSServletURL += "?debug=" + System.getProperty("faurecia.debugging");
            ;
            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/retrieveFilesFromFCSWithTxtFile --> Complete Faurecia FCS URL : " + sFaureciaFCSServletURL);
            // FDPM ADD End JFA 30/01/05 Message Log management

            java.util.Hashtable htParameter = new java.util.Hashtable(5);
            htParameter.put("UserName", sUserName);
            htParameter.put("CurrentState", sCurrentState);
            htParameter.put("Date", sDate);

            AbstractFile abstractFiles = new AbstractFile(sFileName, sJobTicket);
            AbstractFile[] a_AbstractFiles = new AbstractFile[1];
            a_AbstractFiles[0] = abstractFiles;

            Object[] a_myObj = new Object[2];
            a_myObj[0] = htParameter;
            System.arraycopy(a_AbstractFiles, 0, a_myObj, 1, a_AbstractFiles.length);

            // --------------------------------------
            // Connection to the MCS server to retrieve JobReceipts
            // Object[] a_Obj = AppletServletCommunication.requestServerTask(sFaureciaFCSServletURL, a_AbstractFiles);
            Object[] a_Obj = AppletServletCommunication.requestServerTask(sFaureciaFCSServletURL, a_myObj);
            a_AbstractFiles = new AbstractFile[a_Obj.length];
            for (int i = 0; i < a_Obj.length; i++) {
                a_AbstractFiles[i] = (AbstractFile) a_Obj[i];
            }

            files[iCpt] = a_AbstractFiles[0].createFileOnSystemDrive(sLocalDirectoryPath, sFileName);

        }
        return files;
    }

    public static void lockFiles(BusObject[] a_bo, String sMainServerBaseUrl) throws Exception {
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/lockFiles --> Entry in checkinEnd");
        String eMCSServletPath = sMainServerBaseUrl + FAURECIA_eMCS_SERVLET_NAME + "/LockFiles";

        // FDPM ADD Start JFA 30/01/05 Message Log management
        eMCSServletPath += "?debug=" + System.getProperty("faurecia.debugging");
        ;
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/lockFiles --> eMCSServletPath : " + eMCSServletPath);
        // FDPM ADD End JFA 30/01/05 Message Log management

        // --------------------------------------
        // Connection to the MCS server
        AppletServletCommunication.requestServerTask(eMCSServletPath, a_bo);

        return;
    }
    
    public static void unlockFiles(BusObject[] a_bo, String sMainServerBaseUrl) throws Exception {
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/UnlockFiles --> Entry in checkinEnd");
        String eMCSServletPath = sMainServerBaseUrl + FAURECIA_eMCS_SERVLET_NAME + "/UnlockFiles";

        eMCSServletPath += "?debug=" + System.getProperty("faurecia.debugging");
        ;
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/UnlockFiles --> eMCSServletPath : " + eMCSServletPath);

        // --------------------------------------
        // Connection to the MCS server
        AppletServletCommunication.requestServerTask(eMCSServletPath, a_bo);

        return;
    }

    public static File downloadAllTIFFs(String sLocalTempDirectory, String[] ListOfFiles, boolean bLock, String toConnectId, String type, String sMainServerBaseUrl, String sUserName,
            String[] aListCurrentState, String sDate, String[] aListCADObjectsTNR, boolean bIsBanner) throws Exception {

        String sViewId;
        String format;
        String sFileName;
        String sPaperFormat;
        String sViewName;
        String sFaureciaFCSServletURL = "";
        int iNbFiles = ListOfFiles.length / 5;
        File[] files = new File[iNbFiles];
        BusObject[] a_bo = new BusObject[iNbFiles];
        Object[] a_myObj;
        if (bIsBanner) {
            a_myObj = new Object[iNbFiles * 2];
        } else {
            a_myObj = new Object[iNbFiles];
        }

        for (int iCpt = 0; iCpt < (iNbFiles); iCpt++) {
            // String sCADId = sCADDefIds[Math.min(iCpt, sCADDefIds.length - 1)];
            sViewId = ListOfFiles[(5 * iCpt)];
            format = ListOfFiles[(5 * iCpt) + 1];
            sFileName = ListOfFiles[(5 * iCpt) + 2];
            sViewName = ListOfFiles[(5 * iCpt) + 4];
            // ----------------------------------------------------
            // Retrieve title blocks from servlet
            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/downloadAllTIFFs: sCurrentState" + iCpt + ": " + aListCurrentState[iCpt]);

            BusObject bo = new BusObject(sViewId, type, sFileName, format, bLock, toConnectId);

            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/downloadAllTIFFs --> bo : " + bo.toString());
            a_bo[iCpt] = bo;

            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/downloadAllTIFFs --> a_bo.length : " + a_bo.length);
            // Retrieve JobTicket from the MCS server

        }
        a_bo = (BusObject[]) getTicketsForCheckout(a_bo, sMainServerBaseUrl);
        for (int iCpt = 0; iCpt < iNbFiles; iCpt++) {

            sFileName = a_bo[iCpt].getFileName();
            sFileName = aListCADObjectsTNR[iCpt] + sFileName;
            String sJobTicket = a_bo[iCpt].getJobTicket();
            if (!"".equals(sJobTicket)) {

                AbstractFile abstractFiles = new AbstractFile(sFileName, sJobTicket);
                AbstractFile[] a_AbstractFiles = new AbstractFile[1];
                a_AbstractFiles[0] = abstractFiles;

                if (bIsBanner) {
                    java.util.Hashtable htParameter = new java.util.Hashtable(5);
                    htParameter.put("UserName", sUserName);
                    htParameter.put("CurrentState", aListCurrentState[iCpt]);
                    htParameter.put("Date", sDate);

                    a_myObj[iCpt * 2] = htParameter;
                    System.arraycopy(a_AbstractFiles, 0, a_myObj, (iCpt * 2) + 1, a_AbstractFiles.length);
                } else {
                    System.arraycopy(a_AbstractFiles, 0, a_myObj, iCpt, a_AbstractFiles.length);
                }
            }
        }
        String sMatrixFCSServletURL = a_bo[0].getFCSServletURL();
        int iIndex = sMatrixFCSServletURL.indexOf(sShortURLForMxFCSCheckoutServlet);
        if (iIndex > 0)
            sFaureciaFCSServletURL = sMatrixFCSServletURL.substring(0, iIndex);
        else
            sFaureciaFCSServletURL = sMatrixFCSServletURL;
        if (!sFaureciaFCSServletURL.endsWith("/")) {
            sFaureciaFCSServletURL += "/";
        }
        sFaureciaFCSServletURL += FAURECIA_eFCS_SERVLET_NAME;
        urlOfLastFCSCall = sFaureciaFCSServletURL;
        sFaureciaFCSServletURL += "/downloadAllTIFZip";

        // FDPM ADD Start JFA 30/01/05 Message Log management
        sFaureciaFCSServletURL += "?debug=" + System.getProperty("faurecia.debugging");
        ;
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/downloadAllTIFZip --> Complete Faurecia FCS URL : " + sFaureciaFCSServletURL);
        // FDPM ADD End JFA 30/01/05 Message Log management

        // --------------------------------------
        // Connection to the MCS server to retrieve JobReceipts
        // Object[] a_Obj = AppletServletCommunication.requestServerTask(sFaureciaFCSServletURL, a_AbstractFiles);
        Object[] a_Obj = AppletServletCommunication.requestServerTask(sFaureciaFCSServletURL, a_myObj);
        AbstractFile[] a_AbstractFiles = new AbstractFile[a_Obj.length];
        if (a_Obj.length > 0) {
            a_AbstractFiles[0] = (AbstractFile) a_Obj[0];
        }
        sDate = sDate.replace('/', '-');
        sDate = sDate.replace(':', '-');
        sDate = sDate.replace(' ', '_');
        files[0] = a_AbstractFiles[0].createFileOnSystemDrive(sLocalTempDirectory, "DownloadAllTIFFs_" + sUserName + "_" + sDate + ".zip");

        // files[iCpt] = tempFiles[0];
        // doZipFiles(files, sLocalTempDirectory + "\\" + "DownloadAllTIFFs_" + sUserName + "_" + sDate + ".zip");
        return files[0];
    }

    public static void doZipFiles(File[] files, String zipfilename) {
        try {
            byte[] buf = new byte[1024];
            CRC32 crc = new CRC32();
            ZipOutputStream s = new ZipOutputStream((OutputStream) new FileOutputStream(zipfilename));
            Collection cFiles = new Vector();
            for (int i = 0; files.length > i; i++) {
                String sFileName = files[i].getAbsolutePath();
                if (!cFiles.contains(sFileName)) {
                    cFiles.add(sFileName);
                    System.out.println("sFileName : " + sFileName);
                    FileInputStream fis = new FileInputStream(sFileName);

                    s.setLevel(6);

                    ZipEntry entry = new ZipEntry(files[i].getName());
                    entry.setSize((long) buf.length);
                    crc.reset();
                    crc.update(buf);
                    entry.setCrc(crc.getValue());
                    s.putNextEntry(entry);
                    int count;
                    while ((count = fis.read(buf, 0, buf.length)) != -1) {
                        s.write(buf, 0, buf.length);
                    }
                }
            }
            s.finish();
            s.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
