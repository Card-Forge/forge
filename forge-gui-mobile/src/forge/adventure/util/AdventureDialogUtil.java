package forge.adventure.util;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.Align;
import com.github.tommyettinger.textra.TextraButton;
import com.github.tommyettinger.textra.TextraLabel;

import forge.Forge;
import forge.adventure.scene.Scene;

/**
 * Adventure deck-list dialogs: fixed width, measured row heights, UI-window sizing.
 * <p>
 * Layout pattern (top to bottom): title → body line(s) → option rows → abort.
 * Row heights come from wrapped text measurement, not {@link TextraButton#getPrefHeight()}.
 */
public final class AdventureDialogUtil {
    private static final float H_PAD = 6f;
    private static final float V_PAD = 4f;
    private static final float GAP = 2f;
    private static final float BUTTON_TEXT_INSET = 8f;
    private static final float OPTION_ROW_MIN = 14f;
    private static final float OPTION_ROW_MAX = 24f;
    private static final float IMPORT_BODY_MAX = 70f;
    private static final float DIALOG_CHROME_W = 16f;
    private static final GlyphLayout GLYPH = new GlyphLayout();

    private static float bodyScrollMax() {
        return Math.min(96f, Math.max(56f, windowHeight() * 0.38f));
    }

    private AdventureDialogUtil() { }

    public static final class ChoiceOption {
        public final String label;
        public final Runnable action;

        public ChoiceOption(String label, Runnable action) {
            this.label = label;
            this.action = action;
        }
    }

    public static float windowWidth() {
        return Scene.getIntendedWidth();
    }

    public static float windowHeight() {
        return Scene.getIntendedHeight();
    }

    public static float dialogWidth() {
        return Math.min(300f, Math.max(240f, windowWidth() * 0.72f));
    }

    public static float maxDialogHeight() {
        return windowHeight() * 0.88f;
    }

    public static float choiceButtonHeight() {
        return OPTION_ROW_MIN;
    }

    public static float actionButtonHeight() {
        return OPTION_ROW_MIN;
    }

    public static float compactButtonHeight() {
        return OPTION_ROW_MIN;
    }

    public static float compactFieldHeight() {
        return 13f;
    }

    public static float fileListRowHeight() {
        return 11f;
    }

    public static float filePickerMaxHeight() {
        return windowHeight() * 0.86f;
    }

    public static float filePickerChromeHeight(boolean hasHelpText) {
        float h = V_PAD * 2f + 12f;
        if (hasHelpText) {
            h += 18f;
        }
        h += actionButtonHeight() + compactFieldHeight() + 8f;
        h += compactFieldHeight() + 8f;
        h += actionButtonHeight() + GAP + 8f;
        return h;
    }

    public static float filePickerListHeightMax(boolean hasHelpText) {
        return Math.max(28f, filePickerMaxHeight() - filePickerChromeHeight(hasHelpText));
    }

    public static TextraLabel wrappedLabel(String text, float width) {
        float wrapW = Math.max(36f, width - 8f);
        TextraLabel label = Controls.newTextraLabel("");
        label.setWrap(true);
        label.setWidth(wrapW);
        label.layout.setTargetWidth(wrapW);
        label.setText(text);
        return label;
    }

    private static BitmapFont defaultFont() {
        return Controls.getSkin().getFont("default");
    }

    /** Wrapped text height via {@link GlyphLayout} (matches what Label will draw). */
    public static float measureWrappedHeight(String text, float width) {
        if (StringUtils.isEmpty(text)) {
            return 0f;
        }
        float wrapW = Math.max(40f, width - 8f);
        GLYPH.setText(defaultFont(), text, Color.WHITE, wrapW, Align.left, true);
        return GLYPH.height + 4f;
    }

    public static float textBlockHeight(String text, float width, float maxH) {
        return Math.min(maxH, Math.max(12f, measureWrappedHeight(text, width)));
    }

