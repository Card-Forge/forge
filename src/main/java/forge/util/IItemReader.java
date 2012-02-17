package forge.util;

import java.util.Map;



public interface IItemReader<T extends IHasName> {
    Map<String, T> readAll();
    //T read(File file);
}

