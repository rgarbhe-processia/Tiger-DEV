package fpdm.treatmentqueue;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.Job;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;

import matrix.db.Attribute;
import matrix.db.AttributeList;
import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.ExpansionIterator;
import matrix.db.JPO;
import matrix.db.Relationship;
import matrix.util.MatrixException;
import matrix.util.SelectList;

public class TreatmentQueueAbstract_mxJPO {
    private static Logger logger = LoggerFactory.getLogger("fpdm.treatmentqueue.TreatmentQueueAbstract");

    public static final String ATTRIBUTE_TREATMENT_QUEUE_PROCESS_IN_USE = "FPDM_TreatmentQueueProcessInUse"; // TODO From properties

    public static final String ATTRIBUTE_TREATMENT_QUEUE_MAXIMUM_REQUESTS_NUMBER = "FPDM_TreatmentQueueMaximumRequestsNumber"; // TODO From properties

    public static final String ATTRIBUTE_TREATMENT_QUEUE_LIMIT_REQUEST_AGE = "FPDM_TreatmentQueueLimitRequestAge"; // TODO From properties

    public static final String ATTRIBUTE_TREATMENT_QUEUE_LIMIT_OBJECTS_ALERT = "FPDM_TreatmentQueueLimitObjectsAlert"; // TODO From properties

    public static final String ATTRIBUTE_RELATIONSHIP_REPORT_PROCESS_STATUS = "attribute[FPDM_QueueJobProcessStatus]"; // TODO From properties

    public static final String ATTRIBUTE_RELATIONSHIP_REPORT_PROCESS_STATUS_WP = "FPDM_QueueJobProcessStatus"; // TODO From properties

    public static final String ATTRIBUTE_RELATIONSHIP_REPORT_ITERATION = "attribute[FPDM_QueueJobIteration]"; // TODO From properties

    public static final String ATTRIBUTE_RELATIONSHIP_REPORT_ITERATION_WP = "FPDM_QueueJobIteration"; // TODO From properties

    public static final String ATTRIBUTE_RELATIONSHIP_QUEUE_TODO_JOB_REQUESTER = "FPDM_QueueJobRequester";

    public static final String ATTRIBUTE_RELATIONSHIP_QUEUE_TODO_JOB_ID = "FPDM_QueueJobId";

    public static final String ATTRIBUTE_RELATIONSHIP_QUEUE_TODO_JOB_PARAMS = "FPDM_QueueJobParams";

    public static final String ATTRIBUTE_RELATIONSHIP_QUEUE_TODO_JOB_PROCESS_STATUS = "FPDM_QueueJobProcessStatus";

    public static final String ATTRIBUTE_RELATIONSHIP_QUEUE_TODO_JOB_MESSAGE = "FPDM_QueueJobMessage";

    public static final String ATTRIBUTE_RELATIONSHIP_QUEUE_TODO_JOB_ITERATION = "FPDM_QueueJobIteration";

    public static final String VAULT_PRODUCTION = "eService Production";

    /* QUEUE CONFIGURATION - CAN BE EXTENDED... */
    String TYPE_TREATMENT_QUEUE = "FPDM_TreatmentQueue*";

    public String treatmentQueueName = "";

    String RELATIONSHIP_THIS_REPORT_QUEUE = "";

    boolean waitForUnlocked = true;

    String[] queueStatesToDelete = { "DONE" };

    String[] queueStatesTimeOut = { "TIMEOUT" };

    String queueStateIfTimeOutReprocess = "QUEUED";

    String queueStateIfTimeOutFailed = "FAILED";

    String[] queueStatesFailed = { "FAILED" };

    String queueStateAfterFailed = "DONE";

    String[] queueStatesRunning = { "RUNNING" };

    String[] queueStatesQueued = { "QUEUED", "" };

    String queueStateJobStarted = "RUNNING";

    String[] queueStatesToNotShow = { "CANCELLED" };

    String jobJPOName = "";

    String jobMethodName = "";

