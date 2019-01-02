/**
 * Log4jUtil.java
 * Provides methods to retrieve information about instanciated loggers
 * and to control the configuration of Log4j
 * @author goudeyj
 *
 */

package faurecia.logging;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.config.PropertyPrinter;

public class Log4jConfigUtil {

    public static final String ROOT = "Root";

    public static final String NAME = "name";

    public static final String LEVEL = "level";

    public static final String EFFECTIVE_LEVEL = "effective_level";

    /**
     * Intended to be mapped to the integer representation of a level, so as to be able to sort loggers by level
     */
    private static final String LEVEL_SORT_KEY = "level_sort_key";

    public static final String STYLE_INFO_OK = "\"color:green;font-size:10pt\"";

    public static final String STYLE_INFO_ERROR = "\"color:red;font-size:10pt\"";

    /**
     * Retrieves the current configuration of Log4j
     * 
     * @return a String containing the whole set of properties currently defined
     */
    public static String getLog4jConfigString() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(outputStream);
        PropertyPrinter pp = new PropertyPrinter(writer);
        pp.print(writer);
        return outputStream.toString().replaceAll("\n", "<br>");
    }

    /**
     * Reloads the Log4j configuration file
     * 
     * @param originatorClass
     *            the Class that made the call to this method
     * @return a String indicating the result of the operation
     */
    public static String resetLog4j(Class originatorClass) {
        LogManager.resetConfiguration();
        ClassLoader cl = originatorClass.getClassLoader();
        URL log4jprops = cl.getResource("log4j.properties");
        if (log4jprops != null) {
            PropertyConfigurator.configure(log4jprops);
            return "<p style=" + STYLE_INFO_OK + ">Log4j configuration file has been successfully reloaded</p>";
        }
        return "<p style=" + STYLE_INFO_ERROR + ">The configuration file could not be found</p>";

    }

    /**
     * Returns a map with basic information regarding a logger
     * 
     * @param logger
     *            a Logger instance
     * @return a Map
     */
    private static Hashtable<String, String> buildLoggerInfoMap(Logger logger) {
        Level effLevel = logger.getEffectiveLevel();
        Level level = logger.getLevel();
        Hashtable<String, String> ht = new Hashtable<String, String>();
        ht.put(NAME, logger.getName().equals("") ? ROOT : logger.getName());
        ht.put(LEVEL, (level == null) ? "null" : level.toString());
        ht.put(EFFECTIVE_LEVEL, (effLevel == null) ? "null" : effLevel.toString());
        ht.put(LEVEL_SORT_KEY, String.valueOf(effLevel.toInt()));
        return ht;
    }

    public static Map<String, String> getRootLoggerInfo() {
        return buildLoggerInfoMap(Logger.getRootLogger());
    }

    /**
     * Get a list of all current loggers.
     * 
     * @param sortByLevel
     * @return a Vector containing all loggers.
     */
    @SuppressWarnings("unchecked")
    public static ArrayList<Map<String, String>> getLoggersInfos(boolean sortByLevel) {
        ArrayList<Map<String, String>> mlLoggers = new ArrayList<Map<String, String>>();
        Enumeration eLoggers = LogManager.getCurrentLoggers();
        // Add all current loggers to the list
        while (eLoggers.hasMoreElements()) {
            mlLoggers.add(buildLoggerInfoMap((Logger) eLoggers.nextElement()));
        }
        Hashtable<String, String> localHashMap = new Hashtable<String, String>();
        System.out.println("sortByLevel=<"+sortByLevel+">");
        System.out.println("mlLoggers=<"+mlLoggers+">");

        if (sortByLevel) {
            localHashMap.put("name", LEVEL_SORT_KEY);
            localHashMap.put("dir", "ascending");
            localHashMap.put("type", "string");
            ArrayList<Hashtable<String, String>> localArrayList = new ArrayList<Hashtable<String, String>>();
            localArrayList.add(localHashMap);
            Collections.sort(mlLoggers, new MapComparator(localArrayList));
        } else {
            localHashMap.put("name", NAME);
            localHashMap.put("dir", "ascending");
            localHashMap.put("type", "string");
            ArrayList<Hashtable<String, String>> localArrayList = new ArrayList<Hashtable<String, String>>();
            localArrayList.add(localHashMap);
            Collections.sort(mlLoggers, new MapComparator(localArrayList));
        }
        System.out.println("After sort mlLoggers=<"+mlLoggers+">");

        return mlLoggers;
    }

    /**
     * Sets the level of the given logger
     * 
     * @param args
     *            (args[0]: name of the logger to be changed, args[1]: destination level)
     */
    public static void setLoggerLevel(String[] args) {
        String loggerName = args[0];
        Logger logger = (ROOT.equalsIgnoreCase(loggerName) ? Logger.getRootLogger() : Logger.getLogger(loggerName));
        logger.setLevel(Level.toLevel(args[1]));
    }

}
