package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.List;

import forge.Card;
import forge.CardLists;
import forge.CardUtil;
import forge.Singletons;
import forge.CardPredicates.Presets;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;

public class ChooseCardEffect extends SpellEffect {
    @Override
    protected String getStackDescription(java.util.Map<String,String> params, SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
    
        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard()).append(" - ");
        }
    
        ArrayList<Player> tgtPlayers;
    
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }
    
        for (final Player p : tgtPlayers) {
            sb.append(p).append(" ");
        }
        sb.append("chooses a card.");
    
        return sb.toString();
    }

    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        final Card host = sa.getSourceCard();
        final ArrayList<Card> chosen = new ArrayList<Card>();

        ArrayList<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }
        ZoneType choiceZone = ZoneType.Battlefield;
        if (params.containsKey("ChoiceZone")) {
            choiceZone = ZoneType.smartValueOf(params.get("ChoiceZone"));
        }
        List<Card> choices = Singletons.getModel().getGame().getCardsIn(choiceZone);
        if (params.containsKey("Choices")) {
            choices = CardLists.getValidCards(choices, params.get("Choices"), host.getController(), host);
        }
        if (params.containsKey("TargetControls")) {
            choices = CardLists.filterControlledBy(choices, tgtPlayers.get(0));
        }

        final String numericAmount = params.containsKey("Amount") ? params.get("Amount") : "1";
        final int validAmount = !numericAmount.matches("[0-9][0-9]?")
                ? CardFactoryUtil.xCount(host, host.getSVar(params.get("Amount"))) : Integer.parseInt(numericAmount);

        if (params.containsKey("SunderingTitan")) {
            final List<Card> land = Singletons.getModel().getGame().getLandsInPlay();
            final ArrayList<String> basic = CardUtil.getBasicTypes();

            for (final String type : basic) {
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
                        final String choiceTitle = params.containsKey("ChoiceTitle") ? params.get("ChoiceTitle") : "Choose a card ";
                        final Card o = GuiChoose.oneOrNone(choiceTitle, choices);
                        if (o != null) {
                            chosen.add(o);
                            choices.remove(o);
                        } else {
                            break;
                        }
                    } else { // Computer
                        if (params.containsKey("AILogic") && params.get("AILogic").equals("BestBlocker")) {
                            if (!CardLists.filter(choices, Presets.UNTAPPED).isEmpty()) {
                                choices = CardLists.filter(choices, Presets.UNTAPPED);
                            }
                            chosen.add(CardFactoryUtil.getBestCreatureAI(choices));
                        } else if (params.containsKey("AILogic") && params.get("AILogic").equals("Clone")) {
                            if (!CardLists.getValidCards(choices, "Permanent.YouDontCtrl,Permanent.NonLegendary", host.getController(), host).isEmpty()) {
                                choices = CardLists.getValidCards(choices, "Permanent.YouDontCtrl,Permanent.NonLegendary", host.getController(), host);
                            }
                            chosen.add(CardFactoryUtil.getBestAI(choices));
                        } else {
                            chosen.add(CardFactoryUtil.getBestAI(choices));
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