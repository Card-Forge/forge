package forge.screens.home.rogue;

import forge.util.Localizer;

import forge.gamemodes.rogue.AetherUpgrade;
import forge.gamemodes.rogue.RogueMetaProgress;
import forge.gamemodes.rogue.effect.AetherEffect;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.skin.FSkinProp;
import forge.screens.home.EMenuGroup;
import forge.screens.home.IVSubmenu;
import forge.screens.home.VHomeUI;
import forge.toolbox.FButton;
import forge.toolbox.FLabel;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import net.miginfocom.swing.MigLayout;

/**
 * Assembles Swing components for the Aether screen. Allows players to spend Echoes on permanent
 * upgrades (Aetherworks).
 */
public enum VSubmenuRogueAether implements IVSubmenu<CSubmenuRogueAether> {
  SINGLETON_INSTANCE;

  private DragCell parentCell;
  private final Localizer localizer = Localizer.getInstance();
  private final DragTab tab = new DragTab(localizer.getMessage("lblRogueAether"));

  private final FLabel lblTitle = new FLabel.Builder()
      .text(Localizer.getInstance().getMessage("lblRogueTheAether"))
      .fontAlign(SwingConstants.CENTER)
      .opaque(true)
      .fontSize(16)
      .build();

  private final FLabel lblEchoes = new FLabel.Builder()
      .text(localizer.getMessage("lblRogueEchoTotal", 0))
      .fontSize(16)
      .fontStyle(Font.BOLD)
      .build();

  private final FLabel lblSparks = new FLabel.Builder()
      .text(localizer.getMessage("lblRogueSparkTotal", 0))
      .fontSize(16)
      .fontStyle(Font.BOLD)
      .build();

  private final FLabel lblAetherEnergy = new FLabel.Builder()
      .text(localizer.getMessage("lblRogueAetherEnergy", 0, 3))
      .fontSize(14)
      .build();

  // One panel for each Aetherwork
  private final Map<AetherEffect, AetherworkPanel> aetherworkPanels =
      new EnumMap<>(AetherEffect.class);

  // Aether Upgrade card (persistent so listener can be wired once in initialize)
  private final AetherUpgradeCard upgradeCard = new AetherUpgradeCard();

  private final FButton btnBack;
  private final FButton btnResetAetherworks;
  private final FButton btnDevMaxAether = new FButton(localizer.getMessage("lblRogueDevMaxAether"));
  private final FButton btnDevGainEchoes = new FButton(localizer.getMessage("lblRogueDevGainEchoes"));
  private final FButton btnDevGainSparks = new FButton(localizer.getMessage("lblRogueDevGainSparks"));

  VSubmenuRogueAether() {
    lblTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
    lblEchoes.setIcon(FSkin.getIcon(FSkinProp.ICO_QUEST_GOLD));
    lblSparks.setIcon(FSkin.getIcon(FSkinProp.ICO_QUEST_ELIXIR));
    lblSparks.setVisible(false);
    btnBack = new FButton(localizer.getMessage("lblBack"));
    btnBack.setIcon(FSkin.getImage(FSkinProp.ICO_OPEN).resize(24, 24).getIcon());
    btnResetAetherworks = new FButton(localizer.getMessage("lblRogueResetAetherworks"));
    btnResetAetherworks.setIcon(FSkin.getImage(FSkinProp.ICO_DELETE).resize(24, 24).getIcon());

    // Create Aetherwork panels once so listeners can be attached in initialize
    for (AetherEffect aetherEffect : AetherEffect.values()) {
      aetherworkPanels.put(aetherEffect, new AetherworkPanel(aetherEffect));
    }
  }

  @Override
  public EMenuGroup getGroupEnum() {
    return EMenuGroup.ROGUE;
  }

  @Override
  public String getMenuTitle() {
    return Localizer.getInstance().getMessage("lblRogueAether");
  }

  @Override
  public EDocID getItemEnum() {
    return EDocID.HOME_ROGUEAETHER;
  }

  @Override
  public EDocID getDocumentID() {
    return EDocID.HOME_ROGUEAETHER;
  }

  @Override
  public DragTab getTabLabel() {
    return tab;
  }

  @Override
  public CSubmenuRogueAether getLayoutControl() {
    return CSubmenuRogueAether.SINGLETON_INSTANCE;
  }

