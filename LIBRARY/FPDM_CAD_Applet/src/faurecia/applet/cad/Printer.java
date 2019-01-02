
package faurecia.applet.cad;
import java.awt.Button;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.StringTokenizer;

import faurecia.applet.awt.AppletWindowListener;
import faurecia.applet.awt.FrameChooser;
import faurecia.util.AppletServletCommunication;
import faurecia.util.DebugUtil;
import faurecia.util.Local_OS_Information;
import faurecia.util.StringUtil;

/**
 * @author
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>P erences>Java>Code Generation.
 */
public class Printer {

//	FDPM ADD Start JFA 30/01/05 Message Log management
	private final static String CLASSNAME = "Printer";
	//FDPM ADD End JFA 30/01/05 Message Log management
	
    public static String selectServerPrinterAndBuildCommand (String sServerURLBase) throws Exception {

        String sPrinterCommand = "";
        Object[] a_Printers = AppletServletCommunication.requestServerTask(sServerURLBase + "/getPrintersList", new Object[0]);
        if (a_Printers.length >= 1) {
            Hashtable ht_ListPrinters = (Hashtable)a_Printers[0];

            String sPrinterSelected = displayPrinterChooser("Printer selector", "Choose a printer : ", ht_ListPrinters);
            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/selectServerPrinterAndBuildCommand: sPrinterSelected " + sPrinterSelected);

            if (StringUtil.returnNullIfEmpty(sPrinterSelected) == null) {
                throw new Exception("No printer has been selected.");
            }

            if (ht_ListPrinters.containsKey(sPrinterSelected)) {
                sPrinterCommand = (String)ht_ListPrinters.get(sPrinterSelected);
            }
        }

        if (StringUtil.returnNullIfEmpty(sPrinterCommand) == null)
            throw new Exception("No command is available for this printer.");

        return sPrinterCommand;
    }

    public static String selectLocalPrinterAndBuildCommand () throws Exception {
        String PrinterIniFileDirectory = Local_OS_Information.getUnixEnv("MAGELLAN_SCRIPT");
        File printersListFile = new File(PrinterIniFileDirectory, "plt_prt.ini");
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/selectLocalPrinterAndBuildCommand: Ini file for list of printers :" + printersListFile.getAbsolutePath());

        String sPrinterCommand = "";
        Hashtable ht_ListPrinters = retrievePrintersList(printersListFile);
        String sPrinterSelected = displayPrinterChooser("Printer selector", "Choose a printer : ", ht_ListPrinters);
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/selectLocalPrinterAndBuildCommand: sPrinterSelected " + sPrinterSelected);

        if (StringUtil.returnNullIfEmpty(sPrinterSelected) == null) {
            throw new Exception("No printer has been selected.");
        }

        if (ht_ListPrinters.containsKey(sPrinterSelected)) {
            sPrinterCommand = (String)ht_ListPrinters.get(sPrinterSelected);
        }

        if (StringUtil.returnNullIfEmpty(sPrinterCommand) == null)
            throw new Exception("No command is available for this printer.");

        return sPrinterCommand;
    }


    @SuppressWarnings("unchecked")
    public static Hashtable retrievePrintersList(File iniFile) throws Exception {
    	DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/retrievePrintersList: printersListFile  = " + iniFile.getAbsolutePath());
    	DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/retrievePrintersList: Readable          = " + iniFile.canRead());
		Hashtable<Object, Object> vListOfPrinters = new Hashtable();

	    Vector<String> order = new Vector<String>();
	    vListOfPrinters.put(new Integer(0), order);

		BufferedReader bufTitleBlock = new BufferedReader(new FileReader(iniFile.getAbsolutePath()));
        //DebugUtil.debug(DebugUtil.DEBUG, "ok33");
		String sLineRead = bufTitleBlock.readLine();
		String section = "";
		while  (sLineRead != null) {
          //DebugUtil.debug (DebugUtil.DEBUG, "Line read : " + sLineRead);

	      if (sLineRead.indexOf("[") == 0) {
	        // La ligne lue est le debut d'une section a prendre en compte
	        section = sLineRead.trim();
	        section = section.substring(1,section.length() -1);
            order.addElement(section);
	      } else if ((sLineRead.indexOf("hpgl_spool=") == 0) && (!"".equals(section))) {
            vListOfPrinters.put(section, sLineRead.substring("hpgl_spool=".length()));
          }
		  sLineRead = bufTitleBlock.readLine();
		}
		bufTitleBlock.close();

 		// FPDM Add Start - JFA 20051223
		String printersList = AppletCheckInOut.preferedPrinters;

 		//DebugUtil.debug(DebugUtil.DEBUG, "printersList " + printersList);

		StringTokenizer temp = new StringTokenizer(printersList, "|");
		int i = 0;

		Vector<String> tempList = new Vector<String>();

        while(temp.hasMoreTokens()){
			String printer = temp.nextToken();
			//DebugUtil.debug (DebugUtil.DEBUG, "Modif : " + printer + ";");
			// if this printer is already in the tempList, don't display it again
			if (order.contains(printer)){
				//DebugUtil.debug (DebugUtil.DEBUG, " printersList contains : " + printer + ";");
				if (!tempList.contains(printer)){
					order.remove(printer);
					order.add(i,printer);
					i++;
					//DebugUtil.debug (DebugUtil.DEBUG, "Modif OK : " + i);
					tempList.add(printer);
				}
				else{
					//DebugUtil.debug (DebugUtil.DEBUG, " Already modified");
				}
			}
			//DebugUtil.debug (DebugUtil.DEBUG, "Not contained : " + printer + ";");
		}
		// FPDM Add End - JFA 20051223

		return vListOfPrinters;
	}

