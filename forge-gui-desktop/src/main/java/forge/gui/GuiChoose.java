package forge.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import javax.swing.JList;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import forge.card.CardStateName;
import forge.game.card.CardFaceView;
import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Function;

import forge.FThreads;
import forge.card.ICardFace;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.screens.match.CMatchUI;
import forge.toolbox.FOptionPane;
import forge.view.arcane.ListCardArea;
import forge.util.Localizer;

public class GuiChoose {

    /**
     * Convenience for getChoices(message, 0, 1, choices).
     *
     * @param <T>
     *            is automatically inferred.
     * @param message
     *            a {@link java.lang.String} object.
     * @param choices
     *            a T object.
     * @return null if choices is missing, empty, or if the users' choices are
     *         empty; otherwise, returns the first item in the List returned by
     *         getChoices.
     * @see #getChoices(String, int, int, Object...)
     */
    public static <T> T oneOrNone(final String message, final T[] choices) {
        if ((choices == null) || (choices.length == 0)) {
            return null;
        }
        final List<T> choice = GuiChoose.getChoices(message, 0, 1, choices);
        return choice.isEmpty() ? null : choice.get(0);
    } // getChoiceOptional(String,T...)

    public static <T> T oneOrNone(final String message, final Collection<T> choices) {
        if ((choices == null) || choices.isEmpty()) {
            return null;
        }
        final List<T> choice = GuiChoose.getChoices(message, 0, 1, choices);
        return choice.isEmpty() ? null : choice.get(0);
    } // getChoiceOptional(String,T...)

    // Get Integer in range
    public static Integer getInteger(final String message, final int min, final int max) {
        if (max <= min) { return min; } //just return min if max <= min

        //force cutting off after 100 numbers at most
        if (max == Integer.MAX_VALUE) {
            return getInteger(message, min, max, min + 99);
        }
        final int count = max - min + 1;
        if (count > 100) {
            return getInteger(message, min, max, min + 99);
        }

        final Integer[] choices = new Integer[count];
        for (int i = 0; i < count; i++) {
            choices[i] = Integer.valueOf(i + min);
        }
        return GuiChoose.oneOrNone(message, choices);
    }
    public static Integer getInteger(final String message, final int min, final int max, final int cutoff) {
        final Localizer localizer = Localizer.getInstance();
        if (max <= min || cutoff < min) { return min; } //just return min if max <= min or cutoff < min

        if (cutoff >= max) { //fallback to regular integer prompt if cutoff at or after max
            return getInteger(message, min, max);
        }

        final List<Object> choices = new ArrayList<>();
        for (int i = min; i <= cutoff; i++) {
            choices.add(Integer.valueOf(i));
        }
        choices.add(Localizer.getInstance().getMessage("lblOtherInteger"));

        final Object choice = GuiChoose.oneOrNone(message, choices);
        if (choice instanceof Integer || choice == null) {
            return (Integer)choice;
        }

        //if Other option picked, prompt for number input
        String prompt = "";
        if (min != Integer.MIN_VALUE) {
            if (max != Integer.MAX_VALUE) {
                prompt = localizer.getMessage("lblEnterNumberBetweenMinAndMax", String.valueOf(min), String.valueOf(max));
            }
            else {
                prompt = localizer.getMessage("lblEnterNumberGreaterThanOrEqualsToMin", String.valueOf(min));
            }
        }
        else if (max != Integer.MAX_VALUE) {
            prompt = localizer.getMessage("lblEnterNumberLessThanOrEqualsToMax", String.valueOf(max));
        }

        while (true) {
            final String str = FOptionPane.showInputDialog(prompt, message);
            if (str == null) { return null; } // that is 'cancel'

            if (StringUtils.isNumeric(str)) {
                final Integer val = Integer.valueOf(str);
                if (val >= min && val <= max) {
                    return val;
                }
            }
        }
    }

    // returned Object will never be null
    public static <T> List<T> getChoices(final String message, final int min, final int max, final T[] choices) {
        return getChoices(message, min, max, Arrays.asList(choices), null, null);
    }

    public static <T> List<T> getChoices(final String message, final int min, final int max, final Collection<T> choices) {
        return getChoices(message, min, max, choices, null, null);
    }

