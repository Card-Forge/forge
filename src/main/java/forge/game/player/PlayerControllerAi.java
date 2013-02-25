package forge.game.player;

import java.security.InvalidParameterException;
import java.util.List;
import java.util.Map;

import forge.Card;
import forge.GameEntity;
import forge.card.ability.ApiType;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.control.input.Input;
import forge.deck.Deck;
import forge.game.GameState;
import forge.game.GameType;
import forge.game.ai.AiController;
import forge.game.ai.AiInputBlock;
import forge.game.ai.AiInputCommon;
import forge.game.ai.ComputerUtil;
import forge.game.ai.ComputerUtilCombat;
import forge.gui.GuiChoose;


/** 
 * A prototype for player controller class
 * 
 * Handles phase skips for now.
 */
public class PlayerControllerAi extends PlayerController {

    private Input defaultInput;
    private Input blockInput;
    private Input cleanupInput;
    
    private final AiController brains;
    private final AIPlayer player;
    

    public final Input getDefaultInput() {
        return defaultInput;
    }

    public PlayerControllerAi(GameState game, AIPlayer p) {
        super(game);
        player = p;
        
        brains = new AiController(p, game); 
        
        defaultInput = new AiInputCommon(brains);
        blockInput = new AiInputBlock(game, getPlayer());
        cleanupInput = getDefaultInput();
        
    }

    /**
     * Uses GUI to learn which spell the player (human in our case) would like to play
     */
    public SpellAbility getAbilityToPlay(List<SpellAbility> abilities) {
        if (abilities.size() == 0) {
            return null;
        } else if (abilities.size() == 1) {
            return abilities.get(0);
        } else {
            return GuiChoose.oneOrNone("Choose", abilities); // some day network interaction will be here
        }
    }

    /** Input to use when player has to declare blockers */
    public Input getBlockInput() {
        return blockInput;
    }

    /**
     * @return the cleanupInput
     */
    public Input getCleanupInput() {
        return cleanupInput;
    }

    /**
     * TODO: Write javadoc for this method.
     * @param c
     */
    public void playFromSuspend(Card c) {
        final List<SpellAbility> choices = c.getBasicSpells();
        c.setSuspendCast(true);
        for (final SpellAbility sa : choices) {
            //Spells
            if (sa instanceof Spell) {
                Spell spell = (Spell) sa;
                if (!spell.canPlayFromEffectAI(true, true)) {
                    continue;
                }
            } else {
                if (sa.canPlayAI()) {
                    continue;
                }
            }
            
            ComputerUtil.playSpellAbilityWithoutPayingManaCost(player, sa, game);
            break;
        }
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#playCascade(java.util.List, forge.Card)
     */
    @Override
    public boolean playCascade(Card cascadedCard, Card source) {
        final List<SpellAbility> choices = cascadedCard.getBasicSpells();

        for (final SpellAbility sa : choices) {
            sa.setActivatingPlayer(getPlayer());
            //Spells
            if (sa instanceof Spell) {
                Spell spell = (Spell) sa;
                if (!spell.canPlayFromEffectAI(false, true)) {
                    continue;
                }
            } else {
                if (!sa.canPlayAI()) {
                    continue;
                }
            }
            ComputerUtil.playSpellAbilityWithoutPayingManaCost(player, sa, game);
            return true;
        }
        return false;
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public AiController getAi() {
        return brains;
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#mayPlaySpellAbilityForFree(forge.card.spellability.SpellAbility)
     */
    @Override
    public void mayPlaySpellAbilityForFree(SpellAbility copySA) {
        if (copySA instanceof Spell) {
            Spell spell = (Spell) copySA;
            if (spell.canPlayFromEffectAI(false, true)) {
                ComputerUtil.playStackFree(getPlayer(), copySA);
            }
        } else if (copySA.canPlayAI()) {
            ComputerUtil.playStackFree(getPlayer(), copySA);
        }
    }

    @Override
    protected Player getPlayer() {
        return player;
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#sideboard(forge.deck.Deck)
     */
    @Override
    public Deck sideboard(Deck deck, GameType gameType) {
        // AI does not know how to sideboard
        return deck;
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#assignCombatDamage(forge.Card, java.util.List, int, forge.GameEntity)
     */
    @Override
    public Map<Card, Integer> assignCombatDamage(Card attacker, List<Card> blockers, int damageDealt, GameEntity defender) {
        return ComputerUtilCombat.distributeAIDamage(attacker, blockers, damageDealt, defender);
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#announceRequirements()
     */
    @Override
    public String announceRequirements(SpellAbility ability, String announce) {
        // For now, these "announcements" are made within the AI classes of the appropriate SA effects
        return null;
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#choosePermanentsToSacrifice(java.util.List, int, forge.card.spellability.SpellAbility, boolean, boolean)
     */
    @Override
    public List<Card> choosePermanentsToSacrifice(List<Card> validTargets, int amount, SpellAbility sa, boolean destroy, boolean isOptional) {
        return ComputerUtil.choosePermanentsToSacrifice(player, validTargets, amount, sa, destroy, isOptional);
    }

    @Override
    public Card chooseSingleCardForEffect(List<Card> options, SpellAbility sa, String title, boolean isOptional) {
        ApiType api = sa.getApi();
        if ( null == api ) {
            throw new InvalidParameterException("SA is not api-based, this is not supported yet");
        }
        
        switch(api) {
            case Bond: return CardFactoryUtil.getBestCreatureAI(options);
            default: throw new InvalidParameterException("AI chooseSingleCard does not know how to choose card for " + api);
        }
    }

}
