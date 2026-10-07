package forge.screens.match;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import com.google.common.eventbus.Subscribe;

import forge.Forge;
import forge.game.Game;
import forge.game.PlanarDice;
import forge.game.event.GameEventRollDice;
import forge.game.event.GameEventRollDie;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;

/**
 * Plays queued 3D dice animations on the mobile match screen.
 *
 * This class is its own game-event subscriber (the old DiceEventBridge in forge-gui is gone),
 * so nothing dice related lives outside forge-gui-mobile except the GameEventRollDie record.
 *
 * Wiring:
 *   1. When the mobile UI first gets hold of the Game for a match:
 *          DiceOverlay.getInstance().attach(game);          // safe to call repeatedly
 *   2. In Forge.render(), classic path:
 *          DiceOverlay dice = DiceOverlay.getInstance();
 *          dice.update(delta);                      // BEFORE Classic.getInstance().render(screen)
 *          Classic.getInstance().render(screen);
 *          DiceOverlay.getInstance().render();          // AFTER it, so the dice are on top
 *   3. Optional: skipAll() on tap.
 *
 * onRollDie() runs on the GAME thread (the event bus is synchronous) and holds it on a latch until
 * the die has landed, so dialogs/messages that follow the roll show up after the animation.
 *
 * Multiple dice from one roll arrive as a single GameEventRollDice and tumble together (max MAX_DICE shown).
 * Supported: d4..d20 and the planar die (d4 uses a tetrahedron, d5-d6 a cube, d7-d8 share the octahedron, d9-d10 the trapezohedron,
 * d11-d12 the dodecahedron, d13-d20 the icosahedron). Anything else only plays the sound.
 */
public class DiceOverlay implements Disposable {
    private static final String PLANAR_WALK = ForgeConstants.RES_DIR + "skins/default/planar_walk.png";
    private static final String PLANAR_CHAOS = ForgeConstants.RES_DIR + "skins/default/planar_chaos.png";
    private static final int MAX_QUEUE = 4;      // extra rolls beyond this are skipped
    private static final float ROLL_TIME = 1.2f; // seconds of tumbling (plus Dice3D hold time)
    /** Safety net so a stalled render thread (app in background, etc.) can never freeze the game. */
    private static final long MAX_WAIT_MS = 4500;

    private static final int MAX_DICE = 6;      // dice shown at once; the rest are skipped

    private static final class Pending {
        final int sides;
        final int[] results;   // empty for the planar die
        final PlanarDice planar;
        final CountDownLatch latch = new CountDownLatch(1);
        Pending(int sides, int[] results, PlanarDice planar) {
            this.sides = sides;
            this.results = results;
            this.planar = planar;
        }
    }

    // Render thread only:
    private final Queue<Pending> queue = new ArrayDeque<>();
    private Pending current;
    private final List<Dice3D> dice = new ArrayList<>();
    private Dice3D.Skin skin;   // shared by all dice of the current roll
    private int cols, rows;
    private float dieSize;      // on-screen size of one die in pixels
    private final Matrix4 savedProjection = new Matrix4();
    private final Matrix4 projection = new Matrix4();

    private volatile Thread renderThread;
    private WeakReference<Game> attached = new WeakReference<>(null);

    public static DiceOverlay instance;

    public static synchronized DiceOverlay getInstance() {
        if (instance == null) {
            instance = new DiceOverlay();
        }
        return instance;
    }

    /** Subscribes to the game's events once; repeated calls for the same game are ignored. */
    public synchronized void attach(Game game) {
        if (game == null || attached.get() == game) {
            return;
        }
        attached = new WeakReference<>(game);
        game.subscribeToEvents(this);
    }

    // game thread

    /** Planar die only; numeric dice come in through GameEventRollDice. */
    @Subscribe
    public void onRollDie(GameEventRollDie ev) {
        if (ev.planar() != null) {
            await(accept(6, new int[0], ev.planar()));
        }
    }

    @Subscribe
    public void onRollDice(GameEventRollDice ev) {
        int[] r = new int[ev.results().size()];
        for (int i = 0; i < r.length; i++) {
            r[i] = ev.results().get(i);
        }
        await(accept(ev.sides(), r, null));
    }

