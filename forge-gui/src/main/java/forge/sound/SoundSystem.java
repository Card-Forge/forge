package forge.sound;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.google.common.eventbus.Subscribe;

import forge.GuiBase;
import forge.events.UiEvent;
import forge.game.event.GameEvent;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.properties.ForgeConstants;
import forge.properties.ForgePreferences.FPref;

/**
 * Manages playback of all sounds for the client.
 */
public class SoundSystem {
    public static final SoundSystem instance = new SoundSystem();

    public static final int DELAY = 30;

    private static final IAudioClip emptySound = new NoSoundClip();
    private static final Map<SoundEffectType, IAudioClip> loadedClips = new EnumMap<SoundEffectType, IAudioClip>(SoundEffectType.class);
    private static final Map<String, IAudioClip> loadedScriptClips = new HashMap<String, IAudioClip>();

    private final EventVisualizer visualizer;

    private SoundSystem() {
        this.visualizer = new EventVisualizer(GamePlayerUtil.getGuiPlayer());
    }
    private static boolean isUsingAltSystem() {
        return FModel.getPreferences().getPrefBoolean(FPref.UI_ALT_SOUND_SYSTEM);
    }

    /**
     * Fetch a resource based on the sound effect type from the SoundEffectType enumeration.
     *
     * @param type the sound effect type.
     * @return a clip associated with the loaded resource, or emptySound if the resource
     *         was unavailable or failed to load.
     */
    protected IAudioClip fetchResource(final SoundEffectType type) {
        if (!FModel.getPreferences().getPrefBoolean(FPref.UI_ENABLE_SOUNDS)) {
            return emptySound;
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
        if (!FModel.getPreferences().getPrefBoolean(FPref.UI_ENABLE_SOUNDS)) {
            return emptySound;
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

    /**
     * Play the sound associated with the resource specified by the file name
     * ("synchronized" with other sounds of the same kind means: only one can play at a time).
     */
    public void play(final String resourceFileName, final boolean isSynchronized) {
        if (isUsingAltSystem()) {
            GuiBase.getInterface().startAltSoundSystem(ForgeConstants.SOUND_DIR + resourceFileName, isSynchronized);
        }
        else {
            final IAudioClip snd = fetchResource(resourceFileName);
            if (!isSynchronized || snd.isDone()) {
                snd.play();
            }
        }
    }

    /**
     * Play the sound associated with the Sounds enumeration element.
     */
    public void play(final SoundEffectType type, final boolean isSynchronized) {
        if (isUsingAltSystem()) {
            GuiBase.getInterface().startAltSoundSystem(ForgeConstants.SOUND_DIR + type.getResourceFileName(), isSynchronized);
        }
        else {
            final IAudioClip snd = fetchResource(type);
            if (!isSynchronized || snd.isDone()) {
                snd.play();
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

        if (currentPlaylist == null || !FModel.getPreferences().getPrefBoolean(FPref.UI_ENABLE_MUSIC)) {
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
}
