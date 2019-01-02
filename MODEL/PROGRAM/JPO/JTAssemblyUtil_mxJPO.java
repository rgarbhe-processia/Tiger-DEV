
/**
 * <H2>copyright Steria - All Rights Reserved.</H2> ProgramName : JTAssemblyUtil
 * <H1>Description</H1>
 */

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkProperties;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.MatrixException;
import matrix.util.Pattern;
import matrix.util.SelectList;
import matrix.util.StringList;

public class JTAssemblyUtil_mxJPO {

    public static final String FORMAT_JT = PropertyUtil.getSchemaProperty("format_JT");

    public static final String RELATIONSHIP_ACTIVE_VERSION = PropertyUtil.getSchemaProperty("relationship_ActiveVersion");

    public static final String RELATIONSHIP_CAD_SUBCOMPONENT = PropertyUtil.getSchemaProperty("relationship_CADSubComponent");

    public static final String TYPE_3DVIEWABLE = PropertyUtil.getSchemaProperty("type_3DViewable");

    // TIGTK-3663 : START
    public static final String TYPE_VIEWABLE = PropertyUtil.getSchemaProperty("type_Viewable");

    public static final String TYPE_CADMODEL = PropertyUtil.getSchemaProperty("type_CADModel");
    // TIGTK-3663 : END

    public static final String RELATIONSHIP_VIEWABLE = PropertyUtil.getSchemaProperty("relationship_Viewable");

    public static final String REL_PATTERN_ACTIVE_CADSUB = RELATIONSHIP_ACTIVE_VERSION + "," + RELATIONSHIP_CAD_SUBCOMPONENT;

    public static final String REL_PATTERN_ACTIVE_VIEWABLE = RELATIONSHIP_ACTIVE_VERSION + "," + RELATIONSHIP_VIEWABLE;

    public static final Pattern PATTERN_3DVIEWABLE = new Pattern(TYPE_3DVIEWABLE);

    public static final String RELATIONSHIP_VERSIONOF = PropertyUtil.getSchemaProperty("relationship_VersionOf");

    public static final String SELECT_MAJOR_TYPE = "from[" + RELATIONSHIP_VERSIONOF + "].to.type";

    public static final String SELECT_MAJOR_ID = "from[" + RELATIONSHIP_VERSIONOF + "].to.id";

    /**
     * Constructor
     * @param context
     *            the eMatrix Context object
     * @param args
     *            holds no arguments
     * @throws Exception
     */
    public JTAssemblyUtil_mxJPO(Context context, String[] args) throws Exception {
        // sOrganizationName = JTAssemblyUtil_mxJPO.getName(context);
        // sLanguage = context.getSession().getLanguage();
    }

