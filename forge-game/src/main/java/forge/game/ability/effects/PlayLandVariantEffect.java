package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.StaticData;
import forge.card.CardCharacteristicName;
import forge.card.CardRulesPredicates;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardFactory;
import forge.game.card.CardUtil;
import forge.game.event.GameEventLandPlayed;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.Aggregates;

public class PlayLandVariantEffect extends SpellAbilityEffect {


    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        Card source = sa.getHostCard();
        Player activator = sa.getActivatingPlayer();
        final Game game = source.getGame();
        final String landType = sa.getParam("Clone");
        List<PaperCard> cards = Lists.newArrayList(StaticData.instance().getCommonCards().getUniqueCards());
        if ("BasicLand".equals(landType)) {
            Predicate<PaperCard> cpp = Predicates.compose(CardRulesPredicates.Presets.IS_BASIC_LAND, PaperCard.FN_GET_RULES);
            cards = Lists.newArrayList(Iterables.filter(cards, cpp));
        }
        // current color of source card
        ColorSet color = CardUtil.getColors(source);
        if (color.isColorless()) {
            return;
        }
        // find basic lands that can produce mana of one of the card's colors
        final List<String> landnames = new ArrayList<String>();
        for (byte i = 0; i < MagicColor.WUBRG.length; i++) {
            if (color.hasAnyColor(MagicColor.WUBRG[i])) {
                landnames.add(MagicColor.Constant.BASIC_LANDS.get(i));
            }
        }
        Predicate<PaperCard> cp = Predicates.compose(new Predicate<String>() {
            @Override
            public boolean apply(final String name) {
                return landnames.contains(name);
            }
        }, PaperCard.FN_GET_NAME);
        cards = Lists.newArrayList(Iterables.filter(cards, cp));
        // get a random basic land
        PaperCard ran = Aggregates.random(cards);
        Card random = CardFactory.getCard(ran, activator);
        // if activator cannot play the random land, loop
        while (!activator.canPlayLand(random, false) && !cards.isEmpty()) {
            cards.remove(ran);
            if (cards.isEmpty()) return;
            ran = Aggregates.random(cards);
            random = CardFactory.getCard(ran, activator);
        }

        String imageFileName = game.getRules().canCloneUseTargetsImage ? source.getImageKey() : random.getImageKey();
        source.addAlternateState(CardCharacteristicName.Cloner);
        source.switchStates(CardCharacteristicName.Original, CardCharacteristicName.Cloner);
        source.setState(CardCharacteristicName.Original);
        CardCharacteristicName stateToCopy = random.getCurState();
        CardFactory.copyState(random, stateToCopy, source, source.getCurState());
        source.setImageKey(imageFileName);
        
        source.setController(activator, 0);
        game.getAction().moveTo(activator.getZone(ZoneType.Battlefield), source);

        // play a sound
        game.fireEvent(new GameEventLandPlayed(activator, source));
        
        // Run triggers
        final HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("Card", source);
        game.getTriggerHandler().runTrigger(TriggerType.LandPlayed, runParams, false);
        game.getStack().unfreezeStack();
        activator.setNumLandsPlayed(activator.getNumLandsPlayed() + 1);


    }

}
