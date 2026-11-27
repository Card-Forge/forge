package forge.screens.rogue;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Align;
import forge.Forge;
import forge.Graphics;
import forge.assets.FImage;
import forge.gui.FThreads;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FOverlay;
import forge.util.Localizer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * A parchment message with one or two answers, the last one the main action. Back gives the first answer. It springs
 * up when shown; a notice with an image can make more of its entrance.
 */
public class RogueDialog extends FOverlay {
    /** How an image arrives: RAYS turns light behind it with a shine and sparkles, DROP drops it onto a halo. */
    public enum Entrance { PLAIN, RAYS, DROP }

    private static final Color DIM = new Color(0, 0, 0, 0.6f);
    private static final Color[] RAY = {new Color(1f, 0.84f, 0.37f, 0.35f)};
    private static final Color SHINE = new Color(1f, 1f, 1f, 0.55f);
    private static final Color SPARKLE = new Color(1f, 0.95f, 0.69f, 1f);
    private static final float EFFECT_SECONDS = 2.4f;
    // The rays are 30 degrees apart, so one 30 degree turn repeats seamlessly
    private static final float RAY_TURN_SECONDS = 1.2f;
    private static final float[][] SPOTS = {{-0.08f, 0.05f}, {1.06f, 0.2f}, {-0.05f, 0.85f}, {1.04f, 0.92f}, {0.5f, -0.06f}};

    private final String title;
    private final String text;
    private final FImage image;
    private final Entrance entrance;
    private final List<RogueButton> buttons = new ArrayList<>();
    private final Message message;
    private final IntConsumer onChoice;
    private float pop = 1;
    private float time = EFFECT_SECONDS;
    private float spin;

    private RogueDialog(String title, String text, FImage image, Entrance entrance, List<String> answers, IntConsumer onChoice) {
        super(null);
        this.title = title;
        this.text = text;
        this.image = image;
        this.entrance = image == null ? Entrance.PLAIN : entrance;
        this.onChoice = onChoice;
        message = add(new Message(text));
        for (int i = 0; i < answers.size(); i++) {
            int choice = i;
            RogueButton.State state = i == answers.size() - 1 ? RogueButton.State.MAIN : RogueButton.State.NORMAL;
            buttons.add(add(new RogueButton(answers.get(i), null, state, () -> choose(choice))));
        }
    }

    public static void show(String title, String text, List<String> answers, IntConsumer onChoice) {
        show(title, text, null, Entrance.PLAIN, answers, onChoice);
    }

    /** image may be null. */
    public static void show(String title, String text, FImage image, Entrance entrance, List<String> answers, IntConsumer onChoice) {
        RogueDialog dialog = new RogueDialog(title, text, image, entrance, answers, onChoice);
        dialog.show();
        RogueMotion.play(0.3f, t -> {
            dialog.pop = t;
            dialog.revalidate();
        }, null);
        if (dialog.entrance != Entrance.PLAIN) {
            RogueMotion.play(EFFECT_SECONDS, t -> dialog.time = t * EFFECT_SECONDS, null);
        }
        if (dialog.entrance == Entrance.RAYS) {
            dialog.turnRays();
        }
    }

    /** Keeps the light turning for as long as the dialog is up. */
    private void turnRays() {
        RogueMotion.loop(RAY_TURN_SECONDS, t -> spin = t * 30, this::isVisible, null);
    }

    /** An unlock notice, its image arriving in the light; waits for OK unless asked on the GUI thread. */
    public static void notice(FImage image, String text, String title) {
        List<String> ok = List.of(Localizer.getInstance().getMessage("lblOK"));
        if (FThreads.isGuiThread()) {
            show(title, text, image, Entrance.RAYS, ok, choice -> { });
        } else {
            MobileRogueDecisions.dialog(title, text, image, Entrance.RAYS, ok);
        }
    }

    private void choose(int choice) {
        hide();
        onChoice.accept(choice);
    }

    private float zoom() {
        return 0.7f + 0.3f * Interpolation.swingOut.apply(pop);
    }

    /** The image's height: its own size, less when the screen is too short to leave room for the message. */
    private float imageHeight() {
        if (image == null) {
            return 0;
        }
        float u = RogueLook.unit();
        float wanted = (entrance == Entrance.DROP ? 64 : 150) * u;
        float w = (Forge.isLandscapeMode() ? 300 : 254) * u;
        float room = getHeight() - 16 * u - RogueLook.wrappedHeight(text, w - 28 * u, 1) - 96 * u;
        return Math.max(48 * u, Math.min(wanted, room));
    }

    private float[] box() {
        float u = RogueLook.unit();
        float w = (Forge.isLandscapeMode() ? 300 : 254) * u;
        float ih = imageHeight();
        // 4 more for the last line's descenders, which the message's clip would otherwise cut
        float h = Math.min(RogueLook.wrappedHeight(text, w - 28 * u, 1) + 88 * u + (ih > 0 ? ih + 8 * u : 0), getHeight() - 16 * u);
        return new float[]{(getWidth() - w) / 2, (getHeight() - h) / 2, w, h};
    }

    /** The image's place: centred below the title, narrowed to the box when it is too wide. */
    private float[] imageBox(float[] box) {
        float u = RogueLook.unit();
        float ih = imageHeight();
        float iw = ih * image.getWidth() / image.getHeight();
        float maxW = box[2] - 28 * u;
        if (iw > maxW) {
            ih = ih * maxW / iw;
            iw = maxW;
        }
        return new float[]{box[0] + (box[2] - iw) / 2, box[1] + 40 * u, iw, ih};
    }

