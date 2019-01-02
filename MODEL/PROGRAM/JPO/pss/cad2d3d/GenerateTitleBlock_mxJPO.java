package pss.cad2d3d;

import java.awt.Color;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.io.RandomAccessBufferedFileInputStream;
import org.apache.pdfbox.io.ScratchFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.util.Matrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.Job;
import com.matrixone.apps.domain.util.BackgroundProcess;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.framework.ui.UIUtil;

import be.quodlibet.boxable.BaseTable;
import be.quodlibet.boxable.Cell;
import be.quodlibet.boxable.HorizontalAlignment;
import be.quodlibet.boxable.ImageCell;
import be.quodlibet.boxable.Row;
import be.quodlibet.boxable.VerticalAlignment;
import be.quodlibet.boxable.image.Image;
import be.quodlibet.boxable.line.LineStyle;
import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.ExpansionIterator;
import matrix.db.FileItr;
import matrix.db.FileList;
import matrix.util.MatrixException;
import matrix.util.StringList;
import pss.common.utils.TigerAppConfigProperties;
import pss.common.utils.TigerEnums;
import pss.common.utils.TigerEnums.FileExtension;
import pss.common.utils.TigerEnums.FileFormat;
import pss.common.utils.TigerEnums.Font;
import pss.common.utils.TigerEnums.TitleBlockPattern;
import pss.common.utils.TigerUtils;
import pss.constants.TigerConstants;

public class GenerateTitleBlock_mxJPO {
    private static final Logger log = LoggerFactory.getLogger(GenerateTitleBlock_mxJPO.class);

    private static final float TITLE_BLOCK_WIDTH_DEFAULT = 170F;

    private static final float TITLE_BLOCK_RIGHI_MARGIN_DEFAULT = 10F;

    private static final float TITLE_BLOCK_BOTTOM_MARGIN_DEFAULT = 10F;

    private static final float TITLE_BLOCK_BORDER_THICK = 0.35F;

    private static final float TITLE_BLOCK_BORDER_THIN = 0.13F;

    private static final float TITLE_BLOCK_BORDER_ZERO = 0.0F;

    private static final int TITLE_BLOCK_UPDATE_BG_DELAY = 30; // in seconds

    private static final String TITLE_BLOCK_DATA_DIRECTION_ABOVE = "<ABOVE>";

    private static final String TITLE_BLOCK_DATA_DIRECTION_BELOW = "<BELOW>";

    private static final String TITLE_BLOCK_PLOTTING_CUSTOM = "<CUSTOM>";

    private static final String TITLE_BLOCK_CELL_IMAGE_TAG = "<IMAGE>";

    private static final String STRING_PATTERN = "PATTERN";

    private static final String STRING_HISTORY = "HISTORY";

    private static final String STRING_LINKEDPARTBLOCK = "LINKEDPARTBLOCK";

    private static final String STRING_ULSBLOCK = "ULSBLOCK";

    private static final String STRING_PROGRAM = "PROGRAM";

    private static final String STRING_PDF_SHEET = "Sheet_";

    private float TB_WIDTH = 0F;

    private float TB_RIGHT_MARGIN = 0F;

    private float TB_BOTTOM_MARGIN = 0f;

    private float TB_YSTART_NEW_PAGE = 0f;

    private float PDF_PAGE_WIDTH = 0f;

    private String TB_PATTERN = DomainConstants.EMPTY_STRING;

    private String TB_ROW_SEQUENCE = DomainConstants.EMPTY_STRING;

    private String BASE_URL = DomainConstants.EMPTY_STRING;

    private String CONTEXT_WORKSPACE = DomainConstants.EMPTY_STRING;

    // START :: TIGTK-18188 :: ALM-6340
    private String DELAY_TB_GENERATION = DomainConstants.EMPTY_STRING;

    // END :: TIGTK-18188 :: ALM-6340
    public static final String XML_FILE_NAME = "Input.xml";

    public static final String SUITE_KEY_IEF_DESIGN = "emxIEFDesignCenterStringResource";

    private List<BaseTable> INNER_TABLES = new ArrayList<BaseTable>();

    private Map<String, Object> DO_ARCHIVE_DETAILS = new HashMap<String, Object>();

    Cell<PDPage> SHEET_NO = null;

    HashMap<String, List<String[]>> _hmData = new HashMap<String, List<String[]>>();

    private PDFont FONT = PDType1Font.HELVETICA;

    private File FONT_FILE = null;

    private Context MCAD_CONTEXT = null;

    private ScratchFile scratchFile = null;

    private RandomAccessBufferedFileInputStream raFile = null;

    /**
     * constructor method
     * @param context
     *            context for the request
     * @param args
     *            arguments from the request
     * @throws Exception
     *             any unhandeled/run time exceptions
     */
    public GenerateTitleBlock_mxJPO(Context context, String[] args) throws Exception {
        init(context, args);
    }

    /**
     * Initialize the default values
     * @param context
     *            context for the request
     * @param args
     *            arguments from the request
     * @throws Exception
     *             any unhandeled/run time exceptions
     */
    private void init(Context context, String[] args) throws Exception {
        try {
            if (args.length <= 1) {
                return;
            }
            if (!TigerEnums.TitleBlockPattern.isValidPattern(args[1])) {
                return;
            }

            TigerAppConfigProperties.init(context);
            TB_PATTERN = args[1].replaceAll(TigerConstants.STRING_SINGLE_SPACE, DomainConstants.EMPTY_STRING).toUpperCase();
            try {
                TB_WIDTH = Float.parseFloat(TigerAppConfigProperties.getPropertyValue(context, TB_PATTERN + ".BLOCK.WIDTH"));
            } catch (NumberFormatException | NullPointerException e) {
                log.warn(EnoviaResourceBundle.getProperty(context, SUITE_KEY_IEF_DESIGN, context.getLocale(), "emxIEFDesignCenter.TitleBlock.TBWidthNotDefined"));
                TB_WIDTH = TITLE_BLOCK_WIDTH_DEFAULT;
            }
            try {
                TB_RIGHT_MARGIN = Float.parseFloat(TigerAppConfigProperties.getPropertyValue(context, TB_PATTERN + ".BLOCK.MARGIN.RIGHT"));
                TB_BOTTOM_MARGIN = Float.parseFloat(TigerAppConfigProperties.getPropertyValue(context, TB_PATTERN + ".BLOCK.MARGIN.BOTTOM"));
            } catch (NumberFormatException | NullPointerException e) {
                log.warn(EnoviaResourceBundle.getProperty(context, SUITE_KEY_IEF_DESIGN, context.getLocale(), "emxIEFDesignCenter.TitleBlock.TBMarginNotDefined"));
                TB_RIGHT_MARGIN = TITLE_BLOCK_RIGHI_MARGIN_DEFAULT;
                TB_BOTTOM_MARGIN = TITLE_BLOCK_BOTTOM_MARGIN_DEFAULT;
            }
            TB_ROW_SEQUENCE = TigerAppConfigProperties.getPropertyValue(context, TB_PATTERN + ".BLOCK.SEQUENCE");
            BASE_URL = EnoviaResourceBundle.getProperty(context, SUITE_KEY_IEF_DESIGN, context.getLocale(), "emxIEFDesignCenter.Common.TitleBlockURL");
            CONTEXT_WORKSPACE = context.createWorkspace();
            // START :: TIGTK-18188 :: ALM-6340
            try {
                DELAY_TB_GENERATION = EnoviaResourceBundle.getProperty(context, "emxIEFDesignCenter.TitleBlock.Update.BGJob.DelaySeconds");
            } catch (Exception e) {
                DELAY_TB_GENERATION = DomainConstants.EMPTY_STRING;
            }
            // END :: TIGTK-18188 :: ALM-6340
        } catch (MatrixException me) {
            log.error(me.getLocalizedMessage());
            throw new MatrixException(me);
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            throw new Exception(e);
        }
    }

