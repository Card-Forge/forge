package forge.research.onnx;

import java.util.Random;

/**
 * Samples an action index from logits with an action mask applied.
 * Uses numerically stable softmax and categorical sampling.
 */
public class MaskedSampler {

    private final Random random;

    public MaskedSampler() {
        this.random = new Random();
    }

    public MaskedSampler(long seed) {
        this.random = new Random(seed);
    }

    /**
     * Apply mask to logits, compute softmax, and sample from the categorical distribution.
     *
     * @param logits raw logits array (length 256)
     * @param mask   action mask (1 = valid, 0 = invalid)
     * @return sampled action index
     */
    public int sample(float[] logits, byte[] mask) {
        int n = logits.length;

        // Apply mask: invalid actions get -1e8
        float[] masked = new float[n];
        for (int i = 0; i < n; i++) {
            masked[i] = mask[i] == 1 ? logits[i] : -1e8f;
        }

        // Numerically stable softmax: subtract max first
        float max = Float.NEGATIVE_INFINITY;
        for (float v : masked) {
            if (v > max) max = v;
        }

        float sumExp = 0f;
        float[] probs = new float[n];
        for (int i = 0; i < n; i++) {
            probs[i] = (float) Math.exp(masked[i] - max);
            sumExp += probs[i];
        }

        // Normalize
        for (int i = 0; i < n; i++) {
            probs[i] /= sumExp;
        }

        // Sample from categorical
        float u = random.nextFloat();
        float cumulative = 0f;
        for (int i = 0; i < n; i++) {
            cumulative += probs[i];
            if (u <= cumulative) {
                return i;
            }
        }

        // Fallback: return last valid action
        for (int i = n - 1; i >= 0; i--) {
            if (mask[i] == 1) return i;
        }
        return 0;
    }
}
