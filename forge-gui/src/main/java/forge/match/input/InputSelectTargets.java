package forge.match.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import forge.game.GameEntity;
import forge.game.GameObject;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.match.MatchUtil;
import forge.player.PlayerControllerHuman;
import forge.util.ITriggerEvent;
import forge.util.gui.SGuiChoose;


public final class InputSelectTargets extends InputSyncronizedBase {
    private final List<Card> choices;
    // some cards can be targeted several times (eg: distribute damage as you choose)
    private final Map<GameEntity, Integer> targetDepth = new HashMap<GameEntity, Integer>();
    private final TargetRestrictions tgt;
    private final SpellAbility sa;
    private Card lastTarget = null;
    private boolean bCancel = false;
    private boolean bOk = false;
    private final boolean mandatory;
    private static final long serialVersionUID = -1091595663541356356L;

    public final boolean hasCancelled() { return bCancel; }
    public final boolean hasPressedOk() { return bOk; }

    public InputSelectTargets(final PlayerControllerHuman controller, final List<Card> choices, final SpellAbility sa, final boolean mandatory) {
        super(controller);
        this.choices = choices;
        this.tgt = sa.getTargetRestrictions();
        this.sa = sa;
        this.mandatory = mandatory;
    }

    @Override
    public void showMessage() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Targeted:\n");
        for (final Entry<GameEntity, Integer> o : targetDepth.entrySet()) {
            sb.append(o.getKey());
            if( o.getValue() > 1 )
                sb.append(" (").append(o.getValue()).append(" times)");
           sb.append("\n");
        }
        if (!sa.getUniqueTargets().isEmpty()) {
            sb.append("Parent Targeted:");
            sb.append(sa.getUniqueTargets()).append("\n");
        }
        sb.append(sa.getHostCard() + " - " + tgt.getVTSelection());
        
        int maxTargets = tgt.getMaxTargets(sa.getHostCard(), sa);
        int targeted = sa.getTargets().getNumTargeted();
        if(maxTargets > 1)
            sb.append("\n(").append(maxTargets - targeted).append(" more can be targeted)");

        showMessage(sb.toString());