    /**
     * Return all 3DViewable to download with the plmxml from a Cat Element list<br>
     * Used when viewing plmxml from a Folder
     * @param context
     * @param args
     *            contains the Cat Element list ids
     * @return
     * @throws Exception
     */
    public Map<String, StringList> getPLMXML3DViewableInfo(Context context, String[] args) throws Exception {
        Map<String, StringList> m3DInfo = new HashMap<String, StringList>();
        try {
            HashMap<?, ?> paramMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            StringList slElementIds = (StringList) paramMap.get("ids");

            StringList slViewableSelect = new StringList();
            slViewableSelect.addElement(DomainConstants.SELECT_TYPE);
            slViewableSelect.addElement(DomainConstants.SELECT_NAME);
            slViewableSelect.addElement(DomainConstants.SELECT_ID);
            slViewableSelect.addElement("format[" + FORMAT_JT + "].file.name");

            StringList slAssemblySelect = new StringList(1);
            slAssemblySelect.addElement(DomainConstants.SELECT_ID);
            slAssemblySelect.addElement(DomainConstants.SELECT_TYPE);
            slAssemblySelect.addElement(SELECT_MAJOR_ID);
            slAssemblySelect.addElement(SELECT_MAJOR_TYPE);

            StringList slRelatedViewableObjects = new StringList();
            StringList sl3DViewableIds = new StringList();
            StringList sl3DViewableFiles = new StringList();
            StringList withoutfiles = new StringList();
            ArrayList<String> alAlreadyParsedObjects = new ArrayList<String>();
            for (Iterator<?> iterator = slElementIds.iterator(); iterator.hasNext();) {
                String sElementId = (String) iterator.next();

                if (!alAlreadyParsedObjects.contains(sElementId)) {
                    alAlreadyParsedObjects.add(sElementId);
                    DomainObject dobElement = DomainObject.newInstance(context, sElementId);
                    try {
                        dobElement.open(context);
                        String sType = dobElement.getInfo(context, DomainObject.SELECT_TYPE);

                        retrieve3DViewable(context, alAlreadyParsedObjects, sl3DViewableIds, sl3DViewableFiles, sType, dobElement, slViewableSelect, slAssemblySelect, slRelatedViewableObjects,
                                withoutfiles);
                    } finally {
                        dobElement.close(context);
                    }
                }
            }

            m3DInfo.put("slIds", sl3DViewableIds);
            m3DInfo.put("slFiles", sl3DViewableFiles);
            m3DInfo.put("missingfileObjects", withoutfiles);

        } catch (Exception e) {
            System.out.println("Error in get3DViewableInfo()\n" + e);
            throw e;
        }
        return m3DInfo;
    }

    public static final String TYPE_PSS_CATPART = PropertyUtil.getSchemaProperty("type_PSS_CATPart");

    public static final String TYPE_PSS_UG_MODEL = PropertyUtil.getSchemaProperty("type_PSS_UGModel");

    public static final String TYPE_CATIA_CGR = PropertyUtil.getSchemaProperty("type_CATIACGR");

    public static final String TYPE_CATIA_V4_MODEL = PropertyUtil.getSchemaProperty("type_CATIAV4Model");

    public static final String ATTRIBUTE_CAD_FILE_NAME = PropertyUtil.getSchemaProperty("attribute_CV5CADFileName");

