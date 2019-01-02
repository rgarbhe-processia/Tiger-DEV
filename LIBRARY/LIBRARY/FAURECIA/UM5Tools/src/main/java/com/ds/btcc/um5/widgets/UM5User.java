package com.ds.btcc.um5.widgets;

import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.dassault_systemes.platform.restServices.RestService;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.json.JSONArray;
import com.matrixone.json.JSONObject;

import matrix.util.MatrixException;

@Path("/UserInfo")
public class UM5User extends RestService {

	@GET
	@Path("/getSecurityContexts")
	public Response getSecurityContexts(@Context HttpServletRequest request){
		
		JSONObject output=new JSONObject();

		matrix.db.Context context = null;
		
		try {
			output.put("msg", "KO");
			
			try {
				context = authenticate(request);
				
				JSONArray outArr=new JSONArray();
				
				@SuppressWarnings("unchecked")
				Vector<String> resSCs=PersonUtil.getSecurityContextAssignments(context);
				//System.out.println("SC : "+resSCs);
				
				String defaultSC=PersonUtil.getDefaultSecurityContext(context);
				outArr.put(defaultSC);//Put the default Security Context
				
				for(String strSC : resSCs){
					if(!strSC.equals(defaultSC)){
						outArr.put(strSC);//Then put the other Security Contexts
					}
				}
				
				output.put("msg", "OK");
				output.put("data", outArr);
				
			} catch (Exception e) {
				output.put("msg", e.getMessage());
				e.printStackTrace();
			}
		} catch (MatrixException e1) {
			e1.printStackTrace();
		}
		
		return Response.status(HttpServletResponse.SC_OK).entity(output.toString()).build();
	}
	
	@GET
	@Path("/getName")
	public Response getName(@Context HttpServletRequest request){
		
		JSONObject output=new JSONObject();

		matrix.db.Context context = null;
		
		try {
			output.put("msg", "KO");
			
			try {
				context = authenticate(request);
				
				JSONArray outArr=new JSONArray();
				
				String fullName = PersonUtil.getFullName(context);
				
				outArr.put(fullName);
				
				output.put("msg", "OK");
				output.put("data", outArr);
				
			} catch (Exception e) {
				output.put("msg", e.getMessage());
				e.printStackTrace();
			}
		} catch (MatrixException e1) {
			e1.printStackTrace();
		}
		
		return Response.status(HttpServletResponse.SC_OK).entity(output.toString()).build();
	}
}
