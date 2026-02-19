package forge.view.arcane;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.Graphics2D;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
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

import forge.CachedCardImage;
import forge.ImageCache;
import forge.StaticData;
import forge.card.CardRules;
import forge.card.CardSplitType;
import forge.card.ICardFace;
import forge.item.PaperCard;
import forge.item.PaperToken;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.game.keyword.Keyword;
import forge.game.keyword.KeywordInterface;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.toolbox.FSkin;
import forge.toolbox.imaging.FImagePanel;
import forge.toolbox.imaging.FImageUtil;

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

    // Dark overlay colors (Arena-style)
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
    private String cachedCardImageKey = "";

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

        // Content panel (CENTER side — related cards on left, keywords on right)
        contentPanel = new JPanel(new BorderLayout(GAP, 0));
        contentPanel.setOpaque(false);

        keywordsPanel = new JPanel();
        keywordsPanel.setLayout(new BoxLayout(keywordsPanel, BoxLayout.Y_AXIS));
        keywordsPanel.setOpaque(false);

        relatedCardsPanel = new JPanel();
        relatedCardsPanel.setLayout(new BoxLayout(relatedCardsPanel, BoxLayout.Y_AXIS));
        relatedCardsPanel.setOpaque(false);

        // Related cards top-aligned on left, keywords fill remaining space on right
        final JPanel relatedWrapper = new JPanel(new BorderLayout());
        relatedWrapper.setOpaque(false);
        relatedWrapper.add(relatedCardsPanel, BorderLayout.NORTH);
        contentPanel.add(relatedWrapper, BorderLayout.WEST);
        contentPanel.add(keywordsPanel, BorderLayout.CENTER);

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
        return Math.max(100, Math.min(500, value));
    }

    /**
     * Show the popup for the given card, displaying keyword explanations and/or
     * related card images based on the toggle flags.
     */
    public void showForCard(final CardView cardView, final Point cardScreenLocation,
                            final Dimension cardSize, final boolean showKeywords,
                            final boolean showRelatedCards, final boolean showCardImage) {
        if (cardView == null) {
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

        // Check cache — skip rebuilding if same content and settings
        if (keywordKey.equals(cachedKeywordKey) && cardName.equals(cachedCardName)
                && thumbnailHeight == cachedImageSize
                && showCardImage == cachedShowCardImage
                && cardImageKey.equals(cachedCardImageKey)) {
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
        if (showKeywords && !keywordKey.isEmpty()) {
            keywordList = buildKeywords(keywordKey);
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
            cachedCardImageKey = cardImageKey;
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

        // Update UI
        keywordsPanel.setVisible(hasKeywords);
        keywordsPanel.removeAll();
        final int kwMaxWidth = Math.max((int) (thumbnailHeight * MTG_ASPECT_RATIO), POPUP_WIDTH);
        if (hasKeywords) {
            populateKeywords(keywordsPanel, keywordList, kwMaxWidth);
        }

        relatedCardsPanel.setVisible(hasRelated);
        relatedCardsPanel.removeAll();
        if (hasRelated) {
            populateRelatedCards(relatedEntries, thumbnailHeight, maxContentWidth);
        }

        // Update cache
        cachedKeywordKey = keywordKey;
        cachedCardName = cardName;
        cachedHasKeywords = hasKeywords;
        cachedHasRelated = hasRelated;
        cachedImageSize = thumbnailHeight;
        cachedShowCardImage = showCardImage;
        cachedCardImageKey = cardImageKey;

        // Defer pack/show to let layout complete
        final Point loc = cardScreenLocation;
        final Dimension size = cardSize;
        final int finalMaxPopup = Math.max(maxPopupWidth, POPUP_WIDTH);
        SwingUtilities.invokeLater(() -> {
            if (keywordsPanel.isVisible()) {
                keywordsPanel.setPreferredSize(new Dimension(kwMaxWidth,
                        keywordsPanel.getPreferredSize().height));
            }
            window.pack();
            if (window.getWidth() > finalMaxPopup) {
                window.setSize(finalMaxPopup, window.getHeight());
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

    // --- Keyword building ---

    private static class KeywordData {
        final String name;
        final String reminderHtml; // already encoded with FSkin.encodeSymbols

        KeywordData(final String name, final String reminderHtml) {
            this.name = name;
            this.reminderHtml = reminderHtml;
        }
    }

    private static List<KeywordData> buildKeywords(final String keywordKey) {
        final String[] tokens = keywordKey.split(",");
        final LinkedHashMap<Keyword, String> keywordMap = new LinkedHashMap<>();

        for (final String token : tokens) {
            if (token.isEmpty()) {
                continue;
            }
            try {
                final KeywordInterface inst = Keyword.getInstance(token);
                final Keyword kw = inst.getKeyword();
                if (kw == Keyword.UNDEFINED) {
                    continue;
                }
                if (keywordMap.containsKey(kw)) {
                    continue; // deduplicate
                }
                String reminderText;
                try {
                    reminderText = inst.getReminderText();
                } catch (Exception ex) {
                    reminderText = "";
                }
                keywordMap.put(kw, reminderText);
            } catch (Exception e) {
                // Skip malformed keyword tokens
            }
        }

        final List<KeywordData> result = new ArrayList<>();
        for (final Map.Entry<Keyword, String> entry : keywordMap.entrySet()) {
            String reminder = entry.getValue();
            if (!reminder.isEmpty()) {
                reminder = FSkin.encodeSymbols(reminder, false);
            }
            result.add(new KeywordData(entry.getKey().toString(), reminder));
        }
        return result;
    }

    /** Creates a JLabel that uses grayscale AA (LCD AA breaks on transparent windows). */
    private static javax.swing.JLabel createAALabel(final String text) {
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

    private static JPanel createPillPanel() {
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

    private static void populateKeywords(final JPanel panel,
                                          final List<KeywordData> keywords,
                                          final int maxWidth) {
        final FSkin.SkinFont boldFont = FSkin.getBoldFont(12);
        final FSkin.SkinFont reminderFont = FSkin.getFont(11);
        final int textWidth = maxWidth - 2 * PILL_PAD - 2; // account for pill border+pad

        for (int i = 0; i < keywords.size(); i++) {
            if (i > 0) {
                panel.add(javax.swing.Box.createRigidArea(new Dimension(0, 4)));
            }
            final KeywordData kw = keywords.get(i);

            final JPanel pill = createPillPanel();

            // Keyword name label
            final javax.swing.JLabel nameLabel = createAALabel(kw.name);
            nameLabel.setFont(boldFont.getBaseFont());
            nameLabel.setForeground(TEXT_PRIMARY);
            nameLabel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
            nameLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                    nameLabel.getPreferredSize().height));
            pill.add(nameLabel);

            // Reminder text
            if (!kw.reminderHtml.isEmpty()) {
                final javax.swing.JLabel reminderLabel = createAALabel(
                        kw.reminderHtml);
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

    private List<RelatedCardEntry> buildRelatedCards(final String cardName,
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

            // Tokens
            final List<String> tokenNames = rules.getTokens();
            if (tokenNames != null && !tokenNames.isEmpty()) {
                for (final String tokenName : tokenNames) {
                    final PaperToken pt = data.getAllTokens().getToken(tokenName);
                    if (pt != null) {
                        final String imageKey = pt.getCardImageKey();
                        final BufferedImage img = ImageCache.getOriginalImage(
                                imageKey, true, null);
                        if (img != null) {
                            entries.add(new RelatedCardEntry("Creates", pt.getName(), img));
                        }
                        if (img == null || ImageCache.isDefaultImage(img)) {
                            fetchIfMissing(imageKey);
                        }
                    }
                }
            }

            // Other faces based on split type
            final CardSplitType splitType = rules.getSplitType();
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
        } catch (Exception e) {
            // Guard against any lookup failures
        }

        return entries;
    }

    private void addOtherFaceEntry(final List<RelatedCardEntry> entries,
                                    final CardView cardView, final String label) {
        final CardStateView altState = cardView.getAlternateState();
        if (altState == null) {
            return;
        }
        final BufferedImage img = FImageUtil.getImage(altState);
        if (img != null) {
            entries.add(new RelatedCardEntry(label, altState.getName(), img));
        }
        if (img == null || ImageCache.isDefaultImage(img)) {
            fetchIfMissing(altState.getImageKey());
        }
    }

    private void addFlipFaceEntry(final List<RelatedCardEntry> entries,
                                    final CardView cardView) {
        final CardStateView altState = cardView.getAlternateState();
        if (altState == null) {
            return;
        }
        final BufferedImage img = FImageUtil.getImage(altState);
        if (img != null) {
            final BufferedImage displayImg = ImageCache.isDefaultImage(img)
                    ? img : rotateImage180(img);
            entries.add(new RelatedCardEntry("Flips Into", altState.getName(), displayImg));
        }
        if (img == null || ImageCache.isDefaultImage(img)) {
            fetchIfMissing(altState.getImageKey());
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

    private void addSpecializeFaces(final List<RelatedCardEntry> entries,
                                     final CardRules rules, final String cardName,
                                     final StaticData data) {
        final Map<?, ICardFace> specParts = rules.getSpecializeParts();
        if (specParts == null || specParts.isEmpty()) {
            return;
        }
        for (final ICardFace face : specParts.values()) {
            try {
                final String faceName = face.getName();
                final CardRules faceRules = data.getCommonCards().getRules(faceName);
                if (faceRules != null) {
                    final forge.item.PaperCard pc = data.getCommonCards().getCard(faceName);
                    if (pc != null) {
                        final String imageKey = pc.getCardImageKey();
                        final BufferedImage img = ImageCache.getOriginalImage(
                                imageKey, true, null);
                        if (img != null) {
                            entries.add(new RelatedCardEntry("Specializes Into", faceName, img));
                        }
                        if (img == null || ImageCache.isDefaultImage(img)) {
                            // Use prefixed key for fetcher
                            fetchIfMissing(pc.getImageKey(false));
                        }
                    }
                }
            } catch (Exception e) {
                // Skip faces that can't be resolved
            }
        }
    }

    private void addSpellbookEntries(final List<RelatedCardEntry> entries,
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
                    final String imageKey = pc.getCardImageKey();
                    final BufferedImage img = ImageCache.getOriginalImage(
                            imageKey, true, null);
                    if (img != null) {
                        entries.add(new RelatedCardEntry("Spellbook", cardName, img));
                    }
                    if (img == null || ImageCache.isDefaultImage(img)) {
                        // Use prefixed key for fetcher (getCardImageKey returns path format)
                        fetchIfMissing(pc.getImageKey(false));
                    }
                }
            } catch (Exception e) {
                // Skip cards that can't be resolved
            }
        }
    }

    private void addNamedCardEntry(final List<RelatedCardEntry> entries,
                                    final String name, final String label,
                                    final StaticData data) {
        if (name == null || name.isEmpty()) {
            return;
        }
        try {
            final PaperCard pc = data.getCommonCards().getCard(name);
            if (pc != null) {
                final String imageKey = pc.getCardImageKey();
                final BufferedImage img = ImageCache.getOriginalImage(imageKey, true, null);
                if (img != null) {
                    entries.add(new RelatedCardEntry(label, name, img));
                }
                if (img == null || ImageCache.isDefaultImage(img)) {
                    fetchIfMissing(pc.getImageKey(false));
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

    private static final int FULL_SIZE_MAX = 2;
    private static final int HALF_SIZE_PER_ROW = 4;

    private void populateRelatedCards(final List<RelatedCardEntry> entries,
                                      final int thumbnailHeight,
                                      final int maxContentWidth) {
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
                relatedCardsPanel.add(javax.swing.Box.createRigidArea(
                        new Dimension(0, 4)));
            }
            firstGroup = false;

            // Wrap each section group in a pill
            final JPanel pill = createPillPanel();

            // Section label
            final String labelText = group.getKey()
                    + (cards.size() > HALF_SIZE_PER_ROW
                            ? " (" + cards.size() + ")" : "");
            final javax.swing.JLabel sectionLabel = createAALabel(labelText);
            sectionLabel.setFont(boldFont.getBaseFont());
            sectionLabel.setForeground(TEXT_PRIMARY);
            sectionLabel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
            sectionLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                    sectionLabel.getPreferredSize().height));
            pill.add(sectionLabel);
            pill.add(javax.swing.Box.createRigidArea(new Dimension(0, 4)));

            // Up to 2 cards at full size; 3+ scale to half size, max 4 per row
            final boolean fullSize = cards.size() <= FULL_SIZE_MAX;
            final int perRow = fullSize
                    ? Math.min(cards.size(), FULL_SIZE_MAX) : HALF_SIZE_PER_ROW;
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
            for (final RelatedCardEntry entry : cards) {
                if (colIndex == 0) {
                    currentRow = new JPanel();
                    currentRow.setLayout(new BoxLayout(currentRow, BoxLayout.X_AXIS));
                    currentRow.setOpaque(false);
                    currentRow.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
                    pill.add(currentRow);
                }
                final FImagePanel imgPanel = new FImagePanel();
                imgPanel.setImage(entry.image);
                int thumbWidth = (int) (effectiveHeight * MTG_ASPECT_RATIO);
                int thumbHeight = effectiveHeight;
                if (thumbWidth > maxThumbWidth) {
                    thumbWidth = maxThumbWidth;
                    thumbHeight = (int) (thumbWidth / MTG_ASPECT_RATIO);
                }
                final Dimension thumbSize = new Dimension(thumbWidth, thumbHeight);
                imgPanel.setPreferredSize(thumbSize);
                imgPanel.setMinimumSize(thumbSize);
                imgPanel.setMaximumSize(thumbSize);
                currentRow.add(imgPanel);
                colIndex++;
                if (colIndex >= perRow) {
                    colIndex = 0;
                }
            }

            pill.setMaximumSize(new Dimension(maxContentWidth, Integer.MAX_VALUE));
            relatedCardsPanel.add(pill);
        }
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

    private static class RelatedCardEntry {
        final String label;
        final String name;
        final BufferedImage image;

        RelatedCardEntry(final String label, final String name, final BufferedImage image) {
            this.label = label;
            this.name = name;
            this.image = image;
        }
    }
}
