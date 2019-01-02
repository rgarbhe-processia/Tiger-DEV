package fpdm.mbom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.MapList;

import matrix.db.Access;
import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.Relationship;
import matrix.db.RelationshipList;
import matrix.db.RelationshipType;
import matrix.util.MatrixException;
import matrix.util.StringList;

public class MBOM100_mxJPO extends fpdm.mbom.Constants_mxJPO {

    private static Logger logger = LoggerFactory.getLogger("fpdm.mbom.MBOM100");

    public static final StringList HARMONY_BUS_SELECT = new StringList(new String[] { SELECT_TYPE, SELECT_NAME, SELECT_REVISION, "attribute.value" });

    public static final StringList COLOR_BUS_SELECT = new StringList(new String[] { SELECT_TYPE, SELECT_NAME, SELECT_REVISION, "attribute.value" });

    private Context context = null;

    private String sObjectId = null;

    private String sObjectType = null;

    private String sObjectName = null;

    private String sObjectRevision = null;

    private String sPolicy = null;

    private String sPartName = null;

    private String sFCSReference = null;

    private String sColorCode = null;

    private float fQuantity = 1;

    private String sUnit = "";

    private String sFindNumber = "";

    private String sPhantomPart = "No";

    /** If it is null, this is a BOM head. */
    private MBOM100_mxJPO oParent = null;

    /** The default color code XXX */
    private String sDefaultColorCode = null;

    private String sMBOM150PhysicalIdentifier = null;

    private String sMBOM150ID = null;

    private Map<?, ?> mMBOM150Info = null;

    private String sPlantID = null;

    private String sHarmonyAssociationRelId = null;

    private String sHarmonyID = null;

    private String sHarmonyName = "";

    private String sHarmonyNetWeight = null;

    private String sHarmonyGrossWeight = null;

    private String sProductConfigurationPID = "";

    private String sProductConfigurationName = "";

    private String sProductConfigurationDescription = "";

    private String sVariantAssemblyPID = "";

    private String sVariantID = null;

    private String sSynchronized = "No";

    private String sCustomerPartNumber = "";

    private String sOldMaterialNumber = "";

    private String sHarmoniesAttrValue = null;

    private Map<String, Object> mPlantInfo = null;

    private Map<String, Object> mHarmonyInfo = null;

    private Map<String, Object> mColorInfo = null;

    private Map<?, ?> mProductConfigurationInfo = null;

    private boolean isMaterial = false;

    private boolean isLegacy = false;

    // children list after update
    private ArrayList<String> alChildrenObjectsIds = new ArrayList<String>();

    // children and theirs relationships existing on database
    private Map<String, String> mChildrenInDB = null;

    private String sOrganizationName = null;

    /**
     * Constructor
     * @param context
     * @param args
     * @throws Exception
     */
    public MBOM100_mxJPO(Context context, Map<?, ?> mMBOM150Info, Map<String, Object> mPlantInfo, Map<String, Object> mSelectedHarmonyInfo, Map<String, Object> mColorInfo, String sHarmoniesAttrValue,
            boolean isMaterial, String sOrgName) throws Exception {

        this.context = context;
        this.sOrganizationName = sOrgName;
        this.sHarmoniesAttrValue = sHarmoniesAttrValue;
        this.mMBOM150Info = mMBOM150Info;
        this.sMBOM150PhysicalIdentifier = (String) this.mMBOM150Info.get(SELECT_PHYSICALID);
        this.sMBOM150ID = (String) this.mMBOM150Info.get(SELECT_ID);

        this.mPlantInfo = mPlantInfo;
        this.sPlantID = (String) mPlantInfo.get(SELECT_ID);
        this.mHarmonyInfo = mSelectedHarmonyInfo;
        this.mColorInfo = mColorInfo;
        this.isMaterial = isMaterial;

        // default values
        sDefaultColorCode = EnoviaResourceBundle.getProperty(context, "FRCMBOMCentral", "FPDM.ColorManagement.ColorCodeForNonColoredParts", context.getSession().getLanguage());
        if (isMaterial) {
            sObjectType = getSchemaProperty(context, SYMBOLIC_type_FPDM_MBOMMaterial);
        } else {
            sObjectType = getSchemaProperty(context, SYMBOLIC_type_FPDM_MBOMPart);
        }
        sPolicy = getSchemaProperty(context, SYMBOLIC_policy_FPDM_MBOM100);

        // initialize object info
        initializeObjectInformation();
    }

