package forge.sound;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.google.common.eventbus.Subscribe;

import forge.Singletons;
import forge.game.event.GameEvent;
import forge.gui.events.UiEvent;
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

    private boolean isUsingAltSystem() {
	return Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_ALT_SOUND_SYSTEM);
    }

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
     * Play the sound associated with the resource specified by the file name
     * ("synchronized" with other sounds of the same kind means: only one can play at a time).
     */
    public void play(String resourceFileName, boolean isSynchronized) {
        if (isUsingAltSystem()) {
            new AltSoundSystem(String.format("%s/%s", IAudioClip.PathToSound, resourceFileName), isSynchronized).start();
        } else {
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
            new AltSoundSystem(String.format("%s/%s", IAudioClip.PathToSound, type.getResourceFileName()), isSynchronized).start();
        } else {
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
    
    public final UiEventReceiver uiEventsSubscriber = new UiEventReceiver();
    public class UiEventReceiver {
        @Subscribe
        public void receiveEvent(UiEvent evt) {
            SoundEffectType effect = evt.visit(visualizer);
            if (null == effect) {
                return;
            }

            play(effect, effect.isSynced());
        }
    }
}
