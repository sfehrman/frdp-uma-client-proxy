/*
 * Copyright (c) 2019-2021, ForgeRock, Inc., All rights reserved
 * Use subject to license terms.
 */

package com.forgerock.frdp.uma.client.proxy;

import com.forgerock.frdp.common.BasicData;
import com.forgerock.frdp.common.ConstantsIF;
import com.forgerock.frdp.common.DataIF;
import com.forgerock.frdp.uma.client.CachingGateway;
import com.forgerock.frdp.uma.client.GatewayIF;
import com.forgerock.frdp.utils.STR;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * <pre>
 * PATH: .../rest/share/resources/{id}
 * PATH: .../rest/share/resources/{id}/policy 
 * PATH: .../rest/share/owners/{id}/discover 
 * PATH: .../rest/share/withme
 * </pre>
 * @author Scott Fehrman, ForgeRock, Inc.
 */
@Path(ConstantsIF.SHARE)
public class ShareResource implements ProxyIF {

   private Properties _props = null;
   private GatewayIF _gateway = null;
   private final String CLASS = this.getClass().getName();
   private final Logger _logger = Logger.getLogger(this.getClass().getName());
   private static final String CTX_ATTR_PROPS = "com.forgerock.frdp.uma.client.props";
   private static final String CTX_ATTR_GATEWAY = "com.forgerock.frdp.uma.client.gateway";
   private static final String PROP_FILE = "config/proxy.properties";
   private static final String PROP_VAR_OWNER = "__owner__";
   private static final String PROP_VAR_RESOURCE = "__resource__";

   @Context
   private ServletContext _servletCtx;
   @Context
   private HttpHeaders _httpHdrs;
   @Context
   private UriInfo _uriInfo;

   /**
    * Creates a new instance of ShareResource
    */
   public ShareResource() {
      String METHOD = "ShareResource()";

      _logger.entering(CLASS, METHOD);
      _logger.exiting(CLASS, METHOD);

      return;
   }

   @GET // resources/{id}?scopes=scope+list
   @Path("/" + ConstantsIF.RESOURCES + "/{" + ConstantsIF.ID + "}")
   @Produces(MediaType.APPLICATION_JSON)
   public Response getResource(@PathParam(ConstantsIF.ID) String resourceUid,
      @QueryParam(ConstantsIF.SCOPES) String scopes) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String ssoHdrName = null;
      String ssoHdrValue = null;
      Response response = null;
      JSONObject jsonData = null;
      JSONObject jsonInput = null;
      JSONArray arrayScopes = null;
      DataIF input = null;
      DataIF output = null;

      _logger.entering(CLASS, METHOD);

      this.load();

      arrayScopes = new JSONArray();

      if (!STR.isEmpty(scopes)) {
         arrayScopes.addAll(Arrays.asList(scopes.split(" ")));
      }

      ssoHdrName = _props.getProperty(ProxyIF.RS_HEADERS_SSOTOKEN);

      ssoHdrValue = this.getHeaderValue(ssoHdrName);

      jsonData = new JSONObject();
      jsonData.put(ConstantsIF.METHOD, "get"); // String
      jsonData.put(ConstantsIF.SSO_TOKEN, ssoHdrValue); // String
      jsonData.put(ConstantsIF.RESOURCE_ID, resourceUid); // String
      jsonData.put(ConstantsIF.SCOPES, arrayScopes); // Array of Strings

      jsonInput = new JSONObject();
      jsonInput.put(ConstantsIF.DATA, jsonData);

      input = new BasicData();
      input.setJSON(jsonInput);

      try {
         output = _gateway.process(input);
      } catch (Exception ex) {
         this.abort(METHOD,
            "Gateway exception: " + ex.getMessage(),
            Status.INTERNAL_SERVER_ERROR);
      }

      response = this.getResponse(output);

      _logger.exiting(CLASS, METHOD);

