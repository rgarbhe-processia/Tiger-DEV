
/**
 * IEFFindRevision.java Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright
 * notice is precautionary only and does not evidence any actual or intended publication of such program This JPO gets all revisions for a BusinessObject Project. Infocentral Migration to UI level 3
 * $Archive: $ $Revision: 1.2$ $Author: ds-unamagiri$ rahulp
 * @since AEF 9.5.2.0
 */

import java.util.HashMap;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectList;
import matrix.db.Context;
import matrix.db.JPO;

import com.matrixone.apps.domain.util.MapList;

public class IEFFindRevision_mxJPO {
    public IEFFindRevision_mxJPO(Context context, String[] args) throws Exception {
    }

    @com.matrixone.apps.framework.ui.ProgramCallable
    public Object getList(Context context, String[] args) throws Exception {
        MapList totalresultList = new MapList();
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        String objectId = (String) paramMap.get("objectId");
        BusinessObject obj = new BusinessObject(objectId);
        obj.open(context);
        BusinessObjectList list = obj.getRevisions(context);
        for (int i = 0; i < list.size(); i++) {
            BusinessObject rev = (BusinessObject) list.get(i);
            rev.open(context);
            String busid = rev.getObjectId();
            HashMap map = new HashMap();
            map.put("id", busid);
            map.put("type", rev.getTypeName());
            map.put("name", rev.getName());
            map.put("revision", rev.getRevision());
            rev.close(context);
            totalresultList.add(map);
        }
        obj.close(context);
        return totalresultList;
    }
}
