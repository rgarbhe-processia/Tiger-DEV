
/*
 ** DSCRecentlyAccessedParts
 **
 ** Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 **
 */

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Vector;

import matrix.db.Context;
import matrix.db.JPO;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.beans.MCADRecentlyAccessedPartsHelper;
import com.matrixone.MCADIntegration.utils.MCADUrlUtil;
import com.matrixone.MCADIntegration.utils.MCADUtil;
import com.matrixone.apps.domain.util.CacheUtil;
import com.matrixone.apps.domain.util.MapList;

public class DSCRecentlyAccessedParts_mxJPO {
    public DSCRecentlyAccessedParts_mxJPO() {

    }

    public DSCRecentlyAccessedParts_mxJPO(Context context, String[] args) throws Exception {
        if (!context.isConnected()) {
            MCADServerException.createException("not supported no desktop client", null);
        }
    }

    public int mxMain(Context context, String[] args) throws Exception {
        return 0;
    }

    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getRecentlyAccessedBusObjectsList(Context context, String[] args) throws Exception {
        MapList mResultList = new MapList();
        HashMap hashMapArgs = (HashMap) JPO.unpackArgs(args);
        String dateSelected = (String) hashMapArgs.get("dateSelected");
        String inputEncodedList = CacheUtil.getCacheString(context, "RECENTLY_ACCESSED_PARTS");

        mResultList = getRecentlyAccessedBusObjectsList(dateSelected, inputEncodedList);

        return mResultList;
    }

    private MapList getRecentlyAccessedBusObjectsList(String timeFrame, String inputEncodedList) throws Exception {
        MapList mResultList = new MapList();

        try {

            String inputList = MCADUrlUtil.decode(inputEncodedList);
            MapList recentlyAccessedList = (MapList) MCADUtil.covertToObject(inputList, true, true);

            mResultList = getRecentlyAccessedBusObjectList(timeFrame, recentlyAccessedList);
        } catch (Exception e) {
            System.out.println("DSCRecentlyAccessedParts:getRecentlyAccessedBusObjectsList :Error " + e.getMessage());
            e.printStackTrace();
        }

        return mResultList;
    }

    private MapList getRecentlyAccessedBusObjectsList(String timeFrame, String[] args) throws Exception {
        MapList mResultList = new MapList();

        try {
            HashMap hashMapArgs = (HashMap) JPO.unpackArgs(args);
            String inputEncodedList = (String) hashMapArgs.get("RECENTLY_ACCESSED_PARTS");
            String inputList = MCADUrlUtil.decode(inputEncodedList);
            MapList recentlyAccessedList = (MapList) MCADUtil.covertToObject(inputList, true, true);

            mResultList = getRecentlyAccessedBusObjectList(timeFrame, recentlyAccessedList);
        } catch (Exception e) {
            System.out.println("DSCRecentlyAccessedParts:getRecentlyAccessedBusObjectsList :Error " + e.getMessage());
            e.printStackTrace();
        }

        return mResultList;
    }

    private MapList getRecentlyAccessedBusObjectList(String timeFrame, MapList allList) throws Exception {
        MapList busObjectList = new MapList();
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy", Locale.US);
        SimpleDateFormat inputDateFormat = new SimpleDateFormat("MMM dd HH:mm:ss zzz yyyy", Locale.US);

        Date initialBoundaryDate = new Date();

        if (!timeFrame.trim().equalsIgnoreCase(MCADRecentlyAccessedPartsHelper.ALL)) {
            initialBoundaryDate = getInitialBoudaryDate(timeFrame);
            Iterator busObjectLists = allList.iterator();

            while (busObjectLists.hasNext()) {
                HashMap busObjMap = (HashMap) busObjectLists.next();
                Date accessDate = inputDateFormat.parse((String) busObjMap.get("Access Date"));

                if (timeFrame.trim().equalsIgnoreCase(MCADRecentlyAccessedPartsHelper.LAST_SEVEN_DAYS) || timeFrame.trim().equalsIgnoreCase(MCADRecentlyAccessedPartsHelper.LAST_FOURTEEN_DAYS)
                        || timeFrame.trim().equalsIgnoreCase(MCADRecentlyAccessedPartsHelper.LAST_THIRTY_DAYS) || timeFrame.trim().equalsIgnoreCase(MCADRecentlyAccessedPartsHelper.LAST_SIXTY_DAYS)
                        || timeFrame.trim().equalsIgnoreCase(MCADRecentlyAccessedPartsHelper.LAST_THIRTY_DAYS)) {
                    if (accessDate.getTime() >= initialBoundaryDate.getTime()) {
                        busObjectList.add(busObjMap);
                    }
                } else if (timeFrame.trim().equalsIgnoreCase(MCADRecentlyAccessedPartsHelper.TODAY)) {
                    String objDate = sdf.format(accessDate);
                    String todaysDate = sdf.format(initialBoundaryDate).trim();

                    if (objDate.trim().equalsIgnoreCase(todaysDate)) {
                        busObjectList.add(busObjMap);
                    }
                }

            }
        } else {
            busObjectList = allList;
        }

        return busObjectList;
    }

    private Date getInitialBoudaryDate(String timeFrame) {
        int index = 0;

        if (timeFrame.trim().equalsIgnoreCase(MCADRecentlyAccessedPartsHelper.LAST_SEVEN_DAYS)) {
            index = MCADRecentlyAccessedPartsHelper.SEVEN;
        }
        if (timeFrame.trim().equalsIgnoreCase(MCADRecentlyAccessedPartsHelper.LAST_FOURTEEN_DAYS)) {
            index = MCADRecentlyAccessedPartsHelper.FOURTEEN;
        }
        if (timeFrame.trim().equalsIgnoreCase(MCADRecentlyAccessedPartsHelper.LAST_THIRTY_DAYS)) {
            index = MCADRecentlyAccessedPartsHelper.THIRTY;
        }
        if (timeFrame.trim().equalsIgnoreCase(MCADRecentlyAccessedPartsHelper.LAST_SIXTY_DAYS)) {
            index = MCADRecentlyAccessedPartsHelper.SIXTY;
        }

        Calendar cal = new GregorianCalendar();
        cal.add(Calendar.DATE, -index);

        return cal.getTime();
    }

    public Vector getAccessDates(Context context, String[] args) {
        Vector timeStampList = null;
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList busObjectList = (MapList) programMap.get("objectList");

            if (busObjectList != null) {
                timeStampList = new Vector(busObjectList.size());
                Iterator itr = busObjectList.iterator();
                while (itr.hasNext()) {
                    HashMap newMap = (HashMap) itr.next();
                    String accessDate = (String) newMap.get("Access Date");
                    timeStampList.add(accessDate);
                }
            }
        } catch (Exception e) {
            System.out.println("\n\nDSCRecentlyAccessedParts:getAccessDates :Error " + e.getMessage());
            e.printStackTrace();
        }
        return timeStampList;
    }

}
