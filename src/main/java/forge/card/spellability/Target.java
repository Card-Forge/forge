package forge.card.spellability;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import forge.AllZoneUtil;
import forge.Card;
import forge.Constant;
import forge.Player;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.cardfactory.CardFactoryUtil;

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
    private TargetChoices choice = null;

    /**
     * <p>
     * getTargetChoices.
     * </p>
     * 
     * @return a {@link forge.card.spellability.TargetChoices} object.
     */
    public final TargetChoices getTargetChoices() {
        return this.choice;
    }

    /**
     * <p>
     * setTargetChoices.
     * </p>
     * 
     * @param tc
     *            a {@link forge.card.spellability.TargetChoices} object.
     */
    public final void setTargetChoices(final TargetChoices tc) {
        this.choice = tc;
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
        return this.bMandatory;
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
        this.bMandatory = m;
    }

    private boolean tgtValid = false;
    private String[] validTgts;
    private String vtSelection = "";

    /**
     * <p>
     * doesTarget.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean doesTarget() {
        return this.tgtValid;
    }

    /**
     * <p>
     * getValidTgts.
     * </p>
     * 
     * @return an array of {@link java.lang.String} objects.
     */
    public final String[] getValidTgts() {
        return this.validTgts;
    }

    /**
     * <p>
     * getVTSelection.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getVTSelection() {
        return this.vtSelection;
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
        return AbilityFactory.calculateAmount(c, this.minTargets, sa);
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
        return AbilityFactory.calculateAmount(c, this.maxTargets, sa);
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
        return (this.choice != null) && (this.getMaxTargets(c, sa) == this.choice.getNumTargeted());
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
        if (this.getMinTargets(c, sa) == 0) {
            return true;
        }
        return (this.choice != null) && (this.getMinTargets(c, sa) <= this.choice.getNumTargeted());
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
        this.tgtZone = Arrays.asList(tZone);
    }

    /**
     * Sets the zone.
     * 
     * @param tZone
     *            the new zone
     */
    public final void setZone(final List<Constant.Zone> tZone) {
        this.tgtZone = tZone;
    }

    /**
     * <p>
     * getZone.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final List<Constant.Zone> getZone() {
        return this.tgtZone;
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
        this.targetSpellAbilityType = tgtSAType;
    }

    /**
     * <p>
     * Getter for the field <code>targetSpellAbilityType</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getTargetSpellAbilityType() {
        return this.targetSpellAbilityType;
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
        this.saValidTargeting = saValidTgting;
    }

    /**
     * <p>
     * getSAValidTargeting.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getSAValidTargeting() {
        return this.saValidTargeting;
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
        if (this.choice == null) {
            this.choice = new TargetChoices();
        }

        if (o instanceof Card) {
            return this.choice.addTarget((Card) o);
        }

        if (o instanceof Player) {
            return this.choice.addTarget((Player) o);
        }

        if (o instanceof SpellAbility) {
            return this.choice.addTarget((SpellAbility) o);
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
        if (this.choice == null) {
            return new ArrayList<Card>();
        }

        return this.choice.getTargetCards();
    }

    /**
     * <p>
     * getTargetPlayers.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<Player> getTargetPlayers() {
        if (this.choice == null) {
            return new ArrayList<Player>();
        }

        return this.choice.getTargetPlayers();
    }

    /**
     * <p>
     * getTargetSAs.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<SpellAbility> getTargetSAs() {
        if (this.choice == null) {
            return new ArrayList<SpellAbility>();
        }

        return this.choice.getTargetSAs();
    }

    /**
     * <p>
     * getTargets.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<Object> getTargets() {
        if (this.choice == null) {
            return new ArrayList<Object>();
        }

        return this.choice.getTargets();
    }

    /**
     * <p>
     * getNumTargeted.
     * </p>
     * 
     * @return a int.
     */
    public final int getNumTargeted() {
        if (this.choice == null) {
            return 0;
        }
        return this.choice.getNumTargeted();
    }

    /**
     * <p>
     * resetTargets.
     * </p>
     */
    public final void resetTargets() {
        this.choice = null;
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

        this.tgtValid = true;
        this.srcCard = src;

        if (parse.contains("Tgt")) {
            parse = parse.replace("Tgt", "");
        }

        String valid;
        String prompt;
        final StringBuilder sb = new StringBuilder();

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
        this.vtSelection = sb.toString();
        this.validTgts = valid.split(",");

        this.minTargets = min;
        this.maxTargets = max;
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
        this.srcCard = src;
        this.tgtValid = true;
        this.vtSelection = select;
        this.validTgts = valid;

        this.minTargets = min;
        this.maxTargets = max;
    }

    /**
     * <p>
     * getTargetedString.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getTargetedString() {
        final ArrayList<Object> tgts = this.getTargets();
        final StringBuilder sb = new StringBuilder("");
        for (final Object o : tgts) {
            if (o instanceof Player) {
                final Player p = (Player) o;
                sb.append(p.getName());
            }
            if (o instanceof Card) {
                final Card c = (Card) o;
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
        for (final String s : this.validTgts) {
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
        for (final String s : this.validTgts) {
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
        for (final String s : this.validTgts) {
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
        for (final String s : this.validTgts) {
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
        return this.canTgtPlayer() && this.canTgtCreature();
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
    public final boolean hasCandidates(final SpellAbility sa, final boolean isTargeted) {
        if (this.canTgtPlayer()) {
            return true;
        }

        for (final Card c : AllZoneUtil.getCardsIn(this.tgtZone)) {
            if (c.isValid(this.validTgts, this.srcCard.getController(), this.srcCard)
                    && (!isTargeted || c.canTarget(sa))) {
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
        return this.uniqueTargets;
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
