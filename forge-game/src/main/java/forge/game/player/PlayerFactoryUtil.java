package forge.game.player;

import forge.card.CardType;
import forge.game.card.Card;
import forge.game.keyword.KeywordInterface;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
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
        String keyword = inst.getOriginal();
        String effect = null;

        if (keyword.startsWith("Protection")) {
            String validSource = "Card.";
            if (keyword.startsWith("Protection:")) {
                final String[] kws = keyword.split(":");
                String characteristic = kws[1];
                if (characteristic.startsWith("Player")) {
                    validSource += "ControlledBy " + characteristic;
                } else {
                    if (characteristic.endsWith("White") || characteristic.endsWith("Blue")
                        || characteristic.endsWith("Black") || characteristic.endsWith("Red")
                        || characteristic.endsWith("Green") || characteristic.endsWith("Colorless")
                        || characteristic.endsWith("MonoColor") || characteristic.endsWith("MultiColor")) {
                        characteristic += "Source";
                    }
                    validSource = characteristic;
                }
            } else if (keyword.startsWith("Protection from ")) {
                String protectType = keyword.substring("Protection from ".length());
                if (protectType.equals("white")) {
                    validSource += "WhiteSource";
                } else if (protectType.equals("blue")) {
                    validSource += "BlueSource";
                } else if (protectType.equals("black")) {
                    validSource += "BlackSource";
                } else if (protectType.equals("red")) {
                    validSource += "RedSource";
                } else if (protectType.equals("green")) {
                    validSource += "GreenSource";
                } else if (protectType.equals("all colors")) {
                    validSource += "nonColorless";
                } else if (protectType.equals("everything")) {
                    validSource = "";
                } else {
                    validSource = CardType.getSingularType(protectType);
                }
            }

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
