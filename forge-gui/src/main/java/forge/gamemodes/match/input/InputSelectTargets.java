package forge.gamemodes.match.input;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

import forge.game.GameEntity;
import forge.game.GameObject;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardPredicates;
import forge.game.card.CardView;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.gui.FThreads;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.player.PlayerControllerHuman;
import forge.player.PlayerZoneUpdate;
import forge.player.PlayerZoneUpdates;
import forge.util.Aggregates;
import forge.util.ITriggerEvent;
import forge.util.TextUtil;

public final class InputSelectTargets extends InputSyncronizedBase {
    private final List<Card> choices;
    // some cards can be targeted several times (eg: distribute damage as you choose)
    private final Map<GameEntity, Integer> targetDepth = new HashMap<>();
    private final TargetRestrictions tgt;
    private final SpellAbility sa;
    private final Collection<Integer> divisionValues;
    private Card lastTarget = null;
    private boolean bCancel = false;
    private boolean bOk = false;
    private final boolean mandatory;
    private Predicate<GameObject> filter;
    private boolean mustTargetFiltered;
    private static final long serialVersionUID = -1091595663541356356L;

    public final boolean hasCancelled() { return bCancel; }
    public final boolean hasPressedOk() { return bOk; }

    public InputSelectTargets(final PlayerControllerHuman controller, final List<Card> choices, final SpellAbility sa, final boolean mandatory, Collection<Integer> divisionValues, Predicate<GameObject> filter, boolean mustTargetFiltered) {
        super(controller);
        this.choices = choices;
        this.tgt = sa.getTargetRestrictions();
        this.sa = sa;
        this.mandatory = mandatory;
        this.divisionValues = divisionValues;
        this.filter = filter;

        this.mustTargetFiltered = mustTargetFiltered;
        for (final Card card : sa.getTargets().getTargetCards()) {
            targetDepth.put(card, Integer.valueOf(1));
            lastTarget = card;
        }

        controller.getGui().setSelectables(CardView.getCollection(choices));
        final PlayerZoneUpdates zonesToUpdate = new PlayerZoneUpdates();
        for (final Card c : choices) {
            zonesToUpdate.add(new PlayerZoneUpdate(c.getZone().getPlayer().getView(), c.getZone().getZoneType()));
        }
        FThreads.invokeInEdtNowOrLater(new Runnable() {
            @Override
            public void run() {
                for (final GameEntity c : targetDepth.keySet()) {
                    if (c instanceof Card) {
                        controller.getGui().setUsedToPay(CardView.get((Card) c), true);
                    }
                }
                controller.getGui().updateZones(zonesToUpdate);
            }
        });
    }

    @Override
    public void showMessage() {
        // Display targeting card in cardDetailPane in case it's not obviously visible.
        getController().getGui().setCard(CardView.get(sa.getHostCard()));
        final StringBuilder sb = new StringBuilder();
        if (FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_DETAILED_SPELLDESC_IN_PROMPT)) {
            // sb.append(sa.getStackDescription().replace("(Targeting ERROR)", "")).append("\n").append(tgt.getVTSelection());
            // Apparently <b>...</b> tags do not work in mobile Forge, so don't include them (for now)
            sb.append(sa.getHostCard().toString()).append(" - ");
            sb.append(sa.toString()).append("\n");
            if(!ForgeConstants.isGdxPortLandscape)
                sb.append("\n");
            sb.append(tgt.getVTSelection());
        } else {
            sb.append(sa.getHostCard()).append(" - ").append(tgt.getVTSelection());
        }
        if (!targetDepth.entrySet().isEmpty()) {
            sb.append("\nTargeted: ");
        }
        for (final Entry<GameEntity, Integer> o : targetDepth.entrySet()) {
            //if it's not in gdx port landscape mode, append the linebreak
            if (!ForgeConstants.isGdxPortLandscape)
                sb.append("\n");
            sb.append(o.getKey());
            //if it's in gdx port landscape mode, instead append the comma with space...
            if (ForgeConstants.isGdxPortLandscape)
                sb.append(", ");
            if (o.getValue() > 1) {
                sb.append(TextUtil.concatNoSpace(" (", String.valueOf(o.getValue()), " times)"));
            }
        }
        if (!sa.getUniqueTargets().isEmpty()) {
            sb.append("\nParent Targeted:");
            sb.append(sa.getUniqueTargets());
        }

        final int maxTargets = sa.getMaxTargets();
        final int targeted = sa.getTargets().size();
        if(maxTargets > 1) {
            sb.append(TextUtil.concatNoSpace("\n(", String.valueOf(maxTargets - targeted), " more can be targeted)"));
        }

