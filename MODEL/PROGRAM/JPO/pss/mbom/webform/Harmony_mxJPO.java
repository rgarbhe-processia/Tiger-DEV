package pss.mbom.webform;

import java.util.HashMap;
import java.util.Map;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.MapList;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;
import pss.constants.TigerConstants;

public class Harmony_mxJPO {

    // TIGTK-5405 - 11-04-2017 - VB - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Harmony_mxJPO.class);
    // TIGTK-5405 - 11-04-2017 - VB - END

    public Harmony_mxJPO() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * this method is used to autofill the MBOM title on the form,while creating Harmony request object
     * @param context
     * @param args
     * @return List of Harmony Request objects
     * @throws Exception
     *             Exception appears, if error occured
     */

    @SuppressWarnings("rawtypes")
    public String getAffectedMBOM(Context context, String[] args) throws Exception {
        String strMBOMName = DomainObject.EMPTY_STRING;
        try {
            HashMap param = (HashMap) JPO.unpackArgs(args);
            HashMap requestMap = (HashMap) param.get("requestMap");
            String objectId = (String) requestMap.get("objectId");
            DomainObject domMbom = DomainObject.newInstance(context, objectId);
            strMBOMName = domMbom.getAttributeValue(context, TigerConstants.ATTRIBUTE_V_NAME);
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getAffectedMBOM: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
        return strMBOMName;
    }

    /**
     * this method is used to filter the Harmony Request Object for the search
     * @param context
     * @param args
     * @return List of Harmony Request objects
     * @throws Exception
     *             Exception appears, if error occured
     */
    @SuppressWarnings("rawtypes")
    public StringList getHarmonyRequest(Context context, String[] args) throws Exception {
        StringList includeList = new StringList();
        try {
            Map programMap = JPO.unpackArgs(args);
            String strMBOMObjectId = (String) programMap.get("objectId");
            DomainObject domMbom = DomainObject.newInstance(context, strMBOMObjectId);
            MapList mlHarmonyRequest = domMbom.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_HARMONY_REQUEST, // relationship pattern
                    TigerConstants.TYPE_PSS_HARMONY_REQUEST, // object pattern
                    new StringList(DomainConstants.SELECT_ID), // object selects
                    null, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 1, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    null, // Postpattern
                    null, null, null);
            if (!mlHarmonyRequest.isEmpty()) {
                for (int i = 0; i < mlHarmonyRequest.size(); i++) {
                    Map Harmonymap = (Map) mlHarmonyRequest.get(i);
                    String strHarmonyRequestId = (String) Harmonymap.get(DomainConstants.SELECT_ID);
                    DomainObject domHarmonyRequest = DomainObject.newInstance(context, strHarmonyRequestId);
                    MapList mHarmonyList = domHarmonyRequest.getRelatedObjects(context, TigerConstants.RELATIONSHIP_REQUESTED_HARMONY, // relationship pattern
                            TigerConstants.TYPE_PSS_HARMONY, // object pattern
                            new StringList(DomainConstants.SELECT_ID), // object selects
                            null, // relationship selects
                            true, // to direction
                            false, // from direction
                            (short) 1, // recursion level
                            null, // object where clause
                            null, (short) 0, false, // checkHidden
                            true, // preventDuplicates
                            (short) 1000, // pageSize
                            null, // Postpattern
                            null, null, null);
                    if (mHarmonyList.isEmpty()) {
                        includeList.add(strHarmonyRequestId);
                    }
                }
            }
            // TIGTK-8141 - 06-06-2017 - AniketM - START
            if (includeList.isEmpty()) {
                includeList.add(" ");
            }
            // TIGTK-8141 - 06-06-2017 - AniketM - END
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getHarmonyRequest: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
        return includeList;
    }
}
