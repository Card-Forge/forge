package forge.web.protocol;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import forge.gamemodes.net.DeltaPacket;
import forge.trackable.TrackableProperty;
import forge.trackable.TrackableTypes;
import forge.trackable.TrackableTypes.TrackableType;
import forge.web.WebTestSupport;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PropertyJsonTest {
    @BeforeClass
    public void setUp() {
        WebTestSupport.initModel();
    }

    private static TrackableProperty ofType(final TrackableType<?> type) {
        for (final TrackableProperty p : TrackableProperty.values()) {
            if (p.getType() == type) {
                return p;
            }
        }
        throw new AssertionError("no property of type " + type);
    }

    private static JsonElement encodeOne(final int ownerKey, final TrackableProperty prop, final Object value) {
        final Map<TrackableProperty, Object> props = new HashMap<>();
        props.put(prop, value);
        final List<String> skipped = new ArrayList<>();
        final JsonObject out = PropertyJson.encodeProps(ownerKey, props, skipped::add);
        Assert.assertTrue(skipped.isEmpty(), "skipped " + skipped);
        return out.get(prop.name());
    }

    private static int cardKey(final int id) {
        return DeltaPacket.makeDeltaKey(DeltaPacket.TYPE_CARD_VIEW, id);
    }

    // Fails if a property that points at a card is sent as a bare number instead of a reference to the card
    @Test
    public void cardReferenceBecomesRefMarker() {
        Assert.assertEquals(encodeOne(cardKey(1), ofType(TrackableTypes.CardViewType), 7), PropertyJson.ref(DeltaPacket.TYPE_CARD_VIEW, 7));
    }

    // Fails if a card's current state is sent as a bare state number instead of a reference to that state of the same card
    @Test
    public void cardStateOrdinalBecomesCardStateKeyOfTheOwningCard() {
        Assert.assertEquals(encodeOne(cardKey(12), TrackableProperty.CurrentState, 2), PropertyJson.ref(DeltaPacket.TYPE_CSV, 12 * 16 + 2));
    }

    // Fails if combat is not sent as bands whose attackers and defender are references, or a band with no blocker list gets anything but null
    @Test
    public void combatDataBecomesBandsOfRefs() {
        final List<List<Integer>> attackers = List.of(List.of(4));
        final List<int[]> defenders = new ArrayList<>();
        // A player type marker, then player 2's id
        defenders.add(new int[]{1, 2});
        final List<List<Integer>> blockers = new ArrayList<>();
        blockers.add(null);
        final List<List<Integer>> planned = new ArrayList<>();
        planned.add(null);
        final DeltaPacket.CombatData data = new DeltaPacket.CombatData(attackers, defenders, blockers, planned);
        final JsonObject band = encodeOne(DeltaPacket.makeDeltaKey(DeltaPacket.TYPE_GAME_VIEW, 1), TrackableProperty.CombatView, data)
                .getAsJsonArray().get(0).getAsJsonObject();
        Assert.assertEquals(band.getAsJsonArray("attackers").get(0), PropertyJson.ref(DeltaPacket.TYPE_CARD_VIEW, 4));
        Assert.assertEquals(band.get("defender"), PropertyJson.ref(DeltaPacket.TYPE_PLAYER_VIEW, 2));
        Assert.assertTrue(band.get("blockers").isJsonNull());
    }

    // Fails if a value with no JSON form is sent anyway or is dropped without being reported
    @Test
    public void unknownValueTypeIsReportedAndSkipped() {
        final Map<TrackableProperty, Object> props = new LinkedHashMap<>();
        props.put(TrackableProperty.Name, new Object());
        final List<String> skipped = new ArrayList<>();
        final JsonObject out = PropertyJson.encodeProps(cardKey(1), props, skipped::add);
        Assert.assertEquals(skipped, List.of("Name"));
        Assert.assertFalse(out.has("Name"));
    }
}
