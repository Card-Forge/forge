package forge.gamemodes.rogue.effect;

import forge.game.player.RegisteredPlayer;
import forge.gamemodes.rogue.RogueRun;
import java.util.Set;

public enum CursedEffect implements PlaneboundEffect {

    ASCETICISM("cursed_asceticism", "Asceticism",
        "Creatures Planebound controls have hexproof.",
        "Cursed - Asceticism") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
            run.consumeEffect(getId());
        }
    },
    ASSAULT("cursed_assault", "Assault",
        "Creatures Planebound controls get +2/+2 and have deathtouch.",
        "Cursed - Assault") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
            run.consumeEffect(getId());
        }
    },
    BARGAIN("cursed_bargain", "Bargain",
            "Planebound's spells cost {2} less to cast.",
        "Cursed - Bargain") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
            run.consumeEffect(getId());
        }
    },
    BERSERKER("cursed_berserker", "Berserker",
        "Attacking creatures Planebound controls have double strike.",
        "Cursed - Berserker") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
            run.consumeEffect(getId());
        }
    },
    CRUSADER("cursed_crusader", "Crusader",
        "Whenever a creature Planebound controls enters, put a +1/+1 counter on each creature Planebound controls.",
        "Cursed - Crusader") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
            run.consumeEffect(getId());
        }
    },
    DEMONIC("cursed_demonic", "Demonic",
        "At the beginning of Planebound's end step, if Planebound controls exactly one creature, they create a 5/5 black Demon creature token with flying.",
        "Cursed - Demonic") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
            run.consumeEffect(getId());
        }
    },
    DICTATOR("cursed_dictator", "Dictator",
        "Whenever a creature Planebound controls dies, you sacrifice a creature.",
        "Cursed - Dictator") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
            run.consumeEffect(getId());
        }
    },
    ETERNAL("cursed_bloodthirsty", "Bloodthirsty",
        "Planebound's creatures get +2/+2 and have indestructible.",
        "Cursed - Eternal") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
            run.consumeEffect(getId());
        }
    },
    GRACEFUL("cursed_graceful", "Graceful",
        "At the beginning of Planebound's upkeep, they create a 1/1 white Spirit creature token with flying.",
        "Cursed - Graceful") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
            run.consumeEffect(getId());
        }
    },
    INSIGHT("cursed_insight", "Insight",
            "Planebound draws 1 additional card at the start of each match.",
        "Cursed - Insight") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            if (opponent == null) return;
            opponent.setStartingHand(opponent.getStartingHand() + 1);
            addEffectCardToCommandZone(human);
            run.consumeEffect(getId());
        }
    },
    OVERLORD("cursed_overlord", "Overlord",
        "At the beginning of Planebound's upkeep, they may create a token that's a copy of up to one target nonland permanent they control.",
        "Cursed - Overlord") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
            run.consumeEffect(getId());
        }
    },
    RAMP("cursed_ramp", "Ramp",
        "Planebound starts the match with a random land from their deck on the battlefield.",
        null) {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            RogueEffect.moveCardsFromDeckToBattlefield(c -> c.getRules().getType().isLand(),
                1, opponent);
            run.consumeEffect(getId());
        }
    },
    SUMMON("cursed_summon", "Summon",
            "Planebound starts the match with a random nonland permanent card with mana value 3 or less from their deck on the battlefield.",
        null) {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            RogueEffect.moveCardsFromDeckToBattlefield(c ->
                c.getRules().getType().isPermanent() && !c.getRules().getType().isLand()
                    && c.getRules().getManaCost().getCMC() <= 3,
                1, opponent);
            run.consumeEffect(getId());
        }
    };

    private final String id;
    private final String displayName;
    private final String description;
    private final String effectCardReference;

    CursedEffect(String id, String displayName, String description, String effectCardReference) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.effectCardReference = effectCardReference;
    }

    @Override
    public PlaneboundEffectCategory getCategory() { return PlaneboundEffectCategory.CURSED; }

    @Override
    public String getId() { return id; }

    @Override
    public String getDisplayName() { return displayName; }

    @Override
    public String getRawDescription() { return description; }

    @Override
    public String getEffectCardReference() { return effectCardReference; }

    public static CursedEffect getRandomExcluding(Set<CursedEffect> exclude) {
        return PlaneboundEffect.getRandomExcluding(values(), exclude);
    }

    public static CursedEffect fromId(String id) {
        for (CursedEffect c : values())
            if (c.id.equals(id)) return c;
        return null;
    }
}
