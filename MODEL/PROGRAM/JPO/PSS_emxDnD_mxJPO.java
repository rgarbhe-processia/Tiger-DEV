import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.fileupload.FileItem;

import com.matrixone.apps.common.CommonDocument;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MessageUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.framework.ui.UICache;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.BusinessType;
import matrix.db.BusinessTypeList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.RelationshipType;
import matrix.util.StringList;

public class PSS_emxDnD_mxJPO {

    // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PSS_emxDnD_mxJPO.class);

    // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - END
    public String checkinFile(Context context, String[] args) throws Exception {

        try {
            Map<?, ?> paramMap = (Map<?, ?>) JPO.unpackArgs(args);
            String sLanguage = (String) paramMap.get("language");
            String sOID = (String) paramMap.get("objectId");
            String sRelType = (String) paramMap.get("relationship");
            String sFolder = (String) paramMap.get("folder");
            String fromCustomForm = (String) paramMap.get("fromCustomForm");
            String parentId = (String) paramMap.get("parentId");
            List files = (List) paramMap.get("files");

            String documentCommand = (String) paramMap.get("documentCommand");
            DomainObject dObject = new DomainObject(sOID);
            String strType = PropertyUtil.getSchemaProperty(context, DomainObject.SYMBOLIC_type_DOCUMENTS);

            RelationshipType relTypes = new RelationshipType(sRelType);
            BusinessTypeList allowedFromTypes = relTypes.getFromTypes(context, true);
            StringList selectableList = new StringList();
            selectableList.add("type.kindof[" + strType + "]");
            selectableList.add(DomainObject.SELECT_TYPE);
            Map objectInfo = dObject.getInfo(context, selectableList);
            String sIsDocument = (String) objectInfo.get("type.kindof[" + strType + "]");
            String typeName = (String) objectInfo.get(DomainObject.SELECT_TYPE);
            BusinessType bType = allowedFromTypes.find(typeName);

            if (bType == null) {

                Locale locale = context.getLocale();
                String type1 = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", locale, "emxFramework.Type.Document");
                String key = "emxFramework.Type." + dObject.getTypeName().replace(' ', '_');
                String type2 = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", locale, key);
                String[] messageValues = new String[] { type1, type2 };
                String msg = MessageUtil.getMessage(context, null, "emxComponents.DragAndDrop.WarningMessage", messageValues, null, locale, "emxComponentsStringResource");
                return "ERROR" + msg;
            }
            // String strType = PropertyUtil.getSchemaProperty(context,DomainObject.SYMBOLIC_type_DOCUMENTS);
            // String sIsDocument = dObject.getInfo(context, "type.kindof["+strType+"]");

            if (UIUtil.isNullOrEmpty(sRelType)) {
                sRelType = "Reference Document";
            }
            if (UIUtil.isNullOrEmpty(documentCommand)) {
                documentCommand = "APPReferenceDocumentsTreeCategory";
            }

            Iterator iter = files.iterator();
            int index;
            String sFilename = "";
            FileItem file = null;
            File outfile = null;
            StringBuffer errorMessage = new StringBuffer();
            String errorMsg = EnoviaResourceBundle.getProperty(context, "emxComponentsStringResource", context.getLocale(), "emxComponents.CommonDocument.DocumentsAreNotLockedByUser");
            errorMessage.append(errorMsg);
            if (isDuplicateFile(context, sOID, files, errorMessage)) {
                return "ERROR" + errorMessage;
            }
            ContextUtil.startTransaction(context, true);

            if (sRelType.equals("Active Version") && sIsDocument.equalsIgnoreCase("TRUE")) {
                CommonDocument cDoc = new CommonDocument(sOID);
                String sName = cDoc.getName(context);
                while (iter.hasNext()) {
                    file = (FileItem) iter.next();
                    sFilename = file.getName();
                    if (sFilename.contains("/")) {
                        index = sFilename.lastIndexOf("/");
                        sFilename = sFilename.substring(index);
                    }
                    if (sFilename.contains("\\")) {
                        index = sFilename.lastIndexOf("\\");
                        sFilename = sFilename.substring(index + 1);
                    }
                    outfile = new File(sFolder + sFilename);
                    file.write(outfile);
                    // TIGTK-5408 : START
                    // cDoc.checkinFile(context, true, true, "", "generic", sFilename, sFolder);
                    // cDoc.createVersion(context, sFilename, sFilename, null);
                    cDoc.reviseVersion(context, sFilename, sFilename, null);
                    cDoc.checkinFile(context, true, true, "", "generic", sFilename, sFolder);
                    cDoc.setAttributeValue(context, "Title", sName);
                    // TIGTK-5408 : END
                    // outfile.delete();
                    // Find Bug Fix : PRiyanka Salunke : 15-Feb-2017 : START
                    String strErrorMsg = "File Not Deleted Successfully";
                    boolean flag = outfile.delete();
                    if (!flag) {
                        throw new Exception(strErrorMsg);
                    }
                    // Find Bug Fix : PRiyanka Salunke : 15-Feb-2017 : END
                }
            } else {

                String sObjGeneratorName = UICache.getObjectGenerator(context, "type_Document", "");
                String sName = DomainObject.getAutoGeneratedName(context, sObjGeneratorName, "");
                CommonDocument cDoc = new CommonDocument();
                cDoc.createObject(context, DomainObject.TYPE_DOCUMENT, sName, "0", DomainObject.POLICY_DOCUMENT, context.getVault().getName());

                while (iter.hasNext()) {
                    file = (FileItem) iter.next();
                    sFilename = file.getName();
                    if (sFilename.contains("/")) {
                        index = sFilename.lastIndexOf("/");
                        sFilename = sFilename.substring(index);
                    }
                    if (sFilename.contains("\\")) {
                        index = sFilename.lastIndexOf("\\");
                        sFilename = sFilename.substring(index + 1);
                    }
                    outfile = new File(sFolder + sFilename);
                    file.write(outfile);

                    cDoc.checkinFile(context, true, true, "", "generic", sFilename, sFolder);
                    cDoc.createVersion(context, sFilename, sFilename, null);
                    // Find Bug Fix : PRiyanka Salunke : 15-Feb-2017 : START
                    String strErrorMsg = "File Not Deleted Successfully";
                    boolean flag = outfile.delete();
                    if (!flag) {
                        throw new Exception(strErrorMsg);
                    }
                    // Find Bug Fix : PRiyanka Salunke : 15-Feb-2017 : END
                }
                cDoc.addRelatedObject(context, new RelationshipType(sRelType), true, sOID);
                cDoc.setAttributeValue(context, "Title", sName);
            }

            ContextUtil.commitTransaction(context);
            if (fromCustomForm.equals("true")) {
                return emxExtendedHeader_mxJPO.genHeaderDocuments(context, parentId, "Reference Document", "APPReferenceDocumentsTreeCategory", sLanguage, false);
            } else {
                return emxExtendedHeader_mxJPO.genHeaderDocuments(context, sOID, sRelType, documentCommand, sLanguage, false);
            }

        } catch (Exception ex) {
            ContextUtil.abortTransaction(context);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkinFile: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            return "ERROR" + ex.getMessage();
        }
    }