    /**
     * Will fetch all MBOM150 information, and store it under the current object instance. <br>
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private void initializeObjectInformation() throws Exception {

        // get Harmony object info
        if (mHarmonyInfo != null) {
            sHarmonyID = (String) mHarmonyInfo.get(SELECT_ID);
            sHarmonyName = (String) mHarmonyInfo.get(SELECT_NAME);
            if ("NoHarmony".equals(sHarmonyName)) {
                sHarmonyName = "";
            }
            sProductConfigurationPID = (String) mHarmonyInfo.get(STR_PSS_ProductConfigurationPID);
            if (!"".equals(sProductConfigurationPID)) {
                mProductConfigurationInfo = (Map<?, ?>) mHarmonyInfo.get(sProductConfigurationPID);
                sProductConfigurationName = (String) mProductConfigurationInfo.get(SELECT_PRODUCT_CONFIGURATION_NAME);
                sProductConfigurationDescription = (String) mProductConfigurationInfo.get(SELECT_PRODUCT_CONFIGURATION_DESCRIPTION);
            }
        }

        // get Color object info
        if (mColorInfo != null) {
            sColorCode = getAttributeValueFromMap(context, SYMBOLIC_attribute_PSS_ColorCode, mColorInfo);
        } else {
            sColorCode = sDefaultColorCode;
        }

        sObjectName = (String) mMBOM150Info.get(SELECT_NAME) + "-" + (String) mPlantInfo.get(SELECT_NAME);
        sObjectName += sProductConfigurationName.replace("PC", "");
        sObjectName += sHarmonyName.replace("HMY", "");

        sObjectRevision = (String) mMBOM150Info.get(SELECT_REVISION);
        sPartName = getAttributeValueFromMap(context, SYMBOLIC_attribute_PLMEntity_V_Name, mMBOM150Info);

        // check existence
        if (checkObjectExist()) {
            // get MBOM 100 children from database
            getMBOM100Childrens();
        }

        // initialize attributes values from Harmonies attribute
        String sQuantity = "1.0";
        logger.debug("initializeObjectInformation() - sHarmonyID = <" + sHarmonyID + ">");
        if (sHarmonyID != null && sHarmoniesAttrValue != null) {
            String sRegExpHarmonyConfiguration = String.format("%s:\\s*%s[^| ]*%s:\\s*%s", STR_HarmonyId, sHarmonyID, STR_PSS_ProductConfigurationPID, sProductConfigurationPID);
            sHarmonyAssociationRelId = getAttributeValueFromString(sRegExpHarmonyAssociationRelId, sHarmoniesAttrValue, sRegExpHarmonyConfiguration);
            logger.debug("initializeObjectInformation() - sHarmonyAssociationRelId = <" + sHarmonyAssociationRelId + ">");

            // sQuantity = getAttributeValueFromString(sRegExpQuantity, sHarmoniesAttrValue, sRegExpHarmonyConfiguration);
            // sCustomerPartNumber = getAttributeValueFromString(sRegExpCustomerPartNumber, sHarmoniesAttrValue, sRegExpHarmonyConfiguration);
            // sOldMaterialNumber = getAttributeValueFromString(sRegExpOldMaterialNumber, sHarmoniesAttrValue, sRegExpHarmonyConfiguration);
            // sVariantAssemblyPID = getAttributeValueFromString(sRegExpVariantAssemblyPID, sHarmoniesAttrValue, sRegExpHarmonyConfiguration);

            if (sHarmonyAssociationRelId != null && !"".equals(sHarmonyAssociationRelId)) {
                // get Harmony association relationship attribute
                DomainRelationship doRel = DomainRelationship.newInstance(context, sHarmonyAssociationRelId);
                doRel.setName(sHarmonyAssociationRelId);
                Map<String, Object> mHarmonyRelAttributes = doRel.getAttributeMap(context);
                logger.debug("initializeObjectInformation() - mHarmonyRelAttributes = <" + mHarmonyRelAttributes + ">");

                sHarmonyNetWeight = getBasicAttributeValueFromMap(context, getSchemaProperty(context, SYMBOLIC_attribute_PSS_NetWeight), mHarmonyRelAttributes);
                sHarmonyGrossWeight = getBasicAttributeValueFromMap(context, getSchemaProperty(context, SYMBOLIC_attribute_PSS_GrossWeight), mHarmonyRelAttributes);
                sQuantity = getBasicAttributeValueFromMap(context, getSchemaProperty(context, SYMBOLIC_attribute_Quantity), mHarmonyRelAttributes);
                sVariantAssemblyPID = getBasicAttributeValueFromMap(context, getSchemaProperty(context, SYMBOLIC_attribute_PSS_VariantAssemblyPID), mHarmonyRelAttributes);
                sOldMaterialNumber = getBasicAttributeValueFromMap(context, getSchemaProperty(context, SYMBOLIC_attribute_PSS_OldMaterialNumber), mHarmonyRelAttributes);
                sCustomerPartNumber = getBasicAttributeValueFromMap(context, getSchemaProperty(context, SYMBOLIC_attribute_PSS_CustomerPartNumber), mHarmonyRelAttributes);

                if (sVariantAssemblyPID != null && !"".equals(sVariantAssemblyPID)) {
                    DomainObject doVariantAssembly = DomainObject.newInstance(context, sVariantAssemblyPID);
                    String SELECT_ATTRIBUTE_VARIANT_ID = "attribute[" + getSchemaProperty(context, SYMBOLIC_attribute_PSS_VariantID) + "].value";
                    sVariantID = doVariantAssembly.getInfo(context, SELECT_ATTRIBUTE_VARIANT_ID);
                }
            }

            if (logger.isDebugEnabled()) {
                logger.debug("initializeObjectInformation() - sHarmonyNetWeight = <" + sHarmonyNetWeight + "> sHarmonyGrossWeight = <" + sHarmonyGrossWeight + "> sQuantity = <" + sQuantity + ">");
                logger.debug("initializeObjectInformation() - sCustomerPartNumber = <" + sCustomerPartNumber + "> sOldMaterialNumber = <" + sOldMaterialNumber + ">");
                logger.debug("initializeObjectInformation() - sVariantAssemblyPID = <" + sVariantAssemblyPID + "> sVariantID = <" + sVariantID + "> ");
            }
        }

        if (sHarmonyNetWeight == null) {
            sHarmonyNetWeight = getAttributeValueFromMap(context, SYMBOLIC_attribute_PSS_ManufacturingItemExt_PSS_NetWeight, mMBOM150Info);
        }
        if (sHarmonyGrossWeight == null) {
            sHarmonyGrossWeight = getAttributeValueFromMap(context, SYMBOLIC_attribute_PSS_ManufacturingItemExt_PSS_GrossWeight, mMBOM150Info);
        }

        if ("".equals(sQuantity)) {
            sQuantity = "1.0";
        }
        this.fQuantity = Float.parseFloat(sQuantity);
        this.sUnit = getAttributeValueFromMap(context, SYMBOLIC_attribute_PSS_ManufacturingUoMExt_PSS_UnitOfMeasure, mMBOM150Info);
        String sPhantomPartValue = getAttributeValueFromMap(context, SYMBOLIC_attribute_PSS_ManufacturingInstanceExt_PSS_PhantomPart, mMBOM150Info);
        if ("Yes".equals(sPhantomPartValue)) {
            this.sPhantomPart = "Yes";
        }
    }

    /**
     * For debug purpose, display the MBOM 100 object.<br>
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TYPE:").append(this.sObjectType);
        sb.append(" NAME:").append(this.sObjectName);
        sb.append(" REVISION:").append(this.sObjectRevision);
        sb.append(" FCS Reference:").append(this.sFCSReference);
        sb.append(" Quantity:").append(this.fQuantity);
        sb.append(" ID:").append(this.sObjectId);

        return sb.toString();
    }

    /**
     * Construct the MBOM100 BusinessObject
     * @return
     * @throws MatrixException
     */
    private BusinessObject getBusinessObject() throws MatrixException {
        return new BusinessObject(sObjectType, sObjectName, sObjectRevision, VAULT_PRODUCTION);
    }

