/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Nate
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.view.arcane;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

import forge.CachedCardImage;
import forge.FThreads;
import forge.card.CardEdition;
import forge.card.mana.ManaCost;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.gui.CardContainer;
import forge.model.FModel;
import forge.properties.ForgePreferences.FPref;
import forge.screens.match.CMatchUI;
import forge.toolbox.CardFaceSymbols;
import forge.toolbox.FSkin.SkinnedPanel;
import forge.toolbox.IDisposable;
import forge.view.arcane.util.OutlinedLabel;

/**
 * <p>
 * CardPanel class.
 * </p>
 *
 * @author Forge
 * @version $Id: CardPanel.java 25264 2014-03-27 01:59:18Z drdev $
 */
public class CardPanel extends SkinnedPanel implements CardContainer, IDisposable {
    private static final long serialVersionUID = 2361907095724263295L;

    public static final double TAPPED_ANGLE = Math.PI / 2;
    public static final float ASPECT_RATIO = 3.5f / 2.5f;
    public static final float TARGET_ORIGIN_FACTOR_X = 0.25f;
    public static final float TARGET_ORIGIN_FACTOR_Y = 0.5f;
    private static CardPanel dragAnimationPanel;
    public static final float ROUNDED_CORNER_SIZE = 0.1f;
    public static final float SELECTED_BORDER_SIZE = 0.01f;
    public static final float BLACK_BORDER_SIZE = 0.03f;
    private static final float ROT_CENTER_TO_TOP_CORNER = 1.0295630140987000315797369464196f;
    private static final float ROT_CENTER_TO_BOTTOM_CORNER = 0.7071067811865475244008443621048f;

    private final CMatchUI matchUI;
    private CardView card;
    private CardPanel attachedToPanel;
    private List<CardPanel> attachedPanels = new ArrayList<CardPanel>();
    private List<CardPanel> stack;
    private boolean tapped;
    private double tappedAngle = 0;
    private ScaledImagePanel imagePanel;

    private OutlinedLabel titleText;
    private OutlinedLabel ptText;
    private OutlinedLabel damageText;
    private OutlinedLabel cardIdText;
    private boolean displayEnabled = true;
    private boolean isAnimationPanel;
    private int cardXOffset, cardYOffset, cardWidth, cardHeight;
    private boolean isSelected;
    private CachedCardImage cachedImage;

    public CardPanel(final CMatchUI matchUI, final CardView card0) {
        this.matchUI = matchUI;

        setBackground(Color.black);
        setOpaque(false);

        createCardNameOverlay();
        createPTOverlay();
        createCardIdOverlay();
        createScaleImagePanel();

        setCard(card0);
    }

    public CMatchUI getMatchUI() {
        return matchUI;
    }

