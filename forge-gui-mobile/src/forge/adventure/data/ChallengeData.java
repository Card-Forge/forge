package forge.adventure.data;

/**
 * An optional rule a player can turn on for a whole run when starting a new game. A challenge adds an effect to
 * every duel, like the plane's global effect.
 */
public class ChallengeData {
    public String name;
    public String description;
    public String icon;            //Name of an item icon shown beside the challenge, such as "BlackRune".
    public EffectData effect;      //Added to every duel, written for the enemies like the plane's global effect.
}
