package pss.common.utils;

import java.io.ByteArrayInputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.Context;
import matrix.db.Page;
import matrix.util.MatrixException;
import matrix.util.StringList;

public class TigerAppConfigProperties extends EnoviaResourceBundle {

	private static final Logger log = LoggerFactory.getLogger(TigerAppConfigProperties.class);
	private static final String TIGER_APP_CONFIG_FILES = "PSS_TitleBlockConfig_FASFAE.properties,PSS_TitleBlockConfig_RENAULTNISSAN.properties,PSS_TitleBlockConfig_BASISDEFINITION.properties";
	protected static Properties _TIGERCONFIGPROPERTIES = null;
	
	public static void init(Context context) throws Exception {
		log.trace("::::::: Loaging Properties from "+TIGER_APP_CONFIG_FILES+"....");
		try{
			_TIGERCONFIGPROPERTIES = new Properties();
			StringList slProperites = FrameworkUtil.split(TIGER_APP_CONFIG_FILES, ",");
			int isize = slProperites.size();
			for (int i = 0; i < isize; i++ ) {
				Page localPage = new Page((String) slProperites.get(i));
				localPage.open(context);
				byte[] arrayOfByte = localPage.getContents(context).getBytes();
				if (arrayOfByte != null)
					_TIGERCONFIGPROPERTIES.load(new ByteArrayInputStream(arrayOfByte));
				localPage.close(context);
			}
		} catch(MatrixException me) {
			me.printStackTrace();
			log.error(me.getLocalizedMessage());
			throw new Exception(me);
		}
		log.trace("::::::: Successfully loaded from "+TIGER_APP_CONFIG_FILES+"....");
	}
	
	public static String getPropertyValue(Context context, String strKey) throws Exception {
		log.trace(":::::: ENTER :: getPropertyValue :::::::");
		String strValue = DomainConstants.EMPTY_STRING;
		try{
			if(_TIGERCONFIGPROPERTIES == null || _TIGERCONFIGPROPERTIES.isEmpty()){
				init(context);
			}
			
			if(UIUtil.isNotNullAndNotEmpty(strKey)){
				strValue = _TIGERCONFIGPROPERTIES.getProperty(strKey);
		/*		if(UIUtil.isNullOrEmpty(strValue)){
					strValue = checkPageModifiedAndGetValue(context, strKey);
				}*/
				if(UIUtil.isNullOrEmpty(strValue)){
					try{
						strValue = getProperty(context, strKey);
					} catch(FrameworkException fe){
						// As we are handling the error, log only if trace enabled
						log.trace(fe.getLocalizedMessage());
						strValue = DomainConstants.EMPTY_STRING;
					}
				}
			} else{
				strValue = DomainConstants.EMPTY_STRING;
			}
		} catch(Exception e){
			e.printStackTrace();
			String strMsg = ":::::: Problem in Method :: getPropertyValue :: "+e.getMessage();
			log.error(strMsg, e);
			throw new Exception(strMsg, e);
		}
		log.trace(":::::: EXIT :: getPropertyValue :::::::");
		return strValue;
	}
	
	/**
	 * To avoid restart, private call to get the updated values. Will be only called is the property value is empty
	 * @param context
	 * @param strKey
	 * @return
	 * @throws Exception
	 */
	/*private static String checkPageModifiedAndGetValue(Context context, String strKey) throws Exception {
		log.trace(":::::: ENTER :: checkPageModifiedAndGetValue :::::::");
		String strValue = DomainConstants.EMPTY_STRING;
		try {
			init(context);
			strValue = _TIGERCONFIGPROPERTIES.getProperty(strKey);
		} catch (Exception e) {
			e.printStackTrace();
			String strMsg = ":::::: Problem in Method :: checkPageModifiedAndGetValue :: "+e.getMessage();
			log.error(strMsg, e);
			throw new Exception(strMsg, e);
		}
		log.trace(":::::: EXIT :: checkPageModifiedAndGetValue :::::::");
		return strValue;
	}*/
	
}
