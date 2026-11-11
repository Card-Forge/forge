package forge.screens.home.rogue;

import forge.deckchooser.FDeckViewer;
import forge.gamemodes.rogue.RogueMetaProgress;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.screens.home.CHomeUI;
import forge.toolbox.FOptionPane;

/**
 * Controller for the Rogue Commander run history screen.
 */
public enum CSubmenuRogueHistory implements ICDoc {
  SINGLETON_INSTANCE;

  private final VSubmenuRogueHistory view = VSubmenuRogueHistory.SINGLETON_INSTANCE;
  private boolean initialized = false;

  @Override
  public void register() {
  }

  @Override
  public void initialize() {
    if (initialized) {
      return;
    }
    initialized = true;
    view.getBtnBack().addActionListener(e -> goBack());
    view.getBtnResetHistory().addActionListener(e -> confirmResetHistory());
  }

  @Override
  public void update() {
    view.displayHistory(
        RogueMetaProgress.getInstance().getRunHistory(),
        entry -> FDeckViewer.show(entry.getDeckSnapshot())
    );
  }

  private void goBack() {
    CHomeUI.SINGLETON_INSTANCE.itemClick(EDocID.HOME_ROGUESTART);
  }

  private void confirmResetHistory() {
    boolean confirmed = FOptionPane.showConfirmDialog(
        "Are you sure you want to delete all run history?\nThis action cannot be undone.",
        "Reset Run History",
        "Reset",
        "Cancel",
        false
    );

    if (confirmed) {
      RogueMetaProgress.getInstance().clearRunHistory();
      update();
    }
  }
}
