package forge.net;

import com.google.common.eventbus.Subscribe;
import forge.GuiDesktop;
import forge.Singletons;
import forge.game.GameType;
import forge.game.Game;
import forge.game.GameView;
import forge.game.GameLogEntry;
import forge.game.GameLogEntryType;
import forge.game.event.GameEventCardChangeZone;
import forge.game.event.GameEventShuffle;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.gamemodes.match.GameLobby.GameLobbyData;
import forge.gamemodes.match.HostedMatch;
import forge.gamemodes.match.LobbySlotType;
import forge.gamemodes.match.input.Input;
import forge.gamemodes.match.input.InputPassPriority;
import forge.gamemodes.match.input.InputPayMana;
import forge.gamemodes.match.input.InputConfirm;
import forge.gamemodes.match.input.InputSelectEntitiesFromList;
import forge.gamemodes.net.client.ClientGameLobby;
import forge.gamemodes.net.client.FGameClient;
import forge.gamemodes.net.client.NetGameController;
import forge.gamemodes.net.CompatibleObjectDecoder;
import forge.gamemodes.net.DeltaPacket;
import forge.gamemodes.net.ProtocolMethod;
import forge.gamemodes.net.event.GuiGameEvent;
import forge.gamemodes.net.event.UpdateLobbyPlayerEvent;
import forge.gamemodes.net.server.FServerManager;
import forge.gamemodes.net.server.RemoteClientGuiGame;
import forge.gamemodes.net.server.ServerGameLobby;
import forge.gui.FThreads;
import forge.gui.framework.EDocID;
import forge.gui.framework.DragCell;
import forge.gui.framework.IVDoc;
import forge.gui.framework.SDisplayUtil;
import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiGame;
import forge.interfaces.IGameController;
import forge.interfaces.ILobbyListener;
import forge.localinstance.properties.ForgeNetPreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.player.PlayerControllerHuman;
import forge.screens.match.CMatchUI;
import forge.screens.match.GameLogPanel;
import forge.screens.match.views.VField;
import forge.screens.match.views.VDetail;
import forge.screens.match.views.VHand;
import forge.trackable.Tracker;
import forge.trackable.TrackableTypes;
import forge.gui.download.CdnUuidCache;
import forge.interfaces.IUpdateable;
import forge.view.FView;
import forge.view.arcane.CardPanel;
import forge.view.arcane.CardArea;
import forge.view.arcane.FloatingZone;
import forge.toolbox.FOptionPane;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoop;
import forge.util.Localizer;
import forge.deck.Deck;
import forge.deck.DeckProxy;
import forge.game.phase.PhaseType;
import forge.game.spellability.SpellAbilityView;
import forge.game.spellability.StackItemView;
import forge.game.zone.ZoneType;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.swing.AbstractButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JTextArea;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.Window;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import forge.screens.match.views.VLog;
import forge.screens.match.views.VPrompt;

/** Two isolated desktop clients exercise the ordinary host/client DanDan UI path over Forge TCP. */
public final class DanDanTwoJvmMatchUiTest {
    private static final long CHILD_TIMEOUT_SECONDS = 90;
    private static final long WORKER_STARTUP_TIMEOUT_SECONDS = 180;
    private static final List<String> TEST_ADD_OPENS_PAYLOADS = List.of(
            "java.base/java.lang=ALL-UNNAMED",
            "java.base/java.time=ALL-UNNAMED",
            "java.base/java.text=ALL-UNNAMED",
            "java.base/java.util=ALL-UNNAMED",
            "java.base/java.util.regex=ALL-UNNAMED",
            "java.base/java.util.stream=ALL-UNNAMED",
            "java.base/java.lang.reflect=ALL-UNNAMED",
            "java.desktop/javax.imageio.spi=ALL-UNNAMED");

    @Test(timeOut = 420_000)
    public void bothDesktopSeatsRenderAndAnswerTheirOwnMulliganPrompt() throws Exception {
        Assert.assertFalse(GraphicsEnvironment.isHeadless(),
                "This rendered-client check requires the desktop display already used by DanDanMatchUiTest");

        final Path homes = Files.createTempDirectory("forge-dandan-two-jvm-");
        final int port = PortAllocator.allocatePort();
        WorkerProcess host = null;
        WorkerProcess remote = null;
        try {
            host = WorkerProcess.start("HOST", port, homes.resolve("host"));
            Assert.assertTrue(host.await("READY|HOST", Duration.ofSeconds(WORKER_STARTUP_TIMEOUT_SECONDS)),
                    "host process initialized its UI and started the local test server; "
                            + host.failureDiagnostic());
            assertModuleOpenReceipt(host, "HOST");

            remote = WorkerProcess.start("REMOTE", port, homes.resolve("remote"));
            Assert.assertEquals(remote.awaitPrefix("SYNC_MODE|REMOTE|",
                    Duration.ofSeconds(WORKER_STARTUP_TIMEOUT_SECONDS)), "SYNC_MODE|REMOTE|DELTA", workerPairDiagnostic(remote, host));
            final String hostChooser = assertWorkerSucceeded(host, remote,
                    "CHOOSER|HOST|", "APPLIED|HOST|MAIN1|");
            final String remoteChooser = assertWorkerSucceeded(remote, host,
                    "CHOOSER|REMOTE|", "APPLIED|REMOTE|MAIN1|");
            Assert.assertNotNull(hostChooser, "local host CMatchUI rendered and answered a typed mulligan chooser");
            Assert.assertNotNull(remoteChooser, "TCP client CMatchUI rendered and answered a typed mulligan chooser");
            assertModuleOpenReceipt(remote, "REMOTE");
            Assert.assertTrue(hostChooser.startsWith("CHOOSER|HOST|0|"), "host chooser belongs to seat zero");
            Assert.assertTrue(remoteChooser.startsWith("CHOOSER|REMOTE|1|"), "remote chooser belongs to seat one");
            Assert.assertTrue(hostChooser.endsWith("|FREE") || hostChooser.endsWith("|REGULAR"),
                    "host receipt contains only the mulligan prompt category");
            Assert.assertTrue(remoteChooser.endsWith("|FREE") || remoteChooser.endsWith("|REGULAR"),
                    "remote receipt contains only the mulligan prompt category");
            Assert.assertEquals(host.appliedReceipt.substring("APPLIED|HOST|MAIN1|".length()),
                    remote.appliedReceipt.substring("APPLIED|REMOTE|MAIN1|".length()),
                    "the two rendered clients report equal public pile counts and order fingerprint");
            System.out.println("RECEIPT|" + hostChooser);
            System.out.println("RECEIPT|" + remoteChooser);
            System.out.println("RECEIPT|" + host.appliedReceipt);
            System.out.println("RECEIPT|" + remote.appliedReceipt);
            host.command("CONCEDE");
            Assert.assertTrue(host.await("OUTCOME|HOST", Duration.ofSeconds(30)), "host produced an actual game outcome");
            System.out.println("RECEIPT|OUTCOME|HOST");
            remote.command("AWAIT_OUTCOME");
            Assert.assertTrue(remote.await("OUTCOME|REMOTE", Duration.ofSeconds(30)),
                    "remote rendered the server's actual game outcome before disconnect");
            System.out.println("RECEIPT|OUTCOME|REMOTE");
            host.command("QUIT");
            Assert.assertTrue(host.await("DONE|HOST", Duration.ofSeconds(30)), "host completed match teardown");
            System.out.println("RECEIPT|DONE|HOST");
            remote.command("RELEASE");
            Assert.assertTrue(remote.await("DONE|REMOTE", Duration.ofSeconds(30)));
            System.out.println("RECEIPT|DONE|REMOTE");
            Assert.assertTrue(host.process.waitFor(5, TimeUnit.SECONDS));
            Assert.assertTrue(remote.process.waitFor(5, TimeUnit.SECONDS));
            Assert.assertEquals(host.process.exitValue(), 0);
            Assert.assertEquals(remote.process.exitValue(), 0);
        } finally {
            if (remote != null) remote.stop();
            if (host != null) host.stop();
            deleteTree(homes);
        }
    }

    @Test(timeOut = 540_000)
    public void remoteRenderedLogDoesNotExposeHostsTellingTimeChoices() throws Exception {
        Assert.assertFalse(GraphicsEnvironment.isHeadless(),
                "This rendered-client check requires the desktop display already used by DanDanMatchUiTest");

        final Path homes = Files.createTempDirectory("forge-dandan-rendered-tt-");
        final int port = PortAllocator.allocatePort();
        WorkerProcess host = null;
        WorkerProcess remote = null;
        try {
            host = WorkerProcess.start("HOST", port, homes.resolve("host"), false, true);
            Assert.assertTrue(host.await("READY|HOST", Duration.ofSeconds(WORKER_STARTUP_TIMEOUT_SECONDS)), host.failureDiagnostic());
            remote = WorkerProcess.start("REMOTE", port, homes.resolve("remote"), false, true);
            Assert.assertEquals(remote.awaitPrefix("SYNC_MODE|REMOTE|",
                    Duration.ofSeconds(WORKER_STARTUP_TIMEOUT_SECONDS)), "SYNC_MODE|REMOTE|DELTA", workerPairDiagnostic(remote, host));

            final String hostChooser = assertWorkerSucceeded(host, remote,
                    "CHOOSER|HOST|", "APPLIED|HOST|MAIN1|");
            final String remoteChooser = assertWorkerSucceeded(remote, host,
                    "CHOOSER|REMOTE|", "APPLIED|REMOTE|MAIN1|");
            Assert.assertNotNull(hostChooser);
            Assert.assertNotNull(remoteChooser);
            Assert.assertTrue(host.openingPriorityReceipt != null
                    && host.openingPriorityReceipt.contains("|UNTAP=1|"),
                    "host passed its actual opening UNTAP priority");
            Assert.assertTrue(remote.openingPriorityReceipt != null
                    && remote.openingPriorityReceipt.contains("|UNTAP=1|"),
                    "remote passed its actual applied opening UNTAP priority");
            Assert.assertEquals(host.appliedReceipt.substring("APPLIED|HOST|MAIN1|".length()),
                    remote.appliedReceipt.substring("APPLIED|REMOTE|MAIN1|".length()));

            final String roleLine = host.awaitPrivateTellingTimeRoles(Duration.ofSeconds(5));
            Assert.assertNotNull(roleLine, "host retained bounded private setup roles for its own oracle");
            host.command("ARM_TELLING_TIME_LOG");
            remote.command("ARM_TELLING_TIME_LOG|" + roleLine);
            Assert.assertTrue(host.await("TT_ARMED|HOST", Duration.ofSeconds(10)), host.failureDiagnostic());
            Assert.assertTrue(remote.await("TT_ARMED|REMOTE", Duration.ofSeconds(10)), remote.failureDiagnostic());

            host.command("CAST_TELLING_TIME");
            remote.command("PASS_TELLING_TIME_STACK");
            final String remotePass = remote.awaitPrefix("TT_PASS|REMOTE|", Duration.ofSeconds(60));
            Assert.assertNotNull(remotePass, "remote passed the actual spell stack input; " + workerPairDiagnostic(remote, host));
            Assert.assertTrue(remotePass.endsWith("|STACK_PASS=1"),
                    "remote passed the actual spell stack input; " + workerPairDiagnostic(remote, host));
            final String hostCast = host.awaitPrefix("TT_CAST|HOST|", Duration.ofSeconds(60));
            Assert.assertNotNull(hostCast, "host completed the real cast and both Telling Time choices; "
                    + host.failureDiagnostic());
            remote.command("VERIFY_TELLING_TIME_LOG");
            final String remoteLog = remote.awaitPrefix("TT_LOG|REMOTE|", Duration.ofSeconds(60));
            Assert.assertNotNull(remoteLog, "remote inspected its actual rendered REPORT_LOG after resolution; "
                    + remote.failureDiagnostic());
            Assert.assertTrue(hostCast.contains("|RESOLVED=1|APPLIED=1|MANA_SOURCES=2|HOST_PASSES="));
            Assert.assertTrue(hostCast.endsWith("|PUBLIC_LOG=1|HAND_SENTINEL_ABSENT=1|CHOICES=2|INPUT_CHOICES=2|DUAL_LIST_CHOICES=0")
                    || hostCast.endsWith("|PUBLIC_LOG=1|HAND_SENTINEL_ABSENT=1|CHOICES=2|INPUT_CHOICES=1|DUAL_LIST_CHOICES=1")
                    || hostCast.endsWith("|PUBLIC_LOG=1|HAND_SENTINEL_ABSENT=1|CHOICES=2|INPUT_CHOICES=0|DUAL_LIST_CHOICES=2"));
            Assert.assertTrue(remoteLog.contains("|PUBLIC_COMPLETE=1|"));
            Assert.assertTrue(remoteLog.endsWith("|PUBLIC_CAST=1|PUBLIC_RESOLVE=1|PRIVATE_ABSENT=1"));

            host.command("CONCEDE");
            Assert.assertTrue(host.await("OUTCOME|HOST", Duration.ofSeconds(30)));
            remote.command("AWAIT_OUTCOME");
            Assert.assertTrue(remote.await("OUTCOME|REMOTE", Duration.ofSeconds(30)));
            host.command("QUIT");
            Assert.assertTrue(host.await("DONE|HOST", Duration.ofSeconds(30)));
            remote.command("RELEASE");
            Assert.assertTrue(remote.await("DONE|REMOTE", Duration.ofSeconds(30)));
            Assert.assertTrue(host.process.waitFor(5, TimeUnit.SECONDS));
            Assert.assertTrue(remote.process.waitFor(5, TimeUnit.SECONDS));
            Assert.assertEquals(host.process.exitValue(), 0);
            Assert.assertEquals(remote.process.exitValue(), 0);
        } finally {
            abortWorker(remote);
            abortWorker(host);
            if (remote != null) remote.stop();
            if (host != null) host.stop();
            deleteTree(homes);
        }
    }

    @Test(timeOut = 540_000, dataProvider = "renderedSyncModes")
    public void remoteRenderedTellingTimeChooserCastsAndKeepsChoicesPrivate(final String syncMode) throws Exception {
        Assert.assertFalse(GraphicsEnvironment.isHeadless(),
                "This rendered-client check requires the desktop display already used by DanDanMatchUiTest");

        final Path homes = Files.createTempDirectory("forge-dandan-rendered-tt-inverse-");
        final int port = PortAllocator.allocatePort();
        WorkerProcess host = null;
        WorkerProcess remote = null;
        try {
            host = WorkerProcess.start("HOST", port, homes.resolve("host"), true, true, true, syncMode);
            Assert.assertEquals(host.awaitPrefix("SYNC_MODE|HOST|", Duration.ofSeconds(WORKER_STARTUP_TIMEOUT_SECONDS)),
                    "SYNC_MODE|HOST|" + syncMode);
            Assert.assertTrue(host.await("READY|HOST", Duration.ofSeconds(WORKER_STARTUP_TIMEOUT_SECONDS)), host.failureDiagnostic());
            remote = WorkerProcess.start("REMOTE", port, homes.resolve("remote"), true, true, true, syncMode);
            Assert.assertEquals(remote.awaitPrefix("SYNC_MODE|REMOTE|", Duration.ofSeconds(WORKER_STARTUP_TIMEOUT_SECONDS)),
                    "SYNC_MODE|REMOTE|" + syncMode);

            final String hostSkipReady = host.awaitPrefix("INVERSE_PHASE_SKIP_READY|HOST|", Duration.ofSeconds(45));
            Assert.assertNotNull(hostSkipReady,
                    "host observed the remote controller's installed initial skip for host MAIN1; "
                            + workerPairDiagnostic(host, remote));
            Assert.assertTrue(hostSkipReady.endsWith("|INITIAL_SKIP=1"));
            final String remoteSkipReady = remote.awaitPrefix("INVERSE_PHASE_SKIP_READY|REMOTE|", Duration.ofSeconds(45));
            Assert.assertNotNull(remoteSkipReady,
                    "remote observed its current rendered host MAIN1 label disabled; "
                            + workerPairDiagnostic(remote, host));
            Assert.assertTrue(remoteSkipReady.endsWith("|LABEL_DISABLED=1"));
            remote.command("TOGGLE_HOST_MAIN1");
            final String remoteSkipToggled = remote.awaitPrefix("INVERSE_PHASE_SKIP_TOGGLED|REMOTE|",
                    Duration.ofSeconds(15));
            Assert.assertNotNull(remoteSkipToggled,
                    "remote toggled the actual rendered host MAIN1 phase label; "
                            + workerPairDiagnostic(remote, host));
            Assert.assertTrue(remoteSkipToggled.endsWith("|LABEL_ENABLED=1|UI_SKIP=0"));
            host.command("REMOTE_TOGGLE_SENT");
            final String hostSkipApplied = host.awaitPrefix("INVERSE_PHASE_SKIP_APPLIED|HOST|",
                    Duration.ofSeconds(15));
            Assert.assertNotNull(hostSkipApplied,
                    "host's remote PCH applied the remote UI's host MAIN1 unskip; "
                            + workerPairDiagnostic(host, remote));
            Assert.assertTrue(hostSkipApplied.endsWith("|INITIAL_SKIP=1|CURRENT_SKIP=0"));
            remote.command("HOST_SKIP_APPLIED");

            final String hostChooser = assertWorkerSucceeded(host, remote,
                    "CHOOSER|HOST|", "APPLIED|HOST|MAIN1|");
            final String remoteChooser = assertWorkerSucceeded(remote, host,
                    "CHOOSER|REMOTE|", "APPLIED|REMOTE|MAIN1|");
            Assert.assertNotNull(hostChooser);
            Assert.assertNotNull(remoteChooser);
            Assert.assertEquals(host.appliedReceipt.substring("APPLIED|HOST|MAIN1|".length()),
                    remote.appliedReceipt.substring("APPLIED|REMOTE|MAIN1|".length()));
            Assert.assertTrue(host.openingPriorityReceipt != null
                    && host.openingPriorityReceipt.contains("|UNTAP=1|"));
            Assert.assertTrue(remote.openingPriorityReceipt != null
                    && remote.openingPriorityReceipt.contains("|UNTAP=1|"));

            final String privateRoles = host.awaitPrivateTellingTimeRoles(Duration.ofSeconds(5));
            Assert.assertNotNull(privateRoles, "host retained bounded private setup roles for its own oracle");
            host.command("ARM_INVERSE_TELLING_TIME_LOG");
            remote.command("ARM_INVERSE_TELLING_TIME_LOG|" + privateRoles);
            Assert.assertTrue(host.await("TT_INVERSE_ARMED|HOST", Duration.ofSeconds(10)), host.failureDiagnostic());
            Assert.assertTrue(remote.await("TT_INVERSE_ARMED|REMOTE", Duration.ofSeconds(10)), remote.failureDiagnostic());

            host.command("DRIVE_INVERSE_TELLING_TIME");
            remote.command("DRIVE_INVERSE_TELLING_TIME");
            final String hostRun = host.awaitPrefix("TT_INVERSE_HOST|", Duration.ofSeconds(90));
            final String remoteRun = remote.awaitPrefix("TT_INVERSE_REMOTE|", Duration.ofSeconds(90));
            Assert.assertNotNull(hostRun, "host passed actual priority and inspected its rendered log; "
                    + workerPairDiagnostic(host, remote));
            Assert.assertNotNull(remoteRun, "remote cast, paid, chose, and inspected its rendered log; "
                    + workerPairDiagnostic(remote, host));
            Assert.assertTrue(hostRun.endsWith("|HOST_PASS=1|PUBLIC_CAST=1|PUBLIC_RESOLVE=1|PRIVATE_ABSENT=1|HAND_SENTINEL_ABSENT=1|CARDS=80"));
            Assert.assertTrue(remoteRun.endsWith("|REMOTE_CAST=1|MANA_SOURCES=2|CHOICES=2|RECIPIENT=1|TRACKER=1|PUBLIC_CAST=1|PUBLIC_RESOLVE=1"));

            host.command("DRIVE_INVERSE_FENGRAF");
            remote.command("DRIVE_INVERSE_FENGRAF");
            final String hostFengraf = host.awaitPrefix("FENGRAF_HOST|", Duration.ofSeconds(70));
            final String remoteFengraf = remote.awaitPrefix("FENGRAF_REMOTE|", Duration.ofSeconds(70));
            Assert.assertNotNull(hostFengraf, "host acknowledged the actual public Fengraf reveal and hid its temporary hand panel; "
                    + workerPairDiagnostic(host, remote));
            Assert.assertNotNull(remoteFengraf, "remote used the actual recipient hand panel and detail hover; "
                    + workerPairDiagnostic(remote, host));
            Assert.assertTrue(hostFengraf.endsWith("|REMOTE_REVEAL_ACK=1|PANEL_HIDDEN=1|PRIVATE_HAND_ABSENT=1|CARDS=80"));
            Assert.assertTrue(remoteFengraf.endsWith("|ACTUAL_ABILITY=1|MANA_SOURCES=3|SACRIFICE=1|RECIPIENT=1"
                    + "|HAND_HOVER=1|DETAIL=1"));

            if ("FULL".equals(syncMode)) {
                final String fullState = remote.awaitPrefix("FULL_STATE|REMOTE|", Duration.ofSeconds(5));
                Assert.assertNotNull(fullState, "remote captured post-setup full-state callbacks before Day's Undoing");
                Assert.assertTrue(fullState.matches("FULL_STATE\\|REMOTE\\|FULL_SNAPSHOTS=[1-9][0-9]*"
                                + "\\|POPULATED=1\\|WRONG_GAME=0\\|EVENT_ONLY=[1-9][0-9]*"
                                + "\\|UNEXPECTED_DELTAS=0\\|ERROR=0"),
                        "remote decoded populated current-game full snapshots and no unexpected state deltas: "
                                + fullState);
            }

            host.command("ADVANCE_REMOTE_T1_MAIN1");
            final String hostRemoteT1 = host.awaitPrefix("DAY_REMOTE_T1_READY|HOST|", Duration.ofSeconds(70));
            Assert.assertNotNull(hostRemoteT1,
                    "host held the exact remote T1 MAIN1 input before the remote End Turn response; "
                            + workerPairDiagnostic(host, remote));
            Assert.assertEquals(hostRemoteT1, "DAY_REMOTE_T1_READY|HOST|TURN1_MAIN1=1|REMOTE_PRIORITY=1");
            remote.command("END_REMOTE_T1_MAIN1");
            final String remoteEndTurn = remote.awaitPrefix("DAY_REMOTE_ENDTURN|REMOTE|", Duration.ofSeconds(20));
            Assert.assertNotNull(remoteEndTurn,
                    "remote used its actual visible T1 MAIN1 End Turn control once; "
                            + workerPairDiagnostic(remote, host));
            Assert.assertEquals(remoteEndTurn,
                    "DAY_REMOTE_ENDTURN|REMOTE|TURN1_MAIN1=1|VISIBLE_END_TURN=1|DISPATCHED=1");
            host.command("ADVANCE_REMOTE_T2_MAIN1");
            remote.command("AWAIT_REMOTE_T2_MAIN1");
            final String hostDayPriority = host.awaitPrefix("DAY_PRIORITY_READY|HOST|", Duration.ofSeconds(70));
            Assert.assertNotNull(hostDayPriority,
                    "host passed only its actual empty-stack priority inputs through T2 MAIN1; "
                            + workerPairDiagnostic(host, remote));
            Assert.assertEquals(hostDayPriority,
                    "DAY_PRIORITY_READY|HOST|TURN2_MAIN1=1|REMOTE_PRIORITY=1|T1_INPUT_CONSUMED=1");
            final String remoteDayPriority = remote.awaitPrefix("DAY_PRIORITY_READY|REMOTE|", Duration.ofSeconds(70));
            Assert.assertNotNull(remoteDayPriority,
                    "remote retained the same applied view and current T2 MAIN1 priority without another click; "
                            + workerPairDiagnostic(remote, host));
            host.command("ARM_DAY_SHUFFLE");
            host.command("DRIVE_DAY_UNDOING");
            Assert.assertTrue(host.await("DAY_CAPTURE_ACTIVE|HOST", Duration.ofSeconds(10)), host.failureDiagnostic());
            remote.command("DRIVE_DAY_UNDOING");
            host.command("BEGIN_DAY_UNDOING");
            final String hostDay = host.awaitPrefix("DAY_HOST|", Duration.ofSeconds(90));
            final String remoteDay = remote.awaitPrefix("DAY_REMOTE|", Duration.ofSeconds(90));
            Assert.assertNotNull(hostDay,
                    "host observed both actual Day's Undoing shuffle events and T3 MAIN1; "
                            + workerPairDiagnostic(host, remote));
            Assert.assertNotNull(remoteDay,
                    "remote cast, paid, and rendered Day's Undoing through its real UI; "
                            + workerPairDiagnostic(remote, host));
            final String[] remoteDayFields = remoteDay.split("\\|", -1);
            Assert.assertEquals(remoteDayFields.length, 8);
            Assert.assertEquals(String.join("|", java.util.Arrays.copyOf(remoteDayFields, 7)),
                    "DAY_REMOTE|REMOTE_CAST=1|MANA_SOURCES=3|PUBLIC_CAST=1|PUBLIC_RESOLVE=1"
                            + "|OPPONENT_UNVIEWABLE=1|CARDS=80");
            final String[] hostDayFields = hostDay.split("\\|", -1);
            Assert.assertEquals(hostDayFields.length, 10);
            Assert.assertTrue(hostDay.startsWith("DAY_HOST|SHUFFLES=2|HOST_SHUFFLE=1|REMOTE_SHUFFLE=1"
                    + "|DAY_DRAWS_HOST=7|DAY_DRAWS_REMOTE=7|HOST_T3_DRAW=1|T3_MAIN1=1|CARDS=80|"
                    + "HOST_HAND_HASHES="));
            Assert.assertTrue(remoteDayFields[7].startsWith("REMOTE_KNOWN_HASHES="));
            final String knowledgeRecord = remoteDayFields[7].substring("REMOTE_KNOWN_HASHES=".length());
            Assert.assertTrue(knowledgeRecord.matches("[0-9a-f]{64}(,[0-9a-f]{64})*"));
            final Set<String> currentKnownNameHashes = Set.of(knowledgeRecord.split(","));
            final String handHashes = hostDayFields[9].substring("HOST_HAND_HASHES=".length());
            final String unknownNameWitness = java.util.Arrays.stream(handHashes.split(","))
                    .filter(item -> {
                        final String[] parts = item.split(":", -1);
                        return parts.length == 2 && parts[0].matches("[0-9a-f]{64}")
                                && !currentKnownNameHashes.contains(parts[0])
                                && parts[1].matches("[1-9][0-9]*");
                    }).findFirst().orElseThrow(() -> new AssertionError(
                            "the current host hand contains a name outside the remote's current earned knowledge"));
            final String[] unknownNameParts = unknownNameWitness.split(":", -1);
            final String unknownNameHash = unknownNameParts[0];
            final int unknownNameLength = Integer.parseInt(unknownNameParts[1]);
            remote.command("CHECK_DAY_LOG_NAME|" + unknownNameLength + "|" + unknownNameHash);
            Assert.assertTrue(remote.await("DAY_LOG_PRIVACY|UNKNOWN_CURRENT_HAND_NAME_ABSENT=1",
                    Duration.ofSeconds(10)), remote.failureDiagnostic());
            host.command("HOLD_INVERSE_RECONNECT_PRIORITY");
            Assert.assertTrue(host.await("RECONNECT_HOLD|HOST", Duration.ofSeconds(10)),
                    "host retained the exact live priority input before the remote reconnect; "
                            + workerPairDiagnostic(host, remote));
            remote.command("RECONNECT_AND_VERIFY_RENDERED_HISTORY|" + unknownNameLength + "|" + unknownNameHash);
            final String reconnect = remote.awaitPrefix("RECONNECT_HISTORY|REMOTE|", Duration.ofSeconds(100));
            Assert.assertNotNull(reconnect, "the same remote UI reconnected and rendered replayed history; "
                    + workerPairDiagnostic(remote, host));
            Assert.assertTrue(reconnect.endsWith("|SAME_GAME=1|SAME_TRACKER=1|NEW_CHANNEL=1|NEW_FIELD=1"
                    + "|REPLAYED_TT=1|REPLAYED_FENGRAF=1|REPLAYED_DAY=1|PRIVATE_ABSENT=1"
                    + "|UNKNOWN_CURRENT_HAND_NAME_ABSENT=1"), reconnect);
            host.command("RELEASE_INVERSE_RECONNECT_PRIORITY");
            Assert.assertTrue(host.await("RECONNECT_RELEASED|HOST", Duration.ofSeconds(10)),
                    "host held the same exact input throughout remote resynchronization");
            host.command("CONCEDE");
            Assert.assertTrue(host.await("OUTCOME|HOST", Duration.ofSeconds(30)));
            remote.command("AWAIT_OUTCOME");
            Assert.assertTrue(remote.await("OUTCOME|REMOTE", Duration.ofSeconds(30)));
            host.command("CONTINUE");
            remote.command("CONTINUE");
            final String hostContinue = host.awaitPrefix("N1CONTINUE|HOST|", Duration.ofSeconds(30));
            final String remoteContinue = remote.awaitPrefix("N1CONTINUE|REMOTE|", Duration.ofSeconds(30));
            Assert.assertNotNull(hostContinue, "host clicked the visible Next Game control after reconnect; "
                    + host.failureDiagnostic());
            Assert.assertNotNull(remoteContinue, "same remote UI clicked the visible Next Game control; "
                    + remote.failureDiagnostic());
            Assert.assertEquals(hostContinue.substring("N1CONTINUE|HOST|".length()),
                    remoteContinue.substring("N1CONTINUE|REMOTE|".length()),
                    "both clients continued from the same Game N view identity");
            final String hostNextChooser = host.awaitPrefix("N1CHOOSER|HOST|", Duration.ofSeconds(90));
            final String remoteNextChooser = remote.awaitPrefix("N1CHOOSER|REMOTE|", Duration.ofSeconds(90));
            Assert.assertNotNull(hostNextChooser, "host answered its actual N+1 chooser; "
                    + workerPairDiagnostic(host, remote));
            Assert.assertNotNull(remoteNextChooser, "remote answered its actual N+1 chooser; "
                    + workerPairDiagnostic(remote, host));
            Assert.assertTrue(hostNextChooser.startsWith("N1CHOOSER|HOST|0|START=1|"),
                    "the losing host seat owned the N+1 Play/Draw prompt");
            Assert.assertTrue(remoteNextChooser.startsWith("N1CHOOSER|REMOTE|1|START=0|"),
                    "the remote winner waited rather than receiving the N+1 Play/Draw prompt");
            Assert.assertTrue(hostNextChooser.endsWith("|FREE") || hostNextChooser.endsWith("|REGULAR"));
            Assert.assertTrue(remoteNextChooser.endsWith("|FREE") || remoteNextChooser.endsWith("|REGULAR"));
            final String hostNextApplied = host.awaitPrefix("N1APPLIED|HOST|MAIN1|", Duration.ofSeconds(90));
            final String remoteNextApplied = remote.awaitPrefix("N1APPLIED|REMOTE|MAIN1|", Duration.ofSeconds(90));
            Assert.assertNotNull(hostNextApplied, "host applied N+1 through the rendered UI; "
                    + workerPairDiagnostic(host, remote));
            Assert.assertNotNull(remoteNextApplied, "remote applied N+1 through the same rendered UI; "
                    + workerPairDiagnostic(remote, host));
            Assert.assertEquals(hostNextApplied.substring("N1APPLIED|HOST|MAIN1|".length()),
                    remoteNextApplied.substring("N1APPLIED|REMOTE|MAIN1|".length()),
                    "both clients applied the same N+1 view and canonical public-pile order");
            host.command("CONCEDE_N1");
            Assert.assertTrue(host.await("N1OUTCOME|HOST", Duration.ofSeconds(30)),
                    "the actual N+1 host Game produced an outcome and returned");
            remote.command("AWAIT_N1_OUTCOME");
            Assert.assertTrue(remote.await("N1OUTCOME|REMOTE", Duration.ofSeconds(30)),
                    "remote rendered the actual N+1 outcome after the same-UI history reset");
            host.command("QUIT");
            Assert.assertTrue(host.await("DONE|HOST", Duration.ofSeconds(30)));
            remote.command("RELEASE");
            Assert.assertTrue(remote.await("DONE|REMOTE", Duration.ofSeconds(30)));
            Assert.assertTrue(host.process.waitFor(5, TimeUnit.SECONDS));
            Assert.assertTrue(remote.process.waitFor(5, TimeUnit.SECONDS));
            Assert.assertEquals(host.process.exitValue(), 0);
            Assert.assertEquals(remote.process.exitValue(), 0);
        } finally {
            abortWorker(remote);
            abortWorker(host);
            if (remote != null) remote.stop();
            if (host != null) host.stop();
            deleteTree(homes);
        }
    }

    @DataProvider(name = "renderedSyncModes")
    public Object[][] renderedSyncModes() {
        return new Object[][]{{"DELTA"}, {"FULL"}};
    }

    @Test(timeOut = 540_000)
    public void bothDesktopSeatsContinueAndRenderTheNextDanDanGame() throws Exception {
        Assert.assertFalse(GraphicsEnvironment.isHeadless(),
                "This rendered-client check requires the desktop display already used by DanDanMatchUiTest");

        final Path homes = Files.createTempDirectory("forge-dandan-next-game-");
        final int port = PortAllocator.allocatePort();
        WorkerProcess host = null;
        WorkerProcess remote = null;
        try {
            host = WorkerProcess.start("HOST", port, homes.resolve("host"), true);
            Assert.assertTrue(host.await("READY|HOST", Duration.ofSeconds(WORKER_STARTUP_TIMEOUT_SECONDS)),
                    "host process initialized its UI and started the local test server; "
                            + host.failureDiagnostic());
            assertModuleOpenReceipt(host, "HOST");
            remote = WorkerProcess.start("REMOTE", port, homes.resolve("remote"), true);
            Assert.assertEquals(remote.awaitPrefix("SYNC_MODE|REMOTE|",
                    Duration.ofSeconds(WORKER_STARTUP_TIMEOUT_SECONDS)), "SYNC_MODE|REMOTE|DELTA", workerPairDiagnostic(remote, host));

            final String hostChooser = assertWorkerSucceeded(host, remote,
                    "CHOOSER|HOST|", "APPLIED|HOST|MAIN1|");
            final String remoteChooser = assertWorkerSucceeded(remote, host,
                    "CHOOSER|REMOTE|", "APPLIED|REMOTE|MAIN1|");
            Assert.assertNotNull(hostChooser);
            Assert.assertNotNull(remoteChooser);
            assertModuleOpenReceipt(remote, "REMOTE");
            Assert.assertTrue(hostChooser.startsWith("CHOOSER|HOST|0|"));
            Assert.assertTrue(remoteChooser.startsWith("CHOOSER|REMOTE|1|"));
            Assert.assertEquals(host.appliedReceipt.substring("APPLIED|HOST|MAIN1|".length()),
                    remote.appliedReceipt.substring("APPLIED|REMOTE|MAIN1|".length()),
                    "Game N rendered clients have equal canonical public-pile receipts");

            host.command("CONCEDE");
            Assert.assertTrue(host.await("OUTCOME|HOST", Duration.ofSeconds(30)),
                    "host Game N produced an actual outcome and engine return");
            remote.command("AWAIT_OUTCOME");
            Assert.assertTrue(remote.await("OUTCOME|REMOTE", Duration.ofSeconds(30)),
                    "remote rendered Game N's actual outcome");

            host.command("CONTINUE");
            remote.command("CONTINUE");
            final String hostContinue = host.awaitPrefix("N1CONTINUE|HOST|", Duration.ofSeconds(30));
            final String remoteContinue = remote.awaitPrefix("N1CONTINUE|REMOTE|", Duration.ofSeconds(30));
            Assert.assertNotNull(hostContinue, "host clicked the visible Next Game control; "
                    + host.failureDiagnostic());
            Assert.assertNotNull(remoteContinue, "remote clicked the visible Next Game control; "
                    + remote.failureDiagnostic());
            Assert.assertEquals(hostContinue.substring("N1CONTINUE|HOST|".length()),
                    remoteContinue.substring("N1CONTINUE|REMOTE|".length()),
                    "both clients continued from the same Game N view identity");
            final String hostNextChooser = host.awaitPrefix("N1CHOOSER|HOST|", Duration.ofSeconds(90));
            final String remoteNextChooser = remote.awaitPrefix("N1CHOOSER|REMOTE|", Duration.ofSeconds(90));
            Assert.assertNotNull(hostNextChooser, "host answered its actual N+1 chooser; "
                    + workerPairDiagnostic(host, remote));
            Assert.assertNotNull(remoteNextChooser, "remote answered its actual N+1 chooser; "
                    + workerPairDiagnostic(remote, host));
            Assert.assertTrue(hostNextChooser.startsWith("N1CHOOSER|HOST|0|START=1|"),
                    "the losing host seat owned the N+1 Play/Draw prompt");
            Assert.assertTrue(remoteNextChooser.startsWith("N1CHOOSER|REMOTE|1|START=0|"),
                    "the remote winner waited rather than receiving the N+1 Play/Draw prompt");
            Assert.assertTrue(hostNextChooser.endsWith("|FREE") || hostNextChooser.endsWith("|REGULAR"));
            Assert.assertTrue(remoteNextChooser.endsWith("|FREE") || remoteNextChooser.endsWith("|REGULAR"));
            final String hostNextApplied = host.awaitPrefix("N1APPLIED|HOST|MAIN1|", Duration.ofSeconds(90));
            final String remoteNextApplied = remote.awaitPrefix("N1APPLIED|REMOTE|MAIN1|", Duration.ofSeconds(90));
            Assert.assertNotNull(hostNextApplied, "host applied N+1 through the rendered UI; "
                    + workerPairDiagnostic(host, remote));
            Assert.assertNotNull(remoteNextApplied, "remote applied N+1 through the rendered UI; "
                    + workerPairDiagnostic(remote, host));
            Assert.assertEquals(hostNextApplied.substring("N1APPLIED|HOST|MAIN1|".length()),
                    remoteNextApplied.substring("N1APPLIED|REMOTE|MAIN1|".length()),
                    "both clients applied the same N+1 view and canonical public-pile order");
            System.out.println("RECEIPT|" + hostContinue);
            System.out.println("RECEIPT|" + remoteContinue);
            System.out.println("RECEIPT|" + hostNextChooser);
            System.out.println("RECEIPT|" + remoteNextChooser);
            System.out.println("RECEIPT|" + hostNextApplied);
            System.out.println("RECEIPT|" + remoteNextApplied);

            host.command("CONCEDE_N1");
            Assert.assertTrue(host.await("N1OUTCOME|HOST", Duration.ofSeconds(30)),
                    "the actual N+1 host Game produced an outcome and returned");
            remote.command("AWAIT_N1_OUTCOME");
            Assert.assertTrue(remote.await("N1OUTCOME|REMOTE", Duration.ofSeconds(30)),
                    "remote rendered the actual N+1 outcome before disconnect");
            host.command("QUIT");
            Assert.assertTrue(host.await("DONE|HOST", Duration.ofSeconds(30)),
                    "current N+1 host controller completed terminal QUIT cleanup");
            remote.command("RELEASE");
            Assert.assertTrue(remote.await("DONE|REMOTE", Duration.ofSeconds(30)));
            Assert.assertTrue(host.process.waitFor(5, TimeUnit.SECONDS));
            Assert.assertTrue(remote.process.waitFor(5, TimeUnit.SECONDS));
            Assert.assertEquals(host.process.exitValue(), 0);
            Assert.assertEquals(remote.process.exitValue(), 0);
        } finally {
            abortWorker(remote);
            abortWorker(host);
            if (remote != null) remote.stop();
            if (host != null) host.stop();
            deleteTree(homes);
        }
    }

    private static void abortWorker(final WorkerProcess worker) {
        if (worker == null || !worker.process.isAlive()) return;
        try {
            worker.command("ABORT");
        } catch (IOException ignored) {
            // The child may already have closed stdin; stop() remains the bounded fallback.
        }
    }

    private static String assertWorkerSucceeded(final WorkerProcess worker, final WorkerProcess peer,
            final String chooserPrefix,
            final String appliedReceipt) throws Exception {
        final String chooser = worker.awaitPrefix(chooserPrefix, Duration.ofSeconds(CHILD_TIMEOUT_SECONDS));
        Assert.assertNotNull(chooser, "worker reached and answered its own mulligan prompt; "
                + workerPairDiagnostic(worker, peer));
        worker.appliedReceipt = worker.awaitPrefix(appliedReceipt, Duration.ofSeconds(CHILD_TIMEOUT_SECONDS));
        Assert.assertNotNull(worker.appliedReceipt, "worker applied the next public state through its real client UI; "
                + workerPairDiagnostic(worker, peer));
        Assert.assertNull(worker.failure(), "child reported a bounded scalar failure; "
                + workerPairDiagnostic(worker, peer));
        return chooser;
    }

    private static String workerPairDiagnostic(final WorkerProcess worker, final WorkerProcess peer) {
        return "worker{" + worker.failureDiagnostic() + "};peer{" + peer.failureDiagnostic() + "}";
    }

    private static void deleteTree(final Path root) throws IOException {
        if (root == null || !Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Collections.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }

    private static void assertModuleOpenReceipt(final WorkerProcess worker, final String role) {
        final String receipt = worker.moduleOpenReceipt;
        Assert.assertNotNull(receipt, role + " worker reported bounded module-open scalars; "
                + worker.failureDiagnostic());
        Assert.assertTrue(receipt.contains("|PARENT_UTIL_OPEN=1|"),
                role + " parent had the expected java.util test open; " + receipt);
        Assert.assertTrue(receipt.endsWith("|CHILD_UTIL_OPEN=1"),
                role + " child inherited the java.util test open; " + receipt);
        final int copied = Integer.parseInt(receipt.substring(receipt.indexOf("|COPIED=") + 8,
                receipt.indexOf("|CHILD_UTIL_OPEN=")));
        Assert.assertTrue(copied > 0 && copied <= TEST_ADD_OPENS_PAYLOADS.size(),
                role + " copied only active whitelist opens; " + receipt);
        System.out.println("RECEIPT|" + receipt);
    }

    private static List<String> activeTestAddOpens() {
        final List<String> active = new ArrayList<>();
        final List<String> arguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
        for (int i = 0; i < arguments.size(); i++) {
            final String argument = arguments.get(i);
            final String payload;
            if ("--add-opens".equals(argument) && i + 1 < arguments.size()) {
                payload = arguments.get(++i);
            } else if (argument.startsWith("--add-opens=")) {
                payload = argument.substring("--add-opens=".length());
            } else {
                continue;
            }
            if (TEST_ADD_OPENS_PAYLOADS.contains(payload)) {
                final String option = "--add-opens=" + payload;
                if (!active.contains(option)) active.add(option);
            }
        }
        return active;
    }

    private static boolean isParentJavaUtilOpen() {
        return Object.class.getModule().isOpen("java.util", Worker.class.getModule());
    }

    private static final class WorkerProcess {
        private final Process process;
        private final BlockingQueue<String> records = new LinkedBlockingQueue<>();
        private volatile String failure;
        private volatile String appliedReceipt;
        private volatile String moduleOpenReceipt;
        private volatile String uiProgress;
        private volatile String n1Progress;
        private volatile String privateTellingTimeRoles;
        private volatile String openingPriorityReceipt;
        private volatile String tellingTimeArmStage;
        private volatile String startupStage;
        private volatile String tellingTimeCastStage;
        private volatile String tellingTimeResolutionDiagnostic;
        private volatile String inverseTellingTimeProgress;

        private WorkerProcess(final Process process) {
            this.process = process;
            Thread reader = new Thread(() -> {
                try (BufferedReader input = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = input.readLine()) != null) {
                        if (line.startsWith("TT_PRIVATE_ROLES|")) {
                            privateTellingTimeRoles = line.substring("TT_PRIVATE_ROLES|".length());
                            continue;
                        }
                        if (line.startsWith("FAIL|")) failure = line;
                        if (line.startsWith("OPEN|")) moduleOpenReceipt = line;
                        if (line.startsWith("UI|")) uiProgress = line;
                        if (line.startsWith("N1STEP|")) n1Progress = line;
                        if (line.startsWith("OPENING_PRIORITY|")) openingPriorityReceipt = line;
                        if (line.startsWith("ARM_STAGE|")) tellingTimeArmStage = line;
                        if (line.startsWith("STARTUP_STAGE|")) startupStage = line;
                        if (line.startsWith("TT_CAST_STAGE|")) tellingTimeCastStage = line;
                        if (line.startsWith("TT_RESOLUTION_DIAG|")) tellingTimeResolutionDiagnostic = line;
                        if (line.startsWith("TT_INVERSE_PROGRESS|")) inverseTellingTimeProgress = line;
                        if (line.startsWith("TT_INVERSE_ARMED|") || line.startsWith("TT_INVERSE_HOST|")
                                || line.startsWith("TT_INVERSE_REMOTE|")
                                || line.startsWith("FENGRAF_HOST|") || line.startsWith("FENGRAF_REMOTE|")
                                || line.startsWith("DAY_REMOTE_T1_READY|")
                                || line.startsWith("DAY_REMOTE_ENDTURN|")
                                || line.startsWith("DAY_PRIORITY_READY|")
                                || line.startsWith("DAY_CAPTURE_ACTIVE|") || line.startsWith("DAY_HOST|")
                                || line.startsWith("DAY_REMOTE|") || line.startsWith("DAY_LOG_PRIVACY|")
                                || line.startsWith("RECONNECT_HISTORY|")) {
                            records.offer(line);
                        }
                        if (line.startsWith("READY|") || line.startsWith("CHOOSER|")
                                || line.startsWith("APPLIED|") || line.startsWith("OUTCOME|")
                                || line.startsWith("N1CONTINUE|") || line.startsWith("N1CHOOSER|")
                                || line.startsWith("N1APPLIED|") || line.startsWith("N1OUTCOME|")
                                || line.startsWith("DONE|") || line.startsWith("FAIL|")
                                || line.startsWith("TT_ARMED|") || line.startsWith("TT_PASS|")
                                || line.startsWith("TT_CAST|") || line.startsWith("TT_LOG|")) {
                            records.offer(line);
                        }
                        if (line.startsWith("INVERSE_PHASE_SKIP_READY|")
                                || line.startsWith("INVERSE_PHASE_SKIP_TOGGLED|")
                                || line.startsWith("INVERSE_PHASE_SKIP_APPLIED|")
                                || line.startsWith("SYNC_MODE|")
                                || line.startsWith("FULL_STATE|")
                                || line.startsWith("RECONNECT_HOLD|")
                                || line.startsWith("RECONNECT_RELEASED|")) {
                            records.offer(line);
                        }
                    }
                } catch (IOException ignored) {
                    // Process shutdown closes the stream; its bounded exit status is checked by the parent.
                }
            }, "dandan-two-jvm-output");
            reader.setDaemon(true);
            reader.start();
        }

        static WorkerProcess start(final String role, final int port, final Path home) throws IOException {
            return start(role, port, home, false);
        }

        static WorkerProcess start(final String role, final int port, final Path home,
                final boolean continueToNextGame) throws IOException {
            return start(role, port, home, continueToNextGame, false);
        }

        static WorkerProcess start(final String role, final int port, final Path home,
                final boolean continueToNextGame, final boolean renderedTellingTime) throws IOException {
            return start(role, port, home, continueToNextGame, renderedTellingTime, false);
        }

        static WorkerProcess start(final String role, final int port, final Path home,
                final boolean continueToNextGame, final boolean renderedTellingTime,
                final boolean inverseRenderedTellingTime) throws IOException {
            return start(role, port, home, continueToNextGame, renderedTellingTime,
                    inverseRenderedTellingTime, "DELTA");
        }

        static WorkerProcess start(final String role, final int port, final Path home,
                final boolean continueToNextGame, final boolean renderedTellingTime,
                final boolean inverseRenderedTellingTime, final String syncMode) throws IOException {
            Files.createDirectories(home);
            final String java = System.getProperty("java.home") + System.getProperty("file.separator") + "bin"
                    + System.getProperty("file.separator") + "java";
            final List<String> activeOpens = activeTestAddOpens();
            final boolean parentJavaUtilOpen = isParentJavaUtilOpen();
            final List<String> command = new ArrayList<>(List.of(java,
                    "-Duser.home=" + home.toAbsolutePath()));
            command.addAll(activeOpens);
            command.addAll(List.of("-cp", System.getProperty("java.class.path"),
                    Worker.class.getName(), role, Integer.toString(port),
                    parentJavaUtilOpen ? "1" : "0", Integer.toString(activeOpens.size()),
                    continueToNextGame ? "NPLUS1" : "FIRST_ONLY",
                    inverseRenderedTellingTime ? "INVERSE_RENDERED_TELLING_TIME"
                            : renderedTellingTime ? "RENDERED_TELLING_TIME" : "STANDARD", syncMode));
            ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
            // Windows resolves Forge's profile and cache through these environment variables.
            builder.environment().put("APPDATA", home.resolve("AppData/Roaming").toAbsolutePath().toString());
            builder.environment().put("LOCALAPPDATA", home.resolve("AppData/Local").toAbsolutePath().toString());
            return new WorkerProcess(builder.start());
        }

        String awaitPrefix(final String prefix, final Duration timeout) throws InterruptedException {
            final long deadline = System.nanoTime() + timeout.toNanos();
            while (System.nanoTime() < deadline) {
                String record = records.poll(Math.max(1, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
                if (record == null) break;
                if (record.startsWith("FAIL|")) return null;
                if (record.startsWith(prefix)) return record;
            }
            return null;
        }

        boolean await(final String expected, final Duration timeout) throws InterruptedException {
            final long deadline = System.nanoTime() + timeout.toNanos();
            while (System.nanoTime() < deadline) {
                String record = records.poll(Math.max(1, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
                if (record == null) break;
                if (record.equals(expected)) return true;
                if (record.startsWith("FAIL|")) return false;
            }
            return false;
        }

        String failure() { return failure; }

        String awaitPrivateTellingTimeRoles(final Duration timeout) throws InterruptedException {
            final long deadline = System.nanoTime() + timeout.toNanos();
            while (System.nanoTime() < deadline && privateTellingTimeRoles == null) Thread.sleep(10);
            return privateTellingTimeRoles;
        }

        String failureDiagnostic() {
            final String state = (failure != null ? failure : process.isAlive()
                    ? "no-failure-record;child-alive" : "no-failure-record;child-exit-" + process.exitValue())
                    + (uiProgress == null ? "" : ";" + uiProgress);
            return state + (startupStage == null ? "" : ";" + startupStage) + (n1Progress == null ? "" : ";" + n1Progress)
                    + (tellingTimeArmStage == null ? "" : ";" + tellingTimeArmStage)
                    + (tellingTimeCastStage == null ? "" : ";" + tellingTimeCastStage)
                    + (tellingTimeResolutionDiagnostic == null ? "" : ";" + tellingTimeResolutionDiagnostic)
                    + (inverseTellingTimeProgress == null ? "" : ";" + inverseTellingTimeProgress);
        }

        void command(final String command) throws IOException {
            process.getOutputStream().write((command + "\n").getBytes(StandardCharsets.UTF_8));
            process.getOutputStream().flush();
        }

        void stop() throws InterruptedException {
            if (!process.waitFor(1, TimeUnit.SECONDS)) process.destroy();
            if (!process.waitFor(3, TimeUnit.SECONDS)) process.destroyForcibly();
            process.waitFor(3, TimeUnit.SECONDS);
        }
    }

    /** Separate-JVM entry point. Protocol output is scalar; Forge diagnostics stay in the child profile. */
    public static final class Worker {
        private static final PrintStream RESULT = System.out;
        private static final long UI_TIMEOUT_SECONDS = 60;
        private static BufferedReader WORKER_COMMANDS;
        private static final String HOST_NAME = "Local DanDan seat";
        private static final String REMOTE_NAME = "Remote DanDan seat";
        private static final AtomicReference<Throwable> FIRST_EDT_FAILURE = new AtomicReference<>();
        private static final AtomicReference<String> BOOTSTRAP_STATE = new AtomicReference<>();
        private static final AtomicReference<String> LAST_UI_PROGRESS = new AtomicReference<>();
        private static final AtomicReference<String> LAST_N1_UI_PROGRESS = new AtomicReference<>();
        private static final AtomicReference<String> LAST_N1_UI_BINDING_PROGRESS = new AtomicReference<>();
        private static final AtomicReference<String> LAST_UI_GATE = new AtomicReference<>();
        private static final AtomicReference<String> LAST_INVERSE_HOST_PRIORITY_GATE = new AtomicReference<>();
        private static final AtomicReference<String> LAST_REMOTE_STACK_PRIORITY_GATE = new AtomicReference<>();
        private static final AtomicReference<String> LAST_OPENING_PRIORITY_DIAGNOSTIC = new AtomicReference<>();
        private static final AtomicReference<String> FIRST_OPENING_PRIORITY_REJECTION = new AtomicReference<>();
        private static final AtomicInteger OPENING_PRIORITY_DISPATCHES = new AtomicInteger();
        private static final AtomicReference<TellingTimeSetup> TELLING_TIME_SETUP = new AtomicReference<>();
        private static final AtomicReference<Set<String>> INVERSE_REMOTE_EARNED_KNOWLEDGE =
                new AtomicReference<>(Set.of());
        private static final AtomicReference<Throwable> TELLING_TIME_START_HOOK_FAILURE = new AtomicReference<>();
        private static final AtomicReference<String> INVERSE_FENGRAF_PROGRESS = new AtomicReference<>();
        private static final AtomicReference<CMatchUI> ACTIVE_TELLING_TIME_UI = new AtomicReference<>();
        private static final AtomicReference<FGameClient> ACTIVE_REMOTE_CLIENT = new AtomicReference<>();
        private static volatile boolean renderedTellingTimeMode;
        private static volatile boolean inverseRenderedTellingTimeMode;
        private static volatile boolean fullSyncMode;
        private static volatile FullStateTap fullStateTap;

        /** Bounded decoded-state counters; never retains protocol objects or card identities. */
        private static final class FullStateTap implements AutoCloseable {
            private final Channel channel;
            private final String handlerName = "dandanFullStateReceipt";
            // All observer state is confined to the channel EventLoop.
            private int expectedGameViewId = -1;
            private int assignedPlayerViewId = -1;
            private int fullSnapshots;
            private int wrongGame;
            private int eventOnlyPackets;
            private int unexpectedDeltas;
            private boolean populated;
            private boolean error;
            private boolean armed;

            private FullStateTap(final Channel channel) { this.channel = channel; }

            static FullStateTap install(final FGameClient client) throws Exception {
                final Field channelField = FGameClient.class.getDeclaredField("channel");
                channelField.setAccessible(true);
                final Object value = channelField.get(client);
                if (!(value instanceof Channel channel) || !channel.isActive()) {
                    throw new AssertionError("FULL state tap requires the active remote client channel");
                }
                final FullStateTap tap = new FullStateTap(channel);
                tap.runOnEventLoop(() -> {
                    final var pipeline = channel.pipeline();
                    final var decoder = pipeline.context(CompatibleObjectDecoder.class);
                    if (decoder == null || pipeline.get(tap.handlerName) != null) {
                        throw new AssertionError("FULL state tap could not bind after the active decoder");
                    }
                    pipeline.addAfter(decoder.name(), tap.handlerName, new ChannelInboundHandlerAdapter() {
                        @Override public void channelRead(final ChannelHandlerContext context, final Object message)
                                throws Exception {
                            try {
                                if (tap.armed && message instanceof GuiGameEvent event) {
                                    if (event.getMethod() == ProtocolMethod.applyDelta) {
                                        final Object[] args = event.getObjects();
                                        if (args.length != 1 || !(args[0] instanceof DeltaPacket packet)) {
                                            tap.error = true;
                                        } else if (packet.getSequenceNumber() == -1L
                                                && packet.getObjectDeltas().isEmpty()
                                                && packet.getNewObjects().isEmpty()
                                                && packet.getChecksum() == 0
                                                && !packet.hasChecksum()
                                                && packet.hasEvents()) {
                                            tap.eventOnlyPackets++;
                                        } else {
                                            tap.unexpectedDeltas++;
                                        }
                                    } else if (event.getMethod() == ProtocolMethod.setGameView) {
                                        final Object[] args = event.getObjects();
                                        if (args.length == 0 || !(args[0] instanceof GameView view)) {
                                            tap.error = true;
                                        } else if (view.getId() != tap.expectedGameViewId) {
                                            tap.wrongGame++;
                                        } else {
                                            tap.fullSnapshots++;
                                            final var players = view.getPlayers();
                                            if (view.isDanDan() && players != null && players.size() == 2
                                                    && players.get(0).getId() != players.get(1).getId()) {
                                                final PlayerView assigned = players.stream()
                                                        .filter(player -> player.getId() == tap.assignedPlayerViewId)
                                                        .findFirst().orElse(null);
                                                if (assigned != null && !assigned.getCards(ZoneType.Hand).isEmpty()
                                                        && !players.get(0).getCards(ZoneType.Library).isEmpty()) {
                                                    final var firstLibrary = players.get(0).getCards(ZoneType.Library);
                                                    final var secondLibrary = players.get(1).getCards(ZoneType.Library);
                                                    boolean sharedLibraryMatches = firstLibrary.size() == secondLibrary.size();
                                                    for (int index = 0; sharedLibraryMatches && index < firstLibrary.size();
                                                            index++) {
                                                        sharedLibraryMatches = firstLibrary.get(index).getId()
                                                                == secondLibrary.get(index).getId();
                                                    }
                                                    if (sharedLibraryMatches) tap.populated = true;
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (Throwable ignored) {
                                tap.error = true;
                            } finally {
                                context.fireChannelRead(message);
                            }
                        }
                    });
                });
                return tap;
            }

            void arm(final int gameViewId, final int assignedPlayerId) throws Exception {
                runOnEventLoop(() -> {
                    if (armed || gameViewId < 0 || assignedPlayerId < 0) {
                        throw new AssertionError("FULL state tap arm is invalid");
                    }
                    expectedGameViewId = gameViewId;
                    assignedPlayerViewId = assignedPlayerId;
                    armed = true;
                });
            }

            String finishReceipt() throws Exception {
                final AtomicReference<String> result = new AtomicReference<>();
                runOnEventLoop(() -> {
                    armed = false;
                    result.set("FULL_STATE|REMOTE|FULL_SNAPSHOTS=" + fullSnapshots
                            + "|POPULATED=" + (populated ? 1 : 0)
                            + "|WRONG_GAME=" + wrongGame + "|EVENT_ONLY=" + eventOnlyPackets
                            + "|UNEXPECTED_DELTAS=" + unexpectedDeltas
                            + "|ERROR=" + (error ? 1 : 0));
                    final var context = channel.pipeline().context(handlerName);
                    if (context != null) channel.pipeline().remove(context.handler());
                });
                return result.get();
            }

            private void runOnEventLoop(final Runnable action) throws Exception {
                final EventLoop eventLoop = channel.eventLoop();
                if (eventLoop.inEventLoop()) {
                    action.run();
                    return;
                }
                eventLoop.submit(action).get(10, TimeUnit.SECONDS);
            }

            @Override public void close() throws Exception {
                runOnEventLoop(() -> {
                    armed = false;
                    final var context = channel.pipeline().context(handlerName);
                    if (context != null) channel.pipeline().remove(context.handler());
                });
            }
        }

        public static void main(final String[] args) {
            final String role = args.length > 0 ? args[0] : "UNKNOWN";
            final int port = args.length > 1 ? Integer.parseInt(args[1]) : -1;
            final boolean parentJavaUtilOpen = args.length > 2 && "1".equals(args[2]);
            final int copiedOpenCount = args.length > 3 ? Integer.parseInt(args[3]) : 0;
            final boolean continueToNextGame = args.length > 4 && "NPLUS1".equals(args[4]);
            renderedTellingTimeMode = args.length > 5 && ("RENDERED_TELLING_TIME".equals(args[5])
                    || "INVERSE_RENDERED_TELLING_TIME".equals(args[5]));
            inverseRenderedTellingTimeMode = args.length > 5
                    && "INVERSE_RENDERED_TELLING_TIME".equals(args[5]);
            fullSyncMode = args.length > 6 && "FULL".equals(args[6]);
            fullStateTap = null;
            WORKER_COMMANDS = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            TELLING_TIME_SETUP.set(null);
            TELLING_TIME_START_HOOK_FAILURE.set(null);
            INVERSE_FENGRAF_PROGRESS.set(null);
            System.setOut(new PrintStream(OutputStream.nullOutputStream()));
            System.setErr(new PrintStream(OutputStream.nullOutputStream()));
            final String moduleOpenReceipt = "OPEN|" + role + "|PARENT_UTIL_OPEN="
                    + (parentJavaUtilOpen ? 1 : 0) + "|COPIED=" + copiedOpenCount
                    + "|CHILD_UTIL_OPEN="
                    + (Object.class.getModule().isOpen("java.util", Worker.class.getModule()) ? 1 : 0);
            RESULT.println(moduleOpenReceipt);
            RESULT.flush();
            AppliedSession session = null;
            try {
                if ("HOST".equals(role)) session = runHost(port);
                else if ("REMOTE".equals(role)) session = runRemote(port);
                else throw new IllegalArgumentException("unknown role");
                if (renderedTellingTimeMode && "HOST".equals(role)) {
                    final TellingTimeSetup setup = TELLING_TIME_SETUP.get();
                    Assert.assertNotNull(setup, "the named host start hook prepared the rendered Telling Time state");
                    final String roles = "TT_PRIVATE_ROLES|" + setup.privateOneName() + "|" + setup.privateTwoName()
                            + "|" + setup.privateThreeName();
                    if (inverseRenderedTellingTimeMode) {
                        final List<Integer> ttManaIds = setup.manaSources().stream().map(Card::getId).toList();
                        final List<Integer> fengrafManaIds = setup.fengrafManaSources().stream()
                                .map(Card::getId).toList();
                        RESULT.println(roles + "|" + ttManaIds.get(0) + "|" + ttManaIds.get(1)
                                + "|" + setup.fengrafId() + "|" + setup.fengrafCreatureId()
                                + "|" + fengrafManaIds.get(0) + "|" + fengrafManaIds.get(1)
                                + "|" + fengrafManaIds.get(2) + "|" + setup.daysUndoing().getId());
                    } else {
                        RESULT.println(roles);
                    }
                }
                RESULT.println(session.receipt);
                RESULT.flush();
                BufferedReader commands = WORKER_COMMANDS;
                if (inverseRenderedTellingTimeMode) {
                    runInverseRenderedTellingTimeCommand(role, commands);
                } else if (renderedTellingTimeMode) {
                    runRenderedTellingTimeCommand(role, commands);
                }
                final String outcomeCommand = "HOST".equals(role) ? "CONCEDE" : "AWAIT_OUTCOME";
                if (!outcomeCommand.equals(commands.readLine())) throw new IllegalStateException("missing outcome barrier");
                session.awaitOutcome();
                RESULT.println("OUTCOME|" + role);
                RESULT.flush();
                if (continueToNextGame) {
                    if (!"CONTINUE".equals(commands.readLine())) {
                        throw new IllegalStateException("missing N+1 continuation barrier");
                    }
                    session = session.continueToNextGame();
                    RESULT.println(session.chooserReceipt);
                    RESULT.println(session.receipt);
                    RESULT.flush();
                    final String nextOutcomeCommand = "HOST".equals(role) ? "CONCEDE_N1" : "AWAIT_N1_OUTCOME";
                    if (!nextOutcomeCommand.equals(commands.readLine())) {
                        throw new IllegalStateException("missing N+1 outcome barrier");
                    }
                    session.awaitOutcome();
                    RESULT.println("N1OUTCOME|" + role);
                    RESULT.flush();
                }
                final String finishCommand = "HOST".equals(role) ? "QUIT" : "RELEASE";
                if (!finishCommand.equals(commands.readLine())) throw new IllegalStateException("missing finish barrier");
                session.finish();
                RESULT.println("DONE|" + role);
                RESULT.flush();
                System.exit(0);
            } catch (Throwable failure) {
                RESULT.println(failureReceipt(role, failure, moduleOpenReceipt));
                RESULT.flush();
                if (session != null) {
                    try {
                        session.abort();
                        RESULT.println("CLEANUP|" + role + "|ABORT=COMPLETED");
                    }
                    catch (Throwable cleanupFailure) {
                        RESULT.println("CLEANUP|" + role + "|ABORT_FAILED="
                                + cleanupFailure.getClass().getSimpleName());
                    }
                } else {
                    RESULT.println("CLEANUP|" + role + "|ABORT=NO_SESSION");
                }
                RESULT.flush();
                System.exit(1);
            }
        }

        private static String failureReceipt(final String role, final Throwable failure,
                final String moduleOpenReceipt) {
            final StringBuilder receipt = new StringBuilder("FAIL|").append(role).append('|')
                    .append(failure.getClass().getSimpleName()).append('|').append(moduleOpenReceipt);
            appendFailureDetails(receipt, failure);
            for (Throwable cleanupFailure : failure.getSuppressed()) {
                receipt.append("|CLEANUP|").append(cleanupFailure.getClass().getSimpleName());
            }
            receipt.append("|EDT|");
            final Throwable edtFailure = FIRST_EDT_FAILURE.get();
            if (edtFailure == null) receipt.append("none");
            else {
                receipt.append(edtFailure.getClass().getSimpleName());
                appendFailureDetails(receipt, edtFailure);
            }
            receipt.append("|BOOTSTRAP|")
                    .append(BOOTSTRAP_STATE.get() == null ? "unrecorded" : BOOTSTRAP_STATE.get())
                    .append("|UI|").append(LAST_UI_PROGRESS.get() == null ? "unrecorded" : LAST_UI_PROGRESS.get())
                    .append("|UI_GATE|").append(LAST_UI_GATE.get() == null ? "none" : LAST_UI_GATE.get());
            if (inverseRenderedTellingTimeMode && INVERSE_FENGRAF_PROGRESS.get() != null) {
                receipt.append("|FENGRAF_PROGRESS|").append(INVERSE_FENGRAF_PROGRESS.get());
            }
            if (renderedTellingTimeMode) {
                receipt.append("|OPENING_PRIORITY_DIAGNOSTIC|DISPATCHES=")
                        .append(OPENING_PRIORITY_DISPATCHES.get())
                        .append("|FIRST_REJECT_BEFORE_DISPATCH=")
                        .append(FIRST_OPENING_PRIORITY_REJECTION.get() == null
                                ? "none" : FIRST_OPENING_PRIORITY_REJECTION.get())
                        .append("|LAST_ATTEMPT=")
                        .append(LAST_OPENING_PRIORITY_DIAGNOSTIC.get() == null
                                ? "unrecorded" : LAST_OPENING_PRIORITY_DIAGNOSTIC.get());
            }
            final String inverseHostGate = LAST_INVERSE_HOST_PRIORITY_GATE.get();
            if (inverseHostGate != null) receipt.append("|INVERSE_HOST_PRIORITY_GATE|").append(inverseHostGate);
            final String remoteStackGate = LAST_REMOTE_STACK_PRIORITY_GATE.get();
            if (remoteStackGate != null) receipt.append("|REMOTE_STACK_PRIORITY_GATE|").append(remoteStackGate);
            return receipt.toString();
        }

        private static void appendFailureDetails(final StringBuilder receipt, final Throwable failure) {
            Throwable cause = failure.getCause();
            Throwable deepestCause = failure;
            for (int depth = 0; cause != null && cause != failure && depth < 3; depth++) {
                receipt.append("|CAUSE|").append(cause.getClass().getSimpleName());
                deepestCause = cause;
                cause = cause.getCause();
            }
            final StackTraceElement[] stack = failure.getStackTrace();
            for (int i = 0; i < Math.min(3, stack.length); i++) {
                final StackTraceElement frame = stack[i];
                receipt.append("|AT|").append(frame.getClassName()).append('.')
                        .append(frame.getMethodName()).append(':').append(frame.getLineNumber());
            }
            for (StackTraceElement frame : stack) {
                if (frame.getClassName().startsWith("forge.net.DanDanTwoJvmMatchUiTest")) {
                    receipt.append("|FIXTURE_AT|").append(frame.getMethodName())
                            .append(':').append(frame.getLineNumber());
                    break;
                }
            }
            final StackTraceElement[] causeStack = deepestCause.getStackTrace();
            for (int i = 0, emitted = 0; i < causeStack.length && emitted < 3; i++) {
                final StackTraceElement frame = causeStack[i];
                if (frame.getClassName().startsWith("forge.net.DanDanTwoJvmMatchUiTest")) {
                    receipt.append("|ROOT_FIXTURE_AT|").append(frame.getMethodName())
                            .append(':').append(frame.getLineNumber());
                    emitted++;
                }
            }
        }

        private static void captureEdtFailures() {
            final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler((thread, failure) -> {
                if (EventQueue.isDispatchThread()) FIRST_EDT_FAILURE.compareAndSet(null, failure);
                if (previous != null) previous.uncaughtException(thread, failure);
            });
        }

        private static String bootstrapState() {
            final var view = Singletons.getView();
            final var frame = view == null ? null : view.getFrame();
            final var splash = view == null ? null : view.getSplash();
            final boolean navigationPresent = view != null && view.getNavigationBar() != null;
            int visibleModalDialogs = 0;
            final StringBuilder modalClasses = new StringBuilder();
            for (Window window : Window.getWindows()) {
                if (window instanceof Dialog dialog && dialog.isModal() && dialog.isShowing()) {
                    visibleModalDialogs++;
                    if (visibleModalDialogs <= 3) {
                        if (modalClasses.length() > 0) modalClasses.append(',');
                        modalClasses.append(window.getClass().getName());
                    }
                }
            }
            final StringBuilder forgeFrames = new StringBuilder();
            int frameCount = 0;
            for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
                final String className = element.getClassName();
                final String methodName = element.getMethodName();
                if (!className.startsWith("forge.") || className.startsWith(Worker.class.getName())
                        || ("forge.gui.FThreads".equals(className) && methodName.startsWith("invokeInEdt"))
                        || ("forge.GuiDesktop".equals(className) && methodName.startsWith("invokeInEdt"))) {
                    continue;
                }
                if (forgeFrames.length() > 0) forgeFrames.append(',');
                forgeFrames.append(className).append('.').append(methodName).append(':')
                        .append(element.getLineNumber());
                if (++frameCount == 8) break;
            }
            return "FRAME=" + (frame != null ? 1 : 0)
                    + ",DISPLAYABLE=" + (frame != null && frame.isDisplayable() ? 1 : 0)
                    + ",SHOWING=" + (frame != null && frame.isShowing() ? 1 : 0)
                    + ",NAVIGATION=" + (navigationPresent ? 1 : 0)
                    + ",SPLASH=" + (splash != null ? 1 : 0)
                    + ",MODAL=" + visibleModalDialogs
                    + ",MODAL_CLASSES=" + modalClasses
                    + ",FORGE_FRAMES=" + forgeFrames;
        }

        private static AppliedSession runHost(final int port) throws Exception {
            initializeDesktop();
            RESULT.println("SYNC_MODE|HOST|" + (fullSyncMode ? "FULL" : "DELTA"));
            RESULT.flush();
            final UiDesktop desktop = (UiDesktop) GuiBase.getInterface();
            final FServerManager server = FServerManager.getInstance();
            final CountDownLatch remoteReady = new CountDownLatch(1);
            server.setLobbyListener(new ILobbyListener() {
                @Override public void update(final GameLobbyData data, final int slot) { }
                @Override public void message(final String source, final String message,
                        final forge.gamemodes.net.ChatMessage.MessageType type) { }
                @Override public void close() { }
                @Override public ClientGameLobby getLobby() { return null; }
            });
            server.startServer(port);
            try {
            final ServerGameLobby lobby = new ServerGameLobby();
            lobby.setListener(new IUpdateable() {
                @Override public void update(final boolean fullUpdate) {
                    server.updateLobbyState();
                    if (lobby.getSlot(1) != null && lobby.getSlot(1).isReady()) remoteReady.countDown();
                }
                @Override public void update(final int slot, final LobbySlotType type) { }
            });
            server.setLobby(lobby);
            lobby.getSlot(0).setName(HOST_NAME);
            lobby.getSlot(0).setDeck(officialDanDanDeck());
            lobby.applyVariant(GameType.DanDan);
            lobby.getSlot(0).setIsReady(true);
            lobby.getSlot(1).setName(REMOTE_NAME);
            RESULT.println("READY|HOST");
            RESULT.flush();

            Assert.assertTrue(remoteReady.await(WORKER_STARTUP_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                    "TCP client made the second lobby seat ready");
            FThreads.invokeInEdtAndWait(() -> {
                final Runnable start = lobby.startGame();
                Assert.assertNotNull(start, "the two-seat DanDan lobby is ready");
                start.run();
            });
            CMatchUI ui = awaitHostMatchUi(desktop);
            final HostedMatch match = desktop.hostedMatch.get();
            Assert.assertNotNull(match, "host created an ordinary HostedMatch");
            final Game game = match.getGame();
            Assert.assertNotNull(game, "host retained the actual running game until both clients reported MAIN1");
            ACTIVE_TELLING_TIME_UI.set(ui);
            final String receipt = driveLocalUi(ui, "HOST", 0, game);
            desktop.firstGame.set(game);
            final UiSnapshot hostState = observeUiOnEdt(ui, "HOST", 0);
            Assert.assertNotNull(hostState);
            Assert.assertEquals(hostState.gameId(), game.getView().getId(),
                    "host UI MAIN1 receipt belongs to the captured actual game");
            assertHostPhysicalCardConservation(game);
            Assert.assertNotNull(hostState.controller(), "host's installed human controller is retained for outcome cleanup");
            Assert.assertSame(hostState.controller().getGame(), game,
                    "the retained controller belongs to the captured running game");
            if (renderedTellingTimeMode) {
                final TellingTimeSetup setup = TELLING_TIME_SETUP.get();
                Assert.assertNotNull(setup);
                TELLING_TIME_SETUP.set(setup.withController(hostState.controller()));
            }
            return hostAppliedSession(receipt, null, null, ui, desktop, server, match, game, hostState, 1);
            } catch (Exception | Error failure) {
                final HostedMatch activeMatch = desktop.hostedMatch.get();
                if (activeMatch != null) {
                    try { abortHostSession(activeMatch, server, desktop); }
                    catch (Throwable cleanupFailure) {
                        if (cleanupFailure != failure) failure.addSuppressed(cleanupFailure);
                    }
                } else {
                    server.stopServer();
                    FThreads.invokeInEdtAndWait(() -> Singletons.getView().getFrame().dispose());
                }
                throw failure;
            }
        }

        private static AppliedSession hostAppliedSession(final String receipt, final String chooserReceipt,
                final String continueReceipt, final CMatchUI ui, final UiDesktop desktop,
                final FServerManager server, final HostedMatch match, final Game game,
                final UiSnapshot state, final int gameOrdinal) {
            final PlayerControllerHuman controller = state.controller();
            final Cleanup abort = () -> abortHostSession(match, server, desktop);
            final Continuation continuation = gameOrdinal == 1 ? () -> {
                final List<String> previousRenderedRows = inverseRenderedTellingTimeMode
                        ? captureInverseRenderedHistoryRows(ui, state.gameId(), 1) : List.of();
                if (inverseRenderedTellingTimeMode) match.setStartGameHook(null);
                final String clicked = clickNextGame(ui, "HOST", state.gameId());
                final Game nextGame = awaitNextActualGame(match, game, state.gameId());
                final UiSnapshot nextState = driveNextGameUi(ui, "HOST", 0, state.gameId(), true, nextGame);
                Assert.assertNotNull(nextState.controller(), "N+1 host rendered its assigned controller");
                Assert.assertNotSame(nextGame, game, "Continue created an actual fresh host Game");
                Assert.assertNotEquals(nextState.gameId(), state.gameId(), "N+1 has a fresh GameView identity");
                Assert.assertEquals(nextState.gameId(), nextGame.getView().getId(),
                        "host N+1 view receipt belongs to the actual current Game");
                Assert.assertNotSame(nextState.controller(), controller,
                        "N+1 uses a fresh host PlayerControllerHuman");
                Assert.assertSame(nextState.controller().getGame(), nextGame,
                        "N+1 host controller belongs to the current actual Game");
                Assert.assertEquals(nextState.renderedOwnHandCount(), 7,
                        "N+1 host own hand is rendered after its actual Keep choice");
                if (inverseRenderedTellingTimeMode) {
                    assertInverseRenderedHistoryReset(ui, nextState.gameId(), previousRenderedRows);
                }
                assertHostPhysicalCardConservation(nextGame);
                return hostAppliedSession("N1" + nextState.publicReceipt(), nextState.promptReceipt(), clicked,
                        ui, desktop, server, match, nextGame, nextState, 2);
            } : null;

            return new AppliedSession(receipt, () -> {
                Assert.assertSame(match.getGame(), game, "outcome belongs to the current captured host Game");
                Assert.assertSame(controller.getGame(), game, "outcome controller belongs to the current Game");
                FThreads.invokeInEdtAndWait(controller::concede);
                awaitHostGameOutcomeAndReturn(game, desktop);
            }, () -> {
                try {
                    Assert.assertSame(match.getGame(), game, "terminal QUIT uses the current captured host Game");
                    Assert.assertSame(controller.getGame(), game, "terminal QUIT uses its current host controller");
                    FThreads.invokeInEdtAndWait(() -> controller.nextGameDecision(
                            forge.gamemodes.match.NextGameDecision.QUIT));
                    if (!desktop.matchEnded.await(20, TimeUnit.SECONDS)) {
                        throw new AssertionError("HostedMatch did not complete terminal QUIT cleanup");
                    }
                    final Throwable callbackFailure = desktop.matchOverFailure.get();
                    if (callbackFailure != null) {
                        throw new AssertionError("GameLobby match-over callback failed: "
                                + callbackFailure.getClass().getSimpleName());
                    }
                } finally {
                    server.stopServer();
                    FThreads.invokeInEdtAndWait(() -> Singletons.getView().getFrame().dispose());
                }
            }, abort, continuation, continueReceipt, chooserReceipt);
        }

        private static void awaitHostGameOutcomeAndReturn(final Game game, final UiDesktop desktop)
                throws InterruptedException {
            final boolean firstGame = game == desktop.firstGame.get()
                    || game == desktop.returnedFirstGame.get()
                    || (desktop.firstGame.get() == null && desktop.returnedFirstGame.get() == null
                            && desktop.gameReturnCount.get() == 0);
            final CountDownLatch returned = firstGame ? desktop.gameReturned : desktop.nextGameReturned;
            final AtomicReference<Game> returnedGame = firstGame
                    ? desktop.returnedFirstGame : desktop.returnedNextGame;
            if (!returned.await(30, TimeUnit.SECONDS) || returnedGame.get() != game
                    || !game.isGameOver() || game.getOutcome() == null) {
                throw new AssertionError("captured host Game did not produce an outcome and engine return");
            }
        }

        private static void abortHostSession(final HostedMatch match, final FServerManager server,
                final UiDesktop desktop) throws Exception {
            try {
                final Game activeGame = match.getGame();
                if (activeGame != null) {
                    final PlayerControllerHuman activeController = hostControllerFor(activeGame);
                    if (!activeGame.isGameOver()) {
                        FThreads.invokeInEdtAndWait(activeController::concede);
                    }
                    awaitHostGameOutcomeAndReturn(activeGame, desktop);
                    if (match.getGame() == activeGame) {
                        FThreads.invokeInEdtAndWait(() -> activeController.nextGameDecision(
                                forge.gamemodes.match.NextGameDecision.QUIT));
                        if (!desktop.matchEnded.await(20, TimeUnit.SECONDS)) {
                            throw new AssertionError("abort cleanup did not terminate the active HostedMatch");
                        }
                    }
                }
                final Throwable callbackFailure = desktop.matchOverFailure.get();
                if (callbackFailure != null) {
                    throw new AssertionError("HostedMatch lifecycle callback failed: "
                            + callbackFailure.getClass().getSimpleName());
                }
            } finally {
                server.stopServer();
                FThreads.invokeInEdtAndWait(() -> Singletons.getView().getFrame().dispose());
            }
        }

        private static PlayerControllerHuman hostControllerFor(final Game game) {
            final Object controller = game.getPlayers().get(0).getController();
            if (!(controller instanceof PlayerControllerHuman pch) || pch.getGame() != game) {
                throw new AssertionError("active Game lacks its actual seat-zero host controller");
            }
            return pch;
        }

        private static Game awaitNextActualGame(final HostedMatch match, final Game previous,
                final int previousViewId) throws InterruptedException {
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(UI_TIMEOUT_SECONDS);
            while (System.nanoTime() < deadline) {
                final Game current = match.getGame();
                if (current != null && current != previous && current.getView().getId() != previousViewId) {
                    RESULT.println("N1STEP|HOST|STAGE=ACTUAL_GAME|GAME_CHANGED=1|VIEW_CHANGED=1");
                    RESULT.flush();
                    return current;
                }
                Thread.sleep(20);
            }
            throw new AssertionError("both CONTINUE decisions did not install a distinct host Game");
        }

        private static AppliedSession runRemote(final int port) throws Exception {
            initializeDesktop();
            RESULT.println("SYNC_MODE|REMOTE|" + (fullSyncMode ? "FULL" : "DELTA"));
            RESULT.flush();
            final AtomicReference<CMatchUI> uiHolder = new AtomicReference<>();
            FThreads.invokeInEdtAndWait(() -> uiHolder.set(new CMatchUI()));
            final CMatchUI ui = uiHolder.get();
            final ClientGameLobby lobby = new ClientGameLobby();
            final CountDownLatch assigned = new CountDownLatch(1);
            final CountDownLatch readyAcknowledged = new CountDownLatch(1);
            final FGameClient client = new FGameClient(REMOTE_NAME, ui, "127.0.0.1", port);
            try {
            client.addLobbyListener(new ILobbyListener() {
                @Override public void update(final GameLobbyData data, final int slot) {
                    lobby.setData(data);
                    final boolean ready = lobby.getSlot(1) != null
                            && lobby.getSlot(1).getType() == LobbySlotType.REMOTE && lobby.getSlot(1).isReady();
                    if (slot >= 0) {
                        lobby.setLocalPlayer(slot);
                        if (slot != 1) throw new AssertionError("remote assigned an unexpected seat");
                        assigned.countDown();
                        if (!fullSyncMode) client.send(UpdateLobbyPlayerEvent.isReadyUpdate(true));
                    }
                    if (ready) readyAcknowledged.countDown();
                }
                @Override public void message(final String source, final String message,
                        final forge.gamemodes.net.ChatMessage.MessageType type) { }
                @Override public void close() { }
                @Override public ClientGameLobby getLobby() { return lobby; }
            });
            client.connect();
            ACTIVE_REMOTE_CLIENT.set(client);
            Assert.assertTrue(assigned.await(30, TimeUnit.SECONDS), "remote TCP lobby seat was assigned");
            if (fullSyncMode) {
                fullStateTap = FullStateTap.install(client);
                client.send(UpdateLobbyPlayerEvent.isReadyUpdate(true));
            }
            Assert.assertTrue(readyAcknowledged.await(30, TimeUnit.SECONDS), "host confirmed remote readiness");
            final String receipt = driveLocalUi(ui, "REMOTE", 1, null);
            ACTIVE_TELLING_TIME_UI.set(ui);
            final UiSnapshot initialState = observeUiOnEdt(ui, "REMOTE", 1);
            Assert.assertNotNull(initialState);
            return remoteAppliedSession(receipt, null, null, ui, client, initialState.gameId(), 1);
            } catch (Exception | Error failure) {
                try { closeRemoteClient(client); }
                catch (Throwable cleanupFailure) { failure.addSuppressed(cleanupFailure); }
                throw failure;
            }
        }

        private static AppliedSession remoteAppliedSession(final String receipt, final String chooserReceipt,
                final String continueReceipt, final CMatchUI ui, final FGameClient client,
                final int gameViewId, final int gameOrdinal) {
            final Continuation continuation = gameOrdinal == 1 ? () -> {
                final List<String> previousRenderedRows = inverseRenderedTellingTimeMode
                        ? captureInverseRenderedHistoryRows(ui, gameViewId, 2) : List.of();
                final String clicked = clickNextGame(ui, "REMOTE", gameViewId);
                final UiSnapshot nextState = driveNextGameUi(ui, "REMOTE", 1, gameViewId, false, null);
                Assert.assertNotEquals(nextState.gameId(), gameViewId, "remote applied a distinct N+1 GameView");
                Assert.assertEquals(nextState.renderedOwnHandCount(), 7,
                        "N+1 remote own hand is rendered after its actual Keep choice");
                if (inverseRenderedTellingTimeMode) {
                    assertInverseRenderedHistoryReset(ui, nextState.gameId(), previousRenderedRows);
                }
                return remoteAppliedSession("N1" + nextState.publicReceipt(), nextState.promptReceipt(), clicked,
                        ui, client, nextState.gameId(), 2);
            } : null;
            return new AppliedSession(receipt, () -> awaitRemoteGameOutcome(ui, gameViewId), () -> {
                closeRemoteClient(client);
            }, () -> closeRemoteClient(client), continuation, continueReceipt, chooserReceipt);
        }

        private static void closeRemoteClient(final FGameClient client) throws Exception {
            Throwable failure = null;
            final FullStateTap tap = fullStateTap;
            fullStateTap = null;
            if (tap != null) try { tap.close(); } catch (Throwable error) { failure = error; }
            try { client.close(); } catch (Throwable error) {
                if (failure == null) failure = error;
                else failure.addSuppressed(error);
            }
            try { FThreads.invokeInEdtAndWait(() -> Singletons.getView().getFrame().dispose()); }
            catch (Throwable error) {
                if (failure == null) failure = error;
                else failure.addSuppressed(error);
            }
            if (failure instanceof Exception exception) throw exception;
            if (failure instanceof Error error) throw error;
            if (failure != null) throw new RuntimeException(failure);
        }

        private static void awaitRemoteGameOutcome(final CMatchUI ui, final int expectedViewId)
                throws InterruptedException {
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
            while (System.nanoTime() < deadline) {
                final boolean[] ended = new boolean[1];
                FThreads.invokeInEdtAndWait(() -> ended[0] = ui.getGameView() != null
                        && ui.getGameView().getId() == expectedViewId && ui.getGameView().isGameOver());
                if (ended[0]) return;
                Thread.sleep(20);
            }
            throw new AssertionError("match did not finish after normal quit decision");
        }

        private static void initializeDesktop() {
            RESULT.println("STARTUP_STAGE|MODEL");
            RESULT.flush();
            final UiDesktop desktop = new UiDesktop();
            GuiBase.setInterface(desktop);
            CdnUuidCache.markBulkSyncPromptAnswered();
            FModel.initialize(null, preferences -> {
                preferences.setPref(FPref.LOAD_CARD_SCRIPTS_LAZILY, false);
                preferences.setPref(FPref.UI_LANGUAGE, "en-US");
                preferences.setPref(FPref.UI_ENABLE_MUSIC, "false");
                preferences.setPref(FPref.UI_ENABLE_SOUNDS, "false");
                preferences.setPref(FPref.CHECK_SNAPSHOT_AT_STARTUP, "false");
                preferences.setPref(FPref.ENFORCE_DECK_LEGALITY, "false");
                FModel.getNetPreferences().setPref(ForgeNetPreferences.FNetPref.UPnP, "NEVER");
                return null;
            });
            RESULT.println("STARTUP_STAGE|SINGLETONS");
            RESULT.flush();
            captureEdtFailures();
            RemoteClientGuiGame.useDeltaSync = !fullSyncMode;
            Singletons.initializeOnce(true);
            RESULT.println("STARTUP_STAGE|CONTROL");
            RESULT.flush();
            Singletons.getControl().initialize();
            FThreads.invokeInEdtAndWait(() -> {
                final String state = bootstrapState();
                BOOTSTRAP_STATE.set(state);
                Assert.assertTrue(Singletons.getView().getFrame().isShowing(),
                        "worker initialized an actual Swing desktop frame; " + state);
            });
        }

        private static Deck officialDanDanDeck() {
            for (DeckProxy proxy : DeckProxy.getAllDanDanDecks()) {
                if ("DanDan_SecretLair".equals(proxy.getName())) return proxy.getDeck();
            }
            throw new AssertionError("official DanDan preset is unavailable");
        }

        private static CMatchUI awaitHostMatchUi(final UiDesktop desktop) throws InterruptedException {
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(UI_TIMEOUT_SECONDS);
            while (System.nanoTime() < deadline) {
                CMatchUI ui = desktop.matchUi.get();
                if (ui != null && hasAppliedViewOnEdt(ui)) return ui;
                Thread.sleep(20);
            }
            throw new AssertionError("host match UI did not receive its applied view");
        }

        private static boolean hasAppliedViewOnEdt(final CMatchUI ui) {
            final boolean[] ready = new boolean[1];
            FThreads.invokeInEdtAndWait(() -> ready[0] = ui.getGameView() != null && ui.getCurrentPlayer() != null);
            return ready[0];
        }

        private static String driveLocalUi(final CMatchUI ui, final String role, final int expectedSeat,
                final Game expectedGame)
                throws Exception {
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(UI_TIMEOUT_SECONDS);
            boolean answeredMulligan = false;
            boolean startSeen = false;
            boolean startClicked = false;
            boolean mulliganSeen = false;
            int renderedGameViewId = -1;
            String category = null;
            final Set<PhaseType> passedHostPhases = new HashSet<>();
            final Set<PhaseType> passedRemotePhases = new HashSet<>();
            boolean inverseMain1SkipCoordinated = false;
            if (inverseRenderedTellingTimeMode && "REMOTE".equals(role)) {
                INVERSE_REMOTE_EARNED_KNOWLEDGE.set(Set.of());
            }
            if (renderedTellingTimeMode) {
                LAST_OPENING_PRIORITY_DIAGNOSTIC.set("NOT_ATTEMPTED");
                FIRST_OPENING_PRIORITY_REJECTION.set(null);
                OPENING_PRIORITY_DISPATCHES.set(0);
            }
            while (System.nanoTime() < deadline) {
                final Throwable startHookFailure = TELLING_TIME_START_HOOK_FAILURE.get();
                if (startHookFailure != null) {
                    throw new AssertionError("rendered Telling Time start hook failed", startHookFailure);
                }
                final UiSnapshot state = observeUiProgressOnEdt(ui, role, expectedSeat);
                ChoiceKind choiceKind = null;
                final boolean choiceQueried = state != null && !answeredMulligan;
                ChoiceAction action = null;
                if (state != null) {
                    if (renderedGameViewId != state.gameId()) renderedGameViewId = state.gameId();

                    if (!answeredMulligan) {
                        action = inspectAndMaybeClickChoice(ui, expectedSeat, expectedGame, !startClicked, true,
                                !renderedTellingTimeMode || "HOST".equals(role));
                        choiceKind = action.kind();
                        if (choiceKind != null) {
                            if (choiceKind == ChoiceKind.START) {
                                startSeen = true;
                                if (action.clicked()) startClicked = true;
                            } else if (choiceKind == ChoiceKind.FREE_MULLIGAN
                                    || choiceKind == ChoiceKind.REGULAR_MULLIGAN) {
                                mulliganSeen = true;
                                if (action.clicked()) {
                                    answeredMulligan = true;
                                    category = choiceKind.name().replace("_MULLIGAN", "");
                                }
                            }
                        }
                    }
                }
                publishUiProgress(role, expectedSeat, state, choiceKind, choiceQueried, startSeen, startClicked,
                        mulliganSeen, answeredMulligan, category, action);

                if (inverseRenderedTellingTimeMode && answeredMulligan && !inverseMain1SkipCoordinated
                        && state != null && ("REMOTE".equals(role) || TELLING_TIME_SETUP.get() != null)) {
                    coordinateInverseHostMain1Skip(ui, role, expectedSeat, expectedGame, state.gameId());
                    inverseMain1SkipCoordinated = true;
                }
                if (renderedTellingTimeMode && answeredMulligan
                        && (!inverseRenderedTellingTimeMode || inverseMain1SkipCoordinated)) {
                    passOpeningPriorityOnEdt(ui, role, expectedSeat, expectedGame,
                            passedHostPhases, passedRemotePhases);
                }

                if (state != null) {
                    if (answeredMulligan && state.turn() == 1 && state.phase() == PhaseType.MAIN1) {
                        final UiSnapshot finalState = observeUiOnEdt(ui, role, expectedSeat);
                        assertUiGate(finalState != null, "FINAL_SNAPSHOT");
                        Assert.assertEquals(finalState.gameId(), state.gameId(),
                                "final strict layout snapshot belongs to the applied view");
                        if (renderedTellingTimeMode) {
                            final Set<PhaseType> passedPhases = "HOST".equals(role)
                                    ? passedHostPhases : passedRemotePhases;
                            Assert.assertTrue(passedPhases.contains(PhaseType.UNTAP),
                                    "the required opening UNTAP priority was actually passed");
                            RESULT.println("OPENING_PRIORITY|" + role
                                    + "|UNTAP=" + (passedPhases.contains(PhaseType.UNTAP) ? 1 : 0)
                                    + "|UPKEEP=" + (passedPhases.contains(PhaseType.UPKEEP) ? 1 : 0)
                                    + "|DRAW=" + (passedPhases.contains(PhaseType.DRAW) ? 1 : 0)
                                    + "|PASSES=" + passedPhases.size());
                            RESULT.flush();
                        }
                        RESULT.println("CHOOSER|" + role + "|" + expectedSeat + "|" + category);
                        RESULT.flush();
                        return finalState.publicReceipt();
                    }
                }
                Thread.sleep(20);
            }
            throw new AssertionError("rendered client did not reach its mulligan choice and applied MAIN1 state");
        }

        private static void coordinateInverseHostMain1Skip(final CMatchUI ui, final String role,
                final int expectedSeat, final Game expectedGame, final int expectedGameViewId) throws Exception {
            if ("HOST".equals(role)) {
                final TellingTimeSetup setup = TELLING_TIME_SETUP.get();
                Assert.assertNotNull(setup, "host has the inverse game's remote controller for the skip handshake");
                Assert.assertSame(setup.game(), expectedGame);
                Assert.assertTrue(setup.remote().getController() instanceof PlayerControllerHuman,
                        "remote player is represented by its real host-side human controller");
                final PlayerControllerHuman remoteController =
                        (PlayerControllerHuman) setup.remote().getController();
                Assert.assertSame(remoteController.getGame(), expectedGame);
                final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(UI_TIMEOUT_SECONDS);
                while (System.nanoTime() < deadline && !remoteController.getYieldController()
                        .isSkippingPhase(setup.host().getView(), PhaseType.MAIN1)) {
                    Assert.assertTrue(currentHostUiBinding(ui, expectedSeat, expectedGameViewId,
                            expectedGame.getView().getTracker(), remoteController),
                            "host UI remains bound while observing the initial skip");
                    Thread.sleep(20);
                }
                Assert.assertTrue(currentHostUiBinding(ui, expectedSeat, expectedGameViewId,
                        expectedGame.getView().getTracker(), remoteController),
                        "the same remote PCH and game-view binding are current at the initial skip observation");
                Assert.assertSame(setup.remote().getController(), remoteController,
                        "the original remote PCH remains installed at the initial skip observation");
                Assert.assertSame(remoteController.getGame(), expectedGame,
                        "the remote PCH remains bound to the expected game at the initial skip observation");
                Assert.assertTrue(remoteController.getYieldController()
                        .isSkippingPhase(setup.host().getView(), PhaseType.MAIN1),
                        "the remote PCH initially has the host's MAIN1 marked skipped");
                RESULT.println("INVERSE_PHASE_SKIP_READY|HOST|INITIAL_SKIP=1");
                RESULT.flush();
                Assert.assertEquals(WORKER_COMMANDS.readLine(), "REMOTE_TOGGLE_SENT");
                while (System.nanoTime() < deadline && remoteController.getYieldController()
                        .isSkippingPhase(setup.host().getView(), PhaseType.MAIN1)) {
                    Assert.assertTrue(currentHostUiBinding(ui, expectedSeat, expectedGameViewId,
                            expectedGame.getView().getTracker(), remoteController),
                            "host UI remains bound while observing the unskip");
                    Thread.sleep(20);
                }
                Assert.assertTrue(currentHostUiBinding(ui, expectedSeat, expectedGameViewId,
                        expectedGame.getView().getTracker(), remoteController),
                        "the same remote PCH and game-view binding are current at the unskip observation");
                Assert.assertSame(setup.remote().getController(), remoteController,
                        "the original remote PCH remains installed at the unskip observation");
                Assert.assertSame(remoteController.getGame(), expectedGame,
                        "the remote PCH remains bound to the expected game at the unskip observation");
                Assert.assertFalse(remoteController.getYieldController()
                        .isSkippingPhase(setup.host().getView(), PhaseType.MAIN1),
                        "the host's remote PCH applied the remote phase-label toggle");
                RESULT.println("INVERSE_PHASE_SKIP_APPLIED|HOST|INITIAL_SKIP=1|CURRENT_SKIP=0");
                RESULT.flush();
                return;
            }

            final AtomicReference<NetGameController> localController = new AtomicReference<>();
            final AtomicReference<PlayerView> hostPlayer = new AtomicReference<>();
            final AtomicReference<forge.toolbox.special.PhaseLabel> hostMain1 = new AtomicReference<>();
            final AtomicReference<Tracker> expectedTracker = new AtomicReference<>();
            FThreads.invokeInEdtAndWait(() -> {
                final GameView view = ui.getGameView();
                Assert.assertNotNull(view);
                Assert.assertEquals(view.getId(), expectedGameViewId);
                Assert.assertTrue(ui.isCurrentScreen());
                final List<PlayerView> players = new ArrayList<>(view.getPlayers());
                Assert.assertEquals(players.size(), 2);
                final PlayerView local = currentAssignedPlayerOnEdt(ui, view, players, expectedSeat);
                final PlayerView opponent = players.get(1 - expectedSeat);
                Assert.assertSame(opponent.getTracker(), view.getTracker());
                Assert.assertNotEquals(opponent, local);
                Assert.assertTrue(ui.getGameController(local) instanceof NetGameController);
                final NetGameController controller = (NetGameController) ui.getGameController(local);
                final var label = ui.getFieldViewFor(opponent).getPhaseIndicator().getLabelFor(PhaseType.MAIN1);
                Assert.assertNotNull(label);
                Assert.assertTrue(label.isShowing(), "the opponent MAIN1 phase label is rendered");
                Assert.assertFalse(label.getEnabled(), "the actual host-row MAIN1 phase label starts disabled");
                localController.set(controller);
                hostPlayer.set(opponent);
                hostMain1.set(label);
                expectedTracker.set(view.getTracker());
            });
            RESULT.println("INVERSE_PHASE_SKIP_READY|REMOTE|LABEL_DISABLED=1");
            RESULT.flush();
            Assert.assertEquals(WORKER_COMMANDS.readLine(), "TOGGLE_HOST_MAIN1");
            FThreads.invokeInEdtAndWait(() -> {
                final GameView view = ui.getGameView();
                Assert.assertNotNull(view);
                final List<PlayerView> players = new ArrayList<>(view.getPlayers());
                Assert.assertEquals(players.size(), 2);
                final PlayerView local = currentAssignedPlayerOnEdt(ui, view, players, expectedSeat);
                final NetGameController controller = localController.get();
                final PlayerView opponent = hostPlayer.get();
                final var label = hostMain1.get();
                Assert.assertEquals(view.getId(), expectedGameViewId);
                Assert.assertSame(view.getTracker(), expectedTracker.get());
                Assert.assertSame(opponent.getTracker(), expectedTracker.get());
                Assert.assertSame(players.get(1 - expectedSeat), opponent,
                        "the retained opponent is still the current host player object");
                Assert.assertTrue(ui.isCurrentScreen());
                Assert.assertSame(ui.getGameController(local), controller);
                Assert.assertSame(ui.getFieldViewFor(opponent).getPhaseIndicator()
                        .getLabelFor(PhaseType.MAIN1), label,
                        "the retained phase label remains bound to the current opponent and phase");
                Assert.assertTrue(label.isShowing());
                Assert.assertFalse(label.getEnabled());
                label.dispatchEvent(new MouseEvent(label, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
                        0, Math.max(1, label.getWidth() / 2), Math.max(1, label.getHeight() / 2), 1,
                        false, MouseEvent.BUTTON1));
                Assert.assertTrue(label.getEnabled(), "the production phase-label mouse listener enabled MAIN1");
                Assert.assertFalse(ui.isUiSetToSkipPhase(opponent, PhaseType.MAIN1));
            });
            RESULT.println("INVERSE_PHASE_SKIP_TOGGLED|REMOTE|LABEL_ENABLED=1|UI_SKIP=0");
            RESULT.flush();
            Assert.assertEquals(WORKER_COMMANDS.readLine(), "HOST_SKIP_APPLIED");
        }

        private static boolean currentHostUiBinding(final CMatchUI ui, final int expectedSeat,
                final int expectedGameViewId, final Tracker expectedTracker,
                final PlayerControllerHuman remoteController) {
            final AtomicReference<Boolean> valid = new AtomicReference<>(false);
            FThreads.invokeInEdtAndWait(() -> {
                final GameView view = ui.getGameView();
                if (view == null || view.getId() != expectedGameViewId || view.getTracker() != expectedTracker
                        || !ui.isCurrentScreen()) return;
                final List<PlayerView> players = new ArrayList<>(view.getPlayers());
                if (players.size() != 2) return;
                currentAssignedPlayerOnEdt(ui, view, players, expectedSeat);
                if (remoteController.getGame() == null
                        || remoteController.getGame().getView().getId() != expectedGameViewId) return;
                valid.set(true);
            });
            return valid.get();
        }

        private static void passOpeningPriorityOnEdt(final CMatchUI ui, final String role,
                final int expectedSeat, final Game expectedGame, final Set<PhaseType> passedHostPhases,
                final Set<PhaseType> passedRemotePhases) {
            FThreads.invokeInEdtAndWait(() -> {
                final GameView view = ui.getGameView();
                final PhaseType phase = view == null ? null : view.getPhase();
                final String facts = openingPriorityFacts(ui, role, expectedSeat, expectedGame, phase);
                if (view == null || ui.getCurrentPlayer() == null || !ui.isCurrentScreen()
                        || view.getTurn() != 1 || view.getStack() == null || !view.getStack().isEmpty()) {
                    recordOpeningPriorityDiagnostic("VIEW_TURN_OR_STACK", facts, false);
                    return;
                }
                if (phase != PhaseType.UNTAP && phase != PhaseType.UPKEEP && phase != PhaseType.DRAW) {
                    recordOpeningPriorityDiagnostic("PHASE_NOT_ELIGIBLE", facts, false);
                    return;
                }
                final List<PlayerView> players = new ArrayList<>(view.getPlayers());
                final PlayerView local = currentAssignedPlayerOnEdt(ui, view, players, expectedSeat);
                final Object controller = ui.getGameController(local);
                if (!hasOpeningPriorityControls(ui, phase)) {
                    recordOpeningPriorityDiagnostic("PROMPT_OR_BUTTON_GUARD", facts, false);
                    return;
                }

                if ("HOST".equals(role)) {
                    if (!(controller instanceof PlayerControllerHuman pch) || expectedGame == null
                            || pch.getGame() != expectedGame || pch.getGame().getView().getId() != view.getId()) {
                        recordOpeningPriorityDiagnostic("HOST_CONTROLLER_OR_GAME", facts, false);
                        return;
                    }
                    final Input input = pch.getInputProxy().getInput();
                    if (!(input instanceof InputPassPriority) || input.getOwner() == null
                            || input.getOwner().getId() != local.getId()
                            || pch.getGame().getPhaseHandler().getTurn() != 1
                            || pch.getGame().getPhaseHandler().getPhase() != phase
                            || pch.getGame().getPhaseHandler().getPriorityPlayer() != pch.getPlayer()
                            || !pch.getGame().getStack().isEmpty() || passedHostPhases.contains(phase)
                            || pch.getInputQueue().getInput() != input) {
                        recordOpeningPriorityDiagnostic("HOST_INPUT_OR_PRIORITY_GUARD", facts, false);
                        return;
                    }
                    if (ui.getGameView() != view || ui.getGameController(local) != pch
                            || pch.getInputProxy().getInput() != input
                            || pch.getInputQueue().getInput() != input
                            || view.getTurn() != 1 || view.getPhase() != phase
                            || view.getStack() == null || !view.getStack().isEmpty()
                            || pch.getGame().getPhaseHandler().getTurn() != 1
                            || pch.getGame().getPhaseHandler().getPhase() != phase
                            || pch.getGame().getPhaseHandler().getPriorityPlayer() != pch.getPlayer()
                            || !pch.getGame().getStack().isEmpty()
                            || !hasOpeningPriorityControls(ui, phase)) {
                        recordOpeningPriorityDiagnostic("HOST_REVALIDATION_GUARD", facts, false);
                        return;
                    }
                    final AbstractButton ok = openingPriorityOkButton(ui);
                    if (ok == null) {
                        recordOpeningPriorityDiagnostic("HOST_OK_BUTTON_GUARD", facts, false);
                        return;
                    }
                    passedHostPhases.add(phase);
                    ok.doClick(0);
                    recordOpeningPriorityDiagnostic("DISPATCHED", facts, true);
                } else {
                    if (!(controller instanceof NetGameController)) {
                        recordOpeningPriorityDiagnostic("REMOTE_CONTROLLER_GUARD", facts, false);
                        return;
                    }
                    if (!local.getHasPriority()) {
                        recordOpeningPriorityDiagnostic("REMOTE_APPLIED_PRIORITY_GUARD", facts, false);
                        return;
                    }
                    if (passedRemotePhases.contains(phase)) {
                        recordOpeningPriorityDiagnostic("REMOTE_PHASE_ALREADY_PASSED", facts, false);
                        return;
                    }
                    if (ui.getGameView() != view || ui.getGameController(local) != controller
                            || view.getTurn() != 1 || view.getPhase() != phase
                            || view.getStack() == null || !view.getStack().isEmpty()
                            || !local.getHasPriority() || !hasOpeningPriorityControls(ui, phase)) {
                        recordOpeningPriorityDiagnostic("REMOTE_REVALIDATION_GUARD", facts, false);
                        return;
                    }
                    final AbstractButton ok = openingPriorityOkButton(ui);
                    if (ok == null) {
                        recordOpeningPriorityDiagnostic("REMOTE_OK_BUTTON_GUARD", facts, false);
                        return;
                    }
                    passedRemotePhases.add(phase);
                    ok.doClick(0);
                    recordOpeningPriorityDiagnostic("DISPATCHED", facts, true);
                }
            });
        }

        private static String openingPriorityFacts(final CMatchUI ui, final String role,
                final int expectedSeat, final Game expectedGame, final PhaseType phase) {
            final StringBuilder facts = new StringBuilder("ROLE=").append(role)
                    .append("|PHASE=").append(phase == null ? "NONE" : phase.name());
            try {
                final GameView view = ui.getGameView();
                facts.append("|VIEW=").append(view == null ? 0 : 1)
                        .append("|SCREEN=").append(ui.isCurrentScreen() ? 1 : 0)
                        .append("|TURN1=").append(view != null && view.getTurn() == 1 ? 1 : 0)
                        .append("|VIEW_STACK_EMPTY=").append(view != null && view.getStack() != null
                                && view.getStack().isEmpty() ? 1 : 0)
                        .append("|HOST_VIEW_MATCH=").append(expectedGame == null ? "NA"
                                : view != null && expectedGame.getView().getId() == view.getId() ? "1" : "0");
                if (view == null || expectedSeat < 0 || expectedSeat >= view.getPlayers().size()) {
                    facts.append("|LOCAL=unavailable");
                    return facts.toString();
                }
                final List<PlayerView> players = new ArrayList<>(view.getPlayers());
                final PlayerView local = players.get(expectedSeat);
                final Object controller = ui.getGameController(local);
                facts.append("|LOCAL_SEAT=").append(seatOf(players, ui.getCurrentPlayer()))
                        .append("|CONTROLLER=").append(controller instanceof PlayerControllerHuman ? "HUMAN"
                                : controller instanceof NetGameController ? "NETWORK" : "OTHER")
                        .append("|LOCAL_HAS_PRIORITY=").append(local.getHasPriority() ? 1 : 0);

                final VPrompt prompt = openingPriorityPrompt(ui);
                final String promptText = ui.getPromptMessage();
                final Localizer localizer = Localizer.getInstance();
                final boolean promptTurn = promptText != null
                        && promptText.contains(localizer.getMessage("lblTurn") + ": 1");
                final boolean promptPhase = promptText != null && phase != null
                        && promptText.contains(localizer.getMessage("lblPhase") + ": " + phase.nameForUi);
                final boolean promptEmpty = promptText != null && promptText.contains(
                        localizer.getMessage("lblStack") + ": " + localizer.getMessage("lblEmpty"));
                final boolean promptStackLine = promptText != null && promptText.equals(
                        localizer.getMessage("lblStack") + ": " + localizer.getMessage("lblEmpty"));
                final boolean stackYield = promptText != null && promptText.contains(
                        localizer.getMessage("lblCannotRespondToStackYieldPrompt"));
                final boolean noActionsYield = promptText != null && promptText.contains(
                        localizer.getMessage("lblNoActionsAvailableYieldPrompt"));
                facts.append("|PROMPT_BOUND=").append(prompt == null ? 0 : 1)
                        .append("|PROMPT_TURN1=").append(promptTurn ? 1 : 0)
                        .append("|PROMPT_PHASE=").append(promptPhase ? 1 : 0)
                        .append("|PROMPT_EMPTY=").append(promptEmpty ? 1 : 0)
                        .append("|PROMPT_STACK_LINE=").append(promptStackLine ? 1 : 0)
                        .append("|YIELD_STACK=").append(stackYield ? 1 : 0)
                        .append("|YIELD_NO_ACTIONS=").append(noActionsYield ? 1 : 0);
                if (prompt != null) {
                    final AbstractButton ok = prompt.getBtnOK();
                    final AbstractButton cancel = prompt.getBtnCancel();
                    final String undo = localizer.getMessage("lblUndo") + " (";
                    facts.append("|OK_SHOWING=").append(ok.isShowing() ? 1 : 0)
                            .append("|OK_ENABLED=").append(ok.isEnabled() ? 1 : 0)
                            .append("|OK_LABEL=").append(localizer.getMessage("lblOK").equals(ok.getText()) ? 1 : 0)
                            .append("|CANCEL_SHOWING=").append(cancel.isShowing() ? 1 : 0)
                            .append("|CANCEL_ENABLED=").append(cancel.isEnabled() ? 1 : 0)
                            .append("|CANCEL_ENDTURN=").append(localizer.getMessage("lblEndTurn")
                                    .equals(cancel.getText()) ? 1 : 0)
                            .append("|CANCEL_UNDO=").append(cancel.getText() != null
                                    && cancel.getText().startsWith(undo) ? 1 : 0)
                            .append("|CANCEL_ACCEPT=").append(localizer.getMessage("lblAccept")
                                    .equals(cancel.getText()) ? 1 : 0)
                            .append("|CANCEL_DECLINE=").append(localizer.getMessage("lblDecline")
                                    .equals(cancel.getText()) ? 1 : 0);
                }
                if (controller instanceof PlayerControllerHuman pch) {
                    final Input proxyInput = pch.getInputProxy().getInput();
                    final Input queueInput = pch.getInputQueue().getInput();
                    final int proxyOwnerSeat = proxyInput == null || proxyInput.getOwner() == null ? -1
                            : seatOf(players, proxyInput.getOwner());
                    final int queueOwnerSeat = queueInput == null || queueInput.getOwner() == null ? -1
                            : seatOf(players, queueInput.getOwner());
                    final boolean engineGame = pch.getGame() == expectedGame;
                    final boolean enginePriority = engineGame
                            && pch.getGame().getPhaseHandler().getPriorityPlayer() == pch.getPlayer();
                    facts.append("|HOST_GAME=").append(engineGame ? 1 : 0)
                            .append("|ENGINE_TURN1=").append(engineGame
                                    && pch.getGame().getPhaseHandler().getTurn() == 1 ? 1 : 0)
                            .append("|ENGINE_PHASE=").append(engineGame && pch.getGame().getPhaseHandler()
                                    .getPhase() == phase ? 1 : 0)
                            .append("|ENGINE_STACK_EMPTY=").append(engineGame
                                    && pch.getGame().getStack().isEmpty() ? 1 : 0)
                            .append("|ENGINE_PRIORITY_LOCAL=").append(enginePriority ? 1 : 0)
                            .append("|PROXY_TYPE=").append(proxyInput == null ? "NONE"
                                    : proxyInput.getClass().getSimpleName())
                            .append("|QUEUE_TYPE=").append(queueInput == null ? "NONE"
                                    : queueInput.getClass().getSimpleName())
                            .append("|PROXY_IS_PRIORITY=").append(proxyInput instanceof InputPassPriority ? 1 : 0)
                            .append("|QUEUE_IS_PRIORITY=").append(queueInput instanceof InputPassPriority ? 1 : 0)
                            .append("|PROXY_QUEUE_SAME=").append(proxyInput != null && proxyInput == queueInput ? 1 : 0)
                            .append("|PROXY_OWNER_SEAT=").append(proxyOwnerSeat)
                            .append("|QUEUE_OWNER_SEAT=").append(queueOwnerSeat);
                } else {
                    facts.append("|HOST_GAME=NA|ENGINE_TURN1=NA|ENGINE_PHASE=NA|ENGINE_STACK_EMPTY=NA")
                            .append("|ENGINE_PRIORITY_LOCAL=NA|PROXY_TYPE=NA|QUEUE_TYPE=NA")
                            .append("|PROXY_IS_PRIORITY=NA|QUEUE_IS_PRIORITY=NA|PROXY_QUEUE_SAME=NA")
                            .append("|PROXY_OWNER_SEAT=NA|QUEUE_OWNER_SEAT=NA");
                }
            } catch (Throwable diagnosticFailure) {
                facts.append("|FACTS_ERROR=").append(diagnosticFailure.getClass().getSimpleName());
            }
            return facts.toString();
        }

        private static void recordOpeningPriorityDiagnostic(final String gate, final String facts,
                final boolean dispatched) {
            if (dispatched) OPENING_PRIORITY_DISPATCHES.incrementAndGet();
            else if (OPENING_PRIORITY_DISPATCHES.get() == 0) {
                FIRST_OPENING_PRIORITY_REJECTION.compareAndSet(null, gate);
            }
            LAST_OPENING_PRIORITY_DIAGNOSTIC.set("GATE=" + gate + "|" + facts);
        }

        private static boolean hasOpeningPriorityControls(final CMatchUI ui, final PhaseType phase) {
            final VPrompt promptView = openingPriorityPrompt(ui);
            if (promptView == null) return false;
            final String prompt = ui.getPromptMessage();
            if (prompt == null) return false;
            final Localizer localizer = Localizer.getInstance();
            if (!prompt.equals(localizer.getMessage("lblStack") + ": "
                    + localizer.getMessage("lblEmpty"))) return false;
            final AbstractButton ok = promptView.getBtnOK();
            final AbstractButton cancel = promptView.getBtnCancel();
            final String undo = localizer.getMessage("lblUndo") + " (";
            final boolean endTurn = localizer.getMessage("lblEndTurn").equals(cancel.getText());
            final boolean canUndo = cancel.getText() != null && cancel.getText().startsWith(undo);
            return ok.isShowing() && ok.isEnabled() && localizer.getMessage("lblOK").equals(ok.getText())
                    && cancel.isShowing() && cancel.isEnabled() && (endTurn || canUndo);
        }

        private static VPrompt openingPriorityPrompt(final CMatchUI ui) {
            final IVDoc<? extends forge.gui.framework.ICDoc> doc = EDocID.REPORT_MESSAGE.getDoc();
            if (!(doc instanceof VPrompt prompt) || prompt.getLayoutControl().getMatchUI() != ui) return null;
            return prompt;
        }

        private static AbstractButton openingPriorityOkButton(final CMatchUI ui) {
            final VPrompt prompt = openingPriorityPrompt(ui);
            return prompt != null && prompt.getBtnOK().isShowing() && prompt.getBtnOK().isEnabled()
                    && Localizer.getInstance().getMessage("lblOK").equals(prompt.getBtnOK().getText())
                    ? prompt.getBtnOK() : null;
        }

        private static void publishUiProgress(final String role, final int expectedSeat, final UiSnapshot state,
                final ChoiceKind choiceKind, final boolean choiceQueried, final boolean startSeen, final boolean startClicked,
                final boolean mulliganSeen, final boolean mulliganClicked, final String category,
                final ChoiceAction action) {
            final String progress = "ROLE=" + role + "|SEAT=" + expectedSeat
                    + "|BOUND=" + (state == null ? -1 : 1)
                    + "|PHASE=" + (state == null || state.phase() == null ? "UNSET" : state.phase().name())
                    + "|TURN=" + (state == null ? -1 : state.turn())
                    + "|CONTROLLER=" + (state == null ? "UNKNOWN"
                            : state.controller() == null ? "NETWORK" : "HUMAN")
                    + "|SCREEN=" + (state == null ? -1 : 1)
                    + "|HAND=" + (state == null ? -1 : state.renderedOwnHandCount())
                    + "|CHOICE=" + (!choiceQueried ? "NOT_QUERIED" : choiceKind == null ? "NONE" : choiceKind.name())
                    + "|CHOICE_OBSERVED_SHOWING=" + (!choiceQueried ? -1 : choiceKind == null ? 0 : 1)
                    + "|CHOICE_OBSERVED_ENABLED=" + (!choiceQueried ? -1 : choiceKind == null ? 0 : 1)
                    + "|START_SEEN=" + (startSeen ? 1 : 0)
                    + "|START_CLICKED=" + (startClicked ? 1 : 0)
                    + "|MULLIGAN_SEEN=" + (mulliganSeen ? 1 : 0)
                    + "|MULLIGAN_CLICKED=" + (mulliganClicked ? 1 : 0)
                    + "|MULLIGAN_CATEGORY=" + (category == null ? "NONE" : category)
                    + "|INPUT_BEFORE=" + (action == null ? "NOT_SAMPLED" : action.inputBefore())
                    + "|INPUT_OWNER_BEFORE=" + (action == null ? -1 : action.inputOwnerBefore())
                    + "|INPUT_AFTER=" + (action == null ? "NOT_SAMPLED" : action.inputAfter())
                    + "|INPUT_OWNER_AFTER=" + (action == null ? -1 : action.inputOwnerAfter())
                    + "|INPUT_CHANGED=" + (action != null && action.inputChanged() ? 1 : 0)
                    + (inverseRenderedTellingTimeMode
                            ? "|VIEW_ID=" + (state == null ? -1 : state.gameId())
                                    + "|TRACKER=" + (state == null ? -1 : state.trackerToken())
                                    + ("HOST".equals(role) ? "|OPENING_ATTEMPT="
                                            + (LAST_OPENING_PRIORITY_DIAGNOSTIC.get() == null
                                                    ? "none" : LAST_OPENING_PRIORITY_DIAGNOSTIC.get()) : "")
                            : "");
            if (!progress.equals(LAST_UI_PROGRESS.getAndSet(progress))) {
                RESULT.println("UI|" + progress);
                RESULT.flush();
            }
        }

        private static UiSnapshot driveNextGameUi(final CMatchUI ui, final String role, final int expectedSeat,
                final int previousViewId, final boolean expectStartingChoice, final Game expectedGame)
                throws Exception {
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(UI_TIMEOUT_SECONDS);
            boolean answeredStartingChoice = false;
            boolean startPromptDismissed = false;
            boolean mulliganPromptDismissed = false;
            boolean reportedNewView = false;
            String mulliganCategory = null;
            while (System.nanoTime() < deadline) {
                final NextGameUiBindingProbe binding = nextGameUiBindingProbeOnEdt(
                        ui, role, expectedSeat, previousViewId);
                publishNextGameUiBindingProgress(role, expectedSeat, binding);
                if (!binding.ready()) {
                    Thread.sleep(20);
                    continue;
                }
                final UiSnapshot state = observeUiProgressOnEdt(ui, role, expectedSeat);
                if (state == null) {
                    Thread.sleep(20);
                    continue;
                }
                if (state.gameId() == previousViewId) {
                    Thread.sleep(20);
                    continue;
                }

                final ChoiceAction action = inspectAndMaybeClickChoice(ui, expectedSeat, expectedGame,
                        !answeredStartingChoice && expectStartingChoice, mulliganCategory == null);
                final ChoiceKind choiceKind = action.kind();
                if (answeredStartingChoice && choiceKind != ChoiceKind.START) {
                    startPromptDismissed = true;
                }
                if (mulliganCategory != null && choiceKind != ChoiceKind.FREE_MULLIGAN
                        && choiceKind != ChoiceKind.REGULAR_MULLIGAN) {
                    mulliganPromptDismissed = true;
                }
                if (choiceKind == ChoiceKind.START) {
                    if (answeredStartingChoice) {
                        if (startPromptDismissed) {
                            throw new AssertionError("N+1 Play/Draw prompt reappeared after its accepted action");
                        }
                    } else {
                        if (!expectStartingChoice) {
                            throw new AssertionError("N+1 Play/Draw prompt appeared at the waiting seat");
                        }
                        Assert.assertTrue(action.clicked(), "the observed N+1 Play/Draw prompt was clicked atomically");
                        answeredStartingChoice = true;
                    }
                } else if (choiceKind == ChoiceKind.FREE_MULLIGAN
                        || choiceKind == ChoiceKind.REGULAR_MULLIGAN) {
                    if (mulliganCategory != null) {
                        if (mulliganPromptDismissed) {
                            throw new AssertionError("N+1 mulligan prompt reappeared after Keep was accepted");
                        }
                    } else {
                        Assert.assertTrue(action.clicked(), "the observed N+1 mulligan prompt was clicked atomically");
                        mulliganCategory = choiceKind.name().replace("_MULLIGAN", "");
                    }
                }
                publishNextGameUiProgress(role, expectedSeat, state, choiceKind, action,
                        answeredStartingChoice, mulliganCategory);
                if (!reportedNewView) {
                    RESULT.println("N1STEP|" + role + "|STAGE=NEW_UI_VIEW|VIEW_CHANGED=1|TURN="
                            + state.turn() + "|PHASE=" + (state.phase() == null ? "UNSET" : state.phase().name())
                            + "|SCREEN=1|CONTROLLER=" + (state.controller() == null ? "NETWORK" : "HUMAN")
                            + "|HAND=" + state.renderedOwnHandCount() + "|CHOICE="
                            + (choiceKind == null ? "NONE" : choiceKind.name()) + "|CLICKED="
                            + (action.clicked() ? 1 : 0) + "|INPUT_BEFORE=" + action.inputBefore()
                            + "|INPUT_OWNER_BEFORE=" + action.inputOwnerBefore()
                            + "|INPUT_AFTER=" + action.inputAfter()
                            + "|INPUT_OWNER_AFTER=" + action.inputOwnerAfter()
                            + "|INPUT_CHANGED=" + (action.inputChanged() ? 1 : 0));
                    RESULT.flush();
                    reportedNewView = true;
                }

                if (mulliganCategory != null && state.turn() == 1 && state.phase() == PhaseType.MAIN1) {
                    if (answeredStartingChoice != expectStartingChoice) {
                        throw new AssertionError("N+1 starting-player prompt ownership did not match the prior outcome");
                    }
                    if (state.renderedOwnHandCount() != 7) {
                        throw new AssertionError("N+1 local own-hand panel did not render its seven kept cards");
                    }
                    final UiSnapshot finalState = observeUiOnEdt(ui, role, expectedSeat);
                    assertUiGate(finalState != null, "N1_FINAL_SNAPSHOT");
                    Assert.assertEquals(finalState.gameId(), state.gameId(),
                            "N+1 final strict layout belongs to the applied view");
                    Assert.assertEquals(finalState.renderedOwnHandCount(), 7,
                            "N+1 strict snapshot contains the locally rendered seven-card hand");
                    final String promptReceipt = "N1CHOOSER|" + role + "|" + expectedSeat + "|START="
                            + (answeredStartingChoice ? 1 : 0) + "|" + mulliganCategory;
                    return new UiSnapshot(finalState.gameId(), finalState.turn(), finalState.phase(),
                            finalState.publicReceipt(), finalState.controller(), finalState.renderedOwnHandCount(),
                            promptReceipt, finalState.trackerToken());
                }
                Thread.sleep(20);
            }
            throw new AssertionError("rendered client did not answer its N+1 chooser and apply the new MAIN1 view");
        }

        private static void publishNextGameUiProgress(final String role, final int expectedSeat,
                final UiSnapshot state, final ChoiceKind choice, final ChoiceAction action,
                final boolean startClicked, final String mulliganCategory) {
            final String progress = "STAGE=PROGRESS|SEAT=" + expectedSeat + "|GAME_VIEW_CHANGED=1|TURN="
                    + state.turn() + "|PHASE=" + (state.phase() == null ? "UNSET" : state.phase().name())
                    + "|SCREEN=1|CONTROLLER=" + (state.controller() == null ? "NETWORK" : "HUMAN")
                    + "|HAND=" + state.renderedOwnHandCount() + "|CHOICE="
                    + (choice == null ? "NONE" : choice.name()) + "|CLICKED=" + (action.clicked() ? 1 : 0)
                    + "|START_CLICKED=" + (startClicked ? 1 : 0) + "|MULLIGAN="
                    + (mulliganCategory == null ? "NONE" : mulliganCategory)
                    + "|INPUT_BEFORE=" + action.inputBefore() + "|INPUT_OWNER_BEFORE="
                    + action.inputOwnerBefore() + "|INPUT_AFTER=" + action.inputAfter()
                    + "|INPUT_OWNER_AFTER=" + action.inputOwnerAfter() + "|INPUT_CHANGED="
                    + (action.inputChanged() ? 1 : 0);
            if (!progress.equals(LAST_N1_UI_PROGRESS.getAndSet(progress))) {
                RESULT.println("N1STEP|" + role + "|" + progress);
                RESULT.flush();
            }
        }

        private static void publishNextGameUiBindingProgress(final String role, final int expectedSeat,
                final NextGameUiBindingProbe probe) {
            final String progress = "STAGE=BINDING|SEAT=" + expectedSeat + "|VIEW_PRESENT="
                    + (probe.viewPresent() ? 1 : 0) + "|VIEW_CHANGED=" + (probe.viewChanged() ? 1 : 0)
                    + "|DANDAN=" + (probe.danDan() ? 1 : 0) + "|CURRENT_PLAYER="
                    + (probe.currentPlayerPresent() ? 1 : 0) + "|SEAT_MATCH=" + (probe.seatMatch() ? 1 : 0)
                    + "|CONTROLLER=" + probe.controllerClass() + "|ROLE_MATCH="
                    + (probe.roleMatch() ? 1 : 0) + "|SCREEN=" + (probe.currentScreen() ? 1 : 0)
                    + "|LOCAL_COUNT=" + probe.localPlayerCount();
            if (!progress.equals(LAST_N1_UI_BINDING_PROGRESS.getAndSet(progress))) {
                RESULT.println("N1STEP|" + role + "|" + progress);
                RESULT.flush();
            }
        }

        private static NextGameUiBindingProbe nextGameUiBindingProbeOnEdt(final CMatchUI ui, final String role,
                final int expectedSeat, final int previousViewId) {
            final AtomicReference<NextGameUiBindingProbe> result = new AtomicReference<>();
            FThreads.invokeInEdtAndWait(() -> {
                final GameView view = ui.getGameView();
                final PlayerView local = ui.getCurrentPlayer();
                final var currentPlayers = view == null ? null : view.getPlayers();
                final List<PlayerView> players = currentPlayers == null ? List.of()
                        : new ArrayList<>(currentPlayers);
                final int localPlayerCount = ui.getLocalPlayers().size();
                final boolean seatMatch = local != null && !players.isEmpty()
                        && seatOf(players, local) == expectedSeat;
                final Object controller = local == null ? null : ui.getGameController(local);
                final String controllerClass = controller == null ? "NONE" : controller.getClass().getSimpleName();
                final boolean roleMatch = "REMOTE".equals(role) == (controller instanceof NetGameController)
                        && (!"HOST".equals(role) || controller instanceof PlayerControllerHuman);
                final boolean currentScreen = ui.isCurrentScreen();
                final boolean viewPresent = view != null;
                final boolean danDan = viewPresent && view.isDanDan();
                final boolean viewChanged = viewPresent && view.getId() != previousViewId;
                final boolean ready = viewPresent && viewChanged && danDan && local != null && currentScreen
                        && localPlayerCount == 1 && seatMatch && roleMatch;
                result.set(new NextGameUiBindingProbe(viewPresent, viewChanged, danDan, local != null,
                        seatMatch, roleMatch, currentScreen, localPlayerCount, controllerClass, ready));
            });
            return result.get();
        }

        private record NextGameUiBindingProbe(boolean viewPresent, boolean viewChanged, boolean danDan,
                boolean currentPlayerPresent, boolean seatMatch, boolean roleMatch, boolean currentScreen,
                int localPlayerCount, String controllerClass, boolean ready) { }

        private static String clickNextGame(final CMatchUI ui, final String role, final int currentViewId)
                throws Exception {
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(UI_TIMEOUT_SECONDS);
            NextGameUiProbe lastProbe = null;
            boolean reportedInitialProbe = false;
            while (System.nanoTime() < deadline) {
                lastProbe = nextGameUiProbeOnEdt(ui, currentViewId);
                if (!reportedInitialProbe) {
                    RESULT.println("N1STEP|" + role + "|STAGE=CONTINUE_WAIT|" + lastProbe.safeReceipt());
                    RESULT.flush();
                    reportedInitialProbe = true;
                }
                if (lastProbe.choice() != null) {
                    click(lastProbe.choice().button());
                    final String receipt = "N1CONTINUE|" + role + "|" + currentViewId;
                    RESULT.println("N1STEP|" + role + "|STAGE=CONTINUE_CLICKED|VIEW_MATCH_OBSERVED=1"
                            + "|GAME_OVER_OBSERVED=1|CURRENT_SCREEN_OBSERVED="
                            + (lastProbe.currentScreen() ? 1 : 0)
                            + "|NEXT_SHOWING_OBSERVED=1|NEXT_ENABLED_OBSERVED=1");
                    RESULT.println(receipt);
                    RESULT.flush();
                    return receipt;
                }
                Thread.sleep(20);
            }
            RESULT.println("N1STEP|" + role + "|STAGE=CONTINUE_WAIT_TIMEOUT|"
                    + (lastProbe == null ? "UI_PROBE=NONE" : lastProbe.safeReceipt()));
            RESULT.flush();
            throw new AssertionError("actual game-over view did not render its enabled Next Game control; "
                    + (lastProbe == null ? "UI_PROBE=NONE" : lastProbe.safeReceipt()));
        }

        private static NextGameUiProbe nextGameUiProbeOnEdt(final CMatchUI ui, final int currentViewId) {
            final AtomicReference<NextGameUiProbe> result = new AtomicReference<>();
            FThreads.invokeInEdtAndWait(() -> {
                final GameView view = ui.getGameView();
                final boolean viewPresent = view != null;
                final boolean viewMatches = viewPresent && view.getId() == currentViewId;
                final boolean gameOver = viewPresent && view.isGameOver();
                final boolean currentScreen = ui.isCurrentScreen();
                final FView desktopView = Singletons.getView();
                final JFrame frame = desktopView == null ? null : desktopView.getFrame();
                final List<AbstractButton> buttons = frame == null ? List.of() : buttonsIn(frame);
                final String nextGame = Localizer.getInstance().getMessage("btnNextGame");
                int nextCount = 0;
                int showingCount = 0;
                int enabledCount = 0;
                AbstractButton choiceButton = null;
                for (AbstractButton button : buttons) {
                    if (!nextGame.equals(button.getText())) continue;
                    nextCount++;
                    if (button.isShowing()) showingCount++;
                    if (button.isEnabled()) enabledCount++;
                    if (button.isShowing() && button.isEnabled() && choiceButton == null) choiceButton = button;
                }
                final Choice choice = viewMatches && gameOver && desktopView != null && choiceButton != null
                        ? new Choice(ChoiceKind.NEXT_GAME, choiceButton) : null;
                result.set(new NextGameUiProbe(choice, viewPresent, viewMatches, gameOver, currentScreen,
                        frame != null, frame != null && frame.isShowing(), nextCount, showingCount, enabledCount));
            });
            return result.get();
        }

        private record NextGameUiProbe(Choice choice, boolean viewPresent, boolean viewMatches,
                boolean gameOver, boolean currentScreen, boolean framePresent, boolean frameShowing,
                int nextCount, int showingCount, int enabledCount) {
            private String safeReceipt() {
                return "VIEW_PRESENT=" + (viewPresent ? 1 : 0)
                        + "|VIEW_MATCH=" + (viewMatches ? 1 : 0)
                        + "|GAME_OVER=" + (gameOver ? 1 : 0)
                        + "|CURRENT_SCREEN=" + (currentScreen ? 1 : 0)
                        + "|FRAME_PRESENT=" + (framePresent ? 1 : 0)
                        + "|FRAME_SHOWING=" + (frameShowing ? 1 : 0)
                        + "|NEXT_COUNT=" + nextCount
                        + "|NEXT_SHOWING=" + showingCount
                        + "|NEXT_ENABLED=" + enabledCount;
            }
        }

        private static UiSnapshot observeUiProgressOnEdt(final CMatchUI ui, final String role,
                final int expectedSeat) {
            final AtomicReference<UiSnapshot> result = new AtomicReference<>();
            FThreads.invokeInEdtAndWait(() -> result.set(captureUiProgressOnEdt(ui, role, expectedSeat)));
            return result.get();
        }

        private static UiSnapshot captureUiProgressOnEdt(final CMatchUI ui, final String role,
                final int expectedSeat) {
            LAST_UI_GATE.set(null);
            final GameView view = ui.getGameView();
            if (view == null || ui.getCurrentPlayer() == null || !ui.isCurrentScreen()) return null;
            final List<PlayerView> players = new ArrayList<>(view.getPlayers());
            final PlayerView local = currentAssignedPlayerOnEdt(ui, view, players, expectedSeat);
            final Object controller = ui.getGameController(local);
            final boolean networkController = controller instanceof NetGameController;
            assertUiGate(networkController == "REMOTE".equals(role), "CONTROLLER_ROLE");
            assertUiGate(!"HOST".equals(role) || controller instanceof PlayerControllerHuman,
                    "HOST_CONTROLLER");
            assertUiGate(view.isDanDan(), "DANDAN_VARIANT");
            final var localHand = ui.getHandFor(local);
            final int handViews = ui.getHandViews().size();
            assertUiGate(handViews <= 1, "HAND_PANEL_COUNT");
            for (PlayerView player : players) {
                if (!player.equals(local)) assertUiGate(ui.getHandFor(player) == null, "OPPONENT_HAND_PANEL");
            }
            final int renderedOwnHandCount = localHand == null ? -1
                    : localHand.getHandArea().getCardPanels().size();
            return new UiSnapshot(view.getId(), view.getTurn(), view.getPhase(), null,
                    controller instanceof PlayerControllerHuman pch ? pch : null,
                    renderedOwnHandCount, null, System.identityHashCode(view.getTracker()));
        }

        private static UiSnapshot observeUiOnEdt(final CMatchUI ui, final String role, final int expectedSeat) {
            final AtomicReference<UiSnapshot> result = new AtomicReference<>();
            FThreads.invokeInEdtAndWait(() -> {
                final UiSnapshot state = captureUiProgressOnEdt(ui, role, expectedSeat);
                assertUiGate(state != null, "FINAL_SNAPSHOT");
                assertUiGate(state.turn() == 1 && state.phase() == PhaseType.MAIN1, "FINAL_MAIN1");
                final GameView view = ui.getGameView();
                final List<PlayerView> players = new ArrayList<>(view.getPlayers());
                final PlayerView local = currentAssignedPlayerOnEdt(ui, view, players, expectedSeat);
                assertRenderedLayout(ui, players, local);
                final int renderedOwnHandCount = ui.getHandFor(local).getHandArea().getCardPanels().size();
                final Set<Integer> currentHandIds = local.getCards(ZoneType.Hand).stream()
                        .map(card -> card.getId()).collect(java.util.stream.Collectors.toSet());
                final boolean renderedHandUsesCurrentTracker = ui.getHandFor(local).getHandArea()
                        .getCardPanels().stream().allMatch(panel -> panel.getCard() != null
                                && panel.getCard().getTracker() == view.getTracker());
                assertUiGate(renderedHandUsesCurrentTracker, "FINAL_OWN_HAND_CURRENT_GAME");
                final Set<Integer> renderedHandIds = ui.getHandFor(local).getHandArea().getCardPanels().stream()
                        .map(panel -> panel.getCard().getId()).collect(java.util.stream.Collectors.toSet());
                assertUiGate(currentHandIds.size() == renderedOwnHandCount
                        && currentHandIds.equals(renderedHandIds), "FINAL_OWN_HAND_CONTENT");
                result.set(new UiSnapshot(view.getId(), view.getTurn(), view.getPhase(),
                        publicStateReceipt(role, view.getId(), players), state.controller(),
                        renderedOwnHandCount, null, System.identityHashCode(view.getTracker())));
            });
            return result.get();
        }

        private static PlayerView currentAssignedPlayerOnEdt(final CMatchUI ui, final GameView view,
                final List<PlayerView> players, final int expectedSeat) {
            assertUiGate(players.size() == 2 && expectedSeat >= 0 && expectedSeat < players.size(),
                    "CURRENT_GAME_SEATS");
            final Set<PlayerView> localPlayers = ui.getLocalPlayers();
            assertUiGate(localPlayers.size() == 1, "LOCAL_CONTROLLER_COUNT");
            final PlayerView controllerSeat = localPlayers.iterator().next();
            final PlayerView currentSeat = ui.getCurrentPlayer();
            final PlayerView currentGameSeat = players.get(expectedSeat);
            assertUiGate(controllerSeat.equals(currentGameSeat)
                    && seatOf(players, controllerSeat) == expectedSeat, "LOCAL_CONTROLLER_SEAT");
            assertUiGate(currentSeat != null && currentSeat.equals(currentGameSeat)
                    && seatOf(players, currentSeat) == expectedSeat, "CURRENT_PLAYER_SEAT");
            assertUiGate(currentGameSeat.getTracker() == view.getTracker(), "CURRENT_VIEW_PLAYER");
            return currentGameSeat;
        }

        private static void assertUiGate(final boolean passed, final String gate) {
            if (!passed) {
                LAST_UI_GATE.set(gate);
                throw new AssertionError("rendered UI invariant failed");
            }
        }

        private static String publicStateReceipt(final String role, final int gameId, final List<PlayerView> players) {
            final StringBuilder orderedPublicCards = new StringBuilder();
            final PlayerView canonicalSeat = players.get(0);
            final List<Integer> libraryIds = canonicalSeat.getCards(ZoneType.Library).stream()
                    .map(card -> card.getId()).toList();
            final List<Integer> graveyardIds = canonicalSeat.getCards(ZoneType.Graveyard).stream()
                    .map(card -> card.getId()).toList();
            for (ZoneType zone : List.of(ZoneType.Library, ZoneType.Graveyard)) {
                final List<Integer> ids = zone == ZoneType.Library ? libraryIds : graveyardIds;
                orderedPublicCards.append(zone).append(':').append(ids).append(';');
            }
            try {
                final byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                        .digest(orderedPublicCards.toString().getBytes(StandardCharsets.UTF_8));
                return "APPLIED|" + role + "|MAIN1|" + gameId + "|" + libraryIds.size() + "|"
                        + graveyardIds.size() + "|"
                        + HexFormat.of().formatHex(digest);
            } catch (java.security.NoSuchAlgorithmException impossible) {
                throw new AssertionError(impossible);
            }
        }

        private static void assertHostPhysicalCardConservation(final Game game) {
            Assert.assertTrue(game.getRules().isDanDan(), "the captured host game uses DanDan rules");
            Assert.assertEquals(game.getPhaseHandler().getPhase(), PhaseType.MAIN1,
                    "physical conservation is checked at the captured host MAIN1 checkpoint");
            final List<Player> players = game.getRegisteredPlayers();
            Assert.assertEquals(players.size(), 2, "the host engine retains exactly two registered seats");
            Assert.assertSame(players.get(0).getZone(ZoneType.Library), players.get(1).getZone(ZoneType.Library),
                    "the actual game has one shared physical library zone");
            Assert.assertSame(players.get(0).getZone(ZoneType.Graveyard), players.get(1).getZone(ZoneType.Graveyard),
                    "the actual game has one shared physical graveyard zone");
            final var cards = game.getCardsInGame();
            Assert.assertEquals(cards.size(), 80, "the host's captured MAIN1 game retains all 80 physical cards");
            final Set<Integer> physicalIds = new HashSet<>();
            for (var card : cards) physicalIds.add(card.getId());
            Assert.assertEquals(physicalIds.size(), 80,
                    "the host's captured MAIN1 game has 80 distinct physical card identities");
        }

        private record UiSnapshot(int gameId, int turn, PhaseType phase, String publicReceipt,
                PlayerControllerHuman controller, int renderedOwnHandCount, String promptReceipt,
                int trackerToken) { }

        private static void assertRenderedLayout(final CMatchUI ui, final List<PlayerView> players,
                final PlayerView local) {
            final List<PlayerView> byLibrary = new ArrayList<>(players);
            final PlayerView first = byLibrary.get(0);
            final PlayerView second = byLibrary.get(1);
            final List<Integer> firstLibrary = first.getCards(ZoneType.Library).stream()
                    .map(card -> card.getId()).toList();
            final List<Integer> secondLibrary = second.getCards(ZoneType.Library).stream()
                    .map(card -> card.getId()).toList();
            final List<Integer> firstGraveyard = first.getCards(ZoneType.Graveyard).stream()
                    .map(card -> card.getId()).toList();
            final List<Integer> secondGraveyard = second.getCards(ZoneType.Graveyard).stream()
                    .map(card -> card.getId()).toList();
            assertUiGate(firstLibrary.equals(secondLibrary) && firstGraveyard.equals(secondGraveyard),
                    "PUBLIC_PILE_ORDER");
            assertUiGate(ui.getHandViews().size() == 1 && ui.getHandFor(local) != null,
                    "FINAL_OWN_HAND_PANEL");
            for (PlayerView player : players) {
                if (!player.equals(local)) assertUiGate(ui.getHandFor(player) == null, "OPPONENT_HAND_PANEL");
            }
        }

        private static ChoiceAction inspectAndMaybeClickChoice(final CMatchUI ui, final int expectedSeat,
                final Game expectedGame,
                final boolean allowStart,
                final boolean allowMulligan) throws Exception {
            return inspectAndMaybeClickChoice(ui, expectedSeat, expectedGame, allowStart, allowMulligan, true);
        }

        private static ChoiceAction inspectAndMaybeClickChoice(final CMatchUI ui, final int expectedSeat,
                final Game expectedGame, final boolean allowStart, final boolean allowMulligan,
                final boolean choosePlay) throws Exception {
            final AtomicReference<ChoiceAction> action = new AtomicReference<>();
            FThreads.invokeInEdtAndWait(() -> {
                final InputObservation before = observeHostInputOnEdt(ui, expectedSeat, expectedGame);
                final Choice choice = findVisibleChoice(choosePlay);
                boolean clicked = false;
                if (choice != null && ((choice.kind() == ChoiceKind.START && allowStart)
                        || ((choice.kind() == ChoiceKind.FREE_MULLIGAN
                                || choice.kind() == ChoiceKind.REGULAR_MULLIGAN) && allowMulligan))) {
                    final Choice current = findVisibleChoice(choosePlay);
                    if (current != null && current.kind() == choice.kind()
                            && current.button() == choice.button()
                            && choice.button().isShowing() && choice.button().isEnabled()) {
                        if (inverseRenderedTellingTimeMode && expectedSeat == 1
                                && (choice.kind() == ChoiceKind.FREE_MULLIGAN
                                        || choice.kind() == ChoiceKind.REGULAR_MULLIGAN)) {
                            addInverseRemoteVisibleOpeningKnowledgeOnEdt(ui, expectedSeat);
                        }
                        choice.button().doClick(0);
                        clicked = true;
                    }
                }
                final InputObservation after = observeHostInputOnEdt(ui, expectedSeat, expectedGame);
                action.set(new ChoiceAction(choice == null ? null : choice.kind(), clicked,
                        before.kind(), before.ownerSeat(), after.kind(), after.ownerSeat(),
                        before.input() != after.input()));
            });
            return action.get();
        }

        private static InputObservation observeHostInputOnEdt(final CMatchUI ui, final int expectedSeat,
                final Game expectedGame) {
            final GameView view = ui.getGameView();
            if (view == null || ui.getCurrentPlayer() == null) return InputObservation.unavailable();
            final List<PlayerView> players = new ArrayList<>(view.getPlayers());
            final PlayerView local = currentAssignedPlayerOnEdt(ui, view, players, expectedSeat);
            final Object controller = ui.getGameController(local);
            if (!(controller instanceof PlayerControllerHuman pch)
                    || pch.getGame() != expectedGame || pch.getGame().getView().getId() != view.getId()) {
                return InputObservation.unavailable();
            }
            final Input input = pch.getInputProxy().getInput();
            if (input == null) return new InputObservation(null, "NONE", -1);
            final int ownerSeat = input.getOwner() == null ? -1
                    : seatOf(players, input.getOwner());
            return new InputObservation(input, input.getClass().getSimpleName(), ownerSeat);
        }

        private static int seatOf(final List<PlayerView> players, final PlayerView local) {
            for (int i = 0; i < players.size(); i++) if (players.get(i).getId() == local.getId()) return i;
            return -1;
        }

        private static Choice findVisibleChoice() {
            return findVisibleChoice(true);
        }

        private static Choice findVisibleChoice(final boolean choosePlay) {
            final Localizer localizer = Localizer.getInstance();
            final String keep = localizer.getMessage("lblKeep");
            final String mulligan = localizer.getMessage("lblMulligan");
            final String free = localizer.getMessage("lblDanDanFreeMulligan");
            final String regular = localizer.getMessage("lblDanDanRegularMulligan");
            final String play = localizer.getMessage("lblPlay");
            final String draw = localizer.getMessage("lblDraw");

            for (Window window : Window.getWindows()) {
                if (!(window instanceof JDialog dialog) || !dialog.isShowing()) continue;
                List<AbstractButton> buttons = buttonsIn(dialog);
                AbstractButton keepButton = enabledButton(buttons, keep);
                if (keepButton != null && enabledButton(buttons, free) != null
                        && enabledButton(buttons, regular) != null) {
                    return new Choice(ChoiceKind.FREE_MULLIGAN, keepButton);
                }
                AbstractButton playButton = enabledButton(buttons, play);
                AbstractButton drawButton = enabledButton(buttons, draw);
                if (playButton != null && drawButton != null) {
                    return new Choice(ChoiceKind.START, choosePlay ? playButton : drawButton);
                }
            }

            if (Singletons.getView() == null) return null;
            List<AbstractButton> buttons = buttonsIn(Singletons.getView().getFrame());
            AbstractButton playButton = enabledButton(buttons, play);
            AbstractButton drawButton = enabledButton(buttons, draw);
            if (playButton != null && drawButton != null) {
                return new Choice(ChoiceKind.START, choosePlay ? playButton : drawButton);
            }
            AbstractButton keepButton = enabledButton(buttons, keep);
            if (keepButton != null && enabledButton(buttons, mulligan) != null) {
                return new Choice(ChoiceKind.REGULAR_MULLIGAN, keepButton);
            }
            return null;
        }

        private static AbstractButton enabledButton(final List<AbstractButton> buttons, final String text) {
            for (AbstractButton button : buttons) {
                if (button.isShowing() && button.isEnabled() && text.equals(button.getText())) return button;
            }
            return null;
        }

        private static List<AbstractButton> buttonsIn(final Component root) {
            final List<AbstractButton> result = new ArrayList<>();
            collectButtons(root, result);
            return result;
        }

        private static void collectButtons(final Component component, final List<AbstractButton> result) {
            if (component instanceof AbstractButton button) result.add(button);
            if (component instanceof Container container) {
                for (Component child : container.getComponents()) collectButtons(child, result);
            }
        }

        private static void click(final AbstractButton button) throws Exception {
            FThreads.invokeInEdtAndWait(() -> button.doClick(0));
        }

        private static void runRenderedTellingTimeCommand(final String role, final BufferedReader commands)
                throws Exception {
            final CMatchUI ui = ACTIVE_TELLING_TIME_UI.get();
            Assert.assertNotNull(ui, "the actual rendered client UI remains active for the private-log slice");
            if ("HOST".equals(role)) {
                publishTellingTimeArmStage(role, "WAIT_COMMAND");
                final String command = commands.readLine();
                publishTellingTimeArmStage(role, "COMMAND_READ");
                Assert.assertEquals(command, "ARM_TELLING_TIME_LOG");
                armActualReportLog(ui, role);
                RESULT.println("TT_ARMED|HOST");
                RESULT.flush();
                Assert.assertEquals(commands.readLine(), "CAST_TELLING_TIME");
                final TellingTimeSetup setup = TELLING_TIME_SETUP.get();
                final String receipt = castAndResolveTellingTime(ui, setup);
                RESULT.println(receipt);
                RESULT.flush();
            } else {
                final AtomicReference<Integer> armedGameViewId = new AtomicReference<>();
                final AtomicReference<Tracker> armedGameTracker = new AtomicReference<>();
                publishTellingTimeArmStage(role, "PREP_REQUEST");
                FThreads.invokeInEdtAndWait(() -> {
                    publishTellingTimeArmStage(role, "PREP_ENTER");
                    Assert.assertNotNull(ui.getGameView(), "remote fixture retains its applied game view");
                    armedGameViewId.set(ui.getGameView().getId());
                    armedGameTracker.set(ui.getGameView().getTracker());
                });
                publishTellingTimeArmStage(role, "PREP_RETURN");
                publishTellingTimeArmStage(role, "WAIT_COMMAND");
                final String arm = commands.readLine();
                publishTellingTimeArmStage(role, "COMMAND_READ");
                Assert.assertNotNull(arm);
                final String[] fields = arm.split("\\|", -1);
                Assert.assertEquals(fields.length, 4);
                Assert.assertEquals(fields[0], "ARM_TELLING_TIME_LOG");
                final PrivateLogRoles roles = new PrivateLogRoles(fields[1], fields[2], fields[3]);
                armActualReportLog(ui, role);
                RESULT.println("TT_ARMED|REMOTE");
                RESULT.flush();
                Assert.assertEquals(commands.readLine(), "PASS_TELLING_TIME_STACK");
                final boolean passed = passActualRemoteStackPriority(ui, armedGameViewId.get());
                RESULT.println("TT_PASS|REMOTE|STACK_PASS=" + (passed ? 1 : 0));
                RESULT.flush();
                Assert.assertTrue(passed, "remote passed the actual nonempty spell stack through its controller");
                Assert.assertEquals(commands.readLine(), "VERIFY_TELLING_TIME_LOG");
                final String receipt = inspectActualReportLog(ui, roles, armedGameViewId.get(), armedGameTracker.get());
                RESULT.println(receipt);
                RESULT.flush();
            }
        }

        private static void runInverseRenderedTellingTimeCommand(final String role,
                final BufferedReader commands) throws Exception {
            final CMatchUI ui = ACTIVE_TELLING_TIME_UI.get();
            Assert.assertNotNull(ui, "the actual rendered client UI remains active for the inverse Telling Time slice");
            final AtomicReference<Integer> gameViewId = new AtomicReference<>();
            final AtomicReference<Tracker> gameTracker = new AtomicReference<>();
            final AtomicInteger assignedPlayerViewId = new AtomicInteger(-1);
            FThreads.invokeInEdtAndWait(() -> {
                Assert.assertNotNull(ui.getGameView());
                gameViewId.set(ui.getGameView().getId());
                gameTracker.set(ui.getGameView().getTracker());
                if (fullSyncMode && "REMOTE".equals(role)) {
                    final List<PlayerView> players = new ArrayList<>(ui.getGameView().getPlayers());
                    assignedPlayerViewId.set(currentAssignedPlayerOnEdt(ui, ui.getGameView(), players, 1).getId());
                }
            });
            final String arm = commands.readLine();
            Assert.assertNotNull(arm);
            final String[] fields = arm.split("\\|", -1);
            Assert.assertEquals(fields.length, "HOST".equals(role) ? 1 : 12);
            Assert.assertEquals(fields[0], "ARM_INVERSE_TELLING_TIME_LOG");
            final PrivateLogRoles privateRoles = "HOST".equals(role) ? null
                    : new PrivateLogRoles(fields[1], fields[2], fields[3],
                            Set.of(Integer.parseInt(fields[4]), Integer.parseInt(fields[5])),
                            Integer.parseInt(fields[6]), Integer.parseInt(fields[7]),
                            Set.of(Integer.parseInt(fields[8]), Integer.parseInt(fields[9]),
                                    Integer.parseInt(fields[10])), Integer.parseInt(fields[11]));
            armActualReportLog(ui, role);
            if ("REMOTE".equals(role)) addInverseRemoteRenderedKnowledge(ui, gameViewId.get(), gameTracker.get());
            if (fullSyncMode && "REMOTE".equals(role)) {
                final FullStateTap tap = fullStateTap;
                Assert.assertNotNull(tap, "the remote FULL observer was installed before lobby readiness");
                tap.arm(gameViewId.get(), assignedPlayerViewId.get());
            }
            RESULT.println("TT_INVERSE_ARMED|" + role);
            RESULT.flush();
            Assert.assertEquals(commands.readLine(), "DRIVE_INVERSE_TELLING_TIME");
            final String receipt = "HOST".equals(role)
                    ? driveInverseHostTellingTime(ui, TELLING_TIME_SETUP.get(), gameViewId.get(), gameTracker.get())
                    : driveInverseRemoteTellingTime(ui, privateRoles, gameViewId.get(), gameTracker.get());
            if ("REMOTE".equals(role)) {
                final Set<String> known = new HashSet<>(INVERSE_REMOTE_EARNED_KNOWLEDGE.get());
                known.addAll(Set.of(privateRoles.privateOneName(), privateRoles.privateTwoName(),
                        privateRoles.privateThreeName()));
                INVERSE_REMOTE_EARNED_KNOWLEDGE.set(Set.copyOf(known));
            }
            RESULT.println(receipt);
            RESULT.flush();
            Assert.assertEquals(commands.readLine(), "DRIVE_INVERSE_FENGRAF");
            final String fengrafReceipt = "HOST".equals(role)
                    ? driveInverseHostFengraf(ui, TELLING_TIME_SETUP.get(), gameViewId.get(), gameTracker.get())
                    : driveInverseRemoteFengraf(ui, privateRoles, gameViewId.get(), gameTracker.get());
            if ("REMOTE".equals(role)) addInverseRemoteRenderedKnowledge(ui, gameViewId.get(), gameTracker.get());
            RESULT.println(fengrafReceipt);
            if (fullSyncMode && "REMOTE".equals(role)) {
                final FullStateTap tap = fullStateTap;
                Assert.assertNotNull(tap);
                RESULT.println(tap.finishReceipt());
                RESULT.flush();
                fullStateTap = null;
            }
            RESULT.flush();
            if ("HOST".equals(role)) {
                Assert.assertEquals(commands.readLine(), "ADVANCE_REMOTE_T1_MAIN1");
                final InputPassPriority remoteT1Input = advanceInverseHostToRemoteTurnOneMainOne(ui,
                        TELLING_TIME_SETUP.get(), gameViewId.get(), gameTracker.get());
                RESULT.println("DAY_REMOTE_T1_READY|HOST|TURN1_MAIN1=1|REMOTE_PRIORITY=1");
                RESULT.flush();
                Assert.assertEquals(commands.readLine(), "ADVANCE_REMOTE_T2_MAIN1");
                RESULT.println(advanceInverseHostToRemoteTurnTwoMainOne(ui, TELLING_TIME_SETUP.get(),
                        gameViewId.get(), gameTracker.get(), remoteT1Input));
            } else {
                Assert.assertEquals(commands.readLine(), "END_REMOTE_T1_MAIN1");
                RESULT.println(endInverseRemoteTurnOneMainOne(ui, gameViewId.get(), gameTracker.get()));
                RESULT.flush();
                Assert.assertEquals(commands.readLine(), "AWAIT_REMOTE_T2_MAIN1");
                RESULT.println(awaitInverseRemoteTurnTwoMainOne(ui, gameViewId.get(), gameTracker.get()));
                addInverseRemoteRenderedKnowledge(ui, gameViewId.get(), gameTracker.get());
            }
            RESULT.flush();
            if ("HOST".equals(role)) {
                Assert.assertEquals(commands.readLine(), "ARM_DAY_SHUFFLE");
                final TellingTimeSetup setup = TELLING_TIME_SETUP.get();
                final DayShuffleObserver observer = new DayShuffleObserver(
                        setup.game(), setup.host().getView().getId(), setup.remote().getView().getId(),
                        setup.daysUndoing().getId(), setup.historySentinelName());
                // Game subscriptions are add-only; disarm this Game N observer and restore its RNG before continuation.
                TELLING_TIME_SETUP.get().game().subscribeToEvents(observer);
                try {
                    Assert.assertEquals(commands.readLine(), "DRIVE_DAY_UNDOING");
                    observer.arm();
                    RESULT.println("DAY_CAPTURE_ACTIVE|HOST");
                    RESULT.flush();
                    Assert.assertEquals(commands.readLine(), "BEGIN_DAY_UNDOING");
                    final String dayReceipt = driveInverseHostDayUndoing(ui, setup,
                            observer, gameViewId.get(), gameTracker.get());
                    RESULT.println(dayReceipt);
                    RESULT.flush();
                } finally {
                    observer.disarm();
                }
                Assert.assertEquals(commands.readLine(), "HOLD_INVERSE_RECONNECT_PRIORITY");
                holdInverseReconnectPriority(ui, TELLING_TIME_SETUP.get(), commands);
            } else {
                Assert.assertEquals(commands.readLine(), "DRIVE_DAY_UNDOING");
                final String dayReceipt = driveInverseRemoteDayUndoing(ui, privateRoles,
                        gameViewId.get(), gameTracker.get());
                RESULT.println(dayReceipt);
                RESULT.flush();
                final String checkUnknown = commands.readLine();
                Assert.assertNotNull(checkUnknown);
                final String[] checkFields = checkUnknown.split("\\|", -1);
                Assert.assertEquals(checkFields.length, 3);
                Assert.assertEquals(checkFields[0], "CHECK_DAY_LOG_NAME");
                final int nameLength = Integer.parseInt(checkFields[1]);
                Assert.assertTrue(checkFields[2].matches("[0-9a-f]{64}"));
                final AtomicReference<Boolean> nameFoundInRenderedLog = new AtomicReference<>(false);
                FThreads.invokeInEdtAndWait(() -> {
                    final GameView view = ui.getGameView();
                    Assert.assertNotNull(view);
                    Assert.assertEquals(view.getId(), gameViewId.get());
                    Assert.assertSame(view.getTracker(), gameTracker.get());
                    final String rendered = actualRenderedReportLogText(ui, view);
                    nameFoundInRenderedLog.set(containsNameFingerprint(rendered, nameLength, checkFields[2]));
                });
                Assert.assertFalse(nameFoundInRenderedLog.get(),
                        "the actual remote rendered log omits a current host-hand name unknown to this client");
                RESULT.println("DAY_LOG_PRIVACY|UNKNOWN_CURRENT_HAND_NAME_ABSENT=1");
                RESULT.flush();
                final String reconnectCommand = commands.readLine();
                Assert.assertNotNull(reconnectCommand);
                final String[] reconnectFields = reconnectCommand.split("\\|", -1);
                Assert.assertEquals(reconnectFields.length, 3);
                Assert.assertEquals(reconnectFields[0], "RECONNECT_AND_VERIFY_RENDERED_HISTORY");
                final int unknownNameLength = Integer.parseInt(reconnectFields[1]);
                Assert.assertTrue(reconnectFields[2].matches("[0-9a-f]{64}"));
                final String reconnect = reconnectAndVerifyRenderedHistory(ui, privateRoles,
                        gameViewId.get(), gameTracker.get(), fullSyncMode, unknownNameLength, reconnectFields[2]);
                RESULT.println(reconnect);
                RESULT.flush();
            }
        }

        private static void holdInverseReconnectPriority(final CMatchUI ui, final TellingTimeSetup setup,
                final BufferedReader commands) throws Exception {
            Assert.assertNotNull(setup);
            final Game game = setup.game();
            final Player host = setup.host();
            final AtomicReference<GameView> hostView = new AtomicReference<>();
            FThreads.invokeInEdtAndWait(() -> hostView.set(ui.getGameView()));
            final GameView view = hostView.get();
            Assert.assertNotNull(view);
            final int viewId = view.getId();
            final Tracker tracker = view.getTracker();
            final AtomicReference<InputPassPriority> held = new AtomicReference<>();
            FThreads.invokeInEdtAndWait(() -> {
                Assert.assertSame(ui.getGameView(), view);
                Assert.assertEquals(view.getId(), viewId);
                Assert.assertSame(view.getTracker(), tracker);
                Assert.assertSame(game.getView(), view);
                final List<PlayerView> players = new ArrayList<>(view.getPlayers());
                final PlayerView local = currentAssignedPlayerOnEdt(ui, view, players, 0);
                final Input input = setup.controller().getInputProxy().getInput();
                Assert.assertSame(ui.getGameController(local), setup.controller());
                Assert.assertSame(setup.controller().getGame(), game);
                Assert.assertSame(host.getController(), setup.controller());
                Assert.assertTrue(input instanceof InputPassPriority);
                Assert.assertSame(input, setup.controller().getInputQueue().getInput());
                Assert.assertEquals(input.getOwner(), host.getView());
                Assert.assertSame(game.getPhaseHandler().getPriorityPlayer(), host);
                Assert.assertTrue(game.getStack().isEmpty());
                held.set((InputPassPriority) input);
            });
            Assert.assertNotNull(held.get());
            Assert.assertFalse(game.isGameOver());
            Assert.assertNull(game.getOutcome());
            Assert.assertEquals(physicalCardIds(game).size(), 80);
            RESULT.println("RECONNECT_HOLD|HOST");
            RESULT.flush();
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(110);
            while (System.nanoTime() < deadline) {
                Assert.assertSame(setup.controller().getGame(), game);
                Assert.assertSame(game.getView(), view);
                Assert.assertSame(host.getController(), setup.controller());
                Assert.assertSame(setup.controller().getInputProxy().getInput(), held.get());
                Assert.assertSame(setup.controller().getInputQueue().getInput(), held.get());
                Assert.assertSame(game.getPhaseHandler().getPriorityPlayer(), host);
                Assert.assertTrue(game.getStack().isEmpty());
                Assert.assertFalse(game.isGameOver());
                Assert.assertNull(game.getOutcome());
                final AtomicBoolean bound = new AtomicBoolean();
                FThreads.invokeInEdtAndWait(() -> {
                    final GameView current = ui.getGameView();
                    final List<PlayerView> players = current == null ? List.of()
                            : new ArrayList<>(current.getPlayers());
                    if (current == view && current.getId() == viewId && current.getTracker() == tracker
                            && ui.getCurrentPlayer() != null && ui.getCurrentPlayer().equals(host.getView())
                            && currentAssignedPlayerOnEdt(ui, current, players, 0).equals(host.getView())
                            && ui.getGameController(host.getView()) == setup.controller()
                            && setup.controller().getInputProxy().getInput() == held.get()
                            && setup.controller().getInputQueue().getInput() == held.get()) {
                        bound.set(true);
                    }
                });
                Assert.assertTrue(bound.get(), "host retained its same current game, seat, controller and input");
                if (commands.ready()) {
                    Assert.assertEquals(commands.readLine(), "RELEASE_INVERSE_RECONNECT_PRIORITY");
                    Assert.assertEquals(physicalCardIds(game).size(), 80,
                            "the same actual game retains all 80 cards when the remote replay completes");
                    RESULT.println("RECONNECT_RELEASED|HOST");
                    RESULT.flush();
                    return;
                }
                Thread.sleep(20);
            }
            throw new AssertionError("remote reconnect did not finish while host held the exact current input");
        }

        private static InputPassPriority advanceInverseHostToRemoteTurnOneMainOne(final CMatchUI ui,
                final TellingTimeSetup setup, final int expectedViewId, final Tracker expectedTracker)
                throws Exception {
            final Game game = setup.game();
            final Player host = setup.host();
            final Player remote = setup.remote();
            final PlayerControllerHuman remoteController = (PlayerControllerHuman) remote.getController();
            final var handler = game.getPhaseHandler();
            Assert.assertEquals(handler.getTurn(), 1);
            Assert.assertEquals(handler.getPhase(), PhaseType.MAIN1);
            Assert.assertSame(handler.getPriorityPlayer(), host);
            Assert.assertTrue(game.getStack().isEmpty());
            Assert.assertSame(host.getController(), setup.controller());
            Assert.assertSame(remoteController.getGame(), game);
            final Input hostInput = setup.controller().getInputProxy().getInput();
            Assert.assertTrue(hostInput instanceof InputPassPriority);
            Assert.assertSame(hostInput, setup.controller().getInputQueue().getInput());
            Assert.assertEquals(hostInput.getOwner(), host.getView());
            FThreads.invokeInEdtAndWait(() -> {
                final GameView view = ui.getGameView();
                final VPrompt prompt = openingPriorityPrompt(ui);
                Assert.assertNotNull(view);
                Assert.assertSame(view, game.getView());
                Assert.assertEquals(view.getId(), expectedViewId);
                Assert.assertSame(view.getTracker(), expectedTracker);
                Assert.assertEquals(view.getTurn(), 1);
                Assert.assertEquals(view.getPhase(), PhaseType.MAIN1);
                final List<PlayerView> players = new ArrayList<>(view.getPlayers());
                Assert.assertEquals(players.size(), 2);
                Assert.assertSame(players.get(0), host.getView());
                Assert.assertSame(players.get(1), remote.getView());
                Assert.assertSame(ui.getGameController(host.getView()), setup.controller());
                Assert.assertSame(setup.controller().getInputProxy().getInput(), hostInput);
                Assert.assertSame(setup.controller().getInputQueue().getInput(), hostInput);
                Assert.assertSame(game.getPhaseHandler().getPriorityPlayer(), host);
                Assert.assertTrue(game.getStack().isEmpty());
                Assert.assertNotNull(prompt);
                Assert.assertSame(prompt.getLayoutControl().getMatchUI(), ui);
                Assert.assertNotNull(prompt.getParentCell());
                Assert.assertSame(prompt.getParentCell().getSelected(), prompt);
                Assert.assertTrue(prompt.getParentCell().getBody().isShowing());
                Assert.assertTrue(prompt.getBtnCancel().isShowing());
                Assert.assertTrue(prompt.getBtnCancel().isEnabled());
                Assert.assertEquals(prompt.getBtnCancel().getText(), Localizer.getInstance().getMessage("lblEndTurn"));
                prompt.getBtnCancel().doClick(0);
            });
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(70);
            while (System.nanoTime() < deadline) {
                Assert.assertSame(remote.getController(), remoteController);
                Assert.assertSame(remoteController.getGame(), game);
                final Input remoteInput = remoteController.getInputProxy().getInput();
                if (handler.getTurn() == 1 && handler.getPhase() == PhaseType.MAIN1
                        && handler.getPriorityPlayer() == remote && game.getStack().isEmpty()
                        && remoteInput instanceof InputPassPriority
                        && remoteInput == remoteController.getInputQueue().getInput()
                        && remoteInput.getOwner() != null && remoteInput.getOwner().equals(remote.getView())) {
                    final AtomicBoolean appliedRemotePrompt = new AtomicBoolean();
                    FThreads.invokeInEdtAndWait(() -> {
                        final GameView view = ui.getGameView();
                        if (view == null || view != game.getView() || view.getId() != expectedViewId
                                || view.getTracker() != expectedTracker || view.getTurn() != 1
                                || view.getPhase() != PhaseType.MAIN1 || !view.getStack().isEmpty()) return;
                        final List<PlayerView> players = new ArrayList<>(view.getPlayers());
                        if (players.size() == 2 && players.get(0) == host.getView()
                                && players.get(1) == remote.getView() && players.get(1).getHasPriority()) {
                            appliedRemotePrompt.set(true);
                        }
                    });
                    if (appliedRemotePrompt.get()) {
                        Assert.assertFalse(remoteController.canUndoLastAction(),
                                "the remote's localized Cancel action is End Turn, not Undo");
                        Assert.assertNotSame(setup.controller().getInputProxy().getInput(), hostInput,
                                "the actual host End Turn consumed its T1 MAIN1 input");
                        Assert.assertNotSame(setup.controller().getInputQueue().getInput(), hostInput,
                                "the actual host End Turn removed its queued T1 MAIN1 input");
                        Assert.assertTrue(remoteController.getYieldController()
                                .isSkippingPhase(remote.getView(), PhaseType.UPKEEP));
                        Assert.assertTrue(remoteController.getYieldController()
                                .isSkippingPhase(remote.getView(), PhaseType.DRAW));
                        Assert.assertFalse(remoteController.getYieldController()
                                .isSkippingPhase(remote.getView(), PhaseType.MAIN1));
                        return (InputPassPriority) remoteInput;
                    }
                }
                Assert.assertEquals(handler.getTurn(), 1, "remote T1 MAIN1 remained the next controlled input");
                Thread.sleep(20);
            }
            throw new AssertionError("host did not reach the remote T1 MAIN1 input after its real End Turn action");
        }

        private static String endInverseRemoteTurnOneMainOne(final CMatchUI ui,
                final int expectedViewId, final Tracker expectedTracker) throws Exception {
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
            while (System.nanoTime() < deadline) {
                final AtomicBoolean dispatched = new AtomicBoolean();
                FThreads.invokeInEdtAndWait(() -> {
                    final GameView view = ui.getGameView();
                    final PlayerView local = ui.getCurrentPlayer();
                    if (view == null || view.getId() != expectedViewId || view.getTracker() != expectedTracker
                            || view.getTurn() != 1 || view.getPhase() != PhaseType.MAIN1
                            || !view.getStack().isEmpty() || local == null || !ui.isCurrentScreen()) return;
                    final List<PlayerView> players = new ArrayList<>(view.getPlayers());
                    if (players.size() != 2 || players.get(1) != local || !local.getHasPriority()) return;
                    final IGameController controller = ui.getGameController(local);
                    final VPrompt prompt = openingPriorityPrompt(ui);
                    if (!(controller instanceof NetGameController) || ui.getGameView() != view
                            || ui.getCurrentPlayer() != local || ui.getGameController(local) != controller
                            || !local.getHasPriority() || !view.getStack().isEmpty() || view.getTurn() != 1
                            || view.getPhase() != PhaseType.MAIN1 || prompt == null
                            || prompt.getLayoutControl().getMatchUI() != ui || prompt.getParentCell() == null
                            || prompt.getParentCell().getSelected() != prompt
                            || !prompt.getParentCell().getBody().isShowing()
                            || !prompt.getBtnCancel().isShowing() || !prompt.getBtnCancel().isEnabled()
                            || !Localizer.getInstance().getMessage("lblEndTurn").equals(prompt.getBtnCancel().getText())) {
                        return;
                    }
                    prompt.getBtnCancel().doClick(0);
                    dispatched.set(true);
                });
                if (dispatched.get()) {
                    return "DAY_REMOTE_ENDTURN|REMOTE|TURN1_MAIN1=1|VISIBLE_END_TURN=1|DISPATCHED=1";
                }
                Thread.sleep(20);
            }
            throw new AssertionError("remote did not show its actual T1 MAIN1 End Turn control");
        }

        private static String advanceInverseHostToRemoteTurnTwoMainOne(final CMatchUI ui,
                final TellingTimeSetup setup, final int expectedViewId, final Tracker expectedTracker,
                final InputPassPriority heldRemoteInput)
                throws Exception {
            final Game game = setup.game();
            final Player host = setup.host();
            final Player remote = setup.remote();
            final PlayerControllerHuman remoteController = (PlayerControllerHuman) remote.getController();
            final var handler = game.getPhaseHandler();
            Assert.assertNotNull(heldRemoteInput, "the exact remote T1 MAIN1 input was retained before End Turn");
            Assert.assertSame(remoteController.getGame(), game);
            Assert.assertSame(heldRemoteInput.getController(), remoteController);
            Assert.assertEquals(heldRemoteInput.getOwner(), remote.getView());
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(70);
            Input lastDispatchedInput = heldRemoteInput;
            while (System.nanoTime() < deadline) {
                Assert.assertSame(remote.getController(), remoteController);
                Assert.assertSame(remoteController.getGame(), game);
                if (handler.getTurn() == 2 && handler.getPhase() == PhaseType.MAIN1
                        && handler.getPriorityPlayer() == remote && game.getStack().isEmpty()) {
                    final Input currentRemoteInput = remoteController.getInputProxy().getInput();
                    final AtomicBoolean appliedTarget = new AtomicBoolean();
                    FThreads.invokeInEdtAndWait(() -> {
                        final GameView view = ui.getGameView();
                        if (view == null || view != game.getView() || view.getId() != expectedViewId
                                || view.getTracker() != expectedTracker || view.getTurn() != 2
                                || view.getPhase() != PhaseType.MAIN1 || !view.getStack().isEmpty()) return;
                        final List<PlayerView> players = new ArrayList<>(view.getPlayers());
                        if (players.size() == 2 && players.get(0) == host.getView()
                                && players.get(1) == remote.getView() && players.get(1).getHasPriority()) {
                            appliedTarget.set(true);
                        }
                    });
                    if (appliedTarget.get() && currentRemoteInput instanceof InputPassPriority
                            && currentRemoteInput != heldRemoteInput
                            && currentRemoteInput == remoteController.getInputQueue().getInput()
                            && currentRemoteInput.getOwner() != null
                            && currentRemoteInput.getOwner().equals(remote.getView())) {
                        Assert.assertNotSame(remoteController.getInputProxy().getInput(), heldRemoteInput,
                                "remote End Turn consumed its exact T1 MAIN1 input");
                        Assert.assertNotSame(remoteController.getInputQueue().getInput(), heldRemoteInput,
                                "remote End Turn removed its exact queued T1 MAIN1 input");
                        Assert.assertFalse(remoteController.getYieldController().autoPassUntilEndOfTurn(),
                                "Cleanup reset the remote's temporary End Turn yield before T2 MAIN1");
                        Assert.assertTrue(remoteController.getYieldController()
                                .isSkippingPhase(remote.getView(), PhaseType.UPKEEP));
                        Assert.assertTrue(remoteController.getYieldController()
                                .isSkippingPhase(remote.getView(), PhaseType.DRAW));
                        Assert.assertFalse(remoteController.getYieldController()
                                .isSkippingPhase(remote.getView(), PhaseType.MAIN1));
                        return "DAY_PRIORITY_READY|HOST|TURN2_MAIN1=1|REMOTE_PRIORITY=1|T1_INPUT_CONSUMED=1";
                    }
                }
                final Input currentInput = setup.controller().getInputProxy().getInput();
                if (currentInput instanceof InputPassPriority && currentInput != lastDispatchedInput
                        && currentInput == setup.controller().getInputQueue().getInput()
                        && currentInput.getOwner() != null && currentInput.getOwner().equals(host.getView())
                        && handler.getPriorityPlayer() == host && game.getStack().isEmpty()) {
                    if (passInverseHostPriorityOnce(ui, game, setup.controller(), host, currentInput,
                            expectedViewId, expectedTracker)) lastDispatchedInput = currentInput;
                }
                Assert.assertTrue(handler.getTurn() <= 2, "host did not advance beyond remote T2 MAIN1");
                Thread.sleep(20);
            }
            throw new AssertionError("remote did not reach its actual T2 MAIN1 input after End Turn consumption");
        }

        private static String awaitInverseRemoteTurnTwoMainOne(final CMatchUI ui,
                final int expectedViewId, final Tracker expectedTracker) throws Exception {
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(70);
            while (System.nanoTime() < deadline) {
                final AtomicBoolean target = new AtomicBoolean();
                final AtomicBoolean passedTarget = new AtomicBoolean();
                FThreads.invokeInEdtAndWait(() -> {
                    final GameView view = ui.getGameView();
                    final PlayerView local = ui.getCurrentPlayer();
                    if (view == null || view.getId() != expectedViewId || view.getTracker() != expectedTracker
                            || local == null || !ui.isCurrentScreen()) return;
                    final List<PlayerView> players = new ArrayList<>(view.getPlayers());
                    if (players.size() != 2 || players.get(1) != local
                            || !(ui.getGameController(local) instanceof NetGameController)) return;
                    if (view.getTurn() == 2 && view.getPhase() == PhaseType.MAIN1 && local.getHasPriority()
                            && view.getStack().isEmpty() && !players.get(0).getHasPriority()) {
                        target.set(true);
                        return;
                    }
                    passedTarget.set(view.getTurn() > 2);
                });
                if (target.get()) return "DAY_PRIORITY_READY|REMOTE|TURN2_MAIN1=1|LOCAL_PRIORITY=1";
                if (passedTarget.get()) {
                    throw new AssertionError("remote UI advanced beyond its actual T2 MAIN1 priority");
                }
                Thread.sleep(20);
            }
            throw new AssertionError("remote did not reach its actual rendered T2 MAIN1 priority");
        }

        private static boolean passInverseHostPriorityOnce(final CMatchUI ui, final Game game,
                final PlayerControllerHuman controller, final Player host, final Input input,
                final int expectedViewId, final Tracker expectedTracker) throws Exception {
            final AtomicBoolean dispatched = new AtomicBoolean();
            FThreads.invokeInEdtAndWait(() -> {
                final GameView view = ui.getGameView();
                final PhaseType phase = game.getPhaseHandler().getPhase();
                final VPrompt prompt = openingPriorityPrompt(ui);
                if (view == null || view != game.getView() || view.getId() != expectedViewId
                        || view.getTracker() != expectedTracker || view.getTurn() != game.getPhaseHandler().getTurn()
                        || view.getPhase() != phase || ui.getGameController(host.getView()) != controller
                        || controller.getInputProxy().getInput() != input || controller.getInputQueue().getInput() != input
                        || game.getPhaseHandler().getPriorityPlayer() != host || !game.getStack().isEmpty()
                        || !(input instanceof InputPassPriority) || input.getOwner() == null
                        || !input.getOwner().equals(host.getView()) || !hasOpeningPriorityControls(ui, phase)
                        || prompt == null || prompt.getLayoutControl().getMatchUI() != ui
                        || prompt.getParentCell() == null || prompt.getParentCell().getSelected() != prompt
                        || !prompt.getParentCell().getBody().isShowing()) return;
                final List<PlayerView> players = new ArrayList<>(view.getPlayers());
                if (players.size() != 2 || players.get(0) != host.getView()) return;
                openingPriorityOkButton(ui).doClick(0);
                dispatched.set(true);
            });
            return dispatched.get();
        }

        private static String driveInverseHostDayUndoing(final CMatchUI ui, final TellingTimeSetup setup,
                final DayShuffleObserver observer, final int expectedViewId, final Tracker expectedTracker)
                throws Exception {
            final Game game = setup.game();
            final Player host = setup.host();
            final Player remote = setup.remote();
            final PlayerControllerHuman remoteController = (PlayerControllerHuman) remote.getController();
            Assert.assertSame(remoteController.getGame(), game);
            Assert.assertTrue(remoteController.getYieldController()
                    .isSkippingPhase(host.getView(), PhaseType.UPKEEP),
                    "the actual remote PCH skips the host's T3 UPKEEP before Day's Undoing");
            Assert.assertTrue(remoteController.getYieldController()
                    .isSkippingPhase(host.getView(), PhaseType.DRAW),
                    "the actual remote PCH skips the host's T3 DRAW before Day's Undoing");
            Assert.assertFalse(remoteController.getYieldController()
                    .isSkippingPhase(host.getView(), PhaseType.MAIN1),
                    "the actual remote PCH does not skip the host's T3 MAIN1 target");
            final PlayerControllerHuman controller = setup.controller();
            final Set<Integer> expectedManaIds = new HashSet<>();
            setup.manaSources().stream().map(Card::getId).forEach(expectedManaIds::add);
            setup.fengrafManaSources().stream().map(Card::getId).forEach(expectedManaIds::add);
            Assert.assertEquals(expectedManaIds.size(), 5);
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(75);
            Input passedInput = null;
            Set<Integer> paidSourceIds = null;
            boolean passedDayStack = false;
            boolean resolvedAtHostT3 = false;
            while (System.nanoTime() < deadline) {
                final Input input = controller.getInputProxy().getInput();
                if (!passedDayStack && game.getPhaseHandler().getPriorityPlayer() == host
                        && game.getStack().size() == 1
                        && input instanceof InputPassPriority && input == controller.getInputQueue().getInput()
                        && input.getOwner() != null && input.getOwner().equals(host.getView())) {
                    final var ability = game.getStack().peekAbility();
                    Assert.assertNotNull(ability);
                    Assert.assertEquals(ability.getHostCard().getId(), setup.daysUndoing().getId());
                    Assert.assertSame(ability.getActivatingPlayer(), remote);
                    paidSourceIds = ability.getPayingMana().stream().map(mana -> mana.getSourceCard().getId())
                            .collect(java.util.stream.Collectors.toSet());
                    Assert.assertEquals(paidSourceIds.size(), 3,
                            "the actual Day stack records its three-source 2U payment");
                    Assert.assertTrue(expectedManaIds.containsAll(paidSourceIds),
                            "the actual payment uses only the five previously prepared Islands");
                    Assert.assertTrue(paidSourceIds.stream().allMatch(id -> {
                        final Card source = findPhysicalCard(game, id);
                        return source.getName().equals("Island") && source.isTapped();
                    }));
                    final AtomicBoolean passed = new AtomicBoolean();
                    FThreads.invokeInEdtAndWait(() -> {
                        final GameView view = ui.getGameView();
                        final AbstractButton ok = openingPriorityOkButton(ui);
                        if (view == null || view != game.getView() || view.getId() != expectedViewId
                                || view.getTracker() != expectedTracker
                                || view.getTurn() != 2 || view.getPhase() != PhaseType.MAIN1
                                || ui.getGameController(host.getView()) != controller
                                || controller.getInputProxy().getInput() != input
                                || controller.getInputQueue().getInput() != input
                                || game.getPhaseHandler().getPriorityPlayer() != host
                                || game.getStack().size() != 1 || game.getStack().peekAbility() != ability
                                || ok == null) return;
                        final List<PlayerView> players = new ArrayList<>(view.getPlayers());
                        if (players.size() != 2 || players.get(0) != host.getView()
                                || players.get(1) != remote.getView()) return;
                        ok.doClick(0);
                        passed.set(true);
                    });
                    Assert.assertTrue(passed.get(), "host passed the actual paid Day stack through its UI");
                    passedInput = input;
                    passedDayStack = true;
                    continue;
                }
                if (passedDayStack && game.getPhaseHandler().getTurn() == 3
                        && game.getPhaseHandler().getPhase() == PhaseType.MAIN1
                        && game.getPhaseHandler().getPriorityPlayer() == host && game.getStack().isEmpty()
                        && controller.getInputProxy().getInput() instanceof InputPassPriority
                        && controller.getInputProxy().getInput() != passedInput
                        && controller.getInputProxy().getInput() == controller.getInputQueue().getInput()
                        && controller.getInputProxy().getInput().getOwner() != null
                        && controller.getInputProxy().getInput().getOwner().equals(host.getView())) {
                    resolvedAtHostT3 = true;
                    observer.disarm();
                    break;
                }
                if (passedDayStack && game.getPhaseHandler().getTurn() == 3
                        && game.getPhaseHandler().getPhase() != PhaseType.MAIN1
                        && game.getPhaseHandler().getPriorityPlayer() == host && game.getStack().isEmpty()
                        && input instanceof InputPassPriority && input == controller.getInputQueue().getInput()
                        && input != passedInput && input.getOwner() != null
                        && input.getOwner().equals(host.getView())) {
                    if (passInverseHostPriorityOnce(ui, game, controller, host, input,
                            expectedViewId, expectedTracker)) {
                        passedInput = input;
                        continue;
                    }
                }
                Thread.sleep(20);
            }
            Assert.assertTrue(passedDayStack, "host passed the actual remote Day's Undoing stack item");
            Assert.assertTrue(resolvedAtHostT3,
                    "Day's Undoing ended the remote turn and returned priority at host T3 MAIN1");
            Assert.assertNotNull(paidSourceIds);
            Assert.assertNull(observer.witnessFailure.get(), "deterministic Day witness setup remained valid");
            Assert.assertNotNull(observer.expectedSecondShuffle, "the second real shuffle was planned");
            Assert.assertEquals(observer.total(), 2,
                    "the bounded Game N observer saw exactly two shuffle callbacks during Day's Undoing");
            Assert.assertEquals(observer.hostShuffles.get(), 1);
            Assert.assertEquals(observer.remoteShuffles.get(), 1);
            Assert.assertEquals(observer.unassignedShuffles.get(), 0);
            Assert.assertEquals(observer.hostDayDraws.get(), 7,
                    "the actual Day resolution moved seven library cards into the host hand");
            Assert.assertEquals(observer.remoteDayDraws.get(), 7,
                    "the actual Day resolution moved seven library cards into the remote hand");
            Assert.assertEquals(observer.hostTurnThreeDraws.get(), 1,
                    "the host's separate T3 draw happened after Day's seven-card effect");
            Assert.assertEquals(observer.unassignedDayDraws.get(), 0);
            Assert.assertFalse(game.isGameOver());
            Assert.assertNull(game.getOutcome());
            Assert.assertEquals(game.getPhaseHandler().getTurn(), 3);
            Assert.assertEquals(game.getPhaseHandler().getPhase(), PhaseType.MAIN1);
            Assert.assertSame(game.getPhaseHandler().getPriorityPlayer(), host);
            Assert.assertSame(remote.getController(), remoteController);
            Assert.assertTrue(remoteController.getYieldController()
                    .isSkippingPhase(host.getView(), PhaseType.UPKEEP));
            Assert.assertTrue(remoteController.getYieldController()
                    .isSkippingPhase(host.getView(), PhaseType.DRAW));
            Assert.assertFalse(remoteController.getYieldController()
                    .isSkippingPhase(host.getView(), PhaseType.MAIN1));
            Assert.assertTrue(game.getStack().isEmpty());
            final Card resolvedDaysUndoing = findPhysicalCard(game, setup.daysUndoing().getId());
            Assert.assertSame(resolvedDaysUndoing.getOwner(), remote);
            Assert.assertTrue(resolvedDaysUndoing.getZone().is(ZoneType.Exile),
                    "Day's Undoing exiled itself while ending the remote turn");
            final List<Card> preparedIslands = new ArrayList<>(setup.manaSources());
            preparedIslands.addAll(setup.fengrafManaSources());
            Assert.assertEquals(preparedIslands.size(), 5);
            Assert.assertTrue(preparedIslands.stream().filter(Card::isTapped).map(Card::getId)
                            .collect(java.util.stream.Collectors.toSet()).equals(paidSourceIds),
                    "only the three actual Day payment Islands are tapped after remote T2");
            Assert.assertTrue(preparedIslands.stream().allMatch(card -> card.getZone().is(ZoneType.Battlefield)
                            && card.getController() == remote),
                    "all five previously prepared Islands remain on the remote battlefield");
            Assert.assertTrue(physicalCardIds(game).equals(setup.originalPhysicalIds()),
                    "the actual Day effect preserves the official 80 physical cards");
            Assert.assertEquals(host.getCardsIn(ZoneType.Hand).size(), 8,
                    "host drew for its actual T3 after Day's Undoing ended the remote turn");
            Assert.assertEquals(remote.getCardsIn(ZoneType.Hand).size(), 7,
                    "remote has the seven cards drawn by its actual Day's Undoing resolution");
            final List<GameLogEntry> dayEntries = game.getGameLog().getAllEntries().stream()
                    .filter(entry -> entry.sourceCard() != null
                            && entry.sourceCard().getId() == setup.daysUndoing().getId()).toList();
            final GameLogEntry castEntry = dayEntries.stream()
                    .filter(entry -> entry.type() == GameLogEntryType.STACK_ADD).findFirst().orElseThrow();
            final GameLogEntry resolveEntry = dayEntries.stream()
                    .filter(entry -> entry.type() == GameLogEntryType.STACK_RESOLVE).findFirst().orElseThrow();
            Assert.assertFalse(castEntry.message().isBlank());
            Assert.assertFalse(resolveEntry.message().isBlank());
            final String rendered = actualRenderedReportLogText(ui, game.getView());
            Assert.assertTrue(rendered.contains(castEntry.message()) && rendered.contains(resolveEntry.message()));
            Assert.assertFalse(rendered.contains(setup.privateOneName()));
            Assert.assertFalse(rendered.contains(setup.privateTwoName()));
            Assert.assertFalse(rendered.contains(setup.privateThreeName()));
            Assert.assertFalse(rendered.contains(setup.historySentinelName()));
            final String currentHostHandNameHashes = host.getCardsIn(ZoneType.Hand).stream()
                    .map(Card::getName).distinct()
                    .map(name -> nameFingerprint(name) + ":" + name.length()).sorted()
                    .collect(java.util.stream.Collectors.joining(","));
            Assert.assertFalse(currentHostHandNameHashes.isBlank());
            FThreads.invokeInEdtAndWait(() -> {
                final GameView view = ui.getGameView();
                Assert.assertSame(view, game.getView());
                Assert.assertEquals(view.getId(), expectedViewId);
                Assert.assertSame(view.getTracker(), expectedTracker);
                Assert.assertEquals(view.getTurn(), 3);
                Assert.assertEquals(view.getPhase(), PhaseType.MAIN1);
                final List<PlayerView> players = new ArrayList<>(view.getPlayers());
                Assert.assertEquals(players.size(), 2);
                Assert.assertEquals(players.get(0).getZoneSize(ZoneType.Hand), 8);
                Assert.assertEquals(players.get(1).getZoneSize(ZoneType.Hand), 7);
                final Set<PlayerView> localPlayers = ui.getLocalPlayers();
                Assert.assertEquals(localPlayers.size(), 1);
                Assert.assertTrue(localPlayers.contains(players.get(0)));
                Assert.assertFalse(players.get(1).getCards(ZoneType.Hand).isEmpty());
                Assert.assertNull(ui.getHandFor(players.get(1)),
                        "the host renders no opponent hand panel for the hidden cards");
                Assert.assertFalse(CardView.mayViewAny(players.get(1).getCards(ZoneType.Hand), localPlayers),
                        "the current opponent hand contains nonempty cards the host UI may not view");
            });
            return "DAY_HOST|SHUFFLES=" + observer.total() + "|HOST_SHUFFLE="
                    + observer.hostShuffles.get() + "|REMOTE_SHUFFLE=" + observer.remoteShuffles.get()
                    + "|DAY_DRAWS_HOST=" + observer.hostDayDraws.get()
                    + "|DAY_DRAWS_REMOTE=" + observer.remoteDayDraws.get()
                    + "|HOST_T3_DRAW=" + observer.hostTurnThreeDraws.get()
                    + "|T3_MAIN1=1|CARDS=80|HOST_HAND_HASHES=" + currentHostHandNameHashes;
        }

        private static String driveInverseRemoteDayUndoing(final CMatchUI ui, final PrivateLogRoles roles,
                final int expectedViewId, final Tracker expectedTracker) throws Exception {
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(75);
            final Set<Integer> availableManaIds = new HashSet<>(roles.tellingTimeManaIds());
            availableManaIds.addAll(roles.fengrafManaIds());
            Assert.assertEquals(availableManaIds.size(), 5);
            final Set<Integer> dispatchedManaIds = new HashSet<>();
            final Set<Integer> observedPaidManaIds = new HashSet<>();
            final AtomicReference<String> lastManaPrompt = new AtomicReference<>();
            final AtomicReference<String> stackPassGate = new AtomicReference<>("NOT_ATTEMPTED");
            boolean castSent = false;
            boolean stackPassed = false;
            boolean resolvedAtHostT3 = false;
            while (System.nanoTime() < deadline) {
                if (castSent && dispatchedManaIds.size() == 3 && !stackPassed
                        && passInverseRemoteStackPriorityOnce(ui, expectedViewId, expectedTracker,
                                stackPassGate, roles.daysUndoingId(), "Day's Undoing")) {
                    stackPassed = true;
                }
                final boolean castAlreadySent = castSent;
                final AtomicBoolean castNow = new AtomicBoolean();
                final AtomicReference<Throwable> uiFailure = new AtomicReference<>();
                FThreads.invokeInEdtAndWait(() -> {
                    try {
                        final GameView view = ui.getGameView();
                        final PlayerView local = ui.getCurrentPlayer();
                        final IGameController controller = local == null ? null : ui.getGameController(local);
                        if (view == null || view.getId() != expectedViewId || view.getTracker() != expectedTracker
                                || local == null || !ui.isCurrentScreen()
                                || !(controller instanceof NetGameController)) return;
                        Assert.assertSame(local.getTracker(), expectedTracker);
                        if (!castAlreadySent) {
                            if (view.getTurn() != 2 || view.getPhase() != PhaseType.MAIN1
                                    || !local.getHasPriority() || !view.getStack().isEmpty()) return;
                            final VHand hand = ui.getHandFor(local);
                            final CardPanel spellPanel = hand == null ? null
                                    : hand.getHandArea().getCardPanels().stream()
                                            .filter(panel -> panel.getCard() != null
                                                    && panel.getCard().getId() == roles.daysUndoingId()
                                                    && "Day's Undoing".equals(panel.getCard().getName()))
                                            .findFirst().orElse(null);
                            if (spellPanel == null || !spellPanel.isShowing()
                                    || spellPanel.getCard().getTracker() != expectedTracker
                                    || !local.equals(spellPanel.getCard().getOwner())
                                    || !local.equals(spellPanel.getCard().getController())
                                    || spellPanel.getCard().getZone() != ZoneType.Hand) return;
                            if (!controller.selectCard(spellPanel.getCard(), null, null)) {
                                throw new AssertionError("remote did not dispatch the visible Day's Undoing card");
                            }
                            castNow.set(true);
                            return;
                        }

                        final String promptText = ui.getPromptMessage();
                        final IVDoc<?> promptDoc = EDocID.REPORT_MESSAGE.getDoc();
                        final boolean livePrompt = promptDoc instanceof VPrompt prompt
                                && prompt.getLayoutControl().getMatchUI() == ui
                                && prompt.getParentCell() != null && prompt.getParentCell().getSelected() == prompt
                                && prompt.getParentCell().getBody().isShowing()
                                && prompt.getBtnOK().isShowing() && prompt.getBtnCancel().isShowing()
                                && promptText != null && !promptText.isBlank();
                        final CardView castCard = expectedTracker.getObj(
                                TrackableTypes.CardViewType, roles.daysUndoingId());
                        final boolean actualFrozenDayCast = castCard != null
                                && castCard.getTracker() == expectedTracker
                                && castCard.getId() == roles.daysUndoingId()
                                && (castCard.getZone() == ZoneType.Hand || castCard.getZone() == ZoneType.Stack)
                                && local.equals(castCard.getOwner()) && local.equals(castCard.getController())
                                && "Day's Undoing".equals(castCard.getName());
                        final Map<Integer, CardView> islands = new java.util.HashMap<>();
                        for (PlayerView player : view.getPlayers()) {
                            for (CardView card : player.getCards(ZoneType.Battlefield)) {
                                if (card != null && local.equals(card.getController())
                                        && "Island".equals(card.getName()) && availableManaIds.contains(card.getId())
                                        && card.getTracker() == expectedTracker) {
                                    islands.put(card.getId(), card);
                                    if (dispatchedManaIds.contains(card.getId()) && card.isTapped()) {
                                        observedPaidManaIds.add(card.getId());
                                    }
                                }
                            }
                        }
                        final boolean manaPrompt = livePrompt && actualFrozenDayCast
                                && promptText.contains(Localizer.getInstance().getMessage("lblPayManaCost"))
                                && islands.size() == 5;
                        if (manaPrompt && observedPaidManaIds.size() < dispatchedManaIds.size()) return;
                        if (manaPrompt && dispatchedManaIds.size() >= 3) return;
                        if (manaPrompt && !promptText.equals(lastManaPrompt.get())) {
                            final CardView source = islands.values().stream()
                                    .filter(card -> !card.isTapped() && !dispatchedManaIds.contains(card.getId()))
                                    .findFirst().orElse(null);
                            if (source != null) {
                                final VField field = ui.getFieldViewFor(source.getController());
                                final CardPanel panel = field == null ? null
                                        : field.getTabletop().getCardPanel(source.getId());
                                if (panel == null || !panel.isShowing() || !panel.isDisplayEnabled()
                                        || panel.getCard() == null
                                        || panel.getCard().getTracker() != expectedTracker
                                        || panel.getCard().getId() != source.getId()) return;
                                if (!controller.selectCard(source, null, null)) {
                                    throw new AssertionError("remote Island payment source was not accepted");
                                }
                                dispatchedManaIds.add(source.getId());
                                lastManaPrompt.set(promptText);
                            }
                        }
                    } catch (Throwable failure) {
                        uiFailure.set(failure);
                    }
                });
                if (uiFailure.get() != null) {
                    if (uiFailure.get() instanceof Exception exception) throw exception;
                    if (uiFailure.get() instanceof Error error) throw error;
                    throw new AssertionError("remote Day's Undoing UI action failed", uiFailure.get());
                }
                if (castNow.get()) castSent = true;
                if (stackPassed) {
                    final AtomicBoolean atNextTurn = new AtomicBoolean();
                    FThreads.invokeInEdtAndWait(() -> {
                        final GameView view = ui.getGameView();
                        final PlayerView local = ui.getCurrentPlayer();
                        if (view == null || view.getId() != expectedViewId || view.getTracker() != expectedTracker
                                || local == null || !ui.isCurrentScreen()) return;
                        final List<PlayerView> players = new ArrayList<>(view.getPlayers());
                        if (players.size() != 2 || players.get(1) != local || view.getTurn() != 3
                                || !view.getStack().isEmpty()) return;
                        if (view.getPhase() != PhaseType.MAIN1 || !players.get(0).getHasPriority()
                                || !view.getStack().isEmpty()
                                || local.getCards(ZoneType.Hand).size() != 7
                                || players.get(0).getZoneSize(ZoneType.Hand) != 8) return;
                        final VHand hand = ui.getHandFor(local);
                        if (hand == null || hand.getParentCell() == null
                                || hand.getParentCell().getSelected() != hand
                                || !hand.getParentCell().getBody().isShowing()) return;
                        final List<CardPanel> handPanels = hand.getHandArea().getCardPanels();
                        if (handPanels.size() != 7) return;
                        for (CardPanel panel : handPanels) {
                            final CardView card = panel.getCard();
                            if (!panel.isShowing() || card == null || card.getTracker() != expectedTracker
                                    || !local.equals(card.getOwner()) || !local.equals(card.getController())
                                    || card.getZone() != ZoneType.Hand) return;
                        }
                        if (renderedPhysicalCardCount(view) != 80) return;
                        final Set<Integer> currentTappedIslands = new HashSet<>();
                        for (PlayerView player : players) {
                            for (CardView card : player.getCards(ZoneType.Battlefield)) {
                                if (card != null && availableManaIds.contains(card.getId())
                                        && local.equals(card.getController())
                                        && "Island".equals(card.getName()) && card.isTapped()) {
                                    currentTappedIslands.add(card.getId());
                                }
                            }
                        }
                        if (!currentTappedIslands.equals(dispatchedManaIds)) return;
                        Assert.assertNull(ui.getHandFor(players.get(0)),
                                "the remote renders no opponent hand panel for the hidden cards");
                        Assert.assertFalse(CardView.mayViewAny(players.get(0).getCards(ZoneType.Hand),
                                ui.getLocalPlayers()),
                                "the current host hand is a nonempty, unviewable opponent-hand witness");
                        atNextTurn.set(true);
                    });
                    if (atNextTurn.get()) {
                        resolvedAtHostT3 = true;
                        break;
                    }
                }
                Thread.sleep(20);
            }
            Assert.assertTrue(castSent, "remote cast Day's Undoing from its actual rendered T2 hand");
            Assert.assertEquals(dispatchedManaIds.size(), 3,
                    "remote selected three distinct current Island sources through its UI");
            Assert.assertTrue(observedPaidManaIds.equals(dispatchedManaIds),
                    "the applied current view shows each selected payment source tapped");
            Assert.assertTrue(stackPassed, "remote passed the actual Day stack using its current controller");
            Assert.assertTrue(resolvedAtHostT3, "remote observed the actual next turn and seven-card hand");
            addInverseRemoteRenderedKnowledge(ui, expectedViewId, expectedTracker);
            final String remoteKnowledge = INVERSE_REMOTE_EARNED_KNOWLEDGE.get().stream()
                    .map(DanDanTwoJvmMatchUiTest.Worker::nameFingerprint).sorted()
                    .collect(java.util.stream.Collectors.joining(","));
            Assert.assertFalse(remoteKnowledge.isBlank());
            FThreads.invokeInEdtAndWait(() -> {
                final GameView view = ui.getGameView();
                Assert.assertNotNull(view);
                Assert.assertEquals(view.getId(), expectedViewId);
                Assert.assertSame(view.getTracker(), expectedTracker);
                Assert.assertEquals(view.getTurn(), 3);
                Assert.assertEquals(view.getPhase(), PhaseType.MAIN1);
                final List<GameLogEntry> dayEntries = view.getGameLog().getAllEntries().stream()
                        .filter(entry -> entry.sourceCard() != null
                                && entry.sourceCard().getId() == roles.daysUndoingId()).toList();
                final String castMessage = dayEntries.stream()
                        .filter(entry -> entry.type() == GameLogEntryType.STACK_ADD)
                        .map(GameLogEntry::message).findFirst().orElse(null);
                final String resolveMessage = dayEntries.stream()
                        .filter(entry -> entry.type() == GameLogEntryType.STACK_RESOLVE)
                        .map(GameLogEntry::message).findFirst().orElse(null);
                Assert.assertNotNull(castMessage);
                Assert.assertNotNull(resolveMessage);
                Assert.assertFalse(castMessage.isBlank());
                Assert.assertFalse(resolveMessage.isBlank());
                final String rendered = actualRenderedReportLogText(ui, view);
                Assert.assertTrue(rendered.contains(castMessage) && rendered.contains(resolveMessage));
                Assert.assertTrue(privateLogWitnessAbsent(rendered, roles),
                        "the actual remote rendered log omits all setup-private and host-hand sentinel names");
            });
            return "DAY_REMOTE|REMOTE_CAST=1|MANA_SOURCES=" + observedPaidManaIds.size()
                    + "|PUBLIC_CAST=1|PUBLIC_RESOLVE=1|OPPONENT_UNVIEWABLE=1|CARDS=80"
                    + "|REMOTE_KNOWN_HASHES=" + remoteKnowledge;
        }

        private static void addInverseRemoteRenderedKnowledge(final CMatchUI ui, final int expectedViewId,
                final Tracker expectedTracker) {
            final AtomicReference<Set<String>> observed = new AtomicReference<>(Set.of());
            FThreads.invokeInEdtAndWait(() -> {
                final GameView view = ui.getGameView();
                Assert.assertNotNull(view);
                Assert.assertEquals(view.getId(), expectedViewId);
                Assert.assertSame(view.getTracker(), expectedTracker);
                final List<PlayerView> players = new ArrayList<>(view.getPlayers());
                final PlayerView local = currentAssignedPlayerOnEdt(ui, view, players, 1);
                Assert.assertTrue(ui.getGameController(local) instanceof NetGameController);
                final Set<String> known = new HashSet<>();
                for (PlayerView player : players) {
                    for (ZoneType zone : List.of(ZoneType.Battlefield, ZoneType.Graveyard, ZoneType.Exile)) {
                        for (CardView card : player.getCards(zone)) {
                            if (card != null && !card.getName().isBlank()) known.add(card.getName());
                        }
                    }
                }
                for (CardView card : local.getCards(ZoneType.Hand)) {
                    if (card != null && !card.getName().isBlank() && card.getTracker() == expectedTracker) {
                        known.add(card.getName());
                    }
                }
                observed.set(known);
            });
            final Set<String> merged = new HashSet<>(INVERSE_REMOTE_EARNED_KNOWLEDGE.get());
            merged.addAll(observed.get());
            INVERSE_REMOTE_EARNED_KNOWLEDGE.set(Set.copyOf(merged));
        }

        private static void addInverseRemoteVisibleOpeningKnowledgeOnEdt(final CMatchUI ui,
                final int expectedSeat) {
            final GameView view = ui.getGameView();
            Assert.assertNotNull(view);
            Assert.assertNotNull(view.getTracker());
            final List<PlayerView> players = new ArrayList<>(view.getPlayers());
            final PlayerView local = currentAssignedPlayerOnEdt(ui, view, players, expectedSeat);
            Assert.assertTrue(ui.getGameController(local) instanceof NetGameController);
            final VHand hand = ui.getHandFor(local);
            Assert.assertNotNull(hand, "the remote's opening hand is actually rendered before its mulligan action");
            Assert.assertNotNull(hand.getParentCell());
            Assert.assertSame(hand.getParentCell().getSelected(), hand);
            Assert.assertTrue(hand.getParentCell().getBody().isShowing());
            final List<CardPanel> panels = hand.getHandArea().getCardPanels();
            final var cards = local.getCards(ZoneType.Hand);
            Assert.assertEquals(cards.size(), 7,
                    "the current seven-card opening hand is captured before the first mulligan choice");
            Assert.assertEquals(panels.size(), cards.size());
            final Set<Integer> renderedIds = new HashSet<>();
            for (CardPanel panel : panels) {
                final CardView card = panel.getCard();
                Assert.assertTrue(panel.isShowing());
                Assert.assertNotNull(card);
                Assert.assertSame(card.getTracker(), view.getTracker());
                Assert.assertEquals(card.getOwner(), local);
                Assert.assertEquals(card.getController(), local);
                Assert.assertEquals(card.getZone(), ZoneType.Hand);
                Assert.assertTrue(renderedIds.add(card.getId()));
            }
            Assert.assertEquals(renderedIds, cards.stream().map(CardView::getId).collect(
                    java.util.stream.Collectors.toSet()));
            final Set<String> known = new HashSet<>();
            for (PlayerView player : players) {
                for (ZoneType zone : List.of(ZoneType.Battlefield, ZoneType.Graveyard, ZoneType.Exile)) {
                    for (CardView card : player.getCards(zone)) {
                        if (card != null && !card.getName().isBlank()) known.add(card.getName());
                    }
                }
            }
            for (CardPanel panel : panels) {
                if (!panel.getCard().getName().isBlank()) known.add(panel.getCard().getName());
            }
            final Set<String> merged = new HashSet<>(INVERSE_REMOTE_EARNED_KNOWLEDGE.get());
            merged.addAll(known);
            INVERSE_REMOTE_EARNED_KNOWLEDGE.set(Set.copyOf(merged));
        }

        private static String nameFingerprint(final String name) {
            try {
                return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                        .digest(name.getBytes(StandardCharsets.UTF_8)));
            } catch (java.security.NoSuchAlgorithmException impossible) {
                throw new AssertionError("JVM provides SHA-256", impossible);
            }
        }

        private static boolean containsNameFingerprint(final String text, final int nameLength,
                final String expectedFingerprint) {
            if (nameLength <= 0 || nameLength > text.length()) return false;
            try {
                final java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
                for (int start = 0; start + nameLength <= text.length(); start++) {
                    final String candidate = text.substring(start, start + nameLength);
                    if (HexFormat.of().formatHex(digest.digest(candidate.getBytes(StandardCharsets.UTF_8)))
                            .equals(expectedFingerprint)) return true;
                }
                return false;
            } catch (java.security.NoSuchAlgorithmException impossible) {
                throw new AssertionError("JVM provides SHA-256", impossible);
            }
        }

        private static String reconnectAndVerifyRenderedHistory(final CMatchUI ui, final PrivateLogRoles roles,
                final int expectedGameViewId, final Tracker expectedGameTracker, final boolean fullSyncMode,
                final int unknownNameLength, final String unknownNameFingerprint)
                throws Exception {
            Assert.assertNotNull(roles);
            final FGameClient client = ACTIVE_REMOTE_CLIENT.get();
            Assert.assertNotNull(client);
            final Channel previousChannel = activeClientChannel(client);
            final AtomicReference<GameView> previousView = new AtomicReference<>();
            final AtomicReference<VField> previousField = new AtomicReference<>();
            final AtomicReference<NetGameController> previousController = new AtomicReference<>();
            final AtomicReference<List<String>> expectedMessages = new AtomicReference<>();
            final AtomicReference<String> beforeRendered = new AtomicReference<>();
            FThreads.invokeInEdtAndWait(() -> {
                final GameView view = ui.getGameView();
                Assert.assertNotNull(view);
                Assert.assertEquals(view.getId(), expectedGameViewId);
                Assert.assertSame(view.getTracker(), expectedGameTracker);
                final List<PlayerView> players = new ArrayList<>(view.getPlayers());
                final PlayerView local = currentAssignedPlayerOnEdt(ui, view, players, 1);
                Assert.assertSame(local.getTracker(), expectedGameTracker);
                previousView.set(view);
                final IGameController controller = ui.getGameController(local);
                Assert.assertTrue(controller instanceof NetGameController);
                final VField field = ui.getFieldViewFor(local);
                Assert.assertNotNull(field);
                Assert.assertSame(field.getLayoutControl().getMatchUI(), ui);
                Assert.assertNotNull(field.getParentCell());
                Assert.assertTrue(field.getParentCell().getBody().isShowing());
                Assert.assertTrue(field.getAvatarArea().isShowing());
                previousField.set(field);
                previousController.set((NetGameController) controller);
                final List<GameLogEntry> entries = view.getGameLog().getAllEntries();
                final List<GameLogEntry> tellingTime = entries.stream().filter(entry -> entry.sourceCard() != null
                        && "Telling Time".equals(entry.sourceCard().getName())).toList();
                final List<GameLogEntry> fengraf = entries.stream().filter(entry -> entry.sourceCard() != null
                        && entry.sourceCard().getId() == roles.fengrafId()).toList();
                final List<GameLogEntry> daysUndoing = entries.stream().filter(entry -> entry.sourceCard() != null
                        && entry.sourceCard().getId() == roles.daysUndoingId()).toList();
                final List<String> messages = new ArrayList<>();
                for (GameLogEntryType type : List.of(GameLogEntryType.STACK_ADD, GameLogEntryType.STACK_RESOLVE)) {
                    messages.add(tellingTime.stream().filter(entry -> entry.type() == type)
                            .map(GameLogEntry::message).findFirst().orElseThrow());
                    messages.add(fengraf.stream().filter(entry -> entry.type() == type)
                            .map(GameLogEntry::message).findFirst().orElseThrow());
                    messages.add(daysUndoing.stream().filter(entry -> entry.type() == type)
                            .map(GameLogEntry::message).findFirst().orElseThrow());
                }
                Assert.assertTrue(messages.stream().allMatch(message -> message != null && !message.isBlank()),
                        "each actual cast and resolution has nonempty rendered text");
                expectedMessages.set(List.copyOf(messages));
                beforeRendered.set(actualRenderedReportLogText(ui, view));
            });
            final String before = beforeRendered.get();
            Assert.assertEquals(expectedMessages.get().stream()
                            .map(message -> countOccurrences(before, message)).toList(),
                    List.of(1, 1, 1, 1, 1, 1), "each real cast and resolution appeared once before reconnect");
            Assert.assertTrue(privateLogWitnessAbsent(before, roles),
                    "the same private choice witness remains absent before reconnect");

            client.simulateDisconnect(FGameClient.DisconnectMode.OUTBOUND);
            final long reconnectDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(95);
            waitForReconnectState(client, FGameClient.ReconnectState.RECONNECTING, reconnectDeadline);
            waitForReconnectState(client, FGameClient.ReconnectState.CONNECTED, reconnectDeadline);
            final Channel replacementChannel = activeClientChannel(client);
            Assert.assertNotSame(replacementChannel, previousChannel,
                    "reconnect installed a new active transport channel on the existing client");
            Assert.assertTrue(replacementChannel.isActive());
            final long remainingNanos = reconnectDeadline - System.nanoTime();
            Assert.assertTrue(remainingNanos > 0, "the applied reconnect fence remains within its single deadline");
            replacementChannel.eventLoop().submit(() -> { }).get(remainingNanos, TimeUnit.NANOSECONDS);

            final long deadline = reconnectDeadline;
            String after = "";
            boolean rebuiltField = false;
            while (System.nanoTime() < deadline) {
                final AtomicBoolean currentBinding = new AtomicBoolean();
                final AtomicReference<String> renderedSnapshot = new AtomicReference<>();
                FThreads.invokeInEdtAndWait(() -> {
                    final GameView view = ui.getGameView();
                    if (view == null || view != previousView.get() || view.getId() != expectedGameViewId
                            || view.getTracker() != expectedGameTracker || !ui.isCurrentScreen()) return;
                    final List<PlayerView> players = new ArrayList<>(view.getPlayers());
                    if (players.size() != 2) return;
                    final PlayerView local = players.get(1);
                    final Set<PlayerView> localPlayers = ui.getLocalPlayers();
                    if (localPlayers.size() != 1 || !localPlayers.contains(local)
                            || ui.getCurrentPlayer() == null || !ui.getCurrentPlayer().equals(local)) {
                        return;
                    }
                    if (fullSyncMode) {
                        if (local.getTracker() == null || local.getTracker() == expectedGameTracker
                                || local.getTracker().getObj(TrackableTypes.PlayerViewType, local.getId()) != local) {
                            return;
                        }
                    } else {
                        final Tracker localTracker = local.getTracker();
                        if (localTracker == null
                                || localTracker.getObj(TrackableTypes.PlayerViewType, local.getId()) != local) return;
                    }
                    final IGameController controller = ui.getGameController(local);
                    final VField field = ui.getFieldViewFor(local);
                    if (controller instanceof NetGameController && controller != previousController.get()
                            && field != null && field != previousField.get()
                            && field.getLayoutControl().getMatchUI() == ui
                            && field.getParentCell() != null && field.getParentCell().getBody().isShowing()
                            && field.getAvatarArea().isShowing()
                            && ui.getCurrentPlayer() != null && ui.getCurrentPlayer().equals(local)) {
                        renderedSnapshot.set(actualRenderedReportLogText(ui, view));
                        currentBinding.set(true);
                    }
                });
                rebuiltField = currentBinding.get();
                if (rebuiltField && renderedSnapshot.get() != null) {
                    final String renderedNow = renderedSnapshot.get();
                    after = renderedNow;
                    if (expectedMessages.get().stream()
                            .allMatch(message -> countOccurrences(renderedNow, message) == 2)) {
                        break;
                    }
                }
                Thread.sleep(50);
            }
            Assert.assertTrue(rebuiltField,
                    "the same CMatchUI's current assigned-seat field was rebuilt and is showing after openView");
            final String replayedText = after;
            Assert.assertEquals(expectedMessages.get().stream()
                            .map(message -> countOccurrences(replayedText, message)).toList(),
                    List.of(2, 2, 2, 2, 2, 2),
                    "server replay produced a second actual visible occurrence of each public cast and resolution");
            Assert.assertTrue(privateLogWitnessAbsent(replayedText, roles),
                    "the same private choice witness remains absent from the replayed rendered history");
            Assert.assertFalse(containsNameFingerprint(replayedText, unknownNameLength, unknownNameFingerprint),
                    "the unknown current host-hand name remains absent from actual replayed remote text");
            Assert.assertSame(activeClientChannel(client), replacementChannel,
                    "the completed rendered replay belongs to the active replacement channel");
            Assert.assertEquals(client.getReconnectState(), FGameClient.ReconnectState.CONNECTED,
                    "the same remote client remains connected after its rendered replay");
            return "RECONNECT_HISTORY|REMOTE|SAME_GAME=1|SAME_TRACKER=1|NEW_CHANNEL=1|NEW_FIELD=1"
                    + "|REPLAYED_TT=1|REPLAYED_FENGRAF=1|REPLAYED_DAY=1|PRIVATE_ABSENT=1"
                    + "|UNKNOWN_CURRENT_HAND_NAME_ABSENT=1";
        }

        private static Channel activeClientChannel(final FGameClient client) throws Exception {
            final Field field = FGameClient.class.getDeclaredField("channel");
            field.setAccessible(true);
            final Object value = field.get(client);
            if (!(value instanceof Channel channel) || !channel.isActive()) {
                throw new AssertionError("the existing remote client has no active channel");
            }
            return channel;
        }

        private static void waitForReconnectState(final FGameClient client,
                final FGameClient.ReconnectState expected, final long deadline) throws InterruptedException {
            while (System.nanoTime() < deadline) {
                final FGameClient.ReconnectState state = client.getReconnectState();
                if (state == expected) return;
                if (state == FGameClient.ReconnectState.FAILED || state == FGameClient.ReconnectState.SEAT_LOST) {
                    throw new AssertionError("same client reconnect stopped in " + state);
                }
                Thread.sleep(20);
            }
            throw new AssertionError("same client did not reach reconnect state " + expected);
        }

        private static int countOccurrences(final String text, final String needle) {
            int count = 0;
            int offset = 0;
            while ((offset = text.indexOf(needle, offset)) >= 0) {
                count++;
                offset += needle.length();
            }
            return count;
        }

        private static boolean privateLogWitnessAbsent(final String text, final PrivateLogRoles roles) {
            return !text.contains(roles.privateOneName()) && !text.contains(roles.privateTwoName())
                    && !text.contains(roles.privateThreeName());
        }

        private static String driveInverseHostTellingTime(final CMatchUI ui, final TellingTimeSetup setup,
                final int expectedGameViewId, final Tracker expectedGameTracker) throws Exception {
            Assert.assertNotNull(setup, "host retained the inverse Telling Time fixture roles");
            final Game game = setup.game();
            final Player host = setup.host();
            final Player remote = setup.remote();
            Assert.assertSame(setup.tellingTime().getOwner(), remote,
                    "the inverse fixture assigns the actual spell to the remote chooser");
            LAST_INVERSE_HOST_PRIORITY_GATE.set("NOT_SAMPLED");
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(85);
            boolean passedHostPriority = false;
            boolean resolved = false;
            Set<Integer> paidManaSourceIds = null;
            Input lastPassedInput = null;
            Input stableResolutionPriority = null;
            while (System.nanoTime() < deadline) {
                if (!passedHostPriority) {
                    final AtomicReference<Boolean> passed = new AtomicReference<>(false);
                    FThreads.invokeInEdtAndWait(() -> {
                        final GameView view = ui.getGameView();
                        final Input input = setup.controller().getInputProxy().getInput();
                        final AbstractButton ok = openingPriorityOkButton(ui);
                        final Input queued = setup.controller().getInputQueue().getInput();
                        final VPrompt prompt = openingPriorityPrompt(ui);
                        // Retain only scalar gate facts; emit them only in the failure receipt.
                        LAST_INVERSE_HOST_PRIORITY_GATE.set(
                                "VIEW=" + (view != null && view.getId() == expectedGameViewId)
                                + "|TRACKER=" + (view != null && view.getTracker() == expectedGameTracker)
                                + "|CONTROLLER=" + (ui.getGameController(host.getView()) == setup.controller())
                                + "|TURN1=" + (game.getPhaseHandler().getTurn() == 1)
                                + "|MAIN1=" + (game.getPhaseHandler().getPhase() == PhaseType.MAIN1)
                                + "|HOST_PRIORITY=" + (game.getPhaseHandler().getPriorityPlayer() == host)
                                + "|PROXY=" + (input == null ? "NONE" : input.getClass().getSimpleName())
                                + "|QUEUE=" + (queued == null ? "NONE" : queued.getClass().getSimpleName())
                                + "|PRIORITY_INPUT=" + (input instanceof InputPassPriority)
                                + "|OWNER=" + (input != null && input.getOwner() != null
                                        && input.getOwner().equals(host.getView()))
                                + "|QUEUE_MATCH=" + (input == queued)
                                + "|PROMPT_BOUND=" + (prompt != null)
                                + "|OK_SHOWING=" + (prompt != null && prompt.getBtnOK().isShowing())
                                + "|OK_ENABLED=" + (prompt != null && prompt.getBtnOK().isEnabled())
                                + "|OK_LABEL=" + (prompt != null && Localizer.getInstance().getMessage("lblOK")
                                        .equals(prompt.getBtnOK().getText()))
                                + "|OK_READY=" + (ok != null)
                                + "|STACK_EMPTY=" + game.getStack().isEmpty());
                        if (view == null || view.getId() != expectedGameViewId
                                || view.getTracker() != expectedGameTracker || ui.getGameController(host.getView())
                                        != setup.controller()
                                || game.getPhaseHandler().getTurn() != 1
                                || game.getPhaseHandler().getPhase() != PhaseType.MAIN1
                                || game.getPhaseHandler().getPriorityPlayer() != host
                                || !(input instanceof InputPassPriority)
                                || input.getOwner() == null || !input.getOwner().equals(host.getView())
                                || input != setup.controller().getInputQueue().getInput()
                                || ok == null || !game.getStack().isEmpty()) return;
                        ok.doClick(0);
                        passed.set(true);
                    });
                    passedHostPriority = passed.get();
                } else {
                    if (!game.getStack().isEmpty() && paidManaSourceIds == null) {
                        final var ability = game.getStack().peekAbility();
                        if (ability.getHostCard().getId() == setup.tellingTime().getId()
                                && ability.getActivatingPlayer() == remote) {
                            paidManaSourceIds = ability.getPayingMana().stream()
                                    .map(mana -> mana.getSourceCard().getId())
                                    .collect(java.util.stream.Collectors.toSet());
                        }
                    }
                    final Input input = setup.controller().getInputProxy().getInput();
                    if (!game.getStack().isEmpty() && game.getPhaseHandler().getPriorityPlayer() == host
                            && input instanceof InputPassPriority
                            && input == setup.controller().getInputQueue().getInput() && input != lastPassedInput) {
                        final AtomicReference<Boolean> passed = new AtomicReference<>(false);
                        FThreads.invokeInEdtAndWait(() -> {
                            final GameView view = ui.getGameView();
                            final AbstractButton ok = openingPriorityOkButton(ui);
                            if (view != null && view.getId() == expectedGameViewId
                                    && view.getTracker() == expectedGameTracker
                                    && ui.getGameController(host.getView()) == setup.controller()
                                    && setup.controller().getInputProxy().getInput() == input
                                    && setup.controller().getInputQueue().getInput() == input
                                    && game.getPhaseHandler().getPriorityPlayer() == host
                                    && input.getOwner() != null && input.getOwner().equals(host.getView())
                                    && !game.getStack().isEmpty() && ok != null) {
                                ok.doClick(0);
                                passed.set(true);
                            }
                        });
                        if (!passed.get()) throw new AssertionError("host priority changed before actual stack pass");
                        lastPassedInput = input;
                    }
                    final Input currentInput = setup.controller().getInputProxy().getInput();
                    final boolean postResolutionPriority = passedHostPriority
                            && game.getStack().isEmpty()
                            && game.getPhaseHandler().getPriorityPlayer() == host
                            && currentInput instanceof InputPassPriority
                            && currentInput != lastPassedInput
                            && setup.controller().getInputProxy().getInput() == currentInput
                            && setup.controller().getInputQueue().getInput() == currentInput
                            && currentInput.getOwner() != null
                            && currentInput.getOwner().equals(host.getView());
                    if (!postResolutionPriority) {
                        stableResolutionPriority = null;
                        Thread.sleep(20);
                        continue;
                    }
                    if (stableResolutionPriority != currentInput) {
                        stableResolutionPriority = currentInput;
                        Thread.sleep(20);
                        continue;
                    }
                    final Card currentSpell = findPhysicalCard(game, setup.tellingTime().getId());
                    final Card currentChoice = findPhysicalCard(game, setup.privateOneId());
                    if (currentSpell.getZone() == host.getZone(ZoneType.Graveyard)
                            && currentChoice.getZone() == remote.getZone(ZoneType.Hand)
                            && remote.getCardsIn(ZoneType.Library).get(0).getId() == setup.privateTwoId()
                            && remote.getCardsIn(ZoneType.Library)
                                    .get(remote.getCardsIn(ZoneType.Library).size() - 1).getId() == setup.privateThreeId()) {
                        resolved = true;
                        break;
                    }
                }
                Thread.sleep(20);
            }
            Assert.assertTrue(passedHostPriority, "host passed its actual MAIN1 priority through the live controller");
            Assert.assertTrue(resolved, "remote Telling Time resolved all outcomes at fresh host priority");
            final Input resolvedInput = stableResolutionPriority;
            Assert.assertSame(setup.controller().getInputProxy().getInput(), resolvedInput,
                    "the actual fresh host priority input remains installed for final role checks");
            Assert.assertSame(setup.controller().getInputQueue().getInput(), resolvedInput,
                    "the actual fresh host priority input remains queued for final role checks");
            Assert.assertSame(game.getPhaseHandler().getPriorityPlayer(), host,
                    "the active host holds priority after the remote spell resolves");
            Assert.assertTrue(resolvedInput instanceof InputPassPriority && resolvedInput != lastPassedInput,
                    "final role checks use a fresh unconsumed host priority input");
            final Card currentSpell = findPhysicalCard(game, setup.tellingTime().getId());
            final Card currentChoice = findPhysicalCard(game, setup.privateOneId());
            Assert.assertTrue(game.getStack().isEmpty(), "remote Telling Time resolved and left the actual stack empty");
            Assert.assertEquals(game.getPhaseHandler().getTurn(), 1,
                    "the inverse instant case remains on the original turn");
            Assert.assertSame(currentSpell.getOwner(), remote,
                    "the current spell card retains the chooser's actual ownership");
            Assert.assertSame(currentSpell.getZone(), host.getZone(ZoneType.Graveyard),
                    "the current spell card is in the exact shared graveyard");
            Assert.assertSame(currentChoice.getOwner(), remote,
                    "the current selected card retains the chooser's actual ownership");
            Assert.assertSame(currentChoice.getZone(), remote.getZone(ZoneType.Hand),
                    "the current selected card is in the chooser's actual hand");
            Assert.assertEquals(remote.getCardsIn(ZoneType.Library).get(0).getId(), setup.privateTwoId());
            Assert.assertEquals(remote.getCardsIn(ZoneType.Library)
                    .get(remote.getCardsIn(ZoneType.Library).size() - 1).getId(), setup.privateThreeId());
            Assert.assertNotNull(paidManaSourceIds);
            Assert.assertEquals(paidManaSourceIds,
                    setup.manaSources().stream().map(Card::getId).collect(java.util.stream.Collectors.toSet()));
            Assert.assertTrue(setup.manaSources().stream().allMatch(Card::isTapped),
                    "both remote-paid Islands are tapped after actual payment");
            Assert.assertTrue(physicalCardIds(game).equals(setup.originalPhysicalIds()),
                    "inverse spell resolution conserves the actual physical card identity set");
            final List<GameLogEntry> spellEntries = game.getGameLog().getAllEntries().stream()
                    .filter(entry -> entry.sourceCard() != null
                            && entry.sourceCard().getId() == setup.tellingTime().getId()).toList();
            final GameLogEntry castEntry = spellEntries.stream()
                    .filter(entry -> entry.type() == GameLogEntryType.STACK_ADD).findFirst().orElseThrow();
            final GameLogEntry resolveEntry = spellEntries.stream()
                    .filter(entry -> entry.type() == GameLogEntryType.STACK_RESOLVE).findFirst().orElseThrow();
            final String rendered = actualRenderedReportLogText(ui, game.getView());
            Assert.assertTrue(rendered.contains(castEntry.message()) && rendered.contains(resolveEntry.message()));
            Assert.assertFalse(rendered.contains(setup.privateOneName()));
            Assert.assertFalse(rendered.contains(setup.privateTwoName()));
            Assert.assertFalse(rendered.contains(setup.privateThreeName()));
            Assert.assertFalse(rendered.contains(setup.historySentinelName()),
                    "host-hand sentinel is only a no-history control, not a host secrecy claim");
            return "TT_INVERSE_HOST|HOST_PASS=1|PUBLIC_CAST=1|PUBLIC_RESOLVE=1|PRIVATE_ABSENT=1"
                    + "|HAND_SENTINEL_ABSENT=1|CARDS=80";
        }

        private static String driveInverseRemoteTellingTime(final CMatchUI ui, final PrivateLogRoles roles,
                final int expectedGameViewId, final Tracker expectedGameTracker) throws Exception {
            Assert.assertNotNull(roles);
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(85);
            boolean castSent = false;
            int choiceOrdinal = 0;
            final Set<Integer> paidSources = new HashSet<>();
            final Set<Integer> dispatchedManaSources = new HashSet<>();
            final AtomicReference<String> lastManaPaymentPrompt = new AtomicReference<>();
            final AtomicBoolean firstChoiceDispatched = new AtomicBoolean();
            final AtomicBoolean secondChoiceDispatched = new AtomicBoolean();
            final AtomicReference<Integer> castCardId = new AtomicReference<>();
            final AtomicReference<String> stackPassGate = new AtomicReference<>("NOT_ATTEMPTED");
            boolean remoteStackPassed = false;
            boolean resolved = false;
            while (System.nanoTime() < deadline) {
                if (castSent && !remoteStackPassed && passInverseRemoteStackPriorityOnce(ui,
                        expectedGameViewId, expectedGameTracker, stackPassGate, castCardId.get(), "Telling Time")) {
                    remoteStackPassed = true;
                }
                final AtomicReference<Boolean> currentCastSent = new AtomicReference<>(false);
                final AtomicReference<Integer> currentOrdinal = new AtomicReference<>(choiceOrdinal);
                final AtomicReference<Throwable> uiFailure = new AtomicReference<>();
                final boolean castAlreadySent = castSent;
                FThreads.invokeInEdtAndWait(() -> {
                    try {
                        final GameView view = ui.getGameView();
                        final PlayerView local = ui.getCurrentPlayer();
                        final IGameController controller = local == null ? null : ui.getGameController(local);
                        if (view == null || view.getId() != expectedGameViewId
                                || view.getTracker() != expectedGameTracker || local == null
                                || !ui.isCurrentScreen() || controller == null
                                || !(controller instanceof NetGameController)) return;
                        Assert.assertSame(local.getTracker(), view.getTracker());

                        final Map<Integer, CardView> currentManaSources = new java.util.HashMap<>();
                        for (PlayerView player : view.getPlayers()) {
                            for (CardView card : player.getCards(ZoneType.Battlefield)) {
                                if (card != null && local.equals(card.getController())
                                        && "Island".equals(card.getName())
                                        && roles.tellingTimeManaIds().contains(card.getId())
                                        && card.getTracker() == view.getTracker()) {
                                    currentManaSources.put(card.getId(), card);
                                    if (dispatchedManaSources.contains(card.getId()) && card.isTapped()) {
                                        paidSources.add(card.getId());
                                    }
                                }
                            }
                        }

                        if (!castAlreadySent) {
                            if (view.getTurn() != 1 || view.getPhase() != PhaseType.MAIN1
                                    || !local.getHasPriority() || !view.getStack().isEmpty()) return;
                            final CardPanel spellPanel = ui.getHandFor(local).getHandArea().getCardPanels().stream()
                                    .filter(panel -> panel.getCard() != null
                                            && "Telling Time".equals(panel.getCard().getName()))
                                    .findFirst().orElse(null);
                            if (spellPanel == null || !spellPanel.isShowing()
                                    || spellPanel.getCard().getTracker() != view.getTracker()) return;
                            castCardId.set(spellPanel.getCard().getId());
                            controller.selectCard(spellPanel.getCard(), null, null);
                            currentCastSent.set(true);
                            return;
                        }

                        final IVDoc<?> promptDoc = EDocID.REPORT_MESSAGE.getDoc();
                        final boolean livePrompt = promptDoc instanceof VPrompt prompt
                                && prompt.getLayoutControl().getMatchUI() == ui
                                && prompt.getParentCell() != null
                                && prompt.getParentCell().getSelected() == prompt
                                && prompt.getParentCell().getBody().isShowing()
                                && prompt.getBtnOK().isShowing() && prompt.getBtnCancel().isShowing()
                                && ui.getPromptMessage() != null && !ui.getPromptMessage().isBlank();
                        final StackItemView top = view.peekStack();
                        final boolean actualTellingTimeStack = top != null
                                && top.getTracker() == view.getTracker() && top.getSourceCard() != null
                                && top.getSourceCard().getTracker() == view.getTracker()
                                && "Telling Time".equals(top.getSourceCard().getName())
                                && local.equals(top.getActivatingPlayer());
                        final String promptText = ui.getPromptMessage();
                        final Integer castId = castCardId.get();
                        final CardView castCard = castId == null ? null
                                : view.getTracker().getObj(TrackableTypes.CardViewType, castId);
                        // During frozen cost payment the prompt carries the new Stack CardView inline;
                        // the retained tracked view can still describe the spell's prior Hand zone.
                        final boolean actualManaPrompt = livePrompt && castCard != null
                                && castCard.getTracker() == view.getTracker() && castCard.getId() == castId
                                && (castCard.getZone() == ZoneType.Hand || castCard.getZone() == ZoneType.Stack)
                                && local.equals(castCard.getOwner()) && local.equals(castCard.getController())
                                && "Telling Time".equals(castCard.getName())
                                && promptText.contains(Localizer.getInstance().getMessage("lblPayManaCost"))
                                && currentManaSources.size() == 2;
                        if (actualManaPrompt) {
                            final String previousPrompt = lastManaPaymentPrompt.get();
                            if (previousPrompt != null && (paidSources.size() < dispatchedManaSources.size()
                                    || previousPrompt.equals(promptText))) return;
                            final List<CardView> selectableIslands = currentManaSources.values().stream()
                                    .filter(card -> !card.isTapped()).toList();
                            if (!selectableIslands.isEmpty()) {
                                final CardView source = selectableIslands.stream()
                                        .filter(card -> !dispatchedManaSources.contains(card.getId()))
                                        .findFirst().orElse(null);
                                if (source != null) {
                                    final VField field = ui.getFieldViewFor(source.getController());
                                    final CardPanel sourcePanel = field == null ? null
                                            : field.getTabletop().getCardPanel(source.getId());
                                    if (sourcePanel == null || !sourcePanel.isShowing()
                                            || sourcePanel.getCard().getTracker() != view.getTracker()) return;
                                    if (!controller.selectCard(source, null, null)) {
                                        throw new AssertionError("remote Island mana choice was not dispatched");
                                    }
                                    dispatchedManaSources.add(source.getId());
                                    lastManaPaymentPrompt.set(promptText);
                                    return;
                                }
                            }
                        }

                        if (currentOrdinal.get() < 2 && livePrompt && actualTellingTimeStack) {
                            final int ordinal = currentOrdinal.get();
                            final Set<String> expectedNames = ordinal == 0 && !firstChoiceDispatched.get()
                                    ? Set.of(roles.privateOneName(), roles.privateTwoName(), roles.privateThreeName())
                                    : Set.of(roles.privateTwoName(), roles.privateThreeName());
                            final Map<Integer, CardView> selectableViews = new java.util.HashMap<>();
                            for (PlayerView player : view.getPlayers()) {
                                for (CardView card : player.getCards(ZoneType.Library)) {
                                    if (card != null && ui.isSelectable(card)
                                            && card.getTracker() == view.getTracker()) {
                                        selectableViews.put(card.getId(), card);
                                    }
                                }
                            }
                            final Set<String> selectableNames = selectableViews.values().stream()
                                    .map(CardView::getName).collect(java.util.stream.Collectors.toSet());
                            if (selectableNames.equals(expectedNames) && selectableViews.size() == expectedNames.size()
                                    && ui.getSelectionMin() == 1 && ui.getSelectionMax() == 1 && livePrompt) {
                                if (ordinal == 0 && firstChoiceDispatched.get()) {
                                    // The next exact two-card prompt is the applied first selection transition.
                                    currentOrdinal.set(1);
                                }
                                for (CardView offered : selectableViews.values()) {
                                    final CardPanel offeredPanel = FloatingZone.getCardPanel(ui, offered);
                                    if (offeredPanel == null || !offeredPanel.isShowing()
                                            || offeredPanel.getCard().getTracker() != view.getTracker()) return;
                                }
                                final String desiredName = currentOrdinal.get() == 0
                                        ? roles.privateOneName() : roles.privateTwoName();
                                final CardView desired = selectableViews.values().stream()
                                        .filter(card -> desiredName.equals(card.getName())).findFirst().orElse(null);
                                if (ordinal == 0 && !firstChoiceDispatched.get()) {
                                    if (desired == null || !controller.selectCard(desired, null, null)) {
                                        throw new AssertionError("first remote Telling Time choice was not dispatched");
                                    }
                                    firstChoiceDispatched.set(true);
                                    return;
                                }
                                if (currentOrdinal.get() == 1 && !secondChoiceDispatched.get()) {
                                    if (desired == null || !controller.selectCard(desired, null, null)) {
                                        throw new AssertionError("second remote Telling Time choice was not dispatched");
                                    }
                                    secondChoiceDispatched.set(true);
                                }
                            }
                        }
                    } catch (Throwable failure) {
                        uiFailure.set(failure);
                    }
                });
                if (uiFailure.get() != null) {
                    if (uiFailure.get() instanceof Exception exception) throw exception;
                    if (uiFailure.get() instanceof Error error) throw error;
                    throw new AssertionError("remote rendered Telling Time action failed", uiFailure.get());
                }
                if (currentCastSent.get()) castSent = true;
                choiceOrdinal = currentOrdinal.get();
                if (castSent && choiceOrdinal < 2 && automateInverseTellingTimeOrderDialog(ui,
                        expectedGameViewId, expectedGameTracker, roles, choiceOrdinal)) {
                    if (choiceOrdinal == 0) firstChoiceDispatched.set(true);
                    else secondChoiceDispatched.set(true);
                    choiceOrdinal++;
                }

                final AtomicReference<Boolean> nowResolved = new AtomicReference<>(false);
                final int completedChoiceCount = choiceOrdinal;
                final boolean secondChoiceWasDispatched = secondChoiceDispatched.get();
                FThreads.invokeInEdtAndWait(() -> {
                    final GameView view = ui.getGameView();
                    final PlayerView local = ui.getCurrentPlayer();
                    if (view == null || view.getId() != expectedGameViewId
                            || view.getTracker() != expectedGameTracker || local == null) return;
                    final var hand = local.getCards(ZoneType.Hand);
                    final var library = local.getCards(ZoneType.Library);
                    final boolean received = hand.stream().anyMatch(card -> roles.privateOneName().equals(card.getName())
                            && card.getTracker() == view.getTracker());
                    final boolean topCard = !library.isEmpty()
                            && roles.privateTwoName().equals(library.get(0).getName());
                    final boolean bottomCard = !library.isEmpty()
                            && roles.privateThreeName().equals(library.get(library.size() - 1).getName());
                    if (received && topCard && bottomCard && view.getStack().isEmpty()
                            && (completedChoiceCount == 2 || secondChoiceWasDispatched)) {
                        nowResolved.set(true);
                    }
                });
                if (nowResolved.get()) {
                    if (secondChoiceDispatched.get()) choiceOrdinal = 2;
                    resolved = true;
                    break;
                }
                Thread.sleep(25);
            }
            if (!remoteStackPassed) {
                RESULT.println("TT_INVERSE_PROGRESS|REMOTE|CAST_DISPATCHED=" + castSent + "|PASS_GATE="
                        + stackPassGate.get() + "|MANA_APPLIED=" + paidSources.size()
                        + "|MANA_DISPATCHED=" + dispatchedManaSources.size()
                        + "|CHOICES=" + choiceOrdinal + "|CHOICE_1_SENT=" + firstChoiceDispatched.get()
                        + "|CHOICE_2_SENT=" + secondChoiceDispatched.get());
                RESULT.flush();
            }
            Assert.assertTrue(castSent, "remote cast Telling Time from its visible actual hand at live priority");
            Assert.assertTrue(remoteStackPassed,
                    "remote passed the actual Telling Time stack priority through its live controller");
            Assert.assertTrue(resolved, "remote applied the actual recipient hand and both library outcomes");
            Assert.assertEquals(choiceOrdinal, 2, "remote made both actual rendered Telling Time choices");
            final GameView expectedView = ui.getGameView();
            final String logText = actualRenderedReportLogText(ui, expectedView);
            final List<GameLogEntry> entries = expectedView.getGameLog().getAllEntries().stream()
                    .filter(entry -> entry.sourceCard() != null && "Telling Time".equals(entry.sourceCard().getName()))
                    .toList();
            final GameLogEntry castEntry = entries.stream()
                    .filter(entry -> entry.type() == GameLogEntryType.STACK_ADD).findFirst().orElseThrow();
            final GameLogEntry resolveEntry = entries.stream()
                    .filter(entry -> entry.type() == GameLogEntryType.STACK_RESOLVE).findFirst().orElseThrow();
            Assert.assertTrue(logText.contains(castEntry.message()) && logText.contains(resolveEntry.message()));
            Assert.assertFalse(logText.contains(roles.privateOneName()));
            Assert.assertFalse(logText.contains(roles.privateTwoName()));
            Assert.assertFalse(logText.contains(roles.privateThreeName()));
            final PlayerView local = ui.getCurrentPlayer();
            Assert.assertSame(local.getTracker(), expectedView.getTracker());
            Assert.assertTrue(local.getCards(ZoneType.Hand).stream()
                    .anyMatch(card -> roles.privateOneName().equals(card.getName())));
            return "TT_INVERSE_REMOTE|REMOTE_CAST=1|MANA_SOURCES=" + paidSources.size()
                    + "|CHOICES=" + choiceOrdinal + "|RECIPIENT=1|TRACKER=1|PUBLIC_CAST=1|PUBLIC_RESOLVE=1";
        }

        private static String driveInverseHostFengraf(final CMatchUI ui, final TellingTimeSetup setup,
                final int expectedGameViewId, final Tracker expectedGameTracker) throws Exception {
            Assert.assertNotNull(setup);
            Assert.assertEquals(setup.fengrafManaSources().size(), 3);
            Assert.assertTrue(FModel.getPreferences().getPrefBoolean(FPref.UI_SELECT_FROM_CARD_DISPLAYS),
                    "fresh desktop profiles use the actual rendered hand-reveal path");
            final Game game = setup.game();
            final Player host = setup.host();
            final Player remote = setup.remote();
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(65);
            boolean passedToRemote = false;
            boolean passedFengrafStack = false;
            boolean acknowledgedReveal = false;
            boolean resolved = false;
            Set<Integer> paidManaSourceIds = null;
            Input lastPassedInput = null;
            Input stackPassInput = null;
            InputConfirm revealInput = null;
            CardPanel revealedPanel = null;
            Window revealWindow = null;
            Input stableResolutionPriority = null;

            while (System.nanoTime() < deadline) {
                final Input currentInput = setup.controller().getInputProxy().getInput();
                if (!passedToRemote && game.getPhaseHandler().getPriorityPlayer() == host
                        && game.getStack().isEmpty() && currentInput instanceof InputPassPriority
                        && currentInput == setup.controller().getInputQueue().getInput()
                        && currentInput.getOwner() != null && currentInput.getOwner().equals(host.getView())) {
                    final AtomicReference<Boolean> passed = new AtomicReference<>(false);
                    FThreads.invokeInEdtAndWait(() -> {
                        final GameView view = ui.getGameView();
                        final AbstractButton ok = openingPriorityOkButton(ui);
                        if (view == null || view.getId() != expectedGameViewId
                                || view.getTracker() != expectedGameTracker
                                || ui.getGameController(host.getView()) != setup.controller()
                                || setup.controller().getInputProxy().getInput() != currentInput
                                || setup.controller().getInputQueue().getInput() != currentInput
                                || game.getPhaseHandler().getPriorityPlayer() != host
                                || !game.getStack().isEmpty() || ok == null) return;
                        ok.doClick(0);
                        passed.set(true);
                    });
                    if (!passed.get()) throw new AssertionError("host lost its actual empty-stack priority before Fengraf");
                    passedToRemote = true;
                    lastPassedInput = currentInput;
                    stableResolutionPriority = null;
                    Thread.sleep(20);
                    continue;
                }

                if (passedToRemote && !passedFengrafStack && !game.getStack().isEmpty()
                        && game.getPhaseHandler().getPriorityPlayer() == host) {
                    final var ability = game.getStack().peekAbility();
                    if (ability.getHostCard().getId() == setup.fengrafId()
                            && ability.getActivatingPlayer() == remote) {
                        final Input input = setup.controller().getInputProxy().getInput();
                        if (input instanceof InputPassPriority && input != lastPassedInput
                                && input == setup.controller().getInputQueue().getInput()
                                && input.getOwner() != null && input.getOwner().equals(host.getView())) {
                            if (!ability.isManaAbility()) {
                                final Set<Integer> sourceIds = ability.getPayingMana().stream()
                                        .map(mana -> mana.getSourceCard().getId())
                                        .collect(java.util.stream.Collectors.toSet());
                                paidManaSourceIds = sourceIds;
                            }
                            final AtomicReference<Boolean> passed = new AtomicReference<>(false);
                            FThreads.invokeInEdtAndWait(() -> {
                                final GameView view = ui.getGameView();
                                final AbstractButton ok = openingPriorityOkButton(ui);
                                if (view != null && view.getId() == expectedGameViewId
                                        && view.getTracker() == expectedGameTracker
                                        && ui.getGameController(host.getView()) == setup.controller()
                                        && setup.controller().getInputProxy().getInput() == input
                                        && setup.controller().getInputQueue().getInput() == input
                                        && game.getPhaseHandler().getPriorityPlayer() == host
                                        && game.getStack().peekAbility().getHostCard().getId() == setup.fengrafId()
                                        && ok != null) {
                                    ok.doClick(0);
                                    passed.set(true);
                                }
                            });
                            if (!passed.get()) throw new AssertionError("host lost the actual Fengraf stack priority");
                            passedFengrafStack = true;
                            stackPassInput = input;
                            lastPassedInput = input;
                            stableResolutionPriority = null;
                            Thread.sleep(20);
                            continue;
                        }
                    }
                }

                if (passedFengrafStack && !acknowledgedReveal && currentInput instanceof InputConfirm confirm
                        && currentInput == setup.controller().getInputQueue().getInput()
                        && currentInput.getOwner() != null && currentInput.getOwner().equals(host.getView())) {
                    final Card physicalRevealedCard = findPhysicalCard(game, setup.fengrafCreatureId());
                    Assert.assertSame(physicalRevealedCard.getOwner(), remote,
                            "the current physical Fengraf result belongs to the remote recipient");
                    Assert.assertSame(physicalRevealedCard.getZone(), remote.getZone(ZoneType.Hand),
                            "the current physical Fengraf result is in the remote recipient's hand");
                    // A same-ID CardView can remain registered from before CardCopyService's move to the recipient hand.
                    final CardView physicalRevealedCardView = physicalRevealedCard.getView();
                    Assert.assertNotNull(physicalRevealedCardView);
                    Assert.assertSame(physicalRevealedCardView.getTracker(), expectedGameTracker);
                    Assert.assertEquals(physicalRevealedCardView.getId(), setup.fengrafCreatureId());
                    Assert.assertEquals(physicalRevealedCardView.getOwner(), remote.getView());
                    Assert.assertSame(physicalRevealedCardView.getZone(), ZoneType.Hand);
                    final AtomicReference<CardPanel> panelResult = new AtomicReference<>();
                    final AtomicReference<Window> windowResult = new AtomicReference<>();
                    FThreads.invokeInEdtAndWait(() -> {
                        final GameView view = ui.getGameView();
                        final CardView revealedCard = physicalRevealedCardView;
                        final CardPanel panel = FloatingZone.getCardPanel(ui, revealedCard);
                        final IVDoc<?> promptDoc = EDocID.REPORT_MESSAGE.getDoc();
                        final VPrompt prompt = promptDoc instanceof VPrompt value ? value : null;
                        final AbstractButton ok = prompt == null ? null : prompt.getBtnOK();
                        if (view == null || view.getId() != expectedGameViewId
                                || view.getTracker() != expectedGameTracker
                                || ui.getGameController(host.getView()) != setup.controller()
                                || setup.controller().getInputProxy().getInput() != confirm
                                || setup.controller().getInputQueue().getInput() != confirm
                                || prompt == null || prompt.getLayoutControl().getMatchUI() != ui
                                || prompt.getParentCell() == null || prompt.getParentCell().getSelected() != prompt
                                || !prompt.getParentCell().getBody().isShowing()
                                || ok == null || !ok.isShowing() || !ok.isEnabled()
                                || !Localizer.getInstance().getMessage("lblOK").equals(ok.getText())
                                || revealedCard.getTracker() != expectedGameTracker
                                || revealedCard.getId() != setup.fengrafCreatureId()
                                || !remote.getView().equals(revealedCard.getOwner())
                                || revealedCard.getZone() != ZoneType.Hand
                                || panel == null || !panel.isShowing() || !panel.isDisplayEnabled()
                                || panel.getCard() == null || panel.getCard().getTracker() != expectedGameTracker
                                || panel.getCard().getId() != setup.fengrafCreatureId()
                                || !remote.getView().equals(panel.getCard().getOwner())
                                || panel.getCard().getZone() != ZoneType.Hand
                                || !"Dandân".equals(panel.getCard().getName())) return;
                        final Window window = javax.swing.SwingUtilities.getWindowAncestor(panel);
                        if (window == null || !window.isShowing()) return;
                        ok.doClick(0);
                        panelResult.set(panel);
                        windowResult.set(window);
                    });
                    if (panelResult.get() != null) {
                        revealInput = confirm;
                        revealedPanel = panelResult.get();
                        revealWindow = windowResult.get();
                        acknowledgedReveal = true;
                    }
                    Thread.sleep(20);
                    continue;
                }

                if (acknowledgedReveal && revealedPanel != null && revealWindow != null) {
                    final boolean[] revealHidden = new boolean[1];
                    final CardPanel panelToHide = revealedPanel;
                    final Window windowToHide = revealWindow;
                    FThreads.invokeInEdtAndWait(() -> revealHidden[0] = !panelToHide.isShowing()
                            && !windowToHide.isShowing());
                    final boolean revealInputConsumed = setup.controller().getInputProxy().getInput() != revealInput
                            && setup.controller().getInputQueue().getInput() != revealInput;
                    if (!revealHidden[0] || !revealInputConsumed) {
                        Thread.sleep(20);
                        continue;
                    }
                }

                final Input postResolutionInput = setup.controller().getInputProxy().getInput();
                final boolean postResolutionPriority = passedFengrafStack && acknowledgedReveal
                        && game.getStack().isEmpty() && game.getPhaseHandler().getPriorityPlayer() == host
                        && postResolutionInput instanceof InputPassPriority && postResolutionInput != lastPassedInput
                        && setup.controller().getInputQueue().getInput() == postResolutionInput
                        && postResolutionInput.getOwner() != null
                        && postResolutionInput.getOwner().equals(host.getView());
                if (!postResolutionPriority) {
                    stableResolutionPriority = null;
                    Thread.sleep(20);
                    continue;
                }
                if (stableResolutionPriority != postResolutionInput) {
                    stableResolutionPriority = postResolutionInput;
                    Thread.sleep(20);
                    continue;
                }
                final Card currentFengraf = findPhysicalCard(game, setup.fengrafId());
                final Card currentCreature = findPhysicalCard(game, setup.fengrafCreatureId());
                if (currentFengraf.getZone() == remote.getZone(ZoneType.Graveyard)
                        && currentCreature.getOwner() == remote
                        && currentCreature.getZone() == remote.getZone(ZoneType.Hand)
                        && game.getStack().isEmpty()) {
                    resolved = true;
                    break;
                }
                Thread.sleep(20);
            }

            Assert.assertTrue(passedToRemote, "host passed its actual empty-stack priority to the remote Fengraf actor");
            Assert.assertTrue(passedFengrafStack, "host passed the actual nonmana Fengraf ability stack item");
            Assert.assertTrue(acknowledgedReveal, "host acknowledged the actual Fengraf hand-reveal prompt");
            Assert.assertTrue(resolved, "host observed the resolved public Fengraf result at fresh priority");
            Assert.assertNotNull(paidManaSourceIds);
            Assert.assertEquals(paidManaSourceIds,
                    setup.fengrafManaSources().stream().map(Card::getId).collect(java.util.stream.Collectors.toSet()));
            Assert.assertTrue(setup.fengrafManaSources().stream().allMatch(Card::isTapped));
            final Card currentFengraf = findPhysicalCard(game, setup.fengrafId());
            final Card currentCreature = findPhysicalCard(game, setup.fengrafCreatureId());
            Assert.assertSame(currentFengraf.getZone(), remote.getZone(ZoneType.Graveyard),
                    "the actual ability cost sacrificed the remote-owned Fengraf");
            Assert.assertSame(currentCreature.getOwner(), remote,
                    "the actual shared-graveyard acquisition gives its recipient ownership");
            Assert.assertSame(currentCreature.getZone(), remote.getZone(ZoneType.Hand));
            Assert.assertTrue(game.getStack().isEmpty());
            Assert.assertTrue(physicalCardIds(game).equals(setup.originalPhysicalIds()));
            final Input resultInput = setup.controller().getInputProxy().getInput();
            final CardPanel finalRevealPanel = revealedPanel;
            final Window finalRevealWindow = revealWindow;
            Assert.assertSame(setup.controller().getInputQueue().getInput(), resultInput);
            Assert.assertTrue(resultInput instanceof InputPassPriority && resultInput != stackPassInput);
            final List<GameLogEntry> fengrafEntries = game.getGameLog().getAllEntries().stream()
                    .filter(entry -> entry.sourceCard() != null && entry.sourceCard().getId() == setup.fengrafId())
                    .toList();
            final GameLogEntry castEntry = fengrafEntries.stream()
                    .filter(entry -> entry.type() == GameLogEntryType.STACK_ADD).findFirst().orElseThrow();
            final GameLogEntry resolveEntry = fengrafEntries.stream()
                    .filter(entry -> entry.type() == GameLogEntryType.STACK_RESOLVE).findFirst().orElseThrow();
            final String renderedLog = actualRenderedReportLogText(ui, game.getView());
            Assert.assertTrue(renderedLog.contains(castEntry.message()) && renderedLog.contains(resolveEntry.message()));
            final GameView currentView = ui.getGameView();
            FThreads.invokeInEdtAndWait(() -> {
                Assert.assertSame(ui.getGameView(), currentView);
                Assert.assertSame(currentView.getTracker(), expectedGameTracker);
                Assert.assertNull(ui.getHandFor(remote.getView()),
                        "host has no remote private hand view after reveal dismissal");
                Assert.assertFalse(finalRevealPanel.isShowing(), "host reveal CardPanel is hidden after its real OK");
                Assert.assertFalse(finalRevealWindow.isShowing(), "host reveal FloatingZone window is hidden after its real OK");
            });
            return "FENGRAF_HOST|HOST_PASS=1|PUBLIC_CAST=1|PUBLIC_RESOLVE=1|REMOTE_REVEAL_ACK=1"
                    + "|PANEL_HIDDEN=1|PRIVATE_HAND_ABSENT=1|CARDS=80";
        }

        private static String driveInverseRemoteFengraf(final CMatchUI ui, final PrivateLogRoles roles,
                final int expectedGameViewId, final Tracker expectedGameTracker) throws Exception {
            Assert.assertNotNull(roles);
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(65);
            final Set<Integer> tappedSources = new HashSet<>();
            final AtomicReference<String> stackPassGate = new AtomicReference<>("NOT_ATTEMPTED");
            int appliedManaSources = 0;
            boolean fengrafClickSent = false;
            boolean abilityDispatched = false;
            boolean sacrificeConfirmationSent = false;
            boolean stackPassed = false;
            boolean resolved = false;
            while (System.nanoTime() < deadline) {
                if (abilityDispatched && !stackPassed && passInverseRemoteStackPriorityOnce(ui,
                        expectedGameViewId, expectedGameTracker, stackPassGate,
                        roles.fengrafId(), "Haunted Fengraf")) {
                    stackPassed = true;
                }
                final AtomicReference<Boolean> abilitySent = new AtomicReference<>(false);
                final AtomicReference<Window> selectedAbilityDialog = new AtomicReference<>();
                final AtomicReference<Boolean> currentClickSent = new AtomicReference<>(false);
                final AtomicReference<Boolean> manaSent = new AtomicReference<>(false);
                final AtomicReference<Boolean> sacrificeConfirmSent = new AtomicReference<>(false);
                final AtomicReference<Integer> selectedManaId = new AtomicReference<>();
                final AtomicReference<Throwable> uiFailure = new AtomicReference<>();
                final boolean alreadyDispatched = abilityDispatched;
                final boolean alreadyClicked = fengrafClickSent;
                final boolean sacrificeConfirmationAlreadySent = sacrificeConfirmationSent;
                final int appliedManaSourcesBeforePrompt = appliedManaSources;
                FThreads.invokeInEdtAndWait(() -> {
                        try {
                            final GameView view = ui.getGameView();
                            final PlayerView local = ui.getCurrentPlayer();
                            final IGameController controller = local == null ? null : ui.getGameController(local);
                            if (view == null || view.getId() != expectedGameViewId
                                    || view.getTracker() != expectedGameTracker || local == null
                                    || !ui.isCurrentScreen() || !(controller instanceof NetGameController)) return;
                            Assert.assertSame(local.getTracker(), expectedGameTracker);

                            if (!alreadyDispatched && !alreadyClicked) {
                                if (view.getTurn() != 1 || view.getPhase() != PhaseType.MAIN1
                                        || !local.getHasPriority() || !view.getStack().isEmpty()) return;
                                final CardView source = view.getTracker().getObj(
                                        TrackableTypes.CardViewType, roles.fengrafId());
                                final VField field = source == null ? null : ui.getFieldViewFor(source.getController());
                                final CardPanel panel = field == null ? null
                                        : field.getTabletop().getCardPanel(roles.fengrafId());
                                if (source == null || source.getTracker() != expectedGameTracker
                                        || !"Haunted Fengraf".equals(source.getName())
                                        || !local.equals(source.getOwner()) || !local.equals(source.getController())
                                        || source.getZone() != ZoneType.Battlefield || panel == null
                                        || !panel.isShowing() || !panel.isDisplayEnabled()
                                        || panel.getCard() == null || panel.getCard().getId() != roles.fengrafId()
                                        || panel.getCard().getTracker() != expectedGameTracker) return;
                                final var area = field.getTabletop();
                                final Rectangle visible = new Rectangle(panel.getCardX(), panel.getCardY(),
                                        panel.getCardWidth(), panel.getCardHeight()).intersection(area.getVisibleRect());
                                Point click = null;
                                for (int x = visible.x; !visible.isEmpty() && x < visible.x + visible.width; x++) {
                                    for (int y = visible.y; y < visible.y + visible.height; y++) {
                                        if (area.getCardPanel(x, y) == panel) {
                                            click = new Point(x, y);
                                            break;
                                        }
                                    }
                                    if (click != null) break;
                                }
                                if (click == null) return;
                                final long now = System.currentTimeMillis();
                                area.dispatchEvent(new MouseEvent(area, MouseEvent.MOUSE_PRESSED, now,
                                        InputEvent.BUTTON1_DOWN_MASK, click.x, click.y, 1, false,
                                        MouseEvent.BUTTON1));
                                area.dispatchEvent(new MouseEvent(area, MouseEvent.MOUSE_RELEASED, now + 1,
                                        0, click.x, click.y, 1, false, MouseEvent.BUTTON1));
                                currentClickSent.set(true);
                                return;
                            }

                            if (!alreadyDispatched) {
                                if (!local.getHasPriority() || !view.getStack().isEmpty()) return;
                                final String abilityPrompt = Localizer.getInstance()
                                        .getMessage("lblChooseAbilityToPlay");
                                final List<FOptionPane> abilityDialogs = java.util.Arrays.stream(Window.getWindows())
                                        .filter(FOptionPane.class::isInstance).map(FOptionPane.class::cast)
                                        .filter(dialog -> dialog.isShowing()
                                                && abilityPrompt.equals(dialog.getTitle())).toList();
                                if (abilityDialogs.isEmpty()) return;
                                if (abilityDialogs.size() != 1) {
                                    throw new AssertionError("one actual Fengraf ability chooser is showing");
                                }
                                final JDialog dialog = abilityDialogs.get(0);
                                final List<JList<?>> lists = new ArrayList<>();
                                final List<AbstractButton> buttons = new ArrayList<>();
                                collectChoiceListsAndButtons(dialog, lists, buttons);
                                if (lists.size() != 1 || !lists.get(0).isShowing()) {
                                    throw new AssertionError("actual ability chooser has one rendered choice list");
                                }
                                final JList<?> choices = lists.get(0);
                                final CardView menuSource = view.getTracker().getObj(
                                        TrackableTypes.CardViewType, roles.fengrafId());
                                final VField menuField = menuSource == null ? null
                                        : ui.getFieldViewFor(menuSource.getController());
                                final var menuArea = menuField == null ? null : menuField.getTabletop();
                                final CardPanel menuPanel = menuArea == null ? null
                                        : menuArea.getCardPanel(roles.fengrafId());
                                if (menuSource == null || menuSource.getTracker() != expectedGameTracker
                                        || !"Haunted Fengraf".equals(menuSource.getName())
                                        || !local.equals(menuSource.getOwner()) || !local.equals(menuSource.getController())
                                        || menuSource.getZone() != ZoneType.Battlefield || menuPanel == null
                                        || !menuPanel.isShowing() || !menuPanel.isDisplayEnabled()
                                        || menuPanel.getCard() == null
                                        || menuPanel.getCard().getTracker() != expectedGameTracker
                                        || menuPanel.getCard().getId() != roles.fengrafId()) return;
                                final List<Integer> abilityChoices = new ArrayList<>();
                                for (int index = 0; index < choices.getModel().getSize(); index++) {
                                    final Object row = choices.getModel().getElementAt(index);
                                    if (!(row instanceof SpellAbilityView ability)) {
                                        throw new AssertionError("actual ability chooser exposes spell ability rows");
                                    }
                                    final CardView abilitySource = ability.getHostCard();
                                    final boolean sourceIdentity = abilitySource == menuSource;
                                    final boolean sourceTrackerCurrent = abilitySource != null
                                            && abilitySource.getTracker() == expectedGameTracker;
                                    final boolean sourceTrackerAllowed = abilitySource != null
                                            && (abilitySource.getTracker() == null || sourceTrackerCurrent);
                                    final boolean sourceIdMatches = abilitySource != null
                                            && abilitySource.getId() == roles.fengrafId();
                                    final boolean sourceNameMatches = abilitySource != null
                                            && "Haunted Fengraf".equals(abilitySource.getName());
                                    final boolean sourceOwnerMatches = abilitySource != null
                                            && abilitySource.getOwner() != null
                                            && abilitySource.getOwner().getId() == menuSource.getOwner().getId();
                                    final boolean sourceControllerMatches = abilitySource != null
                                            && abilitySource.getController() != null
                                            && abilitySource.getController().getId() == menuSource.getController().getId();
                                    final boolean sourceIsOnBattlefield = abilitySource != null
                                            && abilitySource.getZone() == ZoneType.Battlefield;
                                    final boolean actualCurrentRow = fullSyncMode
                                            ? sourceTrackerAllowed && sourceIdMatches && sourceNameMatches
                                                    && sourceOwnerMatches && sourceControllerMatches
                                                    && sourceIsOnBattlefield
                                            : sourceIdentity && sourceTrackerCurrent && sourceIdMatches
                                                    && sourceNameMatches;
                                    if (!actualCurrentRow) {
                                        throw new AssertionError("actual ability row is bound to current Fengraf");
                                    }
                                    final String description = ability.getDescription().toLowerCase(
                                            java.util.Locale.ROOT);
                                    if (ability.canPlay() && description.contains("graveyard")
                                            && description.contains("hand") && description.contains("sacrifice")) {
                                        abilityChoices.add(index);
                                    }
                                }
                                if (abilityChoices.size() != 1) {
                                    throw new AssertionError("Fengraf chooser has one playable graveyard-to-hand ability");
                                }
                                final int abilityIndex = abilityChoices.get(0);
                                final Object expectedAbility = choices.getModel().getElementAt(abilityIndex);
                                final CardView expectedAbilitySource = ((SpellAbilityView) expectedAbility).getHostCard();
                                final Rectangle selectedBounds = choices.getCellBounds(abilityIndex, abilityIndex);
                                final Rectangle visibleCell = selectedBounds == null ? new Rectangle()
                                        : selectedBounds.intersection(choices.getVisibleRect());
                                if (visibleCell.isEmpty()) return;
                                final Point choicePoint = new Point(visibleCell.x + visibleCell.width / 2,
                                        visibleCell.y + visibleCell.height / 2);
                                if (choices.locationToIndex(choicePoint) != abilityIndex
                                        || !selectedBounds.contains(choicePoint)
                                        || !choices.getVisibleRect().contains(choicePoint)) return;
                                final long now = System.currentTimeMillis();
                                choices.dispatchEvent(new MouseEvent(choices, MouseEvent.MOUSE_PRESSED, now,
                                        InputEvent.BUTTON1_DOWN_MASK, choicePoint.x, choicePoint.y, 1, false,
                                        MouseEvent.BUTTON1));
                                choices.dispatchEvent(new MouseEvent(choices, MouseEvent.MOUSE_RELEASED, now + 1,
                                        0, choicePoint.x, choicePoint.y, 1, false, MouseEvent.BUTTON1));
                                if (choices.getSelectedIndex() != abilityIndex
                                        || choices.getSelectedValue() != expectedAbility
                                        || !(choices.getSelectedValue() instanceof SpellAbilityView selectedAbility)
                                        || selectedAbility.getHostCard() != expectedAbilitySource
                                        || !selectedAbility.canPlay() || !dialog.isShowing()
                                        || ui.getGameView() != view || ui.getCurrentPlayer() != local
                                        || ui.getGameController(local) != controller
                                        || !view.getStack().isEmpty() || !local.getHasPriority()) return;
                                final String okLabel = Localizer.getInstance().getMessage("lblOK");
                                final List<AbstractButton> confirmButtons = buttons.stream()
                                        .filter(button -> okLabel.equals(button.getText())).toList();
                                if (confirmButtons.size() != 1) {
                                    throw new AssertionError("actual ability chooser has one OK action");
                                }
                                final AbstractButton confirm = confirmButtons.get(0);
                                if (!confirm.isShowing() || !confirm.isEnabled()) return;
                                final Rectangle visibleConfirm = confirm.getVisibleRect();
                                if (visibleConfirm.isEmpty()) return;
                                final Point confirmPoint = new Point(visibleConfirm.x + visibleConfirm.width / 2,
                                        visibleConfirm.y + visibleConfirm.height / 2);
                                if (!visibleConfirm.contains(confirmPoint)) return;
                                confirm.dispatchEvent(new MouseEvent(confirm, MouseEvent.MOUSE_PRESSED,
                                        System.currentTimeMillis(), InputEvent.BUTTON1_DOWN_MASK, confirmPoint.x,
                                        confirmPoint.y, 1, false, MouseEvent.BUTTON1));
                                confirm.dispatchEvent(new MouseEvent(confirm, MouseEvent.MOUSE_RELEASED,
                                        System.currentTimeMillis() + 1, 0, confirmPoint.x, confirmPoint.y, 1, false,
                                        MouseEvent.BUTTON1));
                                selectedAbilityDialog.set(dialog);
                                abilitySent.set(true);
                                return;
                            }

                            final IVDoc<?> promptDoc = EDocID.REPORT_MESSAGE.getDoc();
                            if (!local.getHasPriority()) return;
                            if (!(promptDoc instanceof VPrompt prompt)) return;
                            if (prompt.getLayoutControl().getMatchUI() != ui) return;
                            final DragCell promptCell = prompt.getParentCell();
                            if (promptCell == null || promptCell.getSelected() != prompt) return;
                            if (!promptCell.getBody().isShowing()) return;
                            if (!prompt.getBtnOK().isShowing()) return;
                            final String promptMessage = ui.getPromptMessage();
                            final CardView liveSource = view.getTracker().getObj(
                                    TrackableTypes.CardViewType, roles.fengrafId());
                            final String sacrificeQuestion = liveSource == null ? null
                                    : Localizer.getInstance().getMessage("lblSacrificeCardConfirm",
                                            forge.util.CardTranslation.getTranslatedName(liveSource.getName()));
                            if (sacrificeQuestion != null && sacrificeQuestion.equals(promptMessage)) {
                                if (sacrificeConfirmationAlreadySent) return;
                                boolean allManaSourcesApplied = roles.fengrafManaIds().size() == 3
                                        && appliedManaSourcesBeforePrompt == 3 && tappedSources.size() == 3;
                                for (Integer manaId : roles.fengrafManaIds()) {
                                    final CardView manaSource = view.getTracker().getObj(
                                            TrackableTypes.CardViewType, manaId);
                                    if (manaSource == null || manaSource.getTracker() != view.getTracker()
                                            || !local.equals(manaSource.getOwner())
                                            || !local.equals(manaSource.getController())
                                            || manaSource.getZone() != ZoneType.Battlefield || !manaSource.isTapped()) {
                                        allManaSourcesApplied = false;
                                    }
                                }
                                if (!allManaSourcesApplied
                                        || !view.getStack().isEmpty() || liveSource.getTracker() != view.getTracker()
                                        || !local.equals(liveSource.getOwner())
                                        || !local.equals(liveSource.getController())
                                        || !"Haunted Fengraf".equals(liveSource.getName())
                                        || liveSource.getZone() != ZoneType.Battlefield || !liveSource.isTapped()) {
                                    return;
                                }
                                final String yesLabel = Localizer.getInstance().getMessage("lblYes");
                                final String noLabel = Localizer.getInstance().getMessage("lblNo");
                                final var yesButton = prompt.getBtnOK();
                                final var noButton = prompt.getBtnCancel();
                                if (!yesLabel.equals(yesButton.getText()) || !noLabel.equals(noButton.getText())
                                        || !yesButton.isEnabled() || !noButton.isEnabled()
                                        || !yesButton.isShowing() || !noButton.isShowing()) {
                                    return;
                                }
                                final Rectangle visibleYes = yesButton.getVisibleRect();
                                if (visibleYes.isEmpty()) return;
                                final Point yesPoint = new Point(visibleYes.x + visibleYes.width / 2,
                                        visibleYes.y + visibleYes.height / 2);
                                final long now = System.currentTimeMillis();
                                yesButton.dispatchEvent(new MouseEvent(yesButton, MouseEvent.MOUSE_PRESSED, now,
                                        InputEvent.BUTTON1_DOWN_MASK, yesPoint.x, yesPoint.y, 1, false,
                                        MouseEvent.BUTTON1));
                                yesButton.dispatchEvent(new MouseEvent(yesButton, MouseEvent.MOUSE_RELEASED, now + 1,
                                        0, yesPoint.x, yesPoint.y, 1, false, MouseEvent.BUTTON1));
                                sacrificeConfirmSent.set(true);
                                return;
                            }
                            if (promptMessage == null || !promptMessage.contains(Localizer.getInstance()
                                    .getMessage("lblPayManaCost"))) return;
                            if (liveSource == null || liveSource.getTracker() != view.getTracker()
                                    || !local.equals(liveSource.getOwner()) || !local.equals(liveSource.getController())
                                    || !"Haunted Fengraf".equals(liveSource.getName())
                                    || liveSource.getZone() != ZoneType.Battlefield) return;
                            for (CardView card : local.getCards(ZoneType.Battlefield)) {
                                if (!roles.fengrafManaIds().contains(card.getId()) || card.isTapped()
                                        || tappedSources.contains(card.getId())
                                        || card.getTracker() != view.getTracker()
                                        || !local.equals(card.getOwner()) || !local.equals(card.getController())) continue;
                                final VField ownField = ui.getFieldViewFor(local);
                                final CardPanel manaPanel = ownField == null ? null
                                        : ownField.getTabletop().getCardPanel(card.getId());
                                if (manaPanel == null || !manaPanel.isShowing() || !manaPanel.isDisplayEnabled()
                                        || manaPanel.getCard() == null
                                        || manaPanel.getCard().getTracker() != view.getTracker()) continue;
                                if (!controller.selectCard(card, null, null)) {
                                    throw new AssertionError("owned Fengraf mana source was not dispatched");
                                }
                                tappedSources.add(card.getId());
                                selectedManaId.set(card.getId());
                                manaSent.set(true);
                                return;
                            }
                        } catch (Throwable failure) {
                            uiFailure.set(failure);
                        }
                    });
                if (uiFailure.get() != null) {
                    if (uiFailure.get() instanceof Exception exception) throw exception;
                    if (uiFailure.get() instanceof Error error) throw error;
                    throw new AssertionError("rendered Fengraf action failed", uiFailure.get());
                }
                if (abilitySent.get()) {
                    waitForTellingTimeDialogClose(selectedAbilityDialog.get());
                }
                abilityDispatched |= abilitySent.get();
                fengrafClickSent |= currentClickSent.get();
                sacrificeConfirmationSent |= sacrificeConfirmSent.get();
                if (manaSent.get()) {
                    final long tapDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(4);
                    boolean applied = false;
                    while (System.nanoTime() < tapDeadline && !applied) {
                        final AtomicReference<Boolean> tapped = new AtomicReference<>(false);
                        FThreads.invokeInEdtAndWait(() -> {
                            final GameView view = ui.getGameView();
                            if (view == null || view.getId() != expectedGameViewId
                                    || view.getTracker() != expectedGameTracker) return;
                            final Integer sourceId = selectedManaId.get();
                            final CardView source = sourceId == null ? null : view.getTracker().getObj(
                                    TrackableTypes.CardViewType, sourceId);
                            if (source != null && source.isTapped()) tapped.set(true);
                        });
                        applied = tapped.get();
                        if (!applied) Thread.sleep(15);
                    }
                    Assert.assertTrue(applied, "the actual client view applied its dispatched Fengraf mana choice");
                    appliedManaSources++;
                }
                if (abilityDispatched) {
                    final AtomicReference<Boolean> nowResolved = new AtomicReference<>(false);
                    final boolean stackPassApplied = stackPassed;
                    FThreads.invokeInEdtAndWait(() -> {
                        final GameView view = ui.getGameView();
                        final PlayerView local = ui.getCurrentPlayer();
                        if (view == null || view.getId() != expectedGameViewId
                                || view.getTracker() != expectedGameTracker || local == null) return;
                        final StackItemView top = view.peekStack();
                        if (top != null && top.getTracker() == view.getTracker() && top.getSourceCard() != null
                                && roles.fengrafId() == top.getSourceCard().getId()
                                && local.equals(top.getActivatingPlayer())) return;
                        if (!stackPassApplied || !view.getStack().isEmpty()) return;
                        final CardView acquired = view.getTracker().getObj(
                                TrackableTypes.CardViewType, roles.fengrafCreatureId());
                        final CardView fengraf = view.getTracker().getObj(
                                TrackableTypes.CardViewType, roles.fengrafId());
                        if (acquired != null && acquired.getTracker() == view.getTracker()
                                && acquired.getZone() == ZoneType.Hand && local.equals(acquired.getOwner())
                                && fengraf != null && fengraf.getZone() == ZoneType.Graveyard) nowResolved.set(true);
                    });
                    if (nowResolved.get()) {
                        resolved = true;
                        break;
                    }
                }
                Thread.sleep(20);
            }
            if (!stackPassed || !resolved) {
                INVERSE_FENGRAF_PROGRESS.set("CLICK=" + (fengrafClickSent ? 1 : 0)
                        + "|ABILITY=" + (abilityDispatched ? 1 : 0)
                        + "|STACK_PASS=" + (stackPassed ? 1 : 0)
                        + "|PASS_GATE=" + stackPassGate.get()
                        + "|MANA_ACTIONS=" + tappedSources.size()
                        + "|MANA_APPLIED=" + appliedManaSources
                        + "|SACRIFICE_CONFIRM=" + (sacrificeConfirmationSent ? 1 : 0)
                        + "|RESOLVED=" + (resolved ? 1 : 0));
            }
            Assert.assertTrue(abilityDispatched, "remote selected the actual nonmana Fengraf ability from its rendered menu");
            Assert.assertTrue(stackPassed, "remote passed the actual Fengraf stack item through its live controller");
            Assert.assertTrue(resolved, "remote received the actual shared-graveyard card into its rendered hand");
            Assert.assertTrue(sacrificeConfirmationSent,
                    "remote confirmed the actual localized Fengraf sacrifice prompt through its rendered Yes button");
            Assert.assertEquals(tappedSources.size(), 3,
                    "the remote dispatched each of the three distinct required mana sources");

            final long panelReadyDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            boolean targetPanelReady = false;
            while (!targetPanelReady && System.nanoTime() < panelReadyDeadline) {
                final AtomicReference<Boolean> ready = new AtomicReference<>(false);
                FThreads.invokeInEdtAndWait(() -> {
                    final GameView view = ui.getGameView();
                    final PlayerView local = ui.getCurrentPlayer();
                    if (view == null || view.getId() != expectedGameViewId || view.getTracker() != expectedGameTracker
                            || local == null || !ui.isCurrentScreen()) return;
                    final CardView acquired = view.getTracker().getObj(
                            TrackableTypes.CardViewType, roles.fengrafCreatureId());
                    if (acquired == null || acquired.getTracker() != view.getTracker()
                            || acquired.getZone() != ZoneType.Hand || !local.equals(acquired.getOwner())) return;
                    final var hand = ui.getHandFor(local);
                    final CardPanel panel = hand == null ? null : hand.getHandArea().getCardPanel(acquired.getId());
                    final CardView panelCard = panel == null ? null : panel.getCard();
                    if (panel != null && panel.isShowing() && panel.isDisplayEnabled() && panelCard != null
                            && panelCard.getId() == acquired.getId() && panelCard.getTracker() == view.getTracker()) {
                        ready.set(true);
                    }
                });
                targetPanelReady = ready.get();
                if (!targetPanelReady) Thread.sleep(15);
            }
            final boolean panelReadyBeforeHover = targetPanelReady;

            final AtomicReference<Boolean> detailed = new AtomicReference<>(false);
            final AtomicReference<Boolean> allSourcesTapped = new AtomicReference<>(false);
            FThreads.invokeInEdtAndWait(() -> {
                final GameView view = ui.getGameView();
                final PlayerView local = ui.getCurrentPlayer();
                if (view == null || view.getId() != expectedGameViewId || view.getTracker() != expectedGameTracker
                        || local == null || !ui.isCurrentScreen()) return;
                final CardView acquired = view.getTracker().getObj(
                        TrackableTypes.CardViewType, roles.fengrafCreatureId());
                final CardView spentFengraf = view.getTracker().getObj(
                        TrackableTypes.CardViewType, roles.fengrafId());
                allSourcesTapped.set(roles.fengrafManaIds().stream().allMatch(id -> {
                    final CardView source = view.getTracker().getObj(TrackableTypes.CardViewType, id);
                    return source != null && source.getTracker() == view.getTracker() && source.isTapped()
                            && local.equals(source.getOwner()) && local.equals(source.getController());
                }));
                final var hand = ui.getHandFor(local);
                if (acquired == null || acquired.getTracker() != view.getTracker()
                        || acquired.getZone() != ZoneType.Hand || !local.equals(acquired.getOwner())
                        || spentFengraf == null || spentFengraf.getTracker() != view.getTracker()
                        || spentFengraf.getZone() != ZoneType.Graveyard || !view.getStack().isEmpty()) return;
                if (!panelReadyBeforeHover) {
                    LAST_UI_GATE.set("FENGRAF_HOVER_PANEL_NOT_READY");
                    return;
                }
                final CardPanel panel = hand == null ? null : hand.getHandArea().getCardPanel(acquired.getId());
                if (panel == null || !panel.isShowing() || !panel.isDisplayEnabled()
                        || panel.getCard() == null || panel.getCard().getId() != acquired.getId()
                        || panel.getCard().getTracker() != view.getTracker()) return;
                final CardArea area = hand.getHandArea();
                final Rectangle visible = new Rectangle(panel.getCardX(), panel.getCardY(),
                        panel.getCardWidth(), panel.getCardHeight()).intersection(area.getVisibleRect());
                final int hoverY = visible.y + visible.height / 2;
                Point hover = null;
                for (int x = visible.x; !visible.isEmpty() && x < visible.x + visible.width; x++) {
                    if (area.getCardPanel(x, hoverY) == panel) {
                        hover = new Point(x, hoverY);
                        break;
                    }
                }
                if (hover == null) return;
                final VDetail detail = (VDetail) EDocID.CARD_DETAIL.getDoc();
                if (!detail.getPnlDetail().getNameCostLabel().isShowing()) return;
                CardPanel baselinePanel = null;
                Point baselineHover = null;
                for (final CardPanel candidate : area.getCardPanels()) {
                    final CardView baselineCard = candidate.getCard();
                    if (candidate == panel || !candidate.isShowing() || !candidate.isDisplayEnabled()
                            || baselineCard == null || baselineCard.getTracker() != view.getTracker()
                            || baselineCard.getZone() != ZoneType.Hand || !local.equals(baselineCard.getOwner())
                            || acquired.getName().equals(baselineCard.getName())) continue;
                    final Rectangle baselineVisible = new Rectangle(candidate.getCardX(), candidate.getCardY(),
                            candidate.getCardWidth(), candidate.getCardHeight()).intersection(area.getVisibleRect());
                    final int baselineY = baselineVisible.y + baselineVisible.height / 2;
                    for (int x = baselineVisible.x; !baselineVisible.isEmpty()
                            && x < baselineVisible.x + baselineVisible.width; x++) {
                        if (area.getCardPanel(x, baselineY) == candidate) {
                            baselinePanel = candidate;
                            baselineHover = new Point(x, baselineY);
                            break;
                        }
                    }
                    if (baselinePanel != null) break;
                }
                if (baselinePanel == null) return;
                area.dispatchEvent(new MouseEvent(area, MouseEvent.MOUSE_EXITED, System.currentTimeMillis(), 0,
                        -1, -1, 0, false));
                area.dispatchEvent(new MouseEvent(area, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0,
                        baselineHover.x, baselineHover.y, 0, false));
                final String baselineDetail = detail.getPnlDetail().getNameCostLabel().getText();
                if (baselineDetail == null || !baselineDetail.contains(baselinePanel.getCard().getName())
                        || baselineDetail.contains(acquired.getName())) return;
                area.dispatchEvent(new MouseEvent(area, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0,
                        hover.x, hover.y, 0, false));
                final String detailText = detail.getPnlDetail().getNameCostLabel().getText();
                if (detailText != null && detailText.contains(acquired.getName())) {
                    detailed.set(true);
                }
            });
            Assert.assertTrue(detailed.get(), "the real recipient hand hover populated its card detail view");
            Assert.assertTrue(allSourcesTapped.get(), "the current remote view shows all three own Islands tapped");
            final GameView view = ui.getGameView();
            final CardView acquired = view.getTracker().getObj(TrackableTypes.CardViewType, roles.fengrafCreatureId());
            Assert.assertNotNull(acquired);
            Assert.assertEquals(acquired.getZone(), ZoneType.Hand);
            return "FENGRAF_REMOTE|ACTUAL_ABILITY=1|MANA_SOURCES=3|SACRIFICE=1|RECIPIENT=1"
                    + "|HAND_HOVER=1|DETAIL=1";
        }

        private static boolean automateInverseTellingTimeOrderDialog(final CMatchUI ui,
                final int expectedGameViewId, final Tracker expectedGameTracker,
                final PrivateLogRoles roles, final int ordinal) throws InterruptedException {
            final AtomicReference<Window> acted = new AtomicReference<>();
            FThreads.invokeInEdtAndWait(() -> {
                final GameView view = ui.getGameView();
                final PlayerView local = ui.getCurrentPlayer();
                if (view == null || view.getId() != expectedGameViewId || view.getTracker() != expectedGameTracker
                        || local == null || !ui.isCurrentScreen()
                        || !(ui.getGameController(local) instanceof NetGameController)) return;
                for (Window window : Window.getWindows()) {
                    if (!window.isShowing() || !"forge.gui.DualListBox".equals(window.getClass().getName())) continue;
                    final List<JList<?>> lists = new ArrayList<>();
                    final List<AbstractButton> buttons = new ArrayList<>();
                    collectChoiceListsAndButtons(window, lists, buttons);
                    if (lists.size() != 2) throw new AssertionError("actual client order prompt has two lists");
                    final JList<?> source = lists.get(0);
                    final int expectedRows = ordinal == 0 ? 3 : 2;
                    if (source.getModel().getSize() != expectedRows) continue;
                    final Set<String> expectedNames = ordinal == 0
                            ? Set.of(roles.privateOneName(), roles.privateTwoName(), roles.privateThreeName())
                            : Set.of(roles.privateTwoName(), roles.privateThreeName());
                    final Set<String> sourceNames = new HashSet<>();
                    int desiredIndex = -1;
                    final String desiredName = ordinal == 0 ? roles.privateOneName() : roles.privateTwoName();
                    for (int index = 0; index < source.getModel().getSize(); index++) {
                        final Object row = source.getModel().getElementAt(index);
                        if (!(row instanceof CardView cardView)) {
                            throw new AssertionError("actual client order prompt exposes card rows");
                        }
                        if (cardView.getTracker() != expectedGameTracker) {
                            throw new AssertionError("actual client order rows belong to the current applied tracker");
                        }
                        sourceNames.add(cardView.getName());
                        if (desiredName.equals(cardView.getName())) desiredIndex = index;
                    }
                    if (!sourceNames.equals(expectedNames) || desiredIndex < 0) {
                        throw new AssertionError("actual client order prompt exposes the expected role set");
                    }
                    source.setSelectedIndex(desiredIndex);
                    final JList<?> destination = lists.get(1);
                    final AbstractButton add = buttons.stream().filter(button -> ">".equals(button.getText()))
                            .findFirst().orElseThrow(() -> new AssertionError("actual client order prompt has its move action"));
                    if (!add.isShowing() || !add.isEnabled()) {
                        throw new AssertionError("actual client order prompt enables its move action");
                    }
                    add.doClick(0);
                    if (source.getModel().getSize() != expectedRows - 1 || destination.getModel().getSize() != 1
                            || !(destination.getModel().getElementAt(0) instanceof CardView selected)
                            || !desiredName.equals(selected.getName())) {
                        throw new AssertionError("actual client order prompt moved the declared selection");
                    }
                    final String okLabel = Localizer.getInstance().getMessage("lblOK");
                    final AbstractButton ok = buttons.stream().filter(button -> okLabel.equals(button.getText()))
                            .findFirst().orElseThrow(() -> new AssertionError("actual client order prompt has OK"));
                    if (!ok.isShowing() || !ok.isEnabled()) {
                        throw new AssertionError("actual client order prompt enables its required selection");
                    }
                    ok.doClick(0);
                    acted.set(window);
                    return;
                }
            });
            if (acted.get() == null) return false;
            waitForTellingTimeDialogClose(acted.get());
            return true;
        }

        private static TellingTimeSetup prepareRenderedTellingTime(final Game game, final boolean inverse) {
            Assert.assertNotNull(game, "the ordinary HostedMatch start hook receives its real Game");
            final Player host = game.getPlayers().get(0);
            final Player remote = game.getPlayers().get(1);
            final Player chooser = inverse ? remote : host;
            final Player sentinelOwner = inverse ? host : remote;
            final Set<Integer> originalIds = physicalCardIds(game);
            Assert.assertTrue(originalIds.size() == 80,
                    "the real post-opening fixture begins with all 80 physical DanDan cards");
            final Set<String> knownAtStart = new HashSet<>();
            final Set<String> publicZoneNames = new HashSet<>();
            final Set<String> initialChooserHandNames = chooser.getCardsIn(ZoneType.Hand).stream()
                    .map(Card::getName).collect(java.util.stream.Collectors.toSet());
            for (Player player : game.getPlayers()) {
                player.getCardsIn(ZoneType.Hand).stream().map(Card::getName).forEach(knownAtStart::add);
            }
            for (ZoneType zone : List.of(ZoneType.Battlefield, ZoneType.Graveyard, ZoneType.Exile)) {
                game.getCardsIn(zone).stream().map(Card::getName).forEach(name -> {
                    knownAtStart.add(name);
                    publicZoneNames.add(name);
                });
            }
            final String publicHistoryAtSetup = game.getGameLog().getAllEntries().stream()
                    .map(GameLogEntry::message).reduce("", (left, right) -> left + "\n" + right);
            Card tellingTime = allPhysicalCards(game).stream()
                    .filter(card -> "Telling Time".equals(card.getName())).findFirst()
                    .orElseThrow(() -> new AssertionError("official pool contains the declared spell"));
            tellingTime = movePhysicalCardToHand(game, chooser, tellingTime);
            Assert.assertSame(tellingTime.getOwner(), chooser,
                    "the actual hand move gives the chooser ownership of the prepared spell");
            Assert.assertSame(tellingTime.getZone(), chooser.getZone(ZoneType.Hand),
                    "the prepared spell is in the chooser's real hand");
            Assert.assertSame(tellingTime.getController(), chooser,
                    "the prepared spell is controlled by the actual chooser");
            Card daysUndoing = null;
            if (inverse) {
                daysUndoing = allPhysicalCards(game).stream()
                        .filter(card -> "Day's Undoing".equals(card.getName()))
                        .findFirst().orElseThrow(() -> new AssertionError("official pool contains Day's Undoing"));
                daysUndoing = movePhysicalCardToHand(game, chooser, daysUndoing);
                Assert.assertSame(daysUndoing.getOwner(), chooser,
                        "the prepared Day's Undoing belongs to the actual remote chooser");
                Assert.assertSame(daysUndoing.getZone(), chooser.getZone(ZoneType.Hand));
                Assert.assertSame(daysUndoing.getController(), chooser);
            }

            final Set<Integer> usedIds = new HashSet<>();
            usedIds.add(tellingTime.getId());
            if (daysUndoing != null) usedIds.add(daysUndoing.getId());
            final List<Card> mana = new ArrayList<>();
            for (Card card : allPhysicalCards(game)) {
                if ("Island".equals(card.getName()) && (inverse || card.getOwner() == chooser)
                        && usedIds.add(card.getId())) {
                    mana.add(card);
                    if (mana.size() == 2) break;
                }
            }
            Assert.assertEquals(mana.size(), 2, "the official pool has two legal Islands available for the spell");
            final List<Card> manaSources = new ArrayList<>();
            for (Card source : mana) {
                Card ownedSource = inverse ? movePhysicalCardToHand(game, chooser, source) : source;
                Assert.assertSame(ownedSource.getOwner(), chooser,
                        "the actual hand move gives the chooser ownership of each prepared Island");
                Card moved = game.getAction().moveTo(chooser.getZone(ZoneType.Battlefield), ownedSource, null);
                moved.setController(chooser, game.getNextTimestamp());
                game.getAction().controllerChangeZoneCorrection(moved);
                moved.setTapped(false);
                Assert.assertSame(moved.getOwner(), chooser,
                        "the prepared Island retains its actual chooser ownership on the battlefield");
                Assert.assertSame(moved.getController(), chooser,
                        "the prepared Island is controlled by the actual chooser");
                manaSources.add(moved);
            }

            int fengrafId = -1;
            int fengrafCreatureId = -1;
            final List<Card> fengrafManaSources = new ArrayList<>();
            if (inverse) {
                Card fengraf = allPhysicalCards(game).stream()
                        .filter(card -> "Haunted Fengraf".equals(card.getName()) && usedIds.add(card.getId()))
                        .findFirst().orElseThrow(() -> new AssertionError("official pool contains Haunted Fengraf"));
                fengraf = movePhysicalCardToHand(game, chooser, fengraf);
                fengraf = game.getAction().moveTo(chooser.getZone(ZoneType.Battlefield), fengraf, null);
                fengraf.setController(chooser, game.getNextTimestamp());
                game.getAction().controllerChangeZoneCorrection(fengraf);
                fengraf.setTapped(false);
                Assert.assertSame(fengraf.getOwner(), chooser);
                Assert.assertSame(fengraf.getController(), chooser);
                fengrafId = fengraf.getId();

                for (Card card : allPhysicalCards(game)) {
                    if ("Island".equals(card.getName()) && (inverse || card.getOwner() == chooser)
                            && usedIds.add(card.getId())) {
                        Card ownedSource = movePhysicalCardToHand(game, chooser, card);
                        Card moved = game.getAction().moveTo(chooser.getZone(ZoneType.Battlefield), ownedSource, null);
                        moved.setController(chooser, game.getNextTimestamp());
                        game.getAction().controllerChangeZoneCorrection(moved);
                        moved.setTapped(false);
                        Assert.assertSame(moved.getOwner(), chooser);
                        Assert.assertSame(moved.getController(), chooser);
                        fengrafManaSources.add(moved);
                        if (fengrafManaSources.size() == 3) break;
                    }
                }
                Assert.assertEquals(fengrafManaSources.size(), 3,
                        "the official pool provides three separately owned Islands for Fengraf");

                Card creature = allPhysicalCards(game).stream()
                        .filter(Card::isCreature).filter(card -> "Dandân".equals(card.getName()))
                        .filter(card -> usedIds.add(card.getId()))
                        .findFirst().orElseThrow(() -> new AssertionError("official pool contains a creature"));
                creature = movePhysicalCardToHand(game, host, creature);
                creature = game.getAction().moveTo(host.getZone(ZoneType.Graveyard), creature, null);
                Assert.assertSame(creature.getOwner(), host,
                        "the public shared-graveyard creature retains its host origin");
                Assert.assertSame(creature.getZone(), host.getZone(ZoneType.Graveyard));
                Assert.assertSame(creature.getZone(), chooser.getZone(ZoneType.Graveyard));
                fengrafCreatureId = creature.getId();
                Assert.assertEquals(game.getCardsIn(ZoneType.Graveyard).size(), 1,
                        "the shared graveyard contains only the deterministic Fengraf creature");
            }

            for (Player player : game.getPlayers()) {
                player.getCardsIn(ZoneType.Hand).stream().map(Card::getName).forEach(knownAtStart::add);
            }
            for (ZoneType zone : List.of(ZoneType.Battlefield, ZoneType.Graveyard, ZoneType.Exile)) {
                game.getCardsIn(zone).stream().map(Card::getName).forEach(name -> {
                    knownAtStart.add(name);
                    publicZoneNames.add(name);
                });
            }
            final Set<Card> protectedChooserCards = daysUndoing == null
                    ? Set.of(tellingTime) : Set.of(tellingTime, daysUndoing);
            normalizeHandToSeven(game, host, chooser == host ? protectedChooserCards : Set.of(), inverse);
            normalizeHandToSeven(game, remote, chooser == remote ? protectedChooserCards : Set.of(), inverse);
            for (Player player : game.getPlayers()) {
                player.getCardsIn(ZoneType.Hand).stream().map(Card::getName).forEach(knownAtStart::add);
            }
            final List<Card> privateCards = new ArrayList<>();
            final Set<String> privateNames = new HashSet<>();
            for (Card card : chooser.getCardsIn(ZoneType.Library)) {
                if ((inverse || card.getOwner() == chooser) && !knownAtStart.contains(card.getName())
                        && !publicHistoryAtSetup.contains(card.getName())
                        && !"Island".equals(card.getName()) && !"Telling Time".equals(card.getName())
                        && privateNames.add(card.getName())) {
                    privateCards.add(card);
                    if (privateCards.size() == 3) break;
                }
            }
            Assert.assertEquals(privateCards.size(), 3,
                    "three unique library identities are absent from both initial hands and public zones");

            final Card handSentinel = sentinelOwner.getCardsIn(ZoneType.Hand).stream()
                    .filter(card -> card.getOwner() == sentinelOwner && !privateNames.contains(card.getName())
                            && !publicZoneNames.contains(card.getName())
                            && !initialChooserHandNames.contains(card.getName())
                            && !publicHistoryAtSetup.contains(card.getName()))
                    .filter(card -> chooser.getCardsIn(ZoneType.Hand).stream()
                            .noneMatch(chooserCard -> chooserCard.getName().equals(card.getName())))
                    .findFirst().orElseThrow(() -> new AssertionError("nonchooser opening hand contains a sentinel candidate"));

            if (inverse) {
                // Reserve a never-remote-seen name for the later Day's Undoing witness.
                // The intervening remote T2 draw takes a Telling Time role, not this name.
                Assert.assertFalse(initialChooserHandNames.contains(handSentinel.getName()));
                Assert.assertFalse(privateNames.contains(handSentinel.getName()));
                Assert.assertFalse(publicZoneNames.contains(handSentinel.getName()));
                Assert.assertTrue(chooser.getCardsIn(ZoneType.Hand).stream()
                        .noneMatch(card -> handSentinel.getName().equals(card.getName())));
                for (Card copy : new ArrayList<>(chooser.getCardsIn(ZoneType.Library))) {
                    if (handSentinel.getName().equals(copy.getName())) {
                        game.getAction().moveToLibrary(copy, -1, null);
                    }
                }
            }
            for (int index = privateCards.size() - 1; index >= 0; index--) {
                game.getAction().moveToLibrary(privateCards.get(index), 0, null);
            }
            Assert.assertEquals(chooser.getCardsIn(ZoneType.Library).get(0).getName(), privateCards.get(0).getName());
            Assert.assertEquals(chooser.getCardsIn(ZoneType.Library).get(1).getName(), privateCards.get(1).getName());
            Assert.assertEquals(chooser.getCardsIn(ZoneType.Library).get(2).getName(), privateCards.get(2).getName());
            Assert.assertTrue(physicalCardIds(game).equals(originalIds),
                    "declared setup preserves the exact official physical 80-card pool");
            return new TellingTimeSetup(game, tellingTime, daysUndoing, host, remote, privateCards.get(0).getId(),
                    privateCards.get(1).getId(), privateCards.get(2).getId(), privateCards.get(0).getName(),
                    privateCards.get(1).getName(), privateCards.get(2).getName(), handSentinel.getName(),
                    List.copyOf(manaSources), fengrafId, fengrafCreatureId, List.copyOf(fengrafManaSources),
                    Set.copyOf(originalIds), null);
        }

        private static void normalizeHandToSeven(final Game game, final Player player,
                final Set<Card> protectedCards, final boolean allowSharedLibraryCards) {
            while (player.getCardsIn(ZoneType.Hand).size() > 7) {
                final Card extra = player.getCardsIn(ZoneType.Hand).stream()
                        .filter(card -> !protectedCards.contains(card)).reduce((first, second) -> second)
                        .orElseThrow(() -> new AssertionError("fixture cannot remove a protected role from the initial hand"));
                game.getAction().moveToLibrary(extra, 0, null);
            }
            while (player.getCardsIn(ZoneType.Hand).size() < 7) {
                final Card next = player.getCardsIn(ZoneType.Library).stream()
                        .filter(card -> allowSharedLibraryCards || card.getOwner() == player)
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("official pool can restore the ordinary seven-card hand"));
                final Card drawn = game.getAction().moveTo(player.getZone(ZoneType.Hand), next, null);
                Assert.assertSame(drawn.getOwner(), player,
                        "the actual hand move gives the player ownership of each ordinary drawn card");
            }
            Assert.assertEquals(player.getCardsIn(ZoneType.Hand).size(), 7,
                    "rendered setup preserves the ordinary seven-card hand");
        }

        private static Card movePhysicalCardToHand(final Game game, final Player player, Card card) {
            final var hand = player.getZone(ZoneType.Hand);
            if (card.getZone() == hand && card.getOwner() == player) return card;
            if (card.getZone().is(ZoneType.Hand)) {
                card = game.getAction().moveToLibrary(card, 0, null);
            }
            return game.getAction().moveTo(hand, card, null);
        }

        private static List<Card> allPhysicalCards(final Game game) {
            final List<Card> result = new ArrayList<>();
            for (Player player : game.getPlayers()) {
                for (ZoneType zone : List.of(ZoneType.Hand, ZoneType.Library, ZoneType.Battlefield,
                        ZoneType.Graveyard, ZoneType.Exile)) {
                    result.addAll(player.getCardsIn(zone));
                }
            }
            result.addAll(game.getCardsIn(ZoneType.Stack));
            return result;
        }

        private static int renderedPhysicalCardCount(final GameView view) {
            Assert.assertTrue(view.getStack().isEmpty(),
                    "rendered physical conservation is checked after the current spell left the stack");
            final List<PlayerView> players = new ArrayList<>(view.getPlayers());
            Assert.assertEquals(players.size(), 2);
            final List<ZoneType> zones = List.of(ZoneType.Hand, ZoneType.Library, ZoneType.Battlefield,
                    ZoneType.Graveyard, ZoneType.Exile);
            final List<Map<ZoneType, List<Integer>>> idsByPlayer = new ArrayList<>();
            for (PlayerView player : players) {
                final Map<ZoneType, List<Integer>> idsByZone = new java.util.EnumMap<>(ZoneType.class);
                for (ZoneType zone : zones) {
                    final List<Integer> ids = player.getCards(zone).stream().map(CardView::getId).toList();
                    Assert.assertEquals(ids.size(), player.getZoneSize(zone),
                            "the current applied view retains each physical identity in " + zone);
                    Assert.assertEquals(ids.size(), new HashSet<>(ids).size(),
                            "a current view zone contains no repeated physical identity: " + zone);
                    idsByZone.put(zone, ids);
                }
                idsByPlayer.add(idsByZone);
            }
            for (ZoneType shared : List.of(ZoneType.Library, ZoneType.Graveyard)) {
                Assert.assertEquals(idsByPlayer.get(0).get(shared), idsByPlayer.get(1).get(shared),
                        "both assigned views retain the same ordered shared " + shared);
            }
            final Set<Integer> physicalIds = new HashSet<>();
            for (int seat = 0; seat < players.size(); seat++) {
                for (ZoneType zone : zones) {
                    if (seat == 1 && (zone == ZoneType.Library || zone == ZoneType.Graveyard)) continue;
                    for (Integer id : idsByPlayer.get(seat).get(zone)) {
                        Assert.assertTrue(physicalIds.add(id), "current view zones retain unique physical identities");
                    }
                }
            }
            return physicalIds.size();
        }

        private static Set<Integer> physicalCardIds(final Game game) {
            final Set<Integer> ids = new HashSet<>();
            for (Card card : allPhysicalCards(game)) ids.add(card.getId());
            return ids;
        }

        private static Card findPhysicalCard(final Game game, final int id) {
            return allPhysicalCards(game).stream().filter(card -> card.getId() == id).findFirst()
                    .orElseThrow(() -> new AssertionError("declared physical role remains in the conserved pool"));
        }

        private static void publishTellingTimeArmStage(final String role, final String stage) {
            RESULT.println("ARM_STAGE|" + role + "|" + stage);
            RESULT.flush();
        }

        private static void armActualReportLog(final CMatchUI ui, final String role) {
            publishTellingTimeArmStage(role, "EDT_REQUEST");
            FThreads.invokeInEdtAndWait(() -> {
                publishTellingTimeArmStage(role, "EDT_ENTER");
                final IVDoc<?> reportLog = EDocID.REPORT_LOG.getDoc();
                publishTellingTimeArmStage(role, "REPORT_LOG_TYPE_CHECK");
                Assert.assertTrue(reportLog instanceof VLog,
                        "the ordinary match view registers its actual report-log consumer");
                publishTellingTimeArmStage(role, "REPORT_LOG_PARENT_CHECK");
                Assert.assertNotNull(reportLog.getParentCell(),
                        "the ordinary report log is attached to the actual rendered client layout");
                publishTellingTimeArmStage(role, "REPORT_LOG_MATCH_UI_CHECK");
                Assert.assertSame(((VLog) reportLog).getLayoutControl().getMatchUI(), ui,
                        "the rendered report log belongs to this actual client match UI");
                SDisplayUtil.showTab(reportLog);
                publishTellingTimeArmStage(role, "REPORT_LOG_SELECTED_CHECK");
                Assert.assertSame(reportLog.getParentCell().getSelected(), reportLog,
                        "each child has selected its own rendered report log");
                publishTellingTimeArmStage(role, "REPORT_LOG_REFRESH");
                ui.refreshLog();
                publishTellingTimeArmStage(role, "REPORT_LOG_CONSUMER_CHECK");
                Assert.assertNotNull(actualRenderedGameLogPanel(reportLog, ui),
                        "the selected cell body contains its actual showing game-log consumer after refresh");
            });
            publishTellingTimeArmStage(role, "EDT_RETURN");
        }

        private static String castAndResolveTellingTime(final CMatchUI ui, final TellingTimeSetup setup)
                throws Exception {
            publishTellingTimeCastStage("CAST_ENTER");
            Assert.assertNotNull(setup, "the host owns the declared private setup roles");
            final Game game = setup.game();
            final Player host = game.getPlayers().get(0);
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);
            while (System.nanoTime() < deadline) {
                final Input input = setup.controller().getInputProxy().getInput();
                if (game.getPhaseHandler().getPriorityPlayer() == host
                        && input instanceof InputPassPriority && setup.tellingTime().getZone().is(ZoneType.Hand)) {
                    break;
                }
                Thread.sleep(20);
            }
            final Input input = setup.controller().getInputProxy().getInput();
            Assert.assertSame(game.getPhaseHandler().getPriorityPlayer(), host,
                    "the fixture arranged the host as the actual first-priority player");
            Assert.assertTrue(input instanceof InputPassPriority,
                    "host dispatch starts from its actual live priority input");
            Assert.assertTrue(input.getOwner() != null && input.getOwner().equals(host.getView()),
                    "the live priority input belongs to the actual host seat");
            publishTellingTimeCastStage("INPUT_VALIDATED");

            final AtomicReference<Throwable> choiceFailure = new AtomicReference<>();
            final AtomicInteger completedChoices = new AtomicInteger();
            final AtomicInteger inputChoices = new AtomicInteger();
            final AtomicInteger dualListChoices = new AtomicInteger();
            final AtomicInteger hostPasses = new AtomicInteger();
            final AtomicInteger manaSourcesActivated = new AtomicInteger();
            final Set<Card> activatedManaSources = new HashSet<>();
            final AtomicReference<Set<Integer>> paidManaSourcesOnStack = new AtomicReference<>();
            final AtomicReference<Input> lastPassedInput = new AtomicReference<>();
            final AtomicReference<Boolean> choiceRunning = new AtomicReference<>(true);
            final Thread choices = new Thread(() -> {
                final long choiceDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(55);
                while (choiceRunning.get() && System.nanoTime() < choiceDeadline && completedChoices.get() < 2
                        && choiceFailure.get() == null) {
                    try {
                        final int ordinal = completedChoices.get();
                        if (automateTellingTimeInputChoice(ui, setup, ordinal)) {
                            inputChoices.incrementAndGet();
                            completedChoices.incrementAndGet();
                            continue;
                        }
                        final Window dialog = automateTellingTimeOrderDialog(setup, ordinal);
                        if (dialog == null) Thread.sleep(15);
                        else {
                            waitForTellingTimeDialogClose(dialog);
                            dualListChoices.incrementAndGet();
                            completedChoices.incrementAndGet();
                        }
                    } catch (Throwable failure) {
                        choiceFailure.compareAndSet(null, failure);
                    }
                }
            }, "dandan-rendered-tt-order-choices");
            choices.setDaemon(true);
            choices.start();
            boolean resolved = false;
            Input stableResolutionPriority = null;
            try {
                final Input castInput = setup.controller().getInputProxy().getInput();
                FThreads.invokeInEdtAndWait(() -> {
                    Assert.assertSame(setup.controller().getInputProxy().getInput(), castInput,
                            "host still owns the captured priority input when selecting Telling Time");
                    Assert.assertSame(game.getPhaseHandler().getPriorityPlayer(), host);
                    Assert.assertTrue(ui.getGameView().getId() == game.getView().getId(),
                            "the host controller remains bound to its prepared GameView");
                    setup.controller().selectCard(setup.tellingTime().getView(), null, null);
                });
                publishTellingTimeCastStage("SELECT_SENT");
                final long resolveDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(55);
                publishTellingTimeCastStage("RESOLUTION_WAIT");
                while (System.nanoTime() < resolveDeadline) {
                    if (choiceFailure.get() != null) throw new AssertionError(
                            "actual Telling Time order dialog automation failed", choiceFailure.get());
                    final Input currentInput = setup.controller().getInputProxy().getInput();
                    if (currentInput instanceof InputPayMana) {
                        for (Card source : setup.manaSources()) {
                            if (!source.isTapped() && !activatedManaSources.contains(source)) {
                                final AtomicReference<Boolean> selected = new AtomicReference<>(false);
                                FThreads.invokeInEdtAndWait(() -> {
                                    Assert.assertSame(setup.controller().getInputProxy().getInput(), currentInput,
                                            "host payment input remains live before source selection");
                                    Assert.assertSame(setup.controller().getInputQueue().getInput(), currentInput,
                                            "host payment input remains queued before source selection");
                                    Assert.assertEquals(currentInput.getOwner(), host.getView());
                                    Assert.assertSame(source.getController(), host);
                                    Assert.assertTrue(source.getZone().is(ZoneType.Battlefield));
                                    selected.set(setup.controller().selectCard(source.getView(), null, null));
                                });
                                // A tapped first source can precede InputPayMana's asynchronous unlock.
                                // Retry a rejected click within the existing resolution deadline.
                                if (!selected.get()) break;
                                activatedManaSources.add(source);
                                final long paymentDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                                while (System.nanoTime() < paymentDeadline && !source.isTapped()) Thread.sleep(10);
                                if (source.isTapped()) manaSourcesActivated.incrementAndGet();
                                else throw new AssertionError("actual mana input did not activate the selected Island");
                                break;
                            }
                        }
                    }
                    if (!game.getStack().isEmpty() && paidManaSourcesOnStack.get() == null) {
                        final var liveAbility = game.getStack().peekAbility();
                        Assert.assertTrue(liveAbility.getHostCard().getId() == setup.tellingTime().getId(),
                                "the captured stack payment belongs to the actual Telling Time spell");
                        Assert.assertSame(liveAbility.getActivatingPlayer(), host,
                                "the captured spell ability belongs to the casting host seat");
                        paidManaSourcesOnStack.set(liveAbility.getPayingMana().stream()
                                .map(mana -> mana.getSourceCard().getId()).collect(java.util.stream.Collectors.toSet()));
                    }
                    if (!game.getStack().isEmpty() && game.getPhaseHandler().getPriorityPlayer() == host
                            && currentInput instanceof InputPassPriority && lastPassedInput.get() != currentInput) {
                        final AtomicReference<Boolean> passed = new AtomicReference<>(false);
                        FThreads.invokeInEdtAndWait(() -> {
                            if (setup.controller().getInputProxy().getInput() == currentInput
                                    && game.getPhaseHandler().getPriorityPlayer() == host && !game.getStack().isEmpty()) {
                                setup.controller().selectButtonOk();
                                passed.set(true);
                            }
                        });
                        if (passed.get()) {
                            lastPassedInput.set(currentInput);
                            hostPasses.incrementAndGet();
                        }
                    }
                    final boolean postResolutionPriority = completedChoices.get() == 2
                            && game.getStack().isEmpty()
                            && game.getPhaseHandler().getPriorityPlayer() == host
                            && currentInput instanceof InputPassPriority
                            && currentInput != lastPassedInput.get()
                            && setup.controller().getInputProxy().getInput() == currentInput
                            && setup.controller().getInputQueue().getInput() == currentInput
                            && currentInput.getOwner() != null
                            && currentInput.getOwner().equals(host.getView());
                    if (!postResolutionPriority) {
                        stableResolutionPriority = null;
                        Thread.sleep(20);
                        continue;
                    }
                    if (stableResolutionPriority != currentInput) {
                        stableResolutionPriority = currentInput;
                        Thread.sleep(20);
                        continue;
                    }
                    final Card currentSpell = findPhysicalCard(game, setup.tellingTime().getId());
                    final Card currentPrivateOne = findPhysicalCard(game, setup.privateOneId());
                    if (completedChoices.get() == 2 && currentSpell.getZone().is(ZoneType.Graveyard)
                            && currentPrivateOne.getZone() == host.getZone(ZoneType.Hand)
                            && !host.getCardsIn(ZoneType.Library).isEmpty()
                            && host.getCardsIn(ZoneType.Library).get(0).getId() == setup.privateTwoId()
                            && host.getCardsIn(ZoneType.Library)
                                    .get(host.getCardsIn(ZoneType.Library).size() - 1).getId() == setup.privateThreeId()
                            && game.getStack().isEmpty()) {
                        resolved = true;
                        break;
                    }
                    Thread.sleep(20);
                }
            } finally {
                choiceRunning.set(false);
                choices.interrupt();
                choices.join(5000);
                Assert.assertFalse(choices.isAlive(), "choice automation stops before match lifecycle teardown");
            }
            if (!resolved) {
                try {
                    final List<Card> physicalCards = allPhysicalCards(game);
                    final Card currentSpell = physicalCards.stream()
                            .filter(card -> card.getId() == setup.tellingTime().getId()).findFirst().orElse(null);
                    final Card currentPrivateOne = physicalCards.stream()
                            .filter(card -> card.getId() == setup.privateOneId()).findFirst().orElse(null);
                    final List<Card> library = List.copyOf(host.getCardsIn(ZoneType.Library));
                    final Throwable orderFailure = choiceFailure.get();
                    RESULT.println("TT_RESOLUTION_DIAG|CHOICES=" + completedChoices.get()
                            + "|INPUT_CHOICES=" + inputChoices.get()
                            + "|DUAL_LIST_CHOICES=" + dualListChoices.get()
                            + "|SPELL_GRAVEYARD=" + (currentSpell != null
                                    && currentSpell.getZone() == host.getZone(ZoneType.Graveyard) ? 1 : 0)
                            + "|FIRST_CHOICE_HAND=" + (currentPrivateOne != null
                                    && currentPrivateOne.getZone() == host.getZone(ZoneType.Hand) ? 1 : 0)
                            + "|LIBRARY_SIZE=" + library.size()
                            + "|TOP_MATCH=" + (!library.isEmpty()
                                    && library.get(0).getId() == setup.privateTwoId() ? 1 : 0)
                            + "|BOTTOM_MATCH=" + (!library.isEmpty()
                                    && library.get(library.size() - 1).getId() == setup.privateThreeId() ? 1 : 0)
                            + "|STACK_EMPTY=" + (game.getStack().isEmpty() ? 1 : 0)
                            + "|PAID_SOURCE_COUNT=" + (paidManaSourcesOnStack.get() == null
                                    ? 0 : paidManaSourcesOnStack.get().size())
                            + "|MANA_ACTIVATIONS=" + manaSourcesActivated.get()
                            + "|HOST_PASSES=" + hostPasses.get()
                            + "|CHOICE_FAILURE=" + (orderFailure == null
                                    ? "none" : orderFailure.getClass().getSimpleName()));
                } catch (RuntimeException diagnosticFailure) {
                    RESULT.println("TT_RESOLUTION_DIAG|OBSERVATION_FAILED="
                            + diagnosticFailure.getClass().getSimpleName());
                }
                RESULT.flush();
            }
            Assert.assertTrue(resolved, "the actual spell resolved and applied all three declared zone outcomes");
            publishTellingTimeCastStage("RESOLVED");
            Assert.assertEquals(completedChoices.get(), 2,
                    "the real nested Telling Time ability completed exactly two actual choice steps");
            final List<GameLogEntry> spellEntries = game.getGameLog().getAllEntries().stream()
                    .filter(entry -> entry.sourceCard() != null
                            && entry.sourceCard().getId() == setup.tellingTime().getId()).toList();
            Assert.assertEquals(spellEntries.stream().filter(entry -> entry.type() == GameLogEntryType.STACK_ADD).count(),
                    1, "the real spell produced one public cast log entry");
            Assert.assertEquals(spellEntries.stream().filter(entry -> entry.type() == GameLogEntryType.STACK_RESOLVE).count(),
                    1, "the real spell produced one public resolution log entry");
            final Set<Integer> declaredManaIds = setup.manaSources().stream().map(Card::getId)
                    .collect(java.util.stream.Collectors.toSet());
            final Set<Integer> paidManaSourceIds = paidManaSourcesOnStack.get();
            Assert.assertNotNull(paidManaSourceIds, "the paid-mana record was captured from the live spell stack");
            Assert.assertTrue(paidManaSourceIds.equals(declaredManaIds),
                    "the resolved spell's actual paid-mana record names both declared Island sources");
            Assert.assertEquals(manaSourcesActivated.get(), 2,
                    "the actual host payment input activated both required Island mana abilities");
            Assert.assertTrue(setup.manaSources().stream().allMatch(Card::isTapped),
                    "both actual mana ability sources are tapped after payment");
            Assert.assertTrue(physicalCardIds(game).equals(setup.originalPhysicalIds()),
                    "the resolved choice retains all original physical card identities");
            publishTellingTimeCastStage("ENGINE_ORACLES_CHECKED");
            final String logText = actualRenderedReportLogText(ui, game.getView());
            publishTellingTimeCastStage("RENDERED_LOG_READ");
            final GameLogEntry castEntry = spellEntries.stream()
                    .filter(entry -> entry.type() == GameLogEntryType.STACK_ADD).findFirst().orElseThrow();
            final GameLogEntry resolveEntry = spellEntries.stream()
                    .filter(entry -> entry.type() == GameLogEntryType.STACK_RESOLVE).findFirst().orElseThrow();
            Assert.assertTrue(logText.contains("Telling Time"),
                    "the caster's actual rendered log contains the public resolving spell");
            Assert.assertTrue(logText.contains(castEntry.message()) && logText.contains(resolveEntry.message()),
                    "the caster's actual rendered log contains both actual cast and resolution entries");
            final boolean sentinelAbsent = !logText.contains(setup.historySentinelName());
            Assert.assertTrue(sentinelAbsent,
                    "the actual host rendered log omits the remote-owned private-hand sentinel");
            Assert.assertTrue(hostPasses.get() > 0 && hostPasses.get() <= 4,
                    "the host passed each observed nonempty-stack priority a bounded number of times");
            publishTellingTimeCastStage("RENDERED_LOG_CHECKED");
            return "TT_CAST|HOST|RESOLVED=1|APPLIED=1|MANA_SOURCES=" + manaSourcesActivated.get()
                    + "|HOST_PASSES=" + hostPasses.get() + "|PUBLIC_LOG=1|HAND_SENTINEL_ABSENT=1"
                    + "|CHOICES=" + completedChoices.get() + "|INPUT_CHOICES=" + inputChoices.get()
                    + "|DUAL_LIST_CHOICES=" + dualListChoices.get();
        }

        private static boolean automateTellingTimeInputChoice(final CMatchUI ui, final TellingTimeSetup setup,
                final int ordinal) throws Exception {
            if (ordinal < 0 || ordinal > 1) throw new AssertionError("unexpected Telling Time choice ordinal");
            final AtomicReference<Boolean> handled = new AtomicReference<>(false);
            final AtomicReference<Throwable> failure = new AtomicReference<>();
            FThreads.invokeInEdtAndWait(() -> {
                try {
                    final Game game = setup.game();
                    final PlayerControllerHuman controller = setup.controller();
                    final Input proxyInput = controller.getInputProxy().getInput();
                    final Input queueInput = controller.getInputQueue().getInput();
                    if (proxyInput != queueInput || !(proxyInput instanceof InputSelectEntitiesFromList<?> selection)
                            || controller.getGame() != game || game.isGameOver()
                            || setup.host().getController() != controller || !setup.host().equals(game.getPlayers().get(0))) return;
                    final GameView view = ui.getGameView();
                    if (view == null || view.getId() != game.getView().getId()
                            || ui.getGameController(setup.host().getView()) != controller) return;
                    final var ability = game.getStack().isEmpty() ? null : game.getStack().peekAbility();
                    if (ability == null || ability.getHostCard().getId() != setup.tellingTime().getId()
                            || ability.getActivatingPlayer() != setup.host()) return;
                    if (!setup.host().getView().equals(proxyInput.getOwner())) {
                        throw new AssertionError("Telling Time selection input has the wrong owner");
                    }
                    final int desiredId = ordinal == 0 ? setup.privateOneId() : setup.privateTwoId();
                    final Set<Integer> expectedIds = ordinal == 0
                            ? Set.of(setup.privateOneId(), setup.privateTwoId(), setup.privateThreeId())
                            : Set.of(setup.privateTwoId(), setup.privateThreeId());
                    final Set<Integer> candidateIds = new HashSet<>();
                    for (Object choice : selection.getValidChoices()) {
                        if (!(choice instanceof Card card)) {
                            throw new AssertionError("Telling Time list input contains a non-card choice");
                        }
                        candidateIds.add(card.getId());
                    }
                    if (selection.getValidChoices().size() != expectedIds.size()
                            || !candidateIds.equals(expectedIds)) {
                        throw new AssertionError("Telling Time list input did not expose the exact expected role set");
                    }
                    final Card desired = selection.getValidChoices().stream()
                            .filter(Card.class::isInstance).map(Card.class::cast)
                            .filter(card -> card.getId() == desiredId).findFirst()
                            .orElseThrow(() -> new AssertionError("expected Telling Time role is not selectable"));
                    final CardView desiredView = desired.getView();
                    if (desiredView == null || desiredView.getId() != desiredId
                            || desiredView.getTracker() != view.getTracker()) {
                        throw new AssertionError("expected Telling Time choice has no matching current CardView");
                    }
                    if (controller.getInputProxy().getInput() != proxyInput
                            || controller.getInputQueue().getInput() != queueInput
                            || controller.getGame() != game || game.isGameOver()
                            || ui.getGameView() == null || ui.getGameView().getId() != view.getId()
                            || ui.getGameView().getTracker() != view.getTracker()
                            || ui.getGameController(setup.host().getView()) != controller) return;
                    final var currentAbility = game.getStack().isEmpty() ? null : game.getStack().peekAbility();
                    if (currentAbility == null || currentAbility.getHostCard().getId() != setup.tellingTime().getId()
                            || currentAbility.getActivatingPlayer() != setup.host()) return;
                    if (!controller.selectCard(desiredView, null, null)) {
                        throw new AssertionError("Telling Time choice was not accepted by the current input");
                    }
                    final var selected = selection.getSelected();
                    if (selected.size() != 1 || selected.stream().noneMatch(card -> card.getId() == desiredId)) {
                        throw new AssertionError("Telling Time list input did not retain the exact selected role");
                    }
                    if (controller.getInputProxy().getInput() == proxyInput
                            || controller.getInputQueue().getInput() == queueInput) {
                        throw new AssertionError("Telling Time choice did not consume its exact live input");
                    }
                    handled.set(true);
                } catch (Throwable problem) {
                    failure.set(problem);
                }
            });
            if (failure.get() != null) {
                if (failure.get() instanceof Exception exception) throw exception;
                if (failure.get() instanceof Error error) throw error;
                throw new AssertionError("Telling Time input choice failed", failure.get());
            }
            return handled.get();
        }

        private static void publishTellingTimeCastStage(final String stage) {
            RESULT.println("TT_CAST_STAGE|HOST|" + stage);
            RESULT.flush();
        }

        private static boolean passActualRemoteStackPriority(final CMatchUI ui, final int expectedGameViewId)
                throws InterruptedException {
            final AtomicReference<String> lastGate = LAST_REMOTE_STACK_PRIORITY_GATE;
            lastGate.set("NOT_SAMPLED");
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(55);
            while (System.nanoTime() < deadline) {
                final AtomicReference<Boolean> passed = new AtomicReference<>(false);
                FThreads.invokeInEdtAndWait(() -> {
                    final PlayerView local = ui.getCurrentPlayer();
                    final GameView view = ui.getGameView();
                    final IGameController candidate = local == null ? null : ui.getGameController(local);
                    final IVDoc<?> promptDoc = EDocID.REPORT_MESSAGE.getDoc();
                    lastGate.set("LOCAL=" + (local != null) + "|CONTROLLER=" + (candidate != null)
                            + "|VIEW=" + (view != null && view.getId() == expectedGameViewId)
                            + "|PRIORITY=" + (local != null && local.getHasPriority())
                            + "|STACK=" + (view != null && !view.getStack().isEmpty())
                            + "|PROMPT=" + (promptDoc instanceof VPrompt));
                    if (local == null || candidate == null || view == null || view.getId() != expectedGameViewId
                            || !local.getHasPriority() || view.getStack().isEmpty()
                            || view.peekStack() == null || !(promptDoc instanceof VPrompt prompt)) return;
                    final String undo = Localizer.getInstance().getMessage("lblUndo");
                    final String endTurn = Localizer.getInstance().getMessage("lblEndTurn");
                    final boolean actualPriorityButtons = Localizer.getInstance().getMessage("lblOK")
                            .equals(prompt.getBtnOK().getText())
                            && (prompt.getBtnCancel().getText().startsWith(undo)
                                    || endTurn.equals(prompt.getBtnCancel().getText()));
                    lastGate.set(lastGate.get() + "|PRIORITY_BUTTONS=" + actualPriorityButtons
                            + "|OK_SHOWING=" + prompt.getBtnOK().isShowing()
                            + "|OK_ENABLED=" + prompt.getBtnOK().isEnabled()
                            + "|CANCEL_SHOWING=" + prompt.getBtnCancel().isShowing()
                            + "|CANCEL_ENABLED=" + prompt.getBtnCancel().isEnabled()
                            + "|MESSAGE=" + !ui.getPromptMessage().isBlank()
                            + "|PROMPT_BOUND=" + (prompt.getLayoutControl().getMatchUI() == ui));
                    if (!actualPriorityButtons || !prompt.getBtnOK().isShowing() || !prompt.getBtnOK().isEnabled()
                            || !prompt.getBtnCancel().isShowing() || !prompt.getBtnCancel().isEnabled()
                            || ui.getPromptMessage().isBlank()) return;
                    if (prompt.getLayoutControl().getMatchUI() == ui
                            && ui.getCurrentPlayer() == local && ui.getGameView() == view
                            && ui.getGameController(local) == candidate) {
                        candidate.selectButtonOk();
                        passed.set(true);
                    }
                });
                if (passed.get()) return true;
                Thread.sleep(20);
            }
            throw new AssertionError("remote stack priority deadline: " + lastGate.get());
        }

        private static boolean passInverseRemoteStackPriorityOnce(final CMatchUI ui,
                final int expectedGameViewId, final Tracker expectedGameTracker,
                final AtomicReference<String> failedGate, final Integer expectedSourceId,
                final String expectedSourceName) throws InterruptedException {
            final AtomicReference<Boolean> passed = new AtomicReference<>(false);
            FThreads.invokeInEdtAndWait(() -> {
                final PlayerView local = ui.getCurrentPlayer();
                final GameView view = ui.getGameView();
                final IGameController candidate = local == null ? null : ui.getGameController(local);
                final IVDoc<?> promptDoc = EDocID.REPORT_MESSAGE.getDoc();
                final StackItemView top = view == null ? null : view.peekStack();
                if (local == null) { failedGate.set("NO_LOCAL"); return; }
                if (!(candidate instanceof NetGameController)) { failedGate.set("NOT_NET_CONTROLLER"); return; }
                if (view == null) { failedGate.set("NO_VIEW"); return; }
                if (view.getId() != expectedGameViewId || view.getTracker() != expectedGameTracker) {
                    failedGate.set("VIEW_BINDING"); return;
                }
                if (!ui.isCurrentScreen()) { failedGate.set("NOT_CURRENT_SCREEN"); return; }
                if (!local.getHasPriority()) { failedGate.set("NO_LOCAL_PRIORITY"); return; }
                if (top == null) { failedGate.set("NO_TOP"); return; }
                if (top.getTracker() != view.getTracker() || top.getSourceCard() == null
                        || top.getSourceCard().getTracker() != view.getTracker()) {
                    failedGate.set("STALE_TOP"); return;
                }
                if (!expectedSourceName.equals(top.getSourceCard().getName())
                        || expectedSourceId == null || top.getSourceCard().getId() != expectedSourceId) {
                    failedGate.set("WRONG_SOURCE"); return;
                }
                if (!local.equals(top.getActivatingPlayer())) { failedGate.set("NOT_LOCAL_ACTIVATOR"); return; }
                if (!(promptDoc instanceof VPrompt prompt)) { failedGate.set("NO_PROMPT"); return; }
                final DragCell cell = prompt.getParentCell();
                final String undo = Localizer.getInstance().getMessage("lblUndo");
                final String endTurn = Localizer.getInstance().getMessage("lblEndTurn");
                final boolean actualPriorityButtons = Localizer.getInstance().getMessage("lblOK")
                        .equals(prompt.getBtnOK().getText())
                        && (prompt.getBtnCancel().getText().startsWith(undo)
                                || endTurn.equals(prompt.getBtnCancel().getText()));
                if (!actualPriorityButtons) { failedGate.set("NOT_PRIORITY_BUTTONS"); return; }
                if (prompt.getLayoutControl().getMatchUI() != ui) { failedGate.set("PROMPT_BINDING"); return; }
                if (cell == null || cell.getSelected() != prompt) { failedGate.set("PROMPT_NOT_SELECTED"); return; }
                if (!cell.getBody().isShowing()) { failedGate.set("PROMPT_BODY_HIDDEN"); return; }
                if (!prompt.getBtnOK().isShowing() || !prompt.getBtnCancel().isShowing()) {
                    failedGate.set("PROMPT_BUTTON_HIDDEN"); return;
                }
                if (!prompt.getBtnOK().isEnabled() || !prompt.getBtnCancel().isEnabled()) {
                    failedGate.set("PROMPT_BUTTON_DISABLED"); return;
                }
                if (ui.getPromptMessage() == null || ui.getPromptMessage().isBlank()) {
                    failedGate.set("BLANK_PROMPT"); return;
                }
                if (ui.getCurrentPlayer() == local && ui.getGameView() == view
                        && ui.getGameController(local) == candidate && view.peekStack() == top) {
                    candidate.selectButtonOk();
                    passed.set(true);
                    failedGate.set("PASS_DISPATCHED");
                } else {
                    failedGate.set("BINDING_CHANGED");
                }
            });
            return passed.get();
        }

        private static void waitForTellingTimeDialogClose(final Window dialog) throws InterruptedException {
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (System.nanoTime() < deadline) {
                final AtomicReference<Boolean> showing = new AtomicReference<>();
                FThreads.invokeInEdtAndWait(() -> showing.set(dialog.isShowing()));
                if (!showing.get()) return;
                Thread.sleep(10);
            }
            throw new AssertionError("actual order dialog did not close after its enabled OK action");
        }

        private static Window automateTellingTimeOrderDialog(final TellingTimeSetup setup, final int ordinal) {
            final AtomicReference<Window> acted = new AtomicReference<>();
            FThreads.invokeInEdtAndWait(() -> {
                for (Window window : Window.getWindows()) {
                    if (!window.isShowing() || !"forge.gui.DualListBox".equals(window.getClass().getName())) continue;
                    final List<JList<?>> lists = new ArrayList<>();
                    final List<AbstractButton> buttons = new ArrayList<>();
                    collectChoiceListsAndButtons(window, lists, buttons);
                    if (lists.size() != 2) throw new AssertionError("actual order dialog did not expose two choice lists");
                    final JList<?> source = lists.get(0);
                    final int expectedRows = ordinal == 0 ? 3 : 2;
                    if (source.getModel().getSize() != expectedRows) return;
                    int desiredIndex = -1;
                    final int desiredId = ordinal == 0 ? setup.privateOneId() : setup.privateTwoId();
                    for (int index = 0; index < source.getModel().getSize(); index++) {
                        final Object row = source.getModel().getElementAt(index);
                        if (row instanceof CardView cardView && cardView.getId() == desiredId) desiredIndex = index;
                    }
                    if (desiredIndex < 0) throw new AssertionError("expected role was not a row in the actual order dialog");
                    source.setSelectedIndex(desiredIndex);
                    final JList<?> destination = lists.get(1);
                    final AbstractButton add = buttons.stream().filter(button -> ">".equals(button.getText()))
                            .findFirst().orElseThrow(() -> new AssertionError("actual order dialog has no add choice"));
                    if (!add.isShowing() || !add.isEnabled()) {
                        throw new AssertionError("actual order dialog did not enable its move action");
                    }
                    add.doClick(0);
                    if (source.getModel().getSize() != expectedRows - 1
                            || destination.getModel().getSize() != 1) {
                        throw new AssertionError("actual order dialog did not move exactly one selected row");
                    }
                    final String okLabel = Localizer.getInstance().getMessage("lblOK");
                    final AbstractButton ok = buttons.stream().filter(button -> okLabel.equals(button.getText()))
                            .findFirst().orElseThrow(() -> new AssertionError("actual order dialog has no OK choice"));
                    if (!ok.isShowing() || !ok.isEnabled()) throw new AssertionError("actual order dialog did not enable its required selection");
                    ok.doClick(0);
                    acted.set(window);
                    return;
                }
            });
            return acted.get();
        }

        private static void collectChoiceListsAndButtons(final Component component,
                final List<JList<?>> lists, final List<AbstractButton> buttons) {
            if (component instanceof JList<?> list) lists.add(list);
            if (component instanceof AbstractButton button) buttons.add(button);
            if (component instanceof Container container) {
                for (Component child : container.getComponents()) collectChoiceListsAndButtons(child, lists, buttons);
            }
        }

        private static String inspectActualReportLog(final CMatchUI ui, final PrivateLogRoles roles,
                final int expectedGameViewId, final Tracker expectedGameTracker)
                throws InterruptedException {
            selectAndRefreshActualReportLog(ui, expectedGameViewId, expectedGameTracker);
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(55);
            String logText = "";
            boolean publicCast = false;
            boolean publicResolve = false;
            boolean completed = false;
            while (System.nanoTime() < deadline) {
                final AtomicReference<List<GameLogEntry>> entries = new AtomicReference<>(List.of());
                final AtomicReference<Boolean> emptyStack = new AtomicReference<>(false);
                final AtomicReference<Integer> viewId = new AtomicReference<>();
                final AtomicReference<Boolean> sameTracker = new AtomicReference<>(false);
                final AtomicReference<String> rendered = new AtomicReference<>("");
                FThreads.invokeInEdtAndWait(() -> {
                    final GameView view = ui.getGameView();
                    viewId.set(view == null ? null : view.getId());
                    sameTracker.set(view != null && view.getTracker() == expectedGameTracker);
                    emptyStack.set(view != null && view.getStack().isEmpty());
                    if (view != null && view.getGameLog() != null) {
                        entries.set(view.getGameLog().getAllEntries());
                    }
                    final IVDoc<?> reportLog = EDocID.REPORT_LOG.getDoc();
                    final List<JTextArea> textAreas = new ArrayList<>();
                    final GameLogPanel renderedPanel = actualRenderedGameLogPanel(reportLog, ui);
                    if (renderedPanel != null) {
                        collectTextAreas(renderedPanel, textAreas);
                    }
                    rendered.set(textAreas.stream().map(JTextArea::getText).reduce("", (a, b) -> a + "\n" + b));
                });
                final boolean sameAppliedGame = viewId.get() != null
                        && expectedGameViewId == viewId.get().intValue() && sameTracker.get();
                logText = rendered.get();
                final List<GameLogEntry> spellEntries = sameAppliedGame ? entries.get().stream()
                        .filter(entry -> entry.sourceCard() != null
                                && "Telling Time".equals(entry.sourceCard().getName())).toList() : List.of();
                final List<GameLogEntry> castEntries = spellEntries.stream()
                        .filter(entry -> entry.type() == GameLogEntryType.STACK_ADD).toList();
                final List<GameLogEntry> resolveEntries = spellEntries.stream()
                        .filter(entry -> entry.type() == GameLogEntryType.STACK_RESOLVE).toList();
                publicCast = castEntries.size() == 1 && logText.contains(castEntries.get(0).message());
                publicResolve = resolveEntries.size() == 1 && logText.contains(resolveEntries.get(0).message());
                if (sameAppliedGame && emptyStack.get() && publicCast && publicResolve
                        && logText.contains("Telling Time")) {
                    completed = true;
                    break;
                }
                Thread.sleep(20);
            }
            final boolean publicLog = completed && logText.contains("Telling Time") && publicCast && publicResolve;
            final boolean privateAbsent = !logText.contains(roles.privateOneName())
                    && !logText.contains(roles.privateTwoName()) && !logText.contains(roles.privateThreeName());
            return "TT_LOG|REMOTE|PUBLIC_LOG=" + (publicLog ? 1 : 0) + "|PUBLIC_COMPLETE="
                    + (completed ? 1 : 0) + "|PUBLIC_CAST="
                    + (publicCast ? 1 : 0) + "|PUBLIC_RESOLVE=" + (publicResolve ? 1 : 0)
                    + "|PRIVATE_ABSENT=" + (privateAbsent ? 1 : 0);
        }

        private static String actualRenderedReportLogText(final CMatchUI ui, final GameView expectedGameView) {
            selectAndRefreshActualReportLog(ui, expectedGameView.getId(), expectedGameView.getTracker());
            final AtomicReference<String> result = new AtomicReference<>("");
            FThreads.invokeInEdtAndWait(() -> {
                final IVDoc<?> reportLog = EDocID.REPORT_LOG.getDoc();
                Assert.assertTrue(reportLog instanceof VLog,
                        "the caster reads its actual rendered report-log view");
                Assert.assertSame(((VLog) reportLog).getLayoutControl().getMatchUI(), ui);
                final GameLogPanel renderedPanel = actualRenderedGameLogPanel(reportLog, ui);
                Assert.assertNotNull(renderedPanel,
                        "the selected cell body contains its actual showing game-log consumer");
                Assert.assertEquals(ui.getGameView().getId(), expectedGameView.getId());
                Assert.assertSame(ui.getGameView().getTracker(), expectedGameView.getTracker());
                final List<JTextArea> textAreas = new ArrayList<>();
                collectTextAreas(renderedPanel, textAreas);
                result.set(textAreas.stream().map(JTextArea::getText).reduce("", (a, b) -> a + "\n" + b));
            });
            return result.get();
        }

        private static List<String> captureInverseRenderedHistoryRows(final CMatchUI ui,
                final int expectedGameViewId, final int expectedOccurrences) {
            final AtomicReference<GameView> currentView = new AtomicReference<>();
            FThreads.invokeInEdtAndWait(() -> {
                final GameView view = ui.getGameView();
                Assert.assertNotNull(view, "the current rendered Game N view exists before Continue");
                Assert.assertEquals(view.getId(), expectedGameViewId,
                        "retained rendered history belongs to the current Game N view");
                currentView.set(view);
            });
            final GameView view = currentView.get();
            final String rendered = actualRenderedReportLogText(ui, view);
            final AtomicReference<List<String>> rows = new AtomicReference<>(List.of());
            FThreads.invokeInEdtAndWait(() -> {
                Assert.assertSame(ui.getGameView(), view,
                        "the same current Game N view remains installed while its log is sampled");
                final List<GameLogEntry> publicRows = view.getGameLog().getAllEntries().stream()
                        .filter(entry -> entry.sourceCard() != null
                                && ("Telling Time".equals(entry.sourceCard().getName())
                                        || "Haunted Fengraf".equals(entry.sourceCard().getName())
                                        || "Day's Undoing".equals(entry.sourceCard().getName()))
                                && (entry.type() == GameLogEntryType.STACK_ADD
                                        || entry.type() == GameLogEntryType.STACK_RESOLVE))
                        .toList();
                for (String cardName : List.of("Telling Time", "Haunted Fengraf", "Day's Undoing")) {
                    for (GameLogEntryType type : List.of(GameLogEntryType.STACK_ADD,
                            GameLogEntryType.STACK_RESOLVE)) {
                        final long count = publicRows.stream().filter(entry -> cardName.equals(entry.sourceCard().getName())
                                && entry.type() == type).count();
                        Assert.assertEquals(count, (long) expectedOccurrences,
                                "Game N retains the expected public replay multiplicity for each spell row");
                    }
                }
                final List<String> distinctRows = publicRows.stream().map(GameLogEntry::message).distinct().toList();
                Assert.assertEquals(distinctRows.size(), 6,
                        "the cast and resolution rows for all three public spells have six distinct messages");
                Assert.assertTrue(distinctRows.stream().allMatch(message -> message != null && !message.isBlank()));
                rows.set(distinctRows);
            });
            Assert.assertTrue(rows.get().stream().allMatch(row -> countOccurrences(rendered, row)
                            == expectedOccurrences),
                    "all six Game N public rows retain their role-specific rendered multiplicity");
            return rows.get();
        }

        private static void assertInverseRenderedHistoryReset(final CMatchUI ui, final int expectedGameViewId,
                final List<String> previousRenderedRows) {
            Assert.assertEquals(previousRenderedRows.size(), 6,
                    "six verified public Game N rows were retained before Continue");
            final AtomicReference<GameView> currentView = new AtomicReference<>();
            final AtomicReference<String> currentTurnRow = new AtomicReference<>();
            FThreads.invokeInEdtAndWait(() -> {
                final GameView view = ui.getGameView();
                Assert.assertNotNull(view, "the same rendered UI has an applied N+1 GameView");
                Assert.assertEquals(view.getId(), expectedGameViewId,
                        "the rendered reset check uses the newly applied N+1 view");
                final String turnRow = view.getGameLog().getAllEntries().stream()
                        .filter(entry -> entry.type() == GameLogEntryType.TURN && !entry.message().isBlank())
                        .map(GameLogEntry::message).findFirst().orElse(null);
                Assert.assertNotNull(turnRow,
                        "the current N+1 GameView has an actual nonblank turn row at MAIN1");
                currentTurnRow.set(turnRow);
                currentView.set(view);
            });
            final GameView view = currentView.get();
            final String rendered = actualRenderedReportLogText(ui, view);
            Assert.assertTrue(rendered.contains(currentTurnRow.get()),
                    "the actual N+1 rendered log contains its current turn row");
            final long oldRowsStillRendered = previousRenderedRows.stream()
                    .filter(row -> countOccurrences(rendered, row) > 0).count();
            Assert.assertEquals(oldRowsStillRendered, 0L,
                    "the current rendered N+1 log does not retain any public Game N Telling Time, Fengraf, or Day row");
        }

        private static void selectAndRefreshActualReportLog(final CMatchUI ui, final int expectedGameViewId,
                final Tracker expectedGameTracker) {
            FThreads.invokeInEdtAndWait(() -> {
                Assert.assertNotNull(expectedGameTracker,
                        "the fixture retains the actual applied GameView tracker for rendered log proof");
                final GameView view = ui.getGameView();
                Assert.assertNotNull(view, "the client retains its actual applied GameView for rendered log proof");
                Assert.assertEquals(view.getId(), expectedGameViewId);
                Assert.assertSame(view.getTracker(), expectedGameTracker);
                final IVDoc<?> reportLog = EDocID.REPORT_LOG.getDoc();
                Assert.assertTrue(reportLog instanceof VLog,
                        "the ordinary client registers its actual rendered report log");
                Assert.assertNotNull(reportLog.getParentCell());
                Assert.assertSame(((VLog) reportLog).getLayoutControl().getMatchUI(), ui);
                SDisplayUtil.showTab(reportLog);
                Assert.assertSame(reportLog.getParentCell().getSelected(), reportLog);
                ui.refreshLog();
                Assert.assertNotNull(actualRenderedGameLogPanel(reportLog, ui),
                        "refresh attaches the selected log's actual consumer to its showing cell body");
            });
        }

        private static GameLogPanel actualRenderedGameLogPanel(final IVDoc<?> reportLog, final CMatchUI ui) {
            if (!(reportLog instanceof VLog log) || log.getLayoutControl().getMatchUI() != ui) return null;
            final DragCell cell = reportLog.getParentCell();
            if (cell == null || cell.getSelected() != reportLog || !cell.getBody().isShowing()) return null;
            final List<GameLogPanel> panels = new ArrayList<>();
            collectGameLogPanels(cell.getBody(), panels);
            return panels.size() == 1 && panels.get(0).isShowing() ? panels.get(0) : null;
        }

        private static void collectGameLogPanels(final Container parent, final List<GameLogPanel> result) {
            for (Component child : parent.getComponents()) {
                if (child instanceof GameLogPanel gameLogPanel) result.add(gameLogPanel);
                else if (child instanceof Container container) collectGameLogPanels(container, result);
            }
        }

        private static void collectTextAreas(final Container parent, final List<JTextArea> result) {
            for (Component child : parent.getComponents()) {
                if (child instanceof JTextArea area) result.add(area);
                else if (child instanceof Container container) collectTextAreas(container, result);
            }
        }

        private static final class UiDesktop extends GuiDesktop {
            private final AtomicReference<CMatchUI> matchUi = new AtomicReference<>();
            private final AtomicReference<HostedMatch> hostedMatch = new AtomicReference<>();
            private final AtomicReference<Game> firstGame = new AtomicReference<>();
            private final AtomicInteger gameReturnCount = new AtomicInteger();
            private final CountDownLatch gameReturned = new CountDownLatch(1);
            private final CountDownLatch nextGameReturned = new CountDownLatch(1);
            private final AtomicReference<Game> returnedFirstGame = new AtomicReference<>();
            private final AtomicReference<Game> returnedNextGame = new AtomicReference<>();
            private final CountDownLatch matchEnded = new CountDownLatch(1);
            private final AtomicReference<Throwable> matchOverFailure = new AtomicReference<>();
            @Override public IGuiGame getNewGuiGame() {
                final AtomicReference<CMatchUI> ui = new AtomicReference<>();
                FThreads.invokeInEdtAndWait(() -> ui.set(new CMatchUI()));
                matchUi.set(ui.get());
                return ui.get();
            }
            @Override public HostedMatch hostMatch() {
                HostedMatch match = new ObservedHostedMatch(matchEnded, matchOverFailure);
                if (renderedTellingTimeMode) {
                    match.setStartGameHook(() -> {
                        try {
                            final TellingTimeSetup setup = prepareRenderedTellingTime(
                                    match.getGame(), inverseRenderedTellingTimeMode);
                            TELLING_TIME_SETUP.set(setup);
                        } catch (Throwable failure) {
                            TELLING_TIME_START_HOOK_FAILURE.compareAndSet(null, failure);
                            if (failure instanceof RuntimeException runtimeFailure) throw runtimeFailure;
                            if (failure instanceof Error errorFailure) throw errorFailure;
                            throw new IllegalStateException("rendered Telling Time start hook failed", failure);
                        }
                    });
                }
                Singletons.getControl().addMatch(match);
                match.setEndGameHook(() -> {
                    final Game returnedGame = match.getGame();
                    if (returnedGame == null) {
                        matchOverFailure.compareAndSet(null,
                                new IllegalStateException("HostedMatch cleared its Game before the return hook"));
                        return;
                    }
                    final int returned = gameReturnCount.incrementAndGet();
                    if (returned == 1 && returnedFirstGame.compareAndSet(null, returnedGame)) {
                        gameReturned.countDown();
                    } else if (returned == 2 && returnedGame != returnedFirstGame.get()
                            && returnedNextGame.compareAndSet(null, returnedGame)) {
                        nextGameReturned.countDown();
                    } else {
                        matchOverFailure.compareAndSet(null,
                                new IllegalStateException("HostedMatch returned an unexpected game ordinal"));
                    }
                });
                hostedMatch.set(match);
                return match;
            }
        }

        private static final class DayShuffleObserver {
            private final Game game;
            private final int hostViewId;
            private final int remoteViewId;
            private final int daysUndoingId;
            private final String witnessName;
            private java.util.Random originalRandom;
            private volatile List<Integer> expectedSecondShuffle;
            private final AtomicReference<Throwable> witnessFailure = new AtomicReference<>();
            private final AtomicBoolean armed = new AtomicBoolean();
            private final AtomicInteger hostShuffles = new AtomicInteger();
            private final AtomicInteger remoteShuffles = new AtomicInteger();
            private final AtomicInteger unassignedShuffles = new AtomicInteger();
            private final AtomicInteger hostDayDraws = new AtomicInteger();
            private final AtomicInteger remoteDayDraws = new AtomicInteger();
            private final AtomicInteger hostTurnThreeDraws = new AtomicInteger();
            private final AtomicInteger unassignedDayDraws = new AtomicInteger();

            private DayShuffleObserver(final Game game, final int hostViewId, final int remoteViewId,
                    final int daysUndoingId, final String witnessName) {
                Assert.assertNotEquals(hostViewId, remoteViewId);
                this.game = game;
                this.hostViewId = hostViewId;
                this.remoteViewId = remoteViewId;
                this.daysUndoingId = daysUndoingId;
                this.witnessName = witnessName;
            }

            private synchronized void arm() {
                final Player remote = game.getPlayers().stream()
                        .filter(player -> player.getId() == remoteViewId).findFirst().orElseThrow();
                Assert.assertTrue(remote.getCardsIn(ZoneType.Hand).stream()
                        .noneMatch(card -> witnessName.equals(card.getName())),
                        "the remote did not acquire the reserved name before Day's Undoing");
                for (ZoneType zone : List.of(ZoneType.Battlefield, ZoneType.Graveyard, ZoneType.Exile)) {
                    Assert.assertTrue(game.getCardsIn(zone).stream()
                            .noneMatch(card -> witnessName.equals(card.getName())));
                }
                Assert.assertTrue(armed.compareAndSet(false, true));
            }

            private boolean isDayResolution() {
                if (game.getPhaseHandler().getTurn() != 2 || game.getPhaseHandler().getPhase() != PhaseType.MAIN1
                        || game.getStack().size() != 1) return false;
                final var ability = game.getStack().peekAbility();
                return ability != null && ability.getHostCard() != null
                        && ability.getHostCard().getId() == daysUndoingId
                        && ability.getActivatingPlayer() != null
                        && ability.getActivatingPlayer().getView().getId() == remoteViewId;
            }

            @Subscribe
            public synchronized void onGameShuffle(final GameEventShuffle event) {
                if (!armed.get()) return;
                if (!isDayResolution()) {
                    unassignedShuffles.incrementAndGet();
                    return;
                }
                final PlayerView player = event.player();
                if (player == null) unassignedShuffles.incrementAndGet();
                else if (player.getId() == hostViewId) hostShuffles.incrementAndGet();
                else if (player.getId() == remoteViewId) remoteShuffles.incrementAndGet();
                else unassignedShuffles.incrementAndGet();
                try {
                    if (total() == 1) {
                        Assert.assertEquals(hostShuffles.get(), 1);
                        // The first real shuffle has installed its list. ChangeZoneAllEffect
                        // next shuffles this same shared library for the remote player.
                        final List<Card> initial = new ArrayList<>(game.getPlayers().get(0)
                                .getCardsIn(ZoneType.Library));
                        Assert.assertTrue(initial.size() > 14);
                        long chosenSeed = -1;
                        for (long seed = 0; seed < 100000; seed++) {
                            final java.util.Random delegate = new java.util.Random(seed);
                            // Match MyRandom.CountingRandom, not Random.nextInt(bound).
                            final java.util.Random counted = new java.util.Random(0) {
                                @Override protected int next(int bits) {
                                    return delegate.nextInt() >>> (32 - bits);
                                }
                            };
                            final List<Card> trial = new ArrayList<>(initial);
                            java.util.Collections.shuffle(trial, counted);
                            // Both seven-card hands precede the host's separate T3 draw.
                            if (witnessName.equals(trial.get(14).getName())
                                    && trial.subList(0, 14).stream()
                                            .noneMatch(card -> witnessName.equals(card.getName()))) {
                                chosenSeed = seed;
                                expectedSecondShuffle = trial.stream().map(Card::getId).toList();
                                break;
                            }
                        }
                        Assert.assertTrue(chosenSeed >= 0, "bounded search found a valid Day shuffle witness");
                        originalRandom = forge.util.MyRandom.getRandom();
                        forge.util.MyRandom.setRandom(new java.util.Random(chosenSeed));
                    } else if (total() == 2) {
                        Assert.assertEquals(hostShuffles.get(), 1);
                        Assert.assertEquals(remoteShuffles.get(), 1);
                        Assert.assertNotNull(expectedSecondShuffle);
                        Assert.assertEquals(game.getPlayers().get(0).getCardsIn(ZoneType.Library)
                                .stream().map(Card::getId).toList(), expectedSecondShuffle,
                                "the actual second shuffle consumed the planned RNG sequence");
                        restoreRandom();
                    }
                } catch (Throwable failure) {
                    // EventBus may swallow subscriber failures; retain one for the test.
                    witnessFailure.compareAndSet(null, failure);
                    restoreRandom();
                }
            }

            @Subscribe
            public void onGameCardChangeZone(final GameEventCardChangeZone event) {
                if (!armed.get() || event.from() == null || event.to() == null
                        || event.from().zoneType() != ZoneType.Library || event.to().zoneType() != ZoneType.Hand) {
                    return;
                }
                if (event.card() == null || event.to().player() == null) {
                    unassignedDayDraws.incrementAndGet();
                    return;
                }
                final int recipientViewId = event.to().player().getId();
                if (isDayResolution()) {
                    if (recipientViewId == hostViewId) hostDayDraws.incrementAndGet();
                    else if (recipientViewId == remoteViewId) remoteDayDraws.incrementAndGet();
                    else unassignedDayDraws.incrementAndGet();
                } else if (game.getPhaseHandler().getTurn() == 3
                        && game.getPhaseHandler().getPhase() == PhaseType.DRAW && recipientViewId == hostViewId) {
                    hostTurnThreeDraws.incrementAndGet();
                } else {
                    unassignedDayDraws.incrementAndGet();
                }
            }

            private synchronized void restoreRandom() {
                if (originalRandom != null) {
                    forge.util.MyRandom.setRandom(originalRandom);
                    originalRandom = null;
                }
            }
            private synchronized void disarm() { armed.set(false); restoreRandom(); }
            private int total() { return hostShuffles.get() + remoteShuffles.get() + unassignedShuffles.get(); }
        }

        private record TellingTimeSetup(Game game, Card tellingTime, Card daysUndoing, Player host, Player remote,
                int privateOneId, int privateTwoId, int privateThreeId,
                String privateOneName, String privateTwoName, String privateThreeName,
                String historySentinelName, List<Card> manaSources, int fengrafId, int fengrafCreatureId,
                List<Card> fengrafManaSources, Set<Integer> originalPhysicalIds, PlayerControllerHuman controller) {
            private TellingTimeSetup withController(final PlayerControllerHuman value) {
                return new TellingTimeSetup(game, tellingTime, daysUndoing, host, remote, privateOneId, privateTwoId,
                        privateThreeId, privateOneName, privateTwoName, privateThreeName, historySentinelName,
                        manaSources, fengrafId, fengrafCreatureId, fengrafManaSources, originalPhysicalIds, value);
            }
        }

        private record PrivateLogRoles(String privateOneName, String privateTwoName, String privateThreeName,
                Set<Integer> tellingTimeManaIds, int fengrafId, int fengrafCreatureId,
                Set<Integer> fengrafManaIds, int daysUndoingId) {
            private PrivateLogRoles(final String privateOneName, final String privateTwoName,
                    final String privateThreeName) {
                this(privateOneName, privateTwoName, privateThreeName, Set.of(), -1, -1, Set.of(), -1);
            }
        }

        private static final class ObservedHostedMatch extends HostedMatch {
            private final CountDownLatch matchEnded;
            private final AtomicReference<Throwable> matchOverFailure;

            private ObservedHostedMatch(final CountDownLatch matchEnded,
                    final AtomicReference<Throwable> matchOverFailure) {
                this.matchEnded = matchEnded;
                this.matchOverFailure = matchOverFailure;
            }

            @Override public void setOnMatchOver(final Runnable callback) {
                super.setOnMatchOver(() -> {
                    try {
                        if (callback != null) callback.run();
                    } catch (RuntimeException | Error failure) {
                        matchOverFailure.compareAndSet(null, failure);
                        throw failure;
                    } finally {
                        matchEnded.countDown();
                    }
                });
            }
        }

        private static final class AppliedSession {
            private final String receipt;
            private final String chooserReceipt;
            private final String continueReceipt;
            private final Cleanup outcome;
            private final Cleanup cleanup;
            private final Cleanup abort;
            private final Continuation continuation;
            private final java.util.concurrent.atomic.AtomicBoolean closed = new java.util.concurrent.atomic.AtomicBoolean();
            private AppliedSession(final String receipt, final Cleanup outcome, final Cleanup cleanup,
                    final Cleanup abort, final Continuation continuation, final String continueReceipt,
                    final String chooserReceipt) {
                this.receipt = receipt;
                this.outcome = outcome;
                this.cleanup = cleanup;
                this.abort = abort;
                this.continuation = continuation;
                this.continueReceipt = continueReceipt;
                this.chooserReceipt = chooserReceipt;
            }
            private void awaitOutcome() throws Exception { outcome.run(); }
            private AppliedSession continueToNextGame() throws Exception {
                if (continuation == null) throw new IllegalStateException("session has no N+1 UI continuation");
                return continuation.run();
            }
            private void finish() throws Exception { if (closed.compareAndSet(false, true)) cleanup.run(); }
            private void abort() throws Exception { if (closed.compareAndSet(false, true)) abort.run(); }
        }

        @FunctionalInterface
        private interface Cleanup { void run() throws Exception; }

        @FunctionalInterface
        private interface Continuation { AppliedSession run() throws Exception; }

        private enum ChoiceKind { START, FREE_MULLIGAN, REGULAR_MULLIGAN, NEXT_GAME }
        private record Choice(ChoiceKind kind, AbstractButton button) { }
        private record InputObservation(Input input, String kind, int ownerSeat) {
            private static InputObservation unavailable() { return new InputObservation(null, "UNAVAILABLE", -1); }
        }
        private record ChoiceAction(ChoiceKind kind, boolean clicked, String inputBefore, int inputOwnerBefore,
                String inputAfter, int inputOwnerAfter, boolean inputChanged) { }
    }
}
