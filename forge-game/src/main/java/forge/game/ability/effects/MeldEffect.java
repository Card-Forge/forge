package forge.game.ability.effects;

import forge.card.CardStateName;
import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.event.GameEventCombatChanged;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.PlayerZoneBattlefield;
import forge.game.zone.ZoneType;
import forge.util.Localizer;
import java.util.Arrays;

public class MeldEffect extends SpellAbilityEffect {
    @Override
    public void resolve(SpellAbility sa) {
        Card hostCard = sa.getHostCard();
        String primName = sa.getParam("Primary");
        String secName = sa.getParam("Secondary");
        Game game = hostCard.getGame();
        Player controller = sa.getActivatingPlayer();

        // a permanent you control and own named secondary
        CardCollection field = CardLists.filter(
                controller.getCardsIn(ZoneType.Battlefield),
                CardPredicates.isOwner(controller),
                CardPredicates.nameEquals(secName));
        field = CardLists.getType(field, sa.getParamOrDefault("SecondaryType", "Creature"));
        if (field.isEmpty()) {
            return;
        }

        Card secondary = controller.getController().chooseSingleEntityForEffect(field, sa, Localizer.getInstance().getMessage("lblChooseCardToMeld"), null);

        CardCollection exiled = new CardCollection(Arrays.asList(hostCard, secondary));
        exiled = game.getAction().exile(exiled, sa, null);
        Card primary = exiled.get(0);
        secondary = exiled.get(1);

        // cards has wrong name in exile
        if (!primary.sharesNameWith(primName) || !secondary.sharesNameWith(secName)) {
            return;
        }

        for (Card c : exiled) {
            if (c.isToken() || c.getCloneOrigin() != null) {
                // Neither of these things
                return;
            } else if (!c.isInZone(ZoneType.Exile)) {
                return;
            }
        }

        if (sa.hasParam("Tapped")) {
            primary.setTapped(true);
        }

        primary.changeToState(CardStateName.Meld);
        primary.setBackSide(true);
        primary.setMeldedWith(secondary);
        PlayerZoneBattlefield bf = (PlayerZoneBattlefield)controller.getZone(ZoneType.Battlefield);
        bf.addToMelded(secondary);
        Card movedCard = game.getAction().changeZone(primary.getZone(), bf, primary, 0, sa);
        if (addToCombat(movedCard, movedCard.getController(), sa, "Attacking", "Blocking")) {
            game.updateCombatForView();
            game.fireEvent(new GameEventCombatChanged());
        }
    }
}
