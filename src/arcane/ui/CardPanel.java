
package arcane.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

import forge.AllZoneUtil;
import forge.Card;
import forge.CardContainer;
import forge.Counters;
import forge.Gui_NewGame;
import forge.ImageCache;
import arcane.ui.ScaledImagePanel;
import arcane.ui.ScaledImagePanel.MultipassType;
import arcane.ui.ScaledImagePanel.ScalingType;
import arcane.ui.util.GlowText;
import arcane.ui.util.ManaSymbols;

public class CardPanel extends JPanel implements CardContainer{
	private static final long serialVersionUID = 2361907095724263295L;
	static public final double TAPPED_ANGLE = Math.PI / 2;
	static public final float ASPECT_RATIO = 3.5f / 2.5f;

	static public CardPanel dragAnimationPanel;

	static private final float ROUNDED_CORNER_SIZE = 0.1f;
	static private final float SELECTED_BORDER_SIZE = 0.01f;
	static private final float BLACK_BORDER_SIZE = 0.03f;
	static private final int TEXT_GLOW_SIZE = 6;
	static private final float TEXT_GLOW_INTENSITY = 3f;
	static private final float rotCenterToTopCorner = 1.0295630140987000315797369464196f;
	static private final float rotCenterToBottomCorner = 0.7071067811865475244008443621048f;

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

	public CardPanel (Card newGameCard) {
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

	private void setImage (Image srcImage, Image srcImageBlurred) {
		synchronized (imagePanel) {
			imagePanel.setImage(srcImage, srcImageBlurred);
			repaint();
			for (CardPanel cardPanel : imageLoadListeners) {
				cardPanel.setImage(srcImage, srcImageBlurred);
				cardPanel.repaint();
			}
			imageLoadListeners.clear();
		}
		layout();
	}

	public void setImage (final CardPanel panel) {
		synchronized (panel.imagePanel) {
			if (panel.imagePanel.hasImage())
				setImage(panel.imagePanel.srcImage, panel.imagePanel.srcImageBlurred);
			else
				panel.imageLoadListeners.add(this);
		}
	}

	public void setScalingType (ScalingType scalingType) {
		imagePanel.setScalingType(scalingType);
	}

	public void setDisplayEnabled (boolean displayEnabled) {
		this.displayEnabled = displayEnabled;
	}

	public boolean isDisplayEnabled () {
		return displayEnabled;
	}

	public void setAnimationPanel (boolean isAnimationPanel) {
		this.isAnimationPanel = isAnimationPanel;
	}

	public void setSelected (boolean isSelected) {
		this.isSelected = isSelected;
		repaint();
	}

	public void setShowCastingCost (boolean showCastingCost) {
		this.showCastingCost = showCastingCost;
	}

	public void paint (Graphics g) {
		if (!displayEnabled) return;
		if (!isValid()) super.validate();
		Graphics2D g2d = (Graphics2D)g;
		if (tappedAngle > 0) {
			g2d = (Graphics2D)g2d.create();
			float edgeOffset = cardWidth / 2f;
			g2d.rotate(tappedAngle, cardXOffset + edgeOffset, cardYOffset + cardHeight - edgeOffset);
		}
		super.paint(g2d);
	}

	protected void paintComponent (Graphics g) {
		Graphics2D g2d = (Graphics2D)g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setColor(Color.black);
		int cornerSize = Math.max(4, Math.round(cardWidth * ROUNDED_CORNER_SIZE));
		g2d.fillRoundRect(cardXOffset, cardYOffset, cardWidth, cardHeight, cornerSize, cornerSize);
		if (isSelected) {
			g2d.setColor(Color.green);
			int offset = tapped ? 1 : 0;
			for (int i = 1, n = Math.max(1, Math.round(cardWidth * SELECTED_BORDER_SIZE)); i <= n; i++)
				g2d.drawRoundRect(cardXOffset - i, cardYOffset - i + offset, cardWidth + i * 2 - 1, cardHeight + i * 2 - 1,
					cornerSize, cornerSize);
		}
	}

	protected void paintChildren (Graphics g) {
		super.paintChildren(g);
		
		if (showCastingCost && !isAnimationPanel && cardWidth < 200) {
			int width = ManaSymbols.getWidth(gameCard.getManaCost());
			ManaSymbols.draw(g, gameCard.getManaCost(), cardXOffset + cardWidth / 2 - width / 2, cardYOffset + cardHeight / 2);
		}
		
		//int yOff = (cardHeight/4) + 2;
		if (showCastingCost && !isAnimationPanel && cardWidth < 200 && getCard().isAttacking() ) 
			ManaSymbols.drawSymbol("attack", g, cardXOffset + cardWidth / 4 - 16, cardYOffset + cardHeight - (cardHeight/8) - 16);
		else if (showCastingCost && !isAnimationPanel && cardWidth < 200 && getCard().isBlocking() ) 
			ManaSymbols.drawSymbol("defend", g, cardXOffset + cardWidth / 4 - 16, cardYOffset + cardHeight - (cardHeight/8) - 16);
		
		if (showCastingCost && !isAnimationPanel && cardWidth < 200 && getCard().isCreature() && getCard().hasSickness() && AllZoneUtil.isCardInPlay(getCard())) 
			ManaSymbols.drawSymbol("summonsick", g, cardXOffset + cardWidth / 2 - 16, cardYOffset + cardHeight - (cardHeight/8) - 16 );
	}

	public void layout () {		
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

	public String toString () {
		return gameCard.getName();
	}

	public void setCardBounds (int x, int y, int width, int height) {
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

	public void repaint () {
		Rectangle b = getBounds();
		JRootPane rootPane = SwingUtilities.getRootPane(this);
		if (rootPane == null) return;
		Point p = SwingUtilities.convertPoint(getParent(), b.x, b.y, rootPane);
		rootPane.repaint(p.x, p.y, b.width, b.height);
	}

	public int getCardX () {
		return getX() + cardXOffset;
	}

	public int getCardY () {
		return getY() + cardYOffset;
	}

	public int getCardWidth () {
		return cardWidth;
	}

	public int getCardHeight () {
		return cardHeight;
	}

	public Point getCardLocation () {
		Point p = getLocation();
		p.x += cardXOffset;
		p.y += cardYOffset;
		return p;
	}
	public void setText(Card card) {
		if (card == null || !Gui_NewGame.cardOverlay.isSelected()) return;
        if(card.isFaceDown()){
        	titleText.setText("");
        	showCastingCost = false;
        }
        else {
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

	public Card getCard() {
		return gameCard;
	}

	public void setCard(Card card) {
		if(gameCard != null && gameCard.equals(card) && isAnimationPanel && imagePanel.hasImage()) return;
		this.gameCard = card;
        if(!isShowing()) return;
        Insets i = getInsets();
        Image image = card == null? null:ImageCache.getImage(card, getWidth() - i.left - i.right, getHeight()
                - i.top - i.bottom);
        if(gameCard != null && Gui_NewGame.cardOverlay.isSelected()){
	        setText(gameCard);
        }
        
        setImage(image, image);
	}
}
