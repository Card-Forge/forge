package forge.research;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.ImmutablePair;

import forge.LobbyPlayer;
import forge.ai.ComputerUtilAbility;
import forge.ai.PlayerControllerAi;
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
import forge.card.mana.ManaCostShard;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.DelayedReveal;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.ai.ComputerUtil;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.ZoneType;
import forge.research.observation.CardRegistry;
import forge.research.observation.ObservationBuilder;
import forge.research.proto.ActionOption;
import forge.research.proto.DecisionPoint;
import forge.research.proto.DecisionType;
import forge.research.proto.Observation;
import forge.util.collect.FCollectionView;

/**
 * PlayerController that bridges Forge game decisions to an RL agent via queues.
 *
 * Extends PlayerControllerAi so all AI-internal casts (AiCostDecision, AiCardMemory, etc.)
 * work naturally. Tier 1 methods are overridden to block on the decision/response queues
 * for RL agent input. Everything else falls through to the AI implementation.
 */
public class RlPlayerController extends PlayerControllerAi {

    private static final long RESPONSE_TIMEOUT_SECONDS = 300;
    private static final int MAX_CONSECUTIVE_PLAY_FAILURES = 3;

    private final SynchronousQueue<DecisionContext> decisionQueue;
    private final SynchronousQueue<ActionResponse> responseQueue;
    protected final ObservationBuilder observationBuilder = new ObservationBuilder();
    private final CardRegistry cardRegistry = CardRegistry.getInstance();
    protected final int playerIndex;
    private int consecutivePlayFailures = 0;

    public RlPlayerController(Game game, Player p, LobbyPlayer lp,
            SynchronousQueue<DecisionContext> decisionQueue,
            SynchronousQueue<ActionResponse> responseQueue,
            int playerIndex) {
        super(game, p, lp);
        this.decisionQueue = decisionQueue;
        this.responseQueue = responseQueue;
        this.playerIndex = playerIndex;
    }

    /**
     * Protected constructor for subclasses that don't use queues (e.g., ONNX inference).
     */
    protected RlPlayerController(Game game, Player p, LobbyPlayer lp, int playerIndex) {
        super(game, p, lp);
        this.decisionQueue = null;
        this.responseQueue = null;
        this.playerIndex = playerIndex;
    }

    /**
     * Core method: send a decision to the RL agent and wait for the response.
     * Returns the chosen action index, or 0 if timeout/interrupted.
     * Subclasses can override to provide alternative inference (e.g., ONNX).
     */
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
            DecisionContext ctx = new DecisionContext(obs, decisionPoint);
            decisionQueue.put(ctx);

