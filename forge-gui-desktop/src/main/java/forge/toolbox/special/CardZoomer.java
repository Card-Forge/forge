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

import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Timer;

import forge.StaticData;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.game.keyword.Keyword;
import forge.gui.SOverlayUtils;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.localinstance.skin.FSkinProp;
import forge.model.FModel;
import forge.toolbox.FOverlay;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedLabel;
import forge.toolbox.imaging.FImagePanel;
import forge.toolbox.imaging.FImagePanel.AutoSizeImageMode;
import forge.toolbox.imaging.FImageUtil;
import forge.view.arcane.CardInfoPopup;
import forge.view.arcane.CardInfoPopup.KeywordData;
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

        final boolean showKeywords = FModel.getPreferences().getPrefBoolean(FPref.UI_ZOOM_KEYWORD_INFO);
        final boolean showRelated = FModel.getPreferences().getPrefBoolean(FPref.UI_ZOOM_RELATED_CARDS);

        pnlMain.removeAll();
        if ((showKeywords || showRelated) && thisCard != null) {
            final JPanel infoPanel = buildInfoPanel(showKeywords, showRelated);
            if (infoPanel != null) {
                // Switch to horizontal layout (no wrap) for side-by-side display
                pnlMain.setLayout(new MigLayout("insets 0, ax center, ay center"));
                pnlMain.add(imagePanel, "w 45%!, h 80%!");
                final JScrollPane scroll = new JScrollPane(infoPanel);
                scroll.setOpaque(false);
                scroll.getViewport().setOpaque(false);
                scroll.setBorder(null);
                scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                pnlMain.add(scroll, "w 40%!, h 80%!");
            } else {
                pnlMain.add(imagePanel, "w 80%!, h 80%!");
            }
        } else {
            pnlMain.add(imagePanel, "w 80%!, h 80%!");
        }
        pnlMain.validate();
        setFlipIndicator();
    }

    private JPanel buildInfoPanel(final boolean showKeywords, final boolean showRelated) {
        final JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // Derive dimensions from the 40% scroll pane column.
        // Account for scrollbar (~17px), content padding (16px), and border slack.
        final int screenWidth = overlay.getWidth() > 0
                ? overlay.getWidth()
                : java.awt.Toolkit.getDefaultToolkit().getScreenSize().width;
        final int maxWidth = Math.max(200, (int) (screenWidth * 0.40) - 60);
        // Cap thumbnail height so 2 cards fit side-by-side in the column
        final int rawSize = FModel.getPreferences().getPrefInt(FPref.UI_POPUP_IMAGE_SIZE);
        final int popupThumbHeight = Math.max(100, Math.min(500, rawSize));
        final int maxThumbForColumn = (int) ((maxWidth / 2.0) / 0.716);
        final int thumbnailHeight = Math.min(popupThumbHeight, maxThumbForColumn);
        boolean hasContent = false;

        if (showKeywords) {
            final String keywordKey = thisCard.getKeywordKey();
            final String oracleText = thisCard.getOracleText();
            final String cardName = thisCard.getName();
            if ((keywordKey != null && !keywordKey.isEmpty())
                    || (oracleText != null && !oracleText.isEmpty())) {
                final List<KeywordData> keywords = new ArrayList<>();
                final Set<String> addedNames = new LinkedHashSet<>();
                if (keywordKey != null && !keywordKey.isEmpty()) {
                    keywords.addAll(CardInfoPopup.buildKeywords(keywordKey, addedNames));
                }
                if (oracleText != null && !oracleText.isEmpty()) {
                    CardInfoPopup.addKeywordActions(keywords, oracleText, addedNames,
                            cardName != null ? cardName : "");
                }
                if (!keywords.isEmpty()) {
                    final JPanel kwPanel = new JPanel();
                    kwPanel.setLayout(new BoxLayout(kwPanel, BoxLayout.Y_AXIS));
                    kwPanel.setOpaque(false);
                    CardInfoPopup.populateKeywords(kwPanel, keywords, maxWidth);
                    content.add(kwPanel);
                    hasContent = true;
                }
            }
        }

        if (showRelated) {
            final String cardName = thisCard.getName();
            final CardView cardView = thisCard.getCard();
            if (cardName != null && !cardName.isEmpty() && cardView != null) {
                final List<RelatedCardEntry> entries =
                        CardInfoPopup.buildRelatedCardsStatic(cardName, cardView);
                if (!entries.isEmpty()) {
                    if (hasContent) {
                        content.add(javax.swing.Box.createRigidArea(new Dimension(0, 8)));
                    }
                    final JPanel relPanel = new JPanel();
                    relPanel.setLayout(new BoxLayout(relPanel, BoxLayout.Y_AXIS));
                    relPanel.setOpaque(false);
                    CardInfoPopup.populateRelatedCards(relPanel, entries,
                            thumbnailHeight, maxWidth);
                    content.add(relPanel);
                    hasContent = true;
                }
            }
        }

        if (!hasContent) {
            return null;
        }

        // Wrap in BorderLayout.NORTH so content top-aligns instead of stretching
        final JPanel wrapper = new JPanel(new java.awt.BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(content, java.awt.BorderLayout.NORTH);
        return wrapper;
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
