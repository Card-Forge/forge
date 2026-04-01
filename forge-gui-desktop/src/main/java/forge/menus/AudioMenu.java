package forge.menus;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingConstants;

import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.sound.MusicPlaylist;
import forge.sound.SoundSystem;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedLabel;
import forge.toolbox.FSkin.SkinnedSlider;
import forge.util.Localizer;

public final class AudioMenu {
    private static final ForgePreferences prefs = FModel.getPreferences();

    public JMenu getMenu() {
        final Localizer localizer = Localizer.getInstance();
        final JMenu menu = new JMenu(localizer.getMessage("lblAudio"));
        menu.setMnemonic(KeyEvent.VK_A);
        menu.add(getMenu_SoundSetOptions());
        menu.add(getMenu_MusicSetOptions());
        menu.addSeparator();
        int soundsVol = prefs.getPrefBoolean(FPref.UI_ENABLE_SOUNDS) ? prefs.getPrefInt(FPref.UI_VOL_SOUNDS) : 0;
        int musicVol = prefs.getPrefBoolean(FPref.UI_ENABLE_MUSIC) ? prefs.getPrefInt(FPref.UI_VOL_MUSIC) : 0;
        menu.add(buildSliderPanel(
                localizer.getMessage("cbAdjustSoundsVolume"),
                soundsVol,
                value -> {
                    prefs.setPref(FPref.UI_VOL_SOUNDS, String.valueOf(value));
                    prefs.setPref(FPref.UI_ENABLE_SOUNDS, value > 0);
                    prefs.save();
                }));
        menu.add(buildSliderPanel(
                localizer.getMessage("cbAdjustMusicVolume"),
                musicVol,
                value -> {
                    prefs.setPref(FPref.UI_VOL_MUSIC, String.valueOf(value));
                    prefs.setPref(FPref.UI_ENABLE_MUSIC, value > 0);
                    prefs.save();
                    SoundSystem.instance.refreshVolume();
                }));
        return menu;
    }

    private static JMenu getMenu_SoundSetOptions() {
        final Localizer localizer = Localizer.getInstance();
        final JMenu menu = new JMenu(localizer.getMessage("cbpSoundSets"));
        final ButtonGroup group = new ButtonGroup();
        final String currentSet = prefs.getPref(FPref.UI_CURRENT_SOUND_SET);
        for (final String set : SoundSystem.instance.getAvailableSoundSets()) {
            JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(set);
            group.add(menuItem);
            if (set.equals(currentSet)) {
                menuItem.setSelected(true);
            }
            menuItem.setActionCommand(set);
            menuItem.addActionListener(e -> {
                prefs.setPref(FPref.UI_CURRENT_SOUND_SET, e.getActionCommand());
                prefs.save();
                SoundSystem.instance.invalidateSoundCache();
            });
            menu.add(menuItem);
        }
        return menu;
    }

    private static JMenu getMenu_MusicSetOptions() {
        final Localizer localizer = Localizer.getInstance();
        final JMenu menu = new JMenu(localizer.getMessage("cbpMusicSets"));
        final ButtonGroup group = new ButtonGroup();
        final String currentSet = prefs.getPref(FPref.UI_CURRENT_MUSIC_SET);
        for (final String set : SoundSystem.getAvailableMusicSets()) {
            JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(set);
            group.add(menuItem);
            if (set.equals(currentSet)) {
                menuItem.setSelected(true);
            }
            menuItem.setActionCommand(set);
            menuItem.addActionListener(e -> {
                prefs.setPref(FPref.UI_CURRENT_MUSIC_SET, e.getActionCommand());
                prefs.save();
                MusicPlaylist.invalidateMusicPlaylist();
                SoundSystem.instance.changeBackgroundTrack();
            });
            menu.add(menuItem);
        }
        return menu;
    }

    private static JPanel buildSliderPanel(String labelText, int initialValue, SliderCallback callback) {
        final java.awt.Color bg = javax.swing.UIManager.getColor("PopupMenu.background");
        final JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(bg);

        final SkinnedLabel label = new SkinnedLabel();
        label.setText(labelText);
        label.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        label.setFont(FSkin.getFont());
        label.setOpaque(true);
        label.setBackground(bg);

        final SkinnedSlider slider = new SkinnedSlider(SwingConstants.HORIZONTAL, 0, 100, initialValue);
        slider.setMajorTickSpacing(25);
        slider.setMinorTickSpacing(5);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setBackground(bg);
        slider.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        slider.setFont(FSkin.getFont());

        slider.addChangeListener(e -> {
            slider.repaint();
            callback.onValueChanged(slider.getValue());
        });

        panel.add(label, BorderLayout.NORTH);
        panel.add(slider, BorderLayout.CENTER);
        return panel;
    }

    @FunctionalInterface
    private interface SliderCallback {
        void onValueChanged(int value);
    }
}
