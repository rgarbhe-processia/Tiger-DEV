package faurecia.applet.awt;

import java.awt.TextArea;
import java.awt.Color;

/**
 * implements a message area which is not editable but where the user can copy the output
 * @author markusr
 *
 */
public class MessageArea extends TextArea{
	
	public final  Color cBackground = Color.white;
	public final  Color cForeground = Color.black;
	
	/**
	 * initialize the MessageArea
	 *
	 */
	public MessageArea() {
		// this.setSize(200,100);
		// this.setColumns(20);
		setLocation(200, 200);
		setSize(200,100);
		// not editable. Just to show text
		this.setEditable(false);
		// we use white background in the applet.
		this.setBackground(cBackground);
		this.setForeground(cForeground);
	}
	
	/**
	 * appends a message in the message area, followed by a newline characters
	 * @param sMessage
	 */
	public void appendMessage(String sMessage) {
		this.append( sMessage + "\n" );
	}
}
