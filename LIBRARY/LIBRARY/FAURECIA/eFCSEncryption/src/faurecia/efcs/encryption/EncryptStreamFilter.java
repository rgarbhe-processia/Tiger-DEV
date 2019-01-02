package faurecia.efcs.encryption;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;

import com.matrixone.fcs.common.FcsException;
import com.matrixone.fcs.common.Logger;
import com.matrixone.fcs.fcs.FCSStreamFilter;
import com.matrixone.fcs.fcs.FcsContext;
import com.matrixone.fcs.fcs.Location;
import com.matrixone.jdom.Element;

/**
 * The following mql command needs to be run:
 * 
 * mod store STORE checksumoff filter faurecia.efcs.encryption.EncryptStreamFilter file params.xml;
 * 
 * @author 1charlej
 */
public class EncryptStreamFilter implements FCSStreamFilter {

    /**
     * If the JAR is created with this parameter set to `false`, FCS using this JAR will not decrypt/encrypt files
     */
    protected boolean isEncryption = true;

    protected String key = null;
    protected static final String defaultSalt = "87uWIjMil-ObiWanKenobi-D5Ww3a0Ygw9";

    protected boolean shouldEncryptOrDecrypt(FcsContext paramFcsContext) {
        String filename = paramFcsContext.getCurrentItem().getFileName();
        if (!isEncryption) return false;
        if (filename == null || filename.isEmpty()) return false; // not a checkin, not a checkout (ping, synchro, etc.)
        return true;
    }

    @Override
    public InputStream getCheckoutStream(FcsContext paramFcsContext, Location paramLocation, InputStream paramInputStream) throws Exception {
        if (!this.shouldEncryptOrDecrypt(paramFcsContext)) {
            Logger.log("EncryptStreamFilter - getCheckoutStream[off] - " + paramLocation.getName(), Logger.DEBUG);
            return paramInputStream;
        } else {
        	FCSEncryption encryptionProvider = new FCSEncryption(this.key, defaultSalt);
            Logger.log("EncryptStreamFilter - getCheckoutStream[on] - " + paramLocation.getName(), Logger.DEBUG);
            return encryptionProvider.decrypt(paramInputStream);
        }
    }

    @Override
    public OutputStream getCheckinStream(FcsContext paramFcsContext, Location paramLocation, OutputStream paramOutputStream) throws FcsException {
        FCSEncryption encryptionProvider;
        if (!this.shouldEncryptOrDecrypt(paramFcsContext)) {
            Logger.log("EncryptStreamFilter - getCheckinStream[off] - " + paramLocation.getName(), Logger.DEBUG);
            return paramOutputStream;
        } else {
            Logger.log("EncryptStreamFilter - getCheckinStream[on] - " + paramLocation.getName(), Logger.DEBUG);
            try {
            	encryptionProvider = new FCSEncryption(this.key, defaultSalt);
                return encryptionProvider.encrypt(paramOutputStream);
            } catch (GeneralSecurityException e) {
                Logger.log(e);
                throw new FcsException(e);
            }
        }
    }

    @Override
    public void init(Location paramLocation, Element paramElement) {
        try {
            this.key = paramElement.getChild("encryption").getChildText("key");
        } catch (NullPointerException e) {
            Logger.log(e);
            this.isEncryption = false;
        }
        if (!isEncryption) {
            Logger.log("EncryptStreamFilter - init[off]", Logger.DEBUG);
        } else {
            Logger.log("EncryptStreamFilter - init[on]", Logger.DEBUG);
        }
    }
}
