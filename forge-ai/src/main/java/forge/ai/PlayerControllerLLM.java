package forge.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import forge.LobbyPlayer;
import forge.card.MagicColor;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.combat.Combat;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.game.combat.CombatUtil;
import forge.util.ITriggerEvent;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Player controller implementation that uses an LLM service for decision-making.
 * Extends PlayerControllerAi to leverage existing implementations for most methods.
 */
public class PlayerControllerLLM extends PlayerControllerAi {
    private static final Logger logger = Logger.getLogger(PlayerControllerLLM.class.getName());
    private final LLMClient client;
    private final Gson gson = new Gson();
    private final AiController brains;

    /**
     * Creates a new LLM-based player controller.
     *
     * @param game The game instance
     * @param player The player being controlled
     * @param lp The lobby player
     * @param client The LLM client to use for decision-making
     */
    public PlayerControllerLLM(Game game, Player player, LobbyPlayer lp, LLMClient client) {
        super(game, player, lp);
        if (client == null) {
            throw new IllegalArgumentException("LLMClient cannot be null for LLM controller");
        }
        this.client = client;
        this.brains = new AiController(player, game);
        logger.info("Created LLM controller for " + player.getName());
        
        // Verify client is working on initialization
        try {
            JsonObject testRequest = new JsonObject();
            testRequest.addProperty("context", "debug");
            testRequest.addProperty("message", "Initializing PlayerControllerLLM for " + player.getName());
            client.ask(testRequest);
            logger.info("LLM client verification successful for controller of " + player.getName());
        } catch (IOException e) {
            logger.severe("LLM client failed verification for controller of " + player.getName() + ": " + e.getMessage());
            throw new RuntimeException("LLM client failed verification: " + e.getMessage(), e);
        }
    }

    /**
     * Helper method to create a game state JSON object for the LLM
     */
    private JsonObject createGameStateJson(String context) {
        JsonObject state = new JsonObject();
        state.addProperty("context", context);
        
        // Player info
        JsonObject playerInfo = new JsonObject();
        playerInfo.addProperty("name", player.getName());
        playerInfo.addProperty("life", player.getLife());
        
        // Add mana pool info
        JsonObject manaPool = new JsonObject();
        manaPool.addProperty("white", player.getManaPool().getAmountOfColor(MagicColor.WHITE));
        manaPool.addProperty("blue", player.getManaPool().getAmountOfColor(MagicColor.BLUE));
        manaPool.addProperty("black", player.getManaPool().getAmountOfColor(MagicColor.BLACK));
        manaPool.addProperty("red", player.getManaPool().getAmountOfColor(MagicColor.RED));
        manaPool.addProperty("green", player.getManaPool().getAmountOfColor(MagicColor.GREEN));
        manaPool.addProperty("colorless", player.getManaPool().getAmountOfColor(MagicColor.COLORLESS));
        playerInfo.add("manaPool", manaPool);
        
        state.add("player", playerInfo);
        
        // Game phase info
        JsonObject gamePhase = new JsonObject();
        PhaseHandler phaseHandler = getGame().getPhaseHandler();
        PhaseType currentPhase = phaseHandler.getPhase();
        gamePhase.addProperty("currentPhase", currentPhase != null ? currentPhase.toString() : "UNKNOWN");
        gamePhase.addProperty("currentTurn", phaseHandler.getTurn());
        gamePhase.addProperty("isPlayerTurn", phaseHandler.getPlayerTurn() == player);
        state.add("gamePhase", gamePhase);
        
        // Battlefield for player
        JsonArray battlefield = new JsonArray();
        for (Card c : player.getCardsIn(ZoneType.Battlefield)) {
            JsonObject cardObj = createCardJson(c);
            battlefield.add(cardObj);
        }
        state.add("battlefield", battlefield);
        
        // Hand
        JsonArray hand = new JsonArray();
        for (Card c : player.getCardsIn(ZoneType.Hand)) {
            JsonObject cardObj = createCardJson(c);
            hand.add(cardObj);
        }
        state.add("hand", hand);
        
        // Commander(s)
        JsonArray commanders = new JsonArray();
        for (Card c : player.getCommanders()) {
            JsonObject cardObj = createCardJson(c);
            commanders.add(cardObj);
        }
        state.add("commanders", commanders);
        
        // Opponents
        JsonArray opponents = new JsonArray();
        for (Player p : getGame().getPlayers()) {
            if (p != player) {
                JsonObject opponentObj = new JsonObject();
                opponentObj.addProperty("name", p.getName());
                opponentObj.addProperty("life", p.getLife());
                
                // Opponent's battlefield
                JsonArray opponentBattlefield = new JsonArray();
                for (Card c : p.getCardsIn(ZoneType.Battlefield)) {
                    JsonObject cardObj = createCardJson(c);
                    opponentBattlefield.add(cardObj);
                }
                opponentObj.add("battlefield", opponentBattlefield);
                
                // Opponent's commanders
                JsonArray opponentCommanders = new JsonArray();
                for (Card c : p.getCommanders()) {
                    JsonObject cardObj = createCardJson(c);
                    opponentCommanders.add(cardObj);
                }
                opponentObj.add("commanders", opponentCommanders);
                
                opponents.add(opponentObj);
            }
        }
        state.add("opponents", opponents);
        
        return state;
    }
    
