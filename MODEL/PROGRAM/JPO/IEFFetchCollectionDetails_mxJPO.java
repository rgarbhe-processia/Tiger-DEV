
/**
 * IEFFetchCollectionDetails.java Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries,
 * Copyright notice is precautionary only and does not evidence any actual or intended publication of such program JPO to get all BusinessObjects in a collection $Archive: $ $Revision: 1.2$ $Author:
 * ds-unamagiri$
 * @since AEF 9.5.2.0
 */
import java.util.HashMap;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectList;
import matrix.db.Context;
import matrix.db.JPO;

import com.matrixone.apps.domain.util.MapList;

public class IEFFetchCollectionDetails_mxJPO {
    public IEFFetchCollectionDetails_mxJPO(Context context, String[] args) throws Exception {
    }

    @com.matrixone.apps.framework.ui.ProgramCallable
    public Object getList(Context context, String[] args) throws Exception {
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        String setName = (String) paramMap.get("setName");
        MapList mapList = null;

        try {
            // Define the Set object
            matrix.db.Set collection = null;
            collection = new matrix.db.Set(setName);
            collection.open(context);
            // Get all BusinessObject from the collection
            BusinessObjectList businessObjectList = collection.getBusinessObjects(context);
            // Clean up
            collection.close(context);
            // no of BusinessObject in the List
            int boSize = 0;
            if (businessObjectList != null)
                boSize = businessObjectList.size();
            // in case no BuissnessObject in the collection return a empty MapList
            else
                return new MapList();

            mapList = new MapList(boSize);

            String[] boList = new String[boSize];
            for (int i = 0; i < boSize; i++) {
                boList[i] = ((BusinessObject) businessObjectList.elementAt(i)).getObjectId();
                HashMap map = new HashMap();
                String id = boList[i];
                BusinessObject bus = new BusinessObject(id);
                bus.open(context);
                map.put("type", bus.getTypeName());
                map.put("name", bus.getName());
                map.put("revision", bus.getRevision());
                bus.close(context);
                map.put("id", id);
                mapList.add(map);
            }
        } catch (Exception ex) {
            mapList = new MapList();
        }
        return mapList;
    }
}
