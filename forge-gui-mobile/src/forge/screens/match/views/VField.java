package forge.screens.match.views;

import java.util.ArrayList;
import java.util.List;

import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.game.player.PlayerView;
import forge.gui.FThreads;
import forge.screens.match.views.VCardDisplayArea.CardAreaPanel;
import forge.toolbox.FContainer;

public class VField extends FContainer {
    private final PlayerView player;
    private final FieldRow row1, row2;
    private boolean flipped;
    private float commandZoneWidth;
    private float fieldModifier;

    public VField(PlayerView player0) {
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
        List<CardAreaPanel> cardPanels = new ArrayList<>();
        for (CardAreaPanel cardPanel : row1.getCardPanels()) {
            cardPanels.add(cardPanel);
        }
        for (CardAreaPanel cardPanel : row2.getCardPanels()) {
            cardPanels.add(cardPanel);
        }
        return cardPanels;
    }

    public void update(boolean invokeInEdtNowOrLater) {
        if (invokeInEdtNowOrLater)
            FThreads.invokeInEdtNowOrLater(updateRoutine);
        else
            FThreads.invokeInEdtLater(updateRoutine);
    }

    private final Runnable updateRoutine = new Runnable() {
        @Override
        public void run() {
            clear();

            Iterable<CardView> model = player.getBattlefield();
            if (model == null) { return; }

            for (CardView card : model) {
                CardAreaPanel cardPanel = CardAreaPanel.get(card);
                cardPanel.updateCard(card);
                // Clear all stacks since they will be rebuilt in the loop below.
                cardPanel.setNextPanelInStack(null);
                cardPanel.setPrevPanelInStack(null);
            }

            List<CardView> creatures = new ArrayList<>();
            List<CardView> lands = new ArrayList<>();
            List<CardView> otherPermanents = new ArrayList<>();

            for (CardView card : model) {
                CardAreaPanel cardPanel = CardAreaPanel.get(card);
                CardStateView details = card.getCurrentState();
                if (cardPanel.getAttachedToPanel() == null) { //skip attached panels
                    if (details.isCreature()) {
                        if (!tryStackCard(card, creatures)) {
                            creatures.add(card);
                        }
                    }
                    else if (details.isLand()) {
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

    private boolean tryStackCard(CardView card, List<CardView> cardsOfType) {
        if (card.hasCardAttachments()) {
            return false; //can stack with enchanted or equipped card
        }
        if (card.getCurrentState().isCreature() && !card.isToken()) {
            return false; //don't stack non-token creatures
        }
        final String cardName = card.getCurrentState().getName();
        for (CardView c : cardsOfType) {
            if (c.getCurrentState().isCreature()) {
                if (!c.hasCardAttachments() &&
                        cardName.equals(c.getCurrentState().getName()) &&
                        card.hasSameCounters(c) &&
                        card.hasSamePT(c) && //don't stack token with different PT
                        card.getCurrentState().getKeywordKey().equals(c.getCurrentState().getKeywordKey()) &&
                        card.isTapped() == c.isTapped() && // don't stack tapped tokens on untapped tokens
                        card.isSick() == c.isSick() && //don't stack sick tokens on non sick
                        card.isToken() == c.isToken()) { //don't stack tokens on top of non-tokens
                    CardAreaPanel cPanel = CardAreaPanel.get(c);
                    while (cPanel.getNextPanelInStack() != null) {
                        cPanel = cPanel.getNextPanelInStack();
                    }
                    CardAreaPanel cardPanel = CardAreaPanel.get(card);
                    cPanel.setNextPanelInStack(cardPanel);
                    cardPanel.setPrevPanelInStack(cPanel);
                    return true;
                }
            } else {
                if (!c.hasCardAttachments() &&
                        cardName.equals(c.getCurrentState().getName()) &&
                        card.hasSameCounters(c) &&
                        card.getCurrentState().getKeywordKey().equals(c.getCurrentState().getKeywordKey()) &&
                        card.getCurrentState().getColors() == c.getCurrentState().getColors() &&
                        card.isSick() == c.isSick() && //don't stack sick tokens on non sick
                        card.isToken() == c.isToken()) { //don't stack tokens on top of non-tokens
                    CardAreaPanel cPanel = CardAreaPanel.get(c);
                    while (cPanel.getNextPanelInStack() != null) {
                        cPanel = cPanel.getNextPanelInStack();
                    }
                    CardAreaPanel cardPanel = CardAreaPanel.get(card);
                    cPanel.setNextPanelInStack(cardPanel);
                    cardPanel.setPrevPanelInStack(cPanel);
                    return true;
                }
            }
        }
        return false;
    }

    public FieldRow getRow1() {
        return row1;
    }

    public FieldRow getRow2() {
        return row2;
    }

    void setCommandZoneWidth(float commandZoneWidth0) {
        commandZoneWidth = commandZoneWidth0;
    }

    void setFieldModifier(float fieldModifierWidth) {
        fieldModifier = fieldModifierWidth;
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
        row1.setBounds(0, y1, width-fieldModifier, cardSize);
        row2.setBounds(0, y2, (width - commandZoneWidth)-fieldModifier, cardSize);
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
