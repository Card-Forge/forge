package forge.gamemodes.match;

import forge.ai.AIOption;
import forge.gamemodes.net.event.UpdateLobbyPlayerEvent;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class LobbySlotShould {

    private LobbySlot humanSlot() {
        return new LobbySlot(LobbySlotType.LOCAL, "Alice", 0, 0, 0, false, false,
                Collections.emptySet());
    }

    @Test
    public void alwaysBeReadyWhenSlotTypeIsAI() {
        // Given a slot configured as AI, regardless of the ready flag
        LobbySlot slot = new LobbySlot(LobbySlotType.AI, "Bot", 0, 0, 0, false, false,
                Collections.emptySet());
        // Then it reports ready unconditionally
        assertThat(slot.isReady()).isTrue();
    }

    @Test
    public void startNotReadyWhenSlotTypeIsLocal() {
        // Unlike an AI slot, a local slot is not ready until something marks it so
        LobbySlot slot = humanSlot();

        assertThat(slot.isReady()).isFalse();
    }

    @Test
    public void treatNullAiOptionsAsAnEmptySet() {
        LobbySlot slot = new LobbySlot(LobbySlotType.AI, "Bot", 0, 0, 0, false, false, null);

        assertThat(slot.getAiOptions()).isNotNull().isEmpty();
    }

    @Test
    public void updateNameWhenEventCarriesADifferentName() {
        // Given a slot named "Alice"
        LobbySlot slot = humanSlot();
        // When an event arrives carrying a new name
        slot.apply(UpdateLobbyPlayerEvent.nameUpdate("Charlie"));
        // Then the slot reflects the new name
        assertThat(slot.getName()).isEqualTo("Charlie");
    }

    @Test
    public void ignoreNameUpdateWhenNameIsUnchanged() {
        // apply() routes every field through setIfChanged(), which compares the incoming value with
        // Objects.equals() and skips it when unchanged, aggregating a `changed` flag used to gate
        // view updates. Asserting that flag is what makes the no-op observable: a value-only check
        // could not tell "guard skipped the setter" apart from "setter re-applied the same name".
        // Given a slot named "Alice"
        LobbySlot slot = humanSlot();
        // When an event arrives with the same name
        boolean changed = slot.apply(UpdateLobbyPlayerEvent.nameUpdate("Alice"));
        // Then apply() reports no change and the name is unchanged
        assertThat(changed).isFalse();
        assertThat(slot.getName()).isEqualTo("Alice");
    }

    @Test
    public void updateAvatarIndexWhenEventCarriesANewIndex() {
        // Given a slot with avatar index 0
        LobbySlot slot = humanSlot();
        // When an avatar-update event arrives
        slot.apply(UpdateLobbyPlayerEvent.avatarUpdate(5));
        // Then the avatar index is updated
        assertThat(slot.getAvatarIndex()).isEqualTo(5);
    }

    @Test
    public void updateTeamWhenEventCarriesANewTeam() {
        // Given a slot on team 0
        LobbySlot slot = humanSlot();
        // When a team-update event arrives
        slot.apply(UpdateLobbyPlayerEvent.teamUpdate(2));
        // Then the slot is on the new team
        assertThat(slot.getTeam()).isEqualTo(2);
    }

    @Test
    public void becomeReadyWhenEventDemandsIt() {
        LobbySlot slot = humanSlot();
        assertThat(slot.isReady()).isFalse();

        slot.apply(UpdateLobbyPlayerEvent.isReadyUpdate(true));

        assertThat(slot.isReady()).isTrue();
    }

    @Test
    public void enableDevModeWhenEventDemandsIt() {
        LobbySlot slot = humanSlot();
        assertThat(slot.isDevMode()).isFalse();

        slot.apply(UpdateLobbyPlayerEvent.devModeUpdate(true));

        assertThat(slot.isDevMode()).isTrue();
    }

    @Test
    public void applyAllFieldsFromAFullLobbyPlayerEvent() {
        // Given a freshly created human slot
        LobbySlot slot = humanSlot();
        // When a full lobby-player event is applied
        slot.apply(UpdateLobbyPlayerEvent.create(
                LobbySlotType.AI, "NewName", 2, 3, 1, true, false,
                Set.of(AIOption.USE_SIMULATION), "aggro"));
        // Then all provided fields are updated
        assertThat(slot.getType()).isEqualTo(LobbySlotType.AI);
        assertThat(slot.getName()).isEqualTo("NewName");
        assertThat(slot.getAvatarIndex()).isEqualTo(2);
        assertThat(slot.getSleeveIndex()).isEqualTo(3);
        assertThat(slot.getTeam()).isEqualTo(1);
        assertThat(slot.isArchenemy()).isTrue();
        assertThat(slot.getAiProfile()).isEqualTo("aggro");
    }
}
