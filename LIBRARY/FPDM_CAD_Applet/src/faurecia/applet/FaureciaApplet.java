/*
 * Creation Date : 26 juil. 04
 *
 */
package faurecia.applet;

import java.applet.Applet;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;

import faurecia.applet.awt.AppletGUI;
import fpdm.applet.cad.Parameters;

/**
 * @author fcolin<br>
 *         This class should be subclassed by all faurecia applets which do actions on the client.<br>
 *         It provides: <br>
 *         - error handling<br>
 *         - a defined GUI consisting of:<br>
 *         - main text area to show progess messages to the user<br>
 *         - status bar to show the current status<br>
 *         - Buttons to Confirm a warning or to Close after error or success.<br>
 * 
 * 
 * 
 */
public abstract class FaureciaApplet extends Applet {

    /** This is the base URL retrieved from the JSP page. eg: "http://magellan:8080/ematrix" */
    public static String MAIN_SERVER_BASE_URL = "";

    /** This is the name of servlet that will be called first by Faurecia applets. */
    public static final String FAURECIA_eMCS_SERVLET_NAME = "Faurecia_eMCSFileServlet";

    public static final String APPLICATION_NAME = "ApplicationName";

    /** Used to store the applet context. This enable the static use of method "displayProgressMessage" */
    public static FaureciaApplet thisApplet;

    /** Used to store all messages for progression. */
    // private Vector vDisplayedMessage = new Vector();

    /** Ecart entre deux lignes */
    // private static final int iDeltaYOffset = 15;

    /** Ecart entre deux lignes */
    protected String sCloseWindowURL = "engineeringcentral/closeWindow.jsp?refreshOpener=";

    /** Ecart entre deux lignes */
    protected boolean bRefreshOpenerPage = false;

    // FDPM ADD Start JFA 30/01/05 Message Log management
    // private final static String CLASSNAME = "FaureciaApplet";

    // FDPM ADD End JFA 30/01/05 Message Log management

    /** is used to close the window in modal mode */
    protected boolean bContinue = false;

    /** the GUI of the appleet */
    public AppletGUI agGUI = null;

    /** Used to set global parameters (DebugUtil.iDebugLevel & MAIN_SERVER_BASE_URL) */
    public void init() {
        try {

            // --------------------------------------------------------------------
            // Init Debugger
            // DebugUtil.initDebugger("ON".equalsIgnoreCase(this.getParameter(Parameters.DEBUG)));

            thisApplet = this;
            this.setVisible(true);

            // create a new GUI and show it in the applet.
            agGUI = new AppletGUI(this);

            // provide an actionhandler for the "Close" button to the GUI: upon click, the method closeAppletWindow() is called.
            ActionListener actionListener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    closeAppletWindow();
                }
            };
            agGUI.setCloseActionListener(actionListener);

            // ----------------------------------------------------------------
            // Define
            URL currentURL = this.getDocumentBase();
            // DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/init: currentURL  = " + currentURL.toString());
            String sMCSServerProtocol = currentURL.getProtocol();
            String sMCSServerHost = currentURL.getHost();
            int sMCSServerPort = currentURL.getPort();

            String sApplicationName = extractApplicationName(currentURL.getFile(), this.getParameter(APPLICATION_NAME));
            // DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/init: sApplicationName  = " + sApplicationName);

            MAIN_SERVER_BASE_URL = (new URL(sMCSServerProtocol, sMCSServerHost, sMCSServerPort, sApplicationName)).toString();
            Parameters.setMainServerBaseURL(MAIN_SERVER_BASE_URL);
            // DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/init: MAIN_SERVER_BASE_URL  = " + MAIN_SERVER_BASE_URL);

        } catch (Exception e) {
            handleError(e);
        }
    }

    protected String extractApplicationName(String base, String name) {
        if (name == null) {
            name = "/";
        }
        int index = base.indexOf(name);
        return base.substring(0, index + name.length() + 1);
    }

    /**
     * This method handles error so that the applet display an error message, and leaves correctly. The error mesg is put to the message area, and the status bar is put to "Error"
     * 
     * @param e
     *            : Exception to handle
     */
    public void handleError(Throwable e) {

        // DebugUtil.debug(DebugUtil.ERROR, CLASSNAME, "/handleError: Error handled : " + e.toString());
        String sMessage = e.getMessage();
        if (!Exception.class.equals(e.getClass())) {
            sMessage = e.getClass().getName() + " : " + sMessage;
        } else if (sMessage != null && sMessage.indexOf("<null>") >= 0) {
            sMessage = "Server does not respond.";
        }
        // show error and modify the status line
        agGUI.displayErrorAndExit(sMessage);
    }

    public void closeAppletWindow() {

        // ----------------
        // PB : This is not possible to call a javascript from the applet without getting message "Do you want to authorize applet to communicate with javascript ?"
        // + pb with netscape on Unix
        // // Call JAVASCRIPT method of the opener HTML page
        // JSObject win = JSObject.getWindow(this);
        // win.eval("closeApplet()");
        // ----------------

        this.displayProgressMessage("Job is finished.");
        try {
            if (sCloseWindowURL.endsWith("refreshOpener=")) {
                sCloseWindowURL += String.valueOf(bRefreshOpenerPage);
            }
            URL closeWindow = new URL(MAIN_SERVER_BASE_URL + sCloseWindowURL);
            // DebugUtil.debug(DebugUtil.INFO, "PAGE REDIRECTION", closeWindow.toString());
            this.getAppletContext().showDocument(closeWindow);
        } catch (MalformedURLException e) {
        }

        // Clear all memory used by applet
        thisApplet = null;
        this.stop();
        this.destroy();

        System.gc();
        // Netscape => lance une execution Security :
        // System.exit(0);

    }

    /**
     * displays a message to the user in the main area.
     */
    public void displayProgressMessage(String sMsg) {
        agGUI.displayProgressMessage(sMsg);
    }

}
