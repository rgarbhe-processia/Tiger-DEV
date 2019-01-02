package fpdm.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.Relationship;
import matrix.db.RelationshipWithSelect;
import matrix.db.RelationshipWithSelectList;
import matrix.util.MatrixException;
import matrix.util.StringList;

public class SelectData_mxJPO {

    /**
     * To retrieve multiple information on multiple object. No need to instantiate BusinessObject or DomainObject
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param saIds
     *            The list of id
     * @param slSelect
     *            The information to select
     * @return a Map, where keys are the ids and values are a map. In the value Maps, keys are the names of selected information and values are values of selected information (values are Objects, it
     *         can be String or ArrayList<String>) ex : Map<"12345.12345.12345.12345", Map<"current", "Preliminary">> ex : Map<"12345.12345.12345.12345", Map<"state",
     *         {"Preliminary","Review","Release","Canceled"}>>
     * @throws MatrixException
     */
    public static Map<String, Map<String, Object>> getSelectBusinessObjectData(Context context, String[] saIds, StringList slSelect) throws MatrixException {
        Map<String, Map<String, Object>> mObjectsInformation = new HashMap<String, Map<String, Object>>();
        if (saIds != null && saIds.length > 0) {
            BusinessObjectWithSelectList bowsList = BusinessObject.getSelectBusinessObjectData(context, saIds, slSelect);
            if (bowsList != null && saIds.length == bowsList.size()) {
                int k = 0;
                for (Object oBusSelect : bowsList) {
                    if (oBusSelect instanceof BusinessObjectWithSelect) {
                        String sId = saIds[k];
                        Map<String, Object> mValue = new HashMap<String, Object>();
                        BusinessObjectWithSelect bowSelect = (BusinessObjectWithSelect) oBusSelect;
                        populateReturnMap(mValue, bowSelect.getSelectKeys(), bowSelect.getSelectValues());
                        mObjectsInformation.put(sId, mValue);
                    }
                    k++;
                }
            }
        }
        return mObjectsInformation;
    }

    /**
     * To retrieve multiple information on a single object. No need to instantiate BusinessObject or DomainObject
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param sId
     *            The id
     * @param slSelect
     *            The information to select
     * @return a value Map, keys are the names of selected information and values are values of selected information (values are Objects, it can be String or ArrayList<String>) ex : Map<"current",
     *         "Preliminary"> ex : Map<"state", {"Preliminary","Review","Release","Canceled"}>
     * @throws MatrixException
     */
    public static Map<String, Object> getSelectBusinessObjectData(Context context, String sId, StringList slSelect) throws MatrixException {
        Map<String, Object> mObjectsInformation = new HashMap<String, Object>();
        if (sId != null && sId.length() > 0) {
            BusinessObjectWithSelectList bowsList = BusinessObject.getSelectBusinessObjectData(context, new String[] { sId }, slSelect);
            if (bowsList != null && bowsList.size() == 1) {
                for (Object oBusSelect : bowsList) {
                    if (oBusSelect instanceof BusinessObjectWithSelect) {
                        BusinessObjectWithSelect bowSelect = (BusinessObjectWithSelect) oBusSelect;
                        populateReturnMap(mObjectsInformation, bowSelect.getSelectKeys(), bowSelect.getSelectValues());
                    }
                }
            }
        }
        return mObjectsInformation;
    }

