/*
 * Copyright (c) 2019-2021, ForgeRock, Inc., All rights reserved
 * Use subject to license terms.
 */

package com.forgerock.frdp.uma.client;

import com.forgerock.frdp.cache.BasicCache;
import com.forgerock.frdp.cache.CacheIF;
import com.forgerock.frdp.common.BasicData;
import com.forgerock.frdp.common.ConstantsIF;
import com.forgerock.frdp.common.Core;
import com.forgerock.frdp.common.CoreIF;
import com.forgerock.frdp.common.DataIF;
import com.forgerock.frdp.utils.JSON;
import com.forgerock.frdp.utils.STR;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * UMA Client Caching Gateway
 *
 * @author Scott Fehrman, ForgeRock, Inc.
 *
 * get "RPT" from cache if exists contact RS, get "resource" for "scopes" if
 * "permission ticket" contact AS, get "claim token" via "authz code" contact
 * AS, get "RPT" using "perm ticket" + "claim token" save "RPT" in cache contact
 * RS, get "resource" for "scopes" return "resource" to "Client" else "resource"
 * return to "Client"
 */
public class CachingGateway extends Gateway {

   private static final long DEFAULT_DELAY = 30;
   private static final long DEFAULT_SLEEP = 5;
   private final String CLASS = this.getClass().getName();
   private final CacheIF _cache = new BasicCache();
   private ScheduledExecutorService _executor = null;
   private ScheduledFuture<?> _future = null;
   private Runnable _worker = null;
   private long _delay = DEFAULT_DELAY;
   private long _sleep = DEFAULT_SLEEP;

   public CachingGateway(final Map<String, String> params)
      throws Exception {
      super(params);

      String METHOD = "CachingGateway(Map<String, String>)";

      _logger.entering(CLASS, METHOD);

      this.init();

      _logger.exiting(CLASS, METHOD);

      return;
   }

   public CachingGateway(final Properties props)
      throws Exception {
      super(props);

      String METHOD = "CachingGateway(Properties)";

      _logger.entering(CLASS, METHOD);

      this.init();

      _logger.exiting(CLASS, METHOD);

      return;
   }