  @Override
  public void populate() {
    VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().removeAll();
    VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().setLayout(new MigLayout("insets 0, gap 0, wrap"));

    VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblTitle, "w 98%!, h 30px!, gap 1% 0 15px 15px");

    // Echo and Aether Energy display
    JPanel headerPanel = new JPanel(new MigLayout("insets 10, gap 20"));
    headerPanel.setOpaque(false);
    headerPanel.add(lblEchoes);
    headerPanel.add(lblSparks, "hidemode 3");
    headerPanel.add(lblAetherEnergy);
    VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(headerPanel, "w 98%!, gap 1% 0 10px 10px");

    // Aetherwork grid in a scroll pane — takes all remaining vertical space
    RogueMetaProgress progress = RogueMetaProgress.getInstance();
    int upgradeLevel = progress.getAetherUpgradeLevel();
    AetherUpgrade next = AetherUpgrade.forLevel(upgradeLevel + 1);
    JComponent cardToShow;
    if (!progress.isDescensionModeUnlocked()) {
      cardToShow = createLockedInfoPanel();
    } else if (next != null) {
      cardToShow = upgradeCard;
    } else {
      cardToShow = null;
    }
    AetherworkGridPanel aetherworkGrid = createAetherworkGrid(upgradeLevel, cardToShow);
    FScrollPane scrollAetherworks = new FScrollPane(aetherworkGrid, true,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(scrollAetherworks,
        "w 98%!, gap 1% 0 10px 10px, pushy, growy");

    // Buttons always below the scroll area — always visible
    JPanel buttonPanel = new JPanel(new MigLayout("insets 0, gap 10"));
    buttonPanel.setOpaque(false);
    final String buttonSize = "w 180px!, h 40px!";
    buttonPanel.add(btnBack, buttonSize);
    buttonPanel.add(btnResetAetherworks, buttonSize);
    if (ForgePreferences.DEV_MODE) {
      buttonPanel.add(btnDevMaxAether, buttonSize);
      buttonPanel.add(btnDevGainEchoes, buttonSize);
      buttonPanel.add(btnDevGainSparks, buttonSize);
    }
    VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(buttonPanel, "ax center, gap 0 0 10px 10px");

    VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().repaintSelf();
    VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().revalidate();
  }

  private AetherworkGridPanel createAetherworkGrid(int upgradeLevel, JComponent card) {
    List<AetherworkPanel> visible = new ArrayList<>();
    for (AetherEffect aetherEffect : AetherEffect.values()) {
      if (aetherEffect.isAccessibleAt(upgradeLevel)) {
        visible.add(aetherworkPanels.get(aetherEffect));
      }
    }
    return new AetherworkGridPanel(visible, card);
  }

  private static JComponent createLockedInfoPanel() {
    FSkin.SkinnedPanel panel = new FSkin.SkinnedPanel(
        new MigLayout("insets 20 20 20 20, gap 10, wrap, fill, align center center"));
    panel.setOpaque(true);
    panel.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

    FLabel lblInfo = new FLabel.Builder()
        .text(Localizer.getInstance().getMessage("lblRogueAetherEnergyHint"))
        .icon(FSkin.getImage(FSkinProp.ICO_LOCK).resize(20, 20))
        .iconScaleAuto(false)
        .fontSize(13)
        .build();

    panel.add(lblInfo, "growx, push, align center");
    return panel;
  }

