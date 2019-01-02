/*
 * Creation Date : 23 juil. 04
 */
package faurecia.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;

/**
 * @author rinero
 *
 *This little object contains information about file to transfert.
 *This object is intended to be transmitted between client, server and eFCS server.
 *It give information about a file that is ingested in Matrix application. The information are:
 *Object ID, file name, file format, lock (true, false) and append (useful for a checkin request).
 *This class implement the Serializable interface.
 *It is used by the standard comunication system implemented in this package.
 *This object is used for performing a checkout as well as for performing a checkin. The information
 *contained by this object have not exactly the same meaning according to the type of use:
 *For example, the append information is useful only for a checkin. The lock information tell if
 *tell if the file should be locked during a checkout.
 *
 */
public class AbstractFile implements Serializable {
	
    // FDPM ADD Start JFA 30/01/05 Message Log management
    private final static String CLASSNAME          		= "AbstractFile";
    // FDPM ADD End JFA 30/01/05 Message Log management
	
    private String matrixObjectId = "";
    private String fileName = "";
    private String fileFormat = "";
    private Vector vfileContent = new Vector();
    private long fileSize = 0;
    private boolean append = false;
    private boolean lock = false;
    private String sJobTicket  = "";
    
    public AbstractFile() {
    }

    /**
     * The constructor of the object. Ask for all the information that can be contained in this
     * Object.
     * @param objectId The ID of the object in which the file is attached
     * @param fileName The name of the file
     * @param fileFormat The format of the file
     * @param append Information useful for the checkin: The file should be append to the
     * other file of the object, of should it replace the file for this format.
     * @param lock For a checkout, tell if the file is locked, or should be locked.
     */
    public AbstractFile(String objectId, String fileName, String fileFormat, boolean append, boolean lock) {
        this.matrixObjectId = objectId;
        this.fileName = fileName;
        this.fileFormat = fileFormat;
        this.append = append;
        this.lock = lock;
    }
     
    public AbstractFile(File file) throws IOException {
        this.fileName = file.getName();
        this.setContent(file);
    }
    public AbstractFile(String fileName) throws IOException {
        this.fileName = fileName;
    }
    /**
     * The constructor of the object. Ask for all the information that can be contained in this
     * Object.
     * @param objectId The ID of the object in which the file is attached
     * @param fileName The name of the file
     * @param fileFormat The format of the file
     * @param append Information useful for the checkin: The file should be append to the
     * other file of the object, of should it replace the file for this format.
     * @param lock For a checkout, tell if the file is locked, or should be locked.
     */
    public AbstractFile(String fileName, String sJobTicket) {
        this.fileName = fileName;
        this.sJobTicket= sJobTicket;
    }
     
    public void setContent (String fileName) 
        throws IOException, FileNotFoundException  
    {    
        File file = new File(fileName);
        setContent(file);
    }
    
    public void setContent (File file) 
        throws IOException 
    {    
        // Define attributes of the file
        this.fileName = file.getName();
        long fileLength = file.length();
        
        DebugUtil.debug(DebugUtil.INFO, CLASSNAME, "/setContent: Size of the file " + this.fileName + " is : " + fileLength + " bytes.");

        // Read file
        FileInputStream reader = new FileInputStream(file);
        
        this.setContent (reader);

        DebugUtil.debug(DebugUtil.INFO, CLASSNAME, "/setContent: Size of the stream " + this.fileSize + " bytes.");

        if (fileLength < this.fileSize) {
            // File Size is less than content amount.
            throw new IOException ("Too much data read.");
        }
        if (fileLength > this.fileSize) {
            // Some data has not been read.
            throw new IOException ("All bytes have not been transmitted.");
        }
            
        reader.close();
        return;
    }
    
    public void setContent (InputStream inputStream) 
        throws IOException, FileNotFoundException  
    {
        setContent (inputStream, true);
    }

    public void setContent (InputStream inputStream, boolean bIsInputStreamClosedAtEnd) 
        throws IOException, FileNotFoundException  
    {    
        int iLimit = -1;
        if (!bIsInputStreamClosedAtEnd) {
            iLimit = 0;
        }
        this.vfileContent = new Vector();
        int nbRead = 0;
        long nbGlobalRead = 0;
        DebugUtil.debug(DebugUtil.INFO, CLASSNAME, "/setContent: Before reading eFCS input.");
        do {
            byte[] buffer = new byte[8192];
            nbRead = inputStream.read(buffer);
            if (nbRead > 0) {
                // Add to the vector an array of bytes.
                // This array must be resized to the size read
                byte[] buffer2 = new byte[nbRead];
                System.arraycopy(buffer, 0, buffer2, 0, nbRead);

                this.vfileContent.addElement(buffer2);
                nbGlobalRead += nbRead;
            }
        } while (nbRead > iLimit);
            // while is made on strictly positive due to a bug in eMatrix API
            // (the inputstream is not closed by eMatrix API after the read)
        inputStream.close();
        this.fileSize = nbGlobalRead;
        DebugUtil.debug(DebugUtil.INFO, CLASSNAME, "/setContent: " + nbGlobalRead + " bytes have been transmitted from the file.");
        return;
    }