    @Override
    protected void doLayout(float width, float height) {
        RogueLook.zoomed(zoom(), () -> {
            float u = RogueLook.unit();
            float[] box = box();
            float bw = (box[2] - 20 * u) / buttons.size();
            for (int i = 0; i < buttons.size(); i++) {
                buttons.get(i).setBounds(box[0] + 10 * u + i * bw, box[1] + box[3] - 40 * u, bw, 30 * u);
            }
            float top = image == null ? 38 * u : imageHeight() + 46 * u;
            message.setBounds(box[0] + 14 * u, box[1] + top, box[2] - 28 * u, box[3] - top - 46 * u);
        });
    }

    @Override
    public void draw(Graphics g) {
        float fade = Math.min(1, pop / 0.5f);
        if (fade < 1) {
            g.setAlphaComposite(fade);
        }
        RogueLook.zoomed(zoom(), () -> super.draw(g));
        if (fade < 1) {
            g.resetAlphaComposite();
        }
    }

    @Override
    protected void drawBackground(Graphics g) {
        float u = RogueLook.unit();
        g.fillRect(DIM, 0, 0, getWidth(), getHeight());
        float[] box = box();
        float x = box[0];
        float y = box[1];
        float w = box[2];
        RogueLook.paper(g, x, y, w, box[3]);
        RogueLook.fit(g, title, x + 12 * u, y + 10 * u, 1.4f, RogueLook.INK, w - 24 * u, Align.center);
        RogueLook.line(g, x + 14 * u, y + 32 * u, x + w - 14 * u, y + 32 * u, 0.8f, RogueLook.SOFT);
        if (image != null) {
            drawImage(g, box, imageBox(box));
        }
    }

    private void drawImage(Graphics g, float[] box, float[] at) {
        float cx = at[0] + at[2] / 2;
        float cy = at[1] + at[3] / 2;
        switch (entrance) {
            case RAYS -> {
                if (g.startClip(box[0], box[1], box[2], box[3])) {
                    RogueLook.rays(g, RAY, cx, cy, Math.max(box[2], box[3]), spin);
                }
                g.endClip();
                image.draw(g, at[0], at[1], at[2], at[3]);
                drawShine(g, at);
                drawSparkles(g, at);
            }
            case DROP -> {
                float land = Math.min(1, time / 0.6f);
                RogueLook.halo(g, cx, cy, at[3] * 0.75f * Math.min(1, time / 0.8f), time, 1);
                float dy = -80 * RogueLook.unit() * (1 - Interpolation.swingOut.apply(land));
                image.draw(g, at[0], at[1] + dy, at[2], at[3]);
                drawSparkles(g, at);
            }
            default -> image.draw(g, at[0], at[1], at[2], at[3]);
        }
    }

    /** A bright band sweeps across the image twice. */
    private void drawShine(Graphics g, float[] at) {
        float t = (time - 0.5f) / 0.7f;
        if (t > 1) {
            t = (time - 1.4f) / 0.7f;
        }
        if (t <= 0 || t >= 1) {
            return;
        }
        if (!g.startClip(at[0], at[1], at[2], at[3])) {
            g.endClip();
            return;
        }
        float band = at[2] * 0.18f;
        float x = at[0] - at[3] * 0.6f + (at[2] + at[3] * 1.2f) * t;
        float top = at[1];
        float bottom = at[1] + at[3];
        float lean = at[3] * 0.6f;
        g.fillTriangle(SHINE, x + lean, top, x + lean + band, top, x, bottom);
        g.fillTriangle(SHINE, x + lean + band, top, x + band, bottom, x, bottom);
        g.endClip();
    }

    /** Small stars wink around the image, one after another. */
    private void drawSparkles(Graphics g, float[] at) {
        float u = RogueLook.unit();
        for (int i = 0; i < SPOTS.length; i++) {
            float t = (time - 0.4f - i * 0.35f) / 0.5f;
            if (t <= 0 || t >= 1) {
                continue;
            }
            float s = 6 * u * MathUtils.sin(t * MathUtils.PI);
            float sx = at[0] + at[2] * SPOTS[i][0];
            float sy = at[1] + at[3] * SPOTS[i][1];
            g.fillTriangle(SPARKLE, sx - s, sy, sx + s, sy, sx, sy - 2.2f * s);
            g.fillTriangle(SPARKLE, sx - s, sy, sx + s, sy, sx, sy + 2.2f * s);
            g.fillTriangle(SPARKLE, sx, sy - s, sx, sy + s, sx - 2.2f * s, sy);
            g.fillTriangle(SPARKLE, sx, sy - s, sx, sy + s, sx + 2.2f * s, sy);
        }
    }

    @Override
    public boolean keyDown(int keyCode) {
        if (keyCode == Keys.ESCAPE || keyCode == Keys.BACK) {
            choose(0);
            return true;
        }
        return super.keyDown(keyCode);
    }

    /** The message, scrolling when the screen is too short for it. */
    private static final class Message extends RogueScrollPane {
        private final String text;

        Message(String text) {
            this.text = text;
            add(new FDisplayObject() {
                @Override
                public void draw(Graphics g) {
                    RogueLook.wrap(g, Message.this.text, 0, 0, getWidth(), 1, RogueLook.INK, Align.center);
                }
            });
        }

        @Override
        protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
            float h = RogueLook.wrappedHeight(text, visibleWidth, 1);
            getChildAt(0).setBounds(0, 0, visibleWidth, h);
            return new ScrollBounds(visibleWidth, h);
        }
    }
}
