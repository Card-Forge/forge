package forge.gamemodes.rogue.effect;

import com.google.common.collect.ImmutableList;
import forge.StaticData;
import forge.card.CardDb;
import forge.card.CardEdition;
import forge.card.CardRarity;
import forge.card.CardRules;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.rogue.RogueRun;
import forge.gamemodes.rogue.path.NodePlanebound;
import forge.gamemodes.rogue.path.RoguePathNode;
import forge.item.PaperCard;
import java.util.function.Consumer;

/** Difficulty and Bazaar pricing for each Trail continuation. */
public enum EndlessMode implements RogueEffect {
    INSTANCE;

    public static int getLifeBonus(int level) {
        return 5 * level;
    }

    public static int getHandBonus(int level) {
        return level;
    }

    public static int getCostReduction(int level) {
        return level;
    }

    public static int getDamageBonus(int level) {
        return level;
    }

    public static int getBazaarPriceBonus(int level) {
        return level;
    }

    public static String getModifiersDescription(int level) {
        return "<html>Opponents start with +" + getLifeBonus(level) + " Life.<br>"
            + "They get up to +" + getHandBonus(level) + " opening cards and maximum hand size"
            + " (capped at 30% of their main deck size).<br>"
            + "Their spells cost {" + getCostReduction(level) + "} less.<br>"
            + "Their sources deal +" + getDamageBonus(level) + " damage to you.<br>"
            + "Whenever their spells or abilities make you lose life, you lose "
            + getDamageBonus(level) + " more life.<br>"
            + "Bazaar offers cost +" + getBazaarPriceBonus(level) + " Gold.</html>";
    }

    @Override
    public String getDisplayName() {
        return "Endless Mode";
    }

    @Override
    public String getRawDescription() {
        return "Opponent strength and Bazaar prices increase with each Endless Level.";
    }

    @Override
    public String getActiveDescription(RogueRun run) {
        return getModifiersDescription(run.getEndlessLevel());
    }

    @Override
    public void onRunStart(RogueRun run, Consumer<EffectResultContext> handleResult) {
        run.setCurrentLife(run.getMaxLife());
    }

    @Override
    public void afterPathGeneration(RogueRun run) {
        int bonus = getLifeBonus(run.getEndlessLevel());
        for (RoguePathNode node : run.getPath().getNodes()) {
            if (node instanceof NodePlanebound planebound) {
                planebound.setStartingLifeModification(
                    planebound.getStartingLifeModification() + bonus);
            }
        }
    }

    @Override
    public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
        int level = run.getEndlessLevel();
        int handBonus = Math.min(getHandBonus(level), opponent.getDeck().getMain().countAll() * 30 / 100);
        opponent.setStartingHand(opponent.getStartingHand() + handBonus);

        PaperCard card = createMatchCard(level);
        CardDb variants = StaticData.instance().getVariantCards();
        PaperCard registeredCard = variants.getCard(card.getName(), CardEdition.UNKNOWN_CODE);
        if (registeredCard == null) {
            variants.addCard(card);
        } else {
            card = registeredCard;
        }
        RogueEffect.addCardToCommandZone(card, human);
    }

    @Override
    public void onBeforeBazaar(BazaarContext ctx, RogueRun run) {
        ctx.priceAdjustment += getBazaarPriceBonus(run.getEndlessLevel());
    }

    static PaperCard createMatchCard(int level) {
        int handBonus = getHandBonus(level);
        int costReduction = getCostReduction(level);
        int damageBonus = getDamageBonus(level);
        String startingText = "Opponents start with " + getLifeBonus(level) + " additional life and up to "
            + handBonus + " additional " + (handBonus == 1 ? "card" : "cards")
            + " in their opening hand. Their maximum hand size is increased by the same amount."
            + " The Endless bonus cannot exceed 30% of their main deck size.";
        String costText = "Spells your opponents cast cost {" + costReduction + "} less to cast.";
        String damageAndLifeLossText = "If a source an opponent controls would deal damage to you, "
            + "it deals that much damage plus " + damageBonus + " instead; whenever a spell or ability "
            + "an opponent controls causes you to lose life, you lose " + damageBonus + " additional life.";
        CardRules rules = CardRules.fromScript(ImmutableList.of(
            "Name:Endless Mode - Level " + level,
            "ManaCost:no cost",
            "Types:Cursed",
            "Text:" + startingText + "\\r\\n\\r\\n" + costText + "\\r\\n\\r\\n"
                + damageAndLifeLossText,
            "S:Mode$ ReduceCost | ValidCard$ Card | Activator$ Player.Opponent | Type$ Spell | Amount$ "
                + costReduction + " | EffectZone$ Command | Secondary$ True | Description$ " + costText,
            "R:Event$ DamageDone | ActiveZones$ Command | ValidSource$ Card.OppCtrl,Emblem.OppCtrl"
                + " | ValidTarget$ You | ReplaceWith$ DmgPlus | Secondary$ True | Description$ "
                + damageAndLifeLossText,
            "T:Mode$ LifeLost | ValidPlayer$ You | ValidCause$ SpellAbility.OppCtrl"
                + " | LifeAmount$ GE1 | TriggerZones$ Command | Execute$ TrigLifeLoss | Secondary$ True"
                + " | TriggerDescription$ " + damageAndLifeLossText,
            "SVar:DmgPlus:DB$ ReplaceEffect | VarName$ DamageAmount | VarValue$ X",
            "SVar:TrigLifeLoss:DB$ LoseLife | Defined$ You | LifeAmount$ " + damageBonus,
            "SVar:X:ReplaceCount$DamageAmount/Plus." + damageBonus,
            "Oracle:" + startingText + "\\n" + costText + "\\n" + damageAndLifeLossText
        ));
        rules.setCustom();
        return new PaperCard(rules, CardEdition.UNKNOWN_CODE, CardRarity.Special);
    }
}
