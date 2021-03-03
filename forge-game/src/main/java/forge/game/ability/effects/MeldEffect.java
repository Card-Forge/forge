package forge.game.ability.effects;

import com.google.common.collect.Lists;

import forge.card.CardStateName;
import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.PlayerZoneBattlefield;
import forge.game.zone.ZoneType;
import forge.util.Localizer;

public class MeldEffect extends SpellAbilityEffect {
    @Override
    public void resolve(SpellAbility sa) {
        Card hostCard = sa.getHostCard();
        String primName = sa.getParam("Primary");
        String secName = sa.getParam("Secondary");
        Game game = hostCard.getGame();
        Player controller = sa.getActivatingPlayer();

        Card primary = game.getAction().exile(hostCard, sa);

        // a creature you control and own named secondary
        CardCollection field = CardLists.filter(
                controller.getCreaturesInPlay(),
                CardPredicates.isOwner(controller),
                CardPredicates.nameEquals(secName));

        if (field.isEmpty()) {
            return;
        }

        Card secondary = controller.getController().chooseSingleEntityForEffect(field, sa, Localizer.getInstance().getMessage("lblChooseCardToMeld"), null);

        secondary = game.getAction().exile(secondary, sa);

        // cards has wrong name in exile
        if (!primary.sharesNameWith(primName) || !secondary.sharesNameWith(secName)) {
            return;
        }

        for(Card c : Lists.newArrayList(primary, secondary)) {
            if (c.isToken() || c.getCloneOrigin() != null) {
                // Neither of these things
                return;
            } else if (!c.isInZone(ZoneType.Exile)) {
                return;
            }
        }

        primary.changeToState(CardStateName.Meld);
        primary.setMeldedWith(secondary);
        PlayerZoneBattlefield bf = (PlayerZoneBattlefield)controller.getZone(ZoneType.Battlefield);
        game.getAction().changeZone(primary.getZone(), bf, primary, 0, sa);
        bf.addToMelded(secondary);
    }
}
