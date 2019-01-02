package faurecia.applet.awt;

import java.applet.Applet;
import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AppletGUI extends Panel  {
	protected Applet aParentApplet = null;
	protected Button bConfirmButton;
	protected Button bCloseButton;
	protected StatusLine bStatusLine;
	protected MessageArea mMessages;
	
	public final  Color cBackground = Color.white;
	public final  Color cForeground = Color.black;
	
	protected boolean bContinue = false;
	
	protected boolean bIsInProgress = false;
	

	
	/**
	 * constructor. The GUI attaches itself to the Applet.
	 * @param aParent
	 */
	public AppletGUI(Applet aParent) {
		aParentApplet = aParent;
        // initialize the layout
		setupLayout();
        // the default FlowLayout does not work good with a contained GridBagLayout, therefore
        // we change the layoutManager of the Applet
		aParentApplet.setLayout(new java.awt.GridLayout());
		aParentApplet.add(this);
	}
	
	
	/**
	 * initializes the layout with 
	 * <br>MessageArea
	 * <br>StatusBar
	 * <br>ConfirmationButtton, CloseButton
	 *
	 */
	public void setupLayout() {
		// set the colors:
		this.setBackground(cBackground);
		this.setForeground(cForeground);
		
	    GridBagLayout gridbag = new GridBagLayout();
	    GridBagConstraints c = new GridBagConstraints();
	    // get a new status line
		bStatusLine = new StatusLine();
		// get a new message area
		mMessages = new MessageArea();
		// get a new button
		bConfirmButton = createConfirmButton();
		bCloseButton = createCloseButton();
		
		//this.setSize(200,200);
		// set the current layout to grdibag.
		this.setLayout(gridbag);
		
		// add the MessageArea
		c.weightx=1.0;
		c.weighty=1.0;
		//c.fill=GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.NORTH;
		c.gridwidth = GridBagConstraints.REMAINDER;
		addToLayout(mMessages,gridbag,c);
		
		
		// add the statusbar
		c.fill = GridBagConstraints.HORIZONTAL;
		addToLayout(bStatusLine,gridbag,c);    	
	
		// add the Confirm button
		// make occuppy one cell of the grid
		c.gridwidth = 1;
		c.gridheight = 1;
		// don't fill the cell
		c.fill = GridBagConstraints.NONE;
		// place at EAST of cell
		c.anchor = GridBagConstraints.EAST;
		addToLayout(bConfirmButton,gridbag, c);
		
		// place at WEST of cell
		c.anchor = GridBagConstraints.WEST;
		// ad the close button at right
		addToLayout(bCloseButton,gridbag, c);	
	}
	
	/**
	 * mehtod to add components to the gridbaglayout using the given layout and constraints.
	 * @param cComponent
	 * @param gblLayout
	 * @param gbcConstraints
	 */
	protected void addToLayout(Component cComponent, GridBagLayout gblLayout, GridBagConstraints gbcConstraints) {
		gblLayout.setConstraints(cComponent,gbcConstraints);
		this.add(cComponent);
	}
	
	/**
	 * creates a new button which closes the window when pressed.
	 * @return
	 */
	protected Button createCloseButton() {
		Button bNewButton = new Button("Close");
	    // deactivate the OK button until something is to do.
		bNewButton.setEnabled(false);
		return bNewButton;
	}
	
	/**
	 * creates the button which is used to get confirmation 
	 * from the user.
	 * @return
	 */
	protected Button createConfirmButton() {
		Button bNewButton = new Button("Continue");
		// upon click, the continue variable is set to true;
		ActionListener actionListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				bContinue = true;
			}
		};		
		bNewButton.addActionListener(actionListener);
	    // deactivate the OK button until something is to do.
		bNewButton.setEnabled(false);
		return bNewButton;
	}
	


    /**
     * displays a message to the user in the main area.
     * @param sMsg
     */
    public void displayProgressMessage(String sMsg) {
    	if (bIsInProgress) {
    		mMessages.append("...Done\n" + sMsg);
    		
    	} else {
    		mMessages.append(sMsg + "...");
    	}
    	bIsInProgress = true;
    }


	/**
	 * shows a final message and lets the user close the window
	 * @param sMessage
	 */
	private void displayResultMessage(String sMessage) {
		this.mMessages.appendMessage("\n=============================================");
		this.mMessages.appendMessage(sMessage);
		this.enableClose();
	}

	/**
	 * puts the status line to "Done.", displays the result message 
	 * and lets the user exit by the "Close" button.
	 * @param sMessage
	 */
	public void displayResultAndExit(String sMessage) {
		this.bStatusLine.done();
		displayResultMessage(sMessage);
	}

	/**
	 * shows a final message and lets user close the window
	 * @param sType
	 * @param sMessage
	 * @param sButtonText
	 * @deprecated use displayResultAndExit instead
	 */
	public void displayResultMessage(String sType, String sMessage, String sButtonText) {
		// we never change the text of the buttons to make the GUI more coherent.
		//this.bConfirmButton.setLabel(sButtonText);
		this.bStatusLine.showMessage(sType);
		displayResultMessage(sMessage);
	}

	/**
	 * displays an error in the main text area, sets the statusline to "error" and 
	 * enables the close button. 
	 * @param sError
	 */
	public void displayErrorAndExit(String sError) {
		this.mMessages.appendMessage(sError);
		bStatusLine.showError();
		enableClose();
		aParentApplet.requestFocus();
	}
	
	
	/**
	 * shows a message, waits for user input. Attention, this call does not return until 
	 * user has pressed the confirmation button. This is a kind of modal action.
	 * This action does not exit the applet or close the window. After confirmation, the 
	 * code continues. 
	 * @param sType will be shown on the status line (error, warning, ...)
	 * @param sMessage will be added to the main window
	 * @param sActionText is ignored.  
	 */
	public void displayResultMessageAndContinue(String sType, String sMessage, String sActionText) {
		this.bStatusLine.showMessage(sType);
		this.bStatusLine.setBackground(StatusLine.cWarningBackground);
		this.mMessages.appendMessage(sMessage);
		this.bContinue = false;
		this.enableConfirm();	
		// wait ad infinitum or until user clicks ok.
		try {
			while (!this.bContinue) Thread.sleep(100);
		} catch (Exception e) {
			mMessages.appendMessage("interrupted.");
		}
		// when continue, then switch into ActionMode:
		startAction();
		return;
	}
	

	/**
	 * lets you specify what happens if the user executes the "close" button.
	 * @param aCloseAction
	 */
	public void setCloseActionListener(ActionListener aCloseAction) {
		this.bCloseButton.addActionListener(aCloseAction);
	}
	
    /**
	 * sets the statusbar to "working" and disables the buttons.
	 *
	 */
	public void startAction() {
		this.bStatusLine.busy();
		disableButtons();
	}
	
	/**
	 * enables the close button and disables the Confirmation button
	 *
	 */
	@SuppressWarnings("deprecation")
    public void enableClose() {
		this.bConfirmButton.disable();
		this.bCloseButton.enable();
	}
	
	/**
	 * enables the confirmation button and disables the close button.
	 *
	 */
	@SuppressWarnings("deprecation")
    public void enableConfirm() {
		this.bConfirmButton.enable();
		this.bCloseButton.disable();
	}
	
	/**
	 * disables all buttons.
	 *
	 */
	@SuppressWarnings("deprecation")
    public void disableButtons() {
		this.bCloseButton.disable();		
		this.bConfirmButton.disable();
	}
	

}

