package fpdm.cad;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.apache.axis.utils.XMLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.SelectList;
import matrix.util.StringList;

public class JTAssembly_mxJPO extends fpdm.cad.Constants_mxJPO {

    private static Logger logger = LoggerFactory.getLogger("fpdm.cad.JTAssembly");

    private String TYPE_PSS_CATPART = null;

    private String TYPE_PSS_UG_MODEL = null;

    private String TYPE_CATIA_CGR = null;

    private String TYPE_CATIA_V4_MODEL = null;

    /**
     * Constructor
     * @param context
     *            the eMatrix Context object
     * @param args
     *            holds no arguments
     * @throws Exception
     */
    public JTAssembly_mxJPO(Context context, String[] args) throws Exception {

        TYPE_PSS_CATPART = getSchemaProperty(context, SYMBOLIC_type_PSS_CATPart);

        TYPE_PSS_UG_MODEL = getSchemaProperty(context, SYMBOLIC_type_PSS_UGModel);

        TYPE_CATIA_CGR = getSchemaProperty(context, SYMBOLIC_type_CATIACGR);

        TYPE_CATIA_V4_MODEL = getSchemaProperty(context, SYMBOLIC_type_CATIAV4Model);

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
            String sObjectID = (String) paramMap.get("objectId");
            logger.debug("getPLMXML3DViewableInfo() - sObjectID = <" + sObjectID + ">");

            StringList slViewableSelect = new StringList();
            slViewableSelect.addElement(DomainConstants.SELECT_TYPE);
            slViewableSelect.addElement(DomainConstants.SELECT_NAME);
            slViewableSelect.addElement(DomainConstants.SELECT_ID);
            slViewableSelect.addElement("format[" + getSchemaProperty(context, SYMBOLIC_format_JT) + "].file.name");

            StringList slAssemblySelect = new StringList(1);
            slAssemblySelect.addElement(DomainConstants.SELECT_ID);
            slAssemblySelect.addElement(DomainConstants.SELECT_TYPE);

            StringList slRelatedViewableObjects = new StringList();
            StringList sl3DViewableIds = new StringList();
            StringList sl3DViewableFiles = new StringList();
            StringList withoutfiles = new StringList();
            ArrayList<String> alAlreadyParsedObjects = new ArrayList<String>();

            if (!alAlreadyParsedObjects.contains(sObjectID)) {
                alAlreadyParsedObjects.add(sObjectID);
                DomainObject dobElement = DomainObject.newInstance(context, sObjectID);
                try {
                    dobElement.open(context);
                    String sType = dobElement.getInfo(context, DomainObject.SELECT_TYPE);

                    retrieve3DViewable(context, alAlreadyParsedObjects, sl3DViewableIds, sl3DViewableFiles, sType, dobElement, slViewableSelect, slAssemblySelect, slRelatedViewableObjects,
                            withoutfiles);
                } finally {
                    dobElement.close(context);
                }
            }

            m3DInfo.put("slIds", sl3DViewableIds);
            m3DInfo.put("slFiles", sl3DViewableFiles);
            m3DInfo.put("missingfileObjects", withoutfiles);
            logger.debug("getPLMXML3DViewableInfo() - m3DInfo = <" + m3DInfo + ">");

        } catch (Exception e) {
            System.out.println("Error in get3DViewableInfo()\n" + e);
            throw e;
        }
        return m3DInfo;
    }

    private void retrieve3DViewable(Context context, ArrayList<String> alAlreadyParsedObjects, StringList sl3DViewableIds, StringList sl3DViewableFiles, String sType, DomainObject doCurrent,
            StringList slViewableSelect, StringList slAssemblySelect, StringList slRelatedViewableObjects, StringList withoutfiles) throws Exception {

        String FORMAT_JT = getSchemaProperty(context, SYMBOLIC_format_JT);
        String TYPE_VIEWABLE = getSchemaProperty(context, SYMBOLIC_type_VIEWABLE);
        String RELATIONSHIP_VIEWABLE = getSchemaProperty(context, SYMBOLIC_relationship_Viewable);
        String RELATIONSHIP_CAD_SUBCOMPONENT = getSchemaProperty(context, SYMBOLIC_relationship_CADSubComponent);

        logger.debug("retrieve3DViewable() - sType = <" + sType + ">");
        if (sType != null && (sType.equals(TYPE_PSS_CATPART) || sType.equals(TYPE_CATIA_V4_MODEL) || sType.equals(TYPE_PSS_UG_MODEL) || sType.equals(TYPE_CATIA_CGR))) {
            // TIGTK-3663 : START
            MapList mlInfos = doCurrent.getRelatedObjects(context, RELATIONSHIP_VIEWABLE, TYPE_VIEWABLE, slViewableSelect, null, false, true, (short) 1, "", "", 0, null, null, null);
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
            MapList mlRelatedCADInfos = doCurrent.getRelatedObjects(context, RELATIONSHIP_CAD_SUBCOMPONENT, "*", slAssemblySelect, null, false, true, (short) 1, "", "", 0);
            logger.debug("retrieve3DViewable() - mlRelatedCADInfos = <" + mlRelatedCADInfos + ">");

            doCurrent.close(context);

            for (Iterator<?> iterator2 = mlRelatedCADInfos.iterator(); iterator2.hasNext();) {
                Map<?, ?> mCADObject = (Map<?, ?>) iterator2.next();
                String sId = (String) mCADObject.get(DomainObject.SELECT_ID);
                String sRelatedType = (String) mCADObject.get(DomainObject.SELECT_TYPE);
                logger.debug("retrieve3DViewable() - sId = <" + sId + "> sRelatedType = <" + sRelatedType + ">");

                if (!alAlreadyParsedObjects.contains(sId)) {
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
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Vector<String> getPLMXML(Context context, String[] args) {

        Vector<String> vPLMXML = new Vector<String>();
        try {
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            String sObjectId = (String) paramMap.get("objectId");
            String sCadDefName = (String) paramMap.get("CADDefinitionName");
            if (sCadDefName == null) {
                sCadDefName = "";
            }
            String sCadDefRev = (String) paramMap.get("CADDefinitionRevision");
            if (sCadDefRev == null) {
                sCadDefRev = "";
            }
            DomainObject doCADObject = DomainObject.newInstance(context, sObjectId);
            if (logger.isDebugEnabled()) {
                logger.debug("getPLMXML() - doCADObject = <" + doCADObject + ">");
            }

            MapList mlBOM = getCADBomData(context, sObjectId, Integer.MAX_VALUE);

            // CAD Definition informations
            DomainObject cmHeadBom = new DomainObject(sObjectId);
            HashMap<String, String> hmHeadBom = new HashMap<String, String>();
            hmHeadBom.put(DomainConstants.SELECT_RELATIONSHIP_ID, "rootInstance");
            hmHeadBom.put(DomainConstants.SELECT_LEVEL, "-1");
            hmHeadBom.put(DomainConstants.SELECT_ID, sObjectId);
            hmHeadBom.put(DomainConstants.SELECT_NAME, cmHeadBom.getInfo(context, "name"));
            hmHeadBom.put(DomainConstants.SELECT_REVISION, cmHeadBom.getInfo(context, "revision"));
            hmHeadBom.put(DomainConstants.SELECT_DESCRIPTION, cmHeadBom.getInfo(context, "description"));
            mlBOM.add(0, hmHeadBom);

            // construct XML from BOM list
            vPLMXML = constructPLMXmlFromBOM(context, mlBOM);
            logger.debug("getPLMXML() - vPLMXML = <" + vPLMXML + ">");

        } catch (Exception e) {
            logger.error("Error in getPLMXML()\n", e);
        }

        return vPLMXML;
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
    private Vector<String> constructPLMXmlFromBOM(Context context, MapList mlBOM) throws Exception {
        Vector<String> vPLMXML = new Vector<String>();
        StringList slBusToDisplay = new StringList();
        String sAttrSpatialLocation = getSchemaProperty(context, SYMBOLIC_attribute_SpatialLocation);

        String sSpacialPositionFactor = "1";
        try {
            sSpacialPositionFactor = EnoviaResourceBundle.getProperty(context, "FPDM.JTAssemblyManagement.SpacialPosition.Factor");
            logger.debug("constructPLMXmlFromBOM() - sSpacialPositionFactor = <" + sSpacialPositionFactor + ">");
        } catch (Exception expFactor) {
            logger.error("constructPLMXmlFromBOM() - sSpacialPositionFactor not define in PropertieFile", expFactor);
        }

        BigDecimal bdSpacialPositionFactor = new BigDecimal(sSpacialPositionFactor);

        vPLMXML.add("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");

        StringList slInstanceAlreadyinXML = new StringList();
        StringList slPartAlreadyinXML = new StringList();

        vPLMXML.add("<PLMXML xmlns=\"http://www.plmxml.org/Schemas/PLMXMLSchema\" schemaVersion=\"4\" date=\"2006-05-29\" time=\"13:19:13\" author=\" 5.1\">");
        vPLMXML.add("<ProductDef id=\"productdef\">");
        vPLMXML.add("   <InstanceGraph id=\"instancegraph\" rootRefs=\"rootInstance\">");

        StringList slMultipleJTSons = new StringList();
        for (int i = mlBOM.size() - 1; i >= 0; i--) {
            Map<?, ?> mapBOMElement = (Map<?, ?>) mlBOM.get(i);
            String sRelId = (String) mapBOMElement.get(DomainConstants.SELECT_RELATIONSHIP_ID);
            String sBusId = (String) mapBOMElement.get(DomainConstants.SELECT_ID);
            String sBusName = (String) mapBOMElement.get(DomainConstants.SELECT_NAME);
            // String sBusRevision = (String) mapBOMElement.get(DomainConstants.SELECT_REVISION);
            // String sBusDescription = (String) mapBOMElement.get(DomainConstants.SELECT_DESCRIPTION);
            String rootInstance = (String) mapBOMElement.get("rootInstance");
            logger.debug("constructPLMXmlFromBOM() - rootInstance = <" + rootInstance + ">");

            // TIGTK-3663 : START
            String sInstanceName = sBusName;
            // Revision is not required as per latest XML file from Faurecia
            // sInstanceName = sBusName + "-" + sBusRevision;
            // if (sBusRevision == null || "".equals(sBusRevision)) {
            // sInstanceName = sBusName;
            // }
            // TIGTK-3663 : END

            String sTransformation = "1 0 0 0 0 1 0 0 0 0 1 0 0 0 0 1";
            if (sRelId != null && !"rootInstance".equals(sRelId)) {
                String sSpacialLocationValue = MqlUtil.mqlCommand(context, "print connection $1 select $2 dump", sRelId, "attribute[" + sAttrSpatialLocation + "].value");
                logger.debug("constructPLMXmlFromBOM() - sSpacialLocationValue = <" + sSpacialLocationValue + ">");
                if (!"".equals(sSpacialLocationValue) && !"Unassigned".equals(sSpacialLocationValue)) {
                    StringList slSpacialLocation = FrameworkUtil.split(sSpacialLocationValue, ",");
                    StringBuilder sbTemp = new StringBuilder("");

                    // The rotation information must not be multiplied. Only Translations informations 4th, 8th, 12th 13th, 14th, 15th element needed to be multiplied
                    int iCptCoord = 1;
                    for (Enumeration<?> e = slSpacialLocation.elements(); e.hasMoreElements();) {
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

            // escape any XML non-compliant characters
            sInstanceName = XMLUtils.xmlEncodeString(sInstanceName);
            if (logger.isTraceEnabled()) {
                logger.trace("constructPLMXmlFromBOM() - sInstanceName = <" + sInstanceName + "> after escaping");
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
                if (logger.isDebugEnabled()) {
                    logger.debug("constructPLMXmlFromBOM() - slSons : <" + slSons + "> i : <" + i + ">");
                    logger.debug("constructPLMXmlFromBOM() - mapBOMElement : " + mapBOMElement);
                }

                if (!slPartAlreadyinXML.contains(sBusId)) {
                    slPartAlreadyinXML.addElement(sBusId);
                    if (slSons.size() == 0) {
                        String s3DViewableFileName = get3DViewableFileName(context, sBusId);
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
                                    // add new reference to the others JT files (if more than one JT)
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

        }

        vPLMXML.add("   </InstanceGraph>");
        vPLMXML.add("</ProductDef>");
        vPLMXML.add("</PLMXML>");

        return vPLMXML;
    }

    @SuppressWarnings("rawtypes")
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
    @SuppressWarnings("rawtypes")
    private String get3DViewableFileName(Context context, String sCADObjectId) throws Exception {
        try {
            StringList slBus = new StringList();
            slBus.addElement(DomainConstants.SELECT_TYPE);
            slBus.addElement(DomainConstants.SELECT_NAME);
            slBus.addElement(DomainConstants.SELECT_ID);
            slBus.addElement("format[" + getSchemaProperty(context, SYMBOLIC_format_JT) + "].file.name");

            DomainObject dobCADObject = DomainObject.newInstance(context, sCADObjectId);
            // TIGTK-3663 : START
            Map mViewableObject = getViewableInfos(context, dobCADObject, getSchemaProperty(context, SYMBOLIC_type_VIEWABLE), slBus);
            // TIGTK-3663 : END
            String _3DViewableFileName = "";
            if (mViewableObject != null) {
                _3DViewableFileName = (String) mViewableObject.get("format[" + getSchemaProperty(context, SYMBOLIC_format_JT) + "].file.name");
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
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Map getViewableInfos(Context context, DomainObject doCADObject, String sTypePattern, StringList slSelect) throws Exception {
        Map<String, Object> mReturn = null;

        try {
            String FORMAT_JT = getSchemaProperty(context, SYMBOLIC_format_JT);
            String RELATIONSHIP_VIEWABLE = getSchemaProperty(context, SYMBOLIC_relationship_Viewable);

            SelectList sl = new SelectList();
            sl.addAll(slSelect);
            sl.addElement(DomainConstants.SELECT_TYPE);
            // TIGTK-3663 : START
            MapList mlViewableObject = doCADObject.getRelatedObjects(context, RELATIONSHIP_VIEWABLE, sTypePattern, sl, null, false, true, (short) 1, "", "", 0, null, null, null);

            if (mlViewableObject.size() > 0) {
                mReturn = new HashMap<String, Object>();

                for (int a = 0; a < mlViewableObject.size(); a++) {
                    String strFileName = (String) ((Map) mlViewableObject.get(a)).get("format[" + FORMAT_JT + "].file.name");
                    if (strFileName.contains(".jt")) {
                        mReturn.put("format[" + FORMAT_JT + "].file.name", strFileName);
                    }
                }
            }
        } catch (FrameworkException e) {
            logger.error("Error in getViewableInfos()\n", e);
            throw e;
        }
        return mReturn;
    }

    /**
     * Gets CAD BOM info from a CAD Definition
     * @param context
     * @param args
     * @param maxLevel
     * @return
     * @throws Exception
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private MapList getCADBomData(Context context, String sObjectId, int maxLevel) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("getCADBomData() START");
        }
        MapList mlCatBom = new MapList();

        SelectList slSelectCatObj = new SelectList();
        slSelectCatObj.addId();
        slSelectCatObj.addType();
        slSelectCatObj.addCurrentState();
        slSelectCatObj.addPolicy();
        slSelectCatObj.addName();
        slSelectCatObj.addRevision();
        slSelectCatObj.addDescription();

        SelectList slSelectCatRel = new SelectList();
        slSelectCatRel.addElement("id[connection]");

        // head part
        DomainObject dob = new DomainObject(sObjectId);
        Map mCADObject = dob.getInfo(context, slSelectCatObj);
        mCADObject.put("level", String.valueOf(0));
        mlCatBom.add(mCADObject);

        // get children
        String RELATIONSHIP_CAD_SUBCOMPONENT = getSchemaProperty(context, SYMBOLIC_relationship_CADSubComponent);
        getCatBomOnSpecificLevel(context, mCADObject, RELATIONSHIP_CAD_SUBCOMPONENT, slSelectCatObj, slSelectCatRel, mlCatBom, 0, maxLevel);

        if (logger.isDebugEnabled()) {
            logger.debug("getCADBomData() - mlCatBom : " + mlCatBom);
        }

        return mlCatBom;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void getCatBomOnSpecificLevel(Context context, Map mInfoCatElement, String sRelPattern, SelectList slSelectObj, SelectList slSelectRel, MapList mlResult, int level, int maxLevel)
            throws Exception {
        String sCatElementId = (String) mInfoCatElement.get("id");
        logger.debug("getCatBomOnSpecificLevel() - sCatElementId = <" + sCatElementId + "> level = <" + level + ">");

        DomainObject dob = DomainObject.newInstance(context, sCatElementId);
        MapList mlChildren = dob.getRelatedObjects(context, sRelPattern, "*", slSelectObj, slSelectRel, false, true, (short) 1, "", "", 0);

        for (Iterator<?> it = mlChildren.iterator(); it.hasNext();) {
            Map<String, Object> mInfo = (Map<String, Object>) it.next();
            mInfo.put("level", String.valueOf(level + 1));
            mlResult.add(mInfo);
            if (level + 1 < maxLevel) {
                getCatBomOnSpecificLevel(context, mInfo, sRelPattern, slSelectObj, slSelectRel, mlResult, level + 1, maxLevel);
            }
        }

    }

}