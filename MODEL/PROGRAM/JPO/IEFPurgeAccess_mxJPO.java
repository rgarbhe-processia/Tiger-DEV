
/*
 ** IEFPurgeAccess
 **
 ** Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 ** 
 ** Program to use as to check the whether the user has access to Purge.
 */
import java.util.HashMap;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.MatrixWriter;
import matrix.db.BusinessObject;

import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.apps.domain.util.PropertyUtil;

public class IEFPurgeAccess_mxJPO {
    String _sObjId;

    MatrixWriter _mxWriter = null;

    private static String ROLE_IEFADMIN = null;

    /**
     * The no-argument constructor.
     */
    public IEFPurgeAccess_mxJPO() {
    }

    /**
     * Constructor which accepts the Matrix context and an array of String arguments.
     */
    public IEFPurgeAccess_mxJPO(Context context, String[] args) throws Exception {
        _mxWriter = new MatrixWriter(context);
    }

    public int mxMain(Context context, String[] args) throws Exception {
        return 0;
    }

    public Boolean checkAccess(Context context, String[] args) throws Exception {
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        String objectId = (String) paramMap.get("objectId");
        String language = (String) paramMap.get("languageStr");

        boolean isUserHasAccess = false;

        MCADMxUtil mxUtil = new MCADMxUtil(context, new MCADServerResourceBundle(language), new IEFGlobalCache());

        if (MCADMxUtil.isSolutionBasedEnvironment(context)) {
            BusinessObject busObj = new BusinessObject(objectId);
            busObj.open(context);
            String stateName = mxUtil.getCurrentState(context, busObj).getName();
            busObj.close(context);

            String userRole = context.getRole();

            String vplmProjectAdminRoleName = MCADMxUtil.getActualNameForAEFData(context, "role_VPLMProjectAdministrator");
            String vplmAdminRoleName = MCADMxUtil.getActualNameForAEFData(context, "role_VPLMAdmin");
            String vplmProjectLeader = MCADMxUtil.getActualNameForAEFData(context, "role_VPLMProjectLeader");

            String policyName = MCADMxUtil.getActualNameForAEFData(context, "policy_DesignTEAMDefinition");

            String strFrozenState = PropertyUtil.getSchemaProperty(context, "policy", policyName, "state_Approved");
            String strReleasedState = PropertyUtil.getSchemaProperty(context, "policy", policyName, "state_Release");
            String strInWork = PropertyUtil.getSchemaProperty(context, "policy", policyName, "state_Preliminary");

            if (stateName.equals(strReleasedState)) {
                if (userRole.contains(vplmProjectAdminRoleName) || userRole.contains(vplmAdminRoleName))
                    isUserHasAccess = true;
            }

            if (stateName.equals(strFrozenState)) {
                if (userRole.contains(vplmProjectLeader) || userRole.contains(vplmProjectAdminRoleName) || userRole.contains(vplmAdminRoleName))
                    isUserHasAccess = true;
            }
            if (stateName.equals(strInWork)) {
                if (userRole.contains(vplmProjectLeader))
                    isUserHasAccess = true;
            }
        } else {
            ROLE_IEFADMIN = MCADMxUtil.getActualNameForAEFData(context, "role_IEFAdmin");

            if (context.isAssigned(ROLE_IEFADMIN) == true) {
                isUserHasAccess = true;
                _mxWriter.write("The present user does not have role" + ROLE_IEFADMIN + " assigned\n");
            } else {
                isUserHasAccess = false;
                _mxWriter.write("The present user does not have role" + ROLE_IEFADMIN + " assigned\n");
            }

        }
        String[] init = new String[] {};
        Boolean isVersioningEnabled = (Boolean) JPO.invoke(context, "IEFShowVersions", init, "isVersioningEnabled", JPO.packArgs(paramMap), Boolean.class);

        if (isVersioningEnabled.booleanValue() && isUserHasAccess)
            return new Boolean(true);
        else
            return new Boolean(false);

    }
}
