package pss.common.utils;

import java.io.File;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.text.WordUtils;
import org.apache.fontbox.util.autodetect.FontFileFinder;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.StringUtil;
import com.matrixone.apps.framework.ui.UIUtil;

import be.quodlibet.boxable.BaseTable;
import be.quodlibet.boxable.Cell;
import be.quodlibet.boxable.ImageCell;
import be.quodlibet.boxable.Row;
import be.quodlibet.boxable.TableCell;
import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.FileItr;
import matrix.db.FileList;
import matrix.util.MatrixException;
import matrix.util.StringList;
import pss.common.utils.TigerEnums.FileFormat;
import pss.common.utils.TigerEnums.Font;
import pss.common.utils.TigerEnums.MessageType;
import pss.constants.TigerConstants;

public class TigerUtils {
	
	private static Logger log                                           = LoggerFactory.getLogger(TigerUtils.class);
	private static final String datePattern                             = "MM/dd/yyyy hh:mm:ss a";
	private static final HashMap<String, Context> hmMCADContexts        = new HashMap<String, Context>();
	private static String newline                                       = System.getProperty("line.separator");
	
	private TigerUtils(){
		throw new AssertionError("TigerUtils is not to be instantiated...");
	}
	
	/**
	 * This method will show the alert message to user
	 * @param context
	 * @param messageType
	 * @param errorMesssageKey
	 * @throws Exception 
	 */
	public static void showMessage(Context context, MessageType messageType, String errorMesssageKey) throws Exception{
		log.trace("::::::: ENTER :: showMessage :::::::");
		try{
			String strMessage = TigerAppConfigProperties.getPropertyValue(context, errorMesssageKey);
			strMessage = UIUtil.isNullOrEmpty(strMessage) ? errorMesssageKey : strMessage;
			if(TigerEnums.MessageType.ERROR_MESSAGE.equals(messageType)){
				MqlUtil.mqlCommand(context, "error $1", strMessage);
			} else if (TigerEnums.MessageType.WARNING_MESSAGE.equals(messageType)) {
				MqlUtil.mqlCommand(context, "warning $1", strMessage);
			} else {
				MqlUtil.mqlCommand(context, "notice $1", strMessage);
			}
		}catch(FrameworkException fe){
			fe.printStackTrace();
			log.error(fe.getMessage(), fe);
			throw new FrameworkException(fe);
		}
		log.trace("::::::: EXIT :: showMessage :::::::");
	}
	
	/**
	 * This method will show the alert message to user
	 * @param context
	 * @param messageType
	 * @param errorMesssageKey
	 * @throws FrameworkException
	 */
	public static void showMessage(Context context, MessageType messageType, String errorMesssageKey, String suiteKey) throws FrameworkException {
		log.trace("::::::: ENTER :: showMessage :::::::");
		try{
			String strMessage = EnoviaResourceBundle.getProperty(context, 
																	suiteKey,
																	errorMesssageKey, 
																	context.getSession().getLanguage());
			strMessage = UIUtil.isNullOrEmpty(strMessage) ? errorMesssageKey : strMessage;
			if(TigerEnums.MessageType.ERROR_MESSAGE.equals(messageType)){
				MqlUtil.mqlCommand(context, "error $1", strMessage);
			} else if (TigerEnums.MessageType.WARNING_MESSAGE.equals(messageType)) {
				MqlUtil.mqlCommand(context, "warning $1", strMessage);
			} else {
				MqlUtil.mqlCommand(context, "notice $1", strMessage);
			}
		}catch(FrameworkException e){
			e.printStackTrace();
			log.error(e.getMessage(), e);
			throw new FrameworkException(e);
		}
		log.trace(":::::: EXIT :: showMessage :::::::");
	}
	
    /**
     * @param context
     * @throws FrameworkException
     */
    public static void pushContextToSuperUser(Context context) throws FrameworkException {
        try {
            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
        } catch (FrameworkException fe) {
            fe.printStackTrace();
            log.error("Issue encountered during pushing context to super user", fe);
            throw new FrameworkException(fe);
        }
    }
    
