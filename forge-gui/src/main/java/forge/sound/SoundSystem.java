package forge.sound;

import com.google.common.eventbus.Subscribe;
import forge.game.event.GameEvent;
import forge.gui.GuiBase;
import forge.gui.events.UiEvent;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.player.GamePlayerUtil;

import java.io.File;
import java.util.*;

/**
 * Manages playback of all sounds for the client.
 */
public class SoundSystem {
    public static final SoundSystem instance = new SoundSystem();

    public static final int DELAY = 30;

    private static final IAudioClip emptySound = new NoSoundClip();
    private static final Map<SoundEffectType, IAudioClip> loadedClips = new EnumMap<>(SoundEffectType.class);
    private static final Map<String, IAudioClip> loadedScriptClips = new HashMap<>();

    private final EventVisualizer visualizer;

    private SoundSystem() {
        this.visualizer = new EventVisualizer(GamePlayerUtil.getGuiPlayer());
    }
    private static boolean isUsingAltSystem() {
        return !GuiBase.getInterface().isLibgdxPort() && FModel.getPreferences().getPrefBoolean(FPref.UI_ALT_SOUND_SYSTEM);
    }

    /**
     * Fetch a resource based on the sound effect type from the SoundEffectType enumeration.
     *
     * @param type the sound effect type.
     * @return a clip associated with the loaded resource, or emptySound if the resource
     *         was unavailable or failed to load.
     */
    protected IAudioClip fetchResource(final SoundEffectType type) {
        if (GuiBase.getInterface().isLibgdxPort()) {
            if (FModel.getPreferences().getPrefInt(FPref.UI_VOL_SOUNDS)<1) {
                return emptySound;
            }
        } else {
            if (!FModel.getPreferences().getPrefBoolean(FPref.UI_ENABLE_SOUNDS)) {
                return emptySound;
            }
        }

        IAudioClip clip = loadedClips.get(type);
        if (clip == null) { // cache miss
            final String resource = type.getResourceFileName();
            clip = GuiBase.getInterface().createAudioClip(resource);
            if (clip == null) {
                clip = emptySound;
            }
            loadedClips.put(type, clip);
        }
        return clip;
    }

    /**
     * Fetch a resource based on the file name (for SVar:SoundEffect script variables).
     * @param fileName the file name of the resource
     * @return a clip associated with the loaded resource, or emptySound if the resource
     *         was unavailable or failed to load.
     */
    protected IAudioClip fetchResource(final String fileName) {
        if (GuiBase.getInterface().isLibgdxPort()) {
            if (FModel.getPreferences().getPrefInt(FPref.UI_VOL_SOUNDS)<1) {
                return emptySound;
            }
        } else {
            if (!FModel.getPreferences().getPrefBoolean(FPref.UI_ENABLE_SOUNDS)) {
                return emptySound;
            }
        }

        IAudioClip clip = loadedScriptClips.get(fileName);
        if (null == clip) { // cache miss
            clip = GuiBase.getInterface().createAudioClip(fileName);
            if (clip == null) {
                clip = emptySound;
            }
            loadedScriptClips.put(fileName, clip);
        }
        return clip;
    }

    public boolean hasResource(final SoundEffectType type) {
        boolean result = true;
        IAudioClip clip = fetchResource(type);
        if(clip.equals(emptySound)) {
            result = false;
        }
        return result;
    }
    
    /**
     * Play the sound associated with the resource specified by the file name
     * ("synchronized" with other sounds of the same kind means: only one can play at a time).
     */
    public void play(final String resourceFileName, final boolean isSynchronized) {
        if (isUsingAltSystem()) {
            GuiBase.getInterface().startAltSoundSystem(getSoundDirectory() + resourceFileName, isSynchronized);
        }
        else {
            final IAudioClip snd = fetchResource(resourceFileName);
            if (!isSynchronized || snd.isDone()) {
                snd.play(FModel.getPreferences().getPrefInt(FPref.UI_VOL_SOUNDS)/100f);
            }
        }
    }

    /**
     * Play the sound associated with the Sounds enumeration element.
     */
    public void play(final SoundEffectType type, final boolean isSynchronized) {
        if (isUsingAltSystem()) {
            GuiBase.getInterface().startAltSoundSystem(getSoundDirectory() + type.getResourceFileName(), isSynchronized);
        } else {
            final IAudioClip snd = fetchResource(type);
            if (!isSynchronized || snd.isDone()) {
                snd.play(FModel.getPreferences().getPrefInt(FPref.UI_VOL_SOUNDS)/100f);
            }
        }
    }

    /**
     * Play the sound in a looping manner until 'stop' is called.
     */
    public void loop(final String resourceFileName) {
        fetchResource(resourceFileName).loop();
    }

    /**
     * Play the sound in a looping manner until 'stop' is called.
     */
    public void loop(final SoundEffectType type) {
        fetchResource(type).loop();
    }

    /**
     * Stop the sound associated with the given resource file name.
     */
    public void stop(final String resourceFileName) {
        fetchResource(resourceFileName).stop();
    }

    /**
     * Stop the sound associated with the Sounds enumeration element.
     */
    public void stop(final SoundEffectType type) {
        fetchResource(type).stop();
    }

