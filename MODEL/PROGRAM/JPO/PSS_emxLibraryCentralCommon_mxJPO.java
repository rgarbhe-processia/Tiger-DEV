
/*
 * PSS_emxLibraryCentralCommon.java
 *
 * Copyright (c) 1992-2015 Dassault Systemes.
 *
 * All Rights Reserved. This program contains proprietary and trade secret information of MatrixOne, Inc. Copyright notice is precautionary only and does not evidence any actual or intended
 * publication of such program.
 *
 * static const RCSID [] = "$Id: ${CLASSNAME}.java.rca 1.8 Wed Oct 22 16:02:38 2008 przemek Experimental przemek $";
 */
import matrix.db.Context;
import matrix.db.JPO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import matrix.util.StringList;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.framework.ui.UIUtil;
import com.matrixone.apps.library.Libraries;

/**
 * @version AEF Rossini - Copyright (c) 2002, MatrixOne, Inc.
 */
public class PSS_emxLibraryCentralCommon_mxJPO extends emxLibraryCentralCommonBase_mxJPO {

    /**
     * Constructor.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            the Java <code>String[]</code> object
     * @throws Exception
     *             if the operation fails
     * @since AEF 10.6.0.1
     */

    public PSS_emxLibraryCentralCommon_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
    }

    // Addition for Tiger - PCM stream by Processia starts
    public HashMap getRangeValuesForAttribute(Context context, String[] args) throws Exception {
        HashMap rangeMap = new HashMap();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap fieldMap = (HashMap) programMap.get("fieldMap");
            HashMap settingsMap = (HashMap) fieldMap.get("settings");
            String strAttributeName = (String) settingsMap.get("attribute_name");

            matrix.db.AttributeType attribName = new matrix.db.AttributeType(strAttributeName);
            attribName.open(context);

            // actual range values
            List attributeRange = attribName.getChoices();

            // initialize the Stringlists fieldRangeValues, fieldDisplayRangeValues
            StringList fieldRangeValues = new StringList();
            StringList fieldDisplayRangeValues = new StringList();

            fieldRangeValues.addElement("");
            fieldDisplayRangeValues.addElement("");

            List attributeDisplayRange = i18nNow.getAttrRangeI18NStringList(strAttributeName, (StringList) attributeRange, context.getSession().getLanguage());
            for (int i = 0; i < attributeRange.size(); i++) {
                fieldRangeValues.addElement((String) attributeRange.get(i));
                fieldDisplayRangeValues.addElement((String) attributeDisplayRange.get(i));
            }

            attribName.close(context);
            rangeMap.put("field_choices", fieldRangeValues);
            rangeMap.put("field_display_choices", fieldDisplayRangeValues);
        } catch (Exception ex) {
            throw ex;
        }
        return rangeMap;
    }

    // Addition for Tiger - PCM stream by Processia ends

    /**
     * Access Function for GeneralClassType field.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds the following input arguments 0 - progarmMap
     * @return Map contains created objectId
     * @throws Exception
     */
    public static Boolean isGeneralClassType(Context context, String[] args) throws Exception {
        boolean BAccessToField = false;
        String TYPE_GENERAL_CLASS = PropertyUtil.getSchemaProperty(context, "type_GeneralClass");
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String strTypeName = (String) programMap.get("type");
        if (strTypeName.startsWith("_selectedType:")) {
            StringList lstTypeList = FrameworkUtil.split(strTypeName, ",");
            String strSelectedType = (String) lstTypeList.get(0);
            // Find Bug Issue Dead Local Store : Priyanka Salunke : 28-Feb-2017
            StringList lstSelectedClassList = FrameworkUtil.split(strSelectedType, ":");
            strTypeName = (String) lstSelectedClassList.get(1);
        }

        else {
            StringList lstTypeListActual = FrameworkUtil.split(strTypeName, ",");
            String strSelectedTypeActual = (String) lstTypeListActual.get(0);
            strTypeName = PropertyUtil.getSchemaProperty(context, strSelectedTypeActual);
        }

        if (strTypeName.equalsIgnoreCase(TYPE_GENERAL_CLASS)) {
            BAccessToField = true;
        }
        return BAccessToField;
    }

    /**
     * To create a Library Central object like Library, Class etc..
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds the following input arguments 0 - requestMap
     * @return Map contains created objectId
     * @throws Exception
     */
    @com.matrixone.apps.framework.ui.CreateProcessCallable
    public Map createLBCObject(Context context, String[] args) throws Exception {

        final String TYPE_GENERAL_CLASS = PropertyUtil.getSchemaProperty(context, "type_GeneralClass");
        final String INTERFACE_PSS_GENERALCLASS = PropertyUtil.getSchemaProperty(context, "interface_PSS_GeneralClass");
        Map returnMap = new HashMap();

        HashMap requestMap = (HashMap) JPO.unpackArgs(args);
        // Added for RFC-094(CAD-BOM):Start
        String strTypeName = (String) requestMap.get("type");
        if (UIUtil.isNotNullAndNotEmpty(strTypeName)) {
            StringList lstTypeListActual = FrameworkUtil.split(strTypeName, ",");
            String strSelectedTypeActual = (String) lstTypeListActual.get(0);
            strTypeName = PropertyUtil.getSchemaProperty(context, strSelectedTypeActual);
        }
        // Added for RFC-094(CAD-BOM):End

        try {
            Libraries lib = new Libraries();
            String objectId = lib.createLBCObject(context, requestMap);
            // Added for RFC-094(CAD-BOM):Start
            if (strTypeName.equalsIgnoreCase(TYPE_GENERAL_CLASS)) {
                MqlUtil.mqlCommand(context, "modify bus $1 add interface $2;", objectId, INTERFACE_PSS_GENERALCLASS);
            }
            // Added for RFC-094(CAD-BOM):End
            returnMap.put("id", objectId);

        } catch (Exception e) {
            throw new FrameworkException(e);
        }

        return returnMap;
    }

}