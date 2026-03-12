package forge.research.onnx;

import java.util.List;

import forge.research.proto.ActionOption;
import forge.research.proto.CardState;
import forge.research.proto.DecisionPoint;
import forge.research.proto.GameInfo;
import forge.research.proto.ManaPool;
import forge.research.proto.Observation;
import forge.research.proto.PlayerState;
import forge.research.proto.StackEntry;

/**
 * Converts protobuf Observation + DecisionPoint into flat float arrays
 * matching the Python env._build_observation() layout for ONNX inference.
 */
public class ObservationConverter {

    private static final int CARD_FEATURES = 19;
    private static final int STACK_FEATURES = 8;
    private static final int MAX_HAND = 10;
    private static final int MAX_BATTLEFIELD = 20;
    private static final int MAX_GRAVEYARD = 15;
    private static final int MAX_EXILE = 10;
    private static final int MAX_STACK = 10;
    private static final int MAX_ACTIONS = 256;
    private static final int NUM_DECISION_TYPES = 15;
    private static final int ACTION_FEATURES = 7;

    public float[] gameInfo(Observation obs) {
        GameInfo gi = obs.getGameInfo();
        return new float[]{gi.getTurn(), gi.getPhase(), gi.getActivePlayerIndex(), gi.getPriorityPlayerIndex(),
                gi.getAgentMulliganCount(), gi.getOpponentMulliganCount()};
    }

    public float[] playerScalars(PlayerState ps) {
        ManaPool mp = ps.getManaPool();
        return new float[]{
            ps.getLife(),
            ps.getHandSize(),
            ps.getLibrarySize(),
            ps.getLandsPlayed(),
            ps.getMaxLands(),
            mp.getWhite(),
            mp.getBlue(),
            mp.getBlack(),
            mp.getRed(),
            mp.getGreen(),
            mp.getColorless(),
        };
    }

    public float[] cardMatrix(List<CardState> cards, int maxCards) {
        float[] mat = new float[maxCards * CARD_FEATURES];
        java.util.Arrays.fill(mat, -1f);
        int count = Math.min(cards.size(), maxCards);
        for (int i = 0; i < count; i++) {
            CardState c = cards.get(i);
            int base = i * CARD_FEATURES;
            mat[base]      = c.getNameId();
            mat[base + 1]  = c.getPower();
            mat[base + 2]  = c.getToughness();
            mat[base + 3]  = c.getCmc();
            mat[base + 4]  = c.getTapped() ? 1f : 0f;
            mat[base + 5]  = c.getSummoningSick() ? 1f : 0f;
            mat[base + 6]  = c.getColorsBitmask();
            mat[base + 7]  = c.getDamage();
            mat[base + 8]  = c.getLoyalty();
            mat[base + 9]  = c.getControllerIndex();
            mat[base + 10] = c.getAttacking() ? 1f : 0f;
            mat[base + 11] = c.getBlocking() ? 1f : 0f;
            mat[base + 12] = c.getCounterCount();
            mat[base + 13] = c.getKeywordCount();
            mat[base + 14] = c.getCardId();
            mat[base + 15] = c.getOwnerIndex();
            mat[base + 16] = c.getTypeBitmask();
            mat[base + 17] = c.getKeywordBitmask();
            mat[base + 18] = c.getPlusOneCounterCount();
        }
        return mat;
    }

    public float[] stackMatrix(List<StackEntry> entries) {
        float[] mat = new float[MAX_STACK * STACK_FEATURES];
        java.util.Arrays.fill(mat, -1f);
        int count = Math.min(entries.size(), MAX_STACK);
        for (int i = 0; i < count; i++) {
            StackEntry e = entries.get(i);
            int base = i * STACK_FEATURES;
            mat[base]     = e.getSourceCardId();
            mat[base + 1] = e.getControllerIndex();
            // rest are 0 (reserved), overwrite from -1
            for (int j = 2; j < STACK_FEATURES; j++) {
                mat[base + j] = 0f;
            }
        }
        return mat;
    }

    public float[] decisionTypeOneHot(DecisionPoint dp) {
        float[] vec = new float[NUM_DECISION_TYPES];
        if (dp != null) {
            int dt = dp.getType().getNumber();
            if (dt >= 0 && dt < NUM_DECISION_TYPES) {
                vec[dt] = 1f;
            }
        }
        return vec;
    }

    public float[] actionFeatures(DecisionPoint dp) {
        float[] mat = new float[MAX_ACTIONS * ACTION_FEATURES];
        java.util.Arrays.fill(mat, -1f);
        if (dp == null) return mat;

        for (ActionOption action : dp.getLegalActionsList()) {
            int idx = action.getIndex();
            if (idx < 0 || idx >= MAX_ACTIONS) continue;

            int srcNameId = action.getSourceNameId();
            int srcCardId = action.getSourceCardId();
            float isPass = (srcNameId == 0 && srcCardId == 0) ? 1f : 0f;

            int base = idx * ACTION_FEATURES;
            mat[base]     = (float) Math.log1p(srcNameId);
            mat[base + 1] = (float) Math.log1p(srcCardId);
            mat[base + 2] = isPass;
            mat[base + 3] = action.getTargetIsPlayer() ? 1f : 0f;
            mat[base + 4] = (float) Math.log1p(action.getTargetNameId());
            mat[base + 5] = (float) Math.log1p(action.getTargetCardId());
            mat[base + 6] = action.getTargetIsOwn() ? 1f : 0f;
        }
        return mat;
    }

    public byte[] actionMask(DecisionPoint dp) {
        byte[] mask = new byte[MAX_ACTIONS];
        if (dp != null) {
            for (ActionOption action : dp.getLegalActionsList()) {
                int idx = action.getIndex();
                if (idx >= 0 && idx < MAX_ACTIONS) {
                    mask[idx] = 1;
                }
            }
        }
        return mask;
    }

    // Convenience: all ONNX inputs as float arrays (13 total, excluding action_mask)
    public float[][] allInputs(Observation obs, DecisionPoint dp) {
        return new float[][]{
            gameInfo(obs),
            playerScalars(obs.getAgentPlayer()),
            playerScalars(obs.getOpponentPlayer()),
            cardMatrix(obs.getAgentPlayer().getHandList(), MAX_HAND),
            cardMatrix(obs.getAgentPlayer().getBattlefieldList(), MAX_BATTLEFIELD),
            cardMatrix(obs.getOpponentPlayer().getBattlefieldList(), MAX_BATTLEFIELD),
            cardMatrix(obs.getAgentPlayer().getGraveyardList(), MAX_GRAVEYARD),
            cardMatrix(obs.getOpponentPlayer().getGraveyardList(), MAX_GRAVEYARD),
            cardMatrix(obs.getAgentPlayer().getExileList(), MAX_EXILE),
            cardMatrix(obs.getOpponentPlayer().getExileList(), MAX_EXILE),
            stackMatrix(obs.getStackList()),
            decisionTypeOneHot(dp),
            actionFeatures(dp),
        };
    }
}
