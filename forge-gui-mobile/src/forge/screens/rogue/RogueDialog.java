package forge.screens.rogue;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Align;
import forge.Forge;
import forge.Graphics;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FOverlay;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/** A parchment message with one or two answers, the last one the main action. Back gives the first answer. */
public class RogueDialog extends FOverlay {
    private static final Color DIM = new Color(0, 0, 0, 0.6f);

    private final String title;
    private final String text;
    private final List<RogueButton> buttons = new ArrayList<>();
    private final Message message;
    private final IntConsumer onChoice;

    private RogueDialog(String title, String text, List<String> answers, IntConsumer onChoice) {
        super(null);
        this.title = title;
        this.text = text;
        this.onChoice = onChoice;
        message = add(new Message(text));
        for (int i = 0; i < answers.size(); i++) {
            int choice = i;
            RogueButton.State state = i == answers.size() - 1 ? RogueButton.State.MAIN : RogueButton.State.NORMAL;
            buttons.add(add(new RogueButton(answers.get(i), null, state, () -> choose(choice))));
        }
    }

    public static void show(String title, String text, List<String> answers, IntConsumer onChoice) {
        new RogueDialog(title, text, answers, onChoice).show();
    }

    private void choose(int choice) {
        hide();
        onChoice.accept(choice);
    }

    private float[] box() {
        float u = RogueLook.unit();
        float w = (Forge.isLandscapeMode() ? 300 : 254) * u;
        float h = Math.min(RogueLook.wrappedHeight(text, w - 28 * u, 1) + 84 * u, getHeight() - 16 * u);
        return new float[]{(getWidth() - w) / 2, (getHeight() - h) / 2, w, h};
    }

    @Override
    protected void doLayout(float width, float height) {
        float u = RogueLook.unit();
        float[] box = box();
        float bw = (box[2] - 20 * u) / buttons.size();
        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).setBounds(box[0] + 10 * u + i * bw, box[1] + box[3] - 40 * u, bw, 30 * u);
        }
        message.setBounds(box[0] + 14 * u, box[1] + 38 * u, box[2] - 28 * u, box[3] - 84 * u);
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
