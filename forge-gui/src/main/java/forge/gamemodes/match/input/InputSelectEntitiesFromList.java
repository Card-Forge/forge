package forge.gamemodes.match.input;

import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardView;
import forge.game.cost.CostExile;
import forge.game.cost.CostTapType;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.Zone;
import forge.gui.FThreads;
import forge.player.PlayerControllerHuman;
import forge.player.PlayerZoneUpdate;
import forge.player.PlayerZoneUpdates;
import forge.util.ITriggerEvent;
import forge.util.Localizer;
import forge.util.TextUtil;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class InputSelectEntitiesFromList<T extends GameEntity> extends InputSelectManyBase<T> {
    private static final long serialVersionUID = -6609493252672573139L;

    private final FCollectionView<T> validChoices;
    protected final FCollection<T> selected = new FCollection<>();
    protected Iterable<PlayerZoneUpdate> zonesShown; // want to hide these zones when input done
    protected MassSelectMode massSelectMode = null;

    public InputSelectEntitiesFromList(final PlayerControllerHuman controller, final int min, final int max, final FCollectionView<T> validChoices0) {
        this(controller, min, max, validChoices0, null, "", 0);
    }

    public InputSelectEntitiesFromList(final PlayerControllerHuman controller, final int min, final int max, final FCollectionView<T> validChoices0, final SpellAbility sa0) {
        this(controller, min, max, validChoices0, sa0, "", 0);
    }

    public InputSelectEntitiesFromList(final PlayerControllerHuman controller, final int min, final int max, final FCollectionView<T> validChoices0, final SpellAbility sa0, final String tallyType0, final int tally0) {
        super(controller, Math.min(min, validChoices0.size()), Math.min(max, validChoices0.size()), sa0, tallyType0, tally0);
        validChoices = validChoices0;
        if (min > validChoices.size()) { // pfps does this really do anything useful??
            System.out.printf("Trying to choose at least %d things from a list with only %d things!%n", min, validChoices.size());
        }
        ArrayList<CardView> vCards = new ArrayList<>();
        for (T v : validChoices0) {
            if (v instanceof Card c) {
                vCards.add(c.getView());
            }
        }
        getController().getGui().setSelectables(vCards);
        final PlayerZoneUpdates zonesToUpdate = new PlayerZoneUpdates();
        for (final GameEntity ge : validChoices) {
            final Zone cz = ge instanceof Card c ? c.getLastKnownZone() : null;
            if (cz != null) {
                zonesToUpdate.add(new PlayerZoneUpdate(cz.getPlayer().getView(), cz.getZoneType()));
            }
        }
        FThreads.invokeInEdtNowOrLater(() -> {
            getController().getGui().updateZones(zonesToUpdate);
            zonesShown = getController().getGui().tempShowZones(controller.getPlayer().getView(), zonesToUpdate);
        });
    }
    
    @Override
    protected boolean onCardSelected(final Card c, final List<Card> otherCardsToSelect, final ITriggerEvent triggerEvent) {
        if (!selectEntity(c)) {
            return false;
        }
        refresh();
        return true;
    }

    @Override
    public String getActivateAction(final Card card) {
        if (validChoices.contains(card)) {
            if (selected.contains(card)) {
                return "unselect card";
            }
            return "select card";
        }
        return null;
    }

    @Override
    protected void onPlayerSelected(final Player p, final ITriggerEvent triggerEvent) {
        if (!selectEntity(p)) {
            return;
        }
        refresh();
    }

    @Override
    public final Collection<T> getSelected() {
        return selected;
    }

    @SuppressWarnings("unchecked")
    protected boolean selectEntity(final GameEntity c) {
        if (!validChoices.contains(c)) {
            return false;
        }

        final boolean entityWasSelected = selected.contains(c);
        if (entityWasSelected) {
            selected.remove(c);
        } else {
            selected.add((T)c);
        }
        onSelectStateChanged(c, !entityWasSelected);

        return true;
    }

    // might re-define later
    @Override
    protected boolean hasEnoughTargets() { return selected.size() >= min; }
    @Override
    protected boolean hasAllTargets() { return (massSelectMode == null || massSelectMode == MassSelectMode.NONE) && selected.size() >= max; }

    @Override
    protected String getMessage() {
        StringBuilder msg = new StringBuilder();
        msg.append(max == Integer.MAX_VALUE
                ? String.format(message, selected.size())
                        : String.format(message, max - selected.size()));

        if (sa != null) {
            if (sa.getPayCosts().hasSpecificCostType(CostTapType.class) &&
                (sa.isCrew() || sa.isKeyword(Keyword.SADDLE))) {
                msg.append(sa.isCrew() ? "\nCrewing: " : "\nSaddling: ");
                msg.append(CardLists.getTotalPower((FCollection<Card>)getSelected(), sa));
                msg.append(" / ").append(TextUtil.fastReplace(sa.getPayCosts().
                    getCostPartByType(CostTapType.class).getType(), 
                    "Creature.Other+withTotalPowerGE", ""));
            }
            else if (sa.getPayCosts().hasSpecificCostType(CostExile.class) && tally > 0) {
                msg.append("\n");
                if (tallyType.equals("CMC")) {
                    msg.append(Localizer.getInstance().getMessage("lblCMC")).append(": ");
                    msg.append(CardLists.getTotalCMC((FCollection<Card>)getSelected())).append(" / ").append(tally);
                } else if (tallyType.equals("Types")) {
                    msg.append(Localizer.getInstance().getMessage("lblTypes")).append(": ");
                    msg.append(AbilityUtils.countCardTypesFromList((FCollection<Card>)getSelected(), false));
                    msg.append(" / ").append(tally);
                }
            }
        }

        return msg.toString();
    }

    @Override
    protected void onStop() {
        getController().getGui().hideZones(getController().getPlayer().getView(),zonesShown);  
        getController().getGui().clearSelectables();
        super.onStop();
    }

    /**
     * Shows the usual card action dialog but then, if offering the "mass select" function,
     * replaces the "cancel" button with a button that will cycle through "auto-select all
     * targets owned by player", "auto-select all valid targets", and "clear existing target
     * selections". Because the number of possible targets can be 0, simply pressing "OK"
     * without selecting anything is functionally equivalent to "cancel" so the "cancel"
     * button is not needed in this case.
     */
    @Override
    public void showMessage() {
        super.showMessage();
        // Use mass select mode for proliferate. If you wanted to add it to a different effect
        // the effect must allow you to select any number of targets between "none" and "all valid targets"
        if (sa != null && ApiType.Proliferate == sa.getApi() && min == 0) {
            massSelectMode = MassSelectMode.NONE;
            updateMassSelectButton();
        }
    }

    /**
     * Sets the button label to the "mass select" mode's next value representing what it will
     * change to if the button is clicked.
     */
    private void updateMassSelectButton() {
        String selectButtonLabel = massSelectMode.next().getTranslatedName();
        getController().getGui().updateButtons(getOwner(), Localizer.getInstance().getMessage("lblOK"),
                selectButtonLabel, hasEnoughTargets(), allowCancel, true);
    }

    /**
     * If the cancel button has been updated with the mass-select function then execute the appropriate
     * mass-select function when it is clicked. Otherwise, execute the normal cancel function.
     */
    @Override
    protected void onCancel() {
        if (massSelectMode == null) {
            super.onCancel();
        } else {
            massSelectMode = massSelectMode.next();
            updateMassSelectButton();
            // Remove all current selections
            this.getSelected().forEach(selected -> {
                onSelectStateChanged(selected, false);
            });
            this.getSelected().clear();
            if (massSelectMode == MassSelectMode.MINE) { // Select all valid targets owned by player
                for (T v : validChoices) {
                    if (selected.size() == max) {
                        break;
                    }
                    if (v instanceof Card c) {
                        if (c.getController().equals(getController().getPlayer())) {
                            selected.add(v);
                            onSelectStateChanged(v, true);
                        }
                    }
                }
            } else if (massSelectMode == MassSelectMode.ALL) { // Select all valid targets
                for (T c : validChoices) {
                    if (selected.size() == max) {
                        break;
                    }
                    selected.add(c);
                    onSelectStateChanged(c, true);
                }
            }
        }
    }
}
