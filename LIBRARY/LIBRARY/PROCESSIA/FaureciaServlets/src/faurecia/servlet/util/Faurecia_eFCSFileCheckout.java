package faurecia.servlet.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.matrixone.fcs.common.CheckoutData;
import com.matrixone.fcs.common.JobTicket;
import com.matrixone.fcs.fcs.FcsContext;
import com.matrixone.fcs.fcs.Item;

import faurecia.util.AbstractFile;
import faurecia.util.AppletServletCommunication;
import faurecia.util.Local_OS_Information;

/**
 * @author steria To change this generated comment edit the template variable "typecomment": Window>Preferences>Java>Templates. To enable and disable the creation of type comments go to
 *         Window>Preferences>Java>Code Generation.
 */
public class Faurecia_eFCSFileCheckout {

    // private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger("faurecia.servlet.util.Faurecia_eFCSFileCheckout");

    public Faurecia_eFCSFileCheckout() {
    }

    public static void checkout(FcsContext fcscontext, HttpServletRequest request, HttpServletResponse response) throws Exception {
        System.out.println("eFCSFileCheckout : Checkout method");
        Object[] a_TempObj = AppletServletCommunication.readInput(request.getInputStream());
        System.out.println("Object map size  : " + a_TempObj.length);
        // if (logger.isInfoEnabled()) {
        System.out.println("checkout() - Input   read.");
        // }
        AbstractFile[] a_AbstractFile = new AbstractFile[a_TempObj.length];
        for (int i = 0; i < a_TempObj.length; i++) {
            a_AbstractFile[i] = (AbstractFile) a_TempObj[i];
        }

        a_AbstractFile = checkoutBase(fcscontext, a_AbstractFile);

        AppletServletCommunication.sendOutput(response.getOutputStream(), a_AbstractFile);
        // if (logger.isInfoEnabled()) {
        System.out.println("checkout() - done.");
        // }
        return;

    }

