package forge.game.card;

import java.util.Set;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import forge.card.CardCharacteristicName;
import forge.card.CardRarity;
import forge.card.CardType;
import forge.card.ColorSet;
import forge.card.mana.ManaCost;
import forge.game.GameEntityView;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.trackable.TrackableCollection;
import forge.trackable.TrackableObject;
import forge.trackable.TrackableProperty;


public class CardView extends GameEntityView {
    public static CardView get(Card c) {
        return c == null ? null : c.getView();
    }

    public static TrackableCollection<CardView> getCollection(Iterable<Card> cards) {
        if (cards == null) {
            return null;
        }
        TrackableCollection<CardView> collection = new TrackableCollection<CardView>();
        for (Card c : cards) {
            collection.add(c.getView());
        }
        return collection;
    }

    public CardView(int id0) {
        super(id0);
        set(TrackableProperty.Original, new CardStateView(id0));
    }

    public PlayerView getOwner() {
        return get(TrackableProperty.Owner);
    }
    void updateOwner(Card c) {
        set(TrackableProperty.Owner, PlayerView.get(c.getOwner()));
    }

    public PlayerView getController() {
        return get(TrackableProperty.Controller);
    }
    void updateController(Card c) {
        set(TrackableProperty.Controller, PlayerView.get(c.getController()));
    }

    public ZoneType getZone() {
        return get(TrackableProperty.Zone);
    }
    void updateZone(Card c) {
        set(TrackableProperty.Zone, c.getZone() == null ? null : c.getZone().getZoneType());
    }

    public boolean isCloned() {
        return get(TrackableProperty.Cloned);
    }

    public boolean isFaceDown() {
        return get(TrackableProperty.FaceDown);
    }

    public boolean isFlipCard() {
        return get(TrackableProperty.FlipCard);
    }

    public boolean isFlipped() {
        return get(TrackableProperty.Flipped);
    }

    public boolean isSplitCard() {
        return get(TrackableProperty.SplitCard);
    }

    public boolean isTransformed() {
        return get(TrackableProperty.Transformed);
    }

    public String getSetCode() {
        return get(TrackableProperty.SetCode);
    }
    void updateSetCode(Card c) {
        set(TrackableProperty.SetCode, c.getCurSetCode());
    }

    public CardRarity getRarity() {
        return get(TrackableProperty.Rarity);
    }
    void updateRarity(Card c) {
        set(TrackableProperty.Rarity, c.getRarity());
    }

    public boolean isAttacking() {
        return get(TrackableProperty.Attacking);
    }
    void updateAttacking(Card c) {
        set(TrackableProperty.Attacking, c.getGame().getCombat().isAttacking(c));
    }

    public boolean isBlocking() {
        return get(TrackableProperty.Blocking);
    }
    void updateBlocking(Card c) {
        set(TrackableProperty.Blocking, c.getGame().getCombat().isBlocking(c));
    }

    public boolean isPhasedOut() {
        return get(TrackableProperty.PhasedOut);
    }
    void updatePhasedOut(Card c) {
        set(TrackableProperty.PhasedOut, c.isPhasedOut());
    }

    public boolean isFirstTurnControlled() {
        return get(TrackableProperty.Sickness);
    }
    public boolean hasSickness() {
        return isFirstTurnControlled() && !getOriginal().hasHaste();
    }
    public boolean isSick() {
        return getZone() == ZoneType.Battlefield && hasSickness();
    }
    void updateSickness(Card c) {
        set(TrackableProperty.Sickness, c.isInPlay() && c.isSick());
    }

    public boolean isTapped() {
        return get(TrackableProperty.Tapped);
    }
    void updateTapped(Card c) {
        set(TrackableProperty.Tapped, c.isTapped());
    }

    public boolean isToken() {
        return get(TrackableProperty.Token);
    }
    void updateToken(Card c) {
        set(TrackableProperty.Token, c.isToken());
    }

    public Map<CounterType, Integer> getCounters() {
        return get(TrackableProperty.Counters);
    }
    void updateCounters(Card c) {
        set(TrackableProperty.Counters, c.getCounters());
    }