    /**
     * returs StringList of file names for the given format
     * @param context
     * @param busObj
     * @param format
     * @return
     * @throws MatrixException
     */
    public static StringList getFileNames(Context context, BusinessObject busObj, String format) throws MatrixException {
    	StringList slFileNames = new StringList();
    	try{
    		FileList flFiles = (UIUtil.isNotNullAndNotEmpty(format)) ? busObj.getFiles(context, format) : busObj.getFiles(context);
    		FileItr fileItr = new FileItr(flFiles);
    		matrix.db.File file = null;
    		while (fileItr.next()) {
				file = fileItr.obj();
				slFileNames.add(file.getName());
    		}
    	}catch(MatrixException fe){
    		 fe.printStackTrace();
             log.error(fe.getLocalizedMessage(), fe);
             throw new FrameworkException(fe);
    	}
    	return slFileNames;
    }
    
    /**
     * returns SL of file names excluding detailed drawing sheets
     * @param context
     * @param busObj
     * @return
     * @throws MatrixException
     */
    public static StringList getValidPDFSheetsName(Context context, BusinessObject busObj) throws MatrixException {
        StringList slFileNames = new StringList();
        try{
            FileList flFiles = busObj.getFiles(context, FileFormat.PDF());
            FileItr fileItr = new FileItr(flFiles);
            matrix.db.File file = null;
            String strFileName = DomainConstants.EMPTY_STRING;
            while (fileItr.next()) {
                file = fileItr.obj();
                strFileName = file.getName();
                if(strFileName.indexOf("(Detail") == -1){
                    slFileNames.add(strFileName);
                }
            }
        }catch(MatrixException fe){
             fe.printStackTrace();
             log.error(fe.getLocalizedMessage(), fe);
             throw new FrameworkException(fe);
        }
        return slFileNames;
    }
    
    /**
     * returns FileList of files excluding detailed drawing sheets
     * @param context
     * @param busObj
     * @return
     * @throws MatrixException
     */
    public static FileList getValidPDFSheets(Context context, BusinessObject busObj) throws MatrixException {
        FileList returnFiles = new FileList();
        try{
           FileList flFiles = busObj.getFiles(context, "PDF");
           FileItr fileItr = new FileItr(flFiles);
            matrix.db.File file = null;
            String strFileName = DomainConstants.EMPTY_STRING;
            while (fileItr.next()) {
                file = fileItr.obj();
                strFileName = file.getName();
                if(strFileName.indexOf("(Detail") == -1){
                    returnFiles.add(file);
                }
            }
        }catch(MatrixException fe){
             fe.printStackTrace();
             log.error(fe.getLocalizedMessage(), fe);
             throw new FrameworkException(fe);
        }
        return returnFiles;
    }
    
    /**
     * get store for the given Policy
     * @param context
     * @param policy
     * @return
     * @throws FrameworkException
     */
    public static String getStoreForPolicy(Context context, String policy) throws FrameworkException {
		String strStoreName = DomainConstants.EMPTY_STRING;
		try {
			strStoreName = MqlUtil.mqlCommand(context, "print policy $1 select $2 dump;", new String[] { policy, "store" }).replace("\n", "");
		} catch (FrameworkException fe) {
			fe.printStackTrace();
            log.error(fe.getLocalizedMessage(), fe);
            throw new FrameworkException(fe);
		}
		return strStoreName;
	}
    
    /**
     * deletes the file from the given Business Object
     * @param context
     * @param busOID
     * @param format
     * @param fileName
     * @throws FrameworkException
     */
    public static void deleteFile(Context context, String busOID, String format, String fileName) throws FrameworkException {
    	try{
			MqlUtil.mqlCommand(context, "delete bus $1 format $2 file $3", new String[] { busOID, format, fileName });
		}catch(FrameworkException fe){
			fe.printStackTrace();
            log.error(fe.getLocalizedMessage(), fe);
            throw new FrameworkException(fe);
		}
    }
    
