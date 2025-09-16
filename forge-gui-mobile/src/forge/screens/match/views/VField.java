package forge.screens.match.views;

import java.util.*;

import forge.Forge;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.game.player.PlayerView;
import forge.gui.FThreads;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.screens.match.MatchScreen;
import forge.screens.match.views.VCardDisplayArea.CardAreaPanel;
import forge.toolbox.FCardPanel;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;

public class VField extends FContainer {
    private final PlayerView player;
    private final FieldRow row1, row2;
    private boolean flipped;
    private float commandZoneWidth;
    private float fieldModifier;
    private final boolean stackNonTokenCreatures;

    public VField(PlayerView player0) {
        player = player0;
        row1 = add(new FieldRow());
        row2 = add(new FieldRow());

        stackNonTokenCreatures = FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_STACK_CREATURES);
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
            if (model == null) {
                return;
            }

            for (CardView card : model) {
                CardAreaPanel cardPanel = CardAreaPanel.get(card);
                cardPanel.updateCard(card);
                // Clear all stacks since they will be rebuilt in the loop below.
                cardPanel.setNextPanelInStack(null);
                cardPanel.setPrevPanelInStack(null);
            }

            List<CardView> creatures = new ArrayList<>();
            List<CardView> lands = new ArrayList<>();
            List<CardView> contraptions = null; //Usually not used; create on demand.
            List<CardView> otherPermanents = new ArrayList<>();

            for (CardView card : model) {
                CardAreaPanel cardPanel = CardAreaPanel.get(card);
                CardStateView details = card.getCurrentState();
                if (cardPanel.getAttachedToPanel() == null) { //skip attached panels
                    if (details.isCreature()) {
                        if (!tryStackCard(card, creatures)) {
                            creatures.add(card);
                        }
                    } else if (details.isLand()) {
                        if (!tryStackCard(card, lands)) {
                            lands.add(card);
                        }
                    } else if (details.isArtifact() && (details.isContraption() || details.isAttraction())) {
                        if (contraptions == null)
                            contraptions = new ArrayList<>();
                        contraptions.add(card); //Arrange these later.
                    } else {
                        if (!tryStackCard(card, otherPermanents)) {
                            otherPermanents.add(card);
                        }
                    }
                }
            }

            if (contraptions != null) {
                contraptions = arrangeContraptions(contraptions);
                otherPermanents.addAll(contraptions);
            }

            if (creatures.isEmpty()) {
                row1.refreshCardPanels(otherPermanents);
                row2.refreshCardPanels(lands);
            } else {
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
        CardStateView cardState = card.getCurrentState();
        if (!this.stackNonTokenCreatures && cardState.isCreature() && !card.isToken()) {
            return false;
        }
        final String cardName = cardState.getName();
        for (CardView c : cardsOfType) {
            CardStateView cState = c.getCurrentState();
            if (cState.isCreature()) {
                if (!c.hasCardAttachments() &&
                        cardName.equals(cState.getName()) &&
                        card.hasSameCounters(c) &&
                        card.hasSamePT(c) && //don't stack token with different PT
                        cardState.getKeywordKey().equals(cState.getKeywordKey()) &&
                        card.isTapped() == c.isTapped() && // don't stack tapped tokens on untapped tokens
                        card.isSick() == c.isSick() && //don't stack sick tokens on non sick
                        card.isToken() == c.isToken()) { //don't stack tokens on top of non-tokens
                    stackOnto(card, c);
                    return true;
                }
            } else {
                if (!c.hasCardAttachments() &&
                        cardName.equals(cState.getName()) &&
                        card.hasSameCounters(c) &&
                        cardState.getKeywordKey().equals(cState.getKeywordKey()) &&
                        cardState.getColors() == cState.getColors() &&
                        card.isSick() == c.isSick() && //don't stack sick tokens on non sick
                        card.isToken() == c.isToken()) { //don't stack tokens on top of non-tokens
                    stackOnto(card, c);
                    return true;
                }
            }
        }
        return false;
    }

