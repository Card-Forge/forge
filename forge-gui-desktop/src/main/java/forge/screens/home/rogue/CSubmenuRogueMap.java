package forge.screens.home.rogue;

import javax.swing.SwingUtilities;
import forge.util.Localizer;

import forge.Singletons;
import forge.gamemodes.rogue.*;
import forge.gamemodes.rogue.flow.RogueFlow;
import forge.gamemodes.rogue.path.*;
import forge.gui.framework.EDocID;
import forge.gui.framework.FScreen;
import forge.gui.framework.ICDoc;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.deckeditor.controllers.CEditorRogue;
import forge.screens.deckeditor.views.VCardCatalog;
import forge.screens.home.CHomeUI;
import forge.screens.home.VHomeUI;
import forge.toolbox.FOptionPane;
import java.awt.event.ActionListener;
import java.util.*;
import javax.swing.*;

/**
 * Controls the "rogue map" submenu in the home UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 */
public enum CSubmenuRogueMap implements ICDoc {
  SINGLETON_INSTANCE;

  private final RogueFlow flow = new RogueFlow(new DesktopRogueDecisions());
  private final ActionListener actEnterNode = arg0 -> flow.enter();
  private final ActionListener actEditDeck = arg0 -> editDeck();
  private final ActionListener actRerollPlane = arg0 -> rerollSelectedPlanebound();
  private final ActionListener actAbandonRun = arg0 -> abandonRun();
  private final VSubmenuRogueMap view = VSubmenuRogueMap.SINGLETON_INSTANCE;

  @Override
  public void update() {
    if (!RogueUIHelper.progressReadable()) {
      // Without a run, Back does nothing and the sidebar is the only way out
      VHomeUI.SINGLETON_INSTANCE.setSidebarVisible(true);
      return;
    }
    RogueRun currentRun = flow.load().orElse(null);

    // If still no run (no saved runs or all completed/failed), navigate to start screen
    if (currentRun == null) {
      CHomeUI.SINGLETON_INSTANCE.itemClick(EDocID.HOME_ROGUESTART);
      return;
    }

    currentRun.getRunTimer().start();
    updateView();
    SwingUtilities.invokeLater(() -> {
      view.getBtnEnterNode().requestFocusInWindow();
      showTutorials();
    });
  }

  private void showTutorials() {
    RogueTutorialHelper.showIfNotSeen(RogueTutorialTriggers.onMap(flow.run()).toArray(new RogueTutorial[0]));
  }

  @Override
  public void register() {
    // The map attaches its listeners in initialize().
  }

  @Override
  public void initialize() {
    view.getBtnEnterNode().addActionListener(actEnterNode);
    view.getBtnEditDeck().addActionListener(actEditDeck);
    view.getBtnRerollPlane().addActionListener(actRerollPlane);
    view.getBtnBackToHome().addActionListener(e -> backToHome());
    view.getBtnAbandonRun().addActionListener(actAbandonRun);
    view.getBtnDevWinRun().addActionListener(e -> flow.devWinRun());
    view.getBtnDevNextNode().addActionListener(e -> flow.devNextNode());
    view.getPathVisualizer().setNodeClickHandler(this::handleNodeClick);
  }

  /**
   * Handle click on a node panel.
   */
  private void handleNodeClick(NodePanel panel) {
    if (flow.select(view.getPathVisualizer().getNodePanels().indexOf(panel))) {
      updateViewWithSelection();
    }
  }

  private void updateView() {
    view.updateDisplay(flow.run());
    updateViewWithSelection();
  }

  /**
   * Update the view with selection state and button text.
   */
  private void updateViewWithSelection() {
    RogueRun currentRun = flow.run();
    if (currentRun == null) {
      return;
    }

    // Update path visualizer selection
    view.getPathVisualizer().setSelectedNode(currentRun.getCurrentNodeIndex());

    boolean matchInProgress = currentRun.getHostedMatch() != null;
    view.getBtnBackToHome().setEnabled(!matchInProgress);
    view.getBtnAbandonRun().setEnabled(!matchInProgress);

    // Update button state based on current node
    RoguePathNode currentNode = currentRun.getCurrentNode();

    if (currentNode == null) {
      view.getBtnEnterNode().setEnabled(false);
      view.getBtnEnterNode().setText(flow.enterText());
      view.getBtnRerollPlane().setVisible(false);
      view.updateActionButtonLayout();
      return;
    }

    // Disable button if match already in progress (prevents duplicate match tabs)
    view.getBtnEnterNode().setEnabled(!matchInProgress);
    view.getBtnEditDeck().setEnabled(!matchInProgress);
    view.getBtnEnterNode().setText(flow.enterText());
    updatePlaneboundRerollButton();
    view.updateActionButtonLayout();
  }

  private void updatePlaneboundRerollButton() {
    JButton button = view.getBtnRerollPlane();
    RogueFlow.Reroll reroll = flow.reroll();
    if (reroll.left() <= 0) {
      button.setVisible(false);
      button.setToolTipText(null);
    } else {
      button.setVisible(true);
      button.setText(Localizer.getInstance().getMessage("lblRogueRerollPlaneCount", reroll.left()));
      button.setEnabled(reroll.enabled());
      button.setToolTipText(reroll.reason());
    }
  }

  private void rerollSelectedPlanebound() {
    if (flow.rerollPlane()) {
      updateView();
    } else {
      updateViewWithSelection();
    }
  }

  public RogueFlow getFlow() {
    return flow;
  }

  public RogueRun getCurrentRun() {
    return flow.run();
  }

  private void backToHome() {
    RogueRun currentRun = flow.run();
    if (currentRun == null || currentRun.getHostedMatch() != null) {
      return;
    }

    if (!flow.save()) {
      currentRun.getRunTimer().start();
      FOptionPane.showErrorDialog(Localizer.getInstance().getMessage("lblSavefailed"));
      return;
    }
    CHomeUI.SINGLETON_INSTANCE.itemClick(EDocID.HOME_ROGUESTART);
  }

  private void abandonRun() {
    RogueRun currentRun = flow.run();
    if (currentRun == null || currentRun.getHostedMatch() != null) {
      return;
    }

    boolean confirmed = FOptionPane.showConfirmDialog(
        Localizer.getInstance().getMessage("lblRogueAbandonRunConfirm"),
        Localizer.getInstance().getMessage("lblRogueAbandonRun"),
        Localizer.getInstance().getMessage("lblRogueAbandonRun"),
        Localizer.getInstance().getMessage("lblCancel"),
        false);
    if (!confirmed) {
      return;
    }

    flow.abandon();
  }

  public void editDeck() {
    RogueRun currentRun = flow.run();
    if (currentRun == null || currentRun.getHostedMatch() != null) {
      return;
    }

    CDeckEditorUI deckEditor = CDeckEditorUI.SINGLETON_INSTANCE;
    deckEditor.setEditorController(new CEditorRogue(currentRun, FScreen.DECK_EDITOR_ROGUE,
        deckEditor.getCDetailPicture()));
    if (!Singletons.getControl().setCurrentScreen(FScreen.DECK_EDITOR_ROGUE)) {
      return;
    }

    SwingUtilities.invokeLater(() -> {
      VCardCatalog catalog = VCardCatalog.SINGLETON_INSTANCE;
      if (catalog.getParentCell() != null) {
        catalog.getParentCell().setSelected(catalog);
      }
      RogueTutorialHelper.showIfNotSeen(RogueTutorial.DECK_EDITOR);
    });
  }
}
