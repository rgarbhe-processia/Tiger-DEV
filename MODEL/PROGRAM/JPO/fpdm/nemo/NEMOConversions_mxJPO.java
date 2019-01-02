package fpdm.nemo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.bind.DatatypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.AccessUtil;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;

import faurecia.util.NemoMessageStates;
import matrix.db.Access;
import matrix.db.Attribute;
import matrix.db.AttributeList;
import matrix.db.AttributeType;
import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.Query;
import matrix.db.QueryIterator;
import matrix.db.Relationship;
import matrix.db.RelationshipType;
import matrix.util.MatrixException;
import matrix.util.StringList;
import pss.constants.TigerConstants;

/**
 * Class used for NEMO Conversion. Used to request NEMO conversions.
 */
public class NEMOConversions_mxJPO {

    private static Logger logger = LoggerFactory.getLogger("fpdm.nemo.NEMOConversions");

    private AttributeType atNEMOChain;

    private AttributeType atNEMOOptions;

    public static String CUSTO_PREFIX = "customRelAttribute";

    final String regexOnlyId = "id=\"([^\"]+)\"";

    final Pattern pattern = Pattern.compile(regexOnlyId);

    @SuppressWarnings("unused")
    private DomainObject doNemoQueue;

    public NEMOConversions_mxJPO(Context context, String[] args) throws MatrixException {
        atNEMOChain = new AttributeType("FPDMConfType"); // TODO getSchemaProperty
        atNEMOOptions = new AttributeType("FPDMOptions"); // TODO getSchemaProperty

        doNemoQueue = fpdm.nemo.NEMOUtils_mxJPO.getNemoConversionQueueObject(context);
        logger.info("Conversion Queue retrieve !");
    }

    public Hashtable<String, NemoMessageStates> askConversions(Context context, String[] args) throws Exception {
        logger.debug("Nemo conversion");
        logger.debug("Ask for conversions");

        Hashtable<String, NemoMessageStates> htResponse = new Hashtable<String, NemoMessageStates>();

        HashMap<String, Object> paramValuesMap;
        try {
            paramValuesMap = (HashMap<String, Object>) JPO.unpackArgs(args);
        } catch (Exception e) {
            logger.debug("Error happened while unpacking args");
            e.printStackTrace();
            throw e;
        }

        logger.debug("paramValuesMap : " + paramValuesMap);
        if (paramValuesMap != null) {
            logger.debug(paramValuesMap.toString());
            try {
                ContextUtil.startTransaction(context, true);
                @SuppressWarnings("unchecked")
                HashMap<String, String> hmParamMap = (HashMap<String, String>) paramValuesMap.get("paramMap");

                String sId = hmParamMap.get("objectId");

                DomainObject dobCADObject = new DomainObject(sId);
                String sHasActiveVersion = dobCADObject.getInfo(context, "from[" + fpdm.nemo.NEMOUtils_mxJPO.RELATIONSHIP_ACTIVE_VERSION + "]");
                if ("True".equals(sHasActiveVersion)) {
                    sId = dobCADObject.getInfo(context, "from[" + fpdm.nemo.NEMOUtils_mxJPO.RELATIONSHIP_ACTIVE_VERSION + "].to." + DomainConstants.SELECT_ID);
                }

                logger.debug("Object ID : " + sId);
                String sComboObject = hmParamMap.get("comboObjects");
                logger.debug("Combo Object : " + sComboObject);

                if (sId != null && sComboObject != null) {
                    HashMap<String, HashMap<String, Object>> hmChainOptions = getSelectedOptions(context, paramValuesMap);
                    logger.debug("Chain Option : " + hmChainOptions);
                    htResponse = sendConversions(context, sId, sComboObject, hmChainOptions);
                }
                ContextUtil.commitTransaction(context);
            } catch (MatrixException e) {
                ContextUtil.abortTransaction(context);
                htResponse.put("ALL", NemoMessageStates.TECHNICAL_ERROR);
            }
        }
        return htResponse;
    }

