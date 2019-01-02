package fpdm.excelreport.graphicalbom;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MailUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.framework.ui.UIUtil;
import com.matrixone.client.fcs.FcsClient;
import com.matrixone.fcs.mcs.CheckoutOptions;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectProxy;
import matrix.db.Context;
import matrix.db.FcsSupport.AdkOutputStreamSource;
import matrix.db.JPO;
import matrix.db.TicketWrapper;
import matrix.util.MatrixException;
import matrix.util.SelectList;
import matrix.util.StringList;
import pss.constants.TigerConstants;

public class ExportGraphicalMBOMExcelReport_mxJPO {
    // Schema initialization
    private static String VAULT_PRODUCTION = "";

    private static String VAULT_VPLM = "";

    private static String RELATIONSHIP_PART_SPECIFICATION = "";

    private static String RELATIONSHIP_VIEWABLE = "";

    private static String RELATIONSHIP_FPDM_MBOMREL = "";

    private static String TYPE_CAD_MODEl = "";

    private static String TYPE_THUMBNAIL_VIEWABLE = "";

    private static String FORMAT_PNG = "";

    private static String SELECT_PNGFILE_ID = "";

    private static String SELECT_PNGFILE_WITHOUT_CONDITION = "";

    private static String SELECT_FILENAME = "";

    private static String SELECT_FILENAME_WITHOUT_CONDITION = "";

    // MBOM attributes initialization
    private static String ATTRIBUTE_V_DESCRIPTION = "";

    private static String ATTRIBUTE_V_NAME = "";

    private static String ATTRIBUTE_FPDM_SHORT_LENGTH_DESCRIPTION = "";

    private static String ATTRIBUTE_FPDM_GENERATED_FROM = "";

    private static String ATTRIBUTE_PLM_INSTANCE_EXTERNAL_ID = "";

    // MBOM attributes selection initialization
    private static String SELECT_PSS_MANUFACTURING_ITEM_EXT_PSS_FCS_INDEX = "";

    private static String SELECT_ATTRIBUTE_V_DESCRIPTION = "";

    private static String SELECT_ATTRIBUTE_V_NAME = "";

    private static String SELECT_ATTRIBUTE_FPDM_SHORT_LENGTH_DESCRIPTION = "";

    private static String SELECT_ATTRIBUTE_FPDM_GENERATED_FROM = "";

    private static String SELECT_ATTRIBUTE_PLM_INSTANCE_EXTERNAL_ID = "";

    // MBOMs selection initialization
    private StringBuffer stbTypeName = new StringBuffer();

    private StringBuffer stb100TypeName = new StringBuffer();

    private SelectList selectStmtsMBOM = new SelectList();

    private SelectList selectStmtsMBOMImages = new SelectList();

    private SelectList selectStmts100MBOM = new SelectList();

    private SelectList selectStmts100MBOMImages = new SelectList();

    private SelectList selectStmts150MBOM = new SelectList();

    private SelectList selectStmtsCADBOM = new SelectList();

    private SelectList selectRelStmtsMBOM = new SelectList();

    private SelectList selectStmtsMBOMFile = new SelectList();

    private SelectList selectImageDate = new SelectList();

    private static String strLanguage = "";

    private static String objCADWhere = "";

    private static String variantIDs = "";

    private static String applicationURL = "";

    private static String strCheckoutDir = "";

    private static String strImageBasePath = "";

    private static Path temp;

