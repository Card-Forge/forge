package forge.game;

import com.google.common.base.Function;

public interface IIdentifiable {
    int getId();
    Function<IIdentifiable, Integer> FN_GET_ID = new Function<IIdentifiable, Integer>() {
        @Override
        public Integer apply(final IIdentifiable input) {
            return Integer.valueOf(input.getId());
        }
    };
}
