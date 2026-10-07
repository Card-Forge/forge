package forge.screens.match;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;

import forge.Forge;
import forge.game.PlanarDice;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;

/**
 * Plays queued 3D dice animations on the mobile match screen.
 *
 * Wiring:
 *   1. Once at startup (Forge.create()):
 *          DiceEventBridge.instance.setListener(DiceOverlay.get()::accept);
 *   2. In Forge.render(), classic path:
 *          DiceOverlay dice = DiceOverlay.get();
 *          dice.update(delta);                 // BEFORE Classic.getInstance().render(screen)
 *          Classic.getInstance().render(screen);
 *          dice.draw();                        // AFTER it, so the die is on top (also above dialogs)
 *   3. Optional: skipAll() on tap.
 *
 * accept() runs on the game thread and returns a latch; the bridge holds the game thread on it
 * until the die has landed, so dialogs/messages that follow the roll show up after the animation.
 */
public class DiceOverlay {
    private static final String ATLAS = ForgeConstants.RES_DIR + "skins/default/dice/sprite_dice.atlas";
    private static final int MAX_QUEUE = 4;      // extra rolls beyond this are skipped
    private static final float ROLL_TIME = 1.2f; // seconds of tumbling (plus Dice3D hold time)

    private static final class Pending {
        final int result;
        final PlanarDice planar;
        final CountDownLatch latch = new CountDownLatch(1);
        Pending(int result, PlanarDice planar) {
            this.result = result;
            this.planar = planar;
        }
    }

    // Render thread only:
    private final Queue<Pending> queue = new ArrayDeque<>();
    private Pending current;
    private Dice3D die;
    private SpriteBatch batch;

    private volatile Thread renderThread;

    private static DiceOverlay instance;

    public static synchronized DiceOverlay get() {
        if (instance == null) {
            instance = new DiceOverlay();
        }
        return instance;
    }

    // game thread

    /** Matches DiceEventBridge.Listener.onDiceRoll. */
    public CountDownLatch accept(int sides, int result, PlanarDice planar) {
        if (!FModel.getPreferences().getPrefBoolean(FPref.UI_DICE_ANIMATION)) {
            return null; // setting off = old behaviour
        }
        if (Thread.currentThread() == renderThread) {
            return null; // never block the render thread
        }
        final Pending p;
        if (planar != null) {
            p = new Pending(0, planar);
        } else if (sides == 6 && result >= 1 && result <= 6) {
            p = new Pending(result, null);
        } else {
            return null; // d20 and other sizes: later
        }
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

    private TextureAtlas atlas() {
        AssetManager am = Forge.getAssets().manager();
        if (!am.isLoaded(ATLAS, TextureAtlas.class)) {
            am.load(ATLAS, TextureAtlas.class);
            am.finishLoadingAsset(ATLAS);
            for (Texture t : am.get(ATLAS, TextureAtlas.class).getTextures()) {
                t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            }
        }
        return am.get(ATLAS, TextureAtlas.class);
    }

    private void startNext() {
        Pending p = queue.poll();
        if (p == null) {
            return;
        }
        current = p;
        try {
            TextureAtlas a = atlas();
            if (p.planar != null) {
                TextureRegion walk = a.findRegion("planar_walk");
                TextureRegion chaos = a.findRegion("planar_chaos");
                TextureRegion blank = a.findRegion("planar_blank");
                die = new Dice3D(new TextureRegion[] { walk, chaos, blank, blank, blank, blank });
                int face;
                switch (p.planar) {
                    case Planeswalk: face = 0; break;
                    case Chaos:      face = 1; break;
                    default:         face = 2 + MathUtils.random(3); break; // one of the 4 blank faces
                }
                die.roll(face, ROLL_TIME);
            } else {
                TextureRegion[] faces = new TextureRegion[6];
                for (int v = 1; v <= 6; v++) {
                    faces[Dice3D.D6_FACE[v - 1]] = a.findRegion("d6_" + v);
                }
                die = new Dice3D(faces);
                die.roll(Dice3D.D6_FACE[p.result - 1], ROLL_TIME);
            }
        } catch (Exception e) {
            Gdx.app.error("DiceOverlay", "failed to start dice animation", e);
            finishCurrent();
            releaseQueue();
        }
    }

    private void finishCurrent() {
        if (die != null) {
            die.dispose();
            die = null;
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
        if (die == null) {
            startNext();
            if (die == null) {
                return;
            }
        }
        die.update(dt);
        if (die.isDone()) {
            finishCurrent();
        }
    }

    /** Draws the die centred on screen with its own SpriteBatch. Call after the normal frame. */
    public void draw() {
        if (die == null) {
            return;
        }
        if (batch == null) {
            batch = new SpriteBatch();
        }
        float w = Gdx.graphics.getBackBufferWidth();
        float h = Gdx.graphics.getBackBufferHeight();
        float size = Math.min(w, h) * 0.5f;
        batch.getProjectionMatrix().setToOrtho2D(0, 0, w, h);
        batch.begin();
        batch.setColor(1f, 1f, 1f, die.getAlpha());
        batch.draw(die.getRegion(), (w - size) / 2f, (h - size) / 2f, size, size);
        batch.setColor(Color.WHITE);
        batch.end();
    }

    public boolean isActive() {
        return die != null || !queue.isEmpty();
    }

    /** Wire this to a tap to skip whatever is playing (the game continues immediately). */
    public void skipAll() {
        releaseQueue();
        if (die != null) {
            die.skip();
        }
    }

    public void dispose() {
        releaseQueue();
        finishCurrent();
        if (batch != null) {
            batch.dispose();
            batch = null;
        }
    }
}