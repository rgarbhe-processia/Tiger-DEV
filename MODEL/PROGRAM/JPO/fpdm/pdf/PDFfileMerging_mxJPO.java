package fpdm.pdf;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import static java.lang.Math.*;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.multipdf.PDFMergerUtility;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.MailUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.client.fcs.FcsClient;
import com.matrixone.fcs.mcs.CheckoutOptions;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectProxy;
import matrix.db.Context;
import matrix.db.FcsSupport.AdkOutputStreamSource;
import matrix.db.JPO;
import matrix.db.FileList;
import matrix.db.TicketWrapper;
import matrix.util.MatrixException;
import matrix.util.SelectList;
import matrix.util.StringList;

import org.apache.commons.io.FileUtils;

public class PDFfileMerging_mxJPO {
    // Schema initialization

    private String tempFile = ""; // createTemporaryFileName();

    private static String strCheckoutDir = "";

    private static String applicationURL = "";

    private static String zipfilename = "All_MergedPDF.zip";

    private static String mergePDFfilename = "All_MergedPDF.pdf";

    Path temp;

    File file;

    public PDFfileMerging_mxJPO(Context context, String[] args) throws IOException, MatrixException {
    }

    public String allPDFMerge(Context context, String[] args) throws MatrixException, Exception {

        ArrayList listOfFiles = new ArrayList();
        ArrayList<BusinessObjectProxy> alFiles = new ArrayList<BusinessObjectProxy>();

        temp = Files.createTempDirectory("TMP_AllMergedPDFExport_");
        strCheckoutDir = System.getProperty("java.io.tmpdir") + File.separator + temp.getFileName() + File.separator;

        TicketWrapper ticket = null;
        applicationURL = MailUtil.getBaseURL(context);

        HashMap jpoMap = (HashMap) JPO.unpackArgs(args);
        String parentspecobjId = (String) jpoMap.get("parentspecobjId");
        List<String> myList = new ArrayList<String>(Arrays.asList(parentspecobjId.split(",")));
        int ListSize = myList.size();

        if (myList.size() > 0) {
            for (Iterator<String> iterator = myList.iterator(); iterator.hasNext();) {

                String specobjId = iterator.next();
                DomainObject domainObj = DomainObject.newInstance(context, specobjId);

                String RELATIONSHIP_ACTIVE_VERSION = PropertyUtil.getSchemaProperty(context, "relationship_ActiveVersion");
                String sHasActiveVersion = domainObj.getInfo(context, "to[" + RELATIONSHIP_ACTIVE_VERSION + "]");
                DomainObject thisSearchingObject = null;

                if ("True".equals(sHasActiveVersion)) {
                    String majorId = domainObj.getInfo(context, "to[" + RELATIONSHIP_ACTIVE_VERSION + "].from." + DomainConstants.SELECT_ID);
                    thisSearchingObject = new DomainObject(majorId);
                } else {
                    thisSearchingObject = domainObj;
                }

                try {
                    String sObjectId = thisSearchingObject.getInfo(context, DomainConstants.SELECT_ID);
                    StringList slFileNames = toStringList(thisSearchingObject.getInfoList(context, DomainConstants.SELECT_FILE_NAME));
                    StringList slFileFormats = toStringList(thisSearchingObject.getInfoList(context, DomainConstants.SELECT_FILE_FORMAT));
                    Iterator<?> itFiles = slFileNames.iterator();
                    Iterator<?> itFormats = slFileFormats.iterator();
                    String sFileName = "";
                    String sFileFormat = "";
                    while (itFiles.hasNext()) {
                        sFileName = (String) itFiles.next();
                        sFileFormat = (String) itFormats.next();
                        if ("PDF".equals(sFileFormat)) {
                            listOfFiles.add(sFileName);
                            BusinessObjectProxy bop = new BusinessObjectProxy(sObjectId, sFileFormat, sFileName);
                            if (!alFiles.contains(bop)) {
                                alFiles.add(bop);
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Error in getFileFromObject");
                    e.printStackTrace();
                    throw e;
                }
            }
        }

        if (!alFiles.isEmpty()) {
            try {
                String contextConnect = context.getSession().getConnectString();
                CheckoutOptions coForZip = new CheckoutOptions(contextConnect);
                coForZip.list = alFiles;
                ticket = com.matrixone.fcs.mcs.Checkout.doIt(context, coForZip);
                AdkOutputStreamSource outDir = new AdkOutputStreamSource(strCheckoutDir);
                FcsClient.checkout(ticket.getExportString(), ticket.getActionURL(), null, null, outDir, applicationURL);
            } catch (MatrixException e) {
                e.printStackTrace();
            }
        }

        ArrayList<File> listOfAllFiles = new ArrayList<File>();
        if (!listOfFiles.isEmpty()) {
            listOfAllFiles = fileMerge(listOfFiles, strCheckoutDir);
        }

        File zipfile = new File(strCheckoutDir + zipfilename); // Create Zip file and save all PDF file in it
        ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipfile)); //
        byte[] buffer = new byte[1024];

        if (listOfAllFiles.size() > 0) {
            for (Iterator<File> iterator = listOfAllFiles.iterator(); iterator.hasNext();) {
                File file1 = iterator.next();

                FileInputStream inStream = new FileInputStream(file1); // add original file to ZIP folder
                zipOut.putNextEntry(new ZipEntry(file1.getName()));
                int length;

                while ((length = inStream.read(buffer)) > 0) {
                    zipOut.write(buffer, 0, length);
                }
                zipOut.closeEntry();
                inStream.close();
            }
            zipOut.close(); // close zip file
        }

        String zipfilepath = strCheckoutDir + zipfilename;
        File file = new File(zipfilepath);
        FileInputStream fis = new FileInputStream(file);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        byte[] buf = new byte[1024];
        try {
            for (int readNum; (readNum = fis.read(buf)) != -1;) {
                bos.write(buf, 0, readNum); // no doubt here is 0
            }
        } catch (IOException ex) {
        }
        byte[] bytes = bos.toByteArray();

        String sFile = javax.xml.bind.DatatypeConverter.printBase64Binary(bytes);

        FileUtils.deleteDirectory(new File(System.getProperty("java.io.tmpdir") + File.separator + temp.getFileName() + File.separator));

        return sFile;
    }

    // Write All PDF file according to Size
    public static ArrayList fileMerge(ArrayList listOfFiles, String strCheckoutDir) {
        ArrayList listOfAllFiles = new ArrayList();
        String sResult = "";
        PDFMergerUtility PDFmerger = new PDFMergerUtility();
        PDFmerger.setDestinationFileName(strCheckoutDir + mergePDFfilename);
        PDDocument doc = new PDDocument();
        PDDocument document = new PDDocument();
        PDDocument document_A1H = new PDDocument();
        PDDocument document_A2H = new PDDocument();
        PDDocument document_A3H = new PDDocument();
        PDDocument document_A4 = new PDDocument();
        PDDocument document_A0 = new PDDocument();
        PDDocument document_B1 = new PDDocument();
        PDDocument document_B1plus = new PDDocument();
        PDDocument document_B2 = new PDDocument();
        PDDocument document_B2plus = new PDDocument();
        PDDocument document_B3 = new PDDocument();
        PDDocument document_B3plus = new PDDocument();
        PDDocument document_B4 = new PDDocument();
        PDDocument document_B4plus = new PDDocument();
        PDDocument document_B5 = new PDDocument();
        PDDocument document_B5plus = new PDDocument();

        String Formatname = "";
        String iFormat = "";

        try {
            for (Iterator<String> iterator = listOfFiles.iterator(); iterator.hasNext();) {
                String fileName = iterator.next();
                File file1 = new File(strCheckoutDir + fileName);

                doc = PDDocument.load(file1);
                for (PDPage page : doc.getPages()) {
                    double roHeight = Math.round(((page.getMediaBox().getHeight()) / 72) * 100.0) / 100.0;
                    double roWidth = Math.round(((page.getMediaBox().getWidth()) / 72) * 100.0) / 100.0;
                    if (((roHeight == 33.11) && (roWidth == 46.81)) || ((roWidth == 33.11) && (roHeight == 46.81))) {
                        document_A0.addPage(page);
                        continue;
                    } else if (((roHeight == 23.39) && (roWidth == 33.11)) || ((roWidth == 23.39) && (roHeight == 33.11))) {
                        document_A1H.addPage(page);
                        continue;
                    } else if (((roHeight == 23.39) && (roWidth == 16.54)) || ((roWidth == 23.39) && (roHeight == 16.54))) {
                        document_A2H.addPage(page);
                        continue;
                    } else if (((roHeight == 16.54) && (roWidth == 11.69)) || ((roWidth == 16.54) && (roHeight == 11.69))) {
                        document_A3H.addPage(page);
                        continue;
                    } else if (((roHeight == 11.69) && (roWidth == 8.27)) || ((roWidth == 11.69) && (roHeight == 8.27))) {
                        document_A4.addPage(page);
                        continue;
                    } else if (((roHeight == 66.22) && (roWidth == 33.11)) || ((roWidth == 66.22) && (roHeight == 33.11))) {
                        document_B1.addPage(page);
                        continue;
                    } else if (((roHeight == 74.09) && (roWidth == 33.11)) || ((roWidth == 74.09) && (roHeight == 33.11))) {
                        document_B1plus.addPage(page);
                        continue;
                    } else if (((roHeight == 93.62) && (roWidth == 33.11)) || ((roWidth == 93.62) && (roHeight == 33.11))) {
                        document_B2.addPage(page);
                        continue;
                    } else if (((roHeight == 101.50) && (roWidth == 33.11)) || ((roWidth == 101.50) && (roHeight == 33.11))) {
                        document_B2plus.addPage(page);
                        continue;
                    } else if (((roHeight == 140.43) && (roWidth == 33.11)) || ((roWidth == 140.43) && (roHeight == 33.11))) {
                        document_B3.addPage(page);
                        continue;
                    } else if (((roHeight == 148.31) && (roWidth == 33.11)) || ((roWidth == 148.31) && (roHeight == 33.11))) {
                        document_B3plus.addPage(page);
                        continue;
                    } else if (((roHeight == 187.24) && (roWidth == 33.11)) || ((roWidth == 187.24) && (roHeight == 33.11))) {
                        document_B4.addPage(page);
                        continue;
                    } else if (((roHeight == 195.12) && (roWidth == 33.11)) || ((roWidth == 195.12) && (roHeight == 33.11))) {
                        document_B4plus.addPage(page);
                        continue;
                    } else if (((roHeight == 234.06) && (roWidth == 33.11)) || ((roWidth == 234.06) && (roHeight == 33.11))) {
                        document_B5.addPage(page);
                        continue;
                    } else if (((roHeight == 241.93) && (roWidth == 33.11)) || ((roWidth == 241.93) && (roHeight == 33.11))) {
                        document_B5plus.addPage(page);
                        continue;
                    } else {
                        document.addPage(page);
                    }
                }
                // PDFmerger.addSource(file1); // it will put all the file in single file
            }

            if (document.getNumberOfPages() > 0)
                document.save(strCheckoutDir + "RemainingFiles_MergePDF.pdf");
            if (document_A0.getNumberOfPages() > 0)
                document_A0.save(strCheckoutDir + "A0_MergePDF.pdf");
            if (document_A1H.getNumberOfPages() > 0)
                document_A1H.save(strCheckoutDir + "A1H_MergePDF.pdf");
            if (document_A2H.getNumberOfPages() > 0)
                document_A2H.save(strCheckoutDir + "A2H_MergePDF.pdf");
            if (document_A3H.getNumberOfPages() > 0)
                document_A3H.save(strCheckoutDir + "A3H_MergePDF.pdf");
            if (document_A4.getNumberOfPages() > 0)
                document_A4.save(strCheckoutDir + "A4_MergePDF.pdf");
            if (document_B1.getNumberOfPages() > 0)
                document_B1.save(strCheckoutDir + "B1_MergePDF.pdf");
            if (document_B1plus.getNumberOfPages() > 0)
                document_B1plus.save(strCheckoutDir + "B1Plus_MergePDF.pdf");
            if (document_B2.getNumberOfPages() > 0)
                document_B2.save(strCheckoutDir + "B2_MergePDF.pdf");
            if (document_B2plus.getNumberOfPages() > 0)
                document_B2plus.save(strCheckoutDir + "B2Plus_MergePDF.pdf");
            if (document_B3.getNumberOfPages() > 0)
                document_B3.save(strCheckoutDir + "B3_MergePDF.pdf");
            if (document_B3plus.getNumberOfPages() > 0)
                document_B3plus.save(strCheckoutDir + "B3Plus_MergePDF.pdf");
            if (document_B4.getNumberOfPages() > 0)
                document_B4.save(strCheckoutDir + "B4_MergePDF.pdf");
            if (document_B4plus.getNumberOfPages() > 0)
                document_B4plus.save(strCheckoutDir + "B4Plus_MergePDF.pdf");
            if (document_B5.getNumberOfPages() > 0)
                document_B5.save(strCheckoutDir + "B5_MergePDF.pdf");
            if (document_B5plus.getNumberOfPages() > 0)
                document_B5plus.save(strCheckoutDir + "B5Plus_MergePDF.pdf");

            PDFmerger.mergeDocuments();
            document.close();
            document_A0.close();
            document_A1H.close();
            document_A2H.close();
            document_A3H.close();
            document_A4.close();
            document_B1.close();
            document_B1plus.close();
            document_B2.close();
            document_B2plus.close();
            document_B3.close();
            document_B3plus.close();
            document_B4.close();
            document_B4plus.close();
            document_B5.close();
            document_B5plus.close();

            File folder = new File(strCheckoutDir);
            File[] listOfPDFFiles = folder.listFiles();

            for (File file : listOfPDFFiles) {
                if (file.isFile()) {
                    listOfAllFiles.add(file);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return listOfAllFiles;
    }

    public static StringList toStringList(Object obj) throws Exception {
        StringList slObj = new StringList();
        try {
            if (obj != null) {
                if (obj instanceof String) {
                    if (((String) obj).length() > 0) {
                        slObj.addElement((String) obj);
                    }
                } else {
                    slObj = (StringList) obj;
                }
            }
        } catch (Exception e) {
            System.out.println("Error in toStringList() - obj=<" + obj + ">\n");
            throw e;
        }
        return slObj;
    }
}
