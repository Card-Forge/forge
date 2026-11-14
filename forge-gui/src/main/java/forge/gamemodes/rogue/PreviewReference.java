package forge.gamemodes.rogue;

/**
 * Parsed preview reference extracted from Rogue description text.
 */
public record PreviewReference(PreviewReferenceType type, String token, int order) {
}
