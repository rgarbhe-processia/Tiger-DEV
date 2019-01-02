package pss.document.policy;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import com.matrixone.apps.common.CommonDocument;
import com.matrixone.apps.common.Person;
import com.matrixone.apps.common.SubscriptionManager;
import com.matrixone.apps.common.UserTask;
import com.matrixone.apps.common.Workspace;
import com.matrixone.apps.common.WorkspaceVault;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.AccessUtil;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.XSSUtil;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.apps.domain.util.mxType;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.Access;
import matrix.db.BusinessObject;
import matrix.db.BusinessObjectList;
import matrix.db.Context;
import matrix.db.ExpansionWithSelect;
import matrix.db.JPO;
import matrix.db.RelationshipWithSelectItr;
import matrix.util.MatrixException;
import matrix.util.Pattern;
import matrix.util.SelectList;
import matrix.util.StringList;
import pss.constants.TigerConstants;

public class DocumentUtil_mxJPO {

    // TIGTK-5405 - 11-04-2017 - VB - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DocumentUtil_mxJPO.class);
    // TIGTK-5405 - 11-04-2017 - VB - END

    public int mxMain(Context context, String[] args) throws Exception {
        return 0;
    }

    /**
     * This Function will automatically promote the state to Released based on the value of attribute PSS_MandatoryDeliverable .
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param args
     *            holds objectId.
     * @throws Exception
     *             If the operation fails.
     */
    // Method - autoPromoteNonMandatoryDoc STARTS
    public void autoPromoteNonMandatoryDoc(Context context, String[] args) throws Exception {
        String strObjId = args[0];
        try {
            final String ATTRIBUTE_PSS_MANDATORY_DELIVERABLE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_MandatoryDeliverable");
            String ATTRIBUTE_PSS_MANDATORY_DELIVERABLE_VALUE = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", context.getLocale(),
                    "emxFramework.Range.PSS_MandatoryDelivrable.No");

            DomainObject domDoc = DomainObject.newInstance(context, strObjId);

            String strAttValue = domDoc.getAttributeValue(context, ATTRIBUTE_PSS_MANDATORY_DELIVERABLE);
            if (strAttValue.equals(ATTRIBUTE_PSS_MANDATORY_DELIVERABLE_VALUE)) {
                domDoc.promote(context);
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in autoPromoteNonMandatoryDoc: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
    }// Method - autoPromoteNonMandatoryDoc ENDS

    /**
     * This Function on promotion to Released State will change the Policy of the Previous Revision of Document to PSS_DocumentObsolete and disconnect this object from the Related Objects.
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param args
     *            holds objectId.
     * @throws Exception
     *             If the operation fails.
     */

    // Method - updatePreviousRevision STARTS
    public void updatePreviousRevision(Context context, String[] args) throws Exception {
        final String KEY = EnoviaResourceBundle.getProperty(context, "emxComponents.Document.Key_Util");
        String strObjId = args[0];
        try {
            DomainObject domDoc = DomainObject.newInstance(context, strObjId);

            BusinessObject boPreviousRev = domDoc.getPreviousRevision(context);
            if (boPreviousRev.exists(context)) {
                DomainObject domPrevRevObj = DomainObject.newInstance(context, boPreviousRev);
                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, TigerConstants.PERSON_USER_AGENT), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                MqlUtil.mqlCommand(context, "trigger off", true, false);
                domPrevRevObj.setPolicy(context, TigerConstants.POLICY_PSS_DOCUMENTOBSOLETE);
                MqlUtil.mqlCommand(context, "trigger on", true, false);
                ContextUtil.popContext(context);

                StringList slObjSelects = new StringList();
                slObjSelects.add(DomainConstants.SELECT_CURRENT);
                slObjSelects.add(DomainConstants.SELECT_TYPE);

                StringList slRelSelects = new StringList();
                slRelSelects.add(DomainRelationship.SELECT_ID);

                Pattern patRel = new Pattern(DomainConstants.RELATIONSHIP_REFERENCE_DOCUMENT);
                patRel.addPattern(DomainConstants.RELATIONSHIP_VAULTED_DOCUMENTS);
                MapList mlDoc = domPrevRevObj.getRelatedObjects(context, // context
                        patRel.getPattern(), // relationshipPattern
                        DomainConstants.QUERY_WILDCARD, // typePattern
                        slObjSelects, // objectSelects
                        slRelSelects, // relationshipSelects
                        true, // getTo
                        false, // getFrom
                        (short) 0, // recurseToLevel
                        null, // objectWhere
                        null, // relationshipWhere
                        (short) 0, // limit
                        null, // includeType
                        null, // includeRelationship
                        null); // includeMap

                for (int i = 0; i < mlDoc.size(); i++) {
                    Map<?, ?> mDoc = (Map<?, ?>) mlDoc.get(i);
                    String strRelId = (String) mDoc.get(DomainRelationship.SELECT_ID);
                    String strState = (String) mDoc.get(DomainConstants.SELECT_CURRENT);
                    String strType = (String) mDoc.get(DomainConstants.SELECT_TYPE);
                    String strSymType = PropertyUtil.getAliasForAdmin(context, "Type", strType, false);
                    String strKey = KEY + strSymType;
                    StringList slStateValues = FrameworkUtil.split(EnoviaResourceBundle.getProperty(context, strKey), "|");
                    if (slStateValues.contains(strState)) {
                        DomainRelationship.disconnect(context, strRelId);
                    }
                }

            }

        } // Fix for FindBugs issue RuntimeException capture
        catch (RuntimeException e) {
            logger.error("Error in updatePreviousRevision: ", e);
            throw e;
        } catch (Exception e) {
            logger.error("Error in updatePreviousRevision: ", e);
        }

    }// Method - updatePreviousRevision ENDS

    /**
     * This Function will Update the Revision Reason of the Document and connects the Document to the Related Object based on the Current State of the Related Object.
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param args
     *            holds objectId.
     * @throws Exception
     *             If the operation fails.
     */

    // Method - updateRevisionReason STARTS
    public void updateRevisionReason(Context context, String[] args) throws Exception {
        try {
            final String ATTRIBUTE_PSS_REVISIONREASON = PropertyUtil.getSchemaProperty(context, "attribute_PSS_RevisionReason");
            Map<?, ?> programMap = (Map<?, ?>) JPO.unpackArgs(args);
            Map<?, ?> paramMap = (Map<?, ?>) programMap.get("paramMap");
            String strObjId = (String) paramMap.get("objectId"); // Current Document Id
            String strRevReason = (String) paramMap.get("PSS_RevisionReason");
            DomainObject domDoc = DomainObject.newInstance(context, strObjId);
            // TIGTK-4133 - SteepGraph - 02-02-2017 - START
            domDoc = new DomainObject(domDoc.getLastRevision(context));
            // TIGTK-4133 - SteepGraph - 02-02-2017 - END
            String strDate = domDoc.getInfo(context, DomainConstants.SELECT_MODIFIED);
            String[] strDateArr = strDate.split("\\s+");
            String strCurDate = strDateArr[0];
            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            if (strRevReason.isEmpty()) {
                domDoc.setAttributeValue(context, ATTRIBUTE_PSS_REVISIONREASON, strCurDate);
            } else {
                domDoc.setAttributeValue(context, ATTRIBUTE_PSS_REVISIONREASON, strRevReason);
            }
            ContextUtil.popContext(context);
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in updateRevisionReason: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }

    }// Method - updateRevisionReason ENDS

    /**
     * This Function will Revise the Document Object and Connect it to the Related Parts.
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param args
     *            holds objectId.
     * @return String. revised Document Object Id
     * @throws Exception
     *             If the operation fails.
     */

