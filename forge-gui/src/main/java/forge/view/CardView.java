package forge.view;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import forge.ImageKeys;
import forge.card.CardEdition;
import forge.card.CardRarity;
import forge.card.CardType;
import forge.card.ColorSet;
import forge.card.mana.ManaCost;
import forge.game.card.CounterType;
import forge.game.zone.ZoneType;

/**
 * Representation of a {@link forge.game.card.Card}, containing only the
 * information relevant to a user interface.
 * 
 * Conversion from and to Cards happens through {@link LocalGameView}.
 * 
 * @author elcnesh
 */
public class CardView extends GameEntityView {
    private final CardStateView
        original = new CardStateView(),
        alternate = new CardStateView();

    private boolean hasAltState;

    private final int id;
    private boolean mayBeShown;
    private PlayerView owner, controller;
    private ZoneType zone;
    private boolean isCloned, isFaceDown, isFlipCard, isFlipped, isSplitCard, isTransformed;
    private String setCode;
    private CardRarity rarity;
    private boolean isAttacking, isBlocking, isPhasedOut, isSick, isTapped, isToken;
    private Map<CounterType, Integer> counters;
    private int damage, assignedDamage, regenerationShields, preventNextDamage;
    private String chosenType;
    private List<String> chosenColors;
    private PlayerView chosenPlayer;
    private String namedCard;
    private CardView equipping;
    private Iterable<CardView> equippedBy;
    private CardView enchantingCard;
    private PlayerView enchantingPlayer;
    private Iterable<CardView> enchantedBy;
    private CardView fortifying;
    private Iterable<CardView> fortifiedBy;
    private Iterable<CardView> gainControlTargets;
    private CardView cloneOrigin;
    private Iterable<CardView> imprinted, hauntedBy;
    private CardView haunting;
    private Iterable<CardView> mustBlock;
    private CardView pairedWith;

    public CardView(int id0) {
        this.id = id0;
        this.reset();
    }

    public void reset() {
        final Iterable<CardView> emptyIterable = ImmutableSet.of();
        this.mayBeShown = false;
        this.hasAltState = false;
        this.owner = null;
        this.controller = null;
        this.zone = null;
        this.isCloned = false;
        this.isFaceDown = false;
        this.isFlipped = false;
        this.isSplitCard = false;
        this.isTransformed = false;
        this.setCode = "";
        this.rarity = CardRarity.Unknown;
        this.isAttacking = this.isBlocking = this.isPhasedOut = this.isSick = this.isTapped = false;
        this.counters = ImmutableMap.of();
        this.damage = this.assignedDamage = this.regenerationShields = this.preventNextDamage = 0;
        this.chosenType = "";
        this.chosenColors = ImmutableList.of();
        this.chosenPlayer = null;
        this.namedCard = "";
        this.equipping = null;
        this.equippedBy = emptyIterable;
        this.enchantingCard = null;
        this.enchantingPlayer = null;
        this.enchantedBy = emptyIterable;
        this.fortifying = null;
        this.fortifiedBy = emptyIterable;
        this.gainControlTargets = emptyIterable;
        this.cloneOrigin = null;
        this.imprinted = emptyIterable;
        this.hauntedBy = emptyIterable;
        this.haunting = null;
        this.mustBlock = emptyIterable;

        this.original.reset();
        this.alternate.reset();
    }

    /**
     * @return the id
     */
    @Override
    public int getId() {
        return id;
    }

    public boolean mayBeShown() {
        return this.mayBeShown;
    }
    public void setMayBeShown(boolean b0) {
        this.mayBeShown = b0;
    }

    /**
     * @return the owner
     */
    public PlayerView getOwner() {
        return owner;
    }

    /**
     * @param owner the owner to set
     */
    public void setOwner(PlayerView owner) {
        this.owner = owner;
    }

    /**
     * @return the controller
     */
    public PlayerView getController() {
        return controller;
    }

    /**
     * @param controller the controller to set
     */
    public void setController(final PlayerView controller) {
        this.controller = controller;
    }

    /**
     * @return the zone
     */
    public ZoneType getZone() {
        return zone;
    }

    /**
     * @param zone the zone to set
     */
    public void setZone(ZoneType zone) {
        this.zone = zone;
    }

    /**
     * @return the hasAltState
     */
    public boolean hasAltState() {
        return hasAltState;
    }

    /**
     * @param hasAltState the hasAltState to set
     */
    public void setHasAltState(final boolean hasAltState) {
        this.hasAltState = hasAltState;
    }

