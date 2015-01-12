package forge.game.ability.effects;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;

public class ManifestEffect extends SpellAbilityEffect {
    @Override
    public void resolve(SpellAbility sa) {
        final Card source = sa.getHostCard();
        // Usually a number leaving possibility for X, Sacrifice X land: Manifest X creatures.
        final int amount = sa.hasParam("Amount") ? AbilityUtils.calculateAmount(sa.getHostCard(),
                sa.getParam("Amount"), sa) : 1;
        // Most commonly "defined" is Top of Library
        final String defined = sa.hasParam("Defined") ? sa.getParam("Defined") : "TopOfLibrary";

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        for (final Player p : getTargetPlayers(sa, "DefinedPlayer")) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                CardCollection tgtCards;
                if ("TopOfLibrary".equals(defined)) {
                    tgtCards = p.getTopXCardsFromLibrary(amount);
                } else {
                    tgtCards = getTargetCards(sa);
                }

                if (sa.hasParam("Shuffle")) {
                    CardLists.shuffle(tgtCards);
                }

                for(Card c : tgtCards) {
                    Card rem = c.manifest(p);
                    if (sa.hasParam("RememberManifested") && rem != null) {
                        source.addRemembered(rem);
                    }
                }
            }
        }
    }
}
