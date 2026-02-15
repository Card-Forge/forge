package forge.ai.simulation;

import com.google.common.collect.Lists;
import forge.ai.ComputerUtilAbility;
import forge.card.CardStateName;
import forge.card.MagicColor;
import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CounterEnumType;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameSimulationTest extends SimulationTest {
    private Map<AbilityKey, Object> destroyParams(Game game) {
        Map<AbilityKey, Object> params = AbilityKey.newMap();
        params.put(AbilityKey.LastStateBattlefield, game.copyLastStateBattlefield());
        params.put(AbilityKey.LastStateGraveyard, game.copyLastStateGraveyard());
        return params;
    }

    @Test
    public void testActivateAbilityTriggers() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCards("Plains", 3, p);
        String heraldCardName = "Herald of Anafenza";
        Card herald = addCard(heraldCardName, p);
        herald.setSickness(false);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);

        SpellAbility outlastSA = findSAWithPrefix(herald, "Outlast");
        AssertJUnit.assertNotNull(outlastSA);

        GameSimulator sim = createSimulator(game, p);
        int score = sim.simulateSpellAbility(outlastSA).value;
        AssertJUnit.assertTrue(score > 0);
        Game simGame = sim.getSimulatedGameState();

        Card heraldCopy = findCardWithName(simGame, heraldCardName);
        AssertJUnit.assertNotNull(heraldCopy);
        AssertJUnit.assertTrue(heraldCopy.isTapped());
        AssertJUnit.assertTrue(heraldCopy.hasCounters());
        AssertJUnit.assertEquals(1, heraldCopy.getToughnessBonusFromCounters());
        AssertJUnit.assertEquals(1, heraldCopy.getPowerBonusFromCounters());

        Card warriorToken = findCardWithName(simGame, "Warrior Token");
        AssertJUnit.assertNotNull(warriorToken);
        AssertJUnit.assertTrue(warriorToken.isSick());
        AssertJUnit.assertEquals(1, warriorToken.getCurrentPower());
        AssertJUnit.assertEquals(1, warriorToken.getCurrentToughness());
    }

    @Test
    public void testStaticAbilities() {
        String sliverCardName = "Sidewinder Sliver";
        String heraldCardName = "Herald of Anafenza";
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card sliver = addCard(sliverCardName, p);
        sliver.setSickness(false);
        Card herald = addCard(heraldCardName, p);
        herald.setSickness(false);
        addCards("Plains", 3, p);
        addCard("Spear of Heliod", p);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertEquals(1, sliver.getAmountOfKeyword(Keyword.FLANKING));
        AssertJUnit.assertEquals(2, sliver.getNetPower());
        AssertJUnit.assertEquals(2, sliver.getNetToughness());

        SpellAbility outlastSA = findSAWithPrefix(herald, "Outlast");
        AssertJUnit.assertNotNull(outlastSA);

        GameSimulator sim = createSimulator(game, p);
        int score = sim.simulateSpellAbility(outlastSA).value;
        AssertJUnit.assertTrue(score > 0);
        Game simGame = sim.getSimulatedGameState();
        Card sliverCopy = findCardWithName(simGame, sliverCardName);
        AssertJUnit.assertEquals(1, sliverCopy.getAmountOfKeyword(Keyword.FLANKING));
        AssertJUnit.assertEquals(2, sliver.getNetPower());
        AssertJUnit.assertEquals(2, sliver.getNetToughness());
    }

    @Test
    public void testStaticEffectsMonstrous() {
        String lionCardName = "Fleecemane Lion";
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card lion = addCard(lionCardName, p);
        lion.setSickness(false);
        lion.setMonstrous(true);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(lion.isMonstrous());
        AssertJUnit.assertEquals(1, lion.getAmountOfKeyword(Keyword.HEXPROOF));
        AssertJUnit.assertEquals(1, lion.getAmountOfKeyword(Keyword.INDESTRUCTIBLE));

        GameSimulator sim = createSimulator(game, p);
        Game simGame = sim.getSimulatedGameState();
        Card lionCopy = findCardWithName(simGame, lionCardName);
        AssertJUnit.assertTrue(lionCopy.isMonstrous());
        AssertJUnit.assertEquals(1, lionCopy.getAmountOfKeyword(Keyword.HEXPROOF));
        AssertJUnit.assertEquals(1, lionCopy.getAmountOfKeyword(Keyword.INDESTRUCTIBLE));
    }

    @Test
    public void testEquippedAbilities() {
        String bearCardName = "Runeclaw Bear";
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card bear = addCard(bearCardName, p);
        bear.setSickness(false);
        Card cloak = addCard("Whispersilk Cloak", p);
        cloak.attachToEntity(bear, null);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(1, bear.getAmountOfKeyword(Keyword.SHROUD));

        GameSimulator sim = createSimulator(game, p);
        Game simGame = sim.getSimulatedGameState();
        Card bearCopy = findCardWithName(simGame, bearCardName);
        AssertJUnit.assertEquals(1, bearCopy.getAmountOfKeyword(Keyword.SHROUD));
    }

    @Test
    public void testEnchantedAbilities() {
        String bearCardName = "Runeclaw Bear";
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card bear = addCard(bearCardName, p);
        bear.setSickness(false);
        Card lifelink = addCard("Lifelink", p);
        lifelink.attachToEntity(bear, null);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(1, bear.getAmountOfKeyword(Keyword.LIFELINK));

        GameSimulator sim = createSimulator(game, p);
        Game simGame = sim.getSimulatedGameState();
        Card bearCopy = findCardWithName(simGame, bearCardName);
        AssertJUnit.assertEquals(1, bearCopy.getAmountOfKeyword(Keyword.LIFELINK));
    }

    @Test
    public void testEtbTriggers() {
        Game game = initAndCreateGame();
        Player p0 = game.getPlayers().get(0);
        Player p = game.getPlayers().get(1);
        addCard("Black Knight", p);
        addCards("Swamp", 5, p);

        String merchantCardName = "Gray Merchant of Asphodel";
        Card c = addCardToZone(merchantCardName, p, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);

        SpellAbility playMerchantSa = c.getSpellAbilities().get(0);
        playMerchantSa.setActivatingPlayer(p);

        GameSimulator sim = createSimulator(game, p);
        int origScore = sim.getScoreForOrigGame().value;
        int score = sim.simulateSpellAbility(playMerchantSa).value;
        AssertJUnit.assertTrue(String.format("score=%d vs. origScore=%d", score, origScore), score > origScore);
        Game simGame = sim.getSimulatedGameState();

        Player simP0 = simGame.getPlayer(p0.getId());
        Player simP = simGame.getPlayer(p.getId());
        AssertJUnit.assertEquals(24, simP.getLife());
        AssertJUnit.assertEquals(16, simP0.getLife());
    }

    @Test
    public void testSimulateUnmorph() {
        Game game = initAndCreateGame();
        Player p0 = game.getPlayers().get(0);
        Player p = game.getPlayers().get(1);
        Card ripper = createCard("Ruthless Ripper", p);
        ripper.turnFaceDownNoUpdate();
        p.getZone(ZoneType.Battlefield).add(ripper);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertEquals(20, game.getPlayers().get(0).getLife());

        GameSimulator sim = createSimulator(game, p);
        Game simGame = sim.getSimulatedGameState();

        SpellAbility unmorphSA = findSAWithPrefix(ripper, "Morph — Reveal a black card");
        AssertJUnit.assertNotNull(unmorphSA);
        sim.simulateSpellAbility(unmorphSA);

        Player simP0 = simGame.getPlayer(p0.getId());
        AssertJUnit.assertEquals(18, simP0.getLife());
    }

    @Test
    public void testFindingOwnCard() {
        Game game = initAndCreateGame();
        Player p0 = game.getPlayers().get(0);
        Player p1 = game.getPlayers().get(1);
        addCardToZone("Skull Fracture", p0, ZoneType.Hand);
        addCardToZone("Runeclaw Bear", p0, ZoneType.Hand);
        Card fractureP1 = addCardToZone("Skull Fracture", p1, ZoneType.Hand);
        addCard("Swamp", p1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p1);
        game.getAction().checkStateEffects(true);

        GameSimulator sim = createSimulator(game, p1);
        Game simGame = sim.getSimulatedGameState();

        SpellAbility fractureSa = fractureP1.getSpellAbilities().get(0);
        AssertJUnit.assertNotNull(fractureSa);
        fractureSa.getTargets().add(p0);
        sim.simulateSpellAbility(fractureSa);
        Player simP0 = simGame.getPlayer(p0.getId());
        Player simP1 = simGame.getPlayer(p1.getId());
        AssertJUnit.assertEquals(1, simP0.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals(0, simP1.getCardsIn(ZoneType.Hand).size());
    }

    @Test
    public void testPlaneswalkerAbilities() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card sorin = addCard("Sorin, Solemn Visitor", p);
        sorin.addCounterInternal(CounterEnumType.LOYALTY, 5, p, false, null, null);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);

        CardCollection cards = ComputerUtilAbility.getAvailableCards(game, p);
        List<SpellAbility> abilities = ComputerUtilAbility.getSpellAbilities(cards, p);
        SpellAbility minusTwo = findSAWithPrefix(abilities, "-2: Create a 2/2 black Vampire");
        AssertJUnit.assertNotNull(minusTwo);
        minusTwo.setActivatingPlayer(p);
        AssertJUnit.assertTrue(minusTwo.canPlay());

        GameSimulator sim = createSimulator(game, p);
        sim.simulateSpellAbility(minusTwo);
        Game simGame = sim.getSimulatedGameState();
        Card vampireToken = findCardWithName(simGame, "Vampire Token");
        AssertJUnit.assertNotNull(vampireToken);

        Player simP = simGame.getPlayer(p.getId());
        cards = ComputerUtilAbility.getAvailableCards(simGame, simP);
        abilities = ComputerUtilAbility.getSpellAbilities(cards, simP);
        SpellAbility minusTwoSim = findSAWithPrefix(abilities, "-2: Create a 2/2 black Vampire");
        AssertJUnit.assertNotNull(minusTwoSim);
        minusTwoSim.setActivatingPlayer(simP);
        AssertJUnit.assertFalse(minusTwoSim.canPlay());
        AssertJUnit.assertEquals(1, minusTwoSim.getActivationsThisTurn());

        GameCopier copier = new GameCopier(simGame);
        Game copy = copier.makeCopy();
        Player copyP = copy.getPlayer(p.getId());
        cards = ComputerUtilAbility.getAvailableCards(copy, copyP);
        abilities = ComputerUtilAbility.getSpellAbilities(cards, copyP);
        SpellAbility minusTwoCopy = findSAWithPrefix(abilities, "-2: Create a 2/2 black Vampire");
        minusTwoCopy.setActivatingPlayer(copyP);
        AssertJUnit.assertFalse(minusTwoCopy.canPlay());
        AssertJUnit.assertEquals(1, minusTwoCopy.getActivationsThisTurn());
    }

    @Test
    public void testPlaneswalkerEmblems() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        String bearCardName = "Runeclaw Bear";
        addCard(bearCardName, p);
        Card gideon = addCard("Gideon, Ally of Zendikar", p);
        gideon.addCounterInternal(CounterEnumType.LOYALTY, 4, p, false, null, null);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);

        CardCollection cards = ComputerUtilAbility.getAvailableCards(game, p);
        List<SpellAbility> abilities = ComputerUtilAbility.getSpellAbilities(cards, p);
        SpellAbility minusFour = findSAWithPrefix(abilities, "-4: You get an emblem");
        AssertJUnit.assertNotNull(minusFour);
        minusFour.setActivatingPlayer(p);
        AssertJUnit.assertTrue(minusFour.canPlay());

        GameSimulator sim = createSimulator(game, p);
        sim.simulateSpellAbility(minusFour);
        Game simGame = sim.getSimulatedGameState();
        Card simBear = findCardWithName(simGame, bearCardName);
        AssertJUnit.assertEquals(3, simBear.getNetPower());

        GameCopier copier = new GameCopier(simGame);
        Game copy = copier.makeCopy();
        Card copyBear = findCardWithName(copy, bearCardName);
        AssertJUnit.assertEquals(3, copyBear.getNetPower());
    }

    @Test
    public void testManifest() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCard("Plains", p);
        addCard("Plains", p);
        Card soulSummons = addCardToZone("Soul Summons", p, ZoneType.Hand);
        addCardToZone("Ornithopter", p, ZoneType.Library);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);

        SpellAbility manifestSA = soulSummons.getSpellAbilities().get(0);

        GameSimulator sim = createSimulator(game, p);
        sim.simulateSpellAbility(manifestSA);
        Game simGame = sim.getSimulatedGameState();
        Card manifestedCreature = findCardWithName(simGame, "");
        AssertJUnit.assertNotNull(manifestedCreature);

        SpellAbility unmanifestSA = findSAWithPrefix(manifestedCreature.getAllPossibleAbilities(p, false),
                "Unmanifest");
        AssertJUnit.assertNotNull(unmanifestSA);
        AssertJUnit.assertEquals(2, manifestedCreature.getNetPower());
        AssertJUnit.assertFalse(manifestedCreature.hasKeyword(Keyword.FLYING));

        GameSimulator sim2 = createSimulator(simGame, simGame.getPlayers().get(1));
        Game simGame2 = sim2.getSimulatedGameState();
        manifestedCreature = findCardWithName(simGame2, "");
        unmanifestSA = findSAWithPrefix(manifestedCreature.getAllPossibleAbilities(simGame2.getPlayers().get(1), false),
                "Unmanifest");

        sim2.simulateSpellAbility(unmanifestSA);

        Card ornithopter = findCardWithName(simGame2, "Ornithopter");
        AssertJUnit.assertEquals(0, ornithopter.getNetPower());
        AssertJUnit.assertTrue(ornithopter.hasKeyword(Keyword.FLYING));
        AssertJUnit.assertNull(findSAWithPrefix(ornithopter, "Unmanifest"));

        GameCopier copier = new GameCopier(simGame2);
        Game copy = copier.makeCopy();
        Card ornithopterCopy = findCardWithName(copy, "Ornithopter");
        AssertJUnit.assertNull(findSAWithPrefix(ornithopterCopy, "Unmanifest"));
    }

    @Test
    public void testManifest2() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCard("Plains", p);
        addCard("Plains", p);
        Card soulSummons = addCardToZone("Soul Summons", p, ZoneType.Hand);
        addCardToZone("Plains", p, ZoneType.Library);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);

        SpellAbility manifestSA = soulSummons.getSpellAbilities().get(0);

        GameSimulator sim = createSimulator(game, p);
        sim.simulateSpellAbility(manifestSA);
        Game simGame = sim.getSimulatedGameState();
        Card manifestedCreature = findCardWithName(simGame, "");
        AssertJUnit.assertNotNull(manifestedCreature);
        AssertJUnit.assertNull(findSAWithPrefix(manifestedCreature, "Unmanifest"));

        GameCopier copier = new GameCopier(simGame);
        Game copy = copier.makeCopy();
        Card manifestedCreatureCopy = findCardWithName(copy, "");
        AssertJUnit.assertNull(findSAWithPrefix(manifestedCreatureCopy, "Unmanifest"));
    }

    @Test
    public void testManifest3() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCard("Plains", p);
        addCard("Plains", p);
        Card soulSummons = addCardToZone("Soul Summons", p, ZoneType.Hand);
        addCardToZone("Dryad Arbor", p, ZoneType.Library);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);

        SpellAbility manifestSA = soulSummons.getSpellAbilities().get(0);

        GameSimulator sim = createSimulator(game, p);
        sim.simulateSpellAbility(manifestSA);
        Game simGame = sim.getSimulatedGameState();
        Card manifestedCreature = findCardWithName(simGame, "");
        AssertJUnit.assertNotNull(manifestedCreature);
        AssertJUnit.assertNull(findSAWithPrefix(manifestedCreature, "Unmanifest"));

        GameCopier copier = new GameCopier(simGame);
        Game copy = copier.makeCopy();
        Card manifestedCreatureCopy = findCardWithName(copy, "");
        AssertJUnit.assertNull(findSAWithPrefix(manifestedCreatureCopy, "Unmanifest"));
    }

    @Test
    public void testTypeOfPermanentChanging() {
        String sarkhanCardName = "Sarkhan, the Dragonspeaker";
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card sarkhan = addCard(sarkhanCardName, p);
        sarkhan.addCounterInternal(CounterEnumType.LOYALTY, 4, p, false, null, null);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertFalse(sarkhan.isCreature());
        AssertJUnit.assertTrue(sarkhan.isPlaneswalker());

        SpellAbility becomeDragonSA = findSAWithPrefix(sarkhan, "+1");
        AssertJUnit.assertNotNull(becomeDragonSA);

        GameSimulator sim = createSimulator(game, p);
        sim.simulateSpellAbility(becomeDragonSA);
        Game simGame = sim.getSimulatedGameState();
        Card sarkhanSim = findCardWithName(simGame, sarkhanCardName);
        AssertJUnit.assertTrue(sarkhanSim.isCreature());
        AssertJUnit.assertFalse(sarkhanSim.isPlaneswalker());

        GameCopier copier = new GameCopier(simGame);
        Game copy = copier.makeCopy();
        Card sarkhanCopy = findCardWithName(copy, sarkhanCardName);
        AssertJUnit.assertTrue(sarkhanCopy.isCreature());
        AssertJUnit.assertFalse(sarkhanCopy.isPlaneswalker());
    }

    @Test
    public void testDistributeCountersAbility() {
        String ajaniCardName = "Ajani, Mentor of Heroes";
        String ornithoperCardName = "Ornithopter";
        String bearCardName = "Runeclaw Bear";

        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCard(ornithoperCardName, p);
        addCard(bearCardName, p);
        Card ajani = addCard(ajaniCardName, p);
        ajani.addCounterInternal(CounterEnumType.LOYALTY, 4, p, false, null, null);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = findSAWithPrefix(ajani, "+1: Distribute");
        AssertJUnit.assertNotNull(sa);
        sa.setActivatingPlayer(p);

        MultiTargetSelector selector = new MultiTargetSelector(sa, null);
        while (selector.selectNextTargets()) {
            GameSimulator sim = createSimulator(game, p);
            sim.simulateSpellAbility(sa);
            Game simGame = sim.getSimulatedGameState();
            Card thopterSim = findCardWithName(simGame, ornithoperCardName);
            Card bearSim = findCardWithName(simGame, bearCardName);
            AssertJUnit.assertEquals(3,
                    thopterSim.getCounters(CounterEnumType.P1P1) + bearSim.getCounters(CounterEnumType.P1P1));
        }
    }

    @Test
    public void testDamagePreventedTrigger() {
        String ajaniCardName = "Ajani Steadfast";
        String selflessCardName = "Selfless Squire";

        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        addCard(selflessCardName, p);
        addCard("Mountain", p);
        Card boltCard = addCardToZone("Lightning Bolt", p, ZoneType.Hand);
        SpellAbility boltSA = boltCard.getFirstSpellAbility();

        Card ajani = addCard(ajaniCardName, p);
        ajani.addCounterInternal(CounterEnumType.LOYALTY, 8, p, false, null, null);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = findSAWithPrefix(ajani, "-7:");
        AssertJUnit.assertNotNull(sa);
        sa.setActivatingPlayer(p);

        GameSimulator sim = createSimulator(game, p);
        boltSA.getTargets().add(p);
        sim.simulateSpellAbility(sa);
        sim.simulateSpellAbility(boltSA);
        Game simGame = sim.getSimulatedGameState();
        Card simSelfless = findCardWithName(simGame, selflessCardName);

        // only one damage
        AssertJUnit.assertEquals(19, simGame.getPlayers().get(0).getLife());

        // only triggered once
        AssertJUnit.assertTrue(simSelfless.hasCounters());
        AssertJUnit.assertEquals(2, simSelfless.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals(2, simSelfless.getToughnessBonusFromCounters());
        AssertJUnit.assertEquals(2, simSelfless.getPowerBonusFromCounters());
    }

    @Test
    public void testChosenColors() {
        String bearCardName = "Runeclaw Bear";

        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card bear = addCard(bearCardName, p);
        Card hall = addCard("Hall of Triumph", p);
        hall.setChosenColors(Lists.newArrayList("green"));
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(3, bear.getNetToughness());

        GameCopier copier = new GameCopier(game);
        Game copy = copier.makeCopy();
        Card bearCopy = findCardWithName(copy, bearCardName);
        AssertJUnit.assertEquals(3, bearCopy.getNetToughness());
    }

    @Test
    public void testDarkDepthsCopy() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCard("Swamp", p);
        addCard("Swamp", p);
        Card depths = addCard("Dark Depths", p);
        depths.addCounterInternal(CounterEnumType.ICE, 10, p, false, null, null);
        Card thespian = addCard("Thespian's Stage", p);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(depths.hasCounters());

        SpellAbility sa = findSAWithPrefix(thespian,
                "{2}, {T}: CARDNAME becomes a copy of target land, except it has this ability.");
        AssertJUnit.assertNotNull(sa);
        sa.getTargets().add(depths);

        GameSimulator sim = createSimulator(game, p);
        sim.simulateSpellAbility(sa);
        Game simGame = sim.getSimulatedGameState();

        String strSimGame = gameStateToString(simGame);
        AssertJUnit.assertNull(strSimGame, findCardWithName(simGame, "Dark Depths"));
        AssertJUnit.assertNull(strSimGame, findCardWithName(simGame, "Thespian's Stage"));
        AssertJUnit.assertNotNull(strSimGame, findCardWithName(simGame, "Marit Lage"));
    }

    @Test
    public void testThespianStageSelfCopy() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCard("Swamp", p);
        addCard("Swamp", p);
        Card thespian = addCard("Thespian's Stage", p);
        AssertJUnit.assertTrue(thespian.isLand());
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = findSAWithPrefix(thespian,
                "{2}, {T}: CARDNAME becomes a copy of target land, except it has this ability.");
        AssertJUnit.assertNotNull(sa);
        sa.getTargets().add(thespian);

        GameSimulator sim = createSimulator(game, p);
        sim.simulateSpellAbility(sa);
        Game simGame = sim.getSimulatedGameState();
        Card thespianSim = findCardWithName(simGame, "Thespian's Stage");
        AssertJUnit.assertNotNull(gameStateToString(simGame), thespianSim);
        AssertJUnit.assertTrue(thespianSim.isLand());
    }

    @Test
    public void testDash() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Mountain", p);
        addCard("Mountain", p);
        String berserkerCardName = "Lightning Berserker";
        Card berserkerCard = addCardToZone(berserkerCardName, p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);

        SpellAbility dashSA = findSAWithPrefix(berserkerCard, "Dash");
        AssertJUnit.assertNotNull(dashSA);

        GameSimulator sim = createSimulator(game, p);
        int score = sim.simulateSpellAbility(dashSA).value;
        AssertJUnit.assertTrue(score > 0);
        Game simGame = sim.getSimulatedGameState();

        Card berserker = findCardWithName(simGame, berserkerCardName);
        AssertJUnit.assertNotNull(berserker);
        AssertJUnit.assertTrue(berserker.hasSVar("EndOfTurnLeavePlay"));
        AssertJUnit.assertEquals(1, berserker.getNetPower());
        AssertJUnit.assertEquals(1, berserker.getNetToughness());
        AssertJUnit.assertFalse(berserker.isSick());

        SpellAbility pumpSA = findSAWithPrefix(berserker, "{R}: CARDNAME gets +1/+0 until end of turn.");
        AssertJUnit.assertNotNull(pumpSA);
        GameSimulator sim2 = createSimulator(simGame, (Player) sim.getGameCopier().find(p));
        sim2.simulateSpellAbility(pumpSA);
        Game simGame2 = sim2.getSimulatedGameState();

        Card berserker2 = findCardWithName(simGame2, berserkerCardName);
        AssertJUnit.assertNotNull(berserker2);
        AssertJUnit.assertTrue(berserker2.hasSVar("EndOfTurnLeavePlay"));
        AssertJUnit.assertEquals(2, berserker2.getNetPower());
        AssertJUnit.assertEquals(1, berserker2.getNetToughness());
        AssertJUnit.assertFalse(berserker2.isSick());
    }

    @Test
    public void testTokenAbilities() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCards("Forest", 3, p);
        Card callTheScionsCard = addCardToZone("Call the Scions", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);

        SpellAbility callTheScionsSA = callTheScionsCard.getSpellAbilities().get(0);

        GameSimulator sim = createSimulator(game, p);
        int score = sim.simulateSpellAbility(callTheScionsSA).value;
        AssertJUnit.assertTrue(score > 0);
        Game simGame = sim.getSimulatedGameState();

        Card scion = findCardWithName(simGame, "Eldrazi Scion Token");
        AssertJUnit.assertNotNull(scion);
        AssertJUnit.assertEquals(1, scion.getNetPower());
        AssertJUnit.assertEquals(1, scion.getNetToughness());
        AssertJUnit.assertTrue(scion.isSick());
        AssertJUnit.assertNotNull(findSAWithPrefix(scion, "Sacrifice CARDNAME: Add {C}."));

        GameCopier copier = new GameCopier(simGame);
        Game copy = copier.makeCopy();
        Card scionCopy = findCardWithName(copy, "Eldrazi Scion Token");
        AssertJUnit.assertNotNull(scionCopy);
        AssertJUnit.assertEquals(1, scionCopy.getNetPower());
        AssertJUnit.assertEquals(1, scionCopy.getNetToughness());
        AssertJUnit.assertTrue(scionCopy.isSick());
        AssertJUnit.assertNotNull(findSAWithPrefix(scionCopy, "Sacrifice CARDNAME: Add {C}."));
    }

    @Test
    public void testMarkedDamage() {
        // Marked damage is important, as it's used during the AI declare
        // attackers logic which affects game state score - since P/T boosts
        // are evaluated differently for creatures participating in combat.

        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        String giantCardName = "Hill Giant";
        Card giant = addCard(giantCardName, p);
        addCard("Mountain", p);
        Card shockCard = addCardToZone("Shock", p, ZoneType.Hand);
        SpellAbility shockSA = shockCard.getFirstSpellAbility();

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertEquals(3, giant.getNetPower());
        AssertJUnit.assertEquals(3, giant.getNetToughness());
        AssertJUnit.assertEquals(0, giant.getDamage());

        GameSimulator sim = createSimulator(game, p);
        shockSA.setTargetCard(giant);
        sim.simulateSpellAbility(shockSA);
        Game simGame = sim.getSimulatedGameState();
        Card simGiant = findCardWithName(simGame, giantCardName);
        AssertJUnit.assertEquals(2, simGiant.getDamage());

        GameCopier copier = new GameCopier(simGame);
        Game copy = copier.makeCopy();
        Card giantCopy = findCardWithName(copy, giantCardName);
        AssertJUnit.assertEquals(2, giantCopy.getDamage());
    }

    @Test
    public void testLifelinkDamageSpell() {
        Game game = initAndCreateGame();
        Player p1 = game.getPlayers().get(0);
        Player p2 = game.getPlayers().get(1);

        String kalitasName = "Kalitas, Traitor of Ghet";
        String pridemateName = "Ajani's Pridemate";
        String indestructibilityName = "Indestructibility";
        String ignitionName = "Chandra's Ignition";
        String broodName = "Brood Monitor";

        // enough to cast Chandra's Ignition
        addCards("Mountain", 5, p1);

        Card kalitas = addCard(kalitasName, p1);
        Card pridemate = addCard(pridemateName, p1);
        Card indestructibility = addCard(indestructibilityName, p1);

        indestructibility.attachToEntity(pridemate, null);

        Card ignition = addCardToZone(ignitionName, p1, ZoneType.Hand);
        SpellAbility ignitionSA = ignition.getFirstSpellAbility();

        addCard(broodName, p2);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p1);
        game.getAction().checkStateEffects(true);

        GameSimulator sim = createSimulator(game, p1);
        ignitionSA.setTargetCard(kalitas);
        sim.simulateSpellAbility(ignitionSA);
        Game simGame = sim.getSimulatedGameState();
        Card simKalitas = findCardWithName(simGame, kalitasName);
        Card simPridemate = findCardWithName(simGame, pridemateName);
        Card simBrood = findCardWithName(simGame, broodName);

        // because it was destroyed
        AssertJUnit.assertNull(simBrood);
        AssertJUnit.assertNotNull(simPridemate);

        AssertJUnit.assertEquals(0, simKalitas.getDamage());
        AssertJUnit.assertEquals(3, simPridemate.getDamage());

        // only triggered once
        AssertJUnit.assertTrue(simPridemate.hasCounters());
        AssertJUnit.assertEquals(1, simPridemate.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals(1, simPridemate.getToughnessBonusFromCounters());
        AssertJUnit.assertEquals(1, simPridemate.getPowerBonusFromCounters());

        // 3 times 3 damage with life gain = 9 + 20 = 29
        AssertJUnit.assertEquals(29, simGame.getPlayers().get(0).getLife());
        AssertJUnit.assertEquals(17, simGame.getPlayers().get(1).getLife());
    }

    @Test
    public void testLifelinkDamageSpellMultiplier() {
        Game game = initAndCreateGame();
        Player p1 = game.getPlayers().get(0);
        Player p2 = game.getPlayers().get(1);

        String kalitasName = "Kalitas, Traitor of Ghet";
        String pridemateName = "Ajani's Pridemate";
        String giselaName = "Gisela, Blade of Goldnight";
        String ignitionName = "Chandra's Ignition";
        String broodName = "Brood Monitor";

        // enough to cast Chandra's Ignition
        addCards("Mountain", 5, p1);

        Card kalitas = addCard(kalitasName, p1);
        addCard(pridemateName, p1);
        addCard(giselaName, p1);

        Card ignition = addCardToZone(ignitionName, p1, ZoneType.Hand);
        SpellAbility ignitionSA = ignition.getFirstSpellAbility();

        addCard(broodName, p2);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p1);
        game.getAction().checkStateEffects(true);

        GameSimulator sim = createSimulator(game, p1);
        ignitionSA.setTargetCard(kalitas);
        sim.simulateSpellAbility(ignitionSA);
        Game simGame = sim.getSimulatedGameState();
        Card simKalitas = findCardWithName(simGame, kalitasName);
        Card simPridemate = findCardWithName(simGame, pridemateName);
        Card simGisela = findCardWithName(simGame, giselaName);
        Card simBrood = findCardWithName(simGame, broodName);

        // because it was destroyed
        AssertJUnit.assertNull(simBrood);

        AssertJUnit.assertEquals(0, simKalitas.getDamage());
        // 2 of the 3 are prevented
        AssertJUnit.assertEquals(1, simPridemate.getDamage());
        AssertJUnit.assertEquals(1, simGisela.getDamage());

        // only triggered once
        AssertJUnit.assertTrue(simPridemate.hasCounters());
        AssertJUnit.assertEquals(1, simPridemate.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals(1, simPridemate.getToughnessBonusFromCounters());
        AssertJUnit.assertEquals(1, simPridemate.getPowerBonusFromCounters());

        // 2 times 3 / 2 rounded down = 2 * 1 = 2
        // 2 times 3 * 2 = 12
        AssertJUnit.assertEquals(34, simGame.getPlayers().get(0).getLife());
        AssertJUnit.assertEquals(14, simGame.getPlayers().get(1).getLife());
    }

    @Test
    public void testLifelinkDamageSpellRedirected() {
        Game game = initAndCreateGame();
        Player p1 = game.getPlayers().get(0);
        Player p2 = game.getPlayers().get(1);

        String kalitasName = "Kalitas, Traitor of Ghet";
        String pridemateName = "Ajani's Pridemate";
        String indestructibilityName = "Indestructibility";
        String ignitionName = "Chandra's Ignition";
        String broodName = "Brood Monitor";
        String palisadeName = "Palisade Giant";

        // enough to cast Chandra's Ignition
        addCards("Mountain", 5, p1);

        Card kalitas = addCard(kalitasName, p1);
        Card pridemate = addCard(pridemateName, p1);
        Card indestructibility = addCard(indestructibilityName, p1);

        indestructibility.attachToEntity(pridemate, null);

        Card ignition = addCardToZone(ignitionName, p1, ZoneType.Hand);
        SpellAbility ignitionSA = ignition.getFirstSpellAbility();

        addCard(broodName, p2);
        addCard(palisadeName, p2);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p1);
        game.getAction().checkStateEffects(true);

        GameSimulator sim = createSimulator(game, p1);
        ignitionSA.setTargetCard(kalitas);
        sim.simulateSpellAbility(ignitionSA);
        Game simGame = sim.getSimulatedGameState();
        Card simKalitas = findCardWithName(simGame, kalitasName);
        Card simPridemate = findCardWithName(simGame, pridemateName);
        Card simBrood = findCardWithName(simGame, broodName);
        Card simPalisade = findCardWithName(simGame, palisadeName);

        // not destroyed because damage redirected
        AssertJUnit.assertNotNull(simBrood);
        AssertJUnit.assertEquals(0, simBrood.getDamage());

        // destroyed because of to much redirected damage
        AssertJUnit.assertNull(simPalisade);
        AssertJUnit.assertNotNull(simPridemate);

        AssertJUnit.assertEquals(0, simKalitas.getDamage());
        AssertJUnit.assertEquals(3, simPridemate.getDamage());

        // only triggered once
        AssertJUnit.assertTrue(simPridemate.hasCounters());
        AssertJUnit.assertEquals(1, simPridemate.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals(1, simPridemate.getToughnessBonusFromCounters());
        AssertJUnit.assertEquals(1, simPridemate.getPowerBonusFromCounters());

        // 4 times 3 damage with life gain = 12 + 20 = 32
        AssertJUnit.assertEquals(32, simGame.getPlayers().get(0).getLife());
        AssertJUnit.assertEquals(20, simGame.getPlayers().get(1).getLife());
    }

    @Test
    public void testLifelinkDamageSpellMultipleDamage() {
        Game game = initAndCreateGame();
        Player p1 = game.getPlayers().get(0);
        Player p2 = game.getPlayers().get(1);

        String soulfireName = "Soulfire Grand Master";
        String pridemateName = "Ajani's Pridemate";
        String coneName = "Cone of Flame";

        String bearCardName = "Runeclaw Bear";
        String giantCardName = "Hill Giant";

        String tormentName = "Everlasting Torment";
        String meliraName = "Melira, Sylvok Outcast";

        // enough to cast Cone of Flame
        addCards("Mountain", 5, p1);

        addCard(soulfireName, p1);
        addCard(pridemateName, p1);

        Card bearCard = addCard(bearCardName, p2);
        Card giantCard = addCard(giantCardName, p2);

        Card cone = addCardToZone(coneName, p1, ZoneType.Hand);
        SpellAbility coneSA = cone.getFirstSpellAbility();

        coneSA.setTargetCard(bearCard); // one damage to bear
        coneSA.getSubAbility().setTargetCard(giantCard); // two damage to giant
        coneSA.getSubAbility().getSubAbility().getTargets().add(p2); // three damage to player

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p1);
        game.getAction().checkStateEffects(true);

        GameSimulator sim = createSimulator(game, p1);

        sim.simulateSpellAbility(coneSA);
        Game simGame = sim.getSimulatedGameState();
        Card simBear = findCardWithName(simGame, bearCardName);
        Card simGiant = findCardWithName(simGame, giantCardName);
        Card simPridemate = findCardWithName(simGame, pridemateName);

        // spell deals multiple damages to multiple targets, only one cause of lifegain
        AssertJUnit.assertNotNull(simPridemate);
        AssertJUnit.assertTrue(simPridemate.hasCounters());
        AssertJUnit.assertEquals(1, simPridemate.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals(1, simPridemate.getToughnessBonusFromCounters());
        AssertJUnit.assertEquals(1, simPridemate.getPowerBonusFromCounters());

        AssertJUnit.assertNotNull(simBear);
        AssertJUnit.assertEquals(1, simBear.getDamage());

        AssertJUnit.assertNotNull(simGiant);
        AssertJUnit.assertEquals(2, simGiant.getDamage());

        // 1 + 2 + 3 lifegain
        AssertJUnit.assertEquals(26, simGame.getPlayers().get(0).getLife());
        AssertJUnit.assertEquals(17, simGame.getPlayers().get(1).getLife());

        // second pard with Everlasting Torment
        addCard(tormentName, p2);

        GameSimulator sim2 = createSimulator(game, p1);

        sim2.simulateSpellAbility(coneSA);
        Game simGame2 = sim2.getSimulatedGameState();
        Card simBear2 = findCardWithName(simGame2, bearCardName);
        Card simGiant2 = findCardWithName(simGame2, giantCardName);
        Card simPridemate2 = findCardWithName(simGame2, pridemateName);

        // no Lifegain because of Everlasting Torment
        AssertJUnit.assertNotNull(simPridemate2);
        AssertJUnit.assertFalse(simPridemate2.hasCounters());
        AssertJUnit.assertEquals(0, simPridemate2.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals(0, simPridemate2.getToughnessBonusFromCounters());
        AssertJUnit.assertEquals(0, simPridemate2.getPowerBonusFromCounters());

        AssertJUnit.assertNotNull(simBear2);
        AssertJUnit.assertEquals(0, simBear2.getDamage());
        AssertJUnit.assertTrue(simBear2.hasCounters());
        AssertJUnit.assertEquals(1, simBear2.getCounters(CounterEnumType.M1M1));
        AssertJUnit.assertEquals(-1, simBear2.getToughnessBonusFromCounters());
        AssertJUnit.assertEquals(-1, simBear2.getPowerBonusFromCounters());

        AssertJUnit.assertNotNull(simGiant2);
        AssertJUnit.assertEquals(0, simGiant2.getDamage());
        AssertJUnit.assertTrue(simGiant2.hasCounters());
        AssertJUnit.assertEquals(2, simGiant2.getCounters(CounterEnumType.M1M1));
        AssertJUnit.assertEquals(-2, simGiant2.getToughnessBonusFromCounters());
        AssertJUnit.assertEquals(-2, simGiant2.getPowerBonusFromCounters());

        // no life gain
        AssertJUnit.assertEquals(20, simGame2.getPlayers().get(0).getLife());
        AssertJUnit.assertEquals(17, simGame2.getPlayers().get(1).getLife());

        // third pard with Melira prevents wither
        addCard(meliraName, p2);

        GameSimulator sim3 = createSimulator(game, p1);

        sim3.simulateSpellAbility(coneSA);
        Game simGame3 = sim3.getSimulatedGameState();
        Card simBear3 = findCardWithName(simGame3, bearCardName);
        Card simGiant3 = findCardWithName(simGame3, giantCardName);
        Card simPridemate3 = findCardWithName(simGame3, pridemateName);

        // no Lifegain because of Everlasting Torment
        AssertJUnit.assertNotNull(simPridemate3);
        AssertJUnit.assertFalse(simPridemate3.hasCounters());
        AssertJUnit.assertEquals(0, simPridemate3.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals(0, simPridemate3.getToughnessBonusFromCounters());
        AssertJUnit.assertEquals(0, simPridemate3.getPowerBonusFromCounters());

        AssertJUnit.assertNotNull(simBear3);
        AssertJUnit.assertEquals(0, simBear3.getDamage());
        AssertJUnit.assertFalse(simBear3.hasCounters());
        AssertJUnit.assertEquals(0, simBear3.getCounters(CounterEnumType.M1M1));
        AssertJUnit.assertEquals(0, simBear3.getToughnessBonusFromCounters());
        AssertJUnit.assertEquals(0, simBear3.getPowerBonusFromCounters());

        AssertJUnit.assertNotNull(simGiant3);
        AssertJUnit.assertEquals(0, simGiant3.getDamage());
        AssertJUnit.assertFalse(simGiant3.hasCounters());
        AssertJUnit.assertEquals(0, simGiant3.getCounters(CounterEnumType.M1M1));
        AssertJUnit.assertEquals(0, simGiant3.getToughnessBonusFromCounters());
        AssertJUnit.assertEquals(0, simGiant3.getPowerBonusFromCounters());

        // no life gain
        AssertJUnit.assertEquals(20, simGame2.getPlayers().get(0).getLife());
        AssertJUnit.assertEquals(17, simGame2.getPlayers().get(1).getLife());
    }

    @Test
    public void testTransform() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCard("Swamp", p);
        addCard("Swamp", p);
        addCard("Swamp", p);
        String lilianaCardName = "Liliana, Heretical Healer";
        String lilianaPWName = "Liliana, Defiant Necromancer";
        Card lilianaInPlay = addCard(lilianaCardName, p);
        Card lilianaInHand = addCardToZone(lilianaCardName, p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertTrue(lilianaInPlay.isCreature());
        AssertJUnit.assertEquals(2, lilianaInPlay.getNetPower());
        AssertJUnit.assertEquals(3, lilianaInPlay.getNetToughness());

        SpellAbility playLiliana = lilianaInHand.getSpellAbilities().get(0);
        GameSimulator sim = createSimulator(game, p);
        sim.simulateSpellAbility(playLiliana);
        Game simGame = sim.getSimulatedGameState();
        AssertJUnit.assertNull(findCardWithName(simGame, lilianaCardName));
        Card lilianaPW = findCardWithName(simGame, lilianaPWName);
        AssertJUnit.assertNotNull(lilianaPW);
        AssertJUnit.assertTrue(lilianaPW.isPlaneswalker());
        AssertJUnit.assertEquals(3, lilianaPW.getCurrentLoyalty());

        GameCopier copier = new GameCopier(simGame);
        Game copy = copier.makeCopy();
        Card lilianaPWCopy = findCardWithName(copy, lilianaPWName);
        AssertJUnit.assertNotNull(lilianaPWCopy);
        AssertJUnit.assertTrue(lilianaPWCopy.isPlaneswalker());
        AssertJUnit.assertEquals(3, lilianaPWCopy.getCurrentLoyalty());
    }

    @Test
    public void testEnergy() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCard("Island", p);
        String turtleCardName = "Thriving Turtle";
        Card turtleCard = addCardToZone(turtleCardName, p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(0, p.getCounters(CounterEnumType.ENERGY));

        SpellAbility playTurtle = turtleCard.getSpellAbilities().get(0);
        GameSimulator sim = createSimulator(game, p);
        sim.simulateSpellAbility(playTurtle);
        Game simGame = sim.getSimulatedGameState();
        Player simP = simGame.getPlayer(p.getId());
        AssertJUnit.assertEquals(2, simP.getCounters(CounterEnumType.ENERGY));

        GameCopier copier = new GameCopier(simGame);
        Game copy = copier.makeCopy();
        Player copyP = copy.getPlayer(p.getId());
        AssertJUnit.assertEquals(2, copyP.getCounters(CounterEnumType.ENERGY));
    }

    @Test
    public void testFloatingMana() {
        Game game = initAndCreateGame();
        Player p0 = game.getPlayers().get(0);
        Player p1 = game.getPlayers().get(1);
        addCard("Swamp", p1);
        Card darkRitualCard = addCardToZone("Dark Ritual", p1, ZoneType.Hand);
        Card darkConfidantCard = addCardToZone("Dark Confidant", p1, ZoneType.Hand);
        Card deathriteCard = addCardToZone("Deathrite Shaman", p1, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p1);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(p1.getManaPool().isEmpty());

        SpellAbility playRitual = darkRitualCard.getSpellAbilities().get(0);
        GameSimulator sim = createSimulator(game, p1);
        sim.simulateSpellAbility(playRitual);
        Game simGame = sim.getSimulatedGameState();

        Player simP1 = simGame.getPlayer(p1.getId());
        AssertJUnit.assertEquals(3, simP1.getManaPool().totalMana());
        AssertJUnit.assertEquals(3, simP1.getManaPool().getAmountOfColor(MagicColor.BLACK));

        Card darkConfidantCard2 = (Card) sim.getGameCopier().find(darkConfidantCard);
        SpellAbility playDarkConfidant2 = darkConfidantCard2.getSpellAbilities().get(0);
        Card deathriteCard2 = (Card) sim.getGameCopier().find(deathriteCard);

        GameSimulator sim2 = createSimulator(simGame, simP1);
        sim2.simulateSpellAbility(playDarkConfidant2);
        Game sim2Game = sim2.getSimulatedGameState();
        Player sim2P = sim2Game.getPlayer(simP1.getId());
        AssertJUnit.assertEquals(1, sim2P.getManaPool().totalMana());
        AssertJUnit.assertEquals(1, sim2P.getManaPool().getAmountOfColor(MagicColor.BLACK));

        Card deathriteCard3 = (Card) sim2.getGameCopier().find(deathriteCard2);
        SpellAbility playDeathriteCard3 = deathriteCard3.getSpellAbilities().get(0);

        GameSimulator sim3 = createSimulator(sim2Game, sim2P);
        sim3.simulateSpellAbility(playDeathriteCard3);
        Game sim3Game = sim3.getSimulatedGameState();
        Player sim3P = sim3Game.getPlayer(sim2P.getId());
        AssertJUnit.assertEquals(0, sim3P.getManaPool().totalMana());
        AssertJUnit.assertEquals(0, sim3P.getManaPool().getAmountOfColor(MagicColor.BLACK));
    }

    @Test
    public void testEnKor() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);

        String soulfireName = "Soulfire Grand Master";
        String pridemateName = "Ajani's Pridemate";

        String enKorName = "Spirit en-Kor";
        String bearName = "Runeclaw Bear";
        String shockName = "Shock";

        addCard("Mountain", p);

        addCard(soulfireName, p);
        addCard(pridemateName, p);

        Card shockCard = addCardToZone(shockName, p, ZoneType.Hand);

        Card enKor = addCard(enKorName, p);

        SpellAbility enKorSA = findSAWithPrefix(enKor, "{0}:");

        Card bear = addCard(bearName, p);

        SpellAbility shockSA = shockCard.getFirstSpellAbility();

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertEquals(2, enKor.getNetPower());
        AssertJUnit.assertEquals(2, enKor.getNetToughness());
        AssertJUnit.assertEquals(0, enKor.getDamage());

        AssertJUnit.assertEquals(2, bear.getNetPower());
        AssertJUnit.assertEquals(2, bear.getNetToughness());
        AssertJUnit.assertEquals(0, bear.getDamage());

        GameSimulator sim = createSimulator(game, p);
        enKorSA.setTargetCard(bear);
        shockSA.setTargetCard(enKor);
        sim.simulateSpellAbility(enKorSA);
        sim.simulateSpellAbility(shockSA);
        Game simGame = sim.getSimulatedGameState();
        Card simEnKor = findCardWithName(simGame, enKorName);
        Card simBear = findCardWithName(simGame, bearName);

        AssertJUnit.assertNotNull(simEnKor);
        AssertJUnit.assertEquals(1, simEnKor.getDamage());

        AssertJUnit.assertNotNull(simBear);
        AssertJUnit.assertEquals(1, simBear.getDamage());

        Card simPridemate = findCardWithName(simGame, pridemateName);

        // only triggered once
        AssertJUnit.assertTrue(simPridemate.hasCounters());
        AssertJUnit.assertEquals(1, simPridemate.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals(1, simPridemate.getToughnessBonusFromCounters());
        AssertJUnit.assertEquals(1, simPridemate.getPowerBonusFromCounters());

        AssertJUnit.assertEquals(22, simGame.getPlayers().get(0).getLife());
    }

    @Test
    public void testRazia() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);

        String soulfireName = "Soulfire Grand Master";
        String pridemateName = "Ajani's Pridemate";

        String raziaName = "Razia, Boros Archangel";
        String bearName = "Runeclaw Bear";
        String greetingName = "Alchemist's Greeting";

        addCards("Mountain", 5, p);

        addCard(soulfireName, p);
        addCard(pridemateName, p);

        Card greetingCard = addCardToZone(greetingName, p, ZoneType.Hand);

        Card razia = addCard(raziaName, p);

        SpellAbility preventSA = findSAWithPrefix(razia, "{T}:");

        Card bear = addCard(bearName, p);

        SpellAbility greetingSA = greetingCard.getFirstSpellAbility();

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertEquals(0, razia.getDamage());

        AssertJUnit.assertEquals(2, bear.getNetPower());
        AssertJUnit.assertEquals(2, bear.getNetToughness());
        AssertJUnit.assertEquals(0, bear.getDamage());

        GameSimulator sim = createSimulator(game, p);
        preventSA.setTargetCard(razia);
        preventSA.getSubAbility().setTargetCard(bear);
        greetingSA.setTargetCard(razia);
        sim.simulateSpellAbility(preventSA);
        sim.simulateSpellAbility(greetingSA);
        Game simGame = sim.getSimulatedGameState();
        Card simRazia = findCardWithName(simGame, raziaName);
        Card simBear = findCardWithName(simGame, bearName);

        AssertJUnit.assertNotNull(simRazia);
        AssertJUnit.assertEquals(1, simRazia.getDamage());

        // bear destroyed
        AssertJUnit.assertNull(simBear);

        Card simPridemate = findCardWithName(simGame, pridemateName);

        // only triggered once
        AssertJUnit.assertTrue(simPridemate.hasCounters());
        AssertJUnit.assertEquals(1, simPridemate.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals(1, simPridemate.getToughnessBonusFromCounters());
        AssertJUnit.assertEquals(1, simPridemate.getPowerBonusFromCounters());

        AssertJUnit.assertEquals(24, simGame.getPlayers().get(0).getLife());
    }

    @Test
    public void testRazia2() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);

        String soulfireName = "Soulfire Grand Master";
        String pridemateName = "Ajani's Pridemate";

        String raziaName = "Razia, Boros Archangel";
        String elementalName = "Air Elemental";
        String shockName = "Shock";

        addCards("Mountain", 3, p);

        addCard(soulfireName, p);
        addCard(pridemateName, p);

        Card shockCard1 = addCardToZone(shockName, p, ZoneType.Hand);
        Card shockCard2 = addCardToZone(shockName, p, ZoneType.Hand);

        Card razia = addCard(raziaName, p);

        SpellAbility preventSA = findSAWithPrefix(razia, "{T}:");

        Card elemental = addCard(elementalName, p);

        SpellAbility shockSA1 = shockCard1.getFirstSpellAbility();
        SpellAbility shockSA2 = shockCard2.getFirstSpellAbility();

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertEquals(0, razia.getDamage());

        AssertJUnit.assertEquals(4, elemental.getNetPower());
        AssertJUnit.assertEquals(4, elemental.getNetToughness());
        AssertJUnit.assertEquals(0, elemental.getDamage());

        GameSimulator sim = createSimulator(game, p);
        preventSA.setTargetCard(razia);
        preventSA.getSubAbility().setTargetCard(elemental);
        shockSA1.setTargetCard(razia);
        shockSA2.setTargetCard(razia);
        sim.simulateSpellAbility(preventSA);
        sim.simulateSpellAbility(shockSA1);
        sim.simulateSpellAbility(shockSA2);
        Game simGame = sim.getSimulatedGameState();
        Card simRazia = findCardWithName(simGame, raziaName);
        Card simElemental = findCardWithName(simGame, elementalName);

        AssertJUnit.assertNotNull(simRazia);
        AssertJUnit.assertEquals(1, simRazia.getDamage());

        // elemental not destroyed
        AssertJUnit.assertNotNull(simElemental);
        AssertJUnit.assertEquals(3, simElemental.getDamage());

        Card simPridemate = findCardWithName(simGame, pridemateName);

        // only triggered twice
        AssertJUnit.assertTrue(simPridemate.hasCounters());
        AssertJUnit.assertEquals(2, simPridemate.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals(2, simPridemate.getToughnessBonusFromCounters());
        AssertJUnit.assertEquals(2, simPridemate.getPowerBonusFromCounters());

        AssertJUnit.assertEquals(24, simGame.getPlayers().get(0).getLife());
    }

    @Test
    public void testMassRemovalVsKalitas() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        Player opp = game.getPlayers().get(1);

        addCardToZone("Kalitas, Traitor of Ghet", p, ZoneType.Battlefield);

        addCards("Plains", 4, p);

        addCards("Aboroth", 2, opp);

        Card wrathOfGod = addCardToZone("Wrath of God", p, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);

        SpellAbility wrathSA = wrathOfGod.getFirstSpellAbility();
        AssertJUnit.assertNotNull(wrathSA);

        GameSimulator sim = createSimulator(game, p);
        int score = sim.simulateSpellAbility(wrathSA).value;
        AssertJUnit.assertTrue(score > 0);
        Game simGame = sim.getSimulatedGameState();

        int numZombies = countCardsWithName(simGame, "Zombie Token");
        AssertJUnit.assertEquals(2, numZombies);
    }

    @Test
    public void testKalitasNumberOfTokens() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        Player opp = game.getPlayers().get(1);

        addCardToZone("Kalitas, Traitor of Ghet", p, ZoneType.Battlefield);
        addCardToZone("Anointed Procession", p, ZoneType.Battlefield);
        addCardToZone("Swamp", p, ZoneType.Battlefield);
        addCards("Mountain", 4, p);

        Card goblin = addCardToZone("Raging Goblin", opp, ZoneType.Battlefield);
        Card goblin2 = addCardToZone("Raging Goblin", opp, ZoneType.Battlefield);

        // Fatal Push: should generate 2 tokens
        Card fatalPush = addCardToZone("Fatal Push", p, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        SpellAbility fatalPushSA = fatalPush.getFirstSpellAbility();
        AssertJUnit.assertNotNull(fatalPushSA);
        fatalPushSA.setTargetCard(goblin);

        // Electrify: should also generate 2 tokens after the Ixalan rules update
        Card electrify = addCardToZone("Electrify", p, ZoneType.Hand);
        SpellAbility electrifySA = electrify.getFirstSpellAbility();
        AssertJUnit.assertNotNull(electrifySA);
        electrifySA.setTargetCard(goblin2);

        GameSimulator sim = createSimulator(game, p);
        int score = sim.simulateSpellAbility(fatalPushSA).value;
        AssertJUnit.assertTrue(score > 0);
        AssertJUnit.assertEquals(2, countCardsWithName(sim.getSimulatedGameState(), "Zombie Token"));

        score = sim.simulateSpellAbility(electrifySA).value;
        AssertJUnit.assertTrue(score > 0);
        AssertJUnit.assertEquals(4, countCardsWithName(sim.getSimulatedGameState(), "Zombie Token"));
    }

    @Test
    public void testBrutalCatharDoesNotTriggerReturnedSlaughterSpecialistFromItsDeath() {
        Game game = initAndCreateGame();
        Player catharController = game.getPlayers().get(0);
        Player specialistController = game.getPlayers().get(1);

        addCards("Plains", 3, catharController);
        addCards("Swamp", 3, catharController);
        addCards("Mountain", 1, specialistController);

        addCardToZone("Slaughter Specialist", specialistController, ZoneType.Battlefield);
        Card brutalCathar = addCardToZone("Brutal Cathar", catharController, ZoneType.Hand);
        addCardToZone("Murder", catharController, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, catharController);
        SpellAbility catharSa = brutalCathar.getFirstSpellAbility();
        AssertJUnit.assertNotNull(catharSa);

        GameSimulator sim = createSimulator(game, catharController);
        sim.simulateSpellAbility(catharSa);

        Game afterCathar = sim.getSimulatedGameState();
        AssertJUnit.assertEquals(1, countCardsWithName(afterCathar, "Slaughter Specialist", ZoneType.Exile));
        AssertJUnit.assertEquals(0, countCardsWithName(afterCathar, "Slaughter Specialist", ZoneType.Battlefield));
        System.out.println("Brutal Cathar regression setup: Slaughter Specialist exiled.");

        Card simCathar = findCardWithName(afterCathar, "Brutal Cathar");
        AssertJUnit.assertNotNull(simCathar);
        Card simMurder = null;
        for (Card c : afterCathar.getPlayers().get(0).getCardsIn(ZoneType.Hand)) {
            if ("Murder".equals(c.getName())) {
                simMurder = c;
                break;
            }
        }
        AssertJUnit.assertNotNull(simMurder);
        SpellAbility murderSa = simMurder.getFirstSpellAbility();
        AssertJUnit.assertNotNull(murderSa);
        murderSa.setTargetCard(simCathar);
        afterCathar.getTriggerHandler().resetActiveTriggers();
        GameSimulator sim2 = createSimulator(afterCathar, afterCathar.getPlayers().get(0));
        sim2.simulateSpellAbility(murderSa);
        Game afterShock = sim2.getSimulatedGameState();

        AssertJUnit.assertEquals(0, countCardsWithName(afterShock, "Brutal Cathar", ZoneType.Battlefield));
        int specialistOnBattlefield = countCardsWithName(afterShock, "Slaughter Specialist", ZoneType.Battlefield);
        int specialistInExile = countCardsWithName(afterShock, "Slaughter Specialist", ZoneType.Exile);
        AssertJUnit.assertEquals(1, specialistOnBattlefield + specialistInExile);
        if (specialistOnBattlefield == 1) {
            Card returnedSpecialist = findCardWithName(afterShock, "Slaughter Specialist");
            AssertJUnit.assertNotNull(returnedSpecialist);
            AssertJUnit.assertEquals(0, returnedSpecialist.getCounters(CounterEnumType.P1P1));
        }
    }

    @Test
    public void testSlaughterSpecialistTriggersForNormalOpponentCreatureDeath() {
        Game game = initAndCreateGame();
        Player opponent = game.getPlayers().get(0);
        Player specialistController = game.getPlayers().get(1);

        addCardToZone("Slaughter Specialist", specialistController, ZoneType.Battlefield);
        Card opponentCreature = addCardToZone("Raging Goblin", opponent, ZoneType.Battlefield);
        addCardToZone("Mountain", specialistController, ZoneType.Battlefield);
        Card shock = addCardToZone("Shock", specialistController, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, specialistController);
        SpellAbility shockSa = shock.getFirstSpellAbility();
        AssertJUnit.assertNotNull(shockSa);
        shockSa.setTargetCard(opponentCreature);

        game.getTriggerHandler().resetActiveTriggers();
        GameSimulator sim = createSimulator(game, specialistController);
        sim.simulateSpellAbility(shockSa);
        Game afterShock = sim.getSimulatedGameState();

        Card specialist = findCardWithName(afterShock, "Slaughter Specialist");
        AssertJUnit.assertNotNull(specialist);
        AssertJUnit.assertEquals(1, specialist.getCounters(CounterEnumType.P1P1));
    }

    @Test
    public void testSlaughterSpecialistTriggersForMultipleOpponentCreatureDeaths() {
        Game game = initAndCreateGame();
        Player opponent = game.getPlayers().get(0);
        Player specialistController = game.getPlayers().get(1);

        addCardToZone("Slaughter Specialist", specialistController, ZoneType.Battlefield);
        addCardToZone("Raging Goblin", opponent, ZoneType.Battlefield);
        addCardToZone("Raging Goblin", opponent, ZoneType.Battlefield);
        addCards("Mountain", 2, specialistController);
        Card pyroclasm = addCardToZone("Pyroclasm", specialistController, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, specialistController);
        SpellAbility pyroSa = pyroclasm.getFirstSpellAbility();
        AssertJUnit.assertNotNull(pyroSa);

        game.getTriggerHandler().resetActiveTriggers();
        GameSimulator sim = createSimulator(game, specialistController);
        sim.simulateSpellAbility(pyroSa);
        Game afterPyro = sim.getSimulatedGameState();

        Card specialist = findCardWithName(afterPyro, "Slaughter Specialist");
        AssertJUnit.assertNotNull(specialist);
        AssertJUnit.assertEquals(2, specialist.getCounters(CounterEnumType.P1P1));
    }

    @Test
    public void testSlaughterSpecialistStillTriggersAfterBeingBlinked() {
        Game game = initAndCreateGame();
        Player opponent = game.getPlayers().get(0);
        Player specialistController = game.getPlayers().get(1);

        Card specialist = addCardToZone("Slaughter Specialist", specialistController, ZoneType.Battlefield);
        Card opponentCreature = addCardToZone("Raging Goblin", opponent, ZoneType.Battlefield);
        addCardToZone("Plains", specialistController, ZoneType.Battlefield);
        Card cloudshift = addCardToZone("Cloudshift", specialistController, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, specialistController);
        SpellAbility blinkSa = cloudshift.getFirstSpellAbility();
        AssertJUnit.assertNotNull(blinkSa);
        blinkSa.setTargetCard(specialist);

        GameSimulator sim = createSimulator(game, specialistController);
        sim.simulateSpellAbility(blinkSa);
        Game afterBlink = sim.getSimulatedGameState();

        Card simSpecialist = findCardWithName(afterBlink, "Slaughter Specialist");
        AssertJUnit.assertNotNull(simSpecialist);
        Card simOpponentCreature = null;
        for (Card c : afterBlink.getCardsIn(ZoneType.Battlefield)) {
            if ("Raging Goblin".equals(c.getName()) && c.getController().equals(afterBlink.getPlayers().get(0))) {
                simOpponentCreature = c;
                break;
            }
        }
        AssertJUnit.assertNotNull(simOpponentCreature);

        afterBlink.getAction().destroy(simOpponentCreature, null, true, AbilityKey.newMap());
        playUntilStackClear(afterBlink);

        AssertJUnit.assertEquals(1, simSpecialist.getCounters(CounterEnumType.P1P1));
    }

    @Test
    public void testDoomedDissenterDiesTriggerStillWorks() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);

        Card dissenter = addCardToZone("Doomed Dissenter", p, ZoneType.Battlefield);
        addCardToZone("Mountain", p, ZoneType.Battlefield);
        Card shock = addCardToZone("Shock", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        SpellAbility shockSa = shock.getFirstSpellAbility();
        AssertJUnit.assertNotNull(shockSa);
        shockSa.setTargetCard(dissenter);

        game.getTriggerHandler().resetActiveTriggers();
        GameSimulator sim = createSimulator(game, p);
        sim.simulateSpellAbility(shockSa);
        Game afterShock = sim.getSimulatedGameState();

        AssertJUnit.assertEquals(1, countCardsWithName(afterShock, "Zombie Token", ZoneType.Battlefield));
    }

    @Test
    public void testLeavesBattlefieldToHandTriggerStillWorks() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        Player opp = game.getPlayers().get(1);

        Card shambler = addCardToZone("Subterranean Shambler", p, ZoneType.Battlefield);
        addCardToZone("Raging Goblin", opp, ZoneType.Battlefield);
        addCardToZone("Island", p, ZoneType.Battlefield);
        Card unsummon = addCardToZone("Unsummon", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        SpellAbility unsummonSa = unsummon.getFirstSpellAbility();
        AssertJUnit.assertNotNull(unsummonSa);
        unsummonSa.setTargetCard(shambler);

        game.getTriggerHandler().resetActiveTriggers();
        GameSimulator sim = createSimulator(game, p);
        sim.simulateSpellAbility(unsummonSa);
        Game afterUnsummon = sim.getSimulatedGameState();

        AssertJUnit.assertEquals(0, countCardsWithName(afterUnsummon, "Subterranean Shambler", ZoneType.Battlefield));
        AssertJUnit.assertEquals(1, countCardsWithName(afterUnsummon, "Subterranean Shambler", ZoneType.Hand));
        AssertJUnit.assertEquals(0, countCardsWithName(afterUnsummon, "Raging Goblin", ZoneType.Battlefield));
    }

    @Test
    public void testBrutalCatharReturnDoesNotSeeSimultaneousOtherCreatureDeaths() {
        Game game = initAndCreateGame();
        Player catharController = game.getPlayers().get(0);
        Player opponent = game.getPlayers().get(1);

        addCards("Plains", 3, catharController);
        addCards("Mountain", 2, catharController);
        addCardToZone("Slaughter Specialist", opponent, ZoneType.Battlefield);
        Card brutalCathar = addCardToZone("Brutal Cathar", catharController, ZoneType.Hand);
        addCardToZone("Pyroclasm", catharController, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, catharController);
        SpellAbility catharSa = brutalCathar.getFirstSpellAbility();
        AssertJUnit.assertNotNull(catharSa);
        game.getTriggerHandler().resetActiveTriggers();
        GameSimulator sim = createSimulator(game, catharController);
        sim.simulateSpellAbility(catharSa);
        Game afterCathar = sim.getSimulatedGameState();
        AssertJUnit.assertEquals(1, countCardsWithName(afterCathar, "Slaughter Specialist", ZoneType.Exile));
        AssertJUnit.assertEquals(0, countCardsWithName(afterCathar, "Slaughter Specialist", ZoneType.Battlefield));
        addCardToZone("Raging Goblin", afterCathar.getPlayers().get(1), ZoneType.Battlefield);

        Card simCathar = findCardWithName(afterCathar, "Brutal Cathar");
        AssertJUnit.assertNotNull(simCathar);
        Card simGoblin = null;
        for (Card c : afterCathar.getCardsIn(ZoneType.Battlefield)) {
            if ("Raging Goblin".equals(c.getName())) {
                simGoblin = c;
                break;
            }
        }
        AssertJUnit.assertNotNull(simGoblin);

        Card simPyroclasm = null;
        for (Card c : afterCathar.getPlayers().get(0).getCardsIn(ZoneType.Hand)) {
            if ("Pyroclasm".equals(c.getName())) {
                simPyroclasm = c;
                break;
            }
        }
        AssertJUnit.assertNotNull(simPyroclasm);
        SpellAbility pyroSa = simPyroclasm.getFirstSpellAbility();
        AssertJUnit.assertNotNull(pyroSa);
        afterCathar.getTriggerHandler().resetActiveTriggers();
        GameSimulator sim2 = createSimulator(afterCathar, afterCathar.getPlayers().get(0));
        sim2.simulateSpellAbility(pyroSa);
        Game afterWrath = sim2.getSimulatedGameState();
        AssertJUnit.assertEquals(0, countCardsWithName(afterWrath, "Brutal Cathar", ZoneType.Battlefield));
        AssertJUnit.assertEquals(0, countCardsWithName(afterWrath, "Raging Goblin", ZoneType.Battlefield));

        int specialistOnBattlefield = countCardsWithName(afterWrath, "Slaughter Specialist", ZoneType.Battlefield);
        int specialistInExile = countCardsWithName(afterWrath, "Slaughter Specialist", ZoneType.Exile);
        AssertJUnit.assertEquals(1, specialistOnBattlefield + specialistInExile);
        if (specialistOnBattlefield == 1) {
            Card returnedSpecialist = findCardWithName(afterWrath, "Slaughter Specialist");
            AssertJUnit.assertNotNull(returnedSpecialist);
            AssertJUnit.assertEquals(0, returnedSpecialist.getCounters(CounterEnumType.P1P1));
        }
    }

    @Test
    public void testSlaughterSpecialistEtbChangesZoneStillWorks() {
        Game game = initAndCreateGame();
        Player opponent = game.getPlayers().get(0);
        Player specialistController = game.getPlayers().get(1);

        addCards("Swamp", 2, specialistController);
        Card specialist = addCardToZone("Slaughter Specialist", specialistController, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, specialistController);
        SpellAbility specialistSa = specialist.getFirstSpellAbility();
        AssertJUnit.assertNotNull(specialistSa);

        game.getTriggerHandler().resetActiveTriggers();
        GameSimulator sim = createSimulator(game, specialistController);
        sim.simulateSpellAbility(specialistSa);
        Game afterCast = sim.getSimulatedGameState();

        Player simOpponent = afterCast.getPlayers().get(0);
        AssertJUnit.assertEquals(1, simOpponent.getCreaturesInPlay().size());
    }

    @Test
    public void testCommandZoneChangesZoneTriggerStillWorksWithOubliette() {
        Game game = initAndCreateGame();
        Player oublietteController = game.getPlayers().get(0);
        Player opponent = game.getPlayers().get(1);

        addCards("Swamp", 3, oublietteController);
        addCardToZone("Raging Goblin", opponent, ZoneType.Battlefield);
        Card oubliette = addCardToZone("Oubliette", oublietteController, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, oublietteController);
        SpellAbility oublietteSa = oubliette.getFirstSpellAbility();
        AssertJUnit.assertNotNull(oublietteSa);
        game.getTriggerHandler().resetActiveTriggers();
        GameSimulator sim = createSimulator(game, oublietteController);
        sim.simulateSpellAbility(oublietteSa);
        Game afterOubliette = sim.getSimulatedGameState();

        Card simOubliette = findCardWithName(afterOubliette, "Oubliette");
        AssertJUnit.assertNotNull(simOubliette);
        afterOubliette.getAction().destroy(simOubliette, null, true, destroyParams(afterOubliette));
        playUntilStackClear(afterOubliette);

        Card returnedGoblin = null;
        for (Card c : afterOubliette.getCardsIn(ZoneType.Battlefield)) {
            if ("Raging Goblin".equals(c.getName()) && c.getController().equals(afterOubliette.getPlayers().get(1))) {
                returnedGoblin = c;
                break;
            }
        }
        AssertJUnit.assertNotNull(returnedGoblin);
        AssertJUnit.assertFalse(returnedGoblin.isPhasedOut());
        AssertJUnit.assertTrue(returnedGoblin.isTapped());
    }

    @Test
    public void testLkiCounterValueDiesTriggerStillWorksForJotunOwlKeeper() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);

        addCards("Swamp", 3, p);
        Card owlKeeper = addCardToZone("Jotun Owl Keeper", p, ZoneType.Battlefield);
        owlKeeper.addCounterInternal(CounterEnumType.AGE, 2, p, true, null, AbilityKey.newMap());
        AssertJUnit.assertEquals(2, owlKeeper.getCounters(CounterEnumType.AGE));
        Card murder = addCardToZone("Murder", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        SpellAbility murderSa = murder.getFirstSpellAbility();
        AssertJUnit.assertNotNull(murderSa);
        murderSa.setTargetCard(owlKeeper);
        game.getTriggerHandler().resetActiveTriggers();
        GameSimulator sim = createSimulator(game, p);
        sim.simulateSpellAbility(murderSa);
        Game afterMurder = sim.getSimulatedGameState();

        AssertJUnit.assertEquals(2, countCardsWithName(afterMurder, "Bird Token", ZoneType.Battlefield));
    }

    @Test
    public void testFiendHunterReturnDoesNotSeePastOpponentDeath() {
        Game game = initAndCreateGame();
        Player hunterController = game.getPlayers().get(0);
        Player opponent = game.getPlayers().get(1);

        addCards("Plains", 3, hunterController);
        addCardToZone("Slaughter Specialist", opponent, ZoneType.Battlefield);
        Card fiendHunter = addCardToZone("Fiend Hunter", hunterController, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, hunterController);
        SpellAbility fiendHunterSa = fiendHunter.getFirstSpellAbility();
        AssertJUnit.assertNotNull(fiendHunterSa);
        game.getTriggerHandler().resetActiveTriggers();
        GameSimulator sim = createSimulator(game, hunterController);
        sim.simulateSpellAbility(fiendHunterSa);
        Game afterHunter = sim.getSimulatedGameState();
        AssertJUnit.assertEquals(1, countCardsWithName(afterHunter, "Slaughter Specialist", ZoneType.Exile));
        AssertJUnit.assertEquals(0, countCardsWithName(afterHunter, "Slaughter Specialist", ZoneType.Battlefield));
        addCardToZone("Raging Goblin", afterHunter.getPlayers().get(0), ZoneType.Battlefield);

        Card simGoblin = null;
        for (Card c : afterHunter.getCardsIn(ZoneType.Battlefield)) {
            if ("Raging Goblin".equals(c.getName()) && c.getController().equals(afterHunter.getPlayers().get(0))) {
                simGoblin = c;
                break;
            }
        }
        AssertJUnit.assertNotNull(simGoblin);
        Card simHunter = findCardWithName(afterHunter, "Fiend Hunter");
        AssertJUnit.assertNotNull(simHunter);

        Map<AbilityKey, Object> destroyParams = destroyParams(afterHunter);
        afterHunter.getAction().destroy(simGoblin, null, true, destroyParams);
        afterHunter.getAction().destroy(simHunter, null, true, destroyParams);
        playUntilStackClear(afterHunter);

        int specialistOnBattlefield = countCardsWithName(afterHunter, "Slaughter Specialist", ZoneType.Battlefield);
        int specialistInExile = countCardsWithName(afterHunter, "Slaughter Specialist", ZoneType.Exile);
        AssertJUnit.assertEquals(1, specialistOnBattlefield + specialistInExile);
        if (specialistOnBattlefield == 1) {
            Card returnedSpecialist = findCardWithName(afterHunter, "Slaughter Specialist");
            AssertJUnit.assertNotNull(returnedSpecialist);
            AssertJUnit.assertEquals(0, returnedSpecialist.getCounters(CounterEnumType.P1P1));
        }
    }

    @Test
    public void testBanisherPriestReturnDoesNotSeePastOpponentDeath() {
        Game game = initAndCreateGame();
        Player priestController = game.getPlayers().get(0);
        Player opponent = game.getPlayers().get(1);

        addCards("Plains", 3, priestController);
        addCards("Mountain", 1, priestController);
        addCards("Swamp", 3, priestController);
        addCardToZone("Slaughter Specialist", opponent, ZoneType.Battlefield);
        Card banisherPriest = addCardToZone("Banisher Priest", priestController, ZoneType.Hand);
        addCardToZone("Shock", priestController, ZoneType.Hand);
        addCardToZone("Murder", priestController, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, priestController);
        SpellAbility priestSa = banisherPriest.getFirstSpellAbility();
        AssertJUnit.assertNotNull(priestSa);
        game.getTriggerHandler().resetActiveTriggers();
        GameSimulator sim = createSimulator(game, priestController);
        sim.simulateSpellAbility(priestSa);
        Game afterPriest = sim.getSimulatedGameState();
        AssertJUnit.assertEquals(1, countCardsWithName(afterPriest, "Slaughter Specialist", ZoneType.Exile));
        AssertJUnit.assertEquals(0, countCardsWithName(afterPriest, "Slaughter Specialist", ZoneType.Battlefield));
        addCardToZone("Raging Goblin", afterPriest.getPlayers().get(0), ZoneType.Battlefield);

        Card simGoblin = null;
        for (Card c : afterPriest.getCardsIn(ZoneType.Battlefield)) {
            if ("Raging Goblin".equals(c.getName()) && c.getController().equals(afterPriest.getPlayers().get(0))) {
                simGoblin = c;
                break;
            }
        }
        AssertJUnit.assertNotNull(simGoblin);
        Card simPriest = findCardWithName(afterPriest, "Banisher Priest");
        AssertJUnit.assertNotNull(simPriest);

        Card simShock = null;
        for (Card c : afterPriest.getPlayers().get(0).getCardsIn(ZoneType.Hand)) {
            if ("Shock".equals(c.getName())) {
                simShock = c;
                break;
            }
        }
        AssertJUnit.assertNotNull(simShock);
        SpellAbility shockSa = simShock.getFirstSpellAbility();
        AssertJUnit.assertNotNull(shockSa);
        shockSa.setTargetCard(simGoblin);
        afterPriest.getTriggerHandler().resetActiveTriggers();
        GameSimulator sim2 = createSimulator(afterPriest, afterPriest.getPlayers().get(0));
        sim2.simulateSpellAbility(shockSa);
        Game afterShock = sim2.getSimulatedGameState();
        AssertJUnit.assertEquals(0, countCardsWithName(afterShock, "Raging Goblin", ZoneType.Battlefield));

        Card simPriestAfterShock = findCardWithName(afterShock, "Banisher Priest");
        AssertJUnit.assertNotNull(simPriestAfterShock);
        Card simMurder = null;
        for (Card c : afterShock.getPlayers().get(0).getCardsIn(ZoneType.Hand)) {
            if ("Murder".equals(c.getName())) {
                simMurder = c;
                break;
            }
        }
        AssertJUnit.assertNotNull(simMurder);
        SpellAbility murderSa = simMurder.getFirstSpellAbility();
        AssertJUnit.assertNotNull(murderSa);
        murderSa.setTargetCard(simPriestAfterShock);
        afterShock.getTriggerHandler().resetActiveTriggers();
        GameSimulator sim3 = createSimulator(afterShock, afterShock.getPlayers().get(0));
        sim3.simulateSpellAbility(murderSa);
        Game afterMurder = sim3.getSimulatedGameState();
        AssertJUnit.assertEquals(0, countCardsWithName(afterMurder, "Banisher Priest", ZoneType.Battlefield));

        int specialistOnBattlefield = countCardsWithName(afterMurder, "Slaughter Specialist", ZoneType.Battlefield);
        int specialistInExile = countCardsWithName(afterMurder, "Slaughter Specialist", ZoneType.Exile);
        AssertJUnit.assertEquals(1, specialistOnBattlefield + specialistInExile);
        if (specialistOnBattlefield == 1) {
            Card returnedSpecialist = findCardWithName(afterMurder, "Slaughter Specialist");
            AssertJUnit.assertNotNull(returnedSpecialist);
            AssertJUnit.assertEquals(0, returnedSpecialist.getCounters(CounterEnumType.P1P1));
        }
    }

    @Test
    public void testPoisonTipArcherSeesSimultaneousCreatureDeathsWhileInPlay() {
        Game game = initAndCreateGame();
        Player archerController = game.getPlayers().get(0);
        Player opponent = game.getPlayers().get(1);

        addCardToZone("Poison-Tip Archer", archerController, ZoneType.Battlefield);
        addCards("Mountain", 2, archerController);
        addCards("Raging Goblin", 2, opponent);
        Card pyroclasm = addCardToZone("Pyroclasm", archerController, ZoneType.Hand);

        int opponentLifeBefore = opponent.getLife();
        int controllerLifeBefore = archerController.getLife();

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, archerController);
        SpellAbility pyroSa = pyroclasm.getFirstSpellAbility();
        AssertJUnit.assertNotNull(pyroSa);

        game.getTriggerHandler().resetActiveTriggers();
        GameSimulator sim = createSimulator(game, archerController);
        sim.simulateSpellAbility(pyroSa);
        Game afterPyro = sim.getSimulatedGameState();

        Player simController = afterPyro.getPlayers().get(0);
        Player simOpponent = afterPyro.getPlayers().get(1);
        AssertJUnit.assertEquals(0, countCardsWithName(afterPyro, "Raging Goblin", ZoneType.Battlefield));
        AssertJUnit.assertEquals(opponentLifeBefore - 2, simOpponent.getLife());
        AssertJUnit.assertEquals(controllerLifeBefore, simController.getLife());
    }

    @Test
    public void testTokenExiledByReplacementStillCountsAsLeavesBattlefield() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        Player opp = game.getPlayers().get(1);

        addCards("Plains", 3, p);
        addCardToZone("Rest in Peace", p, ZoneType.Battlefield);
        addCardToZone("Raging Goblin", opp, ZoneType.Battlefield);
        Card skyclave = addCardToZone("Skyclave Apparition", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        SpellAbility skyclaveSa = skyclave.getFirstSpellAbility();
        AssertJUnit.assertNotNull(skyclaveSa);
        GameSimulator sim = createSimulator(game, p);
        sim.simulateSpellAbility(skyclaveSa);
        Game afterApparition = sim.getSimulatedGameState();
        AssertJUnit.assertEquals(1, countCardsWithName(afterApparition, "Raging Goblin", ZoneType.Exile));

        Card simApparition = findCardWithName(afterApparition, "Skyclave Apparition");
        AssertJUnit.assertNotNull(simApparition);
        afterApparition.getTriggerHandler().resetActiveTriggers();
        afterApparition.getAction().destroy(simApparition, null, true, destroyParams(afterApparition));
        playUntilStackClear(afterApparition);

        AssertJUnit.assertEquals(1, countCardsWithName(afterApparition, "Illusion Token", ZoneType.Battlefield));
        AssertJUnit.assertEquals(1, countCardsWithName(afterApparition, "Skyclave Apparition", ZoneType.Exile));
    }

    @Test
    public void testPlayerXCount() {
        // If playerXCount is operational, then conditions that count something
        // about the player (e.g.
        // cards in hand, life total) should work, similar to the Bloodghast
        // "Haste" condition.
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        Player opp = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);

        Card bloodghast = addCardToZone("Bloodghast", p, ZoneType.Battlefield);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertFalse(bloodghast.hasKeyword(Keyword.HASTE));

        opp.setLife(5, null);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertTrue(bloodghast.hasKeyword(Keyword.HASTE));
    }

    @Test
    public void testDeathsShadow() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);

        addCardToZone("Platinum Angel", p, ZoneType.Battlefield);
        Card deathsShadow = addCardToZone("Death's Shadow", p, ZoneType.Battlefield);

        p.setLife(1, null);
        game.getAction().checkStateEffects(true);
        assert (deathsShadow.getNetPower() == 12);

        p.setLife(-1, null);
        game.getAction().checkStateEffects(true);
        assert (deathsShadow.getNetPower() == 13); // on negative life, should
        // always be 13/13
    }

    @Test
    public void testBludgeonBrawlLatticeAura() {
        // Enchantment Aura are with Mycosynth Lattice turned into Artifact Enchantment
        // - Aura Equipment
        // Creature Auras should stay on
        String bearCardName = "Runeclaw Bear";
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);

        Card bear = addCard(bearCardName, p);
        bear.setSickness(false);
        Card lifelink = addCard("Lifelink", p);
        lifelink.attachToEntity(bear, null);

        AssertJUnit.assertTrue(bear.isEnchanted());
        AssertJUnit.assertTrue(bear.hasCardAttachment(lifelink));

        // this adds Artifact Type
        addCardToZone("Mycosynth Lattice", p, ZoneType.Battlefield);

        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(bear.isEnchanted());
        AssertJUnit.assertFalse(bear.isEquipped());

        AssertJUnit.assertTrue(lifelink.isArtifact());
        AssertJUnit.assertFalse(lifelink.isEquipment());

        // this add Equipment and causes it to fall off
        addCardToZone("Bludgeon Brawl", p, ZoneType.Battlefield);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(bear.isEnchanted());
        AssertJUnit.assertTrue(bear.isEquipped());

        AssertJUnit.assertTrue(lifelink.isArtifact());
        AssertJUnit.assertTrue(lifelink.isEquipment());

        // still in battlefield
        AssertJUnit.assertTrue(lifelink.isInPlay());
    }

    @Test
    public void testBludgeonBrawlLatticeCurse() {
        // Enchantment Aura are with Mycosynth Lattice turned into Artifact Enchantment
        // - Aura Equipment
        // Curses can only attach Player, but Equipment can only attach to Creature so
        // it does fall off
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);

        final String curseName = "Cruel Reality";

        Card curse = addCard(curseName, p);
        curse.attachToEntity(p, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(p.isEnchanted());
        AssertJUnit.assertTrue(p.hasCardAttachment(curse));

        // this adds Artifact Type
        addCardToZone("Mycosynth Lattice", p, ZoneType.Battlefield);

        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(p.isEnchanted());
        AssertJUnit.assertTrue(curse.isArtifact());

        // this add Equipment and causes it to fall off
        addCardToZone("Bludgeon Brawl", p, ZoneType.Battlefield);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse(p.isEnchanted());

        // not in Battlefield anymore
        AssertJUnit.assertFalse(curse.isInPlay());
        AssertJUnit.assertTrue(curse.isInZone(ZoneType.Graveyard));
    }

    @Test
    public void testBludgeonBrawlFortification() {
        // Bludgeon Brawl makes Fortification into Equipment
        // that means it can't attach a Land anymore if the Land is no Creature

        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);

        Card mountain = addCardToZone("Mountain", p, ZoneType.Battlefield);
        Card fortification = addCardToZone("Darksteel Garrison", p, ZoneType.Battlefield);

        fortification.attachToEntity(mountain, null);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertTrue(fortification.isFortification());
        AssertJUnit.assertFalse(fortification.isEquipment());

        AssertJUnit.assertTrue(mountain.isFortified());
        AssertJUnit.assertTrue(mountain.hasCardAttachment(fortification));
        AssertJUnit.assertTrue(mountain.hasKeyword(Keyword.INDESTRUCTIBLE));

        // adding Brawl will cause the Fortification into Equipment and it to
        // fall off
        addCardToZone("Bludgeon Brawl", p, ZoneType.Battlefield);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertFalse(fortification.isFortification());
        AssertJUnit.assertTrue(fortification.isEquipment());

        AssertJUnit.assertFalse(mountain.hasCardAttachment(fortification));
        AssertJUnit.assertFalse(mountain.hasKeyword(Keyword.INDESTRUCTIBLE));
    }

    @Test
    public void testBludgeonBrawlFortificationDryad() {
        // Bludgeon Brawl makes Fortification into Equipment
        // that means it can't attach a Land anymore if the Land is no Creature too
        // Dryad Arbor is both a Land and a Creature so it stays attached

        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);

        Card dryad = addCardToZone("Dryad Arbor", p, ZoneType.Battlefield);
        Card fortification = addCardToZone("Darksteel Garrison", p, ZoneType.Battlefield);

        fortification.attachToEntity(dryad, null);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertTrue(dryad.isFortified());
        AssertJUnit.assertFalse(dryad.isEquipped());

        AssertJUnit.assertTrue(dryad.hasCardAttachment(fortification));
        AssertJUnit.assertTrue(dryad.hasKeyword(Keyword.INDESTRUCTIBLE));

        // adding Brawl will cause the Fortification into Equipment
        // because Dryad Arbor is a Creature it stays attached
        addCardToZone("Bludgeon Brawl", p, ZoneType.Battlefield);
        game.getAction().checkStateEffects(true);

        // switched from Fortification to Equipment
        AssertJUnit.assertFalse(dryad.isFortified());
        AssertJUnit.assertTrue(dryad.isEquipped());

        AssertJUnit.assertTrue(dryad.hasCardAttachment(fortification));
        AssertJUnit.assertTrue(dryad.hasKeyword(Keyword.INDESTRUCTIBLE));
    }

    @Test
    public void testRiotEnchantment() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);

        final String goblinName = "Zhur-Taa Goblin";

        addCard("Rhythm of the Wild", p);

        Card goblin = addCardToZone(goblinName, p, ZoneType.Hand);

        addCard("Mountain", p);
        addCard("Forest", p);

        SpellAbility goblinSA = goblin.getFirstSpellAbility();
        AssertJUnit.assertNotNull(goblinSA);

        GameSimulator sim = createSimulator(game, p);
        int score = sim.simulateSpellAbility(goblinSA).value;
        AssertJUnit.assertTrue(score > 0);

        Game simGame = sim.getSimulatedGameState();

        Card simGoblin = findCardWithName(simGame, goblinName);

        AssertJUnit.assertNotNull(simGoblin);
        int effects = simGoblin.getCounters(CounterEnumType.P1P1) + simGoblin.getKeywordMagnitude(Keyword.HASTE);
        AssertJUnit.assertEquals(2, effects);
    }

    @Test
    public void testTeysaKarlovXathridNecromancer() {
        // Teysa Karlov and Xathrid Necromancer dying at the same time makes 4 token

        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);

        addCard("Teysa Karlov", p);
        addCard("Xathrid Necromancer", p);

        addCards("Plains", 4, p);

        Card wrathOfGod = addCardToZone("Wrath of God", p, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);

        SpellAbility wrathSA = wrathOfGod.getFirstSpellAbility();
        AssertJUnit.assertNotNull(wrathSA);

        GameSimulator sim = createSimulator(game, p);
        int score = sim.simulateSpellAbility(wrathSA).value;
        AssertJUnit.assertTrue(score > 0);
        Game simGame = sim.getSimulatedGameState();

        int numZombies = countCardsWithName(simGame, "Zombie Token");
        AssertJUnit.assertEquals(4, numZombies);
    }

    @Test
    public void testDoubleTeysaKarlovXathridNecromancer() {
        // Teysa Karlov dieing because of Legendary rule will make Xathrid Necromancer
        // trigger 3 times

        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);

        addCard("Teysa Karlov", p);
        addCard("Xathrid Necromancer", p);

        for (int i = 0; i < 3; i++) {
            addCard("Plains", p);
        }
        addCard("Swamp", p);

        Card second = addCardToZone("Teysa Karlov", p, ZoneType.Hand);

        SpellAbility secondSA = second.getFirstSpellAbility();

        GameSimulator sim = createSimulator(game, p);
        int score = sim.simulateSpellAbility(secondSA).value;
        AssertJUnit.assertTrue(score > 0);
        Game simGame = sim.getSimulatedGameState();

        int numZombies = countCardsWithName(simGame, "Zombie Token");
        AssertJUnit.assertEquals(3, numZombies);
    }

    @Test
    public void testTeysaKarlovGitrogMonster() {

        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);

        addCard("Teysa Karlov", p);
        addCard("The Gitrog Monster", p);
        addCard("Dryad Arbor", p);

        for (int i = 0; i < 4; i++) {
            addCard("Plains", p);
            addCardToZone("Plains", p, ZoneType.Library);
        }

        Card armageddon = addCardToZone("Armageddon", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);

        SpellAbility armageddonSA = armageddon.getFirstSpellAbility();

        GameSimulator sim = createSimulator(game, p);
        int score = sim.simulateSpellAbility(armageddonSA).value;
        AssertJUnit.assertTrue(score > 0);
        Game simGame = sim.getSimulatedGameState();

        // Two cards drawn
        AssertJUnit.assertEquals(2, simGame.getPlayers().get(0).getZone(ZoneType.Hand).size());
    }

    @Test
    public void testTeysaKarlovGitrogMonsterGitrogDies() {

        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);

        Card teysa = addCard("Teysa Karlov", p);
        addCard("The Gitrog Monster", p);
        addCard("Dryad Arbor", p);

        String indestructibilityName = "Indestructibility";
        Card indestructibility = addCard(indestructibilityName, p);

        indestructibility.attachToEntity(teysa, null);

        // update Indestructible state
        game.getAction().checkStateEffects(true);

        for (int i = 0; i < 4; i++) {
            addCard("Plains", p);
            addCardToZone("Plains", p, ZoneType.Library);
        }

        Card armageddon = addCardToZone("Wrath of God", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);

        SpellAbility armageddonSA = armageddon.getFirstSpellAbility();

        GameSimulator sim = createSimulator(game, p);
        int score = sim.simulateSpellAbility(armageddonSA).value;
        AssertJUnit.assertTrue(score > 0);
        Game simGame = sim.getSimulatedGameState();

        // One cards drawn
        AssertJUnit.assertEquals(0, simGame.getPlayers().get(0).getZone(ZoneType.Hand).size());
    }

    @Test
    public void testTeysaKarlovGitrogMonsterTeysaDies() {

        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);

        addCard("Teysa Karlov", p);
        Card gitrog = addCard("The Gitrog Monster", p);
        addCard("Dryad Arbor", p);

        String indestructibilityName = "Indestructibility";
        Card indestructibility = addCard(indestructibilityName, p);

        indestructibility.attachToEntity(gitrog, null);

        // update Indestructible state
        game.getAction().checkStateEffects(true);

        for (int i = 0; i < 4; i++) {
            addCard("Plains", p);
            addCardToZone("Plains", p, ZoneType.Library);
        }

        Card armageddon = addCardToZone("Wrath of God", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);

        SpellAbility armageddonSA = armageddon.getFirstSpellAbility();

        GameSimulator sim = createSimulator(game, p);
        int score = sim.simulateSpellAbility(armageddonSA).value;
        AssertJUnit.assertTrue(score > 0);
        Game simGame = sim.getSimulatedGameState();

        // One cards drawn
        AssertJUnit.assertEquals(1, simGame.getPlayers().get(0).getZone(ZoneType.Hand).size());
    }

    @Test
    public void testCloneTransform() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        Player p2 = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);

        final String outLawName = "Kruin Outlaw";
        final String hillGiantName = "Elite Vanguard";
        final String terrorName = "Terror of Kruin Pass";

        Card outlaw = addCard(outLawName, p2);
        Card giant = addCard(hillGiantName, p);

        AssertJUnit.assertFalse(outlaw.isCloned());
        AssertJUnit.assertTrue(outlaw.isTransformable());
        AssertJUnit.assertTrue(outlaw.hasState(CardStateName.Backside));
        AssertJUnit.assertTrue(outlaw.canTransform(null));
        AssertJUnit.assertFalse(outlaw.isBackSide());

        AssertJUnit.assertFalse(giant.isTransformable());
        AssertJUnit.assertFalse(giant.canTransform(null));

        addCards("Forest", 4, p);
        addCard("Island", p);

        Card cytoCard = addCardToZone("Cytoshape", p, ZoneType.Hand);
        SpellAbility cytoSA = cytoCard.getFirstSpellAbility();

        Card moonmist = addCardToZone("Moonmist", p, ZoneType.Hand);
        SpellAbility moonmistSA = moonmist.getFirstSpellAbility();

        cytoSA.getTargets().add(outlaw);

        GameSimulator sim = createSimulator(game, p);
        int score = sim.simulateSpellAbility(cytoSA).value;

        AssertJUnit.assertTrue(score > 0);

        Game simGame = sim.getSimulatedGameState();

        AssertJUnit.assertEquals(0, countCardsWithName(simGame, outLawName));
        AssertJUnit.assertEquals(2, countCardsWithName(simGame, hillGiantName));
        AssertJUnit.assertEquals(0, countCardsWithName(simGame, terrorName));

        Card clonedOutLaw = (Card) sim.getGameCopier().find(outlaw);

        AssertJUnit.assertTrue(clonedOutLaw.isCloned());
        AssertJUnit.assertTrue(clonedOutLaw.isTransformable());
        AssertJUnit.assertTrue(clonedOutLaw.hasState(CardStateName.Backside));
        AssertJUnit.assertTrue(clonedOutLaw.canTransform(null));
        AssertJUnit.assertFalse(clonedOutLaw.isBackSide());

        AssertJUnit.assertEquals(clonedOutLaw.getName(), hillGiantName);

        score = sim.simulateSpellAbility(moonmistSA).value;
        AssertJUnit.assertTrue(score > 0);

        simGame = sim.getSimulatedGameState();

        AssertJUnit.assertEquals(0, countCardsWithName(simGame, outLawName));
        AssertJUnit.assertEquals(2, countCardsWithName(simGame, hillGiantName));
        AssertJUnit.assertEquals(0, countCardsWithName(simGame, terrorName));

        Card transformOutLaw = (Card) sim.getGameCopier().find(outlaw);

        AssertJUnit.assertTrue(transformOutLaw.isCloned());
        AssertJUnit.assertTrue(transformOutLaw.isTransformable());
        AssertJUnit.assertTrue(transformOutLaw.hasState(CardStateName.Backside));
        AssertJUnit.assertTrue(transformOutLaw.canTransform(null));
        AssertJUnit.assertTrue(transformOutLaw.isBackSide());

        AssertJUnit.assertEquals(transformOutLaw.getName(), hillGiantName);

        // need to clean up the clone state
        simGame.getPhaseHandler().devAdvanceToPhase(PhaseType.CLEANUP);

        AssertJUnit.assertEquals(0, countCardsWithName(simGame, outLawName));
        AssertJUnit.assertEquals(1, countCardsWithName(simGame, hillGiantName));
        AssertJUnit.assertEquals(1, countCardsWithName(simGame, terrorName));

        AssertJUnit.assertFalse(transformOutLaw.isCloned());
        AssertJUnit.assertTrue(transformOutLaw.isTransformable());
        AssertJUnit.assertTrue(transformOutLaw.hasState(CardStateName.Backside));
        AssertJUnit.assertTrue(transformOutLaw.canTransform(null));
        AssertJUnit.assertTrue(transformOutLaw.isBackSide());

        AssertJUnit.assertEquals(transformOutLaw.getName(), terrorName);
    }

    @Test
    public void testVolrathsShapeshifter() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);

        Card volrath = addCard("Volrath's Shapeshifter", p);

        // 1. Assert that Volrath has the Discard ability
        SpellAbility discard = findSAWithPrefix(volrath, "{2}");
        AssertJUnit.assertTrue(discard != null && discard.getApi() == ApiType.Discard);

        // 2. Copy the text from a creature
        addCardToZone("Abattoir Ghoul", p, ZoneType.Graveyard);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertEquals("Abattoir Ghoul", volrath.getName());
        AssertJUnit.assertEquals(3, volrath.getNetPower());
        AssertJUnit.assertEquals(2, volrath.getNetToughness());
        AssertJUnit.assertTrue(volrath.hasKeyword(Keyword.FIRST_STRIKE));

        SpellAbility discardAfterCopy = findSAWithPrefix(volrath, "{2}");
        AssertJUnit.assertTrue(discardAfterCopy != null && discardAfterCopy.getApi() == ApiType.Discard);

        // 3. Revert back to not copying any text
        addCardToZone("Plains", p, ZoneType.Graveyard);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertEquals("Volrath's Shapeshifter", volrath.getName());
        AssertJUnit.assertEquals(0, volrath.getNetPower());
        AssertJUnit.assertEquals(1, volrath.getNetToughness());
        AssertJUnit.assertTrue(volrath.getKeywords().isEmpty());

        SpellAbility discardAfterRevert = findSAWithPrefix(volrath, "{2}");
        AssertJUnit.assertTrue(discardAfterRevert != null && discardAfterRevert.getApi() == ApiType.Discard);
    }

    @Test
    public void testSparkDoubleAndGideon() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);

        addCards("Plains", 7, p);
        addCards("Island", 7, p);

        Card gideon = addCardToZone("Gideon Blackblade", p, ZoneType.Hand);
        Card sparkDouble = addCardToZone("Spark Double", p, ZoneType.Hand);

        SpellAbility gideonSA = gideon.getFirstSpellAbility();
        SpellAbility sparkDoubleSA = sparkDouble.getFirstSpellAbility();

        GameSimulator sim = createSimulator(game, p);
        sim.simulateSpellAbility(gideonSA);
        sim.simulateSpellAbility(sparkDoubleSA);

        Card simSpark = sim.getSimulatedGameState().findById(sparkDouble.getId());

        AssertJUnit.assertNotNull(simSpark);
        AssertJUnit.assertTrue(simSpark.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertEquals(1, simSpark.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals(5, simSpark.getCounters(CounterEnumType.LOYALTY));
    }

    @Test
    public void testVituGhaziAndCytoshape() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);

        addCards("Plains", 7, p);
        addCards("Island", 7, p);
        addCards("Forest", 7, p);

        Card tgtLand = addCardToZone("Wastes", p, ZoneType.Battlefield);

        Card vituGhazi = addCardToZone("Awakening of Vitu-Ghazi", p, ZoneType.Hand);
        Card cytoshape = addCardToZone("Cytoshape", p, ZoneType.Hand);
        Card goblin = addCardToZone("Raging Goblin", p, ZoneType.Battlefield);

        SpellAbility vituSA = vituGhazi.getFirstSpellAbility();
        vituSA.getTargets().add(tgtLand);

        SpellAbility cytoSA = cytoshape.getFirstSpellAbility();
        cytoSA.getTargets().add(tgtLand);

        GameSimulator sim = createSimulator(game, p);
        sim.simulateSpellAbility(vituSA);
        sim.simulateSpellAbility(cytoSA);

        Card awakened = findCardWithName(sim.getSimulatedGameState(), "Vitu-Ghazi");

        AssertJUnit.assertNotNull(awakened);
        AssertJUnit.assertEquals("Vitu-Ghazi", awakened.getName());
        AssertJUnit.assertEquals(9, awakened.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertTrue(awakened.hasKeyword(Keyword.HASTE));
        AssertJUnit.assertTrue(awakened.getType().hasSubtype("Goblin"));
    }

    @Test
    public void testNecroticOozeActivateOnce() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);

        addCards("Swamp", 7, p);
        addCards("Forest", 7, p);

        addCardToZone("Basking Rootwalla", p, ZoneType.Graveyard);
        Card ooze = addCardToZone("Necrotic Ooze", p, ZoneType.Hand);

        SpellAbility oozeSA = ooze.getFirstSpellAbility();
        GameSimulator sim = createSimulator(game, p);
        sim.simulateSpellAbility(oozeSA);

        Card oozeOTB = findCardWithName(sim.getSimulatedGameState(), "Necrotic Ooze");

        AssertJUnit.assertNotNull(oozeOTB);

        SpellAbility copiedSA = findSAWithPrefix(oozeOTB, "{1}{G}:");
        AssertJUnit.assertNotNull(copiedSA);
        AssertJUnit.assertEquals("1", copiedSA.getRestrictions().getLimitToCheck());
    }

    @Test
    public void testEpochrasite() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);

        addCards("Swamp", 7, p);

        Card epo = addCardToZone("Epochrasite", p, ZoneType.Graveyard);
        Card animate = addCardToZone("Animate Dead", p, ZoneType.Hand);

        SpellAbility saAnimate = animate.getFirstSpellAbility();
        saAnimate.getTargets().add(epo);

        GameSimulator sim = createSimulator(game, p);
        sim.simulateSpellAbility(saAnimate);

        Card epoOTB = findCardWithName(sim.getSimulatedGameState(), "Epochrasite");

        AssertJUnit.assertNotNull(epoOTB);
        AssertJUnit.assertEquals(3, epoOTB.getCounters(CounterEnumType.P1P1));
    }

    @SuppressWarnings("unused")
    public void broken_testCloneDimir() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);

        // add enough cards to hand to flip Jushi
        for (int i = 0; i < 9; i++) {
            addCardToZone("Plains", p, ZoneType.Hand);
            addCardToZone("Plains", p, ZoneType.Library);
            addCard("Swamp", p);
            addCard("Island", p);
        }

        Card dimirdg = addCard("Dimir Doppelganger", p);
        // so T can be paid
        dimirdg.setSickness(false);
        SpellAbility saDimirClone = findSAWithPrefix(dimirdg, "{1}{U}{B}");

        AssertJUnit.assertTrue(saDimirClone != null && saDimirClone.getApi() == ApiType.ChangeZone);

        Card jushi = addCardToZone("Jushi Apprentice", p, ZoneType.Graveyard);
        Card bear = addCardToZone("Runeclaw Bear", p, ZoneType.Graveyard);
        Card nezumi = addCardToZone("Nezumi Shortfang", p, ZoneType.Graveyard);

        // Clone Jushi first
        saDimirClone.getTargets().add(jushi);
        GameSimulator sim = createSimulator(game, p);
        int score = sim.simulateSpellAbility(saDimirClone).value;
        AssertJUnit.assertTrue(score > 0);

        Card dimirdgAfterCopy1 = (Card) sim.getGameCopier().find(dimirdg);
        AssertJUnit.assertEquals("Jushi Apprentice", dimirdgAfterCopy1.getName());
        AssertJUnit.assertEquals(1, dimirdgAfterCopy1.getNetPower());
        AssertJUnit.assertEquals(2, dimirdgAfterCopy1.getNetToughness());
        AssertJUnit.assertTrue(dimirdgAfterCopy1.isFlipCard());
        AssertJUnit.assertFalse(dimirdgAfterCopy1.isFlipped());
        AssertJUnit.assertFalse(dimirdgAfterCopy1.getType().isLegendary());

        bear = (Card) sim.getGameCopier().find(bear);

        // make new simulator so new SpellAbility is found
        Game simGame = sim.getSimulatedGameState();
        sim = createSimulator(simGame, p);

        Player copiedPlayer = (Player) sim.getGameCopier().find(p);
        int handSize = copiedPlayer.getCardsIn(ZoneType.Hand).size();
        AssertJUnit.assertEquals(9, handSize);

        SpellAbility draw = findSAWithPrefix(dimirdgAfterCopy1, "{2}{U}");
        score = sim.simulateSpellAbility(draw).value;
        AssertJUnit.assertTrue(score > 0);

        copiedPlayer = (Player) sim.getGameCopier().find(p);
        handSize = copiedPlayer.getCardsIn(ZoneType.Hand).size();
        AssertJUnit.assertEquals(10, handSize);

        simGame = sim.getSimulatedGameState();

        bear = (Card) sim.getGameCopier().find(bear);

        // make new simulator so new SpellAbility is found
        simGame = sim.getSimulatedGameState();
        sim = createSimulator(simGame, p);

        // bear = (Card)sim.getGameCopier().find(bear);

        simGame = sim.getSimulatedGameState();

        Card dimirdgAfterFlip1 = (Card) sim.getGameCopier().find(dimirdgAfterCopy1);

        AssertJUnit.assertEquals("Tomoya the Revealer", dimirdgAfterFlip1.getName());
        AssertJUnit.assertEquals(2, dimirdgAfterFlip1.getNetPower());
        AssertJUnit.assertEquals(3, dimirdgAfterFlip1.getNetToughness());
        AssertJUnit.assertTrue(dimirdgAfterFlip1.isFlipped());
        AssertJUnit.assertTrue(dimirdgAfterFlip1.getType().isLegendary());

        saDimirClone = findSAWithPrefix(dimirdgAfterCopy1, "{1}{U}{B}");
        // Clone Bear first
        saDimirClone.resetTargets();
        saDimirClone.getTargets().add(bear);

        score = sim.simulateSpellAbility(saDimirClone).value;
        AssertJUnit.assertTrue(score > 0);

        Card dimirdgAfterCopy2 = (Card) sim.getGameCopier().find(dimirdgAfterCopy1);

        // System.out.println(sim.getSimulatedGameState().getCardsIn(ZoneType.Battlefield));

        System.out.println(dimirdgAfterCopy2.getName());
        System.out.println(dimirdgAfterCopy2.getCloneStates());
        System.out.println(dimirdgAfterCopy2.getOriginalState(CardStateName.Original).getName());
        System.out.println(dimirdgAfterCopy2.isFlipCard());
        System.out.println(dimirdgAfterCopy2.isFlipped());

        AssertJUnit.assertEquals("Runeclaw Bear", dimirdgAfterCopy2.getName());
        AssertJUnit.assertEquals(2, dimirdgAfterCopy2.getNetPower());
        AssertJUnit.assertEquals(2, dimirdgAfterCopy2.getNetToughness());
        AssertJUnit.assertTrue(dimirdgAfterCopy2.isFlipped());
        AssertJUnit.assertFalse(dimirdgAfterCopy2.getType().isLegendary());
    }

    @Test
    public void testStaticMultiPump() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        Card c1 = addCard("Creakwood Liege", p);
        Card c2 = addCard("Creakwood Liege", p);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);

        // update stats state
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertEquals(4, c1.getNetPower());
        AssertJUnit.assertEquals(4, c1.getNetToughness());

        AssertJUnit.assertEquals(4, c2.getNetPower());
        AssertJUnit.assertEquals(4, c2.getNetToughness());
    }

    @Test
    public void testPathtoExileActofTreason() {
        Game game = initAndCreateGame();
        Player p0 = game.getPlayers().get(0);
        Player p1 = game.getPlayers().get(1);
        Card serraAngel = addCardToZone("Serra Angel", p1, ZoneType.Battlefield);
        Card actOfTreason = addCardToZone("Act of Treason", p0, ZoneType.Hand);
        Card pathToExile = addCardToZone("Path to Exile", p0, ZoneType.Hand);
        addCards("Plateau", 4, p0);
        addCardToZone("Island", p1, ZoneType.Library);
        addCardToZone("Forest", p0, ZoneType.Library);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p0);
        game.getAction().checkStateEffects(true);

        GameSimulator sim = createSimulator(game, p0);
        Game simGame = sim.getSimulatedGameState();

        SpellAbility actSA = actOfTreason.getSpellAbilities().get(0);
        AssertJUnit.assertNotNull(actSA);
        actSA.setActivatingPlayer(p0);
        actSA.setTargetCard(serraAngel);
        sim.simulateSpellAbility(actSA);
        simGame.getAction().checkStateEffects(true);

        SpellAbility pathSA = pathToExile.getSpellAbilities().get(0);
        AssertJUnit.assertNotNull(pathSA);
        pathSA.setActivatingPlayer(p0);
        pathSA.setTargetCard(serraAngel);
        sim.simulateSpellAbility(pathSA);
        simGame.getAction().checkStateEffects(true);

        int numForest = countCardsWithName(simGame, "Forest");
        AssertJUnit.assertEquals(1, numForest);
        AssertJUnit.assertEquals(0, simGame.getPlayers().get(1).getCardsIn(ZoneType.Battlefield).size());
    }

    @Test
    public void testAmassTrigger() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        String WCname = "Woodland Champion";
        addCard(WCname, p);
        addCards("Island", 5, p);

        String CardName = "Eternal Skylord";
        Card c = addCardToZone(CardName, p, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility playSa = c.getSpellAbilities().get(0);
        playSa.setActivatingPlayer(p);

        GameSimulator sim = createSimulator(game, p);
        int origScore = sim.getScoreForOrigGame().value;
        int score = sim.simulateSpellAbility(playSa).value;
        AssertJUnit.assertTrue(String.format("score=%d vs. origScore=%d", score, origScore), score > origScore);

        Game simGame = sim.getSimulatedGameState();

        Card simWC = findCardWithName(simGame, WCname);

        AssertJUnit.assertEquals(1, simWC.getPowerBonusFromCounters());
        AssertJUnit.assertEquals(3, simGame.getPlayers().get(0).getCreaturesInPlay().size());
    }

    @Test
    public void testEverAfterWithWaywardServant() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        String everAfter = "Ever After";
        String waywardServant = "Wayward Servant";
        String goblin = "Raging Goblin";

        addCards("Swamp", 8, p);

        Card cardEverAfter = addCardToZone(everAfter, p, ZoneType.Hand);
        Card cardWaywardServant = addCardToZone(waywardServant, p, ZoneType.Graveyard);
        Card cardRagingGoblin = addCardToZone(goblin, p, ZoneType.Graveyard);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility playSa = cardEverAfter.getSpellAbilities().get(0);
        playSa.setActivatingPlayer(p);
        playSa.getTargets().add(cardWaywardServant);
        playSa.getTargets().add(cardRagingGoblin);

        GameSimulator sim = createSimulator(game, p);
        int origScore = sim.getScoreForOrigGame().value;
        int score = sim.simulateSpellAbility(playSa).value;
        AssertJUnit.assertTrue(String.format("score=%d vs. origScore=%d", score, origScore), score > origScore);

        Game simGame = sim.getSimulatedGameState();

        Card simGoblin = findCardWithName(simGame, goblin);

        simGame.getAction().checkStateEffects(true);
        simGame.getPhaseHandler().devAdvanceToPhase(PhaseType.MAIN2);

        AssertJUnit.assertEquals(21, simGame.getPlayers().get(0).getLife());
        AssertJUnit.assertTrue(simGoblin.isRed() && simGoblin.isBlack());
        AssertJUnit.assertTrue(simGoblin.getType().hasSubtype("Zombie"));
    }

    @Test
    public void testCantBePrevented() {
        String polukranosCardName = "Polukranos, Unchained";
        String hydraCardName = "Phyrexian Hydra";
        String leylineCardName = "Leyline of Punishment";

        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);

        Card polukranos = addCard(polukranosCardName, p);
        polukranos.addCounterInternal(CounterEnumType.P1P1, 6, p, false, null, null);
        addCard(hydraCardName, p);
        addCard(leylineCardName, p);
        for (int i = 0; i < 2; ++i) {
            addCard("Mountain", p);
        }
        Card pyroCard = addCardToZone("Pyroclasm", p, ZoneType.Hand);
        SpellAbility pyroSA = pyroCard.getFirstSpellAbility();

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        GameSimulator sim = createSimulator(game, p);
        sim.simulateSpellAbility(pyroSA);
        Game simGame = sim.getSimulatedGameState();
        Card simPolukranos = findCardWithName(simGame, polukranosCardName);
        Card simHydra = findCardWithName(simGame, hydraCardName);

        AssertJUnit.assertTrue(simPolukranos.hasCounters());
        AssertJUnit.assertEquals(4, simPolukranos.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals(2, simPolukranos.getDamage());

        AssertJUnit.assertFalse(simHydra.hasCounters());
        AssertJUnit.assertEquals(2, simHydra.getDamage());
    }

    @Test
    public void testAlphaBrawl() {
        Game game = initAndCreateGame();
        Player p1 = game.getPlayers().get(0);
        Player p2 = game.getPlayers().get(1);

        String nishobaName = "Phantom Nishoba";
        String capridorName = "Stormwild Capridor";
        String pridemateName = "Ajani's Pridemate";
        String indestructibilityName = "Indestructibility";
        String bearName = "Runeclaw Bear";
        String alphaBrawlName = "Alpha Brawl";

        // enough to cast Alpha Brawl
        addCards("Mountain", 8, p2);

        Card nishoba = addCard(nishobaName, p1);
        nishoba.addCounterInternal(CounterEnumType.P1P1, 7, p1, false, null, null);
        addCard(capridorName, p1);
        Card pridemate = addCard(pridemateName, p1);
        Card indestructibility = addCard(indestructibilityName, p1);
        indestructibility.attachToEntity(pridemate, null);
        addCard(bearName, p1);

        Card alphaBrawl = addCardToZone(alphaBrawlName, p2, ZoneType.Hand);
        SpellAbility alphaBrawlSA = alphaBrawl.getFirstSpellAbility();

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p2);
        game.getAction().checkStateEffects(true);

        GameSimulator sim = createSimulator(game, p2);
        alphaBrawlSA.setTargetCard(nishoba);
        sim.simulateSpellAbility(alphaBrawlSA);
        Game simGame = sim.getSimulatedGameState();
        Card simNishoba = findCardWithName(simGame, nishobaName);
        Card simCapridor = findCardWithName(simGame, capridorName);
        Card simPridemate = findCardWithName(simGame, pridemateName);
        Card simBear = findCardWithName(simGame, bearName);

        // bear is destroyed
        AssertJUnit.assertNull(simBear);

        AssertJUnit.assertNotNull(simNishoba);
        AssertJUnit.assertTrue(simNishoba.hasCounters());
        // Damage prevented and only 1 +1/+1 counter is removed
        AssertJUnit.assertEquals(0, simNishoba.getDamage());
        AssertJUnit.assertTrue(simNishoba.hasCounters());
        AssertJUnit.assertEquals(6, simNishoba.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals(6, simNishoba.getToughnessBonusFromCounters());
        AssertJUnit.assertEquals(6, simNishoba.getPowerBonusFromCounters());

        AssertJUnit.assertNotNull(simCapridor);
        // Damage prevented and that many +1/+1 counters are put
        AssertJUnit.assertEquals(0, simCapridor.getDamage());
        AssertJUnit.assertTrue(simCapridor.hasCounters());
        AssertJUnit.assertEquals(7, simCapridor.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals(7, simCapridor.getToughnessBonusFromCounters());
        AssertJUnit.assertEquals(7, simCapridor.getPowerBonusFromCounters());

        AssertJUnit.assertNotNull(simPridemate);
        AssertJUnit.assertEquals(7, simPridemate.getDamage());
        // Life gain only triggered once
        AssertJUnit.assertTrue(simPridemate.hasCounters());
        AssertJUnit.assertEquals(1, simPridemate.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals(1, simPridemate.getToughnessBonusFromCounters());
        AssertJUnit.assertEquals(1, simPridemate.getPowerBonusFromCounters());

        // 2 times 7 damage with life gain = 14 + 20 = 34 (damage to Stormwild Capridor
        // is prevented)
        Player simplayer1 = simGame.getPlayer(p1.getId());
        AssertJUnit.assertEquals(34, simplayer1.getLife());
    }

    @Test
    public void testGlarecaster() {
        String glarecasterName = "Glarecaster";

        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        Player p2 = game.getPlayers().get(1);

        Card glarecaster = addCard(glarecasterName, p);
        // enough to activate Glarecaster and cast Inferno
        addCards("Plains", 7, p);
        addCards("Mountain", 7, p);

        Card infernoCard = addCardToZone("Inferno", p, ZoneType.Hand);
        SpellAbility infernoSA = infernoCard.getFirstSpellAbility();

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility saGlarecaster = findSAWithPrefix(glarecaster, "{5}{W}");
        AssertJUnit.assertNotNull(saGlarecaster);
        saGlarecaster.getTargets().add(p2);

        GameSimulator sim = createSimulator(game, p);
        int score = sim.simulateSpellAbility(saGlarecaster).value;
        AssertJUnit.assertTrue(score > 0);
        sim.simulateSpellAbility(infernoSA);
        Game simGame = sim.getSimulatedGameState();
        Card simGlarecaster = findCardWithName(simGame, glarecasterName);

        AssertJUnit.assertNotNull(simGlarecaster);
        AssertJUnit.assertEquals(0, simGlarecaster.getDamage());

        // 6 * 3 = 18 damage are all dealt to p2
        AssertJUnit.assertEquals(20, simGame.getPlayers().get(0).getLife());
        AssertJUnit.assertEquals(2, simGame.getPlayers().get(1).getLife());
    }

    @Test
    public void testETBCounterMowu() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        //Player p2 = game.getPlayers().get(1);

        String grumName = "Grumgully, the Generous";
        String mowuName = "Mowu, Loyal Companion";

        addCard(grumName, p);
        Card mowu = addCardToZone(mowuName, p, ZoneType.Hand);

        addCards("Forest", 7, p);
        SpellAbility mowuSA = mowu.getFirstSpellAbility();

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);

        GameSimulator sim = createSimulator(game, p);
        sim.simulateSpellAbility(mowuSA);
        Game simGame = sim.getSimulatedGameState();

        Card simMowu = findCardWithName(simGame, mowuName);

        AssertJUnit.assertNotNull(simMowu);
        AssertJUnit.assertEquals(2, simMowu.getCounters(CounterEnumType.P1P1));
    }

    @Test
    public void testETBCounterCorpsejack() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        //Player p2 = game.getPlayers().get(1);

        String grumName = "Grumgully, the Generous";
        String corpsejackName = "Corpsejack Menace";

        addCard(grumName, p);
        Card corpsejack = addCardToZone(corpsejackName, p, ZoneType.Hand);

        addCards("Forest", 7, p);
        addCards("Swamp", 7, p);
        SpellAbility corpsejackSA = corpsejack.getFirstSpellAbility();

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);

        GameSimulator sim = createSimulator(game, p);
        sim.simulateSpellAbility(corpsejackSA);
        Game simGame = sim.getSimulatedGameState();

        Card simCorpsejack = findCardWithName(simGame, corpsejackName);

        AssertJUnit.assertNotNull(simCorpsejack);
        AssertJUnit.assertEquals(1, simCorpsejack.getCounters(CounterEnumType.P1P1));
    }

    @Test
    public void testETBCounterCorpsejackMentor() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        //Player p2 = game.getPlayers().get(1);

        String grumName = "Grumgully, the Generous";
        String corpsejackName = "Corpsejack Menace";
        String mentorName = "Conclave Mentor";
        String everAfterName = "Ever After";

        addCard(grumName, p);
        Card corpsejack = addCardToZone(corpsejackName, p, ZoneType.Graveyard);
        Card mentor = addCardToZone(mentorName, p, ZoneType.Graveyard);

        Card everAfter = addCardToZone(everAfterName, p, ZoneType.Hand);

        addCards("Swamp", 7, p);
        SpellAbility everSA = everAfter.getFirstSpellAbility();
        everSA.getTargets().add(corpsejack);
        everSA.getTargets().add(mentor);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);

        GameSimulator sim = createSimulator(game, p);
        sim.simulateSpellAbility(everSA);
        Game simGame = sim.getSimulatedGameState();

        Card simCorpsejack = findCardWithName(simGame, corpsejackName);
        Card simMentor = findCardWithName(simGame, mentorName);

        AssertJUnit.assertNotNull(simCorpsejack);
        AssertJUnit.assertEquals(1, simCorpsejack.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertNotNull(simMentor);
        AssertJUnit.assertEquals(1, simMentor.getCounters(CounterEnumType.P1P1));
    }

    @Test
    public void testHushbringer() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);

        addCard("Naban, Dean of Iteration", p);
        addCard("Hushbringer", p);
        addCard("Ingenious Artillerist", p);

        // both ETB together and Artillerist should still trigger from the non-creature
        addCardToZone("Spellbook", p, ZoneType.Library);
        // whereas Naban doesn't see Memnarch to double the trigger
        addCardToZone("Memnarch", p, ZoneType.Library);

        addCards("Forest", 2, p);
        addCards("Island", 3, p);
        addCards("Mountain", 2, p);

        Card genesis = addCardToZone("Genesis Ultimatum", p, ZoneType.Hand);

        SpellAbility genesisSA = genesis.getFirstSpellAbility();

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);

        GameSimulator sim = createSimulator(game, p);
        sim.simulateSpellAbility(genesisSA);
        Game simGame = sim.getSimulatedGameState();

        // 2 damage dealt for 2 artifacts
        AssertJUnit.assertEquals(18, simGame.getPlayers().get(1).getLife());
    }

    @Test
    public void testLKITransformableTokenCopy() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);

        String untransformedName = "Heliod, the Radiant Dawn";
        String transformedName = "Heliod, the Warped Eclipse";

        addCard("Ratadrabik of Urborg", p);
        Card heliod = addCard(untransformedName, p);

        addCards("Island", 4, p);
        addCards("Swamp", 3, p);

        Card murder = addCardToZone("Murder", p, ZoneType.Hand);
        SpellAbility murderSA = murder.getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);

        SpellAbility transformSA = findSAWithPrefix(heliod, "{3}{U/P}: Transform");

        GameSimulator sim = createSimulator(game, p);
        AssertJUnit.assertNotNull(transformSA);
        sim.simulateSpellAbility(transformSA);

        Game simGame = sim.getSimulatedGameState();

        Card transformedHeliod = findCardWithName(simGame, transformedName);
        AssertJUnit.assertNotNull(transformedHeliod);
        murderSA.getTargets().add(transformedHeliod);

        sim.simulateSpellAbility(murderSA);
        simGame = sim.getSimulatedGameState();

        Card transformedHeliodToken = findCardWithName(simGame, transformedName);
        AssertJUnit.assertNotNull(transformedHeliodToken);
        AssertJUnit.assertTrue(transformedHeliodToken.isToken());
        AssertJUnit.assertTrue(transformedHeliodToken.isTransformable());
        AssertJUnit.assertTrue(transformedHeliodToken.isTransformed());
        AssertJUnit.assertTrue(transformedHeliodToken.isBackSide());
    }

    @Test
    public void testBasicSpellFizzling() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);

        addCardToZone("Swamp", p, ZoneType.Library);
        Card bear = addCard("Bear Cub", p);

        addCards("Swamp", 5, p);
        Card destroy = addCardToZone("Annihilate", p, ZoneType.Hand);
        SpellAbility destroySA = destroy.getFirstSpellAbility();
        destroySA.getTargets().add(bear);

        addCards("Island", 2, p);
        Card fizzle = addCardToZone("Mage's Guile", p, ZoneType.Hand);
        SpellAbility fizzleSA = fizzle.getFirstSpellAbility();
        fizzleSA.getTargets().add(bear);

        GameSimulator sim = createSimulator(game, p);
        game = sim.getSimulatedGameState();

        sim.simulateSpellAbility(destroySA, false);
        AssertJUnit.assertEquals(1, game.getStackZone().size());
        sim.simulateSpellAbility(fizzleSA);

        // spell should fizzle so no card was drawn
        AssertJUnit.assertEquals(0, game.getPlayers().get(0).getCardsIn(ZoneType.Hand).size());
    }

    @Test
    public void testControlLayerDependency() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        Player opp = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);

        Card bear = addCard("Bear Cub", p);
        addCards("Island", 6, p);
        addCards("Island", 5, opp);
        addCard("Vedalken Orrery", opp);
        Card control = addCardToZone("Mind Control", opp, ZoneType.Hand);

        GameSimulator sim = createSimulator(game, opp);
        game = sim.getSimulatedGameState();

        SpellAbility controlSA = control.getFirstSpellAbility();
        controlSA.getTargets().add(bear);
        sim.simulateSpellAbility(controlSA);

        p = game.getPlayers().get(0);
        Card confiscate = addCardToZone("Confiscate", p, ZoneType.Hand);
        control = findCardWithName(game, "Mind Control");
        SpellAbility confiscateSA = confiscate.getFirstSpellAbility();
        confiscateSA.getTargets().add(control);

        sim = createSimulator(game, p);
        game = sim.getSimulatedGameState();
        bear = findCardWithName(game, "Bear Cub");

        AssertJUnit.assertTrue(bear.getController().equals(opp));
        sim.simulateSpellAbility(confiscateSA);
        AssertJUnit.assertTrue(bear.getController().equals(p));
    }

    @Test
    public void testTypeLayerDependency() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);

        Card nonBasicForest = addCard("Breeding Pool", p);
        addCard("Life and Limb", p);
        addCard("Blood Moon", p);

        game.getAction().checkStaticAbilities();

        // Blood Moon will be applied first because Life and Limb depends on it
        AssertJUnit.assertFalse(nonBasicForest.isCreature());
        AssertJUnit.assertTrue(nonBasicForest.getType().hasSubtype("Mountain"));

        // adding Saproling causes dependency loop, so Life and Limb gets applied first instead
        addCard("Shroofus Sproutsire", p);

        game.getAction().checkStaticAbilities();

        AssertJUnit.assertTrue(nonBasicForest.isCreature());
        AssertJUnit.assertTrue(nonBasicForest.getType().hasSubtype("Mountain"));
    }

    @Test
    public void testHenzie() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);

        addCard("Henzie \"Toolbox\" Torre", p);
        addCardToZone("Wastes", p, ZoneType.Library);
        addCards("Plains", 5, p);
        Card spell = addCardToZone("Serra Angel", p, ZoneType.Hand);

        game.getAction().checkStaticAbilities();
        List<SpellAbility> sas = spell.getAllPossibleAbilities(p, true);
        SpellAbility blitz = sas.get(1);

        GameSimulator sim = createSimulator(game, p);
        game = sim.getSimulatedGameState();
        sim.simulateSpellAbility(blitz);
        spell = findCardWithName(game, "Serra Angel");

        AssertJUnit.assertEquals(1, spell.getAmountOfKeyword(Keyword.BLITZ));
        AssertJUnit.assertTrue(spell.hasKeyword(Keyword.HASTE));

        playUntilNextTurn(game);

        AssertJUnit.assertEquals(1, game.getPlayers().get(0).getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertTrue(spell.isInZone(ZoneType.Graveyard));
    }

    /**
     * Test for "Volo's Journal" usage by the AI. This test checks if the AI correctly
     * adds the correct types to the "Volo's Journal" when casting the spells in order
     * and makes sure the entries are unique.
     */
    @Test
    public void testVoloJournal() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCards("Island", 7, p);
        addCards("Mountain", 2, p);
        addCards("Forest", 2, p);

        Card c = addCardToZone("Volo, Itinerant Scholar", p, ZoneType.Hand);
        Card[] cards = {
                addCardToZone("Cathartic Adept", p, ZoneType.Hand),
                addCardToZone("Cathartic Adept", p, ZoneType.Hand),
                addCardToZone("Drowner Initiate", p, ZoneType.Hand),
                addCardToZone("Atog", p, ZoneType.Hand),
                addCardToZone("Atog", p, ZoneType.Hand)
        };

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility playVoloSA = c.getFirstSpellAbility();
        playVoloSA.setActivatingPlayer(p);

        GameSimulator sim = createSimulator(game, p);
        sim.simulateSpellAbility(playVoloSA);
        Game simGame = sim.getSimulatedGameState();

        for (Card card : cards) {
            SpellAbility a1 = card.getSpellAbilities().get(0);
            a1.setActivatingPlayer(p);
            sim.simulateSpellAbility(a1);
        }

        Player simP = simGame.getPlayer(p.getId());
        CardCollectionView btlf = simP.getCardsIn(ZoneType.Battlefield);
        List<String> words = List.of(new String[]{"Human", "Wizard", "Atog", "Merfolk"});

        for (Card card : btlf) {
            if (card.getName().equals("Volo's Journal")) {
                // All words are present in the iterable
                AssertJUnit.assertTrue(areWordsInIterable(words, card.getNotedTypes()));
            }
        }
    }

    /**
     * Helper method to check if all words in the given list are present in the iterable and unique.
     *
     * @param words    The list of words to check for.
     * @param iterable The iterable to check against.
     * @return true if all words are present in the iterable, false otherwise.
     */
    protected boolean areWordsInIterable(List<String> words, Iterable<String> iterable) {
        // Create a frequency map for the words in the iterable
        Map<String, Integer> frequencyMap = new HashMap<>();
        for (String item : iterable) {
            frequencyMap.put(item, frequencyMap.getOrDefault(item, 0) + 1);
        }

        // Check if each word in the list appears exactly once
        for (String word : words) {
            if (frequencyMap.getOrDefault(word, 0) != 1) {
                return false;  // If the word doesn't appear exactly once, return false
            }
        }

        return true;  // All words appear exactly once
    }

}
