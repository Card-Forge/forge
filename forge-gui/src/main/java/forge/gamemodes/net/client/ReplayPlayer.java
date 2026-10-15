package forge.gamemodes.net.client;

import forge.game.player.PlayerView;
import forge.gamemodes.match.AbstractGuiGame;
import forge.gamemodes.net.CompatibleObjectDecoder;
import forge.gamemodes.net.ProtocolMethod;
import forge.gamemodes.net.event.GuiGameEvent;
import forge.gamemodes.replay.ReplayReader;
import forge.gui.FThreads;
import forge.gui.interfaces.IGuiGame;
import forge.util.IHasForgeLog;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.serialization.ClassResolvers;

import java.io.File;
import java.io.IOException;

/**
 * Plays a recorded game on a match screen.
 *
 * <p>A recording holds the messages a remote spectator would have received. They are fed through the same decoder and
 * client handler a netplay client uses, so the existing match screens show the game with no replay-specific drawing:
 * the player is the "server" for a client that will never answer back. The pace follows the recorded timing, with long
 * pauses (someone thinking) shortened, and the Pause and Speed controls live on the prompt's two buttons.
 *
 * <p>One daemon thread paces the frames and sleeps while idle or paused; nothing polls.
 */
public final class ReplayPlayer implements Runnable, IHasForgeLog {
    private static final int[] SPEEDS = {1, 2, 4, 8};
    /** Longest a quiet stretch of the recording is shown for, before the speed is applied. */
    private static final int MAX_GAP_MS = 1500;
    private static final int MAX_FRAME_BYTES = 64 * 1024 * 1024;

    private static volatile ReplayPlayer active;

    private final IGuiGame gui;
    private final ReplayReader reader;
    private final ReplayClient client;
    private final EmbeddedChannel channel;
    private final Object lock = new Object();

    private boolean paused;
    private boolean stopped;
    private boolean finished;
    private int speedIndex;
    private volatile boolean controlsShown;
    /** Set once the first batch of game events has gone through: the opening position is on screen. */
    private volatile boolean setupDone;

