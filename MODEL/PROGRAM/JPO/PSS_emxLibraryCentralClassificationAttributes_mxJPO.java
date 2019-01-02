import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.UOMUtil;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.apps.framework.ui.UICache;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;
import pss.constants.TigerConstants;

public class PSS_emxLibraryCentralClassificationAttributes_mxJPO extends emxLibraryCentralClassificationAttributesBase_mxJPO {

    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PSS_emxLibraryCentralClassificationAttributes_mxJPO.class);

    public PSS_emxLibraryCentralClassificationAttributes_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
    }

    /***
     * This method is used to display all the Classification Attributes during create Generic Document or Part. If the Class/PF contains Attribute Group all the attributes belonging each Attribute
     * Group is retrieved using getClassClassificationAttributes method. Once the list is obtained settinsMap and filedMap is constructed using getDynamicFieldsDuringCreate Function Function_033728
     * @param context
     * @param args
     * @return MapList
     * @since R215
     * @throws Exception
     */
    public MapList getClassificationAttributesForForm(Context context, String[] args) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap requestMap = (HashMap) programMap.get("requestMap");
        String endItemObjectId = (String) requestMap.get("objectId");
        String formName = (String) requestMap.get("form");
        MapList fieldMapList = new MapList();

        MapList classificationAttributesList = new MapList();
        DomainObject endItemObj = new DomainObject(endItemObjectId);

        StringList objectSelects = new StringList();
        objectSelects.add(SELECT_ID);
        objectSelects.add(SELECT_NAME);

        MapList classificationList = endItemObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_CLASSIFIEDITEM, "*", objectSelects, null, true, // boolean getTo,
                false, // boolean getFrom,
                (short) 0, null, null, 0);
        // MapList classificationAttributes = getClassClassificationAttributes(context, classObjectId);
        // fieldMapList=getDynamicFieldsDuringCreate(context,classificationAttributes,formName,sLanguage);

        int noOfClasses = classificationList.size();

        if (noOfClasses > 0) {
            Iterator itr = classificationList.iterator();
            while (itr.hasNext()) {
                Map classMap = (Map) itr.next();

                MapList classificationAttributes = getClassClassificationAttributes(context, (String) classMap.get(SELECT_ID));
                if (classificationAttributes.size() > 0) {
                    HashMap classificationAttributesMap = new HashMap();
                    classificationAttributesMap.put("className", classMap.get(SELECT_NAME));
                    classificationAttributesMap.put("attributes", classificationAttributes);

                    classificationAttributesList.add(classificationAttributesMap);
                }
            }

            fieldMapList = getDynamicFieldsMapList(context, classificationAttributesList, formName, false);

        }

        return fieldMapList;
    }

    /***
     * This method create the settingsMap and fieldMap to display all the Classification Attributes. The list of Attributes are looped through and check is performed whether the attributes is of type
     * Integer/String/Real/Date/Boolean, the fieldMap is set with the appropriate settings for each of the attribute type.
     * @param context
     * @param classificationAttributesList
     * @param formMode
     * @param sLanguage
     * @since R215
     * @return MapList containing the settingMap
     * @throws Exception
     */
    private MapList getDynamicFieldsMapList(Context context, MapList classificationAttributesList, String formName, boolean isCreate) throws Exception {

        // Define a new MapList to return.
        MapList fieldMapList = new MapList();
        String strLanguage = context.getSession().getLanguage();

        // attributeAttributeGroupMap contains all the attribute group names to which each attribute belongs
        HashMap attributeAttributeGroupMap = new HashMap();

        if (classificationAttributesList == null)
            return fieldMapList;

        String strParentAttrKey = "emxEngineeringCentral.PSS_DependentAttributes.ParentAttributes";
        String strParentAttr = EnoviaResourceBundle.getProperty(context, strParentAttrKey);
        StringList slParentRegAttr = FrameworkUtil.split(strParentAttr, ",");
        StringList slParentAttr = new StringList();
        HashMap<String, String> mapChildParentAttrMapping = new HashMap<String, String>();
        for (int i = 0; i < slParentRegAttr.size(); i++) {
            String strParentAttribteRegName = (String) slParentRegAttr.get(i);
            String strKeytoGetChildAttrKey = "emxEngineeringCentral.PSS_DependentAttributes.ChildAttribute." + strParentAttribteRegName;
            String strChildAttrRegName = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentral", context.getLocale(), strKeytoGetChildAttrKey);

            String strParentAttributeName = PropertyUtil.getSchemaProperty(context, strParentAttribteRegName);
            String strChildAttributeName = PropertyUtil.getSchemaProperty(context, strChildAttrRegName);

            slParentAttr.addElement(strParentAttributeName);
            mapChildParentAttrMapping.put(strChildAttributeName, strParentAttributeName);

        }

        Iterator classItr = classificationAttributesList.iterator();
        while (classItr.hasNext()) {

            Map classificationAttributesMap = (Map) classItr.next();

            MapList classificationAttributes = (MapList) classificationAttributesMap.get("attributes");
            String className = (String) classificationAttributesMap.get("className");
            if (classificationAttributes.size() > 0) {
                HashMap settingsMapForClassHeader = new HashMap();
                HashMap fieldMapForClassHeader = new HashMap();
                settingsMapForClassHeader.put(SETTING_FIELD_TYPE, "Section Header");
                settingsMapForClassHeader.put(SETTING_REGISTERED_SUITE, "LibraryCentral");
                settingsMapForClassHeader.put("Section Level", "1");
                fieldMapForClassHeader.put(LABEL, className);
                fieldMapForClassHeader.put("settings", settingsMapForClassHeader);
                fieldMapList.add(fieldMapForClassHeader);
            }
            for (int i = 0; i < classificationAttributes.size(); i++) {
                HashMap attributeGroup = (HashMap) classificationAttributes.get(i);
                String attributeGroupName = (String) attributeGroup.get("name");
                MapList attributes = (MapList) attributeGroup.get("attributes");
                HashMap settingsMapForHeader = new HashMap();
                HashMap fieldMapForHeader = new HashMap();
                settingsMapForHeader.put(SETTING_FIELD_TYPE, "Section Header");
                settingsMapForHeader.put(SETTING_REGISTERED_SUITE, "LibraryCentral");
                settingsMapForHeader.put("Section Level", "2");
                fieldMapForHeader.put(LABEL, attributeGroupName);
                fieldMapForHeader.put("settings", settingsMapForHeader);
                fieldMapList.add(fieldMapForHeader);
                for (int j = 0; j < attributes.size(); j++) {
                    HashMap attribute = (HashMap) attributes.get(j);
                    String attributeQualifiedName = (String) attribute.get("qualifiedName");
                    String attributeName = (String) attribute.get("name");
                    HashMap fieldMap = new HashMap();
                    HashMap settingsMap = new HashMap();
                    fieldMap.put(NAME, attributeGroupName + "|" + attributeQualifiedName);
                    fieldMap.put(LABEL, i18nNow.getAttributeI18NString(attributeName, strLanguage));
                    fieldMap.put(EXPRESSION_BUSINESSOBJECT, "attribute[" + attributeQualifiedName + "].value");
                    String attributeType = (String) attribute.get("type");
                    if (attributeType.equals(FORMAT_TIMESTAMP)) {
                        settingsMap.put(SETTING_FORMAT, FORMAT_DATE);
                        settingsMap.put(SETTING_CLEAR, "true");
                    } else if (attributeType.equals(FORMAT_BOOLEAN)) {
                        settingsMap.put(SETTING_INPUT_TYPE, INPUT_TYPE_COMBOBOX);
                        settingsMap.put(SETTING_REMOVE_RANGE_BLANK, "true");
                        MapList range = (MapList) attribute.get("range");

                        if (range == null) {
                            settingsMap.put(SETTING_RANGE_PROGRAM, "emxLibraryCentralClassificationAttributes");
                            settingsMap.put(SETTING_RANGE_FUNCTION, "getRangeValuesForBooleanAttributes");

                        }
                    } else if (attributeType.equals(FORMAT_INTEGER)) {
                        settingsMap.put(SETTING_FORMAT, FORMAT_INTEGER);
                        if (UOMUtil.isAssociatedWithDimension(context, attributeQualifiedName)) {
                            addUOMDetailsToSettingsMap(context, attributeQualifiedName, fieldMap, settingsMap);
                        }
                        if (formName.equals("type_CreatePart"))
                            settingsMap.put(SETTING_VALIDATE, "isValidInteger");
                        // IR-195858V6R2014 checking whether attribute has range,if so
                        // setting the input type to combobox
                        if ((MapList) attribute.get("range") != null) {
                            settingsMap.put(SETTING_INPUT_TYPE, INPUT_TYPE_COMBOBOX);
                            settingsMap.put(SETTING_REMOVE_RANGE_BLANK, "true");
                        }
                    } else if (attributeType.equals(FORMAT_REAL)) {
                        settingsMap.put(SETTING_FORMAT, FORMAT_NUMERIC);
                        if (UOMUtil.isAssociatedWithDimension(context, attributeQualifiedName)) {
                            addUOMDetailsToSettingsMap(context, attributeQualifiedName, fieldMap, settingsMap);
                        }
                        if (formName.equals("type_CreatePart"))
                            settingsMap.put(SETTING_VALIDATE, "checkPositiveReal");
                        // IR-195858V6R2014 checking whether attribute has range,if so
                        // setting the input type to combobox

                        if ((MapList) attribute.get("range") != null) {
                            settingsMap.put(SETTING_INPUT_TYPE, INPUT_TYPE_COMBOBOX);
                            settingsMap.put(SETTING_REMOVE_RANGE_BLANK, "true");
                        }
                    } else if (attributeType.equals(FORMAT_STRING)) {
                        MapList range = (MapList) attribute.get("range");
                        String isMultiline = (String) attribute.get("multiline");
                        if (range != null && range.size() > 0) {
                            // IR-227384V6R2014 due to FIELD_CHOICES & FIELD_DISPLAY_CHOICES
                            // setting ArryIndexoutofBounds exception is thrown,if the
                            // attribute having a range and if one of the values
                            // contains !, since BPS is removing this entry, there is a mismatch
                            // in the length of display values & actual values.
                            // hence settings has been removed. BPS will handle range values
                            settingsMap.put(SETTING_INPUT_TYPE, INPUT_TYPE_COMBOBOX);
                            settingsMap.put(SETTING_FORMAT, FORMAT_STRING);
                            settingsMap.put(SETTING_REMOVE_RANGE_BLANK, "true");
                            settingsMap.put(SETTING_REGISTERED_SUITE, "Framework");
                            String adminName = UICache.getSymbolicName(context, attributeName, "attribute");
                            settingsMap.put("Admin Type", adminName);
                        } else if (BOOLEAN_TRUE.equalsIgnoreCase(isMultiline)) {
                            settingsMap.put(SETTING_INPUT_TYPE, INPUT_TYPE_TEXTAREA);
                        } else {
                            settingsMap.put(SETTING_INPUT_TYPE, INPUT_TYPE_TEXTBOX);
                        }
                    } else {

                    }

                    settingsMap.put(SETTING_FIELD_TYPE, FIELD_TYPE_ATTRIBUTE);
                    if (isCreate) {
                        settingsMap.put(SETTING_UPDATE_PROGRAM, "emxLibraryCentralCommon");
                        settingsMap.put(SETTING_UPDATE_FUNCTION, "dummyUpdateFunction");
                    }

                    // On Change Handler
                    // settingsMap.put("OnChange Handler", "reloadDuplicateAttributesInForm");

                    if (slParentAttr.contains(attributeName)) {
                        // On change handler is not working with the field name when it has space in it, so replaing it with "_"
                        fieldMap.put(NAME, attributeGroupName.replaceAll(" ", "_") + "|" + attributeQualifiedName);
                        settingsMap.put("OnChange Handler", "updateAttrValsDependsOnParentAttrVal");
                    }

                    if (mapChildParentAttrMapping.containsKey(attributeName)) {
                        settingsMap.put(SETTING_RANGE_PROGRAM, "PSS_emxLibraryCentralClassificationAttributes");
                        settingsMap.put(SETTING_RANGE_FUNCTION, "getRangeValuesForChildAttributes");
                        settingsMap.put("PSS_ParentAttributeName", mapChildParentAttrMapping.get(attributeName));
                    }

                    fieldMap.put("settings", settingsMap);
                    fieldMapList.add(fieldMap);
                    String attributeGroupNames = (String) attributeAttributeGroupMap.get(attributeQualifiedName);
                    if (attributeGroupNames == null) {
                        attributeAttributeGroupMap.put(attributeQualifiedName, attributeGroupName);
                    } else {
                        attributeAttributeGroupMap.put(attributeQualifiedName, attributeGroupNames + "|" + attributeGroupName);
                    }
                }
            }

        }

        return fieldMapList;
    }

    /***
     * This method adds all the UOM details required to display Classification Attribute during create Generic Document/Part. To display UOM details settingsMap should contain Field Type=Attribute,
     * otherwise the UI would display only textbox next to the UOM Field.Once the map contains FieldType=Attribute, BPS code assumes that this Attribute is defined on the Type, but in case of
     * Classification Attributes it's not, Hence to overcome this bug a Dummy update program & function is used here, If a update program & Function is defined BPS wouldn't check whether the attribute
     * is defined on the type.
     * @param context
     * @param attributeName
     * @param fieldMap
     * @param settingsMap
     * @since R215
     * @throws FrameworkException
     */
    private void addUOMDetailsToSettingsMap(Context context, String attributeName, HashMap fieldMap, HashMap settingsMap) throws FrameworkException {
        fieldMap.put(UOM_ASSOCIATEDWITHUOM, BOOLEAN_TRUE);
        fieldMap.put(DB_UNIT, UOMUtil.getSystemunit(context, null, attributeName, null));
        fieldMap.put(UOM_UNIT_LIST, UOMUtil.getDimensionUnits(context, attributeName));
        settingsMap.put(SETTING_EDITABLE_FIELD, BOOLEAN_TRUE);
        settingsMap.put(SETTING_INPUT_TYPE, INPUT_TYPE_TEXTBOX);
    }

    /**
     * Range Function for attribute PSS_TechnologyType Assign Ranges to the child according to parent's type
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Map getRangeValuesForChildAttributes(Context context, String args[]) throws Exception {
        Map mRangeValues = new HashMap();
        try {
            Map programMap = (Map) JPO.unpackArgs(args);

            Map fieldMap = (Map) programMap.get("fieldMap");
            Map requestMap = (Map) programMap.get("requestMap");
            Map mSettings = (Map) fieldMap.get("settings");
            String strChildRegName = (String) mSettings.get("Admin Type");
            String strChildName = PropertyUtil.getSchemaProperty(context, strChildRegName);
            String strParentAttr = (String) mSettings.get("PSS_ParentAttributeName");
            String objectId = (String) requestMap.get("objectId");
            DomainObject domainObj = DomainObject.newInstance(context, objectId);
            Map attributeMap = domainObj.getAttributeMap(context, true);
            if (attributeMap.containsKey(strParentAttr)) {
                String strParentRegAttr = UICache.getSymbolicName(context, strParentAttr, "attribute");
                String strParentName = PropertyUtil.getSchemaProperty(context, strParentRegAttr);
                String sttrParentAttrValue = domainObj.getAttributeValue(context, strParentName);
                String strKeyForRanges = "emxEngineeringCentral.PSS_DependentAttributes." + strParentRegAttr + "." + strChildRegName + "." + sttrParentAttrValue.replace(" ", "_") + ".ValidRanges";
                String strValidAttrRanges = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentral", context.getLocale(), strKeyForRanges);
                StringList slValidRanges = FrameworkUtil.split(strValidAttrRanges, ",");
                mRangeValues.put("field_choices", slValidRanges);
                mRangeValues.put("field_display_choices", slValidRanges);
            } else {
                StringList slValidRanges = FrameworkUtil.getRanges(context, strChildName);
                mRangeValues.put("field_choices", slValidRanges);
                mRangeValues.put("field_display_choices", slValidRanges);
            }

        } catch (Exception ex) {
            logger.error("Error in PSS_emxLibraryCentralClassificationAttributes : getRangeValuesForChildAttributes: ", ex);
        }
        return mRangeValues;
    }

}