   @Override
   public synchronized DataIF process(final DataIF dataInput)
      throws Exception {
      boolean cached = false;
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String permTicket = null;
      String authzCode = null;
      String claimToken = null;
      String rpt = null;
      DataIF dataOutput = null;
      JSONObject jsonInput = null;
      JSONObject jsonData = null;
      JSONObject jsonPermTicket = null;
      JSONObject jsonAuthzCode = null;
      JSONObject jsonClaimToken = null;
      JSONObject jsonRPT = null;
      JSONObject jsonResource = null;
      JSONObject jsonCache = null;

      /*
       * NOTE: All requests require the SSO Token
       *
       * 1: RS: request: /share/resources/{id}/?scopes=scope(s)
       *        response: permission ticket and as_uri
       * 2: AS: request: /oauth2/authorize client_id, client_secret, cookie
       *        response: authorization code
       * 3: AS: request: /oauth2/access_token client_id, client_secret, code
       *        response: claim token
       * 4: AS: request: /oauth2/access_token permission ticket, claim token, scopes
       *        response: Requesting Party Token (RPT)
       * 5: RS: request: /share/resources/{id}/?scopes=scope(s) and RPT
       *        response: "success" (OK) ... optional payload ("meta" / "content")
       */
      _logger.entering(CLASS, METHOD);

      /*
       * JSON input
       * {
       *   "data": {
       *     "method": "...", // HTTP Method
       *     "ssotoken": "...", // AM SSO Session Token
       *     "scopes": [ "...", ... ], // Array of Strings (optional)
       *     "resource_id": "..." // Resource Id (GUID)
       *   },
       *   "gateway": {
       *   }
       * }
       */
      this.validateInput(dataInput);

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

      /*
       * Get cached RPT
       * JSON output ...
       * {
       *   "access_token": "..."
       * }
       */
      dataOutput = this.getCachedRPT(dataInput);

      if (dataOutput != null) {
         jsonRPT = dataOutput.getJSON();

         rpt = JSON.getString(jsonRPT, ConstantsIF.ACCESS_TOKEN);

         if (!STR.isEmpty(rpt)) {
            cached = true;
            jsonData.put(ConstantsIF.ACCESS_TOKEN, rpt);
         }
      }

      dataOutput = this.getResource(dataInput);
      /*
       * expected output state:
       * 200 : Success ... got the resource
       * 401 : Unauthorized ... get a permission ticket
       */
      switch (dataOutput.getState()) {
         case SUCCESS: // 200 : resource is returned
         {
            break;
         }
         case NOTAUTHORIZED: // 401 : permission ticket is returned
         {
            /*
             * Get a Permission Ticket 
             * JSON output ...
             * {
             *   "data": {
             *     "ticket": "...",
             *     "as_uri": "...",
             *     "scopes": [ ... ],
             *     "message": "...",
             *     "token": "..."
             *   },
             *   "response": XXX
             * }
             */
            jsonPermTicket = dataOutput.getJSON();

            permTicket = JSON.getString(jsonPermTicket,
               ConstantsIF.DATA + "." + ConstantsIF.TICKET);

            if (STR.isEmpty(permTicket)) {
               this.abort(CLASS, METHOD,
                  "Permission Ticket is empty. status: "
                  + dataOutput.getStatus());
            }

            jsonData.put(ConstantsIF.TICKET, permTicket);

            /*
             * Get an Authorization Code from the Authorization Server
             * expecting 302 : FOUND (REDIRECT)
             * JSON output ...
             * {
             *   "data": {
             *     "code": "..."
             *   },
             *   "headers": { ... },
             *   "response": XXX
             * }
             */
            dataOutput = this.getAuthzCode(dataInput); // Get an Authorization Code

            if (dataOutput.getState() != STATE.SUCCESS) {
               this.abort(CLASS, METHOD,
                  "Expected 302 : FOUND (REDIRECT) response from getAuthzCode(): "
                  + dataOutput.getState().toString() + ": "
                  + dataOutput.getStatus());
            }

            jsonAuthzCode = dataOutput.getJSON();

            authzCode = JSON.getString(jsonAuthzCode,
               ConstantsIF.DATA + "." + ConstantsIF.CODE);

            if (STR.isEmpty(authzCode)) {
               this.abort(CLASS, METHOD,
                  "Authorization Code is empty");
            }

            jsonData.put(ConstantsIF.CODE, authzCode);

            /*
             * Get a Claim Token from the Authorization Server
             * Need to send in the Authorization Code
             * Expect 200 : OK
             * JSON output ...
             * {
             *   "data": {
             *     "id_token": "...",
             *     ...
             *   },
             *   "response": XXX
             * }
             */
            dataOutput = this.getClaimToken(dataInput); // Get a Claim Token

            if (dataOutput.getState() != STATE.SUCCESS) {
               this.abort(CLASS, METHOD,
                  "Expected 200 : OK response from getClaimToken(): "
                  + dataOutput.getState().toString() + ": "
                  + dataOutput.getStatus());
            }

            jsonClaimToken = dataOutput.getJSON();

            claimToken = JSON.getString(jsonClaimToken,
               ConstantsIF.DATA + "." + ConstantsIF.ID_TOKEN);

            if (STR.isEmpty(claimToken)) {
               this.abort(CLASS, METHOD,
                  "Claim Token is empty");
            }

            jsonData.put(ConstantsIF.ID_TOKEN, claimToken);

            /*
             * Get a Requesting Party Token (RPT) 
             * Need to send in "claim token" and "permission ticket"
             * Expect 200 : OK
             * JSON output ...
             * {
             *   "data": {
             *     "access_token": "...", // Requesting Party Token
             *     ...
             *   },
             *   "response": XXX
             * }
             */
            dataOutput = this.getRPT(dataInput); // Get a Requesting Party Token

            switch (dataOutput.getState()) {
               case SUCCESS: {
                  jsonRPT = dataOutput.getJSON();

                  rpt = JSON.getString(jsonRPT,
                     ConstantsIF.DATA + "." + ConstantsIF.ACCESS_TOKEN);

                  if (STR.isEmpty(rpt)) {
                     this.abort(CLASS, METHOD,
                        "Requesting Party Token (RPT) is empty");
                  }

                  jsonData.put(ConstantsIF.ACCESS_TOKEN, rpt);

                  this.saveCachedRPT(dataInput);

                  /*
                   * Get the resource from the Resource Server
                   * Send in the Permission Ticket, Resource Id and Scope(s)
                   * Expect 200 : OK
                   * JSON output ...
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
                  dataOutput = this.getResource(dataInput); // Get the Resource
                  if (dataOutput.getState() != STATE.SUCCESS) {
                     this.abort(CLASS, METHOD,
                        "Expected 200 : OK response from getResource(): "
                        + dataOutput.getState().toString() + ": "
                        + dataOutput.getStatus());
                  }
                  break;
               }
               case WARNING: {
                  /*
                  * Request was submitted
                  * remove "data.ticket" from JSON
                   */
                  jsonRPT = dataOutput.getJSON();
                  jsonData = JSON.getObject(jsonRPT, ConstantsIF.DATA);
                  if (jsonData != null) {
                     jsonData.remove(ConstantsIF.TICKET);
                  }
                  break;
               }
               default: {
                  this.abort(CLASS, METHOD,
                     "Expected SUCCESS or WARNING response from getRPT(): "
                     + dataOutput.getState().toString() + ": "
                     + dataOutput.getStatus());
                  break;
               }
            }
            break;
         }
         default: // other, unexpected state
         {
            /*
             * possible  400 : BAD REQUEST ... bad scopes
             *           404 : NOT FOUND   ... bad resource id
             *           409 : CONFLICT    ... mixed scopes
             */
            this.abort(CLASS, METHOD,
               "Expected SUCCESS or NOTAUTHORIZED response from getResource(): "
               + dataOutput.getState().toString() + ": "
               + dataOutput.getStatus());
            break;
         }
      }

      jsonResource = dataOutput.getJSON();

      jsonData = JSON.getObject(jsonResource, ConstantsIF.DATA);

      jsonCache = new JSONObject();
      jsonCache.put("rpt", cached);

      jsonData.put(ConstantsIF.CACHE, jsonCache);

      /* OUTPUT: add "cache" object
       * {
       *   ...
       *   "cache": {
       *     "rpt": true
       *   }
       * }
       */
      dataOutput.setJSON(jsonData);

      _logger.exiting(CLASS, METHOD);

      return dataOutput;
   }

