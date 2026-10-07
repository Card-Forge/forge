package forge.game;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A field a {@link GameCheckpoint} leaves as it is when it puts the game back to an earlier state:
 * bookkeeping that has to keep moving forward rather than part of the game state.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface KeptOnRestore {
}
