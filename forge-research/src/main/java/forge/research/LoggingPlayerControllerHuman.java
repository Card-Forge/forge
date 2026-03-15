package forge.research;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.ImmutablePair;

import forge.LobbyPlayer;
import forge.ai.ComputerUtilAbility;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.mana.Mana;
import forge.game.player.DelayedReveal;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.ZoneType;
import forge.player.PlayerControllerHuman;
import forge.research.observation.CardRegistry;
import forge.util.collect.FCollectionView;

/**
 * Extends PlayerControllerHuman to log every decision in RL action-space format.
 * The normal Swing GUI handles all user interaction; this class wraps each decision
 * method to record the available options and the human's choice.
 */
public class LoggingPlayerControllerHuman extends PlayerControllerHuman {

    private final List<Map<String, Object>> log;
    private final CardRegistry cardRegistry = CardRegistry.getInstance();
    private final int playerIndex;

    public LoggingPlayerControllerHuman(Game game, Player p, LobbyPlayer lp,
            List<Map<String, Object>> log, int playerIndex) {
        super(game, p, lp);
        this.log = log;
        this.playerIndex = playerIndex;
    }

    @Override
    public boolean isGuiPlayer() {
        return true;
    }

    // ═══════════════════════════════════════════════════════════════
    // Logging helpers
    // ═══════════════════════════════════════════════════════════════