    private void await(CountDownLatch done) {
        if (done != null) {
            try {
                done.await(MAX_WAIT_MS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private CountDownLatch accept(int sides, int[] results, PlanarDice planar) {
        if (!FModel.getPreferences().getPrefBoolean(FPref.UI_DICE_ANIMATION)) {
            return null; // setting off = old behaviour
        }
        if (Thread.currentThread() == renderThread) {
            return null; // never block the render thread
        }
        if (planar == null) {
            if (results.length == 0 || !DiceShape.supportsNumbered(sides)) {
                return null; // unsupported size
            }
            for (int r : results) {
                if (r < 1 || r > sides) {
                    return null;
                }
            }
        }
        final Pending p = new Pending(sides, results, planar);
        Gdx.app.postRunnable(new Runnable() {
            @Override public void run() { enqueue(p); }
        });
        return p.latch;
    }

    // render thread

    private void enqueue(Pending p) {
        if (queue.size() < MAX_QUEUE) {
            queue.add(p);
        } else {
            p.latch.countDown();
        }
    }

    /** Loads (once, cached by the AssetManager) a PNG from the dice skin folder. */
    private Pixmap pixmap(String path) {
        AssetManager am = Forge.getAssets().manager();
        if (!am.isLoaded(path, Pixmap.class)) {
            am.load(path, Pixmap.class);
            am.finishLoadingAsset(path);
        }
        return am.get(path, Pixmap.class);
    }

    /** Grid layout + on-screen die size for n dice (render thread). */
    private void layout(int n) {
        float w = Gdx.graphics.getBackBufferWidth();
        float h = Gdx.graphics.getBackBufferHeight();
        cols = n <= 3 ? n : (n + 1) / 2;
        rows = (n + cols - 1) / cols;
        dieSize = Math.min(Math.min(w, h) * 0.5f, Math.min(w * 0.95f / cols, h * 0.45f / rows));
    }

    private void startNext() {
        Pending p = queue.poll();
        if (p == null) {
            return;
        }
        current = p;
        try {
            int n = p.planar != null ? 1 : Math.min(p.results.length, MAX_DICE);
            layout(n);
            // render a bit above the on-screen size (smoother edges), but never more than needed
            int fb = MathUtils.clamp(Math.round(dieSize * 1.3f), 256, 1024);
            int cell = fb >= 400 ? 256 : 128;

            if (p.planar != null) {
                skin = Dice3D.Skin.planar(pixmap(PLANAR_WALK), pixmap(PLANAR_CHAOS), cell);
                Dice3D d = new Dice3D(skin, fb);
                int face;
                switch (p.planar) {
                    case Planeswalk: face = 0; break;
                    case Chaos:      face = 1; break;
                    default:         face = 2 + MathUtils.random(3); break; // one of the 4 blank faces
                }
                d.roll(face, ROLL_TIME);
                dice.add(d);
                return;
            }
            skin = Dice3D.Skin.numbered(p.sides, cell);
            for (int i = 0; i < n; i++) {
                float time = ROLL_TIME + (n > 1 ? MathUtils.random(0f, 0.3f) : 0f);
                Dice3D d = new Dice3D(skin, fb);
                d.rollValue(p.results[i], time);
                dice.add(d);
            }
        } catch (Exception e) {
            Gdx.app.error("DiceOverlay", "failed to start dice animation", e);
            finishCurrent();
            releaseQueue();
        }
    }

    private void finishCurrent() {
        for (Dice3D d : dice) {
            d.dispose();
        }
        dice.clear();
        if (skin != null) {
            skin.dispose();
            skin = null;
        }
        if (current != null) {
            current.latch.countDown();
            current = null;
        }
    }

    private void releaseQueue() {
        Pending p;
        while ((p = queue.poll()) != null) {
            p.latch.countDown();
        }
    }

    /** Call every frame, outside of an open SpriteBatch. */
    public void update(float dt) {
        renderThread = Thread.currentThread();
        if (dice.isEmpty()) {
            startNext();
            if (dice.isEmpty()) {
                return;
            }
        }
        // the dice are rendered into FrameBuffers, which must not happen while Forge's batch is open
        final Batch fb = Forge.getGraphics().getBatch();
        final boolean wasDrawing = fb.isDrawing();
        if (wasDrawing) {
            fb.end();
        }
        boolean allDone = true;
        for (Dice3D d : dice) {
            d.update(dt);
            allDone &= d.isDone();
        }
        if (wasDrawing) {
            fb.begin();
        }
        if (allDone) {
            finishCurrent();
        }
    }

    /**
     * Draws the finished dice pictures in a centred grid through Forge's shared batch.
     * Call after Classic.render(screen). Works whether or not the batch is currently open.
     */
    public void render() {
        if (dice.isEmpty()) {
            return;
        }
        final Batch b = Forge.getGraphics().getBatch();
        final boolean wasDrawing = b.isDrawing();
        float w = Gdx.graphics.getBackBufferWidth();
        float h = Gdx.graphics.getBackBufferHeight();
        int n = dice.size();
        layout(n); // recomputed so a screen rotation mid-roll still looks right
        float size = dieSize;
        float top = (h + size * rows) / 2f - size; // bottom edge of the first row

        // borrow the batch: own pixel projection and colour, restored afterwards
        savedProjection.set(b.getProjectionMatrix());
        final float oldColor = b.getPackedColor();
        projection.setToOrtho2D(0, 0, w, h);
        b.setProjectionMatrix(projection);
        if (!wasDrawing) {
            b.begin();
        }
        for (int i = 0; i < n; i++) {
            Dice3D d = dice.get(i);
            int row = i / cols;
            int inRow = Math.min(cols, n - row * cols);
            float x = (w - size * inRow) / 2f + (i % cols) * size; // short rows are centred
            float y = top - row * size;
            b.setColor(1f, 1f, 1f, d.getAlpha());
            b.draw(d.getRegion(), x, y, size, size);
        }
        if (!wasDrawing) {
            b.end();
        }
        b.setPackedColor(oldColor);
        b.setProjectionMatrix(savedProjection);
    }

    public boolean isActive() {
        return !dice.isEmpty() || !queue.isEmpty();
    }

    /** Wire this to a tap to skip whatever is playing (the game continues immediately). */
    public void skipAll() {
        releaseQueue();
        for (Dice3D d : dice) {
            d.skip();
        }
    }

    @Override
    public void dispose() {
        releaseQueue();
        finishCurrent();
        Dice3D.releaseShared();
    }
}