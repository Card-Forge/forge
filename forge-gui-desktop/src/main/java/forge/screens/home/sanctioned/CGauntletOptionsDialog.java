package forge.screens.home.sanctioned;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.util.Localizer;

/**
 * Controller for `VGauntletOptionsDialog`.
 */
public enum CGauntletOptionsDialog {
    SINGLETON_INSTANCE;

    private static final int MIN_ROUNDS = 1;
    private static final int MAX_ROUNDS = 99;
    private static final int DEFAULT_ROUNDS = 4;

    public static final class GauntletOptions {
        public final int rounds;
        public final int gamesPerMatch;

        public GauntletOptions(final int rounds0, final int gamesPerMatch0) {
            this.rounds = rounds0;
            this.gamesPerMatch = gamesPerMatch0;
        }
    }

    private final Localizer localizer = Localizer.getInstance();

    public GauntletOptions showDialog() {
        final VGauntletOptionsDialog view = new VGauntletOptionsDialog();
        loadDefaults(view);

        // Keep the dialog focused on values that are actually persisted.
        view.setTitle(localizer.getMessageorUseDefault("lblGauntletOptions", "Gauntlet Options"));

        final Object[] result = new Object[1];

        view.getBtnOk().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    final int rounds = parseBoundedInt(view.getRoundsText(), MIN_ROUNDS, MAX_ROUNDS, DEFAULT_ROUNDS);
                    final int games = parseBoundedInt(view.getSelectedMatchLength(), 1, 9, 3);
                    result[0] = new GauntletOptions(rounds, games);
                    persistDefaults(rounds, games);
                    view.dispose();
                } catch (NumberFormatException ex) {
                    // invalid input: show minimal feedback by updating title
                    view.setTitle(localizer.getMessage("lblInvalidNumber", "Invalid number"));
                }
            }
        });

        view.getBtnCancel().addActionListener(e -> {
            result[0] = null;
            view.dispose();
        });

        view.setVisible(true);
        return (GauntletOptions) result[0];
    }

    private void loadDefaults(final VGauntletOptionsDialog view) {
        final ForgePreferences prefs = FModel.getPreferences();
        final String rounds = prefs.getPref(ForgePreferences.FPref.UI_GAUNTLET_ROUNDS);
        final String matches = prefs.getPref(ForgePreferences.FPref.UI_MATCHES_PER_GAME);
        view.setRoundsText(safeDefault(rounds, String.valueOf(DEFAULT_ROUNDS)));
        view.setMatchLength(safeDefault(matches, "3"));
    }

    private void persistDefaults(final int rounds, final int gamesPerMatch) {
        final ForgePreferences prefs = FModel.getPreferences();
        prefs.setPref(ForgePreferences.FPref.UI_GAUNTLET_ROUNDS, String.valueOf(rounds));
        prefs.setPref(ForgePreferences.FPref.UI_MATCHES_PER_GAME, String.valueOf(gamesPerMatch));
        prefs.save();
    }

    private int parseBoundedInt(final String rawValue, final int minimum, final int maximum, final int fallback) {
        final int parsed = Integer.parseInt(rawValue.trim());
        if (parsed < minimum || parsed > maximum) {
            throw new NumberFormatException("Value out of range: " + parsed);
        }
        return parsed;
    }

    private String safeDefault(final String value, final String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }
}
