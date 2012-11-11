package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import forge.Card;
import forge.CardLists;
import forge.Singletons;
import forge.card.abilityfactory.SpellEffect;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbilityStackInstance;
import forge.card.spellability.Target;
import forge.game.phase.CombatUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;

import com.google.common.base.Predicate;

public class ChooseSourceEffect extends SpellEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        for (final Player p : getTargetPlayers(sa)) {
            sb.append(p).append(" ");
        }
        sb.append("chooses a source.");
    
        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getSourceCard();
        final ArrayList<Card> chosen = new ArrayList<Card>();

        final Target tgt = sa.getTarget();
        final List<Player> tgtPlayers = getTargetPlayers(sa);

        Stack<SpellAbilityStackInstance> stack = Singletons.getModel().getGame().getStack().getStack();

        List<Card> permanentSources = new ArrayList<Card>();
        List<Card> stackSources = new ArrayList<Card>();
        List<Card> referencedSources = new ArrayList<Card>();

        List<Card> sourcesToChooseFrom = new ArrayList<Card>();

        // Get the list of permanent cards
        permanentSources = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);

        // Get the list of cards that produce effects on the stack
        if (stack != null) {
            for (SpellAbilityStackInstance stackinst : stack) {
                if (!stackSources.contains(stackinst.getSourceCard())) {
                    stackSources.add(stackinst.getSourceCard());
                }
                // Get the list of cards that are referenced by effects on the stack
                if (null != stackinst.getSpellAbility().getTriggeringObjects()) {
                    for (Object c : stackinst.getSpellAbility().getTriggeringObjects().values()) {
                        if (c instanceof Card) {
                            if (!stackSources.contains((Card) c)) {
                                referencedSources.add((Card) c);
                            }
                        }
                    }
                }
                if (null != stackinst.getSpellAbility().getTargetCard()) {
                    referencedSources.add(stackinst.getSpellAbility().getTargetCard());
                }
                // TODO: is this necessary?
                if (null != stackinst.getSpellAbility().getReplacingObjects()) {
                    for (Object c : stackinst.getSpellAbility().getReplacingObjects().values()) {
                        if (c instanceof Card) {
                            if (!stackSources.contains((Card) c)) {
                                referencedSources.add((Card) c);
                            }
                        }
                    }
                }
            }
        }

        if (sa.hasParam("Choices")) {
            permanentSources = CardLists.getValidCards(permanentSources, sa.getParam("Choices"), host.getController(), host);

            stackSources = CardLists.getValidCards(stackSources, sa.getParam("Choices"), host.getController(), host);
            referencedSources = CardLists.getValidCards(referencedSources, sa.getParam("Choices"), host.getController(), host);
        }
        if (sa.hasParam("TargetControls")) {
            permanentSources = CardLists.filterControlledBy(permanentSources, tgtPlayers.get(0));
            stackSources = CardLists.filterControlledBy(stackSources, tgtPlayers.get(0));
            referencedSources = CardLists.filterControlledBy(referencedSources, tgtPlayers.get(0));
        }

        Card divPermanentCards = new Card();
        divPermanentCards.setName("--PERMANENT SOURCES:--");
        Card divStackCards = new Card();
        divStackCards.setName("--SOURCES ON STACK:--");
        Card divReferencedCards = new Card();
        divReferencedCards.setName("--SOURCES REFERENCED ON STACK:--");

        if (permanentSources.size() > 0) {
            sourcesToChooseFrom.add(divPermanentCards);
            sourcesToChooseFrom.addAll(permanentSources);
        }
        if (stackSources.size() > 0) {
            sourcesToChooseFrom.add(divStackCards);
            sourcesToChooseFrom.addAll(stackSources);
        }
        if (referencedSources.size() > 0) {
            sourcesToChooseFrom.add(divReferencedCards);
            sourcesToChooseFrom.addAll(referencedSources);
        }

        if (sourcesToChooseFrom.size() == 0) {
            return;
        }

        final String numericAmount = sa.hasParam("Amount") ? sa.getParam("Amount") : "1";
        final int validAmount = !numericAmount.matches("[0-9][0-9]?")
                ? CardFactoryUtil.xCount(host, host.getSVar(sa.getParam("Amount"))) : Integer.parseInt(numericAmount);

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                for (int i = 0; i < validAmount; i++) {
                    if (p.isHuman()) {
                        final String choiceTitle = sa.hasParam("ChoiceTitle") ? sa.getParam("ChoiceTitle") : "Choose a source ";
                        Card o = null;
                        do {
                            o = GuiChoose.one(choiceTitle, sourcesToChooseFrom);
                        } while (o.equals(divPermanentCards) || o.equals(divStackCards) || o.equals(divReferencedCards));
                        chosen.add(o);
                        sourcesToChooseFrom.remove(o);

                    } else { // TODO: AI Support! This is copied from AF ChooseCard! 
                        if (sa.hasParam("AILogic") && sa.getParam("AILogic").equals("NeedsPrevention")) {
                            final Player ai = sa.getActivatingPlayer();
                            sourcesToChooseFrom = CardLists.filter(sourcesToChooseFrom, new Predicate<Card>() {
                                @Override
                                public boolean apply(final Card c) {
                                    if (!c.isAttacking(ai) || !Singletons.getModel().getGame().getCombat().isUnblocked(c)) {
                                        return false;
                                    }
                                    return CombatUtil.damageIfUnblocked(c, ai, Singletons.getModel().getGame().getCombat()) > 0;
                                }
                            });
                            chosen.add(CardFactoryUtil.getBestCreatureAI(sourcesToChooseFrom));
                        } else {
                            chosen.add(CardFactoryUtil.getBestAI(sourcesToChooseFrom));
                        }
                    }
                }
                host.setChosenCard(chosen);
                if (sa.hasParam("RememberChosen")) {
                    for (final Card rem : chosen) {
                        host.addRemembered(rem);
                    }
                }
            }
        }
    }

}
