package forge.game.ability.effects;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.StaticData;
import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.item.PaperCard;
import forge.util.Aggregates;
import forge.util.ComparableOp;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class ChooseCardNameEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        for (final Player p : getTargetPlayers(sa)) {
            sb.append(p).append(" ");
        }
        sb.append("names a card.");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final List<Player> tgtPlayers = getTargetPlayers(sa);

        String valid = "Card";
        String validDesc = "card";
        if (sa.hasParam("ValidCards")) {
            valid = sa.getParam("ValidCards");
            validDesc = sa.getParam("ValidDesc");
        }

        boolean randomChoice = sa.hasParam("AtRandom");
        boolean chooseFromDefined = sa.hasParam("ChooseFromDefinedCards");
        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                String chosen = "";

                if (randomChoice) {
                    // Currently only used for Momir Avatar, if something else gets added here, make it more generic
                    Predicate<CardRules> baseRule = CardRulesPredicates.Presets.IS_CREATURE;

                    String numericAmount = "X";
                    final int validAmount = StringUtils.isNumeric(numericAmount) ? Integer.parseInt(numericAmount) :
                        AbilityUtils.calculateAmount(host, numericAmount, sa);

                    Predicate<CardRules>  additionalRule = CardRulesPredicates.cmc(ComparableOp.EQUALS, validAmount);

                    List<PaperCard> cards = Lists.newArrayList(StaticData.instance().getCommonCards().getUniqueCards());
                    Predicate<PaperCard> cpp = Predicates.and(Predicates.compose(baseRule, PaperCard.FN_GET_RULES), 
                            Predicates.compose(additionalRule, PaperCard.FN_GET_RULES));
                    cards = Lists.newArrayList(Iterables.filter(cards, cpp));
                    if (!cards.isEmpty()) {
                        chosen = Aggregates.random(cards).getName();
                    } else {
                        chosen = "";
                    }
                } else if (chooseFromDefined) {
                    CardCollection choices = AbilityUtils.getDefinedCards(host, sa.getParam("ChooseFromDefinedCards"), sa);
                    choices = CardLists.getValidCards(choices, valid, host.getController(), host);
                    Card c = p.getController().chooseSingleEntityForEffect(choices, sa, "Choose a card name");
                    chosen = c != null ? c.getName() : "";
                } else {
                    final String message = validDesc.equals("card") ? "Name a card" : "Name a " + validDesc + " card.";
                    
                    Predicate<PaperCard> cpp = Predicates.alwaysTrue();
                    if ( StringUtils.containsIgnoreCase(valid, "nonland") ) {
                        cpp = Predicates.compose(CardRulesPredicates.Presets.IS_NON_LAND, PaperCard.FN_GET_RULES);
                    }
                    if ( StringUtils.containsIgnoreCase(valid, "nonbasic") ) {
                        cpp = Predicates.compose(Predicates.not(CardRulesPredicates.Presets.IS_BASIC_LAND), PaperCard.FN_GET_RULES);
                    }
                    
                    if ( StringUtils.containsIgnoreCase(valid, "noncreature") ) {
                        cpp = Predicates.compose(Predicates.not(CardRulesPredicates.Presets.IS_CREATURE), PaperCard.FN_GET_RULES);
                    } else if ( StringUtils.containsIgnoreCase(valid, "creature") ) {
                        cpp = Predicates.compose(CardRulesPredicates.Presets.IS_CREATURE, PaperCard.FN_GET_RULES);
                    }
                    
                    chosen = p.getController().chooseCardName(sa, cpp, valid, message);
                }

                host.setNamedCard(chosen);
                if(!randomChoice) {
                    p.getGame().getAction().nofityOfValue(sa, host, p.getName() + " picked " + chosen, p);
                    p.setNamedCard(chosen);
                }
            }
        }
    }

}
