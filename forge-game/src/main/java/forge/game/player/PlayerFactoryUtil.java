package forge.game.player;

import forge.game.card.Card;
import forge.game.card.CardFactoryUtil;
import forge.game.keyword.KeywordInterface;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
import forge.game.staticability.StaticAbility;

public class PlayerFactoryUtil {

    public static void addStaticAbility(final KeywordInterface inst, final Player player) {
        String keyword = inst.getOriginal();

        if (keyword.startsWith("Hexproof")) {
            final StringBuilder sbDesc = new StringBuilder("Hexproof");
            final StringBuilder sbValid = new StringBuilder();

            if (!keyword.equals("Hexproof")) {
                final String[] k = keyword.split(":");

                sbDesc.append(" from ").append(k[2]);
                sbValid.append("| ValidSource$ ").append(k[1]);
            }

            String effect = "Mode$ CantTarget | Hexproof$ True | ValidPlayer$ Player.You | Secondary$ True "
                    + sbValid.toString() + " | Activator$ Opponent | EffectZone$ Command | Description$ "
                    + sbDesc.toString() + " (" + inst.getReminderText() + ")";

            final Card card = player.getKeywordCard();
            inst.addStaticAbility(StaticAbility.create(effect, card, card.getCurrentState(), false));
        } else if (keyword.equals("Shroud")) {
            String effect = "Mode$ CantTarget | Shroud$ True | ValidPlayer$ Player.You | Secondary$ True "
                    + "| EffectZone$ Command | Description$ Shroud (" + inst.getReminderText() + ")";

            final Card card = player.getKeywordCard();
            inst.addStaticAbility(StaticAbility.create(effect, card, card.getCurrentState(), false));
        } else if (keyword.startsWith("Protection")) {
            String valid = CardFactoryUtil.getProtectionValid(keyword, false);
            String effect = "Mode$ CantTarget | Protection$ True | ValidCard$ Player.You | Secondary$ True ";
            if (!valid.isEmpty()) {
                effect += "| ValidSource$ " + valid;
            }
            final Card card = player.getKeywordCard();
            inst.addStaticAbility(StaticAbility.create(effect, card, card.getCurrentState(), false));

            // Attach
            effect = "Mode$ CantAttach | Protection$ True | Target$ Player.You | Secondary$ True ";
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
            String validSource = CardFactoryUtil.getProtectionValid(keyword, true);

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
