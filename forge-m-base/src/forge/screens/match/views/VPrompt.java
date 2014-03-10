package forge.screens.match.views;

import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinColor.Colors;
import forge.game.Game;
import forge.screens.match.FControl;
import forge.toolbox.FButton;
import forge.toolbox.FButton.Corner;
import forge.toolbox.FContainer;
import forge.utils.Utils;

public class VPrompt extends FContainer {
    public static final float HEIGHT = Utils.AVG_FINGER_HEIGHT;
    public static final float BTN_WIDTH = HEIGHT * 1.5f;
    public static final float PADDING = 2;

    private static final FSkinColor backColor = FSkinColor.get(Colors.CLR_THEME2);
    private static final FSkinColor foreColor = FSkinColor.get(Colors.CLR_TEXT);
    private static final FSkinFont font = FSkinFont.get(11);

    private final FButton btnOk, btnCancel;
    private String message;

    public VPrompt() {
        btnOk = add(new FButton("", new Runnable() {
            @Override
            public void run() {
                FControl.getInputProxy().selectButtonOK();
            }
        }));
        btnCancel = add(new FButton("", new Runnable() {
            @Override
            public void run() {
                FControl.getInputProxy().selectButtonCancel();
            }
        }));
        btnOk.setSize(BTN_WIDTH, HEIGHT);
        btnCancel.setSize(BTN_WIDTH, HEIGHT);
        btnOk.setCorner(Corner.BottomLeft);
        btnCancel.setCorner(Corner.BottomRight);
    }

    public FButton getBtnOk() {
        return btnOk;
    }

    public FButton getBtnCancel() {
        return btnCancel;
    }

    public String getMessage() {
        return message;
    }
    public void setMessage(String message0) {
        message = message0;
    }

    /** Flashes animation on input panel if play is currently waiting on input. */
    public void remind() {
        //SDisplayUtil.remind(view);
    }

    public void updateText(Game game) {
        //FThreads.assertExecutedByEdt(true);
        //final Match match = game.getMatch();
        //final GameRules rules = game.getRules();
        //final String text = String.format("T:%d G:%d/%d [%s]", game.getPhaseHandler().getTurn(), match.getPlayedGames().size() + 1, rules.getGamesPerMatch(), rules.getGameType());
        //view.getLblGames().setText(text);
        //view.getLblGames().setToolTipText(String.format("%s: Game #%d of %d, turn %d", rules.getGameType(), match.getPlayedGames().size() + 1, rules.getGamesPerMatch(), game.getPhaseHandler().getTurn()));
    }

    @Override
    protected void doLayout(float width, float height) {
        btnCancel.setLeft(width - BTN_WIDTH);
    }

    @Override
    protected void drawBackground(Graphics g) {
        float w = getWidth();
        float h = getHeight();

        g.fillRect(backColor, 0, 0, w, h);
        if (!StringUtils.isEmpty(message)) {
            float x = BTN_WIDTH + PADDING;
            float y = PADDING;
            g.drawText(message, font, foreColor, x, y, w - 2 * x, h - 2 * y, true, HAlignment.CENTER, true);
        }
    }
}
