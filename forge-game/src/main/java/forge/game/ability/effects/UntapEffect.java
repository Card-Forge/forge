package forge.game.ability.effects;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates.Presets;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;

import org.apache.commons.lang3.StringUtils;

import java.util.List;


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
            untapChoose(sa, false);
        } else if (sa.hasParam("UntapExactly")) {
            untapChoose(sa, true);
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
     * Choose cards to untap.
     * </p>
     * 
     * @param sa
     *            a {@link SpellAbility}.
     * @param mandatory
     *            whether the untapping is mandatory.
     */
    private static void untapChoose(final SpellAbility sa, final boolean mandatory) {
        final int num = Integer.parseInt(sa.getParam("Amount"));
        final String valid = sa.getParam("UntapType");

        final List<Player> definedPlayers = AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("Defined"), sa);

        for (final Player p : definedPlayers) {
            CardCollectionView list = CardLists.getValidCards(p.getGame().getCardsIn(ZoneType.Battlefield),
                    valid, sa.getActivatingPlayer(), sa.getHostCard());
            list = CardLists.filter(list, Presets.TAPPED);

            final CardCollectionView selected = p.getController().chooseCardsForEffect(list, sa, "Select cards to untap", mandatory ? num : 0, num, !mandatory);
            if (selected != null) {
                for (final Card c : selected) { 
                    c.untap();
                }
            }
        }
    }

}