    private static void retrieve3DViewable(Context context, ArrayList<String> alAlreadyParsedObjects, StringList sl3DViewableIds, StringList sl3DViewableFiles, String sType, DomainObject doCurrent,
            StringList slViewableSelect, StringList slAssemblySelect, StringList slRelatedViewableObjects, StringList withoutfiles) throws Exception {

        if (sType != null && (sType.equals(TYPE_PSS_CATPART) || sType.equals(TYPE_CATIA_V4_MODEL) || sType.equals(TYPE_PSS_UG_MODEL) || sType.equals(TYPE_CATIA_CGR))) {
            // TIGTK-3663 : START
            MapList mlInfos = doCurrent.getRelatedObjects(context, REL_PATTERN_ACTIVE_VIEWABLE, TYPE_CADMODEL + "," + TYPE_VIEWABLE, slViewableSelect, null, false, true, (short) 2, "", "", 0, null,
                    null, null);
            // TIGTK-3663 : END
            for (Iterator<?> iterator2 = mlInfos.iterator(); iterator2.hasNext();) {
                Map<?, ?> m3DObject = (Map<?, ?>) iterator2.next();
                String sViewableId = (String) m3DObject.get(DomainConstants.SELECT_ID);
                String sViewableType = (String) m3DObject.get(DomainConstants.SELECT_TYPE);
                String sViewableName = (String) m3DObject.get(DomainConstants.SELECT_NAME);
                String sFileName = (String) m3DObject.get("format[" + FORMAT_JT + "].file.name");

                if (!slRelatedViewableObjects.contains(sViewableType + "_" + sViewableName)) { // keep only one revision of Viewable
                    slRelatedViewableObjects.addElement(sViewableType + "_" + sViewableName);
                    if (sFileName != null && !sFileName.equals("")) {
                        sl3DViewableIds.addElement(sViewableId);
                        sl3DViewableFiles.addElement(sFileName);
                    } else {
                        withoutfiles.addElement(sViewableName);
                    }
                }

            }
        } else {
            MapList mlInfos = doCurrent.getRelatedObjects(context, REL_PATTERN_ACTIVE_CADSUB, "*", slAssemblySelect, null, false, true, (short) 2, "", "", 0);
            doCurrent.close(context);

            for (Iterator<?> iterator2 = mlInfos.iterator(); iterator2.hasNext();) {
                Map<?, ?> mCADObject = (Map<?, ?>) iterator2.next();
                String sId = (String) mCADObject.get(DomainObject.SELECT_ID);
                String sRelatedType = (String) mCADObject.get(DomainObject.SELECT_TYPE);
                String sMajorId = (String) mCADObject.get(SELECT_MAJOR_ID);
                if (sMajorId != null && sMajorId.length() > 0) {
                    sId = sMajorId;
                    sRelatedType = (String) mCADObject.get(SELECT_MAJOR_TYPE);
                }
                if (!alAlreadyParsedObjects.contains(sId)) {
                    // TIGTK-3663 : START
                    mlInfos = doCurrent.getRelatedObjects(context, REL_PATTERN_ACTIVE_VIEWABLE, TYPE_CADMODEL + "," + TYPE_VIEWABLE, slViewableSelect, null, false, true, (short) 2, "", "", 0, null,
                            null, null);
                    // TIGTK-3663 : END
                    for (iterator2 = mlInfos.iterator(); iterator2.hasNext();) {
                        Map<?, ?> m3DObject = (Map<?, ?>) iterator2.next();
                        String sViewableId = (String) m3DObject.get(DomainConstants.SELECT_ID);
                        String sViewableType = (String) m3DObject.get(DomainConstants.SELECT_TYPE);
                        String sViewableName = (String) m3DObject.get(DomainConstants.SELECT_NAME);
                        String sFileName = (String) m3DObject.get("format[" + FORMAT_JT + "].file.name");

                        if (!slRelatedViewableObjects.contains(sViewableType + "_" + sViewableName)) { // keep only one revision of Viewable
                            slRelatedViewableObjects.addElement(sViewableType + "_" + sViewableName);

                            if (sFileName != null && !sFileName.equals("")) {
                                sl3DViewableIds.addElement(sViewableId);
                                sl3DViewableFiles.addElement(sFileName);
                            } else {
                                withoutfiles.addElement(sViewableName);
                            }
                        }

                    }
                    alAlreadyParsedObjects.add(sId);

                    DomainObject doCADObject = new DomainObject(sId);
                    try {
                        doCADObject.open(context);
                        retrieve3DViewable(context, alAlreadyParsedObjects, sl3DViewableIds, sl3DViewableFiles, sRelatedType, doCADObject, slViewableSelect, slAssemblySelect, slRelatedViewableObjects,
                                withoutfiles);
                    } finally {
                        doCADObject.close(context);
                    }
                }
            }
        }
    }

