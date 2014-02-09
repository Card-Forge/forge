package forge.util;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.mapper.MapperWrapper;

import java.util.ArrayList;
import java.util.List;

/** 
 * TODO: Write javadoc for this type.
 *
 */
/**
 * Xstream subclass that ignores fields that are present in the save but not
 * in the class. This one is intended to skip fields defined in Object class
 * (but are there any fields?)
 */
public class IgnoringXStream extends XStream {
    private final List<String> ignoredFields = new ArrayList<String>();

    @Override
    protected MapperWrapper wrapMapper(final MapperWrapper next) {
        return new MapperWrapper(next) {
            @Override
            public boolean shouldSerializeMember(@SuppressWarnings("rawtypes") final Class definedIn,
                    final String fieldName) {
                if (definedIn == Object.class) {
                    IgnoringXStream.this.ignoredFields.add(fieldName);
                    return false;
                }
                return super.shouldSerializeMember(definedIn, fieldName);
            }
        };
    }
}