    // Method - connectRevisedObject STARTS
    @SuppressWarnings("rawtypes")
    public String connectRevisedObject(Context context, String[] args) throws Exception {
        String objectId = null;
        try {
            final String RELATIONSHIP_REFERENCEDOCUMENT = PropertyUtil.getSchemaProperty(context, "relationship_ReferenceDocument");
            final String RELATIONSHIP_VAULTEDOBJECTS = PropertyUtil.getSchemaProperty(context, "relationship_VaultedDocuments");

            final String KEY = EnoviaResourceBundle.getProperty(context, "emxComponents.Document.Key_Util");

            HashMap programMap = (HashMap) JPO.unpackArgs(args);

            String[] oids = (String[]) programMap.get("emxTableRowId");
            objectId = (String) programMap.get("objectId");
            String oldobjectId = objectId;
            String strCopyFiles = (String) programMap.get("copyFiles");
            String nextRev = (String) programMap.get("nextRev");

            // added for bug 344195
            // boolean isRevised = false;

            DomainObject domDoc = DomainObject.newInstance(context, oldobjectId);
            StringList slObjSelects = new StringList();
            slObjSelects.add(DomainConstants.SELECT_ID);
            slObjSelects.add(DomainConstants.SELECT_CURRENT);
            slObjSelects.add(DomainConstants.SELECT_TYPE);
            slObjSelects.add(DomainConstants.SELECT_POLICY);
            StringList slRelSelects = new StringList();
            Pattern patRel = new Pattern(RELATIONSHIP_REFERENCEDOCUMENT);
            patRel.addPattern(RELATIONSHIP_VAULTEDOBJECTS);

            MapList mlDoc = domDoc.getRelatedObjects(context, // context
                    patRel.getPattern(), // relationshipPattern
                    DomainConstants.QUERY_WILDCARD, // typePattern
                    slObjSelects, // objectSelects
                    slRelSelects, // relationshipSelects
                    true, // getTo
                    false, // getFrom
                    (short) 0, // recurseToLevel
                    null, // objectWhere
                    null, // relationshipWhere
                    (short) 0, // limit
                    null, // includeType
                    null, // includeRelationship
                    null); // includeMap

            // User can reach this page in two ways,
            // One thru Document Summary page, where multiple rows can be selected for revise
            // Two thru Document properties page, in this case ONLY objectId will be passed
            if (oids == null) {
                oids = new String[] { objectId };
            }
            String strNewObjectId = "";
            if (oids != null) {
                Map<?, ?> objectMap = UIUtil.parseRelAndObjectIds(context, oids, false);
                oids = (String[]) objectMap.get("objectIds");
                boolean copyFiles = Boolean.parseBoolean(strCopyFiles);
                try {
                    for (int i = 0; i < oids.length; i++) {
                        // build business object/open/unlock/close
                        String oid = oids[i];

                        CommonDocument commonDocument = (CommonDocument) DomainObject.newInstance(context, oid);
                        String strIsVCDoc = commonDocument.getInfo(context, CommonDocument.SELECT_IS_KIND_OF_VC_DOCUMENT);
                        if (strIsVCDoc != null && strIsVCDoc.equalsIgnoreCase("true")) {
                            BusinessObject lastRev = commonDocument.getLastRevision(context);
                            BusinessObject newbo = lastRev.revise(context, lastRev.getNextSequence(context), lastRev.getVault());
                            strNewObjectId = newbo.getObjectId();
                            commonDocument.setId(strNewObjectId);
                        } else {
                            commonDocument = commonDocument.revise(context, nextRev, copyFiles);
                            objectId = commonDocument.getObjectId();
                        }
                        // isRevised = true;
                    }

                } catch (Exception ex) {
                    // TIGTK-5405 - 11-04-2017 - VB - START
                    logger.error("Error in connectRevisedObject: ", ex);
                    // TIGTK-5405 - 11-04-2017 - VB - END
                }

            }

            for (int i = 0; i < mlDoc.size(); i++) {
                Map<?, ?> mDoc = (Map<?, ?>) mlDoc.get(i);
                String strCurState = (String) mDoc.get(DomainConstants.SELECT_CURRENT);
                String strRelatedObjId = (String) mDoc.get(DomainConstants.SELECT_ID);
                String strType = (String) mDoc.get(DomainConstants.SELECT_TYPE);
                String strSymType = PropertyUtil.getAliasForAdmin(context, "Type", strType, false);
                String strKey = KEY + strSymType;
                StringList slStateValues = FrameworkUtil.split(EnoviaResourceBundle.getProperty(context, strKey), "|");
                DomainObject domRelObj = DomainObject.newInstance(context, strRelatedObjId);
                DomainObject domNewrev = DomainObject.newInstance(context, objectId);
                if (slStateValues.contains(strCurState) && !strType.equalsIgnoreCase(DomainConstants.TYPE_WORKSPACE_VAULT)) {
                    DomainRelationship doRel = DomainRelationship.connect(context, domRelObj, RELATIONSHIP_REFERENCEDOCUMENT, domNewrev);
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in connectRevisedObject: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }

        return objectId;
    }// Method - connectRevisedObject ENDS

    /**
     * This Function is used on trigger and On the command to UnObsolete the Document
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param args
     *            holds objectId.
     * @return int. 0 or 1
     * @throws Exception
     *             If the operation fails.
     */

    // Method - checkForUnCancelOrUnObsoleteDocument STARTS
    public int checkForUnCancelOrUnObsoleteDocument(Context context, String[] args) throws Exception {
        boolean isTrue = false;
        try {
            String strObjId = args[0];
            DomainObject domDoc = DomainObject.newInstance(context, strObjId);

            if (domDoc.isLastRevision(context)) {
                isTrue = true;
            } else {
                BusinessObject boDocNextRev = domDoc.getNextRevision(context);
                DomainObject domDocNextRev = DomainObject.newInstance(context, boDocNextRev);
                String strNextRevState = domDocNextRev.getInfo(context, DomainConstants.SELECT_CURRENT);
                if (strNextRevState.equals("InWork")) {
                    isTrue = true;
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in checkForUnCancelOrUnObsoleteDocument: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
        if (isTrue) {
            return 0;
        }
        return 1;
    }// Method - checkForUnCancelOrUnObsoleteDocument ENDS

    /**
     * This Function will connect the Existing Workspace to Program-Project
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param args
     *            holds objectId and tableRowIds.
     * @return
     * @throws Exception
     *             If the operation fails.
     */

    // Method - connectWorkspaceToProgramProject STARTS
    public void connectWorkspaceToProgramProject(Context context, String[] args) throws Exception {
        try {
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            String[] oids = (String[]) programMap.get("emxTableRowIdActual");
            String objectId = (String) programMap.get("objectId");
            DomainObject domProgProj = DomainObject.newInstance(context, objectId);

            for (int i = 0; i < oids.length; i++) {
                StringTokenizer strTokenizer = new StringTokenizer(oids[i], "|");
                String strWorkspaceObjId = strTokenizer.nextToken();
                DomainObject domWorkspace = DomainObject.newInstance(context, strWorkspaceObjId);

                StringList slSelectRelStmts = new StringList(1);
                slSelectRelStmts.addElement(DomainConstants.SELECT_ID);
                // PTE TIGTK-9359 : 25/08/2017 : START
                DomainRelationship doRel = DomainRelationship.connect(context, domProgProj, TigerConstants.RELATIONSHIP_PSS_CONNECTEDWORKSPACE, domWorkspace);
                // PTE TIGTK-9359 : 25/08/2017 : END
            }
        } catch (Exception e) {
            throw e;
        }

    } // Method - connectWorkspaceToProgramProject ENDS

    /**
     * Added this function to get ExcludedOIds of the Workspace.
     * @param context
     * @param args
     * @return StringList
     * @throws Exception
     */
    // Method - excludeConnectedWorkspaces STARTS
    @SuppressWarnings("rawtypes")
    public static Object excludeConnectedWorkspaces(Context context, String[] args) throws Exception {
        StringList excludeOID = new StringList();
        try {

            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            String objectId = (String) paramMap.get("objectId");
            String strRelationship = (String) paramMap.get("relationship");
            String strFieldtype = (String) paramMap.get("field_actual");
            strFieldtype = strFieldtype.replace("TYPES=", "");
            strFieldtype = strFieldtype.trim();

            StringTokenizer strToken = new StringTokenizer(strFieldtype, ",");
            int i = 0;
            String strTypeArr[] = new String[strToken.countTokens()];
            StringBuffer strtype = new StringBuffer();
            int k = strToken.countTokens();
            while (strToken.hasMoreTokens()) {
                strTypeArr[i] = strToken.nextToken();
                String[] temp = strTypeArr[i].split(":");

                strtype.append(PropertyUtil.getSchemaProperty(context, temp[0]));
                if (k != ++i) {
                    strtype.append(",");
                }
            }
            DomainObject domainObject = new DomainObject(objectId);
            StringList slSelectList = new StringList();
            slSelectList.add(DomainConstants.SELECT_ID);

            domainObject.open(context);
            // boolean c = domainObject.isOpen();
            String strRel = PropertyUtil.getSchemaProperty(context, strRelationship);

            MapList mlList = domainObject.getRelatedObjects(context, strRel, strtype.toString(), slSelectList, null, true, true, (short) 1, null, null, 0);
            if (mlList.size() > 0) {
                Iterator<?> itr = mlList.iterator();
                Map<?, ?> map;
                while (itr.hasNext()) {
                    map = (Map<?, ?>) itr.next();
                    excludeOID.add((String) map.get(DomainConstants.SELECT_ID));
                }
                excludeOID.add(objectId);
            }
            return excludeOID;
        } catch (Exception ex) {
            throw ex;
        }
    }// Method - excludeConnectedWorkspaces ENDS

    /**
     * Added this function to clone the Document.
     * @param context
     * @param args
     * @return String
     * @throws Exception
     */
    // Method - documentClone STARTS
    public String documentClone(Context context, String[] args) throws Exception {
        String strObjectId = "";
        final String TYPE_DOCUMENT = PropertyUtil.getSchemaProperty(context, "type_Document");
        // Fix for TIGTK-3037-Starts
        final String POLICY_PSS_DOCUMENTOBSOLETE = PropertyUtil.getSchemaProperty(context, "policy_PSS_DocumentObsolete");
        final String POLICY_PSS_DOCUMENT = PropertyUtil.getSchemaProperty(context, "policy_PSS_Document");
        // Fix for TIGTK-3037-Ends
        boolean isPushed = false;
        String strObjectGeneratorName = PropertyUtil.getAliasForAdmin(context, "Type", TYPE_DOCUMENT, false);
        try {
            String strObjId = args[0];
            String strOwner = context.getUser();
            DomainObject domObj = DomainObject.newInstance(context, strObjId);
            // Fix for TIGTK-3037-Starts
            String strPolicy = domObj.getInfo(context, DomainConstants.SELECT_POLICY);
            // Fix for TIGTK-3037-Ends
            String strAutoNumberSeries = null;
            String strAutoName = DomainObject.getAutoGeneratedName(context, strObjectGeneratorName, strAutoNumberSeries);
            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            isPushed = true;
            DomainObject clonedSourceObj = new DomainObject(domObj.cloneObject(context, strAutoName, null, null, true));
            strObjectId = clonedSourceObj.getId(context);
            // Fix for TIGTK-3037-Starts
            DomainObject domObjClone = DomainObject.newInstance(context, strObjectId);
            domObjClone.setAttributeValue(context, DomainConstants.ATTRIBUTE_TITLE, strAutoName);
            domObjClone.setOwner(context, strOwner);
            if (strPolicy.equals(POLICY_PSS_DOCUMENTOBSOLETE)) {
                domObjClone.setPolicy(context, POLICY_PSS_DOCUMENT);
            }
            // Fix for TIGTK-3037-Ends

            BusinessObject busClone = new BusinessObject(strObjectId);
            CommonDocument object = (CommonDocument) DomainObject.newInstance(context, strObjectId);
            // Version Object selects
            // get the file (Version Object) data

            // Fix for FindBugs issue Bad use of return value from method: Suchit Gangurde: 28 Feb 2017
            object = object.copyDocumentFiles(context, strObjId, busClone, true);
            if (object == null) {
                throw new RuntimeException("Clonned document object is null");
            }
            return strObjectId;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in documentClone: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        } finally {
            if (isPushed) {
                ContextUtil.popContext(context);
            }
        }
    } // Method - documentClone ENDS

    /**
     * Added this function to Delete the Previous Revision of the Version Object based on the value of attribute PSS_IsIterationsRequired.
     * @param context
     * @param args
     * @return void
     * @throws Exception
     */
    // Method - deleteAllPreviousIterationFiles STARTS
    public void deleteAllPreviousIterationFiles(Context context, String[] args) throws Exception {
        try {
            final String RELATIONSHIP_ACTIVE_VERSION = PropertyUtil.getSchemaProperty(context, "relationship_ActiveVersion");
            final String ATTRIBUTE_PSS_ISITERATIONREQUIRED = PropertyUtil.getSchemaProperty(context, "attribute_PSS_IsIterationsRequired");
            String ATTRIBUTE_PSS_ISITERATIONREQUIRED_VALUE = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", context.getLocale(), "emxFramework.Range.PSS_Template.No");
            String objectId = args[0];
            DomainObject domDoc = DomainObject.newInstance(context, objectId);
            String strAttrVal = domDoc.getAttributeValue(context, ATTRIBUTE_PSS_ISITERATIONREQUIRED);

            if (strAttrVal.equalsIgnoreCase(ATTRIBUTE_PSS_ISITERATIONREQUIRED_VALUE)) {
                StringList slVersionDoc = domDoc.getInfoList(context, "from[" + RELATIONSHIP_ACTIVE_VERSION + "].to.id");
                for (int itr = 0; itr < slVersionDoc.size(); itr++) {
                    String strVerObjId = (String) slVersionDoc.get(itr);
                    // DomainObject domVersion = DomainObject.newInstance(context, strVerObjId);
                    // BusinessObjectList bol = domVersion.getRevisions(context);
                    BusinessObject busObj = new BusinessObject(strVerObjId);
                    BusinessObjectList bol = busObj.getRevisions(context);
                    if (!bol.isEmpty()) {
                        for (int i = 0; i < bol.size(); i++) {
                            BusinessObject boVerObj = bol.getElement(i);
                            String strVerRevId = boVerObj.getObjectId();
                            if (!strVerRevId.equalsIgnoreCase(strVerObjId)) {
                                boVerObj.remove(context, true);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in deleteAllPreviousIterationFiles: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
    }

    // Method - deleteAllPreviousIterationFiles ENDS

    /**
     * Added this function to show if the Higher Revision Exists.
     * @param context
     * @param args
     * @return Vector
     * @throws Exception
     */
    // Added for HighestRevisionExists Start
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Vector<?> getHighestRevisionStatus(Context context, String[] args) throws Exception {
        String sTipConnectToHighestRev = EnoviaResourceBundle.getProperty(context, "emxTeamCentralStringResource", context.getLocale(), "emxTeamCentral.DocumentSummary.ToolTipConnectToHighestRev");

        String sTipHighestRevisionConnected = EnoviaResourceBundle.getProperty(context, "emxTeamCentralStringResource", context.getLocale(),
                "emxTeamCentral.DocumentSummary.ToolTipHighestRevisionConnected");
        Vector vActions = new Vector();
        String strObjId = "";
        String strRelId = "";
        String sHighestRevURL = "";

        StringList slDocs = new StringList();

        try {
            HashMap<?, ?> paramMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            MapList mlObjList = (MapList) paramMap.get("objectList");

            if (!mlObjList.isEmpty()) {
                for (int i = 0; i < mlObjList.size(); i++) {
                    Map<?, ?> mpObj = (Map<?, ?>) mlObjList.get(i);
                    strObjId = (String) mpObj.get(DomainConstants.SELECT_ID);
                    slDocs.add(strObjId);

                }
                for (int i = 0; i < mlObjList.size(); i++) {
                    StringBuffer strBuf = new StringBuffer();
                    Map<?, ?> mpObj = (Map<?, ?>) mlObjList.get(i);
                    strObjId = (String) mpObj.get(DomainConstants.SELECT_ID);
                    strRelId = (String) mpObj.get(DomainRelationship.SELECT_ID);
                    DomainObject domObjectDoc = DomainObject.newInstance(context, strObjId);
                    BusinessObject boLastRevisionObject = domObjectDoc.getLastRevision(context);
                    String docHighestId = boLastRevisionObject.getObjectId();
                    if (!strObjId.equalsIgnoreCase(docHighestId) && !slDocs.contains(docHighestId)) {
                        sHighestRevURL = "../components/PSS_ConnectWithHighestRevision.jsp?objectId=" + XSSUtil.encodeForJavaScript(context, docHighestId) + "&amp;relId="
                                + XSSUtil.encodeForJavaScript(context, strRelId);
                        strBuf.append("<a href=\"javascript:submitWithCSRF('" + sHighestRevURL + "',findFrame(getTopWindow(),'listHidden'));\">");
                        strBuf.append("<img border=\"0\" src=\"../common/images/iconSmallHigherRevision.gif\" alt=\"" + sTipConnectToHighestRev + "\" title=\"" + sTipConnectToHighestRev
                                + "\"></img></a>&#160;");
                    }

                    else if (!strObjId.equalsIgnoreCase(docHighestId) && slDocs.contains(docHighestId)) {
                        strBuf.append("<img border=\"0\" src=\"../common/images/iconSmallHigherRevision.gif\" alt=\"" + sTipHighestRevisionConnected + "\" title=\"" + sTipHighestRevisionConnected
                                + "\"></img>");
                    }

                    else {
                        strBuf.append("&#160;");
                    }
                    vActions.add(strBuf.toString());
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getHighestRevisionStatus: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }

        return vActions;
    }

    // Added for HighestRevisionExists End

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public MapList getDynamicAttributesForDocument(Context context, String args[]) throws Exception {

        final String SETTING_ADMIN_TYPE = "Admin Type";
        final String SETTING_FIELD_TYPE = "Field Type";
        final String SETTING_INPUT_TYPE = "Input Type";
        final String SETTING_REGISTERED_SUITE = "Registered Suite";
        final String SETTING_REQUIRED = "Required";
        final String EXPRESSION_BUSINESSOBJECT = "expression_businessobject";
        final String NAME = "name";
        final String LABEL = "label";
        final String SETTING_FORMAT = "format";

        final String INPUT_TYPE_COMBOBOX = "combobox";

        final String FIELD_TYPE_ATTRIBUTE = "attribute";
        final String REGISTERED_SUITE = "EngineeringCentral";
        final String SETTING_REQUIRED_YES = "true";
        final String SETTING_REQUIRED_NO = "false";

        final String FORMAT_TIMESTAMP = "timestamp";
        final String FORMAT_DATE = "date";
        final String FORMAT_INTEGER = "integer";
        final String FORMAT_BOOLEAN = "boolean";
        final String FORMAT_REAL = "real";
        final String FORMAT_NUMERIC = "numeric";
        final String FORMAT_STRING = "string";

        final String SETTING_ACCESS_EXPRESSION = "Access Expression";
        final String SETTING_ACCESS_EXPRESSION_FALSE = "false";

        MapList fieldMapList = new MapList();
        String strLanguage = context.getSession().getLanguage();

        HashMap programMap = (HashMap) JPO.unpackArgs(args);

        HashMap requestMap = (HashMap) programMap.get("requestMap");
        String strDocumentObjectId = (String) requestMap.get("objectId");

        String strMode = "";
        if (requestMap.containsKey("mode")) {
            strMode = (String) requestMap.get("mode");
        } else {
            strMode = "view";
        }
        StringList lstMandatoryAttributesList = new StringList();
        String strTypeName = "";
        if (UIUtil.isNotNullAndNotEmpty(strDocumentObjectId)) {
            DomainObject domDocumentObject = DomainObject.newInstance(context, strDocumentObjectId);
            String strType = domDocumentObject.getInfo(context, DomainConstants.SELECT_TYPE);
            strTypeName = strType;
        } else {
            String strDocumentType = (String) requestMap.get("type");
            StringList lstSelectedDocumentList = new StringList();
            if (strDocumentType.startsWith("_selectedType:")) {
                StringList lstDocumentTypeList = FrameworkUtil.split(strDocumentType, ",");
                String strDocumentSelectedType = (String) lstDocumentTypeList.get(0);
                lstSelectedDocumentList = FrameworkUtil.split(strDocumentSelectedType, ":");
                strTypeName = (String) lstSelectedDocumentList.get(1);
            } else {
                strTypeName = strDocumentType;
            }
        }
        String strSymbTypeName = FrameworkUtil.getAliasForAdmin(context, DomainConstants.SELECT_TYPE, strTypeName, true);
        MapList mlAttributeList = mxType.getAttributes(context, strTypeName, false);
        int intAttributeListSize = mlAttributeList.size();

        String strMandatoryAttributeNames = EnoviaResourceBundle.getProperty(context, "emxComponentsStringResource", context.getLocale(),
                "emxComponents.PSS_Document.MandatoryAttributeList." + strSymbTypeName);

        if (UIUtil.isNotNullAndNotEmpty(strMandatoryAttributeNames)) {
            lstMandatoryAttributesList = FrameworkUtil.split(strMandatoryAttributeNames, ",");
        }

        String strExcludedAttributeNames = "";
        if (strMode.equalsIgnoreCase("edit")) {
            strExcludedAttributeNames = EnoviaResourceBundle.getProperty(context, "emxComponentsStringResource", context.getLocale(),
                    "emxComponents.PSS_Document.MandatoryAttributeList." + strSymbTypeName + ".ExcludeAttributeList.edit");
        } else {
            strExcludedAttributeNames = EnoviaResourceBundle.getProperty(context, "emxComponentsStringResource", context.getLocale(),
                    "emxComponents.PSS_Document.MandatoryAttributeList." + strSymbTypeName + ".ExcludeAttributeList.view");
        }

        StringList lstExcludedAttributeNames = FrameworkUtil.split(strExcludedAttributeNames, ",");
        for (int intIndex = 0; intIndex < intAttributeListSize; intIndex++) {

            Map mapAttributeDetailsMap = (HashMap) mlAttributeList.get(intIndex);

            HashMap<String, Serializable> fieldMap = new HashMap<String, Serializable>();
            HashMap<String, String> settingsMap = new HashMap<String, String>();

            String strAttributeName = (String) mapAttributeDetailsMap.get("name");

            String strAttributeType = (String) mapAttributeDetailsMap.get("type");
            String strSymbolicAttrName = FrameworkUtil.getAliasForAdmin(context, "attribute", strAttributeName, true);
            // TIGTK- 10645 START 13-oct-2017
            settingsMap.put(SETTING_ADMIN_TYPE, strSymbolicAttrName);
            // TIGTK- 10645 END 13-oct-2017
            if (strAttributeType.equals(FORMAT_TIMESTAMP)) {
                settingsMap.put(SETTING_FORMAT, FORMAT_DATE);
            } else if (strAttributeType.equals(FORMAT_BOOLEAN)) {
                settingsMap.put(SETTING_INPUT_TYPE, INPUT_TYPE_COMBOBOX);
            } else if (strAttributeType.equals(FORMAT_INTEGER)) {
                settingsMap.put(SETTING_FORMAT, FORMAT_INTEGER);
            } else if (strAttributeType.equals(FORMAT_REAL)) {
                settingsMap.put(SETTING_FORMAT, FORMAT_NUMERIC);
            } else if (strAttributeType.equals(FORMAT_STRING)) {
                StringList range = (StringList) mapAttributeDetailsMap.get("choices");
                if (range != null && range.size() > 0) {
                    settingsMap.put(SETTING_INPUT_TYPE, INPUT_TYPE_COMBOBOX);
                    settingsMap.put(SETTING_FORMAT, FORMAT_STRING);
                }
            }

            // TIGTK-4791 - 06-03-2017 - START
            if (strSymbolicAttrName.equalsIgnoreCase("attribute_PSS_PMSITEM")) {
                settingsMap.put("Validate", "validatePMSItem");
            }
            // TIGTK-4791 - 06-03-2017 - END

            settingsMap.put(SETTING_FIELD_TYPE, FIELD_TYPE_ATTRIBUTE);

            fieldMap.put(NAME, strAttributeName);
            fieldMap.put(LABEL, i18nNow.getAttributeI18NString(strAttributeName, strLanguage));
            fieldMap.put(EXPRESSION_BUSINESSOBJECT, "attribute[" + strAttributeName + "].value");
            fieldMap.put(SETTING_ADMIN_TYPE, "attribute_[" + strAttributeName + "]");

            fieldMap.put(SETTING_REGISTERED_SUITE, REGISTERED_SUITE);
            // settingsMap.put(SETTING_REQUIRED, SETTING_REQUIRED_YES);
            if (lstExcludedAttributeNames.contains(strSymbolicAttrName)) {
                settingsMap.put(SETTING_ACCESS_EXPRESSION, SETTING_ACCESS_EXPRESSION_FALSE);
            }
            if (lstMandatoryAttributesList.contains(strSymbolicAttrName)) {
                settingsMap.put(SETTING_REQUIRED, SETTING_REQUIRED_YES);
            } else {
                settingsMap.put(SETTING_REQUIRED, SETTING_REQUIRED_NO);
            }
            fieldMap.put("settings", settingsMap);
            fieldMapList.add(fieldMap);
        }
        fieldMapList.sortStructure(context, DomainConstants.SELECT_NAME, "ascending", "emxSortNumericAlphaSmallerBase");
        return fieldMapList;
    }

    /**
     * This method checks if all the mandatory attributes are filled
     * @param context
     * @param args
     *            --Object Id of Document
     * @return -- '0'if success...'1' for failure with error message
     * @throws Exception
     */

    public int checkMandatoryAttributesFilled(Context context, String[] args) throws Exception {
        int result = 0;

        try {
            String strLanguage = context.getSession().getLanguage();
            final String ATTR_VALUE_UNASSIGNED = "UNASSIGNED";
            String strDocumentObjectID = args[0];

            DomainObject domDocumentObject = DomainObject.newInstance(context, strDocumentObjectID);

            String strMandatoryAttribute = EnoviaResourceBundle.getProperty(context, "emxComponentsStringResource", context.getLocale(),
                    "emxComponents.PSS_Document.MandatoryAttributeList.type_PSS_Document");

            if (UIUtil.isNotNullAndNotEmpty(strMandatoryAttribute)) {
                String attrName = null;

                StringList attrList = new StringList();
                String[] mandatoryAttr = strMandatoryAttribute.split(",");

                for (int j = 0; j < mandatoryAttr.length; j++) {
                    attrName = PropertyUtil.getSchemaProperty(context, mandatoryAttr[j]);
                    attrList.addElement(attrName);
                }
                StringBuffer strbufAlertMessage = new StringBuffer();
                String strAlertMessage = EnoviaResourceBundle.getProperty(context, "emxComponentsStringResource", context.getLocale(),
                        "emxComponents.PSS_Document.Alert.PSS_MandatoryAttributesNotFilled");

                strbufAlertMessage.append(strAlertMessage);

                // check whether mandatory attributes are filled
                for (int k = 0; k < attrList.size(); k++) {
                    String attrValue = domDocumentObject.getAttributeValue(context, (String) attrList.get(k));

                    String attrNameOriginal = null;
                    String attrNameForDisplay = null;

                    attrNameOriginal = attrList.get(k).toString();
                    attrNameForDisplay = i18nNow.getAttributeI18NString(attrNameOriginal, strLanguage);

                    if (attrValue.equalsIgnoreCase(ATTR_VALUE_UNASSIGNED) || UIUtil.isNullOrEmpty(attrValue)) {
                        // strAlertMessage = strAlertMessage + "\n " +attrNameForDisplay;
                        strbufAlertMessage.append("\n");
                        strbufAlertMessage.append(attrNameForDisplay);

                        result = 1;
                        // break;
                    } else if (result == 0) {
                        result = 0;
                    }
                }
                if (result == 1) {
                    String strAlertMessageDisplay = strbufAlertMessage.toString();
                    MqlUtil.mqlCommand(context, "notice $1", strAlertMessageDisplay);
                }
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in checkMandatoryAttributesFilled: ", ex);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
        return result;
    }

    /**
     * TIGTK-4128 : Fix This method for folder deletion Alert
     * @param context
     * @param args
     * @throws Exception
     */

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public HashMap folderDeleteAlert(Context arg0, String[] args) throws Exception {
        String arg4 = DomainObject.EMPTY_STRING;
        Map ProgramMap = JPO.unpackArgs(args);
        String arg1 = (String) ProgramMap.get("sProjectIds");
        String arg2 = (String) ProgramMap.get("objectId");

        String arg5 = EnoviaResourceBundle.getProperty(arg0, "emxTeamCentralStringResource", arg0.getLocale(), "emxTeamCentral.FolderDelete.MsgLinkedFolder");
        String arg6 = "";
        boolean arg7 = true;
        boolean arg8 = false;
        boolean arg9 = false;
        Workspace arg10 = (Workspace) DomainObject.newInstance(arg0, DomainConstants.TYPE_WORKSPACE, "Team");
        arg10.setId(arg2);
        String arg11 = arg2;
        if (arg10.getInfo(arg0, "type").equals(Workspace.TYPE_PROJECT_VAULT)) {
            arg9 = true;
            arg11 = UserTask.getProjectId(arg0, arg2);
            arg10.setId(arg11);
        }

        arg8 = arg10.isProjectLead(arg0, Person.getPerson(arg0));
        arg1 = arg1 != null && arg1.endsWith(";") ? arg1.substring(0, arg1.length() - 1) : arg1;
        StringList arg12 = FrameworkUtil.split(arg1, ";");

        int arg13;
        // Findbug Issue correction start
        // Date: 22/03/2017
        // By: Asha G.
        BusinessObject arg14;
        StringBuffer sbMessage = new StringBuffer();
        for (arg13 = 0; arg13 < arg12.size(); ++arg13) {
            arg6 = (String) arg12.get(arg13);
            if (isLinkedFolder(arg0, arg2, arg6)) {
                arg7 = false;
                arg14 = new BusinessObject(arg6);
                // Findbug Issue correction end
                arg14.open(arg0);
                sbMessage.append(arg5);
                sbMessage.append(arg14.getName());
                sbMessage.append(",");
                arg5 = sbMessage.toString();
                arg14.close(arg0);
            }
        }
        if (!arg7) {
            arg4 = arg5;
        }
        if (arg7) {
            StringBuffer sbMessageCheck = new StringBuffer();
            for (arg13 = 0; arg13 < arg12.size(); ++arg13) {
                arg6 = (String) arg12.get(arg13);
                if (!canDeleteFolder(arg0, arg6)) {
                    arg7 = false;
                    arg14 = DomainObject.newInstance(arg0, arg6);
                    arg14.open(arg0);
                    sbMessageCheck.append(arg4);
                    sbMessageCheck.append(arg14.getName());
                    sbMessageCheck.append(",");
                    arg4 = sbMessageCheck.toString();
                    arg14.close(arg0);
                }
            }

            String arg19 = arg0.getUser();
            arg4 = EnoviaResourceBundle.getProperty(arg0, "emxTeamCentralStringResource", arg0.getLocale(), "emxTeamCentral.FolderDelete.MsgConnected");

            StringBuffer sbMessageUpdated = new StringBuffer();
            for (int arg15 = 0; arg15 < arg12.size(); ++arg15) {
                arg6 = (String) arg12.get(arg15);
                DomainObject arg16 = DomainObject.newInstance(arg0, arg6);
                arg16.open(arg0);
                Access arg17 = arg16.getAccessMask(arg0);
                String arg18 = arg16.getInfo(arg0, "owner");
                if (!arg19.equals(arg18) && !AccessUtil.hasRemoveAccess(arg17) && !arg8) {
                    arg7 = false;
                }

                if (!arg7) {
                    sbMessageUpdated.append(arg4);
                    sbMessageUpdated.append(arg16.getName());
                    sbMessageUpdated.append(",");
                    arg4 = sbMessageUpdated.toString();
                }

                arg16.close(arg0);
            }
        }

        if (arg4.endsWith(",")) {
            arg4 = arg4.substring(0, arg4.length() - 1);
        }

        HashMap arg20 = new HashMap();
        arg20.put("errorMessage", arg4);
        arg20.put("canDelete", Boolean.valueOf(arg7));
        arg20.put("sProjectId", arg6);
        arg20.put("subfold", Boolean.valueOf(arg9));
        arg20.put("sProjectId", arg6);
        arg20.put("workspaceId", arg11);
        return arg20;
    }

    @SuppressWarnings("rawtypes")
    public static boolean isLinkedFolder(Context arg, String arg0, String arg1) throws Exception {
        if (arg == null) {
            throw new Exception("invalid context ");
        } else if (UIUtil.isNotNullAndNotEmpty(arg0.trim())) {
            if (UIUtil.isNotNullAndNotEmpty(arg1.trim())) {
                arg0 = arg0.trim();
                arg1 = arg1.trim();
                String arg2 = PropertyUtil.getSchemaProperty(arg, "relationship_LinkedFolders");
                String arg3 = "from[" + arg2 + "].to.id";
                DomainObject arg4 = DomainObject.newInstance(arg, arg0);
                StringList arg5 = arg4.getInfoList(arg, arg3);
                Iterator arg6 = arg5.iterator();

                String arg7;
                do {
                    if (!arg6.hasNext()) {
                        return false;
                    }

                    arg7 = (String) arg6.next();
                } while (!arg1.equals(arg7));

                return true;
            } else {
                throw new Exception("invalid folder id " + arg1);
            }
        } else {
            throw new Exception("invalid parent id " + arg0);
        }
    }

    public static boolean canDeleteFolder(Context arg, String arg0) throws MatrixException {
        String arg1 = PropertyUtil.getSchemaProperty(arg, "relationship_SubVaults");
        String arg2 = PropertyUtil.getSchemaProperty(arg, "relationship_VaultedDocuments");
        String arg3 = PropertyUtil.getSchemaProperty(arg, "relationship_RouteScope");
        String arg4 = PropertyUtil.getSchemaProperty(arg, "relationship_Message");
        String arg5 = PropertyUtil.getSchemaProperty(arg, "relationship_Thread");
        String arg6 = PropertyUtil.getSchemaProperty(arg, "type_ProjectVault");
        String arg7 = PropertyUtil.getSchemaProperty(arg, "type_DOCUMENTS");
        String arg16 = PropertyUtil.getSchemaProperty(arg, "type_PSS_Portfolio");
        String arg8 = PropertyUtil.getSchemaProperty(arg, "type_Route");
        String arg9 = PropertyUtil.getSchemaProperty(arg, "type_Message");
        String arg10 = PropertyUtil.getSchemaProperty(arg, "type_Thread");
        RelationshipWithSelectItr arg11 = null;
        Pattern arg12 = null;
        Pattern arg13 = null;
        ExpansionWithSelect arg14 = null;
        // Findbug Issue correction start
        // Date: 22/03/2017
        // By: Asha G.
        BusinessObject arg15 = new BusinessObject(arg0);
        // Findbug Issue correction end
        arg15.open(arg);
        arg12 = new Pattern(arg1);
        arg12.addPattern(arg5);
        arg12.addPattern(arg4);
        arg13 = new Pattern(arg6);
        arg13.addPattern(arg10);
        arg13.addPattern(arg9);
        arg14 = arg15.expandSelect(arg, arg12.getPattern(), arg13.getPattern(), new SelectList(), new SelectList(), false, true, (short) 0);
        arg11 = new RelationshipWithSelectItr(arg14.getRelationships());

        do {
            if (!arg11.next()) {
                arg12 = new Pattern(arg1);
                arg12.addPattern(arg3);
                arg13 = new Pattern(arg6);
                arg13.addPattern(arg8);
                arg14 = arg15.expandSelect(arg, arg12.getPattern(), arg13.getPattern(), new SelectList(), new SelectList(), false, true, (short) 0);
                arg11 = new RelationshipWithSelectItr(arg14.getRelationships());

                do {
                    if (!arg11.next()) {
                        arg12 = new Pattern(arg1);
                        arg12.addPattern(arg2);
                        arg13 = new Pattern(arg6);
                        arg13.addPattern(arg7);
                        arg13.addPattern(arg16);
                        arg14 = arg15.expandSelect(arg, arg12.getPattern(), arg13.getPattern(), new SelectList(), new SelectList(), false, true, (short) 0);
                        arg11 = new RelationshipWithSelectItr(arg14.getRelationships());

                        do {
                            if (!arg11.next()) {
                                arg15.close(arg);
                                return true;
                            }
                        } while (!arg11.obj().getTypeName().equals(arg2));

                        arg15.close(arg);
                        return false;
                    }
                } while (!arg11.obj().getTypeName().equals(arg3));

                arg15.close(arg);
                return false;
            }
        } while (!arg11.obj().getTypeName().equals(arg4));

        arg15.close(arg);
        return false;
    }

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     *             This method for Block the Demotion of Document Object while new Revision is already Exist
     * @owner Vishal Bhosale : 19/05/17
     */
    public int checkNewRevisionOfDocument(Context context, String[] args) throws Exception {
        try {
            String strObjectID = args[0];
            DomainObject domObject = DomainObject.newInstance(context, strObjectID);
            BusinessObject boObject = domObject.getLastRevision(context);
            String strID = boObject.getObjectId();
            if (!strObjectID.equalsIgnoreCase(strID)) {
                String strMessage = EnoviaResourceBundle.getProperty(context, "emxTeamCentralStringResource", context.getLocale(), "emxTeamCentral.common.DocumentDemoteErrorMsg");
                MqlUtil.mqlCommand(context, "notice $1", strMessage);
                return 1;
            }
        } catch (Exception ex) {
            logger.error("Exception in  DocumentUtil : checkNewRevisionOfDocument() method  ", ex);
            throw ex;
        }
        return 0;
    }

    // DOC TIGTK-8463: 6/15/2017 : PTE : START
    /**
     * This method is called as "Range Function" on "Keep all iterations after Released" field of Document creatiom.
     * @param context
     * @param args
     * @author -- PTE
     * @return Map - Contains the attribute ranges
     * @throws Exception
     */

    public HashMap getKeepAllIterationsAfterReleasedOptions(Context context, String[] args) throws Exception {
        HashMap rangeMap = new HashMap();
        try {
            StringList strListIsIterationsRequired = FrameworkUtil.getRanges(context, "PSS_IsIterationsRequired");
            strListIsIterationsRequired.add("");
            rangeMap.put("field_choices", strListIsIterationsRequired);
            rangeMap.put("field_display_choices", strListIsIterationsRequired);

        } catch (Exception ex) {
            logger.error("Error in getKeepAllIterationsAfterReleasedOptions: ", ex);
        }
        return rangeMap;
    }
    // DOC TIGTK-8463: 6/15/2017 : PTE : END

    // TIGTK-4138 30-Aug-2017 : Sayali D : START
    /**
     * This Method checks for state of connected Portfolio. Blocks document demote if connected Portfolio is Released
     * @param context
     * @param args
     *            -- "Object Id" of context Object
     * @return int - Returns the status whether to Promote or Restrict the Context Object
     * @throws Exception
     */
    public int checkForAssociatedPortfolioState(Context context, String[] args) throws Exception {

        int intRestrictOrPromoteStatus = 0;
        String strDocObjId = args[0];
        DomainObject domDocObj = DomainObject.newInstance(context, strDocObjId);
        StringList lstSelectStmts = new StringList();
        StringList lstRelStmts = new StringList();
        lstSelectStmts.add(DomainConstants.SELECT_ID);
        lstSelectStmts.add(DomainConstants.SELECT_NAME);
        lstSelectStmts.add(DomainConstants.SELECT_CURRENT);
        lstSelectStmts.add(DomainConstants.SELECT_POLICY);
        lstRelStmts.add(DomainConstants.SELECT_RELATIONSHIP_ID);
        Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_PORTFOLIO);
        Pattern relPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_PORTFOLIO);
        StringBuffer sWhereExp = new StringBuffer();
        sWhereExp.append("(");
        sWhereExp.append("current matchlist '");
        sWhereExp.append(TigerConstants.STATE_PSS_PORTFOLIO_REVIEW);
        sWhereExp.append(",");
        sWhereExp.append(TigerConstants.STATE_PSS_PORTFOLIO_RELEASED);
        sWhereExp.append(",");
        sWhereExp.append(TigerConstants.STATE_PSS_PORTFOLIO_OBSOLETE);
        sWhereExp.append("' ','");
        sWhereExp.append(")");

        // MapList containing "Portfolio" connected to "Document" with "PSS_Portfolio" relationship
        MapList mlConnectedPortfolioList = domDocObj.getRelatedObjects(context, relPattern.getPattern(), // pattern to match relationships
                typePattern.getPattern(), // pattern to match types
                lstSelectStmts, // object selects
                lstRelStmts, // relationship selects
                true, // get TO
                false, // get FROM
                (short) 1, // recurseToLevel
                sWhereExp.toString(), // where clause to apply on object
                null, // where clause to apply on relationship
                0); // limit

        int intConnectedPortfolioListSize = mlConnectedPortfolioList.size();
        if (intConnectedPortfolioListSize > 0) {
            intRestrictOrPromoteStatus = 1;
            StringBuffer strbufAlertMessage = new StringBuffer();
            String strAlertMessage = EnoviaResourceBundle.getProperty(context, "emxComponentsStringResource", context.getLocale(), "emxComponents.PSS_Document.Alert.PSS_PortfolioReleased");
            strbufAlertMessage.append(strAlertMessage);
            MqlUtil.mqlCommand(context, "notice $1", strbufAlertMessage.toString());
        }
        return intRestrictOrPromoteStatus;
    }// end of Method
     // TIGTK-4138 30-Aug-2017 : Sayali D : END

    // TIGTK-9548 :5/9/2017:START
    /****
     * JPO to check if user can checkin Document on context object using drag drop
     * @param context
     * @param args
     * @return String
     * @throws Exception
     */

    public String isReferenceDocumentConnectionAllowed(Context context, String args[]) throws Exception {
        logger.debug("pss.document.policy.DocumentUtil:isReferenceDocumentConnectionAllowed:START");
        String strIsAllowedUser = DomainConstants.EMPTY_STRING;
        try {

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strObjectId = (String) programMap.get("objectId");

            // Create DomainObject of Issue from Issue ID and get State of Issue.
            DomainObject domContextObject = DomainObject.newInstance(context, strObjectId);

            // Get type of Context Object
            String strObjectType = domContextObject.getInfo(context, DomainConstants.SELECT_TYPE);

            if (TigerConstants.TYPE_PSS_ISSUE.equalsIgnoreCase(strObjectType)) {
                pss.issue.IssueManagement_mxJPO issueManageObject = new pss.issue.IssueManagement_mxJPO();
                boolean hasAccess = issueManageObject.showIssueReferenceDocumentsCommands(context, args);
                if (!hasAccess) {
                    strIsAllowedUser = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                            "PSS_EnterpriseChangeMgt.Alert.NotAllowedReferenceDocConnection");
                }
            }

        } catch (Exception e) {
            logger.debug("pss.document.policy.DocumentUtil:isReferenceDocumentConnectionAllowed::ERROR ", e);
        }
        return strIsAllowedUser;
    }
    // TIGTK-9548 :5/9/2017:END

    /**
     * This method is executed after create of document object to send notification through trigger.
     * @param context
     * @param args
     *            Folder Id , Document ID
     * @return void
     * @throws Exception
     * @since : 12-12-2017 : TIGTK-11233
     * @author psalunke
     */

    public static void sendNotificationForContentAddOnFolder(Context context, String[] args) throws Exception {
        try {
            logger.error("\n pss.document.policy.DocumentUtil : sendNotificationForContentAddOnFolder() : START ");
            String folderId = args[0];
            String documentId = args[1];
            WorkspaceVault folder = (WorkspaceVault) DomainObject.newInstance(context, folderId);
            Workspace workspace = null;
            SubscriptionManager wsSubMgr = null;
            String wsId = "";
            DomainObject domObj = new DomainObject(folderId);
            StringList selStmts = new StringList();
            selStmts.add(DomainObject.SELECT_ID);
            selStmts.add(DomainObject.SELECT_TYPE);
            Pattern typePattern = new Pattern(DomainObject.TYPE_WORKSPACE_VAULT);
            typePattern.addPattern(DomainObject.TYPE_WORKSPACE);
            Pattern relPattern = new Pattern(DomainConstants.RELATIONSHIP_PROJECT_VAULTS);
            relPattern.addPattern(DomainConstants.RELATIONSHIP_SUB_VAULTS);
            Pattern includeTypePattern = new Pattern(DomainObject.TYPE_WORKSPACE);
            MapList wksList = domObj.getRelatedObjects(context, relPattern.getPattern(), typePattern.getPattern(), selStmts, new StringList(), true, false, (short) 0, "", "", 0, includeTypePattern,
                    null, null);
            Iterator listItr = wksList.iterator();
            Map objMap = null;
            while (listItr.hasNext()) {
                objMap = (Map) listItr.next();
                wsId = ((String) objMap.get(DomainObject.SELECT_ID));
            }
            // TIGTK-11223 : START
            if (UIUtil.isNotNullAndNotEmpty(wsId)) {
                // TIGTK-11223 : END
                workspace = (Workspace) DomainObject.newInstance(context, wsId);
                wsSubMgr = new SubscriptionManager(workspace);

                if (UIUtil.isNotNullAndNotEmpty(documentId)) {
                    SubscriptionManager subscriptionMgr = new SubscriptionManager(folder);
                    subscriptionMgr.publishEvent(context, folder.EVENT_CONTENT_ADDED, documentId);
                    if (wsSubMgr != null) {
                        wsSubMgr.publishEvent(context, workspace.EVENT_FOLDER_CONTENT_MODIFIED, documentId);
                    }
                }
            }
            logger.error("\n pss.document.policy.DocumentUtil : sendNotificationForContentAddOnFolder() : END ");
        } catch (Exception e) {
            logger.error("Error in pss.document.policy.DocumentUtil : sendNotificationForContentAddOnFolder() : ", e);
            throw e;
        }
    }

    /**
     * This method checks if the value of PMS Item attribute is as expected form i.e XXX.XX
     * @param context
     * @param args
     *            --Object Id of Document
     * @return -- '0'if success...'1' for failure with error message
     * @throws Exception
     * @author psalunke : TIGTK-16158
     * @since 26-07-2018
     */

    public int checkPMSItemAttributeValue(Context context, String[] args) throws Exception {
        logger.error("\n pss.document.policy.DocumentUtil : checkPMSItemAttributeValue() : START ");
        int intCheckResult = 1;
        try {
            String strDocumentObjectID = args[0];
            DomainObject domDocumentObject = DomainObject.newInstance(context, strDocumentObjectID);

            String strPMSItemValue = domDocumentObject.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PMSITEM);
            if (UIUtil.isNotNullAndNotEmpty(strPMSItemValue)) {
                if (strPMSItemValue.length() == 6 && strPMSItemValue.indexOf(".") == 3) {
                    String strRegex = "^[A-Z0-9]*$";
                    StringList slPMSItem = FrameworkUtil.split(strPMSItemValue, ".");
                    if (slPMSItem.size() == 2) {
                        String strPMSItem0 = (String) slPMSItem.get(0);
                        String strPMSItem1 = (String) slPMSItem.get(1);
                        if (strPMSItem0.matches(strRegex) && strPMSItem1.matches(strRegex)) {
                            intCheckResult = 0;
                        }
                    }
                }
            } else {
                intCheckResult = 0;
            }
            if (intCheckResult == 1) {
                String strAlertMessage = EnoviaResourceBundle.getProperty(context, "emxComponentsStringResource", context.getLocale(), "emxComponents.Common.AttributeValidationError.PMSItem");
                MqlUtil.mqlCommand(context, "notice $1", strAlertMessage);
            }
            logger.error("\n pss.document.policy.DocumentUtil : checkPMSItemAttributeValue() : END ");
        } catch (Exception ex) {
            logger.error("\n pss.document.policy.DocumentUtil : checkPMSItemAttributeValue() : " + ex);
            throw ex;
        }
        return intCheckResult;
    }

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     *             This method for Block the Demotion of Document Object if PSS_MandatoryDeliverable attribute value Yes
     */
    public int checkMandatoryDeliverableValueOfDocument(Context context, String[] args) throws Exception {
        try {
            String strObjectID = args[0];
            DomainObject domObject = DomainObject.newInstance(context, strObjectID);
            String strAttValue = domObject.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANDATORYDELIVERABLE);
            String ATTRIBUTE_PSS_MANDATORY_DELIVERABLE_VALUE = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", context.getLocale(),
                    "emxFramework.Range.PSS_MandatoryDelivrable.No");
            if (!ATTRIBUTE_PSS_MANDATORY_DELIVERABLE_VALUE.equals(strAttValue)) {
                String strMessage = EnoviaResourceBundle.getProperty(context, "emxTeamCentralStringResource", context.getLocale(), "emxTeamCentral.Common.DocumentMandatoryDeliverableDemoteErrorMsg");
                MqlUtil.mqlCommand(context, "notice $1", strMessage);
                return 1;
            }

        } catch (Exception ex) {
            logger.error("Exception in  DocumentUtil : checkMandatoryDeliverableValueOfDocument() method  ", ex);
            throw ex;
        }
        return 0;
    }

    /**
     * Method returns Objectids to exclude in FTS
     * @param context
     * @param Objectid
     * @param srcDestRelName
     * @param field_actual
     * @param isTo
     * @return Stringlist
     * @throws Exception
     * @since V6R2015x
     */

    @com.matrixone.apps.framework.ui.ExcludeOIDProgramCallable
    public static Object excludeFolderRelatedContentsObjects(Context context, String[] args) throws Exception {
        StringList excludeOID = new StringList();
        Map paramMap = (HashMap) JPO.unpackArgs(args);
        String objectId = (String) paramMap.get("objectId");
        String strFieldtype = (String) paramMap.get("field_actual");
        if (UIUtil.isNullOrEmpty(strFieldtype)) {
            strFieldtype = (String) paramMap.get("default");
        }
        strFieldtype = strFieldtype.replace("TYPES=", "");
        strFieldtype = strFieldtype.trim();
        StringBuffer strtype = new StringBuffer();
        String[] typeArray = strFieldtype.split(",");
        for (int i = 0; i < typeArray.length; i++) {
            if (typeArray[i].contains(":")) {
                strtype.append(typeArray[i].split(":"));
            } else
                strtype.append(typeArray[i]);
            if (i <= typeArray.length - 2) {
                strtype.append(",");
            }
        }
        DomainObject domainObject = new DomainObject(objectId);
        StringList selectList = new StringList(DomainConstants.SELECT_ID);

        MapList mlist = domainObject.getRelatedObjects(context, PropertyUtil.getSchemaProperty(context, "relationship_VaultedDocuments"), strtype.toString(), selectList, null, false, true, (short) 1,
                null, null, 0, null, null, null);
        if (mlist.size() > 0) {
            Iterator itr = mlist.iterator();
            Map map;
            while (itr.hasNext()) {
                map = (Map) itr.next();
                excludeOID.add((String) map.get(DomainConstants.SELECT_ID));
            }
        }
        StringList slBusSelects = new StringList(DomainConstants.SELECT_ID);
        MapList mlObjListDocument = DomainObject.findObjects(context, "Document", DomainConstants.QUERY_WILDCARD, DomainConstants.QUERY_WILDCARD, DomainConstants.QUERY_WILDCARD,
                TigerConstants.VAULT_ESERVICEPRODUCTION, null, false, slBusSelects);
        if (mlObjListDocument.size() > 0) {
            Iterator itrObjects = mlObjListDocument.iterator();
            Map mapDocs;
            while (itrObjects.hasNext()) {
                mapDocs = (Map) itrObjects.next();
                excludeOID.add((String) mapDocs.get(DomainConstants.SELECT_ID));
            }
        }
        return excludeOID;
    }
}