    /**
     * Helper method to create a JSON representation of a card
     */
    private JsonObject createCardJson(Card card) {
        JsonObject cardObj = new JsonObject();
        cardObj.addProperty("id", card.getId());
        cardObj.addProperty("name", card.getName());
        cardObj.addProperty("type", card.getType() != null ? card.getType().toString() : "Unknown");
        cardObj.addProperty("power", card.getNetPower());
        cardObj.addProperty("toughness", card.getNetToughness());
        cardObj.addProperty("text", card.getSpellText());
        cardObj.addProperty("isTapped", card.isTapped());
        return cardObj;
    }
    
    /**
     * Helper method to send a request to the LLM
     */
    private JsonObject askLLM(JsonObject state, String context) {
        logger.info("Sending " + context + " request to LLM service");
        try {
            // Double check that client is not null before trying to use it
            if (client == null) {
                String errorMsg = "LLM client is null in PlayerControllerLLM";
                logger.severe(errorMsg);
                throw new RuntimeException(errorMsg);
            }
            
            JsonObject response = client.ask(state);
            
            // Verify response is not null
            if (response == null) {
                String errorMsg = "Received null response from LLM service for " + context;
                logger.severe(errorMsg);
                throw new RuntimeException(errorMsg);
            }
            
            logger.info("Received response from LLM service for " + context);
            return response;
        } catch (IOException e) {
            String errorMsg = "Error communicating with LLM service: " + e.getMessage();
            logger.log(Level.SEVERE, errorMsg, e);
            System.err.println("===========================================================");
            System.err.println("CRITICAL ERROR IN LLM COMMUNICATION: " + errorMsg);
            System.err.println("===========================================================");
            e.printStackTrace();
            System.err.println("===========================================================");
            throw new RuntimeException("Failed to communicate with LLM service: " + e.getMessage(), e);
        }
    }

    /**
     * Chooses an ability to play from the given list by consulting the LLM.
     * 
     * @param hostCard the card with the ability
     * @param abilities list of available abilities to choose from
     * @param triggerEvent the event that triggered this choice
     * @return the chosen SpellAbility or null
     */
    @Override
    public SpellAbility getAbilityToPlay(Card hostCard, List<SpellAbility> abilities, ITriggerEvent triggerEvent) {
        if (abilities.isEmpty()) {
            return null;
        }
        
        logger.info("getAbilityToPlay called for " + (hostCard != null ? hostCard.getName() : "Unknown") + " with " + abilities.size() + " abilities");
        
        // Create game state context
        JsonObject state = createGameStateJson("chooseAbility");
        
        // Add specific ability info
        JsonArray abilitiesArr = new JsonArray();
        for (SpellAbility sa : abilities) {
            JsonObject saObj = new JsonObject();
            saObj.addProperty("id", Integer.toString(sa.getId()));
            saObj.addProperty("hostCard", sa.getHostCard() != null ? sa.getHostCard().getName() : "Unknown");
            saObj.addProperty("description", sa.getDescription());
            saObj.addProperty("toString", sa != null ? sa.toString() : "Unknown");
            abilitiesArr.add(saObj);
        }
        state.add("abilities", abilitiesArr);
        
        // Get LLM response - will throw exception if server doesn't respond
        JsonObject response = askLLM(state, "chooseAbility");
        
        if (response != null && response.has("spellAbilityId")) {
            String chosenId = response.get("spellAbilityId").getAsString();
            
            // Find the matching ability
            for (SpellAbility sa : abilities) {
                if (Integer.toString(sa.getId()).equals(chosenId)) {
                    return sa;
                }
            }
        }
        
        // If we got here without finding a valid ability, throw an exception
        throw new RuntimeException("LLM failed to provide a valid ability choice");
    }

