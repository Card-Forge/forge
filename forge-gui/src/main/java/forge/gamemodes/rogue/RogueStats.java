package forge.gamemodes.rogue;

import forge.item.PaperCard;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Trigger-based stat tracking for Rogue Commander mode.
 * Each stat is self-contained: it handles its own counter increments,
 * max-value tracking, and unlock checks via trigger overrides.
 */
public enum RogueStats {

    // --- Max-value stats: fire on match and/or side node as needed ---

    MAX_LIFE("MaxLife", "Have %s+ life at the end of any match.") {
        @Override public int evaluate(RogueRun run, RogueMetaProgress p) { return run.getLastMatchRawLife(); }
        @Override public void onMatchCompleted(RogueRun run, RogueMetaProgress p, boolean won) {
            p.updateStat(this, evaluate(run, p), run);
        }
    },
    MAX_GOLD("MaxGold", "Earn %s gold in a run.") {
        @Override public int evaluate(RogueRun run, RogueMetaProgress p) { return run.getCurrentGold(); }
        @Override public void onMatchCompleted(RogueRun run, RogueMetaProgress p, boolean won) {
            p.updateStat(this, evaluate(run, p), run);
        }
        @Override public void onSideNodeCompleted(RogueRun run, RogueMetaProgress p) {
            p.updateStat(this, evaluate(run, p), run);
        }
    },
    CREATURE_TYPES("CreatureTypes", "Have %s+ creature types in your deck.") {
        @Override public int evaluate(RogueRun run, RogueMetaProgress p) {
            if (run.getCurrentDeck() == null || run.getCurrentDeck().getMain() == null) return 0;
            Set<String> types = new HashSet<>();
            for (PaperCard card : run.getCurrentDeck().getMain().toFlatList()) {
                if (card.getRules().getType().isCreature()) {
                    types.addAll(card.getRules().getType().getCreatureTypes());
                }
            }
            return types.size();
        }
        @Override public void onMatchCompleted(RogueRun run, RogueMetaProgress p, boolean won) {
            p.updateStat(this, evaluate(run, p), run);
        }
        @Override public void onSideNodeCompleted(RogueRun run, RogueMetaProgress p) {
            p.updateStat(this, evaluate(run, p), run);
        }
    },
    MAX_ARTIFACTS("MaxArtifacts", "Have %s+ artifacts in your deck.") {
        @Override public int evaluate(RogueRun run, RogueMetaProgress p) {
            if (run.getCurrentDeck() == null || run.getCurrentDeck().getMain() == null) return 0;
            int count = 0;
            for (PaperCard card : run.getCurrentDeck().getMain().toFlatList()) {
                if (card.getRules().getType().isArtifact()) {
                    count++;
                }
            }
            return count;
        }
        @Override public void onMatchCompleted(RogueRun run, RogueMetaProgress p, boolean won) {
            p.updateStat(this, evaluate(run, p), run);
        }
        @Override public void onSideNodeCompleted(RogueRun run, RogueMetaProgress p) {
            p.updateStat(this, evaluate(run, p), run);
        }
    },
    MAX_SHARED_CREATURE_TYPE("MaxSharedCreatureType", "Have %s+ creatures that share a creature type in your deck.") {
        @Override public int evaluate(RogueRun run, RogueMetaProgress p) {
            if (run.getCurrentDeck() == null || run.getCurrentDeck().getMain() == null) return 0;
            Map<String, Integer> typeCounts = new HashMap<>();
            for (PaperCard card : run.getCurrentDeck().getMain().toFlatList()) {
                if (card.getRules().getType().isCreature()) {
                    for (String type : card.getRules().getType().getCreatureTypes()) {
                        typeCounts.merge(type, 1, Integer::sum);
                    }
                }
            }
            int max = 0;
            for (int count : typeCounts.values()) {
                if (count > max) max = count;
            }
            return max;
        }
        @Override public void onMatchCompleted(RogueRun run, RogueMetaProgress p, boolean won) {
            p.updateStat(this, evaluate(run, p), run);
        }
        @Override public void onSideNodeCompleted(RogueRun run, RogueMetaProgress p) {
            p.updateStat(this, evaluate(run, p), run);
        }
    },
    LEGENDARY_PERMANENTS("LegendaryPermanents", "Have %s+ legendary permanents in your deck.") {
        @Override public int evaluate(RogueRun run, RogueMetaProgress p) {
            if (run.getCurrentDeck() == null || run.getCurrentDeck().getMain() == null) return 0;
            int count = 0;
            for (PaperCard card : run.getCurrentDeck().getMain().toFlatList()) {
                if (card.getRules().getType().isLegendary() && card.getRules().getType().isPermanent()) {
                    count++;
                }
            }
            return count;
        }
        @Override public void onMatchCompleted(RogueRun run, RogueMetaProgress p, boolean won) {
            p.updateStat(this, evaluate(run, p), run);
        }
        @Override public void onSideNodeCompleted(RogueRun run, RogueMetaProgress p) {
            p.updateStat(this, evaluate(run, p), run);
        }
    },
    NONBASIC_LANDS("NonbasicLands", "Have %s+ nonbasic lands in your deck.") {
        @Override public int evaluate(RogueRun run, RogueMetaProgress p) {
            if (run.getCurrentDeck() == null || run.getCurrentDeck().getMain() == null) return 0;
            int count = 0;
            for (PaperCard card : run.getCurrentDeck().getMain().toFlatList()) {
                if (card.getRules().getType().isLand() && !card.getRules().getType().isBasicLand()) {
                    count++;
                }
            }
            return count;
        }
        @Override public void onMatchCompleted(RogueRun run, RogueMetaProgress p, boolean won) {
            p.updateStat(this, evaluate(run, p), run);
        }
        @Override public void onSideNodeCompleted(RogueRun run, RogueMetaProgress p) {
            p.updateStat(this, evaluate(run, p), run);
        }
    },

