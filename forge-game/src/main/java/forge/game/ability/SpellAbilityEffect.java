package forge.game.ability;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;

import forge.GameCommand;
import forge.card.MagicColor;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameObject;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardUtil;
import forge.game.card.CardZoneTable;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
import forge.game.replacement.ReplacementLayer;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.CardTranslation;
import forge.util.Lang;
import forge.util.Localizer;
import forge.util.TextUtil;
import forge.util.collect.FCollection;

/**
 * <p>
 * AbilityFactory_AlterLife class.
 * </p>
 *
 * @author Forge
 * @version $Id: AbilityFactoryAlterLife.java 17656 2012-10-22 19:32:56Z Max mtg $
 */

public abstract class SpellAbilityEffect {

    public abstract void resolve(SpellAbility sa);

    protected String getStackDescription(final SpellAbility sa) {
        // Unless overridden, let the spell description also be the stack description
        return sa.getDescription();
    }

    /**
     * Returns this effect description with needed prelude and epilogue.
     * @param params
     * @param sa
     * @return
     */
    public final String getStackDescriptionWithSubs(final Map<String, String> params, final SpellAbility sa) {
        StringBuilder sb = new StringBuilder();

        if (sa.getApi() != ApiType.PermanentCreature && sa.getApi() != ApiType.PermanentNoncreature) {
            // prelude for when this is root ability
            if (!(sa instanceof AbilitySub)) {
                sb.append(sa.getHostCard()).append(" -");
            }
            sb.append(" ");
        }

        // Own description
        String stackDesc = params.get("StackDescription");
        if (stackDesc != null) {
            // by typing "SpellDescription" they want to bypass the Effect's string builder
            if ("SpellDescription".equalsIgnoreCase(stackDesc)) {
            	if (params.get("SpellDescription") != null) {
            		sb.append(CardTranslation.translateSingleDescriptionText(params.get("SpellDescription"), sa.getHostCard().getName()));
            	}
            	if (sa.getTargets() != null && !sa.getTargets().isEmpty()) {
            		sb.append(" (Targeting: ").append(sa.getTargets()).append(")");
            	}
            } else if (!"None".equalsIgnoreCase(stackDesc)) { // by typing "none" they want to suppress output
                tokenizeString(sa, sb, stackDesc);
            }
        } else {
            final String condDesc = sa.getParam("ConditionDescription");
            final String afterDesc = sa.getParam("AfterDescription");
            final String baseDesc = CardTranslation.translateSingleDescriptionText(this.getStackDescription(sa), sa.getHostCard().getName());
            if (condDesc != null) {
                sb.append(condDesc).append(" ");
            }
            sb.append(condDesc != null && condDesc.endsWith(",") ? StringUtils.uncapitalize(baseDesc) : baseDesc);
            if (afterDesc != null) {
                sb.append(" ").append(afterDesc);
            }
        }

        // only add to StackDescription if its not a Permanent Spell
        if (sa.getApi() != ApiType.PermanentCreature && sa.getApi() != ApiType.PermanentNoncreature) {
            // This includes all subAbilities
            final AbilitySub abSub = sa.getSubAbility();
            if (abSub != null) {
                sb.append(abSub.getStackDescription());
            }
        }

        if (sa.hasParam("Announce")) {
            String svar = sa.getParam("Announce");
            int amount = AbilityUtils.calculateAmount(sa.getHostCard(), svar, sa);
            sb.append(" ");
            sb.append(TextUtil.enclosedParen(TextUtil.concatNoSpace(svar,"=",String.valueOf(amount))));
        } else {
            if (sa.costHasManaX()) {
                int amount = sa.getXManaCostPaid() == null ? 0 : sa.getXManaCostPaid();
                sb.append(" ");
                sb.append(TextUtil.enclosedParen(TextUtil.concatNoSpace("X","=",String.valueOf(amount))));
            }
        }

        String currentName = CardTranslation.getTranslatedName(sa.getHostCard().getName());
        String substitutedDesc = TextUtil.fastReplace(sb.toString(), "CARDNAME", currentName);
        substitutedDesc = TextUtil.fastReplace(substitutedDesc, "NICKNAME", Lang.getInstance().getNickName(currentName));
        return substitutedDesc;
    }

