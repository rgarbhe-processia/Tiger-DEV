
package faurecia.applet.fpdm;
/**
 *
 * This class enable to create Check.
 * 2 different tasks are done in it:
 * - Coherence of parameters is checked.
 * - A Check of the right type (according to checkin or checkout) is returned
 *
 * @author rinero
 *
 *
 */
public class CheckFactory {
	//private static String DEFAULT_FORMAT = "generic";

/**This class verify the coherence of the parameters and<br>
 * return a Check that implement the good type (checkin or checkout)<br>
 * The mandatory parameters are:<br>
 * <ul>
 * - id<br>
 * - hostSite<br>
 * - repertory<br>
 * </ul>
 * <br>
 * For checkin, another parameter is mandatory:<br>
 * <ul>
 * - parameterFile<br>
 * - script<br>
 * </ul>
 * <br>
 * The default value of the parameter generic is defined in<br>
 * the static variable DEFAULT_FORMAT ("generic")<br>
 *<br>
 * @exception: Exception if a mandatory parameter is missing
 */
	public static void createCheck(boolean isCheckin,
									Parameters param) throws Exception {

		FPDMAppletCheckInOut.debug("CheckFactory.createCheck("+isCheckin+","+param.toString()+") START");

		StringBuffer hostSiteBuf = new StringBuffer("http://");
		hostSiteBuf.append(param.hostName);
		if (param.hostPort != null) {
			hostSiteBuf.append(":");
			hostSiteBuf.append(param.hostPort);
		}
		//String hostSite = hostSiteBuf.toString();

		if (isCheckin) {
			FPDMAppletCheckInOut.debug("CheckFactory.createCheck(...) END CHECKIN");
		}
		else {
			FPDMAppletCheckInOut.debug("CheckFactory.createCheck(...) END CHECKOUT");
		}
        return;
	}


}
