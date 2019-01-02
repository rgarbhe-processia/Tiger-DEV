
/*
 ** PSS_emxClassification.java
 **
 ** Copyright (c) 1992-2015 Dassault Systemes. All Rights Reserved. This program contains proprietary and trade secret information of MatrixOne, Inc. Copyright notice is precautionary only and does not
 * evidence any actual or intended publication of such program
 **
 ** FileName : "$RCSfile: ${CLASSNAME}.java.rca $" Author : Version : Date :
 **
 ** staic const RCSID [] = "$Id: ${CLASSNAME}.java.rca 1.7 Wed Oct 22 16:02:19 2008 przemek Experimental przemek $";
 */

import java.util.HashMap;
import java.util.Map;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.PropertyUtil;
import pss.constants.TigerConstants;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

public class PSS_emxClassification_mxJPO extends emxClassificationBase_mxJPO {
    // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PSS_emxClassification_mxJPO.class);
    // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - END

    /**
     * Constructor.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            the Java <code>String[]</code> object
     * @throws Exception
     *             if the operation fails
     * @since AEF 9.5.0.0
     */
    public PSS_emxClassification_mxJPO(Context context, String[] args) throws Exception {
        // Call the super constructor
        super(context, args);
    }

    /**
     * This method is executed if a specific method is not specified.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            the Java <code>String[]</code> object
     * @return int
     * @throws Exception
     *             if the operation fails
     */
    public int mxMain(Context context, String[] args) throws Exception {
        if (true) {
            throw new Exception("Must specify method on emxClassification invocation");
        }

        return 0;
    }

    /**
     * This method returns search query for Library field during classification/reClassification
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds the following input arguments 0 - object Id 1 - rowIds
     * @return String - the search query
     * @throws Exception
     *             if the operation fails
     */
    public String getLibrayDynamicQuery(Context context, String args[]) throws Exception {

        String dyanmicQuery = "";

        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap requestMap = (HashMap) programMap.get("requestMap");
            String objectId = (String) requestMap.get("objectId");
            DomainObject doObj = new DomainObject(objectId);

            StringList slAIInfo = new StringList();
            slAIInfo.addElement(DomainConstants.SELECT_TYPE);
            slAIInfo.addElement(DomainConstants.SELECT_POLICY);

            Map map = doObj.getInfo(context, slAIInfo);
            String strType = (String) map.get(DomainConstants.SELECT_TYPE);
            String strPolicy = (String) map.get(DomainConstants.SELECT_POLICY);
            if (strPolicy.equals(TigerConstants.POLICY_PSS_TOOL)) {
                dyanmicQuery = "TYPES=type_GeneralLibrary";
            } else if (strPolicy.equals(TigerConstants.POLICY_PSS_MATERIAL)) {
                dyanmicQuery = "TYPES=type_GeneralLibrary";
            } else if (strType.equals(TYPE_PART)) {
                dyanmicQuery = "TYPES=type_PartLibrary";
            }
        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getLibrayDynamicQuery: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
        return dyanmicQuery;
    }

    /**
     * This method returns search query for Class field during classification/reClassification
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds the following input arguments 0 - object Id 1 - rowIds
     * @return String - the search query
     * @throws Exception
     *             if the operation fails
     */
    public String getClassDynamicQuery(Context context, String args[]) throws Exception {
        String dyanmicQuery = "";
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            Map fieldValuesMap = (HashMap) programMap.get("fieldValues");

            HashMap requestMap = (HashMap) programMap.get("requestMap");
            String objectId = (String) requestMap.get("objectId");

            DomainObject doObj = new DomainObject(objectId);
            // TIGTK-7235 | Harika Varanasi | 06/07/2017 :Starts

            StringList slAIInfo = new StringList();
            slAIInfo.addElement(DomainConstants.SELECT_TYPE);
            slAIInfo.addElement(DomainConstants.SELECT_POLICY);

            Map map = doObj.getInfo(context, slAIInfo);
            String strType = (String) map.get(DomainConstants.SELECT_TYPE);
            String strPolicy = (String) map.get(DomainConstants.SELECT_POLICY);

            if (strPolicy.equals(TigerConstants.POLICY_PSS_TOOL)) {
                dyanmicQuery = "TYPES=type_GeneralClass:CURRENT=policy_Classification.state_Active:PSS_GeneralClassType=Tooling";

            } else if (strPolicy.equals(TigerConstants.POLICY_PSS_MATERIAL)) {
                dyanmicQuery = "TYPES=type_GeneralClass:CURRENT=policy_Classification.state_Active:PSS_GeneralClassType=Material";
            } else if (strType.equals(TYPE_PART)) {
                dyanmicQuery = "TYPES=type_PartFamily:CURRENT=policy_Classification.state_Active";
            }
            // TIGTK-7235 | Harika Varanasi | 06/07/2017 : Ends
            String librayId = fieldValuesMap.containsKey("LibraryOID") ? (String) fieldValuesMap.get("LibraryOID") : "";

            dyanmicQuery += !"".equals(librayId) ? ":REL_SUBCLASS_FROM_ID=" + librayId : "";
        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getClassDynamicQuery: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
        return dyanmicQuery;
    }

}
