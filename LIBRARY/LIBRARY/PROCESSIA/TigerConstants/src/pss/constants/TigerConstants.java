package pss.constants;

import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.effectivity.EffectivityFramework;

import matrix.db.Context;
import matrix.db.JPOSupport;
import matrix.util.StringList;

public abstract class TigerConstants {
    // Attribute Constants

    public static final String ATTRIBUTE_WORK_FLOW_DUE_DATE;

    public static final String ATTRIBUTE_ABSENCEDELEGATE;

    public static final String ATTRIBUTE_ABSENCESTARTDATE;

    public static final String ATTRIBUTE_ABSENCEENDDATE;

    public static final String ATTRIBUTE_ADDRESS1;

    public static final String ATTRIBUTE_ALTERNATE_DESCRIPTION;

    public static final String ATTRIBUTE_AUTOSTOPONREJECTION;

    public static final String ATTRIBUTE_APPROVAL_STATUS;

    public static final String ATTRIBUTE_BRANCH_TO;

    public static final String ATTRIBUTE_COMPONENTLOCATION;

    public static final String ATTRIBUTE_CUSTOMER_DESCRIPTION;
    
    public static final String ATTRIBUTE_COOWNER;

    public static final String ATTRIBUTE_DEFAULTPARTPOLICY;

    public static final String ATTRIBUTE_DISPLAY_NAME;

    public static final String ATTRIBUTE_DISPLAYNAME;

    public static final String ATTRIBUTE_EFFECTIVITY_VARIABLE_INDEXES;

    public static final String ATTRIBUTE_EFFECTIVITYCOMPILEDFORM;

    public static final String ATTRIBUTE_EFFECTIVITYEXPRESSION;

    public static final String ATTRIBUTE_EFFECTIVITYEXPRESSIONBINARY;

    public static final String ATTRIBUTE_EFFECTIVITYORDEREDCRITERIA;

    public static final String ATTRIBUTE_EFFECTIVITYORDEREDCRITERIADICTIONARY;

    public static final String ATTRIBUTE_EFFECTIVITYORDEREDIMPACTINGCRITERIA;

    public static final String ATTRIBUTE_EFFECTIVITYPROPOSEDEXPRESSION;

    public static final String ATTRIBUTE_EFFECTIVITYTYPES;

    public static final String ATTRIBUTE_ENABLECOMPLIANCE;

    public static final String ATTRIBUTE_EQUIPMENT_TYPE;

    public static final String ATTRIBUTE_FAURECIA_FULL_LENGTH_DESC;

    public static final String ATTRIBUTE_FAURECIA_SHORT_LENGTH_DESC;

    public static final String ATTRIBUTE_FILTERCOMPILEDFORM;

    public static final String ATTRIBUTE_FPDM_RTDEFAULT;

    public static final String ATTRIBUTE_FPDM_RTPURPOSE;

    public static final String ATTRIBUTE_ISVPLMVISIBLE;

    public static final String ATTRIBUTE_MARKETINGNAME;
    
    public static final String ATTRIBUTE_PSS_MANDATORYDELIVERABLE;

    public static final String ATTRIBUTE_ORGANIZATION_PHONE_NUMBER;

    public static final String ATTRIBUTE_PDM_CLASS;

    public static final String ATTRIBUTE_PLM_EXTERNALID;

    public static final String ATTRIBUTE_PLMENTITY_V_DESCRIPTION;

    public static final String ATTRIBUTE_PLMINSTANCE_V_TREEORDER;

    public static final String ATTRIBUTE_PSS_ACTUALEFFECTIVITYDATE;

    public static final String ATTRIBUTE_PSS_ACTUALIMPLEMENTATIONDATE;

    public static final String ATTRIBUTE_PSS_ASSESSMENT_PARTCOST;

    public static final String ATTRIBUTE_PSS_ASSESSMENT_PC_L;

    public static final String ATTRIBUTE_PSS_ASSESSMENT_PROCESSRISK;

    public static final String ATTRIBUTE_PSS_ASSESSMENT_PRODUCTRISK;

    public static final String ATTRIBUTE_PSS_MANDATORYCR;

    public static final String ATTRIBUTE_PSS_ASSESSMENTDEVTCOST;

    public static final String ATTRIBUTE_PSS_ASSESSMENTTOOLINGCOST_GAUGES;

    public static final String ATTRIBUTE_PSS_BPUPDATEDATE;

    public static final String ATTRIBUTE_PSS_CAPEXCONTRIB_LAUNCHCOST;

    public static final String ATTRIBUTE_PSS_CATYPE;

    public static final String ATTRIBUTE_PSS_CHECKAPPROVEORABSTAIN;

    public static final String ATTRIBUTE_PSS_CHECKFLAGFORPASSEDTRIGGER;

    public static final String ATTRIBUTE_PSS_CNREASONFORCANCELLATION;

    public static final String ATTRIBUTE_PSS_CNTYPE;

    public static final String ATTRIBUTE_PSS_COLORABLE;

    public static final String ATTRIBUTE_PSS_COLORCODE;

    public static final String ATTRIBUTE_PSS_COLORPID;

    public static final String ATTRIBUTE_PSS_CONTROLPLAN;

    public static final String ATTRIBUTE_PSS_COSTARTDATE;

    public static final String ATTRIBUTE_PSS_COUNTERMEASURE;

    public static final String ATTRIBUTE_PSS_COVIRTUALIMPLEMENTATIONDATE;

    public static final String ATTRIBUTE_PSS_CRBILLABLE;

    public static final String ATTRIBUTE_PSS_CRCUSTOMERAGREEMENTDATE;

    public static final String ATTRIBUTE_PSS_CRCUSTOMERCHANGENUMBER;

    public static final String ATTRIBUTE_PSS_CRCUSTOMERINVOLVEMENT;

    public static final String ATTRIBUTE_PSS_CRDATEOFLASTTRANSFERTOCHANGEMANAGER;

    public static final String ATTRIBUTE_PSS_CRFASTTRACK;

    public static final String ATTRIBUTE_PSS_CRFASTTRACKCOMMENT;

    public static final String ATTRIBUTE_PSS_CRORIGINSUBTYPE;

    public static final String ATTRIBUTE_PSS_CRORIGINTYPE;

    public static final String ATTRIBUTE_PSS_CRREASONFORCHANGE;

    public static final String ATTRIBUTE_PSS_CRREQUESTEDASSESSMENTENDDATE;

    public static final String ATTRIBUTE_PSS_CRTARGETCHANGEIMPLEMENTATIONCOMMENT;

    public static final String ATTRIBUTE_PSS_CRTARGETCHANGEIMPLEMENTATIONDATE;

    public static final String ATTRIBUTE_PSS_CRTITLE;

    public static final String ATTRIBUTE_PSS_CRTYPE;

    public static final String ATTRIBUTE_PSS_CUSTOMEFFECTIVITYEXPRESSION;

    public static final String ATTRIBUTE_PSS_CUSTOMERPARTNUMBER;

    public static final String ATTRIBUTE_PSS_DECISION;

    public static final String ATTRIBUTE_PSS_DECISION_CUSTOMERSTATUS;

    public static final String ATTRIBUTE_PSS_DECISION_DATE;

    public static final String ATTRIBUTE_PSS_DECISIONCASHPAYMENT;

    public static final String ATTRIBUTE_PSS_DESIGNREVIEWDATE;

    public static final String ATTRIBUTE_PSS_DESIGNREVIEWSTATUS;

    public static final String ATTRIBUTE_PSS_DOMAIN;

    public static final String ATTRIBUTE_PSS_DUEDATE;

    public static final String ATTRIBUTE_PSS_DVP_PVP;

    public static final String ATTRIBUTE_PSS_E2ECODURATIONATPREPARESTATE;

    public static final String ATTRIBUTE_PSS_E2ECRDURATIONATSUBMITSTATE;

    public static final String ATTRIBUTE_PSS_E2EESCALATIONDELAYEDCHGSFREQUENCY;

    public static final String ATTRIBUTE_PSS_E2EESCALATIONOPENEDCHGSFREQUENCY;

    public static final String ATTRIBUTE_PSS_E2EMANAGERNAME;

    public static final String ATTRIBUTE_PSS_E2EMCODURATIONATPREPARESTATE;

    public static final String ATTRIBUTE_PSS_E2ENOTIFICATIONDELAYEDCHGSFREQUENCY;

    public static final String ATTRIBUTE_PSS_E2ENOTIFICATIONOPENEDCHGSFREQUENCY;

    public static final String ATTRIBUTE_PSS_E2ENOTIFICATIONREMAINDERFREQUENCY;

    public static final String ATTRIBUTE_PSS_E2EOPENEDCNDURATION;

    public static final String ATTRIBUTE_PSS_E2EPROJECTNOTIFICATIONACTIVATION;

    public static final String ATTRIBUTE_PSS_EFFECTIVITYDATE;

    public static final String ATTRIBUTE_PSS_FINALPAYMENTAGREEMENTCONFIRMED;

    public static final String ATTRIBUTE_PSS_FINALPAYMENTAGREEMENTCONFIRMED_RANGEE_NO;

    public static final String ATTRIBUTE_PSS_FINALPAYMENTAGREEMENTCONFIRMED_RANGEE_YES;
    
    public static final String ATTRIBUTE_PSS_FILLERNATUREFORMATERIAL;
    
    public static final String ATTRIBUTE_PSS_FILLERCONTENTMATERIAL;   

    public static final String ATTRIBUTE_PSS_FLAGFORPROMOTECO;

    public static final String ATTRIBUTE_PSS_FMEA_D_P_L;

    public static final String ATTRIBUTE_PSS_GEOMETRYTYPE;

    public static final String ATTRIBUTE_PSS_HARMONIES_INSTANCE;

    public static final String ATTRIBUTE_PSS_HARMONIES_REFERENCE;
    
    public static final String ATTRIBUTE_PSS_HEIGHT;
    
    public static final String ATTRIBUTE_PSS_HEAT_RESISTANCE_LEVEL;
    
    public static final String ATTRIBUTE_PSS_IDENTIFICATION_NUMBER;
    
    public static final String ATTRIBUTE_PSS_IMPACT_RESISTANCE;
    
    public static final String ATTRIBUTE_PSS_IMPACT_HEAT_RESISTANCE_BALANCE;
    
    public static final String ATTRIBUTE_PSS_IMPACTED_REGIONS;

    public static final String ATTRIBUTE_PSS_IMPACTONACTBP;
    
    public static final String ATTRIBUTE_PSS_CADMass;

    public static final String ATTRIBUTE_PSS_CAD_System_Mass;

    public static final String ATTRIBUTE_PSS_CAD_Parameter_Mass;

    public static final String ATTRIBUTE_PSS_EBOM_CADMass;

    public static final String ATTRIBUTE_PSS_EBOM_Mass1;

    public static final String ATTRIBUTE_PSS_EBOM_Mass2;

    public static final String ATTRIBUTE_PSS_EBOM_Mass3;

    public static final String ATTRIBUTE_PSS_MANUFACTURINGITEMEXTPSS_GROSSWEIGHT;

    public static final String ATTRIBUTE_PSS_MANUFACTURINGITEMEXTPSS_NETWEIGHT;

    public static final String ATTRIBUTE_PSS_PUBLISHEDPARTPSS_PP_CADMASS;

    public static final String ATTRIBUTE_PSS_GROSSWEIGHT;

    public static final String ATTRIBUTE_PSS_NETWEIGHT;

    public static final String ATTRIBUTE_PSS_PURPOSEOFRELEASE_DEVPART;

    public static final String ATTRIBUTE_PSS_INTERCHANGEABILITY;

    public static final String ATTRIBUTE_PSS_INTERCHANGEABILITYPROPAGATIONSTATUS;

    public static final String ATTRIBUTE_PSS_KCC_KPC;

    public static final String ATTRIBUTE_PSS_LINEDATA_NUMBER;

    public static final String ATTRIBUTE_PSS_LINEDATA_PSS_NUMBEROFCOLORCHANGES;

    public static final String ATTRIBUTE_PSS_MASTER_RnD_CENTER;
    
    public static final String ATTRIBUTE_PSS_MASTERBATCHMATRIXNATURE;

    public static final String ATTRIBUTE_PSS_MANUFACTURING_INSTANCEEXT_PSS_PHANTOM_LEVEL;

    public static final String ATTRIBUTE_PSS_MANUFACTURING_INSTANCEEXT_PSS_PHANTOM_PART;

    public static final String ATTRIBUTE_PSS_MANUFACTURINGITEMEXT_PSS_CUSTOMER_PART_NUMBER;

    public final static String ATTRIBUTE_PSS_MANUFACTURINGITEMEXT_PSS_SPARE;

    public static final String ATTRIBUTE_PSS_MBOM_STRUCTURENODE;

    public static final String ATTRIBUTE_PSS_MBOM_FCSINDEX;
    
    public static final String ATTRIBUTE_PSS_MCO_START_DATE;
    
    public static final String ATTRIBUTE_PSS_MINERALFILLERCONTENT;

    public static final String ATTRIBUTE_PSS_OEMCODE;

    public static final String ATTRIBUTE_PSS_OLD_MATERIAL_NUMBER;

    public static final String ATTRIBUTE_PSS_OPERATION_BG;

    public static final String ATTRIBUTE_PSS_OPERATION_NUMBER;

    public static final String ATTRIBUTE_PSS_OPERATION_PSS_COLOR;

    public static final String ATTRIBUTE_PSS_OPERATION_PSS_OPNCUSTOMER_PART_NUMBER;

    public static final String ATTRIBUTE_PSS_OPERATION_PSS_HARMONY;

    public static final String ATTRIBUTE_PSS_OPERATION_PSS_QUANTITY;

    public static final String ATTRIBUTE_PSS_OPERATION_PSS_TITLE;

    public static final String ATTRIBUTE_PSS_OPERATION_PSS_VARIANTNAME;

    public static final String ATTRIBUTE_PSS_OPERATION_TECHNOLOGY;

    public static final String ATTRIBUTE_PSS_OPERATIONLINEDATA_EXT_PSS_DIRTY;

    public static final String ATTRIBUTE_PSS_PARALLELTRACK;

    public static final String ATTRIBUTE_PSS_PARALLELTRACKCOMMENT;

    public static final String ATTRIBUTE_PSS_PARTPRICE;

    public static final String ATTRIBUTE_PSS_PHYSICAL_IMPLEMENTATION_PLANNED_DATE;

    public static final String ATTRIBUTE_PSS_PLANNEDENDDATE;

    public static final String ATTRIBUTE_PSS_PLANTNAME;
    
    public static final String ATTRIBUTE_PSS_PLATABLEFORMATERIAL;

    public static final String ATTRIBUTE_PSS_PMSITEM;
    
    public static final String ATTRIBUTE_PSS_POSITION;

    public static final String ATTRIBUTE_PSS_PPAP_INTERN_OR_CUST_DATE;

    public static final String ATTRIBUTE_PSS_PPAP_INTERN_OR_CUST_STATUS;

    public static final String ATTRIBUTE_PSS_PROCESS;

    public static final String ATTRIBUTE_PSS_PROCESSCONTINUOUSPROVIDE_PSS_MATERIALTYPE;

    public static final String ATTRIBUTE_PSS_PRODUCT_CONFIGURATION_PID;

    public static final String ATTRIBUTE_PSS_PROGRAMPROJECT;

    public static final String ATTRIBUTE_PSS_PROGRAMTYPE;

    public static final String ATTRIBUTE_PSS_PROJECT_DESCRIPTION;

    public static final String ATTRIBUTE_PSS_PROJECT_PHASE_AT_CREATION;

    public static final String ATTRIBUTE_PSS_PROJECTPHASE;

    public static final String ATTRIBUTE_PSS_PUBLISHEDEBOM_INSTANCENAME;

    public final static String ATTRIBUTE_PSS_PUBLISHEDPART_PSS_CLASSIFICATIONLIST;

    public final static String ATTRIBUTE_PSS_PUBLISHEDPART_PSS_COLORLIST;

    public final static String ATTRIBUTE_PSS_PUBLISHEDPART_PSS_MATERIALLIST;

    public final static String ATTRIBUTE_PSS_PUBLISHEDPART_PSS_PARTSPAREPART;

    public final static String ATTRIBUTE_PSS_PUBLISHEDPART_PSS_TOOLINGLIST;

    public static final String ATTRIBUTE_PSS_PUBLISHEDPART_PSS_VARIANTASSEMBLYLIST;

    public static final String ATTRIBUTE_PSS_PURPOSE_OF_RELEASE;

    public static final String ATTRIBUTE_PSS_RAROLE;

    public static final String ATTRIBUTE_PSS_REQUESTED_CHANGE;

    public static final String ATTRIBUTE_PSS_ROLE;

    public static final String ATTRIBUTE_PSS_ROUTETEMPLATETYPE;

    public static final String ATTRIBUTE_PSS_SALESSTATUS;

    public static final String ATTRIBUTE_PSS_SAPDESCRIPTION;

    public static final String ATTRIBUTE_PSS_SAP_RESPONSE;
    
    public static final String ATTRIBUTE_PSS_SCRATCHRESISTANCEMATERIAL;

    public static final String ATTRIBUTE_PSS_SIMPLEREFFECTIVITYEXPRESSION;

    public static final String ATTRIBUTE_PSS_SLCCOMMENTS;

    public static final String ATTRIBUTE_PSS_STRUCTURENODE;

    public static final String ATTRIBUTE_PSS_SUPPLIER_TEAM_FEASIBILITY_COMMITMENT;

    public static final String ATTRIBUTE_PSS_SYMMETRICALPARTSIDENTICALMASS;

    public static final String ATTRIBUTE_PSS_SYMMETRICALPARTSIDENTICALMASS_RANGEE_YES;

    public static final String ATTRIBUTE_PSS_INWORKCONEWCRTAG_RANGE_YES;

    public static final String ATTRIBUTE_PSS_INWORKCONEWCRTAG_RANGE_NO;

    public static final String ATTRIBUTE_PSS_KEEPREFERENCEDOCUMENT_RANGE_YES;

    public static final String ATTRIBUTE_PSS_CLONECOLORDIVERSITY_RANGE_YES;

    public static final String ATTRIBUTE_PSS_CLONETECHNICALDIVERSITY_RANGE_YES;

    public static final String ATTRIBUTE_PSS_SYMMETRICALPARTSMANAGEINPAIRS;
    
    public static final String ATTRIBUTE_PSS_TECHNICALDESCRIPTION;

    public static final String ATTRIBUTE_PSS_TECHNOLOGY_CLASSIFICATION;

    public static final String ATTRIBUTE_PSS_TITLE;

    public static final String ATTRIBUTE_PSS_TOOLINGKICKOFFDATE;

    public static final String ATTRIBUTE_PSS_TOOLINGKICKOFFSTATUS;

    public static final String ATTRIBUTE_PSS_TOOLINGPRICE;

    public static final String ATTRIBUTE_PSS_TOOLINGREVIEWDATE;

    public static final String ATTRIBUTE_PSS_TOOLINGREVIEWSTATUS;

    public static final String ATTRIBUTE_PSS_TRANSFER_TO_SAP_EXPECTED;

    public static final String ATTRIBUTE_PSS_UNIT_OF_MEASURE;

    public static final String ATTRIBUTE_PSS_UNIT_OF_MEASURE_CATEGORY;

    public static final String ATTRIBUTE_PSS_VALIDATION_MVP_R_R;

    public static final String ATTRIBUTE_PSS_VARIANTASSEMBLY_PID;

    public static final String ATTRIBUTE_VARIANTDISPLAYTEXT;

    public static final String ATTRIBUTE_PSS_VARIANTID;

    public static final String ATTRIBUTE_PSS_VARIANTOPTIONS;

    public static final String ATTRIBUTE_PSS_VIEW;

    public static final String ATTRIBUTE_PSS_XMLSTRUCTURE;

    public static final String ATTRIBUTE_PSSPLANNEDENDDATE;

    public static final String ATTRIBUTE_QUANTITY;

    public static final String ATTRIBUTE_ROUTE_STATUS;

    public static final String ATTRIBUTE_SCHEDULEDCOMPLETIONDATE;

    public static final String ATTRIBUTE_SEQUENCEORDER;

    public static final String ATTRIBUTE_SOURCE;

    public static final String ATTRIBUTE_SUPPLIERS_PART_NUMBER;

    public static final String ATTRIBUTE_TYPE_OF_PART;

    public static final String ATTRIBUTE_V_EFFECTIVITYCOMPILEDFORM;

    public static final String ATTRIBUTE_V_HASCONFIGURATIONEFFECTIVITY;

    public static final String ATTRIBUTE_V_NAME;

    public static final String ATTRIBUTE_WEIGHT;

    public static final String ATTRIBUTE_PSS_OTHER_COMMENTS;

    public static final String ATTRIBUTE_PSS_TRANSFERFROMCRFLAG;

    public static final String ATTRIBUTE_DERIVED_CONTEXT;

    public static final String ATTRIBUTE_PSS_REPLACED_WITH_LATEST_REVISION;

    public static final String ATTRIBUTE_PSS_REFERENCE_EBOM_GENERATED;

    public static final String ATTRIBUTE_REQUESTED_CHANGE;

    public static final String ATTRIBUTE_PSS_TITLEBLOCKCOMPLETED;
    public static final String SELECT_ATTRIBUTE_PSS_TITLEBLOCKCOMPLETED;
    public static final String SELECT_ATTRIBUTE_PSS_TITLEBLOCKCOMPLETED_VALUE;
    public static final String ATTRIBUTE_PSS_DETAIL_SHEET_INFO;
    public static final String SELECT_ATTRIBUTE_PSS_DETAIL_SHEET_INFO;
    public static final String SELECT_ATTRIBUTE_PSS_DETAIL_SHEET_INFO_VALUE;
    public static final String ATTRIBUTE_PNO_VISIBILITY;
    public static final String SELECT_ATTRIBUTE_PNO_VISIBILITY;
    public static final String SELECT_ATTRIBUTE_PNO_VISIBILITY_VALUE;

    public static final String ATTRIBUTE_PLMINSTANCE_V_NAME;

    public static final String ATTRIBUTE_PLMINSTANCE_V_DESCRIPTION;

    public static final String ATTRIBUTE_PSS_TRADE_NAME;

    public static final String ATTRIBUTE_PSS_SUPPLIER;

    public static final String ATTRIBUTE_PSS_FAURECIASHORTLENGHTDESCRIPTION;

    public static final String ATTRIBUTE_PSS_COLORNAME;

    public static final String ATTRIBUTE_PSS_TECHNOLOGYFORMATERIAL;

    public static final String ATTRIBUTE_PSS_CROSSLINKING;

    public static final String ATTRIBUTE_PSS_LASERETCHING;
    
    public static final String ATTRIBUTE_PSS_GLASSFIBRECONTENTFORMATERIAL;

    public static final String ATTRIBUTE_PSS_GLOSS;

    public static final String ATTRIBUTE_PSS_GLOSSFORMATERIAL;

    public static final String ATTRIBUTE_PSS_POLISHABLE;

    public static final String ATTRIBUTE_PSS_SOFT;

    public static final String ATTRIBUTE_PSS_GTSTECHNICALFAMILYMATERIAL;

    public static final String ATTRIBUTE_PSS_MANUFACTURINGITEMEXTPSS_MAKEORBUY_MATERIAL;

    public static final String ATTRIBUTE_PSS_MANUFACTURINGINSTANCEEXTPSS_PHANTOM;

    public static final String ATTRIBUTE_PSS_AVAILABLE_UPDATE;

    public static final String ATTRIBUTE_PSS_AVAILABLE_UPDATEFLAG;

    public static final String ATTRIBUTE_PSS_AVAILABLE_INSTANCEUPDATE;

    public static final String ATTRIBUTE_PSS_AVAILABLE_INSTANCEUPDATEFLAG;

    public static final String ATTRIBUTE_PSS_MANUFACTURINGINSTANCEEXTPSS_ALLOWTOLERANCE;

    public static final String ATTRIBUTE_PSS_MANUFACTURINGINSTANCEEXTPSS_EFFECTIVERATIO;

    public static final String ATTRIBUTE_PSS_MANUFACTURINGINSTANCEEXTPSS_REFERENCERATIO;

    public static final String ATTRIBUTE_PSS_MANUFACTURINGINSTANCEEXTPSS_RATIOTOLERANCE;

    public static final String ATTRIBUTE_PSS_MANUFACTURINGITEMEXTPSS_MATERPLANTNAME;

    public static final String ATTRIBUTE_PSS_MANUFACTURINGPLANTEXTPSS_OWNERSHIP;

    public static final String ATTRIBUTE_PSS_MANUFACTURING_ITEMEXT_PSS_MAKEORBUYMATERIAL;

    public static final String ATTRIBUTE_FINDNUMBER;

    public static final String ATTRIBUTE_REFERENCEDESIGNATOR;

    public static final String ATTRIBUTE_PSS_PUBLISHEDPART_PSS_PARTCOLORABLE;

    public static final String ATTRIBUTE_HASMANUFACTURINGSUBSTITUTE;

    public static final String ATTRIBUTE_PSS_CLONECOLORDIVERSITY;

    public static final String ATTRIBUTE_PSS_CLONETECHNICALDIVERSITY;

    public static final String ATTRIBUTE_PSS_KEEPREFERENCEDOCUMENT;

    public static final String INTERFACE_PSS_EQUIPMENT;

    public static final String ATTRIBUTE_PSS_LISTOFASSESSORSELECTEDROLES;

