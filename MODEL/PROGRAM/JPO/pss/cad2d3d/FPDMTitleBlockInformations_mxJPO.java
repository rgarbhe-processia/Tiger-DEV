package pss.cad2d3d;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import matrix.db.Context;
import matrix.db.JPO;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class FPDMTitleBlockInformations_mxJPO {
    // TIGTK-5406 - 03-04-2017 - VP - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(pss.cad2d3d.FPDMTitleBlockInformations_mxJPO.class);

    // TIGTK-5406 - 03-04-2017 - VP - END
    private Hashtable<String, Object> htTitleBlockInformations = new Hashtable<String, Object>();

    private String sCADGeometryType = "";

    public FPDMTitleBlockInformations_mxJPO() throws Exception {

    }

    public FPDMTitleBlockInformations_mxJPO(Context context, String[] args) throws Exception {
    }

    public Hashtable<String, Object> retrieveTitleblockInformation(Context context, String[] args) throws Exception {
        Hashtable<String, Object> htTitleBlockInfos;
        try {
            HashMap<?, ?> hmParams = (HashMap<?, ?>) JPO.unpackArgs(args);
            String strPattern = (String) hmParams.get("strPattern");
            String strDrawinghistory = (String) hmParams.get("strDrawinghistory");
            String strLinkedParts = (String) hmParams.get("strLinkedParts");
            String sIsUG = (String) hmParams.get("IS_UG");
            boolean bHistory = false;
            boolean bLinkedPart = false;
            boolean bULS = false;
            boolean bBasisDef = false;
            boolean bCAD = false;
            boolean bBasisDefHist = false;
            switch (strPattern) {
            case "FAS FAE":
                if ("yes".equals(strDrawinghistory)) {
                    bHistory = true;
                }
                if ("yes".equals(strLinkedParts)) {
                    bLinkedPart = true;
                }
                bBasisDef = true;
                bCAD = true;
                bULS = true;
                break;
            case "FECT":
                break;
            case "Basis Definition":
                bBasisDefHist = true;
                bCAD = true;
                break;
            case "Renault Nissan":
                if ("yes".equals(strDrawinghistory)) {
                    bHistory = true;
                }
                if ("yes".equals(strLinkedParts)) {
                    bLinkedPart = true;
                }
                bCAD = true;
                bULS = true;
                break;
            default:
                logger.info("Invalid choice");
            }

            // boolean bBonFabrique = (Boolean) hmParams.get("BonFabrique");
            // String sULS = (String) hmParams.get("ULS");
            String strCheckoutDir = (String) hmParams.get("strCheckoutDir");
            String strXMLFileName = (String) hmParams.get("strXMLFileName");

            File inputFile = new File(strCheckoutDir + "/" + strXMLFileName);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            if (bCAD) {
                NodeList nListCADBlock = doc.getElementsByTagName("CADBLOCK");
                if (nListCADBlock.getLength() != 0) {
                    // String strNodeName = "";

                    for (int temp = 0; temp < nListCADBlock.getLength(); temp++) {
                        Node nNode = nListCADBlock.item(temp);
                        if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element eElement = (Element) nNode;
                            htTitleBlockInformations.put("CADMODEL_DISPLAYNAME", eElement.getElementsByTagName("CADNAME").item(0).getTextContent());
                            htTitleBlockInformations.put("CADMODEL_DISPLAYREVISION", eElement.getElementsByTagName("CADREVISION").item(0).getTextContent());
                            htTitleBlockInformations.put("CADMODEL_STATE", eElement.getElementsByTagName("STATUS").item(0).getTextContent().trim());
                            htTitleBlockInformations.put("CADMODEL_DATE", eElement.getElementsByTagName("PROMOTIONDATE").item(0).getTextContent());
                            // htTitleBlockInformations.put("CADMODEL_GENERATEUR", (String) mCADInfo.get("attribute[" + FPDMConstants_mxJPO.ATTRIBUTE_CAD_SOFTWARE + "]"));

                            String strDimStd = eElement.getElementsByTagName("DIMENSIONINGSTANDARD").item(0).getTextContent();
                            if (strDimStd.contains(",")) {
                                String[] arrDimStd = strDimStd.split(",");
                                htTitleBlockInformations.put("CADMODEL_DIMENSIONSTD1", arrDimStd[0]);
                                htTitleBlockInformations.put("CADMODEL_DIMENSIONSTD2", arrDimStd[1]);
                                htTitleBlockInformations.put("CADMODEL_DIMENSIONSTD3", arrDimStd[2]);
                                htTitleBlockInformations.put("CADMODEL_DIMENSIONSTD4", arrDimStd[3]);
                            } else {
                                htTitleBlockInformations.put("CADMODEL_DIMENSIONSTD1", "");
                                htTitleBlockInformations.put("CADMODEL_DIMENSIONSTD2", "");
                                htTitleBlockInformations.put("CADMODEL_DIMENSIONSTD3", "");
                                htTitleBlockInformations.put("CADMODEL_DIMENSIONSTD4", "");
                            }
                            htTitleBlockInformations.put("CADMODEL_UNDIMRADIUS", eElement.getElementsByTagName("INNERRADIUS").item(0).getTextContent());
                            htTitleBlockInformations.put("CADMODEL_ANGULARTOL", eElement.getElementsByTagName("ANGULARTOLERANCE").item(0).getTextContent());
                            htTitleBlockInformations.put("CADMODEL_LINEARTOL", eElement.getElementsByTagName("LINEARTOLERANCE").item(0).getTextContent());
                            htTitleBlockInformations.put("CADMODEL_SCALE", eElement.getElementsByTagName("SCALE").item(0).getTextContent());
                            htTitleBlockInformations.put("CADMODEL_VIEWCONV", eElement.getElementsByTagName("VIEWCONV").item(0).getTextContent());
                            htTitleBlockInformations.put("CADMODEL_DESCRIPTION", eElement.getElementsByTagName("HEADING").item(0).getTextContent());
                            htTitleBlockInformations.put("CADMODEL_GEOMETRYTYPE", sCADGeometryType);

                            htTitleBlockInformations.put("VIEWABLE_FOLIO", eElement.getElementsByTagName("SHEET").item(0).getTextContent().trim());
                            htTitleBlockInformations.put("VIEWABLE_FILEFORMAT", eElement.getElementsByTagName("FORMAT").item(0).getTextContent());
                            htTitleBlockInformations.put("CADMODEL_NBOFFOLIOS", eElement.getElementsByTagName("SHEET").item(0).getTextContent().trim());
                            htTitleBlockInformations.put("CADMODEL_ISUG", sIsUG);
                        }
                    }
                }
            }
            Vector<Hashtable<String, Object>> vLinkedPartsWithPartSpec = new Vector<Hashtable<String, Object>>();
            Vector<Hashtable<String, Object>> vLinkedBasisDef = new Vector<Hashtable<String, Object>>();

            // Linked part block start
            if (bLinkedPart) {
                NodeList nListPart = doc.getElementsByTagName("LINKEDPARTBLOCK");
                if (nListPart.getLength() != 0) {
                    for (int temp = 1; temp < nListPart.getLength(); temp++) {
                        Node nNode = nListPart.item(temp);
                        if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element eElement = (Element) nNode;
                            Hashtable<String, Object> htTemp = new Hashtable<String, Object>();

                            htTemp.put("NAME", eElement.getElementsByTagName("NAME").item(0).getTextContent());
                            htTemp.put("REVISION", eElement.getElementsByTagName("REVISION").item(0).getTextContent());
                            htTemp.put("STATE", eElement.getElementsByTagName("STATUS").item(0).getTextContent());
                            htTemp.put("MASS", eElement.getElementsByTagName("MASS").item(0).getTextContent());
                            htTemp.put("PARTSAFETYCLASS", eElement.getElementsByTagName("PARTSAFETYCLASS").item(0).getTextContent().trim());
                            htTemp.put("MATERIALSAFETYCLASS", eElement.getElementsByTagName("MATERIALSAFETYCLASS").item(0).getTextContent().trim());
                            htTemp.put("TREATSAFETYCLASS", eElement.getElementsByTagName("TREATMENTSAFETYCLASS").item(0).getTextContent().trim());
                            htTemp.put("TREATMENT", eElement.getElementsByTagName("HEATTREATMENT").item(0).getTextContent());
                            htTemp.put("MATERIAL", eElement.getElementsByTagName("MATERIALSTANDARD").item(0).getTextContent());
                            htTemp.put("MATERIALNORM", eElement.getElementsByTagName("SEMIMANUFACTURESTANDARD").item(0).getTextContent());
                            htTemp.put("SYMETRIC", eElement.getElementsByTagName("SYMETRIC").item(0).getTextContent());
                            htTemp.put("SYMREVISION", eElement.getElementsByTagName("SYMREVISION").item(0).getTextContent());
                            htTemp.put("INPAIR", eElement.getElementsByTagName("X2").item(0).getTextContent());
                            vLinkedPartsWithPartSpec.addElement(htTemp);
                        }
                    }
                }
                htTitleBlockInformations.put("PartsLinked", vLinkedPartsWithPartSpec);
            }

            // Linked part block end
            // Basis Definition Block start
            if (bBasisDef) {
                NodeList nListBasisDef = doc.getElementsByTagName("BASISDEFINITIONDESCRIPTION");
                if (nListBasisDef.getLength() != 0) {
                    for (int temp = 0; temp < nListBasisDef.getLength(); temp++) {
                        Node nNode = nListBasisDef.item(temp);
                        if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element eElement = (Element) nNode;
                            Hashtable<String, Object> htLinkedBD = new Hashtable<String, Object>();

                            htLinkedBD.put("NAME", eElement.getElementsByTagName("BASISDEFNAME").item(0).getTextContent());
                            htLinkedBD.put("REVISION", eElement.getElementsByTagName("BASISDEFREVISION").item(0).getTextContent());
                            htLinkedBD.put("STATE", eElement.getElementsByTagName("STATUS").item(0).getTextContent());
                            htLinkedBD.put("DRAWINGS", eElement.getElementsByTagName("BASISDEFINITION").item(0).getTextContent());
                            htLinkedBD.put("DESCRIPTION", eElement.getElementsByTagName("DESCRIPTION").item(0).getTextContent());

                            vLinkedBasisDef.addElement(htLinkedBD);
                        }
                    }
                }
                htTitleBlockInformations.put("BasisInformation", vLinkedBasisDef);
            }
            // ULS Block start
            if (bULS) {
                NodeList nListULSBlock = doc.getElementsByTagName("ULSBLOCK");
                if (nListULSBlock.getLength() != 0) {
                    for (int temp = 0; temp < nListULSBlock.getLength(); temp++) {
                        Node nNode = nListULSBlock.item(temp);
                        if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element eElement = (Element) nNode;
                            String temp_ = "";
                            String blnk = "";
                            temp_ = ((eElement.getElementsByTagName("PROJECT").getLength()) != 0) ? eElement.getElementsByTagName("PROJECT").item(0).getTextContent() : blnk;
                            htTitleBlockInformations.put("PROJECT", temp_);
                            temp_ = ((eElement.getElementsByTagName("VEHICLE").getLength()) != 0) ? eElement.getElementsByTagName("VEHICLE").item(0).getTextContent() : blnk;
                            htTitleBlockInformations.put("VEHICLE", temp_);
                            temp_ = ((eElement.getElementsByTagName("PROGRAM").getLength()) != 0) ? eElement.getElementsByTagName("PROGRAM").item(0).getTextContent() : blnk;
                            htTitleBlockInformations.put("PROGRAM", temp_);
                            temp_ = ((eElement.getElementsByTagName("DEPT_NAME").getLength()) != 0) ? eElement.getElementsByTagName("DEPT_NAME").item(0).getTextContent() : blnk;
                            htTitleBlockInformations.put("DEPT_NAME", temp_);
                            temp_ = ((eElement.getElementsByTagName("DEPT_STREET").getLength()) != 0) ? eElement.getElementsByTagName("DEPT_STREET").item(0).getTextContent() : blnk;
                            htTitleBlockInformations.put("DEPT_STREET", temp_);
                            temp_ = ((eElement.getElementsByTagName("DEPT_CITY").getLength()) != 0) ? eElement.getElementsByTagName("DEPT_CITY").item(0).getTextContent() : blnk;
                            htTitleBlockInformations.put("DEPT_CITY", temp_);
                            temp_ = ((eElement.getElementsByTagName("DEPT_PHONE").getLength()) != 0) ? eElement.getElementsByTagName("DEPT_PHONE").item(0).getTextContent() : blnk;
                            htTitleBlockInformations.put("DEPT_PHONE", temp_);
                        }
                    }
                }
            }

            // ULS Block end

            // Basis Definition History Block start
            if (bBasisDefHist) {
                Vector<Hashtable<String, String>> vToReturn = new Vector<Hashtable<String, String>>();
                NodeList nListBasisDefHist = doc.getElementsByTagName("BASISDEFINITIONHISTORY");
                if (nListBasisDefHist.getLength() != 0) {
                    for (int temp = 0; temp < nListBasisDefHist.getLength() - 1; temp++) {
                        Hashtable<String, String> htBasisDef = new Hashtable<String, String>();
                        Node nNode = nListBasisDefHist.item(temp);
                        if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element eElement = (Element) nNode;

                            htBasisDef.put("BASIS_DEF_REVISION", eElement.getElementsByTagName("REVISION").item(0).getTextContent());
                            htBasisDef.put("BASIS_DEF_STATE", eElement.getElementsByTagName("STATE").item(0).getTextContent());
                            htBasisDef.put("BASIS_DEF_ORIGINATED", eElement.getElementsByTagName("ORIGINATED").item(0).getTextContent());
                            htBasisDef.put("BASIS_DEF_LASTPROMOTE", eElement.getElementsByTagName("LASTPROMOTE").item(0).getTextContent());
                            htBasisDef.put("BASIS_DEF_ORIGINATOR", eElement.getElementsByTagName("OWNER").item(0).getTextContent());
                            htBasisDef.put("BASIS_DEF_DESCRIPTION", eElement.getElementsByTagName("CHECKINREASON").item(0).getTextContent());
                            vToReturn.addElement(htBasisDef);
                        }
                    }
                }
                htTitleBlockInformations.put("HistoryInfosForBasisDefinition", vToReturn);
            }
            // Basis Definition History Block end

            // history block start
            Vector<Hashtable<String, Object>> vPartECO = new Vector<Hashtable<String, Object>>();
            if (bHistory) {
                NodeList nListHistory = doc.getElementsByTagName("HISTORY");
                if (nListHistory.getLength() != 0) {
                    for (int temp = 0; temp < (nListHistory.getLength() - 1); temp++) {
                        Hashtable<String, Object> htHistory = new Hashtable<String, Object>();

                        Node nNode = nListHistory.item(temp);
                        if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element eElement = (Element) nNode;

                            htHistory.put("PART_REVISION", eElement.getElementsByTagName("REVISION").item(0).getTextContent());
                            htHistory.put("PART_STATE", eElement.getElementsByTagName("STATUS").item(0).getTextContent());
                            htHistory.put("CO_STATE", eElement.getElementsByTagName("STATUS").item(0).getTextContent());

                            String sCRCONumbers = eElement.getElementsByTagName("ECOECRNUMBER").item(0).getTextContent();
                            htHistory.put("ECO_NAME", sCRCONumbers);
                            htHistory.put("COCR_NUMBERS", pss.cad2d3d.TitleBlockUtil_mxJPO.split(sCRCONumbers, ","));
                            String sDateAuthor = eElement.getElementsByTagName("DATEAUTHOROFCHANGE").item(0).getTextContent();
                            htHistory.put("ECO_ORIGINATED", sDateAuthor);

                            String sCOOriginated = "";
                            String sCOOriginator = "";
                            if (sDateAuthor.length() > 0) {
                                String[] arrDateAuthor = sDateAuthor.trim().split("(<br\\/>|<br \\/>|,)");
                                if (arrDateAuthor.length >= 2) {
                                    sCOOriginated = arrDateAuthor[0];
                                    sCOOriginator = arrDateAuthor[1];
                                }
                            }
                            htHistory.put("CO_ORIGINATED", sCOOriginated);
                            htHistory.put("CO_ORIGINATOR", sCOOriginator);

                            String s_ItemToChange = eElement.getElementsByTagName("ITEMTOCHANGE").item(0).getTextContent();
                            htHistory.put("ITEMTOCHANGE", s_ItemToChange);
                            htHistory.put("PARTS_IMPACTED", pss.cad2d3d.TitleBlockUtil_mxJPO.split(s_ItemToChange, ","));

                            String strECODesc = eElement.getElementsByTagName("CHANGEDESCRIPTION").item(0).getTextContent();
                            String[] arrECODesc = strECODesc.split("(<br\\/>|<br \\/>|\\n)");
                            Vector<String> vECODesc = new Vector<String>(Arrays.asList(arrECODesc));
                            htHistory.put("ECO_DESCRIPTION", vECODesc);

                            vPartECO.add(htHistory);
                        }
                    }
                }
                htTitleBlockInformations.put("HistoryInfos", vPartECO);
            }

            // history block end
            // vPartECO = historyInfo.getHistoryInfos(bBonFabrique);

            htTitleBlockInfos = new Hashtable<String, Object>(htTitleBlockInformations);
        } catch (Exception e) {
            logger.error("Error in retrieveTitleblockInformation()\n", e);
            throw e;
        }
        return htTitleBlockInfos;
    }

}
