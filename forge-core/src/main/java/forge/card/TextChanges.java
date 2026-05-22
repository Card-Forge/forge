package forge.card;

import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;

import java.util.Map;

public class TextChanges implements ITextChanges {

    private final Table<Long, Long, IChangedText> map = TreeBasedTable.create();
    private final Map<MagicColor.Color, MagicColor.Color> colorChanges = Maps.newHashMap();
    private final Map<String, String> typeChanges = Maps.newHashMap();
    private boolean isDirty = false;

    public long add(final long timestamp, final long staticId, final IChangedText changes) {
        map.put(timestamp, staticId, changes);
        isDirty = true;
        return timestamp;
    }

    public boolean remove(final long timestamp, final long staticId) {
        isDirty = true;
        return map.remove(timestamp, staticId) != null;
    }

    public void clear() {
        map.clear();
        colorChanges.clear();
        typeChanges.clear();
        isDirty = false;
    }

    @Override
    boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public Map<MagicColor.Color, MagicColor.Color> colorChanges() {
        refreshCache();
        return this.colorChanges;
    }

    @Override
    public Map<String, String> typeChanges() {
        refreshCache();
        return this.typeChanges;
    }

    private void refreshCache() {
        if (isDirty) {
            colorChanges.clear();
            typeChanges.clear();
            for (final IChangedText changes : this.map.values()) {
                changes.applyColorChanges(colorChanges);
                changes.applyTextChanges(typeChanges);
            }
            isDirty = false;
        }
    }

    public TextChangesView getView() {
        return new TextChangesView(colorChanges(), typeChanges());
    }
}
