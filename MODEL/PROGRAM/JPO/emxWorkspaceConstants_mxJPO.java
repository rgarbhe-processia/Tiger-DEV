
/*
 * emxWorkspaceConstants.java
 *
 * Copyright (c) 1992-2015 Dassault Systemes.
 *
 * All Rights Reserved. This program contains proprietary and trade secret information of MatrixOne, Inc. Copyright notice is precautionary only and does not evidence any actual or intended
 * publication of such program.
 *
 */
import matrix.db.*;

import java.lang.*;

import pss.constants.TigerConstants;

import com.matrixone.apps.domain.util.AccessUtil;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PersonUtil;

/**
 * @version AEF Rossini - Copyright (c) 2002, MatrixOne, Inc.
 */
public class emxWorkspaceConstants_mxJPO extends emxWorkspaceConstantsBase_mxJPO {
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
    public emxWorkspaceConstants_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
    }

    /**
     * Grants Access to Project Member for the workspace and its data structure.
     * @param context
     *            the eMatrix Context object
     * @param Access
     *            List of Access objects holds the access rights, grantee and grantor.
     * @return void
     * @throws Exception
     *             if the operation fails
     * @since TC V3
     * @grade 0
     */
    public void grantAccess(matrix.db.Context context, String[] args) throws Exception {
        boolean hasAccess = false;
        try {
            // TIGTK-11850 : START
            String strContextUser = context.getUser();
            String strSecurityContext = PersonUtil.getDefaultSecurityContext(context, strContextUser);
            String strRoleAssigned = (strSecurityContext.split("[.]")[0]);

            if (TigerConstants.ROLE_PSS_GLOBAL_ADMINISTRATOR.equalsIgnoreCase(strRoleAssigned) || TigerConstants.ROLE_PSS_PLM_SUPPORT_TEAM.equalsIgnoreCase(strRoleAssigned)) {
                hasAccess = true;
            } else {
                // TIGTK-11850 : END
                // check if the logged in user has access to edit the access for the object
                hasAccess = AccessUtil.isOwnerWorkspaceLead(context, this.getId());
            }

        } catch (Exception fex) {
            throw (fex);
        }
        if (!hasAccess) {
            return;
        }

        // Access Iterator of the Access list passed.
        AccessItr accessItr = new AccessItr((AccessList) JPO.unpackArgs(args));
        Access access = null;
        // BO list of the current object
        BusinessObjectList objList = new BusinessObjectList();
        objList.addElement(this);

        while (accessItr.next()) {
            access = (Access) accessItr.obj();
            // push the context for grantor.
            pushContextForGrantor(context, access.getGrantor());
            try {
                grantAccessRights(context, objList, access);
            } catch (Exception exp) {
                if (access.getGrantor().equals(AEF_WORKSPACE_MEMBER_GRANTOR_USERNAME)) {
                    ContextUtil.popContext(context);
                    MqlUtil.mqlCommand(context, "trigger on;", true);
                }
                ContextUtil.popContext(context);
                throw exp;
            }

            // if Access granted is empty then Revoke the access from grantee for business Object List
            if (access.getGrantor().equals(AEF_WORKSPACE_MEMBER_GRANTOR_USERNAME)) {
                if (access.hasNoAccess()) {
                    revokeAccessGrantorGrantee(context, access.getGrantor(), access.getUser());
                }
                ContextUtil.popContext(context);
                MqlUtil.mqlCommand(context, "trigger on;", true);
            }
            ContextUtil.popContext(context);
        }
    }
}