        String message = TextUtil.fastReplace(TextUtil.fastReplace(sb.toString(),
                "CARDNAME", sa.getHostCard().toString()),
                "(Targeting ERROR)", "");
        showMessage(message, sa.getView());

        if (divisionValues != null && !divisionValues.isEmpty() && sa.getMinTargets() == 0 && sa.getTargets().size() == 0) {
            // extra logic for Divided with min targets = 0, should only work if num targets are 0 too
            getController().getGui().updateButtons(getOwner(), true, true, false);
        } else if (!sa.isMinTargetChosen() || (divisionValues != null && !divisionValues.isEmpty())){
            // If reached Minimum targets, enable OK button
            if (mandatory && tgt.hasCandidates(sa)) {
                // Player has to click on a target
                getController().getGui().updateButtons(getOwner(), false, false, false);
            } else {
                getController().getGui().updateButtons(getOwner(), false, true, false);
            }
        } else {
            if (mandatory && tgt.hasCandidates(sa)) {
                // Player has to click on a target or ok
                getController().getGui().updateButtons(getOwner(), true, false, true);
            } else {
                getController().getGui().updateButtons(getOwner(), true, true, true);
            }
        }
    }

    @Override
    protected final void onCancel() {
        bCancel = true;
        this.done();
    }

    @Override
    protected final void onOk() {
        bOk = true;
        this.done();
    }

    @Override
    protected final boolean onCardSelected(final Card card, final List<Card> otherCardsToSelect, final ITriggerEvent triggerEvent) {
        if (tgt.isUniqueTargets() && targetDepth.containsKey(card)) {
            return false;
        }

        // TODO should use sa.canTarget(card) instead?
        // it doesn't have messages

        //If the card is not a valid target
        if (!card.canBeTargetedBy(sa)) {
            showMessage(sa.getHostCard() + " - Cannot target this card (Shroud? Protection? Restrictions).");
            return false;
        }
        // If all cards must be from the same zone
        if (tgt.isSingleZone() && lastTarget != null && !card.getController().equals(lastTarget.getController())) {
            showMessage(sa.getHostCard() + " - Cannot target this card (not in the same zone)");
            return false;
        }

        // If the cards can't share a creature type
        if (tgt.isWithoutSameCreatureType() && lastTarget != null && card.sharesCreatureTypeWith(lastTarget)) {
            showMessage(sa.getHostCard() + " - Cannot target this card (should not share a creature type)");
            return false;
        }
        // If the cards share a creature type
        if (tgt.isWithSameCreatureType() && lastTarget != null && !card.sharesCreatureTypeWith(lastTarget)) {
            showMessage(sa.getHostCard() + " - Cannot target this card (should share a creature type)");
            return false;
        }

        // If the cards share a card type
        if (tgt.isWithSameCardType() && lastTarget != null && !card.sharesCardTypeWith(lastTarget)) {
            showMessage(sa.getHostCard() + " - Cannot target this card (should share a Card type)");
            return false;
        }

        if (sa.hasParam("MaxTotalTargetCMC")) {
            int maxTotalCMC = tgt.getMaxTotalCMC(sa.getHostCard(), sa);
            if (maxTotalCMC > 0) {
                int soFar = Aggregates.sum(sa.getTargets().getTargetCards(), CardPredicates.Accessors.fnGetCmc);
                if (!sa.isTargeting(card)) {
                    soFar += card.getCMC();
                }
                if (soFar > maxTotalCMC) {
                    showMessage(sa.getHostCard() + " - Cannot target this card (mana value limit exceeded)");
                    return false;
                }
            }
        }

        // If all cards must have same controllers
        if (tgt.isSameController()) {
            final List<Player> targetedControllers = new ArrayList<>();
            for (final GameObject o : targetDepth.keySet()) {
                if (o instanceof Card) {
                    final Player p = ((Card) o).getController();
                    targetedControllers.add(p);
                }
            }
            if (!targetedControllers.isEmpty() && !targetedControllers.contains(card.getController())) {
                showMessage(sa.getHostCard() + " - Cannot target this card (must have same controller)");
                return false;
            }
        }

        if (sa.hasParam("MaxTotalTargetPower")) {
            int maxTotalPower = tgt.getMaxTotalPower(sa.getHostCard(), sa);
            if (maxTotalPower > 0) {
                int soFar = Aggregates.sum(sa.getTargets().getTargetCards(), CardPredicates.Accessors.fnGetNetPower);
                if (!sa.isTargeting(card)) {
                    soFar += card.getNetPower();
                }
                if (soFar > maxTotalPower) {
                    showMessage(sa.getHostCard() + " - Cannot target this card (power limit exceeded)");
                    return false;
                }
            }
        }

        // If all cards must have different controllers
        if (tgt.isDifferentControllers()) {
            final List<Player> targetedControllers = new ArrayList<>();
            for (final GameObject o : targetDepth.keySet()) {
                if (o instanceof Card) {
                    final Player p = ((Card) o).getController();
                    targetedControllers.add(p);
                }
            }
            if (targetedControllers.contains(card.getController())) {
                showMessage(sa.getHostCard() + " - Cannot target this card (must have different controllers)");
                return false;
            }
        }

        // If all cards must have different mana values
        if (tgt.isDifferentCMC()) {
            final List<Integer> targetedCMCs = new ArrayList<>();
            for (final GameObject o : targetDepth.keySet()) {
                if (o instanceof Card) {
                    final Integer cmc = ((Card) o).getCMC();
                    targetedCMCs.add(cmc);
                }
            }
            if (targetedCMCs.contains(card.getCMC())) {
                showMessage(sa.getHostCard() + " - Cannot target this card (must have different mana values)");
                return false;
            }
        }

        if (!choices.contains(card)) {
            showMessage(sa.getHostCard() + " - The selected card is not a valid choice to be targeted.");
            return false;
        }

        if (divisionValues != null && !divisionValues.isEmpty()) {
            Boolean val = onDividedAsYouChoose(card);
            if (val != null) {
                return val;
            }
        }
        addTarget(card);
        return true;
    }

    @Override
    public String getActivateAction(final Card card) {
        if (!tgt.isUniqueTargets() && targetDepth.containsKey(card)) {
            return null;
        }
        if (choices.contains(card)) {
            return "select card as target";
        }
        return null;
    }

    @Override
    protected final void onPlayerSelected(final Player player, final ITriggerEvent triggerEvent) {
        if (!tgt.isUniqueTargets() && targetDepth.containsKey(player)) {
            return;
        }

        if (player.hasLost()) {
            showMessage(sa.getHostCard() + " - Cannot target this player - already lost.");
            return;
        }

        //TODO return the correct reason to display
        if (!sa.canTarget(player) || mustTargetFiltered) {
            showMessage(sa.getHostCard() + " - Cannot target this player (Hexproof? Protection? Restrictions?).");
            return;
        }
        if (filter != null && !filter.apply(player)) {
            showMessage(sa.getHostCard() + " - Cannot target this player (Hexproof? Protection? Restrictions?).");
            return;
        }

        if (divisionValues != null && !divisionValues.isEmpty()) {
            Boolean val = onDividedAsYouChoose(player);
            if (val != null) {
                return;
            }
        }
        addTarget(player);
    }

    protected Boolean onDividedAsYouChoose(GameObject go) {
        String apiBasedMessage = "Distribute how much to ";
        if (sa.getApi() == ApiType.DealDamage) {
            apiBasedMessage = "Select how much damage to deal to ";
        }
        else if (sa.getApi() == ApiType.PreventDamage) {
            apiBasedMessage = "Select how much damage to prevent to ";
        }
        else if (sa.getApi() == ApiType.PutCounter) {
            apiBasedMessage = "Select how many counters to distribute to ";
        }
        final StringBuilder sb = new StringBuilder();
        sb.append(apiBasedMessage);
        sb.append(go.toString());
        final Integer chosen = getController().getGui().oneOrNone(sb.toString(), Lists.newArrayList(divisionValues));
        if (chosen == null) {
            return true; //still return true since there was a valid choice
        }
        divisionValues.remove(chosen);
        sa.addDividedAllocation(go, chosen);
        return null;
    }

    private void addTarget(final GameEntity ge) {
        sa.getTargets().add(ge);
        if (ge instanceof Card) {
            getController().getGui().setUsedToPay(CardView.get((Card) ge), true);
            lastTarget = (Card) ge;
        }
        final Integer val = targetDepth.get(ge);
        targetDepth.put(ge, val == null ? Integer.valueOf(1) : Integer.valueOf(val.intValue() + 1) );

        if (hasAllTargets()) {
            bOk = true;
            this.done();
        } else {
            // If selected one card that is must target, finish this selection, then populate target list again from caller.
            if (ge instanceof Card && mustTargetFiltered) {
                this.done();
            } else {
                this.showMessage();
            }
        }
    }

    private void done() {
        for (final GameEntity c : targetDepth.keySet()) {
            if (c instanceof Card) {
                getController().getGui().setUsedToPay(CardView.get((Card) c), false);
            }
        }

        this.stop();
    }

    private boolean hasAllTargets() {
        return sa.isMaxTargetChosen() || (divisionValues != null && sa.getStillToDivide() == 0)
            || (divisionValues == null && sa.isDividedAsYouChoose() && sa.getTargets().size() == sa.getStillToDivide());
    }

    @Override
    protected void onStop() {
        getController().getGui().clearSelectables();
        super.onStop();
    }

}