    /**
     * @return the isCloned
     */
    public boolean isCloned() {
        return isCloned;
    }

    /**
     * @param isCloned the isCloned to set
     */
    public void setCloned(boolean isCloned) {
        this.isCloned = isCloned;
    }

    /**
     * @return the isFaceDown
     */
    public boolean isFaceDown() {
        return isFaceDown;
    }

    /**
     * @param isFaceDown the isFaceDown to set
     */
    public void setFaceDown(final boolean isFaceDown) {
        this.isFaceDown = isFaceDown;
    }

    /**
     * @return the isFlipCard
     */
    public boolean isFlipCard() {
        return isFlipCard;
    }

    /**
     * @param isFlipCard the isFlipCard to set
     */
    public void setFlipCard(boolean isFlipCard) {
        this.isFlipCard = isFlipCard;
    }

    /**
     * @return the isFlipped
     */
    public boolean isFlipped() {
        return isFlipped;
    }

    /**
     * @param isFlipped the isFlipped to set
     */
    public void setFlipped(final boolean isFlipped) {
        this.isFlipped = isFlipped;
    }

    /**
     * @return the isSplitCard
     */
    public boolean isSplitCard() {
        return isSplitCard;
    }

    /**
     * @param isSplitCard the isSplitCard to set
     */
    public void setSplitCard(boolean isSplitCard) {
        this.isSplitCard = isSplitCard;
    }

    /**
     * @return the isTransformed
     */
    public boolean isTransformed() {
        return isTransformed;
    }

    /**
     * @param isTransformed the isTransformed to set
     */
    public void setTransformed(final boolean isTransformed) {
        this.isTransformed = isTransformed;
    }

    /**
     * @return the setCode
     */
    public String getSetCode() {
        return setCode;
    }

    /**
     * @param setCode the setCode to set
     */
    public void setSetCode(final String setCode) {
        this.setCode = setCode;
    }

    /**
     * @return the rarity
     */
    public CardRarity getRarity() {
        return rarity;
    }

    /**
     * @param rarity the rarity to set
     */
    public void setRarity(final CardRarity rarity) {
        this.rarity = rarity;
    }

    /**
     * @return the isAttacking
     */
    public boolean isAttacking() {
        return isAttacking;
    }

    /**
     * @param isAttacking the isAttacking to set
     */
    public void setAttacking(boolean isAttacking) {
        this.isAttacking = isAttacking;
    }

    /**
     * @return the isBlocking
     */
    public boolean isBlocking() {
        return isBlocking;
    }

    /**
     * @param isBlocking the isBlocking to set
     */
    public void setBlocking(boolean isBlocking) {
        this.isBlocking = isBlocking;
    }

    /**
     * @return the isPhasedOut
     */
    public boolean isPhasedOut() {
        return isPhasedOut;
    }

    /**
     * @param isPhasedOut the isPhasedOut to set
     */
    public void setPhasedOut(final boolean isPhasedOut) {
        this.isPhasedOut = isPhasedOut;
    }

    /**
     * @return the isSick
     */
    public boolean isSick() {
        return isSick;
    }

    /**
     * @param isSick the isSick to set
     */
    public void setSick(boolean isSick) {
        this.isSick = isSick;
    }

    /**
     * @return the isTapped
     */
    public boolean isTapped() {
        return isTapped;
    }

    /**
     * @param isTapped the isTapped to set
     */
    public void setTapped(boolean isTapped) {
        this.isTapped = isTapped;
    }

    /**
     * @return the isToken
     */
    public boolean isToken() {
        return isToken;
    }

    /**
     * @return the counters
     */
    public Map<CounterType, Integer> getCounters() {
        return counters;
    }

    /**
     * @param counters the counters to set
     */
    public void setCounters(final Map<CounterType, Integer> counters) {
        this.counters = Collections.unmodifiableMap(counters);
    }

    /**
     * @return the damage
     */
    public int getDamage() {
        return damage;
    }

    /**
     * @param damage the damage to set
     */
    public void setDamage(final int damage) {
        this.damage = damage;
    }

    /**
     * @return the assignedDamage
     */
    public int getAssignedDamage() {
        return assignedDamage;
    }

    /**
     * @param assignedDamage the assignedDamage to set
     */
    public void setAssignedDamage(final int assignedDamage) {
        this.assignedDamage = assignedDamage;
    }

    public int getLethalDamage() {
        return this.getOriginal().getToughness() - this.getDamage() - this.getAssignedDamage();
    }

