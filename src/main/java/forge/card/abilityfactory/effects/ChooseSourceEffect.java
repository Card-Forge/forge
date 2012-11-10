package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.List;

import forge.Card;
import forge.CardLists;
import forge.Singletons;
import forge.CardPredicates.Presets;
import forge.card.abilityfactory.SpellEffect;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbilityStackInstance;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import java.util.Collection;
import java.util.Stack;

public class ChooseSourceEffect extends SpellEffect {
    @Override
    protected String getStackDescription(java.util.Map<String,String> params, SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        for (final Player p : getTargetPlayers(sa, params)) {
            sb.append(p).append(" ");
        }
        sb.append("chooses a source.");
    
        return sb.toString();
    }

    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        final Card host = sa.getSourceCard();
        final ArrayList<Card> chosen = new ArrayList<Card>();

        final Target tgt = sa.getTarget();
        final List<Player> tgtPlayers = getTargetPlayers(sa, params);

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
                    for (Object c : (Collection<Object>) stackinst.getSpellAbility().getTriggeringObjects().values()) {
                        if (c instanceof Card) {
                            referencedSources.add((Card) c);
                        }
                    }
                }
                if (null != stackinst.getSpellAbility().getTargetList()) {
                    for (Card c : stackinst.getSpellAbility().getTargetList()) {
                        referencedSources.add(c);
                    }
                }
                if (null != stackinst.getSpellAbility().getTargetCard()) {
                    referencedSources.add(stackinst.getSpellAbility().getTargetCard());
                }
                // TODO: is this necessary?
                if (null != stackinst.getSpellAbility().getReplacingObjects()) {
                    for (Object c : (Collection<Object>) stackinst.getSpellAbility().getReplacingObjects().values()) {
                        if (c instanceof Card) {
                            referencedSources.add((Card) c);
                        }
                    }
                }
            }
        }

        ArrayList<String> src_choices = new ArrayList<String>();

        if (params.containsKey("Choices")) {
            permanentSources = CardLists.getValidCards(permanentSources, params.get("Choices"), host.getController(), host);

            stackSources = CardLists.getValidCards(stackSources, params.get("Choices"), host.getController(), host);
            referencedSources = CardLists.getValidCards(referencedSources, params.get("Choices"), host.getController(), host);
        }
        if (params.containsKey("TargetControls")) {
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

        final String numericAmount = params.containsKey("Amount") ? params.get("Amount") : "1";
        final int validAmount = !numericAmount.matches("[0-9][0-9]?")
                ? CardFactoryUtil.xCount(host, host.getSVar(params.get("Amount"))) : Integer.parseInt(numericAmount);

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                for (int i = 0; i < validAmount; i++) {
                    if (p.isHuman()) {
                        final String choiceTitle = params.containsKey("ChoiceTitle") ? params.get("ChoiceTitle") : "Choose a source ";
                        Card o = null;
                        do {
                            o = GuiChoose.one(choiceTitle, sourcesToChooseFrom);
                        } while (o.equals(divPermanentCards) || o.equals(divStackCards) || o.equals(divReferencedCards));
                        if (o != null) {
                            chosen.add(o);
                            sourcesToChooseFrom.remove(o);
                        } else {
                            break;
                        }
                    } else { // TODO: AI Support! This is copied from AF ChooseCard! 
                        if (params.containsKey("AILogic") && params.get("AILogic").equals("BestBlocker")) {
                            if (!CardLists.filter(sourcesToChooseFrom, Presets.UNTAPPED).isEmpty()) {
                                sourcesToChooseFrom = CardLists.filter(sourcesToChooseFrom, Presets.UNTAPPED);
                            }
                            chosen.add(CardFactoryUtil.getBestCreatureAI(sourcesToChooseFrom));
                        } else if (params.containsKey("AILogic") && params.get("AILogic").equals("Clone")) {
                            if (!CardLists.getValidCards(sourcesToChooseFrom, "Permanent.YouDontCtrl,Permanent.NonLegendary", host.getController(), host).isEmpty()) {
                                sourcesToChooseFrom = CardLists.getValidCards(sourcesToChooseFrom, "Permanent.YouDontCtrl,Permanent.NonLegendary", host.getController(), host);
                            }
                            chosen.add(CardFactoryUtil.getBestAI(sourcesToChooseFrom));
                        } else {
                            chosen.add(CardFactoryUtil.getBestAI(sourcesToChooseFrom));
                        }
                    }
                }
                host.setChosenCard(chosen);
                if (params.containsKey("RememberChosen")) {
                    for (final Card rem : chosen) {
                        host.addRemembered(rem);
                    }
                }
            }
        }
    }

}
