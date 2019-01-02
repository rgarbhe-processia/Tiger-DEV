package faurecia.efcs.encryption;

import com.matrixone.fcs.fcs.FcsContext;
import com.matrixone.fcs.fcs.FileProtocol;
import com.matrixone.fcs.fcs.Location;

/**
 * Not used, but very hard to find documentation about it,
 * so it's here in case we need it.
 * 
 * @author 1charlej
 */
public class EncryptedFileProtocol extends FileProtocol {

    public EncryptedFileProtocol(FcsContext paramFcsContext, Location paramLocation) {
        super(paramFcsContext, paramLocation);
    }

    public boolean filesizeStoredInDB() {
        return false;
    }
}
