/*
 * Copyright (c) 2019-2021, ForgeRock, Inc., All rights reserved
 * Use subject to license terms.
 */

package com.forgerock.frdp.uma.client.proxy;

/**
 * Proxy Interface
 * @author Scott Fehrman, ForgeRock, Inc.
 */
public interface ProxyIF {

   public static final String RS_CONNECT_PROTOCOL = "rs.connect.protocol";
   public static final String RS_CONNECT_HOST = "rs.connect.host";
   public static final String RS_CONNECT_PORT = "rs.connect.port";
   public static final String RS_CONNECT_PATH = "rs.connect.path";
   public static final String RS_ENDPOINTS_SHARE_DISCOVER = "rs.endpoints.share.discover";
   public static final String RS_ENDPOINTS_SHARE_OWNERS = "rs.endpoints.share.owner";
   public static final String RS_ENDPOINTS_SHARE_WITHME = "rs.endpoints.share.withme";
   public static final String RS_ENDPOINTS_SHARE_POLICY = "rs.endpoints.share.policy";
   public static final String RS_HEADERS_SSOTOKEN = "rs.headers.ssotoken";
   public static final String RS_HEADERS_RPT = "rs.headers.rpt";
}
