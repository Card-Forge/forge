package forge.player;

import forge.LobbyPlayer;
import forge.ai.AiProfileUtil;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import org.mockito.MockedStatic;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.mockito.Mockito.*;
import static org.testng.Assert.assertNotNull;

public class GamePlayerUtilTest {

    /**
     * Test the createAiPlayer method with null AI profile override. GamePlayerUtil depends on static methods that we mock.
     * Dev note: This test fails under Java 25.
     */
    @Test
    public void testCreateAiPlayer_createsLobbyPlayer_whenProfileOverrideIsNull() {
        // Mock ForgePreferences returned by FModel
        ForgePreferences mockPrefs = mock(ForgePreferences.class);
        when(mockPrefs.getPref(ForgePreferences.FPref.UI_CURRENT_AI_PROFILE)).thenReturn("Default");
        // Allow setPref/save to be called without throwing
        doNothing().when(mockPrefs).setPref(any(), any());
        doNothing().when(mockPrefs).save();

        try (MockedStatic<FModel> fmodelMock = mockStatic(FModel.class);
             MockedStatic<AiProfileUtil> aiMock = mockStatic(AiProfileUtil.class)) {

            fmodelMock.when(FModel::getPreferences).thenReturn(mockPrefs);

            // Ensure the profile list contains the chosen profile
            aiMock.when(AiProfileUtil::getProfilesDisplayList).thenReturn(Arrays.asList("Default"));

            // Optional: stub getRandomProfile if your pref is RANDOM_MATCH
            aiMock.when(AiProfileUtil::getRandomProfile).thenReturn("Default");

            LobbyPlayer lobbyPlayer = GamePlayerUtil.createAiPlayer("name", 0, 0, null, null);
            assertNotNull(lobbyPlayer);
        }
    }
}