    private Hashtable<String, NemoMessageStates> sendConversions(Context context, String sId, String sComboObject, HashMap<String, HashMap<String, Object>> hmChainOptions) throws MatrixException {
        logger.info("Send Conversion : " + sId + " / with chain : " + hmChainOptions);
        logger.info("sId : " + sId);

        Hashtable<String, NemoMessageStates> htResponse = new Hashtable<String, NemoMessageStates>();
        NemoMessageStates nmsError = NemoMessageStates.UNKNOWN;

        try {
            DomainObject doToConvert = DomainObject.newInstance(context, sId);
            try {
                if ("1".equals(sComboObject)) {
                    htResponse.putAll(sendConversionForChains(context, doToConvert, hmChainOptions));
                } else if ("5".equals(sComboObject)) {
                    ArrayList<String> alAlreadyParsed = new ArrayList<String>();
                    StringList slSubSelect = new StringList();
                    slSubSelect.addElement(doToConvert.getObjectId());
                    slSubSelect.addElement(DomainConstants.SELECT_ID);
                    htResponse = parseCADBOMAndSendConv(context, doToConvert, hmChainOptions, alAlreadyParsed, slSubSelect, true);
                } else if ("10".equals(sComboObject)) {
                    ArrayList<String> alAlreadyParsed = new ArrayList<String>();
                    StringList slSubSelect = new StringList();
                    slSubSelect.addElement(doToConvert.getObjectId());
                    slSubSelect.addElement(DomainConstants.SELECT_ID);
                    htResponse = parseCADBOMAndSendConv(context, doToConvert, hmChainOptions, alAlreadyParsed, slSubSelect, false);
                } else {
                    logger.error("comboObject not recognized : " + sComboObject);
                    nmsError = NemoMessageStates.COMBOOBJECT_NOT_RECOGNIZED;
                }
            } finally {
                doToConvert.close(context);
            }
        } catch (MatrixException e) {
            handleErrorSendNemoConversion(e, sId);
            throw e;
        }

        return htResponse;
    }

    private Hashtable<String, NemoMessageStates> parseCADBOMAndSendConv(Context context, DomainObject doToParse, HashMap<String, HashMap<String, Object>> hmChainOptions,
            ArrayList<String> alAlreadyParsed, StringList slSubSelect, boolean bOnlyComponents) throws MatrixException {
        Hashtable<String, NemoMessageStates> htResponse = new Hashtable<String, NemoMessageStates>();

        StringList slSelect = new StringList();
        slSelect.addElement(DomainConstants.SELECT_TYPE);
        slSelect.addElement(DomainConstants.SELECT_ID);
        doToParse.getObjectId();
        @SuppressWarnings("unchecked")
        Map<String, String> mInfos = doToParse.getInfo(context, slSelect);
        String sType = fpdm.nemo.NEMOUtils_mxJPO.getStringFromMap(mInfos, DomainConstants.SELECT_TYPE);
        String sId = fpdm.nemo.NEMOUtils_mxJPO.getSingleValue(mInfos.get(DomainConstants.SELECT_ID));
        logger.info("mInfos : " + mInfos);
        logger.info("sType : " + sType);
        logger.info("sId : " + sId);

        if (fpdm.nemo.NEMOInterface_mxJPO.is2D(context, sId)) {
            if (!bOnlyComponents) {
                htResponse.putAll(sendConversionForChains(context, doToParse, hmChainOptions, sType));
            }
            ArrayList<String> alToParse = getSubObjects(context, doToParse, fpdm.nemo.NEMOUtils_mxJPO.RELATIONSHIP_ASSOCIATED_DRAWING, slSubSelect, false);
            parseListToParseBomAndSend(context, alToParse, alAlreadyParsed, hmChainOptions, slSubSelect, bOnlyComponents);
        } else if (fpdm.nemo.NEMOInterface_mxJPO.isAssembly(context, sId)) {
            logger.info("Assembly");
            if (!bOnlyComponents) {
                htResponse.putAll(sendConversionForChains(context, doToParse, hmChainOptions, sType));
            }
            ArrayList<String> alToParse = getSubObjects(context, doToParse, fpdm.nemo.NEMOUtils_mxJPO.RELATIONSHIP_CAD_SUBCOMPONENT, slSubSelect, true);
            logger.info("alToParse : " + alToParse);
            htResponse.putAll(parseListToParseBomAndSend(context, alToParse, alAlreadyParsed, hmChainOptions, slSubSelect, bOnlyComponents));
        } else {
            htResponse.putAll(sendConversionForChains(context, doToParse, hmChainOptions, sType));
        }

        return htResponse;
    }

