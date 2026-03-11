package forge.research;

import ai.onnxruntime.OrtException;

import forge.LobbyPlayer;
import forge.game.Game;
import forge.game.player.Player;
import forge.research.onnx.MaskedSampler;
import forge.research.onnx.ObservationConverter;
import forge.research.onnx.OnnxInferenceEngine;
import forge.research.proto.DecisionPoint;
import forge.research.proto.Observation;

/**
 * PlayerController that runs ONNX inference locally instead of blocking on gRPC queues.
 * Extends RlPlayerController — all 900+ lines of decision-building logic are inherited.
 */
public class OnnxPlayerController extends RlPlayerController {

    private final OnnxInferenceEngine engine;
    private final ObservationConverter converter = new ObservationConverter();
    private final MaskedSampler sampler = new MaskedSampler();

    public OnnxPlayerController(Game game, Player p, LobbyPlayer lp,
            int playerIndex, OnnxInferenceEngine engine) {
        super(game, p, lp, playerIndex);
        this.engine = engine;
    }

    @Override
    protected int queryAgent(DecisionPoint decisionPoint) {
        try {
            Game game = getGame();
            Player opponent = null;
            for (Player p : game.getPlayers()) {
                if (p != player) {
                    opponent = p;
                    break;
                }
            }
            if (opponent == null) {
                opponent = player;
            }

            Observation obs = observationBuilder.buildObservation(game, player, opponent);

            // Convert to float arrays for ONNX
            float[][] inputs = converter.allInputs(obs, decisionPoint);
            byte[] mask = converter.actionMask(decisionPoint);

            // Run ONNX inference
            float[] logits = engine.infer(inputs);

            // Sample action with mask
            int action = sampler.sample(logits, mask);

            return action;
        } catch (OrtException e) {
            System.err.println("ONNX inference failed: " + e.getMessage());
            return 0; // fallback: first action
        }
    }
}
