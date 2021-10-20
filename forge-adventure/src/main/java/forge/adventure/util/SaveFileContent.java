package forge.adventure.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Interface to save the content the the save game file
 */
public interface SaveFileContent {
    void writeToSaveFile(ObjectOutputStream saveFile) throws IOException ;
    void readFromSaveFile(ObjectInputStream saveFile) throws IOException, ClassNotFoundException;
}
