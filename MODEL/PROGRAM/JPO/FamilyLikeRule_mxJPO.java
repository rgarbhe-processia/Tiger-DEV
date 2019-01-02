import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import matrix.db.Attribute;
import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.Relationship;
import matrix.db.RelationshipList;
import matrix.db.RelationshipType;
import matrix.db.State;
import matrix.util.MatrixException;
import matrix.util.StringList;
import com.matrixone.MCADIntegration.DataValidation.util.DataValidationUtil;
import com.matrixone.MCADIntegration.utils.MCADException;

import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.MqlUtil;
/*
 ** ${CLASSNAME}
 **
 ** Copyright (c) 1993-2016 Dassault Systemes. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes. Copyright notice is precautionary only and does
 * not evidence any actual or intended publication of such program
 */

public class FamilyLikeRule_mxJPO extends CustomBaseRule_mxJPO {

    public String validate(Context context, String[] args) throws MatrixException, Exception {
        super.validate(context, args);

        try {
            if (null == gcoObject)
                return "GCO Object is null";

            if (null == validationObject)
                return "ValidationObject is null";

            BusinessObject revisionObject = validationObject.getValidationObject();

            if (null == revisionObject)
                return "Revision Object is null";

            // -------------------------------------------------------------------------------
            // Check family like rules for major
            // -------------------------------------------------------------------------------
            String errorMsg = CheckFamilyLikeRule(context, revisionObject);

            if (null != errorMsg)
                AddErrorMessage(errorMsg);

            // -------------------------------------------------------------------------------
            // Check family like rules for minors
            // -------------------------------------------------------------------------------
            List<BusinessObject> listMinors = validationObject.getMinors();

            errorMsg = null;
            for (BusinessObject object : listMinors) {
                object.open(context);
                String tempErrorMsg = CheckFamilyLikeRule(context, object);
                if (null != tempErrorMsg) {
                    if (null == errorMsg)
                        errorMsg = tempErrorMsg + object.getObjectId();
                    else
                        errorMsg += tempErrorMsg + object.getObjectId();
                }
            }

            if (null != errorMsg)
                AddErrorMessage(errorMsg);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (null == super._errorMessage)
            super._errorMessage = "OK";

        return super._errorMessage;
    }

    private StringList getAllowedTypes(Context context, String relTypeName, boolean toSide) throws FrameworkException {
        StringList typeList = null;
        Map<?, ?> allowedFromTypes = DomainRelationship.getAllowedTypes(context, relTypeName, toSide, true);
        if (!allowedFromTypes.isEmpty()) {
            String strFromType = (String) allowedFromTypes.get(DomainRelationship.KEY_INCLUDE);
            String[] strlTypes = strFromType.split(",");
            typeList = new StringList(strlTypes);
        }

        return typeList;
    }

    private boolean isFromTypeValid(RelationshipType typeName, String fromObjectType) throws FrameworkException {

        boolean retValue = false;

        StringList typeList = getAllowedTypes(this.context, typeName.toString(), false);
        String strSymbolicType = null;
        strSymbolicType = FrameworkUtil.getAliasForAdmin(this.context, "type", fromObjectType, true);
        if (null != typeList && typeList.contains(strSymbolicType))
            retValue = true;

        return retValue;
    }

    private boolean isToTypeValid(RelationshipType typeName, String toObjectType) throws FrameworkException {

        boolean retValue = false;

        StringList typeList = getAllowedTypes(this.context, typeName.toString(), true);
        String strSymbolicType = null;
        strSymbolicType = FrameworkUtil.getAliasForAdmin(this.context, "type", toObjectType, true);
        if (null != typeList && typeList.contains(strSymbolicType))
            retValue = true;

        return retValue;
    }

    private String CheckFamilyLikeRule(Context context, BusinessObject iBusObject) throws Exception {
        String errorMsg = null;
        String fromDirErrorMessage = null;
        String toDirErrorMessage = null;
        String stateErrorMessage = null;
        String titleErrorMessage = null;

        boolean isFamilyLike = false;
        boolean isInstanceLike = false;
        int nFamliyLikeRelationships = 0;

        Vector familyMxTypes = gcoObject.getValidFamilyTypes();
        Vector instanceMxTypes = DataValidationUtil.getValidInstanceTypeForFamily(familyMxTypes, gcoObject);

        // Check if the current object is of valid family type
        String revisionObjectTypeName = iBusObject.getTypeName();
        if (familyMxTypes.size() > 0 && familyMxTypes.indexOf(revisionObjectTypeName) >= 0)
            isFamilyLike = true;

        if (instanceMxTypes.size() > 0 && instanceMxTypes.indexOf(revisionObjectTypeName) >= 0)
            isInstanceLike = true;

        // Check if Title is present
        titleErrorMessage = this.IsTitleMissing(context, iBusObject);

        Hashtable mxRels = new Hashtable(10);
        // get rel name & dirn table & also update cache
        gcoObject.getMxRelNameDirnTableForClass(mxRels, "all");
        RelationshipList relationships = iBusObject.getAllRelationship(context);

        // TODO: improve this logic , expensive
        Iterator<Relationship> itr = relationships.iterator();

        while (itr.hasNext()) {
            Relationship relationship = itr.next();
            String relTypeName = relationship.getTypeName();

            if (gcoObject.isRelationshipOfClass(relTypeName, "FamilyLike")) {
                ++nFamliyLikeRelationships;

                // Check for proper direction
                BusinessObject fromObject = relationship.getFrom();
                String fromObjectType = fromObject.getTypeName();
                RelationshipType typeName = relationship.getRelationshipType();

                if (!isFromTypeValid(typeName, fromObjectType)) {
                    fromDirErrorMessage = "Following types are not mapped to the relationship\n";
                    fromDirErrorMessage += "Type: " + typeName + " relationship: " + relTypeName + "'From' side";
                }

                BusinessObject toObject = relationship.getTo();
                String toObjectType = toObject.getTypeName();
                if (!isToTypeValid(typeName, toObjectType)) {
                    toDirErrorMessage = "Following types are not mapped to the relationship\n";
                    toDirErrorMessage += "Type: " + typeName + " relationship: " + relTypeName + "'To' side";
                }

                if (!validationObject.isAVersionObject())
                    stateErrorMessage = CheckIfFamilyAndInstancesAreAtSameState(context, fromObject, toObject);

            }
        }

        if (nFamliyLikeRelationships == 0) {
            if (isFamilyLike)
                fromDirErrorMessage = "Missing FamilyLike relationship: Family object is without instance.";
            else if (isInstanceLike)
                fromDirErrorMessage = "Missing FamilyLike relationship: Instance object is without family.";
        }

        if (nFamliyLikeRelationships > 1 && isInstanceLike)
            fromDirErrorMessage = "Instance is attached with more than one FamilyLike relationship.";

        String erroMsgActiveInstance = CheckForActiveInstance(context, iBusObject);

        errorMsg = AddErrorMessage(errorMsg, fromDirErrorMessage);
        errorMsg = AddErrorMessage(errorMsg, toDirErrorMessage);
        errorMsg = AddErrorMessage(errorMsg, stateErrorMessage);
        errorMsg = AddErrorMessage(errorMsg, erroMsgActiveInstance);

        return errorMsg;
    }

    private String IsTitleMissing(Context context, BusinessObject iBusObj) throws MatrixException {
        String errorMessage = null;

        String attributeTitle = DataValidationUtil.getActualNameForAEFData(context, "attribute_Title");
        Attribute attrTitle = iBusObj.getAttributeValues(context, attributeTitle);

        if (null == attrTitle || (null != attrTitle && attrTitle.getValue().isEmpty()))
            errorMessage = "Title is empty";

        return errorMessage;
    }

    private String CheckIfFamilyAndInstancesAreAtSameState(Context context, BusinessObject fromObject, BusinessObject toObject) throws Exception {
        String errorMessage = null;

        State fromState = DataValidationUtil.getCurrentState(context, fromObject);
        State toState = DataValidationUtil.getCurrentState(context, toObject);
        if (fromState.getName().compareTo(toState.getName()) != 0)
            errorMessage = "State mismatch for " + fromObject.getObjectId() + ": " + fromState.getName() + "; " + toObject.getObjectId() + ": " + toState.getName();

        return errorMessage;
    }

    private String CheckForActiveInstance(Context context, BusinessObject fromObject) {
        String errorMessage = null;

        String busId = fromObject.getObjectId();
        String cadType = getCADTypeFromBO(context, busId);

        if (gcoObject.isTypeOfClass(cadType, "TYPE_FAMILY_LIKE")) {
            if (!hasActiveInstance(context, busId))
                errorMessage = "Missing Active Instance for: " + busId;
        }
        return errorMessage;
    }

    private String getCADTypeFromBO(Context context, String objectId) {
        String cadType = "";
        try {

            String actualName = getActualNameForAEFData(context, "attribute_CADType");
            String args[] = new String[3];
            args[0] = objectId;
            args[1] = "attribute[" + actualName + "]";
            args[2] = "dump";

            String MQLResult = MqlUtil.mqlCommand(context, "print bus $1 select $2 $3", args);

            if (null != MQLResult && !MQLResult.isEmpty())
                cadType = MQLResult;

        } catch (Exception e) {
            // TODO Auto-generated catch block
            // e.printStackTrace();
        }
        return cadType;
    }

    private String getActualNameForAEFData(Context context, String symbolicName) throws MCADException {
        String actualName = (String) PropertyUtil.getSchemaProperty(context, symbolicName);

        return actualName;
    }

    private boolean hasActiveInstance(Context context, String objectId) {
        boolean bRetVal = false;
        try {

            String args[] = new String[3];
            args[0] = objectId;
            args[1] = "from[Active Instance].to.id";
            args[2] = "dump";

            String MQLResult = MqlUtil.mqlCommand(context, "print bus $1 select $2 $3", args);

            if (null != MQLResult && !MQLResult.isEmpty())
                bRetVal = true;

        } catch (Exception e) {
            // TODO Auto-generated catch block
            // e.printStackTrace();
        }
        return bRetVal;
    }
}
