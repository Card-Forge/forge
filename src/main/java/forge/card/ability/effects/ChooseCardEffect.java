package forge.card.ability.effects;

import java.util.ArrayList;
import java.util.List;

import forge.Card;
import forge.CardLists;
import forge.Singletons;
import forge.CardPredicates.Presets;
import forge.card.CardType;
import forge.card.ability.SpellEffect;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.util.Aggregates;

public class ChooseCardEffect extends SpellEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        for (final Player p : getTargetPlayers(sa)) {
            sb.append(p).append(" ");
        }
        sb.append("chooses a card.");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getSourceCard();
        final ArrayList<Card> chosen = new ArrayList<Card>();

        final Target tgt = sa.getTarget();
        final List<Player> tgtPlayers = getTargetPlayers(sa);

        ZoneType choiceZone = ZoneType.Battlefield;
        if (sa.hasParam("ChoiceZone")) {
            choiceZone = ZoneType.smartValueOf(sa.getParam("ChoiceZone"));
        }
        List<Card> choices = Singletons.getModel().getGame().getCardsIn(choiceZone);
        if (sa.hasParam("Choices")) {
            choices = CardLists.getValidCards(choices, sa.getParam("Choices"), host.getController(), host);
        }
        if (sa.hasParam("TargetControls")) {
            choices = CardLists.filterControlledBy(choices, tgtPlayers.get(0));
        }

        final String numericAmount = sa.hasParam("Amount") ? sa.getParam("Amount") : "1";
        final int validAmount = !numericAmount.matches("[0-9][0-9]?")
                ? CardFactoryUtil.xCount(host, host.getSVar(sa.getParam("Amount"))) : Integer.parseInt(numericAmount);

        if (sa.hasParam("SunderingTitan")) {
            final List<Card> land = CardLists.filter(Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield), Presets.LANDS);
            for (final String type : CardType.getBasicTypes()) {
                final List<Card> cl = CardLists.getType(land, type);
                if (cl.size() > 0) {
                    final String prompt = "Choose a" + (type.equals("Island") ? "n " : " ") + type;
                    final Object o = GuiChoose.one(prompt, cl);
                    if (null != o) {
                        final Card c = (Card) o;
                        chosen.add(c);
                    }
                }
            }
            host.setChosenCard(chosen);
            return;
        }

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                for (int i = 0; i < validAmount; i++) {
                    if (p.isHuman()) {
                        final String choiceTitle = sa.hasParam("ChoiceTitle") ? sa.getParam("ChoiceTitle") : "Choose a card ";
                        Card o;
                        if (sa.hasParam("AtRandom")) {
                            o = Aggregates.random(choices);
                        } else if (sa.hasParam("Mandatory")) {
                            o = GuiChoose.one(choiceTitle, choices);
                        } else {
                            o = GuiChoose.oneOrNone(choiceTitle, choices);
                        }
                        if (o != null) {
                            chosen.add(o);
                            choices.remove(o);
                        } else {
                            break;
                        }
                    } else { // Computer
                        String logic = sa.getParam("AILogic");
                        Card choice = null;
                        if (logic == null) {
                            // Base Logic is choose "best"
                            choice = CardFactoryUtil.getBestAI(choices);
                        } else if ("WorstCard".equals(logic)) {
                            choice = CardFactoryUtil.getWorstAI(choices);
                        } else if (logic.equals("BestBlocker")) {
                            if (!CardLists.filter(choices, Presets.UNTAPPED).isEmpty()) {
                                choices = CardLists.filter(choices, Presets.UNTAPPED);
                            }
                            choice = CardFactoryUtil.getBestCreatureAI(choices);
                        } else if (logic.equals("Clone")) {
                            if (!CardLists.getValidCards(choices, "Permanent.YouDontCtrl,Permanent.nonLegendary", host.getController(), host).isEmpty()) {
                                choices = CardLists.getValidCards(choices, "Permanent.YouDontCtrl,Permanent.nonLegendary", host.getController(), host);
                            }
                            choice = CardFactoryUtil.getBestAI(choices);
                        } else if (logic.equals("Untap")) {
                            if (!CardLists.getValidCards(choices, "Permanent.YouCtrl,Permanent.tapped", host.getController(), host).isEmpty()) {
                                choices = CardLists.getValidCards(choices, "Permanent.YouCtrl,Permanent.tapped", host.getController(), host);
                            }
                            choice = CardFactoryUtil.getBestAI(choices);
                        }
                        if (choice != null) {
                            chosen.add(choice);
                            choices.remove(choice);
                        } else {
                            break;
                        }
                    }
                }
                host.setChosenCard(chosen);
                if (sa.hasParam("RememberChosen")) {
                    for (final Card rem : chosen) {
                        host.addRemembered(rem);
                    }
                }
                if (sa.hasParam("ForgetChosen")) {
                    for (final Card rem : chosen) {
                        host.removeRemembered(rem);
                    }
                }
            }
        }
    }

}