    /* END OF QUEUE CONFIGURATION */

    BusinessObject boQueue;

    public TreatmentQueueAbstract_mxJPO(Context context, String[] args, String idQueue) throws Exception {
        logger.info("<FPDM_TreatmentQueueAbstract> Queue ID : " + idQueue + " ( " + this.RELATIONSHIP_THIS_REPORT_QUEUE + " )");
        this.boQueue = new BusinessObject(idQueue);
    }

    public TreatmentQueueAbstract_mxJPO(Context context, String[] args) {
        logger.info("TreatmentQueue Abstract - init");
    }

    /**
     * Get all objects in a specified Treatment Queue linked with a specified Relation
     * @param context
     *            the eMatric context
     * @param queueId
     *            ID if the queue we want to have informations
     * @param sRelationName
     *            Name of the relation who link objects with the queue
     * @return List of objects in a Treatment Queue
     * @throws MatrixException
     */
    public static MapList getAllObjectsInTreatmentQueue(Context context, String queueId, String sRelationName) throws MatrixException {
        logger.info("<FPDM_TreatmentQueueAbstract> " + queueId + " / " + sRelationName);
        return TreatmentQueueAbstract_mxJPO.getAllObjectsInTreatmentQueue(context, queueId, sRelationName, new String[] { "*" });
    }

    /**
     * Get all objects in a specified states in a specified Treatment Queue linked with a specified Relation
     * @param context
     *            the eMatric context
     * @param queueId
     *            ID if the queue we want to have informations
     * @param sRelationName
     *            Name of the relation who link objects with the queue
     * @param sStates
     *            List of object states we want to retrieve
     * @return List of objects in a Treatment Queue
     * @throws MatrixException
     */
    public static MapList getAllObjectsInTreatmentQueue(Context context, String argsqueueId, String sRelationName, String[] sStates, String... sNotStates) throws MatrixException {
        return getAllObjectsInTreatmentQueue(context, argsqueueId, sRelationName, "", sStates, sNotStates);
    }

    public static MapList getAllObjectsInTreatmentQueue(Context context, String argsqueueId, String sRelationName, String sWhereRelationShip, String[] sStates, String... sNotStates)
            throws MatrixException {
        BusinessObject boObj = new BusinessObject(argsqueueId);
        ContextUtil.startTransaction(context, false);

        SelectList selectStmts = new SelectList(1);
        selectStmts.add(DomainObject.SELECT_ID);

        SelectList slRelationSelects = new SelectList(2);
        slRelationSelects.add(DomainConstants.SELECT_RELATIONSHIP_ID);
        slRelationSelects.add(DomainConstants.SELECT_MODIFIED);

        StringBuilder sbWhereRelationship = new StringBuilder();
        if (null != sStates) {
            for (int x = 0; x < sStates.length; x++) {
                if (!"*".equals(sStates[x])) {
                    if (x != 0) {
                        sbWhereRelationship.append(" || ");
                    }
                    sbWhereRelationship.append(TreatmentQueueAbstract_mxJPO.ATTRIBUTE_RELATIONSHIP_REPORT_PROCESS_STATUS + "==\"" + sStates[x] + "\"");
                }
            }
        }

        if (null != sNotStates) {
            for (int x = 0; x < sNotStates.length; x++) {
                if (!"*".equals(sNotStates[x])) {
                    if (x != 0) {
                        sbWhereRelationship.append(" || ");
                    }
                    sbWhereRelationship.append(TreatmentQueueAbstract_mxJPO.ATTRIBUTE_RELATIONSHIP_REPORT_PROCESS_STATUS + "!=\"" + sNotStates[x] + "\"");
                }
            }
        }

        String sWhereContent = "";
        if (sbWhereRelationship.length() > 0) {
            sWhereContent = "(" + sbWhereRelationship.toString() + ")";
            if ((null != sWhereRelationShip) && (!sWhereRelationShip.isEmpty())) {
                sWhereContent += " && (" + sWhereRelationShip + ")";
            }
        }

        logger.info("Search for object where : " + sWhereContent);

        ExpansionIterator iter = boObj.getExpansionIterator(context, sRelationName, // relationship
                // pattern
                "*", // type pattern
                selectStmts, // list of select statement pertaining to Business
                // Objects
                slRelationSelects, // list of select statement pertaining to
                // Relationships
                false, // get To relationships
                true, // get From relationships
                (short) 1, // the number of levels to expand, 0 equals expand
                // all
                null, // where clause to apply to objects, can be empty
                sWhereContent, // where clause to apply to
                // relationship, can be empty
                (short) 0, // the maximum number of objects to return
                false, // true to check for hidden types per
                // MX_SHOW_HIDDEN_TYPE_OBJECTS setting; false to return
                // all objects, even if hidden
                false, // true to return each target object only once in
                // expansion
                (short) 1, // page size to use for streaming data source
                false // boolean true to force HashTable data to StringList;
                      // false will return String for single-valued selects,
                      // StringList for multi-valued selects
        );
        MapList mlObjectsList = FrameworkUtil.toMapList(iter, (short) 0, null, null, null, null);

        iter.close();
        ContextUtil.commitTransaction(context);

        return mlObjectsList;
    }

