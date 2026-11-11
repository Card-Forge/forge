package forge.screens.home.rogue;

import forge.LobbyPlayer;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.match.HostedMatch;
import forge.gamemodes.rogue.CodexHelper;
import forge.gamemodes.rogue.RogueConfig;
import forge.gamemodes.rogue.RoguePlanebound;
import forge.gamemodes.rogue.RogueRun;
import forge.gamemodes.rogue.RogueRun.CarryCard;
import forge.gamemodes.rogue.RogueTutorial;
import forge.gamemodes.rogue.effect.*;
import forge.gamemodes.rogue.path.NodePlanebound;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.gui.SOverlayUtils;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgePreferences;
import forge.player.GamePlayerUtil;
import forge.toolbox.FOptionPane;
import forge.toolbox.FSkin;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import net.miginfocom.swing.MigLayout;

class NodePlaneboundHelper {

    void handlePlaneboundNode(NodePlanebound node, RogueRun currentRun) {
        if (currentRun == null || currentRun.getHostedMatch() != null) {
            return;
        }

        handlePlaneboundEffects(node, currentRun);

        SwingUtilities.invokeLater(() -> {
            SOverlayUtils.startGameOverlay();
            SOverlayUtils.showOverlay();
        });

        if (ForgePreferences.DEV_MODE) {
            RoguePlanebound picked = (RoguePlanebound) JOptionPane.showInputDialog(
                null, "Override planebound:", "[DEV] Pick Planebound",
                JOptionPane.PLAIN_MESSAGE, null,
                RogueConfig.loadPlanebounds().toArray(), node.getRoguePlanebound());
            if (picked != null) {
                node.setRoguePlanebound(picked);
            }
        }

        try {
            MatchSetupContext setupCtx = new MatchSetupContext();
            RogueEffectComposite.INSTANCE.onBeforeMatchSetup(setupCtx, currentRun);

            RoguePlanebound planebound = node.getRoguePlanebound();
            List<PaperCard> sharedPlaneDeck = new ArrayList<>();
            if (setupCtx.includePlanes) {
                String cardPlaneName = planebound.planeName();
                CardPool allPlanes = RogueConfig.getAllPlanes();
                for (PaperCard card : allPlanes.toFlatList()) {
                    if (cardPlaneName.equalsIgnoreCase(card.getName())) {
                        sharedPlaneDeck.add(card);
                        break;
                    }
                }
                if (sharedPlaneDeck.isEmpty()) {
                    System.err.println("Warning: Could not find plane card: " + cardPlaneName);
                }
            }

            Set<GameType> appliedVariants = setupCtx.includePlanes
                ? EnumSet.of(GameType.Commander, GameType.Planechase)
                : EnumSet.of(GameType.Commander);

            RegisteredPlayer human = RegisteredPlayer.forVariants(
                2, appliedVariants, currentRun.getCurrentDeck(),
                null, false, sharedPlaneDeck, null
            );
            human.setStartingLife(currentRun.getCurrentLife());

            LobbyPlayer lobbyPlayer = GamePlayerUtil.getGuiPlayer();
            lobbyPlayer.setName(currentRun.getCurrentCommanderName());
            lobbyPlayer.setAvatarIndex(RogueUIHelper.getAvailableAvatarIndex(currentRun.getSelectedRogueDeck().getAvatarIndex()));
            lobbyPlayer.setSleeveIndex(currentRun.getSelectedRogueDeck().getSleeveIndex());
            human.setPlayer(lobbyPlayer);

            if (!currentRun.getCarryCards().isEmpty()) {
                RogueTutorialHelper.showIfNotSeen(RogueTutorial.CARRY_CARDS);
                RogueEffect.addCardToCommandZone("Rogue - Carry Card Enabler", human);
            }
            for (CarryCard card : currentRun.getCarryCards()) {
                RogueEffect.addCardToCommandZone(card.toPaperCard(), human);
            }

            Deck planeboundDeck = RogueConfig.loadPlaneboundDeck(planebound);
            if (planeboundDeck == null) {
                throw new RuntimeException("Planebound deck not found: " + planebound.deckPath());
            }
            CodexHelper.recordPlaneboundEncounter(planebound);
            CodexHelper.recordPlaneboundCommanderCards(planebound, planeboundDeck.getCommanders());

            RegisteredPlayer ai = RegisteredPlayer.forVariants(
                2, appliedVariants, planeboundDeck,
                null, false, sharedPlaneDeck, null
            );
            LobbyPlayer aiLobbyPlayer = GamePlayerUtil.createAiPlayer(
                planebound.planeboundName(),
                RogueUIHelper.getAvailableAvatarIndex(planebound.avatarIndex()),
                0);
            ai.setPlayer(aiLobbyPlayer);

            int planeboundRowCount = currentRun.getPath().countPlaneboundRowsUpTo(node.getRowIndex());
            ai.setStartingLife(node.getPlaneboundLife(planeboundRowCount));

            RogueEffectComposite.INSTANCE.onMatchStart(human, ai, currentRun);

            List<RegisteredPlayer> players = Arrays.asList(human, ai);
            HostedMatch hostedMatch = GuiBase.getInterface().hostMatch();
            if (!RogueTutorialHelper.hasSeenTutorial(RogueTutorial.MATCH_UI)
                    || !RogueTutorialHelper.hasSeenTutorial(RogueTutorial.MATCH_CARD_HIGHLIGHTING)
                    || !RogueTutorialHelper.hasSeenTutorial(RogueTutorial.MATCH_PHASES_AND_YIELDS)) {
                hostedMatch.setStartGameHook(() -> {
                    RogueTutorialHelper.registerMatchTutorials(hostedMatch.getGame());
                    FThreads.invokeInEdtAndWait(() -> RogueTutorialHelper.showIfNotSeen(RogueTutorial.MATCH_UI));
                    hostedMatch.setStartGameHook(null);
                });
            }
            hostedMatch.setEndGameHook(() -> recordPlaneboundPublicCards(planebound, hostedMatch, aiLobbyPlayer));
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
            SwingUtilities.invokeLater(SOverlayUtils::hideOverlay);
        }

        SwingUtilities.invokeLater(SOverlayUtils::hideOverlay);
    }

