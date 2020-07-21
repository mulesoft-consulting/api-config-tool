package com.mulesoft.java;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

public class ApiConfigTool {
	public static String HTTPS_ANYPOINT_MULESOFT_COM = "https://anypoint.mulesoft.com";
	public static boolean makeApiNameBusinessGroupSensitive = false;
	public static String RESOURCES_DIR = "src/main/resources";
	public static String API_VERSION_HEADER_MSG = "ApiConfigTool version 1.0.12";

	public static void main(String[] args) {

		try {
			if (args.length <= 6) {
				System.err.println(API_VERSION_HEADER_MSG);
				System.err.println("\n");
				printHelp();
			} else if (args[0].equals("configureProjectResourceFile")
					|| args[0].equals("mule3ConfigureProjectResourceFile")) {
				System.err.println(API_VERSION_HEADER_MSG + " Starting " + args[0] + " environment: " + args[6]);
				LinkedHashMap<String, Object> returnMap = configureApi((args.length > 1) ? args[1] : "userName",
						(args.length > 2) ? args[2] : "userPass", (args.length > 3) ? args[3] : "orgName",
						(args.length > 4) ? args[4] : "apiName", (args.length > 5) ? args[5] : "apiVersion",
						(args.length > 6) ? args[6] : "DEV", (args.length > 7) ? args[7] : "client-credentials-policy",
						(args.length > 8) ? args[8] : "empty-client-access-list",
						false,
						(args.length > 9) ? args[9] : "empty-sla-tiers-list");
				updateProjectResourceConfigProperties(returnMap, false);
				System.err.println();
				System.err.println(
						API_VERSION_HEADER_MSG + " Completion " + args[0] + " environment: " + args[6]);
				System.err.println("\n");
			} else if (args[0].equals("mule4ConfigureProjectResourceFile")) {
				System.err.println(API_VERSION_HEADER_MSG + " Starting " + args[0] + " environment: " + args[6]);
				LinkedHashMap<String, Object> returnMap = configureApi((args.length > 1) ? args[1] : "userName",
						(args.length > 2) ? args[2] : "userPass", (args.length > 3) ? args[3] : "orgName",
						(args.length > 4) ? args[4] : "apiName", (args.length > 5) ? args[5] : "apiVersion",
						(args.length > 6) ? args[6] : "DEV",
						(args.length > 7) ? args[7] : "mule4-client-credentials-policy",
						(args.length > 8) ? args[8] : "empty-client-access-list",
						true,
						(args.length > 9) ? args[9] : "empty-sla-tiers-list");
				updateProjectResourceConfigProperties(returnMap, true);
				System.err.println();
				System.err.println(
						API_VERSION_HEADER_MSG + " Completion " + args[0] + " environment: " + args[6]);
				System.err.println("\n");
			} else {
				printHelp();
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
			System.exit(500);
		}
	}

	private static void updateProjectResourceConfigProperties(LinkedHashMap<String, Object> map,
			boolean isMule4Runtime) {
		Properties configProperties = new SortedProperties();
		FileInputStream input = null;
		FileOutputStream output = null;
		File resourcesDir = new File(RESOURCES_DIR);

		/*
		 * ObjectMapper mapperw = new ObjectMapper(); String result; try { result =
		 * mapperw.writerWithDefaultPrettyPrinter().writeValueAsString(map);
		 * System.err.println(result); } catch (JsonProcessingException e) {
		 * e.printStackTrace(); }
		 */
		try {
			StringBuilder filename = new StringBuilder();
			filename.append(map.get("envName").toString().toUpperCase()).append("-config.properties");
			File file = new File(resourcesDir, filename.toString());
			StringBuilder yamlFileName = new StringBuilder();
			yamlFileName.append(map.get("envName").toString().toUpperCase()).append("-config.yaml");
			File yamlFile = new File(resourcesDir, yamlFileName.toString());

			System.err.println();
			if ((file.exists() && !isMule4Runtime) || (!yamlFile.exists() && file.exists() && isMule4Runtime) ){
				System.err.println("Reading property file from the location " + file.getAbsolutePath());
				input = FileUtils.openInputStream(file);

				// load a properties file
				configProperties.load(input);

				@SuppressWarnings("unchecked")
				LinkedHashMap<String, String> generatedProperties = (LinkedHashMap<String, String>) map
						.get("properties");
				configProperties.put("api.name", generatedProperties.get("auto-discovery-apiName"));
				configProperties.put("api.version", generatedProperties.get("auto-discovery-apiVersion"));
				configProperties.put("api.id", generatedProperties.get("auto-discovery-apiId"));
				configProperties.put("my.client_id", (generatedProperties.get("my.client_id") != null)?generatedProperties.get("my.client_id"):"");
//				configProperties.put("my.client_secret", (generatedProperties.get("my.client_secret") != null)?generatedProperties.get("my.client_secret"):"");
				configProperties.put("my.client_secret", "");
				configProperties.put("my.client_name", (generatedProperties.get("my.client_name") != null)?generatedProperties.get("my.client_name"):"");

				output = FileUtils.openOutputStream(file);
				configProperties.store(output, null);
				System.err.println("Updated API auto discovery details in the property file : " + file.getAbsolutePath());
			} else if (yamlFile.exists() && isMule4Runtime) {
				System.err.println("Reading property file from the location " + yamlFile.getAbsolutePath());
				String yamlFileLocation = yamlFile.getAbsolutePath();
				YAMLConfigurationUtil yamlConfUtil = new YAMLConfigurationUtil();
				LinkedHashMap<String, Object> yamlConfigProperties = (LinkedHashMap<String, Object>) yamlConfUtil
						.yaml2map(yamlFileLocation);
				@SuppressWarnings("unchecked")
				LinkedHashMap<String, String> generatedProperties = (LinkedHashMap<String, String>) map
						.get("properties");
				yamlConfigProperties.put("api.name", generatedProperties.get("auto-discovery-apiName"));
				yamlConfigProperties.put("api.version", generatedProperties.get("auto-discovery-apiVersion"));
				yamlConfigProperties.put("api.id", generatedProperties.get("auto-discovery-apiId"));
				yamlConfigProperties.put("my.client_id", (generatedProperties.get("my.client_id") != null)?generatedProperties.get("my.client_id"):"");
//				yamlConfigProperties.put("my.client_secret", (generatedProperties.get("my.client_secret") != null)?generatedProperties.get("my.client_secret"):"");
				yamlConfigProperties.put("my.client_secret", "");
				yamlConfigProperties.put("my.client_name", (generatedProperties.get("my.client_name") != null)?generatedProperties.get("my.client_name"):"");
				yamlConfUtil.map2yaml(yamlConfigProperties, yamlFileLocation);
				System.err.println("Updated API auto discovery details in the property file : " + yamlFile.getAbsolutePath());
			} else {
				System.err.println("Creating property file " + file.getAbsolutePath());

				@SuppressWarnings("unchecked")
				LinkedHashMap<String, String> generatedProperties = (LinkedHashMap<String, String>) map
						.get("properties");
				configProperties.put("api.name", generatedProperties.get("auto-discovery-apiName"));
				configProperties.put("api.version", generatedProperties.get("auto-discovery-apiVersion"));
				configProperties.put("api.id", generatedProperties.get("auto-discovery-apiId"));
				configProperties.put("my.client_id", (generatedProperties.get("my.client_id") != null)?generatedProperties.get("my.client_id"):"");
//				configProperties.put("my.client_secret", (generatedProperties.get("my.client_secret") != null)?generatedProperties.get("my.client_secret"):"");
				configProperties.put("my.client_secret", "");
				configProperties.put("my.client_name", (generatedProperties.get("my.client_name") != null)?generatedProperties.get("my.client_name"):"");

				output = FileUtils.openOutputStream(file);
				configProperties.store(output, null);
				System.err.println("Added API auto discovery details in the property file : " + file.getAbsolutePath());
			}
		} catch (IOException ex) {
			IOUtils.closeQuietly(input);
			IOUtils.closeQuietly(output);
			ex.printStackTrace();
			System.exit(2);
		} finally {
			IOUtils.closeQuietly(input);
			IOUtils.closeQuietly(output);
		}
	}

	private static void printHelp() {
		System.err.println("Usage: java -jar ApiConfigTool {operation} [parameters]\n");
		System.err.println("  operations:");
		System.err.println(
				"    configureProjectResourceFile   -Read the Api definition and publish it to Anypoint Platform as Mule 3 api,");
		System.err.println("                                    updating src/main/resources/<env>-config.properties");
		System.err.println("      Parameters:");
		System.err.println("          userName      -Anypoint user name required");
		System.err.println("          userPassword  -Anypoint user's password required");
		System.err.println("          orgName       -Anypoint business org name (no hierarchy) required");
		System.err.println("          apiName       -api name required");
		System.err.println("          apiVersion    -api version required");
		System.err.println("          env           -environment name required");
		System.err.println("          policies      -file containing policy definitions (json array) optional");
		System.err.println(
				"          applications  -file containing client application namess to register for access (json array) optional");
		System.err.println("\n");
		System.err.println(
				"    mule4ConfigureProjectResourceFile   -Read the Api definition and publish it to Anypoint Platform as Mule 4 api,");
		System.err.println(
				"                                         updating src/main/resources/<env>-config.properties");
		System.err.println("      Parameters:");
		System.err.println("          userName      -Anypoint user name required");
		System.err.println("          userPassword  -Anypoint user's password required");
		System.err.println("          orgName       -Anypoint business org name (no hierarchy) required");
		System.err.println("          apiName       -api name required");
		System.err.println("          apiVersion    -api version required");
		System.err.println("          env           -environment name required");
		System.err.println("          policies      -file containing policy definitions (json array) optional");
		System.err.println(
				"          applications  -file containing client application namess to register for access (json array) optional");
		System.err.println("\n");
	}

	@SuppressWarnings("unchecked")
	private static LinkedHashMap<String, Object> configureApi(String userName, String userPass,
			String businessGroupName, String apiName, String apiVersion, String environmentName, String policies,
			String clients, boolean mule4OrAbove, String slaTiers) throws Exception {

		LinkedHashMap<String, Object> returnPayload = new LinkedHashMap<String, Object>();
		LinkedHashMap<String, String> returnPayloadProperties = new LinkedHashMap<String, String>();

		Client client = null;
		client = ClientBuilder.newClient();
		client.register(JacksonJsonProvider.class).register(MultiPartFeature.class);

		returnPayload.put("projectName", "auto-api-registation");
		returnPayload.put("branchName", apiName);
		returnPayload.put("instanceId", apiVersion);
		returnPayload.put("envName", environmentName);

		// registration steps

		/*
		 * Authenticate with Anypoint Platform
		 */
		String apToken = getAPToken(client, userName, userPass);
		String authorizationHdr = "Bearer " + apToken;

		/*
		 * Get the login user information, organizationId and business group id
		 */
		LinkedHashMap<String, Object> myInformation = getMyInformation(client, authorizationHdr);
		String myOrganizationId = (String) ((LinkedHashMap<String, Object>) myInformation.get("user"))
				.get("organizationId");
		String myOrganizationName = (String) ((LinkedHashMap<String, Object>) ((LinkedHashMap<String, Object>) myInformation
				.get("user")).get("organization")).get("name");

		ArrayList<LinkedHashMap<String, Object>> memberOfOrganizations = (ArrayList<LinkedHashMap<String, Object>>) ((LinkedHashMap<String, Object>) myInformation
				.get("user")).get("memberOfOrganizations");
		LinkedHashMap<String, Object> businessGroupInformation = getBusinessGroupInformation(memberOfOrganizations,
				businessGroupName);
		String businessGroupId = (String) businessGroupInformation.get("id");

		/*
		 * Get the environment id
		 */
		LinkedHashMap<String, Object> environment = getEnvironmentInformation(client, authorizationHdr, businessGroupId,
				environmentName);
		String environmentId = (String) environment.get("id");
		ArrayList<LinkedHashMap<String, Object>> applications = null;

		/*
		 * Create the API in Exchange
		 */
		ArrayList<LinkedHashMap<String, Object>> apiAssets = null;
		apiAssets = getExchangeAssets(client, authorizationHdr, businessGroupId, apiName);

		LinkedHashMap<String, Object> apiAsset = null;
		apiAsset = findApiAsset(apiAssets, myOrganizationName, businessGroupName, apiName, apiVersion);

		if (apiAsset == null) {
			publishAPItoExchange(client, authorizationHdr, apiName, apiVersion, myOrganizationName, myOrganizationId,
					businessGroupName, businessGroupId);
			apiAssets = getExchangeAssets(client, authorizationHdr, businessGroupId, apiName);
			apiAsset = findApiAsset(apiAssets, myOrganizationName, businessGroupName, apiName, apiVersion);
		}
//		System.err.println("apiAsset:" + apiAsset);
		String exchangeGroupId = (String) apiAsset.get("groupId");
		String exchangeAssetId = (String) apiAsset.get("assetId");
		String exchangeAssetVersion = (String) apiAsset.get("version");
		String exchangeAssetName = (String) apiAsset.get("name");
		String apiType = (String) apiAsset.get("type");

		/*
		 * Create an API Instance in API Manager
		 */
		LinkedHashMap<String, Object> apiManagerAsset = null;
		apiManagerAsset = getApiManagerAsset(client, authorizationHdr, businessGroupId, environmentId, exchangeAssetId,
				exchangeAssetVersion);
		if (apiManagerAsset == null) {
			registerAPIInstance(client, authorizationHdr, businessGroupId, environmentId, exchangeAssetId,
					exchangeAssetVersion, apiType, mule4OrAbove, exchangeGroupId);
			apiManagerAsset = getApiManagerAsset(client, authorizationHdr, businessGroupId, environmentId,
					exchangeAssetId, exchangeAssetVersion);
		}
		String apiManagerAssetId = apiManagerAsset.get("id").toString();
		String autoDiscoveryApiName = (String) apiManagerAsset.get("autodiscoveryApiName");
		String autoDiscoveryApiVersion = null;
		String autoDiscoveryApiId = null;
		for (LinkedHashMap<String, Object> e : (ArrayList<LinkedHashMap<String, Object>>) apiManagerAsset.get("apis")) {
			if (e.get("instanceLabel").equals("auto-api-registation-" + exchangeAssetId)) {
				autoDiscoveryApiVersion = (String) e.get("autodiscoveryInstanceName");
				autoDiscoveryApiId = e.get("id").toString();
				break;
			}
		}

		/*
		 * Create the my.client application information
		 */
		String generated_client_name = null;
		String generated_client_id = null;
		String generated_client_secret = null;
		StringBuilder applicationName = new StringBuilder();
		applicationName.append(exchangeAssetName.toUpperCase()).append("_").append(environmentName.toUpperCase());
		createApplication(client, authorizationHdr, myOrganizationId, applicationName.toString(), null, autoDiscoveryApiId);
		applications = getApplicationList(client, authorizationHdr, myOrganizationId, environmentId);
		LinkedHashMap<String, Object> applicationInfo = null;
		for (LinkedHashMap<String, Object> e:applications) {
			if (e.get("name").equals(applicationName.toString())) {
				applicationInfo = getApplicationInformation(client, authorizationHdr, myOrganizationId, (int) e.get("id"));
				generated_client_name = (String) applicationInfo.get("name");
				generated_client_id = (String) applicationInfo.get("clientId");
				generated_client_secret = (String) applicationInfo.get("clientSecret");
				break;
			}
		}

		ArrayList<LinkedHashMap<String, Object>> currentPolicies = null;
		try {
			/*
			 * Add API Policies
			 */
			System.err.println();
			currentPolicies = getApiPolicies(client, authorizationHdr, businessGroupId, environmentId, autoDiscoveryApiId);
						
			addApiPolicies(client, authorizationHdr, businessGroupId, environmentId, autoDiscoveryApiId, policies,
					(ArrayList<LinkedHashMap<String, Object>>) currentPolicies, mule4OrAbove);
			
			/*
			 * Add application contracts
			 */
			applications = getApplicationList(client, authorizationHdr, myOrganizationId, environmentId);
			createApplicationContracts(client, authorizationHdr, businessGroupId, businessGroupName, businessGroupId,
					environmentName, environmentId, exchangeAssetId, exchangeAssetVersion, autoDiscoveryApiId,
					apiVersion, clients, applications);
			
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}

		try {
			/*
			 * Add SLA Tiers
			 */
			addSlaTiers(client, authorizationHdr, businessGroupId, environmentId, autoDiscoveryApiId, slaTiers,
					mule4OrAbove);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}

		// save configuration
		ArrayList<Object> empty = new ArrayList<Object>();
		returnPayload.put("imports", empty.toArray());
		returnPayloadProperties.put("secure.properties",
				"generated_client_secret,cps_client_secret,auto_api_registration_client_secret");
		returnPayloadProperties.put("apiName", apiName);
		returnPayloadProperties.put("apiManagerAssetId", apiManagerAssetId);
		returnPayloadProperties.put("apiVersion", apiVersion);
		returnPayloadProperties.put("exchangeAssetName", exchangeAssetName);
		returnPayloadProperties.put("exchangeAssetId", exchangeAssetId);
		returnPayloadProperties.put("exchangeAssetVersion", exchangeAssetVersion);
		returnPayloadProperties.put("exchangeAssetVersionGroup", (String) apiAsset.get("versionGroup"));
		returnPayloadProperties.put("exchangeAssetGroupId", (String) apiAsset.get("groupId"));
		returnPayloadProperties.put("exchangeAssetOrganizationId", (String) apiAsset.get("groupId"));
		returnPayloadProperties.put("auto-discovery-apiId", autoDiscoveryApiId);
		returnPayloadProperties.put("auto-discovery-apiName", autoDiscoveryApiName);
		returnPayloadProperties.put("auto-discovery-apiVersion", autoDiscoveryApiVersion);
		returnPayloadProperties.put("my.client_name", generated_client_name);
		returnPayloadProperties.put("my.client_id", generated_client_id);
		returnPayloadProperties.put("my.client_secret", generated_client_secret);
		
		/*
		 * returnPayloadProperties.put("cps_client_name",
		 * cps_client_name); returnPayloadProperties.put("cps_client_id",
		 * cps_client_id); returnPayloadProperties.put("cps_client_secret",
		 * cps_client_secret);
		 * returnPayloadProperties.put("auto_api_registration_client_name",
		 * auto_reg_client_name);
		 * returnPayloadProperties.put("auto_api_registration_client_id",
		 * auto_reg_client_id);
		 * returnPayloadProperties.put("auto_api_registration_client_secret",
		 * auto_reg_client_secret);
		 */
		returnPayload.put("properties", returnPayloadProperties);

		return returnPayload;
	}

	@SuppressWarnings("unchecked")
	private static String getAPToken(Client restClient, String user, String password) throws JsonProcessingException {
		
		if (user.equalsIgnoreCase("~~~Token~~~")) {
			return password;
		}
		
		String token = null;
		LinkedHashMap<String, Object> loginValues = new LinkedHashMap<String, Object>();
		loginValues.put("username", user);
		loginValues.put("password", password);
		String payload = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(loginValues);
		WebTarget target = restClient.target(HTTPS_ANYPOINT_MULESOFT_COM).path("accounts/login");

		Response response = target.request().accept(MediaType.APPLICATION_JSON)
				.post(Entity.entity(payload, MediaType.APPLICATION_JSON));

		int statuscode = 500;
		Map<String, Object> result = null;
		if (response != null) {
			statuscode = response.getStatus();
		}
		if (response != null && response.getStatus() == 200) {
			result = response.readEntity(Map.class);
			token = (String) result.get("access_token");
		} else {
			System.err.println("Failed to login...check credentials");
			System.exit(statuscode);
		}

		return token;
	}

	@SuppressWarnings("unchecked")
	private static LinkedHashMap<String, Object> getMyInformation(Client restClient, String authorizationHdr)
			throws JsonProcessingException {
		WebTarget target = restClient.target(HTTPS_ANYPOINT_MULESOFT_COM).path("accounts/api/me");

		Response response = target.request().header("Authorization", authorizationHdr)
				.accept(MediaType.APPLICATION_JSON).get();

		int statuscode = 500;
		LinkedHashMap<String, Object> result = null;
		if (response != null) {
			statuscode = response.getStatus();
		}
		if (response != null && response.getStatus() == 200) {
			result = response.readEntity(LinkedHashMap.class);
		} else {
			System.err.println("Failed to get login profile");
			System.exit(statuscode);
		}

		// ObjectMapper mapperw = new ObjectMapper();
		// System.err.println("myInformation: " +
		// mapperw.writerWithDefaultPrettyPrinter().writeValueAsString(result));
		return result;
	}

	private static LinkedHashMap<String, Object> getBusinessGroupInformation(
			ArrayList<LinkedHashMap<String, Object>> memberOfOrganizations, String businessGroupName)
			throws JsonProcessingException {
		LinkedHashMap<String, Object> result = null;

		for (LinkedHashMap<String, Object> i : memberOfOrganizations) {
			if (i.get("name").equals(businessGroupName)) {
				result = i;
				break;
			}
		}

		if (result != null) {
			// ObjectMapper mapperw = new ObjectMapper();
			// System.err.println(
			// "businessGroupInformation: " +
			// mapperw.writerWithDefaultPrettyPrinter().writeValueAsString(result));
			return result;
		} else {
			System.err.println("Failed to find business Group information for " + businessGroupName);
			System.exit(404);
			return null;
		}

	}

	@SuppressWarnings("unchecked")
	private static LinkedHashMap<String, Object> getEnvironmentInformation(Client restClient, String authorizationHdr,
			String businessGroupId, String environmentName) throws JsonProcessingException {
		WebTarget target = restClient.target(HTTPS_ANYPOINT_MULESOFT_COM).path("accounts/api/organizations")
				.path(businessGroupId).path("environments");

		Response response = target.request().header("Authorization", authorizationHdr)
				.accept(MediaType.APPLICATION_JSON).get();

		int statuscode = 500;
		LinkedHashMap<String, Object> result = null;
		if (response != null) {
			statuscode = response.getStatus();
		}
		if (response != null && response.getStatus() == 200) {
			result = response.readEntity(LinkedHashMap.class);
		} else {
			System.err.println("Failed to get environment information");
			System.exit(statuscode);
		}

		for (LinkedHashMap<String, Object> i : (ArrayList<LinkedHashMap<String, Object>>) result.get("data")) {
			if (i.get("name").equals(environmentName)) {
				result = i;
				break;
			}
		}

		if (result != null) {
			// ObjectMapper mapperw = new ObjectMapper();
			// System.err.println("environment: " +
			// mapperw.writerWithDefaultPrettyPrinter().writeValueAsString(result));
			return result;
		} else {
			System.err.println("Failed to find environment information");
			System.exit(404);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private static ArrayList<LinkedHashMap<String, Object>> getApplicationList(Client restClient,
			String authorizationHdr, String organizationId, String environmentId) throws JsonProcessingException {
//		WebTarget target = restClient.target(HTTPS_ANYPOINT_MULESOFT_COM).path("exchange/api/v1/organizations")
//				.path(organizationId).path("environments").path(environmentId).path("applications");
		WebTarget target = restClient.target(HTTPS_ANYPOINT_MULESOFT_COM).path("exchange/api/v1/organizations")
				.path(organizationId).path("applications");

//		System.err.println("getApplicationList: " + target.toString());
		Response response = target.request().header("Authorization", authorizationHdr)
				.accept(MediaType.APPLICATION_JSON).get();

		int statuscode = 500;
		ArrayList<LinkedHashMap<String, Object>> result = null;
		if (response != null) {
			statuscode = response.getStatus();
		}
		if (response != null && response.getStatus() == 200) {
			result = (ArrayList<LinkedHashMap<String, Object>>) response.readEntity(ArrayList.class);
		} else {
			System.err.println("Failed to get application list (" + statuscode + ")");
			System.exit(statuscode);
		}

		if (result != null) {
//			ObjectMapper mapperw = new ObjectMapper();
//			System.err.println("applications: " + mapperw.writerWithDefaultPrettyPrinter().writeValueAsString(result));
			return result;
		} else {
			System.err.println("Failed to find list of applications");
			return null;
		}
	}

	@SuppressWarnings({ "unchecked", "unused" })
	private static LinkedHashMap<String, Object> getApplicationInformation(Client restClient, String authorizationHdr,
			String organizationId, int applicationId) throws JsonProcessingException {
		WebTarget target = restClient.target(HTTPS_ANYPOINT_MULESOFT_COM).path("exchange/api/v1/organizations")
				.path(organizationId).path("applications").path(Integer.toString(applicationId));

		Response response = target.request().header("Authorization", authorizationHdr)
				.accept(MediaType.APPLICATION_JSON).get();

		int statuscode = 500;
		LinkedHashMap<String, Object> result = null;
		if (response != null) {
			statuscode = response.getStatus();
		}
		if (response != null && response.getStatus() == 200) {
			result = (LinkedHashMap<String, Object>) response.readEntity(LinkedHashMap.class);
		} else {
			System.err.println("Failed to get application information (" + statuscode + ")");
			System.exit(statuscode);
		}

		if (result != null) {
//			ObjectMapper mapperw = new ObjectMapper();
//			System.err.println("application: " + mapperw.writerWithDefaultPrettyPrinter().writeValueAsString(result));
			return result;
		} else {
			System.err.println("Failed to find application information");
			return null;
		}
	}

	@SuppressWarnings("unused")
	private static void createApplication(Client restClient, String authorizationHdr, String organizationId,
			String applicationName, String description, String apiInstanceId) throws JsonProcessingException {
		String desc = (description == null)
				? "Auto generated the my_client credentials for this API instance to use calling other dependencies."
				: description;
		LinkedHashMap<String, Object> applicationValues = new LinkedHashMap<String, Object>();
		applicationValues.put("name", applicationName);
		applicationValues.put("description", desc);
		applicationValues.put("redirectUri", new ArrayList<String>());
		applicationValues.put("grantTypes", new ArrayList<String>());
		applicationValues.put("apiEndpoints", false);
		String payload = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(applicationValues);
		WebTarget target = restClient.target(HTTPS_ANYPOINT_MULESOFT_COM).path("exchange/api/v1/organizations")
				.path(organizationId).path("applications").queryParam("apiInstanceId", apiInstanceId);

//		System.out.println("createApplication: " + target.toString());

		Response response = target.request().header("Authorization", authorizationHdr)
				.accept(MediaType.APPLICATION_JSON).post(Entity.entity(payload, MediaType.APPLICATION_JSON));

		int statuscode = 500;
		if (response != null) {
			statuscode = response.getStatus();
		}
		if (response != null && (response.getStatus() == 201 || response.getStatus() == 409)) {
//			System.err.println(response.readEntity(String.class));
		} else if (response.getStatus() == 502) {
			System.err.println("\n***NOTE*** If this is an Exernal Identity OpenId Client Management configuration, you may not have access to the Identity Provider.");
			System.err.println("Failed to create application information (" + statuscode + ")");
			System.err.println(response.readEntity(String.class) + "\n");
		} else {
			System.err.println("Failed to create application information (" + statuscode + ")");
			System.err.println(response.readEntity(String.class));
			System.exit(statuscode);
		}
	}

	private static void createApplicationContracts(Client restClient, String authorizationHdr, String organizationId,
			String businessGroupName, String businessGroupId, String environmentName, String environmentId,
			String exchangeAssetId, String exchangeAssetVersion, String autoDiscoveryApiId, String apiVersion,
			String contractsFileName, ArrayList<LinkedHashMap<String, Object>> applications)
			throws JsonProcessingException {

		ArrayList<LinkedHashMap<String, Object>> contracts;
		ObjectMapper mapper;
		TypeFactory factory;
		CollectionType type;

		factory = TypeFactory.defaultInstance();
		type = factory.constructCollectionType(ArrayList.class, LinkedHashMap.class);
		mapper = new ObjectMapper();

		InputStream is = null;
		File contractsFile = new File(contractsFileName);
		String contractsStr = null;
		try {
			if (contractsFile.exists()) {
				contractsStr = FileUtils.readFileToString(contractsFile, "UTF-8");
			} else {
				is = ApiConfigTool.class.getClassLoader().getResourceAsStream(contractsFileName);
				contractsStr = IOUtils.toString(is, "UTF-8");
			}
//			System.err.println(contractsStr);
			contracts = mapper.readValue(contractsStr, type);

			for (LinkedHashMap<String, Object> i : contracts) {
				int applicationId = 0;
				StringBuilder applicationName = new StringBuilder();
				applicationName.append(i.get("applicationName"));
//				System.err.println(applicationName.toString());
				for (LinkedHashMap<String, Object> e : applications) {
					if (e.get("name").equals(applicationName.toString())) {
						applicationId = (int) e.get("id");
						break;
					}
				}
				if (applicationId != 0) {
					createApplicationContract(restClient, authorizationHdr, organizationId, applicationId,
							businessGroupId, environmentId, exchangeAssetId, exchangeAssetVersion, autoDiscoveryApiId,
							apiVersion);
				} else {
					System.err.println("Could not find application in list: " + applicationName);
				}
			}

		} catch (Exception e) {
			System.err.println("Cannot use contracts file " + contractsFileName);
			e.printStackTrace(System.err);
		} finally {
			if (is != null)
				IOUtils.closeQuietly(is);
		}

	}

	private static void createApplicationContract(Client restClient, String authorizationHdr, String organizationId,
			int applicationId, String businessGroupId, String environmentId, String exchangeAssetId,
			String exchangeAssetVersion, String autoDiscoveryApiId, String apiVersion) throws JsonProcessingException {
		LinkedHashMap<String, Object> contractValues = new LinkedHashMap<String, Object>();
		contractValues.put("apiId", autoDiscoveryApiId);
		contractValues.put("environmentId", environmentId);
		contractValues.put("acceptedTerms", true);
		contractValues.put("organizationId", businessGroupId);
		contractValues.put("groupId", businessGroupId);
		contractValues.put("assetId", exchangeAssetId);
		contractValues.put("version", exchangeAssetVersion);
		contractValues.put("productAPIVersion", apiVersion);
		String payload = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(contractValues);

		WebTarget target = restClient.target(HTTPS_ANYPOINT_MULESOFT_COM).path("exchange/api/v1/organizations")
				.path(organizationId).path("applications").path(Integer.toString(applicationId)).path("contracts");

		Response response = target.request().header("Authorization", authorizationHdr)
				.accept(MediaType.APPLICATION_JSON).post(Entity.entity(payload, MediaType.APPLICATION_JSON));

		int statuscode = 500;
		if (response != null) {
			statuscode = response.getStatus();
		}
		if (response != null && (response.getStatus() == 201 || response.getStatus() == 409)) {
//			System.err.println(response.readEntity(String.class));
		} else {
			System.err.println("Failed to create application contract (" + statuscode + ")");
			System.err.println(response.readEntity(String.class));
		}
	}

	private static LinkedHashMap<String, Object> findApiAsset(ArrayList<LinkedHashMap<String, Object>> assetList,
			String organizationName, String groupName, String apiName, String apiVersion)
			throws JsonProcessingException {
		LinkedHashMap<String, Object> result = null;
		StringBuilder sb = new StringBuilder();
		sb.append(apiName);
		if (makeApiNameBusinessGroupSensitive) {
			sb.append("_").append(groupName);
		}
		String name = sb.toString();

		for (LinkedHashMap<String, Object> i : assetList) {
			if (i.get("name").equals(name) && i.get("productAPIVersion").equals(apiVersion)) {
				result = i;
				break;
			}
		}

		if (result != null) {
//			ObjectMapper mapperw = new ObjectMapper();
//			System.err.println(
//					"existing Exchange asset: " + mapperw.writerWithDefaultPrettyPrinter().writeValueAsString(result));
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private static ArrayList<LinkedHashMap<String, Object>> getExchangeAssets(Client restClient,
			String authorizationHdr, String businessGroupId, String name) throws JsonProcessingException {
		WebTarget target = restClient.target(HTTPS_ANYPOINT_MULESOFT_COM).path("exchange/api/v1/assets")
				.queryParam("search", name).queryParam("organizationId", businessGroupId).queryParam("limit", 400);

		Response response = target.request().header("Authorization", authorizationHdr)
				.accept(MediaType.APPLICATION_JSON).get();

		int statuscode = 500;
		ArrayList<LinkedHashMap<String, Object>> result = null;
		if (response != null) {
			statuscode = response.getStatus();
		}
		if (response != null && response.getStatus() == 200) {
			result = response.readEntity(ArrayList.class);
		} else {
			System.err.println("Failed to get Exchange assets (" + statuscode + ")");
			return null;
		}

		if (result != null) {
			// ObjectMapper mapperw = new ObjectMapper();
			// System.err.println("assets: " +
			// mapperw.writerWithDefaultPrettyPrinter().writeValueAsString(result));
			return result;
		} else {
			// System.err.println("Failed to find Exchange assets");
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private static void publishAPItoExchange(Client restClient, String authorizationHdr, String apiName,
			String apiVersion, String organizationName, String organizationId, String groupName, String groupId)
			throws JsonProcessingException {

		String assetVersion = apiVersion;
		StringBuilder assetId = new StringBuilder();
		assetId.append(groupId).append("_").append(apiName).append("_").append(assetVersion);

		StringBuilder name = new StringBuilder();
		name.append(apiName);
		if (makeApiNameBusinessGroupSensitive) {
			name.append("_").append(groupName);
		}

		WebTarget target = restClient.target(HTTPS_ANYPOINT_MULESOFT_COM).path("exchange/api/v1/assets");

		FormDataMultiPart form = new FormDataMultiPart();
		form.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);
		form.field("organizationId", groupId);
		form.field("groupId", groupId);
		form.field("assetId", assetId.toString());
		form.field("version", assetVersion);
		form.field("name", name.toString());
		form.field("apiVersion", apiVersion);
		form.field("classifier", "http");
		form.field("asset", "undefined");

		Response response = target.request().accept(MediaType.APPLICATION_JSON)
				.header("Authorization", authorizationHdr).post(Entity.entity(form, form.getMediaType()));

		int statuscode = 500;
		LinkedHashMap<String, Object> result = null;
		if (response != null) {
			statuscode = response.getStatus();
		}
		if (response != null && response.getStatus() == 201) {
			result = response.readEntity(LinkedHashMap.class);
		} else {
			System.err.println("Failed to post API to Exchange. (" + statuscode + ")");
			System.err.println(response.readEntity(String.class));

		}

		if (result != null) {
			// ObjectMapper mapperw = new ObjectMapper();
			// System.err.println(
			// "new Exchange asset: " +
			// mapperw.writerWithDefaultPrettyPrinter().writeValueAsString(result));
		} else {
			System.err.println("Failed to publish Exchange asset");
			System.exit(statuscode);
		}
	}

	@SuppressWarnings("unchecked")
	private static LinkedHashMap<String, Object> getApiManagerAsset(Client restClient, String authorizationHdr,
			String businessGroupId, String environmentId, String assetId, String assetVersion)
			throws JsonProcessingException {

		WebTarget target = restClient.target(HTTPS_ANYPOINT_MULESOFT_COM).path("apimanager/api/v1/organizations")
				.path(businessGroupId).path("environments").path(environmentId).path("apis")
				.queryParam("assetId", assetId).queryParam("assetVersion", assetVersion).queryParam("limit", 400)
				.queryParam("instanceLabel", "auto-api-registation-" + assetId);

		Response response = target.request().header("Authorization", authorizationHdr)
				.accept(MediaType.APPLICATION_JSON).get();

		int statuscode = 500;
		LinkedHashMap<String, Object> result = null;
		ArrayList<LinkedHashMap<String, Object>> apiManagerAssets = null;
		if (response != null) {
			statuscode = response.getStatus();
		}
		if (response != null && response.getStatus() == 200) {
			LinkedHashMap<String, Object> apis = (LinkedHashMap<String, Object>) response
					.readEntity(LinkedHashMap.class);
			apiManagerAssets = (ArrayList<LinkedHashMap<String, Object>>) apis.get("assets");
			if (apiManagerAssets != null && !apiManagerAssets.isEmpty()) {
				result = apiManagerAssets.get(0);
			}
		} else {
			System.err.println("Failed to get API Manager asset (" + statuscode + ")");
			return null;
		}

		if (result != null) {
//			System.err.println("Using existing API Manager asset " + result.get("id") + ".");
//			ObjectMapper mapperw = new ObjectMapper();
//			System.err.println("api Instance: " + mapperw.writerWithDefaultPrettyPrinter().writeValueAsString(result));
			return result;
		} else {
//			System.err.println("No existing API Manager asset found.");
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private static void registerAPIInstance(Client restClient, String authorizationHdr, String businessGroupId,
			String environmentId, String assetId, String assetVersion, String apiType, boolean mule4OrAbove,
			String exchangeGroupId) throws JsonProcessingException {
		HashMap<String, Object> body = new HashMap<String, Object>();
		LinkedHashMap<String, Object> specValues = new LinkedHashMap<String, Object>();
		specValues.put("groupId", exchangeGroupId);
		specValues.put("assetId", assetId);
		specValues.put("version", assetVersion);
		body.put("spec", specValues);
		body.put("instanceLabel", "auto-api-registation-" + assetId);
		LinkedHashMap<String, Object> endpointValues = new LinkedHashMap<String, Object>();
		endpointValues.put("type", apiType);
		if (apiType.equalsIgnoreCase("rest-api")) {
			endpointValues.put("uri", null);
		} else {
			endpointValues.put("uri", "https://some.implementation.com");
		}
		endpointValues.put("muleVersion4OrAbove", mule4OrAbove);
		endpointValues.put("proxyUri", null);
		endpointValues.put("isCloudHub", false);
		body.put("endpoint", endpointValues);

		String payload = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(body);
		// System.err.println(payload);
		WebTarget target = restClient.target(HTTPS_ANYPOINT_MULESOFT_COM).path("apimanager/api/v1/organizations")
				.path(businessGroupId).path("environments").path(environmentId).path("apis");

		Response response = target.request().accept(MediaType.APPLICATION_JSON)
				.header("Authorization", authorizationHdr).post(Entity.entity(payload, MediaType.APPLICATION_JSON));

		int statuscode = 500;
		LinkedHashMap<String, Object> result = null;
		if (response != null) {
			statuscode = response.getStatus();
		}
		if (response != null && response.getStatus() == 201) {
			result = response.readEntity(LinkedHashMap.class);
		} else {
			System.err.println("Failed to register API to API Manager. (" + statuscode + ")");
			System.err.println(response.readEntity(String.class));
		}

		if (result != null) {
//			ObjectMapper mapperw = new ObjectMapper();
//			System.err.println(
//					"new API instance: " + mapperw.writerWithDefaultPrettyPrinter().writeValueAsString(result));
		} else {
			System.err.println("Failed to create API instance");
			System.exit(statuscode);
		}
	}

	@SuppressWarnings({ "unchecked", "unused" })
	private static ArrayList <LinkedHashMap<String, Object>> getApiPolicies(Client restClient, String authorizationHdr,
			String businessGroupId, String environmentId, String apiInstanceId) throws JsonProcessingException {
		WebTarget target = restClient.target(HTTPS_ANYPOINT_MULESOFT_COM).path("apimanager/api/v1/organizations")
				.path(businessGroupId).path("environments").path(environmentId).path("apis").path(apiInstanceId)
				.path("policies").queryParam("fullInfo", "false");
//		System.out.println(target.toString());

		Response response = target.request().header("Authorization", authorizationHdr)
				.accept(MediaType.APPLICATION_JSON).get();

		int statuscode = 500;
		ArrayList <LinkedHashMap<String, Object>> result = null;
		if (response != null) {
			statuscode = response.getStatus();
		}
		if (response != null && response.getStatus() == 200) {
			try {
				result = (ArrayList<LinkedHashMap<String, Object>>) response.readEntity(ArrayList.class); 
			} catch (Exception e) {
				System.err.println("***API ERROR*** " + response.toString());
				System.err.println("***INFO*** Policies list not available...check API and version in Exchange and API Manager.");
			}
		} else {
			System.err.println("Failed to get API policies (" + statuscode + ")");
			return null;
		}

		if (result != null) {
//			ObjectMapper mapperw = new ObjectMapper();
//			System.err.println("api policies: " + mapperw.writerWithDefaultPrettyPrinter().writeValueAsString(result));
			return result;
		} else {
			System.err.println("Failed to find API policies");
			return null;
		}
	}

	private static void addApiPolicies(Client restClient, String authorizationHdr, String businessGroupId,
			String environmentId, String apiInstanceId, String apiPolicies, ArrayList <LinkedHashMap<String, Object>> currentPolicies, boolean mule4OrAbove) {

		ArrayList<LinkedHashMap<String, Object>> policies;
		ObjectMapper mapper;
		TypeFactory factory;
		CollectionType type;

		factory = TypeFactory.defaultInstance();
		type = factory.constructCollectionType(ArrayList.class, LinkedHashMap.class);
		mapper = new ObjectMapper();

		InputStream is = null;
		File policyFile = new File(apiPolicies);
		String policiesStr = null;
		try {
			if (policyFile.exists()) {
				policiesStr = FileUtils.readFileToString(policyFile, "UTF-8");
			} else {
				is = ApiConfigTool.class.getClassLoader().getResourceAsStream(apiPolicies);
				policiesStr = IOUtils.toString(is, "UTF-8");
			}
//			System.err.println(policiesStr);
			policies = mapper.readValue(policiesStr, type);

			for (LinkedHashMap<String, Object> i : policies) {
				if (!policyExists(currentPolicies, i, mule4OrAbove)) {
					addApiPolicy(restClient, authorizationHdr, businessGroupId, environmentId, apiInstanceId, i,
							mule4OrAbove);
					System.err.println("***INFO*** " + i.get("assetId") + " policy added.");
				} else {
					System.err.println("***INFO*** " + i.get("assetId") + " already exists, add not performed.");
				}
			}

		} catch (Exception e) {
			System.err.println("Cannot use policies from file " + apiPolicies);
			e.printStackTrace(System.err);
			System.exit(1);
		} finally {
			if (is != null)
				IOUtils.closeQuietly(is);
		}

	}
	
	private static boolean policyExists(ArrayList <LinkedHashMap<String, Object>> currentPolicies, LinkedHashMap<String, Object> apiPolicy, boolean mule4OrAbove) {
		if (currentPolicies == null) return false;
		
		for (LinkedHashMap<String, Object> i : currentPolicies) {
			if (mule4OrAbove) {
				if (i.get("assetId").equals(apiPolicy.get("assetId"))) {
					return true;
				}
			} else {			
				if (i.get("assetId").equals(apiPolicy.get("policyTemplateId"))) {
					return true;
				}
			}
		}
		return false;
	}

	private static void addApiPolicy(Client restClient, String authorizationHdr, String businessGroupId,
			String environmentId, String apiInstanceId, LinkedHashMap<String, Object> apiPolicy, boolean mule4OrAbove)
			throws JsonProcessingException {

		String policyStr = null;
		LinkedHashMap<String, Object> newPolicy = new LinkedHashMap<String, Object>();
		newPolicy.putAll(apiPolicy);
		if (mule4OrAbove) {
			newPolicy.put("apiVersionId", apiInstanceId);
		}

		try {
			ObjectMapper mapperw = new ObjectMapper();
			policyStr = mapperw.writeValueAsString(newPolicy);
//			System.err.println("Setting policy " + policyStr);
			WebTarget target = restClient.target(HTTPS_ANYPOINT_MULESOFT_COM).path("apimanager/api/v1/organizations")
					.path(businessGroupId).path("environments").path(environmentId).path("apis").path(apiInstanceId)
					.path("policies");

			Response response = target.request().accept(MediaType.APPLICATION_JSON)
					.header("Authorization", authorizationHdr)
					.post(Entity.entity(policyStr, MediaType.APPLICATION_JSON));

			int statuscode = 500;
			if (response != null) {
				statuscode = response.getStatus();
			}
			if (response != null && (response.getStatus() == 201 || response.getStatus() == 409)) {
//				System.err.println(response.readEntity(String.class));
			} else {
				System.err.println("Failed to apply policy " + policyStr + ". (" + statuscode + ")");
				System.err.println(response.readEntity(String.class));
			}
		} catch (Exception e) {
			System.err.println("Cannot set policy:\n " + policyStr);
			e.printStackTrace(System.err);
		}
	}

	private static void addSlaTiers(Client restClient, String authorizationHdr, String businessGroupId,
			String environmentId, String apiInstanceId, String slaTiers, boolean mule4OrAbove) {

		ArrayList<LinkedHashMap<String, Object>> tiers;
		ObjectMapper mapper;
		TypeFactory factory;
		CollectionType type;

		factory = TypeFactory.defaultInstance();
		type = factory.constructCollectionType(ArrayList.class, LinkedHashMap.class);
		mapper = new ObjectMapper();

		InputStream is = null;
		File slaTiersFile = new File(slaTiers);
		String slaTierStr = null;
		try {
			if (slaTiersFile.exists()) {
				slaTierStr = FileUtils.readFileToString(slaTiersFile, "UTF-8");
			} else {
				is = ApiConfigTool.class.getClassLoader().getResourceAsStream(slaTiers);
				slaTierStr = IOUtils.toString(is, "UTF-8");
			}
//			System.err.println(slaTierStr);
			tiers = mapper.readValue(slaTierStr, type);

			for (LinkedHashMap<String, Object> i : tiers) {
				addSlaTier(restClient, authorizationHdr, businessGroupId, environmentId, apiInstanceId, i,
						mule4OrAbove);
			}

		} catch (Exception e) {
			System.err.println("Cannot use tiers from file " + slaTiers);
			e.printStackTrace(System.err);
			System.exit(1);
		} finally {
			if (is != null)
				IOUtils.closeQuietly(is);
		}

	}

	private static void addSlaTier(Client restClient, String authorizationHdr, String businessGroupId,
			String environmentId, String apiInstanceId, LinkedHashMap<String, Object> slaTier, boolean mule4OrAbove)
			throws JsonProcessingException {

		String slaTierStr = null;
		LinkedHashMap<String, Object> newTier = new LinkedHashMap<String, Object>();
		newTier.putAll(slaTier);
		if (mule4OrAbove) {
			newTier.put("apiVersionId", apiInstanceId);
		}

		try {
			ObjectMapper mapperw = new ObjectMapper();
			slaTierStr = mapperw.writeValueAsString(newTier);
//			System.err.println("Setting tier " + slaTierStr);
			WebTarget target = restClient.target(HTTPS_ANYPOINT_MULESOFT_COM).path("apimanager/api/v1/organizations")
					.path(businessGroupId).path("environments").path(environmentId).path("apis").path(apiInstanceId)
					.path("tiers");

			Response response = target.request().accept(MediaType.APPLICATION_JSON)
					.header("Authorization", authorizationHdr)
					.post(Entity.entity(slaTierStr, MediaType.APPLICATION_JSON));

			int statuscode = 500;
			if (response != null) {
				statuscode = response.getStatus();
			}
			if (response != null && (response.getStatus() == 201 || response.getStatus() == 409)) {
//				System.err.println(response.readEntity(String.class));
			} else {
				System.err.println("Failed to apply tier " + slaTierStr + ". (" + statuscode + ")");
				System.err.println(response.readEntity(String.class));
			}
		} catch (Exception e) {
			System.err.println("Cannot set tier:\n " + slaTierStr);
			e.printStackTrace(System.err);
		}
	}
}
