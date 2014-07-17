package forge.error;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinColor.Colors;
import forge.toolbox.FOptionPane;
import forge.toolbox.FScrollPane;
import forge.util.Utils;

public class BugReportDialog extends FOptionPane {
    private static boolean dialogShown;

    public static void show(String title, String text, boolean showExitAppBtn) {
        if (dialogShown || Forge.getCurrentScreen() == null) { return; } //don't allow showing if Forge not finished initializing yet

        dialogShown = true;
        BugReportDialog dialog = new BugReportDialog(title, text, showExitAppBtn);
        dialog.show();
    }
    
    private String text;

    private BugReportDialog(String title, String text0, boolean showExitAppBtn) {
        super(BugReporter.HELP_TEXT + "\n\n" + BugReporter.HELP_URL_LABEL + " " + BugReporter.HELP_URL + ".",
                title, null, new TemplateView(text0),
                getOptions(showExitAppBtn), 0, null);
        text = text0;
    }

    @Override
    public void setResult(final int option) {
        switch (option) {
        case 0:
            BugReporter.copyAndGoToForums(text);
            break;
        case 1:
            BugReporter.saveToFile(text);
            break;
        case 2:
            hide();
            dialogShown = false;
            break;
        case 3:
            hide();
            Gdx.app.exit();
            dialogShown = false;
            break;
        }
    }

    private static String[] getOptions(boolean showExitAppBtn) {
        String[] options = new String[showExitAppBtn ? 4 : 3];
        options[0] = BugReporter.REPORT;
        options[1] = BugReporter.SAVE;
        options[2] = BugReporter.CONTINUE;
        if (showExitAppBtn) {
            options[3] = BugReporter.EXIT;
        }
        return options;
    }

    private static class TemplateView extends FScrollPane {
        private static final FSkinFont FONT = FSkinFont.get(11);
        private static final FSkinColor BACK_COLOR = FSkinColor.get(Colors.CLR_ZEBRA);
        private static final FSkinColor FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT);
        private static final FSkinColor BORDER_COLOR = FSkinColor.get(Colors.CLR_BORDERS);
        private static final float PADDING = Utils.scaleMin(3);

        private final String text;

        private TemplateView(String text0) {
            text = text0;
            setHeight(Forge.getCurrentScreen().getHeight() / 3);
        }

        @Override
        protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
            TextBounds bounds = FONT.getMultiLineBounds(text);
            return new ScrollBounds(bounds.width + 2 * PADDING, bounds.height + 2 * PADDING +
                    FONT.getLineHeight() - FONT.getCapHeight()); //account for height below baseline of final line);
        }

        @Override
        public void drawBackground(Graphics g) {
            g.fillRect(BACK_COLOR, 0, 0, getWidth(), getHeight());
            g.drawText(text, FONT, FORE_COLOR, PADDING - getScrollLeft(), PADDING - getScrollTop(), getScrollWidth() - 2 * PADDING, getScrollHeight() - 2 * PADDING, false, HAlignment.LEFT, false);
        }

        @Override
        public void drawOverlay(Graphics g) {
            super.drawOverlay(g);
            g.drawRect(1, BORDER_COLOR, 0, 0, getWidth(), getHeight());
        }
    }
}
