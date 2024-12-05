package forge.game.ability.effects;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.card.CardStateName;
import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardState;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Localizer;

public class UnlockDoorEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card source = sa.getHostCard();
        final Game game = source.getGame();
        final Player activator = sa.getActivatingPlayer();

        CardCollection list;

        if (sa.hasParam("Choices")) {
            Player chooser = activator;
            String title = sa.hasParam("ChoiceTitle") ? sa.getParam("ChoiceTitle") : Localizer.getInstance().getMessage("lblChoose") + " ";

            CardCollection choices = CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield), sa.getParam("Choices"), activator, source, sa);

            Card c = chooser.getController().chooseSingleEntityForEffect(choices, sa, title, Maps.newHashMap());
            if (c == null) {
                return;
            }
            list = new CardCollection(c);
        } else {
            list = getTargetCards(sa);
        }

        for (Card c : list) {
            Map<String, Object> params = Maps.newHashMap();
            params.put("Object", c);
            switch (sa.getParamOrDefault("Mode", "ThisDoor")) {          
            case "ThisDoor":
                c.unlockRoom(activator, sa.getCardStateName());
                break;
            case "Unlock":
                List<CardState> states = c.getLockedRooms().stream().map(c::getState).collect(Collectors.toList());

                // need to choose Room Name
                CardState chosen = activator.getController().chooseSingleCardState(sa, states, "Choose Room to unlock", params);
                if (chosen == null) {
                    continue;
                }
                c.unlockRoom(activator, chosen.getStateName());
                break;
            case "LockOrUnlock":
                switch (c.getLockedRooms().size()) {
                case 0:
                    // no locked, all unlocked, can only lock door
                    List<CardState> unlockStates = c.getUnlockedRooms().stream().map(c::getState).collect(Collectors.toList());
                    CardState chosenUnlock = activator.getController().chooseSingleCardState(sa, unlockStates, "Choose Room to lock", params);
                    if (chosenUnlock == null) {
                        continue;
                    }
                    c.lockRoom(activator, chosenUnlock.getStateName());
                    break;
                case 1:
                    // TODO check for Lock vs Unlock first?
                    List<CardState> bothStates = Lists.newArrayList();
                    bothStates.add(c.getState(CardStateName.LeftSplit));
                    bothStates.add(c.getState(CardStateName.RightSplit));
                    CardState chosenBoth = activator.getController().chooseSingleCardState(sa, bothStates, "Choose Room to lock or unlock", params);
                    if (chosenBoth == null) {
                        continue;
                    }
                    if (c.getLockedRooms().contains(chosenBoth.getStateName())) {
                        c.unlockRoom(activator, chosenBoth.getStateName());
                    } else {
                        c.lockRoom(activator, chosenBoth.getStateName());
                    }
                    break;
                case 2:
                    List<CardState> lockStates = c.getLockedRooms().stream().map(c::getState).collect(Collectors.toList());

                    // need to choose Room Name
                    CardState chosenLock = activator.getController().chooseSingleCardState(sa, lockStates, "Choose Room to unlock", params);
                    if (chosenLock == null) {
                        continue;
                    }
                    c.unlockRoom(activator, chosenLock.getStateName());
                    break;
                }
                break;
            }
        }
    }

}
