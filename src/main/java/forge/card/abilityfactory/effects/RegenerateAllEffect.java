package forge.card.abilityfactory.effects;

import java.util.List;

import forge.Card;
import forge.CardLists;
import forge.Command;
import forge.Singletons;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class RegenerateAllEffect extends SpellEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        return "Regenerate all valid cards.";
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card hostCard = sa.getSourceCard();
        String valid = "";

        if (sa.hasParam("ValidCards")) {
            valid = sa.getParam("ValidCards");
        }

        List<Card> list = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
        list = CardLists.getValidCards(list, valid.split(","), hostCard.getController(), hostCard);

        for (final Card c : list) {
            final Command untilEOT = new Command() {
                private static final long serialVersionUID = 259368227093961103L;

                @Override
                public void execute() {
                    c.resetShield();
                }
            };

            if (c.isInPlay()) {
                c.addShield();
                Singletons.getModel().getGame().getEndOfTurn().addUntil(untilEOT);
            }
        }
    } // regenerateAllResolve

} 