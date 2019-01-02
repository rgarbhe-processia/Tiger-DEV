package com.ds.btcc.um5.widgets;

import java.util.Map;
import java.util.Set;

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
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.json.JSONArray;
import com.matrixone.json.JSONObject;

import matrix.db.Page;
import matrix.util.MatrixException;

@Path("/WdgConfig")
public class UM5WdgConfig extends RestService {

	@GET
	public Response getConfig(@Context HttpServletRequest request, @DefaultValue("") @QueryParam("action") String action, @QueryParam("wdg") String wdg, @QueryParam("configName") String configName, @QueryParam("configContent") String configContent){
		return config(request, action, wdg, configName, configContent);
	}
	@POST
	public Response postConfig(@Context HttpServletRequest request, @DefaultValue("") @FormParam("action") String action, @FormParam("wdg") String wdg, @FormParam("configName") String configName, @FormParam("configContent") String configContent){
		return config(request, action, wdg, configName, configContent);
	}
	
	@SuppressWarnings("deprecation")
	public Response config(HttpServletRequest request, String action, String wdg, String configName, String configContent){
		JSONObject output=new JSONObject();
		
		
		matrix.db.Context context = null;
		
		try {
			output.put("msg", "KO");
			
			try {
				context = authenticate(request);
				
				if(null == wdg || wdg.isEmpty()){
					throw new Exception("Web Service WdgConfig : need a widget name (wdg)");
				}
				
				//Check page existence and create if needed
				String pageName=wdg+"WdgConf";
				Page pageConf=new Page(pageName);
				if(!pageConf.exists(context)){
					ContextUtil.pushContext(context);
					pageConf=Page.create(context, pageName, "Widget Configuration Page by UM5.", "", "[]");
					ContextUtil.popContext(context);
				}
				
				pageConf.open(context);
				JSONArray configsArr=new JSONArray(pageConf.getContents(context));
				
				JSONArray outArr=new JSONArray();
				
				if(action.equals("getConfigsList")){
					for(int i=0; i<configsArr.length(); i++){
						JSONObject jsonObj = configsArr.getJSONObject(i);
						String confName = jsonObj.getString("name");
						outArr.put(confName);
					}
				}else if(action.equals("getConfig")){
					for(int i=0; i<configsArr.length(); i++){
						JSONObject jsonObj = configsArr.getJSONObject(i);
						String confName = jsonObj.getString("name");
						if(confName.equals(configName)){
							outArr.put(jsonObj);
							break;
						}
					}
					
				}else if(action.equals("saveConfig")){
					boolean saveDone=false;
					
					JSONObject jsonConfigContent=new JSONObject();
					
					Map<String, String[]> reqMap=request.getParameterMap();
					Set<String> keySet=reqMap.keySet();
					for (String keyParam : keySet) {
						if(keyParam.startsWith("configContent")){
							String jsonParam=keyParam.substring(keyParam.indexOf("[")+1, keyParam.lastIndexOf("]"));
							String jsonValue=reqMap.get(keyParam)[0];
							jsonConfigContent.put(jsonParam, jsonValue);
						}
					}
					
					for(int i=0; i<configsArr.length(); i++){
						JSONObject jsonObj = configsArr.getJSONObject(i);
						String confName = jsonObj.getString("name");
						if(confName.equals(configName)){
							JSONObject newJsonObj = new JSONObject();
							newJsonObj.put("name", configName);
							newJsonObj.put("prefs", jsonConfigContent);
							configsArr.put(i, newJsonObj);
							saveDone=true;
							break;
						}
					}
					if(!saveDone){
						JSONObject jsonObj = new JSONObject();
						jsonObj.put("name", configName);
						jsonObj.put("prefs", jsonConfigContent);
						configsArr.put(jsonObj);
					}

					pageConf.setContents(context, configsArr.toString());
					pageConf.update(context);
				}else{
					throw new Exception("Action not supported by Web Service WdgConfig");
				}
				pageConf.close(context);
				
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
