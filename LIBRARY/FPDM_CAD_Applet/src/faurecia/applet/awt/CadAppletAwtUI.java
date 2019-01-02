/**
 *
 */
package faurecia.applet.awt;

import java.applet.Applet;

import faurecia.applet.FaureciaApplet;
import fpdm.applet.cad.CadAppletUI;

/**
 * @author lebasn
 *
 */
public class CadAppletAwtUI implements CadAppletUI {
	protected Applet applet = null;

    public void setApplet(Applet applet) {
        this.applet = applet;
    }
    @SuppressWarnings("deprecation")
    public void displayMessage(String s_title, String s_error) {
    	((FaureciaApplet)applet).agGUI.displayResultMessage(s_title, s_error, s_title);
    }
    @SuppressWarnings("deprecation")
    public void displayResultMessage(String s_title, String s_error, boolean displayNewLineChar) {
    	((FaureciaApplet)applet).agGUI.displayResultMessage(s_title, s_error, s_title);
    }
    public void displayProgressMessage(String sMsg) {
        ((FaureciaApplet)applet).agGUI.displayProgressMessage(sMsg);
    }
}