    /**
     * Get all element in a Treatment Queue linked with a specified relation
     * @param context
     *            The eMatrix context
     * @param args
     *            Array of arguments with inside :
     *            <li>
     *            <ul>
     *            relationName : Name of the relation who link objects with the queue
     *            </ul>
     *            </li>
     * @return A list with all elements in a treatment queue linked with a specified relation
     * @throws Exception
     */
    public MapList getAllElements(Context context, String[] args) throws Exception {
        @SuppressWarnings("unchecked")
        HashMap<String, Object> requestMap = (HashMap<String, Object>) JPO.unpackArgs(args);
        String sRelationName = (String) requestMap.get("relationName");

        MapList mlResult = new MapList();

        if (!"".equals(this.treatmentQueueName)) {
            logger.info("<FPDM_TreatmentQueueAbstract> Vault : " + context.getVault().getName());

            BusinessObject boQueue = new BusinessObject(this.TYPE_TREATMENT_QUEUE, this.treatmentQueueName, "-", null);
            logger.info(boQueue.toString());

            boQueue.open(context);

            String queueId = boQueue.getObjectId(context);
            MapList mlObjectsList = TreatmentQueueAbstract_mxJPO.getAllObjectsInTreatmentQueue(context, queueId, sRelationName, new String[] { "*" }, this.queueStatesToNotShow);
            boQueue.close(context);

            mlResult.addAll(mlObjectsList);
        }
        logger.info("<FPDM_TreatmentQueueAbstract> MAPLIST : " + mlResult);
        return mlResult;
    }

