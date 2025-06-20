package forge.ai;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

// Static initialization occurs here which can throw errors if called during class loading
// Moving static initializers into constructor or methods can help prevent ExceptionInInitializerError

/**
 * Client for communicating with an LLM service to make game decisions.
 */
public class LLMClient {
    private final String endpoint;
    private Gson gson;
    private static final boolean DEBUG = true;
    
    /**
     * Creates a new LLM client with the specified endpoint URL.
     * 
     * @param endpointUrl The base URL of the LLM service (e.g. "http://localhost:5000")
     */
    public LLMClient(String endpointUrl) {
        this.endpoint = endpointUrl;
        this.gson = new Gson(); // Initialize Gson here to avoid static initialization issues
        System.out.println("=====================================================");
        System.out.println("LLMClient initialized with endpoint: " + endpointUrl);
        System.out.println("Java version: " + System.getProperty("java.version"));
        System.out.println("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        System.out.println("=====================================================");
        
        // Test connection immediately
        try {
            JsonObject testState = new JsonObject();
            testState.addProperty("context", "debug");
            testState.addProperty("message", "Testing LLM connection on initialization");
            System.out.println("Sending test request to LLM server at: " + endpointUrl);
            JsonObject response = this.ask(testState);
            System.out.println("LLM connection test successful! Response: " + gson.toJson(response));
        } catch (Exception e) {
            System.err.println("!!! ERROR: LLM connection test failed !!!");
            System.err.println("Exception: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            
            // Now that we're removing fallbacks, throw an exception on initialization failure
            throw new RuntimeException("LLM connection test failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Sends the current game state to the LLM service and retrieves the AI's decision.
     * 
     * @param gameState A JSON object containing the current game state
     * @return A JSON object containing the AI's decision
     * @throws IOException If there is an error communicating with the LLM service
     */
    public JsonObject ask(JsonObject gameState) throws IOException {
        if (DEBUG) System.out.println("==== LLMClient.ask() CALLED ====");
        
        // Check if we're dealing with a potential initialization error
        if (gameState == null) {
            System.out.println("WARNING: gameState is null, creating empty JsonObject");
            gameState = new JsonObject();
        }
        
        // Add debug context if not already present
        if (!gameState.has("context")) {
            System.out.println("Adding default debug context");
            gameState.addProperty("context", "debug");
            gameState.addProperty("message", "Testing LLM connection");
        }
        
        String requestBody = gson.toJson(gameState);
        if (DEBUG) {
            System.out.println("Request to: " + endpoint + "/act");
            String context = gameState.has("context") ? gameState.get("context").getAsString() : "unknown";
            System.out.println("Context: " + context);
            System.out.println("Request body (truncated): " + 
                    requestBody.substring(0, Math.min(requestBody.length(), 500)) + 
                    (requestBody.length() > 500 ? "..." : ""));
        }
        
        URL url = new URL(endpoint + "/act");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        try {
            // Configure connection
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(600000); // 10 minutes
            conn.setReadTimeout(600000);    // 10 minutes
            
            if (DEBUG) {
                System.out.println("Connection properties:");
                System.out.println("- Method: " + conn.getRequestMethod());
                System.out.println("- Content-Type: " + conn.getRequestProperty("Content-Type"));
                System.out.println("- Accept: " + conn.getRequestProperty("Accept"));
                System.out.println("- Connect timeout: " + conn.getConnectTimeout() + "ms (" + (conn.getConnectTimeout()/60000) + " minutes)");
                System.out.println("- Read timeout: " + conn.getReadTimeout() + "ms (" + (conn.getReadTimeout()/60000) + " minutes)");
                System.out.println("Sending HTTP request...");
            }
            
            // Send request
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.getBytes("utf-8");
                os.write(input, 0, input.length);
                if (DEBUG) System.out.println("Request data written (" + input.length + " bytes)");
            }
            
            // Check response code
            int responseCode = conn.getResponseCode();
            if (DEBUG) System.out.println("Received HTTP response code: " + responseCode);
            
            if (responseCode != HttpURLConnection.HTTP_OK) {
                String errorMessage = "HTTP error: " + responseCode + " - " + conn.getResponseMessage();
                System.err.println(errorMessage);
                throw new IOException(errorMessage);
            }
            
            // Read and parse response
            if (DEBUG) System.out.println("Reading response...");
            StringBuilder responseBuilder = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    responseBuilder.append(line);
                }
            }
            
            String responseText = responseBuilder.toString();
            if (DEBUG) {
                System.out.println("Response (truncated): " + 
                        responseText.substring(0, Math.min(responseText.length(), 500)) + 
                        (responseText.length() > 500 ? "..." : ""));
            }
            
            JsonObject responseJson = gson.fromJson(responseText, JsonObject.class);
            if (DEBUG) System.out.println("Response parsed successfully");
            
            return responseJson;
            
        } catch (Exception e) {
            System.err.println("Exception in LLMClient.ask(): " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            
            // No fallback anymore - we propagate the exception
            throw new IOException("LLM service communication failed: " + e.getMessage(), e);
        } finally {
            if (DEBUG) System.out.println("Disconnecting HTTP connection");
            conn.disconnect();
            if (DEBUG) System.out.println("==== LLMClient.ask() END ====");
        }
    }
}