    public final static String INTERFACE_PSS_MANUFACTURING_INSTANCE_EXT;

    public static final String INTERFACE_PSS_MANUFACTURING_ITEMEXT;

    public static final String INTERFACE_PSS_MANUFACTURING_PART_EXT;

    public static final String INTERFACE_PSS_MANUFACTURING_UOMEXT;

    public final static String INTERFACE_PSS_OPERATIONLINEDATA_EXT;
    
    public final static String INTERFACE_OTHER_TP;

    public static final String INTERFACE_PSS_PROCESSCONTINUOUSPROVIDE;

    public static final String INTERFACE_PSS_TOOLING;

    public static final String ATTRIBUTE_PSS_UPDATETIMESTAMP;

    public static final String ATTRIBUTE_PSS_PROGRAMRISKLEVEL;

    public static final String ATTRIBUTE_PSS_ISSUE_DESCRIPTION;

    public static final String ATTRIBUTE_PSS_LADEFAULTVALUE;

    public static final String ATTRIBUTE_SUBJECT_TEXT;

    public static final String ATTRIBUTE_REVIEW_COMMENT_NEEDED;

    public static final String ATTRIBUTE_RELATIONSHIPUUID;

    public static final String ATTRIBUTE_PSS_INWORKCONEWCRTAG;

    public static final String ATTRIBUTE_PSS_SLCCAPEXCONTRIBUTION;

    public static final String ATTRIBUTE_PSS_SLCDEVELOPMENTCONTRIBUTION;

    public static final String ATTRIBUTE_PSS_PLANTCHANGECOORDINATOR;

    public static final String ATTRIBUTE_PSS_CRWORKFLOW;

    public static final String ATTRIBUTE_PSS_CRWORKFLOWCOMMENTS;

    public static final String ATTRIBUTE_PLANT_PDM_CLASS;

    public static final String ATTRIBUTE_PSS_FCSCLASSCATEGORY;

    public static final String ATTRIBUTE_PSS_FCSCLASSDESCRIPTION;

    public static final String ATTRIBUTE_PSS_FCSMATERIALTYPE;

    public static final String ATTRIBUTE_PSS_FCSMATERIALTYPEMAKEORBUYSTATUSFCSANDPDM;

    public static final String ATTRIBUTE_PSS_PDMCLASSDESCRIPTION;

    public static final String ATTRIBUTE_PSS_RELATEDTYPES;

    public static final String ATTRIBUTE_PSS_ACTIONCOMMENTS;

    public static final String ATTRIBUTE_PSS_ASSESSMENT_RISK;

    public static final String ATTRIBUTE_PSS_SLCBOPCOSTS;

    public static final String ATTRIBUTE_PSS_SLCCAPEX;

    public static final String ATTRIBUTE_PSS_SLCCAPEXCUSTOMERCASHCONTRIBUTION;

    public static final String ATTRIBUTE_PSS_SLCCONTRIBUTION;

    public static final String ATTRIBUTE_PSS_SLCDIRECTLABORCOST;

    public static final String ATTRIBUTE_PSS_SLCDNDDPROTOSALES;

    public static final String ATTRIBUTE_PSS_SLCFREIGHTOUTCOSTS;

    public static final String ATTRIBUTE_PSS_SLCIMPACTONPPAPAIDBYCUSTOMERPERPART;

    public static final String ATTRIBUTE_PSS_SLCLAUNCHCOSTS;

    public static final String ATTRIBUTE_PSS_SLCLAUNCHCOSTSCUSTOMERPARTICIPATION;

    public static final String ATTRIBUTE_PSS_SLCPROTOCOSTS;

    public static final String ATTRIBUTE_PSS_SLCSCRAP;

    public static final String ATTRIBUTE_PSS_SLCSUBSIDIES;

    public static final String ATTRIBUTE_PSS_SLCTOOLINGCOSTS;

    public static final String ATTRIBUTE_PSS_SLCTOOLINGSALES;

    public static final String ATTRIBUTE_PSS_SLCRMCOST;
    
    public static final String ATTRIBUTE_PSS_WIDTH;
    
    public static final String ATTRIBUTE_PSS_AllowCreateMBOM;
    
    public static final String ATTRIBUTE_PSS_DIMENSIONINGSTANDARDS1;
    public static final String ATTRIBUTE_PSS_DIMENSIONINGSTANDARDS2;
    public static final String ATTRIBUTE_PSS_DIMENSIONINGSTANDARDS3;
    public static final String ATTRIBUTE_PSS_DIMENSIONINGSTANDARDS4;
    public static final String ATTRIBUTE_PSS_UNDIMENSIONEDRADIUS;
    public static final String ATTRIBUTE_PSS_ANGULARTOLERANCE;
    public static final String ATTRIBUTE_PSS_LINEARTOLERANCE;
    public static final String ATTRIBUTE_PSS_SCALE;
    public static final String ATTRIBUTE_PSS_DRAWINGFORMAT;
    public static final String ATTRIBUTE_PSS_FOLIONUMBER;
    public static final String ATTRIBUTE_PSS_DRAWINGVIEWCONVENTION;
    

    public static final String ATTRIBUTE_ADDRESS;
    public static final String ATTRIBUTE_CITY;
    public static final String ATTRIBUTE_POSTALCODE;
    public static final String ATTRIBUTE_ORGANIZATIONPHONENUMBER;
    public static final String ATTRIBUTE_ORGANIZATIONFAXNUMBER;


    public static final String ATTRIBUTE_PSS_SYMMETRICAL_PARTS_MANAGE_IN_PAIRS;
    public static final String ATTRIBUTE_SAFETY_CLASSIFICATION;
    public static final String ATTRIBUTE_MATERIAL_SAFETY_CLASSIFICATION;
    public static final String ATTRIBUTE_SEMI_MANUFACTURED_PRODUCT_STANDARD;
    public static final String ATTRIBUTE_HEATTREATMENT;
    public static final String ATTRIBUTE_TREATMENT_SAFETY_CLASS;
    public static final String ATTRIBUTE_MCADINTEG_COMMENT;

    //TIGTK-17601,TIGTK-17757
    public static final String ATTRIBUTE_PSS_MANDATORYCR_RANGE_OPTIONAL;
    public static final String ATTRIBUTE_PSS_MANDATORYCR_RANGE_MANDATORY;

    // Interface Constants

    public static final StringList LIST_TYPE_MATERIALS;

    public static final StringList LIST_TYPE_CLONEMBOM;

    // BR072
    public static final String INVALID_AFFECTED_ITEMS;

    public static final String VALID_AFFECTED_ITEMS;

    // Policy Constants

    public static final String POLICY_DEVELOPMENTPART;

    public static final String POLICY_ECRSUPPORTINGDOCUMENT;

    public static final String POLICY_NAMED_EFFECTIVITY;

    public static final String POLICY_PSS_CADOBJECT;
    
    public static final String POLICY_PSS_VEHICLE;

    public static final String POLICY_PSS_CHANGENOTICE;

    public static final String POLICY_CHANGEACTION;

    public static final String POLICY_PSS_CHANGEORDER;

    public static final String POLICY_PSS_CHANGEREQUEST;

    public static final String POLICY_PSS_COLOROPTION;

    public static final String POLICY_DERIVEDOUTPUTTEAMPOLICY;

    public static final String POLICY_PSS_DEVELOPMENTPART;

    public static final String POLICY_PSS_DOCUMENT;

    public static final String POLICY_PSS_DOCUMENTOBSOLETE;

    public static final String POLICY_PSS_ECPART;

    public static final String POLICY_PSS_EQUIPMENT;

    public static final String POLICY_PSS_EQUIPMENTREQUEST;

    public static final String POLICY_PSS_EXTERNALREFERENCE;

    public static final String POLICY_PSS_HARMONYREQUEST;

    public static final String POLICY_PSS_IMPACTANALYSIS;

    public static final String POLICY_PSS_ISSUE;

    public static final String POLICY_PSS_Legacy_CAD;

    public static final String POLICY_PSS_LISTOFASSESSORS;

    public static final String POLICY_PSS_MANUFACTURINGCHANGEACTION;

    public static final String POLICY_PSS_MANUFACTURINGCHANGEORDER;

    public static final String POLICY_PSS_MATERIAL;

    public static final String POLICY_PSS_MATERIAL_REQUEST;

    public static final String POLICY_PSS_MBOM;

    public static final String POLICY_PSS_PORTFOLIO;

    public static final String POLICY_PSS_PROGRAM_PROJECT;

    public static final String POLICY_PSS_ROLEASSESSMENT;

    public static final String POLICY_PSS_ROLEASSESSMENT_EVALUATION;

    public static final String POLICY_PSS_STANDARDMBOM;

    public static final String POLICY_PSS_VARIANTASSEMBLY;

    public static final String POLICY_STANDARDPART;

    public static final String POLICY_UNRESOLVEDPART;

    public static final String POLICY_PSS_TOOL;

    public static final String POLICY_PSS_PUBLISHCONTROLOBJECT;

    public static final String POLICY_PSS_MATERIALASSEMBLY;

    public static final String POLICY_PSS_CANCELPART;

    public static final String POLICY_PSS_CANCELCAD;

    public static final String POLICY_PSS_HARMONY;

    public static final String POLICY_OPERATIONLINE_DATA;

    public static final String POLICY_VERSION;

    public static final String POLICY_VERSIONEDDESIGNPOLICY;

    public static final String POLICY_VERSIONEDDESIGNTEAMPOLICY;
    
    public static final String POLICY_PSS_PDFARCHIVE;

    // Policy Constants

    // Relationship Constants

    public static final String RELATIONSHIP_WORK_FLOW_TASK;

    public static final String RELATIONSHIP_ACTIVEVERSION;

    public static final String RELATIONSHIP_ASSIGNEDISSUE;

    public static final String RELATIONSHIP_ASSOCIATEDDRAWING;

    public static final String RELATIONSHIP_CADSUBCOMPONENT;

    public static final String RELATIONSHIP_CHANGEACTION;

    public static final String RELATIONSHIP_CHANGECOORDINATOR;

    public static final String RELATIONSHIP_CHANGEORDER;

    public static final String RELATIONSHIP_PSS_CLONEDFROMISSUE;

    public static final String RELATIONSHIP_CONFIGURATION_FEATURE;

    public static final String RELATIONSHIP_CONFIGURATION_OPTION;

    public static final String RELATIONSHIP_DELFMIFUNCTIONIDENTIFIEDINSTANCE;

    public static final String RELATIONSHIP_DERIVEDOUTPUT;

    public static final String RELATIONSHIP_EBOM;

    public static final String RELATIONSHIP_EFFECTIVITYUSAGE;

    public static final String RELATIONSHIP_FEATUREPRODUCTCONFIGURATION;

    public static final String RELATIONSHIP_FPDM_CNAFFECTEDITEMS;

    public static final String RELATIONSHIP_FPDM_GENERATEDMBOM;

    public static final String RELATIONSHIP_GBOM;

    public static final String RELATIONSHIP_PSS_GLOBALLOCALPROGRAMPROJECT;

    public static final String RELATIONSHIP_ISSUE;

    public static final String RELATIONSHIP_LATESTVERSION;

    public static final String RELATIONSHIP_MAINPRODUCT;

    public static final String RELATIONSHIP_MANDATORYCONFIGURATIONFEATURES;

    public static final String RELATIONSHIP_NAMED_EFFECTIVITY;

    public static final String RELATIONSHIP_OBJECT_ROUTE;

    public static final String RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS;

    public static final String RELATIONSHIP_PRODUCT_CONFIGURATION;

    public static final String RELATIONSHIP_PSS_AFFECTEDITEM;

    public static final String RELATIONSHIP_PSS_ASSIGNEDENGINE;

    public static final String RELATIONSHIP_PSS_ASSIGNEDOEM;

    public static final String RELATIONSHIP_PSS_ASSIGNEDOEMGROUPTOVEHICLE;

    public static final String RELATIONSHIP_PSS_ASSIGNEDPLATFORM;

    public static final String RELATIONSHIP_PSS_ASSIGNEDPLATFORMTOPRODUCT;

    public static final String RELATIONSHIP_PSS_ASSIGNEDPRODUCT;

    public static final String RELATIONSHIP_PSS_ASSIGNEDVEHICLE;

    public static final String RELATIONSHIP_PSS_ASSOCIATED_PLANT;

    public static final String RELATIONSHIP_PSS_BUSINESSGROUP;
    
    public static final String RELATIONSHIP_PSS_BASISDEFINITION;

    public static final String RELATIONSHIP_PSS_CHARTED_DRAWING;

    public static final String RELATIONSHIP_PSS_CNAFFECTEDITEMS;

    public static final String RELATIONSHIP_PSS_CNSUPPORTINGDOCUMENT;

    public static final String RELATIONSHIP_PSS_COLORCATALOG;

    public static final String RELATIONSHIP_PSS_COLORLIST;

    public static final String RELATIONSHIP_PSS_CONNECTEDENGINE;

    public static final String RELATIONSHIP_PSS_CONNECTEDMEMBERS;

    public static final String RELATIONSHIP_PSS_CONNECTEDOEM;

    public static final String RELATIONSHIP_PSS_CONNECTEDOEMGROUP;

    public static final String RELATIONSHIP_PSS_CONNECTEDPCMDATA;

    public static final String RELATIONSHIP_PSS_CONNECTEDPLANTMEMBERTOPROGPROJ;

    public static final String RELATIONSHIP_PSS_CONNECTEDPLATFORM;

    public static final String RELATIONSHIP_PSS_CONNECTEDPRODUCT;

    public static final String RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES;

    public static final String RELATIONSHIP_PSS_CONNECTEDVEHICLE;

    public static final String RELATIONSHIP_PSS_CONNECTEDWORKSPACE;

    public static final String RELATIONSHIP_PSS_EQUIPMENT_REQUEST;

    public static final String RELATIONSHIP_PSS_EXTERNALREFERENCE;

    public static final String RELATIONSHIP_PSS_GOVERNINGISSUE;

    public static final String RELATIONSHIP_PSS_HARMONY_REQUEST;

    public static final String RELATIONSHIP_PSS_HARMONYASSOCIATION;

    public static final String RELATIONSHIP_PSS_HASSYMMETRICALPART;

    public static final String RELATIONSHIP_IMAGEHOLDER;

    public static final String RELATIONSHIP_PSS_IMPACTANALYSIS;

    public static final String RELATIONSHIP_PSS_ITEMASSIGNEE;

    public static final String RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION;

    public static final String RELATIONSHIP_PSS_MANUFACTURINGCHANGEAFFECTEDITEM;

    public static final String RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER;

    public static final String RELATIONSHIP_PSS_MANUFACTURINGRELATEDPLANT;

    public static final String RELATIONSHIP_PSS_MATERIAL;
    
    public static final String RELATIONSHIP_PSS_MATERIALMASTERBATCHCOLOROPTIONS;

    public static final String RELATIONSHIP_PSS_PARTTOOL;

    public static final String RELATIONSHIP_PSS_PARTVARIANTASSEMBLY;

    public static final String RELATIONSHIP_PSS_PCASSOCIATEDTOHARMONY;

    public static final String RELATIONSHIP_PSS_PRODUCTIONENTITY;

    public static final String RELATIONSHIP_PSS_RELATED150MBOM;

    public static final String RELATIONSHIP_PSS_RELATEDCN;

    public static final String RELATIONSHIP_PSS_RELATEDMBOM;

    public static final String RELATIONSHIP_PSS_RELATEDMATERIALS;

    public static final String RELATIONSHIP_PSS_REQUESTED_EQUIPMENT;

    public static final String RELATIONSHIP_PSS_RESPONSIBLEDIVISION;

    public static final String RELATIONSHIP_PSS_ROLEASSESSMENT;

    public static final String RELATIONSHIP_PSS_ROLEASSESSMENT_EVALUATION;

    public static final String RELATIONSHIP_PSS_SPAREMANUFACTURINGITEM;

    public static final String RELATIONSHIP_PSS_SUBPROGRAMPROJECT;

    public static final String RELATIONSHIP_PSS_SUPPORTINGDOCUMENT;

    public static final String RELATIONSHIP_PSS_VARIANT_ASSEMBLY_PRODUCT_CONFIGURATION;

    public static final String RELATIONSHIP_PSS_VED;

    public static final String RELATIONSHIP_PSSCONNECT_HARMONY_REQUEST;

    public static final String RELATIONSHIP_PSSCONNECTED_HARMONY;

    public static final String RELATIONSHIP_PSSMBOM_HARMONIES;

    public static final String RELATIONSHIP_REQUESTED_HARMONY;

    public static final String RELATIONSHIP_ROUTE_TASK;

    public static final String RELATIONSHIP_TECHNICALASSIGNEE;

    public static final String RELATIONSHIP_VIEWABLE;

    public static final String RELATIONSHIP_VERSIONOF;

    public static final String RELATIONSHIP_VOWNER;

    public static final String RELATIONSHIP_PSS_RELATED_CR;

    public static final String RELATIONSHIP_DERIVED;

    public static final String RELATIONSHIP_PSS_REFERENCE_EBOM;

    public static final String RELATIONSHIP_PSS_DERIVEDCAD;

    public static final String RELATIONSHIP_PSS_PORTFOLIO;

    public static final String RELATIONSHIP_CLASSIFIEDITEM;

    public static final String RELATIONSHIP_PSS_PUBLISHCONTROLOBJECT;

    public static final String RELATIONSHIP_PSS_DERIVED_WORKSPACE_TEMPLATE;

    public static final String RELATIONSHIP_SELECTEDOPTIONS;

    public static final String RELATIONSHIP_PSS_CONNECTEDASSESSORS;

    public static final String RELATIONSHIP_PSS_CONNECTEDIMPACTEDPROJECT;

    public static final String RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT;

    public static final String RELATIONSHIP_PSS_PREREQUISITECR;

    public static final String RELATIONSHIP_PSS_CLONEDFROMCR;

    public static final String RELATIONSHIP_WORKFLOW_TASK_ASSIGNEE;

    public static final String RELATIONSHIP_ASSOCIATED_DRAWING;
    
    public static final String RELATIONSHIP_PSS_ARCHIVEDO;
    
    public static final String RELATIONSHIP_PSS_BASIS_DEFINITION;
    
    public static final String RELATIONSHIP_PSS_STANDARD_COLLABORATIVE_SPACE;
    
    public static final String RELATIONSHIP_EBOM_SUBSTITUTE;
    
    
    
    public static final String ROLE_PSS_CHANGE_COORDINATOR;

    public static final String ROLE_PSS_GLOBAL_ADMINISTRATOR;

    public static final String ROLE_PSS_PLM_SUPPORT_TEAM;

    public static final String ROLE_PSS_PRODUCT_DEVELOPMENT_LEAD;

    public static final String ROLE_PSS_PROGRAM_MANUFACTURING_LEADER;

    public static final String ROLE_PSS_PROGRAM_MANAGER;

    public static final String ROLE_PSS_RAW_MATERIAL_ENGINEER;

    public static final String ROLE_PSS_GTS_ENGINEER;
    // Role Constants

    // Select Constants
    public static final String SELECT_ATTRIBUTE_COOWNER;
    
    public static final String SELECT_ATTRIBUTE_CUSTOMER_DESCRIPTION;

    public static final String SELECT_ATTRIBUTE_CUSTOMER_PARTNUMBER;

    public static final String SELECT_ATTRIBUTE_CUSTOMERPART_NUMBER;

    public static final String SELECT_ATTRIBUTE_DEFAULTPARTPOLICY;

    public static final String SELECT_ATTRIBUTE_DISPLAY_NAME;

    public static final String SELECT_ATTRIBUTE_DISPLAYNAME;

    public static final String SELECT_ATTRIBUTE_EFFECTIVITY_VARIABLE_INDEXES;

    public static final String SELECT_ATTRIBUTE_EFFECTIVITYVARIABLEINDEXES;

    public static final String SELECT_ATTRIBUTE_ENABLECOMPLIANCE;

    public static final String SELECT_ATTRIBUTE_FAURECIA_FULL_LENGTH_DESC;

    public static final String SELECT_ATTRIBUTE_FAURECIA_SHORT_LENGTH_DESC;

    public static final String SELECT_ATTRIBUTE_FILTERCOMPILEDFORM;
    
    public static final String SELECT_ATTRIBUTE_PSS_FILLERNATUREFORMATERIAL;
    
    public static final String SELECT_ATTRIBUTE_PSS_FILLERCONTENTMATERIAL;
    
    public static final String SELECT_ATTRIBUTE_PSS_HEAT_RESISTANCE_LEVEL;
    
    public static final String SELECT_ATTRIBUTE_PSS_IMPACT_HEAT_RESISTANCE_BALANCE;
    
    public static final String SELECT_ATTRIBUTE_PSS_IMPACT_RESISTANCE;

    public static final String SELECT_ATTRIBUTE_MARKETINGNAME;

    public static final String SELECT_ATTRIBUTE_PLMINSTANCE_V_TREEORDER;
    
    public static final String SELECT_ATTRIBUTE_PSS_COLORNAME;

    public static final String SELECT_ATTRIBUTE_PSS_COLOR_PID;

    public static final String SELECT_ATTRIBUTE_PSS_COLORCODE;

    public static final String SELECT_ATTRIBUTE_PSS_COLORID;
    
    public static final String SELECT_ATTRIBUTE_PSS_CROSSLINKING;

    public static final String SELECT_ATTRIBUTE_PSS_GEOMETRYTYPE;
    
    public static final String SELECT_ATTRIBUTE_PSS_GLASSFIBRECONTENTFORMATERIAL;
    
    public static final String SELECT_ATTRIBUTE_PSS_GLOSSFORMATERIAL;
    
    public static final String SELECT_ATTRIBUTE_PSS_LASERETCHING;
    
    public static final String SELECT_ATTRIBUTE_PSS_MASTERBATCHMATRIXNATURE;

    public static final String SELECT_ATTRIBUTE_PSS_MBOM_STRUCTURENODE;
    
    public static final String SELECT_ATTRIBUTE_PSS_MINERALFILLERCONTENT;

    public static final String SELECT_ATTRIBUTE_PSS_OPERATION_HARMONY;

    public static final String SELECT_ATTRIBUTE_PSS_OPERATION_NUMBER;

    public static final String SELECT_ATTRIBUTE_PSS_OPERATION_PSS_COLOR;

    public static final String SELECT_ATTRIBUTE_PSS_OPERATION_PSS_HARMONY;

    public static final String SELECT_ATTRIBUTE_PSS_OPERATION_PSS_OPNCUSTOMER_PART_NUMBER;

    public static final String SELECT_ATTRIBUTE_PSS_OPERATION_PSS_QUANTITY;

    public static final String SELECT_ATTRIBUTE_PSS_PDMCLASS;
    
    public static final String SELECT_ATTRIBUTE_PSS_PLATABLEFORMATERIAL;
    
    public static final String SELECT_ATTRIBUTE_PSS_POLISHABLE;

    public static final String SELECT_ATTRIBUTE_PSS_PRODUCT_CONFIGURATION_PID;

    public static final String SELECT_ATTRIBUTE_PSS_PUBLISHEDEBOM_INSTANCENAME;
    
    public static final String SELECT_ATTRIBUTE_PSS_SCRATCHRESISTANCEMATERIAL;
    
    public static final String SELECT_ATTRIBUTE_PSS_SOFT;
    
    public static final String SELECT_ATTRIBUTE_PSS_STRUCTURENODE;

    public static final String SELECT_ATTRIBUTE_PSS_VARIANTID;

    public static final String SELECT_ATTRIBUTE_PSS_VARIANTOPTIONS;

    public static final String SELECT_ATTRIBUTE_PSS_XMLSTRUCTURE;

    public static final String SELECT_ATTRIBUTE_QUANTITY;

    public static final String SELECT_ATTRIBUTE_SUPPLIERS_PART_NUMBER;
    
    public static final String SELECT_ATTRIBUTE_PSS_TECHNICALDESCRIPTION;
    
    public static final String SELECT_ATTRIBUTE_PSS_TECHNOLOGYFORMATERIAL;

    public static final String SELECT_ATTRIBUTE_TYPE_OF_PART;

    public static final String SELECT_ATTRIBUTE_UNITOF_MEASURE;

    public static final String SELECT_ATTRIBUTE_V_NAME;

    public static final String SELECT_CONFIGURATIONFEATURE_ID_FROM_PRODUCTCONFIGURATION;

    public static final String SELECT_CONFIGURATIONFEATURE_ID_FROM_EFFECTIVITY;

    public static final String SELECT_CONFIGURATIONFEATURE_OPTION_RELID_FROM_EFFECTIVITY;

    public static final String SELECT_PHYSICALID;

    public static final String SELECT_RELATIONSHIP_DELFMIFUNCTIONIDENTIFIEDINSTANCE_TOID;

    public static final String SELECT_RELATIONSHIP_DELFMIFUNCTIONIDENTIFIEDINSTANCE_FROMID;

    public static final String SELECT_RELATIONSHIP_HARMONY_ASSOCIATION;

    public static final String SELECT_RELATIONSHIP_HARMONY_ASSOCIATION_ID;

    public static final String SELECT_RELATIONSHIP_HARMONY_ASSOCIATION_TOID;

    public static final String SELECT_RELATIONSHIP_MAINPRODUCT;

    public static final String SELECT_RELATIONSHIP_MAINPRODUCT_TOID;

    public static final String SELECT_RELATIONSHIP_OBJECT_ROUTE_ID;

