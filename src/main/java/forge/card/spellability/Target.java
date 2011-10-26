package forge.card.spellability;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import forge.AllZoneUtil;
import forge.Card;
import forge.Constant;
import forge.Player;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.cardFactory.CardFactoryUtil;

/**
 * <p>
 * Target class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class Target {
    // Target has two things happening:
    // Targeting restrictions (Creature, Min/Maxm etc) which are true for this
    // whole Target
    // Target Choices (which is specific for the StackInstance)
    private Card srcCard;
    private boolean uniqueTargets = false;
    private Target_Choices choice = null;

    /**
     * <p>
     * getTargetChoices.
     * </p>
     * 
     * @return a {@link forge.card.spellability.Target_Choices} object.
     */
    public final Target_Choices getTargetChoices() {
        return choice;
    }

    /**
     * <p>
     * setTargetChoices.
     * </p>
     * 
     * @param tc
     *            a {@link forge.card.spellability.Target_Choices} object.
     */
    public final void setTargetChoices(final Target_Choices tc) {
        choice = tc;
    }

    private boolean bMandatory = false;

    /**
     * <p>
     * getMandatory.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean getMandatory() {
        return bMandatory;
    }

    /**
     * <p>
     * setMandatory.
     * </p>
     * 
     * @param m
     *            a boolean.
     */
    public final void setMandatory(final boolean m) {
        bMandatory = m;
    }

    private boolean tgtValid = false;
    private String[] ValidTgts;
    private String vtSelection = "";

    /**
     * <p>
     * doesTarget.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean doesTarget() {
        return tgtValid;
    }

    /**
     * <p>
     * getValidTgts.
     * </p>
     * 
     * @return an array of {@link java.lang.String} objects.
     */
    public final String[] getValidTgts() {
        return ValidTgts;
    }

    /**
     * <p>
     * getVTSelection.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getVTSelection() {
        return vtSelection;
    }

    private String minTargets;
    private String maxTargets;

    /**
     * <p>
     * Getter for the field <code>minTargets</code>.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a int.
     */
    public final int getMinTargets(final Card c, final SpellAbility sa) {
        return AbilityFactory.calculateAmount(c, minTargets, sa);
    }

    /**
     * <p>
     * Getter for the field <code>maxTargets</code>.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a int.
     */
    public final int getMaxTargets(final Card c, final SpellAbility sa) {
        return AbilityFactory.calculateAmount(c, maxTargets, sa);
    }

    /**
     * <p>
     * isMaxTargetsChosen.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public final boolean isMaxTargetsChosen(final Card c, final SpellAbility sa) {
        return choice != null && getMaxTargets(c, sa) == choice.getNumTargeted();
    }

    /**
     * <p>
     * isMinTargetsChosen.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public final boolean isMinTargetsChosen(final Card c, final SpellAbility sa) {
        if (getMinTargets(c, sa) == 0) {
            return true;
        }
        return choice != null && getMinTargets(c, sa) <= choice.getNumTargeted();
    }

    private List<Constant.Zone> tgtZone = Arrays.asList(Constant.Zone.Battlefield);

    /**
     * <p>
     * setZone.
     * </p>
     * 
     * @param tZone
     *            a {@link java.lang.String} object.
     */
    public final void setZone(final Constant.Zone tZone) {
        tgtZone = Arrays.asList(tZone);
    }

    /**
     * Sets the zone.
     * 
     * @param tZone
     *            the new zone
     */
    public final void setZone(final List<Constant.Zone> tZone) {
        tgtZone = tZone;
    }

    /**
     * <p>
     * getZone.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final List<Constant.Zone> getZone() {
        return tgtZone;
    }

    // Used for Counters. Currently, Spell,Activated,Triggered can be
    // Comma-separated
    private String targetSpellAbilityType = null;

    /**
     * <p>
     * Setter for the field <code>targetSpellAbilityType</code>.
     * </p>
     * 
     * @param tgtSAType
     *            a {@link java.lang.String} object.
     */
    public final void setTargetSpellAbilityType(final String tgtSAType) {
        targetSpellAbilityType = tgtSAType;
    }

    /**
     * <p>
     * Getter for the field <code>targetSpellAbilityType</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getTargetSpellAbilityType() {
        return targetSpellAbilityType;
    }

    // Used for Counters. The target SA of this SA must be targeting a Valid X
    private String saValidTargeting = null;

    /**
     * <p>
     * setSAValidTargeting.
     * </p>
     * 
     * @param saValidTgting
     *            a {@link java.lang.String} object.
     */
    public final void setSAValidTargeting(final String saValidTgting) {
        saValidTargeting = saValidTgting;
    }

    /**
     * <p>
     * getSAValidTargeting.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getSAValidTargeting() {
        return saValidTargeting;
    }

    // Leaving old structure behind for compatibility.
    /**
     * <p>
     * addTarget.
     * </p>
     * 
     * @param o
     *            a {@link java.lang.Object} object.
     * @return a boolean.
     */
    public final boolean addTarget(final Object o) {
        if (choice == null) {
            choice = new Target_Choices();
        }

        if (o instanceof Card) {
            return choice.addTarget((Card) o);
        }

        if (o instanceof Player) {
            return choice.addTarget((Player) o);
        }

        if (o instanceof SpellAbility) {
            return choice.addTarget((SpellAbility) o);
        }

        return false;
    }

    /**
     * <p>
     * getTargetCards.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<Card> getTargetCards() {
        if (choice == null) {
            return new ArrayList<Card>();
        }

        return choice.getTargetCards();
    }

    /**
     * <p>
     * getTargetPlayers.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<Player> getTargetPlayers() {
        if (choice == null) {
            return new ArrayList<Player>();
        }

        return choice.getTargetPlayers();
    }

    /**
     * <p>
     * getTargetSAs.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<SpellAbility> getTargetSAs() {
        if (choice == null) {
            return new ArrayList<SpellAbility>();
        }

        return choice.getTargetSAs();
    }

    /**
     * <p>
     * getTargets.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<Object> getTargets() {
        if (choice == null) {
            return new ArrayList<Object>();
        }

        return choice.getTargets();
    }

    /**
     * <p>
     * getNumTargeted.
     * </p>
     * 
     * @return a int.
     */
    public final int getNumTargeted() {
        if (choice == null) {
            return 0;
        }
        return choice.getNumTargeted();
    }

    /**
     * <p>
     * resetTargets.
     * </p>
     */
    public final void resetTargets() {
        choice = null;
    }

    /**
     * <p>
     * Constructor for Target.
     * </p>
     * 
     * @param src
     *            a {@link forge.Card} object.
     * @param parse
     *            a {@link java.lang.String} object.
     */
    public Target(final Card src, final String parse) {
        this(src, parse, "1", "1");
    }

    /**
     * <p>
     * Constructor for Target.
     * </p>
     * 
     * @param src
     *            a {@link forge.Card} object.
     * @param parse
     *            a {@link java.lang.String} object.
     * @param min
     *            a {@link java.lang.String} object.
     * @param max
     *            a {@link java.lang.String} object.
     */
    public Target(final Card src, String parse, final String min, final String max) {
        // parse=Tgt{C}{P} - Primarily used for Pump or Damage
        // C = Creature P=Player/Planeswalker
        // CP = All three

        tgtValid = true;
        srcCard = src;

        if (parse.contains("Tgt")) {
            parse = parse.replace("Tgt", "");
        }

        String valid;
        String prompt;
        StringBuilder sb = new StringBuilder();

        if (parse.equals("CP")) {
            valid = "Creature,Planeswalker.YouDontCtrl,Player";
            prompt = "Select target creature, planeswalker, or player";
        } else if (parse.equals("C")) {
            valid = "Creature";
            prompt = "Select target creature";
        } else if (parse.equals("P")) {
            valid = "Planeswalker.YouDontCtrl,Player";
            prompt = "Select target planeswalker or player";
        } else {
            System.out.println("Bad Parsing in Target(parse, min, max): " + parse);
            return;
        }

        if (src != null) {
            sb.append(src + " - ");
        }
        sb.append(prompt);
        vtSelection = sb.toString();
        ValidTgts = valid.split(",");

        minTargets = min;
        maxTargets = max;
    }

    /**
     * <p>
     * Constructor for Target.
     * </p>
     * 
     * @param src
     *            a {@link forge.Card} object.
     * @param select
     *            a {@link java.lang.String} object.
     * @param valid
     *            an array of {@link java.lang.String} objects.
     */
    public Target(final Card src, final String select, final String[] valid) {
        this(src, select, valid, "1", "1");
    }

    /**
     * <p>
     * Constructor for Target.
     * </p>
     * 
     * @param src
     *            a {@link forge.Card} object.
     * @param select
     *            a {@link java.lang.String} object.
     * @param valid
     *            a {@link java.lang.String} object.
     */
    public Target(final Card src, final String select, final String valid) {
        this(src, select, valid.split(","), "1", "1");
    }

    /**
     * <p>
     * Constructor for Target.
     * </p>
     * 
     * @param src
     *            a {@link forge.Card} object.
     * @param select
     *            a {@link java.lang.String} object.
     * @param valid
     *            an array of {@link java.lang.String} objects.
     * @param min
     *            a {@link java.lang.String} object.
     * @param max
     *            a {@link java.lang.String} object.
     */
    public Target(final Card src, final String select, final String[] valid, final String min, final String max) {
        srcCard = src;
        tgtValid = true;
        vtSelection = select;
        ValidTgts = valid;

        minTargets = min;
        maxTargets = max;
    }

    /**
     * <p>
     * getTargetedString.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getTargetedString() {
        ArrayList<Object> tgts = getTargets();
        StringBuilder sb = new StringBuilder("");
        for (Object o : tgts) {
            if (o instanceof Player) {
                Player p = (Player) o;
                sb.append(p.getName());
            }
            if (o instanceof Card) {
                Card c = (Card) o;
                sb.append(c);
            }
            sb.append(" ");
        }

        return sb.toString();
    }

    /**
     * <p>
     * canOnlyTgtOpponent.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean canOnlyTgtOpponent() {
        boolean player = false;
        boolean opponent = false;
        for (String s : ValidTgts) {
            if (s.equals("Opponent")) {
                opponent = true;
            } else if (s.equals("Player")) {
                player = true;
            }
        }
        return opponent && !player;
    }

    /**
     * <p>
     * canTgtPlayer.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean canTgtPlayer() {
        for (String s : ValidTgts) {
            if (s.equals("Player") || s.equals("Opponent")) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>
     * canTgtCreature.
     * </p>
     * 
     * @return a boolean.
     */

    public final boolean canTgtPermanent() {
        for (String s : ValidTgts) {
            if (s.contains("Permanent")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Can tgt creature.
     * 
     * @return true, if successful
     */
    public final boolean canTgtCreature() {
        for (String s : ValidTgts) {
            if (s.contains("Creature") && !s.contains("nonCreature")) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>
     * canTgtCreatureAndPlayer.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean canTgtCreatureAndPlayer() {
        return canTgtPlayer() && canTgtCreature();
    }

    /**
     * <p>
     * hasCandidates.
     * </p>
     * 
     * @param isTargeted
     *            Check Valid Candidates and Targeting
     * 
     * @return a boolean.
     */
    public final boolean hasCandidates(final boolean isTargeted) {
        if (canTgtPlayer()) {
            return true;
        }

        for (Card c : AllZoneUtil.getCardsIn(tgtZone)) {
            if (c.isValid(ValidTgts, srcCard.getController(), srcCard)
                    && (!isTargeted || CardFactoryUtil.canTarget(srcCard, c))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if is unique targets.
     * 
     * @return true, if is unique targets
     */
    public final boolean isUniqueTargets() {
        return uniqueTargets;
    }

    /**
     * Sets the unique targets.
     * 
     * @param unique
     *            the new unique targets
     */
    public final void setUniqueTargets(final boolean unique) {
        this.uniqueTargets = unique;
    }
}