    /**
     * Check if the current MBOM 100% already exist on the database. Fill object id value if it's the case
     * @throws MatrixException
     */
    public boolean checkObjectExist() throws MatrixException {
        BusinessObject bo = getBusinessObject();

        boolean bExist = bo.exists(context);
        if (bExist) {
            sObjectId = bo.getObjectId(context);
        }
        return bExist;
    }

    /**
     * Create a new MBOM100 object if it doesn't exist yet
     * @throws MatrixException
     */
    public void createObject() throws MatrixException {
        BusinessObject bo = getBusinessObject();

        if (!bo.exists(context)) {
            bo.create(context, sPolicy);
            sObjectId = bo.getObjectId(context);
            // set attributes values
            setAttributes(bo);
            logger.debug("createObject() - Object created = <" + toString() + ">");

        } else {
            sObjectId = bo.getObjectId(context);
            logger.debug("createObject() - bo = <" + sObjectId + "> already exist.");
        }

        if (oParent != null) {
            connectIfNotYetConnected(context, oParent.getBusinessObject(), bo, getSchemaProperty(context, SYMBOLIC_relationship_FPDM_MBOMRel), getMBOMRelationshipAttributes());
        }

        // connect MBOM100 to the Plant
        connectIfNotYetConnected(context, bo, new BusinessObject(sPlantID), getSchemaProperty(context, SYMBOLIC_relationship_FPDM_ScopeLink), null);

        // connect MBOM150 to MBOM100
        connectMBOM150ToMBOM100(context, new BusinessObject(sMBOM150ID), bo);

    }

