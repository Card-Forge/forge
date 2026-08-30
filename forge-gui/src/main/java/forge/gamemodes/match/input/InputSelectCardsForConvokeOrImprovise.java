package forge.gamemodes.match.input;

import com.google.common.collect.Maps;
import forge.card.ColorSet;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.player.PlayerControllerHuman;
import forge.util.ITriggerEvent;
import forge.util.TextUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public final class InputSelectCardsForConvokeOrImprovise extends InputSelectManyBase<Card> {
    private static final long serialVersionUID = -1779224307654698954L;
    private final Map<Card, ManaCostShard> chosenCards = new HashMap<>();
    private final ManaCostBeingPaid remainingCost;
    private final Player player;
    private final CardCollectionView availableCards;
    private final boolean artifacts;
    private final boolean creatures;
    private final Integer maxSelectable;
    private final String cardType;
    private final String description;

    public InputSelectCardsForConvokeOrImprovise(final PlayerControllerHuman controller, final Player p, final SpellAbility sa, final ManaCost cost, final CardCollectionView untapped, boolean artifacts, boolean creatures, Integer maxReduction) {
        super(controller, 0, Math.min(cost.getCMC(), untapped.size()), sa);
        remainingCost = new ManaCostBeingPaid(cost);
        player = p;
        availableCards = untapped;
        this.artifacts = artifacts;
        this.creatures = creatures;
        this.maxSelectable = maxReduction;

        if (artifacts && creatures) {
            cardType = "artifact or creature";
            description = "Waterbend";
        } else if (!artifacts && !creatures) {
            throw new IllegalArgumentException("At least one of artifacts or creatures must be true");
        } else if (creatures) {
            cardType = "creature";
            description = "Convoke";
        } else {
            cardType = "artifact";
            description = "Improvise";
        }
    }

    @Override
    protected String getMessage() {
        StringBuilder sb = new StringBuilder();
        if ( FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_DETAILED_SPELLDESC_IN_PROMPT) &&
                sa != null ) {
            sb.append(sa.getStackDescription()).append("\n");
        }
        sb.append(TextUtil.concatNoSpace("Choose ", cardType, " to tap for ", description, ".\nRemaining mana cost is ", remainingCost.toString()));

        if (maxSelectable != null) {
            sb.append(". You may select up to ").append(chosenCards.size() - maxSelectable).append(" more ").append(cardType).append("(s).");
        }
        return sb.toString();
    }

    @Override
    protected boolean onCardSelected(final Card card, final List<Card> otherCardsToSelect, final ITriggerEvent triggerEvent) {
        if (!availableCards.contains(card)) {
            // Not in untapped list provided. Not a legal Convoke selection.
            return false;
        }

        final boolean entityWasSelected = chosenCards.containsKey(card);
        if (entityWasSelected) {
            final ManaCostShard color = this.chosenCards.remove(card);
            remainingCost.increaseShard(color, 1);
            onSelectStateChanged(card, false);
        }
        else {
            if (maxSelectable != null && chosenCards.size() >= maxSelectable) {
                // Should show a different message if there's a max selectable
                return false;
            }

            byte chosenColor;
            if (artifacts) {
                // Waterbend/Improvise can be paid with colorless mana from artifacts
                chosenColor = ManaCostShard.COLORLESS.getColorMask();
            } else {
                // Convoke can pay color or generic mana cost from creatures
                ColorSet colors = card.getColor();
                if (colors.isMulticolor()) {
                    //if card is multicolor, strip out any colors which can't be paid towards remaining cost
                    colors = ColorSet.fromMask(colors.getColor() & remainingCost.getUnpaidColors());
                }
                if (colors.isMulticolor()) {
                    //prompt user if more than one option for which color to pay towards convoke
                    chosenColor = player.getController().chooseColorAllowColorless("Convoke " + card.toString() + "  for which color?", card, colors);
                } else {
                    // Since the convoke mana logic can use colored mana as generic if needed,
                    // there is no need to prompt the user when convoking with a mono-color creature.
                    chosenColor = colors.getColor();
                }
            }
            final ManaCostShard shard = remainingCost.payManaViaConvoke(chosenColor);
            if (shard != null) {
                chosenCards.put(card, shard);
                onSelectStateChanged(card, true);
            }
            else {
                showMessage("The colors provided by " + card.toString() + " you've chosen cannot be used to pay the manacost of " + remainingCost.toString());
                return false;
            }
        }

        refresh();
        return true;
    }

    @Override
    public String getActivateAction(final Card card) {
        if (availableCards.contains(card)) {
            return TextUtil.concatNoSpace("tap ", cardType, " for ", description);
        }
        return null;
    }

    public Map<Card, ManaCostShard> getConvokeMap() {
        if (hasCancelled()) {
            return Maps.newHashMap();
        }
        return chosenCards;
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
