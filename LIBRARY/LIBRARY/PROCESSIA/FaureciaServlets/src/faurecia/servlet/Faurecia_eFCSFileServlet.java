package faurecia.servlet;

import java.io.IOException;
import java.util.Iterator;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.matrixone.fcs.common.Resources;
import com.matrixone.fcs.fcs.FcsContext;
import com.matrixone.fcs.fcs.FcsServlet;
import com.matrixone.fcs.fcs.Item;

import faurecia.servlet.util.Faurecia_eFCSFileCheckout;
import faurecia.util.AppletServletCommunication;

@WebServlet(name = "Faurecia_eFCSFileServlet", urlPatterns = { "/Faurecia_eFCSFileServlet/*" })
public class Faurecia_eFCSFileServlet extends FcsServlet implements Servlet {

    public Faurecia_eFCSFileServlet() {
    }

    public void init(ServletConfig servletconfig) throws ServletException {
        try {
            Resources.setServletContext(servletconfig.getServletContext());
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        FcsContext fcscontext = new FcsContext(request, response);

        try {
            System.out.println("Start Faurecia_eFCSFileCheckout.checkout...");
            Faurecia_eFCSFileCheckout.checkout(fcscontext, request, response);
            System.out.println("Complete Faurecia_eFCSFileCheckout.checkout...");
        } catch (Exception exception) {
            // In Case of Exception, remove all the (fcs)items that are in the cache of the user
            try {
                fcscontext.getProtocol().closeProtocol();
                for (Iterator<?> iterator = fcscontext.getItems().iterator(); iterator.hasNext();) {
                    Item item = (Item) iterator.next();
                    try {
                        item.remove();
                    } catch (Exception exception2) {
                    }
                }

            } catch (Exception exception1) {
            }
            handleError(response, exception);
        } finally {
            fcscontext.dispose();
            fcscontext = null;
        }
    }

    protected void handleError(HttpServletResponse response, Exception exception) throws IOException, ServletException {
        Exception[] a_Objects = new Exception[1];
        a_Objects[0] = exception;
        try {
            AppletServletCommunication.sendOutput(response.getOutputStream(), a_Objects);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
