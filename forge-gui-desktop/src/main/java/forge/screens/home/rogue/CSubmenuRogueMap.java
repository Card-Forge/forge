package forge.screens.home.rogue;

import forge.LobbyPlayer;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.io.DeckSerializer;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.match.HostedMatch;
import forge.gamemodes.rogue.*;
import forge.gui.GuiBase;
import forge.gui.SOverlayUtils;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.player.GamePlayerUtil;
import forge.screens.home.CHomeUI;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.swing.SwingUtilities;

/**
 * Controls the "rogue map" submenu in the home UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 */
public enum CSubmenuRogueMap implements ICDoc {
    SINGLETON_INSTANCE;

    private final ActionListener actEnterNode = arg0 -> enterNode();
    private final VSubmenuRogueMap view = VSubmenuRogueMap.SINGLETON_INSTANCE;

    // Test run data (for MVP - will be replaced with proper loading later)
    private RogueRun currentRun;

    @Override
    public void update() {
        // If no run exists, navigate to start screen
        if (currentRun == null) {
            CHomeUI.SINGLETON_INSTANCE.itemClick(EDocID.HOME_ROGUESTART);
            return;
        }

        updateView();
        SwingUtilities.invokeLater(() -> view.getBtnEnterNode().requestFocusInWindow());
    }

    @Override
    public void register() {
        // TODO document why this method is empty
    }

    @Override
    public void initialize() {
        view.getBtnEnterNode().addActionListener(actEnterNode);
    }

    private void updateView() {
        view.updateDisplay(currentRun);
    }

    private void enterNode() {
        if (currentRun == null || currentRun.getCurrentNode() == null) {
            return;
        }

        RoguePathNode node = currentRun.getCurrentNode();

        // Handle different node types
        if (node instanceof NodePlanebound) {
            startMatch((NodePlanebound) node);
        } else if (node instanceof NodeSanctum) {
            // TODO: Show sanctum screen
            NodeSanctum sanctumNode = (NodeSanctum) node;
            currentRun.healLife(sanctumNode.getHealAmount());
            currentRun.nextNode();
            updateView();
        } else if (node instanceof NodeBazaar || node instanceof NodeEvent || node instanceof NodeChest) {
            // TODO: Implement these node types
            currentRun.nextNode();
            updateView();
        }
    }

    private void startMatch(NodePlanebound node) {
        // Show loading overlay
        SwingUtilities.invokeLater(() -> {
            SOverlayUtils.startGameOverlay();
            SOverlayUtils.showOverlay();
        });

        try {
            // Get all plane cards from the centralized cache
            CardPool allPlanes = forge.gamemodes.rogue.RogueConfig.getAllPlanes();

            // Find the designated plane for this node
            String cardPlaneName = node.getRoguePlanebound().planeName();
            PaperCard designatedPlane = null;
            for (PaperCard card : allPlanes.toFlatList()) {
                if (cardPlaneName.equalsIgnoreCase(card.getName())) {
                    designatedPlane = card;
                    break;
                }
            }

            // Create shared plane deck with multiple copies of the designated plane
            List<PaperCard> sharedPlaneDeck = new java.util.ArrayList<>();
            if (designatedPlane != null) {
                // Add 10 copies of the same plane to satisfy Planechase mechanics
                for (int i = 0; i < 10; i++) {
                    sharedPlaneDeck.add(designatedPlane);
                }
            } else {
                System.err.println("Warning: Could not find plane card: " + cardPlaneName);
            }

            // Configure Commander + Planechase variants
            Set<GameType> appliedVariants = EnumSet.of(GameType.Commander, GameType.Planechase);

            // Create human player with persistent life
            RegisteredPlayer human = RegisteredPlayer.forVariants(
                2,                   // player count
                appliedVariants,                // applied variants
                currentRun.getCurrentDeck(),    // player's deck
                null,                           // schemes (not used)
                false,                          // is archenemy
                sharedPlaneDeck,                // shared plane deck
                null                            // vanguard avatar
            );

            // Override starting life with persistent life from run
            human.setStartingLife(currentRun.getCurrentLife());

            // Use the singleton lobbyPlayer for consistent player identification
            // This ensures isMatchWonBy() works correctly in RogueWinLoseController
            LobbyPlayer lobbyPlayer = GamePlayerUtil.getGuiPlayer();
            lobbyPlayer.setName(currentRun.getSelectedRogueDeck().getName());
            lobbyPlayer.setAvatarIndex(currentRun.getSelectedRogueDeck().getAvatarIndex());
            lobbyPlayer.setSleeveIndex(currentRun.getSelectedRogueDeck().getSleeveIndex());
            human.setPlayer(lobbyPlayer);

            // Load Planebound deck
            Deck planeboundDeck = loadPlaneboundDeck(node.getRoguePlanebound().deckPath());

            // Create AI Planebound opponent
            RegisteredPlayer ai = RegisteredPlayer.forVariants(
                2,                                    // player count
                appliedVariants,                      // applied variants
                planeboundDeck,                       // Planebound deck
                null,                                  // schemes (not used)
                false,                                 // is archenemy
                sharedPlaneDeck,                      // shared plane deck
                null                                   // vanguard avatar
            );
            ai.setPlayer(GamePlayerUtil.createAiPlayer(
                node.getRoguePlanebound().planeboundName(),
                node.getRoguePlanebound().avatarIndex(),
                0));

            // Calculate life based on row: 5 + (5 * rowIndex)
            int planeboundLife = 5 + (5 * node.getRowIndex());
            //ai.setStartingLife(planeboundLife);

            //for testing, set to 1 life
            ai.setStartingLife(1);


            // Start match
            List<RegisteredPlayer> players = Arrays.asList(human, ai);
            HostedMatch hostedMatch = GuiBase.getInterface().hostMatch();
            currentRun.setHostedMatch(hostedMatch);

            hostedMatch.startMatch(
                GameType.RogueCommander,
                appliedVariants,
                players,
                human,
                GuiBase.getInterface().getNewGuiGame()
            );

        } catch (Exception e) {
            e.printStackTrace();
            SwingUtilities.invokeLater(() -> {
                SOverlayUtils.hideOverlay();
                // TODO: Show error message to user
            });
        }

        SwingUtilities.invokeLater(SOverlayUtils::hideOverlay);
    }

    private Deck loadPlaneboundDeck(String deckPath) {
        // Load deck from file path
        // The deckPath is relative to the res directory, e.g., "rogue/planebounds/meria.dck"
        File deckFile = new File(ForgeConstants.RES_DIR, deckPath);

        if (!deckFile.exists()) {
            throw new RuntimeException("Planebound deck not found: " + deckPath + " (full path: " + deckFile.getAbsolutePath() + ")");
        }

        return DeckSerializer.fromFile(deckFile);
    }

    public RogueRun getCurrentRun() {
        return currentRun;
    }

    public void setCurrentRun(RogueRun run) {
        this.currentRun = run;
    }
}