    public int getDamage() {
        return get(TrackableProperty.Damage);
    }
    void updateDamage(Card c) {
        set(TrackableProperty.Damage, c.getDamage());
    }

    public int getAssignedDamage() {
        return get(TrackableProperty.AssignedDamage);
    }
    void updateAssignedDamage(Card c) {
        set(TrackableProperty.AssignedDamage, c.getTotalAssignedDamage());
    }

    public int getLethalDamage() {
        return getOriginal().getToughness() - getDamage() - getAssignedDamage();
    }

    public int getShieldCount() {
        return get(TrackableProperty.ShieldCount);
    }
    void updateShieldCount(Card c) {
        set(TrackableProperty.ShieldCount, c.getShieldCount());
    }

    public String getChosenType() {
        return get(TrackableProperty.ChosenType);
    }
    void updateChosenType(Card c) {
        set(TrackableProperty.ChosenType, c.getChosenType());
    }

    public List<String> getChosenColors() {
        return get(TrackableProperty.ChosenColors);
    }
    void updateChosenColors(Card c) {
        set(TrackableProperty.ChosenColors, c.getChosenColors());
    }

    public PlayerView getChosenPlayer() {
        return get(TrackableProperty.ChosenPlayer);
    }
    void updateChosenPlayer(Card c) {
        set(TrackableProperty.ChosenPlayer, c.getChosenPlayer());
    }

    public String getNamedCard() {
        return get(TrackableProperty.NamedCard);
    }
    void updateNamedCard(Card c) {
        set(TrackableProperty.NamedCard, c.getNamedCard());
    }

    public CardView getEquipping() {
        return get(TrackableProperty.Equipping);
    }

    public Iterable<CardView> getEquippedBy() {
        return get(TrackableProperty.EquippedBy);
    }

    public boolean isEquipped() {
        return getEquippedBy() != null;
    }

    public GameEntityView getEnchanting() {
        return get(TrackableProperty.Enchanting);
    }
    void updateEnchanting(Card c) {
        set(TrackableProperty.Owner, GameEntityView.get(c.getEnchanting()));
    }

    public CardView getEnchantingCard() {
        GameEntityView enchanting = getEnchanting();
        if (enchanting instanceof CardView) {
            return (CardView) enchanting;
        }
        return null;
    }
    public PlayerView getEnchantingPlayer() {
        GameEntityView enchanting = getEnchanting();
        if (enchanting instanceof PlayerView) {
            return (PlayerView) enchanting;
        }
        return null;
    }

    public CardView getFortifying() {
        return get(TrackableProperty.Fortifying);
    }

    public Iterable<CardView> getFortifiedBy() {
        return get(TrackableProperty.FortifiedBy);
    }

    public boolean isFortified() {
        return getFortifiedBy() != null;
    }

    public Iterable<CardView> getGainControlTargets() {
        return get(TrackableProperty.GainControlTargets);
    }

    public CardView getCloneOrigin() {
        return get(TrackableProperty.CloneOrigin);
    }

    public Iterable<CardView> getImprintedCards() {
        return get(TrackableProperty.ImprintedCards);
    }

    public Iterable<CardView> getHauntedBy() {
        return get(TrackableProperty.HauntedBy);
    }

    public CardView getHaunting() {
        return get(TrackableProperty.Haunting);
    }

    public Iterable<CardView> getMustBlockCards() {
        return get(TrackableProperty.MustBlockCards);
    }

    public CardView getPairedWith() {
        return get(TrackableProperty.PairedWith);
    }

    public CardStateView getOriginal() {
        return get(TrackableProperty.Original);
    }

    public CardStateView getAlternate() {
        return get(TrackableProperty.Alternate);
    }

