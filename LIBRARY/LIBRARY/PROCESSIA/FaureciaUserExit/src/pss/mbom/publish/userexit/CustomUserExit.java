package pss.mbom.publish.userexit;

import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.eMatrixDateFormat;
import com.matrixone.apps.framework.ui.UIUtil;
import com.matrixone.apps.library.LibraryCentralConstants;

import dsis.com.userexit.DataHolder;
import dsis.com.userexit.UserExit;
import matrix.db.Context;
import matrix.util.Pattern;
import matrix.util.StringList;

public class CustomUserExit implements UserExit {

    final static String TYPE_TOOLING_PART = PropertyUtil.getSchemaProperty("type_ToolingPart");

    final static String TYPE_PROCESS_CONTINUOUS_PROVIDE = PropertyUtil.getSchemaProperty("type_ProcessContinuousProvide");

    final static String TYPE_PSS_COLOR_OPTION = PropertyUtil.getSchemaProperty("type_PSS_ColorOption");

    final static String TYPE_PSS_VARIANT_ASSEMBLY = PropertyUtil.getSchemaProperty("type_PSS_VariantAssembly");

    final static String RELATIONSHIP_PSS_PARTTOOL = PropertyUtil.getSchemaProperty("relationship_PSS_PartTool");

    final static String RELATIONSHIP_PSS_MATRIAL = PropertyUtil.getSchemaProperty("relationship_PSS_Material");

    final static String RELATIONSHIP_PSS_COLOR_LIST = PropertyUtil.getSchemaProperty("relationship_PSS_ColorList");

    final static String RELATIONSHIP_PSS_PART_VARIANT_ASSEMBLY = PropertyUtil.getSchemaProperty("relationship_PSS_PartVariantAssembly");

    final static String SELECT_PHYSICALID = "physicalid";

    final static String STATE_CANCELLED = "Cancelled";
    
    final static String STATE_OBSOLETE = "Obsolete";

    final static String ATTRIBUTE_ESTIMATED_COST = PropertyUtil.getSchemaProperty("attribute_EstimatedCost");

    final static String ATTRIBUTE_LEAD_TIME = PropertyUtil.getSchemaProperty("attribute_LeadTime");

    final static String ATTRIBUTE_PSS_STRUCTURE_NODE = PropertyUtil.getSchemaProperty("attribute_PSS_StructureNode");

    final static String ATTRIBUTE_PRODUCTION_MAKE_BUY_CODE = PropertyUtil.getSchemaProperty("attribute_ProductionMakeBuyCode");

    final static String ATTRIBUTE_SERVICE_MAKE_BUY_CODE = PropertyUtil.getSchemaProperty("attribute_ServiceMakeBuyCode");

    final static String ATTRIBUTE_TARGET_COST = PropertyUtil.getSchemaProperty("attribute_TargetCost");

    final static String ATTRIBUTE_UNIT_OF_MEASURE = PropertyUtil.getSchemaProperty("attribute_UnitofMeasure");

    final static String ATTRIBUTE_CURRENT_VERSION = PropertyUtil.getSchemaProperty("attribute_CurrentVersion");

    final static String ATTRIBUTE_DESIGN_PURCHASE = PropertyUtil.getSchemaProperty("attribute_DesignPurchase");

    final static String ATTRIBUTE_END_ITEM = PropertyUtil.getSchemaProperty("attribute_EndItem");

    final static String ATTRIBUTE_END_ITEM_OVERRIDE_ENABLED = PropertyUtil.getSchemaProperty("attribute_EndItemOverrideEnabled");

    final static String ATTRIBUTE_MATERIAL_CATEGORY = PropertyUtil.getSchemaProperty("attribute_MaterialCategory");

    final static String ATTRIBUTE_SPARE_PART = PropertyUtil.getSchemaProperty("attribute_SparePart");

    final static String ATTRIBUTE_PSS_COLORABLE = PropertyUtil.getSchemaProperty("attribute_PSS_Colorable");

