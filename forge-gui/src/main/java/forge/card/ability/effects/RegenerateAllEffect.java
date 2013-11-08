package forge.card.ability.effects;

import java.util.List;

import forge.Card;
import forge.CardLists;
import forge.Command;
import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.SpellAbility;
import forge.game.Game;
import forge.game.zone.ZoneType;

public class RegenerateAllEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return "Regenerate all valid cards.";
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card hostCard = sa.getSourceCard();
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
                c.addShield();
                game.getEndOfTurn().addUntil(untilEOT);
            }
        }
    } // regenerateAllResolve

}
