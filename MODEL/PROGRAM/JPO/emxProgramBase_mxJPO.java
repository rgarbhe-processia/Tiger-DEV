
/*
 * emxProgramBase
 **
 ** Copyright (c) 2003-2015 Dassault Systemes. All Rights Reserved. This program contains proprietary and trade secret information of MatrixOne, Inc. Copyright notice is precautionary only and does not
 * evidence any actual or intended publication of such program
 **
 ** This JPO contains the implementation of emxTask
 **
 ** static const char RCSID[] = $Id: emxProgramBase.java.rca 1.6 Wed Oct 22 16:21:21 2008 przemek Experimental przemek $
 */

import matrix.db.*;

/**
 * This is a place holder JPO in AEF which will be replaced by installation of other applications. Some of the policies are referring this JPO in their filter expression and if only AEF is installed,
 * core matrix fails to evaluate filter expression if this JPO doesn't exist. It contents all the necessary methods that have been referenced by filter expression. This JPO code will be replaced with
 * appropriate code when other application gets installed.
 * @version AEF 10.0.SP4 - Copyright (c) 2002, MatrixOne, Inc.
 */
public class emxProgramBase_mxJPO {
    /**
     * Constructor.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @throws Exception
     *             if the operation fails
     * @since AEF 10.0.SP4
     * @grade 0
     */
    public emxProgramBase_mxJPO(Context context, String[] args) throws Exception {
    }

    /**
     * This method always returns true.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     * @throws Exception
     *             if the operation fails
     * @since AEF 10.0.SP4
     * @grade 0
     */
    public boolean hasAccess(Context context, String args[]) throws Exception {
        return true;
    }
}
