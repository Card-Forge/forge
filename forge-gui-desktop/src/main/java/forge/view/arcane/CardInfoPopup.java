package forge.view.arcane;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.Graphics2D;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.apache.commons.lang3.tuple.Pair;

import forge.CachedCardImage;
import forge.ImageCache;
import forge.ImageKeys;
import forge.StaticData;
import forge.card.CardRules;
import forge.card.CardSplitType;
import forge.card.CardStateName;
import forge.card.ICardFace;
import forge.card.mana.ManaCost;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.game.card.CounterType;
import forge.game.zone.ZoneType;
import forge.gui.card.KeywordInfoUtil;
import forge.gui.card.KeywordInfoUtil.KeywordData;
import forge.item.PaperCard;
import forge.item.PaperToken;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.toolbox.CardFaceSymbols;
import forge.toolbox.FSkin;
import forge.toolbox.imaging.FImagePanel;
import forge.toolbox.imaging.FImageUtil;
import forge.util.CardTranslation;

/**
 * A floating popup that displays keyword explanations and related card images
 * when hovering over a card during a match.
 */
public class CardInfoPopup {
    private static final int POPUP_WIDTH = 260;
    private static final int GAP = 4;
    private static final int SHOW_DELAY_MS = 100;
    private static final int PADDING = 8;
    private static final double MTG_ASPECT_RATIO = 0.716;

    // Dark overlay colors
    private static final Color BG_COLOR = new Color(30, 30, 30);
    private static final Color BORDER_COLOR = new Color(80, 80, 80);
    private static final Color PILL_BG = new Color(45, 45, 45);
    private static final Color PILL_BORDER = new Color(70, 70, 70);
    private static final Color TEXT_PRIMARY = Color.WHITE;
    private static final Color TEXT_SECONDARY = new Color(210, 210, 210);
    private static final int PILL_CORNER = 12;
    private static final int PILL_PAD = 8;

    // Ensures only one popup is visible across all CardPanelContainer instances
    private static CardInfoPopup activePopup;

    private final Window owner;
    private final JWindow window;
    private final JPanel mainPanel;
    private final JPanel contentPanel;
    private final FImagePanel cardImagePanel;
    private final JPanel keywordsPanel;
    private final JPanel relatedCardsPanel;
    private final Timer showTimer;

    // Cache
    private String cachedKeywordKey = "";
    private String cachedCardName = "";
    private boolean cachedHasKeywords = false;
    private boolean cachedHasRelated = false;
    private int cachedImageSize = -1;
    private boolean cachedShowCardImage = false;
    private boolean cachedShowOverlays = false;
    private String cachedOverlayState = "";
    private String cachedCardImageKey = "";
    private boolean cachedDisableImages = false;

    // Pending show state
    private Point pendingLocation;

    // Saved parameters for auto-download callback re-invocation
    private CardView lastCardView;
    private Point lastCardScreenLocation;
    private Dimension lastCardSize;
    private boolean lastShowKeywords;
    private boolean lastShowRelatedCards;
    private boolean lastShowCardImage;

    public CardInfoPopup(final Window owner) {
        this.owner = owner;
        window = new JWindow(owner);
        window.setFocusableWindowState(false);
        window.setAlwaysOnTop(true);
        window.setBackground(new Color(0, 0, 0, 0)); // fully transparent window

        // Card image panel (WEST side)
        cardImagePanel = new FImagePanel();
        cardImagePanel.setOpaque(false);
        cardImagePanel.setVisible(false);

        // Content panel (CENTER side — keywords on top, related cards below)
        contentPanel = new JPanel(new BorderLayout(0, GAP));
        contentPanel.setOpaque(false);

        keywordsPanel = new JPanel();
        keywordsPanel.setLayout(new BoxLayout(keywordsPanel, BoxLayout.Y_AXIS));
        keywordsPanel.setOpaque(false);

        relatedCardsPanel = new JPanel();
        relatedCardsPanel.setLayout(new BoxLayout(relatedCardsPanel, BoxLayout.Y_AXIS));
        relatedCardsPanel.setOpaque(false);

        // Keywords fixed at top, related cards fill remaining space below
        contentPanel.add(keywordsPanel, BorderLayout.NORTH);
        contentPanel.add(relatedCardsPanel, BorderLayout.CENTER);

        // Wrap contentPanel so it top-aligns instead of stretching to card image height
        final JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.setOpaque(false);
        contentWrapper.add(contentPanel, BorderLayout.NORTH);

        // Main panel wraps card image + content with spacing (transparent background)
        mainPanel = new JPanel(new BorderLayout(GAP, 0));
        mainPanel.setOpaque(false);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(
                PADDING, PADDING, PADDING, PADDING));
        mainPanel.add(cardImagePanel, BorderLayout.WEST);
        mainPanel.add(contentWrapper, BorderLayout.CENTER);

        window.getContentPane().setLayout(new BorderLayout());
        ((JPanel) window.getContentPane()).setOpaque(false);
        window.getContentPane().add(mainPanel, BorderLayout.CENTER);

        showTimer = new Timer(SHOW_DELAY_MS, e -> {
            if (pendingLocation != null && isOwnerFocused()) {
                if (activePopup != null && activePopup != this) {
                    activePopup.hidePopup();
                }
                activePopup = this;
                window.setLocation(pendingLocation);
                window.setVisible(true);
            }
        });
        showTimer.setRepeats(false);

