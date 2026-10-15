package forge.adventure.scene;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.github.tommyettinger.textra.TextraButton;
import com.github.tommyettinger.textra.TextraLabel;
import forge.Forge;
import forge.Graphics;
import forge.adventure.data.RewardData;
import forge.adventure.util.AdventureBackgroundDownloader;
import forge.adventure.util.Config;
import forge.adventure.util.Controls;
import forge.assets.FSkinTexture;
import forge.assets.ImageCache;
import forge.gui.GuiBase;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.screens.match.CardFlightOverlay;
import forge.sound.SoundSystem;
import forge.util.Localizer;

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
    TextraButton newPlane;
    ScrollPane scrollPane;

    SelectBox<String> selectSourcePlane;
    TextField newPlaneName;
    Dialog createNewPlane, copyPlane, errorDialog, restartDialog;

    private void copyNewPlane() {
        Localizer localizer = Forge.getLocalizer();
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
                        localizer.getMessage("lblOK"), localizer.getMessage("lblAbort"), this::removeDialog, this::removeDialog);
            }
            showDialog(errorDialog);
        } else {
            if (copyPlane == null) {
                copyPlane = createGenericDialog("Copied plane", "New plane " + newPlaneName.getText() +
                                " was created\nYou can now start the editor to change the plane\n" +
                                "or edit it manually from the folder\n" + Config.instance().getPlanePath("<user>" + newPlaneName.getText()),
                        localizer.getMessage("lblOK"), localizer.getMessage("lblAbort"), this::removeDialog, this::removeDialog);
            }
            Config.instance().getSettingData().plane = "<user>" + newPlaneName.getText();
            Config.instance().saveSettings();
            Forge.getLocalizer().loadAdventureBundle(Config.instance().getPlanePath(Config.instance().getSettingData().plane) + "languages/");
            showDialog(copyPlane);
        }
    }

    private void createNewPlane() {
        if (createNewPlane == null) {
            createNewPlane = createGenericDialog("Create your own Plane", "Select a plane to copy",
                    Forge.getLocalizer().getMessage("lblOK"),
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
        Localizer localizer = Forge.getLocalizer();

        settingGroup = new Table();
        selectSourcePlane = Controls.newComboBox();
        newPlaneName = Controls.newTextField("");
        selectSourcePlane.setItems(Config.instance().getAllAdventures());
        SelectBox plane = Controls.newComboBox(Config.instance().getAllAdventures(), Config.instance().getSettingData().plane, o -> {
            Config.instance().getSettingData().plane = (String) o;
            Config.instance().saveSettings();
            Forge.getLocalizer().loadAdventureBundle(Config.instance().getPlanePath((String) o) + "languages/");
            return null;
        });
        plane.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                restartForge();
            }
        });
        /*newPlane = Controls.newTextButton("Create own plane");
        newPlane.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                createNewPlane();
            }
        });*/
        addLabel(localizer.getMessage("lblWorld") + " (" + localizer.getMessage("lblRestartRequired") + ")");
        settingGroup.add(plane).align(Align.right).pad(2);
        //addLabel(localizer.getMessage("lblCreate") + localizer.getMessage("lblWorld"));
        settingGroup.add(newPlane).align(Align.right).pad(2);

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
            addLabel(localizer.getMessage("lblVideoMode"));
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
            addLabel(localizer.getMessage("advRewardShopCardDisplayRatio"));
            settingGroup.add(rewardCardAdjLandscape).align(Align.right).pad(2);
            SelectBox<Float> tooltipAdjLandscape = Controls.newComboBox(new Float[]{0.6f, 0.65f, 0.7f, 0.75f, 0.8f, 0.85f, 0.9f, 0.95f, 1f, 1.05f, 1.1f, 1.15f, 1.2f, 1.25f, 1.3f, 1.35f, 1.4f, 1.45f, 1.5f, 1.55f, 1.6f}, Config.instance().getSettingData().cardTooltipAdjLandscape, o -> {
                Float val = (Float) o;
                if (val == null || val == 0f)
                    val = 1f;
                Config.instance().getSettingData().cardTooltipAdjLandscape = val;
                Config.instance().saveSettings();
                return null;
            });
            addLabel(localizer.getMessage("advRewardShopCardTooltipRatio"));
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
            addLabel(localizer.getMessage("advRewardShopCardDisplayRatio"));
            settingGroup.add(rewardCardAdj).align(Align.right).pad(2);
            SelectBox<Float> tooltipAdj = Controls.newComboBox(new Float[]{0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1f, 1.1f, 1.2f, 1.3f, 1.4f, 1.5f, 1.6f, 1.8f, 1.9f, 2f}, Config.instance().getSettingData().cardTooltipAdj, o -> {
                Float val = (Float) o;
                if (val == null || val == 0f)
                    val = 1f;
                Config.instance().getSettingData().cardTooltipAdj = val;
                Config.instance().saveSettings();
                return null;
            });
            addLabel(localizer.getMessage("advRewardShopCardTooltipRatio"));
            settingGroup.add(tooltipAdj).align(Align.right).pad(2);
        }
        if (!GuiBase.isAndroid()) {
            addSettingField(localizer.getMessage("lblFullScreen"), Config.instance().getSettingData().fullScreen, new ChangeListener() {
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
        addSettingField(localizer.getMessage("lblDay") + " | " + localizer.getMessage("lblNight") + " " + localizer.getMessage("lblBackgroundImage"), Config.instance().getSettingData().dayNightBG, new ChangeListener() {
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
        String off = localizer.getMessage("lblOff");
        String planeDefault = localizer.getMessage("lblPlaneDefault");
        String customSource = localizer.getMessage("lblCustomSource");
        boolean useCustomSource = Config.instance().isUsingCustomBattleBackgroundSource();
        String configuredSource = Config.instance().getCustomBattleBackgroundSource();
        SelectBox<String> backgroundSource = Controls.newComboBox();
        backgroundSource.setItems(off, planeDefault, customSource);
        backgroundSource.setSelected(!Config.instance().isExtraBattleBackgroundsEnabled()
                ? off : useCustomSource ? customSource : planeDefault);
        TextField backgroundSourceUrl = Controls.newTextField(configuredSource == null ? "" : configuredSource);
        addLabel(localizer.getMessage("lblBattleBackgrounds", Config.instance().getPlane()));
        settingGroup.add(backgroundSource).align(Align.right).pad(2);
        Cell<TextraLabel> backgroundUrlLabelCell = addLabel(localizer.getMessage("lblBattleBackgroundIndexUrl"));
        TextraLabel backgroundUrlLabel = backgroundUrlLabelCell.getActor();
        Cell<TextField> backgroundUrlCell = settingGroup.add(backgroundSourceUrl).align(Align.right).pad(2);
        Runnable updateBackgroundUrlVisibility = () -> {
            boolean custom = customSource.equals(backgroundSource.getSelected());
            backgroundUrlLabel.setVisible(custom);
            backgroundSourceUrl.setVisible(custom);
            backgroundUrlLabelCell.height(custom ? Value.prefHeight : Value.zero)
                    .padTop(custom ? 2 : 0).padBottom(custom ? 2 : 0)
                    .spaceTop(custom ? 5 : 0).spaceBottom(custom ? 5 : 0);
            backgroundUrlCell.height(custom ? Value.prefHeight : Value.zero)
                    .padTop(custom ? 2 : 0).padBottom(custom ? 2 : 0)
                    .spaceTop(custom ? 5 : 0).spaceBottom(custom ? 5 : 0);
            settingGroup.invalidateHierarchy();
        };
        updateBackgroundUrlVisibility.run();
        backgroundSource.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                boolean custom = customSource.equals(((SelectBox<?>) actor).getSelected());
                boolean enabled = !off.equals(backgroundSource.getSelected());
                updateBackgroundUrlVisibility.run();
                Config.instance().setExtraBattleBackgroundsEnabled(enabled);
                if (enabled) {
                    Config.instance().setUseCustomBattleBackgroundSource(custom);
                }
                Config.instance().saveSettings();
                AdventureBackgroundDownloader.cancel();
                FSkinTexture.invalidateAdventureTextures();
            }
        });
        backgroundSourceUrl.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (customSource.equals(backgroundSource.getSelected())) {
                    Config.instance().setBattleBackgroundSource(((TextField) actor).getText());
                    Config.instance().saveSettings();
                    AdventureBackgroundDownloader.cancel();
                }
            }
        });
        addSettingField(localizer.getMessage("lblDisableWinLose"), Config.instance().getSettingData().disableWinLose, new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Config.instance().getSettingData().disableWinLose = ((CheckBox) actor).isChecked();
                Config.instance().saveSettings();
            }
        });
        addSettingField(localizer.getMessage("lblDisableNotForSaleOverlay"),
                Config.instance().getSettingData().disableNotForSale, new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Config.instance().getSettingData().disableNotForSale = ((CheckBox) actor).isChecked();
                Config.instance().saveSettings();
            }
        });
        addSettingField(localizer.getMessage("lblShowShopOverlay"), Config.instance().getSettingData().showShopOverlay, new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Config.instance().getSettingData().showShopOverlay = ((CheckBox) actor).isChecked();
                Config.instance().saveSettings();
            }
        });
        addSettingField(localizer.getMessage("lblUseAllCardVariants"), Config.instance().getSettingData().useAllCardVariants, new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Config.instance().getSettingData().useAllCardVariants = ((CheckBox) actor).isChecked();
                Config.instance().saveSettings();
                RewardData.invalidateCardPool();
            }
        });
        addSettingField(localizer.getMessage("lblPreferEraMatchedTokenArt"), Config.instance().getSettingData().preferEraMatchedTokenArt, new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                boolean enabled = ((CheckBox) actor).isChecked();
                Config.instance().getSettingData().preferEraMatchedTokenArt = enabled;
                forge.model.FModel.getMagicDb().getAllTokens().setPreferEraMatchedArt(enabled);
                Config.instance().saveSettings();
            }
        });
        addSettingField(localizer.getMessage("lblExcludeAlchemyVariants"), Config.instance().getSettingData().excludeAlchemyVariants, new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Config.instance().getSettingData().excludeAlchemyVariants = ((CheckBox) actor).isChecked();
                Config.instance().saveSettings();
                RewardData.invalidateCardPool();
            }
        });
        addCheckBox(localizer.getMessage("lblEnableUnknownCards") + " (" +
            localizer.getMessage("lblRestartRequired") + ")", ForgePreferences.FPref.UI_LOAD_UNKNOWN_CARDS, this::restartForge);
        addCheckBox(localizer.getMessage("lblEnableNonLegalCards") + " (" +
            localizer.getMessage("lblRestartRequired") + ")", ForgePreferences.FPref.UI_LOAD_NONLEGAL_CARDS, this::restartForge);
        addSettingField(localizer.getMessage("lblGenerateLDADecks"), Config.instance().getSettingData().generateLDADecks, new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Config.instance().getSettingData().generateLDADecks = ((CheckBox) actor).isChecked();
                Config.instance().saveSettings();
            }
        });
        addSettingField(localizer.getMessage("lbldisableCrackedItems"), Config.instance().getSettingData().disableCrackedItems, new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Config.instance().getSettingData().disableCrackedItems = ((CheckBox) actor).isChecked();
                Config.instance().saveSettings();
            }
        });
        addSettingField(localizer.getMessage("lblBindEquipmentLoadoutsToDecks"), Config.instance().getSettingData().bindEquipmentLoadoutsToDecks, new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Config.instance().getSettingData().bindEquipmentLoadoutsToDecks = ((CheckBox) actor).isChecked();
                Config.instance().saveSettings();
            }
        });
        addSettingField(localizer.getMessage("lblDrawChevronsToHiddenEnemiesInClearQuest"), Config.instance().getSettingData().drawChevronsToHiddenEnemiesInClearQuest, new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Config.instance().getSettingData().drawChevronsToHiddenEnemiesInClearQuest = ((CheckBox) actor).isChecked();
                Config.instance().saveSettings();
            }
        });
        CheckBox cbAnte = addCheckBox(localizer.getMessage("cbAnte"), ForgePreferences.FPref.UI_ANTE);
        CheckBox cbAnteMatchRarity = addCheckBox(localizer.getMessage("cbAnteMatchRarity"), ForgePreferences.FPref.UI_ANTE_MATCH_RARITY);
        CheckBox cbAnteIncludeBasicLands = addCheckBox(localizer.getMessage("cbAnteIncludeBasicLands"), ForgePreferences.FPref.UI_ANTE_INCLUDE_BASIC_LANDS);
        boolean anteEnabled = FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_ANTE);
        cbAnteMatchRarity.setDisabled(!anteEnabled);
        cbAnteIncludeBasicLands.setDisabled(!anteEnabled);
        cbAnte.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                boolean enabled = ((CheckBox) actor).isChecked();
                cbAnteMatchRarity.setDisabled(!enabled);
                cbAnteIncludeBasicLands.setDisabled(!enabled);
                RewardData.invalidateCardPool();
            }
        });
        addCheckBox(localizer.getMessage("lblPromptAutoSell"), ForgePreferences.FPref.PROMPT_FOR_AUTOSELL);
        addCheckBox(localizer.getMessage("lblAutoSellVariantsCommander"), ForgePreferences.FPref.ADV_COMMANDER_AUTOSELL_VARIANT);
        addCheckBox(localizer.getMessage("lblShowCardPriceRewardScreen"), ForgePreferences.FPref.ADV_DISPLAY_PRICE_IN_REWARD_SCREEN);
        addCheckBox(localizer.getMessage("lblCardName"), ForgePreferences.FPref.UI_OVERLAY_CARD_NAME);
        addSettingSlider(localizer.getMessage("cbAdjustMusicVolume"), ForgePreferences.FPref.UI_VOL_MUSIC, 0, 100);
        addSettingSlider(localizer.getMessage("cbAdjustSoundsVolume"), ForgePreferences.FPref.UI_VOL_SOUNDS, 0, 100);
        addCheckBox(localizer.getMessage("cbShowAutoTapPreview"), ForgePreferences.FPref.UI_SHOW_AUTOTAP_PREVIEW);
        addCheckBox(localizer.getMessage("lblManaCost"), ForgePreferences.FPref.UI_OVERLAY_CARD_MANA_COST);
        addCheckBox(localizer.getMessage("lblPerpetualManaCost"), ForgePreferences.FPref.UI_OVERLAY_CARD_PERPETUAL_MANA_COST);
        addCheckBox(localizer.getMessage("lblPowerOrToughness"), ForgePreferences.FPref.UI_OVERLAY_CARD_POWER);
        addCheckBox(localizer.getMessage("lblCardID"), ForgePreferences.FPref.UI_OVERLAY_CARD_ID);
        addCheckBox(localizer.getMessage("lblAbilityIcon"), ForgePreferences.FPref.UI_OVERLAY_ABILITY_ICONS);
        addCheckBox(localizer.getMessage("cbImageFetcher"), ForgePreferences.FPref.UI_ENABLE_ONLINE_IMAGE_FETCHER);

        if (!GuiBase.isAndroid()) {
            addCheckBox(localizer.getMessage("lblBattlefieldTextureFiltering"), ForgePreferences.FPref.UI_LIBGDX_TEXTURE_FILTERING);
            //addCheckBox(localizer.getMessage("lblAltZoneTabs"), ForgePreferences.FPref.UI_ALT_PLAYERZONETABS);
        } else {
            addCheckBox(localizer.getMessage("lblLandscapeMode") + " (" +
                localizer.getMessage("lblRestartRequired") + ")",
                    ForgePreferences.FPref.UI_LANDSCAPE_MODE, () -> {
                        boolean landscapeMode = FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_LANDSCAPE_MODE);
                        //ensure device able to save off ini file so landscape change takes effect
                        Forge.getDeviceAdapter().setLandscapeMode(landscapeMode);
                        if (Forge.isLandscapeMode() != landscapeMode) {
                            restartForge();
                        }
                    });
        }

        addCheckBox(localizer.getMessage("lblAnimatedCardTapUntap"), ForgePreferences.FPref.UI_ANIMATED_CARD_TAPUNTAP);
        String currentAnim;
        switch (CardFlightOverlay.style()) { // maps legacy "true"/"false" values too
            case OFF: currentAnim = "Off"; break;
            case SLIDE: currentAnim = "Slide"; break;
            case POPUP: currentAnim = "Popup"; break;
            default: currentAnim = "Rotate"; break;
        }
        SelectBox<String> cardPlayAnim = Controls.newComboBox(new String[]{"Rotate", "Slide", "Popup", "Off"}, currentAnim, o -> {
            String mode = (String) o;
            if (mode == null)
                mode = "Rotate";
            if (!mode.equalsIgnoreCase(FModel.getPreferences().getPref(ForgePreferences.FPref.UI_CARD_PLAY_ANIMATION_STYLE))) {
                FModel.getPreferences().setPref(ForgePreferences.FPref.UI_CARD_PLAY_ANIMATION_STYLE, mode);
                FModel.getPreferences().save();
            }
            return null;
        });
        addLabel(localizer.getMessageorUseDefault("lblCardPlayOption", "Card Play Animation Style"));
        settingGroup.add(cardPlayAnim).align(Align.right).pad(2);
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
                ImageCache.getInstance().disposeTextures();
                return null;
            });
            addLabel(localizer.getMessage("lblBorderMaskOption"));
            settingGroup.add(borderMask).align(Align.right).pad(2);

            addCheckBox(localizer.getMessage("lblAutoCacheSize"), ForgePreferences.FPref.UI_AUTO_CACHE_SIZE);
            addCheckBox(localizer.getMessage("lblDisposeTextures"), ForgePreferences.FPref.UI_ENABLE_DISPOSE_TEXTURES);
        }


        addSettingSlider(localizer.getMessage("lblVibrationIntensity"), ForgePreferences.FPref.UI_VIBRATE_INTENSITY, 0, 100);
        addCheckBox(localizer.getMessage("lblVibrateAfterLongPress"), ForgePreferences.FPref.UI_VIBRATE_ON_LONG_PRESS);
        addCheckBox(localizer.getMessage("lblVibrateWhenLosingLife"), ForgePreferences.FPref.UI_VIBRATE_ON_LIFE_LOSS);
        addCheckBox(localizer.getMessage("lblVibrateOnEnemyEncounter"), ForgePreferences.FPref.UI_VIBRATE_ON_ENEMY_ENCOUNTER);
        addCheckBox(localizer.getMessage("lblVibrateOnAdventureReward"), ForgePreferences.FPref.UI_VIBRATE_ON_ADVENTURE_REWARD);
        addCheckBox(localizer.getMessage("lblVibrateOnShopAction"), ForgePreferences.FPref.UI_VIBRATE_ON_SHOP_ACTION);

        settingGroup.row();
        backButton = ui.findActor("return");
        ui.onButtonPress("return", SettingsScene.this::back);

        scrollPane = ui.findActor("settings");
        scrollPane.setActor(settingGroup);
        addToSelectable(settingGroup);
    }


    public boolean back() {
        GuiBase.setAdventureCacheDirectory(Config.instance().getCachePrefix());
        FSkinTexture.invalidateAdventureTextures();
        AdventureBackgroundDownloader.start();
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

    private CheckBox addCheckBox(String name, ForgePreferences.FPref pref) {
        return addCheckBox(name, pref, null);
    }

    private CheckBox addCheckBox(String name, ForgePreferences.FPref pref, Runnable runnable) {
        CheckBox box = Controls.newCheckBox("");
        box.setChecked(FModel.getPreferences().getPrefBoolean(pref));
        box.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                FModel.getPreferences().setPref(pref, ((CheckBox) actor).isChecked());
                FModel.getPreferences().save();
                if (runnable != null)
                    runnable.run();
            }
        });

        addLabel(name);
        settingGroup.add(box).align(Align.right);
        return box;
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

    private CheckBox addSettingField(String name, boolean value, ChangeListener change) {
        CheckBox box = Controls.newCheckBox("");
        box.setChecked(value);
        box.addListener(change);
        addLabel(name);
        settingGroup.add(box).align(Align.right);
        return box;
    }

    private void addSettingField(String name, int value, ChangeListener change) {
        TextField text = Controls.newTextField(String.valueOf(value));
        text.setTextFieldFilter((textField, c) -> Character.isDigit(c));
        text.addListener(change);
        addLabel(name);
        settingGroup.add(text).align(Align.right);
    }

    Cell<TextraLabel> addLabel(String name) {
        TextraLabel label = Controls.newTextraLabel(name);
        label.setWrap(true);
        settingGroup.row().space(5);
        int w = Forge.isLandscapeMode() ? 160 : 80;
        return settingGroup.add(label).align(Align.left).pad(2, 2, 2, 5).width(w).expand();
    }

    private void restartForge() {
        Localizer localizer = Forge.getLocalizer();
        if (restartDialog == null) {
            restartDialog = createGenericDialog("",
                    localizer.getMessage("lblAreYouSureYouWishRestartForge"),
                    localizer.getMessage("lblOK"),
                    localizer.getMessage("lblAbort"), () -> {
                        Forge.restart(true);
                        removeDialog();
                    }, this::removeDialog);
        }
        showDialog(restartDialog);
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
