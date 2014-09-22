package forge.game;

import com.google.common.base.Function;

public interface IIdentifiable {
    public abstract int getId();
    public static final Function<IIdentifiable, Integer> FN_GET_ID = new Function<IIdentifiable, Integer>() {
        @Override
        public Integer apply(final IIdentifiable input) {
            return Integer.valueOf(input.getId());
        }
    };
}