    public static final String SELECT_RELATIONSHIP_OBJECT_ROUTE_TYPE;

    public static final String SELECT_RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS_TOID;

    public static final String SELECT_RELATIONSHIP_PSS_COLOR_LIST_TOID;

    public static final String SELECT_REL_FROM_REFERENCE_EBOM_EXISTS;

    public static final String SELECT_REL_REFERENCE_EBOM_FROM_LAST_REVISION_EXISTS;

    public static final String SELECT_STATEMENT;

    public static final String SELECT_ATTRIBUTE_PSS_REPLACED_WITH_LATEST_REVISION;

    public static final String SELECT_ATTRIBUTE_PSS_REFERENCE_EBOM_GENERATED;

    public static final String SELECT_ATTRIBUTE_PSS_CRWORKFLOW;
    
    public static final String SELECT_FROM_DERIVED_OUTPUT_TO_ID;
    public static final String SELECT_FROM_PSS_PDF_ARCHIVE_TO_ID;
    public static final String SELECT_TO_DERIVED_OUTPUT_FROM_ID;
    public static final String SELECT_TO_PSS_PDF_ARCHIVE_FROM_ID;
    // Select Constants

    // State Constants
    public static final String STATE_CAD_APPROVED;

    public static final String STATE_CAD_REVIEW;

    public static final String STATE_MATERIAL_APPROVED;

    public static final String STATE_PART_APPROVED;

    public static final String STATE_PART_REVIEW;

    public static final String STATE_PART_RELEASE;

    public static final String STATE_MBOM_APPROVED;

    public static final String STATE_PSS_MBOM_REVIEW;

    public static final String STATE_CHANGEACTION_CANCELLED;

    public static final String STATE_CHANGEACTION_INWORK;

    public static final String STATE_CN_CANCELLED;

    public static final String STATE_CHANGEACTION_COMPLETE;

    public static final String STATE_DEVELOPMENTPART_COMPLETE;

    public static final String STATE_DEVELOPMENTPART_CREATE;

    public static final String STATE_DEVELOPMENTPART_PEERREVIEW;

    public static final String STATE_ECRSUPPORTINGDOCUMENT_COMPLETE;

    public static final String STATE_EVALUATE;

    public static final String STATE_FULLYINTEGRATED;

    public static final String STATE_IMPACTANALYSIS_CANCELLED;

    public static final String STATE_INWORK_CAD_OBJECT;

    public static final String STATE_CHANGEORDER_IMPLEMENTED;

    public static final String STATE_PSS_CHANGEORDER_INAPPROVAL;

    public static final String STATE_PSS_CHANGEORDER_ONHOLD;

    public static final String STATE_PSS_CHANGEORDER_PREPARE;

    public static final String STATE_PSS_CHANGEORDER_COMPLETE;

    public static final String STATE_PSS_CHANGEORDER_CANCELLED;

    public static final String STATE_PSS_CHANGEORDER_INWORK;

    public static final String STATE_INREVIEW_CN;

    public static final String STATE_PREPARE_CN;

    public static final String STATE_INREVIEW_CR;

    public static final String STATE_SUBMIT_CR;

    public static final String STATE_COMPLETE_CR;

    public static final String STATE_PSS_CR_CREATE;

    public static final String STATE_REJECTED_CR;

    public static final String STATE_INTRANSFER;

    public static final String STATE_NOTFULLYINTEGRATED;

    public static final String STATE_CHANGEACTION_PENDING;

    public static final String STATE_CHANGEACTION_INAPPROVAL;

    public static final String STATE_ECRSUPPORTINGDOCUMENT_PRELIMINARY;

    public static final String STATE_PSS_ECPART_PRELIMINARY;

    public static final String STATE_PSS_CANCELCAD_CANCELLED;

    public static final String STATE_PSS_CANCELPART_CANCELLED;

    public static final String STATE_PSS_CHANGENOTICE_CANCELLED;

    public static final String STATE_PSS_CR_INPROCESS;

    public static final String STATE_PSS_COLOROPTION_INWORK;

    public static final String STATE_PSS_DEVELOPMENTPART_COMPLETE;

    public static final String STATE_PSS_DEVELOPMENTPART_CREATE;

    public static final String STATE_PSS_DEVELOPMENTPART_PEERREVIEW;

    public static final String STATE_PSS_DOCUMENT_CANCELLED;

    public static final String STATE_PSS_DOCUMENTOBSOLETE_CANCELLED;

    public static final String STATE_PSS_EQUIPMENT_INWORK;

    public static final String STATE_PSS_EQUIPMENTREQUEST_CANCELLED;

    public static final String STATE_PSS_EQUIPMENTREQUEST_CREATE;

    public static final String STATE_PSS_EXTERNALREFERENCE_CANCELLED;

    public static final String STATE_PSS_HARMONYREQUEST_CANCELLED;

    public static final String STATE_PSS_HARMONYREQUEST_CREATE;

    public static final String STATE_PSS_IMPACTANALYSIS_COMPLETE;

    public static final String STATE_PSS_ISSUE_ACCEPTED;

    public static final String STATE_PSS_ISSUE_ACTIVE;

    public static final String STATE_PSS_ISSUE_ASSIGN;

    public static final String STATE_PSS_ISSUE_CREATE;

    public static final String STATE_PSS_ISSUE_CLOSED;

    public static final String STATE_PSS_ISSUE_REJECTED;

    public static final String STATE_PSS_ISSUE_REVIEW;

    public static final String STATE_PSS_LEGACY_CAD_INWORK;

    public static final String STATE_PSS_MATERIAL_INWORK;

    public static final String STATE_PSS_MCA_CANCELLED;

    public static final String STATE_PSS_MCA_COMPLETE;

    public static final String STATE_PSS_MCA_INREVIEW;

    public static final String STATE_PSS_MCA_INWORK;

    public static final String STATE_PSS_MCA_PREPARE;

    public static final String STATE_PSS_MATERIAL_CANCELLED;

    public static final String STATE_PSS_MATERIALASSEMBLY_INWORK;

    public static final String STATE_PSS_MATERIALASSEMBLY_CANCELLED;

    public static final String STATE_PSS_MATERIAL_REQUEST_CANCELLED;

    public static final String STATE_PSS_MATERIAL_REQUEST_CREATE;

    public static final String STATE_PSS_MBOM_CANCELLED;

    public static final String STATE_PSS_MBOM_INWORK;

    public static final String STATE_PSS_MCO_CANCELLED;

    public static final String STATE_PSS_MCO_COMPLETE;

    public static final String STATE_PSS_MCO_INWORK;

    public static final String STATE_PSS_MCO_IMPLEMENTED;

    public static final String STATE_PSS_MCO_REJECTED;

    public static final String STATE_PSS_MCO_INREVIEW;

    public static final String STATE_PSS_MCO_PREPARE;

    public static final String STATE_PSS_ROLEASSESSMENT_CANCELLED;

    public static final String STATE_PSS_TOOL_CANCELLED;

    public static final String STATE_PSS_TOOL_INWORK;

    public static final String STATE_PSS_TOOL_REVIEW;

    public static final String STATE_ROLEASSESSMENT_COMPLETE;

    public static final String STATE_ROLEASSESSMENT_CANCELLED;

    public static final String STATE_TRANSFERERROR;

    public static final String STATE_RELEASED_CAD_OBJECT;

    public static final String STATE_ACTIVE;

    public static final String STATE_OBSOLETE;

    public static final String STATE_NONAWARDED;

    public static final String STATE_CHANGEACTION_ONHOLD;

    public static final String STATE_PHASE1;

    public static final String STATE_PHASE2A;

    public static final String STATE_PHASE2B;

    public static final String STATE_PART_OBSOLETE;

    public static final String STATE_PSS_DEVELOPMENTPART_OBSOLETE;

    public static final String STATE_STANDARDPART_OBSOLETE;

    public static final String SELECT_ATTRIBUTE_ALTERNATE_DESCRIPTION;

    public static final String SELECT_ATTRIBUTE_AUTOSTOPONREJECTION;

    public static final String STATE_LA_ACTIVE;

    public static final String STATE_LA_INACTIVE;

    public static final String STATE_PSS_PORTFOLIO_RELEASED;

    public static final String STATE_PSS_PORTFOLIO_OBSOLETE;

    public static final String STATE_PSS_PORTFOLIO_REVIEW;

    public static final String STATE_PSS_HARMONY_EXISTS;

    public static final String STATE_PSS_STANDARD_MBOM_RELEASE;

    public static final String STATE_STANDARDPART_RELEASE;

    public static final String STATE_MBOM_RELEASED;

    public static final String STATE_MBOM_OBSOLETE;
    
    public static final String STATE_OBSOLETE_CAD_OBJECT;
    
    public static final String STATE_VEHICLE_INACTIVE;

    // State Constants

    // Type Constants
    public static final String TYPE_BUSINESSUNIT;

    public static final String TYPE_CHANGEACTION;

    public static final String TYPE_CHANGEORDER;

    public static final String TYPE_CONFIGURATIONFEATURE;

    public static final String TYPE_CONFIGURATIONOPTION;

    public static final String TYPE_CREATEASSEMBLY;

    public static final String TYPE_CREATEKIT;

    public static final String TYPE_CREATEMATERIAL;

    public static final String TYPE_DELFMIFUNCTIONREFERENCE;

    public static final String TYPE_FPDM_MBOMPART;

    public static final String TYPE_GENERAL_CLASS;

    public static final String TYPE_HARDWARE_PRODUCT;

    public static final String TYPE_IEFASSEMBLYFAMILY;

    public static final String TYPE_IEFCOMPONENTFAMILY;

    public static final String TYPE_INBOX_TASK;

    public static final String TYPE_ISSUE;

    public static final String TYPE_MAINPRODUCT;

    public static final String TYPE_MCAD_ASSEMBLY;

    public static final String TYPE_MCAD_COMPONENT;

    public static final String TYPE_MCAD_MODEL;

    public static final String TYPE_MCADDRAWING;

    public static final String TYPE_MCADREPRESENTATION;

    public static final String TYPE_MCO;

    public static final String TYPE_MODEL;

    public static final String TYPE_NAMED_EFFECTIVITY;

    public static final String TYPE_PART;

    public static final String TYPE_PLMCORE_REFERENCE;

    public static final String TYPE_PROCESS_CONTINUOUS_CREATE_MATERIAL;

    public static final String TYPE_PROCESSCONTINUOUSPROVIDE;

    public static final String TYPE_PRODUCTCONFIGURATION;

    public static final String TYPE_PRODUCTS;

    public static final String TYPE_PROEASSEMBLY;

    public static final String TYPE_PROJECT;

    public static final String TYPE_PSS_3D_TEXTIL;

    public static final String TYPE_PSS_ALCANTARA;

    public static final String TYPE_PSS_ALLIED_PLASTICS;

    public static final String TYPE_PSS_ALUMINUM;

    public static final String TYPE_PSS_ARTIFICIAL_LEATHER;

    public static final String TYPE_PSS_ASBG_TPO;

    public static final String TYPE_PSS_ASBG_TPU;

    public static final String TYPE_PSS_BEARING_CAGE;

    public static final String TYPE_PSS_BUSH;

    public static final String TYPE_PSS_CARPET_MATERIAL;

    public static final String TYPE_PSS_CASE_HARDED_STEEL;

    public static final String TYPE_PSS_CASE_HARDENING;

    public static final String TYPE_PSS_CATALOG_MODEL_PIN_EJECTOR;

    public static final String TYPE_PSS_CATALOG_MODEL_PIN_LEADER;

    public static final String TYPE_PSS_CATALOG_MODEL_PUNCH;

    public static final String TYPE_PSS_CHANGENOTICE;

    public static final String TYPE_PSS_CHANGEORDER;

    public static final String TYPE_PSS_CHANGEREQUEST;

    public static final String TYPE_PSS_COLORCATALOG;

    public static final String TYPE_PSS_COLOROPTION;

    public static final String TYPE_PSS_DESTINATIONREGION;

    public static final String TYPE_PSS_DIVISION;

    public static final String TYPE_PSS_DOCUMENT;

    public static final String TYPE_PSS_ENGINE;

    public static final String TYPE_PSS_EQUIPMENT_REQUEST;

    public static final String TYPE_PSS_EXTERNALREFERENCE;

    public static final String TYPE_PSS_HARMONY;

    public static final String TYPE_PSS_HARMONY_REQUEST;

    public static final String TYPE_PSS_IMPACTANALYSIS;

    public static final String TYPE_PSS_ISSUE;

    public static final String TYPE_PSS_LINEDATA;

    public static final String TYPE_PSS_LISTOFASSESSORS;

    public static final String TYPE_PSS_MANUFACTURINGCHANGEACTION;

    public static final String TYPE_PSS_MANUFACTURINGCHANGEORDER;

    public static final String TYPE_PSS_MATERIAL_REQUEST;

    public static final String TYPE_PSS_MBOM_VARIANT_ASSEMBLY;

    public static final String TYPE_PSS_OEM;

    public static final String TYPE_PSS_OEMGROUP;

    public static final String TYPE_PSS_OPERATION;

    public static final String TYPE_PSS_PLANT;

    public static final String TYPE_PSS_PLATFORM;

    public static final String TYPE_PSS_PORTFOLIO;

    public static final String TYPE_PSS_PROGRAMPROJECT;

    public static final String TYPE_PSS_PROEASSEMBLY;

    public static final String TYPE_PSS_PROEASSEMBLYFAMILYTABLE;

    public static final String TYPE_PSS_PROEASSEMBLYINSTANCE;

    public static final String TYPE_PSS_PROEDRAWING;

    public static final String TYPE_PSS_PROEFORMAT;

    public static final String TYPE_PSS_PROEPART;

    public static final String TYPE_PSS_PROEPARTFAMILYTABLE;

    public static final String TYPE_PSS_PROEPARTINSTANCE;

    public static final String TYPE_PSS_RnDCENTER;

    public static final String TYPE_PSS_ROLEASSESSMENT;

    public static final String TYPE_PSS_ROLEASSESSMENT_EVALUATION;

    public static final String TYPE_PSS_VARIANTASSEMBLY;

    public static final String TYPE_PSS_VED;

    public static final String TYPE_PSS_VEHICLE;

    public static final String TYPE_ROUTE;

    public static final String TYPE_ROUTETEMPLATE;

    public static final String TYPE_SERVICEPRODUCT;

    public static final String TYPE_SOFTWAREPRODUCT;

    public static final String TYPE_VPMREFERENCE;

    public static final String TYPE_PSS_PDFARCHIVE;

    public static final String TYPE_PSS_PAINTSYSTEM;

    public static final String TYPE_PSS_PAINTLACK;

    public static final String TYPE_PSS_MATERIALMIXTURE;

    public static final String TYPE_PSS_MATERIAL;

    public static final String TYPE_PSS_COLORMASTERBATCH;

    public static final String TYPE_PSS_PUBLISHCONTROLOBJECT;

    public static final String TYPE_PSS_UGASSEMBLY;

    public static final String TYPE_PSS_UGDRAWING;

    public static final String TYPE_PSS_UGMODEL;

    public static final String TYPE_WORK_FLOW_TASK;
    
    public static final String TYPE_DERIVED_OUTPUT;
    
    public static final String TYPE_PSS_CATDRAWING;
    
    public static final String TYPE_PSS_SWDRAWING;
    
    public static final String TYPE_CADDRAWING;
    public static final String TYPE_UGDRAWING;
    // Type Constants

    // Other Constants
    public static final String ALL;

    public static final String APPROVAL_STATUS;

    public static final String ATTR_RANGE_PSSGEOMETRYTYPE;

    public static final String ATTR_VALUE_UNASSIGNED;

    public static final String DEFAULT_EVALUATION_REVIEW_ROUTE_TEMPLATE_FOR_CR;

    public static final String DEFAULT_IMPACT_ANALYSIS_ROUTE_TEMPLATE_FOR_CR;

    public static final String ENGINEERING_CR;

    public static final String FIELD_CHOICES;

    public static final String FIELD_DISPLAY_CHOICES;

    public static final String FOR_CLONE;

    public static final String FOR_REPLACE;

    public static final String IS_UNIT_OF_MEASURE_PC;

    public static final String MANUFACTURING_CR;

    public static final String PERSON_USER_AGENT;

    public static final String PLM_IMPLEMENTLINK_TARGETREFERENCE3;

    public static final String PROGRAM_CR;

    public static final String PROPERTIES_EMXFRCMBOMCENTRAL;

    public static final String PROPERTIES_EMXFRCMBOMCENTRALSTRINGRESOURCE;

    public static final String PATTERN_MBOM_INSTANCE;

    public static final String ROUTE_BASE_PURPOSE_APPROVAL;

    public static final String ROUTE_REVISION;

    public static final String SYMMETRIC_STATUS_ORIGINAL;

    public static final String SYMMETRIC_STATUS_SYMMETRICAL;

    public static final String TABLE_SETTING_DISABLESELECTION;

    public static final String ATTRIBUTE_PSS_CATYPE_CAD;

    public static final String ATTRIBUTE_PSS_CATYPE_PART;

    public static final String ATTRIBUTE_PSS_CATYPE_STD;

    public static final String VAULT_ESERVICEPRODUCTION;

    public static final String VAULT_ESERVICEADMINISTRATION;

    public static final String VAULT_VPLM;

    public static final String PSS_FOR_REPLACE;

    public static final String PSS_FOR_CLONE;

    public static final String ATTR_RANGE_PSS_MANUFACTURINGPLANTEXTPSS_OWNERSHIP_CONSUMER;

    public static final String ATTR_RANGE_PSS_MANUFACTURINGPLANTEXTPSS_OWNERSHIP_MASTER;

    public static final String RANGE_APPROVAL_LIST_FORCOMMERCIALUPDATEONCO;

    public static final String RANGE_APPROVAL_LIST_FORPROTOTYPEONCO;

    public static final String RANGE_APPROVAL_LIST_FORSERIALLAUNCHONCO;

    public static final String RANGE_APPROVAL_LIST_FORDESIGNSTUDYONCO;

    public static final String RANGE_APPROVAL_LIST_FORAcquisitionONCO;

    public static final String RANGE_APPROVAL_LIST_FOROTHERPARTSONCO;

    public static final String RANGE_APPROVAL_LIST_FORCADONCO;

    public static final String RANGE_APPROVAL_LIST_FORSTANDARDPARTSONCO;

    public static final String RANGE_APPROVAL_LIST_FORCOMMERCIALUPDATEONMCO;

    public static final String RANGE_APPROVAL_LIST_FORPROTOTYPEONMCO;

    public static final String RANGE_APPROVAL_LIST_FORSERIALLAUNCHONMCO;

    public static final String RANGE_APPROVAL_LIST_FORDESIGNSTUDYONMCO;

    public static final String RANGE_APPROVAL_LIST_FOROTHERPARTSONMCO;

    public static final String RANGE_APPROVAL_LIST_FORAcquisitionONMCO;

    public static final String RANGE_OTHER;

    public static final String RANGE_DESIGN_STUDY;

    public static final String RANGE_Acquisition;

    public static final String RANGE_SERIAL_TOOL_LAUNCH_MODIFICATION;

    public static final String RANGE_PROTOTYPE_TOOL_LAUNCH_MODIFICATION;

    public static final String RANGE_COMMERCIAL_UPDATE;

    public static final String ATTRIBUTE_PSS_DECISION_RANGE_NODECISION;

    public static final String ATTRIBUTE_PSS_DECISION_RANGE_NOGO;

    public static final String ATTRIBUTE_PSS_DECISION_RANGE_GO;

    public static final String ATTRIBUTE_PSS_DECISION_RANGE_ABSTAIN;

    public static final String SLC_DEFAULT_VIEW;

    public static final String STR_SUCCESS;

    public static final String LOAD_SLC_DEFAULT_VIEW;

    public static final String SET_SLC_DEFAULT_VIEW;

    public static final String ATTRIBUTE_PSS_CRWORKFLOW_RANGE_BASIC;

    public static final String ATTRIBUTE_PSS_CRWORKFLOW_RANGE_FASTTRACK;

    public static final String ATTRIBUTE_PSS_CRWORKFLOW_RANGE_PARALLELTRACK;
    
    public static final String TITLE_BLOCK_CELL_CROSS_TAG;
    
    public static final String STRING_SINGLE_SPACE;
    public static final String STRING_TRUE;
    public static final String STRING_FALSE;
    public static final String STRING_YES;
    public static final String STRING_NO;
    
    public static final String SEPERATOR_DOT;
    public static final String SEPERATOR_COMMA;
    public static final String SEPERATOR_MINUS;
    public static final String SEPERATOR_FORWARD_SLASH;
    
    public static final String RPE_KEY_SKIP_TB_GENERATION_WEB;
    public static final String RPE_KEY_JOB_SKIP_TB_GENERATION_WEB;
    public static final String RPE_KEY_SKIP_TB_GENERATION_NATIVE;
    public static final String RPE_KEY_SKIP_ON_BG_JOB;
    
    public static final String OPERATOR_AND;
    public static final String OPERATOR_OR;
    public static final String OPERATOR_EQ;
    public static final String OPERATOR_NE;
    public static final String OPERATOR_GT;
    public static final String OPERATOR_LT;
    public static final String OPERATOR_LE;
    public static final String OPERATOR_GE;
    public static final String OPERATOR_SMATCH;
    public static final String OPERATOR_NSMATCH;
    public static final String OPERATOR_MATCH;
    public static final String OPERATOR_NMATCH;
    
    public static final String ATTRIBUTE_PSS_SEMIMANUFACTUREDPRODUCTSTANDARD;
    public static final String ATTRIBUTE_PSS_CAD_FILE_FORMAT;

    //TIGTK-17601,TIGTK-17757
    public static final String STRING_PROGRAMPROJECT_TYPE_GOVERNING;
    public static final String STRING_PROGRAMPROJECT_TYPE_IMPACTED;    
    
    // TIGTK-12983 - ssamel : START
    public static final String ATTRIBUTE_PSS_USERTYPE;
    public static final String ATTRIBUTE_PSS_BGTYPE;
    public static final String ATTRIBUTE_PSS_OWNERSHIP;
    
    public static final String ATTRIBUTE_PSS_USERTYPE_RANGE_JV;
    public static final String ATTRIBUTE_PSS_USERTYPE_RANGE_FAURECIA;
    
    public static final String ROLE_PSS_CHANGE_COORDINATOR_JV;
    public static final String ROLE_PSS_PLANT_LAUNCH_TEAM_LEADER_JV;
    public static final String ROLE_PSS_PROGRAM_BUYER_JV;
    public static final String ROLE_PSS_PROGRAM_CONTROLLER_JV;
    public static final String ROLE_PSS_PROGRAM_MANAGER_JV;
    public static final String ROLE_PSS_PROGRAM_MANUFACTURING_LEADER_JV;
    public static final String ROLE_PSS_PROGRAM_QUALITY_LEADER_JV;
    public static final String ROLE_PSS_PROGRAM_SALES_LEADER_JV;
    public static final String ROLE_PSS_PRODUCT_DEVELOPMENT_LEAD_JV;
    public static final String ROLE_PSS_PCANDL_JV;
    public static final String ROLE_PSS_SPDE_AND_DL_JV;
    public static final String ROLE_PSS_CAD_DESIGNER_JV;
    public static final String ROLE_PSS_READ_JV;
    // TIGTK-12983 - ssamel : END
    