    /**
     * Append the description of a {@link SpellAbility} to a
     * {@link StringBuilder}.
     *
     * @param sa
     *            a {@link SpellAbility}.
     * @param sb
     *            a {@link StringBuilder}.
     * @param stackDesc
     *            the stack description of sa, formatted so that text appearing
     *            between braces <code>{ }</code> is replaced with defined
     *            {@link Player}, {@link SpellAbility}, and {@link Card}
     *            objects.
     */
    public static void tokenizeString(final SpellAbility sa, final StringBuilder sb, final String stackDesc) {
        final StringTokenizer st = new StringTokenizer(stackDesc, "{}", true);
        boolean isPlainText = true;

        while (st.hasMoreTokens()) {
            final String t = st.nextToken();
            if ("{".equals(t)) { isPlainText = false; continue; }
            if ("}".equals(t)) { isPlainText = true; continue; }

            if (!isPlainText) {
                if (t.startsWith("n:")) { // {n:<SVar> <noun(opt.)>}
                    String parts[] = t.substring(2).split(" ", 2);
                    int n = AbilityUtils.calculateAmount(sa.getHostCard(), parts[0], sa);
                    sb.append(parts.length == 1 ? Lang.getNumeral(n) : Lang.nounWithNumeral(n, parts[1]));
                } else {
                    final List<? extends GameObject> objs;
                    if (t.startsWith("p:")) {
                        objs = AbilityUtils.getDefinedPlayers(sa.getHostCard(), t.substring(2), sa);
                    } else if (t.startsWith("s:")) {
                        objs = AbilityUtils.getDefinedSpellAbilities(sa.getHostCard(), t.substring(2), sa);
                    } else if (t.startsWith("c:")) {
                        objs = AbilityUtils.getDefinedCards(sa.getHostCard(), t.substring(2), sa);
                    } else {
                        objs = AbilityUtils.getDefinedObjects(sa.getHostCard(), t, sa);
                    }
                    sb.append(StringUtils.join(objs, ", "));
                }
            } else {
                sb.append(t);
            }
        }
    }

    // Target/defined methods
    // Cards
    protected final static CardCollection getTargetCards(final SpellAbility sa) {                                       return getCards(false, "Defined",    sa); }
    protected final static CardCollection getTargetCards(final SpellAbility sa, final String definedParam) {            return getCards(false, definedParam, sa); }
    protected final static CardCollection getDefinedCardsOrTargeted(final SpellAbility sa) {                            return getCards(true,  "Defined",    sa); }
    protected final static CardCollection getDefinedCardsOrTargeted(final SpellAbility sa, final String definedParam) { return getCards(true,  definedParam, sa); }