   private void init() {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();

      _logger.entering(CLASS, METHOD);

      _worker = new CacheCleaner(this.getParams(), _cache);
      _executor = Executors.newScheduledThreadPool(1);
      _future = _executor
         .scheduleAtFixedRate(_worker, _delay, _sleep, TimeUnit.MINUTES);

      _logger.exiting(CLASS, METHOD);

      return;
   }

   private DataIF getCachedRPT(final DataIF dataInput)
      throws Exception {
      long created = 0L;
      long age = 0L;
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String key = null;
      DataIF data = null;

      /*
       * RPTs are, by default, valid for 2 hours 
       * ( 2 hours x 60 minutes x 60 seconds x 1000 msec ) = 3,600,000 msec
       * Check the CREATED time stamp on the DataIF object
       * If "too old", 2 hours - 1 minute () ... delete it 
       */
      _logger.entering(CLASS, METHOD);

      key = this.getHashKey(dataInput);

      if (_cache.containsKey(key)) {
         data = _cache.get(key);

         if (data != null) {
            created = data.getTStamp(TSTAMP.CREATED);
            age = System.currentTimeMillis() - created; // msec

            if (age < 3540000) // 2 hours - 1 minute
            {
               _logger.log(Level.INFO,
                  "Found cache entry, key=''{0}''", key);
            } else {
               _cache.remove(key);
               _logger.log(Level.INFO,
                  "Removed old cache entry, key=''{0}''", key);
            }
         } else {
            _cache.remove(key);
            _logger.log(Level.INFO,
               "Removed null cache entry, key=''{0}''", key);
         }
      }

      _logger.exiting(CLASS, METHOD);

      return data;
   }

   private void saveCachedRPT(final DataIF dataInput)
      throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String key = null;
      String val = null;
      DataIF dataRPT = null;
      JSONObject jsonInput = null;
      JSONObject jsonData = null;
      JSONObject jsonRPT = null;

      /*
       * JSON data ...
       * {
       *   "data": {
       *     "key": "...",
       *     "access_token": "...",
       *     ...
       *   }
       * }
       * only store the "access_token"
       * {
       *    "acccess_token": "..."
       * }
       */
      _logger.entering(CLASS, METHOD);

      jsonInput = dataInput.getJSON();

      if (jsonInput == null || jsonInput.isEmpty()) {
         this.abort(CLASS, METHOD, "JSON Input is null or empty");
      }

      jsonData = JSON.getObject(jsonInput, ConstantsIF.DATA);

      if (jsonData == null || jsonData.isEmpty()) {
         this.abort(CLASS, METHOD, "JSON Data is null or empty");
      }

      key = JSON.getString(jsonData, ConstantsIF.KEY);

      if (STR.isEmpty(key)) {
         this.abort(CLASS, METHOD, "Cache key is empty");
      }

      val = JSON.getString(jsonData, ConstantsIF.ACCESS_TOKEN);

      if (STR.isEmpty(val)) {
         this.abort(CLASS, METHOD, "Cache value is empty");
      }

      jsonRPT = new JSONObject();
      jsonRPT.put(ConstantsIF.ACCESS_TOKEN, val);

      dataRPT = new BasicData();
      dataRPT.setJSON(jsonRPT);

      _cache.put(key, dataRPT);
      _logger.exiting(CLASS, METHOD);