    /**
     * deletes the file from the given Business Object
     * @param context
     * @param busObj
     * @param format
     * @param fileName
     * @throws FrameworkException
     */
    public static void deleteFiles(Context context, String busOID, String format, StringList files) throws FrameworkException {
    	try{
			for (Object fileName : files) {
				MqlUtil.mqlCommand(context, "delete bus $1 format $2 file $3", new String[] { busOID, format, fileName.toString() });
			}
		}catch(FrameworkException fe){
			fe.printStackTrace();
            log.error(fe.getLocalizedMessage(), fe);
            throw new FrameworkException(fe);
		}
    }
    
    /**
     * Deletes all files and folders in the specified Directory/file
     * @param file file or folder to be deleted
     * @throws Exception
     */
    public static void cleanUp(File file) throws Exception{
        try{
            if(file != null){
                if(file.exists()){
                    if(file.isDirectory()){
                        String[] files = file.list();
                        if(files !=null){
                            if(files.length==0){
                                file.delete();
                            }else{
                                for (String strTemp : files) {
                                    File ftempFile = new File(file, strTemp);
                                    cleanUp(ftempFile);
                                }
                                file.delete();
                            }
                        }
                    }else{
                        file.delete();
                    }
                }
            }
        }catch(Exception e){
            log.error(e.getLocalizedMessage(), e);
        }       
    }
    
    /**
     * get width of row by calculating width of cells
     * @param row row of table
     * @return width of cells in the row
     */
    public static float getRowCellsWidht(Row<PDPage> row) {
        float width = 0f;
        List<Cell<PDPage>> Cells = row.getCells();
        for (Cell<PDPage> cell : Cells) {
            width += cell.getWidth();
        }
        return width;
    }
    
    /**
     * Converts point to millimeter
     * @param pt number in point to convert to mm
     * @return converted number
     */
    public static float pt2mm(float pt) {
           return pt * (25.4f / 72);
    }
    
    /**
     * Converts millimeter to point
     * @param mm number in mm to convert to point
     * @return converted number
     */
    public static float mm2pt(float mm) {
           return mm / (25.4f / 72);
    }
    
    /**
     * This method will return system date in format of mm/dd/yyyy hh:mm:ss a with specified delay
     * @param context Current context
     * @param currentDate Current date as a string
     * @param calenderField SECOND, MINUTE, HOUR, WEEK, MONTH, YEAR...
     * @param delay number to be delayed
     * @return strDelay String of Date time with specified delay
     * @throws Exception
     */
    public static String getDelayedTime(Context context, String currentDate, int calenderField, int delay) throws Exception{
        String strDelay = DomainConstants.EMPTY_STRING;
        try{
            SimpleDateFormat dateFormatter = new SimpleDateFormat(datePattern);
            Calendar calender = Calendar.getInstance();
            calender.setTime(dateFormatter.parse(currentDate));
            calender.add(calenderField, delay);
            strDelay = dateFormatter.format(calender.getTime());
        } catch(Exception e){
            e.printStackTrace();
            throw new Exception(e);
        }
        return strDelay;
    }
    
    /**
     * This method will return current system date in format of mm/dd/yyyy hh:mm:ss a
     * @param context current context
     * @return strCurrentDate - Current date & time as a string
     * @throws Exception
     */
    public static String getCurrentDate(Context context) throws Exception {
        String strCurrentDate = DomainConstants.EMPTY_STRING;
        try{
            SimpleDateFormat dateFormatter = new SimpleDateFormat(datePattern);
            Calendar calender = Calendar.getInstance();
            strCurrentDate = dateFormatter.format(calender.getTime());
        } catch(Exception e){
            e.printStackTrace();
            throw new Exception(e);
        }
        return strCurrentDate;
    }
    
    /**
     * Method to get loggedin user name
     * @param context current context
     * @return string of user name
     * @throws FrameworkException
     */
    public static String getLoggedInUserName(Context context) throws FrameworkException{
        String strUser = DomainConstants.EMPTY_STRING;
        try{
            strUser = context.getUser();
            if (("User Agent").equals(strUser))
                strUser = PropertyUtil.getGlobalRPEValue(context, "MX_LOGGED_IN_USER_NAME");
        } catch(FrameworkException fe){
            fe.printStackTrace();
            throw new FrameworkException(fe);
        }
        return strUser;
    }
    