    /**
     * @return the regenerationShields
     */
    public int getRegenerationShields() {
        return regenerationShields;
    }

    /**
     * @param regenerationShields the regenerationShields to set
     */
    public void setRegenerationShields(final int regenerationShields) {
        this.regenerationShields = regenerationShields;
    }

    /**
     * @return the preventNextDamage
     */
    public int getPreventNextDamage() {
        return preventNextDamage;
    }

    /**
     * @param preventNextDamage the preventNextDamage to set
     */
    public void setPreventNextDamage(final int preventNextDamage) {
        this.preventNextDamage = preventNextDamage;
    }

    /**
     * @return the chosenType
     */
    public String getChosenType() {
        return chosenType;
    }

    /**
     * @param chosenType the chosenType to set
     */
    public void setChosenType(final String chosenType) {
        this.chosenType = chosenType;
    }

    /**
     * @return the chosenColors
     */
    public List<String> getChosenColors() {
        return chosenColors;
    }

    /**
     * @param chosenColors the chosenColors to set
     */
    public void setChosenColors(final List<String> chosenColors) {
        this.chosenColors = Collections.unmodifiableList(chosenColors);
    }

    /**
     * @return the chosenPlayer
     */
    public PlayerView getChosenPlayer() {
        return chosenPlayer;
    }

    /**
     * @param chosenPlayer the chosenPlayer to set
     */
    public void setChosenPlayer(final PlayerView chosenPlayer) {
        this.chosenPlayer = chosenPlayer;
    }

    /**
     * @param isToken the isToken to set
     */
    public void setToken(final boolean isToken) {
        this.isToken = isToken;
    }

    /**
     * @return the namedCard
     */
    public String getNamedCard() {
        return namedCard;
    }

    /**
     * @param namedCard the namedCard to set
     */
    public void setNamedCard(final String namedCard) {
        this.namedCard = namedCard;
    }

    /**
     * @return the equipping
     */
    public CardView getEquipping() {
        return equipping;
    }

    /**
     * @param equipping the equipping to set
     */
    public void setEquipping(final CardView equipping) {
        this.equipping = equipping;
    }

    /**
     * @return the equippedBy
     */
    public Iterable<CardView> getEquippedBy() {
        return equippedBy;
    }

    /**
     * @param equippedBy the equippedBy to set
     */
    public void setEquippedBy(final Iterable<CardView> equippedBy) {
        this.equippedBy = Iterables.unmodifiableIterable(equippedBy);
    }

    public boolean isEquipped() {
        return this.getEquippedBy().iterator().hasNext();
    }

    /**
     * @return the enchantingCard
     */
    public CardView getEnchantingCard() {
        return enchantingCard;
    }

    /**
     * @param enchantingCard the enchantingCards to set
     */
    public void setEnchantingCard(final CardView enchantingCard) {
        this.enchantingCard = enchantingCard;
    }

    /**
     * @return the enchantingPlayer
     */
    public PlayerView getEnchantingPlayer() {
        return enchantingPlayer;
    }

    /**
     * @param enchantingPlayer the enchantingPlayer to set
     */
    public void setEnchantingPlayer(final PlayerView enchantingPlayer) {
        this.enchantingPlayer = enchantingPlayer;
    }

    /**
     * @return the enchantedBy
     */
    public Iterable<CardView> getEnchantedBy() {
        return enchantedBy;
    }

    /**
     * @param enchantedBy the enchantedBy to set
     */
    public void setEnchantedBy(final Iterable<CardView> enchantedBy) {
        this.enchantedBy = Iterables.unmodifiableIterable(enchantedBy);
    }

    public boolean isEnchanted() {
        return getEnchantedBy().iterator().hasNext();
    }

    /**
     * @return the fortifying
     */
    public CardView getFortifying() {
        return fortifying;
    }

    /**
     * @param fortifying the fortifying to set
     */
    public void setFortifying(CardView fortifying) {
        this.fortifying = fortifying;
    }

    /**
     * @return the fortifiedBy
     */
    public Iterable<CardView> getFortifiedBy() {
        return fortifiedBy;
    }

    /**
     * @param fortifiedBy the fortifiedBy to set
     */
    public void setFortifiedBy(final Iterable<CardView> fortifiedBy) {
        this.fortifiedBy = Iterables.unmodifiableIterable(fortifiedBy);
    }

    public boolean isFortified() {
        return getFortifiedBy().iterator().hasNext();
    }

