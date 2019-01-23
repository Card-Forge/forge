package forge.match.input;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.card.CardView;

import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.player.PlayerControllerHuman;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;
import forge.util.ITriggerEvent;
import forge.player.PlayerZoneUpdate;
import forge.player.PlayerZoneUpdates;
import forge.game.zone.Zone;
import forge.FThreads;

public class InputSelectFromTwoLists<T extends GameEntity> extends InputSelectManyBase<T> {
    private final FCollectionView<T> valid1, valid2;
    private final FCollection<T> validBoth;
    private FCollectionView<T> validChoices;

    protected final FCollection<T> selected = new FCollection<T>();
    protected final PlayerZoneUpdates zonesToUpdate = new PlayerZoneUpdates();
    protected Iterable<PlayerZoneUpdate> zonesShown; // want to hide these zones when input done

    public InputSelectFromTwoLists(final PlayerControllerHuman controller, final boolean optional,
				   final FCollectionView<T> list1, final FCollectionView<T> list2, final SpellAbility sa0) {
        super(controller, optional?0:1, 2, sa0);
	valid1 = list1;
	valid2 = list2;
	validBoth = new FCollection<T>(valid1);
	for ( T v : valid2 ) { validBoth.add(v); }
	validChoices = validBoth;
	setSelectables();

	for (final GameEntity c : validChoices) {
            final Zone cz = (c instanceof Card) ? ((Card) c).getZone() : null ;
	    if ( cz != null ) {
		zonesToUpdate.add(new PlayerZoneUpdate(cz.getPlayer().getView(),cz.getZoneType()));
	    }
	}
	FThreads.invokeInEdtNowOrLater(new Runnable() {
            @Override public void run() {
		controller.getGui().updateZones(zonesToUpdate);  
		zonesShown = controller.getGui().tempShowZones(controller.getPlayer().getView(),zonesToUpdate);  
            }
	    });
    }
    
    private void setSelectables() {
	ArrayList<CardView> vCards = new ArrayList<CardView>();
	getController().getGui().clearSelectables();
	for ( T c : validChoices ) {
	    if ( c instanceof Card ) {
		vCards.add(((Card)c).getView()) ;
	    }
	}
	getController().getGui().setSelectables(vCards);
    }

    private void setValid() {
	boolean selected1 = false, selected2 = false;
	for ( T s : selected ) {
	    if ( valid1.contains(s) ) { selected1 = true; }
	    if ( valid2.contains(s) ) { selected2 = true; }
	}
	validChoices = selected1 ? ( selected2 ? FCollection.<T>getEmpty() : valid2 ) : ( selected2 ? valid1 : validBoth );
	setSelectables();
	FThreads.invokeInEdtNowOrLater(new Runnable() {
            @Override public void run() {
		getController().getGui().updateZones(zonesToUpdate);  
            }
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
        if (!validChoices.contains(c) && !selected.contains(c)) {
            return false;
        }

        final boolean entityWasSelected = selected.contains(c);
        if (entityWasSelected) {
            selected.remove(c);
        }
        else {
            selected.add((T)c);
        }
	setValid();
        onSelectStateChanged(c, !entityWasSelected);

        return true;
    }

    // might re-define later
    @Override
    protected boolean hasEnoughTargets() { return selected.size() >= min; }
    @Override
    protected boolean hasAllTargets() { return selected.size() >= max; }

    @Override
    protected String getMessage() {
        return max == Integer.MAX_VALUE
                ? String.format(message, selected.size())
                : String.format(message, max - selected.size());
    }

    @Override
    protected void onStop() {
	getController().getGui().hideZones(getController().getPlayer().getView(),zonesShown);  
	getController().getGui().clearSelectables();
	super.onStop();
    }
}