        // Hide popup when owner window loses focus (e.g. ALT-TAB)
        if (owner != null) {
            owner.addWindowFocusListener(new WindowFocusListener() {
                @Override
                public void windowGainedFocus(final WindowEvent e) { }

                @Override
                public void windowLostFocus(final WindowEvent e) {
                    hidePopup();
                }
            });
        }
    }

    private static int getThumbnailHeight() {
        final int value = FModel.getPreferences().getPrefInt(FPref.UI_POPUP_IMAGE_SIZE);
        return Math.max(250, Math.min(500, value));
    }

    /**
     * Show the popup for the given card, displaying keyword explanations and/or
     * related card images based on the toggle flags.
     */
    public void showForCard(final CardView cardView, final Point cardScreenLocation,
                            final Dimension cardSize, final boolean showKeywords,
                            final boolean showRelatedCards, final boolean showCardImage) {
        if (cardView == null || cardView.isFaceDown()) {
            hidePopup();
            return;
        }

        final CardStateView state = cardView.getCurrentState();
        if (state == null) {
            hidePopup();
            return;
        }

        // Save parameters for auto-download callback
        lastCardView = cardView;
        lastCardScreenLocation = cardScreenLocation;
        lastCardSize = cardSize;
        lastShowKeywords = showKeywords;
        lastShowRelatedCards = showRelatedCards;
        lastShowCardImage = showCardImage;

        final int thumbnailHeight = getThumbnailHeight();
        final String keywordKey = showKeywords ? nullSafe(state.getKeywordKey()) : "";
        final String cardName = showRelatedCards ? nullSafe(state.getName()) : "";
        final String cardImageKey = showCardImage ? nullSafe(state.getImageKey()) : "";
        final boolean showOverlays = showCardImage
                && FModel.getPreferences().getPrefBoolean(FPref.UI_POPUP_CARD_OVERLAYS);
        final boolean disableImages = FModel.getPreferences().getPrefBoolean(
                FPref.UI_DISABLE_CARD_IMAGES);
        // Build a lightweight state key from dynamic overlay properties so the
        // cache is only invalidated when actual overlay content changes, not on
        // every mouse move event.
        final String overlayState = showOverlays
                ? buildOverlayStateKey(cardView, state) : "";

        // Check cache — skip rebuilding if same content and settings
        if (keywordKey.equals(cachedKeywordKey) && cardName.equals(cachedCardName)
                && thumbnailHeight == cachedImageSize
                && showCardImage == cachedShowCardImage
                && showOverlays == cachedShowOverlays
                && overlayState.equals(cachedOverlayState)
                && cardImageKey.equals(cachedCardImageKey)
                && disableImages == cachedDisableImages) {
            if (!cachedHasKeywords && !cachedHasRelated && !showCardImage) {
                hidePopup();
                return;
            }
            positionAndShow(cardScreenLocation, cardSize);
            return;
        }

        // Rebuild content
        boolean hasKeywords = false;
        boolean hasRelated = false;

        // --- Card image section ---
        if (showCardImage) {
            BufferedImage cardImg = FImageUtil.getImage(state);
            if (cardImg != null && !ImageCache.isDefaultImage(cardImg)) {
                if (showOverlays) {
                    cardImg = paintOverlaysOnImage(cardImg, cardView);
                }
                cardImagePanel.setImage(cardImg);
                final int imgWidth = (int) (thumbnailHeight * MTG_ASPECT_RATIO);
                final Dimension imgSize = new Dimension(imgWidth, thumbnailHeight);
                cardImagePanel.setPreferredSize(imgSize);
                cardImagePanel.setMinimumSize(imgSize);
                cardImagePanel.setMaximumSize(imgSize);
                cardImagePanel.setVisible(true);
            } else {
                cardImagePanel.setVisible(false);
                // Trigger auto-download for the selected card image
                fetchIfMissing(state.getImageKey());
            }
        } else {
            cardImagePanel.setVisible(false);
        }

        // --- Keyword section ---
        List<KeywordData> keywordList = List.of();
        if (showKeywords) {
            keywordList = new ArrayList<>();
            final Set<String> addedNames = new LinkedHashSet<>();
            if (!keywordKey.isEmpty()) {
                keywordList.addAll(KeywordInfoUtil.buildKeywords(keywordKey, addedNames));
            }
            // Catch granted abilities that the keywordKey parsing may have missed
            KeywordInfoUtil.addMissingKeywordsFromFlags(keywordList, state, addedNames);
            // Scan oracle text for keyword actions (goad, scry, etc.)
            final String oracleText = nullSafe(state.getOracleText());
            if (!oracleText.isEmpty()) {
                KeywordInfoUtil.addKeywordActions(keywordList, oracleText, addedNames,
                        nullSafe(state.getName()));
            }
            // Sort by card text order
            KeywordInfoUtil.sortByOracleText(keywordList, oracleText);
            // Append dynamic count annotations (e.g. devotion, domain)
            KeywordInfoUtil.annotateKeywordCounts(keywordList, cardView);
            hasKeywords = !keywordList.isEmpty();
        }

        // --- Related cards section ---
        List<RelatedCardEntry> relatedEntries = List.of();
        if (showRelatedCards && !cardName.isEmpty()) {
            relatedEntries = buildRelatedCards(cardName, cardView);
            hasRelated = !relatedEntries.isEmpty();
        }

        // Nothing to show
        if (!hasKeywords && !hasRelated && !cardImagePanel.isVisible()) {
            cachedKeywordKey = keywordKey;
            cachedCardName = cardName;
            cachedHasKeywords = false;
            cachedHasRelated = false;
            cachedImageSize = thumbnailHeight;
            cachedShowCardImage = showCardImage;
            cachedShowOverlays = showOverlays;
            cachedOverlayState = overlayState;
            cachedCardImageKey = cardImageKey;
            cachedDisableImages = disableImages;
            hidePopup();
            return;
        }

        // Hide content panel if only card image is shown (no keywords, no related)
        contentPanel.setVisible(hasKeywords || hasRelated);

        // Calculate max content width based on available space within owner window
        final Rectangle ownerBounds = getOwnerBounds();
        final int rightSpace = ownerBounds.x + ownerBounds.width
                - cardScreenLocation.x - cardSize.width - GAP;
        final int leftSpace = cardScreenLocation.x - ownerBounds.x - GAP;
        final int maxPopupWidth = Math.max(rightSpace, leftSpace);
        final int cardImgWidth = cardImagePanel.isVisible()
                ? cardImagePanel.getPreferredSize().width : 0;
        int maxContentWidth = maxPopupWidth - cardImgWidth - 2 * PADDING - 2 - GAP;
        maxContentWidth = Math.max(maxContentWidth, POPUP_WIDTH);

        // Compute natural width for related cards based on thumbnail count
        // Hover tooltip shows up to HOVER_MAX_CARDS at full size
        int relatedWidth = 0;
        int effectiveThumbHeight = thumbnailHeight;
        if (hasRelated) {
            final int perRow = Math.min(relatedEntries.size(), HOVER_MAX_CARDS);
            final int thumbWidth = (int) (thumbnailHeight * MTG_ASPECT_RATIO);
            relatedWidth = perRow * thumbWidth + 2 * PILL_PAD + 2;
        }
        // Content width: use natural related-cards width, capped by available space
        final int contentWidth = Math.max(POPUP_WIDTH,
                Math.min(relatedWidth, maxContentWidth));

        // Update keywords
        keywordsPanel.setVisible(hasKeywords);
        keywordsPanel.removeAll();
        if (hasKeywords) {
            populateKeywords(keywordsPanel, keywordList, contentWidth);
        }

        // Calculate available height for related cards: fit within owner bounds
        relatedCardsPanel.setVisible(hasRelated);
        relatedCardsPanel.removeAll();
        if (hasRelated) {
            // Measure keyword panel height so we can reserve space for it
            int kwHeight = 0;
            if (hasKeywords) {
                keywordsPanel.setPreferredSize(null);
                kwHeight = keywordsPanel.getPreferredSize().height + GAP;
            }
            final int maxPopupHeight = ownerBounds.height - 2 * PADDING;
            final int availableHeight = maxPopupHeight - kwHeight;
            // Count thumbnail rows and overhead to scale height properly
            final Set<String> groups = new LinkedHashSet<>();
            for (final RelatedCardEntry e : relatedEntries) {
                groups.add(e.label);
            }
            final int numGroups = Math.max(1, groups.size());
            // Each group: ~28px overhead (label + spacing), plus inter-group gaps
            final int overhead = numGroups * 28 + (numGroups - 1) * 4 + 20;
            final int thumbRows = numGroups; // 1 row per group with HOVER_MAX_CARDS cap
            final int availableForThumbs = availableHeight - overhead;
            effectiveThumbHeight = Math.min(effectiveThumbHeight,
                    Math.max(80, availableForThumbs / thumbRows));
            populateRelatedCards(relatedCardsPanel, relatedEntries,
                    effectiveThumbHeight, contentWidth);
        }

        // Update cache
        cachedKeywordKey = keywordKey;
        cachedCardName = cardName;
        cachedHasKeywords = hasKeywords;
        cachedHasRelated = hasRelated;
        cachedImageSize = thumbnailHeight;
        cachedShowCardImage = showCardImage;
        cachedShowOverlays = showOverlays;
        cachedOverlayState = overlayState;
        cachedCardImageKey = cardImageKey;
        cachedDisableImages = disableImages;

        // Defer pack/show to let layout complete
        final Point loc = cardScreenLocation;
        final Dimension size = cardSize;
        final int finalMaxPopup = Math.max(maxPopupWidth, POPUP_WIDTH);
        final int maxHeight = ownerBounds.height;
        SwingUtilities.invokeLater(() -> {
            keywordsPanel.setPreferredSize(null);
            window.pack();
            if (window.getWidth() > finalMaxPopup) {
                window.setSize(finalMaxPopup, window.getHeight());
            }
            if (window.getHeight() > maxHeight) {
                window.setSize(window.getWidth(), maxHeight);
            }
            positionAndShow(loc, size);
        });
    }

    public void hidePopup() {
        showTimer.stop();
        window.setVisible(false);
        if (activePopup == this) {
            activePopup = null;
        }
    }

    /** Hide whichever popup is currently showing (if any). */
    public static void hideActive() {
        if (activePopup != null) {
            activePopup.hidePopup();
        }
    }

    /** Release the popup window and its resources. */
    public void dispose() {
        hidePopup();
        window.dispose();
    }

    // --- Auto-download ---

    private void fetchIfMissing(final String imageKey) {
        if (imageKey == null || imageKey.isEmpty()) {
            return;
        }
        CachedCardImage.fetcher.fetchImage(imageKey, () -> {
            // Invalidate cache and re-show with the downloaded image
            cachedCardImageKey = "";
            cachedCardName = "";
            if (lastCardView != null) {
                showForCard(lastCardView, lastCardScreenLocation, lastCardSize,
                        lastShowKeywords, lastShowRelatedCards, lastShowCardImage);
            }
        });
    }

    // --- Keyword display ---

    /** Creates a JLabel that uses grayscale AA (LCD AA breaks on transparent windows). */
    public static javax.swing.JLabel createAALabel(final String text) {
        return new javax.swing.JLabel(text) {
            @Override
            protected void paintComponent(final java.awt.Graphics g) {
                ((Graphics2D) g).setRenderingHint(
                        java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                super.paintComponent(g);
            }
        };
    }

    public static JPanel createPillPanel() {
        final JPanel pill = new JPanel() {
            @Override
            protected void paintComponent(final java.awt.Graphics g) {
                final Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(PILL_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(),
                        PILL_CORNER, PILL_CORNER);
                g2.setColor(PILL_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                        PILL_CORNER, PILL_CORNER);
                g2.dispose();
            }
        };
        pill.setLayout(new BoxLayout(pill, BoxLayout.Y_AXIS));
        pill.setBorder(BorderFactory.createEmptyBorder(
                PILL_PAD, PILL_PAD, PILL_PAD, PILL_PAD));
        pill.setOpaque(false);
        pill.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        return pill;
    }

    public static void populateKeywords(final JPanel panel,
                                          final List<KeywordData> keywords,
                                          final int maxWidth) {
        final FSkin.SkinFont boldFont = FSkin.getBoldFont(12);
        final FSkin.SkinFont reminderFont = FSkin.getFont(12);
        final int textWidth = maxWidth - 2 * PILL_PAD - 2; // account for pill border+pad

        for (int i = 0; i < keywords.size(); i++) {
            if (i > 0) {
                panel.add(javax.swing.Box.createRigidArea(new Dimension(0, 4)));
            }
            final KeywordData kw = keywords.get(i);

            final JPanel pill = createPillPanel();

            // Keyword name label (encode mana symbols for Swing HTML)
            final javax.swing.JLabel nameLabel = createAALabel(
                    FSkin.encodeSymbols(kw.name, false));
            nameLabel.setFont(boldFont.getBaseFont());
            nameLabel.setForeground(TEXT_PRIMARY);
            nameLabel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
            nameLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                    nameLabel.getPreferredSize().height));
            pill.add(nameLabel);

            // Reminder text (encode mana symbols for Swing HTML)
            if (!kw.reminderText.isEmpty()) {
                final javax.swing.JLabel reminderLabel = createAALabel(
                        FSkin.encodeSymbols(kw.reminderText, false));
                reminderLabel.setFont(reminderFont.getBaseFont());
                reminderLabel.setForeground(TEXT_SECONDARY);
                reminderLabel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
                // Use the HTML View to compute wrapped height at constrained width
                final javax.swing.text.View view = (javax.swing.text.View)
                        reminderLabel.getClientProperty(
                                javax.swing.plaf.basic.BasicHTML.propertyKey);
                int prefHeight;
                if (view != null) {
                    view.setSize(textWidth, 0);
                    prefHeight = (int) Math.ceil(
                            view.getPreferredSpan(javax.swing.text.View.Y_AXIS));
                } else {
                    prefHeight = reminderLabel.getPreferredSize().height;
                }
                reminderLabel.setPreferredSize(new Dimension(textWidth, prefHeight));
                reminderLabel.setMaximumSize(new Dimension(textWidth, prefHeight));
                pill.add(reminderLabel);
            }

            pill.setMaximumSize(new Dimension(maxWidth, Integer.MAX_VALUE));
            panel.add(pill);
        }
    }

    // --- Related cards building ---

    /**
     * Build related card entries (static, no auto-download side effects).
     * Callers that need auto-download should iterate entries and call
     * fetchIfMissing for those with null/default images.
     */
    public static List<RelatedCardEntry> buildRelatedCardsStatic(final String cardName,
                                                                  final CardView cardView) {
        final List<RelatedCardEntry> entries = new ArrayList<>();

        try {
            final StaticData data = StaticData.instance();
            if (data == null) {
                return entries;
            }
            final CardRules rules = data.getCommonCards().getRules(cardName);
            if (rules == null) {
                return entries;
            }

            final CardSplitType splitType = rules.getSplitType();

            // Tokens — for multi-face cards, only show if the current face
            // creates them (the token list is shared across all faces)
            boolean showTokens = true;
            if (splitType != null && splitType != CardSplitType.None) {
                final CardStateView curState = cardView.getCurrentState();
                final String oracle = curState != null
                        ? curState.getOracleText() : null;
                showTokens = oracle != null
                        && oracle.toLowerCase().contains("token");
            }
            if (showTokens) {
                final List<String> tokenNames = rules.getTokens();
                if (tokenNames != null && !tokenNames.isEmpty()) {
                    for (final String tokenName : tokenNames) {
                        final PaperToken pt = data.getAllTokens().getToken(
                                tokenName);
                        if (pt != null) {
                            final CardView tokenView =
                                    Card.getCardForUi(pt).getView();
                            final String imageKey = pt.getCardImageKey();
                            final Pair<BufferedImage, Boolean> info =
                                    ImageCache.getCardOriginalImageInfo(
                                            imageKey, true, tokenView);
                            final BufferedImage img = info.getLeft();
                            if (img != null) {
                                entries.add(new RelatedCardEntry("Creates",
                                        pt.getName(), img, imageKey,
                                        info.getRight()));
                            }
                        }
                    }
                }
            }
            // Amass tokens — not declared via TokenScript$, detect from oracle text
            addAmassTokenEntry(entries, cardView, data);

            if (splitType != null) {
                switch (splitType) {
                    case Transform:
                        addOtherFaceEntry(entries, cardView, "Transforms Into");
                        break;
                    case Modal:
                        addOtherFaceEntry(entries, cardView, "Other Face");
                        break;
                    case Meld:
                        addNamedCardEntry(entries, rules.getMeldWith(), "Meld", data);
                        addOtherFaceEntry(entries, cardView, "Meld");
                        break;
                    case Flip:
                        addFlipFaceEntry(entries, cardView);
                        break;
                    case Specialize:
                        addSpecializeFaces(entries, rules, cardName, data);
                        break;
                    default:
                        // Split, Adventure, None — skip
                        break;
                }
            }

            // Partner with
            addNamedCardEntry(entries, rules.getPartnerWith(), "Partner", data);

            // Spellbook entries
            addSpellbookEntries(entries, rules, data);

            // ChooseFromList entries (e.g. Garth One-Eye)
            addChooseFromListEntries(entries, rules, data);

            // The Ring emblem for cards with "the Ring tempts you"
            final CardStateView curState = cardView.getCurrentState();
            final String oracle = curState != null ? curState.getOracleText() : null;
            if (oracle != null && oracle.toLowerCase().contains("the ring tempts you")) {
                addRingEmblemEntry(entries, data);
            }

        } catch (Exception e) {
            // Guard against any lookup failures
        }

        return entries;
    }

    private List<RelatedCardEntry> buildRelatedCards(final String cardName,
                                                     final CardView cardView) {
        final List<RelatedCardEntry> entries = buildRelatedCardsStatic(cardName, cardView);
        // Trigger auto-download for entries with null/default images
        for (final RelatedCardEntry entry : entries) {
            if (entry.image == null || entry.placeholder) {
                fetchIfMissing(entry.imageKey);
            }
        }
        return entries;
    }

    private static void addOtherFaceEntry(final List<RelatedCardEntry> entries,
                                           final CardView cardView, final String label) {
        final CardStateView altState = cardView.getAlternateState();
        if (altState == null) {
            return;
        }
        final Pair<BufferedImage, Boolean> info = ImageCache.getCardOriginalImageInfo(
                altState.getImageKey(), true, altState.getCard());
        final BufferedImage img = info.getLeft();
        if (img != null) {
            entries.add(new RelatedCardEntry(label, altState.getName(),
                    img, altState.getImageKey(), info.getRight()));
        }
    }

    private static void addFlipFaceEntry(final List<RelatedCardEntry> entries,
                                          final CardView cardView) {
        final CardStateView altState = cardView.getAlternateState();
        if (altState == null) {
            return;
        }
        final Pair<BufferedImage, Boolean> info = ImageCache.getCardOriginalImageInfo(
                altState.getImageKey(), true, altState.getCard());
        final BufferedImage img = info.getLeft();
        if (img != null) {
            final BufferedImage displayImg = info.getRight()
                    ? img : rotateImage180(img);
            entries.add(new RelatedCardEntry("Flips Into", altState.getName(),
                    displayImg, altState.getImageKey(), info.getRight()));
        }
    }

    private static BufferedImage rotateImage180(final BufferedImage src) {
        final int w = src.getWidth();
        final int h = src.getHeight();
        final BufferedImage rotated = new BufferedImage(w, h, src.getType());
        final Graphics2D g = rotated.createGraphics();
        g.rotate(Math.PI, w / 2.0, h / 2.0);
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return rotated;
    }

    private static void addSpecializeFaces(final List<RelatedCardEntry> entries,
                                            final CardRules rules, final String cardName,
                                            final StaticData data) {
        final Map<CardStateName, ICardFace> specParts = rules.getSpecializeParts();
        if (specParts == null || specParts.isEmpty()) {
            return;
        }
        final PaperCard baseCard = data.getCommonCards().getCard(cardName);
        if (baseCard == null) {
            return;
        }
        // Use c:-prefixed keys with $wspec/$uspec/etc. postfixes (same format as
        // CardFactory) so ImageCache can resolve specialize faces for both file
        // lookup and Forge renderer fallback
        final String baseKey = baseCard.getImageKey(false);
        for (final Map.Entry<CardStateName, ICardFace> entry : specParts.entrySet()) {
            try {
                final String imageKey = specImageKey(baseKey, entry.getKey());
                if (imageKey == null) {
                    continue;
                }
                final Pair<BufferedImage, Boolean> info =
                        ImageCache.getCardOriginalImageInfo(
                                imageKey, true);
                final BufferedImage img = info.getLeft();
                if (img != null) {
                    entries.add(new RelatedCardEntry("Specializes Into",
                            entry.getValue().getName(), img, imageKey,
                            info.getRight()));
                }
            } catch (Exception e) {
                // Skip faces that can't be resolved
            }
        }
    }

    private static String specImageKey(final String baseKey,
                                        final CardStateName state) {
        switch (state) {
            case SpecializeW: return baseKey + ImageKeys.SPECFACE_W;
            case SpecializeU: return baseKey + ImageKeys.SPECFACE_U;
            case SpecializeB: return baseKey + ImageKeys.SPECFACE_B;
            case SpecializeR: return baseKey + ImageKeys.SPECFACE_R;
            case SpecializeG: return baseKey + ImageKeys.SPECFACE_G;
            default: return null;
        }
    }

    private static void addSpellbookEntries(final List<RelatedCardEntry> entries,
                                             final CardRules rules, final StaticData data) {
        final Set<String> spellbookNames = extractSpellbookNames(rules.getMainPart());
        if (spellbookNames.isEmpty()) {
            return;
        }
        for (final String rawName : spellbookNames) {
            // Spellbook$ uses ";" instead of "," for card names that contain commas
            final String cardName = rawName.replace(";", ",").trim();
            try {
                final PaperCard pc = data.getCommonCards().getCard(cardName);
                if (pc != null) {
                    final String imageKey = pc.getImageKey(false);
                    final Pair<BufferedImage, Boolean> info =
                            ImageCache.getCardOriginalImageInfo(imageKey, true);
                    final BufferedImage img = info.getLeft();
                    if (img != null) {
                        entries.add(new RelatedCardEntry("Spellbook", cardName,
                                img, imageKey, info.getRight()));
                    }
                }
            } catch (Exception e) {
                // Skip cards that can't be resolved
            }
        }
    }

    private static void addNamedCardEntry(final List<RelatedCardEntry> entries,
                                           final String name, final String label,
                                           final StaticData data) {
        if (name == null || name.isEmpty()) {
            return;
        }
        try {
            final PaperCard pc = data.getCommonCards().getCard(name);
            if (pc != null) {
                final String imageKey = pc.getImageKey(false);
                final Pair<BufferedImage, Boolean> info =
                        ImageCache.getCardOriginalImageInfo(imageKey, true);
                final BufferedImage img = info.getLeft();
                if (img != null) {
                    entries.add(new RelatedCardEntry(label, name, img, imageKey,
                            info.getRight()));
                }
            }
        } catch (Exception e) {
            // Skip cards that can't be resolved
        }
    }

    private static Set<String> extractSpellbookNames(final ICardFace face) {
        final Set<String> names = new LinkedHashSet<>();
        // Search abilities
        if (face.getAbilities() != null) {
            for (final String ability : face.getAbilities()) {
                extractSpellbookFromLine(ability, names);
            }
        }
        // Search triggers (some reference SVars containing Spellbook$)
        if (face.getTriggers() != null) {
            for (final String trigger : face.getTriggers()) {
                extractSpellbookFromLine(trigger, names);
            }
        }
        // Search SVars (e.g. SVar:TrigDraft:DB$ Draft | Spellbook$ ...)
        if (face.getVariables() != null) {
            for (final Entry<String, String> svar : face.getVariables()) {
                extractSpellbookFromLine(svar.getValue(), names);
            }
        }
        return names;
    }

    private static void extractSpellbookFromLine(final String line, final Set<String> names) {
        if (line == null || !line.contains("Spellbook$")) {
            return;
        }
        final int start = line.indexOf("Spellbook$") + "Spellbook$".length();
        // Find end: next " |" delimiter or end of string
        int end = line.indexOf(" | ", start);
        if (end < 0) {
            end = line.length();
        }
        final String value = line.substring(start, end).trim();
        if (!value.isEmpty()) {
            for (final String name : value.split(",")) {
                final String trimmed = name.trim();
                if (!trimmed.isEmpty()) {
                    names.add(trimmed);
                }
            }
        }
    }

    private static void addChooseFromListEntries(final List<RelatedCardEntry> entries,
                                                     final CardRules rules,
                                                     final StaticData data) {
        final Set<String> names = extractChooseFromListNames(rules.getMainPart());
        if (names.isEmpty()) {
            return;
        }
        for (final String cardName : names) {
            try {
                final PaperCard pc = data.getCommonCards().getCard(cardName);
                if (pc != null) {
                    final String imageKey = pc.getImageKey(false);
                    final Pair<BufferedImage, Boolean> info =
                            ImageCache.getCardOriginalImageInfo(imageKey, true);
                    final BufferedImage img = info.getLeft();
                    if (img != null) {
                        entries.add(new RelatedCardEntry("Related", cardName,
                                img, imageKey, info.getRight()));
                    }
                }
            } catch (Exception e) {
                // Skip cards that can't be resolved
            }
        }
    }

    private static Set<String> extractChooseFromListNames(final ICardFace face) {
        final Set<String> names = new LinkedHashSet<>();
        if (face.getAbilities() != null) {
            for (final String ability : face.getAbilities()) {
                extractChooseFromListLine(ability, names);
            }
        }
        if (face.getTriggers() != null) {
            for (final String trigger : face.getTriggers()) {
                extractChooseFromListLine(trigger, names);
            }
        }
        if (face.getVariables() != null) {
            for (final Entry<String, String> svar : face.getVariables()) {
                extractChooseFromListLine(svar.getValue(), names);
            }
        }
        return names;
    }

    private static void extractChooseFromListLine(final String line,
                                                    final Set<String> names) {
        if (line == null || !line.contains("ChooseFromList$")) {
            return;
        }
        final int start = line.indexOf("ChooseFromList$")
                + "ChooseFromList$".length();
        int end = line.indexOf(" | ", start);
        if (end < 0) {
            end = line.length();
        }
        final String value = line.substring(start, end).trim();
        if (!value.isEmpty()) {
            for (final String name : value.split(",")) {
                final String trimmed = name.trim();
                if (!trimmed.isEmpty()) {
                    names.add(trimmed);
                }
            }
        }
    }

    private static final String RING_ORACLE_TEXT =
            "1: Your Ring-bearer is legendary and can't be blocked by creatures "
            + "with greater power.\n"
            + "2: Whenever your Ring-bearer attacks, draw a card, then discard a card.\n"
            + "3: Whenever your Ring-bearer becomes blocked by a creature, that "
            + "creature's controller sacrifices it at end of combat.\n"
            + "4: Whenever your Ring-bearer deals combat damage to a player, each "
            + "opponent loses 3 life.";

    private static void addRingEmblemEntry(final List<RelatedCardEntry> entries,
                                              final StaticData data) {
        final String ringKey = data.getOtherImageKey(
                ImageKeys.THE_RING_IMAGE, null);
        // Build a lightweight CardView with oracle text so the renderer
        // can paint a proper fallback. Pure view-layer, no Game needed.
        final Card ringCard = new Card(-1, null, null);
        ringCard.setName("The Ring");
        ringCard.setOracleText(RING_ORACLE_TEXT);
        final CardView ringView = ringCard.getView();
        final Pair<BufferedImage, Boolean> info =
                ImageCache.getCardOriginalImageInfo(ringKey, true, ringView);
        final BufferedImage img = info.getLeft();
        if (img != null) {
            entries.add(new RelatedCardEntry("Emblem",
                    "The Ring", img, ringKey, info.getRight()));
        }
    }

    private static final java.util.regex.Pattern AMASS_PATTERN =
            java.util.regex.Pattern.compile("amass (\\w+)", java.util.regex.Pattern.CASE_INSENSITIVE);

    private static void addAmassTokenEntry(final List<RelatedCardEntry> entries,
                                            final CardView cardView,
                                            final StaticData data) {
        final CardStateView curState = cardView.getCurrentState();
        final String oracle = curState != null ? curState.getOracleText() : null;
        if (oracle == null) {
            return;
        }
        final java.util.regex.Matcher m = AMASS_PATTERN.matcher(oracle);
        if (!m.find()) {
            return;
        }
        // e.g. "Zombies" → "zombie", "Orcs" → "orc"
        String type = m.group(1).toLowerCase();
        if (type.endsWith("s")) {
            type = type.substring(0, type.length() - 1);
        }
        final String tokenName = "b_0_0_" + type + "_army";
        final PaperToken pt = data.getAllTokens().getToken(tokenName);
        if (pt == null) {
            return;
        }
        final CardView tokenView = Card.getCardForUi(pt).getView();
        final String imageKey = pt.getCardImageKey();
        final Pair<BufferedImage, Boolean> info =
                ImageCache.getCardOriginalImageInfo(imageKey, true, tokenView);
        final BufferedImage img = info.getLeft();
        if (img != null) {
            entries.add(new RelatedCardEntry("Creates",
                    pt.getName(), img, imageKey, info.getRight()));
        }
    }

    private static final int FULL_SIZE_MAX = 3;
    private static final int HOVER_MAX_CARDS = 2;

    public static void populateRelatedCards(final JPanel targetPanel,
                                              final List<RelatedCardEntry> entries,
                                              final int thumbnailHeight,
                                              final int maxContentWidth) {
        // Hover tooltip: cap per group to HOVER_MAX_CARDS,
        // with per-group overflow labels inside their pills
        final LinkedHashMap<String, List<RelatedCardEntry>> grouped = new LinkedHashMap<>();
        for (final RelatedCardEntry entry : entries) {
            grouped.computeIfAbsent(entry.label, k -> new ArrayList<>()).add(entry);
        }
        final List<RelatedCardEntry> visible = new ArrayList<>();
        final Map<String, Integer> overflowByGroup = new LinkedHashMap<>();
        for (final Map.Entry<String, List<RelatedCardEntry>> group : grouped.entrySet()) {
            final List<RelatedCardEntry> cards = group.getValue();
            if (cards.size() > HOVER_MAX_CARDS) {
                visible.addAll(cards.subList(0, HOVER_MAX_CARDS));
                overflowByGroup.put(group.getKey(), cards.size() - HOVER_MAX_CARDS);
            } else {
                visible.addAll(cards);
            }
        }
        populateRelatedCards(targetPanel, visible, thumbnailHeight,
                maxContentWidth, HOVER_MAX_CARDS, false,
                overflowByGroup.isEmpty() ? null : overflowByGroup);
    }

    public static void populateRelatedCards(final JPanel targetPanel,
                                              final List<RelatedCardEntry> entries,
                                              final int thumbnailHeight,
                                              final int maxContentWidth,
                                              final int maxPerRow) {
        populateRelatedCards(targetPanel, entries, thumbnailHeight,
                maxContentWidth, maxPerRow, false, null);
    }

    public static void populateRelatedCards(final JPanel targetPanel,
                                              final List<RelatedCardEntry> entries,
                                              final int thumbnailHeight,
                                              final int maxContentWidth,
                                              final int maxPerRow,
                                              final boolean alwaysFullSize,
                                              final Map<String, Integer> overflowCounts) {
        // Group entries by label
        final LinkedHashMap<String, List<RelatedCardEntry>> grouped = new LinkedHashMap<>();
        for (final RelatedCardEntry entry : entries) {
            grouped.computeIfAbsent(entry.label, k -> new ArrayList<>()).add(entry);
        }

        final FSkin.SkinFont boldFont = FSkin.getBoldFont(12);
        boolean firstGroup = true;

        for (final Map.Entry<String, List<RelatedCardEntry>> group : grouped.entrySet()) {
            final List<RelatedCardEntry> cards = group.getValue();

            if (!firstGroup) {
                targetPanel.add(javax.swing.Box.createRigidArea(
                        new Dimension(0, 4)));
            }
            firstGroup = false;

            // Wrap each section group in a pill
            final JPanel pill = createPillPanel();

            // Section label — include total count when truncated or overflowing
            final int overflowForLabel = overflowCounts != null
                    && overflowCounts.containsKey(group.getKey())
                    ? overflowCounts.get(group.getKey()) : 0;
            final int totalInGroup = cards.size() + overflowForLabel;
            final String labelText = group.getKey()
                    + (totalInGroup > maxPerRow
                            ? " (" + totalInGroup + ")" : "");
            final javax.swing.JLabel sectionLabel = createAALabel(labelText);
            sectionLabel.setFont(boldFont.getBaseFont());
            sectionLabel.setForeground(TEXT_PRIMARY);
            sectionLabel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
            sectionLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                    sectionLabel.getPreferredSize().height));
            pill.add(sectionLabel);
            pill.add(javax.swing.Box.createRigidArea(new Dimension(0, 4)));

            // Full size when group is small or forced by caller (zoom view)
            final boolean fullSize = alwaysFullSize
                    || cards.size() <= FULL_SIZE_MAX;
            final int perRow = fullSize
                    ? Math.min(cards.size(), Math.min(FULL_SIZE_MAX, maxPerRow))
                    : maxPerRow;
            final int pillInnerWidth = maxContentWidth - 2 * PILL_PAD - 2;
            int effectiveHeight = fullSize
                    ? thumbnailHeight : Math.max(80, thumbnailHeight / 2);

            // Constrain thumbnail size so cards fit within pill inner width
            final int maxThumbWidth = pillInnerWidth / perRow;
            final int maxHeightForWidth = (int) (maxThumbWidth / MTG_ASPECT_RATIO);
            effectiveHeight = Math.min(effectiveHeight, maxHeightForWidth);

            // Build rows manually (FlowLayout doesn't wrap with pack())
            JPanel currentRow = null;
            int colIndex = 0;
            int thumbWidth = (int) (effectiveHeight * MTG_ASPECT_RATIO);
            int thumbHeight = effectiveHeight;
            if (thumbWidth > maxThumbWidth) {
                thumbWidth = maxThumbWidth;
                thumbHeight = (int) (thumbWidth / MTG_ASPECT_RATIO);
            }
            final Dimension thumbSize = new Dimension(thumbWidth, thumbHeight);

            for (int ci = 0; ci < cards.size(); ci++) {
                if (colIndex == 0) {
                    currentRow = new JPanel();
                    currentRow.setLayout(new BoxLayout(currentRow, BoxLayout.X_AXIS));
                    currentRow.setOpaque(false);
                    currentRow.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);

                    // Center-justify incomplete last row
                    final int remaining = cards.size() - ci;
                    if (remaining < perRow) {
                        currentRow.add(javax.swing.Box.createHorizontalGlue());
                    }

                    pill.add(currentRow);
                }
                final FImagePanel imgPanel = new FImagePanel();
                imgPanel.setImage(cards.get(ci).image);
                imgPanel.setPreferredSize(thumbSize);
                imgPanel.setMinimumSize(thumbSize);
                imgPanel.setMaximumSize(thumbSize);
                currentRow.add(imgPanel);
                colIndex++;
                if (colIndex >= perRow) {
                    colIndex = 0;
                }
            }
            // Close trailing glue for centered last row
            if (colIndex > 0 && colIndex < perRow) {
                currentRow.add(javax.swing.Box.createHorizontalGlue());
            }

            // Add overflow label inside pill if applicable
            if (overflowCounts != null
                    && overflowCounts.containsKey(group.getKey())) {
                final int overflow = overflowCounts.get(group.getKey());
                final javax.swing.JLabel overflowLabel = createAALabel(
                        "+" + overflow + " more (zoom for full list).");
                overflowLabel.setForeground(TEXT_SECONDARY);
                overflowLabel.setFont(FSkin.getBoldFont(11).getBaseFont());
                overflowLabel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
                overflowLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(
                        4, 0, 0, 0));
                pill.add(overflowLabel);
            }

            final int actualPillWidth = perRow * thumbWidth + 2 * PILL_PAD + 2;
            pill.setMaximumSize(new Dimension(actualPillWidth, Integer.MAX_VALUE));
            targetPanel.add(pill);
        }
    }

    // --- Card overlay state key ---

    /** Build a lightweight key capturing dynamic overlay state so the cache
     *  can detect when a rebuild is actually needed. */
    private static String buildOverlayStateKey(final CardView cv,
                                                final CardStateView state) {
        final StringBuilder sb = new StringBuilder(64);
        sb.append(state.getPower()).append('/').append(state.getToughness());
        sb.append(',').append(state.getLoyalty());
        sb.append(',').append(cv.getDamage());
        sb.append(',').append(cv.isAttacking() ? 'A' : cv.isBlocking() ? 'B' : '-');
        sb.append(',').append(cv.isSick() ? 'S' : '-');
        sb.append(',').append(cv.isPhasedOut() ? 'P' : '-');
        final Map<CounterType, Integer> counters = cv.getCounters();
        if (counters != null && !counters.isEmpty()) {
            sb.append(',');
            for (final Map.Entry<CounterType, Integer> e : counters.entrySet()) {
                sb.append(e.getKey().getName()).append('=').append(e.getValue()).append(';');
            }
        }
        // Append hover overlay pref state
        final ForgePreferences prefs = FModel.getPreferences();
        sb.append(',');
        sb.append(prefs.getPrefBoolean(FPref.UI_HOVER_OVERLAY_CARD_NAME) ? '1' : '0');
        sb.append(prefs.getPrefBoolean(FPref.UI_HOVER_OVERLAY_CARD_POWER) ? '1' : '0');
        sb.append(prefs.getPrefBoolean(FPref.UI_HOVER_OVERLAY_CARD_MANA_COST) ? '1' : '0');
        sb.append(prefs.getPrefBoolean(FPref.UI_HOVER_OVERLAY_CARD_PERPETUAL_MANA_COST) ? '1' : '0');
        sb.append(prefs.getPrefBoolean(FPref.UI_HOVER_OVERLAY_CARD_ID) ? '1' : '0');
        sb.append(prefs.getPrefBoolean(FPref.UI_HOVER_OVERLAY_ABILITY_ICONS) ? '1' : '0');
        return sb.toString();
    }

    // --- Card overlay painting ---

    /**
     * Composite the same overlays that CardPanel draws (P/T, name, ability
     * icons, counters, combat status) directly onto a copy of the card image.
     */
    static BufferedImage paintOverlaysOnImage(final BufferedImage src,
                                               final CardView cardView) {
        final ForgePreferences prefs = FModel.getPreferences();
        final CardStateView state = cardView.getCurrentState();
        if (state == null) {
            return src;
        }

        final int w = src.getWidth();
        final int h = src.getHeight();
        final BufferedImage copy = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = copy.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Scale factor: slightly larger than CardPanel's h/340 since
        // tooltip thumbnails are viewed at a larger effective size
        final float scale = h / 270f;
        final Font baseFont = new Font("Dialog", Font.BOLD,
                Math.max(9, Math.round(13 * scale)));
        final Color outlineColor = Color.BLACK;

        // --- Card name ---
        if (prefs.getPrefBoolean(FPref.UI_HOVER_OVERLAY_CARD_NAME)) {
            final String name = CardTranslation.getTranslatedName(
                    state.getName());
            if (name != null && !name.isEmpty()) {
                final int titleX = Math.round(w * (24f / 480));
                final int titleY = Math.round(h * (52f / 640));
                drawOutlinedText(g, name, baseFont, Color.WHITE,
                        outlineColor, titleX, titleY);
            }
        }

        // --- P/T, loyalty, vehicle ---
        if (prefs.getPrefBoolean(FPref.UI_HOVER_OVERLAY_CARD_POWER)) {
            String pt = "";
            if (state.isCreature() && state.isPlaneswalker()) {
                pt = state.getPower() + "/" + state.getToughness()
                        + " (" + state.getLoyalty() + ")";
            } else if (state.isCreature()) {
                pt = state.getPower() + "/" + state.getToughness();
            } else if (state.isVehicle()) {
                pt = "[" + state.getPower() + "/" + state.getToughness() + "]";
            } else if (state.isPlaneswalker()) {
                pt = state.getLoyalty();
            }
            if (!pt.isEmpty()) {
                final Font ptFont = baseFont.deriveFont(
                        baseFont.getSize2D() * 1.1f);
                final FontMetrics fm = g.getFontMetrics(ptFont);
                final int ptX = Math.round(w * (410f / 480))
                        - fm.stringWidth(pt) / 2;
                final int ptY = Math.round(h * (650f / 680));
                drawOutlinedText(g, pt, ptFont, Color.WHITE,
                        outlineColor, ptX, ptY);

                // Damage marker above P/T
                final int damage = cardView.getDamage();
                if (damage > 0) {
                    final String dmgStr = "\u00BB " + damage + " \u00AB";
                    final int dmgX = Math.round(w * (410f / 480))
                            - fm.stringWidth(dmgStr) / 2;
                    final int dmgY = ptY - Math.round(16 * scale);
                    drawOutlinedText(g, dmgStr, ptFont, Color.RED,
                            outlineColor, dmgX, dmgY);
                }
            }
        }

        // --- Card ID ---
        if (prefs.getPrefBoolean(FPref.UI_HOVER_OVERLAY_CARD_ID)) {
            final String id = state.getDisplayId();
            if (id != null && !id.isEmpty()) {
                final Font idFont = baseFont.deriveFont(Font.PLAIN,
                        baseFont.getSize2D() * 0.85f);
                final int idX = Math.round(w * (24f / 480));
                final int idY = Math.round(h * (650f / 680));
                drawOutlinedText(g, id, idFont, new Color(200, 200, 200),
                        outlineColor, idX, idY);
            }
        }

        // --- Mana cost ---
        if (prefs.getPrefBoolean(FPref.UI_HOVER_OVERLAY_CARD_MANA_COST)) {
            final boolean perpetual = prefs.getPrefBoolean(
                    FPref.UI_HOVER_OVERLAY_CARD_PERPETUAL_MANA_COST);
            final ManaCost cost = perpetual
                    ? state.getManaCost() : state.getOriginalManaCost();
            if (cost != null && !cost.isNoCost()) {
                final int symSize = Math.max(10, Math.round(16 * scale));
                final int manaWidth = cost.getGlyphCount() * (symSize + 1);
                final int manaX = (w / 2) - (manaWidth / 2);
                final int manaY = h / 2 - symSize / 2;
                CardFaceSymbols.draw(g, cost, manaX, manaY, symSize);
            }
        }

        // --- Ability icons (battlefield only) ---
        if (prefs.getPrefBoolean(FPref.UI_HOVER_OVERLAY_ABILITY_ICONS)
                && ZoneType.Battlefield.equals(cardView.getZone())) {
            final int abiScale = w / 7;
            final int abiX = w / 2 + w / 3;
            final int abiSpace = w / 7;
            int abiY = w < 200 ? 25 : 50;
            abiY = Math.round(abiY * scale);

            abiY = drawAbilityIcons(g, state, cardView,
                    abiX, abiY, abiScale, abiSpace);
        }

        // --- Combat status icons ---
        if (ZoneType.Battlefield.equals(cardView.getZone())) {
            final int symSize = Math.max(16, Math.round(32 * scale));
            final int combatX = w / 4 - symSize / 2;
            final int stateX = w / 2 - symSize / 2;
            final int ySymbols = h - h / 8 - symSize / 2;

            if (cardView.isAttacking()) {
                CardFaceSymbols.drawAbilitySymbol("attack", g,
                        combatX, ySymbols, symSize, symSize);
            } else if (cardView.isBlocking()) {
                CardFaceSymbols.drawAbilitySymbol("defend", g,
                        combatX, ySymbols, symSize, symSize);
            }
            if (cardView.isSick()) {
                CardFaceSymbols.drawAbilitySymbol("summonsick", g,
                        stateX, ySymbols, symSize, symSize);
            }
            if (cardView.isPhasedOut()) {
                CardFaceSymbols.drawAbilitySymbol("phasing", g,
                        stateX, ySymbols, symSize, symSize);
            }
        }

        // --- Counters ---
        final Map<CounterType, Integer> counters = cardView.getCounters();
        if (counters != null && !counters.isEmpty()) {
            drawCounterOverlay(g, counters, w, h, scale);
        }

        g.dispose();
        return copy;
    }

    /** Draw ability icons on the right side of the card, returning updated Y. */
    private static int drawAbilityIcons(final Graphics2D g,
                                         final CardStateView state,
                                         final CardView cardView,
                                         final int abiX, int abiY,
                                         final int abiScale,
                                         final int abiSpace) {
        if (cardView.isCommander()) {
            CardFaceSymbols.drawAbilitySymbol("commander", g,
                    abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
        }
        if (cardView.isRingBearer()) {
            CardFaceSymbols.drawAbilitySymbol("ringbearer", g,
                    abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
        }
        if (state.hasFlying()) {
            CardFaceSymbols.drawAbilitySymbol("flying", g,
                    abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
        }
        if (state.hasHaste()) {
            CardFaceSymbols.drawAbilitySymbol("haste", g,
                    abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
        }
        if (state.hasDoubleStrike()) {
            CardFaceSymbols.drawAbilitySymbol("doublestrike", g,
                    abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
        } else if (state.hasFirstStrike()) {
            CardFaceSymbols.drawAbilitySymbol("firststrike", g,
                    abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
        }
        if (state.hasAnnihilator()) {
            CardFaceSymbols.drawAbilitySymbol("annihilator", g,
                    abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
        }
        if (state.hasExalted()) {
            CardFaceSymbols.drawAbilitySymbol("exalted", g,
                    abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
        }
        if (state.hasDeathtouch()) {
            CardFaceSymbols.drawAbilitySymbol("deathtouch", g,
                    abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
        }
        if (state.hasToxic()) {
            CardFaceSymbols.drawAbilitySymbol("toxic", g,
                    abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
        }
        if (state.hasIndestructible()) {
            CardFaceSymbols.drawAbilitySymbol("indestructible", g,
                    abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
        }
        if (state.hasMenace()) {
            CardFaceSymbols.drawAbilitySymbol("menace", g,
                    abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
        }
        if (state.hasFear()) {
            CardFaceSymbols.drawAbilitySymbol("fear", g,
                    abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
        }
        if (state.hasIntimidate()) {
            CardFaceSymbols.drawAbilitySymbol("intimidate", g,
                    abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
        }
        if (state.hasShadow()) {
            CardFaceSymbols.drawAbilitySymbol("shadow", g,
                    abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
        }
        if (state.hasHorsemanship()) {
            CardFaceSymbols.drawAbilitySymbol("horsemanship", g,
                    abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
        }
        if (state.hasHexproof()) {
            if (!state.getHexproofKey().isEmpty()) {
                final String[] splitK = state.getHexproofKey().split(":");
                final List<String> listHK = Arrays.asList(splitK);
                if (listHK.contains("generic")) {
                    CardFaceSymbols.drawAbilitySymbol("hexproof", g,
                            abiX, abiY, abiScale, abiScale);
                    abiY += abiSpace;
                }
                if (listHK.contains("R")) {
                    CardFaceSymbols.drawAbilitySymbol("hexproofR", g,
                            abiX, abiY, abiScale, abiScale);
                    abiY += abiSpace;
                }
                if (listHK.contains("B")) {
                    CardFaceSymbols.drawAbilitySymbol("hexproofB", g,
                            abiX, abiY, abiScale, abiScale);
                    abiY += abiSpace;
                }
                if (listHK.contains("U")) {
                    CardFaceSymbols.drawAbilitySymbol("hexproofU", g,
                            abiX, abiY, abiScale, abiScale);
                    abiY += abiSpace;
                }
                if (listHK.contains("G")) {
                    CardFaceSymbols.drawAbilitySymbol("hexproofG", g,
                            abiX, abiY, abiScale, abiScale);
                    abiY += abiSpace;
                }
                if (listHK.contains("W")) {
                    CardFaceSymbols.drawAbilitySymbol("hexproofW", g,
                            abiX, abiY, abiScale, abiScale);
                    abiY += abiSpace;
                }
                if (listHK.contains("monocolored")) {
                    CardFaceSymbols.drawAbilitySymbol("hexproofC", g,
                            abiX, abiY, abiScale, abiScale);
                    abiY += abiSpace;
                }
            } else {
                CardFaceSymbols.drawAbilitySymbol("hexproof", g,
                        abiX, abiY, abiScale, abiScale);
                abiY += abiSpace;
            }
        } else if (state.hasShroud()) {
            CardFaceSymbols.drawAbilitySymbol("shroud", g,
                    abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
        }
        if (state.hasVigilance()) {
            CardFaceSymbols.drawAbilitySymbol("vigilance", g,
                    abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
        }
        if (state.hasTrample()) {
            CardFaceSymbols.drawAbilitySymbol("trample", g,
                    abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
        }
        if (state.hasReach()) {
            CardFaceSymbols.drawAbilitySymbol("reach", g,
                    abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
        }
        if (state.hasLifelink()) {
            CardFaceSymbols.drawAbilitySymbol("lifelink", g,
                    abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
        }
        if (state.hasWard()) {
            CardFaceSymbols.drawAbilitySymbol("ward", g,
                    abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
        }
        if (state.hasWither()) {
            CardFaceSymbols.drawAbilitySymbol("wither", g,
                    abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
        }
        if (state.hasDefender()) {
            CardFaceSymbols.drawAbilitySymbol("defender", g,
                    abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
        }
        // Protection icons
        if (!state.getProtectionKey().isEmpty()) {
            abiY = drawProtectionIcons(g, state.getProtectionKey(),
                    abiX, abiY, abiScale, abiSpace);
        }
        return abiY;
    }

    /** Draw protection icons matching CardPanel logic. */
    private static int drawProtectionIcons(final Graphics2D g,
                                            final String protKey,
                                            final int abiX, int abiY,
                                            final int abiScale,
                                            final int abiSpace) {
        if (protKey.contains("everything") || protKey.contains("allcolors")) {
            CardFaceSymbols.drawAbilitySymbol("protectAll", g,
                    abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
        } else if (protKey.contains("coloredspells")) {
            CardFaceSymbols.drawAbilitySymbol("protectColoredSpells", g,
                    abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
        } else if (protKey.equals("R")) {
            CardFaceSymbols.drawAbilitySymbol("protectR", g,
                    abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
        } else if (protKey.equals("G")) {
            CardFaceSymbols.drawAbilitySymbol("protectG", g,
                    abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
        } else if (protKey.equals("B")) {
            CardFaceSymbols.drawAbilitySymbol("protectB", g,
                    abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
        } else if (protKey.equals("U")) {
            CardFaceSymbols.drawAbilitySymbol("protectU", g,
                    abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
        } else if (protKey.equals("W")) {
            CardFaceSymbols.drawAbilitySymbol("protectW", g,
                    abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
        } else if (isProtKeyPair(protKey, "R", "G")) {
            CardFaceSymbols.drawAbilitySymbol("protectRG", g,
                    abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
        } else if (isProtKeyPair(protKey, "R", "B")) {
            CardFaceSymbols.drawAbilitySymbol("protectRB", g,
                    abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
        } else if (isProtKeyPair(protKey, "R", "U")) {
            CardFaceSymbols.drawAbilitySymbol("protectRU", g,
                    abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
        } else if (isProtKeyPair(protKey, "R", "W")) {
            CardFaceSymbols.drawAbilitySymbol("protectRW", g,
                    abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
        } else if (isProtKeyPair(protKey, "G", "B")) {
            CardFaceSymbols.drawAbilitySymbol("protectGB", g,
                    abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
        } else if (isProtKeyPair(protKey, "G", "U")) {
            CardFaceSymbols.drawAbilitySymbol("protectGU", g,
                    abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
        } else if (isProtKeyPair(protKey, "G", "W")) {
            CardFaceSymbols.drawAbilitySymbol("protectGW", g,
                    abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
        } else if (isProtKeyPair(protKey, "B", "U")) {
            CardFaceSymbols.drawAbilitySymbol("protectBU", g,
                    abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
        } else if (isProtKeyPair(protKey, "B", "W")) {
            CardFaceSymbols.drawAbilitySymbol("protectBW", g,
                    abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
        } else if (isProtKeyPair(protKey, "U", "W")) {
            CardFaceSymbols.drawAbilitySymbol("protectUW", g,
                    abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
        } else if (protKey.contains("generic") || protKey.length() > 2) {
            CardFaceSymbols.drawAbilitySymbol("protectGeneric", g,
                    abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
        }
        return abiY;
    }

    private static boolean isProtKeyPair(final String key,
                                          final String a, final String b) {
        return key.equals(a + b) || key.equals(b + a);
    }

    /** Draw counter tabs or images onto the card overlay. */
    private static void drawCounterOverlay(final Graphics2D g,
                                            final Map<CounterType, Integer> counters,
                                            final int w, final int h,
                                            final float scale) {
        final ForgeConstants.CounterDisplayType displayType =
                ForgeConstants.CounterDisplayType.from(
                        FModel.getPreferences().getPref(
                                FPref.UI_CARD_COUNTER_DISPLAY_TYPE));
        final boolean atTop = ForgeConstants.CounterDisplayLocation.from(
                FModel.getPreferences().getPref(
                        FPref.UI_CARD_COUNTER_DISPLAY_LOCATION))
                == ForgeConstants.CounterDisplayLocation.TOP;

        // Counter image mode (simple icon)
        if (displayType == ForgeConstants.CounterDisplayType.IMAGE
                || displayType == ForgeConstants.CounterDisplayType.HYBRID) {
            int total = 0;
            for (final int c : counters.values()) {
                total += c;
            }
            if (total > 0) {
                final int symSize = Math.max(24, Math.round(32 * scale));
                final int yCounters = h - h / 3 - symSize;
                final String symName;
                if (total == 1) symName = "counters1";
                else if (total == 2) symName = "counters2";
                else if (total == 3) symName = "counters3";
                else symName = "countersMulti";
                CardFaceSymbols.drawAbilitySymbol(symName, g,
                        -Math.round(15 * scale), yCounters, symSize, symSize);
            }
        }

        // Counter text tabs
        if (displayType == ForgeConstants.CounterDisplayType.TEXT
                || displayType == ForgeConstants.CounterDisplayType.OLD_WHEN_SMALL
                || displayType == ForgeConstants.CounterDisplayType.HYBRID) {
            final Font smallFont = new Font("Dialog", Font.BOLD,
                    Math.max(8, Math.round(9 * scale)));
            final Font largeFont = new Font("Dialog", Font.BOLD,
                    Math.max(9, Math.round(12 * scale)));
            final int titleY = Math.round(h * (54f / 640)) - 15;
            final int spaceFromTop = titleY + Math.round(60 * scale);
            final int boxHeight = Math.round(24 * scale);
            final int boxBaseWidth = Math.round(58 * scale);
            final int boxSpacing = 2;
            int idx = 0;
            for (final Map.Entry<CounterType, Integer> entry : counters.entrySet()) {
                final CounterType ct = entry.getKey();
                final int count = entry.getValue();
                final FontMetrics lgFm = g.getFontMetrics(largeFont);
                final int boxW = boxBaseWidth
                        + lgFm.stringWidth(String.valueOf(count));
                final int yOff = atTop
                        ? spaceFromTop - boxHeight + idx * (boxHeight + boxSpacing)
                        : h - spaceFromTop / 2 - boxHeight + idx * (boxHeight + boxSpacing);

                g.setColor(new Color(0, 0, 0, 200));
                g.fill(new RoundRectangle2D.Float(0, yOff, boxW, boxHeight, 9, 9));
                g.fillRect(0, yOff, 9, boxHeight);

                g.setColor(new Color(ct.getRed(), ct.getGreen(), ct.getBlue(), 180));
                g.setFont(smallFont);
                g.drawString(ct.getCounterOnCardDisplayName(), 8, yOff + boxHeight / 2 + g.getFontMetrics().getAscent() / 2 - 1);
                g.setFont(largeFont);
                g.drawString(String.valueOf(count),
                        Math.round(52 * scale),
                        yOff + boxHeight / 2 + lgFm.getAscent() / 2 - 1);
                idx++;
            }
        }
    }

    /** Draw text with a 1px outline, matching OutlinedLabel's pattern. */
    private static void drawOutlinedText(final Graphics2D g, final String text,
                                          final Font font, final Color fg,
                                          final Color outline,
                                          final int x, final int y) {
        if (text == null || text.isEmpty()) {
            return;
        }
        final AttributedString as = new AttributedString(text);
        as.addAttribute(TextAttribute.FONT, font);
        final TextLayout layout = new TextLayout(
                as.getIterator(), g.getFontRenderContext());

        // Drop shadow: draw twice for a thicker effect
        g.setColor(Color.BLACK);
        g.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, 0.6f));
        layout.draw(g, x + 2, y + 2);
        layout.draw(g, x + 3, y + 3);

        // Outline: draw 4x at 1px offsets with partial alpha
        g.setColor(outline);
        g.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, 0.8f));
        layout.draw(g, x + 1, y - 1);
        layout.draw(g, x + 1, y + 1);
        layout.draw(g, x - 1, y - 1);
        layout.draw(g, x - 1, y + 1);

        // Foreground: full alpha
        g.setColor(fg);
        g.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, 1.0f));
        layout.draw(g, x, y);
    }

    // --- Positioning ---

    private void positionAndShow(final Point cardScreenLocation, final Dimension cardSize) {
        if (!isOwnerFocused()) {
            hidePopup();
            return;
        }

        final int popupWidth = Math.max(window.getWidth(), POPUP_WIDTH);
        final int popupHeight = window.getHeight();

        final Rectangle ownerBounds = getOwnerBounds();

        // Try to position to the right of the card
        int x = cardScreenLocation.x + cardSize.width + GAP;
        int y = cardScreenLocation.y;

        // If it extends past the right edge, position to the left
        if (x + popupWidth > ownerBounds.x + ownerBounds.width) {
            x = cardScreenLocation.x - popupWidth - GAP;
        }

        // If it extends below the owner window, shift up
        if (y + popupHeight > ownerBounds.y + ownerBounds.height) {
            y = ownerBounds.y + ownerBounds.height - popupHeight;
        }

        // Ensure not above the owner window
        if (y < ownerBounds.y) {
            y = ownerBounds.y;
        }

        pendingLocation = new Point(x, y);

        if (window.isVisible()) {
            // Already visible, just reposition immediately
            window.setLocation(pendingLocation);
        } else {
            // Hide any other popup before showing this one
            if (activePopup != null && activePopup != this) {
                activePopup.hidePopup();
            }
            activePopup = this;
            // Start show delay
            showTimer.restart();
        }
    }

    private boolean isOwnerFocused() {
        return owner != null && owner.isActive();
    }

    private Rectangle getOwnerBounds() {
        if (owner != null) {
            return owner.getBounds();
        }
        return GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration().getBounds();
    }

    private static String nullSafe(final String s) {
        return s == null ? "" : s;
    }

    public static class RelatedCardEntry {
        public final String label;
        public final String name;
        public final BufferedImage image;
        public final String imageKey;
        public final boolean placeholder;

        RelatedCardEntry(final String label, final String name,
                         final BufferedImage image, final String imageKey,
                         final boolean placeholder) {
            this.label = label;
            this.name = name;
            this.image = image;
            this.imageKey = imageKey;
            this.placeholder = placeholder;
        }
    }
}