    /**
     * update attributes values of MBOM100 object and its relationship to its parent
     * @throws MatrixException
     */
    public void updateObject() throws MatrixException {
        BusinessObject bo = getBusinessObject();

        // set attributes values
        setAttributes(bo);

        if (oParent != null) {
            DomainRelationship doRel = getRelationship(context, oParent.getBusinessObject(), bo, getSchemaProperty(context, SYMBOLIC_relationship_FPDM_MBOMRel));
            if (doRel == null) {
                DomainObject doFrom = DomainObject.newInstance(context, oParent.getBusinessObject());
                doRel = doFrom.addToObject(context, new RelationshipType(getSchemaProperty(context, SYMBOLIC_relationship_FPDM_MBOMRel)), bo.getObjectId(context));
            }
            doRel.setAttributeValues(context, getMBOMRelationshipAttributes());
        }

    }

    /**
     * Connect two objects and set relationship attributes values
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param from
     *            the from object
     * @param to
     *            the to object
     * @param relationshipName
     *            relationship name
     * @param mRelAttributes
     *            relationship attributes and theirs values
     * @throws MatrixException
     */
    private void connectIfNotYetConnected(Context context, BusinessObject from, BusinessObject to, String relationshipName, Map<String, Object> mRelAttributes) throws MatrixException {
        boolean alreadyConnected = false;

        // check if all bo exist
        if (!from.exists(context)) {
            logger.error("from object does not exist < " + from + ">");
        }
        if (!to.exists(context)) {
            logger.error("to object does not exist < " + from + ">");
        }

        RelationshipList rlFroms = to.getToRelationship(context, (short) 0, true);
        for (Iterator<Relationship> it = rlFroms.iterator(); it.hasNext() && !alreadyConnected;) {
            Relationship rl = it.next();
            if (relationshipName.equals(rl.getTypeName()) && rl.getFrom().getObjectId(context).equals(from.getObjectId(context))) {
                alreadyConnected = true;
            }
        }

        if (!alreadyConnected) {

            DomainObject doFrom = DomainObject.newInstance(context, from.getObjectId(context));
            DomainRelationship doRel = doFrom.addToObject(context, new RelationshipType(relationshipName), to.getObjectId(context));

            if (mRelAttributes != null) {
                doRel.setAttributeValues(context, mRelAttributes);
            }
        }

    }

    /**
     * Get relationship between two object
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param from
     *            from object
     * @param to
     *            to object
     * @param relationshipName
     *            relationship name
     * @return
     * @throws MatrixException
     */
    private DomainRelationship getRelationship(Context context, BusinessObject from, BusinessObject to, String relationshipName) throws MatrixException {
        DomainRelationship doRel = null;
        boolean alreadyConnected = false;

        // check if all bo exist
        if (!from.exists(context)) {
            logger.error("from object does not exist < " + from + ">");
        }
        if (!to.exists(context)) {
            logger.error("to object does not exist < " + from + ">");
        }

        RelationshipList rlFroms = to.getToRelationship(context, (short) 0, true);

        for (Iterator<Relationship> it = rlFroms.iterator(); it.hasNext() && !alreadyConnected;) {
            Relationship rl = it.next();
            if (relationshipName.equals(rl.getTypeName()) && rl.getFrom().getObjectId(context).equals(from.getObjectId(context))) {
                alreadyConnected = true;
                doRel = new DomainRelationship(rl);
            }
        }

        return doRel;
    }

    /**
     * Get MBOMRel relationship attributes values
     * @return relationship attributes and theirs values
     * @throws FrameworkException
     */
    private Map<String, Object> getMBOMRelationshipAttributes() throws FrameworkException {
        Map<String, Object> mAttributes = new HashMap<String, Object>();

        mAttributes.put(getSchemaProperty(context, SYMBOLIC_attribute_FPDM_Quantity), String.valueOf(this.fQuantity));
        mAttributes.put(getSchemaProperty(context, SYMBOLIC_attribute_FPDM_Unit), this.sUnit);
        mAttributes.put(getSchemaProperty(context, SYMBOLIC_attribute_FPDM_FindNumber), this.sFindNumber);

        return mAttributes;
    }

