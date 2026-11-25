package forge.gamemodes.rogue.flow;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.GameEndReason;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.player.Player;
import forge.gamemodes.match.HostedMatch;
import forge.gamemodes.rogue.RogueConfig;
import forge.gamemodes.rogue.RogueDeck;
import forge.gamemodes.rogue.RogueEvent;
import forge.gamemodes.rogue.RogueIO;
import forge.gamemodes.rogue.RogueMetaProgress;
import forge.gamemodes.rogue.RoguePlanebound;
import forge.gamemodes.rogue.RogueRun;
import forge.gamemodes.rogue.RogueRunHistoryEntry;
import forge.gamemodes.rogue.RogueRunState;
import forge.gamemodes.rogue.RogueWinLoseController;
import forge.gamemodes.rogue.effect.ChestEffect;
import forge.gamemodes.rogue.flow.RogueDecisions.Pick;
import forge.gamemodes.rogue.flow.RogueDecisions.RogueMatch;
import forge.gamemodes.rogue.path.NodeBazaar;
import forge.gamemodes.rogue.path.NodeChest;
import forge.gamemodes.rogue.path.NodeEvent;
import forge.gamemodes.rogue.path.NodePlanebound;
import forge.gamemodes.rogue.path.NodeSanctum;
import forge.gamemodes.rogue.path.RoguePath;
import forge.gamemodes.rogue.path.RoguePathNode;
import forge.gui.interfaces.IButton;
import forge.gui.interfaces.IWinLoseView;
import forge.item.PaperCard;
import forge.net.TestUtils;
import forge.player.GamePlayerUtil;
import forge.util.FileUtil;
import forge.util.MyRandom;
import java.io.File;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class RogueFlowTest extends AITest {
    /** Calls that only tell the client to redraw, left out of {@link #asked}. */
    private static final Set<String> REDRAWS = Set.of("mapChanged", "runStarted", "showTutorial");

    private final List<String> asked = new ArrayList<>();
    private final Map<String, Deque<Function<Object[], Object>>> script = new HashMap<>();
    private RogueFlow flow;
    private RogueRun run;

    @BeforeMethod
    public void clean() {
        // AITest initialises the model even after a mocked test has; this only swaps in the headless GUI
        TestUtils.ensureFModelInitialized();
        Path dir = Path.of(RogueIO.ROGUE_SAVE_DIR).toAbsolutePath().normalize();
        // Outside the test profile this directory holds the developer's own runs
        if (!dir.toString().replace(File.separatorChar, '/').contains("/target/test-home/")) {
            throw new IllegalStateException("not the test profile: " + dir);
        }
        if (!FileUtil.deleteDirectory(dir.toFile()) && dir.toFile().exists()) {
            throw new IllegalStateException("could not empty " + dir);
        }
        RogueMetaProgress.reload();
        asked.clear();
        script.clear();
        flow = new RogueFlow(decisions());
    }

    @AfterMethod(alwaysRun = true)
    public void plainRandom() {
        MyRandom.setRandom(new Random());
    }

    /** Answers each question from the script; a question with no answer left fails the test. */
    private RogueDecisions decisions() {
        return (RogueDecisions) Proxy.newProxyInstance(RogueDecisions.class.getClassLoader(), new Class<?>[] {RogueDecisions.class},
            (proxy, method, args) -> {
                String name = method.getName();
                if (!REDRAWS.contains(name)) {
                    asked.add(name);
                }
                Deque<Function<Object[], Object>> answers = script.get(name);
                Function<Object[], Object> answer = answers == null ? null : answers.poll();
                if (answer == null && method.getReturnType() != void.class) {
                    throw new AssertionError("no answer left for " + name + " in " + asked);
                }
                return answer == null ? null : answer.apply(args);
            });
    }

    private void then(String question, Function<Object[], Object> answer) {
        script.computeIfAbsent(question, q -> new ArrayDeque<>()).add(answer);
    }

    /**
     * A saved run of the Aragorn deck with 14 of 23 life and 8 gold. Rows 0 to 2 each hold a plane, a sanctum, a bazaar,
     * an event and a chest, in that column order, and row 3 holds the boss. The current node is the one of the given type
     * in row 1, and the one above it is completed.
     */
    private RoguePathNode start(Class<? extends RoguePathNode> at) {
        MyRandom.setRandom(new Random(1));
        List<RoguePathNode> nodes = new ArrayList<>();
        // Two normal planes, then an elite
        for (String deck : new String[] {"anikthea", "rafiq", "urza"}) {
            int row = nodes.size() / 5;
            add(nodes, new NodePlanebound(planebound(deck)), row, 0);
            add(nodes, new NodeSanctum(), row, 1);
            add(nodes, new NodeBazaar(), row, 2);
            add(nodes, new NodeEvent(RogueEvent.WANDERING_HEALER), row, 3);
            add(nodes, new NodeChest(List.of(ChestEffect.BANNER_OF_CHAMPIONS, ChestEffect.CARD_CACHE_GAMECHANGER)), row, 4);
        }
        add(nodes, new NodePlanebound(planebound("korvold")), 3, 0);

        RogueDeck aragorn = RogueConfig.loadRogueDecks().stream()
            .filter(d -> "Aragorn, the Uniter".equals(d.getCommanderCardName())).findFirst().orElseThrow();
        run = new RogueRun(aragorn);
        run.setPath(new RoguePath(nodes));
        for (int i = 5; i < 10; i++) {
            if (at.isInstance(nodes.get(i))) {
                run.setCurrentNodeIndex(i);
                nodes.get(i - 5).setCompleted(true);
            }
        }
        run.initializeMaxLife(23);
        run.setCurrentLife(14);
        run.setCurrentGold(8);
        run.setName("Aragorn_1");
        RogueIO.saveRun(run);
        flow.setRun(run);
        return run.getCurrentNode();
    }

    private static void add(List<RoguePathNode> nodes, RoguePathNode node, int row, int column) {
        node.setRowIndex(row);
        node.setColumnIndex(column);
        nodes.add(node);
    }

    private static RoguePlanebound planebound(String deck) {
        return RogueConfig.loadPlanebounds().stream()
            .filter(p -> p.deckPath().endsWith("/" + deck + ".dck")).findFirst().orElseThrow();
    }

    private RogueMatch startedMatch() {
        RogueMatch[] started = new RogueMatch[1];
        then("startMatch", args -> started[0] = (RogueMatch) args[0]);
        flow.enter();
        Assert.assertNotNull(started[0], "no match in " + asked);
        return started[0];
    }

    /** Enters the current plane and ends its match by a concession before it starts. */
    private RogueWinLoseController fight(boolean win) {
        RogueMatch match = startedMatch();
        run.setHostedMatch(new HostedMatch());
        GameRules rules = new GameRules(GameType.RogueCommander);
        rules.setAppliedVariants(match.variants());
        Game game = new Match(rules, match.players(), "Rogue").createGame();
        for (Player player : game.getRegisteredPlayers()) {
            if ((player.getLobbyPlayer() == GamePlayerUtil.getGuiPlayer()) != win) {
                player.concede();
            }
        }
        game.setGameOver(GameEndReason.AllOpponentsLost);
        return flow.matchOver(game.getView(), resultScreen());
    }

    private static IWinLoseView<?> resultScreen() {
        ClassLoader loader = IWinLoseView.class.getClassLoader();
        Object button = Proxy.newProxyInstance(loader, new Class<?>[] {IButton.class}, (proxy, method, args) -> null);
        return (IWinLoseView<?>) Proxy.newProxyInstance(loader, new Class<?>[] {IWinLoseView.class}, (proxy, method, args) -> {
            if (method.getName().equals("showRewards")) {
                ((Runnable) args[0]).run();
            }
            return method.getReturnType() == IButton.class ? button : null;
        });
    }

    private static RogueRun saved() {
        return RogueIO.loadMostRecentContinuableRun().orElseThrow(AssertionError::new);
    }

    private static RogueRunHistoryEntry onlyHistoryEntry() {
        List<RogueRunHistoryEntry> history = RogueMetaProgress.getInstance().getRunHistory();
        Assert.assertEquals(history.size(), 1);
        return history.get(0);
    }

    @Test
    public void aChestPickCompletesTheNodeAndSaves() {
        RoguePathNode chest = start(NodeChest.class);
        then("chooseChest", args -> new Pick<>(false, ChestEffect.BANNER_OF_CHAMPIONS));
        flow.enter();
        Assert.assertEquals(asked, List.of("chooseChest"));
        Assert.assertTrue(chest.isCompleted());
        Assert.assertEquals(run.getCurrentNode().getRowIndex(), 2);

        RogueRun saved = saved();
        Assert.assertEquals(saved.getActiveChestEffects(), List.of(ChestEffect.BANNER_OF_CHAMPIONS));
        Assert.assertTrue(saved.getPath().getNode(9).isCompleted());
        Assert.assertEquals(saved.getCurrentNodeIndex(), run.getCurrentNodeIndex());
    }

    @Test
    public void enteringAPlaneStartsOneMatchWithTheRunsDeckAndLife() {
        start(NodePlanebound.class);
        RogueMatch match = startedMatch();
        Assert.assertEquals(asked, List.of("startMatch"));
        Assert.assertEquals(match.human().getStartingLife(), 14);
        Assert.assertEquals(match.human().getDeck().getCommanders(), run.getCurrentDeck().getCommanders());
        Assert.assertEquals(match.human().getDeck().getMain(), run.getCurrentDeck().getMain());
    }

    @Test
    public void aWonBattlePaysAndLeavesTheMatch() {
        start(NodePlanebound.class);
        // The elite, which pays 4 gold and 4 echoes and adds a second pick of mythics
        run.setCurrentNodeIndex(10);
        List<PaperCard> chosen = new ArrayList<>();
        for (int cards : new int[] {7, 3}) {
            then("show", args -> {
                List<?> offered = (List<?>) args[1];
                Assert.assertEquals(offered.size(), cards);
                chosen.add((PaperCard) offered.get(0));
                return List.of(offered.get(0));
            });
        }
        RogueWinLoseController result = fight(true);
        Assert.assertEquals(asked, List.of("startMatch", "show", "show"));
        Assert.assertEquals(RogueMetaProgress.getInstance().getTotalEchoes(), 4);

        RogueRun saved = saved();
        Assert.assertEquals(saved.getCurrentGold(), 12);
        chosen.forEach(card -> Assert.assertEquals(saved.getCurrentDeck().getMain().count(card), 1, card.getName()));
        Assert.assertTrue(saved.getPath().getNode(10).isCompleted());
        Assert.assertEquals(saved.getCurrentNodeIndex(), 15);

        flow.resultLeft(result, false);
        Assert.assertNull(run.getHostedMatch());
    }

    @Test
    public void aLostBattleEndsTheRun() {
        NodePlanebound plane = (NodePlanebound) start(NodePlanebound.class);
        RogueWinLoseController result = fight(false);
        Assert.assertEquals(run.getRunState(), RogueRunState.LOST);
        Assert.assertEquals(onlyHistoryEntry().getOutcome(), "DEFEAT");
        Assert.assertEquals(onlyHistoryEntry().getBossOrDefeatedBy(), plane.getRoguePlanebound().planeboundName());
        flow.resultLeft(result, false);
        Assert.assertEquals(asked, List.of("startMatch", "runEnded"));
        Assert.assertNull(flow.run());
        Assert.assertTrue(flow.load().isEmpty());
    }

    @Test
    public void beatingTheBossWinsTheRun() {
        start(NodePlanebound.class);
        run.setCurrentNodeIndex(15);
        RogueWinLoseController result = fight(true);
        Assert.assertTrue(flow.offersEnding(result));
        flow.resultLeft(result, false);
        Assert.assertEquals(asked, List.of("startMatch", "runEnded"));
        Assert.assertEquals(run.getRunState(), RogueRunState.WON);
        Assert.assertEquals(onlyHistoryEntry().getOutcome(), "VICTORY");
        Assert.assertNull(flow.run());
        Assert.assertTrue(flow.load().isEmpty());
    }
}
