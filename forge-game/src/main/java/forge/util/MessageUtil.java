package forge.util;

import org.apache.commons.lang3.StringUtils;

import forge.game.GameObject;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbility;


public class MessageUtil {
    private MessageUtil() { }

    public static String formatMessage(String message, Player player, Object related) {
        if (related instanceof Player && message.indexOf("{player") >= 0) {
            String noun = mayBeYou(player, related);
            message = TextUtil.fastReplace(TextUtil.fastReplace(message, "{player}", noun),"{player's}", Lang.getInstance().getPossesive(noun));
        }
        return message;
    }

    public static String formatMessage(String message, PlayerView player, Object related) {
        if (related instanceof PlayerView && message.indexOf("{player") >= 0) {
            String noun = mayBeYou(player, related);
            message = TextUtil.fastReplace(TextUtil.fastReplace(message, "{player}", noun),"{player's}", Lang.getInstance().getPossesive(noun));
        }
        return message;
    }

    // These are not much related to PlayerController
    public static  String formatNotificationMessage(SpellAbility sa, Player player, GameObject target, String value) {
        if (sa == null || sa.getApi() == null || sa.getHostCard() == null) {
            return Localizer.getInstance().getMessage("lblResultIs", value);
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
}