    /**
     * Construct the PLMXML file
     * @param context
     *            the eMatrix Context object
     * @param args
     *            contains Object id
     * @return
     */
    @SuppressWarnings("unchecked")
    public Vector getPLMXML(Context context, String[] args) {

        Vector vPLMXML = new Vector();
        boolean isFromFolder = false;
        try {
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            String sCadFileNameAttr = "attribute[" + ATTRIBUTE_CAD_FILE_NAME + "]";
            String sObjectId = (String) paramMap.get("objectId");
            String sCadDefName = (String) paramMap.get("CADDefinitionName");
            if (sCadDefName == null) {
                sCadDefName = "";
            }
            String sCadDefRev = (String) paramMap.get("CADDefinitionRevision");
            if (sCadDefRev == null) {
                sCadDefRev = "";
            }
            String sSelectedIds = (String) paramMap.get("listobjid");
            String sSelectedIdslevel = (String) paramMap.get("listobjidlevel");
            StringList slSelectedIds = FrameworkUtil.split(sSelectedIds, "|");
            StringList slSelectedIdslevel = FrameworkUtil.split(sSelectedIdslevel, "|");
            Map<String, String> Maplevles = new HashMap<String, String>();
            // Remove empty strings from slSelectedIds
            for (int i = 0; i < slSelectedIds.size(); i++) {
                String data = (String) slSelectedIds.get(i);
                if (data != null && "".equals(data)) {
                    slSelectedIds.remove(i);
                    i--;
                }
            }
            // Remove empty strings from slSelectedIdslevel
            for (int i = 0; i < slSelectedIdslevel.size(); i++) {
                String data = (String) slSelectedIdslevel.get(i);
                if (data != null && "".equals(data)) {
                    slSelectedIdslevel.remove(i);
                    i--;
                }
            }
            for (Iterator levelit = slSelectedIdslevel.iterator(); levelit.hasNext();) {
                String level = (String) levelit.next();
                String[] temp = level.split("_");
                // Validation for empty/null string or Level information
                if (temp != null && temp[0] != null && !"".equals(temp[0]) && temp.length > 1) {
                    Maplevles.put(temp[0], temp[1]);
                }

            }

            MapList mlBOM = new MapList();

            DomainObject doCADObject = DomainObject.newInstance(context, sObjectId);

            String sVersionOfId = doCADObject.getInfo(context, "from[" + RELATIONSHIP_VERSIONOF + "].to.id");

            if (sVersionOfId != null && !"".equals(sVersionOfId)) {
                sObjectId = sVersionOfId;
            }
            HashMap<String, String> hmParam = new HashMap<String, String>();
            hmParam.put("objectId", sObjectId);

            StringList strListID = new StringList();
            strListID.add(DomainConstants.SELECT_ID);
            strListID.add(DomainConstants.SELECT_NAME);
            strListID.add(DomainConstants.SELECT_REVISION);
            strListID.add(DomainConstants.SELECT_DESCRIPTION);
            strListID.add(sCadFileNameAttr);
            strListID.addElement("to[" + RELATIONSHIP_CAD_SUBCOMPONENT + "].id");
            // TIGTK-3663 : START
            strListID.addElement("from[" + RELATIONSHIP_ACTIVE_VERSION + "].id");
            // TIGTK-3663 : END

            // CAD Definition informations
            for (int i = 0; i < slSelectedIds.size(); i++) {
                DomainObject cmHeadBom = new DomainObject(slSelectedIds.get(i).toString());
                System.out.println("getPLMXML() - slSelectedIds : " + slSelectedIds.get(i).toString());
                HashMap<String, String> hmHeadBom = new HashMap<String, String>();
                Map MapActiveVersionID = cmHeadBom.getInfo(context, strListID);

                String sRelId = "";
                if (slSelectedIds.get(i).toString().equals(sObjectId)) {
                    // TIGTK-3663 : START
                    StringList slRelCADSubComponentIds = toStringList(MapActiveVersionID.get("from[" + RELATIONSHIP_ACTIVE_VERSION + "].id"));
                    if (slRelCADSubComponentIds.size() > 0) {
                        sRelId = (String) slRelCADSubComponentIds.get(0);
                    }
                    hmHeadBom.put(DomainConstants.SELECT_RELATIONSHIP_ID, sRelId);
                    hmHeadBom.put("rootInstance", "true");
                    hmHeadBom.put(DomainConstants.SELECT_LEVEL, "0");
                    // TIGTK-3663 : END
                } else {
                    StringList slRelCADSubComponentIds = toStringList(MapActiveVersionID.get("to[" + RELATIONSHIP_CAD_SUBCOMPONENT + "].id"));
                    if (i > 0 && slRelCADSubComponentIds.size() > 0) {
                        sRelId = (String) slRelCADSubComponentIds.get(0);
                    }
                    hmHeadBom.put(DomainConstants.SELECT_RELATIONSHIP_ID, sRelId);
                    // TIGTK-3663 : START
                    hmHeadBom.put(DomainConstants.SELECT_LEVEL, Maplevles.get(slSelectedIds.get(i).toString()));
                    // TIGTK-3663 : END
                }

                hmHeadBom.put(DomainConstants.SELECT_ID, slSelectedIds.get(i).toString());

                hmHeadBom.put(DomainConstants.SELECT_NAME, (String) MapActiveVersionID.get(DomainConstants.SELECT_NAME));

                hmHeadBom.put(DomainConstants.SELECT_REVISION, (String) MapActiveVersionID.get(DomainConstants.SELECT_REVISION));

                hmHeadBom.put(DomainConstants.SELECT_DESCRIPTION, (String) MapActiveVersionID.get(DomainConstants.SELECT_DESCRIPTION));

                hmHeadBom.put(sCadFileNameAttr, (String) MapActiveVersionID.get(sCadFileNameAttr));

                mlBOM.add(i, hmHeadBom);
            }

            // construct XML from BOM list
            vPLMXML = constructPLMXmlFromBOM(context, mlBOM, isFromFolder);

        } catch (Exception ex) {
            ex.printStackTrace();

            System.out.println("Error in getPLMXML" + ex);
        }
        return vPLMXML;
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
            throw e;
        }
        return slObj;
    }

    /**
     * Return the PLM xml file from the BOM list
     * @param context
     * @param mlBOM
     * @param sBOMType
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private Vector constructPLMXmlFromBOM(Context context, MapList mlBOM, boolean isFromFolder) throws Exception {
        Vector<String> vPLMXML = new Vector<String>();
        StringList slBusToDisplay = new StringList();
        String sAttrSpatialLocation = PropertyUtil.getSchemaProperty("attribute_SpatialLocation");

        String sSpacialPositionFactor = "1";
        try {
            sSpacialPositionFactor = (String) FrameworkProperties.getProperty(context, "FPDM.JTAssemblyManagement.SpacialPosition.Factor");
        } catch (Exception expFactor) {
            sSpacialPositionFactor = "1";

        }

        BigDecimal bdSpacialPositionFactor = new BigDecimal(sSpacialPositionFactor);

        vPLMXML.add("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");

        StringList slInstanceAlreadyinXML = new StringList();
        StringList slPartAlreadyinXML = new StringList();

        // mlBOM.sortStructure(DomainConstants.SELECT_NAME, "ascending", "String");

        vPLMXML.add("<PLMXML xmlns=\"http://www.plmxml.org/Schemas/PLMXMLSchema\" schemaVersion=\"4\" date=\"2006-05-29\" time=\"13:19:13\" author=\" 5.1\">");
        vPLMXML.add("<ProductDef id=\"productdef\">");
        vPLMXML.add("   <InstanceGraph id=\"instancegraph\" rootRefs=\"rootInstance\">");

        StringList slMultipleJTSons = new StringList();
        for (int i = mlBOM.size() - 1; i >= 0; i--) {
            Map mapBOMElement = (Map) mlBOM.get(i);
            String sRelId = (String) mapBOMElement.get(DomainConstants.SELECT_RELATIONSHIP_ID);
            String sBusId = (String) mapBOMElement.get(DomainConstants.SELECT_ID);
            String sBusName = (String) mapBOMElement.get(DomainConstants.SELECT_NAME);
            // TIGTK-3663 : START
            // String sBusRevision = (String) mapBOMElement.get(DomainConstants.SELECT_REVISION);
            // TIGTK-3663 : END
            String sBusDescription = (String) mapBOMElement.get(DomainConstants.SELECT_DESCRIPTION);
            // TIGTK-3663 : START
            String rootInstance = (String) mapBOMElement.get("rootInstance");
            // TIGTK-3663 : END

            // TIGTK-3663 : START
            String sInstanceName = sBusName;
            // Revision is not required as per latest XML file from Faurecia
            // sInstanceName = sBusName + "-" + sBusRevision;
            // if (sBusRevision == null || "".equals(sBusRevision)) {
            // sInstanceName = sBusName;
            // }
            // TIGTK-3663 : END

            String sTransformation = "1 0 0 0 0 1 0 0 0 0 1 0 0 0 0 1";
            // TIGTK-3663 : START
            if (sRelId != null && !"true".equals(rootInstance)) {
                // TIGTK-3663 : END
                String sSpacialLocationValue = MqlUtil.mqlCommand(context, "print connection " + sRelId + " select attribute[" + sAttrSpatialLocation + "] dump ");

                if (!"Unassigned".equals(sSpacialLocationValue) && !"".equals(sSpacialLocationValue)) {
                    StringList slSpacialLocation = FrameworkUtil.split(sSpacialLocationValue, ",");
                    StringBuffer sbTemp = new StringBuffer("");

                    // The rotation information must not be multiplied.
                    // Only Translations informations
                    // 4th, 8th, 12th 13th, 14th, 15th element needed to
                    // be multiplied
                    int iCptCoord = 1;
                    for (Enumeration e = slSpacialLocation.elements(); e.hasMoreElements();) {
                        String sValue = (String) e.nextElement();
                        BigDecimal bdValue = new BigDecimal(sValue);
                        if (iCptCoord == 4 || iCptCoord == 8 || iCptCoord == 12 || iCptCoord == 13 || iCptCoord == 14 || iCptCoord == 15) {
                            bdValue = bdValue.multiply(bdSpacialPositionFactor);
                        }
                        sbTemp.append(bdValue.toString());
                        sbTemp.append(" ");
                        iCptCoord++;
                    }
                    sTransformation = sbTemp.toString();
                }
            }

            if (!slInstanceAlreadyinXML.contains(sRelId)) {
                slInstanceAlreadyinXML.addElement(sRelId);
                vPLMXML.add("       <Instance id=\"" + sRelId + "\" name=\"" + sInstanceName + "\" partRef=\"#" + sBusId + "\">");
                vPLMXML.add("           <Transform>" + sTransformation + "</Transform>");
                vPLMXML.add("       </Instance>");

            }
            if (!slBusToDisplay.contains(sBusId)) {
                slBusToDisplay.addElement(sBusId);
                StringList slSons = getSons(mlBOM, i);

                if (!slPartAlreadyinXML.contains(sBusId)) {
                    slPartAlreadyinXML.addElement(sBusId);
                    if (slSons.size() == 0) {
                        String s3DViewableFileName = "";

                        String[] sConstruct = new String[] { sBusId };
                        s3DViewableFileName = get3DViewableFileName(context, sConstruct);

                        int iFileNumber = 0;
                        if (s3DViewableFileName != null && !"".equals(s3DViewableFileName)) {
                            java.util.StringTokenizer tokenFiles = new java.util.StringTokenizer(s3DViewableFileName, "|");
                            while (tokenFiles.hasMoreTokens()) {
                                iFileNumber++;
                                String sSelectedFile = tokenFiles.nextToken();
                                if (iFileNumber == 1) {
                                    vPLMXML.add("                <Part id=\"" + sBusId + "\" name=\"" + sInstanceName + "\" type=\"solid\">");
                                    vPLMXML.add("                  <Representation format=\"JT\" location=\"./" + sSelectedFile + "\"></Representation>");
                                    vPLMXML.add("            </Part>");

                                } else {
                                    // add new ref to the others JT files (if more than one JT)
                                    slMultipleJTSons.addElement(sBusId + "_" + iFileNumber);
                                    vPLMXML.add("              <Instance id=\"" + sRelId + "_" + iFileNumber + "\" name=\"" + sInstanceName + "\" partRef=\"#" + sBusId + "_" + iFileNumber + "\">");
                                    vPLMXML.add("                    <Transform>" + sTransformation + "</Transform>");
                                    vPLMXML.add("                 </Instance>");
                                    vPLMXML.add("                 <Part id=\"" + sBusId + "_" + iFileNumber + "\" name=\"" + sInstanceName + "\" type=\"solid\">");
                                    vPLMXML.add("                  <Representation format=\"JT\" location=\"./" + sSelectedFile + "\"></Representation>");
                                    vPLMXML.add("                 </Part>");
                                }
                            }

                        }

                        if (iFileNumber == 0) {
                            vPLMXML.add("                <Part id=\"" + sBusId + "\" name=\"" + sInstanceName + "\" type=\"solid\">");
                            vPLMXML.add("            </Part>");
                        }

                    } else {
                        // add ref to the others JT files (if more than one JT)
                        if (slMultipleJTSons.size() > 0) {
                            slSons.addAll(slMultipleJTSons);
                            slMultipleJTSons = new StringList();
                        }
                        String sSons = FrameworkUtil.join(slSons, " ");
                        vPLMXML.add("       <Part id=\"" + sBusId + "\" name=\"" + sInstanceName + "\" instanceRefs=\"" + sSons + "\" type=\"assembly\">");
                        vPLMXML.add("       </Part>");
                    }
                }
            }

            // TIGTK-3663 : START
            if ("true".equalsIgnoreCase(rootInstance)) {
                if (!"".equals(sBusDescription)) {
                    sInstanceName += "-" + sBusDescription;
                }

                vPLMXML.add("       <Instance id=\"rootInstance\" name=\"" + sInstanceName + "\" partRef=\"#" + sBusId + "\">");
                vPLMXML.add("           <Transform>" + sTransformation + "</Transform>");
                vPLMXML.add("       </Instance>");
            }
            // TIGTK-3663 : END
        }

        vPLMXML.add("   </InstanceGraph>");
        vPLMXML.add("</ProductDef>");
        vPLMXML.add("</PLMXML>");

        return vPLMXML;
    }

    private StringList getSons(MapList mlExpand, int iPositionOfParent) {
        StringList slResult = new StringList();

        Map mapParent = (Map) mlExpand.get(iPositionOfParent);

        int iParentLevel = Integer.parseInt((String) mapParent.get(DomainConstants.SELECT_LEVEL));
        int iChildLevel = iParentLevel + 1;

        for (int i = iPositionOfParent + 1; i < mlExpand.size(); i++) {
            Map mapCurrent = (Map) mlExpand.get(i);
            int iCurrentLevel = Integer.parseInt((String) mapCurrent.get(DomainConstants.SELECT_LEVEL));
            // If the level of the current is one more than the parent, it is a direct son of the parent

            if (iChildLevel == iCurrentLevel) {
                slResult.addElement((String) mapCurrent.get(DomainConstants.SELECT_RELATIONSHIP_ID));
            } else if (iParentLevel == iCurrentLevel) {
                break;
            }
        }
        return (slResult);
    }

    // public static final String TYPE_3DVIEWABLE = PropertyUtil.getSchemaProperty("type_3DViewable");

    /**
     * Get and initialize the list of 3DViewable id and file names<br>
     * The result is set in static variables (_3DViewableId, _3DViewableFileName)
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no args
     * @return
     * @throws Exception
     */
    private String get3DViewableFileName(Context context, String[] args) throws Exception {
        try {
            String sCADObjectId = args[0];

            StringList slBus = new StringList();
            slBus.addElement(DomainConstants.SELECT_TYPE);
            slBus.addElement(DomainConstants.SELECT_NAME);
            slBus.addElement(DomainConstants.SELECT_ID);
            slBus.addElement("format[" + FORMAT_JT + "].file.name");

            DomainObject dobCADObject = DomainObject.newInstance(context, sCADObjectId);
            // TIGTK-3663 : START
            Map mViewableObject = getViewableInfos(context, dobCADObject, TYPE_VIEWABLE, slBus);
            // TIGTK-3663 : END
            String _3DViewableId = "";
            String _3DViewableFileName = "";
            if (mViewableObject != null) {
                _3DViewableId = (String) mViewableObject.get(DomainConstants.SELECT_ID);
                _3DViewableFileName = (String) mViewableObject.get("format[" + FORMAT_JT + "].file.name");
            }

            return _3DViewableFileName;

        } catch (Exception e) {
            System.out.println("Error in update3DViewableInfo() :" + e);
            throw e;
        }
    }

    /**
     * get attributes values from the related Viewable object
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param doCADObject
     *            CAD object
     * @param sTypePattern
     * @param slSelect
     *            Select list
     * @param sNemoAction
     *            the code of the action : "QC" for Quality Check
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public static Map getViewableInfos(Context context, DomainObject doCADObject, String sTypePattern, StringList slSelect) throws Exception {
        Map<String, Object> mReturn = null;

        try {
            String sRelPattern = RELATIONSHIP_VIEWABLE;
            sRelPattern += "," + RELATIONSHIP_VERSIONOF;
            sRelPattern += "," + RELATIONSHIP_ACTIVE_VERSION;
            SelectList sl = new SelectList();
            sl.addAll(slSelect);
            sl.addElement(DomainConstants.SELECT_TYPE);
            // TIGTK-3663 : START
            MapList mlViewableObject = doCADObject.getRelatedObjects(context, sRelPattern, sTypePattern, sl, null, false, true, (short) 2, "", "", 0, null, null, null);

            if (mlViewableObject.size() > 0) {
                mReturn = new HashMap<String, Object>();

                for (int a = 0; a < mlViewableObject.size(); a++) {
                    String strFileName = (String) ((Map) mlViewableObject.get(a)).get("format[" + FORMAT_JT + "].file.name");
                    if (strFileName.contains(".jt")) {
                        mReturn.put("format[" + FORMAT_JT + "].file.name", strFileName);
                    }
                }

                /*
                 * Set<Map.Entry<String, Object>> mySet = ((Map<String, Object>) mlViewableObject.get(0)).entrySet(); String sKey = ""; String sValue = ""; for (Map.Entry<String, Object> me : mySet) {
                 * sKey = me.getKey(); sValue = (String)me.getValue(); if (slSelect.contains(sKey) && (!"".equals(sValue) && sValue.contains(".jt"))) { mReturn.put(sKey, me.getValue()); } }
                 */
                // TIGTK-3663 : END
            }
        } catch (FrameworkException e) {
            System.out.println("Error in getViewableInfos()\n" + e);
            throw e;
        }
        return mReturn;
    }

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public Map<String, String> getRelatedIDs(Context context, String[] args) throws Exception {

        Map<String, String> m3DInfo = new HashMap<String, String>();
        String levels = "";
        // unpack args to get map
        HashMap programMap = (HashMap) JPO.unpackArgs(args);

        // get object id
        String strObjectId = (String) programMap.get("objectId");

        try {

            String strRelCADSubComponent = MCADMxUtil.getActualNameForAEFData(context, "relationship_CADSubComponent");
            StringList strListID = new StringList();
            strListID.add(DomainConstants.SELECT_ID);

            DomainObject objDom = new DomainObject(strObjectId);
            MapList mapCADComponentsrel = objDom.getRelatedObjects(context, strRelCADSubComponent, DomainConstants.QUERY_WILDCARD, strListID, DomainConstants.EMPTY_STRINGLIST, false, true, (short) 0,
                    DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, (short) 0);

            String strIDS = strObjectId + "|";
            levels = strObjectId + "_-1|";
            for (int i = 0; i < mapCADComponentsrel.size(); i++) {

                Map mapObj = (Map) mapCADComponentsrel.get(i);
                strIDS += mapObj.get(DomainConstants.SELECT_ID);
                levels += mapObj.get(DomainConstants.SELECT_ID) + "_" + mapObj.get("level");
                if (i != (mapCADComponentsrel.size() - 1)) {
                    strIDS += "|";
                    levels += "|";
                }

            }

            m3DInfo.put("strIDS", strIDS);
            m3DInfo.put("levels", levels);
            return m3DInfo;
        } catch (MatrixException e) {
            e.printStackTrace();

        }
        return m3DInfo;
    }

}