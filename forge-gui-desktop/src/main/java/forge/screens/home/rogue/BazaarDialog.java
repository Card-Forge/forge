package forge.screens.home.rogue;

import forge.deckchooser.FDeckViewer;
import forge.gamemodes.rogue.BazaarPricing;
import forge.gamemodes.rogue.RogueRun;
import forge.gamemodes.rogue.effect.BazaarItem;
import forge.localinstance.skin.FSkinProp;
import forge.sound.SoundEffectType;
import forge.sound.SoundSystem;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedPanel;
import forge.toolbox.FTabbedPane;
import forge.util.Localizer;
import forge.view.arcane.CardPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import net.miginfocom.swing.MigLayout;

/**
 * Dialog for Bazaar-style shopping interactions.
 * Allows the player to purchase cards using gold based on rarity pricing.
 */
public class BazaarDialog {
  static final int MAX_DISPLAY_CARDS = 10;

  private static final int BASE_CARD_WIDTH = 240;
  private static final int PRICE_LABEL_HEIGHT = 40;
  private static final int CARD_SPACING = 10;
  private static final int MAX_CARDS_PER_ROW = 5;
  private static final int MAX_ROWS = 2;
  private static final int HEADER_HEIGHT = 95;
  private static final int TABBED_HEADER_HEIGHT = 30;

  private final MainPanel panel;
  private CardUtil zoomUtil;
  private FOptionPane optionPane;
  private final List<BazaarItem> availableItems;
  private final List<BazaarItem> cardItems = new ArrayList<>();
  private final List<BazaarItem> specialItems = new ArrayList<>();
  private final RogueRun run;
  private final int availableGold;
  private final String dialogTitle;
  private final String rerollButtonLabel;
  private int selectedTabIndex;
  private boolean rerollEnabled = true;
  private final Set<BazaarItem> selectedItems = new HashSet<>();
  private final FLabel lblGold;
  private final FLabel lblCost;
  private final FLabel lblRemaining;

  private int cardWidth;
  private int cardImageHeight;
  private int cardHeight;
  private int priceLabelHeight;

  public BazaarDialog(List<BazaarItem> items, RogueRun run, int gold, String title,
                      String rerollButtonLabel,
                      int selectedTabIndex) {
    this.availableItems = new ArrayList<>(items);
    this.run = run;
    this.availableGold = gold;
    this.dialogTitle = title != null ? title : Localizer.getInstance().getMessage("lblRogueBazaar");
    this.rerollButtonLabel = rerollButtonLabel;
    this.selectedTabIndex = selectedTabIndex;
    splitItems();

    panel = new MainPanel();

    FLabel lblTitle = new FLabel.Builder()
        .text(dialogTitle)
        .fontSize(20)
        .fontStyle(Font.BOLD)
        .fontAlign(SwingConstants.CENTER)
        .build();

    lblGold = new FLabel.Builder()
        .text(Localizer.getInstance().getMessage("lblRogueGoldTotal", availableGold))
        .icon(FSkin.getIcon(FSkinProp.ICO_QUEST_COIN))
        .fontSize(14)
        .fontAlign(SwingConstants.RIGHT)
        .build();
    lblGold.setHorizontalTextPosition(SwingConstants.LEFT);

    lblCost = new FLabel.Builder()
        .text(Localizer.getInstance().getMessage("lblRogueBazaarCost", 0))
        .icon(FSkin.getIcon(FSkinProp.ICO_QUEST_COIN))
        .fontSize(14)
        .fontAlign(SwingConstants.RIGHT)
        .build();
    lblCost.setHorizontalTextPosition(SwingConstants.LEFT);

    SeparatorLine separator = new SeparatorLine();
    lblRemaining = new FLabel.Builder()
        .text(Localizer.getInstance().getMessage("lblRogueRemainingGold", availableGold))
        .icon(FSkin.getIcon(FSkinProp.ICO_QUEST_COIN))
        .fontSize(14)
        .fontStyle(Font.BOLD)
        .fontAlign(SwingConstants.RIGHT)
        .build();
    lblRemaining.setHorizontalTextPosition(SwingConstants.LEFT);

    FLabel lblDescription = new FLabel.Builder()
        .text(specialItems.isEmpty()
            ? Localizer.getInstance().getMessage("lblRogueSelectCardsToBuy")
            : Localizer.getInstance().getMessage("lblRogueSelectOffersToBuy"))
        .fontSize(12)
        .fontAlign(SwingConstants.CENTER)
        .build();

    panel.add(lblTitle, "w 100%!, h 28px!, ax center, wrap");
    panel.add(lblGold, "pos (100%-160) 10, w 150!, h 20px!");
    panel.add(lblCost, "pos (100%-160) 30, w 150!, h 20px!");
    panel.add(separator, "pos (100%-155) 50, w 145!, h 1px!");
    panel.add(lblRemaining, "pos (100%-160) 53, w 150!, h 20px!");
    panel.add(lblDescription, "w 100%!, h 20px!, ax center, gap 0 0 5px 10px, wrap");
    panel.setupShopPanels();

    int cardsPerRow = getMaxCardsPerRow();
    int numRows = getMaxRowCount();

    GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
        .getDefaultScreenDevice().getDefaultConfiguration();
    Rectangle screenBounds = gc.getBounds();
    Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
    int usableWidth = screenBounds.width - screenInsets.left - screenInsets.right;
    int usableHeight = screenBounds.height - screenInsets.top - screenInsets.bottom;

    int maxDialogWidth = (int) (usableWidth * 0.9);
    int maxDialogHeight = (int) (usableHeight * 0.9) - 80;

    int baseCardImageHeight = Math.round(BASE_CARD_WIDTH * CardPanel.ASPECT_RATIO);
    int baseCardHeight = baseCardImageHeight + PRICE_LABEL_HEIGHT;
    int desiredWidth = cardsPerRow * (BASE_CARD_WIDTH + CARD_SPACING) - CARD_SPACING + 40;
    int desiredHeight = numRows * (baseCardHeight + 15) - 15 + HEADER_HEIGHT + 15;
    if (!specialItems.isEmpty()) {
      desiredHeight += TABBED_HEADER_HEIGHT;
    }

    int dialogWidth = Math.min(desiredWidth, maxDialogWidth);
    int dialogHeight = Math.min(desiredHeight, maxDialogHeight);

    cardWidth = BASE_CARD_WIDTH;
    cardImageHeight = baseCardImageHeight;
    priceLabelHeight = PRICE_LABEL_HEIGHT;
    cardHeight = baseCardHeight;

    Dimension dialogSize = new Dimension(dialogWidth, dialogHeight);
    panel.setPreferredSize(dialogSize);
    panel.setMinimumSize(dialogSize);
    panel.setSize(dialogSize);
  }

