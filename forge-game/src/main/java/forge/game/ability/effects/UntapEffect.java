package forge.game.ability.effects;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.card.CardPredicates.Presets;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Lang;
import forge.util.Localizer;

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
            sb.append(Lang.joinHomogenous(getTargetCards(sa)));
        }
        sb.append(".");
        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        if (sa.hasParam("UntapUpTo")) {
            untapChoose(sa, false);
        } else if (sa.hasParam("UntapExactly")) {
            untapChoose(sa, true);
        } else {
            final CardCollection untargetedCards = CardUtil.getRadiance(sa);
            for (final Card tgtC : getTargetCards(sa)) {
                if (sa.usesTargeting() && !tgtC.canBeTargetedBy(sa)) {
                    continue;
                }
                if (tgtC.isInPlay()) {
                    tgtC.untap();
                }
                if (sa.hasParam("ETB")) {
                    // do not fire triggers
                    tgtC.setTapped(false);
                }
            }
            for (final Card tgtC : untargetedCards) {
                if (tgtC.isInPlay()) {
                    tgtC.untap();
                }
                if (sa.hasParam("ETB")) {
                    // do not fire triggers
                    tgtC.setTapped(false);
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

        for (final Player p : AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("Defined"), sa)) {
            CardCollectionView list = CardLists.getValidCards(p.getGame().getCardsIn(ZoneType.Battlefield),
                    valid, sa.getActivatingPlayer(), sa.getHostCard(), sa);
            list = CardLists.filter(list, Presets.TAPPED);

            final CardCollectionView selected = p.getController().chooseCardsForEffect(list, sa, Localizer.getInstance().getMessage("lblSelectCardToUntap"), mandatory ? num : 0, num, !mandatory, null);
            if (selected != null) {
                for (final Card c : selected) { 
                    c.untap();
                }
            }
        }
    }

}
