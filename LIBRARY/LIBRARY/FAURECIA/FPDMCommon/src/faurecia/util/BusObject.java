package faurecia.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.Hashtable;
import java.util.Vector;

/**
 * 
 * This class represent the view of a business object in the applet side.<br>
 * This is just a class that encapsulate the sObjectType, sObjectName, sObjectRevision, and file sObjectName of<br>
 * the object.<br>
 * As this class can't take several file sObjectName, if a Business Object have several<br>
 * associated files, it will be represented by several BusObject (one per file).<br>
 * 
 * @author rinero
 */
public class BusObject implements Serializable {
    private String sObjectId = "";

    private String sObjectType = "";

    private String sObjectName = "";

    private String sObjectRevision = "";

    private String sFileName = "";

    private String sFileFormat = "";

    private String sJobTicket = "";

    private String sFCSServletURL = "";

    private boolean bLock = false;

    private boolean bAppend = false;

    private String sToConnectId = "";

    private Vector<String> v_LSTFileContent = new Vector<String>();

    private Hashtable<Object, Object> hmInfo = new Hashtable<Object, Object>();

    private boolean bIsTIFFOnly = false;

    public BusObject() {
    }

    /**
     * Constructor that instanciate the different parameter.<br>
     * Parameters can be null.<br>
     * <br>
     * Each space at the beginning or at the end of the parameters will be<br>
     * removed automatically.<br>
     * 
     * @param sObjectId
     * @param sObjectType
     * @param sObjectName
     * @param sObjectRevision
     * @param sFileName
     * @param sFileFormat
     */
    public BusObject(String sObjectId, String sObjectType, String sObjectName, String sObjectRevision, String sFileName, String sFileFormat) {
        this.sObjectId = sObjectId;
        this.sObjectType = sObjectType;
        this.sObjectName = sObjectName;
        this.sObjectRevision = sObjectRevision;
        this.sFileName = sFileName;
        this.sFileFormat = sFileFormat;
    }

    /**
     * Constructor that instantiate the different parameter.<br>
     * Parameters can be null.<br>
     * <br>
     * Each space at the beginning or at the end of the parameters will be<br>
     * removed automatically.<br>
     * 
     * @param sObjectId
     * @param sObjectType
     * @param sObjectName
     * @param sObjectRevision
     * @param sFileName
     */
    public BusObject(String sObjectId, String sObjectType, String sObjectName, String sObjectRevision, String sFileName) {
        this.sObjectId = sObjectId;
        this.sObjectType = sObjectType;
        this.sObjectName = sObjectName;
        this.sObjectRevision = sObjectRevision;
        this.sFileName = sFileName;
    }

    /**
     * 
     * Constructor used for checkout.<br>
     * 
     * @param sObjectId
     * @param sObjectType
     * @param sFileName
     * @param sFileFormat
     * @param bLock
     * @param sToConnectId
     * 
     */
    public BusObject(String sObjectId, String sObjectType, String sFileName, String sFileFormat, boolean bLock, String sToConnectId) {

        this.sObjectId = sObjectId;
        this.sObjectType = sObjectType;
        this.sFileName = sFileName;
        this.sFileFormat = sFileFormat;
        this.bLock = bLock;
        this.sToConnectId = sToConnectId;

    }

    /**
     * 
     * Constructor used for checkin.<br>
     * 
     * @param sObjectType
     * @param sObjectName
     * @param sObjectRevision
     * @param sFileName
     * @param bAppend
     * @param bLock
     * @param bTIFFOnly 
     * @param sToConnectId
     * 
     */
    public BusObject(String sObjectType, String sObjectName, String sObjectRevision, String sFileName, boolean bAppend, boolean bLock, boolean bTIFFOnly, String sToConnectId) {

        this.sObjectType = sObjectType;
        this.sObjectName = sObjectName;
        this.sObjectRevision = sObjectRevision;
        this.sFileName = sFileName;
        this.bAppend = bAppend;
        this.bLock = bLock;
        this.sToConnectId = sToConnectId;
        this.bIsTIFFOnly = bTIFFOnly;
    }

