package forge.card.ability.effects;

import java.util.List;
import org.apache.commons.lang3.StringUtils;

import forge.Card;
import forge.CardLists;
import forge.Singletons;
import forge.CardPredicates.Presets;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityEffect;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

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
        final Target tgt = sa.getTarget();

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
            if (p.isHuman()) {
                Singletons.getModel().getMatch().getInput().setInput(CardFactoryUtil.inputUntapUpToNType(num, valid));
            } else {
                List<Card> list = p.getCardsIn(ZoneType.Battlefield);
                list = CardLists.getType(list, valid);
                list = CardLists.filter(list, Presets.TAPPED);

                int count = 0;
                while ((list.size() != 0) && (count < num)) {
                    for (int i = 0; (i < list.size()) && (count < num); i++) {

                        final Card c = CardFactoryUtil.getBestLandAI(list);
                        c.untap();
                        list.remove(c);
                        count++;
                    }
                }
            }
        }
    }

}
