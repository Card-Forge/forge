package forge.adventure.util;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.github.tommyettinger.textra.TextraButton;

import forge.Forge;
import forge.adventure.scene.UIScene;
import forge.gui.GuiBase;
import forge.toolbox.FFileChooser;
import forge.toolbox.FFileChooser.ChoiceType;

/**
 * File picker for Adventure deck import/export.
 */
public final class AdventureFilePicker {
    private static final float PAD = 4f;
    private static final float FIELD_H = 14f;
    private static final float LIST_H = 58f;
    private static final float ROW_H = 12f;
    private static final float BTN_H = 16f;
    private static final float LIST_H_WITH_HELP = 46f;

    private AdventureFilePicker() { }

    public static String getDefaultBaseDir() {
        String downloads = Forge.getDeviceAdapter().getDownloadsDir();
        if (StringUtils.isNotEmpty(downloads)) {
            File dir = new File(downloads);
            if (dir.isDirectory()) {
                String path = dir.getAbsolutePath();
                return path.endsWith(File.separator) ? path : path + File.separator;
            }
        }
        String home = System.getProperty("user.home");
        if (home != null) {
            File fallback = new File(home, "Downloads");
            if (!fallback.isDirectory()) {
                fallback = new File(home);
            }
            String path = fallback.getAbsolutePath();
            return path.endsWith(File.separator) ? path : path + File.separator;
        }
        return File.separator;
    }

    public static void requestStorageAccessIfNeeded() {
        if (GuiBase.isAndroid() && Forge.getDeviceAdapter().needFileAccess()) {
            Forge.getDeviceAdapter().requestFileAcces();
        }
    }

    public static void chooseOpenFile(final String title, final Consumer<File> onChosen) {
        chooseOpenFile(title, null, onChosen);
    }

    public static void chooseOpenFile(final String title, final String helpText,
            final Consumer<File> onChosen) {
        chooseFile(title, false, "", helpText, onChosen);
    }

    public static void chooseSaveFile(final String title,
            final String defaultFilename, final Consumer<File> onChosen) {
        chooseSaveFile(title, defaultFilename, null, onChosen);
    }

    public static void chooseSaveFile(final String title,
            final String defaultFilename, final String helpText,
            final Consumer<File> onChosen) {
        chooseFile(title, true, defaultFilename, helpText, onChosen);
    }

    private static void chooseFile(final String title, final boolean save,
            final String defaultFilename, final String helpText,
            final Consumer<File> onChosen) {
        requestStorageAccessIfNeeded();
        Runnable showPicker = () -> {
            if (Forge.getCurrentScene() instanceof UIScene uiScene) {
                showSceneFileDialog(uiScene, save, title, defaultFilename, helpText, onChosen);
            } else {
                ChoiceType type = save ? ChoiceType.SaveFile : ChoiceType.OpenFile;
                FFileChooser.show(title, type,
                        defaultFilename != null ? defaultFilename : "",
                        getDefaultBaseDir(), path -> {
                    if (!StringUtils.isEmpty(path)) {
                        onChosen.accept(CardUtil.resolveFilePath(path));
                    }
                });
            }
        };
        Gdx.app.postRunnable(showPicker);
    }

    private static void setFieldText(TextField field, String text) {
        field.setText(text);
        field.setCursorPosition(0);
    }

    private static TextraButton actionButton(String text, float width, Runnable onClick) {
        TextraButton button = Controls.newTextButton(text, onClick);
        float inner = Math.max(24f, width - 8f);
        var lbl = button.getTextraLabel();
        lbl.setWrap(true);
        lbl.layout.setTargetWidth(inner);
        lbl.setWidth(inner);
        button.setText(text);
        return button;
    }

    private static Label fieldLabel(String text) {
        Label label = new Label(text, Controls.getSkin());
        label.setAlignment(Align.topLeft);
        return label;
    }

    private static Label listRowLabel(String text) {
        Label label = new Label(text, Controls.getSkin());
        label.setAlignment(Align.topLeft);
        label.setColor(Color.WHITE);
        return label;
    }