    /**
     * Initialize MBOM 100 attributes values
     * @throws FrameworkException
     */
    private Map<String, Object> getObjectAttributesValues() throws FrameworkException {
        Map<String, Object> mAttributes = new HashMap<String, Object>();

        mAttributes.put(getSchemaProperty(context, SYMBOLIC_attribute_FPDM_CustomerPartNumber), sCustomerPartNumber);
        mAttributes.put(getSchemaProperty(context, SYMBOLIC_attribute_FPDM_OldMaterialNumber), sOldMaterialNumber);
        mAttributes.put(getSchemaProperty(context, SYMBOLIC_attribute_FPDM_ProductConfigurationId), sProductConfigurationPID);
        mAttributes.put(getSchemaProperty(context, SYMBOLIC_attribute_FPDM_Harmony), sHarmonyName);
        mAttributes.put(getSchemaProperty(context, SYMBOLIC_attribute_FPDM_Synchronized), "No");
        mAttributes.put(getSchemaProperty(context, SYMBOLIC_attribute_FPDM_GenratedFrom), mMBOM150Info.get(SELECT_NAME));
        mAttributes.put(getSchemaProperty(context, SYMBOLIC_attribute_FPDM_FCSReference), getFCSReference());
        mAttributes.put(getSchemaProperty(context, SYMBOLIC_attribute_V_Name), getFCSReference());
        mAttributes.put(getSchemaProperty(context, SYMBOLIC_attribute_FPDM_Unit), this.sUnit);
        mAttributes.put(getSchemaProperty(context, SYMBOLIC_attribute_FPDM_PhantomPart), this.sPhantomPart);

        String sEffectivityDate = getAttributeValueFromMap(context, SYMBOLIC_attribute_PLMReference_V_ApplicabilityDate, mMBOM150Info);
        if (sEffectivityDate != null && !"".equals(sEffectivityDate)) {
            mAttributes.put(getSchemaProperty(context, SYMBOLIC_attribute_FPDM_EffectivityDate), sEffectivityDate);
        }
        mAttributes.put(getSchemaProperty(context, SYMBOLIC_attribute_FPDM_CustomerDescription),
                getAttributeValueFromMap(context, SYMBOLIC_attribute_PSS_ManufacturingItemExt_PSS_CustomerDescription, mMBOM150Info));
        mAttributes.put(getSchemaProperty(context, SYMBOLIC_attribute_FPDM_PDMClass),
                getAttributeValueFromMap(context, SYMBOLIC_attribute_PSS_ManufacturingItemExt_PSS_FCSClassCategory, mMBOM150Info));

        mAttributes.put(getSchemaProperty(context, SYMBOLIC_attribute_FPDM_NetWeight), sHarmonyNetWeight);
        mAttributes.put(getSchemaProperty(context, SYMBOLIC_attribute_FPDM_GrossWeight), sHarmonyGrossWeight);

        mAttributes.put(getSchemaProperty(context, SYMBOLIC_attribute_FPDM_FaureciaShortLengthDescriptionFCS), getDescription());
        mAttributes.put(getSchemaProperty(context, SYMBOLIC_attribute_FPDM_AlternativeDescription),
                getAttributeValueFromMap(context, SYMBOLIC_attribute_PSS_ManufacturingItemExt_PSS_AlternativeDescription, mMBOM150Info));

        String sMakeOrBuy = getAttributeValueFromMap(context, SYMBOLIC_attribute_PSS_ManufacturingInstanceExt_PSS_TypeOfPart, mMBOM150Info);
        if (sMakeOrBuy == null || "".equals(sMakeOrBuy)) {
            sMakeOrBuy = "Make";
        }
        mAttributes.put(getSchemaProperty(context, SYMBOLIC_attribute_FPDM_TypeOfPart), sMakeOrBuy);

        return mAttributes;
    }

    /**
     * Set MBOM 100 attributes values
     * @param bo
     *            MBOM100 business object
     * @throws FrameworkException
     */
    private void setAttributes(BusinessObject bo) throws FrameworkException {

        DomainObject doObject = new DomainObject(bo);
        doObject.setAttributeValues(context, getObjectAttributesValues());

    }

    /**
     * Return parent object
     * @return
     */
    public MBOM100_mxJPO getParent() {
        return oParent;
    }