    /**
     * Main entry method for TB generation
     * @param context
     *            context for the request
     * @param args
     *            arguments from the request
     * @return String of error message if any else empty string
     * @throws Exception
     *             any unhandeled/run time exceptions
     */
    public String generateTitleBlock(Context context, String[] args) throws Exception {
        log.info("::::::: ENTER :: generateTitleBlock :::::::");
        String strDOId = DomainConstants.EMPTY_STRING;
        String strObjId = DomainConstants.EMPTY_STRING;
        try {
            if (!TigerEnums.TitleBlockPattern.isValidPattern(args[1])) {
                log.debug(String.format(EnoviaResourceBundle.getProperty(context, SUITE_KEY_IEF_DESIGN, context.getLocale(), "emxIEFDesignCenter.TitleBlock.TBPatternNotSupported"), TB_PATTERN));
                return String.format(EnoviaResourceBundle.getProperty(context, SUITE_KEY_IEF_DESIGN, context.getLocale(), "emxIEFDesignCenter.TitleBlock.TBPatternNotSupported"), TB_PATTERN);
            }

            if (!ContextUtil.isTransactionActive(context)) {
                ContextUtil.startTransaction(context, true);
            }

            init(context, args);
            // Reset for Mass TB generate
            INNER_TABLES = new ArrayList<BaseTable>();
            strObjId = args[0];
            DomainObject doDrawing = DomainObject.newInstance(context, strObjId);
            strDOId = doDrawing.getInfo(context, TigerConstants.SELECT_FROM_DERIVED_OUTPUT_TO_ID);
            PropertyUtil.setGlobalRPEValue(context, TigerUtils.getRPEForBUS(TigerConstants.RPE_KEY_SKIP_TB_GENERATION_WEB, strDOId), TigerConstants.STRING_TRUE);
            CONTEXT_WORKSPACE = TigerUtils.getWorkspaceForBus(context, CONTEXT_WORKSPACE, strObjId);
            StringList slFiles = getOriginalPDFs(context, args);
            if (slFiles.isEmpty()) {
                String strReturnMessage = EnoviaResourceBundle.getProperty(context, SUITE_KEY_IEF_DESIGN, context.getLocale(), "emxIEFDesignCenter.TitleBlock.PDFNotFound");
                log.info(strReturnMessage);
                return strReturnMessage;
            }
            pss.cad2d3d.PSS_TitleBlockDataRetrieval_mxJPO tbData = new pss.cad2d3d.PSS_TitleBlockDataRetrieval_mxJPO(context, args);
            _hmData = tbData.generateTitleBlockData(context, doDrawing, args, CONTEXT_WORKSPACE, XML_FILE_NAME);
            // strDOId = (String) DO_ARCHIVE_DETAILS.get("DERIVED_OUTPUT_OID");
            String strPDFArchiveOID = (String) DO_ARCHIVE_DETAILS.get("PDF_ARCHIVE_OID");
            BusinessObject boPDFArchive = new BusinessObject(strPDFArchiveOID);
            BusinessObject boDerivedOutput = new BusinessObject(strDOId);

            // XML checkin
            boPDFArchive.checkinFile(context, true, false, DomainConstants.EMPTY_STRING, FileFormat.XML(), XML_FILE_NAME, CONTEXT_WORKSPACE);
            boDerivedOutput.checkinFile(context, true, false, DomainConstants.EMPTY_STRING, FileFormat.XML(), XML_FILE_NAME, CONTEXT_WORKSPACE);

            File fPDFasTemplate = new File(CONTEXT_WORKSPACE + java.io.File.separator + (String) slFiles.get(0));
            scratchFile = new ScratchFile(MemoryUsageSetting.setupMainMemoryOnly());
            InputStream ins = new FileInputStream(fPDFasTemplate);
            raFile = new RandomAccessBufferedFileInputStream(ins);
            PDDocument doc = load(fPDFasTemplate);
            PDPage page = doc.getPage(0);
            TB_YSTART_NEW_PAGE = page.getMediaBox().getHeight();
            PDF_PAGE_WIDTH = page.getMediaBox().getWidth();
            BaseTable baseTable = null;
            BaseTable backupBaseTable = null;
            String strActiveIteration = doDrawing.getInfo(context,
                    new StringBuilder("from[").append(TigerConstants.RELATIONSHIP_DERIVEDOUTPUT).append("].to.").append(DomainConstants.SELECT_REVISION).toString());
            // Font
            FONT_FILE = TigerUtils.getFontFile(Font.MONOSPAC821_BT);
            FONT = (FONT_FILE != null) ? PDType0Font.load(doc, FONT_FILE) : FONT;
            switch (TigerEnums.TitleBlockPattern.getPattern(args[1])) {
            case FASFAE:
            case BASISDEFINITION:
            case RENAULTNISSAN:
                baseTable = buildTitleBlock(context,
                        new BaseTable(0f, TB_YSTART_NEW_PAGE, 0f, TigerUtils.mm2pt(TB_WIDTH), PDF_PAGE_WIDTH - TigerUtils.mm2pt(TB_WIDTH) - TigerUtils.mm2pt(TB_RIGHT_MARGIN), doc, page, true, true),
                        doc, page);
                // draw() will obsolete the table once the table is drawn in PDF, so keeping the reference for iteration incase of multiple PDF sheets
                baseTable.setYStart(baseTable.getHeaderAndDataHeight() + TigerUtils.mm2pt(TB_BOTTOM_MARGIN));
                backupBaseTable = baseTable;
                break;
            case FCM:
                break;
            default:
                log.debug(String.format(EnoviaResourceBundle.getProperty(context, SUITE_KEY_IEF_DESIGN, context.getLocale(), "emxIEFDesignCenter.TitleBlock.TBPatternNotSupported"), TB_PATTERN));
                return String.format(EnoviaResourceBundle.getProperty(context, SUITE_KEY_IEF_DESIGN, context.getLocale(), "emxIEFDesignCenter.TitleBlock.TBPatternNotSupported"), TB_PATTERN);
            }

            String strFileName = DomainConstants.EMPTY_STRING;
            StringBuilder sbNewFileName = new StringBuilder();
            String strSheetNo = DomainConstants.EMPTY_STRING;
            int iNoOfPDFDocuments = slFiles.size();
            TigerUtils.deleteValidPDFs(context, strDOId);
            raFile.close();
            for (int i = 0; i < iNoOfPDFDocuments; i++) {
                strFileName = (String) slFiles.get(i);
                log.debug(">>> Processing file : " + strFileName);
                sbNewFileName.append(strFileName);
                scratchFile = new ScratchFile(MemoryUsageSetting.setupMainMemoryOnly());
                File fPDFToUpdateTitleBlock = new File(CONTEXT_WORKSPACE + java.io.File.separator + strFileName);
                ins = new FileInputStream(fPDFToUpdateTitleBlock);
                raFile = new RandomAccessBufferedFileInputStream(ins);
                PDDocument documnet = load(fPDFToUpdateTitleBlock);
                FONT = (FONT_FILE != null) ? PDType0Font.load(documnet, FONT_FILE) : FONT;
                int iNoOfPages = documnet.getNumberOfPages();
                if (iNoOfPages > 1) {
                    // multiSheet PDF
                    for (int j = 0; j < iNoOfPages; j++) {
                        PDPage documentPage = documnet.getPage(j);
                        strSheetNo = new StringBuilder(String.valueOf(j + 1)).append(TigerConstants.SEPERATOR_FORWARD_SLASH).append(String.valueOf(iNoOfPages)).toString();
                        baseTable = backupBaseTable;
                        // draw contents into PDF page
                        switch (TigerEnums.TitleBlockPattern.getPattern(args[1])) {
                        case FASFAE:
                        case BASISDEFINITION:
                        case RENAULTNISSAN:
                            SHEET_NO.setText(strSheetNo);
                            drawTitleBlockOnPDF(context, baseTable, documnet, documentPage);
                            drawAskedByFooter(context, documnet, documentPage);
                            break;
                        case FCM:
                            buildTitleBlockForFCM(context, documnet, documentPage);
                            break;
                        }
                    }
                } else {
                    // Single sheet PDFs
                    PDPage documentPage = documnet.getPage(0);
                    if (strFileName.indexOf(STRING_PDF_SHEET) != -1) {
                        strSheetNo = strFileName.substring(strFileName.indexOf(STRING_PDF_SHEET) + STRING_PDF_SHEET.length());
                        strSheetNo = strSheetNo.substring(0, strSheetNo.indexOf(TigerConstants.SEPERATOR_DOT));
                        strSheetNo = new StringBuilder(strSheetNo).append(TigerConstants.SEPERATOR_FORWARD_SLASH).append(String.valueOf(iNoOfPDFDocuments)).toString();
                    } else {
                        strSheetNo = new StringBuilder(String.valueOf(i + 1)).append(TigerConstants.SEPERATOR_FORWARD_SLASH).append(String.valueOf(iNoOfPDFDocuments)).toString();
                    }
                    baseTable = backupBaseTable;
                    // draw contents into PDF page
                    switch (TigerEnums.TitleBlockPattern.getPattern(args[1])) {
                    case FASFAE:
                    case BASISDEFINITION:
                    case RENAULTNISSAN:
                        SHEET_NO.setText(strSheetNo);
                        drawTitleBlockOnPDF(context, baseTable, documnet, documentPage);
                        drawAskedByFooter(context, documnet, documentPage);
                        break;
                    case FCM:
                        buildTitleBlockForFCM(context, documnet, documentPage);
                        break;
                    }
                }

                // rename to actualName + . + Iteration + .pdf ==> KLN-585858_sheet_1.-A.0.pdf
                sbNewFileName.insert(strFileName.indexOf(FileExtension.PDF()), TigerConstants.SEPERATOR_DOT + strActiveIteration);
                log.debug(">>> New File Name :: " + sbNewFileName.toString());
                // Close Stream and save pdf
                File outFile = new File(CONTEXT_WORKSPACE + java.io.File.separator + sbNewFileName.toString());
                FileOutputStream fos = new FileOutputStream(outFile);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                documnet.save(bos);
                ins.close();
                raFile.close();
                bos.flush();
                bos.close();
                fos.flush();
                fos.close();
                // FcsSupport.fcsCheckin(strDOId, context, true, true, FileFormat.PDF(), strStore, sbNewFileName.toString(), CONTEXT_WORKSPACE);
                boDerivedOutput.checkinFile(context, true, true, DomainConstants.EMPTY_STRING, FileFormat.PDF(), sbNewFileName.toString(), CONTEXT_WORKSPACE);
                sbNewFileName.delete(0, sbNewFileName.length());
            }
            doc.close();
            IOUtils.closeQuietly(scratchFile);
            IOUtils.closeQuietly(raFile);
            try {
                TigerUtils.pushContextToSuperUser(context);
                doDrawing.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_TITLEBLOCKCOMPLETED, TigerConstants.STRING_TRUE);
            } finally {
                try {
                    ContextUtil.popContext(context);
                } catch (FrameworkException fe) {
                    log.error(fe.getLocalizedMessage(), fe);
                    throw new FrameworkException(fe);
                }
            }
            // START :: new code from PLMCC
            // generate CAD and LST file
            pss.cad2d3d.TitleBlockCADFileGenerator_mxJPO titleBlockCADGen = new pss.cad2d3d.TitleBlockCADFileGenerator_mxJPO();
            titleBlockCADGen.createTitleBlockAndCheckinCAD(context, doDrawing, doDrawing.getName(), strDOId, args, CONTEXT_WORKSPACE);
            // END :: new code from PLMCC
            PropertyUtil.setGlobalRPEValue(context, TigerUtils.getRPEForBUS(TigerConstants.RPE_KEY_SKIP_TB_GENERATION_WEB, strDOId), TigerConstants.STRING_FALSE);
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            if (ContextUtil.isTransactionActive(context)) {
                ContextUtil.abortTransaction(context);
            }
            return e.getLocalizedMessage();
        } finally {
            TigerUtils.resetGlobalRPEs(context, strDOId);
            PropertyUtil.setGlobalRPEValue(context, TigerUtils.getRPEForBUS(TigerConstants.RPE_KEY_SKIP_ON_BG_JOB, strObjId), TigerConstants.STRING_FALSE);
            if (args.length >= 9 && UIUtil.isNotNullAndNotEmpty(args[8])) {
                PropertyUtil.setGlobalRPEValue(TigerUtils.getMCADSessionContext(context, args[8]), TigerUtils.getRPEForBUS(TigerConstants.RPE_KEY_SKIP_ON_BG_JOB, strDOId),
                        TigerConstants.STRING_FALSE);
            }

            if (ContextUtil.isTransactionActive(context)) {
                ContextUtil.commitTransaction(context);
            }
            // cleanup the workspace
            FileUtils.deleteDirectory(new File(CONTEXT_WORKSPACE));
        }
        log.info("::::::: EXIT :: generateTitleBlock :::::::");
        return DomainConstants.EMPTY_STRING;
    }

    /**
     * Draws constructed Title Block on the PDF
     * @param context
     *            context for the request
     * @param mainTitleBlockTable
     *            BaseTable object which holds the layout details to draw Title Block
     * @param doc
     *            document on whihc Titleblock layout needs to draw
     * @param page
     *            Pdf page of the document
     * @throws Exception
     *             any unhandeled/run time exceptions
     */
    private void drawTitleBlockOnPDF(Context context, BaseTable mainTitleBlockTable, PDDocument doc, PDPage page) throws Exception {
        try {
            TigerUtils.setFont(mainTitleBlockTable, FONT);
            mainTitleBlockTable.setDocument(doc);
            mainTitleBlockTable.setPage(page);
            mainTitleBlockTable.setYStart(mainTitleBlockTable.getHeaderAndDataHeight() + TigerUtils.mm2pt(TB_BOTTOM_MARGIN));
            mainTitleBlockTable.resetPageContentStream();
            mainTitleBlockTable.draw();
            float fYStart = 0.0F;
            for (BaseTable innerTable : INNER_TABLES) {
                TigerUtils.setFont(innerTable, FONT);
                innerTable.setDocument(doc);
                innerTable.setPage(page);
                fYStart = innerTable.getYStart();
                innerTable.setYStart(mainTitleBlockTable.getHeaderAndDataHeight() + TigerUtils.mm2pt(TB_BOTTOM_MARGIN) - innerTable.getYStart());
                innerTable.resetPageContentStream();
                innerTable.draw();
                innerTable.setYStart(fYStart);
            }
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            throw new Exception(e);
        }
    }

    /**
     * Draw AskedBy block on the PDF
     * @param context
     *            context for the request
     * @param doc
     *            document on whihc Titleblock layout needs to draw
     * @param page
     *            Pdf page of the document
     * @throws Exception
     */
    private void drawAskedByFooter(Context context, PDDocument doc, PDPage page) throws Exception {
        try {
            // Bottom Baner :: DRAWING ASKED ON 2018/06/25 AT 08:20 BY WebFull_3DEXP_09
            PDPageContentStream content = new PDPageContentStream(doc, page, AppendMode.APPEND, false);
            SimpleDateFormat date = new SimpleDateFormat("yyyy/MM/dd 'AT' HH:mm");
            String strBanner = String.format(EnoviaResourceBundle.getProperty(context, SUITE_KEY_IEF_DESIGN, context.getLocale(), "emxIEFDesignCenter.TitleBlock.FooterAskedByBanner"),
                    date.format(new Date()), TigerUtils.getLoggedInUserName(context));
            content.beginText();
            content.newLineAtOffset(PDF_PAGE_WIDTH - TigerUtils.mm2pt(TB_WIDTH) - TigerUtils.mm2pt(TB_RIGHT_MARGIN), TigerUtils.mm2pt(TB_BOTTOM_MARGIN) / 5);
            // START :: RFC-153
            String strFontSize = TigerAppConfigProperties.getPropertyValue(context, new StringBuilder(TB_PATTERN).append(".FOOTER.ASKEDBY.FONT").toString());
            content.setFont(FONT, UIUtil.isNotNullAndNotEmpty(strFontSize) ? TigerUtils.mm2pt(Float.parseFloat(strFontSize)) : TigerUtils.mm2pt(3.0F));
            // END :: RFC-153
            content.showText(strBanner);
            content.endText();
            content.close();
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            throw new Exception(e);
        }
    }

    /**
     * Method to build & draw FCM Titleblock
     * @param context
     *            context for the request
     * @param doc
     *            document on whihc Titleblock layout needs to draw
     * @param page
     *            Pdf page of the document
     * @throws Exception
     *             any unhandeled/run time exceptions
     */
    private void buildTitleBlockForFCM(Context context, PDDocument document, PDPage page) throws Exception {
        try {
            PDPageContentStream tableContentStream = new PDPageContentStream(document, page, AppendMode.APPEND, false);
            List<String[]> fcmData = _hmData.get("FCM.DRAWINGBANNER");
            String strContent = DomainConstants.EMPTY_STRING;
            if (fcmData != null && !fcmData.isEmpty()) {
                strContent = fcmData.get(0)[0];
            }
            tableContentStream.beginText();
            tableContentStream.setFont(FONT, TigerUtils.mm2pt(5.0F));
            tableContentStream.setTextMatrix(Matrix.getRotateInstance(Math.PI / 2, TigerUtils.mm2pt(5.0F), TigerUtils.mm2pt(10.0F)));
            tableContentStream.showText(strContent);
            tableContentStream.endText();
            tableContentStream.close();
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            throw new Exception(e);
        }
    }

    /**
     * Method constructs the Title block layout & populates the data by reading & iterating admin configuration files
     * @param context
     *            context for the request
     * @param table
     *            BaseTable object to hold constructed TitleBlock layouts
     * @param doc
     *            document on whihc Titleblock layout needs to draw
     * @param page
     *            Pdf page of the document
     * @return table with updated Layout
     * @throws Exception
     */
    private BaseTable buildTitleBlock(Context context, BaseTable table, PDDocument document, PDPage page) throws Exception {
        try {
            StringList slTBRows = FrameworkUtil.split(TB_ROW_SEQUENCE, TigerConstants.SEPERATOR_COMMA);
            List<String[]> dataList = new ArrayList<String[]>();
            int iRowPosition = 0;
            for (Object tbRowBlock : slTBRows) {
                log.debug(">>> Procession Rows for :" + tbRowBlock.toString());
                StringList slCellSeq = FrameworkUtil.split(TigerAppConfigProperties.getPropertyValue(context,
                        new StringBuilder(TB_PATTERN).append(TigerConstants.SEPERATOR_DOT).append(tbRowBlock.toString()).append(".CELL.SEQUENCE").toString()), TigerConstants.SEPERATOR_COMMA);
                dataList = _hmData.containsKey(new StringBuilder(TB_PATTERN).append(TigerConstants.SEPERATOR_DOT).append(tbRowBlock.toString()).toString())
                        ? _hmData.get(new StringBuilder(TB_PATTERN).append(TigerConstants.SEPERATOR_DOT).append(tbRowBlock.toString()).toString()) : dataList;
                if (dataList.isEmpty()) {
                    /*
                     * if ("BASISDEFINITION.HISTORYBLOCK".equals(TigerUtils.concateStrings(TB_PATTERN, TigerConstants.SEPERATOR_DOT, tbRowBlock.toString())) ||
                     * "FASFAE.BASICDEFINITION".equals(TigerUtils.concateStrings(TB_PATTERN, TigerConstants.SEPERATOR_DOT, tbRowBlock.toString()))){ continue; } else
                     */ if ("RENOTB".equals(tbRowBlock)) {
                        buildRenoBlock(context, table, document, page);
                        /* continue; */
                    }
                    continue;
                }
                iRowPosition += 1;
                if (slCellSeq.size() == 1 && TITLE_BLOCK_PLOTTING_CUSTOM.equals(slCellSeq.get(0))) {
                    // Need some special attention to build blocks/cells due to inner cells
                    table = buildCustomBlocks(context, table, dataList.get(0), tbRowBlock.toString(), document, page);
                } else {
                    // Good to use generic way of building
                    String strDataDirection = TigerAppConfigProperties.getPropertyValue(context,
                            new StringBuilder(TB_PATTERN).append(TigerConstants.SEPERATOR_DOT).append(tbRowBlock.toString()).append(".DATA.DIRECTION").toString());
                    strDataDirection = UIUtil.isNullOrEmpty(strDataDirection) ? TITLE_BLOCK_DATA_DIRECTION_BELOW : strDataDirection;

                    // dataList = getTestData(TB_PATTERN+"."+tbRowBlock.toString());
                    if (TITLE_BLOCK_DATA_DIRECTION_ABOVE.equals(strDataDirection) && iRowPosition == 1) {
                        table = buildDataRows(context, table, dataList, slCellSeq, tbRowBlock.toString());
                        table = buildHeaderRow(context, table, slCellSeq, tbRowBlock.toString(), true);
                    } else if (TITLE_BLOCK_DATA_DIRECTION_BELOW.equals(strDataDirection) && iRowPosition == 1) {
                        table = buildHeaderRow(context, table, slCellSeq, tbRowBlock.toString(), true);
                        table = buildDataRows(context, table, dataList, slCellSeq, tbRowBlock.toString());
                    } else if (TITLE_BLOCK_DATA_DIRECTION_ABOVE.equals(strDataDirection)) {
                        table = buildDataRows(context, table, dataList, slCellSeq, tbRowBlock.toString());
                        table = buildHeaderRow(context, table, slCellSeq, tbRowBlock.toString(), false);
                    } else {
                        table = buildHeaderRow(context, table, slCellSeq, tbRowBlock.toString(), false);
                        table = buildDataRows(context, table, dataList, slCellSeq, tbRowBlock.toString());
                    }
                }
                dataList.clear();
            }
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            throw new Exception(e);
        }
        return table;
    }

    /**
     * Methode to construct TB layout for Header row
     * @param context
     *            context for the request
     * @param table
     *            BaseTable object to hold constructed TitleBlock layouts
     * @param headerCells
     *            StringList of header lables
     * @param block
     *            Block of titleblock (ULS, LinkedPart, History, CAD, BD)
     * @param useThickTopBorder
     *            set Top border of Header row as thick border
     * @return table with updated layout
     * @throws Exception
     *             any unhandeled/run time exceptions
     */
    public BaseTable buildHeaderRow(Context context, BaseTable table, StringList headerCells, String block, boolean useThickTopBorder) throws Exception {
        try {
            float fHeight = Float.parseFloat(
                    TigerAppConfigProperties.getPropertyValue(context, new StringBuilder(TB_PATTERN).append(TigerConstants.SEPERATOR_DOT).append(block).append(".HEADER.HIGHT").toString()));
            Row<PDPage> row = table.createRow(TigerUtils.mm2pt(fHeight));
            Cell<PDPage> headerCell = null;
            ImageCell<PDPage> imageCell = null;
            String strLabel = DomainConstants.EMPTY_STRING;
            String strFont = DomainConstants.EMPTY_STRING;
            boolean bImageCell = false;
            for (Object cell : headerCells) {
                bImageCell = false;
                strLabel = TigerAppConfigProperties.getPropertyValue(context,
                        new StringBuilder(TB_PATTERN).append(TigerConstants.SEPERATOR_DOT).append(block).append(".HEADER.").append(cell.toString()).append(".LABEL").toString());
                StringList slAlign = FrameworkUtil.split(
                        TigerAppConfigProperties.getPropertyValue(context,
                                new StringBuilder(TB_PATTERN).append(TigerConstants.SEPERATOR_DOT).append(block).append(".HEADER.").append(cell.toString()).append(".ALIGN").toString()),
                        TigerConstants.SEPERATOR_COMMA);
                // Check whether the Header Label is image
                if (strLabel.startsWith(TITLE_BLOCK_CELL_IMAGE_TAG)) {
                    imageCell = row
                            .createImageCell(
                                    TigerUtils
                                            .mm2pt(Float.parseFloat(TigerAppConfigProperties.getPropertyValue(context,
                                                    new StringBuilder(TB_PATTERN).append(TigerConstants.SEPERATOR_DOT).append(block).append(".HEADER.").append(cell.toString()).append(".WIDTH")
                                                            .toString())))
                                            * 100 / TigerUtils.mm2pt(TB_WIDTH),
                                    new Image(ImageIO.read(new URL(BASE_URL + strLabel.substring(TITLE_BLOCK_CELL_IMAGE_TAG.length())))));
                    bImageCell = true;
                    imageCell.setFont(FONT);
                } else {
                    headerCell = row
                            .createCell(
                                    TigerUtils
                                            .mm2pt(Float.parseFloat(TigerAppConfigProperties.getPropertyValue(context,
                                                    new StringBuilder(TB_PATTERN).append(TigerConstants.SEPERATOR_DOT).append(block).append(".HEADER.").append(cell.toString()).append(".WIDTH")
                                                            .toString())))
                                            * 100 / TigerUtils.mm2pt(TB_WIDTH),
                                    strLabel, HorizontalAlignment.get((String) slAlign.get(0)), VerticalAlignment.get((String) slAlign.get(1)));
                    strFont = TigerAppConfigProperties.getPropertyValue(context,
                            new StringBuilder(TB_PATTERN).append(TigerConstants.SEPERATOR_DOT).append(block).append(".HEADER.").append(cell.toString()).append(".FONT").toString());
                    if (!strFont.startsWith(TITLE_BLOCK_CELL_IMAGE_TAG) && !strFont.startsWith(TigerConstants.TITLE_BLOCK_CELL_CROSS_TAG)) {
                        headerCell.setFontSize(TigerUtils.mm2pt(Float.parseFloat(strFont)));
                    }
                    headerCell.setFont(FONT);
                }
                // Border for cell
                StringList slBorders = FrameworkUtil.split(
                        TigerAppConfigProperties.getPropertyValue(context,
                                new StringBuilder(TB_PATTERN).append(TigerConstants.SEPERATOR_DOT).append(block).append(".HEADER.").append(cell.toString()).append(".BORDER").toString()),
                        TigerConstants.SEPERATOR_COMMA);
                if (useThickTopBorder) {
                    /* setBorderStyleForCell(bImageCell ? imageCell : headerCell, (String) slBorders.get(1), (String) slBorders.get(1), (String) slBorders.get(2), (String) slBorders.get(3)); */
                    setBorderStyleForCell(bImageCell ? imageCell : headerCell, String.valueOf(TITLE_BLOCK_BORDER_THICK), (String) slBorders.get(1), (String) slBorders.get(2),
                            (String) slBorders.get(3));
                } else {
                    setBorderStyleForCell(bImageCell ? imageCell : headerCell, (String) slBorders.get(0), (String) slBorders.get(1), (String) slBorders.get(2), (String) slBorders.get(3));
                }
                if (bImageCell) {
                    imageCell.scaleToFit();
                }
            }
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            throw new Exception(e);
        }
        return table;
    }

    /**
     * Method to construct TB layouts for Data rows
     * @param context
     *            context for the request
     * @param table
     *            BaseTable object to hold constructed TitleBlock layouts
     * @param dataList
     *            ArrayList of data to be populated into the TB
     * @param headerCells
     *            StringList of header lables
     * @param block
     *            Block of titleblock (ULS, LinkedPart, History, CAD, BD)
     * @return table with updated layout
     * @throws Exception
     *             any unhandeled/run time exceptions
     */
    public BaseTable buildDataRows(Context context, BaseTable table, List<String[]> dataList, StringList headerCells, String block) throws Exception {
        try {
            Row<PDPage> row = null;
            Cell<PDPage> dataCell = null;
            ImageCell<PDPage> imageCell = null;
            String strCellValue = DomainConstants.EMPTY_STRING;
            String strHeader = DomainConstants.EMPTY_STRING;
            String strFont = DomainConstants.EMPTY_STRING;
            boolean bImageCell = false;
            for (int i = 0; i < dataList.size(); i++) {
                String[] data = dataList.get(i);
                row = table.createRow(TigerUtils.mm2pt(5f));
                for (int j = 0; j < data.length; j++) {
                    strCellValue = data[j];
                    strHeader = (String) headerCells.get(j);
                    bImageCell = false;
                    StringList slAlign = FrameworkUtil.split(
                            TigerAppConfigProperties.getPropertyValue(context,
                                    new StringBuilder(TB_PATTERN).append(TigerConstants.SEPERATOR_DOT).append(block).append(".DATA.").append(strHeader).append(".ALIGN").toString()),
                            TigerConstants.SEPERATOR_COMMA);
                    if (strCellValue.startsWith(TITLE_BLOCK_CELL_IMAGE_TAG)) {
                        imageCell = row
                                .createImageCell(
                                        TigerUtils
                                                .mm2pt(Float.parseFloat(TigerAppConfigProperties.getPropertyValue(context,
                                                        new StringBuilder(TB_PATTERN).append(TigerConstants.SEPERATOR_DOT).append(block).append(".HEADER.").append(strHeader).append(".WIDTH")
                                                                .toString())))
                                                * 100 / TigerUtils.mm2pt(TB_WIDTH),
                                        new Image(ImageIO.read(new URL(BASE_URL + strCellValue.substring(TITLE_BLOCK_CELL_IMAGE_TAG.length())))));
                        bImageCell = true;
                        imageCell.setFont(FONT);
                    } else if (strCellValue.startsWith(TigerConstants.TITLE_BLOCK_CELL_CROSS_TAG)) {
                        dataCell = row.createCell(TigerUtils
                                .mm2pt(Float.parseFloat(TigerAppConfigProperties.getPropertyValue(context,
                                        new StringBuilder(TB_PATTERN).append(TigerConstants.SEPERATOR_DOT).append(block).append(".HEADER.").append(strHeader).append(".WIDTH").toString())))
                                * 100 / TigerUtils.mm2pt(TB_WIDTH), DomainConstants.EMPTY_STRING);
                        dataCell.drawCrossLine(0.5F);
                    } else {
                        dataCell = row
                                .createCell(
                                        TigerUtils
                                                .mm2pt(Float.parseFloat(TigerAppConfigProperties.getPropertyValue(context,
                                                        new StringBuilder(TB_PATTERN).append(TigerConstants.SEPERATOR_DOT).append(block).append(".HEADER.").append(strHeader).append(".WIDTH")
                                                                .toString())))
                                                * 100 / TigerUtils.mm2pt(TB_WIDTH),
                                        strCellValue, HorizontalAlignment.get((String) slAlign.get(0)), VerticalAlignment.get((String) slAlign.get(1)));
                        strFont = TigerAppConfigProperties.getPropertyValue(context,
                                new StringBuilder(TB_PATTERN).append(TigerConstants.SEPERATOR_DOT).append(block).append(".DATA.").append(strHeader).append(".FONT").toString());
                        if (!strFont.startsWith(TITLE_BLOCK_CELL_IMAGE_TAG) && !strFont.startsWith(TigerConstants.TITLE_BLOCK_CELL_CROSS_TAG)) {
                            dataCell.setFontSize(TigerUtils.mm2pt(Float.parseFloat(strFont)));
                        }
                    }
                    dataCell.setFont(FONT);
                    // Border for cell
                    StringList slBorders = FrameworkUtil.split(
                            TigerAppConfigProperties.getPropertyValue(context,
                                    new StringBuilder(TB_PATTERN).append(TigerConstants.SEPERATOR_DOT).append(block).append(".DATA.").append(strHeader).append(".BORDER").toString()),
                            TigerConstants.SEPERATOR_COMMA);
                    if (dataList.size() == i + 1) {
                        setBorderStyleForCell(bImageCell ? imageCell : dataCell, (String) slBorders.get(0), String.valueOf(TITLE_BLOCK_BORDER_THICK), (String) slBorders.get(2),
                                (String) slBorders.get(3));
                    } else {
                        setBorderStyleForCell(bImageCell ? imageCell : dataCell, (String) slBorders.get(0), (String) slBorders.get(1), (String) slBorders.get(2), (String) slBorders.get(3));
                    }
                    if (bImageCell) {
                        imageCell.scaleToFit();
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            throw new Exception(e);
        }
        return table;
    }

    /**
     * Entry method for custom Blocks of Title Block
     * @param context
     *            context for the request
     * @param table
     *            BaseTable object to hold constructed TitleBlock layouts
     * @param dataList
     *            StringArray of data to be populated into the TB
     * @param rowBlock
     *            Block of titleblock (ULS, LinkedPart, History, CAD, BD)
     * @param document
     *            document on whihc Titleblock layout needs to draw
     * @param page
     *            Pdf page of the document
     * @return table with updated layout
     * @throws Exception
     *             any unhandeled/run time exceptions
     */
    private BaseTable buildCustomBlocks(Context context, BaseTable table, String dataList[], String rowBlock, PDDocument document, PDPage page) throws Exception {
        try {
            if ("CADBLOCK".equals(rowBlock)) {
                table = buildCADBlock(context, table, dataList, document, page);
            } else if ("ULSBLOCK".equals(rowBlock)) {
                table = buildULSBlock(context, table, dataList, document, page);
            } else if ("RENOTB".equals(rowBlock)) {
                buildRenoBlock(context, table, document, page);
            }
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            throw new Exception(e);
        }
        return table;
    }

    /**
     * Method to construct TB layout for CAD Block changes :: Font size changes on 2018/11/24 as per RFC-153
     * @param context
     *            context for the request
     * @param table
     *            BaseTable object to hold constructed TitleBlock layouts
     * @param dataList
     *            StringArray of data to be populated into the TB
     * @param document
     *            document on whihc Titleblock layout needs to draw
     * @param page
     *            Pdf page of the document
     * @return table with updated layout
     * @throws Exception
     *             any unhandeled/run time exceptions
     */
    private BaseTable buildCADBlock(Context context, BaseTable table, String dataList[], PDDocument document, PDPage page) throws Exception {
        try {
            // check to avoid null or indexOutOfBound exceptions
            if (dataList == null || dataList.length == 0) {
                dataList = new String[] { DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING,
                        "First_Angle.jpg", DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING,
                        DomainConstants.EMPTY_STRING };
            }
            Row<PDPage> row = null;
            Cell<PDPage> cell = null;
            // CAD Description
            row = table.createRow(TigerUtils.mm2pt(15f));
            cell = row.createCell(TigerUtils.mm2pt(TB_WIDTH) * 100 / TigerUtils.mm2pt(TB_WIDTH), (UIUtil.isNullOrEmpty(dataList[0])) ? DomainConstants.EMPTY_STRING : dataList[0],
                    HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE);
            cell.setFontSize(TigerUtils.mm2pt(4.2f));
            cell.setFont(FONT);
            setBorderStyleForCell(cell, TITLE_BLOCK_BORDER_THICK, TITLE_BLOCK_BORDER_THICK, TITLE_BLOCK_BORDER_THICK, TITLE_BLOCK_BORDER_THICK);

            // position for CADInfo as a Inner table(table in a table)
            float fCADInfoYStart = table.getHeaderAndDataHeight();
            float fCADInfoTableWidth = row.getWidth();

            row = table.createRow(TigerUtils.mm2pt(13F));
            // Placeholder for leftCADTable
            cell = row.createCell(TigerUtils.mm2pt(75F) * 100 / TigerUtils.mm2pt(TB_WIDTH), DomainConstants.EMPTY_STRING);
            cell.setFont(FONT);
            setBorderStyleForCell(cell, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_ZERO);
            BaseTable leftCADTable = new BaseTable(0F, TB_YSTART_NEW_PAGE, 0F, cell.getWidth(), PDF_PAGE_WIDTH - table.getWidth() - TigerUtils.mm2pt(TB_RIGHT_MARGIN), document, page, true, true);
            leftCADTable.setYStart(fCADInfoYStart);
            // View Convention
            ImageCell<PDPage> imageCell = row.createImageCell(TigerUtils.mm2pt(17f) * 100 / TigerUtils.mm2pt(TB_WIDTH), new Image(ImageIO.read(new URL(BASE_URL + dataList[5]))));
            setBorderStyleForCell(imageCell, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_THICK, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_THICK);
            imageCell.scaleToFit();
            // Placeholder for CASInfo Table
            cell = row.createCell(TigerUtils.mm2pt(78F) * 100 / TigerUtils.mm2pt(TB_WIDTH), DomainConstants.EMPTY_STRING);
            cell.setFont(FONT);
            setBorderStyleForCell(cell, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_ZERO);
            fCADInfoTableWidth = TigerUtils.mm2pt(78F); // table.getWidth() - getRowCellsWidht(row);
            BaseTable cadInfoTable = new BaseTable(0F, TB_YSTART_NEW_PAGE, 0F, fCADInfoTableWidth, PDF_PAGE_WIDTH - TigerUtils.mm2pt(TB_RIGHT_MARGIN) - cell.getWidth(), document, page, true, true);
            cadInfoTable.setYStart(fCADInfoYStart);

            row = leftCADTable.createRow(TigerUtils.mm2pt(4F));
            // Dimension Standard - header
            cell = row.createCell(TigerUtils.mm2pt(30F) * 100 / leftCADTable.getWidth(), TigerAppConfigProperties.getPropertyValue(context, TB_PATTERN + ".CADBLOCK.HEADER.DIMENSIONINGSTANDARD.LABEL"),
                    HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE);
            cell.setFontSize(TigerUtils.mm2pt(Float.parseFloat(TigerAppConfigProperties.getPropertyValue(context, TB_PATTERN + ".CADBLOCK.HEADER.DIMENSIONINGSTANDARD.FONT"))));
            cell.setFont(FONT);
            setBorderStyleForCell(cell, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_THICK, TITLE_BLOCK_BORDER_THICK);
            // Inner Radious - header
            cell = row.createCell(TigerUtils.mm2pt(15F) * 100 / leftCADTable.getWidth(), TigerAppConfigProperties.getPropertyValue(context, TB_PATTERN + ".CADBLOCK.HEADER.INNERRADIUSNOTDIMENS.LABEL"),
                    HorizontalAlignment.CENTER, VerticalAlignment.BOTTOM);
            cell.setFontSize(TigerUtils.mm2pt(Float.parseFloat(TigerAppConfigProperties.getPropertyValue(context, TB_PATTERN + ".CADBLOCK.HEADER.INNERRADIUSNOTDIMENS.FONT"))));
            cell.setFont(FONT);
            setBorderStyleForCell(cell, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_THIN);
            // Angular Tolerence - header
            cell = row.createCell(TigerUtils.mm2pt(15f) * 100 / leftCADTable.getWidth(), TigerAppConfigProperties.getPropertyValue(context, TB_PATTERN + ".CADBLOCK.HEADER.ANGULARTOLERANCE.LABEL"),
                    HorizontalAlignment.CENTER, VerticalAlignment.BOTTOM);
            cell.setFontSize(TigerUtils.mm2pt(Float.parseFloat(TigerAppConfigProperties.getPropertyValue(context, TB_PATTERN + ".CADBLOCK.HEADER.ANGULARTOLERANCE.FONT"))));
            cell.setFont(FONT);
            setBorderStyleForCell(cell, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_THIN);
            // Linear Tolerence - header
            cell = row.createCell(TigerUtils.mm2pt(15f) * 100 / leftCADTable.getWidth(), TigerAppConfigProperties.getPropertyValue(context, TB_PATTERN + ".CADBLOCK.HEADER.LINEARTOLERANCE.LABEL"),
                    HorizontalAlignment.CENTER, VerticalAlignment.BOTTOM);
            cell.setFontSize(TigerUtils.mm2pt(Float.parseFloat(TigerAppConfigProperties.getPropertyValue(context, TB_PATTERN + ".CADBLOCK.HEADER.LINEARTOLERANCE.FONT"))));
            cell.setFont(FONT);
            setBorderStyleForCell(cell, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_THICK);

            row = leftCADTable.createRow(TigerUtils.mm2pt(9F));
            // Dimension Standard
            cell = row.createCell(TigerUtils.mm2pt(30F) * 100 / leftCADTable.getWidth(), (UIUtil.isNullOrEmpty(dataList[1])) ? DomainConstants.EMPTY_STRING : dataList[1], HorizontalAlignment.CENTER,
                    VerticalAlignment.MIDDLE);
            cell.setFontSize(TigerUtils.mm2pt(Float.parseFloat(TigerAppConfigProperties.getPropertyValue(context, TB_PATTERN + ".CADBLOCK.DATA.DIMENSIONINGSTANDARD.FONT"))));
            cell.setFont(FONT);
            setBorderStyleForCell(cell, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_THICK, TITLE_BLOCK_BORDER_THICK, TITLE_BLOCK_BORDER_THICK);
            // Inner Radious
            cell = row.createCell(TigerUtils.mm2pt(15f) * 100 / leftCADTable.getWidth(), (UIUtil.isNullOrEmpty(dataList[2])) ? DomainConstants.EMPTY_STRING : dataList[2], HorizontalAlignment.CENTER,
                    VerticalAlignment.MIDDLE);
            cell.setFontSize(TigerUtils.mm2pt(Float.parseFloat(TigerAppConfigProperties.getPropertyValue(context, TB_PATTERN + ".CADBLOCK.DATA.INNERRADIUSNOTDIMENS.FONT"))));
            cell.setFont(FONT);
            setBorderStyleForCell(cell, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_THICK, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_THIN);
            // Angular Tolerence
            cell = row.createCell(TigerUtils.mm2pt(15f) * 100 / leftCADTable.getWidth(), (UIUtil.isNullOrEmpty(dataList[3])) ? DomainConstants.EMPTY_STRING : dataList[3], HorizontalAlignment.CENTER,
                    VerticalAlignment.MIDDLE);
            cell.setFontSize(TigerUtils.mm2pt(Float.parseFloat(TigerAppConfigProperties.getPropertyValue(context, TB_PATTERN + ".CADBLOCK.DATA.ANGULARTOLERANCE.FONT"))));
            cell.setFont(FONT);
            setBorderStyleForCell(cell, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_THICK, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_THIN);
            // Linear Tolerence
            cell = row.createCell(TigerUtils.mm2pt(15F) * 100 / leftCADTable.getWidth(), (UIUtil.isNullOrEmpty(dataList[4])) ? DomainConstants.EMPTY_STRING : dataList[4], HorizontalAlignment.CENTER,
                    VerticalAlignment.MIDDLE);
            cell.setFontSize(TigerUtils.mm2pt(Float.parseFloat(TigerAppConfigProperties.getPropertyValue(context, TB_PATTERN + ".CADBLOCK.DATA.LINEARTOLERANCE.FONT"))));
            cell.setFont(FONT);
            setBorderStyleForCell(cell, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_THICK, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_THICK);
            INNER_TABLES.add(leftCADTable);

            // CAD Info block
            // File Name / CAD Description
            row = cadInfoTable.createRow(TigerUtils.mm2pt(6.5F));
            cell = row.createCell(TigerUtils.mm2pt(78F) * 100 / fCADInfoTableWidth, (UIUtil.isNullOrEmpty(dataList[6])) ? DomainConstants.EMPTY_STRING : dataList[6], HorizontalAlignment.LEFT,
                    VerticalAlignment.MIDDLE);
            cell.setFontSize(TigerUtils.mm2pt(3.2F));
            cell.setFont(FONT);
            setBorderStyleForCell(cell, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_THICK, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_THICK);

            row = cadInfoTable.createRow(TigerUtils.mm2pt(2.5F));
            // Scale - header
            cell = row.createCell(TigerUtils.mm2pt(16F) * 100 / fCADInfoTableWidth, TigerAppConfigProperties.getPropertyValue(context, TB_PATTERN + ".CADBLOCK.HEADER.SCALE.LABEL"),
                    HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE);
            cell.setFontSize(TigerUtils.mm2pt(Float.parseFloat(TigerAppConfigProperties.getPropertyValue(context, TB_PATTERN + ".CADBLOCK.HEADER.SCALE.FONT"))));
            cell.setFont(FONT);
            setBorderStyleForCell(cell, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_THIN);
            // Format - header
            cell = row.createCell(TigerUtils.mm2pt(15f) * 100 / fCADInfoTableWidth, TigerAppConfigProperties.getPropertyValue(context, TB_PATTERN + ".CADBLOCK.HEADER.FORMAT.LABEL"),
                    HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE);
            cell.setFontSize(TigerUtils.mm2pt(Float.parseFloat(TigerAppConfigProperties.getPropertyValue(context, TB_PATTERN + ".CADBLOCK.HEADER.FORMAT.FONT"))));
            cell.setFont(FONT);
            setBorderStyleForCell(cell, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_THIN);
            // Sheet - header
            cell = row.createCell(TigerUtils.mm2pt(15f) * 100 / fCADInfoTableWidth, TigerAppConfigProperties.getPropertyValue(context, TB_PATTERN + ".CADBLOCK.HEADER.SHEET.LABEL"),
                    HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE);
            cell.setFontSize(TigerUtils.mm2pt(Float.parseFloat(TigerAppConfigProperties.getPropertyValue(context, TB_PATTERN + ".CADBLOCK.HEADER.SHEET.FONT"))));
            cell.setFont(FONT);
            setBorderStyleForCell(cell, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_THICK);
            // Status - header
            cell = row.createCell(TigerUtils.mm2pt(16f) * 100 / fCADInfoTableWidth, TigerAppConfigProperties.getPropertyValue(context, TB_PATTERN + ".CADBLOCK.HEADER.STATUS.LABEL"),
                    HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE);
            cell.setFontSize(TigerUtils.mm2pt(Float.parseFloat(TigerAppConfigProperties.getPropertyValue(context, TB_PATTERN + ".CADBLOCK.HEADER.STATUS.FONT"))));
            cell.setFont(FONT);
            setBorderStyleForCell(cell, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_THIN);
            // From date - header
            cell = row.createCell(TigerUtils.mm2pt(16f) * 100 / fCADInfoTableWidth, TigerAppConfigProperties.getPropertyValue(context, TB_PATTERN + ".CADBLOCK.HEADER.PROMDATE.LABEL"),
                    HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE);
            cell.setFontSize(TigerUtils.mm2pt(Float.parseFloat(TigerAppConfigProperties.getPropertyValue(context, TB_PATTERN + ".CADBLOCK.HEADER.PROMDATE.FONT"))));
            cell.setFont(FONT);
            setBorderStyleForCell(cell, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_THICK);

            row = cadInfoTable.createRow(TigerUtils.mm2pt(4F));
            // Scale - data
            cell = row.createCell(TigerUtils.mm2pt(16F) * 100 / fCADInfoTableWidth, (UIUtil.isNullOrEmpty(dataList[7])) ? DomainConstants.EMPTY_STRING : dataList[7], HorizontalAlignment.CENTER,
                    VerticalAlignment.MIDDLE);
            cell.setFontSize(TigerUtils.mm2pt(Float.parseFloat(TigerAppConfigProperties.getPropertyValue(context, TB_PATTERN + ".CADBLOCK.DATA.SCALE.FONT"))));
            cell.setFont(FONT);
            setBorderStyleForCell(cell, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_THICK, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_THIN);

            // Format
            cell = row.createCell(TigerUtils.mm2pt(15F) * 100 / fCADInfoTableWidth, (UIUtil.isNullOrEmpty(dataList[8])) ? DomainConstants.EMPTY_STRING : dataList[8], HorizontalAlignment.CENTER,
                    VerticalAlignment.MIDDLE);
            cell.setFontSize(TigerUtils.mm2pt(Float.parseFloat(TigerAppConfigProperties.getPropertyValue(context, TB_PATTERN + ".CADBLOCK.DATA.FORMAT.FONT"))));
            cell.setFont(FONT);
            setBorderStyleForCell(cell, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_THICK, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_THIN);

            // Sheet
            SHEET_NO = row.createCell(TigerUtils.mm2pt(15F) * 100 / fCADInfoTableWidth, (UIUtil.isNullOrEmpty(dataList[9])) ? DomainConstants.EMPTY_STRING : dataList[9], HorizontalAlignment.CENTER,
                    VerticalAlignment.MIDDLE);
            SHEET_NO.setFontSize(TigerUtils.mm2pt(Float.parseFloat(TigerAppConfigProperties.getPropertyValue(context, TB_PATTERN + ".CADBLOCK.DATA.SHEET.FONT"))));
            cell.setFont(FONT);
            setBorderStyleForCell(SHEET_NO, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_THICK, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_THICK);

            // Status
            cell = row.createCell(TigerUtils.mm2pt(16F) * 100 / fCADInfoTableWidth, (UIUtil.isNullOrEmpty(dataList[10])) ? DomainConstants.EMPTY_STRING : dataList[10], HorizontalAlignment.CENTER,
                    VerticalAlignment.MIDDLE);
            cell.setFontSize(TigerUtils.mm2pt(Float.parseFloat(TigerAppConfigProperties.getPropertyValue(context, TB_PATTERN + ".CADBLOCK.DATA.STATUS.FONT"))));
            cell.setFont(FONT);
            setBorderStyleForCell(cell, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_THICK, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_THIN);

            // From date
            cell = row.createCell(TigerUtils.mm2pt(16F) * 100 / fCADInfoTableWidth, (UIUtil.isNullOrEmpty(dataList[11])) ? DomainConstants.EMPTY_STRING : dataList[11], HorizontalAlignment.CENTER,
                    VerticalAlignment.MIDDLE);
            cell.setFontSize(TigerUtils.mm2pt(Float.parseFloat(TigerAppConfigProperties.getPropertyValue(context, TB_PATTERN + ".CADBLOCK.DATA.PROMDATE.FONT"))));
            cell.setFont(FONT);
            setBorderStyleForCell(cell, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_THICK, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_THICK);
            INNER_TABLES.add(cadInfoTable);
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            throw new Exception(e);
        }
        return table;
    }

    /**
     * Method to construct TB layout for ULS Block
     * @param context
     *            context for the request
     * @param table
     *            BaseTable object to hold constructed TitleBlock layouts
     * @param dataList
     *            StringArray of data to be populated into the TB
     * @param document
     *            document on whihc Titleblock layout needs to draw
     * @param page
     *            Pdf page of the document
     * @return table with updated layout
     * @throws Exception
     *             any unhandeled/run time exceptions
     */
    private BaseTable buildULSBlock(Context context, BaseTable table, String dataList[], PDDocument document, PDPage page) throws Exception {
        try {
            Row<PDPage> row = null;
            Cell<PDPage> cell = null;
            row = table.createRow(TigerUtils.mm2pt(13F));
            // PP & Vehicle
            cell = row.createCell(TigerUtils.mm2pt(7f) * 100 / TigerUtils.mm2pt(TB_WIDTH), TigerAppConfigProperties.getPropertyValue(context, TB_PATTERN + ".ULSBLOCK.HEADER.PP.LABEL"),
                    HorizontalAlignment.RIGHT, VerticalAlignment.MIDDLE);
            cell.setRightPadding(0.0F);
            cell.setFontSize(TigerUtils.mm2pt(1.1F));
            cell.setFont(FONT);
            setBorderStyleForCell(cell, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_THICK, TITLE_BLOCK_BORDER_THICK, TITLE_BLOCK_BORDER_ZERO);
            cell = row.createCell(TigerUtils.mm2pt(53f) * 100 / TigerUtils.mm2pt(TB_WIDTH), (UIUtil.isNullOrEmpty(dataList[0])) ? DomainConstants.EMPTY_STRING : dataList[0], HorizontalAlignment.LEFT,
                    VerticalAlignment.MIDDLE);
            cell.setLeftPadding(0.0F);
            cell.setFontSize(TigerUtils.mm2pt(1.1F));
            cell.setFont(FONT);
            setBorderStyleForCell(cell, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_THICK, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_THIN);

            // Faurecia Logo
            ImageCell<PDPage> imageCell = row.createImageCell(TigerUtils.mm2pt(32f) * 100 / TigerUtils.mm2pt(TB_WIDTH), new Image(ImageIO.read(new URL(BASE_URL + dataList[1]))));
            setBorderStyleForCell(imageCell, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_THICK, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_ZERO);
            imageCell.scaleToFit();

            // dummy to keep blanck space between Logo & Address
            cell = row.createCell(TigerUtils.mm2pt(8F) * 100 / TigerUtils.mm2pt(TB_WIDTH), DomainConstants.EMPTY_STRING);
            setBorderStyleForCell(cell, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_THICK, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_ZERO);
            cell.setFont(FONT);

            // Address
            cell = row.createCell(TigerUtils.mm2pt(70F) * 100 / TigerUtils.mm2pt(TB_WIDTH), (UIUtil.isNullOrEmpty(dataList[2])) ? DomainConstants.EMPTY_STRING : dataList[2], HorizontalAlignment.LEFT,
                    VerticalAlignment.MIDDLE);
            setBorderStyleForCell(cell, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_THICK, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_THICK);
            cell.setFontSize(TigerUtils.mm2pt(1.8F));
            cell.setFont(FONT);

            // Copy rights
            row = table.createRow(TigerUtils.mm2pt(6F));
            cell = row.createCell(TigerUtils.mm2pt(114F) * 100 / TigerUtils.mm2pt(TB_WIDTH),
                    EnoviaResourceBundle.getProperty(context, SUITE_KEY_IEF_DESIGN, context.getLocale(), "emxIEFDesignCenter.TitleBlock.ProprietaryText"), HorizontalAlignment.CENTER,
                    VerticalAlignment.MIDDLE);
            cell.setFontSize(TigerUtils.mm2pt(1.7F));
            cell.setFont(FONT);
            setBorderStyleForCell(cell, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_THICK, TITLE_BLOCK_BORDER_THICK, TITLE_BLOCK_BORDER_THIN);

            cell = row.createCell(TigerUtils.mm2pt(56F) * 100 / TigerUtils.mm2pt(TB_WIDTH),
                    EnoviaResourceBundle.getProperty(context, SUITE_KEY_IEF_DESIGN, context.getLocale(), "emxIEFDesignCenter.TitleBlock.DrawingSource"), HorizontalAlignment.CENTER,
                    VerticalAlignment.MIDDLE);
            cell.setFontSize(TigerUtils.mm2pt(1.7F));
            cell.setFont(FONT);
            setBorderStyleForCell(cell, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_THICK, TITLE_BLOCK_BORDER_ZERO, TITLE_BLOCK_BORDER_THICK);
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getLocalizedMessage(), e);
            throw new Exception(e);
        }
        return table;
    }

    /**
     * Method to construct TB layout for Renault Nissan block
     * @param context
     *            context for the request
     * @param table
     *            BaseTable object to hold constructed TitleBlock layouts
     * @param document
     *            document on whihc Titleblock layout needs to draw
     * @param page
     *            Pdf page of the document
     * @return table with updated layout
     * @throws Exception
     *             any unhandeled/run time exceptions
     */
    private BaseTable buildRenoBlock(Context context, BaseTable table, PDDocument document, PDPage page) throws Exception {
        try {
            Row<PDPage> row = table.createRow(TigerUtils.mm2pt(100F));
            Cell<PDPage> cell = row.createCell(TigerUtils.mm2pt(TB_WIDTH) * 100 / TigerUtils.mm2pt(TB_WIDTH), DomainConstants.EMPTY_STRING);
            cell.setFont(FONT);
            setBorderStyleForCell(cell, TITLE_BLOCK_BORDER_THICK, TITLE_BLOCK_BORDER_THICK, TITLE_BLOCK_BORDER_THICK, TITLE_BLOCK_BORDER_THICK);
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getLocalizedMessage(), e);
            throw new Exception(e);
        }
        return table;
    }

    /**
     * method to checkout & return original/new pdf sheets
     * 
     * <pre>
     * 1. check for PDFArchive
     *  1.1. If exists, Checkout files & returns SL of file names
     *  1.2. If not exists,
     *      1.2.1. Create PDFArchive & connect with CATDrawing
     *      1.2.2. Checkout PDF files from DO(CATDrawing-->Derived Output)
     *      1.2.3. Checkin PDF files into PDFArchive
     * </pre>
     * 
     * @param context
     *            context for the request
     * @param args
     *            arguments from the request
     * @return StringList of PDF file names
     * @throws Exception
     *             any unhandeled/run time exceptions
     */
    private StringList getOriginalPDFs(Context context, String[] args) throws Exception {
        StringList slFileNames = new StringList();
        try {
            String strObjId = args[0];
            DomainObject doDrawingArchive = DomainObject.newInstance(context);
            BusinessObject boDrawingArchive = new BusinessObject();
            DomainRelationship drArchivedDO = null;
            boolean bPDFArchiveExists = false;
            boolean bCheckIn = false; // true is possible if, 1. new PDFArchive. 2. new iteration
            doDrawingArchive.setId(strObjId);
            StringList slSelectables = new StringList(2);
            slSelectables.add(TigerConstants.SELECT_FROM_DERIVED_OUTPUT_TO_ID);
            slSelectables.add(TigerConstants.SELECT_FROM_PSS_PDF_ARCHIVE_TO_ID);
            Map<String, String> mpDOs = doDrawingArchive.getInfo(context, slSelectables);
            String strDOId = mpDOs.get(TigerConstants.SELECT_FROM_DERIVED_OUTPUT_TO_ID); // must not be empty
            String strPDFArchiveOID = mpDOs.containsKey(TigerConstants.SELECT_FROM_PSS_PDF_ARCHIVE_TO_ID) ? mpDOs.get(TigerConstants.SELECT_FROM_PSS_PDF_ARCHIVE_TO_ID) : DomainConstants.EMPTY_STRING;

            BusinessObject boDeriveOutupt = new BusinessObject(strDOId);
            if (UIUtil.isNotNullAndNotEmpty(strPDFArchiveOID)) {
                // PDFArchive with Original PDFs are already available
                boDrawingArchive = new BusinessObject(strPDFArchiveOID);
                bPDFArchiveExists = true;
            } else {
                DomainObject doObject = DomainObject.newInstance(context);
                // first time TB PDF generation, no PDFArchive avaliable --> create PDFArchive & connect with drawing
                drArchivedDO = doObject.createAndConnect(context, TigerConstants.TYPE_PSS_PDFARCHIVE, doDrawingArchive.getName(), doDrawingArchive.getRevision(), TigerConstants.POLICY_PSS_PDFARCHIVE,
                        TigerConstants.VAULT_ESERVICEPRODUCTION, TigerConstants.RELATIONSHIP_PSS_ARCHIVEDO, doDrawingArchive, true);
                boDrawingArchive = boDeriveOutupt;
                strPDFArchiveOID = drArchivedDO.getTo().getObjectId(context);
                bCheckIn = true;
            }
            StringList slDOFiles = TigerUtils.getValidPDFSheetsName(context, boDeriveOutupt);
            StringList slPDFArchiveFiles = TigerUtils.getFileNames(context, new BusinessObject(strPDFArchiveOID), FileFormat.PDF());
            slDOFiles.sort();
            slPDFArchiveFiles.sort();
            DO_ARCHIVE_DETAILS.put("DERIVED_OUTPUT_OID", strDOId);
            DO_ARCHIVE_DETAILS.put("PDF_ARCHIVE_OID", strPDFArchiveOID);
            DO_ARCHIVE_DETAILS.put("DERIVED_OUTPUT_FILES", slDOFiles);
            DO_ARCHIVE_DETAILS.put("PDF_ARCHIVE_FILES", slPDFArchiveFiles);
            // Incase of new iteration, file names will be same for DO & PDFArchive
            if (bPDFArchiveExists) {
                // PDFArchive must contains all files that are in DO &
                // size() is required to identify add/delete diff list
                if (slPDFArchiveFiles.size() != slDOFiles.size() || slPDFArchiveFiles.containsAll(slDOFiles)) {
                    // If so, new iteration --> checkout files from DO
                    boDrawingArchive = boDeriveOutupt;
                    bCheckIn = true;
                }
            }
            // above logics decides from where(DO or PDFArchive) checkout for original files
            // FileList flFiles = boDrawingArchive.getFiles(context, "PDF");
            FileList flFiles = TigerUtils.getValidPDFSheets(context, boDrawingArchive);
            FileItr fileItr = new FileItr(flFiles);
            matrix.db.File file = null;
            while (fileItr.next()) {
                file = fileItr.obj();
                slFileNames.add(file.getName());
            }
            boDrawingArchive.checkoutFiles(context, false, FileFormat.PDF(), flFiles, CONTEXT_WORKSPACE);
            /**
             * below block will executed in 2 cases
             * 
             * <pre>
             *  1. If PDFArchive is created newly then, checkin original files from Derived Output
             *  2. If PDFArchive with original file already availabe & there is a change in Drawing (ex. new iteration)
             *       then, delete old file from PDFArchive & checkin new files from DO into PDFArchive
             * </pre>
             */
            if (bCheckIn) {
                if (bPDFArchiveExists) {
                    // delete all old original pdfs
                    TigerUtils.deleteFiles(context, strPDFArchiveOID, FileFormat.PDF(), slPDFArchiveFiles);
                }
                BusinessObject boDerivedOutput = new BusinessObject(strPDFArchiveOID);
                for (Object fileName : slFileNames) {
                    boDerivedOutput.checkinFile(context, true, true, DomainConstants.EMPTY_STRING, FileFormat.PDF(), fileName.toString(), CONTEXT_WORKSPACE);
                }
                slPDFArchiveFiles = TigerUtils.getFileNames(context, new BusinessObject(strPDFArchiveOID), FileFormat.PDF());
                slPDFArchiveFiles.sort();
                DO_ARCHIVE_DETAILS.put("PDF_ARCHIVE_FILES", slPDFArchiveFiles);
            }
        } catch (FrameworkException fe) {
            log.error(fe.getLocalizedMessage(), fe);
            throw new Exception(fe);
        } catch (MatrixException me) {
            log.error(me.getLocalizedMessage(), me);
            throw new Exception(me);
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            throw new Exception(e);
        }
        return slFileNames;
    }

    /**
     * set border for cell
     * @param cell
     *            cell of row
     * @param topborder
     *            top border size in string
     * @param bottomBorder
     *            bottom border size in string
     * @param leftBorder
     *            left border size in string
     * @param rightBorder
     *            right border size in string
     */
    private void setBorderStyleForCell(Cell<PDPage> cell, String topborder, String bottomBorder, String leftBorder, String rightBorder) {
        setBorderStyleForCell(cell, UIUtil.isNullOrEmpty(topborder) ? 0.0F : Float.valueOf(topborder), UIUtil.isNullOrEmpty(bottomBorder) ? 0.0F : Float.valueOf(bottomBorder),
                UIUtil.isNullOrEmpty(leftBorder) ? 0.0F : Float.valueOf(leftBorder), UIUtil.isNullOrEmpty(rightBorder) ? 0.0F : Float.valueOf(rightBorder));
    }

    /**
     * set border for cell
     * @param cell
     *            cell of row
     * @param topborder
     *            top border size
     * @param bottomBorder
     *            bottom border size
     * @param leftBorder
     *            left border size
     * @param rightBorder
     *            right border size
     */
    private void setBorderStyleForCell(Cell<PDPage> cell, float topborder, float bottomBorder, float leftBorder, float rightBorder) {
        if (TITLE_BLOCK_BORDER_ZERO == topborder) {
            cell.setTopBorderStyle(null);
        } else {
            cell.setTopBorderStyle(new LineStyle(Color.black, TigerUtils.mm2pt(topborder)));
        }
        if (TITLE_BLOCK_BORDER_ZERO == bottomBorder) {
            cell.setBottomBorderStyle(null);
        } else {
            cell.setBottomBorderStyle(new LineStyle(Color.black, TigerUtils.mm2pt(bottomBorder)));
        }
        if (TITLE_BLOCK_BORDER_ZERO == leftBorder) {
            cell.setLeftBorderStyle(null);
        } else {
            cell.setLeftBorderStyle(new LineStyle(Color.black, TigerUtils.mm2pt(leftBorder)));
        }
        if (TITLE_BLOCK_BORDER_ZERO == rightBorder) {
            cell.setRightBorderStyle(null);
        } else {
            cell.setRightBorderStyle(new LineStyle(Color.black, TigerUtils.mm2pt(rightBorder)));
        }
    }

    /**
     * reads XML file & prepares String[] of arg
     * @param context
     *            context for the request
     * @param cadDrawing
     *            domainObject of CAD Drawing
     * @return String[] of params to generate TB
     * @throws Exception
     *             any unhandeled/run time exceptions
     */
    public String[] getTitelBlockParamsFromXML(Context context, DomainObject cadDrawing, boolean checkoutFromDO) throws Exception {
        String[] args = new String[10];
        try {
            args[0] = cadDrawing.getObjectId(context);
            args[1] = DomainConstants.EMPTY_STRING; // Pattern
            args[2] = TigerConstants.STRING_NO.toLowerCase(); // History
            args[3] = "3"; // No. of revision
            args[4] = TigerConstants.STRING_NO.toLowerCase(); // Linked Part
            args[5] = TigerConstants.STRING_NO.toLowerCase(); // Show ULS
            String strCheckOutOID = checkoutFromDO ? cadDrawing.getInfo(context, TigerConstants.SELECT_FROM_DERIVED_OUTPUT_TO_ID)
                    : cadDrawing.getInfo(context, TigerConstants.SELECT_FROM_PSS_PDF_ARCHIVE_TO_ID);
            if (UIUtil.isNullOrEmpty(strCheckOutOID) && !checkoutFromDO) {
                strCheckOutOID = cadDrawing.getInfo(context, TigerConstants.SELECT_FROM_DERIVED_OUTPUT_TO_ID);
            }
            BusinessObject boCheckoutBUS = new BusinessObject(strCheckOutOID);
            String strWorkSpace = TigerUtils.getWorkspaceForBus(context, DomainConstants.EMPTY_STRING, strCheckOutOID);
            try {
                FileList flFiles = new FileList(1);
                matrix.db.File file = new matrix.db.File(XML_FILE_NAME, FileFormat.XML());
                flFiles.addElement(file);
                boCheckoutBUS.checkoutFiles(context, false, FileFormat.XML(), flFiles, strWorkSpace);
            } catch (MatrixException me) {
                log.error(me.getLocalizedMessage(), me);
            }
            File fXMLFile = new File(new StringBuilder(strWorkSpace).append(java.io.File.separator).append(XML_FILE_NAME).toString());
            if (fXMLFile.exists()) {
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();
                Document document = docBuilder.parse(fXMLFile);
                document.getDocumentElement().normalize();
                // history block start
                NodeList nlPattern = document.getElementsByTagName(STRING_PATTERN);
                if (nlPattern != null && nlPattern.getLength() != 0) {
                    args[1] = nlPattern.item(0).getTextContent();
                }

                // History Block
                NodeList nlHistory = document.getElementsByTagName(STRING_HISTORY);
                if (args[1] != null && TitleBlockPattern.BASISDEFINITION.toString().equals(args[1])) {
                    nlHistory = document.getElementsByTagName("BASISDEFINITIONHISTORY");
                }
                if (nlHistory != null && nlHistory.getLength() != 0) {
                    args[3] = String.valueOf(nlHistory.getLength());
                    args[2] = TigerConstants.STRING_YES.toLowerCase();
                }

                // Linked Part
                NodeList nlLinkedPart = document.getElementsByTagName(STRING_LINKEDPARTBLOCK);
                if (nlLinkedPart != null && nlLinkedPart.getLength() != 0) {
                    args[4] = TigerConstants.STRING_YES.toLowerCase();
                }

                // For FCM - always Linked Part is yes.
                if (TigerEnums.TitleBlockPattern.FCM.toString().equals(args[1])) {
                    args[4] = TigerConstants.STRING_YES.toLowerCase();
                }

                // ULS
                NodeList nlULS = document.getElementsByTagName(STRING_ULSBLOCK);
                if (nlULS != null && nlULS.getLength() != 0) {
                    NodeList nlProgram = document.getElementsByTagName(STRING_PROGRAM);
                    if (nlProgram != null && nlProgram.getLength() != 0) {
                        args[5] = TigerConstants.STRING_YES.toLowerCase();
                    }
                }
                TigerUtils.cleanUp(fXMLFile);
            }
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
        return args;
    }

    /**
     * Method to create background job
     * @param context
     *            context for the request
     * @param program
     *            JPO name to be invoked
     * @param method
     *            Method of JPO to be invoked
     * @param args
     *            String[] of arguments
     * @param title
     *            Title for the Job to be created
     * @throws Exception
     *             FrameworkException
     */
    private void createAndSubmitJob(Context context, String program, String method, String[] args, String title) throws Exception {
        log.debug("::::::: ENTER :: createAndSubmitJob :::::::");
        try {
            Job job = new Job();
            job.setTitle(title);
            job.setNotifyOwner("No");
            job.create(context);
            job.setOwner(context, TigerUtils.getLoggedInUserName(context));
            args[8] = job.getInfo(context, DomainConstants.SELECT_NAME);
            if (MCAD_CONTEXT != null) {
                TigerUtils.setMCADSessionContext(MCAD_CONTEXT, args[8]);
            }
            Context localContext = context.getFrameContext(args[8]);
            BackgroundProcess bgProcess = new BackgroundProcess();
            bgProcess.submitJob(localContext, program, method, args, job.getId(context));
        } catch (FrameworkException fe) {
            log.error(fe.getLocalizedMessage(), fe);
            throw new Exception(fe);
        }
        log.debug("::::::: EXIT :: createAndSubmitJob :::::::");
    }

    /**
     * Method will be triggred whenever a PDF file(s) get checkedin into Derived Output of Drawing object
     * @param context
     *            context for the request
     * @param args
     *            arguments from the request
     * @throws Exception
     *             FrameworkException/ runtime exception
     */
    public void updateTitleBlockOnCheckinBackgroudJobProcess(Context context, String[] args) throws Exception {
        log.debug("::::::: ENTER :: updateTitleBlockOnCheckinBackgroudJobProcess :::::::");
        try {
            String ObjectID = args[0];
            // skip check during TB generation & checkin from Web
            String strRPE = TigerUtils.getRPEForBUS(TigerConstants.RPE_KEY_SKIP_TB_GENERATION_WEB, ObjectID);
            String strCheckinDuring = PropertyUtil.getGlobalRPEValue(context, strRPE);
            if (UIUtil.isNotNullAndNotEmpty(strCheckinDuring) && TigerConstants.STRING_TRUE.equalsIgnoreCase(strCheckinDuring)) {
                log.debug(">>> SKIP ==> " + strRPE + " == " + strCheckinDuring);
                return;
            }

            // skip check to avoid job creation during checiIn operation from another job
            strRPE = TigerUtils.getRPEForBUS(TigerConstants.RPE_KEY_JOB_SKIP_TB_GENERATION_WEB, ObjectID);
            strCheckinDuring = PropertyUtil.getGlobalRPEValue(context, strRPE);
            if (UIUtil.isNotNullAndNotEmpty(strCheckinDuring) && TigerConstants.STRING_TRUE.equalsIgnoreCase(strCheckinDuring)) {
                log.debug(">>> SKIP ==> JOB_" + strRPE + " == " + strCheckinDuring);
                return;
            }

            // skip check during checkin from Native
            strRPE = TigerUtils.getRPEForBUS(TigerConstants.RPE_KEY_SKIP_TB_GENERATION_NATIVE, ObjectID);
            strCheckinDuring = PropertyUtil.getGlobalRPEValue(context, strRPE);
            if (UIUtil.isNotNullAndNotEmpty(strCheckinDuring) && TigerConstants.STRING_TRUE.equalsIgnoreCase(strCheckinDuring)) {
                log.debug(">>> RPE ==> " + strRPE + " == " + strCheckinDuring);
                return;
            }

            String strFormat = args[1];
            String valREP = PropertyUtil.getRPEValue(context, ContextUtil.MX_LOGGED_IN_USER_NAME + "-" + ObjectID, true);
            // START :: fix for impact from TIGTK-17327 :: ALM-6162 :: Rename PDF
            if ((UIUtil.isNotNullAndNotEmpty(valREP) && TigerConstants.STRING_TRUE.equalsIgnoreCase(valREP)) || !FileFormat.PDF().equalsIgnoreCase(strFormat)) {
                // END :: fix for impact from TIGTK-17327 :: ALM-6162 :: Rename PDF
                PropertyUtil.setRPEValue(context, ContextUtil.MX_LOGGED_IN_USER_NAME + "-" + ObjectID, TigerConstants.STRING_FALSE, true);
                log.debug("::::::: EXIT :: updateTitleBlockOnCheckinBackgroudJobProcess :::::::");
                return;
            }

            PropertyUtil.setGlobalRPEValue(context, strRPE, TigerConstants.STRING_TRUE);
            String strTitle = EnoviaResourceBundle.getProperty(context, SUITE_KEY_IEF_DESIGN, context.getLocale(), "emxIEFDesignCenter.TitleBlock.UpdateTBCheckInFromCAD");
            createAndSubmitJob(context, "pss.cad2d3d.GenerateTitleBlock", "updateTitleBlockOnCheckin", args, strTitle);
            log.debug(">>> Job created & Submited");
        } catch (FrameworkException fe) {
            log.error(fe.getLocalizedMessage(), fe);
            throw new FrameworkException(fe);
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
        log.debug("::::::: EXIT :: updateTitleBlockOnCheckinBackgroudJobProcess :::::::");
    }

    /**
     * Method will be called from BG JOB to update TB during checking from Native
     * @param context
     *            context for the request
     * @param args
     *            arguments packed for background execution
     * @throws Exception
     *             FrameworkException/ runtime exception
     */
    public void updateTitleBlockOnCheckin(Context context, String[] args) throws Exception {
        log.debug("::::::: ENTER :: updateTitleBlockOnCheckin :::::::");
        String ObjectID = DomainConstants.EMPTY_STRING;
        try {
            // sleep of one minute will solve the checkin related problems whihc will benifite the smooth TB generation
            if (UIUtil.isNotNullAndNotEmpty(DELAY_TB_GENERATION)) {
                TimeUnit.SECONDS.sleep(Integer.parseInt(DELAY_TB_GENERATION));
            } else {
                TimeUnit.SECONDS.sleep(TITLE_BLOCK_UPDATE_BG_DELAY); // 30seconds
            }
            ContextUtil.startTransaction(context, true);
            ObjectID = args[0];
            String strTypePattern = new StringBuilder(TigerConstants.TYPE_PSS_CATDRAWING).append(TigerConstants.SEPERATOR_COMMA).append(TigerConstants.TYPE_PSS_PROEDRAWING)
                    .append(TigerConstants.SEPERATOR_COMMA).append(TigerConstants.TYPE_PSS_SWDRAWING).append(TigerConstants.SEPERATOR_COMMA).append(TigerConstants.TYPE_PSS_UGDRAWING).toString();
            StringList slObjectSelects = new StringList(2);
            slObjectSelects.add(DomainConstants.SELECT_ID);
            slObjectSelects.add(TigerConstants.SELECT_ATTRIBUTE_PSS_TITLEBLOCKCOMPLETED);
            String strObjWhere = new StringBuilder(DomainConstants.SELECT_POLICY).append(TigerConstants.OPERATOR_EQ).append(TigerConstants.POLICY_PSS_CADOBJECT).toString();
            DomainObject doDerivedOutput = DomainObject.newInstance(context, ObjectID);
            MapList mlCADDrawings = doDerivedOutput.getRelatedObjects(context, TigerConstants.RELATIONSHIP_DERIVEDOUTPUT, strTypePattern, slObjectSelects, DomainConstants.EMPTY_STRINGLIST, true,
                    false, (short) 1, strObjWhere, DomainConstants.EMPTY_STRING, 0);
            if (mlCADDrawings != null && !mlCADDrawings.isEmpty()) {
                Map mpDrawing = (Map) mlCADDrawings.get(0);
                String strDrawingOID = (String) mpDrawing.get(DomainConstants.SELECT_ID);
                String strTBCompleted = (String) mpDrawing.get(TigerConstants.SELECT_ATTRIBUTE_PSS_TITLEBLOCKCOMPLETED);
                String[] arg = new String[10];
                if (TigerConstants.STRING_TRUE.equalsIgnoreCase(strTBCompleted)) {
                    arg = getTitelBlockParamsFromXML(context, DomainObject.newInstance(context, strDrawingOID), false);
                } else {
                    String strPreferenceViewOptions = PropertyUtil.getAdminProperty(context, DomainConstants.TYPE_PERSON, TigerUtils.getLoggedInUserName(context), "preference_Search_ViewOptions");
                    log.debug(">>> TigerUtils.getLoggedInUserName(context) :: " + TigerUtils.getLoggedInUserName(context));
                    log.debug(">>> strPreferenceViewOptions :: " + strPreferenceViewOptions);
                    if (UIUtil.isNullOrEmpty(strPreferenceViewOptions) || TigerConstants.STRING_NO.equalsIgnoreCase(strPreferenceViewOptions)) {
                        log.info("User Preference is not set. Skipping TB generation from Background.");
                        return;
                    }
                    String strFASDrawingHistory = TigerConstants.STRING_NO;
                    String strFASNoRevision = "3";
                    String strFASLinkedParts = TigerConstants.STRING_YES;
                    String strFASULS = TigerConstants.STRING_NO;
                    if (TigerEnums.TitleBlockPattern.FASFAE.toString().equals(strPreferenceViewOptions)) {
                        strFASDrawingHistory = PropertyUtil.getAdminProperty(context, DomainConstants.TYPE_PERSON, context.getUser(), "preference_Search_FASDrawinghistory");
                        strFASNoRevision = PropertyUtil.getAdminProperty(context, DomainConstants.TYPE_PERSON, context.getUser(), "preference_Search_FASNoRevision");
                        strFASLinkedParts = PropertyUtil.getAdminProperty(context, DomainConstants.TYPE_PERSON, context.getUser(), "preference_Search_FASLinkedParts");
                        strFASULS = PropertyUtil.getAdminProperty(context, DomainConstants.TYPE_PERSON, context.getUser(), "preference_Search_FASUpperLevelStructure");
                    }
                    arg[0] = strDrawingOID;
                    arg[1] = strPreferenceViewOptions;
                    arg[2] = UIUtil.isNullOrEmpty(strFASDrawingHistory) ? TigerConstants.STRING_NO : strFASDrawingHistory.toLowerCase();
                    arg[3] = UIUtil.isNullOrEmpty(strFASNoRevision) ? "3" : strFASNoRevision;
                    arg[4] = UIUtil.isNullOrEmpty(strFASLinkedParts) ? TigerConstants.STRING_NO : strFASLinkedParts.toLowerCase();
                    arg[5] = UIUtil.isNullOrEmpty(strFASULS) ? TigerConstants.STRING_NO : strFASULS.toLowerCase();
                }
                // START :: TIGTK-17750 :: ALM-837
                checkAndRemoveDetailSheet(context, ObjectID, strDrawingOID);
                // END :: TIGTK-17750 :: ALM-837
                arg[6] = TigerConstants.STRING_YES.toLowerCase(); // BG process
                arg[8] = args[8];
                generateTitleBlock(context, arg);
            }
        } catch (FrameworkException fe) {
            log.error(fe.getLocalizedMessage(), fe);
            ContextUtil.abortTransaction(context);
            throw new FrameworkException(fe);
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            ContextUtil.abortTransaction(context);
            throw new Exception(e.getLocalizedMessage(), e);
        } finally {
            try {
                if (ContextUtil.isTransactionActive(context)) {
                    ContextUtil.commitTransaction(context);
                }
            } catch (FrameworkException fe) {
                log.error(fe.getLocalizedMessage(), fe);
            }
            TigerUtils.resetGlobalRPEs(context, ObjectID);
        }
        log.debug("::::::: EXIT :: updateTitleBlockOnCheckin :::::::");
    }

    /**
     * This method will generate Title Block on promotion to Released state.
     * @param context
     *            context for the request
     * @param args
     *            arguments from the request
     * @throws Exception
     *             FrameworkException/RuntimeException
     */
    public int updateTitleBlockonRelease(Context context, String[] args) throws Exception {
        log.debug("::::::: ENTER :: updateTitleBlockonRelease :::::::");
        int iReturn = 0;
        String strObjOID = DomainConstants.EMPTY_STRING;
        try {
            strObjOID = args[0];
            DomainObject doObject = DomainObject.newInstance(context, strObjOID);
            if (doObject.isKindOf(context, TigerConstants.TYPE_CADDRAWING)) {
                strObjOID = doObject.getInfo(context, TigerConstants.SELECT_FROM_DERIVED_OUTPUT_TO_ID);
                if (UIUtil.isNullOrEmpty(strObjOID)) {
                    log.info("SKIP TB Update >>> No Derived Output found for '" + doObject.getName());
                    return 0;
                }
                String strRPE = TigerUtils.getRPEForBUS(TigerConstants.RPE_KEY_SKIP_TB_GENERATION_WEB, strObjOID);
                String strCheckinDuring = PropertyUtil.getGlobalRPEValue(context, strRPE);
                if (UIUtil.isNotNullAndNotEmpty(strCheckinDuring) && TigerConstants.STRING_TRUE.equalsIgnoreCase(strCheckinDuring)) {
                    log.debug(">>> SKIP ==> " + strRPE + " == " + strCheckinDuring);
                    return 0;
                }

                MCAD_CONTEXT = context;
                strRPE = TigerUtils.getRPEForBUS(TigerConstants.RPE_KEY_SKIP_ON_BG_JOB, strObjOID);
                strCheckinDuring = PropertyUtil.getGlobalRPEValue(context, strRPE);
                if (UIUtil.isNotNullAndNotEmpty(strCheckinDuring) && TigerConstants.STRING_TRUE.equalsIgnoreCase(strCheckinDuring)) {
                    log.debug(">>> SKIP ==> " + strRPE + " == " + strCheckinDuring);
                    return 0;
                } else {
                    PropertyUtil.setGlobalRPEValue(context, strRPE, TigerConstants.STRING_TRUE);
                }

                String LoggedUserName = PropertyUtil.getGlobalRPEValue(context, "PSS_CAD_PROMOTE_USERNAME");
                if (UIUtil.isNotNullAndNotEmpty(LoggedUserName)) {
                    args[10] = LoggedUserName;
                }
                String strTitle = EnoviaResourceBundle.getProperty(context, SUITE_KEY_IEF_DESIGN, context.getLocale(), "emxIEFDesignCenter.TitleBlock.UpdateTBOnRelease");
                createAndSubmitJob(context, "pss.cad2d3d.GenerateTitleBlock", "updateTitleBlockonPromoteBGJOB", args, strTitle);
            }
        } catch (FrameworkException fe) {
            log.error(fe.getLocalizedMessage(), fe);
            throw new FrameworkException(fe);
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
        log.debug("::::::: EXIT :: updateTitleBlockonRelease :::::::");
        return iReturn;
    }

    public int updateTitleBlockonPromoteBGJOB(Context context, String args[]) throws Exception {
        try {
            if (UIUtil.isNotNullAndNotEmpty(DELAY_TB_GENERATION)) {
                TimeUnit.SECONDS.sleep(Integer.parseInt(DELAY_TB_GENERATION));
            } else {
                TimeUnit.SECONDS.sleep(TITLE_BLOCK_UPDATE_BG_DELAY); // 30seconds
            }
            ContextUtil.startTransaction(context, true);
            return updateTitleBlockonPromote(context, args);
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            ContextUtil.abortTransaction(context);
            throw new Exception(e.getLocalizedMessage(), e);
        } finally {
            try {
                if (ContextUtil.isTransactionActive(context)) {
                    ContextUtil.commitTransaction(context);
                }
            } catch (FrameworkException fe) {
                log.error(fe.getLocalizedMessage(), fe);
            }
        }
    }

    /**
     * This method will generate Title Block on promotion.
     * @param context
     *            context for the request
     * @param args
     *            arguments from the request
     * @throws Exception
     *             FrameworkException/RuntimeException
     */
    public int updateTitleBlockonPromote(Context context, String[] args) throws Exception {
        log.debug("::::::: ENTER :: updateTitleBlockonPromote :::::::");
        String strDOId = DomainConstants.EMPTY_STRING;
        String strobjectId = DomainConstants.EMPTY_STRING;
        try {
            strobjectId = args[0];
            DomainObject domObject = DomainObject.newInstance(context, strobjectId);
            if (domObject.isKindOf(context, TigerConstants.TYPE_CADDRAWING)) {
                String[] arg = getTitelBlockParamsFromXML(context, domObject, false);
                arg[6] = TigerConstants.STRING_YES.toLowerCase();
                arg[8] = args[8];
                String strRPE = TigerUtils.getRPEForBUS(TigerConstants.RPE_KEY_SKIP_TB_GENERATION_WEB, domObject.getInfo(context, TigerConstants.SELECT_FROM_DERIVED_OUTPUT_TO_ID));
                String strCheckinDuring = PropertyUtil.getGlobalRPEValue(context, strRPE);
                log.debug(">>> updateTitleBlockonPromote :: RPE ==> " + strRPE + " == " + strCheckinDuring);
                if (UIUtil.isNotNullAndNotEmpty(strCheckinDuring) && TigerConstants.STRING_TRUE.equalsIgnoreCase(strCheckinDuring)) {
                    log.debug(">>> SKIP ==> " + strRPE + " == " + strCheckinDuring);
                    log.debug("::::::: EXIT :: updateTitleBlockonPromote :::::::");
                    return 0;
                }
                if (deleteOldFileAndCheckInOriginalFiles(context, domObject)) {
                    generateTitleBlock(context, arg);
                }
            }
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        } finally {
            if (UIUtil.isNotNullAndNotEmpty(strDOId)) {
                TigerUtils.resetGlobalRPEs(context, strDOId);
                PropertyUtil.setGlobalRPEValue(context, TigerUtils.getRPEForBUS(TigerConstants.RPE_KEY_SKIP_ON_BG_JOB, strobjectId), TigerConstants.STRING_FALSE);
                if (args.length >= 9 && UIUtil.isNotNullAndNotEmpty(args[8])) {
                    PropertyUtil.setGlobalRPEValue(TigerUtils.getMCADSessionContext(context, args[8]), TigerUtils.getRPEForBUS(TigerConstants.RPE_KEY_SKIP_ON_BG_JOB, strobjectId),
                            TigerConstants.STRING_FALSE);
                }
            }
        }
        log.debug("::::::: EXIT :: updateTitleBlockonPromote :::::::");
        return 0;
    }

    /**
     * delete(from DO) & checkin(from PDFArchive to DO) original files to solve the conflicts during the new iteration while demoting
     * @param context
     *            context context for the request
     * @param catDrawing
     *            DomainObject of CATDrawing
     * @return
     * 
     *         <pre>
     *         True - deleted & checkedIn originalfiles False - already Update TB is in progress by another Job, so skip current
     * 
     *         <pre>
     * @throws Exception
     */
    private boolean deleteOldFileAndCheckInOriginalFiles(Context context, DomainObject catDrawing) throws Exception {
        log.debug("::::::: ENTER :: deleteOldFileAndCheckInOriginalFiles :::::::");
        String strWorkspace = DomainConstants.EMPTY_STRING;
        String strDOId = DomainConstants.EMPTY_STRING;
        try {
            if (catDrawing.isKindOf(context, TigerConstants.TYPE_CADDRAWING)) {
                StringList slSelectables = new StringList(2);
                slSelectables.add(TigerConstants.SELECT_FROM_DERIVED_OUTPUT_TO_ID);
                slSelectables.add(TigerConstants.SELECT_FROM_PSS_PDF_ARCHIVE_TO_ID);
                Map<String, String> mpDOs = catDrawing.getInfo(context, slSelectables);
                strDOId = mpDOs.get(TigerConstants.SELECT_FROM_DERIVED_OUTPUT_TO_ID); // must not be empty
                String strPDFArchiveOID = mpDOs.containsKey(TigerConstants.SELECT_FROM_PSS_PDF_ARCHIVE_TO_ID) ? mpDOs.get(TigerConstants.SELECT_FROM_PSS_PDF_ARCHIVE_TO_ID)
                        : DomainConstants.EMPTY_STRING;
                if (UIUtil.isNotNullAndNotEmpty(strPDFArchiveOID) && UIUtil.isNotNullAndNotEmpty(strDOId)) {
                    BusinessObject boPDFArchive = new BusinessObject(strPDFArchiveOID);
                    FileList flFiles = TigerUtils.getValidPDFSheets(context, boPDFArchive);
                    if (flFiles.size() > 0) {
                        String strRPE = TigerUtils.getRPEForBUS(TigerConstants.RPE_KEY_SKIP_TB_GENERATION_WEB, strDOId);
                        String strCheckinDuring = PropertyUtil.getGlobalRPEValue(context, strRPE);
                        if (UIUtil.isNotNullAndNotEmpty(strCheckinDuring) && TigerConstants.STRING_TRUE.equals(strCheckinDuring)) {
                            log.debug(">>> SKIP ==> " + strRPE + " == " + strCheckinDuring);
                            log.debug("::::::: EXIT :: deleteOldFileAndCheckInOriginalFiles :::::::");
                            return false;
                        }
                        StringList slDOFiles = TigerUtils.getValidPDFSheetsName(context, new BusinessObject(strDOId));
                        try {
                            if (!slDOFiles.isEmpty()) {
                                TigerUtils.deleteFiles(context, strDOId, FileFormat.PDF(), slDOFiles);
                            } else {
                                return false;
                            }
                            FileItr fileItr = new FileItr(flFiles);
                            matrix.db.File file = null;
                            StringList slFileNames = new StringList(flFiles.size());
                            while (fileItr.next()) {
                                file = fileItr.obj();
                                slFileNames.add(file.getName());
                            }
                            strWorkspace = TigerUtils.getWorkspaceForBus(context, DomainConstants.EMPTY_STRING, strDOId + "-JOB");
                            boPDFArchive.checkoutFiles(context, false, FileFormat.PDF(), flFiles, strWorkspace);
                            PropertyUtil.setGlobalRPEValue(context, "JOB_" + strRPE, TigerConstants.STRING_TRUE);
                            BusinessObject boDerivedOutput = new BusinessObject(strDOId);
                            for (Object fileName : slFileNames) {
                                boDerivedOutput.checkinFile(context, true, true, DomainConstants.EMPTY_STRING, FileFormat.PDF(), fileName.toString(), strWorkspace);
                            }
                        } catch (FrameworkException fe) {
                            log.error(fe.getLocalizedMessage());
                            return false;
                        } catch (MatrixException me) {
                            log.error(me.getLocalizedMessage());
                            return false;
                        } finally {
                            PropertyUtil.setGlobalRPEValue(context, "JOB_" + strRPE, TigerConstants.STRING_FALSE);
                        }
                        if (UIUtil.isNotNullAndNotEmpty(strWorkspace))
                            TigerUtils.cleanUp(new File(strWorkspace));
                    }
                }
            }
        } catch (FrameworkException fe) {
            log.error(fe.getLocalizedMessage());
            return false;
        } catch (MatrixException me) {
            log.error(me.getLocalizedMessage());
            return false;
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            return false;
        }
        log.debug("::::::: EXIT :: deleteOldFileAndCheckInOriginalFiles :::::::");
        return true;
    }

    /**
     * This method will generate Title Block on promotion to Obsolete state.
     * @param context
     *            context context for the request
     * @param args
     *            arguments from the request
     * @throws Exception
     */
    public int updateTitleBlockonObsolete(Context context, String[] args) throws Exception {
        log.debug("::::::: ENTER :: updateTitleBlockonObsolete :::::::");
        int iReturn = 0;
        String strObjOID = DomainConstants.EMPTY_STRING;
        try {
            strObjOID = args[0];
            DomainObject doObject = DomainObject.newInstance(context, strObjOID);
            if (doObject.isKindOf(context, TigerConstants.TYPE_CADDRAWING)) {
                strObjOID = doObject.getInfo(context, TigerConstants.SELECT_FROM_DERIVED_OUTPUT_TO_ID);
                if (UIUtil.isNullOrEmpty(strObjOID)) {
                    log.info("SKIP TB Update >>> No Derived Output found for '" + doObject.getName());
                    return 0;
                }
                String strRPE = TigerUtils.getRPEForBUS(TigerConstants.RPE_KEY_SKIP_TB_GENERATION_WEB, strObjOID);
                String strCheckinDuring = PropertyUtil.getGlobalRPEValue(context, strRPE);
                if (UIUtil.isNotNullAndNotEmpty(strCheckinDuring) && TigerConstants.STRING_TRUE.equalsIgnoreCase(strCheckinDuring)) {
                    log.debug(">>> SKIP ==> " + strRPE + " == " + strCheckinDuring);
                    return 0;
                }

                MCAD_CONTEXT = context;
                strRPE = TigerUtils.getRPEForBUS(TigerConstants.RPE_KEY_SKIP_ON_BG_JOB, strObjOID);
                strCheckinDuring = PropertyUtil.getGlobalRPEValue(context, strRPE);
                if (UIUtil.isNotNullAndNotEmpty(strCheckinDuring) && TigerConstants.STRING_TRUE.equalsIgnoreCase(strCheckinDuring)) {
                    log.debug(">>> SKIP ==> " + strRPE + " == " + strCheckinDuring);
                    return 0;
                } else {
                    PropertyUtil.setGlobalRPEValue(context, strRPE, TigerConstants.STRING_TRUE);
                }

                String LoggedUserName = PropertyUtil.getGlobalRPEValue(context, "PSS_CAD_PROMOTE_USERNAME");
                if (UIUtil.isNotNullAndNotEmpty(LoggedUserName)) {
                    args[10] = LoggedUserName;
                }
                String strTitle = EnoviaResourceBundle.getProperty(context, SUITE_KEY_IEF_DESIGN, context.getLocale(), "emxIEFDesignCenter.TitleBlock.UpdateTBOnObsolete");
                createAndSubmitJob(context, "pss.cad2d3d.GenerateTitleBlock", "updateTitleBlockonPromoteBGJOB", args, strTitle);
            }
        } catch (FrameworkException fe) {
            log.error(fe.getLocalizedMessage(), fe);
            throw new FrameworkException(fe);
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
        log.debug("::::::: EXIT :: updateTitleBlockonObsolete :::::::");
        return iReturn;
    }

    /**
     * Method to update the TitleBlock in background during promote/Demote
     * @param context
     *            context context for the request
     * @param args
     *            arguments from the request
     * @return
     * @throws Exception
     */
    public int updateTitleBlockBackgroudJobProcess(Context context, String[] args) throws Exception {
        log.debug("::::::: ENTER :: updateTitleBlockBackgroudJobProcess :::::::");
        int iReturn = 0;
        try {
            String ObjectID = args[0];
            String strPolicy = args[1];
            DomainObject domObject = DomainObject.newInstance(context, ObjectID);
            HashSet<String> hsDOIDs = new HashSet<String>();
            HashMap<String, String> hmCADForDO = new HashMap<String, String>();
            if (domObject.isKindOf(context, TigerConstants.TYPE_CADDRAWING)) {
                if (TigerConstants.POLICY_PSS_CADOBJECT.equalsIgnoreCase(strPolicy)) {
                    String strDo = domObject.getInfo(context, TigerConstants.SELECT_FROM_DERIVED_OUTPUT_TO_ID);
                    hsDOIDs.add(strDo);
                    hmCADForDO.put(strDo, ObjectID);
                }
            } else if (!TigerConstants.POLICY_PSS_CADOBJECT.equalsIgnoreCase(strPolicy)) {
                BusinessObject boPart = new BusinessObject(ObjectID);
                String strRelPattern = new StringBuilder(TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING).append(TigerConstants.SEPERATOR_COMMA)
                        .append(DomainRelationship.RELATIONSHIP_PART_SPECIFICATION).toString();
                StringList slObjSel = new StringList();
                slObjSel.add(DomainConstants.SELECT_ID);
                slObjSel.add(TigerConstants.SELECT_FROM_DERIVED_OUTPUT_TO_ID);
                ContextUtil.startTransaction(context, true);
                ExpansionIterator expItPart = boPart.getExpansionIterator(context, strRelPattern, TigerConstants.TYPE_CADDRAWING, slObjSel, new StringList(), false, true, (short) 1,
                        DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, (short) 1000, false, true, (short) 1000);
                MapList mpPartList = FrameworkUtil.toMapList(expItPart, (short) 0, null, null, null, null);
                expItPart.close();
                ContextUtil.commitTransaction(context);
                for (int i = 0; i < mpPartList.size(); i++) {
                    Map mpCADMap = (Map) mpPartList.get(i);
                    String strDOID = (String) ((StringList) mpCADMap.get(TigerConstants.SELECT_FROM_DERIVED_OUTPUT_TO_ID)).get(0);
                    hsDOIDs.add(strDOID);
                    hmCADForDO.put(strDOID, (String) ((StringList) mpCADMap.get(DomainConstants.SELECT_ID)).get(0));
                }
            }
            if (hsDOIDs != null && !hsDOIDs.isEmpty()) {
                String strRPE = DomainConstants.EMPTY_STRING;
                for (String strDOID : hsDOIDs) {
                    strRPE = TigerUtils.getRPEForBUS(TigerConstants.RPE_KEY_SKIP_TB_GENERATION_WEB, strDOID);
                    String strCheckinDuring = PropertyUtil.getGlobalRPEValue(context, strRPE);
                    if (UIUtil.isNotNullAndNotEmpty(strCheckinDuring) && TigerConstants.STRING_TRUE.equalsIgnoreCase(strCheckinDuring)) {
                        log.debug(">>> SKIP ==> " + strRPE + " == " + strCheckinDuring);
                        continue;
                    }

                    MCAD_CONTEXT = context;
                    strRPE = TigerUtils.getRPEForBUS(TigerConstants.RPE_KEY_SKIP_ON_BG_JOB, strDOID);
                    strCheckinDuring = PropertyUtil.getGlobalRPEValue(context, strRPE);
                    if (UIUtil.isNotNullAndNotEmpty(strCheckinDuring) && TigerConstants.STRING_TRUE.equalsIgnoreCase(strCheckinDuring)) {
                        log.debug(">>> SKIP ==> " + strRPE + " == " + strCheckinDuring);
                        continue;
                    } else {
                        PropertyUtil.setGlobalRPEValue(context, strRPE, TigerConstants.STRING_TRUE);
                    }

                    String LoggedUserName = PropertyUtil.getGlobalRPEValue(context, "PSS_CAD_PROMOTE_USERNAME");
                    if (UIUtil.isNotNullAndNotEmpty(LoggedUserName)) {
                        args[10] = LoggedUserName;
                    }

                    String strTitle = EnoviaResourceBundle.getProperty(context, SUITE_KEY_IEF_DESIGN, context.getLocale(), "emxIEFDesignCenter.TitleBlock.UpdateTBOnPromoteDemote");
                    // Passing CADDrawing
                    args[0] = hmCADForDO.get(strDOID);
                    createAndSubmitJob(context, "pss.cad2d3d.GenerateTitleBlock", "updateTitleBlock", args, strTitle);
                }
            }
        } catch (FrameworkException fe) {
            log.error(fe.getLocalizedMessage(), fe);
            throw new FrameworkException(fe);
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
        log.debug("::::::: EXIT :: updateTitleBlockBackgroudJobProcess :::::::");
        return iReturn;
    }

    /**
     * This method will generate Title Block on promotion/demotion of objects
     * @param context
     *            context context for the request
     * @param args
     *            arguments from the request
     * @throws Exception
     */
    public int updateTitleBlock(Context context, String[] args) throws Exception {
        log.debug("::::::: ENTER :: updateTitleBlock :::::::");
        int iReturn = 0;
        String strObjectID = DomainConstants.EMPTY_STRING;
        try {
            if (UIUtil.isNotNullAndNotEmpty(DELAY_TB_GENERATION)) {
                TimeUnit.SECONDS.sleep(Integer.parseInt(DELAY_TB_GENERATION));
            } else {
                TimeUnit.SECONDS.sleep(TITLE_BLOCK_UPDATE_BG_DELAY); // 30seconds
            }
            strObjectID = args[0];
            DomainObject domObject = DomainObject.newInstance(context, strObjectID);
            if (domObject.isKindOf(context, TigerConstants.TYPE_CADDRAWING)) {
                iReturn = updateTitleBlockonPromote(context, args);
            }
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        } finally {
            PropertyUtil.setGlobalRPEValue(context, TigerUtils.getRPEForBUS(TigerConstants.RPE_KEY_SKIP_ON_BG_JOB, strObjectID), TigerConstants.STRING_FALSE);
            if (args.length >= 9 && UIUtil.isNotNullAndNotEmpty(args[8])) {
                PropertyUtil.setGlobalRPEValue(TigerUtils.getMCADSessionContext(context, args[8]), TigerUtils.getRPEForBUS(TigerConstants.RPE_KEY_SKIP_ON_BG_JOB, strObjectID),
                        TigerConstants.STRING_FALSE);
            }
        }
        log.debug("::::::: EXIT :: updateTitleBlock :::::::");
        return iReturn;
    }

    /**
     * Method to create a job to generate TB in mass from background
     * @param context
     *            context for the request
     * @param args
     *            arguments from the request
     * @return
     * @throws Exception
     */
    public int generateMassTitleBlockBackgroudJobProcess(Context context, String[] args) throws Exception {
        log.debug("::::::: ENTER :: generateMassTitleBlockBackgroudJobProcess :::::::");
        int iReturn = 0;
        try {
            String LoggedUserName = PropertyUtil.getGlobalRPEValue(context, "PSS_CAD_PROMOTE_USERNAME");
            if (UIUtil.isNotNullAndNotEmpty(LoggedUserName)) {
                args[10] = LoggedUserName;
            }
            String strTitle = EnoviaResourceBundle.getProperty(context, SUITE_KEY_IEF_DESIGN, context.getLocale(), "emxIEFDesignCenter.TitleBlock.MassTBGeneration");
            createAndSubmitJob(context, "pss.cad2d3d.GenerateTitleBlock", "generateMassTitleBlock", args, strTitle);
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
        log.debug("::::::: EXIT :: generateMassTitleBlockBackgroudJobProcess :::::::");
        return iReturn;
    }

    /**
     * Method to generate mass TB invoked by the job
     * @param context
     *            context for the request
     * @param args
     *            arguments from the request
     * @return
     * @throws Exception
     */
    public int generateMassTitleBlock(Context context, String[] args) throws Exception {
        log.debug("::::::: ENTER :: generateMassTitleBlock :::::::");
        int iReturn = 0;
        boolean isActive = false;
        try {
            ContextUtil.startTransaction(context, true);
            isActive = true;
            String strobjectId = args[0];
            DomainObject domObjt = new DomainObject(strobjectId);
            StringBuilder sbRelPattern = new StringBuilder();
            StringList slObjSel = new StringList(1);
            slObjSel.add(TigerConstants.SELECT_ATTRIBUTE_PSS_TITLEBLOCKCOMPLETED);
            slObjSel.add(DomainConstants.SELECT_ID);
            if (domObjt.isKindOf(context, DomainConstants.TYPE_PART)) {
                sbRelPattern.append(TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING).append(TigerConstants.SEPERATOR_COMMA).append(DomainRelationship.RELATIONSHIP_PART_SPECIFICATION)
                        .append(TigerConstants.SEPERATOR_COMMA).append(TigerConstants.RELATIONSHIP_EBOM);
            } else {
                sbRelPattern.append(TigerConstants.RELATIONSHIP_ASSOCIATEDDRAWING).append(TigerConstants.SEPERATOR_COMMA).append(TigerConstants.RELATIONSHIP_CADSUBCOMPONENT);
            }
            String strPostTypePattern = new StringBuilder(TigerConstants.TYPE_PSS_CATDRAWING).append(TigerConstants.SEPERATOR_COMMA).append(TigerConstants.TYPE_PSS_PROEDRAWING)
                    .append(TigerConstants.SEPERATOR_COMMA).append(TigerConstants.TYPE_PSS_SWDRAWING).append(TigerConstants.SEPERATOR_COMMA).append(TigerConstants.TYPE_PSS_UGDRAWING).toString();
            MapList mpDrawingList = domObjt.getRelatedObjects(context, sbRelPattern.toString(), DomainConstants.QUERY_WILDCARD, false, true, (short) 0, slObjSel, DomainConstants.EMPTY_STRINGLIST,
                    DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, 0, DomainConstants.EMPTY_STRING, strPostTypePattern, null);
            log.debug("::::::: No of Drawing to be update is :: " + mpDrawingList.size());
            for (int i = 0; i < mpDrawingList.size(); i++) {
                Map mpCrCoId = (Map) mpDrawingList.get(i);
                String isTitleBlockgenrated = (String) mpCrCoId.get(TigerConstants.SELECT_ATTRIBUTE_PSS_TITLEBLOCKCOMPLETED);
                if (TigerConstants.STRING_FALSE.equalsIgnoreCase(isTitleBlockgenrated)) {
                    String strDOID = DomainObject.newInstance(context, (String) mpCrCoId.get(DomainConstants.SELECT_ID)).getInfo(context, TigerConstants.SELECT_FROM_DERIVED_OUTPUT_TO_ID);
                    if (UIUtil.isNotNullAndNotEmpty(strDOID)) {
                        String strRPE = TigerUtils.getRPEForBUS(TigerConstants.RPE_KEY_SKIP_TB_GENERATION_WEB, strDOID);
                        String strCheckinDuring = PropertyUtil.getGlobalRPEValue(context, strRPE);
                        if (UIUtil.isNotNullAndNotEmpty(strCheckinDuring) && TigerConstants.STRING_TRUE.equalsIgnoreCase(strCheckinDuring)) {
                            continue;
                        }
                        String[] arg = args;
                        arg[0] = (String) mpCrCoId.get(DomainConstants.SELECT_ID);
                        arg[6] = TigerConstants.STRING_YES.toLowerCase();
                        generateTitleBlock(context, arg);
                    }
                }
            }
        } catch (Exception e) {
            if (isActive)
                ContextUtil.abortTransaction(context);
            e.printStackTrace();
            log.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        } finally {
            if (isActive) {
                ContextUtil.commitTransaction(context);
            }
        }
        log.debug("::::::: EXIT :: generateMassTitleBlock :::::::");
        return iReturn;
    }

    /**
     * Method to create a job to generate TB on revise from background
     * @param context
     *            context for the request
     * @param args
     *            arguments from the request
     * @throws Exception
     */
    public void updateTitleOnReviseBlockBackgroudJobProcess(Context context, String[] args) throws Exception {
        log.debug("::::::: ENTER :: updateTitleOnReviseBlockBackgroudJobProcess :::::::");
        // START :: PSI :: TIGTK-16397 :: ALM-6119
        boolean bPopCOntext = false;
        // END :: PSI :: TIGTK-16397 :: ALM-6119
        try {
            String strObjOID = args[0];
            String strNewRevision = args[1];
            String strType = args[2];
            String strName = args[3];
            BusinessObject boNewRevisionDO = new BusinessObject(strType, strName, strNewRevision, DomainConstants.EMPTY_STRING);
            if (boNewRevisionDO.exists(context)) {
                DomainObject domRevisedObject = new DomainObject(boNewRevisionDO);
                strObjOID = domRevisedObject.getInfo(context, TigerConstants.SELECT_FROM_DERIVED_OUTPUT_TO_ID);
                if (UIUtil.isNotNullAndNotEmpty(strObjOID)) {
                    String strRPE = TigerUtils.getRPEForBUS(TigerConstants.RPE_KEY_SKIP_TB_GENERATION_WEB, strObjOID);
                    String strCheckinDuring = PropertyUtil.getGlobalRPEValue(context, strRPE);
                    if (UIUtil.isNotNullAndNotEmpty(strCheckinDuring) && TigerConstants.STRING_TRUE.equalsIgnoreCase(strCheckinDuring)) {
                        log.debug(">>> SKIP ==> " + strRPE + " == " + strCheckinDuring);
                        return;
                    }

                    MCAD_CONTEXT = context;
                    strRPE = TigerUtils.getRPEForBUS(TigerConstants.RPE_KEY_SKIP_ON_BG_JOB, strObjOID);
                    strCheckinDuring = PropertyUtil.getGlobalRPEValue(context, strRPE);
                    if (UIUtil.isNotNullAndNotEmpty(strCheckinDuring) && TigerConstants.STRING_TRUE.equalsIgnoreCase(strCheckinDuring)) {
                        log.debug(">>> SKIP ==> " + strRPE + " == " + strCheckinDuring);
                        return;
                    } else {
                        PropertyUtil.setGlobalRPEValue(context, strRPE, TigerConstants.STRING_TRUE);
                    }

                    String LoggedUserName = PropertyUtil.getGlobalRPEValue(context, "PSS_CAD_PROMOTE_USERNAME");
                    if (UIUtil.isNotNullAndNotEmpty(LoggedUserName)) {
                        args[10] = LoggedUserName;
                    }
                    String strTitle = EnoviaResourceBundle.getProperty(context, SUITE_KEY_IEF_DESIGN, context.getLocale(), "emxIEFDesignCenter.TitleBlock.UpdateTBOnRevise");
                    // START :: PSI :: TIGTK-16397 :: ALM-6119
                    try {
                        context.getFrameContext("PSS_LOCAL_CONTEXT");
                    } catch (Exception e) {
                        ContextUtil.pushContext(context, TigerUtils.getLoggedInUserName(context), context.getPassword(), context.getVault().getName());
                        bPopCOntext = true;
                    }
                    // END :: PSI :: TIGTK-16397 :: ALM-6119
                    createAndSubmitJob(context, "pss.cad2d3d.GenerateTitleBlock", "updateTitleOnRevise", args, strTitle);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
        // START :: PSI :: TIGTK-16397 :: ALM-6119
        finally {
            if (bPopCOntext) {
                ContextUtil.popContext(context);
            }
        }
        // END :: PSI :: TIGTK-16397 :: ALM-6119
        log.debug("::::::: EXIT :: updateTitleOnReviseBlockBackgroudJobProcess :::::::");
    }

    /**
     * Method to generate TB on revise, invoked by the job.
     * @param context
     *            context for the request
     * @param args
     *            arguments from the request
     * @throws Exception
     */
    public void updateTitleOnRevise(Context context, String[] args) throws Exception {
        log.debug("::::::: ENTER :: updateTitleOnRevise :::::::");
        String strObjectID = DomainConstants.EMPTY_STRING;
        String strRPE = DomainConstants.EMPTY_STRING;
        String newDerivedObjectID = DomainConstants.EMPTY_STRING;
        try {
            if (UIUtil.isNotNullAndNotEmpty(DELAY_TB_GENERATION)) {
                TimeUnit.SECONDS.sleep(Integer.parseInt(DELAY_TB_GENERATION));
            } else {
                TimeUnit.SECONDS.sleep(TITLE_BLOCK_UPDATE_BG_DELAY); // 30seconds
            }
            strObjectID = args[0];
            String strNewRevision = args[1];
            String strType = args[2];
            String strName = args[3];
            DomainObject domoldRevision = DomainObject.newInstance(context, strObjectID);
            StringList objectSelects = new StringList(2);
            objectSelects.add(TigerConstants.SELECT_ATTRIBUTE_PSS_TITLEBLOCKCOMPLETED);
            objectSelects.add(TigerConstants.SELECT_FROM_DERIVED_OUTPUT_TO_ID);
            objectSelects.add(TigerConstants.SELECT_FROM_PSS_PDF_ARCHIVE_TO_ID);
            Map dos_prew = domoldRevision.getInfo(context, objectSelects);
            String isTitleBlockgenrated = (String) dos_prew.get(TigerConstants.SELECT_ATTRIBUTE_PSS_TITLEBLOCKCOMPLETED);
            if (TigerConstants.STRING_TRUE.equalsIgnoreCase(isTitleBlockgenrated)) {
                BusinessObject revisedMajorObject = new BusinessObject(strType, strName, strNewRevision, DomainConstants.EMPTY_STRING);
                if (revisedMajorObject.exists(context)) {
                    DomainObject domRevisedObject = new DomainObject(revisedMajorObject);
                    newDerivedObjectID = domRevisedObject.getInfo(context, TigerConstants.SELECT_FROM_DERIVED_OUTPUT_TO_ID);
                    if (UIUtil.isNotNullAndNotEmpty(newDerivedObjectID)) {
                        if (UIUtil.isNotNullAndNotEmpty((String) dos_prew.get(TigerConstants.SELECT_FROM_PSS_PDF_ARCHIVE_TO_ID))) {
                            // START :: TIGTK-16397 :: ALM-6119
                            // ContextUtil.startTransaction(context, true);
                            // END :: TIGTK-16397 :: ALM-6119
                            TigerUtils.deleteFiles(context, newDerivedObjectID, FileFormat.PDF(), TigerUtils.getValidPDFSheetsName(context, new BusinessObject(newDerivedObjectID)));
                            try {
                                TigerUtils.deleteFile(context, newDerivedObjectID, FileFormat.GENERIC(), "all");
                            } catch (FrameworkException fe) {
                                log.warn("Skipping Delete :: " + fe.getLocalizedMessage());
                            }
                            BusinessObject boPDFArchive = new BusinessObject((String) dos_prew.get(TigerConstants.SELECT_FROM_PSS_PDF_ARCHIVE_TO_ID));
                            FileList flFiles = TigerUtils.getValidPDFSheets(context, boPDFArchive);
                            String strWorkspace = TigerUtils.getWorkspaceForBus(context, DomainConstants.EMPTY_STRING, strObjectID + "_JOB_REVISE");
                            boPDFArchive.checkoutFiles(context, false, FileFormat.PDF(), flFiles, strWorkspace);
                            FileItr fileItr = new FileItr(flFiles);
                            matrix.db.File file = null;
                            StringList slFileNames = new StringList(flFiles.size());
                            while (fileItr.next()) {
                                file = fileItr.obj();
                                slFileNames.add(file.getName());
                            }
                            strRPE = TigerUtils.getRPEForBUS(TigerConstants.RPE_KEY_SKIP_TB_GENERATION_WEB, newDerivedObjectID);
                            PropertyUtil.setGlobalRPEValue(context, "JOB_" + strRPE, TigerConstants.STRING_TRUE);
                            BusinessObject boNewDerivedOutput = new BusinessObject(newDerivedObjectID);
                            for (Object fileName : slFileNames) {
                                boNewDerivedOutput.checkinFile(context, true, true, DomainConstants.EMPTY_STRING, FileFormat.PDF(), fileName.toString(), strWorkspace);
                            }

                            String[] arg = getTitelBlockParamsFromXML(context, domoldRevision, false);
                            arg[0] = revisedMajorObject.getObjectId(context);
                            arg[6] = TigerConstants.STRING_YES.toLowerCase(); // BG process
                            arg[8] = args[8];
                            TigerUtils.cleanUp(new File(strWorkspace));
                            // START :: TIGTK-16397 :: ALM-6119
                            // ContextUtil.commitTransaction(context);
                            // END :: TIGTK-16397 :: ALM-6119
                            generateTitleBlock(context, arg);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            // START :: TIGTK-16397 :: ALM-6119
            /*
             * if(ContextUtil.isTransactionActive(context)){ ContextUtil.abortTransaction(context); }
             */
            // END :: TIGTK-16397 :: ALM-6119
            throw new Exception(e.getLocalizedMessage(), e);
        } finally {
            // START :: TIGTK-16397 :: ALM-6119
            /*
             * if(ContextUtil.isTransactionActive(context)){ ContextUtil.commitTransaction(context); }
             */
            // END :: TIGTK-16397 :: ALM-6119
            TigerUtils.resetGlobalRPEs(context, newDerivedObjectID);
            PropertyUtil.setGlobalRPEValue(context, "JOB_" + strRPE, TigerConstants.STRING_FALSE);
            PropertyUtil.setGlobalRPEValue(context, TigerUtils.getRPEForBUS(TigerConstants.RPE_KEY_SKIP_ON_BG_JOB, strObjectID), TigerConstants.STRING_FALSE);
            if (args.length >= 9 && UIUtil.isNotNullAndNotEmpty(args[8])) {
                PropertyUtil.setGlobalRPEValue(TigerUtils.getMCADSessionContext(context, args[8]), TigerUtils.getRPEForBUS(TigerConstants.RPE_KEY_SKIP_ON_BG_JOB, strObjectID),
                        TigerConstants.STRING_FALSE);
            }
        }
        log.debug("::::::: EXIT :: updateTitleOnRevise :::::::");
    }

    /**
     * Creates Job to Update TitleBlock during connect/disconnect drawing
     * @param context
     *            context context for the request
     * @param args
     *            arguments from the request
     * @throws Exception
     */
    public void updateTitleBlockOnConnectDisconnectDrawingBackgroudJobProcess(Context context, String[] args) throws Exception {
        log.debug("::::::: ENTER :: updateTitleBlockOnConnectDisconnectDrawingBackgroudJobProcess :::::::");
        // START :: TIGTK-16397 :: ALM-6119
        boolean bPopCOntext = false;
        // END :: TIGTK-16397 :: ALM-6119
        try {
            String strObjOID = args[0];
            DomainObject doObject = DomainObject.newInstance(context, strObjOID);
            if (doObject.isKindOf(context, TigerConstants.TYPE_CADDRAWING)) {
                strObjOID = doObject.getInfo(context, TigerConstants.SELECT_FROM_DERIVED_OUTPUT_TO_ID);
                if (UIUtil.isNullOrEmpty(strObjOID)) {
                    log.info("SKIP TB Update >>> No Derived Output found for '" + doObject.getName());
                    return;
                }
                String strRPE = TigerUtils.getRPEForBUS(TigerConstants.RPE_KEY_SKIP_TB_GENERATION_WEB, strObjOID);
                String strCheckinDuring = PropertyUtil.getGlobalRPEValue(context, strRPE);
                if (UIUtil.isNotNullAndNotEmpty(strCheckinDuring) && TigerConstants.STRING_TRUE.equalsIgnoreCase(strCheckinDuring)) {
                    log.debug(">>> SKIP ==> " + strRPE + " == " + strCheckinDuring);
                    return;
                }

                MCAD_CONTEXT = context;
                strRPE = TigerUtils.getRPEForBUS(TigerConstants.RPE_KEY_SKIP_ON_BG_JOB, strObjOID);
                strCheckinDuring = PropertyUtil.getGlobalRPEValue(context, strRPE);
                if (UIUtil.isNotNullAndNotEmpty(strCheckinDuring) && TigerConstants.STRING_TRUE.equalsIgnoreCase(strCheckinDuring)) {
                    log.debug(">>> SKIP ==> " + strRPE + " == " + strCheckinDuring);
                    return;
                } else {
                    PropertyUtil.setGlobalRPEValue(context, strRPE, TigerConstants.STRING_TRUE);
                }

                String LoggedUserName = PropertyUtil.getGlobalRPEValue(context, "PSS_CAD_PROMOTE_USERNAME");
                if (UIUtil.isNotNullAndNotEmpty(LoggedUserName)) {
                    args[10] = LoggedUserName;
                }
                String strTitle = EnoviaResourceBundle.getProperty(context, SUITE_KEY_IEF_DESIGN, context.getLocale(), "emxIEFDesignCenter.TitleBlock.UpdateTBOnConnectDisconnectDrawing");
                // START :: TIGTK-16397 :: ALM-6119
                try {
                    context.getFrameContext("PSS_LOCAL_CONTEXT");
                } catch (Exception e) {
                    ContextUtil.pushContext(context, TigerUtils.getLoggedInUserName(context), context.getPassword(), context.getVault().getName());
                    bPopCOntext = true;
                }
                // END :: TIGTK-16397 :: ALM-6119
                createAndSubmitJob(context, "pss.cad2d3d.GenerateTitleBlock", "updateTitleBlockOnConnectDisconnectDrawing", args, strTitle);
            }
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
        // START :: TIGTK-16397 :: ALM-6119
        finally {
            if (bPopCOntext) {
                ContextUtil.popContext(context);
            }
        }
        // END :: TIGTK-16397 :: ALM-6119
        log.debug("::::::: EXIT :: updateTitleBlockOnConnectDisconnectDrawingBackgroudJobProcess :::::::");
    }

    /**
     * Method executed by job to Update TitleBlock during connect/disconnect drawing
     * @param context
     *            context context for the request
     * @param args
     *            arguments from the request
     * @throws Exception
     */
    public void updateTitleBlockOnConnectDisconnectDrawing(Context context, String[] args) throws Exception {
        log.debug("::::::: ENTER :: updateTitleBlockOnConnectDisconnectDrawing :::::::");
        String ObjectID = args[0];
        DomainObject dom = DomainObject.newInstance(context, ObjectID);
        String strAttrTitleBlock = dom.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_TITLEBLOCKCOMPLETED);
        try {
            if (dom.isKindOf(context, TigerConstants.TYPE_CADDRAWING) && TigerConstants.STRING_TRUE.equalsIgnoreCase(strAttrTitleBlock)) {
                // sleep job to skip from creating multiple job
                if (UIUtil.isNotNullAndNotEmpty(DELAY_TB_GENERATION)) {
                    TimeUnit.SECONDS.sleep(Integer.parseInt(DELAY_TB_GENERATION));
                } else {
                    TimeUnit.SECONDS.sleep(TITLE_BLOCK_UPDATE_BG_DELAY); // 30seconds
                }
                updateTitleBlockonPromote(context, args);
            }
        } catch (FrameworkException fe) {
            log.error(fe.getLocalizedMessage(), fe);
            throw new Exception(fe.getLocalizedMessage(), fe);
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
        log.debug("::::::: EXIT :: updateTitleBlockOnConnectDisconnectDrawing :::::::");
    }

    /**
     * This method is used to copy PSS_ArchivedDO on clone object.
     * @param context
     * @param args
     * @throws Exception
     */
    public void connectOriginalPDFIfTitleBlockGeneratedForSourceCAD(Context context, String args[]) throws Exception {
        log.debug("::::::: ENTER :: connectOriginalPDFIfTitleBlockGeneratedForSourceCAD :::::::");
        try {
            String strOriginalId = args[0];
            String strClonedIOrRevisedId = args[1];
            DomainObject domOldObject = DomainObject.newInstance(context, strOriginalId);
            DomainObject domNewObject = DomainObject.newInstance(context, strClonedIOrRevisedId);
            String strPDFArchive = domOldObject.getInfo(context, TigerConstants.SELECT_FROM_PSS_PDF_ARCHIVE_TO_ID);
            if (UIUtil.isNotNullAndNotEmpty(strPDFArchive)) {
                DomainObject doObject = DomainObject.newInstance(context);
                DomainRelationship drArchivedDO = doObject.createAndConnect(context, TigerConstants.TYPE_PSS_PDFARCHIVE, domNewObject.getName(), domNewObject.getRevision(),
                        TigerConstants.POLICY_PSS_PDFARCHIVE, TigerConstants.VAULT_ESERVICEPRODUCTION, TigerConstants.RELATIONSHIP_PSS_ARCHIVEDO, domNewObject, true);
                BusinessObject BOToObjectNew = drArchivedDO.getTo();
                BusinessObject BOToObject = new BusinessObject(strPDFArchive);
                StringList slFileList = TigerUtils.getFileNames(context, BOToObject, FileFormat.PDF());
                FileList flFiles = TigerUtils.getValidPDFSheets(context, BOToObject);
                String strWorkspace = TigerUtils.getWorkspaceForBus(context, DomainConstants.EMPTY_STRING, strOriginalId);
                BOToObject.checkoutFiles(context, false, FileFormat.PDF(), flFiles, strWorkspace);
                int fileCount = slFileList.size();
                for (int i = 0; i < fileCount; i++) {
                    BOToObjectNew.checkinFile(context, false, true, DomainConstants.EMPTY_STRING, FileFormat.PDF(), (String) slFileList.get(i), strWorkspace);
                }
                domNewObject.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_TITLEBLOCKCOMPLETED, TigerConstants.STRING_TRUE);
                TigerUtils.cleanUp(new File(strWorkspace));
            }
        } catch (FrameworkException fe) {
            log.error(fe.getLocalizedMessage(), fe);
            throw new Exception(fe.getLocalizedMessage(), fe);
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
        log.debug("::::::: EXIT :: connectOriginalPDFIfTitleBlockGeneratedForSourceCAD :::::::");
    }

    /**
     * This method will check for file present in Derived Output.
     * @param context
     * @param args
     * @throws Exception
     * @author Steepgraph Systems
     */
    public String checkFilePresentInObject(Context context, String[] args) throws Exception {
        String strObjId = args[0];
        String strFile = DomainConstants.EMPTY_STRING;
        boolean isActive = false;
        try {
            DomainObject domObject = DomainObject.newInstance(context, strObjId);
            if (domObject.isKindOf(context, TigerConstants.TYPE_CADDRAWING)) {
                String strDrawingName = domObject.getName();
                StringList strList = new StringList(2);
                strList.add(TigerConstants.SELECT_ATTRIBUTE_PSS_TITLEBLOCKCOMPLETED);
                strList.add(TigerConstants.SELECT_FROM_PSS_PDF_ARCHIVE_TO_ID);
                Map map = domObject.getInfo(context, strList);
                String isTitleBlock = (String) map.get(TigerConstants.SELECT_ATTRIBUTE_PSS_TITLEBLOCKCOMPLETED);
                String strADOID = (String) map.get(TigerConstants.SELECT_FROM_PSS_PDF_ARCHIVE_TO_ID);

                if (TigerConstants.STRING_TRUE.equalsIgnoreCase(isTitleBlock) && UIUtil.isNotNullAndNotEmpty(strADOID)) {
                    DomainObject domADO = DomainObject.newInstance(context, strADOID);
                    strFile = isFilePresent(context, DomainConstants.EMPTY_STRING, strDrawingName, domADO, FileFormat.PDF());
                } else {
                    StringList slObjSel = new StringList();
                    slObjSel.add(DomainConstants.SELECT_ID);
                    slObjSel.add(DomainConstants.SELECT_NAME);
                    StringList slRelSel = new StringList();
                    ContextUtil.startTransaction(context, true);
                    isActive = true;
                    ExpansionIterator expItDerived = domObject.getExpansionIterator(context, TigerConstants.RELATIONSHIP_DERIVEDOUTPUT, TigerConstants.TYPE_DERIVED_OUTPUT, slObjSel, slRelSel, false,
                            true, (short) 1, null, null, (short) 1, false, true, (short) 1);
                    MapList mpDerivedList = FrameworkUtil.toMapList(expItDerived, (short) 0, null, null, null, null);
                    expItDerived.close();
                    String strId = (String) ((StringList) ((Map) mpDerivedList.get(0)).get(DomainConstants.SELECT_ID)).get(0);
                    DomainObject domObjDerived = DomainObject.newInstance(context, strId);
                    strFile = isFilePresent(context, DomainConstants.EMPTY_STRING, strDrawingName, domObjDerived, FileFormat.PDF());
                }
            } else {
                strFile = "CADMODELORPart";
            }
        } catch (Exception e) {
            log.error("Error in checkFilePresentInObject: ", e);
            if (isActive)
                ContextUtil.abortTransaction(context);
        } finally {
            if (isActive)
                ContextUtil.commitTransaction(context);
        }
        return strFile;
    }

    /**
     * This method will check for file present in an object.
     * @param context
     * @param strFileName
     * @param strDrawingName
     * @param domObject
     * @param strFormat
     * @throws Exception
     * @author Steepgraph Systems
     */
    public String isFilePresent(Context context, String strFileName, String strDrawingName, DomainObject domObject, String strFormat) throws Exception {
        String strObjFileName = DomainConstants.EMPTY_STRING;
        String strFileNameReturn = DomainConstants.EMPTY_STRING;
        try {
            MapList mlFiles = domObject.getAllFormatFiles(context);
            // String strName = strDrawingName + ".";
            if (mlFiles != null) {
                for (int i = 0; i < mlFiles.size(); i++) {
                    Map mapFiles = (Map) mlFiles.get(i);
                    strObjFileName = (String) mapFiles.get("filename");
                    if (FileFormat.PDF().equalsIgnoreCase(strFormat)) {
                        // if (strObjFileName.contains(strName)) {
                        String strObjFormat = (String) mapFiles.get("format");
                        if (strFormat.equalsIgnoreCase(strObjFormat)) {
                            strFileNameReturn = strObjFileName;
                        }
                        // }
                    } else if (FileFormat.XML().equalsIgnoreCase(strFormat)) {
                        if (strFileName.equals(strObjFileName)) {
                            strFileNameReturn = strObjFileName;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error in isFilePresent: ", e);
        }
        return strFileNameReturn;
    }

    /**
     * Methode to remove the Details sheet PDFs from the Derived Output at the time of checkin from CATIA V5 TIGTK-17750 :: ALM-837
     * @param context
     * @param derivedOutputOID
     * @param drawingOID
     * @throws Exception
     */
    private void checkAndRemoveDetailSheet(Context context, String derivedOutputOID, String drawingOID) throws Exception {
        log.debug("::::::: ENTER :: checkAndRemoveDetailSheet :::::::");
        try {
            DomainObject doDrawing = DomainObject.newInstance(context, drawingOID);
            String strDetialSheetInfo = doDrawing.getInfo(context, TigerConstants.SELECT_ATTRIBUTE_PSS_DETAIL_SHEET_INFO_VALUE);
            if (UIUtil.isNotNullAndNotEmpty(strDetialSheetInfo)) {
                BusinessObject boDerivedOutput = new BusinessObject(derivedOutputOID);
                FileList flPDFs = boDerivedOutput.getFiles(context, FileFormat.PDF());
                if (flPDFs != null && !flPDFs.isEmpty()) {
                    StringList slDetailSheetList = FrameworkUtil.split(strDetialSheetInfo, TigerConstants.SEPERATOR_COMMA);
                    ArrayList<String> alDetailSheetNames = new ArrayList<String>(slDetailSheetList.size());
                    for (Object object : slDetailSheetList) {
                        StringList slDetailSheet = FrameworkUtil.split((String) object, "|");
                        String strName = FrameworkUtil.findAndReplace((String) slDetailSheet.get(0), ".", "_");
                        strName = FrameworkUtil.findAndReplace(strName, " ", "_");
                        alDetailSheetNames.add(strName);
                    }
                    if (!alDetailSheetNames.isEmpty()) {
                        String strWorkspace = TigerUtils.getWorkspaceForBus(context, CONTEXT_WORKSPACE, derivedOutputOID);
                        boDerivedOutput.checkoutFiles(context, false, FileFormat.PDF(), flPDFs, strWorkspace);

                        FileItr fileItr = new FileItr(flPDFs);
                        matrix.db.File file = null;
                        String strPDFName = DomainConstants.EMPTY_STRING;
                        String strSheetName = DomainConstants.EMPTY_STRING;
                        // 34049774003.CATDrawing.pdf --> Multi sheet PDF
                        // 34049774003.CATDrawing_Sheet_1.pdf --> Single Sheet PDF
                        while (fileItr.next()) {
                            file = fileItr.obj();
                            strPDFName = file.getName();
                            strSheetName = strPDFName.substring(0, strPDFName.indexOf(".pdf"));
                            if (strSheetName.contains(".CATDrawing_")) {
                                // singel sheet drawing
                                strSheetName = strSheetName.substring(strSheetName.indexOf(".CATDrawing_") + ".CATDrawing_".length());
                                if (alDetailSheetNames.contains(strSheetName)) {
                                    TigerUtils.deleteFile(context, derivedOutputOID, FileFormat.PDF(), strPDFName);
                                }
                            } else if (strSheetName.endsWith(".CATDrawing")) {
                                // multi sheet drawing or single page PDF
                                File pdfFile = new File(strWorkspace + java.io.File.separator + strPDFName);
                                scratchFile = new ScratchFile(MemoryUsageSetting.setupMainMemoryOnly());
                                InputStream ins = new FileInputStream(pdfFile);
                                raFile = new RandomAccessBufferedFileInputStream(ins);
                                ins.close();
                                PDDocument doc = load(pdfFile);
                                int i = alDetailSheetNames.size();
                                while (i != 0) {
                                    doc.removePage(doc.getNumberOfPages() - 1);
                                    i--;
                                }
                                String strNewWorkspace = TigerUtils.getWorkspaceForBus(context, CONTEXT_WORKSPACE, derivedOutputOID);
                                File outFile = new File(strNewWorkspace + java.io.File.separator + strPDFName);
                                FileOutputStream fos = new FileOutputStream(outFile);
                                BufferedOutputStream bos = new BufferedOutputStream(fos);
                                doc.save(bos);
                                doc.close();
                                IOUtils.closeQuietly(scratchFile);
                                IOUtils.closeQuietly(raFile);
                                bos.flush();
                                bos.close();
                                fos.flush();
                                fos.close();
                                TigerUtils.deleteFile(context, derivedOutputOID, FileFormat.PDF(), strPDFName);
                                boDerivedOutput.checkinFile(context, true, true, DomainConstants.EMPTY_STRING, FileFormat.PDF(), strPDFName, strNewWorkspace);
                                TigerUtils.cleanUp(new File(strNewWorkspace));
                            }
                            TigerUtils.cleanUp(new File(strWorkspace));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
        log.debug("::::::: EXIT :: checkAndRemoveDetailSheet :::::::");
    }

    private PDDocument load(File file) throws IOException {
        try {
            try {
                PDFParser parser = new PDFParser(raFile, "", null, null, scratchFile);
                parser.parse();
                return parser.getPDDocument();
            } catch (IOException ioe) {
                IOUtils.closeQuietly(scratchFile);
                throw ioe;
            }
        } catch (IOException ioe) {
            IOUtils.closeQuietly(raFile);
            throw ioe;
        }
    }

    /**
     * ALM-6341 Note: Not to use this method for auto TB generation to check the status. Only on Manual TB Generation.
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public String getTBStatusForDrawingForManualGeneration(Context context, String args[]) throws Exception {
        log.debug("::::::: ENTER :: getTBStatusForDrawingForManualGeneration :::::::");
        String strStatus = DomainConstants.EMPTY_STRING;
        try {
            String strDrawingOID = args[0];
            DomainObject doDrawing = DomainObject.newInstance(context, strDrawingOID);
            String strDOid = doDrawing.getInfo(context, TigerConstants.SELECT_FROM_DERIVED_OUTPUT_TO_ID);
            String strIsTBRunning = PropertyUtil.getGlobalRPEValue(context, TigerUtils.getRPEForBUS(TigerConstants.RPE_KEY_SKIP_ON_BG_JOB, strDOid));
            if (UIUtil.isNotNullAndNotEmpty(strIsTBRunning) && TigerConstants.STRING_TRUE.equalsIgnoreCase(strIsTBRunning)) {
                strStatus = EnoviaResourceBundle.getProperty(context, SUITE_KEY_IEF_DESIGN, context.getLocale(), "emxIEFDesignCenter.TitleBlock.TBGenerationIsInProgress");
            }
            if (UIUtil.isNullOrEmpty(strStatus)) {
                strIsTBRunning = TigerUtils.getRPEForBUS(TigerConstants.RPE_KEY_SKIP_TB_GENERATION_WEB, strDOid);
                if (UIUtil.isNotNullAndNotEmpty(strIsTBRunning) && TigerConstants.STRING_TRUE.equalsIgnoreCase(strIsTBRunning)) {
                    strStatus = EnoviaResourceBundle.getProperty(context, SUITE_KEY_IEF_DESIGN, context.getLocale(), "emxIEFDesignCenter.TitleBlock.TBGenerationIsInProgress");
                }
            }
            // TB may running with Integration context
            if (UIUtil.isNullOrEmpty(strStatus)) {
                ArrayList<Context> alIntegContexts = TigerUtils.getMCADSessionContextsList(context);
                for (Context integContext : alIntegContexts) {
                    strIsTBRunning = PropertyUtil.getGlobalRPEValue(integContext, TigerUtils.getRPEForBUS(TigerConstants.RPE_KEY_SKIP_ON_BG_JOB, strDOid));
                    if (UIUtil.isNotNullAndNotEmpty(strIsTBRunning) && TigerConstants.STRING_TRUE.equalsIgnoreCase(strIsTBRunning)) {
                        strStatus = EnoviaResourceBundle.getProperty(context, SUITE_KEY_IEF_DESIGN, context.getLocale(), "emxIEFDesignCenter.TitleBlock.TBGenerationIsInProgress");
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
        log.debug("::::::: EXIT :: getTBStatusForDrawingForManualGeneration :::::::");
        return strStatus;
    }

}
