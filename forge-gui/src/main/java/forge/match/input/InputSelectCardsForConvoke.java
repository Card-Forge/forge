package forge.match.input;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.ImmutablePair;

import forge.card.ColorSet;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardUtil;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.Player;
import forge.player.PlayerControllerHuman;
import forge.util.ITriggerEvent;


public final class InputSelectCardsForConvoke extends InputSelectManyBase<Card> {
    private static final long serialVersionUID = -1779224307654698954L;
    private final Map<Card, ImmutablePair<Byte, ManaCostShard>> chosenCards = new HashMap<Card, ImmutablePair<Byte, ManaCostShard>>();
    private final ManaCostBeingPaid remainingCost;
    private final Player player;
    private final CardCollectionView availableCreatures;

    public InputSelectCardsForConvoke(final PlayerControllerHuman controller, final Player p, final ManaCost cost, final CardCollectionView untapped) {
        super(controller, 0, Math.min(cost.getCMC(), untapped.size()));
        remainingCost = new ManaCostBeingPaid(cost);
        player = p;
        availableCreatures = untapped;
    }

    @Override
    protected String getMessage() {
        return "Choose creatures to tap for convoke.\nRemaining mana cost is " + remainingCost.toString();
    }

    @Override
    protected boolean onCardSelected(final Card card, final List<Card> otherCardsToSelect, final ITriggerEvent triggerEvent) {
        if (!availableCreatures.contains(card)) {
            // Not in untapped creatures list provided. Not a legal Convoke selection.
            return false;
        }

        final boolean entityWasSelected = chosenCards.containsKey(card);
        if (entityWasSelected) {
            final ImmutablePair<Byte, ManaCostShard> color = this.chosenCards.remove(card);
            remainingCost.increaseShard(color.right, 1);
            onSelectStateChanged(card, false);
        }
        else {
            byte chosenColor;
            final ColorSet colors = CardUtil.getColors(card);
            if (colors.isMonoColor()) {
                // Since the convoke mana logic can use colored mana as colorless if needed,
                // there is no need to prompt the user when convoking with a mono-color creature.
                chosenColor = colors.getColor();
            } else {
                chosenColor = player.getController().chooseColorAllowColorless("Convoke " + card.toString() + "  for which color?", card, colors);
            }
            final ManaCostShard shard = remainingCost.payManaViaConvoke(chosenColor);
            if (shard != null) {
                chosenCards.put(card, ImmutablePair.of(chosenColor, shard));
                onSelectStateChanged(card, true);
            }
            else {
                showMessage("The colors provided by " + card.toString() + " you've chosen cannot be used to decrease the manacost of " + remainingCost.toString());
                return false;
            }
        }

        refresh();
        return true;
    }

    @Override
    public String getActivateAction(final Card card) {
        if (availableCreatures.contains(card)) {
            return "tap creature for Convoke";
        }
        return null;
    }

    @Override
    protected final void onPlayerSelected(final Player player, final ITriggerEvent triggerEvent) {
    }

    public Map<Card, ManaCostShard> getConvokeMap() {
        final Map<Card, ManaCostShard> result = new HashMap<Card, ManaCostShard>();
        if(!hasCancelled()) {
            for(final Entry<Card, ImmutablePair<Byte, ManaCostShard>> c : chosenCards.entrySet()) {
                result.put(c.getKey(), c.getValue().right);
            }
        }
        return result;
    }

    @Override
    protected boolean hasEnoughTargets() { return true; }

    @Override
    protected boolean hasAllTargets() { return false; }

    @Override
    public Collection<Card> getSelected() {
        // TODO Auto-generated method stub
        return chosenCards.keySet();
    }
}