    public void launchTreatmentQueue(Context context, String[] args) throws FrameworkException, MatrixException {
        logger.info("<FPDM_TreatmentQueueAbstract> Relation for Treatment : " + this.RELATIONSHIP_THIS_REPORT_QUEUE);

        boQueue.open(context);
        Attribute attQueueInUse = boQueue.getAttributeValues(context, TreatmentQueueAbstract_mxJPO.ATTRIBUTE_TREATMENT_QUEUE_PROCESS_IN_USE);

        if ("TRUE".equals(attQueueInUse.getValue())) {
            logger.info("<FPDM_TreatmentQueueAbstract> Queue in use...");
            return;
        } else if ("LOCKED".equals(attQueueInUse.getValue())) {
            logger.info("<FPDM_TreatmentQueueAbstract> Queue locked...");
            return;
        } else {
            boQueue.setAttributeValue(context, TreatmentQueueAbstract_mxJPO.ATTRIBUTE_TREATMENT_QUEUE_PROCESS_IN_USE, "TRUE");
        }

        Iterator<Hashtable<String, Object>> itMLProcess = null;

        // The CJ asks for the queue for the relation who can be deleted and
        // delete this relations
        MapList hmToDelete = this.getRelationsToDelete(context, args);
        logger.info("<FPDM_TreatmentQueueAbstract> relations DONE : " + hmToDelete);

        itMLProcess = hmToDelete.iterator();
        while (itMLProcess.hasNext()) {
            Hashtable<String, Object> htRelationToDelete = itMLProcess.next();
            Relationship rlToDelete = new Relationship((String) htRelationToDelete.get("id[connection]"));
            rlToDelete.remove(context);
        }

        // For relations with the status TIMEOUT, we check the number of times
        // we tried to do the job. If this number >
        // FPDM_TreatmentQueueMaximumTries of the queue object, we set the
        // status of this relation to FAILED. Otherwise, we set the status of
        // the relation to QUEUED.
        MapList hmTimeOut = this.getRelationsTimeOut(context, args);
        itMLProcess = hmTimeOut.iterator();

        while (itMLProcess.hasNext()) {
            Hashtable<String, Object> htRelationToProcess = itMLProcess.next();
            Relationship rlToProcess = new Relationship((String) htRelationToProcess.get("id[connection]"));
            logger.info("<FPDM_TreatmentQueueAbstract> relation timeout : " + rlToProcess);

            Attribute attToProcess = rlToProcess.getAttributeValues(context, TreatmentQueueAbstract_mxJPO.ATTRIBUTE_RELATIONSHIP_REPORT_ITERATION_WP);

            int intNumberIteration = Integer.parseInt(attToProcess.getValue());
            logger.info("<FPDM_TreatmentQueueAbstract> Number of iteration : " + intNumberIteration);

            AttributeList alToSendToRelation = new AttributeList();
            Attribute attToSend = rlToProcess.getAttributeValues(context, TreatmentQueueAbstract_mxJPO.ATTRIBUTE_RELATIONSHIP_REPORT_PROCESS_STATUS_WP);
            if (intNumberIteration <= 0) {
                logger.info("<FPDM_TreatmentQueueAbstract> Reached max number of attempts");
                attToSend.setValue(this.queueStateIfTimeOutFailed);
                alToSendToRelation.add(attToSend);
                rlToProcess.setAttributeValues(context, alToSendToRelation);
            } else {
                logger.info("<FPDM_TreatmentQueueAbstract> We can launch this treatment again");
                attToSend.setValue(this.queueStateIfTimeOutReprocess);
                alToSendToRelation.add(attToSend);
                rlToProcess.setAttributeValues(context, alToSendToRelation);
            }
        }

        // Treat the failed connections
        MapList hmFailed = this.getRelationsFailed(context, args);
        itMLProcess = hmFailed.iterator();
        while (itMLProcess.hasNext()) {
            Hashtable<String, Object> htRelationToProcess = itMLProcess.next();
            Relationship rlToProcess = new Relationship((String) htRelationToProcess.get("id[connection]"));
            logger.info("<FPDM_TreatmentQueueAbstract> relation failed : " + rlToProcess);
            this.processFailedJobs(context, args, rlToProcess);
        }

        // The CJ asks the queue if it is full (number of relation with process
        // status RUNNING
        MapList hmRunning = this.getRelationsRunning(context, args);
        logger.info("<FPDM_TreatmentQueueAbstract> relations RUNNING : " + hmRunning);
        Attribute aMaxRequestsNumber = boQueue.getAttributeValues(context, ATTRIBUTE_TREATMENT_QUEUE_MAXIMUM_REQUESTS_NUMBER);
        int maxNumberOfRequest = Integer.parseInt(aMaxRequestsNumber.getValue());
        logger.info("<FPDM_TreatmentQueueAbstract> Max number of requests : " + maxNumberOfRequest);

        if (hmRunning.size() >= maxNumberOfRequest) {
            logger.info("<FPDM_TreatmentQueueAbstract> Max number of requests reached");
            return;
        }

        logger.info("<FPDM_TreatmentQueueAbstract> Max number of requests not reached (" + hmRunning.size() + " running processes)");
        // If the queue is not full, asks for the x (x =
        // FPDM_TreatementQueueMaximumRequestsNumber - number of relation with
        // process status RUNNING) first relations with the status QUEUED
        // (sorted by request date ascending)
        MapList hmToLaunch = this.getRelationsToLaunch(context, args);
        // Sort by modified
        hmToLaunch.addSortKey(DomainConstants.SELECT_MODIFIED, "ascending", "String");
        hmToLaunch.sortStructure();
        logger.info("<FPDM_TreatmentQueueAbstract> relations to launch : " + hmToLaunch);

        int nbrProcesses = maxNumberOfRequest - hmRunning.size();
        logger.info("<FPDM_TreatmentQueueAbstract> Number of processes to launch : " + nbrProcesses);
        itMLProcess = hmToLaunch.iterator();
        while ((itMLProcess.hasNext()) && (nbrProcesses > 0)) {
            Hashtable<String, Object> htRelationToProcess = itMLProcess.next();
            Relationship rlToProcess = new Relationship((String) htRelationToProcess.get("id[connection]"));
            logger.info("<FPDM_TreatmentQueueAbstract> relation to launch : " + rlToProcess);
            this.newJob(context, args, rlToProcess);
            nbrProcesses -= 1;
        }

        boQueue.setAttributeValue(context, TreatmentQueueAbstract_mxJPO.ATTRIBUTE_TREATMENT_QUEUE_PROCESS_IN_USE, "FALSE");
        logger.info("<FPDM_TreatmentQueueAbstract> End of the process");
    }

