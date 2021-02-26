/*
 * Copyright (c) 2019-2021, ForgeRock, Inc., All rights reserved
 * Use subject to license terms.
 */

package com.forgerock.frdp.uma.client;

import com.forgerock.frdp.common.DataIF;

/**
 * Client Gateway
 * @author Scott Fehrman, ForgeRock, Inc.
 */
public interface GatewayIF
{
   public static final String RS_CONNECT_PROTOCOL = "rs.connect.protocol";
   public static final String RS_CONNECT_HOST = "rs.connect.host";
   public static final String RS_CONNECT_PORT = "rs.connect.port";
   public static final String RS_CONNECT_PATH = "rs.connect.path";
   public static final String RS_ENDPOINTS_RESOURCES = "rs.endpoints.share.resources";
   public static final String RS_ENDPOINTS_OWNERS = "rs.endpoints.share.owners";
   public static final String RS_ENDPOINTS_WITHME = "rs.endpoints.share.withme";
   public static final String RS_HEADERS_SSOTOKEN = "rs.headers.ssotoken";
   public static final String RS_HEADERS_RPT = "rs.headers.rpt";
   public static final String AS_CONNECT_PROTOCOL = "as.connect.protocol";
   public static final String AS_CONNECT_HOST = "as.connect.host";
   public static final String AS_CONNECT_PORT = "as.connect.port";
   public static final String AS_CONNECT_PATH = "as.connect.path";
   public static final String AS_COOKIE = "as.cookie";
   public static final String AS_ENDPOINTS_SESSIONS = "as.endpoints.sessions";
   public static final String AS_ENDPOINTS_AUTHORIZE = "as.endpoints.oauth2.authorize";
   public static final String AS_ENDPOINTS_ACCESS = "as.endpoints.oauth2.access";
   public static final String AS_VERSION_SESSIONS = "as.version.sessions";
   public static final String AS_VERSION_AUTHORIZE = "as.version.oauth2.authorize";
   public static final String AS_VERSION_ACCESS = "as.version.oauth2.access";
   public static final String AS_OAUTH2_CLIENT_ID = "as.oauth2.client.id";
   public static final String AS_OAUTH2_CLIENT_SECRET = "as.oauth2.client.secret";
   public static final String AS_OAUTH2_CLIENT_REDIRECT = "as.oauth2.client.redirect";
   public static final String AS_TTL_RPT = "as.ttl.rpt";
   
   public DataIF process(DataIF data) throws Exception;
}
