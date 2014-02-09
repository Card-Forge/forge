package forge.game.ability.effects;

import forge.Command;
import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardShields;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.List;

public class RegenerateAllEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return "Regenerate all valid cards.";
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card hostCard = sa.getHostCard();
        final Game game = hostCard.getGame();
        String valid = "";

        if (sa.hasParam("ValidCards")) {
            valid = sa.getParam("ValidCards");
        }

        List<Card> list = game.getCardsIn(ZoneType.Battlefield);
        list = CardLists.getValidCards(list, valid.split(","), hostCard.getController(), hostCard);

        for (final Card c : list) {
            final Command untilEOT = new Command() {
                private static final long serialVersionUID = 259368227093961103L;

                @Override
                public void run() {
                    c.resetShield();
                }
            };

            if (c.isInPlay()) {
                c.addShield(new CardShields(sa, null));
                game.getEndOfTurn().addUntil(untilEOT);
            }
        }
    } // regenerateAllResolve

}
