package forge.gui.util;

/** A heading row in a combo box list. It groups the items below it and cannot be selected. */
public record ListHeading(String label) {
    @Override
    public String toString() {
        return label;
    }
}
