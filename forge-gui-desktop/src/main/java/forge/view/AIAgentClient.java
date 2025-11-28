package forge.view;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * HTTP client for communicating with an external AI agent.
 * 
 * This client sends game state and action options to an AI endpoint
 * and receives back decision responses (action indices, target selections, etc.).
 * 
 * Request format:
 * {
 *   "gameId": "unique-game-identifier",
 *   "requestType": "action" | "target" | "combat" | ...,
 *   "gameState": { ... full game state ... },
 *   "actionState": { ... what needs to be decided ... },
 *   "context": { ... additional context ... }
 * }
 * 
 * Response format:
 * {
 *   "decision": {
 *     "type": "action" | "target" | "pass" | ...,
 *     "index": 0,  // for action/target selection
 *     "indices": [0, 1],  // for multi-select
 *     "metadata": { ... optional ... }
 *   }
 * }
 */
public class AIAgentClient {
    private final String endpointUrl;
    private final int timeoutMs;
    private final String authToken;
    private final Gson gson;
    
    private static final int DEFAULT_TIMEOUT_MS = 30000; // 30 seconds
    
    /**
     * Create an AI agent client with the specified configuration.
     * 
     * @param endpointUrl The URL of the AI agent endpoint
     * @param timeoutMs Connection and read timeout in milliseconds
     * @param authToken Optional bearer token for authentication (can be null)
     */
    public AIAgentClient(String endpointUrl, int timeoutMs, String authToken) {
        this.endpointUrl = endpointUrl;
        this.timeoutMs = timeoutMs > 0 ? timeoutMs : DEFAULT_TIMEOUT_MS;
        this.authToken = authToken;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }
    
    /**
     * Create an AI agent client with default timeout.
     */
    public AIAgentClient(String endpointUrl) {
        this(endpointUrl, DEFAULT_TIMEOUT_MS, null);
    }
    
    /**
     * Request a decision from the AI agent.
     * 
     * @param request The request containing game state and action options
     * @return The AI agent's decision response
     * @throws AIAgentException if communication fails or response is invalid
     */
    public AIAgentResponse requestDecision(AIAgentRequest request) throws AIAgentException {
        HttpURLConnection connection = null;
        try {
            URL url = URI.create(endpointUrl).toURL();
            connection = (HttpURLConnection) url.openConnection();
            
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(timeoutMs);
            connection.setReadTimeout(timeoutMs);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            
            if (authToken != null && !authToken.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + authToken);
            }
            
            // Send request
            String requestBody = gson.toJson(request.toJson());
            try (OutputStream os = connection.getOutputStream()) {
                os.write(requestBody.getBytes(StandardCharsets.UTF_8));
            }
            
            // Read response
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                throw new AIAgentException("AI agent returned HTTP " + responseCode);
            }
            
            try (InputStreamReader reader = new InputStreamReader(
                    connection.getInputStream(), StandardCharsets.UTF_8)) {
                StringBuilder sb = new StringBuilder();
                char[] buffer = new char[1024];
                int read;
                while ((read = reader.read(buffer)) != -1) {
                    sb.append(buffer, 0, read);
                }
                
                JsonObject responseJson = JsonParser.parseString(sb.toString()).getAsJsonObject();
                return AIAgentResponse.fromJson(responseJson);
            }
            
        } catch (IOException e) {
            throw new AIAgentException("Failed to communicate with AI agent: " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * Check if the AI agent endpoint is reachable.
     * Uses a minimal POST request since some AI endpoints may not support HEAD.
     * 
     * @return true if the endpoint responds with a 2xx status, false otherwise
     */
    public boolean isAvailable() {
        HttpURLConnection connection = null;
        try {
            URL url = URI.create(endpointUrl).toURL();
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            
            // Send minimal health check request
            try (OutputStream os = connection.getOutputStream()) {
                os.write("{\"healthCheck\":true}".getBytes(StandardCharsets.UTF_8));
            }
            
            int responseCode = connection.getResponseCode();
            // Only accept 2xx responses as truly available
            return responseCode >= 200 && responseCode < 300;
        } catch (IOException e) {
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    public String getEndpointUrl() {
        return endpointUrl;
    }
    
    /**
     * Request object sent to the AI agent.
     */
    public static class AIAgentRequest {
        private final String gameId;
        private final String requestType;
        private final JsonObject gameState;
        private final JsonObject actionState;
        private final JsonObject context;
        
        public AIAgentRequest(String gameId, String requestType, JsonObject gameState, 
                              JsonObject actionState, JsonObject context) {
            this.gameId = gameId;
            this.requestType = requestType;
            this.gameState = gameState;
            this.actionState = actionState;
            this.context = context;
        }
        
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("gameId", gameId);
            json.addProperty("requestType", requestType);
            json.add("gameState", gameState);
            json.add("actionState", actionState);
            if (context != null) {
                json.add("context", context);
            }
            return json;
        }
    }
    
    /**
     * Response object received from the AI agent.
     */
    public static class AIAgentResponse {
        private final String decisionType;
        private final int index;
        private final int[] indices;
        private final JsonObject metadata;
        
        private AIAgentResponse(String decisionType, int index, int[] indices, JsonObject metadata) {
            this.decisionType = decisionType;
            this.index = index;
            this.indices = indices;
            this.metadata = metadata;
        }
        
        public static AIAgentResponse fromJson(JsonObject json) throws AIAgentException {
            if (!json.has("decision")) {
                throw new AIAgentException("Response missing 'decision' field");
            }
            
            JsonObject decision = json.getAsJsonObject("decision");
            
            String type = decision.has("type") ? decision.get("type").getAsString() : "action";
            int index = decision.has("index") ? decision.get("index").getAsInt() : -1;
            
            int[] indices = null;
            if (decision.has("indices") && decision.get("indices").isJsonArray()) {
                com.google.gson.JsonArray arr = decision.getAsJsonArray("indices");
                indices = new int[arr.size()];
                for (int i = 0; i < arr.size(); i++) {
                    indices[i] = arr.get(i).getAsInt();
                }
            }
            
            JsonObject metadata = decision.has("metadata") && decision.get("metadata").isJsonObject() 
                ? decision.getAsJsonObject("metadata") : null;
            
            return new AIAgentResponse(type, index, indices, metadata);
        }
        
        public String getDecisionType() {
            return decisionType;
        }
        
        public int getIndex() {
            return index;
        }
        
        public int[] getIndices() {
            return indices;
        }
        
        public JsonObject getMetadata() {
            return metadata;
        }
        
        /**
         * Check if this is a "pass" decision.
         */
        public boolean isPass() {
            return "pass".equals(decisionType) || "pass_priority".equals(decisionType);
        }
    }
    
    /**
     * Exception thrown when AI agent communication fails.
     */
    public static class AIAgentException extends Exception {
        public AIAgentException(String message) {
            super(message);
        }
        
        public AIAgentException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
