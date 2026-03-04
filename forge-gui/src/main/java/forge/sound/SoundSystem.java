package forge.sound;

import com.google.common.eventbus.Subscribe;
import forge.game.event.GameEvent;
import forge.gui.GuiBase;
import forge.gui.events.UiEvent;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.player.GamePlayerUtil;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;

/**
 * Manages playback of all sounds for the client.
 */
public class SoundSystem {
    public static final SoundSystem instance = new SoundSystem();

    public static final int DELAY = 30;

    public static final FilenameFilter PLAYABLE_AUDIO = (dir, name) -> GuiBase.getInterface().isSupportedAudioFormat(new File(dir, name));
    private static final String[] SOUND_RESOURCE_PATHS = {ForgeConstants.USER_CUSTOM_DIR, ForgeConstants.CACHE_DIR};

    private static final IAudioClip emptySound = new NoSoundClip();
    private static final Map<SoundEffectType, IAudioClip> loadedClips = new EnumMap<>(SoundEffectType.class);
    private static final Map<String, IAudioClip> loadedScriptClips = new HashMap<>();

    private final EventVisualizer visualizer;

    private boolean shouldPlayMusic = true;
    private boolean hasWindowFocus = true;
    private boolean ignorePlayRequests = false;

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
                return emptySound;
            } else
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
                return emptySound;
            } else
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
        if (ignorePlayRequests) {
            return;
        }

        if (isUsingAltSystem()) {
            File file = getSoundResource(resourceFileName);
            if(file == null)
                return;
            GuiBase.getInterface().startAltSoundSystem(file.getPath(), isSynchronized);
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
        if (ignorePlayRequests) {
            return;
        }

        if (isUsingAltSystem()) {
            File file = getSoundResource(type.getResourceFileName());
            if(file == null)
                return;
            GuiBase.getInterface().startAltSoundSystem(file.getPath(), isSynchronized);
        } else {
            final IAudioClip snd = fetchResource(type);
            if (!isSynchronized || snd.isDone()) {
                snd.play(FModel.getPreferences().getPrefInt(FPref.UI_VOL_SOUNDS)/100f);
            }
        }
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

    //Shelved tracks, for when we want to switch back to a previous track and want to resume playback where we left off.
    private MusicPlaylist shelvedPlaylist;
    private IAudioMusic shelvedTrack;

    public void setBackgroundMusic(final MusicPlaylist playlist) {
        setBackgroundMusic(playlist, false);
    }

    public void setBackgroundMusic(final MusicPlaylist playlist, boolean shelvePrevious) {
        if(playlist == currentPlaylist)
            return;
        
        if(playlist == shelvedPlaylist && playlist != null) {
            if(!shelvePrevious) {
                //Dispose current, resume shelved.
                if (currentTrack != null)
                    currentTrack.dispose();
                currentTrack = shelvedTrack;
                shelvedTrack = null;
                shelvedPlaylist = null;
            }
            else {
                //Swap current and shelved.
                if (currentTrack != null)
                    currentTrack.pause();
                IAudioMusic temp = currentTrack;
                currentTrack = shelvedTrack;
                shelvedTrack = temp;
                shelvedPlaylist = currentPlaylist;
            }
            currentPlaylist = playlist;
            refreshVolume();
            if (currentTrack != null) {
                currentTrack.resume();
            }
            
            return;
        }

        if (shelvedTrack != null) {
            // We've switched to a third track. Safe to discard the shelf.
            clearShelvedPlaylist();
        }

        if (shelvePrevious) {
            //Shelve current.
            if(currentTrack != null)
                currentTrack.pause();
            shelvedTrack = currentTrack;
            shelvedPlaylist = currentPlaylist;
            currentTrack = null;
        }

        currentPlaylist = playlist;
        changeBackgroundTrack();
    }

    public MusicPlaylist getCurrentPlaylist() {
        return currentPlaylist;
    }

    public MusicPlaylist getShelvedPlaylist() {
        return shelvedPlaylist;
    }

    public void clearShelvedPlaylist() {
        if(this.shelvedTrack == null)
            return;
        shelvedTrack.dispose();
        shelvedTrack = null;
        shelvedPlaylist = null;
    }

    public void changeBackgroundTrack() {
        //ensure old track stopped and disposed of if needed
        if (currentTrack != null) {
            currentTrack.dispose();
            currentTrack = null;
        }

        if (currentPlaylist == null || isMuted()) {
            return;
        }

        final String filename = currentPlaylist.getRandomFilename();
        if (filename == null) { return; }

        try {
            currentTrack = GuiBase.getInterface().createAudioMusic(filename);
            shouldPlayMusic = true;
            currentTrack.play(() -> {
                try {
                    Thread.sleep(SoundSystem.DELAY);
                } catch (final InterruptedException ex) {
                    ex.printStackTrace();
                }
                changeBackgroundTrack(); //change track when music completes on its own
            });
            refreshVolume();
        } catch (final Exception ex) {
            System.err.println("Unable to load music file: " + filename);
        }
    }

    public void refreshVolume() {
        if (currentTrack != null) {
            currentTrack.setVolume(FModel.getPreferences().getPrefInt(FPref.UI_VOL_MUSIC) / 100f);
        }
        else if (currentPlaylist != null) {
            changeBackgroundTrack();
        }
    }
    public void pause() {
        shouldPlayMusic = false;
        updatePlayPause();
    }

    public void resume() {
        shouldPlayMusic = true;
        updatePlayPause();
        refreshVolume();
    }

    private void updatePlayPause() {
        if(currentTrack == null)
            return;
        boolean shouldPlay = shouldPlayMusic && hasWindowFocus;
        if(shouldPlay && !currentTrack.isPlaying())
            currentTrack.resume();
        else if(!shouldPlay && currentTrack.isPlaying())
            currentTrack.pause();
    }

    public void stopBackgroundMusic() {
        if (currentTrack != null) {
            currentTrack.dispose();
            currentTrack = null;
        }
        if (shelvedTrack != null) {
            shelvedTrack.dispose();
            shelvedTrack = null;
        }
        currentPlaylist = null;
        shelvedPlaylist = null;
        shouldPlayMusic = false;
    }

    public void dispose() {
        stopBackgroundMusic();
        invalidateSoundCache();
    }

    public void setWindowFocus(boolean hasWindowFocus) {
        this.hasWindowFocus = hasWindowFocus;
        updatePlayPause();
    }

    public void fadeModifier(float value) {
        if (currentTrack != null) {
            currentTrack.setVolume((FModel.getPreferences().getPrefInt(FPref.UI_VOL_MUSIC)*value)/100f);
        }
    }

    private boolean isMuted() {
        return GuiBase.getInterface().isLibgdxPort()
                ? FModel.getPreferences().getPrefInt(FPref.UI_VOL_MUSIC) < 1
                : !FModel.getPreferences().getPrefBoolean(FPref.UI_ENABLE_MUSIC);
    }

    private static final Map<Integer, List<String>> soundResourceDirectoryCache = new HashMap<>();

    /**
     * Returns a list of audio resource directories, in order of overrides. The subPath parameter is usually either
     * `music/` or `sound/`. The fallback order used here is:
     * <li>The user's override of the current adventure directory, if in adventure mode.</li>
     * <li>The user's override of the common adventure directory, if in adventure mode.</li>
     * <li>The current adventure directory, if in adventure mode.</li>
     * <li>The common adventure directory if in adventure mode.</li>
     * <li>The current user audio profile directory.</li>
     * <li>The common resource directory.</li>
     *
     * User overrides and profiles are searched for first in the custom directory, and then the cache directory.
     */
    private static List<String> getSoundResourceDirectoryFallbacks(String profileName, String subPath) {
        String adventureDirectory = GuiBase.getAdventureDirectory();
        int cacheKey = Objects.hash(profileName, subPath, adventureDirectory);
        if(soundResourceDirectoryCache.containsKey(cacheKey))
            return soundResourceDirectoryCache.get(cacheKey);
        List<String> out = new ArrayList<>(5);
        if (adventureDirectory != null) {
            //Check user folders for matching music folder. Last path part should be the plane name.
            String directoryName = new File(adventureDirectory).getName();
            for(String path : SOUND_RESOURCE_PATHS) {
                out.add(path + subPath + directoryName + ForgeConstants.PATH_SEPARATOR);
            }
            out.add(adventureDirectory + subPath);
            out.add(ForgeConstants.ADVENTURE_COMMON_DIR + subPath);
        }
        if(profileName != null && !"Default".equals(profileName)) {
            for (String path : SOUND_RESOURCE_PATHS) {
                out.add(path + subPath + profileName + ForgeConstants.PATH_SEPARATOR);
            }
        }
        out.add(ForgeConstants.RES_DIR + subPath);
        soundResourceDirectoryCache.put(cacheKey, out);
        return out;
    }


    public void invalidateSoundCache() {
        for (IAudioClip c : loadedClips.values()) {
            c.dispose();
        }
        loadedClips.clear();
        for (IAudioClip c : loadedScriptClips.values()) {
            c.dispose();
        }
        loadedScriptClips.clear();
        soundResourceDirectoryCache.clear();
        soundResourceAssetCache.clear();
    }

    public String[] getAvailableSoundSets()
    {
        List<String> availableSets = collectProfiles(ForgeConstants.SOUND_DIR);

        if (availableSets.size() == 1 || !availableSets.contains(FModel.getPreferences().getPref(FPref.UI_CURRENT_SOUND_SET))) {
            // Default profile only or the current set is no longer available - revert the preference setting to default
            FModel.getPreferences().setPref(FPref.UI_CURRENT_SOUND_SET, "Default");
            invalidateSoundCache();
        }

        return availableSets.toArray(new String[0]);
    }

    private static final Map<Integer, File> soundResourceAssetCache = new HashMap<>();

    /**
     * Searches through available sound resource directories for a sound matching the given filename.
     * Returns null if the file does not exist in any sound directory.
     */
    public File getSoundResource(String filename) {
        String adventureDirectory = GuiBase.getAdventureDirectory();
        String profileName = FModel.getPreferences().getPref(FPref.UI_CURRENT_SOUND_SET);
        int cacheKey = Objects.hash(filename, adventureDirectory, profileName);
        if(soundResourceAssetCache.containsKey(cacheKey) && soundResourceAssetCache.get(cacheKey).isFile())
            return soundResourceAssetCache.get(cacheKey);
        FilenameFilter nameFilter = (dir, name) -> name.equals(filename) || (name.startsWith(filename + ".") && PLAYABLE_AUDIO.accept(dir, name));
        File out = getSoundResourceDirectoryFallbacks(profileName, ForgeConstants.SOUND_DIR).stream()
                .map(File::new)
                .filter(File::isDirectory)
                .map((d) -> d.listFiles(nameFilter))
                .filter(Objects::nonNull)
                .flatMap(Arrays::stream)
                .findFirst()
                .orElse(null);
        if(out != null)
            soundResourceAssetCache.put(cacheKey, out);
        return out;
    }

    public static String[] getAvailableMusicSets() {
        List<String> availableSets = collectProfiles(ForgeConstants.MUSIC_DIR);

        if (availableSets.size() == 1 || !availableSets.contains(FModel.getPreferences().getPref(FPref.UI_CURRENT_MUSIC_SET))) {
            // Default profile only or the current set is no longer available - revert the preference setting to default
            FModel.getPreferences().setPref(FPref.UI_CURRENT_MUSIC_SET, "Default");
            MusicPlaylist.invalidateMusicPlaylist();
        }

        return availableSets.toArray(new String[0]);
    }

    /**
     * Searches through available music resource directories for a directory matching the given playlist.
     * Returns null if the playlist does not exist in any music directory.
     */
    public static File findMusicDirectory(MusicPlaylist playlist) {
        String profileName = FModel.getPreferences().getPref(FPref.UI_CURRENT_MUSIC_SET);
        return getSoundResourceDirectoryFallbacks(profileName, ForgeConstants.MUSIC_DIR).stream()
                .map((p) -> new File(p, playlist.getSubDir()))
                .filter(File::isDirectory)
                .filter((f) -> Objects.requireNonNull(f.listFiles(PLAYABLE_AUDIO)).length > 0)
                .findFirst()
                .orElse(null);
    }

    private static List<String> collectProfiles(String subPath) {
        Set<String> foundSets = new HashSet<>();
        for(String path : SOUND_RESOURCE_PATHS) {
            File[] files = new File(path + subPath).listFiles(File::isDirectory);
            if(files != null)
                Arrays.stream(files).map(File::getName).forEach(foundSets::add);
        }
        foundSets.remove("Default");
        List<String> availableSets = new ArrayList<>(foundSets);

        Collections.sort(availableSets);
        availableSets.add(0, "Default");
        return availableSets;
    }

    public void setIgnorePlayRequests(boolean ignorePlayRequests) {
        this.ignorePlayRequests = ignorePlayRequests;
    }
}
