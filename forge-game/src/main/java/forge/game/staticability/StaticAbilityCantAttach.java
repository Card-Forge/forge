package forge.game.staticability;

import forge.game.GameEntity;
import forge.game.card.Card;

public class StaticAbilityCantAttach {

   public static boolean applyCantAttachAbility(final StaticAbility stAb, final Card card, final GameEntity target) {
       final Card hostCard = stAb.getHostCard();

       if (stAb.hasParam("ValidCard")
               && !card.isValid(stAb.getParam("ValidCard").split(","), hostCard.getController(), hostCard, null)) {
           return false;
       }

       if (stAb.hasParam("Target")
               && !target.isValid(stAb.getParam("Target").split(","), hostCard.getController(), hostCard, null)) {
           return false;
       }

       if (stAb.hasParam("ValidCardToTarget")) {
           if (!(target instanceof Card)) {
               return false;
           }
           Card tcard = (Card) target;

           if (!card.isValid(stAb.getParam("ValidCardToTarget").split(","), tcard.getController(), tcard, null)) {
               return false;
           }
       }

       return true;
    }
}