    /**
     * Chooses targets for a SpellAbility by consulting the LLM.
     * 
     * @param sa the SpellAbility that needs targets
     * @return true if targeting was successful, false otherwise
     */
    @Override
    public boolean chooseTargetsFor(SpellAbility sa) {
        // Create game state
        JsonObject state = createGameStateJson("chooseTargets");
        
        // Add ability info
        JsonObject saObj = new JsonObject();
        saObj.addProperty("id", Integer.toString(sa.getId()));
        saObj.addProperty("hostCard", sa.getHostCard() != null ? sa.getHostCard().getName() : "Unknown");
        saObj.addProperty("description", sa.getDescription());
        saObj.addProperty("toString", sa != null ? sa.toString() : "Unknown");
        
        // Add potential targets
        CardCollectionView potentialTargets = getGame().getCardsInGame();
        JsonArray targetsArr = new JsonArray();
        for (Card c : potentialTargets) {
            if (sa.canTarget(c)) {
                targetsArr.add(createCardJson(c));
            }
        }
        saObj.add("potentialTargets", targetsArr);
        
        state.add("spellAbility", saObj);
        
        // Get LLM response - will throw exception if server doesn't respond
        JsonObject response = askLLM(state, "chooseTargets");
        
        if (response != null && response.has("targets")) {
            JsonArray targetIds = response.getAsJsonArray("targets");
            
            // Process targets
            for (int i = 0; i < targetIds.size(); i++) {
                String targetId = targetIds.get(i).getAsString();
                
                // Find the card by ID
                for (Card c : potentialTargets) {
                    if (Integer.toString(c.getId()).equals(targetId) && sa.canTarget(c)) {
                        sa.getTargets().add(c);
                        break;
                    }
                }
            }
            
            if (sa.getTargets().size() > 0) {
                return true;
            }
        }
        
        // If we got here without setting valid targets, throw an exception
        throw new RuntimeException("LLM failed to provide valid targets");
    }
    
