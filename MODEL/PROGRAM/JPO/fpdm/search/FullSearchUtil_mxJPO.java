package fpdm.search;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import matrix.db.Context;
import matrix.db.JPO;

public class FullSearchUtil_mxJPO {

    private static Logger logger = LoggerFactory.getLogger("fpdm.search.FullSearchUtil");

    /**
     * Constructor.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @throws Exception
     *             if the operation fails
     */

    public FullSearchUtil_mxJPO(Context context, String[] args) throws Exception {
    }

    /**
     * Check if the queryType is Indexed mode to show the Toggle RealTime Query Button
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            contains the Request Map
     * @return Boolean true or false
     */

    public static Boolean isRealTimeQueryType(Context context, String[] args) throws Exception {
        // Define a boolean to return
        Boolean bAccess = Boolean.valueOf(false);

        try {
            HashMap requestMap = (HashMap) JPO.unpackArgs(args);
            String sQueryType = (String) requestMap.get("queryType");
            logger.debug("isRealTimeQueryType() - sQueryType = <" + sQueryType + ">");

            if (sQueryType != null && !"RealTime".equals(sQueryType)) {
                bAccess = Boolean.valueOf(true);
            }
        } catch (Exception e) {
            logger.error("Error in isRealTimeQueryType()\n", e);
            throw e;
        }

        return bAccess;
    }

    /**
     * Check if the queryType is Indexed mode to show the Toggle Indexed Query Button
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            contains the Request Map
     * @return Boolean true or false
     */

    public static Boolean isIndexedQueryType(Context context, String[] args) throws Exception {
        // Define a boolean to return
        Boolean bAccess = Boolean.valueOf(false);

        try {
            HashMap requestMap = (HashMap) JPO.unpackArgs(args);
            String sQueryType = (String) requestMap.get("queryType");
            logger.debug("isIndexedQueryType() - sQueryType = <" + sQueryType + ">");

            if (sQueryType != null && !"Indexed".equals(sQueryType)) {
                bAccess = Boolean.valueOf(true);
            }
        } catch (Exception e) {
            logger.error("Error in isIndexedQueryType()\n", e);
            throw e;
        }

        return bAccess;
    }
}
