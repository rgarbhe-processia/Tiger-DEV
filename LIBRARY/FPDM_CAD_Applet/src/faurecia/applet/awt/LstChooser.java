package faurecia.applet.awt;
//import AppletCheckInOut;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import faurecia.applet.cad.AppletCheckInOut;

/**
 * @author steria
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class LstChooser extends Frame implements ActionListener {
  java.awt.List liste=new java.awt.List();
  public java.awt.List lst  = new java.awt.List(10, true);

  public LstChooser(String sTitle) {
  	super(sTitle);
  }


  public void actionPerformed(ActionEvent e)
    {
      String nom = e.getActionCommand();

      if (nom.equals("OK")) {
        AppletCheckInOut.sFiletoCheckin = lst.getSelectedItems();
      	this.dispose();
      }

    }

}
