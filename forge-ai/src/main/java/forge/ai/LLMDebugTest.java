package forge.ai;

import com.google.gson.JsonObject;

/**
 * Debug test class for LLM integration with detailed error handling.
 */
public class LLMDebugTest {
    
    public static void main(String[] args) {
        try {
            System.out.println("=== Starting LLM Debug Test ===");
            System.out.println("Classpath: " + System.getProperty("java.class.path"));
            
            // Step 1: Test basic class loading
            try {
                System.out.println("1. Testing basic class loading...");
                Class.forName("com.google.gson.JsonObject");
                System.out.println("   JsonObject class loaded successfully");
                
                Class.forName("forge.ai.LLMClient");
                System.out.println("   LLMClient class loaded successfully");
                
                System.out.println("   All basic classes loaded successfully");
            } catch (ClassNotFoundException e) {
                System.err.println("   Error loading class: " + e.getMessage());
                e.printStackTrace();
                return;
            }
            
            // Step 2: Test LLMClient instantiation
            try {
                System.out.println("2. Testing LLMClient instantiation...");
                String endpoint = "http://localhost:7860";
                LLMClient client = new LLMClient(endpoint);
                System.out.println("   LLMClient instantiated successfully");
                
                // Test basic JSON creation
                JsonObject testObj = new JsonObject();
                testObj.addProperty("test", "value");
                System.out.println("   JsonObject created successfully: " + testObj);
            } catch (Exception e) {
                System.err.println("   Error instantiating LLMClient: " + e.getMessage());
                e.printStackTrace();
                return;
            }
            
            // Step 3: Test making an API request
            try {
                System.out.println("3. Testing API request...");
                String endpoint = "http://localhost:7860";
                LLMClient client = new LLMClient(endpoint);
                
                JsonObject testState = new JsonObject();
                testState.addProperty("context", "debug");
                testState.addProperty("message", "This is a debug test");
                
                System.out.println("   Sending request to LLM service...");
                JsonObject response = client.ask(testState);
                System.out.println("   Response: " + response);
                System.out.println("   API request completed successfully");
            } catch (Exception e) {
                System.err.println("   Error making API request: " + e.getMessage());
                e.printStackTrace();
                return;
            }
            
            System.out.println("=== LLM Debug Test completed successfully ===");
        } catch (Throwable t) {
            System.err.println("Unexpected error: " + t.getMessage());
            t.printStackTrace();
        }
    }
}