    private static void showSceneFileDialog(final UIScene uiScene, final boolean save,
            final String title, final String defaultFilename, final String helpText,
            final Consumer<File> onChosen) {
        var loc = Forge.getLocalizer();
        final boolean hasHelp = StringUtils.isNotEmpty(helpText);
        final float w = AdventureDialogUtil.dialogWidth();

        File startDir = new File(getDefaultBaseDir());
        final TextField folderField = Controls.newTextField(startDir.getAbsolutePath());
        final TextField nameField = Controls.newTextField(
            StringUtils.isNotEmpty(defaultFilename) ? defaultFilename : "");
        setFieldText(folderField, startDir.getAbsolutePath());

        final Table fileTable = new Table();
        fileTable.top().left();
        fileTable.pad(0);
        fileTable.defaults().left().pad(0);

        final ScrollPane listScroll = new ScrollPane(fileTable, Controls.getSkin());
        AdventureDialogUtil.configureScrollPane(listScroll);
        listScroll.setScrollingDisabled(true, false);
        AdventureDialogUtil.retainScrollFocus(listScroll);

        Dialog dialog = new Dialog("", Controls.getSkin());
        dialog.getTitleTable().clearChildren();
        dialog.getButtonTable().clearChildren();
        dialog.setUserObject((Runnable) uiScene::removeDialog);
        dialog.getContentTable().clear();

        Table root = new Table();
        root.top().left();
        root.pad(PAD);
        root.defaults().left().pad(0);
        dialog.getContentTable().add(root).width(w).top().left();

        root.add(fieldLabel(title)).width(w).height(12f).left().padBottom(2f).row();

        if (hasHelp) {
            AdventureDialogUtil.addBodyContent(root, w, helpText);
        }

        root.add(fieldLabel(loc.getMessage("lblAdvFolder"))).width(w).height(10f).left().row();
        root.add(folderField).width(w).height(FIELD_H).left().padBottom(2f).row();
        root.add(fieldLabel(loc.getMessage("lblAdvFileName"))).width(w).height(10f).left().row();
        root.add(nameField).width(w).height(FIELD_H).left().padBottom(2f).row();
        root.add(fieldLabel(loc.getMessage("lblAdvFiles"))).width(w).height(10f).left().padBottom(1f).row();
        float listH = hasHelp ? LIST_H_WITH_HELP : LIST_H;
        root.add(listScroll).width(w).height(listH).left().row();

        float half = (w - 4f) * 0.5f;
        String okText = save ? loc.getMessage("lblSave") : loc.getMessage("lblOK");
        Runnable onOk = () -> {
            File result = resolveFolderAndName(folderField.getText(), nameField.getText());
            if (result == null) {
                return;
            }
            if (save) {
                if (result.isDirectory()) {
                    return;
                }
                Runnable finishSave = () -> {
                    uiScene.removeDialog();
                    onChosen.accept(result);
                };
                if (result.exists()) {
                    String overwriteBody = loc.getMessage("lblAdvOverwriteConfirm", result.getName());
                    Dialog overwriteDlg = AdventureDialogUtil.buildConfirmDialog(title, overwriteBody,
                        loc.getMessage("lblAdvOverwrite"),
                        () -> {
                            uiScene.removeDialog();
                            finishSave.run();
                        },
                        loc.getMessage("lblAbort"),
                        uiScene::removeDialog);
                    uiScene.showDialog(overwriteDlg);
                } else {
                    finishSave.run();
                }
            } else {
                if (!result.exists() || result.isDirectory()) {
                    return;
                }
                uiScene.removeDialog();
                onChosen.accept(result);
            }
        };
        Table actions = new Table();
        actions.defaults().pad(0);
        actions.add(actionButton(okText, half, onOk)).width(half).height(BTN_H);
        actions.add(actionButton(loc.getMessage("lblAbort"), half, uiScene::removeDialog))
            .width(half).height(BTN_H).padLeft(4f);
        dialog.getButtonTable().pad(PAD);
        dialog.getButtonTable().add(actions).width(w).height(BTN_H + 2f);

        final Runnable[] refreshHolder = new Runnable[1];
        refreshHolder[0] = () -> {
            fileTable.clearChildren();
            File dir = directoryForPath(folderField.getText(), getDefaultBaseDir());
            if (dir == null || !dir.isDirectory()) {
                listScroll.setScrollY(0);
                listScroll.setScrollX(0);
                return;
            }
            setFieldText(folderField, dir.getAbsolutePath());
            float rowW = w - 14f;
            File parent = dir.getParentFile();
            if (parent != null) {
                Label upRow = listRowLabel("..");
                upRow.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        setFieldText(folderField, parent.getAbsolutePath());
                        nameField.setText("");
                        refreshHolder[0].run();
                    }
                });
                fileTable.add(upRow).height(ROW_H).width(rowW).left();
                fileTable.row();
            }
            File[] entries = dir.listFiles(AdventureFilePicker::showInFileList);
            if (entries == null) {
                listScroll.setScrollY(0);
                listScroll.setScrollX(0);
                listScroll.layout();
                return;
            }
            Arrays.sort(entries, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
            for (File entry : entries) {
                String labelText = entry.isDirectory() ? "[ " + entry.getName() + " ]" : entry.getName();
                Label row = listRowLabel(labelText);
                row.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        if (entry.isDirectory()) {
                            setFieldText(folderField, entry.getAbsolutePath());
                            nameField.setText("");
                            refreshHolder[0].run();
                        } else {
                            setFieldText(folderField, entry.getParentFile().getAbsolutePath());
                            setFieldText(nameField, entry.getName());
                            if (getTapCount() >= 2) {
                                onOk.run();
                            }
                        }
                    }
                });
                fileTable.add(row).height(ROW_H).width(rowW).left();
                fileTable.row();
            }
            listScroll.setScrollY(0);
            listScroll.setScrollX(0);
            listScroll.layout();
        };

        folderField.setTextFieldListener((f, c) -> {
            if (c == '\n' || c == '\t') {
                refreshHolder[0].run();
            }
        });

        dialog.pack();
        float chrome = 16f;
        dialog.setWidth(w + chrome);
        refreshHolder[0].run();
        uiScene.showDialog(dialog, listScroll);
    }

    private static boolean showInFileList(File dir, String name) {
        return !name.startsWith(".");
    }

    private static File resolveFolderAndName(String folderText, String nameText) {
        if (StringUtils.isBlank(folderText)) {
            return null;
        }
        File folder = new File(folderText.trim());
        if (StringUtils.isBlank(nameText)) {
            return folder.isDirectory() ? folder : null;
        }
        String name = nameText.trim();
        if (name.contains(File.separator) || name.contains("/")) {
            return CardUtil.resolveFilePath(name);
        }
        return new File(folder, name);
    }

    private static File directoryForPath(String pathText, String defaultBaseDir) {
        if (StringUtils.isBlank(pathText)) {
            return new File(defaultBaseDir);
        }
        File file = new File(pathText.trim());
        if (file.isDirectory()) {
            return file;
        }
        File parent = file.getParentFile();
        if (parent != null && parent.isDirectory()) {
            return parent;
        }
        return new File(defaultBaseDir);
    }
}