  public void setRerollEnabled(boolean enabled) {
    this.rerollEnabled = enabled;
  }

  public int getSelectedTabIndex() {
    return selectedTabIndex;
  }

  private void splitItems() {
    for (BazaarItem item : availableItems) {
      if (item.type() == BazaarItem.Type.TRAIT || item.type() == BazaarItem.Type.CARRY_CARD) {
        specialItems.add(item);
      } else {
        cardItems.add(item);
      }
    }
  }

  private int getMaxCardsPerRow() {
    int maxCardsPerRow = Math.min(cardItems.size(), MAX_CARDS_PER_ROW);
    int traitCount = 0;
    int carryCardCount = 0;
    for (BazaarItem item : specialItems) {
      if (item.type() == BazaarItem.Type.TRAIT) {
        traitCount++;
      } else if (item.type() == BazaarItem.Type.CARRY_CARD) {
        carryCardCount++;
      }
    }

    maxCardsPerRow = Math.max(maxCardsPerRow, Math.min(MAX_CARDS_PER_ROW, traitCount));
    maxCardsPerRow = Math.max(maxCardsPerRow, Math.min(MAX_CARDS_PER_ROW, carryCardCount));
    return Math.max(1, maxCardsPerRow);
  }

  private int getMaxRowCount() {
    int cardRows = Math.min(MAX_ROWS,
        (int) Math.ceil(cardItems.size() / (double) MAX_CARDS_PER_ROW));
    boolean hasTraits = false;
    boolean hasCarryCards = false;
    for (BazaarItem item : specialItems) {
      if (item.type() == BazaarItem.Type.TRAIT) {
        hasTraits = true;
      } else if (item.type() == BazaarItem.Type.CARRY_CARD) {
        hasCarryCards = true;
      }
    }

    int specialRows = 0;
    if (hasCarryCards) {
      specialRows = MAX_ROWS;
    } else if (hasTraits) {
      specialRows = 1;
    }
    return Math.max(1, Math.max(cardRows, specialRows));
  }

