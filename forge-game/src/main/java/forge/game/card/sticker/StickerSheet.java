package forge.game.card.sticker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import forge.game.ability.AbilityFactory;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.game.keyword.KeywordInterface;

/**
 * A sheet script lists its stickers in a {@code K:StickerSheet:STK1,STK2,...} keyword, each defined
 * in an SVar of that name.
 */
public final class StickerSheet {
    private static final String KEYWORD = "StickerSheet:";
    // CR 123.5 - the same test GameAction.changeZone keeps stickers by
    private static final EnumSet<ZoneType> PUBLIC_ZONES = EnumSet.copyOf(
            Arrays.stream(ZoneType.values()).filter(z -> !z.isHidden()).toList());

    private StickerSheet() {
    }

    public static boolean isSheet(Card c) {
        return c != null && c.getType().isStickers();
    }

    /** CR 123.3 - stickers on the player's revealed sheets that are not on an object they own. */
    public static List<Sticker> getAvailableStickers(Player p) {
        return getAvailableStickers(p, p.getCounters(CounterEnumType.TICKET));
    }

    public static List<Sticker> getAvailableStickers(Player p, int tickets) {
        List<Sticker> available = new ArrayList<>();
        collectAvailable(p, tickets, available);
        return available;
    }

    public static boolean hasAvailableSticker(Player p) {
        return collectAvailable(p, p.getCounters(CounterEnumType.TICKET), null);
    }

    private static boolean collectAvailable(Player p, int tickets, List<Sticker> out) {
        Set<String> onSomething = new HashSet<>();
        for (Card c : p.getCardsIn(PUBLIC_ZONES)) {
            for (AppliedSticker applied : c.getStickers()) {
                onSomething.add(identity(applied.getSticker()));
            }
        }
        boolean any = false;
        for (Card sheet : p.getCardsIn(ZoneType.StickerSheets)) {
            for (Sticker s : getStickers(sheet)) {
                // CR 123.3c
                if (onSomething.contains(identity(s)) || !s.isImplemented() || s.getTickets() > tickets) {
                    continue;
                }
                if (out == null) {
                    return true;
                }
                out.add(s);
                any = true;
            }
        }
        return any;
    }

    public static String describe(Card c) {
        if (isSheet(c)) {
            List<Sticker> stickers = getStickers(c);
            if (stickers.isEmpty()) {
                return "";
            }
            List<Sticker> free = c.getOwner() == null ? List.of() : getAvailableStickers(c.getOwner(),
                    Integer.MAX_VALUE);
            StringBuilder sb = new StringBuilder("Stickers on this sheet:");
            for (Sticker s : stickers) {
                boolean taken = free.stream().noneMatch(f -> identity(f).equals(identity(s)));
                sb.append("\r\n  ").append(s.getDescription());
                if (s.getTickets() > 0) {
                    sb.append(" (").append("{TK}".repeat(s.getTickets())).append(")");
                }
                if (taken) {
                    sb.append(" - used");
                }
            }
            return sb.toString();
        }
        if (!c.isStickered()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("Stickers:");
        for (AppliedSticker applied : c.getStickers()) {
            sb.append("\r\n  ").append(applied.getSticker().getDescription())
                    .append(" (").append(applied.getKind().name().toLowerCase()).append(")");
        }
        return sb.toString();
    }

    /** CR 123.3a - a sticker is its sheet and its slot, never its text. */
    private static String identity(Sticker s) {
        return s.getSheet().getId() + "/" + s.getSlot();
    }

    public static List<Sticker> getStickers(Card sheet) {
        if (!isSheet(sheet)) {
            return List.of();
        }
        List<Sticker> cached = sheet.getSheetStickers();
        if (cached == null) {
            cached = readStickers(sheet);
            sheet.setSheetStickers(cached);
        }
        return cached;
    }

    private static List<Sticker> readStickers(Card sheet) {
        List<Sticker> stickers = new ArrayList<>();
        for (KeywordInterface kw : sheet.getKeywords()) {
            String original = kw.getOriginal();
            if (original == null || !original.startsWith(KEYWORD)) {
                continue;
            }
            for (String slot : original.substring(KEYWORD.length()).split(",")) {
                slot = slot.trim();
                String sVar = sheet.getSVar(slot);
                if (sVar == null || sVar.isEmpty()) {
                    continue;
                }
                Sticker sticker = Sticker.parse(sheet, slot, AbilityFactory.getMapParams(sVar));
                if (sticker != null) {
                    stickers.add(sticker);
                }
            }
        }
        return stickers;
    }
}
