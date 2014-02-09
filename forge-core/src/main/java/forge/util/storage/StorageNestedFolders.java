package forge.util.storage;

import com.google.common.base.Function;

import java.io.File;
import java.util.HashMap;

public class StorageNestedFolders<T> extends StorageBase<IStorage<T>> {
    private final File thisFolder;

    public StorageNestedFolders(File thisFolder, Iterable<File> subfolders, Function<File, IStorage<T>> factory) {
        super("<Subfolders>", new HashMap<String, IStorage<T>>());
        this.thisFolder = thisFolder;
        for (File sf : subfolders) {
            IStorage<T> newUnit = factory.apply(sf);
            map.put(sf.getName(), newUnit);
        }
    }

    // need code implementations for folder create/delete operations

    @Override
    public void add(IStorage<T> item) {
        File subdir = new File(thisFolder, item.getName());
        subdir.mkdir();

        // TODO: save recursivelly the passed IStorage
        throw new UnsupportedOperationException("method is not implemented");
    }

    @Override
    public void delete(String itemName) {
        File subdir = new File(thisFolder, itemName);
        IStorage<T> f = map.remove(itemName);

        // TODO: Clear all that files from disk
        if (f != null) {
            subdir.delete(); // won't work if not empty;
        }
    }
}
