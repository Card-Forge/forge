package forge.game.ability.effects;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Lang;
import forge.util.Localizer;

public class TapEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getHostCard();
        final boolean remTapped = sa.hasParam("RememberTapped");
        final boolean alwaysRem = sa.hasParam("AlwaysRemember");
        if (remTapped) {
            card.clearRemembered();
        }

        Iterable<Card> toTap;

        if (sa.hasParam("CardChoices")) { // choosing outside Defined/Targeted
            final Player activator = sa.getActivatingPlayer();
            CardCollection choices = CardLists.getValidCards(card.getGame().getCardsIn(ZoneType.Battlefield), sa.getParam("CardChoices"), activator, card, sa);
            int n = sa.hasParam("ChoiceAmount") ?
                    AbilityUtils.calculateAmount(card, sa.getParam("ChoiceAmount"), sa) : 1;
            int min = sa.hasParam("AnyNumber") ? 0 : n;
            final String prompt = sa.hasParam("ChoicePrompt") ? sa.getParam("ChoicePrompt") :
                    Localizer.getInstance().getMessage("lblChoosePermanentstoTap");
            toTap = activator.getController().chooseEntitiesForEffect(choices, min, n, null, sa, prompt, null, null);
        } else {
            toTap = getTargetCards(sa);
        }

        for (final Card tgtC : toTap) {
            if (tgtC.isPhasedOut()) {
                continue;
            }
            if (tgtC.isInPlay()) {
                if (tgtC.isUntapped() && remTapped || alwaysRem) {
                    card.addRemembered(tgtC);
                }
                tgtC.tap(true);
            }
            if (sa.hasParam("ETB")) {
                // do not fire Taps triggers
                tgtC.setTapped(true);
            }
        }
    }

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        sb.append("Tap ");
        sb.append(Lang.joinHomogenous(getTargetCards(sa)));
        sb.append(".");
        return sb.toString();
    }

}
