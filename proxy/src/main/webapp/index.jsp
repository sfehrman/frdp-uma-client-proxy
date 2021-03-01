<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>UMA Proxy</title>
    </head>
    <body>
        <h1>Welcome to the UMA (Requesting Party) Proxy</h1>
        
        <p>
            The following <tt>curl</tt> commands can be used
            to test the UMA Proxy:
        </p>
        
        <h2>Authenticate</h2>
        <p>
            The Requesting Party user must be authenticated.
            Save the response token (SSOTOKEN).
        </p>
        <pre>
curl -X POST \
-H "X-OpenAM-Username: __USERNAME__" \
-H "X-OpenAM-Password: __PASSWORD__" \
-H "accept-api-version: resource=2.0,protocol=1.0" \
-H "Content-Type: application/json" \
https://FQDN/am/json/realms/root/authenticate
        </pre>
        
        <h2>Shared With Me</h2>
        <p>
            List the resources that have been shared with the Requesting Party.
        </p>
        <pre>
curl -X GET \
-H "x-frdp-ssotoken: __SSOTOKEN__" \
https://FQDN/uma-proxy/rest/share/withme
        </pre>
        
        <h2>Discoverable</h2>
        <p>
            List the resources that are "discoverable" for a given Resource Owner
        </p>
        <pre>
curl -x GET \
-H "x-frdp-ssotoken: __SSOTOKEN__" \
https://FQDN/uma-proxy/rest/share/owners/__OWNER__/discover
        </pre>
        
        <h2>Get Resource</h2>
        <p>
            Get the resource, for the given scope(s)
        </p>
        <pre>
curl -x GET \
-H "x-frdp-ssotoken: __SSOTOKEN__" \
https://FQDN/uma-proxy/rest/share/resources/__RESOURCEID__/?scopes=content
        </pre>
        
        <h2>Remove My Access</h2>
        <p>
            Requesting Party revokes their own access
        </p>
        <pre>
curl -X DELETE \
-H "x-frdp-ssotoken: __SSOTOKEN__" \
https://FQDN/uma-proxy/rest/share/resources/__RESOURCEID__/policy
        </pre>
    </body>
</html>