    private static void stackOnto(CardView card, CardView topOfStack) {
        CardAreaPanel cPanel = CardAreaPanel.get(topOfStack);
        while (cPanel.getNextPanelInStack() != null) {
            cPanel = cPanel.getNextPanelInStack();
        }
        CardAreaPanel cardPanel = CardAreaPanel.get(card);
        cPanel.setNextPanelInStack(cardPanel);
        cardPanel.setPrevPanelInStack(cPanel);
    }

    private List<CardView> arrangeContraptions(List<CardView> contraptions) {
        TreeSet<CardView> row = new TreeSet<>((c1, c2) -> {
            //Order is sprocket-less cards, then sprocket 1, sprocket 2, sprocket 3, and finally attractions.
            int sprocket1 = c1.getSprocket(), sprocket2 = c2.getSprocket();
            if (sprocket1 == 0 && c1.getCurrentState().isAttraction())
                sprocket1 = 4;
            if (sprocket2 == 0 && c2.getCurrentState().isAttraction())
                sprocket2 = 4;
            return sprocket1 - sprocket2;
        });
        outer:
        for (CardView card : contraptions) {
            if (card.hasCardAttachments()) {
                row.add(card); //Don't stack contraptions or attractions with attachments.
                continue;
            }
            if (card.getCurrentState().isAttraction()) {
                //Stack attractions with other attractions.
                for (CardView c : row) {
                    if (c.getCurrentState().isAttraction() && !c.hasCardAttachments()) {
                        stackOnto(card, c);
                        continue outer;
                    }
                }
                row.add(card);
                continue;
            }
            if (card.getSprocket() <= 0) {
                //Sprocket-less contraptions don't stack. They're probably awaiting an SBA that assembles them onto one.
                row.add(card);
                continue;
            }
            for (CardView c : row) {
                if (c.getSprocket() == card.getSprocket() && !c.hasCardAttachments()) {
                    stackOnto(card, c);
                    continue outer;
                }
            }
            row.add(card);
        }
        return List.copyOf(row);
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
        } else {
            y1 = 0;
            y2 = cardSize;
        }
        if (Forge.altZoneTabs && "Horizontal".equalsIgnoreCase(Forge.altZoneTabMode)) {
            row1.setBounds(0, y1, width, cardSize);
            row2.setBounds(0, y2, width, cardSize);
        } else {
            row1.setBounds(0, y1, width - fieldModifier, cardSize);
            row2.setBounds(0, y2, (width - commandZoneWidth) - fieldModifier, cardSize);
        }
    }

    public class FieldRow extends VCardDisplayArea {
        private int selected = -1;
        private FDisplayObject selectedChild;

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

        @Override
        public void setNextSelected(int val) {
            this.selected++;
            if (this.selected >= this.getChildCount())
                this.selected = this.getChildCount() - 1;
            if (this.selectedChild != null)
                this.selectedChild.setHovered(false);
            this.selectedChild = getChildAt(this.selected);
            this.selectedChild.setHovered(true);
            MatchScreen.setPotentialListener(Arrays.asList(this.selectedChild));
        }

        public void selectCurrent() {
            if (this.selectedChild != null) {
                this.selectedChild.setHovered(true);
                MatchScreen.setPotentialListener(Arrays.asList(this.selectedChild));
            } else {
                this.setNextSelected(1);
            }
        }

        public void unselectCurrent() {
            if (this.selectedChild != null) {
                this.selectedChild.setHovered(false);
                MatchScreen.nullPotentialListener();
            }
        }

        @Override
        public void setPreviousSelected(int val) {
            if (this.getChildCount() < 1)
                return;
            this.selected--;
            if (this.selected < 0)
                this.selected = 0;
            if (this.selectedChild != null)
                this.selectedChild.setHovered(false);
            this.selectedChild = getChildAt(this.selected);
            this.selectedChild.setHovered(true);
            MatchScreen.setPotentialListener(Arrays.asList(this.selectedChild));
        }

        @Override
        public void showZoom() {
            if (this.selectedChild instanceof FCardPanel)
                VCardDisplayArea.CardAreaPanel.get(((FCardPanel) this.selectedChild).getCard()).showZoom();
        }

        @Override
        public void tapChild() {
            if (this.selectedChild instanceof FCardPanel)
                VCardDisplayArea.CardAreaPanel.get(((FCardPanel) this.selectedChild).getCard()).selectCard(false);
        }
    }
}
