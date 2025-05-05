package forge.util;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

public class URLValidator {

    //no more need for URL regex as we are now passing any non conforming url to the inetAddressLookup function
    //Removed manual ip parsing as the URI parsing will also take care of validating ip addresses as well
    public static HostPort parseURL(String url) {
        try {
            //prepend http:// if a protocol is missing so URI parsing does not fail
            String formattedUrl = url.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*") ? url : "http://" + url;
            URI uri = new URI(formattedUrl);
            String host = uri.getHost();
            int port = uri.getPort(); // Returns -1 if port is not specified
            if (host == null) {
               return inetAddressLookup(url);
            }
            return new HostPort(host, port);
        } catch (URISyntaxException e) {
            return inetAddressLookup(url);
        }
    }

    // Attempt to resolve as hostname
    private static HostPort inetAddressLookup(String url) {
        try {
            // Split input to handle cases like "localhost:36743"
            String[] parts = url.split(":");
            String host = parts[0];
            // if no port exists, default to -1 just like URI parsing
            // invalid port number will throw NumberFormatException caught below
            int port = parts.length > 1 ? Integer.parseInt(parts[1]) : -1;
            // address isn't needed but its presence suppresses an intelij warning
            InetAddress address = InetAddress.getByName(host);
            return new HostPort(host, port);
        } catch (NumberFormatException | UnknownHostException ex) {
            return null; // Invalid port or hostname does not resolve
        }
    }


    //This is fine, Records were introduced in Java 16, its essentially a DTO class with implicit getters and constructors
    public record HostPort(String host, Integer port) {
    }

}