    public static float optionRowHeight(String text, float width) {
        float inner = Math.max(40f, width - BUTTON_TEXT_INSET);
        float h = measureWrappedHeight(text, inner) + 2f;
        return Math.min(OPTION_ROW_MAX, Math.max(OPTION_ROW_MIN, h));
    }

    public static void addBodyLine(Table parent, String text, float width) {
        addBodyLine(parent, text, width, bodyScrollMax());
    }

    public static void addBodyLine(Table parent, String text, float width, float maxH) {
        if (StringUtils.isEmpty(text)) {
            return;
        }
        float h = textBlockHeight(text, width, maxH);
        Label label = new Label(text, Controls.getSkin());
        label.setWrap(true);
        label.setAlignment(Align.topLeft);
        parent.add(label).width(width - 4f).height(h).align(Align.topLeft).padBottom(GAP).row();
    }

    /**
     * Body text with scroll when it exceeds {@link #bodyScrollMax()}.
     *
     * @return scroll pane when scrollable, else null
     */
    public static ScrollPane addBodyContent(Table parent, float width, String text) {
        if (StringUtils.isEmpty(text)) {
            return null;
        }
        float maxH = bodyScrollMax();
        float fullH = measureWrappedHeight(text, width);
        if (fullH <= maxH) {
            addBodyLine(parent, text, width, fullH);
            return null;
        }
        Label bodyLabel = new Label(text, Controls.getSkin());
        bodyLabel.setWrap(true);
        bodyLabel.setAlignment(Align.topLeft);
        bodyLabel.setWidth(width - 8f);
        bodyLabel.setHeight(multiLineLabelHeight(text, width));
        ScrollPane scroll = new ScrollPane(bodyLabel, Controls.getSkin());
        configureScrollPane(scroll);
        scroll.setScrollingDisabled(true, false);
        retainScrollFocus(scroll);
        parent.add(scroll).width(width).height(maxH).align(Align.topLeft).padBottom(GAP).row();
        return scroll;
    }

    private static void finishDialog(Dialog dialog, float contentWidth) {
        dialog.pack();
        float minW = contentWidth + DIALOG_CHROME_W;
        if (dialog.getWidth() < minW) {
            dialog.setWidth(minW);
            dialog.invalidate();
            dialog.layout();
        }
    }

    private static void addDialogButtonRow(Dialog dialog, float width, String primaryLabel, Runnable onPrimary,
            String secondaryLabel, Runnable onSecondary) {
        Table actions = new Table();
        actions.defaults().pad(0);
        if (secondaryLabel == null || onSecondary == null) {
            float rowH = optionRowHeight(primaryLabel, width);
            TextraButton btn = choiceButton(primaryLabel, width, onPrimary);
            btn.setSize(width, rowH);
            actions.add(btn).width(width).height(rowH);
        } else {
            addSideBySideActions(actions, width, primaryLabel, onPrimary, secondaryLabel, onSecondary);
        }
        dialog.getButtonTable().pad(V_PAD, H_PAD, V_PAD, H_PAD);
        dialog.getButtonTable().add(actions).width(width);
    }

    /**
     * Content area plus OK | secondary in the button row (e.g. rename with extra fields).
     */
    public static Dialog buildFormDialog(String title, String confirmLabel, Runnable onConfirm,
            Runnable onCancel, java.util.function.Consumer<Table> extendContent) {
        float w = dialogWidth();
        Dialog dialog = newEmptyDialog(onCancel);
        Table root = newRoot();
        dialog.getContentTable().add(root).width(w).top().left();
        if (StringUtils.isNotEmpty(title)) {
            root.add(titleLabel(title, w)).width(w).align(Align.topLeft).padBottom(GAP).row();
        }
        extendContent.accept(root);
        var loc = Forge.getLocalizer();
        addDialogButtonRow(dialog, w, confirmLabel, onConfirm, loc.getMessage("lblAbort"), onCancel);
        finishDialog(dialog, w);
        return dialog;
    }

