package faurecia.applet.fpdm;

import java.awt.Button;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import netscape.security.PrivilegeManager;

import com.ms.security.PermissionID;
import com.ms.security.PolicyEngine;

import faurecia.applet.FaureciaApplet;
import faurecia.applet.efcs.Checkiner;
import faurecia.applet.efcs.Checkouter;
import faurecia.util.DebugUtil;
import faurecia.util.Local_OS_Information;

/**
 * 
 * Applet that should be able to make checkin and checkout by <br>
 * discussing with a servlet. The original servlet was StServletCheckInOut.<br>
 * <br>
 * This class just retrieve the parameter given to the applet, <br>
 * and call CheckFactory to have an abstract object Check whose type will depend<br>
 * on the value of the parameters.<br>
 * <br>
 * Then it call the abstract method doProcess that will do a checkin or a checkout.<br>
 * <br>
 * 
 * @author rinero
 */
public class FPDMAppletCheckInOut extends FaureciaApplet {

    public static boolean DEBUG_MODE = false;

    public static String LANGUAGE = "";

    private void error(String error) {
        displayDialog("Error!", error);
        debug(error);
        /*
         * URL currentURL = this.getDocumentBase(); String urlString = currentURL.getFile(); int index = urlString.indexOf("/", 1); String applicationName = urlString.substring(0,index); URL
         * closeWindow = new URL(currentURL.getProtocol(),currentURL.getHost(),currentURL.getPort(), applicationName + "/engineeringcentral/closeWindow.jsp");
         * this.getAppletContext().showDocument(closeWindow);
         */
    }

