package forge.game.zone;

import forge.game.player.PlayerView;

import java.io.Serializable;

/**
 * A serializable snapshot of a zone's owner and type, used by game events
 * to track zone transitions without referencing engine objects.
 */
public record ZoneView(PlayerView player, ZoneType zoneType) implements Serializable {}
