/*
 * Copyright (c) 2019-2021, ForgeRock, Inc., All rights reserved
 * Use subject to license terms.
 */

 /*
 * Disclaimer:
 *
 * The sample code described herein is provided on an "as is" basis, without 
 * warranty of any kind, to the fullest extent permitted by law. ForgeRock does 
 * not warrant or guarantee the individual success developers may have in 
 * implementing the sample code on their development platforms or in production 
 * configurations.
 *
 * ForgeRock does not warrant, guarantee or make any representations regarding 
 * the use, results of use, accuracy, timeliness or completeness of any data or 
 * information relating to the sample code. ForgeRock disclaims all warranties, 
 * expressed or implied, and in particular, disclaims all warranties of 
 * merchantability, and warranties related to the code, or any service or 
 * software related thereto.
 *
 * ForgeRock shall not be liable for any direct, indirect or consequential 
 * damages or costs of any type arising out of any action taken by you or others 
 * related to the sample code. 
 */
package com.forgerock.frdp.uma.client;

import com.forgerock.frdp.common.BasicData;
import com.forgerock.frdp.common.ConstantsIF;
import com.forgerock.frdp.common.Core;
import com.forgerock.frdp.common.CoreIF;
import com.forgerock.frdp.common.DataIF;
import com.forgerock.frdp.utils.JSON;
import com.forgerock.frdp.utils.STR;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Client Gateway
 * @author Scott Fehrman, ForgeRock, Inc.
 */
public abstract class Gateway extends Core implements GatewayIF {

   private final String CLASS = this.getClass().getName();
   protected final Logger _logger = Logger.getLogger(CLASS);
   protected final Level DEBUG_LEVEL = Level.FINE;
   protected WebTarget _webTargetRS = null;
   protected WebTarget _webTargetAS = null;
   protected JSONParser _parser = null;

   public Gateway(final Map<String, String> params) throws Exception {
      super(params);

      String METHOD = "Gateway(Map<String, String>)";

      _logger.entering(CLASS, METHOD);

      this.init();

      _logger.exiting(CLASS, METHOD);

      return;
   }

   public Gateway(final Properties props) throws Exception {
      super(props);

      String METHOD = "Gateway(Properties)";

      _logger.entering(CLASS, METHOD);

      this.init();

      _logger.exiting(CLASS, METHOD);

      return;
   }

   @Override
   public CoreIF copy() {
      throw new UnsupportedOperationException("Not supported yet.");
   }

   protected void abort(final String clazz, final String method,
      final String message) throws Exception {
      String msg = null;

      msg = (clazz == null ? NULL : clazz) + ":"
         + (method == null ? NULL : method) + ": "
         + (message == null ? NULL : message);

      _logger.severe(msg);

      throw new Exception(msg);
   }

   protected void cleanJSON(final JSONObject json) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      JSONObject jsonScopes = null;

      /*
       * JSON input ...
       * {
       *   "content": { ... },
       *   "meta": { ... },
       *   "message": "...",
       *   "token": "...",
       *   "scopes": {
       *     "request": [ ... ],
       *     "resource": [ ... ],
       *     "policy": [ ... ],
       *     "token": [ ... ]
       *   }
       * }
       * remove ...
       *   "token"
       *   "scopes.token"
       */
      _logger.entering(CLASS, METHOD);

      if (json != null) {
         json.remove(ConstantsIF.TOKEN);

         jsonScopes = JSON.getObject(json, ConstantsIF.SCOPES);

         if (jsonScopes != null && !jsonScopes.isEmpty()) {
            jsonScopes.remove(ConstantsIF.TOKEN);
         }
      }

      _logger.exiting(CLASS, METHOD);

