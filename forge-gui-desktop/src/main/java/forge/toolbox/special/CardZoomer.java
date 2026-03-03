/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2013  Forge Team
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

package forge.toolbox.special;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;

import forge.CachedCardImage;
import forge.StaticData;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.game.keyword.Keyword;
import forge.gui.SOverlayUtils;
import forge.gui.card.KeywordInfoUtil;
import forge.gui.card.KeywordInfoUtil.KeywordData;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.localinstance.skin.FSkinProp;
import forge.model.FModel;
import forge.toolbox.FOverlay;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedLabel;
import forge.toolbox.imaging.FImagePanel;
import forge.toolbox.imaging.FImagePanel.AutoSizeImageMode;
import forge.toolbox.imaging.FImageUtil;
import forge.view.arcane.CardInfoPopup;
import forge.view.arcane.CardInfoPopup.RelatedCardEntry;
import net.miginfocom.swing.MigLayout;

/**
 * Displays card image at its original size and correct orientation.
 * <p>
 * Supports split, flip and double-sided cards as well as cards that
 * can be played face-down (eg. morph).
 *
 * @version $Id: CardZoomer.java 24769 2014-02-09 13:56:04Z Hellfish $
 *
 */
public enum CardZoomer {
    SINGLETON_INSTANCE;

    // Gui controls
    private final JPanel overlay = FOverlay.SINGLETON_INSTANCE.getPanel();
    private JPanel pnlMain;
    private FImagePanel imagePanel;
    private final SkinnedLabel lblFlipcard = new SkinnedLabel();

    // Details about the current card being displayed.
    private CardStateView thisCard;
    private boolean mayFlip, isInAltState;
    private boolean isSplitRotated = false;

    // The zoomer is in button mode when it is activated by holding down the
    // middle mouse button or left and right mouse buttons simultaneously.
    private boolean isButtonMode = false;
    private boolean isOpen = false;
    private long lastClosedTime;

    // Used to ignore mouse wheel rotation for a short period of time.
    private Timer mouseWheelCoolDownTimer;
    private boolean isMouseWheelEnabled = false;

    // ctr
    CardZoomer() {
        lblFlipcard.setIcon(FSkin.getIcon(FSkinProp.ICO_FLIPCARD));
        setMouseButtonListener();
        setMouseWheelListener();
        setKeyListeners();
    }

    public void setCard(final CardStateView card, final boolean mayFlip) {
        this.thisCard = card;
        this.mayFlip = mayFlip;
        this.isInAltState = card != null && card == card.getCard().getAlternateState();
    }

