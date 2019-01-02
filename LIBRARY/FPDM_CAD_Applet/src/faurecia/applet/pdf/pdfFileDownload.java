package faurecia.applet.pdf;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Base64;
import java.util.Date;
import java.text.SimpleDateFormat;

import faurecia.applet.FaureciaApplet;
import faurecia.util.StringUtil;


/**
 * @author FAURECIA
 *
 */
public class pdfFileDownload extends FaureciaApplet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String sLocalTempDirectory;
	public void init() {
		super.init();
	}

	public void start() {
		// indicate in status bar that we are starting the work
		try {
			agGUI.startAction();
			String strDirectoryName = "";
			String strFolder="PDFMergeFolder";

			String strDownloadPath = returnEmptyIfNull(this.getParameter("strDownloadPath"));
			String strFileData = returnEmptyIfNull(this.getParameter("strFileData"));
			
			strDirectoryName = strDownloadPath + File.separator + strFolder;
			File directory = new File(strDirectoryName);
			if (!strDownloadPath.isEmpty())
			{	
				if (!directory.exists()) {
					directory.mkdirs();
				}
			}

			Date now = new Date();
			SimpleDateFormat dateFormat = new SimpleDateFormat("hh mm ss");
			String time = (dateFormat.format(now)).replace( " " , "-" );
			
			byte[] decodedString = Base64.getDecoder().decode(strFileData.getBytes("UTF-8"));
			File file = new File(strDirectoryName + File.separator + "All_MergedPDF_" + time + ".zip");

			createPDFFile(file, decodedString);

			sLocalTempDirectory = strDirectoryName;
			displayProgressMessage("File is downloaded at :" + sLocalTempDirectory);

		} catch (Throwable e) {
			handleError(e);
			//this.closeAppletWindow();
		} 
	}

	private static void createPDFFile(File file, byte[] byteData) throws Exception {
		FileOutputStream fos = new FileOutputStream(file);
		fos.write(byteData);
		fos.close();
	}

	public static String returnEmptyIfNull(String sParameterValue) {
		if (sParameterValue==null) {
			return "";
		}
		return sParameterValue;
	}


}