    /**
     * Constructor that instantiate the different parameter.<br>
     * Parameters can be null.<br>
     * <br>
     * Each space at the beginning or at the end of the parameters will be<br>
     * removed automatically.<br>
     * 
     * @param sObjectId
     * @param sObjectType
     * @param sObjectName
     * @param sObjectRevision
     * @param sFileName
     * @param bIsTIFFOnly 
     */
    public BusObject(String sObjectId, String sObjectType, String sObjectName, String sObjectRevision, String sFileName, boolean bIsTIFFOnly) {
        this.sObjectId = sObjectId;
        this.sObjectType = sObjectType;
        this.sObjectName = sObjectName;
        this.sObjectRevision = sObjectRevision;
        this.sFileName = sFileName;
        this.bIsTIFFOnly = bIsTIFFOnly;
    }

    public String getFileName() {
        return sFileName;
    }

    public String getName() {
        return sObjectName;
    }

    public String getRevision() {
        return sObjectRevision;
    }

    public String getType() {
        return sObjectType;
    }

    public String getId() {
        return sObjectId;
    }

    public Hashtable getInfoMap() {
        return hmInfo;
    }

    /**
     * Two Businness objects are equals if they have the same sObjectType, sObjectName and description.<br>
     * The comparison is case-sensitive.<br>
     */
    public boolean equals(Object bo) {
        if (!(bo instanceof BusObject)) {
            return false;
        }
        BusObject param = (BusObject) bo;
        if ((this.sObjectType.equals(param.getType())) && (this.sObjectName.equals(param.getName())) && (this.sObjectRevision.equals(param.getRevision()))) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (this.sObjectType.toLowerCase().hashCode() + this.sObjectName.toLowerCase().hashCode() + this.sObjectRevision.toLowerCase().hashCode());
    }

    /**
     * For debug purpose, display the value of each field.<br>
     */
    public String toString() {
        return "TYPE:" + this.sObjectType + " NAME:" + this.sObjectName + " REVISION:" + this.sObjectRevision + " ID:" + this.sObjectId + " FILENAME:" + this.sFileName;
    }

    /**
     * Set the sObjectId of the object.<br>
     * 
     * @param sObjectId
     */
    public void setId(String sObjectId) {
        this.sObjectId = sObjectId;
    }

    public static BusObject[] retrieveBusInfoFromServlet(BusObject[] busObjects, String sMCSServletURL) throws Exception {
        Object[] a_Obj = AppletServletCommunication.requestServerTask(sMCSServletURL + "/getBusInfo", busObjects);
        busObjects = new BusObject[a_Obj.length];
        for (int i = 0; i < a_Obj.length; i++) {
            busObjects[i] = (BusObject) a_Obj[i];
        }

        return busObjects;
    }

    public static BusObject[] retrieveBusInfoFromServletForCheckinAll(BusObject[] busObjects, String sMCSServletURL) throws Exception {
        Object[] a_Obj = AppletServletCommunication.requestServerTask(sMCSServletURL + "/getBusInfo?checkinAll=true", busObjects);
        busObjects = new BusObject[a_Obj.length];
        for (int i = 0; i < a_Obj.length; i++) {
            busObjects[i] = (BusObject) a_Obj[i];
        }

        return busObjects;
    }

    public boolean isBAppend() {
        return bAppend;
    }

    public boolean isBLock() {
        return bLock;
    }

    public String getFCSServletURL() {
        return sFCSServletURL;
    }

    public String getFormat() {
        return sFileFormat;
    }

    public String getJobTicket() {
        return sJobTicket;
    }

    public String getToConnectId() {
        return sToConnectId;
    }

    public void setAppend(boolean b) {
        bAppend = b;
    }

    public void setFCSServletURL(String string) {
        sFCSServletURL = string;
    }

    public void setFormat(String string) {
        sFileFormat = string;
    }

    public void setJobTicket(String string) {
        sJobTicket = string;
    }

    public Vector getLstFileContent() {
        return v_LSTFileContent;
    }

    public void setLstFileContent(File file) throws IOException {
        v_LSTFileContent = new Vector<String>();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        for (String sLine = reader.readLine(); sLine != null; sLine = reader.readLine()) {
            v_LSTFileContent.addElement(sLine);
        }

    }

    public void setToConnectId(String sCADDefinitionID) {
        this.sToConnectId = sCADDefinitionID;
    }

    public void setInfo(Object key, Object value) {
        this.hmInfo.put(key, value);
    }

    public boolean isTIFFOnly() {
        return bIsTIFFOnly;
    }
}
