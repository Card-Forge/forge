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
            message = TextUtil.fastReplace(TextUtil.fastReplace(message, "{player}", noun),"{player's}", Lang.getPossesive(noun));
        }
        return message;
    }

    public static String formatMessage(String message, PlayerView player, Object related) {
        if (related instanceof PlayerView && message.indexOf("{player") >= 0) {
            String noun = mayBeYou(player, related);
            message = TextUtil.fastReplace(TextUtil.fastReplace(message, "{player}", noun),"{player's}", Lang.getPossesive(noun));
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
                return sa.hasParam("Random")
                        ? TextUtil.concatWithSpace("Randomly chosen number for", mayBeYou(player, target),"is", value)
                        : TextUtil.concatWithSpace( mayBeYou(player, target),"chooses number:", value);
            case ChooseType:
                return TextUtil.concatWithSpace(choser, Lang.joinVerb(choser, "choose"), value, "for effect of", sa.getHostCard().getName());
            case FlipACoin:
                String flipper = StringUtils.capitalize(mayBeYou(player, target));
                return sa.hasParam("NoCall")
                        ? TextUtil.concatWithSpace(Lang.getPossesive(flipper),"flip comes up", value)
                        : TextUtil.concatWithSpace(flipper, Lang.joinVerb(flipper, value), "the flip");
            case Protection:
                return TextUtil.concatWithSpace(choser, Lang.joinVerb(choser, "choose"), value);
            case Vote:
                String chooser = StringUtils.capitalize(mayBeYou(player, target));
                return TextUtil.concatWithSpace(chooser, Lang.joinVerb(chooser,"vote"), value);
            default:
                String tgt = mayBeYou(player, target);
                if (tgt.equals("(null)")) {
                    return TextUtil.concatWithSpace(sa.getHostCard().getName(),"effect's value is", value);
                } else {
                    return TextUtil.concatWithSpace(sa.getHostCard().getName(),"effect's value for", tgt,"is", value);
                }
        }
    }

    public static String mayBeYou(Player player, Object what) {
        return what == null ? "(null)" : what == player ? "you" : what.toString();
    }
    public static String mayBeYou(PlayerView player, Object what) {
        return what == null ? "(null)" : what == player ? "you" : what.toString();
    }
}
