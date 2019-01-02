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

import org.apache.commons.lang3.StringUtils;

import com.dassault_systemes.platform.restServices.RestService;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.framework.ui.UINavigatorUtil;
import com.matrixone.json.JSONArray;
import com.matrixone.json.JSONObject;

import matrix.db.JPO;
import matrix.util.MatrixException;
import matrix.util.StringList;

@Path("/ExpandObject")
public class UM5ExpandObject extends RestService {

	
	@GET
	public Response getExpand(@Context HttpServletRequest request, @QueryParam("objectId") String objectId, @QueryParam("expandProgram") String expandProgram, @QueryParam("expandFunction") String expandFunction,@DefaultValue("") @QueryParam("expandParams") String expandParams, @DefaultValue("*") @QueryParam("expandTypes") String expandTypes, @DefaultValue("*") @QueryParam("expandRels") String expandRels, @DefaultValue("1") @QueryParam("expandLevel") String expandLevel, @DefaultValue("") @QueryParam("selects") String selects, @DefaultValue("") @QueryParam("relSelects") String relSelects, @DefaultValue("") @QueryParam("whereObj") String whereObj, @DefaultValue("") @QueryParam("whereRel") String whereRel){
		return expand(request, objectId, expandProgram, expandFunction, expandParams, expandTypes, expandRels, expandLevel, selects, relSelects, whereObj, whereRel);
	}
	
	@POST
	public Response postExpand(@Context HttpServletRequest request, @FormParam("objectId") String objectId, @FormParam("expandProgram") String expandProgram, @FormParam("expandFunction") String expandFunction,@DefaultValue("") @FormParam("expandParams") String expandParams, @DefaultValue("*") @FormParam("expandTypes") String expandTypes, @DefaultValue("*") @FormParam("expandRels") String expandRels, @DefaultValue("1") @FormParam("expandLevel") String expandLevel, @DefaultValue("") @FormParam("selects") String selects, @DefaultValue("") @FormParam("relSelects") String relSelects, @DefaultValue("") @FormParam("whereObj") String whereObj, @DefaultValue("") @FormParam("whereRel") String whereRel){
		return expand(request, objectId, expandProgram, expandFunction, expandParams, expandTypes, expandRels, expandLevel, selects, relSelects, whereObj, whereRel);
	}
	
