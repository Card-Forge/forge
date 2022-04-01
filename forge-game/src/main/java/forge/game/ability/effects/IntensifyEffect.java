package forge.game.ability.effects;

import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;

public class IntensifyEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        String these = sa.hasParam("DefinedDesc") ? sa.getParam("DefinedDesc") :
                Lang.joinHomogenous(getDefinedCardsOrTargeted(sa));
        final int amount = AbilityUtils.calculateAmount(sa.getHostCard(),
                sa.getParamOrDefault("Amount", "1"), sa);

        sb.append(sa.getActivatingPlayer()).append(" perpetually increases the intensity of ").append(these);
        sb.append(" by ").append(amount).append(".");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final int amount = AbilityUtils.calculateAmount(sa.getHostCard(),
                sa.getParamOrDefault("Amount", "1"), sa);

        CardCollectionView toIntensify;
        if (sa.hasParam("AllDefined")) {
            toIntensify = CardLists.getValidCards(host.getGame().getCardsInGame(), sa.getParam("AllDefined"),
                        sa.getActivatingPlayer(), host, sa);
        } else {
            toIntensify = getDefinedCardsOrTargeted(sa);
        }

        for (final Card tgtC : toIntensify) {
            tgtC.addIntensity(amount);
        }
    }
}
