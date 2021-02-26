/*
 * Copyright (c) 2019-2021, ForgeRock, Inc., All rights reserved
 * Use subject to license terms.
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
