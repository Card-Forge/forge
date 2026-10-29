package forge.game.ability.effects;

import forge.game.card.Card;
import forge.game.card.CardState;
import forge.game.card.sticker.AppliedSticker;
import forge.game.card.sticker.StickerKind;
import forge.game.spellability.SpellAbility;
import forge.util.CardTranslation;
import forge.util.Localizer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * TODO: Write javadoc for this type.
 *
 */
public class PermanentCreatureEffect extends PermanentEffect {

    @Override
    public String getStackDescription(final SpellAbility sa) {
        final CardState source = sa.getCardState();
        final StringBuilder sb = new StringBuilder();
        String name = CardTranslation.getTranslatedName(source.getName());
        String power = sa.getParamOrDefault("SetPower", source.getBasePowerString());
        String toughness = sa.getParamOrDefault("SetToughness", source.getBaseToughnessString());
        if (sa.getHostCard().isStickered()) {
            List<AppliedSticker> stickers = sa.getHostCard().getStickers();
            List<AppliedSticker> nameStickers = new ArrayList<>();
            List<AppliedSticker> ptStickers = new ArrayList<>();
            for (AppliedSticker s : stickers) {
                if (s.getKind() == StickerKind.NAME) {
                    nameStickers.add(s);
                } else if (s.getKind() == StickerKind.PT) {
                    ptStickers.add(s);
                }
            }
            nameStickers.sort(Comparator.comparingLong(AppliedSticker::getTimestamp));
            ptStickers.sort(Comparator.comparingLong(AppliedSticker::getTimestamp));

            for (AppliedSticker s : nameStickers) {
                name = Card.addStickerWord(name, s.getSticker().getWord(), s.getNamePosition());
            }
            if (!ptStickers.isEmpty()) {
                AppliedSticker ptSticker = ptStickers.get(ptStickers.size() - 1);
                power = "" + ptSticker.getSticker().getPower();
                toughness = "" + ptSticker.getSticker().getToughness();
            }
        }
        sb.append(name).append(" - ").append(Localizer.getInstance().getMessage("lblCreature")).append(" ");
        sb.append(power);
        sb.append(" / ").append(toughness);
        return sb.toString();
    }
}
