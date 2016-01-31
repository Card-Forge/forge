package forge.util.collect;

import java.io.File;

import forge.util.FileUtil;

public abstract class FCollectionReader<T> {
    protected final File file;

    protected FCollectionReader(String filePath) {
        file = new File(filePath);
    }

    void readAll(FCollection<T> collection) {
        for (final String line : FileUtil.readFile(file)) {
            final T item = read(line);
            if (item != null) {
                collection.add(item);
            }
        }
    }

    protected abstract T read(String line);
}