    /**
     * To retrieve single information (with possibly multiple values) on a single object. No need to instantiate BusinessObject or DomainObject ONLY ONE KEY IS EXPECTED IN RETURN. Ex : Select
     * from[*].to.id may return multiple keys (from[EBOM].to.id=XXX, from[EBOM history].to.id==XXX), in that case, the method will only return the values of the first key. Ex : Select revisions.id may
     * return multiple keys (revisions[01].id=XXX, revisions[02].id==XXX ...), in that case, the method will only return the id of the revision 01.
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param sId
     *            The id
     * @param sSelect
     *            The information to select
     * @return an Object (a String or an ArrayList of String)
     * @throws MatrixException
     */
    public static Object getSelectBusinessObjectData(Context context, String sId, String sSelect) throws MatrixException {
        if (sId != null && sId.length() > 0) {
            BusinessObjectWithSelectList bowsList = BusinessObject.getSelectBusinessObjectData(context, new String[] { sId }, new StringList(sSelect));
            if (bowsList != null && bowsList.size() == 1) {
                for (Object oBusSelect : bowsList) {
                    if (oBusSelect instanceof BusinessObjectWithSelect) {
                        BusinessObjectWithSelect bowSelect = (BusinessObjectWithSelect) oBusSelect;
                        Vector<?> vValues = bowSelect.getSelectValues();
                        if (vValues.size() > 0) {
                            String sData = (String) vValues.get(0);
                            return getDataStringOrList(sData);
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * To retrieve multiple information on a single Relationship. No need to instantiate Relationship
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param sId
     *            The id
     * @param slSelect
     *            The information to select
     * @return a value Map, keys are the names of selected information and values are values of selected information (values are Objects, it can be String or ArrayList<String>) ex : Map<"current",
     *         "Preliminary"> ex : Map<"state", {"Preliminary","Review","Release","Canceled"}>
     * @throws MatrixException
     */
    public static HashMap<String, Object> getSelectRelationshipData(Context context, String sId, StringList slSelect) throws MatrixException {
        HashMap<String, Object> mObjectsInformation = new HashMap<String, Object>();
        if (sId != null && sId.length() > 0) {
            RelationshipWithSelectList bowsList = Relationship.getSelectRelationshipData(context, new String[] { sId }, slSelect);
            if (bowsList != null && bowsList.size() == 1) {
                for (Object oBusSelect : bowsList) {
                    if (oBusSelect instanceof RelationshipWithSelect) {
                        RelationshipWithSelect rwSelect = (RelationshipWithSelect) oBusSelect;
                        populateReturnMap(mObjectsInformation, rwSelect.getSelectKeys(), rwSelect.getSelectValues());
                    }
                }
            }
        }
        return mObjectsInformation;
    }

    /**
     * To retrieve multiple information on multiple Relationships. No need to instantiate Relationship
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param saIds
     *            The list of id
     * @param slSelect
     *            The information to select
     * @return a Map, where keys are the ids and values are a map. In the value Maps, keys are the names of selected information and values are values of selected information (values are Objects, it
     *         can be String or ArrayList<String>) ex : Map<"12345.12345.12345.12345", Map<"current", "Preliminary">> ex : Map<"12345.12345.12345.12345", Map<"state",
     *         {"Preliminary","Review","Release","Canceled"}>>
     * @throws MatrixException
     */
    public static Map<String, Map<String, Object>> getSelectRelationshipData(Context context, String[] saIds, StringList slSelect) throws MatrixException {
        Map<String, Map<String, Object>> mObjectsInformation = new HashMap<String, Map<String, Object>>();
        if (saIds != null && saIds.length > 0) {
            RelationshipWithSelectList bowsList = Relationship.getSelectRelationshipData(context, saIds, slSelect);
            if (bowsList != null && saIds.length == bowsList.size()) {
                int k = 0;
                for (Object oRelSelect : bowsList) {
                    if (oRelSelect instanceof RelationshipWithSelect) {
                        String sId = saIds[k];
                        Map<String, Object> mValue = new HashMap<String, Object>();
                        RelationshipWithSelect rwSelect = (RelationshipWithSelect) oRelSelect;
                        populateReturnMap(mValue, rwSelect.getSelectKeys(), rwSelect.getSelectValues());
                        mObjectsInformation.put(sId, mValue);
                    }
                    k++;
                }
            }
        }
        return mObjectsInformation;
    }

    /**
     * To retrieve multiple information on multiple object in a MapList. No need to instantiate BusinessObject or DomainObject
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param saIds
     *            The list of id
     * @param slSelect
     *            The information to select
     * @return a MapList
     * @throws MatrixException
     */
    public static ArrayList<Map<String, Object>> getSelectMapList(Context context, String[] saIds, StringList slSelect) throws MatrixException {
        ArrayList<Map<String, Object>> alObjectsInformation = new ArrayList<Map<String, Object>>();
        if (saIds != null && saIds.length > 0) {
            BusinessObjectWithSelectList bowsList = BusinessObject.getSelectBusinessObjectData(context, saIds, slSelect);
            if (bowsList != null && saIds.length == bowsList.size()) {
                int k = 0;
                for (Object oBusSelect : bowsList) {
                    if (oBusSelect instanceof BusinessObjectWithSelect) {
                        String sId = saIds[k];
                        Map<String, Object> mValue = new HashMap<String, Object>();
                        BusinessObjectWithSelect bowSelect = (BusinessObjectWithSelect) oBusSelect;
                        populateReturnMap(mValue, bowSelect.getSelectKeys(), bowSelect.getSelectValues());
                        mValue.put(DomainObject.SELECT_ID, sId);
                        alObjectsInformation.add(mValue);
                    }
                    k++;
                }
            }
        }
        return alObjectsInformation;
    }

    /**
     * To retrieve multiple information on multiple object in a MapList. No need to instantiate BusinessObject or DomainObject
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param saIds
     *            The list of id
     * @param slSelect
     *            The information to select
     * @return a MapList
     * @throws MatrixException
     */
    public static ArrayList<Map<String, Object>> getSelectRelsMapList(Context context, String[] saIds, StringList slSelect) throws MatrixException {
        ArrayList<Map<String, Object>> alObjectsInformation = new ArrayList<Map<String, Object>>();
        if (saIds != null && saIds.length > 0) {
            RelationshipWithSelectList relsList = Relationship.getSelectRelationshipData(context, saIds, slSelect);
            if (relsList != null && saIds.length == relsList.size()) {
                int k = 0;
                for (Object oRelSelect : relsList) {
                    if (oRelSelect instanceof RelationshipWithSelect) {
                        String sId = saIds[k];
                        Map<String, Object> mValue = new HashMap<String, Object>();
                        RelationshipWithSelect relSelect = (RelationshipWithSelect) oRelSelect;
                        populateReturnMap(mValue, relSelect.getSelectKeys(), relSelect.getSelectValues());
                        mValue.put(DomainObject.SELECT_ID, sId);
                        alObjectsInformation.add(mValue);
                    }
                    k++;
                }
            }
        }
        return alObjectsInformation;
    }

    /**
     * To retrieve multiple information on multiple object in a MapList. No need to instantiate BusinessObject or DomainObject
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param saIds
     *            The list of id
     * @param slSelect
     *            The information to select
     * @return a MapList
     * @throws MatrixException
     */
    public static ArrayList<Map<String, Object>> getRelatedInfos(Context context, String sId, String sSelectRelatedId, StringList slSelect) throws MatrixException {
        Map<String, Object> mInfos = getSelectBusinessObjectData(context, sId, new StringList(sSelectRelatedId));
        Collection<Object> cRelated = mInfos.values();
        ArrayList<String> alAllRelateds = new ArrayList<String>();
        for (Object oRelated : cRelated) {
            alAllRelateds.addAll(getListOfValues(oRelated));
        }
        if (alAllRelateds.size() > 0) {
            String[] saRelateds = new String[alAllRelateds.size()];
            alAllRelateds.toArray(saRelateds);
            return getSelectMapList(context, saRelateds, slSelect);
        }
        return new ArrayList<Map<String, Object>>();
    }

    /**
     * To retrieve multiple information on multiple object in a MapList. No need to instantiate BusinessObject or DomainObject
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param saIds
     *            The list of id
     * @param slSelect
     *            The information to select
     * @return a MapList
     * @throws MatrixException
     */
    public static ArrayList<Map<String, Object>> getRelatedInfosByRel(Context context, String sId, String sSelectRelationId, StringList slSelectOnRels, boolean bSelectFromObject,
            StringList slSelectOnRelatedObjects) throws MatrixException {
        Map<String, Object> mInfos = getSelectBusinessObjectData(context, sId, new StringList(sSelectRelationId));
        Collection<Object> cRels = mInfos.values();
        ArrayList<String> alAllRels = new ArrayList<String>();
        for (Object oRel : cRels) {
            alAllRels.addAll(getListOfValues(oRel));
        }
        if (alAllRels.size() > 0) {
            String[] saRels = new String[alAllRels.size()];
            alAllRels.toArray(saRels);
            return getRelatedObjectsInfosFromRelsAndSelectOnRels(context, saRels, slSelectOnRels, bSelectFromObject, slSelectOnRelatedObjects);
        }
        return new ArrayList<Map<String, Object>>();
    }

    private static ArrayList<Map<String, Object>> getRelatedObjectsInfosFromRelsAndSelectOnRels(Context context, String[] saRels, StringList slSelectOnRels, boolean bSelectFromObject,
            StringList slSelectOnRelatedObjects) throws MatrixException {
        String sSelectRelatedObjects = null;
        if (slSelectOnRelatedObjects != null && slSelectOnRelatedObjects.size() > 0) {
            if (bSelectFromObject) {
                sSelectRelatedObjects = "from.id";
            } else {
                sSelectRelatedObjects = "to.id";
            }
            if (slSelectOnRels == null) {
                slSelectOnRels = new StringList();
            }
            slSelectOnRels.addElement(sSelectRelatedObjects);
        }
        if (slSelectOnRels != null && slSelectOnRels.size() > 0) {
            ArrayList<Map<String, Object>> mlToReturn = new ArrayList<Map<String, Object>>();
            Map<String, Map<String, Object>> mRelInfos = getSelectRelationshipData(context, saRels, slSelectOnRels);
            ArrayList<String> alRelatedObjects = new ArrayList<String>();
            for (Entry<String, Map<String, Object>> eEntry : mRelInfos.entrySet()) {
                String sRelId = eEntry.getKey();
                Map<String, Object> mRelInfo = eEntry.getValue();
                mRelInfo.put(DomainConstants.SELECT_RELATIONSHIP_ID, sRelId);
                if (sSelectRelatedObjects != null) {
                    String sObjectId = getSingleValue(mRelInfo.get(sSelectRelatedObjects));
                    mRelInfo.put(DomainConstants.SELECT_ID, sObjectId);
                    mlToReturn.add(mRelInfo);
                    alRelatedObjects.add(sObjectId);
                }
            }
            if (slSelectOnRelatedObjects != null && slSelectOnRelatedObjects.size() > 0 && alRelatedObjects.size() > 0) {
                String[] saIds = new String[alRelatedObjects.size()];
                alRelatedObjects.toArray(saIds);
                Map<String, Map<String, Object>> mObjectInfos = getSelectBusinessObjectData(context, saIds, slSelectOnRelatedObjects);
                for (Map<String, Object> mRelInfo : mlToReturn) {
                    String sObjectId = (String) mRelInfo.get(DomainConstants.SELECT_ID);
                    if (sObjectId != null) {
                        Map<String, Object> mObjectInfo = mObjectInfos.get(sObjectId);
                        mRelInfo.putAll(mObjectInfo);
                    }
                }
            }
            return mlToReturn;
        }
        ArrayList<Map<String, Object>> mlToReturn = new ArrayList<Map<String, Object>>();
        for (String sRelId : saRels) {
            Map<String, Object> mRelInfos = new HashMap<String, Object>();
            mRelInfos.put(DomainConstants.SELECT_RELATIONSHIP_ID, sRelId);
        }
        return mlToReturn;
    }

    /**
     * Parse keys and values to populate the Map of values
     * @param mReturn
     * @param vKeys
     * @param vValues
     */
    @SuppressWarnings("unchecked")
    private static void populateReturnMap(Map<String, Object> mReturn, Vector<?> vKeys, Vector<?> vValues) {
        int index = 0;
        if (vKeys.size() > vValues.size()) {
            System.err.println("there is more keys : " + vKeys.size() + " than values : " + vValues.size());
        }
        for (Object oSelect : vKeys) {
            if (oSelect instanceof String) {
                String sData = (String) vValues.get(index);
                Object oData = getDataStringOrList(sData);
                Object oExisting = mReturn.get((String) oSelect);
                if (oExisting != null) {
                    ArrayList<String> alExistingValues = null;
                    if (oExisting instanceof String) {
                        alExistingValues = new ArrayList<String>();
                        alExistingValues.add((String) oExisting);
                    } else if (oExisting instanceof ArrayList) {
                        alExistingValues = (ArrayList<String>) oExisting;
                    }
                    if (alExistingValues != null) {
                        if (oData instanceof String) {
                            alExistingValues.add((String) oData);
                        } else if (oData instanceof ArrayList) {
                            alExistingValues.addAll((ArrayList<String>) oData);
                        }
                    }
                    mReturn.put((String) oSelect, alExistingValues);
                } else {
                    mReturn.put((String) oSelect, oData);
                }
            }
            index++;
        }
    }

    /**
     * Standard process to retrieve a single value or a list of values
     * @param sData
     *            The data directly returned by getSelectBusinessObjectData
     * @return A String or an ArrayList<String>
     */
    private static Object getDataStringOrList(String sData) {
        if (sData != null && sData.indexOf('\7') >= 0) {
            // return it as an ArrayList
            ArrayList<String> alValues = new ArrayList<String>();
            int prev = 0;
            int curr;
            while ((curr = sData.indexOf('\7', prev)) >= 0) {
                alValues.add(sData.substring(prev, curr));
                prev = curr + 1;
            }
            alValues.add((prev > 0) ? sData.substring(prev) : sData);
            return alValues;
        } else {
            // return it as a String
            return sData;
        }
    }

    /**
     * Transform an object to an ArrayList<String> if object is one of String, ArrayList<String> or StringList
     * @param oValue
     * @return an ArrayList<String>
     */
    @SuppressWarnings("unchecked")
    public static ArrayList<String> getListOfValues(Object oValue) {
        if (oValue instanceof ArrayList) {
            return (ArrayList<String>) oValue;
        } else if (oValue instanceof String) {
            ArrayList<String> alValues = new ArrayList<String>();
            alValues.add((String) oValue);
            return alValues;
        } else if (oValue instanceof StringList) {
            ArrayList<String> alValues = new ArrayList<String>();
            for (Iterator<?> ite = ((StringList) oValue).iterator(); ite.hasNext();) {
                String sValue = (String) ite.next();
                alValues.add(sValue);
            }
            return alValues;
        }
        return new ArrayList<String>();
    }

    /**
     * Transform or cast an object to a String. Object can be String, ArrayList<String> or StringList<String> else method will an empty String. If multiple values, retrieves the first value.
     * @param oValue
     * @return a String
     */
    public static String getSingleValue(Object oValue) {
        if (oValue instanceof String) {
            return (String) oValue;
        } else if (oValue instanceof ArrayList) {
            ArrayList<?> alValues = (ArrayList<?>) oValue;
            if (alValues.size() > 0) {
                Object oSingleValue = alValues.get(0);
                if (oSingleValue instanceof String) {
                    return (String) oSingleValue;
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } else if (oValue instanceof StringList) {
            Iterator<?> ite = ((StringList) oValue).iterator();
            if (ite.hasNext()) {
                Object oSingleValue = ite.next();
                if (oSingleValue instanceof String) {
                    return (String) oSingleValue;
                }
            }
            return null;
        }
        return null;
    }
}