      return;
   }

   protected String getUsernameFromSSOToken(final DataIF dataInput)
      throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String username = null;
      DataIF dataOutput = null;
      JSONObject jsonOutput = null;

      /*
       * dataInput JSON ...
       * {
       *   "data": {
       *     ...,
       *     "ssotoken": "...", // AM sso token
       *     ...
       *   }
       * }
       * dataOutput JSON ...
       * {
       *   "data": {
       *     "valid":true,
       *     "sessionUid":"209331b0-6d31-4740-8d5f-740286f6e69f-326295",
       *     "uid":"demo",
       *     "realm":"/"
       *   }
       * }
       * -or-
       * {
       *   "valid":false
       * }
       */
      _logger.entering(CLASS, METHOD);

      dataOutput = this.validateSSOToken(dataInput);

      if (dataOutput != null && dataOutput.getState() == STATE.SUCCESS) {
         jsonOutput = dataOutput.getJSON();
         if (jsonOutput != null && !jsonOutput.isEmpty()) {
            username = JSON.getString(jsonOutput,
               ConstantsIF.DATA + "." + ConstantsIF.UID);
         }
      }

      _logger.exiting(CLASS, METHOD);

      return username;
   }

   protected DataIF validateSSOToken(final DataIF dataInput)
      throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String path = null;
      String sso_token = null;
      DataIF dataOutput = null;
      JSONObject jsonInput = null;
      JSONObject jsonData = null;
      WebTarget target = null;
      MultivaluedMap headers = null;
      Builder builder = null;
      Response response = null;

      /*
       * dataInput JSON ...
       * {
       *   "data": {
       *     ...,
       *     "ssotoken": "...", // AM sso token
       *     ...
       *   }
       * }
       * dataOutput JSON ...
       * {
       *   "data": {
       *     "valid":true,
       *     "sessionUid":"209331b0-6d31-4740-8d5f-740286f6e69f-326295",
       *     "uid":"demo",
       *     "realm":"/"
       *   }
       * }
       * -or-
       * {
       *   "valid":false
       * }
       */
      _logger.entering(CLASS, METHOD);

      jsonInput = dataInput.getJSON();

      jsonData = JSON.getObject(jsonInput, ConstantsIF.DATA);

      if (jsonData == null || jsonData.isEmpty()) {
         this.abort(CLASS, METHOD,
            "JSON data is empty or null");
      }

      sso_token = JSON.getString(jsonData, ConstantsIF.SSO_TOKEN);

      if (STR.isEmpty(sso_token)) {
         this.abort(CLASS, METHOD,
            "sso_token is empty");
      }

      /*
       * Get session details related to the SSO token
       * curl -X POST \
       * https://.../openam/json/realms/root/sessions?_action=validate \
       * --header "Content-type: application/json" \
       * --header "Accept-API-Version: resource=2.1,protocol=1.0" \
       * --header "iPlanetDirectoryPro=sso_token" \
       * --data '{ "tokenId": "S9DSqBv...AlMxAAA.*" }' \
       */
      path = this.getParamNotEmpty(GatewayIF.AS_ENDPOINTS_SESSIONS);

      target = _webTargetAS.path(path);
      target = target.queryParam(ConstantsIF._ACTION, ConstantsIF.VALIDATE);

      headers = new MultivaluedHashMap();
      headers.add(ConstantsIF.ACCEPT_API_VERSION,
         this.getParamNotEmpty(GatewayIF.AS_VERSION_SESSIONS));
      headers.add(this.getParamNotEmpty(GatewayIF.AS_COOKIE),
         sso_token);

      builder = target.request(MediaType.APPLICATION_JSON_TYPE);
      builder.headers(headers);

      response = builder.post(Entity.entity("", MediaType.APPLICATION_JSON_TYPE));

      dataOutput = this.getDataFromResponse(response);

      _logger.exiting(CLASS, METHOD);

      return dataOutput;
   }

   protected DataIF getPermTicket(final DataIF dataInput)
      throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String path = null;
      String resourceId = null;
      String method = null;
      String scopes = null;
      StringBuilder buf = null;
      JSONObject jsonInput = null;
      JSONObject jsonOutput = null;
      JSONObject jsonData = null;
      JSONObject jsonScopes = null;
      Builder builder = null;
      Response response = null;
      WebTarget target = null;
      MultivaluedMap headers = null;
      DataIF dataOutput = null;

      /*
       * input ...
       * {
       *   "data": {
       *     "method": "...", // HTTP Method
       *     "sso_token": "...", // AM SSO Session Token
       *     "scopes": [ "...", ... ], // Array of Strings (optional)
       *     "resource_id": "..." // Resource Id (GUID)
       *   }
       * }
       * output ...
       * {
       *   "data": {
       *     "ticket": "...", // the Permission Ticket
       *     "as_uri": "...", /// URL to the Authorization Server
       *     "scopes": [ "...", ... ],
       *     "message": "...",
       *     "token": "..."
       *   },
       *   "headers": {
       *   },
       *   "response": xxx
       * }
       */
      _logger.entering(CLASS, METHOD);

      jsonInput = dataInput.getJSON();

      jsonData = JSON.getObject(jsonInput, ConstantsIF.DATA);

      scopes = this.getScopesAsString(JSON.getArray(jsonData, ConstantsIF.SCOPES));

      resourceId = JSON.getString(jsonData, ConstantsIF.RESOURCE_ID);

      /*
       * Get a Permission Ticket from the Resource Server
       * curl https://.../rest/share/resoures/{id}/?scopes={scopes}
       * -H "x-frdp-ssostoken xxxx"
       * expect a 401 NOT AUTHORIZED, with the Permission Ticket "token"
       */
      path = this.getParamNotEmpty(GatewayIF.RS_ENDPOINTS_RESOURCES);

      target = _webTargetRS.path(path).path(resourceId);

      target = target.queryParam(ConstantsIF.SCOPES, scopes);

      headers = new MultivaluedHashMap();
      headers.add(this.getParamNotEmpty(GatewayIF.RS_HEADERS_SSOTOKEN),
         JSON.getString(jsonData, ConstantsIF.SSO_TOKEN));

      builder = target.request(MediaType.APPLICATION_JSON_TYPE);

      builder.headers(headers);

      method = JSON.getString(jsonData, ConstantsIF.METHOD);

      switch (method) {
         case ConstantsIF.GET: {
            response = builder.get();
            break;
         }
         default: {
            this.abort(CLASS, METHOD,
               "Unsupported HTTP Method: '" + method + "'");
         }
      }

      /*
       * possible responses:
       * - 400: BAD REQUEST ... invalid scope(s)
       * - 401: UNAUTHORIZED ... scope(s) not in token, missing/bad session
       * - 404: NOT FOUND ... the resource does not exist
       * - 409: CONFLICT ... mixed scopes
       */
      dataOutput = this.getDataFromResponse(response);

      if (dataOutput.getState() != STATE.NOTAUTHORIZED) {
         jsonOutput = dataOutput.getJSON();

         jsonData = JSON.getObject(jsonOutput, ConstantsIF.DATA);

         this.cleanJSON(jsonData);
      }

      _logger.exiting(CLASS, METHOD);

      return dataOutput;
   }

   protected DataIF getAuthzCode(final DataIF dataInput)
      throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String path = null;
      String sso_token = null;
      String code = null;
      DataIF dataOutput = null;
      JSONObject jsonInput = null;
      JSONObject jsonOutput = null;
      JSONObject jsonData = null;
      WebTarget target = null;
      MultivaluedMap headers = null;
      Form form = null;
      Builder builder = null;
      Response response = null;

      /*
       * input ...
       * {
       *   "data": {
       *     "method": "...", // HTTP Method
       *     "sso_token": "...", // AM SSO Session Token
       *     "scopes": [ "...", ... ], // Array of Strings (optional)
       *     "resource_id": "...", // Resource Id (GUID)
       *     "ticket": "..." // Permission Ticket
       *   }
       * }
       * output ...
       * {
       *   "data": {
       *     "code": "...", // Authorization code
       *   },
       *   "headers": {
       *   },
       *   "response": xxx
       * }
       */
      _logger.entering(CLASS, METHOD);

      jsonInput = dataInput.getJSON();

      jsonData = JSON.getObject(jsonInput, ConstantsIF.DATA);

      sso_token = JSON.getString(jsonData, ConstantsIF.SSO_TOKEN);

      /*
       * Get authorization code, for the sso token, from Authorization Server
       * curl https://.../openam/oauth2/realms/root/authorize
       * Expect 302 Found / redirect ... 
       * {
       *   "headers": {
       *     "Location": "http...&code=...";
       *     ...
       *   }
       * }
       */
      path = this.getParamNotEmpty(GatewayIF.AS_ENDPOINTS_AUTHORIZE);

      target = _webTargetAS.path(path);

      headers = new MultivaluedHashMap();
      headers.add(ConstantsIF.ACCEPT_API_VERSION,
         this.getParamNotEmpty(GatewayIF.AS_VERSION_AUTHORIZE));
      headers.add(ConstantsIF.CONTENT_TYPE,
         ConstantsIF.APPLICATION_FORM_URLENCODED);
      headers.add(this.getParamNotEmpty(GatewayIF.AS_COOKIE),
         sso_token);
      headers.add(ConstantsIF.COOKIE,
         this.getParamNotEmpty(GatewayIF.AS_COOKIE) + "=" + sso_token);

      builder = target.request(MediaType.WILDCARD_TYPE);
      builder.headers(headers);

      form = new Form();
      form.param(ConstantsIF.RESPONSE_TYPE,
         ConstantsIF.CODE);
      form.param(ConstantsIF.SAVE_CONSENT,
         ConstantsIF.OFF);
      form.param(ConstantsIF.DECISION,
         ConstantsIF.ALLOW);
      form.param(ConstantsIF.CLIENT_ID,
         this.getParamNotEmpty(GatewayIF.AS_OAUTH2_CLIENT_ID));
      form.param(ConstantsIF.REDIRECT_URI,
         this.getParamNotEmpty(GatewayIF.AS_OAUTH2_CLIENT_REDIRECT));
      form.param(ConstantsIF.SCOPE,
         ConstantsIF.OPENID);
      form.param(ConstantsIF.CSRF,
         sso_token);

      response = builder.post(Entity.entity(form,
         MediaType.APPLICATION_FORM_URLENCODED_TYPE));

      /*
       * possible responses:
       * - 302: FOUND ... redirect
       * {
       *   "data": { ... },
       *   "headers": { ... },
       *   "response": XXX
       * }
       */
      dataOutput = this.getDataFromResponse(response);

      if (dataOutput.getState() != STATE.SUCCESS) {
         this.abort(CLASS, METHOD,
            "Output state is NOT success: "
            + dataOutput.getState().toString() + " : "
            + dataOutput.getStatus());
      }

      jsonOutput = dataOutput.getJSON();

      code = this.getCodeFromLocationHeader(jsonOutput);

      jsonData = JSON.getObject(jsonOutput, ConstantsIF.DATA);

      jsonData.put(ConstantsIF.CODE, code);

      _logger.exiting(CLASS, METHOD);

      return dataOutput;
   }

   protected DataIF getClaimToken(final DataIF dataInput)
      throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String code = null;
      String path = null;
      String basicDecode = null;
      String basicEncode = null;
      DataIF dataOutput = null;
      JSONObject jsonInput = null;
      JSONObject jsonData = null;
      WebTarget target = null;
      MultivaluedMap headers = null;
      Builder builder = null;
      Form form = null;
      Response response = null;
      Encoder encoder = null;

      /*
       * input ...
       * {
       *   "data": {
       *     "method": "...", // HTTP Method
       *     "sso_token": "...", // AM SSO Session Token
       *     "scopes": [ "...", ... ], // Array of Strings (optional)
       *     "resource_id": "..." // Resource Id (GUID)
       *     "ticket": "...", // Permission Ticket
       *     "code": "...", // Authorization Code
       *   }
       * }
       * output ...
       * {
       *   "data": {
       *     "id_token": "...", // Claim Token
       *   },
       *   "response": xxx
       * }
       */
      _logger.entering(CLASS, METHOD);

      jsonInput = dataInput.getJSON();

      jsonData = JSON.getObject(jsonInput, ConstantsIF.DATA);

      code = JSON.getString(jsonData, ConstantsIF.CODE);

      if (STR.isEmpty(code)) {
         this.abort(CLASS, METHOD,
            "Authorization Code is empty");
      }

      /*
       * Get Claim Token, using Authorization Code, from Authorization Server
       * curl https://.../openam/oauth2/realms/root/access_token
       * Expect 200 OK ... 
       * {
       *   "data": {
       *     "id_token": "..."; // Claim Token
       *     ...
       *   }
       * }
       */
      encoder = Base64.getEncoder();

      basicDecode = this.getParamNotEmpty(GatewayIF.AS_OAUTH2_CLIENT_ID)
         + ":" + this.getParamNotEmpty(GatewayIF.AS_OAUTH2_CLIENT_SECRET);

      basicEncode = encoder.encodeToString(basicDecode.getBytes());

      path = this.getParamNotEmpty(GatewayIF.AS_ENDPOINTS_ACCESS);

      target = _webTargetAS.path(path);

      headers = new MultivaluedHashMap();
      headers.add(ConstantsIF.ACCEPT_API_VERSION,
         this.getParamNotEmpty(GatewayIF.AS_VERSION_ACCESS));
      headers.add(ConstantsIF.CONTENT_TYPE,
         ConstantsIF.APPLICATION_FORM_URLENCODED);
      headers.add(ConstantsIF.AUTHORIZATION,
         "Basic " + basicEncode);

      builder = target.request(MediaType.WILDCARD_TYPE);
      builder.headers(headers);

      form = new Form();
      form.param(ConstantsIF.GRANT_TYPE,
         ConstantsIF.AUTHORIZATION_CODE);
      form.param(ConstantsIF.REDIRECT_URI,
         this.getParamNotEmpty(GatewayIF.AS_OAUTH2_CLIENT_REDIRECT));
      form.param(ConstantsIF.CODE, code);

      response = builder.post(Entity.entity(form,
         MediaType.APPLICATION_FORM_URLENCODED_TYPE));

      dataOutput = this.getDataFromResponse(response);

      if (dataOutput.getState() != STATE.SUCCESS) {
         this.abort(CLASS, METHOD,
            "Output state is NOT success: "
            + dataOutput.getState().toString() + " : "
            + dataOutput.getStatus());
      }

      _logger.exiting(CLASS, METHOD);

      return dataOutput;
   }

   protected DataIF getRPT(final DataIF dataInput)
      throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String permTicket = null;
      String claimToken = null;
      String path = null;
      String basicDecode = null;
      String basicEncode = null;
      String scopes = null;
      DataIF dataOutput = null;
      JSONObject jsonInput = null;
      JSONObject jsonData = null;
      WebTarget target = null;
      MultivaluedMap headers = null;
      Builder builder = null;
      Form form = null;
      Response response = null;
      Encoder encoder = null;

      /*
       * input ...
       * {
       *   "data": {
       *     "method": "...", // HTTP Method
       *     "sso_token": "...", // AM SSO Session Token
       *     "scopes": [ "...", ... ], // Array of Strings (optional)
       *     "resource_id": "..." // Resource Id (GUID)
       *     "ticket": "...", // Permission Ticket
       *     "code": "..." // Authorization Code
       *     "id_token": "..." // Claim Token
       *   }
       * }
       * output ...
       * {
       *   "data": {
       *     "access_token": "...", // Requesting Party Token (an access token)
       *   },
       *   "response": xxx
       * }
       */
      _logger.entering(CLASS, METHOD);

      jsonInput = dataInput.getJSON();

      jsonData = JSON.getObject(jsonInput, ConstantsIF.DATA);

      permTicket = JSON.getString(jsonData,
         ConstantsIF.DATA + "." + ConstantsIF.TICKET);

      if (STR.isEmpty(permTicket)) {
         this.abort(CLASS, METHOD,
            "Permission Ticket is empty");
      }

      claimToken = JSON.getString(jsonData,
         ConstantsIF.DATA + "." + ConstantsIF.ID_TOKEN);

      if (STR.isEmpty(claimToken)) {
         this.abort(CLASS, METHOD,
            "Claim Token is empty");
      }

      scopes = this.getScopesAsString(JSON.getArray(jsonData, ConstantsIF.SCOPES));

      if (STR.isEmpty(scopes)) {
         this.abort(CLASS, METHOD,
            "Missing scope(s)");
      }

      /*
       * Get Requesitng Party Token, using Claim Token and Permission Ticket
       * curl https://.../openam/oauth2/realms/root/access_token
       * Expect 200 OK ... 
       * {
       *   "data": {
       *     "access_token": "..."; // Requesting Party Token (an access token)
       *     ...
       *   }
       * }
       */
      encoder = Base64.getEncoder();

      basicDecode = this.getParamNotEmpty(GatewayIF.AS_OAUTH2_CLIENT_ID)
         + ":" + this.getParamNotEmpty(GatewayIF.AS_OAUTH2_CLIENT_SECRET);

      basicEncode = encoder.encodeToString(basicDecode.getBytes());

      path = this.getParamNotEmpty(GatewayIF.AS_ENDPOINTS_ACCESS);

      target = _webTargetAS.path(path);

      headers = new MultivaluedHashMap();
      headers.add(ConstantsIF.ACCEPT_API_VERSION,
         this.getParamNotEmpty(GatewayIF.AS_VERSION_ACCESS));
      headers.add(ConstantsIF.CONTENT_TYPE,
         ConstantsIF.APPLICATION_FORM_URLENCODED);
      headers.add(ConstantsIF.AUTHORIZATION,
         "Basic " + basicEncode);

      builder = target.request(MediaType.WILDCARD_TYPE);
      builder.headers(headers);

      form = new Form();
      form.param(ConstantsIF.GRANT_TYPE,
         "urn:ietf:params:oauth:grant-type:uma-ticket");
      form.param(ConstantsIF.TICKET,
         permTicket);
      form.param(ConstantsIF.SCOPE,
         scopes);
      form.param(ConstantsIF.CLAIM_TOKEN,
         claimToken);
      form.param(ConstantsIF.CLAIM_TOKEN_FORMAT,
         "http://openid.net/specs/openid-connect-core-1_0.html#IDToken");

      response = builder.post(Entity.entity(form,
         MediaType.APPLICATION_FORM_URLENCODED_TYPE));

      dataOutput = this.getDataFromResponse(response);

      if (dataOutput.getState() == STATE.SUCCESS
         || dataOutput.getState() == STATE.WARNING) {
         // return the data
      } else {
         this.abort(CLASS, METHOD,
            "Output state is NOT success: "
            + dataOutput.getState().toString() + " : "
            + dataOutput.getStatus());
      }

      _logger.exiting(CLASS, METHOD);

      return dataOutput;
   }

   protected DataIF getResource(final DataIF dataInput)
      throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String scopes = null;
      String resourceId = null;
      String method = null;
      String access_token = null;
      String path = null;
      DataIF dataOutput = null;
      JSONObject jsonInput = null;
      JSONObject jsonData = null;
      MultivaluedMap headers = null;
      WebTarget target = null;
      Builder builder = null;
      Response response = null;

      /*
       * input ...
       * {
       *   "data": {
       *     "method": "...", // HTTP Method
       *     "sso_token": "...", // AM SSO Session Token
       *     "scopes": [ "...", ... ], // Array of Strings (optional)
       *     "resource_id": "..." // Resource Id (GUID)
       *     "ticket": "...", // Permission Ticket
       *     "code": "..." // Authorization Code
       *     "id_token": "..." // Claim Token,
       *     "access_token": "..." // Requesting Party Token
       *   }
       * }
       * output ...
       * {
       *   "data": {
       *     "meta": { ... },
       *     "content": { ...},
       *     "scopes": [ ... ],
       *     "message": "..."
       *   },
       *   "response": XXX
       * }
       */
      _logger.entering(CLASS, METHOD);

      jsonInput = dataInput.getJSON();

      jsonData = JSON.getObject(jsonInput, ConstantsIF.DATA);

      scopes = this.getScopesAsString(JSON.getArray(jsonData, ConstantsIF.SCOPES));

      if (STR.isEmpty(scopes)) {
         this.abort(CLASS, METHOD,
            "Missing scope(s)");
      }

      resourceId = JSON.getString(jsonData, ConstantsIF.RESOURCE_ID);

      access_token = JSON.getString(jsonData, ConstantsIF.ACCESS_TOKEN);

      method = JSON.getString(jsonData, ConstantsIF.METHOD);

      /*
       * Get the Resource from the Resource Server
       * curl https://.../rest/share/resoures/{id}/?scopes={scopes}
       * -H "x-frdp-ssostoken xxxx"
       * expect a 200 OK
       */
      path = this.getParamNotEmpty(GatewayIF.RS_ENDPOINTS_RESOURCES);

      target = _webTargetRS.path(path).path(resourceId);

      target = target.queryParam(ConstantsIF.SCOPES, scopes);

      headers = new MultivaluedHashMap();
      headers.add(this.getParamNotEmpty(GatewayIF.RS_HEADERS_SSOTOKEN),
         JSON.getString(jsonData, ConstantsIF.SSO_TOKEN));
      headers.add(this.getParamNotEmpty(GatewayIF.RS_HEADERS_RPT),
         access_token);

      builder = target.request(MediaType.APPLICATION_JSON_TYPE);

      builder.headers(headers);

      switch (method) {
         case ConstantsIF.GET: {
            response = builder.get();
            break;
         }
         default: {
            this.abort(CLASS, METHOD,
               "Unsupported HTTP Method: '" + method + "'");
         }
      }

      /*
       * possible responses:
       * - 200: OK ... success
       * - 400: BAD REQUEST ... invalid scope(s)
       * - 401: UNAUTHORIZED ... scope(s) not in token, missing/bad session
       * - 404: NOT FOUND ... the resource does not exist
       * - 409: CONFLICT ... mixed scopes
       */
      dataOutput = this.getDataFromResponse(response);

      _logger.exiting(CLASS, METHOD);

      return dataOutput;
   }

   protected void validateInput(final DataIF dataInput)
      throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String value = null;
      String[] attrNames = {
         ConstantsIF.SSO_TOKEN,
         ConstantsIF.RESOURCE_ID,
         ConstantsIF.METHOD
      };
      JSONObject jsonInput = null;
      JSONObject jsonData = null;

      _logger.entering(CLASS, METHOD);

      if (dataInput == null) {
         this.abort(CLASS, METHOD,
            "Data input is null");
      }

      jsonInput = dataInput.getJSON();

      if (jsonInput == null || jsonInput.isEmpty()) {
         this.abort(CLASS, METHOD,
            "JSON Input is null or empty");
      }

      jsonData = JSON.getObject(jsonInput, ConstantsIF.DATA);

      if (jsonData == null || jsonData.isEmpty()) {
         this.abort(CLASS, METHOD,
            "JSON Data is null or empty");
      }

      for (String name : attrNames) {
         value = JSON.getString(jsonData, name);

         if (STR.isEmpty(value)) {
            this.abort(CLASS, METHOD,
               "Attribute '" + name + "' is empty");
         }
      }

      if (JSON.getArray(jsonData, ConstantsIF.SCOPES) == null) {
         this.abort(CLASS, METHOD,
            "Scopes Array is null");
      }

      _logger.exiting(CLASS, METHOD);

      return;
   }

   private DataIF getDataFromResponse(final Response response)
      throws Exception {
      int status = 0;
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String entity = null;
      String value = null;
      JSONObject jsonData = null;
      JSONObject jsonOutput = null;
      JSONObject jsonHeaders = null;
      DataIF dataOutput = null;

      _logger.entering(CLASS, METHOD);

      if (response == null) {
         this.abort(CLASS, METHOD,
            "Response is null");
      }

      jsonOutput = new JSONObject();

      dataOutput = new BasicData();
      dataOutput.setJSON(jsonOutput);

      entity = response.readEntity(String.class);

      if (STR.isEmpty(entity)) {
         entity = NULL;
         jsonOutput.put(ConstantsIF.DATA, new JSONObject());
      } else {
         jsonData = this.parseEntity(entity);

         if (jsonData != null) {
            this.cleanJSON(jsonData);
            jsonOutput.put(ConstantsIF.DATA, jsonData);
         } else {
            jsonOutput.put(ConstantsIF.DATA, new JSONObject());
         }
      }

      status = response.getStatus();

      jsonOutput.put(ConstantsIF.RESPONSE, status);

      switch (status) {
         case 200:
         case 201:
         case 204: { // OK, CREATED, NO CONTENT
            dataOutput.setState(STATE.SUCCESS);
            dataOutput.setStatus("Response: "
               + response.getStatus() + ", "
               + response.getStatusInfo().toString());
            break;
         }
         case 302: { // FOUND (REDIRECT)
            jsonHeaders = new JSONObject();

            for (String s : response.getHeaders().keySet()) {
               if (!STR.isEmpty(s)) {
                  value = response.getHeaderString(s);
                  if (!STR.isEmpty(value)) {
                     jsonHeaders.put(s, value);
                  }
               }
            }

            jsonOutput.put(ConstantsIF.HEADERS, jsonHeaders);

            dataOutput.setState(STATE.SUCCESS);
            dataOutput.setStatus("REDIRECT: "
               + response.getStatus() + ", "
               + response.getStatusInfo().toString()
               + ": '" + entity != null ? entity : "(null)" + "'");
            break;
         }
         case 400: { // BAD REQUEST
            dataOutput.setError(true);
            dataOutput.setState(STATE.FAILED);
            break;
         }
         case 401: { // UNAUTHORIZED
            dataOutput.setError(true);
            dataOutput.setState(STATE.NOTAUTHORIZED);
            break;
         }
         case 403: { // FORBIDDEN
            dataOutput.setError(true);
            dataOutput.setState(STATE.WARNING);
            break;
         }
         case 404: { // NOT FOUND
            dataOutput.setError(true);
            dataOutput.setState(STATE.NOTEXIST);
            break;
         }
         case 409: { // CONFLICT
            dataOutput.setError(true);
            dataOutput.setState(STATE.FAILED);
            break;
         }
         default: { // everything else
            dataOutput.setError(true);
            dataOutput.setState(STATE.ERROR);
            break;
         }
      }

      if (dataOutput.isError()) {
         dataOutput.setStatus(response.getStatus() + ", "
            + response.getStatusInfo().toString()
            + ": '" + entity != null ? entity : "(null)" + "'");
      }

      _logger.exiting(CLASS, METHOD);

      return dataOutput;
   }

   private JSONObject parseEntity(final String entity) {
      Object obj = null;
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      JSONObject jsonOutput = null;

      _logger.entering(CLASS, METHOD);

      if (!STR.isEmpty(entity)) {
         try {
            obj = _parser.parse(entity);
         } catch (Exception ex) {
            obj = null;
         }

         if (obj != null) {
            if (obj instanceof JSONObject) {
               jsonOutput = (JSONObject) obj;
            } else if (obj instanceof JSONArray) {
               jsonOutput = new JSONObject();
               jsonOutput.put(ConstantsIF.RESULTS, (JSONArray) obj);
            }
         }
      }

      _logger.exiting(CLASS, METHOD);

      return jsonOutput;
   }

   private Map<String, String> getQueryParams(URL url)
      throws UnsupportedEncodingException {
      int index = 0;
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String query = url.getQuery();
      String[] pairs = query.split("&");
      Map<String, String> query_pairs = new LinkedHashMap<>();

      _logger.entering(CLASS, METHOD);

      for (String pair : pairs) {
         index = pair.indexOf("=");
         query_pairs.put(
            URLDecoder.decode(pair.substring(0, index), "UTF-8"),
            URLDecoder.decode(pair.substring(index + 1), "UTF-8"));
      }

      _logger.exiting(CLASS, METHOD);

      return query_pairs;
   }

   private String getCodeFromLocationHeader(final JSONObject json)
      throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String location = null;
      String code = null;
      JSONObject jsonHeaders = null;
      URL url = null;
      Map<String, String> queryParams = null;

      _logger.entering(CLASS, METHOD);

      jsonHeaders = JSON.getObject(json, ConstantsIF.HEADERS);

      if (jsonHeaders == null || jsonHeaders.isEmpty()) {
         this.abort(CLASS, METHOD,
            "Response Headers are null or empty");
      }

      if (jsonHeaders.containsKey("location")) {
         location = JSON.getString(jsonHeaders, "location");
      } else if (jsonHeaders.containsKey("Location")) {
         location = JSON.getString(jsonHeaders, "Location");
      } else {
         this.abort(CLASS, METHOD,
            "Response Headers does not have a 'location' attribute");
      }

      url = new URL(location);

      queryParams = this.getQueryParams(url);

      if (queryParams.containsKey(ConstantsIF.CODE)) {
         code = queryParams.get(ConstantsIF.CODE);
         if (STR.isEmpty(code)) {
            this.abort(CLASS, METHOD,
               "Query param 'code' is empty");
         }
      } else {
         this.abort(CLASS, METHOD,
            "Query param 'code' is missing");
      }

      _logger.exiting(CLASS, METHOD);

      return code;
   }

   private String getScopesAsString(final JSONArray scopes) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      StringBuilder buf = new StringBuilder();

      _logger.entering(CLASS, METHOD);

      if (scopes != null && !scopes.isEmpty()) {
         for (Object o : scopes) {
            if (o != null && o instanceof String && !STR.isEmpty((String) o)) {
               if (buf.length() > 0) {
                  buf.append(" ");
               }
               buf.append((String) o);
            }
         }
      }

      _logger.exiting(CLASS, METHOD);

      return buf.toString();
   }

   private void init()
      throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      ClientConfig config = null;
      Client client = null;
      StringBuilder baseRS = new StringBuilder();
      StringBuilder baseAS = new StringBuilder();

      _logger.entering(CLASS, METHOD);

      config = new ClientConfig();
      config.property(ClientProperties.FOLLOW_REDIRECTS, false);

      client = ClientBuilder.newClient(config);

      baseRS
         .append(this.getParamNotEmpty(GatewayIF.RS_CONNECT_PROTOCOL))
         .append("://")
         .append(this.getParamNotEmpty(GatewayIF.RS_CONNECT_HOST))
         .append(":")
         .append(this.getParamNotEmpty(GatewayIF.RS_CONNECT_PORT));

      baseAS
         .append(this.getParamNotEmpty(GatewayIF.AS_CONNECT_PROTOCOL))
         .append("://")
         .append(this.getParamNotEmpty(GatewayIF.AS_CONNECT_HOST))
         .append(":")
         .append(this.getParamNotEmpty(GatewayIF.AS_CONNECT_PORT));

      _webTargetRS = client
         .target(baseRS.toString())
         .path(this.getParamNotEmpty(GatewayIF.RS_CONNECT_PATH));

      _webTargetAS = client
         .target(baseAS.toString())
         .path(this.getParamNotEmpty(GatewayIF.AS_CONNECT_PATH));

      _parser = new JSONParser();

      this.setState(STATE.READY);
      this.setStatus("Initialization complete");

      _logger.exiting(CLASS, METHOD);

      return;
   }
}
