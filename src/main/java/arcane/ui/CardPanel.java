package arcane.ui;

import arcane.ui.ScaledImagePanel.MultipassType;
import arcane.ui.ScaledImagePanel.ScalingType;
import arcane.ui.util.GlowText;
import arcane.ui.util.ManaSymbols;
import forge.*;

import javax.swing.*;
import java.awt.Color;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>CardPanel class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class CardPanel extends JPanel implements CardContainer {
    /** Constant <code>serialVersionUID=2361907095724263295L</code> */
    private static final long serialVersionUID = 2361907095724263295L;
    /**
     * Constant <code>TAPPED_ANGLE=Math.PI / 2</code>
     */
    public static final double TAPPED_ANGLE = Math.PI / 2;
    /**
     * Constant <code>ASPECT_RATIO=3.5f / 2.5f</code>
     */
    public static final float ASPECT_RATIO = 3.5f / 2.5f;

    /**
     * Constant <code>dragAnimationPanel</code>
     */
    public static CardPanel dragAnimationPanel;

    /** Constant <code>ROUNDED_CORNER_SIZE=0.1f</code> */
    private static final float ROUNDED_CORNER_SIZE = 0.1f;
    /** Constant <code>SELECTED_BORDER_SIZE=0.01f</code> */
    private static final float SELECTED_BORDER_SIZE = 0.01f;
    /** Constant <code>BLACK_BORDER_SIZE=0.03f</code> */
    private static final float BLACK_BORDER_SIZE = 0.03f;
    /** Constant <code>TEXT_GLOW_SIZE=6</code> */
    private static final int TEXT_GLOW_SIZE = 6;
    /** Constant <code>TEXT_GLOW_INTENSITY=3f</code> */
    private static final float TEXT_GLOW_INTENSITY = 3f;
    /** Constant <code>rotCenterToTopCorner=1.0295630140987000315797369464196f</code> */
    private static final float rotCenterToTopCorner = 1.0295630140987000315797369464196f;
    /** Constant <code>rotCenterToBottomCorner=0.7071067811865475244008443621048f</code> */
    private static final float rotCenterToBottomCorner = 0.7071067811865475244008443621048f;

    public Card gameCard;
    public CardPanel attachedToPanel;
    public List<CardPanel> attachedPanels = new ArrayList<CardPanel>();
    public boolean tapped;
    public double tappedAngle = 0;
    public ScaledImagePanel imagePanel;

    private GlowText titleText;
    private GlowText ptText;
    private List<CardPanel> imageLoadListeners = new ArrayList<CardPanel>(2);
    private boolean displayEnabled = true;
    private boolean isAnimationPanel;
    private int cardXOffset, cardYOffset, cardWidth, cardHeight;
    private boolean isSelected;
    private boolean showCastingCost;

    /**
     * <p>Constructor for CardPanel.</p>
     *
     * @param newGameCard a {@link forge.Card} object.
     */
    public CardPanel(Card newGameCard) {
        this.gameCard = newGameCard;

        setBackground(Color.black);
        setOpaque(false);

        titleText = new GlowText();
        titleText.setFont(getFont().deriveFont(Font.BOLD, 13f));
        titleText.setForeground(Color.white);
        titleText.setGlow(Color.black, TEXT_GLOW_SIZE, TEXT_GLOW_INTENSITY);
        titleText.setWrap(true);
        add(titleText);

        ptText = new GlowText();
        ptText.setFont(getFont().deriveFont(Font.BOLD, 13f));
        ptText.setForeground(Color.white);
        ptText.setGlow(Color.black, TEXT_GLOW_SIZE, TEXT_GLOW_INTENSITY);
        add(ptText);

        imagePanel = new ScaledImagePanel();
        add(imagePanel);
        imagePanel.setScaleLarger(true);
        imagePanel.setScalingType(ScalingType.nearestNeighbor);
        imagePanel.setScalingBlur(true);
        imagePanel.setScalingMultiPassType(MultipassType.none);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                setCard(gameCard);
            }

            @Override
            public void componentResized(ComponentEvent e) {
                setCard(gameCard);
            }
        });

        setCard(newGameCard);
    }

    /**
     * <p>setImage.</p>
     *
     * @param srcImage a {@link java.awt.Image} object.
     * @param srcImageBlurred a {@link java.awt.Image} object.
     * @param srcImageBlurred a {@link java.awt.Image} object.
     */
    private void setImage(Image srcImage, Image srcImageBlurred) {
        synchronized (imagePanel) {
            imagePanel.setImage(srcImage, srcImageBlurred);
            repaint();
            for (CardPanel cardPanel : imageLoadListeners) {
                cardPanel.setImage(srcImage, srcImageBlurred);
                cardPanel.repaint();
            }
            imageLoadListeners.clear();
        }
        doLayout();
    }

    /**
     * <p>setImage.</p>
     *
     * @param panel a {@link arcane.ui.CardPanel} object.
     */
    public void setImage(final CardPanel panel) {
        synchronized (panel.imagePanel) {
            if (panel.imagePanel.hasImage())
                setImage(panel.imagePanel.srcImage, panel.imagePanel.srcImageBlurred);
            else
                panel.imageLoadListeners.add(this);
        }
    }

    /**
     * <p>setScalingType.</p>
     *
     * @param scalingType a {@link arcane.ui.ScaledImagePanel.ScalingType} object.
     */
    public void setScalingType(ScalingType scalingType) {
        imagePanel.setScalingType(scalingType);
    }

    /**
     * <p>Setter for the field <code>displayEnabled</code>.</p>
     *
     * @param displayEnabled a boolean.
     */
    public void setDisplayEnabled(boolean displayEnabled) {
        this.displayEnabled = displayEnabled;
    }

    /**
     * <p>isDisplayEnabled.</p>
     *
     * @return a boolean.
     */
    public boolean isDisplayEnabled() {
        return displayEnabled;
    }

    /**
     * <p>setAnimationPanel.</p>
     *
     * @param isAnimationPanel a boolean.
     */
    public void setAnimationPanel(boolean isAnimationPanel) {
        this.isAnimationPanel = isAnimationPanel;
    }

    /**
     * <p>setSelected.</p>
     *
     * @param isSelected a boolean.
     */
    public void setSelected(boolean isSelected) {
        this.isSelected = isSelected;
        repaint();
    }

    /**
     * <p>Setter for the field <code>showCastingCost</code>.</p>
     *
     * @param showCastingCost a boolean.
     */
    public void setShowCastingCost(boolean showCastingCost) {
        this.showCastingCost = showCastingCost;
    }

    /** {@inheritDoc} */
    public void paint(Graphics g) {
        if (!displayEnabled) return;
        if (!isValid()) super.validate();
        Graphics2D g2d = (Graphics2D) g;
        if (tappedAngle > 0) {
            g2d = (Graphics2D) g2d.create();
            float edgeOffset = cardWidth / 2f;
            g2d.rotate(tappedAngle, cardXOffset + edgeOffset, cardYOffset + cardHeight - edgeOffset);
        }
        super.paint(g2d);
    }

    /** {@inheritDoc} */
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // + White borders for Core sets Unlimited - 9th +
        int cornerSize = Math.max(4, Math.round(cardWidth * ROUNDED_CORNER_SIZE));

        if (this.gameCard != null) {
            if ((!this.gameCard.getImageFilename().equals("none")) && (!this.gameCard.getName().equals("Morph"))) {
                if ((this.gameCard.getCurSetCode().equals("2ED")) ||
                        (this.gameCard.getCurSetCode().equals("3ED")) ||
                        (this.gameCard.getCurSetCode().equals("4ED")) ||
                        (this.gameCard.getCurSetCode().equals("5ED")) ||
                        (this.gameCard.getCurSetCode().equals("6ED")) ||
                        (this.gameCard.getCurSetCode().equals("7ED")) ||
                        (this.gameCard.getCurSetCode().equals("8ED")) ||
                        (this.gameCard.getCurSetCode().equals("9ED")) ||
                        (this.gameCard.getCurSetCode().equals("CHR")) ||
                        (this.gameCard.getCurSetCode().equals("S99")) ||
                        (this.gameCard.getCurSetCode().equals("PTK")) ||
                        (this.gameCard.getCurSetCode().equals("S00"))) {
                    if (!isSelected) {
                        g2d.setColor(Color.black);
                        int offset = tapped ? 1 : 0;
                        for (int i = 1, n = Math.max(1, Math.round(cardWidth * SELECTED_BORDER_SIZE)); i <= n; i++)
                            g2d.drawRoundRect(cardXOffset - i, cardYOffset - i + offset, cardWidth + i * 2 - 1, cardHeight + i * 2 - 1,
                                    cornerSize, cornerSize);
                    }
                    g2d.setColor(Color.white);
                } else {
                    g2d.setColor(Color.black);
                }
            }
        }
        // - White borders for Core sets Unlimited - 9th -

        g2d.fillRoundRect(cardXOffset, cardYOffset, cardWidth, cardHeight, cornerSize, cornerSize);
        if (isSelected) {
            g2d.setColor(Color.green);
            int offset = tapped ? 1 : 0;
            for (int i = 1, n = Math.max(1, Math.round(cardWidth * SELECTED_BORDER_SIZE)); i <= n; i++)
                g2d.drawRoundRect(cardXOffset - i, cardYOffset - i + offset, cardWidth + i * 2 - 1, cardHeight + i * 2 - 1,
                        cornerSize, cornerSize);
        }
    }

    /** {@inheritDoc} */
    protected void paintChildren(Graphics g) {
        super.paintChildren(g);
        
        boolean canDrawOverCard = showCastingCost && !isAnimationPanel && cardWidth < 200;
        if (canDrawOverCard) {
            int width = ManaSymbols.getWidth(gameCard.getManaCost());
            ManaSymbols.draw(g, gameCard.getManaCost(), cardXOffset + cardWidth / 2 - width / 2, cardYOffset + cardHeight / 2);
        }

        //int yOff = (cardHeight/4) + 2;
        if (canDrawOverCard && getCard().isAttacking()) {
            ManaSymbols.drawSymbol("attack", g, cardXOffset + cardWidth / 4 - 16, cardYOffset + cardHeight - (cardHeight / 8) - 16);
        } else if (canDrawOverCard && getCard().isBlocking()) {
            ManaSymbols.drawSymbol("defend", g, cardXOffset + cardWidth / 4 - 16, cardYOffset + cardHeight - (cardHeight / 8) - 16);
        }

        if (canDrawOverCard && getCard().isCreature() && getCard().hasSickness() && AllZoneUtil.isCardInPlay(getCard()))
            ManaSymbols.drawSymbol("summonsick", g, cardXOffset + cardWidth / 2 - 16, cardYOffset + cardHeight - (cardHeight / 8) - 16);

        if (canDrawOverCard && getCard() != null) {
            if (this.gameCard.getFoil() > 0) {
            	String fl = String.format("foil%02d", getCard().getFoil());
            	int z = Math.round(cardWidth * BLACK_BORDER_SIZE);
            	ManaSymbols.draw(g, fl, cardXOffset + z, cardYOffset + z, cardWidth - (2*z), cardHeight - (2*z));
            }
        	
            if (getCard().getName().equals("Mana Pool") && !isAnimationPanel) {

                if (AllZone.getHumanPlayer().getManaPool() != null) {
                    String s = AllZone.getHumanPlayer().getManaPool().getManaList();
                    if (!s.equals("|||||||||||")) {

                        String mList[] = s.split("\\|", 12);

                        int n = 0;
                        for (int i = 0; i < 2; i++) {
                            for (int j = 0; j < 6; j++) {
                                if (!mList[n].equals("")) {
                                    int width = ManaSymbols.getWidth(mList[n]);
                                    ManaSymbols.draw(g, mList[n], cardXOffset + ((i + 1) * (cardWidth / 3)) - width / 2, cardYOffset + ((j + 1) * (cardHeight / 7)));
                                }

                                n++;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * <p>doLayout.</p>
     *
     * @since 1.0.15
     */
    public void doLayout() {
        int borderSize = Math.round(cardWidth * BLACK_BORDER_SIZE);
        imagePanel.setLocation(cardXOffset + borderSize, cardYOffset + borderSize);
        imagePanel.setSize(cardWidth - borderSize * 2, cardHeight - borderSize * 2);

        int fontHeight = Math.round(cardHeight * (27f / 680));
        boolean showText = !imagePanel.hasImage() || (!isAnimationPanel && fontHeight < 12);
        titleText.setVisible(showText);
        ptText.setVisible(showText);

        int titleX = Math.round(cardWidth * (20f / 480));
        int titleY = Math.round(cardHeight * (9f / 680));
        titleText.setBounds(cardXOffset + titleX, cardYOffset + titleY, cardWidth - titleX, cardHeight);

        Dimension ptSize = ptText.getPreferredSize();
        ptText.setSize(ptSize.width, ptSize.height);
        int ptX = Math.round(cardWidth * (420f / 480)) - ptSize.width / 2;
        int ptY = Math.round(cardHeight * (675f / 680)) - ptSize.height;
        ptText.setLocation(cardXOffset + ptX - TEXT_GLOW_SIZE / 2, cardYOffset + ptY - TEXT_GLOW_SIZE / 2);

        if (isAnimationPanel || cardWidth < 200)
            imagePanel.setScalingType(ScalingType.nearestNeighbor);
        else
            imagePanel.setScalingType(ScalingType.bilinear);
    }

    /**
     * <p>toString.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String toString() {
        return gameCard.getName();
    }

    /**
     * <p>setCardBounds.</p>
     *
     * @param x      a int.
     * @param y      a int.
     * @param width  a int.
     * @param height a int.
     */
    public void setCardBounds(int x, int y, int width, int height) {
        cardWidth = width;
        cardHeight = height;
        int rotCenterX = Math.round(width / 2f);
        int rotCenterY = height - rotCenterX;
        int rotCenterToTopCorner = Math.round(width * CardPanel.rotCenterToTopCorner);
        int rotCenterToBottomCorner = Math.round(width * CardPanel.rotCenterToBottomCorner);
        int xOffset = rotCenterX - rotCenterToBottomCorner;
        int yOffset = rotCenterY - rotCenterToTopCorner;
        cardXOffset = -xOffset;
        cardYOffset = -yOffset;
        width = -xOffset + rotCenterX + rotCenterToTopCorner;
        height = -yOffset + rotCenterY + rotCenterToBottomCorner;
        setBounds(x + xOffset, y + yOffset, width, height);
    }

    /**
     * <p>repaint.</p>
     */
    public void repaint() {
        Rectangle b = getBounds();
        JRootPane rootPane = SwingUtilities.getRootPane(this);
        if (rootPane == null) return;
        Point p = SwingUtilities.convertPoint(getParent(), b.x, b.y, rootPane);
        rootPane.repaint(p.x, p.y, b.width, b.height);
    }

    /**
     * <p>getCardX.</p>
     *
     * @return a int.
     */
    public int getCardX() {
        return getX() + cardXOffset;
    }

    /**
     * <p>getCardY.</p>
     *
     * @return a int.
     */
    public int getCardY() {
        return getY() + cardYOffset;
    }

    /**
     * <p>Getter for the field <code>cardWidth</code>.</p>
     *
     * @return a int.
     */
    public int getCardWidth() {
        return cardWidth;
    }

    /**
     * <p>Getter for the field <code>cardHeight</code>.</p>
     *
     * @return a int.
     */
    public int getCardHeight() {
        return cardHeight;
    }

    /**
     * <p>getCardLocation.</p>
     *
     * @return a {@link java.awt.Point} object.
     */
    public Point getCardLocation() {
        Point p = getLocation();
        p.x += cardXOffset;
        p.y += cardYOffset;
        return p;
    }

    /**
     * <p>setText.</p>
     *
     * @param card a {@link forge.Card} object.
     */
    public void setText(Card card) {
        if (card == null || !Singletons.getModel().getPreferences().cardOverlay)
            return;
        
        if (card.isFaceDown()) {
            titleText.setText("");
            showCastingCost = false;
        } else {
            titleText.setText(card.getName());
            showCastingCost = true;
        }

        if (card.isCreature() && card.isPlaneswalker()) {
            ptText.setText(card.getNetAttack() + "/" + card.getNetDefense() + " (" + String.valueOf(card.getCounters(Counters.LOYALTY)) + ")");
        } else if (card.isCreature()) {
            ptText.setText(card.getNetAttack() + "/" + card.getNetDefense());
        } else if (card.isPlaneswalker()) {
            ptText.setText(String.valueOf(card.getCounters(Counters.LOYALTY)));
        } else {
            ptText.setText("");
        }
    }

    /**
     * <p>getCard.</p>
     *
     * @return a {@link forge.Card} object.
     */
    public Card getCard() {
        return gameCard;
    }

    /** {@inheritDoc} */
    public void setCard(Card card) {
        if (gameCard != null && gameCard.equals(card) && isAnimationPanel && imagePanel.hasImage()) return;
        this.gameCard = card;
        if (!isShowing()) return;
        Insets i = getInsets();
        Image image = card == null ? null : ImageCache.getImage(card, getWidth() - i.left - i.right, getHeight()
                - i.top - i.bottom);
        if (gameCard != null && Singletons.getModel().getPreferences().cardOverlay) {
            setText(gameCard);
        }

        setImage(image, image);
    }
}
