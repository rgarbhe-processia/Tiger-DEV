
/*
 * cusProjectRetrieverForChanges.java
 *
 * Copyright (c) 1992-2015 Dassault Systemes.
 *
 * All Rights Reserved. This program contains proprietary and trade secret information of MatrixOne, Inc. Copyright notice is precautionary only and does not evidence any actual or intended
 * publication of such program.
 *
 */
import java.lang.*;
import java.util.Locale;
import java.util.Vector;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

import matrix.db.*;
import matrix.util.StringList;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.PropertyUtil;

import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeConstants;

/**
 *
 */
public class cusProjectRetrieverForChanges_mxJPO {
    public cusProjectRetrieverForChanges_mxJPO() throws Exception {
    }

    /**
     * This method returns the names of the projects associated with the Changes Request/Order/Action
     * @param context
     * @param args
     * @return Vector
     * @throws Exception
     * @since V6R2014x
     */
    public Vector getChangesProjects(Context context, String[] args) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        Vector result = new Vector();

        MapList busObjectList = (MapList) programMap.get("objectList");

        for (Iterator iterator = busObjectList.iterator(); iterator.hasNext();) {
            Map tempMap = (Map) iterator.next();
            String tmpOID = (String) tempMap.get(DomainConstants.SELECT_ID);
            String tmpType = (String) tempMap.get(DomainConstants.SELECT_TYPE);
            DomainObject tmpObj = DomainObject.newInstance(context, tmpOID);
            String sPrjName;
            if (tmpType.equals(ChangeConstants.TYPE_CHANGE_ACTION)) {
                // sPrjName = tmpObj.getInfo(context, "to["+ChangeConstants.RELATIONSHIP_CHANGE_ACTION+"].from.to["+DomainConstants.RELATIONSHIP_DESIGN_RESPONSIBILITY+"].from.name");
                sPrjName = tmpObj.getInfo(context, "to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from.to[PSS_ConnectedPCMData].from.name");
            } else {
                // sPrjName = tmpObj.getInfo(context, "to["+DomainConstants.RELATIONSHIP_DESIGN_RESPONSIBILITY+"].from.name");
                sPrjName = tmpObj.getInfo(context, "to[PSS_ConnectedPCMData].from.name");
            }
            result.addElement(sPrjName == null ? "" : sPrjName);
        }
        return result;
    }

}