      return response;
   }

   @DELETE // .../resources/{id}/policy
   @Path("/" + ConstantsIF.RESOURCES + "/{" + ConstantsIF.ID + "}/" + ConstantsIF.POLICY)
   public Response deletePolicy(@PathParam(ConstantsIF.ID) String resourceUid) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      WebTarget target = null;
      Builder builder = null;
      Response response = null;

      _logger.entering(CLASS, METHOD);

      this.load();

      target = this.getTarget(
         _props.getProperty(ProxyIF.RS_ENDPOINTS_SHARE_POLICY)
            .replaceAll(PROP_VAR_RESOURCE, resourceUid));

      builder = target.request(MediaType.WILDCARD_TYPE);
      builder.headers(this.getHeaders());

      response = builder.delete();

      _logger.exiting(CLASS, METHOD);

      return response;
   }

   @GET // .../owners/{id}/discover
   @Produces(MediaType.APPLICATION_JSON)
   @Path("/" + ConstantsIF.OWNERS + "/{" + ConstantsIF.ID + "}/" + ConstantsIF.DISCOVER)
   public Response useDiscover(@PathParam(ConstantsIF.ID) String owner) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      WebTarget target = null;
      Builder builder = null;
      Response response = null;

      _logger.entering(CLASS, METHOD);

      this.load();

      target = this.getTarget(
         _props.getProperty(ProxyIF.RS_ENDPOINTS_SHARE_DISCOVER)
            .replaceAll(PROP_VAR_OWNER, owner));

      builder = target.request(MediaType.WILDCARD_TYPE);
      builder.headers(this.getHeaders());

      response = builder.get();

      _logger.exiting(CLASS, METHOD);

      return response;
   }

   @GET // .../withme
   @Produces(MediaType.APPLICATION_JSON)
   @Path("/withme")
   public Response getSharedWithme() {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      WebTarget target = null;
      Builder builder = null;
      Response response = null;

      _logger.entering(CLASS, METHOD);

      this.load();

      target = this.getTarget(_props.getProperty(ProxyIF.RS_ENDPOINTS_SHARE_WITHME));

      builder = target.request(MediaType.WILDCARD_TYPE);
      builder.headers(this.getHeaders());

      response = builder.get();

      _logger.exiting(CLASS, METHOD);

      return response;
   }

   private synchronized void load() {
      Object obj = null;
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String realPath = null;
      String propFile = null;

      _logger.entering(CLASS, METHOD);

      if (_props == null) {
         obj = _servletCtx.getAttribute(CTX_ATTR_PROPS);

         if (obj != null && obj instanceof Properties) {
            _props = (Properties) obj;
         } else {
            realPath = _servletCtx.getRealPath("/");
            propFile = realPath + "WEB-INF" + File.separator + PROP_FILE;

            try {
               _props = new Properties();
               _props.load(new FileInputStream(propFile));
            } catch (IOException ex) {
               this.abort(METHOD,
                  "Could not load Properties, Exception: " + ex.getMessage(),
                  Status.INTERNAL_SERVER_ERROR);
            }

            _servletCtx.setAttribute(CTX_ATTR_PROPS, _props);
         }
      }

      if (_gateway == null) {
         obj = _servletCtx.getAttribute(CTX_ATTR_GATEWAY);

         if (obj != null && obj instanceof GatewayIF) {
            _gateway = (GatewayIF) obj;
         } else {
            try {
               _gateway = new CachingGateway(_props);
            } catch (Exception ex) {
               this.abort(METHOD,
                  "Could not create Gateway, Exception: " + ex.getMessage(),
                  Status.INTERNAL_SERVER_ERROR);
            }

            _servletCtx.setAttribute(CTX_ATTR_GATEWAY, _gateway);
         }
      }

      _logger.exiting(CLASS, METHOD);

      return;
   }

   private synchronized WebTarget getTarget(final String path) {
      Object oKey = null;
      Object oVal = null;
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      StringBuilder base = null;
      Client client = null;
      WebTarget target = null;
      MultivaluedMap<String, String> queryParams = null;

      _logger.entering(CLASS, METHOD);

      client = ClientBuilder.newClient();

      base = new StringBuilder();
      base.append(_props.getProperty(ProxyIF.RS_CONNECT_PROTOCOL))
         .append("://")
         .append(_props.getProperty(ProxyIF.RS_CONNECT_HOST))
         .append(":")
         .append(_props.getProperty(ProxyIF.RS_CONNECT_PORT));

      /*
       * Set the Path
       */
      target = client
         .target(base.toString())
         .path(_props.getProperty(ProxyIF.RS_CONNECT_PATH))
         .path(path);

      /*
       * Set Query Params (if used in request)
       */
      queryParams = _uriInfo.getQueryParameters();

      if (queryParams != null && !queryParams.isEmpty()) {
         for (Entry e : queryParams.entrySet()) {
            if (e != null) {
               oKey = e.getKey();
               if (oKey != null
                  && oKey instanceof String
                  && !STR.isEmpty((String) oKey)) {
                  oVal = e.getValue();
                  if (oVal != null) {
                     if (oVal instanceof String
                        && !STR.isEmpty((String) oVal)) {
                        target = target.queryParam((String) oKey,
                           (String) oVal);
                     } else if (oVal instanceof LinkedList
                        && !((LinkedList) oVal).isEmpty()) {
                        target = target.queryParam((String) oKey,
                           ((LinkedList) oVal).get(0));
                     }
                  }
               }
            }
         }
      }

      _logger.exiting(CLASS, METHOD);

      return target;
   }

   private MultivaluedMap<String, Object> getHeaders() {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      MultivaluedMap<String, Object> headers = null;
      List<String> listValues = null;

      _logger.entering(CLASS, METHOD);

      headers = new MultivaluedHashMap<>();

      listValues = _httpHdrs.getRequestHeader(
         _props.getProperty(ProxyIF.RS_HEADERS_SSOTOKEN));

      if (listValues != null && !listValues.isEmpty()) {
         headers.add(_props.getProperty(ProxyIF.RS_HEADERS_SSOTOKEN),
            listValues.get(0));
      }

      _logger.exiting(CLASS, METHOD);

      return headers;
   }

   private Response getResponse(final DataIF data) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      Response response = null;
      Status status = null;
      JSONObject jsonData = null;

      /*
       * JSON input ...
       * {
       *   "data": {
       *   },
       *   "response": XXX
       * }
       */
      _logger.entering(CLASS, METHOD);

      if (data == null) {
         this.abort(METHOD, "Data is null",
            Status.INTERNAL_SERVER_ERROR);
      }

      jsonData = data.getJSON();

      if (jsonData == null || jsonData.isEmpty()) {
         this.abort(METHOD, "JSON is null or empty",
            Status.INTERNAL_SERVER_ERROR);
      }

      switch (data.getState()) {
         case SUCCESS: { // map to 200
            status = Status.OK;
            break;
         }
         case FAILED: { // map to 400
            status = Status.BAD_REQUEST;
            break;
         }
         case NOTAUTHORIZED: { // map to 401
            status = Status.UNAUTHORIZED;
            break;
         }
         case WARNING: { // map to 403
            status = Status.FORBIDDEN;
            break;
         }
         case NOTEXIST: { // map to 404
            status = Status.NOT_FOUND;
            break;
         }
         default: {
            status = Status.INTERNAL_SERVER_ERROR;
            break;
         }
      }

      response = Response.status(status)
         .type(MediaType.APPLICATION_JSON_TYPE)
         .entity(jsonData.toJSONString())
         .build();

      _logger.exiting(CLASS, METHOD);

      return response;
   }

   private String getHeaderValue(final String name) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String value = null;
      List<String> hdrValues = null;

      _logger.entering(CLASS, METHOD);

      if (!STR.isEmpty(name)) {
         hdrValues = _httpHdrs.getRequestHeader(name);

         if (hdrValues == null || hdrValues.isEmpty()) {
            this.abort(METHOD,
               "Missing header '" + name + "'",
               Status.BAD_REQUEST);
         }

         value = hdrValues.get(0);

         if (STR.isEmpty(value)) {
            this.abort(METHOD,
               "Header '" + name + "' is empty",
               Status.BAD_REQUEST);
         }
      }

      _logger.exiting(CLASS, METHOD);

      return value;
   }

   private void abort(final String method, final String msg, final Status status)
      throws WebApplicationException {
      Response response = null;

      _logger.log(Level.SEVERE, "{0}:{1}: {2}", new Object[]{
         CLASS,
         (method == null ? "" : method),
         (msg == null ? "null message" : msg)
      });

      response = Response.status(status)
         .type(MediaType.TEXT_PLAIN)
         .entity(method + ": " + msg)
         .build();

      throw new WebApplicationException(response);
   }

}
