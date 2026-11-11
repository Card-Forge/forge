package forge.screens.home.rogue;

import forge.deckchooser.FDeckViewer;
import forge.gamemodes.rogue.RogueTutorial;
import forge.item.PaperCard;
import forge.localinstance.skin.FSkinProp;
import forge.sound.SoundEffectType;
import forge.sound.SoundSystem;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedPanel;
import forge.util.Localizer;
import forge.view.arcane.CardPanel;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;

/**
 * Dialog for selecting reward cards visually. Displays cards as images and allows selecting up to a
 * maximum number.
 */
public class CardRewardDialog {

  private static final int BASE_CARD_WIDTH = 240;  // Desired card width
  private static final int CARD_SPACING = 15;
  private static final int PADDING = 20;
  private static final int MAX_CARDS_PER_ROW = 4;
  private static final int MAX_ROWS = 2;
  private static final int HEADER_HEIGHT = 65;  // Space for labels (compact)
  private static final int MIN_DIALOG_WIDTH = 900;  // Minimum width for better zoom
  private static final int MIN_DIALOG_HEIGHT = 700; // Minimum height for better zoom

  private final String title;
  private final int maxSelections;
  private final int gold;
  private final String rerollLabel;
  private final boolean rerollEnabled;
  private final Set<PaperCard> selectedCards;
  private final List<SelectableCardPanel> cardPanels;
  private final MainPanel panel;
  private final FLabel lblInfo;
  private final FLabel lblRewards;
  private final FLabel lblGold;
  private FOptionPane optionPane;
  private CardUtil zoomUtil;

  // Computed card dimensions (may be scaled down)
  private int cardWidth;
  private int cardHeight;

  /**
   * Create a card reward selection dialog with optional reroll button.
   *
   * @param title            Dialog title
   * @param cards            List of cards to choose from
   * @param maxSelections    Maximum number of cards to select
   * @param rerollLabel      Label for the reroll button (with cost)
   * @param rerollEnabled    Whether the reroll button is enabled (false if can't afford)
   * @param gold             Player's current gold (displayed in header)
   */
  public CardRewardDialog(String title, List<PaperCard> cards, int maxSelections,
                          String rerollLabel, boolean rerollEnabled, int gold) {
    this.title = title;
    this.maxSelections = maxSelections;
    this.gold = gold;
    this.rerollLabel = rerollLabel;
    this.rerollEnabled = rerollEnabled;
    this.selectedCards = new HashSet<>();
    this.cardPanels = new ArrayList<>();

    // Create rewards label
    lblRewards = new FLabel.Builder()
        .text(getRewardsText())
        .fontSize(16)
        .fontStyle(Font.BOLD)
        .fontAlign(SwingConstants.CENTER)
        .build();

    // Create info label
    lblInfo = new FLabel.Builder()
        .text(getInfoText())
        .fontSize(14)
        .fontAlign(SwingConstants.CENTER)
        .build();

    // Create gold label (top-right, text before icon)
    lblGold = new FLabel.Builder()
        .text("Gold: " + gold)
        .icon(FSkin.getIcon(FSkinProp.ICO_QUEST_COIN))
        .fontSize(14)
        .fontAlign(SwingConstants.RIGHT)
        .build();
    lblGold.setHorizontalTextPosition(SwingConstants.LEFT);

    // Create main panel
    panel = new MainPanel();
    panel.add(lblRewards);
    panel.add(lblInfo);
    panel.add(lblGold);

    // Create card panels
    for (PaperCard card : cards) {
      SelectableCardPanel cardPanel = new SelectableCardPanel(card);
      cardPanels.add(cardPanel);
      panel.add(cardPanel);
    }

    // Calculate layout: max cards per row, max 2 rows
    int cardsPerRow = Math.min(cards.size(), MAX_CARDS_PER_ROW);
    int numRows = Math.min(MAX_ROWS, (int) Math.ceil(cards.size() / (double) cardsPerRow));

    // Get usable screen space (accounts for taskbar and DPI scaling)
    GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
        .getDefaultScreenDevice().getDefaultConfiguration();
    Rectangle screenBounds = gc.getBounds();
    Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
    int usableWidth = screenBounds.width - screenInsets.left - screenInsets.right;
    int usableHeight = screenBounds.height - screenInsets.top - screenInsets.bottom;

    // Reserve space for dialog title bar (~30px) and FOptionPane buttons (~50px)
    int maxDialogWidth = (int) (usableWidth * 0.9);
    int maxDialogHeight = (int) (usableHeight * 0.9) - 80;

    // Calculate desired dimensions at full card size
    int baseCardHeight = Math.round(BASE_CARD_WIDTH * CardPanel.ASPECT_RATIO);
    int desiredWidth = cardsPerRow * (BASE_CARD_WIDTH + CARD_SPACING) - CARD_SPACING + 2 * PADDING;
    int desiredHeight =
        numRows * (baseCardHeight + CARD_SPACING) - CARD_SPACING + HEADER_HEIGHT + PADDING;

    // Dialog size: apply minimum, then cap to screen bounds
    int dialogWidth = Math.min(Math.max(desiredWidth, MIN_DIALOG_WIDTH), maxDialogWidth);
    int dialogHeight = Math.min(Math.max(desiredHeight, MIN_DIALOG_HEIGHT), maxDialogHeight);

    // Initialize card dimensions (doLayout will recalculate based on actual size)
    cardWidth = BASE_CARD_WIDTH;
    cardHeight = baseCardHeight;

    Dimension dialogSize = new Dimension(dialogWidth, dialogHeight);
    panel.setPreferredSize(dialogSize);
    panel.setMinimumSize(dialogSize);
    panel.setSize(dialogSize);
  }