    private void createScaleImagePanel() {
        imagePanel = new ScaledImagePanel();
        add(imagePanel);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(final ComponentEvent e) {
                CardPanel.this.setCard(CardPanel.this.getCard());
            }
            @Override
            public void componentResized(final ComponentEvent e) {
                CardPanel.this.setCard(CardPanel.this.getCard());
            }
        });
    }

    private void createCardNameOverlay() {
        titleText = new OutlinedLabel();
        titleText.setFont(getFont().deriveFont(Font.BOLD, 13f));
        titleText.setForeground(Color.white);
        titleText.setGlow(Color.black);
        titleText.setWrap(true);
        add(titleText);
    }

    private void createPTOverlay() {
        // Power/Toughness
        ptText = new OutlinedLabel();
        ptText.setFont(getFont().deriveFont(Font.BOLD, 13f));
        ptText.setForeground(Color.white);
        ptText.setGlow(Color.black);
        add(ptText);

        // Damage
        damageText = new OutlinedLabel();
        damageText.setFont(getFont().deriveFont(Font.BOLD, 15f));
        damageText.setForeground(new Color(160,0,0));
        damageText.setGlow(Color.white);
        add(damageText);
    }

    private void createCardIdOverlay() {
        cardIdText = new OutlinedLabel();
        cardIdText.setFont(getFont().deriveFont(Font.BOLD, 11f));
        cardIdText.setForeground(Color.LIGHT_GRAY);
        cardIdText.setGlow(Color.black);
        add(cardIdText);
    }

    private void updateImage() {
        FThreads.assertExecutedByEdt(true);

        if (card == null)  {
            cachedImage = null;
            setImage(null);
            return;
        }

        cachedImage = new CachedCardImage(card, matchUI.getLocalPlayers(), imagePanel.getWidth(), imagePanel.getHeight()) {
            @Override
            public void onImageFetched() {
                if (cachedImage != null) {
                    setImage(cachedImage.getImage());
                }
            }
        };
        setImage(cachedImage.getImage());
    }

    private void setImage(final BufferedImage srcImage) {
        if (imagePanel == null || imagePanel.getSrcImage() == srcImage) {
            return;
        }
        imagePanel.setImage(srcImage);
        repaint();
    }

    public final boolean isDisplayEnabled() {
        return displayEnabled;
    }
    public final void setDisplayEnabled(final boolean displayEnabled0) {
        displayEnabled = displayEnabled0;
    }

    public final void setAnimationPanel(final boolean isAnimationPanel0) {
        isAnimationPanel = isAnimationPanel0;
    }

    public final boolean isSelected() {
        return isSelected;
    }
    public final void setSelected(final boolean isSelected0) {
        isSelected = isSelected0;
        repaint();
    }

    @Override
    public final void paint(final Graphics g) {
        if (!displayEnabled) {
            return;
        }
        if (!isValid()) {
            super.validate();
        }
        Graphics2D g2d = (Graphics2D) g;
        if (getTappedAngle() > 0) {
            g2d = (Graphics2D) g2d.create();
            final float edgeOffset = cardWidth / 2f;
            g2d.rotate(getTappedAngle(), cardXOffset + edgeOffset, (cardYOffset + cardHeight)
                    - edgeOffset);
        }
        super.paint(g2d);
    }

    @Override
    protected final void paintComponent(final Graphics g) {
        final Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        final int cornerSize = Math.max(4, Math.round(cardWidth * CardPanel.ROUNDED_CORNER_SIZE));
        final int offset = isTapped() ? 1 : 0;

        // Magenta outline for when card was chosen to pay
        if (matchUI.isUsedToPay(getCard())) {
            g2d.setColor(Color.magenta);
            final int n2 = Math.max(1, Math.round(2 * cardWidth * CardPanel.SELECTED_BORDER_SIZE));
            g2d.fillRoundRect(cardXOffset - n2, (cardYOffset - n2) + offset, cardWidth + (n2 * 2), cardHeight + (n2 * 2), cornerSize + n2, cornerSize + n2);
        }

        // Green outline for hover
        if (isSelected) {
            g2d.setColor(Color.green);
            final int n = Math.max(1, Math.round(cardWidth * CardPanel.SELECTED_BORDER_SIZE));
            g2d.fillRoundRect(cardXOffset - n, (cardYOffset - n) + offset, cardWidth + (n * 2), cardHeight + (n * 2), cornerSize + n , cornerSize + n);
        }

        // Black fill - (will become outline for white bordered cards)
        final int n = 0;
        g2d.setColor(Color.black);
        g2d.fillRoundRect(cardXOffset - n, (cardYOffset - n) + offset, cardWidth + (n * 2), cardHeight + (n * 2), cornerSize + n , cornerSize + n);

        // White border if card is known to have it.
        if (getCard() != null && matchUI.mayView(getCard())) {
            final CardStateView state = getCard().getCurrentState();
            final CardEdition ed = FModel.getMagicDb().getEditions().get(state.getSetCode());
            boolean colorIsSet = false;
            if (state.getType().isEmblem() || state.getType().hasStringType("Effect")) {
                // Effects are drawn with orange border
                g2d.setColor(Color.ORANGE);
                colorIsSet = true;
            } else if (ed != null && ed.isWhiteBorder() && state.getFoilIndex() == 0) {
                // Non-foil cards from white-bordered sets are drawn with white border
                g2d.setColor(Color.WHITE);
                colorIsSet = true;
            }
            if (colorIsSet) {
                final int ins = 1;
                g2d.fillRoundRect(cardXOffset + ins, cardYOffset + ins, cardWidth - ins*2, cardHeight - ins*2, cornerSize-ins, cornerSize-ins);
            }
        }
    }

    private void drawManaCost(final Graphics g, final ManaCost cost, final int deltaY) {
        final int width = CardFaceSymbols.getWidth(cost);
        final int height = CardFaceSymbols.getHeight();
        CardFaceSymbols.draw(g, cost, (cardXOffset + (cardWidth / 2)) - (width / 2), deltaY + cardYOffset + (cardHeight / 2) - height/2);
    }

    @Override
    protected final void paintChildren(final Graphics g) {
        super.paintChildren(g);

        if (isAnimationPanel || card == null) {
            return;
        }

        final boolean canShow = matchUI.mayView(card);
        displayIconOverlay(g, canShow);
        if (canShow) {
            drawFoilEffect(g, card, cardXOffset, cardYOffset,
                    cardWidth, cardHeight, Math.round(cardWidth * BLACK_BORDER_SIZE));
        }
    }

    public static void drawFoilEffect(final Graphics g, final CardView card2, final int x, final int y, final int width, final int height, final int borderSize) {
        if (isPreferenceEnabled(FPref.UI_OVERLAY_FOIL_EFFECT)) {
            final int foil = card2.getCurrentState().getFoilIndex();
            if (foil > 0) {
                CardFaceSymbols.drawOther(g, String.format("foil%02d", foil),
                        x + borderSize, y + borderSize, width - 2 * borderSize, height - 2 * borderSize);
            }
        }
    }

    @Override
    public final void doLayout() {
        final int borderSize = Math.round(cardWidth * CardPanel.BLACK_BORDER_SIZE);

        final Point imgPos = new Point(cardXOffset + borderSize, cardYOffset + borderSize);
        final Dimension imgSize = new Dimension(cardWidth - (borderSize * 2), cardHeight - (borderSize * 2));

        imagePanel.setLocation(imgPos);
        imagePanel.setSize(imgSize);

        final boolean canShow = matchUI.mayView(card);
        final boolean showText = !imagePanel.hasImage() || !isAnimationPanel;

        displayCardNameOverlay(showText && canShow && showCardNameOverlay(), imgSize, imgPos);
        displayPTOverlay(showText && (canShow || card.isFaceDown()) && showCardPowerOverlay(), imgSize, imgPos);
        displayCardIdOverlay(showText && canShow && showCardIdOverlay(), imgSize, imgPos);
    }

    private void displayCardIdOverlay(final boolean isVisible, final Dimension imgSize, final Point imgPos) {
        if (isVisible) {
            final Dimension idSize = cardIdText.getPreferredSize();
            cardIdText.setSize(idSize.width, idSize.height);
            final int idX = Math.round(imgSize.width * (24f / 480));
            final int idY = Math.round(imgSize.height * (650f / 680)) - 8;
            cardIdText.setLocation(imgPos.x + idX, imgPos.y + idY);
        }
        cardIdText.setVisible(isVisible);
    }

    private void displayPTOverlay(final boolean isVisible, final Dimension imgSize, final Point imgPos) {
        if (isVisible) {
            final int rightLine = Math.round(imgSize.width * (412f / 480)) + 3;
            // Power
            final Dimension ptSize = ptText.getPreferredSize();
            ptText.setSize(ptSize.width, ptSize.height);
            final int ptX = rightLine - ptSize.width/2;
            final int ptY = Math.round(imgSize.height * (650f / 680)) - 10;
            ptText.setLocation(imgPos.x + ptX, imgPos.y + ptY);
            // Toughness
            final Dimension dmgSize = damageText.getPreferredSize();
            damageText.setSize(dmgSize.width, dmgSize.height);
            final int dmgX = rightLine - dmgSize.width / 2;
            final int dmgY =  ptY - 16;
            damageText.setLocation(imgPos.x + dmgX, imgPos.y + dmgY);
        }
        ptText.setVisible(isVisible);
        damageText.setVisible(isVisible);
    }

    private void displayCardNameOverlay(final boolean isVisible, final Dimension imgSize, final Point imgPos) {
        if (isVisible) {
            final int titleX = Math.round(imgSize.width * (24f / 480));
            final int titleY = Math.round(imgSize.height * (54f / 640)) - 15;
            final int titleH = Math.round(imgSize.height * (360f / 640));
            titleText.setBounds(imgPos.x + titleX, imgPos.y + titleY + 2, imgSize.width - 2 * titleX, titleH - titleY);
        }
        titleText.setVisible(isVisible);
    }

    private void displayIconOverlay(final Graphics g, final boolean canShow) {
        if (canShow && showCardManaCostOverlay() && cardWidth < 200) {
            final boolean showSplitMana = card.isSplitCard();
            if (!showSplitMana) {
                drawManaCost(g, card.getCurrentState().getManaCost(), 0);
            } else {
                drawManaCost(g, card.getCurrentState().getManaCost(), +12);
                drawManaCost(g, card.getAlternateState().getManaCost(), -12);
            }
        }

        int number = 0;
        if (card.getCounters() != null) {
            for (final Integer i : card.getCounters().values()) {
                number += i.intValue();
            }
        }

        final int counters = number;
        final int yCounters = (cardYOffset + cardHeight) - (cardHeight / 3) - 40;

        if (counters == 1) {
            CardFaceSymbols.drawSymbol("counters1", g, cardXOffset - 15, yCounters);
        } else if (counters == 2) {
            CardFaceSymbols.drawSymbol("counters2", g, cardXOffset - 15, yCounters);
        } else if (counters == 3) {
            CardFaceSymbols.drawSymbol("counters3", g, cardXOffset - 15, yCounters);
        } else if (counters > 3) {
            CardFaceSymbols.drawSymbol("countersMulti", g, cardXOffset - 15, yCounters);
        }

        final int combatXSymbols = (cardXOffset + (cardWidth / 4)) - 16;
        final int stateXSymbols = (cardXOffset + (cardWidth / 2)) - 16;
        final int ySymbols = (cardYOffset + cardHeight) - (cardHeight / 8) - 16;

        if (card.isAttacking()) {
            CardFaceSymbols.drawSymbol("attack", g, combatXSymbols, ySymbols);
        }
        else if (card.isBlocking()) {
            CardFaceSymbols.drawSymbol("defend", g, combatXSymbols, ySymbols);
        }

        if (card.isSick()) {
            CardFaceSymbols.drawSymbol("summonsick", g, stateXSymbols, ySymbols);
        }

        if (card.isPhasedOut()) {
            CardFaceSymbols.drawSymbol("phasing", g, stateXSymbols, ySymbols);
        }

        if (matchUI.isUsedToPay(card)) {
            CardFaceSymbols.drawSymbol("sacrifice", g, (cardXOffset + (cardWidth / 2)) - 20,
                    (cardYOffset + (cardHeight / 2)) - 20);
        }
    }

    @Override
    public final String toString() {
        return getCard().toString();
    }

    public final void setCardBounds(final int x, final int y, int width, int height) {
        cardWidth = width;
        cardHeight = height;
        final int rotCenterX = Math.round(width / 2f);
        final int rotCenterY = height - rotCenterX;
        final int rotCenterToTopCorner = Math.round(width * CardPanel.ROT_CENTER_TO_TOP_CORNER);
        final int rotCenterToBottomCorner = Math.round(width * CardPanel.ROT_CENTER_TO_BOTTOM_CORNER);
        final int xOffset = rotCenterX - rotCenterToBottomCorner;
        final int yOffset = rotCenterY - rotCenterToTopCorner;
        cardXOffset = -xOffset;
        cardYOffset = -yOffset;
        width = -xOffset + rotCenterX + rotCenterToTopCorner;
        height = -yOffset + rotCenterY + rotCenterToBottomCorner;
        setBounds(x + xOffset, y + yOffset, width, height);
    }

    @Override
    public final void repaint() {
        final Rectangle b = getBounds();
        final JRootPane rootPane = SwingUtilities.getRootPane(this);
        if (rootPane == null) {
            return;
        }
        final Point p = SwingUtilities.convertPoint(getParent(), b.x, b.y, rootPane);
        rootPane.repaint(p.x, p.y, b.width, b.height);
    }

    public final int getCardX() {
        return getX() + cardXOffset;
    }

    public final int getCardY() {
        return getY() + cardYOffset;
    }

    public final int getCardWidth() {
        return cardWidth;
    }

    public final int getCardHeight() {
        return cardHeight;
    }

    public final Point getCardLocation() {
        final Point p = getLocation();
        p.x += cardXOffset;
        p.y += cardYOffset;
        return p;
    }

    public final Point getCardLocationOnScreen() {
        final Point p = getLocationOnScreen();
        p.x += cardXOffset;
        p.y += cardYOffset;
        return p;
    }

    public final void updateText() {
        if (card == null) {
            return;
        }

        // Card name overlay
        titleText.setText(card.getCurrentState().getName());

        final int damage = card.getDamage();
        damageText.setText(damage > 0 ? "\u00BB " + String.valueOf(damage) + " \u00AB" : "");

        // Card Id overlay
        cardIdText.setText(card.getCurrentState().getDisplayId());
    }

    public final void updatePTOverlay() {
        // P/T overlay
        final CardStateView state = card.getCurrentState();
        String sPt = "";
        if (state.isCreature() && state.isPlaneswalker()) {
            sPt = String.format("%d/%d (%d)", state.getPower(), state.getToughness(), state.getLoyalty());
        }
        else if (state.isCreature()) {
            sPt = String.format("%d/%d", state.getPower(), state.getToughness());
        }
        else if (state.isPlaneswalker()) {
            sPt = String.valueOf(state.getLoyalty());
        }
        ptText.setText(sPt);
    }

    @Override
    public final CardView getCard() {
        return card;
    }

    /** {@inheritDoc} */
    @Override
    public final void setCard(final CardView cardView) {
        final CardView oldCard = card;
        card = cardView; //always update card in case new card view instance for same card

        if (imagePanel == null) {
            return;
        }
        if (oldCard != null && oldCard.equals(card) && isAnimationPanel && imagePanel.hasImage()) {
            return; //prevent unnecessary update logic for animation panel
        }

        updateText();
        updatePTOverlay();
        updateImage();
        repaint();
    }

    @Override
    public void dispose() {
        attachedToPanel = null;
        attachedPanels = null;
        stack = null;
        imagePanel.setImage(null);
        imagePanel = null;
        card = null;
    }

    public static CardPanel getDragAnimationPanel() {
        return CardPanel.dragAnimationPanel;
    }
    public static void setDragAnimationPanel(final CardPanel dragAnimationPanel0) {
        CardPanel.dragAnimationPanel = dragAnimationPanel0;
    }

    public final CardPanel getAttachedToPanel() {
        return attachedToPanel;
    }
    public final void setAttachedToPanel(final CardPanel attachedToPanel0) {
        attachedToPanel = attachedToPanel0;
    }

    public final List<CardPanel> getAttachedPanels() {
        return attachedPanels;
    }

    public final List<CardPanel> getStack() {
        return stack;
    }
    public final void setStack(final List<CardPanel> stack0) {
        stack = stack0;
    }

    public final boolean isTapped() {
        return tapped;
    }
    public final void setTapped(final boolean tapped0) {
        tapped = tapped0;
    }

    public final double getTappedAngle() {
        return tappedAngle;
    }
    public final void setTappedAngle(final double tappedAngle0) {
        tappedAngle = tappedAngle0;
    }

    private static boolean isPreferenceEnabled(final FPref preferenceName) {
        return FModel.getPreferences().getPrefBoolean(preferenceName);
    }

    private boolean isShowingOverlays() {
        return isPreferenceEnabled(FPref.UI_SHOW_CARD_OVERLAYS) && card != null;
    }

    private boolean showCardNameOverlay() {
        return isShowingOverlays() && isPreferenceEnabled(FPref.UI_OVERLAY_CARD_NAME);
    }

    private boolean showCardPowerOverlay() {
        return isShowingOverlays() && isPreferenceEnabled(FPref.UI_OVERLAY_CARD_POWER);
    }

    private boolean showCardManaCostOverlay() {
        return isShowingOverlays() &&
                isPreferenceEnabled(FPref.UI_OVERLAY_CARD_MANA_COST);
    }

    private boolean showCardIdOverlay() {
        return isShowingOverlays() && isPreferenceEnabled(FPref.UI_OVERLAY_CARD_ID);
    }

    public void repaintOverlays() {
        repaint();
        doLayout();
    }
}
