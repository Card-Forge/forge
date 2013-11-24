package forge.game.ability.effects;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.Singletons;
import forge.ai.ComputerUtilCard;
import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates.Presets;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
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

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final List<Player> tgtPlayers = getTargetPlayers(sa);

        String valid = "Card";
        String validDesc = "card";
        if (sa.hasParam("ValidCards")) {
            valid = sa.getParam("ValidCards");
            validDesc = sa.getParam("ValidDesc");
        }

        boolean randomChoice = sa.hasParam("AtRandom");
        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                String chosen = "";

                while (true) {
                    if (randomChoice) {
                        // Currently only used for Momir Avatar, if something else gets added here, make it more generic
                        Predicate<CardRules> baseRule = CardRulesPredicates.Presets.IS_CREATURE;

                        String numericAmount = "X";
                        final int validAmount = StringUtils.isNumeric(numericAmount) ? Integer.parseInt(numericAmount) :
                            AbilityUtils.calculateAmount(host, numericAmount, sa);

                        Predicate<CardRules>  additionalRule = CardRulesPredicates.cmc(ComparableOp.EQUALS, validAmount);

                        List<PaperCard> cards = Lists.newArrayList(Singletons.getMagicDb().getCommonCards().getUniqueCards());
                        Predicate<PaperCard> cpp = Predicates.and(Predicates.compose(baseRule, PaperCard.FN_GET_RULES), 
                                Predicates.compose(additionalRule, PaperCard.FN_GET_RULES));
                        cards = Lists.newArrayList(Iterables.filter(cards, cpp));
                        if (!cards.isEmpty()) {
                            chosen = Aggregates.random(cards).getName();
                        } else {
                            chosen = "";
                        }
                        break;
                    } 

                    if (p.isHuman()) {
                        final String message = validDesc.equals("card") ? "Name a card" : "Name a " + validDesc + " card.";
                        
                        Predicate<PaperCard> cpp = null;
                        
                        
                        if ( StringUtils.containsIgnoreCase(valid, "nonland") )
                        {
                            cpp = Predicates.compose(CardRulesPredicates.Presets.IS_NON_LAND, PaperCard.FN_GET_RULES);
                        }
                        if ( StringUtils.containsIgnoreCase(valid, "nonbasic") )
                        {
                            cpp = Predicates.compose(Predicates.not(CardRulesPredicates.Presets.IS_BASIC_LAND), PaperCard.FN_GET_RULES);
                        }
                        if ( StringUtils.containsIgnoreCase(valid, "noncreature") )
                        {
                            cpp = Predicates.compose(Predicates.not(CardRulesPredicates.Presets.IS_CREATURE), PaperCard.FN_GET_RULES);
                        }
                        else if ( StringUtils.containsIgnoreCase(valid, "creature") )
                        {
                            cpp = Predicates.compose(CardRulesPredicates.Presets.IS_CREATURE, PaperCard.FN_GET_RULES);
                        }

                        PaperCard cp = p.getController().chooseSinglePaperCard(sa, message, cpp, sa.getSourceCard().getName());
                        Card instanceForPlayer = Card.fromPaperCard(cp, p); // the Card instance for test needs a game to be tested
                        if (!instanceForPlayer.isValid(valid, host.getController(), host)) 
                            continue;

                        chosen = cp.getName();
                    } else {
                        
                        if (sa.hasParam("AILogic")) {
                            final String logic = sa.getParam("AILogic");
                            if (logic.equals("MostProminentInComputerDeck")) {
                                chosen = ComputerUtilCard.getMostProminentCardName(p.getCardsIn(ZoneType.Library));
                            } else if (logic.equals("MostProminentInHumanDeck")) {
                                chosen = ComputerUtilCard.getMostProminentCardName(p.getOpponent().getCardsIn(ZoneType.Library));
                            } else if (logic.equals("BestCreatureInComputerDeck")) {
                                chosen = ComputerUtilCard.getBestCreatureAI(p.getCardsIn(ZoneType.Library)).getName();
                            } else if (logic.equals("RandomInComputerDeck")) {
                                chosen = Aggregates.random(p.getCardsIn(ZoneType.Library)).getName();
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
                    }
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
