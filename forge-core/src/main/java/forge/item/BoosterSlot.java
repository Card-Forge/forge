package forge.item;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.TreeMap;

public class BoosterSlot {
    private final String slotName;
    private String baseRarity;
    private float startRange = 0.0f;
    private final TreeMap<Float, String> slotPercentages = new TreeMap<>();

    public BoosterSlot(final String slotName, final List<String> contents) {
        this.slotName = slotName;
        this.baseRarity = null;
        parseContents(contents);
    }

    public final String getSlotName() {
        return slotName;
    }

    public static BoosterSlot parseSlot(final String slotName, final List<String> contents) {
        return new BoosterSlot(slotName, contents);
    }

    private void parseContents(List<String> contents) {
        for (String content : contents) {
            if (content.startsWith("#")) {
                continue;
            }
            String[] parts = content.split("=", 2);
            String key = parts[0];
            String value = parts[1];

            if (key.equalsIgnoreCase("Base")) {
                baseRarity = value;
            } else if (key.equalsIgnoreCase("Replace")) {
                // Are there other things?
                String[] replaceParts = value.split(" ", 2);
                float pct = Float.parseFloat(replaceParts[0]);
                startRange += pct;
                slotPercentages.put(startRange, replaceParts[1]);
            }
        }
    }

    public List<String> getSlotSheet(int amount) {
        // For the first item in the slot, run float percentages
        List<String> sheets = Lists.newArrayList();
        sheets.add(replaceSlot());
        for(int i = 1; i < amount; i++) {
            sheets.add(baseRarity);
        }
        return sheets;
    }

    public String replaceSlot() {
        double rand = Math.random() * 100;
        for (Float key : slotPercentages.keySet()) {
            if (rand < key) {
                System.out.println("Replaced a base slot! " + slotName + " -> " + slotPercentages.get(key));

                return slotPercentages.get(key);
            }
        }

        // If we didn't find a key, return the base rarity from that edition
        return baseRarity;
    }
}