    private Hashtable<String, NemoMessageStates> parseListToParseBomAndSend(Context context, ArrayList<String> alToParse, ArrayList<String> alAlreadyParsed,
            HashMap<String, HashMap<String, Object>> hmChainOptions, StringList slSubSelect, boolean bOnlyComponents) throws MatrixException {
        logger.info("alToParse : " + alToParse);
        logger.info("alAlreadyParsed : " + alAlreadyParsed);

        Hashtable<String, NemoMessageStates> htResponse = new Hashtable<String, NemoMessageStates>();

        for (String sId : alToParse) {
            if (!alAlreadyParsed.contains(sId)) {
                alAlreadyParsed.add(sId);
                DomainObject doSub = DomainObject.newInstance(context, sId);
                try {
                    htResponse.putAll(parseCADBOMAndSendConv(context, doSub, hmChainOptions, alAlreadyParsed, slSubSelect, bOnlyComponents));
                } finally {
                    doSub.close(context);
                }
            }
        }

        return htResponse;
    }

    private ArrayList<String> getSubObjects(Context context, DomainObject doToParse, String sRelToParse, StringList slSubSelect, boolean bFrom) throws FrameworkException {
        ArrayList<String> alToReturn = new ArrayList<String>();
        MapList mlSubObjects = doToParse.getRelatedObjects(context, sRelToParse, "*", slSubSelect, null, !bFrom, bFrom, (short) 1, "", "", 0);
        logger.debug("mlSubObjects : " + mlSubObjects);
        logger.debug("sRelToParse : " + sRelToParse);
        logger.debug("bFrom : " + bFrom);
        logger.debug("doToParse.getObjectId() : " + doToParse.getObjectId());
        for (Iterator<?> ite = mlSubObjects.iterator(); ite.hasNext();) {
            Map<?, ?> mInfos = (Map<?, ?>) ite.next();
            String sId = (String) mInfos.get(DomainConstants.SELECT_ID);
            alToReturn.add(sId);
        }
        return alToReturn;
    }

    private void handleErrorSendNemoConversion(Exception e, String sId) {
        logger.error("Error while parsing object to send conversions to NEMO on object : " + sId, e);
    }

    private Hashtable<String, NemoMessageStates> sendConversionForChains(Context context, DomainObject doToConvert, HashMap<String, HashMap<String, Object>> hmChainOptions) throws MatrixException {
        logger.info("SendConversionForChains : " + doToConvert + " / " + hmChainOptions);
        String sType = doToConvert.getInfo(context, DomainConstants.SELECT_TYPE);
        return sendConversionForChains(context, doToConvert, hmChainOptions, sType);
    }

    private Hashtable<String, NemoMessageStates> sendConversionForChains(Context context, DomainObject doToConvert, HashMap<String, HashMap<String, Object>> hmChainOptions, String sType)
            throws MatrixException {
        Hashtable<String, NemoMessageStates> htResponse = new Hashtable<String, NemoMessageStates>();
        logger.info("SendConversionForChains : " + doToConvert + " / " + hmChainOptions + " / of type : " + sType);
        for (Entry<String, HashMap<String, Object>> eEntry : hmChainOptions.entrySet()) {
            String sChain = eEntry.getKey();
            logger.info("Entry : " + sChain);
            HashMap<String, Object> hmOpt = eEntry.getValue();
            htResponse.putAll(sendConversionForChain(context, doToConvert, sType, sChain, hmOpt));
        }
        return htResponse;
    }