    public CardStateView getState(final boolean alternate0) {
        return alternate0 ? getAlternate() : getOriginal();
    }
    void updateState(Card c, boolean fromCurStateChange) {
        boolean isDoubleFaced = c.isDoubleFaced();
        boolean isFaceDown = c.isFaceDown();
        boolean isFlipCard = c.isFlipCard();
        boolean isFlipped = c.getCurState() == CardCharacteristicName.Flipped;
        boolean isSplitCard = c.isSplitCard();
        boolean isTransformed = c.getCurState() == CardCharacteristicName.Transformed;
        boolean hasAltState = isDoubleFaced || isFlipCard || isSplitCard || (isFaceDown/* && mayShowCardFace*/);

        set(TrackableProperty.Alternate, hasAltState ? new CardStateView(getId()) : null);
        set(TrackableProperty.Cloned, c.isCloned());
        set(TrackableProperty.FaceDown, isFaceDown);
        set(TrackableProperty.SplitCard, isSplitCard);
        set(TrackableProperty.FlipCard, isFlipCard);

        if (fromCurStateChange) {
            set(TrackableProperty.Flipped, isFlipped);
            set(TrackableProperty.Transformed, isTransformed);
            updateRarity(c); //rarity and set based on current state
            updateSetCode(c);
        }

        if (isSplitCard) {
            final CardCharacteristicName orig, alt;
            if (c.getCurState() == CardCharacteristicName.RightSplit) {
                // If right half on stack, place it first
                orig = CardCharacteristicName.RightSplit;
                alt = CardCharacteristicName.LeftSplit;
            }
            else {
                orig = CardCharacteristicName.LeftSplit;
                alt = CardCharacteristicName.RightSplit;
            }
            updateState(c, getOriginal(), orig);
            updateState(c, getAlternate(), alt);
            return;
        }

        final CardStateView origView = getOriginal();
        origView.updateName(c);
        origView.updateColors(c);
        origView.updateImageKey(c);
        origView.updateType(c);
        origView.updateManaCost(c);
        origView.updatePower(c);
        origView.updateToughness(c);
        origView.updateLoyalty(c);
        origView.updateText(c);
        origView.updateChangedColorWords(c);
        origView.updateChangedTypes(c);
        origView.updateManaCost(c);
        origView.updateKeywords(c);
        origView.updateFoilIndex(c);

        if (hasAltState) {
            if (isFlipCard && !isFlipped) {
                updateState(c, getAlternate(), CardCharacteristicName.Flipped);
            }
            else if (isDoubleFaced && !isTransformed) {
                updateState(c, getAlternate(), CardCharacteristicName.Transformed);
            }
            else {
                updateState(c, getAlternate(), CardCharacteristicName.Original);
            }
        }
    }
    private void updateState(Card c, CardStateView view, CardCharacteristicName state) {
        final CardCharacteristics chars = c.getState(state);
        if (chars == null) { return; } //can happen when split card initialized before both sides have been initialized

        view.updateName(chars);
        view.updateColors(chars);
        view.updateImageKey(chars);
        view.updateType(chars);
        view.updateManaCost(chars);
        view.updatePower(chars);
        view.updateToughness(chars);
        view.updateLoyalty(chars); 
        view.updateText(chars);
        view.updateFoilIndex(chars);
    }

    @Override
    public String toString() {
        if (getId() <= 0) { //if fake card, just return name
            return getOriginal().getName();
        }

        /*if (!mayBeShown) {
            return "(Unknown card)";
        }*/

        if (StringUtils.isEmpty(getOriginal().getName())) {
            CardStateView alternate = getAlternate();
            if (alternate != null) {
                return "Face-down card (" + getAlternate().getName() + ")";
            }
            return "(" + getId() + ")";
        }
        return getOriginal().getName() + " (" + getId() + ")";
    }

    public String determineName(final CardStateView state) {
        if (state == getOriginal()) {
            return toString();
        }
        return getAlternate().getName() + " (" + getId() + ")";
    }

    public class CardStateView extends TrackableObject {
        public CardStateView(int id0) {
            super(id0);
        }

        @Override
        public String toString() {
            return getCard().determineName(this);
        }

        public CardView getCard() {
            return CardView.this;
        }

        public String getName() {
            return get(TrackableProperty.Name);
        }
        void updateName(Card c) {
            set(TrackableProperty.Name, c.getName());
        }
        void updateName(CardCharacteristics c) {
            set(TrackableProperty.Name, c.getName());
        }

        public ColorSet getColors() {
            return get(TrackableProperty.Colors);
        }
        void updateColors(Card c) {
            set(TrackableProperty.Colors, c.determineColor());
        }
        void updateColors(CardCharacteristics c) {
            set(TrackableProperty.Colors, c.determineColor());
        }

