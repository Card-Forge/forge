package forge.game.player;

import forge.game.card.Card;
import forge.game.keyword.Hexproof;
import forge.game.keyword.KeywordInterface;
import forge.game.keyword.Protection;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
import forge.game.staticability.StaticAbility;

public class PlayerFactoryUtil {

    public static void addStaticAbility(final KeywordInterface inst, final Player player) {
        String keyword = inst.getOriginal();

        if (keyword.startsWith("Hexproof") && inst instanceof Hexproof hexproof) {
            final StringBuilder sbValid = new StringBuilder();
            if (!hexproof.getValidType().isEmpty()) {
                sbValid.append("| ValidSource$ ").append(hexproof.getValidType());
            }
            String effect = "Mode$ CantTarget | ValidTarget$ Player.You | Secondary$ True "
                    + sbValid + " | Activator$ Opponent | EffectZone$ Command | Description$ "
                    + inst.getTitle() + " (" + inst.getReminderText() + ")";

            final Card card = player.getKeywordCard();
            inst.addStaticAbility(StaticAbility.create(effect, card, card.getCurrentState(), false));
        } else if (keyword.equals("Shroud")) {
            String effect = "Mode$ CantTarget | ValidTarget$ Player.You | Secondary$ True "
                    + "| EffectZone$ Command | Description$ Shroud (" + inst.getReminderText() + ")";

            final Card card = player.getKeywordCard();
            inst.addStaticAbility(StaticAbility.create(effect, card, card.getCurrentState(), false));
        } else if (keyword.startsWith("Protection")) {
            String valid = Protection.getProtectionValid(keyword, false);
            String effect = "Mode$ CantTarget | ValidTarget$ Player.You | EffectZone$ Command | Secondary$ True ";
            if (!valid.isEmpty()) {
                effect += "| ValidSource$ " + valid;
            }
            final Card card = player.getKeywordCard();
            inst.addStaticAbility(StaticAbility.create(effect, card, card.getCurrentState(), false));

            // Attach
            effect = "Mode$ CantAttach | Target$ Player.You | EffectZone$ Command | Secondary$ True ";
            if (!valid.isEmpty()) {
                effect += "| ValidCard$ " + valid;
            }
            // This effect doesn't remove something
            if (keyword.startsWith("Protection:")) {
                final String[] kws = keyword.split(":");
                if (kws.length > 3) {
                    effect += " | Exceptions$ " + kws[3];
                }
            }
            inst.addStaticAbility(StaticAbility.create(effect, card, card.getCurrentState(), false));
        }
    }

    public static void addTriggerAbility(final KeywordInterface inst, Player player) {
    }

    public static void addReplacementEffect(final KeywordInterface inst, Player player) {
        String keyword = inst.getOriginal();
        String effect = null;

        if (keyword.startsWith("Protection")) {
            String validSource = Protection.getProtectionValid(keyword, true);

            effect = "Event$ DamageDone | Prevent$ True | ActiveZones$ Command | ValidTarget$ You";
            if (!validSource.isEmpty()) {
                effect += " | ValidSource$ " + validSource;
            }
            effect += " | Secondary$ True | Description$ " + keyword;
        }

        if (effect != null) {
            final Card card = player.getKeywordCard();
            ReplacementEffect re = ReplacementHandler.parseReplacement(effect, card, false, card.getCurrentState());
            inst.addReplacement(re);
        }
    }

    public static void addSpellAbility(final KeywordInterface inst, Player player) {
    }
}
