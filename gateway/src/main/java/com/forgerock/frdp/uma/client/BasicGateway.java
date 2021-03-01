/*
 * Copyright (c) 2019-2021, ForgeRock, Inc., All rights reserved
 * Use subject to license terms.
 */

package com.forgerock.frdp.uma.client;

import com.forgerock.frdp.common.ConstantsIF;
import com.forgerock.frdp.common.DataIF;
import com.forgerock.frdp.utils.JSON;
import com.forgerock.frdp.utils.STR;
import java.util.Map;
import java.util.Properties;
import org.json.simple.JSONObject;

/**
 * UMA Client Basic Gateway
 * @author Scott Fehrman, ForgeRock, Inc.
 */
public class BasicGateway extends Gateway {

   private final String CLASS = this.getClass().getName();

   public BasicGateway(final Map<String, String> params) throws Exception {
      super(params);

      String METHOD = "BasicGateway(Map<String, String>)";

      _logger.entering(CLASS, METHOD);
      _logger.exiting(CLASS, METHOD);

      return;
   }

   public BasicGateway(final Properties props) throws Exception {
      super(props);

      String METHOD = "BasicGateway(Properties)";

      _logger.entering(CLASS, METHOD);
      _logger.exiting(CLASS, METHOD);

      return;
   }

   @Override
   public synchronized DataIF process(final DataIF dataInput)
      throws Exception {
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
      STATE stateRPT = STATE.NEW;

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
       *   }
       * }
       */
      this.validateInput(dataInput);

      /*
       * Get a Permission Ticket from the Resource Server
       * expecting 401 : UNAUTHORIZED ... missing RPT
       * possible  400 : BAD REQUEST ... bad scopes
       *           404 : NOT FOUND ... bad resource id
       *           409 : CONFLICT ... mixed scopes
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

      dataOutput = this.getPermTicket(dataInput); // Get a Permission Ticket

      if (dataOutput.getState() == STATE.NOTAUTHORIZED) // 401: UNAUTHORIZED
      {
         jsonData = new JSONObject();

         jsonPermTicket = dataOutput.getJSON();

         permTicket = JSON.getString(jsonPermTicket,
            ConstantsIF.DATA + "." + ConstantsIF.TICKET);

         if (STR.isEmpty(permTicket)) {
            this.abort(CLASS, METHOD, 
               "Permission Ticket is empty");
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

         stateRPT = dataOutput.getState();

         switch (stateRPT) {
            case SUCCESS: {
               jsonRPT = dataOutput.getJSON();

               rpt = JSON.getString(jsonRPT,
                  ConstantsIF.DATA + "." + ConstantsIF.ACCESS_TOKEN);

               if (STR.isEmpty(rpt)) {
                  this.abort(CLASS, METHOD,
                     "Requesting Party Token (RPT) is empty");
               }

               jsonData.put(ConstantsIF.ACCESS_TOKEN, rpt);

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
                     "ERROR: Expected 200 : OK response from getResource(): "
                     + dataOutput.getState().toString() + ": "
                     + dataOutput.getStatus());
               }
               break;
            }
            case WARNING: {
               // Request was submitted, remove "data.ticket" from JSON
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
            }
         }

      }

      jsonResource = dataOutput.getJSON();

      jsonData = JSON.getObject(jsonResource, ConstantsIF.DATA);

      dataOutput.setJSON(jsonData);

      _logger.exiting(CLASS, METHOD);

      return dataOutput;
   }
}
