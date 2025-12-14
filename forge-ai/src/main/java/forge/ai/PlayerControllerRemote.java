package forge.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import forge.ai.AIAgentClient.AIAgentRequest;
import forge.ai.AIAgentClient.AIAgentResponse;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.player.DelayedReveal;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollectionView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import forge.StaticData;
import forge.game.spellability.TargetRestrictions;
import forge.item.PaperCard;

public class PlayerControllerRemote extends PlayerControllerAi {

    private final AIAgentClient aiAgentClient;
    private final String gameId;

    public PlayerControllerRemote(Game game, Player player, LobbyPlayerAi lobbyPlayer, AIAgentClient aiAgentClient) {
        super(game, player, lobbyPlayer);
        this.aiAgentClient = aiAgentClient;
        this.gameId = UUID.randomUUID().toString();
        System.out.println("PlayerControllerRemote instantiated. GameID: " + gameId);
    }

    @Override
    public boolean mulliganKeepHand(Player player, int cardsToReturn) {
        // TODO: Implement remote mulligan decision
        return true;
    }

    private List<SpellAbility> getPossibleSpellAbilities() {
        List<SpellAbility> allAbilities = new ArrayList<>();

        // Get available lands to play
        CardCollection lands = ComputerUtilAbility.getAvailableLandsToPlay(getGame(), player);
        if (lands != null && !lands.isEmpty()) {
            for (Card land : lands) {
                SpellAbility sa = land.getSpellPermanent();
                if (sa != null) {
                    allAbilities.add(sa);
                }
            }
        }

        // Get available spells and abilities
        CardCollection availableCards = ComputerUtilAbility.getAvailableCards(getGame(), player);
        List<SpellAbility> spellAbilities = ComputerUtilAbility.getSpellAbilities(availableCards, player);

        for (SpellAbility sa : spellAbilities) {
            // Filter to only abilities the player can actually activate
            if (sa.canPlay() && sa.getActivatingPlayer() == player) {
                allAbilities.add(sa);
            }
        }

        return allAbilities;
    }

