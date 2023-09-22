package forge.adventure.scene;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.github.tommyettinger.textra.TextraButton;
import com.github.tommyettinger.textra.TextraLabel;
import forge.Forge;
import forge.Graphics;
import forge.adventure.util.Config;
import forge.adventure.util.Controls;
import forge.assets.ImageCache;
import forge.gui.GuiBase;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.sound.SoundSystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Scene to handle settings of the base forge and adventure mode
 */
public class SettingsScene extends UIScene {
    private final Table settingGroup;
    TextraButton backButton;
    //TextraButton newPlane;
    ScrollPane scrollPane;

    SelectBox<String> selectSourcePlane;
    TextField newPlaneName;
    Dialog createNewPlane, copyPlane, errorDialog;

    private void copyNewPlane() {
        String plane = selectSourcePlane.getSelected();
        Path source = Paths.get(Config.instance().getPlanePath(plane));
        Path destination = Paths.get(Config.instance().getPlanePath("<user>" + newPlaneName.getText()));
        AtomicBoolean somethingWentWrong = new AtomicBoolean(false);
        try (Stream<Path> stream = Files.walk(source)) {
            Files.createDirectories(destination);
            stream.forEach(s -> {
                try {
                    Files.copy(s, destination.resolve(source.relativize(s)), REPLACE_EXISTING);
                } catch (IOException e) {
                    somethingWentWrong.set(true);
                }
            });
        } catch (IOException e) {
            somethingWentWrong.set(true);
        }
        if (somethingWentWrong.get()) {
            if (errorDialog == null) {
                errorDialog = createGenericDialog("Something went wrong", "Copy was not successful check your access right\n and if the folder is in use",
                        Forge.getLocalizer().getMessage("lblOk"), Forge.getLocalizer().getMessage("lblAbort"), this::removeDialog, this::removeDialog);
            }
            showDialog(errorDialog);
        } else {
            if (copyPlane == null) {
                copyPlane = createGenericDialog("Copied plane", "New plane " + newPlaneName.getText() +
                                " was created\nYou can now start the editor to change the plane\n" +
                                "or edit it manually from the folder\n" + Config.instance().getPlanePath("<user>" + newPlaneName.getText()),
                        Forge.getLocalizer().getMessage("lblOk"), Forge.getLocalizer().getMessage("lblAbort"), this::removeDialog, this::removeDialog);
            }
            Config.instance().getSettingData().plane = "<user>" + newPlaneName.getText();
            Config.instance().saveSettings();
            showDialog(copyPlane);
        }
    }

    private void createNewPlane() {
        if (createNewPlane == null) {
            createNewPlane = createGenericDialog("Create your own Plane", "Select a plane to copy",
                    Forge.getLocalizer().getMessage("lblOk"),
                    Forge.getLocalizer().getMessage("lblAbort"), () -> {
                        this.copyNewPlane();
                        removeDialog();
                    }, this::removeDialog);
            createNewPlane.getContentTable().row();
            createNewPlane.getContentTable().add(selectSourcePlane);
            createNewPlane.getContentTable().row();
            createNewPlane.text("Set new plane name");
            createNewPlane.getContentTable().row();
            createNewPlane.getContentTable().add(newPlaneName);
            newPlaneName.setText(selectSourcePlane.getSelected() + "_copy");
        }
        showDialog(createNewPlane);
    }

