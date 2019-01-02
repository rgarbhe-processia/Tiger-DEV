package pss.cadbom;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PropertyUtil;

import matrix.db.Access;
import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.Pattern;
import matrix.util.StringList;
import pss.constants.TigerConstants;

public class externalReference_mxJPO {

    // TIGTK-5405 - 11-04-2017 - VB - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(externalReference_mxJPO.class);
    // TIGTK-5405 - 11-04-2017 - VB - END

    /**
     * This method is executed if Customer Company
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @return int value 0 for success
     * @throws Exception
     *             if the operation fails
     */
    public String mxMain(Context context, String[] args) throws Exception {
        String rangeValues = "";
        try {
            // TIGTK-9592 : PKH :START
            StringList slBus = new StringList(1);
            slBus.addElement(DomainObject.SELECT_NAME);
            slBus.addElement(DomainObject.SELECT_TYPE);

            Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_OEM);
            typePattern.addPattern(TigerConstants.TYPE_PSS_OEMGROUP);

            MapList mlPrograms = DomainObject.findObjects(context, typePattern.getPattern(), TigerConstants.VAULT_ESERVICEPRODUCTION, DomainConstants.EMPTY_STRING, slBus);

            mlPrograms.addSortKey(DomainObject.SELECT_TYPE, "ascending", "String");
            mlPrograms.addSortKey(DomainObject.SELECT_NAME, "ascending", "String");
            mlPrograms.sortStructure();

            Iterator<Map<?, ?>> itrmlOEMObject = (Iterator<Map<?, ?>>) mlPrograms.iterator();
            String sProgramName = DomainConstants.EMPTY_STRING;
            String strTypeName = DomainConstants.EMPTY_STRING;
            StringList slPrograms = new StringList();
            // Modified for PPDM-ERGO(TIGTK-4504):PK:01/03/2017:Start
            slPrograms.addElement(TigerConstants.ATTR_VALUE_UNASSIGNED);
            // Modified for PPDM-ERGO(TIGTK-4504):PK:01/03/2017:End
            while (itrmlOEMObject.hasNext()) {
                Map<?, ?> mapProgram = (Map<?, ?>) itrmlOEMObject.next();
                sProgramName = (String) mapProgram.get(DomainObject.SELECT_NAME);
                strTypeName = (String) mapProgram.get(DomainObject.SELECT_TYPE);
                StringBuffer sbRange = new StringBuffer();
                if (TigerConstants.TYPE_PSS_OEM.equals(strTypeName)) {
                    sbRange.append("\"");
                    sbRange.append(sProgramName);
                    sbRange.append(" - OEM");
                    sbRange.append("\"");
                } else {
                    sbRange.append("\"");
                    sbRange.append(sProgramName);
                    sbRange.append(" - OEM Group");
                    sbRange.append("\"");
                }
                // TIGTK-9592 : PKH :END

                slPrograms.add(sbRange.toString());
            }
            rangeValues = slPrograms.join(" ");
        } catch (Exception ex) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in mxMain: ", ex);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw ex;
        }

        return rangeValues;
    }

    /**
     * Method to get the connected External Reference to Part Object.
     * @param context
     * @param args
     * @return MapList - Contains the "Object Id" and "Relationship Id" of Parts which are connected to PSS_ExternalReference Object.
     * @throws Exception
     */
    public MapList getExternalReferenceOnPart(Context context, String[] args) throws Exception {
        MapList mlConnectedExternalReferenceList = null;
        try {

            HashMap<String, String> programMap = (HashMap) JPO.unpackArgs(args);
            String strPartObjectId = (String) programMap.get("objectId");

            DomainObject domPartObject = DomainObject.newInstance(context, strPartObjectId);

            StringList lstSelectStmts = new StringList();
            StringList lstRelStmts = new StringList();

            lstSelectStmts.add(DomainConstants.SELECT_ID);
            lstRelStmts.add(DomainConstants.SELECT_RELATIONSHIP_ID);

            // MapList containing the "PSS_ExternalReference" connected with "Parts" using "PSS_ExternalReference" relationship

            mlConnectedExternalReferenceList = domPartObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_EXTERNALREFERENCE, TigerConstants.TYPE_PSS_EXTERNALREFERENCE, lstSelectStmts,
                    lstRelStmts, false, true, (short) 0, null, null, 0);

        } catch (Exception ex) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getExternalReferenceOnPart: ", ex);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw ex;
        }

        return mlConnectedExternalReferenceList;
    }

    /**
     * Method to promote Previous Revision To Cancelled State.
     * @param context
     * @param args
     * @throws Exception
     */

    public void promotePreviousRevToCancelledState(Context context, String[] args) throws Exception {
        try {
            String objectId = args[0];
            String CANCELLED = "Cancelled";
            DomainObject domExternalReference = new DomainObject(objectId);
            BusinessObject boPreviousRevision = domExternalReference.getPreviousRevision(context);
            if (boPreviousRevision.exists(context)) {
                // Getting previous revision ID
                String strPreviousRevID = boPreviousRevision.getObjectId(context);
                // Modified on 30/01/2017 for Issue TIGTK-4051 :Start by SIE
                DomainObject domExtRef = new DomainObject(strPreviousRevID);
                String strCurrentState = (String) domExtRef.getInfo(context, DomainConstants.SELECT_CURRENT);
                if (!strCurrentState.equals(CANCELLED)) {
                    ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                    domExtRef.promote(context);
                    ContextUtil.popContext(context);
                }
                // Modified on 30/01/2017 for Issue TIGTK-4051 :End

            }
        } catch (Exception ex) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in promotePreviousRevToCancelledState: ", ex);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw ex;
        }
    }

    /**
     * Method to disconnect Previous Revision of External Reference and connect latest revision of External Reference to Part
     * @param context
     * @param args
     * @return void
     * @throws Exception
     * @since 13-09-2017
     * @author psalunke - TIGTK-9595
     */

    public void floatExternalReferenceOnRelease(Context context, String[] args) throws Exception {
        logger.debug("pss.cadbom.externalReference : floatExternalReferenceOnRelease : START");
        boolean bIsConextPushed = false;
        try {
            String strExtRefOjectId = args[0];
            DomainObject domExternalReference = new DomainObject(strExtRefOjectId);
            BusinessObject boPreviousRevision = domExternalReference.getPreviousRevision(context);
            if (boPreviousRevision.exists(context)) {
                // Getting previous revision ID
                DomainObject domExtRefPreviousRev = new DomainObject(boPreviousRevision);
                StringList slObjectSelects = new StringList(1);
                slObjectSelects.addElement(DomainConstants.SELECT_ID);
                slObjectSelects.addElement(DomainConstants.SELECT_POLICY);
                slObjectSelects.addElement(DomainConstants.SELECT_CURRENT);
                // Object Where clause
                StringBuffer sbWhereClause = new StringBuffer();
                sbWhereClause.append("current!= ");
                sbWhereClause.append(TigerConstants.STATE_PART_OBSOLETE);
                sbWhereClause.append(" && current!= ");
                sbWhereClause.append(TigerConstants.STATE_PSS_DEVELOPMENTPART_OBSOLETE);
                sbWhereClause.append(" && current!= ");
                sbWhereClause.append(TigerConstants.STATE_STANDARDPART_OBSOLETE);
                sbWhereClause.append(" && current!= ");
                sbWhereClause.append(TigerConstants.STATE_PSS_CANCELPART_CANCELLED);

                // Get connected part list of previous revision
                MapList mlConnectedPartList = domExtRefPreviousRev.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_EXTERNALREFERENCE, DomainConstants.TYPE_PART, slObjectSelects,
                        new StringList(DomainRelationship.SELECT_ID), true, false, (short) 1, sbWhereClause.toString(), null, 0);

                if (mlConnectedPartList != null && !mlConnectedPartList.isEmpty()) {

                    Access mAccess = domExtRefPreviousRev.getAccessMask(context);
                    // TIGTK-16824 : 24-08-2018 : START
                    if (!mAccess.hasToDisconnectAccess() || !mAccess.hasModifyAccess()) {
                        // TIGTK-16824 : 24-08-2018 : END
                        ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, TigerConstants.PERSON_USER_AGENT), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                        bIsConextPushed = true;
                    }

                    for (int i = 0; i < mlConnectedPartList.size(); i++) {
                        Map mPartMap = (Map) mlConnectedPartList.get(i);
                        String strConnectionId = (String) mPartMap.get(DomainRelationship.SELECT_ID);
                        DomainRelationship.setToObject(context, strConnectionId, domExternalReference);
                    }
                }
                logger.debug("pss.cadbom.externalReference : floatExternalReferenceOnRelease : END");
            }
        } catch (Exception ex) {
            logger.error("Error in pss.cadbom.externalReference : floatExternalReferenceOnRelease : ", ex);
            throw ex;
        } finally {
            if (bIsConextPushed) {
                ContextUtil.popContext(context);
            }
        }
    }

}
