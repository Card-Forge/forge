package forge.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Shared identity and bundled release notes for the community localization. */
public final class CommunityEditionInfo {
    public static final String EDITION_NAME = "Forge 简体中文民间汉化版";
    public static final String UPDATE_CHANNEL_NAME = "民间汉化版更新渠道";
    public static final String QQ_GROUP = "813597628";
    public static final String SUPPORT_CONTACT = "QQ群：" + QQ_GROUP;
    public static final String DISCLAIMER =
            "本版本由社区爱好者制作，是非官方简体中文版本，与 Card Forge 官方及 Wizards of the Coast 无隶属或授权关系。";

    private static final String RELEASE_NOTES_PROPERTY = "forge.community.releaseNotes";
    private static final String RELEASE_NOTES_RESOURCE = "/forge-community-release-notes-zh-CN.txt";
    private static final String RELEASE_NOTES = loadReleaseNotes();

    private CommunityEditionInfo() {
    }

    public static String getReleaseNotes() {
        return RELEASE_NOTES;
    }

    private static String loadReleaseNotes() {
        final String configured = System.getProperty(RELEASE_NOTES_PROPERTY, "").trim();
        if (!configured.isEmpty()) {
            return configured;
        }

        try (InputStream stream = CommunityEditionInfo.class.getResourceAsStream(RELEASE_NOTES_RESOURCE)) {
            if (stream != null) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8).trim();
            }
        } catch (IOException e) {
            System.err.println("Unable to read bundled community release notes: " + e.getMessage());
        }
        return EDITION_NAME + "\n\n" + DISCLAIMER;
    }
}
