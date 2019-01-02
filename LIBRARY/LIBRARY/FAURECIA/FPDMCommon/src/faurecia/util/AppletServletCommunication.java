/*
 * Creation Date : 23 juil. 04
 *  v1.3
 */
package faurecia.util;

import java.io.EOFException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.Date;


//-----------------------------------
// FROM APPLET TO SERVLET :
//   sendOutput in applet is zipped       => readInput in servlet is zipped
// FROM SERVLET TO APPLET :
//   sendOutput in servlet is zipped      => readInput in applet may be unrecognizable (object not serializable) - MAYBE ZIPPED TWICE
//   sendOutput in servlet is not zipped  => readInput in applet is zipped or not, and ContentEncoding is filled (or not?)
//-----------------------------------


/**
 * @author fcolin
 */
public class AppletServletCommunication {

    // FDPM ADD Start JFA 30/01/05 Message Log management
    private final static String CLASSNAME          		= "AppletServletCommunication";
    // FDPM ADD End JFA 30/01/05 Message Log management
	
    /*
     * Send an array of Objects to the output Stream
     * All streams are compressed with GZIP (which is buffered)
     *
     */
     // Used by SERVLET => NOT ZIPPED
    public static void sendOutput (OutputStream out, Object[] a_Objects)
        throws Exception
    {
        //DebugUtil.debug(DebugUtil.DEBUG, "ServletCommunication.sendOutput", "Enter.");
        ObjectOutputStream objOutStream = new ObjectOutputStream(out);

        for (int iCpt = 0; iCpt < a_Objects.length; iCpt++) {
            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/sendOutput: " + a_Objects[iCpt].getClass().getName() + ".");
            if (Exception.class.isInstance(a_Objects[iCpt])) {
                DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/sendOutput: convert into Exception.");
                a_Objects[iCpt] = new Exception(a_Objects[iCpt].getClass().getName() + " : " + ((Exception)a_Objects[iCpt]).getMessage());
            }

            objOutStream.writeObject(a_Objects[iCpt]);
        }
        objOutStream.flush();
        objOutStream.close();

        //DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/sendOutput: Exit.");
        return;
    }

    // Used by APPLET => ZIPPED
    public static void sendZippedOutput (OutputStream out, Object[] a_Objects)
        throws Exception
    {
        try{
		//DebugUtil.debug(DebugUtil.DEBUG, "ServletCommunication.sendZippedOutput", "Enter.");
        GZIPOutputStream gzipOut = new GZIPOutputStream (out);
        ObjectOutputStream objOutStream = new ObjectOutputStream(gzipOut);

        for (int iCpt = 0; iCpt < a_Objects.length; iCpt++) {
        	DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/sendZippedOutput: " + a_Objects[iCpt].getClass().getName() + ".");
            if (Exception.class.isInstance(a_Objects[iCpt])) {
            	DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/sendZippedOutput: convert into Exception.");
                a_Objects[iCpt] = new Exception(a_Objects[iCpt].getClass().getName() + " : " + ((Exception)a_Objects[iCpt]).getMessage());
            }

            objOutStream.writeObject(a_Objects[iCpt]);
        }
        objOutStream.flush();
        objOutStream.close();

        //DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/sendOutput: Exit.");
        return;
		}catch(Exception e)
		{
			DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/sendZippedOutput: convert into Exception."+e );
		}
    }

    /*
     * Retrieve a list of Objects from the requester
     * All streams must have been compressed with GZIP
     */
    // Used by SERVLET => ZIPPED or NOT => the server is at least in JVM 1.3.1 => it can recognize if stream is zipped or not
    public static Object[] readInput (InputStream in)
        throws Exception
    {
        //DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/readInput: Enter.");
        String sSupposedContentType = "";
        if (in.markSupported()) {
        	DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/readInput: Mark supported = true" );
            sSupposedContentType = URLConnection.guessContentTypeFromStream(in);
        } else {
        	DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/readInput: Mark supported = false" );
        }

        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/readInput: Content Type guessed = " + sSupposedContentType);
        ObjectInputStream objInStream ;
        if ( "application/x-java-serialized-object".equals(sSupposedContentType)) {
            objInStream = new ObjectInputStream(in);
        } else {
            try {
            	DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/readInput: Try to retrieve GZIP");
                GZIPInputStream gzipIn = new GZIPInputStream (in);
                DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/readInput: GZIP Retrieved");
                objInStream = new ObjectInputStream(gzipIn);
            }
            catch (Exception e) {
            	DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/readInput: GZIP retrieving failed");
                objInStream = new ObjectInputStream(in);
            }
        }
        //DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/readInput: ObjectStream Retrieved");

        return readObjectInput(objInStream);
    }