    public void setContent (Vector v_Lines) 
        throws IOException, FileNotFoundException  
    {    

        // -----------------------------------------
        // Convert vector of lines into a String
        String sContent = "";
        for (int i = 0; i < v_Lines.size(); i++) {
            sContent += (String)v_Lines.elementAt(i) + "\n";
        }

        // -----------------------------------------
        // Transmit the content into internal vector of byte[]
        this.vfileContent = new Vector();

        byte[] entireBuffer = sContent.getBytes();
        this.fileSize = entireBuffer.length;

        int nextPositionInBuffer = 0;
        // Transmit bytes in the Vector of the class
        
        long nbUntransmittedBytes = this.fileSize;
        while (nbUntransmittedBytes > 0) {
            
            if (nbUntransmittedBytes > 8192) {
                byte[] smallBuffer = new byte[8192];
                System.arraycopy(entireBuffer, nextPositionInBuffer, smallBuffer, 0, 8192);
                nextPositionInBuffer += 8192;
                this.vfileContent.addElement(smallBuffer);                       

            } else {
                byte[] smallBuffer = new byte[(int)nbUntransmittedBytes];
                System.arraycopy(entireBuffer, nextPositionInBuffer, smallBuffer, 0, (int)nbUntransmittedBytes);
                nextPositionInBuffer += nbUntransmittedBytes;
                this.vfileContent.addElement(smallBuffer);                       
                        
            }
            nbUntransmittedBytes = this.fileSize - nextPositionInBuffer;
                
        }           
        return;
    }

    public void setContentAfterUnCompress (InputStream inputStream, String sTempDirectory, boolean bIsInputStreamClosedAtEnd) 
        throws Exception 
    {    
        this.setContent(inputStream, bIsInputStreamClosedAtEnd);
        this.createFileOnSystemDrive(sTempDirectory, this.fileName);

        File filein = Local_OS_Information.Compressfile(new File("/tmp", this.fileName), false);
        
        this.setContent(filein);
        return;
    }

    public void createFileOnSystemDrive (String sDirectory) 
        throws IOException, FileNotFoundException  
    {    
        createFileOnSystemDrive (sDirectory, this.fileName);
    }

    public File createFileOnSystemDrive (String sDirectory, String fileName) 
        throws IOException, FileNotFoundException  
    {    
        File file = new File(sDirectory, fileName);

        DebugUtil.debug(DebugUtil.INFO, CLASSNAME, "/createFileOnSystemDrive: create file with name = " + file.getAbsolutePath());
        FileOutputStream writer = new FileOutputStream(file);
        if (this.fileSize != 0) {
            for (Enumeration eEnum = this.vfileContent.elements(); eEnum.hasMoreElements();){
                writer.write((byte[])eEnum.nextElement());
            }
        }
        writer.flush();
        writer.close();
        

        // New file is longer than the content transmitted.
        if (file.length() > this.fileSize) {
            throw new IOException("New file is longer than the content transmitted.");
        } else if (file.length() < this.fileSize) {
            throw new IOException("New file is smaller than the content transmitted.");
        }
        
        return file;
    }
        
    /**
     * @return The format of the file that is represented by this object.
     */
    public String getFileFormat() {
        return fileFormat;
    }

    /**
     * @return The name of the file that is represented by this object.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @return Tell whether the object is locked, or should be locked, according to the use
     * of this object.
     */
    public boolean isLock() {
        return lock;
    }

    /**
     * @return The ID of the object that contain the file that is represented by this object.
     */
    public String getObjectId() {
        return matrixObjectId;
    }

    /**
     * Set the Format of the file represented by this object.
     * @param string Format of the file represented by this object
     */
    public void setFileFormat(String string) {
        fileFormat = string;
    }

    /**
     * Set the name of the file represented by this object.
     * @param string Name of the file
     */
    public void setFileName(String string) {
        fileName = string;
    }

    /**
     * Set the lock information about the file represented by this object.
     * @param b : Tell whether the file is locked or should be locked.
     */
    public void setLock(boolean b) {
        lock = b;
    }

    /**
     * Set the ID of the object that contain the file represented by this object.
     * @param string : The ID of the object that contain the file represented by this object.
     */
    public void setObjectId(String string) {
        matrixObjectId = string;
    }

    /**
     * Get the append information about the file represented by this object. Useful during a checkin.
     * Indeed when a checkin is performed the file can be added to the existing files on the same object
     * and format, or it can replace them.
     * @return
     */
    public boolean isAppend() {
        return append;
    }

    /**
     * Set the append information about the file represented by this object. Useful during a checkin.
     * Indeed when a checkin is performed the file can be added to the existing files on the same object
     * and format, or it can replace them.
     * @param b : True if the file can be added, false if it should replace the other.
     */
    public void setAppend(boolean b) {
        append = b;
    }

    public String getJobTicket() {
        return sJobTicket;
    }

    /**
     * Transmit the content to an OutputStream
     * @param out
     * @return
     * @throws IOException
     */
    public long transmitContent(OutputStream out) throws IOException {
        for (Enumeration eEnum = this.vfileContent.elements(); eEnum.hasMoreElements();){
            out.write((byte[])eEnum.nextElement());
        }
        return this.fileSize;
    }

}
