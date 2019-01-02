package com.ds.btcc.um5.widgets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

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
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.framework.ui.UINavigatorUtil;
import com.matrixone.json.JSONArray;
import com.matrixone.json.JSONObject;

import matrix.db.JPO;
import matrix.util.MatrixException;
import matrix.util.StringList;

@Path("/ObjectInfo")
public class UM5ObjectInfos extends RestService {

	@GET
	public Response getInfos(@Context HttpServletRequest request, @QueryParam("action") String action, @QueryParam("objectIds") String oids, @DefaultValue("") @QueryParam("selects") String selects){
		return infos(request, action, oids, selects);
	}
	
	@POST
	public Response postInfos(@Context HttpServletRequest request, @FormParam("action") String action, @FormParam("objectIds") String oids, @DefaultValue("") @FormParam("selects") String selects){
		return infos(request, action, oids, selects);
	}
	
	public Response infos(HttpServletRequest request, String action, String oids, String selects){

		JSONObject output=new JSONObject();

		matrix.db.Context context = null;

		try {
			output.put("msg", "KO");

			try {
				context = authenticate(request);

				ContextUtil.startTransaction(context, false);
				try{
					
					JSONArray outArr=new JSONArray();
	
					if(action.equals("getInfos")){
						if(null!= oids && !oids.isEmpty()){
							//System.out.println("oids="+oids);
							String[] arrOids=oids.split(",");
	
							StringList typeSl=new StringList();
							typeSl.add("id");
							typeSl.add("physicalid");
							typeSl.add("type");
							typeSl.add("current");
							typeSl.add("policy");
							
							ArrayList<String> jpoSelects=new ArrayList<String>();
							String[] arrSelects =selects.split(",");
							for (int i = 0; i < arrSelects.length; i++) {
								String select=arrSelects[i];
								if(select.startsWith("JPO-")){
									jpoSelects.add(select);
								}else{
									typeSl.add(select);
								}
							}
	
							MapList mlRes=DomainObject.getInfo(context, arrOids, typeSl);
							
							Map<String,Vector<String>> mapJPORes=new HashMap<String,Vector<String>>();
							for (String selectJPO : jpoSelects) {
								String[] selElems=selectJPO.split("-");
								if(selElems.length>=3){
									String prog=selElems[1];
									String funct=selElems[2];
									
									HashMap<String, Object> mapArgs = new HashMap<String, Object>();
									mapArgs.put("objectList", mlRes);
									
									String[] args = JPO.packArgs(mapArgs);
									@SuppressWarnings("unchecked")
									Vector<String> vRes = (Vector<String>) JPO.invoke(context, prog, args, funct, args, Vector.class);
									mapJPORes.put(selectJPO, vRes);
								}
							}
							
							for (int i=0; i < mlRes.size(); i++) {
								@SuppressWarnings("unchecked")
								Map<String, Object> mapObj=(Map<String, Object>) mlRes.get(i);
								
								//Add Specific JPO selects
								for (String selectJPO : jpoSelects) {
									Vector<String> vecJPOs=mapJPORes.get(selectJPO);
									if(null != vecJPOs){
										String val=vecJPOs.get(i);
										mapObj.put(selectJPO, val);
									}
								}
								
								//Add NLS
								for (Object keySelect : typeSl) {
									try{
										String strKey = (String) keySelect;
										String value = (String) mapObj.get(strKey);
										if(null != value){
											String nlsType = "";
											String nlsInfo ="";
											if(strKey.equals("type")){
												nlsType = "Type";
												nlsInfo=value.trim().replaceAll(" ", "_");
											}else if(strKey.equals("current")){
												nlsType = "State";
												
												String strPolicy = (String) mapObj.get("policy");
												strPolicy = strPolicy.replaceAll(" ", "_");
												
												nlsInfo = strPolicy+"."+value.replaceAll(" ", "_");
											}
											
											String strValueNLS = EnoviaResourceBundle.getFrameworkStringResourceProperty(context, "emxFramework."+nlsType+"."+nlsInfo , request.getLocale());
											
											if(null != strValueNLS && !strValueNLS.equals("") && !strValueNLS.startsWith("emxFramework.")){
												mapObj.put("nls!"+strKey, strValueNLS);
											}
										}
									}catch(Exception ex){
										//Silent catch
									}
								}
								
								
								JSONObject jsonObj=new JSONObject(mapObj);
								
								String type=(String)mapObj.get("type");
								if(null!=type && !type.isEmpty()){
									String icon = UINavigatorUtil.getTypeIconProperty(context, type);
									jsonObj.put("iconType", "/common/images/"+icon);
								}
								outArr.put(jsonObj);
							}
							
							
						}
					}else{
						throw new Exception("Action not supported by Web Service UserInfo");
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