    public static <T> List<T> getChoices(final String message, final int min, final int max, final Collection<T> choices, final T selected, final Function<T, String> display) {
        return getChoices(message, min, max, choices, selected, display, null);
    }
    public static <T> List<T> getChoices(final String message, final int min, final int max, final Collection<T> choices, final T selected, final Function<T, String> display, final CMatchUI matchUI) {
        if (choices == null || choices.isEmpty()) {
            if (min == 0) {
                return new ArrayList<>();
            }
            throw new RuntimeException("choice required from empty list");
        }

        final Callable<List<T>> showChoice = new Callable<List<T>>() {
            @Override
            public List<T> call() {
                final ListChooser<T> c = new ListChooser<>(message, min, max, choices, display);
                final JList<T> list = c.getLstChoices();
                if (matchUI != null) {
                    list.addListSelectionListener(new ListSelectionListener() {
                        @Override
                        public void valueChanged(final ListSelectionEvent ev) {
                            final T sel = list.getSelectedValue();
                            if (sel instanceof InventoryItem) {
                                matchUI.setCard((InventoryItem) list.getSelectedValue());
                                return;
                            } else if (sel instanceof ICardFace || sel instanceof CardFaceView) {
                                String faceName;
                                if (sel instanceof ICardFace) {
                                    faceName = ((ICardFace) sel).getName();
                                } else {
                                    faceName = ((CardFaceView) sel).getOracleName();
                                }
                                PaperCard paper = FModel.getMagicDb().getCommonCards().getUniqueByName(faceName);
                                if (paper == null) {
                                    paper = FModel.getMagicDb().getVariantCards().getUniqueByName(faceName);
                                }

                                if (paper != null) {
                                    Card c = Card.getCardForUi(paper);
                                    boolean foundState = false;
                                    for (CardStateName cs : c.getStates()) {
                                        if (c.getState(cs).getName().equals(faceName)) {
                                            foundState = true;
                                            c.setState(cs, true);
                                            matchUI.setCard(c.getView());
                                            break;
                                        }
                                    }
                                    if (!foundState) {
                                        matchUI.setCard(paper);
                                    }
                                }

                                return;
                            }

                            final CardView card;
                            if (sel instanceof CardStateView) {
                                card = ((CardStateView) sel).getCard();
                            } else if (sel instanceof CardView) {
                                card = (CardView) sel;
                            } else if (sel instanceof Card) {
                                card = CardView.get((Card) sel);
                            } else {
                                card = null;
                            }

                            matchUI.setCard(card);
                            matchUI.clearPanelSelections();
                            matchUI.setPanelSelection(card);
                        }
                    });
                }

                if (selected != null) {
                    c.show(selected);
                }
                else {
                    c.show();
                }

                if (matchUI != null) {
                    matchUI.clearPanelSelections();
                }
                return c.getSelectedValues();
            }
        };

        final FutureTask<List<T>> future = new FutureTask<>(showChoice);
        FThreads.invokeInEdtAndWait(future);
        try {
            return future.get();
        } catch (final Exception e) { // should be no exception here
            e.printStackTrace();
        }
        return null;
    }

    public static <T extends Comparable<? super T>> List<T> sideboard(final CMatchUI matchUI, final List<T> sideboard, final List<T> deck, final String message) {
        Collections.sort(deck);
        Collections.sort(sideboard);
        return order(Localizer.getInstance().getMessage("lblSideboardForPlayer", message), Localizer.getInstance().getMessage("ttMain"), -1, -1, sideboard, deck, null, true, matchUI);
    }

    public static <T> List<T> order(final String title, final String top, final int remainingObjectsMin, final int remainingObjectsMax,
            final List<T> sourceChoices, final List<T> destChoices) {
        return order(title, top, remainingObjectsMin, remainingObjectsMax, sourceChoices, destChoices, null, false, null);
    }
    public static <T> List<T> order(final String title, final String top, final int remainingObjectsMin, final int remainingObjectsMax,
            final List<T> sourceChoices, final List<T> destChoices, final CardView referenceCard, final boolean sideboardingMode, final CMatchUI matchUI) {
        // An input box for handling the order of choices.

        final Callable<List<T>> callable = new Callable<List<T>>() {
            @Override
            public List<T> call() {
                final DualListBox<T> dual = new DualListBox<>(remainingObjectsMin, remainingObjectsMax, sourceChoices, destChoices, matchUI);
                dual.setSecondColumnLabelText(top);

                dual.setSideboardMode(sideboardingMode);

                dual.setTitle(title);
                dual.pack();
                dual.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                if (matchUI != null && referenceCard != null) {
                    matchUI.setCard(referenceCard);
                    // MARKED FOR UPDATE
                }
                dual.setVisible(true);

                final List<T> objects = dual.getOrderedList();

                dual.dispose();
                if (matchUI != null) {
                    matchUI.clearPanelSelections();
                }
                return objects;
            }
        };

        final FutureTask<List<T>> ft = new FutureTask<>(callable);
        FThreads.invokeInEdtAndWait(ft);
        try {
            return ft.get();
        } catch (final Exception e) { // we have waited enough
            e.printStackTrace();
        }
        return null;
    }

    public static List<CardView> manipulateCardList(final CMatchUI gui, final String title, final Iterable<CardView> cards, final Iterable<CardView> manipulable, 
						    final boolean toTop, final boolean toBottom, final boolean toAnywhere) {
	gui.setSelectables(manipulable);
	@SuppressWarnings("Convert2Lambda") // Avoid lambdas to maintain compatibility with Android 5 API
    final Callable<List<CardView>> callable = new Callable<List<CardView>>() {
        @Override
        public List<CardView> call()  {
            ListCardArea tempArea = ListCardArea.show(gui,title,cards,manipulable,toTop,toBottom,toAnywhere);

            //		tempArea.pack();
            tempArea.setVisible(true);
            return tempArea.getCards();
        }
    };
	final FutureTask<List<CardView>> ft = new FutureTask<>(callable);
        FThreads.invokeInEdtAndWait(ft);
	gui.clearSelectables();
        try {
            List<CardView> result = ft.get();
            return result;
        } catch (final Exception e) { // we have waited enough
            e.printStackTrace();
        }
        return null;
    }

}

