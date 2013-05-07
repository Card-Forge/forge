package forge.gui.match.nonsingleton;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import forge.Card;
import forge.CardCharacteristicName;
import forge.Singletons;
import forge.card.cardfactory.CardFactory;
import forge.game.zone.PlayerZone;
import forge.gui.ForgeAction;
import forge.gui.GuiChoose;

/**
 * Receives click and programmatic requests for viewing data stacks in the
 * "zones" of a player field: hand, library, etc.
 * 
 */
class ZoneAction extends ForgeAction {
    private static final long serialVersionUID = -5822976087772388839L;
    private final PlayerZone zone;
    private final String title;

    /**
     * Receives click and programmatic requests for viewing data stacks in
     * the "zones" of a player field: hand, graveyard, etc. The library
     * "zone" is an exception to the rule; it's handled in DeckListAction.
     * 
     * @param zone
     *            &emsp; PlayerZone obj
     * @param property
     *            &emsp; String obj
     */
    public ZoneAction(final PlayerZone zone, MatchConstants property) {
        super(property);
        this.title = property.title;
        this.zone = zone;
    }

    /**
     * @param e
     *            &emsp; ActionEvent obj
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        final List<Card> choices = this.getCardsAsIterable();
        if (choices.isEmpty()) {
            GuiChoose.oneOrNone(this.title, new String[] { "no cards" });
            return;
        } 

        final ArrayList<Card> choices2 = new ArrayList<Card>();
        for (Card crd : choices) {
            Card toAdd = crd;
            if (crd.isFaceDown()) {
                if (crd.canBeShownTo(Singletons.getControl().getPlayer())) {
                    toAdd = CardFactory.copyCard(crd);
                    toAdd.setState(CardCharacteristicName.Original);
                } else {
                    toAdd = new Card();
                    toAdd.setName("Face Down");
                }
            } 
            choices2.add(toAdd);
        }

        final Card choice = GuiChoose.oneOrNone(this.title, choices2);
        if (choice != null) {
            this.doAction(choice);
        }
    }

    protected List<Card> getCardsAsIterable() {
        return this.zone.getCards();
    }

    protected void doAction(final Card c) {
    }
} // End ZoneAction