package faurecia.logging.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Level;

import faurecia.logging.Log4jConfigUtil;

/**
 * A servlet used to dynamically adjust package logging levels while an application is running. NOTE: This servlet is only aware of pre-configured packages and packages that contain objects that have
 * logged at least one message since application startup.
 * 
 * This program was adapted from the Configuration Servlet which can be found here: http://wiki.apache.org/logging-log4j/UsefulCode
 * 
 * @author goudeyj
 */
public class Log4jServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /**
     * The name of the class / package.
     */
    private static final String PARAM_CLASS = "class";

    /**
     * The logging level.
     */
    private static final String PARAM_LEVEL = "level";

    /**
     * Sort by level?
     */
    private static final String PARAM_SORTBYLEVEL = "sortbylevel";

    /**
     * All the log levels.
     */
    private static final String[] LEVELS = new String[] { Level.OFF.toString(), Level.FATAL.toString(), Level.ERROR.toString(), Level.WARN.toString(), Level.INFO.toString(), Level.DEBUG.toString(),
            Level.TRACE.toString(), Level.ALL.toString() };

    private static final String PARAM_PRINT_CONFIG = "printConfig";

    private static final String PARAM_RESET_CONFIG = "resetConfig";

    /**
     * Print the status of all current <code>Logger</code> s and an option to change their respective logging levels.
     * 
     * @param request
     *            a <code>HttpServletRequest</code> value
     * @param response
     *            a <code>HttpServletResponse</code> value
     * @exception ServletException
     *                if an error occurs
     * @exception IOException
     *                if an error occurs
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter out = response.getWriter();
        ArrayList<Map<String, String>> mlLoggers = null;
        Map<String, String> mRootLogger = null;
        /*
         * Reset configuration ?
         */
        String sResetConfig = request.getParameter(PARAM_RESET_CONFIG);
        boolean bResetConfig = ("true".equalsIgnoreCase(sResetConfig) || "yes".equalsIgnoreCase(sResetConfig));
        if (bResetConfig) {
            String sResult = "";
            sResult = Log4jConfigUtil.resetLog4j(this.getClass());
            out.println(sResult);
        }
        /*
         * Display the configuration ?
         */
        String sPrintConfig = request.getParameter(PARAM_PRINT_CONFIG);
        boolean bPrintConfig = ("true".equalsIgnoreCase(sPrintConfig) || "yes".equalsIgnoreCase(sPrintConfig));
        if (bPrintConfig) {
            String sConfig = "";
            sConfig = Log4jConfigUtil.getLog4jConfigString();
            out.println("<div style=\"font-size:9pt;font-weight:bold\">Current Log4j configuration:</div><p>" + sConfig + "</p>");
        }
        String sortByLevelParam = request.getParameter(PARAM_SORTBYLEVEL);
        boolean sortByLevel = ("true".equalsIgnoreCase(sortByLevelParam) || "yes".equalsIgnoreCase(sortByLevelParam));

        /*******************************************************************************************************************************************************************************************
         * Retrieve loggers
         ******************************************************************************************************************************************************************************************/
        mRootLogger = Log4jConfigUtil.getRootLoggerInfo();
        mlLoggers = Log4jConfigUtil.getLoggersInfos(sortByLevel);

        /***********************************************************************************************************************************************************************************************
         * Output the page
         **********************************************************************************************************************************************************************************************/
        response.setContentType("text/html");
        // print title and header
        printHeader(out, request);
        // print scripts
        out.println("<p><a href=\"" + request.getRequestURI() + "?" + PARAM_RESET_CONFIG + "=true&" + PARAM_PRINT_CONFIG + "=true\">Reload configuration file</a></p>");
        out.println("<p><a href=\"" + request.getRequestURI() + "?" + PARAM_PRINT_CONFIG + "=true\">Display current Log4j configuration</a></p>");
        out.println("<a href=\"" + request.getRequestURI() + "\">Refresh</a>");
        out.println("<table class=\"log4jtable\">");
        out.println("<thead><tr>");
        out.println("<th title=\"Logger name\">");
        out.println("<a href=\"?" + PARAM_SORTBYLEVEL + "=false\">Class</a>");
        out.println("</th>");
        out.println("<th title=\"Is logging level inherited from parent?\" style=\"text-align:right\" >*</th>");
        out.println("<th title=\"Logger level\">");
        out.println("<a href=\"?" + PARAM_SORTBYLEVEL + "=true\">Level</a>");
        out.println("</th>");
        out.println("</tr></thead>");
        out.println("<tbody>");
        int loggerNum = 0;
        // print the root Logger
        displayLogger(out, mRootLogger, loggerNum++, request);
        // print the rest of the loggers
        if (mlLoggers != null) {
            Iterator<Map<String, String>> iterator = mlLoggers.iterator();
            while (iterator.hasNext()) {
                displayLogger(out, iterator.next(), loggerNum++, request);
            }
        }
        out.println("</tbody>");
        out.println("</table>");
        out.println("<a href=\"" + request.getRequestURI() + "\">Refresh</a>");
        out.println("</body></html>");
        out.flush();
        out.close();
    }

    /**
     * Change a <code>Logger</code>'s level, then call <code>doGet</code> to refresh the page.
     * 
     * @param request
     *            a <code>HttpServletRequest</code> value
     * @param response
     *            a <code>HttpServletResponse</code> value
     * @exception ServletException
     *                if an error occurs
     * @exception IOException
     *                if an error occurs
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String className = request.getParameter(PARAM_CLASS);
        String level = request.getParameter(PARAM_LEVEL);
        // Change the logger level if posted parameters are not null
        if (className != null && level != null) {
            String message = setClass(request, className, level);
            if (message != null) {
                response.getWriter().print(message);
            }
        }
        // Refresh the page
        doGet(request, response);
    }

    /**
     * Print a Logger and its current level.
     * 
     * @param out
     *            the output writer.
     * @param logger
     *            the logger to output.
     * @param row
     *            the row number in the table this logger will appear in.
     * @param request
     *            the servlet request.
     */
    private void displayLogger(PrintWriter out, Map<String, String> mLoggerInfo, int row, HttpServletRequest request) {
        String color = null;
        String loggerName = "null";
        String loggerLevel = "null";
        String loggerEffLevel = "null";
        if (mLoggerInfo != null) {
            loggerName = mLoggerInfo.get(Log4jConfigUtil.NAME);
            loggerLevel = mLoggerInfo.get(Log4jConfigUtil.LEVEL);
            loggerEffLevel = mLoggerInfo.get(Log4jConfigUtil.EFFECTIVE_LEVEL);
        }
        color = ((row % 2) == 1) ? "even" : "odd";
        out.println("<tr class=\"" + color + "\">");
        // logger
        out.println("<td>");
        out.println(loggerName);
        out.println("</td>");
        // level inherited?
        out.println("<td style=\"text-align:right\">");
        if ("null".equals(loggerLevel)) {
            out.println("*");
        }
        out.println("</td>");
        // level and selection
        out.println("<td>");
        out.println("<form action=\"Log4jConfig\" method=\"post\">");
        printLevelSelector(out, loggerEffLevel);
        out.println("<input type=\"hidden\" name=\"" + PARAM_CLASS + "\" value=\"" + loggerName + "\">");
        out.print("<input type=\"submit\" name=\"Set\" value=\"Set \">");
        out.println("</form>");
        out.println("</td>");
        out.println("</tr>");
    }

    /**
     * Set a logger's level.
     * 
     * @param className
     *            class name of the logger to set.
     * @param level
     *            the level to set the logger to.
     * @return String return message for display.
     * @throws Throwable
     */
    private synchronized String setClass(HttpServletRequest request, String className, String level) {
        String args[] = new String[] { className, level };
        try {
            Log4jConfigUtil.setLoggerLevel(args);
        } catch (Exception e) {
            e.printStackTrace();
            return "<div style=" + Log4jConfigUtil.STYLE_INFO_ERROR + "><b>An error occured while setting level " + level + " for " + className + "</b>:</div>" + e;
        }
        return "<div style=" + Log4jConfigUtil.STYLE_INFO_OK + ">Level successfully set to <b>" + level + "</b> for <b>" + className + "</b></div>";
    }

    /**
     * Prints the page header.
     * 
     * @param out
     *            The output writer
     * @param request
     *            The request
     */
    private void printHeader(PrintWriter out, HttpServletRequest request) {
        out.println("<html><head><title>Log4J Control Console</title>");
        out.println("<style type=\"text/css\">");
        out.println("body{ background-color:#fff; }");
        out.println("body, td, th, select, input{ font-family:Verdana, Geneva, Arial, sans-serif; font-size: 8pt;}");
        out.println("select, input{ border: 1px solid #ccc;}");
        out.println("table.log4jtable, table.log4jtable td { border-collapse:collapse; border: 1px solid #ccc; ");
        out.println("white-space: nowrap; text-align: left; }");
        out.println("form { margin:0; padding:0; }");
        out.println("table.log4jtable thead tr th{ background-color: #5991A6; padding: 2px; }");
        out.println("table.log4jtable tr.even { background-color: #eee; }");
        out.println("table.log4jtable tr.odd { background-color: #fff; }");
        out.println("</style>");
        out.println("</head>");
        out.println("<body>");
        out.println("<h3>Log4J Control Console</h3>");
    }

    /**
     * Prints the Level select HTML.
     * 
     * @param out
     *            The output writer
     * @param currentLevel
     *            the current level for the log (the selected option).
     */
    private void printLevelSelector(PrintWriter out, String currentLevel) {
        out.println("<select id=\"" + PARAM_LEVEL + "\" name=\"" + PARAM_LEVEL + "\">");
        for (int j = 0; j < LEVELS.length; j++) {
            out.print("<option");
            if (LEVELS[j].equals(currentLevel)) {
                out.print(" selected=\"selected\"");
            }
            out.print(">");
            out.print(LEVELS[j]);
            out.println("</option>");
        }
        out.println("</select>");
    }

    /**
     * @see javax.servlet.Servlet#init(javax.servlet.ServletConfig)
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

}