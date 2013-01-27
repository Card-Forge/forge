package forge.game.player;

import java.util.List;

import forge.Card;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.control.input.Input;
import forge.game.GameState;
import forge.game.ai.AiController;
import forge.game.ai.AiInputBlock;
import forge.game.ai.AiInputCommon;
import forge.game.ai.ComputerUtil;
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
    

    public final Input getDefaultInput() {
        return defaultInput;
    }

    public PlayerControllerAi(GameState game, AIPlayer p) {
        super(game, p);
        
        brains = new AiController(player, game); 
        
        defaultInput = new AiInputCommon(brains);
        blockInput = new AiInputBlock(game, player);
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
            sa.setActivatingPlayer(player);
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
                ComputerUtil.playStackFree(player, copySA);
            }
        } else if (copySA.canPlayAI()) {
            ComputerUtil.playStackFree(player, copySA);
        }
    }



}
