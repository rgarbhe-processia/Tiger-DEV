package fpdm.cad;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PropertyUtil;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.util.StringList;

public class TitleblockCADFiles_mxJPO {
    private static Logger logger = LoggerFactory.getLogger("fpdm.cad.TitleblockCADFiles");

    /**
     * Get all linked Derived Output files information of LST and CAD files<br>
     * @plm.usage JSP: FPDM_CADDrawingGenerateCompleteTitleBlock.jsp
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            CAD Drawing ID
     * @return
     * @throws Exception
     */
    @SuppressWarnings({ "unchecked", "deprecation" })
    public MapList getFiles(Context context, String[] args) throws Exception {
        MapList mlTitleBlockCADFilesList = new MapList();
        try {
            String sCADDrawingId = args[0];
            logger.debug("getFiles() - sCADDrawingId = <" + sCADDrawingId + ">");

            // get linked neutral files
            StringList slSelect = new StringList();
            slSelect.addElement(DomainConstants.SELECT_ID);
            slSelect.addElement(DomainConstants.SELECT_TYPE);
            slSelect.addElement(DomainConstants.SELECT_NAME);
            slSelect.addElement(DomainConstants.SELECT_REVISION);
            slSelect.addElement(DomainConstants.SELECT_FILE_NAME);
            slSelect.addElement(DomainConstants.SELECT_FILE_FORMAT);

            String sRelatioshipPattern = PropertyUtil.getSchemaProperty(context, "relationship_DerivedOutput");
            String sTypePattern = PropertyUtil.getSchemaProperty(context, "type_DerivedOutput");

            DomainObject doCADObject = DomainObject.newInstance(context, sCADDrawingId);

            // get related Derived Output objects
            MapList mlDerivedObjects = doCADObject.getRelatedObjects(context, sRelatioshipPattern, sTypePattern, slSelect, null, false, true, (short) 1, null, null, 0);
            logger.debug("getFiles() - sCADDrawingId = <" + sCADDrawingId + "> mlDerivedObjects = <" + mlDerivedObjects + ">");

            if (mlDerivedObjects.size() > 0) {
                Map<?, ?> mDerivedObjectInfos = null;
                String sID = null;
                ArrayList<String> slFileName = null;
                ArrayList<String> slFileFormat = null;
                String sFileName = null;
                String sFileFormat = null;
                HashMap<String, Object> mNeutralFileInfos = null;

                for (Iterator<?> iterator = mlDerivedObjects.iterator(); iterator.hasNext();) {
                    mDerivedObjectInfos = (Map<?, ?>) iterator.next();
                    logger.debug("getFiles() - mDerivedObjectInfos = <" + mDerivedObjectInfos + ">");

                    sID = (String) mDerivedObjectInfos.get(DomainConstants.SELECT_ID);
                    slFileName = fpdm.utils.SelectData_mxJPO.getListOfValues(mDerivedObjectInfos.get(DomainConstants.SELECT_FILE_NAME));
                    slFileFormat = fpdm.utils.SelectData_mxJPO.getListOfValues(mDerivedObjectInfos.get(DomainConstants.SELECT_FILE_FORMAT));

                    BusinessObject bo = new BusinessObject(sID);
                    String sCheckoutDir = context.createWorkspace();
                    logger.debug("getFiles() - sCheckoutDir = <" + sCheckoutDir + ">");

                    for (int i = 0; i < slFileName.size(); i++) {
                        sFileName = slFileName.get(i);

                        if (sFileName.endsWith(".cad") || sFileName.endsWith(".lst")) {
                            sFileFormat = slFileFormat.get(i);
                            mNeutralFileInfos = new HashMap<String, Object>();
                            mNeutralFileInfos.put("id", sID);
                            mNeutralFileInfos.put("FileName", sFileName);
                            mNeutralFileInfos.put("format", sFileFormat);

                            bo.checkoutFile(context, false, sFileFormat, sFileName, sCheckoutDir);
                            mNeutralFileInfos.put("file", readFileToBytes(sCheckoutDir + "/" + sFileName));

                            mlTitleBlockCADFilesList.add(mNeutralFileInfos);
                        }
                    }
                }
            }
            logger.debug("getFiles() - mlTitleBlockCADFilesList = <" + mlTitleBlockCADFilesList + ">");

        } catch (RuntimeException e) {
            logger.error("Error in getFiles()\n", e);
            throw e;
        } catch (Exception e) {
            logger.error("Error in getFiles()\n", e);
            throw e;
        }

        return mlTitleBlockCADFilesList;
    }

    byte[] readFileToBytes(String aInputFileName) {
        logger.debug("Reading in binary file named : " + aInputFileName);
        File file = new File(aInputFileName);

        logger.debug("File size: " + file.length());
        byte[] result = new byte[(int) file.length()];
        try {
            InputStream input = null;
            try {
                int totalBytesRead = 0;
                input = new BufferedInputStream(new FileInputStream(file));
                while (totalBytesRead < result.length) {
                    int bytesRemaining = result.length - totalBytesRead;
                    // input.read() returns -1, 0, or more :
                    int bytesRead = input.read(result, totalBytesRead, bytesRemaining);
                    if (bytesRead > 0) {
                        totalBytesRead = totalBytesRead + bytesRead;
                    }
                }
                /*
                 * the above style is a bit tricky: it places bytes into the 'result' array; 'result' is an output parameter; the while loop usually has a single iteration only.
                 */
                logger.debug("Num bytes read: " + totalBytesRead);
            } finally {
                logger.debug("Closing input stream.");
                input.close();
            }
        } catch (FileNotFoundException ex) {
            logger.error("File not found.", ex);
        } catch (IOException ex) {
            logger.error("IO", ex);
        }
        return result;
    }
}
