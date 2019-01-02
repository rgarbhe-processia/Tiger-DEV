
/**
 * ${CLASSNAME}.java Copyright Dassault Systemes, 1992-2015. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright
 * notice is precautionary only and does not evidence any actual or intended publication of such program This JPO performs Pre-SaveAs Evaluation of the MCAD model.
 */

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.PropertyUtil;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

public class DECPreSaveAsCheck_mxJPO {

    public DECPreSaveAsCheck_mxJPO(Context context, String[] args) throws Exception {

    }

    public String[] checkSaveAs(Context c, String[] args) throws Exception {
        String[] sRet = null;

        sRet = isExclude(c, args);

        if (sRet == null || sRet[0].equalsIgnoreCase("FALSE"))
            sRet = isReuse(c, args);
        else
            sRet = new String[] { "FALSE" };

        return sRet;
    }

    public String[] isExclude(Context c, String[] args) throws Exception {
        String[] sRet = null;

        String sourceName = args[0];
        String sourceType = args[1];
        String SourceRev = args[2];
        String sourceBusid = args[3];
        String sourcePhid = args[4];
        String sourceCollabSapce = args[5];
        String selectedRegEx = args[6];
        String replaceString = args[7];
        String targetName = args[8];
        String targetRev = args[9];

        // logic to determine if a design is standard to not. Return TRUE incase of standard design
        // Sample Logic based on Source Object's collab space
        if (sourceCollabSapce == null || sourceCollabSapce.length() == 0) {
            sRet = new String[] { "FALSE" };
        } else {
            String projectFamily = PropertyUtil.getAdminProperty(c, "Role", sourceCollabSapce, "FAMILY");

            if (null != projectFamily && projectFamily.contains("Standard"))
                sRet = new String[] { "TRUE" };
            else
                sRet = new String[] { "FALSE" };
        }
        return sRet;

    }

    public String[] isReuse(Context c, String[] args) throws Exception {
        String[] sRet = null;

        String sourceName = args[0];
        String sourceType = args[1];
        String sourceRev = args[2];
        String sourceBusid = args[3];
        String sourcePhid = args[4];
        String sourceCollabSapce = args[5];
        String selectedRegEx = args[6];
        String replaceString = args[7];
        String targetName = args[8];
        String targetRev = args[9];

        // write a logic to determine if source object is to be used as common and get its common name to be used
        // then check if object with that common name exists, if yes, the String Array {TRUE <physical id of design to be reused>};
        // Sample logic based on Regular Expression is given below

        if (null == targetName || targetName.trim().length() == 0) {
            targetName = sourceName.replaceAll(selectedRegEx, replaceString);
            targetRev = sourceRev;
            BusinessObject targetObj = new BusinessObject(sourceType, targetName, targetRev, "");
            if (targetObj.exists(c)) {
                DomainObject targetDomObject = DomainObject.newInstance(c, targetObj.getObjectId(c));
                StringList selectables = new StringList();
                selectables.add("physicalid");
                Map objInfo = targetDomObject.getInfo(c, selectables);
                String sPhyId = (String) objInfo.get("physicalid");

                sRet = new String[] { "TRUE", targetObj.getObjectId(c) };

            } else {
                sRet = new String[] { "FALSE", targetName };
            }

        }
        // logic to determine if an object with targetName & targetRevision is already present
        // and is common part to be reused
        // if yes then return String Array TRUE <physical id of design to be reused>};

        // Sample logic based on Target Name is as below
        else if (null != targetName && targetName.startsWith("PRJ")) {
            BusinessObject targetObj = new BusinessObject(sourceType, targetName, targetRev, "");
            if (targetObj.exists(c)) {
                DomainObject targetDomObject = DomainObject.newInstance(c, targetObj.getObjectId(c));
                StringList selectables = new StringList();
                selectables.add("physicalid");
                Map objInfo = targetDomObject.getInfo(c, selectables);
                String sPhyId = (String) objInfo.get("physicalid");

                sRet = new String[] { "TRUE", sPhyId };
            } else
                sRet = new String[] { "FALSE", targetName };
        }

        return sRet;
    }

}
