package forge.toolbox;

import com.google.common.collect.ImmutableList;
import forge.game.card.CardView;
import forge.util.Callback;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/** 
 * Holds player interactions using standard windows 
 */
public class GuiDialog {
    private static final ImmutableList<String> defaultConfirmOptions = ImmutableList.of("Yes", "No");

    public static void confirm(final CardView c, final String question, final Callback<Boolean> callback) {
        GuiDialog.confirm(c, question, true, null, callback);
    }
    public static void confirm(final CardView c, final String question, final boolean defaultChoice, final Callback<Boolean> callback) {
        GuiDialog.confirm(c, question, defaultChoice, null, callback);
    }
    public static void confirm(final CardView c, final String question, final List<String> options, final Callback<Boolean> callback) {
        GuiDialog.confirm(c, question, true, options, callback);
    }

    public static void confirm(final CardView c, final String question, final boolean defaultIsYes, final List<String> options, final Callback<Boolean> callback) {
        final String title = c == null ? "Question" : c + " - Ability";
        String questionToUse = StringUtils.isBlank(question) ? "Activate card's ability?" : question;
        final List<String> opts = options == null ? defaultConfirmOptions : options;
        FOptionPane.showCardOptionDialog(c, questionToUse, title, FOptionPane.QUESTION_ICON, opts, defaultIsYes ? 0 : 1, new Callback<Integer>() {
            @Override public void run(final Integer result) {
                callback.run(result.intValue() == 0);
            }
        });
    }

    /**
     * <p>
     * showInfoDialg.
     * </p>
     * 
     * @param message
     *            a {@link java.lang.String} object.
     */
    public static void message(final String message) {
        message(message, "Forge");
    }

    public static void message(final String message, final String title) {
        FOptionPane.showMessageDialog(message, title, null);
    }
}