    private static CardCollection getCards(final boolean definedFirst, final String definedParam, final SpellAbility sa) {
        final boolean useTargets = sa.usesTargeting() && (!definedFirst || !sa.hasParam(definedParam));
        return useTargets ? new CardCollection(sa.getTargets().getTargetCards())
                : AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam(definedParam), sa);
    }

    // Players
    protected final static PlayerCollection getTargetPlayers(final SpellAbility sa) {                                       return getPlayers(false, "Defined",    sa); }
    protected final static PlayerCollection getTargetPlayers(final SpellAbility sa, final String definedParam) {            return getPlayers(false, definedParam, sa); }
    protected final static PlayerCollection getDefinedPlayersOrTargeted(final SpellAbility sa) {                            return getPlayers(true,  "Defined",    sa); }
    protected final static PlayerCollection getDefinedPlayersOrTargeted(final SpellAbility sa, final String definedParam) { return getPlayers(true,  definedParam, sa); }

    private static PlayerCollection getPlayers(final boolean definedFirst, final String definedParam, final SpellAbility sa) {
        final boolean useTargets = sa.usesTargeting() && (!definedFirst || !sa.hasParam(definedParam));
        PlayerCollection players = useTargets ? new PlayerCollection(sa.getTargets().getTargetPlayers())
                : AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam(definedParam), sa);
        // try sort in APNAP order
        int indexAP = players.indexOf(sa.getHostCard().getGame().getPhaseHandler().getPlayerTurn());
        if (indexAP != -1) {
            Collections.rotate(players, - indexAP);
        }
        return players;
    }

    // Spells
    protected final static List<SpellAbility> getTargetSpells(final SpellAbility sa) {                                       return getSpells(false, "Defined",    sa); }
    protected final static List<SpellAbility> getTargetSpells(final SpellAbility sa, final String definedParam) {            return getSpells(false, definedParam, sa); }
    protected final static List<SpellAbility> getDefinedSpellsOrTargeted(final SpellAbility sa, final String definedParam) { return getSpells(true,  definedParam, sa); }

    private static List<SpellAbility> getSpells(final boolean definedFirst, final String definedParam, final SpellAbility sa) {
        final boolean useTargets = sa.usesTargeting() && (!definedFirst || !sa.hasParam(definedParam));
        return useTargets ? Lists.newArrayList(sa.getTargets().getTargetSpells())
                : AbilityUtils.getDefinedSpellAbilities(sa.getHostCard(), sa.getParam(definedParam), sa);
    }

    // Targets of card or player type
    protected final static List<GameEntity> getTargetEntities(final SpellAbility sa) {                                 return getEntities(false, "Defined",    sa); }
    protected final static List<GameEntity> getTargetEntities(final SpellAbility sa, final String definedParam) {      return getEntities(false, definedParam, sa); }
    protected final static List<GameEntity> getDefinedEntitiesOrTargeted(SpellAbility sa, final String definedParam) { return getEntities(true,  definedParam, sa); }

    private static List<GameEntity> getEntities(final boolean definedFirst, final String definedParam, final SpellAbility sa) {
        final boolean useTargets = sa.usesTargeting() && (!definedFirst || !sa.hasParam(definedParam));
        return useTargets ? Lists.newArrayList(sa.getTargets().getTargetEntities())
                : AbilityUtils.getDefinedEntities(sa.getHostCard(), sa.getParam(definedParam), sa);
    }

    // Targets of unspecified type
    protected final static List<GameObject> getTargets(final SpellAbility sa) {                                return getTargetables(false, "Defined",    sa); }
    protected final static List<GameObject> getTargets(final SpellAbility sa, final String definedParam) {     return getTargetables(false, definedParam, sa); }
    protected final static List<GameObject> getDefinedOrTargeted(SpellAbility sa, final String definedParam) { return getTargetables(true,  definedParam, sa); }

    private static List<GameObject> getTargetables(final boolean definedFirst, final String definedParam, final SpellAbility sa) {
        final boolean useTargets = sa.usesTargeting() && (!definedFirst || !sa.hasParam(definedParam));
        return useTargets ? Lists.newArrayList(sa.getTargets())
                : AbilityUtils.getDefinedObjects(sa.getHostCard(), sa.getParam(definedParam), sa);
    }

    protected final static List<Card> getCardsfromTargets(final SpellAbility sa) {
        List<Card> cards = getTargetCards(sa);
        // some card effects can also target a spell
        for (SpellAbility s : sa.getTargets().getTargetSpells()) {
            cards.add(s.getHostCard());
        }
        return cards;
    }

    protected static void registerDelayedTrigger(final SpellAbility sa, String location, final Iterable<Card> crds) {
        boolean intrinsic = sa.isIntrinsic();
        boolean your = location.startsWith("Your");
        boolean combat = location.endsWith("Combat");

        String desc = sa.getParamOrDefault("AtEOTDesc", "");

        if (your) {
            location = location.substring("Your".length());
        }
        if (combat) {
            location = location.substring(0, location.length() - "Combat".length());
        }

        if (desc.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            if (location.equals("Hand")) {
                sb.append("Return ");
            } else if (location.equals("SacrificeCtrl")) {
                sb.append("Its controller sacrifices ");
            } else {
                sb.append(location).append(" ");
            }
            sb.append(Lang.joinHomogenous(crds));
            if (location.equals("Hand")) {
                sb.append(" to your hand");
            }
            sb.append(" at the ");
            if (combat) {
                sb.append("end of combat.");
            } else {
                sb.append("beginning of ");
                sb.append(your ? "your" : "the");
                sb.append(" next end step.");
            }
            desc = sb.toString();
        }

        StringBuilder delTrig = new StringBuilder();
        delTrig.append("Mode$ Phase | Phase$ ");
        delTrig.append(combat ? "EndCombat "  : "End Of Turn ");

        if (your) {
            delTrig.append("| ValidPlayer$ You ");
        }
        delTrig.append("| TriggerDescription$ ").append(desc);

        final Trigger trig = TriggerHandler.parseTrigger(delTrig.toString(), CardUtil.getLKICopy(sa.getHostCard()), intrinsic);
        long ts = sa.getHostCard().getGame().getNextTimestamp();
        for (final Card c : crds) {
            trig.addRemembered(c);

            // Svar for AI
            c.addChangedSVars(Collections.singletonMap("EndOfTurnLeavePlay", "AtEOT"), ts, 0);
        }
        String trigSA = "";
        if (location.equals("Hand")) {
            trigSA = "DB$ ChangeZone | Defined$ DelayTriggerRememberedLKI | Origin$ Battlefield | Destination$ Hand";
        } else if (location.equals("SacrificeCtrl")) {
            trigSA = "DB$ SacrificeAll | Defined$ DelayTriggerRememberedLKI";
        } else if (location.equals("Sacrifice")) {
            trigSA = "DB$ SacrificeAll | Defined$ DelayTriggerRememberedLKI | Controller$ You";
        } else if (location.equals("Exile")) {
            trigSA = "DB$ ChangeZone | Defined$ DelayTriggerRememberedLKI | Origin$ Battlefield | Destination$ Exile";
        } else if (location.equals("Destroy")) {
            trigSA = "DB$ Destroy | Defined$ DelayTriggerRememberedLKI";
        }
        if (sa.hasParam("AtEOTCondition")) {
            String var = sa.getParam("AtEOTCondition");
            trigSA += "| ConditionCheckSVar$ " + var;
        }
        final SpellAbility newSa = AbilityFactory.getAbility(trigSA, sa.getHostCard());
        newSa.setIntrinsic(intrinsic);
        trig.setOverridingAbility(newSa);
        sa.getActivatingPlayer().getGame().getTriggerHandler().registerDelayedTrigger(trig);
    }

    protected static void addSelfTrigger(final SpellAbility sa, String location, final Card card) {
    	String trigStr = "Mode$ Phase | Phase$ End of Turn | TriggerZones$ Battlefield " +
    	     "| TriggerDescription$ At the beginning of the end step, " + location.toLowerCase()  + " CARDNAME.";
    	
    	final Trigger trig = TriggerHandler.parseTrigger(trigStr, card, true);
    	
    	String trigSA = "";
        if (location.equals("Sacrifice")) {
            trigSA = "DB$ Sacrifice | SacValid$ Self";
        } else if (location.equals("Exile")) {
            trigSA = "DB$ ChangeZone | Origin$ Battlefield | Destination$ Exile | Defined$ Self";
        }
        trig.setOverridingAbility(AbilityFactory.getAbility(trigSA, card));
        card.addTrigger(trig);

        // Svar for AI
        card.addChangedSVars(Collections.singletonMap("EndOfTurnLeavePlay", "AtEOT"), card.getGame().getNextTimestamp(), 0);
    }

    protected static SpellAbility getForgetSpellAbility(final Card card) {
        String forgetEffect = "DB$ Pump | ForgetObjects$ TriggeredCard";
        String exileEffect = "DB$ ChangeZone | Defined$ Self | Origin$ Command | Destination$ Exile"
                + " | ConditionDefined$ Remembered | ConditionPresent$ Card | ConditionCompare$ EQ0";

        SpellAbility saForget = AbilityFactory.getAbility(forgetEffect, card);
        AbilitySub saExile = (AbilitySub) AbilityFactory.getAbility(exileEffect, card);
        saForget.setSubAbility(saExile);
        return saForget;
    }

    protected static void addForgetOnMovedTrigger(final Card card, final String zone) {
        String trig = "Mode$ ChangesZone | ValidCard$ Card.IsRemembered | Origin$ " + zone + " | ExcludedDestinations$ Stack | Destination$ Any | TriggerZones$ Command | Static$ True";

        final Trigger parsedTrigger = TriggerHandler.parseTrigger(trig, card, true);
        parsedTrigger.setOverridingAbility(getForgetSpellAbility(card));
        card.addTrigger(parsedTrigger);
    }

    protected static void addForgetOnCastTrigger(final Card card) {
        String trig = "Mode$ SpellCast | ValidCard$ Card.IsRemembered | TriggerZones$ Command | Static$ True";

        final Trigger parsedTrigger = TriggerHandler.parseTrigger(trig, card, true);
        parsedTrigger.setOverridingAbility(getForgetSpellAbility(card));
        card.addTrigger(parsedTrigger);
    }

    protected static void addExileOnMovedTrigger(final Card card, final String zone) {
        String trig = "Mode$ ChangesZone | ValidCard$ Card.IsRemembered | Origin$ " + zone + " | Destination$ Any | TriggerZones$ Command | Static$ True";
        String effect = "DB$ ChangeZone | Defined$ Self | Origin$ Command | Destination$ Exile";
        final Trigger parsedTrigger = TriggerHandler.parseTrigger(trig, card, true);
        parsedTrigger.setOverridingAbility(AbilityFactory.getAbility(effect, card));
        final Trigger addedTrigger = card.addTrigger(parsedTrigger);
        addedTrigger.setIntrinsic(true);
    }

    protected static void addExileOnCounteredTrigger(final Card card) {
        String trig = "Mode$ Countered | ValidCard$ Card.IsRemembered | TriggerZones$ Command | Static$ True";
        String effect = "DB$ ChangeZone | Defined$ Self | Origin$ Command | Destination$ Exile";
        final Trigger parsedTrigger = TriggerHandler.parseTrigger(trig, card, true);
        parsedTrigger.setOverridingAbility(AbilityFactory.getAbility(effect, card));
        final Trigger addedTrigger = card.addTrigger(parsedTrigger);
        addedTrigger.setIntrinsic(true);
    }

    protected static void addForgetOnPhasedInTrigger(final Card card) {
        String trig = "Mode$ PhaseIn | ValidCard$ Card.IsRemembered | TriggerZones$ Command | Static$ True";

        final Trigger parsedTrigger = TriggerHandler.parseTrigger(trig, card, true);
        parsedTrigger.setOverridingAbility(getForgetSpellAbility(card));
        card.addTrigger(parsedTrigger);
    }

    protected static void addForgetCounterTrigger(final Card card, final String counterType) {
        String trig = "Mode$ CounterRemoved | TriggerZones$ Command | ValidCard$ Card.IsRemembered | CounterType$ " + counterType + " | NewCounterAmount$ 0 | Static$ True";

        final Trigger parsedTrigger = TriggerHandler.parseTrigger(trig, card, true);
        parsedTrigger.setOverridingAbility(getForgetSpellAbility(card));
        card.addTrigger(parsedTrigger);
    }

    protected static void addLeaveBattlefieldReplacement(final Card card, final SpellAbility sa, final String zone) {
        final Card host = sa.getHostCard();
        final Game game = card.getGame();
        final Card eff = createEffect(sa, sa.getActivatingPlayer(), host.getName() + "'s Effect", host.getImageKey());

        addLeaveBattlefieldReplacement(eff, zone);

        eff.addRemembered(card);

        // Add forgot trigger
        addExileOnMovedTrigger(eff, "Battlefield");

        // Copy text changes
        if (sa.isIntrinsic()) {
            eff.copyChangedTextFrom(card);
        }

        // TODO: Add targeting to the effect so it knows who it's dealing with
        game.getTriggerHandler().suppressMode(TriggerType.ChangesZone);
        game.getAction().moveTo(ZoneType.Command, eff, sa, null);
        eff.updateStateForView();
        game.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);
    }

    protected static void addLeaveBattlefieldReplacement(final Card eff, final String zone) {
        final String repeffstr = "Event$ Moved | ValidCard$ Card.IsRemembered "
                + "| Origin$ Battlefield | ExcludeDestination$ " + zone
                + "| Description$ If Creature would leave the battlefield, "
                + " exile it instead of putting it anywhere else.";
        String effect = "DB$ ChangeZone | Defined$ ReplacedCard | Origin$ Battlefield | Destination$ " + zone;

        ReplacementEffect re = ReplacementHandler.parseReplacement(repeffstr, eff, true);
        re.setLayer(ReplacementLayer.Other);

        re.setOverridingAbility(AbilityFactory.getAbility(effect, eff));
        eff.addReplacementEffect(re);
    }

    // create a basic template for Effect to be used somewhere else
    protected static Card createEffect(final SpellAbility sa, final Player controller, final String name,
            final String image) {
        final Card hostCard = sa.getHostCard();
        final Game game = hostCard.getGame();
        final Card eff = new Card(game.nextCardId(), game);
        eff.setTimestamp(game.getNextTimestamp());
        eff.setName(name);
        eff.setColor(hostCard.getColor().getColor());
        // if name includes emblem then it should be one
        if (name.startsWith("Emblem")) {
            eff.setEmblem(true);
            // Emblem needs to be colorless
            eff.setColor(MagicColor.COLORLESS);
        }

        eff.setOwner(controller);
        eff.setSVars(sa.getSVars());

        eff.setImageKey(image);

        eff.setImmutable(true);
        eff.setEffectSource(sa);

        return eff;
    }

    protected static void replaceDying(final SpellAbility sa) {
        if (sa.hasParam("ReplaceDyingDefined") || sa.hasParam("ReplaceDyingValid")) {
            if (sa.hasParam("ReplaceDyingCondition")) {
                // currently there is only one with Kicker
                final String condition = sa.getParam("ReplaceDyingCondition");
                if ("Kicked".equals(condition)) {
                    if (!sa.isKicked()) {
                        return;
                    }
                }
            }

            final Card host = sa.getHostCard();
            final Player controller = sa.getActivatingPlayer();
            final Game game = host.getGame();
            String zone = sa.getParamOrDefault("ReplaceDyingZone", "Exile");

            CardCollection cards = null;

            if (sa.hasParam("ReplaceDyingDefined")) {
                cards = AbilityUtils.getDefinedCards(host, sa.getParam("ReplaceDyingDefined"), sa);
                // no cards, no need for Effect
                if (cards.isEmpty()) {
                    return;
                }
            }

            // build an Effect with that information
            String name = host.getName() + "'s Effect";

            final Card eff = createEffect(sa, controller, name, host.getImageKey());
            if (cards != null) {
                eff.addRemembered(cards);
            }

            String valid = sa.getParamOrDefault("ReplaceDyingValid", "Card.IsRemembered");

            String repeffstr = "Event$ Moved | ValidLKI$ " + valid +
                    "| Origin$ Battlefield | Destination$ Graveyard " +
                    "| Description$ If that permanent would die this turn, exile it instead.";
            String effect = "DB$ ChangeZone | Defined$ ReplacedCard | Origin$ Battlefield | Destination$ " + zone;

            ReplacementEffect re = ReplacementHandler.parseReplacement(repeffstr, eff, true);
            re.setLayer(ReplacementLayer.Other);

            re.setOverridingAbility(AbilityFactory.getAbility(effect, eff));
            eff.addReplacementEffect(re);

            if (cards != null) {
                // Add forgot trigger
                addForgetOnMovedTrigger(eff, "Battlefield");
            }

            // Copy text changes
            if (sa.isIntrinsic()) {
                eff.copyChangedTextFrom(host);
            }

            final GameCommand endEffect = new GameCommand() {
                private static final long serialVersionUID = -5861759814760561373L;

                @Override
                public void run() {
                    game.getAction().exile(eff, null);
                }
            };

            game.getEndOfTurn().addUntil(endEffect);

            // TODO: Add targeting to the effect so it knows who it's dealing with
            game.getTriggerHandler().suppressMode(TriggerType.ChangesZone);
            game.getAction().moveTo(ZoneType.Command, eff, sa, null);
            eff.updateStateForView();
            game.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);
        }
    }

    protected static boolean addToCombat(Card c, Player controller, SpellAbility sa, String attackingParam, String blockingParam) {
        final Card host = sa.getHostCard();
        final Game game = controller.getGame();
        if (!game.getPhaseHandler().inCombat()) {
            return false;
        }
        boolean combatChanged = false;
        final Combat combat = game.getCombat();

        if (sa.hasParam(attackingParam) && combat.getAttackingPlayer().equals(controller)) {
            String attacking = sa.getParam(attackingParam);

            GameEntity defender = null;
            FCollection<GameEntity> defs = null;
            // important to update defenders here, maybe some PW got removed
            combat.initConstraints();
            if (sa.hasParam("ChoosePlayerOrPlaneswalker")) {
                PlayerCollection defendingPlayers = AbilityUtils.getDefinedPlayers(sa.hasParam("ForEach") ? c : host, attacking, sa);
                defs = new FCollection<>();
                for (Player p : defendingPlayers) {
                    defs.addAll(combat.getDefendersControlledBy(p));
                }
            } else if ("True".equalsIgnoreCase(attacking)) {
                defs = (FCollection<GameEntity>) combat.getDefenders();
            } else {
                defs = AbilityUtils.getDefinedEntities(host, attacking, sa);
            }

            if (defs != null) {
                Map<String, Object> params = Maps.newHashMap();
                params.put("Attacker", c);
                Player chooser;
                if (sa.hasParam("Chooser")) {
                    chooser = Iterables.getFirst(AbilityUtils.getDefinedPlayers(host, sa.getParam("Chooser"), sa), null);
                } else {
                    chooser = controller;
                }
                defender = chooser.getController().chooseSingleEntityForEffect(defs, sa,
                        Localizer.getInstance().getMessage("lblChooseDefenderToAttackWithCard", CardTranslation.getTranslatedName(c.getName())), false, params);
            }

            if (defender != null) {
                combat.addAttacker(c, defender);
                combat.getBandOfAttacker(c).setBlocked(false);
                combatChanged = true;
            }
        }
        if (sa.hasParam(blockingParam)) {
            final Card attacker = Iterables.getFirst(AbilityUtils.getDefinedCards(host, sa.getParam(blockingParam), sa), null);
            if (attacker != null) {
                final boolean wasBlocked = combat.isBlocked(attacker);
                combat.addBlocker(attacker, c);
                combat.orderAttackersForDamageAssignment(c);

                {
                    final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
                    runParams.put(AbilityKey.Attacker, attacker);
                    runParams.put(AbilityKey.Blocker, c);
                    game.getTriggerHandler().runTrigger(TriggerType.AttackerBlockedByCreature, runParams, false);
                }
                {
                    final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
                    runParams.put(AbilityKey.Attackers, attacker);
                    game.getTriggerHandler().runTrigger(TriggerType.AttackerBlockedOnce, runParams, false);
                }

                // Run triggers for new blocker and add it to damage assignment order
                if (!wasBlocked) {
                    final CardCollection blockers = combat.getBlockers(attacker);
                    final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
                    runParams.put(AbilityKey.Attacker, attacker);
                    runParams.put(AbilityKey.Blockers, blockers);
                    runParams.put(AbilityKey.NumBlockers, blockers.size());
                    runParams.put(AbilityKey.Defender, combat.getDefenderByAttacker(attacker));
                    runParams.put(AbilityKey.DefendingPlayer, combat.getDefenderPlayerByAttacker(attacker));
                    game.getTriggerHandler().runTrigger(TriggerType.AttackerBlocked, runParams, false);

                    combat.setBlocked(attacker, true);
                    combat.addBlockerToDamageAssignmentOrder(attacker, c);
                }
                combatChanged = true;
            }
        }
        return combatChanged;
    }

    protected static GameCommand untilHostLeavesPlayCommand(final CardZoneTable triggerList, final SpellAbility sa) {
        final Card hostCard = sa.getHostCard();
        final Game game = hostCard.getGame();
        hostCard.addUntilLeavesBattlefield(triggerList.allCards());
        final TriggerHandler trigHandler  = game.getTriggerHandler();
        final Card lki = CardUtil.getLKICopy(hostCard);
        lki.clearControllers();
        lki.setOwner(sa.getActivatingPlayer());

        return new GameCommand() {

            private static final long serialVersionUID = 1L;

            @Override
            public void run() {
                CardCollectionView untilCards = hostCard.getUntilLeavesBattlefield();
                // if the list is empty, then the table doesn't need to be checked anymore
                if (untilCards.isEmpty()) {
                    return;
                }
                CardZoneTable untilTable = new CardZoneTable();
                Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
                moveParams.put(AbilityKey.LastStateBattlefield, game.copyLastStateBattlefield());
                moveParams.put(AbilityKey.LastStateGraveyard, game.copyLastStateGraveyard());
                for (Table.Cell<ZoneType, ZoneType, CardCollection> cell : triggerList.cellSet()) {
                    for (Card c : cell.getValue()) {
                        // check if card is still in the until host leaves play list
                        if (!untilCards.contains(c)) {
                            continue;
                        }
                        // better check if card didn't changed zones again?
                        Card newCard = game.getCardState(c, null);
                        if (newCard == null || !newCard.equalsWithTimestamp(c)) {
                            continue;
                        }
                        Trigger trig = null;
                        if (sa.hasAdditionalAbility("ReturnAbility")) {
                            String valid = sa.getParamOrDefault("ReturnValid", "Card.IsTriggerRemembered");

                            String trigSA = "Mode$ ChangesZone | Origin$ " + cell.getColumnKey() + " | Destination$ " + cell.getRowKey() + " | ValidCard$ " + valid +
                                    " | TriggerDescription$ " + sa.getAdditionalAbility("ReturnAbility").getParam("SpellDescription");

                            trig = TriggerHandler.parseTrigger(trigSA, hostCard, sa.isIntrinsic(), null);
                            trig.setSpawningAbility(sa.copy(lki, sa.getActivatingPlayer(), true));
                            trig.setActiveZone(null);
                            trig.addRemembered(newCard);

                            SpellAbility overridingSA = sa.getAdditionalAbility("ReturnAbility").copy(hostCard, sa.getActivatingPlayer(), false);
                            // need to reset the parent, additionalAbility does set it to this
                            if (overridingSA instanceof AbilitySub) {
                                ((AbilitySub)overridingSA).setParent(null);
                            }

                            trig.setOverridingAbility(overridingSA);

                            // Delayed Trigger should only happen once, no need for cleanup?
                            trigHandler.registerThisTurnDelayedTrigger(trig);
                        }
                        // no cause there?
                        Card movedCard = game.getAction().moveTo(cell.getRowKey(), newCard, 0, null, moveParams);
                        untilTable.put(cell.getColumnKey(), cell.getRowKey(), movedCard);
                    }
                }
                untilTable.triggerChangesZoneAll(game, null);
            }

        };
    }

    protected static void discard(SpellAbility sa, CardZoneTable table, final boolean effect, Map<Player, CardCollectionView> discardedMap, Map<AbilityKey, Object> params) {
        Set<Player> discarders = discardedMap.keySet();
        for (Player p : discarders) {
            final CardCollection discardedByPlayer = new CardCollection();
            for (Card card : Lists.newArrayList(discardedMap.get(p))) { // without copying will get concurrent modification exception
                if (card == null) { continue; }
                if (p.discard(card, sa, effect, table, params) != null) {
                    discardedByPlayer.add(card);

                    if (sa.hasParam("RememberDiscarded")) {
                        sa.getHostCard().addRemembered(card);
                    }
                }
            }
            discardedMap.put(p, discardedByPlayer);
        }

        for (Player p : discarders) {
            CardCollectionView discardedByPlayer = discardedMap.get(p);
            if (!discardedByPlayer.isEmpty()) {
                boolean firstDiscard = p.getNumDiscardedThisTurn() - discardedByPlayer.size() == 0;
                final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
                runParams.put(AbilityKey.Player, p);
                runParams.put(AbilityKey.Cards, discardedByPlayer);
                runParams.put(AbilityKey.Cause, sa);
                runParams.put(AbilityKey.FirstTime, firstDiscard);
                p.getGame().getTriggerHandler().runTrigger(TriggerType.DiscardedAll, runParams, false);

                if (sa.hasParam("RememberDiscardingPlayers")) {
                    sa.getHostCard().addRemembered(p);
                }
            }
        }
    }

    protected static void addUntilCommand(final SpellAbility sa, GameCommand until) {
        Card host = sa.getHostCard();
        final Game game = host.getGame();
        final String duration = sa.getParam("Duration");
        // in case host was LKI or still resolving
        if (host.isLKI() || host.getZone() == null || host.getZone().is(ZoneType.Stack)) {
            host = game.getCardState(host);
        }

        if ("UntilEndOfCombat".equals(duration)) {
            game.getEndOfCombat().addUntil(until);
        } else if ("UntilYourNextUpkeep".equals(duration)) {
            game.getUpkeep().addUntil(sa.getActivatingPlayer(), until);
        } else if ("UntilTheEndOfYourNextUpkeep".equals(duration)) {
            if (game.getPhaseHandler().is(PhaseType.UPKEEP)) {
                game.getUpkeep().registerUntilEnd(host.getController(), until);
            } else {
                game.getUpkeep().addUntilEnd(host.getController(), until);
            }
        }  else if ("UntilTheEndOfYourNextTurn".equals(duration)) {
            if (game.getPhaseHandler().isPlayerTurn(sa.getActivatingPlayer())) {
                game.getEndOfTurn().registerUntilEnd(sa.getActivatingPlayer(), until);
            } else {
                game.getEndOfTurn().addUntilEnd(sa.getActivatingPlayer(), until);
            }
        } else if ("UntilTheEndOfTargetedNextTurn".equals(duration)) {
            Player targeted = sa.getTargets().getFirstTargetedPlayer();
            if (game.getPhaseHandler().isPlayerTurn(targeted)) {
                game.getEndOfTurn().registerUntilEnd(targeted, until);
            } else {
                game.getEndOfTurn().addUntilEnd(targeted, until);
            }
        } else if (duration != null && duration.startsWith("UntilAPlayerCastSpell")) {
            game.getStack().addCastCommand(duration.split(" ")[1], until);
        } else if ("UntilHostLeavesPlay".equals(duration)) {
            host.addLeavesPlayCommand(until);
        } else if ("UntilHostLeavesPlayOrEOT".equals(duration)) {
            host.addLeavesPlayCommand(until);
            game.getEndOfTurn().addUntil(until);
        } else if ("UntilLoseControlOfHost".equals(duration)) {
            host.addLeavesPlayCommand(until);
            host.addChangeControllerCommand(until);
        } else if ("UntilYourNextTurn".equals(duration)) {
            game.getCleanup().addUntil(sa.getActivatingPlayer(), until);
        } else if ("UntilUntaps".equals(duration)) {
            host.addUntapCommand(until);
        } else if ("UntilUnattached".equals(duration)) {
            host.addLeavesPlayCommand(until); //if it leaves play, it's unattached
            host.addUnattachCommand(until);
        } else if ("UntilFacedown".equals(duration)) {
            host.addFacedownCommand(until);
        } else {
            game.getEndOfTurn().addUntil(until);
        }
    }
}
