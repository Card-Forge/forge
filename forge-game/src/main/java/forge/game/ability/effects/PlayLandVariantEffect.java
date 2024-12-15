package forge.game.ability.effects;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Lists;

import forge.StaticData;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardFactory;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.item.PaperCard;
import forge.item.PaperCardPredicates;
import forge.util.Aggregates;

public class PlayLandVariantEffect extends SpellAbilityEffect {

    @Override
    public void resolve(final SpellAbility sa) {
        final Card source = sa.getHostCard();
        final Player activator = sa.getActivatingPlayer();
        final Game game = source.getGame();
        final String landType = sa.getParam("Clone");
        Stream<PaperCard> cardStream = StaticData.instance().getCommonCards().streamUniqueCards();
        if ("BasicLand".equals(landType)) {
            cardStream = cardStream.filter(PaperCardPredicates.IS_BASIC_LAND);
        }
        // current color of source card
        final ColorSet color = source.getColor();
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

        cardStream = cardStream.filter(x -> landNames.contains(x.getName()));
        List<PaperCard> cards = cardStream.collect(Collectors.toList());
        // get a random basic land
        Card random;
        // if activator cannot play the random land, loop
        do {
            if (cards.isEmpty()) return;
            PaperCard ran = Aggregates.random(cards);
            random = CardFactory.getCard(ran, activator, game);
            cards.remove(ran);
        } while (!activator.canPlayLand(random, false, random.getFirstSpellAbility()));

        source.addCloneState(CardFactory.getCloneStates(random, source, sa), game.getNextTimestamp());
        source.updateStateForView();

        activator.playLandNoCheck(source, sa);
    }
}
