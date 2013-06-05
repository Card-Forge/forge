package forge.card.ability.effects;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.Card;
import forge.CardLists;
import forge.CardPredicates.Presets;
import forge.card.CardDb;
import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.ai.ComputerUtilCard;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.item.PaperCard;
import forge.util.Aggregates;
import forge.util.ComparableOp;

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
        final Card host = sa.getSourceCard();

        final Target tgt = sa.getTarget();
        final List<Player> tgtPlayers = getTargetPlayers(sa);

        String valid = "Card";
        String validDesc = "card";
        if (sa.hasParam("ValidCards")) {
            valid = sa.getParam("ValidCards");
            validDesc = sa.getParam("ValidDesc");
        }

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                boolean ok = false;
                while (!ok) {
                    if (sa.hasParam("AtRandom")) {
                        // Currently only used for Momir Avatar, if something else gets added here, make it more generic
                        Predicate<CardRules> baseRule = CardRulesPredicates.Presets.IS_CREATURE;

                        String numericAmount = "X";
                        final int validAmount = StringUtils.isNumeric(numericAmount) ? Integer.parseInt(numericAmount) :
                            AbilityUtils.calculateAmount(host, numericAmount, sa);

                        Predicate<CardRules>  additionalRule = CardRulesPredicates.cmc(ComparableOp.EQUALS, validAmount);

                        List<PaperCard> cards = Lists.newArrayList(CardDb.instance().getUniqueCards());
                        Predicate<PaperCard> cpp = Predicates.and(Predicates.compose(baseRule, PaperCard.FN_GET_RULES), 
                                Predicates.compose(additionalRule, PaperCard.FN_GET_RULES));
                        cards = Lists.newArrayList(Iterables.filter(cards, cpp));
                        if (!cards.isEmpty()) {
                            host.setNamedCard(Aggregates.random(cards).getName());
                        } else {
                            host.setNamedCard("");
                        }
                        ok = true;
                    } else if (p.isHuman()) {
                        final String message = validDesc.equals("card") ? "Name a card" : "Name a " + validDesc + " card.";
                        
                        List<PaperCard> cards = Lists.newArrayList(CardDb.instance().getUniqueCards());
                        if ( StringUtils.containsIgnoreCase(valid, "nonland") )
                        {
                            Predicate<PaperCard> cpp = Predicates.compose(CardRulesPredicates.Presets.IS_NON_LAND, PaperCard.FN_GET_RULES);
                            cards = Lists.newArrayList(Iterables.filter(cards, cpp));
                        }
                        if ( StringUtils.containsIgnoreCase(valid, "nonbasic") )
                        {
                            Predicate<PaperCard> cpp = Predicates.compose(Predicates.not(CardRulesPredicates.Presets.IS_BASIC_LAND), PaperCard.FN_GET_RULES);
                            cards = Lists.newArrayList(Iterables.filter(cards, cpp));
                        }
                        if ( StringUtils.containsIgnoreCase(valid, "noncreature") )
                        {
                            Predicate<PaperCard> cpp = Predicates.compose(Predicates.not(CardRulesPredicates.Presets.IS_CREATURE), PaperCard.FN_GET_RULES);
                            cards = Lists.newArrayList(Iterables.filter(cards, cpp));
                        }
                        else if ( StringUtils.containsIgnoreCase(valid, "creature") )
                        {
                            Predicate<PaperCard> cpp = Predicates.compose(CardRulesPredicates.Presets.IS_CREATURE, PaperCard.FN_GET_RULES);
                            cards = Lists.newArrayList(Iterables.filter(cards, cpp));
                        }

                        Collections.sort(cards);
                            
                        PaperCard cp = GuiChoose.one(message, cards);
                        if (cp.getMatchingForgeCard().isValid(valid, host.getController(), host)) {
                            host.setNamedCard(cp.getName());
                            ok = true;
                        }
                    } else {
                        String chosen = "";
                        if (sa.hasParam("AILogic")) {
                            final String logic = sa.getParam("AILogic");
                            if (logic.equals("MostProminentInComputerDeck")) {
                                chosen = ComputerUtilCard.getMostProminentCardName(p.getCardsIn(ZoneType.Library));
                            } else if (logic.equals("MostProminentInHumanDeck")) {
                                chosen = ComputerUtilCard.getMostProminentCardName(p.getOpponent().getCardsIn(ZoneType.Library));
                            } else if (logic.equals("BestCreatureInComputerDeck")) {
                                chosen = ComputerUtilCard.getBestCreatureAI(p.getCardsIn(ZoneType.Library)).getName();
                            }
                        } else {
                            List<Card> list = CardLists.filterControlledBy(p.getGame().getCardsInGame(), p.getOpponent());
                            list = CardLists.filter(list, Predicates.not(Presets.LANDS));
                            if (!list.isEmpty()) {
                                chosen = list.get(0).getName();
                            }
                        }
                        if (chosen.equals("")) {
                            chosen = "Morphling";
                        }
                        GuiChoose.one("Computer picked: ", new String[]{chosen});
                        host.setNamedCard(chosen);
                        ok = true;
                    }
                }
            }
        }
    }

}