  /**
   * Update the display with current meta progress data.
   */
  public void updateDisplay(int echoes, int sparks, boolean descensionUnlocked,
      int activeAetherworkCount, int upgradeLevel, Map<AetherEffect, Integer> effectRanks,
      Set<AetherEffect> activeEffects) {
    lblEchoes.setText(localizer.getMessage("lblRogueEchoTotal", echoes));
    lblSparks.setText(localizer.getMessage("lblRogueSparkTotal", sparks));
    lblSparks.setVisible(descensionUnlocked);

    // Compute actual slot count for label
    int energyCapacity = 3;
    for (int l = 1; l <= upgradeLevel; l++) {
      AetherUpgrade u = AetherUpgrade.forLevel(l);
      if (u != null) energyCapacity += u.extraEnergy;
    }
    lblAetherEnergy.setText(localizer.getMessage("lblRogueAetherEnergy", activeAetherworkCount, energyCapacity));

    // Update upgrade card if visible
    AetherUpgrade next = AetherUpgrade.forLevel(upgradeLevel + 1);
    if (next != null && descensionUnlocked) {
      upgradeCard.update(next, echoes, sparks);
    }

    // Update Aetherwork panels
    for (Map.Entry<AetherEffect, AetherworkPanel> entry : aetherworkPanels.entrySet()) {
      AetherEffect aetherEffect = entry.getKey();
      AetherworkPanel panel = entry.getValue();
      int rank = effectRanks.getOrDefault(aetherEffect, 0);
      boolean isActive = activeEffects.contains(aetherEffect);
      panel.update(rank, isActive, echoes, activeAetherworkCount, upgradeLevel);
    }
  }

  public JButton getBtnBack() {
    return btnBack;
  }

  public JButton getBtnResetAetherworks() {
    return btnResetAetherworks;
  }

  public JButton getBtnDevMaxAether() {
    return btnDevMaxAether;
  }

  public JButton getBtnDevGainEchoes() {
    return btnDevGainEchoes;
  }

  public JButton getBtnDevGainSparks() {
    return btnDevGainSparks;
  }

  public AetherUpgradeCard getUpgradeCard() {
    return upgradeCard;
  }

  public Map<AetherEffect, AetherworkPanel> getAetherworkPanels() {
    return aetherworkPanels;
  }

  @Override
  @SuppressWarnings("java:S3066") // Required by IVDoc.
  public void setParentCell(DragCell cell0) {
    this.parentCell = cell0;
  }

  @Override
  public DragCell getParentCell() {
    return parentCell;
  }

  /**
   * Card displayed in the first row of the Aetherwork grid when an Aether Upgrade is available.
   * Same visual structure as AetherworkPanel: name (top), description (middle), button (bottom).
   */
  static class AetherUpgradeCard extends FSkin.SkinnedPanel {
    private final FLabel lblName;
    private final FLabel lblDescription;
    private final FButton btnUpgrade;
    private boolean isHovered = false;

    AetherUpgradeCard() {
      super(new MigLayout("insets 15 15 15 15, gap 5, wrap, fill"));
      setOpaque(true);
      setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
      addMouseListener(new MouseAdapter() {
        @Override public void mouseEntered(MouseEvent e) { isHovered = true;  repaint(); }
        @Override public void mouseExited(MouseEvent e)  { isHovered = false; repaint(); }
      });

      lblName = new FLabel.Builder()
          .text("")
          .fontSize(16).fontStyle(Font.BOLD).fontAlign(SwingConstants.CENTER).build();

      lblDescription = new FLabel.Builder()
          .text("")
          .fontSize(14).fontAlign(SwingConstants.CENTER).build();

      btnUpgrade = new FButton(Localizer.getInstance().getMessage("lblRogueUpgrade"));
      btnUpgrade.setHorizontalTextPosition(SwingConstants.LEFT);

      add(lblName, "growx, ax center");
      add(lblDescription, "growx, ax center, wmax 370px");
      JPanel spacer = new JPanel();
      spacer.setOpaque(false);
      add(spacer, "growy, pushy");
      add(btnUpgrade, "ax center, w 220px!, h 30px!");
    }

    void update(AetherUpgrade upgrade, int echoes, int sparks) {
      lblName.setText(upgrade.name);
      lblDescription.setText(upgrade.description);
      btnUpgrade.setText(Localizer.getInstance().getMessage("lblRogueUpgradeLabel"));
      btnUpgrade.setIcon(new UpgradeCostIcon(upgrade.sparkCost, upgrade.echoCost));
      btnUpgrade.setEnabled(sparks >= upgrade.sparkCost && echoes >= upgrade.echoCost);
    }

    FButton getBtnUpgrade() { return btnUpgrade; }

    private static class UpgradeCostIcon implements javax.swing.Icon {
      private static final int ICON_SIZE = 16;
      private static final int GAP = 4;

      private final int sparkCost;
      private final int echoCost;
      private final javax.swing.Icon sparkIcon;
      private final javax.swing.Icon echoIcon;

