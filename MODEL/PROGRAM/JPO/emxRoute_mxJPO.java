
/*
 * emxRoute.java
 *
 * Copyright (c) 1992-2015 Dassault Systemes.
 *
 * All Rights Reserved. This program contains proprietary and trade secret information of MatrixOne, Inc. Copyright notice is precautionary only and does not evidence any actual or intended
 * publication of such program.
 *
 */

import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.Context;

/**
 * @version AEF Rossini - Copyright (c) 2002, MatrixOne, Inc.
 */
public class emxRoute_mxJPO extends emxRouteBase_mxJPO {

    /**
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @throws Exception
     *             if the operation fails
     * @since AEF Rossini
     * @grade 0
     */
    public emxRoute_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
    }

    /**
     * Check Trigger to block the remove of finished routes if it's not an admin user This trigger will check if user is admin, then remove command will be shown.
     * @param context
     *            the Enovia <code>Context</code> object
     * @param args
     *            0 - String containing object id.
     */
    public int checkRemoveRouteInFinishedState(Context context, String[] args) throws Exception {

        // TIGTK-17240 : START
        String isPromoteFromCOAction = context.getCustomData("isPromoteFromCOAction");

        if (UIUtil.isNotNullAndNotEmpty(isPromoteFromCOAction) && isPromoteFromCOAction.equalsIgnoreCase("true")) {
            return 0;
        }
        // TIGTK-17240 : START
        String loggedInRole = PersonUtil.getDefaultSecurityContext(context);
        boolean isFinishedState = false;
        String roleProjectAdmin = PropertyUtil.getSchemaProperty(context, "role_VPLMProjectAdministrator");
        String roleAdmin = PropertyUtil.getSchemaProperty(context, "role_VPLMAdmin");
        String routeStatus = DomainObject.newInstance(context, args[0]).getInfo(context, "attribute[" + ATTRIBUTE_ROUTE_STATUS + "]");
        isFinishedState = routeStatus.equalsIgnoreCase("Finished");
        if (!(loggedInRole.contains(roleProjectAdmin) || loggedInRole.contains(roleAdmin)) && isFinishedState) {
            emxContextUtil_mxJPO.mqlNotice(context, EnoviaResourceBundle.getProperty(context, "emxComponentsStringResource", context.getLocale(), "emxComponents.Route.RemoveFinishedRoute"));
            return 1;
        } else {
            return 0;
        }
    }

}
