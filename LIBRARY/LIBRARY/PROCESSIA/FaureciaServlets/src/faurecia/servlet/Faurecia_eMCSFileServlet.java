package faurecia.servlet;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.matrixone.fcs.common.Resources;
import com.matrixone.fcs.fcs.FcsServlet;
import com.matrixone.fcs.http.HttpCheckout;
import com.matrixone.servlet.Framework;

import faurecia.util.AppletServletCommunication;
import faurecia.util.BusObject;
import matrix.db.TicketWrapper;

@WebServlet(name = "Faurecia_eMCSFileServlet", urlPatterns = { "/Faurecia_eMCSFileServlet/getCheckoutTicket" })
public class Faurecia_eMCSFileServlet extends FcsServlet implements Servlet {

    public Faurecia_eMCSFileServlet() {
    }

    public void init(ServletConfig servletconfig) throws ServletException {
        System.out.println("/service: debugStr =entry ");
        Resources.setServletContext(servletconfig.getServletContext());
    }

    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(true);
        System.out.println("/service: debugStr =entry ");
        try {
            matrix.db.Context context = Framework.getFrameContext(session);
            if (context == null) {
                throw new Exception("The context cannot be found. You are not logged in.");
            }

            Object[] a_TempObj = AppletServletCommunication.readInput(request.getInputStream());
            BusObject[] a_bo = new BusObject[a_TempObj.length];
            for (int i = 0; i < a_TempObj.length; i++) {
                a_bo[i] = (BusObject) a_TempObj[i];
            }

            for (int iCpt = 0; iCpt < a_bo.length; iCpt++) {
                String sObjId = a_bo[iCpt].getId();
                String sFileName = a_bo[iCpt].getFileName();
                String sFormat = a_bo[iCpt].getFormat();
                boolean bLock = a_bo[iCpt].isBLock();

                String[] objIds = new String[1];
                String[] fileNames = new String[1];
                String[] formats = new String[1];
                String[] locks = new String[1];
                String[] paths = new String[1];

                objIds[0] = sObjId;
                fileNames[0] = sFileName;
                formats[0] = sFormat;
                locks[0] = String.valueOf(bLock);
                paths[0] = "C:\\TEMP";

                System.out.println("/getCheckoutTicket: sObjId : " + sObjId);
                System.out.println("/getCheckoutTicket: sFileName : " + sFileName);
                System.out.println("/getCheckoutTicket: sFormat : " + sFormat);
                System.out.println("/getCheckoutTicket: bLock : " + bLock);
                System.out.println("/getCheckoutTicket: context : " + context);
                System.out.println("/getCheckoutTicket: Before getting ticket");

                TicketWrapper ticket = HttpCheckout.doIt(context, objIds, fileNames, formats, locks, paths, false, "", "", request, response);

                System.out.println("/getCheckoutTicket: ticket has been retrieved");

                String sTicket = ticket.getExportString();
                System.out.println("sTicket   :::: " + sTicket);

                String ftaAction = ticket.getActionURL();
                System.out.println("/getCheckoutTicket: ftaAction  :::: " + ftaAction);

                a_bo[iCpt].setJobTicket(sTicket);
                a_bo[iCpt].setFCSServletURL(ftaAction);
            }

            AppletServletCommunication.sendOutput(response.getOutputStream(), a_bo);

            return;
        } catch (Exception exception) {
            exception.printStackTrace();
            handleError(response, exception);
        }
    }

    private void handleError(HttpServletResponse response, Exception exception) throws IOException, ServletException {

        Exception[] a_Objects = new Exception[1];
        a_Objects[0] = exception;
        try {
            AppletServletCommunication.sendOutput(response.getOutputStream(), a_Objects);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
