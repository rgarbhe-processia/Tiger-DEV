package faurecia.applet.fpdm;
/**
 * @author steria
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class Parameters {

	public static final String LOCAL_REPERTORY	= "localRepertory";
	public static final String SERVER_REPERTORY	= "serverRepertory";
	public static final String IN_OUT 			= "inOut";
	public static final String OBJECT_ID		= "objectId";
	public static final String HOST_NAME 		= "hostName";
	public static final String HOST_PORT		= "hostPort";
	public static final String SERVLET_NAME		= "servletName";
	public static final String FORMAT			= "format";
	public static final String FILENAME			= "fileName";
	public static final String SESSION_ID		= "sessionId";
	public static final String SESSION_NAME		= "sessionName";
	public static final String DEBUG			= "debug";
	public static final String EMCSSERVLETPATH	= "eMCSServletPath";
	public static final String BUSTYPE			= "BusType";
	public static final String BUSNAME			= "BusName";
	public static final String BUSREVISION		= "BusRevision";
	public static final String LANGUAGE			= "language";
	
	//Define the size of the buffer used during the stream transmission operation
	public static final short BUFFER_SIZE = 1024;
	public static final String DELIM = "|";
	
	public static final String CHECKIN 			= "CHECKIN" ;
	public static final String CHECKOUT 		= "CHECKOUT" ;
	public static final String CHECKOUTNOLOCK 		= "CHECKOUTNOLOCK" ;
	public static final String DEFAULT_FORMAT	= "generic" ;
	
	public String localRepertory;
	public String serverRepertory;
	public String inOrout;
	public String id;
	public String hostName;
	public String hostPort;
	public String servletName;
	public String format;
	public String fileName;
	public String sessionId;
	public String sessionName;
	public String eMCSServletPath;
	public String debug;
	public String busType;
	public String busName;
	public String busRevision;
	public String language;

	public void formatParameters() {

		FPDMAppletCheckInOut.debug("Parameters.formatParameters() START");
		if (isNull(localRepertory)) 	{localRepertory = null;}
		if (isNull(serverRepertory)) 	{serverRepertory = null;}
		if (isNull(inOrout)) 			{ inOrout = null;}
		if (isNull(id)) 				{ id = null;}
		if (isNull(hostName)) 			{hostName = null;}
		if (isNull(hostPort)) 			{hostPort = null;}
		if (isNull(servletName)) 		{servletName = null;}
		if (isNull(format)) 			{format = DEFAULT_FORMAT;}
		if (isNull(fileName)) 			{fileName = null;}
		if (isNull(sessionId)) 			{sessionId = null;}
		if (isNull(sessionName)) 		{sessionName = null;}
		if (isNull(eMCSServletPath)) 	{eMCSServletPath = null;}
		if (isNull(debug)) 				{debug = null;}
		if (isNull(busType)) 			{busType = null;}
		if (isNull(busName)) 			{busName = null;}
		if (isNull(busRevision)) 		{busRevision = null;}
		if (isNull(language)) 			{language = null;}
		FPDMAppletCheckInOut.debug("Parameters.formatParameters() END");
		
	}

	private boolean isNull(String s) {
		return ((s == null)||("".equals(s.trim()))||("null".equalsIgnoreCase(s)));
	}

	public String toString() {
		String sResult = "";
		sResult = sResult.concat("localRepertory " +localRepertory+"\n");
		sResult = sResult.concat("serverRepertory " +serverRepertory+"\n");
		sResult = sResult.concat("inOrout " +inOrout+"\n");
		sResult = sResult.concat("id " +id+"\n");
		sResult = sResult.concat("hostName " +hostName+"\n");
		sResult = sResult.concat("hostPort " +hostPort+"\n");
		sResult = sResult.concat("servletName " +servletName+"\n");
		sResult = sResult.concat("format " +format+"\n");
		sResult = sResult.concat("fileName " +fileName+"\n");
		sResult = sResult.concat("sessionId " +sessionId+"\n");
		sResult = sResult.concat("eMCSServletPath " +eMCSServletPath+"\n");
		sResult = sResult.concat("debug " +debug+"\n");
		sResult = sResult.concat("sBusType " +busType+"\n");
		sResult = sResult.concat("sBusName " +busName+"\n");
		sResult = sResult.concat("sBusRevision " +busRevision);

		return sResult ;
	}
	
	public void checkParameters()  throws Exception {
		FPDMAppletCheckInOut.debug("Parameters.checkParameters() START");
		
		if (inOrout == null || (!(CHECKIN).equalsIgnoreCase(inOrout) && !(CHECKOUT).equalsIgnoreCase(inOrout)) && !(CHECKOUTNOLOCK).equalsIgnoreCase(inOrout) ) {
			throw new Exception(FPDMAppletCheckInOut.translateMessages("Parameter.inOut.NotCorrect"));
		}
		if (isNull(id)) {
			throw new Exception(FPDMAppletCheckInOut.translateMessages("Parameter.id.NotCorrect"));
		}
		if (isNull(fileName)) {
			throw new Exception(FPDMAppletCheckInOut.translateMessages("Parameter.fileName.NotCorrect"));
		}
		if (isNull(busType)) {
			throw new Exception(FPDMAppletCheckInOut.translateMessages("Parameter.busType.NotCorrect"));
		}
		if (isNull(busName)) {
			throw new Exception(FPDMAppletCheckInOut.translateMessages("Parameter.busName.NotCorrect"));
		}
		if (isNull(busRevision)) {
			throw new Exception(FPDMAppletCheckInOut.translateMessages("Parameter.busRevision.NotCorrect"));
		}
		if (isNull(sessionId)) {
			throw new Exception(FPDMAppletCheckInOut.translateMessages("Parameter.sessionId.NotCorrect"));
		}
		if (isNull(hostName)) {
			throw new Exception(FPDMAppletCheckInOut.translateMessages("Parameter.hostName.NotCorrect"));
		}
		if (isNull(servletName)) {
			throw new Exception(FPDMAppletCheckInOut.translateMessages("Parameter.servletName.NotCorrect"));
		}
		if (isNull(localRepertory)) {
			throw new Exception(FPDMAppletCheckInOut.translateMessages("Parameter.localRepertory.NotCorrect"));
		}
		if (isNull(sessionName)) {
			throw new Exception(FPDMAppletCheckInOut.translateMessages("Parameter.sessionName.NotCorrect"));
		}


		FPDMAppletCheckInOut.debug("Parameters.checkParameters() END");
	}

}


