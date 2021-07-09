package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.base.Predicates;

import forge.StaticData;
import forge.card.CardRulesPredicates;
import forge.card.ICardFace;
import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CounterType;
import forge.game.event.GameEventCardCounters;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.Localizer;

public class VentureEffect  extends SpellAbilityEffect {

    private Card getDungeonCard(SpellAbility sa, Player player) {
        final Game game = player.getGame();

        CardCollectionView commandCards = player.getCardsIn(ZoneType.Command);
        for (Card card : commandCards) {
            if (card.getType().isDungeon()) {
                if (!card.isInLastRoom()) {
                    return card;
                }
                // If the current dungeon is already in last room, complete it first.
                game.getAction().completeDungeon(player, card);
                break;
            }
        }

        // Create a new dungeon card chosen by player in command zone.
        List<PaperCard> dungeonCards = StaticData.instance().getVariantCards().getAllCards(
            Predicates.compose(CardRulesPredicates.Presets.IS_DUNGEON, PaperCard.FN_GET_RULES));
        List<ICardFace> faces = new ArrayList<>();
        for (PaperCard pc : dungeonCards) {
            faces.add(pc.getRules().getMainPart());
        }
        String message = Localizer.getInstance().getMessage("lblChooseDungeon");
        String chosen = player.getController().chooseCardName(sa, faces, message);
        Card dungeon = Card.fromPaperCard(StaticData.instance().getVariantCards().getUniqueByName(chosen), player);

        game.getTriggerHandler().suppressMode(TriggerType.ChangesZone);
        game.getAction().moveTo(ZoneType.Command, dungeon, sa);
        game.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);

        return dungeon;
    }

    private String chooseNextRoom(SpellAbility sa, Player player, Card dungeon, String room) {
        String nextRoomParam = "";
        for (final Trigger t : dungeon.getTriggers()) {
            SpellAbility roomSA = t.getOverridingAbility();
            if (roomSA.getParam("RoomName").equals(room)) {
                nextRoomParam = roomSA.getParam("NextRoomName");
                break;
            }
        }
        String [] nextRoomNames = nextRoomParam.split(",");
        if (nextRoomNames.length > 1) {
            List<SpellAbility> candidates = new ArrayList<>();
            for (String nextRoomName : nextRoomNames) {
                for (final Trigger t : dungeon.getTriggers()) {
                    SpellAbility roomSA = t.getOverridingAbility();
                    if (roomSA.getParam("RoomName").equals(nextRoomName)) {
                        candidates.add(new WrappedAbility(t, roomSA, player));
                        break;
                    }
                }
            }
            final String title = Localizer.getInstance().getMessage("lblChooseRoom");
            SpellAbility chosen = player.getController().chooseSingleSpellForEffect(candidates, sa, title, null);
            return chosen.getParam("RoomName");
        } else {
            return nextRoomNames[0];
        }
    }

    private void ventureIntoDungeon(SpellAbility sa, Player player) {
        if (player.getVenturedThisTurn() >= 1 && player.hasKeyword("You can't venture into the dungeon more than once each turn.")) {
            return;
        }

        final Game game = player.getGame();
        Card dungeon = getDungeonCard(sa, player);
        String room = dungeon.getCurrentRoom();
        String nextRoom = null;

        // Determine next room to venture into
        if (room == null || room.isEmpty()) {
            SpellAbility roomSA = dungeon.getTriggers().get(0).getOverridingAbility();
            nextRoom = roomSA.getParam("RoomName");
        } else {
            nextRoom = chooseNextRoom(sa, player, dungeon, room);
        }

        dungeon.setCurrentRoom(nextRoom);
        // TODO: Currently play the Add Counter sound, but maybe add soundeffect for marker?
        game.fireEvent(new GameEventCardCounters(dungeon, CounterType.getType("LEVEL"), 0, 1));

        // Run RoomEntered trigger
        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(dungeon);
        runParams.put(AbilityKey.RoomName, nextRoom);
        game.getTriggerHandler().runTrigger(TriggerType.RoomEntered, runParams, false);

        player.incrementVenturedThisTurn();
    }

    @Override
    public void resolve(SpellAbility sa) {
        for (final Player p : getTargetPlayers(sa)) {
            if (!sa.usesTargeting() || p.canBeTargetedBy(sa)) {
                ventureIntoDungeon(sa, p);
            }
        }
    }

}
