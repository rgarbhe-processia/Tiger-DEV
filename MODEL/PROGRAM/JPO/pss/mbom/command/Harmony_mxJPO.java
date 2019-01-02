package pss.mbom.command;

import java.util.HashMap;
import java.util.Map;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.RelationshipType;
import matrix.util.StringList;
import pss.constants.TigerConstants;

public class Harmony_mxJPO {

    // TIGTK-5405 - 06-04-2017 - VB - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Harmony_mxJPO.class);

    // TIGTK-5405 - 06-04-2017 - VB - END

    public Harmony_mxJPO() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * this method is used to connect Harmony request with program object ,which is related to the parent MBOM object
     * @param context
     * @param args
     * @return
     * @throws Exception
     *             Exception appears, if error occured
     */

    public Map connectHarmonyRequestWithProgram(Context context, String[] args) throws Exception {
        Map returnMap = new HashMap();
        final String PROGRAM = "Program";

        int flag = 0;
        try {
            Map map = JPO.unpackArgs(args);
            Map paramMap = (Map) map.get("paramMap");
            String strHarmonyRequestId = (String) paramMap.get("objectId");
            Map requestMap = (Map) map.get("requestMap");
            String strMBOMId = (String) requestMap.get("objectId");

            DomainObject domHarmonyRequest = DomainObject.newInstance(context, strHarmonyRequestId);

            MapList mList = pss.mbom.MBOMUtil_mxJPO.getProgramFromMBOM(context, strMBOMId);
            // TIGTK-8729:Rutuja Ekatpure:23/6/2017:Start
            if (!mList.isEmpty()) {
                for (int i = 0; i < mList.size(); i++) {
                    Map programObjectmap = (Map) mList.get(i);
                    String strObjectId = (String) programObjectmap.get(DomainConstants.SELECT_ID);
                    DomainObject domProgramobj = DomainObject.newInstance(context, strObjectId);
                    String strCurrentState = domProgramobj.getInfo(context, DomainConstants.SELECT_CURRENT);

                    if (!strCurrentState.equalsIgnoreCase(TigerConstants.STATE_ACTIVE)) {
                        DomainRelationship.connect(context, domProgramobj, new RelationshipType(TigerConstants.RELATIONSHIP_PSSCONNECT_HARMONY_REQUEST), domHarmonyRequest);
                        flag = 1;
                        break;
                    }
                }
            }
            // TIGTK-8196:PKH:Start
            if (flag == 0 || mList.isEmpty()) {
                returnMap.put("Action", "ERROR");
                String strAlertMessage = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", "PSS_FRCMBOMCentral.ErrorMessage.ProgramProjectError",
                        context.getSession().getLanguage());
                returnMap.put("Message", strAlertMessage);
            }
            // TIGTK-8196:PKH:End
            // TIGTK-8729:Rutuja Ekatpure:23/6/2017:End
        } catch (Exception e) {
            // TIGTK-5405 - 06-04-2017 - VB - START
            logger.error("Error in connectHarmonyRequestWithProgram: ", e);
            throw e;
            // TIGTK-5405 - 06-04-2017 - VB - END
        }
        return returnMap;
    }

    /**
     * this method is used to connect Harmony with parent MBOM object
     * @param context
     * @param args
     * @return
     * @throws Exception
     *             Exception appears, if error occured
     */
    public Map connectHarmonyWithMBOM(Context context, String args[]) throws Exception {
        Map returnMap = new HashMap();

        String strCurrent = DomainConstants.EMPTY_STRING;
        int flag = 0;
        try {
            DomainObject domProgram = null;
            Map map = JPO.unpackArgs(args);
            Map paramMap = (Map) map.get("paramMap");
            String strHarmonyId = (String) paramMap.get("objectId");
            DomainObject domHarmony = DomainObject.newInstance(context, strHarmonyId);
            Map requestMap = (Map) map.get("requestMap");
            String strMBOMId = (String) requestMap.get("objectId");
            DomainObject domMBOM = DomainObject.newInstance(context, strMBOMId);

            String strHarmonyRequestId = (String) requestMap.get("Harmony Request");
            if (!UIUtil.isNullOrEmpty(strHarmonyRequestId)) {
                DomainObject domHarmonyRequest = DomainObject.newInstance(context, strHarmonyRequestId);
                MapList programList = domHarmonyRequest.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSSCONNECT_HARMONY_REQUEST, // relationship pattern
                        TigerConstants.TYPE_PSS_PROGRAMPROJECT, // object pattern
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

                if (!(programList.isEmpty())) {
                    Map programMap = (Map) programList.get(0);
                    String strProgramId = (String) programMap.get(DomainConstants.SELECT_ID);
                    domProgram = DomainObject.newInstance(context, strProgramId);
                    strCurrent = domProgram.getInfo(context, DomainConstants.SELECT_CURRENT);
                    if (!strCurrent.equalsIgnoreCase(TigerConstants.STATE_ACTIVE)) {
                        DomainRelationship.connect(context, domProgram, new RelationshipType(TigerConstants.RELATIONSHIP_PSSCONNECTED_HARMONY), domHarmony);
                    }
                }

                DomainRelationship.connect(context, domHarmony, new RelationshipType(TigerConstants.RELATIONSHIP_REQUESTED_HARMONY), domHarmonyRequest);
            } else {
                MapList programList = pss.mbom.MBOMUtil_mxJPO.getProgramFromMBOM(context, strMBOMId);
                if (!(programList.isEmpty())) {
                    Map mProgramMap = (Map) programList.get(0);
                    String strProgramId = (String) mProgramMap.get(DomainConstants.SELECT_ID);
                    domProgram = DomainObject.newInstance(context, strProgramId);
                    strCurrent = domProgram.getInfo(context, DomainConstants.SELECT_CURRENT);
                    if (!strCurrent.equalsIgnoreCase(TigerConstants.STATE_ACTIVE)) {
                        DomainRelationship.connect(context, domProgram, new RelationshipType(TigerConstants.RELATIONSHIP_PSSCONNECTED_HARMONY), domHarmony);
                    }
                } // TIGTK-8729:Rutuja Ekatpure:23/6/2017:Start
                else {
                    // if no Program project connected
                    flag = 1;
                } // TIGTK-8729:Rutuja Ekatpure:23/6/2017:End
            }
            // TIGTK-8729:Rutuja Ekatpure:23/6/2017:Start
            // TIGTK-8196:PKH:Start
            if (strCurrent.equalsIgnoreCase(TigerConstants.STATE_ACTIVE) || flag == 1) {

                returnMap.put("Action", "ERROR");
                String strAlertMessage = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", "PSS_FRCMBOMCentral.ErrorMessage.ProgramProjectError",
                        context.getSession().getLanguage());
                returnMap.put("Message", strAlertMessage);
            }
            // TIGTK-8196:PKH:End
            // TIGTK-8729:Rutuja Ekatpure:23/6/2017:End
            DomainRelationship.connect(context, domMBOM, new RelationshipType(TigerConstants.RELATIONSHIP_PSSMBOM_HARMONIES), domHarmony);

        } catch (Exception e) {
            // TIGTK-5405 - 06-04-2017 - VB - START
            logger.error("Error in connectHarmonyWithMBOM: ", e);
            // TIGTK-5405 - 06-04-2017 - VB - END
        }
        return returnMap;
    }

    // TIGTK-6812 | 20/06/2017 | Harika Varanasi : Starts
    /**
     * getHarmoniesConnectedToMBOM method
     * @param context
     * @param args
     * @return MapList
     * @throws Exception
     * @author Harika Varanasi | 20/06/2017 | TIGTK-6812
     */
    @SuppressWarnings({ "rawtypes" })
    public MapList getHarmoniesConnectedToMBOM(Context context, String args[]) throws Exception {
        MapList mlHarmonies = null;
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strMBOMId = (String) programMap.get("objectId");

            if (UIUtil.isNotNullAndNotEmpty(strMBOMId)) {
                DomainObject domMBOM = DomainObject.newInstance(context, strMBOMId);
                StringList objectSelects = new StringList();
                objectSelects.addElement(DomainConstants.SELECT_NAME);
                objectSelects.addElement(DomainConstants.SELECT_ID);

                StringList relSelects = new StringList();
                relSelects.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

                mlHarmonies = domMBOM.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSSMBOM_HARMONIES, // PSS_MBOMHarmonies
                        TigerConstants.TYPE_PSS_HARMONY, // PSS_Harmony
                        objectSelects, // ID,Name
                        relSelects, // relationship id
                        false, // getTo false
                        true, // getFrom true
                        (short) 1, // 1
                        DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, 0); // limit 0
            }

        } catch (Exception ex) {
            logger.error("Error in getHarmoniesConnectedToMBOM: ", ex);
        }
        return mlHarmonies;
    }

    /**
     * getHarmoniesListConnectedToMBOM method
     * @param context
     * @param args
     * @return StringList
     * @throws Exception
     * @author Harika Varanasi | 20/06/2017 | TIGTK-6812
     */
    @SuppressWarnings("rawtypes")
    public StringList getHarmoniesListConnectedToMBOM(Context context, String args[]) throws Exception {
        StringList slHarmonies = new StringList();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strMBOMId = (String) programMap.get("objectId");

            if (UIUtil.isNotNullAndNotEmpty(strMBOMId)) {
                DomainObject domMBOM = DomainObject.newInstance(context, strMBOMId);
                slHarmonies = domMBOM.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSSMBOM_HARMONIES + "].to.id");
            }

        } catch (Exception ex) {
            logger.error("Error in getHarmoniesListConnectedToMBOM: ", ex);
        }
        return slHarmonies;
    }

    // TIGTK-6812 | 20/06/2017 | Harika Varanasi : Ends

}