    final static String ATTRIBUTE_EFFECTIVITY_DATE = PropertyUtil.getSchemaProperty("attribute_EffectivityDate");

    final static String ATTRIBUTE_PART_CLASSIFICATION = PropertyUtil.getSchemaProperty("attribute_PartClassification");

    final static String ATTRIBUTE_WEIGHT = PropertyUtil.getSchemaProperty("attribute_Weight");

    final static String ATTRIBUTE_RELATIONSHIP_UUID = PropertyUtil.getSchemaProperty("attribute_RelationshipUUID");

    final static String ATTRIBUTE_COMPONENT_LOCATION = PropertyUtil.getSchemaProperty("attribute_ComponentLocation");

    final static String ATTRIBUTE_REFERENCE_DESIGNATOR = PropertyUtil.getSchemaProperty("attribute_ReferenceDesignator");

    final static String ATTRIBUTE_QUANTITY = PropertyUtil.getSchemaProperty("attribute_Quantity");

    final static String ATTRIBUTE_SOURCE = PropertyUtil.getSchemaProperty("attribute_Source");

    final static String ATTRIBUTE_NOTES = PropertyUtil.getSchemaProperty("attribute_Notes");

    final static String ATTRIBUTE_PROCESS = PropertyUtil.getSchemaProperty("attribute_Process");

    final static String ATTRIBUTE_FIND_NUMBER = PropertyUtil.getSchemaProperty("attribute_FindNumber");

    final static String ATTRIBUTE_END_EFFECTIVITY_DATE = PropertyUtil.getSchemaProperty("attribute_EndEffectivityDate");

    final static String ATTRIBUTE_START_EFFECTIVITY_DATE = PropertyUtil.getSchemaProperty("attribute_StartEffectivityDate");

    final static String ATTRIBUTE_USAGE = PropertyUtil.getSchemaProperty("attribute_Usage");

    final static String ATTRIBUTE_PSS_CARRY_OVER = PropertyUtil.getSchemaProperty("attribute_PSS_Carry_over");
    
    final static String ATTRIBUTE_PSS_EBOM_CADMASS = PropertyUtil.getSchemaProperty("attribute_PSS_EBOM_CADMass");

    final static String TYPE_PROCESSCONTINOUSCREATEMATERIAL = PropertyUtil.getSchemaProperty("type_ProcessContinuousCreateMaterial");

    final static String POLICY_STANDARDPART = PropertyUtil.getSchemaProperty("policy_StandardPart");

    // TIGTK-3727
    final static SimpleDateFormat DATE_FORMAT_PUBLISH_EXPORT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    final static String TYPE_VPMREFERENCE = PropertyUtil.getSchemaProperty("type_VPMReference");