    @Subscribe
    public void receiveEvent(final GameEvent evt) {
        final SoundEffectType effect = evt.visit(visualizer);
        if (null == effect) {
            return;
        }
        if (effect == SoundEffectType.ScriptedEffect) {
            final String resourceName = visualizer.getScriptedSoundEffectName(evt);
            if (!resourceName.isEmpty()) {
                play(resourceName, false);
            }
        } else {
            play(effect, effect.isSynced());
        }
    }

    @Subscribe
    public void receiveEvent(final UiEvent evt) {
        final SoundEffectType effect = evt.visit(visualizer);
        if (null != effect) {
            play(effect, effect.isSynced());
        }
    }

    //Background Music
    private IAudioMusic currentTrack;
    private MusicPlaylist currentPlaylist;

    public void setBackgroundMusic(final MusicPlaylist playlist) {
        currentPlaylist = playlist;
        changeBackgroundTrack();
    }

    public void changeBackgroundTrack() {
        //ensure old track stopped and disposed of if needed
        if (currentTrack != null) {
            currentTrack.dispose();
            currentTrack = null;
        }

        if (currentPlaylist == null || GuiBase.getInterface().isLibgdxPort()
                ? FModel.getPreferences().getPrefInt(FPref.UI_VOL_MUSIC) < 1
                : !FModel.getPreferences().getPrefBoolean(FPref.UI_ENABLE_MUSIC)) {
            return;
        }

        final String filename = currentPlaylist.getRandomFilename();
        if (filename == null) { return; }

        try {
            currentTrack = GuiBase.getInterface().createAudioMusic(filename);
            currentTrack.play(new Runnable() {
                @Override public void run() {
                    try {
                        Thread.sleep(SoundSystem.DELAY);
                    } catch (final InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    changeBackgroundTrack(); //change track when music completes on its own
                }
            });
            currentTrack.setVolume(FModel.getPreferences().getPrefInt(FPref.UI_VOL_MUSIC)/100f);
        } catch (final Exception ex) {
            System.err.println("Unable to load music file: " + filename);
        }
    }

    public void pause() {
        if (currentTrack != null) {
            currentTrack.pause();
        }
    }

    public void resume() {
        if (currentTrack != null) {
            currentTrack.resume();
        }
    }

    public void dispose() {
        if (currentTrack != null) {
            currentTrack.dispose();
            currentTrack = null;
        }
    }

    public String[] getAvailableSoundSets()
    {
        final List<String> availableSets = new ArrayList<>();

        final File dir = new File(ForgeConstants.CACHE_SOUND_DIR);
        if (dir != null && dir.exists()) {
            final String[] files = dir.list();
            for (String fileName : files) {
                String fullPath = ForgeConstants.CACHE_SOUND_DIR + fileName;
                if (!fileName.equals("Default") && new File(fullPath).isDirectory()) {
                    availableSets.add(fileName);
                }
            }
        }

        Collections.sort(availableSets);
        availableSets.add(0, "Default");

        if (availableSets.size() == 1 || !availableSets.contains(FModel.getPreferences().getPref(FPref.UI_CURRENT_SOUND_SET))) {
            // Default profile only or the current set is no longer available - revert the preference setting to default
            FModel.getPreferences().setPref(FPref.UI_CURRENT_SOUND_SET, "Default");
            invalidateSoundCache();
        }

        return availableSets.toArray(new String[availableSets.size()]);
    }

    public String getSoundDirectory() {
        String profileName = FModel.getPreferences().getPref(FPref.UI_CURRENT_SOUND_SET);
        if (profileName.equals("Default")) {
            return ForgeConstants.SOUND_DIR;
        } else {
            return ForgeConstants.CACHE_SOUND_DIR + profileName + ForgeConstants.PATH_SEPARATOR;
        }
    }

    public void invalidateSoundCache() {
        loadedClips.clear();
        loadedScriptClips.clear();
    }

    public String getMusicDirectory() {
        String profileName = FModel.getPreferences().getPref(ForgePreferences.FPref.UI_CURRENT_MUSIC_SET);
        if (profileName.equals("Default")) {
            return ForgeConstants.MUSIC_DIR;
        } else {
            return ForgeConstants.CACHE_MUSIC_DIR + profileName + ForgeConstants.PATH_SEPARATOR;
        }
    }

    public static String[] getAvailableMusicSets()
    {
        final List<String> availableSets = new ArrayList<>();

        final File dir = new File(ForgeConstants.CACHE_MUSIC_DIR);
        if (dir != null && dir.exists()) {
            final String[] files = dir.list();
            for (String fileName : files) {
                String fullPath = ForgeConstants.CACHE_MUSIC_DIR + fileName;
                if (!fileName.equals("Default") && new File(fullPath).isDirectory()) {
                    availableSets.add(fileName);
                }
            }
        }

        Collections.sort(availableSets);
        availableSets.add(0, "Default");

        if (availableSets.size() == 1 || !availableSets.contains(FModel.getPreferences().getPref(FPref.UI_CURRENT_MUSIC_SET))) {
            // Default profile only or the current set is no longer available - revert the preference setting to default
            FModel.getPreferences().setPref(FPref.UI_CURRENT_MUSIC_SET, "Default");
            MusicPlaylist.invalidateMusicPlaylist();
        }

        return availableSets.toArray(new String[availableSets.size()]);
    }
}
