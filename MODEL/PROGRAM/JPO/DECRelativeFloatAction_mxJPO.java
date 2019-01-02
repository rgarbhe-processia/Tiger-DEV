
/*
 ** DECRelativeFloatAction
 **
 ** Copyright (c) 1993-2015 Dassault Systemes. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes. Copyright notice is precautionary only and does
 * not evidence any actual or intended publication of such program
 */

import java.util.Map;

import matrix.db.Context;
import matrix.db.BusinessObject;
import matrix.util.StringList;

import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.MCADServerException;

public class DECRelativeFloatAction_mxJPO extends emxDomainObject_mxJPO {

    /**
     * Constructor
     * @param context
     * @param args
     * @throws Exception
     */
    public DECRelativeFloatAction_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
    }

    /**
     * RelativeFloatActionmethod
     * @param context
     * @param args
     * @throws Exception
     *             Floats the relationship Part_Specification if -->the present revision of the Object(CATPart/CAT PRoduct) is not connected to any EC Part/Development Part and -->previous revisions
     *             of the object is connected to EC PArt/Development Part which is in Released state
     */

    public void RelativeFloatActionmethod(Context context, String[] args) throws Exception {
        try {
            String sRel = args[0];
            String sDirection = args[1];
            String sOperation = args[2];
            String sState = args[3];
            String sType = args[4];
            String sName = args[5];
            String sRevision = args[6];
            String objid = args[7];
            String All_REVISIONS = "revisions";
            String sConnectedPartId = "";
            String sRelPartSpec = MCADMxUtil.getActualNameForAEFData(context, sRel);

            boolean bFromDir = false;
            boolean btoDir = false;

            BusinessObject busObj = new BusinessObject(objid);

            if (sDirection.equalsIgnoreCase("from")) {
                btoDir = false;
                bFromDir = true;
            } else if (sDirection.equalsIgnoreCase("to")) {
                btoDir = true;
                bFromDir = false;
            }

            BusinessObject bo = busObj.getPreviousRevision(context);

            DomainObject prevRevObject = new DomainObject(bo);

            DomainObject oldRevObject = new DomainObject(busObj);

            StringList slObjectSle = new StringList(1);
            slObjectSle.addElement(DomainConstants.SELECT_ID);

            StringList slRelSle = new StringList(1);
            slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

            MapList mlPartsConnectedPreObj = oldRevObject.getRelatedObjects(context, sRelPartSpec, "*", slObjectSle, slRelSle, true, false, (short) 1, null, null);

            if (mlPartsConnectedPreObj.isEmpty()) {
                if (prevRevObject.exists(context)) {
                    MapList mlPartsConnected = prevRevObject.getRelatedObjects(context, sRelPartSpec, "*", // object pattern
                            slObjectSle, // object selects
                            slRelSle, // relationship selects
                            btoDir, // to direction
                            bFromDir, // from direction
                            (short) 1, // recursion level
                            null, // object where clause
                            null); // relationship where clause

                    for (int i = 0; i < mlPartsConnected.size(); i++) {
                        Map mItem = (Map) mlPartsConnected.get(i);
                        sConnectedPartId = (String) mItem.get(DomainRelationship.SELECT_RELATIONSHIP_ID);

                        if (sConnectedPartId != null) {
                            String Args[] = new String[5];
                            Args[0] = sConnectedPartId;
                            Args[1] = sDirection;
                            Args[2] = sType;
                            Args[3] = sName;
                            Args[4] = sRevision;

                            boolean isPushed = false;

                            try {
                                com.matrixone.apps.domain.util.ContextUtil.pushContext(context);
                                isPushed = true;
                                MqlUtil.mqlCommand(context, "modify connection $1 $2 '$3' '$4' $5", Args);
                            } catch (Exception ex) {
                                MCADServerException.createException(ex.getMessage(), ex);
                            } finally {
                                if (isPushed) {
                                    try {
                                        com.matrixone.apps.domain.util.ContextUtil.popContext(context);
                                    } catch (Exception ex) {
                                        MCADServerException.createException(ex.getMessage(), ex);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

}
