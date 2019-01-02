import java.util.Hashtable;
import java.util.Vector;

import matrix.db.BusinessObject;
import matrix.db.Context;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADStringUtils;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.framework.ui.UIUtil;

public class PSS_DECModifyUpdateStamp_mxJPO extends DECModifyUpdateStamp_mxJPO {
    private boolean isCheckinEx = false;

    private boolean isCheckin = false;

    private boolean isAttributeSynch = false;

    private boolean isLockUnlock = false;

    private boolean isCheckout = false;

    private boolean isCheckoutEx = false;

    private static Hashtable<String, String> eventAttributeMap = new Hashtable<String, String>();

    private static Hashtable<String, String> eventsMappedToGCO = new Hashtable<String, String>();

    static {
        eventsMappedToGCO.put("Checkin", MCADAppletServletProtocol.UPDATESTAMP_EVENT_CHECKIN);
        eventsMappedToGCO.put("ChangeOwner", MCADAppletServletProtocol.UPDATESTAMP_EVENT_ATTRIBUTE_MODIFICATION);
        eventsMappedToGCO.put("ChangePolicy", MCADAppletServletProtocol.UPDATESTAMP_EVENT_ATTRIBUTE_MODIFICATION);
        eventsMappedToGCO.put("ChangeVault", MCADAppletServletProtocol.UPDATESTAMP_EVENT_ATTRIBUTE_MODIFICATION);
        eventsMappedToGCO.put("Lock", MCADAppletServletProtocol.UPDATESTAMP_EVENT_ATTRIBUTE_MODIFICATION);
        eventsMappedToGCO.put("Unlock", MCADAppletServletProtocol.UPDATESTAMP_EVENT_ATTRIBUTE_MODIFICATION);
        eventsMappedToGCO.put("Removefile", MCADAppletServletProtocol.UPDATESTAMP_EVENT_CHECKIN);

        eventAttributeMap.put("ChangeOwner", "$$owner$$");
        eventAttributeMap.put("ChangePolicy", "$$policy$$");
        eventAttributeMap.put("ChangeVault", "$$vault$$");
        eventAttributeMap.put("Lock", "$$locker$$");
        eventAttributeMap.put("Unlock", "$$locker$$");
    }

    public PSS_DECModifyUpdateStamp_mxJPO(Context context, String args[]) throws Exception {
        String language = "en-us";
        mxUtil = new MCADMxUtil(context, new MCADServerResourceBundle(language), new IEFGlobalCache());

        isCheckin = getRPEValue(context, mxUtil, MCADServerSettings.IEF_CHECKIN, true).equalsIgnoreCase("true");
        isCheckinEx = getRPEValue(context, mxUtil, MCADServerSettings.IEF_CHECKINEX, true).equalsIgnoreCase("true");
        isAttributeSynch = getRPEValue(context, mxUtil, MCADServerSettings.IEF_ATTR_SYNC, true).equalsIgnoreCase("true");

        isLockUnlock = getRPEValue(context, mxUtil, MCADServerSettings.IEF_LOCK_UNLOCK, true).equalsIgnoreCase("true");

        isCheckout = getRPEValue(context, mxUtil, MCADServerSettings.IEF_CHECKOUT, true).equalsIgnoreCase("true");
        isCheckoutEx = getRPEValue(context, mxUtil, MCADServerSettings.IEF_CHECKOUTEX, true).equalsIgnoreCase("true");

        globalcache = new IEFGlobalCache();

        serverResourceBundle = new MCADServerResourceBundle("");
    }

    /**
     * this method is customised for CAD change ownership issue,RPE value check added for issue TIGTK-7481 by rutuja Ekatpure
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            1. event 2. object id
     * @throws Exception
     *             if the operation fails
     * @return 0 if success 1 if fail
     */
    public int modifyUpdateStamp(matrix.db.Context context, String[] args) throws Exception {
        String strRPEValue = PropertyUtil.getRPEValue(context, "PSS_Update_Project_owner", true);
        if (strRPEValue.equalsIgnoreCase("True") || UIUtil.isNullOrEmpty(strRPEValue)) {
            return 0;
        }
        if (!(isCheckin || isCheckinEx || isAttributeSynch || isLockUnlock || isCheckout || isCheckoutEx)) {
            String event = args[0];
            String objectId = args[1];
            String sKindOfChng = args[2];

            if (sKindOfChng != null && !sKindOfChng.equalsIgnoreCase("null")) {
                if (("owner").equalsIgnoreCase(sKindOfChng)) {
                    updateOwnerInfo(context, event, objectId);
                }
            }
            if (eventsMappedToGCO.containsKey(event)) {
                String mappedGCOEvent = (String) eventsMappedToGCO.get(event);
                String integrationName = mxUtil.getIntegrationName(context, objectId);
                if (integrationName != null && integrationName.contains("DENIED")) {
                    return 0;
                } else {
                    if (!MCADStringUtils.isNullOrEmpty(integrationName)) {
                        MCADGlobalConfigObject globalConfigObj = getGlobalConfigObject(context, integrationName, mxUtil);
                        if (globalConfigObj != null) {
                            if (!MCADStringUtils.isNullOrEmpty(mappedGCOEvent) && globalConfigObj.isModificationEvent(mappedGCOEvent)) {
                                BusinessObject busObject = new BusinessObject(objectId);
                                busObject.open(context, false);

                                String cadType = mxUtil.getCADTypeForBO(context, busObject);
                                String mxType = busObject.getTypeName();

                                busObject.close(context);

                                // [NDM] OP6
                                /*
                                 * if(!globalConfigObj.isMajorType(mxType)) mxType = mxUtil.getCorrespondingType(context, mxType);
                                 */

                                if (event.equalsIgnoreCase("ChangeOwner") || event.equalsIgnoreCase("ChangePolicy") || event.equalsIgnoreCase("ChangeVault") || event.equalsIgnoreCase("Lock")
                                        || event.equalsIgnoreCase("Unlock")) {
                                    Vector attr = globalConfigObj.getCADAttribute(mxType, (String) eventAttributeMap.get(event), cadType);

                                    if (!attr.isEmpty())
                                        mxUtil.modifyUpdateStamp(context, objectId);
                                } else if (event.equalsIgnoreCase("checkin") || event.equalsIgnoreCase("removefile")) {
                                    String format = this.getRPEValue(context, mxUtil, "FORMAT", false);

                                    String primaryFormat = globalConfigObj.getFormatsForType(mxType, cadType);

                                    if (primaryFormat.equals(format))
                                        mxUtil.modifyUpdateStamp(context, objectId);
                                }
                            }
                        }
                    }
                }
            }
        }

        return 0;
    }

    private String getRPEValue(Context context, MCADMxUtil mxUtil, String variableName, boolean isGlobal) {
        String sResult = "";
        String Args[] = new String[2];
        Args[0] = "global";
        Args[1] = variableName;
        if (isGlobal)
            sResult = mxUtil.executeMQL(context, "get env $1 $2", Args);
        else {
            Args = new String[1];
            Args[0] = variableName;
            sResult = mxUtil.executeMQL(context, "get env $1", Args);
        }
        String result = "";

        if (sResult.startsWith("true")) {
            result = sResult.substring(sResult.indexOf("|") + 1, sResult.length());
        }

        return result;
    }
}