    public Vector<Object> getActionCommands(Context context, String[] args) throws Exception {
        logger.info("<FPDM_TreatmentQueue> getActionCommand");
        @SuppressWarnings("rawtypes")
        HashMap requestMap = (HashMap) JPO.unpackArgs(args);

        @SuppressWarnings("rawtypes")
        HashMap paramList = (HashMap) requestMap.get("paramList");

        String sJPOToSearch = (String) paramList.get("program");
        sJPOToSearch = sJPOToSearch.split(":")[0];

        Vector<Object> columnValues = new Vector<Object>();

        String queueCalled = (String) requestMap.get("treatmentQueueCalled");
        logger.info("<FPDM_TreatmentQueue> " + queueCalled);

        if ("true".equals(queueCalled)) {
            MapList objList = (MapList) requestMap.get("objectList");

            @SuppressWarnings({ "rawtypes", "unchecked" })
            Iterator<Hashtable> itObjectList = objList.iterator();

            while (itObjectList.hasNext()) {
                @SuppressWarnings("rawtypes")
                Hashtable oneObject = itObjectList.next();
                columnValues.add("<a href=\"FPDM_TreatmentQueue/FPDM_TreatmentQueueUtils.jsp?action=cancel&idconnection=" + oneObject.get("id[connection]") + "\">Cancel</a>");
            }
        } else {
            requestMap.put("treatmentQueueCalled", "true");
            args = JPO.packArgs(requestMap);

            columnValues = JPO.invoke(context, sJPOToSearch, null, "getActionCommands", args, java.util.Vector.class);
        }

        return columnValues;
    }

    public MapList getRelationsToDelete(Context context, String[] args) throws FrameworkException, MatrixException {
        if (this.boQueue != null) {
            logger.info("<FPDM_TreatmentQueueAbstract> getRelationsToDelete");
            return TreatmentQueueAbstract_mxJPO.getAllObjectsInTreatmentQueue(context, boQueue.getObjectId(), this.RELATIONSHIP_THIS_REPORT_QUEUE, this.queueStatesToDelete);
        }
        return new MapList();
    }

