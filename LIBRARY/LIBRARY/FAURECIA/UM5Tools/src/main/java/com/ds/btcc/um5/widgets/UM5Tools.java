package com.ds.btcc.um5.widgets;

import javax.ws.rs.ApplicationPath;

import com.dassault_systemes.platform.restServices.ModelerBase;

@ApplicationPath("/UM5Tools")
public class UM5Tools extends ModelerBase {

	@Override
	public Class<?>[] getServices() {
		return new Class<?>[] { UM5FindObjects.class, UM5User.class, UM5ExpandObject.class, UM5ObjectInfos.class, UM5WdgConfig.class, UM5RelationshipInfos.class };
	}

}