    private SettingsScene() {
        super(Forge.isLandscapeMode() ? "ui/settings.json" : "ui/settings_portrait.json");

        settingGroup = new Table();
        //temporary disable custom world until it works correctly on each update
        /*selectSourcePlane = Controls.newComboBox();
        newPlaneName = Controls.newTextField("");

        selectSourcePlane.setItems(Config.instance().getAllAdventures());
        SelectBox plane = Controls.newComboBox(Config.instance().getAllAdventures(), Config.instance().getSettingData().plane, o -> {
            Config.instance().getSettingData().plane = (String) o;
            Config.instance().saveSettings();
            return null;
        });
        newPlane = Controls.newTextButton("Create own plane");
        newPlane.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                createNewPlane();
            }
        });
        addLabel(Forge.getLocalizer().getMessage("lblWorld"));
        settingGroup.add(plane).align(Align.right).pad(2);
        addLabel(Forge.getLocalizer().getMessage("lblCreate") + Forge.getLocalizer().getMessage("lblWorld"));
        settingGroup.add(newPlane).align(Align.right).pad(2);*/

        if (!GuiBase.isAndroid()) {
            SelectBox<String> videomode = Controls.newComboBox(ForgeConstants.VIDEO_MODES, Config.instance().getSettingData().videomode, o -> {
                String mode = (String) o;
                if (mode == null)
                    mode = "720p";
                Graphics.setVideoMode(mode);

                //update
                if (!FModel.getPreferences().getPref(ForgePreferences.FPref.UI_VIDEO_MODE).equalsIgnoreCase(mode)) {
                    FModel.getPreferences().setPref(ForgePreferences.FPref.UI_VIDEO_MODE, mode);
                    FModel.getPreferences().save();
                }
                return null;
            });
            addLabel(Forge.getLocalizer().getMessage("lblVideoMode"));
            settingGroup.add(videomode).align(Align.right).pad(2);
        }
        if (Forge.isLandscapeMode()) {
            //different adjustment to landscape
            SelectBox<Float> rewardCardAdjLandscape = Controls.newComboBox(new Float[]{0.6f, 0.65f, 0.7f, 0.75f, 0.8f, 0.85f, 0.9f, 0.95f, 1f, 1.05f, 1.1f, 1.15f, 1.2f, 1.25f, 1.3f, 1.35f, 1.4f, 1.45f, 1.5f, 1.55f, 1.6f}, Config.instance().getSettingData().rewardCardAdjLandscape, o -> {
                Float val = (Float) o;
                if (val == null || val == 0f)
                    val = 1f;
                Config.instance().getSettingData().rewardCardAdjLandscape = val;
                Config.instance().saveSettings();
                return null;
            });
            addLabel("Reward/Shop Card Display Ratio");
            settingGroup.add(rewardCardAdjLandscape).align(Align.right).pad(2);
            SelectBox<Float> tooltipAdjLandscape = Controls.newComboBox(new Float[]{0.6f, 0.65f, 0.7f, 0.75f, 0.8f, 0.85f, 0.9f, 0.95f, 1f, 1.05f, 1.1f, 1.15f, 1.2f, 1.25f, 1.3f, 1.35f, 1.4f, 1.45f, 1.5f, 1.55f, 1.6f}, Config.instance().getSettingData().cardTooltipAdjLandscape, o -> {
                Float val = (Float) o;
                if (val == null || val == 0f)
                    val = 1f;
                Config.instance().getSettingData().cardTooltipAdjLandscape = val;
                Config.instance().saveSettings();
                return null;
            });
            addLabel("Reward/Shop Card Tooltip Ratio");
            settingGroup.add(tooltipAdjLandscape).align(Align.right).pad(2);
        } else {
            //portrait adjustment
            SelectBox<Float> rewardCardAdj = Controls.newComboBox(new Float[]{0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1f, 1.1f, 1.2f, 1.3f, 1.4f, 1.5f, 1.6f, 1.8f, 1.9f, 2f}, Config.instance().getSettingData().rewardCardAdj, o -> {
                Float val = (Float) o;
                if (val == null || val == 0f)
                    val = 1f;
                Config.instance().getSettingData().rewardCardAdj = val;
                Config.instance().saveSettings();
                return null;
            });
            addLabel("Reward/Shop Card Display Ratio");
            settingGroup.add(rewardCardAdj).align(Align.right).pad(2);
            SelectBox<Float> tooltipAdj = Controls.newComboBox(new Float[]{0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1f, 1.1f, 1.2f, 1.3f, 1.4f, 1.5f, 1.6f, 1.8f, 1.9f, 2f}, Config.instance().getSettingData().cardTooltipAdj, o -> {
                Float val = (Float) o;
                if (val == null || val == 0f)
                    val = 1f;
                Config.instance().getSettingData().cardTooltipAdj = val;
                Config.instance().saveSettings();
                return null;
            });
            addLabel("Reward/Shop Card Tooltip Ratio");
            settingGroup.add(tooltipAdj).align(Align.right).pad(2);
        }
        if (!GuiBase.isAndroid()) {
            addSettingField(Forge.getLocalizer().getMessage("lblFullScreen"), Config.instance().getSettingData().fullScreen, new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    boolean value = ((CheckBox) actor).isChecked();
                    Config.instance().getSettingData().fullScreen = value;
                    Config.instance().saveSettings();
                    //update
                    FModel.getPreferences().setPref(ForgePreferences.FPref.UI_FULLSCREEN_MODE, Config.instance().getSettingData().fullScreen);
                    FModel.getPreferences().save();
                }
            });
        }
        addSettingField(Forge.getLocalizer().getMessage("lblDay") + " | " + Forge.getLocalizer().getMessage("lblNight") + " " + Forge.getLocalizer().getMessage("lblBackgroundImage"), Config.instance().getSettingData().dayNightBG, new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                boolean value = ((CheckBox) actor).isChecked();
                Config.instance().getSettingData().dayNightBG = value;
                Config.instance().saveSettings();
                if (value) {
                    updateBG(true);
                }
            }
        });
        addSettingField(Forge.getLocalizer().getMessage("lblDisableWinLose"), Config.instance().getSettingData().disableWinLose, new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Config.instance().getSettingData().disableWinLose = ((CheckBox) actor).isChecked();
                Config.instance().saveSettings();
            }
        });
        addSettingField(Forge.getLocalizer().getMessage("lblShowShopOverlay"), Config.instance().getSettingData().showShopOverlay, new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Config.instance().getSettingData().showShopOverlay = ((CheckBox) actor).isChecked();
                Config.instance().saveSettings();
            }
        });
        addCheckBox(Forge.getLocalizer().getMessage("lblCardName"), ForgePreferences.FPref.UI_OVERLAY_CARD_NAME);
        addSettingSlider(Forge.getLocalizer().getMessage("cbAdjustMusicVolume"), ForgePreferences.FPref.UI_VOL_MUSIC, 0, 100);
        addSettingSlider(Forge.getLocalizer().getMessage("cbAdjustSoundsVolume"), ForgePreferences.FPref.UI_VOL_SOUNDS, 0, 100);
        addCheckBox(Forge.getLocalizer().getMessage("lblManaCost"), ForgePreferences.FPref.UI_OVERLAY_CARD_MANA_COST);
        addCheckBox(Forge.getLocalizer().getMessage("lblPowerOrToughness"), ForgePreferences.FPref.UI_OVERLAY_CARD_POWER);
        addCheckBox(Forge.getLocalizer().getMessage("lblCardID"), ForgePreferences.FPref.UI_OVERLAY_CARD_ID);
        addCheckBox(Forge.getLocalizer().getMessage("lblAbilityIcon"), ForgePreferences.FPref.UI_OVERLAY_ABILITY_ICONS);
        addCheckBox(Forge.getLocalizer().getMessage("cbImageFetcher"), ForgePreferences.FPref.UI_ENABLE_ONLINE_IMAGE_FETCHER);


        if (!GuiBase.isAndroid()) {
            addCheckBox(Forge.getLocalizer().getMessage("lblBattlefieldTextureFiltering"), ForgePreferences.FPref.UI_LIBGDX_TEXTURE_FILTERING);
            addCheckBox(Forge.getLocalizer().getMessage("lblAltZoneTabs"), ForgePreferences.FPref.UI_ALT_PLAYERZONETABS);
        }

        addCheckBox(Forge.getLocalizer().getMessage("lblLandscapeMode"), ForgePreferences.FPref.UI_LANDSCAPE_MODE);
        addCheckBox(Forge.getLocalizer().getMessage("lblAnimatedCardTapUntap"), ForgePreferences.FPref.UI_ANIMATED_CARD_TAPUNTAP);
        if (!GuiBase.isAndroid()) {
            final String[] item = {FModel.getPreferences().getPref(ForgePreferences.FPref.UI_ENABLE_BORDER_MASKING)};
            SelectBox<String> borderMask = Controls.newComboBox(new String[]{"Off", "Crop", "Full", "Art"}, item[0], o -> {
                String mode = (String) o;
                if (mode == null)
                    mode = "Crop";
                item[0] = mode;
                //update
                if (!FModel.getPreferences().getPref(ForgePreferences.FPref.UI_ENABLE_BORDER_MASKING).equalsIgnoreCase(mode)) {
                    FModel.getPreferences().setPref(ForgePreferences.FPref.UI_ENABLE_BORDER_MASKING, mode);
                    FModel.getPreferences().save();
                    Forge.enableUIMask = FModel.getPreferences().getPref(ForgePreferences.FPref.UI_ENABLE_BORDER_MASKING);
                }
                ImageCache.clearGeneratedCards();
                ImageCache.disposeTextures();
                return null;
            });
            addLabel(Forge.getLocalizer().getMessage("lblBorderMaskOption"));
            settingGroup.add(borderMask).align(Align.right).pad(2);

            addCheckBox(Forge.getLocalizer().getMessage("lblPreloadExtendedArtCards"), ForgePreferences.FPref.UI_ENABLE_PRELOAD_EXTENDED_ART);
            addCheckBox(Forge.getLocalizer().getMessage("lblAutoCacheSize"), ForgePreferences.FPref.UI_AUTO_CACHE_SIZE);
            addCheckBox(Forge.getLocalizer().getMessage("lblDisposeTextures"), ForgePreferences.FPref.UI_ENABLE_DISPOSE_TEXTURES);
        }


        settingGroup.row();
        backButton = ui.findActor("return");
        ui.onButtonPress("return", SettingsScene.this::back);

        scrollPane = ui.findActor("settings");
        scrollPane.setActor(settingGroup);
        addToSelectable(settingGroup);
    }


    public boolean back() {
        Forge.switchToLast();
        return true;
    }

    private void addInputField(String name, ForgePreferences.FPref pref) {
        TextField box = Controls.newTextField("");
        box.setText(FModel.getPreferences().getPref(pref));
        box.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                FModel.getPreferences().setPref(pref, ((TextField) actor).getText());
                FModel.getPreferences().save();
            }
        });

        addLabel(name);
        settingGroup.add(box).align(Align.right);
    }

    private void addCheckBox(String name, ForgePreferences.FPref pref) {
        CheckBox box = Controls.newCheckBox("");
        box.setChecked(FModel.getPreferences().getPrefBoolean(pref));
        box.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                FModel.getPreferences().setPref(pref, ((CheckBox) actor).isChecked());
                FModel.getPreferences().save();
            }
        });

        addLabel(name);
        settingGroup.add(box).align(Align.right);
    }

    private void addSettingSlider(String name, ForgePreferences.FPref pref, int min, int max) {
        Slider slide = Controls.newSlider(min, max, 1, false);
        slide.setValue(FModel.getPreferences().getPrefInt(pref));
        slide.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                FModel.getPreferences().setPref(pref, String.valueOf((int) ((Slider) actor).getValue()));
                FModel.getPreferences().save();
                if (ForgePreferences.FPref.UI_VOL_MUSIC.equals(pref))
                    SoundSystem.instance.refreshVolume();
            }
        });
        addLabel(name);
        settingGroup.add(slide).align(Align.right);
    }

    private void addSettingField(String name, boolean value, ChangeListener change) {
        CheckBox box = Controls.newCheckBox("");
        box.setChecked(value);
        box.addListener(change);
        addLabel(name);
        settingGroup.add(box).align(Align.right);
    }

    private void addSettingField(String name, int value, ChangeListener change) {
        TextField text = Controls.newTextField(String.valueOf(value));
        text.setTextFieldFilter((textField, c) -> Character.isDigit(c));
        text.addListener(change);
        addLabel(name);
        settingGroup.add(text).align(Align.right);
    }

    void addLabel(String name) {
        TextraLabel label = Controls.newTextraLabel(name);
        label.setWrap(true);
        settingGroup.row().space(5);
        int w = Forge.isLandscapeMode() ? 160 : 80;
        settingGroup.add(label).align(Align.left).pad(2, 2, 2, 5).width(w).expand();
    }


    private static SettingsScene object;

    public static SettingsScene instance() {
        if (object == null)
            object = new SettingsScene();
        return object;
    }


    @Override
    public void dispose() {
        if (stage != null)
            stage.dispose();
    }

}