    private Hashtable<String, NemoMessageStates> sendConversionForChain(Context context, DomainObject doToConvert, String sType, String sChain, HashMap<String, Object> hmChainOptions)
            throws MatrixException {
        logger.info("SendConversionForChains : " + doToConvert + " / " + hmChainOptions + " / of type : " + sType);
        logger.info("With options : " + hmChainOptions);
        logger.info("Chain options : " + fpdm.nemo.NEMOUtils_mxJPO.getStringOptions(hmChainOptions));
        logger.info("Send conversion for object : " + doToConvert.getObjectId() + " on chain " + sChain);
        logger.info("-----------------");

        Hashtable<String, NemoMessageStates> hmResponse = new Hashtable<String, NemoMessageStates>();

        String idReturn = "";

        boolean contextPushed = false;
        try {
            fpdm.treatmentqueue.TreatmentQueueNemoConversion_mxJPO tqNemo = new fpdm.treatmentqueue.TreatmentQueueNemoConversion_mxJPO(context, new String[] {});
            String sQueueName = tqNemo.treatmentQueueName;
            String sCustoQueue = (String) hmChainOptions.get("queue");
            if ((null != sCustoQueue) && (!sCustoQueue.isEmpty())) {
                sQueueName = sCustoQueue;
            }

            String sRelationName = fpdm.nemo.NEMOUtils_mxJPO.RELATIONSHIP_NEMO_CONVERSION_QUEUE;
            String sCustoRelation = (String) hmChainOptions.get("relation");
            if ((null != sCustoRelation) && (!sCustoRelation.isEmpty())) {
                sRelationName = sCustoRelation;
            }

            BusinessObject boQueue = new BusinessObject(fpdm.nemo.NEMOUtils_mxJPO.TYPE_NEMO_CONVERSION_QUEUE, sQueueName, "-", null);
            BusinessObject boObjectToConvert = new BusinessObject(doToConvert);

            StringBuilder sbId = new StringBuilder();
            sbId.append(boObjectToConvert.getObjectId(context));
            sbId.append("|");
            sbId.append(boObjectToConvert.getTypeName());
            sbId.append("|");
            sbId.append(boObjectToConvert.getName());
            sbId.append("|");
            sbId.append(boObjectToConvert.getRevision());
            sbId.append("|");
            sbId.append(sChain);
            sbId.append("|");
            sbId.append(fpdm.nemo.NEMOUtils_mxJPO.getStringOptions(hmChainOptions));

            idReturn = sbId.toString();

            logger.info(fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_NEMO_ACTION + "=" + sChain);

            NemoMessageStates nmsConversionExists = testIfConversionExists(context, doToConvert, sType, sChain, hmChainOptions);

            if (NemoMessageStates.UNKNOWN != nmsConversionExists) {
                hmResponse.put(idReturn, nmsConversionExists);
                return hmResponse;
            } else {
                String userName = context.getUser();
                RelationshipType rtRelation = new RelationshipType(sRelationName);
                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                contextPushed = true;
                Relationship rlObjectToChain = boQueue.connect(context, rtRelation, true, boObjectToConvert);
                AttributeList alRelation = new AttributeList();

                Attribute aQueueParam = new Attribute(new AttributeType(fpdm.treatmentqueue.TreatmentQueueAbstract_mxJPO.ATTRIBUTE_RELATIONSHIP_QUEUE_TODO_JOB_PARAMS),
                        fpdm.nemo.NEMOUtils_mxJPO.getStringOptions(hmChainOptions));
                alRelation.add(aQueueParam);

                Attribute aQueueStatus = new Attribute(new AttributeType(fpdm.treatmentqueue.TreatmentQueueAbstract_mxJPO.ATTRIBUTE_RELATIONSHIP_REPORT_PROCESS_STATUS_WP), "");
                alRelation.add(aQueueStatus);

                Attribute aRequester = new Attribute(new AttributeType(fpdm.treatmentqueue.TreatmentQueueAbstract_mxJPO.ATTRIBUTE_RELATIONSHIP_QUEUE_TODO_JOB_REQUESTER), userName);
                alRelation.add(aRequester);

                Attribute aNemoAction = new Attribute(new AttributeType(fpdm.treatmentqueue.TreatmentQueueNemoConversion_mxJPO.ATTRIBUTE_RELATIONSHIP_QUEUE_TODO_NEMO_ACTION), sChain);
                alRelation.add(aNemoAction);

                Map<String, Object> hmCustoAttributes = (Map<String, Object>) hmChainOptions.get("customRelAttributes");
                System.out.println("hmCustoAttributes : " + hmCustoAttributes);
                if (null != hmCustoAttributes) {
                    hmCustoAttributes.keySet().stream().forEach(item -> {
                        Object sAttributeValue = hmCustoAttributes.get(item);
                        System.out.println("<Nemo Conversion Custom Attribute>" + item + " : " + sAttributeValue + "- " + sAttributeValue.getClass());// + " : " + sAttributeValue[0]);
                        if (sAttributeValue instanceof String[]) {
                            String[] asValue = (String[]) sAttributeValue;
                            Attribute aNemoCustoAttribute = new Attribute(new AttributeType(item), asValue[0]);
                            alRelation.add(aNemoCustoAttribute);
                        }
                    });
                }

                rlObjectToChain.setAttributes(context, alRelation);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (contextPushed) {
                ContextUtil.popContext(context);
            }
        }

        hmResponse.put(idReturn, NemoMessageStates.OK);

        return hmResponse;

    }

    private NemoMessageStates testIfConversionExists(Context context, DomainObject doToConvert, String sType, String sChain, HashMap<String, Object> hmChainOptions)
            throws FrameworkException, MatrixException {
        BusinessObject boObjectToConvert = new BusinessObject(doToConvert);
        String sChainOptions = fpdm.nemo.NEMOUtils_mxJPO.getStringOptions(hmChainOptions);

        // Search if a connection with the same params for the same chain exists
        StringList selectStmts = new StringList(1);
        selectStmts.addElement(DomainConstants.SELECT_ID);
        selectStmts.addElement(DomainConstants.SELECT_NAME);
        StringList selectRelStmts = new StringList(3);
        selectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
        selectRelStmts.addElement(fpdm.nemo.NEMOUtils_mxJPO.SELECT_NEMO_ACTION);
        selectRelStmts.addElement("attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_NEMO_CONVERSION_QUEUE_JOB_PARAMS + "]");

        MapList mpExistingConversionsLinks = FrameworkUtil.toMapList(boObjectToConvert.getExpansionIterator(context, fpdm.nemo.NEMOUtils_mxJPO.RELATIONSHIP_NEMO_CONVERSION_QUEUE, // relationship
                // pattern
                "*", // type pattern
                selectStmts, // list of select statement pertaining to Business Objects
                selectRelStmts, // list of select statement pertaining to Relationships
                true, // get To relationships
                false, // get From relationships
                (short) 1, // the number of levels to expand, 0 equals expand all
                null, // where clause to apply to objects, can be empty
                "((attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_NEMO_ACTION + "].value==\'" + sChain + "\') && (attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_NEMO_CONVERSION_QUEUE_JOB_PARAMS
                        + "].value==\'" + sChainOptions + "\'))", // where clause to apply to relationship, can be empty
                (short) 0, // the maximum number of objects to return
                false, // true to check for hidden types per MX_SHOW_HIDDEN_TYPE_OBJECTS setting; false to return all objects, even if hidden
                false, // true to return each target object only once in expansion
                (short) 1, // page size to use for streaming data source
                false // boolean true to force HashTable data to StringList; false will return String for single-valued selects, StringList for multi-valued selects
        ), (short) 0, null, null, null, null);

        if (mpExistingConversionsLinks.size() > 0) {
            return NemoMessageStates.ALREADY_IN_QUEUE;
        }

        // Or if a viewable converted with this params for the same chain exists
        mpExistingConversionsLinks = FrameworkUtil.toMapList(boObjectToConvert.getExpansionIterator(context, fpdm.nemo.NEMOUtils_mxJPO.RELATIONSHIP_VIEWABLE, // relationship pattern
                "FPDM_Viewable", // type pattern
                selectStmts, // list of select statement pertaining to Business Objects
                selectRelStmts, // list of select statement pertaining to Relationships
                false, // get To relationships
                true, // get From relationships
                (short) 1, // the number of levels to expand, 0 equals expand all
                "attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_NEMO_CONVERSION_OPTIONS + "].value==\'" + sChainOptions + "\' and name ~~ \'*" + sChain + "*\'", // null, // where clause to apply to
                // objects, can be empty
                null, // where clause to apply to relationship, can be empty
                (short) 0, // the maximum number of objects to return
                false, // true to check for hidden types per MX_SHOW_HIDDEN_TYPE_OBJECTS setting; false to return all objects, even if hidden
                false, // true to return each target object only once in expansion
                (short) 1, // page size to use for streaming data source
                false // boolean true to force HashTable data to StringList; false will return String for single-valued selects, StringList for multi-valued selects
        ), (short) 0, null, null, null, null);

