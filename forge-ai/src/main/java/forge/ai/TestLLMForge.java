package forge.ai;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Standalone test class for LLM integration with Forge.
 */
public class TestLLMForge {
    public static void main(String[] args) {
        System.out.println("======================================================");
        System.out.println("STARTING DIRECT TEST OF LLM INTEGRATION");
        System.out.println("======================================================");
        
        try {
            // Get endpoint from system property or use default
            String endpoint = System.getProperty("llm.endpoint", "http://localhost:7861");
            System.out.println("Using LLM endpoint from system property: " + endpoint);
            
            // Initialize LLMClient - this should already perform a connection test
            System.out.println("Creating LLMClient instance...");
            LLMClient client = new LLMClient(endpoint);
            
            // Create a dummy player
            System.out.println("Creating LobbyPlayerLLM instance...");
            LobbyPlayerLLM player = new LobbyPlayerLLM("TestPlayer", client);
            System.out.println("Created LobbyPlayerLLM: " + player.getName());
            
            // Test a simple request
            System.out.println("======================================================");
            System.out.println("PREPARING DIRECT TEST REQUEST");
            System.out.println("======================================================");
            
            JsonObject testState = new JsonObject();
            testState.addProperty("context", "testing");
            testState.addProperty("message", "This is a DIRECT test of the LLM integration with Forge");
            
            System.out.println("Sending DIRECT test request to LLM service...");
            JsonObject response = client.ask(testState);
            System.out.println("DIRECT response received: " + new Gson().toJson(response));
            
            System.out.println("======================================================");
            System.out.println("TEST COMPLETED SUCCESSFULLY!");
            System.out.println("======================================================");
        } catch (Exception e) {
            System.err.println("======================================================");
            System.err.println("ERROR TESTING LLM INTEGRATION");
            System.err.println("======================================================");
            System.err.println("Exception: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}