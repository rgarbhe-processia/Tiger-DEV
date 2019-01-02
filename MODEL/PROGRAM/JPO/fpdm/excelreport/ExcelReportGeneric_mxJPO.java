package fpdm.excelreport;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFShape;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import fpdm.org.jxls.area.Area;
import fpdm.org.jxls.builder.AreaBuilder;
import fpdm.org.jxls.builder.xls.XlsCommentAreaBuilder;
import fpdm.org.jxls.command.GridCommand;
import fpdm.org.jxls.common.CellData;
import fpdm.org.jxls.common.CellRef;
import fpdm.org.jxls.formula.StandardFormulaProcessor;
import fpdm.org.jxls.transform.Transformer;
import fpdm.org.jxls.util.TransformerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.matrixone.apps.domain.util.MailUtil;
import com.matrixone.client.fcs.FcsClient;
import com.matrixone.fcs.mcs.CheckoutOptions;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectProxy;
import matrix.db.Context;
import matrix.db.FcsSupport.AdkOutputStreamSource;
import matrix.db.FileList;
import matrix.db.TicketWrapper;
import matrix.util.MatrixException;

public class ExcelReportGeneric_mxJPO {
    private static Logger logger = LoggerFactory.getLogger("FPDM_ExcelReportGeneric");

    protected static String applicationURL = "";

    String templatePath = ""; // path of the xlsx file (including file name and extension)

    String renderPath = ""; // path of the render xlslx file (including file name and extension)

    private String tempFile = ""; // createTemporaryFileName();

    private List<String> listTempFiles = new ArrayList<String>();

    private String expressionNotationBegin;

    private String expressionNotationEnd;

    private AreaBuilder areaBuilder = new XlsCommentAreaBuilder();

    private boolean processFormulas = true;

    final Path temp;

    private boolean complexity = false;

    String reportId = "";

    String reportTemplateFileName = "";

    Context enoviaContext = null;

    public String getTemplatePath() {
        return templatePath;
    }

    public void setTemplatePath(String templatePath) {
        this.templatePath = templatePath;
    }

    public String getRenderPath() {
        return renderPath;
    }

    public void setRenderPath(String renderPath) {
        this.renderPath = renderPath;
    }

    /*
     * 
     * Callable methods
     * 
     */

    /**
     * Contructor<br/>
     * Create temp folder, checkout the template file and set template path of this object.
     * @param context
     *            an eMatrix Context object
     * @param args
     *            must contain report id in args[0]
     * @throws IOException
     * @throws MatrixException
     */
    public ExcelReportGeneric_mxJPO(Context context, String[] args) throws IOException, MatrixException {
        this.enoviaContext = context;

        applicationURL = MailUtil.getBaseURL(context);

        temp = Files.createTempDirectory("TMPXL_");
        tempFile = System.getProperty("java.io.tmpdir") + File.separator + temp.getFileName() + File.separator + "tempcomments.xlsx";

        BusinessObject boReportSrc = new BusinessObject(args[0]);
        FileList listOfFiles = new FileList();
        try {
            listOfFiles = boReportSrc.getFiles(context);
        } catch (Exception e) {
            logger.debug(e.getMessage());
        }

        this.reportTemplateFileName = listOfFiles.get(0).toString();

        logger.debug("Checkout of files : " + this.reportTemplateFileName);
        logger.debug("Checkout in folder : " + temp.getFileName().toString());

        try {
            boReportSrc.checkoutFiles(context, false, "xlsx", listOfFiles, temp.toAbsolutePath().toString());
            this.setTemplatePath(temp.toAbsolutePath().toString() + File.separatorChar + this.reportTemplateFileName);
            this.listTempFiles.add(this.getTemplatePath());
        } catch (Exception e) {
            e.printStackTrace();
        }

        logger.debug("End Generic constructor ----------------");
    }

    /**
     * Method to surcharge if we want a new report
     * @param context
     *            an eMatrix Context object
     * @param args
     *            the args passed to the report
     * @return
     * @throws MatrixException
     * @throws Exception
     */
    public HashMap<String, Object> run(Context context, String[] args) throws MatrixException, Exception {
        /* this method must be surcharged by the JPO extending this */
        return new HashMap<String, Object>();
    }

