package forge.adventure.util;

/**
 * Interface to save the content of the save game file
 */
public interface SaveFileContent {
    void load(SaveFileData data);
    SaveFileData save();
}
