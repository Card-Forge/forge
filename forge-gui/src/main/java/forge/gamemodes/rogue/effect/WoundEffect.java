package forge.gamemodes.rogue.effect;

import forge.game.player.RegisteredPlayer;
import forge.gamemodes.rogue.RogueRun;

public enum WoundEffect implements RogueEffect {

    ARM("arm", "Arm", "Whenever you cast your second spell each turn, this Trait deals 3 damage to you.",
            EffectType.PERMANENT, "Wound - Arm") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    LEFT_LEG("left_leg", "Left Leg",
            "Spells you cast from the command zone cost {1} more to cast.",
            EffectType.PERMANENT, "Wound - Left Leg") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    RIGHT_LEG("right_leg", "Right Leg",
            "Artifacts and creatures you control enter tapped.",
            EffectType.PERMANENT, "Wound - Right Leg") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    HEAD("head", "Head", "Start each match with 1 less card in hand.",
            EffectType.PERMANENT, "Wound - Head") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            human.setStartingHand(human.getStartingHand() - 1);
            addEffectCardToCommandZone(human);
        }
    },
    EYE("eye", "Eye", "Planes on the path are not revealed when you reach them.",
            EffectType.PERMANENT, "Wound - Eye") {
        @Override
        public void onPathUpdate(PathUpdateContext ctx, RogueRun run) {
            ctx.hidePlanes = true;
        }
    };

    private final String id;
    private final String displayName;
    private final String description;
    private final EffectType effectType;
    private final String effectCardReference;

    WoundEffect(String id, String displayName, String description, EffectType effectType,
                String effectCardReference) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.effectType = effectType;
        this.effectCardReference = effectCardReference;
    }

    @Override
    public EffectType getEffectType() { return effectType; }

    @Override
    public String getId() { return id; }

    @Override
    public String getDisplayName() { return displayName; }

    @Override
    public String getRawDescription() { return description; }

    @Override
    public String getEffectCardReference() { return effectCardReference; }

    public static WoundEffect fromId(String id) {
        for (WoundEffect w : values())
            if (w.id.equals(id)) return w;
        return null;
    }
}
