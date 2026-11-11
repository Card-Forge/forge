package forge.gamemodes.rogue;

import forge.gamemodes.rogue.effect.AetherEffect;
import java.util.Arrays;
import java.util.Comparator;

/** Changes to persisted Rogue Commander data, applied in version order on load. */
public enum RogueSaveMigration {
    TOLARIAN_ARCHIVE_RANK(1) {
        @Override
        void migrateMetaProgress(RogueMetaProgress progress) {
            AetherEffect effect = AetherEffect.TOLARIAN_ARCHIVE;
            progress.capAetherEffectRank(effect, effect.getEffectiveMaxRank(progress.getAetherUpgradeLevel()));
        }

        @Override
        void migrateRun(RogueRun run) {
            // A run does not record its starting Aether Upgrade level, so retain rank 3.
            run.capAetherEffectRank(AetherEffect.TOLARIAN_ARCHIVE, 3);
        }
    },
    TOTAL_PLAYTIME(2) {
        @Override
        void migrateMetaProgress(RogueMetaProgress progress) {
            progress.backfillTotalPlaytime();
        }
    },
    RETIRE_TEMPORAL_OBSERVATORY(3) {
        @Override
        void migrateMetaProgress(RogueMetaProgress progress) {
            progress.retireAetherEffect(TEMPORAL_OBSERVATORY_ID, new int[]{6, 10, 14});
        }

        @Override
        void migrateRun(RogueRun run) {
            run.removeAetherEffect(TEMPORAL_OBSERVATORY_ID);
        }
    };

    private static final String TEMPORAL_OBSERVATORY_ID = "temporal_observatory";

    private final int version;

    RogueSaveMigration(int version) {
        this.version = version;
    }

    void migrateMetaProgress(RogueMetaProgress progress) { }

    void migrateRun(RogueRun run) { }

    public static int currentVersion() {
        int current = 0;
        for (RogueSaveMigration migration : values()) {
            current = Math.max(current, migration.version);
        }
        return current;
    }

    static void applyToMetaProgress(RogueMetaProgress progress) {
        int savedVersion = progress.getSaveVersion();
        for (RogueSaveMigration migration : inVersionOrder()) {
            if (migration.version > savedVersion) {
                migration.migrateMetaProgress(progress);
            }
        }
        progress.setSaveVersion(currentVersion());
    }

    static void applyToRun(RogueRun run) {
        int savedVersion = run.getSaveVersion();
        for (RogueSaveMigration migration : inVersionOrder()) {
            if (migration.version > savedVersion) {
                migration.migrateRun(run);
            }
        }
        run.setSaveVersion(currentVersion());
    }

    private static RogueSaveMigration[] inVersionOrder() {
        RogueSaveMigration[] migrations = values();
        Arrays.sort(migrations, Comparator.comparingInt(migration -> migration.version));
        return migrations;
    }
}