  private void revealAllCards() {
    final int[] revealIndex = {0};
    Timer revealTimer = new Timer(100, e -> {
      if (revealIndex[0] < cardPanels.size()) {
        if (revealIndex[0] == 0 && cardPanels.get(0).faceDown) {
          SoundSystem.instance.play(SoundEffectType.FlipCard,
              SoundEffectType.FlipCard.isSynced());
        }
        cardPanels.get(revealIndex[0]).flip();
        revealIndex[0]++;
      } else {
        ((Timer) e.getSource()).stop();
        RogueTutorialHelper.showIfNotSeen(RogueTutorial.CARD_REWARDS);
      }
    });
    revealTimer.start();
  }

  private String getRewardsText() {
    return String.format("Victory! %s.", title);
  }

  /**
   * Show the dialog and return the selected cards.
   * Returns null if the reroll button was clicked (only when constructed with a rerollButtonLabel).
   *
   * @return List of selected cards, null if reroll was clicked, or empty list if canceled
   */
  public List<PaperCard> show() {
    final Localizer localizer = Localizer.getInstance();

    // Build button list: [OK, (optional Reroll), View Deck]
    final boolean showReroll = rerollLabel != null;
    final int REROLL_OPTION = showReroll ? 1 : -1;
    final int VIEW_DECK_OPTION = showReroll ? 2 : 1;
    final List<String> buttons = new ArrayList<>();
    buttons.add(localizer.getMessage("lblOK"));
    if (showReroll) {
      buttons.add(rerollLabel);
    }
    buttons.add("View Deck");

    // Cache coin icon for reroll button
    final javax.swing.Icon coinIcon = createCoinIcon();

    int result;
    do {
      optionPane = new FOptionPane(
          null,
          "Card Rewards",
          null,
          panel,
          buttons,
          0
      );
      optionPane.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
      RogueUIHelper.disableSpacebarButtonActivation(optionPane.getContentPane());

      optionPane.getButton(VIEW_DECK_OPTION).setIcon(FSkin.getIcon(FSkinProp.ICO_CARD_IMAGE));
      optionPane.getButton(VIEW_DECK_OPTION).setHorizontalTextPosition(SwingConstants.RIGHT);

      // Set coin icon on reroll button and enable/disable
      if (showReroll) {
        optionPane.getButton(REROLL_OPTION).setIcon(coinIcon);
        optionPane.getButton(REROLL_OPTION).setHorizontalTextPosition(SwingConstants.LEFT);
        optionPane.getButton(REROLL_OPTION).setEnabled(rerollEnabled);
      }

      // Setup zoom utility
      zoomUtil = new CardUtil(optionPane);
      zoomUtil.setupZoomOverlay();

      panel.revalidate();
      panel.repaint();

      // Automatically reveal all cards after dialog is shown
      // Use invokeLater to ensure it runs after the dialog is displayed
      SwingUtilities.invokeLater(() -> {
        Timer revealTimer = new Timer(200, e -> {
          revealAllCards();
          ((Timer) e.getSource()).stop();
        });
        revealTimer.setRepeats(false);
        revealTimer.start();
      });

      optionPane.setVisible(true);
      result = optionPane.getResult();
      optionPane.dispose();

      // If View Deck clicked, show deck and re-display dialog
      if (result == VIEW_DECK_OPTION) {
        showCurrentDeck();
      }
    } while (result == VIEW_DECK_OPTION);

    if (result == 0) {
      return new ArrayList<>(selectedCards);
    }
    if (showReroll && result == REROLL_OPTION) {
      return null; // Reroll signal
    }
    return new ArrayList<>(); // Cancel
  }


