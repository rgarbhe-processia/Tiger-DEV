
/*
 ** ${CLASS:MarketingFeature}
 **
 ** Copyright (c) 1993-2015 Dassault Systemes. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes. Copyright notice is precautionary only and does
 * not evidence any actual or intended publication of such program
 */

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.engineering.EngineeringUtil;
import com.matrixone.apps.engineering.Part;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.Access;
import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;
import pss.constants.TigerConstants;

/**
 * The <code>emxENCActionLinkAcess</code> class contains code for the "Action Link".
 * @version EC 10.5 - Copyright (c) 2002, MatrixOne, Inc.
 */
public class emxENCActionLinkAccess_mxJPO extends emxENCActionLinkAccessBase_mxJPO {
    /**
     * Constructor.
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param args
     *            holds no arguments.
     * @throws Exception
     *             if the operation fails.
     * @since EC 10.5
     */
    public emxENCActionLinkAccess_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
    }

    /**
     * Overrided OOTB method for TIGTK-17756 :: ALM-5978 Checking if modification is allowed for Part objects.
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param args
     *            holds objectId.
     * @return Boolean.
     * @throws Exception
     *             If the operation fails.
     * @since EC10-5.
     */
    public Boolean isModificationAllowedForPart(Context context, String[] args) throws Exception {
        boolean allowChanges = true;
        try {

            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            String parentId = (String) paramMap.get("objectId");
            String obsoleteState = PropertyUtil.getSchemaProperty(context, "policy", DomainConstants.POLICY_EC_PART, "state_Obsolete");
            String approvedState = PropertyUtil.getSchemaProperty(context, "policy", DomainConstants.POLICY_EC_PART, "state_Approved");

            StringList strList = new StringList(2);
            strList.add(SELECT_CURRENT);
            strList.add("policy");

            DomainObject domObj = new DomainObject(parentId);

            Map map = domObj.getInfo(context, strList);

            String objState = (String) map.get(SELECT_CURRENT);
            String objPolicy = (String) map.get("policy");
            String policyClass = EngineeringUtil.getPolicyClassification(context, objPolicy);
            String propAllowLevel = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentral.Part.RestrictPartEdit");
            StringList propAllowLevelList = new StringList();

            // START :: TIGTK-17756 :: ALM-5978
            String strLogginRole = (String) FrameworkUtil.split(context.getRole().substring(5), ".").get(0);
            if (UIUtil.isNotNullAndNotEmpty(strLogginRole) && (TigerConstants.ROLE_PSS_PLM_SUPPORT_TEAM.equals(strLogginRole) || TigerConstants.ROLE_PSS_GLOBAL_ADMINISTRATOR.equals(strLogginRole))
                    && ((TigerConstants.POLICY_PSS_ECPART.equals(objPolicy) && TigerConstants.STATE_PART_RELEASE.equals(objState))
                            || (TigerConstants.POLICY_PSS_DEVELOPMENTPART.equals(objPolicy) && TigerConstants.STATE_DEVELOPMENTPART_COMPLETE.equals(objState))
                            || (TigerConstants.POLICY_STANDARDPART.equals(objPolicy) && TigerConstants.STATE_STANDARDPART_RELEASE.equals(objState)))) {
                return true;
            }
            // END :: TIGTK-17756 :: ALM-5978

            try {
                Part part = new Part();
                part.deleteLicenseCheck(context, objPolicy);
            } catch (Exception e) {
                return false;
            }

            if (propAllowLevel != null && !"null".equals(propAllowLevel) && propAllowLevel.length() > 0) {
                StringTokenizer stateTok = new StringTokenizer(propAllowLevel, ",");
                while (stateTok.hasMoreTokens()) {
                    String tok = (String) stateTok.nextToken();
                    propAllowLevelList.add(FrameworkUtil.lookupStateName(context, objPolicy, tok));
                }
            }
            if ("Production".equals(policyClass)) {
                allowChanges = (!propAllowLevelList.contains(objState));
            } else if (EngineeringUtil.isMBOMInstalled(context) && EngineeringUtil.isManuPartPolicy(context, objPolicy)) {
                allowChanges = (!objState.equals(DomainObject.STATE_PART_REVIEW) && !objState.equals(approvedState) && !objState.equals(DomainObject.STATE_PART_RELEASE)
                        && !objState.equals(obsoleteState));
            }

            String propAllowLevelDevPart = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentral.Part.RestrictDevelopmentPartEdit");
            StringList propAllowLevelListDevPart = new StringList();

            if (propAllowLevelDevPart != null && !"null".equals(propAllowLevelDevPart) && propAllowLevelDevPart.length() > 0) {
                StringTokenizer stateToken = new StringTokenizer(propAllowLevelDevPart, ",");
                while (stateToken.hasMoreTokens()) {
                    String token = (String) stateToken.nextToken();
                    propAllowLevelListDevPart.add(FrameworkUtil.lookupStateName(context, objPolicy, token));
                }
            }
            if ("Development".equals(policyClass)) {
                allowChanges = (!propAllowLevelListDevPart.contains(objState));
            }

            Access access = new Access();
            BusinessObject boPart = new BusinessObject(parentId);
            access = boPart.getAccessMask(context);
            if (!access.hasModifyAccess()) {
                allowChanges = false;
            }

            String sBOMViewFilter = "";
            if (EngineeringUtil.isMBOMInstalled(context)) {
                sBOMViewFilter = (String) paramMap.get("ENCBillOfMaterialsViewCustomFilter");
                if (sBOMViewFilter != null && !"engineering".equalsIgnoreCase(sBOMViewFilter)) {
                    allowChanges = false;
                }
            }

        } catch (Exception e) {
            throw new Exception(e.toString());
        }
        return Boolean.valueOf(allowChanges);
    }
}
