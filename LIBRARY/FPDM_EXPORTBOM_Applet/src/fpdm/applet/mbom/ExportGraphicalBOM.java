package fpdm.applet.mbom;

import java.applet.Applet;

import javax.swing.JFileChooser;

public class ExportGraphicalBOM extends Applet {
	private static final long serialVersionUID = 1L;
	
	@SuppressWarnings({"unchecked", "rawtypes"})
	public String setCustomDirectoryPath() {
		String choosenFiles = null;
		
		choosenFiles = new String((String) java.security.AccessController.doPrivileged(new java.security.PrivilegedAction() {public Object run() {
			String choosenFiles = new String();
			
			JFileChooser chooser = new JFileChooser();
			chooser.setCurrentDirectory(new java.io.File("."));
			chooser.setDialogTitle("Select folder");
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			chooser.setAcceptAllFileFilterUsed(false);

		    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
		    	choosenFiles = chooser.getSelectedFile().getAbsolutePath();
		    }
		    
		    return choosenFiles;
		}}));
		
       return choosenFiles;
	}
}