    // Used by APPLET when ContentType is "gzip"
    public static Object[] readZippedInput (InputStream in)
        throws Exception
    {
    	DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/readZippedInput: Enter.");
        //DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/readZippedInput: Try to retrieve GZIP");
        GZIPInputStream gzipIn = new GZIPInputStream (in);
        //DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/readZippedInput: GZIP Retrieved");
        ObjectInputStream objInStream = new ObjectInputStream(gzipIn);
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/readZippedInput: ObjectStream Retrieved");

        return readObjectInput(objInStream);
    }

    /*
     * Retrieve a list of Objects from the requester
     * All streams must have been compressed with GZIP
     */
    // Used by APPLET when ContentType is not "gzip"
    public static Object[] readNotZippedInput (InputStream in)
        throws Exception
    {
        //DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/readNotZippedInput: Enter.");

        String sSupposedContentType = "";
        boolean bMarkSupported = in.markSupported();
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/readNotZippedInput: Mark supported = " + bMarkSupported );
        if (bMarkSupported) {
            sSupposedContentType = URLConnection.guessContentTypeFromStream(in);
            DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/readNotZippedInput: Content Type guessed = " + sSupposedContentType);
        }

        ObjectInputStream objInStream = new ObjectInputStream(in);
        //DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/readNotZippedInput: ObjectStream Retrieved");

        return readObjectInput(objInStream);
    }

    public static Object[] readObjectInput (ObjectInputStream objInStream)
        throws Exception
    {
        Vector vObjects = new Vector();
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/readObjectInput: Bytes available = " + objInStream.available());
        try {
            while (true) {
                Object tempObject = objInStream.readObject();
                DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/readObjectInput: Object retrieved = " + tempObject.getClass().getName());
                if (Exception.class.isInstance(tempObject)) {
                    DebugUtil.debug(DebugUtil.ERROR, CLASSNAME, "/readObjectInput: Exception thrown by the servlet : " + ((Exception)tempObject).getMessage());
                    throw (Exception)tempObject;
                }
                vObjects.addElement(tempObject);
            }
        }
        catch (EOFException eofEx) {
        	DebugUtil.debug(DebugUtil.ERROR, CLASSNAME, "/readObjectInput: End of the stream. " + eofEx.getMessage());
        }


        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/readObjectInput: Nb of objects retrieved from the servlet : " + vObjects.size());
        objInStream.close();

        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/readObjectInput: objInStream has been fully read.");

        // --------------------------------------------------------------------
        // CONVERT A VECTOR OF OBJECTS INTO AN ARRAY
        // --------------------------------------------------------------------
        Object[] a_Objects =new Object[vObjects.size()];
        for (int iCpt = 0; iCpt < vObjects.size(); iCpt++) {
            a_Objects[iCpt] = vObjects.elementAt(iCpt);
        }
        DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/readObjectInput: return of readInput : ", a_Objects);
        //DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/readObjectInput: Exit.");
        return a_Objects;
    }

    // Used by Applet
    public static Object[] requestServerTask(String sServletURL, Object[] inputArray)
        throws Exception
    {

        // --------------------------------------
        // Connection to the MCS server & send BusObject
        URL url = new URL(sServletURL);
/*        String sQuery = url.getQuery();
    	if (sQuery == null || sQuery.equals("")) {
    			sServletURL += "?time=" + new Date().getTime();
    			url = new URL(sServletURL);
    	}else if (!sQuery.contains("time=")) {
    			sServletURL += "&time=" + new Date().getTime();
    			url = new URL(sServletURL);
    	}
*/
    	DebugUtil.debug(DebugUtil.DEBUG, CLASSNAME, "/requestServerTask: Connecting to : " + sServletURL);
    	
    	
        URLConnection URLConn = url.openConnection();
        URLConn.setUseCaches(true);
        URLConn.setDoInput(true);
        URLConn.setDoOutput(true);
        URLConn.setDefaultUseCaches(true);
        URLConn.setRequestProperty("Content-Type", "application/octet-stream");

        sendZippedOutput(URLConn.getOutputStream(), inputArray);

        // --------------------------------------
        // Reading output of the servlet
        String sEncoding = URLConn.getContentEncoding();

        if ("gzip".equalsIgnoreCase(sEncoding)) {
            return readZippedInput(URLConn.getInputStream());
        } else {
            return readNotZippedInput(URLConn.getInputStream());
        }
    }

}
