package fpdm.applet.mbom;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Base64;

import javax.swing.JOptionPane;

import faurecia.applet.FaureciaApplet;

public class DownloadBOMToMachine extends FaureciaApplet{
	private static final long serialVersionUID = 1L;

	public void init() {
        super.init();
    }
	
	public void start() {
		try {
			agGUI.startAction();
			displayProgressMessage("Export of Graphical MBOM started...");
			
			String strDirectoryName = "";
			String strImageDirectoryName = "";

			String strDownloadPath = returnEmptyIfNull(this.getParameter("strDownloadPath"));
			String str150MBOM = returnEmptyIfNull(this.getParameter("str150MBOM"));
			String strSingleFile = returnEmptyIfNull(this.getParameter("strSingleFile"));
			String strTopParentFolder = returnEmptyIfNull(this.getParameter("strTopParentFolder"));
			String strFileName = returnEmptyIfNull(this.getParameter("strFileName"));
			String strFileData = returnEmptyIfNull(this.getParameter("strFileData"));
			String strImageData = returnEmptyIfNull(this.getParameter("strImageData"));
			String strImageName = returnEmptyIfNull(this.getParameter("strImageName"));
			
			if (!str150MBOM.isEmpty() && str150MBOM.equalsIgnoreCase("true")) {
				str150MBOM = "150MBOM";
			} else {
				str150MBOM = "100MBOM";
			}
			
			if (!strDownloadPath.isEmpty() && !strTopParentFolder.isEmpty()) {
				strDirectoryName = strDownloadPath + File.separator + strTopParentFolder + File.separator + str150MBOM;
				File directory = new File(strDirectoryName);
		        if (!directory.exists()) {
		        	directory.mkdirs();
		        }
		        
		        if (!strSingleFile.isEmpty() && strSingleFile.equalsIgnoreCase("true")) {
		        	if (!strFileData.isEmpty() && !strFileName.isEmpty()) {
		        		byte[] decodedString = Base64.getDecoder().decode(strFileData.getBytes("UTF-8"));
				        File file = new File(strDirectoryName + File.separator + strFileName);
				        
				        if (!file.exists() && !file.isDirectory()) {
				        	createExcelFile(file, decodedString);
				        	displayProgressMessage("Single MBOM downloaded...");
				        } else {
				        	int response = JOptionPane.showConfirmDialog(null, "The Export exists in " + strDirectoryName + ", do you want to overwrite", "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
				        	if (response == JOptionPane.YES_OPTION) {
				        		createExcelFile(file, decodedString);
				        		displayProgressMessage("Single MBOM downloaded...");
				        	} else if (response == JOptionPane.NO_OPTION) {
				        		return;
				        	}
				        }
		        	}
				} else {
					if (!strFileData.isEmpty() && !strFileName.isEmpty()) {
						String strName = "";
						String strData = "";
						String[] saFileName = strFileName.split("`");
						String[] saFileData = strFileData.split("`");
						
						for (int i = 0; i < saFileData.length; i++) {
							strName = saFileName[i];
							strData = saFileData[i];
							byte[] decodedString = Base64.getDecoder().decode(strData.getBytes("UTF-8"));
							File file = new File(strDirectoryName + File.separator + strName);
							
							if (!file.exists() && !file.isDirectory()) {
					        	createExcelFile(file, decodedString);
					        } else {
					        	int response = JOptionPane.showConfirmDialog(null, "The Export exists in " + strDirectoryName + ", do you want to overwrite", "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
					        	if (response == JOptionPane.YES_OPTION) {
					        		createExcelFile(file, decodedString);
					        	}
					        }	
						}
						displayProgressMessage("Multiple MBOMs downloaded...");
					}
				}
		        
		        strImageDirectoryName = strDownloadPath + File.separator + strTopParentFolder + File.separator + "IMAGES";

		        if (!strImageName.isEmpty() && !strImageData.isEmpty()) {
		        	displayProgressMessage("Download of Images started...");
					File directoryImage = new File(strImageDirectoryName);
			        if (!directoryImage.exists()) {
			        	directoryImage.mkdirs();
			        }
			        
			        String strImageFileName = "";
					String strImageFileData = "";
					String[] saImageFileName = strImageName.split("`");
					String[] saImageFileData = strImageData.split("`");
					
					for (int i = 0; i < saImageFileData.length; i++) {
						strImageFileName = saImageFileName[i];
						strImageFileData = saImageFileData[i];
						byte[] decodedString = Base64.getDecoder().decode(strImageFileData.getBytes("UTF-8"));
						File file = new File(strImageDirectoryName + File.separator + strImageFileName);
						createExcelFile(file, decodedString);
					}
					
			        displayProgressMessage("Download of Images completed...");
					displayProgressMessage("Export of Graphical MBOM completed...");
		        } else {
		        	displayProgressMessage("No Images are downloaded for the selected MBOMs due to unavailabilty of CAD BOM...");
					displayProgressMessage("Export of Graphical MBOM completed...");
		        }
                agGUI.enableClose();
			}     
		} catch (Throwable e) {
		    e.printStackTrace();
			handleError(e);
        } 
	}
	
	private static void createExcelFile(File file, byte[] byteData) throws Exception {
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