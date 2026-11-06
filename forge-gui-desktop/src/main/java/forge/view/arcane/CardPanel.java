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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.font.TextAttribute;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

import com.google.common.collect.Multiset;

import forge.CachedCardImage;
import forge.StaticData;
import forge.card.CardEdition;
import forge.card.CardStateName;
import forge.card.mana.ManaCost;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.game.card.CounterType;
import forge.game.keyword.Keyword;
import forge.game.zone.ZoneType;
import forge.gui.CardContainer;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgeConstants.CounterDisplayType;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.localinstance.skin.FSkinProp;
import forge.model.FModel;
import forge.screens.match.CMatchUI;
import forge.toolbox.ArenaArt;
import forge.toolbox.ArenaTheme;
import forge.toolbox.CardFaceSymbols;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedPanel;
import forge.toolbox.IDisposable;
import forge.util.CardTranslation;
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
    /** Arena tiles are wider than tall and tap with a slight clockwise tilt. */
    public static final float ARENA_TILE_RATIO = 1.27f;
    private static final double ARENA_TAPPED_ANGLE = Math.toRadians(8);
    public static final float ASPECT_RATIO = 3.5f / 2.5f;
    public static final float TARGET_ORIGIN_FACTOR_X = 0.25f;
    public static final float TARGET_ORIGIN_FACTOR_Y = 0.5f;
    private static CardPanel dragAnimationPanel;
    public static final float ROUNDED_CORNER_SIZE = 0.1f;
    public static final float SELECTED_BORDER_SIZE = 0.01f;
    public static final float BLACK_BORDER_SIZE = 0.03f;
    private static final float ROT_CENTER_TO_TOP_CORNER = 1.0295630140987000315797369464196f;
    private static final float ROT_CENTER_TO_BOTTOM_CORNER = 0.7071067811865475244008443621048f;
    private static final Map<ZoneType, Color> ZONE_COLORS = Map.of(
        ZoneType.Graveyard, new Color(80, 20, 100),
        ZoneType.Exile, new Color(180, 185, 200),
        ZoneType.Command, new Color(190, 155, 30),
        ZoneType.Library, new Color(130, 110, 75),
        ZoneType.Sideboard, new Color(60, 60, 80),
        ZoneType.Flashback, new Color(80, 20, 100)
    );
    private static final Color DEFAULT_ZONE_COLOR = new Color(60, 60, 80);
    private static final Color GHOST_TINT = new Color(90, 120, 175, 110);

    private final CMatchUI matchUI;
    private CardView card;
    private CardPanel attachedToPanel;
    private List<CardPanel> attachedPanels = new ArrayList<>();
    private List<CardPanel> stack;
    private boolean tapped;
    private double tappedAngle = 0;
    private ScaledImagePanel imagePanel;

    private OutlinedLabel titleText;
    private OutlinedLabel ptText;
    private OutlinedLabel damageText;
    private OutlinedLabel cardIdText;
    private boolean displayEnabled = true;
    private boolean ghost; // faded stand-in for a card this permanent holds in exile
    private boolean isAnimationPanel;
    private int cardXOffset, cardYOffset, cardWidth, cardHeight;
    private boolean isSelected;
    private boolean hasFlash;
    private String zoneBannerText;
    private int arenaCostWidth; // Arena hand: the uncovered width the cost above the card lines up with; 0 outside a fan
    private double arenaFanAngle; // Arena hand: how far this card leans in the fan
    private boolean arenaStackBack; // Arena board: a card tucked behind another in its pile, which shows no name or stats
    private BufferedImage tileArtTapped; // the art greyed out, made from tileArtTappedFrom
    private BufferedImage tileArtTappedFrom;
    private Color zoneBannerColor;
    private CachedCardImage cachedImage;
    // Arena tiles: a larger copy of the card image and the art cropped from it
    private CachedCardImage tileSource;
    private int tileSourceHeight;
    private BufferedImage tileArt;
    private BufferedImage tileArtSource;
    private int groupCount;
    private int hotkeyDigit; // 1..9 paints a numbered badge for Ctrl+digit selection; 0 hides
    private Font badgeFont;
    private int badgeFontCardWidth; // cardWidth when badgeFont was last computed

    private static final Color BADGE_BG_COLOR = new Color(0, 0, 0, 180);
    private static final Color HOTKEY_BADGE_BG = new Color(20, 20, 20, 220);
    private static final Color HOTKEY_BADGE_RING = new Color(220, 220, 220);
    private static final int HOTKEY_BADGE_MIN_CARD_WIDTH = 80;
    private static final int HOTKEY_BADGE_MAX_DIAMETER = 28;
    private static Font smallCounterFont;
    private static Font largeCounterFont;

    static {

        try {
            Font roboto = Font.createFont(Font.TRUETYPE_FONT, Paths.get(ForgeConstants.COMMON_FONTS_DIR, "Roboto-Bold.ttf").toFile());

            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(roboto);

            Map<TextAttribute, Object> attributes = new HashMap<>();

            attributes.put(TextAttribute.FAMILY, "Roboto Bold");
            attributes.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
            attributes.put(TextAttribute.SIZE, 11);
            attributes.put(TextAttribute.KERNING, TextAttribute.KERNING_ON);

            smallCounterFont = Font.getFont(attributes);

            attributes.put(TextAttribute.SIZE, 14);
            largeCounterFont = Font.getFont(attributes);

        } catch (FontFormatException | IOException e) {
            e.printStackTrace();
        }

    }

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

        if (ArenaTheme.isEnabled()) {
            ptText.setFont(getFont().deriveFont(Font.BOLD, 14f));
            ptText.setForeground(ArenaTheme.TEXT);
            ptText.setBadge(new Color(8, 13, 25, 230), ArenaTheme.GOLD_DIM);
            damageText.setFont(getFont().deriveFont(Font.BOLD, 13f));
            damageText.setForeground(Color.white);
            damageText.setGlow(new Color(90, 0, 0));
            damageText.setBadge(new Color(170, 30, 36, 235), new Color(255, 140, 140));
        }
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
        tileSource = null;
        tileArt = null;

        if (card == null)  {
            cachedImage = null;
            setImage(null);
            return;
        }

        // Obtain screen DPI scale and apply them
        final float screenScale = GuiBase.getInterface().getScreenScale();
        int imageWidth = Math.round(imagePanel.getWidth() * screenScale);
        int imageHeight = Math.round(imagePanel.getHeight() * screenScale);
        cachedImage = new CachedCardImage(card, matchUI.getLocalPlayers(), imageWidth, imageHeight) {
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

    public int getGroupCount() {
        return groupCount;
    }
    public void setGroupCount(int count) {
        this.groupCount = count;
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

    /** Every turned card is painted in this one picture in its turn, rather than a frame allocating one per card. */
    private static BufferedImage arenaScratch;
    private static boolean arenaScratchInUse;

    /**
     * A leaning or tilted Arena card: painted flat at twice the size, then turned and scaled down with
     * filtering, so edges and frame lines stay clean. Only the part that can land in the area being repainted
     * is painted at all, since most of a hand card is below the window or under its neighbour.
     */
    private void paintArenaTurned(final Graphics2D g) {
        final double base = Math.max(1d, Math.abs(g.getTransform().getScaleX()));
        final double scale = base * (base >= 1.5d ? 1 : 2);
        final java.awt.geom.AffineTransform turn = new java.awt.geom.AffineTransform();
        if (getTappedAngle() > 0) {
            turn.rotate(getTappedAngle(), cardXOffset + cardWidth / 2f, cardYOffset + cardHeight / 2f);
        }
        if (arenaFanAngle != 0) {
            turn.rotate(arenaFanAngle, cardXOffset + cardWidth / 2f, cardYOffset + cardHeight);
        }
        // the panel's margins leave room for the turn; the picture itself only reaches as far as the glow and badges
        final boolean tile = isArenaTile();
        final int reach = Math.round(cardWidth * (tile ? 0.13f : 0.2f));
        final Rectangle whole = new Rectangle(0, 0, getWidth(), getHeight()).intersection(new Rectangle(cardXOffset - reach, cardYOffset - reach,
                cardWidth + 2 * reach, cardHeight + 2 * reach + cardHeight / 12));
        Rectangle needed = whole;
        final Rectangle clip = g.getClipBounds();
        if (clip != null) {
            try {
                final Rectangle back = turn.createInverse().createTransformedShape(clip).getBounds();
                back.grow(3, 3); // the filter reaches a little beyond what it lands on
                needed = whole.intersection(back);
            } catch (final java.awt.geom.NoninvertibleTransformException e) {
                needed = whole; // not for a rotation
            }
        }
        if (needed.isEmpty()) {
            return;
        }
        final int pw = (int) Math.ceil(needed.width * scale), ph = (int) Math.ceil(needed.height * scale);
        final boolean shared = !arenaScratchInUse;
        final BufferedImage flat;
        if (shared) {
            if (arenaScratch == null || arenaScratch.getWidth() < pw || arenaScratch.getHeight() < ph) {
                arenaScratch = new BufferedImage(arenaScratch == null ? pw : Math.max(pw, arenaScratch.getWidth()),
                        arenaScratch == null ? ph : Math.max(ph, arenaScratch.getHeight()), BufferedImage.TYPE_INT_ARGB_PRE);
            }
            flat = arenaScratch;
            arenaScratchInUse = true;
        } else {
            flat = new BufferedImage(pw, ph, BufferedImage.TYPE_INT_ARGB_PRE);
        }
        try {
            final Graphics2D fg = flat.createGraphics();
            fg.setComposite(java.awt.AlphaComposite.Clear);
            fg.fillRect(0, 0, pw, ph);
            fg.setComposite(java.awt.AlphaComposite.SrcOver);
            fg.scale(scale, scale);
            fg.translate(-needed.x, -needed.y);
            fg.setClip(needed);
            super.paint(fg);
            fg.dispose();
            final Graphics2D turned = (Graphics2D) g.create();
            try {
                turned.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                turned.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                turned.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                turned.transform(turn);
                turned.translate(needed.x, needed.y);
                turned.scale(1d / scale, 1d / scale);
                turned.drawImage(flat.getSubimage(0, 0, pw, ph), 0, 0, null);
            } finally {
                turned.dispose();
            }
        } finally {
            if (shared) {
                arenaScratchInUse = false;
            }
        }
    }

    /** How far a tapped card turns: a slight tilt on the Arena board, a quarter turn otherwise. */
    public static double tappedAngle() {
        return ArenaTheme.isEnabled() ? ARENA_TAPPED_ANGLE : TAPPED_ANGLE;
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
        if (ArenaTheme.isEnabled() && (getTappedAngle() > 0 || arenaFanAngle != 0) && getWidth() > 0 && getHeight() > 0) {
            paintArenaTurned(g2d);
            return;
        }
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

        boolean noBorderPref = !isPreferenceEnabled(FPref.UI_RENDER_BLACK_BORDERS);
        boolean cardImgHasAlpha = imagePanel != null && imagePanel.getSrcImage() != null && imagePanel.getSrcImage().getColorModel().hasAlpha();

        // if we are rendering borderless and the card has no alpha channel, use square corners to avoid visual glitches with highlight frames.
        final int cornerSize = noBorderPref && !cardImgHasAlpha ? 0 : Math.max(4, Math.round(cardWidth * CardPanel.ROUNDED_CORNER_SIZE));
        final int offset = isTapped() && (!noBorderPref || cardImgHasAlpha) ? 1 : 0;

        if (ArenaTheme.isEnabled()) {
            paintArenaFrame(g2d, cornerSize, offset);
            return;
        }

        // Yellow glow for cards that Auto would tap to pay (actionable strength >= 2)
        if (isPreferenceEnabled(FPref.UI_SHOW_AUTOTAP_PREVIEW) && matchUI.getActionableStrength(getCard()) >= 2) {
            for (int layer = 2; layer >= 1; layer--) {
                g2d.setColor(new Color(1f, 1f, 0f, 0.14f * layer));
                final int n = Math.max(1, Math.round(layer * cardWidth * CardPanel.SELECTED_BORDER_SIZE));
                g2d.fillRoundRect(cardXOffset - n, (cardYOffset - n) + offset, cardWidth + (n * 2), cardHeight + (n * 2), cornerSize + n, cornerSize + n);
            }
        }

        // Magenta outline for when card is chosen
        if (matchUI.isHighlighted(getCard())) {
            g2d.setColor(Color.magenta);
            final int n2 = Math.max(1, Math.round(2 * cardWidth * CardPanel.SELECTED_BORDER_SIZE));
            g2d.fillRoundRect(cardXOffset - n2, (cardYOffset - n2) + offset, cardWidth + (n2 * 2), cardHeight + (n2 * 2), cornerSize + n2, cornerSize + n2);
        }

        // Green outline for hover
        if (isSelected) {
            g2d.setColor(Color.green);
            final int n = Math.max(1, Math.round(cardWidth * CardPanel.SELECTED_BORDER_SIZE));
            g2d.fillRoundRect(cardXOffset - n, (cardYOffset - n) + offset, cardWidth + (n * 2), cardHeight + (n * 2), cornerSize + n , cornerSize + n);
        } else if (hasFlash && (getCard() != null && matchUI.mayView(getCard()))) {
            g2d.setColor(Color.cyan);
            final int n = Math.max(1, Math.round(cardWidth * CardPanel.SELECTED_BORDER_SIZE));
            g2d.fillRoundRect(cardXOffset - n, (cardYOffset - n) + offset, cardWidth + (n * 2), cardHeight + (n * 2), cornerSize + n , cornerSize + n);
        } else if (zoneBannerColor != null) {
            g2d.setColor(zoneBannerColor);
            final int n = Math.max(1, Math.round(cardWidth * CardPanel.SELECTED_BORDER_SIZE));
            g2d.fillRoundRect(cardXOffset - n, (cardYOffset - n) + offset, cardWidth + (n * 2), cardHeight + (n * 2), cornerSize + n, cornerSize + n);
        }

        // Black fill - (will become an outline for white bordered cards)
        g2d.setColor(Color.black);
        g2d.fillRoundRect(cardXOffset, cardYOffset  + offset, cardWidth, cardHeight, cornerSize, cornerSize);

        // White border if card is known to have it.
        if (getCard() != null && matchUI.mayView(getCard())) {
            final CardStateView state = getCard().getCurrentState();
            final CardEdition ed = FModel.getMagicDb().getEditions().get(state.getSetCode());
            boolean colorIsSet = false;
            if (getCard().isImmutable()) {
                // Effects are drawn with orange border
                g2d.setColor(Color.ORANGE);
                colorIsSet = true;
            } else if (ed != null && state.getFoilIndex() == 0) {
                // Non-foil cards from white-bordered sets are drawn with white border
                switch (ed.getBorderColor()) {
                case WHITE:
                    g2d.setColor(Color.WHITE);
                    colorIsSet = true;
                    break;
                case GOLD:
                    g2d.setColor(Color.ORANGE);
                    colorIsSet = true;
                    break;
                case SILVER:
                    g2d.setColor(Color.GRAY);
                    colorIsSet = true;
                }
            }
            if (colorIsSet) {
                final int ins = 1;
                g2d.fillRoundRect(cardXOffset + ins, cardYOffset + ins, cardWidth - ins*2, cardHeight - ins*2, cornerSize-ins, cornerSize-ins);
            }
        }

        // Inner highlight: white for fully selectable cards, the configured colour for
        // actionable ones. With black borders enabled this sits in the
        // border gap as a 1px inset; with borders disabled the card image fills the whole
        // panel and would cover an inset, so draw it as an outset (like the frames above).
        Color innerBorder = null;
        if (matchUI.isSelectable(getCard())) {
            innerBorder = Color.WHITE;
        } else if (isPreferenceEnabled(FPref.UI_SHOW_ACTIONABLE_HIGHLIGHTS) && matchUI.isActionable(getCard())) {
            innerBorder = parseActionableHighlightColor();
        }
        if (innerBorder != null) {
            g2d.setColor(innerBorder);
            if (noBorderPref && !isSelected) {
                final int n = Math.max(1, Math.round(cardWidth * CardPanel.SELECTED_BORDER_SIZE));
                g2d.fillRoundRect(cardXOffset - n, (cardYOffset - n) + offset, cardWidth + (n * 2), cardHeight + (n * 2), cornerSize + n, cornerSize + n);
            } else {
                final int ins = 1;
                g2d.fillRoundRect(cardXOffset + ins, cardYOffset + ins, cardWidth - ins * 2, cardHeight - ins * 2, cornerSize - ins, cornerSize - ins);
            }
        }
    }

    /**
     * Arena card frame: a soft drop shadow, then one glow for the most important state
     * (chosen, targetable, attacking/blocking, hovered, playable) with a crisp rim on top.
     */
    private void paintArenaFrame(final Graphics2D g2d, final int cornerSize, final int offset) {
        final int x = cardXOffset;
        final int y = cardYOffset + offset;
        final int arc = isArenaTile() ? Math.max(6, Math.round(cardWidth * 0.08f)) : Math.max(cornerSize, Math.max(8, Math.round(cardWidth * 0.11f)));
        final int spread = Math.max(6, Math.round(cardWidth * 0.085f));
        final CardView cv = getCard();
        // a token's arch has to be followed by whatever surrounds it
        final boolean arched = isArenaTile() && matchUI.mayView(card) && card.isToken();
        // a tile's thick base is part of its silhouette: what lights it or shades it goes round the base too
        final int tall = isArenaTile() ? cardHeight + Math.max(2, Math.round(cardHeight * 0.025f)) : cardHeight;

        // Shadow beneath the card
        if (!isAnimationPanel) {
            // cast down and away, so that the card sits on the board
            final int drop = isArenaTile() ? Math.max(4, cardHeight / 24) : Math.max(2, cardWidth / 30);
            final int blur = isArenaTile() ? spread : Math.max(2, spread / 2 + 1);
            if (arched) {
                paintTokenGlow(g2d, x, y + drop, cardWidth, tall, arc, Color.BLACK, blur, 190);
            } else {
                ArenaTheme.paintGlowAround(g2d, x, y + drop, cardWidth, tall, arc, Color.BLACK, blur, 190, arc + drop);
            }
        }

        Color glow = null;
        int strength = 230;
        if (cv != null) {
            final boolean actionable = matchUI.isActionable(cv);
            if (matchUI.isHighlighted(cv)) {
                glow = ArenaTheme.CHOSEN;
            } else if (matchUI.isSelectable(cv)) {
                glow = ArenaTheme.SELECTABLE;
            } else if (isArenaStackTarget(cv)) {
                glow = ARENA_TARGET;
                strength = 255;
            } else if (cv.isAttacking()) {
                glow = ARENA_ATTACKER;
                strength = 255;
            } else if (cv.isBlocking()) {
                glow = ARENA_BLOCKER;
                strength = 255;
            } else if (isArenaStackSource(cv)) {
                glow = new Color(0xFFB020);
            } else if (actionable) {
                glow = ArenaTheme.PLAYABLE;
                strength = isSelected ? 255 : 235;
            } else if (isSelected) {
                glow = ArenaTheme.HOVER;
                strength = 150;
            } else if (isPreferenceEnabled(FPref.UI_SHOW_AUTOTAP_PREVIEW) && matchUI.getActionableStrength(cv) >= 2) {
                glow = new Color(255, 236, 120);
                strength = 170;
            } else if (hasFlash && matchUI.mayView(cv)) {
                glow = new Color(110, 230, 255);
                strength = 150;
            }
        }
        if (ArenaTheme.snapshotting) {
            glow = null; // the backdrop of a dialog stays unlit
        }
        final boolean fighting = glow == ARENA_ATTACKER || glow == ARENA_BLOCKER;
        if (glow != null) {
            final int reach = (isArenaTile() ? spread : Math.max(5, Math.round(cardWidth * 0.05f))) + (isSelected ? 2 : 0) + (fighting ? 4 : 0);
            if (arched) {
                paintTokenGlow(g2d, x, y, cardWidth, tall, arc, glow, reach, strength);
            } else {
                ArenaTheme.paintGlowAround(g2d, x, y, cardWidth, tall, arc, glow, reach, strength, arc);
            }
        }

        // Card body; white-bordered and gold-bordered printings keep their border colour
        Color body = Color.black;
        if (cv != null && matchUI.mayView(cv)) {
            final CardStateView state = cv.getCurrentState();
            final CardEdition ed = FModel.getMagicDb().getEditions().get(state.getSetCode());
            if (cv.isImmutable()) {
                body = ArenaTheme.GOLD_DIM;
            } else if (ed != null && state.getFoilIndex() == 0) {
                switch (ed.getBorderColor()) {
                    case WHITE: body = Color.WHITE; break;
                    case GOLD: body = Color.ORANGE; break;
                    case SILVER: body = Color.GRAY; break;
                    default: break;
                }
            }
        }
        if (isArenaTile()) {
            paintArenaTile(g2d, x, y, cardWidth, cardHeight, arc);
        } else {
            g2d.setColor(body);
            g2d.fillRoundRect(x, y, cardWidth, cardHeight, cornerSize, cornerSize);
        }

        if (glow != null) {
            // a solid rim in the bloom's own colour, with a fine bright line along its inside
            final float out = fighting ? 1.6f : 1.2f;
            final Shape rim = arched ? arenaTokenShape(x - out, y - out, cardWidth + 2 * out, tall + 2 * out, arc + 2 * out)
                    : new RoundRectangle2D.Float(x - out, y - out, cardWidth + 2 * out, tall + 2 * out, arc + 2 * out, arc + 2 * out);
            g2d.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 250));
            g2d.setStroke(new BasicStroke(fighting ? 3.2f : 3f));
            g2d.draw(rim);
            final Shape line = arched ? arenaTokenShape(x - 0.5f, y - 0.5f, cardWidth + 1, tall + 1, arc + 1)
                    : new RoundRectangle2D.Float(x - 0.5f, y - 0.5f, cardWidth + 1, tall + 1, arc + 1, arc + 1);
            g2d.setColor(new Color((glow.getRed() + 255) / 2, (glow.getGreen() + 255) / 2, (glow.getBlue() + 255) / 2, 235));
            g2d.setStroke(new BasicStroke(1f));
            g2d.draw(line);
        }
    }

    private static final Color ARENA_ATTACKER = new Color(0xFF6A1F);
    private static final Color ARENA_BLOCKER = new Color(0x4C8DFF);
    private static final Color ARENA_TARGET = new Color(0xBFEFFF);

    /** What the ability or spell on top of the stack is aimed at answers the arc reaching it. */
    private boolean isArenaStackTarget(final CardView cv) {
        final forge.game.GameView game = matchUI.getGameView();
        forge.game.spellability.StackItemView item = game == null ? null : game.peekStack();
        while (item != null) {
            if (item.getTargetCards() != null) {
                for (final CardView target : item.getTargetCards()) {
                    if (cv.equals(target)) {
                        return true;
                    }
                }
            }
            item = item.getSubInstance();
        }
        return false;
    }

    private static Color arenaMix(final Color a, final Color b, final float t) {
        return new Color(Math.round(a.getRed() + (b.getRed() - a.getRed()) * t), Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    /** A token's arched glow, drawn once for each size and colour and kept. */
    private static void paintTokenGlow(final Graphics2D g, final int x, final int y, final int w, final int h, final int arc,
            final Color colour, final int spread, final int strength) {
        final int pad = spread + 1;
        ArenaTheme.paintSprite(g, x - pad, y - pad, w + 2 * pad, h + 2 * pad,
                "token|" + w + "|" + h + "|" + arc + "|" + colour.getRGB() + "|" + spread + "|" + strength, s -> {
                    s.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    paintShapeGlow(s, arenaTokenShape(pad, pad, w, h, arc), colour, spread, strength);
                });
    }

    /** A glow that follows any outline: nested strokes, faint and wide to strong and narrow. */
    private static void paintShapeGlow(final Graphics2D g, final Shape shape, final Color colour, final int spread, final int strength) {
        final int steps = Math.max(1, Math.min(6, spread));
        for (int i = steps; i >= 1; i--) {
            final float t = 1f - (i - 1f) / steps;
            g.setColor(new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), Math.min(255, Math.round(strength * 0.22f * t))));
            g.setStroke(new BasicStroke(2f * i * spread / steps, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(shape);
        }
        g.setStroke(new BasicStroke(1f));
    }

    /** The permanent whose ability is on top of the stack shows which one it is. */
    private boolean isArenaStackSource(final CardView cv) {
        final forge.game.GameView game = matchUI.getGameView();
        final forge.game.spellability.StackItemView top = game == null ? null : game.peekStack();
        return top != null && cv.equals(top.getSourceCard()) && ZoneType.Battlefield.equals(cv.getZone());
    }

    /** During a fight everything not in it steps back a little. */
    private boolean isArenaSidelined() {
        final forge.game.GameView game = matchUI.getGameView();
        final forge.game.combat.CombatView combat = game == null ? null : game.getCombat();
        return combat != null && combat.getNumAttackers() > 0 && !card.isAttacking() && !card.isBlocking()
                && !matchUI.isSelectable(card) && !matchUI.isHighlighted(card);
    }

    /** On the Arena board, visible battlefield cards are drawn as compact art tiles rather than full cards. */
    private boolean isArenaTile() {
        return ArenaTheme.isEnabled() && !isAnimationPanel && card != null && ZoneType.Battlefield.equals(card.getZone());
    }

    /** Text in the bottom box of a tile: power/toughness, or loyalty; null when the card has neither. */
    private String getArenaTileStats() {
        final CardStateView state = card.getCurrentState();
        if (state.isCreature() || state.isVehicle()) {
            return state.getPower() + "/" + state.getToughness();
        }
        return state.isPlaneswalker() ? state.getLoyalty() : null;
    }

    /**
     * Where the parts of a tile go. A tile is a black slab with a thin frame in the card's colour set inside
     * it, and the slab shows as a footer under the frame: the name on a pale lozenge at the top of the frame,
     * the art filling the rest, and the plate and chips sitting across the art's lower edge and the footer.
     */
    private record TileLayout(int line, int nameH, int row, Rectangle frame, Rectangle bar, Rectangle art, Rectangle stats) { }

    private TileLayout layoutArenaTile(final Graphics g) {
        final int x = cardXOffset, y = cardYOffset, w = cardWidth, h = cardHeight;
        final String stats = matchUI.mayView(card) ? getArenaTileStats() : null;
        final boolean token = card.isToken() && matchUI.mayView(card);
        final int edge = Math.max(3, Math.round(w * 0.026f));
        // the footer is there to carry the power/toughness: with none, the art runs on down to the bezel
        final int footer = stats != null ? Math.max(9, Math.round(h * 0.125f)) : edge;
        final int line = Math.max(2, Math.round(w * 0.021f));
        final int gap = Math.max(2, Math.round(w * 0.011f));
        final Rectangle frame = new Rectangle(x + edge, y + edge, w - 2 * edge, h - edge - footer);
        final int nameH = token ? 0 : Math.max(13, Math.round(h * 0.1f));
        final Rectangle bar = new Rectangle(frame.x + gap, frame.y + gap, frame.width - 2 * gap, nameH);
        final int artTop = token ? frame.y + line : bar.y + bar.height + gap;
        final Rectangle art = new Rectangle(frame.x + line, artTop, frame.width - 2 * line, frame.y + frame.height - line - artTop);
        final int row = Math.max(15, Math.round(h * 0.19f));
        Rectangle statsBox = null;
        if (stats != null) {
            final int textW = g.getFontMetrics(ArenaTheme.numeralFont(Font.BOLD, row * 0.64f)).stringWidth(stats);
            final int bw = Math.max(Math.round(w * 0.34f), textW + Math.round(row * 0.9f));
            statsBox = new Rectangle(x + w - bw - Math.round(w * 0.022f), y + h - row - Math.round(h * 0.022f), bw, row);
        }
        return new TileLayout(line, nameH, row, frame, bar, art, statsBox);
    }

    /**
     * The art crop for a tile, at the art window's size in device pixels. It is cut from
     * a copy of the card image fetched large enough that the art box needs no upscaling.
     */
    private BufferedImage getArenaTileArt(final int width, final int height) {
        final float screenScale = GuiBase.getInterface().getScreenScale();
        final int pw = Math.max(1, Math.round(width * screenScale)), ph = Math.max(1, Math.round(height * screenScale));
        // the art box is about 45% of a card's height
        final int srcH = Math.min(1040, Math.max(Math.round(cardHeight * screenScale), Math.round(ph / 0.45f)));
        if (tileSource == null || tileSourceHeight != srcH) {
            tileSourceHeight = srcH;
            tileSource = new CachedCardImage(card, matchUI.getLocalPlayers(), Math.round(srcH / ASPECT_RATIO), srcH) {
                @Override
                public void onImageFetched() {
                    tileArt = null;
                    repaint();
                }
            };
            tileArt = null;
        }
        final BufferedImage src = tileSource.getImage();
        if (src == null) {
            return null;
        }
        if (tileArt == null || tileArtSource != src || tileArt.getWidth() != pw || tileArt.getHeight() != ph) {
            // the art box of a standard frame: about 10% in from the sides, 11% to 56% down; cover-fit it
            final float sx = src.getWidth() * 0.1f, sy = src.getHeight() * 0.11f;
            final float sw = src.getWidth() * 0.8f, sh = src.getHeight() * 0.45f;
            final float fit = Math.max(pw / sw, ph / sh);
            final float cw = pw / fit, ch = ph / fit;
            final float cx = sx + (sw - cw) / 2f, cy = sy + (sh - ch) * 0.22f;
            final BufferedImage art = new BufferedImage(pw, ph, BufferedImage.TYPE_INT_RGB);
            final Graphics2D ag = art.createGraphics();
            ag.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            ag.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            ag.drawImage(src, 0, 0, pw, ph, Math.round(cx), Math.round(cy), Math.round(cx + cw), Math.round(cy + ch), null);
            ag.dispose();
            final java.awt.image.Kernel crisp = new java.awt.image.Kernel(3, 3, new float[] {0f, -0.2f, 0f, -0.2f, 1.8f, -0.2f, 0f, -0.2f, 0f});
            tileArt = new java.awt.image.ConvolveOp(crisp, java.awt.image.ConvolveOp.EDGE_NO_OP, null).filter(art, null);
            tileArtSource = src;
        }
        return tileArt;
    }

    /**
     * Arena battlefield tile: see {@link #layoutArenaTile}. A tapped tile is tilted by paint(); here it all goes
     * to grey, and a large translucent arrow lies over its art.
     */
    private void paintArenaTile(final Graphics2D g, final int x, final int y, final int w, final int h, final int arc) {
        final CardStateView state = card.getCurrentState();
        final boolean known = matchUI.mayView(card);
        final TileLayout tl = layoutArenaTile(g);
        final Rectangle a = tl.art(), f = tl.frame(), b = tl.bar();
        final Color tint = known ? ArenaTheme.frameLine(ArenaTheme.tileTint(state)) : new Color(0x8A8F98);
        final boolean tapped = isTapped();
        final boolean token = known && card.isToken();
        final Shape outline = token ? arenaTokenShape(x, y, w, h, arc) : new RoundRectangle2D.Float(x, y, w, h, arc, arc);

        // the slab: near-black, a little thicker at the foot, its top edge just catching the light
        final int lip = Math.max(2, Math.round(h * 0.025f));
        final java.awt.geom.AffineTransform oldT = g.getTransform();
        g.translate(0, lip);
        g.setColor(new Color(3, 3, 5));
        g.fill(outline);
        g.setTransform(oldT);
        g.setPaint(new GradientPaint(0, y, new Color(0x202026), 0, y + h, new Color(0x0B0B0E)));
        g.fill(outline);
        g.setPaint(new GradientPaint(0, y, new Color(255, 255, 255, 60), 0, y + h * 0.5f, new Color(255, 255, 255, 0)));
        g.setStroke(new BasicStroke(1f));
        g.draw(outline);

        // the thin frame in the card's colour; it is the frame's colour that shows round the art and behind the name
        final Color line = tapped ? arenaMix(ArenaTheme.fade(tint), new Color(0x4A4A4E), 0.55f) : tint;
        final float frameArc = Math.max(5f, arc * 0.7f);
        final Shape frame = token ? arenaTokenShape(f.x, f.y, f.width, f.height, frameArc) : new RoundRectangle2D.Float(f.x, f.y, f.width, f.height, frameArc, frameArc);
        g.setPaint(new GradientPaint(0, f.y, ArenaTheme.lift(line), 0, f.y + f.height, line.darker()));
        g.fill(frame);

        if (a.width > 0 && a.height > 0) {
            BufferedImage art = getArenaTileArt(a.width, a.height);
            final Shape oldClip = g.getClip();
            final float artArc = Math.max(3f, frameArc - tl.line());
            final Shape artShape = token ? arenaTokenShape(a.x, a.y, a.width, a.height, artArc) : new RoundRectangle2D.Float(a.x, a.y, a.width, a.height, artArc, artArc);
            g.clip(artShape);
            if (art != null) {
                if (tapped) {
                    art = getTappedTileArt(art);
                }
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.drawImage(art, a.x, a.y, a.width, a.height, null);
            } else {
                g.setColor(ArenaTheme.NAVY);
                g.fill(a);
            }
            // the art is set into the frame, not laid on it
            g.setColor(new Color(0, 0, 0, 120));
            g.setStroke(new BasicStroke(3f));
            g.draw(artShape);
            g.setStroke(new BasicStroke(1f));
            g.setClip(oldClip);
        }

        // the name, small and dark, on a pale lozenge in a cast of the card's colour
        if (tl.nameH() > 0) {
            final Color pale = tapped ? new Color(0x87878B) : arenaMix(tint, Color.WHITE, 0.64f);
            final float barArc = b.height * 0.62f;
            final RoundRectangle2D lozenge = new RoundRectangle2D.Float(b.x, b.y, b.width, b.height, barArc, barArc);
            g.setPaint(new GradientPaint(0, b.y, pale, 0, b.y + b.height, arenaMix(pale, Color.BLACK, 0.1f)));
            g.fill(lozenge);
            g.setColor(new Color(10, 10, 12, 120));
            g.draw(lozenge);
            if (!arenaStackBack) {
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                String name = known ? CardTranslation.getTranslatedName(state.getName()) : "";
                final int pad = Math.max(4, b.height / 3);
                final int maxNameW = b.width - 2 * pad;
                // one size on every tile; only a very long name gives way, first by a little, then with an ellipsis
                float nameSize = Math.max(10.5f, b.height * 0.8f);
                final float minSize = nameSize * 0.86f;
                g.setFont(ArenaTheme.displayFont(Font.BOLD, nameSize));
                while (nameSize > minSize && g.getFontMetrics().stringWidth(name) > maxNameW) {
                    nameSize -= 0.25f;
                    g.setFont(ArenaTheme.displayFont(Font.BOLD, nameSize));
                }
                final FontMetrics fm = g.getFontMetrics();
                if (fm.stringWidth(name) > maxNameW) {
                    while (name.length() > 1 && fm.stringWidth(name + "\u2026") > maxNameW) {
                        name = name.substring(0, name.length() - 1);
                    }
                    name = name + "\u2026";
                }
                g.setColor(new Color(16, 14, 12));
                g.drawString(name, b.x + pad, b.y + (b.height + fm.getAscent() - fm.getDescent()) / 2f);
            }
        }

        if (tapped && a.width > 0) {
            final java.awt.Composite solid = g.getComposite();
            g.setComposite(java.awt.AlphaComposite.SrcOver.derive(0.72f));
            ArenaArt.paintTapArrow(g, a.x + a.width / 2, a.y + a.height / 2, Math.round(Math.min(a.width, a.height) * 0.88f));
            g.setComposite(solid);
        }

        // power/toughness: a tinted plate in a pale rim with a dark edge, across the art's lower edge and the footer
        final Rectangle sb = tl.stats();
        if (sb != null && !arenaStackBack) {
            final String stats = getArenaTileStats();
            final float pa = sb.height * 0.5f;
            final int rim = Math.max(2, Math.round(sb.height * 0.09f));
            g.setColor(new Color(4, 4, 6));
            g.fill(new RoundRectangle2D.Float(sb.x - 1.5f, sb.y - 1.5f, sb.width + 3f, sb.height + 3f, pa + 2, pa + 2));
            g.setPaint(new GradientPaint(0, sb.y, tapped ? new Color(0xB4B4B4) : Color.WHITE, 0, sb.y + sb.height, tapped ? new Color(0x707070) : new Color(0xA8A49A)));
            g.fill(new RoundRectangle2D.Float(sb.x, sb.y, sb.width, sb.height, pa, pa));
            final Color top = tapped ? new Color(0xA6A6A6) : arenaMix(new Color(0xFBF8EF), tint, 0.17f);
            final Color bottom = tapped ? new Color(0x868686) : arenaMix(new Color(0xE0DACC), tint, 0.27f);
            final RoundRectangle2D plate = new RoundRectangle2D.Float(sb.x + rim, sb.y + rim, sb.width - 2f * rim, sb.height - 2f * rim, pa - rim, pa - rim);
            g.setPaint(new GradientPaint(0, sb.y + rim, top, 0, sb.y + sb.height - rim, bottom));
            g.fill(plate);
            g.setColor(new Color(50, 44, 36, 170));
            g.draw(plate);
            g.setFont(ArenaTheme.numeralFont(Font.BOLD, sb.height * 0.64f));
            final FontMetrics sfm = g.getFontMetrics();
            g.setColor(tapped ? new Color(16, 16, 18) : getArenaStatsColor(state));
            g.drawString(stats, sb.x + (sb.width - sfm.stringWidth(stats)) / 2f, sb.y + (sb.height + sfm.getAscent() - sfm.getDescent()) / 2f);
        }

        if (isArenaSidelined()) {
            g.setColor(new Color(0, 0, 0, 64));
            g.fill(outline);
        }
    }

    /** A token's tile is arched at the top, as on the Arena board. */
    private static Shape arenaTokenShape(final float x, final float y, final float w, final float h, final float arc) {
        final float rise = h * 0.2f;
        final java.awt.geom.Path2D.Float path = new java.awt.geom.Path2D.Float();
        path.moveTo(x, y + rise);
        path.curveTo(x + w * 0.16f, y + rise * 0.25f, x + w * 0.34f, y, x + w / 2f, y);
        path.curveTo(x + w * 0.66f, y, x + w * 0.84f, y + rise * 0.25f, x + w, y + rise);
        path.lineTo(x + w, y + h - arc / 2f);
        path.quadTo(x + w, y + h, x + w - arc / 2f, y + h);
        path.lineTo(x + arc / 2f, y + h);
        path.quadTo(x, y + h, x, y + h - arc / 2f);
        path.closePath();
        return path;
    }

    /** Stats in dark type; blue when above the printed values, red when wounded. */
    private Color getArenaStatsColor(final CardStateView state) {
        if (card.getDamage() > 0) {
            return new Color(0xB3261E);
        }
        if (state.isCreature() && !card.isToken()) {
            final PaperCard printed = FModel.getMagicDb().getCommonCards().getCard(state.getName());
            if (printed != null && printed.getRules() != null) {
                final int basePower = printed.getRules().getIntPower(), baseToughness = printed.getRules().getIntToughness();
                if (state.getPower() > basePower || state.getToughness() > baseToughness) {
                    return new Color(0x1450A8);
                }
            }
        }
        return new Color(20, 18, 16);
    }

    /** The art with its colour mostly gone and its light turned down, as Arena shows anything tapped. */
    private BufferedImage getTappedTileArt(final BufferedImage art) {
        if (tileArtTappedFrom != art) {
            final int aw = art.getWidth(), ah = art.getHeight();
            // every tapped card ends up about equally dark, whether its art was a white sky or a black cave
            long sum = 0;
            for (int py = 0; py < ah; py++) {
                for (int px = 0; px < aw; px++) {
                    final int rgb = art.getRGB(px, py);
                    sum += (((rgb >> 16) & 255) * 30 + ((rgb >> 8) & 255) * 59 + (rgb & 255) * 11) / 100;
                }
            }
            final float mean = Math.max(1f, sum / (float) (aw * ah));
            final BufferedImage grey = new BufferedImage(aw, ah, BufferedImage.TYPE_INT_RGB);
            for (int py = 0; py < ah; py++) {
                for (int px = 0; px < aw; px++) {
                    final int rgb = art.getRGB(px, py);
                    final int r = (rgb >> 16) & 255, gr = (rgb >> 8) & 255, b = rgb & 255;
                    final int l = (r * 30 + gr * 59 + b * 11) / 100;
                    // centred on the art's own brightness, so a pale sky and a black cave both keep their shapes
                    final float dim = Math.max(26f, Math.min(138f, (l - mean) * 0.9f + 80f));
                    final int nr = Math.max(0, Math.min(255, Math.round(dim + (r - l) * 0.08f - 2f)));
                    final int ng = Math.max(0, Math.min(255, Math.round(dim + (gr - l) * 0.08f)));
                    final int nb = Math.max(0, Math.min(255, Math.round(dim + (b - l) * 0.08f + 4f)));
                    grey.setRGB(px, py, (nr << 16) | (ng << 8) | nb);
                }
            }
            tileArtTapped = grey;
            tileArtTappedFrom = art;
        }
        return tileArtTapped;
    }

    /**
     * Everything else on a tile, kept to its bottom edge as on the Arena board. Filling
     * leftwards from the stats box: counters and markers as chips ("1x +1/+1"), then
     * summoning sickness and phasing; keyword icons fill in from the left edge. Chips
     * that do not fit on that row stack up above the stats box. Damage is a red badge.
     */
    private void drawArenaTileOverlays(final Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        final TileLayout tl = layoutArenaTile(g);
        final Rectangle a = tl.art(), sb = tl.stats();
        final int row = tl.row();
        final int rowY = sb != null ? sb.y : cardYOffset + cardHeight - row - Math.round(cardHeight * 0.03f);
        int rx = (sb != null ? sb.x : cardXOffset + cardWidth - Math.round(cardWidth * 0.03f)) - 3; // next free x, moving left

        final List<String> chips = new ArrayList<>();
        if (card.getCounters() != null) {
            for (final Multiset.Entry<CounterType> e : card.getCounters().entrySet()) {
                final CounterType counter = e.getElement();
                chips.add(e.getCount() + "x " + counter.getCounterOnCardDisplayName());
            }
        }
        if (card.getMarkerText() != null) {
            for (final String marker : card.getMarkerText()) {
                chips.add(marker);
            }
        }
        // smaller than the plate they sit beside
        final int chipH = Math.round(row * 0.7f);
        g.setFont(ArenaTheme.numeralFont(Font.BOLD, Math.max(10f, chipH * 0.72f)));
        final FontMetrics fm = g.getFontMetrics();
        final int stackRight = sb != null ? sb.x + sb.width : a.x + a.width - 1;
        final int chipRowY = rowY + row - chipH;
        int overflowY = chipRowY;
        for (int i = 0; i < chips.size(); i++) {
            final int cw = fm.stringWidth(chips.get(i)) + chipH * 3 / 4;
            final int chipX, chipY;
            if (rx - cw >= a.x + 1) {
                chipX = rx - cw;
                chipY = chipRowY;
                rx -= cw + 3;
            } else {
                // no room on the bottom row: stack above the stats box
                overflowY -= chipH + 2;
                if (overflowY < a.y) {
                    break;
                }
                chipX = Math.max(a.x + 1, stackRight - cw);
                chipY = overflowY;
            }
            final RoundRectangle2D chip = new RoundRectangle2D.Float(chipX, chipY, cw, chipH, chipH * 0.5f, chipH * 0.5f);
            g.setPaint(new GradientPaint(0, chipY, new Color(0xF4F2EC), 0, chipY + chipH, new Color(0xC6C3BA)));
            g.fill(chip);
            g.setColor(new Color(10, 10, 12, 210));
            g.draw(chip);
            g.setColor(new Color(14, 14, 18));
            g.drawString(chips.get(i), chipX + (cw - fm.stringWidth(chips.get(i))) / 2, chipY + (chipH + fm.getAscent() - fm.getDescent()) / 2);
        }

        int ix = cardXOffset + Math.round(cardWidth * 0.03f);
        if (card.isPhasedOut() && ix + row <= rx - 1) {
            paintArenaBadge(g, ix, rowY, row);
            CardFaceSymbols.drawSymbol("phasing", g, ix + 2, rowY + 2, row - 4);
            ix += row + 2;
        }
        if (showAbilityIcons()) {
            for (final FSkinProp prop : FSkinProp.iconsFromCardState(card.getCurrentState())) {
                if (ix + row > rx - 1) {
                    break;
                }
                paintArenaBadge(g, ix, rowY, row);
                g.drawImage(ArenaTheme.darkIcon(prop.name(), FSkin.getImage(prop).getIcon().getImage()), ix + 2, rowY + 2, row - 4, row - 4, null);
                ix += row + 2;
            }
        }
        if (card.isSick() && ix + row <= rx - 1) {
            // summoning sick: a small moon-and-z glyph in the same chip
            paintArenaBadge(g, ix, rowY, row);
            g.setColor(new Color(14, 14, 18));
            g.setFont(ArenaTheme.displayFont(Font.BOLD, row * 0.62f));
            final FontMetrics zz = g.getFontMetrics();
            g.drawString("z", ix + (row - zz.stringWidth("z")) / 2 - 1, rowY + (row + zz.getAscent() - zz.getDescent()) / 2);
            g.setFont(ArenaTheme.displayFont(Font.BOLD, row * 0.42f));
            g.drawString("z", ix + row / 2 + 1, rowY + row / 2);
            ix += row + 2;
        }

        if (card.getDamage() > 0) {
            final String dmg = String.valueOf(card.getDamage());
            g.setFont(ArenaTheme.numeralFont(Font.BOLD, Math.max(10f, row * 0.75f)));
            final FontMetrics dfm = g.getFontMetrics();
            final int d = Math.max(row, dfm.stringWidth(dmg) + 8);
            final int dx = a.x + a.width - d - 1, dy = a.y + 2;
            g.setColor(new Color(178, 30, 36, 240));
            g.fillOval(dx, dy, d, d);
            g.setColor(new Color(255, 170, 170));
            g.drawOval(dx, dy, d, d);
            g.setColor(Color.WHITE);
            g.drawString(dmg, dx + (d - dfm.stringWidth(dmg)) / 2, dy + (d + dfm.getAscent() - dfm.getDescent()) / 2);
        }
    }

    /** The one badge style on a tile: a pale rounded square with a dark edge, for a black glyph. */
    private static void paintArenaBadge(final Graphics2D g, final int x, final int y, final int size) {
        final RoundRectangle2D chip = new RoundRectangle2D.Float(x, y, size, size, size * 0.34f, size * 0.34f);
        g.setPaint(new GradientPaint(0, y, new Color(0xF4F2EC), 0, y + size, new Color(0xC6C3BA)));
        g.fill(chip);
        g.setColor(new Color(10, 10, 12, 210));
        g.draw(chip);
    }

    /** Pref is normalized to 6 hex chars on the write side; this just parses,
     *  falling back to the FPref default if the stored value is malformed. */
    private static Color parseActionableHighlightColor() {
        String s = forge.model.FModel.getPreferences().getPref(FPref.UI_ACTIONABLE_HIGHLIGHT_COLOR);
        try {
            if (s != null && s.length() == 6) return new Color(Integer.parseInt(s, 16));
        } catch (NumberFormatException ignored) {}
        return new Color(Integer.parseInt(FPref.UI_ACTIONABLE_HIGHLIGHT_COLOR.getDefault(), 16));
    }

    private void drawManaCost(final Graphics g, final ManaCost cost, final int deltaY) {
        final int width = CardFaceSymbols.getWidth(cost);
        final int height = CardFaceSymbols.getHeight();
        final int counterOffset = card.getCounters() != null && !card.getCounters().isEmpty() ? 15 : 0;
        CardFaceSymbols.draw(g, cost, (cardXOffset + (cardWidth / 2)) - (width / 2), deltaY + cardYOffset + (cardHeight / 2) - height/2 + counterOffset);
    }

    @Override
    protected final void paintChildren(final Graphics g) {
        if (ArenaTheme.isEnabled() && !isArenaTile()) {
            // Arena cards have rounded corners all the way through, image included
            final Graphics2D cg = (Graphics2D) g.create();
            final int border = calculateBorderSize();
            final int arc = Math.max(4, Math.round(cardWidth * CardPanel.ROUNDED_CORNER_SIZE)) - border;
            cg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            cg.clip(new RoundRectangle2D.Float(cardXOffset + border, cardYOffset + border,
                    cardWidth - 2 * border, cardHeight - 2 * border, arc, arc));
            super.paintChildren(cg);
            cg.dispose();
        } else {
            super.paintChildren(g);
        }

        if (isAnimationPanel || card == null) {
            return;
        }

        final boolean canShow = matchUI.mayView(card);
        if (canShow && ZoneType.Battlefield.equals(card.getZone())) {
            CardStateView cardStateView = card.getCurrentState();
            if (card.isSplitCard() && card.hasAlternateState() && !card.isFaceDown() && !CardStateName.Original.equals(cardStateView.getState())) {
                switch (cardStateView.getState()) {
                    case EmptyRoom -> {
                        FSkin.drawImage(g, FSkin.getIcon(FSkinProp.ICO_PADLOCK), cardXOffset, cardYOffset + cardHeight / 2, cardWidth, cardHeight);
                        FSkin.drawImage(g, FSkin.getIcon(FSkinProp.ICO_PADLOCK), cardXOffset, cardYOffset, cardWidth, cardHeight);
                    }
                    case RightSplit ->
                            FSkin.drawImage(g, FSkin.getIcon(FSkinProp.ICO_PADLOCK), cardXOffset, cardYOffset + cardHeight / 2, cardWidth, cardHeight);
                    case LeftSplit ->
                            FSkin.drawImage(g, FSkin.getIcon(FSkinProp.ICO_PADLOCK), cardXOffset, cardYOffset, cardWidth, cardHeight);
                }
            }
        }
        displayIconOverlay(g, canShow);
        if (groupCount >= 2) {
            drawGroupCountBadge(g);
        }
        if (hotkeyDigit > 0 && cardWidth >= HOTKEY_BADGE_MIN_CARD_WIDTH) {
            drawHotkeyDigitBadge(g);
        }
        if (zoneBannerText != null) {
            drawZoneBanner(g);
        }
        if (arenaCostWidth > 0 && ArenaTheme.isEnabled() && canShow) {
            drawArenaCostRow((Graphics2D) g);
        }
        if (canShow) {
            drawFoilEffect(g, card, cardXOffset, cardYOffset,
                    cardWidth, cardHeight, Math.round(cardWidth * BLACK_BORDER_SIZE));
        }

        boolean nonselectable = matchUI.isSelecting() && !matchUI.isSelectable(getCard());
        // if selecting, darken non-selectable cards
        if (nonselectable) {
            boolean noBorderPref = !isPreferenceEnabled(FPref.UI_RENDER_BLACK_BORDERS);
            boolean cardImgHasAlpha = imagePanel != null && imagePanel.getSrcImage() != null && imagePanel.getSrcImage().getColorModel().hasAlpha();
            final int cornerSize = noBorderPref && !cardImgHasAlpha ? 0 : Math.max(4, Math.round(cardWidth * CardPanel.ROUNDED_CORNER_SIZE));
            final int offset = isTapped() && (!noBorderPref || cardImgHasAlpha) ? 1 : 0;
            g.setColor(new Color(0.0f,0.0f,0.0f,0.6f));
            g.fillRoundRect(cardXOffset, cardYOffset  + offset, cardWidth, cardHeight, cornerSize, cornerSize);
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
        int borderSize = calculateBorderSize();

        final Point imgPos = new Point(cardXOffset + borderSize, cardYOffset + borderSize);
        final Dimension imgSize = calculateImageSize();

        imagePanel.setLocation(imgPos);
        imagePanel.setSize(imgSize);
        final boolean tile = isArenaTile();
        imagePanel.setVisible(!tile);

        final boolean canShow = matchUI.mayView(card);
        final boolean showText = !imagePanel.hasImage() || !isAnimationPanel;

        // Arena cards carry no text over the card image; tiles draw their own name and stats
        final boolean arena = ArenaTheme.isEnabled();
        displayCardNameOverlay(showText && canShow && showCardNameOverlay() && !arena, imgSize, imgPos);
        displayPTOverlay(showText && (canShow || card.isFaceDown()) && showCardPowerOverlay() && !arena, imgSize, imgPos);
        // Arena shows no card numbers, in hand or on the battlefield
        displayCardIdOverlay(showText && canShow && showCardIdOverlay() && !ArenaTheme.isEnabled(), imgSize, imgPos);
    }

    private int calculateBorderSize() {
        if (ArenaTheme.isEnabled()) {
            return Math.max(1, Math.round(cardWidth * 0.02f));
        }
        // Determine whether to render border from properties
        boolean noBorderPref = !isPreferenceEnabled(FPref.UI_RENDER_BLACK_BORDERS);

        // Borderless cards should be accounted for here
        // Amonkhet Invocations
        boolean noBorderOnCard = getCard().getCurrentState().getSetCode().equalsIgnoreCase("MPS_AKH");
        // Unstable basic lands
        noBorderOnCard |= getCard().getCurrentState().isBasicLand() && getCard().getCurrentState().getSetCode().equalsIgnoreCase("UST");

        boolean cardImgHasAlpha = imagePanel != null && imagePanel.getSrcImage() != null && imagePanel.getSrcImage().getColorModel().hasAlpha();

        if (!noBorderPref && !(noBorderOnCard && cardImgHasAlpha)) {
            // A 2 px border is necessary to ensure the rounded card corners don't glitch when the card is highlighted
            return noBorderOnCard ? 2 : Math.round(cardWidth * CardPanel.BLACK_BORDER_SIZE);
        }

        return 0;
    }

    private Dimension calculateImageSize() {
        int borderSize = calculateBorderSize();
        return new Dimension(cardWidth - (borderSize * 2), cardHeight - (borderSize * 2));
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
        if (isVisible && ArenaTheme.isEnabled()) {
            // Arena: badges tucked into the bottom-right corner, damage stacked above P/T
            final int inset = Math.max(2, imgSize.width / 40);
            final Dimension ptSize = ptText.getPreferredSize();
            ptText.setSize(ptSize);
            final int ptX = imgSize.width - ptSize.width - inset;
            final int ptY = imgSize.height - ptSize.height - inset;
            ptText.setLocation(imgPos.x + ptX, imgPos.y + ptY);
            final Dimension dmgSize = damageText.getPreferredSize();
            damageText.setSize(dmgSize);
            damageText.setLocation(imgPos.x + imgSize.width - dmgSize.width - inset, imgPos.y + ptY - dmgSize.height - 2);
        }
        else if (isVisible) {
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

    private void drawGroupCountBadge(final Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (ArenaTheme.isEnabled()) {
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setFont(ArenaTheme.displayFont(Font.BOLD, Math.max(11f, cardHeight * 0.125f)));
            final FontMetrics chip = g2d.getFontMetrics();
            final String label = "x" + groupCount;
            final int cw = chip.stringWidth(label) + 10, ch = chip.getAscent() + 3;
            // outside the corner, clear of the name
            final int cx = cardXOffset - Math.round(cardWidth * 0.05f), cy = cardYOffset - ch - 3;
            g2d.setColor(new Color(6, 6, 8, 240));
            g2d.fillRoundRect(cx, cy, cw, ch, 6, 6);
            g2d.setColor(new Color(255, 255, 255, 190));
            g2d.drawRoundRect(cx, cy, cw, ch, 6, 6);
            g2d.setColor(Color.WHITE);
            g2d.drawString(label, cx + 5, cy + ch - 3 - chip.getDescent() / 2);
            return;
        }

        if (badgeFont == null || badgeFontCardWidth != cardWidth) {
            badgeFont = new Font("Dialog", Font.BOLD, Math.max(10, cardWidth / 5));
            badgeFontCardWidth = cardWidth;
        }

        String text = "\u00D7" + groupCount;
        FontMetrics fm = g2d.getFontMetrics(badgeFont);

        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getAscent();
        int padX = Math.max(4, cardWidth / 20);
        int padY = Math.max(2, cardHeight / 30);
        int badgeWidth = textWidth + padX * 2;
        int badgeHeight = textHeight + padY * 2;
        int badgeX = cardXOffset + 2;
        int badgeY = cardYOffset + 2;
        int cornerRadius = Math.max(4, cardWidth / 16);

        g2d.setColor(BADGE_BG_COLOR);
        g2d.fillRoundRect(badgeX, badgeY, badgeWidth, badgeHeight, cornerRadius, cornerRadius);

        g2d.setColor(Color.WHITE);
        g2d.setFont(badgeFont);
        g2d.drawString(text, badgeX + padX, badgeY + padY + textHeight);
    }

    public int getHotkeyDigit() {
        return hotkeyDigit;
    }

    public void setHotkeyDigit(final int digit) {
        if (hotkeyDigit == digit) return;
        hotkeyDigit = digit;
        repaint();
    }

    private void drawHotkeyDigitBadge(final Graphics g) {
        final Graphics2D g2d = (Graphics2D) g.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            final int diameter = Math.min(HOTKEY_BADGE_MAX_DIAMETER, cardWidth / 5);
            final int bx = cardXOffset + cardWidth - diameter - 2;
            final int by = cardYOffset + 2;
            g2d.setColor(HOTKEY_BADGE_BG);
            g2d.fillOval(bx, by, diameter, diameter);
            g2d.setColor(HOTKEY_BADGE_RING);
            g2d.drawOval(bx, by, diameter, diameter);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Dialog", Font.BOLD, Math.max(11, diameter * 2 / 3)));
            final String text = String.valueOf(hotkeyDigit);
            final FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(text, bx + (diameter - fm.stringWidth(text)) / 2,
                    by + (diameter + fm.getAscent()) / 2 - 2);
        } finally {
            g2d.dispose();
        }
    }

    public boolean isBadgeHit(int mouseX, int mouseY) {
        if (groupCount < 2) {
            return false;
        }
        // Badge is drawn at (cardXOffset+2, cardYOffset+2) in the card's local
        // coordinate space. Mouse coordinates are container-relative. When the
        // card is tapped, the graphics are rotated but mouse events are not, so
        // we must inverse-rotate the mouse point into the card's local frame.
        int localX = mouseX - getX();
        int localY = mouseY - getY();
        if (tappedAngle > 0) {
            float pivotX = cardXOffset + cardWidth / 2f;
            float pivotY = cardYOffset + cardHeight - cardWidth / 2f;
            double cos = Math.cos(-tappedAngle);
            double sin = Math.sin(-tappedAngle);
            float dx = localX - pivotX;
            float dy = localY - pivotY;
            localX = (int) Math.round(cos * dx - sin * dy + pivotX);
            localY = (int) Math.round(sin * dx + cos * dy + pivotY);
        }
        int badgeX = cardXOffset + 2;
        int badgeY = cardYOffset + 2;
        int badgeWidth = Math.max(30, cardWidth / 3);
        int badgeHeight = Math.max(20, cardHeight / 6);
        return localX >= badgeX && localX <= badgeX + badgeWidth
            && localY >= badgeY && localY <= badgeY + badgeHeight;
    }

    private void displayIconOverlay(final Graphics g, final boolean canShow) {
        if (ArenaTheme.isEnabled()) {
            hasFlash = computeHasFlash();
            if (isArenaTile()) {
                drawArenaTileOverlays((Graphics2D) g);
            }
            if (matchUI.isHighlighted(card)) {
                CardFaceSymbols.drawSymbol("sacrifice", g, (cardXOffset + (cardWidth / 2)) - 20,
                        (cardYOffset + (cardHeight / 2)) - 20);
            }
            if (ghost) {
                drawGhostOverlay(g);
            }
            return;
        }
        if (canShow && showCardManaCostOverlay() && cardWidth < 200) {
            final boolean showSplitMana = card.isSplitCard() && card.getZone() != ZoneType.Battlefield;
            boolean showPerpetualManaCost = showPerpetualManaCost();
            if (!showSplitMana) {
                ManaCost manaCost = showPerpetualManaCost ? card.getCurrentState().getManaCost() : card.getCurrentState().getOriginalManaCost();
                drawManaCost(g, manaCost, 0);
            } else {
                if (!card.isFaceDown()) { // no need to draw mana symbols on face down split cards (e.g. manifested)
                    PaperCard pc = null;
                    if (!card.getOracleName().isEmpty()) {
                        pc = StaticData.instance().getCommonCards().getCard(card.getOracleName());
                    }
                    int ofs = pc != null && Card.getCardForUi(pc).hasKeyword(Keyword.AFTERMATH) ? -12 : 12;
                    ManaCost leftManaCost = showPerpetualManaCost ? card.getLeftSplitState().getManaCost() : card.getLeftSplitState().getOriginalManaCost();
                    ManaCost rightManaCost = showPerpetualManaCost ? card.getRightSplitState().getManaCost() : card.getRightSplitState().getOriginalManaCost();
                    drawManaCost(g, leftManaCost, ofs);
                    drawManaCost(g, rightManaCost, -ofs);
                }
            }
        }

        if (card.getCounters() != null && !card.getCounters().isEmpty()) {
            switch (CounterDisplayType.from(FModel.getPreferences().getPref(FPref.UI_CARD_COUNTER_DISPLAY_TYPE))) {
                case OLD_WHEN_SMALL:
                case TEXT:
                    drawCounterTabs(g);
                    break;
                case IMAGE:
                    drawCounterImage(g);
                    break;
                case HYBRID:
                    drawCounterImage(g);
                    drawCounterTabs(g);
                    break;
            }
        }

        if(card.getMarkerText() != null) {
            drawMarkersTabs(g, card.getMarkerText());
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

        if (matchUI.isHighlighted(card)) {
            CardFaceSymbols.drawSymbol("sacrifice", g, (cardXOffset + (cardWidth / 2)) - 20,
                    (cardYOffset + (cardHeight / 2)) - 20);
        }

        //Ability Icons

        int abiScale = cardWidth / 7;
        int abiX = cardXOffset + (cardWidth / 2) + (cardWidth / 3);
        int abiSpace = (cardWidth / 7);
        int abiY = cardWidth < 200 ? cardYOffset + 25 : cardYOffset + 50;
        hasFlash = false;
        if (!showAbilityIcons()) {
            return;
        }
        if (ZoneType.Battlefield.equals(card.getZone())) {
            for (FSkinProp prop : FSkinProp.iconsFromCardState(card.getCurrentState())) {
                FSkin.drawImage(g, FSkin.getImage(prop), abiX, abiY, abiScale, abiScale);
                abiY += abiSpace;
            }
        } else {
            hasFlash = computeHasFlash();
            if (hasFlash) {
                FSkin.drawImage(g, FSkin.getImage(FSkinProp.IMG_ABILITY_FLASH), cardXOffset + (cardWidth / 2) + (cardWidth / 3), cardWidth < 200 ? cardYOffset + 25 : cardYOffset + 50, cardWidth / 7, cardWidth / 7);
            }
        }
        if (ghost) {
            drawGhostOverlay(g);
        }
    }

    /** Whether a card off the battlefield can be cast at instant speed (drives the flash highlight). */
    private boolean computeHasFlash() {
        if (ZoneType.Battlefield.equals(card.getZone())) {
            return false;
        }
        final String abilityText = card.getCurrentState().getAbilityText();
        if (card.getCurrentState().hasKeyword(Keyword.FLASH)
                || ((abilityText.contains("May be played by"))
                        && (abilityText.contains("and as though it has flash")))) {
            return !card.isFaceDown() && ((!ZoneType.Library.equals(card.getZone()) && !ZoneType.Hand.equals(card.getZone())) || matchUI.mayView(card));
        }
        return false;
    }

    private void drawGhostOverlay(final Graphics g) {
        final Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        final int cornerSize = Math.max(4, Math.round(cardWidth * CardPanel.ROUNDED_CORNER_SIZE));
        g2d.setColor(GHOST_TINT);
        g2d.fillRoundRect(cardXOffset, cardYOffset, cardWidth, cardHeight, cornerSize, cornerSize);
    }

    /**
     * Arena hand: how far this card leans in the fan, and the part of it its neighbour leaves
     * uncovered, which its cost lines up with. Call before setCardBounds, which makes room for both.
     */
    public final void setArenaFan(final double angle, final int costWidth) {
        arenaFanAngle = angle;
        arenaCostWidth = costWidth;
    }

    /** Arena board: this card is tucked behind another in its pile. */
    public final void setArenaStackBack(final boolean back) {
        arenaStackBack = back;
    }

    public final double getArenaFanAngle() {
        return arenaFanAngle;
    }

    /**
     * Arena hand: bright mana pips sitting on the card's top edge, readable while the card is
     * sunk into the hand row. A card castable from another zone wears that zone's icon on the left.
     */
    private void drawArenaCostRow(final Graphics2D g) {
        final Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // a fine light rim keeps black borders apart where the cards overlap
            final int rimArc = Math.max(8, Math.round(cardWidth * 0.06f));
            g2.setColor(new Color(255, 255, 255, 70));
            g2.drawRoundRect(cardXOffset, cardYOffset, cardWidth - 1, cardHeight - 1, rimArc, rimArc);
            // full-art basics have no title where the hand shows them
            if (matchUI.mayView(card) && card.getCurrentState().getType() != null && card.getCurrentState().getType().isBasicLand()) {
                final int stripH = Math.round(cardHeight * 0.075f);
                final int sx = cardXOffset + Math.round(cardWidth * 0.04f), sy = cardYOffset + Math.round(cardHeight * 0.022f);
                final int sw = cardWidth - 2 * Math.round(cardWidth * 0.04f);
                g2.setColor(new Color(6, 7, 11, 225));
                g2.fillRoundRect(sx, sy, sw, stripH, stripH / 2, stripH / 2);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setFont(ArenaTheme.displayFont(Font.BOLD, stripH * 0.64f));
                final FontMetrics stripFm = g2.getFontMetrics();
                g2.setColor(Color.WHITE);
                g2.drawString(CardTranslation.getTranslatedName(card.getCurrentState().getName()), sx + stripH / 2, sy + (stripH + stripFm.getAscent() - stripFm.getDescent()) / 2);
            }
            final int size = Math.max(16, Math.min(32, Math.round(cardWidth * 0.14f)));
            final int pipY = cardYOffset - Math.round(size * 0.45f);
            final ManaCost cost = card.getCurrentState().getManaCost();
            if (cost != null && !cost.isNoCost() && cost.getGlyphCount() > 0) {
                final int w = cost.getGlyphCount() * (size + 1);
                final int px = cardXOffset + Math.max(2, Math.min(cardWidth, arenaCostWidth) - w - Math.round(cardWidth * 0.03f));
                g2.drawImage(ArenaTheme.brightPips(cost, size), px - 2, pipY - 2, null);
            }
            final FSkinProp icon = arenaZoneIcon(card.getZone());
            if (icon != null) {
                final int d = size + 8;
                final int zx = cardXOffset - d * 2 / 3, zy = cardYOffset + d / 2;
                g2.setColor(new Color(6, 6, 8, 240));
                g2.fillOval(zx, zy, d, d);
                g2.setColor(ArenaTheme.GOLD);
                g2.drawOval(zx, zy, d, d);
                FSkin.drawImage(g2, FSkin.getImage(icon), zx + 4, zy + 4, d - 8, d - 8);
            }
        } finally {
            g2.dispose();
        }
    }

    private static FSkinProp arenaZoneIcon(final ZoneType zone) {
        if (zone == null) {
            return null;
        }
        return switch (zone) {
            case Graveyard -> FSkinProp.IMG_ZONE_GRAVEYARD;
            case Exile -> FSkinProp.IMG_ZONE_EXILE;
            case Library -> FSkinProp.IMG_ZONE_LIBRARY;
            case Command -> FSkinProp.IMG_ZONE_COMMAND;
            case Flashback -> FSkinProp.IMG_ZONE_FLASHBACK;
            default -> null;
        };
    }

    private void drawZoneBanner(final Graphics g) {
        final Graphics2D g2d = (Graphics2D) g;
        final Object oldAA = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        final Font bannerFont = smallCounterFont != null ? smallCounterFont : g.getFont();
        g2d.setFont(bannerFont);
        final FontMetrics fm = g2d.getFontMetrics();
        final int textWidth = fm.stringWidth(zoneBannerText);
        final int padding = 6;
        final int bannerWidth = textWidth + padding * 2;
        final int bannerHeight = fm.getHeight() + 2;
        final int bannerX = cardXOffset + (cardWidth - bannerWidth) / 2;
        final int bannerY = Math.max(0, cardYOffset - bannerHeight - 1);

        g2d.setColor(zoneBannerColor != null ? zoneBannerColor : DEFAULT_ZONE_COLOR);
        g2d.fillRoundRect(bannerX, bannerY, bannerWidth, bannerHeight, 6, 6);

        g2d.setColor(Color.WHITE);
        final int textX = bannerX + padding;
        final int textY = bannerY + (bannerHeight - fm.getHeight()) / 2 + fm.getAscent();
        g2d.drawString(zoneBannerText, textX, textY);

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA != null ? oldAA : RenderingHints.VALUE_ANTIALIAS_DEFAULT);
    }

    private void drawCounterTabs(final Graphics g) {
        final Dimension imgSize = calculateImageSize();
        final int titleY = Math.round(imgSize.height * (54f / 640)) - 15;

        final int spaceFromTopOfCard = titleY + 60;
        final int counterBoxHeight = 24;
        final int counterBoxBaseWidth = 58;
        final int counterBoxSpacing = 2;

        int currentCounter = 0;

        FontMetrics smallFontMetrics = g.getFontMetrics(smallCounterFont);
        FontMetrics largeFontMetrics = g.getFontMetrics(largeCounterFont);

        if (CounterDisplayType.from(FModel.getPreferences().getPref(FPref.UI_CARD_COUNTER_DISPLAY_TYPE)) == CounterDisplayType.OLD_WHEN_SMALL) {
            int maxCounters = card.getCounters().entrySet().stream().mapToInt(Multiset.Entry::getCount).max().orElse(0);

            if (counterBoxBaseWidth + largeFontMetrics.stringWidth(String.valueOf(maxCounters)) > cardWidth) {
                drawCounterImage(g);
                return;
            }

        }

        for (Multiset.Entry<CounterType> counterEntry : new HashSet<>(card.getCounters().entrySet())) {
            final CounterType counter = counterEntry.getElement();
            final int numberOfCounters = counterEntry.getCount();
            final int counterBoxRealWidth = counterBoxBaseWidth + largeFontMetrics.stringWidth(String.valueOf(numberOfCounters));

            final int counterYOffset;

            if (ForgeConstants.CounterDisplayLocation.from(FModel.getPreferences().getPref(FPref.UI_CARD_COUNTER_DISPLAY_LOCATION)) == ForgeConstants.CounterDisplayLocation.TOP) {
                counterYOffset = cardYOffset + spaceFromTopOfCard - counterBoxHeight + currentCounter++ * (counterBoxHeight + counterBoxSpacing);
            } else {
                counterYOffset = cardYOffset + cardHeight - spaceFromTopOfCard / 2 - counterBoxHeight + currentCounter++ * (counterBoxHeight + counterBoxSpacing);
            }

            if (isSelected) {
                g.setColor(new Color(0, 0, 0, 255));
            } else {
                g.setColor(new Color(0, 0, 0, 200));
            }

            RoundRectangle2D counterArea = new RoundRectangle2D.Float(cardXOffset, counterYOffset, counterBoxRealWidth, counterBoxHeight, 9, 9);
            ((Graphics2D) g).fill(counterArea);

            g.fillRect(cardXOffset, counterYOffset, 9, counterBoxHeight);

            if (isSelected) {
                g.setColor(new Color(counter.getRed(), counter.getGreen(), counter.getBlue()));
            } else {
                g.setColor(new Color(counter.getRed(), counter.getGreen(), counter.getBlue(), 180));
            }

            Rectangle nameBounds = counterArea.getBounds();
            nameBounds.x += 8;
            nameBounds.y -= 1;
            nameBounds.width = 43;
            drawVerticallyCenteredString(g, counter.getCounterOnCardDisplayName(), nameBounds, smallCounterFont, smallFontMetrics);

            Rectangle numberBounds = counterArea.getBounds();
            numberBounds.x += 52;
            drawVerticallyCenteredString(g, String.valueOf(numberOfCounters), numberBounds, largeCounterFont, largeFontMetrics);

        }
    }

    private void drawCounterImage(final Graphics g) {
        int counters = card.getCounters().size();

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
    }

    private void drawMarkersTabs(final Graphics g, List<String> markers) {
        final Dimension imgSize = calculateImageSize();
        final int titleY = Math.round(imgSize.height * (54f / 640)) - 15;

        final int spaceFromTopOfCard = titleY + 60;
        final int markerBoxHeight = 24;
        final int markerBoxBaseWidth = 14;
        final int markerBoxSpacing = 2;

        int currentMarker = 0;

        FontMetrics smallFontMetrics = g.getFontMetrics(smallCounterFont);

        for (String marker : markers) {
            final int markerBoxRealWidth = markerBoxBaseWidth + smallFontMetrics.stringWidth(marker);
            final int markerYOffset;

            if (ForgeConstants.CounterDisplayLocation.from(FModel.getPreferences().getPref(FPref.UI_CARD_COUNTER_DISPLAY_LOCATION)) == ForgeConstants.CounterDisplayLocation.TOP) {
                markerYOffset = cardYOffset + spaceFromTopOfCard - markerBoxHeight + currentMarker++ * (markerBoxHeight + markerBoxSpacing);
            } else {
                markerYOffset = cardYOffset + cardHeight - spaceFromTopOfCard / 2 - markerBoxHeight + currentMarker++ * (markerBoxHeight + markerBoxSpacing);
            }

            if (isSelected) {
                g.setColor(new Color(0, 0, 0, 255));
            } else {
                g.setColor(new Color(0, 0, 0, 200));
            }

            RoundRectangle2D markerArea = new RoundRectangle2D.Float(cardXOffset, markerYOffset, markerBoxRealWidth, markerBoxHeight, 9, 9);
            ((Graphics2D) g).fill(markerArea);

            g.fillRect(cardXOffset, markerYOffset, 9, markerBoxHeight);

            if (isSelected) {
                g.setColor(new Color(200, 200, 200));
            } else {
                g.setColor(new Color(200, 200, 200, 180));
            }

            Rectangle nameBounds = markerArea.getBounds();
            nameBounds.x += 8;
            nameBounds.y -= 1;
            nameBounds.width = 43;
            drawVerticallyCenteredString(g, marker, nameBounds, smallCounterFont, smallFontMetrics);
        }
    }

    /**
     * Draws a String justified to the left of the rectangle, centered vertically.
     *
     * @param g The Graphics instance.
     * @param text The String to draw.
     * @param area The Rectangle to center the text in.
     * @param font The font to use to draw the text.
     */
    private void drawVerticallyCenteredString(Graphics g, String text, Rectangle area, Font font, final FontMetrics fontMetrics) {
        Font oldFont = g.getFont();

        int x = area.x;
        int y = area.y + (area.height - fontMetrics.getHeight()) / 2 + fontMetrics.getAscent() + 1;

        g.setFont(font);
        g.drawString(text, x, y);
        g.setFont(oldFont);
    }

    @Override
    public final String toString() {
        return getCard().toString();
    }

    static final int ZONE_BANNER_HEIGHT = 16;

    public final void setCardBounds(final int x, final int y, int width, int height) {
        cardWidth = width;
        cardHeight = height;
        if (isArenaTile() || (arenaCostWidth > 0 && ArenaTheme.isEnabled())) {
            // room around the card for its glow, its tilt or fan lean, and the cost above it
            final boolean tile = isArenaTile();
            final int mx = Math.round(width * (tile ? 0.2f : 0.5f));
            final int top = Math.round(tile ? height * 0.28f : width * 0.25f);
            final int bottom = Math.round(tile ? height * 0.28f : width * 0.14f);
            cardXOffset = mx;
            cardYOffset = top;
            setBounds(x - mx, y - top, width + 2 * mx, height + top + bottom);
            return;
        }
        final int rotCenterX = Math.round(width / 2f);
        final int rotCenterY = height - rotCenterX;
        final int rotCenterToTopCorner = Math.round(width * CardPanel.ROT_CENTER_TO_TOP_CORNER);
        final int rotCenterToBottomCorner = Math.round(width * CardPanel.ROT_CENTER_TO_BOTTOM_CORNER);
        final int xOffset = rotCenterX - rotCenterToBottomCorner;
        final int yOffset = rotCenterY - rotCenterToTopCorner;
        cardXOffset = -xOffset;
        cardYOffset = -yOffset;

        // Add extra space above zone-bannered cards so the banner is not clipped
        final int bannerSpace = zoneBannerText != null ? ZONE_BANNER_HEIGHT : 0;
        cardYOffset += bannerSpace;

        width = -xOffset + rotCenterX + rotCenterToTopCorner;
        height = -yOffset + rotCenterY + rotCenterToBottomCorner + bannerSpace;
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

    public String getZoneBannerText() {
        return zoneBannerText;
    }

    public void setZoneBanner(final String text, final ZoneType zone) {
        this.zoneBannerText = text;
        this.zoneBannerColor = zone != null ? ZONE_COLORS.getOrDefault(zone, DEFAULT_ZONE_COLOR) : null;
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
        titleText.setText(CardTranslation.getTranslatedName(card.getCurrentState().getName()));
        // Screen readers can't tell if a card is tapped.
        if (isPreferenceEnabled(FPref.UI_SCREENREADER_OPTIMIZE)) {
                if (this.isTapped()) {
                    titleText.getAccessibleContext().setAccessibleDescription("tapped");
                } else {
                    titleText.getAccessibleContext().setAccessibleDescription("");
                }
            }
        final int damage = card.getDamage();
        damageText.setText(damage > 0 ? "\u00BB " + damage + " \u00AB" : "");

        // Card Id overlay
        cardIdText.setText(card.getCurrentState().getDisplayId());
    }

    public final void updatePTOverlay() {
        // P/T overlay
        final CardStateView state = card.getCurrentState();
        String sPt = "";
        if (state.isCreature() && state.isPlaneswalker()) {
            sPt = state.getPower() + "/" + state.getToughness() +
                    " (" + state.getLoyalty() + ")";
        }
        else if (state.isCreature()) {
            sPt = state.getPower() + "/" + state.getToughness();
        }
        else if (state.isVehicle()) {
            sPt = "[" + state.getPower() + "/" + state.getToughness() + "]";
        }
        else if (state.isPlaneswalker()) {
            sPt = state.getLoyalty();
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

    public final boolean isGhost() {
        return ghost;
    }
    public final void setGhost(final boolean ghost0) {
        ghost = ghost0;
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

    // don't show overlays on non-selectable cards when selecting
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
        return isShowingOverlays() && isPreferenceEnabled(FPref.UI_OVERLAY_CARD_MANA_COST);
    }

    private boolean showPerpetualManaCost() {
        return isPreferenceEnabled(FPref.UI_OVERLAY_CARD_PERPETUAL_MANA_COST);
    }

    private boolean showCardIdOverlay() {
        return isShowingOverlays() && isPreferenceEnabled(FPref.UI_OVERLAY_CARD_ID);
    }

    private boolean showAbilityIcons() {
        return isShowingOverlays() && isPreferenceEnabled(FPref.UI_OVERLAY_ABILITY_ICONS);
    }

    public void repaintOverlays() {
        repaint();
        doLayout();
    }
}
