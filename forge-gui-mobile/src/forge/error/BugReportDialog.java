package forge.error;

import com.badlogic.gdx.utils.Align;
import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.gui.error.BugReporter;
import forge.screens.FScreen;
import forge.toolbox.FButton;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FScrollPane;
import forge.toolbox.FTextArea;
import forge.util.Callback;
import forge.util.TextBounds;
import forge.util.Utils;

public class BugReportDialog extends FScreen { //use screen rather than dialog so screen with bug isn't rendered
    private static final float PADDING = Utils.scale(5);
    private static final float BUTTON_HEIGHT = Utils.AVG_FINGER_HEIGHT * 0.75f;
    private static boolean isOpen;

    public static void show(String title, String text, boolean showExitAppBtn) {
        if (isOpen || Forge.getCurrentScreen() == null) { return; } //don't allow showing if Forge not finished initializing yet

        isOpen = true;
        Forge.openScreen(new BugReportDialog(title, text, showExitAppBtn));
    }

    private final FTextArea lblHeader = add(new FTextArea(false, "Report Bug"));
    private final TemplateView tvDetails;
    private final FButton btnReport = add(new FButton(BugReporter.REPORT));
    private final FButton btnSave = add(new FButton(BugReporter.SAVE));
    private final FButton btnDiscard = add(new FButton(BugReporter.DISCARD));
    private final FButton btnExit = add(new FButton(BugReporter.EXIT));

    private BugReportDialog(String title, String text0, boolean showExitAppBtn) {
        super(title);
        lblHeader.setFont(FSkinFont.get(12));
        tvDetails = add(new TemplateView(text0));
        btnReport.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                BugReporter.sendSentry();
                Forge.back();
            }
        });
        btnSave.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                BugReporter.saveToFile(tvDetails.text);
            }
        });
        btnDiscard.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                Forge.back();
            }
        });
        if (showExitAppBtn) {
            btnExit.setCommand(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    Forge.exit(true);
                }
            });
        }
        else {
            btnExit.setVisible(false);
        }
    }

    @Override
    public FScreen getLandscapeBackdropScreen() {
        return null;
    }

    @Override
    public void onClose(Callback<Boolean> canCloseCallback) {
        super.onClose(canCloseCallback);
        isOpen = false;
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        float x = PADDING;
        float y = startY + PADDING;
        float w = width - 2 * PADDING;

        lblHeader.setBounds(x, y, w, lblHeader.getPreferredHeight(w));
        y += lblHeader.getHeight() + PADDING;

        float buttonWidth, totalButtonHeight;
        float buttonHeight = BUTTON_HEIGHT;
        boolean landscapeMode = Forge.isLandscapeMode();
        if (landscapeMode) {
            buttonWidth = (w - 3 * PADDING) / 4;
            totalButtonHeight = buttonHeight;
        }
        else {
            buttonWidth = (w - PADDING) / 2;
            totalButtonHeight = 2 * buttonHeight + PADDING;
        }

        tvDetails.setBounds(x, y, w, height - totalButtonHeight - 2 * PADDING - y);
        y += tvDetails.getHeight() + PADDING;

        btnReport.setBounds(x, y, buttonWidth, buttonHeight);
        btnSave.setBounds(x + buttonWidth + PADDING, y, buttonWidth, buttonHeight);
        if (landscapeMode) {
            x += 2 * (buttonWidth + PADDING);
        }
        else {
            y += buttonHeight + PADDING;
        }
        if (btnExit.isVisible()) {
            btnDiscard.setBounds(x, y, buttonWidth, buttonHeight);
            btnExit.setBounds(x + buttonWidth + PADDING, y, buttonWidth, buttonHeight);
        }
        else {
            btnDiscard.setBounds(x, y, 2 * buttonWidth + PADDING, buttonHeight);
        }
    }

    private static class TemplateView extends FScrollPane {
        private static final FSkinFont FONT = FSkinFont.get(11);
        private static final FSkinColor BACK_COLOR = FSkinColor.get(Colors.CLR_ZEBRA);
        private static final FSkinColor FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT);
        private static final FSkinColor BORDER_COLOR = FSkinColor.get(Colors.CLR_BORDERS);
        private static final float PADDING = Utils.scale(3);

        private final String text;

        private TemplateView(String text0) {
            text = text0;
            setHeight(Forge.getScreenHeight() / 3);
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
            g.drawText(text, FONT, FORE_COLOR, PADDING - getScrollLeft(), PADDING - getScrollTop(), getScrollWidth() - 2 * PADDING, getScrollHeight() - 2 * PADDING, false, Align.left, false);
        }

        @Override
        public void drawOverlay(Graphics g) {
            super.drawOverlay(g);
            g.drawRect(1, BORDER_COLOR, 0, 0, getWidth(), getHeight());
        }
    }
}