    /**
     * method start of the applet. This method will be called by the navigator after the init method.<br>
     * In this applet, the init method is not defined. Thus this method will be the first called.<br>
     * 
     * First, this method set the privilege to be able to access to the local disc, <br>
     * then retrieve parameters, make some treatment to set them to null or to<br>
     * retrieve boolean value, and finally call the CheckFactory and the Check.doProcess();<br>
     * 
     */
    public void start() {
        boolean isNetscape = Local_OS_Information.isNetscape();
        Parameters myParameter = new Parameters();

        try {
            // get the value for the debug Flag
            myParameter.debug = this.getParameter(Parameters.DEBUG);
            if ("ON".equalsIgnoreCase(myParameter.debug)) {
                DEBUG_MODE = true;
            } else {
                DEBUG_MODE = false;
            }

            debug("FPDMAppletCheckInOut.start() START");
            if (isNetscape) {
                PrivilegeManager.enablePrivilege("UniversalPropertyRead");
                PrivilegeManager.enablePrivilege("UniversalExecAccess");
                PrivilegeManager.enablePrivilege("UniversalFileAccess");
                PrivilegeManager.enablePrivilege("UniversalConnect");
            } else {
                PolicyEngine.assertPermission(PermissionID.FILEIO);
                PolicyEngine.assertPermission(PermissionID.NETIO);
                PolicyEngine.assertPermission(PermissionID.EXEC);
            }

            myParameter.inOrout = this.getParameter(Parameters.IN_OUT);
            myParameter.id = this.getParameter(Parameters.OBJECT_ID);
            myParameter.hostName = this.getParameter(Parameters.HOST_NAME);
            myParameter.hostPort = this.getParameter(Parameters.HOST_PORT);
            myParameter.servletName = this.getParameter(Parameters.SERVLET_NAME);
            myParameter.localRepertory = this.getParameter(Parameters.LOCAL_REPERTORY);
            myParameter.serverRepertory = this.getParameter(Parameters.SERVER_REPERTORY);
            myParameter.format = this.getParameter(Parameters.FORMAT);
            myParameter.eMCSServletPath = getParameter(Parameters.EMCSSERVLETPATH);
            myParameter.sessionId = this.getParameter(Parameters.SESSION_ID);
            myParameter.sessionName = this.getParameter(Parameters.SESSION_NAME);
            myParameter.fileName = this.getParameter(Parameters.FILENAME);
            myParameter.busType = this.getParameter(Parameters.BUSTYPE);
            myParameter.busName = this.getParameter(Parameters.BUSNAME);
            myParameter.busRevision = this.getParameter(Parameters.BUSREVISION);

            myParameter.formatParameters();
            myParameter.checkParameters();

            boolean isCheckin = false;

            if ("checkin".equalsIgnoreCase(myParameter.inOrout)) {
                isCheckin = true;
            }

            // Format the information passed in parameter
            DebugUtil.debug(DebugUtil.DEBUG, "myParameter " + myParameter.toString());

            CheckFactory.createCheck(isCheckin, myParameter);
            debug("FPDMAppletCheckInOut.start : After createCheck");

            if ("checkout".equalsIgnoreCase(myParameter.inOrout)) {
                debug("FPDMAppletCheckInOut.start : OPTION is checkout");

                File localDirectory = new File(myParameter.localRepertory);

                // Check that local directory exists, is a directory and can be filled by user
                if (!localDirectory.exists() || !localDirectory.isDirectory() || !localDirectory.canWrite()) {
                    throw new IOException("Local directory " + localDirectory.getAbsolutePath() + " does not exists or is not writable.");
                }

                Checkouter.checkoutAndLockFile(myParameter.localRepertory, myParameter.id, myParameter.fileName, myParameter.format, myParameter.busType, MAIN_SERVER_BASE_URL);

                // Launch the correct Application according to the file checked out
                boolean bIsApplicationOpened = Local_OS_Information.openApplication(myParameter.localRepertory, myParameter.fileName);
                if (!bIsApplicationOpened) {
                    String message = FPDMAppletCheckInOut.translateMessages("APPLET.Checkout.Successfull");
                    FPDMAppletCheckInOut.displayDialog("Information", message);
                    DebugUtil.debug(DebugUtil.DEBUG, "Checkout.doProcess() END " + message);
                }

                debug("FPDMAppletCheckInOut.start : checkout processed");

            } else if ("checkoutnolock".equalsIgnoreCase(myParameter.inOrout)) {
                debug("FPDMAppletCheckInOut.start : OPTION is checkout");

                File localDirectory = new File(myParameter.localRepertory);

                // Check that local directory exists, is a directory and can be filled by user
                if (!localDirectory.exists() || !localDirectory.isDirectory() || !localDirectory.canWrite()) {
                    throw new IOException("Local directory " + localDirectory.getAbsolutePath() + " does not exists or is not writable.");
                }

                Checkouter.checkoutFile(myParameter.localRepertory, myParameter.id, myParameter.fileName, myParameter.format, false, "", myParameter.busType, MAIN_SERVER_BASE_URL);

                // Launch the correct Application according to the file checked out
                boolean bIsApplicationOpened = Local_OS_Information.openApplication(myParameter.localRepertory, myParameter.fileName);
                if (!bIsApplicationOpened) {
                    String message = FPDMAppletCheckInOut.translateMessages("APPLET.Checkout.Successfull");
                    FPDMAppletCheckInOut.displayDialog("Information", message);
                    DebugUtil.debug(DebugUtil.DEBUG, "Checkout.doProcess() END " + message);
                }

                debug("FPDMAppletCheckInOut.start : checkout processed");

            } else if ("checkin".equalsIgnoreCase(myParameter.inOrout)) {
                debug("FPDMAppletCheckInOut.start  : OPTION is checkin");
                File fileToCheckin = new File(myParameter.localRepertory, myParameter.fileName);

                // Check that file exists, is a file and can be read by user
                if (!fileToCheckin.exists() || !fileToCheckin.isFile() || !fileToCheckin.canRead()) {
                    throw new IOException("File " + fileToCheckin.getAbsolutePath() + " does not exists or can not be accessed.");
                }

                debug("FPDMAppletCheckInOut.startmyParameter.format: " + myParameter.format);
                Checkiner.checkinAndUnlockFile(myParameter.localRepertory, myParameter.busType, myParameter.busName, myParameter.busRevision, myParameter.fileName, myParameter.format, true, false,
                        MAIN_SERVER_BASE_URL);
                fileToCheckin.delete();
                // LINKAGE JHS 26/05/05 ADD Start
                if (("Checking Instruction".equals(myParameter.busType)) || ("Task Instruction".equals(myParameter.busType)) || ("Method Template".equals(myParameter.busType))) {
                    String sXMLFileName = myParameter.fileName.substring(0, myParameter.fileName.lastIndexOf("."));
                    sXMLFileName += ".xml";
                    File fileXMLToDelete = new File(myParameter.localRepertory, sXMLFileName);
                    if (fileXMLToDelete.exists() && fileXMLToDelete.isFile() && fileXMLToDelete.canRead()) {
                        fileXMLToDelete.delete();
                    }
                }
                // LINKAGE JHS 26/05/05 ADD End
                String message = translateMessages("APPLET.Checkin.Successfull");
                FPDMAppletCheckInOut.displayDialog("Information", message);
                DebugUtil.debug(DebugUtil.DEBUG, "Checkin.doProcess() END : " + message);
            }

            debug("FPDMAppletCheckInOut.start : before getDocumentBase()");
            URL currentURL = this.getDocumentBase();
            String urlString = currentURL.getFile();
            debug("FPDMAppletCheckInOut.start : urlString:" + urlString);
            int index = urlString.indexOf("/", 1);
            debug("index:" + index);
            String applicationName = urlString.substring(0, index);
            debug("FPDMAppletCheckInOut.start : applicationName:" + applicationName);

            bRefreshOpenerPage = true;
            this.closeAppletWindow();
        } catch (Exception e) {
            debug("FPDMAppletCheckInOut.start() EXECEPTION e.toString()");
            error(e.getMessage());
            e.printStackTrace();
        }
        debug("FPDMAppletCheckInOut.start() END");

    }