    /**
     * Initialize parent
     * @param oParent
     */
    public void setParent(MBOM100_mxJPO oParent) {
        this.oParent = oParent;
    }

    public String getMBOM150Identifier() {
        return sMBOM150PhysicalIdentifier;
    }

    /**
     * Return Part name
     * @return
     */
    public String getPartName() {
        return sPartName;
    }

    /**
     * Return Part name
     * @return
     * @throws FrameworkException
     */
    public String getFCSReference() throws FrameworkException {
        if (this.sFCSReference == null) {
            // initialize SAP Part Number
            initFCSReferenceValue();
        }
        return sFCSReference;
    }

    /**
     * Return Part name
     * @return
     */
    public String getPlantName() {
        return (String) mPlantInfo.get(SELECT_NAME);
    }

    /**
     * Return the Harmony name
     * @return
     */
    public String getHarmonyName() {
        return sHarmonyName;
    }

    /**
     * Return the Product Configuration ID
     * @return
     */
    public String getProductConfigurationPID() {
        return sProductConfigurationPID;
    }

    /**
     * Return the Product Configuration Name
     * @return
     */
    public String getProductConfigurationName() {
        return sProductConfigurationName;
    }

    /**
     * Return the Product Configuration Name
     * @return
     */
    public String getProductConfigurationDescription() {
        return sProductConfigurationDescription;
    }

    /**
     * Return the Variant Assembly ID
     * @return
     */
    public String getVariantAssemblyPID() {
        return sVariantAssemblyPID;
    }

    /**
     * Return the Synchronized attribute value
     * @return
     */
    public String getSynchronized() {
        return sSynchronized;
    }

    /**
     * Initialize FCS Reference value
     * @throws FrameworkException
     */
    private void initFCSReferenceValue() throws FrameworkException {

        StringBuilder sbSAPPartNumber = new StringBuilder();
        String sAttrSAPPartNumber = getAttributeValueFromMap(context, SYMBOLIC_attribute_PSS_ManufacturingItemExt_PSS_SAPPartNumber, mMBOM150Info);
        if (STR_CUSTOMER_PART_NUMBER.equals(sAttrSAPPartNumber)) {
            sbSAPPartNumber.append(getAttributeValueFromMap(context, SYMBOLIC_attribute_PSS_ManufacturingItemExt_PSS_CustomerPartNumber, mMBOM150Info));
        } else if (STR_SUPPLIER_PART_NUMBER.equals(sAttrSAPPartNumber)) {
            sbSAPPartNumber.append(getAttributeValueFromMap(context, SYMBOLIC_attribute_PSS_ManufacturingItemExt_PSS_SupplierPartNumber, mMBOM150Info));
        } else {
            // Case of Variant Assembly is not empty
            if (sVariantID != null && !"".equals(sVariantID)) {
                sbSAPPartNumber.append(sVariantID);
            } else {
                // STR_FAURECIA_PART_NUMBER
                sbSAPPartNumber.append(sPartName);
            }
        }
        // Add revision
        if (isLegacy) {
            // Legacy Parts with X
            if (sbSAPPartNumber.charAt(sbSAPPartNumber.length() - 1) == 'X') {
                // suppress the X character at the end of the part name if color code on 3 digits
                if (this.sColorCode.length() == 3 && sbSAPPartNumber.length() == 8) {
                    sbSAPPartNumber = sbSAPPartNumber.deleteCharAt(sbSAPPartNumber.length() - 1);
                }
            }
            if ("FAS".equals(this.sOrganizationName)) {
                // add Engineering revision on name only for Legacy FAS Part
                sbSAPPartNumber.append(StringUtils.substring(this.sObjectRevision, 0, 2));
            }
        } else {
            // add FCS index value to have 12 digits (only 10 digit for Materials)
            if (!this.isMaterial) {
                // add Engineering revision on name only for Part (not for material)
                sbSAPPartNumber.append(StringUtils.substring(this.sObjectRevision, 0, 2));
            }
        }

        // Add color code
        if (this.isMaterial) {
            // No color for Legacy FAS Material
            Matcher mMaterialCodification = patLegacyFASMaterail.matcher(sbSAPPartNumber);
            if (!mMaterialCodification.find()) {
                sbSAPPartNumber.append(sColorCode);
            }
        } else {
            if ("FECT".equals(this.sOrganizationName) && !isLegacy) {
                // Uncolored Part and Packaging variants (FCM), we keep only first character instead of AXX or triple X
                sbSAPPartNumber.append(StringUtils.substring(this.sColorCode, 0, 1)); // add only the first character to have only 10 digits
            } else if ("FECT".equals(this.sOrganizationName) && sbSAPPartNumber.length() > 8) {
                // FECT Legacy Part with name length up to 10 characters: as is, no revision and no X
                sbSAPPartNumber.append("");
            } else if (sDefaultColorCode.equals(sColorCode)) {
                // Uncolored Part, we keep only X instead of triple X
                sbSAPPartNumber.append("X");
            } else {
                // Colored Part
                for (int i = this.sColorCode.length(); i < 3; i++) {
                    // case color code on 2 characters, we add the X before
                    sbSAPPartNumber.append("X");
                }
                sbSAPPartNumber.append(sColorCode);
            }
        }

        // set FCS Reference value
        sFCSReference = sbSAPPartNumber.toString();
        logger.debug("initFCSReferenceValue() - sFCSReference = <" + sFCSReference + "> sVariantID = <" + sVariantID + ">");

    }

