package faurecia.applet.awt;

import java.awt.Color;
import java.awt.TextField;

/**
 * a status line that has the status "Error", "Idle", "Busy" and "Done"
 * 
 * @author markusr
 * 
 */
public class StatusLine extends TextField {

    public static final Color cErrorBackground = Color.red;

    public static final Color cBusyBackground = Color.lightGray;

    public static final Color cIdleBackground = Color.white;

    public static final Color cDoneBackground = Color.white;

    public static final Color cWarningBackground = Color.orange;

    public static final Color cForeground = Color.black;

    public final String sMessageError = "Error!";

    public final String sMessageIdle = "Idle.";

    public final String sMessageBusy = "Busy...";

    public final String sMessageDone = "Done.";

    public StatusLine() {
        super();
        this.setForeground(cForeground);
        this.setEditable(false);
        this.clear();
    }

    /**
     * indicate an Error
     * 
     */
    public void showError() {
        this.setBackground(cErrorBackground);
        this.setText(sMessageError);
    }

    /**
     * whow a text in the status line (should not be used)
     * 
     * @param sText
     */
    public void showMessage(String sText) {
        this.setBackground(cIdleBackground);
        this.setText(sText);
    }

    /**
     * clear the status (idle)
     * 
     */
    public void clear() {
        this.setBackground(Color.white);
        this.setText(sMessageIdle);
    }

    /**
     * show busy
     * 
     */
    public void busy() {
        this.setBackground(cBusyBackground);
        this.setText(sMessageBusy);
    }

    /**
     * show done.
     * 
     */
    public void done() {
        this.setBackground(cDoneBackground);
        this.setText(sMessageDone);
    }
}
