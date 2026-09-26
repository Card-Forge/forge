package forge.gamemodes.limited;

import forge.item.PaperCard;
import forge.util.Localizer;

import java.io.Serializable;

/** A draft ability the engine currently offers a seat. */
public record DraftAction(Kind kind, PaperCard source, PaperCard target) implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Kind { PICK, CHOOSE, POOL }

    public static DraftAction pick(PaperCard source, PaperCard target) {
        return new DraftAction(Kind.PICK, source, target);
    }

    /** Drafts the card and asks which of its pick variants to use together. */
    public static DraftAction choose(PaperCard target) {
        return new DraftAction(Kind.CHOOSE, null, target);
    }

    public static DraftAction pool(PaperCard source) {
        return new DraftAction(Kind.POOL, source, null);
    }

    public boolean isPickFor(PaperCard card) {
        return kind != Kind.POOL && target.equals(card);
    }

    // A pool copy can be another printing of the source, so pool actions match by name
    public boolean isPoolActionFor(PaperCard card) {
        return kind == Kind.POOL && source.getName().equals(card.getName());
    }

    public String label() {
        return switch (kind) {
            case PICK -> Localizer.getInstance().getMessage("lblDraftWithSource", source.getDisplayName());
            case CHOOSE -> Localizer.getInstance().getMessage("lblDraftWithAbilities");
            case POOL -> Localizer.getInstance().getMessage("lblActivateSource", source.getDisplayName());
        };
    }

    public String tooltip() {
        if (kind == Kind.CHOOSE) {
            return Localizer.getInstance().getMessage("lblDraftWithAbilitiesTooltip");
        }
        return String.join(" ", source.getRules().getMainPart().getDraftActions())
                .replace("CARDNAME", source.getName());
    }
}
