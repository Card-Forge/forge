package forge.screens.home.rogue;

import forge.gamemodes.rogue.*;
import forge.gui.UiCommand;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.screens.home.CHomeUI;
import java.util.List;
import javax.swing.SwingUtilities;

/**
 * Controls the Rogue Commander start screen.
 * Handles commander selection via card grid and new run creation.
 */
public enum CSubmenuRogueStart implements ICDoc {
    SINGLETON_INSTANCE;

    private final VSubmenuRogueStart view = VSubmenuRogueStart.SINGLETON_INSTANCE;
    private RogueDeck selectedDeck;

    @Override
    public void register() {
    }

    @Override
    public void initialize() {
        view.getBtnBeginRun().setCommand((UiCommand) this::beginNewRun);
    }

    @Override
    public void update() {
        loadAvailableCommanders();
        SwingUtilities.invokeLater(() -> view.getBtnBeginRun().requestFocusInWindow());
    }

    private void loadAvailableCommanders() {
        List<RogueDeck> availableDecks = RogueConfig.loadRogueDecks();

        // Clear existing commander panels
        view.getCommanderGridPanel().clear();

        // Create card panel for each commander
        for (RogueDeck deck : availableDecks) {
            CommanderCardPanel cardPanel = new CommanderCardPanel(deck, view);

            // Set selection callback to update details and handle single-selection
            cardPanel.setSelectionCallback(this::onCommanderSelected);

            view.getCommanderGridPanel().addCommanderPanel(cardPanel);
        }

        // Select first commander by default
        if (!availableDecks.isEmpty() && !view.getCommanderPanels().isEmpty()) {
            CommanderCardPanel firstPanel = view.getCommanderPanels().get(0);
            firstPanel.setSelected(true);
            selectedDeck = firstPanel.getCommander();
            updateCommanderDetails();
        }

        // Refresh layout
        view.getCommanderGridPanel().revalidate();
        view.getCommanderGridPanel().repaint();
    }

    private void onCommanderSelected(CommanderCardPanel clickedPanel) {
        // Deselect all other panels (single-selection mode)
        for (CommanderCardPanel panel : view.getCommanderPanels()) {
            if (panel != clickedPanel) {
                panel.setSelected(false);
            }
        }

        // Toggle the clicked panel
        boolean newState = !clickedPanel.isSelected();
        clickedPanel.setSelected(newState);

        // Update selected deck
        if (newState) {
            selectedDeck = clickedPanel.getCommander();
            updateCommanderDetails();
        } else {
            // If deselecting, clear details
            selectedDeck = null;
            view.getLblCommanderName().setText("");
            view.getTxtDescription().setText("");
            view.getTxtTheme().setText("");
        }
    }

    private void updateCommanderDetails() {
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
        RoguePath path = RogueConfig.getDefaultPath();

        // Create new run
        RogueRun newRun = new RogueRun(
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
