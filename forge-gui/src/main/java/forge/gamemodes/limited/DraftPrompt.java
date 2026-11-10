package forge.gamemodes.limited;

import forge.gui.util.SGuiChoose;
import forge.item.PaperCard;
import forge.util.MyRandom;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * A choice the engine asks one seat to make. Exactly one option list is used;
 * {@code max == 0} marks a prompt that only shows cards and expects no answer.
 */
public record DraftPrompt(int promptId, int seatIndex, String message,
                          List<PaperCard> cardOptions, List<String> textOptions,
                          int min, int max) implements Serializable {
    private static final long serialVersionUID = 1L;

    public DraftPrompt {
        cardOptions = List.copyOf(cardOptions);
        textOptions = List.copyOf(textOptions);
    }

    public static DraftPrompt cards(int seat, String message, List<PaperCard> options, boolean optional) {
        return new DraftPrompt(0, seat, message, options, List.of(), optional ? 0 : 1, 1);
    }

    public static DraftPrompt text(int seat, String message, List<String> options, boolean optional) {
        return new DraftPrompt(0, seat, message, List.of(), options, optional ? 0 : 1, 1);
    }

    public static DraftPrompt anyCards(int seat, String message, List<PaperCard> options) {
        return new DraftPrompt(0, seat, message, options, List.of(), 0, options.size());
    }

    public static DraftPrompt info(int seat, String message, List<PaperCard> cards) {
        return new DraftPrompt(0, seat, message, cards, List.of(), 0, 0);
    }

    public DraftPrompt withPromptId(int newPromptId) {
        return new DraftPrompt(newPromptId, seatIndex, message, cardOptions, textOptions, min, max);
    }

    public boolean isInfoOnly() {
        return max == 0;
    }

    public List<?> options() {
        return cardOptions.isEmpty() ? textOptions : cardOptions;
    }

    public boolean isValidAnswer(List<Integer> chosen) {
        if (chosen == null || chosen.size() < min || chosen.size() > max) {
            return false;
        }
        int count = options().size();
        return new HashSet<>(chosen).size() == chosen.size()
                && chosen.stream().allMatch(i -> i != null && i >= 0 && i < count);
    }

    /** Declines an optional prompt; picks at random for a mandatory one. */
    public List<Integer> defaultAnswer() {
        if (min == 0) {
            return List.of();
        }
        return List.of(MyRandom.getRandom().nextInt(options().size()));
    }

    /** Blocking local answer through the GUI, for local drafts. */
    public List<Integer> answerLocally() {
        if (isInfoOnly()) {
            SGuiChoose.reveal(message, options());
            return List.of();
        }
        return indicesOf(options(), SGuiChoose.getChoices(message, min, max, options()));
    }

    /** Maps chosen items back to option indices; equal duplicates take successive indices. */
    public static List<Integer> indicesOf(List<?> options, List<?> chosen) {
        List<Integer> result = new ArrayList<>();
        if (chosen == null) {
            return result;
        }
        for (Object c : chosen) {
            for (int i = 0; i < options.size(); i++) {
                if (!result.contains(i) && options.get(i).equals(c)) {
                    result.add(i);
                    break;
                }
            }
        }
        return result;
    }
}
