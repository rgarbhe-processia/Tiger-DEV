
/*
 * emxCommonDocumentUI.java
 *
 * Copyright (c) 1992-2015 Dassault Systemes.
 *
 * All Rights Reserved. This program contains proprietary and trade secret information of MatrixOne, Inc. Copyright notice is precautionary only and does not evidence any actual or intended
 * publication of such program.
 *
 */
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import com.matrixone.apps.common.CommonDocument;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.Pattern;
import matrix.util.StringList;
import pss.constants.TigerConstants;

/**
 * @version AEF Rossini - Copyright (c) 2002, MatrixOne, Inc.
 */
public class emxCommonDocumentUI_mxJPO extends emxCommonDocumentUIBase_mxJPO {

    /**
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @throws Exception
     *             if the operation fails
     * @since AEF Rossini
     * @grade 0
     */
    public emxCommonDocumentUI_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
    }

    /**
     * overide OOTB method for TIGTK-17801 :: ALM-6222 gets the list of connected DOCUMENTS to the master Object Used for APPDocumentSummary table
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds the following input arguments: 0 - objectId - parent object OID
     * @returns Object
     * @throws Exception
     *             if the operation fails
     * @since Common 10.5
     * @grade 0
     */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public Object getDocuments(Context context, String[] args) throws Exception {
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String parentId = (String) programMap.get("objectId");
            String parentRel = (String) programMap.get("parentRelName");
            Pattern relPattern = new Pattern("");

            // If parent relation ship is passed separated by comma
            // Tokenize and add it rel pattern

            if (parentRel != null) {
                StringTokenizer relString = new StringTokenizer(parentRel, ",");
                while (relString.hasMoreTokens()) {
                    String relStr = relString.nextToken().trim();
                    if (relStr != null && !"null".equals(relStr) && !"".equals(relStr)) {
                        String actRelName = PropertyUtil.getSchemaProperty(context, relStr);
                        if (actRelName != null && !"null".equals(actRelName) && !"".equals(actRelName)) {
                            relPattern.addPattern(actRelName);
                        }
                    }
                }
            }

            // if not passed, or non-existing relationship passed then default to "Reference Document" relationship
            if ("".equals(relPattern.getPattern())) {
                relPattern.addPattern(PropertyUtil.getSchemaProperty(context, CommonDocument.SYMBOLIC_relationship_ReferenceDocument));
            }

            String objectWhere = "";// CommonDocument.SELECT_IS_VERSION_OBJECT + "==\"False\"";

            DomainObject masterObject = DomainObject.newInstance(context, parentId);

            StringList typeSelects = new StringList(2);
            typeSelects.add(CommonDocument.SELECT_ID);
            typeSelects.add(CommonDocument.SELECT_CURRENT);
            StringList relSelects = new StringList(1);
            relSelects.add(CommonDocument.SELECT_RELATIONSHIP_ID);
            MapList documentList = masterObject.getRelatedObjects(context, relPattern.getPattern(), "*", typeSelects, relSelects, false, true, (short) 1, objectWhere, null, null, null, null);
            // START :: TIGTK-17801 :: ALM-6222
            String strMasterObjState = masterObject.getInfo(context, CommonDocument.SELECT_CURRENT);
            if (masterObject.isKindOf(context, TigerConstants.TYPE_PSS_CHANGEREQUEST)
                    && !(TigerConstants.STATE_PSS_CR_CREATE.equals(strMasterObjState) || TigerConstants.STATE_SUBMIT_CR.equals(strMasterObjState))) {
                MapList mlReturnList = new MapList(documentList.size());
                Iterator itr = documentList.iterator();
                String STATE_RELEASE = PropertyUtil.getSchemaProperty(context, "policy", TigerConstants.POLICY_PSS_DOCUMENT, "state_Released");
                while (itr.hasNext()) {
                    Map mpDocument = (Map) itr.next();
                    String strCurrent = (String) mpDocument.get(CommonDocument.SELECT_CURRENT);
                    if (UIUtil.isNotNullAndNotEmpty(strCurrent) && STATE_RELEASE.equals(strCurrent)) {
                        mpDocument.put(TigerConstants.TABLE_SETTING_DISABLESELECTION, "true");
                    }
                    mlReturnList.add(mpDocument);
                }
                documentList = mlReturnList;
            }
            // END :: TIGTK-17801 :: ALM-6222
            return documentList;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }
}
