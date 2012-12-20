package forge.sound;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.google.common.eventbus.Subscribe;

import forge.Singletons;
import forge.game.event.Event;
import forge.properties.ForgePreferences.FPref;

/** 
 * Manages playback of all sounds for the client.
 *
 */
public class SoundSystem {

    private static final IAudioClip emptySound = new NoSoundClip();
    private static final Map<SoundEffectType, IAudioClip> loadedClips = new EnumMap<SoundEffectType, IAudioClip>(SoundEffectType.class);
    private static final Map<String, IAudioClip> loadedScriptClips = new HashMap<String, IAudioClip>();

    private final EventVisualizer visualizer = new EventVisualizer();

    /**
     * Fetch a resource based on the sound effect type from the SoundEffectType enumeration.
     * 
     * @param type the sound effect type.
     * @return a clip associated with the loaded resource, or emptySound if the resource
     *         was unavailable or failed to load.
     */
    protected IAudioClip fetchResource(SoundEffectType type) {

        if (!Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_ENABLE_SOUNDS)) {
            return emptySound;
        }

        IAudioClip clip = loadedClips.get(type);
        if (null == clip) { // cache miss
            String resource = type.getResourceFileName();
            clip = AudioClip.fileExists(resource) ? new AudioClip(resource) : emptySound;
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
        if (!Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_ENABLE_SOUNDS)) {
            return emptySound;
        }

        IAudioClip clip = loadedScriptClips.get(fileName);
        if (null == clip) { // cache miss
            clip = AudioClip.fileExists(fileName) ? new AudioClip(fileName) : emptySound;
            loadedScriptClips.put(fileName, clip);
        }
        return clip;
    }

    /**
     * Play the sound associated with the Sounds enumeration element.
     */
    public void play(SoundEffectType type) {
        fetchResource(type).play();
    }

    /**
     * Play the sound associated with a specific resource file.
     */
    public void play(String resourceFileName) {
        fetchResource(resourceFileName).play();
    }

    /**
     * Play the sound associated with the resource specified by the file name 
     * (synchronized with other sounds of the same kind, so only one can play
     * at the same time).
     */
    public void playSync(String resourceFileName) {
        IAudioClip snd = fetchResource(resourceFileName);
        if (snd.isDone()) {
            snd.play();
        }
    }

    /**
     * Play the sound associated with the Sounds enumeration element
     * (synchronized with other sounds of the same kind, so only one can play
     * at the same time).
     */
    public void playSync(SoundEffectType type) {
        IAudioClip snd = fetchResource(type);
        if (snd.isDone()) {
            snd.play();
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
    public void receiveEvent(Event evt) {
        SoundEffectType effect = visualizer.getSoundForEvent(evt);
        if (null == effect) {
            return;
        }
        if (effect == SoundEffectType.ScriptedEffect) {
            String resourceName = visualizer.getScriptedSoundEffectName(evt);
            if (!resourceName.isEmpty()) {
                play(resourceName);
            }
        } else {
            boolean isSync = visualizer.isSyncSound(effect);
            if (isSync) {
                playSync(effect);
            } else {
                play(effect);
            }
        }
    }
}