    public ExportGraphicalMBOMExcelReport_mxJPO(Context context, String[] args) throws IOException, MatrixException {
        // Schema definition
        VAULT_PRODUCTION = PropertyUtil.getSchemaProperty(context, "vault_eServiceProduction");
        VAULT_VPLM = PropertyUtil.getSchemaProperty(context, "vault_vplm");
        RELATIONSHIP_PART_SPECIFICATION = PropertyUtil.getSchemaProperty(context, "relationship_PartSpecification");
        RELATIONSHIP_VIEWABLE = PropertyUtil.getSchemaProperty(context, "relationship_Viewable");
        RELATIONSHIP_FPDM_MBOMREL = PropertyUtil.getSchemaProperty(context, "relationship_FPDM_MBOMRel");
        TYPE_CAD_MODEl = PropertyUtil.getSchemaProperty(context, "type_CADModel");
        TYPE_THUMBNAIL_VIEWABLE = PropertyUtil.getSchemaProperty(context, "type_ThumbnailViewable");
        FORMAT_PNG = PropertyUtil.getSchemaProperty(context, "format_PNG");
        SELECT_PNGFILE_ID = "from[" + RELATIONSHIP_VIEWABLE + "|to.type=='" + TYPE_THUMBNAIL_VIEWABLE + "'].to.id";
        SELECT_PNGFILE_WITHOUT_CONDITION = "from[" + RELATIONSHIP_VIEWABLE + "].to.id";
        SELECT_FILENAME = "from[" + RELATIONSHIP_VIEWABLE + "|to.type=='" + TYPE_THUMBNAIL_VIEWABLE + "'].to.format[" + FORMAT_PNG + "].file.name";
        SELECT_FILENAME_WITHOUT_CONDITION = "from[" + RELATIONSHIP_VIEWABLE + "].to.format[" + FORMAT_PNG + "].file.name";

        // MBOM attributes definition
        ATTRIBUTE_V_DESCRIPTION = PropertyUtil.getSchemaProperty(context, "attribute_PLMEntity.V_description");
        ATTRIBUTE_V_NAME = PropertyUtil.getSchemaProperty(context, "attribute_V_Name");
        ATTRIBUTE_FPDM_SHORT_LENGTH_DESCRIPTION = PropertyUtil.getSchemaProperty(context, "attribute_FPDM_FaureciaShortLengthDescriptionFCS");
        ATTRIBUTE_FPDM_GENERATED_FROM = PropertyUtil.getSchemaProperty(context, "attribute_FPDM_GenratedFrom");
        ATTRIBUTE_PLM_INSTANCE_EXTERNAL_ID = PropertyUtil.getSchemaProperty(context, "attribute_PLMInstance.PLM_ExternalID");

        // MBOM attributes selection definition
        SELECT_PSS_MANUFACTURING_ITEM_EXT_PSS_FCS_INDEX = "attribute[" + TigerConstants.ATTRIBUTE_PSS_MBOM_FCSINDEX + "].value";
        SELECT_ATTRIBUTE_V_DESCRIPTION = "attribute[" + ATTRIBUTE_V_DESCRIPTION + "]";
        SELECT_ATTRIBUTE_V_NAME = "attribute[" + ATTRIBUTE_V_NAME + "]";
        SELECT_ATTRIBUTE_FPDM_SHORT_LENGTH_DESCRIPTION = "attribute[" + ATTRIBUTE_FPDM_SHORT_LENGTH_DESCRIPTION + "]";
        SELECT_ATTRIBUTE_FPDM_GENERATED_FROM = "attribute[" + ATTRIBUTE_FPDM_GENERATED_FROM + "]";
        SELECT_ATTRIBUTE_PLM_INSTANCE_EXTERNAL_ID = "attribute[" + ATTRIBUTE_PLM_INSTANCE_EXTERNAL_ID + "]";

        // MBOMs selection definition
        stbTypeName.append(TigerConstants.TYPE_CREATEASSEMBLY);
        stbTypeName.append(",");
        stbTypeName.append(TigerConstants.TYPE_CREATEMATERIAL);
        stbTypeName.append(",");
        stbTypeName.append(TigerConstants.TYPE_PSS_OPERATION);
        stbTypeName.append(",");
        stbTypeName.append(TigerConstants.TYPE_PSS_LINEDATA);

        stb100TypeName.append(TigerConstants.TYPE_FPDM_MBOMPART);

        selectStmtsMBOM.add(DomainConstants.SELECT_ID);
        selectStmtsMBOM.add(DomainConstants.SELECT_TYPE);
        selectStmtsMBOM.add(DomainConstants.SELECT_NAME);
        selectStmtsMBOM.add(SELECT_PSS_MANUFACTURING_ITEM_EXT_PSS_FCS_INDEX);
        selectStmtsMBOM.add(SELECT_ATTRIBUTE_V_DESCRIPTION);
        selectStmtsMBOM.add(TigerConstants.SELECT_ATTRIBUTE_V_NAME);
        selectStmtsMBOM.add(DomainConstants.SELECT_REVISION);

        selectStmtsMBOMImages.add(TigerConstants.SELECT_ATTRIBUTE_V_NAME);
        selectStmtsMBOMImages.add(DomainConstants.SELECT_REVISION);
        selectStmtsMBOMImages.add(SELECT_PSS_MANUFACTURING_ITEM_EXT_PSS_FCS_INDEX);
        selectStmtsMBOMImages.add(SELECT_ATTRIBUTE_V_DESCRIPTION);

        selectStmts100MBOM.add(DomainConstants.SELECT_ID);
        selectStmts100MBOM.add(DomainConstants.SELECT_TYPE);
        selectStmts100MBOM.add(SELECT_ATTRIBUTE_V_NAME);
        selectStmts100MBOM.add(DomainConstants.SELECT_NAME);
        selectStmts100MBOM.add(SELECT_ATTRIBUTE_FPDM_SHORT_LENGTH_DESCRIPTION);

        selectStmts100MBOMImages.add(DomainConstants.SELECT_ID);
        selectStmts100MBOMImages.add(SELECT_ATTRIBUTE_V_NAME);
        selectStmts100MBOMImages.add(SELECT_ATTRIBUTE_FPDM_SHORT_LENGTH_DESCRIPTION);
        selectStmts100MBOMImages.add(SELECT_ATTRIBUTE_FPDM_GENERATED_FROM);

        selectStmts150MBOM.add(TigerConstants.SELECT_ATTRIBUTE_V_NAME);
        selectStmts150MBOM.add(SELECT_PSS_MANUFACTURING_ITEM_EXT_PSS_FCS_INDEX);
        selectStmts150MBOM.add(DomainConstants.SELECT_REVISION);

        selectStmtsCADBOM.add(SELECT_PNGFILE_ID);
        selectStmtsCADBOM.add(SELECT_FILENAME);

        selectRelStmtsMBOM.add(SELECT_ATTRIBUTE_PLM_INSTANCE_EXTERNAL_ID);

        selectStmtsMBOMFile.add(SELECT_ATTRIBUTE_V_NAME);
        selectStmtsMBOMFile.add(DomainConstants.SELECT_NAME);
        selectStmtsMBOMFile.add(DomainConstants.SELECT_REVISION);
        selectStmtsMBOMFile.add(SELECT_ATTRIBUTE_FPDM_SHORT_LENGTH_DESCRIPTION);

        selectImageDate.add(DomainConstants.SELECT_NAME);
        selectImageDate.add(DomainConstants.SELECT_DESCRIPTION);
        selectImageDate.add(DomainConstants.SELECT_REVISION);

        objCADWhere = "attribute[" + TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE + "]==MG";
        variantIDs = "from[" + TigerConstants.RELATIONSHIP_PSS_PARTVARIANTASSEMBLY + "].to.attribute[" + TigerConstants.ATTRIBUTE_PSS_VARIANTID + "].value";
        temp = Files.createTempDirectory("TMP_Export_");
        strCheckoutDir = System.getProperty("java.io.tmpdir") + File.separator + temp.getFileName() + File.separator;
        applicationURL = MailUtil.getBaseURL(context);

        if (UIUtil.isNotNullAndNotEmpty(applicationURL)) {
            String strMBOMPath = EnoviaResourceBundle.getProperty(context, "FRCMBOMCentral", "FPDM_FRCMBOMCentral.label.ExportGBOMImageBasePath", strLanguage);

            if (UIUtil.isNotNullAndNotEmpty(strMBOMPath)) {
                String[] tokens = applicationURL.split("common");
                strImageBasePath = tokens[0] + strMBOMPath;
            }
        }
    }