    /**
     * Process the template and return an HashMap...<br/>
     * The Result contains : - file : an array of byte representing the final ExcelReport - download : true if we have the file in the result - notfound : a HashSet containing all entries of the
     * template not found by the process
     * @param parameters
     *            parameters Parameters to pass to the context
     * @param complexity
     *            if we set complexity to true, we call processComplexeTemplate and we don't call addComments
     * @return hmResult
     * @throws FileNotFoundException
     * @throws IOException
     */
    public HashMap<String, Object> processTemplate(HashMap<String, Object> parameters, boolean complexity) throws FileNotFoundException, IOException {
        this.complexity = complexity;
        HashMap<String, Object> hmResult = processTemplate(parameters);

        logger.debug("-------- dans Process Template : " + hmResult.size());

        return hmResult;
    }

    /**
     * Process the template and return an HashMap...<br/>
     * The Result contains : - file : an array of byte representing the final ExcelReport - download : true if we have the file in the result - notfound : a HashSet containing all entries of the
     * template not found by the process
     * @param parameters
     *            Parameters to pass to the context
     * @return hmResult
     * @throws FileNotFoundException
     * @throws IOException
     */
    public HashMap<String, Object> processTemplate(HashMap<String, Object> parameters) throws FileNotFoundException, IOException {

        HashMap<String, Object> hmResult = new HashMap<String, Object>();

        CellData.notFoundEntries = new HashSet<String>();

        logger.debug("Process Template");

        Collection<String> parametersList = new HashSet<String>();

        String fileToProcess = this.templatePath;

        if (new File(this.tempFile).isFile()) {
            fileToProcess = this.tempFile;
        }

        // We put parameters in the context
        fpdm.org.jxls.common.Context context = new fpdm.org.jxls.common.Context();

        Iterator<?> it = parameters.entrySet().iterator();
        while (it.hasNext()) {
            @SuppressWarnings({ "unchecked" })
            Map.Entry<String, Object> entry = (Map.Entry<String, Object>) it.next();
            parametersList.add(entry.getKey());

            logger.debug("Parameters in Excel Parameter");
            logger.debug((String) entry.getKey() + " ---> " + entry.getValue());

            context.putVar((String) entry.getKey(), entry.getValue());
        }

        String lastTransformationTempFile = createTemporaryFileName();

        logger.debug("Temp file name : " + lastTransformationTempFile);

        if (!complexity) {
            String transformerTempFile = lastTransformationTempFile;

            logger.debug(fileToProcess + " => " + transformerTempFile);

            try (InputStream is = new FileInputStream(fileToProcess); OutputStream os = new FileOutputStream(transformerTempFile);) {

                Transformer transformer = TransformerFactory.createTransformer(is, os);
                if (transformer == null) {
                    throw new IllegalStateException("Cannot load XLS transformer. Please make sure a Transformer implementation is in classpath");
                }

                areaBuilder.setTransformer(transformer);
                List<Area> xlsAreaList = areaBuilder.build();

                logger.debug("XLSArea : " + xlsAreaList);

                for (Area xlsArea : xlsAreaList) {
                    xlsArea.applyAt(new CellRef(xlsArea.getStartCellRef().getCellName()), context);
                    if (processFormulas) {
                        xlsArea.setFormulaProcessor(new StandardFormulaProcessor());
                        xlsArea.processFormulas();
                    }
                }
                transformer.write();
            } catch (Exception e) {
                logger.debug("Exception in create transformer");
                e.printStackTrace();
            }
        } else {
            processComplexeTemplate(context, lastTransformationTempFile);
            logger.debug("Complexe template...");
        }

        /*
         * 
         * 
         * Search for links
         * 
         * 
         */
        String linkTempFile = this.createTemporaryFileName();
        logger.debug(lastTransformationTempFile + " => " + linkTempFile);

        try (InputStream isZ = new FileInputStream(lastTransformationTempFile); OutputStream osZ = new FileOutputStream(linkTempFile);) {

            @SuppressWarnings("resource")
            XSSFWorkbook wb = new XSSFWorkbook(isZ);

            CreationHelper factory = wb.getCreationHelper();
            XSSFSheet sheet = wb.getSheetAt(0);

            HashSet<Cell> hsC = ExcelReportGeneric_mxJPO.findRow(sheet, ".link(");
            Iterator<Cell> itLinks = hsC.iterator();

            while (itLinks.hasNext()) {
                Cell workCell = (Cell) itLinks.next();
                String cellContent = workCell.getStringCellValue();
                String[] cellContentArray = cellContent.split(Pattern.quote(".link("));

                String newContent = cellContentArray[0];
                String link = cellContentArray[1].substring(0, cellContentArray[1].length() - 1);

                try {
                    Hyperlink hLink = factory.createHyperlink(Hyperlink.LINK_URL);
                    hLink.setAddress(link);

                    workCell.setCellValue(newContent);
                    workCell.setHyperlink(hLink);
                } catch (Exception e) {
                    logger.info("<ExcelReportGeneric> In retrieve links : add link error");
                }
            }

            wb.write(osZ);
            osZ.close();
        }

        /*
         * 
         * 
         * Search for images
         * 
         * 
         */

        String imgTempFile = this.createTemporaryFileName();
        logger.debug(linkTempFile + " => " + imgTempFile);

        try (InputStream isZ = new FileInputStream(linkTempFile); OutputStream osZ = new FileOutputStream(imgTempFile);) {

            @SuppressWarnings("resource")
            XSSFWorkbook wb = new XSSFWorkbook(isZ);

            // CreationHelper factory = wb.getCreationHelper();
            XSSFSheet sheet = wb.getSheetAt(0);

            HashSet<Cell> hsC = ExcelReportGeneric_mxJPO.findRow(sheet, "fpdmimage(");
            Iterator<Cell> itimages = hsC.iterator();

            String sCheckout_Dir = System.getProperty("java.io.tmpdir") + File.separator + temp.getFileName() + File.separator;

            if (itimages.hasNext()) {
                ArrayList<BusinessObjectProxy> alFiles = new ArrayList<BusinessObjectProxy>();

                while (itimages.hasNext()) {
                    // First of all, dress a list of all images in the report
                    Cell workCell = (Cell) itimages.next();
                    String cellContent = workCell.getStringCellValue();
                    String[] cellContentArray = cellContent.split(Pattern.quote("fpdmimage("));

                    String image = cellContentArray[1].substring(0, cellContentArray[1].length() - 1);
                    logger.debug("Image in ExcelReport : " + image);

                    String[] aImage = image.split(",");

                    if (3 == aImage.length) {
                        BusinessObjectProxy bop = new BusinessObjectProxy(aImage[0].trim(), aImage[1].trim(), aImage[2].trim());
                        alFiles.add(bop);
                    }
                }

                String contextConnect = this.enoviaContext.getSession().getConnectString();
                CheckoutOptions coForZip = new CheckoutOptions(contextConnect);
                coForZip.list = alFiles;

                TicketWrapper ticket = null;
                try {
                    ticket = com.matrixone.fcs.mcs.Checkout.doIt(this.enoviaContext, coForZip);
                } catch (MatrixException e) {
                    e.printStackTrace();
                }

                File fCheckout_Dir = new File(sCheckout_Dir);
                File[] listOfFilesBefore = fCheckout_Dir.listFiles();
                HashSet<File> hsBefore = new HashSet<File>();
                hsBefore.addAll(Arrays.asList(listOfFilesBefore));

                try {
                    AdkOutputStreamSource outDir = new AdkOutputStreamSource(sCheckout_Dir);
                    FcsClient.checkout(ticket.getExportString(), ticket.getActionURL(), null, null, outDir, applicationURL);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                File[] listOfFilesAfter = fCheckout_Dir.listFiles();
                HashSet<File> hsAfter = new HashSet<File>();
                hsAfter.addAll(Arrays.asList(listOfFilesAfter));

                hsAfter.removeAll(hsBefore);
            }

            HashSet<String> imageFileNames = new HashSet<String>();

            itimages = hsC.iterator();
            while (itimages.hasNext()) {
                try {
                    Cell workCell = (Cell) itimages.next();
                    String cellContent = workCell.getStringCellValue();
                    String[] cellContentArray = cellContent.split(Pattern.quote("fpdmimage("));

                    String image = cellContentArray[1].substring(0, cellContentArray[1].length() - 1);
                    logger.debug("Image in ExcelReport : " + image);

                    String[] aImage = image.split(",");

                    workCell.setCellValue("");

                    if (3 == aImage.length) {
                        String filename = aImage[2].trim();

                        String imageFileName = sCheckout_Dir + filename;

                        InputStream isImage = new FileInputStream(imageFileName);
                        byte[] bytes = IOUtils.toByteArray(isImage);
                        int pictureID = wb.addPicture(bytes, Workbook.PICTURE_TYPE_PNG);
                        isImage.close();

                        XSSFDrawing drawing = sheet.createDrawingPatriarch();
                        XSSFClientAnchor anchor = new XSSFClientAnchor();

                        anchor.setCol1(workCell.getColumnIndex());
                        anchor.setRow1(workCell.getRowIndex());

                        XSSFPicture newPicture = drawing.createPicture(anchor, pictureID);
                        int imageHeight = fpdm.excelreport.PixelUtil_mxJPO.heightUnits2Pixel((short) workCell.getRow().getHeight());
                        int imageWidth = fpdm.excelreport.PixelUtil_mxJPO.widthUnits2Pixel((short) sheet.getColumnWidth(workCell.getColumnIndex()));

                        File fileImage = new File(imageFileName);
                        BufferedImage bimg = ImageIO.read(fileImage);
                        int oldImageWidth = bimg.getWidth();
                        int oldImageHeight = bimg.getHeight();

                        logger.debug("Image : " + oldImageHeight + "x" + oldImageWidth + " -> " + imageHeight + "x" + imageWidth);

                        double ratioHeight = (double) imageHeight / (double) oldImageHeight;
                        double ratioWidth = (double) imageWidth / (double) oldImageWidth;
                        logger.debug("Ratio : " + ratioHeight + "x" + ratioWidth);

                        double ratioimage = 1;
                        if (ratioHeight < ratioWidth) {
                            ratioimage = ratioHeight;
                        } else {
                            ratioimage = ratioWidth;
                        }
                        logger.debug("Ratio image : " + ratioimage);
                        newPicture.resize(ratioimage);

                        imageFileNames.add(imageFileName);
                    }
                } catch (Exception e) {
                    logger.debug("Error in Image...");
                    e.printStackTrace();
                }

                Iterator<String> iImageFileNames = imageFileNames.iterator();
                while (iImageFileNames.hasNext()) {
                    File iFile = new File(iImageFileNames.next());
                    iFile.delete();
                }
            }

            wb.write(osZ);
            osZ.close();
        }

        /*
         * 
         * 
         * Put file (in binary format) in hmResult
         * 
         * 
         * 
         */

        hmResult.put("file", readFileToBytes(imgTempFile));

        /*
         * 
         * 
         * Delete temp files
         * 
         * 
         */

        try {
            Iterator<String> itFiles = listTempFiles.iterator();

            while (itFiles.hasNext()) {
                String fileTempName = itFiles.next();

                logger.debug("Delete " + fileTempName);
                File tempFileToDelete = new File(fileTempName);
                tempFileToDelete.delete();
            }

            Files.delete(temp);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Object[] obj = CellData.notFoundEntries.toArray();
        logger.debug("Values not found : ");
        for (Object o : obj)
            logger.debug(o.toString());

        hmResult.put("download", true);
        hmResult.put("notfound", CellData.notFoundEntries);

        return hmResult;
    }

    /**
     * Process Simple template.<br/>
     * - Add comments to a template - call processTemplate with parameters
     * @param parameters
     *            parameters passed to generation (
     * @return an HashMap with the result (see processTemplate result)
     * @throws FileNotFoundException
     * @throws IOException
     */
    public HashMap<String, Object> processSimpleTemplate(HashMap<String, Object> parameters) throws FileNotFoundException, IOException {
        logger.debug("Process Simple Template");

        this.addComments();
        HashMap<String, Object> hmResult = this.processTemplate(parameters);

        logger.debug("-------- dans processSimpleTemplate : " + hmResult.size());

        return hmResult;
    }

    /**
     * Process complexe template to temporary file.<br/>
     * This method can generate double entry tables and lot more. Check the doc for more informations.
     * @param context
     *            an eMatrix Context object
     * @param lastTransformationTempFile
     *            the complete path of the file to generate
     */
    public void processComplexeTemplate(fpdm.org.jxls.common.Context context, String lastTransformationTempFile) {
        logger.debug("Process Complexe Template");

        try {
            logger.debug("Last var : " + context.getLastVar());
            // List<Object> myDatacols = (List<Object>)context.getVar(context.getLastVar());

            String tempFileName = createTemporaryFileName();
            try (InputStream is = new FileInputStream(this.templatePath)) {
                try (OutputStream os = new FileOutputStream(tempFileName)) {
                    logger.debug("_____________________ POI ___________________________");
                    List<XSSFShape> listPictures = new ArrayList<XSSFShape>();
                    InputStream isZ = new FileInputStream(this.templatePath);
                    @SuppressWarnings("resource")
                    XSSFWorkbook wb = new XSSFWorkbook(isZ);
                    XSSFSheet sheet = wb.getSheetAt(0);
                    XSSFDrawing drawing = sheet.createDrawingPatriarch();
                    for (XSSFShape shape : drawing.getShapes()) {
                        XSSFShape picture = shape;
                        listPictures.add(picture);
                    }

                    logger.debug("_____________________ JXLS __________________________");
                    Transformer transformer = createTransformer(is, os);
                    areaBuilder.setTransformer(transformer);

                    List<Area> xlsAreaList = areaBuilder.build();

                    logger.debug("Before Area : " + xlsAreaList.size());

                    for (Area xlsArea : xlsAreaList) {
                        String decale = "";

                        @SuppressWarnings("unchecked")
                        List<Object> myDatacols = (List<Object>) context.getVar(decale);

                        if (xlsArea.getCommandDataList().size() > 0) {
                            String commandName = xlsArea.getCommandDataList().get(0).getCommand().getName();

                            logger.debug("decale " + decale);
                            try {
                                decale = xlsArea.getCommandDataList().get(0).getDecale();
                            } catch (NullPointerException e) {
                                logger.debug("No decale...");
                                decale = "";
                            }
                            logger.debug("decale " + decale);

                            if (null == decale) {
                                decale = "";
                            }

                            if ("each".equals(commandName)) {
                                CellRef cellref = new CellRef(xlsArea.getStartCellRef().getCellName());
                                cellref.setSheetName("Test");
                                logger.debug("Colonne " + cellref.getCol());
                                if (!"".equalsIgnoreCase(decale)) {
                                    logger.debug("D E C A L A T I O N (pour ainsi dire)");
                                    cellref.setSheetName("Test");
                                    cellref.setCol(cellref.getCol() + myDatacols.size() - 1);
                                }

                                xlsArea.applyAt(cellref, context);
                            } else if ("grid".equals(commandName)) {
                                GridCommand gridCommand = (GridCommand) xlsArea.getCommandDataList().get(0).getCommand();
                                logger.debug("Datas : " + gridCommand.getData());

                                @SuppressWarnings("unchecked")
                                List<String> headers = (List<String>) context.getVar(gridCommand.getHeaders());

                                StringBuilder sb = new StringBuilder();
                                for (int x = 0; x < headers.size(); x++) {
                                    sb.append("lineQuantity,");
                                }
                                sb.setLength(sb.length() - 1);

                                gridCommand.setProps(sb.toString());
                                xlsArea.setFormulaProcessor(new StandardFormulaProcessor());

                                CellRef crTemp = new CellRef(xlsArea.getStartCellRef().getCellName());
                                crTemp.setSheetName("Test");

                                xlsArea.applyAt(crTemp, context);
                                xlsArea.processFormulas();
                            }
                        } else {

                            if (!"".equalsIgnoreCase(decale)) {
                                logger.debug("D E C A L A T I O N (pour ainsi dire)");
                                CellRef cellref = new CellRef(xlsArea.getStartCellRef().getCellName());
                                cellref.setCol(cellref.getCol() + myDatacols.size() - 1);
                                cellref.setSheetName("Test");
                                xlsArea.applyAt(cellref, context);
                            } else {
                                CellRef crTemp = new CellRef(xlsArea.getStartCellRef().getCellName());
                                logger.debug("P A S    D E C A L A T I O N (pour ainsi dire)");
                                crTemp.setSheetName("Test");
                                logger.debug(crTemp.toString());
                                xlsArea.applyAt(crTemp, context);
                            }
                        }
                    }

                    transformer.write();

                    logger.debug("_____________________ PUT IMAGES POI ________________");
                    FileInputStream isE = new FileInputStream(tempFileName);
                    XSSFWorkbook wbE = new XSSFWorkbook(isE);
                    XSSFSheet sheetE = wbE.getSheetAt(1);
                    Iterator<XSSFShape> itr = listPictures.iterator();
                    XSSFDrawing drawingPat = sheetE.createDrawingPatriarch();
                    CreationHelper helper = wb.getCreationHelper();
                    while (itr.hasNext()) {
                        XSSFPicture picture = (XSSFPicture) itr.next();
                        int pictureIdx = wbE.addPicture(picture.getPictureData().getData(), XSSFWorkbook.PICTURE_TYPE_JPEG);

                        ClientAnchor pictureAnchor = picture.getPreferredSize();
                        ClientAnchor ca = helper.createClientAnchor();
                        ca.setCol1(pictureAnchor.getCol1() + 2);
                        ca.setRow1(pictureAnchor.getRow1());
                        ca.setCol2(pictureAnchor.getCol2() + 2);
                        ca.setRow2(pictureAnchor.getRow2());

                        ca.setDx1(pictureAnchor.getDx1());
                        ca.setDy1(pictureAnchor.getDy1());
                        ca.setDx2(pictureAnchor.getDx1());
                        ca.setDy2(pictureAnchor.getDy2());

                        XSSFPicture pict = drawingPat.createPicture(ca, pictureIdx);
                        pict.resize();
                    }
                    wbE.removeSheetAt(0);
                    wbE.write(new FileOutputStream(lastTransformationTempFile));
                    isE.close();
                }
            }

            // logger.debug ("Variables qui n'existent pas :");
            /*
             * Object[] obj = CellData.notFoundEntries.toArray(); for(Object o : obj) logger.debug(o.toString());
             */

            logger.debug("Finished");
        } catch (Exception e) {
            logger.debug("Can't retrieve Information for columns incrementation");

            e.printStackTrace();
        }
    }

    /**
     * Add comments in a template from this object template path
     * @throws IOException
     */
    public void addComments() throws IOException {
        logger.debug("addComments");

        InputStream isZ = new FileInputStream(this.getTemplatePath());

        XSSFWorkbook wb = new XSSFWorkbook(isZ);
        CreationHelper factory = wb.getCreationHelper();
        XSSFSheet sheet = wb.getSheetAt(0);

        logger.debug("Sheet name : " + sheet.getSheetName());

        XSSFRow xssfr = sheet.getRow(0);
        xssfr = sheet.createRow(0);

        XSSFCell cell = null;
        try {
            cell = sheet.getRow(0).getCell(0);
        } catch (NullPointerException npe) {
            npe.printStackTrace();
        } finally {
            cell = xssfr.createCell(0);
            cell.setCellType(Cell.CELL_TYPE_STRING);
        }

        Collection<ForEachClass> fecs = new HashSet<ForEachClass>();
        Collection<String> referencesUsed = new HashSet<String>();

        for (int x = 0; x <= sheet.getLastRowNum(); x++) {
            if (sheet.getRow(x) != null) {
                for (int y = 0; y <= sheet.getRow(x).getLastCellNum(); y++) {
                    XSSFCell thisCell = sheet.getRow(x).getCell(y);

                    if (thisCell != null) {
                        // Check if ref cell is not in the HashList
                        try {
                            // logger.debug("Cell : " + x + " / " + y + " => " + sheet.getRow(x).getCell(y).getReference() + " = " + sheet.getRow(x).getCell(y).getStringCellValue());
                            if (referencesUsed.contains(thisCell.getReference())) {
                                // logger.debug("Cell already known !");
                            } else {
                                // Check cell content
                                String cellContent = thisCell.getStringCellValue();
                                // If we find a ., this is a variable and we find the name
                                String thisVar = "";
                                if (cellContent.contains(".")) {
                                    thisVar = cellContent.split(Pattern.quote("."))[0];
                                }

                                if (!cellContent.contains("header")) {
                                    // Searching in the right the same variable
                                    XSSFCell nextCell = sheet.getRow(x).getCell(y);
                                    ForEachClass fec = new ForEachClass();
                                    fec.setStartCell(nextCell);

                                    int newy = y;
                                    boolean stop = false;
                                    if (!"".equalsIgnoreCase(thisVar)) {
                                        do {
                                            referencesUsed.add(nextCell.getReference());
                                            fec.setStopCell(nextCell);
                                            newy += 1;
                                            nextCell = sheet.getRow(x).getCell(newy);

                                            if (nextCell != null) {
                                                if (!nextCell.getStringCellValue().contains(thisVar)) {
                                                    stop = true;
                                                }
                                            } else {
                                                stop = true;
                                            }
                                        } while (stop == false);
                                    }

                                    boolean isSuite = false;
                                    if (newy > y + 1) {
                                        isSuite = true;
                                        fecs.add(fec);
                                    }

                                    stop = false;
                                    // If nothing on the right, searching bottom
                                    if (!isSuite) {
                                        nextCell = sheet.getRow(x).getCell(y);
                                        int newx = x;
                                        if (!"".equalsIgnoreCase(thisVar)) {
                                            do {
                                                referencesUsed.add(nextCell.getReference());
                                                fec.stopCell = nextCell;
                                                newx += 1;

                                                if (sheet.getRow(newx) != null) {
                                                    nextCell = sheet.getRow(newx).getCell(y);

                                                    if (nextCell != null) {
                                                        if (!nextCell.getStringCellValue().contains(thisVar)) {
                                                            stop = true;
                                                        }
                                                    } else {
                                                        stop = true;
                                                    }
                                                } else {
                                                    stop = true;
                                                }

                                            } while (stop == false);
                                        }

                                        if (newx > x + 1) {
                                            fecs.add(fec);
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        ClientAnchor ca1 = new XSSFClientAnchor();
        ca1.setCol1(cell.getColumnIndex());
        ca1.setCol2(cell.getColumnIndex() + 1);
        ca1.setRow1(cell.getRowIndex());
        ca1.setRow2(cell.getRowIndex() + 1);

        Drawing drawing = sheet.createDrawingPatriarch();
        Comment comment = drawing.createCellComment(ca1);

        int numberOfRows = sheet.getLastRowNum();
        int lastRow = 0;
        int lastCell = 0;

        for (int i = 0; i <= numberOfRows; i++) {
            try {
                int lastRowCell = sheet.getRow(i).getLastCellNum();

                if (lastRowCell > lastCell) {
                    lastCell = lastRowCell;
                }

                logger.debug("Last cell of row " + i + " : " + sheet.getRow(i).getLastCellNum());
                lastRow = i;
            } catch (NullPointerException e) {
                logger.debug("No cell in row " + i);
            }
        }
        logger.debug("Last Row : " + lastRow);
        logger.debug("Last Cell : " + lastCell);

        // We must detect last cell name to include in the comment
        // RichTextString str = factory.createRichTextString("jx:area(lastCell='" + sheet.getRow(sheet.getLastRowNum()).getCell(sheet.getRow(sheet.getLastRowNum()).getLastCellNum()-1).getReference() +
        // "')");

        RichTextString str = factory.createRichTextString("jx:area(lastCell='" + sheet.getRow(lastRow).getCell(lastCell - 1).getReference() + "')");
        comment.setString(str);
        comment.setAuthor("Auto generated");

        Iterator<ForEachClass> it = fecs.iterator();
        while (it.hasNext()) {
            ForEachClass fec = it.next();
            logger.debug("- From " + fec.getStartCell().getReference() + " to " + fec.getStopCell().getReference());

            String varName = getVarName(fec.getStartCell().getStringCellValue());

            logger.debug("Variable name : " + varName);

            XSSFCell cell2 = fec.getStartCell();
            ClientAnchor ca2 = new XSSFClientAnchor();
            ca2.setCol1(cell2.getColumnIndex());
            ca2.setCol2(cell2.getColumnIndex() + 1);
            ca2.setRow1(cell2.getRowIndex());
            ca2.setRow2(cell2.getRowIndex() + 1);

            Comment comment2 = drawing.createCellComment(ca2);
            String cellCommand = "jx:each(items='" + varName + "s' var='" + varName + "' lastCell='" + fec.getStopCell().getReference() + "')";

            logger.debug(cellCommand);

            RichTextString str2 = factory.createRichTextString(cellCommand);
            comment2.setString(str2);
            comment2.setAuthor("Auto generated");

            cell2.setCellComment(comment2);
        }

        tempFile = System.getProperty("java.io.tmpdir") + File.separatorChar + temp.getFileName() + File.separatorChar + "tempcomments.xlsx";
        this.listTempFiles.add(tempFile);

        logger.debug("Writing " + tempFile + " ..... done");

        wb.write(new FileOutputStream(tempFile));
        isZ.close();
    }

    /**
     * Return the var name of a cell
     * @param rawContent
     *            a string of type ${varname.anything}
     * @return the var name
     */
    private String getVarName(String rawContent) {
        logger.debug("getVarName");
        return rawContent.split(Pattern.quote("."))[0].split(Pattern.quote("{"))[1];
    }

    private class ForEachClass {
        XSSFCell startCell = null;

        XSSFCell stopCell = null;

        public XSSFCell getStartCell() {
            return startCell;
        }

        public void setStartCell(XSSFCell startCell) {
            this.startCell = startCell;
        }

        public XSSFCell getStopCell() {
            return stopCell;
        }

        public void setStopCell(XSSFCell stopCell) {
            this.stopCell = stopCell;
        }
    }

    /**
     * Search for a content in a sheet and return cells containing this content in a HashSet
     * @param sheet
     *            a XSSFSheet object
     * @param cellContent
     *            A content to search for
     * @return A HashSet of cells
     */
    private static HashSet<Cell> findRow(XSSFSheet sheet, String cellContent) {
        logger.debug("findRow");
        HashSet<Cell> result = new HashSet<Cell>();

        for (Row row : sheet) {
            for (Cell cell : row) {
                if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
                    if (cell.getRichStringCellValue().getString().contains(cellContent)) {
                        result.add(cell);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Create a Transformer object from a template stream in direction of a target stream
     * @param templateStream
     *            An InputStream of a template file
     * @param targetStream
     *            An OutputStream of the target file
     * @return the Transformer object generated
     */
    private Transformer createTransformer(InputStream templateStream, OutputStream targetStream) {
        logger.debug("createTransformer");
        Transformer transformer = TransformerFactory.createTransformer(templateStream, targetStream);
        if (transformer == null) {
            throw new IllegalStateException("Cannot load XLS transformer. Please make sure a Transformer implementation is in classpath");
        }
        if (this.expressionNotationBegin != null && this.expressionNotationEnd != null) {
            transformer.getTransformationConfig().buildExpressionNotation(expressionNotationBegin, expressionNotationEnd);
        }
        return transformer;
    }

    /**
     * give a name and create a new temporary file
     * @return the full path and name of the new file
     */
    private String createTemporaryFileName() {
        logger.debug("createTemporaryFileName");
        String newFileName = System.getProperty("java.io.tmpdir") + File.separator + temp.getFileName() + File.separator + UUID.randomUUID() + ".xlsx";
        listTempFiles.add(newFileName);
        return (newFileName);
    }

    /**
     * Read the given binary file, and return its contents as a byte array.
     * @param aInputFileName
     *            full path of the file we want to retrieve
     * @return an array of byte representing the file
     */
    byte[] readFileToBytes(String aInputFileName) {
        logger.debug("Reading in binary file named : " + aInputFileName);
        File file = new File(aInputFileName);

        logger.debug("File size: " + file.length());
        byte[] result = new byte[(int) file.length()];
        try {
            InputStream input = null;
            try {
                int totalBytesRead = 0;
                input = new BufferedInputStream(new FileInputStream(file));
                while (totalBytesRead < result.length) {
                    int bytesRemaining = result.length - totalBytesRead;
                    // input.read() returns -1, 0, or more :
                    int bytesRead = input.read(result, totalBytesRead, bytesRemaining);
                    if (bytesRead > 0) {
                        totalBytesRead = totalBytesRead + bytesRead;
                    }
                }
                /*
                 * the above style is a bit tricky: it places bytes into the 'result' array; 'result' is an output parameter; the while loop usually has a single iteration only.
                 */
                logger.debug("Num bytes read: " + totalBytesRead);
            } finally {
                logger.debug("Closing input stream.");
                input.close();
            }
        } catch (FileNotFoundException ex) {
            logger.error("File not found.", ex);
        } catch (IOException ex) {
            logger.error("IO", ex);
        }
        return result;
    }
}