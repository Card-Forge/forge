package forge.game.ability.effects;

import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.StaticData;
import forge.card.CardStateName;
import forge.card.ICardFace;
import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
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
            switch (sa.getParamOrDefault("Mode", "ThisDoor")) {          
            case "ThisDoor":
                c.unlockRoom(activator, sa.getCardStateName());
                break;
            case "Unlock":
                //List<ICardFace> faces = c.getLockedRoomNames().stream().map(face -> StaticData.instance().getCommonCards().getFaceByName(face)).collect(Collectors.toList());

                Map<ICardFace, CardStateName> map = Maps.newHashMap();
                
                for (CardStateName faceStateName : c.getLockedRooms()) {
                    if (!c.hasState(faceStateName)) {
                        continue;
                    }
                    String faceName = c.getState(faceStateName).getName();
                    ICardFace face = StaticData.instance().getCommonCards().getFaceByName(faceName);
                    map.put(face, faceStateName);
                }

                // need to choose Room Name
                ICardFace chosen = activator.getController().chooseSingleCardFace(sa, Lists.newArrayList(map.keySet()), "Choose Room to unlock");
                if (chosen == null) {
                    continue;
                }
                c.unlockRoom(activator, map.get(chosen));
                break;
            }
        }
    }

}
