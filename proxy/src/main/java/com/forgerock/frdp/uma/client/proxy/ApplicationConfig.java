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
package com.forgerock.frdp.uma.client.proxy;

import java.util.Set;
import javax.ws.rs.core.Application;

/**
 *
 * @author Scott Fehrman, ForgeRock, Inc.
 */
@javax.ws.rs.ApplicationPath("rest")
public class ApplicationConfig extends Application {

   @Override
   public Set<Class<?>> getClasses() {
      Set<Class<?>> resources = new java.util.HashSet<Class<?>>();
      addRestResourceClasses(resources);
      return resources;
   }

   /**
    * Do not modify addRestResourceClasses() method. It is automatically
    * populated with all resources defined in the project. If required, comment
    * out calling this method in getClasses().
    */
   private void addRestResourceClasses(Set<Class<?>> resources) {
      resources.add(com.forgerock.frdp.uma.client.proxy.ShareResource.class);
   }

}
