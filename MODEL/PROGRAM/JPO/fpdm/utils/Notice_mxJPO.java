package fpdm.utils;

import com.matrixone.apps.domain.util.MqlUtil;

import matrix.db.Context;

public class Notice_mxJPO {

    /**
     * This method displays mql error message.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param error
     *            String contains error message
     * @throws Exception
     *             if the operation fails
     */

    public static void mqlError(Context context, String error) throws Exception {
        MqlUtil.mqlCommand(context, "error $1", error);
    }

    /**
     * This method displays mql notice message.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param notice
     *            String containing notice message
     * @throws Exception
     *             if the operation fails
     */

    public static void mqlNotice(Context context, String notice) throws Exception {
        MqlUtil.mqlCommand(context, "notice $1", notice);
    }

    /**
     * This method displays mql warning message.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param warning
     *            String containing warning message
     * @throws Exception
     *             if the operation fails
     */
    public static void mqlWarning(Context context, String warning) throws Exception {
        MqlUtil.mqlCommand(context, "warning $1", warning);
    }
}
