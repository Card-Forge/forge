package forge.screens.home.rogue;

import forge.deck.Deck;
import forge.deckchooser.FDeckViewer;
import forge.gamemodes.rogue.RogueRun;
import forge.gamemodes.rogue.RogueTutorial;
import forge.item.PaperCard;
import forge.itemmanager.GroupDef;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedPanel;
import forge.localinstance.skin.FSkinProp;
import forge.view.arcane.CardPanel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Dialog for selecting cards from a provided list. Cards are shown face-up in a scrollable grid.
 * The player must select between the provided minimum and maximum counts before confirming.
 */
public class CardSelectionDialog {

  private static final int BASE_CARD_WIDTH = 240;
  private static final int CARD_SPACING = 15;
  private static final int PADDING = 20;
  private static final int CARDS_PER_ROW = 4;
  private static final int HEADER_HEIGHT = 65;

  private final String title;
  private final String subtitle;
  private final RogueRun currentRun;
  private final Function<List<PaperCard>, Deck> buildDeckPreview;
  private final int minSelections;
  private final int maxSelections;
  private final Set<PaperCard> selectedCards;
  private final List<SelectableCardPanel> cardPanels;
  private final GridPanel gridPanel;
  private final FLabel lblTitle;
  private final FLabel lblInfo;
  private FOptionPane optionPane;
  private CardUtil zoomUtil;

  public CardSelectionDialog(String title, String subtitle, List<PaperCard> cards,
      RogueRun currentRun, int minSelections, int maxSelections,
      Function<List<PaperCard>, Deck> buildDeckPreview) {
    this.title = title;
    this.subtitle = subtitle;
    this.currentRun = currentRun;
    this.buildDeckPreview = buildDeckPreview;
    this.minSelections = minSelections;
    this.maxSelections = maxSelections;
    this.selectedCards = new HashSet<>();
    this.cardPanels = new ArrayList<>();

    lblTitle = new FLabel.Builder()
        .text(title)
        .fontSize(16)
        .fontStyle(Font.BOLD)
        .fontAlign(SwingConstants.CENTER)
        .build();

    lblInfo = new FLabel.Builder()
        .text(getInfoText())
        .fontSize(14)
        .fontAlign(SwingConstants.CENTER)
        .build();

    // Sort by type (Deck Editor order), then by CMC
    cards = new ArrayList<>(cards);
    cards.sort(Comparator
        .comparingInt(CardSelectionDialog::typeOrder)
        .thenComparingInt(c -> c.getRules().getManaCost().getCMC()));

    // Grid panel holds all cards with custom layout
    gridPanel = new GridPanel();
    for (PaperCard card : cards) {
      SelectableCardPanel cardPanel = new SelectableCardPanel(card);
      cardPanels.add(cardPanel);
      gridPanel.add(cardPanel);
    }

    // Calculate grid preferred size
    int numRows = (int) Math.ceil(cards.size() / (double) CARDS_PER_ROW);
    int cardHeight = Math.round(BASE_CARD_WIDTH * CardPanel.ASPECT_RATIO);
    int gridWidth = CARDS_PER_ROW * (BASE_CARD_WIDTH + CARD_SPACING) - CARD_SPACING + 2 * PADDING;
    int gridHeight = numRows * (cardHeight + CARD_SPACING) - CARD_SPACING + PADDING;
    gridPanel.setPreferredSize(new Dimension(gridWidth, gridHeight));
  }

