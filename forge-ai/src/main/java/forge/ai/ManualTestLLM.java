package forge.ai;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Manual test for LLM communication that doesn't depend on any Forge classes.
 * This is a completely standalone test to verify the server connectivity.
 */
public class ManualTestLLM {
    public static void main(String[] args) {
        System.out.println("===================================================");
        System.out.println("MANUAL LLM SERVER CONNECTIVITY TEST");
        System.out.println("===================================================");
        
        // Get endpoint from system property or use default
        String endpoint = System.getProperty("llm.endpoint", "http://localhost:7861");
        System.out.println("Using LLM endpoint: " + endpoint);
        
        try {
            // Create a test JSON payload manually (without using Gson)
            String jsonPayload = "{\"context\":\"testing\",\"message\":\"Test message from ManualTestLLM\"}";
            System.out.println("Test payload: " + jsonPayload);
            
            // Create URL and open connection
            URL url = new URL(endpoint + "/act");
            System.out.println("Connecting to: " + url);
            
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(5000); // 5 seconds
            conn.setReadTimeout(5000);    // 5 seconds
            
            System.out.println("Sending HTTP request...");
            
            // Send the request
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes("utf-8");
                os.write(input, 0, input.length);
                System.out.println("Request data written");
            }
            
            // Check response code
            int responseCode = conn.getResponseCode();
            System.out.println("Response code: " + responseCode);
            
            // Read response
            StringBuilder responseBuilder = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    responseBuilder.append(line);
                }
            }
            
            System.out.println("Response: " + responseBuilder.toString());
            System.out.println("===================================================");
            System.out.println("TEST COMPLETED SUCCESSFULLY");
            System.out.println("===================================================");
            
        } catch (Exception e) {
            System.err.println("===================================================");
            System.err.println("TEST FAILED WITH EXCEPTION");
            System.err.println("===================================================");
            System.err.println("Exception: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}