/**
 * 
 */
package com.naren.openstack.client.executer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import com.google.gson.GsonBuilder;
import com.naren.openstack.dataobjects.rest.Credentials;
import com.naren.openstack.dataobjects.rest.Endpoint;
import com.naren.openstack.dataobjects.rest.Service;

/**
 * @author narender
 *
 */
public class OpenStackClient {

	/**
	 * 
	 */
	public OpenStackClient() {
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new OpenStackClient().execute();

	}
	/**
	 * Start point of program
	 */
	public void execute() {
		String region="RegionOne";
		String module="compute";
		String serverId="681ca874-0f35-4a86-b7ab-3836ffdadcdc";//P1 Server /"7c28c337-e60d-46a2-a020-8ef9d9560121";
		Map<String ,String> context=new HashMap<String,String>();
		context.put("TENANT_ID","admin");
		context.put("USER_NAME","engguser");
		context.put("PASSWORD","test123");
		context.put("REGION","RegionOne");
		context.put("ACCOUNT_METADATA","{username=engguser, password=test123}");
		context.put("END_POINT_URLS","http://172.18.11.210:5000/v2.0/");
		Credentials credentials=null;
		try {
			credentials= authorize(context);
		} catch (Exception e) {
			e.printStackTrace();
		}
		//Get Server Detail
		String serverResponse=getServerDetail(credentials,context,region,module,serverId);
		System.out.println("Server Detail : "+serverResponse);/*
		String serverList=getServerlist(credentials,context,region,module);
		System.out.println("ServerList : "+serverList);*/
		//String listFlavour=getFlavourList(credentials,context,region,module);
		//	System.out.println(listFlavour);
		/*resizeInstance(credentials,context,region,module,serverId,String.valueOf(2));
		String serverResponse1=getServerDetail(credentials,context,region,module,serverId);
		System.out.println("Server Detail : "+serverResponse1);*/
		//confirmResizeInstance(credentials,context,region,module,serverId);
		String serverResponse11=getServerDetail(credentials,context,region,module,serverId);
		System.out.println("Server Detail : "+serverResponse11);
		//	String imageResponse=createImage(credentials,context,region,module,serverId,"P1Image","nstanwar");
		//System.out.println(imageResponse);
	}
	/**
	 * This method is use authorize 
	 * @param context
	 * @return Credentials
	 * @throws Exception
	 */
	public Credentials authorize(Map<String ,String> context) throws Exception {
		Credentials credentials = null;
		try {
			String tenantId = context.get("TENANT_ID");
			String identity = context.get("USER_NAME");
			String password = context.get("PASSWORD");
			String endPoint = context.get("END_POINT_URLS");
			HttpClient client = new DefaultHttpClient();
			String credentialsStr = "{\"auth\":{\"passwordCredentials\":{\"username\":\""+identity+"\",\"password\":\""+password+"\" },\"tenantName\":\""+tenantId+"\"}}";
			StringEntity inputEntity = new StringEntity(credentialsStr);
			inputEntity.setContentType("application/json");
			HttpPost postRequest = new HttpPost(endPoint.concat("tokens"));
			postRequest.setEntity(inputEntity);
			setCustomHeaders(postRequest, "authentication", context);
			HttpResponse postResponse = client.execute(postRequest);
			BufferedReader rd = new BufferedReader(new InputStreamReader(postResponse.getEntity().getContent()));
			String line = "";
			while ((line = rd.readLine()) != null) {
				System.out.println("AOpenStackComputeRESTExecutor token with responsecode = " + postResponse.getStatusLine().getStatusCode() + "	request Response : " + line);
				if(postResponse.getStatusLine().getStatusCode() == 401) {
					System.out.println("ERROR  == 401");
				} else if(postResponse.getStatusLine().getStatusCode() == 404) {
					System.out.println("ERROR == 404");
				} else  if (postResponse.getStatusLine().getStatusCode() >= 300){
					System.out.println("ERROR  >= 300");
				} else {
					credentials = new GsonBuilder().serializeNulls().create().fromJson(line, Credentials.class);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return credentials;
	}
	/**
	 * This method is used to set custom header data to ISV requests
	 * @param httpRequestBase
	 * @param actionType
	 */
	public void setCustomHeaders(HttpRequestBase httpRequestBase, String actionType, Map<String,String> headers) {
		for(String key:headers.keySet()) {
			httpRequestBase.setHeader(key,headers.get(key));
		}
		httpRequestBase.setHeader("ACTION_TYPE",actionType);
	}
	/**
	 * This method is used to fetch the exact url to be used to make further calls from authentication.
	 * @param region
	 * @param module
	 * @return
	 * @throws Exception
	 */
	public String getServiceCatalogPublicendPoint(String region, String module, Credentials credentials) throws Exception {
		String serviceCatalogPublicendPoint = null;
		List<Service> catalogServices = credentials.getAccess().getServiceCatalog();
		Iterator<Service> it = catalogServices.iterator();
		while(it.hasNext()) {
			Service catalogService = it.next();
			if( catalogService != null && module.equalsIgnoreCase(catalogService.getType())){
				List<Endpoint> endPoints =  catalogService.getEndpoints();
				if( endPoints != null && endPoints.size() > 0) {
					Iterator<Endpoint> endpointIt = endPoints.iterator();
					while(endpointIt.hasNext()) {
						Endpoint endpoint = endpointIt.next();
						if(endpoint != null && endpoint.getRegion() != null && region.equalsIgnoreCase(endpoint.getRegion()) ) {
							serviceCatalogPublicendPoint = endpoint.getPublicURL();
							break;
						}
					}
				}
			}
		}
		return serviceCatalogPublicendPoint;
	}

	/**
	 * Get Server List
	 * @param credentials
	 * @param context
	 * @return String (response)
	 */
	public String getServerlist(Credentials credentials,Map<String ,String> context,String region,String module) {
		String response="";
		HttpClient client = new DefaultHttpClient();
		try {
			String endPointUrl=getServiceCatalogPublicendPoint(region,module, credentials).concat("/servers");
			HttpGet getServerGetRequest = new HttpGet(endPointUrl);
			getServerGetRequest.setHeader("X-Auth-Token", credentials.getAccess().getToken().getId());
			setCustomHeaders(getServerGetRequest, "getserver", context);
			HttpResponse getServerResponse = client.execute(getServerGetRequest);
			BufferedReader reader = new BufferedReader(new InputStreamReader(getServerResponse.getEntity().getContent()));
			String isvResponse;
			while (( isvResponse = reader.readLine()) != null) {
				System.out.println("Instance details isv Response = " + isvResponse);
				if( getServerResponse.getStatusLine().getStatusCode() == 404) {
					System.out.println("Instance is already terminated at ISV and isvResponse = " + isvResponse);
					return null;
				} else if(getServerResponse.getStatusLine().getStatusCode() >= 300) {
					System.out.println("ERROR >=300"+isvResponse);
				} else {
					response +=isvResponse;
				}
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		return response;
	}

	/**
	 * Get Server Detail using server Id
	 * @param credentials
	 * @param context
	 * @return String (response)
	 */
	public  String getServerDetail(Credentials credentials,Map<String ,String> context,String region,String module,String serverId) {
		String response="";
		HttpClient client = new DefaultHttpClient();
		try {
			String endPointUrl=getServiceCatalogPublicendPoint(region,module, credentials).concat("/servers/").concat(serverId);
			HttpGet getServerGetRequest = new HttpGet(endPointUrl);
			getServerGetRequest.setHeader("X-Auth-Token", credentials.getAccess().getToken().getId());
			setCustomHeaders(getServerGetRequest, "getserver", context);
			HttpResponse getServerResponse = client.execute(getServerGetRequest);
			BufferedReader reader = new BufferedReader(new InputStreamReader(getServerResponse.getEntity().getContent()));
			String isvResponse;
			while (( isvResponse = reader.readLine()) != null) {
				System.out.println("Instance details isv Response = " + isvResponse);
				if( getServerResponse.getStatusLine().getStatusCode() == 404) {
					System.out.println("Instance is already terminated at ISV and isvResponse = " + isvResponse);
					return null;
				} else if(getServerResponse.getStatusLine().getStatusCode() >= 300) {
					System.out.println("ERROR >=300 "+isvResponse);
				} else {
					response +=isvResponse;
				}
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		return response;
	}
	/**
	 * Get Avsilable Flavours
	 * @param credentials
	 * @param context
	 * @param region
	 * @param module
	 * @return
	 */
	public String getFlavourList(Credentials credentials,Map<String ,String> context,String region,String module) {
		String response="";
		HttpClient client=new DefaultHttpClient();
		try {
			String endPointUrl=getServiceCatalogPublicendPoint(region, module, credentials).concat("/flavors/detail");
			HttpGet getRequet=new HttpGet(endPointUrl);
			getRequet.setHeader("X-Auth-Token",  credentials.getAccess().getToken().getId());
			setCustomHeaders(getRequet, "listflavour", context);
			HttpResponse httpResponse=client.execute(getRequet);
			BufferedReader reader=new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
			String isvResponse="";
			while((isvResponse =reader.readLine())!=null) {
				if(httpResponse.getStatusLine().getStatusCode() ==404) {
					System.out.println("ERROR  == 404"+isvResponse);
				}else if (httpResponse.getStatusLine().getStatusCode() >=300) {
					System.out.println("ERROR  >=300"+isvResponse);
				}else {
					response+=isvResponse;
				}
			}
		}catch(Exception e) {

		}
		return response;
	}
	/**
	 * Resize an Instance
	 * @param credentials
	 * @param context
	 * @param region
	 * @param module
	 * @param serverId
	 * @param flavour
	 * @return
	 */
	public String resizeInstance(Credentials credentials,Map<String ,String> context,String region,String module,String serverId,String flavour) {
		String response="";
		HttpClient client=new DefaultHttpClient();
		try {
			String endPointUrl=getServiceCatalogPublicendPoint(region, module, credentials).concat("/servers/").concat(serverId).concat("/action");
			HttpPost httpPost=new HttpPost(endPointUrl);
			String postBody="{\"resize\": {\"flavorRef\": \""+flavour+"\"}}";
			StringEntity entity=new StringEntity(postBody);
			entity.setContentType("application/json");
			httpPost.setHeader("X-Auth-Token",credentials.getAccess().getToken().getId());
			httpPost.setEntity(entity);
			setCustomHeaders(httpPost, "resizeInstance",context);
			System.out.println("End Point Url"+endPointUrl+"\n Context : "+context+"\n PostBody : "+postBody);
			HttpResponse httpResponse=client.execute(httpPost);
			BufferedReader reader=new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
			String isvResponse="";
			while((isvResponse = reader.readLine())!=null) {
				if(httpResponse.getStatusLine().getStatusCode() ==4004) {
					System.out.println("ERROR ==404"+isvResponse);
				}else if(httpResponse.getStatusLine().getStatusCode() >=300) {
					System.out.println("ERROR > = 300"+isvResponse);
				}else {
					response+=isvResponse;
				}
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		return response;
	}
	/**
	 * Confirm Resize of Instance
	 * @param credentials
	 * @param context
	 * @param region
	 * @param module
	 * @param serverId
	 * @return
	 */
	public String confirmResizeInstance(Credentials credentials,Map<String ,String> context,String region,String module,String serverId) {
		String response="";
		HttpClient client=new DefaultHttpClient();
		try {
			String endPointUrl=getServiceCatalogPublicendPoint(region, module, credentials).concat("/servers/").concat(serverId).concat("/action");
			HttpPost httpPost=new HttpPost(endPointUrl);
			String postBody="{\"confirmResize\": null}";
			StringEntity entity=new StringEntity(postBody);
			entity.setContentType("application/json");
			httpPost.setHeader("X-Auth-Token",credentials.getAccess().getToken().getId());
			httpPost.setEntity(entity);
			setCustomHeaders(httpPost, "resizeInstance",context);
			System.out.println("End Point Url"+endPointUrl+"\n Context : "+context+"\n PostBody : "+postBody);
			HttpResponse httpResponse=client.execute(httpPost);
			BufferedReader reader=new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
			String isvResponse="";
			while((isvResponse = reader.readLine())!=null) {
				if(httpResponse.getStatusLine().getStatusCode() ==4004) {
					System.out.println("ERROR ==404"+isvResponse);
				}else if(httpResponse.getStatusLine().getStatusCode() >=300) {
					System.out.println("ERROR > = 300"+isvResponse);
				}else {
					response+=isvResponse;
				}
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		return response;
	}
	/**
	 * Create Snappshot Image of instance
	 * @param credentials
	 * @param context
	 * @param region
	 * @param module
	 * @param serverId
	 * @param imageName
	 * @param metadataName
	 * @return
	 */
	public String createImage(Credentials credentials,Map<String ,String> context,String region,String module,String serverId,String imageName,String metadataName) {
		String response="";
		HttpClient client=new DefaultHttpClient();

		try {
			String url=getServiceCatalogPublicendPoint(region, module, credentials).concat("/servers/").concat(serverId).concat("/action");
			HttpPost httpPost=new HttpPost(url);
			String body="{\"createImage\": {\"name\": \""+imageName+"\",\"metadata\": {\"myvar\": \""+metadataName+"\"}}}";
			StringEntity entity=new StringEntity(body);
			entity.setContentType("application/json");
			httpPost.setEntity(entity);
			httpPost.setHeader("X-Auth-Token", credentials.getAccess().getToken().getId());
			setCustomHeaders(httpPost, "createImage", context);
			HttpResponse httpResponse=client.execute(httpPost);
			BufferedReader reader=new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
			String isvResponse="";
			while((isvResponse = reader.readLine())!=null) {
				if(httpResponse.getStatusLine().getStatusCode() ==4004) {
					System.out.println("ERROR ==404"+isvResponse);
				}else if(httpResponse.getStatusLine().getStatusCode() >=300) {
					System.out.println("ERROR > = 300"+isvResponse);
				}else {
					response+=isvResponse;
				}
			}

		}catch(Exception e) {
			e.printStackTrace();
		}
		return response;
	}
}