    public static final String ATTRIBUTE_PSS_READYFORDESCISION; 
    static {
        Context context = JPOSupport.getContext();
        // Attribute Constants
        ATTRIBUTE_WORK_FLOW_DUE_DATE = PropertyUtil.getSchemaProperty(context, "attribute_DueDate");
        ATTRIBUTE_ABSENCEDELEGATE = PropertyUtil.getSchemaProperty(context, "attribute_AbsenceDelegate");
        ATTRIBUTE_ABSENCESTARTDATE = PropertyUtil.getSchemaProperty(context, "attribute_AbsenceStartDate");
        ATTRIBUTE_ABSENCEENDDATE = PropertyUtil.getSchemaProperty(context, "attribute_AbsenceEndDate");
        ATTRIBUTE_ALTERNATE_DESCRIPTION = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ManufacturingItemExt.PSS_AlternativeDescription");
        ATTRIBUTE_AUTOSTOPONREJECTION = PropertyUtil.getSchemaProperty(context, "attribute_AutoStopOnRejection");
        ATTRIBUTE_ADDRESS1 = PropertyUtil.getSchemaProperty(context, "attribute_Address1");
        ATTRIBUTE_APPROVAL_STATUS = PropertyUtil.getSchemaProperty(context, "attribute_ApprovalStatus");
        ATTRIBUTE_BRANCH_TO = PropertyUtil.getSchemaProperty(context, "attribute_BranchTo");
        ATTRIBUTE_COMPONENTLOCATION = PropertyUtil.getSchemaProperty(context, "attribute_ComponentLocation");
        ATTRIBUTE_CUSTOMER_DESCRIPTION = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ManufacturingItemExt.PSS_CustomerDescription");
        ATTRIBUTE_COOWNER = PropertyUtil.getSchemaProperty(context, "attribute_CoOwner");
        ATTRIBUTE_DEFAULTPARTPOLICY = PropertyUtil.getSchemaProperty(context, "attribute_DefaultPartPolicy");
        ATTRIBUTE_DISPLAY_NAME = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Equipment.PSS_DisplayName");
        ATTRIBUTE_DISPLAYNAME = PropertyUtil.getSchemaProperty(context, "attribute_DisplayName");
        ATTRIBUTE_EFFECTIVITY_VARIABLE_INDEXES = PropertyUtil.getSchemaProperty(context, "attribute_EffectivityVariableIndexes");
        ATTRIBUTE_EFFECTIVITYCOMPILEDFORM = PropertyUtil.getSchemaProperty(context, "attribute_EffectivityCompiledForm");
        ATTRIBUTE_EFFECTIVITYEXPRESSION = PropertyUtil.getSchemaProperty(context, "attribute_EffectivityExpression");
        ATTRIBUTE_EFFECTIVITYEXPRESSIONBINARY = PropertyUtil.getSchemaProperty(context, "attribute_EffectivityExpressionBinary");
        ATTRIBUTE_EFFECTIVITYORDEREDCRITERIA = PropertyUtil.getSchemaProperty(context, "attribute_EffectivityOrderedCriteria");
        ATTRIBUTE_EFFECTIVITYORDEREDCRITERIADICTIONARY = PropertyUtil.getSchemaProperty(context, "attribute_EffectivityOrderedCriteriaDictionary");
        ATTRIBUTE_EFFECTIVITYORDEREDIMPACTINGCRITERIA = PropertyUtil.getSchemaProperty(context, "attribute_EffectivityOrderedImpactingCriteria");
        ATTRIBUTE_EFFECTIVITYPROPOSEDEXPRESSION = PropertyUtil.getSchemaProperty(context, "attribute_EffectivityProposedExpression");
        ATTRIBUTE_EFFECTIVITYTYPES = PropertyUtil.getSchemaProperty(context, "attribute_EffectivityTypes");
        ATTRIBUTE_ENABLECOMPLIANCE = PropertyUtil.getSchemaProperty(context, "attribute_EnableCompliance");
        ATTRIBUTE_EQUIPMENT_TYPE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Equipment.PSS_EquipmentType");
        ATTRIBUTE_FAURECIA_FULL_LENGTH_DESC = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ManufacturingItemExt.PSS_FaureciaFullLengthDescription");
        ATTRIBUTE_FAURECIA_SHORT_LENGTH_DESC = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ManufacturingItemExt.PSS_FaureciaShortLengthDescriptionFCS");
        ATTRIBUTE_FILTERCOMPILEDFORM = PropertyUtil.getSchemaProperty(context, "attribute_FilterCompiledForm");
        ATTRIBUTE_FPDM_RTDEFAULT = PropertyUtil.getSchemaProperty(context, "attribute_FPDM_RTDefault");
        ATTRIBUTE_FPDM_RTPURPOSE = PropertyUtil.getSchemaProperty(context, "attribute_FPDM_RTPurpose");
        ATTRIBUTE_ISVPLMVISIBLE = PropertyUtil.getSchemaProperty(context, "attribute_isVPMVisible");
        ATTRIBUTE_MARKETINGNAME = PropertyUtil.getSchemaProperty(context, "attribute_MarketingName");
        ATTRIBUTE_PSS_MANDATORYDELIVERABLE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_MandatoryDeliverable");
        ATTRIBUTE_ORGANIZATION_PHONE_NUMBER = PropertyUtil.getSchemaProperty(context, "attribute_OrganizationPhoneNumber");
        ATTRIBUTE_PDM_CLASS = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ManufacturingItemExt.PSS_PDMClass");
        ATTRIBUTE_PLM_EXTERNALID = PropertyUtil.getSchemaProperty(context, "attribute_PLMEntity.PLM_ExternalID");
        ATTRIBUTE_PLMENTITY_V_DESCRIPTION = PropertyUtil.getSchemaProperty(context, "attribute_PLMEntity.V_description");
        ATTRIBUTE_PLMINSTANCE_V_TREEORDER = PropertyUtil.getSchemaProperty(context, "attribute_PLMInstance.V_TreeOrder");
        ATTRIBUTE_PSS_ACTUALEFFECTIVITYDATE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Actual_Effectivity_Date");
        ATTRIBUTE_PSS_ACTUALIMPLEMENTATIONDATE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ActualImplementationDate");
        ATTRIBUTE_PSS_ASSESSMENT_PARTCOST = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Assessment_PartCost");
        ATTRIBUTE_PSS_ASSESSMENT_PC_L = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Assessment_PC_L");
        ATTRIBUTE_PSS_ASSESSMENT_PROCESSRISK = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Assessment_ProcessRisk");
        ATTRIBUTE_PSS_ASSESSMENT_PRODUCTRISK = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Assessment_ProductRisk");
        ATTRIBUTE_PSS_MANDATORYCR = PropertyUtil.getSchemaProperty(context, "attribute_PSS_MandatoryCR");
        ATTRIBUTE_PSS_ASSESSMENTDEVTCOST = PropertyUtil.getSchemaProperty(context, "attribute_PSS_AssessmentDevtCost");
        ATTRIBUTE_PSS_ASSESSMENTTOOLINGCOST_GAUGES = PropertyUtil.getSchemaProperty(context, "attribute_PSS_AssessmentToolingCost_Gauges");
        ATTRIBUTE_PSS_BPUPDATEDATE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_BPUpdateDate");
        ATTRIBUTE_PSS_CAPEXCONTRIB_LAUNCHCOST = PropertyUtil.getSchemaProperty(context, "attribute_PSS_CapexContrib_LaunchCost");
        ATTRIBUTE_PSS_CATYPE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_CAType");
        ATTRIBUTE_PSS_CHECKAPPROVEORABSTAIN = PropertyUtil.getSchemaProperty(context, "attribute_PSS_CheckApproveOrAbstain");
        ATTRIBUTE_PSS_CHECKFLAGFORPASSEDTRIGGER = PropertyUtil.getSchemaProperty(context, "attribute_PSS_CheckFlagForPassedTrigger");
        ATTRIBUTE_PSS_CNREASONFORCANCELLATION = PropertyUtil.getSchemaProperty(context, "attribute_PSS_CNReasonForCancellation");
        ATTRIBUTE_PSS_CNTYPE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_CN_Type");
        ATTRIBUTE_PSS_COLORABLE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Colorable");
        ATTRIBUTE_PSS_COLORCODE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ColorCode");
        ATTRIBUTE_PSS_COLORPID = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ColorPID");
        ATTRIBUTE_PSS_CONTROLPLAN = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ControlPlan");
        ATTRIBUTE_PSS_COSTARTDATE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_COStartDate");
        ATTRIBUTE_PSS_COUNTERMEASURE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Countermeasure");
        ATTRIBUTE_PSS_COVIRTUALIMPLEMENTATIONDATE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_COVirtualImplementationDate");
        ATTRIBUTE_PSS_CRBILLABLE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_CRBillable");
        ATTRIBUTE_PSS_CRCUSTOMERAGREEMENTDATE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_CRCustomerAgreementDate");
        ATTRIBUTE_PSS_CRCUSTOMERCHANGENUMBER = PropertyUtil.getSchemaProperty(context, "attribute_PSS_CRCustomerChangeNumber");
        ATTRIBUTE_PSS_CRCUSTOMERINVOLVEMENT = PropertyUtil.getSchemaProperty(context, "attribute_PSS_CRCustomerInvolvement");
        ATTRIBUTE_PSS_CRDATEOFLASTTRANSFERTOCHANGEMANAGER = PropertyUtil.getSchemaProperty(context, "attribute_PSS_CRDateOfLastTransferToChangeManager");
        ATTRIBUTE_PSS_CRFASTTRACK = PropertyUtil.getSchemaProperty(context, "attribute_PSS_CRFastTrack");
        ATTRIBUTE_PSS_CRFASTTRACKCOMMENT = PropertyUtil.getSchemaProperty(context, "attribute_PSS_CRFastTrackComment");
        ATTRIBUTE_PSS_CRORIGINSUBTYPE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_CROriginSubType");
        ATTRIBUTE_PSS_CRORIGINTYPE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_CROriginType");
        ATTRIBUTE_PSS_CRREASONFORCHANGE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_CRReasonForChange");
        ATTRIBUTE_PSS_CRREQUESTEDASSESSMENTENDDATE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_CRRequestedAssessmentEndDate");
        ATTRIBUTE_PSS_CRTARGETCHANGEIMPLEMENTATIONCOMMENT = PropertyUtil.getSchemaProperty(context, "attribute_PSS_CRTargetChangeImplementationComment");
        ATTRIBUTE_PSS_CRTARGETCHANGEIMPLEMENTATIONDATE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_CRTargetChangeImplementationDate");
        ATTRIBUTE_PSS_CRTITLE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_CRTitle");
        ATTRIBUTE_PSS_CRTYPE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_CRType");
        ATTRIBUTE_PSS_CUSTOMEFFECTIVITYEXPRESSION = PropertyUtil.getSchemaProperty(context, "attribute_PSS_CustomEffectivityExpression");
        ATTRIBUTE_PSS_CUSTOMERPARTNUMBER = PropertyUtil.getSchemaProperty(context, "attribute_PSS_CustomerPartNumber");
        ATTRIBUTE_PSS_DECISION = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Decision");
        ATTRIBUTE_PSS_DECISION_CUSTOMERSTATUS = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Decision_CustomerStatus");
        ATTRIBUTE_PSS_DECISION_DATE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Decision_Date");
        ATTRIBUTE_PSS_DECISIONCASHPAYMENT = PropertyUtil.getSchemaProperty(context, "attribute_PSS_DecisionCashPayment");
        ATTRIBUTE_PSS_DESIGNREVIEWDATE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_DesignReviewDate");
        ATTRIBUTE_PSS_DESIGNREVIEWSTATUS = PropertyUtil.getSchemaProperty(context, "attribute_PSS_DesignReviewStatus");
        ATTRIBUTE_PSS_DOMAIN = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Domain");
        ATTRIBUTE_PSS_DUEDATE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_DueDate");
        ATTRIBUTE_PSS_DVP_PVP = PropertyUtil.getSchemaProperty(context, "attribute_PSS_DVP_PVP");
        ATTRIBUTE_PSS_E2ECODURATIONATPREPARESTATE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_E2ECODurationAtPrepareState");
        ATTRIBUTE_PSS_E2ECRDURATIONATSUBMITSTATE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_E2ECRDurationAtSubmitState");
        ATTRIBUTE_PSS_E2EESCALATIONDELAYEDCHGSFREQUENCY = PropertyUtil.getSchemaProperty(context, "attribute_PSS_E2EEscalationDelayedChgsFrequency");
        ATTRIBUTE_PSS_E2EESCALATIONOPENEDCHGSFREQUENCY = PropertyUtil.getSchemaProperty(context, "attribute_PSS_E2EEscalationOpenedChgsFrequency");
        ATTRIBUTE_PSS_E2EMANAGERNAME = PropertyUtil.getSchemaProperty(context, "attribute_PSS_E2EManagerName");
        ATTRIBUTE_PSS_E2EMCODURATIONATPREPARESTATE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_E2EMCODurationAtPrepareState");
        ATTRIBUTE_PSS_E2ENOTIFICATIONDELAYEDCHGSFREQUENCY = PropertyUtil.getSchemaProperty(context, "attribute_PSS_E2ENotificationDelayedChgsFrequency");
        ATTRIBUTE_PSS_E2ENOTIFICATIONOPENEDCHGSFREQUENCY = PropertyUtil.getSchemaProperty(context, "attribute_PSS_E2ENotificationOpenedChgsFrequency");
        ATTRIBUTE_PSS_E2ENOTIFICATIONREMAINDERFREQUENCY = PropertyUtil.getSchemaProperty(context, "attribute_PSS_E2ENotificationRemainderFrequency");
        ATTRIBUTE_PSS_E2EOPENEDCNDURATION = PropertyUtil.getSchemaProperty(context, "attribute_PSS_E2EOpenedCNDuration");
        ATTRIBUTE_PSS_E2EPROJECTNOTIFICATIONACTIVATION = PropertyUtil.getSchemaProperty(context, "attribute_PSS_E2EProjectNotificationActivation");
        ATTRIBUTE_PSS_EFFECTIVITYDATE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Effectivity_Date");
        ATTRIBUTE_PSS_FINALPAYMENTAGREEMENTCONFIRMED = PropertyUtil.getSchemaProperty(context, "attribute_PSS_FinalPaymentAgreementConfirmed");
        ATTRIBUTE_PSS_FINALPAYMENTAGREEMENTCONFIRMED_RANGEE_NO = "No";
        ATTRIBUTE_PSS_FINALPAYMENTAGREEMENTCONFIRMED_RANGEE_YES = "Yes";
        ATTRIBUTE_PSS_FILLERNATUREFORMATERIAL = PropertyUtil.getSchemaProperty(context, "attribute_PSS_FillerNatureMaterial");
        ATTRIBUTE_PSS_FILLERCONTENTMATERIAL = PropertyUtil.getSchemaProperty(context, "attribute_PSS_FillerContentMaterial");
        ATTRIBUTE_PSS_FLAGFORPROMOTECO = PropertyUtil.getSchemaProperty(context, "attribute_PSS_FlagForPromoteCO");
        ATTRIBUTE_PSS_FMEA_D_P_L = PropertyUtil.getSchemaProperty(context, "attribute_PSS_FMEA_D_P_L");
        ATTRIBUTE_PSS_GEOMETRYTYPE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_GeometryType");
        ATTRIBUTE_PSS_HARMONIES_INSTANCE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ManufacturingInstanceExt.PSS_Harmonies");
        ATTRIBUTE_PSS_HARMONIES_REFERENCE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ManufacturingItemExt.PSS_Harmonies");
        ATTRIBUTE_PSS_HEIGHT = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Height");
        ATTRIBUTE_PSS_HEAT_RESISTANCE_LEVEL = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Heat_Resistance_Level");
        ATTRIBUTE_PSS_IDENTIFICATION_NUMBER = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Identification_Number");
        ATTRIBUTE_PSS_IMPACT_RESISTANCE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Impact_Resistance");
        ATTRIBUTE_PSS_IMPACT_HEAT_RESISTANCE_BALANCE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Impact_Heat_Resistance_Balance");
        ATTRIBUTE_PSS_IMPACTED_REGIONS = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Impacted_regions");
        ATTRIBUTE_PSS_IMPACTONACTBP = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ImpactOnActBP");
        ATTRIBUTE_PSS_INTERCHANGEABILITY = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Interchangeability");
        ATTRIBUTE_PSS_INTERCHANGEABILITYPROPAGATIONSTATUS = PropertyUtil.getSchemaProperty(context, "attribute_PSS_InterchangeabilityPropagationStatus");
        ATTRIBUTE_PSS_KCC_KPC = PropertyUtil.getSchemaProperty(context, "attribute_PSS_KCC_KPC");
        ATTRIBUTE_PSS_LINEDATA_NUMBER = PropertyUtil.getSchemaProperty(context, "attribute_PSS_LineData.PSS_LineDataNumber");
        ATTRIBUTE_PSS_LINEDATA_PSS_NUMBEROFCOLORCHANGES = PropertyUtil.getSchemaProperty(context, "attribute_PSS_LineData.PSS_NumberOfColorChanges");
        ATTRIBUTE_PSS_MASTER_RnD_CENTER = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Master_RnD_Center");
        ATTRIBUTE_PSS_MASTERBATCHMATRIXNATURE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_MasterbatchMatrixNature");
        ATTRIBUTE_PSS_MANUFACTURING_INSTANCEEXT_PSS_PHANTOM_LEVEL = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ManufacturingInstanceExt.PSS_PhantomLevel");
        ATTRIBUTE_PSS_MANUFACTURING_INSTANCEEXT_PSS_PHANTOM_PART = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ManufacturingInstanceExt.PSS_PhantomPart");
        ATTRIBUTE_PSS_MANUFACTURINGITEMEXT_PSS_CUSTOMER_PART_NUMBER = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ManufacturingItemExt.PSS_CustomerPartNumber");
        ATTRIBUTE_PSS_MANUFACTURINGITEMEXT_PSS_SPARE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ManufacturingItemExt.PSS_Spare");
        ATTRIBUTE_PSS_MBOM_STRUCTURENODE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ManufacturingItemExt.PSS_StructureNode");
        ATTRIBUTE_PSS_MBOM_FCSINDEX = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ManufacturingItemExt.PSS_FCSIndex");
        ATTRIBUTE_PSS_MCO_START_DATE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_MCO_Start_Date");
        ATTRIBUTE_PSS_MINERALFILLERCONTENT = PropertyUtil.getSchemaProperty(context, "attribute_PSS_MineralFillerContent");
        ATTRIBUTE_PSS_OEMCODE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_OEMCode");
        ATTRIBUTE_PSS_OLD_MATERIAL_NUMBER = PropertyUtil.getSchemaProperty(context, "attribute_PSS_OldMaterialNumber");
        ATTRIBUTE_PSS_OPERATION_BG = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Operation.PSS_BG");
        ATTRIBUTE_PSS_OPERATION_NUMBER = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Operation.PSS_OperationNumber");
        ATTRIBUTE_PSS_OPERATION_PSS_COLOR = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Operation.PSS_Color");
        ATTRIBUTE_PSS_OPERATION_PSS_OPNCUSTOMER_PART_NUMBER = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Operation.PSS_OPNCustomerPartNumber");
        ATTRIBUTE_PSS_OPERATION_PSS_HARMONY = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Operation.PSS_Harmony");
        ATTRIBUTE_PSS_OPERATION_PSS_QUANTITY = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Operation.PSS_Quantity");
        ATTRIBUTE_PSS_OPERATION_PSS_TITLE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Operation.PSS_Title");
        ATTRIBUTE_PSS_OPERATION_PSS_VARIANTNAME = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Operation.PSS_VariantName");
        ATTRIBUTE_PSS_OPERATION_TECHNOLOGY = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Operation.PSS_Technology");
        ATTRIBUTE_PSS_OPERATIONLINEDATA_EXT_PSS_DIRTY = PropertyUtil.getSchemaProperty(context, "attribute_PSS_OperationLineDataExt.PSS_Dirty");
        ATTRIBUTE_PSS_PARALLELTRACK = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ParallelTrack");
        ATTRIBUTE_PSS_PARALLELTRACKCOMMENT = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ParallelTrackComment");
        ATTRIBUTE_PSS_PARTPRICE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_PartPrice");
        ATTRIBUTE_PSS_PHYSICAL_IMPLEMENTATION_PLANNED_DATE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Physical_Implementation_Planned_Date");
        ATTRIBUTE_PSS_PLANNEDENDDATE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_PlannedEndDate");
        ATTRIBUTE_PSS_PLANTNAME = PropertyUtil.getSchemaProperty(context, "attribute_PSS_PlantName");
        ATTRIBUTE_PSS_PLATABLEFORMATERIAL = PropertyUtil.getSchemaProperty(context, "attribute_PSS_PlatableForMaterial");
        ATTRIBUTE_PSS_PMSITEM = PropertyUtil.getSchemaProperty(context, "attribute_PSS_PMSITEM");
        ATTRIBUTE_PSS_POSITION = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Position");
        ATTRIBUTE_PSS_PPAP_INTERN_OR_CUST_DATE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_PPAP_Intern_Or_Cust_Date");
        ATTRIBUTE_PSS_PPAP_INTERN_OR_CUST_STATUS = PropertyUtil.getSchemaProperty(context, "attribute_PSS_PPAP_Intern_Or_Cust_Status");
        ATTRIBUTE_PSS_PROCESS = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Process");
        ATTRIBUTE_PSS_PROCESSCONTINUOUSPROVIDE_PSS_MATERIALTYPE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ProcessContinuousProvide.PSS_MaterialType");
        ATTRIBUTE_PSS_PRODUCT_CONFIGURATION_PID = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ProductConfigurationPID");
        ATTRIBUTE_PSS_PROGRAMPROJECT = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ProgramProject");
        ATTRIBUTE_PSS_PROGRAMTYPE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ProgramType");
        ATTRIBUTE_PSS_PROJECT_DESCRIPTION = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Project_Description");
        ATTRIBUTE_PSS_PROJECT_PHASE_AT_CREATION = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Project_Phase_at_Creation");
        ATTRIBUTE_PSS_PROJECTPHASE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ProjectPhase");
        ATTRIBUTE_PSS_PUBLISHEDEBOM_INSTANCENAME = PropertyUtil.getSchemaProperty(context, "attribute_PSS_PublishedEBOM.PSS_InstanceName");
        ATTRIBUTE_PSS_PUBLISHEDPART_PSS_CLASSIFICATIONLIST = PropertyUtil.getSchemaProperty(context, "attribute_PSS_PublishedPart.PSS_ClassificationList");
        ATTRIBUTE_PSS_PUBLISHEDPART_PSS_COLORLIST = PropertyUtil.getSchemaProperty(context, "attribute_PSS_PublishedPart.PSS_ColorList");
        ATTRIBUTE_PSS_PUBLISHEDPART_PSS_MATERIALLIST = PropertyUtil.getSchemaProperty(context, "attribute_PSS_PublishedPart.PSS_MaterialList");
        ATTRIBUTE_PSS_PUBLISHEDPART_PSS_TOOLINGLIST = PropertyUtil.getSchemaProperty(context, "attribute_PSS_PublishedPart.PSS_ToolingList");
        ATTRIBUTE_PSS_PUBLISHEDPART_PSS_PARTSPAREPART = PropertyUtil.getSchemaProperty(context, "attribute_PSS_PublishedPart.PSS_PartSparePart");
        ATTRIBUTE_PSS_PUBLISHEDPART_PSS_VARIANTASSEMBLYLIST = PropertyUtil.getSchemaProperty(context, "attribute_PSS_PublishedPart.PSS_VariantAssemblyList");
        ATTRIBUTE_PSS_PURPOSE_OF_RELEASE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Purpose_Of_Release");
        ATTRIBUTE_PSS_RAROLE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_RARole");
        ATTRIBUTE_PSS_REQUESTED_CHANGE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Requested_Change");
        ATTRIBUTE_PSS_ROLE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Role");
        ATTRIBUTE_PSS_ROUTETEMPLATETYPE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_RouteTemplateType");
        ATTRIBUTE_PSS_SALESSTATUS = PropertyUtil.getSchemaProperty(context, "attribute_PSS_SalesStatus");
        ATTRIBUTE_PSS_SAPDESCRIPTION = PropertyUtil.getSchemaProperty(context, "attribute_PSS_SAPDescription");
        ATTRIBUTE_PSS_SAP_RESPONSE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_SAP_Response");
        ATTRIBUTE_PSS_SCRATCHRESISTANCEMATERIAL = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ScratchResistanceMaterial");
        ATTRIBUTE_PSS_SIMPLEREFFECTIVITYEXPRESSION = PropertyUtil.getSchemaProperty(context, "attribute_PSS_SimplerEffectivityExpression");
        ATTRIBUTE_PSS_SLCCOMMENTS = PropertyUtil.getSchemaProperty(context, "attribute_PSS_SLCComments");
        ATTRIBUTE_PSS_STRUCTURENODE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_StructureNode");
        ATTRIBUTE_PSS_SUPPLIER_TEAM_FEASIBILITY_COMMITMENT = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Supplier_Team_Feasibility_Commitment");
        ATTRIBUTE_PSS_SYMMETRICALPARTSIDENTICALMASS = PropertyUtil.getSchemaProperty(context, "attribute_PSS_SymmetricalPartsIdenticalMass");
        ATTRIBUTE_PSS_SYMMETRICALPARTSMANAGEINPAIRS = PropertyUtil.getSchemaProperty(context, "attribute_PSS_SymmetricalPartsManageInPairs");
        ATTRIBUTE_PSS_TECHNICALDESCRIPTION = PropertyUtil.getSchemaProperty(context, "attribute_PSS_TechnicalDescription");
        ATTRIBUTE_PSS_TECHNOLOGY_CLASSIFICATION = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Technology_Classification");
        ATTRIBUTE_PSS_TITLE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Title");
        ATTRIBUTE_PSS_TOOLINGKICKOFFDATE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ToolingKickOffDate");
        ATTRIBUTE_PSS_TOOLINGKICKOFFSTATUS = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ToolingKickOffStatus");
        ATTRIBUTE_PSS_TOOLINGPRICE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ToolingPrice");
        ATTRIBUTE_PSS_TOOLINGREVIEWDATE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ToolingReviewDate");
        ATTRIBUTE_PSS_TOOLINGREVIEWSTATUS = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ToolingReviewStatus");
        ATTRIBUTE_PSS_TRANSFER_TO_SAP_EXPECTED = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Transfer_To_SAP_Expected");
        ATTRIBUTE_PSS_UNIT_OF_MEASURE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ManufacturingUoMExt.PSS_UnitOfMeasure");
        ATTRIBUTE_PSS_UNIT_OF_MEASURE_CATEGORY = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ManufacturingUoMExt.PSS_UnitOfMeasureCategory");
        ATTRIBUTE_PSS_VALIDATION_MVP_R_R = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Validation_MVP_R_R");
        ATTRIBUTE_PSS_VARIANTASSEMBLY_PID = PropertyUtil.getSchemaProperty(context, "attribute_PSS_VariantAssemblyPID");
        ATTRIBUTE_VARIANTDISPLAYTEXT = PropertyUtil.getSchemaProperty(context, "attribute_VariantDisplayText");
        ATTRIBUTE_PSS_VARIANTID = PropertyUtil.getSchemaProperty(context, "attribute_PSS_VariantID");
        ATTRIBUTE_PSS_VARIANTOPTIONS = PropertyUtil.getSchemaProperty(context, "attribute_PSS_VariantOptions");
        ATTRIBUTE_PSS_VIEW = PropertyUtil.getSchemaProperty(context, "attribute_PSS_View");
        ATTRIBUTE_PSS_XMLSTRUCTURE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_XMLStructure");
        ATTRIBUTE_PSSPLANNEDENDDATE = PropertyUtil.getSchemaProperty(context, "attribute_PSSPlannedEndDate");
        ATTRIBUTE_QUANTITY = PropertyUtil.getSchemaProperty(context, "attribute_Quantity");
        ATTRIBUTE_ROUTE_STATUS = PropertyUtil.getSchemaProperty(context, "attribute_RouteStatus");
        ATTRIBUTE_SCHEDULEDCOMPLETIONDATE = PropertyUtil.getSchemaProperty(context, "attribute_ScheduledCompletionDate");
        ATTRIBUTE_SEQUENCEORDER = PropertyUtil.getSchemaProperty(context, "attribute_SequenceOrder");
        ATTRIBUTE_SOURCE = PropertyUtil.getSchemaProperty(context, "attribute_Source");
        ATTRIBUTE_SUPPLIERS_PART_NUMBER = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ManufacturingItemExt.PSS_SupplierPartNumber");
        ATTRIBUTE_TYPE_OF_PART = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ManufacturingInstanceExt.PSS_TypeOfPart");
        ATTRIBUTE_V_EFFECTIVITYCOMPILEDFORM = PropertyUtil.getSchemaProperty(context, "attribute_PLMInstance.V_EffectivityCompiledForm");
        ATTRIBUTE_V_HASCONFIGURATIONEFFECTIVITY = PropertyUtil.getSchemaProperty(context, "attribute_PLMInstance.V_hasConfigEffectivity");
        ATTRIBUTE_V_NAME = PropertyUtil.getSchemaProperty(context, "attribute_PLMEntity.V_Name");
        ATTRIBUTE_WEIGHT = PropertyUtil.getSchemaProperty(context, "attribute_Weight");
        ATTRIBUTE_PSS_TRANSFERFROMCRFLAG = PropertyUtil.getSchemaProperty(context, "attribute_PSS_TransferFromCRFlag");
        ATTRIBUTE_PSS_OTHER_COMMENTS = PropertyUtil.getSchemaProperty(context, "attribute_PSS_OtherComments");
        ATTRIBUTE_DERIVED_CONTEXT = PropertyUtil.getSchemaProperty(context, "attribute_DerivedContext");
        ATTRIBUTE_PSS_REPLACED_WITH_LATEST_REVISION = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ReplacedWithLatestRevision");
        ATTRIBUTE_PSS_REFERENCE_EBOM_GENERATED = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ReferenceEBOMGenerated");
        ATTRIBUTE_REQUESTED_CHANGE = PropertyUtil.getSchemaProperty(context, "attribute_RequestedChange");
        ATTRIBUTE_PSS_TITLEBLOCKCOMPLETED = PropertyUtil.getSchemaProperty(context, "attribute_PSS_TitleBlockCompleted");
        SELECT_ATTRIBUTE_PSS_TITLEBLOCKCOMPLETED = "attribute["+ ATTRIBUTE_PSS_TITLEBLOCKCOMPLETED + "]";
        SELECT_ATTRIBUTE_PSS_TITLEBLOCKCOMPLETED_VALUE = SELECT_ATTRIBUTE_PSS_TITLEBLOCKCOMPLETED + ".value";
        ATTRIBUTE_PSS_DETAIL_SHEET_INFO = PropertyUtil.getSchemaProperty(context, "attribute_PSS_DetailSheetInfo");
        SELECT_ATTRIBUTE_PSS_DETAIL_SHEET_INFO = "attribute["+ ATTRIBUTE_PSS_DETAIL_SHEET_INFO + "]";
        SELECT_ATTRIBUTE_PSS_DETAIL_SHEET_INFO_VALUE = SELECT_ATTRIBUTE_PSS_DETAIL_SHEET_INFO + ".value";
        ATTRIBUTE_PNO_VISIBILITY = PropertyUtil.getSchemaProperty(context, "attribute_PnOVisibility");
        SELECT_ATTRIBUTE_PNO_VISIBILITY = "attribute["+ ATTRIBUTE_PNO_VISIBILITY + "]";
        SELECT_ATTRIBUTE_PNO_VISIBILITY_VALUE = SELECT_ATTRIBUTE_PNO_VISIBILITY + ".value";
        
        ATTRIBUTE_PLMINSTANCE_V_NAME = PropertyUtil.getSchemaProperty(context, "attribute_PLMInstance.V_Name");
        ATTRIBUTE_PLMINSTANCE_V_DESCRIPTION = PropertyUtil.getSchemaProperty(context, "attribute_PLMInstance.V_description");
        ATTRIBUTE_PSS_TRADE_NAME = PropertyUtil.getSchemaProperty(context, "attribute_PSS_TRADE_NAME");
        ATTRIBUTE_PSS_SUPPLIER = PropertyUtil.getSchemaProperty(context, "attribute_PSS_SUPPLIER");
        ATTRIBUTE_PSS_FAURECIASHORTLENGHTDESCRIPTION = PropertyUtil.getSchemaProperty(context, "attribute_PSS_FaureciaShortLenghtDescription");
        ATTRIBUTE_PSS_COLORNAME = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ColorName");
        ATTRIBUTE_PSS_TECHNOLOGYFORMATERIAL = PropertyUtil.getSchemaProperty(context, "attribute_PSS_TechnologyForMaterial");
        ATTRIBUTE_PSS_CROSSLINKING = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Crosslinking");
        ATTRIBUTE_PSS_LASERETCHING = PropertyUtil.getSchemaProperty(context, "attribute_PSS_LaserEtching");
        ATTRIBUTE_PSS_GLASSFIBRECONTENTFORMATERIAL = PropertyUtil.getSchemaProperty(context, "attribute_PSS_GlassFibreContentForMaterial");
        ATTRIBUTE_PSS_GLOSS = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Gloss");
        ATTRIBUTE_PSS_GLOSSFORMATERIAL = PropertyUtil.getSchemaProperty(context, "attribute_PSS_GlossForMaterial");
        ATTRIBUTE_PSS_POLISHABLE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Polishable");
        ATTRIBUTE_PSS_SOFT = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Soft");
        ATTRIBUTE_PSS_GTSTECHNICALFAMILYMATERIAL = PropertyUtil.getSchemaProperty(context, "attribute_PSS_GTSTechnicalFamilyMaterial");
        ATTRIBUTE_PSS_MANUFACTURINGITEMEXTPSS_MAKEORBUY_MATERIAL = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ManufacturingItemExt.PSS_MakeOrBuy_Material");
        ATTRIBUTE_PSS_MANUFACTURINGINSTANCEEXTPSS_PHANTOM = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ManufacturingInstanceExt.PSS_Phantom");
        ATTRIBUTE_PSS_UPDATETIMESTAMP = PropertyUtil.getSchemaProperty(context, "attribute_PSS_UpdateTimeStamp");
        ATTRIBUTE_PSS_AVAILABLE_INSTANCEUPDATE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ManufacturingInstanceExt.PSS_AvailableUpdates");
        ATTRIBUTE_PSS_AVAILABLE_INSTANCEUPDATEFLAG = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ManufacturingInstanceExt.PSS_UpdatesAvailableFlag");
        ATTRIBUTE_PSS_AVAILABLE_UPDATE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ManufacturingItemExt.PSS_AvailableUpdates");
        ATTRIBUTE_PSS_AVAILABLE_UPDATEFLAG = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ManufacturingItemExt.PSS_UpdatesAvailableFlag");
        ATTRIBUTE_PSS_MANUFACTURINGINSTANCEEXTPSS_ALLOWTOLERANCE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ManufacturingInstanceExt.PSS_AllowTolerance");
        ATTRIBUTE_PSS_MANUFACTURINGINSTANCEEXTPSS_EFFECTIVERATIO = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ManufacturingInstanceExt.PSS_EffectiveRatio");
        ATTRIBUTE_PSS_MANUFACTURINGINSTANCEEXTPSS_REFERENCERATIO = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ManufacturingInstanceExt.PSS_ReferenceRatio");
        ATTRIBUTE_PSS_MANUFACTURINGINSTANCEEXTPSS_RATIOTOLERANCE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ManufacturingInstanceExt.PSS_RatioTolerance");
        ATTRIBUTE_PSS_MANUFACTURINGITEMEXTPSS_MATERPLANTNAME = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ManufacturingItemExt.PSS_MasterPlantName");
        ATTRIBUTE_PSS_MANUFACTURINGPLANTEXTPSS_OWNERSHIP = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ManufacturingPlantExt.PSS_Ownership");
        ATTRIBUTE_PSS_PROGRAMRISKLEVEL = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ProgramRiskLevel");
        ATTRIBUTE_FINDNUMBER = PropertyUtil.getSchemaProperty(context, "attribute_FindNumber");
        ATTRIBUTE_REFERENCEDESIGNATOR = PropertyUtil.getSchemaProperty(context, "attribute_ReferenceDesignator");
        ATTRIBUTE_HASMANUFACTURINGSUBSTITUTE = PropertyUtil.getSchemaProperty(context, "attribute_HasManufacturingSubstitute");
        ATTRIBUTE_PSS_ISSUE_DESCRIPTION = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Issue_Description");
        ATTRIBUTE_PSS_LISTOFASSESSORSELECTEDROLES = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ListofAssessorSelectedRoles");
        ATTRIBUTE_PSS_LADEFAULTVALUE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_LADefaultValue");
        ATTRIBUTE_SUBJECT_TEXT = PropertyUtil.getSchemaProperty(context, "attribute_SubjectText");
        ATTRIBUTE_REVIEW_COMMENT_NEEDED = PropertyUtil.getSchemaProperty(context, "attribute_ReviewCommentsNeeded");
        ATTRIBUTE_RELATIONSHIPUUID = PropertyUtil.getSchemaProperty(context, "attribute_RelationshipUUID");
        ATTRIBUTE_PSS_INWORKCONEWCRTAG = PropertyUtil.getSchemaProperty(context, "attribute_PSS_InWorkCONewCRTag");
        ATTRIBUTE_PSS_PUBLISHEDPART_PSS_PARTCOLORABLE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_PublishedPart.PSS_PartColorable");
        ATTRIBUTE_PSS_MANUFACTURING_ITEMEXT_PSS_MAKEORBUYMATERIAL = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ManufacturingItemExt.PSS_MakeOrBuy_Material");
        ATTRIBUTE_PSS_CADMass = PropertyUtil.getSchemaProperty(context, "attribute_PSS_CADMass");
        ATTRIBUTE_PSS_CAD_System_Mass = PropertyUtil.getSchemaProperty(context, "attribute_PSS_CAD_System_Mass");
        ATTRIBUTE_PSS_CAD_Parameter_Mass = PropertyUtil.getSchemaProperty(context, "attribute_PSS_CAD_Parameter_Mass");
        ATTRIBUTE_PSS_EBOM_CADMass = PropertyUtil.getSchemaProperty(context, "attribute_PSS_EBOM_CADMass");
        ATTRIBUTE_PSS_EBOM_Mass1 = PropertyUtil.getSchemaProperty(context, "attribute_PSS_EBOM_Mass1");
        ATTRIBUTE_PSS_EBOM_Mass2 = PropertyUtil.getSchemaProperty(context, "attribute_PSS_EBOM_Mass2");
        ATTRIBUTE_PSS_EBOM_Mass3 = PropertyUtil.getSchemaProperty(context, "attribute_PSS_EBOM_Mass3");
        ATTRIBUTE_PSS_PUBLISHEDPARTPSS_PP_CADMASS = PropertyUtil.getSchemaProperty(context, "attribute_PSS_PublishedPart.PSS_PP_CADMass");
        ATTRIBUTE_PSS_MANUFACTURINGITEMEXTPSS_GROSSWEIGHT = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ManufacturingItemExt.PSS_GrossWeight");
        ATTRIBUTE_PSS_MANUFACTURINGITEMEXTPSS_NETWEIGHT = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ManufacturingItemExt.PSS_NetWeight");
        ATTRIBUTE_PSS_GROSSWEIGHT = PropertyUtil.getSchemaProperty(context, "attribute_PSS_GrossWeight");
        ATTRIBUTE_PSS_NETWEIGHT = PropertyUtil.getSchemaProperty(context, "attribute_PSS_NetWeight");
        ATTRIBUTE_PSS_PURPOSEOFRELEASE_DEVPART = PropertyUtil.getSchemaProperty(context, "attribute_PSS_PurposeOfRelease_DEVPart");
        ATTRIBUTE_PSS_PLANTCHANGECOORDINATOR = PropertyUtil.getSchemaProperty(context, "attribute_PSS_PlantChangeCoordinator");
        ATTRIBUTE_PSS_CRWORKFLOW = PropertyUtil.getSchemaProperty(context, "attribute_PSS_CRWorkFlow");
        ATTRIBUTE_PSS_CRWORKFLOWCOMMENTS = PropertyUtil.getSchemaProperty(context, "attribute_PSS_CRWorkFlowComments");

        ATTRIBUTE_PSS_CLONECOLORDIVERSITY = PropertyUtil.getSchemaProperty(context, "attribute_PSS_CloneColorDiversity");
        ATTRIBUTE_PSS_CLONETECHNICALDIVERSITY = PropertyUtil.getSchemaProperty(context, "attribute_PSS_CloneTechnicalDiversity");
        ATTRIBUTE_PSS_KEEPREFERENCEDOCUMENT = PropertyUtil.getSchemaProperty(context, "attribute_PSS_KeepReferenceDocument");
        ATTRIBUTE_PLANT_PDM_CLASS = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ManufacturingPlantExt.PSS_PDMClass");
        ATTRIBUTE_PSS_FCSCLASSCATEGORY = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ManufacturingPlantExt.PSS_FCSClassCategory");
        ATTRIBUTE_PSS_FCSCLASSDESCRIPTION = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ManufacturingPlantExt.PSS_FCSClassDescription");
        ATTRIBUTE_PSS_FCSMATERIALTYPE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ManufacturingPlantExt.PSS_FCSMaterialType");
        ATTRIBUTE_PSS_FCSMATERIALTYPEMAKEORBUYSTATUSFCSANDPDM = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ManufacturingPlantExt.PSS_FCSMaterialTypeMakeOrBuyStatusFCSandPDM");
        ATTRIBUTE_PSS_PDMCLASSDESCRIPTION = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ManufacturingPlantExt.PSS_PDMClassDescription");
        ATTRIBUTE_PSS_RELATEDTYPES = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ManufacturingPlantExt.PSS_RelatedTypes");
        ATTRIBUTE_PSS_SLCCAPEXCONTRIBUTION = PropertyUtil.getSchemaProperty(context, "attribute_PSS_SLCCapexContribution");
        ATTRIBUTE_PSS_SLCDEVELOPMENTCONTRIBUTION = PropertyUtil.getSchemaProperty(context, "attribute_PSS_SLCDevelopmentContribution");
        ATTRIBUTE_PSS_ACTIONCOMMENTS = PropertyUtil.getSchemaProperty(context, "attribute_PSS_ActionComments");
        ATTRIBUTE_PSS_ASSESSMENT_RISK = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Assessment_Risk");

        ATTRIBUTE_PSS_SLCBOPCOSTS = PropertyUtil.getSchemaProperty(context, "attribute_PSS_SLCBOPCosts");
        ATTRIBUTE_PSS_SLCCAPEX = PropertyUtil.getSchemaProperty(context, "attribute_PSS_SLCCapex");
        ATTRIBUTE_PSS_SLCCAPEXCUSTOMERCASHCONTRIBUTION = PropertyUtil.getSchemaProperty(context, "attribute_PSS_SLCCapexCustomerCashContribution");
        ATTRIBUTE_PSS_SLCCONTRIBUTION = PropertyUtil.getSchemaProperty(context, "attribute_PSS_SLCContribution");
        ATTRIBUTE_PSS_SLCDIRECTLABORCOST = PropertyUtil.getSchemaProperty(context, "attribute_PSS_SLCDirectLaborCost");
        ATTRIBUTE_PSS_SLCDNDDPROTOSALES = PropertyUtil.getSchemaProperty(context, "attribute_PSS_SLCDndDProtoSales");
        ATTRIBUTE_PSS_SLCFREIGHTOUTCOSTS = PropertyUtil.getSchemaProperty(context, "attribute_PSS_SLCFreightOutCosts");
        ATTRIBUTE_PSS_SLCIMPACTONPPAPAIDBYCUSTOMERPERPART = PropertyUtil.getSchemaProperty(context, "attribute_PSS_SLCImpactOnPPAPaidByCustomerPerPart");
        ATTRIBUTE_PSS_SLCLAUNCHCOSTS = PropertyUtil.getSchemaProperty(context, "attribute_PSS_SLCLaunchCosts");
        ATTRIBUTE_PSS_SLCLAUNCHCOSTSCUSTOMERPARTICIPATION = PropertyUtil.getSchemaProperty(context, "attribute_PSS_SLCLaunchCostsCustomerParticipation");
        ATTRIBUTE_PSS_SLCPROTOCOSTS = PropertyUtil.getSchemaProperty(context, "attribute_PSS_SLCProtoCosts");
        ATTRIBUTE_PSS_SLCSCRAP = PropertyUtil.getSchemaProperty(context, "attribute_PSS_SLCScrap");
        ATTRIBUTE_PSS_SLCSUBSIDIES = PropertyUtil.getSchemaProperty(context, "attribute_PSS_SLCSubsidies");
        ATTRIBUTE_PSS_SLCTOOLINGCOSTS = PropertyUtil.getSchemaProperty(context, "attribute_PSS_SLCToolingCosts");
        ATTRIBUTE_PSS_SLCTOOLINGSALES = PropertyUtil.getSchemaProperty(context, "attribute_PSS_SLCToolingSales");
        ATTRIBUTE_PSS_SLCRMCOST = PropertyUtil.getSchemaProperty(context, "attribute_PSS_SLCRMCost");
        ATTRIBUTE_PSS_WIDTH = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Width");
        ATTRIBUTE_PSS_AllowCreateMBOM = PropertyUtil.getSchemaProperty(context, "attribute_PSS_PublishedPart.PSS_AllowCreateMBOM");
        
        //TitleBlock : START
        ATTRIBUTE_PSS_DIMENSIONINGSTANDARDS1 = PropertyUtil.getSchemaProperty(context, "attribute_PSS_DimensioningStandards1");
        ATTRIBUTE_PSS_DIMENSIONINGSTANDARDS2 = PropertyUtil.getSchemaProperty(context, "attribute_PSS_DimensioningStandards2");
        ATTRIBUTE_PSS_DIMENSIONINGSTANDARDS3 = PropertyUtil.getSchemaProperty(context, "attribute_PSS_DimensioningStandards3");
        ATTRIBUTE_PSS_DIMENSIONINGSTANDARDS4 = PropertyUtil.getSchemaProperty(context, "attribute_PSS_DimensioningStandards4");
        ATTRIBUTE_PSS_UNDIMENSIONEDRADIUS = PropertyUtil.getSchemaProperty(context, "attribute_PSS_UndimensionedRadius");
        ATTRIBUTE_PSS_ANGULARTOLERANCE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_AngularTolerance");
        ATTRIBUTE_PSS_LINEARTOLERANCE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_LinearTolerance");
        ATTRIBUTE_PSS_SCALE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Scale");
        ATTRIBUTE_PSS_DRAWINGFORMAT = PropertyUtil.getSchemaProperty(context, "attribute_PSS_DrawingFormat");
        ATTRIBUTE_PSS_FOLIONUMBER = PropertyUtil.getSchemaProperty(context, "attribute_PSS_FolioNumber");
        ATTRIBUTE_PSS_DRAWINGVIEWCONVENTION = PropertyUtil.getSchemaProperty(context, "attribute_PSS_DrawingViewConvention");
        ATTRIBUTE_ADDRESS = PropertyUtil.getSchemaProperty(context, "attribute_Address");
        ATTRIBUTE_CITY = PropertyUtil.getSchemaProperty(context, "attribute_City");
        ATTRIBUTE_POSTALCODE = PropertyUtil.getSchemaProperty(context, "attribute_PostalCode");
        ATTRIBUTE_ORGANIZATIONPHONENUMBER = PropertyUtil.getSchemaProperty(context, "attribute_OrganizationPhoneNumber");
        ATTRIBUTE_ORGANIZATIONFAXNUMBER = PropertyUtil.getSchemaProperty(context, "attribute_OrganizationFaxNumber");

		ATTRIBUTE_PSS_SYMMETRICAL_PARTS_MANAGE_IN_PAIRS = PropertyUtil.getSchemaProperty(context, "attribute_PSS_SymmetricalPartsManageInPairs");
		ATTRIBUTE_SAFETY_CLASSIFICATION = PropertyUtil.getSchemaProperty(context, "attribute_PSS_SAFETY_CLASSIFICATION");
		ATTRIBUTE_MATERIAL_SAFETY_CLASSIFICATION = PropertyUtil.getSchemaProperty(context, "attribute_PSS_MATERIAL_SAFETY_CLASSIFICATION");
		ATTRIBUTE_SEMI_MANUFACTURED_PRODUCT_STANDARD = PropertyUtil.getSchemaProperty(context, "attribute_PSS_SEMI_MANUFACTURED_PRODUCT_STANDARD");
		ATTRIBUTE_HEATTREATMENT = PropertyUtil.getSchemaProperty(context, "attribute_PSS_HEAT_TREATMENT");
		ATTRIBUTE_TREATMENT_SAFETY_CLASS = PropertyUtil.getSchemaProperty(context, "attribute_PSS_TREATMENT_SAFETY_CLASSIFICATION");
        ATTRIBUTE_MCADINTEG_COMMENT = PropertyUtil.getSchemaProperty(context, "attribute_MCADInteg-Comment");
        ATTRIBUTE_PSS_SEMIMANUFACTUREDPRODUCTSTANDARD = PropertyUtil.getSchemaProperty(context, "attribute_PSS_SemiManufacturedProductStandard");
        ATTRIBUTE_PSS_CAD_FILE_FORMAT = PropertyUtil.getSchemaProperty(context, "attribute_PSS_CADFileFormat");

        // Attribute Constants

        // Interface Conastants
        INTERFACE_PSS_EQUIPMENT = PropertyUtil.getSchemaProperty(context, "interface_PSS_Equipment");
        INTERFACE_PSS_MANUFACTURING_INSTANCE_EXT = PropertyUtil.getSchemaProperty(context, "interface_PSS_ManufacturingInstanceExt");
        INTERFACE_PSS_MANUFACTURING_ITEMEXT = PropertyUtil.getSchemaProperty(context, "interface_PSS_ManufacturingItemExt");
        INTERFACE_PSS_MANUFACTURING_PART_EXT = PropertyUtil.getSchemaProperty(context, "interface_PSS_ManufacturingPartExt");
        INTERFACE_PSS_MANUFACTURING_UOMEXT = PropertyUtil.getSchemaProperty(context, "interface_PSS_ManufacturingUoMExt");
        INTERFACE_PSS_OPERATIONLINEDATA_EXT = PropertyUtil.getSchemaProperty(context, "interface_PSS_OperationLineDataExt");
        INTERFACE_OTHER_TP = PropertyUtil.getSchemaProperty(context, "interface_PSS_OperationLineDataExt");
        INTERFACE_PSS_PROCESSCONTINUOUSPROVIDE = PropertyUtil.getSchemaProperty(context, "interface_PSS_ProcessContinuousProvide");
        INTERFACE_PSS_TOOLING = PropertyUtil.getSchemaProperty(context, "interface_PSS_Tooling");
        // Interface Conastants

        // Policy Constants

        POLICY_DEVELOPMENTPART = PropertyUtil.getSchemaProperty(context, "policy_DevelopmentPart");
        POLICY_ECRSUPPORTINGDOCUMENT = PropertyUtil.getSchemaProperty(context, "policy_ECRSupportingDocument");
        POLICY_NAMED_EFFECTIVITY = PropertyUtil.getSchemaProperty(context, "policy_NamedEffectivity");
        POLICY_CHANGEACTION = PropertyUtil.getSchemaProperty(context, "policy_ChangeAction");
        POLICY_PSS_CADOBJECT = PropertyUtil.getSchemaProperty(context, "policy_PSS_CAD_Object");
        POLICY_PSS_VEHICLE = PropertyUtil.getSchemaProperty(context, "policy_PSS_Vehicle");
        POLICY_PSS_CHANGENOTICE = PropertyUtil.getSchemaProperty(context, "policy_PSS_ChangeNotice");
        POLICY_PSS_CHANGEORDER = PropertyUtil.getSchemaProperty(context, "policy_PSS_ChangeOrder");
        POLICY_PSS_CHANGEREQUEST = PropertyUtil.getSchemaProperty(context, "policy_PSS_ChangeRequest");
        POLICY_PSS_COLOROPTION = PropertyUtil.getSchemaProperty(context, "policy_PSS_ColorOption");
        POLICY_DERIVEDOUTPUTTEAMPOLICY = PropertyUtil.getSchemaProperty(context, "policy_DerivedOutputTEAMPolicy");

        POLICY_PSS_DEVELOPMENTPART = PropertyUtil.getSchemaProperty(context, "policy_PSS_Development_Part");
        POLICY_PSS_DOCUMENT = PropertyUtil.getSchemaProperty(context, "policy_PSS_Document");
        POLICY_PSS_DOCUMENTOBSOLETE = PropertyUtil.getSchemaProperty(context, "policy_PSS_DocumentObsolete");
        POLICY_PSS_ECPART = PropertyUtil.getSchemaProperty(context, "policy_PSS_EC_Part");
        POLICY_PSS_EQUIPMENT = PropertyUtil.getSchemaProperty(context, "policy_PSS_Equipment");
        POLICY_PSS_EQUIPMENTREQUEST = PropertyUtil.getSchemaProperty(context, "policy_PSS_EquipmentRequest");
        POLICY_PSS_EXTERNALREFERENCE = PropertyUtil.getSchemaProperty(context, "policy_PSS_ExternalReference");
        POLICY_PSS_HARMONYREQUEST = PropertyUtil.getSchemaProperty(context, "policy_PSS_HarmonyRequest");
        POLICY_PSS_IMPACTANALYSIS = PropertyUtil.getSchemaProperty(context, "policy_PSS_ImpactAnalysis");
        POLICY_PSS_ISSUE = PropertyUtil.getSchemaProperty(context, "policy_PSS_Issue");
        POLICY_PSS_Legacy_CAD = PropertyUtil.getSchemaProperty(context, "policy_PSS_Legacy_CAD");
        POLICY_PSS_MANUFACTURINGCHANGEACTION = PropertyUtil.getSchemaProperty(context, "policy_PSS_ManufacturingChangeAction");
        POLICY_PSS_MANUFACTURINGCHANGEORDER = PropertyUtil.getSchemaProperty(context, "policy_PSS_ManufacturingChangeOrder");
        POLICY_PSS_MATERIAL = PropertyUtil.getSchemaProperty(context, "policy_PSS_Material");
        POLICY_PSS_MATERIAL_REQUEST = PropertyUtil.getSchemaProperty(context, "policy_PSS_Material_Request");
        POLICY_PSS_MBOM = PropertyUtil.getSchemaProperty(context, "policy_PSS_MBOM");
        POLICY_PSS_PORTFOLIO = PropertyUtil.getSchemaProperty(context, "policy_PSS_Portfolio");
        POLICY_PSS_PROGRAM_PROJECT = PropertyUtil.getSchemaProperty(context, "policy_PSS_Program_Project");
        POLICY_PSS_ROLEASSESSMENT = PropertyUtil.getSchemaProperty(context, "policy_PSS_RoleAssessment");
        POLICY_PSS_ROLEASSESSMENT_EVALUATION = PropertyUtil.getSchemaProperty(context, "policy_PSS_RoleAssessmentEvaluation");
        POLICY_PSS_STANDARDMBOM = PropertyUtil.getSchemaProperty(context, "policy_PSS_StandardMBOM");
        POLICY_PSS_VARIANTASSEMBLY = PropertyUtil.getSchemaProperty(context, "policy_PSS_VariantAssembly");
        POLICY_STANDARDPART = PropertyUtil.getSchemaProperty(context, "policy_StandardPart");
        POLICY_UNRESOLVEDPART = PropertyUtil.getSchemaProperty(context, "policy_UnresolvedPart");
        POLICY_PSS_TOOL = PropertyUtil.getSchemaProperty(context, "policy_PSS_Tool");
        POLICY_PSS_PUBLISHCONTROLOBJECT = PropertyUtil.getSchemaProperty(context, "policy_PSS_PublishControlObject");
        POLICY_PSS_MATERIALASSEMBLY = PropertyUtil.getSchemaProperty(context, "policy_PSS_MaterialAssembly");
        POLICY_PSS_CANCELPART = PropertyUtil.getSchemaProperty(context, "policy_PSS_CancelPart");
        POLICY_PSS_CANCELCAD = PropertyUtil.getSchemaProperty(context, "policy_PSS_CancelCAD");
        POLICY_PSS_LISTOFASSESSORS = PropertyUtil.getSchemaProperty(context, "policy_PSS_ListOfAssessors");
        POLICY_PSS_HARMONY = PropertyUtil.getSchemaProperty(context, "policy_PSS_Harmony");
        POLICY_OPERATIONLINE_DATA = PropertyUtil.getSchemaProperty(context, "policy_PSS_OperationLineData");
        POLICY_VERSION = PropertyUtil.getSchemaProperty(context, "policy_Version");
        POLICY_VERSIONEDDESIGNPOLICY = PropertyUtil.getSchemaProperty(context, "policy_VersionedDesignPolicy");
        POLICY_VERSIONEDDESIGNTEAMPOLICY = PropertyUtil.getSchemaProperty(context, "policy_VersionedDesignTEAMPolicy");
        POLICY_PSS_PDFARCHIVE = PropertyUtil.getSchemaProperty(context, "policy_PSS_PDFArchive");
        // Policy Constants

        // Relationship Constants
        RELATIONSHIP_ACTIVEVERSION = PropertyUtil.getSchemaProperty(context, "relationship_ActiveVersion");
        RELATIONSHIP_WORK_FLOW_TASK = PropertyUtil.getSchemaProperty(context, "relationship_WorkflowTask");
        RELATIONSHIP_ASSIGNEDISSUE = PropertyUtil.getSchemaProperty(context, "relationship_AssignedIssue");
        RELATIONSHIP_ASSOCIATEDDRAWING = PropertyUtil.getSchemaProperty(context, "relationship_AssociatedDrawing");
        RELATIONSHIP_CADSUBCOMPONENT = PropertyUtil.getSchemaProperty(context, "relationship_CADSubComponent");
        RELATIONSHIP_CHANGEACTION = PropertyUtil.getSchemaProperty(context, "relationship_ChangeAction");
        RELATIONSHIP_CHANGECOORDINATOR = PropertyUtil.getSchemaProperty(context, "relationship_ChangeCoordinator");
        RELATIONSHIP_CHANGEORDER = PropertyUtil.getSchemaProperty(context, "relationship_ChangeOrder");
        RELATIONSHIP_PSS_CLONEDFROMISSUE = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ClonedFromIssue");
        RELATIONSHIP_CONFIGURATION_FEATURE = PropertyUtil.getSchemaProperty(context, "relationship_ConfigurationFeatures");
        RELATIONSHIP_CONFIGURATION_OPTION = PropertyUtil.getSchemaProperty(context, "relationship_ConfigurationOptions");
        RELATIONSHIP_DELFMIFUNCTIONIDENTIFIEDINSTANCE = PropertyUtil.getSchemaProperty(context, "relationship_DELFmiFunctionIdentifiedInstance");
        RELATIONSHIP_DERIVEDOUTPUT = PropertyUtil.getSchemaProperty(context, "relationship_DerivedOutput");
        RELATIONSHIP_EBOM = PropertyUtil.getSchemaProperty(context, "relationship_EBOM");
        RELATIONSHIP_EFFECTIVITYUSAGE = PropertyUtil.getSchemaProperty(context, "relationship_EffectivityUsage");
        RELATIONSHIP_FEATUREPRODUCTCONFIGURATION = PropertyUtil.getSchemaProperty(context, "relationship_FeatureProductConfiguration");
        RELATIONSHIP_FPDM_CNAFFECTEDITEMS = PropertyUtil.getSchemaProperty(context, "relationship_FPDM_CNAffectedItems");
        RELATIONSHIP_FPDM_GENERATEDMBOM = PropertyUtil.getSchemaProperty(context, "relationship_FPDM_GeneratedMBOM");
        RELATIONSHIP_GBOM = PropertyUtil.getSchemaProperty(context, "relationship_GBOM");
        RELATIONSHIP_PSS_GLOBALLOCALPROGRAMPROJECT = PropertyUtil.getSchemaProperty(context, "relationship_PSS_GlobalLocalProgramProject");
        RELATIONSHIP_ISSUE = PropertyUtil.getSchemaProperty(context, "relationship_Issue");
        RELATIONSHIP_LATESTVERSION = PropertyUtil.getSchemaProperty(context, "relationship_LatestVersion");
        RELATIONSHIP_MAINPRODUCT = PropertyUtil.getSchemaProperty(context, "relationship_MainProduct");
        RELATIONSHIP_MANDATORYCONFIGURATIONFEATURES = PropertyUtil.getSchemaProperty(context, "relationship_MandatoryConfigurationFeatures");
        RELATIONSHIP_NAMED_EFFECTIVITY = PropertyUtil.getSchemaProperty(context, "relationship_NamedEffectivity");
        RELATIONSHIP_OBJECT_ROUTE = PropertyUtil.getSchemaProperty(context, "relationship_ObjectRoute");
        RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS = PropertyUtil.getSchemaProperty(context, "relationship_ProcessInstanceContinuous");
        RELATIONSHIP_PRODUCT_CONFIGURATION = PropertyUtil.getSchemaProperty(context, "relationship_ProductConfiguration");
        RELATIONSHIP_PSS_AFFECTEDITEM = PropertyUtil.getSchemaProperty(context, "relationship_PSS_AffectedItem");
        RELATIONSHIP_PSS_ASSIGNEDENGINE = PropertyUtil.getSchemaProperty(context, "relationship_PSS_AssignedEngine");
        RELATIONSHIP_PSS_ASSIGNEDOEM = PropertyUtil.getSchemaProperty(context, "relationship_PSS_AssignedOEM");
        RELATIONSHIP_PSS_ASSIGNEDOEMGROUPTOVEHICLE = PropertyUtil.getSchemaProperty(context, "relationship_PSS_AssignedOEMGroupToVehicle");
        RELATIONSHIP_PSS_ASSIGNEDPLATFORM = PropertyUtil.getSchemaProperty(context, "relationship_PSS_AssignedPlatform");
        RELATIONSHIP_PSS_ASSIGNEDPLATFORMTOPRODUCT = PropertyUtil.getSchemaProperty(context, "relationship_PSS_AssignedPlatformToProduct");
        RELATIONSHIP_PSS_ASSIGNEDPRODUCT = PropertyUtil.getSchemaProperty(context, "relationship_PSS_AssignedProduct");
        RELATIONSHIP_PSS_ASSIGNEDVEHICLE = PropertyUtil.getSchemaProperty(context, "relationship_PSS_AssignedVehicle");
        RELATIONSHIP_PSS_ASSOCIATED_PLANT = PropertyUtil.getSchemaProperty(context, "relationship_PSS_AssociatedPlant");
        RELATIONSHIP_PSS_BUSINESSGROUP = PropertyUtil.getSchemaProperty(context, "relationship_PSS_BusinessGroup");
        RELATIONSHIP_PSS_BASISDEFINITION = PropertyUtil.getSchemaProperty(context, "relationship_PSS_BasisDefinition");
        RELATIONSHIP_PSS_CHARTED_DRAWING = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ChartedDrawing");
        RELATIONSHIP_PSS_CNAFFECTEDITEMS = PropertyUtil.getSchemaProperty(context, "relationship_PSS_CNAffectedItems");
        RELATIONSHIP_PSS_CNSUPPORTINGDOCUMENT = PropertyUtil.getSchemaProperty(context, "relationship_PSS_CNSupportingDocument");
        RELATIONSHIP_PSS_COLORCATALOG = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ColorCatalog");
        RELATIONSHIP_PSS_COLORLIST = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ColorList");
        RELATIONSHIP_PSS_CONNECTEDENGINE = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ConnectedEngine");
        RELATIONSHIP_PSS_CONNECTEDMEMBERS = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ConnectedMembers");
        RELATIONSHIP_PSS_CONNECTEDOEM = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ConnectedOEM");
        RELATIONSHIP_PSS_CONNECTEDOEMGROUP = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ConnectedOEMGroup");
        RELATIONSHIP_PSS_CONNECTEDPCMDATA = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ConnectedPCMData");
        RELATIONSHIP_PSS_CONNECTEDPLANTMEMBERTOPROGPROJ = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ConnectedPlantMemberToProgProj");
        RELATIONSHIP_PSS_CONNECTEDPLATFORM = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ConnectedPlatform");
        RELATIONSHIP_PSS_CONNECTEDPRODUCT = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ConnectedProduct");
        RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ConnectedRouteTemplates");
        RELATIONSHIP_PSS_CONNECTEDVEHICLE = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ConnectedVehicle");
        RELATIONSHIP_PSS_CONNECTEDWORKSPACE = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ConnectedWorkspace");
        RELATIONSHIP_PSS_EQUIPMENT_REQUEST = PropertyUtil.getSchemaProperty(context, "relationship_PSS_EquipmentRequest");
        RELATIONSHIP_PSS_EXTERNALREFERENCE = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ExternalReference");
        RELATIONSHIP_PSS_GOVERNINGISSUE = PropertyUtil.getSchemaProperty(context, "relationship_PSS_GoverningIssue");
        RELATIONSHIP_PSS_HARMONY_REQUEST = PropertyUtil.getSchemaProperty(context, "relationship_PSS_HarmonyRequest");
        RELATIONSHIP_PSS_HARMONYASSOCIATION = PropertyUtil.getSchemaProperty(context, "relationship_PSS_HarmonyAssociation");
        RELATIONSHIP_PSS_HASSYMMETRICALPART = PropertyUtil.getSchemaProperty(context, "relationship_PSS_HasSymmetricalPart");
        RELATIONSHIP_IMAGEHOLDER = PropertyUtil.getSchemaProperty(context, "relationship_ImageHolder");
        RELATIONSHIP_PSS_IMPACTANALYSIS = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ImpactAnalysis");
        RELATIONSHIP_PSS_ITEMASSIGNEE = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ItemAssignee");
        RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ManufacturingChangeAction");
        RELATIONSHIP_PSS_MANUFACTURINGCHANGEAFFECTEDITEM = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ManufacturingChangeAffectedItem");
        RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ManufacturingChangeOrder");
        RELATIONSHIP_PSS_MANUFACTURINGRELATEDPLANT = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ManufacturingRelatedPlant");
        RELATIONSHIP_PSS_MATERIAL = PropertyUtil.getSchemaProperty(context, "relationship_PSS_Material");
        RELATIONSHIP_PSS_MATERIALMASTERBATCHCOLOROPTIONS = PropertyUtil.getSchemaProperty(context, "relationship_PSS_MaterialMasterBatchColorOptions");
        RELATIONSHIP_PSS_PARTTOOL = PropertyUtil.getSchemaProperty(context, "relationship_PSS_PartTool");
        RELATIONSHIP_PSS_PARTVARIANTASSEMBLY = PropertyUtil.getSchemaProperty(context, "relationship_PSS_PartVariantAssembly");
        RELATIONSHIP_PSS_PCASSOCIATEDTOHARMONY = PropertyUtil.getSchemaProperty(context, "relationship_PSS_PCAssociatedToHarmony");
        RELATIONSHIP_PSS_PRODUCTIONENTITY = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ProductionEntity");
        RELATIONSHIP_PSS_RELATED150MBOM = PropertyUtil.getSchemaProperty(context, "relationship_PSS_Related150MBOM");
        RELATIONSHIP_PSS_RELATEDCN = PropertyUtil.getSchemaProperty(context, "relationship_PSS_RelatedCN");
        RELATIONSHIP_PSS_RELATEDMBOM = PropertyUtil.getSchemaProperty(context, "relationship_PSS_RelatedMBOM");
        RELATIONSHIP_PSS_REQUESTED_EQUIPMENT = PropertyUtil.getSchemaProperty(context, "relationship_PSS_RequestedEquipment");
        RELATIONSHIP_PSS_RESPONSIBLEDIVISION = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ResponsibleDivision");
        RELATIONSHIP_PSS_ROLEASSESSMENT = PropertyUtil.getSchemaProperty(context, "relationship_PSS_RoleAssessment");
        RELATIONSHIP_PSS_ROLEASSESSMENT_EVALUATION = PropertyUtil.getSchemaProperty(context, "relationship_PSS_RoleAssessmentEvaluation");
        RELATIONSHIP_PSS_SPAREMANUFACTURINGITEM = PropertyUtil.getSchemaProperty(context, "relationship_PSS_SpareManufacturingItem");
        RELATIONSHIP_PSS_SUBPROGRAMPROJECT = PropertyUtil.getSchemaProperty(context, "relationship_PSS_SubProgramProject");
        RELATIONSHIP_PSS_SUPPORTINGDOCUMENT = PropertyUtil.getSchemaProperty(context, "relationship_PSS_SupportingDocument");
        RELATIONSHIP_PSS_VARIANT_ASSEMBLY_PRODUCT_CONFIGURATION = PropertyUtil.getSchemaProperty(context, "relationship_PSS_VariantAssemblyProductConfiguration");
        RELATIONSHIP_PSS_VED = PropertyUtil.getSchemaProperty(context, "relationship_PSS_VED");
        RELATIONSHIP_PSSCONNECT_HARMONY_REQUEST = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ConnectedHarmonyRequest");
        RELATIONSHIP_PSSCONNECTED_HARMONY = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ConnectedHarmony");
        RELATIONSHIP_PSSMBOM_HARMONIES = PropertyUtil.getSchemaProperty(context, "relationship_PSS_MBOMHarmonies");
        RELATIONSHIP_REQUESTED_HARMONY = PropertyUtil.getSchemaProperty(context, "relationship_PSS_RequestedHarmony");
        RELATIONSHIP_PSS_RELATEDMATERIALS = PropertyUtil.getSchemaProperty(context, "relationship_PSS_RelatedMaterials");
        RELATIONSHIP_ROUTE_TASK = PropertyUtil.getSchemaProperty(context, "relationship_RouteTask");
        RELATIONSHIP_TECHNICALASSIGNEE = PropertyUtil.getSchemaProperty(context, "relationship_TechnicalAssignee");
        RELATIONSHIP_VIEWABLE = PropertyUtil.getSchemaProperty(context, "relationship_Viewable");
        RELATIONSHIP_VERSIONOF = PropertyUtil.getSchemaProperty(context, "relationship_VersionOf");
        RELATIONSHIP_VOWNER = PropertyUtil.getSchemaProperty(context, "relationship_VPLMrel@PLMConnection@V_Owner");
        RELATIONSHIP_PSS_RELATED_CR = PropertyUtil.getSchemaProperty(context, "relationship_PSS_RelatedCR");
        RELATIONSHIP_DERIVED = PropertyUtil.getSchemaProperty(context, "relationship_Derived");
        RELATIONSHIP_PSS_REFERENCE_EBOM = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ReferenceEBOM");
        RELATIONSHIP_PSS_DERIVEDCAD = PropertyUtil.getSchemaProperty(context, "relationship_PSS_DerivedCAD");
        RELATIONSHIP_PSS_PORTFOLIO = PropertyUtil.getSchemaProperty(context, "relationship_PSS_Portfolio");
        RELATIONSHIP_CLASSIFIEDITEM = PropertyUtil.getSchemaProperty(context, "relationship_ClassifiedItem");
        RELATIONSHIP_PSS_PUBLISHCONTROLOBJECT = PropertyUtil.getSchemaProperty(context, "relationship_PSS_PublishControlObject");
        RELATIONSHIP_PSS_DERIVED_WORKSPACE_TEMPLATE = PropertyUtil.getSchemaProperty(context, "relationship_PSS_DerivedWorkspaceTemplate");
        RELATIONSHIP_SELECTEDOPTIONS = PropertyUtil.getSchemaProperty(context, "relationship_SelectedOptions");
        RELATIONSHIP_PSS_CONNECTEDASSESSORS = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ConnectedAssessors");
        RELATIONSHIP_PSS_CONNECTEDIMPACTEDPROJECT = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ConnectedImpactedProject");
        RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ConnectedGoverningProject");
        RELATIONSHIP_PSS_PREREQUISITECR = PropertyUtil.getSchemaProperty(context, "relationship_PSS_PrerequisiteCR");
        RELATIONSHIP_PSS_CLONEDFROMCR = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ClonedFromCR");
        RELATIONSHIP_WORKFLOW_TASK_ASSIGNEE = PropertyUtil.getSchemaProperty(context, "relationship_WorkflowTaskAssignee");
        RELATIONSHIP_ASSOCIATED_DRAWING = PropertyUtil.getSchemaProperty(context, "relationship_AssociatedDrawing");
        RELATIONSHIP_PSS_ARCHIVEDO = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ArchivedDO");
        RELATIONSHIP_PSS_BASIS_DEFINITION = PropertyUtil.getSchemaProperty(context, "relationship_PSS_BasisDefinition");
        RELATIONSHIP_PSS_STANDARD_COLLABORATIVE_SPACE = PropertyUtil.getSchemaProperty(context, "relationship_PSS_StandardCollaborativeSpace");
        RELATIONSHIP_EBOM_SUBSTITUTE = PropertyUtil.getSchemaProperty(context, "relationship_EBOMSubstitute");
        
        // Role Constants
        ROLE_PSS_CHANGE_COORDINATOR = PropertyUtil.getSchemaProperty(context, "role_PSS_Change_Coordinator");
        ROLE_PSS_GLOBAL_ADMINISTRATOR = PropertyUtil.getSchemaProperty(context, "role_PSS_Global_Administrator");
        ROLE_PSS_PLM_SUPPORT_TEAM = PropertyUtil.getSchemaProperty(context, "role_PSS_PLM_Support_Team");
        ROLE_PSS_PRODUCT_DEVELOPMENT_LEAD = PropertyUtil.getSchemaProperty(context, "role_PSS_Product_Development_Lead");
        ROLE_PSS_PROGRAM_MANUFACTURING_LEADER = PropertyUtil.getSchemaProperty(context, "role_PSS_Program_Manufacturing_Leader");
        ROLE_PSS_PROGRAM_MANAGER = PropertyUtil.getSchemaProperty(context, "role_PSS_Program_Manager");
        ROLE_PSS_RAW_MATERIAL_ENGINEER = PropertyUtil.getSchemaProperty(context, "role_PSS_Raw_Material_Engineer");
        ROLE_PSS_GTS_ENGINEER = PropertyUtil.getSchemaProperty(context, "role_PSS_GTS_Engineer");

        // Role Constants

        // Select Constants
        SELECT_ATTRIBUTE_COOWNER = "attribute[" + ATTRIBUTE_COOWNER + "].value";
        SELECT_ATTRIBUTE_ALTERNATE_DESCRIPTION = "attribute[" + ATTRIBUTE_ALTERNATE_DESCRIPTION + "].value";
        SELECT_ATTRIBUTE_AUTOSTOPONREJECTION = "attribute[" + ATTRIBUTE_AUTOSTOPONREJECTION + "].value";
        SELECT_ATTRIBUTE_CUSTOMER_DESCRIPTION = "attribute[" + ATTRIBUTE_CUSTOMER_DESCRIPTION + "].value";
        SELECT_ATTRIBUTE_CUSTOMER_PARTNUMBER = "attribute[" + ATTRIBUTE_PSS_CUSTOMERPARTNUMBER + "].value";
        SELECT_ATTRIBUTE_CUSTOMERPART_NUMBER = "attribute[" + ATTRIBUTE_PSS_MANUFACTURINGITEMEXT_PSS_CUSTOMER_PART_NUMBER + "]";
        SELECT_ATTRIBUTE_DEFAULTPARTPOLICY = "attribute[" + TigerConstants.ATTRIBUTE_DEFAULTPARTPOLICY + "]";
        SELECT_ATTRIBUTE_DISPLAY_NAME = "attribute[" + ATTRIBUTE_DISPLAYNAME + "]";
        SELECT_ATTRIBUTE_DISPLAYNAME = "attribute[" + ATTRIBUTE_DISPLAYNAME + "]";
        SELECT_ATTRIBUTE_EFFECTIVITY_VARIABLE_INDEXES = "attribute[" + ATTRIBUTE_EFFECTIVITY_VARIABLE_INDEXES + "]";
        SELECT_ATTRIBUTE_EFFECTIVITYVARIABLEINDEXES = "attribute[" + ATTRIBUTE_EFFECTIVITY_VARIABLE_INDEXES + "]";
        SELECT_ATTRIBUTE_ENABLECOMPLIANCE = "attribute[" + TigerConstants.ATTRIBUTE_ENABLECOMPLIANCE + "]";
        SELECT_ATTRIBUTE_FAURECIA_FULL_LENGTH_DESC = "attribute[" + ATTRIBUTE_FAURECIA_FULL_LENGTH_DESC + "].value";
        SELECT_ATTRIBUTE_FAURECIA_SHORT_LENGTH_DESC = "attribute[" + ATTRIBUTE_FAURECIA_SHORT_LENGTH_DESC + "].value";
        SELECT_ATTRIBUTE_FILTERCOMPILEDFORM = "attribute[" + EffectivityFramework.ATTRIBUTE_FILTER_COMPILED_FORM + "]";
        SELECT_ATTRIBUTE_PSS_FILLERNATUREFORMATERIAL = "attribute[" + ATTRIBUTE_PSS_FILLERNATUREFORMATERIAL + "]";
        SELECT_ATTRIBUTE_PSS_FILLERCONTENTMATERIAL = "attribute[" + ATTRIBUTE_PSS_FILLERCONTENTMATERIAL + "]"; 
        SELECT_ATTRIBUTE_PSS_HEAT_RESISTANCE_LEVEL = "attribute[" + ATTRIBUTE_PSS_HEAT_RESISTANCE_LEVEL + "]";
        SELECT_ATTRIBUTE_PSS_IMPACT_HEAT_RESISTANCE_BALANCE = "attribute[" + ATTRIBUTE_PSS_IMPACT_HEAT_RESISTANCE_BALANCE + "]";
        SELECT_ATTRIBUTE_PSS_IMPACT_RESISTANCE = "attribute[" + ATTRIBUTE_PSS_IMPACT_RESISTANCE + "]";
        SELECT_ATTRIBUTE_MARKETINGNAME = "attribute[" + ATTRIBUTE_MARKETINGNAME + "]";
        SELECT_ATTRIBUTE_PLMINSTANCE_V_TREEORDER = "attribute[" + ATTRIBUTE_PLMINSTANCE_V_TREEORDER + "].value";
        SELECT_ATTRIBUTE_PSS_COLORNAME = "attribute[" + ATTRIBUTE_PSS_COLORNAME + "]";
        SELECT_ATTRIBUTE_PSS_COLOR_PID = "attribute[" + ATTRIBUTE_PSS_COLORPID + "]";
        SELECT_ATTRIBUTE_PSS_COLORCODE = "attribute[" + TigerConstants.ATTRIBUTE_PSS_COLORCODE + "]";
        SELECT_ATTRIBUTE_PSS_COLORID = "attribute[" + ATTRIBUTE_PSS_COLORPID + "]";
        SELECT_ATTRIBUTE_PSS_CROSSLINKING = "attribute[" + ATTRIBUTE_PSS_CROSSLINKING + "]";
        SELECT_ATTRIBUTE_PSS_GEOMETRYTYPE = "attribute[" + TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE + "]";
        SELECT_ATTRIBUTE_PSS_GLASSFIBRECONTENTFORMATERIAL = "attribute[" + ATTRIBUTE_PSS_GLASSFIBRECONTENTFORMATERIAL + "]";
        SELECT_ATTRIBUTE_PSS_GLOSSFORMATERIAL = "attribute[" + TigerConstants.ATTRIBUTE_PSS_GLOSSFORMATERIAL + "]";
        SELECT_ATTRIBUTE_PSS_LASERETCHING = "attribute[" + TigerConstants.ATTRIBUTE_PSS_LASERETCHING + "]";
        SELECT_ATTRIBUTE_PSS_MASTERBATCHMATRIXNATURE = "attribute[" + ATTRIBUTE_PSS_MASTERBATCHMATRIXNATURE + "]";
        SELECT_ATTRIBUTE_PSS_MBOM_STRUCTURENODE = "attribute[" + ATTRIBUTE_PSS_MBOM_STRUCTURENODE + "]";
        SELECT_ATTRIBUTE_PSS_MINERALFILLERCONTENT = "attribute[" + ATTRIBUTE_PSS_MINERALFILLERCONTENT + "]";
        SELECT_ATTRIBUTE_PSS_OPERATION_HARMONY = "attribute[" + ATTRIBUTE_PSS_OPERATION_PSS_HARMONY + "]";
        SELECT_ATTRIBUTE_PSS_OPERATION_NUMBER = "attribute[" + ATTRIBUTE_PSS_OPERATION_NUMBER + "]";
        SELECT_ATTRIBUTE_PSS_OPERATION_PSS_COLOR = "attribute[" + ATTRIBUTE_PSS_OPERATION_PSS_COLOR + "]";
        SELECT_ATTRIBUTE_PSS_OPERATION_PSS_HARMONY = "attribute[" + ATTRIBUTE_PSS_OPERATION_PSS_HARMONY + "]";
        SELECT_ATTRIBUTE_PSS_OPERATION_PSS_OPNCUSTOMER_PART_NUMBER = "attribute[" + ATTRIBUTE_PSS_OPERATION_PSS_OPNCUSTOMER_PART_NUMBER + "]";
        SELECT_ATTRIBUTE_PSS_OPERATION_PSS_QUANTITY = "attribute[" + ATTRIBUTE_PSS_OPERATION_PSS_QUANTITY + "]";
        SELECT_ATTRIBUTE_PSS_PDMCLASS = "attribute[" + ATTRIBUTE_PDM_CLASS + "].value";
        SELECT_ATTRIBUTE_PSS_PLATABLEFORMATERIAL = "attribute[" + ATTRIBUTE_PSS_PLATABLEFORMATERIAL + "].value";
        SELECT_ATTRIBUTE_PSS_POLISHABLE = "attribute[" + ATTRIBUTE_PSS_POLISHABLE + "].value";
        SELECT_ATTRIBUTE_PSS_PRODUCT_CONFIGURATION_PID = "attribute[" + ATTRIBUTE_PSS_PRODUCT_CONFIGURATION_PID + "]";
        SELECT_ATTRIBUTE_PSS_PUBLISHEDEBOM_INSTANCENAME = "attribute[" + ATTRIBUTE_PSS_PUBLISHEDEBOM_INSTANCENAME + "]";
        SELECT_ATTRIBUTE_PSS_SCRATCHRESISTANCEMATERIAL = "attribute[" + ATTRIBUTE_PSS_SCRATCHRESISTANCEMATERIAL + "]";
        SELECT_ATTRIBUTE_PSS_SOFT = "attribute[" + ATTRIBUTE_PSS_SOFT + "]";
        SELECT_ATTRIBUTE_PSS_STRUCTURENODE = "attribute[" + ATTRIBUTE_PSS_STRUCTURENODE + "]";
        SELECT_ATTRIBUTE_PSS_VARIANTID = "attribute[" + ATTRIBUTE_PSS_VARIANTID + "]";
        SELECT_ATTRIBUTE_PSS_VARIANTOPTIONS = "attribute[" + ATTRIBUTE_PSS_VARIANTOPTIONS + "]";
        SELECT_ATTRIBUTE_PSS_XMLSTRUCTURE = "attribute[" + ATTRIBUTE_PSS_XMLSTRUCTURE + "]";
        SELECT_ATTRIBUTE_QUANTITY = "attribute[" + ATTRIBUTE_QUANTITY + "]";
        SELECT_ATTRIBUTE_SUPPLIERS_PART_NUMBER = "attribute[" + ATTRIBUTE_SUPPLIERS_PART_NUMBER + "].value";
        SELECT_ATTRIBUTE_PSS_TECHNICALDESCRIPTION = "attribute[" + ATTRIBUTE_PSS_TECHNICALDESCRIPTION + "].value";
        SELECT_ATTRIBUTE_PSS_TECHNOLOGYFORMATERIAL = "attribute[" + ATTRIBUTE_PSS_TECHNOLOGYFORMATERIAL + "].value";
        SELECT_ATTRIBUTE_TYPE_OF_PART = "attribute[" + ATTRIBUTE_TYPE_OF_PART + "].value";
        SELECT_ATTRIBUTE_UNITOF_MEASURE = "attribute[" + ATTRIBUTE_PSS_UNIT_OF_MEASURE + "].value";
        SELECT_ATTRIBUTE_V_NAME = "attribute[" + ATTRIBUTE_V_NAME + "]";
        SELECT_PHYSICALID = "physicalid";
        SELECT_RELATIONSHIP_DELFMIFUNCTIONIDENTIFIEDINSTANCE_TOID = "from[" + RELATIONSHIP_DELFMIFUNCTIONIDENTIFIEDINSTANCE + "].to.id";
        SELECT_RELATIONSHIP_DELFMIFUNCTIONIDENTIFIEDINSTANCE_FROMID = "to[" + RELATIONSHIP_DELFMIFUNCTIONIDENTIFIEDINSTANCE + "].from.id";
        SELECT_RELATIONSHIP_HARMONY_ASSOCIATION = "frommid[" + RELATIONSHIP_PSS_HARMONYASSOCIATION + "].to.id";
        SELECT_RELATIONSHIP_HARMONY_ASSOCIATION_ID = "frommid[" + RELATIONSHIP_PSS_HARMONYASSOCIATION + "].id";
        SELECT_RELATIONSHIP_HARMONY_ASSOCIATION_TOID = "frommid[" + RELATIONSHIP_PSS_HARMONYASSOCIATION + "].to.id";
        SELECT_RELATIONSHIP_MAINPRODUCT = "from[" + RELATIONSHIP_MAINPRODUCT + "].to.id";
        SELECT_RELATIONSHIP_MAINPRODUCT_TOID = "from[" + RELATIONSHIP_MAINPRODUCT + "].to.id";
        SELECT_RELATIONSHIP_OBJECT_ROUTE_ID = "to[" + RELATIONSHIP_OBJECT_ROUTE + "].from.id";
        SELECT_RELATIONSHIP_OBJECT_ROUTE_TYPE = "to[" + RELATIONSHIP_OBJECT_ROUTE + "].from.type";
        SELECT_RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS_TOID = "from[" + RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS + "].to.id";
        SELECT_RELATIONSHIP_PSS_COLOR_LIST_TOID = "from[" + RELATIONSHIP_PSS_COLORLIST + "].to.id";
        SELECT_REL_FROM_REFERENCE_EBOM_EXISTS = "from[" + TigerConstants.RELATIONSHIP_PSS_REFERENCE_EBOM + "]";
        SELECT_REL_REFERENCE_EBOM_FROM_LAST_REVISION_EXISTS = "last.from[" + TigerConstants.RELATIONSHIP_PSS_REFERENCE_EBOM + "]";
        SELECT_STATEMENT = "from[Selected Options].torel[Configuration Options].id";
        SELECT_ATTRIBUTE_PSS_REFERENCE_EBOM_GENERATED = DomainObject.getAttributeSelect(ATTRIBUTE_PSS_REFERENCE_EBOM_GENERATED);
        SELECT_ATTRIBUTE_PSS_REPLACED_WITH_LATEST_REVISION = DomainObject.getAttributeSelect(ATTRIBUTE_PSS_REPLACED_WITH_LATEST_REVISION);
        SELECT_ATTRIBUTE_PSS_CRWORKFLOW = "attribute[" + ATTRIBUTE_PSS_CRWORKFLOW + "].value";
        SELECT_FROM_DERIVED_OUTPUT_TO_ID = "from[" + RELATIONSHIP_DERIVEDOUTPUT + "].to.id";
        SELECT_FROM_PSS_PDF_ARCHIVE_TO_ID = "from[" + RELATIONSHIP_PSS_ARCHIVEDO + "].to.id";
        SELECT_TO_DERIVED_OUTPUT_FROM_ID = "to[" + RELATIONSHIP_DERIVEDOUTPUT + "].from.id";
        SELECT_TO_PSS_PDF_ARCHIVE_FROM_ID = "to[" + RELATIONSHIP_PSS_ARCHIVEDO + "].from.id";
        // Select Constants

        // State constants
        STATE_CAD_APPROVED = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_CADOBJECT, "state_Approved");
        STATE_CAD_REVIEW = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_CADOBJECT, "state_Review");
        STATE_INWORK_CAD_OBJECT = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_CADOBJECT, "state_Preliminary");
        STATE_RELEASED_CAD_OBJECT = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_CADOBJECT, "state_Release");
        STATE_MATERIAL_APPROVED = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_MATERIAL, "state_Approved");

        STATE_PART_OBSOLETE = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_ECPART, "state_Obsolete");
        STATE_PART_APPROVED = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_ECPART, "state_Approved");
        STATE_PART_REVIEW = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_ECPART, "state_Review");
        STATE_PART_RELEASE = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_ECPART, "state_Release");

        STATE_MBOM_APPROVED = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_MBOM, "state_Approved");
        STATE_MBOM_RELEASED = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_MBOM, "state_Released");
        STATE_PSS_MBOM_REVIEW = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_MBOM, "state_Review");
        STATE_PSS_MBOM_CANCELLED = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_MBOM, "state_Cancelled");
        STATE_PSS_MBOM_INWORK = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_MBOM, "state_InWork");

        STATE_CHANGEACTION_CANCELLED = PropertyUtil.getSchemaProperty(context, "policy", POLICY_CHANGEACTION, "state_Cancelled");
        STATE_CHANGEACTION_ONHOLD = PropertyUtil.getSchemaProperty(context, "policy", POLICY_CHANGEACTION, "state_OnHold");

        STATE_CN_CANCELLED = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_CHANGENOTICE, "state_Cancelled");
        STATE_INREVIEW_CN = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_CHANGENOTICE, "state_InReview");
        STATE_EVALUATE = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_CHANGEREQUEST, "state_Evaluate");
        STATE_FULLYINTEGRATED = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_CHANGENOTICE, "state_FullyIntegrated");
        STATE_IMPACTANALYSIS_CANCELLED = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_IMPACTANALYSIS, "state_Cancelled");
        STATE_CHANGEORDER_IMPLEMENTED = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_CHANGEORDER, "state_Implemented");
        STATE_PSS_CHANGEORDER_INAPPROVAL = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_CHANGEORDER, "state_InApproval");
        STATE_PSS_CHANGEORDER_ONHOLD = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_CHANGEORDER, "state_OnHold");

        STATE_SUBMIT_CR = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_CHANGEREQUEST, "state_Submit");
        STATE_INREVIEW_CR = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_CHANGEREQUEST, "state_InReview");
        STATE_COMPLETE_CR = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_CHANGEREQUEST, "state_Complete");
        STATE_PSS_CR_CREATE = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_CHANGEREQUEST, "state_Create");
        STATE_PSS_CR_INPROCESS = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_CHANGEREQUEST, "state_InProcess");
        STATE_REJECTED_CR = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_CHANGEREQUEST, "state_Rejected");

        STATE_INTRANSFER = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_CHANGENOTICE, "state_InTransfer");
        STATE_NOTFULLYINTEGRATED = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_CHANGENOTICE, "state_NotFullyIntegrated");
        STATE_CHANGEACTION_PENDING = PropertyUtil.getSchemaProperty(context, "policy", POLICY_CHANGEACTION, "state_Pending");
        STATE_CHANGEACTION_INAPPROVAL = PropertyUtil.getSchemaProperty(context, "policy", POLICY_CHANGEACTION, "state_InApproval");
        STATE_ECRSUPPORTINGDOCUMENT_PRELIMINARY = PropertyUtil.getSchemaProperty(context, "policy", POLICY_ECRSUPPORTINGDOCUMENT, "state_Preliminary");
        STATE_PSS_ECPART_PRELIMINARY = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_ECPART, "state_Preliminary");

        STATE_PSS_CHANGEORDER_PREPARE = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_CHANGEORDER, "state_Prepare");
        STATE_PREPARE_CN = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_CHANGENOTICE, "state_Prepare");
        STATE_CHANGEACTION_COMPLETE = PropertyUtil.getSchemaProperty(context, "policy", POLICY_CHANGEACTION, "state_Complete");
        STATE_CHANGEACTION_INWORK = PropertyUtil.getSchemaProperty(context, "policy", POLICY_CHANGEACTION, "state_InWork");
        STATE_DEVELOPMENTPART_COMPLETE = PropertyUtil.getSchemaProperty(context, "policy", POLICY_DEVELOPMENTPART, "state_Complete");
        STATE_DEVELOPMENTPART_CREATE = PropertyUtil.getSchemaProperty(context, "policy", POLICY_DEVELOPMENTPART, "state_Create");
        STATE_DEVELOPMENTPART_PEERREVIEW = PropertyUtil.getSchemaProperty(context, "policy", POLICY_DEVELOPMENTPART, "state_PeerReview");
        STATE_PSS_DEVELOPMENTPART_OBSOLETE = PropertyUtil.getSchemaProperty(context, "policy", POLICY_DEVELOPMENTPART, "state_Obsolete");
        STATE_ECRSUPPORTINGDOCUMENT_COMPLETE = PropertyUtil.getSchemaProperty(context, "policy", POLICY_ECRSUPPORTINGDOCUMENT, "state_Complete");
        STATE_PSS_CHANGEORDER_COMPLETE = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_CHANGEORDER, "state_Complete");
        STATE_PSS_CANCELCAD_CANCELLED = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_CANCELCAD, "state_Cancelled");
        STATE_PSS_CANCELPART_CANCELLED = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_CANCELPART, "state_Cancelled");
        STATE_PSS_CHANGENOTICE_CANCELLED = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_CHANGENOTICE, "state_Cancelled");
        STATE_PSS_CHANGEORDER_CANCELLED = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_CHANGEORDER, "state_Cancelled");
        STATE_PSS_CHANGEORDER_INWORK = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_CHANGEORDER, "state_InWork");
        STATE_PSS_COLOROPTION_INWORK = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_COLOROPTION, "state_InWork");
        STATE_PSS_DEVELOPMENTPART_COMPLETE = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_DEVELOPMENTPART, "state_Complete");
        STATE_PSS_DEVELOPMENTPART_CREATE = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_DEVELOPMENTPART, "state_Create");
        STATE_PSS_DEVELOPMENTPART_PEERREVIEW = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_DEVELOPMENTPART, "state_PeerReview");
        STATE_PSS_DOCUMENT_CANCELLED = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_DOCUMENT, "state_Cancelled");
        STATE_PSS_DOCUMENTOBSOLETE_CANCELLED = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_DOCUMENTOBSOLETE, "state_Cancelled");
        STATE_PSS_EQUIPMENT_INWORK = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_EQUIPMENT, "state_InWork");
        STATE_PSS_EQUIPMENTREQUEST_CANCELLED = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_EQUIPMENTREQUEST, "state_Cancelled");
        STATE_PSS_EQUIPMENTREQUEST_CREATE = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_EQUIPMENTREQUEST, "state_Create");
        STATE_PSS_EXTERNALREFERENCE_CANCELLED = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_EXTERNALREFERENCE, "state_Cancelled");
        STATE_PSS_HARMONYREQUEST_CANCELLED = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_HARMONYREQUEST, "state_Cancelled");
        STATE_PSS_HARMONYREQUEST_CREATE = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_HARMONYREQUEST, "state_Create");
        STATE_PSS_IMPACTANALYSIS_COMPLETE = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_IMPACTANALYSIS, "state_Complete");
        STATE_PSS_ISSUE_ACCEPTED = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_ISSUE, "state_Accepted");
        STATE_PSS_ISSUE_ACTIVE = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_ISSUE, "state_Active");
        STATE_PSS_ISSUE_ASSIGN = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_ISSUE, "state_Assign");
        STATE_PSS_ISSUE_CREATE = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_ISSUE, "state_Create");
        STATE_PSS_ISSUE_CLOSED = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_ISSUE, "state_Closed");
        STATE_PSS_ISSUE_REJECTED = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_ISSUE, "state_Rejected");
        STATE_PSS_ISSUE_REVIEW = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_ISSUE, "state_Review");
        STATE_PSS_LEGACY_CAD_INWORK = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_Legacy_CAD, "state_Preliminary");
        STATE_PSS_MATERIAL_INWORK = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_MATERIAL, "state_InWork");
        STATE_PSS_MATERIALASSEMBLY_INWORK = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_MATERIALASSEMBLY, "state_InWork");
        STATE_PSS_MCA_CANCELLED = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_MANUFACTURINGCHANGEACTION, "state_Cancelled");
        STATE_PSS_MCA_COMPLETE = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_MANUFACTURINGCHANGEACTION, "state_Complete");
        STATE_PSS_MCA_INREVIEW = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_MANUFACTURINGCHANGEACTION, "state_InReview");
        STATE_PSS_MCA_INWORK = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_MANUFACTURINGCHANGEACTION, "state_InWork");
        STATE_PSS_MCA_PREPARE = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_MANUFACTURINGCHANGEACTION, "state_Prepare");
        STATE_PSS_MATERIAL_CANCELLED = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_MATERIAL, "state_Cancelled");
        STATE_PSS_MATERIALASSEMBLY_CANCELLED = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_MATERIALASSEMBLY, "state_Cancelled");
        STATE_PSS_MATERIAL_REQUEST_CANCELLED = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_MATERIAL_REQUEST, "state_Cancelled");
        STATE_PSS_MATERIAL_REQUEST_CREATE = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_MATERIAL_REQUEST, "state_Create");
        STATE_PSS_MCO_CANCELLED = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_MANUFACTURINGCHANGEORDER, "state_Cancelled");
        STATE_PSS_MCO_COMPLETE = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_MANUFACTURINGCHANGEORDER, "state_Complete");
        STATE_PSS_MCO_IMPLEMENTED = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_MANUFACTURINGCHANGEORDER, "state_Implemented");
        STATE_PSS_MCO_REJECTED = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_MANUFACTURINGCHANGEORDER, "state_Rejected");
        STATE_PSS_MCO_INREVIEW = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_MANUFACTURINGCHANGEORDER, "state_InReview");
        STATE_PSS_MCO_INWORK = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_MANUFACTURINGCHANGEORDER, "state_InWork");
        STATE_PSS_MCO_PREPARE = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_MANUFACTURINGCHANGEORDER, "state_Prepare");
        STATE_PSS_ROLEASSESSMENT_CANCELLED = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_ROLEASSESSMENT, "state_Cancelled");
        STATE_PSS_TOOL_CANCELLED = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_TOOL, "state_Cancelled");
        STATE_PSS_TOOL_INWORK = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_TOOL, "state_InWork");
        STATE_PSS_TOOL_REVIEW = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_TOOL, "state_Review");
        STATE_PSS_PORTFOLIO_RELEASED = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_PORTFOLIO, "state_Released");
        STATE_PSS_PORTFOLIO_OBSOLETE = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_PORTFOLIO, "state_Obsolete");
        STATE_PSS_PORTFOLIO_REVIEW = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_PORTFOLIO, "state_Review");
        STATE_ROLEASSESSMENT_COMPLETE = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_ROLEASSESSMENT, "state_Complete");
        STATE_ROLEASSESSMENT_CANCELLED = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_ROLEASSESSMENT, "state_Cancelled");
        STATE_PSS_HARMONY_EXISTS = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_HARMONY, "state_Exists");

        STATE_PSS_STANDARD_MBOM_RELEASE = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_STANDARDMBOM, "state_Released");

        STATE_STANDARDPART_RELEASE = PropertyUtil.getSchemaProperty(context, "policy", POLICY_STANDARDPART, "state_Release");
        STATE_TRANSFERERROR = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_CHANGENOTICE, "state_TransferError");

        STATE_ACTIVE = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_PROGRAM_PROJECT, "state_Active");
        STATE_OBSOLETE = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_PROGRAM_PROJECT, "state_Obsolete");
        STATE_NONAWARDED = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_PROGRAM_PROJECT, "state_NonAwarded");
        STATE_PHASE1 = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_PROGRAM_PROJECT, "state_Phase1");
        STATE_PHASE2A = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_PROGRAM_PROJECT, "state_Phase2a");
        STATE_PHASE2B = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_PROGRAM_PROJECT, "state_Phase2b");
        STATE_LA_ACTIVE = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_LISTOFASSESSORS, "state_Active");
        STATE_LA_INACTIVE = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_LISTOFASSESSORS, "state_Inactive");
        STATE_STANDARDPART_OBSOLETE = PropertyUtil.getSchemaProperty(context, "policy", POLICY_STANDARDPART, "state_Obsolete");
        STATE_MBOM_OBSOLETE = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_MBOM, "state_Obsolete");

        STATE_OBSOLETE_CAD_OBJECT = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_CADOBJECT, "state_Obsolete");
        STATE_VEHICLE_INACTIVE = PropertyUtil.getSchemaProperty(context, "policy", POLICY_PSS_VEHICLE, "state_Inactive");
        // State Constants

        // Type Constants
        TYPE_BUSINESSUNIT = PropertyUtil.getSchemaProperty(context, "type_BusinessUnit");
        TYPE_WORK_FLOW_TASK = PropertyUtil.getSchemaProperty(context, "type_WorkflowTask");
        TYPE_CHANGEACTION = PropertyUtil.getSchemaProperty(context, "type_ChangeAction");
        TYPE_CHANGEORDER = PropertyUtil.getSchemaProperty(context, "type_ChangeOrder");
        TYPE_CONFIGURATIONFEATURE = PropertyUtil.getSchemaProperty(context, "type_ConfigurationFeature");
        TYPE_CONFIGURATIONOPTION = PropertyUtil.getSchemaProperty(context, "type_ConfigurationOption");
        TYPE_CREATEASSEMBLY = PropertyUtil.getSchemaProperty(context, "type_CreateAssembly");
        TYPE_CREATEKIT = PropertyUtil.getSchemaProperty(context, "type_CreateKit");
        TYPE_CREATEMATERIAL = PropertyUtil.getSchemaProperty(context, "type_CreateMaterial");
        TYPE_DELFMIFUNCTIONREFERENCE = PropertyUtil.getSchemaProperty(context, "type_DELFmiFunctionReference");
        TYPE_FPDM_MBOMPART = PropertyUtil.getSchemaProperty(context, "type_FPDM_MBOMPart");
        TYPE_GENERAL_CLASS = PropertyUtil.getSchemaProperty(context, "type_GeneralClass");
        TYPE_HARDWARE_PRODUCT = PropertyUtil.getSchemaProperty(context, "type_HardwareProduct");
        TYPE_IEFASSEMBLYFAMILY = PropertyUtil.getSchemaProperty(context, "type_IEFAssemblyFamily");
        TYPE_IEFCOMPONENTFAMILY = PropertyUtil.getSchemaProperty(context, "type_IEFComponentFamily");
        TYPE_INBOX_TASK = PropertyUtil.getSchemaProperty(context, "type_InboxTask");
        TYPE_ISSUE = PropertyUtil.getSchemaProperty(context, "type_Issue");
        TYPE_MAINPRODUCT = PropertyUtil.getSchemaProperty(context, "type_MainProduct");
        TYPE_MCAD_ASSEMBLY = PropertyUtil.getSchemaProperty(context, "type_MCADAssembly");
        TYPE_MCAD_COMPONENT = PropertyUtil.getSchemaProperty(context, "type_MCADComponent");
        TYPE_MCAD_MODEL = PropertyUtil.getSchemaProperty(context, "type_MCADModel");
        TYPE_MCADDRAWING = PropertyUtil.getSchemaProperty(context, "type_MCADDrawing");
        TYPE_MCADREPRESENTATION = PropertyUtil.getSchemaProperty(context, "type_MCADRepresentation");
        TYPE_MCO = PropertyUtil.getSchemaProperty(context, "type_MCO");
        TYPE_MODEL = PropertyUtil.getSchemaProperty(context, "type_Model");
        TYPE_NAMED_EFFECTIVITY = PropertyUtil.getSchemaProperty(context, "type_NamedEffectivity");
        TYPE_PART = PropertyUtil.getSchemaProperty(context, "type_Part");
        TYPE_PLMCORE_REFERENCE = PropertyUtil.getSchemaProperty(context, "type_PLMCoreReference");
        TYPE_PROCESS_CONTINUOUS_CREATE_MATERIAL = PropertyUtil.getSchemaProperty(context, "type_ProcessContinuousCreateMaterial");
        TYPE_PROCESSCONTINUOUSPROVIDE = PropertyUtil.getSchemaProperty(context, "type_ProcessContinuousProvide");
        TYPE_PRODUCTCONFIGURATION = PropertyUtil.getSchemaProperty(context, "type_ProductConfiguration");
        TYPE_PRODUCTS = PropertyUtil.getSchemaProperty(context, "type_Products");
        TYPE_PROEASSEMBLY = PropertyUtil.getSchemaProperty(context, "type_ProEAssembly");
        TYPE_PROJECT = PropertyUtil.getSchemaProperty(context, "type_Project");
        TYPE_PSS_3D_TEXTIL = PropertyUtil.getSchemaProperty(context, "type_PSS_3D_TEXTIL");
        TYPE_PSS_ALCANTARA = PropertyUtil.getSchemaProperty(context, "type_PSS_ALCANTARA");
        TYPE_PSS_ALLIED_PLASTICS = PropertyUtil.getSchemaProperty(context, "type_PSS_ALLIED_PLASTICS");
        TYPE_PSS_ALUMINUM = PropertyUtil.getSchemaProperty(context, "type_PSS_ALUMINUM");
        TYPE_PSS_ARTIFICIAL_LEATHER = PropertyUtil.getSchemaProperty(context, "type_PSS_ARTIFICIAL_LEATHER");
        TYPE_PSS_ASBG_TPO = PropertyUtil.getSchemaProperty(context, "type_PSS_ASBG_TPO");
        TYPE_PSS_ASBG_TPU = PropertyUtil.getSchemaProperty(context, "type_PSS_ASBG_TPU");
        TYPE_PSS_BEARING_CAGE = PropertyUtil.getSchemaProperty(context, "type_PSS_BEARING_CAGE");
        TYPE_PSS_BUSH = PropertyUtil.getSchemaProperty(context, "type_PSS_BUSH");
        TYPE_PSS_CARPET_MATERIAL = PropertyUtil.getSchemaProperty(context, "type_PSS_CARPET_MATERIAL");
        TYPE_PSS_CASE_HARDED_STEEL = PropertyUtil.getSchemaProperty(context, "type_PSS_CASE_HARDED_STEEL");
        TYPE_PSS_CASE_HARDENING = PropertyUtil.getSchemaProperty(context, "type_PSS_CASE_HARDENING");
        TYPE_PSS_CATALOG_MODEL_PIN_EJECTOR = PropertyUtil.getSchemaProperty(context, "type_PSS_CATALOG_MODEL_PIN_EJECTOR");
        TYPE_PSS_CATALOG_MODEL_PIN_LEADER = PropertyUtil.getSchemaProperty(context, "type_PSS_CATALOG_MODEL_PIN_LEADER");
        TYPE_PSS_CATALOG_MODEL_PUNCH = PropertyUtil.getSchemaProperty(context, "type_PSS_CATALOG_MODEL_PUNCH");
        TYPE_PSS_CHANGENOTICE = PropertyUtil.getSchemaProperty(context, "type_PSS_ChangeNotice");
        TYPE_PSS_CHANGEORDER = PropertyUtil.getSchemaProperty(context, "type_PSS_ChangeOrder");
        TYPE_PSS_CHANGEREQUEST = PropertyUtil.getSchemaProperty(context, "type_PSS_ChangeRequest");
        TYPE_PSS_COLORCATALOG = PropertyUtil.getSchemaProperty(context, "type_PSS_ColorCatalog");
        TYPE_PSS_COLOROPTION = PropertyUtil.getSchemaProperty(context, "type_PSS_ColorOption");
        TYPE_PSS_DESTINATIONREGION = PropertyUtil.getSchemaProperty(context, "type_PSS_DestinationRegion");
        TYPE_PSS_DIVISION = PropertyUtil.getSchemaProperty(context, "type_PSS_Division");
        TYPE_PSS_DOCUMENT = PropertyUtil.getSchemaProperty(context, "type_PSS_Document");
        TYPE_PSS_ENGINE = PropertyUtil.getSchemaProperty(context, "type_PSS_Engine");
        TYPE_PSS_EQUIPMENT_REQUEST = PropertyUtil.getSchemaProperty(context, "type_PSS_EquipmentRequest");
        TYPE_PSS_EXTERNALREFERENCE = PropertyUtil.getSchemaProperty(context, "type_PSS_ExternalReference");
        TYPE_PSS_HARMONY = PropertyUtil.getSchemaProperty(context, "type_PSS_Harmony");
        TYPE_PSS_HARMONY_REQUEST = PropertyUtil.getSchemaProperty(context, "type_PSS_HarmonyRequest");
        TYPE_PSS_IMPACTANALYSIS = PropertyUtil.getSchemaProperty(context, "type_PSS_ImpactAnalysis");
        TYPE_PSS_ISSUE = PropertyUtil.getSchemaProperty(context, "type_PSS_Issue");
        TYPE_PSS_LINEDATA = PropertyUtil.getSchemaProperty(context, "type_PSS_LineData");
        TYPE_PSS_MANUFACTURINGCHANGEACTION = PropertyUtil.getSchemaProperty(context, "type_PSS_ManufacturingChangeAction");
        TYPE_PSS_MANUFACTURINGCHANGEORDER = PropertyUtil.getSchemaProperty(context, "type_PSS_ManufacturingChangeOrder");
        TYPE_PSS_MATERIAL_REQUEST = PropertyUtil.getSchemaProperty(context, "type_PSS_Material_Request");
        TYPE_PSS_MBOM_VARIANT_ASSEMBLY = PropertyUtil.getSchemaProperty(context, "type_PSS_MBOMVariantAssembly");
        TYPE_PSS_OEM = PropertyUtil.getSchemaProperty(context, "type_PSS_OEM");
        TYPE_PSS_OEMGROUP = PropertyUtil.getSchemaProperty(context, "type_PSS_OEMGroup");
        TYPE_PSS_OPERATION = PropertyUtil.getSchemaProperty(context, "type_PSS_Operation");
        TYPE_PSS_PLANT = PropertyUtil.getSchemaProperty(context, "type_PSS_Plant");
        TYPE_PSS_PLATFORM = PropertyUtil.getSchemaProperty(context, "type_PSS_Platform");
        TYPE_PSS_PORTFOLIO = PropertyUtil.getSchemaProperty(context, "type_PSS_Portfolio");
        TYPE_PSS_PROGRAMPROJECT = PropertyUtil.getSchemaProperty(context, "type_PSS_ProgramProject");
        TYPE_PSS_RnDCENTER = PropertyUtil.getSchemaProperty(context, "type_PSS_RnDCenter");
        TYPE_PSS_ROLEASSESSMENT = PropertyUtil.getSchemaProperty(context, "type_PSS_RoleAssessment");
        TYPE_PSS_ROLEASSESSMENT_EVALUATION = PropertyUtil.getSchemaProperty(context, "type_PSS_RoleAssessmentEvaluation");
        TYPE_PSS_VARIANTASSEMBLY = PropertyUtil.getSchemaProperty(context, "type_PSS_VariantAssembly");
        TYPE_PSS_VED = PropertyUtil.getSchemaProperty(context, "type_PSS_VED");
        TYPE_PSS_VEHICLE = PropertyUtil.getSchemaProperty(context, "type_PSS_Vehicle");
        TYPE_ROUTE = PropertyUtil.getSchemaProperty(context, "type_Route");
        TYPE_ROUTETEMPLATE = PropertyUtil.getSchemaProperty(context, "type_RouteTemplate");
        TYPE_SERVICEPRODUCT = PropertyUtil.getSchemaProperty(context, "type_ServiceProduct");
        TYPE_SOFTWAREPRODUCT = PropertyUtil.getSchemaProperty(context, "type_SoftwareProduct");
        TYPE_VPMREFERENCE = PropertyUtil.getSchemaProperty(context, "type_VPMReference");
        TYPE_PSS_PDFARCHIVE = PropertyUtil.getSchemaProperty(context, "type_PSS_PDFArchive");
        TYPE_PSS_PAINTSYSTEM = PropertyUtil.getSchemaProperty(context, "type_PSS_PaintSystem");
        TYPE_PSS_PAINTLACK = PropertyUtil.getSchemaProperty(context, "type_PSS_PaintLack");
        TYPE_PSS_MATERIALMIXTURE = PropertyUtil.getSchemaProperty(context, "type_PSS_MaterialMixture");
        TYPE_PSS_MATERIAL = PropertyUtil.getSchemaProperty(context, "type_PSS_Material");
        TYPE_PSS_COLORMASTERBATCH = PropertyUtil.getSchemaProperty(context, "type_PSS_ColorMasterbatch");
        TYPE_PSS_PUBLISHCONTROLOBJECT = PropertyUtil.getSchemaProperty(context, "type_PSS_PublishControlObject");
        TYPE_PSS_LISTOFASSESSORS = PropertyUtil.getSchemaProperty(context, "type_PSS_ListOfAssessors");
        TYPE_PSS_PROEASSEMBLY = PropertyUtil.getSchemaProperty(context, "type_PSS_ProEAssembly");
        TYPE_PSS_PROEASSEMBLYFAMILYTABLE = PropertyUtil.getSchemaProperty(context, "type_PSS_ProEAssemblyFamilyTable");
        TYPE_PSS_PROEASSEMBLYINSTANCE = PropertyUtil.getSchemaProperty(context, "type_PSS_ProEAssemblyInstance");
        TYPE_PSS_PROEDRAWING = PropertyUtil.getSchemaProperty(context, "type_PSS_ProEDrawing");
        TYPE_PSS_PROEFORMAT = PropertyUtil.getSchemaProperty(context, "type_PSS_ProEFormat");
        TYPE_PSS_PROEPART = PropertyUtil.getSchemaProperty(context, "type_PSS_ProEPart");
        TYPE_PSS_PROEPARTFAMILYTABLE = PropertyUtil.getSchemaProperty(context, "type_PSS_ProEPartFamilyTable");
        TYPE_PSS_PROEPARTINSTANCE = PropertyUtil.getSchemaProperty(context, "type_PSS_ProEPartInstance");
        TYPE_PSS_UGASSEMBLY = PropertyUtil.getSchemaProperty(context, "type_PSS_UGAssembly");
        TYPE_PSS_UGDRAWING = PropertyUtil.getSchemaProperty(context, "type_PSS_UGDrawing");
        TYPE_PSS_UGMODEL = PropertyUtil.getSchemaProperty(context, "type_PSS_UGModel");
        TYPE_DERIVED_OUTPUT = PropertyUtil.getSchemaProperty(context, "type_DerivedOutput");
        
        TYPE_PSS_CATDRAWING = PropertyUtil.getSchemaProperty(context, "type_PSS_CATDrawing");
        
        TYPE_PSS_SWDRAWING = PropertyUtil.getSchemaProperty(context, "type_PSS_SWDrawing");
        
        TYPE_CADDRAWING = PropertyUtil.getSchemaProperty(context, "type_CADDrawing");
        TYPE_UGDRAWING = PropertyUtil.getSchemaProperty(context, "type_UGDrawing");
        // Type Constants

        // Others Constants
        ALL = "All";
        APPROVAL_STATUS = "Reject";
        ATTR_RANGE_PSSGEOMETRYTYPE = "MG";
        ATTR_VALUE_UNASSIGNED = "UNASSIGNED";
        DEFAULT_EVALUATION_REVIEW_ROUTE_TEMPLATE_FOR_CR = "Default Evaluation Review Route Template for CR";
        DEFAULT_IMPACT_ANALYSIS_ROUTE_TEMPLATE_FOR_CR = "Default Impact Analysis Route Template for CR";
        ENGINEERING_CR = "Engineering CR";
        FIELD_CHOICES = "field_choices";
        FIELD_DISPLAY_CHOICES = "field_display_choices";
        FOR_CLONE = "For Clone";
        FOR_REPLACE = "For Replace";
        PSS_FOR_REPLACE = "PSS_FOR_REPLACE";
        PSS_FOR_CLONE = "PSS_FOR_CLONE";
        IS_UNIT_OF_MEASURE_PC = "PC";
        MANUFACTURING_CR = "Manufacturing CR";
        PERSON_USER_AGENT = PropertyUtil.getSchemaProperty(context, "person_UserAgent");
        PLM_IMPLEMENTLINK_TARGETREFERENCE3 = "PLM_ImplementLink_TargetReference3";
        PROGRAM_CR = "Program CR";
        PROPERTIES_EMXFRCMBOMCENTRAL = "emxFRCMBOMCentral";
        PROPERTIES_EMXFRCMBOMCENTRALSTRINGRESOURCE = "emxFRCMBOMCentralStringResources";
        ROUTE_BASE_PURPOSE_APPROVAL = "Approval";
        ROUTE_REVISION = "1";
        PATTERN_MBOM_INSTANCE = PropertyUtil.getSchemaProperty(context, "relationship_DELFmiFunctionIdentifiedInstance") + ","
                + PropertyUtil.getSchemaProperty(context, "relationship_ProcessInstanceContinuous");
        SYMMETRIC_STATUS_ORIGINAL = "O";
        SYMMETRIC_STATUS_SYMMETRICAL = "S";
        TABLE_SETTING_DISABLESELECTION = "disableSelection";
        ATTRIBUTE_PSS_CATYPE_CAD = "CAD";
        ATTRIBUTE_PSS_CATYPE_PART = "Part";
        ATTRIBUTE_PSS_CATYPE_STD = "Standard";
        VAULT_ESERVICEPRODUCTION = PropertyUtil.getSchemaProperty(context, "vault_eServiceProduction");
        VAULT_ESERVICEADMINISTRATION = PropertyUtil.getSchemaProperty(context, "vault_eServiceAdministration");
        VAULT_VPLM = PropertyUtil.getSchemaProperty(context, "vault_vplm");
        RANGE_APPROVAL_LIST_FORCOMMERCIALUPDATEONCO = "Approval List for Commercial update on CO";
        RANGE_APPROVAL_LIST_FORPROTOTYPEONCO = "Approval List for Prototype on CO";
        RANGE_APPROVAL_LIST_FORSERIALLAUNCHONCO = "Approval List for Serial Launch on CO";
        RANGE_APPROVAL_LIST_FORDESIGNSTUDYONCO = "Approval List for Design study on CO";
        RANGE_APPROVAL_LIST_FORAcquisitionONCO = "Approval List for Acquisition on CO";
        RANGE_APPROVAL_LIST_FOROTHERPARTSONCO = "Approval List for Other Parts on CO";
        RANGE_APPROVAL_LIST_FORCADONCO = "Approval List for CAD on CO";
        RANGE_APPROVAL_LIST_FORSTANDARDPARTSONCO = "Approval List for Standard Parts on CO";
        RANGE_APPROVAL_LIST_FORCOMMERCIALUPDATEONMCO = "Approval List for Commercial update on MCO";
        RANGE_APPROVAL_LIST_FORPROTOTYPEONMCO = "Approval List for Prototype on MCO";
        RANGE_APPROVAL_LIST_FORSERIALLAUNCHONMCO = "Approval List for Serial Launch on MCO";
        RANGE_APPROVAL_LIST_FORDESIGNSTUDYONMCO = "Approval List for Design study on MCO";
        RANGE_APPROVAL_LIST_FOROTHERPARTSONMCO = "Approval List for Other Parts on MCO";
        RANGE_APPROVAL_LIST_FORAcquisitionONMCO = "Approval List for Acquisition on MCO";
        RANGE_OTHER = "Other";
        RANGE_DESIGN_STUDY = "Design study";
        RANGE_Acquisition = "Acquisition";
        RANGE_SERIAL_TOOL_LAUNCH_MODIFICATION = "Serial Tool Launch/Modification";
        RANGE_PROTOTYPE_TOOL_LAUNCH_MODIFICATION = "Prototype Tool Launch/Modification";
        RANGE_COMMERCIAL_UPDATE = "Commercial Update";
        ATTRIBUTE_PSS_DECISION_RANGE_NODECISION = "No Decision";
        ATTRIBUTE_PSS_DECISION_RANGE_NOGO = "No Go";
        ATTRIBUTE_PSS_DECISION_RANGE_GO = "Go";
        ATTRIBUTE_PSS_DECISION_RANGE_ABSTAIN = "Abstain";
        ATTRIBUTE_PSS_SYMMETRICALPARTSIDENTICALMASS_RANGEE_YES = "Yes";
        ATTRIBUTE_PSS_INWORKCONEWCRTAG_RANGE_YES = "YES";
        ATTRIBUTE_PSS_INWORKCONEWCRTAG_RANGE_NO = "NO";
        ATTRIBUTE_PSS_KEEPREFERENCEDOCUMENT_RANGE_YES = "Yes";
        ATTRIBUTE_PSS_CLONECOLORDIVERSITY_RANGE_YES = "Yes";
        ATTRIBUTE_PSS_CLONETECHNICALDIVERSITY_RANGE_YES = "Yes";

        SELECT_CONFIGURATIONFEATURE_ID_FROM_PRODUCTCONFIGURATION = "from[" + RELATIONSHIP_SELECTEDOPTIONS + "].torel[" + RELATIONSHIP_CONFIGURATION_OPTION + "].from[" + TYPE_CONFIGURATIONFEATURE
                + "].id";
        SELECT_CONFIGURATIONFEATURE_ID_FROM_EFFECTIVITY = "frommid[" + RELATIONSHIP_EFFECTIVITYUSAGE + "].torel.from.id";
        SELECT_CONFIGURATIONFEATURE_OPTION_RELID_FROM_EFFECTIVITY = "frommid[" + RELATIONSHIP_EFFECTIVITYUSAGE + "].torel.id";

        ATTR_RANGE_PSS_MANUFACTURINGPLANTEXTPSS_OWNERSHIP_CONSUMER = "Consumer";
        ATTR_RANGE_PSS_MANUFACTURINGPLANTEXTPSS_OWNERSHIP_MASTER = "Master";
        // Others Constants

        LIST_TYPE_MATERIALS = new StringList(new String[] { TYPE_PROCESS_CONTINUOUS_CREATE_MATERIAL, TYPE_PROCESSCONTINUOUSPROVIDE, TYPE_PSS_PAINTLACK, TYPE_PSS_PAINTSYSTEM, TYPE_PSS_MATERIALMIXTURE,
                TYPE_PSS_MATERIAL, TYPE_PSS_COLORMASTERBATCH });
        LIST_TYPE_CLONEMBOM = new StringList(new String[] { TYPE_PROCESS_CONTINUOUS_CREATE_MATERIAL, TYPE_PROCESSCONTINUOUSPROVIDE, TYPE_PSS_PAINTLACK, TYPE_PSS_PAINTSYSTEM, TYPE_PSS_MATERIALMIXTURE,
                TYPE_PSS_MATERIAL, TYPE_PSS_COLORMASTERBATCH, TYPE_PSS_OPERATION, TYPE_PSS_LINEDATA });

        SLC_DEFAULT_VIEW = "slc_default_view";
        STR_SUCCESS = "success";
        LOAD_SLC_DEFAULT_VIEW = "loadSLCDefaultView";
        SET_SLC_DEFAULT_VIEW = "setSLCDefaultView";
        ATTRIBUTE_PSS_CRWORKFLOW_RANGE_BASIC = "Basic";
        ATTRIBUTE_PSS_CRWORKFLOW_RANGE_FASTTRACK = "FastTrack";
        ATTRIBUTE_PSS_CRWORKFLOW_RANGE_PARALLELTRACK = "ParallelTrack";
        INVALID_AFFECTED_ITEMS = "InvalidAffectedItems";
        VALID_AFFECTED_ITEMS = "ValidAffectedItems";
        
        TITLE_BLOCK_CELL_CROSS_TAG = "<CROSS>";
        STRING_SINGLE_SPACE = " ";
        STRING_TRUE = "TRUE";
        STRING_FALSE = "FALSE";
        STRING_YES = "YES";
        STRING_NO = "NO";
        
        SEPERATOR_DOT = ".";
        SEPERATOR_COMMA = ",";
        SEPERATOR_MINUS = "-";
        SEPERATOR_FORWARD_SLASH = "/";
        
        OPERATOR_AND = " && ";
        OPERATOR_OR = " || ";
        OPERATOR_EQ = " == ";
        OPERATOR_NE = " != ";
        OPERATOR_GT = " > ";
        OPERATOR_LT = " < ";
        OPERATOR_LE = " <= ";
        OPERATOR_GE = " >= ";
        OPERATOR_SMATCH = " ~~ ";
        OPERATOR_NSMATCH = " !~~ ";
        OPERATOR_MATCH = " ~= ";
        OPERATOR_NMATCH = " !~= ";
        
        RPE_KEY_SKIP_TB_GENERATION_WEB = "PSS_CHECKIN_DURING_TITLE_BLOCK_GENERATION";
        RPE_KEY_JOB_SKIP_TB_GENERATION_WEB = "JOB_PSS_CHECKIN_DURING_TITLE_BLOCK_GENERATION";
        RPE_KEY_SKIP_TB_GENERATION_NATIVE = "PSS_SKIP_ON_CHECKIN_FROM_NATIVE";
        RPE_KEY_SKIP_ON_BG_JOB = "PSS_SKIP_ON_BG_JOB";
        
        ATTRIBUTE_PSS_MANDATORYCR_RANGE_OPTIONAL = "Optional";
        ATTRIBUTE_PSS_MANDATORYCR_RANGE_MANDATORY="Mandatory";
        STRING_PROGRAMPROJECT_TYPE_GOVERNING = "GOVERNING";
        STRING_PROGRAMPROJECT_TYPE_IMPACTED = "IMPACTED";
        
        ATTRIBUTE_PSS_READYFORDESCISION=PropertyUtil.getSchemaProperty(context, "attribute_PSS_ReadyForDecesion");
        
        // TIGTK-12983 - ssamel : START
        ATTRIBUTE_PSS_USERTYPE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_UserType");
        ATTRIBUTE_PSS_BGTYPE = PropertyUtil.getSchemaProperty(context, "attribute_PSS_BGType");
        ATTRIBUTE_PSS_OWNERSHIP = PropertyUtil.getSchemaProperty(context, "attribute_PSS_Ownership");

        ATTRIBUTE_PSS_USERTYPE_RANGE_JV = "JV";
        ATTRIBUTE_PSS_USERTYPE_RANGE_FAURECIA = "Faurecia";
        
        
        ROLE_PSS_CHANGE_COORDINATOR_JV = PropertyUtil.getSchemaProperty(context, "role_PSS_Change_Coordinator_JV");
        ROLE_PSS_PLANT_LAUNCH_TEAM_LEADER_JV = PropertyUtil.getSchemaProperty(context, "role_PSS_Plant_Launch_Team_Leader_JV");
        ROLE_PSS_PROGRAM_BUYER_JV = PropertyUtil.getSchemaProperty(context, "role_PSS_Program_Buyer_JV");
        ROLE_PSS_PROGRAM_CONTROLLER_JV = PropertyUtil.getSchemaProperty(context, "role_PSS_Program_Controller_JV");
        ROLE_PSS_PROGRAM_MANAGER_JV = PropertyUtil.getSchemaProperty(context, "role_PSS_Program_Manager_JV");
        ROLE_PSS_PROGRAM_MANUFACTURING_LEADER_JV = PropertyUtil.getSchemaProperty(context, "role_PSS_Program_Manufacturing_Leader_JV");
        ROLE_PSS_PROGRAM_QUALITY_LEADER_JV = PropertyUtil.getSchemaProperty(context, "role_PSS_Program_Quality_Leader_JV");
        ROLE_PSS_PROGRAM_SALES_LEADER_JV = PropertyUtil.getSchemaProperty(context, "role_PSS_Program_Sales_Leader_JV");
        ROLE_PSS_PRODUCT_DEVELOPMENT_LEAD_JV = PropertyUtil.getSchemaProperty(context, "role_PSS_Product_Development_Lead_JV");
        ROLE_PSS_PCANDL_JV = PropertyUtil.getSchemaProperty(context, "role_PSS_PCAndL_JV");
        ROLE_PSS_SPDE_AND_DL_JV = PropertyUtil.getSchemaProperty(context, "role_PSS_SPDE_AND_DL_JV");
        ROLE_PSS_CAD_DESIGNER_JV = PropertyUtil.getSchemaProperty(context, "role_PSS_CAD_Designer_JV");
        ROLE_PSS_READ_JV = PropertyUtil.getSchemaProperty(context, "role_PSS_Read_JV");
        // TIGTK-12983 - ssamel : END
    }
}