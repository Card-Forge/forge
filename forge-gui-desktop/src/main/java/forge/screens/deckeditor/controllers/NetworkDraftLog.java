package forge.screens.deckeditor.controllers;

import forge.gamemodes.net.EventParticipant;
import forge.util.Localizer;

import java.awt.Color;
import java.util.List;

/**
 * Utility for logging network draft events to the existing Editor Log tab
 * ({@link forge.screens.deckeditor.controllers.CEditorLog} / {@link forge.screens.deckeditor.views.VEditorLog}).
 * All entries are driven by protocol events — no new network messages needed.
 */
public final class NetworkDraftLog {
    private static final Color COLOR_BANNER = new Color(100, 150, 200);     // muted blue
    private static final Color COLOR_SEPARATOR = new Color(130, 130, 130);  // gray
    private static final Color COLOR_MY_PICK = new Color(50, 200, 50);      // green
    private static final Color COLOR_OTHER_PICK = new Color(180, 180, 180); // light gray

    private static final String BANNER = "======================================";

    private static final Localizer localizer = Localizer.getInstance();

    private NetworkDraftLog() { } // utility class

    /** Log the draft start banner with pod information. */
    public static void logDraftStart(List<EventParticipant> participants, int totalPacks,
            String productName, int mySeatIndex) {
        log(BANNER, COLOR_BANNER);
        log("  " + localizer.getMessage("lblDraftLogDraftStarted", String.valueOf(participants.size())), COLOR_BANNER);
        log("  " + localizer.getMessage("lblDraftLogPacksOf", String.valueOf(totalPacks), productName), COLOR_BANNER);

        StringBuilder humans = new StringBuilder("  " + localizer.getMessage("lblDraftLogPlayersYou"));
        StringBuilder ais = new StringBuilder("  " + localizer.getMessage("lblDraftLogAiSeats"));
        boolean hasAI = false;
        for (EventParticipant p : participants) {
            if (p.isHuman() && p.getSeatIndex() != mySeatIndex) {
                humans.append(", ").append(p.getName());
            } else if (p.isAI()) {
                ais.append(" ").append(p.getName()).append(",");
                hasAI = true;
            }
        }
        log(humans.toString(), COLOR_BANNER);
        if (hasAI) {
            // Trim trailing comma
            log(ais.substring(0, ais.length() - 1), COLOR_BANNER);
        }
        log(BANNER, COLOR_BANNER);
    }

    /** Log a pack round header. */
    public static void logPackHeader(int packNumber, boolean passingRight) {
        String direction = passingRight ? localizer.getMessage("lblDraftLogPassingRight")
                : localizer.getMessage("lblDraftLogPassingLeft");
        log(localizer.getMessage("lblDraftLogPackHeader", String.valueOf(packNumber), direction), COLOR_SEPARATOR);
    }

    public static void logOtherPick(String playerName, int queueDepth) {
        String base = localizer.getMessage("lblDraftLogOtherPick", playerName);
        log(base + waitingSuffix(queueDepth), COLOR_OTHER_PICK);
    }

    public static void logMyPick(String cardName, int packNumber, int pickInPack, int queueDepth, boolean auto) {
        String displayName = auto ? cardName + " (auto)" : cardName;
        String base = localizer.getMessage("lblDraftLogMyPick", displayName,
                String.valueOf(packNumber), String.valueOf(pickInPack));
        log(base + waitingSuffix(queueDepth), COLOR_MY_PICK);
    }

    private static String waitingSuffix(int queueDepth) {
        if (queueDepth <= 0) return "";
        return " " + localizer.getMessage("lblDraftLogWaiting", String.valueOf(queueDepth));
    }

    /** Log draft completion. */
    public static void logDraftComplete(int totalCards) {
        log(BANNER, COLOR_BANNER);
        log("  " + localizer.getMessage("lblDraftLogDraftComplete", String.valueOf(totalCards)), COLOR_BANNER);
        log("  " + localizer.getMessage("lblDraftLogBuildingDeck"), COLOR_BANNER);
        log(BANNER, COLOR_BANNER);
    }

    private static void log(String message, Color color) {
        CEditorLog.SINGLETON_INSTANCE.addLogEntry(message, color);
    }
}
