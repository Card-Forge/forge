package forge.game.ability.effects;

import forge.game.EvenOdd;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.player.PlayerController.BinaryChoiceType;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;
import forge.util.Localizer;

public class ChooseEvenOddEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        sb.append(Lang.joinHomogenous(getTargetPlayers(sa)));
        sb.append("chooses even or odd.");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getHostCard();

        for (final Player p : getTargetPlayers(sa)) {
            if (!p.isInGame()) {
                continue;
            }
            EvenOdd chosen = p.getController().chooseBinary(sa, "odd or even", BinaryChoiceType.OddsOrEvens) ? EvenOdd.Odd : EvenOdd.Even;
            card.setChosenEvenOdd(chosen);
            if (sa.hasParam("Notify")) {
                p.getGame().getAction().notifyOfValue(sa, card, Localizer.getInstance().getMessage("lblPlayerPickedChosen", p.getName(), chosen), p);
            }
        }
        card.updateStateForView();
    }
}