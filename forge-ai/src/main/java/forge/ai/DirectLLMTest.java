package forge.ai;

import com.google.gson.JsonObject;

/**
 * Standalone test for LLM client that directly uses the LLMClient class.
 */
public class DirectLLMTest {
    public static void main(String[] args) {
        System.out.println("=====================================================");
        System.out.println("DIRECT LLM CLIENT TEST");
        System.out.println("=====================================================");
        
        try {
            // Get endpoint from system property or use default
            String endpoint = System.getProperty("llm.endpoint", "http://localhost:7861");
            System.out.println("Using endpoint: " + endpoint);
            
            // This will already test the connection during initialization
            System.out.println("Creating LLMClient...");
            LLMClient client = new LLMClient(endpoint);
            
            // Send a manual test request
            System.out.println("Sending manual test request...");
            JsonObject testRequest = new JsonObject();
            testRequest.addProperty("context", "debug");
            testRequest.addProperty("message", "Direct test request");
            
            JsonObject response = client.ask(testRequest);
            System.out.println("Response received: " + response);
            
            System.out.println("Test completed successfully!");
            
        } catch (Exception e) {
            System.err.println("Test failed with exception:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}