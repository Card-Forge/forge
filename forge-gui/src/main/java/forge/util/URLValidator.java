package forge.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

public class URLValidator {

    private static final Pattern DOMAIN_NAME_PATTERN = Pattern.compile("^(?!-)[A-Za-z0-9-]{1,63}(?<!-)(\\.[A-Za-z]{2,})+$");

    public static HostPort parseIP(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) return null;
        for (String part : parts) {
            try {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) return null;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return new HostPort(ip, null); // No port for raw IP
    }


    public static HostPort parseURL(String url) {
        try {
            //prepend http:// if a protocol is missing so URI parsing does not fail
            String formattedUrl = url.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*") ? url : "http://" + url;
            URI uri = new URI(formattedUrl);
            String host = uri.getHost();
            int port = uri.getPort(); // Returns -1 if port is not specified
            if (host == null) return null;
            HostPort hostPort;
            if (parseIP(host) != null) {
                hostPort = new HostPort(host, port == -1 ? null : port);
            } else if (DOMAIN_NAME_PATTERN.matcher(host).matches()) {
                hostPort = new HostPort(host, port == -1 ? null : port);
            } else {
                return null;
            }
            return hostPort;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    //This is fine, Records were introduced in Java 16, its essentially a DTO class with implicit getters and constructors
    public record HostPort(String host, Integer port) {
    }

}
