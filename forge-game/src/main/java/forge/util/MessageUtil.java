package forge.util;

import org.apache.commons.lang3.StringUtils;

import forge.card.CardType;
import forge.game.GameObject;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbility;

public class MessageUtil {
    private MessageUtil() { }

    public static String formatMessage(String message, Player player, Object related) {
        if (related instanceof Player && message.contains("{player")) {
            String noun = mayBeYou(player, related);
            message = TextUtil.fastReplace(TextUtil.fastReplace(message, "{player}", noun),"{player's}", Lang.getInstance().getPossesive(noun));
        }
        return message;
    }

    public static String formatMessage(String message, PlayerView player, Object related) {
        if (related instanceof PlayerView && message.contains("{player")) {
            String noun = mayBeYou(player, related);
            message = TextUtil.fastReplace(TextUtil.fastReplace(message, "{player}", noun),"{player's}", Lang.getInstance().getPossesive(noun));
        }
        return message;
    }

    // These are not much related to PlayerController
    public static  String formatNotificationMessage(SpellAbility sa, Player player, GameObject target, String value) {
        if (sa == null || sa.getApi() == null || sa.getHostCard() == null) {
            return String.valueOf(value);
        }
        String choser = StringUtils.capitalize(mayBeYou(player, target));
        switch(sa.getApi()) {
            case ChoosePlayer:
            case ChooseDirection:
            case Clash:
            case DigMultiple:
            case Seek:
                return value;
            case ChooseColor:
            case Mana:
                return sa.hasParam("Random")
                        ? Localizer.getInstance().getMessage("lblRandomColorChosen", value)
                        : Localizer.getInstance().getMessage("lblPlayerPickedChosen", choser, value);
            case ChooseNumber:
                if (sa.hasParam("Secretly")) {
                    return value;
                }
                return sa.hasParam("Random")
                        ? Localizer.getInstance().getMessage("lblPlayerRandomChosenNumberIs",
                            mayBeYou(player, target), value)
                        : Localizer.getInstance().getMessage("lblPlayerChoosesNumberIs",
                            mayBeYou(player, target), value);
            case ChooseType:
                return sa.hasParam("AtRandom")
                        ? Localizer.getInstance().getMessage("lblRandomTypeChosen", value)
                        : Localizer.getInstance().getMessage("lblPlayerPickedChosen", choser, value);
            case FlipACoin:
                String flipper = StringUtils.capitalize(mayBeYou(player, target));
                return sa.hasParam("NoCall")
                        ? Localizer.getInstance().getMessage("lblPlayerFlipComesUpValue", Lang.getInstance().getPossesive(flipper), value)
                        : Localizer.getInstance().getMessage("lblPlayerActionFlip", flipper, Lang.joinVerb(flipper, value));
            case GenericChoice:
                if ((sa.hasParam("Secretly")) || 
                    (sa.hasParam("ShowChoice") && sa.getParam("ShowChoice").equals("Description"))) {
                    return value;
                }
            case Protection:
                return Localizer.getInstance().getMessage("lblPlayerChooseValue", choser, value);
            case RollDice:
            case RollPlanarDice:
            case PutCounter:// For Clay Golem cost text
                return value;
            case Vote:
                if (sa.hasParam("Secretly")) {
                    return value;
                } else {
                    String chooser = StringUtils.capitalize(mayBeYou(player, target));
                    return Localizer.getInstance().getMessage("lblPlayerVoteValue", chooser, value);
                }
            default:
                String tgt = mayBeYou(player, target);
                if (tgt.equals("(null)")) {
                    return Localizer.getInstance().getMessage("lblCardEffectValueIs", CardTranslation.getTranslatedName(sa.getHostCard().getName()), value);
                } else {
                    return Localizer.getInstance().getMessage("lblCardEffectToTargetValueIs", CardTranslation.getTranslatedName(sa.getHostCard().getName()), tgt, value);
                }
        }
    }

    public static String mayBeYou(Player player, Object what) {
        return what == null ? "(null)" : what == player ? Localizer.getInstance().getMessage("lblYou") : what.toString();
    }
    public static String mayBeYou(PlayerView player, Object what) {
        return what == null ? "(null)" : what == player ? Localizer.getInstance().getMessage("lblYou") : what.toString();
    }