      return;
   }

   private String getHashKey(final DataIF dataInput)
      throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String key = null;
      String userName = null;
      String resourceId = null;
      StringBuilder buf = new StringBuilder();
      JSONObject jsonInput = null;
      JSONObject jsonData = null;
      JSONArray array = null;

      /*
       * dataInput JSON
       * {
       *   "data": {
       *     "method": "...", // HTTP Method
       *     "ssotoken": "...", // AM SSO Session Token
       *     "scopes": [ "...", ... ], // Array of Strings (optional)
       *     "resource_id": "..." // Resource Id (GUID)
       *     "key": "..." // added in this function
       *   }
       * }
       * Get "username" from validating the SSO Token
       * Build string of scope names
       * concatinate user name + resourceId + scope names 
       */
      _logger.entering(CLASS, METHOD);

      if (dataInput != null) {
         userName = this.getUsernameFromSSOToken(dataInput);

         if (!STR.isEmpty(userName)) {
            buf.append(userName).append(":");

            jsonInput = dataInput.getJSON();

            if (jsonInput != null && !jsonInput.isEmpty()) {
               jsonData = JSON.getObject(jsonInput, ConstantsIF.DATA);

               if (jsonData != null && !jsonData.isEmpty()) {
                  resourceId = JSON.getString(jsonData, ConstantsIF.RESOURCE_ID);

                  if (!STR.isEmpty(resourceId)) {
                     buf.append(resourceId).append(":");

                     array = JSON.getArray(jsonData, ConstantsIF.SCOPES);

                     if (array != null && !array.isEmpty()) {
                        for (Object o : array) {
                           if (o != null
                              && o instanceof String
                              && !STR.isEmpty((String) o)) {
                              buf.append((String) o);
                           }
                        }

                        // bjensen:32f3d4e2-ae3d-4867-a85a-68e118156927:metacontent
                        key = buf.toString();
                     }
                  }
               }
            }
         } else {
            this.abort(CLASS, METHOD,
               "Could not get username from SSO token");
         }
      }

      jsonData.put(ConstantsIF.KEY, key); // add "key" to JSON

      _logger.exiting(CLASS, METHOD);

      return key;
   }

   private class CacheCleaner extends Core implements Runnable {

      private final String CLASS = this.getClass().getName();
      private final long DEFAULT_TTL = 3600000L;
      private CacheIF _cache = null;
      private long _ttl = 0L;
      private final Logger _logger = Logger.getLogger(CLASS);

      public CacheCleaner(final Map<String, String> params, final CacheIF cache) {
         super(params);

         _cache = cache;

         if (params.containsKey(GatewayIF.AS_TTL_RPT)) {
            try {
               _ttl = Long.parseLong(params.get(GatewayIF.AS_TTL_RPT));
            } catch (NumberFormatException ex) {
               _ttl = DEFAULT_TTL;
            }
         } else {
            _ttl = DEFAULT_TTL;
         }

         _logger.log(Level.INFO, "{0} created", CLASS);

         return;
      }

      @Override
      public void run() {
         boolean remove = false;
         long created = 0L;
         long age = 0L;
         String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
         DataIF data = null;

         _logger.log(Level.INFO,
            "{0}:{1}: cache size: {2}, ttl: {3}",
            new Object[]{
               CLASS,
               METHOD,
               _cache != null ? _cache.size() : "null",
               _ttl
            });

         /*
          * Remove cached items that have expired
          */
         if (_cache != null && _cache.size() > 0) {
            for (String key : _cache.keySet()) {
               remove = false;
               if (!STR.isEmpty(key)) {
                  try {
                     data = _cache.get(key);
                  } catch (Exception ex) {
                     _logger.log(Level.WARNING,
                        "{0}:{1}: exception: {2}",
                        new Object[]{
                           CLASS,
                           METHOD,
                           ex.getMessage()
                        });
                  }

                  if (data != null) {
                     created = data.getTStamp(TSTAMP.CREATED);
                     age = System.currentTimeMillis() - created; // msec

                     if (age > _ttl) {
                        remove = true;
                     }
                  } else {
                     remove = true;
                  }
               }
               if (remove) {
                  try {
                     _cache.remove(key);

                     _logger.log(Level.INFO,
                        "{0}:{1}: removed expired entry: {2}",
                        new Object[]{
                           CLASS,
                           METHOD,
                           key
                        });
                  } catch (Exception ex) {
                     _logger.log(Level.WARNING,
                        "{0}:{1}: exception: {2}",
                        new Object[]{
                           CLASS,
                           METHOD,
                           ex.getMessage()
                        });
                  }
               }
            }
         }

         return;
      }

      @Override
      public CoreIF copy() {
         throw new UnsupportedOperationException("Not supported yet.");
      }

   }
}
