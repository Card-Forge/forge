package forge.player;

/*import com.google.common.base.Predicate;
import com.google.common.collect.Multimap;

import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameObject;
import forge.game.GameType;
import forge.game.ability.effects.CharmEffect;
import forge.game.card.Card;
import forge.game.card.CardShields;
import forge.game.card.CounterType;
import forge.game.combat.Combat;
import forge.game.cost.Cost;
import forge.game.cost.CostPart;
import forge.game.cost.CostPartMana;
import forge.game.mana.Mana;
import forge.game.phase.PhaseType;
import forge.game.player.LobbyPlayer;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.player.PlayerController;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.spellability.TargetChoices;
import forge.game.trigger.Trigger;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.Lang;
import forge.util.TextUtil;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.*;


*//** 
 * A prototype for player controller class
 * 
 * Handles phase skips for now.
 *//*
public class PlayerControllerHuman extends PlayerController {
    public PlayerControllerHuman(Game game0, Player p, LobbyPlayer lp) {
        super(game0, p, lp);
    }

    @Override
    public SpellAbility getAbilityToPlay(List<SpellAbility> abilities,
            MouseEvent triggerEvent) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void playSpellAbilityForFree(SpellAbility copySA,
            boolean mayChoseNewTargets) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void playSpellAbilityNoStack(SpellAbility effectSA,
            boolean mayChoseNewTargets) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public List<PaperCard> sideboard(Deck deck, GameType gameType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<PaperCard> chooseCardsYouWonToAddToDeck(List<PaperCard> losses) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<Card, Integer> assignCombatDamage(Card attacker,
            List<Card> blockers, int damageDealt, GameEntity defender,
            boolean overrideOrder) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Integer announceRequirements(SpellAbility ability, String announce,
            boolean allowZero) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Card> choosePermanentsToSacrifice(SpellAbility sa, int min,
            int max, List<Card> validTargets, String message) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Card> choosePermanentsToDestroy(SpellAbility sa, int min,
            int max, List<Card> validTargets, String message) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TargetChoices chooseNewTargetsFor(SpellAbility ability) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean chooseTargetsFor(SpellAbility currentAbility) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Pair<SpellAbilityStackInstance, GameObject> chooseTarget(
            SpellAbility sa,
            List<Pair<SpellAbilityStackInstance, GameObject>> allTargets) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Card> chooseCardsForEffect(List<Card> sourceList,
            SpellAbility sa, String title, int min, int max, boolean isOptional) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T extends GameEntity> T chooseSingleEntityForEffect(
            Collection<T> sourceList, SpellAbility sa, String title,
            boolean isOptional, Player relatedPlayer) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SpellAbility chooseSingleSpellForEffect(List<SpellAbility> spells,
            SpellAbility sa, String title) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean confirmAction(SpellAbility sa, PlayerActionConfirmMode mode,
            String message) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean confirmStaticApplication(Card hostCard, GameEntity affected,
            String logic, String message) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean confirmTrigger(SpellAbility sa, Trigger regtrig,
            Map<String, String> triggerParams, boolean isMandatory) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean getWillPlayOnFirstTurn(boolean isFirstGame) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<Card> orderBlockers(Card attacker, List<Card> blockers) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Card> orderAttackers(Card blocker, List<Card> attackers) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void reveal(Collection<Card> cards, ZoneType zone, Player owner,
            String messagePrefix) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void notifyOfValue(SpellAbility saSource, GameObject realtedTarget,
            String value) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public ImmutablePair<List<Card>, List<Card>> arrangeForScry(List<Card> topN) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean willPutCardOnTop(Card c) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<Card> orderMoveToZoneList(List<Card> cards,
            ZoneType destinationZone) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Card> chooseCardsToDiscardFrom(Player playerDiscard,
            SpellAbility sa, List<Card> validCards, int min, int max) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void playMiracle(SpellAbility miracle, Card card) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public List<Card> chooseCardsToDelve(int colorLessAmount, List<Card> grave) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Card> chooseCardsToRevealFromHand(int min, int max,
            List<Card> valid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Card> chooseCardsToDiscardUnlessType(int min, List<Card> hand,
            String param, SpellAbility sa) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<SpellAbility> chooseSaToActivateFromOpeningHand(
            List<SpellAbility> usableFromOpeningHand) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Mana chooseManaFromPool(List<Mana> manaChoices) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String chooseSomeType(String kindOfType, SpellAbility sa,
            List<String> validTypes, List<String> invalidTypes,
            boolean isOptional) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Pair<CounterType, String> chooseAndRemoveOrPutCounter(
            Card cardWithCounter) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean confirmReplacementEffect(
            ReplacementEffect replacementEffect, SpellAbility effectSA,
            String question) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<Card> getCardsToMulligan(boolean isCommander, Player firstPlayer) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void declareAttackers(Player attacker, Combat combat) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void declareBlockers(Player defender, Combat combat) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public SpellAbility chooseSpellAbilityToPlay() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void playChosenSpellAbility(SpellAbility sa) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public List<Card> chooseCardsToDiscardToMaximumHandSize(int numDiscard) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean payManaOptional(Card card, Cost cost, SpellAbility sa,
            String prompt, ManaPaymentPurpose purpose) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int chooseNumber(SpellAbility sa, String title, int min, int max) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int chooseNumber(SpellAbility sa, String title,
            List<Integer> values, Player relatedPlayer) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean chooseBinary(SpellAbility sa, String question,
            BinaryChoiceType kindOfChoice, Boolean defaultChioce) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean chooseFlipResult(SpellAbility sa, Player flipper,
            boolean[] results, boolean call) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Card chooseProtectionShield(GameEntity entityBeingDamaged,
            List<String> options, Map<String, Card> choiceMap) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<AbilitySub> chooseModeForAbility(SpellAbility sa, int min,
            int num) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public byte chooseColor(String message, SpellAbility sa, ColorSet colors) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public byte chooseColorAllowColorless(String message, Card c,
            ColorSet colors) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public PaperCard chooseSinglePaperCard(SpellAbility sa, String message,
            Predicate<PaperCard> cpp, String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String> chooseColors(String message, SpellAbility sa, int min,
            int max, List<String> options) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CounterType chooseCounterType(Collection<CounterType> options,
            SpellAbility sa, String prompt) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean confirmPayment(CostPart costPart, String string) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public ReplacementEffect chooseSingleReplacementEffect(String prompt,
            List<ReplacementEffect> possibleReplacers,
            HashMap<String, Object> runParams) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String chooseProtectionType(String string, SpellAbility sa,
            List<String> choices) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CardShields chooseRegenerationShield(Card c) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean payCostToPreventEffect(Cost cost, SpellAbility sa,
            boolean alreadyPaid, List<Player> allPayers) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void orderAndPlaySimultaneousSa(List<SpellAbility> activePlayerSAs) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void playTrigger(Card host, WrappedAbility wrapperAbility,
            boolean isMandatory) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean playSaFromPlayEffect(SpellAbility tgtSA) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Map<GameEntity, CounterType> chooseProliferation() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean chooseCardsPile(SpellAbility sa, List<Card> pile1,
            List<Card> pile2, boolean faceUp) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void revealAnte(String message,
            Multimap<Player, PaperCard> removedAnteCards) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean payManaCost(ManaCost toPay, CostPartMana costPartMana,
            SpellAbility sa, String prompt, boolean isActivatedAbility) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Map<Card, ManaCostShard> chooseCardsForConvoke(SpellAbility sa,
            ManaCost manaCost, List<Card> untappedCreats) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String chooseCardName(SpellAbility sa, Predicate<PaperCard> cpp,
            String valid, String message) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Card chooseSingleCardForZoneChange(ZoneType destination,
            List<ZoneType> origin, SpellAbility sa, List<Card> fetchList,
            String selectPrompt, boolean b, Player decider) {
        // TODO Auto-generated method stub
        return null;
    }
}*/