    /**
     * Creates listener for keys that are recognised by zoomer.
     * <p><ul>
     * <li>ESC will close zoomer in mouse-wheel mode only.
     * <li>CTRL will flip or transform card in either mode if applicable.
     */
    private void setKeyListeners() {
        overlay.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(final KeyEvent e) {
                if (!isButtonMode) {
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        closeZoomer();
                    }
                }
                if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
                    toggleCardImage();
                }
            }
        });
    }

    /**
     * Creates listener for mouse button events.
     * <p>
     * NOTE: Needed even if ButtonMode to prevent Zoom getting stuck open on
     * certain systems.
     */
    private void setMouseButtonListener() {
        overlay.addMouseListener(new MouseAdapter() {
            @Override public void mouseReleased(final MouseEvent e) {
                closeZoomer();
            }
        });
    }

    /**
     * Creates listener for mouse wheel events.
     * <p>
     * If the zoomer is opened using the mouse wheel then additional actions can
     * be performed dependent on the card type -
     * <p>
     * <ul>
     * <li>If mouse wheel is rotated back then close zoomer.
     * <li>If mouse wheel is rotated forward and...
     * <ul>
     * <li>if image is a flip card then rotate 180 degrees.
     * <li>if image is a double-sided card then show other side.
     */
    private void setMouseWheelListener() {
        overlay.addMouseWheelListener(e -> {
            if (!isButtonMode) {
                if (isMouseWheelEnabled) {
                    isMouseWheelEnabled = false;
                    if (e.getWheelRotation() > 0) {
                        closeZoomer();
                    } else {
                        toggleCardImage();
                        startMouseWheelCoolDownTimer(250);
                    }
                }
            }
        });
    }

    /**
     * Opens zoomer in mouse wheel mode and displays the image associated with
     * the given card based.
     * <p>
     * This method should be called if the zoomer is activated by rotating the
     * mouse wheel.
     */
    public void doMouseWheelZoom() {
        isButtonMode = false;
        displayCard();
        startMouseWheelCoolDownTimer(200);
    }

    /**
     * Opens zoomer in mouse button mode and displays the image associated with
     * the given card.
     * <p>
     * This method should be called if the zoomer is activated by holding down
     * the middle mouse button or left and right mouse buttons simultaneously.
     */
    public void doMouseButtonZoom() {
        // don't display zoom if already zoomed or just closed zoom
        // (handles mouse wheeling while middle clicking)
        if (isOpen || System.currentTimeMillis() - lastClosedTime < 250) {
            return;
        }

        isButtonMode = true;
        displayCard();
    }

    public boolean isZoomerOpen() {
        return isOpen;
    }

    private void displayCard() {
        if (thisCard == null) {
            return;
        }

        isMouseWheelEnabled = false;
        setLayout();
        setImage();
        SOverlayUtils.showOverlay();
        isOpen = true;
    }

    /**
     * Displays a graphical indicator that shows whether the current card can be flipped or transformed.
     */
    private void setFlipIndicator() {
        if (thisCard != null && mayFlip) {
            imagePanel.setLayout(new MigLayout("insets 0, w 100%!, h 100%!"));
            imagePanel.add(lblFlipcard, "pos (100% - 100px) 0");
        }
    }

    /**
     * Needs to be called whenever the source image changes.
     */
    private void setImage() {
        imagePanel = new FImagePanel();

        final BufferedImage xlhqImage = FImageUtil.getImageXlhq(thisCard);
        imagePanel.setImage(xlhqImage == null ? FImageUtil.getImage(thisCard) : xlhqImage, getInitialRotation(), AutoSizeImageMode.SOURCE);

        final boolean zoomEnabled = FModel.getPreferences().getPrefBoolean(FPref.UI_SHOW_ZOOM_TOOLTIPS);
        final boolean showKeywords = zoomEnabled
                && FModel.getPreferences().getPrefBoolean(FPref.UI_ZOOM_KEYWORD_INFO);
        final boolean showRelated = zoomEnabled
                && FModel.getPreferences().getPrefBoolean(FPref.UI_ZOOM_RELATED_CARDS);

        pnlMain.removeAll();
        final boolean isFaceDown = thisCard != null && thisCard.getCard() != null
                && thisCard.getCard().isFaceDown();
        if ((showKeywords || showRelated) && thisCard != null && !isFaceDown) {
            final JPanel sidePanel = buildSidePanel(showKeywords, showRelated);
            if (sidePanel != null) {
                pnlMain.setLayout(new MigLayout("insets 0, ax center, ay center"));
                pnlMain.add(imagePanel, "w 45%!, h 80%!");
                pnlMain.add(sidePanel, sidePanel.getClientProperty("widthConstraint") + ", h 80%!");
            } else {
                pnlMain.add(imagePanel, "w 80%!, h 80%!");
            }
        } else {
            pnlMain.add(imagePanel, "w 80%!, h 80%!");
        }
        pnlMain.validate();
        setFlipIndicator();
    }

    /**
     * Build the side panel for the zoom view. Keywords go in a fixed header,
     * related cards go in a scrollable area below.
     */
    private JPanel buildSidePanel(final boolean showKeywords, final boolean showRelated) {
        // Check for related card entries first to determine column width
        List<RelatedCardEntry> relatedEntries = List.of();
        if (showRelated) {
            final String cardName = thisCard.getName();
            final CardView cardView = thisCard.getCard();
            if (cardName != null && !cardName.isEmpty() && cardView != null) {
                relatedEntries = CardInfoPopup.buildRelatedCardsStatic(cardName, cardView);
                for (final RelatedCardEntry entry : relatedEntries) {
                    if ((entry.image == null || entry.placeholder)
                            && entry.imageKey != null && !entry.imageKey.isEmpty()) {
                        CachedCardImage.fetcher.fetchImage(entry.imageKey, () -> { });
                    }
                }
            }
        }
        final boolean hasRelated = !relatedEntries.isEmpty();

        final int screenWidth = overlay.getWidth() > 0
                ? overlay.getWidth()
                : java.awt.Toolkit.getDefaultToolkit().getScreenSize().width;
        // Padding: scrollbar (~12px) + left content padding (8px) + slack (2px)
        final int sidePadding = 22;

        // Compute column width based on actual content
        int columnPx;
        int maxWidth;
        int thumbnailHeight = 0;

        if (hasRelated) {
            final int maxColumnPx = Math.max(200, (int) (screenWidth * 0.40));
            final int rawSize = FModel.getPreferences().getPrefInt(FPref.UI_POPUP_IMAGE_SIZE);
            final int popupThumbHeight = Math.max(100, Math.min(500, rawSize));

            // Use user's preferred size; populateRelatedCards constrains
            // to fit the column width when needed
            thumbnailHeight = popupThumbHeight;

            int naturalContentWidth = 0;
            final Map<String, List<RelatedCardEntry>> grouped = new LinkedHashMap<>();
            for (final RelatedCardEntry entry : relatedEntries) {
                grouped.computeIfAbsent(entry.label, k -> new ArrayList<>()).add(entry);
            }
            for (final List<RelatedCardEntry> cards : grouped.values()) {
                final int perRow = Math.min(cards.size(), 2);
                final int thumbWidth = (int) (thumbnailHeight * 0.716);
                final int groupWidth = perRow * thumbWidth + 18;
                naturalContentWidth = Math.max(naturalContentWidth, groupWidth);
            }
            columnPx = Math.max(200,
                    Math.min(naturalContentWidth + sidePadding, maxColumnPx));
            maxWidth = columnPx - sidePadding;
        } else {
            columnPx = Math.max(200, (int) (screenWidth * 0.25));
            maxWidth = columnPx - sidePadding;
        }

        // Build keyword panel
        JPanel kwPanel = null;
        if (showKeywords) {
            final String keywordKey = thisCard.getKeywordKey();
            final String oracleText = thisCard.getOracleText();
            final String cardName = thisCard.getName();
            final List<KeywordData> keywords = new ArrayList<>();
            final Set<String> addedNames = new LinkedHashSet<>();
            if (keywordKey != null && !keywordKey.isEmpty()) {
                keywords.addAll(KeywordInfoUtil.buildKeywords(keywordKey, addedNames));
            }
            KeywordInfoUtil.addMissingKeywordsFromFlags(keywords, thisCard, addedNames);
            if (oracleText != null && !oracleText.isEmpty()) {
                KeywordInfoUtil.addKeywordActions(keywords, oracleText, addedNames,
                        cardName != null ? cardName : "");
            }
            KeywordInfoUtil.sortByOracleText(keywords, oracleText);
            KeywordInfoUtil.annotateKeywordCounts(keywords, thisCard.getCard());
            KeywordInfoUtil.addGraveyardCounts(keywords, thisCard.getCard());
            if (!keywords.isEmpty()) {
                kwPanel = new JPanel();
                kwPanel.setLayout(new BoxLayout(kwPanel, BoxLayout.Y_AXIS));
                kwPanel.setOpaque(false);
                kwPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 0));
                // Cap keyword width to 1-card pill width when related cards shown
                final int kwWidth;
                if (hasRelated) {
                    final int thumbW = (int) (thumbnailHeight * 0.716);
                    kwWidth = Math.min(thumbW + 18, maxWidth);
                } else {
                    kwWidth = maxWidth;
                }
                CardInfoPopup.populateKeywords(kwPanel, keywords, kwWidth);
            }
        }

        if (kwPanel == null && relatedEntries.isEmpty()) {
            return null;
        }

        // Assemble: keywords and related cards in a single scrollable panel
        final JPanel side = new JPanel(new java.awt.BorderLayout());
        side.setOpaque(false);
        side.putClientProperty("widthConstraint", "w " + columnPx + "!");

        // Build a single content panel with keywords on top and related cards below
        final JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        if (kwPanel != null) {
            content.add(kwPanel);
        }

        if (!relatedEntries.isEmpty()) {
            final JPanel relPanel = new JPanel();
            relPanel.setLayout(new BoxLayout(relPanel, BoxLayout.Y_AXIS));
            relPanel.setOpaque(false);
            relPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 0));
            CardInfoPopup.populateRelatedCards(relPanel, relatedEntries,
                    thumbnailHeight, maxWidth, 2, true, null);
            content.add(relPanel);
        }

        // Wrap entire content in a scroll pane so everything scrolls together
        final JPanel wrapper = new JPanel(new java.awt.BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(content, java.awt.BorderLayout.NORTH);
        final FScrollPane scroll = new FScrollPane(wrapper, false,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        side.add(scroll, java.awt.BorderLayout.CENTER);

        // Click anywhere on the side panel dismisses the zoomer
        // Use mouseClicked (not mouseReleased) so scroll wheel release doesn't dismiss
        final MouseAdapter dismissClick = new MouseAdapter() {
            @Override public void mouseClicked(final MouseEvent e) {
                closeZoomer();
            }
        };
        addMouseListenerRecursive(side, dismissClick);

        // Increase scroll speed for comfortable browsing
        scroll.getVerticalScrollBar().setUnitIncrement(48);

        // Replace scroll pane's default wheel behavior:
        // scroll-down dismisses unless scrollbar can still scroll further
        for (final java.awt.event.MouseWheelListener l : scroll.getMouseWheelListeners()) {
            scroll.removeMouseWheelListener(l);
        }
        scroll.addMouseWheelListener(e -> {
            if (isButtonMode || !isMouseWheelEnabled) {
                e.consume();
                return;
            }
            isMouseWheelEnabled = false;
            if (e.getWheelRotation() > 0) {
                final javax.swing.JScrollBar vbar = scroll.getVerticalScrollBar();
                if (vbar.isVisible()
                        && vbar.getValue() + vbar.getVisibleAmount() < vbar.getMaximum()) {
                    vbar.setValue(vbar.getValue()
                            + e.getUnitsToScroll() * vbar.getUnitIncrement(1));
                    startMouseWheelCoolDownTimer(250);
                } else {
                    closeZoomer();
                }
            } else {
                final javax.swing.JScrollBar vbar = scroll.getVerticalScrollBar();
                if (vbar.isVisible() && vbar.getValue() > 0) {
                    vbar.setValue(vbar.getValue()
                            + e.getUnitsToScroll() * vbar.getUnitIncrement(1));
                    startMouseWheelCoolDownTimer(250);
                } else {
                    toggleCardImage();
                    startMouseWheelCoolDownTimer(250);
                }
            }
            e.consume();
        });

        return side;
    }

    private static void addMouseListenerRecursive(final java.awt.Component comp,
                                                   final MouseAdapter listener) {
        comp.addMouseListener(listener);
        if (comp instanceof java.awt.Container) {
            for (final java.awt.Component child : ((java.awt.Container) comp).getComponents()) {
                addMouseListenerRecursive(child, listener);
            }
        }
    }

    private int getInitialRotation() {
        if (thisCard == null) {
            return 0;
        }
        if (thisCard.getCard().isSplitCard()) {
            String cardName = thisCard.getCard().getOracleName();
            if (cardName.isEmpty()) { cardName = thisCard.getCard().getAlternateState().getOracleName(); }
            
            PaperCard pc = StaticData.instance().getCommonCards().getCard(cardName);
            boolean isAftermath = pc != null && Card.getCardForUi(pc).hasKeyword(Keyword.AFTERMATH);

            return thisCard.getCard().isFaceDown() || isSplitRotated ? 0 : isAftermath ? 270 : 90; // rotate Aftermath splits the other way to correctly show the right split (graveyard) half
        }

        if (thisCard.getCard().isFlipped()) {
            return 180;
        }

        return thisCard.getType().isPlane() || thisCard.getType().isPhenomenon() ? 90 : 0;
    }

    private void setLayout() {
        overlay.removeAll();
        pnlMain = new JPanel();
        pnlMain.setOpaque(false);
        overlay.setLayout(new MigLayout("insets 0, w 100%!, h 100%!"));
        pnlMain.setLayout(new MigLayout("insets 0, wrap, ax center, ay center"));
        overlay.add(pnlMain, "w 100%!, h 100%!");
    }

    public void closeZoomer() {
        if (!isOpen) { return; }
        stopMouseWheelCoolDownTimer();
        isOpen = false;
        SOverlayUtils.hideOverlay();
        lastClosedTime = System.currentTimeMillis();
    }

    /** Rebuild the zoom view content (e.g. after toggling tooltip preferences). */
    public void refreshIfOpen() {
        if (isOpen) {
            setImage();
        }
    }

    /**
     * If the zoomer is ativated using the mouse wheel then ignore
     * wheel for a short period of time after opening. This will
     * prevent flip and double side cards from immediately flipping.
     */
    private void startMouseWheelCoolDownTimer(final int millisecsDelay) {
        isMouseWheelEnabled = false;
        createMouseWheelCoolDownTimer(millisecsDelay);
        mouseWheelCoolDownTimer.setInitialDelay(millisecsDelay);
        mouseWheelCoolDownTimer.restart();
    }

    /**
     * Used to ignore mouse wheel rotation for {@code millisecsDelay} milliseconds.
     */
    private void createMouseWheelCoolDownTimer(final int millisecsDelay) {
        if (mouseWheelCoolDownTimer == null) {
            mouseWheelCoolDownTimer = new Timer(millisecsDelay, e -> isMouseWheelEnabled = true);
        }
    }

    private void stopMouseWheelCoolDownTimer() {
        if (mouseWheelCoolDownTimer != null && mouseWheelCoolDownTimer.isRunning()) {
            mouseWheelCoolDownTimer.stop();
        }
    }

    /**
     * Toggles between primary and alternate image associated with card if applicable.
     */
    private void toggleCardImage() {
        if (thisCard != null) {
            if (mayFlip) {
                toggleFlipCard();
            } else if (thisCard.getCard().isSplitCard()) {
                toggleSplitCardRotation();
            }
        }
    }

    /**
     * Flips image by rotating 180 degrees each time.
     * <p>
     * No need to get the alternate card image from cache.
     * Can simply rotate current card image in situ to get same effect.
     */
    private void toggleFlipCard() {
        try { //prevent NPE trying to view card without alternate
            isInAltState = !isInAltState;
            thisCard = thisCard.getCard().getState(isInAltState);
            imagePanel.setRotation(thisCard.getCard().isFlipCard() && isInAltState ? 180 : 0);
            setImage();
        } catch (Exception e){}
    }

    /**
     * Controls the rotation of a split card.
     */
    private void toggleSplitCardRotation() {
        isSplitRotated = !isSplitRotated;
        setImage();
    }

}
