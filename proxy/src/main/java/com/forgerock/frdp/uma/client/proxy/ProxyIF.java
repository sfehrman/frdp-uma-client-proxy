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