    @Override
    public List<SpellAbility> chooseSpellAbilityToPlay() {
        if (aiAgentClient != null) {
            System.out.println("aiAgentClient is present. Preparing request...");
            try {
                List<SpellAbility> actions = getPossibleSpellAbilities();
                JsonObject gameState = extractGameState(getGame());
                JsonObject actionState = new JsonObject();
                JsonArray actionsList = new JsonArray();

                for (int i = 0; i < actions.size(); i++) {
                    SpellAbility sa = actions.get(i);
                    JsonObject action = new JsonObject();
                    action.addProperty("index", i);
                    Card source = sa.getHostCard();

                    // Determine action type
                    if (source != null && source.isLand() && sa.isSpell()) {
                        action.addProperty("type", "play_land");
                        action.addProperty("card_id", source.getId());
                        action.addProperty("card_name", source.getName());
                    } else if (sa.isSpell()) {
                        action.addProperty("type", "cast_spell");
                        action.addProperty("card_id", source != null ? source.getId() : -1);
                        action.addProperty("card_name", source != null ? source.getName() : "Unknown");
                        action.addProperty("ability_description", sa.getDescription());
                        action.addProperty("mana_cost",
                                sa.getPayCosts() != null ? sa.getPayCosts().toSimpleString() : "");

                        // Add target information
                        if (sa.usesTargeting()) {
                            TargetRestrictions tgt = sa.getTargetRestrictions();
                            if (tgt != null) {
                                action.addProperty("requires_targets", true);
                                action.addProperty("target_min", tgt.getMinTargets(sa.getHostCard(), sa));
                                action.addProperty("target_max", tgt.getMaxTargets(sa.getHostCard(), sa));
                                action.addProperty("target_zone",
                                        tgt.getZone() != null ? tgt.getZone().toString() : "any");
                            }
                        } else {
                            action.addProperty("requires_targets", false);
                        }
                    } else {
                        action.addProperty("type", "activate_ability");
                        action.addProperty("card_id", source != null ? source.getId() : -1);
                        action.addProperty("card_name", source != null ? source.getName() : "Unknown");
                        action.addProperty("ability_description", sa.getDescription());
                        action.addProperty("mana_cost",
                                sa.getPayCosts() != null ? sa.getPayCosts().toSimpleString() : "no cost");
                        action.addProperty("requires_targets", sa.usesTargeting());
                    }

                    // Add legacy "card" field for compatibility with some agents
                    action.addProperty("card", source != null ? source.getName() : "Unknown");
                    
                    actionsList.add(action);
                }

                // Always available: pass priority
                JsonObject passAction = new JsonObject();
                passAction.addProperty("index", actions.size()); // Index after the last action
                passAction.addProperty("type", "pass_priority");
                actionsList.add(passAction);

                actionState.add("possible_actions", actionsList);

                JsonObject context = new JsonObject();
                context.addProperty("requestType", "possible_actions");
                context.addProperty("phase", getGame().getPhaseHandler().getPhase().toString());
                context.addProperty("turn", getGame().getPhaseHandler().getTurn());
                context.addProperty("playerName", player.getName());

                AIAgentRequest request = new AIAgentRequest(
                        gameId, "possible_actions", gameState, actionState, context);

                System.out.println("Calling AI agent for possible_actions...");
                AIAgentResponse response = aiAgentClient.requestDecision(request);

                if ("possible_actions".equals(response.getDecisionType())) {
                    int chosenIndex = response.getIndex();
                    if (chosenIndex >= 0 && chosenIndex < actions.size()) {
                        SpellAbility chosen = actions.get(chosenIndex);
                        System.out.println("AI chose action: " + chosen.toString());
                        List<SpellAbility> result = new ArrayList<>();
                        result.add(chosen);
                        return result;
                    } else if (chosenIndex == actions.size()) {
                        System.out.println("AI chose to pass priority.");
                        return null; // Null means pass priority
                    }
                }
            } catch (AIAgentClient.AIAgentException e) {
                System.out.println("AIAgentClient communication failed: " + e.getMessage());
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println("Unexpected error in PlayerControllerRemote: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("aiAgentClient is null!");
        }

        System.out.println("Falling back to default AI logic (PlayerControllerAi)...");
        return super.chooseSpellAbilityToPlay();
    }

    @Override
    public void declareAttackers(Player attacker, Combat combat) {
        if (aiAgentClient != null) {
            try {
                JsonObject gameState = extractGameState(getGame());
                JsonObject actionState = new JsonObject();

                // Attackers
                JsonArray attackersJson = new JsonArray();
                CardCollection potentialAttackers = CardLists.filter(attacker.getCreaturesInPlay(),
                        c -> CombatUtil.canAttack(c));
                for (int i = 0; i < potentialAttackers.size(); i++) {
                    Card c = potentialAttackers.get(i);
                    JsonObject att = new JsonObject();
                    att.addProperty("index", i);
                    att.addProperty("id", c.getId());
                    att.addProperty("name", c.getName());
                    att.addProperty("power", c.getNetPower());
                    att.addProperty("toughness", c.getNetToughness());
                    attackersJson.add(att);
                }
                actionState.add("attackers", attackersJson);

                // Defenders
                JsonArray defendersJson = new JsonArray();
                List<GameEntity> defenders = new ArrayList<GameEntity>();
                for (GameEntity d : combat.getDefenders()) {
                    defenders.add(d);
                }
                for (int i = 0; i < defenders.size(); i++) {
                    GameEntity d = defenders.get(i);
                    JsonObject def = new JsonObject();
                    def.addProperty("index", i);
                    def.addProperty("id", d.getId());
                    def.addProperty("name", d.getName());
                    def.addProperty("type", d instanceof Player ? "Player" : "Planeswalker");
                    defendersJson.add(def);
                }
                actionState.add("defenders", defendersJson);

                JsonObject context = new JsonObject();
                context.addProperty("requestType", "declare_attackers");
                context.addProperty("phase", getGame().getPhaseHandler().getPhase().toString());
                context.addProperty("turn", getGame().getPhaseHandler().getTurn());
                context.addProperty("playerName", player.getName());

                AIAgentRequest request = new AIAgentRequest(
                        gameId, "declare_attackers", gameState, actionState, context);

                System.out.println("Calling AI agent for declare_attackers...");
                AIAgentResponse response = aiAgentClient.requestDecision(request);

                if ("declare_attackers".equals(response.getDecisionType())) {
                    JsonArray attackersDec = response.getAttackers();
                    if (attackersDec != null) {
                        for (int i = 0; i < attackersDec.size(); i++) {
                            JsonObject dec = attackersDec.get(i).getAsJsonObject();
                            int attIdx = dec.get("attacker_index").getAsInt();
                            int defIdx = dec.get("defender_index").getAsInt();

                            if (attIdx >= 0 && attIdx < potentialAttackers.size() &&
                                    defIdx >= 0 && defIdx < defenders.size()) {
                                Card attackerCard = potentialAttackers.get(attIdx);
                                GameEntity defenderEntity = defenders.get(defIdx);
                                combat.addAttacker(attackerCard, defenderEntity);
                                System.out.println("AI declared attacker: " + attackerCard.getName() + " -> "
                                        + defenderEntity.getName());
                            }
                        }
                    }
                }
                return;
            } catch (Exception e) {
                System.err.println("AI agent error in declareAttackers: " + e.getMessage());
                e.printStackTrace();
            }
        }
        // Fallback to default AI
        super.declareAttackers(attacker, combat);
    }

    @Override
    public void declareBlockers(Player defender, Combat combat) {
        if (aiAgentClient != null) {
            try {
                JsonObject gameState = extractGameState(getGame());
                JsonObject actionState = new JsonObject();

                // Attackers (to be blocked)
                JsonArray attackersJson = new JsonArray();
                CardCollection attackers = combat.getAttackers();
                for (int i = 0; i < attackers.size(); i++) {
                    Card c = attackers.get(i);
                    JsonObject att = new JsonObject();
                    att.addProperty("index", i);
                    att.addProperty("id", c.getId());
                    att.addProperty("name", c.getName());
                    att.addProperty("power", c.getNetPower());
                    att.addProperty("toughness", c.getNetToughness());
                    GameEntity attacked = combat.getDefenderByAttacker(c);
                    att.addProperty("attacking", attacked != null ? attacked.getName() : "Unknown");
                    attackersJson.add(att);
                }
                actionState.add("attackers", attackersJson);

                // Blockers
                JsonArray blockersJson = new JsonArray();
                CardCollection potentialBlockers = CardLists.filter(defender.getCreaturesInPlay(),
                        c -> CombatUtil.canBlock(c));
                for (int i = 0; i < potentialBlockers.size(); i++) {
                    Card c = potentialBlockers.get(i);
                    JsonObject blk = new JsonObject();
                    blk.addProperty("index", i);
                    blk.addProperty("id", c.getId());
                    blk.addProperty("name", c.getName());
                    blk.addProperty("power", c.getNetPower());
                    blk.addProperty("toughness", c.getNetToughness());
                    blockersJson.add(blk);
                }
                actionState.add("blockers", blockersJson);

                JsonObject context = new JsonObject();
                context.addProperty("requestType", "declare_blockers");
                context.addProperty("phase", getGame().getPhaseHandler().getPhase().toString());
                context.addProperty("turn", getGame().getPhaseHandler().getTurn());
                context.addProperty("playerName", player.getName());

                AIAgentRequest request = new AIAgentRequest(
                        gameId, "declare_blockers", gameState, actionState, context);

                System.out.println("Calling AI agent for declare_blockers...");
                AIAgentResponse response = aiAgentClient.requestDecision(request);

                if ("declare_blockers".equals(response.getDecisionType())) {
                    JsonArray blocksDec = response.getBlocks();
                    if (blocksDec != null) {
                        for (int i = 0; i < blocksDec.size(); i++) {
                            JsonObject dec = blocksDec.get(i).getAsJsonObject();
                            int blkIdx = dec.get("blocker_index").getAsInt();
                            int attIdx = dec.get("attacker_index").getAsInt();

                            if (blkIdx >= 0 && blkIdx < potentialBlockers.size() &&
                                    attIdx >= 0 && attIdx < attackers.size()) {
                                Card blockerCard = potentialBlockers.get(blkIdx);
                                Card attackerCard = attackers.get(attIdx);
                                combat.addBlocker(attackerCard, blockerCard);
                                System.out.println("AI declared blocker: " + blockerCard.getName() + " -> "
                                        + attackerCard.getName());
                            }
                        }
                    }
                }
                return;
            } catch (Exception e) {
                System.err.println("AI agent error in declareBlockers: " + e.getMessage());
                e.printStackTrace();
            }
        }
        // Fallback to default AI
        super.declareBlockers(defender, combat);
    }

    private <T extends GameEntity> JsonObject createTargetOptionsJson(FCollectionView<T> optionList, int min,
            int max, String title) {
        JsonObject result = new JsonObject();
        result.addProperty("min", min);
        result.addProperty("max", max);
        result.addProperty("title", title);

        JsonArray options = new JsonArray();
        int index = 0;
        for (T target : optionList) {
            JsonObject option = new JsonObject();
            option.addProperty("index", index++);
            option.addProperty("type", target.getClass().getSimpleName());
            option.addProperty("name", target.getName());
            option.addProperty("id", target.getId());

            if (target instanceof Player) {
                option.addProperty("life", ((Player) target).getLife());
            }

            options.add(option);
        }
        result.add("targets", options);
        return result;
    }

    @Override
    public <T extends GameEntity> T chooseSingleEntityForEffect(FCollectionView<T> optionList,
            DelayedReveal delayedReveal, SpellAbility sa, String title, boolean isOptional, Player targetedPlayer,
            Map<String, Object> params) {
        List<T> results = chooseEntitiesForEffect(optionList, isOptional ? 0 : 1, 1, delayedReveal, sa, title,
                targetedPlayer, params);
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public <T extends GameEntity> List<T> chooseEntitiesForEffect(FCollectionView<T> optionList, int min, int max,
            DelayedReveal delayedReveal, SpellAbility sa, String title, Player targetedPlayer,
            Map<String, Object> params) {

        List<T> selected = new ArrayList<>();
        List<T> options = new ArrayList<>();
        for (T t : optionList)
            options.add(t);

        if (options.isEmpty()) {
            return selected;
        }

        // Create action state for target selection
        JsonObject actionState = createTargetOptionsJson(optionList, min, max, title);

        // If AI agent is configured, call out to it for decision
        if (aiAgentClient != null) {
            try {
                JsonObject gameState = extractGameState(getGame());
                JsonObject context = new JsonObject();
                context.addProperty("requestType", "target");
                context.addProperty("spellName",
                        sa != null && sa.getHostCard() != null ? sa.getHostCard().getName() : "Unknown");
                context.addProperty("spellDescription", sa != null ? sa.getDescription() : "");

                AIAgentRequest request = new AIAgentRequest(
                        gameId, "target", gameState, actionState, context);

                System.out.println("Calling AI agent for target selection...");
                AIAgentResponse response = aiAgentClient.requestDecision(request);

                // Handle multi-select responses
                if (response.getIndices() != null) {
                    for (int idx : response.getIndices()) {
                        if (idx >= 0 && idx < options.size() && selected.size() < max) {
                            T target = options.get(idx);
                            if (!selected.contains(target)) {
                                selected.add(target);
                            }
                        }
                    }
                } else if (response.getIndex() >= 0 && response.getIndex() < options.size()) {
                    selected.add(options.get(response.getIndex()));
                }

                System.out.println("AI agent selected " + selected.size() + " target(s)");
                return selected;

            } catch (Exception e) {
                System.err.println("AI agent error, falling back to default AI: " + e.getMessage());
            }
        }

        // Fallback to default AI
        return super.chooseEntitiesForEffect(optionList, min, max, delayedReveal, sa, title, targetedPlayer, params);
    }

    private final java.util.Set<String> knownCardNames = new java.util.HashSet<>();

    private JsonObject getCardDefinition(String cardName) {
        JsonObject def = new JsonObject();
        PaperCard pc = StaticData.instance().getCommonCards().getCard(cardName);
        if (pc == null) {
            // Try to find by name if not exact match (sometimes names vary slightly)
            // But for now, just return basic info if not found
            def.addProperty("name", cardName);
            def.addProperty("error", "Card not found in DB");
            return def;
        }

        def.addProperty("name", pc.getName());
        def.addProperty("mana_cost", pc.getRules().getManaCost().toString());
        def.addProperty("type", pc.getRules().getType().toString());
        def.addProperty("oracle_text", pc.getRules().getOracleText());
        
        if (pc.getRules().getType().isCreature()) {
            def.addProperty("power", pc.getRules().getPower().toString());
            def.addProperty("toughness", pc.getRules().getToughness().toString());
        }

        return def;
    }

    private void addVisibleCardsToKnownSet(Game game) {
        // Player 1 Zones (Our Agent)
        for (Card c : player.getCardsIn(ZoneType.Hand)) knownCardNames.add(c.getName());
        for (Card c : player.getCardsIn(ZoneType.Graveyard)) knownCardNames.add(c.getName());
        for (Card c : player.getCardsIn(ZoneType.Battlefield)) knownCardNames.add(c.getName());
        for (Card c : player.getCardsIn(ZoneType.Exile)) knownCardNames.add(c.getName());
        for (Card c : player.getCardsIn(ZoneType.Command)) knownCardNames.add(c.getName());

        // Player 2 Zones (Visible)
        Player opponent = player.getSingleOpponent(); 
        if (opponent != null) {
            for (Card c : opponent.getCardsIn(ZoneType.Graveyard)) knownCardNames.add(c.getName());
            for (Card c : opponent.getCardsIn(ZoneType.Battlefield)) knownCardNames.add(c.getName());
            for (Card c : opponent.getCardsIn(ZoneType.Exile)) knownCardNames.add(c.getName());
            for (Card c : opponent.getCardsIn(ZoneType.Command)) knownCardNames.add(c.getName());
        }

        // Stack
        for (forge.game.spellability.SpellAbilityStackInstance stackItem : game.getStack()) {
            Card source = stackItem.getSpellAbility().getHostCard();
            if (source != null) {
                knownCardNames.add(source.getName());
            }
        }
    }

    private JsonObject extractGameState(Game game) {
        // Update tracking of seen cards
        addVisibleCardsToKnownSet(game);

        JsonObject state = new JsonObject();

        // Add definitions for all known cards
        JsonObject cardDefinitions = new JsonObject();
        for (String cardName : knownCardNames) {
            cardDefinitions.add(cardName, getCardDefinition(cardName));
        }
        state.add("card_definitions", cardDefinitions);

        // General Game Info
        state.addProperty("turn", game.getPhaseHandler().getTurn());
        state.addProperty("phase", game.getPhaseHandler().getPhase().toString());
        state.addProperty("activePlayerId", game.getPhaseHandler().getPlayerTurn().getId());
        state.addProperty("priorityPlayerId", game.getPhaseHandler().getPlayerTurn().getId()); // Approximate

        // Combat
        if (game.getPhaseHandler().getCombat() != null) {
            JsonObject combatJson = new JsonObject();
            Combat combat = game.getPhaseHandler().getCombat();

            JsonArray attackers = new JsonArray();
            for (Card attacker : combat.getAttackers()) {
                JsonObject att = new JsonObject();
                att.addProperty("name", attacker.getName());
                att.addProperty("id", attacker.getId());
                attackers.add(att);
            }
            combatJson.add("attackers", attackers);

            JsonArray blockers = new JsonArray();
            for (Card attacker : combat.getAttackers()) {
                CardCollection blockingCards = combat.getBlockers(attacker);
                if (blockingCards != null && !blockingCards.isEmpty()) {
                    for (Card blocker : blockingCards) {
                        JsonObject blk = new JsonObject();
                        blk.addProperty("blocker_name", blocker.getName());
                        blk.addProperty("blocker_id", blocker.getId());
                        blk.addProperty("attacker_name", attacker.getName());
                        blk.addProperty("attacker_id", attacker.getId());
                        blockers.add(blk);
                    }
                }
            }
            combatJson.add("blockers", blockers);

            state.add("combat", combatJson);
        }

        // Stack - show what spells/abilities are on the stack
        JsonArray stackArray = new JsonArray();
        for (forge.game.spellability.SpellAbilityStackInstance stackItem : game.getStack()) {
            JsonObject stackObj = new JsonObject();
            SpellAbility sa = stackItem.getSpellAbility();
            Card source = sa.getHostCard();
            stackObj.addProperty("card_name", source != null ? source.getName() : "Unknown");
            stackObj.addProperty("card_id", source != null ? source.getId() : -1);
            stackObj.addProperty("description", sa.getStackDescription());
            stackObj.addProperty("controller", sa.getActivatingPlayer().getName());
            stackArray.add(stackObj);
        }
        state.add("stack", stackArray);
        state.addProperty("stack_size", game.getStack().size());

        // Players
        JsonArray playersArray = new JsonArray();
        for (Player p : game.getPlayers()) {
            JsonObject playerObj = new JsonObject();
            playerObj.addProperty("id", p.getId());
            playerObj.addProperty("name", p.getName());
            playerObj.addProperty("life", p.getLife());
            playerObj.addProperty("libraryCount", p.getCardsIn(ZoneType.Library).size());

            // Hand
            JsonArray handArray = new JsonArray();
            for (Card c : p.getCardsIn(ZoneType.Hand)) {
                handArray.add(c.getName());
            }
            playerObj.add("hand", handArray);

            // Other Zones
            playerObj.add("graveyard", getZoneJson(p, ZoneType.Graveyard));
            playerObj.add("battlefield", getZoneJson(p, ZoneType.Battlefield));
            playerObj.add("exile", getZoneJson(p, ZoneType.Exile));

            playersArray.add(playerObj);
        }
        state.add("players", playersArray);

        return state;
    }

    private JsonArray getZoneJson(Player p, ZoneType zone) {
        JsonArray zoneArray = new JsonArray();
        for (Card c : p.getCardsIn(zone)) {
            JsonObject cardObj = new JsonObject();
            cardObj.addProperty("name", c.getName());
            cardObj.addProperty("id", c.getId());
            cardObj.addProperty("zone", zone.toString());
            zoneArray.add(cardObj);
        }
        return zoneArray;
    }
}