      private UpgradeCostIcon(int sparkCost, int echoCost) {
        this.sparkCost = sparkCost;
        this.echoCost = echoCost;
        sparkIcon = FSkin.getImage(FSkinProp.ICO_QUEST_ELIXIR).resize(ICON_SIZE, ICON_SIZE).getIcon();
        echoIcon = FSkin.getImage(FSkinProp.ICO_QUEST_GOLD).resize(ICON_SIZE, ICON_SIZE).getIcon();
      }

      @Override
      public int getIconWidth() {
        FontMetrics metrics = getFontMetrics();
        return metrics.stringWidth(String.valueOf(sparkCost))
            + ICON_SIZE
            + metrics.stringWidth(String.valueOf(echoCost))
            + ICON_SIZE
            + (GAP * 3);
      }

      @Override
      public int getIconHeight() {
        return ICON_SIZE;
      }

      @Override
      public void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {
        FontMetrics metrics = getFontMetrics();
        int textY = y + ((ICON_SIZE - metrics.getHeight()) / 2) + metrics.getAscent() - 1;
        int iconY = y - 2;
        Color oldColor = g.getColor();
        Color textColor = c.isEnabled() ? c.getForeground() : UIManager.getColor("Button.disabledText");
        g.setColor(textColor != null ? textColor : oldColor);

        g.drawString(String.valueOf(sparkCost), x, textY);
        x += metrics.stringWidth(String.valueOf(sparkCost)) + GAP;
        sparkIcon.paintIcon(c, g, x, iconY);
        x += ICON_SIZE + GAP;

        g.drawString(String.valueOf(echoCost), x, textY);
        x += metrics.stringWidth(String.valueOf(echoCost)) + GAP;
        echoIcon.paintIcon(c, g, x, iconY);
        g.setColor(oldColor);
      }

      private FontMetrics getFontMetrics() {
        return new JLabel().getFontMetrics(FSkin.getFont(14).getBaseFont());
      }
    }

