package forge.sound;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.google.common.eventbus.Subscribe;

import forge.GuiBase;
import forge.events.UiEvent;
import forge.game.event.GameEvent;
import forge.interfaces.IGuiBase;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.properties.ForgeConstants;
import forge.properties.ForgePreferences.FPref;

/** 
 * Manages playback of all sounds for the client.
 *
 */
public class SoundSystem {
    public static final SoundSystem instance = new SoundSystem(GuiBase.getInterface());

    public static final int DELAY = 30;

    private static final IAudioClip emptySound = new NoSoundClip();
    private static final Map<SoundEffectType, IAudioClip> loadedClips = new EnumMap<SoundEffectType, IAudioClip>(SoundEffectType.class);
    private static final Map<String, IAudioClip> loadedScriptClips = new HashMap<String, IAudioClip>();

    private final IGuiBase gui;
    private final EventVisualizer visualizer;

    private SoundSystem(final IGuiBase gui) {
        this.gui = gui;
        this.visualizer = new EventVisualizer(GamePlayerUtil.getGuiPlayer());
    }
    private boolean isUsingAltSystem() {
        return FModel.getPreferences().getPrefBoolean(FPref.UI_ALT_SOUND_SYSTEM);
    }

    /**
     * Fetch a resource based on the sound effect type from the SoundEffectType enumeration.
     * 
     * @param type the sound effect type.
     * @return a clip associated with the loaded resource, or emptySound if the resource
     *         was unavailable or failed to load.
     */
    protected IAudioClip fetchResource(SoundEffectType type) {
        if (!FModel.getPreferences().getPrefBoolean(FPref.UI_ENABLE_SOUNDS)) {
            return emptySound;
        }

        IAudioClip clip = loadedClips.get(type);
        if (clip == null) { // cache miss
            String resource = type.getResourceFileName();
            clip = gui.createAudioClip(resource);
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
    protected IAudioClip fetchResource(String fileName) {
        if (!FModel.getPreferences().getPrefBoolean(FPref.UI_ENABLE_SOUNDS)) {
            return emptySound;
        }

        IAudioClip clip = loadedScriptClips.get(fileName);
        if (null == clip) { // cache miss
            clip = gui.createAudioClip(fileName);
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
    public void play(String resourceFileName, boolean isSynchronized) {
        if (isUsingAltSystem()) {
            gui.startAltSoundSystem(ForgeConstants.SOUND_DIR + resourceFileName, isSynchronized);
        }
        else {
            IAudioClip snd = fetchResource(resourceFileName);
            if (!isSynchronized || snd.isDone()) {
                snd.play();
            }
        }
    }

    /**
     * Play the sound associated with the Sounds enumeration element.
     */
    public void play(SoundEffectType type, boolean isSynchronized) {
        if (isUsingAltSystem()) {
            gui.startAltSoundSystem(ForgeConstants.SOUND_DIR + type.getResourceFileName(), isSynchronized);
        }
        else {
            IAudioClip snd = fetchResource(type);
            if (!isSynchronized || snd.isDone()) {
                snd.play();
            }
        }
    }

    /**
     * Play the sound in a looping manner until 'stop' is called.
     */
    public void loop(String resourceFileName) {
        fetchResource(resourceFileName).loop();
    }

    /**
     * Play the sound in a looping manner until 'stop' is called.
     */
    public void loop(SoundEffectType type) {
        fetchResource(type).loop();
    }

    /**
     * Stop the sound associated with the given resource file name.
     */
    public void stop(String resourceFileName) {
        fetchResource(resourceFileName).stop();
    }

    /**
     * Stop the sound associated with the Sounds enumeration element.
     */
    public void stop(SoundEffectType type) {
        fetchResource(type).stop();
    }

    @Subscribe
    public void receiveEvent(GameEvent evt) {
        SoundEffectType effect = evt.visit(visualizer);
        if (null == effect) {
            return;
        }
        if (effect == SoundEffectType.ScriptedEffect) {
            String resourceName = visualizer.getScriptedSoundEffectName(evt);
            if (!resourceName.isEmpty()) {
                play(resourceName, false);
            }
        } else {
            play(effect, effect.isSynced());
        }
    }
    
    @Subscribe
    public void receiveEvent(UiEvent evt) {
        SoundEffectType effect = evt.visit(visualizer);
        if (null != effect) {
            play(effect, effect.isSynced());
        }
    }
    
    //Background Music
    private IAudioMusic currentTrack;
    private MusicPlaylist currentPlaylist;

    public void setBackgroundMusic(MusicPlaylist playlist) {
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

        String filename = currentPlaylist.getRandomFilename();
        if (filename == null) { return; }

        try {
            currentTrack = gui.createAudioMusic(filename);
            currentTrack.play(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(SoundSystem.DELAY);
                    }
                    catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    changeBackgroundTrack(); //change track when music completes on its own
                }
            });
        }
        catch (Exception ex) {
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