    @SuppressWarnings({ "rawtypes", "resource" })
    public String exportGraphicalMBOM(Context context, String[] args) throws MatrixException, Exception {
        XSSFWorkbook workbook = new XSSFWorkbook();
        Map<String, XSSFCellStyle> styles = createStyles(workbook);
        StringList slObjectIdsList = null;
        String strHeaderColumnName = EnoviaResourceBundle.getProperty(context, "FRCMBOMCentral", "FPDM_FRCMBOMCentral.ColumnHeader.ExportGMBOMReport", strLanguage);
        String strHeaderColumnNameWithImages = EnoviaResourceBundle.getProperty(context, "FRCMBOMCentral", "FPDM_FRCMBOMCentral.ColumnHeader.ExportGMBOMReportImages", strLanguage);
        int intHeaderSize = getColumnHeaderSize(context, strHeaderColumnName);
        int intHeaderSizeWithImages = getColumnHeaderSize(context, strHeaderColumnNameWithImages);

        HashMap jpoMap = (HashMap) JPO.unpackArgs(args);
        String strObjectId = (String) jpoMap.get("objectId");
        boolean boolMBOMWithPictures = Boolean.parseBoolean((String) jpoMap.get("bMBOMPictures"));
        boolean bool150MBOM = Boolean.parseBoolean((String) jpoMap.get("b150MBOM"));
        strLanguage = (String) jpoMap.get("sLanguage");

        if (bool150MBOM) {
            if (UIUtil.isNotNullAndNotEmpty(strObjectId)) {
                slObjectIdsList = new StringList(strObjectId);
            }
        } else {
            strObjectId = (String) jpoMap.get("sIds");
            if (UIUtil.isNotNullAndNotEmpty(strObjectId)) {
                slObjectIdsList = FrameworkUtil.split(strObjectId, "|");
                slObjectIdsList.remove("");
            }
        }

        for (int i = 0; i < slObjectIdsList.size(); i++) {
            HashMap<Integer, Map<Integer, String>> hmReportData = new HashMap<Integer, Map<Integer, String>>();
            String strSheetName = "";
            String strObjId = (String) slObjectIdsList.get(i);

            if (UIUtil.isNotNullAndNotEmpty(strObjId)) {
                DomainObject doobj = DomainObject.newInstance(context, strObjId);

                if (bool150MBOM) {
                    strSheetName = doobj.getInfo(context, TigerConstants.SELECT_ATTRIBUTE_V_NAME);
                    strSheetName = "MBOM150% " + strSheetName;
                } else {
                    strSheetName = doobj.getInfo(context, SELECT_ATTRIBUTE_V_NAME);
                    strSheetName = "MBOM100% " + strSheetName;
                }

                XSSFSheet sheet = workbook.createSheet(strSheetName);
                sheet = setColumnWidthForSheet(context, sheet);
                sheet = CreateHeaderforExcelReport(context, sheet, styles, strHeaderColumnName);

                if (bool150MBOM) {
                    hmReportData = processObject(context, doobj, selectStmtsMBOM, true);
                } else {
                    hmReportData = process100Object(context, doobj, selectStmts100MBOM, true);
                }
                workbook = WriteExcelFiles(context, hmReportData, sheet, styles, intHeaderSize, workbook, 26f, true);

                if (boolMBOMWithPictures) {
                    strSheetName = strSheetName + "_With Images";
                    sheet = workbook.createSheet(strSheetName);
                    sheet = setColumnWidthForSheetWithImages(context, sheet);
                    sheet = CreateHeaderforExcelReport(context, sheet, styles, strHeaderColumnNameWithImages);

                    if (bool150MBOM) {
                        hmReportData = processObject(context, doobj, selectStmtsMBOMImages, false);
                    } else {
                        hmReportData = process100Object(context, doobj, selectStmts100MBOMImages, false);
                    }
                    workbook = WriteExcelFiles(context, hmReportData, sheet, styles, intHeaderSizeWithImages, workbook, 104f, false);
                }
            }
        }

        FileUtils.deleteDirectory(new File(strCheckoutDir));
        ByteArrayOutputStream outByteStream = new ByteArrayOutputStream();
        workbook.write(outByteStream);
        byte[] outArray = outByteStream.toByteArray();
        String sFile = javax.xml.bind.DatatypeConverter.printBase64Binary(outArray);

        return sFile;
    }

    @SuppressWarnings("rawtypes")
    public ArrayList<String> exportGraphicalMBOMInMultipleFiles(Context context, String[] args) throws MatrixException, Exception {
        ArrayList<String> alData = new ArrayList<String>();
        StringBuffer sbFileData = new StringBuffer();
        StringBuffer sbFileName = new StringBuffer();
        StringList slObjectIdsList = null;
        String strHeaderColumnName = EnoviaResourceBundle.getProperty(context, "FRCMBOMCentral", "FPDM_FRCMBOMCentral.ColumnHeader.ExportGMBOMReport", strLanguage);
        String strHeaderColumnNameWithImages = EnoviaResourceBundle.getProperty(context, "FRCMBOMCentral", "FPDM_FRCMBOMCentral.ColumnHeader.ExportGMBOMReportImages", strLanguage);
        int intHeaderSize = getColumnHeaderSize(context, strHeaderColumnName);
        int intHeaderSizeWithImages = getColumnHeaderSize(context, strHeaderColumnNameWithImages);

        HashMap jpoMap = (HashMap) JPO.unpackArgs(args);
        String strObjectId = (String) jpoMap.get("sIds");
        boolean boolMBOMWithPictures = Boolean.parseBoolean((String) jpoMap.get("bMBOMPictures"));
        strLanguage = (String) jpoMap.get("sLanguage");

        if (UIUtil.isNotNullAndNotEmpty(strObjectId)) {
            slObjectIdsList = FrameworkUtil.split(strObjectId, "|");
            slObjectIdsList.remove("");
        }

        int intIdsSize = slObjectIdsList.size();
        for (int i = 0; i < intIdsSize; i++) {
            String strObjId = (String) slObjectIdsList.get(i);

            if (UIUtil.isNotNullAndNotEmpty(strObjId)) {
                XSSFWorkbook workbook = new XSSFWorkbook();
                Map<String, XSSFCellStyle> styles = createStyles(workbook);
                DomainObject doobj = DomainObject.newInstance(context, strObjId);
                Map mapMBOM = (Map) doobj.getInfo(context, selectStmtsMBOMFile);
                String strSheetName = "MBOM100% " + ((String) mapMBOM.get(SELECT_ATTRIBUTE_V_NAME));
                XSSFSheet sheet = workbook.createSheet(strSheetName);
                sheet = setColumnWidthForSheet(context, sheet);
                sheet = CreateHeaderforExcelReport(context, sheet, styles, strHeaderColumnName);
                sbFileName.append(((String) mapMBOM.get(DomainConstants.SELECT_NAME)) + "_" + ((String) mapMBOM.get(DomainConstants.SELECT_REVISION)) + "_"
                        + ((String) mapMBOM.get(SELECT_ATTRIBUTE_FPDM_SHORT_LENGTH_DESCRIPTION)) + ".xlsx");

                HashMap<Integer, Map<Integer, String>> hmReportData = process100Object(context, doobj, selectStmts100MBOM, true);
                workbook = WriteExcelFiles(context, hmReportData, sheet, styles, intHeaderSize, workbook, 26f, true);

                if (boolMBOMWithPictures) {
                    strSheetName = strSheetName + "_With Images";
                    sheet = workbook.createSheet(strSheetName);
                    sheet = setColumnWidthForSheetWithImages(context, sheet);
                    sheet = CreateHeaderforExcelReport(context, sheet, styles, strHeaderColumnNameWithImages);

                    hmReportData = process100Object(context, doobj, selectStmts100MBOMImages, false);
                    workbook = WriteExcelFiles(context, hmReportData, sheet, styles, intHeaderSizeWithImages, workbook, 104f, false);
                }

                ByteArrayOutputStream outByteStream = new ByteArrayOutputStream();
                workbook.write(outByteStream);
                byte[] outArray = outByteStream.toByteArray();
                sbFileData.append(javax.xml.bind.DatatypeConverter.printBase64Binary(outArray));

                if (i != (intIdsSize - 1)) {
                    sbFileData.append("`");
                    sbFileName.append("`");
                }
            }
        }

        alData.add(sbFileData.toString());
        alData.add(sbFileName.toString());
        FileUtils.deleteDirectory(new File(strCheckoutDir));

        return alData;
    }