    /**
     * Return SAP Part Number value with a link to the MBOM 100%
     * @return
     * @throws FrameworkException
     */
    public String getTopAssemblyName() throws FrameworkException {
        StringBuilder sb = new StringBuilder();

        sb.append("<a href='../common/emxTree.jsp?objectId=");
        sb.append(sObjectId);
        sb.append("&emxSuiteDirectory=FRCMBOMCentral&suiteKey=FRCMBOMCentral' ");
        sb.append(" target='content'>");
        sb.append(getFCSReference());
        sb.append("</a>");

        return sb.toString();
    }

    /**
     * Extract Attribute value from the PSS_Harmonies attribute value corresponding the selected Harmony ID
     * @param sAttrName
     *            Attribute name
     * @param sHarmonies
     *            MBOM 150% attribute PSS_Harmonies value
     * @param sSelectedHarmonyID
     *            Selected Harmony ID
     * @return
     */
    private String getAttributeValueFromString(String sRegExpAttrName, String sHarmonies, String sRegExpHarmonyConfiguration) {
        String sAttributeValue = null;
        String[] savalues = sHarmonies.split("\\|");

        Matcher mAttrName = null;
        Matcher mHarmonyConfiguration = null;
        for (int i = 0; i < savalues.length; i++) {
            mHarmonyConfiguration = Pattern.compile(sRegExpHarmonyConfiguration).matcher(savalues[i]);
            if (mHarmonyConfiguration.find()) {
                mAttrName = Pattern.compile(sRegExpAttrName).matcher(savalues[i]);
                if (mAttrName.find()) {
                    sAttributeValue = mAttrName.group(1);
                }
            }
        }
        return sAttributeValue;
    }

    public float getQuantity() {
        return fQuantity;
    }

    public void setQuantity(float fQuantity) {
        this.fQuantity = fQuantity;
    }

    public void addQuantity(float fQuantity) {
        this.fQuantity += fQuantity;
    }

    public boolean hasTheSameParent(MBOM100_mxJPO oMBOM100) {
        MBOM100_mxJPO oMBOM100Parent = oMBOM100.getParent();
        if (this.oParent != null && oMBOM100Parent != null && this.oParent.getMBOM150Identifier().equals(oMBOM100Parent.getMBOM150Identifier()))
            if (this.sMBOM150PhysicalIdentifier.equals(oMBOM100.getMBOM150Identifier())) {
                return true;
            }
        return false;
    }

    public boolean isMaterial() {
        return isMaterial;
    }

    // when the short description is empty, we return the MBOM150% description which it's the same than the Part
    private String getDescription() throws FrameworkException {
        StringBuilder sbDescription = new StringBuilder();
        sbDescription.append(getAttributeValueFromMap(context, SYMBOLIC_attribute_PSS_ManufacturingItemExt_PSS_FaureciaShortLengthDescriptionFCS, mMBOM150Info));

        // For MBOM150% (top level part) : description = MBOM 150% Faurecia Short Length desc + PC Desc(if PC exist) + Color Desc (if Color exist)
        // For MBOM150% (no top level part) : description = MBOM 150% Faurecia Short Length desc + Color Desc (if Color exist)
        if (this.mProductConfigurationInfo != null) {
            sbDescription.append(" ");
            sbDescription.append(getBasicAttributeValueFromMap(context, SELECT_DESCRIPTION, this.mProductConfigurationInfo));
        }
        if (this.mColorInfo != null) {
            sbDescription.append(" ");
            sbDescription.append(getBasicAttributeValueFromMap(context, SELECT_DESCRIPTION, this.mColorInfo));
        }

        return sbDescription.toString();
    }

    public boolean isLegacy() {
        return isLegacy;
    }

