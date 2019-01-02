
/*
 ** DSCViewableFileBase
 **
 ** Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 **
 */
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.IEFSimpleConfigObject;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.IEFIntegAccessUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADUrlUtil;
import com.matrixone.MCADIntegration.utils.MCADUtil;
import com.matrixone.apps.common.CommonDocument;
import com.matrixone.apps.domain.util.MapList;

public class DSCViewableFileBase_mxJPO extends emxAPPQuickFileBase_mxJPO {

    protected MCADServerResourceBundle serverResourceBundle = null;

    protected IEFGlobalCache cache = null;

    protected IEFIntegAccessUtil mxUtil = null;

    /**
     * Constructor.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @throws Exception
     *             if the operation fails
     */
    public DSCViewableFileBase_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
    }

    /**
     * This method is executed if a specific method is not specified.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @return int 0, status code.
     * @throws Exception
     *             if the operation fails
     */
    public int mxMain(Context context, String[] args) throws Exception {
        if (!context.isConnected())
            throw new Exception("not supported on desktop client");
        return 0;
    }

    /**
     * This method gets the List of Files Checked into the Related Object
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @return HashMap contains list of Files
     * @throws Exception
     *             if the operation fails
     */
    public HashMap listReferenceDocuments(Context context, String[] args) throws Exception {
        String relFilter = "from[relationship_ActiveVersion].to.id,from[relationship_ActiveVersion].to.from[relationship_DerivedOutput].to.id,from[relationship_ActiveVersion].to.from[relationship_Viewable].to.id";
        String relFilterForFinalize = "id,from[relationship_DerivedOutput].to.id,from[relationship_Viewable].to.id";

        HashMap hmpInput = (HashMap) JPO.unpackArgs(args);
        HashMap paramMap = (HashMap) hmpInput.get("paramMap");
        HashMap commandMap = (HashMap) hmpInput.get("commandMap");
        HashMap settingsMap = (HashMap) commandMap.get("settings");
        HashMap requestMap = (HashMap) hmpInput.get("requestMap");
        String strlanguage = (String) requestMap.get("languageStr");
        String objectId = (String) paramMap.get("objectId");

        serverResourceBundle = new MCADServerResourceBundle(strlanguage);
        cache = new IEFGlobalCache();
        mxUtil = new IEFIntegAccessUtil(context, serverResourceBundle, cache);
        String integrationName = mxUtil.getIntegrationName(context, objectId);

        StringBuffer allowedFormats = new StringBuffer();

        IEFSimpleConfigObject simpleGCO = null;

        if (mxUtil.getUnassignedIntegrations(context).contains(integrationName))
            simpleGCO = IEFSimpleConfigObject.getSimpleGCOForUnassginedInteg(context, integrationName);
        else
            simpleGCO = IEFSimpleConfigObject.getSimpleGCO(context, integrationName);

        if (simpleGCO != null) {
            Hashtable typeClassMapping = simpleGCO.getAttributeAsHashtable(MCADMxUtil.getActualNameForAEFData(context, "attribute_MCADInteg-TypeClassMapping"), "\n", "|");
            String derivedOutputLikeTypes = (String) typeClassMapping.get("TYPE_DERIVEDOUTPUT_LIKE");
            Vector derivedOutputTypesList = MCADUtil.getVectorFromString(derivedOutputLikeTypes, ",");
            Vector typeFormatList = simpleGCO.getAttributeAsVector(MCADMxUtil.getActualNameForAEFData(context, "attribute_MCADInteg-TypeFormatMapping"), "\n");
            Hashtable typeFormatsTable = new Hashtable();
            Vector allowedFormatList = new Vector();

            for (int i = 0; i < typeFormatList.size(); i++) {
                String typeFormat = (String) typeFormatList.get(i);
                StringTokenizer typeFormatTokens = new StringTokenizer(typeFormat, "|");
                String type = typeFormatTokens.nextToken();
                String mxType_Format = typeFormatTokens.nextToken();
                StringTokenizer mxTypeFormatTokens = new StringTokenizer(mxType_Format, ",");
                String mxType = mxTypeFormatTokens.nextToken();
                String format = mxTypeFormatTokens.nextToken();

                if (typeFormatsTable.containsKey(type)) {
                    Vector formatList = (Vector) typeFormatsTable.get(type);
                    if (!formatList.contains(format)) {
                        formatList.add(format);
                    }
                } else {
                    Vector formatList = new Vector(1);

                    formatList.add(format);
                    typeFormatsTable.put(type, formatList);
                }
            }

            for (int i = 0; i < derivedOutputTypesList.size(); i++) {
                String derivedOutputType = (String) derivedOutputTypesList.get(i);

                if (!derivedOutputType.equalsIgnoreCase("attachment")) {
                    Vector formatList = (Vector) typeFormatsTable.get(derivedOutputType);

                    for (int j = 0; j < formatList.size(); j++) {
                        String allowedFormat = (String) formatList.get(j);
                        if (!allowedFormatList.contains(allowedFormat)) {
                            allowedFormatList.add(allowedFormat);
                            allowedFormats.append(allowedFormat);
                            allowedFormats.append(",");
                        }
                    }
                }
            }

            String strAlwdFormat = allowedFormats.toString();
            // To Remove last ,
            strAlwdFormat = strAlwdFormat.substring(0, strAlwdFormat.length() - 1);
            settingsMap.put("Allowed formats", strAlwdFormat);

            BusinessObject busObject = new BusinessObject(objectId);
            busObject.open(context);
            String policyName = busObject.getPolicy(context).getName();
            String currentState = mxUtil.getCurrentState(context, objectId);
            busObject.close(context);

            Hashtable policyFinalizationStateMap = simpleGCO.getAttributeAsHashtable(MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-FinalizationState"), "\n", "|");
            String finalizationState = (String) policyFinalizationStateMap.get(policyName);

            Hashtable policyReleasedStateMap = simpleGCO.getAttributeAsHashtable(MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-ReleasedState"), "\n", "|");
            String releasedState = (String) policyReleasedStateMap.get(policyName);

            if (currentState.equals(finalizationState) || currentState.equals(releasedState)) {
                settingsMap.put("Relationship Filter", relFilterForFinalize);
            } else {
                settingsMap.put("Relationship Filter", relFilter);
            }
        }

        String[] jpoArgs = JPO.packArgs(hmpInput);

        HashMap hmpDummy = super.listReferenceDocuments(context, jpoArgs);
        return hmpDummy;
    }

    public MapList getCheckedInFiles(Context context, String[] args) throws Exception {
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        String partId = (String) paramMap.get("objectId");
        StringList strFormatList = (StringList) paramMap.get("Allowed_Formats");
        boolean bolNoFormatchk = false;
        if (strFormatList == null || strFormatList.size() == 0) {
            bolNoFormatchk = true;
        }

        MapList CheckedInFiles = new MapList();

        StringList selectList = new StringList(3);
        selectList.add(CommonDocument.SELECT_FILE_NAME);
        selectList.add(CommonDocument.SELECT_FILE_FORMAT);
        selectList.add(CommonDocument.SELECT_FILE_SIZE);

        String file = "";
        String format = "";
        String fileSize = "";
        int fileSizeKB = 0;

        HashMap settings = new HashMap();
        settings.put("Registered Suite", "Components");
        settings.put("Image", "../common/images/iconSmallDocumentGray.gif");
        settings.put("Registered Suite", "Framework");
        settings.put("Pull Right", "false");

        boolean versionable = CommonDocument.allowFileVersioning(context, partId);
        MapList fileInDoc = null;
        String[] args1 = JPO.packArgs(paramMap);

        // To get the non Versioned file
        emxCommonFileUI_mxJPO fileUI = new emxCommonFileUI_mxJPO(context, null);
        fileInDoc = (MapList) fileUI.getNonVersionableFiles(context, args1);

        if (fileInDoc != null && fileInDoc.size() > 0) {
            Map fileMap = null;
            int noOfFiles = fileInDoc.size();
            for (int j = 0; j < noOfFiles; j++) {
                fileMap = (Map) fileInDoc.get(j);
                format = (String) fileMap.get("format.file.format");
                file = (String) fileMap.get("format.file.name");

                if ((bolNoFormatchk || strFormatList.contains(format)) && !"".equals(file)) {
                    String encodedFile = MCADUrlUtil.hexEncode(file);

                    HashMap hmpChildMap = new HashMap();
                    fileSize = (String) fileMap.get("format.file.size");
                    fileSizeKB = 0;
                    if (fileSize != null && !"".equals(fileSize)) {
                        fileSizeKB = Integer.parseInt(fileSize) / 1024;
                    }
                    hmpChildMap.put("type", "command");
                    hmpChildMap.put("label", file + " (" + fileSizeKB + " KB)");
                    hmpChildMap.put("description", "file details");
                    hmpChildMap.put("roles", new StringList("all"));

                    StringBuffer hrefBuffer = new StringBuffer("javascript:openWindow('../iefdesigncenter/DSCComponentCheckoutWrapper.jsp?objectId=");
                    hrefBuffer.append(partId);
                    hrefBuffer.append("&action=download&format=");
                    hrefBuffer.append(format);
                    hrefBuffer.append("&fileName=");
                    hrefBuffer.append(encodedFile);
                    hrefBuffer.append("&refresh=false')");
                    hmpChildMap.put("href", hrefBuffer.toString());
                    hmpChildMap.put("settings", settings);
                    CheckedInFiles.add(hmpChildMap);
                }
            }
        }
        return CheckedInFiles;

    }

}
