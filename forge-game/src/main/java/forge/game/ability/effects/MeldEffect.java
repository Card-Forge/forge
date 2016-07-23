package forge.game.ability.effects;

import forge.card.CardStateName;
import forge.game.Game;
import forge.game.GameAction;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.PlayerZone;
import forge.game.zone.PlayerZoneBattlefield;
import forge.game.zone.ZoneType;

public class MeldEffect extends SpellAbilityEffect {
    @Override
    public void resolve(SpellAbility sa) {
        Card hostCard = sa.getHostCard();
        String name = sa.getParam("Name");
        String primName = sa.getParam("Primary");
        String secName = sa.getParam("Secondary");
        Game game = hostCard.getGame();
        Player controller = sa.getActivatingPlayer();
        CardCollection list = AbilityUtils.getDefinedCards(hostCard, sa.getParam("Defined"), sa);

        if (list.size() < 2) {
            // Haven't found enough cards to Meld, leave Melding choices in their current zone
            return;
        }

        Card primary = null;
        Card secondary = null;

        for(Card c : list) {
            if (c.isToken() || c.getCloneOrigin() != null) {
                // Neither of these things
                return;
            }
            if (primName.equals(c.getName())) {
                primary = c;
            } else if (secName.equals(c.getName())) {
                secondary = c;
            } else {
                return;
            }
        }



        primary.changeToState(CardStateName.Meld);
        PlayerZoneBattlefield bf = (PlayerZoneBattlefield)controller.getZone(ZoneType.Battlefield);
        Card melded = game.getAction().changeZone(primary.getZone(), bf, primary, 0);
        bf.addToMelded(secondary);
        melded.setMeldedWith(secondary);
    }
}
