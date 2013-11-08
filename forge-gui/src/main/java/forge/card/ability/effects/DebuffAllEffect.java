package forge.card.ability.effects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import forge.Card;
import forge.CardLists;
import forge.Command;
import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.SpellAbility;
import forge.game.Game;
import forge.game.zone.ZoneType;

public class DebuffAllEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        if (sa.hasParam("DebuffAllDescription")) {
            return sa.getParam("DebuffAllDescription");
        }

        return "";
    } // debuffAllStackDescription()

    /**
     * <p>
     * debuffAllResolve.
     * </p>
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param af
     *            a {@link forge.card.ability.AbilityFactory} object.
     */

    @Override
    public void resolve(SpellAbility sa) {
        final Card hostCard = sa.getSourceCard();
        final List<String> kws = sa.hasParam("Keywords") ? Arrays.asList(sa.getParam("Keywords").split(" & ")) : new ArrayList<String>();
        final Game game = sa.getActivatingPlayer().getGame();

        String valid = "";

        if (sa.hasParam("ValidCards")) {
            valid = sa.getParam("ValidCards");
        }

        List<Card> list = game.getCardsIn(ZoneType.Battlefield);
        list = CardLists.getValidCards(list, valid.split(","), hostCard.getController(), hostCard);

        for (final Card tgtC : list) {
            final ArrayList<String> hadIntrinsic = new ArrayList<String>();
            if (tgtC.isInPlay() && tgtC.canBeTargetedBy(sa)) {
                for (final String kw : kws) {
                    if (tgtC.getIntrinsicKeyword().contains(kw)) {
                        hadIntrinsic.add(kw);
                    }
                    tgtC.removeIntrinsicKeyword(kw);
                    tgtC.removeExtrinsicKeyword(kw);
                }
            }
            if (!sa.hasParam("Permanent")) {
                game.getEndOfTurn().addUntil(new Command() {
                    private static final long serialVersionUID = 7486231071095628674L;

                    @Override
                    public void run() {
                        if (tgtC.isInPlay()) {
                            for (final String kw : hadIntrinsic) {
                                tgtC.addIntrinsicKeyword(kw);
                            }
                        }
                    }
                });
            }
        }
    } // debuffAllResolve()

}