    public XSSFWorkbook WriteExcelFiles(Context context, HashMap<Integer, Map<Integer, String>> hmTemp, XSSFSheet sheettemp, Map<String, XSSFCellStyle> styles, int intColumnSize,
            XSSFWorkbook workbook, float fValue, boolean bValue) throws Exception {
        XSSFRow row;
        String strImageFilePath = "";
        int rowNumber = 1;

        if (!bValue) {
            checkoutImagesDirectory(context, hmTemp);
        }

        for (int j = 0; j < hmTemp.size(); j++) {
            Map<Integer, String> map = hmTemp.get(j);
            row = sheettemp.createRow(rowNumber);
            row.setHeightInPoints(fValue);

            for (int k = 0; k < intColumnSize; k++) {
                if (k != (intColumnSize - 1)) {
                    XSSFCell cell = row.createCell(k);
                    cell.setCellValue((String) map.get(k));
                    cell.setCellStyle(styles.get("data"));
                } else {
                    XSSFCell workCell = row.createCell(k);
                    workCell.setCellStyle(styles.get("data"));

                    strImageFilePath = (String) map.get(21);
                    if (UIUtil.isNotNullAndNotEmpty(strImageFilePath)) {
                        insertImageInExcel(context, strImageFilePath, workbook, sheettemp, workCell);
                    }
                }
            }
            rowNumber++;
        }

        return workbook;
    }