        // If reached Minimum targets, enable OK button
        if (!tgt.isMinTargetsChosen(sa.getHostCard(), sa) || tgt.isDividedAsYouChoose()) {
            if (mandatory && tgt.hasCandidates(sa, true)) {
                // Player has to click on a target
                ButtonUtil.update(getOwner(), false, false, false);
            }
            else {
                ButtonUtil.update(getOwner(), false, true, false);
            }
        }
        else {
            if (mandatory && tgt.hasCandidates(sa, true)) {
                // Player has to click on a target or ok
                ButtonUtil.update(getOwner(), true, false, true);
            }
            else {
                ButtonUtil.update(getOwner(), true, true, true);
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
    protected final boolean onCardSelected(final Card card, final ITriggerEvent triggerEvent) {
        if (!tgt.isUniqueTargets() && targetDepth.containsKey(card)) {
            return false;
        }
        
        // leave this in temporarily, there some seriously wrong things going on here
        // Can be targeted doesn't check if the target is a valid type, only if a card is generally "targetable"
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

        // If all cards must have different controllers
        if (tgt.isDifferentControllers()) {
            final List<Player> targetedControllers = new ArrayList<Player>();
            for (GameObject o : targetDepth.keySet()) {
                if (o instanceof Card) {
                    Player p = ((Card) o).getController();
                    targetedControllers.add(p);
                }
            }
            if (targetedControllers.contains(card.getController())) {
                showMessage(sa.getHostCard() + " - Cannot target this card (must have different controllers)");
                return false;
            }
        }

        if (!choices.contains(card)) {
            if (card.isPlaneswalker() && sa.getApi() == ApiType.DealDamage) {
                showMessage(sa.getHostCard() + " - To deal an opposing Planeswalker direct damage, target its controller and then redirect the damage on resolution.");
            }
            else {
                showMessage(sa.getHostCard() + " - The selected card is not a valid choice to be targeted.");
            }
            return false;
        }
        
        if (tgt.isDividedAsYouChoose()) {
            final int stillToDivide = tgt.getStillToDivide();
            int allocatedPortion = 0;
            // allow allocation only if the max targets isn't reached and there are more candidates
            if ((sa.getTargets().getNumTargeted() + 1 < tgt.getMaxTargets(sa.getHostCard(), sa))
                    && (tgt.getNumCandidates(sa, true) - 1 > 0) && stillToDivide > 1) {
                final Integer[] choices = new Integer[stillToDivide];
                for (int i = 1; i <= stillToDivide; i++) {
                    choices[i - 1] = i;
                }
                String apiBasedMessage = "Distribute how much to ";
                if (sa.getApi() == ApiType.DealDamage) {
                    apiBasedMessage = "Select how much damage to deal to ";
                } else if (sa.getApi() == ApiType.PreventDamage) {
                    apiBasedMessage = "Select how much damage to prevent to ";
                } else if (sa.getApi() == ApiType.PutCounter) {
                    apiBasedMessage = "Select how many counters to distribute to ";
                }
                final StringBuilder sb = new StringBuilder();
                sb.append(apiBasedMessage);
                sb.append(card.toString());
                Integer chosen = SGuiChoose.oneOrNone(sb.toString(), choices);
                if (chosen == null) {
                    return true; //still return true since there was a valid choice
                }
                allocatedPortion = chosen;
            } else { // otherwise assign the rest of the damage/protection
                allocatedPortion = stillToDivide;
            }
            tgt.setStillToDivide(stillToDivide - allocatedPortion);
            tgt.addDividedAllocation(card, allocatedPortion);
        }
        addTarget(card);
        return true;
    } // selectCard()

    @Override
    protected final void onPlayerSelected(Player player, final ITriggerEvent triggerEvent) {
        if (!tgt.isUniqueTargets() && targetDepth.containsKey(player)) {
            return;
        }

        if (!sa.canTarget(player)) {
            showMessage(sa.getHostCard() + " - Cannot target this player (Hexproof? Protection? Restrictions?).");
            return;
        }
        
        if (tgt.isDividedAsYouChoose()) {
            final int stillToDivide = tgt.getStillToDivide();
            int allocatedPortion = 0;
            // allow allocation only if the max targets isn't reached and there are more candidates
            if ((sa.getTargets().getNumTargeted() + 1 < tgt.getMaxTargets(sa.getHostCard(), sa)) && (tgt.getNumCandidates(sa, true) - 1 > 0) && stillToDivide > 1) {
                final Integer[] choices = new Integer[stillToDivide];
                for (int i = 1; i <= stillToDivide; i++) {
                    choices[i - 1] = i;
                }
                String apiBasedMessage = "Distribute how much to ";
                if (sa.getApi() == ApiType.DealDamage) {
                    apiBasedMessage = "Select how much damage to deal to ";
                } else if (sa.getApi() == ApiType.PreventDamage) {
                    apiBasedMessage = "Select how much damage to prevent to ";
                }
                final StringBuilder sb = new StringBuilder();
                sb.append(apiBasedMessage);
                sb.append(player.getName());
                Integer chosen = SGuiChoose.oneOrNone(sb.toString(), choices);
                if (null == chosen) {
                    return;
                }
                allocatedPortion = chosen;
            } else { // otherwise assign the rest of the damage/protection
                allocatedPortion = stillToDivide;
            }
            tgt.setStillToDivide(stillToDivide - allocatedPortion);
            tgt.addDividedAllocation(player, allocatedPortion);
        }
        addTarget(player);
    }

    private void addTarget(final GameEntity ge) {
        sa.getTargets().add(ge);
        if (ge instanceof Card) {
            MatchUtil.setUsedToPay(getController().getCardView((Card) ge), true);
            lastTarget = (Card) ge;
        }
        final Integer val = targetDepth.get(ge);
        targetDepth.put(ge, val == null ? Integer.valueOf(1) : Integer.valueOf(val.intValue() + 1) );
        
        if(hasAllTargets()) {
            bOk = true;
            this.done();
        }
        else
            this.showMessage();
    }
    
    private void done() {
        for (final GameEntity c : targetDepth.keySet()) {
            if (c instanceof Card) {
                MatchUtil.setUsedToPay(getController().getCardView((Card) c), false);
            }
        }

        this.stop();
    }
    
    private boolean hasAllTargets() {
        return tgt.isMaxTargetsChosen(sa.getHostCard(), sa) || ( tgt.getStillToDivide() == 0 && tgt.isDividedAsYouChoose());
    }
}