	@SuppressWarnings({ "unused", "deprecation", "unchecked" })
    public static String displayPrinterChooser(String title, String message, Hashtable ht_ListPrinters) {
		//DebugUtil.debug (DebugUtil.DEBUG, "entry in displayPrinterChooser");
		//DebugUtil.debug (DebugUtil.DEBUG, "ht_ListPrinters : "+ ht_ListPrinters);

		FrameChooser frameChooser = new FrameChooser(title);
		Dialog d = new Dialog(frameChooser, title, true);
		d.addWindowListener(new AppletWindowListener(d));
		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints constraint = new GridBagConstraints();
		constraint.insets.top = 2;
		constraint.insets.bottom= 2;
		constraint.insets.left = 0;
		constraint.insets.right = 0;
		constraint.anchor = GridBagConstraints.CENTER;
		constraint.fill = GridBagConstraints.NONE;
		constraint.gridwidth = GridBagConstraints.REMAINDER;
		Label l = new Label(message);
		layout.setConstraints(l, constraint);

		d.setLayout(layout);
		d.add(l);
		d.pack();

		int iwidth = 600;

		Vector<String> printers = (Vector) ht_ListPrinters.get(new Integer(0));

    	// FPDM Add Start - JFA 20051223

		String printersList = AppletCheckInOut.preferedPrinters;

		//DebugUtil.debug(DebugUtil.DEBUG, "printersList " + printersList);

		StringTokenizer temp = new StringTokenizer(printersList, "|");
		int i = 0;

		Vector<String> tempList = new Vector<String>();

		while(temp.hasMoreTokens()){
			String printer = temp.nextToken();
			//DebugUtil.debug (DebugUtil.DEBUG, "Modif : " + printer + ";");
			// if this printer is already in the tempList, don't display it again
			if (printers.contains(printer)){
				//DebugUtil.debug (DebugUtil.DEBUG, " printersList contains : " + printer + ";");
				if (!tempList.contains(printer)){
					printers.remove(printer);
					printers.add(i,printer);
					i++;
					//DebugUtil.debug (DebugUtil.DEBUG, "Modif OK : " + i);
					tempList.add(printer);
				}
				else{
					//DebugUtil.debug (DebugUtil.DEBUG, " Already modified");
				}
			}
			//DebugUtil.debug (DebugUtil.DEBUG, "Not contained : " + printer + ";");
		}
		// FPDM Add End - JFA 20051223

		for (Enumeration e = printers.elements() ; e.hasMoreElements() ;) {
		 String sPrinter = (String)e.nextElement();
		 int iLength = sPrinter.length() * 10;
		 if (iwidth < iLength ) {
		 	iwidth = iLength ;
		 }
		 Rectangle boundList = frameChooser.lst.getBounds();
		 frameChooser.lst.add(sPrinter);
		}
		int height = 40;
		frameChooser.lst.setSize(iwidth + 200, height);
		constraint.gridwidth = GridBagConstraints.REMAINDER;
		constraint.ipadx= 300;
		constraint.ipady= 5;
		layout.setConstraints(frameChooser.lst, constraint);
		d.add(frameChooser.lst);

		Button okButton = new Button("OK");
		okButton.setActionCommand("OK");
		okButton.addActionListener(frameChooser);

		constraint.gridwidth = GridBagConstraints.REMAINDER;
		constraint.ipadx= 50;
		constraint.ipady= 5;
		layout.setConstraints(okButton, constraint);
		 d.add(okButton);

		Rectangle rec = d.getBounds();
		double width = rec.width;
		double heigth = rec.height;

		if (width < 500) {
			width = 500;
		}
		if (heigth < 300) {
			heigth = 300;
		}
		d.setBounds(500,300, (int) width, (int) heigth);
		d.show();
		return AppletCheckInOut.sPrinterSelected;
	}


}