    public void setLegacy(boolean isLegacy) {
        this.isLegacy = isLegacy;
    }

    public void setFindNumber(int iFindNumber) {
        this.sFindNumber = String.format("%1$04d", iFindNumber);
    }

    /**
     * Connect The MBOM 150% object to its related MBOM 100% object
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param from
     *            the from object
     * @param to
     *            the to object
     * @throws MatrixException
     */
    private void connectMBOM150ToMBOM100(Context context, BusinessObject from, BusinessObject to) throws MatrixException {
        boolean bPushedContext = false;

        try {
            boolean alreadyConnected = false;

            String relationshipName = getSchemaProperty(context, SYMBOLIC_relationship_FPDM_GeneratedMBOM);
            String PERSON_USER_AGENT = getSchemaProperty(context, "person_UserAgent");

            // check if all business object exist
            if (!from.exists(context)) {
                logger.error("from object does not exist < " + from + ">");
            }
            if (!to.exists(context)) {
                logger.error("to object does not exist < " + from + ">");
            }

            RelationshipList rlFroms = to.getToRelationship(context, (short) 0, true);
            for (Iterator<Relationship> it = rlFroms.iterator(); it.hasNext() && !alreadyConnected;) {
                Relationship rl = it.next();
                if (relationshipName.equals(rl.getTypeName()) && rl.getFrom().getObjectId(context).equals(from.getObjectId(context))) {
                    alreadyConnected = true;
                }
            }

            if (!alreadyConnected) {
                DomainObject doFrom = DomainObject.newInstance(context, from.getObjectId(context));
                Access acAccessMask = doFrom.getAccessMask(context);
                // Test if Business Object has the fromconnect access right
                boolean bHasFromConnectAccess = acAccessMask.hasFromConnectAccess();

                if (!bHasFromConnectAccess) {
                    // Push context to User Agent
                    ContextUtil.pushContext(context, PERSON_USER_AGENT, null, null);
                    bPushedContext = true;
                }

                doFrom.addToObject(context, new RelationshipType(relationshipName), to.getObjectId(context));

            }
        } catch (RuntimeException e) {
            logger.debug("connectMBOM150ToMBOM100() - Exception encountered.", e);
            throw e;
        } catch (Exception e) {
            logger.debug("connectMBOM150ToMBOM100() - Exception encountered.", e);
            throw e;
        } finally {
            if (bPushedContext) {
                // Pop context from User Agent
                ContextUtil.popContext(context);
                bPushedContext = false;
            }
        }

    }

    /**
     * Get the current MBOM100 object ID
     * @return
     */
    public String getObjectId() {
        return sObjectId;
    }

    /**
     * Add a new MBOM100 child on children list of the current MBOM100 (found on the resolution)
     * @param sChildObjectId
     */
    public void addNewChildObjectId(String sChildObjectId) {
        if (!alChildrenObjectsIds.contains(sChildObjectId)) {
            alChildrenObjectsIds.add(sChildObjectId);
        }
    }

    /**
     * Get all children found by the update process
     * @return
     */
    public ArrayList<String> getChildrenObjectsIds() {
        return alChildrenObjectsIds;
    }

    /**
     * MBOM100 children list on the database
     * @return
     */
    public Map<String, String> getChildren() {
        return mChildrenInDB;
    }

    /**
     * Get MBOM100 children list on the database
     * @throws MatrixException
     */
    private void getMBOM100Childrens() throws MatrixException {
        mChildrenInDB = new HashMap<String, String>();

        DomainObject doMBOM100 = DomainObject.newInstance(context, sObjectId);

        // Call Expand
        String sRelationPattern = getSchemaProperty(context, SYMBOLIC_relationship_FPDM_MBOMRel);
        String sTypePattern = getSchemaProperty(context, SYMBOLIC_type_FPDM_MBOMPart);
        MapList mlMBOM100 = doMBOM100.getRelatedObjects(context, sRelationPattern, sTypePattern, new StringList(SELECT_ID), new StringList(SELECT_RELATIONSHIP_ID), false, true, (short) 1, null, null,
                0);
        for (Iterator<?> iterator = mlMBOM100.iterator(); iterator.hasNext();) {
            Map<?, ?> mChildInfos = (Map<?, ?>) iterator.next();
            logger.debug("getMBOM100Childrens() - mChildInfos = <" + mChildInfos + ">");

            mChildrenInDB.put((String) mChildInfos.get(SELECT_ID), (String) mChildInfos.get(SELECT_RELATIONSHIP_ID));
        }

    }
}