    @Override
    public void paint(Graphics g) {
      super.paint(g);
      Graphics2D g2d = (Graphics2D) g;
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      if (isHovered) {
        g2d.setColor(new Color(140, 140, 140, 180));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 10, 10);
      } else {
        g2d.setColor(new Color(80, 80, 80, 120));
        g2d.setStroke(new BasicStroke(1));
        g2d.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 10, 10);
      }
    }
  }

  /**
   * Responsive grid of AetherworkPanels. Fixed card size (400×150); breaks from 3 → 2 → 1 columns
   * as the viewport narrows. An optional AetherUpgradeCard occupies its own first row, centred.
   * Implements Scrollable so the enclosing FScrollPane tracks width and enables vertical
   * scrolling only — matching the pattern in VSubmenuRogueHistory.
   */
  private static class AetherworkGridPanel extends JPanel implements Scrollable {
    private static final int CARD_W = 400;
    private static final int CARD_H = 170;
    private static final int GAP    = 15;
    private static final int INSET  = 20;

    private final List<AetherworkPanel> panels;
    private final JComponent upgradeCard; // null = no first row

    AetherworkGridPanel(List<AetherworkPanel> panels, JComponent upgradeCard) {
      super(null);
      this.panels = panels;
      this.upgradeCard = upgradeCard;
      setOpaque(false);
      if (upgradeCard != null) add(upgradeCard);
      for (AetherworkPanel panel : panels) add(panel);
    }

    private static int cols(int width) {
      int avail = width - 2 * INSET;
      if (avail >= 3 * CARD_W + 2 * GAP) return 3;
      if (avail >= 2 * CARD_W +     GAP) return 2;
      return 1;
    }

    @Override
    public void doLayout() {
      int startY = INSET;

      // Row 0: upgrade card centred on its own row
      if (upgradeCard != null) {
        int cardX = Math.max(INSET, (getWidth() - CARD_W) / 2);
        upgradeCard.setBounds(cardX, startY, CARD_W, CARD_H);
        startY += CARD_H + GAP;
      }

      if (!panels.isEmpty()) {
        int cols = cols(getWidth());
        int gridW = cols * CARD_W + (cols - 1) * GAP;
        int startX = Math.max(INSET, (getWidth() - gridW) / 2);
        for (int i = 0; i < panels.size(); i++) {
          int col = i % cols;
          int row = i / cols;
          panels.get(i).setBounds(startX + col * (CARD_W + GAP), startY + row * (CARD_H + GAP), CARD_W, CARD_H);
        }
        int rows = (int) Math.ceil(panels.size() / (double) cols);
        startY += rows * CARD_H + (rows - 1) * GAP;
      }

      setPreferredSize(new Dimension(getWidth(), startY + INSET));
    }

    @Override
    public Dimension getPreferredSize() {
      int width = getWidth();
      if (width <= 0) {
        Container parent = getParent();
        width = parent == null ? 1300 : parent.getWidth();
      }
      int startY = INSET;
      if (upgradeCard != null) startY += CARD_H + GAP;
      if (!panels.isEmpty()) {
        int cols = cols(width);
        int rows = (int) Math.ceil(panels.size() / (double) cols);
        startY += rows * CARD_H + (rows - 1) * GAP;
      }
      return new Dimension(width, startY + INSET);
    }

    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
    @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 64; }
    @Override public boolean getScrollableTracksViewportWidth()  { return true;  }
    @Override public boolean getScrollableTracksViewportHeight() { return false; }
  }

  /**
   * Inner class representing a single Aetherwork panel in the grid. Click it to toggle active
   * state (when unlocked). Shows green border and localizer.getMessage("lblRogueActiveAetherwork") badge when active. Shows yellow/gold
   * border on hover.
   */
  public static class AetherworkPanel extends FSkin.SkinnedPanel {
    private final AetherEffect aetherEffect;
    private final FLabel lblName;
    private final FLabel lblDescription;
    private final FLabel lblRank;
    private final FButton btnUpgrade;

    // Visual state
    private boolean isActive = false;
    private boolean isHovered = false;
    private boolean canToggle = false;
    private int currentRank = 0;

    // Click callback for toggling active state
    private transient Consumer<AetherworkPanel> toggleCallback;

    public AetherworkPanel(AetherEffect aetherEffect) {
      super(new MigLayout("insets 15, gap 5, fill",
          "[grow,sg footer][160px!][grow,sg footer]", "[][grow,top][]"));
      this.aetherEffect = aetherEffect;

      setOpaque(true);
      setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

      lblName = new FLabel.Builder()
          .text(aetherEffect.getDisplayName())
          .fontSize(16)
          .fontStyle(Font.BOLD)
          .fontAlign(SwingConstants.CENTER)
          .build();

      lblDescription = new FLabel.Builder()
          .text("")
          .fontSize(14)
          .fontAlign(SwingConstants.CENTER)
          .build();

      Localizer localizer = Localizer.getInstance();
      lblRank = new FLabel.Builder()
          .text(localizer.getMessage("lblRogueRank", 0, aetherEffect.getMaxRank()))
          .fontSize(12)
          .fontAlign(SwingConstants.CENTER)
          .build();

      btnUpgrade = new FButton(localizer.getMessage("lblRogueBuild"));
      final Icon rawEchoIcon = FSkin.getImage(FSkinProp.ICO_QUEST_GOLD).resize(20, 20).getIcon();
      btnUpgrade.setIcon(new Icon() {
        @Override public int getIconWidth()  { return rawEchoIcon.getIconWidth(); }
        @Override public int getIconHeight() { return rawEchoIcon.getIconHeight(); }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
          rawEchoIcon.paintIcon(c, g, x, y - 2);
        }
      });
      btnUpgrade.setHorizontalTextPosition(SwingConstants.LEFT);

      add(lblName, "span 3, growx, wrap");
      add(lblDescription, "span 3, growx, wmax 370px, wrap");
      add(lblRank, "alignx left");
      add(btnUpgrade, "w 160px!, h 30px!");

      // Add mouse listener for hover and click
      addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
          // Only toggle if clicking outside the upgrade button and can toggle
          if (canToggle && toggleCallback != null) {
            toggleCallback.accept(AetherworkPanel.this);
          }
        }

        @Override
        public void mouseEntered(MouseEvent e) {
          isHovered = true;
          if (canToggle) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
          }
          repaint();
        }

        @Override
        public void mouseExited(MouseEvent e) {
          isHovered = false;
          setCursor(Cursor.getDefaultCursor());
          repaint();
        }
      });
    }

    /**
     * Set the callback for when the panel is clicked to toggle active state.
     */
    public void setToggleCallback(Consumer<AetherworkPanel> callback) {
      this.toggleCallback = callback;
    }

    /**
     * Update the panel display based on the current Aetherwork state.
     */
    public void update(int rank, boolean active, int echoes, int activeAetherworkCount,
        int upgradeLevel) {
      this.currentRank = rank;
      this.isActive = active;
      int effectiveMax = aetherEffect.getEffectiveMaxRank(upgradeLevel);
      // Compute actual Aether Energy capacity
      int energyCapacity = 3;
      for (int l = 1; l <= upgradeLevel; l++) {
        AetherUpgrade u = AetherUpgrade.forLevel(l);
        if (u != null) energyCapacity += u.extraEnergy;
      }
      this.canToggle = rank > 0 && (active || activeAetherworkCount < energyCapacity);

      Localizer localizer = Localizer.getInstance();
      lblRank.setText(localizer.getMessage("lblRogueRank", rank, effectiveMax));

      // Update description to show all rank values with current rank highlighted
      lblDescription.setText(aetherEffect.getDescriptionWithAllRanks(rank, upgradeLevel));

      if (rank >= effectiveMax) {
        btnUpgrade.setText(localizer.getMessage("lblRogueMaxRank"));
        btnUpgrade.setEnabled(false);
      } else {
        int cost = aetherEffect.getEchoCostForRank(rank + 1);
        btnUpgrade.setText(localizer.getMessage(rank == 0 ? "lblRogueBuildCost" : "lblRogueUpgradeCost", cost));
        btnUpgrade.setEnabled(echoes >= cost);
      }

      // Update cursor based on toggle ability
      if (isHovered && canToggle) {
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      } else {
        setCursor(Cursor.getDefaultCursor());
      }

      repaint();
    }

    @Override
    public void paint(Graphics g) {
      super.paint(g);

      Graphics2D g2d = (Graphics2D) g;
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      int width = getWidth();
      int height = getHeight();

      // Draw border based on state
      if (isHovered && canToggle) {
        // Hovered (and can toggle): yellow/gold border - takes priority to show clickability
        g2d.setColor(new Color(255, 215, 0));
        g2d.setStroke(new BasicStroke(4));
        g2d.drawRoundRect(2, 2, width - 4, height - 4, 10, 10);
      } else if (isActive) {
        // Active (not hovered): thick green border
        g2d.setColor(new Color(0, 255, 0, 200));
        g2d.setStroke(new BasicStroke(4));
        g2d.drawRoundRect(2, 2, width - 4, height - 4, 10, 10);
      } else if (currentRank > 0) {
        // Unlocked but not active: subtle border
        g2d.setColor(new Color(100, 100, 100, 150));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(2, 2, width - 4, height - 4, 10, 10);
      } else if (isHovered) {
        // Locked + hovered: slightly brighter grey border
        g2d.setColor(new Color(140, 140, 140, 180));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(2, 2, width - 4, height - 4, 10, 10);
      } else {
        // Locked: thin grey border
        g2d.setColor(new Color(80, 80, 80, 120));
        g2d.setStroke(new BasicStroke(1));
        g2d.drawRoundRect(1, 1, width - 2, height - 2, 10, 10);
      }

      // Draw localizer.getMessage("lblRogueActiveAetherwork") badge in top-right corner (always shown when active)
      if (isActive) {
        int badgeWidth = 60;
        int badgeHeight = 20;
        int badgeX = width - badgeWidth - 8;
        int badgeY = 8;

        // Badge background
        g2d.setColor(new Color(0, 200, 0, 230));
        g2d.fillRoundRect(badgeX, badgeY, badgeWidth, badgeHeight, 8, 8);

        // Badge text
        g2d.setColor(Color.WHITE);
        g2d.setFont(g2d.getFont().deriveFont(Font.BOLD, 11f));
        g2d.drawString(Localizer.getInstance().getMessage("lblRogueActiveAetherwork"), badgeX + 8, badgeY + 14);
      }
    }

    public AetherEffect getAetherEffect() {
      return aetherEffect;
    }

    public FButton getBtnUpgrade() {
      return btnUpgrade;
    }

    public boolean isActive() {
      return isActive;
    }
  }
}
