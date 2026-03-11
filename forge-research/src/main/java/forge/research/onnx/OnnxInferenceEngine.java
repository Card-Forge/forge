package forge.research.onnx;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads an ONNX model and runs inference to produce logits.
 * Takes 13 float arrays matching the ONNX input names, creates OnnxTensor objects
 * with proper shapes, runs the session, and returns logits float[256].
 */
public class OnnxInferenceEngine implements AutoCloseable {

    private static final String[] INPUT_NAMES = {
        "game_info",            // (1, 6)
        "agent_scalars",        // (1, 11)
        "opponent_scalars",     // (1, 11)
        "agent_hand",           // (1, 10, 16)
        "agent_battlefield",    // (1, 20, 16)
        "opponent_battlefield", // (1, 20, 16)
        "agent_graveyard",      // (1, 15, 16)
        "opponent_graveyard",   // (1, 15, 16)
        "agent_exile",          // (1, 10, 16)
        "opponent_exile",       // (1, 10, 16)
        "stack",                // (1, 10, 8)
        "decision_type",        // (1, 15)
        "action_features",      // (1, 256, 7)
    };

    // Shapes for 2D inputs (rows, cols) — 1D inputs have cols only
    private static final int[][] INPUT_2D_SHAPES = {
        null,           // game_info: 1D, 6
        null,           // agent_scalars: 1D, 11
        null,           // opponent_scalars: 1D, 11
        {10, 16},       // agent_hand
        {20, 16},       // agent_battlefield
        {20, 16},       // opponent_battlefield
        {15, 16},       // agent_graveyard
        {15, 16},       // opponent_graveyard
        {10, 16},       // agent_exile
        {10, 16},       // opponent_exile
        {10, 8},        // stack
        null,           // decision_type: 1D, 15
        {256, 7},       // action_features
    };

    private final OrtEnvironment env;
    private final OrtSession session;

    public OnnxInferenceEngine(String modelPath) throws OrtException {
        this.env = OrtEnvironment.getEnvironment();
        this.session = env.createSession(modelPath);
    }

    /**
     * Run inference on 13 float arrays and return logits float[256].
     *
     * @param inputs 13 float arrays in the order of INPUT_NAMES
     * @return logits array of length 256
     */
    public float[] infer(float[]... inputs) throws OrtException {
        if (inputs.length != INPUT_NAMES.length) {
            throw new IllegalArgumentException(
                "Expected " + INPUT_NAMES.length + " inputs, got " + inputs.length);
        }

        Map<String, OnnxTensor> feed = new HashMap<>();
        OnnxTensor[] tensors = new OnnxTensor[inputs.length];

        try {
            for (int i = 0; i < inputs.length; i++) {
                if (INPUT_2D_SHAPES[i] != null) {
                    // 2D input → reshape to [1, rows, cols]
                    int rows = INPUT_2D_SHAPES[i][0];
                    int cols = INPUT_2D_SHAPES[i][1];
                    float[][][] data = new float[1][rows][cols];
                    for (int r = 0; r < rows; r++) {
                        System.arraycopy(inputs[i], r * cols, data[0][r], 0, cols);
                    }
                    tensors[i] = OnnxTensor.createTensor(env, data);
                } else {
                    // 1D input → shape [1, N]
                    float[][] data = new float[1][inputs[i].length];
                    System.arraycopy(inputs[i], 0, data[0], 0, inputs[i].length);
                    tensors[i] = OnnxTensor.createTensor(env, data);
                }
                feed.put(INPUT_NAMES[i], tensors[i]);
            }

            OrtSession.Result result = session.run(feed);
            float[][] output = (float[][]) result.get(0).getValue();
            result.close();
            return output[0]; // batch dim 0
        } finally {
            for (OnnxTensor t : tensors) {
                if (t != null) {
                    t.close();
                }
            }
        }
    }

    @Override
    public void close() {
        try {
            session.close();
        } catch (OrtException e) {
            // ignore
        }
    }
}