    /**
     * Confirms an action by consulting the LLM.
     * 
     * @param sa the SpellAbility that needs confirmation
     * @param mode the confirmation mode
     * @param message the message to display
     * @param options available options for the confirmation
     * @param cardToShow card to show during confirmation
     * @param params additional parameters
     * @return true if the action is confirmed, false otherwise
     */
    @Override
    public boolean confirmAction(SpellAbility sa, forge.game.player.PlayerActionConfirmMode mode, String message, 
            List<String> options, Card cardToShow, Map<String, Object> params) {
        logger.info("confirmAction called for " + (sa.getHostCard() != null ? sa.getHostCard().getName() : "Unknown") + ": " + message);
        
        // Create game state
        JsonObject state = createGameStateJson("confirmAction");
        
        // Add ability info
        JsonObject saObj = new JsonObject();
        saObj.addProperty("id", Integer.toString(sa.getId()));
        saObj.addProperty("hostCard", sa.getHostCard() != null ? sa.getHostCard().getName() : "Unknown");
        saObj.addProperty("description", sa.getDescription());
        saObj.addProperty("message", message);
        saObj.addProperty("mode", mode != null ? mode.toString() : "Unknown");
        state.add("spellAbility", saObj);
        
        // Get LLM response
        JsonObject response = askLLM(state, "confirmAction");
        
        if (response != null && response.has("confirm")) {
            return response.get("confirm").getAsBoolean();
        }
        
        // If we got here without a valid response, throw an exception
        throw new RuntimeException("LLM failed to provide a valid confirmation choice");
    }
    
        
        
        
    /**
     * Declares attackers for combat by consulting the LLM.
     * 
     * @param attacker the attacking player
     * @param combat the current combat instance
     */
    @Override
    public void declareAttackers(Player attacker, Combat combat) {
        if (attacker != player) {
            return;
        }
        
        logger.info("declareAttackers called for " + attacker.getName());
        
        // Let parent handle attacking logic for now, since this method
        // is complex and involves multiple steps
        PhaseHandler ph = getGame().getPhaseHandler();
        
        if (ph.getPhase() != PhaseType.COMBAT_DECLARE_ATTACKERS) {
            return;
        }
        
        // Create game state
        JsonObject state = createGameStateJson("declareAttackers");
        
        // Add potential attackers
        CardCollectionView possibleAttackers = player.getCreaturesInPlay();
        JsonArray attackersArr = new JsonArray();
        for (Card c : possibleAttackers) {
            if (CombatUtil.canAttack(c)) {
                attackersArr.add(createCardJson(c));
            }
        }
        state.add("possibleAttackers", attackersArr);
        
        // Add potential defenders
        JsonArray defendersArr = new JsonArray();
        for (GameEntity defender : combat.getDefenders()) {
            JsonObject defenderObj = new JsonObject();
            defenderObj.addProperty("id", defender.getId());
            defenderObj.addProperty("name", defender.getName());
            if (defender instanceof Player) {
                defenderObj.addProperty("type", "player");
                defenderObj.addProperty("life", ((Player) defender).getLife());
            } else {
                defenderObj.addProperty("type", "planeswalker");
                Card pw = (Card) defender;
                defenderObj.addProperty("loyalty", pw.getCurrentLoyalty());
            }
            defendersArr.add(defenderObj);
        }
        state.add("possibleDefenders", defendersArr);
        
        // Get LLM response
        JsonObject response = askLLM(state, "declareAttackers");
        
        if (response != null && response.has("attackers")) {
            JsonArray attackerAssignments = response.getAsJsonArray("attackers");
            
            // Process attackers
            for (int i = 0; i < attackerAssignments.size(); i++) {
                JsonObject assignment = attackerAssignments.get(i).getAsJsonObject();
                String attackerId = assignment.get("cardId").getAsString();
                String defenderId = assignment.get("defenderId").getAsString();
                
                // Find the attacker
                Card attackerCard = null;
                GameEntity defenderEntity = null;
                
                for (Card c : possibleAttackers) {
                    if (Integer.toString(c.getId()).equals(attackerId)) {
                        attackerCard = c;
                        break;
                    }
                }
                
                for (GameEntity d : combat.getDefenders()) {
                    if (Integer.toString(d.getId()).equals(defenderId)) {
                        defenderEntity = d;
                        break;
                    }
                }
                
                // Declare the attack if both attacker and defender are valid
                if (attackerCard != null && defenderEntity != null && CombatUtil.canAttack(attackerCard, defenderEntity)) {
                    combat.addAttacker(attackerCard, defenderEntity);
                }
            }
            
            return;
        }
        
        // If we got here without a valid response, throw an exception
        throw new RuntimeException("LLM failed to provide valid attacker declarations");
    }
    
    /**
     * Decides whether to keep the current hand or mulligan by consulting the LLM.
     * 
     * @param player the player making the decision
     * @param cardsToReturn number of cards to return for partial mulligan
     * @return true if the hand should be kept, false to mulligan
     */
    @Override
    public boolean mulliganKeepHand(Player player, int cardsToReturn) {
        logger.info("mulliganKeepHand called for " + player.getName());
        
        // Create game state
        JsonObject state = createGameStateJson("mulliganKeepHand");
        
        // Add hand cards
        JsonArray handCards = new JsonArray();
        for (Card c : player.getCardsIn(ZoneType.Hand)) {
            handCards.add(createCardJson(c));
        }
        state.add("hand", handCards);
        state.addProperty("cardsToReturn", cardsToReturn);
        
        // Get LLM response
        JsonObject response = askLLM(state, "mulliganKeepHand");
        
        if (response != null && response.has("keepHand")) {
            return response.get("keepHand").getAsBoolean();
        }
        
        // If we got here without a valid response, return a reasonable default
        logger.warning("LLM failed to provide valid mulligan choice, defaulting to keep hand");
        return true; // Default to keeping hand
    }
}