    /**
     * Creates temporary workspace for checkin/checout files
     * @param context current context
     * @param baseWorkSpace base workspace folder
     * @param busOID OID of bussinessobject
     * @return String of woekspace folder path
     * @throws Exception
     */
    public static String getWorkspaceForBus(Context context, String baseWorkSpace, String busOID) throws Exception {
        String strWSForBus = DomainConstants.EMPTY_STRING;
        try{
            synchronized (busOID) {
                strWSForBus = UIUtil.isNullOrEmpty(baseWorkSpace) ? context.createWorkspace() : baseWorkSpace;
                DateTimeFormatter millSecFormatter = ISODateTimeFormat.basicDateTimeNoMillis();
                File fWorkspace = new File(new StringBuilder(strWSForBus).append(java.io.File.separator)
                                                .append(millSecFormatter.print(new DateTime())).append("-").append(busOID).toString());
                if(!fWorkspace.exists()){
                    fWorkspace.mkdirs();
                }
                strWSForBus = fWorkspace.getAbsolutePath();
            }
        }catch(MatrixException me){
            log.error(me.getLocalizedMessage(), me);
        }catch(Exception e){
            log.error(e.getLocalizedMessage(), e);
        }
        return strWSForBus;
    }
    
    /**
     * Method to find the specified font in system & returns the ttf font file if found.
     * @param font Font name
     * @return ttf font file
     * @throws Exception
     */
    public static File getFontFile(Font font) throws Exception {
        File fontFile = null;
        try{
            FontFileFinder ffFinder = new FontFileFinder();
            List<URI> lFontURI = ffFinder.find();
            for (URI uri : lFontURI) {
                fontFile =  new File(uri);
                if (font.getFontName().equalsIgnoreCase(fontFile.getName())) {
                    break;
                }
                fontFile = null;
            }
        }catch(Exception e){
            log.error(e.getLocalizedMessage(), e);
        }
        return fontFile;
    }
    
    /**
     * Method to set specified font for all cells of the specified table
     * @param table BaseTable to set the font
     * @param font Font to set
     * @throws Exception
     */
    public static void setFont(BaseTable table, PDFont font) throws Exception{
        try{
            if(table == null || font == null){
                return;
            }
            List<Row<PDPage>> lRows= table.getRows();
            for (Row<PDPage> row : lRows) {
                List<Cell<PDPage>> lCells = row.getCells();
                for (Cell<PDPage> cell : lCells) {
                    if(cell instanceof ImageCell){
                        continue;
                    } else if(cell instanceof TableCell){
                        continue;
                    } else {
                        cell.setFont(font);
                    }
                }
            }
        }catch(Exception e){
            log.error(e.getLocalizedMessage(), e);
        }
    }
    