  public Set<BazaarItem> show() {
    final Localizer localizer = Localizer.getInstance();
    SoundSystem.instance.play(SoundEffectType.CoinsDrop,
        SoundEffectType.CoinsDrop.isSynced());

    final int BUY_OPTION = 0;
    final boolean showReroll = rerollButtonLabel != null;
    final int REROLL_OPTION = showReroll ? 1 : -1;
    final int VIEW_DECK_OPTION = showReroll ? 2 : 1;
    final int VIEW_MAP_OPTION = showReroll ? 3 : 2;
    final int SKIP_OPTION = showReroll ? 4 : 3;
    final List<String> buttons = new ArrayList<>();
    buttons.add(specialItems.isEmpty() ? localizer.getMessage("lblRogueBuySelectedCards") : localizer.getMessage("lblRogueBuySelected"));
    if (showReroll) {
      buttons.add(rerollButtonLabel);
    }
    buttons.add(Localizer.getInstance().getMessage("lblViewDeck"));
    buttons.add(localizer.getMessage("lblRogueViewMap"));
    buttons.add(localizer.getMessage("lblSkip"));

    final javax.swing.Icon coinIcon = createCoinIcon();

    int result;
    do {
      optionPane = new FOptionPane(
          null,
          dialogTitle,
          null,
          panel,
          buttons,
          SKIP_OPTION
      );
      optionPane.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
      RogueUIHelper.disableSpacebarButtonActivation(optionPane.getContentPane());

      optionPane.getButton(VIEW_DECK_OPTION).setIcon(FSkin.getIcon(FSkinProp.ICO_CARD_IMAGE));
      optionPane.getButton(VIEW_DECK_OPTION).setHorizontalTextPosition(SwingConstants.RIGHT);
      optionPane.getButton(VIEW_MAP_OPTION).setIcon(
          FSkin.getImage(FSkinProp.ICO_QUEST_MAP).resize(24, 24).getIcon());
      optionPane.getButton(VIEW_MAP_OPTION).setHorizontalTextPosition(SwingConstants.RIGHT);

      if (showReroll) {
        optionPane.getButton(REROLL_OPTION).setIcon(coinIcon);
        optionPane.getButton(REROLL_OPTION).setHorizontalTextPosition(SwingConstants.LEFT);
        optionPane.getButton(REROLL_OPTION).setEnabled(rerollEnabled);
      }

      optionPane.getButton(BUY_OPTION).setEnabled(!selectedItems.isEmpty());

      zoomUtil = new CardUtil(optionPane);
      zoomUtil.setupZoomOverlay();

      panel.startRevealAnimation();

      panel.revalidate();
      panel.repaint();

      optionPane.setVisible(true);
      result = optionPane.getResult();
      selectedTabIndex = panel.getSelectedTabIndex();
      optionPane.dispose();

      if (result == VIEW_DECK_OPTION) {
        showCurrentDeck();
      } else if (result == VIEW_MAP_OPTION) {
        RogueMapDialog.show(run);
      }
    } while (result == VIEW_DECK_OPTION || result == VIEW_MAP_OPTION);

    if (result == BUY_OPTION) {
      return selectedItems;
    }
    if (showReroll && result == REROLL_OPTION) {
      return null;
    }
    return new HashSet<>();
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

  private static class SeparatorLine extends javax.swing.JPanel {
    SeparatorLine() {
      setOpaque(true);
      setBackground(Color.GRAY);
    }
  }

  private void showCurrentDeck() {
    if (run != null && run.getCurrentDeck() != null) {
      FDeckViewer.show(run.getCurrentDeck());
    }
  }

  private class MainPanel extends SkinnedPanel {

    private final List<SelectableCardPanel> cardPanels = new ArrayList<>();
    private ShopGridPanel cardGridPanel;
    private ShopGridPanel specialGridPanel;
    private FTabbedPane tabbedPane;
    private Timer revealTimer;
    private int revealIndex = 0;

    private MainPanel() {
      super(new MigLayout("insets 10, gap 0, wrap", "[grow, center]", ""));
      setOpaque(false);
    }

    private void setupShopPanels() {
      cardGridPanel = new ShopGridPanel(buildRows(cardItems));
      if (specialItems.isEmpty()) {
        add(cardGridPanel);
        return;
      }

      List<BazaarItem> traitItems = new ArrayList<>();
      List<BazaarItem> carryItems = new ArrayList<>();
      for (BazaarItem item : specialItems) {
        if (item.type() == BazaarItem.Type.TRAIT) {
          traitItems.add(item);
        } else {
          carryItems.add(item);
        }
      }

      List<List<BazaarItem>> specialRows = new ArrayList<>();
      specialRows.add(traitItems);
      specialRows.add(carryItems);
      specialGridPanel = new ShopGridPanel(specialRows);
      tabbedPane = new FTabbedPane();
      tabbedPane.addTab(Localizer.getInstance().getMessage("lblCards"), cardGridPanel);
      tabbedPane.addTab(Localizer.getInstance().getMessage("lblRogueTraitsAndCarryCards"), specialGridPanel);
      tabbedPane.setSelectedIndex(Math.max(0, Math.min(selectedTabIndex, tabbedPane.getTabCount() - 1)));
      add(tabbedPane);
    }

    private int getSelectedTabIndex() {
      return tabbedPane != null ? tabbedPane.getSelectedIndex() : 0;
    }

    private List<List<BazaarItem>> buildRows(List<BazaarItem> items) {
      List<List<BazaarItem>> rows = new ArrayList<>();
      for (int i = 0; i < items.size() && rows.size() < MAX_ROWS; i += MAX_CARDS_PER_ROW) {
        rows.add(items.subList(i, Math.min(items.size(), i + MAX_CARDS_PER_ROW)));
      }
      return rows;
    }

    public void startRevealAnimation() {
      revealIndex = 0;
      revealTimer = new Timer(100, e -> {
        if (revealIndex < cardPanels.size()) {
          if (revealIndex == 0 && cardPanels.get(0).faceDown) {
            SoundSystem.instance.play(SoundEffectType.FlipCard,
                SoundEffectType.FlipCard.isSynced());
          }
          cardPanels.get(revealIndex).reveal();
          revealIndex++;
        } else {
          revealTimer.stop();
        }
      });
      revealTimer.start();
    }

    @Override
    public void doLayout() {
      super.doLayout();

      int shopY = HEADER_HEIGHT;
      int shopHeight = getHeight() - shopY - 10;
      if (tabbedPane != null) {
        tabbedPane.setBounds(10, shopY, Math.max(0, getWidth() - 20), Math.max(0, shopHeight));
      } else if (cardGridPanel != null) {
        cardGridPanel.setBounds(0, shopY, getWidth(), Math.max(0, shopHeight));
      }
    }

    private SelectableCardPanel createCardPanel(BazaarItem item) {
      SelectableCardPanel cardPanel = new SelectableCardPanel(item);
      cardPanels.add(cardPanel);
      return cardPanel;
    }

    private class ShopGridPanel extends SkinnedPanel {
      private final List<List<SelectableCardPanel>> rows = new ArrayList<>();

      private ShopGridPanel(List<List<BazaarItem>> itemRows) {
        super(null);
        setOpaque(false);
        for (List<BazaarItem> itemRow : itemRows) {
          List<SelectableCardPanel> panelRow = new ArrayList<>();
          for (BazaarItem item : itemRow) {
            SelectableCardPanel cardPanel = createCardPanel(item);
            panelRow.add(cardPanel);
            add(cardPanel);
          }
          rows.add(panelRow);
        }
      }

      @Override
      public void doLayout() {
        if (rows.isEmpty()) {
          return;
        }

        calculateCardSize();
        int y = 0;
        for (List<SelectableCardPanel> row : rows) {
          layoutRow(row, y);
          y += cardHeight + 15;
        }
      }

      private void calculateCardSize() {
        int availableWidth = getWidth() - 40;
        int availableHeight = getHeight() - 10;
        int cardsPerRow = rows.stream()
            .mapToInt(List::size)
            .max()
            .orElse(1);
        int numRows = Math.min(MAX_ROWS, rows.size());

        int baseCardImageHeight = Math.round(BASE_CARD_WIDTH * CardPanel.ASPECT_RATIO);
        int baseCardHeight = baseCardImageHeight + PRICE_LABEL_HEIGHT;
        int desiredWidth = cardsPerRow * (BASE_CARD_WIDTH + CARD_SPACING) - CARD_SPACING;
        int desiredHeight = numRows * (baseCardHeight + 15) - 15;

        float widthScale =
            availableWidth > 0 ? Math.min(1.0f, (float) availableWidth / desiredWidth) : 1.0f;
        float heightScale =
            availableHeight > 0 ? Math.min(1.0f, (float) availableHeight / desiredHeight) : 1.0f;
        float scale = Math.min(widthScale, heightScale);

        cardWidth = Math.round(BASE_CARD_WIDTH * scale);
        cardImageHeight = Math.round(cardWidth * CardPanel.ASPECT_RATIO);
        priceLabelHeight = Math.round(PRICE_LABEL_HEIGHT * scale);
        cardHeight = cardImageHeight + priceLabelHeight;
      }

      private void layoutRow(List<SelectableCardPanel> rowPanels, int y) {
        int rowWidth = rowPanels.size() * cardWidth + (rowPanels.size() - 1) * CARD_SPACING;
        int x = (getWidth() - rowWidth) / 2;
        for (SelectableCardPanel cardPanel : rowPanels) {
          cardPanel.setBounds(x, y, cardWidth, cardHeight);
          x += cardWidth + CARD_SPACING;
        }
      }
    }
  }

  private class SelectableCardPanel extends SelectableCardPanelBase {
    private final BazaarItem item;

    private SelectableCardPanel(BazaarItem item) {
      super(item.card(), () -> BazaarDialog.this.zoomUtil, true, ZoomActivation.MOUSE_WHEEL);
      this.item = item;
    }

    public void reveal() {
      flip();
    }

    @Override
    public void doLayout() {
      super.doLayout();
      cardPicture.setBounds(0, 0, getWidth(), cardImageHeight);
    }

    @Override
    protected void toggleSelection() {
      if (!selected) {
        int potentialCost = BazaarPricing.calculateItemsTotalCost(selectedItems) + item.getPrice();
        if (potentialCost > availableGold) {
          return;
        }
      }

      selected = !selected;
      if (selected) {
        selectedItems.add(item);
      } else {
        selectedItems.remove(item);
      }

      updateGoldStatus();
      repaint();
    }

    @Override
    public void paint(Graphics g) {
      super.paint(g);

      if (!faceDown) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawPriceLabel(g2d, getWidth());
      }
    }

    private void drawPriceLabel(Graphics2D g2d, int width) {
      int price = item.getPrice();
      int basePrice = item.getBasePrice();
      boolean isDiscounted = item.isDiscounted();
      int labelY = cardImageHeight;
      int iconSize = Math.max(16, Math.round(28 * priceLabelHeight / (float) PRICE_LABEL_HEIGHT));
      int fontSize = Math.max(12, Math.round(20 * priceLabelHeight / (float) PRICE_LABEL_HEIGHT));

      Image coinIcon = FSkin.getImage(FSkinProp.ICO_QUEST_COIN).getIcon().getImage();
      int iconX = (width - iconSize - 55) / 2;
      int iconY = labelY + (priceLabelHeight - iconSize) / 2;
      g2d.drawImage(coinIcon, iconX, iconY, iconSize, iconSize, null);

      Font priceFont = new Font("Arial", Font.BOLD, fontSize);
      g2d.setFont(priceFont);
      FontMetrics fm = g2d.getFontMetrics();
      int textX = iconX + iconSize + 8;
      int textY = labelY + (priceLabelHeight + fm.getAscent()) / 2 - 2;

      if (isDiscounted) {
        drawDiscountedPrice(g2d, fm, textX, textY, price, basePrice);
      } else {
        g2d.setColor(Color.BLACK);
        g2d.drawString(String.valueOf(price), textX + 1, textY + 1);
        g2d.setColor(Color.YELLOW);
        g2d.drawString(String.valueOf(price), textX, textY);
      }
    }

    private void drawDiscountedPrice(Graphics2D g2d, FontMetrics fm, int textX, int textY,
                                     int price, int basePrice) {
      String origText = String.valueOf(basePrice);
      g2d.setColor(Color.GRAY);
      g2d.drawString(origText, textX, textY);
      int origWidth = fm.stringWidth(origText);
      int strikeY = textY - fm.getAscent() / 3;
      Stroke oldStroke = g2d.getStroke();
      g2d.setStroke(new BasicStroke(2f));
      g2d.drawLine(textX, strikeY, textX + origWidth, strikeY);
      g2d.setStroke(oldStroke);

      int discountedX = textX + origWidth + 6;
      String discText = String.valueOf(price);
      g2d.setColor(Color.BLACK);
      g2d.drawString(discText, discountedX + 1, textY + 1);
      g2d.setColor(new Color(100, 255, 100));
      g2d.drawString(discText, discountedX, textY);
    }

    private void updateGoldStatus() {
      int totalCost = BazaarPricing.calculateItemsTotalCost(selectedItems);
      lblCost.setText(Localizer.getInstance().getMessage("lblRogueBazaarCost", -totalCost));
      lblRemaining.setText(Localizer.getInstance().getMessage("lblRogueRemainingGold", availableGold - totalCost));
      if (optionPane != null) {
        optionPane.getButton(0).setEnabled(!selectedItems.isEmpty());
      }
    }
  }
}
