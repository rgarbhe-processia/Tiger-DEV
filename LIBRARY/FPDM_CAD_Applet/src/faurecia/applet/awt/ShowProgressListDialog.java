package faurecia.applet.awt;

import java.awt.Button;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
//import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;





/**
 * @author rinero
 *
 *This class enable to open a dialog which show to the user the different steps
 *of the process and which one is completed.
 *The list of the steps are given in the constructor. Then each calls of nextItem() will
 *pass to the next step.
 *If some errors or warning occurs, you can show them to the user with the method
 *error() and warning(). This method use the class DisplayDialog.
 */
public class ShowProgressListDialog extends Dialog {

	private Label[] labels;
	private int index;
	private Button ok;
	//private TextArea text;

	/**
	 * The constructor create the dialog box with all the steps
	 * @param arg0 Main frame used to position the frame at the beginning
	 * @param listItem list of the steps to display
	 * @param title Title of the dialog box
	 */
	public ShowProgressListDialog(Frame arg0, String[] listItem, String title) {
		super(arg0);
		GridBagLayout grid = new GridBagLayout();
		GridBagConstraints constraints = new GridBagConstraints();

		this.setLayout(grid);
		constraints.fill = GridBagConstraints.BOTH;
		constraints.insets = new Insets(0,0,0,0);
		this.setTitle(title);
		this.setBackground(Color.white);
		Dimension d = getToolkit().getScreenSize();
		this.setLocation(d.width / 4, d.height / 3);

		constraints.gridwidth = GridBagConstraints.REMAINDER;
		Label titleLabel = new Label("Processing:");
		titleLabel.setBackground(Color.white);

		grid.setConstraints(titleLabel, constraints);
		this.add(titleLabel);

		int maxLength = 0;
		for (int i = 0; i < listItem.length; i++) {
			maxLength =  listItem[i].length() > maxLength?listItem[i].length():maxLength;
		}
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < maxLength; i++) {
			buf.append("m");
		}
		buf.append("mmmmm");

		Label whiteLine = new Label(buf.toString());
		whiteLine.setForeground(this.getBackground());
		whiteLine.setBackground(Color.white);

		constraints.gridwidth = GridBagConstraints.REMAINDER;
		grid.setConstraints(whiteLine, constraints);
		this.add(whiteLine);
		labels = new Label[listItem.length];
		String indent = "    ";
		for (int i = 0; i < listItem.length; i++) {
			labels[i] = new Label(indent + listItem[i]);
			labels[i].setForeground(Color.lightGray);
			labels[i].setBackground(Color.white);

			constraints.gridwidth = GridBagConstraints.REMAINDER;
			grid.setConstraints(labels[i], constraints);
			this.add(labels[i]);
		}
		this.ok = new Button("OK");
		Panel panelOk = new Panel();
		panelOk.add(ok);
		panelOk.setBackground(Color.white);

//		grid.addLayoutComponent("panelOk", panelOk);
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		constraints.gridheight = GridBagConstraints.REMAINDER;
		grid.setConstraints(panelOk, constraints);
		this.add(panelOk);
		ok.setEnabled(false);

		ActionListener actionListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ShowProgressListDialog.this.close();
			}
		};

		ok.addActionListener(actionListener);
		this.addWindowListener(new AppletWindowListener(this));
		updateOkButton();
		this.pack();
		this.setVisible(true);
	}


	/**
	 * This method enable to retrieve the list of steps.
	 * @return The list of steps in String array
	 */
	public String[] getListItem() {
		String[] ret = new String[this.labels.length];
		for (int i = 0; i < this.labels.length; i++) {
			ret[i] = this.labels[i].getText();
		}
		return ret;
	}

	/**
	 * This method enable to pass to the next step.
	 * At first this method have to be called to activate the first step.
	 * After the last step, this method have to be called to enable the OK button.
	 */
	public void nextItem() {
		if (this.index>0) {
			String text = this.labels[this.index-1].getText();
			text += "..........OK!";
			this.labels[this.index-1].setText(text);
		}
		if (!updateOkButton()) {
			this.labels[this.index].setForeground(Color.black);
			this.index++;
		}
	}

	private boolean updateOkButton() {
		boolean ret = false;
		if (this.index >= this.labels.length) {
			this.ok.setEnabled(true);
			ret =  true;
		}
		return ret;
	}

	/**This method permit to show an error message in a dialog box. Then when the user
	 * click OK, both dialog boxes are closed. the process abort.
	 * @param message Error message to display to the user
	 */
    @SuppressWarnings({ "deprecation", "unused" })
    public void error(String message) {
        DisplayMessage d = new DisplayMessage(null, "Error", message, "ERROR: ", "OK");
		this.close();
	}

	/**
	 * This method permit to show a warning message to the user. This will not cause the
	 * closing of the main window. The process can continue.
	 * @param message
	 */
    @SuppressWarnings({ "deprecation", "unused" })
    public void warning(String message) {
        DisplayMessage d = new DisplayMessage(null, "Warning", message, "WARNING: ", "OK");
	}


	private void close() {
		this.setVisible(false);
		this.dispose();
	}

}
