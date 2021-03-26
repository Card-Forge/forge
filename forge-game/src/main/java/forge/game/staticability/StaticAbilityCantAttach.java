package forge.game.staticability;

import forge.game.GameEntity;
import forge.game.card.Card;

public class StaticAbilityCantAttach {

   public static boolean applyCantAttachAbility(final StaticAbility stAb, final Card card, final GameEntity target) {
       if (!stAb.matchesValidParam("ValidCard", card)) {
           return false;
       }

       if (!stAb.matchesValidParam("Target", target)) {
           return false;
       }

       if (stAb.hasParam("ValidCardToTarget")) {
           if (!(target instanceof Card)) {
               return false;
           }
           Card tcard = (Card) target;

           if (!stAb.matchesValid(card, stAb.getParam("ValidCardToTarget").split(","), tcard)) {
               return false;
           }
       }

       return true;
    }
}