            ActionResponse response = responseQueue.poll(RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (response == null) {
                return 0; // timeout fallback: pick first option
            }
            return response.getActionIndex();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        }
    }

    private DecisionPoint.Builder baseDecision(DecisionType type, String prompt) {
        return DecisionPoint.newBuilder()
                .setType(type)
                .setPrompt(prompt != null ? prompt : "")
                .setPlayerIndex(playerIndex);
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

    private ActionOption buildAction(int index, String description, Card source) {
        return buildAction(index, description, source, null, false);
    }

    private ActionOption buildAction(int index, String description, Card source,
            GameEntity target, boolean targetIsOwn) {
        ActionOption.Builder opt = ActionOption.newBuilder()
                .setIndex(index)
                .setDescription(description != null ? description : "");
        if (source != null) {
            opt.setSourceCardId(source.getId());
            opt.setSourceCardName(source.getName());
            opt.setSourceNameId(cardRegistry.getNameId(source.getName()));
        }
        if (target != null) {
            if (target instanceof Card) {
                Card tc = (Card) target;
                opt.setTargetCardId(tc.getId());
                opt.setTargetNameId(cardRegistry.getNameId(tc.getName()));
            }
            opt.setTargetIsOwn(targetIsOwn);
            opt.setTargetIsPlayer(target instanceof Player);
        }
        return opt.build();
    }

    // ═══════════════════════════════════════════════════════════════
    // TIER 1 — RL decisions (block on queue)
    // ═══════════════════════════════════════════════════════════════

    @Override
    public List<SpellAbility> chooseSpellAbilityToPlay() {
        // If too many consecutive play failures, force pass to avoid infinite loops
        if (consecutivePlayFailures >= MAX_CONSECUTIVE_PLAY_FAILURES) {
            consecutivePlayFailures = 0;
            return null;
        }

        CardCollectionView cards = ComputerUtilAbility.getAvailableCards(getGame(), player);
        List<SpellAbility> playable = ComputerUtilAbility.getSpellAbilities(cards, player);
        // Filter out mana abilities — they are handled automatically during mana payment
        // and offering them as strategic choices causes infinite loops
        playable.removeIf(sa -> sa.isManaAbility());
        if (playable.isEmpty()) {
            return null;
        }

        DecisionPoint.Builder dp = baseDecision(DecisionType.CHOOSE_SPELL_ABILITY,
                "Choose spell or ability to play (or pass)");
        dp.setMinSelections(0);
        dp.setMaxSelections(1);

        // Action 0 = pass priority
        dp.addLegalActions(buildAction(0, "Pass priority", null));

        for (int i = 0; i < playable.size(); i++) {
            SpellAbility sa = playable.get(i);
            Card host = sa.getHostCard();
            dp.addLegalActions(buildAction(i + 1, sa.toString(), host));
        }

        int choice = queryAgent(dp.build());

        if (choice <= 0 || choice > playable.size()) {
            consecutivePlayFailures = 0;
            return null; // pass
        }

        SpellAbility chosen = playable.get(choice - 1);
        return Collections.singletonList(chosen);
    }

    @Override
    public boolean playChosenSpellAbility(SpellAbility sa) {
        if (sa.isLandAbility()) {
            if (sa.canPlay()) {
                sa.resolve();
            }
            consecutivePlayFailures = 0;
            return true;
        }

        // Set targets via RL agent before handing off to the game engine
        if (!rlChooseTargets(sa)) {
            // Agent couldn't or chose not to target — fizzle
            consecutivePlayFailures++;
            return false;
        }

        ComputerUtil.handlePlayingSpellAbility(player, sa, null);
        consecutivePlayFailures = 0;
        return true;
    }

    /**
     * Walk the SA chain and let the RL agent pick targets for each ability
     * that uses targeting. Returns false if mandatory targeting fails.
     */
    private boolean rlChooseTargets(SpellAbility sa) {
        SpellAbility cur = sa;
        while (cur != null) {
            if (cur.usesTargeting()) {
                TargetRestrictions tr = cur.getTargetRestrictions();
                List<GameEntity> candidates = tr.getAllCandidates(cur, true);

                if (candidates.isEmpty()) {
                    // No valid targets — fail if targets are mandatory
                    return cur.getMinTargets() == 0;
                }

                int minTgts = cur.getMinTargets();
                int maxTgts = cur.getMaxTargets();

                // For simplicity, select targets one at a time up to max
                for (int t = 0; t < maxTgts; t++) {
                    // Recompute candidates (previous selections may exclude some)
                    candidates = tr.getAllCandidates(cur, true);
                    if (candidates.isEmpty()) {
                        break;
                    }

                    boolean optional = t >= minTgts;

                    DecisionPoint.Builder dp = baseDecision(DecisionType.CHOOSE_ENTITY,
                            "Choose target for " + cur.getHostCard().getName()
                            + (maxTgts > 1 ? " (" + (t + 1) + "/" + maxTgts + ")" : ""));
                    dp.setMinSelections(optional ? 0 : 1);
                    dp.setMaxSelections(1);

                    int offset = 0;
                    if (optional) {
                        dp.addLegalActions(buildAction(0, "Done targeting", null));
                        offset = 1;
                    }

                    for (int i = 0; i < candidates.size(); i++) {
                        GameEntity entity = candidates.get(i);
                        Card card = (entity instanceof Card) ? (Card) entity : null;
                        String desc = entity.toString();
                        if (entity instanceof Player) {
                            desc = "Player: " + ((Player) entity).getName();
                        }
                        boolean isOwn = isOwnEntity(entity);
                        dp.addLegalActions(buildAction(i + offset, desc, card, entity, isOwn));
                    }

                    int choice = queryAgent(dp.build());

                    if (optional && choice == 0) {
                        break; // agent says done
                    }

                    int idx = choice - offset;
                    if (idx < 0 || idx >= candidates.size()) {
                        if (optional) break;
                        idx = 0; // fallback to first valid target
                    }

                    cur.getTargets().add(candidates.get(idx));
                }

                // Check we met minimum
                if (!cur.isMinTargetChosen()) {
                    return false;
                }
            }
            cur = cur.getSubAbility();
        }
        return true;
    }

    @Override
    public boolean mulliganKeepHand(Player mulliganingPlayer, int cardsToReturn) {
        DecisionPoint.Builder dp = baseDecision(DecisionType.MULLIGAN,
                "Keep hand or mulligan? (" + cardsToReturn + " cards to return)");
        dp.setMinSelections(1);
        dp.setMaxSelections(1);
        dp.addLegalActions(buildAction(0, "Keep", null));
        dp.addLegalActions(buildAction(1, "Mulligan", null));

        int choice = queryAgent(dp.build());
        return choice == 0; // 0 = keep
    }

    @Override
    public void declareAttackers(Player attacker, Combat combat) {
        GameEntity defender = combat.getDefenders().isEmpty() ? null : combat.getDefenders().iterator().next();
        if (defender == null) {
            return;
        }

        // Iteratively select attackers one at a time
        List<Card> remaining = new ArrayList<>();
        for (Card c : attacker.getCreaturesInPlay()) {
            if (CombatUtil.canAttack(c, defender)) {
                remaining.add(c);
            }
        }
        if (remaining.isEmpty()) {
            return;
        }

        while (!remaining.isEmpty()) {
            DecisionPoint.Builder dp = baseDecision(DecisionType.DECLARE_ATTACKERS,
                    "Declare attackers");
            dp.setMinSelections(0);
            dp.setMaxSelections(1);

            dp.addLegalActions(buildAction(0, "Done declaring attackers", null));

            for (int i = 0; i < remaining.size(); i++) {
                Card c = remaining.get(i);
                dp.addLegalActions(buildAction(i + 1,
                        "Attack with " + c.getName() + " (" + c.getNetPower() + "/" + c.getNetToughness() + ")", c));
            }

            int choice = queryAgent(dp.build());

            if (choice <= 0 || choice > remaining.size()) {
                break; // done declaring
            }

            Card attackerCard = remaining.remove(choice - 1);
            combat.addAttacker(attackerCard, defender);
        }
    }

    @Override
    public void declareBlockers(Player defender, Combat combat) {
        List<Card> attackers = combat.getAttackers();
        if (attackers.isEmpty()) {
            return;
        }

        // Build list of eligible blockers
        List<Card> remainingBlockers = new ArrayList<>();
        for (Card c : defender.getCreaturesInPlay()) {
            if (CombatUtil.canBlock(c, combat)) {
                remainingBlockers.add(c);
            }
        }
        if (remainingBlockers.isEmpty()) {
            return;
        }

        // Iteratively assign blockers: pick a blocker, then pick which attacker it blocks
        while (!remainingBlockers.isEmpty()) {
            // Step 1: Pick a blocker (or done)
            DecisionPoint.Builder dpBlocker = baseDecision(DecisionType.DECLARE_BLOCKERS,
                    "Choose creature to block with");
            dpBlocker.setMinSelections(0);
            dpBlocker.setMaxSelections(1);

            dpBlocker.addLegalActions(buildAction(0, "Done declaring blockers", null));

            for (int i = 0; i < remainingBlockers.size(); i++) {
                Card blocker = remainingBlockers.get(i);
                dpBlocker.addLegalActions(buildAction(i + 1,
                        "Block with " + blocker.getName() + " (" + blocker.getNetPower() + "/" + blocker.getNetToughness() + ")", blocker));
            }

            int blockerChoice = queryAgent(dpBlocker.build());
            if (blockerChoice <= 0 || blockerChoice > remainingBlockers.size()) {
                break; // done declaring blockers
            }

            Card chosenBlocker = remainingBlockers.get(blockerChoice - 1);

            // Step 2: Pick which attacker to block
            // Filter to attackers this blocker can legally block
            List<Card> blockableAttackers = new ArrayList<>();
            for (Card atk : attackers) {
                if (CombatUtil.canBlock(chosenBlocker, atk, combat)) {
                    blockableAttackers.add(atk);
                }
            }

            if (blockableAttackers.isEmpty()) {
                remainingBlockers.remove(blockerChoice - 1);
                continue;
            }

            if (blockableAttackers.size() == 1) {
                // Only one valid attacker to block — auto-assign
                combat.addBlocker(blockableAttackers.get(0), chosenBlocker);
                remainingBlockers.remove(blockerChoice - 1);
                continue;
            }

            DecisionPoint.Builder dpAttacker = baseDecision(DecisionType.DECLARE_BLOCKERS,
                    "Choose attacker for " + chosenBlocker.getName() + " to block");
            dpAttacker.setMinSelections(1);
            dpAttacker.setMaxSelections(1);

            for (int i = 0; i < blockableAttackers.size(); i++) {
                Card atk = blockableAttackers.get(i);
                dpAttacker.addLegalActions(buildAction(i,
                        "Block " + atk.getName() + " (" + atk.getNetPower() + "/" + atk.getNetToughness() + ")", atk));
            }

            int atkChoice = queryAgent(dpAttacker.build());
            atkChoice = Math.max(0, Math.min(atkChoice, blockableAttackers.size() - 1));

            combat.addBlocker(blockableAttackers.get(atkChoice), chosenBlocker);
            remainingBlockers.remove(blockerChoice - 1);
        }
    }

    @Override
    public CardCollectionView choosePermanentsToSacrifice(SpellAbility sa, int min, int max,
            CardCollectionView validTargets, String message) {
        if (validTargets.isEmpty()) {
            return new CardCollection();
        }
        if (validTargets.size() <= min) {
            return validTargets;
        }

        return queryAgentMultiSelect(new ArrayList<>(validTargets),
                message != null ? message : "Choose permanents to sacrifice",
                DecisionType.CHOOSE_CARDS, min, max);
    }

    @Override
    public CardCollectionView choosePermanentsToDestroy(SpellAbility sa, int min, int max,
            CardCollectionView validTargets, String message) {
        return choosePermanentsToSacrifice(sa, min, max, validTargets, message);
    }

    @Override
    public CardCollection chooseCardsToDiscardFrom(Player playerDiscard, SpellAbility sa,
            CardCollection validCards, int min, int max) {
        if (validCards.isEmpty()) {
            return new CardCollection();
        }
        if (validCards.size() <= min) {
            return validCards;
        }

        return queryAgentMultiSelect(new ArrayList<>(validCards),
                "Choose card(s) to discard",
                DecisionType.CHOOSE_CARDS, min, max);
    }

    @Override
    public boolean confirmAction(SpellAbility sa, PlayerActionConfirmMode mode, String message,
            List<String> options, Card cardToShow, Map<String, Object> params) {
        DecisionPoint.Builder dp = baseDecision(DecisionType.CONFIRM_ACTION,
                message != null ? message : "Confirm action?");
        dp.setMinSelections(1);
        dp.setMaxSelections(1);
        dp.addLegalActions(buildAction(0, "Yes", cardToShow));
        dp.addLegalActions(buildAction(1, "No", cardToShow));

        int choice = queryAgent(dp.build());
        return choice == 0;
    }

    // ═══════════════════════════════════════════════════════════════
    // Helper — iterative multi-select via repeated queryAgent calls
    // ═══════════════════════════════════════════════════════════════

    /**
     * Iteratively select cards by querying the agent once per selection.
     * Presents remaining options each round, with a "Done" action once min is met.
     */
    private CardCollection queryAgentMultiSelect(List<Card> options, String prompt,
            DecisionType type, int min, int max) {
        CardCollection selected = new CardCollection();
        List<Card> remaining = new ArrayList<>(options);

        while (selected.size() < max && !remaining.isEmpty()) {
            DecisionPoint.Builder dp = baseDecision(type, prompt
                    + " (" + selected.size() + "/" + max + " selected)");
            dp.setMinSelections(1);
            dp.setMaxSelections(1);

            int offset = 0;
            if (selected.size() >= min) {
                dp.addLegalActions(buildAction(0, "Done selecting", null));
                offset = 1;
            }

            for (int i = 0; i < remaining.size(); i++) {
                Card c = remaining.get(i);
                dp.addLegalActions(buildAction(i + offset, c.getName(), c));
            }

            int choice = queryAgent(dp.build());

            if (selected.size() >= min && choice == 0) {
                break; // agent chose "Done"
            }

            int cardIdx = choice - offset;
            if (cardIdx < 0 || cardIdx >= remaining.size()) {
                if (selected.size() >= min) break;
                cardIdx = 0; // fallback: pick first
            }

            selected.add(remaining.remove(cardIdx));
        }
        return selected;
    }

    // ═══════════════════════════════════════════════════════════════
    // Group 1 — Binary decisions
    // ═══════════════════════════════════════════════════════════════

    @Override
    public boolean confirmTrigger(WrappedAbility sa) {
        if (sa.isMandatory()) {
            return true;
        }
        DecisionPoint.Builder dp = baseDecision(DecisionType.CONFIRM_ACTION,
                "Confirm trigger: " + sa.toString());
        dp.setMinSelections(1);
        dp.setMaxSelections(1);
        dp.addLegalActions(buildAction(0, "Yes", sa.getHostCard()));
        dp.addLegalActions(buildAction(1, "No", sa.getHostCard()));

        return queryAgent(dp.build()) == 0;
    }

    @Override
    public boolean playTrigger(Card host, WrappedAbility wrapperAbility, boolean isMandatory) {
        if (isMandatory) {
            return super.playTrigger(host, wrapperAbility, true);
        }
        // Optional trigger: ask agent whether to play it
        DecisionPoint.Builder dp = baseDecision(DecisionType.CONFIRM_ACTION,
                "Play optional trigger: " + wrapperAbility.toString());
        dp.setMinSelections(1);
        dp.setMaxSelections(1);
        dp.addLegalActions(buildAction(0, "Yes", host));
        dp.addLegalActions(buildAction(1, "No", host));

        if (queryAgent(dp.build()) == 0) {
            // Pass isMandatory=true so the AI executes it without second-guessing
            // the RL agent's strategic decision
            return super.playTrigger(host, wrapperAbility, true);
        }
        return false;
    }

    @Override
    public boolean confirmReplacementEffect(ReplacementEffect replacementEffect,
            SpellAbility effectSA, GameEntity affected, String question) {
        DecisionPoint.Builder dp = baseDecision(DecisionType.CONFIRM_ACTION,
                question != null ? question : "Apply replacement effect?");
        dp.setMinSelections(1);
        dp.setMaxSelections(1);
        Card host = replacementEffect.getHostCard();
        dp.addLegalActions(buildAction(0, "Yes", host));
        dp.addLegalActions(buildAction(1, "No", host));

        return queryAgent(dp.build()) == 0;
    }

    @Override
    public boolean chooseCardsPile(SpellAbility sa, CardCollectionView pile1,
            CardCollectionView pile2, String faceUp) {
        DecisionPoint.Builder dp = baseDecision(DecisionType.CHOOSE_PILE, "Choose a pile");
        dp.setMinSelections(1);
        dp.setMaxSelections(1);

        StringBuilder p1Desc = new StringBuilder("Pile 1 (" + pile1.size() + " cards):");
        for (Card c : pile1) p1Desc.append(" ").append(c.getName());
        StringBuilder p2Desc = new StringBuilder("Pile 2 (" + pile2.size() + " cards):");
        for (Card c : pile2) p2Desc.append(" ").append(c.getName());

        dp.addLegalActions(buildAction(0, p1Desc.toString(), null));
        dp.addLegalActions(buildAction(1, p2Desc.toString(), null));

        return queryAgent(dp.build()) == 0; // 0 = pile 1 = true
    }

    @Override
    public Mana chooseManaFromPool(List<Mana> manaChoices) {
        if (manaChoices.size() <= 1) {
            return manaChoices.get(0);
        }

        DecisionPoint.Builder dp = baseDecision(DecisionType.CHOOSE_COLOR,
                "Choose mana from pool");
        dp.setMinSelections(1);
        dp.setMaxSelections(1);

        for (int i = 0; i < manaChoices.size(); i++) {
            dp.addLegalActions(buildAction(i, manaChoices.get(i).toString(), null));
        }

        int choice = queryAgent(dp.build());
        choice = Math.max(0, Math.min(choice, manaChoices.size() - 1));
        return manaChoices.get(choice);
    }

    @Override
    public SpellAbility chooseManaAbilityForPayment(ManaCostBeingPaid cost, SpellAbility sa,
            ManaCostShard toPay, java.util.Collection<SpellAbility> manaAbilities, boolean checkCosts) {
        List<SpellAbility> options = new ArrayList<>(manaAbilities);
        if (options.isEmpty()) {
            return null;
        }
        if (options.size() == 1) {
            return options.get(0);
        }

        DecisionPoint.Builder dp = baseDecision(DecisionType.CHOOSE_CARDS,
                "Choose mana source to tap for " + toPay);
        dp.setMinSelections(1);
        dp.setMaxSelections(1);

        for (int i = 0; i < options.size(); i++) {
            Card host = options.get(i).getHostCard();
            String desc = host.getName();
            if (host.isTapped()) {
                desc += " (tapped)";
            }
            dp.addLegalActions(buildAction(i, desc, host));
        }

        int choice = queryAgent(dp.build());
        choice = Math.max(0, Math.min(choice, options.size() - 1));
        return options.get(choice);
    }

    // ═══════════════════════════════════════════════════════════════
    // Group 2 — Single selection from list
    // ═══════════════════════════════════════════════════════════════

    @Override
    public <T extends GameEntity> T chooseSingleEntityForEffect(FCollectionView<T> optionList,
            DelayedReveal delayedReveal, SpellAbility sa, String title, boolean isOptional,
            Player relatedPlayer, Map<String, Object> params) {
        if (delayedReveal != null) {
            reveal(delayedReveal);
        }
        if (optionList.isEmpty()) {
            return null;
        }

        List<T> options = new ArrayList<>(optionList);

        DecisionPoint.Builder dp = baseDecision(DecisionType.CHOOSE_ENTITY,
                title != null ? title : "Choose an entity");
        dp.setMinSelections(isOptional ? 0 : 1);
        dp.setMaxSelections(1);

        int offset = 0;
        if (isOptional) {
            dp.addLegalActions(buildAction(0, "None", null));
            offset = 1;
        }

        for (int i = 0; i < options.size(); i++) {
            T entity = options.get(i);
            Card card = (entity instanceof Card) ? (Card) entity : null;
            GameEntity ge = (entity instanceof GameEntity) ? (GameEntity) entity : null;
            dp.addLegalActions(buildAction(i + offset, entity.toString(), card,
                    ge, ge != null && isOwnEntity(ge)));
        }

        int choice = queryAgent(dp.build());

        if (isOptional && choice == 0) {
            return null;
        }

        int idx = choice - offset;
        if (idx < 0 || idx >= options.size()) {
            return isOptional ? null : options.get(0);
        }
        return options.get(idx);
    }

    @Override
    public byte chooseColor(String message, SpellAbility sa, ColorSet colors) {
        DecisionPoint.Builder dp = baseDecision(DecisionType.CHOOSE_COLOR,
                message != null ? message : "Choose a color");
        dp.setMinSelections(1);
        dp.setMaxSelections(1);

        List<Byte> availableColors = new ArrayList<>();
        for (byte color : MagicColor.WUBRG) {
            if (colors.hasAnyColor(color)) {
                availableColors.add(color);
            }
        }

        if (availableColors.isEmpty()) {
            return MagicColor.WHITE; // fallback
        }

        for (int i = 0; i < availableColors.size(); i++) {
            dp.addLegalActions(buildAction(i,
                    MagicColor.toLongString(availableColors.get(i)), null));
        }

        int choice = queryAgent(dp.build());
        choice = Math.max(0, Math.min(choice, availableColors.size() - 1));
        return availableColors.get(choice);
    }

    @Override
    public Card chooseSingleCardForZoneChange(ZoneType destination, List<ZoneType> origin,
            SpellAbility sa, CardCollection fetchList, DelayedReveal delayedReveal,
            String selectPrompt, boolean isOptional, Player decider) {
        if (delayedReveal != null) {
            reveal(delayedReveal);
        }
        if (fetchList.isEmpty()) {
            return null;
        }

        DecisionPoint.Builder dp = baseDecision(DecisionType.CHOOSE_CARDS,
                selectPrompt != null ? selectPrompt : "Choose a card");
        dp.setMinSelections(isOptional ? 0 : 1);
        dp.setMaxSelections(1);

        int offset = 0;
        if (isOptional) {
            dp.addLegalActions(buildAction(0, "None", null));
            offset = 1;
        }

        for (int i = 0; i < fetchList.size(); i++) {
            Card c = fetchList.get(i);
            dp.addLegalActions(buildAction(i + offset, c.getName(), c));
        }

        int choice = queryAgent(dp.build());

        if (isOptional && choice == 0) {
            return null;
        }

        int idx = choice - offset;
        if (idx < 0 || idx >= fetchList.size()) {
            return isOptional ? null : fetchList.get(0);
        }
        return fetchList.get(idx);
    }

    // ═══════════════════════════════════════════════════════════════
    // Group 3 — Multi-selection via iterative queryAgent loop
    // ═══════════════════════════════════════════════════════════════

    @Override
    public CardCollectionView chooseCardsForEffect(CardCollectionView sourceList, SpellAbility sa,
            String title, int min, int max, boolean isOptional, Map<String, Object> params) {
        if (sourceList.isEmpty()) {
            return new CardCollection();
        }
        if (sourceList.size() <= min) {
            return sourceList;
        }
        int effectiveMin = isOptional ? 0 : min;
        return queryAgentMultiSelect(new ArrayList<>(sourceList), title,
                DecisionType.CHOOSE_CARDS, effectiveMin, max);
    }

    @Override
    public <T extends GameEntity> List<T> chooseEntitiesForEffect(FCollectionView<T> optionList,
            int min, int max, DelayedReveal delayedReveal, SpellAbility sa, String title,
            Player relatedPlayer, Map<String, Object> params) {
        if (delayedReveal != null) {
            reveal(delayedReveal);
        }
        List<T> remaining = new ArrayList<>(optionList);
        List<T> selected = new ArrayList<>();

        while (selected.size() < max && !remaining.isEmpty()) {
            // Build a temporary FCollectionView for chooseSingleEntityForEffect
            forge.util.collect.FCollection<T> remainingColl = new forge.util.collect.FCollection<>(remaining);
            T choice = chooseSingleEntityForEffect(remainingColl, null, sa, title,
                    selected.size() >= min, relatedPlayer, params);
            if (choice == null) {
                break;
            }
            remaining.remove(choice);
            selected.add(choice);
        }
        return selected;
    }

    @Override
    public List<AbilitySub> chooseModeForAbility(SpellAbility sa, List<AbilitySub> possible,
            int min, int num, boolean allowRepeat) {
        List<AbilitySub> selected = new ArrayList<>();
        List<AbilitySub> remaining = new ArrayList<>(possible);

        while (selected.size() < num && !remaining.isEmpty()) {
            DecisionPoint.Builder dp = baseDecision(DecisionType.CHOOSE_MODE,
                    "Choose mode (" + selected.size() + "/" + num + " selected)");
            dp.setMinSelections(1);
            dp.setMaxSelections(1);

            int offset = 0;
            if (selected.size() >= min) {
                dp.addLegalActions(buildAction(0, "Done selecting modes", null));
                offset = 1;
            }

            for (int i = 0; i < remaining.size(); i++) {
                dp.addLegalActions(buildAction(i + offset, remaining.get(i).toString(), null));
            }

            int choice = queryAgent(dp.build());

            if (selected.size() >= min && choice == 0) {
                break;
            }

            int idx = choice - offset;
            if (idx < 0 || idx >= remaining.size()) {
                if (selected.size() >= min) break;
                idx = 0;
            }

            AbilitySub chosen = remaining.get(idx);
            selected.add(chosen);
            if (!allowRepeat) {
                remaining.remove(idx);
            }
        }
        return selected;
    }

    @Override
    public CardCollection chooseCardsToDiscardToMaximumHandSize(int numDiscard) {
        CardCollection hand = new CardCollection(player.getCardsIn(ZoneType.Hand));
        return queryAgentMultiSelect(new ArrayList<>(hand), "Discard to maximum hand size",
                DecisionType.CHOOSE_CARDS, numDiscard, numDiscard);
    }

    @Override
    public CardCollectionView tuckCardsViaMulligan(Player mulliganingPlayer, int cardsToReturn) {
        CardCollection hand = new CardCollection(player.getCardsIn(ZoneType.Hand));
        return queryAgentMultiSelect(new ArrayList<>(hand),
                "Choose cards to put on bottom of library",
                DecisionType.CHOOSE_CARDS, cardsToReturn, cardsToReturn);
    }

    // ═══════════════════════════════════════════════════════════════
    // Group 4 — Arrangement (binary per card)
    // ═══════════════════════════════════════════════════════════════

    @Override
    public ImmutablePair<CardCollection, CardCollection> arrangeForScry(CardCollection topN) {
        CardCollection toTop = new CardCollection();
        CardCollection toBottom = new CardCollection();

        for (Card c : topN) {
            DecisionPoint.Builder dp = baseDecision(DecisionType.ORDER_CARDS,
                    "Scry: where to put " + c.getName() + "?");
            dp.setMinSelections(1);
            dp.setMaxSelections(1);
            dp.addLegalActions(buildAction(0, "Put on top", c));
            dp.addLegalActions(buildAction(1, "Put on bottom", c));

            if (queryAgent(dp.build()) == 0) {
                toTop.add(c);
            } else {
                toBottom.add(c);
            }
        }
        return ImmutablePair.of(toTop, toBottom);
    }

    @Override
    public ImmutablePair<CardCollection, CardCollection> arrangeForSurveil(CardCollection topN) {
        CardCollection toTop = new CardCollection();
        CardCollection toGraveyard = new CardCollection();

        for (Card c : topN) {
            DecisionPoint.Builder dp = baseDecision(DecisionType.ORDER_CARDS,
                    "Surveil: where to put " + c.getName() + "?");
            dp.setMinSelections(1);
            dp.setMaxSelections(1);
            dp.addLegalActions(buildAction(0, "Put on top", c));
            dp.addLegalActions(buildAction(1, "Put to graveyard", c));

            if (queryAgent(dp.build()) == 0) {
                toTop.add(c);
            } else {
                toGraveyard.add(c);
            }
        }
        return ImmutablePair.of(toTop, toGraveyard);
    }

    // ═══════════════════════════════════════════════════════════════
    // Combat damage assignment
    // ═══════════════════════════════════════════════════════════════

    @Override
    public Map<Card, Integer> assignCombatDamage(Card attacker, CardCollectionView blockers,
            CardCollectionView remaining, int damageDealt, GameEntity defender,
            boolean overrideOrder) {
        Map<Card, Integer> assignment = new HashMap<>();
        int damageLeft = damageDealt;

        boolean hasDeathtouch = attacker.hasKeyword(forge.game.keyword.Keyword.DEATHTOUCH);
        boolean hasTrample = attacker.hasKeyword(forge.game.keyword.Keyword.TRAMPLE);

        // Assign damage to each blocker in order
        for (int i = 0; i < blockers.size(); i++) {
            Card blocker = blockers.get(i);
            if (damageLeft <= 0) {
                assignment.put(blocker, 0);
                continue;
            }

            int lethal = hasDeathtouch ? 1 : blocker.getLethalDamage();
            lethal = Math.max(lethal, 0);

            // Last blocker and no trample: must assign all remaining
            boolean isLast = (i == blockers.size() - 1) && !hasTrample;
            if (isLast) {
                assignment.put(blocker, damageLeft);
                damageLeft = 0;
                continue;
            }

            // If only one valid assignment (must assign lethal, nothing extra possible)
            if (damageLeft <= lethal) {
                assignment.put(blocker, damageLeft);
                damageLeft = 0;
                continue;
            }

            // Ask agent how much damage to assign to this blocker
            int maxAssignable = damageLeft;
            int minAssignable = Math.min(lethal, damageLeft);

            DecisionPoint.Builder dp = baseDecision(DecisionType.CHOOSE_CARDS,
                    "Assign damage to " + blocker.getName()
                            + " (lethal=" + lethal + ", " + damageLeft + " remaining)");
            dp.setMinSelections(1);
            dp.setMaxSelections(1);

            // Offer damage amounts from min to max
            for (int dmg = minAssignable; dmg <= maxAssignable; dmg++) {
                dp.addLegalActions(buildAction(dmg - minAssignable,
                        dmg + " damage to " + blocker.getName(), blocker));
            }

            int choice = queryAgent(dp.build());
            int assigned = minAssignable + Math.max(0, Math.min(choice, maxAssignable - minAssignable));

            assignment.put(blocker, assigned);
            damageLeft -= assigned;
        }

        // Remaining damage goes to defender (trample) or is lost
        if (damageLeft > 0 && hasTrample && defender instanceof Card) {
            assignment.put((Card) defender, damageLeft);
        }

        return assignment;
    }

    // ═══════════════════════════════════════════════════════════════
    // Group 5 — Ordering (iterative pick-next)
    // ═══════════════════════════════════════════════════════════════

    @Override
    public CardCollection orderBlockers(Card attacker, CardCollection blockers) {
        if (blockers == null || blockers.size() <= 1) {
            return blockers;
        }

        CardCollection ordered = new CardCollection();
        List<Card> remaining = new ArrayList<>(blockers);

        while (!remaining.isEmpty()) {
            DecisionPoint.Builder dp = baseDecision(DecisionType.ORDER_CARDS,
                    "Order blockers: which receives damage next for "
                            + attacker.getName() + "?");
            dp.setMinSelections(1);
            dp.setMaxSelections(1);

            for (int i = 0; i < remaining.size(); i++) {
                Card c = remaining.get(i);
                dp.addLegalActions(buildAction(i, c.getName(), c));
            }

            int choice = queryAgent(dp.build());
            choice = Math.max(0, Math.min(choice, remaining.size() - 1));
            ordered.add(remaining.remove(choice));
        }
        return ordered;
    }

    @Override
    public CardCollection orderAttackers(Card blocker, CardCollection attackers) {
        if (attackers == null || attackers.size() <= 1) {
            return attackers;
        }

        CardCollection ordered = new CardCollection();
        List<Card> remaining = new ArrayList<>(attackers);

        while (!remaining.isEmpty()) {
            DecisionPoint.Builder dp = baseDecision(DecisionType.ORDER_CARDS,
                    "Order attackers: which receives damage next from "
                            + blocker.getName() + "?");
            dp.setMinSelections(1);
            dp.setMaxSelections(1);

            for (int i = 0; i < remaining.size(); i++) {
                Card c = remaining.get(i);
                dp.addLegalActions(buildAction(i, c.getName(), c));
            }

            int choice = queryAgent(dp.build());
            choice = Math.max(0, Math.min(choice, remaining.size() - 1));
            ordered.add(remaining.remove(choice));
        }
        return ordered;
    }

    @Override
    public CardCollection orderBlocker(Card attacker, Card blocker, CardCollection oldBlockers) {
        if (oldBlockers == null || oldBlockers.isEmpty()) {
            CardCollection result = new CardCollection();
            result.add(blocker);
            return result;
        }

        DecisionPoint.Builder dp = baseDecision(DecisionType.ORDER_CARDS,
                "Insert " + blocker.getName() + " into blocker order for "
                        + attacker.getName());
        dp.setMinSelections(1);
        dp.setMaxSelections(1);

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
            dp.addLegalActions(buildAction(i, desc, blocker));
        }

        int choice = queryAgent(dp.build());
        choice = Math.max(0, Math.min(choice, oldBlockers.size()));

        CardCollection result = new CardCollection(oldBlockers);
        result.add(choice, blocker);
        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    // Group 6 — Card-specific decision overrides
    // ═══════════════════════════════════════════════════════════════

    /**
     * Whether to play a spell from a play effect (e.g., Wrenn's Resolve, Reckless Impulse exile-and-play).
     * Returns true if the agent chooses to play it.
     */
    @Override
    public boolean playSaFromPlayEffect(SpellAbility tgtSA) {
        boolean optional = !tgtSA.getPayCosts().isMandatory();

        if (!optional) {
            // Mandatory — must play. Delegate to AI to handle the mechanics.
            return super.playSaFromPlayEffect(tgtSA);
        }

        DecisionPoint.Builder dp = baseDecision(DecisionType.CONFIRM_ACTION,
                "Play " + tgtSA.getHostCard().getName() + " from exile?");
        dp.setMinSelections(1);
        dp.setMaxSelections(1);
        dp.addLegalActions(buildAction(0, "Don't play", tgtSA.getHostCard()));
        dp.addLegalActions(buildAction(1, "Play " + tgtSA.getHostCard().getName(), tgtSA.getHostCard()));

        int choice = queryAgent(dp.build());

        if (choice == 1) {
            return ComputerUtil.playStack(tgtSA, player, getGame());
        }
        return false;
    }

    /**
     * Order cards being moved to a zone (e.g., Brainstorm putting cards back on top of library).
     * Uses iterative pick-next ordering.
     */
    @Override
    public CardCollectionView orderMoveToZoneList(CardCollectionView cards, ZoneType destinationZone, SpellAbility source) {
        if (cards.isEmpty() || cards.size() <= 1) {
            return cards;
        }

        CardCollection ordered = new CardCollection();
        List<Card> remaining = new ArrayList<>(cards);

        while (!remaining.isEmpty()) {
            if (remaining.size() == 1) {
                ordered.add(remaining.remove(0));
                break;
            }

            DecisionPoint.Builder dp = baseDecision(DecisionType.ORDER_CARDS,
                    "Order card to " + destinationZone.name()
                            + " (" + (ordered.size() + 1) + "/" + cards.size() + ")");
            dp.setMinSelections(1);
            dp.setMaxSelections(1);

            for (int i = 0; i < remaining.size(); i++) {
                Card c = remaining.get(i);
                dp.addLegalActions(buildAction(i, c.getName(), c));
            }

            int choice = queryAgent(dp.build());
            choice = Math.max(0, Math.min(choice, remaining.size() - 1));
            ordered.add(remaining.remove(choice));
        }
        return ordered;
    }

    /**
     * Whether to put a card on top of library (true) or bottom (false).
     * Used by Clash and similar effects.
     */
    @Override
    public boolean willPutCardOnTop(Card c) {
        DecisionPoint.Builder dp = baseDecision(DecisionType.CONFIRM_ACTION,
                "Put " + c.getName() + " on top of library?");
        dp.setMinSelections(1);
        dp.setMaxSelections(1);
        dp.addLegalActions(buildAction(0, "Put on bottom", c));
        dp.addLegalActions(buildAction(1, "Put on top", c));

        return queryAgent(dp.build()) == 1;
    }

    /**
     * Choose cards to discard unless they are of a specific type.
     * If the hand has cards of the matching type, the agent can choose one to reveal instead of discarding.
     */
    @Override
    public CardCollectionView chooseCardsToDiscardUnlessType(int min, CardCollectionView hand, String param, SpellAbility sa) {
        if (hand.isEmpty()) {
            return new CardCollection();
        }

        // Offer the agent a choice: reveal a matching card, or discard
        // Build list of all cards in hand as options
        DecisionPoint.Builder dp = baseDecision(DecisionType.CHOOSE_CARDS,
                "Discard " + min + " card(s) unless you reveal a " + param);
        dp.setMinSelections(1);
        dp.setMaxSelections(1);

        // Check if any cards match the type — those can be revealed instead
        List<Card> allCards = new ArrayList<>(hand);
        for (int i = 0; i < allCards.size(); i++) {
            Card c = allCards.get(i);
            dp.addLegalActions(buildAction(i, c.getName(), c));
        }

        // Let the agent pick one card. If it's of the right type, it counts as "reveal to avoid discard".
        // If not, we discard. This mirrors the AI logic but gives the RL agent the choice.
        int choice = queryAgent(dp.build());
        choice = Math.max(0, Math.min(choice, allCards.size() - 1));

        Card chosen = allCards.get(choice);

        // Check if the chosen card matches the required type
        String[] restrictions = param.split(",");
        boolean matches = chosen.isValid(restrictions, sa.getActivatingPlayer(), sa.getHostCard(), sa);

        if (matches) {
            // Reveal this card to satisfy the condition (avoid discarding)
            return new CardCollection(chosen);
        }

        // Doesn't match — need to actually discard. Use multi-select for the discard.
        return queryAgentMultiSelect(new ArrayList<>(hand), "Choose cards to discard",
                DecisionType.CHOOSE_CARDS, min, min);
    }

    /**
     * Choose cards to reveal from hand (e.g., Lórien Revealed cycling — reveal Island cards).
     */
    @Override
    public CardCollectionView chooseCardsToRevealFromHand(int min, int max, CardCollectionView valid) {
        if (valid.isEmpty() || max == 0) {
            return new CardCollection();
        }
        if (valid.size() <= min) {
            return valid;
        }
        return queryAgentMultiSelect(new ArrayList<>(valid), "Choose cards to reveal",
                DecisionType.CHOOSE_CARDS, min, max);
    }

    // ═══════════════════════════════════════════════════════════════
    // TIER 3 — No-op overrides (notifications that don't need AI logic)
    // ═══════════════════════════════════════════════════════════════

    @Override
    public void reveal(CardCollectionView cards, ZoneType zone, Player owner,
            String messagePrefix, boolean addMsgSuffix) {
        // No-op: RL agent sees cards via observation
    }

    @Override
    public void notifyOfValue(SpellAbility saSource, forge.game.GameObject realtedTarget, String value) {
        // No-op
    }
}
