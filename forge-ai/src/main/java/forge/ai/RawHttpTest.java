package forge.ai;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Raw HTTP test for LLM server connection.
 * This class doesn't depend on any Forge code or libraries.
 */
public class RawHttpTest {
    public static void main(String[] args) {
        System.out.println("======================================================");
        System.out.println("RAW HTTP TEST FOR LLM SERVER");
        System.out.println("======================================================");
        
        // Get endpoint from system property or use default
        String endpoint = System.getProperty("llm.endpoint", "http://localhost:7861");
        System.out.println("Target endpoint: " + endpoint);
        
        try {
            // Create a simple JSON payload manually (without gson)
            String jsonPayload = "{\"context\":\"debug\",\"message\":\"Raw HTTP test\"}";
            System.out.println("Test payload: " + jsonPayload);
            
            // Create URL
            URL url = new URL(endpoint + "/act");
            System.out.println("Connecting to: " + url);
            
            // Open connection
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(10000); // 10 seconds
            conn.setReadTimeout(10000);    // 10 seconds
            
            System.out.println("Connection properties:");
            System.out.println("- Method: " + conn.getRequestMethod());
            System.out.println("- Content-Type: " + conn.getRequestProperty("Content-Type"));
            System.out.println("- Accept: " + conn.getRequestProperty("Accept"));
            System.out.println("- Connect timeout: " + conn.getConnectTimeout() + "ms");
            System.out.println("- Read timeout: " + conn.getReadTimeout() + "ms");
            
            try {
                System.out.println("Sending HTTP request...");
                
                // Send the request
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes("utf-8");
                    os.write(input, 0, input.length);
                    System.out.println("Request data written (" + input.length + " bytes)");
                }
                
                // Check response code
                int responseCode = conn.getResponseCode();
                System.out.println("Response code: " + responseCode);
                
                // Read response
                StringBuilder responseBuilder = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        responseBuilder.append(responseLine.trim());
                    }
                }
                
                System.out.println("Response: " + responseBuilder.toString());
                System.out.println("======================================================");
                System.out.println("TEST COMPLETED SUCCESSFULLY");
                System.out.println("======================================================");
                
            } finally {
                conn.disconnect();
                System.out.println("Connection closed");
            }
            
        } catch (IOException e) {
            System.err.println("======================================================");
            System.err.println("TEST FAILED WITH EXCEPTION");
            System.err.println("======================================================");
            System.err.println("Exception: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}