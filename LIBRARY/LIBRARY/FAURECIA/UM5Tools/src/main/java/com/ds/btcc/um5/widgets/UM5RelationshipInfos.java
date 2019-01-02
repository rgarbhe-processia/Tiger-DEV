package com.ds.btcc.um5.widgets;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.dassault_systemes.platform.restServices.RestService;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.json.JSONArray;
import com.matrixone.json.JSONObject;

import matrix.util.MatrixException;
import matrix.util.StringList;

@Path("/RelationshipInfo")
public class UM5RelationshipInfos extends RestService {

	@GET
	public Response getInfos(@Context HttpServletRequest request,@QueryParam("relIds") String relIds, @DefaultValue("") @QueryParam("selects") String selects){
		return infos(request, relIds, selects);
	}
	
	@POST
	public Response postInfos(@Context HttpServletRequest request, @FormParam("relIds") String relIds, @DefaultValue("") @FormParam("selects") String selects){
		return infos(request, relIds, selects);
	}
	
	public Response infos(HttpServletRequest request, String relIds, String selects){
		
		JSONObject output=new JSONObject();

		matrix.db.Context context = null;

		try {
			output.put("msg", "KO");

			try {
				context = authenticate(request);

				ContextUtil.startTransaction(context, false);
				try{
					
					JSONArray outArr=new JSONArray();
					if(null!= relIds && !relIds.isEmpty()){
						//System.out.println("oids="+oids);
						String[] arrRelIds=relIds.split(",");

						StringList relSl=new StringList();
						relSl.add("id[connection]");
						relSl.add("name[connection]");
						relSl.add("from.id");
						relSl.add("to.id");
						
						//ArrayList<String> jpoSelects=new ArrayList<String>();
						String[] arrSelects =selects.split(",");
						for (int i = 0; i < arrSelects.length; i++) {
							String select=arrSelects[i];
							if(select.startsWith("JPO-")){
								//jpoSelects.add(select);
							}else{
								relSl.add(select);
							}
						}

						MapList mlRes=DomainRelationship.getInfo(context, arrRelIds, relSl);
						
						for (int i=0; i < mlRes.size(); i++) {
							@SuppressWarnings("unchecked")
							Map<String, Object> mapRel=(Map<String, Object>) mlRes.get(i);
							
							JSONObject jsonObj=new JSONObject(mapRel);
							
							outArr.put(jsonObj);
						}
						
					}
	
					output.put("msg", "OK");
					output.put("data", outArr);
					
					ContextUtil.commitTransaction(context);
				}catch(Exception e){
					ContextUtil.abortTransaction(context);
					throw e;
				}
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
