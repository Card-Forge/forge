package forge.gamemodes.rogue.effect;

import forge.game.player.RegisteredPlayer;
import forge.gamemodes.rogue.RogueRun;
import java.util.Set;

public enum WrathfulEffect implements PlaneboundEffect {

    AGGRESSION("wrathful_aggression", "Aggression",
        "Whenever one or more creatures Planebound controls deal combat damage to you, Planebound draws a card.",
        "Wrathful - Aggression") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
            run.consumeEffect(getId());
        }
    },
    EFFICIENCY("wrathful_efficiency", "Efficiency",
        "Planebound Commander costs {1} less to cast.",
        "Wrathful - Efficiency") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
            run.consumeEffect(getId());
        }
    },
    FEASTING("wrathful_feasting", "Feasting",
        "Whenever a creature Planebound controls enters, Planebound gains 1 life.",
        "Wrathful - Feasting") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
            run.consumeEffect(getId());
        }
    },
    FERVOR("wrathful_fervor", "Fervor",
        "Creatures Planebound controls have haste.",
        "Wrathful - Fervor") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
            run.consumeEffect(getId());
        }
    },
    FORESIGHT("wrathful_foresight", "Foresight",
        "At the beginning of each of Planebound's upkeeps, they scry 1.",
        "Wrathful - Foresight") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
            run.consumeEffect(getId());
        }
    },
    KINGDOM("wrathful_kingdom", "Kingdom",
        "Landfall - Whenever a land Planebound controls enters, put a +1/+1 counter on target creature Planebound controls.",
        "Wrathful - Kingdom") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
            run.consumeEffect(getId());
        }
    },
    MACHINATION("wrathful_machination", "Machination",
        "At the beginning of combat on Planebound's turn, target creature Planebound controls gains indestructible until end of turn.",
        "Wrathful - Machination") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
            run.consumeEffect(getId());
        }
    },
    RESILIENCE("wrathful_resilience", "Resilience",
        "Planebound gains 1 life at the beginning of each of their upkeeps.",
        "Wrathful - Resilience") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
            run.consumeEffect(getId());
        }
    },
    STRENGTH("wrathful_strength", "Strength",
        "Planebound Commander gets +2/+2 and has trample.",
        "Wrathful - Strength") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
            run.consumeEffect(getId());
        }
    },
    TYRANNY("wrathful_tyranny", "Tyranny",
        "Planebound may play an additional land on each of their turns.",
        "Wrathful - Tyranny") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
            run.consumeEffect(getId());
        }
    },
    VIGOR("wrathful_vigor", "Vigor",
        "Whenever a permanent Planebound controls enters tapped, untap it.",
        "Wrathful - Vigor") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
            run.consumeEffect(getId());
        }
    },
    WATCHING("wrathful_watching", "Watching",
        "Creatures Planebound controls get +1/+1 and have vigilance.",
        "Wrathful - Watching") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
            run.consumeEffect(getId());
        }
    },
    WILDNESS("wrathful_wildness", "Wildness",
        "At the beginning of Planebound's end step, untap all lands they control.",
        "Wrathful - Wildness") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
            run.consumeEffect(getId());
        }
    };

    private final String id;
    private final String displayName;
    private final String description;
    private final String effectCardReference;

    WrathfulEffect(String id, String displayName, String description, String effectCardReference) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.effectCardReference = effectCardReference;
    }

    @Override
    public PlaneboundEffectCategory getCategory() { return PlaneboundEffectCategory.WRATHFUL; }

    @Override
    public String getId() { return id; }

    @Override
    public String getDisplayName() { return displayName; }

    @Override
    public String getRawDescription() { return description; }

    @Override
    public String getEffectCardReference() { return effectCardReference; }

    public static WrathfulEffect getRandomExcluding(Set<WrathfulEffect> exclude) {
        return PlaneboundEffect.getRandomExcluding(values(), exclude);
    }

    public static WrathfulEffect fromId(String id) {
        for (WrathfulEffect w : values())
            if (w.id.equals(id)) return w;
        return null;
    }
}