    private ReplayPlayer(final ReplayReader reader0, final IGuiGame gui0) {
        reader = reader0;
        gui = gui0;
        client = new ReplayClient(reader0.getTitle(), gui0, this);
        channel = new EmbeddedChannel(new CompatibleObjectDecoder(MAX_FRAME_BYTES, ClassResolvers.cacheDisabled(null)));
        // added after the channel is live, so the handler's "connected" step (which logs in to a server) never runs
        channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
                if (msg instanceof GuiGameEvent e && e.getMethod() == ProtocolMethod.applyDelta) {
                    setupDone = true;
                }
                ctx.fireChannelRead(msg);
            }
        });
        channel.pipeline().addLast(new GameClientHandler(client));
        if (gui0 instanceof AbstractGuiGame g) {
            g.setReplay(true); // so the screens know this is a recording: no new match, a smaller menu
        }
    }

    /** Ends the replay shown on {@code gui}, if any; {@code closeScreen} also closes its match screen. */
    public static void stopFor(final IGuiGame gui, final boolean closeScreen) {
        final ReplayPlayer p = active;
        if (p != null && p.gui == gui) {
            p.stop(closeScreen);
        }
    }

    /** True while a replay is on screen, so a second one is not started over it. */
    public static boolean isPlaying() {
        return active != null;
    }

    /** Starts playing {@code file} on {@code gui}, a fresh match GUI. Returns as soon as playback has started. */
    public static void play(final File file, final IGuiGame gui) throws IOException {
        final ReplayReader reader = ReplayReader.open(file);
        final ReplayPlayer player = new ReplayPlayer(reader, gui);
        active = player;
        final Thread t = new Thread(player, "Forge-Replay");
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void run() {
        int lastMs = 0;
        try {
            while (reader.next()) {
                // The game start is not paced: the opening position (hands, board) appears right away, not after
                // however long the player took to keep their hand.
                final int gap = setupDone ? Math.min(Math.max(reader.getTimeMs() - lastMs, 0), MAX_GAP_MS) : 0;
                lastMs = reader.getTimeMs();
                if (!pause(gap)) {
                    return;
                }
                feed(reader.getPayload());
                showControlsOnce();
            }
            synchronized (lock) {
                finished = true;
            }
            updateControls();
        } catch (final RuntimeException e) {
            netLog.error(e, "Replay stopped");
            stop(true);
        }
    }

    private void feed(final byte[] payload) {
        if (!channel.isOpen()) {
            stop(true);
            return;
        }
        final ByteBuf frame = Unpooled.buffer(payload.length + 4, payload.length + 4);
        frame.writeInt(payload.length).writeBytes(payload);
        channel.writeInbound(frame);
    }

    /** Sleeps for a frame's gap at the current speed; false when the replay was stopped meanwhile. */
    private boolean pause(final int gapMs) {
        synchronized (lock) {
            long deadline = System.nanoTime() + gapMs * 1_000_000L / SPEEDS[speedIndex];
            try {
                while (!stopped) {
                    if (paused) {
                        final long pausedAt = System.nanoTime();
                        lock.wait();
                        deadline += System.nanoTime() - pausedAt;
                        continue;
                    }
                    final long left = deadline - System.nanoTime();
                    if (left <= 0) {
                        return true;
                    }
                    lock.wait(left / 1_000_000L, (int) (left % 1_000_000L));
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                stopped = true;
            }
            return false;
        }
    }

    /** A button on the match screen was pressed; called from the UI thread. */
    void onControl(final ProtocolMethod method) {
        switch (method) {
            case selectButtonOk:
                synchronized (lock) {
                    if (finished) {
                        break;
                    }
                    paused = !paused;
                    lock.notifyAll();
                }
                updateControls();
                return;
            case selectButtonCancel:
                synchronized (lock) {
                    if (finished) {
                        break;
                    }
                    speedIndex = (speedIndex + 1) % SPEEDS.length;
                    lock.notifyAll();
                }
                updateControls();
                return;
            default:
                break;
        }
        stop(true); // leaving: quit, concede, the match screen's own buttons, or a button once the replay is over
    }

    private void stop(final boolean closeScreen) {
        synchronized (lock) {
            if (stopped) {
                return;
            }
            stopped = true;
            lock.notifyAll();
        }
        active = null;
        try {
            reader.close();
        } catch (final IOException ignored) {
            // nothing useful left to do
        }
        channel.close();
        if (closeScreen) {
            FThreads.invokeInEdtNowOrLater(gui::afterGameEnd);
        }
    }

    private PlayerView viewer() {
        if (gui instanceof AbstractGuiGame g) {
            for (final PlayerView p : g.getLocalPlayers()) {
                return p;
            }
        }
        return null;
    }

    /** The first time the match screen exists, put the controls on the prompt. */
    private void showControlsOnce() {
        if (!controlsShown && viewer() != null) {
            controlsShown = true;
            updateControls();
        }
    }

    private void updateControls() {
        final String message;
        final String ok;
        final String cancel;
        final boolean cancelEnabled;
        synchronized (lock) {
            if (stopped || !controlsShown && !finished) {
                return;
            }
            if (finished) {
                message = "Replay finished.";
                ok = "Close";
                cancel = "";
                cancelEnabled = false;
            } else {
                message = (paused ? "Replay paused." : "Replaying") + " Speed " + SPEEDS[speedIndex] + "x.";
                ok = paused ? "Resume" : "Pause";
                cancel = "Speed " + SPEEDS[(speedIndex + 1) % SPEEDS.length] + "x";
                cancelEnabled = true;
            }
        }
        final PlayerView who = viewer();
        if (who == null) {
            return;
        }
        FThreads.invokeInEdtNowOrLater(() -> {
            gui.showPromptMessage(who, message);
            gui.updateButtons(who, ok, cancel, true, cancelEnabled, true);
        });
    }
}
