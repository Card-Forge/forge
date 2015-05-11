package forge.gui;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableList;

import forge.FThreads;
import forge.game.card.CardView;
import forge.screens.match.CMatchUI;
import forge.toolbox.FOptionPane;

/**
 * Holds player interactions using standard windows
 *
 */
public class GuiDialog {
    private static final ImmutableList<String> defaultConfirmOptions = ImmutableList.of("Yes", "No");

    public static boolean confirm(final CardView c, final String question, final boolean defaultIsYes, final List<String> options, final CMatchUI matchUI) {
        final Callable<Boolean> confirmTask = new Callable<Boolean>() {
            @Override public final Boolean call() {
                if (matchUI != null && c != null) {
                    matchUI.setCard(c);
                }

                final String title = c == null ? "Question" : c + " - Ability";
                final String questionToUse = StringUtils.isBlank(question) ? "Activate card's ability?" : question;
                final List<String> opts = options == null ? defaultConfirmOptions : options;
                final int answer = FOptionPane.showOptionDialog(questionToUse, title, FOptionPane.QUESTION_ICON, opts, defaultIsYes ? 0 : 1);
                return Boolean.valueOf(answer == 0);
            }};

        final FutureTask<Boolean> future = new FutureTask<Boolean>(confirmTask);
        FThreads.invokeInEdtAndWait(future);
        try {
            return future.get().booleanValue();
        } catch (final InterruptedException | ExecutionException e) { // should be no exception here
            e.printStackTrace();
        }
        return false;
    }

}
