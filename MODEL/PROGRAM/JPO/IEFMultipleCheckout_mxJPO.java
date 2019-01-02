
/*
 ** IEFMultipleCheckout
 **
 ** Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 **
 ** Program for multiple checkout of IEF objects.
 */

import java.util.HashMap;
import java.util.Hashtable;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADLocalConfigObject;

public class IEFMultipleCheckout_mxJPO {
    private HashMap _parametersMap = null;

    private MCADMxUtil _util = null;

    private MCADServerResourceBundle _serverResourceBundle = null;

    private IEFGlobalCache _cache = null;

    private HashMap _integrationNameGCOMap = null;

    private MCADLocalConfigObject _localConfigObject = null;

    private int _numErrorMessages = 0;

    private int _numWarningMessages = 0;

    public IEFMultipleCheckout_mxJPO(Context context, String[] args) throws Exception {
    }

    private void init(Context context, String[] args) throws Exception {
        _parametersMap = (HashMap) JPO.unpackArgs(args);

        _integrationNameGCOMap = (HashMap) _parametersMap.get("GCOTable");
        _serverResourceBundle = new MCADServerResourceBundle((String) _parametersMap.get("LocaleLanguage"));
        _cache = new IEFGlobalCache();
        _util = new MCADMxUtil(context, _serverResourceBundle, _cache);
        _localConfigObject = (MCADLocalConfigObject) _parametersMap.get("LCO");
        _numWarningMessages = 0;
        _numErrorMessages = 0;
    }

    public String[] getValidObjIdsForCheckout(Context context, String[] args) throws Exception {
        String[] returnStrArray = new String[5];

        // First of all initialize the class variables.
        init(context, args);

        StringBuffer globalWarningMsgBuffer = new StringBuffer();
        StringBuffer globalErrorMsgBuffer = new StringBuffer();
        StringBuffer globalBoidBuffer = new StringBuffer();

        String integrationName = "";
        String globalResult = "true";

        String[] objectIDs = (String[]) _parametersMap.get("ObjectIDs");
        HashMap objectIDsDMUSessionNameMap = (HashMap) _parametersMap.get("ObjectIDsDMUSessionNameTable");

        boolean isIntegrationSelected = false;
        MCADGlobalConfigObject _globalConfig = null;
        returnStrArray[4] = MCADAppletServletProtocol.TYPE_NEUTRAL;

        for (int i = 0; i < objectIDs.length; i++) {
            String objectID = objectIDs[i];

            BusinessObject bus = new BusinessObject(objectID);
            bus.open(context);
            String busType = bus.getTypeName();
            String busName = bus.getName();
            String busRev = bus.getRevision();
            String cadType = _util.getCADTypeForBO(context, bus);
            bus.close(context);

            if (!isIntegrationSelected) {
                integrationName = _util.getIntegrationName(context, objectID);
                returnStrArray[0] = integrationName;
                isIntegrationSelected = true;

                _globalConfig = (MCADGlobalConfigObject) _integrationNameGCOMap.get(integrationName);
                if (_globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_NEUTRAL)) {
                    isIntegrationSelected = false;
                    String integrationNameFromLCO = _localConfigObject.getPreferredIntegrationNameFromLCO();
                    if (integrationNameFromLCO != null && integrationNameFromLCO.length() > 0) {
                        integrationName = integrationNameFromLCO;
                        returnStrArray[0] = integrationName;
                    } else {
                        returnStrArray[0] = "";
                    }
                }
            }

            if (!_globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_NEUTRAL)) {
                returnStrArray[4] = "";
            }

            if (_globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_NEUTRAL) || isCompatibleSource(context, objectID, integrationName)) {
                // String objectCheckoutDetails = getObjectDetailsForCheckout(objectID, "MCADJPOUtils", "getBoIdToCheckout", integrationName);
                _globalConfig = (MCADGlobalConfigObject) _integrationNameGCOMap.get(integrationName);
                MCADServerGeneralUtil serverGeneralUtil = new MCADServerGeneralUtil(context, _globalConfig, _serverResourceBundle, _cache);

                String[] objectCheckoutDetails = serverGeneralUtil.getValidObjctIdForCheckout(context, objectID);
                String tmpRes = objectCheckoutDetails[0];
                String tmpBoid = objectCheckoutDetails[1];
                String tmpMsg = objectCheckoutDetails[2];
                ;

                String dmuSessionName = "";
                if (objectIDsDMUSessionNameMap != null)
                    dmuSessionName = (String) objectIDsDMUSessionNameMap.get(objectID);

                if (tmpRes.compareTo("false") == 0) {
                    globalResult = "false";
                    addErrorMsg(globalErrorMsgBuffer, tmpMsg);
                } else {
                    // boid is passed to javascript only if it's valid.
                    addBoInfo(globalBoidBuffer, tmpBoid, dmuSessionName, i);
                    addWarningMsg(globalWarningMsgBuffer, tmpMsg);
                }
            } else {
                globalResult = "false";

                Hashtable msgTable = new Hashtable();
                msgTable.put("TYPE", busType);
                msgTable.put("NAME", busName);
                msgTable.put("REVISION", busRev);

                // Models created by multiple CAD tools selected for checkout. Unselect object T N R.
                String cseIncompatibilityError = _serverResourceBundle.getString("mcadIntegration.Server.Message.CSEIncompatible", msgTable);
                ;
                addErrorMsg(globalErrorMsgBuffer, cseIncompatibilityError);
            }
        }

        returnStrArray[1] = globalResult;
        returnStrArray[2] = globalBoidBuffer.toString();
        if (globalResult != null && globalResult.equals("true"))
            returnStrArray[3] = globalWarningMsgBuffer.toString();
        else
            returnStrArray[3] = globalErrorMsgBuffer.toString();

        return returnStrArray;
    }

    private void addErrorMsg(StringBuffer globalErrorMsgBuffer, String msg) {
        if (msg.length() > 0) {
            if (_numErrorMessages != 0)
                globalErrorMsgBuffer.append("|" + msg);
            else
                globalErrorMsgBuffer.append(msg);

            _numErrorMessages++;
        }
    }

    private void addWarningMsg(StringBuffer globalWarningMsgBuffer, String msg) {
        if (msg.length() > 0) {
            if (_numWarningMessages != 0)
                globalWarningMsgBuffer.append("|" + msg);
            else
                globalWarningMsgBuffer.append(msg);

            _numWarningMessages++;
        }
    }

    private void addBoInfo(StringBuffer globalBoidBuffer, String boid, String dmuSessionName, int i) {
        if (i != 0)
            globalBoidBuffer.append("|" + boid + "|||" + dmuSessionName);
        else
            globalBoidBuffer.append(boid + "|||" + dmuSessionName);
    }

    private boolean isCompatibleSource(Context context, String busId, String activeCSE) throws MCADException {
        boolean res = true;

        String objectSource = _util.getIntegrationName(context, busId);
        if (!objectSource.equalsIgnoreCase(activeCSE)) {
            res = false;
        }

        return res;
    }
}
