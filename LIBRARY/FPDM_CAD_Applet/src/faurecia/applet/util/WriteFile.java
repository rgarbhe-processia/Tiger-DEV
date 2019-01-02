package faurecia.applet.util;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import faurecia.applet.FaureciaApplet;
import faurecia.util.Local_OS_Information;

public class WriteFile extends FaureciaApplet {
    private static final long serialVersionUID = 1L;

    private static final String REGISTRY_KEY_NAME = "IN";

    private static final Pattern REGISTRY_KEY_REGEX = Pattern.compile("^\\s*(" + WriteFile.REGISTRY_KEY_NAME + ")\\s*([\\S]+)\\s*(.*)");

    private static final String REGISTRY_KEY_COMMAND = "reg query HKEY_CURRENT_USER\\Software\\titleblock\\ /f " + WriteFile.REGISTRY_KEY_NAME + " /v /e";

    public void init() {
        super.init();
    }

    public void start() {
        try {
            agGUI.startAction();
            displayProgressMessage("Export of Files started...");

            String sDownloadDirectory = returnEmptyIfNull(this.getParameter("DOWNLOAD_DIRECTORY"));
            String sAllFileName = returnEmptyIfNull(this.getParameter("FILES_NAME"));
            String sAllFileData = returnEmptyIfNull(this.getParameter("FILES_BYTE_DATA"));
            String sFileSeparator = returnEmptyIfNull(this.getParameter("FILE_SEPARATOR"));

            String sDirectoryName = null;
            if (!sDownloadDirectory.isEmpty()) {
                sDirectoryName = sDownloadDirectory;
            } else {
                sDirectoryName = getInputFolder();
            }
            displayProgressMessage("Input Directory Name = <" + sDirectoryName + ">");

            if (sDirectoryName != null && !sDirectoryName.isEmpty()) {
                File directory = new File(sDirectoryName);
                if (!directory.exists()) {
                    directory.mkdirs();
                    displayProgressMessage("The folder <" + sDirectoryName + "> has been created.");
                }
                displayProgressMessage("The file(s) will be saved on the directory <" + sDirectoryName + "> ...");

                if (!sAllFileData.isEmpty() && !sAllFileName.isEmpty()) {
                    String sFileName = "";
                    String sFileData = "";
                    String[] saFileName = sAllFileName.split(sFileSeparator);
                    String[] saFileData = sAllFileData.split(sFileSeparator);

                    for (int i = 0; i < saFileData.length; i++) {
                        sFileName = saFileName[i];
                        sFileData = saFileData[i];
                        byte[] decodedString = Base64.getDecoder().decode(sFileData.getBytes("UTF-8"));
                        File file = new File(sDirectoryName + File.separator + sFileName);

                        createFile(file, decodedString);
                        displayProgressMessage("The file <" + sFileName + "> has been created.");
                    }
                    displayProgressMessage("Download finished successfully. You can close the window.");
                } else {
                    displayProgressMessage("No file(s) saved because filename or data is empty.");
                }
                agGUI.enableClose();
            } else {
                displayProgressMessage("No file(s) saved because the IN directory is not defined or is empty.");
            }
        } catch (Throwable e) {
            e.printStackTrace();
            handleError(e);
        }
    }

    private void createFile(File file, byte[] byteData) throws Exception {
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(byteData);
        fos.close();
    }

    private String returnEmptyIfNull(String sParameterValue) {
        if (sParameterValue == null) {
            return "";
        }
        return sParameterValue;
    }

    private String getInputFolder() throws Exception {
        String sInputFolder = null;

        displayProgressMessage("/getInputFolder: REGISTRY_KEY_COMMAND = <" + WriteFile.REGISTRY_KEY_COMMAND + ">");

        String sRegResult = "";
        boolean bRegCommandCompletedSuccessfully = false;
        try {
            // execute the reg query command to fetch the software to launch
            sRegResult = Local_OS_Information.executeCommand(WriteFile.REGISTRY_KEY_COMMAND, 30000, true);
            bRegCommandCompletedSuccessfully = true;
        } catch (Exception e) {
            displayProgressMessage("/getInputFolder: the registry command handled an error <" + e.getMessage() + ">.");
        }

        if (bRegCommandCompletedSuccessfully) {
            // the command has executed without error
            displayProgressMessage("/getInputFolder: sRegResult = <" + sRegResult + ">");

            // the command will output several lines of data, which we will have to parse to fetch the JT viewer path
            String[] saRegResult = sRegResult.split("[\\r\\n]+");
            for (String sCurrentRegQueryResult : saRegResult) {
                // apply the regex on the current
                Matcher mRegResult = WriteFile.REGISTRY_KEY_REGEX.matcher(sCurrentRegQueryResult);
                if (mRegResult.matches()) {
                    // group 3 will be the path
                    String sRegKeyValue = mRegResult.group(3);
                    displayProgressMessage("/getInputFolder: sRegKeyValue = <" + sRegKeyValue + ">");

                    // A folder name has been found. Step out of the command results parsing loop.
                    if (!"".equals(sRegKeyValue)) {
                        sInputFolder = sRegKeyValue;
                        break;
                    }
                }
            }
        }
        return sInputFolder;
    }
}
