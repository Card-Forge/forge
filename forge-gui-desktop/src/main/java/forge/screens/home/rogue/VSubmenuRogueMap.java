package forge.screens.home.rogue;

import forge.gamemodes.rogue.RogueMetaProgress;
import forge.gamemodes.rogue.RogueRun;
import forge.gamemodes.rogue.RogueRun.CarryCard;
import forge.gamemodes.rogue.effect.DescensionLevel;
import forge.gamemodes.rogue.effect.EndlessMode;
import forge.gamemodes.rogue.effect.RogueEffect;
import forge.gamemodes.rogue.effect.RogueEffectComposite;
import forge.gui.CardPicturePanel;
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
import forge.item.PaperCard;
import forge.util.Localizer;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import net.miginfocom.swing.MigLayout;

/**
 * Assembles Swing components of "rogue map" submenu singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VSubmenuRogueMap implements IVSubmenu<CSubmenuRogueMap> {
  SINGLETON_INSTANCE;

  private static final int CARRY_CARDS_PER_ROW = 5;
  private static final String LIFE_TOTAL_KEY = "lblRogueLifeTotal";
  private static final String GOLD_TOTAL_KEY = "lblRogueGoldTotal";
  private static final String REMOVAL_CREDITS_KEY = "lblRogueRemovalCredits";

  final Localizer localizer = Localizer.getInstance();
  private DragCell parentCell;
  private final DragTab tab = new DragTab(localizer.getMessage("lblRogueCommander"));

  private final FLabel lblCommanderAvatar = new FLabel.Builder()
      .iconScaleFactor(0.99f).iconInBackground(true).build();

  private final FLabel lblCommanderName = new FLabel.Builder()
      .text("")
      .fontSize(14)
      .fontStyle(Font.BOLD)
      .build();

  private final FLabel lblLife = new FLabel.Builder()
      .text(localizer.getMessage(LIFE_TOTAL_KEY, 20, 20))
      .fontSize(14)
      .fontStyle(Font.BOLD)
      .build();

  private final FLabel lblGold = new FLabel.Builder()
      .text(localizer.getMessage(GOLD_TOTAL_KEY, 0))
      .fontSize(14)
      .fontStyle(Font.BOLD)
      .build();

  private final FLabel lblEchoes = new FLabel.Builder()
      .text(localizer.getMessage("lblRogueEchoTotal", 0))
      .fontSize(14)
      .fontStyle(Font.BOLD)
      .build();

  private final FLabel lblRemovalCredits = new FLabel.Builder()
      .text(localizer.getMessage(REMOVAL_CREDITS_KEY, 0))
      .fontSize(14)
      .fontStyle(Font.BOLD)
      .build();

  private final FLabel lblDescension = new FLabel.Builder()
      .text("")
      .fontSize(14)
      .fontStyle(Font.BOLD)
      .build();

  private final FLabel lblEndless = new FLabel.Builder()
      .text("")
      .fontSize(14)
      .fontStyle(Font.BOLD)
      .build();

  private final JPanel pnlEffects;
  private final FScrollPane scrollEffects;
  private final JPanel pnlCarryCards;
  private final FScrollPane scrollCarryCards;
  private final RogueMapPanel mapPanel;
  private final FButton btnEnterNode;
  private final FButton btnEditDeck;
  private final FButton btnRerollPlane;
  private final FButton btnAbandonRun;
  private final FButton btnDevWinRun = new FButton(localizer.getMessage("lblRogueDevWinRun"));
  private final FButton btnDevNextNode = new FButton(localizer.getMessage("lblRogueDevNextNode"));
  private CardUtil zoomUtil;

  VSubmenuRogueMap() {
    // Add icons to labels
    lblGold.setIcon(FSkin.getIcon(FSkinProp.ICO_QUEST_COIN));
    lblEchoes.setIcon(FSkin.getIcon(FSkinProp.ICO_QUEST_GOLD));
    lblRemovalCredits.setIcon(FSkin.getIcon(FSkinProp.ICO_CARD_IMAGE));
    lblDescension.setIcon(FSkin.getIcon(FSkinProp.ICO_QUEST_ZEP));
    lblDescension.setVisible(false);
    lblEndless.setIcon(FSkin.getIcon(FSkinProp.ICO_QUEST_CHARM));
    lblEndless.setVisible(false);

    // Setup buttons with icons
    btnEditDeck = new FButton(localizer.getMessage("lblRogueEditDeck"));
    btnEditDeck.setIcon(FSkin.getIcon(FSkinProp.ICO_CARD_IMAGE));

    btnEnterNode = new FButton(localizer.getMessage("lblRogueEnterNode"));
    btnEnterNode.setIcon(FSkin.getIcon(FSkinProp.ICO_OPEN));

    btnRerollPlane = new FButton(localizer.getMessage("lblRogueRerollPlane"));
    btnRerollPlane.setIcon(FSkin.getImage(FSkinProp.ICO_FLIPCARD).resize(24, 24).getIcon());
    btnRerollPlane.setVisible(false);

    btnAbandonRun = new FButton(localizer.getMessage("lblRogueAbandonRun"));
    btnAbandonRun.setIcon(FSkin.getImage(FSkinProp.ICO_DELETE).resize(24, 24).getIcon());

    pnlEffects = RogueUIHelper.createEffectPanel();
    scrollEffects = new FScrollPane(pnlEffects, false,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    pnlCarryCards = new JPanel(new MigLayout("insets 0 4 0 4, gap 4 2, wrap " + CARRY_CARDS_PER_ROW));
    pnlCarryCards.setOpaque(false);
    scrollCarryCards = new FScrollPane(pnlCarryCards, false,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scrollCarryCards.setVisible(false);
    mapPanel = new RogueMapPanel(createInfoRow());
  }

  /**
   * Update the display with current run data.
   */
  public void updateDisplay(RogueRun run) {
    // Echoes come from meta progression (persistent), not from the run
    int totalEchoes = RogueMetaProgress.getInstance().getTotalEchoes();
    lblEchoes.setText(localizer.getMessage("lblRogueEchoTotal", totalEchoes));

    if (run != null) {
      lblCommanderName.setText(run.getCurrentCommanderName());
      lblCommanderAvatar.setIcon(FSkin.getAvatars().get(RogueUIHelper.getAvailableAvatarIndex(run.getSelectedRogueDeck().getAvatarIndex())));
      lblLife.setText(localizer.getMessage(LIFE_TOTAL_KEY, run.getCurrentLife(), run.getMaxLife()));
      lblGold.setText(localizer.getMessage(GOLD_TOTAL_KEY, run.getCurrentGold()));
      lblRemovalCredits.setText(localizer.getMessage(REMOVAL_CREDITS_KEY, run.getRemovalCredits()));
      updateDescension(run.getDescensionLevel());
      int endlessLevel = run.getEndlessLevel();
      lblEndless.setVisible(endlessLevel > 0);
      lblEndless.setText(endlessLevel > 0 ? localizer.getMessage("lblRogueEndlessLevel", endlessLevel) : "");
      lblEndless.setToolTipText(endlessLevel > 0
          ? EndlessMode.getModifiersDescription(endlessLevel) : null);

      // Populate active effects (Aetherworks, descension, event traits, chest traits, wounds...)
      List<RogueEffect> allEffects = RogueEffectComposite.getAllEffects(run);
      allEffects.removeIf(effect -> effect instanceof DescensionLevel || effect instanceof EndlessMode);
      RogueUIHelper.populateEffectPanel(pnlEffects, allEffects, run);
      RogueUIHelper.resizeEffectScroller(scrollEffects, pnlEffects);
      scrollEffects.revalidate();
      populateCarryCardPanel(run.getCarryCards());

      mapPanel.updatePath(run);
    } else {
      lblCommanderName.setText("");
      lblCommanderAvatar.setIcon(FSkin.getIcon(FSkinProp.ICO_MINUS));
      lblLife.setText(localizer.getMessage(LIFE_TOTAL_KEY, 20, 20));
      lblGold.setText(localizer.getMessage(GOLD_TOTAL_KEY, 0));
      lblRemovalCredits.setText(localizer.getMessage(REMOVAL_CREDITS_KEY, 0));
      updateDescension(0);
      lblEndless.setVisible(false);
      lblEndless.setToolTipText(null);
      btnRerollPlane.setVisible(false);
      populateCarryCardPanel(List.of());
      mapPanel.clearPath();
    }
  }

  private void updateDescension(int descLevel) {
    lblDescension.setVisible(descLevel > 0);
    if (descLevel <= 0) {
      lblDescension.setToolTipText(null);
      return;
    }
    lblDescension.setText(localizer.getMessage("lblRogueDescensionLevel", descLevel));
    StringBuilder tooltip = new StringBuilder("<html>");
    for (int level = 1; level <= descLevel; level++) {
      DescensionLevel descension = DescensionLevel.forLevel(level);
      if (descension != null) {
        tooltip.append("* ")
            .append(localizer.getMessage("lblRogueDescensionTooltip", level, descension.name, descension.description))
            .append("<br>");
      }
    }
    lblDescension.setToolTipText(tooltip.append("</html>").toString());
  }

  @Override
  public EMenuGroup getGroupEnum() {
    return EMenuGroup.ROGUE;
  }

  @Override
  public String getMenuTitle() {
    return Localizer.getInstance().getMessage("lblRogueContinueRun");
  }

  @Override
  public EDocID getItemEnum() {
    return EDocID.HOME_ROGUEMAP;
  }

  @Override
  public void populate() {
    VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().removeAll();
    VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().setLayout(new MigLayout("insets 0, gap 0, wrap"));

    VHomeUI.SINGLETON_INSTANCE.getPnlDisplay()
        .add(mapPanel, "w 100%!, gap 0, pushy, growy");

    JPanel buttonRow = new JPanel(new MigLayout("insets 0, gap 10, fillx"));
    buttonRow.setOpaque(false);
    final String buttonConstraints = "wmin 0, growx, shrinkx, h 40px!";
    buttonRow.add(btnEnterNode, buttonConstraints);
    buttonRow.add(btnEditDeck, buttonConstraints);
    buttonRow.add(btnRerollPlane, buttonConstraints + ", hidemode 3");
    buttonRow.add(btnAbandonRun, buttonConstraints);
    if (ForgePreferences.DEV_MODE) {
      buttonRow.add(btnDevNextNode, buttonConstraints);
      buttonRow.add(btnDevWinRun, buttonConstraints);
    }
    VHomeUI.SINGLETON_INSTANCE.getPnlDisplay()
        .add(buttonRow, "w 96%!, h 40px!, gap 2% 2% 10px 20px, ax center");

    VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().repaintSelf();
    VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().revalidate();
  }

  public JButton getBtnEnterNode() {
    return btnEnterNode;
  }

  public JButton getBtnEditDeck() {
    return btnEditDeck;
  }

  public JButton getBtnRerollPlane() {
    return btnRerollPlane;
  }

  public JButton getBtnAbandonRun() {
    return btnAbandonRun;
  }

  public JButton getBtnDevWinRun() {
    return btnDevWinRun;
  }

  public JButton getBtnDevNextNode() {
    return btnDevNextNode;
  }

  public PathVisualizerPanel getPathVisualizer() {
    return mapPanel.getPathVisualizer();
  }

  @Override
  public EDocID getDocumentID() {
    return EDocID.HOME_ROGUEMAP;
  }

  @Override
  public DragTab getTabLabel() {
    return tab;
  }

  @Override
  public CSubmenuRogueMap getLayoutControl() {
    return CSubmenuRogueMap.SINGLETON_INSTANCE;
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

  private void populateCarryCardPanel(List<CarryCard> carryCards) {
    pnlCarryCards.removeAll();

    for (CarryCard carryCard : carryCards) {
      PaperCard paperCard = carryCard.toPaperCard();
      if (paperCard == null) {
        continue;
      }
      pnlCarryCards.add(createCarryCardThumbnail(carryCard, paperCard));
    }

    pnlCarryCards.revalidate();
    int thumbnailCount = pnlCarryCards.getComponentCount();
    boolean needsScrollbar = thumbnailCount > CARRY_CARDS_PER_ROW;
    scrollCarryCards.setVerticalScrollBarPolicy(needsScrollbar
        ? ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS : ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
    if (!needsScrollbar) {
      scrollCarryCards.scrollToTop();
    }

    int viewportWidth = pnlCarryCards.getPreferredSize().width;
    int viewportHeight = thumbnailCount == 0 ? 0 : pnlCarryCards.getComponent(0).getPreferredSize().height;
    scrollCarryCards.getViewport().setPreferredSize(new Dimension(viewportWidth, viewportHeight));
    int scrollbarWidth = needsScrollbar ? scrollCarryCards.getVerticalScrollBar().getPreferredSize().width : 0;
    scrollCarryCards.setPreferredSize(new Dimension(viewportWidth + scrollbarWidth, viewportHeight));
    scrollCarryCards.setVisible(thumbnailCount > 0);
    scrollCarryCards.revalidate();
    scrollCarryCards.repaint();
  }

  private JPanel createCarryCardThumbnail(CarryCard carryCard, PaperCard paperCard) {
    int thumbnailWidth = 32;
    int thumbnailHeight = 45;

    CardPicturePanel picturePanel = new CardPicturePanel();
    picturePanel.setOpaque(false);
    picturePanel.setItem(paperCard);
    picturePanel.setPreferredSize(new Dimension(thumbnailWidth, thumbnailHeight));
    picturePanel.setMinimumSize(new Dimension(thumbnailWidth, thumbnailHeight));
    picturePanel.setMaximumSize(new Dimension(thumbnailWidth, thumbnailHeight));
    picturePanel.setToolTipText(buildCarryCardTooltip(carryCard, paperCard));
    picturePanel.addMouseWheelListener(e -> {
      if (e.getWheelRotation() < 0 && getZoomUtil() != null) {
        getZoomUtil().showZoom(paperCard);
      }
    });

    JPanel thumbnailPanel = new JPanel(new MigLayout("insets 0, gap 0"));
    thumbnailPanel.setOpaque(false);
    thumbnailPanel.setPreferredSize(new Dimension(thumbnailWidth, thumbnailHeight));
    thumbnailPanel.setMinimumSize(new Dimension(thumbnailWidth, thumbnailHeight));
    thumbnailPanel.setMaximumSize(new Dimension(thumbnailWidth, thumbnailHeight));
    thumbnailPanel.setToolTipText(buildCarryCardTooltip(carryCard, paperCard));
    thumbnailPanel.add(picturePanel, "w " + thumbnailWidth + "!, h " + thumbnailHeight + "!");
    thumbnailPanel.addMouseWheelListener(e -> {
      if (e.getWheelRotation() < 0 && getZoomUtil() != null) {
        getZoomUtil().showZoom(paperCard);
      }
    });
    return thumbnailPanel;
  }

  private String buildCarryCardTooltip(CarryCard carryCard, PaperCard paperCard) {
    return paperCard.getName() + " (" + switch (carryCard.type()) {
      case ITEM -> localizer.getMessage("lblRogueCarryItem");
      case FELLOW -> localizer.getMessage("lblRogueCarryFellow");
      case SCROLL -> localizer.getMessage("lblRogueCarryScroll");
    } + ")";
  }

  private JPanel createInfoRow() {
    JPanel infoRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 3));
    infoRow.setOpaque(false);
    lblCommanderAvatar.setPreferredSize(new Dimension(45, 45));
    infoRow.add(lblCommanderAvatar);
    infoRow.add(lblCommanderName);
    infoRow.add(lblLife);
    infoRow.add(lblGold);
    infoRow.add(lblEchoes);
    infoRow.add(lblRemovalCredits);
    infoRow.add(lblDescension);
    infoRow.add(scrollEffects);
    infoRow.add(scrollCarryCards);
    infoRow.add(lblEndless);
    return infoRow;
  }

  private CardUtil getZoomUtil() {
    if (zoomUtil == null && pnlCarryCards != null) {
      Window window = SwingUtilities.getWindowAncestor(pnlCarryCards);
      if (window != null) {
        zoomUtil = new CardUtil(window);
        zoomUtil.setupZoomOverlay();
      }
    }
    return zoomUtil;
  }

}
