
/*
 ** DSCStartCADTollCommandAccess
 **
 ** Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 **
 ** Program to display Checkout Icon
 */
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;

import matrix.db.Context;
import matrix.db.JPO;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.beans.MCADConfigObjectLoader;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.utils.MCADLocalConfigObject;

public class DSCStartCADToolCommandAccess_mxJPO {

    public DSCStartCADToolCommandAccess_mxJPO() {
    }

    public DSCStartCADToolCommandAccess_mxJPO(Context context, String[] args) throws Exception {
        if (!context.isConnected()) {
            MCADServerException.createException("not supported no desktop client", null);
        }
    }

    public int mxMain(Context context, String[] args) throws Exception {
        return 0;
    }

    public boolean isIntegrationAssigned(Context context, String[] args) throws Exception {
        try {
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            HashMap settingsMap = (HashMap) paramMap.get("SETTINGS");

            if (null != settingsMap) {
                String _localObjectRevision = MCADMxUtil.getConfigObjectRevision(context);

                String integNameFromCommand = (String) settingsMap.get("IntegrationName");
                String _localObjectTypeName = MCADMxUtil.getActualNameForAEFData(context, "type_MCADInteg-LocalConfig");

                MCADConfigObjectLoader configObjectLoader = new MCADConfigObjectLoader(null);
                MCADLocalConfigObject lco = configObjectLoader.createLocalConfigObject(_localObjectTypeName, context.getUser(), _localObjectRevision, context);

                Hashtable integrationMapping = lco.getIntegrationNameGCONameMapping();

                if (null != integrationMapping) {
                    Enumeration itr = integrationMapping.keys();
                    if (null != itr)
                        while (itr.hasMoreElements()) {
                            String sIntegName = (String) itr.nextElement();
                            String sIntegArc = (String) integrationMapping.get(sIntegName);
                            if (sIntegName.equals(integNameFromCommand)) {
                                return true;
                            }
                        }
                }
            }
        } catch (Exception e) {
        }
        return false;
    }
}
