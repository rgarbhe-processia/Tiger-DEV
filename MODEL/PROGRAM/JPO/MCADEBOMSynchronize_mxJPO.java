
/*
 ** MCADEBOMSynchronize
 **
 ** Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 **
 ** JPO for performing EBOM synchronization.
 */

import java.util.Vector;
import matrix.util.StringList;

import matrix.db.Context;
import matrix.db.Relationship;
import com.dassault_systemes.vplmintegration.util.config.VPLMIntegConfigEntityServicesUtil;
import com.dassault_systemes.vplmintegration.sdk.enovia.VPLMBusConnection;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.DomainObject;

public class MCADEBOMSynchronize_mxJPO extends MCADEBOMSynchronizeBase_mxJPO {
    /**
     * Constructor.
     * @since Sourcing V6R2008-2
     */
    public MCADEBOMSynchronize_mxJPO() {
        super();
    }

    /**
     * Constructor.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @throws Exception
     *             if the operation fails
     * @since Sourcing V6R2008-2
     */
    public MCADEBOMSynchronize_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
    }

    // Delete the parts which are in asStoredChildIdVector, but NOT in latestChildIdVector
    // Delete their EBOM link with parentPartObdId
    // Note: latestChildIdVector can be null also
    protected void deleteUnWantedParts(Context _context, String parentPartObdId, Vector latestChildIdVector, Vector asStoredChildIdVector) throws Exception {
        System.out.println("******* deleteUnWantedParts ***********");
        StringList busSelect = new StringList(1);
        String newChildObjid = "";
        String childObjid = "";
        String notesNew = "";
        String notesOld = "";
        if (parentPartObdId != null && parentPartObdId.length() > 0) {
            for (int i = 0; i < asStoredChildIdVector.size(); i++) {
                childObjid = (String) asStoredChildIdVector.elementAt(i);
                if (!childObjid.equals("") && (latestChildIdVector == null || !latestChildIdVector.contains(childObjid))) {
                    if (_util.doesRelationExist(_context, childObjid, parentPartObdId, relEBOM)) {
                        // Read Effectivity
                        DomainObject obj = DomainObject.newInstance(_context, childObjid);
                        String oldRelId = obj.getInfo(_context, "to[" + relEBOM + "].id");
                        System.out.println("oldRelId = " + oldRelId);
                        DomainRelationship relOld = new DomainRelationship(oldRelId);
                        notesOld = relOld.getAttributeValue(_context, "Reference Designator");
                        System.out.println("notes old: " + notesOld);
                        for (int j = 0; j < latestChildIdVector.size(); j++) {
                            newChildObjid = (String) latestChildIdVector.elementAt(j);
                            DomainObject objNew = DomainObject.newInstance(_context, newChildObjid);

                            // busSelect.add("from["+relEBOM+"]attribute_ReferenceDesignator");
                            String newRelId = objNew.getInfo(_context, "to[" + relEBOM + "].id");
                            System.out.println("newRelId: " + newRelId);
                            DomainRelationship rel = new DomainRelationship(newRelId);
                            notesNew = rel.getAttributeValue(_context, "Reference Designator");
                            System.out.println("notes new: " + notesNew);
                            if (notesNew.equalsIgnoreCase(notesOld)) {
                                System.out.println("There is a Part in last Synchro");
                                Relationship oldRelEBOM = new Relationship(oldRelId);
                                String sEff = VPLMIntegConfigEntityServicesUtil.getEffectivityInformationForECC(_context, new VPLMBusConnection(_context, oldRelEBOM));
                                System.out.println("Effectivity old: " + sEff);
                                if ((null != sEff) && (!(sEff.isEmpty()))) {
                                    System.out.println("There is an effectivity to add");
                                    // Relationship newRelEBOM = new Relationship(newRelId);
                                    VPLMIntegConfigEntityServicesUtil.seteffectivityExpressionOnEBOM(_context, new VPLMBusConnection(_context, newRelId), sEff);
                                    break;
                                }
                            }
                        }

                        System.out.println("EBOM REL EXISTS, childObjid = " + childObjid);
                        String Args[] = new String[5];
                        Args[0] = parentPartObdId;
                        Args[1] = "relationship";
                        Args[2] = "EBOM";
                        Args[3] = "to";
                        Args[4] = childObjid;
                        _util.executeMQL(_context, "disconnect bus $1 $2 $3 $4 $5", Args);

                        EBOMRelIsRemovedPartObjIds += childObjid;
                    }
                }
            }
        }
    }
}