  private void updateInfoLabel() {
    lblInfo.setText(getInfoText());
  }

  private String getInfoText() {
    return String.format("Select up to %d cards (%d selected)", maxSelections,
        selectedCards.size());
  }

  private static javax.swing.Icon createCoinIcon() {
    final javax.swing.Icon raw = FSkin.getImage(FSkinProp.ICO_QUEST_COIN).resize(16, 16).getIcon();
    return new javax.swing.Icon() {
      public int getIconWidth()  { return raw.getIconWidth(); }
      public int getIconHeight() { return raw.getIconHeight(); }
      public void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {
        raw.paintIcon(c, g, x, y - 2);
      }
    };
  }

  private void showCurrentDeck() {
    var currentRun = CSubmenuRogueMap.SINGLETON_INSTANCE.getCurrentRun();
    if (currentRun != null && currentRun.getCurrentDeck() != null) {
      FDeckViewer.show(currentRun.getCurrentDeck());
    }
  }

  private void toggleCardSelection(SelectableCardPanel cardPanel) {
    PaperCard card = cardPanel.card;

    if (selectedCards.contains(card)) {
      // Deselect
      selectedCards.remove(card);
      cardPanel.setSelected(false);
    } else if (selectedCards.size() < maxSelections) {
      // Select (if under limit)
      selectedCards.add(card);
      cardPanel.setSelected(true);
    }

    updateInfoLabel();
  }

  private class MainPanel extends SkinnedPanel {

    private MainPanel() {
      super(null);
      setOpaque(false);
    }

    @Override
    public void doLayout() {
      int totalWidth = getWidth();
      int totalHeight = getHeight();

      int y = PADDING;

      // Layout rewards label (compact)
      lblRewards.setBounds(PADDING, y, totalWidth - 2 * PADDING, 28);

      // Layout gold label (top-right, same row as rewards)
      lblGold.setBounds(totalWidth - PADDING - 150, y, 150, 28);
      y += 28 + 3;

      // Layout info label (compact)
      lblInfo.setBounds(PADDING, y, totalWidth - 2 * PADDING, 22);
      y += 22 + 5;

      // Calculate available space for cards
      int availableWidth = totalWidth - 2 * PADDING;
      int availableHeight = totalHeight - y - 10; // small bottom margin

      // Calculate cards per row and number of rows
      int cardsPerRow = Math.min(MAX_CARDS_PER_ROW, cardPanels.size());
      int numRows = Math.min(MAX_ROWS, (int) Math.ceil(cardPanels.size() / (double) cardsPerRow));

      // Calculate scale to fit cards in available space
      int baseCardHeight = Math.round(BASE_CARD_WIDTH * CardPanel.ASPECT_RATIO);
      int desiredWidth = cardsPerRow * (BASE_CARD_WIDTH + CARD_SPACING) - CARD_SPACING;
      int desiredHeight = numRows * (baseCardHeight + CARD_SPACING) - CARD_SPACING;

      float widthScale =
          availableWidth > 0 ? Math.min(1.0f, (float) availableWidth / desiredWidth) : 1.0f;
      float heightScale =
          availableHeight > 0 ? Math.min(1.0f, (float) availableHeight / desiredHeight) : 1.0f;
      float scale = Math.min(widthScale, heightScale);

      // Apply scale to card dimensions
      cardWidth = Math.round(BASE_CARD_WIDTH * scale);
      cardHeight = Math.round(cardWidth * CardPanel.ASPECT_RATIO);

      // Layout card panels (top-aligned after header)
      int cardIndex = 0;
      int cardY = y;
      for (int row = 0; cardIndex < cardPanels.size() && row < MAX_ROWS; row++) {
        int cardsInThisRow = Math.min(cardsPerRow, cardPanels.size() - cardIndex);
        int rowWidth = cardsInThisRow * cardWidth + (cardsInThisRow - 1) * CARD_SPACING;
        int startX = (totalWidth - rowWidth) / 2;

        int x = startX;
        for (int col = 0; col < cardsInThisRow; col++) {
          SelectableCardPanel cardPanel = cardPanels.get(cardIndex);
          cardPanel.setBounds(x, cardY, cardWidth, cardHeight);
          x += cardWidth + CARD_SPACING;
          cardIndex++;
        }

        cardY += cardHeight + CARD_SPACING;
      }
    }
  }

  private class SelectableCardPanel extends SelectableCardPanelBase {

    private SelectableCardPanel(PaperCard card) {
      super(card, () -> CardRewardDialog.this.zoomUtil, true, ZoomActivation.MOUSE_WHEEL);
    }

    @Override
    protected void toggleSelection() {
      toggleCardSelection(this);
    }
  }
}
