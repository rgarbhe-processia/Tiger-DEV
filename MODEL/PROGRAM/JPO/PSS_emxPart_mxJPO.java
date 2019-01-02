
/*
 ** ${CLASS:PSS_emxPart} Cloned from emxPart JPO
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeConstants;
import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeOrder;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.IEFEBOMConfigObject;
import com.matrixone.MCADIntegration.server.beans.MCADIntegrationSessionData;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.apps.common.CommonDocument;
import com.matrixone.apps.common.Document;
import com.matrixone.apps.common.Person;
import com.matrixone.apps.common.util.ComponentsUtil;
import com.matrixone.apps.common.util.ImageConversionUtil;
import com.matrixone.apps.configuration.ConfigurationConstants;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkProperties;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.XSSUtil;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.apps.domain.util.mxType;
import com.matrixone.apps.effectivity.EffectivityFramework;
import com.matrixone.apps.engineering.EBOMMarkup;
import com.matrixone.apps.engineering.EngineeringConstants;
import com.matrixone.apps.engineering.EngineeringUtil;
import com.matrixone.apps.engineering.IPartMaster;
import com.matrixone.apps.engineering.Part;
import com.matrixone.apps.engineering.PartFamily;
import com.matrixone.apps.framework.ui.UIComponent;
import com.matrixone.apps.framework.ui.UIMenu;
import com.matrixone.apps.framework.ui.UINavigatorUtil;
import com.matrixone.apps.framework.ui.UITableIndented;
import com.matrixone.apps.framework.ui.UIUtil;
import com.matrixone.apps.productline.ProductLineConstants;
import com.matrixone.fcs.common.Logger;
import com.matrixone.fcs.common.Resources;
import com.matrixone.fcs.http.HttpVcCheckout;
import com.matrixone.fcs.mcs.Checkin;
import com.matrixone.fcs.mcs.Checkout;
import com.matrixone.fcs.mcs.McsBase;
import com.matrixone.fcs.mcs.PreCheckin;
import com.matrixone.jdom.Element;
import com.matrixone.servlet.Framework;

import matrix.db.Access;
import matrix.db.BusinessObject;
import matrix.db.BusinessObjectList;
import matrix.db.BusinessObjectProxy;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessType;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.JPOSupport;
import matrix.db.Policy;
import matrix.db.PolicyItr;
import matrix.db.PolicyList;
import matrix.db.Query;
import matrix.db.QueryIterator;
import matrix.db.Relationship;
import matrix.db.TicketWrapper;
import matrix.util.Pattern;
import matrix.util.SelectList;
import matrix.util.StringItr;
import matrix.util.StringList;
import matrix.util.StringUtils;
import pss.common.utils.TigerUtils;
import pss.constants.TigerConstants;

/**
 * The <code>PSS_emxPart</code> class contains code for the "Part" business type.
 */
@SuppressWarnings("serial")
public class PSS_emxPart_mxJPO extends emxPartBase_mxJPO {
    // TIGTK-5406 - 03-04-2017 - VP - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PSS_emxPart_mxJPO.class);

    private static final String MARKUP_ADD = "add";

    private static final String MARKUP_NEW = "new";

    private static final String MARKUP_CUT = "cut";

    private static final String SELECT_FROM_DERIVED_IDS = "from[" + RELATIONSHIP_DERIVED + "]." + DomainConstants.SELECT_TO_ID;

    // TIGTK-5406 - 03-04-2017 - VP - END
    StringBuffer sbInvalidObj = new StringBuffer(EMPTY_STRING);

    int Count = 0;

    /**
     * Constructor.
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param args
     *            holds no arguments.
     * @throws Exception
     *             if the operation fails.
     */

    public PSS_emxPart_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
    }

    // Addition for Tiger - CAD BOM stream by SGS starts

    /**
     * Method to get the connected Symmetrical Parts to Original Part.
     * @param context
     * @param args
     * @return MapList - Contains the Object Id of Symmetrical Part , RelationShipId connecting the two parts
     * @throws Exception
     */
    public MapList getSymmetricalParts(Context context, String[] args) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String strobjectId = (String) programMap.get("objectId");
        StringList selectStmts = new StringList(1);
        selectStmts.addElement(DomainConstants.SELECT_ID);
        StringList relStmts = new StringList(1);
        relStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

        DomainObject domOriginalPart = DomainObject.newInstance(context, strobjectId);
        MapList mlobjMapList = domOriginalPart.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART, // relationship pattern
                DomainConstants.TYPE_PART, // object pattern
                selectStmts, // object selects
                relStmts, // relationship selects
                true, // to direction
                true, // from direction
                (short) 1, // recursion level
                null, // object where clause
                null); // relationship where clause
        return mlobjMapList;
    }

    /**
     * Method is created for column 'O/S' to show Part is Original Part or Symmetric Part.
     * @param context
     * @param args
     * @return Vector - Contains the Symmetric Part Status.
     * @throws Exception
     */
    public Vector getOriginalSymmetricInfo(Context context, String[] args) throws Exception {
        try {

            Vector returnVector = new Vector();

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");

            Iterator itrobjectList = objectList.iterator();

            while (itrobjectList.hasNext()) {
                Map mapCurrentObject = (Map) itrobjectList.next();
                String strCurrentPartId = (String) mapCurrentObject.get(DomainConstants.SELECT_ID);

                DomainObject domCurrentPart = DomainObject.newInstance(context, strCurrentPartId);
                String strSymmetricalPart = domCurrentPart.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART + "]");

                if ("TRUE".equalsIgnoreCase(strSymmetricalPart)) {
                    returnVector.add(TigerConstants.SYMMETRIC_STATUS_ORIGINAL);
                } else {

                    strSymmetricalPart = domCurrentPart.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART + "]");

                    if ("TRUE".equalsIgnoreCase(strSymmetricalPart)) {
                        returnVector.add(TigerConstants.SYMMETRIC_STATUS_SYMMETRICAL);
                    }

                    else {
                        returnVector.add(DomainConstants.EMPTY_STRING);
                    }
                }

            }

            return returnVector;

        } catch (Exception ex) {
            // TIGTK-5405 - 03-04-2017 - VP - START
            logger.error("Error in getOriginalSymmetricInfo: ", ex);
            // TIGTK-5405 - 03-04-2017 - VP - END
            throw ex;
        }

    }

    /**
     * Method to display checkbox to show the check box field in webform Name of the checkbox field will be the name of the webform field. If PSS_Checked setting for the webform field is TRUE, then
     * the checkbox will checked by default. Otherwise, it will be unchecked by defaul.
     * @param context
     * @param args
     * @return String - HTML output string to show the checkbox field
     * @throws Exception
     */
    public String displayCheckBoxForWebform(Context context, String[] args) throws Exception {
        Map programMap = (Map) JPO.unpackArgs(args);
        Map fieldMap = (Map) programMap.get("fieldMap");

        String strFieldName = (String) fieldMap.get(DomainConstants.SELECT_NAME);

        Map mapSettings = (Map) fieldMap.get("settings");

        String strIsChecked = (String) mapSettings.get("PSS_Checked");// PSS_Checked

        if ("TRUE".equalsIgnoreCase(strIsChecked)) {
            return "<input type=\"checkbox\" checked=\"true\" name=\"" + strFieldName + "\"/> ";
        } else {
            return "<input type=\"checkbox\" name=\"" + strFieldName + "\"/> ";
        }
    }

    /**
     * Method to display radio button to Assembly Link to same components
     * @param context
     * @param args
     * @return String --- HTML output string to show the Radio Button Object for 'Assembly Link to Same Components'
     * @throws Exception
     */
    public String displayAssemblyLinkToSameComponentsRadiobutton(Context context, String[] args) throws Exception {
        return "<input type=\"radio\" value=\"AssemblyLinkToSameComponents\" checked=\"true\" name=\"radioForSameAndSymmetricComponents\"/> ";
    }

    /**
     * Method to display radio button to Assembly Link To Symmetric Components
     * @param context
     * @param args
     * @return String --- HTML output string to show the Radio Button Object for 'Assembly Link to Symmetric Components'
     * @throws Exception
     */
    public String displayAssemblyLinkToSymmetricComponentsRadiobutton(Context context, String[] args) throws Exception {
        return "<input type=\"radio\" value=\"AssemblyLinkToSymmetricComponents\" name=\"radioForSameAndSymmetricComponents\"/> ";
    }

    /**
     * Method to get Range Values for the Attribute.
     * @param context
     * @param args
     * @return HashMap --- Contains the Attribute Value Range
     * @throws Exception
     */

    public HashMap getRangeValuesForAttribute(Context context, String[] args) throws Exception {

        HashMap mapRange = new HashMap();
        try {

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap fieldMap = (HashMap) programMap.get("fieldMap");
            HashMap settingsMap = (HashMap) fieldMap.get("settings");

            String strSymbolicAttributeName = (String) settingsMap.get("Admin Type");
            String strAttributeName = (String) PropertyUtil.getSchemaProperty(context, strSymbolicAttributeName);
            matrix.db.AttributeType attribName = new matrix.db.AttributeType(strAttributeName);
            attribName.open(context);

            // actual range values
            List attributeRange = attribName.getChoices();

            // initialize the Stringlists fieldRangeValues,
            // fieldDisplayRangeValues
            StringList fieldRangeValues = new StringList();
            StringList fieldDisplayRangeValues = new StringList();

            // Process information to obtain the range values and add them to
            // fieldRangeValues
            // Get the internationlized value of the range values and add them
            // to fieldDisplayRangeValues
            List attributeDisplayRange = i18nNow.getAttrRangeI18NStringList(strAttributeName, (StringList) attributeRange, context.getSession().getLanguage());
            for (int i = 0; i < attributeRange.size(); i++) {
                fieldRangeValues.addElement((String) attributeRange.get(i));
                fieldDisplayRangeValues.addElement((String) attributeDisplayRange.get(i));
            }

            attribName.close(context);
            mapRange.put("field_choices", fieldRangeValues);
            mapRange.put("field_display_choices", fieldDisplayRangeValues);
        } catch (Exception ex) {
            // TIGTK-5405 - 03-04-2017 - VP - START
            logger.error("Error in getRangeValuesForAttribute: ", ex);
            // TIGTK-5405 - 03-04-2017 - VP - END
        }
        return mapRange;
    }

    /**
     * Method called on the post process functionality of Symmetrical Parts depending on mode as "Create Symmetrical" or "Add Existing". Method makes a check whether to copy CAD Object from Original
     * Part to Symmetrical Part. Method makes a check whether to copy EBOM from Original Part to Symmetrical Part. Method makes a check whether the Original Part should be on TO/FROM Side of
     * 'Symmetrical Part' relationship.
     * @param context
     * @param args
     * @return HashMap
     * @throws Exception
     */

    @com.matrixone.apps.framework.ui.PostProcessCallable
    public HashMap postProcessForSymmetricalParts(Context context, String[] args) throws Exception {
        HashMap returnMap = new HashMap();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap requestMap = (HashMap) programMap.get("requestMap");
            HashMap paramMap = (HashMap) programMap.get("paramMap");

            String strCurrentPartOriginal = (String) requestMap.get("CurrentPartOriginal");
            String strMainObjectId = (String) requestMap.get("copyObjectId");
            String strSymmetricalPartId = (String) paramMap.get("newObjectId");

            if (UIUtil.isNullOrEmpty(strSymmetricalPartId)) {
                StringTokenizer tokens = new StringTokenizer((String) requestMap.get("emxTableRowId"), "|");
                strSymmetricalPartId = tokens.nextToken();
                strMainObjectId = tokens.nextToken();
            }

            DomainObject domMainObject = DomainObject.newInstance(context, strMainObjectId);
            DomainObject domSymmetricalPartObject = DomainObject.newInstance(context, strSymmetricalPartId);

            String strMode = (String) requestMap.get("PSS_mode");

            // Apply for connecting new object and parent object with
            // PSS_HasSymmetricalPart relationship on the base of 'Current Part
            // as Original' selection.

            DomainRelationship domRel;
            if ("on".equals(strCurrentPartOriginal)) {
                domRel = DomainRelationship.connect(context, domMainObject, TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART, domSymmetricalPartObject);
            } else {
                domRel = DomainRelationship.connect(context, domSymmetricalPartObject, TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART, domMainObject);
            }

            if ("PSS_AddExistingSymmetricalParts".equals(strMode)) {
                // If user has selected 'Same CAD Model' option, then execute
                // this block.
                String strcheckboxSameCADModel = (String) requestMap.get("Same_CAD_Model");

                if ("on".equalsIgnoreCase(strcheckboxSameCADModel)) {
                    copyCADDocumentToSymmetricalPart(context, strMainObjectId, strSymmetricalPartId, DomainConstants.TYPE_CAD_MODEL);
                }

                // Copy EBOM of Original part to symmetrical part
                String strSameOrSymmetricComponents = (String) requestMap.get("radioForSameAndSymmetricComponents");

                copyEBOMPartsForAdExistingSymmetrical(context, strMainObjectId, strSymmetricalPartId, strSameOrSymmetricComponents);
            } else {
                // Apply for set value in Same Mass & In pairs Attibutes.
                String strSameMassOption = (String) requestMap.get("Same Mass");
                String strInPairsOption = (String) requestMap.get("In Pairs");
                HashMap mapNewValuesAttribute = new HashMap();

                if ("Yes".equalsIgnoreCase(strSameMassOption)) {
                    mapNewValuesAttribute.put(TigerConstants.ATTRIBUTE_PSS_SYMMETRICALPARTSIDENTICALMASS, strSameMassOption);
                }
                if ("Yes".equalsIgnoreCase(strInPairsOption)) {
                    mapNewValuesAttribute.put(TigerConstants.ATTRIBUTE_PSS_SYMMETRICALPARTSMANAGEINPAIRS, strInPairsOption);
                }
                if (mapNewValuesAttribute.size() > 0)
                    DomainRelationship.setAttributeValues(context, domRel.toString(), mapNewValuesAttribute);

                // If AssemblyLinkToSameComponents then not required to connect
                // Symmetrical Part with EBOM Part because the EBOM
                // relationships as Clone flag on from side on the relationship
                // is
                // "Replicate"

                String strSameOrSymmetricComponents = (String) requestMap.get("radioForSameAndSymmetricComponents");

                if ("AssemblyLinkToSymmetricComponents".equalsIgnoreCase(strSameOrSymmetricComponents)) {
                    copyEBOMPartsForCreateSymmetrical(context, strMainObjectId, strSymmetricalPartId);
                }

                // If user has selected 'Same CAD Drawing' option, then execute
                // this block.
                String strcheckboxSameCADDrawing = (String) requestMap.get("CAD_Drawing");
                if ("on".equalsIgnoreCase(strcheckboxSameCADDrawing)) {
                    copyCADDocumentToSymmetricalPart(context, strMainObjectId, strSymmetricalPartId, DomainConstants.TYPE_CAD_DRAWING);
                }

            }

        } catch (Exception ex) {
            // TIGTK-5405 - 03-04-2017 - VP - START
            logger.error("Error in postProcessForSymmetricalParts: ", ex);
            // TIGTK-5405 - 03-04-2017 - VP - END
            throw ex;
        }
        return returnMap;
    }

    /**
     * Method to copy the EBOM of Original Part on Symmetric Part in 'Create Symmetrical Part' mode.
     * @param context
     * @param args0
     *            Parent Object Id/Original Part Id
     * @param args1
     *            New Object Id/Symmetrical Part Id
     * @return void
     * @throws Exception
     */
    public void copyEBOMPartsForCreateSymmetrical(Context context, String strParentObjectId, String strNewObjectId) throws Exception {
        // DomainObject domParentObject = DomainObject.newInstance(context,
        // strParentObjectId);
        DomainObject domNewObject = DomainObject.newInstance(context, strNewObjectId);

        final String strSymmetricalPartExpression = "from[" + TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART + "].to.id";

        StringList lstselectStmts = new StringList(2);
        lstselectStmts.addElement(DomainConstants.SELECT_ID);
        lstselectStmts.addElement(strSymmetricalPartExpression);

        StringList lstrelStmts = new StringList();
        lstrelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

        MapList mlEBOM = domNewObject.getRelatedObjects(context, RELATIONSHIP_EBOM, DomainConstants.TYPE_PART, lstselectStmts, lstrelStmts, false, true, (short) 1, null, null, 0);

        for (int i = 0; i < mlEBOM.size(); i++) {

            Map<String, String> map = (Map<String, String>) mlEBOM.get(i);
            String strSymmetricpartId = map.get(strSymmetricalPartExpression);

            if (UIUtil.isNotNullAndNotEmpty(strSymmetricpartId)) {

                // Get Original Attributes from EBOM Part.
                String strOriginalEBOMRelId = map.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                DomainRelationship domOriginalEBOMRel = new DomainRelationship(strOriginalEBOMRelId);
                Map mapOriginalEBOMAttributes = domOriginalEBOMRel.getAttributeMap(context, true);

                DomainRelationship.disconnect(context, strOriginalEBOMRelId);

                // Connect Symmetrical Part with Parent EBOM's Symmetrical part
                // and set Original Attributes to that Part.
                DomainObject domEBOMSymmetricComponentObj = DomainObject.newInstance(context, strSymmetricpartId);
                DomainRelationship domSymmetricalEBOMRel = DomainRelationship.connect(context, domNewObject, RELATIONSHIP_EBOM, domEBOMSymmetricComponentObj);
                // Added for TIGTK-3723 by PKH(CAD-BOM):Start
                mapOriginalEBOMAttributes.remove(TigerConstants.ATTRIBUTE_ISVPLMVISIBLE);

                if (mapOriginalEBOMAttributes.containsKey(TigerConstants.ATTRIBUTE_EFFECTIVITYCOMPILEDFORM)) {
                    String strModRelMQL = "mod connection $1 add interface $2";
                    // MqlUtil.mqlCommand(context, strModRelMQL, false);

                    String args_temp[] = new String[2];
                    args_temp[0] = domSymmetricalEBOMRel.toString();
                    args_temp[1] = "Effectivity Framework";

                    MqlUtil.mqlCommand(context, strModRelMQL, args_temp);
                }

                // Added for TIGTK-3723 by PKH(CAD-BOM):End.
                domSymmetricalEBOMRel.setAttributeValues(context, mapOriginalEBOMAttributes);

            } // End of If
        }
    }

    /**
     * Method to copy the CAD Document associated to the Current Part with 'Part Specification' relationship based on the mode passed to the newly created part. 'CAD Drawing' gets copied as 'Part
     * Specification' if the mode is Create. 'CAD MOdel' gets copied as 'Part Specification' if the mode is Add Existing.
     * @param context
     * @param args0
     *            Parent Object Id/Original Part Id
     * @param args1
     *            New Object Id/Symmetrical Part Id
     * @param args2
     *            type as --- CAD Drawing / CAD Model
     * @return void
     * @throws Exception
     */
    public void copyCADDocumentToSymmetricalPart(Context context, String strParentObjectId, String strNewObjectId, String strType) throws Exception {

        DomainObject domParentObject = DomainObject.newInstance(context, strParentObjectId);
        DomainObject domNewObject = DomainObject.newInstance(context, strNewObjectId);

        StringList lstselectStmts = new StringList(1);
        lstselectStmts.addElement(DomainConstants.SELECT_ID);

        StringList lstrelStmts = new StringList();

        // Getting the Related "CAD Drawing" of Current Part object
        MapList mlCADObjectId = domParentObject.getRelatedObjects(context, RELATIONSHIP_PART_SPECIFICATION, strType, lstselectStmts, lstrelStmts, false, true, (short) 1, null, null);

        if (mlCADObjectId != null && !mlCADObjectId.isEmpty()) {
            Iterator iteratorCADObjectId = mlCADObjectId.iterator();

            while (iteratorCADObjectId.hasNext()) {

                Map<String, String> map = (Map<String, String>) iteratorCADObjectId.next();
                String strCADObjectId = (String) map.get(DomainConstants.SELECT_ID);

                DomainObject domCADObjectId = DomainObject.newInstance(context, strCADObjectId);
                DomainRelationship domCADObjectrel = DomainRelationship.connect(context, domNewObject, RELATIONSHIP_PART_SPECIFICATION, domCADObjectId); // Connect
                // with
                // Part
                // Specification
                // RelationShip

            }
        }
    }

    /**
     * Method to copy the EBOM of Original Part on Symmetric Part in Add Existing Mode.
     * @param context
     * @param args0
     *            Parent Object Id/Original Part Id
     * @param args1
     *            New Object Id/Symmetrical Part Id
     * @param args2
     *            Same or Symmetric Components Attribute Value
     * @return void
     * @throws Exception
     */
    public void copyEBOMPartsForAdExistingSymmetrical(Context context, String strParentObjectId, String strNewObjectId, String strSameOrSymmetricComponents) throws Exception {
        DomainObject domParentObject = DomainObject.newInstance(context, strParentObjectId);
        DomainObject domNewObject = DomainObject.newInstance(context, strNewObjectId);

        final String strSymmetricalPartExpression = "from[" + TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART + "].to.id";

        StringList lstselectStmts = new StringList(2);
        lstselectStmts.addElement(DomainConstants.SELECT_ID);

        if ("AssemblyLinkToSymmetricComponents".equalsIgnoreCase(strSameOrSymmetricComponents)) {
            lstselectStmts.addElement(strSymmetricalPartExpression);
        }

        StringList lstrelStmts = new StringList(1);
        lstrelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

        MapList mlEBOM = domParentObject.getRelatedObjects(context, RELATIONSHIP_EBOM, DomainConstants.TYPE_PART, lstselectStmts, lstrelStmts, false, true, (short) 1, null, null, 0);

        for (int i = 0; i < mlEBOM.size(); i++) {

            Map map = (Map) mlEBOM.get(i);
            String strEBOMChildPartId = (String) map.get(strSymmetricalPartExpression);

            // Get Original Attributes from EBOM Part.
            String strOriginalEBOMRelId = (String) map.get(DomainConstants.SELECT_RELATIONSHIP_ID);
            DomainRelationship domOriginalEBOMRel = new DomainRelationship(strOriginalEBOMRelId);
            Map mapOriginalEBOMAttributes = domOriginalEBOMRel.getAttributeMap(context, true);
            if (UIUtil.isNullOrEmpty(strEBOMChildPartId)) {
                strEBOMChildPartId = (String) map.get(DomainConstants.SELECT_ID);
            }

            DomainObject domEBOMChildPart = DomainObject.newInstance(context, strEBOMChildPartId);
            DomainRelationship domSymmetricalEBOMRel = DomainRelationship.connect(context, domNewObject, RELATIONSHIP_EBOM, domEBOMChildPart);
            // Added for TIGTK-3723 by PKH(CAD-BOM):Start
            mapOriginalEBOMAttributes.remove(TigerConstants.ATTRIBUTE_ISVPLMVISIBLE);

            if (mapOriginalEBOMAttributes.containsKey(TigerConstants.ATTRIBUTE_EFFECTIVITYCOMPILEDFORM)) {
                String strModRelMQL = "mod connection $1 add interface $2";

                String args_temp[] = new String[2];
                args_temp[0] = domSymmetricalEBOMRel.toString();
                args_temp[1] = "Effectivity Framework";

                MqlUtil.mqlCommand(context, strModRelMQL, args_temp);
            }

            // Added for TIGTK-3723 by PKH(CAD-BOM):End.
            domSymmetricalEBOMRel.setAttributeValues(context, mapOriginalEBOMAttributes);
        }
    }

    /**
     * Method to copy the original value of Mass attribute on Symmetrical Part
     * @param context
     * @param args
     * @return int
     * @throws Exception
     */
    public int copyOriginalPartMassToSymmetric(Context context, String[] args) throws Exception {
        int copyMassStatus = 0;
        String strPSS_SymmetricalPartsIdenticalMassValue = args[0];
        String strFromObjectId = args[1];
        String strToObjectId = args[2];

        DomainObject domFromObject = new DomainObject(strFromObjectId);
        DomainObject domToObject = new DomainObject(strToObjectId);

        if (strPSS_SymmetricalPartsIdenticalMassValue.equals(TigerConstants.ATTRIBUTE_PSS_SYMMETRICALPARTSIDENTICALMASS_RANGEE_YES)) {
            // CAD-BOM : TIGTK-8860 : PSE : 04-07-2017 : START
            String strMassAttributes = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentral.Classify.MassAttributeList");
            StringList slMassAttributeList = FrameworkUtil.split(strMassAttributes, ",");
            int massAttributeListSize = slMassAttributeList.size();
            // Get all attributes of To and From side object
            Map mFromObjectAttributes = domFromObject.getAttributeMap(context);
            Map mToObjectAttributes = domToObject.getAttributeMap(context);
            for (int i = 0; i < massAttributeListSize; i++) {
                String strMassAttributeName = (String) slMassAttributeList.get(i);
                // Check both side parts have Mass attribute
                if (mFromObjectAttributes.containsKey(strMassAttributeName.trim()) && mToObjectAttributes.containsKey(strMassAttributeName.trim())) {
                    // Get value of Mass attribute
                    String strFromObjectMassAttributeValue = (String) mFromObjectAttributes.get(strMassAttributeName.trim());
                    String strToObjectMassAttributeValue = (String) mToObjectAttributes.get(strMassAttributeName.trim());
                    // Make mass attribute value of To side part as From side part
                    if (!(strFromObjectMassAttributeValue.equals(strToObjectMassAttributeValue))) {
                        domToObject.setAttributeValue(context, strMassAttributeName.trim(), strFromObjectMassAttributeValue);
                        break;
                    }
                }
            }
            // CAD-BOM : TIGTK-8860 : PSE : 04-07-2017 : END
        }
        return copyMassStatus;
    }

    /**
     * Method to get Ids of all Parts connected with Symmetrical Part relationship and exclude them in search results.
     * @param context
     * @param args
     * @return StringList --- List containing Excluded Part OIDs
     * @throws Exception
     */
    public StringList excludeAlreadyConnectedSymmetricalParts(Context context, String[] args) throws Exception {

        StringList lstExcludeOIDList = new StringList();

        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strparentPartId = (String) programMap.get("objectId");

            // Exclude context part
            if (strparentPartId != null)
                lstExcludeOIDList.add(strparentPartId);

            // selectables for query
            StringList lstbusSelList = new StringList();
            lstbusSelList.add(DomainObject.SELECT_ID);

            String strWhereExpression = "from[" + TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART + "] == TRUE || to[" + TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART + "] == TRUE";

            MapList mlExcludeParts = DomainObject.findObjects(context, DomainConstants.TYPE_PART, DomainConstants.QUERY_WILDCARD, DomainConstants.QUERY_WILDCARD, DomainConstants.QUERY_WILDCARD,
                    TigerConstants.VAULT_ESERVICEPRODUCTION, strWhereExpression, true, lstbusSelList);

            // Gettinfo all connected Object Id
            Iterator itrmlExcludeParts = mlExcludeParts.iterator();
            while (itrmlExcludeParts.hasNext()) {
                Map mapExcludePart = (Map) itrmlExcludeParts.next();
                String strExcludePartId = (String) mapExcludePart.get(DomainConstants.SELECT_ID);
                lstExcludeOIDList.add(strExcludePartId);
            } // End of for loop

        } catch (Exception e) {
            // TIGTK-5405 - 03-04-2017 - VP - START
            logger.error("Error in excludeAlreadyConnectedSymmetricalParts: ", e);
            // TIGTK-5405 - 03-04-2017 - VP - END
        } // End of Catch

        return lstExcludeOIDList;
    }

    /**
     * Method called on Part properties page to get Original/Symmetric Part status.
     * @param context
     * @param args
     * @return String --- Original/Symmetric Part Status Value
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public String getOriginalSymmetricInfoOnPropertiesPage(Context context, String[] args) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        Map paramMap = (Map) programMap.get("paramMap");
        String strobjectId = (String) paramMap.get("objectId");

        StringList lstselectStmts = new StringList(4);
        lstselectStmts.addElement(DomainConstants.SELECT_ID);

        StringList lstrelStmts = new StringList(4);
        lstrelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

        DomainObject domPartobj = new DomainObject(strobjectId);
        MapList mlSymmetricalPartsMapList = domPartobj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART, "*", lstselectStmts, lstrelStmts, false, true, (short) 1, null,
                null, 0);

        if (mlSymmetricalPartsMapList.size() > 0) {
            return TigerConstants.SYMMETRIC_STATUS_ORIGINAL;
        } else {
            mlSymmetricalPartsMapList = domPartobj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART, "*", lstselectStmts, lstrelStmts, true, false, (short) 0, null, null,
                    0);
            if (mlSymmetricalPartsMapList.size() > 0) {
                return TigerConstants.SYMMETRIC_STATUS_SYMMETRICAL;
            } else {
                return DomainConstants.EMPTY_STRING;
            }

        }
    }

    /**
     * Method called on policy object to sync the lifecycle state of Original and Symmetric Part.
     * @param context
     * @param args
     * @return int
     * @throws Exception
     */

    public int lifeCycleSync(Context context, String[] args, String action) throws Exception {
        int intsyncstatus = 0;
        try {
            String strObjID = args[0];
            String strobjState = args[1];
            DomainObject domobjOrg = new DomainObject(strObjID);

            String strConObjid = domobjOrg.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART + "].from.id");
            if (strConObjid == null) {
                strConObjid = domobjOrg.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART + "].to.id");
            }

            if (strConObjid != null) {
                DomainObject domobjConnectedobj = new DomainObject(strConObjid);
                String strConobjState = domobjConnectedobj.getInfo(context, DomainConstants.SELECT_CURRENT);

                if (!strobjState.equals(strConobjState)) {
                    if (action.equals("promote")) {
                        domobjConnectedobj.promote(context);
                    } else if (action.equals("demote")) {
                        domobjConnectedobj.demote(context);
                    }

                } // end of inner if
            } // end of outer if
        } // end of try block

        catch (Exception e) {
            // TIGTK-5405 - 03-04-2017 - VP - START
            logger.error("Error in lifeCycleSync: ", e);
            // TIGTK-5405 - 03-04-2017 - VP - END
            throw new Exception(e);
        } // End of Catch
        return intsyncstatus;
    }

    /**
     * Method to automatically promote the parts connected with Symmetric Part relationship with respect to Context Part current state.
     * @param context
     * @param args
     * @return int
     * @throws Exception
     */

    public int autoPromoteSymmetricPart(Context context, String[] args) throws Exception {
        int intpromotestatus = 0;
        try {
            if (args == null || args.length < 1) {
                throw (new IllegalArgumentException());
            } // end of if

            intpromotestatus = lifeCycleSync(context, args, "promote");

        } // end of try
        catch (Exception e) {
            intpromotestatus = 1;
            // TIGTK-5405 - 03-04-2017 - VP - START
            logger.error("Error in autoPromoteSymmetricPart: ", e);
            throw new Exception(e);
            // TIGTK-5405 - 03-04-2017 - VP - END
        } // end of Catch
        return intpromotestatus;
    }

    /**
     * Method to automatically demote the parts connected with Symmetric Part relationship with respect to Context Part current state.
     * @param context
     * @param args
     * @return int
     * @throws Exception
     */

    public int autoDemoteSymmetricPart(Context context, String[] args) throws Exception {
        int intdemotestatus = 0;
        try {
            if (args == null || args.length < 1) {
                throw (new IllegalArgumentException());
            } // end of if

            intdemotestatus = lifeCycleSync(context, args, "demote");

        } // end of try
        catch (Exception e) {
            intdemotestatus = 1;
            // TIGTK-5405 - 03-04-2017 - VP - START
            logger.error("Error in autoDemoteSymmetricPart: ", e);
            // TIGTK-5405 - 03-04-2017 - VP - END
        } // end of Catch
        return intdemotestatus;
    }

    /**
     * Generic method to clone the object Number generator for type PART is used Auto Name Series will be determined based on Source Object Autoname Series
     * @param context
     * @param strSourceObjectId
     *            - Source object Id which is to be cloned
     * @param strPolicyValue
     *            - Policy of the Source Object
     * @param strChoosePolicyOptionValue
     *            - Policy Chooser value to decide whether to keep policy same or apply "DEV Part" policy.
     * @return - the object Id of the newly cloned object
     * @throws Exception
     */
    public String cloneStructureObject(Context context, String strSourceObjectId, String strPolicyValue, String strChoosePolicyOptionValue) throws Exception {

        DomainObject domSourceObj = DomainObject.newInstance(context, strSourceObjectId);

        String strNumberGeneratorType = domSourceObj.getInfo(context, DomainConstants.SELECT_TYPE);

        String strObjectGeneratorName = FrameworkUtil.getAliasForAdmin(context, DomainConstants.SELECT_TYPE, strNumberGeneratorType, true);
        // Modified for PPDM-ERGO:TIGTK-4500:PK:06/03/2017:Start
        String strAutoNumberSeries = DomainConstants.EMPTY_STRING;

        String strAutoName = DomainObject.getAutoGeneratedName(context, strObjectGeneratorName, strAutoNumberSeries);
        // Modified for PPDM-ERGO:TIGTK-4500:PK:06/03/2017:End

        DomainObject domclonedSourceObj = new DomainObject(domSourceObj.cloneObject(context, strAutoName));

        if (strNumberGeneratorType.equals(DomainConstants.TYPE_PART)) {

            // Manually Disconnecting the EBOM relationships as Clone flag on
            // from side on the relationship is "Replicate"
            StringList lstEBOMRelIds = domclonedSourceObj.getInfoList(context, "from[" + DomainConstants.RELATIONSHIP_EBOM + "].id");

            String[] strRelIds = (String[]) lstEBOMRelIds.toArray(new String[lstEBOMRelIds.size()]);
            // TIGTK-6945 - 24-04-2017 - VP - START
            try {
                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), "", "");
                DomainRelationship.disconnect(context, strRelIds);
            } catch (Exception ex) {
                logger.error("Error in cloneStructureObject while disconnecting Replicate dEBOM  Relationships: ", ex.toString());
            } finally {
                ContextUtil.popContext(context);
            }
            // TIGTK-6945 - 24-04-2017 - VP - END
        }
        String strclonedObjectId = domclonedSourceObj.getObjectId(context);

        if (strChoosePolicyOptionValue.equalsIgnoreCase("same policy")) {

        } else {
            try {
                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), "", "");
                domclonedSourceObj.setPolicy(context, TigerConstants.POLICY_PSS_DEVELOPMENTPART);
            } catch (Exception ex) {
                // TIGTK-5405 - 03-04-2017 - VP - START
                logger.error("Error in cloneStructureObject: ", ex);
                // TIGTK-5405 - 03-04-2017 - VP - END
            } finally {
                ContextUtil.popContext(context);
            }

        }

        return strclonedObjectId;
    }

    /**
     * Generic method to get the CAD objects to be clonned for the Selected Part
     * @param context
     * @param strSourcePartObjectId
     *            - Source object Id which is to be cloned
     * @return
     * @throws Exception
     */
    public MapList getRelatedCADObjectsForClonning(Context context, String strSourcePartObjectId) throws Exception {

        DomainObject domSourcePartObj = DomainObject.newInstance(context, strSourcePartObjectId);

        StringList lstselectStmts = new StringList(3);
        lstselectStmts.addElement(DomainConstants.SELECT_ID);
        lstselectStmts.addElement(DomainConstants.SELECT_TYPE);
        lstselectStmts.addElement(DomainConstants.SELECT_NAME);

        StringList lstrelStmts = new StringList();
        lstrelStmts.add(DomainConstants.SELECT_RELATIONSHIP_NAME);

        Pattern typePattern = new Pattern(DomainConstants.TYPE_CAD_DRAWING);
        typePattern.addPattern(DomainConstants.TYPE_CAD_MODEL);

        // TIGTK-5259 - 20-03-2017 - VP - START
        Pattern relPattern = new Pattern(RELATIONSHIP_PART_SPECIFICATION);
        String sClause = "";
        // TIGTK-5259 - 20-03-2017 - VP - END

        // with "Part Specification/Charted Drawing" relationship
        MapList mlCADObject = domSourcePartObj.getRelatedObjects(context, relPattern.getPattern(), typePattern.getPattern(), lstselectStmts, lstrelStmts, false, true, (short) 1, sClause, null, 0);

        return mlCADObject;
    }

    /**
     * Generic method to clone the connected CAD Object for Clone EBOM Structure
     * @param context
     * @param strSourcePartObjectId
     *            - Source object Id which is to be cloned
     * @param strClonedPartObjectId
     *            - Cloned Object Id generated for the Source Object
     * @return
     * @throws Exception
     */
    public void copySourcePartCADObjectToClonedPart(Context context, String strSourcePartObjectId, String strClonedPartObjectId) throws Exception {

        DomainObject domSourcePartObj = DomainObject.newInstance(context, strSourcePartObjectId);
        DomainObject domClonedPartObj = DomainObject.newInstance(context, strClonedPartObjectId);

        StringList lstselectStmts = new StringList(3);
        lstselectStmts.addElement(DomainConstants.SELECT_ID);
        lstselectStmts.addElement(DomainConstants.SELECT_TYPE);
        lstselectStmts.addElement(DomainConstants.SELECT_NAME);

        StringList lstrelStmts = new StringList();
        lstrelStmts.add(DomainConstants.SELECT_RELATIONSHIP_NAME);

        Pattern typePattern = new Pattern(DomainConstants.TYPE_CAD_DRAWING);
        typePattern.addPattern(DomainConstants.TYPE_CAD_MODEL);

        Pattern relPattern = new Pattern(RELATIONSHIP_PART_SPECIFICATION);
        relPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING);

        // MapList containing the "CAD Drawing/CAD Model" connected to "Part"
        // with "Part Specification/Charted Drawing" relationship
        MapList mlCADObject = domSourcePartObj.getRelatedObjects(context, relPattern.getPattern(), typePattern.getPattern(), lstselectStmts, lstrelStmts, false, true, (short) 1, null, null, 0);

        StringList lstConnectedCADTypes = new StringList();

        // Iterating through MapList containing "CAD Drawing/CAD Model"
        // connected with "Part Specification/Charted Drawing" relationship
        for (int i = 0; i < mlCADObject.size(); i++) {
            Map<String, String> mapCADObject = (Map<String, String>) mlCADObject.get(i);
            String strCADObjectId = (String) mapCADObject.get(DomainConstants.SELECT_ID);
            DomainObject domCADSourceObj = DomainObject.newInstance(context, strCADObjectId);
            String strCADSourceObjectName = mapCADObject.get(DomainConstants.SELECT_NAME);
            String strCADSourceObjectType = mapCADObject.get(DomainConstants.SELECT_TYPE);

            String strRelationshipName = mapCADObject.get(DomainConstants.SELECT_RELATIONSHIP_NAME);

            String strClonedCADObjectName = "";
            if (lstConnectedCADTypes.contains(strCADSourceObjectType)) {
                String strAutoNumberSeries = "FAURECIA";

                String strObjectGeneratorName = FrameworkUtil.getAliasForAdmin(context, DomainConstants.SELECT_TYPE, strCADSourceObjectType, true);

                strClonedCADObjectName = DomainObject.getAutoGeneratedName(context, strObjectGeneratorName, strAutoNumberSeries);
            } else {
                strClonedCADObjectName = domClonedPartObj.getName(context);
                lstConnectedCADTypes.addElement(strCADSourceObjectType);
            }

            DomainObject domClonedCADObj = new DomainObject(domCADSourceObj.cloneObject(context, strClonedCADObjectName, null, context.getVault().getName(), true, true));

            String strClonedCADObjectId = domClonedCADObj.getInfo(context, DomainConstants.SELECT_ID);
            BusinessObject boClonedCADObject = new BusinessObject(strClonedCADObjectId);

            boClonedCADObject.setAttributeValue(context, DomainObject.ATTRIBUTE_TITLE, strClonedCADObjectName);

            // Added for TIGTK-2878(CAD-BOM) Stream -Start
            cloneAndConnectVersionedCADObjects(context, strCADObjectId, strClonedCADObjectId);
            // Added for TIGTK-2878(CAD-BOM) Stream -End
            DomainRelationship.connect(context, domClonedPartObj, strRelationshipName, domClonedCADObj);
        }
    }

    /**
     * Generic method to determine whether to reuse or clone Documents in the Clone BOM
     * @param context
     * @param strSourceObjectId
     *            - Source object Id which is to be cloned
     * @param strClonedObjectId
     *            - Cloned Object Id generated for the Source Object
     * @return
     * @throws Exception
     */
    public void copySourcePartReferenceDocumentsToClonedPart(Context context, String strSourceObjectId, String strClonedObjectId) throws Exception {
        DomainObject domSourceObj = DomainObject.newInstance(context, strSourceObjectId);
        DomainObject domClonedSourceObj = DomainObject.newInstance(context, strClonedObjectId);

        StringList lstselectStmts = new StringList(1);
        lstselectStmts.addElement(DomainConstants.SELECT_ID);

        StringList lstrelStmts = new StringList();

        // MapList containing the "Reference Documents" connected to "Part" with
        // "Reference Document" relationship

        // TIGTK-10336 : START
        MapList mlReferenceDocument = domSourceObj.getRelatedObjects(context, RELATIONSHIP_REFERENCE_DOCUMENT, CommonDocument.DEFAULT_DOCUMENT_TYPE, lstselectStmts, lstrelStmts, false, true,
                (short) 1, null, null, 0);
        // TIGTK-10336 : END

        // Iterating through MapList containing "Reference Documents" connected
        // with "Reference Document" relationship
        for (int i = 0; i < mlReferenceDocument.size(); i++) {
            Map<String, String> mapReferenceDocument = (Map<String, String>) mlReferenceDocument.get(i);
            String strReferenceDocumentObjectId = (String) mapReferenceDocument.get(DomainConstants.SELECT_ID);
            DomainObject domReferenceDocumentSourceObj = DomainObject.newInstance(context, strReferenceDocumentObjectId);

            // TIGTK-10336 : START
            String strObjectGeneratorName = FrameworkUtil.getAliasForAdmin(context, DomainConstants.SELECT_TYPE, CommonDocument.DEFAULT_DOCUMENT_TYPE, true);
            // TIGTK-10336 : END

            String strAutoName = DomainObject.getAutoGeneratedName(context, strObjectGeneratorName, null);
            DomainObject domClonedReferenceDocumentObj = new DomainObject(domReferenceDocumentSourceObj.cloneObject(context, strAutoName));

            DomainRelationship.connect(context, domClonedSourceObj, RELATIONSHIP_REFERENCE_DOCUMENT, domClonedReferenceDocumentObj);
        }
    }

    /**
     * Generic method to connect the Cloned Objects to "Change Order" object
     * @param context
     * @param strChangeOrderId
     *            - Change Order Id which is to be connected to Cloned Objects
     * @param clonedItemsList
     *            - StringList containing Cloned Items
     * @return
     * @throws Exception
     */
    public void connectCOToCloneParts(Context context, String strChangeOrderId, StringList clonedItemsList) throws Exception {

        ChangeOrder changeorder = new ChangeOrder(strChangeOrderId);
        changeorder.connectAffectedItems(context, clonedItemsList);

    }

    /**
     * Method called on the post process functionality of Clone BOM. Method makes a check whether to create Clone of CAD in the EBOM structure. Method makes a check whether to Re-use Material in the
     * EBOM structure. Method makes a check whether to Re-use Documents in the EBOM structure.
     * @param context
     * @param args
     *            , String array containing: 1. List of selected items on UI which is to be cloned
     * @return String - Clone Object Id of the Root part in EBOM Structure.
     * @throws Exception
     */

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @com.matrixone.apps.framework.ui.PostProcessCallable
    public String postProcessForCloneEBOM(Context context, String[] args) throws Exception {
        String strClonedRootID = DomainObject.EMPTY_STRING;
        // TIGTK-8812:Modified on 6/7/2017 :Start by SIE
        PropertyUtil.setGlobalRPEValue(context, "PSS_CloneEBOM", "CloneEBOM");
        boolean isTrasactionActive = false;
        try {
            ContextUtil.startTransaction(context, true);
            isTrasactionActive = true;
            // Create a map which will help us identify which object has been
            // copied from which object
            HashMap<String, String> originalCloneMap = new HashMap<String, String>();

            StringList lstselectedrelId = new StringList();
            StringList lstselectStmts = new StringList(2);
            StringList lstrelStmts = new StringList(2);

            lstselectStmts.addElement(DomainConstants.SELECT_ID);
            lstselectStmts.addElement(DomainConstants.SELECT_POLICY);
            lstselectStmts.addElement(DomainConstants.SELECT_TYPE);

            lstrelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            lstrelStmts.addElement(DomainConstants.SELECT_FROM_ID);

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MCADIntegrationSessionData integSessionData = (MCADIntegrationSessionData) programMap.get("integSessionData");

            String strSourceRootObjectID = (String) programMap.get("parentOID");

            DomainObject domSourceRootObject = DomainObject.newInstance(context, strSourceRootObjectID);
            String strSourceRootObjectPolicy = domSourceRootObject.getInfo(context, DomainConstants.SELECT_POLICY);

            MapList mlConnectedPartsList = domSourceRootObject.getRelatedObjects(context, RELATIONSHIP_EBOM, DomainConstants.TYPE_PART, lstselectStmts, lstrelStmts, false, true, (short) 0, null, null,
                    0);

            String strSameOrDevPartPolicy = (String) programMap.get("ChoosePolicy");
            String strCADClone = (String) programMap.get("CloneOfCAD");
            // TIGTK-10212 : PSE : START
            String strKeepChartedDrawing = (String) programMap.get("KeepChartedDrawing");
            // TIGTK-10212 : PSE : END
            String strReuseDocumentsOption = (String) programMap.get("DocumentOption");
            String strCOActualOID = (String) programMap.get("COActualOID");
            String strReuseMaterial = (String) programMap.get("chkReuseMaterial");
            String strColorDiversity = (String) programMap.get("chkKeepColorDiversity");
            String strTechnicalDiversity = (String) programMap.get("chkKeepTechnicalDiversity");

            StringList lstExcludeAttribute = new StringList();
            lstExcludeAttribute.add(TigerConstants.ATTRIBUTE_ISVPLMVISIBLE);
            lstExcludeAttribute.add(TigerConstants.ATTRIBUTE_PSS_REPLACED_WITH_LATEST_REVISION);
            if (UIUtil.isNullOrEmpty(strTechnicalDiversity)) {
                lstExcludeAttribute.add(TigerConstants.ATTRIBUTE_EFFECTIVITYORDEREDIMPACTINGCRITERIA);
                lstExcludeAttribute.add(TigerConstants.ATTRIBUTE_EFFECTIVITYORDEREDCRITERIADICTIONARY);
                lstExcludeAttribute.add(TigerConstants.ATTRIBUTE_EFFECTIVITYTYPES);
                lstExcludeAttribute.add(TigerConstants.ATTRIBUTE_EFFECTIVITYEXPRESSION);
                lstExcludeAttribute.add(TigerConstants.ATTRIBUTE_EFFECTIVITYCOMPILEDFORM);
                lstExcludeAttribute.add(TigerConstants.ATTRIBUTE_EFFECTIVITY_VARIABLE_INDEXES);
                lstExcludeAttribute.add(TigerConstants.ATTRIBUTE_EFFECTIVITYEXPRESSIONBINARY);
                lstExcludeAttribute.add(TigerConstants.ATTRIBUTE_EFFECTIVITYPROPOSEDEXPRESSION);
                lstExcludeAttribute.add(TigerConstants.ATTRIBUTE_EFFECTIVITYORDEREDCRITERIA);
                lstExcludeAttribute.add(TigerConstants.ATTRIBUTE_PSS_SIMPLEREFFECTIVITYEXPRESSION);
                lstExcludeAttribute.add(TigerConstants.ATTRIBUTE_PSS_CUSTOMEFFECTIVITYEXPRESSION);

            }

            // First clone parentOID --> put in originalCloneMap --> key =
            // parentOID, value = cloned Parent OID

            // Creating clone of Root object in EBOM Structure
            // TIGTK-10212 : PSE : START
            strClonedRootID = cloneBOMPart(context, strSourceRootObjectID, strSourceRootObjectPolicy, strSameOrDevPartPolicy, strCADClone, strReuseDocumentsOption, strReuseMaterial, strColorDiversity,
                    strKeepChartedDrawing);
            // TIGTK-10212 : PSE : END
            originalCloneMap.put(strSourceRootObjectID, strClonedRootID);

            StringList lstSelectedItemList = (StringList) programMap.get("SelectedItems");
            int intSelectedItemListSize = 0;
            if (lstSelectedItemList != null) {
                intSelectedItemListSize = lstSelectedItemList.size();
            }

            // Iterating through Select Parts List obtained from JSP
            for (int intIndex = 0; intIndex < intSelectedItemListSize; intIndex++) {
                String selectedItem = (String) lstSelectedItemList.get(intIndex);
                String strSelectedrelId = selectedItem.split("\\|")[0];

                if (UIUtil.isNotNullAndNotEmpty(strSelectedrelId))
                    lstselectedrelId.add(strSelectedrelId);
            }

            int intConnectedPartsListSize = mlConnectedPartsList.size();

            // This list will be used to decide whether the object needs to
            // be reused or cloned
            StringList lstReusedItems = new StringList();
            StringList lstClonedItems = new StringList();
            StringList lstAlreadyProceedRelId = new StringList();

            for (int intIndex = 0; intIndex < intConnectedPartsListSize; intIndex++) {
                Map<String, String> mapConnectedPartsMap = (Map) mlConnectedPartsList.get(intIndex);
                String strSourceObjectId = (String) mapConnectedPartsMap.get(DomainConstants.SELECT_ID);
                String strPolicy = (String) mapConnectedPartsMap.get(DomainConstants.SELECT_POLICY);
                String strParentId = (String) mapConnectedPartsMap.get(DomainConstants.SELECT_FROM_ID);
                String relId = (String) mapConnectedPartsMap.get(DomainConstants.SELECT_RELATIONSHIP_ID);

                // Add for TIGTK-3898:Start
                if (!lstAlreadyProceedRelId.contains(relId)) {
                    // Add for TIGTK-3898:End
                    if (!lstReusedItems.contains(strParentId)) {

                        String strClonedParentId = originalCloneMap.get(strParentId);
                        lstClonedItems.add(strClonedParentId);

                        DomainRelationship domRel = new DomainRelationship();
                        // changes done for Standard Part
                        if (lstselectedrelId.contains(relId) && !strPolicy.equalsIgnoreCase("Standard Part")) {
                            String strSourceClonedObjId = DomainObject.EMPTY_STRING;

                            if (originalCloneMap.containsKey(strSourceObjectId)) {
                                strSourceClonedObjId = originalCloneMap.get(strSourceObjectId);

                            } else {
                                boolean boolCloneObject = true;
                                if (boolCloneObject) {
                                    // TIGTK-10212 : PSE : START
                                    strSourceClonedObjId = cloneBOMPart(context, strSourceObjectId, strPolicy, strSameOrDevPartPolicy, strCADClone, strReuseDocumentsOption, strReuseMaterial,
                                            strColorDiversity, strKeepChartedDrawing);
                                    // TIGTK-10212 : PSE : END
                                    originalCloneMap.put(strSourceObjectId, strSourceClonedObjId);
                                    lstClonedItems.add(strSourceClonedObjId);

                                }
                            }
                            // Connecting the "Cloned Parent" to the
                            // "Cloned Object Id"
                            domRel = DomainRelationship.connect(context, DomainObject.newInstance(context, strClonedParentId), RELATIONSHIP_EBOM,
                                    DomainObject.newInstance(context, strSourceClonedObjId));
                        } else {

                            // Need to reuse so connect the current object as it is.
                            // Connecting the "Cloned Parent" to the "Object Ids"
                            // which are to be reused
                            // TIGTK-6945 - 24-04-2017 - VP - START
                            try {
                                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), "", "");
                                domRel = DomainRelationship.connect(context, DomainObject.newInstance(context, strClonedParentId), RELATIONSHIP_EBOM,
                                        DomainObject.newInstance(context, strSourceObjectId));
                            } catch (Exception ex) {
                                logger.error("Error in postProcessForCloneEBOM while connecting Reused EBOM  Relationships: ", ex.toString());
                            } finally {
                                ContextUtil.popContext(context);
                            }
                            // TIGTK-6945 - 24-04-2017 - VP - END

                            lstReusedItems.addElement(strSourceObjectId);
                        }
                        // copy all their relationship attributes

                        // TIGTK-7421 - 02-05-2017 - VP - START
                        if (UIUtil.isNotNullAndNotEmpty(domRel.toString())) {
                            if (UIUtil.isNotNullAndNotEmpty(strTechnicalDiversity)) {
                                String strModRelMQL = "mod connection $1 add interface $2";

                                String args_temp[] = new String[2];
                                args_temp[0] = domRel.toString();
                                args_temp[1] = "Effectivity Framework";

                                MqlUtil.mqlCommand(context, strModRelMQL, args_temp);
                            }
                            copySourceRelDataToNewRel(context, relId, domRel.toString(), lstExcludeAttribute, true);
                        }
                        // TIGTK-7421 - 02-05-2017 - VP - END

                        // Connecting "Change Order" to "Cloned Objects"
                        if (UIUtil.isNotNullAndNotEmpty(strCOActualOID)) {
                            StringList lstClonedECStdItems = new StringList();
                            for (int p = 0; p < lstClonedItems.size(); p++) {
                                String strObjectId = (String) lstClonedItems.get(p);
                                DomainObject domCloneId = DomainObject.newInstance(context, strObjectId);
                                String StrClonePolicy = domCloneId.getInfo(context, DomainConstants.SELECT_POLICY);
                                if (!StrClonePolicy.equals(TigerConstants.POLICY_PSS_DEVELOPMENTPART)) {
                                    lstClonedECStdItems.add(strObjectId);
                                }
                            }
                            connectCOToCloneParts(context, strCOActualOID, lstClonedECStdItems);
                        }
                    }
                    // Add for TIGTK-3898:Start
                    // Starts for TIGTK-4505 by PTE
                    else {
                        lstReusedItems.add(strSourceObjectId);// adding the current
                        // object in
                        // the reuse list so that
                        // its child can also be
                        // re-used
                    }
                    // Ends for TIGTK-4505 by PTE
                    // Add for TIGTK-3898:Start
                    lstAlreadyProceedRelId.add(relId);
                }

            }
            if (isTrasactionActive)
                ContextUtil.commitTransaction(context);

            // Clone the connected CAD for Clone BOM if strCADClone is not Empty
            if (UIUtil.isNotNullAndNotEmpty(strCADClone)) {
                cloneAndConnectRelatedCADObjects(context, strSourceRootObjectID, originalCloneMap, integSessionData);
            }
            // TIGTK-4291 - START
            // If execution is Background Execution then send an Icon Mail to the Owner of the Process
            String strIsBackgroundExecution = (String) programMap.get("isBackgroundExecution");
            if (strIsBackgroundExecution.equalsIgnoreCase("true")) {
                sendBackGroundProcessCompletionNotification(context, strClonedRootID, strSourceRootObjectID);
            }
            // TIGTK-4291 - END
        } catch (Exception ex) {
            // TIGTK-5405 - 03-04-2017 - VP - START
            logger.error("Error in postProcessForCloneEBOM: ", ex);
            // TIGTK-5405 - 03-04-2017 - VP - END
            if (isTrasactionActive)
                ContextUtil.abortTransaction(context);
            throw ex;

        } finally {
            PropertyUtil.setGlobalRPEValue(context, "PSS_CloneEBOM", "");
            // TIGTK-8812:Modified on 6/7/2017 :End by SIE
        }
        return strClonedRootID;
    }

    /**
     * This method sends the email Notification to the Back ground Process owner on completion of the Process TIGTK 4291
     * @param context
     * @param strClonedPartId
     * @param strSelectedPartId
     * @throws Exception
     */
    public void sendBackGroundProcessCompletionNotification(Context context, String strClonedPartId, String strSelectedPartId) throws Exception {

        String strObjectCreator = "";
        StringBuffer strMessageKeyBuffer = null;

        StringList slEmailReceiver = null;
        StringList lstAttachments = new StringList();

        String strSubject = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(),
                "PSS_emxEngineeringCentral.CloneEBOM.BackgroundProcess.CompletionNotificationSubject");
        String strMessage = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(),
                "PSS_emxEngineeringCentral.CloneEBOM.BackgroundProcess.CompletionNotificationMessage");

        try {
            DomainObject domSelectedPart = DomainObject.newInstance(context, strSelectedPartId);
            String strSelectedPartName = domSelectedPart.getInfo(context, DomainConstants.SELECT_NAME);

            DomainObject domClonedPart = DomainObject.newInstance(context, strClonedPartId);
            String strClonedPartName = domClonedPart.getInfo(context, DomainConstants.SELECT_NAME);

            strSubject = strSubject.replace("{SELECTEDPARTNAME}", strSelectedPartName);
            strMessage = strMessage.replace("{SELECTEDPARTNAME}", strSelectedPartName);
            strMessage = strMessage.replace("{CLONEDPARTNAME}", strClonedPartName);

            strObjectCreator = context.getUser();

            slEmailReceiver = new StringList();
            slEmailReceiver.add(strObjectCreator);

            strMessageKeyBuffer = new StringBuffer();
            strMessageKeyBuffer.append("\n\n");
            strMessageKeyBuffer.append(strMessage);
            strMessageKeyBuffer.append("\n\n");

            strMessage = strMessageKeyBuffer.toString();

            String[] subjectKeys = {};
            String[] subjectValues = {};
            String[] messageKeys = {};
            String[] messageValues = {};

            lstAttachments.add(strSelectedPartId);
            lstAttachments.add(strClonedPartId);

            emxMailUtil_mxJPO.sendNotification(context, (StringList) slEmailReceiver, null, null, strSubject, subjectKeys, subjectValues, strMessage, messageKeys, messageValues,
                    (StringList) lstAttachments, null);

        } catch (Exception e) {
            // TIGTK-5405 - 03-04-2017 - VP - START
            logger.error("Error in sendBackGroundProcessCompletionNotification: ", e);
            // TIGTK-5405 - 03-04-2017 - VP - END
            // Findbug Issue correction start
            // Date: 22/03/2017
            // By: Asha G.
            throw e;
            // Findbug Issue correction End
        }
    }

    /**
     * Method called to clone the CAD structure of the selected Parts and connect the clonned CAD to new clonned Part Objects. Added for DF_ALM_1491
     * @param context
     * @param strSourceRootObjectID
     * @param originalCloneMap
     * @param integSessionData
     * @return
     * @throws Exception
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void cloneAndConnectRelatedCADObjects(Context context, String strSourceRootObjectID, HashMap<String, String> originalCloneMap, MCADIntegrationSessionData integSessionData)
            throws Exception {

        try {
            StringBuffer sbSourceRootCADObjectID = new StringBuffer();
            MapList mlCADObjects = null;

            StringBuffer sbRelatedCADs = new StringBuffer();
            Map mapTemp = new HashMap();
            StringBuilder strSelectedPartStructure = new StringBuilder();
            MapList mlSelectedPartStructure = new MapList();
            String strNewPartId = "";
            String strNewPartName = "";
            DomainObject domObject;
            String strCADId = "";
            String strCADName = "";
            String strSelectedPartName = "";

            // Modification done by PTE for find bug(Performance Category) on date 1/18/2017

            // TIGTK-5259 - 20-03-2017 - VP - START
            for (Object objSelectedPartID : originalCloneMap.entrySet()) {

                sbRelatedCADs.setLength(0);
                strNewPartId = ((Entry<String, String>) objSelectedPartID).getValue();
                String strSelectedPartID = ((Entry<String, String>) objSelectedPartID).getKey();
                domObject = DomainObject.newInstance(context, strNewPartId);
                strNewPartName = domObject.getInfo(context, DomainConstants.SELECT_NAME);

                domObject = DomainObject.newInstance(context, strSelectedPartID);
                strSelectedPartName = domObject.getInfo(context, DomainConstants.SELECT_NAME);

                mlCADObjects = getRelatedCADObjectsForClonning(context, strSelectedPartID);
                for (int i = 0; i < mlCADObjects.size(); i++) {
                    mapTemp = (Map) mlCADObjects.get(i);
                    strCADId = (String) mapTemp.get(DomainConstants.SELECT_ID);
                    strCADName = (String) mapTemp.get(DomainConstants.SELECT_NAME);
                    int nPartNameIndex = strCADName.indexOf(strSelectedPartName);

                    if (nPartNameIndex > -1) {
                        strCADName = strCADName.replace(strSelectedPartName, strNewPartName);
                    } else {
                        strCADName = strNewPartName + "_" + strCADName;
                    }

                    sbRelatedCADs.append(strCADId);
                    sbRelatedCADs.append("&");
                    sbRelatedCADs.append(strCADName);
                    sbRelatedCADs.append(",");

                    Map mapCAD = new HashMap();
                    mapCAD.put("type", (String) mapTemp.get(DomainConstants.SELECT_TYPE));
                    mapCAD.put("relationship", (String) mapTemp.get(DomainConstants.SELECT_RELATIONSHIP_NAME));
                    mapCAD.put("name", strCADName);
                    mapCAD.put("id", strNewPartId);
                    mlSelectedPartStructure.add(mapCAD);
                }

                if (mlCADObjects.size() > 0) {
                    // OriginalPartId|NewPartId|NewPartName|RelatedCADObjects<comma separated>@
                    strSelectedPartStructure.append(strSelectedPartID).append("|").append(strNewPartId).append("|").append(sbRelatedCADs.toString()).append("@");
                    ;
                }

            }
            // TIGTK-5259 - 20-03-2017 - VP - END
            // TIGTK- 8812 - Modified on 6/7/2017 by SIE :Start
            StringBuffer sbCADDrawingItems = new StringBuffer();
            StringBuffer sbCADItems = new StringBuffer();
            mlCADObjects = getRelatedCADObjectsForClonning(context, strSourceRootObjectID);
            for (int i = 0; i < mlCADObjects.size(); i++) {
                mapTemp = (Map) mlCADObjects.get(i);

                String strRootCADId = (String) mapTemp.get(DomainConstants.SELECT_ID);
                DomainObject domCAD = DomainObject.newInstance(context, strRootCADId);

                if (domCAD.isKindOf(context, ConfigurationConstants.TYPE_CAD_DRAWING)) {
                    sbCADDrawingItems.append(strRootCADId);
                    sbCADDrawingItems.append(";");
                } else {
                    sbCADItems.append(strRootCADId);
                    sbCADItems.append(";");
                }
            }
            if (UIUtil.isNotNullAndNotEmpty(sbCADDrawingItems.toString())) {
                sbSourceRootCADObjectID.append(sbCADDrawingItems);
            }
            if (UIUtil.isNotNullAndNotEmpty(sbCADItems.toString())) {
                sbSourceRootCADObjectID.append(sbCADItems);
            }
            // TIGTK- 8812 - Modified on 6/7/2017 by SIE :End
            if (UIUtil.isNotNullAndNotEmpty(sbSourceRootCADObjectID.toString())) {

                HashMap argsMap = new HashMap();
                argsMap.put("objectId", sbSourceRootCADObjectID.toString());
                argsMap.put("integSessionData", integSessionData);
                argsMap.put("SelectedPartStructure", strSelectedPartStructure.toString());
                String[] paramArgs = JPO.packArgs(argsMap);

                String strCloneResult = "";
                // TIGTK-7800 : START
                PSS_MCADSaveAs_mxJPO mcadSaveAs = new PSS_MCADSaveAs_mxJPO();
                strCloneResult = mcadSaveAs.cloneCAD(context, paramArgs);
                // TIGTK-7800 : END

                // Connect the Clonned CAD Structure with Clonned Part Structure
                if ("SaveAs operation successful".equals(strCloneResult)) {

                    String strCADType = "";
                    String strRelationshipName = "";
                    BusinessObject busCADObject;

                    for (int i = 0; i < mlSelectedPartStructure.size(); i++) {
                        mapTemp = (Map) mlSelectedPartStructure.get(i);
                        strNewPartId = (String) mapTemp.get("id");
                        strCADType = (String) mapTemp.get("type");
                        strRelationshipName = (String) mapTemp.get("relationship");
                        strNewPartName = (String) mapTemp.get("name");

                        busCADObject = new BusinessObject(strCADType, strNewPartName, "-A", TigerConstants.VAULT_ESERVICEPRODUCTION);

                        if (busCADObject.exists(context)) {
                            // TIGTK-5259 - 20-03-2017 - VP - START
                            MqlUtil.mqlCommand(context, "connect bus $1   relationship '$2' to  '$3' '$4' $5;", new String[] { strNewPartId, strRelationshipName, strCADType, strNewPartName, "-A" });
                            // TIGTK-5259 - 20-03-2017 - VP - END
                        }
                    }
                }
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 03-04-2017 - VP - START
            logger.error("Error in cloneAndConnectRelatedCADObjects: ", ex);
            // TIGTK-5405 - 03-04-2017 - VP - END
            throw ex;

        }
    }

    /**
     * This method copies attributes on the relationship "EBOM" from source to target it doesn't copy hidden attributes
     * @param context
     * @param strSourceRelId
     *            - Source relationship id from which the attributes to be copied
     * @param strTargetRelId
     *            - target relationship id to which the attribute to be copied
     * @param lstExcludeAttribute
     *            - optional - can be null - attribute list which is not to be copied to the new list
     * @throws Exception
     */
    public void copySourceRelDataToNewRel(Context context, String strSourceRelId, String strTargetRelId, StringList lstExcludeAttribute, boolean boolCopyHiddenAttributes) throws Exception {
        DomainRelationship domSourceRel = new DomainRelationship(strSourceRelId);

        Map mapSourceAttribute = domSourceRel.getAttributeMap(context, boolCopyHiddenAttributes);
        // Copying all the hidden/non-hidden attributes, so keeping last
        // argument as true

        if (lstExcludeAttribute != null) {
            for (int i = 0; i < lstExcludeAttribute.size(); i++) {
                String strAttr = (String) lstExcludeAttribute.get(i);
                if (mapSourceAttribute.containsKey(strAttr)) {
                    mapSourceAttribute.remove(strAttr);
                }

            }
            DomainRelationship domTargetRel = new DomainRelationship(strTargetRelId);
            domTargetRel.setAttributeValues(context, mapSourceAttribute);
        }
    }

    /**
     * This method is called to Clone the Parts present in EBOM Structure.
     * @param context
     * @param strSourceObjectId
     *            - Source object Id which is to be cloned
     * @param strPolicyValue
     *            - Policy of the Source Object
     * @param strSameOrDevPartPolicy
     *            - Policy Chooser value to decide whether to keep policy same or apply "DEV Part" policy.
     * @param strCADClone
     *            -Whether to clone connected CAD or not
     * @param strReuseDocumentsOption
     *            -Whether to clone or reuse the associated "Reference Document"
     * @return - the object Id of the newly cloned object
     * @throws Exception
     */
    public String cloneBOMPart(Context context, String strSourceObjectId, String strPolicy, String strSameOrDevPartPolicy, String strCADClone, String strReuseDocumentsOption, String strReuseMaterial,
            String strColorDiversity, String strKeepChartedDrawing) throws Exception {

        final String REFERENCE_DOCUMENT_CLONE = "Clone";
        final String REFERENCE_DOCUMENT_NONE = "None";

        String strSourceClonedObjId = cloneStructureObject(context, strSourceObjectId, strPolicy, strSameOrDevPartPolicy);

        DomainObject domSourceClonedObj = DomainObject.newInstance(context, strSourceClonedObjId);
        DomainObject domSourceObj = DomainObject.newInstance(context, strSourceObjectId);

        // TIGTK-9591 -Start
        // TIGTK-10360 : START
        // Connecting the Original and Cloned Part Object with "Derived" Relationship
        try {
            ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, null, null);
            DomainRelationship domainRelationship = DomainRelationship.connect(context, domSourceObj, RELATIONSHIP_DERIVED, domSourceClonedObj);
            domainRelationship.setAttributeValue(context, TigerConstants.ATTRIBUTE_DERIVED_CONTEXT, "PSS_Clone_BOM");
        } finally {
            ContextUtil.popContext(context);
        }
        // TIGTK-10360 : END
        // TIGTK-9591 -End

        if (UIUtil.isNotNullAndNotEmpty(strReuseMaterial)) {

            StringList lstselectStmts = new StringList(1);
            lstselectStmts.addElement(DomainConstants.SELECT_ID);

            StringList lstrelStmts = new StringList();
            MapList mlMaterialObject = domSourceObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_MATERIAL, DomainConstants.QUERY_WILDCARD, lstselectStmts, lstrelStmts, false, true,
                    (short) 1, null, null, 0);
            for (int i = 0; i < mlMaterialObject.size(); i++) {
                Map<String, String> mapMaterialObject = (Map<String, String>) mlMaterialObject.get(i);
                String strMaterialObjectId = (String) mapMaterialObject.get(DomainConstants.SELECT_ID);
                DomainObject domMaterialSourceObj = DomainObject.newInstance(context, strMaterialObjectId);
                DomainRelationship.connect(context, domSourceClonedObj, TigerConstants.RELATIONSHIP_PSS_MATERIAL, domMaterialSourceObj);

            }
        }
        if (UIUtil.isNotNullAndNotEmpty(strColorDiversity)) {

            StringList lstselectStmts = new StringList(1);
            lstselectStmts.addElement(DomainConstants.SELECT_ID);

            StringList lstrelStmts = new StringList();
            MapList mlColorDiversityObject = domSourceObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_COLORLIST, TigerConstants.TYPE_PSS_COLOROPTION, lstselectStmts, lstrelStmts, false,
                    true, (short) 1, null, null, 0);
            for (int i = 0; i < mlColorDiversityObject.size(); i++) {
                Map<String, String> mapColordiversityObject = (Map<String, String>) mlColorDiversityObject.get(i);
                String strMaterialObjectId = (String) mapColordiversityObject.get(DomainConstants.SELECT_ID);
                DomainObject domColorDiversitySourceObj = DomainObject.newInstance(context, strMaterialObjectId);
                DomainRelationship.connect(context, domSourceClonedObj, TigerConstants.RELATIONSHIP_PSS_COLORLIST, domColorDiversitySourceObj);

            }

        }

        // Attach the connected Document to Clone BOM if strReuseDocuments is
        // not Empty
        if (UIUtil.isNotNullAndNotEmpty(strReuseDocumentsOption)) {
            if (strReuseDocumentsOption.equalsIgnoreCase(REFERENCE_DOCUMENT_CLONE) || strReuseDocumentsOption.equalsIgnoreCase(REFERENCE_DOCUMENT_NONE)) {
                // manually disconnecting the Reference Documents relationships
                // as Clone flag on from side on the relationship is "Replicate"
                StringList lstReferenceDocumentRelIds = domSourceClonedObj.getInfoList(context, "from[" + DomainConstants.RELATIONSHIP_REFERENCE_DOCUMENT + "].id");

                String[] strRelIds = (String[]) lstReferenceDocumentRelIds.toArray(new String[lstReferenceDocumentRelIds.size()]);
                DomainRelationship.disconnect(context, strRelIds);

                if (strReuseDocumentsOption.equalsIgnoreCase(REFERENCE_DOCUMENT_CLONE)) {
                    copySourcePartReferenceDocumentsToClonedPart(context, strSourceObjectId, strSourceClonedObjId);
                }

            }
        }

        // this block is the case where the object is
        // cloned, so sourceClonedObjId is the new id
        // which is cloned

        // TIGTK-10212 : PSE : START
        String strKeepChartedDrawings = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(),
                "emxEngineeringCentral.PSS_CloneEBOMOptionsForm.KeepChartedDrawings");
        if (UIUtil.isNotNullAndNotEmpty(strKeepChartedDrawing) && strKeepChartedDrawing.equalsIgnoreCase(strKeepChartedDrawings)) {
            copySourcePartChartedDrawingsToClonedPart(context, strSourceObjectId, strSourceClonedObjId);
        }
        // TIGTK-10212 : PSE : END

        return strSourceClonedObjId;
    }

    // Addition for Tiger - CAD BOM stream by SGS ends

    /**
     * checkPartStateforChartedDrawing - This method is used to check state of Part. if Part is not in INWORK state then it will not allow to delete the specifications
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds the following input arguments: 0 - objectList MapList
     * @returns Object
     * @throws Exception
     *             if the operation fails
     */

    public int checkPartStateforChartedDrawing(Context context, String[] args) throws Exception {
        try {
            String strRPEValue = PropertyUtil.getGlobalRPEValue(context, "PSS_CAD_DISCONNECT_FROM_COPROMOTE");
            if (strRPEValue.equalsIgnoreCase("PSS_CAD_DISCONNECT_FROM_COPROMOTE")) {
                return 0;
            }

            String strPerformTransitionOnCoPromote = PropertyUtil.getGlobalRPEValue(context, "performTransitionOnCOPromote");

            if (UIUtil.isNotNullAndNotEmpty(strPerformTransitionOnCoPromote) && strPerformTransitionOnCoPromote.equalsIgnoreCase("True")) {
                return 0;
            }
            String strFromobjectID = args[0];// Getting the Part Object id
            // id
            DomainObject PartObj = DomainObject.newInstance(context, strFromobjectID);
            String strCurrentState = (String) PartObj.getInfo(context, DomainConstants.SELECT_CURRENT);// current state of Part

            String strChartedDrawingConnectState = EnoviaResourceBundle.getProperty(context, "PSS_emxIEFDesignCenter.ChartedDrawing.Disconnect.AllowedState");

            String strChartedDrawingDeleteMessage = EnoviaResourceBundle.getProperty(context, "emxIEFDesignCenterStringResource", context.getLocale(),
                    "PSS_emxIEFDesignCenter.ChartedDrawing.Delete.Message");
            if ((strChartedDrawingConnectState).contains(strCurrentState)) {
                return 0; // allow delete for specified state
            }
            emxContextUtil_mxJPO.mqlNotice(context, strChartedDrawingDeleteMessage);
            return 1;
        } catch (Exception e) {
            throw e;
        }
    }// End of checkPartStateforChartedDrawing method

    /**
     * The Program function to be called to validate all the child information of root Part.
     * @param context
     * @param args
     * @return String - Maplist
     * @throws Exception
     */

    // TIGTK-5788 :Rutuja Ekatpure :4/4/2017:Start
    @SuppressWarnings("unused")
    public MapList validateFreezeBOM(Context context, String[] args) throws Exception {
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strRootObjectId = (String) programMap.get("objectId");
            DomainObject domRootPartObj = DomainObject.newInstance(context, strRootObjectId);
            String rootObjectState = domRootPartObj.getInfo(context, DomainConstants.SELECT_CURRENT);
            StringList strList = new StringList(1);
            strList.addElement(DomainConstants.SELECT_ID);
            strList.addElement(DomainConstants.SELECT_TYPE);
            strList.addElement(DomainConstants.SELECT_POLICY);
            strList.addElement(DomainConstants.SELECT_CURRENT);
            StringBuffer sWhereClause = new StringBuffer();
            sWhereClause.append("policy!= " + "\"" + TigerConstants.POLICY_STANDARDPART + "\"");
            sWhereClause.append(" && policy!= " + "\"" + TigerConstants.POLICY_PSS_ECPART + "\"");
            sWhereClause.append(" && current!= " + "\"" + TigerConstants.STATE_OBSOLETE + "\"");
            sWhereClause.append(" && current!= " + "\"" + TigerConstants.STATE_RELEASED_CAD_OBJECT + "\"");
            sWhereClause.append(" && current!= " + "\"" + TigerConstants.STATE_DEVELOPMENTPART_COMPLETE + "\"");
            // TIGTK-8361 : 28-09-2017 : START
            // Added below check for the root part get the list of child objects with current state not beyond the root parts state
            if (TigerConstants.STATE_DEVELOPMENTPART_CREATE.equalsIgnoreCase(rootObjectState)) {
                sWhereClause.append(" && current!= " + "\"" + TigerConstants.STATE_DEVELOPMENTPART_PEERREVIEW + "\" ");
            }
            // TIGTK-8361 : 28-09-2017 : END
            Pattern relPattern = new Pattern(DomainConstants.RELATIONSHIP_EBOM);
            relPattern.addPattern(DomainConstants.RELATIONSHIP_PART_SPECIFICATION);

            MapList mlobjMapList = domRootPartObj.getRelatedObjects(context, relPattern.getPattern(), // relationship
                    "*", // object pattern
                    strList, // object selects
                    null, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 0, // recursion level
                    sWhereClause.toString(), // object where clause
                    null, // relationship where clause
                    0);

            mlobjMapList.sort(DomainConstants.SELECT_LEVEL, "descending", "string");
            return mlobjMapList;
        } catch (Exception e) {
            // TIGTK-5405 - 03-04-2017 - VP - START
            logger.error("Error in validateFreezeBOM: ", e);
            // TIGTK-5405 - 03-04-2017 - VP - END
            throw e;
        }
    }
    // TIGTK-5788 :Rutuja Ekatpure :4/4/2017:End
    // method end.

    /**
     * This method checks if atleast one part family is connected to the Part object
     * @param context
     * @param args
     *            --Object Id of Part
     * @return -- '0'if success...'1' for failure with error message
     * @throws Exception
     */
    public int checkPartFamily(Context context, String[] args) throws Exception {
        String strPartObjectID = args[0];
        DomainObject domPartObject = DomainObject.newInstance(context, strPartObjectID);
        StringList lstselectStmts = new StringList(1);
        lstselectStmts.addElement(DomainConstants.SELECT_TYPE);

        MapList mlobjMapList = domPartObject.getRelatedObjects(context,

                DomainConstants.RELATIONSHIP_CLASSIFIED_ITEM, // relationship pattern
                DomainConstants.TYPE_PART_FAMILY, // object pattern
                lstselectStmts, // object selects
                null, // relationship selects
                true, // to direction
                false, // from direction
                (short) 1, // recursion level
                null, // object where clause
                null, 0);

        if (mlobjMapList.isEmpty()) // check whether part family is connected..
        {
            // Added for TIGTK-3751 by PT and PS on date : 07-Dec-2016 : START
            String strPartName = domPartObject.getInfo(context, DomainConstants.SELECT_NAME);
            strPartName += " : ";
            String strAlertMessage = strPartName.concat(EngineeringUtil.i18nStringNow(context, "emxEngineeringCentral.Alert.PSS_PartFamilyNotConnected", context.getSession().getLanguage()));
            // Added for TIGTK-3751 by PT on date : 07-Dec-2016 : END
            emxContextUtil_mxJPO.mqlNotice(context, strAlertMessage);
            return 1;

        } else {
            return 0;
        }

    }

    // method end.
    // TIGTK-11260 : 10-01-2018 : START
    /**
     * This method checks if all the mandatory attributes are filled
     * @param context
     * @param args
     *            --Object Id of Part
     * @return -- '0'if success...'1' for failure with error message
     * @throws Exception
     */

    public int checkMandatoryAttributesFilled(Context context, String[] args) throws Exception {
        int iCheckResult = 0;

        try {
            final String ATTR_VALUE_UNASSIGNED = "UNASSIGNED";

            String strObjectId = args[0];
            DomainObject domObj = DomainObject.newInstance(context, strObjectId);

            String strMandatoryAttribute = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentral.Attributes.MandatoryAttributeList");

            if (UIUtil.isNotNullAndNotEmpty(strMandatoryAttribute)) {
                StringList slNonFilledMandatoryAttributeListForDisplay = new StringList();
                // TIGTK-16127,TIGTK-16126:20-07-2018:STARTS
                strMandatoryAttribute = strMandatoryAttribute.replaceAll(" ", "");
                // TIGTK-16127,TIGTK-16126:20-07-2018:ENDS
                StringList slAttributeList = FrameworkUtil.split(strMandatoryAttribute, ",");

                // Get object attribute map
                Map mapAttribute = domObj.getAttributeMap(context);
                Iterator<Map.Entry> it = mapAttribute.entrySet().iterator();
                while (it.hasNext()) {
                    Entry e = it.next();
                    String strAttributeName = (String) e.getKey();
                    String symbolicAttrName = FrameworkUtil.getAliasForAdmin(context, "attribute", strAttributeName, true);
                    if (slAttributeList.contains(symbolicAttrName)) {
                        // TIGTK-9586 : START
                        if (strAttributeName.equals(TigerConstants.ATTRIBUTE_PSS_TRADE_NAME) || strAttributeName.equals(TigerConstants.ATTRIBUTE_PSS_SUPPLIER)
                                || strAttributeName.equals(TigerConstants.ATTRIBUTE_PSS_FAURECIASHORTLENGHTDESCRIPTION)) {
                            pss.cadbom.Material_mxJPO matrialJPO = new pss.cadbom.Material_mxJPO();
                            HashMap programMap = new HashMap();
                            programMap.put("objectId", strObjectId);
                            Boolean isVisibile = matrialJPO.isVisibleAttribute(context, JPO.packArgs(programMap));
                            if (!isVisibile)
                                continue;

                        }
                        // TIGTK-9586 : END
                        boolean isAttributeNotFilled = false;
                        String strAttrNameForDisplay = i18nNow.getAttributeI18NString(strAttributeName, context.getSession().getLanguage());
                        String strAttributeValue = (String) e.getValue();
                        if (strAttributeValue.equalsIgnoreCase(ATTR_VALUE_UNASSIGNED) || UIUtil.isNullOrEmpty(strAttributeValue)) {
                            isAttributeNotFilled = true;
                        } else {
                            // Check that default and current value of attribute is numeric or not
                            if (strAttributeValue.matches(".*[0-9].*")) {
                                double dAttributeValue;
                                try {
                                    dAttributeValue = Double.parseDouble(strAttributeValue);
                                } catch (Exception ex) {
                                    // no action to be taken just skip this attribute General Class
                                    continue;
                                }
                                double dAttributeFloorValue = Math.floor(dAttributeValue);
                                double dAttributeCeilValue = Math.ceil(dAttributeValue);
                                if (dAttributeFloorValue == 0.0 && dAttributeCeilValue == 0.0) {
                                    isAttributeNotFilled = true;
                                }
                            }
                        }
                        // Attribute list which are not filled
                        if (isAttributeNotFilled) {
                            slNonFilledMandatoryAttributeListForDisplay.addElement(strAttrNameForDisplay);
                        }
                    }
                }

                // Alert message
                if (slNonFilledMandatoryAttributeListForDisplay.size() > 0) {
                    iCheckResult = 1;
                    String strKey = "";
                    if (domObj.isKindOf(context, DomainConstants.TYPE_PART)) {
                        strKey = "PSS_emxEngineeringCentral.Alert.PSS_MandatoryAttributesNotFilled";
                    } else if (domObj.isKindOf(context, TigerConstants.TYPE_VPMREFERENCE)) {
                        strKey = "PSS_emxEngineeringCentral.Alert.PSS_MandatoryAttributesNotFilledForToolingPart";
                    } else {
                        strKey = "PSS_emxEngineeringCentral.Alert.PSS_MandatoryAttributesNotFilledForMaterial";
                    }

                    StringList slObjectSelects = new StringList();
                    slObjectSelects.addElement(DomainConstants.SELECT_TYPE);
                    slObjectSelects.addElement(DomainConstants.SELECT_NAME);
                    slObjectSelects.addElement(DomainConstants.SELECT_REVISION);
                    Map mObjectInfo = domObj.getInfo(context, slObjectSelects);

                    String strObjectType = (String) mObjectInfo.get(DomainConstants.SELECT_TYPE);
                    strObjectType = i18nNow.getTypeI18NString(strObjectType, context.getSession().getLanguage());
                    String strObjectName = (String) mObjectInfo.get(DomainConstants.SELECT_NAME);
                    String strObjectRevision = (String) mObjectInfo.get(DomainConstants.SELECT_REVISION);

                    String strAlertMessage = EngineeringUtil.i18nStringNow(context, strKey, context.getSession().getLanguage());
                    strAlertMessage = strAlertMessage.replace("$<type>", strObjectType);
                    strAlertMessage = strAlertMessage.replace("$<name>", strObjectName);
                    strAlertMessage = strAlertMessage.replace("$<revision>", strObjectRevision);
                    StringBuffer sbAlertMessange = new StringBuffer();
                    sbAlertMessange.append(strAlertMessage);
                    sbAlertMessange.append("\n");
                    sbAlertMessange.append(slNonFilledMandatoryAttributeListForDisplay.join("\n"));

                    MqlUtil.mqlCommand(context, "notice $1", sbAlertMessange.toString());
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("Error in checkMandatoryAttributesFilled: ", ex);
        }
        return iCheckResult;
    }

    // method end.
    // TIGTK-11260 : 10-01-2018 : END
    /**
     * This method to be called to promote entire BOM including connected CAD Objects. It will be executed after successful validation of entire BOM.
     * @param context
     * @param args
     * @return -- String
     * @throws Exception
     */

    // TIGTK-5788 :Rutuja Ekatpure :4/4/2017:Start
    public String freezeBOM(Context context, String[] args) throws Exception {
        String strPartObjectId = (String) JPO.unpackArgs(args);
        BusinessObject busObj = new BusinessObject(strPartObjectId);
        DomainObject domObj = new DomainObject(strPartObjectId);
        String strCurrent = domObj.getInfo(context, DomainConstants.SELECT_CURRENT);
        String strPolicy = (String) domObj.getInfo(context, DomainConstants.SELECT_POLICY);
        boolean isDoublePromote = false;
        try {
            busObj.open(context);
            if (TigerConstants.POLICY_PSS_CADOBJECT.equals(strPolicy) && TigerConstants.STATE_CAD_REVIEW.equals(strCurrent)) {
                busObj.promote(context);
                isDoublePromote = true;
            }
            busObj.promote(context);
            busObj.close(context);
        } catch (Exception e) {
            return "Fail|" + e.getMessage();
        }
        if (isDoublePromote)
            return "DoublePromote";
        else
            return "Pass";
    }
    // TIGTK-5788 :Rutuja Ekatpure :4/4/2017:End

    // method end.

    public boolean displayFieldNoOfQuantity(Context context, String args[]) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String strCreateMode = (String) programMap.get("createMode");
        if ("EBOM".equalsIgnoreCase(strCreateMode)) {
            return true;
        }
        return false;
    }

    public MapList getSelectedPartsForAddExisting(Context context, String[] args) throws Exception {
        MapList formFieldList = new MapList();
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap requestMap = (HashMap) programMap.get("requestMap");
        String strSelectedItems = (String) requestMap.get("selectedParts");
        StringTokenizer strToken = new StringTokenizer(strSelectedItems, "|");
        int i = 0;
        String type[] = new String[strToken.countTokens()];
        // int k = strToken.countTokens();
        while (strToken.hasMoreTokens()) {

            type[i] = strToken.nextToken();
            DomainObject domPart = new DomainObject(type[i]);
            String strPartName = domPart.getInfo(context, DomainConstants.SELECT_NAME);
            String strObjectId = domPart.getInfo(context, DomainConstants.SELECT_ID);

            Map formFieldSetting = new HashMap();
            formFieldSetting.put("Editable", "true");
            formFieldSetting.put("Registered Suite", "EngineeringCentral");
            formFieldSetting.put("Input Type", "textbox");
            formFieldSetting.put("Default", "1");
            formFieldSetting.put("Required", "true");
            Map formFieldMap = new HashMap();
            formFieldMap.put("label", strPartName);
            formFieldMap.put("name", strObjectId);
            formFieldMap.put("settings", formFieldSetting);
            formFieldList.add(formFieldMap);
            i++;

        }
        return formFieldList;

    }

    /**
     * This method will get the EBOM List of Part Instances and pass it to expandEBOMInQuantityDisplayMode method to get consolidated EBOM List.
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param args
     *            holds objectId.
     * @return MapList.
     * @throws Exception
     *             If the operation fails.
     * @author Suchit Gangurde
     * @date 26-02-2016
     */

    public MapList getEBOMInQuantityDisplayMode(Context context, String[] args) throws Exception {
        StringList slObjectSelect = new StringList();
        slObjectSelect.add(SELECT_ID);

        StringList slRelSelect = new StringList();
        slRelSelect.add(SELECT_RELATIONSHIP_ID);
        slRelSelect.add(SELECT_ATTRIBUTE_FIND_NUMBER);
        slRelSelect.add(SELECT_ATTRIBUTE_QUANTITY);
        slRelSelect.add(SELECT_FROM_ID);
        slRelSelect.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_CUSTOMEFFECTIVITYEXPRESSION + "]"); // Effectivity
        MapList mlEbomList = getEBOMsWithRelSelectablesSB(context, args);
        // TIGTK-4111 - SteepGraph - 01-02-2017 - START
        mlEbomList.sortStructure(context, "relationship," + DomainConstants.SELECT_ATTRIBUTE_FIND_NUMBER + "", ",", "string,integer", ",");
        // TIGTK-4111 - SteepGraph - 01-02-2017 - END
        MapList mlResultList = expandEBOMInQuantityDisplayMode(context, mlEbomList);

        return mlResultList;
    }

    /**
     * This method will get the provide a consolidated view of EBOMs based on original EBOM list of Part instances
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param MapList
     *            holds MapList.
     * @return MapList.
     * @throws Exception
     *             If the operation fails.
     * @author Suchit Gangurde
     * @date 26-02-2016
     */

    public MapList expandEBOMInQuantityDisplayMode(Context context, MapList mlEbomList) throws Exception {
        // TIGTK-4481 - START
        MapList mlConsolidatedEbomList = new MapList();
        Map<String, String> mUniqueInstanceMap = new HashMap<String, String>();
        StringList slLevelRelId = new StringList();

        Iterator ebomListItr = mlEbomList.iterator();

        try {
            while (ebomListItr.hasNext()) {
                Map mtempMap = (Map) ebomListItr.next();

                String strObjId = (String) mtempMap.get(DomainConstants.SELECT_ID);
                String strLevel = (String) mtempMap.get(DomainConstants.SELECT_LEVEL);
                String strFromId = (String) mtempMap.get(DomainConstants.SELECT_FROM_ID);
                String strRelId = (String) mtempMap.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                String strEffectivityExp = (String) mtempMap.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_CUSTOMEFFECTIVITYEXPRESSION + "]");
                String strQuantity = (String) mtempMap.get(DomainConstants.SELECT_ATTRIBUTE_QUANTITY);

                double dblQuantity = 0d;
                String strConnectionIds = strRelId;

                String strObjIdLevel = strObjId + ":" + strLevel + ":" + strFromId + ":" + strEffectivityExp; // Key combining
                // object ID,
                // Level,
                // Relationship
                // From ID and
                // Effectivity
                // Expression
                String strLevelRelIdExp = strLevel + ":" + strRelId; // Key
                // combining
                // level
                // and
                // Relationship
                // ID

                // Check for Quantity value to ensure that null is not obtained
                if (UIUtil.isNotNullAndNotEmpty(strQuantity)) {
                    dblQuantity = Double.parseDouble((String) mtempMap.get(DomainConstants.SELECT_ATTRIBUTE_QUANTITY));
                }

                // If mUniqueInstanceMap contains the key strObjIdLevel and if
                // slLevelRelId contains the key strLevelRelIdExp, only Quantity
                // value is appended to the key strObjIdLevel,
                // else map is added to MapList, and update map with
                // strObjIdLevel and dblQuantity

                if (mUniqueInstanceMap.containsKey(strObjIdLevel)) {
                    if (slLevelRelId.contains(strLevelRelIdExp)) {
                        continue;
                    }

                    dblQuantity += Double.parseDouble((String) mUniqueInstanceMap.get(strObjIdLevel));
                    strConnectionIds = strConnectionIds + "|" + (String) mUniqueInstanceMap.get(strObjIdLevel + "_Relationships");

                } else {
                    mlConsolidatedEbomList.add(mtempMap);
                }
                slLevelRelId.add(strLevelRelIdExp);
                mUniqueInstanceMap.put(strObjIdLevel, String.valueOf(dblQuantity));
                mUniqueInstanceMap.put(strObjIdLevel + "_Relationships", strConnectionIds);

            }
        } catch (Exception e) {
            logger.error("Error in expandEBOMInQuantityDisplayMode: ", e);
        }

        ebomListItr = mlConsolidatedEbomList.iterator();

        // Iterate over MapList and finally return the consolidated MapList for
        // display in EBOM In Quantity tab
        try {
            while (ebomListItr.hasNext()) {
                Map mtempMap = (Map) ebomListItr.next();

                String strObjId = (String) mtempMap.get(DomainConstants.SELECT_ID);
                String strLevel = (String) mtempMap.get(DomainConstants.SELECT_LEVEL);
                String strFromId = (String) mtempMap.get(DomainConstants.SELECT_FROM_ID);
                String strEffectivityExp = (String) mtempMap.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_CUSTOMEFFECTIVITYEXPRESSION + "]");
                String strObjIdLevel = strObjId + ":" + strLevel + ":" + strFromId + ":" + strEffectivityExp;

                double dblquantity = Double.parseDouble((String) mUniqueInstanceMap.get(strObjIdLevel));
                mtempMap.put(DomainConstants.SELECT_ATTRIBUTE_QUANTITY, String.valueOf(dblquantity));
                String strconnectionIds = (String) mUniqueInstanceMap.get(strObjIdLevel + "_Relationships");
                mtempMap.put("Relationships", strconnectionIds);
            }
        } catch (Exception e) {
            logger.error("Error in expandEBOMInQuantityDisplayMode: ", e);
        }
        return mlConsolidatedEbomList;
        // TIGTK-4481 - END
    }

    public Vector getConsolidatedQuantity(Context context, String[] args) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        MapList mlObjectList = (MapList) programMap.get("objectList");
        Vector ConsolidatedQuantity = new Vector(mlObjectList.size());
        Iterator objListitr = mlObjectList.iterator();
        try {
            while (objListitr.hasNext()) {
                Map m = (Map) objListitr.next();
                String strConsQty = (String) m.get(DomainRelationship.SELECT_ATTRIBUTE_QUANTITY);
                // TIGTK-4481 - START
                String Relationships = (String) m.get("Relationships");
                StringBuilder sb = new StringBuilder();

                sb.append("<div id='").append((String) m.get(DomainRelationship.SELECT_ID)).append("'>");
                sb.append(strConsQty == null ? 1.0 : strConsQty);
                sb.append("<input type='hidden' id='associatedRelationships' name='associatedRelationships' value='").append(Relationships).append("'/>");
                sb.append("</div>");
                ConsolidatedQuantity.addElement(sb.toString());
                // TIGTK-4481 - END
            }
        } catch (Exception e) {
            logger.error("Error in getConsolidatedQuantity: ", e);
        }
        return ConsolidatedQuantity;
    }

    /**
     * Method to get the Connected Product Configuration as column
     * @name getProductConfigurationColumns
     * @param context
     * @param args
     *            programMap-->requestMap-->parentOID
     * @return MapList - Contains the List of Production Configuration Dynamic Columns
     * @throws Exception
     * @author Chintan DADHANIA
     * @date 09-March-2016
     */

    @SuppressWarnings("unchecked")
    public List<?> getProductConfigurationColumns(Context context, String[] args) throws Exception {
        HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
        HashMap<?, ?> requestMap = (HashMap<?, ?>) programMap.get("requestMap");
        // Define column MapList to return.
        MapList columnMapList = new MapList();

        // TIGTK-9454:24-07-2018:START
        MapList columnMap = (MapList) programMap.get("columnMap");
        Map mpSettingsMap = (Map) columnMap.get(9);

        Map mpSettings = (Map) mpSettingsMap.get("settings");
        String strWidth = (String) mpSettings.get("Width");
        if (UIUtil.isNotNullAndNotEmpty(strWidth)) {
            int intWidth = Integer.parseInt(strWidth);
            intWidth = (int) (intWidth * 1.5);
            strWidth = Integer.toString(intWidth);
        }
        // TIGTK-9454:24-07-2018:END

        // fetch parent OID
        String strParentOID = (String) requestMap.get("parentOID");

        // Product Not Connected Error Message
        String STR_PRODUCT_IS_NOT_CONNECTED_TO_EBOM = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(),
                "PSS_emxEngineeringCentral.Error.ProductIsNotEBOMMessage");

        // object select list
        StringList slSelectFromObject = new StringList(2);
        slSelectFromObject.addElement(DomainConstants.SELECT_NAME);
        slSelectFromObject.addElement(DomainConstants.SELECT_ID);
        // TIGTK-8019:Rutuja Ekatpure:26/5/2017:Start
        slSelectFromObject.addElement("attribute[" + TigerConstants.ATTRIBUTE_MARKETINGNAME + "].value");

        DomainObject domParentPartObject = DomainObject.newInstance(context, strParentOID);

        MapList mlProduct = (MapList) domParentPartObject.getRelatedObjects(context, RELATIONSHIP_GBOM, // relationship
                "*", // types to fetch from other end
                true, // getTO
                false, // getFrom
                0, // recursionTo level
                slSelectFromObject, // object selects
                new StringList(), // relationship selects
                null, // object where
                null, // relationship where
                0, // limit
                null, // post rel pattern
                null, // post type pattern
                null); // post patterns
        // if no product is connected show pop up message
        // "The system does not find a product connected to this eBOM.Please connect one and try again."

        if (mlProduct.isEmpty()) {
            emxContextUtil_mxJPO.mqlNotice(context, STR_PRODUCT_IS_NOT_CONNECTED_TO_EBOM);
            // throw new Exception(STR_PRODUCT_IS_NOT_CONNECTED_TO_EBOM);
        } else {
            Iterator<?> itr = mlProduct.iterator();

            StringList selectStmtsProdConfig = new StringList(3);
            selectStmtsProdConfig.addElement(DomainConstants.SELECT_NAME);
            selectStmtsProdConfig.addElement(DomainConstants.SELECT_ID);
            selectStmtsProdConfig.addElement("attribute[" + TigerConstants.ATTRIBUTE_MARKETINGNAME + "]");
            // Declare Variables
            String strProductConfigName, strProductName, strType, strProductId, strProductConfigId, strMarketingName;
            Map<?, ?> mProductConfig;
            HashMap<String, Object> colMap;
            HashMap<String, String> settingsMap;
            String strProductMarketingName = "";
            while (itr.hasNext()) {
                Map<?, ?> mpProduct = (Map<?, ?>) itr.next();
                strProductName = (String) mpProduct.get(DomainConstants.SELECT_NAME);
                strType = (String) mpProduct.get(DomainConstants.SELECT_TYPE);
                strProductId = (String) mpProduct.get(DomainConstants.SELECT_ID);
                strProductMarketingName = (String) mpProduct.get("attribute[" + TigerConstants.ATTRIBUTE_MARKETINGNAME + "].value");
                // get product configurations connected to a product
                // there are 2 for now
                DomainObject domProductObject = DomainObject.newInstance(context, strProductId);

                MapList mlProductConfiguration = (MapList) domProductObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PRODUCT_CONFIGURATION, // relationship
                        "*", // types to fetch from other end
                        false, // getTO
                        true, // getFrom
                        0, // recursionTo level
                        selectStmtsProdConfig, // object selects
                        new StringList(), // relationship selects
                        null, // object where
                        null, // relationship where
                        0, // limit
                        null, // post rel pattern
                        null, // post type pattern
                        null); // post patterns

                Iterator<?> itr1 = mlProductConfiguration.iterator();

                while (itr1.hasNext()) {
                    mProductConfig = (Map<?, ?>) itr1.next();
                    strProductConfigId = (String) mProductConfig.get("id");
                    strProductConfigName = (String) mProductConfig.get("name");
                    strMarketingName = (String) mProductConfig.get("attribute[" + TigerConstants.ATTRIBUTE_MARKETINGNAME + "]");

                    // create a column map to be returned.
                    colMap = new HashMap<String, Object>();
                    settingsMap = new HashMap<String, String>();

                    // Set information of Column Settings in settingsMap
                    settingsMap.put("Column Type", "programHTMLOutput");
                    settingsMap.put("program", "PSS_emxPart");
                    settingsMap.put("function", "getPartPresenceDataInProductConfiguration");
                    settingsMap.put("Editable", "false");
                    settingsMap.put("Sortable", "true");
                    settingsMap.put("Group Header", "Product Configuration");
                    settingsMap.put("Style Column", "PSSPositionCenter");
                    settingsMap.put("Registered Suite", "EngineeringCentral");
                    settingsMap.put("Export", "true");
                    // TIGTK-9454:24-07-2018:START
                    if (UIUtil.isNotNullAndNotEmpty(strWidth)) {
                        settingsMap.put("Width", strWidth);
                    }
                    // TIGTK-9454:24-07-2018:END
                    // set column information
                    colMap.put("name", strProductConfigId);
                    // DIVERSITY:PHASE2.0 : TIGTK-9186 : PSE : 02-08-2017 : START
                    StringBuffer sbMarketingName = new StringBuffer();
                    sbMarketingName.append(strProductMarketingName);
                    sbMarketingName.append("<br><center>");
                    sbMarketingName.append(strMarketingName);
                    sbMarketingName.append("</center>");
                    colMap.put("label", sbMarketingName.toString());
                    // DIVERSITY:PHASE2.0 : TIGTK-9186 : PSE : 02-08-2017 : END
                    colMap.put("settings", settingsMap);
                    columnMapList.add(colMap);
                }
            }
            return columnMapList;
            // TIGTK-8019:Rutuja Ekatpure:26/5/2017:End
        }
        return columnMapList;
    }

    /**
     * Method to get the Product Configuration Column values
     * @name getProductConfigurationColumns
     * @param context
     * @param args
     *            programMap-->requestMap-->parentOID
     * @return MapList - Contains the List of Production Configuration Dynamic Columns
     * @throws Exception
     * @author Chintan DADHANIA
     * @date 10-March-2016
     */

    // data extractor for dynamic column
    public StringList getPartPresenceDataInProductConfiguration(Context context, String[] args) throws Exception {

        HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
        HashMap<?, ?> mapParamList = (HashMap<?, ?>) programMap.get("paramList");
        // Get Parent Part ID
        String strParentOID = (String) mapParamList.get("parentOID");

        // Get Column (Product Configuration Id)
        HashMap<?, ?> columnMap = (HashMap<?, ?>) programMap.get("columnMap");
        String strProductConfigId = (String) columnMap.get("name");

        StringList slReturnList = new StringList();
        // Create domain object of Product Configuration
        DomainObject domProductConfigObject = DomainObject.newInstance(context, strProductConfigId);

        // Effectivity Compiled Form
        String strCompiledForm = domProductConfigObject.getInfo(context, "attribute[" + TigerConstants.ATTRIBUTE_FILTERCOMPILEDFORM + "]");

        // Create domainObject of Parent Part Id
        DomainObject domPartObject = DomainObject.newInstance(context, strParentOID);

        // get EBOM
        MapList objectMapList = (MapList) programMap.get("objectList");
        // Part Attribute Selection list
        StringList slObjectSelect = new StringList(1);
        slObjectSelect.add(DomainConstants.SELECT_ID);
        slObjectSelect.add(DomainConstants.SELECT_NAME);

        // EBOM Relationship Attribute Selection list
        StringList slRelSelect = new StringList(2);
        slRelSelect.add("attribute[" + TigerConstants.ATTRIBUTE_EFFECTIVITYCOMPILEDFORM + "]");
        slRelSelect.add(DomainRelationship.SELECT_ID);
        slRelSelect.add(DomainRelationship.SELECT_NAME);

        MapList filterEBOMList = domPartObject.getRelatedObjects(context, RELATIONSHIP_EBOM, // relationshipPattern
                TYPE_PART, // typePattern
                slObjectSelect, // objectSelects
                slRelSelect, // relationshipSelects
                false, // getTo
                true, // getFrom
                (short) 0, // recurseToLevel
                "", // objectWhereClause
                "", // relationshipWhereClause
                (short) 0, // limit
                true, // checkHidden
                false, // preventDuplicates
                (short) 0, // pageSize
                null, // includeType
                null, // includeRelationship
                null, // includeMap
                "", // relKeyPrefix
                strCompiledForm, // filterExpression
                (short) 0); // filterFlag
        // Temporary map
        Map<?, ?> mapTemp;
        Iterator<?> itr = filterEBOMList.iterator();
        String strEffectivityValue;

        // Local StringList to store all those Part (EBOM Connection ID) are
        // connected to Current Product Configuration and have effectivity value
        // on connection.

        StringList slFilteredBOM = new StringList();
        String strConnectionId;

        while (itr.hasNext()) {
            mapTemp = (Map<?, ?>) itr.next();
            strEffectivityValue = (String) mapTemp.get("attribute[" + TigerConstants.ATTRIBUTE_EFFECTIVITYCOMPILEDFORM + "]");
            // Modified for TIGTK-3345 by Priyanka Salunke on Date : 07/10/2016
            slFilteredBOM.add((String) mapTemp.get(DomainRelationship.SELECT_ID));
        }

        itr = objectMapList.iterator();
        while (itr.hasNext()) {
            mapTemp = (Map<?, ?>) itr.next();
            strConnectionId = (String) mapTemp.get(DomainRelationship.SELECT_ID);
            // If Current connection found on StringList (slFilteredBOM) the
            // return "X" or blank("")
            if (slFilteredBOM.contains(strConnectionId)) {
                slReturnList.add("X");
            } else {
                slReturnList.add("");
            }
        }
        return slReturnList;
    }

    /**
     * This Function will connect selected part as number of times specified in the web form for REPEAT PART Functionality with EBOM relationship.
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param args
     *            holds objectList.
     * @throws Exception
     *             If the operation fails.
     */

    public void repeatSelectedPartInEBOM(Context context, String[] args) throws Exception {

        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap requestMap = (HashMap) programMap.get("requestMap");

        String selectedParentPartId = (String) requestMap.get("selectedParentPartId");
        String selectedPartId = (String) requestMap.get("selectedParts");
        String selectedRelId = (String) requestMap.get("selectedRelId");
        String RepeatPartCount = (String) requestMap.get(selectedPartId);
        String duplicateEffectivity = (String) requestMap.get("DuplicateEffectivity");
        // TIGTK-11654:RE:12/15/2017:Start
        String duplicateQuntity = (String) requestMap.get("DuplicateQuantity");
        // TIGTK-11654:RE:12/15/2017:End
        String strEndEffectivityDate = DomainRelationship.getAttributeValue(context, selectedRelId, DomainConstants.ATTRIBUTE_END_EFFECTIVITY_DATE);
        String strStartEffectivityDate = DomainRelationship.getAttributeValue(context, selectedRelId, DomainConstants.ATTRIBUTE_START_EFFECTIVITY_DATE);
        String strQuantity = DomainRelationship.getAttributeValue(context, selectedRelId, DomainConstants.ATTRIBUTE_QUANTITY);
        String strUsage = DomainRelationship.getAttributeValue(context, selectedRelId, DomainConstants.ATTRIBUTE_USAGE);
        String strReferenceDesignator = DomainRelationship.getAttributeValue(context, selectedRelId, DomainConstants.ATTRIBUTE_REFERENCE_DESIGNATOR);
        String strComponentLocation = DomainRelationship.getAttributeValue(context, selectedRelId, DomainConstants.ATTRIBUTE_COMPONENT_LOCATION);

        String strEffectivityOrderedImpactingCriteria = DomainRelationship.getAttributeValue(context, selectedRelId, TigerConstants.ATTRIBUTE_EFFECTIVITYORDEREDIMPACTINGCRITERIA);
        String strEffectivityOrderedCriteriaDictionary = DomainRelationship.getAttributeValue(context, selectedRelId, TigerConstants.ATTRIBUTE_EFFECTIVITYORDEREDCRITERIADICTIONARY);
        String strEffectivityExpression = DomainRelationship.getAttributeValue(context, selectedRelId, TigerConstants.ATTRIBUTE_EFFECTIVITYEXPRESSION);
        String strEffectivityCompiledForm = DomainRelationship.getAttributeValue(context, selectedRelId, TigerConstants.ATTRIBUTE_EFFECTIVITYCOMPILEDFORM);
        String strEffectivityVariableIndexes = DomainRelationship.getAttributeValue(context, selectedRelId, TigerConstants.ATTRIBUTE_EFFECTIVITY_VARIABLE_INDEXES);
        String strEffectivityExpressionBinary = DomainRelationship.getAttributeValue(context, selectedRelId, TigerConstants.ATTRIBUTE_EFFECTIVITYEXPRESSIONBINARY);
        String strEffectivityProposedExpression = DomainRelationship.getAttributeValue(context, selectedRelId, TigerConstants.ATTRIBUTE_EFFECTIVITYPROPOSEDEXPRESSION);
        String strEffectivityOrderedCriteria = DomainRelationship.getAttributeValue(context, selectedRelId, TigerConstants.ATTRIBUTE_EFFECTIVITYORDEREDCRITERIA);
        String strSimplerEffectivityExpression = DomainRelationship.getAttributeValue(context, selectedRelId, TigerConstants.ATTRIBUTE_PSS_SIMPLEREFFECTIVITYEXPRESSION);
        String strCustomEffectivityExpression = DomainRelationship.getAttributeValue(context, selectedRelId, TigerConstants.ATTRIBUTE_PSS_CUSTOMEFFECTIVITYEXPRESSION);

        try {
            StringList selectStmts = new StringList(1);
            selectStmts.addElement(DomainConstants.SELECT_ID);
            StringList selectRelStmts = new StringList(3);
            selectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            selectRelStmts.addElement(DomainConstants.SELECT_ATTRIBUTE_FIND_NUMBER);

            DomainObject selectedPartObj = DomainObject.newInstance(context, selectedPartId);
            DomainObject domObj = DomainObject.newInstance(context, selectedParentPartId);
            MapList mpConnectedPartList = domObj.getRelatedObjects(context, DomainConstants.RELATIONSHIP_EBOM, DomainConstants.TYPE_PART, // object
                    // pattern
                    selectStmts, // object selects
                    selectRelStmts, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 0, // recursion level
                    null, // object where clause
                    null, 0); // relationship where clause

            mpConnectedPartList.sort("attribute[Find Number]", "ascending", "integer");

            Iterator<?> busIterator = mpConnectedPartList.iterator();
            String strHigestFindNumber = "";
            while (busIterator.hasNext()) {
                Map<?, ?> busMap = (Map<?, ?>) busIterator.next();
                strHigestFindNumber = (String) busMap.get("attribute[Find Number]");
            }

            HashMap<String, String> RelAttributesMap;
            int nextFindNumber = Integer.parseInt(strHigestFindNumber);
            // PCM TIGTK-7058 : PSE : 30-05-2017 : START
            DecimalFormat numFormatFour = new DecimalFormat("0000");
            // PCM TIGTK-7058 : PSE : 30-05-2017 : END
            for (int i = 0; i < Integer.parseInt(RepeatPartCount); i++) {
                // PCM TIGTK-7058 : PSE : 30-05-2017 : START
                nextFindNumber = nextFindNumber + 10;
                // PCM TIGTK-7058 : PSE : 30-05-2017 : END
                DomainRelationship domRelation = DomainRelationship.connect(context, domObj, DomainConstants.RELATIONSHIP_EBOM, selectedPartObj);
                RelAttributesMap = new HashMap<String, String>();
                if ("on".equals(duplicateEffectivity) && !"null".equals(duplicateEffectivity) && !"".equals(duplicateEffectivity)) {
                    String strModRelMQL = "mod connection " + domRelation + " add interface \"Effectivity Framework\"";
                    MqlUtil.mqlCommand(context, strModRelMQL);

                    // PCM TIGTK-7058 : PSE : 30-05-2017 : START
                    // Find Bug TIGTK-8360 : PSE : 02-06-2017 : START
                    RelAttributesMap.put(DomainConstants.ATTRIBUTE_FIND_NUMBER, numFormatFour.format(Integer.valueOf(nextFindNumber)));
                    // Find Bug TIGTK-8360 : PSE : 02-06-2017 : END
                    // PCM TIGTK-7058 : PSE : 30-05-2017 : END
                    RelAttributesMap.put(DomainConstants.ATTRIBUTE_END_EFFECTIVITY_DATE, strEndEffectivityDate);
                    RelAttributesMap.put(DomainConstants.ATTRIBUTE_START_EFFECTIVITY_DATE, strStartEffectivityDate);
                    RelAttributesMap.put(DomainConstants.ATTRIBUTE_USAGE, strUsage);
                    RelAttributesMap.put(DomainConstants.ATTRIBUTE_REFERENCE_DESIGNATOR, strReferenceDesignator);
                    RelAttributesMap.put(DomainConstants.ATTRIBUTE_COMPONENT_LOCATION, strComponentLocation);
                    RelAttributesMap.put(TigerConstants.ATTRIBUTE_EFFECTIVITYORDEREDIMPACTINGCRITERIA, strEffectivityOrderedImpactingCriteria);
                    RelAttributesMap.put(TigerConstants.ATTRIBUTE_EFFECTIVITYORDEREDCRITERIADICTIONARY, strEffectivityOrderedCriteriaDictionary);
                    RelAttributesMap.put(TigerConstants.ATTRIBUTE_EFFECTIVITYEXPRESSION, strEffectivityExpression);
                    RelAttributesMap.put(TigerConstants.ATTRIBUTE_EFFECTIVITYCOMPILEDFORM, strEffectivityCompiledForm);
                    RelAttributesMap.put(TigerConstants.ATTRIBUTE_EFFECTIVITY_VARIABLE_INDEXES, strEffectivityVariableIndexes);
                    RelAttributesMap.put(TigerConstants.ATTRIBUTE_EFFECTIVITYEXPRESSIONBINARY, strEffectivityExpressionBinary);
                    RelAttributesMap.put(TigerConstants.ATTRIBUTE_EFFECTIVITYPROPOSEDEXPRESSION, strEffectivityProposedExpression);
                    RelAttributesMap.put(TigerConstants.ATTRIBUTE_EFFECTIVITYORDEREDCRITERIA, strEffectivityOrderedCriteria);
                    RelAttributesMap.put(TigerConstants.ATTRIBUTE_PSS_SIMPLEREFFECTIVITYEXPRESSION, strSimplerEffectivityExpression);
                    RelAttributesMap.put(TigerConstants.ATTRIBUTE_PSS_CUSTOMEFFECTIVITYEXPRESSION, strCustomEffectivityExpression);
                } else {

                    // PCM TIGTK-7058 : PSE : 30-05-2017 : START
                    // Find Bug TIGTK-8360 : PSE : 02-06-2017 : START
                    RelAttributesMap.put(DomainConstants.ATTRIBUTE_FIND_NUMBER, numFormatFour.format(Integer.valueOf(nextFindNumber)));
                    // Find Bug TIGTK-8360 : PSE : 02-06-2017 : END
                    // PCM TIGTK-7058 : PSE : 30-05-2017 : END
                }
                // TIGTK-11654:RE:12/15/2017:Start
                if ("on".equals(duplicateQuntity) && !"null".equals(duplicateQuntity) && !"".equals(duplicateQuntity)) {
                    RelAttributesMap.put(DomainConstants.ATTRIBUTE_QUANTITY, strQuantity);
                }
                // TIGTK-11654:RE:12/15/2017:End
                domRelation.setAttributeValues(context, RelAttributesMap);
            }
        } // Fix for FindBugs issue RuntimeException capture: Suchit Gangurde: 28 Feb 2017: START
        catch (RuntimeException e) {
            // TIGTK-5405 - 03-04-2017 - VP - START
            logger.error("Error in repeatSelectedPartInEBOM: ", e);
            // TIGTK-5405 - 03-04-2017 - VP - END
            throw e;
        } // Fix for FindBugs issue RuntimeException capture: Suchit Gangurde: 28 Feb 2017: END
        catch (Exception e) {
            logger.error("Error in repeatSelectedPartInEBOM: ", e);
        }

    }

    /**
     * This Function will return a list of Product Configurations associated with Products which are related to that particular Part.
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param args
     *            holds objectList.
     * @throws Exception
     *             If the operation fails.
     */

    public MapList getProductConfigurationsListForEBOM(Context context, String[] args) throws Exception {

        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String strpartId = (String) programMap.get("objectId");
        // TIGTK-3063 : 13/09/2016 : START

        Pattern relPattern = new Pattern(RELATIONSHIP_GBOM);
        relPattern.addPattern(RELATIONSHIP_EBOM);

        DomainObject selectedPartObj = DomainObject.newInstance(context, strpartId);

        StringList selectStmts = new StringList(1);
        selectStmts.addElement(DomainConstants.SELECT_ID);

        StringList slUniqueIds = new StringList();
        MapList mpResultList = new MapList();
        try {

            Pattern relPostPattern = new Pattern(RELATIONSHIP_GBOM);
            // Added for TIGTK-3724 by Priyanka Salunke on Date:02/12/2016 : START
            // Added prevent duplicate true to resolve issue
            MapList mpConnectedProductList = selectedPartObj.getRelatedObjects(context, relPattern.getPattern(), // relationship pattern
                    "*", // object pattern
                    selectStmts, // object selects
                    DomainConstants.EMPTY_STRINGLIST, // relationship selects
                    true, // to direction
                    false, // from direction
                    (short) 0, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 0, // pageSize
                    null, relPostPattern, null, null, null);

            // // Added for TIGTK-3724 by Priyanka Salunke on Date:02/12/2016 : END

            if (mpConnectedProductList.size() == 0) {

                String test = "There is no Product Configuration to be displayed because the top levelpart" + " " + selectedPartObj.getInfo(context, DomainConstants.SELECT_NAME) + " "
                        + "is not connected to a Product";

                emxContextUtil_mxJPO.mqlNotice(context, test);
            } else {
                // Made changes for TIGTK-3724 by Priyanka Salunke on Date : 05-Dec-2016 : START
                // Made changes to prevent duplication of Product and Product Configuration
                int intCount = mpConnectedProductList.size();
                StringList slUniqueProductIds = new StringList();
                for (int itr = 0; itr < intCount; itr++) {
                    if (!slUniqueProductIds.contains((String) ((Map) mpConnectedProductList.get(itr)).get(DomainConstants.SELECT_ID))) {
                        slUniqueProductIds.add((String) ((Map) mpConnectedProductList.get(itr)).get(DomainConstants.SELECT_ID));
                    }
                }
                int intUniqueIdListSize = slUniqueProductIds.size();
                String strObjects[] = new String[intUniqueIdListSize];
                for (int i = 0; i < intUniqueIdListSize; i++) {

                    strObjects[i] = (String) slUniqueProductIds.get(i);
                }
                // Made changes for TIGTK-3724 by Priyanka Salunke on Date : 05-Dec-2016 : END
                // TIGTK-3161 : 22/09/2016 : START
                DomainConstants.MULTI_VALUE_LIST.add("from[" + TigerConstants.RELATIONSHIP_PRODUCT_CONFIGURATION + "].to.id");
                DomainConstants.MULTI_VALUE_LIST.add("from[" + TigerConstants.RELATIONSHIP_PRODUCT_CONFIGURATION + "].id");
                DomainConstants.MULTI_VALUE_LIST.add("from[" + TigerConstants.RELATIONSHIP_PRODUCT_CONFIGURATION + "].to.type");
                DomainConstants.MULTI_VALUE_LIST.add("from[" + TigerConstants.RELATIONSHIP_PRODUCT_CONFIGURATION + "].name");

                StringList slSelect = new StringList();
                slSelect.add("from[" + TigerConstants.RELATIONSHIP_PRODUCT_CONFIGURATION + "].to.id");
                slSelect.add("from[" + TigerConstants.RELATIONSHIP_PRODUCT_CONFIGURATION + "].id");
                slSelect.add("from[" + TigerConstants.RELATIONSHIP_PRODUCT_CONFIGURATION + "].to.type");
                slSelect.add("from[" + TigerConstants.RELATIONSHIP_PRODUCT_CONFIGURATION + "].name");
                mpConnectedProductList = DomainObject.getInfo(context, strObjects, slSelect);

                DomainConstants.MULTI_VALUE_LIST.remove("from[" + TigerConstants.RELATIONSHIP_PRODUCT_CONFIGURATION + "].to.id");
                DomainConstants.MULTI_VALUE_LIST.remove("from[" + TigerConstants.RELATIONSHIP_PRODUCT_CONFIGURATION + "].id");
                DomainConstants.MULTI_VALUE_LIST.remove("from[" + TigerConstants.RELATIONSHIP_PRODUCT_CONFIGURATION + "].to.type");
                DomainConstants.MULTI_VALUE_LIST.remove("from[" + TigerConstants.RELATIONSHIP_PRODUCT_CONFIGURATION + "].name");
                intCount = mpConnectedProductList.size();

                int intPCCount = 0;
                HashMap tempMap = null;
                for (int i = 0; i < intCount; i++) {
                    StringList tempIdSl = (StringList) ((Map) mpConnectedProductList.get(i)).get("from[" + TigerConstants.RELATIONSHIP_PRODUCT_CONFIGURATION + "].to.id");
                    StringList tempRelIdSl = (StringList) ((Map) mpConnectedProductList.get(i)).get("from[" + TigerConstants.RELATIONSHIP_PRODUCT_CONFIGURATION + "].id");
                    StringList tempRelNameSl = (StringList) ((Map) mpConnectedProductList.get(i)).get("from[" + TigerConstants.RELATIONSHIP_PRODUCT_CONFIGURATION + "].name");
                    StringList tempTypeSl = (StringList) ((Map) mpConnectedProductList.get(i)).get("from[" + TigerConstants.RELATIONSHIP_PRODUCT_CONFIGURATION + "].to.type");
                    intPCCount = tempIdSl.size();
                    for (int j = 0; j < intPCCount; j++) {
                        String objId = (String) tempIdSl.get(j);
                        if (!slUniqueIds.contains(objId)) {
                            tempMap = new HashMap();
                            tempMap.put(DomainConstants.SELECT_ID, (String) tempIdSl.get(j));
                            tempMap.put(DomainConstants.SELECT_RELATIONSHIP_ID, (String) tempRelIdSl.get(j));
                            tempMap.put(DomainConstants.SELECT_RELATIONSHIP_NAME, (String) tempRelNameSl.get(j));
                            tempMap.put(DomainConstants.SELECT_TYPE, (String) tempTypeSl.get(j));
                            mpResultList.add(tempMap);
                        } else {
                            slUniqueIds.add(objId);
                        }
                    }
                }
                // TIGTK-3161 : 22/09/2016 : END
                if (mpResultList.size() == 0) {
                    String strAbsenceOfPC = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(), "PSS_emxEngineeringCentral.Alert.AbsenceOfPC");
                    emxContextUtil_mxJPO.mqlNotice(context, strAbsenceOfPC);
                }
            }
            // TIGTK-3063 : 13/09/2016 : END
        } catch (Exception e) {
            logger.error("Error in getProductConfigurationsListForEBOM: ", e);
        }
        return mpResultList;
    }

    /*
     * Retrieves the EBOM data. Modified for expand issue for latest and latest complete.
     * 
     * @param context the ENOVIA <code>Context</code> object
     * 
     * @param args[] programMap
     * 
     * @throws Exception if error encountered while carrying out the request
     */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getEBOMsWithRelSelectablesSB(Context context, String[] args) throws Exception {
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);

        String sExpandLevels = getStringValue(paramMap, "emxExpandFilter");
        String selectedFilterValue = getStringValue(paramMap, "ENCBOMRevisionCustomFilter");
        MapList retList = expandEBOM(context, paramMap);

        if ("Latest".equals(selectedFilterValue) || "Latest Complete".equals(selectedFilterValue)) {
            // handles manual expansion by each level for latest and latest
            // complete
            // Modified for TIGTK-5064:Start
            if (UIUtil.isNullOrEmpty(sExpandLevels)) {
                sExpandLevels = "1";
            }
            // Modified for TIGTK-5064:End
            int expandLevel = "All".equals(sExpandLevels) ? 0 : Integer.parseInt(sExpandLevels);
            MapList childList = null;
            Map obj = null;
            int level;
            for (int index = 0; index < retList.size(); index++) {
                obj = (Map) retList.get(index);
                if (expandLevel == 0 || Integer.parseInt((String) obj.get("level")) < expandLevel) {
                    paramMap.put("partId", (String) obj.get(SELECT_ID));
                    childList = expandEBOM(context, paramMap);
                    if (childList != null && !childList.isEmpty()) {
                        for (int cnt = 0; cnt < childList.size(); cnt++) {
                            level = Integer.parseInt((String) obj.get("level")) + 1;
                            ((Map) childList.get(cnt)).put("level", String.valueOf(level));
                        }
                        retList.addAll(index + 1, childList);
                    }
                }
            }
        }
        return retList;
    }

    /*
     * Retrieves the EBOM data. Method is added to expand the ebom and fetch the Latest or Latest Complete nodes in child.
     * 
     * @param context the ENOVIA <code>Context</code> object
     * 
     * @param HashMap paramMap
     * 
     * @throws Exception if error encountered while carrying out the request
     */

    public MapList expandEBOM(Context context, HashMap paramMap) throws Exception { // name modified from

        int nExpandLevel = 0;

        String partId = getStringValue(paramMap, "objectId");
        String sExpandLevels = getStringValue(paramMap, "emxExpandFilter");
        String selectedFilterValue = getStringValue(paramMap, "ENCBOMRevisionCustomFilter");
        String complete = PropertyUtil.getSchemaProperty(context, "policy", DomainConstants.POLICY_DEVELOPMENT_PART, "state_Complete");
        String curRevision;
        String latestObjectId;
        String latestRevision;

        String selectedProductConfId = getStringValue(paramMap, "CFFExpressionFilterInput_OID");
        String compileForm = "";
        if (selectedProductConfId == null || selectedProductConfId.equals("undefined")) {
            selectedProductConfId = "";
        }

        if (isValidData(selectedProductConfId)) {
            DomainObject productConfObj;
            // TIGTK-14167 : If filtering is done with "Refinment" button on EBOM then take effectivity expression as it instead take it from PC
            try {
                productConfObj = DomainObject.newInstance(context, selectedProductConfId);
                compileForm = productConfObj.getAttributeValue(context, EffectivityFramework.ATTRIBUTE_FILTER_COMPILED_FORM);
            } catch (Exception e) {
                compileForm = selectedProductConfId;
            }

        }

        if (!isValidData(selectedFilterValue)) {
            selectedFilterValue = "As Stored";
        }

        if (!isValidData(sExpandLevels)) {
            sExpandLevels = getStringValue(paramMap, "ExpandFilter");
        }

        StringList objectSelect = createStringList(new String[] { SELECT_ID, SELECT_NAME, SELECT_REVISION, SELECT_LAST_ID, SELECT_LAST_REVISION, SELECT_REL_FROM_EBOM_EXISTS,
                SELECT_REL_EBOM_FROM_LAST_REVISION_EXISTS, TigerConstants.SELECT_ATTRIBUTE_ENABLECOMPLIANCE });

        String SELECT_ATTRIBUTE_EFFECTIVITY_EXPRESSION = "attribute[" + PropertyUtil.getSchemaProperty(context, "attribute_PSS_CustomEffectivityExpression") + "]";
        StringList relSelect = createStringList(
                new String[] { SELECT_RELATIONSHIP_ID, SELECT_ATTRIBUTE_FIND_NUMBER, SELECT_ATTRIBUTE_QUANTITY, SELECT_FROM_ID, SELECT_ATTRIBUTE_EFFECTIVITY_EXPRESSION });

        if (!isValidData(sExpandLevels) || ("Latest".equals(selectedFilterValue) || "Latest Complete".equals(selectedFilterValue))) {
            nExpandLevel = 1;
            partId = getStringValue(paramMap, "partId") == null ? partId : getStringValue(paramMap, "partId");
        } else if ("All".equalsIgnoreCase(sExpandLevels)) {
            nExpandLevel = 0;
        } else {
            nExpandLevel = Integer.parseInt(sExpandLevels);
        }

        Part partObj = new Part(partId);

        MapList ebomList = partObj.getRelatedObjects(context, RELATIONSHIP_EBOM, TYPE_PART, objectSelect, relSelect, false, true, (short) nExpandLevel, null, null, (short) 0, false, false,
                DomainObject.PAGE_SIZE, null, null, null, null, compileForm);

        Iterator itr = ebomList.iterator();
        Map newMap;

        StringList ebomDerivativeList = EngineeringUtil.getDerivativeRelationships(context, RELATIONSHIP_EBOM, true);

        if ("Latest".equals(selectedFilterValue) || ("Latest Complete".equals(selectedFilterValue))) {
            // Iterate through the maplist and add those parts that are latest
            // but not connected

            while (itr.hasNext()) {
                newMap = (Map) itr.next();

                curRevision = getStringValue(newMap, SELECT_REVISION);
                latestObjectId = getStringValue(newMap, SELECT_LAST_ID);
                latestRevision = getStringValue(newMap, SELECT_LAST_REVISION);

                if (nExpandLevel != 0) {
                    newMap.put("hasChildren", EngineeringUtil.getHasChildren(newMap, ebomDerivativeList));
                }

                if ("Latest".equals(selectedFilterValue)) {
                    newMap.put(SELECT_ID, latestObjectId);
                    newMap.put("hasChildren", EngineeringUtil.getHasChildren(newMap, ebomDerivativeList));
                }

                else {
                    DomainObject domObjLatest = DomainObject.newInstance(context, latestObjectId);
                    String currSta = domObjLatest.getInfo(context, DomainConstants.SELECT_CURRENT);

                    if (curRevision.equals(latestRevision)) {
                        if (complete.equalsIgnoreCase(currSta)) {
                            newMap.put(SELECT_ID, latestObjectId);
                        } else {
                            itr.remove();
                        }
                    } else {
                        while (true) {
                            if (currSta.equalsIgnoreCase(complete)) {
                                newMap.put(SELECT_ID, latestObjectId);
                                break;
                            } else {
                                BusinessObject boObj = domObjLatest.getPreviousRevision(context);
                                if (!(boObj.toString()).equals("..")) {
                                    boObj.open(context);
                                    latestObjectId = boObj.getObjectId();
                                    domObjLatest = DomainObject.newInstance(context, latestObjectId);
                                    currSta = domObjLatest.getInfo(context, DomainConstants.SELECT_CURRENT);
                                } else {
                                    itr.remove();
                                    break;
                                }
                            }
                        } // End of while
                    } // End of Else
                }
            } // End of While

        } // End of IF, Latest or Latest complete filter is selected

        else if (nExpandLevel != 0) {
            while (itr.hasNext()) {
                newMap = (Map) itr.next();

                // To display + or - in the bom display
                // newMap.put("hasChildren", getStringValue(newMap,
                // SELECT_REL_FROM_EBOM_EXISTS));
                newMap.put("hasChildren", EngineeringUtil.getHasChildren(newMap, ebomDerivativeList));
            }
        }

        return ebomList;
    }

    private String getStringValue(Map map, String key) {
        return (String) map.get(key);
    }

    private boolean isValidData(String data) {
        return ((data == null || "null".equals(data)) ? 0 : data.trim().length()) > 0;
    }

    private StringList createStringList(String[] selectable) {
        int length = length(selectable);
        StringList list = new StringList(length);
        for (int i = 0; i < length; i++)
            list.add(selectable[i]);
        return list;
    }

    private int length(Object[] array) {
        return array == null ? 0 : array.length;
    }

    public List<String> displayAddRemoveRowIcon(Context context, String args[]) throws Exception {
        List<String> returnList = new StringList();
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        MapList objectList = (MapList) programMap.get("objectList");
        // TIGTK-2545 : Diversity (SGS):29-07-2016 : START
        HashMap paramList = (HashMap) programMap.get("paramList");
        String strObjectId = (String) paramList.get("objectId");

        DomainObject domParentObject = DomainObject.newInstance(context, strObjectId);
        String strParentPartState = domParentObject.getInfo(context, DomainConstants.SELECT_CURRENT);
        // TIGTK-2545 : Diversity (SGS):29-07-2016 : END
        String strValidStates = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(), "PSS_emxEngineeringCentral.BOM.ListOfValidStates");
        StringBuilder sbCheckboxId;
        StringBuilder sbColumnValue;
        String strImageStatus = "false";
        for (int i = 0; i < objectList.size(); i++) {
            Map mapObjectInfo = (Map) objectList.get(i);
            String strRowId = "1";
            strImageStatus = "false";
            sbCheckboxId = new StringBuilder();
            sbColumnValue = new StringBuilder();
            String strPartState = (String) mapObjectInfo.get(DomainConstants.SELECT_CURRENT);

            // Modified for Diveristy stream issue TIGTK-3779 : Priyanka Salunke:21-Dec-2016 : START
            String strPartConnectionId = (String) mapObjectInfo.get(DomainConstants.SELECT_RELATIONSHIP_ID);
            if (UIUtil.isNotNullAndNotEmpty(strPartConnectionId)) {
                DomainRelationship domRelationshipId = DomainRelationship.newInstance(context, strPartConnectionId);
                String strConnectionIdArr[] = new String[] { strPartConnectionId };
                MapList mlParentPartIds = DomainRelationship.getInfo(context, strConnectionIdArr, new StringList(DomainConstants.SELECT_FROM_ID));
                // Check part have parent or not
                if (mlParentPartIds != null && !mlParentPartIds.isEmpty()) {
                    int intSize = mlParentPartIds.size();
                    for (int itr = 0; itr < intSize; itr++) {
                        Map mParentPartMap = (Map) mlParentPartIds.get(itr);
                        String strParentId = (String) mParentPartMap.get(DomainConstants.SELECT_FROM_ID);
                        if (UIUtil.isNotNullAndNotEmpty(strParentId)) {
                            DomainObject domParentPartObject = DomainObject.newInstance(context, strParentId);
                            String strImmediateParentPartState = domParentPartObject.getInfo(context, DomainConstants.SELECT_CURRENT);
                            if (strValidStates.contains(strImmediateParentPartState) && strValidStates.contains(strParentPartState)) {
                                strImageStatus = "true";
                            }
                        }
                    }
                }
            }
            // Modified for Diveristy stream issue TIGTK-3779 : Priyanka Salunke:21-Dec-2016 :END

            sbCheckboxId.append("\"").append((String) mapObjectInfo.get("id[parent]")).append("\",\"");
            sbCheckboxId.append((String) mapObjectInfo.get("id[connection]")).append("\",\"").append((String) mapObjectInfo.get("id"));
            sbCheckboxId.append("\",\"").append(strRowId).append("\",\"").append(strImageStatus).append("\"");
            if ((String) mapObjectInfo.get("id[connection]") == null) {
                sbColumnValue.append(" ");
            } else {
                sbColumnValue.append("<img src='../effectivity/images/buttonMinus.png' class='PSSRemoveRow' onclick='parent.removeOneRow(").append(sbCheckboxId.toString()).append(")'/>");
                sbColumnValue.append("<img src='../effectivity/images/buttonPlus.png' class='PSSAddRow' onclick='parent.addNewRow(").append(sbCheckboxId.toString()).append(")'/>");
            }

            returnList.add(i, sbColumnValue.toString());
        }
        return returnList;
    }

    public List getProductRelatedOptionsAsColumns(Context context, String[] args) throws Exception {
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        HashMap requestMap = (HashMap) paramMap.get("requestMap");
        String strProductId = (String) requestMap.get("productId");
        // TIGTK-2544 : Diversity (SGS):29-07-2016 : START
        String strObjectId = (String) requestMap.get("objectId");
        DomainObject domParentObject = DomainObject.newInstance(context, strObjectId);
        String strParentPartStatus = domParentObject.getInfo(context, DomainConstants.SELECT_CURRENT);
        // TIGTK-2544 : Diversity (SGS):29-07-2016 : END
        String STR_PHYSICAL_ID = "physicalid";
        // TIGTK-1950 fixes
        String SELECT_CONFIGURATION_FEATURE_DISPLAY_NAME = "from." + TigerConstants.SELECT_ATTRIBUTE_DISPLAYNAME;
        // TIGTK-1950 fixes
        String objectIcon = UINavigatorUtil.getTypeIconProperty(context, TigerConstants.RELATIONSHIP_CONFIGURATION_OPTION);

        if (objectIcon == null || objectIcon.length() == 0) {
            objectIcon = "iconSmallConfigurationOption.gif";
        }

        StringList selectStmts = new StringList();
        selectStmts.addElement(DomainConstants.SELECT_NAME);
        selectStmts.addElement(DomainConstants.SELECT_ID);
        selectStmts.addElement(TigerConstants.SELECT_ATTRIBUTE_DISPLAYNAME);
        // Added by Suchit G. for TIGTK-4488 on 06/03/2017: START
        selectStmts.addElement(DomainConstants.SELECT_TYPE);
        // Added by Suchit G. for TIGTK-4488 on 06/03/2017: END
        StringList relSelectStmts = new StringList();
        relSelectStmts.addElement(DomainConstants.SELECT_FROM_NAME);
        // TIGTK-1950 fixes
        relSelectStmts.addElement(SELECT_CONFIGURATION_FEATURE_DISPLAY_NAME);
        // TIGTK-1950 fixes
        relSelectStmts.addElement(DomainConstants.SELECT_FROM_ID);
        relSelectStmts.addElement(STR_PHYSICAL_ID);
        // Added by Suchit G. for TIGTK-4488 on 06/03/2017: START
        relSelectStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_SEQUENCEORDER + "]");
        // Added by Suchit G. for TIGTK-4488 on 06/03/2017: END

        DomainObject domProductObject = DomainObject.newInstance(context, strProductId);

        // Modified by Suchit G. for TIGTK-4488 on 06/03/2017: START
        MapList lstColor = (MapList) domProductObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_CONFIGURATION_FEATURE + "," + TigerConstants.RELATIONSHIP_CONFIGURATION_OPTION, "*",
                selectStmts, relSelectStmts, false, true, (short) 0, "", "", 0, null, null, null);
        // Modified by Suchit G. for TIGTK-4488 on 06/03/2017: END

        // Define a new MapList to return.
        MapList columnMapList = new MapList();
        // Added for TIGTK-3476 by Priyanka Salunke on Date : 24/10/2016 : START

        // Modified by Suchit G. for TIGTK-4488 on 06/03/2017: START
        lstColor.sortStructure("attribute[" + TigerConstants.ATTRIBUTE_SEQUENCEORDER + "]", "ascending", "integer");
        // Modified by Suchit G. for TIGTK-4488 on 06/03/2017: END

        // Added for TIGTK-3476 by Priyanka Salunke on Date : 24/10/2016 : END
        int i = 0;
        Iterator itr = lstColor.iterator();

        while (itr.hasNext()) {
            Map newMap = (Map) itr.next();
            String strColumnName = (String) newMap.get(TigerConstants.SELECT_ATTRIBUTE_DISPLAYNAME);
            String strColumnId = (String) newMap.get(DomainConstants.SELECT_ID);

            // TIGTK-1950 fixes
            String strColumnGroupName = (String) newMap.get(SELECT_CONFIGURATION_FEATURE_DISPLAY_NAME);
            // TIGTK-1950 fixes

            String strColumnGroupId = (String) newMap.get(DomainConstants.SELECT_FROM_ID);
            String strConfigConnectionPhysicalId = (String) newMap.get(STR_PHYSICAL_ID);

            // Added by Suchit G. for TIGTK-4488 on 06/03/2017: START
            String strObjectType = (String) newMap.get(DomainConstants.SELECT_TYPE);
            if (strObjectType.equalsIgnoreCase(ConfigurationConstants.TYPE_CONFIGURATION_FEATURE)) {
                continue;
            }
            // Added by Suchit G. for TIGTK-4488 on 06/03/2017: END

            // create a column map to be returned.
            HashMap colMap = new HashMap();
            HashMap settingsMap = new HashMap();

            // Set information of Column Settings in settingsMap
            settingsMap.put("Column Type", "programHTMLOutput");
            settingsMap.put("program", "PSS_emxPart");
            settingsMap.put("function", "getCheckboxesOfEffectivityForPart");
            settingsMap.put("Editable", "true");
            settingsMap.put("Sortable", "false");
            settingsMap.put("Group Header", strColumnGroupName);
            settingsMap.put("Registered Suite", "EngineeringCentral");
            // TIGTK-6819:PKH:Phase-2.0:Start

            if (i % 2 == 0) {
                settingsMap.put("Style Column", "PSS_EditorsColumn");
            }
            i++;
            // TIGTK-6819:PKH:Phase-2.0:End
            settingsMap.put("Option Name", strColumnName);
            // TIGTK-2544 : Diversity (SGS):29-07-2016 : START
            settingsMap.put("strParentPartStatus", strParentPartStatus);
            // TIGTK-2544 : Diversity (SGS):29-07-2016 : END

            // set column information
            colMap.put("name", strColumnGroupId + "_" + strColumnId + "_" + strConfigConnectionPhysicalId);
            colMap.put("label", "<img src='../common/images/" + objectIcon + "' class='PSSRotateColumn' onload='parent.rotateText(this)'/>" + strColumnName + "");
            colMap.put("settings", settingsMap);
            // user added key value pair.
            columnMapList.add(colMap);
        }
        return columnMapList;
    }

    public List<String> getCheckboxesOfEffectivityForPart(Context context, String args[]) throws Exception {
        try {
            logger.debug("PSS_emxPart : getCheckboxesOfEffectivityForPart() : START");
            // TIGTK-9934 : START
            final String SELECT_FROM_PROJECT = "from.project";
            final String SELECT_FROM_CURRENT = "from.current";
            // TIGTK-9934 : END
            List<String> returnList = new StringList();
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");
            Map columnMap = (Map) programMap.get("columnMap");
            Map mapSettings = (Map) columnMap.get("settings");
            String strOptionName = (String) mapSettings.get("Option Name");
            String strFeatureName = (String) mapSettings.get("Group Header");
            // TIGTK-2544 : Diversity (SGS):29-07-2016 : START
            String strParentPartStatus = (String) mapSettings.get("strParentPartStatus");
            // TIGTK-2544 : Diversity (SGS):29-07-2016 : END
            String strColumnName = (String) columnMap.get("name");
            String strValidStates = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(), "PSS_emxEngineeringCentral.BOM.ListOfValidStates");

            StringBuilder sbColumnValue;
            String strRowId = "1";
            int intRowCount = 0;
            String[] arrExpression = null;
            String strImageStatus = "false";
            // TIGTK-9934 : START
            String strLoggedInCS = PersonUtil.getDefaultProject(context, context.getUser());
            StringList slObjectSelects = new StringList();
            slObjectSelects.add(DomainConstants.SELECT_FROM_ID);
            slObjectSelects.add(SELECT_FROM_PROJECT);
            slObjectSelects.add(SELECT_FROM_CURRENT);
            // TIGTK-9934 : END

            for (int i = 0; i < objectList.size(); i++) {
                sbColumnValue = new StringBuilder();

                Map mapObjectInfo = (Map) objectList.get(i);
                strImageStatus = "false";
                String strTableRowId = (String) mapObjectInfo.get("id[level]");
                String strPartName = (String) mapObjectInfo.get(DomainConstants.SELECT_NAME);
                String strPartId = (String) mapObjectInfo.get(DomainConstants.SELECT_ID);
                String strPartState = (String) mapObjectInfo.get(DomainConstants.SELECT_CURRENT);
                // Added for Diversity stream issue : TIGTK-3779 : Priyanka Salunke : 21-Dec-2016 : START
                String strPartConnectionId = (String) mapObjectInfo.get(DomainConstants.SELECT_RELATIONSHIP_ID);

                if (UIUtil.isNotNullAndNotEmpty(strPartConnectionId)) {
                    String strConnectionIdArr[] = new String[] { strPartConnectionId };
                    // TIGTK-9934 : START
                    MapList mlParentPartIds = DomainRelationship.getInfo(context, strConnectionIdArr, slObjectSelects);
                    // TIGTK-9934 : END
                    // Check part have parent or not
                    if (mlParentPartIds != null && !mlParentPartIds.isEmpty()) {
                        int intSize = mlParentPartIds.size();
                        for (int itr = 0; itr < intSize; itr++) {
                            Map mParentPartMap = (Map) mlParentPartIds.get(itr);
                            String strParentId = (String) mParentPartMap.get(DomainConstants.SELECT_FROM_ID);
                            if (UIUtil.isNotNullAndNotEmpty(strParentId)) {
                                // Modified by PTE for Find bug issue
                                // TIGTK-9934 : START
                                String strImmediateParentPartState = (String) mParentPartMap.get(SELECT_FROM_CURRENT);
                                String strImmediateParentPartCS = (String) mParentPartMap.get(SELECT_FROM_PROJECT);
                                if (strValidStates.contains(strImmediateParentPartState) && strValidStates.contains(strParentPartStatus)) {
                                    if (strLoggedInCS.equalsIgnoreCase(strImmediateParentPartCS)) {
                                        strImageStatus = "true";
                                    }
                                    // TIGTK-9934 : END
                                }
                            }
                        }
                    }
                }
                // Added for Diversity stream issue : TIGTK-3779 : Priyanka Salunke : 21-Dec-2016 : END
                // TIGTK-2544 : Diversity (SGS):29-07-2016 : START
                if (strPartState == null) {
                    DomainObject domParentPartObject = DomainObject.newInstance(context, strPartId);
                    strPartState = domParentPartObject.getInfo(context, DomainConstants.SELECT_CURRENT);
                }
                // TIGTK-2544 : Diversity (SGS):29-07-2016 : END
                try {
                    intRowCount = (int) mapObjectInfo.get("Row Count");
                    arrExpression = (String[]) mapObjectInfo.get("Rows");
                } catch (Exception ex) {
                    intRowCount = 0;
                    arrExpression = null;
                }
                strRowId = "1";
                String strConnectionId = (String) mapObjectInfo.get("id[connection]");

                String strCheckBoxId = strConnectionId + "_" + strColumnName + "_" + strTableRowId + "_" + strPartId + "_" + strRowId;
                String strCheckBoxClass = strConnectionId + "_" + strColumnName;
                String strCheckBoxName = strConnectionId + "_" + strRowId;

                String strConnectioPhyId = strColumnName.split("_")[2];
                if (mapObjectInfo.get("id[connection]") == null) {
                    sbColumnValue.append(" ");
                } else {
                    sbColumnValue.append("<div name=\"").append(strConnectionId).append("\">");
                    if (intRowCount > 0) {
                        for (int j = 0; j < intRowCount; j++) {
                            // strRowId = j + 1 + "";
                            strRowId = String.valueOf(j + 1);
                            strCheckBoxId = strConnectionId + "_" + strColumnName + "_" + strTableRowId + "_" + strPartId + "_" + strRowId;
                            strCheckBoxClass = strConnectionId + "_" + strColumnName;
                            strCheckBoxName = strConnectionId + "_" + strRowId;
                            if (arrExpression[j].indexOf(strConnectioPhyId) > -1) {
                                // TIGTK-3874 : Vasant PADHIYAR : 01-Feb-2017 : START
                                String strIconUrl = "./images/checkeddisable.png";
                                if (strImageStatus.equalsIgnoreCase("true")) {
                                    strIconUrl = "./images/checked.png";
                                }
                                // TIGTK-3874 : Vasant PADHIYAR : 01-Feb-2017 : END
                                sbColumnValue.append("<img src='").append(strIconUrl).append("' partName=\"").append(strPartName).append("\" optionName=\"").append(strOptionName)
                                        .append("\" featureName=\"").append(strFeatureName).append("\" rowid=\"").append(strRowId).append("\" name=\"").append(strCheckBoxName)
                                        .append("\" onclick=\"parent.updateCellValue('" + strCheckBoxClass + "','" + strRowId + "','" + strImageStatus + "')\" id=\"").append(strCheckBoxId)
                                        .append("\" class=\"").append(strCheckBoxClass).append("\" common=\"").append(strConnectionId).append("\"/>");
                            } else {
                                // TIGTK-3874 : Vasant PADHIYAR : 01-Feb-2017 : START
                                String strIconUrl = "./images/uncheckeddisable.png";
                                if (strImageStatus.equalsIgnoreCase("true")) {
                                    strIconUrl = "./images/unchecked.png";
                                }
                                // TIGTK-3874 : Vasant PADHIYAR : 01-Feb-2017 : END
                                sbColumnValue.append("<img src='").append(strIconUrl).append("' partName=\"").append(strPartName).append("\" optionName=\"").append(strOptionName)
                                        .append("\" featureName=\"").append(strFeatureName).append("\" rowid=\"").append(strRowId).append("\" name=\"").append(strCheckBoxName)
                                        .append("\" onclick=\"parent.updateCellValue('" + strCheckBoxClass + "','" + strRowId + "','" + strImageStatus + "')\" id=\"").append(strCheckBoxId)
                                        .append("\" class=\"").append(strCheckBoxClass).append("\" common=\"").append(strConnectionId).append("\"/>");
                            }
                            if (j + 1 < intRowCount) {
                                sbColumnValue.append("<br/>");
                            }
                        }
                    } else {
                        // TIGTK-3874 : Vasant PADHIYAR : 01-Feb-2017 : START
                        String strIconUrl = "./images/uncheckeddisable.png";
                        if (strImageStatus.equalsIgnoreCase("true")) {
                            strIconUrl = "./images/unchecked.png";
                        }
                        // TIGTK-3874 : Vasant PADHIYAR : 01-Feb-2017 : END
                        sbColumnValue.append("<img src='").append(strIconUrl).append("' partName=\"").append(strPartName).append("\" optionName=\"").append(strOptionName).append("\" featureName=\"")
                                .append(strFeatureName).append("\" rowid=\"").append(strRowId).append("\" name=\"").append(strCheckBoxName)
                                .append("\" onclick=\"parent.updateCellValue('" + strCheckBoxClass + "','" + strRowId + "','" + strImageStatus + "')\" id=\"").append(strCheckBoxId)
                                .append("\" class=\"").append(strCheckBoxClass).append("\" common=\"").append(strConnectionId).append("\"/>");
                    }
                    sbColumnValue.append("</div>");
                }
                returnList.add(i, sbColumnValue.toString());
            }
            logger.error("PSS_emxPart : getCheckboxesOfEffectivityForPart() : END");
            return returnList;
        } catch (Exception e) {
            logger.error("Error in PSS_emxPart : getCheckboxesOfEffectivityForPart()\n", e);
            throw e;
        }
    }

    public MapList getEBOMStructureOfSelectedPartForEffectivity(Context context, String[] args) throws Exception {
        try {
            logger.debug("PSS_emxPart : getEBOMStructureOfSelectedPartForEffectivity() : START");

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String objectId = (String) programMap.get("objectId");
            String productId = (String) programMap.get("productId");
            String strsExpandLevels = (String) programMap.get("emxExpandFilter");
            if (UIUtil.isNullOrEmpty(strsExpandLevels)) {
                strsExpandLevels = "1";
            }
            int nExpandLevel = "All".equals(strsExpandLevels) ? 0 : Integer.parseInt(strsExpandLevels);

            StringList slObjectSelect = new StringList();
            slObjectSelect.add(SELECT_ID);
            slObjectSelect.add(SELECT_NAME);
            slObjectSelect.add(SELECT_CURRENT);

            StringList slRelSelect = new StringList();
            slRelSelect.add(SELECT_RELATIONSHIP_ID);
            slRelSelect.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_CUSTOMEFFECTIVITYEXPRESSION + "]");
            // TIGTK-9432 : PSE : START
            slRelSelect.add(TigerConstants.SELECT_CONFIGURATIONFEATURE_OPTION_RELID_FROM_EFFECTIVITY);
            // TIGTK-9432 : PSE : END

            Part partObj = new Part(objectId);

            MapList mlResultPartsForEffectivity = partObj.getRelatedObjects(context, RELATIONSHIP_EBOM, TYPE_PART, slObjectSelect, slRelSelect, false, true, (short) nExpandLevel, null, null, 0);
            mlResultPartsForEffectivity = replaceCustExpConnectionIdByObjIds(context, mlResultPartsForEffectivity, productId);

            logger.debug("PSS_emxPart : getEBOMStructureOfSelectedPartForEffectivity() : END");

            return mlResultPartsForEffectivity;
        } catch (Exception e) {
            logger.error("Error in PSS_emxPart : getEBOMStructureOfSelectedPartForEffectivity()\n", e);
            throw e;
        }
    }

    public MapList replaceCustExpConnectionIdByObjIds(Context context, MapList mlCustomExp, String strProductId) throws Exception {
        try {
            logger.debug("PSS_emxPart : replaceCustExpConnectionIdByObjIds() : START");

            Map mapTemp;
            String strCustExpression, strTempExp, strFeatureOptionIds;
            String[] arrExpression, arrTemp;
            StringList slRelSelect = new StringList();
            slRelSelect.add(DomainConstants.SELECT_FROM_ID);
            slRelSelect.add(DomainConstants.SELECT_TO_ID);
            MapList mlReturnInfo;

            DomainObject domProductObj = DomainObject.newInstance(context, strProductId);

            StringList selectObjStmts = new StringList(1);
            selectObjStmts.addElement("physicalid");

            MapList mlProducts = domProductObj.getRelatedObjects(context, TigerConstants.TYPE_MAINPRODUCT, TigerConstants.TYPE_MODEL, selectObjStmts, new StringList(), true, false, (short) 0, null,
                    null, 0);

            if (mlProducts.size() > 0) {
                for (int i = 0; i < mlCustomExp.size(); i++) {
                    mapTemp = (Map) mlCustomExp.get(i);
                    strCustExpression = (String) mapTemp.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_CUSTOMEFFECTIVITYEXPRESSION + "]");
                    strCustExpression = strCustExpression.replaceAll("@EF_FO", "").replaceAll("PHY@EF:", "").replaceAll("\\p{P}", "");
                    arrExpression = strCustExpression.split("\\|\\|");
                    mapTemp.put("Row Count", arrExpression.length);

                    for (int j = 0; j < arrExpression.length; j++) {
                        arrTemp = arrExpression[j].split("OR|AND");
                        StringBuffer sbFeatureOptionIds = new StringBuffer();
                        if (arrTemp.length > 0) {
                            for (int k = 0; k < arrTemp.length; k++) {
                                strTempExp = arrTemp[k].split("~")[0].trim();
                                if (strTempExp.length() > 0) {
                                    sbFeatureOptionIds.append(strTempExp);
                                    if (k + 1 < arrTemp.length)
                                        sbFeatureOptionIds.append("|");
                                }
                            }
                        }
                        arrExpression[j] = sbFeatureOptionIds.toString();
                    }
                    mapTemp.put("Rows", arrExpression);

                    mlCustomExp.set(i, mapTemp);
                }
            }
            logger.debug("PSS_emxPart : replaceCustExpConnectionIdByObjIds() : END");
            return mlCustomExp;

        } catch (Exception e) {
            logger.error("Error in PSS_emxPart : replaceCustExpConnectionIdByObjIds()\n", e);
            throw e;
        }
    }

    public String[] removeDuplicatesFromArray(String[] myArray) {
        Set<String> setTemp = new HashSet<String>();
        for (String value : myArray) {
            setTemp.add(value);
        }
        java.util.Arrays.fill(myArray, null);
        Iterator<String> setitr = setTemp.iterator();
        int i = 0;
        while (setitr.hasNext()) {
            myArray[i] = setitr.next();
            i++;
        }
        return myArray;
    }

    /**
     * This method to be called on relationship 'Part Specification' trigger object to copy the Part's Description to CAD Object.
     * @param context
     * @param args
     * @param args0
     *            -- Part Object Id
     * @param args1
     *            -- CAD Object Id
     * @return -- int -- Return Status of Success or Failure of Copy Description from Part to CAD Object
     * @throws Exception
     */
    public int synchronizeDescriptionToCAD(Context context, String[] args) throws Exception {

        int intReturnStatus = 0;
        boolean updateDescription = false;

        String strPartObjectId = args[0];
        String strCADObjectId = args[1];

        DomainObject domPartObject = DomainObject.newInstance(context, strPartObjectId);
        DomainObject domCADObject = DomainObject.newInstance(context, strCADObjectId);

        // Get Current State of Part Object
        String strFromCurrentState = domPartObject.getInfo(context, DomainConstants.SELECT_CURRENT);
        String strCADCurrentState = domCADObject.getInfo(context, DomainConstants.SELECT_CURRENT);

        if ((strFromCurrentState.equals(TigerConstants.STATE_PSS_ECPART_PRELIMINARY) || strFromCurrentState.equals(TigerConstants.STATE_DEVELOPMENTPART_CREATE))
                && strCADCurrentState.equals(TigerConstants.STATE_INWORK_CAD_OBJECT)) {
            String strCADGeometryType = domCADObject.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE);

            if (strCADGeometryType.equals(TigerConstants.ATTR_RANGE_PSSGEOMETRYTYPE)) {
                String strPartObjDesc = domPartObject.getInfo(context, DomainConstants.SELECT_DESCRIPTION);
                domCADObject.setDescription(context, strPartObjDesc);
            }
        }

        // TIGTK-3823 - PTE - CAD Object description should be updated only for In Work CADs - END

        return intReturnStatus;
    }

    /**
     * Return the CAD Object Name column for the table PSS_SynchronizationCADObjectEBOMSummary
     * @param context
     *            : the eMatrix <code>Context</code> object
     * @param args
     *            : holds no arguments
     * @return
     * @throws Exception
     */
    public Vector<String> getCADObjectNameCADToBOM(Context context, String[] args) throws Exception {
        try {
            Vector<String> vCADObjectNames = new Vector<String>();

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");

            Map objectMap = null;
            StringBuffer outPut = null;
            String sCADObjectId = null;
            String sCADObjName = "";
            String sCADObjParent = "";
            String sIsSynchronize = "";
            int index = 0;
            Iterator objectListItr = objectList.iterator();
            while (objectListItr.hasNext()) {
                objectMap = (Map) objectListItr.next();

                sCADObjectId = (String) objectMap.get(SELECT_ID);
                sCADObjName = (String) objectMap.get(SELECT_NAME);
                sCADObjParent = (String) objectMap.get("PARENT");
                sIsSynchronize = (String) objectMap.get("IsSynchronize");

                outPut = new StringBuffer();
                outPut.append("<NOBR>");
                outPut.append(sCADObjName);
                if ("true".equalsIgnoreCase(sIsSynchronize)) {
                    outPut.append("&nbsp;<img src=\"../common/images/buttonDialogDone.gif\">");
                } else {
                    outPut.append("&nbsp;<img src=\"../common/images/buttonDialogCancel.gif\">");
                }
                outPut.append("<input type=\"hidden\" name=\"CADObjectId\"  value=\"" + sCADObjectId + "\"/>");
                outPut.append("<input type=\"hidden\" name=\"CADObjectName\" value=\"" + sCADObjName + "\"/>");
                outPut.append("<input type=\"hidden\" name=\"CADObjectParent\" value=\"" + sCADObjParent + "\"/>");
                if (index == 0) {
                    outPut.append("<input type=\"hidden\" id=\"EBOMLinksCreation\" name=\"EBOMLinksCreation\" value=\"" + "No" + "\"/>");
                    outPut.append("<input type=\"hidden\" id=\"ReconciliationLinksDelete\" name=\"ReconciliationLinksDelete\" value=\"" + "No" + "\"/>");
                    // TIGTK-8512 - VP - 2017-07-10 - START
                    outPut.append("<input type=\"hidden\" id=\"KeepCADNameForPartName\" name=\"KeepCADNameForPartName\" value=\"" + "No" + "\"/>");
                    outPut.append("<input type=\"hidden\" id=\"SelectedPolicyForPart\" name=\"SelectedPolicyForPart\" value=\"" + "PSS_Development_Part" + "\"/>");
                    // TIGTK-8512 - VP - 2017-07-10 - END
                    // added for TIGTK-2308
                    /*
                     * TIGTK-8512 - VP - 2017-07-10 - START outPut.append(
                     * "<script>$(document).ready(function() {$(\"input[type='checkbox']\").change(function(){ if(this.checked){if(this.parentNode.parentNode.cells[7].childNodes[2].value){}else{alert(\"No Part Object selected for the CAD\");this.checked = false;}}});});</script>"
                     * ); //TIGTK-8512 - VP - 2017-07-10 - END
                     */
                }
                outPut.append("</NOBR>");

                vCADObjectNames.add(outPut.toString());
                index++;
            }

            return vCADObjectNames;
        } catch (Exception ex) {
            logger.error("Error in getCADObjectName ", ex);
            throw ex;
        }
    }

    /**
     * Return the Geometry Type column values to fill the table
     * @param context
     *            : the eMatrix <code>Context</code> object
     * @param args
     *            : holds no arguments
     * @return
     * @throws Exception
     */
    public Vector<String> getGeometryTypeRange(Context context, String[] args) throws Exception {
        try {

            String sLanguage = context.getSession().getLanguage();

            Vector<String> vCADObjNames = new Vector<String>();

            // get all range values
            StringList strListGeometryType = FrameworkUtil.getRanges(context, TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE);

            strListGeometryType.remove("");
            strListGeometryType.sort();
            // remove from the selection list the BD type
            // strListGeometryType.remove(RANGE_BASIS_DEFINITION);

            // Most used and important ranges
            ResourceBundle iefProps = ResourceBundle.getBundle("emxIEFDesignCenter");
            String strGeometryTypeMostUsedRanges = iefProps.getString("AttributeRange.PSS_GeometryType.MostUsedRanges");

            StringList strGeometryTypeMostUsedRangesList = FrameworkUtil.split(strGeometryTypeMostUsedRanges, ",");

            boolean propertiesMostUsedRanges = false;

            if (strGeometryTypeMostUsedRangesList.size() > 0) {
                propertiesMostUsedRanges = true;
            }

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");

            StringBuffer outPut = null;
            Map objectMap = null;
            String sCADObjGeometryType = "";
            String sPartObjId = "";
            String sCADObjState = "";
            StringBuffer sbOptionValues = null;
            String sSelected = "";
            int iSelectedIndex, iIndex;
            String sDefaultValue = "";
            String attrValue = "";
            String dispValue = "";
            Iterator objectListItr = objectList.iterator();
            while (objectListItr.hasNext()) {
                objectMap = (Map) objectListItr.next();

                sCADObjGeometryType = (String) objectMap.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE + "]");
                sPartObjId = (String) objectMap.get(SELECT_ID);
                // TIGTK-11319:START
                boolean bCheck = false;
                String strState = (String) objectMap.get(SELECT_CURRENT);
                // TIGTK-11319:END

                sCADObjState = (String) objectMap.get("CADObjectCurrent");
                logger.debug("getGeometryTypeRange() - sCADObjState = <" + sCADObjState + ">");

                sbOptionValues = new StringBuffer("");
                sSelected = "";
                iSelectedIndex = 0;
                iIndex = 0;
                sDefaultValue = "MG";
                if (sCADObjGeometryType != null && !"".equals(sCADObjGeometryType)) {
                    sDefaultValue = sCADObjGeometryType;
                }

                if (propertiesMostUsedRanges) {
                    for (int i = 0; i < strGeometryTypeMostUsedRangesList.size(); i++) {
                        attrValue = (String) strGeometryTypeMostUsedRangesList.get(i);
                        dispValue = i18nNow.getRangeI18NString(TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE, attrValue, sLanguage);

                        if (sDefaultValue.equals(attrValue)) {
                            sSelected = "selected";
                            iSelectedIndex = iIndex;
                        } else {
                            sSelected = "";
                        }
                        // Findbug Issue correction start
                        // Date: 22/03/2017
                        // By: Asha G.
                        sbOptionValues.append("<option ");
                        sbOptionValues.append(sSelected);
                        sbOptionValues.append(" value=\"");
                        sbOptionValues.append(attrValue);
                        sbOptionValues.append("\">");
                        sbOptionValues.append(dispValue);
                        sbOptionValues.append("</option>");
                        // Findbug Issue correction End
                        iIndex++;
                    }
                }

                for (Iterator iterator = strListGeometryType.iterator(); iterator.hasNext();) {
                    attrValue = (String) iterator.next();
                    dispValue = i18nNow.getRangeI18NString(TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE, attrValue, sLanguage);
                    if (sDefaultValue.equals(attrValue)) {
                        sSelected = "selected";
                        iSelectedIndex = iIndex;
                    } else {
                        sSelected = "";
                    }

                    if (propertiesMostUsedRanges) {
                        if (!strGeometryTypeMostUsedRangesList.contains(attrValue)) {

                            // Find Bug : Dodgy Code : PS :21-March-2017 : START
                            sbOptionValues.append("<option ");
                            sbOptionValues.append(sSelected);
                            sbOptionValues.append(" value=\"");
                            sbOptionValues.append(attrValue);
                            sbOptionValues.append("\">");
                            sbOptionValues.append(dispValue);
                            sbOptionValues.append("</option>");
                            // Find Bug : Dodgy Code : PS :21-March-2017 : END
                            iIndex++;
                        } else {
                            // Already added from properties file - do nothing
                        }
                    } else {
                        // Find Bug : Dodgy Code : PS :21-March-2017 : START
                        sbOptionValues.append("<option ");
                        sbOptionValues.append(sSelected);
                        sbOptionValues.append(" value=\"");
                        sbOptionValues.append(attrValue);
                        sbOptionValues.append("\">");
                        sbOptionValues.append(dispValue);
                        sbOptionValues.append("</option>");
                        // Find Bug : Dodgy Code : PS :21-March-2017 : END
                        iIndex++;
                    }
                }

                outPut = new StringBuffer();
                DomainObject domObj = DomainObject.newInstance(context, sPartObjId);
                if (domObj.isKindOf(context, TYPE_PART)) {
                    // TIGTK-11319:START
                    if (TigerConstants.STATE_PSS_ECPART_PRELIMINARY.equals(strState) || TigerConstants.STATE_PSS_DEVELOPMENTPART_CREATE.equals(strState)) {
                        bCheck = true;
                    }
                    // TIGTK-11319:END
                } else {
                    bCheck = true;
                }
                if (bCheck) {

                    outPut.append("<select modifygeometry=\"true\" name=\"CADObjectGeometryType\"");
                    if (TigerConstants.STATE_RELEASED_CAD_OBJECT.equals(sCADObjState)) {

                        outPut.append("onchange=\"unchangeGeometryTypeSelectedValue(this, " + iSelectedIndex + ");\"");
                    }
                    outPut.append(">");
                    outPut.append(sbOptionValues.toString());
                    outPut.append("</select>");
                    vCADObjNames.add(outPut.toString());

                } else {
                    if (UIUtil.isNotNullAndNotEmpty(sCADObjGeometryType)) {
                        outPut.append(sCADObjGeometryType);
                    } else {
                        outPut.append("");
                    }

                    // TIGTK-9710 : START
                    outPut.append("<input type=\"hidden\" name=\"CADObjectGeometryType\" value=\"" + sCADObjGeometryType + "\"/>");
                    // TIGTK-9710 : END
                    vCADObjNames.add(outPut.toString());
                }

            }
            return vCADObjNames;

        } catch (Exception ex) {
            logger.error("Error in getGeometryTypeRange()\n", ex);
            throw ex;
        }
    }

    /**
     * Return the Part Object Name column for the table PSS_SynchronizationCADObjectEBOMSummary
     * @param context
     *            : the eMatrix <code>Context</code> object
     * @param args
     *            : holds no arguments
     * @return
     * @throws Exception
     */
    public Vector<String> getPartObjectNameCADToBOM(Context context, String[] args) throws Exception {
        try {
            int PART_OBJECT_NAME_COLUMN_INDEX = 7;
            int PART_OBJECT_REVISON_COLUMN_INDEX = 8;
            // TIGTK-8512 - 2017-07-10 - VP - START
            int PART_OBJECT_DESCRIPTION_COLUMN_INDEX = 10;
            int PART_FAMILY_COLUMN_INDEX = 9;
            // TIGTK-8512 - 2017-07-10 - VP - END

            Vector<String> vPartObjNames = new Vector<String>();

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");

            // TIGTK-8512 - 2017-07-10 - VP - START
            String aCulumnsTextEdit = "[" + PART_OBJECT_NAME_COLUMN_INDEX + "," + PART_FAMILY_COLUMN_INDEX + "]";
            String aCulumnsText = "[" + PART_OBJECT_REVISON_COLUMN_INDEX + "," + PART_OBJECT_DESCRIPTION_COLUMN_INDEX + "]";
            // TIGTK-8512 - 2017-07-10 - VP - END

            String sCADObjectId = "";
            // modified for RFC-071

            // TIGTK-9915 : 18/09/2017 : AB : START
            // get the Coolabrative Space of current User
            String strSecurityContext = PersonUtil.getDefaultSecurityContext(context, context.getUser());
            String strCollaborativeSpace = (strSecurityContext.split("[.]")[2]);

            String sCreateLink = "../common/emxCreate.jsp?nameField=autoname&form=type_PSS_CreatePart&type=type_Part"
                    + "&multiPartCreation=false&preProcessJavaScript=preProcessInCreatePart&suiteKey=EngineeringCentral"
                    + "&typeChooser=true&InclusionList=type_Part&ExclusionList=type_ManufacturingPart"
                    + "&postProcessURL=../engineeringcentral/PSS_EngineeringCentralCommonProcess.jsp?PSS_mode=PSS_createNewPart";

            String sLinkPrefix = "<a class=\"object\" href=\"javaScript:emxTableColumnLinkClick('" + sCreateLink;
            String sLinkValueSuffix = "', '780', '500', 'true', 'popup', '')";

            String sLinkSuffix = "\">";
            String sLinkImage = "<img border=\"0\" title=\"Create new Part\" src=\"../common/images/iconActionCreateNewPart.gif\" onclick=createNewPart();>";
            String sLinkEnd = "</a>";

            String strSubmitURL = "../engineeringcentral/PSS_EngineeringCentralCommonProcess.jsp?PSS_mode=PSS_getPartIdfromSearch";

            String strSearchURL = "../common/emxFullSearch.jsp?field=TYPES=type_Part:CURRENT=policy_PSS_EC_Part.state_Preliminary,policy_PSS_Development_Part.state_Create:PROJECT="
                    + strCollaborativeSpace + "&showInitialResults=false&table=AEFGeneralSearchResults&selection=single&showSavedQuery=True&searchCollectionEnabled=True";

            // TIGTK-9915 : 18/09/2017 : AB : END

            StringBuffer outPut = null;
            Map objectMap = null;
            String sPartObjId = "";
            String sPartObjName = "";
            String sIsSynchronized = "";
            String sPartObjectType = "";

            int index = 0;

            Iterator objectListItr = objectList.iterator();
            while (objectListItr.hasNext()) {
                objectMap = (Map) objectListItr.next();

                sCADObjectId = (String) objectMap.get(SELECT_ID);
                sPartObjId = (String) objectMap.get("PartObjectId");
                sIsSynchronized = (String) objectMap.get("IsSynchronize");
                sPartObjName = (String) objectMap.get("PartObjectName");
                sPartObjectType = (String) objectMap.get("PartObjectType");

                if (sPartObjName == null) {
                    sPartObjName = "";
                }

                outPut = new StringBuffer();
                outPut.append("<script language=\"javascript\" src=\"../common/scripts/emxUIModal.js\"></script>");
                outPut.append("<script language=\"javascript\" src=\"../common/scripts/emxUITableUtil.js\"></script>");
                // Find Bug : Dodgy Code : PS :21-March-2017 : START
                outPut.append("<input type=\"text\" name=\"PartObjectName\"  size=\"9\" value=\"");
                outPut.append(sPartObjName);
                outPut.append("\"");
                // Find Bug : Dodgy Code : PS :21-March-2017 : END
                outPut.append(" >&nbsp;");

                outPut.append("<input class=\"button\" type=\"button\"");
                outPut.append(" name=\"btnPartObjectChooser\" size=\"6\" ");

                outPut.append("value=\"...\" alt=\"\"  onClick=\"javascript:showChooser('");
                // Find Bug : Dodgy Code : PS :21-March-2017 : START
                outPut.append(strSearchURL);
                outPut.append("&submitURL=");
                outPut.append(strSubmitURL);
                outPut.append("&PSS_Index=");
                outPut.append(index);
                outPut.append("&cadObjectId=");
                outPut.append(sCADObjectId);
                outPut.append("', 700, 500)\">&nbsp;");
                outPut.append("<a href=\"javaScript:updatePartRevisions(");
                outPut.append(index);
                outPut.append(")");

                outPut.append("\">");

                outPut.append("<img border=\"0\" title=\"Update Part revision\" src=\"../common/images/iconActionReset.gif\">");
                outPut.append("</a>");

                outPut.append("<a href=\"JavaScript:clearTableCellTextEdit(");
                outPut.append(index);
                outPut.append(",");
                outPut.append(aCulumnsTextEdit);
                outPut.append(");");

                outPut.append("clearTableCell(");
                outPut.append(index);
                outPut.append(",");
                outPut.append(aCulumnsText);
                outPut.append(");");

                outPut.append("\">");

                outPut.append("<img border=\"0\" title=\"Reset Fields\" src=\"../common/images/iconActionRefresh.gif\">");
                outPut.append("</a>");

                outPut.append(sLinkPrefix);
                outPut.append("&PSS_Index=");
                outPut.append(index);
                outPut.append(sLinkValueSuffix);
                outPut.append(sLinkSuffix);
                outPut.append(sLinkImage);
                outPut.append(sLinkEnd);

                outPut.append("<input type=\"hidden\" name=\"PartObjectId\" value=\"");
                outPut.append(sPartObjId);
                outPut.append("\"/>");
                // Find Bug : Dodgy Code : PS :21-March-2017 : END
                if ("true".equals(sIsSynchronized)) {
                    outPut.append("<input type=\"hidden\" name=\"SynchronizedPartObjectId\" value=\"");
                    outPut.append(sPartObjId);
                    outPut.append("\"/>");
                } else {
                    outPut.append("<input type=\"hidden\" name=\"SynchronizedPartObjectId\" value=\"");
                    outPut.append("\"/>");
                }
                outPut.append("</NOBR>");

                vPartObjNames.add(outPut.toString());
                index++;
            }

            return vPartObjNames;
        } catch (Exception ex) {
            logger.error("Error in getPartObjectName ", ex);
            throw ex;
        }
    }

    /**
     * Return the Part Object Revision column for the table PSS_SynchronizationCADObjectEBOMSummary
     * @param context
     *            : the eMatrix <code>Context</code> object
     * @param args
     *            : holds no arguments
     * @return
     * @throws Exception
     */
    public Vector<String> getPartObjectRevisionCADToBOM(Context context, String[] args) throws Exception {

        try {
            Vector<String> vPartObjRevisions = new Vector<String>();
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");

            StringBuffer outPut = null;
            Map objectMap = null;
            String sPartId = null;
            String sPartRev = null;
            String sCADObjectId = null;
            MapList mlPartRevisions = null;

            int index = 0;
            Iterator objectListItr = objectList.iterator();
            while (objectListItr.hasNext()) {
                objectMap = (Map) objectListItr.next();

                sPartId = (String) objectMap.get("PartObjectId");
                sPartRev = (String) objectMap.get("PartObjectRevision");
                sCADObjectId = (String) objectMap.get(SELECT_ID);
                logger.debug("getPartObjectRevision() - sPartId:<" + sPartId + "> sPartRev:<" + sPartRev + "> sCADObjectId:<" + sCADObjectId + ">");
                // TIGTK-17791 : stembulkar : start
                MapList mlFinalList = new MapList();
                // TIGTK-17791 : stembulkar : end

                // get Part object revisions
                mlPartRevisions = getPartRevisions(context, sPartId);
                // TIGTK-17791 : stembulkar : start
                for (int i = 0; i < mlPartRevisions.size(); i++) {
                    Map partRev = (Map) mlPartRevisions.get(i);
                    String sObjId = (String) partRev.get(SELECT_ID);
                    DomainObject dob = DomainObject.newInstance(context, sObjId);
                    String sCurrent = (String) dob.getCurrentState(context).getName();
                    if (!sCurrent.equals(TigerConstants.STATE_OBSOLETE) && !sCurrent.equals(TigerConstants.STATE_PSS_CANCELPART_CANCELLED)) {
                        mlFinalList.add(partRev);
                    }
                }

                // construct Select list field from revisions list
                outPut = constructPartRevisionsSelectList(context, mlFinalList, sPartRev, sCADObjectId, Integer.toString(index));
                // TIGTK-17791 : stembulkar : end

                vPartObjRevisions.add(outPut.toString());
                index++;
            }

            return vPartObjRevisions;
        } catch (Exception ex) {
            logger.error("Error in getPartObjectRevision ", ex);
            throw ex;
        }
    }

    /**
     * Return informations of all revisions of the Part
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sCADObjId
     *            CAD Object id
     * @return
     * @throws Exception
     */
    private MapList getPartRevisions(Context context, String sPartObjId) throws Exception {
        MapList mlPartRevisions = new MapList();

        if (UIUtil.isNotNullAndNotEmpty(sPartObjId)) {
            SelectList slSelectStmts = new SelectList();
            slSelectStmts.addElement(SELECT_ID);
            slSelectStmts.addElement(SELECT_NAME);
            slSelectStmts.addElement(SELECT_TYPE);
            slSelectStmts.addElement(SELECT_REVISION);
            slSelectStmts.addElement(SELECT_DESCRIPTION);
            slSelectStmts.addElement(SELECT_POLICY);
            slSelectStmts.addElement("last");

            DomainObject dobPart = DomainObject.newInstance(context, sPartObjId);
            mlPartRevisions = dobPart.getRevisionsInfo(context, slSelectStmts, new StringList());
        }
        logger.debug("getPartRevisions() - mlPartRevisions = <" + mlPartRevisions + ">");

        return mlPartRevisions;
    }

    /**
     * Construct a HTML select list from a list of Part object revisions
     * @param mlPartRevisions
     *            Part object revisions list
     * @param sPartRev
     *            Selected Part object revision
     * @param sFieldNumber
     *            the table row number in the synchronization window
     * @return
     * @throws Exception
     */
    private StringBuffer constructPartRevisionsSelectList(Context context, MapList mlPartRevisions, String sPartRev, String sCADObjectId, String sFieldNumber) throws Exception {
        try {
            logger.debug("constructPartRevisionsSelectList() start");
            StringBuffer sbOptionValues = new StringBuffer("");
            String sSelected = "";
            Map mPart = null;
            String sId = null;
            String sName = null;
            String sRevision = null;
            String sLastRevision = null;

            String sDescription = null;

            StringList slSelect = new StringList();
            slSelect.addElement(SELECT_CURRENT);

            if (mlPartRevisions == null || mlPartRevisions.size() == 0) {
                // START:TIGTK-4153:Modified on 8/2/2017
                StringBuffer outPut = new StringBuffer("");
                outPut.append("<select name='PartObjectRevisions'>");
                // Find Bug : Dodgy Code : PS : 21-March-2017 : START
                outPut.append("<option ");
                outPut.append(sSelected);
                outPut.append(" value='");
                outPut.append("");
                outPut.append("'>");
                outPut.append("");
                outPut.append("</option>");
                // Find Bug : Dodgy Code : PS : 21-March-2017 : END
                outPut.append("</select>");
                return outPut;
                // END:TIGTK-4153:Modified on 8/2/2017
            }

            if (sCADObjectId != null && !"".equals(sCADObjectId)) {
                BusinessObject busObj = new BusinessObject(sCADObjectId);
                if (busObj.exists(context)) {

                }
            }
            StringBuffer sbValue = null;
            for (Iterator iterator = mlPartRevisions.iterator(); iterator.hasNext();) {
                mPart = (Map) iterator.next();
                sId = (String) mPart.get(SELECT_ID);

                sName = (String) mPart.get(SELECT_NAME);
                sRevision = (String) mPart.get(SELECT_REVISION);
                sLastRevision = (String) mPart.get("last");
                logger.debug("constructPartRevisionsSelectList() - sId=<" + sId + "> sRevision=<" + sRevision + "> sLastRevision=<" + sLastRevision + ">");
                sDescription = (String) mPart.get(SELECT_DESCRIPTION);

                if (UIUtil.isNotNullAndNotEmpty(sId)) {
                    sbValue = new StringBuffer(sId);
                } else {
                    sbValue = new StringBuffer();
                }
                // Find Bug : Dodgy Code : PS : 21-March-2017 : START
                sbValue.append("|");
                sbValue.append(sName);
                sbValue.append("|");
                sbValue.append(sDescription);
                sbValue.append("|");
                sbValue.append(sFieldNumber);
                // Find Bug : Dodgy Code : PS : 21-March-2017 : END

                logger.debug("constructPartRevisionsSelectList() - sbValue = <" + sbValue + ">");
                if (sPartRev != null) {
                    if (sRevision.equals(sPartRev)) {
                        sSelected = "selected";
                    } else {
                        sSelected = "";
                    }
                } else {
                    if (sRevision.equals(sLastRevision)) {
                        sSelected = "selected";
                    } else {
                        sSelected = "";
                    }
                }

                // Find Bug : Dodgy Code : PS : 21-March-2017 : START
                sbOptionValues.append("<option ");
                sbOptionValues.append(sSelected);
                sbOptionValues.append(" value='");
                sbOptionValues.append(sbValue);
                sbOptionValues.append("'>");
                sbOptionValues.append(sRevision);
                sbOptionValues.append("</option>");
                // Find Bug : Dodgy Code : PS : 21-March-2017 : END

            }

            StringBuffer outPut = new StringBuffer("");
            // Find Bug : Dodgy Code : PS : 21-March-2017 : START
            outPut.append("<select name='PartObjectRevisions' onChange='javascript:changeSelectedPartRevisionfromDocumentWindow(");
            outPut.append(sFieldNumber);
            outPut.append(")'>");
            // Find Bug : Dodgy Code : PS : 21-March-2017 : END
            outPut.append(sbOptionValues.toString());
            outPut.append("</select>");

            return outPut;
        } catch (Exception e) {
            logger.error("Error in constructPartRevisionsSelectList()\n", e);
            throw e;
        }
    }

    /**
     * Returns info to fill column of table
     * @param context
     *            : the eMatrix <code>Context</code> object
     * @param args
     * @return
     * @returns : Object of type Vector
     * @throws Exception
     */
    public static Vector<String> getInfoFromMapList(Context context, String[] args) throws Exception {
        Vector<String> vResult = new Vector<String>();
        Vector<String> vLevels = new Vector<String>();
        String sLanguage = context.getSession().getLanguage();
        try {
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");

            HashMap<?, ?> hmColumnMap = (HashMap<?, ?>) programMap.get("columnMap");
            HashMap<?, ?> hmSettings = (HashMap<?, ?>) hmColumnMap.get("settings");

            String strObjectId = "";
            String sInfo = (String) hmSettings.get("PSS_Info");

            String sColumnType = (String) hmSettings.get("Column Type");
            logger.debug("getInfoFromMapList() - sInfo = <" + sInfo + ">");
            StringList slParamValue = null;
            Iterator<?> objectListItr = objectList.iterator();
            String sCADObjectLevel = "";

            while (objectListItr.hasNext()) {
                Map<?, ?> objectMap = (Map<?, ?>) objectListItr.next();

                strObjectId = (String) objectMap.get(DomainConstants.SELECT_ID);
                DomainObject domObj = DomainObject.newInstance(context, strObjectId);

                // get parameter value from Map

                slParamValue = toStringList(objectMap.get(sInfo));

                StringList slTypeNameDisplayList = new StringList();

                if (sInfo.equals("level")) {
                    sCADObjectLevel = (String) objectMap.get(SELECT_LEVEL);
                    vLevels.add(sCADObjectLevel);

                }

                if (sInfo.equals("type")) {
                    for (int i = 0; i < slParamValue.size(); i++) {
                        String sTypeString = EnoviaResourceBundle.getTypeI18NString(context, (String) slParamValue.get(0), sLanguage);
                        slTypeNameDisplayList.add(sTypeString);
                    }
                }

                String sColor = (String) objectMap.get("Color");
                if (sInfo.equals("type")) {
                    if ("programHTMLOutput".equals(sColumnType)) {
                        vResult.add(returnColorizedLine(sColor, join(slTypeNameDisplayList, ", ")));
                    } else {
                        vResult.add(join(slTypeNameDisplayList, ", "));
                    }
                } else if (sInfo.equals("level")) {
                    if ("programHTMLOutput".equals(sColumnType)) {
                        vResult = vLevels;
                    }
                } else {
                    if ("programHTMLOutput".equals(sColumnType)) {
                        vResult.add(returnColorizedLine(sColor, join(slParamValue, ", ")));
                    } else {
                        vResult.add(join(slParamValue, ", "));
                    }
                }
            }

        } catch (Exception ex) {
            logger.error("Error in getInfoFromMapList()\n", ex);
            throw ex;
        }

        return vResult;
    }

    /**
     * Return info to fill the table PSS_SynchronizationCADObjectEBOMSummary
     * @param context
     *            : the eMatrix <code>Context</code> object
     * @param args
     *            : ID of the CAD Definition
     * @return
     * @throws Exception
     * @plm.usage table PSS_SynchronizationCADObjectEBOMSummary
     */
    public MapList getCADBOM(Context context, String[] args, boolean doExpand) throws Exception {
        logger.debug("getCADBOM() - start ...");
        long lStartTime = System.currentTimeMillis();
        try {
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            String sCADObjectId = (String) paramMap.get("objectId");
            logger.debug("getCADBOM() - sCADObjectId:<" + sCADObjectId + ">");

            SelectList slSelectStmts = new SelectList();
            slSelectStmts.addElement(SELECT_LEVEL);
            slSelectStmts.addElement(SELECT_ID);
            slSelectStmts.addElement(SELECT_TYPE);
            slSelectStmts.addElement(SELECT_NAME);
            slSelectStmts.addElement(SELECT_REVISION);
            slSelectStmts.addElement(SELECT_CURRENT);
            // TIGTK-8512 - 2017-07-10 - VP - START
            slSelectStmts.addElement("project");
            // TIGTK-8512 - 2017-07-10 - VP - END
            slSelectStmts.addElement("from[" + TigerConstants.RELATIONSHIP_VERSIONOF + "].to.id");
            slSelectStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE + "]");
            slSelectStmts.addElement("to[" + RELATIONSHIP_PART_SPECIFICATION + "]");

            SelectList selectRelStmts = new SelectList();

            // get CAD Object BOM
            MapList bomList = getCADObjectBOM(context, sCADObjectId, slSelectStmts, selectRelStmts, doExpand);

            // retrieve all Parts and CAD Definition Objects and exclude BD
            // objects
            bomList = getRelatedParts(context, bomList);
            logger.debug("getCADBOM() - end.");
            logger.debug("\n\n getCADBOM - END - " + (System.currentTimeMillis() - lStartTime));
            int rowIndex = 0;
            for (Iterator iterator = bomList.iterator(); iterator.hasNext();) {
                Map mObj = (Map) iterator.next();
                mObj.put(SELECT_RELATIONSHIP_ID, Integer.toString(rowIndex));
                rowIndex++;
            }
            return bomList;
        } catch (Exception ex) {
            logger.error("Error in getCADBOM()\n", ex);
            throw ex;
        }
    }

    /**
     * Return info to fill the table PSS_SynchronizationCADObjectEBOMSummary
     * @param context
     *            : the eMatrix <code>Context</code> object
     * @param args
     *            : ID of the CAD Definition
     * @return
     * @throws Exception
     * @plm.usage table PSS_SynchronizationCADObjectEBOMSummary
     */

    public MapList getCADBOM(Context context, String[] args) throws Exception {
        return getCADBOM(context, args, true);
    }

    /**
     * Get CAD object BOM
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sCADObjectId
     *            CAD object Id
     * @param slSelectStmts
     *            select list on types
     * @param selectRelStmts
     *            select list on relations
     * @return
     * @throws Exception
     */
    public static MapList getCADObjectBOM(Context context, String sCADObjectId, SelectList slSelectStmts, SelectList selectRelStmts) throws Exception {
        return getCADObjectBOM(context, sCADObjectId, slSelectStmts, selectRelStmts, true);
    }

    /**
     * Get CAD object BOM
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sCADObjectId
     *            CAD object Id
     * @param slSelectStmts
     *            select list on types
     * @param selectRelStmts
     *            select list on relations
     * @param doExpand
     *            : indicates if the process should perform the expand on the CAD BOM connection
     * @return
     * @throws Exception
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static MapList getCADObjectBOM(Context context, String sCADObjectId, SelectList slSelectStmts, SelectList selectRelStmts, boolean doExpand) throws Exception {
        logger.debug("getCADObjectBOM() - start ...");
        try {
            DomainObject dobCADObj = DomainObject.newInstance(context, sCADObjectId);

            MapList mlBOMList = new MapList();
            int iLevel = 0;
            String sParentLevel = Integer.toString(iLevel);
            Map mFirstCADObj = dobCADObj.getInfo(context, slSelectStmts);
            mFirstCADObj.put(SELECT_LEVEL, sParentLevel);
            mFirstCADObj.put("LEVEL_POSITION", sParentLevel);
            mFirstCADObj.put("PARENT", "");

            if (!slSelectStmts.contains("from[" + TigerConstants.RELATIONSHIP_VERSIONOF + "].to.id")) {
                slSelectStmts.addElement("from[" + TigerConstants.RELATIONSHIP_VERSIONOF + "].to.id");
            }

            // check first element type
            if (dobCADObj.isKindOf(context, TigerConstants.TYPE_MCADDRAWING)) {
                mlBOMList.add(mFirstCADObj);
                iLevel++;
                StringBuilder sbDrawingChildren = new StringBuilder();

                // get related elements
                DomainObject dobAssociatedElement = DomainObject.newInstance(context);
                Map mAssociatedElement = null;
                String sIdNonVersioned = null;
                StringList slAssociatedElementIds = getAssociatedElementIdsToTheDrawing(context, dobCADObj);
                for (Iterator iterator = slAssociatedElementIds.iterator(); iterator.hasNext();) {
                    String sAssociatedElementId = (String) iterator.next();
                    logger.debug("getCADObjectBOM() - sAssociatedElementId = <" + sAssociatedElementId + ">");
                    dobAssociatedElement.setId(sAssociatedElementId);
                    // get associated element
                    mAssociatedElement = dobAssociatedElement.getInfo(context, slSelectStmts);
                    // get Id of associated element
                    sIdNonVersioned = (String) mAssociatedElement.get("from[" + TigerConstants.RELATIONSHIP_VERSIONOF + "].to.id");
                    logger.debug("getCADObjectBOM() - sIdNonVersioned:<" + sIdNonVersioned + ">");
                    if (sIdNonVersioned != null && !"".equals(sIdNonVersioned)) {
                        dobAssociatedElement.setId(sIdNonVersioned);
                        mAssociatedElement = dobAssociatedElement.getInfo(context, slSelectStmts);
                        sbDrawingChildren.append("|");
                        sbDrawingChildren.append(sIdNonVersioned);
                    } else {
                        sbDrawingChildren.append("|");
                        sbDrawingChildren.append(sAssociatedElementId);
                    }

                    mAssociatedElement.put(SELECT_LEVEL, Integer.toString(iLevel));
                    mAssociatedElement.put("LEVEL_POSITION", sParentLevel + "_" + iLevel);
                    mAssociatedElement.put("PARENT", sCADObjectId);
                    // TIGTK-4143 - 29-03-2017 - VP :START
                    getChildrenCatBom(context, mAssociatedElement, slSelectStmts, selectRelStmts, mlBOMList, iLevel, new StringBuilder(), doExpand);
                    // TIGTK-4143 - 29-03-2017 - VP :END
                    sbDrawingChildren.append("|");
                    sbDrawingChildren.append((String) mAssociatedElement.get("CHILDREN"));
                }
                mFirstCADObj.put("CHILDREN", sbDrawingChildren.toString());
            } else {
                // TIGTK-4143 - 29-03-2017 - VP :START
                getChildrenCatBom(context, mFirstCADObj, slSelectStmts, selectRelStmts, mlBOMList, iLevel, new StringBuilder(), doExpand);
                // TIGTK-4143 - 29-03-2017 - VP :END
            }

            // display CAD Object only one time (if there is ore than one
            // connection to an object from the same object, we display only one
            // line for this one)
            // drawing object must be displayed one time
            MapList bomList = removeDuplicatedObjectsFromBOMList(context, mlBOMList);

            // sort BOM list
            bomList.addSortKey("LEVEL_POSITION", "ascending", "String");
            bomList.sort();

            Map mObj = null;
            int rowIndex = 0;
            for (Iterator iterator = bomList.iterator(); iterator.hasNext();) {
                mObj = (Map) iterator.next();
                mObj.put(SELECT_RELATIONSHIP_ID, Integer.toString(rowIndex));

                rowIndex++;
            }
            logger.debug("getCADObjectBOM() - bomList = <" + bomList + ">");
            logger.debug("getCADObjectBOM() - end.");
            return bomList;
        } catch (Exception ex) {
            logger.error("Error in getCADObjectBOM()\n", ex);
            throw ex;
        }

    }

    /**
     * Get objects list associated to the Drawing object
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param dobDrawing
     *            Drawing object
     * @return
     * @throws Exception
     */
    private static StringList getAssociatedElementIdsToTheDrawing(Context context, DomainObject dobDrawing) throws Exception {
        try {
            StringList slElementIds = dobDrawing.getInfoList(context, "to[" + TigerConstants.RELATIONSHIP_ASSOCIATEDDRAWING + "].from.id");
            if (slElementIds.size() == 0) {
                slElementIds = dobDrawing.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_ACTIVEVERSION + "].to.to[" + TigerConstants.RELATIONSHIP_ASSOCIATEDDRAWING + "].from.id");
            }
            logger.debug("getAssociatedElementIdsToTheDrawing() - slElementIds = <" + slElementIds + ">");
            return slElementIds;
        } catch (Exception e) {
            logger.error("Error in getAssociatedElementIdsToTheDrawing()\n", e);
            throw e;
        }
    }

    /**
     * Remove duplicated objects in the BOM list <br>
     * Keep CAD Object only one time (if there is ore than one connection to an object from the same object, we display only one line for this one)<br>
     * Drawing object must be displayed one time
     * @param mlBOMList
     *            CAD Objects BOM list
     * @return
     * @throws Exception
     */
    private static MapList removeDuplicatedObjectsFromBOMList(Context context, MapList mlBOMList) throws Exception {
        MapList bomList = new MapList();
        try {
            Map<String, StringList> mParentIds = new HashMap<String, StringList>();
            StringList slDrawingIds = new StringList();

            Map mCADObject = null;
            StringList slParentIds = null;
            String sParentId = null;
            String sType = null;
            String sId = null;
            for (Iterator iterator = mlBOMList.iterator(); iterator.hasNext();) {
                mCADObject = (Map) iterator.next();
                sParentId = (String) mCADObject.get("PARENT");
                sType = (String) mCADObject.get(SELECT_TYPE);
                sId = (String) mCADObject.get(SELECT_ID);
                DomainObject domCADObject = DomainObject.newInstance(context, sId);

                if (domCADObject.isKindOf(context, TigerConstants.TYPE_MCADDRAWING)) {
                    if (!slDrawingIds.contains(sId)) {
                        bomList.add(mCADObject);
                        slDrawingIds.addElement(sId);
                    }
                } else {
                    slParentIds = mParentIds.get(sId);
                    if (slParentIds == null || !slParentIds.contains(sParentId)) {
                        bomList.add(mCADObject);
                        if (slParentIds == null) {
                            slParentIds = new StringList();
                            mParentIds.put(sId, slParentIds);
                        }
                        slParentIds.addElement(sParentId);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error in removeDuplicatedObjectsFromBOMList()\n", e);
            throw e;
        }
        return bomList;
    }

    /**
     * Search recursively the children Cat BOM starting with the given CAD Object <mParentCADObj>
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param mParentCADObj
     *            Map containing information of the current CAD Object
     * @param slSelectObj
     *            list information to retrieve on business object
     * @param slSelectRel
     *            list information to retrieve on relationship
     * @param mlResult
     *            List of CAD Objects found (global list)
     * @param doCadBomExpand
     *            indicates if the process should perform the expand on the CAD BOM connection
     * @throws Exception
     */
    private static void getChildrenCatBom(Context context, Map mParentCADObj, SelectList slSelectObj, SelectList slSelectRel, MapList mlResult, int iLevel, StringBuilder sbChildrenIds,
            boolean doCadBomExpand) throws Exception {
        try {
            // TIGTK-4143 - 29-03-2017 - VP :START
            mlResult.add(mParentCADObj);
            String sCatElementId = (String) mParentCADObj.get(SELECT_ID);
            String sParentLevel = (String) mParentCADObj.get("LEVEL_POSITION");
            String sParentRevision = (String) mParentCADObj.get(SELECT_REVISION);
            if (sParentRevision == null) {
                sParentRevision = "";
            }
            DomainObject dob = DomainObject.newInstance(context, sCatElementId);

            MapList hsResult = new MapList();
            String sRelPattern = TigerConstants.RELATIONSHIP_ASSOCIATEDDRAWING;
            if (doCadBomExpand) {
                sRelPattern += "," + TigerConstants.RELATIONSHIP_CADSUBCOMPONENT;
            }

            MapList mlChildren = dob.getRelatedObjects(context, sRelPattern, "*", slSelectObj, slSelectRel, false, true, (short) 1, "", "", 0);
            hsResult.addAll(mlChildren);

            if (mlChildren.isEmpty()) {
                // get versioned id
                String sVersionedId = dob.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_VERSIONOF + "].from.id");
                logger.debug("getChildrenCatBom() - sCatElementId = <" + sCatElementId + "> sVersionedId = <" + sVersionedId + ">");
                if (sVersionedId != null && !"".equals(sVersionedId)) {

                    dob.setId(sVersionedId);
                    MapList mlChildrenVersioned = dob.getRelatedObjects(context, sRelPattern, "*", slSelectObj, slSelectRel, false, true, (short) 1, "", "", 0);
                    hsResult.addAll(mlChildrenVersioned);
                }
            }

            hsResult.addSortKey(KEY_RELATIONSHIP, "ascending", "String");
            hsResult.sort();
            logger.debug("getChildrenCatBom() - hsResult:<" + hsResult + ">");

            int i = 1;
            int iNewLevel = iLevel + 1;

            Map mCADObj = null;
            Map mInfoNonVersioned = null;
            String sIdNonVersionedObject = "";
            for (Iterator it = hsResult.iterator(); it.hasNext();) {
                mCADObj = (Map) it.next();
                sIdNonVersionedObject = (String) mCADObj.get("from[" + TigerConstants.RELATIONSHIP_VERSIONOF + "].to.id");
                logger.debug("getChildrenCatBom() - sIdNonVersionedObject:<" + sIdNonVersionedObject + ">");
                StringBuilder sbChilds = new StringBuilder();
                if (sIdNonVersionedObject != null && !"".equals(sIdNonVersionedObject)) {
                    dob.setId(sIdNonVersionedObject);
                    mInfoNonVersioned = dob.getInfo(context, slSelectObj);
                    sbChildrenIds.append("|");
                    sbChildrenIds.append((String) mInfoNonVersioned.get(SELECT_ID));
                    mInfoNonVersioned.put(SELECT_LEVEL, Integer.toString(iNewLevel));
                    mInfoNonVersioned.put("LEVEL_POSITION", sParentLevel + "_" + i++);
                    mInfoNonVersioned.put("PARENT", sCatElementId);
                    mInfoNonVersioned.put("PARENTREVISION", sParentRevision);
                    getChildrenCatBom(context, mInfoNonVersioned, slSelectObj, slSelectRel, mlResult, iNewLevel, sbChilds, doCadBomExpand);
                    sbChilds.append("|");
                    sbChilds.append((String) mInfoNonVersioned.get("CHILDREN"));
                    if (UIUtil.isNotNullAndNotEmpty(sbChilds.toString())) {
                        sbChildrenIds.append("|");
                        sbChildrenIds.append(sbChilds);
                    }
                } else {
                    sbChildrenIds.append("|");
                    sbChildrenIds.append((String) mCADObj.get(SELECT_ID));
                    mCADObj.put(SELECT_LEVEL, Integer.toString(iNewLevel));
                    mCADObj.put("LEVEL_POSITION", sParentLevel + "_" + i++);
                    mCADObj.put("PARENT", sCatElementId);
                    mCADObj.put("PARENTREVISION", sParentRevision);
                    getChildrenCatBom(context, mCADObj, slSelectObj, slSelectRel, mlResult, iNewLevel, sbChilds, doCadBomExpand);
                    sbChilds.append("|");
                    sbChilds.append((String) mCADObj.get("CHILDREN"));
                    if (UIUtil.isNotNullAndNotEmpty(sbChilds.toString())) {
                        sbChildrenIds.append("|");
                        sbChildrenIds.append(sbChilds);
                    }
                }

            }
            logger.debug("getChildrenCatBom() - sChildrenIds = <" + sbChildrenIds.toString() + ">");
            mParentCADObj.put("CHILDREN", sbChildrenIds.toString());
            logger.debug("getChildrenCatBom() - mParentCADObj = <" + mParentCADObj + ">");
            // TIGTK-4143 - 29-03-2017 - VP :END
        } catch (FrameworkException e) {
            logger.error("Error in getChildrenCatBom()\n", e);
            throw e;
        } catch (Exception e) {
            logger.error("Error in getChildrenCatBom()\n", e);
            throw e;
        }
    }

    /**
     * Get related CAD Definition
     * @param context
     *            : the eMatrix <code>Context</code> object
     * @param ebomList
     *            : EBOM list
     * @return
     * @throws Exception
     */

    private MapList getRelatedParts(Context context, MapList bomList) throws Exception {
        logger.debug("getRelatedParts() - bomList = " + bomList);
        MapList mlFinal = new MapList();
        String sLanguage = context.getSession().getLanguage();

        try {
            SelectList slSelectStmts = new SelectList();
            slSelectStmts.addElement(SELECT_ID);
            slSelectStmts.addElement(SELECT_TYPE);
            slSelectStmts.addElement(SELECT_NAME);
            slSelectStmts.addElement(SELECT_REVISION);
            slSelectStmts.addElement(SELECT_DESCRIPTION);
            slSelectStmts.addElement(SELECT_CURRENT);
            slSelectStmts.addElement("to[" + RELATIONSHIP_PART_SPECIFICATION + "]");
            // TIGTK-8512 - 2017-07-10 - VP - START
            slSelectStmts.addElement("to[" + RELATIONSHIP_CLASSIFIED_ITEM + "].from.name");
            slSelectStmts.addElement("to[" + RELATIONSHIP_CLASSIFIED_ITEM + "].from.id");
            // TIGTK-8512 - 2017-07-10 - VP - END

            SelectList slRelStmts = new SelectList();

            Map mCADObj = null;
            Map mCADObjFinal = null;
            Map mPart = null;
            MapList mParts = null;
            String sCADObjId = "";
            String sCADObjName = "";
            String sCADObjRevision = "";
            String sCADObjectType = "";
            String sPartObjID;
            String sPartObjType;
            String sPartObjName;
            String sPartObjCurrent;
            String sPartObjRev;
            String sPartObjDesc;
            String sIsSynchronize;
            String sCADObjectRelationshipPartSpecification;
            String sGeometryType = null;
            // TIGTK-8512 - 2017-07-10 - VP - START
            String sPartFamilyId = "";
            String sPartFamilyName = "";
            // TIGTK-8512 - 2017-07-10 - VP - END
            DomainObject doCADObject = DomainObject.newInstance(context);
            for (Iterator iterator = bomList.iterator(); iterator.hasNext();) {
                mCADObj = (Map) iterator.next();
                sGeometryType = (String) mCADObj.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE + "]");

                sCADObjId = (String) mCADObj.get(SELECT_ID);
                sCADObjName = (String) mCADObj.get(SELECT_NAME);
                sCADObjRevision = (String) mCADObj.get(SELECT_REVISION);
                sCADObjectType = (String) mCADObj.get(SELECT_TYPE);
                sCADObjectRelationshipPartSpecification = (String) mCADObj.get("to[" + RELATIONSHIP_PART_SPECIFICATION + "]");

                logger.debug("getRelatedParts() - sCADObjId=<" + sCADObjId + "> CAD Object:<" + sCADObjectType + ", " + sCADObjName + ", " + sCADObjRevision + ">");

                sPartObjID = "";
                sPartObjType = "";
                sPartObjName = "";
                sPartObjRev = "";
                sPartObjCurrent = "";
                sPartObjDesc = "";
                sIsSynchronize = "false";
                // TIGTK-8512 - 2017-07-10 - VP - START
                sPartFamilyId = "";
                sPartFamilyName = "";
                // TIGTK-8512 - 2017-07-10 - VP - END
                // get already synchronized Part id
                logger.debug("getRelatedParts() - sCADObjectRelationshipPartSpecification = <" + sCADObjectRelationshipPartSpecification + ">");
                // Initialize the mapList to empty list
                mParts = new MapList();

                if ("true".equalsIgnoreCase(sCADObjectRelationshipPartSpecification)) {
                    sIsSynchronize = "true";

                    doCADObject.setId(sCADObjId);
                    MapList mSynchronizedParts = getAllLinkedPartsInfo(context, doCADObject);
                    for (Object oSynchronizedPart : mSynchronizedParts) {
                        Map mSynchronizedPart = (Map) oSynchronizedPart;
                        mParts.add(mSynchronizedPart);
                    }
                } else {
                    // get the same Part linked to the 2D or 3D object linked by
                    // the current object
                    mPart = getPartFromRelated2D3D(context, sCADObjId, slSelectStmts, slRelStmts);
                    if (mPart != null) {
                        mParts.add(mPart);
                    } else {
                        // get the last revision of the Part linked to the
                        // previous revision of the CAD Object
                        mPart = getPartFromPreviousRevision(context, sCADObjId, slSelectStmts);
                        if (mPart == null) {
                            // get Part from CAD Object name
                            mPart = getPartFromName(context, sCADObjName, slSelectStmts);
                        }
                        if (mPart != null) {
                            mParts.add(mPart);
                        }
                    }
                }

                if (mParts.isEmpty()) {
                    mCADObjFinal = new HashMap();
                    mCADObjFinal.putAll(mCADObj);
                    mCADObjFinal.put("PartObjectId", "");
                    mCADObjFinal.put("PartObjectType", "");
                    mCADObjFinal.put("PartObjectName", "");
                    mCADObjFinal.put("PartObjectRevision", "");
                    mCADObjFinal.put("PartObjectCurrent", "");
                    mCADObjFinal.put("PartObjectDescription", "");
                    mCADObjFinal.put("IsSynchronize", sIsSynchronize);
                    // TIGTK-8512 - 2017-07-10 - VP - START
                    mCADObjFinal.put("PartFamilyId", "");
                    mCADObjFinal.put("PartFamilyName", "");
                    // TIGTK-8512 - 2017-07-10 - VP - END
                    mlFinal.add(mCADObjFinal);
                } else {
                    Iterator iteratorBis = mParts.iterator();
                    while (iteratorBis.hasNext()) {
                        mPart = (Map) iteratorBis.next();
                        mCADObjFinal = new HashMap();
                        mCADObjFinal.putAll(mCADObj);

                        // TIGTK-11080 : START
                        sPartFamilyId = "";
                        sPartFamilyName = "";
                        // TIGTK-11080 : END
                        if (mPart != null) {
                            sPartObjID = (String) mPart.get(SELECT_ID);
                            sPartObjType = (String) mPart.get(SELECT_TYPE);
                            sPartObjType = i18nNow.getTypeI18NString(sPartObjType, sLanguage);
                            sPartObjName = (String) mPart.get(SELECT_NAME);
                            sPartObjRev = (String) mPart.get(SELECT_REVISION);
                            sPartObjCurrent = (String) mPart.get(SELECT_CURRENT);
                            sPartObjDesc = (String) mPart.get(SELECT_DESCRIPTION);
                            // TIGTK-8512 - 2017-07-10 - VP - START
                            Object oPartFamilyInfo = (Object) mPart.get("to[" + RELATIONSHIP_CLASSIFIED_ITEM + "].from.id");
                            if (oPartFamilyInfo instanceof String) {
                                sPartFamilyId = (String) mPart.get("to[" + RELATIONSHIP_CLASSIFIED_ITEM + "].from.id");
                                sPartFamilyName = (String) mPart.get("to[" + RELATIONSHIP_CLASSIFIED_ITEM + "].from.name");
                            } else if (oPartFamilyInfo instanceof StringList) {
                                StringList slPartFamilyInfo = (StringList) mPart.get("to[" + RELATIONSHIP_CLASSIFIED_ITEM + "].from.id");

                                StringBuilder sbPartFamilyInfo = new StringBuilder();
                                for (int i = 0; i < slPartFamilyInfo.size(); i++) {
                                    sbPartFamilyInfo.append((String) slPartFamilyInfo.getElement(i)).append(",");
                                }
                                sPartFamilyId = sbPartFamilyInfo.toString();
                                sbPartFamilyInfo = new StringBuilder();
                                slPartFamilyInfo = (StringList) mPart.get("to[" + RELATIONSHIP_CLASSIFIED_ITEM + "].from.name");
                                sPartFamilyName = "";
                                for (int i = 0; i < slPartFamilyInfo.size(); i++) {
                                    sbPartFamilyInfo.append((String) slPartFamilyInfo.getElement(i)).append(",");
                                }
                                sPartFamilyName = sbPartFamilyInfo.toString();
                            }
                            // TIGTK-8512 - 2017-07-10 - VP - END
                        }

                        mCADObjFinal.put("PartObjectId", sPartObjID);
                        mCADObjFinal.put("PartObjectType", sPartObjType);
                        mCADObjFinal.put("PartObjectName", sPartObjName);
                        mCADObjFinal.put("PartObjectRevision", sPartObjRev);
                        mCADObjFinal.put("PartObjectCurrent", sPartObjCurrent);
                        mCADObjFinal.put("PartObjectDescription", sPartObjDesc);
                        mCADObjFinal.put("IsSynchronize", sIsSynchronize);
                        // TIGTK-8512 - 2017-07-10 - VP - START
                        mCADObjFinal.put("PartFamilyId", sPartFamilyId);
                        mCADObjFinal.put("PartFamilyName", sPartFamilyName);
                        // TIGTK-8512 - 2017-07-10 - VP - END
                        logger.debug("getRelatedParts() - Add Part Object in the display list");
                        logger.debug("getRelatedParts() - mCADObjFinal = <" + mCADObjFinal + ">");

                        mlFinal.add(mCADObjFinal);
                    }
                }

            }
            logger.debug("getRelatedParts() - mlFinal:<" + mlFinal + ">");
        } catch (Exception ex) {
            logger.error("Error in getRelatedParts()\n", ex);
            throw ex;
        }
        logger.debug("getRelatedParts() - end.");
        return mlFinal;
    }

    /**
     * Return all Part linked to the current CAD object
     * @param context
     * @param doCADObject
     * @return
     * @throws Exception
     */
    protected MapList getAllLinkedPartsInfo(Context context, DomainObject doCADObject) throws Exception {
        MapList mlLinkedParts = null;
        try {
            logger.debug("getLinkedPartsInfo() - doCADObject = <" + doCADObject + ">");
            StringList slSelectStmts = new SelectList();
            slSelectStmts.addElement(SELECT_ID);
            slSelectStmts.addElement(SELECT_NAME);
            slSelectStmts.addElement(SELECT_TYPE);
            slSelectStmts.addElement(SELECT_REVISION);
            slSelectStmts.addElement(SELECT_DESCRIPTION);
            slSelectStmts.addElement(SELECT_CURRENT);
            // TIGTK-8512 - 2017-07-10 - VP - START
            slSelectStmts.addElement("to[" + RELATIONSHIP_CLASSIFIED_ITEM + "].from.name");
            slSelectStmts.addElement("to[" + RELATIONSHIP_CLASSIFIED_ITEM + "].from.id");
            // TIGTK-8512 - 2017-07-10 - VP - END
            StringList slSelectRelationship = new StringList();
            slSelectRelationship.addElement(SELECT_RELATIONSHIP_ID);
            // TIGTK-17791 : stembulkar : start
            mlLinkedParts = doCADObject.getRelatedObjects(context, RELATIONSHIP_PART_SPECIFICATION, TYPE_PART, slSelectStmts, slSelectRelationship, true, false, (short) 1,
                    "current != " + TigerConstants.STATE_OBSOLETE + " && current != " + TigerConstants.STATE_PSS_CANCELPART_CANCELLED, "", 0);
            // TIGTK-17791 : stembulkar : end
            logger.debug("getLinkedPartsInfo() - mlLinkedParts.size() : < " + mlLinkedParts.size() + " >");

        } catch (Exception e) {
            logger.error("Error in getLinkedPartInfo()\n", e);
            throw e;
        }
        return mlLinkedParts;
    }

    /**
     * Get Info on the the Part linked to related 2D or 3D object
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sCADObjectId
     *            CAD Object Id
     * @param slSelectStmts
     *            Info list to retrieve
     * @return Info of the Part found
     * @throws Exception
     */
    private Map getPartFromRelated2D3D(Context context, String sCADObjectId, SelectList slSelectStmts, SelectList slRelStmts) throws Exception {
        Map mPart = null;
        try {
            DomainObject dobCADObject = DomainObject.newInstance(context, sCADObjectId);
            // String sRelPattern = TigerConstants.RELATIONSHIP_ASSOCIATEDDRAWING + "," + TigerConstants.RELATIONSHIP_VERSIONOF + "," + RELATIONSHIP_PART_SPECIFICATION;

            String sRelPattern = TigerConstants.RELATIONSHIP_ASSOCIATEDDRAWING + "," + RELATIONSHIP_PART_SPECIFICATION;

            MapList mlParts = dobCADObject.getRelatedObjects(context, sRelPattern, "*", slSelectStmts, slRelStmts, true, true, (short) 1, "", "", 0);
            logger.debug("getPartFromRelated2D3D() - mlParts = <" + mlParts + ">");

            if (mlParts.size() > 0) {
                String sObjId = null;
                String strTypeName = "";
                Map mObject = null;
                DomainObject dobObj = DomainObject.newInstance(context);
                for (Iterator iterator = mlParts.iterator(); iterator.hasNext();) {
                    mObject = (Map) iterator.next();
                    sObjId = (String) mObject.get(SELECT_ID);
                    strTypeName = (String) mObject.get(SELECT_TYPE);
                    dobObj.setId(sObjId);
                    // Modified for TIGTK-4143:Start
                    // if (dobObj.isKindOf(context, TYPE_PART)) {
                    if (strTypeName.equalsIgnoreCase(TYPE_PART)) {
                        mPart = mObject;
                        // break;
                        // Modified for TIGTK-4143:End
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error in getPartFromRelated2D3D", e);
            throw e;
        }
        return mPart;
    }

    /**
     * Get Info on the the Part linked to the previous revision of the current CAD Object
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sCADObjectId
     *            CAD Object Id
     * @param slSelectStmts
     *            Info list to retrieve
     * @return Info of the Part found
     * @throws Exception
     */
    private Map getPartFromPreviousRevision(Context context, String sCADObjectId, SelectList slSelectStmts) throws Exception {
        Map mPart = null;
        BusinessObject bo = null;
        boolean bBoOpened = false;
        try {
            DomainObject dobCADObject = DomainObject.newInstance(context, sCADObjectId);
            bo = dobCADObject.getPreviousRevision(context);
            if (bo.exists(context)) {
                bo.open(context);
                bBoOpened = true;
                DomainObject dobCADObjectPreviousRev = DomainObject.newInstance(context, bo);

                String sPartId = dobCADObjectPreviousRev.getInfo(context, "to[" + RELATIONSHIP_PART_SPECIFICATION + "].from.id");
                logger.debug("getPartFromPreviousRevision() - sPartId = <" + sPartId + ">");
                if (sPartId != null && !"".equals(sPartId)) {
                    DomainObject dobPartPreviousRev = DomainObject.newInstance(context, sPartId);
                    if (!dobPartPreviousRev.isLastRevision(context)) {
                        BusinessObject boPart = dobPartPreviousRev.getLastRevision(context);
                        boPart.open(context);
                        DomainObject dobPart = DomainObject.newInstance(context, boPart);
                        mPart = dobPart.getInfo(context, slSelectStmts);
                        boPart.close(context);
                    }
                }
                bo.close(context);
            }
        } catch (Exception e) {
            logger.error("Error in getPartFromPreviousRevision", e);
            throw e;
        } finally {
            if (bBoOpened) {
                bo.close(context);
            }
        }
        return mPart;
    }

    /**
     * Method to get the Part Name which contains "CAD Object"
     * @param context
     * @param sCADObjectName
     * @param slSelectStmts
     * @return
     * @throws Exception
     */
    private Map getPartFromName(Context context, String sCADObjectName, SelectList slSelectStmts) throws Exception {

        Map mPart = new HashMap();
        try {
            String sName = null;
            // search Part which the name start with the 8 first characters of
            // the CAD Object name
            String sClause = "revision == last";

            // TIGTK-6193 - 11-04-2017 - VP - START
            // check this name
            java.util.regex.Pattern regex1 = java.util.regex.Pattern.compile("(([0-9]{7})[X|x])(.*)");
            java.util.regex.Pattern regex3 = java.util.regex.Pattern.compile("([0-9a-zA-Z])(.*)");
            java.util.regex.Pattern regex2 = java.util.regex.Pattern.compile("(([0-9]{7}))(.*)");
            // TIGTK-6193 - 11-04-2017 - VP - END

            Matcher regexMatcher = null;
            regexMatcher = regex1.matcher(sCADObjectName);

            if (regexMatcher.find()) {
                sName = regexMatcher.group(1);
                sName = sName.toUpperCase();
            } else {
                regexMatcher = regex2.matcher(sCADObjectName);
                if (regexMatcher.find()) {
                    sName = regexMatcher.group(1);
                } else {
                    regexMatcher = regex3.matcher(sCADObjectName);
                    if (regexMatcher.find()) {
                        sName = regexMatcher.group(1);
                    } else {
                        sName = sCADObjectName;
                    }
                }
            }

            logger.debug("getPartFromName() - sName = <" + sName + ">");
            if (sName != null) {
                // TIGTK-4143 - 29-03-2017 - VP :START
                // TIGTK-8512 - 2017-07-10 - VP - START
                // TIGTK-12516 : START
                String sQueryWhere = "revision == last";
                // TIGTK-12516 : END
                // TIGTK-8512 - 2017-07-10 - VP - END

                Query query = new Query();
                query.setBusinessObjectType(TYPE_PART);
                // TIGTK-12516 : START
                query.setBusinessObjectName(sName);
                // TIGTK-12516 : END
                query.setBusinessObjectRevision("*");
                query.setVaultPattern(TigerConstants.VAULT_ESERVICEPRODUCTION);
                query.setWhereExpression(sQueryWhere); // It will lead to findbug error but it is required for Performance Optimization
                query.setOwnerPattern("*");
                query.setExpandType(false);
                QueryIterator queryIterator = query.getIterator(context, slSelectStmts, (short) 100);
                while (queryIterator.hasNext()) {
                    BusinessObjectWithSelect busWithSelect = queryIterator.next();
                    String strPartName = busWithSelect.getSelectData(DomainConstants.SELECT_NAME);
                    if (sCADObjectName.indexOf(strPartName) > -1) {
                        for (int v = 0; v < slSelectStmts.size(); v++) {
                            mPart.put((String) slSelectStmts.getElement(v), busWithSelect.getSelectData((String) slSelectStmts.getElement(v)));
                        }
                        break;
                    }
                }
                // TIGTK-4143 - 29-03-2017 - VP :END
            }
        } catch (Exception e) {
            logger.error("Error in getPartFromName", e);
            throw e;
        }
        return mPart;
    }

    /**
     * Transform an Object (String or StringList) into a string list
     * @param obj
     *            Object to transform
     * @return StringList : The string list
     * @throws Exception
     */
    public static StringList toStringList(Object obj) throws Exception {
        StringList slObj = new StringList();
        try {
            if (obj != null) {
                if (obj instanceof String) {
                    if (((String) obj).length() > 0) {
                        slObj.addElement((String) obj);
                    }
                } else {
                    slObj = (StringList) obj;
                }
            }
        } catch (Exception e) {
            logger.error("Error in toStringList() - obj=<" + obj + ">\n", e);
            throw e;
        }
        return slObj;
    }

    /**
     * Join a String list to one separated a delimiter
     * @param list
     *            String list values
     * @param delimiter
     *            Delimiter to put between the String list values
     * @return
     * @throws Exception
     */
    public static String join(Collection list, String delimiter) throws Exception {
        StringBuffer sb = new StringBuffer();
        try {
            Iterator iter = list.iterator();
            while (iter.hasNext()) {
                sb.append(iter.next());
                if (iter.hasNext()) {
                    sb.append(delimiter);
                }
            }
        } catch (Exception e) {
            logger.error("Error in join() - list=<" + list + "> delimiter=<" + "delimiter" + ">\n", e);
            throw e;
        }
        return sb.toString();
    }

    /**
     * Method to return Colorized Line based on Color and Value passed
     * @param context
     * @param sColor
     * @param sValue
     * @return String
     * @throws Exception
     */
    private static String returnColorizedLine(String sColor, String sValue) {
        StringBuilder sbColoredValue = new StringBuilder();
        sbColoredValue.append("<span style=\"color:").append(sColor).append(";\">").append(sValue).append("</span>");
        return sbColoredValue.toString();
    }

    /**
     * Retrieve Part object by name and return a list of revisions<br>
     * Called from the JSPs :<br< - emxEngrValidation.jsp<br>
     * - PSS_EngineeringCentralCommonProcess.jsp
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            CAD object name and the row number in the table
     * @return
     * @throws Exception
     */
    public String getPartRevisionsInfo(Context context, String[] args) throws Exception {
        StringBuffer outPut = new StringBuffer();
        try {
            HashMap param = (HashMap) JPO.unpackArgs(args);

            String sCADObjectId = (String) param.get("cadObjectId");
            String sPartId = (String) param.get("partId");
            String sPartName = (String) param.get("partName");
            String sFieldNumber = (String) param.get("fieldNumber");
            logger.debug("getPartRevisionsInfo() - Starting. params = " + param);
            logger.debug("getPartRevisionsInfo() --> sFieldNumber:<" + sFieldNumber + "> sPartId:<" + sPartId + "> sPartName:<" + sPartName + "> sCADObjectId:<" + sCADObjectId + ">");

            SelectList slSelectStmts = new SelectList();
            slSelectStmts.addElement(SELECT_ID);
            slSelectStmts.addElement(SELECT_TYPE);
            slSelectStmts.addElement(SELECT_NAME);
            slSelectStmts.addElement(SELECT_REVISION);

            String sPartType = null;
            String sPartRev = null;
            if (sPartId != null) {
                DomainObject dobPart = DomainObject.newInstance(context, sPartId);
                Map mSelectedPart = dobPart.getInfo(context, slSelectStmts);
                sPartType = (String) mSelectedPart.get(SELECT_TYPE);
                sPartRev = (String) mSelectedPart.get(SELECT_REVISION);
                sPartName = (String) mSelectedPart.get(SELECT_NAME);
            }

            slSelectStmts.addElement(SELECT_DESCRIPTION);
            slSelectStmts.addElement("last");
            slSelectStmts.addElement("from[" + RELATIONSHIP_PART_SPECIFICATION + "].to.id");

            String sSearchTypes = TYPE_PART;
            if (sPartType != null) {
                sSearchTypes = sPartType;
            }
            // TIGTK-17791 : stembulkar : start
            MapList mlPartRevisions = DomainObject.findObjects(context, sSearchTypes, sPartName, "*", "*", TigerConstants.VAULT_ESERVICEPRODUCTION,
                    "current != " + TigerConstants.STATE_OBSOLETE + " && current != " + TigerConstants.STATE_PSS_CANCELPART_CANCELLED, true, slSelectStmts);
            // TIGTK-17791 : stembulkar : end
            logger.debug("getPartRevisionsInfo() - mlPartRevisions = <" + mlPartRevisions + ">");

            // TIGTK-11182 : START
            if (UIUtil.isNotNullAndNotEmpty(sPartName)) {
                outPut = constructPartRevisionsSelectList(context, mlPartRevisions, sPartRev, sCADObjectId, sFieldNumber);
                return outPut.toString();
            }
            // TIGTK-11182 : END

            if (mlPartRevisions.size() > 0) {
                boolean bAuthorized = true;
                if (sCADObjectId != null) {
                    bAuthorized = checkIfPartCanBeSynchronized(context, mlPartRevisions, sCADObjectId);
                }
                if (bAuthorized) {
                    outPut = constructPartRevisionsSelectList(context, mlPartRevisions, sPartRev, sCADObjectId, sFieldNumber);
                } else {
                    outPut.append("CADOBJECTALREADYSYNCHRONIZED");
                }
            } else {
                outPut.append("OK");
            }
        } catch (Exception ex) {
            logger.error("Error in getPartRevisionsInfo()\n ", ex);
            throw ex;
        }
        return outPut.toString();
    }

    /**
     * Check if found Part can be synchronized <br>
     * A Part can be synchronized if it's not done yet or if it's already synchronized to Charted Drawing
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param mlPartRevisions
     *            Parts to be synchronized
     * @param sCADObjectId
     *            CAD Object id to synchronized
     * @return
     * @throws Exception
     */

    private boolean checkIfPartCanBeSynchronized(Context context, MapList mlPartRevisions, String sCADObjectId) throws Exception {
        boolean bCanBeSynchronized = false;
        logger.debug("checkIfPartCanBeSynchronized() - mlPartRevisions <" + mlPartRevisions + ">");
        logger.debug("checkIfPartCanBeSynchronized() - sCADObjectId <" + sCADObjectId + ">");
        DomainObject doCADObject = DomainObject.newInstance(context, sCADObjectId);
        StringList slSynchronizedIds = doCADObject.getInfoList(context, "to[" + RELATIONSHIP_PART_SPECIFICATION + "].from.id");

        if (slSynchronizedIds.isEmpty()) {
            bCanBeSynchronized = true;
            logger.debug("checkIfPartCanBeSynchronized() - The CAD Object is not synchronized");
        }
        for (Iterator iterator = slSynchronizedIds.iterator(); iterator.hasNext();) {
            String partId = (String) iterator.next();
            DomainObject doSynchronizedPart = DomainObject.newInstance(context, partId);
        }
        logger.debug("checkIfPartCanBeSynchronized() - Found Part can be synchronized = <" + bCanBeSynchronized + ">");
        return bCanBeSynchronized;
    }

    /**
     * Copy the OOTB method for customization To display the policy list in part creation page Also considers the app specific policies
     * @param context
     * @param args
     * @return HashMap
     * @throws Exception
     */
    public HashMap getPolicy(Context context, String[] args) throws Exception {
        HashMap hmPolicyMap = new HashMap();
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap requestMap = (HashMap) programMap.get("requestMap");
        String createMode = (String) requestMap.get("createMode");
        String typeString = (String) requestMap.get("type");
        try {

            if (typeString.indexOf(',') != -1 && !typeString.startsWith("_selectedType")) {
                typeString = typeString.substring(0, typeString.indexOf(','));

            }
            if (typeString.startsWith("type_")) {
                typeString = PropertyUtil.getSchemaProperty(context, typeString);

            } else if (typeString.startsWith("_selectedType")) {
                typeString = typeString.substring(typeString.indexOf(':') + 1, typeString.indexOf(','));

            } else if ("".equals(typeString) || typeString == null) {
                typeString = DomainConstants.TYPE_PART;

            }

            BusinessType partBusinessType = new BusinessType(typeString, context.getVault());
            if (!mxType.isOfParentType(context, typeString, DomainConstants.TYPE_PART)) {
                throw new FrameworkException();
            }

            PolicyList allPartPolicyList = partBusinessType.getPoliciesForPerson(context, false);
            PolicyItr partPolicyItr = new PolicyItr(allPartPolicyList);

            boolean bcamInstall = FrameworkUtil.isSuiteRegistered(context, "appVersionX-BOMCostAnalytics", false, null, null);

            String languageStr = context.getSession().getLanguage();
            String defaultPolicy = getDefaultPolicy(context, requestMap); // IR-082946V6R2012

            Policy policyValue = null;
            String policyName = "";
            String policyClassification = "";

            StringList display = new StringList();
            StringList actualVal = new StringList();

            if ("assignTopLevelPart".equals(createMode) || POLICY_CONFIGURED_PART.equals(defaultPolicy)) {
                display.addElement(i18nNow.getAdminI18NString("Policy", POLICY_CONFIGURED_PART, languageStr));

                actualVal.addElement(POLICY_CONFIGURED_PART);

            } else if ("MFG".equals(createMode)) {
                StringList slMfgPolicy = EngineeringUtil.getManuPartPolicy(context);

                if (slMfgPolicy.size() > 0) {
                    defaultPolicy = (String) slMfgPolicy.get(0);

                }
                for (int i = 0; i < slMfgPolicy.size(); i++) {
                    policyName = (String) slMfgPolicy.get(i);

                    if (EngineeringUtil.getPolicyClassification(context, policyName).equals("Equivalent")) {

                        continue;
                    } else if (policyName.equals(PropertyUtil.getSchemaProperty(context, "policy_StandardPart"))) {

                        continue;
                    }

                    display.addElement(i18nNow.getAdminI18NString("Policy", policyName, languageStr));
                    actualVal.addElement(policyName);
                    if (i == 0) {
                        defaultPolicy = (String) slMfgPolicy.get(i);

                    }
                    if (policyName.equals(PropertyUtil.getSchemaProperty(context, "policy_ManufacturingPart"))) {
                        defaultPolicy = policyName;

                    }
                }
            } else {

                while (partPolicyItr.next()) {
                    policyValue = (Policy) partPolicyItr.obj();
                    policyName = policyValue.getName();

                    // when upgrading from previous release, skip this policy
                    if (policyName.equals(TigerConstants.POLICY_UNRESOLVEDPART)) {

                        continue;
                    }

                    policyClassification = EngineeringUtil.getPolicyClassification(context, policyName);

                    // Modified for TBE Packaging & Scalability
                    if (!EngineeringUtil.isENGInstalled(context, args) && !EngineeringUtil.getPolicyClassification(context, policyName).equals("Development")) {

                        continue;
                    }

                    if (POLICY_CONFIGURED_PART.equalsIgnoreCase(policyName)) {

                        continue;
                    }
                    if (bcamInstall) {
                        if ("Cost".equals(policyClassification)) {

                            continue;
                        }
                    }
                    if (EngineeringUtil.getPolicyClassification(context, policyName).equals("Equivalent") || EngineeringUtil.getPolicyClassification(context, policyName).equals("Manufacturing")) {

                        continue;
                    }

                    display.addElement(i18nNow.getAdminI18NString("Policy", policyName, languageStr));

                    actualVal.addElement(policyName);

                }
            }
            int position = actualVal.indexOf(defaultPolicy);
            if (position > 0) {
                String positionDisplay = (String) display.get(position);
                String positionActual = (String) actualVal.get(position);
                display.setElementAt(display.get(0), position);
                actualVal.setElementAt(actualVal.get(0), position);
                display.setElementAt(positionDisplay, 0);
                actualVal.setElementAt(positionActual, 0);

            }
            hmPolicyMap.put("field_choices", actualVal);

            hmPolicyMap.put("field_display_choices", display);

        } catch (Exception e) {
            // TIGTK-5405 - 03-04-2017 - VP - START
            logger.error("Error in getPolicy: ", e);
            // TIGTK-5405 - 03-04-2017 - VP - END
            throw e;
        }
        return hmPolicyMap;
    }

    /**
     * Following Method has been added by CAD BOM stream by copying the method from emxECPartBase JPO as it was private in that JPO and we wanted to use it in this JPO. if objectId is of Part Family
     * then policy will be returned from partfamily attribute "Deafult Part Policy" else it will return "Development Part" as default policy.
     * @param context
     *            ematrix context
     * @param map
     *            contains request map
     * @return String policy name
     * @throws Exception
     *             if error occurs
     */
    private String getDefaultPolicy(Context context, HashMap map) throws Exception {

        // Following variable was not present in OOTB method. It was declared as
        // static.

        String defaultPolicy = EnoviaResourceBundle.getProperty(context, "type_Part.defaultDevPolicy");
        String objectId = getStringValue(map, "objectId");

        if (isValidData(objectId)) {
            StringList objectSelect = createStringList(new String[] { DomainConstants.SELECT_TYPE, TigerConstants.SELECT_ATTRIBUTE_DEFAULTPARTPOLICY });

            DomainObject domObj = DomainObject.newInstance(context, objectId);
            Map dataMap = domObj.getInfo(context, objectSelect);

            String strType = getStringValue(dataMap, DomainConstants.SELECT_TYPE);

            if (DomainConstants.TYPE_PART_FAMILY.equals(strType)) {
                String strPolicy = getStringValue(dataMap, TigerConstants.SELECT_ATTRIBUTE_DEFAULTPARTPOLICY);

                if (isValidData(strPolicy)) {
                    defaultPolicy = PropertyUtil.getSchemaProperty(context, strPolicy);
                }
            }
        }

        return defaultPolicy;
    }

    /**
     * Method called on the post process functionality of attaching automatically CAD to BOM. This method also makes a check for "EBOM Synchronization" as well as "Remove existing Synchronization"
     * @param context
     * @param args
     * @return String
     * @throws Exception
     */
    @com.matrixone.apps.framework.ui.PostProcessCallable
    // Changes done for TIGTK-2089
    public HashMap postProcessForCADToBOM(Context context, String[] args) throws Exception {
        int iTemp = 0;
        int iSetAttr = 0;
        boolean bEBOMLinksCreation = false;
        boolean bReconciliationLinksDelete = false;
        HashMap returnMap = new HashMap();
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        StringBuffer sbSynhMessage = new StringBuffer();
        String[] rowCADObjectGeometryTypes = (String[]) programMap.get("CADObjectGeometryType");
        String[] rowCADObjectParents = (String[]) programMap.get("CADObjectParent");
        String strPartObjectIds[] = (String[]) programMap.get("PartObjectId");
        String strTableRowIds[] = (String[]) programMap.get("emxTableRowId");
        String strEBOMLinksCreation[] = (String[]) programMap.get("EBOMLinksCreation");
        String strReconciliationLinksDelete[] = (String[]) programMap.get("ReconciliationLinksDelete");
        // TIGTK-8512 - 2017-07-10 - VP - START
        String strPartFamilyIds[] = (String[]) programMap.get("PartFamilyId");
        String strFamilyConnectionRequireds[] = (String[]) programMap.get("FamilyConnectionRequired");
        // TIGTK-8512 - 2017-07-10 - VP - END
        // TIGTK-4478 - 17-03-2017 - VP - START
        MCADIntegrationSessionData integSessionData = (MCADIntegrationSessionData) programMap.get("integSessionData");
        // TIGTK-4478 - 17-03-2017 - VP - END
        String strCADParent = EMPTY_STRING;
        String[] strCbxValueSplited;
        String strNumber = EMPTY_STRING;
        String strCADObjectId = EMPTY_STRING;
        int index;
        String strPartObjectId = EMPTY_STRING;
        boolean isPartPresent = true;
        String strCADObjectGeometryType = EMPTY_STRING;
        String strTableRow = EMPTY_STRING;
        String strSynchronizedPartId = EMPTY_STRING;
        ArrayList<String> alSynchronizedParts = new ArrayList<String>();
        StringList selectlist = new StringList();
        selectlist.add(DomainConstants.SELECT_NAME);
        selectlist.add(DomainConstants.SELECT_TYPE);
        selectlist.add(TigerConstants.SELECT_ATTRIBUTE_PSS_GEOMETRYTYPE);
        String strConnectedPart = EMPTY_STRING;
        // TIGTK-8512 - 2017-07-10 - VP - START
        String strPartFamilyId = EMPTY_STRING;
        String strFamilyConnectionRequired = EMPTY_STRING;
        // TIGTK-8512 - 2017-07-10 - VP - END
        StringList slConnectedPartList = new StringList();
        try {

            // TIGTK-8886 - PTE - 2017-07-11 - START
            context.setCustomData("PSS_THROW_EXCEPTION", "TRUE");
            // TIGTK-8886 - PTE - 2017-07-11 - END

            if ("true".equals(strEBOMLinksCreation[0])) {
                bEBOMLinksCreation = true;
            }

            if ("true".equals(strReconciliationLinksDelete[0])) {
                bReconciliationLinksDelete = true;
            }

            logger.debug("synchronizeCADObjectswithEBOM() - bEBOMLinksCreation = <" + bEBOMLinksCreation + ">");
            logger.debug("synchronizeCADObjectswithEBOM() - bReconciliationLinksDelete = <" + bReconciliationLinksDelete + ">");

            if (strTableRowIds == null) {
                returnMap.put("Action", "strEBOMMSyncSelectionMessage");
            } else {

                // Added for TIGTK-3631 - START
                // Setting Geometry types values first with MG being the last
                HashMap<String, String> mapCADMGGeometryMap = new HashMap<String, String>();
                HashMap<String, String> mapCADNonMGGeometryMap = new HashMap<String, String>();
                StringList slMGRowCADIds = new StringList();
                StringList slNonMGRowCADIds = new StringList();

                for (int i = 0; i < strTableRowIds.length; i++) {
                    strTableRow = strTableRowIds[i];

                    strCbxValueSplited = strTableRow.split("[|]", -1);
                    strNumber = strCbxValueSplited[0];
                    strCADObjectId = strCbxValueSplited[1];
                    index = Integer.parseInt(strNumber);

                    strCADObjectGeometryType = rowCADObjectGeometryTypes[index];
                    String sCADDefRevision = "00";
                    String sCADDefMajorRevision = "";
                    String sCADDefName = "";

                    if ("MG".equalsIgnoreCase(strCADObjectGeometryType)) {
                        mapCADMGGeometryMap.put(strCADObjectId, strCADObjectGeometryType);
                        slMGRowCADIds.addElement(strTableRowIds[i]);
                    } else {
                        mapCADNonMGGeometryMap.put(strCADObjectId, strCADObjectGeometryType);
                        slNonMGRowCADIds.addElement(strTableRowIds[i]);

                    }
                } // END of first for loop
                if (!bReconciliationLinksDelete) {

                    LinkedHashMap<String, String> mapCADGeometryMap = new LinkedHashMap<String, String>();
                    mapCADGeometryMap.putAll(mapCADNonMGGeometryMap);
                    mapCADGeometryMap.putAll(mapCADMGGeometryMap);
                    setGeomteryTypeForCAD(context, mapCADGeometryMap);
                }

                // Added for TIGTK-3631 - END

                StringList slSortedRowCADIDs = new StringList();
                if (bReconciliationLinksDelete) {
                    slSortedRowCADIDs.addAll(slNonMGRowCADIds);
                    slSortedRowCADIDs.addAll(slMGRowCADIds);
                } else {
                    slSortedRowCADIDs.addAll(slMGRowCADIds);
                    slSortedRowCADIDs.addAll(slNonMGRowCADIds);
                }

                for (int i = 0; i < slSortedRowCADIDs.size(); i++) {
                    strTableRow = (String) slSortedRowCADIDs.get(i);

                    strCbxValueSplited = strTableRow.split("[|]", -1);
                    strNumber = strCbxValueSplited[0];
                    strCADObjectId = strCbxValueSplited[1];
                    index = Integer.parseInt(strNumber);
                    strPartObjectId = strPartObjectIds[index];
                    strCADObjectGeometryType = rowCADObjectGeometryTypes[index];
                    // TIGTK-8512 - 2017-07-10 - VP - START
                    strPartFamilyId = strPartFamilyIds[index];
                    strFamilyConnectionRequired = strFamilyConnectionRequireds[index];
                    // TIGTK-8512 - 2017-07-10 - VP - END
                    String sCADDefRevision = "00";
                    String sCADDefMajorRevision = "";
                    String sCADDefName = "";

                    // Creating new CAD Object Instance and fetch required values
                    DomainObject domCADObject = DomainObject.newInstance(context, strCADObjectId);
                    Map mapCADObjectInfo = domCADObject.getInfo(context, selectlist);

                    if (UIUtil.isNullOrEmpty(strPartObjectId)) {
                        isPartPresent = false;
                    } else {
                        // TIGTK-8546 - 06-22-2017 - PTE - START

                        // Creating new Part Object Instance
                        DomainObject domPartObject = DomainObject.newInstance(context, strPartObjectId);
                        try {

                            String sCADObjectParent = rowCADObjectParents[index];
                            StringList slCADRelId = getRelationshipIdBetweenObjects(context, sCADObjectParent, strCADObjectId, TigerConstants.RELATIONSHIP_CADSUBCOMPONENT);
                            if (bReconciliationLinksDelete) {
                                if (!slCADRelId.isEmpty()) {
                                    for (int j = 0; j < slCADRelId.size(); j++) {
                                        String strCADRelId = (String) slCADRelId.get(j);
                                        String strEBOMId = getEBOMRelFromCADRel(context, domPartObject, strCADRelId);
                                        disconnectPartAndCADObject(context, strPartObjectId, strCADObjectId, strEBOMId);
                                    }
                                } else {
                                    disconnectPartAndCADObject(context, strPartObjectId, strCADObjectId, EMPTY_STRING);
                                }
                                returnMap.put("Action", "strEBOMSyncSuccessMessage");
                            } else {

                                // TIGTK-8512 - 2017-07-10 - VP - START
                                if (UIUtil.isNotNullAndNotEmpty(strPartFamilyId) && UIUtil.isNotNullAndNotEmpty(strFamilyConnectionRequired) && strFamilyConnectionRequired.equalsIgnoreCase("TRUE")) {
                                    DomainObject domPartFamily = DomainObject.newInstance(context, strPartFamilyId);
                                    DomainRelationship.connect(context, domPartFamily, RELATIONSHIP_CLASSIFIED_ITEM, domPartObject);
                                    // START :: TIGTK-17756 :: ALM-5978
                                    // String[] setDescriptionArray = { strPartObjectId, "Modify", domPartObject.getInfo(context, SELECT_CURRENT) };
                                    StringList slSelectables = new StringList(2);
                                    slSelectables.add(SELECT_CURRENT);
                                    slSelectables.add(SELECT_POLICY);
                                    Map mpInfo = domPartObject.getInfo(context, slSelectables);
                                    String[] setDescriptionArray = { strPartObjectId, "Modify", (String) mpInfo.get(SELECT_CURRENT), (String) mpInfo.get(SELECT_POLICY) };
                                    // END :: TIGTK-17756 :: ALM-5978
                                    populateAutoDescription(context, setDescriptionArray);
                                }
                                // TIGTK-8512 - 2017-07-10 - VP - END
                                // get select Part for the parent of this CAD Object

                                String sPartParentId = getSelectedPartId(sCADObjectParent, strTableRowIds, strPartObjectIds);
                                strConnectedPart = domCADObject.getInfo(context, "to[" + RELATIONSHIP_PART_SPECIFICATION + "].from.id");
                                if (UIUtil.isNotNullAndNotEmpty(strConnectedPart)) {
                                    slConnectedPartList.add(strConnectedPart);
                                }
                                // Synchronize Part to CAD Object

                                // TIGTK-4478 - 17-03-2017 - VP - START
                                synchronizePartToCADObject(context, strPartObjectId, strCADObjectId, strCADObjectGeometryType, strSynchronizedPartId);
                                // TIGTK-4478 - 17-03-2017 - VP - END

                                if (!alSynchronizedParts.contains(strPartObjectId)) {
                                    alSynchronizedParts.add(strPartObjectId);
                                }
                                // create EBOM connection between sPartId and
                                // sPartParentId
                                if (bEBOMLinksCreation && sPartParentId != null && !"".equals(sPartParentId) && !strPartObjectId.equals(sPartParentId)) {
                                    // TIGTK-3905:START Modified 13/1/2017

                                    int retStatus = 0;

                                    retStatus = connectPartToPartParent(context, strPartObjectId, sPartParentId, strCADObjectId, sCADObjectParent);

                                    // TIGTK-3905:END Modified 13/1/2017
                                    if (retStatus == 0) {
                                        returnMap.put("Action", "strEBOMSyncSuccessMessage");
                                    }
                                }

                                // TIGTK-4478 - 17-03-2017 - VP - START
                                // TIGTK-6080 - 31-03-2017 - VP - START
                                StringList slEBOMRelId = getRelationshipIdBetweenObjects(context, sPartParentId, strPartObjectId, TigerConstants.RELATIONSHIP_EBOM);

                                if (!slEBOMRelId.isEmpty() && !slCADRelId.isEmpty()) {
                                    int nEBOMInstances = slEBOMRelId.size();
                                    int nCADInstances = slCADRelId.size();

                                    if (nEBOMInstances == nCADInstances) {
                                        for (int j = 0; j < nEBOMInstances; j++) {
                                            String strEBOMRelId = (String) slEBOMRelId.getElement(j);
                                            String strCADRelId = (String) slCADRelId.getElement(j);
                                            integrateEBOMSynchronizationForCADObject(context, strPartObjectId, strCADObjectId, strEBOMRelId, strCADRelId, integSessionData);
                                        }
                                    }
                                } else {
                                    integrateEBOMSynchronizationForCADObject(context, strPartObjectId, strCADObjectId, null, null, integSessionData);
                                }
                                // TIGTK-6080 - 31-03-2017 - VP - END
                                // TIGTK-4478 - 17-03-2017 - VP - END
                            }
                        } catch (Exception ex) {
                            Count++;
                            if (!(Count > 10)) {
                                String strCADObjectName = (String) mapCADObjectInfo.get(DomainConstants.SELECT_NAME);
                                String strMessage = ex.getMessage();
                                strMessage = strMessage.replace("java.lang.Exception: Message:System Error: #5000001: java.lang.Exception:", " ");
                                strMessage = strMessage.replace("Severity:3 ErrorCode:5000001", " ");
                                strMessage = strMessage.replace("java.lang.Exception: Message:", " ");
                                strMessage = strMessage.replace("Severity:2 ErrorCode:1500174", " ");
                                sbInvalidObj.append("Synchronization on ' ");
                                sbInvalidObj.append("CAD Object ");
                                sbInvalidObj.append(strCADObjectName);
                                sbInvalidObj.append(" ' failed : ");
                                sbInvalidObj.append(strMessage);
                                sbInvalidObj.append("\n");
                            }

                        } // TIGTK-8546 - 06-22-2017 - PTE - END

                    }

                }
                // TIGTK-8546 - 06-22-2017 - PTE - START
                if (Count != 0) {
                    if (Count == strTableRowIds.length) {
                        sbSynhMessage.append("No EBOM synchronized with ");
                    } else {
                        sbSynhMessage.append("EBOM partially synchronized with ");
                    }
                    sbSynhMessage.append(Count);
                    sbSynhMessage.append(" synchronization failed:");
                    sbSynhMessage.append("\n");
                    sbSynhMessage.append(sbInvalidObj);
                    MqlUtil.mqlCommand(context, "notice $1", sbSynhMessage.toString());
                    returnMap.put("Action", "strEBOMPartialSyncSuccessMessage");
                } else {
                    returnMap.put("Action", "strEBOMSyncSuccessMessage");
                }
                // TIGTK-8546 - 06-22-2017 - PTE - END
                // Showing the alert if Part is not selected for any "CAD object"
                if (!isPartPresent) {
                    returnMap.put("Action", "strNoPartSelectionMessage");
                }
            }

        }
        // TIGTK-8886 - PTE - 2017-07-11 - START
        finally {
            context.setCustomData("PSS_THROW_EXCEPTION", "FALSE");
            context.removeFromCustomData("PSS_THROW_EXCEPTION");
        }
        // TIGTK-8886 - PTE - 2017-07-11 - END
        return returnMap;

    }

    // TIGTK-6080 - 31-03-2017 - VP - START
    // TIGTK-4478 - 17-03-2017 - VP - START
    /**
     * Get Relationship Id between two objects
     * @param context
     * @param strFromObjectId
     * @param strToObjectId
     * @param strRelationshipName
     * @return strRelationshipId
     * @throws Exception
     * @author Vasant PADHIYAR : SteepGraph
     */
    public StringList getRelationshipIdBetweenObjects(Context context, String strFromObjectId, String strToObjectId, String strRelationshipName) throws Exception {
        StringList slRelationshipId = new StringList();
        try {
            if (UIUtil.isNotNullAndNotEmpty(strFromObjectId) && UIUtil.isNotNullAndNotEmpty(strToObjectId)) {
                SelectList slSelectStmts = new SelectList();
                slSelectStmts.addElement(SELECT_ID);

                SelectList slRelSelectStmts = new SelectList();
                slRelSelectStmts.addElement(SELECT_RELATIONSHIP_ID);

                String objectWhere = "id == '" + strToObjectId + "'";

                DomainObject domFromObject = DomainObject.newInstance(context, strFromObjectId);

                MapList mlObjects = domFromObject.getRelatedObjects(context, strRelationshipName, "*", slSelectStmts, slRelSelectStmts, false, true, (short) 1, objectWhere, "", 0);
                String sObjectId = DomainObject.EMPTY_STRING;
                for (Iterator iterator = mlObjects.iterator(); iterator.hasNext();) {
                    Map mObject = (Map) iterator.next();
                    sObjectId = (String) mObject.get(SELECT_ID);
                    if (strToObjectId.equals(sObjectId)) {
                        slRelationshipId.addElement((String) mObject.get(SELECT_RELATIONSHIP_ID));
                    }
                }
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 03-04-2017 - VP - START
            logger.error("Error in getRelationshipIdBetweenObjects: ", ex);
            // TIGTK-5405 - 03-04-2017 - VP - END
        }
        return slRelationshipId;
    }
    // TIGTK-4478 - 17-03-2017 - VP - END
    // TIGTK-6080 - 31-03-2017 - VP - END

    /**
     * Disconnect Part to CAD Object by the relationship RELATIONSHIP_PART_SPECIFICATION
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sPartId
     *            Part id
     * @param sCADObjectId
     *            CAD Object id
     * @param sGeometryType
     *            Geometry type value
     * @throws Exception
     */
    public void disconnectPartAndCADObject(Context context, String sPartId, String sCADObjectId, String strEBOMId) throws Exception {
        boolean bPushedContext = false;
        boolean bCheck = false;
        try {

            String sRelId = getPartSpecificationRelationshipId(context, sPartId, sCADObjectId);

            // TIGTK-11319:START
            DomainObject dobPart = DomainObject.newInstance(context, sPartId);
            String sPartState = dobPart.getInfo(context, SELECT_CURRENT);
            if ((TigerConstants.STATE_PSS_ECPART_PRELIMINARY.equals(sPartState) || TigerConstants.STATE_PSS_DEVELOPMENTPART_CREATE.equals(sPartState))) {
                bCheck = true;
            }
            // TIGTK-11319:END

            if (bCheck) {
                if (UIUtil.isNotNullAndNotEmpty(sRelId)) {
                    DomainRelationship.disconnect(context, sRelId);
                }

                if (UIUtil.isNotNullAndNotEmpty(strEBOMId)) {

                    StringList slAttrEBOMList = new StringList();
                    slAttrEBOMList.add(DomainConstants.ATTRIBUTE_REFERENCE_DESIGNATOR);
                    slAttrEBOMList.add(DomainConstants.ATTRIBUTE_COMPONENT_LOCATION);
                    slAttrEBOMList.add(TigerConstants.ATTRIBUTE_RELATIONSHIPUUID);
                    slAttrEBOMList.add(TigerConstants.ATTRIBUTE_SOURCE);
                    resetRelAttrValue(context, strEBOMId, slAttrEBOMList);
                }
            }
        } catch (Exception ex) {
            logger.error("Error in disconnectPartAndCADObject()\n", ex);
            throw ex;
        } finally {
            if (bPushedContext) {
                // Pop context from User Agent
                ContextUtil.popContext(context);
            }
        }
    }

    /**
     * Get the relationship Id between the Part and the CAD Object
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sPartId
     *            Part Id
     * @param sCADObjectId
     *            CAD Object Id
     * @return
     * @throws Exception
     */
    public String getPartSpecificationRelationshipId(Context context, String sPartId, String sCADObjectId) throws Exception {
        String sRelId = "";
        try {
            SelectList slSelectStmts = new SelectList();
            slSelectStmts.addElement(SELECT_ID);

            SelectList slRelSelectStmts = new SelectList();
            slRelSelectStmts.addElement(SELECT_RELATIONSHIP_ID);

            DomainObject dobPart = DomainObject.newInstance(context, sPartId);

            MapList mlObjects = dobPart.getRelatedObjects(context, RELATIONSHIP_PART_SPECIFICATION, "*", slSelectStmts, slRelSelectStmts, false, true, (short) 1, "", "", 0);
            Map mObject = null;
            String sObjectId = "";
            for (Iterator iterator = mlObjects.iterator(); iterator.hasNext();) {
                mObject = (Map) iterator.next();
                sObjectId = (String) mObject.get(SELECT_ID);
                if (sCADObjectId.equals(sObjectId)) {
                    sRelId = (String) mObject.get(SELECT_RELATIONSHIP_ID);
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Error in getPartSpecificationRelationshipId()\n", e);
            throw e;
        }
        return sRelId;
    }

    /**
     * Check if two Parts are connected with the EBOM relationship and connect them if it is not.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sPartId
     *            Part id
     * @param sPartParentId
     *            Part parent id
     * @throws Exception
     */
    protected int connectPartToPartParent(Context context, String sPartId, String sPartParentId, String sCADObjectId, String sCADObjectParentId) throws Exception {

        int retStatus = 0;
        try {
            DomainObject dobPartParent = DomainObject.newInstance(context, sPartParentId);

            // Get Part id already connected to the Parent
            StringList sParentChildIds = dobPartParent.getInfoList(context, "from[" + RELATIONSHIP_EBOM + "].to.id");
            logger.debug("connectPartToPartParent() - Parts already connected to the Parent :<" + sParentChildIds + ">");

            DomainObject dobPart = DomainObject.newInstance(context, sPartId);
            if (!sParentChildIds.contains(sPartId)) {
                logger.debug("connectPartToPartParent() - connect Part:<" + sPartId + "> to Part :<" + sPartParentId + ">");

                // TIGTK-6080 - 31-03-2017 - VP - START

                // Retrieve the quantity value
                int iQauntity = getEBOMQuantityValue(context, sCADObjectId, sCADObjectParentId);

                // connect Parts
                for (int i = 0; i < iQauntity; i++) {

                    // Retrieve the next find number value
                    StringList slFindNumbers = dobPartParent.getInfoList(context, "from[" + RELATIONSHIP_EBOM + "].attribute[" + ATTRIBUTE_FIND_NUMBER + "]");
                    logger.debug("connectPartToPartParent() - slFindNumbers :<" + slFindNumbers + ">");

                    String sFindNumber = getNextFindNumber(slFindNumbers);
                    logger.debug("connectPartToPartParent() - sFindNumber = <" + sFindNumber + ">");
                    // TIGTK-9233 - 22-08-2017 - PTE - START
                    boolean isPushedContext = false;
                    try {
                        DomainRelationship doRel = DomainRelationship.connect(context, dobPartParent, RELATIONSHIP_EBOM, dobPart);
                        doRel.setAttributeValue(context, ATTRIBUTE_FIND_NUMBER, sFindNumber);
                        doRel.setAttributeValue(context, ATTRIBUTE_QUANTITY, "1");
                        if (UIUtil.isNotNullAndNotEmpty(doRel.getName())) {
                            retStatus = 0;
                        }
                    } catch (Exception e) {
                        try {
                            ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, null, null);
                            isPushedContext = true;
                            DomainRelationship doRel = DomainRelationship.connect(context, dobPartParent, RELATIONSHIP_EBOM, dobPart);
                            doRel.setAttributeValue(context, ATTRIBUTE_FIND_NUMBER, sFindNumber);
                            doRel.setAttributeValue(context, ATTRIBUTE_QUANTITY, "1");
                            if (UIUtil.isNotNullAndNotEmpty(doRel.getName())) {
                                retStatus = 0;
                            }
                        } catch (Exception ex) {
                            throw ex;
                        }
                    } finally {
                        if (isPushedContext)
                            ContextUtil.popContext(context);
                    }

                }
                // TIGTK-9233 - 22-08-2017 - PTE - END
                // TIGTK-6080 - 31-03-2017 - VP - END

            } else {
                logger.debug("connectPartToPartParent() - Part:<" + sPartId + "> is already connected to Part:<" + sPartParentId + ">");
            }

        } catch (Exception ex) {
            logger.error("Error in connectPartToPartParent()\n", ex);
            throw ex;
        }
        return retStatus;
    }

    /**
     * Calculate and return the next value of the attribute Find Number
     * @param slFindNumbers
     *            Find Number values
     * @return
     */
    private String getNextFindNumber(StringList slFindNumbers) {
        String sFindNumber = "0010";
        try {
            logger.debug("getNextFindNumber() - slFindNumbers :<" + slFindNumbers + ">");
            if (slFindNumbers.size() > 0) {
                int iFindNumber = 0;

                String sFN = "";
                int iFN;
                for (Iterator iterator = slFindNumbers.iterator(); iterator.hasNext();) {
                    sFN = (String) iterator.next();
                    iFN = Integer.parseInt(sFN);
                    if (iFindNumber < iFN) {
                        iFindNumber = iFN;
                    }
                }
                logger.debug("getNextFindNumber() - iFindNumber = <" + iFindNumber + ">");

                // PCM TIGTK-7058 : PSE : 30-05-2017 : START
                Context context = JPOSupport.getContext();
                String incrementFN = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentral.StructureBrowser.FNIncrement");
                sFindNumber = Integer.toString(iFindNumber + Integer.parseInt(incrementFN));
                // PCM TIGTK-7058 : PSE : 30-05-2017 : END

                while (sFindNumber.length() < 4) {
                    sFindNumber = "0" + sFindNumber;
                }
            }
            logger.debug("getNextFindNumber() - sFindNumber = <" + sFindNumber + ">");
        } catch (Exception e) {
            // TIGTK-5405 - 03-04-2017 - VP - START
            logger.error("Error in getNextFindNumber: ", e);
            // TIGTK-5405 - 03-04-2017 - VP - END
        }

        return sFindNumber;

    }

    /**
     * Count the connection number between the CAD object and its parent
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sCADObjectId
     *            CAD object ID
     * @param sCADObjectParentId
     *            Parent ID
     * @return
     * @throws Exception
     */
    private int getEBOMQuantityValue(Context context, String sCADObjectId, String sCADObjectParentId) throws Exception {
        int iQuantity = 1;

        logger.debug("getEBOMQuantityValue() - sCADObjectParentId = <" + sCADObjectParentId + ">  sCADObjectId = <" + sCADObjectId + ">");
        if (sCADObjectParentId != null && !"".equals(sCADObjectParentId)) {
            DomainObject dobParent = DomainObject.newInstance(context, sCADObjectParentId);

            SelectList slSelectStmts = new SelectList();
            slSelectStmts.addElement(SELECT_ID);
            slSelectStmts.addElement(SELECT_TYPE);
            slSelectStmts.addElement("from[" + TigerConstants.RELATIONSHIP_VERSIONOF + "].to.id");

            MapList mlChilds = dobParent.getRelatedObjects(context, TigerConstants.RELATIONSHIP_CADSUBCOMPONENT, "*", slSelectStmts, null, false, true, (short) 1, "", "", 0);

            if (mlChilds.size() == 0) {
                String sVersionedId = dobParent.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_ACTIVEVERSION + "].to.id");
                if (sVersionedId != null && !"".equals(sVersionedId)) {
                    dobParent.setId(sVersionedId);
                    mlChilds = dobParent.getRelatedObjects(context, TigerConstants.RELATIONSHIP_CADSUBCOMPONENT, "*", slSelectStmts, null, false, true, (short) 1, "", "", 0);
                }
            }
            logger.debug("getEBOMQuantityValue() - mlChilds = <" + mlChilds + ">");

            int iNumberOccurence = 0;
            String sChildId = null;
            String sVersionOfId = null;
            Map mChildInfo = null;
            for (Iterator iterator = mlChilds.iterator(); iterator.hasNext();) {
                mChildInfo = (Map) iterator.next();
                sVersionOfId = (String) mChildInfo.get("from[" + TigerConstants.RELATIONSHIP_VERSIONOF + "].to.id");
                if (sVersionOfId != null && !"".equals(sVersionOfId)) {
                    sChildId = sVersionOfId;
                } else {
                    sChildId = (String) mChildInfo.get(SELECT_ID);
                }

                logger.debug("getEBOMQuantityValue() - sChildId = <" + sChildId + "> sCADObjectId = <" + sCADObjectId + ">");
                if (sCADObjectId.equals(sChildId)) {
                    iNumberOccurence++;
                }
            }
            iQuantity = iNumberOccurence;
        }
        logger.debug("getEBOMQuantityValue() - iQuantity = <" + iQuantity + ">");

        return iQuantity;
    }

    /**
     * Return the Part id to be synchronized with the CAD Object <sCADObjectParent>
     * @param sCADObjectParent
     *            CAD Object id
     * @param rowCADObjIds
     *            CAD Objects ids
     * @param rowPartObjIds
     *            Part ids
     * @return
     */
    private String getSelectedPartId(String sCADObjectParent, String[] rowCADObjIds, String[] rowPartObjIds) {
        String sPartParentId = null;

        int index = -1;
        for (int i = 0; i < rowCADObjIds.length; i++) {
            if (rowCADObjIds[i].contains(sCADObjectParent)) {
                index = Integer.parseInt(rowCADObjIds[i].split("[|]")[0]);
                break;
            }
        }
        logger.debug("getSelectedPartId() - sCADObjectParent:<" + sCADObjectParent + ">");
        logger.debug("checkPartAndParentTypes() - rowCADObjIds = <" + java.util.Arrays.toString(rowCADObjIds) + ">");
        logger.debug("checkPartAndParentTypes() - index = <" + index + ">");
        logger.debug("checkPartAndParentTypes() - rowPartObjIds = <" + java.util.Arrays.toString(rowPartObjIds) + ">");
        if (index != -1 && !"".equals(rowPartObjIds[index])) {
            sPartParentId = rowPartObjIds[index];
        }
        logger.debug("getSelectedPartId() - sPartParentId:<" + sPartParentId + ">");
        return sPartParentId;
    }

    /**
     * connect Part to CAD Object by the relationship RELATIONSHIP_PART_SPECIFICATION
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sPartId
     *            Part id
     * @param sCADObjectId
     *            CAD Object id
     * @param sGeometryType
     *            Geometry type value
     * @throws Exception
     *             if error occurs
     * @return the relationship object
     */
    private void connectPartToCADObject(Context context, String sPartId, String sCADObjectId, String sGeometryType, Map<String, String> mRelAttributes) throws Exception {
        try {

            DomainObject dobCADObj = DomainObject.newInstance(context, sCADObjectId);

            // Get Part id already connected to CAD Object
            StringList slLinkedPartId = dobCADObj.getInfoList(context, "to[" + RELATIONSHIP_PART_SPECIFICATION + "].from.id");
            logger.debug("connectPartToCADObject() - Part already connected :<" + slLinkedPartId + ">");

            if (!slLinkedPartId.contains(sPartId)) {
                DomainObject dobPart = DomainObject.newInstance(context, sPartId);
                connectPartAndCADObject(context, sPartId, dobPart, dobCADObj, mRelAttributes, sGeometryType);
                logger.debug("connectPartToCADObject() - Part:<" + sPartId + "> connected to CAD Obj:<" + sCADObjectId + ">");

            } else {
                logger.debug("connectPartToCADObject() - Part:<" + sPartId + "> is already connected to CAD Obj:<" + sCADObjectId + ">");
            }

        } catch (Exception ex) {
            logger.error("Error in connectPartToCADObject()\n", ex);
            throw ex;
        }
    }

    /**
     * Connect a Part to a CAD object with Part Specification relationship and set attributes values on created relation
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param doPart
     *            Part object
     * @param doCADObject
     *            CAD object
     * @param mRelAttributes
     *            Relation attributes values
     * @throws Exception
     */
    private void connectPartAndCADObject(Context context, String sPartId, DomainObject doPart, DomainObject doCADObject, Map mRelAttributes, String sGeometryType) throws Exception {

        String sHasFromConnectAccess = doPart.getInfo(context, "current.access[fromconnect]");
        StringList slPartsOfCADObject = doCADObject.getInfoList(context, "to[" + RELATIONSHIP_PART_SPECIFICATION + "].from.id");

        logger.debug("connectPartAndCADObject() - slPartsOfCADObject = <" + slPartsOfCADObject + "> sPartId = <" + sPartId + ">");

        if (!slPartsOfCADObject.contains(sPartId)) {
            // connect Part and CAD Object and set relation attributes values
            boolean bIsPushed = false;
            try {
                if (!"true".equalsIgnoreCase(sHasFromConnectAccess)) {
                    bIsPushed = true;
                    ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, null, null);
                }
                logger.debug("connectPartAndCADObject() - Connect CAD Object to Part");
                DomainRelationship doRel = DomainRelationship.connect(context, doPart, RELATIONSHIP_PART_SPECIFICATION, doCADObject);
                logger.debug("connectPartAndCADObject() - doRel = <" + doRel + "> mRelAttributes = <" + mRelAttributes + ">");

                if (mRelAttributes != null) {
                    doRel.setAttributeValues(context, mRelAttributes);
                }

            } catch (Exception e) {
                throw e;
            } finally {
                if (bIsPushed) {
                    ContextUtil.popContext(context);
                }
            }
        }
    }

    // Addition of Code for BOM To CAD Iteration-07
    /**
     * Return info to fill the table PSS_SynchronizationPartBOMSummary
     * @param context
     *            : the eMatrix <code>Context</code> object
     * @param args
     *            : Part Id
     * @param doExpand
     *            : indicates if the process should perform the expand on the EBOM connection
     * @return
     * @throws Exception
     */
    public MapList getEBOM(Context context, String[] args) throws Exception {

        return getEBOM(context, args, true);
    }

    /**
     * Return info to fill the table PSS_SynchronizationPartBOMSummary
     * @param context
     *            : the eMatrix <code>Context</code> object
     * @param args
     *            : Part Id
     * @param doExpand
     *            : indicates if the process should perform the expand on the EBOM connection
     * @return
     * @throws Exception
     */
    public MapList getEBOM(Context context, String[] args, boolean doExpand) throws Exception {

        MapList ebomList = new MapList();

        try {
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            String sPartId = (String) paramMap.get("objectId");
            Part partObj = new Part(sPartId);

            SelectList slSelectStmts = new SelectList();
            slSelectStmts.addElement(SELECT_LEVEL);
            slSelectStmts.addElement(SELECT_ID);
            slSelectStmts.addElement(SELECT_TYPE);
            slSelectStmts.addElement(SELECT_NAME);
            slSelectStmts.addElement(SELECT_REVISION);
            slSelectStmts.addElement(SELECT_CURRENT);
            slSelectStmts.addElement(SELECT_DESCRIPTION);
            slSelectStmts.addElement(SELECT_POLICY);
            // TIGTK-8512 - 2017-07-10 - VP - START
            slSelectStmts.addElement("project");
            // TIGTK-8512 - 2017-07-10 - VP - END

            StringList selectRelStmts = new StringList();
            selectRelStmts.addElement("from.id");
            // TIGTK-11319:START
            selectRelStmts.addElement("from.current");
            // TIGTK-11319:END

            selectRelStmts.addElement(SELECT_RELATIONSHIP_ID);

            Map mObjPart = partObj.getInfo(context, slSelectStmts);
            mObjPart.put(SELECT_LEVEL, "0");

            // add current part
            ebomList.add(mObjPart);

            // if the EBOM expand should be done
            if (doExpand) {
                // add EBOM
                MapList objEBOMList = partObj.getEBOMs(context, slSelectStmts, selectRelStmts, RELATIONSHIP_EBOM, false, Part.ALL_LEVELS, false);
                ebomList.addAll(objEBOMList);
            }

            // retrieve all CAD Objects
            ebomList = getRelatedCADObjects(context, ebomList);

            // sort EBOM list
            ebomList.addSortKey(SELECT_LEVEL, "ascending", "String");
            ebomList.addSortKey(SELECT_NAME, "ascending", "String");
            ebomList.sortStructure();
            int rowIndex = 0;
            for (Iterator iterator = ebomList.iterator(); iterator.hasNext();) {
                Map mObj = (Map) iterator.next();
                mObj.put("from.id", (String) mObj.get(SELECT_RELATIONSHIP_ID));
                mObj.put(SELECT_RELATIONSHIP_ID, Integer.toString(rowIndex));
                rowIndex++;
                // Modification Done by PTE for TIGTK-4161
                String strCurrentGMType = EMPTY_STRING;
                String sCADObjID = (String) mObj.get("CADObjectId");
                if (UIUtil.isNotNullAndNotEmpty(sCADObjID)) {
                    DomainObject domainObj = DomainObject.newInstance(context, sCADObjID);
                    strCurrentGMType = domainObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE);
                }

                if ("false".equalsIgnoreCase((String) mObj.get("IsSynchronize"))) {
                    mObj.put("attribute[" + TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE + "]", strCurrentGMType);
                }
                // Ends for TIGTK-4161
            }

            JPO.packArgs(ebomList);
        } catch (Exception ex) {
            logger.error("Error in getEBOM()\n", ex);
            throw ex;
        }
        return ebomList;
    }

    /**
     * Get related CAD Objects
     * @param context
     *            : the eMatrix <code>Context</code> object
     * @param ebomList
     *            : EBOM list
     * @return
     * @throws Exception
     */
    public MapList getRelatedCADObjects(Context context, MapList ebomList) throws Exception {
        MapList mlFinal = new MapList();
        try {

            SelectList slSelectStmts = new SelectList();
            slSelectStmts.addElement(SELECT_ID);
            slSelectStmts.addElement(SELECT_TYPE);
            slSelectStmts.addElement(SELECT_NAME);
            slSelectStmts.addElement(SELECT_REVISION);
            slSelectStmts.addElement(SELECT_CURRENT);
            slSelectStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE + "]");
            slSelectStmts.addElement("to[" + RELATIONSHIP_PART_SPECIFICATION + "].from.id");
            slSelectStmts.addElement("last.revision");

            SelectList slSelectRelationship = new SelectList();
            DomainObject dobCADObject = DomainObject.newInstance(context);
            boolean bTreatedObject = false;

            for (Iterator iterator = ebomList.iterator(); iterator.hasNext();) {
                Map mPartObj = (Map) iterator.next();

                String sPartName = (String) mPartObj.get(SELECT_NAME);
                String sPartId = (String) mPartObj.get(SELECT_ID);

                // get not synchronized CAD Objects which the name starts as the
                // Part name
                MapList mlNotSynchronizedCADObjects = getNotSynchronizedCADObjects(context, sPartName, slSelectStmts);

                // get the CAD Objects connected to the Part
                MapList mlSynchronizedCADObjects = getSynchronizedCADObjects(context, sPartId, slSelectStmts, slSelectRelationship);

                // get the CAD objects last revisions of the previous part that
                // are not synchronized
                MapList mlNotSynchronizedPreviousPartCADObjects = getNotSynchronizedPreviousPartCADObjects(context, sPartId, slSelectStmts);

                MapList mlAllCADObjects = new MapList();
                mlAllCADObjects.addAll(mlSynchronizedCADObjects);
                mlAllCADObjects.addAll(mlNotSynchronizedCADObjects);
                mlAllCADObjects.addAll(mlNotSynchronizedPreviousPartCADObjects);

                if (mlAllCADObjects.size() > 0) {
                    ArrayList<String> slCADObjectIds = new ArrayList<String>();
                    String strLocale = context.getSession().getLanguage();
                    for (Iterator iterator2 = mlAllCADObjects.iterator(); iterator2.hasNext();) {

                        Map mObject = (Map) iterator2.next();
                        // CAD Object id
                        String sCADObjID = (String) mObject.get(SELECT_ID);
                        // CAD Objects revisions
                        dobCADObject.setId(sCADObjID);
                        BusinessObjectList bolCADObjRevisions = dobCADObject.getRevisions(context);
                        // check if the CAD Object has already added to the
                        // final list
                        bTreatedObject = false;
                        for (Iterator itRevisions = bolCADObjRevisions.iterator(); itRevisions.hasNext();) {
                            String sRevisionId = ((BusinessObject) itRevisions.next()).getObjectId();
                            logger.debug("getRelatedCADObjects() - sRevisionId = <" + sRevisionId + ">");
                            if (slCADObjectIds.contains(sRevisionId)) {
                                bTreatedObject = true;
                                break;
                            }

                        }
                        if (!bTreatedObject) { // check if the CAD Object has
                            // already added to the final
                            // list
                            String sCADObjType = (String) mObject.get(SELECT_TYPE);
                            String sCADObjName = (String) mObject.get(SELECT_NAME);
                            String sCADObjRev = (String) mObject.get(SELECT_REVISION);
                            String sCADObjLastRev = (String) mObject.get("last.revision");
                            String sCADObjCurrent = (String) mObject.get(SELECT_CURRENT);
                            // TIGTK - 17791 : stembulkar : start
                            if (!sCADObjCurrent.equalsIgnoreCase(TigerConstants.STATE_PSS_CANCELPART_CANCELLED) && !sCADObjCurrent.equalsIgnoreCase(TigerConstants.STATE_OBSOLETE)) {
                                // TIGTK - 17791 : stembulkar : end
                                String sCADObjPolicy = (String) mObject.get(SELECT_POLICY);

                                String sCADObjGeometryType = (String) mObject.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE + "]");
                                StringList slConnectedParts = toStringList(mObject.get("to[" + RELATIONSHIP_PART_SPECIFICATION + "].from.id"));
                                boolean bIsSychonized = false;
                                if (slConnectedParts.contains(sPartId)) {
                                    bIsSychonized = true;
                                }

                                Map mPartFinal = new HashMap();
                                mPartFinal.putAll(mPartObj);
                                mPartFinal.put("CADObjectId", sCADObjID);
                                mPartFinal.put("CADObjectType", sCADObjType);
                                mPartFinal.put("CADObjectTypeDisplay", i18nNow.getTypeI18NString(sCADObjType, strLocale));
                                mPartFinal.put("CADObjectName", sCADObjName);
                                mPartFinal.put("CADObjectRevision", sCADObjRev);
                                mPartFinal.put("CADObjectLastRevision", sCADObjLastRev);
                                mPartFinal.put("CADObjectCurrent", sCADObjCurrent);
                                mPartFinal.put("attribute[" + TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE + "]", sCADObjGeometryType);
                                mPartFinal.put("CADObjectPolicy", sCADObjPolicy);
                                mPartFinal.put("IsSynchronize", String.valueOf(bIsSychonized));

                                mlFinal.add(mPartFinal);
                                slCADObjectIds.add(sCADObjID);
                            }
                        } else {
                            logger.debug("getRelatedCADObjects() - The CAD Object with ID " + sCADObjID + " seems to have already been treated");
                        }
                    }
                } else {
                    Map mPartFinal = new HashMap();
                    mPartFinal.putAll(mPartObj);

                    mPartFinal.put("CADObjectId", "");
                    mPartFinal.put("CADObjectType", "");
                    mPartFinal.put("CADObjectName", "");
                    mPartFinal.put("CADObjectRevision", "");
                    mPartFinal.put("CADObjectCurrent", "");
                    mPartFinal.put("CADObjectPolicy", "");
                    mlFinal.add(mPartFinal);
                }

            }
            logger.debug("getRelatedCADObjects() - mlFinal:<" + mlFinal + ">");
        } catch (Exception ex) {
            logger.error("Error in getRelatedCADObjects()\n", ex);
            throw ex;
        }

        return mlFinal;
    }

    /**
     * Return the next revisions of CAD objects linked to the previous revision of the Part<br>
     * Return also the release CAD objects linked to the previous revision of the Part if it is the last revision of the CAD object <br>
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sPartId
     *            Part id
     * @param slSelectStmts
     *            select statement to get on objects
     * @return
     * @throws Exception
     */

    public MapList getNotSynchronizedPreviousPartCADObjects(Context context, String sPartId, SelectList slSelectStmts) throws Exception {

        String sCADObjectStateRelease = PropertyUtil.getSchemaProperty(context, "policy", TigerConstants.POLICY_PSS_CADOBJECT, "state_Release");

        MapList mlCADObjects = new MapList();
        try {

            DomainObject dobPart = DomainObject.newInstance(context, sPartId);

            BusinessObject bo = dobPart.getPreviousRevision(context);

            if (bo.exists(context)) {
                bo.open(context);
                DomainObject dobPreviousRev = DomainObject.newInstance(context, bo);
                bo.close(context);

                StringList sCADObjectIdList = dobPreviousRev.getInfoList(context, "from[" + RELATIONSHIP_PART_SPECIFICATION + "].to.id");
                SelectList slBus = new SelectList();
                slBus.add(SELECT_ID);
                slBus.add(SELECT_CURRENT);
                slBus.add("to[" + RELATIONSHIP_PART_SPECIFICATION + "]");
                slBus.add(SELECT_POLICY);

                String sCADObjectId = "";
                String sCADObjectRevId = "";
                String sCADObjectRevCurrent = "";
                String sPartSpecification = "";
                DomainObject dobCADObject = DomainObject.newInstance(context);
                BusinessObject boCAD = null;
                boolean isLastRevision = true;
                for (Iterator iterator = sCADObjectIdList.iterator(); iterator.hasNext();) {
                    sCADObjectId = (String) iterator.next();
                    dobCADObject.setId(sCADObjectId);
                    isLastRevision = true;
                    int i = 0;// limit to avoid unexpected infinite loop, and
                    // server freeze
                    while (!dobCADObject.isLastRevision(context) && i++ < 255) {
                        isLastRevision = false;
                        boCAD = dobCADObject.getNextRevision(context);
                        boCAD.open(context);
                        dobCADObject = DomainObject.newInstance(context, boCAD);
                        boCAD.close(context);
                    }
                    if (dobCADObject.isLastRevision(context)) {
                        Map mCADObject = dobCADObject.getInfo(context, slBus);
                        sCADObjectRevId = (String) mCADObject.get(SELECT_ID);
                        sCADObjectRevCurrent = (String) mCADObject.get(SELECT_CURRENT);
                        sPartSpecification = (String) mCADObject.get("to[" + RELATIONSHIP_PART_SPECIFICATION + "]");
                        logger.debug("sCADObjectRevId = <" + sCADObjectRevId + "> Current = <" + sCADObjectRevCurrent + "> sPartSpecification = <" + sPartSpecification + ">");

                        if ("false".equalsIgnoreCase(sPartSpecification) || (isLastRevision && sCADObjectStateRelease.equals(sCADObjectRevCurrent))) {
                            mlCADObjects.add(dobCADObject.getInfo(context, slSelectStmts));
                        }
                    }
                }
            }

        } // Fix for FindBugs issue RuntimeException capture: Suchit Gangurde: 28 Feb 2017: START
        catch (RuntimeException e) {
            // TIGTK-5405 - 03-04-2017 - VP - START
            logger.error("Error in getNextFindNumber: ", e);
            // TIGTK-5405 - 03-04-2017 - VP - END
            throw e;
        } // Fix for FindBugs issue RuntimeException capture: Suchit Gangurde: 28 Feb 2017: END
        catch (Exception e) {
            logger.error("Error in getNotSynchronizedPreviousPartCADObjects()\n", e);
            throw e;
        }

        return mlCADObjects;
    }

    /**
     * Get CAD Objects connected to the Part
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sPartId
     *            Part id
     * @return
     * @throws Exception
     */
    public MapList getSynchronizedCADObjects(Context context, String sPartId, SelectList slSelectStmts, SelectList slSelectRelationship) throws Exception {

        final String RANGE_BASIS_DEFINITION = "BD";
        String RECONCILIATION_AUTHORIZED_TYPES = TigerConstants.TYPE_MCAD_MODEL + "," + TigerConstants.TYPE_MCADDRAWING;

        MapList mlCADObjects = new MapList();
        if (sPartId != null && !"".equals(sPartId.trim())) {
            try {
                DomainObject dobPart = DomainObject.newInstance(context, sPartId);
                String sRelPattern = RELATIONSHIP_PART_SPECIFICATION;
                String sObjPattern = RECONCILIATION_AUTHORIZED_TYPES;
                String sClause = "(attribute[" + TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE + "]!= \"" + RANGE_BASIS_DEFINITION + "\" )";
                // String sRelClause = "";
                String sRelClause = "(from.id ==" + sPartId + ")";

                // get related CAD Objects
                mlCADObjects = dobPart.getRelatedObjects(context, sRelPattern, sObjPattern, slSelectStmts, slSelectRelationship, true, true, (short) 1, sClause, sRelClause, 0);

                // Added for JIRA Ticket 1457 ---By Pooja -- CAD-BOM Stream
                // if (mlCADObjects.size() > 0) {
                for (int i = 0; i < mlCADObjects.size(); i++) {
                    Map mapCADObject = (Map) mlCADObjects.get(i);
                    String strCADObjectId = (String) mapCADObject.get(DomainConstants.SELECT_ID);
                    DomainObject domCADObject = DomainObject.newInstance(context, strCADObjectId);

                    StringList objselect = new StringList();
                    objselect.addElement(DomainConstants.SELECT_ID);
                    objselect.addElement(DomainConstants.SELECT_NAME);
                    objselect.addElement(DomainConstants.SELECT_TYPE);
                    objselect.addElement(DomainConstants.SELECT_REVISION);
                    objselect.addElement(DomainConstants.SELECT_CURRENT);
                    objselect.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE + "]");

                    if (domCADObject.isKindOf(context, TigerConstants.TYPE_MCAD_MODEL)) {
                        // Find Bug Issue Dead Local Store : Priyanka Salunke : 28-Feb-2017
                        MapList mlAssociatedDrawingList = domCADObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_ASSOCIATEDDRAWING, TigerConstants.TYPE_MCADDRAWING, objselect, null,
                                false, true, (short) 0, null, null, 0);
                        if (mlAssociatedDrawingList.size() > 0) {
                            mlCADObjects.addAll(mlAssociatedDrawingList);
                        }
                    }
                }

                // End of addition for JIRA Ticket 1457 ---By Pooja -- CAD-BOM
                // Stream
            } catch (Exception ex) {
                logger.error("Error in getSynchronizedCADObjects()\n", ex);
                throw ex;
            }
        }

        return mlCADObjects;
    }

    /**
     * Get not synchronized CAD Objects which the name starts as the Part name (also with name in lower case)
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sPartId
     *            Part id
     * @return
     * @throws Exception
     */

    public MapList getNotSynchronizedCADObjects(Context context, String sPartName, SelectList slSelectStmts) throws Exception {
        final String RANGE_BASIS_DEFINITION = "BD";
        String RECONCILIATION_AUTHORIZED_TYPES = TigerConstants.TYPE_MCAD_MODEL + "," + TigerConstants.TYPE_MCADDRAWING;
        MapList mlNotSynchronizedCADObjects = new MapList();

        try {
            String sClause = "(attribute[" + TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE + "]!= \"" + RANGE_BASIS_DEFINITION + "\" )";
            sClause += " &&  (to[" + RELATIONSHIP_PART_SPECIFICATION + "] == False)";
            sClause += "&& policy == " + "\"" + TigerConstants.POLICY_PSS_CADOBJECT + "\"";
            String sObjPattern = RECONCILIATION_AUTHORIZED_TYPES;
            String sSearchName = "*" + sPartName + "*," + "*" + sPartName.toLowerCase() + "*";
            MapList mlCADObjects = DomainObject.findObjects(context, sObjPattern, sSearchName, "*", "*", TigerConstants.VAULT_ESERVICEPRODUCTION, sClause, true, slSelectStmts);

            Map<String, String> mObjectRevision = new HashMap<String, String>();
            for (Iterator iterator = mlCADObjects.iterator(); iterator.hasNext();) {
                Map mCADObject = (Map) iterator.next();

                String sCADObjectType = (String) mCADObject.get(SELECT_TYPE);
                String sCADObjectName = (String) mCADObject.get(SELECT_NAME);
                String sCADObjectRev = (String) mCADObject.get(SELECT_REVISION);
                String sRev = mObjectRevision.get(sCADObjectType + sCADObjectName);

                if (sRev == null || compareCADObjectRevisions(sRev, sCADObjectRev) < 0) {
                    mObjectRevision.put(sCADObjectType + sCADObjectName, sCADObjectRev);
                    mlNotSynchronizedCADObjects.add(0, mCADObject);
                } else {
                    mlNotSynchronizedCADObjects.add(mCADObject);
                }
            }

        } catch (Exception ex) {
            logger.error("Error in getNotSynchronizedCADObjects()\n", ex);
            throw ex;
        }

        return mlNotSynchronizedCADObjects;
    }

    /**
     * Compares the revision of CAD Objects
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sRev1
     *            First revision of CAD Object
     * @param sRev2
     *            Second revision of CAD Object
     * @return == int -- Status of CAD Object Revision Comparison
     * @throws Exception
     */
    public int compareCADObjectRevisions(String sRev1, String sRev2) {
        if (sRev1.length() == sRev2.length()) {
            return sRev1.compareTo(sRev2);
        } else if (sRev1.length() > sRev2.length()) {
            return -1;
        }
        return 1;
    }

    /**
     * Returns data for the part specification connection between the part with the specified id and the CAD object with the specified id.
     * @param context
     *            the ematrix context
     * @param sPartObjectId
     *            the part id
     * @param sCADObjectId
     *            the CAD Object id
     * @return a MapList with connection
     * @throws Exception
     * @since PDM 2.8.0
     */
    public MapList getPartSpecificationInfo(Context context, String sPartObjectId, String sCADObjectId) throws Exception {
        logger.debug("getPartSpecificationInfo() - sPartObjectId = <" + sPartObjectId + "> sCADObjectId = <" + sCADObjectId + ">");

        MapList partCADObjects = new MapList();
        if (sPartObjectId == null || sCADObjectId == null || "".equals(sPartObjectId.trim()) || "".equals(sCADObjectId.trim())) {
            return partCADObjects;
        }

        DomainObject doPart = new DomainObject(sPartObjectId);

        StringList slSelectStmts = new SelectList();
        slSelectStmts.addElement(SELECT_ID);
        slSelectStmts.addElement(SELECT_NAME);
        slSelectStmts.addElement(SELECT_TYPE);
        slSelectStmts.addElement(SELECT_REVISION);
        slSelectStmts.addElement(SELECT_CURRENT);

        StringList slSelectRelationship = new StringList();
        slSelectRelationship.addElement(SELECT_RELATIONSHIP_ID);

        String sWhereObjects = "(id == " + sCADObjectId + ")";

        partCADObjects = doPart.getRelatedObjects(context, RELATIONSHIP_PART_SPECIFICATION, "*", slSelectStmts, slSelectRelationship, false, true, (short) 1, sWhereObjects, "", 0);
        logger.debug("getPartSpecificationInfo() - related objects founds : < " + partCADObjects + " >");

        return partCADObjects;
    }

    /**
     * Return info to fill the table PSS_SynchronizationPartBOMSummary<br>
     * Return only first level Parts of the EBom
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            Part Id
     * @return
     * @throws Exception
     * @plm.usage table PSS_SynchronizationPartBOMSummary
     */

    public MapList getEBOMNoExpand(Context context, String[] args) throws Exception {
        return getEBOM(context, args, false);
    }

    /**
     * Return info to fill the table PSS_SynchronizationPartBOMSummary<br>
     * Return the Parts of the EBom which are not synchronized with a CAD Object
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            Part Id
     * @return
     * @throws Exception
     * @plm.usage table PSS_SynchronizationPartBOMSummary
     */

    public MapList getEBOMNotSynchronized(Context context, String[] args) throws Exception {
        MapList mlNotSynchronizedEBOMList = new MapList();
        logger.debug("getEBOMNotSynchronized() - start ...");
        try {
            MapList ebomList = getEBOM(context, args);

            String sIsSynchronize = "";
            Map mObject = null;
            int rowIndex = 0;
            for (Iterator iterator = ebomList.iterator(); iterator.hasNext();) {
                mObject = (Map) iterator.next();

                sIsSynchronize = (String) mObject.get("IsSynchronize");
                logger.debug("getEBOMNotSynchronized() - sIsSynchronize = <" + sIsSynchronize + ">");

                if (!"true".equalsIgnoreCase(sIsSynchronize)) {
                    mObject.put(SELECT_RELATIONSHIP_ID, Integer.toString(rowIndex));
                    rowIndex++;
                    mlNotSynchronizedEBOMList.add(mObject);
                }
            }

        } catch (Exception ex) {
            logger.error("Error in getEBOMNotSynchronized()\n", ex);
            throw ex;
        }
        logger.debug("getEBOMNotSynchronized() - end.");

        return mlNotSynchronizedEBOMList;
    }

    /**
     * Return the Add new line column for the table PSS_ynchronizationPartBOMSummary
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @return
     * @throws Exception
     */
    public Vector<String> getAddLineIconBOMToCAD(Context context, String[] args) throws Exception {
        try {
            int CAD_OBJECT_NAME_COLUMN_INDEX = 6;
            int CAD_OBJECT_REVISON_COLUMN_INDEX = 7;
            int CAD_OBJECT_TYPE_COLUMN_INDEX = 8;
            int CAD_OBJECT_STATE_COLUMN_INDEX = 9;

            Vector<String> vResult = new Vector<String>();

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");

            Map objectMap = null;
            String sPartObjId;
            int index = 0;
            String sBlank = "";
            String sLink = "";
            Iterator objectListItr = objectList.iterator();
            while (objectListItr.hasNext()) {
                objectMap = (Map) objectListItr.next();
                sPartObjId = (String) objectMap.get(SELECT_ID);
                // TIGTK-11319:START
                boolean bCheck = false;
                String strState = (String) objectMap.get(SELECT_CURRENT);

                if ((TigerConstants.STATE_PSS_ECPART_PRELIMINARY.equals(strState) || TigerConstants.STATE_PSS_DEVELOPMENTPART_CREATE.equals(strState))) {
                    bCheck = true;
                }
                // TIGTK-11319:END

                if (bCheck) {

                    String sLinkPrefix = "<a href=\"";
                    String sLinkSuffix = "\">";
                    String sLinkImage = "<img border=\"0\" title=\"Add new line\" src=\"../common/images/iconActionAdd.gif\">";
                    String sLinkEnd = "</a>";

                    String aCulumnsTextEdit = "[" + CAD_OBJECT_NAME_COLUMN_INDEX + "]";
                    String aCulumnsText = "[" + CAD_OBJECT_REVISON_COLUMN_INDEX + "," + CAD_OBJECT_TYPE_COLUMN_INDEX + "," + CAD_OBJECT_STATE_COLUMN_INDEX + "]";

                    int indexTableRow;
                    indexTableRow = index + 2;

                    sLink = "JavaScript:addRowToTable(document," + indexTableRow + "," + aCulumnsTextEdit + "," + aCulumnsText + ")";

                    vResult.add(sLinkPrefix + sLink + sLinkSuffix + sLinkImage + sLinkEnd);
                } else {
                    vResult.add(sBlank);
                }

                index++;

            }
            return vResult;
        } catch (Exception ex) {
            logger.error("Error in getAddLineIcon()\n", ex);
            throw ex;
        }
    }

    /**
     * Return the CAD Object Name column for the table PSS_SynchronizationPartBOMSummary
     * @param context
     *            : the eMatrix <code>Context</code> object
     * @param args
     *            : holds no arguments
     * @return
     * @throws Exception
     */
    public Vector<String> getCADObjectNameBOMToCAD(Context context, String[] args) throws Exception {
        try {
            int CAD_OBJECT_NAME_COLUMN_INDEX = 6;
            int CAD_OBJECT_REVISON_COLUMN_INDEX = 7;
            int CAD_OBJECT_TYPE_COLUMN_INDEX = 8;
            int CAD_OBJECT_STATE_COLUMN_INDEX = 9;

            Vector<String> vCADObjNames = new Vector<String>();

            String RECONCILIATION_AUTHORIZED_TYPES = TigerConstants.TYPE_MCAD_MODEL + "," + TigerConstants.TYPE_MCADDRAWING;

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");
            String aCulumnsTextEdit = "[" + CAD_OBJECT_NAME_COLUMN_INDEX + "]";
            String aCulumnsText = "[" + CAD_OBJECT_REVISON_COLUMN_INDEX + "," + CAD_OBJECT_TYPE_COLUMN_INDEX + "," + CAD_OBJECT_STATE_COLUMN_INDEX + "]";

            // TIGTK-9915 : 18/09/2017 : AB : START
            // get the Coolabrative Space of current User
            String strSecurityContext = PersonUtil.getDefaultSecurityContext(context, context.getUser());
            String strCollaborativeSpace = (strSecurityContext.split("[.]")[2]);

            String strSearchURL = "../common/emxFullSearch.jsp?field=TYPES=" + RECONCILIATION_AUTHORIZED_TYPES + ":Policy!=policy_VersionedDesignPolicy:PROJECT=" + strCollaborativeSpace
                    + "&table=ENCGeneralSearchResult&selection=multiple&submitAction=refreshCaller&AddExistingSpecification=true&submitURL=../engineeringcentral/PSS_EngineeringCentralCommonProcess.jsp?PSS_mode=PSS_SearchCAD";

            // TIGTK-9915 : 18/09/2017 : AB : END

            int index = 0;
            StringBuffer outPut = null;
            // int indexTableRow;
            Map objectMap = null;
            String sCADObjId = "";
            String sPartState = "";
            String sCADObjRevision = "";
            String sCADObjName = "";
            String sPartId = "";
            Iterator objectListItr = objectList.iterator();

            while (objectListItr.hasNext()) {
                objectMap = (Map) objectListItr.next();
                sPartId = (String) objectMap.get(SELECT_ID);
                sPartState = (String) objectMap.get(SELECT_CURRENT);

                sCADObjId = (String) objectMap.get("CADObjectId");
                sCADObjRevision = (String) objectMap.get("CADObjectRevision");
                sCADObjName = (String) objectMap.get("CADObjectName");
                if (sCADObjName == null) {
                    sCADObjName = "";
                }

                outPut = new StringBuffer();
                // TIGTK-11319 :START
                boolean bCheck = false;
                if (TigerConstants.STATE_PSS_ECPART_PRELIMINARY.equals(sPartState) || TigerConstants.STATE_PSS_DEVELOPMENTPART_CREATE.equals(sPartState)) {
                    bCheck = true;
                }
                // TIGTK-11319 :END

                if (bCheck) {
                    outPut.append("<NOBR>");
                    outPut.append("<input type=\"text\" name=\"CADObjectName\" size=\"15\" value=\"" + sCADObjName + "\"");
                    outPut.append(" >&nbsp;");
                    outPut.append("<input class=\"button\" type=\"button\"");
                    outPut.append(" name=\"btnCADObjectChooser\" size=\"6\" ");
                    outPut.append("value=\"...\" alt=\"\"  onClick=\"javascript:showChooser('");

                    outPut.append(strSearchURL);
                    if (sPartId != null && !"".equals(sPartId)) {
                        outPut.append("&objectId=" + sPartId + "&");
                    }

                    outPut.append("FieldNumber=" + index);
                    outPut.append("', 700, 500)\">&nbsp;&nbsp;");
                    outPut.append("<script language=\"javascript\" src=\"../common/scripts/emxUITableUtil.js\"></script>");
                    outPut.append("<a href=\"javaScript:updateCADObjectRevisions(" + index + ")");
                    outPut.append("\">");
                    outPut.append("<img border=\"0\" title=\"Update CAD Object revision\" src=\"../common/images/iconActionReset.gif\">");
                    outPut.append("</a>&nbsp;&nbsp;");

                    outPut.append("<a href=\"JavaScript:clearTableCellTextEdit(" + index + "," + aCulumnsTextEdit + ");");
                    outPut.append("clearTableCell(" + index + "," + aCulumnsText + ");");
                    outPut.append("\">");
                    outPut.append("<img border=\"0\" title=\"Reset Fields\" src=\"../common/images/iconActionRefresh.gif\">");
                    outPut.append("</a>");
                    outPut.append("<input type=\"hidden\" name=\"CADObjectId\" value=\"" + sCADObjId + "\"/>");
                    outPut.append("<input type=\"hidden\" name=\"CADObjectRevision\" value=\"" + sCADObjRevision + "\"/>");
                    outPut.append("</NOBR>");
                    vCADObjNames.add(outPut.toString());

                } else {

                    outPut.append(sCADObjName);
                    // TIGTK-9710 : START
                    outPut.append("<input type=\"hidden\" name=\"CADObjectId\" value=\"" + sCADObjId + "\"/>");
                    outPut.append("<input type=\"hidden\" name=\"CADObjectRevision\" value=\"" + sCADObjRevision + "\"/>");
                    outPut.append("<input type=\"hidden\" name=\"CADObjectName\" value=\"" + sCADObjName + "\"/>");
                    // TIGTK-9710 : END
                    vCADObjNames.add(outPut.toString());
                }

                index++;
            }
            return vCADObjNames;
        } catch (Exception ex) {
            logger.error("Error in getCADObjectName ", ex);
            throw ex;
        }
    }

    /**
     * Return the CAD Object Revision column for the table PSS_SynchronizationPartBOMSummary
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @return
     * @throws Exception
     */
    public Vector<String> getCADObjectRevisionBOMToCAD(Context context, String[] args) throws Exception {
        try {
            Vector<String> vCADObjNames = new Vector<String>();

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");

            MapList mlCADObjectRevisions = null;
            StringBuffer outPut = null;
            Map objectMap = null;
            String sPartObjId = "";
            String sCADObjId = "";
            String sCADObjRev = "";
            String sCADObjRevToSelect = null;
            String sIsSynchronize = "";
            int index = 0;
            Iterator objectListItr = objectList.iterator();
            StringList slCADDefinitionAllRevision = null;
            String sCadDefName = null;

            while (objectListItr.hasNext()) {
                boolean bCheck = false;
                objectMap = (Map) objectListItr.next();
                sCadDefName = (String) objectMap.get("CADDefinitionName");
                slCADDefinitionAllRevision = getStringListObjects(objectMap.get("CADDefinitionAllRevisions"));
                sCADObjId = (String) objectMap.get("CADObjectId");
                sPartObjId = (String) objectMap.get(SELECT_ID);
                sCADObjRev = (String) objectMap.get("CADObjectRevision");
                sIsSynchronize = (String) objectMap.get("IsSynchronize");

                logger.debug("getCADObjectRevision() - sCADObjId:<" + sCADObjId + "> sCADObjRev:<" + sCADObjRev + "> sIsSynchronize:<" + sIsSynchronize + ">");
                // TIGTK - 17791 : stembulkar : start
                MapList mlFinalList = new MapList();
                // TIGTK - 17791 : stembulkar : end
                // TIGTK-11319 :START
                String strState = (String) objectMap.get(SELECT_CURRENT);
                if ((TigerConstants.STATE_PSS_ECPART_PRELIMINARY.equals(strState) || TigerConstants.STATE_PSS_DEVELOPMENTPART_CREATE.equals(strState))) {
                    bCheck = true;
                }
                // TIGTK-11319 :END

                if (bCheck) {

                    sCADObjRevToSelect = sCADObjRev;

                    // get CAD object revisions
                    mlCADObjectRevisions = getCADObjectRevisions(context, sCADObjId);
                    // TIGTK-17791 : stembulkar : start
                    for (int i = 0; i < mlCADObjectRevisions.size(); i++) {
                        Map cadRev = (Map) mlCADObjectRevisions.get(i);
                        String sCurrent = (String) cadRev.get(SELECT_CURRENT);
                        if (!sCurrent.equals(TigerConstants.STATE_OBSOLETE) && !sCurrent.equals(TigerConstants.STATE_PSS_CANCELPART_CANCELLED)) {
                            mlFinalList.add(cadRev);
                        }
                    }
                    // construct Select list field from revisions list
                    outPut = constructCADObjectRevisionsSelectList(context, mlFinalList, sCADObjRevToSelect, Integer.toString(index), sCadDefName, slCADDefinitionAllRevision);
                    // TIGTK-17791 : stembulkar : end

                    vCADObjNames.add(outPut.toString());
                } else {
                    outPut = new StringBuffer();
                    outPut.append(sCADObjRev);
                    outPut.append("<input type=\"hidden\" name=\"CADObjectRevisions\" value=\"" + sCADObjRev + "\"/>");
                    vCADObjNames.add(outPut.toString());
                }
                index++;
            }
            return vCADObjNames;
        } catch (Exception ex) {
            logger.error("Error in getCADObjectRevision ", ex);
            throw ex;
        }
    }

    /**
     * Return informations of all revisions of the CAD Object
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sCADObjId
     *            CAD Object id
     * @return
     * @throws Exception
     */

    public MapList getCADObjectRevisions(Context context, String sCADObjId) throws Exception {
        final String ATTRIBUTE_PSS_GEOMETRY_TYPE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_GeometryType");
        MapList mlCADObjectRevisions = new MapList();

        if (!"".equals(sCADObjId)) {
            SelectList slSelectStmts = new SelectList();
            slSelectStmts.addElement(SELECT_ID);
            slSelectStmts.addElement(SELECT_NAME);
            slSelectStmts.addElement(SELECT_TYPE);
            slSelectStmts.addElement(SELECT_REVISION);
            slSelectStmts.addElement(SELECT_CURRENT);
            slSelectStmts.addElement(SELECT_DESCRIPTION);
            slSelectStmts.addElement("last");
            slSelectStmts.addElement("attribute[" + ATTRIBUTE_PSS_GEOMETRY_TYPE + "]");

            DomainObject dobCADObject = DomainObject.newInstance(context, sCADObjId);
            // get CAD objects revisions
            mlCADObjectRevisions = dobCADObject.getRevisionsInfo(context, slSelectStmts, new StringList());
        }

        return mlCADObjectRevisions;
    }

    /**
     * Construct a HTML select list from a list of CAD object revisions
     * @param mlCADObjectRevisions
     *            CAD object revisions list
     * @param sCADObjectRev
     *            Selected CAD object revision
     * @param sFieldNumber
     *            the table row number in the synchronization window
     * @return
     * @throws Exception
     */
    public StringBuffer constructCADObjectRevisionsSelectList(Context context, MapList mlCADObjectRevisions, String sCADObjectRev, String sFieldNumber, String sCadDefName,
            StringList slCadDefRevisions) throws Exception {
        try {
            String sLanguage = context.getSession().getLanguage();
            StringBuffer sbRevOption = new StringBuffer(EMPTY_STRING);
            logger.debug("constructCADObjectRevisionsSelectList() - Starting");

            if (mlCADObjectRevisions == null || mlCADObjectRevisions.size() == 0) {
                sbRevOption.append("<select name='CADObjectRevisions'>");
                // Findbug Issue correction start
                // Date: 22/03/2017
                // By: Asha G.
                sbRevOption.append("<option ");
                sbRevOption.append(" value='");
                sbRevOption.append(EMPTY_STRING);
                sbRevOption.append("'>");
                sbRevOption.append(EMPTY_STRING);
                sbRevOption.append("</option>");
                sbRevOption.append("</select>");
                // Findbug Issue correction End
                return sbRevOption;
                // return new StringBuffer(EMPTY_STRING);
            }

            StringBuffer sbOptionValues = new StringBuffer("");
            String sSelected = "";
            Map mCADObject = null;
            String sId = "";
            String sName = "";
            String sRevision = "";
            String sLastRevision = "";
            String sType = "";
            String sCurrent = "";
            String sPolicy = "";
            String sGeometryType = "";
            StringBuffer sbValue = null;
            String sAllowedCadDefMinorRevisions = "";
            String sSynchronizedCADDefinitionName = null;
            String sSynchronizedCADDefinitionRevision = null;
            String sSynchronizedCADDefinitionMajorRev = null;

            sAllowedCadDefMinorRevisions = join(slCadDefRevisions, ",");
            if (UIUtil.isNotNullAndNotEmpty(sAllowedCadDefMinorRevisions)) {
                sAllowedCadDefMinorRevisions = "00";
            }
            logger.debug("constructCADObjectRevisionsSelectList() - sAllowedCadDefMinorRevisions = <" + sAllowedCadDefMinorRevisions + ">");

            String strLocale = context.getSession().getLanguage();
            for (Iterator iterator = mlCADObjectRevisions.iterator(); iterator.hasNext();) {
                mCADObject = (Map) iterator.next();
                sId = (String) mCADObject.get(SELECT_ID);
                sName = (String) mCADObject.get(SELECT_NAME);
                sRevision = (String) mCADObject.get(SELECT_REVISION);
                sLastRevision = (String) mCADObject.get("last");
                sType = (String) mCADObject.get(SELECT_TYPE);
                String sTypeDisplay = i18nNow.getTypeI18NString(sType, strLocale);
                sCurrent = (String) mCADObject.get(SELECT_CURRENT);
                sPolicy = (String) mCADObject.get(SELECT_POLICY);
                String sCurrentStateTranslated = i18nNow.getStateI18NString(sPolicy, sCurrent, sLanguage);
                sGeometryType = (String) mCADObject.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE + "]");

                if (UIUtil.isNotNullAndNotEmpty(sId)) {
                    sbValue = new StringBuffer(sId);
                } else {
                    sbValue = new StringBuffer();
                    sbOptionValues.append(sbValue);
                }

                // Find Bug : Dodgy Code : PS : 21-March-2017 : START
                sbValue.append("|");
                sbValue.append(sName);
                sbValue.append("|");
                sbValue.append(sTypeDisplay);
                sbValue.append("|");
                sbValue.append(sCurrentStateTranslated);
                sbValue.append("|");
                sbValue.append(sGeometryType);
                // Find Bug : Dodgy Code : PS : 21-March-2017 : END

                logger.debug("constructCADObjectRevisionsSelectList() - sbValue = <" + sbValue + ">");
                if (sCADObjectRev != null) {
                    if (sRevision.equals(sCADObjectRev)) {
                        sSelected = "selected";
                    } else {
                        sSelected = "";
                    }
                } else {
                    if (sRevision.equals(sLastRevision)) {
                        sSelected = "selected";
                    } else {
                        sSelected = "";
                    }
                }

                // Find Bug : Dodgy Code : PS :21-March-2017 : START
                sbOptionValues.append("<option ");
                sbOptionValues.append(sSelected);
                sbOptionValues.append(" value='");
                sbOptionValues.append(sbValue);
                sbOptionValues.append("'>");
                sbOptionValues.append(sRevision);
                sbOptionValues.append("</option>");
                // Find Bug : Dodgy Code : PS :21-March-2017 : END

            }

            StringBuffer outPut = new StringBuffer("");
            outPut.append("<select name='CADObjectRevisions' onChange=\"javascript:changeSelectedCADObjectRevision(" + sFieldNumber + ")\">");
            outPut.append(sbOptionValues.toString());
            outPut.append("</select>");

            return outPut;
        } catch (Exception e) {
            logger.error("Error in constructCADObjectRevisionsSelectList()\n", e);
            throw e;
        }
    }

    /**
     * Transform an objet into a string list
     * @param obj
     *            : Object to transform
     * @return StringList : The string list
     * @throws Exception
     */
    public static StringList getStringListObjects(Object obj) throws Exception {
        StringList slObj = new StringList();
        try {
            if (obj != null) {
                String sClassName = (String) obj.getClass().getName();
                if (sClassName != null && sClassName.length() > 0 && "java.lang.String".equals(sClassName)) {
                    slObj.add((String) obj);
                } else {
                    slObj = (StringList) obj;
                }
            }
        } catch (Exception e) {
            logger.error("Error in getStringListObjects()\n", e);
            throw e;
        }
        return slObj;
    }

    /**
     * Return the first Part linked to the current CAD object
     * @param context
     * @param doCADObject
     * @return
     * @throws Exception
     */
    public Map getLinkedPartInfo(Context context, DomainObject doCADObject) throws Exception {
        Map mPart = null;
        try {
            logger.debug("getLinkedPartInfo() - doCADObject = <" + doCADObject + ">");
            StringList slSelectStmts = new SelectList();
            slSelectStmts.addElement(SELECT_ID);
            slSelectStmts.addElement(SELECT_NAME);
            slSelectStmts.addElement(SELECT_TYPE);
            slSelectStmts.addElement(SELECT_REVISION);
            slSelectStmts.addElement(SELECT_DESCRIPTION);
            slSelectStmts.addElement(SELECT_CURRENT);

            StringList slSelectRelationship = new StringList();
            slSelectRelationship.addElement(SELECT_RELATIONSHIP_ID);

            MapList mlLinkedParts = doCADObject.getRelatedObjects(context, RELATIONSHIP_PART_SPECIFICATION, TYPE_PART, slSelectStmts, slSelectRelationship, true, false, (short) 1, "", "", 0);
            logger.debug("getLinkedPartInfo() - mlLinkedParts.size() : < " + mlLinkedParts.size() + " >");
            if (mlLinkedParts.size() > 0) {
                String sLinkedPartType = null;
                for (Iterator iterator = mlLinkedParts.iterator(); iterator.hasNext();) {
                    Map mLinkedPart = (Map) iterator.next();
                    sLinkedPartType = (String) mLinkedPart.get(SELECT_TYPE);
                }
                // No Charted Drawing, retrun the first
                mPart = (Map) mlLinkedParts.get(0);
            }

        } catch (Exception e) {
            logger.error("Error in getLinkedPartInfo()\n", e);
            throw e;
        }
        return mPart;
    }

    /**
     * Return the Parts name column for the table PSS_SynchronizationPartBOMSummary
     * @param context
     *            : the eMatrix <code>Context</code> object
     * @param args
     *            : holds no arguments
     * @return
     * @throws Exception
     */
    public Vector<String> getPartNameLinkBOMToCAD(Context context, String[] args) throws Exception {
        try {
            Vector<String> vPartNames = new Vector<String>();
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");
            String startTag1 = "<a class=\"object\" href=\"javaScript:emxTableColumnLinkClick('../common/emxTree.jsp?objectId=";
            String startTag2 = "&emxSuiteDirectory=engineeringcentral&suiteKey=engineeringcentral&DefaultCategory=ENCSpecifications', '700', '600', 'false', 'popup', '')\" >";
            String endTag = "</a>";

            int index = 0;
            Iterator objectListItr = objectList.iterator();
            while (objectListItr.hasNext()) {
                Map objectMap = (Map) objectListItr.next();
                String sPartId = (String) objectMap.get(SELECT_ID);
                String sPartName = (String) objectMap.get(SELECT_NAME);
                String sIsSynchronize = (String) objectMap.get("IsSynchronize");
                String sFromObjectId = (String) objectMap.get("from.id");

                StringBuffer outPut = new StringBuffer();
                outPut.append(startTag1 + sPartId + startTag2 + sPartName + endTag);
                outPut.append("<input type=\"hidden\" name=\"PartObjectName\" value=\"" + sPartName + "\"/>");
                outPut.append("<input type=\"hidden\" name=\"ParentPartObjectId\" value=\"" + sFromObjectId + "\"/>");
                if ("true".equalsIgnoreCase(sIsSynchronize))

                    outPut.append("&nbsp;<img src=\"../common/images/buttonDialogDone.gif\">");
                else
                    outPut.append("&nbsp;<img src=\"../common/images/buttonDialogCancel.gif\"></img>");

                if (index == 0) {
                    outPut.append("<input type=\"hidden\" id=\"ReconciliationLinksDelete\" name=\"ReconciliationLinksDelete\" value=\"" + "No" + "\"/>");
                    // added for TIGTK-2308
                    outPut.append(
                            "<script>$(document).ready(function() {$(\"input[type='checkbox']\").change(function(){if(this.checked){ if(this.parentNode.parentNode.cells[6].childNodes[0].firstElementChild.value){ }else{alert(\"No CAD Object selected for the Part\");this.checked = false;}}});});</script>");

                }

                vPartNames.add(outPut.toString());
                index++;
            }

            return vPartNames;
        } catch (Exception ex) {
            logger.error("Error in getPartNameLink ", ex);
            throw ex;
        }
    }

    /**
     * Return info to fill the table PSS_SynchronizationPartBOMSummary<br>
     * Return Parts of the EBom which are synchronized with the latest revision of the connected CAD Object
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            Part Id
     * @return
     * @throws Exception
     * @plm.usage table PSS_SynchronizationPartBOMSummary
     */

    public MapList getEBOMSynchronized(Context context, String[] args) throws Exception {
        MapList mlSynchronizedEBOMList = new MapList();
        logger.debug("getEBOMSynchronized() - start ...");
        try {
            MapList ebomList = getEBOM(context, args);

            String sIsSynchronize = "";
            String sRevision = "";
            String sLastRevision = "";
            Map mObject = null;
            int rowIndex = 0;
            for (Iterator iterator = ebomList.iterator(); iterator.hasNext();) {
                mObject = (Map) iterator.next();

                sIsSynchronize = (String) mObject.get("IsSynchronize");
                logger.debug("getEBOMSynchronized() - sIsSynchronize = <" + sIsSynchronize + ">");

                if ("true".equalsIgnoreCase(sIsSynchronize)) {
                    mObject.put(SELECT_RELATIONSHIP_ID, Integer.toString(rowIndex));
                    rowIndex++;
                    mlSynchronizedEBOMList.add(mObject);
                }
            }

        } catch (Exception ex) {
            logger.error("Error in getEBOMSynchronized()\n", ex);
            throw ex;
        }
        logger.debug("getEBOMSynchronized() - end.");

        return mlSynchronizedEBOMList;
    }

    /**
     * Method called on the post process functionality of attaching automatically BOM to CAD. This method also makes a check for "Remove existing Synchronization"
     * @param context
     * @param args
     * @return String
     * @throws Exception
     */
    @com.matrixone.apps.framework.ui.PostProcessCallable
    public String postProcessForBOMToCAD(Context context, String[] args) throws Exception {

        String SUCCESS = "success";
        String FALSE = "failure";
        String sResult = "";
        String finalResult = SUCCESS;
        String sPartObjectId = "";
        boolean bReconciliationLinksDelete = false;
        StringBuffer sbSynhMessage = new StringBuffer();
        try {
            // TIGTK-8886 - PTE - 2017-07-11 - START
            context.setCustomData("PSS_THROW_EXCEPTION", "TRUE");
            // TIGTK-8886 - PTE - 2017-07-11 - END
            HashMap programMap = (HashMap) JPO.unpackArgs(args);

            String[] rowPartObjIds = (String[]) programMap.get("emxTableRowId");
            String[] rowCADObjIds = (String[]) programMap.get("CADObjectId");
            String[] rowCADObjNames = (String[]) programMap.get("CADObjectName");
            String[] rowCADObjectGeometryTypes = (String[]) programMap.get("CADObjectGeometryType");
            String[] rowPartObjNames = (String[]) programMap.get("PartObjectName");
            String[] sParentPartObjectIds = (String[]) programMap.get("ParentPartObjectId");
            String[] rowReconciliationLinksDelete = (String[]) programMap.get("ReconciliationLinksDelete");
            // TIGTK-4478 - 17-03-2017 - VP - START
            MCADIntegrationSessionData integSessionData = (MCADIntegrationSessionData) programMap.get("integSessionData");
            // TIGTK-4478 - 17-03-2017 - VP - END

            Map<String, String> mPartCADDef = new HashMap<String, String>();
            Map<String, String> mCADObjectIds = new HashMap<String, String>();

            String sCbxValue = "";
            String[] sCbxValueSplited;
            String sNumber = "";

            int index;
            String sCADObjectId = "";
            String sCADObjectName = "";
            String sCADObjectRevision = "";
            String sCADObjectGeometryType = "";
            String sCADDefName = "";
            String sCADDefRevision = "00";
            String strSynchronizedPartId = "";
            String sCADDefMajorRevision = "";
            String sCADObjectParent = "";
            String sPartParentId = "";
            String strParentCADId = "";

            if (rowReconciliationLinksDelete != null) {
                String sReconciliationLinksDelete = rowReconciliationLinksDelete[0];

                if ("true".equalsIgnoreCase(sReconciliationLinksDelete)) {
                    bReconciliationLinksDelete = true;
                }
            }

            // Commented below code for TIGTK-3631 - PKH - START
            // Setting Geometry types values first with MG being the last
            HashMap<String, String> mapCADMGGeometryMap = new HashMap<String, String>();
            HashMap<String, String> mapCADNonMGGeometryMap = new HashMap<String, String>();
            StringList slMGRowPartIds = new StringList();
            StringList slNonMGRowPartIds = new StringList();
            for (int i = 0; i < rowPartObjIds.length; i++) {
                // TIGTK-11319 :START
                boolean bCheck = false;
                // TIGTK-11319 :END
                sCbxValue = rowPartObjIds[i];

                sCbxValueSplited = sCbxValue.split("[|]", -1);
                sNumber = sCbxValueSplited[0];

                sPartObjectId = sCbxValueSplited[1];
                index = Integer.parseInt(sNumber);
                sCADObjectId = rowCADObjIds[index];

                sCADObjectGeometryType = rowCADObjectGeometryTypes[index];

                // check that a CAD Object is selected for each Part

                if ("".equals(sCADObjectId) || "null".equals(sCADObjectId)) {
                    sResult += "No CAD Object selected for the Part " + rowPartObjNames[index] + "\n";
                    MqlUtil.mqlCommand(context, "notice $1", sResult);
                    finalResult = FALSE;
                    continue;
                }
                if (mCADObjectIds.keySet().contains(sCADObjectId)) {

                    // check that the same CAD Object is not selected to be
                    // synchronized to two Parts
                    if (!sPartObjectId.equals(mCADObjectIds.get(sCADObjectId))) {

                        sResult += "You can not synchronize two different Parts to the same CAD Object " + rowCADObjNames[index] + "\n";
                        MqlUtil.mqlCommand(context, "notice $1", sResult);
                        finalResult = FALSE;

                        continue;
                    } else {

                        String sValue = sCADDefName + "|" + sCADDefRevision;
                        mPartCADDef.put(sPartObjectId, sValue);
                    }
                }
                // check Geometry type value: "MG" only when CAD Definition is
                // at In Work state
                if ("MG".equals(sCADObjectGeometryType)) {
                    // TIGTK-11319 :START
                    DomainObject dobPart = DomainObject.newInstance(context, sPartObjectId);
                    String sPartState = dobPart.getInfo(context, SELECT_CURRENT);
                    if ((TigerConstants.STATE_PSS_ECPART_PRELIMINARY.equals(sPartState) || TigerConstants.STATE_PSS_DEVELOPMENTPART_CREATE.equals(sPartState))) {
                        bCheck = true;
                    }
                    // TIGTK-11319 :END

                    if (!bCheck) {
                        sResult += "The CAD Object <" + rowCADObjNames[index] + "> Geometry type value is MG. You can reconciliate it only when the Part is In Work\n";
                        MqlUtil.mqlCommand(context, "notice $1", sResult);
                        finalResult = FALSE;
                    }

                }
                // TIGTK-6912 - 23-05-2017 - VP - START
                if ("MG".equalsIgnoreCase(sCADObjectGeometryType)) {
                    mapCADMGGeometryMap.put(sCADObjectId, sCADObjectGeometryType);
                    slMGRowPartIds.addElement(rowPartObjIds[i]);
                } else {
                    mapCADNonMGGeometryMap.put(sCADObjectId, sCADObjectGeometryType);
                    slNonMGRowPartIds.addElement(rowPartObjIds[i]);
                }
                // TIGTK-6912 - 23-05-2017 - VP - END
            }
            // TIGTK-6912 - 23-05-2017 - VP - START
            if (!bReconciliationLinksDelete) {
                LinkedHashMap<String, String> mapCADGeometryMap = new LinkedHashMap<String, String>();
                mapCADGeometryMap.putAll(mapCADNonMGGeometryMap);
                mapCADGeometryMap.putAll(mapCADMGGeometryMap);
                setGeomteryTypeForCAD(context, mapCADGeometryMap);
            }
            // TIGTK-6912 - 23-05-2017 - VP - END
            // Added code for TIGTK-3631 - PKH - END

            // TIGTK-6080 - 31-03-2017 - VP - START
            StringList slProcessedCADIds = new StringList();
            // TIGTK-6912 - 23-05-2017 - VP - START
            StringList slSortedRowPartIDs = new StringList();
            if (bReconciliationLinksDelete) {
                slSortedRowPartIDs.addAll(slNonMGRowPartIds);
                slSortedRowPartIDs.addAll(slMGRowPartIds);
            } else {
                slSortedRowPartIDs.addAll(slMGRowPartIds);
                slSortedRowPartIDs.addAll(slNonMGRowPartIds);
            }

            for (int i = 0; i < slSortedRowPartIDs.size(); i++) {
                sCbxValue = (String) slSortedRowPartIDs.getElement(i);
                // TIGTK-6912 - 23-05-2017 - VP - END

                sCbxValueSplited = sCbxValue.split("[|]", -1);
                sNumber = sCbxValueSplited[0];
                sPartObjectId = sCbxValueSplited[1];

                index = Integer.parseInt(sNumber);

                sCADObjectId = rowCADObjIds[index];

                sCADObjectGeometryType = rowCADObjectGeometryTypes[index];

                // check that a CAD Object is selected for each Part
                // TIGTK-8886 - PTE - 2017-07-7 - START
                try {
                    String strEBOMRelId = sParentPartObjectIds[index];
                    // TIGTK-8886 - PTE - 2017-07-7 - END
                    if (bReconciliationLinksDelete) {

                        // disconnect CAD Object from Part
                        disconnectPartAndCADObject(context, sPartObjectId, sCADObjectId, strEBOMRelId);
                    } else {
                        // Synchronize Part to CAD Object

                        // TIGTK-4478 - 17-03-2017 - VP - START
                        synchronizePartToCADObject(context, sPartObjectId, sCADObjectId, sCADObjectGeometryType, strSynchronizedPartId);
                        // TIGTK-9593 - Use table row index instead of loop index i : START

                        // TIGTK-9593 : END
                        String strCADRelId = null; // it resulted into the Dead store as it is assigned into for loop and used outside the main if.

                        if (UIUtil.isNotNullAndNotEmpty(strEBOMRelId)) {
                            String fromObjId = MqlUtil.mqlCommand(context, "print connection " + strEBOMRelId + " select from.id dump", false, false);
                            DomainObject domSourcePartObj = DomainObject.newInstance(context, fromObjId);

                            StringList lstselectStmts = new StringList(1);
                            lstselectStmts.addElement(DomainConstants.SELECT_ID);

                            StringList lstrelStmts = new StringList();
                            lstrelStmts.add(DomainConstants.SELECT_RELATIONSHIP_NAME);

                            Pattern typePattern = new Pattern(DomainConstants.TYPE_CAD_DRAWING);
                            typePattern.addPattern(DomainConstants.TYPE_CAD_MODEL);

                            Pattern relPattern = new Pattern(RELATIONSHIP_PART_SPECIFICATION);

                            String sClause = "(attribute[" + TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE + "]== \"" + TigerConstants.ATTR_RANGE_PSSGEOMETRYTYPE + "\" )";

                            MapList mlCADObject = domSourcePartObj.getRelatedObjects(context, relPattern.getPattern(), typePattern.getPattern(), lstselectStmts, lstrelStmts, false, true, (short) 1,
                                    sClause, null, 0);
                            // TIGTK-8878 :Modified on 6/7/2017 :Start by SIE
                            if (!mlCADObject.isEmpty()) {
                                strParentCADId = (String) ((Map) mlCADObject.get(0)).get(DomainConstants.SELECT_ID);
                                StringList slCADRelId = getRelationshipIdBetweenObjects(context, strParentCADId, sCADObjectId, TigerConstants.RELATIONSHIP_CADSUBCOMPONENT);
                                if (!slCADRelId.isEmpty()) {
                                    for (int j = 0; j < slCADRelId.size(); j++) {
                                        strCADRelId = (String) slCADRelId.getElement(j);
                                        if (!slProcessedCADIds.contains(strCADRelId)) {
                                            slProcessedCADIds.addElement(strCADRelId);
                                            break;
                                        }
                                    }
                                }
                            }
                            // TIGTK-8878 :Modified on 6/7/2017 :End by SIE
                        }
                        integrateEBOMSynchronizationForCADObject(context, sPartObjectId, sCADObjectId, strEBOMRelId, strCADRelId, integSessionData);

                        // TIGTK-4478 - 17-03-2017 - VP - END
                    }
                    // TIGTK-8886 - PTE - 2017-07-7 - START
                } catch (Exception ex) {
                    Count++;
                    if (!(Count > 10)) {
                        String strMessage = ex.getMessage();
                        strMessage = strMessage.replace("java.lang.Exception: Message:System Error: #5000001: java.lang.Exception:", " ");
                        strMessage = strMessage.replace("Severity:3 ErrorCode:5000001", " ");
                        strMessage = strMessage.replace("java.lang.Exception: Message:", " ");
                        strMessage = strMessage.replace("Severity:2 ErrorCode:1500174", " ");
                        DomainObject doPart = DomainObject.newInstance(context, sPartObjectId);
                        String strPartName = doPart.getInfo(context, DomainConstants.SELECT_NAME);
                        sbInvalidObj.append("Synchronization on ' ");
                        sbInvalidObj.append("Part ");
                        sbInvalidObj.append(strPartName);
                        sbInvalidObj.append(" ' failed : ");
                        sbInvalidObj.append(strMessage);
                        sbInvalidObj.append("\n");
                    }
                }
                // TIGTK-8886 - PTE - 2017-07-7 - END
            }
            // TIGTK-6080 - 31-03-2017 - VP - END

            // TIGTK-8546 - 06-22-2017 - PTE - START
            if (Count != 0) {
                if (Count == rowPartObjIds.length) {
                    sbSynhMessage.append("No EBOM synchronized with ");
                } else {
                    sbSynhMessage.append("EBOM partially synchronized with ");
                }
                sbSynhMessage.append(Count);
                sbSynhMessage.append(" synchronization failed:");
                sbSynhMessage.append("\n");
                sbSynhMessage.append(sbInvalidObj);
                MqlUtil.mqlCommand(context, "notice $1", sbSynhMessage.toString());
                finalResult = "partiallySynchronized";
            }
            // TIGTK-8546 - 06-22-2017 - PTE - END

        } catch (Exception ex) {
            logger.error("Error in postProcessForBOMToCAD: ", ex);
        }
        // TIGTK-8886 - PTE - 2017-07-11 - START
        finally {
            context.setCustomData("PSS_THROW_EXCEPTION", "FALSE");
            context.removeFromCustomData("PSS_THROW_EXCEPTION");
        }
        // TIGTK-8886 - PTE - 2017-07-11 - END

        return finalResult;
    }

    /**
     * Retrieve CAD objects info<br>
     * Called from the JSPs :<br>
     * - PSS_EngineeringCentralCommonProcess.jsp<br>
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            Selected CAD objects ids
     * @return
     * @throws Exception
     */
    public MapList getSelectedCADObjectsInfos(Context context, String[] args) throws Exception {
        MapList mlCADObjectsInfos = null;
        String sCADId = "";

        try {
            HashMap hmParams = (HashMap) JPO.unpackArgs(args);

            String sPartId = (String) hmParams.get("objectId");
            String[] sSelectedIdsFromTable = (String[]) hmParams.get("SelectedIdsFromTable");
            String sFieldNumber = (String) hmParams.get("fieldNumber");

            // int iFieldNumber = Integer.valueOf(sFieldNumber);
            int iFieldNumber = Integer.parseInt(sFieldNumber);

            SelectList slSelect = new SelectList();
            slSelect.addElement(DomainObject.SELECT_ID);
            slSelect.addElement(DomainObject.SELECT_TYPE);
            slSelect.addElement(DomainObject.SELECT_NAME);
            slSelect.addElement(DomainObject.SELECT_REVISION);
            slSelect.addElement(DomainObject.SELECT_CURRENT);
            slSelect.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE + "]");

            mlCADObjectsInfos = DomainObject.getInfo(context, sSelectedIdsFromTable, slSelect);

            Map mCADObjectInfo = null;
            String sCADObjectId = null;
            String sCADObjectType = null;
            String sCADObjectDisplayType = null;
            String sCADObjectName = null;
            String sCADObjectRev = null;
            String sGeometryType = null;
            StringList slConnectedPartsIds = null;
            StringList slConnectedPartsTypes = null;
            StringList slConnectedPartsNames = null;
            StringBuffer sbIsSychronizedMsg = null;

            String sSymetricPartId = null;

            DomainObject doCADObject = DomainObject.newInstance(context);
            for (Iterator iterator = mlCADObjectsInfos.iterator(); iterator.hasNext();) {
                mCADObjectInfo = (Map) iterator.next();
                sCADObjectId = (String) mCADObjectInfo.get(DomainObject.SELECT_ID);
                sCADObjectType = (String) mCADObjectInfo.get(DomainObject.SELECT_TYPE);

                sCADObjectName = (String) mCADObjectInfo.get(DomainObject.SELECT_NAME);
                sCADObjectRev = (String) mCADObjectInfo.get(DomainObject.SELECT_REVISION);
                sGeometryType = (String) mCADObjectInfo.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE + "]");
                doCADObject.setId(sCADObjectId);
                if (doCADObject.isKindOf(context, TigerConstants.TYPE_MCADDRAWING) || doCADObject.isKindOf(context, TigerConstants.TYPE_MCAD_COMPONENT)
                        || doCADObject.isKindOf(context, TigerConstants.TYPE_MCAD_ASSEMBLY)) {

                    // not synchronized yet
                    sFieldNumber = Integer.toString(iFieldNumber);
                    String sResult = getCADObjectRevisionsInfo(context, sCADObjectId, sCADObjectType, sCADObjectName, sCADObjectRev, sFieldNumber, sPartId);
                    mCADObjectInfo.put("Revisions", sResult);

                }
            }

        } catch (Exception ex) {
            logger.error("Error in getSelectedCADObjectsInfos()\n ", ex);
            throw ex;
        }
        return mlCADObjectsInfos;
    }

    /**
     * Retrieve CAD object by name and return a list of revisions<br>
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            CAD object name and the row number in the table
     * @return
     * @throws Exception
     */
    public String getCADObjectRevisionsInfo(Context context, String[] args) throws Exception {
        String outPut = "";

        try {
            HashMap param = (HashMap) JPO.unpackArgs(args);

            String sCADObjectId = (String) param.get("CADObjectId");
            String sCADObjectType = (String) param.get("CADObjectType");
            String sCADObjectName = (String) param.get("CADObjectName");
            String sCADObjectRev = (String) param.get("CADObjectRevision");
            String sFieldNumber = (String) param.get("fieldNumber");
            String sPartId = (String) param.get("objectId");

            outPut = getCADObjectRevisionsInfo(context, sCADObjectId, sCADObjectType, sCADObjectName, sCADObjectRev, sFieldNumber, sPartId);
        } catch (Exception ex) {
            logger.error("Error in getCADObjectRevisionsInfo()\n ", ex);
            throw ex;
        }
        return outPut;
    }

    /**
     * Retrieve CAD object by name and return a list of revisions<br>
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            CAD object name and the row number in the table
     * @return
     * @throws Exception
     */
    private String getCADObjectRevisionsInfo(Context context, String sCADObjectId, String sCADObjectType, String sCADObjectName, String sCADObjectRev, String sFieldNumber, String sPartId)
            throws Exception {
        StringBuffer outPut = new StringBuffer();
        String sCadDefName = null;
        StringList slCadDefRevisions = new StringList();
        String RECONCILIATION_AUTHORIZED_TYPES = TigerConstants.TYPE_MCAD_MODEL + "," + TigerConstants.TYPE_MCADDRAWING;
        MapList mObjMapList = new MapList();
        // case of selected object from a the search window
        if (sCADObjectId != null && !"".equals(sCADObjectId)) {
            MapList mlRevisions = getCADObjectRevisions(context, sCADObjectId);
            // TIGTK-17791 : stembulkar : start
            for (int j = 0; j < mlRevisions.size(); j++) {
                Map mObjMap = (Map) mlRevisions.get(j);
                String sCurrentState = (String) mObjMap.get(SELECT_CURRENT);
                if (!sCurrentState.equals(TigerConstants.STATE_OBSOLETE) && !sCurrentState.equals(TigerConstants.STATE_PSS_CANCELPART_CANCELLED)) {
                    mObjMapList.add(mObjMap);
                }
            }
            outPut = constructCADObjectRevisionsSelectList(context, mObjMapList, sCADObjectRev, sFieldNumber, sCadDefName, slCadDefRevisions);
            // TIGTK-17791 : stembulkar : end
            return outPut.toString();
        }

        // case of selected object by name
        // find types (Assemblies, Models, Drawings)
        String sSearchTypes = RECONCILIATION_AUTHORIZED_TYPES;
        if (sCADObjectType != null) {
            sSearchTypes = sCADObjectType;
        }
        SelectList slSelectStmts = new SelectList();
        slSelectStmts.addElement(SELECT_ID);
        slSelectStmts.addElement(SELECT_TYPE);
        slSelectStmts.addElement("to[" + RELATIONSHIP_PART_SPECIFICATION + "].from.type");

        MapList mlCADObjectRevisions = DomainObject.findObjects(context, sSearchTypes, sCADObjectName, "*", "*", TigerConstants.VAULT_ESERVICEPRODUCTION, null, true, slSelectStmts);

        if (mlCADObjectRevisions.size() > 0) {
            boolean isOnlyOneTypeFound = checkCADObjectsHasTheSameType(mlCADObjectRevisions);
            if (isOnlyOneTypeFound) {
                // check if the CAD Object is already synchronized that it is
                // linked to a Charted Drawing
                boolean bAuthorized = checkIfCADObjectCanBeSynchronized(context, mlCADObjectRevisions, sPartId);
                logger.debug("getCADObjectRevisionsInfo() - bAuthorized = <" + bAuthorized + ">");

                if (bAuthorized) {
                    // get a revision id of the found objects
                    sCADObjectId = (String) ((Map) mlCADObjectRevisions.get(0)).get(SELECT_ID);
                    MapList mlRevisions = getCADObjectRevisions(context, sCADObjectId);
                    // TIGTK-17791 : stembulkar : start
                    for (int j = 0; j < mlRevisions.size(); j++) {
                        Map mObjMap = (Map) mlRevisions.get(j);
                        String sCurrentState = (String) mObjMap.get(SELECT_CURRENT);
                        if (!sCurrentState.equals(TigerConstants.STATE_OBSOLETE) && !sCurrentState.equals(TigerConstants.STATE_PSS_CANCELPART_CANCELLED)) {
                            mObjMapList.add(mObjMap);
                        }
                    }
                    // construct Select list field from found object revisions
                    // list
                    outPut = constructCADObjectRevisionsSelectList(context, mObjMapList, sCADObjectRev, sFieldNumber, sCadDefName, slCadDefRevisions);
                    // TIGTK-17791 : stembulkar : end
                } else {
                    outPut.append("CADOBJECTALREADYSYNCHRONIZED");
                }
            } else {
                outPut.append("NOONLYCADOBJECTTYPEFOUND");
            }
        } else {
            outPut.append("NOCADOBJECTFOUND");
        }
        return outPut.toString();
    }

    /**
     * Check that only revisions of the same CAD Object in the list
     * @param mlCADObjectRevisions
     *            CAD Objects list found
     * @return true/false
     * @throws Exception
     */
    private boolean checkCADObjectsHasTheSameType(MapList mlCADObjectRevisions) throws Exception {
        boolean isOnlyOneTypeFound = true;

        try {
            Map mCADObject = null;
            String sFirstCADObjectType = null;
            for (Iterator iterator = mlCADObjectRevisions.iterator(); iterator.hasNext();) {
                mCADObject = (Map) iterator.next();

                String sCADObjectType = (String) mCADObject.get(SELECT_TYPE);
                logger.debug("checkCADObjectsHasTheSameType() - sCADObjectType = <" + sCADObjectType + ">");
                if (sFirstCADObjectType != null && !sFirstCADObjectType.equals(sCADObjectType)) {
                    isOnlyOneTypeFound = false;
                    break;
                } else if (sFirstCADObjectType == null) {
                    sFirstCADObjectType = sCADObjectType;
                }

            }
        } catch (Exception e) {
            logger.error("Error in checkCADObjectsType()\n", e);
            throw e;
        }
        return isOnlyOneTypeFound;
    }

    /**
     * Check if found CAD object can be synchronized <br>
     * A CAD object can be synchronized if it's not done yet or if it's already synchronized to Charted Drawing
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param mlCADObjectRevisions
     *            CAD objects to be synchronized
     * @param sPartId
     *            Part id to synchronized
     * @return
     * @throws Exception
     */
    private boolean checkIfCADObjectCanBeSynchronized(Context context, MapList mlCADObjectRevisions, String sPartId) throws Exception {
        boolean bCanBeSynchronized = false;

        DomainObject doPart = DomainObject.newInstance(context, sPartId);
        Map mCADObject = null;
        for (Iterator iterator = mlCADObjectRevisions.iterator(); iterator.hasNext();) {
            mCADObject = (Map) iterator.next();

            StringList slLinkedIds = toStringList(mCADObject.get("to[" + RELATIONSHIP_PART_SPECIFICATION + "].from.id"));

            if (slLinkedIds.isEmpty()) {
                bCanBeSynchronized = true;
                break;
            } else {
                SelectList selectList = new SelectList();
                selectList.addId();

                MapList mlSymetricParts = doPart.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART, "*", selectList, null, true, true, (short) 1, "", "", 1);

                StringList slSymetricPartIds = new StringList();
                if (!mlSymetricParts.isEmpty()) {
                    Iterator itSym = mlSymetricParts.iterator();
                    while (itSym.hasNext()) {
                        Map mSym = (Map) itSym.next();
                        logger.debug("checkIfCADObjectCanBeSynchronized() - Symetric part id <" + mSym.get(SELECT_ID) + ">");
                        slSymetricPartIds.addElement((String) mSym.get(SELECT_ID));
                    }
                }

                Iterator itLinkedId = slLinkedIds.iterator();
                while (itLinkedId.hasNext()) {
                    String sConnectedPartId = (String) itLinkedId.next();
                    DomainObject alreadyConnectedPart = DomainObject.newInstance(context, sConnectedPartId);

                }
            }
        }

        return bCanBeSynchronized;
    }

    /**
     * Return the Parts translated states column for the CAD to BOM and BOM to CAD functionalities
     * @param context
     *            : the eMatrix <code>Context</code> object
     * @param args
     *            : holds no arguments
     * @return
     * @throws Exception
     */

    public Vector<String> getTranslatedStateName(Context context, String[] args) throws Exception {
        try {
            String sLanguage = context.getSession().getLanguage();
            String sPartStateTranslated = "";
            Vector<String> vObjectStates = new Vector<String>();
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");

            // Map paramListMap = (HashMap) programMap.get("paramList");
            Map columnMap = (Map) programMap.get("columnMap");
            Map settingsMap = (Map) columnMap.get("settings");

            boolean boolStateFromObject = true;
            String strPolicyKey = (String) settingsMap.get("PSS_Policy_Key");

            Iterator objectListItr = objectList.iterator();

            if (UIUtil.isNotNullAndNotEmpty(strPolicyKey) && !"OBJECT".equals(strPolicyKey)) {
                boolStateFromObject = false;
            }

            if (boolStateFromObject) {

                while (objectListItr.hasNext()) {
                    Map objectMap = (Map) objectListItr.next();
                    String strObjectState = (String) objectMap.get(SELECT_CURRENT);
                    String strObjectPolicy = (String) objectMap.get(SELECT_POLICY);
                    sPartStateTranslated = i18nNow.getStateI18NString(strObjectPolicy, strObjectState, sLanguage);
                    vObjectStates.add(sPartStateTranslated);

                }

            }

            else

            {
                String strStateKey = (String) settingsMap.get("PSS_State_Key");

                while (objectListItr.hasNext()) {
                    Map objectMap = (Map) objectListItr.next();
                    String strObjectState = (String) objectMap.get(strStateKey);
                    String strObjectPolicy = (String) objectMap.get(strPolicyKey);
                    sPartStateTranslated = i18nNow.getStateI18NString(strObjectPolicy, strObjectState, sLanguage);
                    vObjectStates.add(sPartStateTranslated);
                }
            }
            return vObjectStates;

        } catch (Exception ex) {

            logger.error("Error in getPartStateTranslated ", ex);

            throw ex;

        }

    }

    // End of Addition of Code for BOM To CAD Iteration-07

    // TIGTK-4478 - 17-03-2017 - VP - Modified Method signature
    /**
     * Synchronize CAD Object with a Part. Create CAD Definition if no one
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sPartId
     *            Part id
     * @param sCADObjectId
     *            CAD object id
     * @param sCADObjectGeometryType
     *            Geometry type attribute value
     * @param sCADDefId
     *            CAD Definition id
     * @param sCADDefMinorRevision
     *            CAD Definition minor revision
     * @param sCADDefMajorRevision
     *            CAD Definition major revision
     * @throws Exception
     */
    protected int synchronizePartToCADObject(Context context, String sPartId, String sCADObjectId, String sGeometryType, String sSynchronizedPartId) throws Exception {
        int returnStatus = 0;
        boolean bEBOMLinksCreation = true;
        try {

            logger.debug("synchronizePartToCADObject() - Starting ");

            // Start Of Write Transaction
            ContextUtil.startTransaction(context, true);

            boolean bNewMinorRevCADDef = false;

            if (sSynchronizedPartId == null || "".equals(sSynchronizedPartId) || !sSynchronizedPartId.equals(sPartId) || bNewMinorRevCADDef) {
                Map<String, String> mRelAttributes = new HashMap<String, String>();

                // connect Part to CAD Object RELATIONSHIP_PART_SPECIFICATION
                connectPartToCADObject(context, sPartId, sCADObjectId, sGeometryType, mRelAttributes);

            } else {
                logger.debug("synchronizePartToCADObject() - Part:<" + sSynchronizedPartId + "> is already connected to CAD Obj:<" + sCADObjectId + ">");
            }

            // End Transaction
            ContextUtil.commitTransaction(context);

        } catch (Exception ex) {
            // Abort transaction.
            ContextUtil.abortTransaction(context);
            logger.error("Error in synchronizePartToCADObject()\n", ex);
            throw ex;
        }
        return returnStatus;

    }

    /**
     * This method is used to auto populate the description field from the listed attributes of Part Family selected by context user at the time of Part creation
     * @param context
     * @param args
     *            -- material Object ID is passed as an argument
     * @throws Exception
     *             returns -- nothing
     */
    public void populateAutoDescription(Context context, String args[]) throws Exception {
        int intstatus = 0;

        ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, TigerConstants.PERSON_USER_AGENT), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
        try {

            String strLanguage = context.getSession().getLanguage();
            String strPartObjectID = args[0];
            String strEventName = args[1];
            String strCurrentState = args[2];
            DomainObject domPartObject = DomainObject.newInstance(context, strPartObjectID);
            if (UIUtil.isNullOrEmpty(strCurrentState)) {
                strCurrentState = domPartObject.getInfo(context, DomainConstants.SELECT_CURRENT);
            }

            // START :: TIGTK-17756 :: ALM-5978
            boolean bModify = false;
            String strPolicy = args[3];
            if (UIUtil.isNullOrEmpty(strPolicy)) {
                strPolicy = domPartObject.getInfo(context, DomainConstants.SELECT_POLICY);
            }
            String strLogginRole = (String) FrameworkUtil.split(context.getRole().substring(5), ".").get(0);
            if ("Modify".equals(strEventName) && UIUtil.isNotNullAndNotEmpty(strLogginRole)
                    && (TigerConstants.ROLE_PSS_PLM_SUPPORT_TEAM.equals(strLogginRole) || TigerConstants.ROLE_PSS_GLOBAL_ADMINISTRATOR.equals(strLogginRole))
                    && ((TigerConstants.POLICY_PSS_ECPART.equals(strPolicy)
                            && (TigerConstants.STATE_PART_RELEASE.equals(strCurrentState) || TigerConstants.STATE_PSS_ECPART_PRELIMINARY.equals(strCurrentState)))
                            || (TigerConstants.POLICY_PSS_DEVELOPMENTPART.equals(strPolicy)
                                    && (TigerConstants.STATE_DEVELOPMENTPART_COMPLETE.equals(strCurrentState) || TigerConstants.STATE_DEVELOPMENTPART_CREATE.equals(strCurrentState)))
                            || (TigerConstants.POLICY_STANDARDPART.equals(strPolicy) && TigerConstants.STATE_STANDARDPART_RELEASE.equals(strCurrentState)))) {
                bModify = true;
            }
            // END :: TIGTK-17756 :: ALM-5978

            if ((strEventName.endsWith("Modify") && (strCurrentState.equals(TigerConstants.STATE_PSS_ECPART_PRELIMINARY) || strCurrentState.equals(TigerConstants.STATE_DEVELOPMENTPART_CREATE)))
                    || (strEventName.endsWith("Promote") && (strCurrentState.equals(TigerConstants.STATE_PART_REVIEW) || strCurrentState.equals(TigerConstants.STATE_DEVELOPMENTPART_PEERREVIEW)))
                    || bModify) {
                Map attributeMap = domPartObject.getAttributeMap(context, true);
                StringList lstselectStmts = new StringList(1);
                lstselectStmts.addElement(DomainConstants.SELECT_NAME);
                lstselectStmts.addElement(DomainConstants.SELECT_ID);
                lstselectStmts.addElement(DomainConstants.SELECT_ORGANIZATION);
                lstselectStmts.addElement(DomainConstants.SELECT_CURRENT);

                MapList mlPartFamilyMapList = domPartObject.getRelatedObjects(context, DomainConstants.RELATIONSHIP_CLASSIFIED_ITEM, // relationship
                        // pattern
                        DomainConstants.TYPE_PART_FAMILY, // object pattern
                        lstselectStmts, // object selects
                        null, // relationship selects
                        true, // to direction
                        false, // from direction
                        (short) 1, // recursion level
                        null, // object where clause
                        null, 0);
                // START :: TIGTK-18259 :: ALM-6406
                // if (mlPartFamilyMapList.size() > 0) {
                if (mlPartFamilyMapList.size() == 1) {
                    // END :: TIGTK-18259 :: ALM-6406
                    Map<String, String> mapPartFamily = (Map<String, String>) mlPartFamilyMapList.get(0);
                    String strPartFamilyName = (String) mapPartFamily.get(DomainConstants.SELECT_NAME);
                    // PHASE1.1 : TIGTK-9606 : START
                    String strPartFamilyKey = strPartFamilyName.trim().replace(" ", "_");
                    // PHASE1.1 : TIGTK-9606 : END
                    String strautoDescriptionAttributes = "";
                    String strAutoDesFormat = "";
                    String strPartFamilyOrg = (String) mapPartFamily.get(DomainConstants.SELECT_ORGANIZATION);
                    try {
                        strautoDescriptionAttributes = EnoviaResourceBundle.getProperty(context,
                                "emxEngineeringCentral.PSS_AutoDescriptionField.AttributeList." + strPartFamilyOrg + "." + strPartFamilyKey);
                        strAutoDesFormat = EnoviaResourceBundle.getProperty(context,
                                "emxEngineeringCentral.PSS_AutoDescriptionField.AttributeList.format." + strPartFamilyOrg + "." + strPartFamilyKey);
                    } catch (Exception e) {
                        strautoDescriptionAttributes = "";
                        strAutoDesFormat = "";
                    }

                    if (UIUtil.isNotNullAndNotEmpty(strautoDescriptionAttributes) && UIUtil.isNotNullAndNotEmpty(strAutoDesFormat)) {
                        strAutoDesFormat = strAutoDesFormat.replaceFirst("%t", strPartFamilyName);
                        StringList lstautoDesAttributesList = FrameworkUtil.split(strautoDescriptionAttributes, ",");
                        boolean isUpperCase = false; // TIGTK-6781 : Harika Varanasi
                        if ((lstautoDesAttributesList != null)) {
                            for (int intIndex = 0; intIndex < lstautoDesAttributesList.size(); intIndex++) {
                                String strattributes = (String) lstautoDesAttributesList.get(intIndex);
                                if (strattributes.startsWith("attribute_")) {
                                    String strattribOrgName = PropertyUtil.getSchemaProperty(context, strattributes);
                                    String strattribValue = (String) attributeMap.get(strattribOrgName);
                                    // TIGTK_6781 | 06/06/2017 | Harika Varanasi | Starts
                                    if (strattributes.equalsIgnoreCase("attribute_PSS_Additional_Description")) {
                                        domPartObject.setAttributeValue(context, strattribOrgName, strattribValue.toUpperCase());
                                        isUpperCase = true;
                                    }
                                    // TIGTK_6781 | 06/06/2017 | Harika Varanasi | Starts
                                    // TIGTK-8127 :Modified on 31/05/2017 by SIE :Start
                                    String attrNameForDisplay = i18nNow.getRangeI18NString(strattribOrgName, strattribValue, strLanguage);
                                    // TIGTK-9603 : START
                                    if (UIUtil.isNotNullAndNotEmpty(attrNameForDisplay) && !attrNameForDisplay.equalsIgnoreCase(TigerConstants.ATTR_VALUE_UNASSIGNED)
                                            && !strattributes.equalsIgnoreCase("attribute_PSS_Technology_Type")) {
                                        // TIGTK-9603 : END
                                        strAutoDesFormat = strAutoDesFormat.replaceFirst("%s", Matcher.quoteReplacement(attrNameForDisplay));
                                        // TIGTK-8127 :Modified on 31/05/2017 by SIE :End
                                    } // end of if
                                    else if (strattributes.equalsIgnoreCase("attribute_PSS_Technology_Type") && UIUtil.isNotNullAndNotEmpty(strattribValue)) {
                                        String strAttrTechnologyValue = (String) attributeMap.get(TigerConstants.ATTRIBUTE_PSS_TECHNOLOGY_CLASSIFICATION);
                                        String strNewAttrValueTechnology = strAttrTechnologyValue.replace(" ", "_");
                                        String strTechnologyValue = DomainConstants.EMPTY_STRING;
                                        String strNewAttrTechnologyType = strattribValue.replace(" ", "_");
                                        String strTechnologyTypeValue = DomainConstants.EMPTY_STRING;
                                        try {
                                            strTechnologyTypeValue = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentral.PSS_AutoDescriptionField.AttributeList.FIS.PSS_Technology_Type."
                                                    + strNewAttrTechnologyType + "." + strNewAttrValueTechnology);
                                        } catch (Exception ex) {
                                            strTechnologyTypeValue = DomainConstants.EMPTY_STRING;
                                        }

                                        strAutoDesFormat = strAutoDesFormat.replaceFirst("%s", Matcher.quoteReplacement(strTechnologyTypeValue.trim()));
                                    } else {
                                        strAutoDesFormat = strAutoDesFormat.replaceFirst("%s", " ");
                                    }
                                } // end of if

                            } // end of for loop
                              // TIGTK_6781 | 06/06/2017 | Harika Varanasi | Starts
                            if (isUpperCase) {
                                strAutoDesFormat = strAutoDesFormat.toUpperCase();
                            }
                            domPartObject.setDescription(context, strAutoDesFormat);
                            // TIGTK_6781 | 06/06/2017 | Harika Varanasi | Ends
                            // TIGTK-10089: 27-09-2017 : Added below code to update cad object description : START
                            StringBuilder sbWhere = new StringBuilder();
                            sbWhere.append("current == '" + TigerConstants.STATE_INWORK_CAD_OBJECT + "'");
                            // START :: TIGTK-18259 :: ALM-6406
                            if (bModify) {
                                sbWhere.append(" || current == '" + TigerConstants.STATE_RELEASED_CAD_OBJECT + "'");
                            }
                            // END :: TIGTK-18259 :: ALM-6406
                            sbWhere.append(" && attribute[" + TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE + "].value == 'MG'");
                            Pattern pattern = new Pattern(DomainConstants.TYPE_CAD_DRAWING);
                            pattern.addPattern(DomainConstants.TYPE_CAD_MODEL);

                            MapList mlCADObjectList = domPartObject.getRelatedObjects(context, DomainConstants.RELATIONSHIP_PART_SPECIFICATION, // relationship
                                    // pattern
                                    pattern.getPattern(), // object pattern
                                    lstselectStmts, // object selects
                                    null, // relationship selects
                                    false, // to direction
                                    true, // from direction
                                    (short) 1, // recursion level
                                    sbWhere.toString(), // object where clause
                                    null, 0);
                            for (Object obj : mlCADObjectList) {
                                Map map = (Map) obj;
                                String strCADobjectId = (String) map.get(DomainConstants.SELECT_ID);
                                DomainObject domCADObject = DomainObject.newInstance(context, strCADobjectId);
                                domCADObject.setDescription(context, strAutoDesFormat);
                            }
                            // TIGTK-10089: 27-09-2017 : *** : END
                        } // end of if
                    }
                } // end of if for Part family
            } // end if loop
        } // end of try block

        catch (RuntimeException e) {
            logger.error("Error in populateAutoDescription: ", e);
            throw e;
        } catch (Exception e) {
            // TIGTK-5405 - 03-04-2017 - VP - START
            logger.error("Error in populateAutoDescription: ", e);
            // TIGTK-5405 - 03-04-2017 - VP - END
            throw e;
        } // end of catch
        finally {
            ContextUtil.popContext(context);
        } // end of finally block
    }// end of method

    /**
     * This method enables or disables the checkbox in the PSS_SynchronizationPartBOMSummary table.If state is not in work, checkbox will be disabled.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @returns Vector of "true/false" values for each row
     * @throws Exception
     *             if the operation fails
     */
    public Vector showBOMtoCADCheckBox(Context context, String args[]) throws Exception {
        Vector columnVals = null;
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objList = (MapList) programMap.get("objectList");

            columnVals = new Vector(objList.size());
            Iterator i = objList.iterator();
            Map objectMap;
            String sPartObjId = "";

            // TIGTK-8512 - 2017-07-10 - VP - START
            String strCollaborativeSpace = DomainObject.EMPTY_STRING;
            while (i.hasNext()) {
                objectMap = (Map) (i).next();
                sPartObjId = (String) objectMap.get(SELECT_ID);
                strCollaborativeSpace = (String) objectMap.get("project");

                // TIGTK-11319 :START
                String strState = (String) objectMap.get("from.current");
                if (UIUtil.isNullOrEmpty(strState) || (TigerConstants.STATE_PSS_ECPART_PRELIMINARY.equals(strState) || TigerConstants.STATE_PSS_DEVELOPMENTPART_CREATE.equals(strState))) {
                    columnVals.add("true");
                } else {
                    columnVals.add("false");
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 03-04-2017 - VP - START
            logger.error("Error in showBOMtoCADCheckBox: ", e);
            // TIGTK-5405 - 03-04-2017 - VP - END
            throw e;
        }

        return columnVals;
    }

    /**
     * This method is used on the Table Column that displays already applied Product Configuration with marked X.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @returns StringList of X
     * @throws Exception
     *             if the operation fails
     */

    // Method displayAppliedProductConfiguration - Ends
    public StringList displayAppliedProductConfiguration(Context context, String[] args) throws Exception {
        StringList slProductConfigurationresult = new StringList();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap paramMap = (HashMap) programMap.get("paramList");
            String strAppliedProductConfigurationId = (String) paramMap.get("PSS_ProductConfigurationFilter_OID");
            MapList mlObjList = (MapList) programMap.get("objectList");
            for (int i = 0; i < mlObjList.size(); i++) {
                Map mPcObj = (Map) mlObjList.get(i);
                String strPcId = (String) mPcObj.get(DomainConstants.SELECT_ID);
                if (strPcId.equals(strAppliedProductConfigurationId)) {
                    slProductConfigurationresult.add("X");
                } else {
                    slProductConfigurationresult.add("");
                }
            }

        } catch (Exception e) {
            // TIGTK-5405 - 03-04-2017 - VP - START
            logger.error("Error in displayAppliedProductConfiguration: ", e);
            // TIGTK-5405 - 03-04-2017 - VP - END
            throw e;
        }
        return slProductConfigurationresult;
    }

    // Method displayAppliedProductConfiguration - Ends

    /**
     * The JPO retrieve the Part and Applied Product Configuration from args and based on that retrieve the MG type CAD Assembly to Open
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author Vasant PADHIYAR
     * @date 21-06-2016
     */
    public String exportFilteredPartDocuments(Context context, String[] args) throws Exception {
        StringBuilder strResultCADObjects = new StringBuilder();
        String strPartId = args[0];
        String strProductConfigurationId = args[1];
        // TIGTK-7004 - 2017-05-17 - VP - START
        String strRelationshipToExpand = args[2];
        // TIGTK-7004 - 2017-05-17 - VP - END
        if (strPartId == null || strProductConfigurationId == null || strPartId.equals("") || strProductConfigurationId.equals("")) {
            return strResultCADObjects.toString();
        }
        // Create domain object of Product Configuration
        DomainObject domProductConfigObject = DomainObject.newInstance(context, strProductConfigurationId);
        // get attribute name from Symbolic name

        // Effectivity Compiled Form
        String strCompiledForm = domProductConfigObject.getInfo(context, "attribute[" + TigerConstants.ATTRIBUTE_FILTERCOMPILEDFORM + "]");

        // Create domainObject of Parent Part Id
        DomainObject domPartObject = DomainObject.newInstance(context, strPartId);

        // Part Attribute Selection list
        StringList slObjectSelect = new StringList(4);
        slObjectSelect.add(DomainConstants.SELECT_ID);
        slObjectSelect.add(DomainConstants.SELECT_TYPE);
        slObjectSelect.add(DomainConstants.SELECT_NAME);
        slObjectSelect.add(DomainConstants.SELECT_REVISION);
        // TIGTK-10676 - 2017-11-23 - TS - START
        // EBOM Relationship Attribute Selection list
        StringList slRelSelect = new StringList(1);
        slRelSelect.add(DomainRelationship.SELECT_ID);
        slRelSelect.add("attribute[" + TigerConstants.ATTRIBUTE_REFERENCEDESIGNATOR + "]");

        String sClause = "(attribute[" + TigerConstants.ATTRIBUTE_PSS_PROCESS + "] != \"Process\" )";
        // TIGTK-7004 - 2017-05-17 - VP - START
        MapList mlFilterEBOMList = domPartObject.getRelatedObjects(context, PropertyUtil.getSchemaProperty(context, strRelationshipToExpand), // relationshipPattern
                TYPE_PART, // typePattern
                slObjectSelect, // objectSelects
                slRelSelect, // relationshipSelects
                false, // getTo
                true, // getFrom
                (short) 0, // recurseToLevel
                "", // objectWhereClause
                sClause, // relationshipWhereClause
                (short) 0, // limit
                true, // checkHidden
                false, // preventDuplicates
                (short) 0, // pageSize
                null, // includeType
                null, // includeRelationship
                null, // includeMap
                "", // relKeyPrefix
                strCompiledForm, // filterExpression
                (short) 0); // filterFlag
        // TIGTK-7004 - 2017-05-17 - VP - END
        // Add parent Part first to EBOM part list to get parent CAD of the assembly structure
        Map parentPartMap = new HashMap();
        parentPartMap.put(DomainConstants.SELECT_ID, strPartId);
        parentPartMap.put(DomainConstants.SELECT_NAME, domPartObject.getInfo(context, DomainConstants.SELECT_NAME));
        parentPartMap.put("level", "0");
        mlFilterEBOMList.add(0, parentPartMap);

        sClause = "(attribute[" + TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE + "]== \"" + TigerConstants.ATTR_RANGE_PSSGEOMETRYTYPE + "\" )";
        String strRootPart = null;
        StringBuffer sbParentInstancePath = new StringBuffer();
        int iPrevLevel = 0;
        for (int i = 0; i < mlFilterEBOMList.size(); i++) {
            Map mapTemp = (Map) mlFilterEBOMList.get(i);
            strPartId = (String) mapTemp.get(DomainConstants.SELECT_ID);
            String strRefDes = (String) mapTemp.get("attribute[" + TigerConstants.ATTRIBUTE_REFERENCEDESIGNATOR + "]");
            if (strRefDes == null) {
                strRefDes = "";
            }
            String strLevel = (String) mapTemp.get("level");
            int iLevel = Integer.parseInt(strLevel);
            StringBuffer sbInstancePath = new StringBuffer();
            domPartObject = DomainObject.newInstance(context, strPartId);
            // ALM4987:TIGTK-10676 --> Added Type "MCAD Model" to open 3D with MG Geometry type according to Open Partial rules and NOT to take consideration of Drawings
            MapList mlFilteredCADObjects = domPartObject.getRelatedObjects(context, RELATIONSHIP_PART_SPECIFICATION, TigerConstants.TYPE_MCAD_MODEL, slObjectSelect, slRelSelect, false, true,
                    (short) 1, sClause, "", 0);

            for (int j = 0; j < mlFilteredCADObjects.size(); j++) {
                mapTemp = (Map) mlFilteredCADObjects.get(j);
                if (iLevel == 0)
                    strRootPart = (String) mapTemp.get(DomainConstants.SELECT_NAME);
                else
                    sbInstancePath.append("\\" + strRootPart);
                if (iLevel == 1) {
                    sbInstancePath.append("\\" + strRefDes);
                } else if (iLevel > iPrevLevel) {
                    sbInstancePath = sbParentInstancePath;
                    sbInstancePath.append("\\" + strRefDes);
                } else {
                    StringList slPath = FrameworkUtil.split(sbParentInstancePath.toString(), "\\");
                    sbInstancePath = new StringBuffer();
                    for (int nCnt = 0; nCnt < slPath.size(); nCnt++) {
                        if (nCnt < iLevel) {
                            sbInstancePath.append("\\" + slPath.get(nCnt));
                        }
                    }
                    sbInstancePath.append("\\" + strRefDes);
                }

                sbParentInstancePath = sbInstancePath;
                iPrevLevel = iLevel;
                strResultCADObjects.append((String) mapTemp.get(DomainConstants.SELECT_TYPE));
                strResultCADObjects.append(",");
                strResultCADObjects.append((String) mapTemp.get(DomainConstants.SELECT_NAME));
                strResultCADObjects.append(",");
                strResultCADObjects.append((String) mapTemp.get(DomainConstants.SELECT_REVISION));
                strResultCADObjects.append(",");
                strResultCADObjects.append(sbInstancePath.toString());
                strResultCADObjects.append("\n");
            }
            // TIGTK-10676 - 2017-11-23 - TS - END
        }

        return strResultCADObjects.toString();
    }

    /**
     * Populates the default revision value with respect to default Production Policy
     * @param context
     *            Context : User's Context.
     * @param String
     *            [] args
     * @return <code>String</code>
     * @throws Exception
     */
    public String showDefaultProductionRevision(Context context, String[] args) throws Exception {

        /* Addition for Tiger - CAD-BOM stream by SGS starts */
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap requestMap = (HashMap) programMap.get("requestMap");
            String strNextRevision = (String) requestMap.get("PSS_NextRevision");

            if (UIUtil.isNotNullAndNotEmpty(strNextRevision)) {
                return strNextRevision;
            } else {
                String defaultPolicy = EnoviaResourceBundle.getProperty(context, "type_Part.defaultProdPolicy");
                Policy policy = new Policy(defaultPolicy);
                String strPolicySequence = policy.getFirstInSequence(context);
                // return new Policy(defaultPolicy).getFirstInSequence(context);
                return strPolicySequence;
            }
        } catch (Exception e) {
            // TIGTK-5405 - 03-04-2017 - VP - START
            logger.error("Error in showDefaultProductionRevision: ", e);
            // TIGTK-5405 - 03-04-2017 - VP - END
            throw e;
        }
        /* Addition for Tiger - CAD-BOM stream by SGS ends */
    }

    /**
     * This method is executed for 'Range Funtion' to populate 'Revision Filter' with values like As Stored, Latest and Latest Complete.
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param args
     *            contains a packed HashMap
     * @return HashMap.
     * @throws Exception
     *             if the operation fails.
     */
    public HashMap getRevisionFilterOptions(Context context, String args[]) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap requestMap = (HashMap) programMap.get("requestMap");

        String partId = (String) requestMap.get("objectId");
        StringList slDisplayValue = new StringList();
        StringList slActualValue = new StringList();

        try {
            HashMap viewMap = new HashMap();
            DomainObject dmPart = DomainObject.newInstance(context, partId);
            String sPolicy = dmPart.getInfo(context, DomainConstants.SELECT_POLICY);
            // Get the Revision Filter Options from Property
            String strBOMFilterProp = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentral.Filter.RevisionOptions");
            StringList slIterationValue = FrameworkUtil.split(strBOMFilterProp, "|");

            StringItr strItr = new StringItr(slIterationValue);

            while (strItr.next()) {
                String sActualValue = (String) strItr.obj();

                // Display Fields
                if (!"Latest Complete".equals(sActualValue)) {
                    slActualValue.add(sActualValue);
                    String sDisplayValue = sActualValue.replace(' ', '_');
                    // Multitenant
                    sDisplayValue = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(),
                            "emxEngineeringCentral.RevisionFilterOption." + sDisplayValue);
                    slDisplayValue.add(sDisplayValue);
                } else {
                    // Display "Latest Complete" option only when Policy equals
                    // Development Part
                    // Add Custom Policy for Go To Production RFC......
                    if (sPolicy.equals(TigerConstants.POLICY_PSS_DEVELOPMENTPART)) {
                        slActualValue.add(sActualValue);
                        String sDisplayValue = sActualValue.replace(' ', '_');
                        // Multitenant
                        sDisplayValue = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(),
                                "emxEngineeringCentral.RevisionFilterOption." + sDisplayValue);
                        slDisplayValue.add(sDisplayValue);
                    }
                }
            }

            viewMap.put("field_choices", slActualValue);
            viewMap.put("field_display_choices", slDisplayValue);
            return viewMap;
        } catch (Exception ex) {
            throw ex;
        }
    } // end of method: getRevisionFilterOptions

    // Added to fix TIGTK-2244 - Start
    public void connectAffectedItemToCO(Context context, String[] args) throws Exception {
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        HashMap progMap = (HashMap) paramMap.get("paramMap");

        String strChangeId = (String) progMap.get("New OID");

        String strPartId = (String) progMap.get("objectId");
        StringList slAffectedPart = new StringList();
        slAffectedPart.add(strPartId);

        if (UIUtil.isNotNullAndNotEmpty(strChangeId)) {
            try {
                ChangeOrder changeOrder = new ChangeOrder(strChangeId);
                String type = changeOrder.getInfo(context, DomainConstants.SELECT_TYPE);

                if (type.equals(TigerConstants.TYPE_PSS_CHANGEREQUEST)) {
                    DomainRelationship.connect(context, new DomainObject(strChangeId), TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM, new DomainObject(strPartId));
                } else {
                    for (Object object : slAffectedPart) {
                        DomainObject domObj = DomainObject.newInstance(context, strPartId);
                        String policy = domObj.getInfo(context, DomainConstants.SELECT_POLICY);
                        if (policy.equals(TigerConstants.POLICY_PSS_DEVELOPMENTPART)) {
                            throw new Exception("Cannot connect development part to Change Order!");
                        }
                    }
                    // String contextUser = context.getUser();
                    ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                    changeOrder.connectAffectedItems(context, slAffectedPart);
                    ContextUtil.popContext(context);
                }
            } catch (Exception e) {
                // TIGTK-5405 - 03-04-2017 - VP - START
                logger.error("Error in connectAffectedItemToCO: ", e);
                // TIGTK-5405 - 03-04-2017 - VP - END
                throw e;
            }
        }
    }

    // Added to fix TIGTK-2244 - End
    // Added for RFC-071 by PTE -start
    /**
     * Method is called from createJPO when cloning the part from global actions and part properties page. This method checks weather context user has license for creating part, If user has license
     * only then it clones the selected object.
     */

    @com.matrixone.apps.framework.ui.CreateProcessCallable
    public HashMap checkLicenseAndCloneObject(Context context, String[] args) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        Part part = (Part) DomainObject.newInstance(context, DomainConstants.TYPE_PART, DomainConstants.ENGINEERING);
        // Added below lines of code in existing method for RFC-071.
        // Modified for PPDM-ERGO:TIGTK-4500:PK:06/03/2017:Start
        programMap.put("AutoNameSeries", DomainConstants.EMPTY_STRING);
        // Modified for PPDM-ERGO:TIGTK-4500:PK:06/03/2017:Start
        programMap.put("autoNameCheck", "true");
        programMap.put("Name", "");
        // END
        String clonedObjectId = part.clonePart(context, programMap);
        HashMap returnMap = new HashMap(1);
        returnMap.put(DomainConstants.SELECT_ID, clonedObjectId);
        return returnMap;
    }

    // Added for RFC-071 -End

    /**
     * @description: This method is used to send Notifications to the users.
     * @param context
     * @param args
     * @return Map
     * @throws Exception
     */
    // Added for E2E Stream - Starts
    public static void sendJavaMail(Context context, String[] args) throws Exception {
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        StringList toList = (StringList) paramMap.get("toList");
        StringList ccList = (StringList) paramMap.get("ccList");
        StringList bccList = (StringList) paramMap.get("bccList");
        StringList replyTo = (StringList) paramMap.get("replyTo");
        String subject = (String) paramMap.get("subject");
        String messageText = (String) paramMap.get("messageText");
        String messageHTML = (String) paramMap.get("messageHTML");

        emxNotificationUtil_mxJPO.sendJavaMail(context, toList, ccList, bccList, subject, messageText, messageHTML, context.getUser(), replyTo, new StringList(0), "both");
        if (toList == null) {
            toList = new StringList();
        }
        if (ccList == null) {
            ccList = new StringList();
        }
        MqlUtil.mqlCommand(context, "notice $1", "Notification Sent \n" + toList.toString() + "\n" + ccList.toString());

    }

    // Added for E2E Stream - Ends

    // Added for TIGTK-2878(CAD-BOM) Stream -Start
    /**
     * @description: This method is used to Connect Clone CAD to Clone Part.
     * @param context
     * @param args
     * @return Map
     * @throws Exception
     */

    public String cloneAndConnectVersionedCADObjects(Context context, String strSourceCADObjectId, String strClonedCADObjectId) throws Exception {
        String strSuccess = "";
        try {
            // call method to Connect Version Object to clone CAD.
            copyVersionObjects(context, strSourceCADObjectId, strClonedCADObjectId);
            // call method to Connect Viewable Object to clone CAD.
            copyThumbnailObjects(context, strSourceCADObjectId, strClonedCADObjectId);
            copyDerivedOutputObjects(context, strSourceCADObjectId, strClonedCADObjectId);
        } catch (Exception e) {
            // TIGTK-5405 - 03-04-2017 - VP - START
            logger.error("Error in cloneAndConnectVersionedCADObjects: ", e);
            // TIGTK-5405 - 03-04-2017 - VP - END
        }
        return strSuccess;
    }

    /**
     * @description: This method is used to Connect Version Object to clone CAD.
     * @param context
     * @param args
     * @return Map
     * @throws Exception
     */

    public String copyVersionObjects(Context context, String strSourceCADObjectId, String strClonedCADObjectId) throws Exception {
        String strResultSuccess = "";
        String strVersionObjectId = "";
        String strVersionObjectTYPE = "";
        DomainObject domCADSourceObj = DomainObject.newInstance(context, strSourceCADObjectId);
        DomainObject domClonedCADObj = DomainObject.newInstance(context, strClonedCADObjectId);
        String strClonedCADObjectName = domClonedCADObj.getInfo(context, DomainConstants.SELECT_NAME);
        StringList selectStmts = new StringList(1);
        selectStmts.addElement(DomainConstants.SELECT_ID);
        selectStmts.addElement(DomainConstants.SELECT_TYPE);
        StringList relStmts = new StringList(1);
        relStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

        Pattern relPattern = new Pattern(CommonDocument.RELATIONSHIP_ACTIVE_VERSION);
        MapList mlobjMapList = domCADSourceObj.getRelatedObjects(context, relPattern.getPattern(), // relationship pattern
                "*", // object pattern
                selectStmts, // object selects
                relStmts, // relationship selects
                false, // to direction
                true, // from direction
                (short) 1, // recursion level
                null, // object where clause
                null, 0); // relationship where clause
        if (mlobjMapList != null) {
            for (int j = 0; j < mlobjMapList.size(); j++) {
                Map mapVersionObject = (Map) mlobjMapList.get(j);
                strVersionObjectId = (String) mapVersionObject.get(DomainConstants.SELECT_ID);
                DomainObject domObj = DomainObject.newInstance(context, strVersionObjectId);
                DomainObject domVersionedClonedCADObj = new DomainObject(domObj.cloneObject(context, strClonedCADObjectName, "-A.0", context.getVault().getName(), true, true));
                DomainRelationship.connect(context, domClonedCADObj, CommonDocument.RELATIONSHIP_ACTIVE_VERSION, domVersionedClonedCADObj);
                DomainRelationship.connect(context, domClonedCADObj, CommonDocument.RELATIONSHIP_LATEST_VERSION, domVersionedClonedCADObj);
                DomainRelationship.connect(context, domVersionedClonedCADObj, TigerConstants.RELATIONSHIP_VERSIONOF, domClonedCADObj);
            }
        }
        return strResultSuccess;
    }

    /**
     * @description: This method is used to Connect Viewable Object to clone CAD.
     * @param context
     * @param args
     * @return Map
     * @throws Exception
     */
    public String copyThumbnailObjects(Context context, String strSourceCADObjectId, String strClonedCADObjectId) throws Exception {
        final String RELATIONSHIP_VIEWABLE = PropertyUtil.getSchemaProperty(context, "relationship_Viewable");
        String strSuccess = "";
        try {
            String strVersionObjectId = "";
            String strVersionObjectTYPE = "";
            String varNameAttr = MCADMxUtil.getActualNameForAEFData(context, "attribute_CADObjectName");
            DomainObject domCADSourceObj = DomainObject.newInstance(context, strSourceCADObjectId);
            DomainObject domClonedCADObj = DomainObject.newInstance(context, strClonedCADObjectId);
            String strClonedCADObjectName = domClonedCADObj.getInfo(context, DomainConstants.SELECT_NAME);
            StringList selectStmts = new StringList(1);
            selectStmts.addElement(DomainConstants.SELECT_ID);
            selectStmts.addElement(DomainConstants.SELECT_TYPE);
            StringList relStmts = new StringList(1);
            relStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            String strConnectedVersionObject = domClonedCADObj.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_LATESTVERSION + "].to.id");
            DomainObject domConnectedVersionObj = DomainObject.newInstance(context, strConnectedVersionObject);
            String strConnectedThumbnailObjectName = domCADSourceObj.getInfo(context, "from[" + RELATIONSHIP_VIEWABLE + "].to.name");
            String strSuffix = strConnectedThumbnailObjectName.substring(strConnectedThumbnailObjectName.lastIndexOf("."));

            Pattern relPattern = new Pattern(RELATIONSHIP_VIEWABLE);
            MapList mlobjMapList = domCADSourceObj.getRelatedObjects(context, relPattern.getPattern(), // relationship pattern
                    "*", // object pattern
                    selectStmts, // object selects
                    relStmts, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 1, // recursion level
                    null, // object where clause
                    null, 0); // relationship where clause
            if (mlobjMapList != null) {
                for (int j = 0; j < mlobjMapList.size(); j++) {
                    Map mapVersionObject = (Map) mlobjMapList.get(j);
                    strVersionObjectId = (String) mapVersionObject.get(DomainConstants.SELECT_ID);
                    DomainObject domObj = DomainObject.newInstance(context, strVersionObjectId);
                    DomainObject domVersionedClonedCADObj = new DomainObject(domObj.cloneObject(context, strClonedCADObjectName + strSuffix, "-A.0", context.getVault().getName(), true, true));
                    DomainRelationship domVersionRelID = DomainRelationship.connect(context, domClonedCADObj, RELATIONSHIP_VIEWABLE, domVersionedClonedCADObj);
                    DomainRelationship domRelID = DomainRelationship.connect(context, domConnectedVersionObj, RELATIONSHIP_VIEWABLE, domVersionedClonedCADObj);
                    domVersionRelID.setAttributeValue(context, varNameAttr, strClonedCADObjectName);
                    domRelID.setAttributeValue(context, varNameAttr, strClonedCADObjectName);
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            // TIGTK-5405 - 03-04-2017 - VP - START
            logger.error("Error in copyThumbnailObjects: ", e);
            // TIGTK-5405 - 03-04-2017 - VP - END
        }
        return strSuccess;
    }

    /**
     * @description: This method is used to Connect Viewable Object to clone CAD.
     * @param context
     * @param args
     * @return Map
     * @throws Exception
     */
    public String copyDerivedOutputObjects(Context context, String strSourceCADObjectId, String strClonedCADObjectId) throws Exception {

        String strSuccess = "";
        try {
            String strVersionObjectId = "";
            String strVersionObjectTYPE = "";
            String varNameAttr = MCADMxUtil.getActualNameForAEFData(context, "attribute_CADObjectName");
            DomainObject domCADSourceObj = DomainObject.newInstance(context, strSourceCADObjectId);
            DomainObject domClonedCADObj = DomainObject.newInstance(context, strClonedCADObjectId);
            String strClonedCADObjectName = domClonedCADObj.getInfo(context, DomainConstants.SELECT_NAME);
            StringList selectStmts = new StringList(1);
            selectStmts.addElement(DomainConstants.SELECT_ID);
            selectStmts.addElement(DomainConstants.SELECT_TYPE);
            StringList relStmts = new StringList(1);
            relStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            String strConnectedVersionObject = domClonedCADObj.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_LATESTVERSION + "].to.id");
            String strConnectedDerevideOutputObjectName = domCADSourceObj.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_DERIVEDOUTPUT + "].to.name");
            String strSuffix = strConnectedDerevideOutputObjectName.substring(strConnectedDerevideOutputObjectName.lastIndexOf("."));

            DomainObject domConnectedVersionObj = DomainObject.newInstance(context, strConnectedVersionObject);

            Pattern relPattern = new Pattern(TigerConstants.RELATIONSHIP_DERIVEDOUTPUT);
            MapList mlobjMapList = domCADSourceObj.getRelatedObjects(context, relPattern.getPattern(), // relationship pattern
                    "*", // object pattern
                    selectStmts, // object selects
                    relStmts, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 1, // recursion level
                    null, // object where clause
                    null, 0); // relationship where clause
            if (mlobjMapList != null) {
                for (int j = 0; j < mlobjMapList.size(); j++) {
                    Map mapVersionObject = (Map) mlobjMapList.get(j);
                    strVersionObjectId = (String) mapVersionObject.get(DomainConstants.SELECT_ID);
                    DomainObject domObj = DomainObject.newInstance(context, strVersionObjectId);
                    DomainObject domVersionedClonedCADObj = new DomainObject(domObj.cloneObject(context, strClonedCADObjectName + strSuffix, "-A.0", context.getVault().getName(), true, true));
                    DomainRelationship domVersionRelID = DomainRelationship.connect(context, domClonedCADObj, TigerConstants.RELATIONSHIP_DERIVEDOUTPUT, domVersionedClonedCADObj);
                    DomainRelationship domRelID = DomainRelationship.connect(context, domConnectedVersionObj, TigerConstants.RELATIONSHIP_DERIVEDOUTPUT, domVersionedClonedCADObj);
                    domVersionRelID.setAttributeValue(context, varNameAttr, strClonedCADObjectName);
                    domRelID.setAttributeValue(context, varNameAttr, strClonedCADObjectName);
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            // TIGTK-5405 - 03-04-2017 - VP - START
            logger.error("Error in copyDerivedOutputObjects: ", e);
            // TIGTK-5405 - 03-04-2017 - VP - END
        }
        return strSuccess;
    }

    // Added for TIGTK-2878(CAD-BOM) Stream -End
    // Added for TIGTK-3416 (Diversity) Stream -Start
    /**
     * @description: This method is used to Connect Revision of Part to Product(s) connected to Original Part.
     * @param context
     * @param args
     * @return void
     * @throws Exception
     */

    public void connectNewRevisionToProduct(Context context, String[] args) throws Exception {
        String strObjectId = args[0]; // Original Part ID
        String strRevId = args[1]; // Part revision ID

        try {
            DomainObject domPartObj = DomainObject.newInstance(context, strObjectId);

            StringList slSelectStmts = new StringList(1); // object selects
            slSelectStmts.addElement(DomainConstants.SELECT_ID);

            StringList slRelSelects = new StringList(1); // relationship selects
            slRelSelects.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

            // Get Products connected to current Part
            MapList mlConnectedProductsList = domPartObj.getRelatedObjects(context, RELATIONSHIP_GBOM, // relationship
                    TigerConstants.TYPE_PRODUCTS, // types to fetch from other end
                    true, // getTO
                    false, // getFrom
                    1, // recursionTo level
                    slSelectStmts, // object selects
                    slRelSelects, // relationship selects
                    DomainConstants.EMPTY_STRING, // object where
                    DomainConstants.EMPTY_STRING, // relationship where
                    0, // limit
                    DomainConstants.EMPTY_STRING, // post rel pattern
                    DomainConstants.EMPTY_STRING, // post type pattern
                    null); // post patterns

            if (mlConnectedProductsList.size() > 0) {
                for (int j = 0; j < mlConnectedProductsList.size(); j++) {
                    Map mTempMap = (Map) mlConnectedProductsList.get(j);
                    String strConnectedProdId = (String) mTempMap.get(DomainConstants.SELECT_ID);

                    // Connect Part revision to Product(s) connected to the Part
                    DomainRelationship.connect(context, strConnectedProdId, RELATIONSHIP_GBOM, strRevId, false);
                }
            }

        } catch (Exception e) {
            // TIGTK-5405 - 03-04-2017 - VP - START
            logger.error("Error in connectNewRevisionToProduct: ", e);
            // TIGTK-5405 - 03-04-2017 - VP - END
            throw e;
        }

    }

    // Ad for TIGTK-3416(Diversity) Stream -End
    // Add for TIGTK-3654(CAD-BOM) Stream -Start
    public void reviseAndConnectSymmetricalPart(Context context, String[] args) throws Exception {
        String strObjectId = args[0]; // Original Part ID
        String strRevId = args[1]; // Original Part revision ID

        try {
            DomainObject domOriginalPartObj = DomainObject.newInstance(context, strObjectId);
            String strdevpolicy = domOriginalPartObj.getInfo(context, DomainConstants.SELECT_POLICY);
            if (strdevpolicy.equalsIgnoreCase(TigerConstants.POLICY_PSS_DEVELOPMENTPART)) {
                StringList selectStmts = new StringList(2);
                selectStmts.addElement(DomainConstants.SELECT_ID);
                selectStmts.addElement(DomainConstants.SELECT_CURRENT);

                StringList relStmts = new StringList(1);
                relStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

                boolean isSymmetricalPartsFrom = false;

                MapList mlobjMapList = domOriginalPartObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART, // relationship pattern
                        DomainConstants.TYPE_PART, // object pattern
                        selectStmts, // object selects
                        relStmts, // relationship selects
                        false, // to direction
                        true, // from direction
                        (short) 1, // recursion level
                        null, // object where clause
                        null, // relationship where clause
                        0);

                if (mlobjMapList.size() > 0) {
                    isSymmetricalPartsFrom = true;
                } else {

                    mlobjMapList = domOriginalPartObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART, // relationship pattern
                            DomainConstants.TYPE_PART, // object pattern
                            selectStmts, // object selects
                            relStmts, // relationship selects
                            true, // to direction
                            false, // from direction
                            (short) 1, // recursion level
                            null, // object where clause
                            null, // relationship where clause
                            0);

                    if (mlobjMapList.size() > 0) {
                        isSymmetricalPartsFrom = false;
                    }
                }

                for (int j = 0; j < mlobjMapList.size(); j++) {
                    Map mTempMap = (Map) mlobjMapList.get(j);
                    String strConnectedSymmetricalCurrentState = (String) mTempMap.get(DomainConstants.SELECT_CURRENT);
                    String strConnectedSymmetricalId = (String) mTempMap.get(DomainConstants.SELECT_ID);
                    DomainObject domSymmetricalPartObj = DomainObject.newInstance(context, strConnectedSymmetricalId);

                    if (domSymmetricalPartObj.isLastRevision(context)) {

                        String strRevisedObjectId = DomainConstants.EMPTY_STRING;
                        String strReviseStatus = PropertyUtil.getGlobalRPEValue(context, "PSS_REVISE_FROM_GOTOPROD" + strObjectId);
                        String strIsGoToProduction = context.getCustomData("BOM_GO_TO_PRODUCTION");
                        if (UIUtil.isNotNullAndNotEmpty(strReviseStatus) && strReviseStatus.equals("REVISE_FROM_GOTOPROD")
                                || (UIUtil.isNotNullAndNotEmpty(strIsGoToProduction) && strIsGoToProduction.equals("TRUE"))) {
                            String strRevision = domSymmetricalPartObj.getNextSequence(context);
                            String strVault = domSymmetricalPartObj.getInfo(context, DomainConstants.SELECT_VAULT);
                            Part part = new Part();
                            part.setId(strConnectedSymmetricalId);
                            strRevisedObjectId = part.triggerPolicyDevelopmentPartStateReviewPromoteAction(context, TigerConstants.POLICY_PSS_ECPART, strRevision, strVault);
                        } else {
                            BusinessObject busRevisedObject = domSymmetricalPartObj.reviseObject(context, true);
                            strRevisedObjectId = busRevisedObject.getObjectId(context);
                        }

                        if (isSymmetricalPartsFrom) {
                            DomainRelationship.connect(context, strRevId, TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART, strRevisedObjectId, false);
                        } else {
                            DomainRelationship.connect(context, strRevisedObjectId, TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART, strRevId, false);
                        }
                    }
                } // end of for loop
            }

        } catch (RuntimeException e) {
            logger.error("Error in reviseAndConnectSymmetricalPart: ", e);
            throw e;
        } catch (Exception e) {
            logger.error("Error in reviseAndConnectSymmetricalPart: ", e);
            throw e;
        }

    }

    // Add for TIGTK-3654(CAD-BOM) Stream -End.

    /**
     * This method stores the Geometry type for CAD for each entry in the mapCADGeometryMap
     * @param context
     * @param mapCADGeometryMap
     * @throws Exception
     */
    public void setGeomteryTypeForCAD(Context context, LinkedHashMap<String, String> mapCADGeometryMap) throws Exception {
        java.util.Set set = mapCADGeometryMap.entrySet();
        Iterator iter = set.iterator();

        while (iter.hasNext()) {
            String strCADName = DomainConstants.EMPTY_STRING;
            try {
                Map.Entry<String, String> valueEntryMap = (Map.Entry<String, String>) iter.next();
                String strCADObjectId = valueEntryMap.getKey();
                String strGeomteryTypeValue = valueEntryMap.getValue();
                DomainObject dobCADObj = DomainObject.newInstance(context, strCADObjectId);
                strCADName = dobCADObj.getInfo(context, DomainConstants.SELECT_NAME);
                dobCADObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE, strGeomteryTypeValue);
            } catch (Exception ex) {
                Count++;
                if (!(Count > 10)) {
                    String strMessage = ex.getMessage();
                    strMessage = strMessage.replace("java.lang.Exception: Message:System Error: #5000001: java.lang.Exception:", " ");
                    strMessage = strMessage.replace("Severity:3 ErrorCode:5000001", " ");
                    sbInvalidObj.append("Modification of Geometry Type for '");
                    sbInvalidObj.append("CAD Object ");
                    sbInvalidObj.append(strCADName);
                    sbInvalidObj.append(" ' failed : ");
                    sbInvalidObj.append(strMessage);
                    sbInvalidObj.append("\n");
                }
            }
        }
    }

    // Add for TIGTK-2986(CAD-BOM) Stream -Start.
    /**
     * This method Invert symmetrical Part link.
     * @param context
     * @param
     * @throws Exception
     */
    public String invertSymmetricalPartLink(Context context, String[] args) throws Exception {
        String strSymObjId = "";
        String strRelId = null;
        String strResult = "";
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);

            String strOrgObjId = (String) programMap.get("objectId");
            String[] strcheckBoxId = (String[]) programMap.get("emxTableRowId");
            for (int i = 0; i < strcheckBoxId.length; i++) {
                StringTokenizer strTokenizer = new StringTokenizer(strcheckBoxId[i], "|");
                strRelId = strTokenizer.nextToken();
                strSymObjId = strTokenizer.nextToken();

            }
            DomainObject domParentObj = new DomainObject(strOrgObjId);
            DomainObject domChildObj = new DomainObject(strSymObjId);
            String strSobjId = domParentObj.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART + "].from.id");
            String strOobjId = domParentObj.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART + "].to.id");
            DomainRelationship domSymRel = new DomainRelationship(strRelId);
            Map mapOriginalSymAttributes = domSymRel.getAttributeMap(context);
            // code for disconnecting
            DomainRelationship.disconnect(context, strRelId);

            DomainObject domNewParentObject = new DomainObject();
            DomainObject domNewChildObject = new DomainObject();
            if (strSobjId != null) {
                domNewParentObject = new DomainObject(domParentObj);
                domNewChildObject = new DomainObject(domChildObj);

            } else if (strOobjId != null) {
                domNewParentObject = new DomainObject(domChildObj);
                domNewChildObject = new DomainObject(domParentObj);
            }
            // code for connecting
            DomainRelationship domSymmetricalRel = DomainRelationship.connect(context, domNewParentObject, TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART, domNewChildObject);
            domSymmetricalRel.setAttributeValues(context, mapOriginalSymAttributes);
            String strNewOriginalPartName = domNewParentObject.getInfo(context, DomainConstants.SELECT_NAME);
            String strNewSymmetricPartName = domNewChildObject.getInfo(context, DomainConstants.SELECT_NAME);
            String strInvertedAlertMessage = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(),
                    "emxEngineeringCentral.Alert.PSS_InvertSymmetricLink");
            strInvertedAlertMessage = strInvertedAlertMessage.replace("${ORIGINAL_PART_NAME}", strNewOriginalPartName);
            strInvertedAlertMessage = strInvertedAlertMessage.replace("${SYMMETRIC_PART_NAME}", strNewSymmetricPartName);
            strResult = strInvertedAlertMessage;
        } catch (Exception e) {
            // TIGTK-5405 - 03-04-2017 - VP - START
            logger.error("Error in invertSymmetricalPartLink: ", e);
            // TIGTK-5405 - 03-04-2017 - VP - END
        }
        return strResult;
    }

    /**
     * This method Clone the Material.
     * @param context
     * @param
     * @throws Exception
     */
    public String cloneMaterial(Context context, String[] args) throws Exception {
        String strClonedObjectId = "";
        boolean isContextPushed = false;
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);

            String strOriginalMaterialId = (String) programMap.get("objectId");
            DomainObject domSourceMaterialObj = DomainObject.newInstance(context, strOriginalMaterialId);
            String strObjectGeneratorName = FrameworkUtil.getAliasForAdmin(context, DomainConstants.SELECT_TYPE, TigerConstants.TYPE_PROCESSCONTINUOUSPROVIDE, true);
            String strAutoName = DomainObject.getAutoGeneratedName(context, strObjectGeneratorName, "");
            // TIGTK-15533:28-06-2018:START
            DomainObject domClonedMaterialObj = new DomainObject(domSourceMaterialObj.cloneObject(context, strAutoName, "01.1", "vplm", true));
            // TIGTK-15533:28-06-2018:END
            domClonedMaterialObj.setAttributeValue(context, "PLMEntity.PLM_ExternalID", strAutoName);
            domClonedMaterialObj.setAttributeValue(context, "PLMEntity.V_Name", strAutoName);
            StringList lstMaterialRequestRelIds = domClonedMaterialObj.getInfoList(context, "from[" + DomainConstants.RELATIONSHIP_REFERENCE_DOCUMENT + "].id");
            String[] strRelIds = (String[]) lstMaterialRequestRelIds.toArray(new String[lstMaterialRequestRelIds.size()]);
            // 17-07-2018:Starts
            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, TigerConstants.PERSON_USER_AGENT), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            isContextPushed = true;
            DomainRelationship.disconnect(context, strRelIds);
            ContextUtil.popContext(context);
            isContextPushed = false;
            // 17-07-2018:ENDS
            strClonedObjectId = domClonedMaterialObj.getId(context);

            // Connecting the Original and Cloned Material Object with "Derived" Relationship
            DomainRelationship domainRelationship = DomainRelationship.connect(context, domSourceMaterialObj, RELATIONSHIP_DERIVED, domClonedMaterialObj);
            // PCM: TIGTK-8083 : 29/05/2017: PTE: START
            domainRelationship.setAttributeValue(context, TigerConstants.ATTRIBUTE_DERIVED_CONTEXT, "Clone");
            // PCM: TIGTK-8083 : 29/05/2017: PTE: END
        } catch (Exception e) {
            // TIGTK-5405 - 03-04-2017 - VP - START
            logger.error("Error in cloneMaterial: ", e);
            // TIGTK-5405 - 03-04-2017 - VP - END
            // 17-07-2018:Starts
        } finally {
            if (isContextPushed) {
                ContextUtil.popContext(context);
            }
        }
        // 17-07-2018:ENDS
        return strClonedObjectId;

    }
    // Add for TIGTK-2986(CAD-BOM) Stream -End.

    // Added for Ergonomic Development : PPDM : START
    /**
     * This method is for display the CO Purpose of the release
     * @param context
     * @param string
     *            args
     * @throws Exception
     * @author Priyanka Salunke
     * @since 01-March-2017
     */

    public Vector getCOPurposeOfReleaseFromPartForTableView(Context context, String[] args) throws Exception {
        Vector vReturnVector = new Vector();
        try {
            HashMap<?, ?> mpProgramMap = (HashMap) JPO.unpackArgs(args);
            MapList mlObjectList = (MapList) mpProgramMap.get("objectList");
            HashMap<?, ?> mpColumnMap = (HashMap<?, ?>) mpProgramMap.get("columnMap");
            Map mpSettingsMap = (Map) mpColumnMap.get("settings");
            String strPSSExpressionValue = (String) mpSettingsMap.get("PSS_Expression");
            if (mlObjectList != null && !mlObjectList.isEmpty()) {
                Iterator itrObjectList = mlObjectList.iterator();
                while (itrObjectList.hasNext()) {
                    Map mpObjectIdMap = (Map) itrObjectList.next();
                    String strCurrentPartId = (String) mpObjectIdMap.get(DomainConstants.SELECT_ID);
                    // TIGTK-13012 : 25-01-2018 : START
                    DomainObject domPartObject = DomainObject.newInstance(context, strCurrentPartId);
                    String strPartPolicy = domPartObject.getInfo(context, DomainConstants.SELECT_POLICY);
                    String strCODetailsFromPart = DomainConstants.EMPTY_STRING;
                    if (strPartPolicy.equalsIgnoreCase(TigerConstants.POLICY_PSS_DEVELOPMENTPART)) {
                        strCODetailsFromPart = domPartObject.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PURPOSEOFRELEASE_DEVPART);
                    } else {
                        strCODetailsFromPart = getCOForReleaseInfoFromPart(context, strCurrentPartId, strPSSExpressionValue, false, true);
                    }
                    // TIGTK-13012 : 25-01-2018 : END
                    vReturnVector.add(strCODetailsFromPart);
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 03-04-2017 - VP - START
            logger.error("Error in getCOPurposeOfReleaseFromPartForTableView: ", e);
            // TIGTK-5405 - 03-04-2017 - VP - END
        }
        return vReturnVector;
    }// End of Method getCOPurposeOfReleaseFromPartForTableView()

    /**
     * This method is for getting the CO details from Part
     * @param context
     * @param string
     *            args
     * @throws Exception
     * @author Priyanka Salunke
     * @since 02-March-2017
     */

    public String getCOForReleaseInfoFromPart(Context context, String objectId, String expression, Boolean boolTreeLink, Boolean boolIsTable) throws Exception {
        StringBuffer sbReturnBuffer = new StringBuffer();
        try {
            if (UIUtil.isNotNullAndNotEmpty(objectId)) {
                DomainObject domPartObject = DomainObject.newInstance(context, objectId);
                // Object Selects
                StringList slObjectSelect = new StringList();
                slObjectSelect.add(DomainConstants.SELECT_ID);
                slObjectSelect.add(DomainConstants.SELECT_CURRENT);
                slObjectSelect.add(DomainConstants.SELECT_NAME);
                // Relationship Selects
                StringList slRelSelect = new StringList();
                slRelSelect.add(DomainConstants.SELECT_RELATIONSHIP_ID);
                slRelSelect.add(ChangeConstants.SELECT_ATTRIBUTE_REQUESTED_CHANGE);
                // Object Where clause
                String strObjectWhere = "current != " + TigerConstants.STATE_PSS_CANCELPART_CANCELLED;
                // Relationship Where clause
                String strRelWhere = "attribute[" + ChangeConstants.ATTRIBUTE_REQUESTED_CHANGE + "] == '" + ChangeConstants.FOR_RELEASE + "'";
                // Relationship Pattern
                // Pattern relationshipPattern = new Pattern(ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM);
                // TIGTK-9617| 06/09/17 : Start
                Pattern relationshipPattern = new Pattern(ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM);
                relationshipPattern.addPattern(ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM);
                // TIGTK-9617| 06/09/17 : End
                // Type Pattern
                Pattern typePattern = new Pattern(TigerConstants.TYPE_CHANGEACTION);
                // Get connected CAs of Part
                MapList mlConnectedCASOfPartList = domPartObject.getRelatedObjects(context, // context
                        relationshipPattern.getPattern(), // relationship
                        // pattern
                        typePattern.getPattern(), // object pattern
                        slObjectSelect, // object selects
                        slRelSelect, // relationship selects
                        true, // to direction
                        false, // from direction
                        (short) 0, // recursion level
                        strObjectWhere, // object where clause
                        strRelWhere, // relationship where clause
                        (short) 0, false, // checkHidden
                        true, // preventDuplicates
                        (short) 1000, // pageSize
                        null, null, null, null);
                if (mlConnectedCASOfPartList != null && !mlConnectedCASOfPartList.isEmpty()) {
                    Iterator itrConnectedCASOfPartList = mlConnectedCASOfPartList.iterator();
                    while (itrConnectedCASOfPartList.hasNext()) {
                        Map mpCAMap = (Map) itrConnectedCASOfPartList.next();
                        String strCAObjectId = (String) mpCAMap.get(DomainConstants.SELECT_ID);
                        if (UIUtil.isNotNullAndNotEmpty(strCAObjectId)) {
                            DomainObject domCAObject = DomainObject.newInstance(context, strCAObjectId);
                            // Object Selects
                            StringList slObjectSel = new StringList();
                            slObjectSel.add(DomainConstants.SELECT_ID);
                            slObjectSel.add(DomainConstants.SELECT_NAME);
                            slObjectSel.add(expression);
                            // Relationship Selects
                            StringList slRelSel = new StringList();
                            slRelSel.add(DomainConstants.SELECT_RELATIONSHIP_ID);
                            MapList mlConnectedCOofCAList = domCAObject.getRelatedObjects(context, // context
                                    TigerConstants.RELATIONSHIP_CHANGEACTION, // relationship
                                    TigerConstants.TYPE_PSS_CHANGEORDER, // type
                                    slObjectSel, // object selects
                                    slRelSel, // relationship selects
                                    true, // to direction
                                    false, // from direction
                                    (short) 0, // recursion level
                                    DomainConstants.EMPTY_STRING, // object where clause
                                    DomainConstants.EMPTY_STRING, // relationship where clause
                                    0);
                            if (mlConnectedCOofCAList != null && !mlConnectedCOofCAList.isEmpty()) {
                                Iterator itrConnectedCOofCAList = mlConnectedCOofCAList.iterator();
                                while (itrConnectedCOofCAList.hasNext()) {
                                    Map mpCOMap = (Map) itrConnectedCOofCAList.next();
                                    String strExpression = (String) mpCOMap.get(expression);
                                    if (!boolTreeLink) {
                                        sbReturnBuffer.append(strExpression);
                                    } // if end
                                    else {
                                        // TIGTK-9162 - PTE - 2017-7-24 - START
                                        String strRelatedCOObjID = (String) mpCOMap.get(DomainConstants.SELECT_ID);
                                        if (boolIsTable) {
                                            sbReturnBuffer.append("<a class=\"object\" onClick='JavaScript:emxTableColumnLinkClick(\"../common/emxTree.jsp?objectId=");
                                            sbReturnBuffer.append(XSSUtil.encodeForHTMLAttribute(context, strRelatedCOObjID));
                                            sbReturnBuffer.append("\")'>");

                                        } else {
                                            sbReturnBuffer.append("&#160;&#160;<a href=\"JavaScript:emxFormLinkClick('../common/emxTree.jsp?objectId=");
                                            sbReturnBuffer.append(XSSUtil.encodeForHTMLAttribute(context, strRelatedCOObjID));
                                            sbReturnBuffer.append("','content', '', '', '')\">");
                                        }
                                        // TIGTK-9162 - PTE - 2017-7-24 - END
                                        sbReturnBuffer.append(strExpression);
                                        sbReturnBuffer.append("</a>");
                                    }
                                } // Co while en
                            } // CO if end

                        } // CA if end
                    } // CA while end
                } else {
                    sbReturnBuffer.append(EMPTY_STRING);
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            // TIGTK-5405 - 03-04-2017 - VP - START
            logger.error("Error in getCOForReleaseInfoFromPart: ", e);
            // TIGTK-5405 - 03-04-2017 - VP - END
        }
        return sbReturnBuffer.toString();
    }// End of Method getCOForReleaseInfoFromPart()

    /**
     * This method is for getting the related CO from Part
     * @param context
     * @param string
     *            args
     * @throws Exception
     * @author Priyanka Salunke
     * @since 02-March-2017
     */

    public Vector getRelatedCOFromPart(Context context, String[] args) throws Exception {
        Vector vReturnVector = new Vector();
        try {
            HashMap<?, ?> mpProgramMap = (HashMap) JPO.unpackArgs(args);
            MapList mlObjectList = (MapList) mpProgramMap.get("objectList");
            HashMap<?, ?> mpColumnMap = (HashMap<?, ?>) mpProgramMap.get("columnMap");
            Map mpSettingsMap = (Map) mpColumnMap.get("settings");
            String strPSSExpressionValue = (String) mpSettingsMap.get("PSS_Expression");
            if (mlObjectList != null && !mlObjectList.isEmpty()) {
                Iterator itrObjectList = mlObjectList.iterator();
                while (itrObjectList.hasNext()) {
                    Map mpObjectIdMap = (Map) itrObjectList.next();
                    String strCurrentPartId = (String) mpObjectIdMap.get(DomainConstants.SELECT_ID);
                    String strRelatedCOFromPart = getCOForReleaseInfoFromPart(context, strCurrentPartId, strPSSExpressionValue, true, true);
                    vReturnVector.add(strRelatedCOFromPart);
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 03-04-2017 - VP - START
            logger.error("Error in getRelatedCOFromPart: ", e);
            // TIGTK-5405 - 03-04-2017 - VP - END
        }
        return vReturnVector;
    }// End of Method getRelatedCOFromPart()

    /**
     * This method is for showing the Related Objects Count with Hyperlink
     * @param context
     * @param string
     *            args
     * @throws Exception
     * @author Priyanka Salunke
     * @since 03-March-2017
     */

    public Vector showRelatedObjectsCountAndHyperlink(Context context, String[] args) throws Exception {
        Vector vReturnVector = new Vector();
        try {
            HashMap<?, ?> mpProgramMap = (HashMap) JPO.unpackArgs(args);
            MapList mlObjectList = (MapList) mpProgramMap.get("objectList");
            HashMap<?, ?> mpColumnMap = (HashMap<?, ?>) mpProgramMap.get("columnMap");
            Map mpSettingsMap = (Map) mpColumnMap.get("settings");
            String actualCommandName = (String) mpSettingsMap.get("PSS_CommandName");
            String relToTraverse = PropertyUtil.getSchemaProperty(context, (String) mpSettingsMap.get("PSS_Relationship"));
            String direction = (String) mpSettingsMap.get("PSS_Direction");
            String typeToTraverse = (String) mpSettingsMap.get("PSS_Type");
            StringList slTypeToTraverse = FrameworkUtil.split(typeToTraverse, ",");
            int iTypeToTraveseSize = slTypeToTraverse.size();
            Pattern typePattern = null;
            for (int i = 0; i < iTypeToTraveseSize; i++) {
                String strTypeToTraverse = (String) slTypeToTraverse.get(i);
                String strType = PropertyUtil.getSchemaProperty(context, strTypeToTraverse);
                // type Pattern
                if (i == 0) {
                    typePattern = new Pattern(strType);
                } else {
                    typePattern.addPattern(strType);
                }
            }
            boolean getFrom;
            boolean getTo;
            if (direction.equalsIgnoreCase("from")) {
                getFrom = true;
                getTo = false;
            } else {
                getFrom = false;
                getTo = true;
            }

            if (mlObjectList != null && !mlObjectList.isEmpty()) {
                Iterator itrObjectList = mlObjectList.iterator();
                while (itrObjectList.hasNext()) {
                    Map mpObjectIdMap = (Map) itrObjectList.next();
                    String strObjectId = (String) mpObjectIdMap.get(DomainConstants.SELECT_ID);
                    if (UIUtil.isNotNullAndNotEmpty(strObjectId)) {
                        DomainObject domObject = DomainObject.newInstance(context, strObjectId);
                        // Object Selects
                        StringList slObjectSelect = new StringList();
                        slObjectSelect.add(DomainConstants.SELECT_ID);
                        slObjectSelect.add(DomainConstants.SELECT_NAME);
                        // Relationship Selects
                        StringList slRelSelect = new StringList();
                        slRelSelect.add(DomainConstants.SELECT_RELATIONSHIP_ID);
                        // Relationship Pattern
                        Pattern relationshipPattern = new Pattern(relToTraverse);
                        // Get Related Objects of Part
                        MapList mlRelatedObjectsList = domObject.getRelatedObjects(context, // context
                                relationshipPattern.getPattern(), // relationship
                                // pattern
                                typePattern.getPattern(), // object pattern
                                slObjectSelect, // object selects
                                slRelSelect, // relationship selects
                                getTo, // to direction
                                getFrom, // from direction
                                (short) 0, // recursion level
                                null, // object where clause
                                null, // relationship where clause
                                (short) 0, false, // checkHidden
                                true, // preventDuplicates
                                (short) 1000, // pageSize
                                null, null, null, null);

                        if (mlRelatedObjectsList != null && !mlRelatedObjectsList.isEmpty()) {
                            int intSize = mlRelatedObjectsList.size();
                            HashMap componentMap = (HashMap) UIMenu.getCommand(context, actualCommandName);
                            String componentHRef = UIComponent.getHRef(componentMap);
                            String sRegisteredSuite = UIComponent.getSetting(componentMap, "Registered Suite");
                            String menuRegisteredDir = UINavigatorUtil.getRegisteredDirectory(context, sRegisteredSuite);
                            StringList slHref = FrameworkUtil.split(componentHRef, "/");
                            StringBuffer sbFinalHref = new StringBuffer("../common/");
                            sbFinalHref.append((String) slHref.get(1));
                            StringBuffer sbBuffer = new StringBuffer();
                            sbBuffer.append("<a class=\"object\" onClick='JavaScript:emxTableColumnLinkClick(\"");
                            sbBuffer.append(sbFinalHref.toString().replace("&", "&amp;"));
                            sbBuffer.append("&amp;objectId=");
                            sbBuffer.append(XSSUtil.encodeForHTMLAttribute(context, strObjectId));
                            sbBuffer.append("&amp;emxSuiteDirectory=");
                            sbBuffer.append(menuRegisteredDir);
                            sbBuffer.append("&amp;suiteKey=");
                            sbBuffer.append(sRegisteredSuite);
                            sbBuffer.append("\")'>");
                            sbBuffer.append(intSize);
                            sbBuffer.append("</a>");
                            vReturnVector.add(sbBuffer.toString());
                        } else {
                            vReturnVector.add(EMPTY_STRING);
                        }
                    }
                }
            }

        } catch (Exception e) {
            // TIGTK-5405 - 03-04-2017 - VP - START
            logger.error("Error in showRelatedObjectsCountAndHyperlink: ", e);
            // TIGTK-5405 - 03-04-2017 - VP - END
        }
        return vReturnVector;
    }// End of Method showRelatedObjectsCountAndHyperlink()
     // Added for Ergonomic Development : PPDM : END
     // Add for TIGTK-2986(CAD-BOM) Stream -End.
     // ADD for PPDM-ERGO:TIGTK-4500:PK:Start

    /**
     * This method is for show value of Purpose of Released on part Property page.
     * @param context
     * @param string
     *            args
     * @throws Exception
     * @author PK
     * @since 06-March-2017
     */
    public String getCOPurposeOfReleaseFromPart(Context context, String[] args) throws Exception {
        String strResult = "";
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap requestMap = (HashMap) programMap.get("requestMap");
            String strObjectd = (String) requestMap.get("objectId");
            String expression = "attribute[PSS_Purpose_Of_Release]";
            strResult = getCOForReleaseInfoFromPart(context, strObjectd, expression, false, true);
        } catch (Exception e) {
            // TIGTK-5405 - 03-04-2017 - VP - START
            logger.error("Error in getCOPurposeOfReleaseFromPart: ", e);
            // TIGTK-5405 - 03-04-2017 - VP - END
        }
        return strResult;
    }
    // ADD for PPDM-ERGO:TIGTK-4500:PK:End

    /**
     * Generic method to the change Policy of Part Object This method code is completely replace with new code as per ALM-4106
     * @param context
     * @param args
     * @throws Exception
     */
    public void changePolicy(Context context, String[] args) throws Exception {
        boolean bSuperUser = false;
        try {
            String strLanguage = context.getSession().getLanguage();
            HashMap mpProgramMap = (HashMap) JPO.unpackArgs(args);
            String strObjId = (String) mpProgramMap.get("objectId");
            if (UIUtil.isNotNullAndNotEmpty(strObjId)) {
                String strChangeTo = (String) mpProgramMap.get("changeTo");
                DomainObject doPart = DomainObject.newInstance(context, strObjId);
                StringList slObjSelects = new StringList(2);
                slObjSelects.add(DomainConstants.SELECT_POLICY);
                slObjSelects.add("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT + "].id");
                Map mpPartInfo = doPart.getInfo(context, slObjSelects);
                if (mpPartInfo != null && !mpPartInfo.isEmpty()) {
                    String strPolicy = (String) mpPartInfo.get(DomainConstants.SELECT_POLICY);
                    String strGPRelID = (String) mpPartInfo.get("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT + "].id");
                    if (UIUtil.isNotNullAndNotEmpty(strPolicy)) {
                        TigerUtils.pushContextToSuperUser(context);
                        bSuperUser = true;
                    }
                    if ("Development".equalsIgnoreCase(strChangeTo)) {
                        doPart.setPolicy(context, TigerConstants.POLICY_PSS_DEVELOPMENTPART);
                        if (UIUtil.isNotNullAndNotEmpty(strGPRelID)) {
                            DomainRelationship.disconnect(context, strGPRelID);
                        }
                    } else if ("EC".equalsIgnoreCase(strChangeTo)) {
                        doPart.setPolicy(context, TigerConstants.POLICY_PSS_ECPART);
                        pss.uls.ULSUIUtil_mxJPO ulsUtil = new pss.uls.ULSUIUtil_mxJPO();
                        ContextUtil.startTransaction(context, false);
                        String strPPOID = ulsUtil.getLatestMaturedProgramProjectFromObjectCS(context, doPart);
                        ContextUtil.commitTransaction(context);
                        if (UIUtil.isNotNullAndNotEmpty(strPPOID)) {
                            DomainRelationship.connect(context, DomainObject.newInstance(context, strPPOID), TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT, doPart);
                        }
                    } else if ("Standard".equals(strChangeTo)) {
                        doPart.setPolicy(context, TigerConstants.POLICY_STANDARDPART);
                        if (UIUtil.isNotNullAndNotEmpty(strGPRelID)) {
                            DomainRelationship.disconnect(context, strGPRelID);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            emxContextUtil_mxJPO.mqlNotice(context, e.getMessage());
            throw new Exception(e.getLocalizedMessage(), e);
        } finally {
            if (bSuperUser) {
                ContextUtil.popContext(context);
            }
        }
    }

    // TIGTK-4478 - 17-03-2017 - VP - START
    /**
     * Method to Perform the EBOM Synchronization for single Part and CAD object
     * @param context
     * @param strPartObjectId
     * @param strCADObjectId
     * @param strEBOMRelId
     * @param strCADRelId
     * @param integSessionData
     * @return
     * @throws Exception
     * @author Vasant PADHIYAR : SteepGraph
     */
    public void integrateEBOMSynchronizationForCADObject(Context context, String strPartObjectId, String strCADObjectId, String strEBOMRelId, String strCADRelId,
            MCADIntegrationSessionData integSessionData) throws Exception {
        try {
            String language = (String) integSessionData.getLanguageName();
            MCADMxUtil mxUtil = new MCADMxUtil(context, new MCADServerResourceBundle(language), new IEFGlobalCache());

            // Create BusinessObject of the CAD Object
            BusinessObject cadObject = new BusinessObject(strCADObjectId);

            cadObject.open(context);
            String cadType = mxUtil.getCADTypeForBO(context, cadObject);
            String busType = cadObject.getTypeName();
            cadObject.close(context);

            // get Integration Name - CATIA, NX or ..
            String integrationName = mxUtil.getIntegrationName(context, strCADObjectId);
            // get the GCO of the CAD tool from Integration Session Data using Integration Name
            MCADGlobalConfigObject globalConfigObject = integSessionData.getGlobalConfigObject(integrationName, context);

            MCADServerGeneralUtil serverGeneralUtil = new MCADServerGeneralUtil(context, integSessionData, integrationName);

            // check whether CAD object is like Drawing
            boolean isDrawingLike = serverGeneralUtil.isDrawingLike(context, busType);
            // check whether CAD object is Valid CAD
            boolean isValidObject = serverGeneralUtil.isValidObjectForEBOM(context, cadObject, cadType, globalConfigObject, isDrawingLike);
            String busGeometryType = (String) cadObject.getAttributeValues(context, TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE).getValue();

            // check whether the current CAD is MG and Valid CAD to be Synchronized. Also it can be synchronized only if its not like family object
            if (busGeometryType.equals(TigerConstants.ATTR_RANGE_PSSGEOMETRYTYPE) && !globalConfigObject.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_FAMILY_LIKE) && isValidObject
                    && !isDrawingLike) {

                // Create an argument array for the CAD to be Synchronized
                String sourceName = serverGeneralUtil.getCSENameForBusObject(context, cadObject);

                String confObjTNR = globalConfigObject.getEBOMRegistryTNR();
                StringTokenizer token = new StringTokenizer(confObjTNR, "|");

                String confObjType = (String) token.nextElement();
                String confObjName = (String) token.nextElement();
                String confObjRev = (String) token.nextElement();

                IEFEBOMConfigObject ebomConfObject = new IEFEBOMConfigObject(context, confObjType, confObjName, confObjRev);

                if (UIUtil.isNotNullAndNotEmpty(strCADRelId) && UIUtil.isNotNullAndNotEmpty(strEBOMRelId)) {
                    IEFEBOMSyncConnectPartsBase_mxJPO iefEBOMSyncConnectPart = new IEFEBOMSyncConnectPartsBase_mxJPO();
                    iefEBOMSyncConnectPart.ebomConfObject = ebomConfObject;
                    iefEBOMSyncConnectPart.mxUtil = mxUtil;
                    iefEBOMSyncConnectPart.init(context);
                    iefEBOMSyncConnectPart.updateEBOMAttribute(context, new Relationship(strEBOMRelId), strCADRelId, sourceName, ebomConfObject, null, "true");
                }

                String[] jpoArgs = new String[4];
                jpoArgs[0] = confObjType;
                jpoArgs[1] = confObjName;
                jpoArgs[2] = confObjRev;
                jpoArgs[3] = language;

                IEFEBOMSyncFindMatchingPartBase_mxJPO ebomFMJPO = new IEFEBOMSyncFindMatchingPart_mxJPO(context, jpoArgs);

                String[] argsTransAttr = new String[5];
                argsTransAttr[0] = strCADObjectId;
                argsTransAttr[1] = strPartObjectId;
                argsTransAttr[2] = ""; // TODO:instanceName is passed blank

                // Find Bug : Dodgy Code : PS :21-March-2017
                String[] packedGCO = JPO.packArgs(globalConfigObject);
                argsTransAttr[3] = packedGCO[0];
                argsTransAttr[4] = packedGCO[1];

                ebomFMJPO.transferCadAttribsToPart(context, argsTransAttr);

            }
        } catch (Exception e) {
            // TIGTK-5405 - 03-04-2017 - VP - START
            logger.error("Error in integrateEBOMSynchronizationForCADObject: ", e);
            throw e;
            // TIGTK-5405 - 03-04-2017 - VP - END
        }
    }
    // TIGTK-4478 - 17-03-2017 - VP - END

    // TIGTK-5855 - 30-03-2017 - VP - START

    /**
     * lookupEntries method checks the object entered manually is exists or not in the database This method is invoked on clicking on Lookup button in EBOM
     * @param args
     *            String array having the object Id(s) of the part.
     * @throws FrameworkException
     *             if creation of the Part Master object fails.
     */
    @com.matrixone.apps.framework.ui.LookUpCallable
    public MapList lookupEntries(Context context, String[] args) throws Exception {

        HashMap inputMap = (HashMap) JPO.unpackArgs(args);
        HashMap requestMap = (HashMap) inputMap.get("requestMap");
        HashMap curObjectMap;
        HashMap itemMap;

        MapList objectMapList = (MapList) inputMap.get("objectList");
        MapList returnList = new MapList();
        MapList resultsList;

        String languageStr = (String) requestMap.get("languageStr");
        String multipleMessage = EngineeringUtil.i18nStringNow(context, "emxEngineeringCentral.MultipleError.Message", languageStr);
        String noMessage = EngineeringUtil.i18nStringNow(context, "emxEngineeringCentral.NoObject.Message", languageStr);
        String latestReleased = EngineeringUtil.i18nStringNow(context, "emxEngineeringCentral.Part.WhereUsedRevisionLatestReleased", languageStr);
        String latest = EngineeringUtil.i18nStringNow(context, "emxEngineeringCentral.EBOMManualAddExisting.RevisionOption.Latest", languageStr);

        // Added for IR-153213
        String LATEST_RELEASED = EngineeringUtil.i18nStringNow(context, "emxEngineeringCentral.Part.WhereUsedRevisionLatestReleased", "en");
        String LATEST = EngineeringUtil.i18nStringNow(context, "emxEngineeringCentral.EBOMManualAddExisting.RevisionOption.Latest", "en");

        String strLatestRelesed;
        String fromConfigBOM = "false";

        // 2012x
        boolean isECCInstalled = FrameworkUtil.isSuiteRegistered(context, "appVersionEngineeringConfigurationCentral", false, null, null);
        fromConfigBOM = (String) requestMap.get("fromConfigBOM");
        // 2012x

        StringBuffer sbObjectWhere;

        StringList objectSelect = createStringList(new String[] { DomainConstants.SELECT_ID, DomainConstants.SELECT_CURRENT });

        Iterator objectItr = objectMapList.iterator();
        String sparePart = PropertyUtil.getSchemaProperty(context, "attribute_SparePart");

        while (objectItr.hasNext()) {
            curObjectMap = (HashMap) objectItr.next();

            String objectName = getValue(curObjectMap, "Name");
            String objectType = getValue(curObjectMap, "Type");
            String objectRev = getValue(curObjectMap, "Revision");
            String Policy = getValue(curObjectMap, "Policy");

            strLatestRelesed = objectRev;

            sbObjectWhere = new StringBuffer(128);
            sbObjectWhere.append("(policy == '");
            sbObjectWhere.append(TigerConstants.POLICY_PSS_ECPART);
            // TIGTK-16357 : 01-08-2018 : START
            sbObjectWhere.append("' || policy == '");
            sbObjectWhere.append(TigerConstants.POLICY_STANDARDPART);
            // TIGTK-16357 : 01-08-2018 : END
            sbObjectWhere.append("' || policy == '");
            String policy_Configured = "";
            if (isECCInstalled && "true".equalsIgnoreCase(fromConfigBOM)) {
                policy_Configured = PropertyUtil.getSchemaProperty(context, "policy_ConfiguredPart");
                sbObjectWhere.append(policy_Configured);
                sbObjectWhere.append("' || policy == '");
            }
            sbObjectWhere.append(TigerConstants.POLICY_PSS_DEVELOPMENTPART);
            sbObjectWhere.append("')");

            if (objectRev.equals(latestReleased) || objectRev.equalsIgnoreCase(LATEST_RELEASED)) { // Modified for IR-153213
                // Modified for IR-048513V6R2012x, IR-118107V6R2012x
                sbObjectWhere.append(" && ((current == '" + DomainConstants.STATE_PART_RELEASE + "' && (revision == 'last' || (next.current != '" + DomainConstants.STATE_PART_RELEASE
                        + "' && next.current != '" + DomainConstants.STATE_PART_OBSOLETE + "'))) || (revision == 'last' && current != '" + DomainConstants.STATE_PART_OBSOLETE + "'))");
            } else if (objectRev.equals(latest) || objectRev.equalsIgnoreCase(LATEST)) { // Modified for IR-153213
                // Modified for IR-048513V6R2012x, IR-118107V6R2012x
                sbObjectWhere.append(" && (revision == 'last' && current != '" + DomainConstants.STATE_PART_OBSOLETE + "')");
            }

            itemMap = new HashMap();

            if ("".equals(objectRev) || objectRev.equals(latestReleased) || objectRev.equals(latest) || objectRev.equalsIgnoreCase(LATEST_RELEASED) || objectRev.equalsIgnoreCase(LATEST)) { // Modified
                // for
                // IR-153213
                objectRev = DomainConstants.QUERY_WILDCARD;
            }

            if (isECCInstalled && "true".equalsIgnoreCase(fromConfigBOM) && "policy_ConfiguredPart".equalsIgnoreCase(Policy)) {
                String STATESUPERSEDED = PropertyUtil.getSchemaProperty(context, "policy", policy_Configured, "state_Superseded");
                sbObjectWhere.append(" && (revision == 'last' && current !=" + STATESUPERSEDED + ")");
                objectRev = DomainConstants.QUERY_WILDCARD;
            }
            sbObjectWhere.append("&&(attribute[" + sparePart + "] == No)");
            // TIGTK-6090 - 31-03-2017 - VP - START
            ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            resultsList = DomainObject.findObjects(context, // eMatrix context
                    objectType, // type pattern
                    objectName, // name pattern
                    objectRev, // revision pattern
                    DomainConstants.QUERY_WILDCARD, // owner pattern
                    TigerConstants.VAULT_ESERVICEPRODUCTION, // vault pattern
                    sbObjectWhere.toString(), // where expression
                    true, // Expand Type
                    objectSelect); // object selects
            ContextUtil.popContext(context);
            // TIGTK-6090 - 31-03-2017 - VP - END
            if (strLatestRelesed.equals(latestReleased) || strLatestRelesed.equalsIgnoreCase(LATEST_RELEASED)) { // Modified for IR-153213
                // If we have 2 revisions and latest Part is not released then
                // we will get 2 objects from the above query. we have to select
                // the object which is Released so that it will be latest Released.

                // if (resultsList != null && resultsList.size() > 1) { //Modified for IR-150912
                if (resultsList != null && resultsList.size() > 0) {
                    resultsList = getReleasedList(resultsList);
                }
            }

            if (resultsList != null && resultsList.size() == 1) {
                itemMap.put("id", ((Map) resultsList.get(0)).get(DomainConstants.SELECT_ID));
            } else if (resultsList != null && resultsList.size() > 0) {
                itemMap.put("Error", multipleMessage);
            } else {
                itemMap.put("Error", noMessage);
            }

            returnList.add(itemMap);
        }

        return returnList;
    }
    // TIGTK-5855 - 30-03-2017 - VP - END

    // TIGTK-5855 - 30-03-2017 - VP - START
    /**
     * Iterates through MapList and gets the Map whose current state is Released.
     * @param list
     *            contains datas retrived from the database.
     * @return MapList which contains only one map with released part information.
     */
    private MapList getReleasedList(MapList list) {
        Map mapTemp = null;
        MapList listReturn = new MapList(); // Modified for IR-150912
        String strCurrent;
        for (int i = 0, size = list.size(); i < size; i++) {
            mapTemp = (Map) list.get(i);
            strCurrent = getStringValue(mapTemp, DomainObject.SELECT_CURRENT);
            if ("Release".equals(strCurrent)) {
                listReturn.add(mapTemp); // Modified for IR-150912
                // break;
            }
        }
        return listReturn;
    }
    // TIGTK-5855 - 30-03-2017 - VP - END

    // TIGTK-5855 - 30-03-2017 - VP - START
    /**
     * @param dataMap
     *            contains data like type, name, revision,...
     * @param key
     *            String which contains the key.
     * @return value from the map of that particular key.
     */
    private String getValue(HashMap dataMap, String key) {
        String value = getStringValue(dataMap, key);
        return (value == null) ? "" : value.trim();
    }
    // TIGTK-5855 - 30-03-2017 - VP - END

    // TIGTK-6941:Rutuja Ekatpure:3/5/2017:start
    /***
     * this method called from trigger written on EBOM relationship create Check,this method checks Instantiation of EC/Dev part under Standard part
     * @param context
     * @param args
     * @return int :0 if check trigger passes,1 if check trigger fail
     * @throws Exception
     */
    public int checkInstantiationOfECOrDEVPartOnSTDPart(Context context, String[] args) throws Exception {

        int intReturn = 0;
        try {
            String strFromObjectId = args[0];
            String strToObjectId = args[1];
            // from side domain object
            DomainObject domFromPart = new DomainObject(strFromObjectId);
            String strFromPartPolicy = domFromPart.getInfo(context, DomainConstants.SELECT_POLICY);
            if (TigerConstants.POLICY_STANDARDPART.equalsIgnoreCase(strFromPartPolicy)) {
                // to side domain object
                DomainObject domToPart = new DomainObject(strToObjectId);
                String strToPartPolicy = domToPart.getInfo(context, DomainConstants.SELECT_POLICY);
                if (TigerConstants.POLICY_PSS_DEVELOPMENTPART.equalsIgnoreCase(strToPartPolicy) || TigerConstants.POLICY_PSS_ECPART.equalsIgnoreCase(strToPartPolicy)) {
                    String strLanguage = context.getSession().getLanguage();
                    String strErrorMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", new Locale(strLanguage),
                            "PSS_EnterpriseChangeMgt.Alert.STDPartInstantiationError");
                    // show alert message
                    MqlUtil.mqlCommand(context, "notice $1", strErrorMessage);
                    intReturn = 1;
                }
            }
        } catch (Exception e) {
            logger.error("Error in checkInstantiationOfECOrDEVPartOnSTDPart: ", e);
        }
        return intReturn;
    }
    // TIGTK-6941:Rutuja Ekatpure:3/5/2017:End

    // TIGTK-7004 - 2017-05-17 - VP - START
    /***
     * this method gives the Reference EBOM Structure. Added for TIGTK 7004
     * @param context
     * @param args
     * @return MapList
     * @throws Exception
     * @author VP
     */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getReferenceEBOMWithSelectables(Context context, String[] args) throws Exception {
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);

        String sExpandLevels = getStringValue(paramMap, "emxExpandFilter");
        String selectedFilterValue = getStringValue(paramMap, "ENCBOMRevisionCustomFilter");
        MapList retList = expandReferenceEBOM(context, paramMap);

        if ("Latest".equals(selectedFilterValue) || "Latest Complete".equals(selectedFilterValue)) {
            // handles manual expansion by each level for latest and latest
            // complete
            // Modified for TIGTK-5064:Start
            if (UIUtil.isNullOrEmpty(sExpandLevels)) {
                sExpandLevels = "1";
            }
            // Modified for TIGTK-5064:End
            int expandLevel = "All".equals(sExpandLevels) ? 0 : Integer.parseInt(sExpandLevels);
            MapList childList = null;
            Map obj = null;
            int level;
            for (int index = 0; index < retList.size(); index++) {
                obj = (Map) retList.get(index);
                if (expandLevel == 0 || Integer.parseInt((String) obj.get("level")) < expandLevel) {
                    paramMap.put("partId", (String) obj.get(SELECT_ID));
                    childList = expandReferenceEBOM(context, paramMap);
                    if (childList != null && !childList.isEmpty()) {
                        for (int cnt = 0; cnt < childList.size(); cnt++) {
                            level = Integer.parseInt((String) obj.get("level")) + 1;
                            ((Map) childList.get(cnt)).put("level", String.valueOf(level));
                        }
                        retList.addAll(index + 1, childList);
                    }
                }
            }
        }
        return retList;
    }

    /***
     * this method gives the expanded Reference EBOM Structure. Added for TIGTK 7004
     * @param context
     * @param args
     * @return MapList
     * @throws Exception
     * @author VP
     */
    public MapList expandReferenceEBOM(Context context, HashMap paramMap) throws Exception { // name modified from

        int nExpandLevel = 0;

        String partId = getStringValue(paramMap, "objectId");
        String sExpandLevels = getStringValue(paramMap, "emxExpandFilter");
        String selectedFilterValue = getStringValue(paramMap, "ENCBOMRevisionCustomFilter");
        String complete = PropertyUtil.getSchemaProperty(context, "policy", DomainConstants.POLICY_DEVELOPMENT_PART, "state_Complete");
        String curRevision;
        String latestObjectId;
        String latestRevision;

        String selectedProductConfId = getStringValue(paramMap, "CFFExpressionFilterInput_OID");
        String compileForm = "";
        if (selectedProductConfId == null || selectedProductConfId.equals("undefined")) {
            selectedProductConfId = "";
        }

        if (isValidData(selectedProductConfId)) {
            DomainObject productConfObj = DomainObject.newInstance(context, selectedProductConfId);
            compileForm = productConfObj.getAttributeValue(context, EffectivityFramework.ATTRIBUTE_FILTER_COMPILED_FORM);
        }

        if (!isValidData(selectedFilterValue)) {
            selectedFilterValue = "As Stored";
        }

        if (!isValidData(sExpandLevels)) {
            sExpandLevels = getStringValue(paramMap, "ExpandFilter");
        }

        // TIGTK-9670 : START
        StringList objectSelect = createStringList(new String[] { SELECT_ID, SELECT_NAME, SELECT_REVISION, SELECT_LAST_ID, SELECT_LAST_REVISION, TigerConstants.SELECT_REL_FROM_REFERENCE_EBOM_EXISTS,
                TigerConstants.SELECT_REL_REFERENCE_EBOM_FROM_LAST_REVISION_EXISTS, TigerConstants.SELECT_ATTRIBUTE_ENABLECOMPLIANCE });
        // TIGTK-9670 : END
        String SELECT_ATTRIBUTE_EFFECTIVITY_EXPRESSION = DomainObject.getAttributeSelect(TigerConstants.ATTRIBUTE_PSS_CUSTOMEFFECTIVITYEXPRESSION);
        StringList relSelect = createStringList(
                new String[] { SELECT_RELATIONSHIP_ID, SELECT_ATTRIBUTE_FIND_NUMBER, SELECT_ATTRIBUTE_QUANTITY, SELECT_FROM_ID, SELECT_ATTRIBUTE_EFFECTIVITY_EXPRESSION });

        if (!isValidData(sExpandLevels) || ("Latest".equals(selectedFilterValue) || "Latest Complete".equals(selectedFilterValue))) {
            nExpandLevel = 1;
            partId = getStringValue(paramMap, "partId") == null ? partId : getStringValue(paramMap, "partId");
        } else if ("All".equalsIgnoreCase(sExpandLevels)) {
            nExpandLevel = 0;
        } else {
            nExpandLevel = Integer.parseInt(sExpandLevels);
        }

        Part partObj = new Part(partId);

        MapList ebomList = partObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_REFERENCE_EBOM, TYPE_PART, objectSelect, relSelect, false, true, (short) nExpandLevel, null, null,
                (short) 0, false, false, DomainObject.PAGE_SIZE, null, null, null, null, compileForm);
        Iterator itr = ebomList.iterator();
        Map newMap;

        StringList ebomDerivativeList = EngineeringUtil.getDerivativeRelationships(context, TigerConstants.RELATIONSHIP_PSS_REFERENCE_EBOM, true);

        if ("Latest".equals(selectedFilterValue) || ("Latest Complete".equals(selectedFilterValue))) {
            // Iterate through the maplist and add those parts that are latest
            // but not connected

            while (itr.hasNext()) {
                newMap = (Map) itr.next();

                curRevision = getStringValue(newMap, SELECT_REVISION);
                latestObjectId = getStringValue(newMap, SELECT_LAST_ID);
                latestRevision = getStringValue(newMap, SELECT_LAST_REVISION);

                if (nExpandLevel != 0) {
                    // newMap.put("hasChildren", getStringValue(newMap,
                    // SELECT_REL_FROM_EBOM_EXISTS));
                    newMap.put("hasChildren", EngineeringUtil.getHasChildren(newMap, ebomDerivativeList));
                }

                if ("Latest".equals(selectedFilterValue)) {
                    newMap.put(SELECT_ID, latestObjectId);
                    // newMap.put("hasChildren", getStringValue(newMap,
                    // SELECT_REL_EBOM_FROM_LAST_REVISION_EXISTS));
                    newMap.put("hasChildren", EngineeringUtil.getHasChildren(newMap, ebomDerivativeList));
                }

                else {
                    DomainObject domObjLatest = DomainObject.newInstance(context, latestObjectId);
                    String currSta = domObjLatest.getInfo(context, DomainConstants.SELECT_CURRENT);

                    if (curRevision.equals(latestRevision)) {
                        if (complete.equalsIgnoreCase(currSta)) {
                            newMap.put(SELECT_ID, latestObjectId);
                        } else {
                            itr.remove();
                        }
                    } else {
                        while (true) {
                            if (currSta.equalsIgnoreCase(complete)) {
                                newMap.put(SELECT_ID, latestObjectId);
                                break;
                            } else {
                                BusinessObject boObj = domObjLatest.getPreviousRevision(context);
                                if (!(boObj.toString()).equals("..")) {
                                    boObj.open(context);
                                    latestObjectId = boObj.getObjectId();
                                    domObjLatest = DomainObject.newInstance(context, latestObjectId);
                                    currSta = domObjLatest.getInfo(context, DomainConstants.SELECT_CURRENT);
                                } else {
                                    itr.remove();
                                    break;
                                }
                            }
                        } // End of while
                    } // End of Else
                }
            } // End of While

        } // End of IF, Latest or Latest complete filter is selected

        else if (nExpandLevel != 0) {
            while (itr.hasNext()) {
                newMap = (Map) itr.next();

                // To display + or - in the bom display
                // newMap.put("hasChildren", getStringValue(newMap,
                // SELECT_REL_FROM_EBOM_EXISTS));
                newMap.put("hasChildren", EngineeringUtil.getHasChildren(newMap, ebomDerivativeList));
            }
        }

        return ebomList;
    }

    /***
     * this method Replaces the Part EBOM with latest Revision based on Magic button click. Added for TIGTK 7004
     * @param context
     * @param args
     * @return String
     * @throws Exception
     * @author VP
     */
    public String replaceBOMPartsWithLatestRevision(Context context, String[] args) throws Exception {
        boolean isTrasactionActive = false;
        boolean isPushedContext = false;
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        try {
            ContextUtil.startTransaction(context, true);
            isTrasactionActive = true;
            String strPersonUserAgent = PropertyUtil.getSchemaProperty(context, "person_UserAgent");
            String rpeValue = "True";
            PropertyUtil.setRPEValue(context, "PSS_ReplaceWithLatestRevision", rpeValue, true);
            StringList lstselectStmts = new StringList(4);
            StringList lstrelStmts = new StringList(4);

            lstselectStmts.addElement(DomainConstants.SELECT_ID);
            lstselectStmts.addElement(DomainConstants.SELECT_LAST_ID);
            String SELECT_ATTRIBUTE_DERIVED_CONTEXT = "from[Derived|attribute[Derived Context].value == 'PSS_FOR_REPLACE'].to.id";
            lstselectStmts.addElement(SELECT_ATTRIBUTE_DERIVED_CONTEXT);

            lstrelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            lstrelStmts.addElement(DomainConstants.SELECT_FROM_ID);
            lstrelStmts.addElement(TigerConstants.SELECT_ATTRIBUTE_PSS_REPLACED_WITH_LATEST_REVISION);
            lstrelStmts.addElement("logicalid");

            String strSourceRootObjectID = (String) programMap.get("parentOID");

            DomainObject domSourceRootObject = DomainObject.newInstance(context, strSourceRootObjectID);

            // Added for : TIGTK-8857 - START
            // Added where condition to exclude Standard Parts
            StringBuffer sWhereClause = new StringBuffer();
            sWhereClause.append("policy!= " + "\"" + TigerConstants.POLICY_STANDARDPART + "\"");
            // Added for : TIGTK-8857 - END
            MapList mlConnectedPartsList = domSourceRootObject.getRelatedObjects(context, RELATIONSHIP_EBOM, DomainConstants.TYPE_PART, lstselectStmts, lstrelStmts, false, true, (short) 0,
                    sWhereClause.toString(), null, 0);

            int intConnectedPartsListSize = mlConnectedPartsList.size();

            StringList lstAlreadyProceedRelId = new StringList();

            // Create a map which will help us identify which part is to be replaced with which part
            // Key will be the current BOM part and value will be its latest revision part
            HashMap<String, String> oldNewBOMPartMap = new HashMap<String, String>();

            // List of Rel IDs which are to be deleted
            List<String> lstRelIDsTobeDeleted = new ArrayList<String>();
            String strAttrReplaceWithLatestValue = null;

            StringList slExcludeAttributeLst = new StringList();
            slExcludeAttributeLst.add(TigerConstants.ATTRIBUTE_PSS_REPLACED_WITH_LATEST_REVISION);

            for (int intIndex = 0; intIndex < intConnectedPartsListSize; intIndex++) {
                Map<String, String> mapConnectedPartsMap = (Map) mlConnectedPartsList.get(intIndex);
                String relId = (String) mapConnectedPartsMap.get(DomainConstants.SELECT_RELATIONSHIP_ID);

                if (!lstAlreadyProceedRelId.contains(relId)) {
                    String strBOMPartId = (String) mapConnectedPartsMap.get(DomainConstants.SELECT_ID);
                    String strParentId = (String) mapConnectedPartsMap.get(DomainConstants.SELECT_FROM_ID);
                    String strLastRevisionId = (String) mapConnectedPartsMap.get(DomainConstants.SELECT_LAST_ID);
                    strAttrReplaceWithLatestValue = (String) mapConnectedPartsMap.get(TigerConstants.SELECT_ATTRIBUTE_PSS_REPLACED_WITH_LATEST_REVISION);
                    String strForReplacementID = (String) mapConnectedPartsMap.get("from[Derived].to.id");
                    String strLogicalId = (String) mapConnectedPartsMap.get("logicalid");

                    // TIGTK-8742 - MC - Reference EBOM will not be generated now. But it will be created once the part gets released - START
                    // create the Reference EBOM Relationship bw Parent and current Object and copy all Relationship Attributes to newly created relationship
                    // createAndConnectReferenceEBOM(context, strParentId, strBOMPartId, relId);
                    // TIGTK-8742 - MC - Reference EBOM will not be generated now. But it will be created once the part gets released - END

                    // If the Last revision id and the part object id is same i.e. that the part is not revised, so we won't replace it
                    if (!strBOMPartId.equals(strLastRevisionId) || UIUtil.isNotNullAndNotEmpty(strForReplacementID)) {
                        // If the parent of the BOM part has latest revision, then connect the latest revision of the parent to the child
                        if (oldNewBOMPartMap.containsKey(strParentId)) {
                            strParentId = oldNewBOMPartMap.get(strParentId);
                        }
                        // If Revised object of the Parent is not available then only disconnect
                        else {
                            lstRelIDsTobeDeleted.add(relId);
                        }

                        // Connecting the parent to the latest revision of the part
                        // TIGTK-12664 : FindBug : 05-01-2017 : START
                        DomainRelationship domRel = null;
                        // TIGTK-12664 : FindBug : 05-01-2017 : END
                        if (strAttrReplaceWithLatestValue.equals("FALSE")) {
                            ContextUtil.pushContext(context, strPersonUserAgent, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                            isPushedContext = true;
                            if (UIUtil.isNotNullAndNotEmpty(strForReplacementID)) {
                                String strResult = MqlUtil.mqlCommand(context, "connect bus $1 to $2 relationship $3 logicalid $4 select $5 dump $6",
                                        new String[] { strParentId, strForReplacementID, RELATIONSHIP_EBOM, strLogicalId, DomainConstants.SELECT_ID, "|" });
                                DomainRelationship.setAttributeValue(context, relId, TigerConstants.ATTRIBUTE_PSS_REPLACED_WITH_LATEST_REVISION, "TRUE");
                                domRel = new DomainRelationship(strResult);
                            } else {
                                String strResult = MqlUtil.mqlCommand(context, "connect bus $1 to $2 relationship $3 logicalid $4 select $5 dump $6",
                                        new String[] { strParentId, strLastRevisionId, RELATIONSHIP_EBOM, strLogicalId, DomainConstants.SELECT_ID, "|" });
                                DomainRelationship.setAttributeValue(context, relId, TigerConstants.ATTRIBUTE_PSS_REPLACED_WITH_LATEST_REVISION, "TRUE");
                                domRel = new DomainRelationship(strResult);
                            }

                            // copy all their relationship attributes
                            if (UIUtil.isNotNullAndNotEmpty(domRel.toString())) {
                                MqlUtil.mqlCommand(context, "mod connection $1 add interface $2", new String[] { domRel.toString(), "Effectivity Framework" });
                                copySourceRelDataToNewRel(context, relId, domRel.toString(), slExcludeAttributeLst, true);
                            }

                            ContextUtil.popContext(context);
                            isPushedContext = false;
                            // Putting the mapping so that when the child of the strBOMPartId comes and it has latest revision, we can connect the latest revision of child with latest revision of the
                            // parent.
                            oldNewBOMPartMap.put(strBOMPartId, strLastRevisionId);
                        }

                    }
                }

                lstAlreadyProceedRelId.add(relId);
            }

            // if for a relationship, both the parent and child are being replaced, then the scenario will happen that new revision parent will have 2 revisions of the same part as child
            // Consider this case : Part-1-1 --> Part-2-1 --> Part-3-1. If Part-2-2 and Part-3-2 both exist, then above code will create the following structure:
            // Part-1-1
            // |__ Part-2-1 (This will be removed because relId is already added in lstRelIDsTobeDeleted)
            // |_ Part-3-1
            // |__ Part-2-2
            // |_ Part-3-1 (This is extra and should be removed as it has come from replicate mechanism of the from side EBOM rel)
            // |_ Part-3-2
            // Following code is to remove extra rels. We will add them in lstRelIDsTobeDeleted to remove them.
            Iterator iterator = oldNewBOMPartMap.entrySet().iterator();
            StringList slLogid = new StringList();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                // String strBOMPartId = (String) entry.getKey();
                String strLastRevisionId = (String) entry.getValue();

                DomainObject domLastRevObj = DomainObject.newInstance(context, strLastRevisionId);

                mlConnectedPartsList = domLastRevObj.getRelatedObjects(context, RELATIONSHIP_EBOM, DomainConstants.TYPE_PART, lstselectStmts, lstrelStmts, false, true, (short) 1, null, null, 0);

                for (int intIndex = 0; intIndex < mlConnectedPartsList.size(); intIndex++) {
                    Map<String, String> mapConnectedPartsMap = (Map) mlConnectedPartsList.get(intIndex);
                    String strChildPartId = (String) mapConnectedPartsMap.get(DomainConstants.SELECT_ID);
                    if (oldNewBOMPartMap.containsValue(strChildPartId) && !slLogid.contains(strChildPartId)) {
                        String relId = (String) mapConnectedPartsMap.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                        slLogid.add(strChildPartId);
                        if (!lstRelIDsTobeDeleted.contains(relId))
                            lstRelIDsTobeDeleted.add(relId);
                    }
                }
            }

            ContextUtil.pushContext(context, strPersonUserAgent, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            isPushedContext = true;
            if (lstRelIDsTobeDeleted.size() > 0) {
                String[] strRelIds = (String[]) lstRelIDsTobeDeleted.toArray(new String[lstRelIDsTobeDeleted.size()]);
                DomainRelationship.disconnect(context, strRelIds);
            }
            ContextUtil.popContext(context);
            isPushedContext = false;
            if (isTrasactionActive)
                ContextUtil.commitTransaction(context);

        } catch (Exception ex) {
            logger.error("Error in replaceBOMPartsWithLatestRevision: ", ex);
            if (isTrasactionActive)
                ContextUtil.abortTransaction(context);
            throw ex;

        } finally {
            PropertyUtil.setRPEValue(context, "PSS_ReplaceWithLatestRevision", "", true);
            if (isPushedContext) {
                ContextUtil.popContext(context);
            }
        }
        return "";
    }

    /***
     * this method gives the related ACtive CO of the Part which is responsible for its Release. Added for TIGTK 7004
     * @param context
     * @param args
     * @return StringList
     * @throws Exception
     * @author VP
     */
    public StringList getRelatedActiveCOForPart(Context context, String[] args) throws Exception {
        StringList slActiveCOForPart = new StringList();
        try {
            HashMap<?, ?> mpProgramMap = (HashMap) JPO.unpackArgs(args);
            MapList mlObjectList = (MapList) mpProgramMap.get("objectList");

            if (mlObjectList != null && !mlObjectList.isEmpty()) {
                Iterator itrObjectList = mlObjectList.iterator();
                while (itrObjectList.hasNext()) {
                    Map mpObjectIdMap = (Map) itrObjectList.next();
                    String strCurrentPartId = (String) mpObjectIdMap.get(DomainConstants.SELECT_ID);

                    StringBuffer sbReturnBuffer = new StringBuffer();

                    if (UIUtil.isNotNullAndNotEmpty(strCurrentPartId)) {

                        DomainObject domPartObject = DomainObject.newInstance(context, strCurrentPartId);
                        // Object Selects
                        StringList slObjectSelect = new StringList();
                        slObjectSelect.add(DomainConstants.SELECT_ID);
                        slObjectSelect.add(DomainConstants.SELECT_CURRENT);
                        slObjectSelect.add(DomainConstants.SELECT_NAME);
                        // Relationship Selects
                        StringList slRelSelect = new StringList();
                        slRelSelect.add(DomainConstants.SELECT_RELATIONSHIP_ID);
                        slRelSelect.add(ChangeConstants.SELECT_ATTRIBUTE_REQUESTED_CHANGE);
                        // Object Where clause
                        String strObjectWhere = "current != " + TigerConstants.STATE_PSS_CANCELPART_CANCELLED;
                        // Relationship Where clause
                        String strRelWhere = "attribute[" + ChangeConstants.ATTRIBUTE_REQUESTED_CHANGE + "] == '" + ChangeConstants.FOR_RELEASE + "'";
                        // Relationship Pattern
                        Pattern relationshipPattern = new Pattern(ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM);
                        // Type Pattern
                        Pattern typePattern = new Pattern(TigerConstants.TYPE_CHANGEACTION);
                        // Get connected CAs of Part
                        MapList mlConnectedCASOfPartList = domPartObject.getRelatedObjects(context, // context
                                relationshipPattern.getPattern(), // relationship
                                // pattern
                                typePattern.getPattern(), // object pattern
                                slObjectSelect, // object selects
                                slRelSelect, // relationship selects
                                true, // to direction
                                false, // from direction
                                (short) 0, // recursion level
                                strObjectWhere, // object where clause
                                strRelWhere, // relationship where clause
                                (short) 0, false, // checkHidden
                                true, // preventDuplicates
                                (short) 1000, // pageSize
                                null, null, null, null);
                        if (mlConnectedCASOfPartList != null && !mlConnectedCASOfPartList.isEmpty()) {
                            Iterator itrConnectedCASOfPartList = mlConnectedCASOfPartList.iterator();
                            while (itrConnectedCASOfPartList.hasNext()) {
                                Map mpCAMap = (Map) itrConnectedCASOfPartList.next();
                                String strCAObjectId = (String) mpCAMap.get(DomainConstants.SELECT_ID);
                                if (UIUtil.isNotNullAndNotEmpty(strCAObjectId)) {
                                    DomainObject domCAObject = DomainObject.newInstance(context, strCAObjectId);
                                    // Object Selects
                                    StringList slObjectSel = new StringList();
                                    slObjectSel.add(DomainConstants.SELECT_ID);
                                    slObjectSel.add(DomainConstants.SELECT_NAME);
                                    // Relationship Selects
                                    StringList slRelSel = new StringList();
                                    slRelSel.add(DomainConstants.SELECT_RELATIONSHIP_ID);
                                    strObjectWhere = "(current == '" + TigerConstants.STATE_CHANGEACTION_INWORK + "' || current == '" + TigerConstants.STATE_CHANGEACTION_INAPPROVAL + "')";

                                    MapList mlConnectedCOofCAList = domCAObject.getRelatedObjects(context, // context
                                            TigerConstants.RELATIONSHIP_CHANGEACTION, // relationship
                                            TigerConstants.TYPE_PSS_CHANGEORDER, // type
                                            slObjectSel, // object selects
                                            slRelSel, // relationship selects
                                            true, // to direction
                                            false, // from direction
                                            (short) 0, // recursion level
                                            strObjectWhere, // object where clause
                                            DomainConstants.EMPTY_STRING, // relationship where clause
                                            0);
                                    if (mlConnectedCOofCAList != null && !mlConnectedCOofCAList.isEmpty()) {
                                        Iterator itrConnectedCOofCAList = mlConnectedCOofCAList.iterator();
                                        while (itrConnectedCOofCAList.hasNext()) {
                                            Map mpCOMap = (Map) itrConnectedCOofCAList.next();

                                            String strRelatedCOObjID = (String) mpCOMap.get(DomainConstants.SELECT_ID);
                                            String strRelatedCOName = (String) mpCOMap.get(DomainConstants.SELECT_NAME);
                                            sbReturnBuffer.append("<a class=\"object\" onClick='JavaScript:emxTableColumnLinkClick(\"../common/emxTree.jsp?objectId=");
                                            sbReturnBuffer.append(XSSUtil.encodeForHTMLAttribute(context, strRelatedCOObjID));
                                            sbReturnBuffer.append("\")'>");
                                            sbReturnBuffer.append(strRelatedCOName);
                                            sbReturnBuffer.append("</a>");
                                        } // Co while en
                                    } // CO if end

                                } // CA if end
                            } // CA while end
                        } else {
                            sbReturnBuffer.append(EMPTY_STRING);
                        }
                    }
                    slActiveCOForPart.add(sbReturnBuffer.toString());
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error in getRelatedActiveCOForPart: ", e);
        }
        return slActiveCOForPart;
    }// End of Method getCOForReleaseInfoFromPart()
     // TIGTK-7004 - 2017-05-17 - VP - END

    public void replaceBOMPartsWithLatestRevisionForDEVParts(Context context, String[] args) throws Exception {
        boolean isTrasactionActive = false;
        boolean isPushedContext = false;
        try {
            // When Go to production is done, then also the part is revised. But for Go to production,we don't have to replace the BOM Parts with
            // its latest revision.
            // OOTB Code puts "BOM_GO_TO_PRODUCTION" variable in the context at the time when Go to production is launched, if its value is true, then
            // will return from the method
            String rpeValue = "True";
            PropertyUtil.setRPEValue(context, "PSS_ReplaceWithLatestRevision", rpeValue, true);

            // The current method is applicable only for the DEV Parts, hence if it is other than DEV Part, then we will return from the method.
            String strOldRevPartID = args[0];
            String strPolicy = args[1];
            if (!TigerConstants.POLICY_PSS_DEVELOPMENTPART.equals(strPolicy))
                return;

            ContextUtil.startTransaction(context, true);
            isTrasactionActive = true;

            StringList lstselectStmts = new StringList(2);
            StringList lstrelStmts = new StringList(4);

            lstselectStmts.addElement(DomainConstants.SELECT_ID);
            lstselectStmts.addElement(DomainConstants.SELECT_LAST_ID);
            lstselectStmts.addElement("last.current");

            lstrelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            lstrelStmts.addElement(DomainConstants.SELECT_FROM_ID);
            lstrelStmts.addElement("from.current");
            lstrelStmts.addElement(TigerConstants.SELECT_ATTRIBUTE_PSS_REPLACED_WITH_LATEST_REVISION);
            lstrelStmts.addElement("logicalid");

            String strNewRevPartID = args[2];
            DomainObject domNewRevPartObject = DomainObject.newInstance(context, strNewRevPartID);

            String strUserAgent = PropertyUtil.getSchemaProperty(context, "person_UserAgent");
            StringList lstEBOMRelIds = domNewRevPartObject.getInfoList(context, "from[" + DomainConstants.RELATIONSHIP_EBOM + "].id");

            String[] strRelIds = (String[]) lstEBOMRelIds.toArray(new String[lstEBOMRelIds.size()]);
            try {
                ContextUtil.pushContext(context, strUserAgent, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                DomainRelationship.disconnect(context, strRelIds);
            } catch (Exception ex) {
                logger.error("Error in replaceBOMPartsWithLatestRevisionForDEVParts while disconnecting EBOM  Relationships: ", ex.toString());
            } finally {
                ContextUtil.popContext(context);
            }

            DomainObject domOldRevPartObject = DomainObject.newInstance(context, strOldRevPartID);
            MapList mlConnectedPartsList = domOldRevPartObject.getRelatedObjects(context, RELATIONSHIP_EBOM, DomainConstants.TYPE_PART, lstselectStmts, lstrelStmts, false, true, (short) 0, null, null,
                    0);

            int intConnectedPartsListSize = mlConnectedPartsList.size();

            StringList lstAlreadyProceedRelId = new StringList();

            // Create a map which will help us identify which part is to be replaced with which part
            // Key will be the current BOM part and value will be its latest revision part
            HashMap<String, String> oldNewBOMPartMap = new HashMap<String, String>();

            // List of Rel IDs which are to be deleted
            List<String> lstRelIDsTobeDeleted = new ArrayList<String>();

            StringList slExcludeAttributeLst = new StringList();
            slExcludeAttributeLst.add(TigerConstants.ATTRIBUTE_PSS_REPLACED_WITH_LATEST_REVISION);

            HashMap<String, String> mapPartStateMapping = new HashMap<String, String>();

            for (int intIndex = 0; intIndex < intConnectedPartsListSize; intIndex++) {
                Map<String, String> mapConnectedPartsMap = (Map) mlConnectedPartsList.get(intIndex);
                String relId = (String) mapConnectedPartsMap.get(DomainConstants.SELECT_RELATIONSHIP_ID);

                if (!lstAlreadyProceedRelId.contains(relId)) {
                    String strBOMPartId = (String) mapConnectedPartsMap.get(DomainConstants.SELECT_ID);
                    String strBOMPartLevel = (String) mapConnectedPartsMap.get(DomainConstants.SELECT_LEVEL);
                    String strParentId = (String) mapConnectedPartsMap.get(DomainConstants.SELECT_FROM_ID);
                    String strLastRevisionId = (String) mapConnectedPartsMap.get(DomainConstants.SELECT_LAST_ID);
                    String strLogicalId = (String) mapConnectedPartsMap.get("logicalid");

                    String strLastRevisionState = (String) mapConnectedPartsMap.get("last.current");
                    if (!mapPartStateMapping.containsKey(strLastRevisionId))
                        mapPartStateMapping.put(strLastRevisionId, strLastRevisionState);

                    String strParentState = (String) mapConnectedPartsMap.get("from.current");
                    if (!mapPartStateMapping.containsKey(strParentId))
                        mapPartStateMapping.put(strParentId, strParentState);

                    if (strBOMPartLevel.equalsIgnoreCase("1")) {
                        ContextUtil.pushContext(context, strUserAgent, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                        isPushedContext = true;
                        String strResult = MqlUtil.mqlCommand(context, "connect bus $1 to $2 relationship $3 logicalid $4 select $5 dump $6",
                                new String[] { strNewRevPartID, strLastRevisionId, RELATIONSHIP_EBOM, strLogicalId, DomainConstants.SELECT_ID, "|" });
                        DomainRelationship domRel = new DomainRelationship(strResult);
                        DomainRelationship.setAttributeValue(context, relId, TigerConstants.ATTRIBUTE_PSS_REPLACED_WITH_LATEST_REVISION, "TRUE");

                        // copy all their relationship attributes
                        if (UIUtil.isNotNullAndNotEmpty(domRel.toString())) {
                            MqlUtil.mqlCommand(context, "mod connection $1 add interface $2", new String[] { domRel.toString(), "Effectivity Framework" });
                            copySourceRelDataToNewRel(context, relId, domRel.toString(), slExcludeAttributeLst, true);
                        }

                        ContextUtil.popContext(context);
                        isPushedContext = false;

                        if (!strBOMPartId.equals(strLastRevisionId)) {
                            oldNewBOMPartMap.put(strBOMPartId, strLastRevisionId);
                        }

                    } // If the Last revision id and the part object id is same i.e. that the part is not revised, so we won't replace it
                    else if (!strBOMPartId.equals(strLastRevisionId)) {
                        // If the parent of the BOM part has latest revision, then connect the latest revision of the parent to the child
                        if (oldNewBOMPartMap.containsKey(strParentId)) {
                            strParentId = oldNewBOMPartMap.get(strParentId);
                        }
                        // If Revised object of the Parent is not available then only disconnect
                        else {
                            lstRelIDsTobeDeleted.add(relId);
                        }

                        // Connecting the parent to the latest revision of the part

                        String strAttrReplaceWithLatestValue = (String) mapConnectedPartsMap.get(TigerConstants.SELECT_ATTRIBUTE_PSS_REPLACED_WITH_LATEST_REVISION);
                        if (strAttrReplaceWithLatestValue.equals("FALSE")) {
                            ContextUtil.pushContext(context, strUserAgent, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                            isPushedContext = true;
                            String strCurrentParentState = mapPartStateMapping.get(strParentId);
                            if (TigerConstants.STATE_DEVELOPMENTPART_CREATE.equals(strCurrentParentState)) {
                                String strResult = MqlUtil.mqlCommand(context, "connect bus $1 to $2 relationship $3 logicalid $4 select $5 dump $6",
                                        new String[] { strParentId, strLastRevisionId, RELATIONSHIP_EBOM, strLogicalId, DomainConstants.SELECT_ID, "|" });
                                DomainRelationship domRel = new DomainRelationship(strResult);
                                DomainRelationship.setAttributeValue(context, relId, TigerConstants.ATTRIBUTE_PSS_REPLACED_WITH_LATEST_REVISION, "TRUE");

                                // copy all their relationship attributes
                                if (UIUtil.isNotNullAndNotEmpty(domRel.toString())) {
                                    MqlUtil.mqlCommand(context, "mod connection $1 add interface $2", new String[] { domRel.toString(), "Effectivity Framework" });
                                    copySourceRelDataToNewRel(context, relId, domRel.toString(), slExcludeAttributeLst, true);
                                }

                                ContextUtil.popContext(context);
                                isPushedContext = false;
                            }
                            // if we are not connecting the part with EBOM because of parent, then the rel should not be removed
                            else {
                                lstRelIDsTobeDeleted.remove(relId);
                            }
                        }
                        // TIGTK-10743 : START
                        // Putting the mapping so that when the child of the strBOMPartId comes and it has latest revision, we can connect the latest revision of child with latest revision of the
                        // parent.
                        oldNewBOMPartMap.put(strBOMPartId, strLastRevisionId);
                        // TIGTK-10743 : END
                    }
                }

                lstAlreadyProceedRelId.add(relId);
            }

            // if for a relationship, both the parent and child are being replaced, then the scenario will happen that new revision parent will have 2 revisions of the same part as child
            // Consider this case : Part-1-1 --> Part-2-1 --> Part-3-1. If Part-2-2 and Part-3-2 both exist, then above code will create the following structure:
            // Part-1-1
            // |__ Part-2-1 (This will be removed because relId is already added in lstRelIDsTobeDeleted)
            // |_ Part-3-1
            // |__ Part-2-2
            // |_ Part-3-1 (This is extra and should be removed as it has come from replicate mechanism of the from side EBOM rel)
            // |_ Part-3-2
            // Following code is to remove extra rels. We will add them in lstRelIDsTobeDeleted to remove them.
            Iterator iterator = oldNewBOMPartMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                // String strBOMPartId = (String) entry.getKey();
                String strLastRevisionId = (String) entry.getValue();

                DomainObject domLastRevObj = DomainObject.newInstance(context, strLastRevisionId);

                mlConnectedPartsList = domLastRevObj.getRelatedObjects(context, RELATIONSHIP_EBOM, DomainConstants.TYPE_PART, lstselectStmts, lstrelStmts, false, true, (short) 1, null, null, 0);

                for (int intIndex = 0; intIndex < mlConnectedPartsList.size(); intIndex++) {
                    Map<String, String> mapConnectedPartsMap = (Map) mlConnectedPartsList.get(intIndex);
                    String strChildPartId = (String) mapConnectedPartsMap.get(DomainConstants.SELECT_ID);
                    if (oldNewBOMPartMap.containsKey(strChildPartId)) {
                        String relId = (String) mapConnectedPartsMap.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                        if (!lstRelIDsTobeDeleted.contains(relId))
                            lstRelIDsTobeDeleted.add(relId);
                    }
                }
            }

            ContextUtil.pushContext(context, strUserAgent, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            isPushedContext = true;
            if (lstRelIDsTobeDeleted.size() > 0) {
                String[] strRelIdS = (String[]) lstRelIDsTobeDeleted.toArray(new String[lstRelIDsTobeDeleted.size()]);
                DomainRelationship.disconnect(context, strRelIdS);
            }
            ContextUtil.popContext(context);
            isPushedContext = false;
            if (isTrasactionActive)
                ContextUtil.commitTransaction(context);

        } catch (Exception ex) {
            logger.error("Error in replaceBOMPartsWithLatestRevision: ", ex);
            if (isTrasactionActive)
                ContextUtil.abortTransaction(context);
            throw ex;

        } finally {
            PropertyUtil.setRPEValue(context, "PSS_ReplaceWithLatestRevision", "", true);
            if (isPushedContext)
                ContextUtil.popContext(context);
        }
        return;
    }

    // Added for TIGTK-8079 : PSE : 01-06-2017 : START
    /**
     * Calculate and return the next value of the attribute Find Number
     * @param
     * @return String
     * @throws Exception
     * @author PSE
     * @since 01-06-2017
     */

    public String getNextFindNumber(Context context, String[] args) throws Exception {
        String sFindNumber = DomainConstants.EMPTY_STRING;
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            sFindNumber = getNextFindNumber((StringList) programMap.get("slFindNumbers"));
        } catch (Exception ex) {
            logger.error("Error in PSS_emxPart : getRepalceFindNumber() : ", ex);
        }
        return sFindNumber;
    }
    // Added for TIGTK-8079 : PSE : 01-06-2017 : END

    // Added for TIGTK-6287 : Pkh : Phase-2.0 : Start
    /**
     * Check trigger to check the rule when Dev part is connected under EC part and Std Part.
     * @param args
     * @return int :0 if check trigger passes,1 if check trigger fail
     * @throws Exception
     * @author PKH
     * @since 05-06-2017
     */
    public int checkIfParentIsStandardOrECPart(Context context, String[] args) throws Exception {
        int chkTriggerResult = 0;
        try {
            String strFromObjectId = args[0];
            String strToObjectId = args[1];
            DomainObject domFromPart = DomainObject.newInstance(context, strFromObjectId);
            DomainObject domToPart = DomainObject.newInstance(context, strToObjectId);
            String strToObjectPolicy = domToPart.getInfo(context, DomainConstants.SELECT_POLICY);
            String strFromObjectPolicy = domFromPart.getInfo(context, DomainConstants.SELECT_POLICY);
            if (TigerConstants.POLICY_PSS_DEVELOPMENTPART.equalsIgnoreCase(strToObjectPolicy)) {
                if (strFromObjectPolicy.equals(TigerConstants.POLICY_PSS_ECPART)) {

                    String strUnableToAddDevPartInECPartMessage = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(),
                            "PSS_emxEngineeringCentral.Validation.ConnectDevPartToECPartMessage");
                    emxContextUtil_mxJPO.mqlNotice(context, strUnableToAddDevPartInECPartMessage);
                    chkTriggerResult = 1;
                }

            }

        } catch (Exception e) {
            logger.error("Error in checkIfParentIsStandardOrECPart: ", e);
        }

        return chkTriggerResult;

    }

    // Added for TIGTK-6287 : Pkh : Phase-2.0 : End
    // Added for TIGTK-6289 : Pkh : Phase-2.0 : Start
    /**
     * Check trigger to check related MG specs are beyond Release state
     * @param args
     * @return int :0 if check trigger passes,1 if check trigger fail
     * @throws Exception
     * @author PKH
     * @since 05-06-2017
     */
    public int checkAttachedMGSpecsBeyondRelease(Context context, String[] args) throws Exception {
        int chkTriggerResult = 0;
        try {
            String strObjectId = args[0];
            if (UIUtil.isNotNullAndNotEmpty(strObjectId)) {
                DomainObject domSourcePartObj = DomainObject.newInstance(context, strObjectId);
                StringList lstselectStmts = new StringList(2);
                lstselectStmts.addElement(DomainConstants.SELECT_ID);
                lstselectStmts.addElement(DomainConstants.SELECT_CURRENT);

                StringList lstrelStmts = new StringList(1);
                lstrelStmts.add(DomainConstants.SELECT_RELATIONSHIP_ID);

                Pattern typePattern = new Pattern(DomainConstants.TYPE_CAD_DRAWING);
                typePattern.addPattern(DomainConstants.TYPE_CAD_MODEL);

                Pattern relPattern = new Pattern(RELATIONSHIP_PART_SPECIFICATION);

                String sClause = "(attribute[" + TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE + "] == \"" + TigerConstants.ATTR_RANGE_PSSGEOMETRYTYPE + "\" )";

                MapList mlCADObject = domSourcePartObj.getRelatedObjects(context, relPattern.getPattern(), typePattern.getPattern(), lstselectStmts, lstrelStmts, false, true, (short) 1, sClause, null,
                        0);
                if (mlCADObject.size() > 0) {
                    if (!((String) ((Map) mlCADObject.get(0)).get(DomainConstants.SELECT_CURRENT)).equals(TigerConstants.STATE_RELEASED_CAD_OBJECT)) {
                        String strAttachedMGSpecValidationFailedToReleaseDevPartMessage = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(),
                                "PSS_emxEngineeringCentral.Validation.AttachedMGSpecValidationFailedToReleaseDevPart");
                        emxContextUtil_mxJPO.mqlNotice(context, strAttachedMGSpecValidationFailedToReleaseDevPartMessage);

                        chkTriggerResult = 1;
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Error in checkAttachedMGSpecsBeyondRelease: ", e);
        }

        return chkTriggerResult;

    }

    // Added for TIGTK-6289 : Pkh : Phase-2.0 : End
    // TIGTK-6787 | 08/06/2017 | Harika Varanasi : Starts
    /**
     * getPartWhereUsed method Added this method for TIGTK-6787 by Harika Varanasi
     */
    public MapList getPartWhereUsed(Context context, String[] args) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String SELECT_PART_MODE = "to[" + RELATIONSHIP_PART_REVISION + "].from.attribute[" + PropertyUtil.getSchemaProperty(context, "attribute_PartMode") + "]";
        String SELECT_RAISED_AGAINST_ECR_CURRENT = "to[" + RELATIONSHIP_RAISED_AGAINST_ECR + "].from[" + TYPE_ECR + "].current";
        String SELECT_RAISED_AGAINST_ECR = "to[" + RELATIONSHIP_RAISED_AGAINST_ECR + "].from[" + TYPE_ECR + "].name";
        boolean isWhereUsedPLBOM = false;
        String sWhereUsedPLBOM = (String) programMap.get("whereUsedPLBOM");

        if ("true".equalsIgnoreCase(sWhereUsedPLBOM))
            isWhereUsedPLBOM = true;

        String objectId = getStringValue(programMap, "objectId");
        MapList finalListReturn = new MapList();

        String parentId = getStringValue(programMap, "parentId");
        if (parentId != null && !(parentId.equals(objectId))) {
            return finalListReturn;
        }
        if (isValidData(objectId)) {
            MapList endNodeList = null;
            MapList ebomSubstituteList = null;
            MapList spareSubAltPartList = null;
            MapList partWhereUsedEBOMList = null;
            // TIGTK-14536 29-11-2018 mkakade - START
            MapList partWhereUsedEBOMListAll = null;
            // TIGTK-14536 29-11-2018 mkakade - END
            String strCompiledBinaryCode = null;

            String strSelectedFN = null;
            String strSelectedLevel = null;
            String strEffectivityOID = null;
            String strSelectedRefDes = null;
            String strEBOMSubstitute = null;
            String strSelectedRevisions = null;
            String strSelectedLevelValue = null;
            StringList objectSelect = null;
            StringList relSelect = null;
            String pcFilter = null;

            String REL_TYPE = DomainConstants.RELATIONSHIP_EBOM + "," + RELATIONSHIP_EBOM_PENDING;

            if (isWhereUsedPLBOM) {

                String sViewFilter = (String) programMap.get("MFGPlanningMBOMWhereUsedViewFilter");
                strSelectedLevel = getStringValue(programMap, "MFGPlanningWhereUsedLevelCustomFilter");
                strSelectedLevelValue = getStringValue(programMap, "MFGPlanningMBOMWhereUsedLevelText");

                strCompiledBinaryCode = (String) programMap.get("CompiledBinaryCode");

                if (("Current").equalsIgnoreCase(sViewFilter) || UIUtil.isNullOrEmpty(sViewFilter)) {
                    REL_TYPE = RELATIONSHIP_PLBOM;
                } else {
                    REL_TYPE = RELATIONSHIP_PLBOM + "," + RELATIONSHIP_PLBOM_PENDING;
                }

                objectSelect = new StringList(5);
                relSelect = new StringList(8);

                objectSelect.add(DomainConstants.SELECT_ID);
                objectSelect.add(DomainConstants.SELECT_DESCRIPTION);
                objectSelect.add(DomainConstants.SELECT_TYPE);
                objectSelect.add(DomainConstants.SELECT_POLICY);
                objectSelect.add(DomainConstants.SELECT_NAME);
                objectSelect.add(SELECT_PART_MODE);

                relSelect.add(DomainRelationship.SELECT_ID);
                relSelect.add(DomainRelationship.SELECT_TYPE);
                relSelect.add(DomainObject.SELECT_FIND_NUMBER);
                relSelect.add(DomainConstants.SELECT_ATTRIBUTE_QUANTITY);
                relSelect.add(EngineeringConstants.SELECT_PLANT_ID);
                relSelect.add(DomainConstants.SELECT_ATTRIBUTE_REFERENCE_DESIGNATOR);
                relSelect.add(DomainConstants.SELECT_ATTRIBUTE_USAGE);
                relSelect.add(DomainConstants.SELECT_LEVEL);

            } else {
                strSelectedFN = getStringValue(programMap, "ENCPartWhereUsedFNTextBox");
                strSelectedLevel = getStringValue(programMap, "ENCPartWhereUsedLevel");
                strEffectivityOID = getStringValue(programMap, "CFFExpressionFilterInput_actualValue");
                strSelectedRefDes = getStringValue(programMap, "ENCPartWhereUsedRefDesTextBox");
                strEBOMSubstitute = getStringValue(programMap, "displayEBOMSub");
                strSelectedRevisions = getStringValue(programMap, "ENCPartWhereUsedRevisions");
                strSelectedLevelValue = getStringValue(programMap, "ENCPartWhereUsedLevelTextBox");

                pcFilter = getStringValue(programMap, "PUEUEBOMProductConfigurationFilter_actualValue");

                objectSelect = createStringList(new String[] { DomainConstants.SELECT_NAME, DomainConstants.SELECT_ID, SELECT_PART_MODE, SELECT_RAISED_AGAINST_ECR, SELECT_PART_TO_ECR_CURRENT,
                        SELECT_PART_TO_ECO_CURRENT, REL_TO_EBOM_EXISTS, SELECT_RAISED_AGAINST_ECR_CURRENT, POLICY_CLASSIFICATION });

                relSelect = createStringList(new String[] { DomainConstants.SELECT_LEVEL, DomainConstants.SELECT_FIND_NUMBER, DomainConstants.SELECT_ATTRIBUTE_REFERENCE_DESIGNATOR,
                        DomainRelationship.SELECT_NAME, DomainConstants.SELECT_ATTRIBUTE_QUANTITY, DomainRelationship.SELECT_ID });

                strCompiledBinaryCode = getCompiledBinaryCode(context, strEffectivityOID, pcFilter);
            }

            String REV_ALL = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", new Locale("en"), "emxEngineeringCentral.Part.WhereUsedRevisionAll");
            String LEVEL_ALL = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", new Locale("en"), "emxEngineeringCentral.Part.WhereUsedLevelAll");
            String LEVEL_UPTO = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", new Locale("en"), "emxEngineeringCentral.Part.WhereUsedLevelUpTo");
            String LEVEL_HIGHEST = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", new Locale("en"), "emxEngineeringCentral.Part.WhereUsedLevelHighest");
            String REV_LATEST_RELEASED = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", new Locale("en"),
                    "emxEngineeringCentral.Part.WhereUsedRevisionLatestReleased");
            String LEVEL_UPTO_AND_HIGHEST = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", new Locale("en"),
                    "emxEngineeringCentral.Part.WhereUsedLevelUpToAndHighest");

            boolean boolAddEndItemsToList = false;

            boolean isECCInstalled = FrameworkUtil.isSuiteRegistered(context, "appVersionEngineeringConfigurationCentral", false, null, null);

            String objectWhere = "revision == 'last'";
            // TIGTK-14536 29-11-2018 mkakade - START
            String objectWhereAll = null;
            if (isECCInstalled) {
                String STATE_SUPERSEDED = PropertyUtil.getSchemaProperty(context, "Policy", POLICY_CONFIGURED_PART, "state_Superseded");// TIGTK-8936
                objectWhereAll = "current != '" + STATE_SUPERSEDED + "'";
            }
            // TIGTK-14536 29-11-2018 mkakade - END
            if (REV_ALL.equals(strSelectedRevisions)) {
                objectWhere = null;

                if (isECCInstalled) {
                    String STATE_SUPERSEDED = PropertyUtil.getSchemaProperty(context, "Policy", POLICY_CONFIGURED_PART, "state_Superseded");// TIGTK-8936
                    objectWhere = "current != '" + STATE_SUPERSEDED + "'";
                }
            } else if (REV_LATEST_RELEASED.equals(strSelectedRevisions)) {
                objectWhere = "(current == '" + DomainConstants.STATE_PART_RELEASE + "' && (revision == 'last' || next.current != '" + DomainConstants.STATE_PART_RELEASE + "'))";
            }

            Short shRecurseToLevel = 1;
            if (LEVEL_HIGHEST.equals(strSelectedLevel)) {
                shRecurseToLevel = -1;
            } else if (LEVEL_ALL.equals(strSelectedLevel)) {
                shRecurseToLevel = 0;
            } else if ((LEVEL_UPTO.equals(strSelectedLevel) || LEVEL_UPTO_AND_HIGHEST.equals(strSelectedLevel)) && isValidData(strSelectedLevelValue)) {
                shRecurseToLevel = Short.parseShort(strSelectedLevelValue);
            }

            DomainObject domObj = DomainObject.newInstance(context, objectId);

            partWhereUsedEBOMList = domObj.getRelatedObjects(context, REL_TYPE, DomainConstants.TYPE_PART, objectSelect, relSelect, true, false, shRecurseToLevel, objectWhere, null, (short) 0, false,
                    false, (short) 0, null, null, null, null, strCompiledBinaryCode);
            // TIGTK-14536 29-11-2018 mkakade - START
            if (!REV_ALL.equals(strSelectedRevisions)) {
                partWhereUsedEBOMListAll = domObj.getRelatedObjects(context, REL_TYPE, DomainConstants.TYPE_PART, objectSelect, relSelect, true, false, shRecurseToLevel, objectWhereAll, null,
                        (short) 0, false, false, (short) 0, null, null, null, null, strCompiledBinaryCode);
                if (null == partWhereUsedEBOMList) {
                    partWhereUsedEBOMList = partWhereUsedEBOMListAll;
                } else {
                    partWhereUsedEBOMList.addAll(partWhereUsedEBOMListAll);
                }
            }
            // TIGTK-14536 29-11-2018 mkakade - END

            int nPartWhereUsedEBOMListCnt = partWhereUsedEBOMList.size();
            MapList tempPartWhereUsedEBOMList = new MapList();
            partWhereUsedEBOMList.sortStructure(DomainConstants.SELECT_NAME, "ascending", "String");
            String strCurrentLevel = "";
            String strCurrentName = "";
            // TIGTK-10686 | 30/10/2017 | TS : Starts
            StringList slUniquePartAndLevel = new StringList();
            String strCurrentPartAndLevel = "";
            for (int i = 0; i < nPartWhereUsedEBOMListCnt; i++) {
                Map tempMap = (Map) partWhereUsedEBOMList.get(i); // TIGTK-8938
                strCurrentLevel = (String) tempMap.get(DomainConstants.SELECT_LEVEL);
                strCurrentName = (String) tempMap.get(DomainConstants.SELECT_NAME);
                strCurrentPartAndLevel = strCurrentName + strCurrentLevel;
                if (!slUniquePartAndLevel.contains(strCurrentPartAndLevel)) {
                    slUniquePartAndLevel.add(strCurrentName + strCurrentLevel);
                    tempPartWhereUsedEBOMList.add(tempMap);
                }

            }
            // TIGTK-10686 | 30/10/2017 | TS : Ends
            partWhereUsedEBOMList = tempPartWhereUsedEBOMList;

            if (!isValidData(strSelectedRefDes) && !isValidData(strSelectedFN)) {
                spareSubAltPartList = getSpareSubAltPartList(context, domObj, objectSelect, relSelect, objectWhere);
            }

            if (LEVEL_UPTO_AND_HIGHEST.equals(strSelectedLevel)) {
                boolAddEndItemsToList = true;
                endNodeList = domObj.getRelatedObjects(context, REL_TYPE, DomainConstants.TYPE_PART, objectSelect, relSelect, true, false, (short) -1, objectWhere, null, (short) 0, false, false,
                        (short) 0, null, null, null, null, strCompiledBinaryCode);
            }

            if ("true".equalsIgnoreCase(strEBOMSubstitute)) { // Only if MFG is installed strEBOMSubstitute can be true.
                ebomSubstituteList = getEbomSustituteParts(context, objectId, objectSelect, relSelect, objectWhere);
            }

            finalListReturn = mergeList(partWhereUsedEBOMList, endNodeList, spareSubAltPartList, ebomSubstituteList, boolAddEndItemsToList, strSelectedRefDes, strSelectedFN);
        }
        addRowEditableToList(finalListReturn);
        return finalListReturn;
    }

    /**
     * getSpareSubAltPartList method
     * @param context
     * @param domObj
     * @param objectSelect
     * @param relSelect
     * @param objectWhere
     * @return
     * @throws Exception
     *             Added this method for TIGTK-6787 by Harika Varanasi
     */
    private MapList getSpareSubAltPartList(Context context, DomainObject domObj, StringList objectSelect, StringList relSelect, String objectWhere) throws Exception {
        String relSpareSubAlternate = DomainConstants.RELATIONSHIP_ALTERNATE + "," + DomainConstants.RELATIONSHIP_SPARE_PART;

        MapList whereUsedSpareSubAltList = domObj.getRelatedObjects(context, relSpareSubAlternate, DomainConstants.QUERY_WILDCARD, objectSelect, relSelect, true, false, (short) 1, objectWhere, null,
                null, null, null);

        int size = whereUsedSpareSubAltList == null ? 0 : whereUsedSpareSubAltList.size();
        MapList listReturn = new MapList(size);

        Map map;

        for (int i = 0; i < size; i++) {
            map = (Map) whereUsedSpareSubAltList.get(i);
            listReturn.add(map);
        }

        return listReturn;
    }

    /**
     * mergeList method
     * @param whereUsedList
     * @param endItemList
     * @param spareSubAltPartList
     * @param ebomSubList
     * @param boolAddEndItemsToList
     * @param refDesFilter
     * @param fnFilter
     * @return Added this method for TIGTK-6787 by Harika Varanasi
     */
    private MapList mergeList(MapList whereUsedList, MapList endItemList, MapList spareSubAltPartList, MapList ebomSubList, boolean boolAddEndItemsToList, String refDesFilter, String fnFilter) {
        int iWhereUsedListSize = whereUsedList == null ? 0 : whereUsedList.size();
        int iEndItemListSize = endItemList == null ? 0 : endItemList.size();
        int iEbomSubListSize = ebomSubList == null ? 0 : ebomSubList.size();
        int iSpareSubAltSize = spareSubAltPartList == null ? 0 : spareSubAltPartList.size();

        StringList sListEndItemId = getDataForThisKey(endItemList, DomainConstants.SELECT_ID);

        MapList listReturn = new MapList(iWhereUsedListSize);

        Map map;

        String objectId;
        String strLevel;
        String strRelEBOMExists;

        for (int i = 0; i < iWhereUsedListSize; i++) {
            map = (Map) whereUsedList.get(i);

            objectId = getStringValue(map, DomainConstants.SELECT_ID);

            if (isFNAndRefDesFilterPassed(map, refDesFilter, fnFilter)) {

                strLevel = getStringValue(map, "level");
                map.put("objectLevel", strLevel);

                strRelEBOMExists = getStringValue(map, REL_TO_EBOM_EXISTS);
                if ("False".equals(strRelEBOMExists)) {
                    map.put("EndItem", "Yes");
                    sListEndItemId.remove(objectId);
                }
                if ("Unresolved".equals(getStringValue(map, POLICY_CLASSIFICATION))) {
                    map.put("RowEditable", "readonly");
                    map.put("disableSelection", "true");
                }
                listReturn.add(map);
            }
        }

        for (int i = 0; i < iEbomSubListSize; i++) {
            map = (Map) ebomSubList.get(i);

            if (isFNAndRefDesFilterPassed(map, refDesFilter, fnFilter)) {
                strLevel = getStringValue(map, "level");

                map.put("objectLevel", strLevel);
                map.put("relationship", RELATIONSHIP_EBOM_SUBSTITUE);

                listReturn.add(map);
            }
        }

        if (boolAddEndItemsToList) {
            for (int i = 0; i < iEndItemListSize; i++) {
                map = (Map) endItemList.get(i);
                objectId = getStringValue(map, DomainConstants.SELECT_ID);

                if (sListEndItemId.contains(objectId) && isFNAndRefDesFilterPassed(map, refDesFilter, fnFilter)) {
                    if ("Unresolved".equals(getStringValue(map, POLICY_CLASSIFICATION))) {
                        map.put("RowEditable", "readonly");
                        map.put("disableSelection", "true");
                    }

                    map.put("EndItem", "Yes");
                    listReturn.add(map);
                }
            }
        }

        for (int i = 0; i < iSpareSubAltSize; i++) {
            map = (Map) spareSubAltPartList.get(i);

            strLevel = getStringValue(map, "level");
            map.put("objectLevel", strLevel);

            listReturn.add(map);
        }

        return listReturn;
    }

    /**
     * getDataForThisKey method
     * @param list
     * @param key
     * @return Added this method for TIGTK-6787 by Harika Varanasi
     */
    private StringList getDataForThisKey(MapList list, String key) {
        int size = list == null ? 0 : list.size();

        StringList listReturn = new StringList(size);

        String strTemp;

        for (int i = 0; i < size; i++) {
            strTemp = (String) ((Map) list.get(i)).get(key);
            if (!isValidData(strTemp)) {
                strTemp = "";
            }
            listReturn.addElement(strTemp);
        }

        return listReturn;
    }

    /**
     * If user has entered some valid value in Ref Des OR Findnumber it compares with attributes exists in map and if both are same only then it returns true, if both findNumber, refDes == NUll or "",
     * then this method returns true.
     * @param map
     *            contains properties of object like id, name, rel attributes, object attributes.
     * @param refDes
     *            value given by user from UI in Reference Designator textfield.
     * @param findNumber
     *            value given by user from UI in Find Number textfield.
     * @return boolean. Added this method for TIGTK-6787 by Harika Varanasi
     */
    private boolean isFNAndRefDesFilterPassed(Map map, String refDes, String findNumber) {
        boolean boolRefDesFilterPass = true;
        boolean boolFNFilterPass = true;

        String strRefDes = getStringValue(map, DomainConstants.SELECT_ATTRIBUTE_REFERENCE_DESIGNATOR);
        String strFindNumber = getStringValue(map, DomainConstants.SELECT_ATTRIBUTE_FIND_NUMBER);

        if (isValidData(refDes) && !refDes.equals(strRefDes)) {
            boolRefDesFilterPass = false;
        }

        if (isValidData(findNumber) && !findNumber.equals(strFindNumber)) {
            boolFNFilterPass = false;
        }

        return (boolRefDesFilterPass && boolFNFilterPass);
    }

    /**
     * addRowEditableToList Method
     * @param list
     *            Added this method for TIGTK-6787 by Harika Varanasi
     */
    private void addRowEditableToList(MapList list) {
        Map map;
        for (Iterator itr = list.iterator(); itr.hasNext();) {
            map = (Map) itr.next();
            map.put("RowEditable", "false");
        }
    }

    // TIGTK-6787 | 08/06/2017 | Harika Varanasi : Starts
    // DIVERSITY :TIGTK-6829 : TS : 07-06-2017 : START
    public MapList getVariantsProductConfigurationForEBOM(Context context, String args[]) throws Exception {
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        String strPartId = (String) paramMap.get("objectId");
        StringList selectStmts = new StringList(2);
        selectStmts.addElement(DomainConstants.SELECT_ID);
        StringList relSelectStmts = new StringList(2);
        relSelectStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
        DomainObject domPartObject = DomainObject.newInstance(context, strPartId);
        Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_PARTVARIANTASSEMBLY);
        relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_VARIANT_ASSEMBLY_PRODUCT_CONFIGURATION);

        Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_VARIANTASSEMBLY);
        typePattern.addPattern(TigerConstants.TYPE_PRODUCTCONFIGURATION);
        Pattern typePostPattern = new Pattern(TigerConstants.TYPE_PRODUCTCONFIGURATION);

        MapList mlProductConfigurations = domPartObject.getRelatedObjects(context, relationshipPattern.getPattern() // String relationshipPattern
                , typePattern.getPattern() // String typePattern
                , selectStmts // StringList objectSelects
                , relSelectStmts // StringList relationshipSelects
                , false // boolean getTo
                , true // boolean getFrom
                , (short) 2 // short recurseToLevel
                , null // String objectWhere
                , null, (short) 0, false // checkHidden
                , true // preventDuplicates
                , (short) 1000 // pageSize
                , typePostPattern, null, null, null, null);
        if (mlProductConfigurations.size() == 0) {
            String strLanguage = context.getSession().getLanguage();
            String strAlertMessage = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", new Locale(strLanguage), "PSS_emxEngineeringCentral.Alert.NoVariantAssembly");
            strAlertMessage = MessageFormat.format(strAlertMessage, domPartObject.getInfo(context, DomainConstants.SELECT_NAME));
            emxContextUtil_mxJPO.mqlNotice(context, strAlertMessage);
        }
        return mlProductConfigurations;
    }
    // DIVERSITY :TIGTK-6829 : TS : 07-06-2017 : END

    // TIGTK-8445 : PKH:Start
    /***
     * this method gives the status column in EBOM and Reference EBOM tab if new Revision of that Part is exists.
     * @param context
     * @param args
     * @return StringList
     * @throws Exception
     * @author PKH
     */
    public StringList getReplaceRevisionStatus(Context context, String[] args) throws Exception {
        try {
            StringList slReturnList = new StringList();
            // Get object list information from packed arguments
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");

            for (Iterator itrTableRows = objectList.iterator(); itrTableRows.hasNext();) {
                Map mapObjectInfo = (Map) itrTableRows.next();
                String strIconLink = DomainConstants.EMPTY_STRING;
                String strID = (String) mapObjectInfo.get(DomainConstants.SELECT_ID);
                String strLatestRevisionObjectId = DomainObject.EMPTY_STRING;
                String strRevisionStatus = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(),
                        "PSS_emxEngineeringCentral.tooltip.HigherRevisionExists");
                String strReplacementStatus = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(),
                        "PSS_emxEngineeringCentral.tooltip.ReplacementpartExists");
                DomainObject domCurrentObject = DomainObject.newInstance(context, strID);
                BusinessObject busLatestObjectRevisionObject = domCurrentObject.getLastRevision(context);
                if (busLatestObjectRevisionObject.exists(context)) {
                    strLatestRevisionObjectId = busLatestObjectRevisionObject.getObjectId();
                }
                if (!strID.equals(strLatestRevisionObjectId)) {
                    strIconLink = "<img src='../common/images/PSS_iconSmallHigherRevisionRed.gif' alt='" + strRevisionStatus + "' title='" + strRevisionStatus + "'/>";

                } else if (strID.equals(strLatestRevisionObjectId)) {
                    StringList lstselectStmts = new StringList();
                    StringList lstrelStmts = new StringList();

                    lstselectStmts.addElement(DomainConstants.SELECT_ID);
                    String strRELWhere = DomainObject.getAttributeSelect(TigerConstants.ATTRIBUTE_DERIVED_CONTEXT) + " == 'PSS_FOR_REPLACE'";

                    MapList mlConnectedPartsList = domCurrentObject.getRelatedObjects(context, RELATIONSHIP_DERIVED, DomainConstants.TYPE_PART, lstselectStmts, lstrelStmts, false, true, (short) 1,
                            null, strRELWhere, 0);
                    int intConnectedPartsListSize = mlConnectedPartsList.size();

                    if (intConnectedPartsListSize > 0) {
                        strIconLink = "<img src='../common/images/PSS_iconSmallHigherRevisionRed.gif' alt='" + strReplacementStatus + "' title='" + strReplacementStatus + "'/>";
                    }
                } else {
                    strIconLink = " ";
                }

                slReturnList.add(strIconLink);
            }
            return slReturnList;

        } catch (Exception e) {
            logger.error("Error in getReplaceRevisionStatus: ", e);
            throw e;
        }

    }

    // TIGTK-8445 : PKH:End

    // TIGTK-6827:PKH:Phase-2.0:Start
    /**
     * Method to get the Variant Assembly.
     * @param context
     * @param args
     * @return MapList - Contains the Variant Assembly.
     * @throws Exception
     */
    public MapList getVariantAssemblyForEBOM(Context context, String args[]) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String strobjectId = (String) programMap.get("objectId");
        StringList selectStmts = new StringList(1);
        selectStmts.addElement(DomainConstants.SELECT_ID);
        selectStmts.addElement(DomainConstants.SELECT_NAME);
        StringList relStmts = new StringList(1);
        relStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

        DomainObject domObjPart = DomainObject.newInstance(context, strobjectId);
        MapList mlobjMapList = domObjPart.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_PARTVARIANTASSEMBLY, // relationship pattern
                TigerConstants.TYPE_PSS_VARIANTASSEMBLY, // object pattern
                selectStmts, // object selects
                relStmts, // relationship selects
                false, // to direction
                true, // from direction
                (short) 1, // recursion level
                null, // object where clause
                null, 0); // relationship where clause
        return mlobjMapList;

    }
    // TIGTK-6827:PKH:Phase-2.0:End

    /**
     * Method to promote Previous Revision To Obsolete State.
     * @param context
     * @param args
     *            @TIGTK-8743 :Added on 23/06/2017 :by SIE
     * @throws Exception
     */

    public void promotePreviousRevToObsoleteState(Context context, String[] args) throws Exception {
        try {
            String objectId = args[0];
            String OBSOLETE = PropertyUtil.getSchemaProperty(context, "policy", TigerConstants.POLICY_PSS_DEVELOPMENTPART, "state_Obsolete");
            DomainObject domPart = new DomainObject(objectId);
            BusinessObject boPreviousRevision = domPart.getPreviousRevision(context);
            if (boPreviousRevision.exists(context)) {
                // Getting previous revision ID
                String strPreviousRevID = boPreviousRevision.getObjectId(context);
                DomainObject domOldPart = new DomainObject(strPreviousRevID);
                String strCurrentState = (String) domOldPart.getInfo(context, DomainConstants.SELECT_CURRENT);
                String strObjectPolicy = domOldPart.getInfo(context, DomainConstants.SELECT_POLICY);
                if ((strObjectPolicy.equals(TigerConstants.POLICY_PSS_DEVELOPMENTPART)) && !strCurrentState.equals(OBSOLETE)) {
                    domOldPart.promote(context);
                }
            }
        } catch (Exception ex) {
            logger.error("Error in promotePreviousRevToCancelledState: ", ex);
            throw ex;
        }
    }

    // CAD-BOM: PHASE2.0 : TIGTK-8281 : PSE : 15-07-2017 : START
    /**
     * Method to restrict connection/disconnection of Part/CAD under DEV Part when Dev part is in "In Review" or "Released" state
     * @param context
     * @param args
     *            From object id, Relationship name , Event Name
     * @return int
     * @throws Exception
     * @since : 15-07-2017
     * @author psalunke
     */
    public int checkIfParentIsInWork(Context context, String[] args) throws Exception {
        int chkTriggerResult = 0;
        try {
            // TIGTK-16271:25-07-2018:START
            String isCADMassAlreadySet = PropertyUtil.getGlobalRPEValue(context, "PSS_Dev_Complete");
            if ("TRUE".equalsIgnoreCase(isCADMassAlreadySet)) {
                return chkTriggerResult;
            } else {
                // TIGTK-16271:25-07-2018:END
                String strFromObjectId = args[0];
                String strRelationshipName = args[1];
                String strEventName = args[2];
                if (UIUtil.isNotNullAndNotEmpty(strFromObjectId)) {
                    DomainObject domFromObject = DomainObject.newInstance(context, strFromObjectId);
                    // Get the value of the Policy & State from the FromSIde DomainObject
                    StringList slObjectSelects = new StringList(2);
                    slObjectSelects.addElement(DomainConstants.SELECT_POLICY);
                    slObjectSelects.addElement(DomainConstants.SELECT_CURRENT);
                    Map<String, String> mFromObjectInfoMap = domFromObject.getInfo(context, slObjectSelects);
                    String strFromObjectPolicy = mFromObjectInfoMap.get(DomainConstants.SELECT_POLICY);
                    String strFromObjectState = mFromObjectInfoMap.get(DomainConstants.SELECT_CURRENT);
                    // If FromSide Object policy is Development Part Policy and its state is NOT In Work (Create), then throw message.
                    if (strFromObjectPolicy.equals(TigerConstants.POLICY_PSS_DEVELOPMENTPART) && !strFromObjectState.equals(TigerConstants.STATE_DEVELOPMENTPART_CREATE)) {

                        StringBuffer sbErrorBuffer = new StringBuffer();
                        sbErrorBuffer.append("PSS_emxEngineeringCentral.Validation.DEVPartInWork.");
                        sbErrorBuffer.append(strEventName);
                        sbErrorBuffer.append(".");
                        sbErrorBuffer.append(strRelationshipName.replace(" ", "_"));

                        String strErrorMessage = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(), sbErrorBuffer.toString());

                        String strStatus = context.getCustomData("PSS_THROW_EXCEPTION");
                        if (UIUtil.isNotNullAndNotEmpty(strStatus) && strStatus.equals("TRUE")) {
                            throw new Exception(strErrorMessage);
                        } else {
                            emxContextUtil_mxJPO.mqlNotice(context, strErrorMessage);
                            chkTriggerResult = 1;
                        }
                    }

                }
            }
        } catch (Exception e) {
            logger.error("Error in PSS_emxPart : checkIfParentIsInWork() : ", e);
            throw e;
        }
        return chkTriggerResult;
    }

    // CAD-BOM: PHASE2.0 : TIGTK-8281 : PSE : 15-07-2017 : END

    // CAD-BOM : TIGTK-8860 : PSE : 04-07-2017 : START
    /**
     * Method to synchronize the Mass attribute on Symmetrical Part
     * @param context
     * @param args
     *            Current Part Id , New attribute vale and Attribute name
     * @return int
     * @throws Exception
     * @since : 04-07-2017
     * @author psalunke
     */
    public int synchronizeClassificationMassOfSymmetricParts(Context context, String[] args) throws Exception {
        int synchronizemassstatus = 0;
        final String SELECT_ATTRIBUTE_PSS_SYMMETRICALPARTSIDENTICALMASS = "attribute[" + TigerConstants.ATTRIBUTE_PSS_SYMMETRICALPARTSIDENTICALMASS + "]";

        String strobjectId = args[0];
        String strCurrentPartMassValue = args[1];
        String strCurrentMassAttributeName = args[2];
        int intsize = 0;
        StringList slSelectStmts = new StringList(1);
        slSelectStmts.addElement(DomainConstants.SELECT_ID);
        StringList slSelectRelStmts = new StringList(1);
        slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
        slSelectRelStmts.addElement(SELECT_ATTRIBUTE_PSS_SYMMETRICALPARTSIDENTICALMASS);

        DomainObject domPart = new DomainObject(strobjectId);

        // Getting the list of Symmetrical Parts for Current Part
        MapList mlSymmetricalPartsList = domPart.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART, DomainConstants.TYPE_PART, slSelectStmts, slSelectRelStmts, true, true,
                (short) 1, null, null, 0);
        intsize = mlSymmetricalPartsList.size();

        if (intsize > 0) {
            Map mapSymmetricalPartMap = (Map) mlSymmetricalPartsList.get(0);
            String strPSS_SymmetricalPartsIdenticalMassValue = (String) mapSymmetricalPartMap.get(SELECT_ATTRIBUTE_PSS_SYMMETRICALPARTSIDENTICALMASS);

            // Check for "PSS_SymmetricalPartsIdenticalMass" Attribute value
            if (strPSS_SymmetricalPartsIdenticalMassValue.equals(TigerConstants.ATTRIBUTE_PSS_SYMMETRICALPARTSIDENTICALMASS_RANGEE_YES)) {
                String strSymmetricalPartId = (String) mapSymmetricalPartMap.get(DomainConstants.SELECT_ID);
                DomainObject domSymmetricalPart = new DomainObject(strSymmetricalPartId);

                // Get attribute map of symmetrical part
                Map mpSymmetricalAttributes = domSymmetricalPart.getAttributeMap(context);
                if (mpSymmetricalAttributes.containsKey(strCurrentMassAttributeName)) {
                    String strSymmetricalPartMass = (String) mpSymmetricalAttributes.get(strCurrentMassAttributeName);
                    // Make mass attribute value of symmetrical part as current part
                    if (!(strCurrentPartMassValue.equals(strSymmetricalPartMass))) {
                        domSymmetricalPart.setAttributeValue(context, strCurrentMassAttributeName, strCurrentPartMassValue);
                    }
                }
            }
        }
        return synchronizemassstatus;
    }
    // CAD-BOM : TIGTK-8860 : PSE : 04-07-2017 : END

    // TIGTK-8886 - PTE - 2017-07-7 - START
    /*
     * To create the part object from create component
     * 
     * @param context
     * 
     * @param args
     * 
     * @return Map
     * 
     * @throws Exception
     * 
     * @Since R211
     */
    @com.matrixone.apps.framework.ui.CreateProcessCallable
    public Map revisePartJPO(Context context, String[] args) throws Exception {
        try {
            context.setCustomData("PSS_PART_REVISE", "TRUE");
            HashMap programMap = (HashMap) JPO.unpackArgs(args);

            String strPartId = (String) programMap.get("copyObjectId");
            String sCustomRevisionLevel = (String) programMap.get("CustomRevisionLevel");
            String sVault = (String) programMap.get("lastRevVault");

            Map returnMap = new HashMap();

            Part part = new Part(strPartId);
            DomainObject nextRev = new DomainObject(part.revisePart(context, sCustomRevisionLevel, sVault, true, false));
            returnMap.put("id", nextRev.getId(context));

            return returnMap;
        } finally {
            context.setCustomData("PSS_PART_REVISE", "FALSE");
            context.removeFromCustomData("PSS_PART_REVISE");
        }
    }
    // TIGTK-8886 - PTE - 2017-07-7 - END

    // TIGTK-8512 - 2017-07-10 - VP - START
    /***
     * this method Creates the Multiple Parts for the selected CAD based on command Input from the Connect CAD to BOM. TIGTK 8512
     * @param context
     * @param args
     * @return Vector
     * @throws Exception
     * @author VP
     */
    public MapList createMultiplePartsForCADToBOM(Context context, String[] args) throws Exception {
        MapList mlCreatedPartInfo = new MapList();
        try {

            HashMap programMap = (HashMap) JPO.unpackArgs(args);

            String strSelectedPolicyForPart = (String) programMap.get("SelectedPolicyForPart");
            String strKeepCADNameForPartName = (String) programMap.get("KeepCADNameForPartName");
            String[] strArrayPartObjectIds = (String[]) programMap.get("PartObjectId");
            String[] strArrayTableRowIds = (String[]) programMap.get("emxTableRowId");

            String strSelectedTableRow = DomainObject.EMPTY_STRING;
            String[] strArraySelectedRowValues;
            String strSelectionIndex = DomainObject.EMPTY_STRING;
            int nSelectedIndex = 0;
            String strCADObjectId = DomainObject.EMPTY_STRING;
            String strPartObjectName = DomainObject.EMPTY_STRING;

            String strRiskInCreationOfPart = EngineeringUtil.i18nStringNow(context, "emxEngineeringCentral.Alert.PSS_RiskInCreationOfPart", context.getSession().getLanguage());
            StringBuilder sbErrorMessage = new StringBuilder();
            Map mapCADPartMapping = new HashMap();
            for (int i = 0; i < strArrayTableRowIds.length; i++) {
                strSelectedTableRow = strArrayTableRowIds[i];

                strArraySelectedRowValues = strSelectedTableRow.split("[|]", -1);
                strSelectionIndex = strArraySelectedRowValues[0];
                strCADObjectId = strArraySelectedRowValues[1];
                nSelectedIndex = Integer.parseInt(strSelectionIndex);

                if (UIUtil.isNullOrEmpty(strArrayPartObjectIds[nSelectedIndex])) {
                    if (strKeepCADNameForPartName.equals("Yes")) {
                        DomainObject domCADObject = DomainObject.newInstance(context, strCADObjectId);
                        String strCADName = domCADObject.getInfo(context, DomainConstants.SELECT_NAME);

                        java.util.regex.Pattern regex1 = java.util.regex.Pattern.compile("(([0-9]{7})[X|x])(.*)");
                        java.util.regex.Pattern regex3 = java.util.regex.Pattern.compile("([0-9a-zA-Z])(.*)");
                        java.util.regex.Pattern regex2 = java.util.regex.Pattern.compile("(([0-9]{7}))(.*)");

                        Matcher regexMatcher = null;
                        regexMatcher = regex1.matcher(strCADName);

                        if (regexMatcher.find()) {
                            strPartObjectName = regexMatcher.group(1);
                        } else {
                            regexMatcher = regex2.matcher(strCADName);
                            if (regexMatcher.find()) {
                                strPartObjectName = regexMatcher.group(1);
                            } else {
                                regexMatcher = regex3.matcher(strCADName);
                                if (regexMatcher.find()) {
                                    strPartObjectName = regexMatcher.group(1);
                                } else {
                                    strPartObjectName = strCADName;
                                }
                            }
                        }

                        if (isNumeric(strPartObjectName)) {
                            double dPartObjectName = Double.parseDouble(strPartObjectName);
                            if (dPartObjectName < 3000000) {
                                if (UIUtil.isNullOrEmpty(sbErrorMessage.toString())) {
                                    sbErrorMessage.append(strRiskInCreationOfPart).append("\n \n");
                                }
                                sbErrorMessage.append(strPartObjectName).append(", ");
                                continue;
                            }
                        } else {
                            if (UIUtil.isNullOrEmpty(sbErrorMessage.toString())) {
                                sbErrorMessage.append(strRiskInCreationOfPart).append("\n \n");
                            }
                            sbErrorMessage.append(strPartObjectName).append(", ");
                            continue;
                        }
                    } else {
                        String strNumberGeneratorType = TigerConstants.TYPE_PART;
                        String strObjectGeneratorName = FrameworkUtil.getAliasForAdmin(context, DomainConstants.SELECT_TYPE, strNumberGeneratorType, true);
                        String strAutoNumberSeries = DomainConstants.EMPTY_STRING;
                        strPartObjectName = DomainObject.getAutoGeneratedName(context, strObjectGeneratorName, strAutoNumberSeries);
                    }
                    DomainObject domPartObject = new DomainObject();
                    if (!mapCADPartMapping.containsKey(strCADObjectId)) {
                        domPartObject.createObject(context, TigerConstants.TYPE_PART, strPartObjectName, "01", strSelectedPolicyForPart, TigerConstants.VAULT_ESERVICEPRODUCTION);
                    } else {
                        domPartObject = new DomainObject((String) mapCADPartMapping.get(strCADObjectId));
                    }

                    StringList slPartInfo = new StringList();
                    slPartInfo.addElement(DomainConstants.SELECT_ID);
                    slPartInfo.addElement(DomainConstants.SELECT_NAME);
                    slPartInfo.addElement(DomainConstants.SELECT_REVISION);

                    Map mapPartInfo = (Map) domPartObject.getInfo(context, slPartInfo);
                    if (!mapCADPartMapping.containsKey(strCADObjectId))
                        mapCADPartMapping.put(strCADObjectId, mapPartInfo.get(DomainConstants.SELECT_ID));

                    Map mapPartInfoMap = new HashMap();
                    mapPartInfoMap.put("FieldNumber", strSelectionIndex);
                    mapPartInfoMap.put("CADObjectId", strCADObjectId);
                    mapPartInfoMap.put("PartObjectId", mapPartInfo.get(DomainConstants.SELECT_ID));
                    mapPartInfoMap.put("PartObjectName", mapPartInfo.get(DomainConstants.SELECT_NAME));
                    mapPartInfoMap.put("PartObjectRevision", mapPartInfo.get(DomainConstants.SELECT_REVISION));

                    mlCreatedPartInfo.add(mapPartInfoMap);
                }
            }
            if (UIUtil.isNotNullAndNotEmpty(sbErrorMessage.toString())) {
                String strErrorMessage = sbErrorMessage.toString();
                strErrorMessage = strErrorMessage.substring(0, strErrorMessage.length() - 2);
                emxContextUtil_mxJPO.mqlNotice(context, strErrorMessage);
            }
        } catch (Exception ex) {
            logger.error("Error in createMultiplePartsForCADToBOM: ", ex);
            emxContextUtil_mxJPO.mqlNotice(context, ex.getMessage());
            throw ex;
        }
        return mlCreatedPartInfo;
    }

    /***
     * this method check whether entered String is Numeric or not. TIGTK 8512
     * @param str
     * @return boolean
     * @throws Exception
     * @author VP
     */
    public boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");
    }

    /***
     * this method gives the classification Path of the Part into Connect CAD to BOM. TIGTK 8512
     * @param context
     * @param args
     * @return Vector
     * @throws Exception
     * @author VP
     */
    public Vector<String> getClassificationPathObjectNameCADToBOM(Context context, String[] args) throws Exception {
        try {
            Vector<String> vClassificationPathObjectNames = new Vector<String>();

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");

            String strSearchURL = "../common/emxFullSearch.jsp?field=TYPES=type_PartFamily:CURRENT=policy_Classification.state_Active&selection=single&formName=emxCreateForm&submitAction=refreshCaller&suiteKey=EngineeringCentral&table=ENCAddExistingGeneralSearchResults&submitURL=../engineeringcentral/PSS_EngineeringCentralCommonProcess.jsp?PSS_mode=PSS_SearchPartFamilyForPart";

            int index = 0;
            StringBuffer outPut = null;

            Map objectMap = null;
            boolean bCheck;
            String sCADObjId = "";
            String sPartFamilyId = "";
            String sPartFamilyName = "";

            Iterator objectListItr = objectList.iterator();

            while (objectListItr.hasNext()) {
                objectMap = (Map) objectListItr.next();

                sCADObjId = (String) objectMap.get(SELECT_ID);
                sPartFamilyId = (String) objectMap.get("PartFamilyId");
                sPartFamilyName = (String) objectMap.get("PartFamilyName");

                if (UIUtil.isNullOrEmpty(sPartFamilyName)) {
                    sPartFamilyName = "";
                }

                outPut = new StringBuffer();

                outPut.append("<script language=\"javascript\" src=\"../common/scripts/emxUIModal.js\"></script>");
                outPut.append("<script language=\"javascript\" src=\"../common/scripts/emxUITableUtil.js\"></script>");

                outPut.append("<input type=\"text\" name=\"PartFamilyName\"  size=\"9\" value=\"");
                outPut.append(sPartFamilyName);
                outPut.append("\"");
                outPut.append(" >&nbsp;");

                outPut.append("<input class=\"button\" type=\"button\"");
                outPut.append(" name=\"btnClassificationPathObjectChooser\" size=\"6\" ");
                outPut.append("value=\"...\" alt=\"\"  onClick=\"javascript:showChooser('");

                outPut.append(strSearchURL);
                if (UIUtil.isNotNullAndNotEmpty(sCADObjId)) {
                    outPut.append("&objectId=" + sCADObjId + "&");
                }

                outPut.append("&FieldNumber=");
                outPut.append(index);
                outPut.append("&PSS_Index=");
                outPut.append(index);
                outPut.append("', 700, 500)\">&nbsp;&nbsp;");

                outPut.append("<input type=\"hidden\" name=\"PartFamilyId\" value=\"" + sPartFamilyId + "\"/>");
                outPut.append("<input type=\"hidden\" name=\"isFamilyConnectionRequired\" value=\"false\"/>");
                outPut.append("</NOBR>");
                vClassificationPathObjectNames.add(outPut.toString());
                index++;
            }
            return vClassificationPathObjectNames;
        } catch (Exception ex) {
            logger.error("Error in getClassificationPathObjectNameCADToBOM ", ex);
            throw ex;
        }
    }

    // TIGTK-8512 - 2017-07-10 - VP - END
    /**
     * This method returns the last released revision of the object. In case of DEV part, it checks for the Complete state For any other policies, it checks for Release state. If no previous revision
     * found for the object then it returns the objectId passed in the args.
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author MC - TIGTK-8742
     */
    public String getLastReleasedRevision(Context context, String[] args) throws Exception {
        Map paramMap = JPO.unpackArgs(args);
        String objectId = (String) paramMap.get("objectId");
        String strLastReleaseObjectId = objectId;

        StringList lstSelectStmt = new StringList();
        lstSelectStmt.addElement(DomainConstants.SELECT_CURRENT);
        lstSelectStmt.addElement(DomainConstants.SELECT_POLICY);

        try {
            BusinessObject busObj = new BusinessObject(objectId);
            while (busObj.exists(context)) {
                busObj.open(context);
                DomainObject domObj = DomainObject.newInstance(context, busObj);
                busObj.close(context);
                Map<String, String> objectMap = domObj.getInfo(context, lstSelectStmt);

                String strPolicy = objectMap.get(DomainConstants.SELECT_POLICY);
                String strObjectState = objectMap.get(DomainConstants.SELECT_CURRENT);

                // Re-checking the policy because in the case of BOM to Go to Production,
                // the part and its previous revision might have different policies.
                String strTargetState = TigerConstants.STATE_PART_RELEASE;

                if (strPolicy.equals(TigerConstants.POLICY_PSS_DEVELOPMENTPART)) {
                    strTargetState = TigerConstants.STATE_PSS_DEVELOPMENTPART_COMPLETE;
                }

                if (strTargetState.equals(strObjectState)) {
                    strLastReleaseObjectId = domObj.getObjectId(context);
                    break;
                }

                busObj = domObj.getPreviousRevision(context);
            }
        } catch (Exception ex) {
            logger.error("error in getLastReleasedRevision: ", ex);
            ex.printStackTrace();
            throw ex;
        }

        return strLastReleaseObjectId;
    }

    /**
     * Method to Copy current EBOM to Reference EBOM when the Part gets released It first deletes existing reference EBOM, if present.
     * @param context
     * @param args
     * @param args0
     *            Current Part Id
     * @return int
     * @throws Exception
     */
    public void copyEBOMToReferenceEBOM(Context context, String[] args) throws Exception {

        boolean isPushedContext = false;
        try {
            // First delete the reference EBOM of the current part
            deleteReferenceEBOM(context, args);

            // Pushing the context in beginning so that no matter what user's security context is, EBOM gets copied to Ref EBOM
            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            isPushedContext = true;

            // Copy the current EBOM to Reference EBOM
            String strPartId = args[0];
            DomainObject domPartObject = DomainObject.newInstance(context, strPartId);

            StringList lstselectStmts = new StringList(2);
            StringList lstrelStmts = new StringList(1);

            lstselectStmts.addElement(DomainConstants.SELECT_ID);

            lstrelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

            MapList mlConnectedPartsList = domPartObject.getRelatedObjects(context, RELATIONSHIP_EBOM, DomainConstants.TYPE_PART, lstselectStmts, lstrelStmts, false, true, (short) 1, null, null, 0);

            int intConnectedPartsListSize = mlConnectedPartsList.size();

            StringList slExcludeAttributeLst = new StringList();
            slExcludeAttributeLst.add(TigerConstants.ATTRIBUTE_PSS_REPLACED_WITH_LATEST_REVISION);
            slExcludeAttributeLst.add(TigerConstants.ATTRIBUTE_PSS_REFERENCE_EBOM_GENERATED);
            slExcludeAttributeLst.add(TigerConstants.ATTRIBUTE_PSS_IDENTIFICATION_NUMBER);

            for (int intIndex = 0; intIndex < intConnectedPartsListSize; intIndex++) {
                Map<String, String> mapConnectedPartsMap = (Map) mlConnectedPartsList.get(intIndex);
                String strChildPartId = (String) mapConnectedPartsMap.get(DomainConstants.SELECT_ID);
                String strRelId = (String) mapConnectedPartsMap.get(DomainConstants.SELECT_RELATIONSHIP_ID);

                DomainRelationship domTargetRel = DomainRelationship.connect(context, domPartObject, TigerConstants.RELATIONSHIP_PSS_REFERENCE_EBOM, DomainObject.newInstance(context, strChildPartId));

                MqlUtil.mqlCommand(context, "mod connection $1 add interface $2", new String[] { domTargetRel.toString(), "Effectivity Framework" });

                copySourceRelDataToNewRel(context, strRelId, domTargetRel.toString(), slExcludeAttributeLst, true);
            }
        } finally {
            if (isPushedContext)
                ContextUtil.popContext(context);
        }
    }

    /**
     * This method deletes the reference EBOM of the Part passed as the first argument It just deletes the immediate links i.e. level-1 links.
     * @param context
     * @param args
     */
    public void deleteReferenceEBOM(Context context, String args[]) throws Exception {
        boolean isPushedContext = false;
        try {
            String strPartId = args[0];
            DomainObject domPartObject = DomainObject.newInstance(context, strPartId);

            StringList lstReferenceEBOMRelIds = domPartObject.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_REFERENCE_EBOM + "].id");

            String[] strRelIds = (String[]) lstReferenceEBOMRelIds.toArray(new String[lstReferenceEBOMRelIds.size()]);

            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            isPushedContext = true;
            DomainRelationship.disconnect(context, strRelIds);
        } catch (Exception ex) {
            logger.error("Error in deleteReferenceEBOM while disconnecting Reference EBOM  Relationships: ", ex.toString());
            throw ex;
        } finally {
            if (isPushedContext)
                ContextUtil.popContext(context);
        }
    }

    // CAD-BOM : TIGTK-8742 : MC : 07-07-2017 : END

    // TIGTK-8336 - PTE - 2017-15-7 - START

    /**
     * Following Method has been added by CAD BOM stream by copying the method from emxECPartBase JPO Returns a StringList of the object ids which are connected using Part Specification Relationship
     * and objects revisions ids for a given context And CAD which are created in logged in user collab space, will be listed.
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param args
     *            contains a packed HashMap containing objectId of object
     * @return StringList.
     * @since EngineeringCentral X3
     * @throws Exception
     *             if the operation fails.
     */
    @com.matrixone.apps.framework.ui.ExcludeOIDProgramCallable
    public StringList excludeOIDPartSpecificationConnectedItems(Context context, String args[]) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String parentObjectId = (String) programMap.get("objectId");
        StringList result = new StringList();
        if (parentObjectId == null) {
            return (result);
        }
        DomainObject domObj = new DomainObject(parentObjectId);

        String strRelationship = (String) programMap.get("srcDestRelName");
        String strFieldtype = (String) programMap.get("field_actual");
        StringTokenizer strToken = new StringTokenizer(strFieldtype, ":");
        StringBuffer sbType = new StringBuffer();
        StringBuffer sbPolicy = new StringBuffer();
        while (strToken.hasMoreTokens()) {
            String strKey = strToken.nextToken();
            if (strKey.contains("TYPES=")) {
                strKey = strKey.replace("TYPES=", DomainConstants.EMPTY_STRING);
                String[] strTypeArr = strKey.split(",");
                int iTypeCount = strTypeArr.length;
                for (int i = 0; i < iTypeCount; i++) {
                    String strTypeSymbolic = strTypeArr[i];
                    sbType.append(PropertyUtil.getSchemaProperty(context, strTypeSymbolic));
                    if (!(i == iTypeCount - 1)) {
                        sbType.append(",");

                    }
                }

            } else if (strKey.contains("POLICY=")) {
                strKey = strKey.replace("POLICY=", DomainConstants.EMPTY_STRING);
                strKey = strKey.trim();
                String[] strPolicyArr = strKey.split(",");
                int iPolicyCount = strPolicyArr.length;
                for (int i = 0; i < iPolicyCount; i++) {
                    String strPolicySymbolic = strPolicyArr[i];
                    sbPolicy.append(PropertyUtil.getSchemaProperty(context, strPolicySymbolic));
                    if (!(i == iPolicyCount - 1)) {
                        sbPolicy.append(",");

                    }
                }
            }
        }
        String relToExpand = PropertyUtil.getSchemaProperty(context, strRelationship);

        StringList selectStmts = new StringList(1);
        selectStmts.addElement(DomainConstants.SELECT_ID);
        MapList mapList = domObj.getRelatedObjects(context, relToExpand, // relationship pattern
                sbType.toString(), // object pattern
                selectStmts, // object selects
                null, // relationship selects
                false, // to direction
                true, // from direction
                (short) 1, // recursion level
                null, // object where clause
                null, 0); // relationship where clause
        Iterator i1 = mapList.iterator();
        while (i1.hasNext()) {
            Map m1 = (Map) i1.next();
            String strId = (String) m1.get(DomainConstants.SELECT_ID);
            DomainObject dObj = new DomainObject(strId);

            MapList revmapList = dObj.getRevisionsInfo(context, selectStmts, new StringList());
            Iterator i2 = revmapList.iterator();
            while (i2.hasNext()) {
                Map m2 = (Map) i2.next();
                String strIds = (String) m2.get(DomainConstants.SELECT_ID);
                result.addElement(strIds);
            }
        }
        String strProjectName = PersonUtil.getDefaultProject(context, context.getUser());
        String objectWhere = "Project!=" + strProjectName + "&&" + "policy=='" + sbPolicy.toString() + "'";
        StringList slBusSelects = new StringList(DomainConstants.SELECT_ID);
        MapList mlObjList = DomainObject.findObjects(context, sbType.toString(), TigerConstants.VAULT_ESERVICEPRODUCTION, objectWhere, slBusSelects);
        Iterator itr1 = mlObjList.iterator();
        while (itr1.hasNext()) {
            Map m1 = (Map) itr1.next();
            String strId = (String) m1.get(DomainConstants.SELECT_ID);
            DomainObject dObj = new DomainObject(strId);

            MapList revmapList = dObj.getRevisionsInfo(context, selectStmts, new StringList());
            Iterator itr2 = revmapList.iterator();
            while (itr2.hasNext()) {
                Map m2 = (Map) itr2.next();
                String strIds = (String) m2.get(DomainConstants.SELECT_ID);
                if (!result.contains(strIds)) {
                    result.addElement(strIds);
                }

            }
        }
        return result;
    }

    /**
     * To include CAD created with login CS
     * @param context
     * @param args
     * @return StringList
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public StringList getCurrentContextCADObject(Context context, String[] args) throws Exception {
        StringList result = new StringList();
        try {
            Pattern typePattern = new Pattern(TigerConstants.TYPE_MCAD_MODEL);
            typePattern.addPattern(TigerConstants.TYPE_MCADDRAWING);

            String strProjectName = PersonUtil.getDefaultProject(context, context.getUser());
            String objectWhere = "Project==" + strProjectName;
            StringList slBusSelects = new StringList(DomainConstants.SELECT_ID);
            MapList mapList = DomainObject.findObjects(context, typePattern.getPattern(), TigerConstants.VAULT_ESERVICEPRODUCTION, objectWhere, slBusSelects);
            StringList selectStmts = new StringList(1);
            selectStmts.addElement(DomainConstants.SELECT_ID);
            Iterator i1 = mapList.iterator();
            while (i1.hasNext()) {
                Map m1 = (Map) i1.next();
                String strId = (String) m1.get(DomainConstants.SELECT_ID);
                DomainObject dObj = new DomainObject(strId);

                MapList revmapList = dObj.getRevisionsInfo(context, selectStmts, new StringList());
                Iterator i2 = revmapList.iterator();
                while (i2.hasNext()) {
                    Map m2 = (Map) i2.next();
                    String strIds = (String) m2.get(DomainConstants.SELECT_ID);
                    result.addElement(strIds);
                }
            }
        } catch (Exception ex) {
            throw ex;
        }
        return result;
    }

    /**
     * To include Part created with login CS
     * @param context
     * @param args
     * @return StringList
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public StringList getCurrentContextPartObject(Context context, String[] args) throws Exception {
        StringList result = new StringList();
        try {
            String strProjectName = PersonUtil.getDefaultProject(context, context.getUser());
            String objectWhere = "Project==" + strProjectName;
            StringList slBusSelects = new StringList(DomainConstants.SELECT_ID);
            MapList mapList = DomainObject.findObjects(context, DomainConstants.TYPE_PART, TigerConstants.VAULT_ESERVICEPRODUCTION, objectWhere, slBusSelects);
            Iterator i1 = mapList.iterator();
            while (i1.hasNext()) {
                Map m1 = (Map) i1.next();
                String strId = (String) m1.get(DomainConstants.SELECT_ID);
                result.addElement(strId);
            }
        } catch (Exception ex) {
            throw ex;
        }
        return result;
    }

    // TIGTK-8336 - PTE - 2017-15-7 - END

    // TIGTK-9162 - PTE - 2017-7-24 - START
    /**
     * This method is for show value Change To Release on part Property page.
     * @param context
     * @param string
     *            args
     * @throws Exception
     * @author PTE
     * @since 24-July-2017
     */
    public String getChangeToReleaseFromPart(Context context, String[] args) throws Exception {
        String strResult = "";
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap requestMap = (HashMap) programMap.get("requestMap");
            String strObjectd = (String) requestMap.get("objectId");
            String expression = DomainConstants.SELECT_NAME;
            strResult = getCOForReleaseInfoFromPart(context, strObjectd, expression, true, false);
        } catch (Exception e) {

            logger.error("Error in getChangeToReleaseFromPart: ", e);

        }
        return strResult;
    }

    // TIGTK-9162 - PTE - 2017-7-24 - END
    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public StringList excludeConnectedMaterialObjects(Context context, String args[]) throws Exception {

        StringList excludeList = new StringList();
        try {
            Map programMap = (Map) JPO.unpackArgs(args);

            String strObjectId = (String) programMap.get("objectId");
            String strRelationship = (String) programMap.get("relName");
            if (UIUtil.isNullOrEmpty(strObjectId)) {
                strObjectId = (String) programMap.get("targetMBOMid");
                strRelationship = TigerConstants.RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS;
            }

            DomainObject domObj = DomainObject.newInstance(context, strObjectId);

            String strMaterialTypes = TigerConstants.TYPE_PROCESSCONTINUOUSPROVIDE + "," + TigerConstants.TYPE_PROCESS_CONTINUOUS_CREATE_MATERIAL;
            // TIGTK-11335:Rutuja Ekatpure:16/11/2017:Start
            MapList childObjects = domObj.getRelatedObjects(context, PropertyUtil.getSchemaProperty(context, strRelationship), strMaterialTypes, new StringList(DomainConstants.SELECT_ID), null, false,
                    true, (short) 1, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);

            for (int i = 0; i < childObjects.size(); i++) {
                Map tempMap = (Map) childObjects.get(i);
                DomainObject domChildObj = DomainObject.newInstance(context, (String) tempMap.get(DomainConstants.SELECT_ID));
                StringList slChildMajorIds = domChildObj.getInfoList(context, "majorids");
                for (int revindx = 0; revindx < slChildMajorIds.size(); revindx++) {
                    String strRevId = (String) slChildMajorIds.getElement(revindx);
                    DomainObject domChildRevObj = DomainObject.newInstance(context, strRevId);
                    excludeList.add(domChildRevObj.getInfo(context, DomainConstants.SELECT_ID));
                }
            }
            // TIGTK-11335:Rutuja Ekatpure:16/11/2017:End
        } catch (Exception e) {
            logger.error("error in getLastReleasedRevision: ", e);
        }

        return excludeList;
    }

    // TIGTK-7243 : Harika Varanasi : 17-07-2017 : START
    /**
     * checkColorOptionForPartAndMaterial
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author Harika Varanasi
     */
    public int checkColorOptionForPartAndMaterial(Context context, String[] args) throws Exception {
        int chkTriggerResult = 0;
        boolean isColorNotConnected = false;
        try {
            String strObjectId = args[0];
            if (UIUtil.isNotNullAndNotEmpty(strObjectId)) {
                DomainObject domPartObject = DomainObject.newInstance(context, strObjectId);
                StringList slColorListForPart = domPartObject.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_COLORLIST + "].to.id");

                StringList lstselectStmts = new StringList(2);
                lstselectStmts.addElement(DomainConstants.SELECT_ID);
                lstselectStmts.addElement("from[" + TigerConstants.RELATIONSHIP_PSS_COLORLIST + "].to.id");
                DomainObject.MULTI_VALUE_LIST.add("from[" + TigerConstants.RELATIONSHIP_PSS_COLORLIST + "].to.id");
                StringList lstrelStmts = new StringList();
                String strWhere = "attribute[" + TigerConstants.ATTRIBUTE_PSS_PROCESSCONTINUOUSPROVIDE_PSS_MATERIALTYPE + "]==Specific";
                MapList mlMaterial = domPartObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_MATERIAL, DomainConstants.QUERY_WILDCARD, lstselectStmts, lstrelStmts, false, true,
                        (short) 1, strWhere, null, 0);
                StringList slColorListOfMaterial = new StringList();
                if (mlMaterial != null && !mlMaterial.isEmpty()) {
                    Iterator iterator = mlMaterial.iterator();
                    while (iterator.hasNext()) {
                        Map tempMap = (Map) iterator.next();
                        if (tempMap.containsKey("from[" + TigerConstants.RELATIONSHIP_PSS_COLORLIST + "].to.id")) {
                            StringList slMaterialColorList = new StringList();
                            try {
                                slMaterialColorList.addElement((String) tempMap.get("from[" + TigerConstants.RELATIONSHIP_PSS_COLORLIST + "].to.id"));
                            } catch (ClassCastException cse) {
                                slMaterialColorList = (StringList) tempMap.get("from[" + TigerConstants.RELATIONSHIP_PSS_COLORLIST + "].to.id");
                            }
                            if (slMaterialColorList != null && slMaterialColorList.size() > 0) {
                                slColorListOfMaterial.addAll(slMaterialColorList);
                            }
                        }
                    }

                    if (slColorListForPart.size() != slColorListOfMaterial.size())
                        isColorNotConnected = true;
                    else if (!slColorListForPart.isEmpty() && !slColorListOfMaterial.isEmpty()) {
                        if (slColorListForPart.size() == slColorListOfMaterial.size()) {
                            for (int j = 0; j < slColorListForPart.size(); j++) {
                                String strCurrentColorId = (String) slColorListForPart.get(j);
                                if (!slColorListOfMaterial.contains(strCurrentColorId)) {
                                    isColorNotConnected = true;
                                    break;
                                }
                            }
                        }
                    }

                    if (isColorNotConnected) {
                        String strErrorMessage = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(),
                                "PSS_emxEngineeringCentral.Alert.CheckColorOptionForPartAnMaterial");
                        MqlUtil.mqlCommand(context, "notice $1", strErrorMessage);
                        chkTriggerResult = 1;
                    }
                }
            }
        } catch (RuntimeException e) {
            logger.error("Error in PSS_emxPart : checkColorOptionForPartAndMaterial : ", e);
            throw e;
        } catch (Exception e) {
            logger.error("Error in PSS_emxPart : checkColorOptionForPartAndMaterial : ", e);
            throw e;
        }
        // chkTriggerResult = 1;
        return chkTriggerResult;
    }

    // TIGTK-7243 : Harika Varanasi : 17-07-2017 : : END

    // MBOM:PHASE2.0 : TIGTK-7247 : PSE : 19-07-2017 : START
    /**
     * Method to check Color Options of Part And Paint System Material
     * @param context
     * @param args
     * @return int
     * @throws Exception
     * @since 19-07-2017
     * @author psalunke
     */
    public int checkColorForPartAndPaintSystemMaterial(Context context, String[] args) throws Exception {
        try {

            int chkTriggerResult = 0;
            String strPartObjectID = args[0];
            if (UIUtil.isNotNullAndNotEmpty(strPartObjectID)) {
                DomainObject domPartObject = DomainObject.newInstance(context, strPartObjectID);
                // Get connected Color Options to part
                StringList slColorListForPart = domPartObject.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_COLORLIST + "]." + DomainConstants.SELECT_TO_ID);

                StringList slSelectStmts = new StringList();
                slSelectStmts.addElement(DomainConstants.SELECT_ID);
                slSelectStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_PROCESSCONTINUOUSPROVIDE_PSS_MATERIALTYPE + "]");

                StringBuffer sbWhereExpression = new StringBuffer();
                sbWhereExpression.append("attribute[");
                sbWhereExpression.append(TigerConstants.ATTRIBUTE_PSS_PROCESSCONTINUOUSPROVIDE_PSS_MATERIALTYPE);
                sbWhereExpression.append("]==Generic");

                MapList mlMaterialList = domPartObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_MATERIAL, // relationship pattern
                        TigerConstants.TYPE_PSS_PAINTSYSTEM, // object pattern
                        slSelectStmts, // object selects
                        new StringList(DomainRelationship.SELECT_ID), // relationship selects
                        false, // to direction
                        true, // from direction
                        (short) 1, // recursion level
                        sbWhereExpression.toString(), // object where clause
                        null, (short) 0, false, // checkHidden
                        true, // preventDuplicates
                        (short) 1000, // pageSize
                        null, null, null, null, null);

                StringList slColorListOfMaterial = new StringList();
                if (mlMaterialList != null && !mlMaterialList.isEmpty()) {
                    int iMaterialListSize = mlMaterialList.size();
                    for (int i = 0; i < iMaterialListSize; i++) {
                        Map mMaterialMap = (Map) mlMaterialList.get(i);
                        String strMaterialId = (String) mMaterialMap.get(DomainConstants.SELECT_ID);
                        DomainObject domMaterialObject = DomainObject.newInstance(context, strMaterialId);
                        // Get color options of material
                        StringList slColorOptionIds = domMaterialObject.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_COLORLIST + "]." + DomainConstants.SELECT_TO_ID);
                        // Store all connected materials color options in single string list
                        if (slColorOptionIds != null && !slColorOptionIds.isEmpty()) {
                            slColorListOfMaterial.addAll(slColorOptionIds);
                        }

                    }
                    boolean isColorNotConnected = false;
                    // Check that All color which are connected to part connect to Generic Paint System material or not

                    if (slColorListForPart.size() != slColorListOfMaterial.size())
                        isColorNotConnected = true;
                    else if (!slColorListForPart.isEmpty() && !slColorListOfMaterial.isEmpty()) {
                        if (slColorListForPart.size() == slColorListOfMaterial.size()) {
                            for (int j = 0; j < slColorListForPart.size(); j++) {
                                String strCurrentColorId = (String) slColorListForPart.get(j);
                                if (!slColorListOfMaterial.contains(strCurrentColorId)) {
                                    isColorNotConnected = true;
                                    break;
                                }
                            }
                        }
                    }
                    // If Parts all color options are connected to Generic Paint System material then part is promoted from InWork to Review ,If No throw alert Message and denied promotion.
                    if (isColorNotConnected) {
                        String strResult = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(),
                                "PSS_FRCMBOMCentral.Alert.CheckColorForPartAndPaintSystemMaterial");
                        MqlUtil.mqlCommand(context, "notice $1", strResult);
                        chkTriggerResult = 1;
                    }
                }
            }
            return chkTriggerResult;
        } catch (RuntimeException e) {
            logger.error("Error in PSS_emxPart : checkColorForPartAndPaintSystemMaterial() : ", e);
            throw e;
        } catch (Exception e) {
            logger.error("Error in PSS_emxPart : checkColorForPartAndPaintSystemMaterial() : ", e);
            throw e;
        }
    }

    // MBOM:PHASE2.0 : TIGTK-7247 : PSE : 19-07-2017 : END

    // TIGTK-9212:PKH:CAD-BOM:Start
    /**
     * To Show Apply link in EBOM emxTable/Indented Table based on the state of the Parent assembly Part
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param args
     *            holds objectId.
     * @return Boolean.
     * @throws Exception
     *             If the operation fails.
     * @since X+3.
     */

    public Boolean isApplyAllowed(Context context, String[] args) throws Exception {
        boolean allowApply = false;
        try {
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            String parentId = (String) paramMap.get("objectId");
            // modified as per changes for bug 311050
            // check the parent obj state
            StringList strList = new StringList(4);
            strList.add(SELECT_CURRENT);
            strList.add(SELECT_POLICY);
            strList.add(SELECT_REVISION);
            strList.add("first.revision");

            DomainObject domObj = new DomainObject(parentId);
            Map map = domObj.getInfo(context, strList);

            String objState = (String) map.get(SELECT_CURRENT);
            String objPolicy = (String) map.get(SELECT_POLICY);
            String objRev = (String) map.get(SELECT_REVISION);
            String objFirstRev = (String) map.get("first.revision");

            String propAllowLevel = null;

            matrix.db.Access mAccess = domObj.getAccessMask(context);
            // 371781 - modified the if else condition to check for PolicyClassification instead of Policy
            String policyClassification = EngineeringUtil.getPolicyClassification(context, objPolicy);
            if (mAccess.has(Access.cModify)) {
                if ("Production".equalsIgnoreCase(policyClassification)) {
                    propAllowLevel = (String) FrameworkProperties.getProperty(context, "emxEngineeringCentral.PSS_EC_Part.AllowApply");
                } else if ("Development".equalsIgnoreCase(policyClassification)) {
                    propAllowLevel = (String) FrameworkProperties.getProperty(context, "emxEngineeringCentral.PSS_Development_Part.AllowApply");
                }

                String propAllowLevelList = DomainConstants.EMPTY_STRING;

                if (propAllowLevel != null && !"null".equals(propAllowLevel) && propAllowLevel.length() > 0) {
                    StringTokenizer stateTok = new StringTokenizer(propAllowLevel, ",");
                    while (stateTok.hasMoreTokens()) {
                        String tok = (String) stateTok.nextToken();
                        propAllowLevelList = PropertyUtil.getSchemaProperty(context, "policy", objPolicy, tok);
                    }
                }
                allowApply = propAllowLevelList.contains(objState);

                // 371781 - Modified to check for PolicyClassification instead of Policy
                if (allowApply && "Production".equalsIgnoreCase(policyClassification)) {
                    String strIsVersion = domObj.getInfo(context, "attribute[" + PropertyUtil.getSchemaProperty(context, "attribute_IsVersion") + "]");

                    if ((!objRev.equals(objFirstRev) || "TRUE".equals(strIsVersion)) && (!(DomainConstants.STATE_PART_PRELIMINARY).equals(objState))) {
                        allowApply = false;
                    }
                }
            }
            // end of changes
        } catch (Exception e) {
            logger.error("Error in PSS_emxPart : isApplyAllowed() : ", e);
            throw e;
        }

        return Boolean.valueOf(allowApply);
    }
    // TIGTK-9212:PKH:CAD-BOM:End

    public TicketWrapper getDownloadPackage(Context paramContext, String[] args) throws Exception {
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        String partId = (String) paramMap.get("partId");
        String[] fileObjs = (String[]) paramMap.get("fileObjs");
        String selectedlevel = (String) paramMap.get("selectedlevel");
        String errorPage = (String) paramMap.get("errorPage");
        String strDownloadDateTime = (String) paramMap.get("strDownloadDateTime");
        HttpServletRequest request = (HttpServletRequest) paramMap.get("request");
        HttpServletResponse response = (HttpServletResponse) paramMap.get("response");
        boolean bIncBOMStructure = (boolean) paramMap.get("bIncBOMStructure");
        // TIGTK-14543 - Start
        String strArchiveName = (String) paramMap.get("treeLabel");
        // getDownloadPackage(paramContext, String paramString1, String[] paramArrayOfString, String paramString2, String paramString3, String paramString4, HttpServletRequest paramHttpServletRequest,
        // HttpServletResponse paramHttpServletResponse, boolean paramBoolean)

        return getDownloadPackage(paramContext, partId, fileObjs, selectedlevel, strDownloadDateTime, errorPage, request, response, bIncBOMStructure, strArchiveName);
        // TIGTK-14543 - End
    }

    public TicketWrapper getDownloadPackage(Context paramContext, String partID, String[] paramArrayOfString, String paramString2, String paramString3, String paramString4,
            HttpServletRequest paramHttpServletRequest, HttpServletResponse paramHttpServletResponse, boolean paramBoolean, String strArchiveName) throws Exception {
        String[] fileData = paramArrayOfString;
        boolean isWindows = ImageConversionUtil.isWindows();
        String strworkSpace = null;
        String strPartPath = null;
        char c = '\t';
        String strTab = String.valueOf(c);
        Vector vecData = new Vector();
        Vector vecIDS = new Vector();
        Vector vecFileNames = new Vector();
        Vector vecFileFormats = new Vector();
        Vector vecFilePaths = new Vector();
        Vector vecBoolean = new Vector();
        Vector vecCounts = new Vector();
        Vector vecVersionIDs = new Vector();

        String str4 = null;

        TicketWrapper localTicketWrapper = null;

        try {
            Object slSelect = new StringList(5);
            ((StringList) slSelect).add("name");
            ((StringList) slSelect).add("type");
            ((StringList) slSelect).add("revision");
            ((StringList) slSelect).add("current");
            ((StringList) slSelect).add("description");

            DomainObject domobjPart = new DomainObject(partID);
            Map mapPartInfo = domobjPart.getInfo(paramContext, (StringList) slSelect);

            String strPartName = (String) mapPartInfo.get("name");

            if (strPartName.indexOf("/") != -1) {
                strPartName = StringUtils.replaceAll(strPartName, "/", "-");
            }

            String strRev = (String) mapPartInfo.get("revision");

            strworkSpace = paramContext.createWorkspace();
            strPartPath = strworkSpace + File.separator + strPartName;
            File flFolder = new File(strPartPath);

            boolean isDirectoryCreated = flFolder.mkdirs();
            if (!isDirectoryCreated) {
                logger.error("Error in getDownloadPackage: ", flFolder.getName());
            }
            StringList slUniqueFiles = new StringList();
            if (fileData != null) {
                String strParentPartPath = "";
                for (int i = 0; i < fileData.length; i++) {

                    StringTokenizer localObject2 = new StringTokenizer(fileData[i], strTab);

                    if (((StringTokenizer) localObject2).hasMoreTokens()) {
                        String filePath = ((StringTokenizer) localObject2).nextToken();

                        if ((isWindows) && (((String) filePath).indexOf("/") != -1)) {
                            filePath = StringUtils.replaceAll((String) filePath, "/", "_");
                        }

                        String strIds = ((StringTokenizer) localObject2).nextToken();

                        String strFileFormat = ((StringTokenizer) localObject2).nextToken();

                        String strFileName = ((StringTokenizer) localObject2).nextToken();
                        // TIGTK-14276 : START
                        if (slUniqueFiles.contains(strFileName))
                            continue;
                        else
                            slUniqueFiles.add(strFileName);

                        // TIGTK-14276 : END
                        Object strLevel = ((StringTokenizer) localObject2).hasMoreTokens() ? ((StringTokenizer) localObject2).nextToken() : "";
                        Object strVersionID = ((StringTokenizer) localObject2).hasMoreTokens() ? ((StringTokenizer) localObject2).nextToken() : "";

                        StringTokenizer arrIds = new StringTokenizer(strIds, "|");

                        String strParentPartID = null;
                        Object strDerivedOutputID = null;
                        if (((StringTokenizer) arrIds).hasMoreTokens()) {
                            strParentPartID = ((StringTokenizer) arrIds).nextToken();
                            strDerivedOutputID = ((StringTokenizer) arrIds).nextToken();
                        }

                        String strData = strParentPartID + "|" + (String) strDerivedOutputID + "|" + (String) strFileName;

                        if (((String) strLevel).equals("1"))
                            strFileName = "";
                        vecData.addElement(strData);
                        vecIDS.addElement(strDerivedOutputID);
                        vecFileNames.addElement(strFileName);
                        vecFileFormats.addElement(strFileFormat);

                        StringTokenizer folderStructure = new StringTokenizer(filePath, File.separator);
                        int count = folderStructure.countTokens();
                        String path = "";
                        if (i == 0) {
                            for (int cnt = 0; cnt < count - 1; cnt++) {
                                if (count == 1 || (count != 1 && cnt == count - 2)) {
                                    path = folderStructure.nextToken();
                                } else {
                                    path = folderStructure.nextToken() + File.separator;
                                }
                            }
                            filePath = path;
                            strParentPartPath = path;

                        } else {
                            filePath = strParentPartPath;
                        }
                        if ((paramBoolean) && (((String) strLevel).equals("0"))) {

                            vecFilePaths.addElement(filePath);
                        } else if (((String) strLevel).equals("0")) {

                            String strTempPath = filePath;
                            int k = ((String) strTempPath).lastIndexOf(File.separator);
                            String strShortPath = ((String) strTempPath).substring(k + File.separator.length());
                            String strName = ((String) strTempPath).substring(0, k);
                            int n = strName.lastIndexOf(File.separator);
                            String strIntermediate = strName.substring(n + File.separator.length());
                            String strFinal = (String) strIntermediate + File.separator + (String) strShortPath;

                            vecFilePaths.addElement(strFinal);
                        } else {
                            vecFilePaths.addElement("");
                        }

                        vecBoolean.addElement("false");
                        vecCounts.addElement(strLevel);
                        vecVersionIDs.addElement(strVersionID);
                    }
                }
            }

            int m = vecIDS.size();
            String strZipName = "";
            // TIGTK-14543 : TIGTK-18262- Start
            if (UIUtil.isNotNullAndNotEmpty(strArchiveName)) {
                String strCurrentDate = DomainConstants.EMPTY_STRING;
                SimpleDateFormat formatter = new SimpleDateFormat("MM-dd-yyyy_hh-mm-ss_a");
                Calendar calender = Calendar.getInstance();
                strCurrentDate = formatter.format(calender.getTime());
                StringBuilder sbPackage = new StringBuilder(64);
                String strArchivePackage = sbPackage.append("Download_").append(strArchiveName).append("_").append(strCurrentDate).append(".zip").toString();
                // strArchivePackage = "\""+strArchivePackage+"\"";

                strZipName = strArchivePackage;
            } else
                strZipName = strPartName + "_" + strRev + ".zip";
            // TIGTK-14543 : TIGTK-18262 - End
            String[] strIDS = new String[m];
            String[] strFileNames = new String[m];
            String[] strFormats = new String[m];
            String[] strPaths = new String[m];
            String[] strBoolean = new String[m];
            String[] strVersionIds = new String[m];
            long[] lnData = new long[m];

            String strCount = "";

            for (int i2 = 0; i2 < m; i2++) {

                strIDS[i2] = ((String) vecIDS.elementAt(i2));

                strFileNames[i2] = ((String) vecFileNames.elementAt(i2));

                strFormats[i2] = ((String) vecFileFormats.elementAt(i2));

                strPaths[i2] = ((String) vecFilePaths.elementAt(i2));

                strBoolean[i2] = ((String) vecBoolean.elementAt(i2));

                strVersionIds[i2] = ((String) vecVersionIDs.elementAt(i2));

                strCount = (String) vecCounts.elementAt(i2);

                if ((strCount == null) || (((String) strCount).equals("")))
                    strCount = "0";
                lnData[i2] = Long.parseLong(strCount);

            }

            localTicketWrapper = HttpVcCheckout.doIt(paramContext, (String[]) strIDS, (String[]) strFileNames, (String[]) strFormats, strBoolean, (long[]) lnData, (String[]) strVersionIds,
                    (String[]) strPaths, true, strZipName, paramString4, paramHttpServletRequest, paramHttpServletResponse);

            File localFile = null;

            if (strPartPath != null) {
                localFile = new File(strPartPath);
                if ((((File) localFile).exists()) && (!((File) localFile).delete())) {
                    ((File) localFile).deleteOnExit();
                }
            }

            if (strworkSpace != null) {
                localFile = new File(strworkSpace);
                if ((((File) localFile).exists()) && (!((File) localFile).delete())) {
                    ((File) localFile).deleteOnExit();
                }
            }
        } catch (Exception localException) {
            logger.error("Error in getDownloadPackage: ", localException);
            ContextUtil.abortTransaction(paramContext);
            throw new FrameworkException(localException);

        } finally {
            File localFile = null;

            if (strPartPath != null) {
                localFile = new File(strPartPath);
                if ((localFile.exists()) && (!localFile.delete())) {
                    localFile.deleteOnExit();
                }
            }

            if (strworkSpace != null) {
                localFile = new File(strworkSpace);
                if ((localFile.exists()) && (!localFile.delete())) {
                    localFile.deleteOnExit();
                }
            }
        }
        return localTicketWrapper;
    }

    // TIGTK-8368:CAD-BOM:PKH:Start
    public MapList getAddExistingPart(Context context, String[] args) throws Exception {
        MapList mlFinalPartList = new MapList();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strObjectIdList = (String) programMap.get("selectedParts");
            StringList slObjectList = FrameworkUtil.split(strObjectIdList, "|");
            for (int j = 0; j < slObjectList.size(); j++) {
                String strObjectID = (String) slObjectList.get(j);
                if (UIUtil.isNotNullAndNotEmpty(strObjectID)) {
                    Map mParts = new HashMap();
                    mParts.put(DomainObject.SELECT_ID, strObjectID);
                    mlFinalPartList.add(mParts);
                }

            }
        } catch (Exception e) {
            logger.error("Error in PSS_emxPart : getAddExistingPart: ", e);
            throw e;
        }
        return mlFinalPartList;
    }

    public Vector<String> getFindNumber(Context context, String[] args) throws Exception {

        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        MapList objectList = (MapList) programMap.get("objectList");
        Vector fnValuesVector = new Vector();

        try {
            Map objectMap;
            Iterator iterator = objectList.iterator();
            while (iterator.hasNext()) {
                objectMap = (Map) iterator.next();

                String strId = (String) objectMap.get("id");

                DomainObject domObjectPart = DomainObject.newInstance(context, strId);

                // TIGTK-16182 : 06-08-2018 : START
                StringList slPartFNList = domObjectPart.getInfoList(context, "from[" + DomainConstants.RELATIONSHIP_EBOM + "].attribute[" + DomainConstants.ATTRIBUTE_FIND_NUMBER + "]");
                String strFindNumber = DomainConstants.EMPTY_STRING;
                if (!slPartFNList.isEmpty()) {
                    strFindNumber = getNextFindNumber(slPartFNList);
                }
                // TIGTK-16182 : 06-08-2018 : END

                if (UIUtil.isNotNullAndNotEmpty(strFindNumber)) {
                    StringBuffer outPut = new StringBuffer();
                    outPut.append("<input type=\"textbox\" id=\"findNumber\"  value=\"" + strFindNumber + "\" name=\"findNumber\" onChange=\"javascript:validateFN(this.value)\" />");
                    outPut.append("");
                    fnValuesVector.addElement(outPut.toString());
                } else {
                    StringBuffer outPut = new StringBuffer();
                    outPut.append("<input type=\"textbox\" id=\"findNumber\" value=\"\" name=\"findNumber\" onChange=\"javascript:validateFN(this.value)\" />");
                    outPut.append("");
                    fnValuesVector.addElement(outPut.toString());
                }
            }

        } catch (Exception e) {
            logger.error("Error in PSS_emxPart : getFindNumber: ", e);
            throw e;
        }
        return fnValuesVector;

    }

    public Vector<String> getRefDesignator(Context context, String[] args) throws Exception {

        Vector vRefDesValuesVector = new Vector();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");
            Iterator iterator = objectList.iterator();

            Map objectMap;
            while (iterator.hasNext()) {
                objectMap = (Map) iterator.next();

                String strId = (String) objectMap.get("id");

                DomainObject domObjectPart = DomainObject.newInstance(context, strId);
                String strRefDesValue = domObjectPart.getInfo(context, "from[" + RELATIONSHIP_EBOM + "].attribute[" + ATTRIBUTE_REFERENCE_DESIGNATOR + "]");

                if (UIUtil.isNotNullAndNotEmpty(strRefDesValue)) {
                    StringBuffer outPut = new StringBuffer();
                    outPut.append("<input type=\"textbox\" id=\"RefDes\" value=\"" + strRefDesValue + "\" name=\"RefDes\" />");
                    outPut.append("");
                    vRefDesValuesVector.addElement(outPut.toString());
                } else {
                    StringBuffer outPut = new StringBuffer();
                    outPut.append("<input type=\"textbox\" id=\"RefDes\" value=\"\" name=\"RefDes\" />");
                    outPut.append("");
                    vRefDesValuesVector.addElement(outPut.toString());
                }
            }
        } catch (Exception e) {
            logger.error("Error in PSS_emxPart : getRefDesignator: ", e);
            throw e;
        }
        return vRefDesValuesVector;
    }

    public MapList getReplaceParts(Context context, String[] args) throws Exception {
        MapList mlFinalPartList = new MapList();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strObjectIdList = (String) programMap.get("selectedPartsForModify");
            StringList slObjectList = FrameworkUtil.split(strObjectIdList, "~");
            for (int j = 0; j < slObjectList.size(); j++) {
                String strRowID = (String) slObjectList.get(j);

                if (UIUtil.isNotNullAndNotEmpty(strRowID)) {
                    StringList slObjectIds = FrameworkUtil.split(strRowID, "|");
                    String strObjectID = (String) slObjectIds.get(1);
                    if (UIUtil.isNotNullAndNotEmpty(strObjectID)) {
                        Map mParts = new HashMap();
                        mParts.put(DomainObject.SELECT_ID, strObjectID);
                        mlFinalPartList.add(mParts);
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Error in PSS_emxPart : getRefDesignator: ", e);
            throw e;
        }
        return mlFinalPartList;
    }

    public Vector<String> getQuantity(Context context, String[] args) throws Exception {

        Vector vQuantityValuesVector = new Vector();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");

            Iterator iterator = objectList.iterator();

            Map objectMap;
            int index = 0;
            while (iterator.hasNext()) {
                objectMap = (Map) iterator.next();

                String strId = (String) objectMap.get("id");

                DomainObject domObjectPart = DomainObject.newInstance(context, strId);
                String strQuantityValue = domObjectPart.getInfo(context, "from[" + RELATIONSHIP_EBOM + "].attribute[" + ATTRIBUTE_QUANTITY + "]");

                if (UIUtil.isNotNullAndNotEmpty(strQuantityValue)) {
                    StringBuffer outPut = new StringBuffer();
                    // TIGTK-12456 :START
                    outPut.append("<input type=\"textbox\" id=\"" + strId + "\" value=\"" + strQuantityValue + "\" name=\"Quantity\"  onChange=\"javascript:validateQuantity(this.value,this.id" + ","
                            + index + ")\" />");
                    // TIGTK-12456 :END
                    outPut.append("");
                    vQuantityValuesVector.addElement(outPut.toString());
                } else {
                    StringBuffer outPut = new StringBuffer();
                    // TIGTK-12456 :START
                    outPut.append("<input type=\"textbox\" id=\"" + strId + "\" value=\"\" name=\"Quantity\"  onChange=\"javascript:validateQuantity(this.value,this.id" + "," + index + ")\" />");
                    // TIGTK-12456 :END
                    outPut.append("");
                    vQuantityValuesVector.addElement(outPut.toString());
                }
                index++;
            }
        } catch (Exception e) {
            logger.error("Error in PSS_emxPart : getQuantity: ", e);
            throw e;
        }

        return vQuantityValuesVector;
    }

    public Vector<String> getHasManufacturingSubstitute(Context context, String[] args) throws Exception {
        Vector vHasSubstituteValuesVector = new Vector();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");

            Iterator iterator = objectList.iterator();

            Map objectMap;
            while (iterator.hasNext()) {
                objectMap = (Map) iterator.next();
                String strAttribute = "Has Manufacturing Substitute";
                String strId = (String) objectMap.get("id");

                DomainObject domObjectPart = DomainObject.newInstance(context, strId);
                String strManufacturingValue = domObjectPart.getInfo(context, "from[" + RELATIONSHIP_EBOM + "].attribute[" + TigerConstants.ATTRIBUTE_HASMANUFACTURINGSUBSTITUTE + "]");

                if (UIUtil.isNotNullAndNotEmpty(strManufacturingValue)) {
                    StringBuffer outPut = new StringBuffer();
                    outPut.append("<input type=\"textbox\" id=\"ManufacturingSubstitute\" value=\"" + strManufacturingValue
                            + "\" name=\"ManufacturingSubstitute\"  onChange=\"javascript:validateMAnufacturingSubstitute(this.value)\" />");
                    outPut.append("");
                    vHasSubstituteValuesVector.addElement(outPut.toString());
                } else {
                    StringBuffer outPut = new StringBuffer();
                    outPut.append(
                            "<input type=\"textbox\" id=\"ManufacturingSubstitute\" value=\"\" name=\"ManufacturingSubstitute\"  onChange=\"javascript:validateMAnufacturingSubstitute(this.value)\" />");
                    outPut.append("");
                    vHasSubstituteValuesVector.addElement(outPut.toString());
                }
            }

        } catch (Exception e) {
            logger.error("Error in PSS_emxPart : getHasManufacturingSubstitute: ", e);
            throw e;
        }
        return vHasSubstituteValuesVector;
    }
    // TIGTK-8368:CAD-BOM:PKH:End

    // TIGTK-10212 : PSE : START
    /**
     * Method to copy source parts charted drawing objects to cloned part
     * @param context
     * @param strSourceObjectId
     *            - Source object Id which is to be cloned
     * @param strClonedObjectId
     *            - Cloned Object Id generated for the Source Object
     * @return void
     * @throws Exception
     * @author psalunke
     * @since 09-10-2017
     */
    public void copySourcePartChartedDrawingsToClonedPart(Context context, String strSourceObjectId, String strClonedObjectId) throws Exception {
        logger.error("PSS_emxPart : copySourcePartChartedDrawingsToClonedPart() : START");
        try {
            if (UIUtil.isNotNullAndNotEmpty(strSourceObjectId) && UIUtil.isNotNullAndNotEmpty(strClonedObjectId)) {
                DomainObject domSourcePartObj = DomainObject.newInstance(context, strSourceObjectId);
                DomainObject domClonedPartObj = DomainObject.newInstance(context, strClonedObjectId);
                MapList mlChartedDrawingObjects = domSourcePartObj.getRelatedObjects(context, // context
                        TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING, // relationship
                        // pattern
                        DomainConstants.QUERY_WILDCARD, // object pattern
                        new StringList(DomainConstants.SELECT_ID), // object selects
                        new StringList(DomainRelationship.SELECT_ID), // relationship selects
                        false, // to direction
                        true, // from direction
                        (short) 1, // recursion level
                        DomainConstants.EMPTY_STRING, // object where clause
                        DomainConstants.EMPTY_STRING, // relationship where clause
                        (short) 0, false, // checkHidden
                        true, // preventDuplicates
                        (short) 1000, // pageSize
                        null, null, null, null);
                for (int i = 0; i < mlChartedDrawingObjects.size(); i++) {
                    Map mpChartedDrawingObject = (Map) mlChartedDrawingObjects.get(i);
                    String strChartedDrawingObjectId = (String) mpChartedDrawingObject.get(DomainConstants.SELECT_ID);
                    DomainObject domChartedDrawingObject = DomainObject.newInstance(context, strChartedDrawingObjectId);
                    DomainRelationship.connect(context, domClonedPartObj, TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING, domChartedDrawingObject);
                }
            }
            logger.error("PSS_emxPart : copySourcePartChartedDrawingsToClonedPart() : END");
        } catch (Exception e) {
            logger.error("Error in PSS_emxPart : copySourcePartChartedDrawingsToClonedPart() : ", e);
            throw e;
        }

    }

    // TIGTK-10212 : PSE : END
    /**
     * Method to get EBOM relId when Part Specification relationship removed during CAD to BOM and BOM to CAD
     * @param context
     * @param domToObject
     *            - Source part object Id which is to be removed
     * @param strCADRelId
     *            - RelId between Parent CAD and Cad object to be disconnected
     * @return void
     * @throws Exception
     * @author PTE
     * @since 10-12-2017
     */
    public String getEBOMRelFromCADRel(Context context, DomainObject domToObject, String strCADRelId) throws Exception {
        String strEBOMRelID = DomainConstants.EMPTY_STRING;
        try {

            DomainRelationship domRel = DomainRelationship.newInstance(context, strCADRelId);
            String strRelUUIDValue = (String) domRel.getAttributeValue(context, TigerConstants.ATTRIBUTE_RELATIONSHIPUUID);

            SelectList slRelSelectStmts = new SelectList();
            slRelSelectStmts.addElement(SELECT_RELATIONSHIP_ID);

            String strRelWhere = "attribute[" + TigerConstants.ATTRIBUTE_RELATIONSHIPUUID + "] == '" + strRelUUIDValue + "'";

            MapList mlObjects = domToObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_EBOM, DomainConstants.TYPE_PART, DomainConstants.EMPTY_STRINGLIST, slRelSelectStmts, true, false,
                    (short) 0, "", strRelWhere, 0);
            for (Iterator iterator = mlObjects.iterator(); iterator.hasNext();) {
                Map mObject = (Map) iterator.next();
                strEBOMRelID = (String) mObject.get(SELECT_RELATIONSHIP_ID);

            }

        } catch (Exception ex) {

            logger.error("Error in getEBOMRelFromCADRel: ", ex);

        }
        return strEBOMRelID;
    }

    /**
     * Method to reset attribute on EBOM relId when Part Specification relationship removed during CAD to BOM and BOM to CAD
     * @param context
     * @param strEBOMId
     *            - relId to be update
     * @param slAttrList
     *            - Attribute to be reset =* @return void
     * @throws Exception
     * @author PTE
     * @since 10-12-2017
     */
    public void resetRelAttrValue(Context context, String strEBOMId, StringList slAttrList) throws Exception {

        try {
            DomainRelationship domainRelationship = DomainRelationship.newInstance(context, strEBOMId);
            for (int i = 0; i < slAttrList.size(); i++) {
                String strAttrname = (String) slAttrList.getElement(i);
                domainRelationship.setAttributeValue(context, strAttrname, DomainConstants.EMPTY_STRING);
            }
        } catch (Exception ex) {
            logger.error("Error in resetRelAttrValue()\n", ex);
            throw ex;
        }
    }

    /**
     * Method to display part name with red color when effectivity contains other configuration features and options than current context Product
     * @param context
     * @param args[]
     * @return List
     * @throws Exception
     * @author psalunke
     * @since 24-10-2017 for TIGTK-9432
     */

    public StringList colorPartNameWithDifferentEffectivity(Context context, String args[]) throws Exception {
        try {
            logger.error("Error in PSS_emxPart : displayPartWithDifferentEffectivity() : START");

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap paramList = (HashMap) programMap.get("paramList");
            String strProductId = (String) paramList.get("productId");
            MapList mlObjectList = (MapList) programMap.get("objectList");

            StringList slPartList = new StringList();
            // Get CF-CO of current context Product
            if (UIUtil.isNotNullAndNotEmpty(strProductId)) {
                StringList selectStmts = new StringList();
                selectStmts.addElement(DomainConstants.SELECT_ID);
                StringList relSelectStmts = new StringList();
                relSelectStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
                relSelectStmts.addElement(DomainConstants.SELECT_FROM_ID);
                relSelectStmts.addElement(TigerConstants.SELECT_PHYSICALID);
                // Rel Pattern
                Pattern relPattern = new Pattern(TigerConstants.RELATIONSHIP_CONFIGURATION_FEATURE);
                relPattern.addPattern(TigerConstants.RELATIONSHIP_CONFIGURATION_OPTION);

                // Type Pattern
                Pattern typePattern = new Pattern(TigerConstants.TYPE_CONFIGURATIONFEATURE);
                typePattern.addPattern(TigerConstants.TYPE_CONFIGURATIONOPTION);

                Pattern typePostPattern = new Pattern(TigerConstants.TYPE_CONFIGURATIONOPTION);

                DomainObject domProductObj = DomainObject.newInstance(context, strProductId);
                MapList mlProductsConfigurationFeaturesList = domProductObj.getRelatedObjects(context, relPattern.getPattern(), // relationship
                        typePattern.getPattern(), // types to fetch from other end
                        false, // getTO
                        true, // getFrom
                        2, // recursionTo level
                        selectStmts, // object selects
                        relSelectStmts, // relationship selects
                        DomainConstants.EMPTY_STRING, // object where
                        DomainConstants.EMPTY_STRING, // relationship where
                        0, // limit
                        DomainConstants.EMPTY_STRING, // post rel pattern
                        typePostPattern.getPattern(), // post type pattern
                        null); // post patterns

                StringList slCFCOConnectionIdList = new StringList();
                if (mlProductsConfigurationFeaturesList != null && !mlProductsConfigurationFeaturesList.isEmpty()) {
                    for (int i = 0; i < mlProductsConfigurationFeaturesList.size(); i++) {
                        Map mpCFCOInfo = (Map) mlProductsConfigurationFeaturesList.get(i);
                        String strCFCOConnectionId = (String) mpCFCOInfo.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                        String strCFCoPhysicalId = (String) mpCFCOInfo.get(TigerConstants.SELECT_PHYSICALID);
                        if (UIUtil.isNotNullAndNotEmpty(strCFCOConnectionId)) {
                            slCFCOConnectionIdList.addElement(strCFCOConnectionId);
                        }
                    }
                }

                if (mlObjectList != null && !mlObjectList.isEmpty()) {
                    for (int i = 0; i < mlObjectList.size(); i++) {
                        Map mPartInfoMap = (Map) mlObjectList.get(i);
                        String strPartObjectId = (String) mPartInfoMap.get(DomainConstants.SELECT_ID);
                        String strConnectionId = (String) mPartInfoMap.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                        String strPartName = (String) mPartInfoMap.get(DomainConstants.SELECT_NAME);
                        if (UIUtil.isNullOrEmpty(strPartName)) {
                            DomainObject domPartObject = DomainObject.newInstance(context, strPartObjectId);
                            strPartName = domPartObject.getInfo(context, DomainConstants.SELECT_NAME);
                        }

                        boolean bIsRed = false;

                        if (UIUtil.isNotNullAndNotEmpty(strConnectionId)) {
                            // Get CF-CO connection id from Effectivity expression
                            Object objCFCOConnectionIds = mPartInfoMap.get(TigerConstants.SELECT_CONFIGURATIONFEATURE_OPTION_RELID_FROM_EFFECTIVITY);
                            StringList slEffectivityCFCOConnectionIds = new StringList();
                            if (objCFCOConnectionIds != null) {
                                if (objCFCOConnectionIds instanceof StringList) {
                                    slEffectivityCFCOConnectionIds = (StringList) objCFCOConnectionIds;
                                } else {
                                    slEffectivityCFCOConnectionIds.addElement((String) objCFCOConnectionIds);
                                }
                            }

                            // Check already set CF-CO are same as current context Product
                            if (slEffectivityCFCOConnectionIds != null && !slEffectivityCFCOConnectionIds.isEmpty()) {
                                for (int intCFIds = 0; intCFIds < slEffectivityCFCOConnectionIds.size(); intCFIds++) {
                                    String strCFCOConnectionId = (String) slEffectivityCFCOConnectionIds.get(intCFIds);
                                    if (!slCFCOConnectionIdList.contains(strCFCOConnectionId)) {
                                        bIsRed = true;
                                        break;
                                    }
                                }
                            }
                        }

                        StringBuffer sbBuffer = new StringBuffer();
                        if (bIsRed) {
                            sbBuffer.append("<a style=\"color: red\" href=\"javascript:emxTableColumnLinkClick('../common/emxTree.jsp?objectId=");
                        } else {
                            sbBuffer.append("<a href=\"javascript:emxTableColumnLinkClick('../common/emxTree.jsp?objectId=");
                        }

                        sbBuffer.append(strPartObjectId);
                        sbBuffer.append("', '860', '520', 'false', 'popup')\">");
                        sbBuffer.append(" " + strPartName);
                        sbBuffer.append("</a>");
                        slPartList.addElement(sbBuffer.toString());
                    }
                }
            }
            logger.debug("Error in PSS_emxPart : colorPartNameWithDifferentEffectivity() : END");
            return slPartList;
        } catch (Exception e) {
            logger.error("Error in PSS_emxPart : colorPartNameWithDifferentEffectivity()\n", e);
            throw e;
        }

    }

    // TIGTK-9432 : PSE : END

    // TIGTK-9215 ::: START
    /**
     * Method to check For PSS EBOM Mass3 Attribute Value
     * @param context
     * @param args[]
     * @return List
     * @throws Exception
     * @author
     */
    public int checkForPSSEBOMMass3AttributeValue(Context context, String[] args) throws Exception {
        logger.debug("Error in PSS_emxPart : checkForPSSEBOMMass3AttributeValue() : START");
        try {
            String strPartPolicy = args[1];
            if (TigerConstants.POLICY_PSS_DEVELOPMENTPART.equalsIgnoreCase(strPartPolicy)) {
                return 0;
            } else {
                String strPartObjectId = args[0];
                StringList strList = new StringList(DomainConstants.SELECT_ORGANIZATION);
                strList.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_EBOM_Mass3 + "]");
                strList.add(DomainConstants.SELECT_NAME);
                DomainObject domObj = DomainObject.newInstance(context, strPartObjectId);
                Map partInfo = domObj.getInfo(context, strList);
                String strAttrEBOMMass3 = (String) partInfo.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_EBOM_Mass3 + "]");
                String strPartName = (String) partInfo.get(DomainConstants.SELECT_NAME);
                Map attributeMap = domObj.getAttributeMap(context, true);
                if (!attributeMap.containsKey(TigerConstants.ATTRIBUTE_PSS_EBOM_Mass3)) {
                    return 0;
                } else {
                    String strPurposeOfRelease = getCOForReleaseInfoFromPart(context, strPartObjectId, "attribute[" + TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE + "]", false, true);
                    if (!(TigerConstants.RANGE_SERIAL_TOOL_LAUNCH_MODIFICATION.equalsIgnoreCase(strPurposeOfRelease))) {
                        return 0;
                    } else {
                        if (UIUtil.isNullOrEmpty(strAttrEBOMMass3) || "0.0".equals(strAttrEBOMMass3)) {
                            String strErrorMsg = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(), "emxEngineeringCentral.Alert.MassValueNull");
                            strErrorMsg = strErrorMsg.replace("<PART_NAME>", strPartName);
                            MqlUtil.mqlCommand(context, "notice $1", strErrorMsg);
                            return 1;
                        } else {
                            return 0;
                        }
                    }
                }

            }
        } catch (Exception e) {
            logger.error("Error in PSS_emxPart : checkForPSSEBOMMass3AttributeValue()\n", e);
            throw e;
        }
    }
    // TIGTK-9215 ::: END

    /**
     * Method to check Valid attribute range
     * @param context
     * @param args[]
     * @return int
     * @throws Exception
     * @author
     */
    public int checkValidAttrRanges(Context context, String[] args) throws Exception {
        int iReturn = 0;
        String strObjectID = args[0];
        DomainObject domObj = DomainObject.newInstance(context, strObjectID);
        Map attributeMap = domObj.getAttributeMap(context, true);

        try {
            String strParentAttrKey = "emxEngineeringCentral.PSS_DependentAttributes.ParentAttributes";
            String strParentAttr = EnoviaResourceBundle.getProperty(context, strParentAttrKey);

            StringList slParentAttr = FrameworkUtil.split(strParentAttr, ",");
            for (int i = 0; i < slParentAttr.size(); i++) {
                String strParentAttrName = (String) slParentAttr.get(i);

                String strParentRealName = PropertyUtil.getSchemaProperty(context, strParentAttrName);
                if (!attributeMap.containsKey(strParentRealName))
                    continue;

                String strChildAttrKey = "emxEngineeringCentral.PSS_DependentAttributes.ChildAttribute." + strParentAttrName;
                String strChildAttrName = EnoviaResourceBundle.getProperty(context, strChildAttrKey);
                String strChildRealName = PropertyUtil.getSchemaProperty(context, strChildAttrName);
                if (!attributeMap.containsKey(strChildRealName))
                    continue;

                String strParentAttributeValue = (String) attributeMap.get(strParentRealName);
                String strChildAttributeValue = (String) attributeMap.get(strChildRealName);

                String strValidRangesKey = "emxEngineeringCentral.PSS_DependentAttributes." + strParentAttrName.replace(" ", "_") + "." + strChildAttrName.replace(" ", "_") + "."
                        + strParentAttributeValue.replace(" ", "_") + ".ValidRanges";

                String strValidRanges = EnoviaResourceBundle.getProperty(context, strValidRangesKey);
                StringList slValidRanges = FrameworkUtil.split(strValidRanges, ",");
                if (!slValidRanges.contains(strChildAttributeValue)) {
                    String strMessage = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentral.PSS_DependentAttributes.IncompatibleValues");
                    String strLanguage = context.getSession().getLanguage();
                    strMessage = strMessage.replace("${CHILD_ATTRIBUTE_NAME}", i18nNow.getAttributeI18NString(strChildRealName, strLanguage));
                    strMessage = strMessage.replace("${CHILD_ATTRIBUTE_VALUE}", i18nNow.getRangeI18NString(strChildRealName, strChildAttributeValue, strLanguage));
                    strMessage = strMessage.replace("${PARENT_ATTRIBUTE_NAME}", i18nNow.getAttributeI18NString(strParentRealName, strLanguage));
                    strMessage = strMessage.replace("${PARENT_ATTRIBUTE_VALUE}", i18nNow.getRangeI18NString(strParentRealName, strParentAttributeValue, strLanguage));

                    MqlUtil.mqlCommand(context, "notice $1", strMessage);
                    iReturn = 1;
                    break;
                }
            }
        } catch (RuntimeException e) {
            logger.error("Error in PSS_emxPart : checkValidAttrRanges()\n", e);
            throw e;
        } catch (Exception e) {
            logger.error("Error in PSS_emxPart : checkValidAttrRanges()\n", e);
            throw e;
        }

        return iReturn;

    }

    /**
     * This method is called on part edit post process action
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @com.matrixone.apps.framework.ui.PostProcessCallable
    public HashMap partEditPostProcess(Context context, String[] args) throws Exception {
        HashMap resultMap = checkLicense(context, args);
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            Map requestMap = (Map) programMap.get("requestMap");
            String objectId = (String) requestMap.get("objectId");
            String strUOM = (String) requestMap.get("Uom");
            Part part = new Part(objectId);

            if (resultMap.get("Message") == null) {
                boolean isMFGInstalled = EngineeringUtil.isMBOMInstalled(context);
                if (isMFGInstalled) {
                    part.setEndItem(context);
                    String sPlanningReqd = (String) requestMap.get("PlanningRequired");
                    if (null != sPlanningReqd && !DomainConstants.EMPTY_STRING.equals(sPlanningReqd)) {
                        part.setPlanningReq(context, sPlanningReqd);
                    }
                    Class clazz = Class.forName("com.matrixone.apps.mbom.PartMaster");
                    IPartMaster partMaster = (IPartMaster) clazz.newInstance();
                    partMaster.updateManuRespMakeBuy(context, objectId);
                }
            }
            // TIGTK-11654:RE:15/12/2017:Start
            if (UIUtil.isNotNullAndNotEmpty(strUOM) && !"False".equalsIgnoreCase(strUOM)) {
                String[] strArryId = new String[2];
                strArryId[0] = objectId;
                strArryId[1] = strUOM;
                int iResult = checkForValidUnitofMeasure(context, strArryId);
                if (iResult == 1) {
                    Map actionMap = new HashMap<>();
                    actionMap.put("Action", "stop");
                    resultMap.putAll(actionMap);
                }
            }
            // TIGTK-11654:RE:15/12/2017:End
            String[] strArryId = new String[1];
            strArryId[0] = objectId;
            // TIGTK-11320 : START
            int iResult = checkValidAttrRanges(context, strArryId);
            if (iResult == 1) {
                Map actionMap = new HashMap<>();
                actionMap.put("Action", "stop");
                resultMap.putAll(actionMap);
            }
            // TIGTK-11320 : END
        } catch (RuntimeException e) {
            logger.error("Error in PSS_emxPart : partEditPostProcess()\n", e);
            throw e;
        } catch (Exception e) {
            logger.error("Error in PSS_emxPart : partEditPostProcess()\n", e);
            throw e;
        }
        return resultMap;
    }

    // TIGTK-11654:RE:15/12/2017:Start
    /****
     * this method used to check UOM is classified or not ,if not then UOM will be other than EA.
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public int checkForValidUnitofMeasure(Context context, String[] args) throws Exception {
        int iReturn = 0;
        boolean isAutorisedUOM = false;
        String strObjectId = args[0];
        String newVlaue = args[1];
        DomainObject domObject = DomainObject.newInstance(context, strObjectId);
        String strPartFamily = domObject.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_CLASSIFIEDITEM + "].from.name");

        String strAuthorisedPartClassification = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(),
                "emxEngineeringCentral.PartClassification.AuthorisedValue");
        String strAlertMessage = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(), "emxEngineeringCentral.UOMAttributes.InvalidRange.Alert");
        String[] slAuthorisedPartClassification = strAuthorisedPartClassification.split(",");
        if (UIUtil.isNullOrEmpty(strPartFamily) && newVlaue.equalsIgnoreCase("EA (each)")) {
            isAutorisedUOM = true;
        } else if (UIUtil.isNotNullAndNotEmpty(strPartFamily)) {
            for (int i = 0; i < slAuthorisedPartClassification.length; i++) {
                if (strPartFamily.equalsIgnoreCase(slAuthorisedPartClassification[i])) {
                    isAutorisedUOM = true;
                    break;
                }
            }
        }
        if (isAutorisedUOM == false && !newVlaue.equalsIgnoreCase("EA (each)")) {
            iReturn = 1;
            MqlUtil.mqlCommand(context, "notice $1", strAlertMessage);
        }
        return iReturn;
    }

    // TIGTK-11654:RE:15/12/2017:End
    /**
     * Method copy from emxECPartBase for JIRA -8655 Added for excluding the reference documents connected to the object. This can be used for generic purpose.
     * @param context
     * @param args
     * @return List of Object Ids
     * @throws Exception
     * @since R211
     */
    @com.matrixone.apps.framework.ui.ExcludeOIDProgramCallable
    public StringList excludeConnectedObjects(Context context, String[] args) throws Exception {

        Map programMap = (Map) JPO.unpackArgs(args);
        String strObjectIds = (String) programMap.get("objectId");
        String strRelationship = (String) programMap.get("relName");
        // Get the From side from the URL to decide on traversal
        String strFrom = (String) programMap.get("from");
        String sMode = (String) programMap.get("sMode");
        StringList excludeList = new StringList();
        String strField = (String) programMap.get("field");
        if (strField != null) {
            // get the Field value from URL param to know the types
            strField = strField.substring(strField.indexOf('=') + 1, strField.length());
            if (strField.indexOf(':') > 0) {
                strField = strField.substring(0, strField.indexOf(':'));
            }
        }
        StringList sSelectables = new StringList();
        sSelectables.add(DomainConstants.SELECT_ID);
        String sWhere = "";

        // Maplist to get the records from DB
        MapList childObjects = null;
        if (sMode != null && sMode.equals("ECRAddExisting")) {
            sWhere = "to[" + PropertyUtil.getSchemaProperty(context, "relationship_ECRSupportingDocument") + "]== TRUE";
            childObjects = DomainObject.findObjects(context, strField, TigerConstants.VAULT_ESERVICEPRODUCTION, sWhere, sSelectables);
        }

        // fix for bug IR-067474V6R2012
        // while connecting Markup to Drawing Print through Add Existing, markups which are already connected should not be displayed on the search page.

        else if (sMode != null && sMode.equals("MarkupAddExisting")) {
            sWhere = "to[" + PropertyUtil.getSchemaProperty(context, "relationship_Markup") + "]== TRUE";
            childObjects = DomainObject.findObjects(context, strField, TigerConstants.VAULT_ESERVICEPRODUCTION, sWhere, sSelectables);

        } // fix for bug TIGTK-8655
          // while connecting Part And MBOM to Tool through Add Existing,which are already connected and Cancelled and Obsolete state part should not be displayed on the search page.

        else if (sMode != null && sMode.equals("ToolAddExisting")) {

            StringBuffer whereExpression = new StringBuffer(128);
            whereExpression.append('(');
            whereExpression.append("from[");
            whereExpression.append(TigerConstants.RELATIONSHIP_PSS_PARTTOOL);
            whereExpression.append("].to.id==");
            whereExpression.append(strObjectIds);
            whereExpression.append(" || ");
            whereExpression.append('(');
            whereExpression.append("(current=='");
            whereExpression.append(TigerConstants.STATE_PSS_DEVELOPMENTPART_OBSOLETE);
            whereExpression.append("')");
            whereExpression.append(" || ");
            whereExpression.append("(current=='");
            whereExpression.append(TigerConstants.STATE_MBOM_OBSOLETE);
            whereExpression.append("')");
            whereExpression.append(" || ");
            whereExpression.append("(current=='");
            whereExpression.append(TigerConstants.STATE_PSS_MBOM_CANCELLED);
            whereExpression.append("')");
            whereExpression.append(" || ");
            whereExpression.append("(current=='");
            whereExpression.append(STATE_PART_OBSOLETE);
            whereExpression.append("')))");

            childObjects = DomainObject.findObjects(context, strField, TigerConstants.VAULT_VPLM + "," + TigerConstants.VAULT_ESERVICEPRODUCTION, whereExpression.toString(), sSelectables);

        }

        else {

            boolean bisTo = true;
            boolean bisFrom = false;
            DomainObject domObj = new DomainObject(strObjectIds);

            if (strFrom != null && strFrom.equalsIgnoreCase("true")) {
                bisTo = false;
                bisFrom = true;
            }
            childObjects = domObj.getRelatedObjects(context, PropertyUtil.getSchemaProperty(context, strRelationship), strField == null ? "*" : strField, new StringList(DomainConstants.SELECT_ID),
                    null, bisTo, bisFrom, (short) 1, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, 0);
        }
        for (int i = 0; i < childObjects.size(); i++) {
            Map tempMap = (Map) childObjects.get(i);
            excludeList.add(tempMap.get(DomainConstants.SELECT_ID));
        }
        excludeList.add(strObjectIds);
        return excludeList;
    }

    /****
     * this method used to get Quantity value in AddIn table column
     * @param context
     * @param args
     * @return
     * @throws Exception
     *             //TIGTK-12456 :START
     */
    public Vector<String> getQuantityOnAddIn(Context context, String[] args) throws Exception {
        logger.debug("PSS_emxPart : getQuantityOnAddIn : START");
        Vector vQuantity = new Vector();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");
            // TIGTK-13654 : START
            HashMap paramList = (HashMap) programMap.get("paramList");
            String strObjectId = (String) paramList.get("objectId");
            Iterator iterator = objectList.iterator();
            int index = 0;
            Map objectMap;
            while (iterator.hasNext()) {
                objectMap = (Map) iterator.next();
                String strId = (String) objectMap.get("id");
                StringBuffer outPut = new StringBuffer();
                outPut.append(
                        "<input type=\"textbox\" id=\"" + strId + "\" value=\"1.0\" name=\"Quantity\"  onChange=\"javascript:validateQuantity(this.value,'" + strObjectId + "','" + index + "')\" />");
                vQuantity.addElement(outPut.toString());
                index++;
            }
            // TIGTK-13654 : END
            logger.debug("PSS_emxPart : getQuantityOnAddIn : END");
        } catch (Exception e) {
            logger.error("Error in PSS_emxPart : getQuantityOnAddIn: ", e);
            throw e;
        }
        return vQuantity;

    }

    /**
     * This Function will return a list of Part and respective Parent Part which is selected for Mass Revise.
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param args
     *            holds objectList.
     * @throws Exception
     *             If the operation fails.
     */

    public MapList getPartsForReviseSelctedDEVParts(Context context, String[] args) throws Exception {

        MapList returnMaplist = new MapList();
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);

        String strTableRowId = (String) paramMap.get("emxTableRowId");
        StringList lstSelectedItemList = FrameworkUtil.split(strTableRowId, "~");

        final String CUSTOM_SELECTED = "CUSTOM_SELECTED";
        final String MANU_SELECTED = "MANU_SELECTED";
        final String AUTO_SELECTED = "AUTO_SELECTED";
        final String NOT_SELECTED_DIFF_POLICY = "NOT_SELECTED_DIFF_POLICY";
        final String NOT_SELECTED_DIFF_PROJECT = "NOT_SELECTED_DIFF_PROJECT";
        final String SELECTED_NOT_LAST_REVISION = "SELECTED_NOT_LAST_REVISION";
        final String CHILDREN_REUSED_PARENT = "CHILDREN_REUSED_PARENT";
        final String NOT_RELEASED_PART = "NOT_RELEASED_PART";

        StringList slObjectSelects = new StringList();
        slObjectSelects.addElement(SELECT_POLICY);
        slObjectSelects.addElement(SELECT_CURRENT);
        slObjectSelects.addElement("project");
        slObjectSelects.addElement("islast");

        String strCurrentProjName = PersonUtil.getDefaultProject(context, context.getUser());

        StringList lstReusedItems = new StringList();

        for (int i = 0; i < lstSelectedItemList.size(); i++) {
            String strSelectItem = (String) lstSelectedItemList.get(i);
            StringList lstSelectedItem = FrameworkUtil.split(strSelectItem, "|");
            String strSelectionType = (String) lstSelectedItem.get(0);
            String strRelId = (String) lstSelectedItem.get(1);
            String strObjId = (String) lstSelectedItem.get(2);
            String strParentId = (String) lstSelectedItem.get(3);
            String strLevelId = (String) lstSelectedItem.get(4);

            Map<String, String> map = new HashMap<String, String>();
            map.put(SELECT_ID, strObjId);
            map.put(SELECT_RELATIONSHIP_ID, strRelId);
            map.put(SELECT_FROM_ID, strParentId);

            Integer intLevel = ((StringList) FrameworkUtil.split(strLevelId, ",")).size() - 1;
            map.put(SELECT_LEVEL, Integer.toString(intLevel));// if level is "0,0", then level would be 1, if it is "0", then level would be 0.

            // if parent is reused, then child also to be reused.
            if (lstReusedItems.contains(strParentId)) {
                map.put(CUSTOM_SELECTED, CHILDREN_REUSED_PARENT);
            } else {
                boolean boolAllChildrenReuse = true;
                DomainObject domObj = DomainObject.newInstance(context, strObjId);
                Map<String, String> mapInfo = domObj.getInfo(context, slObjectSelects);

                String strPolicy = mapInfo.get(SELECT_POLICY);
                String strCurrentState = mapInfo.get(SELECT_CURRENT);
                String strProject = mapInfo.get("project");
                String isLastRevision = mapInfo.get("islast");

                if (TigerConstants.POLICY_PSS_ECPART.equals(strPolicy) || TigerConstants.POLICY_STANDARDPART.equals(strPolicy)) {
                    map.put(CUSTOM_SELECTED, NOT_SELECTED_DIFF_POLICY);
                } else if (!strCurrentProjName.equals(strProject)) {
                    map.put(CUSTOM_SELECTED, NOT_SELECTED_DIFF_PROJECT);
                } else if (!TigerConstants.STATE_DEVELOPMENTPART_COMPLETE.equals(strCurrentState)) {
                    map.put(CUSTOM_SELECTED, NOT_RELEASED_PART);
                    boolAllChildrenReuse = false;
                } else if (!"TRUE".equals(isLastRevision)) {
                    map.put(CUSTOM_SELECTED, SELECTED_NOT_LAST_REVISION);
                } else {
                    map.put(CUSTOM_SELECTED, strSelectionType);
                    boolAllChildrenReuse = false;
                }

                if (boolAllChildrenReuse)
                    lstReusedItems.addElement(strObjId);
            } // end of else for reused items parent

            returnMaplist.add(map);
        }

        return returnMaplist;
    }

    /**
     * Get ICON on "Selected Part to be revised" table with different ICON with tooltip to show where part is revised or reused with respective reason
     * @param context
     * @param args
     * @return Vector
     * @throws Exception
     */
    public Vector<String> getIconForReviseSelctedDEVParts(Context context, String[] args) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        MapList objectList = (MapList) programMap.get("objectList");
        Iterator objectListItr = objectList.iterator();

        final String CUSTOM_SELECTED = "CUSTOM_SELECTED";
        final String MANU_SELECTED = "MANU_SELECTED";
        final String AUTO_SELECTED = "AUTO_SELECTED";
        final String NOT_SELECTED_DIFF_POLICY = "NOT_SELECTED_DIFF_POLICY";
        final String NOT_SELECTED_DIFF_PROJECT = "NOT_SELECTED_DIFF_PROJECT";
        final String SELECTED_NOT_LAST_REVISION = "SELECTED_NOT_LAST_REVISION";
        final String CHILDREN_REUSED_PARENT = "CHILDREN_REUSED_PARENT";
        final String NOT_RELEASED_PART = "NOT_RELEASED_PART";
        final String ALWAYS_REUSE = "ALWAYS_REUSE";
        Vector<String> vIcons = new Vector<String>();
        while (objectListItr.hasNext()) {
            boolean boolAlwaysReuse = true;
            Map objectMap = (Map) objectListItr.next();
            String strObjId = (String) objectMap.get(SELECT_ID);
            String strRelId = (String) objectMap.get(SELECT_RELATIONSHIP_ID);
            String strParentId = (String) objectMap.get(SELECT_FROM_ID);
            String strCustomSelected = (String) objectMap.get(CUSTOM_SELECTED);
            String strObjProjectName = (String) objectMap.get("project");
            String strCurrentProjName = PersonUtil.getDefaultProject(context, context.getUser());

            StringBuffer output = new StringBuffer();
            output.append("<NOBR>");

            if (MANU_SELECTED.equals(strCustomSelected)) {
                output.append("&nbsp;<img src=\"../common/images/buttonDialogDone.gif\">");
                boolAlwaysReuse = false;
            } else if (AUTO_SELECTED.equals(strCustomSelected)) {
                String strToolTip = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(),
                        "PSS_emxEngineeringCentral.ReviseSelectedDevParts.ToolTip.PropagateselectionChecked");

                // putting id in the image tag so that it can be replaced everytime when the auto selection parent checkbox is toggled
                output.append("&nbsp;<img id=\"AUTO_SELECTED\" title=\"" + strToolTip + "\" src=\"../common/images/PSS_buttonDialogDoneOrange.gif\">");
                boolAlwaysReuse = false;
            } else if (SELECTED_NOT_LAST_REVISION.equals(strCustomSelected)) {
                String strToolTip = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(),
                        "PSS_emxEngineeringCentral.ReviseSelectedDevParts.ToolTip.LatestRevisionPresent");

                output.append("&nbsp;<img  title=\"" + strToolTip + "\" src=\"../common/images/PSS_iconSmallHigherRevisionOrange.gif\">");
                boolAlwaysReuse = false;
            } else {
                // To get from string resource
                String strToolTip = "";
                if (NOT_SELECTED_DIFF_POLICY.equals(strCustomSelected)) {
                    strToolTip = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(),
                            "PSS_emxEngineeringCentral.ReviseSelectedDevParts.ToolTip.ECOrSTDPart");
                } else if (NOT_SELECTED_DIFF_PROJECT.equals(strCustomSelected)) {
                    strToolTip = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(),
                            "PSS_emxEngineeringCentral.ReviseSelectedDevParts.ToolTip.DifferentCS");
                } else if (NOT_RELEASED_PART.equals(strCustomSelected)) {
                    strToolTip = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(),
                            "PSS_emxEngineeringCentral.ReviseSelectedDevParts.ToolTip.PartIsNotReleased");
                    // TIGTK-14329 : Make always reuse flag as false for In Work Parent
                    boolAlwaysReuse = false;
                } else if (CHILDREN_REUSED_PARENT.equals(strCustomSelected)) {
                    strToolTip = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(),
                            "PSS_emxEngineeringCentral.ReviseSelectedDevParts.ToolTip.ParentPartReused");
                }

                output.append("&nbsp;<img title=\"" + strToolTip + "\" src=\"../common/images/buttonDialogCancel.gif\">");
            }

            output.append("</NOBR>");

            String strHiddenFieldValue = "";
            if (boolAlwaysReuse) {
                strHiddenFieldValue = strRelId + "|" + strObjId + "|" + strParentId + "|" + ALWAYS_REUSE;
            } else {
                strHiddenFieldValue = strRelId + "|" + strObjId + "|" + strParentId + "|" + strCustomSelected;
            }

            output.append("&nbsp;<input type=\"hidden\" name=\"" + CUSTOM_SELECTED + "\" value=\"" + strHiddenFieldValue + "\"");

            vIcons.add(output.toString());
        }

        return vIcons;
    }

    public Vector<String> getActionColumnForReviseSelctedDEVParts(Context context, String[] args) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        MapList objectList = (MapList) programMap.get("objectList");
        Iterator objectListItr = objectList.iterator();

        final String CUSTOM_SELECTED = "CUSTOM_SELECTED";
        final String MANU_SELECTED = "MANU_SELECTED";
        final String AUTO_SELECTED = "AUTO_SELECTED";

        final String REVISE_ACTION = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(),
                "PSS_emxEngineeringCentral.ReviseSelectedDevParts.RowText.Revise");
        final String REUSE_ACTION = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(),
                "PSS_emxEngineeringCentral.ReviseSelectedDevParts.RowText.Reuse");

        Vector<String> vActions = new Vector<String>();
        while (objectListItr.hasNext()) {
            Map objectMap = (Map) objectListItr.next();
            String strCustomSelected = (String) objectMap.get(CUSTOM_SELECTED);

            StringBuffer output = new StringBuffer();

            if (MANU_SELECTED.equals(strCustomSelected)) {
                output.append(REVISE_ACTION);
            } else if (AUTO_SELECTED.equals(strCustomSelected)) {
                output.append("<span class=\"AUTO_SELECTED_ACTION\" style=\"color:black\">" + REVISE_ACTION + "</span>");

            } else {
                output.append(REUSE_ACTION);
            }
            vActions.add(output.toString());
        }

        return vActions;
    }

    /**
     * Method called on the post process functionality of Revise Mass Dev Part. Method makes a check whether to Re-use Or Revise in the EBOM structure.
     * @param context
     * @param args
     *            , String array containing: 1. List of selected items on UI which is to be Revised
     * @return String - Revise Object Id of the Root part in EBOM Structure.
     * @throws Exception
     */

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @com.matrixone.apps.framework.ui.PostProcessCallable
    public String postProcessForReviseSelctedDEVParts(Context context, String[] args) throws Exception {
        PropertyUtil.setGlobalRPEValue(context, "PSS_ReviseSelctedDEVParts", "TRUE");
        boolean isTrasactionActive = false;

        final String AUTO_SELECTED = "AUTO_SELECTED";
        final String ALWAYS_REUSE = "ALWAYS_REUSE";

        final String SELECTED_NOT_LAST_REVISION = "SELECTED_NOT_LAST_REVISION";
        final String NOT_RELEASED_PART = "NOT_RELEASED_PART";
        try {
            ContextUtil.startTransaction(context, true);
            isTrasactionActive = true;

            HashMap<String, String> originalReviseMap = new HashMap<String, String>();

            StringList lstExcludeAttribute = new StringList();
            lstExcludeAttribute.add(TigerConstants.ATTRIBUTE_ISVPLMVISIBLE);
            lstExcludeAttribute.add(TigerConstants.ATTRIBUTE_PSS_REPLACED_WITH_LATEST_REVISION);

            Map paramMap = JPO.unpackArgs(args);
            String[] selectedItemsArray = (String[]) paramMap.get("CUSTOM_SELECTED");

            int intSelectedItemListSize = 0;
            if (selectedItemsArray != null) {
                intSelectedItemListSize = selectedItemsArray.length;
            }

            StringList lstReusedItems = new StringList();
            StringList lstAlreadyProceedRelId = new StringList();

            String strPSS_SelectionForParent = (String) paramMap.get("PSS_SelectionForParent");

            boolean boolSelectionForParent = "TRUE".equalsIgnoreCase(strPSS_SelectionForParent) ? true : false;

            StringList sourceRelSelects = new StringList("logicalid");

            for (int intIndex = 0; intIndex < intSelectedItemListSize; intIndex++) {
                String strSelectedItem = selectedItemsArray[intIndex];
                StringList lstSelectedItem = FrameworkUtil.split(strSelectedItem, "|");
                String strRelId = EMPTY_STRING;
                String strObjId = EMPTY_STRING;
                String strParentId = EMPTY_STRING;
                String strCustomSelected = EMPTY_STRING;
                if (lstSelectedItem.size() > 3) {
                    strRelId = (String) lstSelectedItem.get(0);
                    strObjId = (String) lstSelectedItem.get(1);
                    strParentId = (String) lstSelectedItem.get(2);
                    strCustomSelected = (String) lstSelectedItem.get(3);
                } else {
                    strObjId = (String) lstSelectedItem.get(0);
                    strCustomSelected = (String) lstSelectedItem.get(2);
                }

                if (!lstAlreadyProceedRelId.contains(strRelId)) {
                    if (!lstReusedItems.contains(strParentId)) {

                        // Get logical id of the source rel
                        String strSourceRelLogicalId = DomainConstants.EMPTY_STRING;
                        if (UIUtil.isNotNullAndNotEmpty(strRelId)) {
                            DomainRelationship domSourceRel = new DomainRelationship(strRelId);
                            Hashtable hs = domSourceRel.getRelationshipData(context, sourceRelSelects);
                            StringList slSourceRelLogicalId = (StringList) hs.get("logicalid");
                            strSourceRelLogicalId = (String) slSourceRelLogicalId.get(0);

                        }
                        String strRevisedParentId = originalReviseMap.get(strParentId);

                        if (ALWAYS_REUSE.equals(strCustomSelected)) {

                            lstReusedItems.add(strObjId);
                        } else if (!boolSelectionForParent && AUTO_SELECTED.equals(strCustomSelected)) {
                            // do nothing
                            // TIGTK-14329 : For Not Release Parent Do nothing
                        } else if (NOT_RELEASED_PART.equals(strCustomSelected)) {
                            // do nothing
                        } else if (SELECTED_NOT_LAST_REVISION.equals(strCustomSelected)) {
                            // When the parent is revised, then due to replicate, the child is already replicated
                            // So, now we will replace it with latest revision child part.
                            if (UIUtil.isNotNullAndNotEmpty(strRevisedParentId)) {
                                replaceToSidePartWithLatestRevisionUsingLogicalId(context, strRevisedParentId, strObjId, strSourceRelLogicalId);
                            }
                            lstReusedItems.add(strObjId);
                        } else {
                            // revise the part and copying of the attributes
                            String strRevisedObjId = DomainObject.EMPTY_STRING;

                            if (originalReviseMap.containsKey(strObjId)) {
                                strRevisedObjId = originalReviseMap.get(strObjId);
                            } else {
                                strRevisedObjId = reviseBOMPart(context, strObjId, false);
                                originalReviseMap.put(strObjId, strRevisedObjId);
                            }

                        }

                    } // end of if for reused items check
                    else {
                        lstReusedItems.add(strObjId);// adding the current object in the reuse list so that its child can also be re-used
                    }

                    lstAlreadyProceedRelId.addElement(strRelId);
                } // end of if for already processed rel ids

            } // end of foor loop

            if (isTrasactionActive)
                ContextUtil.commitTransaction(context);

        } catch (Exception ex) {
            logger.error("Error in postProcessForReviseSelctedDEVParts: ", ex);

            if (isTrasactionActive)
                ContextUtil.abortTransaction(context);
            throw ex;

        } finally {
            PropertyUtil.setGlobalRPEValue(context, "PSS_ReviseSelctedDEVParts", "");
        }

        return "SUCCESS";
    }

    /***
     * this method revise Development part based on "Revise Selected" click.
     * @param context
     * @param strPartId
     * @param boolDisconnectEBOMRels
     * @return String
     * @throws Exception
     */
    public String reviseBOMPart(Context context, String strPartId, boolean boolDisconnectEBOMRels) throws Exception {
        DomainObject domObj = DomainObject.newInstance(context, strPartId);
        BusinessObject lastRevisionBO = domObj.getLastRevision(context);

        String strRevision = lastRevisionBO.getNextSequence(context);

        Map map = new HashMap();
        map.put("copyObjectId", strPartId);
        map.put("CustomRevisionLevel", strRevision);
        map.put("lastRevVault", domObj.getInfo(context, SELECT_VAULT));
        Map<String, String> returnMap = revisePartJPO(context, JPO.packArgs(map));
        String strRevisedObjectId = returnMap.get(SELECT_ID);

        if (boolDisconnectEBOMRels) {
            DomainObject domRevisedObj = DomainObject.newInstance(context, strRevisedObjectId);
            StringList lstEBOMRelIds = domRevisedObj.getInfoList(context, "from[" + DomainConstants.RELATIONSHIP_EBOM + "].id");
            // Disconnect EBOM Rels
            String[] strRelIds = (String[]) lstEBOMRelIds.toArray(new String[lstEBOMRelIds.size()]);
            try {
                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), "", "");
                DomainRelationship.disconnect(context, strRelIds);
            } catch (Exception ex) {
                // 10-02-2018
                logger.error("Error in reviseBOMPart while disconnecting Replicated EBOM  Relationships: ", ex.toString());
            } finally {
                ContextUtil.popContext(context);
            }
        }

        return strRevisedObjectId;
    }

    /**
     * @Description: This method called on Revise of Development part,It will replace BOM Part with latest revision In All InWork Assemblies.
     * @param context
     * @param args
     * @throws Exception
     */
    public void replaceBOMPartsInAllInWorkAssembliesForDEVParts(Context context, String[] args) throws Exception {
        boolean isTrasactionActive = false;
        boolean isPushedContext = false;
        try {
            String rpeValue = "True";
            PropertyUtil.setRPEValue(context, "PSS_ReplaceLatestRevisionPartInAllInWorkAssemblies", rpeValue, true);

            // The current method is applicable only for the DEV Parts, hence if it is other than DEV Part, then we will return from the method.
            String strOldRevPartID = args[0];
            String strPolicy = args[1];
            if (!TigerConstants.POLICY_PSS_DEVELOPMENTPART.equals(strPolicy))
                return;
            StringList lstselectStmts = new StringList(4);
            StringList lstrelStmts = new StringList(4);

            lstselectStmts.addElement(DomainConstants.SELECT_ID);

            lstrelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            lstrelStmts.addElement(DomainConstants.SELECT_FROM_ID);
            String strWhere = "current == " + TigerConstants.STATE_DEVELOPMENTPART_CREATE;
            DomainObject domOldRevPartObject = DomainObject.newInstance(context, strOldRevPartID);
            MapList mlConnectedPartsList = domOldRevPartObject.getRelatedObjects(context, RELATIONSHIP_EBOM, DomainConstants.TYPE_PART, lstselectStmts, lstrelStmts, true, false, (short) 1, strWhere,
                    null, 0);

            String strNewRevisionPartId = args[2];
            DomainObject domNewRevisePartObject = DomainObject.newInstance(context, strNewRevisionPartId);

            int intConnectedPartsListSize = mlConnectedPartsList.size();
            for (int intIndex = 0; intIndex < intConnectedPartsListSize; intIndex++) {
                Map<String, String> mapConnectedPartsMap = (Map) mlConnectedPartsList.get(intIndex);

                String relId = (String) mapConnectedPartsMap.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                DomainRelationship.setToObject(context, relId, domNewRevisePartObject);

            }
        } catch (RuntimeException e) {
            logger.error("Error in replaceBOMPartsInAllInWorkAssembliesForDEVParts: ", e);

        } catch (Exception ex) {
            logger.error("Error in replaceBOMPartsInAllInWorkAssembliesForDEVParts: ", ex);

        } finally {
            PropertyUtil.setRPEValue(context, "PSS_ReplaceLatestRevisionPartInAllInWorkAssemblies", "", true);
            if (isPushedContext) {
                ContextUtil.popContext(context);
            }
        }
    }

    public void replaceToSidePartWithLatestRevisionUsingLogicalId(Context context, String strRevisedObjectId, String strPreviousRevisionId, String strSourceRelLogicalId) throws Exception {
        DomainObject domRevisedObj = DomainObject.newInstance(context, strPreviousRevisionId);
        StringList lstEBOMRelIds = domRevisedObj.getInfoList(context,
                "from[" + DomainConstants.RELATIONSHIP_EBOM + "|to.id==" + strPreviousRevisionId + " && logicalid==" + strSourceRelLogicalId + "].id");

        DomainObject domChildObj = DomainObject.newInstance(context, strPreviousRevisionId);
        BusinessObject lastRevisionBO = domChildObj.getLastRevision(context);

        DomainObject domObjLastRevision = new DomainObject(lastRevisionBO);
        try {
            // Ideally lstEBOMRelIds length will be 1, but still running the loop, just in case more relid with same logicalid comes
            for (int i = 0; i < lstEBOMRelIds.size(); i++) {
                String relId = (String) lstEBOMRelIds.get(i);
                DomainRelationship.setToObject(context, relId, domObjLastRevision);
            }

        } catch (Exception ex) {
            logger.error("Error in replaceToSidePartWithLatestRevisionUsingLogicalId: ", ex.toString());
            throw ex;
        }
    }

    /**
     * This method is used to update the modified value of Identification Number value in EBOM system tables custom view and FAS view
     * @param context
     * @param args
     * @return void
     * @throws Exception
     * @since : 15-06-2018 TIGTK-14545
     * @author psalunke
     */
    public void updateIdentificationNumber(Context context, String[] args) throws Exception {
        logger.debug("\n PSS_emxPart : updateIdentificationNumber : START");
        try {
            Map<?, ?> mProgramMap = (Map<?, ?>) JPO.unpackArgs(args);
            Map<?, ?> mParamMap = (Map<?, ?>) mProgramMap.get("paramMap");
            String strPartRelId = (String) mParamMap.get("relId");
            if (UIUtil.isNotNullAndNotEmpty(strPartRelId)) {
                DomainRelationship domPartRel = DomainRelationship.newInstance(context, strPartRelId);
                String strNewValue = (String) mParamMap.get("New Value");
                domPartRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_IDENTIFICATION_NUMBER, strNewValue);
            }
            logger.debug("\n PSS_emxPart : updateIdentificationNumber : END");
        } catch (Exception e) {
            logger.error("Error in PSS_emxPart : updateIdentificationNumber: ", e);
            throw e;
        }

    }

    /**
     * This method is used to call check trigger of Replace Part form JSP for TIGTK-13949
     * @param context
     * @param args
     * @return void
     * @throws Exception
     */
    public String checkIfParentIsStandardOrECPartFromReplacePartJSP(Context context, String[] args) throws Exception {
        String strReturnValue = DomainConstants.EMPTY_STRING;
        try {
            int nReturn = checkIfParentIsStandardOrECPart(context, args);
            if (nReturn == 1) {
                strReturnValue = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(),
                        "PSS_emxEngineeringCentral.Validation.ConnectDevPartToECPartMessage");
            }
        } catch (Exception e) {
            logger.error("Error in PSS_emxPart : checkIfParentIsStandardOrECPartFromReplacePartJSP: ", e);
        }
        return strReturnValue;

    }

    /**
     * This Method is copied from emxECPartBase for TIGTK-14853 lookupEntries method checks the object entered manually is exists or not in the database Method to Inline create & connect new Part
     * objects in EBOM powerview IndentedTable This method is invoked on clicking on Apply button in EBOM
     * @param args
     *            String array having the object Id(s) of the part.
     * @throws FrameworkException
     *             if creation of the Part Master object fails.
     */

    @com.matrixone.apps.framework.ui.ConnectionProgramCallable
    public HashMap inlineCreateAndConnectPart(Context context, String[] args) throws Exception {
        HashMap doc = new HashMap();
        HashMap request = (HashMap) JPO.unpackArgs(args);
        HashMap paramMap = (HashMap) request.get("paramMap");
        HashMap hmRelAttributesMap;
        HashMap columnsMap;
        HashMap changedRowMap;
        HashMap returnMap;

        String sType = (String) paramMap.get("type");
        String sSymbolicName = com.matrixone.apps.framework.ui.UICache.getSymbolicName(context, sType, "type");

        Map smbAttribMap;

        Element elm = (Element) request.get("contextData");

        MapList chgRowsMapList = UITableIndented.getChangedRowsMapFromElement(context, elm);
        MapList mlItems = new MapList();

        String strRelType = (String) paramMap.get("relType");
        String parentObjectId = (String) request.get("parentOID");
        String rowFormat = "";
        String CONNECT_AS_DERIVED = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentral.ReplaceBOM.Derived");
        String vpmControlState = null;
        String sUser = context.getUser();
        String objectName = "";
        String vName = "";
        String strComponentLocation = "";
        String[] attributeKeys = { DomainConstants.ATTRIBUTE_FIND_NUMBER, DomainConstants.ATTRIBUTE_REFERENCE_DESIGNATOR, DomainConstants.ATTRIBUTE_COMPONENT_LOCATION,
                DomainConstants.ATTRIBUTE_QUANTITY, DomainConstants.ATTRIBUTE_USAGE, DomainConstants.ATTRIBUTE_NOTES };

        StringList sResultList = new StringList();
        String tokValue = "";
        String tok = "";
        String sResult = "";
        String parentBusType = "";
        BusinessType busType = null;
        if (UIUtil.isNotNullAndNotEmpty(sSymbolicName)) {
            sResult = MqlUtil.mqlCommand(context, "temp query bus $1 $2 $3 select $4 dump", "eService Object Generator", sSymbolicName, "*", "revision");
            while (UIUtil.isNullOrEmpty(sResult)) {
                busType = new BusinessType(sType, context.getVault());
                if (busType != null) {
                    parentBusType = busType.getParent(context);
                    if (UIUtil.isNotNullAndNotEmpty(parentBusType)) {
                        sType = parentBusType;
                        sSymbolicName = com.matrixone.apps.framework.ui.UICache.getSymbolicName(context, sType, "type");
                        sResult = MqlUtil.mqlCommand(context, "temp query bus $1 $2 $3 select $4 dump", "eService Object Generator", sSymbolicName, "*", "revision");
                        if (UIUtil.isNotNullAndNotEmpty(sResult)) {
                            break;
                        }
                    }
                }
            }
        }
        StringTokenizer stateTok = new StringTokenizer(sResult, "\n");
        while (stateTok.hasMoreTokens()) {
            tok = (String) stateTok.nextToken();
            tokValue = tok.substring(tok.lastIndexOf(',') + 1);
            sResultList.add(tokValue);
        }
        int sResultListSize = sResultList.size();
        StringList objAutoNameList = new StringList();

        for (int i = 0; i < sResultListSize; i++) {
            objAutoNameList.add(UINavigatorUtil.getI18nString("emxEngineeringCentral.Common." + ((String) sResultList.get(i)).replace(" ", ""), "emxEngineeringCentralStringResource", "en"));
        }

        boolean isENGSMBInstalled = EngineeringUtil.isENGSMBInstalled(context, false); // Commented for IR-213006

        if (isENGSMBInstalled) { // Commented for IR-213006
            vpmControlState = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", parentObjectId, "from[" + RELATIONSHIP_PART_SPECIFICATION + "|to.type.kindof["
                    + EngineeringConstants.TYPE_VPLM_CORE_REF + "]].to.attribute[" + EngineeringConstants.ATTRIBUTE_VPM_CONTROLLED + "]");
        }

        // ContextUtil.pushContext(context);

        try {
            DomainObject parentObj = DomainObject.newInstance(context, parentObjectId);
            DomainObject childObj;

            DomainRelationship domRelation;

            EBOMMarkup ebomMarkup = new EBOMMarkup();

            for (int i = 0, size = chgRowsMapList.size(); i < size; i++) {
                try {
                    changedRowMap = (HashMap) chgRowsMapList.get(i);

                    String childObjectId = (String) changedRowMap.get("childObjectId");
                    String sRelId = (String) changedRowMap.get("relId");
                    String sRowId = (String) changedRowMap.get("rowId");
                    rowFormat = "[rowId:" + sRowId + "]";
                    String markup = (String) changedRowMap.get("markup");
                    String strParam2 = (String) changedRowMap.get("param2");
                    // get parameters for replace operation
                    String strParam1 = (String) changedRowMap.get("param1");

                    columnsMap = (HashMap) changedRowMap.get("columns");

                    String strUOM = (String) columnsMap.get("UOM");
                    String desc = (String) columnsMap.get("Description");

                    hmRelAttributesMap = getAttributes(columnsMap, attributeKeys);
                    strComponentLocation = getStringValue(columnsMap, DomainConstants.ATTRIBUTE_COMPONENT_LOCATION);
                    if (UIUtil.isNotNullAndNotEmpty(strComponentLocation)) {
                        columnsMap.put(DomainConstants.ATTRIBUTE_COMPONENT_LOCATION, StringUtils.replace(strComponentLocation, "&", "&amp;"));
                    }
                    String vpmVisible = "";
                    // TBE
                    if (isENGSMBInstalled) { // Commented for IR-213006
                        vpmVisible = getStringValue(columnsMap, "VPMVisible");
                        // If part is not in VPM Control set the isVPMVisible value according to user selection.
                        if (isValidData(vpmVisible) && !"true".equalsIgnoreCase(vpmControlState))
                            hmRelAttributesMap.put(EngineeringConstants.ATTRIBUTE_VPM_VISIBLE, vpmVisible);
                    }
                    // TBE

                    changedRowMap.put("parentObj", parentObj);

                    if (MARKUP_ADD.equals(markup)) {
                        childObj = DomainObject.newInstance(context, childObjectId);

                        changedRowMap.put("childObj", childObj);
                        changedRowMap.put("strRelType", strRelType);

                        domRelation = ebomMarkup.connectToChildPart(context, changedRowMap);

                        if (isValidData(desc)) {
                            boolean isPushedContext = false;
                            try {

                                ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, null, null);
                                isPushedContext = true;
                                childObj.setDescription(context, desc);
                            } finally {
                                if (isPushedContext)
                                    ContextUtil.popContext(context);
                            }
                        }

                        domRelation.setAttributeValues(context, hmRelAttributesMap);

                        if ("true".equalsIgnoreCase(CONNECT_AS_DERIVED) && isValidData(strParam2)) {
                            StringList slParamObjs = childObj.getInfoList(context, SELECT_FROM_DERIVED_IDS);

                            if (slParamObjs == null || !slParamObjs.contains(strParam2)) {
                                DomainRelationship doRelDerived = DomainRelationship.connect(context, new DomainObject(strParam2), RELATIONSHIP_DERIVED, childObj);
                                doRelDerived.setAttributeValue(context, TigerConstants.ATTRIBUTE_DERIVED_CONTEXT, "Replace");
                            }
                        }

                        sRelId = domRelation.toString();
                    }

                    else if (MARKUP_NEW.equals(markup)) {

                        objectName = (String) columnsMap.get("Name");
                        String objectType = (String) columnsMap.get("Type");
                        String objectRev = (String) columnsMap.get("Revision");
                        String objectPolicy = (String) columnsMap.get("Policy");
                        String objectVault = (String) columnsMap.get("Vault");
                        String objectPartFamily = (String) columnsMap.get("Part Family");

                        smbAttribMap = new HashMap();
                        smbAttribMap.put(DomainConstants.ATTRIBUTE_UNIT_OF_MEASURE, strUOM);
                        smbAttribMap.put(DomainConstants.ATTRIBUTE_ORIGINATOR, sUser);

                        // TBE
                        if (isENGSMBInstalled) { // Commented for IR-213006
                            vName = getStringValue(columnsMap, "V_Name");
                            vName = UIUtil.isNullOrEmpty(vName) ? (String) columnsMap.get("V_Name1") : vName;
                            if (isValidData(vName))
                                smbAttribMap.put(EngineeringConstants.ATTRIBUTE_V_NAME, vName);
                            if (isValidData(vpmVisible) && !"true".equalsIgnoreCase(vpmControlState))
                                smbAttribMap.put(EngineeringConstants.ATTRIBUTE_VPM_VISIBLE, "TRUE");
                        }
                        // TBE

                        // Use Part Family for naming the Part - Start

                        if (isValidData(objectPartFamily)) {
                            PartFamily partFamilyObject = null;
                            try {
                                partFamilyObject = new PartFamily(objectPartFamily);
                                partFamilyObject.open(context);

                                if (partFamilyObject.exists(context)) {
                                    // check the "Part Family Name Generator On" attribute
                                    String usePartFamilyForName = partFamilyObject.getAttributeValue(context, ATTRIBUTE_PART_FAMILY_NAME_GENERATOR_ON);

                                    if ("TRUE".equalsIgnoreCase(usePartFamilyForName)) {
                                        objectName = partFamilyObject.getPartFamilyMemberName(context);
                                    }
                                }
                            } finally {
                                if (partFamilyObject != null) {
                                    partFamilyObject.close(context);
                                }
                            }
                        }

                        // Use Part Family for naming the Part - End

                        childObj = DomainObject.newInstance(context);
                        childObj = createchildObj(context, objectType, objectName, objectRev, objectPolicy, objectVault, childObj, objAutoNameList.contains(objectName));

                        // parts created with inline had owner - user agent. removing if condition to change owner.
                        childObj.setOwner(context, sUser);

                        if (isValidData(desc)) {
                            childObj.setDescription(context, desc);
                        }
                        childObj.setAttributeValues(context, smbAttribMap);

                        changedRowMap.put("childObj", childObj);
                        changedRowMap.put("strRelType", strRelType);

                        domRelation = ebomMarkup.connectToChildPart(context, changedRowMap);
                        domRelation.setAttributeValues(context, hmRelAttributesMap);

                        /* Connecting Part Family with Part starts */
                        if (isValidData(objectPartFamily)) {
                            DomainObject PartFamilyObj = DomainObject.newInstance(context, objectPartFamily);
                            DomainRelationship.connect(context, PartFamilyObj, DomainConstants.RELATIONSHIP_CLASSIFIED_ITEM, childObj);
                        }
                        /* Connecting Part Family with Part ends */

                        // Added RDO Convergence start
                        String strDefaultRDO = childObj.getAltOwner1(context).toString();
                        String defaultRDOId = MqlUtil.mqlCommand(context, "temp query bus $1 $2 $3 select $4 dump $5", DomainConstants.TYPE_ORGANIZATION, strDefaultRDO, "*", "id", "|");
                        defaultRDOId = defaultRDOId.substring(defaultRDOId.lastIndexOf('|') + 1);
                        DomainRelationship.connect(context, new DomainObject(defaultRDOId), // from side object Design Responsibilty
                                DomainConstants.RELATIONSHIP_DESIGN_RESPONSIBILITY, // Relationship
                                childObj);// toSide object Document

                        // Added RDO Convergence End

                        childObjectId = childObj.getId();
                        sRelId = domRelation.toString();

                    }

                    else if (MARKUP_CUT.equals(markup)) {
                        if (!"replace".equals(strParam1)) {
                            ebomMarkup.disconnectChildPart(context, changedRowMap);
                        }
                    }

                    returnMap = new HashMap();
                    returnMap.put("pid", parentObjectId);
                    returnMap.put("relid", sRelId);
                    returnMap.put("oid", childObjectId);
                    returnMap.put("rowId", sRowId);
                    returnMap.put("markup", markup);
                    objectName = (String) columnsMap.get("Name");
                    if (objectName != null && !"null".equals(objectName) && !"".equals(objectName)) {
                        columnsMap.put("Name", StringUtils.replace(objectName, "&", "&amp;"));
                    }
                    if (isENGSMBInstalled) { // Commented for IR-213006
                        vName = getStringValue(columnsMap, "V_Name");
                        if (vName != null)
                            columnsMap.put("V_Name", StringUtils.replace(vName, "&", "&amp;"));
                    }

                    returnMap.put("columns", columnsMap);

                    mlItems.add(returnMap); // returnMap having all the

                } catch (FrameworkException e) {
                    if (e.toString().indexOf("license") > -1) {
                        throw e;
                    }
                    e.addMessage(rowFormat + e);
                    throw e;
                }

            }
            doc.put("Action", "success"); // Here the action can be "Success" or
            // "refresh"
            doc.put("changedRows", mlItems);// Adding the key "ChangedRows"
        } catch (Exception e) {
            doc.put("Action", "ERROR"); // If any exception is there send "Action" as "ERROR"

            if (e.toString().indexOf("license") > -1) { // If any License Issue throw the exception to user.
                doc.put("Message", rowFormat);
                throw e;
            }

            if ((e.toString().indexOf("recursion")) != -1) {
                // Multitenant
                String recursionMesssage = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(), "emxEngineeringCentral.RecursionError.Message");
                doc.put("Message", rowFormat + recursionMesssage);
            }
            // Multitenant
            else if ((e.toString().indexOf("recursion")) == -1 && ((e.toString().indexOf("Check trigger")) != -1)) {

                String tnrMesssage = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(), "emxEngineeringCentral.TNRError.Message");
                doc.put("Message", rowFormat + tnrMesssage);
            }

            else {
                String strExcpn = e.toString();
                int j = strExcpn.indexOf(']');
                strExcpn = strExcpn.substring(j + 1, strExcpn.length());
                doc.put("Message", rowFormat + strExcpn);
            }

        } finally {
            // ContextUtil.popContext(context);
        }

        return doc;

    }

    private HashMap getAttributes(HashMap map, String[] keys) throws Exception {
        int length = length(keys);
        HashMap mapReturn = new HashMap(length);
        String data;
        for (int i = 0; i < length; i++) {
            data = getStringValue(map, keys[i]);
            if (isValidData(data)) {
                mapReturn.put(keys[i], data);
            }
        }
        return mapReturn;
    }

    /**
     * Returns a StringList of the parent object ids which are connected using EBOM Relationship for a given context.
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param args
     *            contains a packed HashMap containing objectId of object
     * @return StringList.
     * @since EngineeringCentral X3
     * @throws Exception
     *             if the operation fails.
     */
    @com.matrixone.apps.framework.ui.ExcludeOIDProgramCallable
    public StringList excludeRecursiveOIDAddExisting(Context context, String args[]) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String strPartObjectId = (String) programMap.get("objectId");
        StringList result = new StringList();
        if (strPartObjectId == null) {
            return (result);
        } else {
            result.add(strPartObjectId);
        }

        DomainObject domObj = new DomainObject(strPartObjectId);
        StringList selectStmts = new StringList(1);
        selectStmts.addElement(DomainConstants.SELECT_ID);
        MapList mapList = domObj.getRelatedObjects(context, RELATIONSHIP_EBOM, // relationship pattern
                DomainConstants.TYPE_PART, // object pattern
                selectStmts, // object selects
                null, // relationship selects
                true, // to direction
                false, // from direction
                (short) 0, // recursion level
                null, // object where clause
                null); // relationship where clause

        Iterator i1 = mapList.iterator();
        while (i1.hasNext()) {
            Map m1 = (Map) i1.next();
            String strId = (String) m1.get(DomainConstants.SELECT_ID);
            result.addElement(strId);
        }
        return result;
    }

    // TIGTK-16357 :Copied OOTB's method 06-08-2018 : START
    /* Range function to display policies in dropdown box */
    public HashMap getPolicies(Context context, String[] args) throws Exception {

        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        HashMap requestMap = (HashMap) paramMap.get("requestMap");
        String parentid = (String) requestMap.get("objectId");
        DomainObject domPartObject = DomainObject.newInstance(context, parentid);
        String parentType = (String) domPartObject.getInfo(context, DomainConstants.SELECT_TYPE);
        BusinessType partBusType = new BusinessType(parentType, context.getVault());
        PolicyList partPolicyList = partBusType.getPoliciesForPerson(context, false);
        PolicyItr partPolicyItr = new PolicyItr(partPolicyList);
        Policy partPolicy = null;
        String policyName = "";
        String policyAdminName = "";
        String policyClassification = "";

        HashMap rangeMap = new HashMap();
        StringList columnVals = new StringList();
        StringList columnVals_Choices = new StringList();

        while (partPolicyItr.next()) {
            partPolicy = partPolicyItr.obj();
            policyName = partPolicy.getName();
            policyClassification = EngineeringUtil.getPolicyClassification(context, policyName);

            // Modified for TBE Packaging & Scalability
            if (!EngineeringUtil.isENGInstalled(context, args) && !EngineeringUtil.getPolicyClassification(context, policyName).equals("Development")) {
                continue;
            }

            if ("Unresolved".equals(policyClassification) || "Equivalent".equals(policyClassification) || "Manufacturing".equals(policyClassification)) {
                continue;
            }
            policyAdminName = FrameworkUtil.getAliasForAdmin(context, "Policy", policyName, true);

            String tempPolicyName = FrameworkUtil.findAndReplace(policyName.trim(), " ", "_");
            columnVals.add(EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", context.getLocale(), "emxFramework.Policy." + tempPolicyName));

            columnVals_Choices.add(policyAdminName);
        }
        rangeMap.put("field_choices", columnVals_Choices);
        rangeMap.put("field_display_choices", columnVals);
        return rangeMap;
    }

    // TIGTK-16357 : 06-08-2018 : END
    /**
     * Checks Part create access to display in the Actions Menu.
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param args
     *            holds objectId.
     * @return Boolean.
     * @throws Exception
     *             If the operation fails.
     * @since EC10-5.
     */
    public Boolean checkCreatePartAccess(Context context, String[] args) throws Exception {
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        String objectId = (String) paramMap.get("objectId");
        String strCurrentOrganization = PersonUtil.getDefaultOrganization(context, context.getUser());
        boolean isRestrictedClassification = false;

        if (!"null".equalsIgnoreCase(objectId) && !"".equalsIgnoreCase(objectId) && objectId != null) {
            setObjectId(objectId);
            String policy = getInfo(context, DomainConstants.SELECT_POLICY);
            String policyClass = EngineeringUtil.getPolicyClassification(context, policy);
            // modified the condition to not to give create access along for following
            // "Plant","Reported", "Sub-tier" MCC parts along with Equivalent for EC
            if (policyClass.equalsIgnoreCase("Equivalent")) {
                isRestrictedClassification = true;
            } else {
                boolean mccInstall = FrameworkUtil.isSuiteRegistered(context, "appVersionMaterialsComplianceCentral", false, null, null);
                if (mccInstall) {
                    String sCompliancePlantSpecificPart = PropertyUtil.getSchemaProperty(context, "type_CompliancePlantSpecificPart");
                    String sComplianceReportedPart = PropertyUtil.getSchemaProperty(context, "type_ComplianceReportedPart");
                    String sComplianceSubtierPart = PropertyUtil.getSchemaProperty(context, "type_ComplianceSubtierPart");
                    if (isKindOf(context, sCompliancePlantSpecificPart) || isKindOf(context, sComplianceReportedPart) || isKindOf(context, sComplianceSubtierPart)) {
                        isRestrictedClassification = true;
                    }
                }
            }
        }

        boolean hasCreateAccess = false;
        BusinessType busType = new BusinessType(DomainConstants.TYPE_PART, context.getVault());
        PolicyList policyList = busType.getPoliciesForPerson(context, false);
        if (policyList.size() > 0 && !isRestrictedClassification && strCurrentOrganization.equals("FAS")) {

            hasCreateAccess = true;
        }
        return Boolean.valueOf(hasCreateAccess);
    }

    // TIGTK-16357 : 06-08-2018 : END
    /**
     * Checks Part create access to display in the Actions Menu.
     * @param args
     *            holds objectId.
     * @return Boolean.
     * @throws Exception
     *             If the operation fails.
     * @since 07-09-2018 : TIGTK-17113
     */
    public Boolean hasShowSymmetricalPart(Context context, String[] args) throws Exception {
        try {
            String strCurrentOrganization = PersonUtil.getDefaultOrganization(context, context.getUser());
            boolean hasCreateAccess = false;
            // TIGTK-17113 : 10-09-2018 : START
            if (!strCurrentOrganization.equals("FECT")) {
                // TIGTK-17113 : 10-09-2018 : END
                hasCreateAccess = true;
            }
            return Boolean.valueOf(hasCreateAccess);
        } catch (Exception ex) {
            logger.error("Error in PSS_emxPart : hasShowSymmetricalPart: ", ex);
            throw ex;
        }
    }

    /**
     * This is column JPO method used to render Display Name and revision for the Alternate Parts in EBOM powerview ALM-5572 :: Alternate Parts
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public StringList getAlternatePartDetails(Context context, String[] args) throws Exception {
        StringList slColumnValues = null;
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");
            slColumnValues = new StringList(objectList.size());

            StringList objectSelects = new StringList(3);
            objectSelects.add(DomainConstants.SELECT_ID);
            objectSelects.add(DomainConstants.SELECT_NAME);
            objectSelects.add(DomainConstants.SELECT_REVISION);

            DomainObject doPart = DomainObject.newInstance(context);
            MapList mlAlternateParts = null;
            int iAlternateCount = 0;
            String strPartOID = DomainConstants.EMPTY_STRING;
            String strAlternatePartOID = DomainConstants.EMPTY_STRING;
            String strAlternatePartName = DomainConstants.EMPTY_STRING;
            String strAlternatePartRevision = DomainConstants.EMPTY_STRING;

            Iterator itrObjectListIterator = objectList.iterator();
            while (itrObjectListIterator.hasNext()) {
                iAlternateCount = 0;
                StringBuilder sbAlternateHyperLink = new StringBuilder(512);
                Map mpObjectMap = (Map) itrObjectListIterator.next();
                strPartOID = (String) mpObjectMap.get(DomainConstants.SELECT_ID);
                doPart.setId(strPartOID);
                mlAlternateParts = doPart.getRelatedObjects(context, EngineeringConstants.RELATIONSHIP_ALTERNATE, // Relationship Pattern
                        DomainConstants.TYPE_PART, // Type Pattern
                        objectSelects, // Objects Selectables
                        null, // Relationship Selectables
                        false, // Traverse to direction
                        true, // Traverse from firection
                        (short) 1, // Recurse level
                        DomainConstants.EMPTY_STRING, // Object Where Condition
                        DomainConstants.EMPTY_STRING, // Relationship Where Condition
                        0); // result limit
                iAlternateCount = mlAlternateParts.size();
                if (iAlternateCount > 1) {
                    sbAlternateHyperLink.append("<a href=\"JavaScript:emxTableColumnLinkClick('../common/emxIndentedTable.jsp?program=emxPart:getAlternateParts");
                    sbAlternateHyperLink.append("&amp;table=ENCAlternatePartsSummary");
                    sbAlternateHyperLink.append("&amp;objectId=");
                    sbAlternateHyperLink.append(strPartOID);
                    sbAlternateHyperLink.append("&amp;header=emxEngineeringCentral.Part.AlternatePartSummaryPageHeading");
                    sbAlternateHyperLink.append("&amp;suiteKey=EngineeringCentral");
                    sbAlternateHyperLink.append("&amp;selection=multiple");
                    sbAlternateHyperLink.append("&amp;HelpMarker=emxhelppartalternate");
                    sbAlternateHyperLink.append("&amp;PrinterFriendly=true");
                    sbAlternateHyperLink.append("', '450', '300', 'true', 'popup')\">");
                    sbAlternateHyperLink.append(iAlternateCount);
                    sbAlternateHyperLink.append("</a>");
                } else if (iAlternateCount == 1) {
                    Map mpAlternatePart = (Map) mlAlternateParts.get(0);
                    sbAlternateHyperLink.append("<a href=\"javascript:showModalDialog('../common/emxTree.jsp?objectId=" + (String) mpAlternatePart.get(DomainConstants.SELECT_ID)
                            + "', '700', '500', 'false', 'popup', '' )\">" + (String) mpAlternatePart.get(DomainConstants.SELECT_NAME) + " "
                            + (String) mpAlternatePart.get(DomainConstants.SELECT_REVISION) + "</a>");
                } else {
                    sbAlternateHyperLink.append(" ");
                }
                slColumnValues.add(sbAlternateHyperLink.toString());
            }
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            throw new FrameworkException(e);
        }
        return slColumnValues;
    }

    /**
     * This is column JPO method used to render Display Name and revision for the Substitute Parts in EBOM powerview ALM-5572 :: Substitute Parts
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public StringList getSubstitutePartDetails(Context context, String[] args) throws Exception {
        StringList slColumnValues = null;
        String SELECT_EBOM_SUBSTITUTE = DomainConstants.EMPTY_STRING;
        String SELECT_EBOM_SUBSTITUTE_ID = DomainConstants.EMPTY_STRING;
        String SELECT_EBOM_SUBSTITUTE_NAME = DomainConstants.EMPTY_STRING;
        String SELECT_EBOM_SUBSTITUTE_REVISION = DomainConstants.EMPTY_STRING;
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");
            slColumnValues = new StringList(objectList.size());

            SELECT_EBOM_SUBSTITUTE = "frommid[" + EngineeringConstants.RELATIONSHIP_EBOM_SUBSTITUTE + "].to";
            SELECT_EBOM_SUBSTITUTE_ID = SELECT_EBOM_SUBSTITUTE + ".id";
            SELECT_EBOM_SUBSTITUTE_NAME = SELECT_EBOM_SUBSTITUTE + ".name";
            SELECT_EBOM_SUBSTITUTE_REVISION = SELECT_EBOM_SUBSTITUTE + ".revision";

            StringList slObjectSelects = new StringList(6);
            slObjectSelects.add(DomainConstants.SELECT_ID);
            slObjectSelects.add(DomainConstants.SELECT_TYPE);
            slObjectSelects.add(DomainConstants.SELECT_NAME);
            slObjectSelects.add(DomainConstants.SELECT_REVISION);

            StringList slRelSelects = new StringList(6);
            slRelSelects.add(SELECT_EBOM_SUBSTITUTE_NAME);
            slRelSelects.add(SELECT_EBOM_SUBSTITUTE_REVISION);
            slRelSelects.add(SELECT_EBOM_SUBSTITUTE_ID);

            DomainObject doPart = DomainObject.newInstance(context);
            StringBuffer sbSubstituteHyperLink = new StringBuffer();
            int iSubstituteCount = 0;
            String strPartOID = DomainConstants.EMPTY_STRING;
            String strSubstitutePartOID = DomainConstants.EMPTY_STRING;
            String strSubstitutePartName = DomainConstants.EMPTY_STRING;
            String strSubstitutePartRevision = DomainConstants.EMPTY_STRING;

            Iterator itrObjectListIterator = objectList.iterator();
            while (itrObjectListIterator.hasNext()) {
                iSubstituteCount = 0;
                sbSubstituteHyperLink.setLength(0);
                Map mpObjectMap = (Map) itrObjectListIterator.next();
                strPartOID = (String) mpObjectMap.get(DomainConstants.SELECT_ID);
                doPart.setId(strPartOID);

                MapList mlEBOMSubstitutes = doPart.getRelatedObjects(context, EngineeringConstants.RELATIONSHIP_EBOM, // Relationship Pattern
                        EngineeringConstants.TYPE_PART, // Type Pattern
                        slObjectSelects, // Objects Selectables
                        slRelSelects, // Relationship Selectables
                        false, // Traverse to direction
                        true, // Traverse from direction
                        (short) 1, // Recurse level
                        DomainConstants.EMPTY_STRING, // Object Where Condition
                        DomainConstants.EMPTY_STRING, // Relationship Where Condition
                        0); // result limit
                if (!mlEBOMSubstitutes.isEmpty()) {
                    Iterator itrEBOMSubstitute = mlEBOMSubstitutes.iterator();
                    while (itrEBOMSubstitute.hasNext()) {
                        Map mpSubstituteParts = (Map) itrEBOMSubstitute.next();
                        Object objSubstitute = mpSubstituteParts.get(SELECT_EBOM_SUBSTITUTE_NAME);
                        if (objSubstitute != null) {
                            if (objSubstitute instanceof String) {
                                iSubstituteCount += 1;
                                strSubstitutePartName = (String) mpSubstituteParts.get(SELECT_EBOM_SUBSTITUTE_NAME);
                                strSubstitutePartRevision = (String) mpSubstituteParts.get(SELECT_EBOM_SUBSTITUTE_REVISION);
                                strSubstitutePartOID = (String) mpSubstituteParts.get(SELECT_EBOM_SUBSTITUTE_ID);
                            } else {
                                StringList slSubstitutes = (StringList) mpSubstituteParts.get(SELECT_EBOM_SUBSTITUTE_NAME);
                                iSubstituteCount += slSubstitutes.size();
                            }
                        }
                    }
                }

                if (iSubstituteCount > 1) {
                    sbSubstituteHyperLink.append("<a href=\"javascript:emxTableColumnLinkClick('../common/emxIndentedTable.jsp?program=emxPart:getSubstitutePart");
                    sbSubstituteHyperLink.append("&amp;table=ENCSubstitutePartSummary");
                    sbSubstituteHyperLink.append("&amp;toolbar=ENCpartReviewSubstitutePartSummaryToolBar");
                    sbSubstituteHyperLink.append("&amp;objectId=");
                    sbSubstituteHyperLink.append(strPartOID);
                    sbSubstituteHyperLink.append("&amp;header=emxEngineeringCentral.Common.SubstituteInAssemblyPageHeading");
                    sbSubstituteHyperLink.append("&amp;suiteKey=EngineeringCentral");
                    sbSubstituteHyperLink.append("&amp;selection=multiple");
                    sbSubstituteHyperLink.append("&amp;sortColumnName=Name");
                    sbSubstituteHyperLink.append("&amp;sortDirection=ascending");
                    sbSubstituteHyperLink.append("&amp;HelpMarker=emxhelppartsubstitute");
                    sbSubstituteHyperLink.append("&amp;PrinterFriendly=true");
                    sbSubstituteHyperLink.append("&amp;editLink=true");
                    sbSubstituteHyperLink.append("', '450', '300', 'true', 'popup')\">");
                    sbSubstituteHyperLink.append(iSubstituteCount);
                    sbSubstituteHyperLink.append("</a>");
                } else if (iSubstituteCount == 1) {
                    sbSubstituteHyperLink.append("<a href=\"javascript:showModalDialog('../common/emxTree.jsp?objectId=" + strSubstitutePartOID + "', '700', '500', 'false', 'popup', '' )\">"
                            + strSubstitutePartName + " " + strSubstitutePartRevision + "</a>");
                } else {
                    sbSubstituteHyperLink.append(" ");
                }
                slColumnValues.add(sbSubstituteHyperLink.toString());
            }
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            throw new FrameworkException(e);
        }
        return slColumnValues;
    }

    /**
     * This is JPO method used to exclude the connected product in the search result ALM 5631UE03 - Part cannot be connected to Product from Part side
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public StringList getProductExcludeOIDs(Context context, String[] args) throws Exception {
        StringList slExcludeOIDs = new StringList();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String objectId = (String) programMap.get("objectId");
            StringList slObjSelects = new StringList(1);
            slObjSelects.add(DomainConstants.SELECT_ID);

            DomainObject domObj = DomainObject.newInstance(context, objectId);
            MapList mlConnectedProducts = domObj.getRelatedObjects(context, ConfigurationConstants.RELATIONSHIP_GBOM, // Relationship Pattern
                    ProductLineConstants.TYPE_HARDWARE_PRODUCT, // Type Pattern
                    slObjSelects, // Objects Selectables
                    new StringList(0), // Relationship Selectables
                    true, // Traverse to direction
                    false, // Traverse from direction
                    (short) 1, // Recurse level
                    DomainConstants.EMPTY_STRING, // Object Where Condition
                    DomainConstants.EMPTY_STRING, // Relationship Where Condition
                    0); // result limit
            Iterator itrProducts = mlConnectedProducts.iterator();
            while (itrProducts.hasNext()) {
                Map mpProduct = (Map) itrProducts.next();
                slExcludeOIDs.add((String) mpProduct.get(DomainConstants.SELECT_ID));
            }
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            throw new FrameworkException(e);
        }
        return slExcludeOIDs;
    }

    // TIGTK-14547 - mkakade : START
    /*
     * @param context
     * 
     * @param args
     * 
     * @throws Exception Custom method to delete old revisions of CAD
     */

    public int disconnectPreviousCADRevision(Context context, String[] args) throws Exception {
        int iReturn = 0;
        try {
            String strPartId = args[0];
            String strCADId = args[1];

            if (UIUtil.isNotNullAndNotEmpty(strPartId) && UIUtil.isNotNullAndNotEmpty(strCADId)) {
                String POLICY_PSS_Development_Part = PropertyUtil.getSchemaProperty(context, "policy_PSS_Development_Part");
                StringList slCADRevisionList = new StringList();
                DomainObject dmPart = new DomainObject(strPartId);
                StringList slSelects = new StringList(2);
                slSelects.addElement(DomainConstants.SELECT_POLICY);
                slSelects.addElement(DomainConstants.SELECT_CURRENT);

                Map mpPartData = dmPart.getInfo(context, slSelects);
                if (null != mpPartData && !mpPartData.isEmpty()) {
                    String strPolicy = (String) mpPartData.get(DomainConstants.SELECT_POLICY);
                    String strCurrent = (String) mpPartData.get(DomainConstants.SELECT_CURRENT);

                    String strCADRev = null;
                    // Get All revisions of CAD
                    if (UIUtil.isNotNullAndNotEmpty(strPolicy) && POLICY_PSS_Development_Part.equalsIgnoreCase(strPolicy) && UIUtil.isNotNullAndNotEmpty(strCurrent)
                            && TigerConstants.STATE_DEVELOPMENTPART_CREATE.equalsIgnoreCase(strCurrent)) {
                        // DomainObject dmCAD = new DomainObject(strCADId);
                        BusinessObject boCADObject = new BusinessObject(strCADId);
                        BusinessObjectList bolCADObjRevisions = boCADObject.getRevisions(context);

                        String sRevisionId = null;
                        for (Iterator itRevisions = bolCADObjRevisions.iterator(); itRevisions.hasNext();) {
                            sRevisionId = ((BusinessObject) itRevisions.next()).getObjectId();
                            slCADRevisionList.add(sRevisionId);
                        }
                    }

                    if (null != slCADRevisionList && !slCADRevisionList.isEmpty() && slCADRevisionList.contains(strCADId)) {
                        slCADRevisionList.remove(strCADId);
                    }

                    // if(null != slCADRevisionList && !slCADRevisionList.isEmpty()){
                    ArrayList<String> alDeleteRelList = new ArrayList<String>(slCADRevisionList.size());
                    StringList selectStmts = new StringList(2);
                    selectStmts.addElement(DomainConstants.SELECT_ID);
                    selectStmts.addElement(DomainConstants.SELECT_REVISION);
                    // selectStmts.addElement(DomainConstants.SELECT_CURRENT);
                    StringList relStmts = new StringList(1);
                    relStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

                    Pattern typePattern = new Pattern(DomainConstants.TYPE_CAD_DRAWING);
                    typePattern.addPattern(DomainConstants.TYPE_CAD_MODEL);

                    Pattern relPattern = new Pattern(DomainConstants.RELATIONSHIP_PART_SPECIFICATION);
                    // Get CAD connected to Part
                    MapList mlCADObject = dmPart.getRelatedObjects(context, relPattern.getPattern(), typePattern.getPattern(), selectStmts, relStmts, false, true, (short) 1, null, null, 0);

                    if (null != mlCADObject && !mlCADObject.isEmpty()) {
                        int iSize = mlCADObject.size();
                        Map mpCADData = null;
                        String strCADObjRev = null;
                        String strCADRevId = null;
                        String strRelId = null;
                        int iResult = 0;
                        for (int i = 0; i < iSize; i++) {
                            mpCADData = (Map) mlCADObject.get(i);
                            if (null != mpCADData && !mpCADData.isEmpty()) {
                                strCADRevId = (String) mpCADData.get(DomainConstants.SELECT_ID);
                                strCADObjRev = (String) mpCADData.get(DomainConstants.SELECT_REVISION);
                                strRelId = (String) mpCADData.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                                // Check old CADId is connected to Part
                                if (UIUtil.isNotNullAndNotEmpty(strCADRevId) && slCADRevisionList.contains(strCADRevId)) {
                                    alDeleteRelList.add(strRelId);
                                }
                            }
                        }
                        if (null != alDeleteRelList && !alDeleteRelList.isEmpty()) {
                            String[] saRelDelete = alDeleteRelList.toArray(new String[alDeleteRelList.size()]);
                            DomainRelationship.disconnect(context, saRelDelete);
                        }
                    }
                    // }
                }
            }
        } catch (Exception ex) {
            throw ex;
        }

        return iReturn;
    }
    // TIGTK-14547 - mkakade : END

    /**
     * TIGTK-13635 : ALM-5736
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public int connectGoverningProjectForPartOnCreate(Context context, String args[]) throws Exception {
        int iReturn = 0;
        try {
            String strPolicy = args[1];
            String strCurrent = args[2];
            if (UIUtil.isNotNullAndNotEmpty(strPolicy) && TigerConstants.POLICY_PSS_ECPART.equals(strPolicy) && UIUtil.isNotNullAndNotEmpty(strCurrent)
                    && TigerConstants.STATE_PSS_ECPART_PRELIMINARY.equals(strCurrent)) {
                String strPartOID = args[0];
                DomainObject doPart = DomainObject.newInstance(context, strPartOID);
                pss.uls.ULSUIUtil_mxJPO ulsUtil = new pss.uls.ULSUIUtil_mxJPO();
                String strPPOID = ulsUtil.getLatestMaturedProgramProjectFromObjectCS(context, doPart);
                if (UIUtil.isNotNullAndNotEmpty(strPPOID)) {
                    DomainRelationship.connect(context, DomainObject.newInstance(context, strPPOID), TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT, doPart);
                }
            }
        } catch (Exception e) {
            iReturn = 1;
            logger.error(e.getLocalizedMessage(), e);
        }
        return iReturn;
    }

    // TIGTK-14860 : stembulkar : start
    /**
     * TIGTK-14860 : ALM-5890
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public int checkPurposeOfReleaseForChildParts(Context context, String[] args) throws Exception {
        String sObjectId = args[0];
        int retFlag = 0;
        try {
            DomainObject dPart = DomainObject.newInstance(context, sObjectId);
            String[] arrPROfPart = new String[2];

            arrPROfPart[0] = dPart.getInfo(context, "to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].from.to[" + TigerConstants.TYPE_CHANGEACTION + "].from.attribute["
                    + TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE + "]");
            if (!UIUtil.isNotNullAndNotEmpty(arrPROfPart[0])) {
                arrPROfPart[0] = dPart.getInfo(context, "to[" + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM + "].from.to[" + TigerConstants.TYPE_CHANGEACTION + "].from.attribute["
                        + TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE + "]");
            }
            StringList slChildPartIdList = dPart.getInfoList(context, "from[" + RELATIONSHIP_EBOM + "].to.id");

            String strChildPartId = "";
            String strChildPR = "";
            DomainObject dobPreviousRev = DomainObject.newInstance(context);
            for (int i = 0; i < slChildPartIdList.size(); i++) {
                strChildPartId = (String) slChildPartIdList.get(i);
                DomainObject dChildPart = DomainObject.newInstance(context, strChildPartId);
                BusinessObject busObj = dChildPart.getPreviousRevision(context);
                if (busObj.exists(context)) {
                    busObj.open(context);
                    dobPreviousRev = DomainObject.newInstance(context, busObj);
                    busObj.close(context);
                }

                StringList coObjList = dChildPart.getInfoList(context, "to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].from.to[" + TigerConstants.TYPE_CHANGEACTION + "].from.id");
                if (coObjList.size() == 0) {
                    coObjList = dChildPart.getInfoList(context, "to[" + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM + "].from.to[" + TigerConstants.TYPE_CHANGEACTION + "].from.id");
                    StringList coStateList = dChildPart.getInfoList(context,
                            "to[" + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM + "].from.to[" + TigerConstants.TYPE_CHANGEACTION + "].from.current");
                    if (!coStateList.contains("Complete")) {
                        if (dobPreviousRev != null) {
                            coObjList = dobPreviousRev.getInfoList(context, "to[" + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM + "].from.to[" + TigerConstants.TYPE_CHANGEACTION + "].from.id");
                            coStateList = dobPreviousRev.getInfoList(context,
                                    "to[" + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM + "].from.to[" + TigerConstants.TYPE_CHANGEACTION + "].from.current");
                            if (!coStateList.contains("Complete")) {
                                coObjList = dobPreviousRev.getInfoList(context,
                                        "to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].from.to[" + TigerConstants.TYPE_CHANGEACTION + "].from.id");
                            }
                        }
                    }
                }

                int size = coObjList.size();
                for (int j = 0; j < size; j++) {
                    String coObjId = (String) coObjList.get(j);
                    DomainObject dCOObject = DomainObject.newInstance(context, coObjId);
                    String strCurrentState = dCOObject.getCurrentState(context).getName();

                    if (size > 1) {
                        if (strCurrentState.equalsIgnoreCase(TigerConstants.STATE_CHANGEACTION_COMPLETE)) {
                            arrPROfPart[1] = dCOObject.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE);
                        }
                    } else {
                        arrPROfPart[1] = dCOObject.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE);
                    }
                    if (!comparePurposeOfRelease(context, arrPROfPart)) {
                        retFlag = 1;
                        break;
                    }
                }
                if (retFlag == 1) {
                    break;
                }
            }
        } catch (RuntimeException e) {
            retFlag = 1;
            throw e;
        } catch (Exception ex) {
            retFlag = 1;
            throw ex;
        }

        if (retFlag == 1) {
            String strMessage = "Part cannot be promoted to Review as there is Purpose of Release inconsistency between active CO connected to Part and Latest CO connected to its children. Please correct";
            emxContextUtil_mxJPO.mqlNotice(context, strMessage);
        }

        return retFlag;
    }

    /**
     * TIGTK-14860 : ALM-5890
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public boolean comparePurposeOfRelease(Context context, String[] args) {
        boolean bFlag = false;

        String strPRForParentPart = args[0];
        String strPRForChildPart = args[1];

        try {
            if (UIUtil.isNotNullAndNotEmpty(strPRForParentPart) && UIUtil.isNotNullAndNotEmpty(strPRForChildPart)) {
                if ("Acquisition".equalsIgnoreCase(strPRForParentPart)) {
                    if ("Acquisition".equalsIgnoreCase(strPRForChildPart) || "Prototype Tool Launch/Modification".equalsIgnoreCase(strPRForChildPart)
                            || "Serial Tool Launch/Modification".equalsIgnoreCase(strPRForChildPart)) {
                        bFlag = true;
                    }
                } else if ("Prototype Tool Launch/Modification".equalsIgnoreCase(strPRForParentPart)) {
                    if ("Prototype Tool Launch/Modification".equalsIgnoreCase(strPRForChildPart) || "Serial Tool Launch/Modification".equalsIgnoreCase(strPRForChildPart)) {
                        bFlag = true;
                    }
                } else if ("Serial Tool Launch/Modification".equalsIgnoreCase(strPRForParentPart)) {
                    if ("Serial Tool Launch/Modification".equalsIgnoreCase(strPRForChildPart)) {
                        bFlag = true;
                    }
                } else {
                    bFlag = true;
                }
            }
        } catch (Exception ex) {
            throw ex;
        }
        return bFlag;
    }

    // TIGTK-14860 : stembulkar : end
    /**
     * Update Governing Project of Part & Specification except Charted Drawing TIGTK-14892 :: ALM-4106
     * @param context
     * @param args
     * @throws Exception
     */
    public void updateGoverningProjectofPartAndSpec(Context context, String args[]) throws Exception {
        logger.debug(":::::::: ENTER :: updateGoverningProjectofPartAndSpec ::::::::");
        boolean bSuperUser = false;
        try {
            HashMap hmArgumentMap = (HashMap) JPO.unpackArgs(args);
            HashMap hmParamMap = (HashMap) hmArgumentMap.get("paramMap");
            String strNewPPOID = (String) hmParamMap.get("New OID");
            String strPartOID = (String) hmParamMap.get("objectId");
            if (UIUtil.isNotNullAndNotEmpty(strNewPPOID) && UIUtil.isNotNullAndNotEmpty(strPartOID)) {
                TigerUtils.pushContextToSuperUser(context);
                bSuperUser = true;
                DomainObject doPart = DomainObject.newInstance(context, strPartOID);
                MapList mlPartSpecification = doPart.getRelatedObjects(context, DomainConstants.RELATIONSHIP_PART_SPECIFICATION, DomainConstants.QUERY_WILDCARD,
                        new StringList(DomainConstants.SELECT_ID), null, false, true, (short) 1, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, 0);
                Map hmPart = new HashMap(1);
                hmPart.put(DomainConstants.SELECT_ID, strPartOID);
                mlPartSpecification.add(hmPart);
                int iPS = mlPartSpecification.size();
                StringList slObjectSelects = new StringList(4);
                slObjectSelects.add("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT + "].id");
                slObjectSelects.add("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT + "].from.id");
                slObjectSelects.add("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDIMPACTEDPROJECT + "].id");
                slObjectSelects.add("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDIMPACTEDPROJECT + "].from.id");
                DomainObject doProgramProject = DomainObject.newInstance(context, strNewPPOID);
                for (int i = 0; i < iPS; i++) {
                    Map mpPS = (Map) mlPartSpecification.get(i);
                    strPartOID = (String) mpPS.get(DomainConstants.SELECT_ID);
                    BusinessObject boPart = new BusinessObject(strPartOID);
                    BusinessObjectWithSelect bows = boPart.select(context, slObjectSelects);
                    String strGPRelID = bows.getSelectData("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT + "].id");
                    String strPartGPOID = bows.getSelectData("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT + "].from.id");
                    StringList slIPRelID = bows.getSelectDataList("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDIMPACTEDPROJECT + "].id");
                    StringList slPartIPOID = bows.getSelectDataList("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDIMPACTEDPROJECT + "].from.id");
                    boolean bConnect = false;

                    if (UIUtil.isNotNullAndNotEmpty(strGPRelID) && !strNewPPOID.equals(strPartGPOID)) {
                        try {
                            ContextUtil.startTransaction(context, true);
                            DomainRelationship.disconnect(context, strGPRelID);
                            ContextUtil.commitTransaction(context);
                            bConnect = true;
                        } catch (Exception e) {
                            ContextUtil.abortTransaction(context);
                            emxContextUtil_mxJPO.mqlError(context, e.getLocalizedMessage());
                            logger.error(e.getLocalizedMessage(), e);
                            throw new Exception(e.getLocalizedMessage(), e);
                        }
                    } else if (UIUtil.isNullOrEmpty(strGPRelID)) {
                        bConnect = true;
                    }
                    if (bConnect) {
                        if (slPartIPOID != null && !slPartIPOID.isEmpty() && slPartIPOID.contains(strNewPPOID)) {
                            String strIPRelID = (String) slIPRelID.get(slPartIPOID.indexOf(strNewPPOID));
                            DomainRelationship.disconnect(context, strIPRelID);
                            slIPRelID.remove(slPartIPOID.indexOf(strNewPPOID));
                            slPartIPOID.remove(strNewPPOID);
                        }
                        DomainObject doGPPart = DomainObject.newInstance(context, strPartOID);
                        DomainRelationship.connect(context, doProgramProject, TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT, doGPPart);
                    }
                    pss.uls.ULSUIUtil_mxJPO ulsUtil = new pss.uls.ULSUIUtil_mxJPO();
                    ulsUtil.checkAndDisconnectInvalidImpacttedProjectLinks(context, doPart, slPartIPOID, slIPRelID);
                    ulsUtil.createImpactedProjectLinkForEBOM(context, doPart, doProgramProject);
                }
                ContextUtil.popContext(context);
                bSuperUser = false;
            }
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        } finally {
            if (bSuperUser) {
                ContextUtil.popContext(context);
                bSuperUser = false;
            }
        }
        logger.debug(":::::::: EXIT :: updateGoverningProjectofPartAndSpec ::::::::");
    }

    // TIGTK-14543 - Start : copied OOTB method to rename Zip folder/package name

    /**
     * customized OOTB method to rename Zip folder/package name
     * @param paramContext
     * @param args
     * @return TicketWrapper
     * @throws Exception
     */
    public TicketWrapper getDownloadPackageNeutral(Context paramContext, String[] args) throws Exception {
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        String partId = (String) paramMap.get("partId");
        String[] fileObjs = (String[]) paramMap.get("fileObjs");
        String selectedlevel = (String) paramMap.get("selectedlevel");
        String errorPage = (String) paramMap.get("errorPage");
        String strDownloadDateTime = (String) paramMap.get("strDownloadDateTime");
        HttpServletRequest request = (HttpServletRequest) paramMap.get("request");
        HttpServletResponse response = (HttpServletResponse) paramMap.get("response");
        String strArchiveName = (String) paramMap.get("treeLabel");
        boolean bIncBOMStructure = (boolean) paramMap.get("bIncBOMStructure");
        // getDownloadPackage(paramContext, String paramString1, String[] paramArrayOfString, String paramString2, String paramString3, String paramString4, HttpServletRequest paramHttpServletRequest,
        // HttpServletResponse paramHttpServletResponse, boolean paramBoolean)

        return getDownloadPackageNeutral(paramContext, partId, fileObjs, selectedlevel, strDownloadDateTime, errorPage, request, response, bIncBOMStructure, strArchiveName);

    }

    /**
     * @param paramContext
     *            Context
     * @param partID
     *            Object Id
     * @param paramArrayOfString
     * @param paramString2
     * @param paramString3
     * @param paramString4
     * @param paramHttpServletRequest
     * @param paramHttpServletResponse
     * @param paramBoolean
     * @param strArchiveName
     * @return
     * @throws Exception
     */
    public TicketWrapper getDownloadPackageNeutral(Context paramContext, String partID, String[] paramArrayOfString, String paramString2, String paramString3, String paramString4,
            HttpServletRequest paramHttpServletRequest, HttpServletResponse paramHttpServletResponse, boolean paramBoolean, String strArchiveName)

            throws Exception

    {

        String[] fileData = paramArrayOfString;

        boolean isWindows = ImageConversionUtil.isWindows();

        String strworkSpace = null;

        String strPartPath = null;

        char c = '\t';

        String strTab = String.valueOf(c);

        Vector vecData = new Vector();

        Vector vecIDS = new Vector();

        Vector vecFileNames = new Vector();

        Vector vecFileFormats = new Vector();

        Vector vecFilePaths = new Vector();

        Vector vecBoolean = new Vector();

        Vector vecCounts = new Vector();

        Vector vecVersionIDs = new Vector();

        String str4 = null;

        String str5 = null;

        String str6 = null;

        String str7 = DomainObject.FORMAT_GENERIC;

        TicketWrapper localTicketWrapper = null;

        try

        {

            Object slSelect = new StringList(5);

            ((StringList) slSelect).add("name");

            ((StringList) slSelect).add("type");

            ((StringList) slSelect).add("revision");

            ((StringList) slSelect).add("current");

            ((StringList) slSelect).add("description");

            DomainObject domobjPart = new DomainObject(partID);

            Map mapPartInfo = domobjPart.getInfo(paramContext, (StringList) slSelect);

            String strPartName = (String) mapPartInfo.get("name");

            if (strPartName.indexOf("/") != -1) {

                strPartName = StringUtils.replaceAll(strPartName, "/", "-");

            }

            String str9 = (String) mapPartInfo.get("type");

            String strRev = (String) mapPartInfo.get("revision");

            strworkSpace = paramContext.createWorkspace();

            strPartPath = strworkSpace + File.separator + strPartName;

            File flFolder = new File(strPartPath);

            boolean bCreated = flFolder.mkdirs();

            Object strShortPath;

            Object strIntermediate;

            Object strFinal;

            if (fileData != null) {

                for (int i = 0; i < fileData.length; i++)

                {

                    StringTokenizer localObject2 = new StringTokenizer(fileData[i], strTab);

                    if (((StringTokenizer) localObject2).hasMoreTokens())

                    {

                        String filePath = ((StringTokenizer) localObject2).nextToken();

                        if ((isWindows) && (((String) filePath).indexOf("/") != -1)) {

                            filePath = StringUtils.replaceAll((String) filePath, "/", "_");

                        }

                        String strIds = ((StringTokenizer) localObject2).nextToken();

                        String strFileFormat = ((StringTokenizer) localObject2).nextToken();

                        String strFileName = ((StringTokenizer) localObject2).nextToken();

                        Object strLevel = ((StringTokenizer) localObject2).hasMoreTokens() ? ((StringTokenizer) localObject2).nextToken() : "";

                        Object strVersionID = ((StringTokenizer) localObject2).hasMoreTokens() ? ((StringTokenizer) localObject2).nextToken() : "";

                        StringTokenizer arrIds = new StringTokenizer(strIds, "|");

                        String strParentPartID = null;

                        Object strDerivedOutputID = null;

                        if (((StringTokenizer) arrIds).hasMoreTokens())

                        {

                            strParentPartID = ((StringTokenizer) arrIds).nextToken();

                            strDerivedOutputID = ((StringTokenizer) arrIds).nextToken();

                        }

                        String strData = strParentPartID + "|" + (String) strDerivedOutputID + "|" + (String) strFileName;

                        if (((String) strLevel).equals("1")) {

                            strFileName = "";

                        }

                        vecData.addElement(strData);

                        vecIDS.addElement(strDerivedOutputID);

                        vecFileNames.addElement(strFileName);

                        vecFileFormats.addElement(strFileFormat);

                        if ((paramBoolean) && (((String) strLevel).equals("0")))

                        {

                            vecFilePaths.addElement(filePath);

                        }

                        else if (((String) strLevel).equals("0"))

                        {

                            String strTempPath = filePath;

                            int k = ((String) strTempPath).lastIndexOf(File.separator);

                            strShortPath = ((String) strTempPath).substring(k + File.separator.length());

                            String strName = ((String) strTempPath).substring(0, k);

                            int n = strName.lastIndexOf(File.separator);

                            strIntermediate = strName.substring(n + File.separator.length());

                            strFinal = (String) strIntermediate + File.separator + (String) strShortPath;

                            vecFilePaths.addElement(strFinal);

                        }

                        else

                        {

                            vecFilePaths.addElement("");

                        }

                        vecBoolean.addElement("false");

                        vecCounts.addElement(strLevel);

                        vecVersionIDs.addElement(strVersionID);

                    }

                }

            }

            str4 = strPartPath + File.separator;

            Person localPerson = Person.getPerson(paramContext);

            Object localObject2 = new StringList(5);

            ((StringList) localObject2).add(Person.SELECT_COMPANY_ID);

            ((StringList) localObject2).add(Person.SELECT_COMPANY_NAME);

            ((StringList) localObject2).add("name");

            ((StringList) localObject2).add(com.matrixone.apps.common.Part.SELECT_COMPANY_DOCUMENT_HOLDER_ID);

            ((StringList) localObject2).add(DomainConstants.SELECT_COMPANY_STORE);

            Object localObject3 = localPerson.getInfo(paramContext, (StringList) localObject2);

            String str11 = (String) ((Map) localObject3).get(DomainConstants.SELECT_COMPANY_STORE);

            boolean bool2 = EnoviaResourceBundle.getProperty(paramContext, "emxComponents.Package.TruncateRevision").equalsIgnoreCase("true");

            if (fileData != null)

            {

                str5 = strArchiveName + "_PackageReport.csv";

                createPackageFile(paramContext, mapPartInfo, (Map) localObject3, paramString3, vecData, str4, str5, bool2, paramHttpServletRequest);

            }

            str6 = strArchiveName + "_BOMReport.csv";

            createBOMFile(paramContext, domobjPart, mapPartInfo, paramString2, paramString3, str4, str6, bool2, paramHttpServletRequest);

            Object localObject4 = createDocumentAndCheckInFiles(paramContext, (Map) localObject3, str9 + " " + strPartName + " " + strRev, str7, str4, str5, str6);

            Object localObject5 = (DomainObject) ((Map) localObject4).get(DomainConstants.TYPE_DOCUMENT);

            Object localObject6 = (DomainObject) ((Map) localObject4).get(DomainConstants.TYPE_HOLDER);

            Object localObject7 = ((DomainObject) localObject5).getInfo(paramContext, "id");

            if (fileData != null)

            {

                vecIDS.addElement(localObject7);

                vecFileNames.addElement(str5);

                vecFileFormats.addElement(str7);

                vecFilePaths.addElement("");

                vecBoolean.addElement("false");

                vecCounts.addElement("0");

                vecVersionIDs.addElement("0");

            }

            vecIDS.addElement(localObject7);

            vecFileNames.addElement(str6);

            vecFileFormats.addElement(str7);

            vecFilePaths.addElement("");

            vecBoolean.addElement("false");

            vecCounts.addElement("0");

            vecVersionIDs.addElement("0");

            int j = 0;

            Object localObject8 = null;

            String str14 = paramHttpServletRequest.getParameter("workspaceFolderId");

            Object localObject9 = paramHttpServletRequest.getParameter("archiveName");

            String str15 = "";

            int m = vecIDS.size();

            String str16 = "";

            Object localObject15;

            Object localObject16;

            Object localObject17;

            Object localObject18;

            Object localObject19;

            String strCurrentDate = DomainConstants.EMPTY_STRING;
            SimpleDateFormat formatter = new SimpleDateFormat("MM-dd-yyyy_hh-mm-ss_a");
            Calendar calender = Calendar.getInstance();
            strCurrentDate = formatter.format(calender.getTime());
            // strCurrentDate = strCurrentDate.replaceAll(" ", "_");
            StringBuilder sbPackage = new StringBuilder(64);
            String strArchivePackage = sbPackage.append("Download_").append(strArchiveName).append("_").append(strCurrentDate).append(".zip").toString();

            if (j != 0)

            {
                str16 = (String) strArchivePackage;

                StringList localObject11 = new StringList(4);

                ((StringList) localObject11).add("policy.defaultformat");

                ((StringList) localObject11).add("policy.store");

                strIntermediate = ((com.matrixone.apps.common.CommonDocument) localObject8).getInfo(paramContext, (StringList) localObject11);

                strFinal = (String) ((Map) strIntermediate).get("policy.defaultformat");

                if ((str11 == null) || ("".equals(str11))) {

                    str11 = (String) ((Map) strIntermediate).get("policy.store");

                } else {

                    str11 = PropertyUtil.getSchemaProperty(paramContext, str11);

                }

                localObject15 = new ArrayList();

                for (int i1 = 0; i1 < m; i1++) {

                    ((ArrayList) localObject15).add(new BusinessObjectProxy((String) vecIDS.elementAt(i1), (String) vecFileFormats.elementAt(i1), (String) vecFileNames.elementAt(i1),
                            (String) vecFilePaths.elementAt(i1), false, false));

                }

                int i1 = 1;

                localObject16 = Framework.getFullClientSideURL(paramHttpServletRequest, paramHttpServletResponse, "");

                localTicketWrapper = Checkout.doIt(paramContext, true, str15, (String) localObject16, (ArrayList) localObject15);

                localObject17 = localTicketWrapper.getExportString();

                localObject18 = new ArrayList();

                localObject19 = new BusinessObjectProxy(((com.matrixone.apps.common.CommonDocument) localObject8).getObjectId(paramContext), (String) strFinal, str15, true, false);

                ((ArrayList) localObject18).add(localObject19);

                localTicketWrapper = PreCheckin.doIt(paramContext, str11, (String) localObject16, ((ArrayList) localObject18).size());

                String str17 = localTicketWrapper.getExportString();

                String str18 = Resources.setAction(localTicketWrapper.getActionURL(), "checkoutAndCheckin");

                URL localURL = new URL(str18);

                HttpURLConnection localHttpURLConnection = (HttpURLConnection) localURL.openConnection();

                localHttpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                localHttpURLConnection.setDoOutput(true);

                localHttpURLConnection.setDoInput(true);

                localHttpURLConnection.setUseCaches(false);

                localHttpURLConnection.setRequestProperty("Accept-Language", "en-us");

                localHttpURLConnection.setRequestMethod("POST");

                localHttpURLConnection.connect();

                OutputStream localOutputStream = localHttpURLConnection.getOutputStream();

                String str19 = McsBase.resolveFcsParam("checkinTicket") + "=" + XSSUtil.encodeForURL(str17);

                str19 = str19 + "&" + McsBase.resolveFcsParam("checkoutTicket") + "=" + XSSUtil.encodeForURL((String) localObject17);

                localOutputStream.write((str19 + "\r\n").getBytes(Charset.forName("UTF-8")));

                if (localHttpURLConnection.getResponseCode() != 200)

                {

                    Object localObject20 = paramContext.getSession().getLanguage();

                    String strRequestFailed = ComponentsUtil.i18nStringNow("emxComponents.Part.Requestfailed", (String) localObject20);

                    throw new Exception(strRequestFailed);

                }

                Object localObject20 = new BufferedReader(new InputStreamReader(localHttpURLConnection.getInputStream(), Charset.forName("UTF-8")));

                Object localObject21 = new StringBuffer();

                String str20;

                while ((str20 = ((BufferedReader) localObject20).readLine()) != null) {

                    ((StringBuffer) localObject21).append(str20);

                }

                ((BufferedReader) localObject20).close();

                Logger.log("Got: " + ((StringBuffer) localObject21).toString());

                Checkin.doIt(paramContext, ((StringBuffer) localObject21).toString(), str11, (ArrayList) localObject18);

                String[] arrayOfString3 = new String[m];

                String[] arrayOfString4 = new String[m];

                String[] arrayOfString5 = new String[m];

                String[] arrayOfString6 = new String[m];

                String[] arrayOfString7 = new String[m];

                String[] arrayOfString8 = new String[m];

                long[] arrayOfLong = new long[m];

                String str21 = "";

                for (int i3 = 0; i3 < m; i3++)

                {

                    arrayOfString3[i3] = ((String) vecIDS.elementAt(i3));

                    arrayOfString4[i3] = ((String) vecFileNames.elementAt(i3));

                    arrayOfString5[i3] = ((String) vecFileFormats.elementAt(i3));

                    arrayOfString6[i3] = ((String) vecFilePaths.elementAt(i3));

                    arrayOfString7[i3] = ((String) vecBoolean.elementAt(i3));

                    arrayOfString8[i3] = ((String) vecVersionIDs.elementAt(i3));

                    str21 = (String) vecCounts.elementAt(i3);

                    if ((str21 == null) || (str21.equals(""))) {

                        str21 = "0";

                    }

                    // Long localLong = new Long(str21);

                    arrayOfLong[i3] = Long.parseLong(str21);

                }

                localTicketWrapper = HttpVcCheckout.doIt(paramContext, arrayOfString3, arrayOfString4, arrayOfString5, arrayOfString7, arrayOfLong, arrayOfString8, arrayOfString6, true, str16,
                        paramString4, paramHttpServletRequest, paramHttpServletResponse);

            }

            else

            {

                str16 = strArchivePackage;

                String[] strIDS = new String[m];

                String[] strFileNames = new String[m];

                String[] strFormats = new String[m];

                String[] strPaths = new String[m];

                String[] strBoolean = new String[m];

                String[] strVersionIds = new String[m];

                long[] lnData = new long[m];
                /*
                 * String[] strIDS = new String[m]; String[] strFileNames = new String[m]; String[] strFormats = new String[m]; String[] strPaths = new String[m]; String[] strBoolean = new String[m];
                 * String[] strVersionIds = new String[m]; long[] lnData = new long[m];
                 */

                localObject18 = "";

                for (int i2 = 0; i2 < m; i2++)

                {

                    strIDS[i2] = ((String) vecIDS.elementAt(i2));

                    strFileNames[i2] = ((String) vecFileNames.elementAt(i2));

                    strFormats[i2] = ((String) vecFileFormats.elementAt(i2));

                    strPaths[i2] = ((String) vecFilePaths.elementAt(i2));

                    strBoolean[i2] = ((String) vecBoolean.elementAt(i2));

                    strVersionIds[i2] = ((String) vecVersionIDs.elementAt(i2));

                    localObject18 = (String) vecCounts.elementAt(i2);

                    if ((localObject18 == null) || (((String) localObject18).equals(""))) {

                        localObject18 = "0";

                    }

                    // localObject19 = new Long((String) localObject18);
                    // lnData[i2] = ((Long) localObject19).longValue();

                    lnData[i2] = Long.parseLong((String) localObject18);

                }

                localTicketWrapper = HttpVcCheckout.doIt(paramContext, (String[]) strIDS, (String[]) strFileNames, (String[]) strFormats, strBoolean, (long[]) lnData, (String[]) strVersionIds,
                        (String[]) strPaths, true, str16, paramString4, paramHttpServletRequest, paramHttpServletResponse);

            }

            slSelect = null;

            if (str4 != null)

            {

                if (str5 != null)

                {

                    slSelect = new File(str4 + str5);

                    if ((((File) slSelect).exists()) && (!((File) slSelect).delete())) {

                        ((File) slSelect).deleteOnExit();

                    }

                }

                if (str6 != null)

                {

                    slSelect = new File(str4 + str6);

                    if ((((File) slSelect).exists()) && (!((File) slSelect).delete())) {

                        ((File) slSelect).deleteOnExit();

                    }

                }

            }

            if (strPartPath != null)

            {

                slSelect = new File(strPartPath);

                if ((((File) slSelect).exists()) && (!((File) slSelect).delete())) {

                    ((File) slSelect).deleteOnExit();

                }

            }

            if (strworkSpace != null)

            {

                slSelect = new File(strworkSpace);

                if ((((File) slSelect).exists()) && (!((File) slSelect).delete())) {

                    ((File) slSelect).deleteOnExit();

                }

            }

        }

        catch (Exception localException)

        {

            ContextUtil.abortTransaction(paramContext);

            throw new FrameworkException(localException);

        }

        finally

        {

            File localFile2 = null;

            if (str4 != null)

            {

                if (str5 != null)

                {

                    localFile2 = new File(str4 + str5);

                    if ((localFile2.exists()) && (!localFile2.delete())) {

                        localFile2.deleteOnExit();

                    }

                }

                if (str6 != null)

                {

                    localFile2 = new File(str4 + str6);

                    if ((localFile2.exists()) && (!localFile2.delete())) {

                        localFile2.deleteOnExit();

                    }

                }

            }

            if (strPartPath != null)

            {

                localFile2 = new File(strPartPath);

                if ((localFile2.exists()) && (!localFile2.delete())) {

                    localFile2.deleteOnExit();

                }

            }

            if (strworkSpace != null)

            {

                localFile2 = new File(strworkSpace);

                if ((localFile2.exists()) && (!localFile2.delete())) {

                    localFile2.deleteOnExit();

                }

            }

        }

        return localTicketWrapper;

    }

    /**
     * Copied OOTB method from Part jar
     * @param paramContext
     * @param paramMap
     * @param paramString1
     * @param paramString2
     * @param paramString3
     * @param paramString4
     * @param paramString5
     * @return
     * @throws FrameworkException
     */
    private Map createDocumentAndCheckInFiles(Context paramContext, Map paramMap, String paramString1, String paramString2, String paramString3, String paramString4, String paramString5)
            throws FrameworkException {
        try {
            String str1 = (String) paramMap.get(Person.SELECT_COMPANY_NAME);
            String str2 = (String) paramMap.get(Person.SELECT_COMPANY_ID);
            DomainObject localDomainObject1 = DomainObject.newInstance(paramContext, str2);
            String str3 = (String) paramMap.get(com.matrixone.apps.common.Part.SELECT_COMPANY_DOCUMENT_HOLDER_ID);

            Document localDocument = (Document) DomainObject.newInstance(paramContext, DomainConstants.TYPE_DOCUMENT);
            localDocument = localDocument.create(paramContext, DomainObject.TYPE_DOCUMENT, "", Document.POLICY_DOCUMENT, paramString1, "Document Title", null);

            DomainObject localDomainObject2 = DomainObject.newInstance(paramContext, DomainObject.TYPE_HOLDER);

            if ((str3 == null) || (str3.equals(""))) {
                localDomainObject2.createObject(paramContext, DomainObject.TYPE_HOLDER, str1 + " Reports", "-", Document.POLICY_HOLDER, paramContext.getVault().toString());
                localDomainObject2.connect(paramContext, com.matrixone.apps.common.Part.RELATIONSHIP_DOCUMENT_HOLDER, localDomainObject1, true);
            } else {
                localDomainObject2 = DomainObject.newInstance(paramContext, str3);
            }

            localDocument.connect(paramContext, com.matrixone.apps.common.Part.RELATIONSHIP_COMPANY_DOCUMENTS, localDomainObject2, true);

            if (paramString4 != null)
                localDocument.checkinFile(paramContext, true, true, null, paramString2, paramString4, paramString3);
            localDocument.checkinFile(paramContext, true, true, null, paramString2, paramString5, paramString3);

            HashMap localHashMap = new HashMap();
            localHashMap.put(DomainConstants.TYPE_DOCUMENT, localDocument);
            localHashMap.put(DomainConstants.TYPE_HOLDER, localDomainObject2);
            return localHashMap;
        } catch (Exception localException) {
            throw new FrameworkException(localException);
        }
    }

    /**
     * Copied OOTB method from Part jar
     * @param paramContext
     * @param paramMap1
     * @param paramMap2
     * @param paramString1
     * @param paramVector
     * @param paramString2
     * @param paramString3
     * @param paramBoolean
     * @param paramHttpServletRequest
     * @throws Exception
     */
    private void createPackageFile(Context paramContext, Map paramMap1, Map paramMap2, String paramString1, Vector paramVector, String paramString2, String paramString3, boolean paramBoolean,
            HttpServletRequest paramHttpServletRequest) throws Exception {
        // StringBuffer localStringBuffer1 = new StringBuffer();

        StringBuffer localStringBuffer2 = new StringBuffer(5000);

        // String str1 = EnoviaResourceBundle.getProperty(paramContext, "emxComponents.Package.CompanyName");
        // String str2 = EnoviaResourceBundle.getProperty(paramContext, "emxComponents.Package.LoginUser");

        Locale localLocale = paramContext.getLocale();

        String str3 = EnoviaResourceBundle.getProperty(paramContext, "emxComponentsStringResource", localLocale, "emxComponents.Package.SummaryReport");
        String str4 = EnoviaResourceBundle.getProperty(paramContext, "emxComponentsStringResource", localLocale, "emxComponents.Basic.Type");
        String str5 = EnoviaResourceBundle.getProperty(paramContext, "emxComponentsStringResource", localLocale, "emxComponents.Package.AssemblyNo");
        String str6 = EnoviaResourceBundle.getProperty(paramContext, "emxComponentsStringResource", localLocale, "emxComponents.Basic.Revision");
        String str7 = EnoviaResourceBundle.getProperty(paramContext, "emxComponentsStringResource", localLocale, "emxComponents.Basic.State");
        String str8 = EnoviaResourceBundle.getProperty(paramContext, "emxComponentsStringResource", localLocale, "emxComponents.Package.Description");
        String str9 = EnoviaResourceBundle.getProperty(paramContext, "emxComponentsStringResource", localLocale, "emxComponents.Common.CompanyName");
        String str10 = EnoviaResourceBundle.getProperty(paramContext, "emxComponentsStringResource", localLocale, "emxComponents.Package.downloadedOn");
        String str11 = EnoviaResourceBundle.getProperty(paramContext, "emxComponentsStringResource", localLocale, "emxComponents.Package.result");
        String str12 = EnoviaResourceBundle.getProperty(paramContext, "emxComponentsStringResource", localLocale, "emxComponents.Package.sucess");
        String str13 = EnoviaResourceBundle.getProperty(paramContext, "emxComponentsStringResource", localLocale, "emxComponents.Common.FileName");
        String str14 = EnoviaResourceBundle.getProperty(paramContext, "emxComponentsStringResource", localLocale, "emxComponents.Package.AssociatedObj");
        String str15 = EnoviaResourceBundle.getProperty(paramContext, "emxComponentsStringResource", localLocale, "emxComponents.Package.PartName");
        String str16 = EnoviaResourceBundle.getProperty(paramContext, "emxComponentsStringResource", localLocale, "emxComponents.Package.LoginUser");

        localStringBuffer2.append(str3).append("\n");
        localStringBuffer2.append(str4).append(",").append(str5).append(",").append(str6).append("\n");
        localStringBuffer2.append(paramMap1.get("type")).append(",").append("=\"").append(paramMap1.get("name")).append("\"").append(",");
        String str17 = (String) paramMap1.get("revision");

        if (paramBoolean) {
            localStringBuffer2.append(str17).append("\n");
        } else {
            localStringBuffer2.append("'").append(str17).append("'\n");
        }
        localStringBuffer2.append(str7).append(",").append(paramMap1.get("current")).append("\n");
        localStringBuffer2.append(str8).append(",\"").append(paramMap1.get("description")).append("\"\n\n");
        localStringBuffer2.append(str9).append(",\"").append(paramMap2.get(Person.SELECT_COMPANY_NAME)).append("\"\n");
        localStringBuffer2.append(str16).append(",\"").append(paramMap2.get("name")).append("\"\n");
        localStringBuffer2.append(str10).append(",\"").append(paramString1).append("\"\n");
        localStringBuffer2.append(str11).append(",").append(str12).append("\n\n");
        localStringBuffer2.append(str13).append(",").append(str8).append(",").append(str14).append(",").append(str15).append(",").append(str6).append("\n");

        for (int i = 0; i < paramVector.size(); i++) {
            String localObject = (String) paramVector.elementAt(i);
            if (localObject != null) {
                StringTokenizer localStringTokenizer = new StringTokenizer((String) localObject, "|");
                if (localStringTokenizer.hasMoreTokens()) {
                    String str19 = localStringTokenizer.nextToken();
                    String str20 = localStringTokenizer.nextToken();
                    String str21 = localStringTokenizer.nextToken();

                    StringList localStringList = new StringList(2);
                    localStringList.add("name");
                    localStringList.add("revision");

                    DomainObject localDomainObject1 = DomainObject.newInstance(paramContext);
                    localDomainObject1.setId(str19);
                    Map localMap1 = localDomainObject1.getInfo(paramContext, localStringList);
                    String str22 = (String) localMap1.get("name");
                    String str23 = (String) localMap1.get("revision");

                    DomainObject localDomainObject2 = DomainObject.newInstance(paramContext);
                    localDomainObject2.setId(str20);
                    Map localMap2 = localDomainObject2.getInfo(paramContext, localStringList);
                    String str24 = (String) localMap2.get("name");

                    MapList localMapList = localDomainObject2.getRelatedObjects(paramContext, Document.RELATIONSHIP_ACTIVE_VERSION, Document.TYPE_DOCUMENTS,
                            new StringList(Document.SELECT_CHECKIN_REASON), null, false, true, (short) 1, Document.SELECT_TITLE + " == \"" + str21 + "\"", null);

                    Iterator localIterator = localMapList.iterator();
                    String str25 = "";

                    if (localIterator.hasNext()) {
                        Map localMap3 = (Map) localIterator.next();
                        str25 = (String) localMap3.get(Document.SELECT_CHECKIN_REASON);
                    }

                    localStringBuffer2.append("\"").append(str21).append("\",\"").append(str25).append("\",").append(str24).append(",");
                    localStringBuffer2.append("=\"").append(str22).append("\"").append(",");

                    if (paramBoolean) {
                        localStringBuffer2.append(str23).append("\n");
                    } else {
                        localStringBuffer2.append("'").append(str23).append("'\n");
                    }
                }
            }
        }
        String str18 = UINavigatorUtil.getFileEncoding(paramContext, paramHttpServletRequest);
        Object localObject = new RandomAccessFile(paramString2 + paramString3, "rw");
        ((RandomAccessFile) localObject).write(localStringBuffer2.toString().getBytes(str18));
        ((RandomAccessFile) localObject).close();
    }

    /**
     * Copied OOTB method from Part jar
     * @param paramContext
     * @param paramDomainObject
     * @param paramMap
     * @param paramString1
     * @param paramString2
     * @param paramString3
     * @param paramString4
     * @param paramBoolean
     * @param paramHttpServletRequest
     * @throws Exception
     */
    private void createBOMFile(Context paramContext, DomainObject domobjPart, Map paramMap, String paramString1, String paramString2, String paramString3, String paramString4, boolean paramBoolean,
            HttpServletRequest paramHttpServletRequest) throws Exception {
        StringList slSelectList = new StringList(7);
        slSelectList.addElement("id");
        slSelectList.addElement("type");
        slSelectList.addElement("name");
        slSelectList.addElement("revision");
        slSelectList.addElement("description");
        slSelectList.addElement(DomainObject.SELECT_ATTRIBUTE_UNITOFMEASURE);
        slSelectList.addElement("from[" + DomainConstants.POLICY_MANUFACTURER_EQUIVALENT + "].to.name");

        StringList slRelSelect = new StringList(3);
        slRelSelect.addElement(DomainObject.SELECT_ATTRIBUTE_QUANTITY);
        slRelSelect.addElement(DomainObject.SELECT_ATTRIBUTE_REFERENCE_DESIGNATOR);
        slRelSelect.addElement(DomainObject.SELECT_FIND_NUMBER);

        short s = Short.parseShort(paramString1);

        MapList localMapList1 = domobjPart.getRelatedObjects(paramContext, DomainRelationship.RELATIONSHIP_EBOM, DomainObject.TYPE_PART, slSelectList, slRelSelect, false, true, s, "", "");

        StringBuffer localStringBuffer1 = new StringBuffer(5000);

        String str1 = "";
        String str2 = "";
        String str3 = "";
        Locale localLocale = paramContext.getLocale();
        String strType = EnoviaResourceBundle.getProperty(paramContext, "emxComponentsStringResource", localLocale, "emxComponents.Basic.Type");
        String strAssemblyNo = EnoviaResourceBundle.getProperty(paramContext, "emxComponentsStringResource", localLocale, "emxComponents.Package.AssemblyNo");
        String strRevision = EnoviaResourceBundle.getProperty(paramContext, "emxComponentsStringResource", localLocale, "emxComponents.Basic.Revision");
        String strBOMLevel = EnoviaResourceBundle.getProperty(paramContext, "emxComponentsStringResource", localLocale, "emxComponents.Package.Level");
        String strFindNum = EnoviaResourceBundle.getProperty(paramContext, "emxComponentsStringResource", localLocale, "emxComponents.Package.FindNum");
        String strQuantity = EnoviaResourceBundle.getProperty(paramContext, "emxComponentsStringResource", localLocale, "emxComponents.Common.Quantity");
        String strRefDesignator = EnoviaResourceBundle.getProperty(paramContext, "emxComponentsStringResource", localLocale, "emxComponents.Package.RefDesignator");
        String strItem = EnoviaResourceBundle.getProperty(paramContext, "emxComponentsStringResource", localLocale, "emxComponents.Package.Item");
        String strDescription = EnoviaResourceBundle.getProperty(paramContext, "emxComponentsStringResource", localLocale, "emxComponents.Package.Description");
        String strUOM = EnoviaResourceBundle.getProperty(paramContext, "emxComponentsStringResource", localLocale, "emxComponents.Common.UOM");
        String strMPN = EnoviaResourceBundle.getProperty(paramContext, "emxComponentsStringResource", localLocale, "emxComponents.Package.MPN");
        String strManufacturer = EnoviaResourceBundle.getProperty(paramContext, "emxComponentsStringResource", localLocale, "emxComponents.Package.Manufacturer");
        String strLocation_Status = EnoviaResourceBundle.getProperty(paramContext, "emxFrameworkStringResource", localLocale, "emxFramework.Attribute.Location_Status");
        String strLocation_Preference = EnoviaResourceBundle.getProperty(paramContext, "emxFrameworkStringResource", localLocale, "emxFramework.Attribute.Location_Preference");
        String strLocation = EnoviaResourceBundle.getProperty(paramContext, "emxComponentsStringResource", localLocale, "emxComponents.Common.Location");
        localStringBuffer1.append(strType).append(",").append(strAssemblyNo).append(",").append(strRevision).append("\n");

        localStringBuffer1.append(paramMap.get("type")).append(",").append("=\"").append(paramMap.get("name")).append("\"").append(",");

        if (paramBoolean) {
            localStringBuffer1.append(paramMap.get("revision")).append("\n");
        } else {
            localStringBuffer1.append("'").append(paramMap.get("revision")).append("'\n");
        }

        localStringBuffer1.append(strBOMLevel).append(",").append(strFindNum).append(",").append(strQuantity).append(",").append(strRefDesignator).append(",").append(strItem).append(",")
                .append(strRevision).append(",").append(strDescription).append(",").append(strUOM).append(",").append(strMPN).append(",").append(strManufacturer).append(",").append(strLocation)
                .append(",").append(strLocation_Status).append(",").append(strLocation_Preference).append("\n");

        Iterator localIterator = localMapList1.iterator();

        while (localIterator.hasNext()) {
            Map localObject1 = (Map) localIterator.next();

            localStringBuffer1.append(((Map) localObject1).get("level")).append(",\"").append(((Map) localObject1).get(DomainConstants.SELECT_FIND_NUMBER)).append("\",");
            localStringBuffer1.append(((Map) localObject1).get(DomainConstants.SELECT_ATTRIBUTE_QUANTITY)).append(",\"");
            localStringBuffer1.append(((Map) localObject1).get(DomainConstants.SELECT_ATTRIBUTE_REFERENCE_DESIGNATOR)).append("\",");
            localStringBuffer1.append("=\"").append(((Map) localObject1).get("name")).append("\"").append(",");

            if (paramBoolean) {
                localStringBuffer1.append(((Map) localObject1).get("revision")).append(",");
            } else {
                localStringBuffer1.append("'").append(((Map) localObject1).get("revision")).append("',");
            }

            String localObject2 = (String) ((Map) localObject1).get("description");
            localObject2 = FrameworkUtil.findAndReplace((String) localObject2, "\"", "\"\"");
            localStringBuffer1.append("\"").append((String) localObject2).append("\",");

            str3 = (String) ((Map) localObject1).get(DomainObject.SELECT_ATTRIBUTE_UNITOFMEASURE);
            str3 = str3.replace(' ', '_');
            str3 = "emxFramework.Range.Unit_of_Measure." + str3;
            str3 = EnoviaResourceBundle.getProperty(paramContext, "emxFrameworkStringResource", localLocale, str3);
            localStringBuffer1.append(str3).append(",");

            DomainObject localDomainObject = DomainObject.newInstance(paramContext, (String) ((Map) localObject1).get("id"));

            String str19 = "relationship[" + DomainObject.RELATIONSHIP_MANUFACTURING_RESPONSIBILITY + "].from.name";

            String str20 = "relationship[" + RELATIONSHIP_ALLOCATION_RESPONSIBILITY + "].attribute[" + ATTRIBUTE_LOCATION_PREFERENCE + "].value";
            String str21 = "relationship[" + RELATIONSHIP_ALLOCATION_RESPONSIBILITY + "].attribute[" + ATTRIBUTE_LOCATION_STATUS + "].value";
            String str22 = "relationship[" + RELATIONSHIP_ALLOCATION_RESPONSIBILITY + "].from.name";

            StringList localStringList3 = new StringList(4);
            localStringList3.addElement("id");
            localStringList3.addElement("type");
            localStringList3.addElement("name");
            localStringList3.addElement(str19);

            localStringList3.addElement(str22);
            localStringList3.addElement(str21);
            localStringList3.addElement(str20);

            StringList localStringList4 = new StringList(1);
            localStringList4.addElement("name[connection]");

            StringBuffer localStringBuffer2 = new StringBuffer(RELATIONSHIP_LOCATION_EQUIVALENT);
            localStringBuffer2.append(",").append(RELATIONSHIP_MANUFACTURER_EQUIVALENT);

            StringBuffer localStringBuffer3 = new StringBuffer(TYPE_LOCATION_EQUIVALENT_OBJECT);
            localStringBuffer3.append(",").append(TYPE_PART).append(",");

            MapList localMapList2 = new MapList();
            if (((Map) localObject1).containsKey("from[" + DomainConstants.POLICY_MANUFACTURER_EQUIVALENT + "].to.name")) {
                localMapList2 = localDomainObject.getRelatedObjects(paramContext, localStringBuffer2.toString(), localStringBuffer3.toString(), localStringList3, localStringList4, false, true,
                        (short) 2, null, null);
            }

            Vector localVector1 = new Vector();
            Vector localVector2 = new Vector();
            Vector localVector3 = new Vector();

            Vector localVector4 = new Vector();
            Vector localVector5 = new Vector();
            Vector localVector6 = new Vector();

            for (int i = 0; i < localMapList2.size(); i++) {
                Map localMap = (Map) localMapList2.get(i);
                String str24 = (String) localMap.get("name[connection]");

                if (("2".equals((String) localMap.get("level"))) || (str24.equals(RELATIONSHIP_MANUFACTURER_EQUIVALENT))) {
                    localVector1.addElement(localMap.get("id"));
                    localVector2.addElement(localMap.get("name"));
                    localVector3.addElement(localMap.get(str19));

                    localVector6.addElement(localMap.get(str22));
                    localVector5.addElement(localMap.get(str21));
                    localVector4.addElement(localMap.get(str20));
                }
            }

            if (localVector1.size() == 0) {
                localStringBuffer1.append(",\n");
            } else {
                localStringBuffer1.append("\"").append(localVector2.elementAt(0)).append("\",\"");

                localStringBuffer1.append(localVector3.elementAt(0) + "\",\"");
                String str23 = (String) localVector6.elementAt(0);
                if (UIUtil.isNullOrEmpty(str23)) {
                    str23 = "";
                }
                localStringBuffer1.append(str23 + "\",\"");
                str1 = (String) localVector5.elementAt(0);
                if (UIUtil.isNotNullAndNotEmpty(str1)) {
                    str1 = str1.replace(' ', '_');
                    str1 = "emxFramework.Range.Location_Status." + str1;
                    str1 = EnoviaResourceBundle.getProperty(paramContext, "emxFrameworkStringResource", localLocale, str1);
                } else {
                    str1 = "";
                }
                localStringBuffer1.append(str1 + "\",\"");
                str2 = (String) localVector4.elementAt(0);
                if (UIUtil.isNotNullAndNotEmpty(str2)) {
                    str2 = str2.replace(' ', '_');
                    str2 = "emxFramework.Range.Location_Preference." + str2;
                    str2 = EnoviaResourceBundle.getProperty(paramContext, "emxFrameworkStringResource", localLocale, str2);
                } else {
                    str2 = "";
                }
                localStringBuffer1.append(str2 + "\"\n");
                for (int j = 1; j < localVector1.size(); j++) {
                    localStringBuffer1.append(",,,,,,,,");
                    localStringBuffer1.append("\"").append(localVector2.elementAt(j)).append("\",\"");
                    localStringBuffer1.append(localVector3.elementAt(j) + "\"").append(",\"");
                    localStringBuffer1.append(localVector6.elementAt(j) + "\"");
                    str1 = (String) localVector5.elementAt(j);
                    if (UIUtil.isNotNullAndNotEmpty(str1)) {
                        str1 = str1.replace(' ', '_');
                        str1 = "emxFramework.Range.Location_Status." + str1;
                        str1 = EnoviaResourceBundle.getProperty(paramContext, "emxFrameworkStringResource", localLocale, str1);
                    } else {
                        str1 = "";
                    }
                    localStringBuffer1.append(",\"").append(str1 + "\"");
                    str2 = (String) localVector4.elementAt(j);
                    if (UIUtil.isNotNullAndNotEmpty(str2)) {
                        str2 = str2.replace(' ', '_');
                        str2 = "emxFramework.Range.Location_Preference." + str2;
                        str2 = EnoviaResourceBundle.getProperty(paramContext, "emxFrameworkStringResource", localLocale, str2);
                    } else {
                        str2 = "";
                    }
                    localStringBuffer1.append(",\"").append(str2 + "\"\n");
                }
            }
        }

        Object localObject1 = UINavigatorUtil.getFileEncoding(paramContext, paramHttpServletRequest);
        Object localObject2 = new RandomAccessFile(paramString3 + paramString4, "rw");
        ((RandomAccessFile) localObject2).write(localStringBuffer1.toString().getBytes((String) localObject1));
        ((RandomAccessFile) localObject2).close();
    }
    // TIGTK-14543 - END : copied OOTB method to rename Zip folder/package name

}