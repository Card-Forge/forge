package forge.card.staticability;

import java.util.HashMap;

import forge.Card;

/**
 * The Class StaticAbility_CantBeCast.
 */
public class StaticAbilityETBTapped {

    /**
     * TODO Write javadoc for this method.
     * 
     * @param stAb
     *            a StaticAbility
     * @param card
     *            the card
     * @param activator
     *            the activator
     * @return true, if successful
     */
    public static boolean applyETBTappedAbility(final StaticAbility stAb, final Card card) {
        final HashMap<String, String> params = stAb.getMapParams();
        final Card hostCard = stAb.getHostCard();

        if (params.containsKey("ValidCard")
                && !card.isValid(params.get("ValidCard").split(","), hostCard.getController(), hostCard)) {
            return false;
        }

        return true;
    }

}