    public MapList getRelationsTimeOut(Context context, String[] args) throws FrameworkException, MatrixException {
        logger.info("<FPDM_TreatmentQueueAbstract> Before getRelationsTimeOut");
        logger.info("<FPDM_TreatmentQueueAbstract> " + this.boQueue);
        if (this.boQueue != null) {
            logger.info("<FPDM_TreatmentQueueAbstract> getRelationsTimeOut");
            return TreatmentQueueAbstract_mxJPO.getAllObjectsInTreatmentQueue(context, boQueue.getObjectId(), this.RELATIONSHIP_THIS_REPORT_QUEUE, this.queueStatesTimeOut);
        }

        return new MapList();
    }

    public MapList getRelationsFailed(Context context, String[] args) throws FrameworkException, MatrixException {
        if (this.boQueue != null) {
            logger.info("<FPDM_TreatmentQueueAbstract> getRelationsFailed");
            return TreatmentQueueAbstract_mxJPO.getAllObjectsInTreatmentQueue(context, boQueue.getObjectId(), this.RELATIONSHIP_THIS_REPORT_QUEUE, this.queueStatesFailed);
        }

        return new MapList();
    }

    public MapList getRelationsRunning(Context context, String[] args) throws FrameworkException, MatrixException {
        if (this.boQueue != null) {
            logger.info("<FPDM_TreatmentQueueAbstract> getRelationsRunning");
            return TreatmentQueueAbstract_mxJPO.getAllObjectsInTreatmentQueue(context, boQueue.getObjectId(), this.RELATIONSHIP_THIS_REPORT_QUEUE, this.queueStatesRunning);
        }

        return new MapList();
    }

    public void processFailedJobs(Context context, String[] args, Relationship rlToProcess) throws MatrixException {
        logger.info("<FPDM_TreatmentQueueAbstract> Failed job : " + rlToProcess);
        context.start(true);
        AttributeList alToSendToRelation = new AttributeList();
        Attribute attToSend = rlToProcess.getAttributeValues(context, TreatmentQueueAbstract_mxJPO.ATTRIBUTE_RELATIONSHIP_REPORT_PROCESS_STATUS_WP);
        attToSend.setValue(this.queueStateAfterFailed);
        alToSendToRelation.add(attToSend);
        rlToProcess.setAttributeValues(context, alToSendToRelation);
        context.commit();
    }

    public MapList getRelationsToLaunch(Context context, String[] args) throws FrameworkException, MatrixException {
        if (this.boQueue != null) {
            logger.info("<FPDM_TreatmentQueueAbstract> getRelationsToLaunch");
            return TreatmentQueueAbstract_mxJPO.getAllObjectsInTreatmentQueue(context, boQueue.getObjectId(), this.RELATIONSHIP_THIS_REPORT_QUEUE, this.queueStatesQueued);
        }

        return new MapList();
    }

    public MapList newJob(Context context, String[] args, Relationship rlToProcess) throws MatrixException {
        logger.info("<FPDM_TreatmentQueueAbstract> --- New JOB ---");
        logger.info("<FPDM_TreatmentQueueAbstract> Treatment Queue : " + this.treatmentQueueName);

        try {

            logger.info("<FPDM_TreatmentQueueAbstract> " + this.jobJPOName + " -> " + this.jobMethodName);
            Job job = new Job(this.jobJPOName, this.jobMethodName, args);
            job.setActionOnCompletion("Delete");
            job.createAndSubmit(context);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return new MapList();
    }

    public boolean lockQueue(Context context) {
        logger.info("<FPDM_TreatmentQueueAbstract> Treatment Queue : " + this.treatmentQueueName);
        try {
            boQueue.setAttributeValue(context, TreatmentQueueAbstract_mxJPO.ATTRIBUTE_TREATMENT_QUEUE_PROCESS_IN_USE, "LOCKED");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public boolean unlockQueue(Context context) {
        logger.info("<FPDM_TreatmentQueueAbstract> Treatment Queue : " + this.treatmentQueueName);
        try {
            boQueue.setAttributeValue(context, TreatmentQueueAbstract_mxJPO.ATTRIBUTE_TREATMENT_QUEUE_PROCESS_IN_USE, "FALSE");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
