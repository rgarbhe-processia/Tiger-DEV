
/*
 * DSCMeetingJPO.java
 *
 * Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 *
 */

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.Relationship;
import matrix.db.RelationshipItr;
import matrix.db.RelationshipList;
import matrix.db.RelationshipType;
import matrix.db.State;
import matrix.db.StateItr;
import matrix.db.StateList;
import matrix.util.MatrixException;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.apps.domain.util.PropertyUtil;

public class MeetingJPO_mxJPO {
    MCADMxUtil mxUtil = null;

    MCADServerResourceBundle serverResourceBundle = null;

    IEFGlobalCache cache = null;

    public MeetingJPO_mxJPO(Context context, String[] args) throws Exception {
    }

    public int mxMain(Context context, String[] args) throws Exception {
        return 0;
    }

    public String addCollaboratedDocument(Context context, String[] args) {
        String meetingId = args[0];
        String fileId = args[1];

        try {
            // Create with blank resource bundle, which will get the default locale language. Used only to create util object.
            mxUtil = new MCADMxUtil(context, new MCADServerResourceBundle(""), new IEFGlobalCache());

            String strRelMeetingAttachment = PropertyUtil.getSchemaProperty(context, "relationship_MeetingAttachments");
            BusinessObject busMeeting = new BusinessObject(meetingId);
            BusinessObject busFile = new BusinessObject(fileId);
            RelationshipType rel = new RelationshipType(strRelMeetingAttachment);
            RelationshipList relations = mxUtil.getFromRelationship(context, busMeeting, (short) 0, false);
            RelationshipItr relIter = new RelationshipItr(relations);
            boolean found = false;

            while (relIter.next()) {
                Relationship relObj = relIter.obj();
                BusinessObject fileObj = relObj.getTo();

                if (fileObj.getObjectId().equals(fileId)) {
                    found = true;
                    break;
                }
            }

            // if not, connect the file to the meeting object
            if (found == false) {
                // connect the from relationship
                busMeeting.connect(context, rel, true, busFile);
            }
        } catch (MatrixException e) {
            System.out.println(e.getMessage());
            return "FAILED";
        }

        return "SUCCESS";
    }

    public String closeMeeting(Context context, String[] args) {
        try {
            String meetingId = args[0];
            BusinessObject busMeeting = new BusinessObject(meetingId);
            busMeeting.open(context);

            // gets the all the states
            StateList states = busMeeting.getStates(context);
            StateItr stateIter = new StateItr(states);
            State currentState = null;

            // finds the current state
            while (stateIter.next()) {
                State stateObj = stateIter.obj();

                // checks for the current flag be true
                if (stateObj.isCurrent() == true) {
                    currentState = stateObj;
                    break;
                }

            }

            // if the current state is "In Progress", changes to "Complete"
            if ((currentState != null) && ((currentState.getName().equals("In Progress") == true))) {
                busMeeting.promote(context);
            }
            busMeeting.close(context);
        } catch (MatrixException e) {
            System.out.println(e.getMessage());
            return "FAILED";
        }

        return "SUCCESS";
    }
}