        public String getImageKey() {
            return get(TrackableProperty.ImageKey);
        }
        void updateImageKey(Card c) {
            set(TrackableProperty.ImageKey, c.getImageKey());
        }
        void updateImageKey(CardCharacteristics c) {
            set(TrackableProperty.ImageKey, c.getImageKey());
        }

        public Set<String> getType() {
            return get(TrackableProperty.Type);
        }
        void updateType(Card c) {
            set(TrackableProperty.Type, c.getType());
        }
        void updateType(CardCharacteristics c) {
            set(TrackableProperty.Type, c.getType());
        }

        public ManaCost getManaCost() {
            return get(TrackableProperty.ManaCost);
        }
        void updateManaCost(Card c) {
            set(TrackableProperty.ManaCost, c.getManaCost());
        }
        void updateManaCost(CardCharacteristics c) {
            set(TrackableProperty.ManaCost, c.getManaCost());
        }

        public int getPower() {
            return get(TrackableProperty.Power);
        }
        void updatePower(Card c) {
            set(TrackableProperty.Power, c.getNetAttack());
        }
        void updatePower(CardCharacteristics c) {
            set(TrackableProperty.Power, c.getBaseAttack());
        }

        public int getToughness() {
            return get(TrackableProperty.Toughness);
        }
        void updateToughness(Card c) {
            set(TrackableProperty.Toughness, c.getNetDefense());
        }
        void updateToughness(CardCharacteristics c) {
            set(TrackableProperty.Toughness, c.getBaseDefense());
        }

        public int getLoyalty() {
            return get(TrackableProperty.Loyalty);
        }
        void updateLoyalty(Card c) {
            set(TrackableProperty.Loyalty, c.getCurrentLoyalty());
        }
        void updateLoyalty(CardCharacteristics c) {
            set(TrackableProperty.Loyalty, 0); // Q why is loyalty not a property of CardCharacteristic? A: because no alt states have a base loyalty (only candidate is Garruk Relentless).
        }

        public String getText() {
            return get(TrackableProperty.Text);
        }
        void updateText(Card c) {
            set(TrackableProperty.Text, c.getText());
        }
        void updateText(CardCharacteristics c) {
            set(TrackableProperty.Text, c.getOracleText());
        }

        public int getFoilIndex() {
            return get(TrackableProperty.FoilIndex);
        }
        void updateFoilIndex(Card c) {
            set(TrackableProperty.FoilIndex, c.getCharacteristics().getFoil());
        }
        void updateFoilIndex(CardCharacteristics c) {
            set(TrackableProperty.FoilIndex, c.getFoil());
        }

        public Map<String, String> getChangedColorWords() {
            return get(TrackableProperty.ChangedColorWords);
        }
        void updateChangedColorWords(Card c) {
            set(TrackableProperty.ChangedColorWords, c.getChangedTextColorWords());
        }

        public Map<String, String> getChangedTypes() {
            return get(TrackableProperty.ChangedTypes);
        }
        void updateChangedTypes(Card c) {
            set(TrackableProperty.ChangedTypes, c.getChangedTextTypeWords());
        }

        public boolean hasDeathtouch() {
            return get(TrackableProperty.HasDeathtouch);
        }
        public boolean hasHaste() {
            return get(TrackableProperty.HasHaste);
        }
        public boolean hasInfect() {
            return get(TrackableProperty.HasInfect);
        }
        public boolean hasStorm() {
            return get(TrackableProperty.HasStorm);
        }
        public boolean hasTrample() {
            return get(TrackableProperty.HasTrample);
        }
        void updateKeywords(Card c) {
            set(TrackableProperty.HasDeathtouch, c.hasKeyword("Deathtouch"));
            set(TrackableProperty.HasHaste, c.hasKeyword("Haste"));
            set(TrackableProperty.HasInfect, c.hasKeyword("Infect"));
            set(TrackableProperty.HasStorm, c.hasKeyword("Storm"));
            set(TrackableProperty.HasTrample, c.hasKeyword("Trample"));
        }