    private void recordPlaneboundPublicCards(RoguePlanebound planebound, HostedMatch hostedMatch,
                                             LobbyPlayer aiLobbyPlayer) {
        Game game = hostedMatch.getGame();
        if (game == null) {
            return;
        }
        for (Player player : game.getRegisteredPlayers()) {
            if (player.getLobbyPlayer() == aiLobbyPlayer) {
                CodexHelper.recordPlaneboundPublicCardsOwnedBy(planebound, game.getRegisteredPlayers(),
                    List.of(player));
                return;
            }
        }
    }

    private void handlePlaneboundEffects(NodePlanebound node, RogueRun currentRun) {
        int wrathfulCount = node.getWrathfulCount();
        int cursedCount = node.getCursedCount();
        if (wrathfulCount == 0 && cursedCount == 0) {
            return;
        }

        ImageIcon flameIcon = NodePlaneboundPanel.createFlameIcon(14, 18);
        ImageIcon pentagramIcon = NodePlaneboundPanel.createPentagramIcon(14, 18);
        FSkin.SkinnedPanel effectsPanel = new FSkin.SkinnedPanel(
            new MigLayout("insets 5, gap 0, wrap", "[grow]"));
        effectsPanel.setOpaque(false);

        Set<WrathfulEffect> usedWrathfulEffect = new HashSet<>();
        for (int i = 0; i < wrathfulCount; i++) {
            WrathfulEffect w = WrathfulEffect.getRandomExcluding(usedWrathfulEffect);
            usedWrathfulEffect.add(w);
            currentRun.addWrathful(w);
            JLabel lbl = new JLabel(w.getDisplayName() + " - " + w.getDescription(), flameIcon, SwingConstants.LEFT);
            lbl.setFont(FSkin.getRelativeFont(12).getBaseFont());
            lbl.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT).getColor());
            lbl.setIconTextGap(5);
            lbl.setOpaque(false);
            effectsPanel.add(lbl, "growx, h 24px!, wrap");
        }

        Set<CursedEffect> usedCursedEffect = new HashSet<>();
        for (int i = 0; i < cursedCount; i++) {
            CursedEffect c = CursedEffect.getRandomExcluding(usedCursedEffect);
            usedCursedEffect.add(c);
            currentRun.addCursed(c);
            JLabel lbl = new JLabel(c.getDisplayName() + " - " + c.getDescription(), pentagramIcon, SwingConstants.LEFT);
            lbl.setFont(FSkin.getRelativeFont(12).getBaseFont());
            lbl.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT).getColor());
            lbl.setIconTextGap(5);
            lbl.setOpaque(false);
            effectsPanel.add(lbl, "growx, h 24px!, wrap");
        }

        boolean hasWrathful = wrathfulCount > 0;
        boolean hasCursed = cursedCount > 0;
        String title = hasWrathful && hasCursed ? "Wrathful & Cursed Planebound"
            : hasCursed ? "Cursed Planebound" : "Wrathful Planebound";
        String message = hasWrathful && hasCursed ? "This Planebound is Wrathful and Cursed!"
            : hasCursed ? "This Planebound is Cursed!" : "This Planebound is Wrathful!";
        FOptionPane.showOptionDialog(message, title, null, effectsPanel, List.of("OK"), 0);
    }
}
