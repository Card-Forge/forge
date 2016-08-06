package forge.util;

import org.apache.commons.lang3.StringUtils;

import forge.game.GameObject;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbility;

public class MessageUtil {
    private MessageUtil() { };

    public static String formatMessage(String message, Player player, Object related) {
        if (related instanceof Player && message.indexOf("{player") >= 0) {
            String noun = mayBeYou(player, related);
            message = message.replace("{player}", noun).replace("{player's}", Lang.getPossesive(noun));
        }
        return message;
    }

    public static String formatMessage(String message, PlayerView player, Object related) {
        if (related instanceof PlayerView && message.indexOf("{player") >= 0) {
            String noun = mayBeYou(player, related);
            message = message.replace("{player}", noun).replace("{player's}", Lang.getPossesive(noun));
        }
        return message;
    }

    // These are not much related to PlayerController
    public static  String formatNotificationMessage(SpellAbility sa, Player player, GameObject target, String value) {
        if (sa == null || sa.getApi() == null || sa.getHostCard() == null) {
            return ("Result: " + value);
        }
        String choser = StringUtils.capitalize(mayBeYou(player, target));
        switch(sa.getApi()) {
            case ChooseDirection:
                return value;
            case ChooseNumber:
                if (sa.hasParam("SecretlyChoose")) {
                    return value;
                }
                final boolean random = sa.hasParam("Random");
                return String.format(random ? "Randomly chosen number for %s is %s" : "%s chooses number: %s", mayBeYou(player, target), value);
            case ChooseType:
                return String.format("%s chooses %s for effect of %s", choser, value, sa.getHostCard().getName());
            case FlipACoin:
                String flipper = StringUtils.capitalize(mayBeYou(player, target));
                return sa.hasParam("NoCall")
                        ? String.format("%s flip comes up %s", Lang.getPossesive(flipper), value)
                        : String.format("%s %s the flip", flipper, Lang.joinVerb(flipper, value));
            case Protection:
                return String.format("%s %s protection from %s", choser, Lang.joinVerb(choser, "choose"), value);
            case Vote:
                String chooser = StringUtils.capitalize(mayBeYou(player, target));
                return String.format("%s %s %s", chooser, Lang.joinVerb(chooser, "vote"), value);
            default:
                return String.format("%s effect's value for %s is %s", sa.getHostCard().getName(), mayBeYou(player, target), value);
        }
    }

    public static String mayBeYou(Player player, Object what) {
        return what == null ? "(null)" : what == player ? "you" : what.toString();
    }
    public static String mayBeYou(PlayerView player, Object what) {
        return what == null ? "(null)" : what == player ? "you" : what.toString();
    }
}