	@SuppressWarnings("unchecked")
	public Response expand(HttpServletRequest request, String objectId, String expandProgram, String expandFunction, String expandParams, String expandTypes, String expandRels, String expandLevel, String selects, String relSelects, String whereObj, String whereRel){

		//Test sample : https://3dexp.16xfd04.ds/3DSpace/UM5Tools/ExpandObject?objectId=12928.22295.39204.32382&expandTypes=*&expandRels=*&selects=name
		
		JSONObject output=new JSONObject();

		matrix.db.Context context = null;
		
		try {
			output.put("msg", "KO");
			
			try {
				context = authenticate(request);
				
				ContextUtil.startTransaction(context, false);
				try{
					MapList mlObjs=null;
					
					StringList typeSl=new StringList();
					typeSl.add("id");
					typeSl.add("physicalid");
					typeSl.add("type");
					typeSl.add("current");
					typeSl.add("policy");
					
					ArrayList<String> jpoSelects=new ArrayList<String>();
					String[] arrSelects =selects.split(",");
					
					//System.out.println("selects="+selects);
					
					for (int i = 0; i < arrSelects.length; i++) {
						String select=arrSelects[i];
						if(select.startsWith("JPO-")){
							jpoSelects.add(select);
						}else{
							typeSl.add(select);
						}
					}
					
					StringList relSl=new StringList(relSelects.split(","));
					relSl.add("id[connection]");
					relSl.add("from.id");
					relSl.add("to.id");
					
					if(null != expandProgram && null!= expandFunction && !expandProgram.isEmpty() && ! expandFunction.isEmpty()){
						HashMap<String, String> mapArgs = new HashMap<String, String>();
						mapArgs.put("objectId", objectId);
						mapArgs.put("expandLevel", expandLevel);
						
						//System.out.println("expandParams="+expandParams);
						if(!expandParams.isEmpty()){
							String[] pairsParams = expandParams.split("&");
							for (int i = 0; i < pairsParams.length; i++) {
								String[] pair = pairsParams[i].split("=");
								if(pair.length >= 2){
									mapArgs.put(pair[0], pair[1]);
								}
							}
						}
						
						//System.out.println("Call JPO : "+expandProgram+"."+expandFunction);
						//System.out.println("mapArgs="+mapArgs);
						
						String[] args = JPO.packArgs(mapArgs);
						mlObjs = (MapList) JPO.invoke(context, expandProgram, args, expandFunction, args, MapList.class);
						
						// Filter empty maps + Add selects that are not in the expand program
						MapList mlRes=new MapList();
						
						for (Object obj : mlObjs) {
							Map<String, Object> mapObj=(Map<String, Object>) obj;
							String oidTest=(String) mapObj.get("id");
							String levelTest=(String) mapObj.get("level");
							//Make sure that there is an object id, filter objects for some expand program where last or first map is not an object
							if(null!=oidTest && !oidTest.isEmpty() && null != levelTest && levelTest.equals("1")){
								DomainObject dom=new DomainObject(oidTest);
								
								@SuppressWarnings("rawtypes")
								Map mapRes=dom.getInfo(context, typeSl);
								
								//Select for the rels
								String oidRelTest=(String) mapObj.get("id[connection]");
								if(null!=oidRelTest && !oidRelTest.isEmpty()){
									DomainRelationship dr=new DomainRelationship(oidRelTest);
									
									@SuppressWarnings("rawtypes")
									Map mapRel=dr.getRelationshipData(context, relSl);
									mapRes.putAll(mapRel);
								}
								
								mlRes.add(mapRes);
							}
						}
						
						mlObjs=mlRes;
					}else{
						DomainObject dom=new DomainObject(objectId);
						
						String[] relsInfos = expandRels.split(",");
						ArrayList<String> fromRels = new ArrayList<String>();
						ArrayList<String> toRels = new ArrayList<String>();
						for(int i=0; i < relsInfos.length; i++){
							String[] pairInfo = relsInfos[i].split("\\|");
							if(pairInfo.length >= 2){
								String direction = pairInfo[0];
								String relType = pairInfo[1];
								if(direction.equalsIgnoreCase("BOTH")){
									fromRels.add(relType);
									toRels.add(relType);
								}else if(direction.equalsIgnoreCase("TO")){
									toRels.add(relType);
								}else{
									fromRels.add(relType);
								}
							}else{
								fromRels.add(pairInfo[0]);
							}
						}
						short expLvl=1;
						try{
							expLvl = Short.parseShort(expandLevel);
						}catch (NumberFormatException nfe) {
							expLvl = 1;
						}
						if(fromRels.size()>0){
							String expRelsFrom = StringUtils.join(fromRels,",");
							mlObjs=dom.getRelatedObjects(context,
								expRelsFrom,
								expandTypes,
								typeSl,
								relSl,
								false,
								true,
								expLvl,
								whereObj,
								whereRel,
								0);
							for (int i=0; i < mlObjs.size(); i++) {
								Map<String, Object> mapObj=(Map<String, Object>) mlObjs.get(i);
								mapObj.put("relDirection", "from");
							}
						}
						if(toRels.size() > 0){
							String expRelsTo = StringUtils.join(toRels,",");
							MapList mlObjsTo=dom.getRelatedObjects(context,
								expRelsTo,
								expandTypes,
								typeSl,
								relSl,
								true,
								false,
								expLvl,
								whereObj,
								whereRel,
								0);
							for (int i=0; i < mlObjsTo.size(); i++) {
								Map<String, Object> mapObj=(Map<String, Object>) mlObjsTo.get(i);
								mapObj.put("relDirection", "to");
							}
							mlObjs.addAll(mlObjsTo);
						}
					}
					
					JSONArray outArr=new JSONArray();
					
					Map<String,Vector<String>> mapJPORes=new HashMap<String,Vector<String>>();
					for (String selectJPO : jpoSelects) {
						String[] selElems=selectJPO.split("-");
						if(selElems.length>=3){
							String prog=selElems[1];
							String funct=selElems[2];
							
							HashMap<String, Object> mapArgs2 = new HashMap<String, Object>();
							mapArgs2.put("objectList", mlObjs);
							
							String[] args2 = JPO.packArgs(mapArgs2);
							
							Vector<String> vRes = (Vector<String>) JPO.invoke(context, prog, args2, funct, args2, Vector.class);
							mapJPORes.put(selectJPO, vRes);
						}
					}
					
					for (int i=0; i < mlObjs.size(); i++) {
						Map<String, Object> mapObj=(Map<String, Object>) mlObjs.get(i);
						
						//Add Specific JPO selects
						for (String selectJPO : jpoSelects) {
							//System.out.println("selectJPO="+selectJPO);
							String val=mapJPORes.get(selectJPO).get(i);
							mapObj.put(selectJPO, val);
						}
						
						//Add NLS + Manage multi-values results
						for (Object keySelect : typeSl) {
							String strKey = (String) keySelect;
							
							// Multi-values results
							Object objVal = mapObj.get(strKey);
							if(objVal instanceof StringList){
								String valAsString = "";
								valAsString = StringUtils.join((StringList) objVal, "");//[BEL] char same as when a find is done
								mapObj.put(strKey, valAsString);
							}
							
							//NLS
							try{
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
									if(!nlsType.isEmpty()){
										String strValueNLS = EnoviaResourceBundle.getFrameworkStringResourceProperty(context, "emxFramework."+nlsType+"."+nlsInfo , request.getLocale());
										
										if(null != strValueNLS && !strValueNLS.equals("") && !strValueNLS.startsWith("emxFramework.")){
											mapObj.put("nls!"+strKey, strValueNLS);
										}
									}
								}
							}catch(Exception ex){
								//Silent catch
							}
						}
					
						JSONObject jsonObj=new JSONObject(mapObj);
						
						String type=(String) mapObj.get("type");
						if(null!=type && !type.isEmpty()){
							String icon = UINavigatorUtil.getTypeIconProperty(context, type);
							jsonObj.put("iconType", "/common/images/"+icon);
						}
						outArr.put(jsonObj);
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
