/**
 *
 */
package fpdm.applet.cad;

import java.applet.Applet;

/**
 * @author lebasn
 *
 */
public interface CadAppletUI {
    public void setApplet(Applet applet);
    public void displayMessage(String s_title, String s_error);
    public void displayResultMessage(String s_title, String s_error, boolean displayNewLineChar);
    public void displayProgressMessage(String sMsg);
}
