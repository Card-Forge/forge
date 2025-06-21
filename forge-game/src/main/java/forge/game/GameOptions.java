package forge.game;

import forge.game.config.ConfigOption;

import static forge.game.config.ConfigOption.key;

public class GameOptions {

    public static final ConfigOption<Boolean> EXPERIMENTAL_RESTORE_SNAPSHOT =
            key("game.experimental-restore-snapshot")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Experimental Snapshots for undoing spell/abilities")
                    .build();

    public static final ConfigOption<Integer> AI_TIMEOUT =
            key("game.ai.timeout")
                    .intType()
                    .defaultValue(5)
                    .withDescription("How long (in seconds) the AI take to compute attacks")
                    .build();

    public static final ConfigOption<Boolean> AI_CAN_USE_TIMEOUT =
            key("game.ai.can-use-timeout")
                    .booleanType()
                    .defaultValue(true)
                    .withDescription("Set this to false if the platform is restricted from using CompletableFuture.completeOnTimeout()")
                    .build();
}