    /**
     * To build RPE key as desierd.. Currrently the combination is RPEKey+"-"+BUSOID
     * @param rpeKey
     * @param busOID
     * @return
     * @throws Exception
     */
    public static String getRPEForBUS(String rpeKey, String busOID) throws Exception {
        String strRPE = DomainConstants.EMPTY_STRING;
        try{
            strRPE = new StringBuilder(rpeKey).append(TigerConstants.SEPERATOR_MINUS).append(busOID).toString();
        }catch(Exception e){
            log.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
        return strRPE;
    }
    
    /**
     * Method to concat string values
     * @param base
     * @param args
     * @return
     * @throws Exception
     */
    public static String concateStrings(String base, String... args) throws Exception {
        try{
            StringBuilder sb = new StringBuilder(base);
            for (int i = 0; i < args.length; i++){
                sb.append(args[i]);
            }
            base = sb.toString();
        }catch(Exception e){
            log.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
        return base;
    }
    
    /**
     * This method used to replace And Word Wrap
     * @param context
     * @param strConvertedValue
     * @throws Exception
     * @author PSI
     */
    public static String replaceAndWordWrap(Context context, String strConvertedValue, int iWordLength) throws Exception {
        try {
            if (UIUtil.isNotNullAndNotEmpty(strConvertedValue)) {
                strConvertedValue = strConvertedValue.replaceAll("_", "_ ");
                strConvertedValue = strConvertedValue.replaceAll("-", "- ");
                strConvertedValue = strConvertedValue.replaceAll("/", "/ ");
                strConvertedValue = WordUtils.wrap(strConvertedValue, iWordLength, "&lt;br/&gt;", true);
                strConvertedValue = strConvertedValue.replaceAll("_ ", "_");
                strConvertedValue = strConvertedValue.replaceAll("- ", "-");
                strConvertedValue = strConvertedValue.replaceAll("/ ", "/");
            } else {
                strConvertedValue = DomainConstants.EMPTY_STRING;
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
        return strConvertedValue;
    }

    /**
     * This method used to replace Word Chars for pdf
     * @param context
     * @param strConvertedValue
     * @throws Exception
     * @author PSI
     */
    public static String replaceWordCharsForPDF(Context context, String strConvertedValue)
        throws Exception
        {
          try
          {
            if (UIUtil.isNotNullAndNotEmpty(strConvertedValue)) {
                strConvertedValue = strConvertedValue.replaceAll("&lt;br/&gt;", "<br>");
                strConvertedValue = strConvertedValue.replaceAll("&#x9;", " ");
                strConvertedValue = strConvertedValue.replaceAll("&#xa;", ".");
                strConvertedValue = strConvertedValue.replaceAll("&#xd;", " ");
                strConvertedValue = strConvertedValue.replaceAll("&#x20;", " ");
                strConvertedValue = strConvertedValue.replaceAll("&#x21;", "!");
                strConvertedValue = strConvertedValue.replaceAll("&#x22;", "\"");
                strConvertedValue = strConvertedValue.replaceAll("&#x23;", "#");
                strConvertedValue = strConvertedValue.replaceAll("&#x25;", "%");
                strConvertedValue = strConvertedValue.replaceAll("&#x26;", "&");
                strConvertedValue = strConvertedValue.replaceAll("&#x27;", "'");
                strConvertedValue = strConvertedValue.replaceAll("&#x28;", "(");
                strConvertedValue = strConvertedValue.replaceAll("&#x29;", ")");
                strConvertedValue = strConvertedValue.replaceAll("&#x2a;", "*");
                strConvertedValue = strConvertedValue.replaceAll("&#x2b;", "+");
                strConvertedValue = strConvertedValue.replaceAll("&#x2c;", ",");
                strConvertedValue = strConvertedValue.replaceAll("&#x2d;", "-");
                strConvertedValue = strConvertedValue.replaceAll("&#x2e;", ".");
                strConvertedValue = strConvertedValue.replaceAll("&#x2f;", "/");
                strConvertedValue = strConvertedValue.replaceAll("&#x3a;", ":");
                strConvertedValue = strConvertedValue.replaceAll("&#x3b;", ";");
                strConvertedValue = strConvertedValue.replaceAll("&#x3c;", "<");
                strConvertedValue = strConvertedValue.replaceAll("&#x3d;", "=");
                strConvertedValue = strConvertedValue.replaceAll("&#x3e;", ">");
                strConvertedValue = strConvertedValue.replaceAll("&#x3f;", "?");
                strConvertedValue = strConvertedValue.replaceAll("&amp;", "&");
                strConvertedValue = strConvertedValue.replaceAll("&gt;", ">");
                strConvertedValue = strConvertedValue.replaceAll("&lt;", "<");
                strConvertedValue = strConvertedValue.replaceAll("&#x3a;", ":");
                strConvertedValue = strConvertedValue.replaceAll("&#x40;", "@");
                strConvertedValue = strConvertedValue.replaceAll("&#x5b;", "[");
                strConvertedValue = strConvertedValue.replaceAll("&#x5d;", "]");
                strConvertedValue = strConvertedValue.replaceAll("&#x5e;", "^");
                strConvertedValue = strConvertedValue.replaceAll("&#x5f;", "_");
                strConvertedValue = strConvertedValue.replaceAll("&#x60;", "`");
                strConvertedValue = strConvertedValue.replaceAll("&#x91;", "‘");
                strConvertedValue = strConvertedValue.replaceAll("&#x92;", "’");
                strConvertedValue = strConvertedValue.replaceAll("&#x93;", "“");
                strConvertedValue = strConvertedValue.replaceAll("&#x94;", "”");
                strConvertedValue = strConvertedValue.replaceAll("&#x95;", "•");
                strConvertedValue = strConvertedValue.replaceAll("&#x96;", "–");
                strConvertedValue = strConvertedValue.replaceAll("&#x97;", "—");
                strConvertedValue = strConvertedValue.replaceAll("&#x98;", "~");
                strConvertedValue = strConvertedValue.replaceAll("\t", "    ");
                strConvertedValue = strConvertedValue.replaceAll("&#x2022;", "•");
                strConvertedValue = strConvertedValue.replaceAll(newline, " ");
                
            } else{
                strConvertedValue = DomainConstants.EMPTY_STRING;
            }
          }catch (Exception e) {
              e.printStackTrace();
              throw new Exception(e.getLocalizedMessage(), e);
          }
          return strConvertedValue;
    }
    

    /**
     * This method used to replace Word Chars And Encode For XML
     * @param context
     * @param strConvertedValue
     * @throws Exception
     * @author PSI
     */
    public static String replaceWordCharsForXML(Context context, String strConvertedValue) throws Exception {
        try {
            if (UIUtil.isNotNullAndNotEmpty(strConvertedValue)) {
                // smart single quotes and apostrophe
                strConvertedValue = strConvertedValue.replaceAll("[\u2018\u2019\u201A]", "'");
                // smart double quotes
                strConvertedValue = strConvertedValue.replaceAll("[\u201C\u201D\u201E]", "\"");
                // ellipsis
                strConvertedValue = strConvertedValue.replaceAll("\u2026", "...");
                // dashes
                strConvertedValue = strConvertedValue.replaceAll("[\u2013\u2014]", "-");
                // circumflex
                strConvertedValue = strConvertedValue.replaceAll("\u02C6", "^");
                // open angle bracket
                strConvertedValue = strConvertedValue.replaceAll("\u2039", "<");
                strConvertedValue = strConvertedValue.replaceAll("<", "&lt;");
                // close angle bracket
                strConvertedValue = strConvertedValue.replaceAll("\u203A", ">");
                // spaces
                strConvertedValue = strConvertedValue.replaceAll("[\u02DC\u00A0]", " ");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e.getLocalizedMessage(), e);
        }
        return strConvertedValue;
    }
    
    /**
     * To reset all Global RPE variables specific to TB update
     * @param context
     * @param derivedOutputOID
     * @throws Exception
     */
    public static void resetGlobalRPEs(Context context, String derivedOutputOID) throws Exception {
        try {
            PropertyUtil.setGlobalRPEValue(context, getRPEForBUS(TigerConstants.RPE_KEY_SKIP_TB_GENERATION_WEB, derivedOutputOID), TigerConstants.STRING_FALSE);
            PropertyUtil.setGlobalRPEValue(context, getRPEForBUS(TigerConstants.RPE_KEY_SKIP_TB_GENERATION_NATIVE, derivedOutputOID), TigerConstants.STRING_FALSE);
            PropertyUtil.setGlobalRPEValue(context, getRPEForBUS(TigerConstants.RPE_KEY_JOB_SKIP_TB_GENERATION_WEB, derivedOutputOID), TigerConstants.STRING_FALSE);
            PropertyUtil.setGlobalRPEValue(context, getRPEForBUS(TigerConstants.RPE_KEY_SKIP_ON_BG_JOB, derivedOutputOID), TigerConstants.STRING_FALSE);
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
        }
    }
    
    /**
     * Delete PDF files other than Detail sheet PDF from DerivedOutput 
     * @param context
     * @param derivedOutput
     * @throws Exception
     */
    public static void deleteValidPDFs(Context context, String derivedOutput) throws Exception {
        try{
            StringList slPDFFiles = getValidPDFSheetsName(context, new BusinessObject(derivedOutput));
            deleteFiles(context, derivedOutput, FileFormat.PDF(), slPDFFiles);
        }catch(Exception e){
            log.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
    }
    
    /**
     * Method to get the MCAD session context for the 'Job created to update TitleBlock'
     * Also, removes the key-value pair
     * @param context
     * @param JobName
     * @return
     * @throws Exception
     */
    public static Context getMCADSessionContext(Context context, String JobName) throws Exception{
        Context mcadContext = null;
        try{
            if(!hmMCADContexts.isEmpty()){
                mcadContext = hmMCADContexts.getOrDefault(JobName, context);
                hmMCADContexts.remove(JobName);
            }else{
                mcadContext = context;
            }
        }catch(Exception e){
            log.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
        return mcadContext;
    }
    
    /**
     * Method to set the MCAD session context for the 'Job created to update TitleBlock'
     * @param context
     * @param JobName
     * @throws Exception
     */
    public static void setMCADSessionContext(Context context, String JobName) throws Exception{
        try{
            hmMCADContexts.put(JobName, context);
        }catch(Exception e){
            log.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
    }
    
    /**
     * Return List of all MCADSession contexs on which auto TB generation is in progress
     * @param context
     * @return
     * @throws Exception
     */
    public static ArrayList<Context> getMCADSessionContextsList(Context context) throws Exception{
        ArrayList<Context> alContexts = new ArrayList<>(hmMCADContexts.size());
        try{
            Iterator<String> itrKeys = hmMCADContexts.keySet().iterator();
            while (itrKeys.hasNext()) {
                alContexts.add(hmMCADContexts.get(itrKeys.next()));
            }
        }catch(Exception e){
            log.error(e.getLocalizedMessage(), e);
        }
        return alContexts;
    }
    
    /**
     * Method get States of Policy
     * @param context
     * @param policy
     * @return
     * @throws Exception
     */
    public static StringList getStates(Context context, String policy) throws Exception {
        StringList slStates = new StringList();
        if (context != null && UIUtil.isNotNullAndNotEmpty(policy)) {
            String sQuery = "print policy '$1' select state dump $2";
            try {
                String strStates = MqlUtil.mqlCommand(context, sQuery, new String[] { policy, "|" });
                slStates = StringUtil.split(strStates, "|");
            } catch (FrameworkException fe) {
                log.error(fe.getLocalizedMessage(), fe);
                throw new Exception(fe.getLocalizedMessage(), fe);
            }
        }
        return slStates;
    }
    
    /**
     * Methode to get the Active role name from the active Security Context
     * @param context
     * @return
     * @throws Exception
     */
    public static String getActiveSecurityContextRole(Context context) throws Exception {
        String strActiveRole = DomainConstants.EMPTY_STRING;
        try{
            strActiveRole = (String) FrameworkUtil.split(context.getRole().substring(5), ".").get(0);
        }catch(Exception e){
            log.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
        return strActiveRole;
    }
    
    /**
     * Methode to get the Active company name from the active Security Context
     * @param context
     * @return
     * @throws Exception
     */
    public static String getActiveSecurityContextOrganization(Context context) throws Exception {
        String strActiveOrganization = DomainConstants.EMPTY_STRING;
        try{
            strActiveOrganization = (String) FrameworkUtil.split(context.getRole().substring(5), ".").get(1);
        }catch(Exception e){
            log.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
        return strActiveOrganization;
    }
    
    /**
     * Methode to get the Active Collaborative Space name from the active Security Context
     * @param context
     * @return
     * @throws Exception
     */
    public static String getActiveSecurityContextCollaborativeSpace(Context context) throws Exception {
        String strActiveCS = DomainConstants.EMPTY_STRING;
        try{
            strActiveCS = (String) FrameworkUtil.split(context.getRole().substring(5), ".").get(2);
        }catch(Exception e){
            log.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
        return strActiveCS;
    }
}
