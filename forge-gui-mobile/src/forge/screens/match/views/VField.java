package forge.screens.match.views;

import java.util.ArrayList;
import java.util.List;

import forge.FThreads;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.screens.match.views.VCardDisplayArea.CardAreaPanel;
import forge.toolbox.FContainer;

public class VField extends FContainer {
    private final Player player;
    private final FieldRow row1, row2;
    private boolean flipped;

    public VField(Player player0) {
        player = player0;
        row1 = add(new FieldRow());
        row2 = add(new FieldRow());
    }

    public boolean isFlipped() {
        return flipped;
    }
    public void setFlipped(boolean flipped0) {
        flipped = flipped0;
    }

    public Iterable<CardAreaPanel> getCardPanels() {
        List<CardAreaPanel> cardPanels = new ArrayList<CardAreaPanel>();
        for (CardAreaPanel cardPanel : row1.getCardPanels()) {
            cardPanels.add(cardPanel);
        }
        for (CardAreaPanel cardPanel : row2.getCardPanels()) {
            cardPanels.add(cardPanel);
        }
        return cardPanels;
    }

    public void update() {
        FThreads.invokeInEdtNowOrLater(updateRoutine);
    }

    private final Runnable updateRoutine = new Runnable() {
        @Override
        public void run() {
            clear();

            List<Card> model = player.getZone(ZoneType.Battlefield).getCards();
            for (Card card : model) {
                updateCard(card);
            }

            List<Card> creatures = new ArrayList<Card>();
            List<Card> lands = new ArrayList<Card>();
            List<Card> otherPermanents = new ArrayList<Card>();

            for (Card card : model) {
                CardAreaPanel cardPanel = CardAreaPanel.get(card);
                if (cardPanel.getAttachedToPanel() == null) { //skip attached panels
                    if (card.isCreature()) {
                        if (!tryStackCard(card, creatures)) {
                            creatures.add(card);
                        }
                    }
                    else if (card.isLand()) {
                        if (!tryStackCard(card, lands)) {
                            lands.add(card);
                        }
                    }
                    else {
                        if (!tryStackCard(card, otherPermanents)) {
                            otherPermanents.add(card);
                        }
                    }
                }
            }

            if (creatures.isEmpty()) {
                row1.refreshCardPanels(otherPermanents);
                row2.refreshCardPanels(lands);
            }
            else {
                row1.refreshCardPanels(creatures);
                lands.addAll(otherPermanents);
                row2.refreshCardPanels(lands);
            }
        }
    };

    private boolean tryStackCard(Card card, List<Card> cardsOfType) {
        if (card.isEnchanted() || card.isEquipped()) {
            return false; //can stack with enchanted or equipped card
        }
        if (card.isCreature() && !card.isToken()) {
            return false; //don't stack non-token creatures
        }
        for (Card c : cardsOfType) {
            if (!c.isEnchanted() && !c.isEquipped() &&
                    card.getName().equals(c.getName()) &&
                    card.getCounters().equals(card.getCounters())) {
                CardAreaPanel cPanel = CardAreaPanel.get(c);
                CardAreaPanel cardPanel = CardAreaPanel.get(card);
                cPanel.getAttachedPanels().add(cardPanel);
                cardPanel.setAttachedToPanel(cPanel);
                return true;
            }
        }
        return false;
    }

    public void updateCard(final Card card) {
        final CardAreaPanel toPanel = CardAreaPanel.get(card);
        if (toPanel == null) { return; }

        if (card.isTapped()) {
            toPanel.setTapped(true);
            toPanel.setTappedAngle(CardAreaPanel.TAPPED_ANGLE);
        }
        else {
            toPanel.setTapped(false);
            toPanel.setTappedAngle(0);
        }

        toPanel.getAttachedPanels().clear();
        if (card.isEnchanted()) {
            final ArrayList<Card> enchants = card.getEnchantedBy();
            for (final Card e : enchants) {
                final CardAreaPanel cardE = CardAreaPanel.get(e);
                if (cardE != null) {
                    toPanel.getAttachedPanels().add(cardE);
                }
            }
        }
   
        if (card.isEquipped()) {
            final ArrayList<Card> enchants = card.getEquippedBy();
            for (final Card e : enchants) {
                final CardAreaPanel cardE = CardAreaPanel.get(e);
                if (cardE != null) {
                    toPanel.getAttachedPanels().add(cardE);
                }
            }
        }

        if (card.isFortified()) {
            final ArrayList<Card> fortifications = card.getFortifiedBy();
            for (final Card e : fortifications) {
                final CardAreaPanel cardE = CardAreaPanel.get(e);
                if (cardE != null) {
                    toPanel.getAttachedPanels().add(cardE);
                }
            }
        }

        if (card.isEnchantingCard()) {
            toPanel.setAttachedToPanel(CardAreaPanel.get(card.getEnchantingCard()));
        }
        else if (card.isEquipping()) {
            toPanel.setAttachedToPanel(CardAreaPanel.get(card.getEquipping().get(0)));
        }
        else if (card.isFortifying()) {
            toPanel.setAttachedToPanel(CardAreaPanel.get(card.getFortifying().get(0)));
        }
        else {
            toPanel.setAttachedToPanel(null);
        }

        toPanel.setCard(toPanel.getCard());
    }

    public FieldRow getRow1() {
        return row1;
    }

    public FieldRow getRow2() {
        return row2;
    }

    @Override
    public void clear() {
        row1.clear(); //clear rows instead of removing the rows
        row2.clear();
    }

    @Override
    protected void doLayout(float width, float height) {
        float cardSize = height / 2;
        float y1, y2;
        if (flipped) {
            y1 = cardSize;
            y2 = 0;
        }
        else {
            y1 = 0;
            y2 = cardSize;
        }
        row1.setBounds(0, y1, width, cardSize);
        row2.setBounds(0, y2, width, cardSize);
    }

    public class FieldRow extends VCardDisplayArea {
        private FieldRow() {
            setVisible(true); //make visible by default unlike other display areas
        }

        @Override
        protected float getCardWidth(float cardHeight) {
            return cardHeight; //allow cards room to tap
        }

        @Override
        public void update() { //no logic needed
        }
    }
}
