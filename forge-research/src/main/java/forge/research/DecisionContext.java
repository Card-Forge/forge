package forge.research;

import forge.research.proto.DecisionPoint;
import forge.research.proto.Observation;

/**
 * Payload sent from game thread to gRPC thread when a decision is needed.
 * Contains the current game observation and the legal actions available.
 */
public class DecisionContext {

    private final Observation observation;
    private final DecisionPoint decisionPoint;
    private final boolean gameOver;
    private final float reward;

    public DecisionContext(Observation observation, DecisionPoint decisionPoint) {
        this.observation = observation;
        this.decisionPoint = decisionPoint;
        this.gameOver = false;
        this.reward = 0f;
    }

    private DecisionContext(Observation observation, float reward) {
        this.observation = observation;
        this.decisionPoint = null;
        this.gameOver = true;
        this.reward = reward;
    }

    public static DecisionContext gameOver(Observation observation, float reward) {
        return new DecisionContext(observation, reward);
    }

    public Observation getObservation() {
        return observation;
    }

    public DecisionPoint getDecisionPoint() {
        return decisionPoint;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public float getReward() {
        return reward;
    }
}
