package forge.card.ability.effects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Predicate;
import forge.Card;
import forge.CardLists;
import forge.CounterType;
import forge.FThreads;
import forge.GameEntity;
import forge.Singletons;
import forge.card.ability.SpellAbilityEffect;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.control.input.InputSelectManyBase;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;

public class CountersProliferateEffect extends SpellAbilityEffect {
    /** 
     * TODO: Write javadoc for this type.
     *
     */
    public static final class InputProliferate extends InputSelectManyBase<GameEntity> {
        private static final long serialVersionUID = -1779224307654698954L;
        private Map<GameEntity, CounterType> chosenCounters = new HashMap<GameEntity, CounterType>();

        public InputProliferate() {
            super(1, Integer.MAX_VALUE);
        }

        @Override
        protected String getMessage() {
            StringBuilder sb = new StringBuilder("Choose permanents and/or players with counters on them to add one more counter of that type.");
            sb.append("\n\nYou've selected so far:\n");
            if( selected.isEmpty()) 
                sb.append("(none)");
            else 
                for(GameEntity ge : selected ) {
                    if( ge instanceof Player )
                        sb.append("* A poison counter to player ").append(ge).append("\n");
                    else
                        sb.append("* ").append(ge).append(" -> ").append(chosenCounters.get(ge)).append("counter\n");
                }
            
            return sb.toString();
        }

        @Override
        public void selectCard(final Card card) {
            if( !selectEntity(card) )
                return;
            
            if( selected.contains(card) ) {
                final List<CounterType> choices = new ArrayList<CounterType>();
                for (final CounterType ct : CounterType.values()) {
                    if (card.getCounters(ct) > 0) {
                        choices.add(ct);
                    }
                }
                
                CounterType toAdd = choices.size() == 1 ? choices.get(0) : GuiChoose.one("Select counter type", choices);
                chosenCounters.put(card, toAdd);
            }
            
            refresh();
        }

        @Override
        public void selectPlayer(final Player player) {
            if( !selectEntity(player) )
                return;
            refresh();
        }

        @Override
        protected boolean isValidChoice(GameEntity choice) {
            if (choice instanceof Player)
                return ((Player) choice).getPoisonCounters() > 0 && !choice.hasKeyword("You can't get poison counters");
            
            if (choice instanceof Card)
                return ((Card) choice).hasCounters();
            
            return false;
        }
        
        public CounterType getCounterFor(GameEntity ge) {
            return chosenCounters.get(ge);
        }
    }


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
            InputProliferate inp = new InputProliferate();
            inp.setCancelAllowed(true);
            FThreads.setInputAndWait(inp);
            if ( inp.hasCancelled() )
                return;
            
            for(GameEntity ge: inp.getSelected()) {
                if( ge instanceof Player )
                    ((Player) ge).addPoisonCounters(1, sa.getSourceCard());
                else if( ge instanceof Card)
                    ((Card) ge).addCounter(inp.getCounterFor(ge), 1, true);
            }
        } else {
            resolveAI(controller, sa);
        }
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
                    c.addCounter(c1.getKey(), 1, true);
                    break;
                }
                if (!CardFactoryUtil.isNegativeCounter(c1.getKey()) && allies.contains(c.getController()))
                {
                    c.addCounter(c1.getKey(), 1, true);
                    break;
                }
            }
        }

        for (final Player p : playersToPoison) {
            p.addPoisonCounters(1, sa.getSourceCard());
        }
    }

}
