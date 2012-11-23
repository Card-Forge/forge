package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

import forge.Card;
import forge.CardLists;
import forge.CounterType;
import forge.Singletons;
import forge.card.abilityfactory.SpellEffect;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.control.input.Input;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.gui.match.CMatchUI;
import forge.view.ButtonUtil;

public class CountersProliferateEffect extends SpellEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Proliferate.");
        sb.append(" (You choose any number of permanents and/or players with ");
        sb.append("counters on them, then give each another counter of a kind already there.)");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        Player controller = sa.getSourceCard().getController();
        if (controller.isHuman()) {
            resolveHuman(sa);
        } else {
            resolveAI(controller, sa);
        }
    }

    private static void resolveHuman(final SpellAbility sa) {
        final List<Card> unchosen = Lists.newArrayList(Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield));
        final List<Player> players = new ArrayList<Player>(Singletons.getModel().getGame().getPlayers());
        Singletons.getModel().getMatch().getInput().setInput(new Input() {
            private static final long serialVersionUID = -1779224307654698954L;

            @Override
            public void showMessage() {
                ButtonUtil.enableOnlyOK();
                CMatchUI.SINGLETON_INSTANCE.showMessage("Proliferate: Choose permanents and/or players");
            }

            @Override
            public void selectButtonOK() {
                // Hacky intermittent solution to triggers that look for
                // counters being put on. They used
                // to wait for another priority passing after proliferate
                // finished.
                Singletons.getModel().getGame().getStack().chooseOrderOfSimultaneousStackEntryAll();
                this.stop();
            }

            @Override
            public void selectCard(final Card card) {
                if (!unchosen.contains(card)) {
                    return;
                }
                unchosen.remove(card);
                final ArrayList<String> choices = new ArrayList<String>();
                for (final CounterType c1 : CounterType.values()) {
                    if (card.getCounters(c1) != 0) {
                        choices.add(c1.getName());
                    }
                }
                if (choices.size() > 0) {
                    card.addCounter(
                            CounterType.getType((choices.size() == 1 ? choices.get(0) : GuiChoose.one(
                                    "Select counter type", choices).toString())), 1);
                }
            }

            @Override
            public void selectPlayer(final Player player) {
                if (players.indexOf(player) >= 0) {

                    players.remove(player); // no second selection
                    if (player.getPoisonCounters() > 0) {
                        player.addPoisonCounters(1, sa.getSourceCard());
                    }
                }
            }
        });
    }


    private static void resolveAI(final Player ai, final SpellAbility sa) {
        final List<Player> allies = ai.getAllies();
        allies.add(ai);
        final List<Player> enemies = ai.getOpponents();
        final Predicate<Card> predProliferate = new Predicate<Card>() {
            @Override
            public boolean apply(Card crd) {
                for (final Entry<CounterType, Integer> c1 : crd.getCounters().entrySet()) {
                    if (CardFactoryUtil.isNegativeCounter(c1.getKey()) && enemies.contains(crd.getController())) {
                        return true;
                    }
                    if (!CardFactoryUtil.isNegativeCounter(c1.getKey()) && allies.contains(crd.getController())) {
                        return true;
                    }
                }
                return false;
            }
        };

        List<Card> cardsToProliferate = CardLists.filter(Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield), predProliferate);
        List<Player> playersToPoison = new ArrayList<Player>();
        for (Player e : enemies) {
            if (e.getPoisonCounters() > 0) {
                playersToPoison.add(e);
            }
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("<html>Proliferate. Computer selects:<br>");
        if (cardsToProliferate.isEmpty() && playersToPoison.isEmpty()) {
            sb.append("<b>nothing</b>.");
        } else {
            for (Card c : cardsToProliferate) {
                sb.append(c.getController().getName());
                sb.append("'s <b>");
                sb.append(c.getName());
                sb.append("</b><br>");
            }

            if (!playersToPoison.isEmpty()) {
                sb.append("<br>The following players: <br>");
            }
            for (Player p : playersToPoison) {
                sb.append("<b>");
                sb.append(p.getName());
                sb.append("</b><br>");
            }
        } // else
        sb.append("</html>");

        // add a counter of one counter type, if it would benefit the
        // computer
        for (final Card c : cardsToProliferate) {
            for (final Entry<CounterType, Integer> c1 : c.getCounters().entrySet()) {
                if (CardFactoryUtil.isNegativeCounter(c1.getKey()) && enemies.contains(c.getController()))
                {
                    c.addCounter(c1.getKey(), 1);
                    break;
                }
                if (!CardFactoryUtil.isNegativeCounter(c1.getKey()) && allies.contains(c.getController()))
                {
                    c.addCounter(c1.getKey(), 1);
                    break;
                }
            }
        }

        for (final Player p : playersToPoison) {
            p.addPoisonCounters(1, sa.getSourceCard());
        }
    }

}
