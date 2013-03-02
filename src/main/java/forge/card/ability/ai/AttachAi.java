package forge.card.ability.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import forge.Card;
import forge.CardLists;
import forge.CardPredicates;
import forge.CardUtil;
import forge.Singletons;
import forge.CardPredicates.Presets;
import forge.card.ability.AbilityUtils;
import forge.card.ability.ApiType;
import forge.card.ability.SpellAbilityAi;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.card.staticability.StaticAbility;
import forge.game.ai.ComputerUtilCard;
import forge.game.ai.ComputerUtilCost;
import forge.game.ai.ComputerUtilMana;
import forge.game.phase.CombatUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

public class AttachAi extends SpellAbilityAi {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(AIPlayer ai, SpellAbility sa) {
        final Random r = MyRandom.getRandom();
        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getSourceCard();

        if (abCost != null) {
            // AI currently disabled for these costs
            if (!ComputerUtilCost.checkSacrificeCost(ai, abCost, source)) {
                return false;
            }
            if (!ComputerUtilCost.checkLifeCost(ai, abCost, source, 4, null)) {
                return false;
            }
        }

        // prevent run-away activations - first time will always return true
        final boolean chance = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

        // Attach spells always have a target
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgt.resetTargets();
            if (!attachPreference(sa, tgt, false)) {
                return false;
            }
        }

        if (abCost.getTotalMana().countX() > 0 && source.getSVar("X").equals("Count$xPaid")) {
            // Set PayX here to maximum value. (Endless Scream and Venarian
            // Gold)
            final int xPay = ComputerUtilMana.determineLeftoverMana(sa, ai);

            if (xPay == 0) {
                return false;
            }

            source.setSVar("PayX", Integer.toString(xPay));
        }