        if (mpExistingConversionsLinks.size() > 0) {
            return NemoMessageStates.ALREADY_IN_VIEWABLES;
        }

        return NemoMessageStates.UNKNOWN;
    }

    @SuppressWarnings("unused")
    private void createViewableAndUpdateAttributes(Context context, DomainObject doToConvert, String sType, String sChain, HashMap<String, Object> hmChainOptions, String sViewableType,
            String sViewablePolicy) throws MatrixException {
        DomainObject doViewable = fpdm.nemo.NEMOUtils_mxJPO.createViewable(context, doToConvert, sChain, sViewableType, sViewablePolicy);
        AttributeList alAttributes = new AttributeList();
        alAttributes.addElement(new Attribute(atNEMOChain, sChain));
        alAttributes.addElement(new Attribute(atNEMOOptions, fpdm.nemo.NEMOUtils_mxJPO.getStringOptions(hmChainOptions)));
        doViewable.setAttributeValues(context, alAttributes);
    }

    private boolean checkChainInType(Context context, String sType, String sChain) throws MatrixException { // TODO WHAT ?
        logger.info("sType : " + sType);
        logger.info("sChain : " + sChain);
        return true;
    }

    private HashMap<String, HashMap<String, Object>> getSelectedOptions(Context context, HashMap<String, Object> paramValuesMap) throws MatrixException {
        logger.debug("getSelectedOptions : " + paramValuesMap);
        HashMap<String, HashMap<String, Object>> hmChainOptions = new HashMap<String, HashMap<String, Object>>();

        try {
            Set<Entry<String, Object>> sEntries = paramValuesMap.entrySet();

            logger.debug(paramValuesMap.toString());
            logger.debug("sEntries : " + sEntries);

            String[] slCustomQueue = (String[]) paramValuesMap.get("customQueue");
            String sCustomQueue = "";
            if ((null != slCustomQueue) && (slCustomQueue.length > 0)) {
                sCustomQueue = (String) slCustomQueue[0];
            }

            String[] slCustomRelation = (String[]) paramValuesMap.get("customRelation");
            String sCustomRelation = "";
            if ((null != slCustomRelation) && (slCustomRelation.length > 0)) {
                sCustomRelation = (String) slCustomRelation[0];
            }

            Map<String, Object> mapCustomRelAttributes = paramValuesMap.entrySet().stream().filter(x -> x.getKey().startsWith(CUSTO_PREFIX))
                    .collect(Collectors.toMap(x -> x.getKey().substring(CUSTO_PREFIX.length()).replaceAll("\\[", "").replaceAll("\\]", ""), x -> x.getValue()));

            logger.debug("mapCustomRelAttributes : {}", mapCustomRelAttributes);

            logger.info("getSelectedOptions Entries : " + sEntries);
            StringList slChains = (StringList) paramValuesMap.get("conversions");
            Iterator<String> iChains = slChains.iterator();

            while (iChains.hasNext()) {
                String sChain = iChains.next();

                BusinessObject boConversion = new BusinessObject("FPDM_NemoConversionConfiguration", sChain, "-", TigerConstants.VAULT_ESERVICEPRODUCTION);
                DomainObject doObject = new DomainObject(boConversion);

                String sAttributeNemoConvOptions = "attribute[" + PropertyUtil.getSchemaProperty(context, "attribute_FPDM_NemoConversionOptions") + "]";
                String sOptions = doObject.getInfo(context, sAttributeNemoConvOptions);

                List<String> lsOptionsToRetrieve = new ArrayList<String>();

                HashMap<String, ArrayList<Object>> oOptions = null;
                List<Object> lOption = new ArrayList<Object>();
                try {
                    Object oFrom64 = fpdm.nemo.NEMOConversions_mxJPO.fromString64(sOptions);
                    oOptions = (HashMap<String, ArrayList<Object>>) oFrom64;
                    lOption = oOptions.get(sChain);

                    lOption.stream().forEach(item -> {
                        String[] asOption = (String[]) item;
                        List<String> lInsideOption = Arrays.asList(asOption);

                        lInsideOption.stream().forEach(item2 -> {
                            String thisOptionHTML = (String) item2;
                            try {
                                Matcher matcher = pattern.matcher(thisOptionHTML);

                                if (matcher.find()) {
                                    lsOptionsToRetrieve.add(matcher.group(1));
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    });
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                logger.debug("Options to retrieve : " + lsOptionsToRetrieve);

                HashMap<String, Object> hmOpt = new HashMap<String, Object>();
                lsOptionsToRetrieve.stream().forEach(item -> {
                    if (!sChain.equals(item)) {
                        if (item.startsWith(sChain)) {
                            String sOptName = item.substring(sChain.length());
                            Object oOptValue = paramValuesMap.get(item);

                            if (null != oOptValue) {
                                hmOpt.put(sOptName, oOptValue);
                                logger.debug(sOptName + " -> " + oOptValue);
                            }
                        }
                    }
                });
                HashMap<String, Object> hmCusAttr = new HashMap<>();
                hmCusAttr.putAll(mapCustomRelAttributes);
                hmOpt.put("customRelAttributes", hmCusAttr);
                if (!"".equals(sCustomQueue)) {
                    hmOpt.put("queue", sCustomQueue);
                }
                if (!"".equals(sCustomRelation)) {
                    hmOpt.put("relation", sCustomRelation);
                }
                hmChainOptions.put(sChain, hmOpt);
            }
            logger.debug("hmChainOptions : " + hmChainOptions);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hmChainOptions;
    }

    /**
     * Return the Profiles with Environments list existing on NEMO
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            no arguments
     * @return
     * @throws Exception
     */
    public Map<String, ArrayList<String[]>> getConversionOptions(Context context, String[] args) throws Exception {
        logger.info("getConversionOptions");
        HashMap<String, ArrayList<String[]>> hmToReturn = new HashMap<String, ArrayList<String[]>>();

        String objectId = "";
        if ((null != args) && (args.length > 0)) {
            objectId = args[0];
        } else {
            return hmToReturn;
        }

        DomainObject doObject = DomainObject.newInstance(context, objectId);
        String sType = doObject.getInfo(context, DomainObject.SELECT_TYPE);

        StringList objSelects = new StringList();
        String sAttributeNemoConvOptions = "attribute[" + PropertyUtil.getSchemaProperty(context, "attribute_FPDM_NemoConversionOptions") + "]";
        objSelects.addElement(sAttributeNemoConvOptions);
        objSelects.addElement(DomainConstants.SELECT_ID);
        objSelects.addElement(DomainConstants.SELECT_NAME);
        objSelects.addElement(DomainConstants.SELECT_DESCRIPTION);

        // Check which "file" we want to show (from object type) -> Convertion via properties
        logger.info("sType : " + sType);
        String convertionPropertyName = "FPDM.NEMO.CONVERSIONS." + sType;
        logger.info("convertionPropertyName : " + convertionPropertyName);
        ArrayList<String> alConversionName = fpdm.utils.Page_mxJPO.getPropertyValuesFromPage(context, "emxEngineeringCentral.properties", convertionPropertyName, "|");
        logger.info("convertionName : " + alConversionName);

        String sConvertionName = "";
        if ((null != alConversionName) && (alConversionName.size() > 0)) {
            sConvertionName = alConversionName.get(0);
        } else {
            return hmToReturn;
        }

        Query query = new Query();
        query.setBusinessObjectType(fpdm.nemo.NEMOUtils_mxJPO.TYPE_NEMO_CONVERSION_CONFIGURATION);
        logger.info("Type : " + fpdm.nemo.NEMOUtils_mxJPO.TYPE_NEMO_CONVERSION_CONFIGURATION);

        String safeConvertionName = sConvertionName.replaceAll(" ", "_");

        String sWhereExpression = "attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_FPDM_NEMO_CONVERSION_NAME + "]=='" + safeConvertionName + "'";

        if (fpdm.nemo.NEMOInterface_mxJPO.is2D(context, objectId)) {
            sWhereExpression += " && attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_FPDM_NEMO_CONVERSION_PDM_TYPE + "]=='2D'";
        }

        if (fpdm.nemo.NEMOInterface_mxJPO.isModel(context, objectId)) {
            sWhereExpression += " && attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_FPDM_NEMO_CONVERSION_PDM_TYPE + "]=='3D'";
        }

        if (fpdm.nemo.NEMOInterface_mxJPO.isAssembly(context, objectId)) {
            sWhereExpression += " && attribute[" + fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_NEMO_CONVERSION_FOR_ASSEMBLY + "]=='1'";
        }

        logger.info("sWhereExpression : " + sWhereExpression);

        query.setWhereExpression(sWhereExpression);

        QueryIterator qItr = null;
        try {
            ContextUtil.startTransaction(context, false);
            qItr = query.getIterator(context, objSelects, (short) 0);
            while (qItr.hasNext()) {
                BusinessObjectWithSelect busWithSelect = qItr.next();
                String convertionDescription = (String) busWithSelect.getSelectData(DomainConstants.SELECT_DESCRIPTION);
                String convertionName = (String) busWithSelect.getSelectData(DomainConstants.SELECT_NAME);

                HashMap hmResult = (HashMap) fromString64((String) busWithSelect.getSelectData(sAttributeNemoConvOptions));
                ArrayList<String[]> asOptions = (ArrayList<String[]>) hmResult.get(convertionName);

                if (!convertionDescription.isEmpty()) {
                    String[] asConversionDescription = { "Description :", "<span class='conversionDescription'>" + convertionDescription + "</span>" };
                    asOptions.add(1, asConversionDescription);
                }
                hmToReturn.put(convertionName, asOptions);
            }
            qItr.close();
            ContextUtil.commitTransaction(context);
        } catch (Exception ex) {
            ContextUtil.abortTransaction(context);
            throw new Exception(ex.toString());
        } finally {

        }

        logger.info("hmToReturn : " + hmToReturn);

        return hmToReturn;
    }

    /** Read the object from Base64 string. */
    private static Object fromString64(String s) throws IOException, ClassNotFoundException {
        byte[] data = DatatypeConverter.parseBase64Binary(s);
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
        Object o = ois.readObject();
        ois.close();
        return o;
    }

    public static boolean hasConversionAccess(Context context, String[] args) {
        HashMap<String, Object> paramValuesMap;

        logger.debug("hasConversinoAccess");

        String sId = "";
        try {
            paramValuesMap = (HashMap<String, Object>) JPO.unpackArgs(args);
            sId = (String) paramValuesMap.get("objectId");
            logger.debug("Object ID : " + sId);
        } catch (Exception e) {
            logger.info("Error happened while unpacking args");
            e.printStackTrace();
        }

        DomainObject doCAD = null;
        if (!sId.isEmpty()) {
            try {
                doCAD = new DomainObject(sId);
            } catch (Exception e) {
                logger.info("Error happened getting DomainObject");
                e.printStackTrace();
            }
        } else {
            return false;
        }

        boolean cadInstance = false;
        if (null != doCAD) {
            try {
                cadInstance = doCAD.isKindOf(context, DomainConstants.TYPE_CAD_DRAWING) || doCAD.isKindOf(context, DomainConstants.TYPE_CAD_MODEL);
                logger.debug("Drawing : " + doCAD.isKindOf(context, DomainConstants.TYPE_CAD_DRAWING));
                logger.debug("Model : " + doCAD.isKindOf(context, DomainConstants.TYPE_CAD_MODEL));
                logger.debug("cadInstance : " + cadInstance);
            } catch (FrameworkException e) {
                e.printStackTrace();
            }
        } else {
            return false;
        }

        if (cadInstance) {
            try {
                logger.debug("Policy : " + doCAD.getPolicy(context));
                logger.debug("Policy VERSIONED : " + pss.cad2d3d.DECTGUtil_mxJPO.POLICY_VERSIONEDDESIGNPOLICY);
                if (pss.cad2d3d.DECTGUtil_mxJPO.POLICY_VERSIONEDDESIGNPOLICY.equals(doCAD.getPolicy(context).getName())) {
                    logger.debug("Not master");
                    String masterID = doCAD.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_VERSIONOF + "].to.id");
                    logger.debug("masterID : " + masterID);
                    doCAD = new DomainObject(masterID);
                } else {
                    logger.debug("Master");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            return false;
        }

        boolean hasReadAcces = false;
        Access contextAccess;
        try {
            contextAccess = doCAD.getAccessMask(context);
            boolean hasAccess = AccessUtil.hasReadAccess(contextAccess);
            logger.debug("hasAccess : " + hasAccess);
            if (hasAccess) {
                hasReadAcces = true;
            }
        } catch (MatrixException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (hasReadAcces) {
            boolean accepted = false;
            try {
                logger.debug("Current : " + doCAD.getInfo(context, DomainConstants.SELECT_CURRENT));
                String sCurrent = doCAD.getInfo(context, DomainConstants.SELECT_CURRENT).replaceAll(" ", "_");
                String sPropertyConversion = "NEMO.conversion.access." + sCurrent + ".roles";
                List<String> lRoles = fpdm.utils.Page_mxJPO.getPropertyValuesFromPage(context, "emxEngineeringCentral.properties", sPropertyConversion, "|");
                logger.debug("Role : " + PersonUtil.getDefaultSCRole(context, context.getUser()));
                logger.debug("Roles accepted : " + lRoles);

                boolean contain = false;
                String sRole = PersonUtil.getDefaultSCRole(context, context.getUser());

                Iterator<String> itRole = lRoles.iterator();
                while (itRole.hasNext() && contain != true) {
                    String role = itRole.next().trim();
                    if (role.equals(sRole.trim())) {
                        logger.debug(role);
                        contain = true;
                    }
                }

                logger.debug("lRoles contains : " + contain);
                accepted = contain;
                logger.debug("Accepted : " + accepted);
                return contain;
            } catch (FrameworkException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            return false;
        }

        return true;
    }

}
