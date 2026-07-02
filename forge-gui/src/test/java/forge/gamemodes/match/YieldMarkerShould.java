package forge.gamemodes.match;

import forge.game.phase.PhaseType;
import forge.game.player.PlayerView;
import forge.util.Localizer;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public class YieldMarkerShould {

    private PlayerView owner;
    private PlayerView otherOwner;

    @BeforeSuite
    public void initLocalizer() {
        Localizer.getInstance().initialize("en-US", "res/languages/");
    }

    @BeforeMethod
    public void setUp() {
        owner = Mockito.mock(PlayerView.class);
        otherOwner = Mockito.mock(PlayerView.class);
    }

    @Test
    public void rejectANullPhaseOwner() {
        assertThatNullPointerException()
                .isThrownBy(() -> new YieldMarker(null, PhaseType.MAIN1));
    }

    @Test
    public void rejectANullPhase() {
        assertThatNullPointerException()
                .isThrownBy(() -> new YieldMarker(owner, null));
    }

    @Test
    public void beEqualToAnotherMarkerWithTheSameOwnerAndPhase() {
        YieldMarker first = new YieldMarker(owner, PhaseType.MAIN1);
        YieldMarker second = new YieldMarker(owner, PhaseType.MAIN1);

        assertThat(first).isEqualTo(second);
    }

    @Test
    public void differFromAMarkerTargetingADifferentPhase() {
        assertThat(new YieldMarker(owner, PhaseType.MAIN1))
                .isNotEqualTo(new YieldMarker(owner, PhaseType.MAIN2));
    }

    @Test
    public void differFromAMarkerTargetingADifferentPlayer() {
        assertThat(new YieldMarker(owner, PhaseType.MAIN1))
                .isNotEqualTo(new YieldMarker(otherOwner, PhaseType.MAIN1));
    }

    @Test
    public void giveEqualMarkersEqualHashCodes() {
        // YieldMarker hand-writes equals() and hashCode() (it is not a record) and is used as a
        // hash-map key. The equals/hashCode contract — equal objects must share a hash code — is
        // maintained by hand and can silently drift if one method is edited without the other.
        // Checked as a property across every PhaseType rather than one hardcoded pair, so it keeps
        // holding as PhaseType grows and is not just an example of the equals() tests above.
        for (PhaseType phase : PhaseType.values()) {
            YieldMarker first = new YieldMarker(owner, phase);
            YieldMarker second = new YieldMarker(owner, phase);

            assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
        }
    }
}
