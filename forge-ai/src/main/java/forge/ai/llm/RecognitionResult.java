package forge.ai.llm;

import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.annotations.SerializedName;

/**
 * Response payload returned by the sidecar's {@code /recognize} endpoint: the
 * LLM's guess at the opponent's deck archetype, plus piloting advice for the
 * AI's own deck.
 */
public record RecognitionResult(
        String archetype,
        double confidence,
        String reasoning,
        List<String> alternatives,
        /** Advice on how the AI should pilot its own deck; {@code null} on old sidecars. */
        PilotingAdvice piloting,
        @SerializedName("schema_version") int schemaVersion) {

    /**
     * "Who's the Beatdown?" assessment from the sidecar. Tells the AI whether
     * it should currently behave as the aggressor or the controller in this
     * matchup, even when that contradicts the deck's natural strategy.
     */
    public record RoleAssessment(
            @SerializedName("ai_role") String aiRole,
            @SerializedName("opponent_role") String opponentRole,
            @SerializedName("winning_side") String winningSide,
            double margin,
            @SerializedName("role_flipped") boolean roleFlipped,
            String reasoning) {
    }

    /** Per-card value score for a card in the AI's hand. */
    public record HandValuation(
            String card,
            double value,
            String role,
            String reasoning) {
    }

    /** Inferred category of card the human likely still holds. */
    public record OpponentHandGuess(
            String category,
            @SerializedName("example_cards") List<String> exampleCards,
            double probability,
            String reasoning) {

        public OpponentHandGuess {
            if (exampleCards == null) {
                exampleCards = List.of();
            }
        }
    }

    /** Ordered target preference for a spell/ability. */
    public record TargetPriority(
            String spell,
            List<String> targets,
            String reasoning) {

        public TargetPriority {
            if (targets == null) {
                targets = List.of();
            }
        }
    }

    /** Combo-line recommendation for the AI's own deck (v7). */
    public record ComboPlan(
            @SerializedName("line_name") String lineName,
            @SerializedName("go_for_it_now") boolean goForItNow,
            @SerializedName("readiness_score") double readinessScore,
            @SerializedName("missing_pieces") List<String> missingPieces,
            @SerializedName("needed_cards") List<String> neededCards,
            @SerializedName("needed_mana") String neededMana,
            @SerializedName("preferred_line") String preferredLine,
            @SerializedName("wait_reason") String waitReason,
            List<String> sequence,
            @SerializedName("protection_plan") String protectionPlan,
            @SerializedName("risk_assessment") String riskAssessment,
            @SerializedName("action_adjustments") List<ActionScore> actionAdjustments) {

        public ComboPlan {
            if (missingPieces == null) {
                missingPieces = List.of();
            }
            if (neededCards == null) {
                neededCards = List.of();
            }
            if (sequence == null) {
                sequence = List.of();
            }
            if (actionAdjustments == null) {
                actionAdjustments = List.of();
            }
        }
    }

    /** One turn in the rolling early-game plan (v8). */
    public record PlannedTurn(
            int turn,
            String land,
            List<String> spells,
            @SerializedName("mana_rationale") String manaRationale,
            @SerializedName("draw_assumption") String drawAssumption,
            @SerializedName("opponent_adjustment") String opponentAdjustment) {
        public PlannedTurn {
            if (spells == null) {
                spells = List.of();
            }
        }
    }

    /** Opening-hand and rolling early-game plan (v8). */
    public record EarlyGamePlan(
            String decision,
            double confidence,
            @SerializedName("influence_weight") int influenceWeight,
            @SerializedName("keep_reason") String keepReason,
            @SerializedName("mulligan_reason") String mulliganReason,
            @SerializedName("bottom_cards") List<String> bottomCards,
            @SerializedName("land_sequence") List<String> landSequence,
            @SerializedName("fetch_plan") List<String> fetchPlan,
            @SerializedName("planned_turns") List<PlannedTurn> plannedTurns,
            @SerializedName("draw_assumptions") List<String> drawAssumptions,
            @SerializedName("probability_notes") String probabilityNotes,
            List<String> contingencies,
            @SerializedName("estimated_win_turn") Integer estimatedWinTurn,
            @SerializedName("estimated_loss_turn") Integer estimatedLossTurn,
            String reasoning,
            @SerializedName("last_updated_turn") int lastUpdatedTurn) {
        public EarlyGamePlan {
            if (bottomCards == null) {
                bottomCards = List.of();
            }
            if (landSequence == null) {
                landSequence = List.of();
            }
            if (fetchPlan == null) {
                fetchPlan = List.of();
            }
            if (plannedTurns == null) {
                plannedTurns = List.of();
            }
            if (drawAssumptions == null) {
                drawAssumptions = List.of();
            }
            if (contingencies == null) {
                contingencies = List.of();
            }
        }
    }

    /**
     * Per-action manabase decision (v10): whether/what to fetch, which land to
     * play, untapped/pay-life, and which utility lands to hold. LLM-owned on the
     * sidecar; validated there against cards actually available. All fields are
     * advisory — the engine falls back to stock heuristics when this is null or
     * a named card is no longer present.
     */
    public record ManaPlan(
            @SerializedName("crack_fetch") String crackFetch,
            @SerializedName("fetch_target") String fetchTarget,
            @SerializedName("fetch_alternatives") List<String> fetchAlternatives,
            @SerializedName("enter_untapped") boolean enterUntapped,
            @SerializedName("land_to_play") String landToPlay,
            @SerializedName("land_alternatives") List<String> landAlternatives,
            @SerializedName("color_needs") List<String> colorNeeds,
            @SerializedName("hold_utility_lands") List<String> holdUtilityLands,
            String reasoning) {
        public ManaPlan {
            if (crackFetch == null || crackFetch.isBlank()) {
                crackFetch = "auto";
            }
            if (fetchTarget == null) {
                fetchTarget = "";
            }
            if (fetchAlternatives == null) {
                fetchAlternatives = List.of();
            }
            if (landToPlay == null) {
                landToPlay = "";
            }
            if (landAlternatives == null) {
                landAlternatives = List.of();
            }
            if (colorNeeds == null) {
                colorNeeds = List.of();
            }
            if (holdUtilityLands == null) {
                holdUtilityLands = List.of();
            }
        }

        /** Fetch targets in priority order: explicit target first, then alternatives. */
        public List<String> fetchPriority() {
            if (fetchTarget == null || fetchTarget.isBlank()) {
                return fetchAlternatives;
            }
            final List<String> out = new java.util.ArrayList<>();
            out.add(fetchTarget);
            out.addAll(fetchAlternatives);
            return out;
        }
    }

    /**
     * Piloting advice for the AI's own deck. Nested in the {@code /recognize}
     * response under the {@code piloting} key.
     */
    public record PilotingAdvice(
            @SerializedName("own_archetype") String ownArchetype,
            @SerializedName("guide_source") String guideSource,
            @SerializedName("recommended_play") String recommendedPlay,
            String reasoning,
            List<String> alternatives,
            @SerializedName("mulligan_advice") String mulliganAdvice,
            List<ActionScore> actions,
            /** Board-aware additions (v4). All nullable / empty on older sidecars. */
            RoleAssessment role,
            @SerializedName("hand_values") List<HandValuation> handValues,
            @SerializedName("opponent_hand") List<OpponentHandGuess> opponentHand,
            @SerializedName("target_priorities") List<TargetPriority> targetPriorities,
            @SerializedName("combo_plan") ComboPlan comboPlan,
            @SerializedName("early_game_plan") EarlyGamePlan earlyGamePlan,
            @SerializedName("mana_plan") ManaPlan manaPlan) {

        public PilotingAdvice {
            if (actions == null) {
                actions = List.of();
            }
            if (handValues == null) {
                handValues = List.of();
            }
            if (opponentHand == null) {
                opponentHand = List.of();
            }
            if (targetPriorities == null) {
                targetPriorities = List.of();
            }
        }
    }

    /** Human-readable one-liner suitable for the game log. */
    public String toLogMessage() {
        return String.format("LLM opponent-deck guess: human opponent is playing '%s' (confidence %.0f%%)",
                archetype, confidence * 100.0);
    }

    /**
     * Human-readable piloting advice for the game log, or {@code null} if the
     * sidecar returned no usable advice.
     */
    public String toPilotingLogMessage() {
        if (piloting == null) {
            return null;
        }
        final boolean hasMulligan = piloting.mulliganAdvice() != null
                && !piloting.mulliganAdvice().isEmpty();
        final String advice = hasMulligan ? piloting.mulliganAdvice() : piloting.recommendedPlay();
        final StringBuilder sb = new StringBuilder();
        if (advice != null && !advice.isEmpty()) {
            sb.append(String.format("LLM piloting advice for AI's own deck ('%s'): %s",
                    piloting.ownArchetype(), advice));
        }
        // Append structured action scores
        if (piloting.actions() != null && !piloting.actions().isEmpty()) {
            final String actionLine = piloting.actions().stream()
                    .limit(3)
                    .map(ActionScore::toLogMessage)
                    .collect(Collectors.joining(" | "));
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append("Actions: ").append(actionLine);
        }
        if (piloting.comboPlan() != null && piloting.comboPlan().lineName() != null
                && !piloting.comboPlan().lineName().isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append(String.format("Combo: %s (%s, %.0f%% ready)",
                    piloting.comboPlan().lineName(),
                    piloting.comboPlan().goForItNow() ? "go now" : "setup",
                    piloting.comboPlan().readinessScore()));
        }
        if (piloting.earlyGamePlan() != null && piloting.earlyGamePlan().decision() != null
                && !piloting.earlyGamePlan().decision().isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append(String.format("Plan: %s (%.0f%% confidence)",
                    piloting.earlyGamePlan().decision(),
                    piloting.earlyGamePlan().confidence() * 100.0));
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    /** Game-log line summarizing the role assessment, or null when absent. */
    public String toRoleLogMessage() {
        if (piloting == null || piloting.role() == null) {
            return null;
        }
        final RoleAssessment r = piloting.role();
        return String.format(
                "Who's the Beatdown? AI=%s, opp=%s, winning=%s (margin %.0f%%)%s%s",
                r.aiRole(), r.opponentRole(), r.winningSide(), r.margin() * 100.0,
                r.roleFlipped() ? " [ROLE FLIPPED]" : "",
                r.reasoning() != null && !r.reasoning().isEmpty() ? " — " + r.reasoning() : "");
    }

    /** Game-log line summarizing the top hand valuations, or null when absent. */
    public String toHandValuesLogMessage() {
        if (piloting == null || piloting.handValues() == null || piloting.handValues().isEmpty()) {
            return null;
        }
        final String top = piloting.handValues().stream()
                .limit(5)
                .map(hv -> String.format("%s=%.0f(%s)", hv.card(), hv.value(), hv.role()))
                .collect(Collectors.joining(", "));
        return "AI hand value: " + top;
    }

    /** Game-log line summarizing inferred opponent hand contents, or null. */
    public String toOpponentHandLogMessage() {
        if (piloting == null || piloting.opponentHand() == null || piloting.opponentHand().isEmpty()) {
            return null;
        }
        final String top = piloting.opponentHand().stream()
                .limit(3)
                .map(g -> String.format("%s ~%.0f%%", g.category(), g.probability() * 100.0))
                .collect(Collectors.joining(", "));
        return "AI thinks opponent holds: " + top;
    }
}
