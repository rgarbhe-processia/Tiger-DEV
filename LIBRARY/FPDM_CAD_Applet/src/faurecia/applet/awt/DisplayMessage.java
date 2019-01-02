
package faurecia.applet.awt;

import java.awt.Button;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * @author rinero
 * @deprecated
 * 
 * Do not use this modal Dialog. It is not good GUI to have modal dialogs on top of modal dialogs.
 * 
 * This class enable to show message in Dialog boxes. The Dialog just show a sub title
 * that indicate the type of message, then some sentences,
 * manage the carriage return and have 1 Button that
 * close the Dialog.
 * It is convenient to give information to the user about the current process: errors,
 * warnings, informations, etc.
 */


public class DisplayMessage extends Dialog
    implements ActionListener
{


    /**
     * The constructor is the main method of the class: This create the dialog box and show it
     * to the user. New Frame is created.
     * @param s_title The title of the dialog box window
     * @param s_error The message to display in the dialog box
     * @param subTitle The subtitle that indicate the type of message (Info, warning, error, or whatever...)
     * @param buttonText Text on the button
     */
    public DisplayMessage(String s_title, String s_error, String subTitle, String buttonText)
    {
       this(new Frame(), s_title, s_error, subTitle, buttonText);
    }

    public DisplayMessage(Frame frame, String s_title, String s_error, String subTitle, String buttonText){
        this(frame, s_title, s_error, subTitle, buttonText, false);
    }

 	/**
     * The constructor is the main method of the class: This create the dialog box and show it
     * to the user.
	 * @param frame The main frame. It is used to know where to put this dialog box on the screen
	 * @param s_title The title of the dialog box window
	 * @param s_error The message to display in the dialog box
	 * @param subTitle The subtitle that indicate the type of message (Info, warning, error, or whatever...)
	 * @param buttonText Text on the button
	 */
    public DisplayMessage(Frame frame, String s_title, String s_error, String subTitle, String buttonText, boolean displayNewLineChar)
    {
        super(frame, s_title, true);
		this.OK = buttonText;
        StringTokenizer st = new StringTokenizer(s_error);
        int ii = 0;
        String s_temp = "";
        Vector<String> vv = new Vector<String>(10);
        while(st.hasMoreTokens())
        {
            s_temp = s_temp + " " + st.nextToken();
            if(s_temp.length() > 75 || (displayNewLineChar && s_temp.endsWith("\\n")))
            {
                if(s_temp.endsWith("\\n")){
                    s_temp = s_temp.substring(0,s_temp.length()-2);
                }
                vv.addElement(s_temp);
                s_temp = "";
                ii++;
            }
        }
        if(s_temp.length() > 0)
            vv.addElement(s_temp);
        setLayout(new GridLayout(ii + 3, 1, 400, 5));
		add(new Label(subTitle));
       for(int i = 0; i < vv.size(); i++)
            add(new Label((String)vv.elementAt(i)));

        addOKCancelPanel();
        createFrame();
        pack();
        setVisible(true);
    }

    void addOKCancelPanel()
    {
        Panel p = new Panel();
        p.setLayout(new FlowLayout());
        createButtons(p);
        add(p);
    }

    void createButtons(Panel p)
    {
        p.add(ok = new Button(OK));
        ok.addActionListener(this);
    }

    void createFrame()
    {
        Dimension d = getToolkit().getScreenSize();

        setLocation(d.width / 4, d.height / 3);
    }

    /**Action performed when the user click on the dialog button.
     * The dialog box is closed.
     *
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent ae)
    {
        if(ae.getSource() == ok)
            setVisible(false);
    }

    private String OK;
    Button ok;
}