    private static AbstractFile[] checkoutBase(FcsContext fcscontext, AbstractFile[] a_AbstractFile) throws Exception {
        // if (logger.isDebugEnabled()) {
        System.out.println("checkoutBase() - a_AbstractFile.length = <" + a_AbstractFile.length + ">");
        // }
        try {
            for (int iCpt = 0; iCpt < a_AbstractFile.length; iCpt++) {
                String sJobTicket = a_AbstractFile[iCpt].getJobTicket();
                // if (logger.isDebugEnabled()) {
                System.out.println("checkoutBase() - sJobTicket = <" + sJobTicket + ">");
                // }

                // build real JobTicket from export String
                JobTicket jobticket = new JobTicket(sJobTicket, fcscontext);
                // if (logger.isDebugEnabled()) {
                System.out.println("checkoutBase() - jobTicket = <" + jobticket + ">");
                // }

                Class abv = jobticket.getClass();
                List list = jobticket.getRequests(CheckoutData.class);
                // if (logger.isDebugEnabled()) {
                System.out.println("checkoutBase() - list = <" + list + ">");
                // }

                Iterator iterator = list.iterator();
                if (iterator.hasNext()) {
                    CheckoutData checkoutdata = (CheckoutData) iterator.next();
                    String sFileName = checkoutdata.getFileName();
                    // if (logger.isDebugEnabled()) {
                    System.out.println("checkoutBase() - checkoutdata.getFileName() = <" + sFileName + ">");
                    System.out.println("checkoutBase() - checkoutdata.getHashName() = <" + checkoutdata.getHashName() + ">");
                    System.out.println("checkoutBase() - checkoutdata.getPath() = <" + checkoutdata.getPath() + ">");
                    System.out.println("checkoutBase() - checkoutdata.getFileSize() = <" + checkoutdata.getFileSize() + ">");
                    // }
                    // System.out.println("fcscontext protocol------>" + fcscontext.getProtocol(fcscontext.getLocation(checkoutdata).getName()));
                    // ML 06/08/2008 Modified during 10.7 ugrade. Add getLock for be compliant
                    // Item item = checkoutdata.getItem(jobticket.getMCSInfo(), checkoutdata.getPath(), checkoutdata.getHashName(), fcscontext, checkoutdata.getFileSize(), checkoutdata.getLock());
                    Item item = Item.getItem(checkoutdata, checkoutdata.getPath(), checkoutdata.getHashName(), fcscontext, checkoutdata.getFileSize(), checkoutdata.getLock());

                    // ALM-1626 : JIRA TIGTK-3663 : START
                    fcscontext.setCurrent(item);
                    // Item item1 = fcscontext.getCurrentItem();
                    item.setFileAndHashNames(sFileName, checkoutdata.getHashName());
                    // ALM-1626 : JIRA TIGTK-3663 : END

                    // if (logger.isDebugEnabled()) {
                    System.out.println("checkoutBase() - After Item not null");
                    // }

                    if (item == null) {
                        // if (logger.isDebugEnabled()) {
                        System.out.println("checkoutBase() - Item is null");
                        // }
                        throw new Exception("Cannot get information from server, the Item is null");
                    }
                    InputStream inStrOfItem = (InputStream) item.getInputStream();
                    if (inStrOfItem == null) {
                        // if (logger.isDebugEnabled()) {
                        System.out.println("checkoutBase() - Input Stream of Item is null");
                        // }
                        throw new Exception("Cannot get information from server, the Input Stream of Item is null");
                    }
                    // if (logger.isDebugEnabled()) {
                    System.out.println("checkoutBase() - After inputStream of Item not null");
                    // }
                    BufferedInputStream bufInStr = new BufferedInputStream(inStrOfItem);

                    // ML 06/08/2008 Commented during 10.7 ugrade. getRoot method does not exist anymore
                    // DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/checkoutBase: checkoutdata.getRoot().toString() : " + checkoutdata.getRoot().toString());

                    // Specific treatment if file is empty
                    // due to a failure in matrix API item.getInputStream()
                    if (sFileName.lastIndexOf("plt.Z") >= 0 || sFileName.lastIndexOf("tif.Z") >= 0) {
                        // a_AbstractFile[iCpt].setContentAfterUnCompress(bufInStr, "/tmp", false);

                        // TRANSMIT AFTER UNCOMPRESSION
                        a_AbstractFile[iCpt].setContent(bufInStr, false);
                        a_AbstractFile[iCpt].createFileOnSystemDrive("/tmp", a_AbstractFile[iCpt].getFileName());
                        File filein = Local_OS_Information.DirectUncompressFile(new File("/tmp", a_AbstractFile[iCpt].getFileName()));
                        a_AbstractFile[iCpt].setContent(filein);
                    } else {
                        a_AbstractFile[iCpt].setContent(bufInStr, false);
                    }
                }
            }
            return a_AbstractFile;
        }

        catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    public static void doZipFiles(File[] files, String zipfilename) {
        try {
            byte[] buf = new byte[1024];
            CRC32 crc = new CRC32();
            ZipOutputStream s = new ZipOutputStream((OutputStream) new FileOutputStream(zipfilename));
            Collection<String> cFiles = new Vector<String>();
            for (int i = 0; files.length > i; i++) {
                String sFileName = files[i].getAbsolutePath();
                if (!cFiles.contains(sFileName)) {
                    cFiles.add(sFileName);
                    // if (logger.isDebugEnabled()) {
                    System.out.println("doZipFiles() - sFileName = <" + sFileName + ">");
                    // }
                    FileInputStream fis = new FileInputStream(sFileName);

                    s.setLevel(6);

                    ZipEntry entry = new ZipEntry(files[i].getName());
                    entry.setSize((long) buf.length);
                    crc.reset();
                    crc.update(buf);
                    entry.setCrc(crc.getValue());
                    s.putNextEntry(entry);
                    while ((fis.read(buf, 0, buf.length)) != -1) {
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
