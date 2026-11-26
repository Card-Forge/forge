package forge.gamemodes.rogue.flow;

import forge.LobbyPlayer;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.match.HostedMatch;
import forge.gamemodes.rogue.CodexHelper;
import forge.gamemodes.rogue.RogueConfig;
import forge.gamemodes.rogue.RogueIO;
import forge.gamemodes.rogue.RoguePlanebound;
import forge.gamemodes.rogue.RogueRun;
import forge.gamemodes.rogue.RogueRun.CarryCard;
import forge.gamemodes.rogue.effect.*;
import forge.gamemodes.rogue.flow.RogueDecisions.RogueMatch;
import forge.gamemodes.rogue.path.NodePlanebound;
import forge.gui.GuiBase;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgePreferences;
import forge.player.GamePlayerUtil;
import forge.util.Localizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.tinylog.Logger;

class NodePlaneboundHelper {
    private final RogueFlow flow;
    private Runnable restoreHuman;

    NodePlaneboundHelper(RogueFlow flow) {
        this.flow = flow;
    }

    void handlePlaneboundNode(NodePlanebound node, RogueRun currentRun) {
        if (currentRun == null || currentRun.getHostedMatch() != null) {
            return;
        }

        handlePlaneboundEffects(node, currentRun);

        if (ForgePreferences.DEV_MODE) {
            RoguePlanebound current = node.getRoguePlanebound();
            RoguePlanebound picked = flow.decisions().devPick(
                Localizer.getInstance().getMessage("lblRogueDevPickPlanebound"), RogueConfig.loadPlanebounds(),
                Collections.singletonList(current), 1).get(0);
            if (picked != current) {
                node.setRoguePlanebound(picked);
            }
        }

        try {
            MatchSetupContext setupCtx = new MatchSetupContext();
            RogueEffectComposite.INSTANCE.onBeforeMatchSetup(setupCtx, currentRun);

            RoguePlanebound planebound = node.getRoguePlanebound();
            List<PaperCard> sharedPlaneDeck = getSharedPlaneDeck(planebound, setupCtx.includePlanes);

            Set<GameType> appliedVariants = setupCtx.includePlanes
                ? EnumSet.of(GameType.Commander, GameType.Planechase)
                : EnumSet.of(GameType.Commander);

            RegisteredPlayer human = RegisteredPlayer.forVariants(
                2, appliedVariants, currentRun.getCurrentDeck(),
                null, false, sharedPlaneDeck, null
            );
            human.setStartingLife(currentRun.getCurrentLife());

            // RogueWinLoseController recognises the winner by this player
            LobbyPlayer humanLobbyPlayer = GamePlayerUtil.getGuiPlayer();
            String name = humanLobbyPlayer.getName();
            int avatar = humanLobbyPlayer.getAvatarIndex();
            int sleeve = humanLobbyPlayer.getSleeveIndex();
            restoreHuman = () -> {
                humanLobbyPlayer.setName(name);
                humanLobbyPlayer.setAvatarIndex(avatar);
                humanLobbyPlayer.setSleeveIndex(sleeve);
            };
            humanLobbyPlayer.setName(currentRun.getCurrentCommanderName());
            humanLobbyPlayer.setAvatarIndex(availableAvatar(currentRun.getSelectedRogueDeck().getAvatarIndex()));
            humanLobbyPlayer.setSleeveIndex(currentRun.getSelectedRogueDeck().getSleeveIndex());
            human.setPlayer(humanLobbyPlayer);

            if (!currentRun.getCarryCards().isEmpty()) {
                RogueEffect.addCardToCommandZone("Rogue - Carry Card Enabler", human);
            }
            for (CarryCard card : currentRun.getCarryCards()) {
                RogueEffect.addCardToCommandZone(card.toPaperCard(), human);
            }

            Deck planeboundDeck = RogueConfig.loadPlaneboundDeck(planebound);
            if (planeboundDeck == null) {
                throw new IllegalStateException("Planebound deck not found: " + planebound.deckPath());
            }
            CodexHelper.recordPlaneboundEncounter(planebound);
            CodexHelper.recordPlaneboundCommanderCards(planebound, planeboundDeck.getCommanders());

            RegisteredPlayer ai = RegisteredPlayer.forVariants(
                2, appliedVariants, planeboundDeck,
                null, false, sharedPlaneDeck, null
            );
            LobbyPlayer aiLobbyPlayer = GamePlayerUtil.createAiPlayer(
                planebound.planeboundName(),
                availableAvatar(planebound.avatarIndex()),
                0);
            ai.setPlayer(aiLobbyPlayer);

            int planeboundRowCount = currentRun.getPath().countPlaneboundRowsUpTo(node.getRowIndex());
            ai.setStartingLife(node.getPlaneboundLife(planeboundRowCount));

            RogueEffectComposite.INSTANCE.onMatchStart(human, ai, currentRun);

            List<RegisteredPlayer> players = Arrays.asList(human, ai);
            flow.decisions().startMatch(new RogueMatch(
                currentRun,
                node,
                appliedVariants,
                players,
                human,
                hostedMatch -> recordPlaneboundPublicCards(planebound, hostedMatch, aiLobbyPlayer)
            ));
        } catch (Exception e) {
            Logger.error(e, "Could not start Rogue match for Planebound {}", node.getRoguePlanebound());
            // Charges and event effects are spent before a match exists, so the last save is the only state to return to
            currentRun.setHostedMatch(null);
            flow.setRun(RogueIO.loadRun(RogueIO.getRogueFile(currentRun)));
            flow.decisions().showMessage(
                Localizer.getInstance().getMessage("lblRogueRogueCommanderMap"),
                Localizer.getInstance().getMessage("lblRogueMatchNotStarted"));
            flow.decisions().mapChanged(flow.run());
        }
    }

    /** The human plays as the commander only during the match. */
    void restoreHuman() {
        if (restoreHuman != null) {
            restoreHuman.run();
            restoreHuman = null;
        }
    }

    /** The avatar tiles end with the question mark at 125, and a skin may have fewer. */
    private static int availableAvatar(int index) {
        return index >= 0 && index < Math.min(126, GuiBase.getInterface().getAvatarCount()) ? index : 0;
    }

    private static List<PaperCard> getSharedPlaneDeck(RoguePlanebound planebound, boolean includePlanes) {
        List<PaperCard> sharedPlaneDeck = new ArrayList<>();
        if (!includePlanes) {
            return sharedPlaneDeck;
        }

        PaperCard plane = CodexHelper.getPlaneCard(planebound);
        if (plane == null) {
            Logger.warn("Could not find plane card: {}", planebound.planeName());
        } else {
            sharedPlaneDeck.add(plane);
        }
        return sharedPlaneDeck;
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

        Set<WrathfulEffect> usedWrathfulEffect = new LinkedHashSet<>();
        for (int i = 0; i < wrathfulCount; i++) {
            WrathfulEffect w = WrathfulEffect.getRandomExcluding(usedWrathfulEffect);
            usedWrathfulEffect.add(w);
            currentRun.addWrathful(w);
        }

        Set<CursedEffect> usedCursedEffect = new LinkedHashSet<>();
        for (int i = 0; i < cursedCount; i++) {
            CursedEffect c = CursedEffect.getRandomExcluding(usedCursedEffect);
            usedCursedEffect.add(c);
            currentRun.addCursed(c);
        }

        flow.decisions().showPlaneboundEffects(node,
            new ArrayList<>(usedWrathfulEffect), new ArrayList<>(usedCursedEffect));
    }
}