  public List<PaperCard> show() {
    // Wrapper panel with header + scroll pane
    SkinnedPanel wrapper = new SkinnedPanel(new BorderLayout());
    wrapper.setOpaque(false);

    // Header
    SkinnedPanel header = new SkinnedPanel(new GridLayout(2, 1));
    header.setOpaque(false);
    header.setPreferredSize(new Dimension(0, HEADER_HEIGHT));
    header.add(lblTitle);
    header.add(lblInfo);
    wrapper.add(header, BorderLayout.NORTH);

    // Scrollable card area
    FScrollPane scrollPane = new FScrollPane(gridPanel, false,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    wrapper.add(scrollPane, BorderLayout.CENTER);

    // Dialog size — fit screen
    GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
        .getDefaultScreenDevice().getDefaultConfiguration();
    Rectangle screenBounds = gc.getBounds();
    Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
    int usableWidth = screenBounds.width - screenInsets.left - screenInsets.right;
    int usableHeight = screenBounds.height - screenInsets.top - screenInsets.bottom;
    int dialogWidth = Math.min((int) (usableWidth * 0.9),
        CARDS_PER_ROW * (BASE_CARD_WIDTH + CARD_SPACING) + 2 * PADDING + 30); // +30 for scrollbar
    int dialogHeight = (int) (usableHeight * 0.85) - 80;

    Dimension dialogSize = new Dimension(dialogWidth, dialogHeight);
    wrapper.setPreferredSize(dialogSize);
    wrapper.setMinimumSize(dialogSize);

    final int OK_OPTION = 0;
    final int VIEW_DECK_OPTION = 1;
    int result;
    do {
      optionPane = new FOptionPane(null, title, null, wrapper, List.of("OK", "View Deck"), 0);
      optionPane.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
      RogueUIHelper.disableSpacebarButtonActivation(optionPane.getContentPane());
      optionPane.getTitleBar().setVisible(false);
      optionPane.setButtonEnabled(OK_OPTION, minSelections == 0
          || selectedCards.size() >= minSelections && selectedCards.size() <= maxSelections);
      optionPane.getButton(VIEW_DECK_OPTION).setIcon(FSkin.getIcon(FSkinProp.ICO_CARD_IMAGE));
      optionPane.getButton(VIEW_DECK_OPTION).setHorizontalTextPosition(SwingConstants.RIGHT);

      zoomUtil = new CardUtil(optionPane);
      zoomUtil.setupZoomOverlay();

      SwingUtilities.invokeLater(() -> RogueTutorialHelper.showIfNotSeen(RogueTutorial.CARD_SELECTION_ZOOM));
      optionPane.setVisible(true);
      result = optionPane.getResult();
      optionPane.dispose();

      if (result == VIEW_DECK_OPTION) {
        showCurrentDeck();
      }
    } while (result == VIEW_DECK_OPTION);

    if (result == OK_OPTION) {
      return new ArrayList<>(selectedCards);
    }
    return new ArrayList<>();
  }

  private void showCurrentDeck() {
    if (buildDeckPreview != null) {
      FDeckViewer.show(buildDeckPreview.apply(List.copyOf(selectedCards)));
    } else if (currentRun != null && currentRun.getCurrentDeck() != null) {
      FDeckViewer.show(currentRun.getCurrentDeck());
    }
  }

  private String getInfoText() {
    if (minSelections == maxSelections) {
      return String.format("%s (%d / %d selected)", subtitle, selectedCards.size(), maxSelections);
    }
    return String.format("%s (%d selected, choose %d to %d)", subtitle, selectedCards.size(),
        minSelections, maxSelections);
  }

  /** Returns type sort index matching Deck Editor order (GroupDef.CARD_TYPE). */
  private static int typeOrder(PaperCard c) {
    int groupIndex = GroupDef.CARD_TYPE.getItemGroupIndex(c);
    return groupIndex < 0 ? 8 : groupIndex;
  }

  /** Grid panel that lays out cards in a fixed-width grid. */
  private class GridPanel extends SkinnedPanel {
    GridPanel() {
      super(null);
      setOpaque(false);
    }

    @Override
    public void doLayout() {
      int totalWidth = getWidth();
      int availableWidth = totalWidth - 2 * PADDING;

      // Scale cards to fit available width
      int desiredRowWidth = CARDS_PER_ROW * (BASE_CARD_WIDTH + CARD_SPACING) - CARD_SPACING;
      float scale = Math.min(1.0f, (float) availableWidth / desiredRowWidth);
      int cardWidth = Math.round(BASE_CARD_WIDTH * scale);
      int cardHeight = Math.round(cardWidth * CardPanel.ASPECT_RATIO);
      int spacing = Math.round(CARD_SPACING * scale);

      int y = PADDING / 2;
      int cardIndex = 0;
      while (cardIndex < cardPanels.size()) {
        int cardsInRow = Math.min(CARDS_PER_ROW, cardPanels.size() - cardIndex);
        int rowWidth = cardsInRow * cardWidth + (cardsInRow - 1) * spacing;
        int x = (totalWidth - rowWidth) / 2;

        for (int col = 0; col < cardsInRow; col++) {
          cardPanels.get(cardIndex).setBounds(x, y, cardWidth, cardHeight);
          x += cardWidth + spacing;
          cardIndex++;
        }
        y += cardHeight + spacing;
      }

      // Update preferred size so scroll pane knows full content height
      setPreferredSize(new Dimension(totalWidth, y + PADDING / 2));
    }
  }

  private class SelectableCardPanel extends SelectableCardPanelBase {
    SelectableCardPanel(PaperCard card) {
      super(card, () -> CardSelectionDialog.this.zoomUtil, false,
          ZoomActivation.MIDDLE_MOUSE_BUTTON);
    }

    @Override
    protected void toggleSelection() {
      if (selectedCards.contains(card)) {
        selectedCards.remove(card);
        setSelected(false);
      } else if (selectedCards.size() < maxSelections) {
        selectedCards.add(card);
        setSelected(true);
      }

      updateInfoLabel();
      // Enable OK only when the current selection is within the allowed range.
      if (optionPane != null) {
        optionPane.setButtonEnabled(0, selectedCards.size() >= minSelections
            && selectedCards.size() <= maxSelections);
      }
    }

    private void updateInfoLabel() {
      lblInfo.setText(getInfoText());
    }
  }
}