    public static Label titleLabel(String text, float width) {
        Label label = new Label(text, Controls.getSkin());
        label.setAlignment(Align.topLeft);
        label.setWrap(true);
        label.setWidth(Math.max(36f, width - 8f));
        return label;
    }

    public static void addWrappedBody(Table parent, String body, float width) {
        addBodyLine(parent, body, width);
    }

    public static ScrollPane newFileListScrollPane(Table fileTable) {
        ScrollPane scroll = new ScrollPane(fileTable, Controls.getSkin(), "paper");
        configureScrollPane(scroll);
        return scroll;
    }

    public static void configureScrollPane(ScrollPane scroll) {
        scroll.setFadeScrollBars(false);
        scroll.setScrollbarsVisible(true);
        scroll.setScrollingDisabled(true, false);
        scroll.setOverscroll(false, false);
    }

    /** Keep wheel / scroll keys on this pane while the pointer is over it. */
    public static void retainScrollFocus(ScrollPane scroll) {
        scroll.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                event.getStage().setScrollFocus(scroll);
            }
        });
    }

    private static float multiLineLabelHeight(String text, float width) {
        if (StringUtils.isEmpty(text)) {
            return defaultFont().getLineHeight();
        }
        int lines = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lines++;
            }
        }
        float lineH = defaultFont().getLineHeight();
        float wrapW = Math.max(40f, width - 8f);
        float wrapped = measureWrappedHeight(text, width);
        return Math.max(lines * lineH + 4f, wrapped);
    }

    public static void fitOnStage(Stage stage, Dialog dialog) {
        if (stage == null || dialog == null) {
            return;
        }
        dialog.pack();
        float margin = 4f;
        float sw = stage.getWidth();
        float sh = stage.getHeight();
        float maxW = sw - margin * 2f;
        float maxH = maxDialogHeight();
        float dw = dialog.getWidth();
        float dh = dialog.getHeight();
        if (dw > maxW) {
            dw = maxW;
        }
        if (dh > maxH) {
            dh = maxH;
        }
        dialog.setSize(dw, dh);
        dialog.invalidate();
        dialog.layout();
        float x = (sw - dw) * 0.5f;
        float y = (sh - dh) * 0.5f;
        if (x < margin) {
            x = margin;
        }
        if (x + dw > sw - margin) {
            x = sw - margin - dw;
        }
        if (y < margin) {
            y = margin;
        }
        if (y + dh > sh - margin) {
            y = sh - margin - dh;
        }
        dialog.setPosition(x, y);
    }

    private static Dialog newEmptyDialog(Runnable onEscape) {
        Dialog dialog = new Dialog("", Controls.getSkin());
        dialog.getTitleTable().clearChildren();
        dialog.getButtonTable().clearChildren();
        dialog.getContentTable().clear();
        dialog.setUserObject(onEscape);
        return dialog;
    }

    private static Table newRoot() {
        Table root = new Table();
        root.pad(V_PAD, H_PAD, V_PAD, H_PAD);
        root.defaults().pad(0);
        return root;
    }

    /**
     * Import-deck menu: title, intro, prompt, three modes, abort.
     */
    public static Dialog buildImportDeckDialog(String intro, String prompt,
            String ownedLabel, Runnable onOwned,
            String buyLabel, Runnable onBuy,
            String giveLabel, Runnable onGive,
            Runnable onAbort) {
        float w = dialogWidth();
        Dialog dialog = newEmptyDialog(onAbort);
        Table root = newRoot();
        dialog.getContentTable().add(root).width(w).top().left();

        root.add(titleLabel(Forge.getLocalizer().getMessage("lblAdvImportDeck"), w))
            .width(w).align(Align.topLeft).padBottom(GAP).row();

        String bodyText = intro + "\n\n" + prompt;
        float bodyH = textBlockHeight(bodyText, w, IMPORT_BODY_MAX);
        Label bodyLabel = new Label(bodyText, Controls.getSkin());
        bodyLabel.setWrap(true);
        bodyLabel.setAlignment(Align.topLeft);
        ScrollPane bodyScroll = new ScrollPane(bodyLabel, Controls.getSkin());
        configureScrollPane(bodyScroll);
        bodyScroll.setScrollingDisabled(true, false);
        root.add(bodyScroll).width(w).height(bodyH).align(Align.topLeft).padBottom(GAP).row();

        addCompactOptionRow(root, w, ownedLabel, onOwned);
        addCompactOptionRow(root, w, buyLabel, onBuy);
        addCompactOptionRow(root, w, giveLabel, onGive);
        addCompactOptionRow(root, w, Forge.getLocalizer().getMessage("lblAbort"), onAbort);

        finishDialog(dialog, w);
        return dialog;
    }

    public static Dialog buildChoiceDialog(String title, String body,
            List<ChoiceOption> options, Runnable onAbort) {
        float w = dialogWidth();
        Dialog dialog = newEmptyDialog(onAbort);
        Table root = newRoot();
        dialog.getContentTable().add(root).width(w).top().left();

        if (StringUtils.isNotEmpty(title)) {
            root.add(titleLabel(title, w)).width(w).align(Align.topLeft).padBottom(GAP).row();
        }
        addBodyContent(root, w, body);
        for (ChoiceOption option : options) {
            addCompactOptionRow(root, w, option.label, option.action);
        }
        if (onAbort != null) {
            addDialogButtonRow(dialog, w, Forge.getLocalizer().getMessage("lblAbort"), onAbort,
                null, null);
        }

        finishDialog(dialog, w);
        return dialog;
    }

    public static Dialog buildMessageDialog(String title, String body, Runnable onClose) {
        return buildMessageDialog(title, body, Forge.getLocalizer().getMessage("lblAbort"), onClose);
    }

    /** Brief status with no buttons (caller dismisses). */
    public static Dialog buildTransientMessage(String message) {
        float w = dialogWidth();
        Dialog dialog = new Dialog("", Controls.getSkin());
        dialog.getTitleTable().clearChildren();
        dialog.getButtonTable().clearChildren();
        dialog.getContentTable().clear();
        Table root = newRoot();
        dialog.getContentTable().add(root).width(w).top().left();
        root.add(titleLabel(message, w)).width(w).align(Align.center).row();
        finishDialog(dialog, w);
        return dialog;
    }

    public static Dialog buildMessageDialog(String title, String body, String closeLabel, Runnable onClose) {
        float w = dialogWidth();
        Dialog dialog = newEmptyDialog(onClose);
        Table root = newRoot();
        dialog.getContentTable().add(root).width(w).top().left();

        if (StringUtils.isNotEmpty(title)) {
            root.add(titleLabel(title, w)).width(w).align(Align.topLeft).padBottom(GAP).row();
        }
        addBodyContent(root, w, body);
        addDialogButtonRow(dialog, w, closeLabel, onClose, null, null);
        finishDialog(dialog, w);
        return dialog;
    }

    /**
     * Long report (e.g. missing-card list): fixed-height scroll body, Save | Abort in button row.
     *
     * @param scrollOut optional single-element array; receives the scroll pane for {@code showDialog(dialog, scroll)}
     */
    public static Dialog buildScrollableReportDialog(String title, String body, String saveLabel,
            Runnable onSave, Runnable onClose, ScrollPane[] scrollOut) {
        float w = dialogWidth();
        float scrollH = Math.min(132f, Math.max(72f, windowHeight() * 0.48f));
        Dialog dialog = newEmptyDialog(onClose);
        Table root = newRoot();
        dialog.getContentTable().add(root).width(w).top().left();

        if (StringUtils.isNotEmpty(title)) {
            root.add(titleLabel(title, w)).width(w).align(Align.topLeft).padBottom(GAP).row();
        }

        Label bodyLabel = new Label(body, Controls.getSkin());
        bodyLabel.setAlignment(Align.topLeft);
        bodyLabel.setWrap(true);
        bodyLabel.setWidth(w - 8f);
        bodyLabel.setHeight(multiLineLabelHeight(body, w));

        ScrollPane scroll = new ScrollPane(bodyLabel, Controls.getSkin());
        configureScrollPane(scroll);
        scroll.setScrollingDisabled(true, false);
        retainScrollFocus(scroll);
        root.add(scroll).width(w).height(scrollH).align(Align.topLeft).row();

        addDialogButtonRow(dialog, w, saveLabel, onSave,
            Forge.getLocalizer().getMessage("lblAbort"), onClose);

        if (scrollOut != null && scrollOut.length > 0) {
            scrollOut[0] = scroll;
        }
        finishDialog(dialog, w);
        return dialog;
    }

    /**
     * Scrollable report with a single OK button (e.g. mark-for-sale results).
     */
    public static Dialog buildScrollableOkDialog(String title, String body, Runnable onOk,
            ScrollPane[] scrollOut) {
        return buildScrollableOkDialog(title, body, onOk, null, scrollOut);
    }

    public static Dialog buildScrollableOkDialog(String title, String body, Runnable onOk,
            Runnable onRollback, ScrollPane[] scrollOut) {
        float w = dialogWidth();
        float scrollH = Math.min(132f, Math.max(72f, windowHeight() * 0.48f));
        Dialog dialog = newEmptyDialog(onOk);
        Table root = newRoot();
        dialog.getContentTable().add(root).width(w).top().left();

        if (StringUtils.isNotEmpty(title)) {
            root.add(titleLabel(title, w)).width(w).align(Align.topLeft).padBottom(GAP).row();
        }

        Label bodyLabel = new Label(body, Controls.getSkin());
        bodyLabel.setAlignment(Align.topLeft);
        bodyLabel.setWrap(true);
        bodyLabel.setWidth(w - 8f);
        bodyLabel.setHeight(multiLineLabelHeight(body, w));

        ScrollPane scroll = new ScrollPane(bodyLabel, Controls.getSkin());
        configureScrollPane(scroll);
        scroll.setScrollingDisabled(true, false);
        retainScrollFocus(scroll);
        root.add(scroll).width(w).height(scrollH).align(Align.topLeft).row();

        if (onRollback != null) {
            addDialogButtonRow(dialog, w, Forge.getLocalizer().getMessage("lblOK"), onOk,
                Forge.getLocalizer().getMessage("lblAdvRollback"), onRollback);
        } else {
            addDialogButtonRow(dialog, w, Forge.getLocalizer().getMessage("lblOK"), onOk, null, null);
        }

        if (scrollOut != null && scrollOut.length > 0) {
            scrollOut[0] = scroll;
        }
        finishDialog(dialog, w);
        return dialog;
    }

    public static Dialog buildConfirmDialog(String title, String body, String confirmLabel,
            Runnable onConfirm, Runnable onAbort) {
        return buildConfirmDialog(title, body, confirmLabel, onConfirm,
            Forge.getLocalizer().getMessage("lblAbort"), onAbort);
    }

    public static Dialog buildConfirmDialog(String title, String body, String confirmLabel,
            Runnable onConfirm, String cancelLabel, Runnable onCancel) {
        float w = dialogWidth();
        Dialog dialog = newEmptyDialog(onCancel);
        Table root = newRoot();
        dialog.getContentTable().add(root).width(w).top().left();

        if (StringUtils.isNotEmpty(title)) {
            root.add(titleLabel(title, w)).width(w).align(Align.topLeft).padBottom(GAP).row();
        }
        addBodyContent(root, w, body);
        addDialogButtonRow(dialog, w, confirmLabel, onConfirm, cancelLabel, onCancel);
        finishDialog(dialog, w);
        return dialog;
    }

    private static void addCompactOptionRow(Table parent, float width, String text, Runnable action) {
        float rowH = optionRowHeight(text, width);
        TextraButton button = choiceButton(text, width, action);
        button.setSize(width, rowH);
        parent.add(button).width(width).height(rowH).padTop(1f).row();
    }

    private static void addOptionRow(Table parent, float width, String text, Runnable action) {
        addCompactOptionRow(parent, width, text, action);
    }

    public static void addSideBySideActions(Table parent, float width, String primaryLabel,
            Runnable onPrimary, Runnable onAbort) {
        addSideBySideActions(parent, width, primaryLabel, onPrimary,
            Forge.getLocalizer().getMessage("lblAbort"), onAbort);
    }

    public static void addSideBySideActions(Table parent, float width, String primaryLabel,
            Runnable onPrimary, String secondaryLabel, Runnable onSecondary) {
        float half = (width - GAP) * 0.5f;
        float rowH = Math.max(optionRowHeight(primaryLabel, half),
            optionRowHeight(secondaryLabel, half));
        parent.defaults().pad(0);
        TextraButton ok = choiceButton(primaryLabel, half, onPrimary);
        TextraButton cancel = choiceButton(secondaryLabel, half, onSecondary);
        ok.setSize(half, rowH);
        cancel.setSize(half, rowH);
        parent.add(ok).width(half).height(rowH);
        parent.add(cancel).width(half).height(rowH).padLeft(GAP);
    }

    public static void addSideBySideActions(Table parent, String primaryLabel, Runnable onPrimary,
            Runnable onAbort) {
        addSideBySideActions(parent, dialogWidth(), primaryLabel, onPrimary, onAbort);
    }

    public static void addAbortRow(Table parent, Runnable onAbort) {
        if (onAbort == null) {
            return;
        }
        addOptionRow(parent, dialogWidth(), Forge.getLocalizer().getMessage("lblAbort"), onAbort);
    }

    public static TextraButton choiceButton(String text, float cellWidth, Runnable onClick) {
        float innerW = Math.max(32f, cellWidth - BUTTON_TEXT_INSET);
        TextraButton button = Controls.newTextButton(text, onClick);
        TextraLabel lbl = button.getTextraLabel();
        lbl.setWrap(true);
        lbl.setWidth(innerW);
        lbl.layout.setTargetWidth(innerW);
        button.setText(text);
        lbl.setWrap(true);
        lbl.layout.setTargetWidth(innerW);
        lbl.setWidth(innerW);
        float rowH = optionRowHeight(text, cellWidth);
        button.setSize(cellWidth, rowH);
        return button;
    }

    public static TextraButton choiceButton(String text, Runnable onClick) {
        return choiceButton(text, dialogWidth(), onClick);
    }

    public static Cell<TextraButton> addFullWidthButton(Table table, String text, Runnable onClick) {
        float w = dialogWidth();
        float h = optionRowHeight(text, w);
        TextraButton button = choiceButton(text, w, onClick);
        return table.add(button).width(w).height(h).fillX().padTop(GAP);
    }

    public static TextraButton compactButton(String text, Runnable onClick) {
        return choiceButton(text, onClick);
    }

    public static TextField compactTextField(String text) {
        return Controls.newTextField(text);
    }

    public static TextraLabel compactLabel(String text, boolean wrap) {
        return wrappedLabel(text, dialogWidth());
    }

    public static void addDialogActions(Table parent, String primaryLabel, Runnable onPrimary,
            Runnable onAbort) {
        addSideBySideActions(parent, primaryLabel, onPrimary, onAbort);
    }

    public static void centerOnStage(Stage stage, Dialog dialog) {
        fitOnStage(stage, dialog);
    }
}
