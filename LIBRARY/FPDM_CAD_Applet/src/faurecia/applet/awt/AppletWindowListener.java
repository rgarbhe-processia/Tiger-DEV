package faurecia.applet.awt;

import java.awt.Dialog;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;


/**
 * @author rinero
 *
 */
public class AppletWindowListener implements WindowListener {
	private Dialog d;

	public AppletWindowListener(Dialog d) {
		this.d = d;
	}


	public void windowOpened(WindowEvent arg0) {
	}


	public void windowClosing(WindowEvent arg0) {
        d.setVisible(false);
		d.dispose();
	}


	public void windowClosed(WindowEvent arg0) {

	}


	public void windowIconified(WindowEvent arg0) {


	}

	public void windowDeiconified(WindowEvent arg0) {


	}


	public void windowActivated(WindowEvent arg0) {


	}


	public void windowDeactivated(WindowEvent arg0) {

	}

}
