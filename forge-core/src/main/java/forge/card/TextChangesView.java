package forge.card;

import com.google.common.collect.Maps;

import java.util.Map;

public record TextChangesView(Map<MagicColor.Color, MagicColor.Color> colorChanges, Map<String, String> typeChanges) implements ITextChanges {

    public static TextChangesView EMPTY = new TextChangesView(Map.of(), Map.of());

    @Override
    public boolean isEmpty() {
        return colorChanges.isEmpty() && typeChanges.isEmpty();
    }

    @Override
    public ITextChanges combine(ITextChanges output) {
        if (output.isEmpty()) {
            return this;
        }
        if (this.isEmpty()) {
            return output;
        };

        return new TextChangesView(
            _combineChangedMap(colorChanges(), output.colorChanges()),
            _combineChangedMap(typeChanges(), output.typeChanges())
        );
    }

    private <T> Map<T, T> _combineChangedMap(Map<T, T> input, Map<T, T> output) {
        // no need to do something, just return hash
        if (input.isEmpty()) {
            return output;
        }
        if (output.isEmpty()) {
            return input;
        }
        // magic combine them
        Map<T, T> result = Maps.newHashMap(input);
        for (Map.Entry<T, T> e : result.entrySet()) {
            e.setValue(output.getOrDefault(e.getValue(), e.getValue()));
        }
        result.putAll(output);
        return result;
    }
}