        if (Singletons.getModel().getGame().getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)
                && !"Curse".equals(sa.getParam("AILogic"))) {
            return false;
        }

        return chance;
    }

    /**
     * Acceptable choice.
     * 
     * @param c
     *            the c
     * @param mandatory
     *            the mandatory
     * @return the card
     */
    private static Card acceptableChoice(final Card c, final boolean mandatory) {
        if (mandatory) {
            return c;
        }

        // TODO If Not Mandatory, make sure the card is "good enough"
        if (c.isCreature()) {
            final int eval = ComputerUtilCard.evaluateCreature(c);
            if (eval < 130) {
                return null;
            }
        }

        return c;
    }

    /**
     * Choose unpreferred.
     * 
     * @param mandatory
     *            the mandatory
     * @param list
     *            the list
     * @return the card
     */
    private static Card chooseUnpreferred(final boolean mandatory, final List<Card> list) {
        if (!mandatory) {
            return null;
        }

        return ComputerUtilCard.getWorstPermanentAI(list, true, true, true, false);
    }

    /**
     * Choose less preferred.
     * 
     * @param mandatory
     *            the mandatory
     * @param list
     *            the list
     * @return the card
     */
    private static Card chooseLessPreferred(final boolean mandatory, final List<Card> list) {
        if (!mandatory) {
            return null;
        }

        return ComputerUtilCard.getBestAI(list);
    }

    /**
     * Attach ai change type preference.
     * 
     * @param sa
     *            the sa
     * @param list
     *            the list
     * @param mandatory
     *            the mandatory
     * @param attachSource
     *            the attach source
     * @return the card
     */
    private static Card attachAIChangeTypePreference(final SpellAbility sa, List<Card> list, final boolean mandatory,
            final Card attachSource) {
        // AI For Cards like Evil Presence or Spreading Seas

        String type = "";

        for (final StaticAbility stAb : attachSource.getStaticAbilities()) {
            final HashMap<String, String> stab = stAb.getMapParams();
            if (stab.get("Mode").equals("Continuous") && stab.containsKey("AddType")) {
                type = stab.get("AddType");
            }
        }

        if ("ChosenType".equals(type)) {
            // TODO ChosenTypeEffect should have exact same logic that's here
            // For now, Island is as good as any for a default value
            type = "Island";
        }

        list = CardLists.getNotType(list, type); // Filter out Basic Lands that have the
                                      // same type as the changing type

        final Card c = ComputerUtilCard.getBestAI(list);

        // TODO Port over some of the existing code, but rewrite most of it.
        // Ultimately, these spells need to be used to reduce mana base of a
        // color. So it might be better to choose a Basic over a Nonbasic
        // Although a nonbasic card with a nasty ability, might be worth it to
        // cast on

        if (c == null) {
            return chooseLessPreferred(mandatory, list);
        }

        return acceptableChoice(c, mandatory);
    }

    /**
     * Attach ai keep tapped preference.
     * 
     * @param sa
     *            the sa
     * @param list
     *            the list
     * @param mandatory
     *            the mandatory
     * @param attachSource
     *            the attach source
     * @return the card
     */
    private static Card attachAIKeepTappedPreference(final SpellAbility sa, final List<Card> list, final boolean mandatory, final Card attachSource) {
        // AI For Cards like Paralyzing Grasp and Glimmerdust Nap
        final List<Card> prefList = CardLists.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                // Don't do Untapped Vigilance cards
                if (c.isCreature() && c.hasKeyword("Vigilance") && c.isUntapped()) {
                    return false;
                }

                if (!c.isEnchanted()) {
                    return true;
                }

                final ArrayList<Card> auras = c.getEnchantedBy();
                final Iterator<Card> itr = auras.iterator();
                while (itr.hasNext()) {
                    final Card aura = itr.next();
                    SpellAbility auraSA = aura.getSpells().get(0);
                    if (auraSA.getApi() == ApiType.Attach) {
                        if ("KeepTapped".equals(auraSA.getParam("AILogic"))) {
                            // Don't attach multiple KeepTapped Auras to one
                            // card
                            return false;
                        }
                    }
                }

                return true;
            }
        });

        final Card c = ComputerUtilCard.getBestAI(prefList);

        if (c == null) {
            return chooseLessPreferred(mandatory, list);
        }

        return acceptableChoice(c, mandatory);
    }

    /**
     * Attach to player ai preferences.
     * 
     * @param af
     *            the af
     * @param sa
     *            the sa
     * @param mandatory
     *            the mandatory
     * @return the player
     */
    private static Player attachToPlayerAIPreferences(final Player aiPlayer, final SpellAbility sa,
            final boolean mandatory) {
        List<Player> targetable = new ArrayList<Player>();
        for (final Player player : Singletons.getModel().getGame().getPlayers()) {
            if (sa.canTarget(player)) {
                targetable.add(player);
            }
        }

        if ("Curse".equals(sa.getParam("AILogic"))) {
            if (!mandatory) {
                targetable.removeAll(aiPlayer.getAllies());
                targetable.remove(aiPlayer);
            }
            if (targetable.size() > 0) {
                // first try get weakest opponent to reduce opponents faster
                if (targetable.contains(aiPlayer.getWeakestOpponent())) {
                    return aiPlayer.getWeakestOpponent();
                } else {
                    // then try any other opponent
                    for (final Player curseChoice : targetable) {
                        if (curseChoice.isOpponentOf(aiPlayer)) {
                            return curseChoice;
                        }
                    }
                    // only reaches here if no preferred targets are targetable and sa is mandatory
                    return targetable.get(0);
                }
            }
        } else {
            if (!mandatory) {
                targetable.removeAll(aiPlayer.getOpponents());
            }
            if (targetable.size() > 0) {
                // first try self
                if (targetable.contains(aiPlayer)) {
                    return aiPlayer;
                } else {
                    // then try allies
                    for (final Player boonChoice : targetable) {
                        if (!boonChoice.isOpponentOf(aiPlayer)) {
                            return boonChoice;
                        }
                    }
                    // only reaches here if no preferred choices are targetable and sa is mandatory
                    return targetable.get(0);
                }
            }
        }

        return null;
    }

    /**
     * Attach ai control preference.
     * 
     * @param sa
     *            the sa
     * @param list
     *            the list
     * @param mandatory
     *            the mandatory
     * @param attachSource
     *            the attach source
     * @return the card
     */
    private static Card attachAIAnimatePreference(final SpellAbility sa, final List<Card> list, final boolean mandatory,
            final Card attachSource) {
        // AI For choosing a Card to Animate.
        List<Card> betterList = CardLists.getNotType(list, "Creature");
        if (sa.getSourceCard().getName().equals("Animate Artifact")) {
            betterList = CardLists.filter(betterList, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return c.getCMC() > 0;
                }
            });
        }

        final Card c = ComputerUtilCard.getMostExpensivePermanentAI(betterList);

        // If Mandatory (brought directly into play without casting) gotta
        // choose something
        if (c == null && mandatory) {
            return chooseLessPreferred(mandatory, list);
        }

        return c;
    }

    /**
     * Attach ai control preference.
     * 
     * @param sa
     *            the sa
     * @param list
     *            the list
     * @param mandatory
     *            the mandatory
     * @param attachSource
     *            the attach source
     * @return the card
     */
    private static Card attachAIReanimatePreference(final SpellAbility sa, final List<Card> list, final boolean mandatory,
            final Card attachSource) {
        // AI For choosing a Card to Animate.
        // TODO Add some more restrictions for Reanimation Auras
        final Card c = ComputerUtilCard.getBestCreatureAI(list);

        // If Mandatory (brought directly into play without casting) gotta
        // choose something
        if (c == null && mandatory) {
            return chooseLessPreferred(mandatory, list);
        }

        return c;
    }

    /**
     * Attach ai control preference.
     * 
     * @param sa
     *            the sa
     * @param list
     *            the list
     * @param mandatory
     *            the mandatory
     * @param attachSource
     *            the attach source
     * @return the card
     */
    private static Card attachAISpecificCardPreference(final SpellAbility sa, final List<Card> list, final boolean mandatory,
            final Card attachSource) {
        // I know this isn't much better than Hardcoding, but some cards need it for now
        Player ai = sa.getActivatingPlayer();
        Card chosen = null;
        if ("Guilty Conscience".equals(sa.getSourceCard().getName())) {
            List<Card> aiStuffies = CardLists.filter(list, Predicates.and(CardPredicates.isController(ai), CardPredicates.nameEquals("Stuffy Doll")));
            if (!aiStuffies.isEmpty()) {
                chosen = aiStuffies.get(0);
            } else {
                List<Card> creatures = CardLists.filterControlledBy(list, ai.getOpponents());
                creatures = CardLists.filter(creatures, new Predicate<Card>() {
                    @Override
                    public boolean apply(final Card c) {
                        // Don't enchant creatures that can survive
                        if (!c.canBeDestroyed() || c.getNetCombatDamage() < c.getNetDefense()) {
                            return false;
                        }
                        return true;
                    }
                });
                chosen = ComputerUtilCard.getBestCreatureAI(creatures);
            }
        }

        // If Mandatory (brought directly into play without casting) gotta
        // choose something
        if (chosen == null && mandatory) {
            return chooseLessPreferred(mandatory, list);
        }

        return chosen;
    }

    // Should generalize this code a bit since they all have similar structures
    /**
     * Attach ai control preference.
     * 
     * @param sa
     *            the sa
     * @param list
     *            the list
     * @param mandatory
     *            the mandatory
     * @param attachSource
     *            the attach source
     * @return the card
     */
    private static Card attachAIControlPreference(final SpellAbility sa, final List<Card> list, final boolean mandatory,
            final Card attachSource) {
        // AI For choosing a Card to Gain Control of.

        if (sa.getTarget().canTgtPermanent()) {
            // If can target all Permanents, and Life isn't in eminent danger,
            // grab Planeswalker first, then Creature
            // if Life < 5 grab Creature first, then Planeswalker. Lands,
            // Enchantments and Artifacts are probably "not good enough"

        }

        final Card c = ComputerUtilCard.getBestAI(list);

        // If Mandatory (brought directly into play without casting) gotta
        // choose something
        if (c == null) {
            return chooseLessPreferred(mandatory, list);
        }

        return acceptableChoice(c, mandatory);
    }

    /**
     * Attach ai curse preference.
     * 
     * @param sa
     *            the sa
     * @param list
     *            the list
     * @param mandatory
     *            the mandatory
     * @param attachSource
     *            the attach source
     * @return the card
     */
    private static Card attachAICursePreference(final SpellAbility sa, final List<Card> list, final boolean mandatory,
            final Card attachSource) {
        // AI For choosing a Card to Curse of.

        // TODO Figure out some way to combine The "gathering of data" from
        // statics used in both Pump and Curse
        String stCheck = null;
        if (attachSource.isAura()) {
            stCheck = "EnchantedBy";
        } else if (attachSource.isEquipment()) {
            stCheck = "EquippedBy";
        }

        int totToughness = 0;
        int totPower = 0;
        final ArrayList<String> keywords = new ArrayList<String>();

        for (final StaticAbility stAbility : attachSource.getStaticAbilities()) {
            final Map<String, String> stabMap = stAbility.getMapParams();

            if (!stabMap.get("Mode").equals("Continuous")) {
                continue;
            }

            final String affected = stabMap.get("Affected");

            if (affected == null) {
                continue;
            }
            if ((affected.contains(stCheck) || affected.contains("AttachedBy"))) {
                totToughness += AttachAi.parseSVar(attachSource, stabMap.get("AddToughness"));
                totPower += AttachAi.parseSVar(attachSource, stabMap.get("AddPower"));

                String kws = stabMap.get("AddKeyword");
                if (kws != null) {
                    for (final String kw : kws.split(" & ")) {
                        keywords.add(kw);
                    }
                }
                kws = stabMap.get("AddHiddenKeyword");
                if (kws != null) {
                    for (final String kw : kws.split(" & ")) {
                        keywords.add(kw);
                    }
                }
            }
        }

        List<Card> prefList = null;
        if (totToughness < 0) {
            // Kill a creature if we can
            final int tgh = totToughness;
            prefList = CardLists.filter(list, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    if (!c.hasKeyword("Indestructible") && (c.getLethalDamage() <= Math.abs(tgh))) {
                        return true;
                    }

                    return c.getNetDefense() <= Math.abs(tgh);
                }
            });
        }

        Card c = null;
        if ((prefList == null) || prefList.isEmpty()) {
            prefList = new ArrayList<Card>(list);
        } else {
            c = ComputerUtilCard.getBestAI(prefList);
            if (c != null) {
                return c;
            }
        }

        if (!keywords.isEmpty()) {
            prefList = CardLists.filter(prefList, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return containsUsefulCurseKeyword(keywords, c, sa);
                }
            });
        } else if (totPower < 0) {
            prefList = CardLists.filter(prefList, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return CombatUtil.canAttackNextTurn(c) && c.getNetAttack() > 0;
                }
            });
        }

        c = ComputerUtilCard.getBestAI(prefList);

        if (c == null) {
            return chooseLessPreferred(mandatory, list);
        }

        return acceptableChoice(c, mandatory);
    }


    /**
     * Attach do trigger ai.
     * @param sa
     *            the sa
     * @param mandatory
     *            the mandatory
     * @param af
     *            the af
     * 
     * @return true, if successful
     */
    @Override
    protected boolean doTriggerAINoCost(final AIPlayer ai, final SpellAbility sa, final boolean mandatory) {
        final Card card = sa.getSourceCard();
        // Check if there are any valid targets
        ArrayList<Object> targets = new ArrayList<Object>();
        final Target tgt = sa.getTarget();
        if (tgt == null) {
            targets = AbilityUtils.getDefinedObjects(sa.getSourceCard(), sa.getParam("Defined"), sa);
        } else {
            AttachAi.attachPreference(sa, tgt, mandatory);
        }

        if (!mandatory && card.isEquipment() && !targets.isEmpty()) {
            Card newTarget = (Card) targets.get(0);
            //don't equip human creatures
            if (newTarget.getController().isOpponentOf(ai)) {
                return false;
            }

            //don't equip a worse creature
            if (card.isEquipping()) {
                Card oldTarget = card.getEquipping().get(0);
                if (ComputerUtilCard.evaluateCreature(oldTarget) > ComputerUtilCard.evaluateCreature(newTarget)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * parseSVar TODO - flesh out javadoc for this method.
     * 
     * @param hostCard
     *            the Card with the SVar on it
     * @param amount
     *            a String
     * @return the calculated number
     */
    public static int parseSVar(final Card hostCard, final String amount) {
        int num = 0;
        if (amount == null) {
            return num;
        }
    
        try {
            num = Integer.valueOf(amount);
        } catch (final NumberFormatException e) {
            num = CardFactoryUtil.xCount(hostCard, hostCard.getSVar(amount).split("\\$")[1]);
        }
    
        return num;
    }

    /**
     * Attach preference.
     * 
     * @param af
     *            the af
     * @param sa
     *            the sa
     * @param sa
     *            the sa
     * @param tgt
     *            the tgt
     * @param mandatory
     *            the mandatory
     * @return true, if successful
     */
    public static boolean attachPreference(final SpellAbility sa, final Target tgt, final boolean mandatory) {
        Object o;
        if (tgt.canTgtPlayer()) {
            o = attachToPlayerAIPreferences(sa.getActivatingPlayer(), sa, mandatory);
        } else {
            o = attachToCardAIPreferences(sa.getActivatingPlayer(), sa, mandatory);
        }

        if (o == null) {
            return false;
        }

        tgt.addTarget(o);
        return true;
    }

    /**
     * Attach ai pump preference.
     * 
     * @param sa
     *            the sa
     * @param list
     *            the list
     * @param mandatory
     *            the mandatory
     * @param attachSource
     *            the attach source
     * @return the card
     */
    private static Card attachAIPumpPreference(final Player ai, final SpellAbility sa, final List<Card> list, final boolean mandatory,
            final Card attachSource) {
        // AI For choosing a Card to Pump
        Card c = null;
        List<Card> magnetList = null;
        String stCheck = null;
        if (attachSource.isAura()) {
            stCheck = "EnchantedBy";
            magnetList = CardLists.filter(list, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    if (!c.isCreature()) {
                        return false;
                    }
                    String sVar = c.getSVar("EnchantMe");
                    return sVar.equals("Multiple") || (sVar.equals("Once") && !c.isEnchanted());
                }
            });
        } else if (attachSource.isEquipment()) {
            stCheck = "EquippedBy";
            magnetList = CardLists.filter(list, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    if (!c.isCreature()) {
                        return false;
                    }
                    String sVar = c.getSVar("EquipMe");
                    return sVar.equals("Multiple") || (sVar.equals("Once") && !c.isEquipped());
                }
            });
        }

        if ((magnetList != null) && !magnetList.isEmpty()) {
            // Always choose something from the Magnet List.
            // Probably want to "weight" the list by amount of Enchantments and
            // choose the "lightest"

            magnetList = CardLists.filter(magnetList, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return CombatUtil.canAttack(c, ai.getWeakestOpponent());
                }
            });

            return ComputerUtilCard.getBestAI(magnetList);
        }

        int totToughness = 0;
        int totPower = 0;
        final ArrayList<String> keywords = new ArrayList<String>();
        boolean grantingAbilities = false;

        for (final StaticAbility stAbility : attachSource.getStaticAbilities()) {
            final Map<String, String> stabMap = stAbility.getMapParams();

            if (!stabMap.get("Mode").equals("Continuous")) {
                continue;
            }

            final String affected = stabMap.get("Affected");

            if (affected == null) {
                continue;
            }
            if ((affected.contains(stCheck) || affected.contains("AttachedBy"))) {
                totToughness += AttachAi.parseSVar(attachSource, stabMap.get("AddToughness"));
                totPower += AttachAi.parseSVar(attachSource, stabMap.get("AddPower"));

                grantingAbilities |= stabMap.containsKey("AddAbility");

                String kws = stabMap.get("AddKeyword");
                if (kws != null) {
                    for (final String kw : kws.split(" & ")) {
                        keywords.add(kw);
                    }
                }
                kws = stabMap.get("AddHiddenKeyword");
                if (kws != null) {
                    for (final String kw : kws.split(" & ")) {
                        keywords.add(kw);
                    }
                }
            }
        }

        List<Card> prefList = new ArrayList<Card>(list);
        if (totToughness < 0) {
            // Don't kill my own stuff with Negative toughness Auras
            final int tgh = totToughness;
            prefList = CardLists.filter(prefList, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return c.getLethalDamage() > Math.abs(tgh);
                }
            });
        }

        //only add useful keywords unless P/T bonus is significant
        if (totToughness + totPower < 4 && !keywords.isEmpty()) {
            prefList = CardLists.filter(prefList, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return containsUsefulKeyword(keywords, c, sa);
                }
            });
        }

        // Don't pump cards that will die.
        prefList = CardLists.filter(prefList, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return !c.getSVar("Targeting").equals("Dies");
            }
        });

        if (attachSource.isAura()) {
            // TODO For Auras like Rancor, that aren't as likely to lead to
            // card disadvantage, this check should be skipped
            prefList = CardLists.filter(prefList, Predicates.not(Presets.ENCHANTED));
        }

        if (!grantingAbilities && keywords.isEmpty()) {
            // Probably prefer to Enchant Creatures that Can Attack
            // Filter out creatures that can't Attack or have Defender
            prefList = CardLists.filter(prefList, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return !c.isCreature() || CombatUtil.canAttackNextTurn(c);
                }
            });
            c = ComputerUtilCard.getBestAI(prefList);
        } else {
            // If we grant abilities, we may want to put it on something Weak?
            // Possibly more defensive?
            c = ComputerUtilCard.getWorstPermanentAI(prefList, false, false, false, false);
        }

        if (c == null) {
            return chooseLessPreferred(mandatory, list);
        }

        return c;
    }

    /**
     * Attach to card ai preferences.
     * 
     * @param sa
     *            the sa
     * @param sa
     *            the sa
     * @param mandatory
     *            the mandatory
     * @return the card
     */
    private static Card attachToCardAIPreferences(final Player aiPlayer, final SpellAbility sa, final boolean mandatory) {
        final Target tgt = sa.getTarget();
        final Card attachSource = sa.getSourceCard();
        // TODO AttachSource is currently set for the Source of the Spell, but
        // at some point can support attaching a different card

        // Don't equip if already equipping
        if (attachSource.getEquippingCard() != null && attachSource.getEquippingCard().getController() == aiPlayer) {
            return null;
        }

        List<Card> list = Singletons.getModel().getGame().getCardsIn(tgt.getZone());
        list = CardLists.getValidCards(list, tgt.getValidTgts(), sa.getActivatingPlayer(), attachSource);

        // TODO If Attaching without casting, don't need to actually target.
        // I believe this is the only case where mandatory will be true, so just
        // check that when starting that work
        // But we shouldn't attach to things with Protection
        if (tgt.getZone().contains(ZoneType.Battlefield) && !mandatory) {
            list = CardLists.getTargetableCards(list, sa);
        } else {
            list = CardLists.filter(list, Predicates.not(CardPredicates.isProtectedFrom(attachSource)));
        }

        if (list.isEmpty()) {
            return null;
        }
        List<Card> prefList = list;
        if (sa.hasParam("AITgts")) {
            prefList = CardLists.getValidCards(list, sa.getParam("AITgts"), sa.getActivatingPlayer(), attachSource);
        }

        Card c = attachGeneralAI(aiPlayer, sa, prefList, mandatory, attachSource, sa.getParam("AILogic"));

        if ((c == null) && mandatory) {
            CardLists.shuffle(list);
            c = list.get(0);
        }

        return c;
    }

    /**
     * Attach general ai.
     * 
     * @param sa
     *            the sa
     * @param list
     *            the list
     * @param mandatory
     *            the mandatory
     * @param attachSource
     *            the attach source
     * @param logic
     *            the logic
     * @return the card
     */
    private static Card attachGeneralAI(final Player ai, final SpellAbility sa, final List<Card> list, final boolean mandatory,
            final Card attachSource, final String logic) {
        Player prefPlayer = ai.getWeakestOpponent();
        if ("Pump".equals(logic) || "Animate".equals(logic)) {
            prefPlayer = ai;
        }
        // Some ChangeType cards are beneficial, and PrefPlayer should be
        // changed to represent that
        final List<Card> prefList;

        if ("Reanimate".equals(logic) || "SpecificCard".equals(logic)) {
            // Reanimate or SpecificCard aren't so restrictive
            prefList = list;
        } else {
            prefList = CardLists.filterControlledBy(list, prefPlayer);
        }

        // If there are no preferred cards, and not mandatory bail out
        if (prefList.size() == 0) {
            return chooseUnpreferred(mandatory, list);
        }

        // Preferred list has at least one card in it to make to the actual
        // Logic
        Card c = null;
        if ("GainControl".equals(logic)) {
            c = attachAIControlPreference(sa, prefList, mandatory, attachSource);
        } else if ("Curse".equals(logic)) {
            c = attachAICursePreference(sa, prefList, mandatory, attachSource);
        } else if ("Pump".equals(logic)) {
            c = attachAIPumpPreference(ai, sa, prefList, mandatory, attachSource);
        } else if ("ChangeType".equals(logic)) {
            c = attachAIChangeTypePreference(sa, prefList, mandatory, attachSource);
        } else if ("KeepTapped".equals(logic)) {
            c = attachAIKeepTappedPreference(sa, prefList, mandatory, attachSource);
        } else if ("Animate".equals(logic)) {
            c = attachAIAnimatePreference(sa, prefList, mandatory, attachSource);
        } else if ("Reanimate".equals(logic)) {
            c = attachAIReanimatePreference(sa, prefList, mandatory, attachSource);
        } else if ("SpecificCard".equals(logic)) {
            c = attachAISpecificCardPreference(sa, prefList, mandatory, attachSource);
        }

        return c;
    }

    /**
     * Contains useful keyword.
     * 
     * @param keywords
     *            the keywords
     * @param card
     *            the card
     * @param sa SpellAbility
     * @return true, if successful
     */
    private static boolean containsUsefulKeyword(final ArrayList<String> keywords, final Card card, final SpellAbility sa) {
        for (final String keyword : keywords) {
            if (isUsefulAttachKeyword(keyword, card, sa)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Contains useful curse keyword.
     * 
     * @param keywords
     *            the keywords
     * @param card
     *            the card
     * @param sa SpellAbility
     * @return true, if successful
     */
    private static boolean containsUsefulCurseKeyword(final ArrayList<String> keywords, final Card card, final SpellAbility sa) {
        for (final String keyword : keywords) {
            if (isUsefulCurseKeyword(keyword, card, sa)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if is useful keyword.
     * 
     * @param keyword
     *            the keyword
     * @param card
     *            the card
     * @param sa SpellAbility
     * @return true, if is useful keyword
     */
    private static boolean isUsefulAttachKeyword(final String keyword, final Card card, final SpellAbility sa) {
        final PhaseHandler ph = Singletons.getModel().getGame().getPhaseHandler();
        if (!CardUtil.isStackingKeyword(keyword) && card.hasKeyword(keyword)) {
            return false;
        }
        final boolean evasive = (keyword.endsWith("Unblockable") || keyword.equals("Fear")
                || keyword.equals("Intimidate") || keyword.equals("Shadow")
                || keyword.equals("Flying") || keyword.equals("Horsemanship")
                || keyword.endsWith("walk"));
        // give evasive keywords to creatures that can attack and deal damage
        if (evasive) {
            if (card.getNetCombatDamage() <= 0
                    || !CombatUtil.canAttackNextTurn(card)
                    || !CombatUtil.canBeBlocked(card)) {
                return false;
            }
        } else if (keyword.equals("Haste")) {
            if (!card.hasSickness() || !ph.isPlayerTurn(sa.getActivatingPlayer()) || card.isTapped()
                    || card.getNetCombatDamage() <= 0
                    || card.hasKeyword("CARDNAME can attack as though it had haste.")
                    || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)
                    || !CombatUtil.canAttackNextTurn(card)) {
                return false;
            }
        } else if (keyword.endsWith("Indestructible")) {
            return true;
        } else if (keyword.endsWith("Deathtouch") || keyword.endsWith("Wither")) {
            if (card.getNetCombatDamage() <= 0
                    || ((!CombatUtil.canBeBlocked(card) || !CombatUtil.canAttackNextTurn(card))
                            && !CombatUtil.canBlock(card, true))) {
                return false;
            }
        } else if (keyword.equals("Double Strike") || keyword.equals("Lifelink")) {
            if (card.getNetCombatDamage() <= 0
                    || (!CombatUtil.canAttackNextTurn(card) && !CombatUtil.canBlock(card, true))) {
                return false;
            }
        } else if (keyword.equals("First Strike")) {
            if (card.getNetCombatDamage() <= 0 || card.hasKeyword("Double Strike")) {
                return false;
            }
        } else if (keyword.startsWith("Flanking")) {
            if (card.getNetCombatDamage() <= 0
                    || !CombatUtil.canAttackNextTurn(card)
                    || !CombatUtil.canBeBlocked(card)) {
                return false;
            }
        } else if (keyword.startsWith("Bushido")) {
            if ((!CombatUtil.canBeBlocked(card) || !CombatUtil.canAttackNextTurn(card))
                    && !CombatUtil.canBlock(card, true)) {
                return false;
            }
        } else if (keyword.equals("Trample")) {
            if (card.getNetCombatDamage() <= 1
                    || !CombatUtil.canBeBlocked(card)
                    || !CombatUtil.canAttackNextTurn(card)) {
                return false;
            }
        } else if (keyword.equals("Infect")) {
            if (card.getNetCombatDamage() <= 0
                    || !CombatUtil.canAttackNextTurn(card)) {
                return false;
            }
        } else if (keyword.equals("Vigilance")) {
            if (card.getNetCombatDamage() <= 0
                    || !CombatUtil.canAttackNextTurn(card)
                    || !CombatUtil.canBlock(card, true)) {
                return false;
            }
        } else if (keyword.equals("Reach")) {
            if (card.hasKeyword("Flying") || !CombatUtil.canBlock(card, true)) {
                return false;
            }
        } else if (keyword.endsWith("CARDNAME can block an additional creature.")) {
            if (!CombatUtil.canBlock(card, true) || card.hasKeyword("CARDNAME can block any number of creatures.")) {
                return false;
            }
        } else if (keyword.equals("CARDNAME can attack as though it didn't have defender.")) {
            if (!card.hasKeyword("Defender") || card.hasKeyword("CARDNAME can attack as though it didn't have defender.")) {
                return false;
            }
        } else if (keyword.equals("Shroud") || keyword.equals("Hexproof")) {
            if (card.hasKeyword("Shroud") || card.hasKeyword("Hexproof")) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if is useful curse keyword.
     * 
     * @param keyword
     *            the keyword
     * @param card
     *            the card
     * @param sa SpellAbility
     * @return true, if is useful keyword
     */
    private static boolean isUsefulCurseKeyword(final String keyword, final Card card, final SpellAbility sa) {
        final Player ai = sa.getActivatingPlayer();
        if (!CardUtil.isStackingKeyword(keyword) && card.hasKeyword(keyword)) {
            return false;
        }

        if (keyword.endsWith("CARDNAME can't attack.") || keyword.equals("Defender")
                || keyword.endsWith("CARDNAME can't attack or block.")) {
            if (!CombatUtil.canAttackNextTurn(card)) {
                return false;
            }
        } else if (keyword.endsWith("CARDNAME attacks each turn if able.")) {
            if (!CombatUtil.canAttackNextTurn(card) || !CombatUtil.canBlock(card, true) || ai.getCreaturesInPlay().isEmpty()) {
                return false;
            }
        } else if (keyword.endsWith("CARDNAME can't block.") || keyword.contains("CantBlock")) {
            if (!CombatUtil.canBlock(card, true)) {
                return false;
            }
        } else if (keyword.endsWith("CARDNAME's activated abilities can't be activated.")) {
            for (SpellAbility ability : card.getSpellAbilities()) {
                if (ability.isAbility()) {
                    return true;
                }
            }
            return false;
        } else if (keyword.endsWith("Prevent all combat damage that would be dealt by CARDNAME.")) {
            if (!CombatUtil.canAttackNextTurn(card) || card.getNetCombatDamage() < 1) {
                return false;
            }
        } else if (keyword.endsWith("Prevent all combat damage that would be dealt to and dealt by CARDNAME.")
                || keyword.endsWith("Prevent all damage that would be dealt to and dealt by CARDNAME.")) {
            if (!CombatUtil.canAttackNextTurn(card) || card.getNetCombatDamage() < 2) {
                return false;
            }
        } else if (keyword.endsWith("CARDNAME doesn't untap during your untap step.")) {
            if (card.isUntapped()) {
                return false;
            }
        }
        return true;
    }

}
