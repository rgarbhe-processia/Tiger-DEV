
/*
 ** DSCEBOMAccess
 **
 ** Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 ** 
 ** Program to use as to check the whether the user has access to Purge.
 */
import java.util.HashMap;
import java.util.StringTokenizer;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.MatrixWriter;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.IEFIntegAccessUtil;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.apps.domain.util.FrameworkUtil;

public class DSCEBOMAccess_mxJPO {
    String _sObjId;

    MatrixWriter _mxWriter = null;

    private MCADServerResourceBundle serverResourceBundle = null;

    private IEFGlobalCache cache = null;

    private IEFIntegAccessUtil mxUtil = null;

    /**
     * The no-argument constructor.
     */
    public DSCEBOMAccess_mxJPO() {
    }

    /**
     * Constructor which accepts the Matrix context and an array of String arguments.
     */
    public DSCEBOMAccess_mxJPO(Context context, String[] args) throws Exception {
        _mxWriter = new MatrixWriter(context);
    }

    public int mxMain(Context context, String[] args) throws Exception {
        return 0;
    }

    public Boolean checkAccess(Context context, String[] args) throws Exception {
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        String language = (String) paramMap.get("languageStr");

        serverResourceBundle = new MCADServerResourceBundle(language);
        cache = new IEFGlobalCache();
        mxUtil = new IEFIntegAccessUtil(context, serverResourceBundle, cache);

        if (isAEFInstalled(context))
            return new Boolean(true);
        else
            return new Boolean(false);
    }

    private boolean isAEFInstalled(Context context) {
        boolean isAEFInstalled = false;
        String installedAEFVersion = "";
        String Args[] = new String[1];
        Args[0] = "eServiceHelpAbout.tcl";
        String sResult = mxUtil.executeMQL(context, "execute program $1", Args);

        if (sResult.startsWith("true")) {
            boolean reachedFrameworkIndex = false;
            StringTokenizer appVersionTokens = new StringTokenizer(sResult, "|");
            while (appVersionTokens.hasMoreTokens()) {
                if (reachedFrameworkIndex) {
                    installedAEFVersion = appVersionTokens.nextToken();
                    break;
                }
                if ((appVersionTokens.nextToken()).equalsIgnoreCase("Framework"))
                    reachedFrameworkIndex = true;
            }
        }

        if (!installedAEFVersion.equals("")) {
            if (installedAEFVersion.startsWith("10-0") || installedAEFVersion.startsWith("10-5"))
                isAEFInstalled = true;
            else {

                Args = new String[2];
                Args[0] = "eServiceSystemInformation.tcl";
                Args[1] = "property[appSchemaAllMatrixApplications].value";
                String sResult1 = mxUtil.executeMQL(context, "print program $1 select $2 dump", Args);

                if (sResult.startsWith("true")) {
                    sResult1 = sResult1.substring(5);
                    if (!sResult1.equals(""))
                        isAEFInstalled = true;
                    else
                        isAEFInstalled = false;
                }
            }
        }

        return isAEFInstalled;
    }

    private boolean hasIntegrationAccess(Context context, String objectId, String language) {
        boolean hasIntegrationAccess = true;

        try {
            String integrationName = mxUtil.getIntegrationName(context, objectId);
            if (!mxUtil.getAssignedIntegrations(context).contains(integrationName))
                hasIntegrationAccess = false;
        } catch (Throwable e) {
        }

        return hasIntegrationAccess;
    }

    public Boolean canShowCommandForIntegUser(Context context, String[] args) throws Exception {
        boolean isShowCommand = true;

        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        String objectId = (String) paramMap.get("objectId");
        String language = (String) paramMap.get("languageStr");

        serverResourceBundle = new MCADServerResourceBundle(language);
        cache = new IEFGlobalCache();
        mxUtil = new IEFIntegAccessUtil(context, serverResourceBundle, cache);

        try {
            if ((!hasIntegrationAccess(context, objectId, language)) || (!canShowEbomSync(context, args)))
                isShowCommand = false;
        } catch (Throwable e) {
        }

        return new Boolean(isShowCommand);
    }

    public Boolean canShowEbomSync(Context context, String[] args) throws Exception {
        boolean isEbomSyncAccess = false;
        try {
            String ecVersion = FrameworkUtil.getApplicationVersion(context, "X-BOMEngineering");

            if (ecVersion != null && !ecVersion.equals(""))
                isEbomSyncAccess = true;

            if (isEbomSyncAccess) {
                HashMap paramMap = (HashMap) JPO.unpackArgs(args);
                String objectId = (String) paramMap.get("objectId");
                String language = (String) paramMap.get("languageStr");
                if (objectId != null && !"".equals(objectId)) {
                    String attrExcludeFromBOM = MCADMxUtil.getActualNameForAEFData(context, ("attribute_IEF-ExcludeFromBOM"));
                    MCADMxUtil util = new MCADMxUtil(context, new MCADServerResourceBundle(language), new IEFGlobalCache());
                    String excludeFromBOM = util.getAttributeForBO(context, objectId, attrExcludeFromBOM);
                    // if object is excluded then don't show EBOM command
                    if (excludeFromBOM != null && "true".equalsIgnoreCase(excludeFromBOM))
                        isEbomSyncAccess = false;
                }
            }
        } catch (Exception e) {
            isEbomSyncAccess = false;
        }

        return new Boolean(isEbomSyncAccess);
    }

}
