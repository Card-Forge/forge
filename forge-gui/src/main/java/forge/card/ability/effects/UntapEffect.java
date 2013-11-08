package forge.card.ability.effects;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import forge.Card;
import forge.CardLists;
import forge.Singletons;
import forge.CardPredicates.Presets;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.TargetRestrictions;
import forge.game.ai.ComputerUtilCard;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.input.InputSelectCards;
import forge.gui.input.InputSelectCardsFromList;

public class UntapEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        // when getStackDesc is called, just build exactly what is happening
        final StringBuilder sb = new StringBuilder();

        sb.append("Untap ");

        if (sa.hasParam("UntapUpTo")) {
            sb.append("up to ").append(sa.getParam("Amount")).append(" ");
            sb.append(sa.getParam("UntapType")).append("s");
        } else {
            List<Card> tgtCards = getTargetCards(sa);
            sb.append(StringUtils.join(tgtCards, ", "));
        }
        sb.append(".");
        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final TargetRestrictions tgt = sa.getTargetRestrictions();

        if (sa.hasParam("UntapUpTo")) {
            untapChooseUpTo(sa);
        } else {

            final List<Card> tgtCards = getTargetCards(sa);

            for (final Card tgtC : tgtCards) {
                if (tgtC.isInPlay() && ((tgt == null) || tgtC.canBeTargetedBy(sa))) {
                    tgtC.untap();
                }
            }
        }
    }

    /**
     * <p>
     * untapChooseUpTo.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.ability.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param sa
     *            a {@link java.util.HashMap} object.
     */
    private void untapChooseUpTo(final SpellAbility sa) {
        final int num = Integer.parseInt(sa.getParam("Amount"));
        final String valid = sa.getParam("UntapType");

        final List<Player> definedPlayers = AbilityUtils.getDefinedPlayers(sa.getSourceCard(), sa.getParam("Defined"), sa);

        for (final Player p : definedPlayers) {
            List<Card> list = CardLists.getType(p.getCardsIn(ZoneType.Battlefield), valid);
            list = CardLists.filter(list, Presets.TAPPED);
            
            if (p.isHuman()) {
                InputSelectCards sc = new InputSelectCardsFromList(0, num, list);
                Singletons.getControl().getInputQueue().setInputAndWait(sc);
                if( !sc.hasCancelled() )
                    for( Card c : sc.getSelected() ) 
                        c.untap();
            } else {
                for (int count = 0; !list.isEmpty() && count < num; count++) {
                    final Card c = ComputerUtilCard.getBestLandAI(list);
                    c.untap();
                    list.remove(c);
                }
            }
        }
    }

}
