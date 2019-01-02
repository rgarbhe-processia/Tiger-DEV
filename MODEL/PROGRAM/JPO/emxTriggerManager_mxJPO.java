
/*
 * emxTriggerManager
 **
 ** Copyright (c) 1992-2015 Dassault Systemes. All Rights Reserved. This program contains proprietary and trade secret information of MatrixOne, Inc. Copyright notice is precautionary only and does not
 * evidence any actual or intended publication of such program
 **
 ** This JPO contains the utility methods for policy.
 **
 */

import matrix.db.Context;

/**
 * The <code>emxTriggerManager</code> jpo contains policy utility methods.
 * @version EC 10.0.0.0 - Copyright (c) 2003, MatrixOne, Inc.
 */

public class emxTriggerManager_mxJPO extends emxTriggerManagerBase_mxJPO {
    /**
     * Constructor.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @throws Exception
     *             if the operation fails
     * @since EC 10.0.0.0
     */

    public emxTriggerManager_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
    }
}
