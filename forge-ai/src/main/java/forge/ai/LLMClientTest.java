package forge.ai;

import com.google.gson.JsonObject;

/**
 * Test class to verify that the LLMClient works correctly.
 */
public class LLMClientTest {
    public static void main(String[] args) {
        try {
            System.out.println("Testing LLMClient...");
            String endpoint = "http://localhost:7860";
            LLMClient client = new LLMClient(endpoint);
            
            JsonObject testState = new JsonObject();
            testState.addProperty("test", "This is a test request");
            testState.addProperty("context", "testing");
            
            System.out.println("Sending request to " + endpoint);
            JsonObject response = client.ask(testState);
            
            System.out.println("Response: " + response);
            System.out.println("Test completed successfully!");
        } catch (Exception e) {
            System.err.println("Error testing LLMClient: " + e.getMessage());
            e.printStackTrace();
        }
    }
}