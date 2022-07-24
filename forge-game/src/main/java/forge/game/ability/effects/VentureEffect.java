package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.base.Predicates;

import forge.StaticData;
import forge.card.CardRulesPredicates;
import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CounterType;
import forge.game.event.GameEventCardCounters;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityCantVenture;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.Localizer;
import forge.util.PredicateString.StringOp;

public class VentureEffect  extends SpellAbilityEffect {

    private Card getDungeonCard(SpellAbility sa, Player player, Map<AbilityKey, Object> moveParams) {
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

        List<PaperCard> dungeonCards = null;
        if (sa.hasParam("Dungeon")) {
            dungeonCards = StaticData.instance().getVariantCards()
                    .getAllCards(Predicates.compose(
                            Predicates.and(CardRulesPredicates.Presets.IS_DUNGEON,
                                    CardRulesPredicates.subType(StringOp.EQUALS, sa.getParam("Dungeon"))),
                            PaperCard.FN_GET_RULES));
        } else {
            // Create a new dungeon card chosen by player in command zone.
            dungeonCards = StaticData.instance().getVariantCards().getAllCards(
                Predicates.compose(CardRulesPredicates.Presets.IS_DUNGEON, PaperCard.FN_GET_RULES));
            dungeonCards.removeIf(c -> !c.getRules().isEnterableDungeon());
        }
        String message = Localizer.getInstance().getMessage("lblChooseDungeon");
        Card dungeon = player.getController().chooseDungeon(player, dungeonCards, message);

        game.getTriggerHandler().suppressMode(TriggerType.ChangesZone);
        game.getAction().moveTo(ZoneType.Command, dungeon, sa, moveParams);
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

    private void ventureIntoDungeon(SpellAbility sa, Player player, Map<AbilityKey, Object> moveParams) {
        if (StaticAbilityCantVenture.cantVenture(player)) {
            return;
        }

        final Game game = player.getGame();
        Card dungeon = getDungeonCard(sa, player, moveParams);
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
        Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
        moveParams.put(AbilityKey.LastStateBattlefield, sa.getLastStateBattlefield());
        moveParams.put(AbilityKey.LastStateGraveyard, sa.getLastStateGraveyard());

        for (final Player p : getTargetPlayers(sa)) {
            if (!sa.usesTargeting() || p.canBeTargetedBy(sa)) {
                ventureIntoDungeon(sa, p, moveParams);
            }
        }
    }

}
