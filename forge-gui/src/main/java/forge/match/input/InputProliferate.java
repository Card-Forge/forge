package forge.match.input;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.player.PlayerControllerHuman;
import forge.util.ITriggerEvent;

public final class InputProliferate extends InputSelectManyBase<GameEntity> {
    private static final long serialVersionUID = -1779224307654698954L;
    private final Map<GameEntity, CounterType> chosenCounters = new HashMap<GameEntity, CounterType>();

    public InputProliferate(final PlayerControllerHuman controller) {
        super(controller, 1, Integer.MAX_VALUE);
    }

    @Override
    protected String getMessage() {
        final StringBuilder sb = new StringBuilder("Choose permanents and/or players with counters on them to add one more counter of that type.");
        sb.append("\n\nYou've selected so far:\n");
        if (chosenCounters.isEmpty()) {
            sb.append("(none)");
        }
        else {
            for (final Entry<GameEntity, CounterType> ge : chosenCounters.entrySet()) {
                if (ge.getKey() instanceof Player) {
                    sb.append("* A poison counter to player ").append(ge.getKey()).append("\n");
                }
                else {
                    sb.append("* ").append(ge.getKey()).append(" -> ").append(ge.getValue()).append("counter\n");
                }
            }
        }

        return sb.toString();
    }

    @Override
    protected boolean onCardSelected(final Card card, final List<Card> otherCardsToSelect, final ITriggerEvent triggerEvent) {
        if (!card.hasCounters()) {
            return false;
        }

        final boolean entityWasSelected = chosenCounters.containsKey(card);
        if (entityWasSelected) {
            this.chosenCounters.remove(card);
        }
        else {
            final List<CounterType> choices = new ArrayList<CounterType>();
            for (final CounterType ct : CounterType.values()) {
                if (card.getCounters(ct) > 0) {
                    choices.add(ct);
                }
            }

            final CounterType toAdd = choices.size() == 1 ? choices.get(0) : getController().getGui().one("Select counter type", choices);
            chosenCounters.put(card, toAdd);
        }

        refresh();
        return true;
    }

    @Override
    public String getActivateAction(final Card card) {
        if (card.hasCounters() && !chosenCounters.containsKey(card)) {
            return "add counter to card";
        }
        return null;
    }

    @Override
    protected final void onPlayerSelected(final Player player, final ITriggerEvent triggerEvent) {
        if (player.getPoisonCounters() == 0 || player.hasKeyword("You can't get poison counters")) {
            return;
        }

        final boolean entityWasSelected = chosenCounters.containsKey(player);
        if (entityWasSelected) {
            this.chosenCounters.remove(player);
        } else {
            this.chosenCounters.put(player, null /* POISON counter is meant */);
        }

        refresh();
    }

    public Map<GameEntity, CounterType> getProliferationMap() {
        return chosenCounters;
    }


    @Override
    protected boolean hasEnoughTargets() { return true; }

    @Override
    protected boolean hasAllTargets() { return false; }


    @Override
    public Collection<GameEntity> getSelected() {
        // TODO Auto-generated method stub
        return chosenCounters.keySet();
    }
}