    private boolean isDuplicateFile(Context context, String objectID, List checkinFiles, StringBuffer error) throws Exception {
        DomainObject domainObject = DomainObject.newInstance(context, objectID);
        if (!(domainObject instanceof CommonDocument)) {
            return false;
        }
        StringList files = getFiles(context, domainObject);
        return isFilePresent(context, files, checkinFiles, error);
    }

    private StringList getFiles(Context context, DomainObject domainObject) throws Exception {
        if (domainObject instanceof CommonDocument) {
            CommonDocument object = (CommonDocument) domainObject;
            StringList selectList = new StringList();
            selectList.add(CommonDocument.SELECT_MOVE_FILES_TO_VERSION);
            selectList.add(CommonDocument.SELECT_ACTIVE_FILE_VERSION_ID);
            Map selectMap = object.getInfo(context, selectList);
            StringList selectFileList = new StringList();
            selectFileList.add(CommonDocument.SELECT_TITLE);
            selectFileList.add(CommonDocument.SELECT_LOCKER);
            return (StringList) selectMap.get(CommonDocument.SELECT_ACTIVE_FILE_VERSION_ID);
        }
        return null;
    }

    private boolean isFilePresent(Context context, StringList files, List checkinFiles, StringBuffer error) throws Exception {
        boolean isFileFound = false;
        if (files != null && !files.isEmpty()) {
            Iterator fileItr = files.iterator();
            while (fileItr.hasNext()) {
                Iterator revItr = getRevisionFiles(context, (String) fileItr.next()).iterator();
                while (revItr.hasNext()) {
                    Map fileMap = (Map) revItr.next();
                    if (fileFound(context, fileMap, checkinFiles, error)) {
                        isFileFound = true;
                    }
                }
            }
        }
        return isFileFound;
    }

    private MapList getRevisionFiles(Context context, String fileId) throws Exception {
        // TIGTK-5408 : START
        // DomainObject versionObj = DomainObject.newInstance(context, fileId);
        // TIGTK-5408 : END
        StringList selectFileList = new StringList();
        selectFileList.add(CommonDocument.SELECT_TITLE);
        selectFileList.add(CommonDocument.SELECT_LOCKER);
        // TIGTK-5408 : START
        // return versionObj.getRevisionsInfo(context, selectFileList, new StringList());
        MapList mpFilesInfoList = DomainObject.getInfo(context, new String[] { fileId }, selectFileList);
        return mpFilesInfoList;
        // TIGTK-5408 : END
    }

    private boolean fileFound(Context context, Map fileMap, List checkinFiles, StringBuffer error) {
        String fileTitle = (String) fileMap.get(CommonDocument.SELECT_TITLE);
        String fileLocker = (String) fileMap.get(CommonDocument.SELECT_LOCKER);
        String user = context.getUser();
        Iterator chekingFileItr = checkinFiles.iterator();
        boolean isFileFound = false;
        while (chekingFileItr.hasNext()) {
            FileItem file = (FileItem) chekingFileItr.next();
            String sFilename = file.getName();
            String[] sFilenamePath = sFilename.split("\\\\");
            sFilename = sFilenamePath[sFilenamePath.length - 1].trim();
            if (sFilename.equalsIgnoreCase(fileTitle) && !fileLocker.equalsIgnoreCase(user)) {
                error.append(" \n" + sFilename);
                isFileFound = true;
                break;
            }
        }
        return isFileFound;

    }
}