        public boolean isBasicLand() {
            return isLand() && Iterables.any(getType(), Predicates.in(CardType.getBasicTypes()));
        }
        public boolean isCreature() {
            return getType().contains("Creature");
        }
        public boolean isLand() {
            return getType().contains("Land");
        }
        public boolean isPlane() {
            return getType().contains("Plane");
        }
        public boolean isPhenomenon() {
            return getType().contains("Phenomenon");
        }
        public boolean isPlaneswalker() {
            return getType().contains("Planeswalker");
        }
    }

    //special methods for updating card and player properties as needed and returning the new collection
    Card setCard(Card oldCard, Card newCard, TrackableProperty key) {
        if (newCard != oldCard) {
            set(key, CardView.get(newCard));
        }
        return newCard;
    }
    CardCollection setCards(CardCollection oldCards, CardCollection newCards, TrackableProperty key) {
        set(key, CardView.getCollection(newCards)); //TODO prevent overwriting list if not necessary
        return newCards;
    }
    CardCollection setCards(CardCollection oldCards, Iterable<Card> newCards, TrackableProperty key) {
        if (newCards == null) {
            set(key, null);
            return null;
        }
        return setCards(oldCards, new CardCollection(newCards), key);
    }
    CardCollection addCard(CardCollection oldCards, Card cardToAdd, TrackableProperty key) {
        if (cardToAdd == null) { return oldCards; }

        if (oldCards == null) {
            oldCards = new CardCollection();
        }
        if (oldCards.add(cardToAdd)) {
            TrackableCollection<CardView> views = get(key);
            if (views == null) {
                views = new TrackableCollection<CardView>();
                views.add(cardToAdd.getView());;
                set(key, views);
            }
            else if (views.add(cardToAdd.getView())) {
                flagAsChanged(key);
            }
        }
        return oldCards;
    }
    CardCollection addCards(CardCollection oldCards, Iterable<Card> cardsToAdd, TrackableProperty key) {
        if (cardsToAdd == null) { return oldCards; }

        TrackableCollection<CardView> views = get(key);
        if (oldCards == null) {
            oldCards = new CardCollection();
        }
        boolean needFlagAsChanged = false;
        for (Card c : cardsToAdd) {
            if (oldCards.add(c)) {
                if (views == null) {
                    views = new TrackableCollection<CardView>();
                    views.add(c.getView());
                    set(key, views);
                }
                else if (views.add(c.getView())) {
                    needFlagAsChanged = true;
                }
            }
        }
        if (needFlagAsChanged) {
            flagAsChanged(key);
        }
        return oldCards;
    }
    CardCollection removeCard(CardCollection oldCards, Card cardToRemove, TrackableProperty key) {
        if (cardToRemove == null || oldCards == null) { return oldCards; }

        if (oldCards.remove(cardToRemove)) {
            TrackableCollection<CardView> views = get(key);
            if (views != null && views.remove(cardToRemove.getView())) {
                if (views.isEmpty()) {
                    set(key, null); //avoid keeping around an empty collection
                }
                else {
                    flagAsChanged(key);
                }
            }
            if (oldCards.isEmpty()) {
                oldCards = null; //avoid keeping around an empty collection
            }
        }
        return oldCards;
    }
    CardCollection removeCards(CardCollection oldCards, Iterable<Card> cardsToRemove, TrackableProperty key) {
        if (cardsToRemove == null || oldCards == null) { return oldCards; }

        TrackableCollection<CardView> views = get(key);
        boolean needFlagAsChanged = false;
        for (Card c : cardsToRemove) {
            if (oldCards.remove(c)) {
                if (views != null && views.remove(c.getView())) {
                    if (views.isEmpty()) {
                        views = null;
                        set(key, null); //avoid keeping around an empty collection
                        needFlagAsChanged = false; //doesn't need to be flagged a second time
                    }
                    else {
                        needFlagAsChanged = true;
                    }
                }
                if (oldCards.isEmpty()) {
                    oldCards = null; //avoid keeping around an empty collection
                    break;
                }
            }
        }
        if (needFlagAsChanged) {
            flagAsChanged(key);
        }
        return oldCards;
    }
    CardCollection clearCards(CardCollection oldCards, TrackableProperty key) {
        if (oldCards != null) {
            set(key, null);
        }
        return null;
    }
}
