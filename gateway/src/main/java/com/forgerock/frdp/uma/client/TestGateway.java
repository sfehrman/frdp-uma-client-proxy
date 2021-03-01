/*
 * Copyright (c) 2019-2021, ForgeRock, Inc., All rights reserved
 * Use subject to license terms.
 */

package com.forgerock.frdp.uma.client;

import com.forgerock.frdp.common.BasicData;
import com.forgerock.frdp.common.ConstantsIF;
import com.forgerock.frdp.common.DataIF;
import java.io.FileInputStream;
import java.util.Properties;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Test Gateway
 * @author Scott Fehrman, ForgeRock, Inc.
 */
public class TestGateway {

   public static void main(String[] args) {
      TestGateway test = new TestGateway();

      try {
         test.run();
      } catch (Exception ex) {
         System.out.println(ex.getMessage());
      }

      return;
   }

   private void run() throws Exception {
      long tstart = 0L;
      long tstop = 0L;
      String scopes = null;
      DataIF input = null;
      DataIF output = null;
      JSONObject jsonData = null;
      JSONObject jsonInput = null;
      JSONObject jsonOutput = null;
      JSONArray arrayScopes = null;
      GatewayIF gateway = null;
      Properties props = null;

      props = new Properties();
      props.load(new FileInputStream("src/main/resources/gateway.properties"));

      gateway = new CachingGateway(props);

      arrayScopes = new JSONArray();
      scopes = props.getProperty("test.scopes");
      for (String s : scopes.split(" ")) {
         arrayScopes.add(s);
      }

      // 1
      jsonData = new JSONObject();
      jsonData.put(ConstantsIF.METHOD, "get"); // String
      jsonData.put(ConstantsIF.SSO_TOKEN, props.get("test.sso")); // String
      jsonData.put(ConstantsIF.RESOURCE_ID, props.get("test.resource")); // String
      jsonData.put(ConstantsIF.SCOPES, arrayScopes); // Array of Strings

      jsonInput = new JSONObject();
      jsonInput.put(ConstantsIF.DATA, jsonData);

      input = new BasicData();
      input.setJSON(jsonInput);

      tstart = System.currentTimeMillis();
      output = gateway.process(input);
      tstop = System.currentTimeMillis();
      jsonOutput = output.getJSON();

      System.out.println("__1__: msec: " + (tstop - tstart));
      System.out.println("__1__: " + output.toString());
      System.out.println("__1__: " + jsonOutput.toString());

      // 2
      jsonData = new JSONObject();
      jsonData.put(ConstantsIF.METHOD, "get"); // String
      jsonData.put(ConstantsIF.SSO_TOKEN, props.get("test.sso")); // String
      jsonData.put(ConstantsIF.RESOURCE_ID, props.get("test.resource")); // String
      jsonData.put(ConstantsIF.SCOPES, arrayScopes); // Array of Strings

      jsonInput = new JSONObject();
      jsonInput.put(ConstantsIF.DATA, jsonData);

      input = new BasicData();
      input.setJSON(jsonInput);

      tstart = System.currentTimeMillis();
      output = gateway.process(input);
      tstop = System.currentTimeMillis();
      jsonOutput = output.getJSON();

      System.out.println("__2__: msec: " + (tstop - tstart));
      System.out.println("__2__: " + output.toString());
      System.out.println("__2__: " + jsonOutput.toString());

      // 3
      jsonData = new JSONObject();
      jsonData.put(ConstantsIF.METHOD, "get"); // String
      jsonData.put(ConstantsIF.SSO_TOKEN, props.get("test.sso")); // String
      jsonData.put(ConstantsIF.RESOURCE_ID, props.get("test.resource")); // String
      jsonData.put(ConstantsIF.SCOPES, arrayScopes); // Array of Strings

      jsonInput = new JSONObject();
      jsonInput.put(ConstantsIF.DATA, jsonData);

      input = new BasicData();
      input.setJSON(jsonInput);

      tstart = System.currentTimeMillis();
      output = gateway.process(input);
      tstop = System.currentTimeMillis();
      jsonOutput = output.getJSON();

      System.out.println("__3__: msec: " + (tstop - tstart));
      System.out.println("__3__: " + output.toString());
      System.out.println("__3__: " + jsonOutput.toString());

      return;
   }
}
