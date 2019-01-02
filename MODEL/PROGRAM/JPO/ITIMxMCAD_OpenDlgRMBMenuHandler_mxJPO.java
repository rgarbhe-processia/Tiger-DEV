
/*
 ** ITIMxMCAD_OpenDlgRMBMenuHandler
 **
 ** Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 **
 ** Program to check versions, baseline & srep exist on Major Object
 */

import java.util.*;
import matrix.db.Context;
import matrix.util.MatrixException;
import matrix.util.StringList;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainConstants;
import java.util.Iterator;

public class ITIMxMCAD_OpenDlgRMBMenuHandler_mxJPO {
    /**
     * The no-argument constructor.
     */
    public ITIMxMCAD_OpenDlgRMBMenuHandler_mxJPO() {
    }

    /**
     * Constructor which accepts the Matrix context and an array of String arguments.
     */
    public ITIMxMCAD_OpenDlgRMBMenuHandler_mxJPO(Context context, String[] args) {
    }

    public int mxMain(Context context, String[] args) {
        return 0;
    }

    public String checkIfVersionsBaselineAndSrepExist(Context context, String[] args) {
        /**
         * This method will return boolean value if version exist, boolean value if baseline exist, boolean value if srep exist on the major object [But this will be used only in MxPro]
         * @param context
         *            - Target system URL
         * @param args
         *            - Object Id of major object
         * @return String - versionsEnabled + baselineEnabled + sRepEnabled if successful, "failed" in case of failures
         */
        boolean versionsEnabled = false;
        String sResult = "failed";
        try {
            MapList mListVerObj = countVersions(context, args[0]);
            int versionsCount = mListVerObj.size();
            // Do we need to enable versions command if the versionsCount is zero?
            if (versionsCount > 1) {
                versionsEnabled = true;
            }

            boolean baselineEnabled = checkIfBaselineExist(context, mListVerObj);
            boolean sRepEnabled = checkIfSrepsExist(context, args[0]);
            sResult = versionsEnabled + "," + baselineEnabled + "," + sRepEnabled;
        } catch (Exception e) {
            sResult = "failed";
        }

        return sResult;

    }

    private MapList countVersions(Context context, String majorObjID) {
        // This function finds the Minor objs connected to a major object & returns
        // them in Maplist
        StringList sListObjSelect = new StringList();
        sListObjSelect.add(DomainConstants.SELECT_ID);
        MapList mListMinorObjs = new MapList();
        try {

            String relname = PropertyUtil.getSchemaProperty(context, "relationship_VersionOf");
            DomainObject dob = new DomainObject(majorObjID);

            mListMinorObjs = (MapList) dob.getRelatedObjects(context, relname, DomainConstants.QUERY_WILDCARD, sListObjSelect, null, true, false, (short) 1, null, null);

        } catch (Exception e) {
        }
        return mListMinorObjs;

    }

    private boolean checkIfSrepsExist(Context context, String majorObjID) {
        // This function checks if simplified representation exist on an object

        int srepCount = 0;
        boolean srepEnabled = false;

        try {
            DomainObject dob = new DomainObject(majorObjID);

            String attrname = PropertyUtil.getSchemaProperty(context, "attribute_ProESimplifiedReps");
            String srepValue = (String) dob.getAttributeValue(context, attrname);

            if (srepValue != null && srepValue.length() > 0) {
                String[] sreps = srepValue.split(",");
                srepCount = sreps.length;

                if (srepCount == 1) {
                    String[] srep = srepValue.split(":");

                    if (srep[0].equals("DEFAULT REP") != true) {
                        // If obj contains only one rep & it is not the "DEFAULT REP"
                        // it needs to be displayed
                        srepEnabled = true;
                    }
                } else if (srepCount > 1) {
                    srepEnabled = true;
                } else {
                    srepEnabled = false;
                }

            }
        } catch (Exception e) {
            srepEnabled = true;
        }
        return srepEnabled;
    }

    private boolean checkIfBaselineExist(Context context, MapList mListVerObj) {
        // This function checks if baseline exists on an object

        boolean baselineExist = false;
        StringList sListObjSelect = new StringList();
        sListObjSelect.add(DomainConstants.SELECT_ID);

        String relname = PropertyUtil.getSchemaProperty(context, "relationship_DesignBaseline");

        try {

            Iterator elements = mListVerObj.iterator();
            // Iterate through each versioned object and find out if any of them has
            // baselines connected
            while (elements.hasNext()) {

                Map minorObj = (Map) elements.next();
                String minorObjId = (String) minorObj.get(DomainConstants.SELECT_ID);
                DomainObject dob = new DomainObject(minorObjId);

                Map baseline = (Map) dob.getRelatedObject(context, relname, true, sListObjSelect, null);

                if (baseline != null) {
                    baselineExist = true;
                    // If a baseline is found then break, no need to iterate further
                    break;
                }
            }
        } catch (Exception e) {
            baselineExist = true;
        }
        return baselineExist;

    }

}