    /**
     * @return the gainControlTargets
     */
    public Iterable<CardView> getGainControlTargets() {
        return gainControlTargets;
    }

    /**
     * @param gainControlTargets the gainControlTargets to set
     */
    public void setGainControlTargets(final Iterable<CardView> gainControlTargets) {
        this.gainControlTargets = Iterables.unmodifiableIterable(gainControlTargets);
    }

    /**
     * @return the cloneOrigin
     */
    public CardView getCloneOrigin() {
        return cloneOrigin;
    }

    /**
     * @param cloneOrigin the cloneOrigin to set
     */
    public void setCloneOrigin(final CardView cloneOrigin) {
        this.cloneOrigin = cloneOrigin;
    }

    /**
     * @return the imprinted
     */
    public Iterable<CardView> getImprinted() {
        return imprinted;
    }

    /**
     * @param imprinted the imprinted to set
     */
    public void setImprinted(final Iterable<CardView> imprinted) {
        this.imprinted = Iterables.unmodifiableIterable(imprinted);
    }

    /**
     * @return the hauntedBy
     */
    public Iterable<CardView> getHauntedBy() {
        return hauntedBy;
    }

    /**
     * @param hauntedBy the hauntedBy to set
     */
    public void setHauntedBy(final Iterable<CardView> hauntedBy) {
        this.hauntedBy = Iterables.unmodifiableIterable(hauntedBy);
    }

    /**
     * @return the haunting
     */
    public CardView getHaunting() {
        return haunting;
    }

    /**
     * @param haunting the haunting to set
     */
    public void setHaunting(final CardView haunting) {
        this.haunting = haunting;
    }

    /**
     * @return the mustBlock
     */
    public Iterable<CardView> getMustBlock() {
        return mustBlock;
    }

    /**
     * @param mustBlock the mustBlock to set
     */
    public void setMustBlock(final Iterable<CardView> mustBlock) {
        this.mustBlock = Iterables.unmodifiableIterable(mustBlock);
    }

    public CardView getPairedWith() {
        return pairedWith;
    }

    public void setPairedWith(final CardView pairedWith) {
        this.pairedWith = pairedWith;
    }

    public CardStateView getOriginal() {
        return this.original;
    }

    public CardStateView getAlternate() {
        return this.alternate;
    }

    public CardStateView getState(final boolean alternate) {
        return alternate ? this.alternate : this.original;
    }

    @Override
    public String toString() {
        if (this.getId() <= 0) { //if fake card, just return name
            return this.getOriginal().getName();
        }

        if (!mayBeShown) {
            return "(Unknown card)";
        }

        if (StringUtils.isEmpty(this.getOriginal().getName())) {
            if (this.hasAltState()) {
                return "Face-down card (" + this.getAlternate().getName() + ")";
            }
            return "(" + this.getId() + ")";
        }

        return this.getOriginal().getName() + " (" + this.getId() + ")";
    }

    public String determineName(final CardStateView state) {
        if (state == original) {
            return this.toString();
        }

        return this.getAlternate().getName() + " (" + this.getId() + ")";
    }

    public class CardStateView {
        private String name;
        private ColorSet colors;
        private String imageKey;
        private List<String> type;
        private ManaCost manaCost;
        private int power, toughness, loyalty;
        private String text;
        private Map<String, String> changedColorWords,
                changedTypes;
        private boolean hasDeathtouch, hasInfect, hasStorm, hasTrample;
        private int foilIndex;

        public CardStateView() {
            this.reset();
        }

        public void reset() {
            this.name = "";
            this.colors = ColorSet.getNullColor();
            this.imageKey = ImageKeys.HIDDEN_CARD;
            this.type = Collections.emptyList();
            this.manaCost = ManaCost.NO_COST;
            this.power = 0;
            this.toughness = 0;
            this.loyalty = 0;
            this.text = "";
            this.changedColorWords = ImmutableMap.of();
            this.changedTypes = ImmutableMap.of();
            this.hasDeathtouch = false;
            this.hasInfect = false;
            this.hasStorm = false;
            this.hasTrample = false;
            this.foilIndex = 0;
        }

        @Override
        public String toString() {
            return this.getCard().determineName(this);
        }

        public CardView getCard() {
            return CardView.this;
        }

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * @param name the name to set
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * @return the colors
         */
        public ColorSet getColors() {
            return colors;
        }

        /**
         * @param colors the colors to set
         */
        public void setColors(final ColorSet colors) {
            this.colors = colors;
        }