    public void checkoutImagesDirectory(Context context, HashMap<Integer, Map<Integer, String>> hmTemp) throws Exception {

        String strImageFileName = "";
        String strImageFileId = "";
        ArrayList<BusinessObjectProxy> alFiles = new ArrayList<BusinessObjectProxy>();
        TicketWrapper ticket = null;

        for (int j = 0; j < hmTemp.size(); j++) {
            Map<Integer, String> map = hmTemp.get(j);
            strImageFileName = (String) map.get(21);
            strImageFileId = (String) map.get(22);

            if (UIUtil.isNotNullAndNotEmpty(strImageFileName) && UIUtil.isNotNullAndNotEmpty(strImageFileId)) {
                BusinessObjectProxy bop = new BusinessObjectProxy(strImageFileId, "PNG", strImageFileName);
                alFiles.add(bop);
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

    }

    public static void insertImageInExcel(Context context, String strImageFileName, XSSFWorkbook workbook, XSSFSheet sheettemp, XSSFCell workCell) throws Exception {
        InputStream isImage = new FileInputStream(strImageFileName);
        byte[] bytes = IOUtils.toByteArray(isImage);
        int pictureID = workbook.addPicture(bytes, Workbook.PICTURE_TYPE_PNG);
        isImage.close();

        XSSFDrawing drawing = sheettemp.createDrawingPatriarch();
        XSSFClientAnchor anchor = new XSSFClientAnchor();
        anchor.setCol1(workCell.getColumnIndex());
        anchor.setRow1(workCell.getRowIndex());

        XSSFPicture newPicture = drawing.createPicture(anchor, pictureID);
        int imageHeight = fpdm.excelreport.PixelUtil_mxJPO.heightUnits2Pixel((short) workCell.getRow().getHeight());
        int imageWidth = fpdm.excelreport.PixelUtil_mxJPO.widthUnits2Pixel((short) sheettemp.getColumnWidth(workCell.getColumnIndex()));

        File fileImage = new File(strImageFileName);
        BufferedImage bimg = ImageIO.read(fileImage);
        int oldImageWidth = bimg.getWidth();
        int oldImageHeight = bimg.getHeight();
        double ratioHeight = (double) imageHeight / (double) oldImageHeight;
        double ratioWidth = (double) imageWidth / (double) oldImageWidth;

        double ratioimage = 1;
        if (ratioHeight < ratioWidth) {
            ratioimage = ratioHeight;
        } else {
            ratioimage = ratioWidth;
        }

        newPicture.resize(ratioimage);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public HashMap<Integer, Map<Integer, String>> processObject(Context context, DomainObject domObj, SelectList slSelectData, boolean bValue) throws Exception {
        HashMap<Integer, Map<Integer, String>> hmData = new HashMap<Integer, Map<Integer, String>>();
        Map<String, String> mapFirstPart = domObj.getInfo(context, slSelectData);
        int rowNumber = 1;

        if (bValue) {
            Map<Integer, String> mapParent = setReportsMBOMData(context, mapFirstPart, true);
            hmData.put(0, mapParent);
        } else {
            Map<Integer, String> mapParent = setReportsMBOMDataImages(context, mapFirstPart, true);
            hmData.put(0, mapParent);
        }

        ContextUtil.startTransaction(context, false);
        MapList mlMBOMData = FrameworkUtil.toMapList(domObj.getExpansionIterator(context, TigerConstants.RELATIONSHIP_DELFMIFUNCTIONIDENTIFIEDINSTANCE, stbTypeName.toString(), slSelectData,
                selectRelStmtsMBOM, false, true, (short) 0, DomainObject.EMPTY_STRING, DomainObject.EMPTY_STRING, (short) 0, false, false, (short) 100, false), (short) 0, null, null, null, null);
        ContextUtil.commitTransaction(context);

        Iterator iterator = mlMBOMData.iterator();
        while (iterator.hasNext()) {
            Map mapMBOM = (Map) iterator.next();

            if (bValue) {
                Map<Integer, String> mapChild = setReportsMBOMData(context, mapMBOM, false);
                hmData.put(rowNumber, mapChild);
            } else {
                Map<Integer, String> mapChild = setReportsMBOMDataImages(context, mapMBOM, false);
                hmData.put(rowNumber, mapChild);
            }
            rowNumber++;
        }

        return hmData;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public HashMap<Integer, Map<Integer, String>> process100Object(Context context, DomainObject domObj, SelectList slSelectData, boolean bValue) throws Exception {
        HashMap<Integer, Map<Integer, String>> hmData = new HashMap<Integer, Map<Integer, String>>();
        Map<String, String> mapFirstPart = domObj.getInfo(context, slSelectData);
        int rowNumber = 1;

        if (bValue) {
            Map<Integer, String> mapParent = setReports100MBOMData(context, mapFirstPart, true);
            hmData.put(0, mapParent);
        } else {
            Map<Integer, String> mapParent = setReports100MBOMDataImages(context, mapFirstPart, true);
            hmData.put(0, mapParent);
        }

        ContextUtil.startTransaction(context, false);
        MapList mlMBOMData = FrameworkUtil.toMapList(domObj.getExpansionIterator(context, RELATIONSHIP_FPDM_MBOMREL, stb100TypeName.toString(), slSelectData, new StringList(0), false, true, (short) 0,
                DomainObject.EMPTY_STRING, DomainObject.EMPTY_STRING, (short) 0, false, false, (short) 100, false), (short) 0, null, null, null, null);
        ContextUtil.commitTransaction(context);

        Iterator iterator = mlMBOMData.iterator();
        while (iterator.hasNext()) {
            Map mapMBOM = (Map) iterator.next();

            if (bValue) {
                Map<Integer, String> mapChild = setReports100MBOMData(context, mapMBOM, false);
                hmData.put(rowNumber, mapChild);
            } else {
                Map<Integer, String> mapChild = setReports100MBOMDataImages(context, mapMBOM, false);
                hmData.put(rowNumber, mapChild);
            }
            rowNumber++;
        }

        return hmData;
    }

    public Map<Integer, String> setReportsMBOMData(Context context, Map<String, String> mapPartLocal, boolean isParent) throws Exception {
        Map<Integer, String> mapTemp = new HashMap<Integer, String>();

        String strObjectType = (String) mapPartLocal.get(DomainConstants.SELECT_TYPE);
        String strImagePath = "";
        String strFinalPath = "";

        if (isParent) {
            mapTemp.put(0, "0");
        } else {
            mapTemp.put(0, (String) mapPartLocal.get("level"));
        }

        mapTemp.put(1, (String) mapPartLocal.get(SELECT_ATTRIBUTE_PLM_INSTANCE_EXTERNAL_ID));
        mapTemp.put(2, (String) mapPartLocal.get(DomainConstants.SELECT_NAME));
        mapTemp.put(3, (String) mapPartLocal.get(SELECT_PSS_MANUFACTURING_ITEM_EXT_PSS_FCS_INDEX));
        mapTemp.put(4, (String) mapPartLocal.get(SELECT_ATTRIBUTE_V_DESCRIPTION));
        mapTemp.put(5, (String) mapPartLocal.get(TigerConstants.SELECT_ATTRIBUTE_V_NAME));
        mapTemp.put(6, (String) mapPartLocal.get(DomainConstants.SELECT_REVISION));
        mapTemp.put(7, getVariantID(context, (String) mapPartLocal.get(DomainConstants.SELECT_ID)));

        if (UIUtil.isNotNullAndNotEmpty(strObjectType) && (strObjectType.equalsIgnoreCase(TigerConstants.TYPE_CREATEASSEMBLY) || strObjectType.equalsIgnoreCase(TigerConstants.TYPE_CREATEMATERIAL)
                || strObjectType.equalsIgnoreCase(TigerConstants.TYPE_PSS_OPERATION) || strObjectType.equalsIgnoreCase(TigerConstants.TYPE_PSS_LINEDATA))) {
            strImagePath = EnoviaResourceBundle.getProperty(context, "FRCMBOMCentral", "FPDM_FRCMBOMCentral.label.ExportGBOMImagePath." + strObjectType, strLanguage);
        }

        if (UIUtil.isNotNullAndNotEmpty(strImageBasePath) && UIUtil.isNotNullAndNotEmpty(strImagePath)) {
            try (InputStream in = new URL(strImageBasePath + strImagePath).openStream()) {
                strFinalPath = strCheckoutDir + strImagePath;
                File f = new File(strFinalPath);

                if (!f.exists() && !f.isDirectory()) {
                    Files.copy(in, Paths.get(strFinalPath));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            mapTemp.put(21, strFinalPath);
        }

        return mapTemp;
    }

    public Map<Integer, String> setReportsMBOMDataImages(Context context, Map<String, String> mapPartLocal, boolean isParent) throws Exception {
        Map<Integer, String> mapTemp = new HashMap<Integer, String>();
        String strObjectName = (String) mapPartLocal.get(TigerConstants.SELECT_ATTRIBUTE_V_NAME);
        String strPartRevision = (String) mapPartLocal.get(SELECT_PSS_MANUFACTURING_ITEM_EXT_PSS_FCS_INDEX);
        String strStructureRevision = (String) mapPartLocal.get(DomainConstants.SELECT_REVISION);

        if (isParent) {
            mapTemp.put(0, "0");
        } else {
            mapTemp.put(0, (String) mapPartLocal.get("level"));
        }

        mapTemp.put(1, strObjectName);
        mapTemp.put(2, (String) mapPartLocal.get(SELECT_ATTRIBUTE_V_DESCRIPTION));

        if (UIUtil.isNotNullAndNotEmpty(strObjectName) && UIUtil.isNotNullAndNotEmpty(strPartRevision) && UIUtil.isNotNullAndNotEmpty(strStructureRevision)) {
            BusinessObject boEngineeringPart = new BusinessObject(DomainConstants.TYPE_PART, strObjectName, strPartRevision, VAULT_PRODUCTION);
            BusinessObject boStructurePart = new BusinessObject(TigerConstants.TYPE_VPMREFERENCE, strObjectName, strStructureRevision, VAULT_VPLM);

            if (boEngineeringPart.exists(context) && boStructurePart.exists(context)) {
                boEngineeringPart.open(context);
                String strPartId = boEngineeringPart.getObjectId();
                boEngineeringPart.close(context);

                if (UIUtil.isNotNullAndNotEmpty(strPartId)) {
                    StringList slCAData = getCADImageData(context, strPartId);

                    if (slCAData.size() == 2) {
                        mapTemp.put(21, strCheckoutDir + (String) slCAData.get(1));
                        mapTemp.put(22, (String) slCAData.get(0));
                    }
                }
            }
        }
        return mapTemp;
    }

    public Map<Integer, String> setReports100MBOMData(Context context, Map<String, String> mapPartLocal, boolean isParent) throws Exception {
        Map<Integer, String> mapTemp = new HashMap<Integer, String>();
        String strObjectType = (String) mapPartLocal.get(DomainConstants.SELECT_TYPE);
        String strImagePath = "";
        String strFinalPath = "";

        if (isParent) {
            mapTemp.put(0, "0");
        } else {
            mapTemp.put(0, (String) mapPartLocal.get("level"));
        }

        mapTemp.put(1, (String) mapPartLocal.get(SELECT_ATTRIBUTE_V_NAME));
        mapTemp.put(2, (String) mapPartLocal.get(DomainConstants.SELECT_NAME));
        mapTemp.put(4, (String) mapPartLocal.get(SELECT_ATTRIBUTE_FPDM_SHORT_LENGTH_DESCRIPTION));

        if (UIUtil.isNotNullAndNotEmpty(strObjectType) && (strObjectType.equalsIgnoreCase(TigerConstants.TYPE_FPDM_MBOMPART))) {
            strImagePath = EnoviaResourceBundle.getProperty(context, "FRCMBOMCentral", "FPDM_FRCMBOMCentral.label.ExportGBOMImagePath." + strObjectType, strLanguage);
        }

        if (UIUtil.isNotNullAndNotEmpty(strImageBasePath) && UIUtil.isNotNullAndNotEmpty(strImagePath)) {
            try (InputStream in = new URL(strImageBasePath + strImagePath).openStream()) {
                strFinalPath = strCheckoutDir + strImagePath;
                File f = new File(strFinalPath);

                if (!f.exists() && !f.isDirectory()) {
                    Files.copy(in, Paths.get(strFinalPath));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            mapTemp.put(21, strFinalPath);
        }

        return mapTemp;
    }

    @SuppressWarnings("rawtypes")
    public Map<Integer, String> setReports100MBOMDataImages(Context context, Map<String, String> mapPartLocal, boolean isParent) throws Exception {
        Map<Integer, String> mapTemp = new HashMap<Integer, String>();
        String strObjectId = (String) mapPartLocal.get(DomainConstants.SELECT_ID);
        String objectWhere = "name==" + (String) mapPartLocal.get(SELECT_ATTRIBUTE_FPDM_GENERATED_FROM);

        if (isParent) {
            mapTemp.put(0, "0");
        } else {
            mapTemp.put(0, (String) mapPartLocal.get("level"));
        }

        mapTemp.put(1, (String) mapPartLocal.get(SELECT_ATTRIBUTE_V_NAME));
        mapTemp.put(2, (String) mapPartLocal.get(SELECT_ATTRIBUTE_FPDM_SHORT_LENGTH_DESCRIPTION));

        if (UIUtil.isNotNullAndNotEmpty(strObjectId)) {
            DomainObject doobj = DomainObject.newInstance(context, strObjectId);

            ContextUtil.startTransaction(context, false);
            MapList mlMBOMData = FrameworkUtil.toMapList(doobj.getExpansionIterator(context, TigerConstants.RELATIONSHIP_FPDM_GENERATEDMBOM, stbTypeName.toString(), selectStmts150MBOM,
                    new StringList(0), true, false, (short) 0, objectWhere, DomainObject.EMPTY_STRING, (short) 0, false, false, (short) 100, false), (short) 0, null, null, null, null);
            ContextUtil.commitTransaction(context);

            if (mlMBOMData.size() == 1) {
                Map mapData = (Map) mlMBOMData.get(0);
                String strObjectName = (String) mapData.get(TigerConstants.SELECT_ATTRIBUTE_V_NAME);
                String strPartRevision = (String) mapData.get(SELECT_PSS_MANUFACTURING_ITEM_EXT_PSS_FCS_INDEX);
                String strStructureRevision = (String) mapData.get(DomainConstants.SELECT_REVISION);

                if (UIUtil.isNotNullAndNotEmpty(strObjectName) && UIUtil.isNotNullAndNotEmpty(strPartRevision) && UIUtil.isNotNullAndNotEmpty(strStructureRevision)) {
                    BusinessObject boEngineeringPart = new BusinessObject(DomainConstants.TYPE_PART, strObjectName, strPartRevision, VAULT_PRODUCTION);
                    BusinessObject boStructurePart = new BusinessObject(TigerConstants.TYPE_VPMREFERENCE, strObjectName, strStructureRevision, VAULT_VPLM);

                    if (boEngineeringPart.exists(context) && boStructurePart.exists(context)) {
                        boEngineeringPart.open(context);
                        String strPartId = boEngineeringPart.getObjectId();
                        boEngineeringPart.close(context);

                        if (UIUtil.isNotNullAndNotEmpty(strPartId)) {
                            StringList slCAData = getCADImageData(context, strPartId);

                            if (slCAData.size() == 2) {
                                mapTemp.put(21, strCheckoutDir + (String) slCAData.get(1));
                                mapTemp.put(22, (String) slCAData.get(0));
                            }
                        }
                    }
                }
            }
        }
        return mapTemp;
    }

    @SuppressWarnings("rawtypes")
    public StringList getCADImageData(Context context, String strPartId) throws Exception {
        StringList slCADImages = new StringList();
        DomainObject domobj = DomainObject.newInstance(context, strPartId);

        ContextUtil.startTransaction(context, false);
        MapList mlCADData = FrameworkUtil.toMapList(domobj.getExpansionIterator(context, RELATIONSHIP_PART_SPECIFICATION, TYPE_CAD_MODEl, new StringList(DomainConstants.SELECT_ID), new StringList(0),
                true, true, (short) 0, objCADWhere, DomainObject.EMPTY_STRING, (short) 0, false, false, (short) 100, false), (short) 0, null, null, null, null);
        ContextUtil.commitTransaction(context);

        if (mlCADData.size() == 1) {
            String strCADDataId = (String) ((Map) mlCADData.get(0)).get(DomainConstants.SELECT_ID);

            if (UIUtil.isNotNullAndNotEmpty(strCADDataId)) {
                DomainObject domCAD = DomainObject.newInstance(context, strCADDataId);

                Map mapCADData = domCAD.getInfo(context, selectStmtsCADBOM);
                slCADImages.add(mapCADData.get(SELECT_PNGFILE_WITHOUT_CONDITION));
                slCADImages.add(mapCADData.get(SELECT_FILENAME_WITHOUT_CONDITION));
            }
        }
        return slCADImages;
    }

    public String getVariantID(Context context, String strObjectId) throws Exception {
        StringBuffer sbVariantID = new StringBuffer();

        if (UIUtil.isNotNullAndNotEmpty(strObjectId)) {
            DomainObject dom = DomainObject.newInstance(context, strObjectId);
            String strStructureNode = dom.getInfo(context, TigerConstants.SELECT_ATTRIBUTE_PSS_MBOM_STRUCTURENODE);

            if (strStructureNode.equalsIgnoreCase("Yes")) {
                StringList slVariantIds = dom.getInfoList(context, variantIDs);
                int intProductConfigSize = slVariantIds.size();

                for (int i = 0; i < intProductConfigSize; i++) {
                    sbVariantID.append(slVariantIds.get(i));

                    if (i != (intProductConfigSize - 1)) {
                        sbVariantID.append("|");
                    }
                }
            }
        }
        return sbVariantID.toString();
    }

    public int getColumnHeaderSize(Context context, String strHeader) throws Exception {
        StringList slReportColumnHeaders = null;

        if (UIUtil.isNotNullAndNotEmpty(strHeader)) {
            slReportColumnHeaders = FrameworkUtil.split(strHeader, "|");
        }

        return slReportColumnHeaders.size();
    }

    public XSSFSheet CreateHeaderforExcelReport(Context context, XSSFSheet sheettemp, Map<String, XSSFCellStyle> styles, String strReportColumnHeaders) throws Exception {
        XSSFRow row = sheettemp.createRow(0);
        row.setHeightInPoints(26f);

        if (UIUtil.isNotNullAndNotEmpty(strReportColumnHeaders)) {
            StringList slReportColumnHeaders = FrameworkUtil.split(strReportColumnHeaders, "|");
            for (int i = 0; i < slReportColumnHeaders.size(); i++) {
                XSSFCell cell = row.createCell(i);
                cell.setCellValue((String) slReportColumnHeaders.get(i));
                cell.setCellStyle(styles.get("header"));
            }
        }
        return sheettemp;
    }

    public XSSFSheet setColumnWidthForSheet(Context context, XSSFSheet sheettemp) throws Exception {
        sheettemp.setColumnWidth(1, 4000);
        sheettemp.setColumnWidth(2, 6000);
        sheettemp.setColumnWidth(4, 8000);
        sheettemp.setColumnWidth(5, 4000);
        sheettemp.setColumnWidth(6, 4000);
        sheettemp.setColumnWidth(7, 4000);

        return sheettemp;
    }

    public XSSFSheet setColumnWidthForSheetWithImages(Context context, XSSFSheet sheettemp) throws Exception {
        sheettemp.setColumnWidth(1, 4000);
        sheettemp.setColumnWidth(2, 8000);
        sheettemp.setColumnWidth(3, 8000);

        return sheettemp;
    }

    public XSSFCellStyle createBorderedStyle(XSSFWorkbook wb) throws Exception {
        BorderStyle thin = BorderStyle.THIN;
        short black = IndexedColors.BLACK.getIndex();

        XSSFCellStyle style = wb.createCellStyle();
        style.setBorderRight(thin);
        style.setRightBorderColor(black);
        style.setBorderBottom(thin);
        style.setBottomBorderColor(black);
        style.setBorderLeft(thin);
        style.setLeftBorderColor(black);
        style.setBorderTop(thin);
        style.setTopBorderColor(black);

        return style;
    }

    public Map<String, XSSFCellStyle> createStyles(XSSFWorkbook wb) throws Exception {
        Map<String, XSSFCellStyle> styles = new HashMap<String, XSSFCellStyle>();
        XSSFCellStyle style;
        XSSFFont headerFont = wb.createFont();
        headerFont.setFontHeightInPoints((short) 10);

        style = createBorderedStyle(wb);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFont(headerFont);
        style.setWrapText(true);
        styles.put("header", style);

        style = createBorderedStyle(wb);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFont(headerFont);
        style.setWrapText(true);
        styles.put("data", style);

        return styles;
    }

    @SuppressWarnings("rawtypes")
    public ArrayList<String> exportGraphicalMBOMOnlyImages(Context context, String[] args) throws MatrixException, Exception {
        ArrayList<String> alData = new ArrayList<String>();

        HashMap jpoMap = (HashMap) JPO.unpackArgs(args);
        String strObjectId = (String) jpoMap.get("objectId");
        strLanguage = (String) jpoMap.get("sLanguage");

        if (UIUtil.isNotNullAndNotEmpty(strObjectId)) {
            DomainObject doobj = DomainObject.newInstance(context, strObjectId);
            MapList mlReportData = processImages(context, doobj, selectStmtsMBOMImages);

            alData = checkoutImages(context, mlReportData);
        }

        return alData;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public MapList processImages(Context context, DomainObject domObj, SelectList slSelectData) throws Exception {
        MapList mlData = new MapList();
        Map<String, String> mapFirstPart = domObj.getInfo(context, slSelectData);
        Map<Integer, String> mapParent = setDataImages(context, mapFirstPart);
        if (!mapParent.isEmpty()) {
            mlData.add(mapParent);
        }

        ContextUtil.startTransaction(context, false);
        MapList mlMBOMData = FrameworkUtil.toMapList(domObj.getExpansionIterator(context, TigerConstants.RELATIONSHIP_DELFMIFUNCTIONIDENTIFIEDINSTANCE, stbTypeName.toString(), slSelectData,
                new StringList(0), false, true, (short) 0, DomainObject.EMPTY_STRING, DomainObject.EMPTY_STRING, (short) 0, false, false, (short) 100, false), (short) 0, null, null, null, null);
        ContextUtil.commitTransaction(context);

        Iterator iterator = mlMBOMData.iterator();
        while (iterator.hasNext()) {
            Map mapMBOM = (Map) iterator.next();
            Map<Integer, String> mapChild = setDataImages(context, mapMBOM);
            if (!mapChild.isEmpty()) {
                mlData.add(mapChild);
            }
        }
        return mlData;
    }

    @SuppressWarnings({ "unchecked" })
    public Map<Integer, String> setDataImages(Context context, Map<String, String> mapPartLocal) throws Exception {
        Map<Integer, String> mapTemp = new HashMap<Integer, String>();
        String strObjectName = (String) mapPartLocal.get(TigerConstants.SELECT_ATTRIBUTE_V_NAME);
        String strPartRevision = (String) mapPartLocal.get(SELECT_PSS_MANUFACTURING_ITEM_EXT_PSS_FCS_INDEX);
        String strStructureRevision = (String) mapPartLocal.get(DomainConstants.SELECT_REVISION);

        if (UIUtil.isNotNullAndNotEmpty(strObjectName) && UIUtil.isNotNullAndNotEmpty(strPartRevision) && UIUtil.isNotNullAndNotEmpty(strStructureRevision)) {
            BusinessObject boEngineeringPart = new BusinessObject(DomainConstants.TYPE_PART, strObjectName, strPartRevision, VAULT_PRODUCTION);
            BusinessObject boStructurePart = new BusinessObject(TigerConstants.TYPE_VPMREFERENCE, strObjectName, strStructureRevision, VAULT_VPLM);

            if (boEngineeringPart.exists(context) && boStructurePart.exists(context)) {
                boEngineeringPart.open(context);
                String strPartId = boEngineeringPart.getObjectId();
                boEngineeringPart.close(context);

                if (UIUtil.isNotNullAndNotEmpty(strPartId)) {
                    DomainObject doPart = DomainObject.newInstance(context, strPartId);
                    Map<String, String> mapFirstPart = (Map<String, String>) doPart.getInfo(context, selectImageDate);
                    mapTemp.put(0, (String) mapFirstPart.get(DomainConstants.SELECT_NAME) + "_" + (String) mapFirstPart.get(DomainConstants.SELECT_REVISION) + "_"
                            + (String) mapFirstPart.get(DomainConstants.SELECT_DESCRIPTION) + ".png");

                    StringList slCAData = getCADImageData(context, strPartId);
                    if (slCAData.size() == 2) {
                        mapTemp.put(1, (String) slCAData.get(1));
                        mapTemp.put(2, (String) slCAData.get(0));
                    }
                }
            }
        }
        return mapTemp;
    }

    @SuppressWarnings("rawtypes")
    public ArrayList<String> checkoutImages(Context context, MapList mlTemp) throws Exception {
        StringList slDataPath = new StringList();
        StringList slDataName = new StringList();
        ArrayList<String> alData = new ArrayList<String>();
        StringBuffer sbFileData = new StringBuffer();
        StringBuffer sbFileName = new StringBuffer();

        String strImageFileName = "";
        String strImageFileId = "";
        ArrayList<BusinessObjectProxy> alFiles = new ArrayList<BusinessObjectProxy>();
        TicketWrapper ticket = null;

        for (int j = 0; j < mlTemp.size(); j++) {
            Map map = (Map) mlTemp.get(j);
            strImageFileName = (String) map.get(1);
            strImageFileId = (String) map.get(2);

            if (UIUtil.isNotNullAndNotEmpty(strImageFileName) && UIUtil.isNotNullAndNotEmpty(strImageFileId)) {
                BusinessObjectProxy bop = new BusinessObjectProxy(strImageFileId, "PNG", strImageFileName);
                alFiles.add(bop);
                slDataName.add((String) map.get(0));
                slDataPath.add(strCheckoutDir + strImageFileName);
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

                int intImageSize = slDataPath.size();
                for (int k = 0; k < intImageSize; k++) {
                    InputStream isImage = new FileInputStream((String) slDataPath.get(k));
                    byte[] bytes = IOUtils.toByteArray(isImage);
                    sbFileData.append(javax.xml.bind.DatatypeConverter.printBase64Binary(bytes));
                    sbFileName.append((String) slDataName.get(k));

                    if (k != (intImageSize - 1)) {
                        sbFileData.append("`");
                        sbFileName.append("`");
                    }
                }
            } catch (MatrixException e) {
                e.printStackTrace();
            }
        }

        alData.add(sbFileData.toString());
        alData.add(sbFileName.toString());
        FileUtils.deleteDirectory(new File(strCheckoutDir));

        return alData;
    }
}