    public static String complexTargetTypesToString(String types, boolean pluralize) {
        String[] parts = types.split(";");
        StringBuilder sb = new StringBuilder();
        boolean hasOther = false;

        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                if (i == parts.length - 1) {
                    sb.append(" or ");
                } else {
                    sb.append(", ");
                }
            }

            String currentTypeTarget = parts[i];
            String[] currentTypeTargetParts = currentTypeTarget.split("\\.");
            boolean isCoreType = CardType.CoreType.isValidEnum(currentTypeTargetParts[0]);

            String baseType = isCoreType ||
                currentTypeTargetParts[0].equals("Card") ||
                currentTypeTargetParts[0].equals("Permanent")
                    ? currentTypeTargetParts[0].toLowerCase()
                    : currentTypeTargetParts[0];
            if (pluralize) {
                baseType = Lang.getPlural(baseType);
            }

            if (currentTypeTargetParts.length == 1) {
                sb.append(baseType);
                continue;
            }

            StringBuilder prefixes = new StringBuilder();
            StringBuilder suffixes = new StringBuilder();
            String[] prefixesAndSuffixesParts = currentTypeTargetParts[1].split("\\+");

            for (String pOrS : prefixesAndSuffixesParts) {
                String currentTerm = "";
                boolean isNegated = false;

                // Negation
                if (pOrS.startsWith("non")) {
                    currentTerm += " non";
                    isNegated = true;

                    pOrS = pOrS.substring(3);
                }

                if (pOrS.startsWith("!")) {
                    isNegated = true;

                    pOrS = pOrS.substring(1);
                }

                // Prefixes
                if (pOrS.equals("Other")) {
                    if (hasOther) {
                        continue;
                    }

                    currentTerm += pluralize ? "other " : "another ";
                    prefixes.append(currentTerm);
                    hasOther = true;
                    continue;
                }

                if (CardType.Supertype.isValidEnum(pOrS) ||
                    CardType.CoreType.isValidEnum(pOrS)) {
                    currentTerm += pOrS.toLowerCase();
                    prefixes.append(currentTerm);
                    continue;
                }

                if (CardType.isASubType(pOrS)) {
                    if (isNegated) {
                        currentTerm += "-";
                    }
                    currentTerm += StringUtils.capitalize(pOrS.toLowerCase());
                    prefixes.append(currentTerm);
                    continue;
                }
                
                // Suffixes
                switch (pOrS) {
                    case "YouOwn" -> {
                        currentTerm += " you own";
                        suffixes.append(currentTerm);
                        continue;
                    }
                    case "YouCtrl" -> {
                        currentTerm += " you control";
                        suffixes.append(currentTerm);
                        continue;
                    }
                    case "OppCtrl" -> {
                        currentTerm += " an opponent controls";
                        suffixes.append(currentTerm);
                        continue;
                    }
                    case "OppOwn" -> {
                        currentTerm += " an opponent owns";
                        suffixes.append(currentTerm);
                        continue;
                    }
                }

                if (pOrS.startsWith("cmc")) {
                    currentTerm += " with mana value ";

                    String rest = pOrS.substring(3);
                    String value = rest.substring(2);

                    if (rest.startsWith("LT")) {
                        currentTerm += "less than ";
                    } else if (rest.startsWith("GT")) {
                        currentTerm += "greater than ";
                    } else if (rest.startsWith("EQ")) {
                        currentTerm += "equal to ";
                    } else if (rest.startsWith("LE")) {
                        currentTerm += "less or equal to ";
                    } else if (rest.startsWith("GE")) {
                        currentTerm += "greater or equal to ";
                    }

                    currentTerm += value;
                }

                // Special case: token
                if (pOrS.equals("token")) {
                    if (isNegated) {
                        currentTerm += " non";
                    }

                    currentTerm += "token";

                    if (isCoreType) {
                        prefixes.append(currentTerm);
                    } else {
                        suffixes.append(currentTerm);
                    }

                    continue;
                }

                // Unknown/unsupported terms
                currentTerm += pOrS.toLowerCase();
                prefixes.append(currentTerm);
                System.out.println("Warning: unsupported term found: \"" + pOrS + "\"");
            }

            sb.append(prefixes).append(" ");
            // Anje, Maid of Dishonor
            if (!isCoreType && i > 0) {
                baseType = Lang.startsWithVowel(baseType) ? "an " : "a " + baseType;
            }
            sb.append(baseType).append(" ");
            sb.append(suffixes);
        }

        return sb.toString().trim().replaceAll("\\s+", " ");
    }
}
