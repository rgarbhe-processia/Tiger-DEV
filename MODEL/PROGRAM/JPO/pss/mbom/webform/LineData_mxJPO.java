package pss.mbom.webform;

import java.util.HashMap;
import java.util.Map;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;
import pss.constants.TigerConstants;

public class LineData_mxJPO {

    // TIGTK-5405 - 11-04-2017 - VB - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(LineData_mxJPO.class);
    // TIGTK-5405 - 11-04-2017 - VB - END

    public LineData_mxJPO() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * this method is used to autofill the MBOM color on the form,while creating LineData object
     * @param context
     * @param args
     * @return List of Harmony Request objects
     * @throws Exception
     *             Exception appears, if error occured
     */
    @SuppressWarnings("rawtypes")
    public String getColorForMBOMLineData(Context context, String[] args) throws Exception {

        String strColorChanges = DomainObject.EMPTY_STRING;
        try {
            HashMap param = (HashMap) JPO.unpackArgs(args);
            HashMap requestMap = (HashMap) param.get("requestMap");
            String objectId = (String) requestMap.get("objectId");
            DomainObject domMbom = DomainObject.newInstance(context, objectId);
            StringList slObjSelectStmts = new StringList();
            slObjSelectStmts.addElement(DomainConstants.SELECT_ID);
            slObjSelectStmts.addElement(DomainConstants.SELECT_NAME);
            // Relationship selects
            StringList slSelectRelStmts = new StringList(1);
            slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

            MapList mlColorOptions = domMbom.getRelatedObjects(context, // context
                    TigerConstants.RELATIONSHIP_PSS_COLORLIST, // relationship
                    // pattern
                    TigerConstants.TYPE_PSS_COLOROPTION, // object pattern
                    slObjSelectStmts, // object selects
                    slSelectRelStmts, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 0, // recursion level
                    null, // object where clause
                    null, // relationship where clause
                    (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    null, null, null, null);
            int nNoOfColorChnages = mlColorOptions.size() - 1;

            strColorChanges = Integer.toString(nNoOfColorChnages);

        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getColorForMBOMLineData: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
        return strColorChanges;
    }

    @SuppressWarnings("rawtypes")
    public void setColorForMBOMLineData(Context context, String[] args) throws Exception {
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap paramMap = (HashMap) programMap.get("paramMap");
            String objectId = (String) paramMap.get("objectId");
            if (UIUtil.isNotNullAndNotEmpty(objectId)) {
                DomainObject domObj = DomainObject.newInstance(context, objectId);
                Map fieldMap = (HashMap) programMap.get("fieldMap");
                StringList strFieldName = (StringList) fieldMap.get("field_value");
                String strField = (String) strFieldName.get(0);
                domObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_LINEDATA_PSS_NUMBEROFCOLORCHANGES, strField);
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in setColorForMBOMLineData: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
    }
}