    // --- Counter stats: fire on specific events only ---

    RUNS_STARTED("RunsStarted", "Start %s Run(s).") {
        @Override public int evaluate(RogueRun run, RogueMetaProgress p) {
            return p.getStatValueForRun(getConditionKey(), run);
        }
        @Override public void onRunStarted(RogueRun run, RogueMetaProgress p) {
            p.trackCommanderStarted(run);
            p.updateStat(this, evaluate(run, p) + 1, run);
        }
    },
    MATCHES_WON("MatchesWon", "Win %s matches.") {
        @Override public int evaluate(RogueRun run, RogueMetaProgress p) {
            return p.getStatValueForRun(getConditionKey(), run);
        }
        @Override public void onMatchCompleted(RogueRun run, RogueMetaProgress p, boolean won) {
            if (won) p.updateStat(this, evaluate(run, p) + 1, run);
        }
    },
    MATCHES_LOST("MatchesLost", "Lose %s matches.") {
        @Override public int evaluate(RogueRun run, RogueMetaProgress p) {
            return p.getStatValueForRun(getConditionKey(), run);
        }
        @Override public void onMatchCompleted(RogueRun run, RogueMetaProgress p, boolean won) {
            if (!won) p.updateStat(this, evaluate(run, p) + 1, run);
        }
    },
    RUNS_COMPLETED("RunsCompleted", "Complete %s Run(s).") {
        @Override public int evaluate(RogueRun run, RogueMetaProgress p) {
            return p.getStatValueForRun(getConditionKey(), run);
        }
        @Override public void onRunCompleted(RogueRun run, RogueMetaProgress p, boolean won) {
            p.updateStat(this, evaluate(run, p) + 1, run);
        }
    },
    RUNS_WON("RunsWon", "Win a Run.") {
        @Override public int evaluate(RogueRun run, RogueMetaProgress p) {
            return p.getStatValueForRun(getConditionKey(), run);
        }
        @Override public void onRunCompleted(RogueRun run, RogueMetaProgress p, boolean won) {
            if (won) {
                p.mergeRunsWonPerCommander(run);
                p.updateStat(this, evaluate(run, p) + 1, run);
            }
        }
    };

    private final String conditionKey;
    private final String unlockDescriptionFormat;

    RogueStats(String conditionKey, String unlockDescriptionFormat) {
        this.conditionKey = conditionKey;
        this.unlockDescriptionFormat = unlockDescriptionFormat;
    }

    public String getConditionKey() { return conditionKey; }

    public boolean isCounterStat() {
        return this == RUNS_STARTED || this == MATCHES_WON || this == MATCHES_LOST
            || this == RUNS_COMPLETED || this == RUNS_WON;
    }

    /** Returns human-readable unlock description with the threshold value substituted. */
    public String getUnlockDescription(String value) {
        return String.format(unlockDescriptionFormat, value);
    }

    public abstract int evaluate(RogueRun run, RogueMetaProgress progress);

    // Default trigger methods: no-op (stats override the ones they care about)
    public void onRunStarted(RogueRun run, RogueMetaProgress p) {}
    public void onMatchCompleted(RogueRun run, RogueMetaProgress p, boolean won) {}
    public void onSideNodeCompleted(RogueRun run, RogueMetaProgress p) {}
    public void onRunCompleted(RogueRun run, RogueMetaProgress p, boolean won) {}

    // --- Static dispatchers (called from game event sites) ---

    public static void fireOnRunStarted(RogueRun run, RogueMetaProgress p) {
        for (RogueStats s : values()) s.onRunStarted(run, p);
        p.save();
    }

    public static void fireOnMatchCompleted(RogueRun run, RogueMetaProgress p, boolean won) {
        for (RogueStats s : values()) s.onMatchCompleted(run, p, won);
        p.save();
    }

    public static void fireOnSideNodeCompleted(RogueRun run, RogueMetaProgress p) {
        for (RogueStats s : values()) s.onSideNodeCompleted(run, p);
        p.save();
    }

    public static void fireOnRunCompleted(RogueRun run, RogueMetaProgress p, boolean won) {
        for (RogueStats s : values()) s.onRunCompleted(run, p, won);
        p.save();
    }

    public static RogueStats fromKey(String key) {
        for (RogueStats s : values()) {
            if (s.conditionKey.equals(key)) return s;
        }
        return null;
    }
}
