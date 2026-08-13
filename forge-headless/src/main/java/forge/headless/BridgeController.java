package forge.headless;

import java.io.PrintStream;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import forge.LobbyPlayer;
import forge.ai.PlayerControllerAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetChoices;
import forge.game.zone.ZoneType;

/**
 * Protocol boundary around Forge's real AI controller.
 *
 * <p>The Forge-controlled seat delegates decisions to {@link PlayerControllerAi}. Opponent
 * actions enter through {@link #acceptOpponentAction(JsonNode, PrintStream)}; Task C will replay
 * those actions into the mirrored game at that single boundary.</p>
 */
final class BridgeController extends PlayerControllerAi {
    private final int seat;
    private final boolean forgeAiSeat;

    BridgeController(Game game, Player player, LobbyPlayer lobbyPlayer, int seat, boolean forgeAiSeat) {
        super(game, player, lobbyPlayer);
        this.seat = seat;
        this.forgeAiSeat = forgeAiSeat;
    }

    ObjectNode decidePriority() {
        requireForgeAiSeat();
        List<SpellAbility> choices = super.chooseSpellAbilityToPlay();
        if (choices == null || choices.isEmpty()) {
            return passAction();
        }
        return describeAction(choices.get(0));
    }

    ObjectNode decideMulligan(int cardsToReturn) {
        requireForgeAiSeat();
        boolean keep = super.mulliganKeepHand(player, cardsToReturn);
        ObjectNode result = BridgeTransport.JSON.createObjectNode();
        if (!keep) {
            result.put("type", "mulligan");
            return result;
        }

        result.put("type", "keep");
        ArrayNode bottom = result.putArray("bottom");
        if (cardsToReturn > 0) {
            CardCollectionView cards = super.londonMulliganReturnCards(player, cardsToReturn);
            for (Card card : cards) {
                bottom.add(cardReference(card));
            }
        }
        return result;
    }

    void acceptOpponentAction(JsonNode action, PrintStream diagnostics) {
        if (forgeAiSeat) {
            throw new IllegalStateException("opponent_action cannot target the Forge AI seat");
        }
        // Task C replaces this acceptance point with exact legal-action matching and replay.
        diagnostics.println("Bridge accepted scripted opponent action for seat " + seat + ": " + action);
    }

    private ObjectNode describeAction(SpellAbility ability) {
        ObjectNode result = BridgeTransport.JSON.createObjectNode();
        if (ability.isLandAbility()) {
            result.put("type", "play_land");
            result.set("card", cardReference(ability.getHostCard()));
            return result;
        }

        result.put("type", ability.isSpell() ? "cast" : "activate");
        if (ability.isSpell()) {
            result.set("card", cardReference(ability.getHostCard()));
        } else {
            result.set("source", cardReference(ability.getHostCard()));
        }
        ArrayNode targets = result.putArray("targets");
        TargetChoices chosenTargets = ability.getTargets();
        for (Player target : chosenTargets.getTargetPlayers()) {
            ObjectNode targetNode = targets.addObject();
            targetNode.put("kind", "player");
            targetNode.put("seat", target.getId() + 1);
        }
        for (Card target : chosenTargets.getTargetCards()) {
            ObjectNode targetNode = targets.addObject();
            targetNode.put("kind", "card");
            targetNode.set("card", cardReference(target));
        }
        result.putArray("modes");
        result.putNull("x");
        result.put("payment", "auto");
        return result;
    }

    private ObjectNode cardReference(Card card) {
        ObjectNode reference = BridgeTransport.JSON.createObjectNode();
        reference.put("name", card.getName());
        reference.put("zone", card.getZone() == null ? "none" : card.getZone().getZoneType().name().toLowerCase());
        reference.put("idx", sameNameIndex(card));
        if (card.isInZone(ZoneType.Battlefield)) {
            reference.put("object_id", "forge-" + card.getId());
        }
        return reference;
    }

    private int sameNameIndex(Card card) {
        if (card.getZone() == null) {
            return 0;
        }
        int index = 0;
        for (Card candidate : card.getZone().getCards()) {
            if (candidate == card) {
                return index;
            }
            if (candidate.getName().equals(card.getName())) {
                index++;
            }
        }
        return index;
    }

    private static ObjectNode passAction() {
        ObjectNode result = BridgeTransport.JSON.createObjectNode();
        result.put("type", "pass");
        return result;
    }

    private void requireForgeAiSeat() {
        if (!forgeAiSeat) {
            throw new IllegalStateException("Decision requested from mirrored opponent seat " + seat);
        }
    }
}
