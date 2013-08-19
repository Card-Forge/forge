package forge.util.storage;

import java.io.File;
import java.util.HashMap;

import org.apache.commons.lang.NotImplementedException;

import com.google.common.base.Function;

import forge.util.IItemReader;


public class StorageNestedFolders<T> extends StorageBase<IStorage<T>> {

    public StorageNestedFolders(IItemReader<T> io, Function<IItemReader<T>, IStorage<T>> factory) {
        super(new HashMap<String, IStorage<T>>());
        for(File sf : io.getSubFolders() )
        {
            map.put(sf.getName(), factory.apply(io.getReaderForFolder(sf)));
        }
    }

    // need code implementations for folder create/delete operations
    
    @Override
    public void add(IStorage<T> deck) {
        // need folder name here!
        throw new NotImplementedException();
    }
    
    @Override
    public void delete(String deckName) {
        throw new NotImplementedException();
    }
    
}
