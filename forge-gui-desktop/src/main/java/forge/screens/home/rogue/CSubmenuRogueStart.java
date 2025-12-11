package forge.screens.home.rogue;

import forge.gamemodes.rogue.PathData;
import forge.gamemodes.rogue.RogueConfig;
import forge.gamemodes.rogue.RogueDeckData;
import forge.gamemodes.rogue.RogueIO;
import forge.gamemodes.rogue.RogueRunData;
import forge.gui.UiCommand;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.screens.home.CHomeUI;

import javax.swing.SwingUtilities;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * Controls the Rogue Commander start screen.
 * Handles commander selection and new run creation.
 */
public enum CSubmenuRogueStart implements ICDoc {
    SINGLETON_INSTANCE;

    private final VSubmenuRogueStart view = VSubmenuRogueStart.SINGLETON_INSTANCE;
    private final ActionListener actCommanderSelected = e -> updateCommanderDetails();

  private RogueDeckData selectedDeck;

    @Override
    public void register() {
    }

    @Override
    public void initialize() {
        view.getBtnBeginRun().setCommand((UiCommand) this::beginNewRun);
        view.getCbxCommander().addActionListener(actCommanderSelected);
    }

    @Override
    public void update() {
        loadAvailableCommanders();
        SwingUtilities.invokeLater(() -> view.getBtnBeginRun().requestFocusInWindow());
    }

    private void loadAvailableCommanders() {
      List<RogueDeckData> availableDecks = RogueConfig.loadRogueDecks();

        // Populate combo box
        view.getCbxCommander().removeAllItems();
        for (RogueDeckData deck : availableDecks) {
            view.getCbxCommander().addItem(deck);
        }

        // Select first commander by default
        if (!availableDecks.isEmpty()) {
            view.getCbxCommander().setSelectedIndex(0);
            selectedDeck = availableDecks.get(0);
            updateCommanderDetails();
        }
    }

    private void updateCommanderDetails() {
        selectedDeck = view.getCbxCommander().getSelectedItem();

        if (selectedDeck != null) {
            view.getLblCommanderName().setText(selectedDeck.getCommanderCardName());
            view.getTxtDescription().setText(selectedDeck.getDescription());
            view.getTxtTheme().setText(selectedDeck.getThemeDescription());

            // Force UI refresh for text areas
            view.getTxtDescription().revalidate();
            view.getTxtDescription().repaint();
            view.getTxtTheme().revalidate();
            view.getTxtTheme().repaint();
        }
    }

    private void beginNewRun() {
        if (selectedDeck == null) {
            System.err.println("Error: No commander selected");
            return;
        }

        // Generate path for the run
        PathData path = RogueConfig.getDefaultPath();

        // Create new run
        RogueRunData newRun = new RogueRunData(
            selectedDeck,
            path
        );

        // Generate unique name for the run (used as filename)
        // Format: DeckName_Timestamp (e.g., "MeriaRogueCommander_12-11-25_143022")
        String runName = selectedDeck.getName() + "_" + System.currentTimeMillis();
        newRun.setName(runName);

        // Save the run
        RogueIO.saveRun(newRun);

        // Set as current run in the map controller
        CSubmenuRogueMap.SINGLETON_INSTANCE.setCurrentRun(newRun);

        // Navigate to the Rogue Map
        CHomeUI.SINGLETON_INSTANCE.itemClick(EDocID.HOME_ROGUEMAP);
    }
}
