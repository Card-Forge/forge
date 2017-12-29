package forge.util.collect;

import java.io.File;

import forge.util.FileUtil;

public abstract class FCollectionReader<T> {
    protected final File file;

    protected FCollectionReader(String filePath) {
        file = new File(filePath);
    }

    void readAll(FCollection<T> collection) {
        for (String line : FileUtil.readFile(file)) {
            line = line.trim();
            if (line.isEmpty()) {
                continue; //ignore blank or whitespace lines
            }

            T item = read(line);
            if (item != null) {
                collection.add(item);
            }
        }
    }

    protected void alertInvalidLine(String line, String message) {
        System.err.println(message);
        System.err.println(line);
        System.err.println(file.getPath());
        System.err.println();
    }

    protected abstract T read(String line);
}