    private Map<String, Object> newEntry(String decisionType, String prompt) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("decision_type", decisionType);
        entry.put("prompt", prompt);
        entry.put("player_index", playerIndex);
        Game game = getGame();
        if (game != null) {
            entry.put("turn", game.getPhaseHandler().getTurn());
            forge.game.phase.PhaseType phase = game.getPhaseHandler().getPhase();
            entry.put("phase", phase != null ? phase.toString() : "PREGAME");
        }
        return entry;
    }

    private Map<String, Object> buildActionMap(int index, String description, Card source) {
        return buildActionMap(index, description, source, null, false);
    }

    private Map<String, Object> buildActionMap(int index, String description, Card source,
            GameEntity target, boolean targetIsOwn) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("index", index);
        action.put("description", description != null ? description : "");
        if (source != null) {
            action.put("source_card_name", source.getName());
            action.put("source_card_id", source.getId());
            action.put("source_name_id", cardRegistry.getNameId(source.getName()));
        }
        if (target != null) {
            if (target instanceof Card) {
                Card tc = (Card) target;
                action.put("target_card_id", tc.getId());
                action.put("target_name_id", cardRegistry.getNameId(tc.getName()));
            }
            action.put("target_is_own", targetIsOwn);
            action.put("target_is_player", target instanceof Player);
        }
        return action;
    }

    private boolean isOwnEntity(GameEntity entity) {
        if (entity instanceof Card) {
            return ((Card) entity).getController() == player;
        }
        if (entity instanceof Player) {
            return entity == player;
        }
        return false;
    }

    private void logEntry(Map<String, Object> entry) {
        synchronized (log) {
            log.add(entry);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // chooseSpellAbilityToPlay
    // ═══════════════════════════════════════════════════════════════

    @Override
    public List<SpellAbility> chooseSpellAbilityToPlay() {
        // Compute playable SAs before showing GUI (same logic as RlPlayerController)
        CardCollectionView cards = ComputerUtilAbility.getAvailableCards(getGame(), player);
        List<SpellAbility> playable = ComputerUtilAbility.getSpellAbilities(cards, player);
        playable.removeIf(sa -> sa.isManaAbility());

        // Call the human GUI
        List<SpellAbility> result = super.chooseSpellAbilityToPlay();

        // Only log if there were actual choices
        if (!playable.isEmpty()) {
            Map<String, Object> entry = newEntry("CHOOSE_SPELL_ABILITY",
                    "Choose spell or ability to play (or pass)");
            List<Map<String, Object>> actions = new ArrayList<>();
            actions.add(buildActionMap(0, "Pass priority", null));
            for (int i = 0; i < playable.size(); i++) {
                SpellAbility sa = playable.get(i);
                actions.add(buildActionMap(i + 1, sa.toString(), sa.getHostCard()));
            }
            entry.put("legal_actions", actions);

            // Map result back to index
            int chosenIndex = 0; // default = pass
            if (result != null && !result.isEmpty()) {
                SpellAbility chosen = result.get(0);
                for (int i = 0; i < playable.size(); i++) {
                    if (playable.get(i) == chosen) {
                        chosenIndex = i + 1;
                        break;
                    }
                }
                entry.put("chosen_description", chosen.toString());
            } else {
                entry.put("chosen_description", "Pass priority");
            }
            entry.put("chosen_index", chosenIndex);
            logEntry(entry);
        }

        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    // declareAttackers
    // ═══════════════════════════════════════════════════════════════

    @Override
    public void declareAttackers(Player attacker, Combat combat) {
        // Snapshot attackable creatures before GUI
        GameEntity defender = combat.getDefenders().isEmpty() ? null : combat.getDefenders().iterator().next();
        List<Card> attackable = new ArrayList<>();
        if (defender != null) {
            for (Card c : attacker.getCreaturesInPlay()) {
                if (CombatUtil.canAttack(c, defender)) {
                    attackable.add(c);
                }
            }
        }

        // Let the human choose via GUI
        super.declareAttackers(attacker, combat);

        // Diff: which creatures are now attacking?
        if (!attackable.isEmpty()) {
            Map<String, Object> entry = newEntry("DECLARE_ATTACKERS", "Declare attackers");
            List<Map<String, Object>> actions = new ArrayList<>();
            for (int i = 0; i < attackable.size(); i++) {
                Card c = attackable.get(i);
                boolean isAttacking = combat.isAttacking(c);
                actions.add(buildActionMap(i, c.getName() + " (" + c.getNetPower()
                        + "/" + c.getNetToughness() + ")" + (isAttacking ? " [ATTACKING]" : ""), c));
            }
            entry.put("legal_actions", actions);

            // Log which were chosen
            List<Integer> chosenIndices = new ArrayList<>();
            for (int i = 0; i < attackable.size(); i++) {
                if (combat.isAttacking(attackable.get(i))) {
                    chosenIndices.add(i);
                }
            }
            entry.put("chosen_indices", chosenIndices);
            entry.put("chosen_description", chosenIndices.size() + " attackers declared");
            logEntry(entry);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // declareBlockers
    // ═══════════════════════════════════════════════════════════════

    @Override
    public void declareBlockers(Player defender, Combat combat) {
        // Snapshot blockable creatures before GUI
        List<Card> blockable = new ArrayList<>();
        for (Card c : defender.getCreaturesInPlay()) {
            if (CombatUtil.canBlock(c, combat)) {
                blockable.add(c);
            }
        }
        List<Card> attackers = new ArrayList<>(combat.getAttackers());

        // Let the human choose via GUI
        super.declareBlockers(defender, combat);

        // Log blocking assignments
        if (!blockable.isEmpty() && !attackers.isEmpty()) {
            Map<String, Object> entry = newEntry("DECLARE_BLOCKERS", "Declare blockers");

            List<Map<String, Object>> actions = new ArrayList<>();
            for (int i = 0; i < blockable.size(); i++) {
                Card blocker = blockable.get(i);
                CardCollection blocked = combat.getAttackersBlockedBy(blocker);
                String desc = blocker.getName() + " (" + blocker.getNetPower()
                        + "/" + blocker.getNetToughness() + ")";
                Card firstBlocked = (blocked != null && !blocked.isEmpty()) ? blocked.getFirst() : null;
                if (firstBlocked != null) {
                    desc += " -> blocks " + firstBlocked.getName();
                }
                actions.add(buildActionMap(i, desc, blocker,
                        firstBlocked, firstBlocked != null && isOwnEntity(firstBlocked)));
            }
            entry.put("legal_actions", actions);

            List<Integer> chosenIndices = new ArrayList<>();
            for (int i = 0; i < blockable.size(); i++) {
                if (combat.isBlocking(blockable.get(i))) {
                    chosenIndices.add(i);
                }
            }
            entry.put("chosen_indices", chosenIndices);
            entry.put("chosen_description", chosenIndices.size() + " blockers declared");
            logEntry(entry);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Binary decisions
    // ═══════════════════════════════════════════════════════════════

    @Override
    public boolean mulliganKeepHand(Player mulliganingPlayer, int cardsToReturn) {
        boolean result = super.mulliganKeepHand(mulliganingPlayer, cardsToReturn);
        logBinary("MULLIGAN", "Keep hand or mulligan?",
                "Keep", "Mulligan", result ? 0 : 1, null);
        return result;
    }

    @Override
    public boolean confirmAction(SpellAbility sa, PlayerActionConfirmMode mode, String message,
            List<String> options, Card cardToShow, Map<String, Object> params) {
        boolean result = super.confirmAction(sa, mode, message, options, cardToShow, params);
        logBinary("CONFIRM_ACTION", message != null ? message : "Confirm action?",
                "Yes", "No", result ? 0 : 1, cardToShow);
        return result;
    }

    @Override
    public boolean confirmTrigger(WrappedAbility sa) {
        if (sa.isMandatory()) {
            return super.confirmTrigger(sa);
        }
        boolean result = super.confirmTrigger(sa);
        logBinary("CONFIRM_TRIGGER", "Confirm trigger: " + sa.toString(),
                "Yes", "No", result ? 0 : 1, sa.getHostCard());
        return result;
    }

    @Override
    public boolean playTrigger(Card host, WrappedAbility wrapperAbility, boolean isMandatory) {
        boolean result = super.playTrigger(host, wrapperAbility, isMandatory);
        if (!isMandatory) {
            logBinary("PLAY_TRIGGER", "Play optional trigger: " + wrapperAbility.toString(),
                    "Yes", "No", result ? 0 : 1, host);
        }
        return result;
    }

    @Override
    public boolean confirmReplacementEffect(ReplacementEffect replacementEffect,
            SpellAbility effectSA, GameEntity affected, String question) {
        boolean result = super.confirmReplacementEffect(replacementEffect, effectSA, affected, question);
        logBinary("CONFIRM_REPLACEMENT", question != null ? question : "Apply replacement effect?",
                "Yes", "No", result ? 0 : 1, replacementEffect.getHostCard());
        return result;
    }

    @Override
    public boolean playSaFromPlayEffect(SpellAbility tgtSA) {
        boolean result = super.playSaFromPlayEffect(tgtSA);
        logBinary("PLAY_SA_FROM_EFFECT",
                "Play " + tgtSA.getHostCard().getName() + " from effect?",
                "Yes", "No", result ? 0 : 1, tgtSA.getHostCard());
        return result;
    }

    @Override
    public boolean willPutCardOnTop(Card c) {
        boolean result = super.willPutCardOnTop(c);
        logBinary("WILL_PUT_ON_TOP", "Put " + c.getName() + " on top of library?",
                "Put on top", "Put on bottom", result ? 0 : 1, c);
        return result;
    }

    private void logBinary(String type, String prompt, String yesLabel, String noLabel,
            int chosenIndex, Card source) {
        Map<String, Object> entry = newEntry(type, prompt);
        List<Map<String, Object>> actions = new ArrayList<>();
        actions.add(buildActionMap(0, yesLabel, source));
        actions.add(buildActionMap(1, noLabel, source));
        entry.put("legal_actions", actions);
        entry.put("chosen_index", chosenIndex);
        entry.put("chosen_description", chosenIndex == 0 ? yesLabel : noLabel);
        logEntry(entry);
    }

    // ═══════════════════════════════════════════════════════════════
    // Card selection from list
    // ═══════════════════════════════════════════════════════════════

    @Override
    public CardCollectionView choosePermanentsToSacrifice(SpellAbility sa, int min, int max,
            CardCollectionView validTargets, String message) {
        CardCollectionView result = super.choosePermanentsToSacrifice(sa, min, max, validTargets, message);
        logCardSelection("CHOOSE_PERMANENTS_SACRIFICE",
                message != null ? message : "Choose permanents to sacrifice",
                validTargets, result);
        return result;
    }

    @Override
    public CardCollectionView chooseCardsToDiscardFrom(Player playerDiscard, SpellAbility sa,
            CardCollection validCards, int min, int max) {
        CardCollectionView result = super.chooseCardsToDiscardFrom(playerDiscard, sa, validCards, min, max);
        logCardSelection("CHOOSE_CARDS_DISCARD", "Choose card(s) to discard",
                validCards, result);
        return result;
    }

    @Override
    public CardCollectionView chooseCardsForEffect(CardCollectionView sourceList, SpellAbility sa,
            String title, int min, int max, boolean isOptional, Map<String, Object> params) {
        CardCollectionView result = super.chooseCardsForEffect(sourceList, sa, title, min, max, isOptional, params);
        logCardSelection("CHOOSE_CARDS_FOR_EFFECT",
                title != null ? title : "Choose cards",
                sourceList, result);
        return result;
    }

    @Override
    public CardCollectionView chooseCardsToRevealFromHand(int min, int max, CardCollectionView valid) {
        CardCollectionView result = super.chooseCardsToRevealFromHand(min, max, valid);
        logCardSelection("CHOOSE_CARDS_REVEAL", "Choose cards to reveal", valid, result);
        return result;
    }

    @Override
    public CardCollectionView chooseCardsToDiscardUnlessType(int num, CardCollectionView hand,
            String param, SpellAbility sa) {
        CardCollectionView result = super.chooseCardsToDiscardUnlessType(num, hand, param, sa);
        logCardSelection("CHOOSE_CARDS_DISCARD_UNLESS",
                "Discard " + num + " card(s) unless you reveal a " + param,
                hand, result);
        return result;
    }

    @Override
    public CardCollection chooseCardsToDiscardToMaximumHandSize(int numDiscard) {
        CardCollection result = super.chooseCardsToDiscardToMaximumHandSize(numDiscard);
        CardCollectionView hand = new CardCollection(player.getCardsIn(ZoneType.Hand));
        logCardSelection("CHOOSE_CARDS_DISCARD_MAX_HAND",
                "Discard to maximum hand size", hand, result);
        return result;
    }

    private void logCardSelection(String type, String prompt,
            CardCollectionView options, CardCollectionView selected) {
        if (options == null || options.isEmpty()) return;

        Map<String, Object> entry = newEntry(type, prompt);
        List<Map<String, Object>> actions = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            Card c = options.get(i);
            actions.add(buildActionMap(i, c.getName(), c));
        }
        entry.put("legal_actions", actions);

        // Map selected cards back to indices
        List<Integer> chosenIndices = new ArrayList<>();
        if (selected != null) {
            for (Card sel : selected) {
                for (int i = 0; i < options.size(); i++) {
                    if (options.get(i) == sel) {
                        chosenIndices.add(i);
                        break;
                    }
                }
            }
        }
        entry.put("chosen_indices", chosenIndices);
        entry.put("chosen_description", chosenIndices.size() + " card(s) selected");
        logEntry(entry);
    }

    // ═══════════════════════════════════════════════════════════════
    // Single entity selection
    // ═══════════════════════════════════════════════════════════════

    @Override
    public <T extends GameEntity> T chooseSingleEntityForEffect(FCollectionView<T> optionList,
            DelayedReveal delayedReveal, SpellAbility sa, String title, boolean isOptional,
            Player relatedPlayer, Map<String, Object> params) {
        T result = super.chooseSingleEntityForEffect(optionList, delayedReveal, sa, title,
                isOptional, relatedPlayer, params);
        logEntitySelection("CHOOSE_ENTITY", title != null ? title : "Choose an entity",
                optionList, result, isOptional);
        return result;
    }

    @Override
    public Card chooseSingleCardForZoneChange(ZoneType destination, List<ZoneType> origin,
            SpellAbility sa, CardCollection fetchList, DelayedReveal delayedReveal,
            String selectPrompt, boolean isOptional, Player decider) {
        Card result = super.chooseSingleCardForZoneChange(destination, origin, sa,
                fetchList, delayedReveal, selectPrompt, isOptional, decider);
        logEntitySelection("CHOOSE_CARD_ZONE_CHANGE",
                selectPrompt != null ? selectPrompt : "Choose a card",
                fetchList, result, isOptional);
        return result;
    }

    private <T extends GameEntity> void logEntitySelection(String type, String prompt,
            FCollectionView<T> options, T selected, boolean isOptional) {
        if (options == null || options.isEmpty()) return;

        Map<String, Object> entry = newEntry(type, prompt);
        List<Map<String, Object>> actions = new ArrayList<>();

        int offset = 0;
        if (isOptional) {
            actions.add(buildActionMap(0, "None", null));
            offset = 1;
        }

        List<T> optionsList = new ArrayList<>(options);
        for (int i = 0; i < optionsList.size(); i++) {
            T entity = optionsList.get(i);
            Card card = (entity instanceof Card) ? (Card) entity : null;
            GameEntity ge = (entity instanceof GameEntity) ? (GameEntity) entity : null;
            actions.add(buildActionMap(i + offset, entity.toString(), card,
                    ge, ge != null && isOwnEntity(ge)));
        }
        entry.put("legal_actions", actions);

        // Map result back to index
        int chosenIndex;
        if (selected == null) {
            chosenIndex = isOptional ? 0 : -1;
            entry.put("chosen_description", "None");
        } else {
            chosenIndex = -1;
            for (int i = 0; i < optionsList.size(); i++) {
                if (optionsList.get(i) == selected) {
                    chosenIndex = i + offset;
                    break;
                }
            }
            entry.put("chosen_description", selected.toString());
        }
        entry.put("chosen_index", chosenIndex);
        logEntry(entry);
    }

    // ═══════════════════════════════════════════════════════════════
    // Color choice
    // ═══════════════════════════════════════════════════════════════

    @Override
    public byte chooseColor(String message, SpellAbility sa, ColorSet colors) {
        byte result = super.chooseColor(message, sa, colors);

        Map<String, Object> entry = newEntry("CHOOSE_COLOR",
                message != null ? message : "Choose a color");
        List<Map<String, Object>> actions = new ArrayList<>();
        List<Byte> availableColors = new ArrayList<>();
        for (byte color : MagicColor.WUBRG) {
            if (colors.hasAnyColor(color)) {
                availableColors.add(color);
            }
        }
        for (int i = 0; i < availableColors.size(); i++) {
            actions.add(buildActionMap(i, MagicColor.toLongString(availableColors.get(i)), null));
        }
        entry.put("legal_actions", actions);

        int chosenIndex = 0;
        for (int i = 0; i < availableColors.size(); i++) {
            if (availableColors.get(i) == result) {
                chosenIndex = i;
                break;
            }
        }
        entry.put("chosen_index", chosenIndex);
        entry.put("chosen_description", MagicColor.toLongString(result));
        logEntry(entry);
        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    // Mode choice
    // ═══════════════════════════════════════════════════════════════

    @Override
    public List<AbilitySub> chooseModeForAbility(SpellAbility sa, List<AbilitySub> possible,
            int min, int num, boolean allowRepeat) {
        List<AbilitySub> result = super.chooseModeForAbility(sa, possible, min, num, allowRepeat);

        Map<String, Object> entry = newEntry("CHOOSE_MODE", "Choose mode for ability");
        List<Map<String, Object>> actions = new ArrayList<>();
        for (int i = 0; i < possible.size(); i++) {
            actions.add(buildActionMap(i, possible.get(i).toString(), null));
        }
        entry.put("legal_actions", actions);

        List<Integer> chosenIndices = new ArrayList<>();
        if (result != null) {
            for (AbilitySub chosen : result) {
                for (int i = 0; i < possible.size(); i++) {
                    if (possible.get(i) == chosen) {
                        chosenIndices.add(i);
                        break;
                    }
                }
            }
        }
        entry.put("chosen_indices", chosenIndices);
        entry.put("chosen_description", chosenIndices.size() + " mode(s) selected");
        logEntry(entry);
        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    // Arrangement (scry/surveil)
    // ═══════════════════════════════════════════════════════════════

    @Override
    public ImmutablePair<CardCollection, CardCollection> arrangeForScry(CardCollection topN) {
        ImmutablePair<CardCollection, CardCollection> result = super.arrangeForScry(topN);
        logArrangement("ARRANGE_SCRY", "Scry", topN, result.getLeft(), result.getRight(),
                "top", "bottom");
        return result;
    }

    @Override
    public ImmutablePair<CardCollection, CardCollection> arrangeForSurveil(CardCollection topN) {
        ImmutablePair<CardCollection, CardCollection> result = super.arrangeForSurveil(topN);
        logArrangement("ARRANGE_SURVEIL", "Surveil", topN, result.getLeft(), result.getRight(),
                "top", "graveyard");
        return result;
    }

    private void logArrangement(String type, String prompt, CardCollection cards,
            CardCollection toFirst, CardCollection toSecond,
            String firstLabel, String secondLabel) {
        if (cards == null || cards.isEmpty()) return;

        Map<String, Object> entry = newEntry(type, prompt);
        List<Map<String, Object>> actions = new ArrayList<>();
        for (int i = 0; i < cards.size(); i++) {
            Card c = cards.get(i);
            String dest = "unknown";
            if (toFirst != null && toFirst.contains(c)) dest = firstLabel;
            else if (toSecond != null && toSecond.contains(c)) dest = secondLabel;
            actions.add(buildActionMap(i, c.getName() + " -> " + dest, c));
        }
        entry.put("legal_actions", actions);

        // Per-card binary decisions
        List<Map<String, Object>> perCard = new ArrayList<>();
        for (Card c : cards) {
            Map<String, Object> pc = new LinkedHashMap<>();
            pc.put("card", c.getName());
            pc.put("card_id", c.getId());
            boolean isFirst = toFirst != null && toFirst.contains(c);
            pc.put("chosen", isFirst ? firstLabel : secondLabel);
            pc.put("chosen_index", isFirst ? 0 : 1);
            perCard.add(pc);
        }
        entry.put("per_card_decisions", perCard);
        logEntry(entry);
    }

    // ═══════════════════════════════════════════════════════════════
    // Ordering
    // ═══════════════════════════════════════════════════════════════

    @Override
    public CardCollectionView orderMoveToZoneList(CardCollectionView cards,
            ZoneType destinationZone, SpellAbility source) {
        CardCollectionView result = super.orderMoveToZoneList(cards, destinationZone, source);
        if (cards != null && cards.size() > 1) {
            logOrdering("ORDER_CARDS", "Order cards to " + destinationZone.name(),
                    cards, result);
        }
        return result;
    }

    @Override
    public CardCollection orderBlockers(Card attacker, CardCollection blockers) {
        CardCollection result = super.orderBlockers(attacker, blockers);
        if (blockers != null && blockers.size() > 1) {
            logOrdering("ORDER_BLOCKERS",
                    "Order blockers for " + attacker.getName(), blockers, result);
        }
        return result;
    }

    @Override
    public CardCollection orderAttackers(Card blocker, CardCollection attackers) {
        CardCollection result = super.orderAttackers(blocker, attackers);
        if (attackers != null && attackers.size() > 1) {
            logOrdering("ORDER_ATTACKERS",
                    "Order attackers for " + blocker.getName(), attackers, result);
        }
        return result;
    }

    @Override
    public CardCollection orderBlocker(Card attacker, Card blocker, CardCollection oldBlockers) {
        CardCollection result = super.orderBlocker(attacker, blocker, oldBlockers);
        Map<String, Object> entry = newEntry("ORDER_BLOCKER",
                "Insert " + blocker.getName() + " into blocker order for " + attacker.getName());
        List<Map<String, Object>> actions = new ArrayList<>();
        if (oldBlockers != null) {
            for (int i = 0; i <= oldBlockers.size(); i++) {
                String desc;
                if (i == 0) {
                    desc = "Before " + oldBlockers.get(0).getName();
                } else if (i == oldBlockers.size()) {
                    desc = "After " + oldBlockers.get(i - 1).getName();
                } else {
                    desc = "Between " + oldBlockers.get(i - 1).getName()
                            + " and " + oldBlockers.get(i).getName();
                }
                actions.add(buildActionMap(i, desc, blocker));
            }
        }
        entry.put("legal_actions", actions);
        // Find where the blocker was inserted
        int chosenIndex = result != null ? result.indexOf(blocker) : -1;
        entry.put("chosen_index", chosenIndex);
        entry.put("chosen_description", "Position " + chosenIndex);
        logEntry(entry);
        return result;
    }

    private void logOrdering(String type, String prompt,
            CardCollectionView options, CardCollectionView result) {
        Map<String, Object> entry = newEntry(type, prompt);
        List<Map<String, Object>> actions = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            Card c = options.get(i);
            actions.add(buildActionMap(i, c.getName(), c));
        }
        entry.put("legal_actions", actions);
        List<String> order = new ArrayList<>();
        if (result != null) {
            for (Card c : result) {
                order.add(c.getName());
            }
        }
        entry.put("chosen_order", order);
        logEntry(entry);
    }

    // ═══════════════════════════════════════════════════════════════
    // Combat damage assignment
    // ═══════════════════════════════════════════════════════════════

    @Override
    public Map<Card, Integer> assignCombatDamage(Card attacker, CardCollectionView blockers,
            CardCollectionView remaining, int damageDealt, GameEntity defender,
            boolean overrideOrder) {
        Map<Card, Integer> result = super.assignCombatDamage(attacker, blockers, remaining,
                damageDealt, defender, overrideOrder);
        if (blockers != null && !blockers.isEmpty()) {
            Map<String, Object> entry = newEntry("ASSIGN_COMBAT_DAMAGE",
                    "Assign " + damageDealt + " damage from " + attacker.getName());
            List<Map<String, Object>> actions = new ArrayList<>();
            for (int i = 0; i < blockers.size(); i++) {
                Card b = blockers.get(i);
                int assigned = result.getOrDefault(b, 0);
                actions.add(buildActionMap(i, b.getName() + " <- " + assigned + " damage", b));
            }
            entry.put("legal_actions", actions);
            // Log the full assignment
            List<Map<String, Object>> assignments = new ArrayList<>();
            for (Map.Entry<Card, Integer> e : result.entrySet()) {
                Map<String, Object> a = new LinkedHashMap<>();
                a.put("card", e.getKey().getName());
                a.put("card_id", e.getKey().getId());
                a.put("damage", e.getValue());
                assignments.add(a);
            }
            entry.put("damage_assignments", assignments);
            logEntry(entry);
        }
        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    // Pile choice
    // ═══════════════════════════════════════════════════════════════

    @Override
    public boolean chooseCardsPile(SpellAbility sa, CardCollectionView pile1,
            CardCollectionView pile2, String faceUp) {
        boolean result = super.chooseCardsPile(sa, pile1, pile2, faceUp);
        Map<String, Object> entry = newEntry("CHOOSE_PILE", "Choose a pile");
        List<Map<String, Object>> actions = new ArrayList<>();
        StringBuilder p1Desc = new StringBuilder("Pile 1 (" + pile1.size() + " cards):");
        for (Card c : pile1) {
            p1Desc.append(" ").append(c.getName());
        }
        StringBuilder p2Desc = new StringBuilder("Pile 2 (" + pile2.size() + " cards):");
        for (Card c : pile2) {
            p2Desc.append(" ").append(c.getName());
        }
        actions.add(buildActionMap(0, p1Desc.toString(), null));
        actions.add(buildActionMap(1, p2Desc.toString(), null));
        entry.put("legal_actions", actions);
        entry.put("chosen_index", result ? 0 : 1);
        entry.put("chosen_description", result ? "Pile 1" : "Pile 2");
        logEntry(entry);
        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    // Mana choice
    // ═══════════════════════════════════════════════════════════════

    @Override
    public Mana chooseManaFromPool(List<Mana> manaChoices) {
        Mana result = super.chooseManaFromPool(manaChoices);
        if (manaChoices.size() > 1) {
            Map<String, Object> entry = newEntry("CHOOSE_MANA", "Choose mana from pool");
            List<Map<String, Object>> actions = new ArrayList<>();
            for (int i = 0; i < manaChoices.size(); i++) {
                actions.add(buildActionMap(i, manaChoices.get(i).toString(), null));
            }
            entry.put("legal_actions", actions);
            int chosenIndex = manaChoices.indexOf(result);
            entry.put("chosen_index", chosenIndex);
            entry.put("chosen_description", result != null ? result.toString() : "None");
            logEntry(entry);
        }
        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    // Mulligan tuck (London mulligan)
    // ═══════════════════════════════════════════════════════════════

    @Override
    public CardCollectionView tuckCardsViaMulligan(Player mulliganingPlayer, int cardsToReturn) {
        CardCollectionView result = super.tuckCardsViaMulligan(mulliganingPlayer, cardsToReturn);
        CardCollectionView hand = new CardCollection(player.getCardsIn(ZoneType.Hand));
        logCardSelection("TUCK_MULLIGAN",
                "Choose " + cardsToReturn + " card(s) to put on bottom of library",
                hand, result);
        return result;
    }
}