    /**
     * Method that display a message to the user in a dialog box.
     * 
     */
    @SuppressWarnings("deprecation")
    public static void displayDialog(String title, String message) {
        final Dialog d = new Dialog(new Frame(), title, true);
        d.addWindowListener(new AppletWindowListener(d));
        if (message.charAt(message.length() - 1) == '\n') {
            message = message.substring(0, message.length() - 1);
        }
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraint = new GridBagConstraints();
        constraint.insets.top = 15;
        constraint.insets.bottom = 15;

        constraint.anchor = GridBagConstraints.CENTER;
        constraint.fill = GridBagConstraints.NONE;
        constraint.gridwidth = GridBagConstraints.REMAINDER;
        Label l = new Label(message);
        layout.setConstraints(l, constraint);

        d.setLayout(layout);
        d.add(l);

        constraint.anchor = GridBagConstraints.CENTER;
        constraint.fill = GridBagConstraints.NONE;
        constraint.gridwidth = GridBagConstraints.REMAINDER;
        Button ok = new Button("OK");
        ok.setActionCommand("OK");
        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                d.dispose();
            }
        });
        constraint.ipadx = 50;
        constraint.ipady = 5;
        layout.setConstraints(ok, constraint);
        d.add(ok);

        d.pack();
        Rectangle rec = d.getBounds();
        double width = rec.width;
        double heigth = rec.height;

        if (width < 300) {
            width = 300;
        }
        if (heigth < 200) {
            heigth = 200;
        }
        // d.setSize((int) width, (int) heigth);
        d.setBounds(300, 200, (int) width, (int) heigth);
        d.show();
    }

    /**
     * Method that print debug information on the standart output.<br>
     * This method print information only if the applet is in a debug mode, it means<br>
     * that a parameter is passed to it: debug=ON.<br>
     * @param message : The message to print<br>
     */
    public static void debug(String message) {
        if (DEBUG_MODE) {
            System.out.println(message);
        }
    }

    public static String constructURL(String sHost, String sPort, String sServletPath) throws IOException {
        StringBuffer sRetCode = new StringBuffer("http://");
        sRetCode.append(sHost);
        if (sPort != null && !"".equals(sPort))
            sRetCode.append(":" + sPort);
        sRetCode.append("/" + sServletPath);

        return sRetCode.toString();
    }

    public static String translateMessages(String sMessageName) {

        String sResult = "";
        if ("French".equals(LANGUAGE)) {
            if ("APPLET.Checkin.Successfull".equals(sMessageName)) {
                sResult = "Le Checkin s'est d�roul� correctement";
            } else if ("APPLET.Checkout.Successfull".equals(sMessageName)) {
                sResult = "Le Checkout s'est d�roul� correctement";
            } else if ("Parameter.inOut.NotCorrect".equals(sMessageName)) {
                sResult = "The parameter inOut was not filled properly!";
            } else if ("Parameter.id.NotCorrect".equals(sMessageName)) {
                sResult = "The parameter inOut was not filled properly!";
            } else if ("Parameter.fileName.NotCorrect".equals(sMessageName)) {
                sResult = "The parameter inOut was not filled properly!";
            } else if ("Parameter.busType.NotCorrect".equals(sMessageName)) {
                sResult = "The parameter inOut was not filled properly!";
            } else if ("Parameter.busName.NotCorrect".equals(sMessageName)) {
                sResult = "The parameter inOut was not filled properly!";
            } else if ("Parameter.busRevision.NotCorrect".equals(sMessageName)) {
                sResult = "The parameter inOut was not filled properly!";
            } else if ("Parameter.sessionId.NotCorrect".equals(sMessageName)) {
                sResult = "The parameter inOut was not filled properly!";
            } else if ("Parameter.hostName.NotCorrect".equals(sMessageName)) {
                sResult = "The parameter inOut was not filled properly!";
            } else if ("Parameter.servletName.NotCorrect".equals(sMessageName)) {
                sResult = "The parameter inOut was not filled properly!";
            } else if ("Parameter.localRepertory.NotCorrect".equals(sMessageName)) {
                sResult = "The parameter inOut was not filled properly!";
            } else if ("Parameter.sessionName.NotCorrect".equals(sMessageName)) {
                sResult = "The parameter inOut was not filled properly!";
            }
        } else if ("German".equals("sLanguage")) {
            if ("APPLET.Checkin.Successfull".equals(sMessageName)) {
                sResult = "Le Checkin s'est d�roul� correctement";
            } else if ("APPLET.Checkout.Successfull".equals(sMessageName)) {
                sResult = "Le Checkout s'est d�roul� correctement";
            } else if ("Parameter.inOut.NotCorrect".equals(sMessageName)) {
                sResult = "The parameter inOut was not filled properly!";
            } else if ("Parameter.id.NotCorrect".equals(sMessageName)) {
                sResult = "The parameter inOut was not filled properly!";
            } else if ("Parameter.fileName.NotCorrect".equals(sMessageName)) {
                sResult = "The parameter inOut was not filled properly!";
            } else if ("Parameter.busType.NotCorrect".equals(sMessageName)) {
                sResult = "The parameter inOut was not filled properly!";
            } else if ("Parameter.busName.NotCorrect".equals(sMessageName)) {
                sResult = "The parameter inOut was not filled properly!";
            } else if ("Parameter.busRevision.NotCorrect".equals(sMessageName)) {
                sResult = "The parameter inOut was not filled properly!";
            } else if ("Parameter.sessionId.NotCorrect".equals(sMessageName)) {
                sResult = "The parameter inOut was not filled properly!";
            } else if ("Parameter.hostName.NotCorrect".equals(sMessageName)) {
                sResult = "The parameter inOut was not filled properly!";
            } else if ("Parameter.servletName.NotCorrect".equals(sMessageName)) {
                sResult = "The parameter inOut was not filled properly!";
            } else if ("Parameter.localRepertory.NotCorrect".equals(sMessageName)) {
                sResult = "The parameter inOut was not filled properly!";
            } else if ("Parameter.sessionName.NotCorrect".equals(sMessageName)) {
                sResult = "The parameter inOut was not filled properly!";
            }
        } else {
            if ("APPLET.Checkin.Successfull".equals(sMessageName)) {
                sResult = "Checkin done correctly";
            } else if ("APPLET.Checkout.Successfull".equals(sMessageName)) {
                sResult = "Checkout done correctly";
            } else if ("Parameter.inOut.NotCorrect".equals(sMessageName)) {
                sResult = "The parameter inOut was not filled properly!";
            } else if ("Parameter.id.NotCorrect".equals(sMessageName)) {
                sResult = "The parameter id was not filled properly!";
            } else if ("Parameter.fileName.NotCorrect".equals(sMessageName)) {
                sResult = "The parameter fileName was not filled properly!";
            } else if ("Parameter.busType.NotCorrect".equals(sMessageName)) {
                sResult = "The parameter busType was not filled properly!";
            } else if ("Parameter.busName.NotCorrect".equals(sMessageName)) {
                sResult = "The parameter busName was not filled properly!";
            } else if ("Parameter.busRevision.NotCorrect".equals(sMessageName)) {
                sResult = "The parameter busRevision was not filled properly!";
            } else if ("Parameter.sessionId.NotCorrect".equals(sMessageName)) {
                sResult = "The parameter sessionId was not filled properly!";
            } else if ("Parameter.hostName.NotCorrect".equals(sMessageName)) {
                sResult = "The parameter hostName was not filled properly!";
            } else if ("Parameter.servletName.NotCorrect".equals(sMessageName)) {
                sResult = "The parameter servletName was not filled properly!";
            } else if ("Parameter.localRepertory.NotCorrect".equals(sMessageName)) {
                sResult = "The parameter localRepertory was not filled properly!";
            } else if ("Parameter.sessionName.NotCorrect".equals(sMessageName)) {
                sResult = "The parameter sessionName was not filled properly!";
            } else {
                sResult = sMessageName;
            }
        }

        return sResult;
    }

    public String getPrintCommand(String commandBeginning, String filePath, String nbcopy, String scale, String color) {
        StringBuffer result = new StringBuffer(commandBeginning);
        result.append(" -f ");
        result.append(filePath);
        result.append(" -n ");
        result.append(nbcopy);
        result.append(" -e ");
        result.append(scale);
        result.append(" -c ");
        result.append(color);
        return result.toString();

    }

}
