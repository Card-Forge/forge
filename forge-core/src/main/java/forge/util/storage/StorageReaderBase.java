package forge.util.storage;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import forge.util.IItemReader;

import java.io.File;

public abstract class StorageReaderBase<T> implements IItemReader<T> {
    protected final Function<? super T, String> keySelector;
    public StorageReaderBase(final Function<? super T, String> keySelector0) {
        keySelector = keySelector0;
    }

    @Override
    public Iterable<File> getSubFolders() {
        // TODO Auto-generated method stub
        return ImmutableList.of();
    }

    @Override
    public IItemReader<T> getReaderForFolder(File subfolder) {
        throw new UnsupportedOperationException("This reader is not supposed to have nested folders");
    }
}