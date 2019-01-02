
/*
 ** PSS_emxDomainAccess TIGTK-7776 Copyright (c) 1992-2015 Dassault Systemes. All Rights Reserved. This program contains proprietary and trade secret information of MatrixOne, Inc. Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program.
 **
 */

import com.matrixone.apps.domain.DomainAccess;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainSymbolicConstants;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;

import matrix.db.Context;
import matrix.util.StringList;

/**

 */
public class PSS_emxDomainAccess_mxJPO extends emxDomainAccessBase_mxJPO {

    /**
     * Constructor.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no argumentss
     * @throws Exception
     *             if the operation
     * @since V6R2011x
     * @grade 0
     */
    public PSS_emxDomainAccess_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
    }

    /************ APIs related to trigger manager ***************/

    public boolean createObjectOwnershipInheritance(Context context, String[] args) throws Exception {
        String fromId = args[0];
        String toId = args[1];
        String relId = args[2];
        String relType = args[3];
        String fromType = args[4];
        String toType = args[5];
        return createObjectOwnershipInheritance(context, fromId, toId, relId, relType, fromType, toType, null, true);
    }

    public boolean createObjectOwnershipInheritance(Context context, String fromId, String toId, String relId, String relType, String fromType, String toType, String comment, boolean runAsUserAgent)
            throws Exception {
        String attrAccessTypeSelect = DomainObject.getAttributeSelect(PropertyUtil.getSchemaProperty(context, DomainSymbolicConstants.SYMBOLIC_attribute_AccessType));
        DomainObject obj = DomainObject.newInstance(context, toId);
        String attrAccessType = obj.getInfo(context, attrAccessTypeSelect);
        boolean needGrants = true;
        if (DomainObject.RELATIONSHIP_VAULTED_OBJECTS.equals(relType)) {
            if (fromType == null || "".equals(fromType)) {
                DomainObject fromObject = DomainObject.newInstance(context, fromId);
                fromType = fromObject.getInfo(context, DomainObject.SELECT_TYPE);
            }
            if (fromType.equals(DomainObject.TYPE_PROJECT_VAULT)) {
                needGrants = false;
                StringList contentTypesList = new StringList();
                try {
                    String inheritAccessForTypes = EnoviaResourceBundle.getProperty(context, "emxFramework.FolderContentTypesThatRequireGrants");

                    boolean checkSubTypes = "true".equalsIgnoreCase(EnoviaResourceBundle.getProperty(context, "emxFramework.IncludeSubTypesForGrants"));
                    StringList types = FrameworkUtil.split(inheritAccessForTypes, ",");
                    for (int i = 0; i < types.size(); i++) {
                        String sType = PropertyUtil.getSchemaProperty(context, (String) types.get(i));
                        if (sType != null && !"".equals(sType.trim()) /* && !PROP_ACT_TYPES.contains(sType) */) {
                            // PROP_ACT_TYPES.add(sType);
                            contentTypesList.add(sType);
                            if (checkSubTypes) {
                                String subTypes = MqlUtil.mqlCommand(context, "print type \"" + sType + "\" select derivative dump |", true);
                                contentTypesList.addAll(FrameworkUtil.split(subTypes, "|"));
                            }
                        }
                    }

                    // ends

                } catch (Exception ex) {
                    needGrants = true;
                }
                if (!needGrants) {
                    if (toType == null || "".equals(toType)) {
                        DomainObject toObject = DomainObject.newInstance(context, toId);
                        toType = toObject.getInfo(context, DomainObject.SELECT_TYPE);
                    }
                    // contentTypesList = FrameworkUtil.split(contentTypes, ",");
                    // if(contentTypesList.indexOf(symbolicType) > -1)
                    if (contentTypesList.indexOf(toType) > -1) {
                        needGrants = true;
                    }
                }
            }
        }
        if (needGrants && (attrAccessType == null || "".equals(attrAccessType) || !"Specific".equals(attrAccessType))) {
            DomainAccess.createObjectOwnership(context, toId, fromId, comment, runAsUserAgent);
        }
        return true;
    }
}
