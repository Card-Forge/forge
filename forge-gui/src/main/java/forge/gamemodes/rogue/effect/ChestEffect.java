package forge.gamemodes.rogue.effect;

import forge.game.player.RegisteredPlayer;
import forge.gamemodes.rogue.RogueMetaProgress;
import forge.gamemodes.rogue.RogueRun;
import forge.item.PaperCard;
import forge.util.Localizer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public enum ChestEffect implements RogueEffect {

    // ONESHOT effects
    CARD_CACHE("card_cache", "Card Cache", "Gain a {{Card Reward}}.", EffectType.ONESHOT,
        null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            ctx.trigger = EffectResultContext.ActionTriggerType.CARD_REWARD;
        }
    },
    CARD_CACHE_MYTHIC("card_cache_mythic", "Mythic Card Cache", "Gain a mythic {{Card Reward}}.",
        EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            ctx.trigger = EffectResultContext.ActionTriggerType.MYTHIC_CARD_REWARD;
        }
    },
    CARD_CACHE_GAMECHANGER("card_cache_gamechanger", "Gamechanging Card Cache",
        "Gain a {{Card Reward}} from the Gamechanger list.", EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            List<PaperCard> gamechangerCards = run.getGamechangerCardsForActiveCommander();
            if (gamechangerCards.isEmpty()) {
                return;
            }
            triggerCustomCardReward(ctx, Localizer.getInstance().getMessage("lblRogueChooseRewards"),
                gamechangerCards, 7, 3);
        }
    },
    CARD_CACHE_BAN("card_cache_ban", "Forbidden Card Cache",
        "Gain a {{Card Reward}} from the Commander Banlist.", EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            List<PaperCard> banlistCards = run.getBanlistCardsForActiveCommander();
            if (banlistCards.isEmpty()) {
                return;
            }
            triggerCustomCardReward(ctx, Localizer.getInstance().getMessage("lblRogueChooseRewards"),
                banlistCards, 7, 3);
        }
    },
    POTION_OF_VITALITY("potion_of_vitality", "Potion Of Vitality", "Gain 10 {{Max. Life}}.",
        EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            run.addMaxLife(10);
        }
    },
    POTION_OF_MERCY("potion_of_mercy", "Potion Of Mercy",
        "Your life total becomes equal to your Max. Life total.",
        EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            run.gainLifeUpToMax(run.getMaxLife());
        }
    },
    TREASURE("treasure", "Treasure", "Gain 10 {{Gold}} and 10 {{Echoes}}.", EffectType.ONESHOT,
        null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            run.addGold(10);
            RogueMetaProgress.getInstance().addEchoes(10);
        }
    },

    // PERMANENT / CONSUME effects (Traits)
    BANNER_OF_CHAMPIONS("banner_of_champions", "Banner Of Champions",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Banner Of Champions") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    BANNER_OF_CHRYSALIS("banner_of_chrysalis", "Banner Of Chrysalis",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Banner Of Chrysalis") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    BANNER_OF_FERVOR("banner_of_fervor", "Banner Of Fervor",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Banner Of Fervor") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    BANNER_OF_RITES("banner_of_rites", "Banner Of Rites",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Banner Of Rites") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    BANNER_OF_STENSIA("banner_of_stensia", "Banner Of Stensia",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Banner Of Stensia") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    BANNER_OF_WAR("banner_of_war", "Banner Of War",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Banner Of War") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    CHARM_OF_ASCENDANCY("charm_of_ascendancy", "Charm Of Ascendancy",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Charm Of Ascendancy") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    CHARM_OF_HIGH_SCORE("charm_of_high_score", "Charm Of High Score",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Charm Of High Score") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    CHARM_OF_THE_GOD_PHARAOH("charm_of_the_god_pharaoh", "Charm Of The God-Pharaoh",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Charm Of The God-Pharaoh") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    CHARM_OF_FAE("charm_of_fae", "Charm Of Fae",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Charm Of Fae") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    CHARM_OF_THE_DEAD("charm_of_the_dead", "Charm Of The Dead",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Charm Of The Dead") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    CHARM_OF_THE_TRAVELER("charm_of_the_traveler", "Charm Of The Traveler",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Charm Of The Traveler") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    CHARM_OF_THE_WASTELAND("charm_of_the_wasteland", "Charm Of The Wasteland",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Charm Of The Wasteland") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    CHARM_OF_WILDERNESS("charm_of_wilderness", "Charm Of Wilderness",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Charm Of Wilderness") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    IDOL_OF_DARKNESS("idol_of_darkness", "Idol Of Darkness",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Idol Of Darkness") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    IDOL_OF_BROODHEARTS("idol_of_broodhearts", "Idol Of Broodhearts",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Idol Of Broodhearts") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    IDOL_OF_DREAD("idol_of_dread", "Idol Of Dread",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Idol Of Dread") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    IDOL_OF_EMBERS("idol_of_embers", "Idol Of Embers",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Idol Of Embers") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    IDOL_OF_LUCK("idol_of_luck", "Idol Of Luck",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Idol Of Luck") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    IDOL_OF_IVORY("idol_of_ivory", "Idol Of Ivory",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Idol Of Ivory") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    IDOL_OF_MANY_EYES("idol_of_many_eyes", "Idol Of Many Eyes",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Idol Of Many Eyes") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    IDOL_OF_THE_RINGCALLER("idol_of_the_ringcaller", "Idol Of The Ringcaller",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Idol Of The Ringcaller") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    OMEN_OF_BURGEONING("omen_of_burgeoning", "Omen Of Burgeoning",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Omen Of Burgeoning") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    OMEN_OF_COUNTERBALANCE("omen_of_counterbalance", "Omen Of Counterbalance",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Omen Of Counterbalance") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    OMEN_OF_DISSIPATION("omen_of_dissipation", "Omen Of Dissipation",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Omen Of Dissipation") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    OMEN_OF_NIGHTMARES("omen_of_nightmares", "Omen Of Nightmares",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Omen Of Nightmares") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    OMEN_OF_POWERBALANCE("omen_of_powerbalance", "Omen Of Powerbalance",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Omen Of Powerbalance") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    OMEN_OF_SPLENDOR("omen_of_splendor", "Omen Of Splendor",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Omen Of Splendor") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    RELIC_OF_AGILITY("relic_of_agility", "Relic Of Agility",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Relic Of Agility") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    RELIC_OF_ARTS("relic_of_arts", "Relic Of Arts",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Relic Of Arts") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    RELIC_OF_CHARISMA("relic_of_charisma", "Relic Of Charisma",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Relic Of Charisma") {
        @Override
        public void onBeforeBazaar(BazaarContext ctx, RogueRun run) {
            ctx.priceAdjustment -= 2;
        }
    },
    RELIC_OF_ENDLESS_WATER("relic_of_endless_water", "Relic Of Endless Water",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Relic Of Endless Water") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    RELIC_OF_EXPLORATION("relic_of_exploration", "Relic Of Exploration",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Relic Of Exploration") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    RELIC_OF_ICHOR("relic_of_ichor", "Relic Of Ichor",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Relic Of Ichor") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    RELIC_OF_INSIGHT("relic_of_insight", "Relic Of Insight",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Relic Of Insight") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    RELIC_OF_MONUMENTS("relic_of_monuments", "Relic Of Monuments",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Relic Of Monuments") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    RELIC_OF_OBEDIENCE("relic_of_obedience", "Relic Of Obedience",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Relic Of Obedience") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    RELIC_OF_PATHFINDING("relic_of_pathfinding", "Relic Of Pathfinding",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Relic Of Pathfinding") {
        @Override
        public void onPathUpdate(PathUpdateContext ctx, RogueRun run) {
            ctx.allowAllNodesInCurrentRow = true;
        }
    },
    RELIC_OF_PRISMS("relic_of_prisms", "Relic Of Prisms",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Relic Of Prisms") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    RELIC_OF_REGENERATION("relic_of_regeneration", "Relic Of Regeneration",
        TRAIT_GAIN_DESCRIPTION + " !{{Max. Life}}",
        EffectType.PERMANENT, "Chest Trait - Relic Of Regeneration") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }

        @Override
        public void onMatchWin(RogueRun run) {
            run.gainLifeUpToMax(5);
        }
    },
    RELIC_OF_STRENGTH("relic_of_strength", "Relic Of Strength",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Relic Of Strength") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    RELIC_OF_TELEPATHY("relic_of_telepathy", "Relic Of Telepathy",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Relic Of Telepathy") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    RELIC_OF_VIGOR("relic_of_vigor", "Relic Of Vigor",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Relic Of Vigor") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    RELIC_OF_WEALTH("relic_of_wealth", "Relic Of Wealth",
        "Gain the {{Trait}} %s. !{{Gold}}",
        EffectType.PERMANENT, "Chest Trait - Relic Of Wealth") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }

        @Override
        public void onBeforeRewards(MatchRewardContext ctx, RogueRun run) {
            ctx.goldRewardAdjustment += 3;
        }
    },
    RELIC_OF_WISDOM("relic_of_wisdom", "Relic Of Wisdom",
            TRAIT_GAIN_DESCRIPTION,
            EffectType.PERMANENT, "Chest Trait - Relic Of Wisdom") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            human.setStartingHand(human.getStartingHand() + 1);
            addEffectCardToCommandZone(human);
        }
    },
    SHIELD_OF_CARE("shield_of_care", "Shield Of Care",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Shield Of Care") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    SHIELD_OF_CONVALESCENCE("shield_of_convalescence", "Shield Of Convalescence",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Shield Of Convalescence") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    SHIELD_OF_FAITH("shield_of_faith", "Shield Of Faith",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Shield Of Faith") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    SHIELD_OF_GHOSTS("shield_of_ghosts", "Shield Of Ghosts",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Shield Of Ghosts") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    SHIELD_OF_MACHINATION("shield_of_machination", "Shield Of Machination",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Shield Of Machination") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    SHIELD_OF_THE_GODS("shield_of_the_gods", "Shield Of The Gods",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Shield Of The Gods") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    SIGIL_OF_ABZAN("sigil_of_abzan", "Sigil Of Abzan",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Sigil Of Abzan") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    SIGIL_OF_BRIGHTHEARTH("sigil_of_brighthearth", "Sigil Of Brighthearth",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Sigil Of Brighthearth") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    SIGIL_OF_JESKAI("sigil_of_jeskai", "Sigil Of Jeskai",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Sigil Of Jeskai") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    SIGIL_OF_MULTICOLOR("sigil_of_multicolor", "Sigil Of Multicolor",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Sigil Of Multicolor") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    SIGIL_OF_THE_BEANSTALK("sigil_of_the_beanstalk", "Sigil Of The Beanstalk",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Sigil Of The Beanstalk") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    SIGIL_OF_THE_FLAME("sigil_of_the_flame", "Sigil Of The Flame",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Sigil Of The Flame") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    SIGIL_OF_THE_GOBLIN("sigil_of_the_goblin", "Sigil Of The Goblin",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Sigil Of The Goblin") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    SIGIL_OF_THE_KINGDOM("sigil_of_the_kingdom", "Sigil Of The Kingdom",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Sigil Of The Kingdom") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    SIGIL_OF_THE_MILITARY("sigil_of_the_military", "Sigil Of The Military",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Sigil Of The Military") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    SIGIL_OF_THE_MOLTEN("sigil_of_the_molten", "Sigil Of The Molten",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Sigil Of The Molten") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    SIGIL_OF_THE_VEIL("sigil_of_the_veil", "Sigil Of The Veil",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Sigil Of The Veil") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    SIGIL_OF_WELCOME("sigil_of_welcome", "Sigil Of Welcome",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Chest Trait - Sigil Of Welcome") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    };

    private final String id;
    private final String displayName;
    private final String description;
    private final EffectType effectType;
    private final String effectCardReference;

    ChestEffect(String id, String displayName, String description, EffectType effectType,
                String effectCardReference) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.effectType = effectType;
        this.effectCardReference = effectCardReference;
    }

    @Override
    public void applyEffect(RogueRun run, EffectResultContext ctx) { /* Override in ONESHOT constants to apply immediate chest effects. */ }

    @Override
    public EffectType getEffectType() { return effectType; }

    @Override
    public String getId() { return id; }

    @Override
    public String getDisplayName() { return displayName; }

    @Override
    public String getRawDescription() { return formatEffectCardDescription(description); }

    @Override
    public String getEffectCardReference() { return effectCardReference; }

    public static List<ChestEffect> getAvailableEffects(RogueRun run, EffectType effectType) {
        List<String> activeChestEffectIds = run.getActiveChestEffects().stream()
            .map(RogueEffect::getId)
            .collect(Collectors.toList());

        List<ChestEffect> effects = new ArrayList<>();
        for (ChestEffect chestEffect : ChestEffect.values()) {
            if ((effectType == null || chestEffect.getEffectType() == effectType)
                && !activeChestEffectIds.contains(chestEffect.getId())) {
                effects.add(chestEffect);
            }
        }
        return effects;
    }

    public static ChestEffect fromId(String id) {
        for (ChestEffect cl : values())
            if (cl.id.equals(id)) return cl;
        return null;
    }
}
