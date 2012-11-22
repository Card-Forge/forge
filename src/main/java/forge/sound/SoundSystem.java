package forge.sound;

import java.util.EnumMap;
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

    private final static IAudioClip emptySound = new NoSoundClip();
    private final static Map<SoundEffectType, IAudioClip> loadedClips = new EnumMap<SoundEffectType, IAudioClip>(SoundEffectType.class);
    
    private final EventVisualilzer visualizer = new EventVisualilzer();
    
    protected IAudioClip fetchResource(SoundEffectType type) {

        if (!Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_ENABLE_SOUNDS))
            return emptySound;

        IAudioClip clip = loadedClips.get(type);
        if ( null == clip ) { // cache miss
            String resource = type.getResourceFileName();
            clip = AudioClip.fileExists(resource) ? new AudioClip(resource) : emptySound;
            loadedClips.put(type, clip);
        }
        return null;
    }
    
    
    /**
     * Play the sound associated with the Sounds enumeration element.
     */
    public void play(SoundEffectType type) {
        fetchResource(type).play();
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
    public void loop(SoundEffectType type) {
        fetchResource(type).loop();
    }

    /**
     * Stop the sound associated with the Sounds enumeration element.
     */
    public void stop(SoundEffectType type) {
        fetchResource(type).stop();
    }
    
    @Subscribe
    public void recieveEvent(Event evt) {
        SoundEffectType effect = visualizer.getSoundForEvent(evt);
        if ( null == effect ) return;
        boolean isSync = visualizer.isSyncSound(evt);
        if ( isSync ) 
            playSync(effect);
        else 
            play(effect);
    }

}