    @Override
    public int executionSequence() {
        return 0;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public DataHolder extractAttributesOnProd(Context context, String objectId) {
        try {
            DomainObject dObj = DomainObject.newInstance(context, objectId);
            String objType = dObj.getInfo(context, DomainConstants.SELECT_TYPE);
            String objPolicy = dObj.getInfo(context, DomainConstants.SELECT_POLICY);
            DataHolder dataHolder = null;
            if (objType.equals(DomainConstants.TYPE_PART) || objType.equals(TYPE_TOOLING_PART)) {
                Map attributes = dObj.getAttributeMap(context, true);

                Pattern relPattern = new Pattern(RELATIONSHIP_PSS_MATRIAL);
                relPattern.addPattern(RELATIONSHIP_PSS_COLOR_LIST);
                relPattern.addPattern(RELATIONSHIP_PSS_PART_VARIANT_ASSEMBLY);
                relPattern.addPattern(DomainConstants.RELATIONSHIP_CLASSIFIED_ITEM);
                relPattern.addPattern(RELATIONSHIP_PSS_PARTTOOL);

                Pattern typePattern = new Pattern(TYPE_PROCESS_CONTINUOUS_PROVIDE);
                typePattern.addPattern(TYPE_PSS_COLOR_OPTION);
                typePattern.addPattern(TYPE_PSS_VARIANT_ASSEMBLY);
                typePattern.addPattern(DomainConstants.TYPE_PART_FAMILY);
                typePattern.addPattern(LibraryCentralConstants.TYPE_CLASSIFICATION);
                typePattern.addPattern(TYPE_PROCESSCONTINOUSCREATEMATERIAL);
                typePattern.addPattern(TYPE_VPMREFERENCE);

                StringList objectSelects = new StringList(SELECT_PHYSICALID);
                objectSelects.addElement(DomainConstants.SELECT_TYPE);
                objectSelects.addElement(DomainConstants.SELECT_CURRENT);

                MapList mapList = dObj.getRelatedObjects(context, relPattern.getPattern(), typePattern.getPattern(), objectSelects, null, true, true, (short) 1, null, null, 0);
                Iterator<Map> itr = mapList.iterator();

                StringList colorList = new StringList();
                StringList materialList = new StringList();
                StringList variantAssemblyLIst = new StringList();
                StringList classificationList = new StringList();
                StringList toolingList = new StringList();

                while (itr.hasNext()) {
                    Map objInfo = itr.next();
                    String typeStr = (String) objInfo.get(DomainConstants.SELECT_TYPE);
                    String physicalId = (String) objInfo.get(SELECT_PHYSICALID);
                    String currentStr = (String) objInfo.get(DomainConstants.SELECT_CURRENT);
                    DomainObject domObj = DomainObject.newInstance(context, physicalId);

                    if (typeStr.equals(TYPE_PSS_COLOR_OPTION)) {
                        colorList.addElement(physicalId);
                    } else if (UIUtil.isNotNullAndNotEmpty(currentStr) && !currentStr.equalsIgnoreCase(STATE_CANCELLED) &&  !currentStr.equalsIgnoreCase(STATE_OBSOLETE)
                            && (domObj.isKindOf(context, TYPE_PROCESS_CONTINUOUS_PROVIDE) || domObj.isKindOf(context, TYPE_PROCESSCONTINOUSCREATEMATERIAL))) {
                        materialList.addElement(physicalId);
                    } else if (typeStr.equals(TYPE_PSS_VARIANT_ASSEMBLY)) {
                        variantAssemblyLIst.addElement(physicalId);
                    } else if (typeStr.equals(DomainConstants.TYPE_PART_FAMILY) || domObj.isKindOf(context, LibraryCentralConstants.TYPE_CLASSIFICATION)) {
                        classificationList.addElement(physicalId);
                    }else if ((typeStr.equals(TYPE_VPMREFERENCE)) && (!currentStr.equalsIgnoreCase("Cancelled")) && (!currentStr.equalsIgnoreCase("Obsolete"))){
                    toolingList.addElement(physicalId);
                    }
                }

                dataHolder = new DataHolder("XPDMProduct", "PSS_PublishedPart");
                // connected object list
                dataHolder.addExtensionAttribute("string", "PSSColorList", FrameworkUtil.join(colorList, "|"));
                dataHolder.addExtensionAttribute("string", "PSSMaterialList", FrameworkUtil.join(materialList, "|"));
                dataHolder.addExtensionAttribute("string", "PSSVariantAssemblyList", FrameworkUtil.join(variantAssemblyLIst, "|"));
                dataHolder.addExtensionAttribute("string", "PSSClassificationList", FrameworkUtil.join(classificationList, "|"));
                dataHolder.addExtensionAttribute("string", "PSSToolingList", FrameworkUtil.join(toolingList, "|"));

                // object attributes
                String pssPartEstimatedCost = (String) attributes.get(ATTRIBUTE_ESTIMATED_COST);
                if (UIUtil.isNotNullAndNotEmpty(pssPartEstimatedCost)) {
                    dataHolder.addExtensionAttribute("double", "PSSPartEstimatedCost", pssPartEstimatedCost);
                }

                String pssPartLeadTime = (String) attributes.get(ATTRIBUTE_LEAD_TIME);
                if (UIUtil.isNotNullAndNotEmpty(pssPartLeadTime)) {
                    dataHolder.addExtensionAttribute("string", "PSSPartLeadTime", pssPartLeadTime);
                }

                String pssPartStructureNode = (String) attributes.get(ATTRIBUTE_PSS_STRUCTURE_NODE);
                if (UIUtil.isNotNullAndNotEmpty(pssPartStructureNode)) {
                    dataHolder.addExtensionAttribute("string", "PSSPartStructureNode", pssPartStructureNode);
                }

                String pssPartProductionMakeBuyCode = (String) attributes.get(ATTRIBUTE_PRODUCTION_MAKE_BUY_CODE);
                if (UIUtil.isNotNullAndNotEmpty(pssPartProductionMakeBuyCode)) {
                    dataHolder.addExtensionAttribute("string", "PSSPartProductionMakeBuyCode", pssPartProductionMakeBuyCode);
                }

                String pssPartSrviceMakeBuyCode = (String) attributes.get(ATTRIBUTE_SERVICE_MAKE_BUY_CODE);
                if (UIUtil.isNotNullAndNotEmpty(pssPartSrviceMakeBuyCode)) {
                    dataHolder.addExtensionAttribute("string", "PSSPartServiceMakeBuyCode", pssPartSrviceMakeBuyCode);
                }

                String pssPartTargetCost = (String) attributes.get(ATTRIBUTE_TARGET_COST);
                if (UIUtil.isNotNullAndNotEmpty(pssPartTargetCost)) {
                    dataHolder.addExtensionAttribute("double", "PSSPartTargetCost", pssPartTargetCost);
                }

                String pssPartUnitOfMeasure = (String) attributes.get(ATTRIBUTE_UNIT_OF_MEASURE);
                if (UIUtil.isNotNullAndNotEmpty(pssPartUnitOfMeasure)) {
                    dataHolder.addExtensionAttribute("string", "PSSPartUnitofMeasure", pssPartUnitOfMeasure);
                }

                String pssPartCurrentVersion = (String) attributes.get(ATTRIBUTE_CURRENT_VERSION);
                if (UIUtil.isNotNullAndNotEmpty(pssPartCurrentVersion)) {
                    dataHolder.addExtensionAttribute("integer", "PSSPartCurrentVersion", pssPartCurrentVersion);
                }

                String pssPartDesignPurchase = (String) attributes.get(ATTRIBUTE_DESIGN_PURCHASE);
                if (UIUtil.isNotNullAndNotEmpty(pssPartDesignPurchase)) {
                    dataHolder.addExtensionAttribute("string", "PSSPartDesignPurchase", pssPartDesignPurchase);
                }

                String pssPartEndItem = (String) attributes.get(ATTRIBUTE_END_ITEM);
                if (UIUtil.isNotNullAndNotEmpty(pssPartEndItem)) {
                    dataHolder.addExtensionAttribute("string", "PSSPartEndItem", pssPartEndItem);
                }

                String pssPartEndItemOverrideEnabled = (String) attributes.get(ATTRIBUTE_END_ITEM_OVERRIDE_ENABLED);
                if (UIUtil.isNotNullAndNotEmpty(pssPartEndItemOverrideEnabled)) {
                    dataHolder.addExtensionAttribute("string", "PSSPartEndItemOverrideEnabled", pssPartEndItemOverrideEnabled);
                }

                String pssPartMaterialCategory = (String) attributes.get(ATTRIBUTE_MATERIAL_CATEGORY);
                if (UIUtil.isNotNullAndNotEmpty(pssPartMaterialCategory)) {
                    dataHolder.addExtensionAttribute("string", "PSSPartMaterialCategory", pssPartMaterialCategory);
                }

                String pssPartSparePart = (String) attributes.get(ATTRIBUTE_SPARE_PART);
                if (UIUtil.isNotNullAndNotEmpty(pssPartSparePart)) {
                    dataHolder.addExtensionAttribute("string", "PSSPartSparePart", pssPartSparePart);
                }

                String pssPartColorable = (String) attributes.get(ATTRIBUTE_PSS_COLORABLE);
                if (UIUtil.isNotNullAndNotEmpty(pssPartColorable)) {
                    dataHolder.addExtensionAttribute("string", "PSSPartColorable", pssPartColorable);
                }

                String pssPartEffectivityDate = (String) attributes.get(ATTRIBUTE_EFFECTIVITY_DATE);
                if (UIUtil.isNotNullAndNotEmpty(pssPartEffectivityDate)) {
                    // TIGTK-3727
                    dataHolder.addExtensionAttribute("dateTime", "PSSPartEffectivityDate", DATE_FORMAT_PUBLISH_EXPORT.format(eMatrixDateFormat.getJavaDate(pssPartEffectivityDate)));
                }

                String partClassification = (String) attributes.get(ATTRIBUTE_PART_CLASSIFICATION);
                if (UIUtil.isNotNullAndNotEmpty(partClassification)) {
                    dataHolder.addExtensionAttribute("string", "PSSPartPartClassification", partClassification);
                }

                String pssPartWeight = (String) attributes.get(ATTRIBUTE_WEIGHT);
                if (UIUtil.isNotNullAndNotEmpty(pssPartWeight)) {
                    dataHolder.addExtensionAttribute("double", "PSSPartWeight", pssPartWeight);
                }
               
                String pssModifiedDate = dObj.getInfo(context, DomainConstants.SELECT_MODIFIED);
                if (UIUtil.isNotNullAndNotEmpty(pssModifiedDate)) {
                    dataHolder.addExtensionAttribute("dateTime", "PSSPublishDate", DATE_FORMAT_PUBLISH_EXPORT.format(eMatrixDateFormat.getJavaDate(pssModifiedDate)));
                }

               dataHolder.addExtensionAttribute("boolean", "PSSAllowCreateMBOM", "TRUE");
 
                String userName = PropertyUtil.getRPEValue(context, "CURRENT_USER",true);
                String strLoggedUserSecurityContext = PersonUtil.getDefaultSecurityContext(context, userName);
                String assignedRoles = (strLoggedUserSecurityContext.split("[.]")[0]);
                //if (UIUtil.isNotNullAndNotEmpty(objPolicy) && POLICY_STANDARDPART.equals(objPolicy) && "PSS_GTS_Engineer".equals(assignedRoles)) {
                if (UIUtil.isNotNullAndNotEmpty(objPolicy) && POLICY_STANDARDPART.equals(objPolicy)) {
                    dataHolder.addExtensionAttribute("string", "PSSStandardReference", "Standard");
                }
            }

            return dataHolder;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public DataHolder extractAttributesOnProdInst(Context context, String productInstId) {
        try {
            DomainRelationship domRel = DomainRelationship.newInstance(context, productInstId);
            MapList infoList = DomainRelationship.getInfo(context, new String[] { productInstId }, new StringList(DomainConstants.SELECT_RELATIONSHIP_NAME));
            String relType = (String) ((Map) infoList.get(0)).get(DomainConstants.SELECT_RELATIONSHIP_NAME);
            Map attributes = domRel.getAttributeMap(context, true);

            DataHolder dataHolder = new DataHolder("MyCustoName", "PSS_PublishedEBOM");
            dataHolder.addExtensionAttribute("string", "PSSInstanceName", relType);

            // relationship attributes
            String pssEBOMRelationshipUUID = (String) attributes.get(ATTRIBUTE_RELATIONSHIP_UUID);
            if (UIUtil.isNotNullAndNotEmpty(pssEBOMRelationshipUUID)) {
                dataHolder.addExtensionAttribute("string", "PSSEBOMRelationshipUUID", pssEBOMRelationshipUUID);
            }

            String pssEBOMComponentLocation = (String) attributes.get(ATTRIBUTE_COMPONENT_LOCATION);
            if (UIUtil.isNotNullAndNotEmpty(pssEBOMComponentLocation)) {
                dataHolder.addExtensionAttribute("string", "PSSEBOMComponentLocation", pssEBOMComponentLocation);
            }

            String pssEBOMReferenceDesignator = (String) attributes.get(ATTRIBUTE_REFERENCE_DESIGNATOR);
            if (UIUtil.isNotNullAndNotEmpty(pssEBOMReferenceDesignator)) {
                dataHolder.addExtensionAttribute("string", "PSSEBOMReferenceDesignator", pssEBOMReferenceDesignator);
            }

            String pssEBOMQuantity = (String) attributes.get(ATTRIBUTE_QUANTITY);
            if (UIUtil.isNotNullAndNotEmpty(pssEBOMQuantity)) {
                dataHolder.addExtensionAttribute("double", "PSSEBOMQuantity", pssEBOMQuantity);
            }

            String pssEBOMSource = (String) attributes.get(ATTRIBUTE_SOURCE);
            if (UIUtil.isNotNullAndNotEmpty(pssEBOMSource)) {
                dataHolder.addExtensionAttribute("string", "PSSEBOMSource", pssEBOMSource);
            }

            String pssEBOMNotes = (String) attributes.get(ATTRIBUTE_NOTES);
            if (UIUtil.isNotNullAndNotEmpty(pssEBOMNotes)) {
                dataHolder.addExtensionAttribute("string", "PSSEBOMNotes", pssEBOMNotes);
            }

            String pssEBOMProcess = (String) attributes.get(ATTRIBUTE_PROCESS);
            if (UIUtil.isNotNullAndNotEmpty(pssEBOMProcess)) {
                dataHolder.addExtensionAttribute("string", "PSSEBOMProcess", pssEBOMProcess);
            }

            String pssEBOMFindNumber = (String) attributes.get(ATTRIBUTE_FIND_NUMBER);
            if (UIUtil.isNotNullAndNotEmpty(pssEBOMFindNumber)) {
                dataHolder.addExtensionAttribute("string", "PSSEBOMFindNumber", pssEBOMFindNumber);
            }

            String pssEBOMEndEffectivityDate = (String) attributes.get(ATTRIBUTE_END_EFFECTIVITY_DATE);
            if (UIUtil.isNotNullAndNotEmpty(pssEBOMEndEffectivityDate)) {
                // TIGTK-3727
                dataHolder.addExtensionAttribute("dateTime", "PSSEBOMEndEffectivityDate", DATE_FORMAT_PUBLISH_EXPORT.format(eMatrixDateFormat.getJavaDate(pssEBOMEndEffectivityDate)));
            }

            String pssEBOMStartEffectivityDate = (String) attributes.get(ATTRIBUTE_START_EFFECTIVITY_DATE);
            if (UIUtil.isNotNullAndNotEmpty(pssEBOMStartEffectivityDate)) {
                // TIGTK-3727
                dataHolder.addExtensionAttribute("dateTime", "PSSEBOMStartEffectivityDate", DATE_FORMAT_PUBLISH_EXPORT.format(eMatrixDateFormat.getJavaDate(pssEBOMStartEffectivityDate)));
            }

            String pssEBOMUsage = (String) attributes.get(ATTRIBUTE_USAGE);
            if (UIUtil.isNotNullAndNotEmpty(pssEBOMUsage)) {
                dataHolder.addExtensionAttribute("string", "PSSEBOMUsage", pssEBOMUsage);
            }

            String pssEBOMCarryOver = (String) attributes.get(ATTRIBUTE_PSS_CARRY_OVER);
            if (UIUtil.isNotNullAndNotEmpty(pssEBOMCarryOver)) {
                dataHolder.addExtensionAttribute("string", "PSSEBOMCarryOver", pssEBOMCarryOver);
            }

            return dataHolder;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public DataHolder extractAttributesOnRep3D(Context arg0, String arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DataHolder extractAttributesOnRep3DInst(Context arg0, String arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MapList extractCustoChild(Context context, String objectId) {
        try {
            System.out.println("USER EXIT ===> START");
            StringList objectSelects = new StringList();
            objectSelects.add(DomainConstants.SELECT_ID);
            objectSelects.add(DomainConstants.SELECT_REVISION);
            objectSelects.add(DomainConstants.SELECT_TYPE);
            objectSelects.add(DomainConstants.SELECT_CURRENT);
            objectSelects.add(DomainConstants.SELECT_POLICY);
            objectSelects.add(DomainConstants.SELECT_OWNER);
            objectSelects.add(DomainConstants.SELECT_ORGANIZATION);
            objectSelects.add(DomainConstants.SELECT_DESCRIPTION);
            objectSelects.add(DomainConstants.SELECT_NAME);
            objectSelects.add(DomainConstants.SELECT_ORIGINATED);
            objectSelects.add(DomainConstants.SELECT_MODIFIED);
            objectSelects.add("revindex");
            objectSelects.add("minororder");
            objectSelects.add("previousminor");
            objectSelects.add("physicalid");
            objectSelects.add("logicalid");
            objectSelects.add("majorid");
            objectSelects.add("project");

            StringList relSelects = new StringList();
            relSelects.add(DomainConstants.SELECT_RELATIONSHIP_ID);
            relSelects.add(DomainConstants.SELECT_RELATIONSHIP_TYPE);
            relSelects.add(DomainConstants.SELECT_RELATIONSHIP_NAME);
            relSelects.add("to.name");
            relSelects.add("to.type");
            relSelects.add("to.id");
            relSelects.add("attribute[Relationship UUID]");
            relSelects.add("originated[connection]");
            relSelects.add("modified[connection]");
            relSelects.add("owner[connection]");
            relSelects.add("owner[connection].id");
            relSelects.add("organization[connection]");
            relSelects.add("project[connection]");
            relSelects.add("logicalid[connection]");
            relSelects.add("majorid[connection]");
            relSelects.add("attribute[Reference Designator]");

            DomainObject dom = DomainObject.newInstance(context, objectId);

            Pattern relPattern = new Pattern(DomainConstants.RELATIONSHIP_ALTERNATE);
            relPattern.addPattern(DomainConstants.RELATIONSHIP_SPARE_PART);

            Pattern typePattern = new Pattern(DomainConstants.TYPE_PART);

            MapList list = dom.getRelatedObjects(context, relPattern.getPattern(), typePattern.getPattern(), objectSelects, relSelects, false, true, (short) 1, null, null, 0);
            System.out.println("USER EXIT maplist===>" + list);

            if (list.size() == 0) {
                list = null;
            } 
            System.out.println("USER EXIT ===> END");
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public MapList extractDocument(Context context, String productId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    /**
     * This method is just for test. But mandatory to execute the extraction successful
     */
    public Map<String, String> getUserInfoForPostProcessing(Context paramContext) {
        LinkedHashMap<String, String> userInfoMap = new LinkedHashMap<String, String>();
        try {
            String strCurrentUser = PropertyUtil.getRPEValue(paramContext, "CURRENT_USER", true);
            if (UIUtil.isNullOrEmpty(strCurrentUser)) {
                strCurrentUser = "";
            }
            userInfoMap.put("MailID", strCurrentUser);
            userInfoMap.put("Site", "FRANCE");
        } catch (FrameworkException e) {
            e.printStackTrace();
        }
        return userInfoMap;
    }
}