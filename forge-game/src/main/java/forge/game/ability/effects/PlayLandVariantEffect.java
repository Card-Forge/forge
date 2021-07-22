package forge.game.ability.effects;

import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.StaticData;
import forge.card.CardRulesPredicates;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardFactory;
import forge.game.card.CardUtil;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.item.PaperCard;
import forge.util.Aggregates;

public class PlayLandVariantEffect extends SpellAbilityEffect {

    @Override
    public void resolve(final SpellAbility sa) {
        final Card source = sa.getHostCard();
        final Player activator = sa.getActivatingPlayer();
        final Game game = source.getGame();
        final String landType = sa.getParam("Clone");
        List<PaperCard> cards = Lists.newArrayList(StaticData.instance().getCommonCards().getUniqueCards());
        if ("BasicLand".equals(landType)) {
            final Predicate<PaperCard> cpp = Predicates.compose(CardRulesPredicates.Presets.IS_BASIC_LAND, PaperCard.FN_GET_RULES);
            cards = Lists.newArrayList(Iterables.filter(cards, cpp));
        }
        // current color of source card
        final ColorSet color = CardUtil.getColors(source);
        if (color.isColorless()) {
            return;
        }
        // find basic lands that can produce mana of one of the card's colors
        final List<String> landNames = Lists.newArrayList();
        for (byte i = 0; i < MagicColor.WUBRG.length; i++) {
            if (color.hasAnyColor(MagicColor.WUBRG[i])) {
                landNames.add(MagicColor.Constant.BASIC_LANDS.get(i));
                landNames.add(MagicColor.Constant.SNOW_LANDS.get(i));
            }
        }

        final Predicate<PaperCard> cp = Predicates.compose(new Predicate<String>() {
            @Override
            public boolean apply(final String name) {
                return landNames.contains(name);
            }
        }, PaperCard.FN_GET_NAME);
        cards = Lists.newArrayList(Iterables.filter(cards, cp));
        // get a random basic land
        Card random;
        // if activator cannot play the random land, loop
        do {
            if (cards.isEmpty()) return;
            PaperCard ran = Aggregates.random(cards);
            random = CardFactory.getCard(ran, activator, game);
            cards.remove(ran);
        } while (!activator.canPlayLand(random, false));

        source.addCloneState(CardFactory.getCloneStates(random, source, sa), game.getNextTimestamp());
        source.updateStateForView();

        activator.playLandNoCheck(source, sa);
    }
}
