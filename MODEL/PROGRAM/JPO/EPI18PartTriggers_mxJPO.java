import java.util.Map;
import java.util.Vector;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;

import matrix.db.*;
import matrix.util.*;

import com.matrixone.apps.domain.*;
import com.matrixone.apps.domain.util.*;
import com.matrixone.apps.common.Company;

public class EPI18PartTriggers_mxJPO {
    public EPI18PartTriggers_mxJPO(Context context, String[] args) {
    }

    public static int attachModelsToRevisePart(Context context, String[] args) throws Exception {
        try {
            System.out.println("EPI18PartTriggers : attachModelsToRevisePart " + "${OBJECTID}=" + args[0]);
            String partOldRevPID = args[0];

            String partOldRevType = MqlUtil.mqlCommand(context, "print bus " + partOldRevPID + " select type dump |", false, false);
            // System.out.println("EPI18PartTriggers : partOldRevType = " + partOldRevType);

            String partOldRevName = MqlUtil.mqlCommand(context, "print bus " + partOldRevPID + " select name dump |", false, false);
            // System.out.println("EPI18PartTriggers : partOldRevName = " + partOldRevName);

            String partNewRev = MqlUtil.mqlCommand(context, "print bus " + partOldRevPID + " select lastminor dump |", false, false);
            // System.out.println("EPI18PartTriggers : partNewRev = " + partNewRev);

            String partNewRevPID = MqlUtil.mqlCommand(context, "print bus '" + partOldRevType + "' '" + partOldRevName + "' '" + partNewRev + "' select physicalid dump |", false, false);
            // System.out.println("EPI18PartTriggers : partNewRevPID = " + partNewRevPID);

            String modelPIDListStr = MqlUtil.mqlCommand(context, "print bus " + partOldRevPID + " select from[Configuration Context].to.physicalid dump |", false, false);
            // System.out.println("EPI18PartTriggers : modelPIDListStr = " + modelPIDListStr);

            if (!"".equals(modelPIDListStr)) {
                String[] modelPIDList = modelPIDListStr.split("\\|");
                for (String modelPID : modelPIDList) {
                    // System.out.println("EPI18PartTriggers : modelPID = " + modelPID);
                    MqlUtil.mqlCommand(context, "add connection 'Configuration Context' from " + partNewRevPID + " to " + modelPID, false, false);
                    System.out.println("EPI18PartTriggers : Done.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

}