        /**
         * @return the imageKey
         */
        public String getImageKey() {
            return mayBeShown ? imageKey : ImageKeys.HIDDEN_CARD;
        }

        /**
         * @param imageKey the imageKey to set
         */
        public void setImageKey(final String imageKey) {
            this.imageKey = imageKey;
        }

        /**
         * @return the type
         */
        public List<String> getType() {
            return type;
        }

        /**
         * @param type the type to set
         */
        public void setType(final List<String> type) {
            this.type = Collections.unmodifiableList(type);
        }

        /**
         * @return the manaCost
         */
        public ManaCost getManaCost() {
            return manaCost;
        }

        /**
         * @param manaCost the manaCost to set
         */
        public void setManaCost(final ManaCost manaCost) {
            this.manaCost = manaCost;
        }

        /**
         * @return the power
         */
        public int getPower() {
            return power;
        }

        /**
         * @param power the power to set
         */
        public void setPower(final int power) {
            this.power = power;
        }

        /**
         * @return the toughness
         */
        public int getToughness() {
            return toughness;
        }

        /**
         * @param toughness the toughness to set
         */
        public void setToughness(final int toughness) {
            this.toughness = toughness;
        }

        /**
         * @return the loyalty
         */
        public int getLoyalty() {
            return loyalty;
        }

        /**
         * @param loyalty the loyalty to set
         */
        public void setLoyalty(final int loyalty) {
            this.loyalty = loyalty;
        }

        /**
         * @return the text
         */
        public String getText() {
            return text;
        }

        /**
         * @param text the text to set
         */
        public void setText(final String text) {
            this.text = text;
        }

        /**
         * @return the changedColorWords
         */
        public Map<String, String> getChangedColorWords() {
            return changedColorWords;
        }

        /**
         * @param changedColorWords the changedColorWords to set
         */
        public void setChangedColorWords(final Map<String, String> changedColorWords) {
            this.changedColorWords = Collections.unmodifiableMap(changedColorWords);
        }

        /**
         * @return the changedTypes
         */
        public Map<String, String> getChangedTypes() {
            return changedTypes;
        }

        /**
         * @param changedTypes the changedTypes to set
         */
        public void setChangedTypes(final Map<String, String> changedTypes) {
            this.changedTypes = Collections.unmodifiableMap(changedTypes);
        }

        /**
         * @return the hasDeathtouch
         */
        public boolean hasDeathtouch() {
            return hasDeathtouch;
        }

        /**
         * @param hasDeathtouch the hasDeathtouch to set
         */
        public void setHasDeathtouch(boolean hasDeathtouch) {
            this.hasDeathtouch = hasDeathtouch;
        }

        /**
         * @return the hasInfect
         */
        public boolean hasInfect() {
            return hasInfect;
        }

        /**
         * @param hasInfect the hasInfect to set
         */
        public void setHasInfect(boolean hasInfect) {
            this.hasInfect = hasInfect;
        }

        /**
         * @return the hasStorm
         */
        public boolean hasStorm() {
            return hasStorm;
        }

        /**
         * @param hasStorm the hasStorm to set
         */
        public void setHasStorm(boolean hasStorm) {
            this.hasStorm = hasStorm;
        }

        /**
         * @return the hasTrample
         */
        public boolean hasTrample() {
            return hasTrample;
        }

        /**
         * @param hasTrample the hasTrample to set
         */
        public void setHasTrample(boolean hasTrample) {
            this.hasTrample = hasTrample;
        }

        /**
         * @return the foilIndex
         */
        public int getFoilIndex() {
            return foilIndex;
        }

        /**
         * @param foilIndex the foilIndex to set
         */
        public void setFoilIndex(final int foilIndex) {
            this.foilIndex = foilIndex;
        }

        public void setRandomFoil() {
            this.setFoilIndex(CardEdition.getRandomFoil(getSetCode()));
        }

        public boolean isBasicLand() {
            return this.isLand() && Iterables.any(type, Predicates.in(CardType.getBasicTypes()));
        }
        public boolean isCreature() {
            return this.type.contains("Creature");
        }
        public boolean isLand() {
            return this.type.contains("Land");
        }
        public boolean isPlane() {
            return this.type.contains("Plane");
        }
        public boolean isPhenomenon() {
            return this.type.contains("Phenomenon");
        }
        public boolean isPlaneswalker() {
            return this.type.contains("Planeswalker");
        }
    }
}
