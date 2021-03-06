package forge.game.player;

import forge.game.card.Card;
import forge.game.keyword.KeywordInterface;
import forge.game.staticability.StaticAbility;

public class PlayerFactoryUtil {

    public static void addStaticAbility(final KeywordInterface inst, final Player player) {
        String keyword = inst.getOriginal();
        String effect = null;
        if (keyword.startsWith("Hexproof")) {
            final StringBuilder sbDesc = new StringBuilder("Hexproof");
            final StringBuilder sbValid = new StringBuilder();

            if (!keyword.equals("Hexproof")) {
                final String[] k = keyword.split(":");

                sbDesc.append(" from ").append(k[2]);
                sbValid.append("| ValidSource$ ").append(k[1]);
            }

            effect = "Mode$ CantTarget | Hexproof$ True | ValidPlayer$ Player.You | Secondary$ True "
                    + sbValid.toString() + " | Activator$ Opponent | EffectZone$ Command | Description$ "
                    + sbDesc.toString() + " (" + inst.getReminderText() + ")";
        } else if (keyword.equals("Shroud")) {
            effect = "Mode$ CantTarget | Shroud$ True | ValidPlayer$ Player.You | Secondary$ True "
                    + "| EffectZone$ Command | Description$ Shroud (" + inst.getReminderText() + ")";
        }
        if (effect != null) {
            final Card card = player.getKeywordCard();
            StaticAbility st = new StaticAbility(effect, card, card.getCurrentState());
            st.setIntrinsic(false);
            inst.addStaticAbility(st);
        }
    }

    public static void addTriggerAbility(final KeywordInterface inst, Player player) {
    }

    public static void addReplacementEffect(final KeywordInterface inst, Player player) {
    }

    public static void addSpellAbility(final KeywordInterface inst, Player player) {
    }
}
