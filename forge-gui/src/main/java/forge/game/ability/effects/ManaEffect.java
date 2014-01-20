package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import forge.ai.ComputerUtilCard;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.mana.ManaCostShard;
import forge.game.GameActionUtil;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;

public class ManaEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getSourceCard();

        AbilityManaPart abMana = sa.getManaPart();

        // Spells are not undoable
        sa.setUndoable(sa.isAbility() && sa.isUndoable());

        final List<Player> tgtPlayers = getTargetPlayers(sa);
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final boolean optional = sa.hasParam("Optional");
        final Game game = sa.getActivatingPlayer().getGame();

        if (optional && !sa.getActivatingPlayer().getController().confirmAction(sa, null, "Do you want to add mana to your mana pool?")) {
            return;
        }
        if (abMana.isComboMana()) {
            for (Player p : tgtPlayers) {
                int amount = sa.hasParam("Amount") ? AbilityUtils.calculateAmount(card, sa.getParam("Amount"), sa) : 1;
                if (tgt == null || p.canBeTargetedBy(sa)) {
                    Player activator = sa.getActivatingPlayer();
                    // AI color choice is set in ComputerUtils so only human players need to make a choice
                    if (activator.isHuman()) {
                        //String colorsNeeded = abMana.getExpressChoice();
                        String[] colorsProduced = abMana.getComboColors().split(" ");
                        
                        
                        final StringBuilder choiceString = new StringBuilder();
                        ColorSet colorOptions = null;
                        if (!abMana.isAnyMana()) {
                            colorOptions = ColorSet.fromNames(colorsProduced);
                        }
                        else {
                            colorOptions = ColorSet.fromNames(MagicColor.Constant.ONLY_COLORS);
                        }
                        for (int nMana = 1; nMana <= amount; nMana++) {
                            String choice = "";
                            byte chosenColor = activator.getController().chooseColor("Select Mana to Produce", sa, colorOptions);
                            if (chosenColor == 0) {
                                final StringBuilder sb = new StringBuilder();
                                sb.append("AbilityFactoryMana::manaResolve() - Human color mana choice is empty for ");
                                sb.append(card.getName());
                                throw new RuntimeException(sb.toString());
                            } else {
                                choice = MagicColor.toShortString(chosenColor);
                                if (nMana != 1) {
                                    choiceString.append(" ");
                                }
                                choiceString.append(choice);
                            }
                        }
                        abMana.setExpressChoice(choiceString.toString());
                    }
                    else {
                        // TODO: Add some logic for AI choice (ArsenalNut 2012/09/16)
                        if (!sa.hasParam("AILogic") || sa.getParam("AILogic").equals("MostProminentInComputerHand")) {
                            String chosen = MagicColor.Constant.BLACK;
                            List<Card> hand = new ArrayList<Card>(activator.getCardsIn(ZoneType.Hand));
                            hand.addAll(activator.getCardsIn(ZoneType.Stack));
                            chosen = ComputerUtilCard.getMostProminentColor(hand);
                            if (chosen.equals("")) {
                                chosen = MagicColor.Constant.BLACK;
                            }
                            game.action.nofityOfValue(sa, card, "Computer picked" + chosen, activator);
                            String manaString = "";
                            for (int i = 0; i < amount; i++) {
                                manaString = manaString + MagicColor.toShortString(chosen) + " ";
                            }
                            abMana.setExpressChoice(manaString);
                        }
                        if (abMana.getExpressChoice().isEmpty() && amount > 0) {
                            System.out.println("AbilityFactoryMana::manaResolve() - combo mana color choice is empty for " + card.getName());
                        }
                    }
                }
            }
        }
        else if (abMana.isAnyMana()) {
            for (Player p : tgtPlayers) {
                if (tgt == null || p.canBeTargetedBy(sa)) {
                    Player act = sa.getActivatingPlayer();
                    // AI color choice is set in ComputerUtils so only human players need to make a choice
                    if (act.isHuman()) {
                        String colorsNeeded = abMana.getExpressChoice();
                        String choice = "";
                        if (colorsNeeded.length() == 1) {
                            choice = colorsNeeded;
                        }
                        else {
                            ColorSet colorMenu = null;
                            if (colorsNeeded.length() > 1 && colorsNeeded.length() < 5) {
                                byte mask = 0;
                                //loop through colors to make menu
                                for (int nChar = 0; nChar < colorsNeeded.length(); nChar++) {
                                    mask |= forge.card.MagicColor.fromName(colorsNeeded.substring(nChar, nChar + 1));
                                }
                                colorMenu = ColorSet.fromMask(mask);
                            }
                            else {
                                colorMenu = ColorSet.fromNames(MagicColor.Constant.ONLY_COLORS);
                            }
                            byte val = act.getController().chooseColor("Select Mana to Produce", sa, colorMenu);
                            if (0 == val) {
                                final StringBuilder sb = new StringBuilder();
                                sb.append("AbilityFactoryMana::manaResolve() - Human color mana choice is empty for ");
                                sb.append(card.getName());
                                throw new RuntimeException(sb.toString());
                            }
                            choice = MagicColor.toShortString(val);
                        }
                        abMana.setExpressChoice(choice);
                    }
                    else {
                        if (abMana.getExpressChoice().isEmpty()) {
                            final String logic = sa.hasParam("AILogic") ? sa.getParam("AILogic") : null;
                            String chosen = MagicColor.Constant.BLACK;
                            if (logic == null || logic.equals("MostProminentInComputerHand")) {
                                chosen = ComputerUtilCard.getMostProminentColor(act.getCardsIn(ZoneType.Hand));
                            }
                            if (chosen.equals("")) {
                                chosen = MagicColor.Constant.GREEN;
                            }
                            game.action.nofityOfValue(sa, card, "Computer picked " + chosen, act);
                            abMana.setExpressChoice(MagicColor.toShortString(chosen));
                        }
                        if (abMana.getExpressChoice().isEmpty()) {
                            final StringBuilder sb = new StringBuilder();
                            sb.append("AbilityFactoryMana::manaResolve() - any color mana choice is empty for ");
                            sb.append(card.getName());
                            throw new RuntimeException(sb.toString());
                        }
                    }
                }
            }
        }
        else if (abMana.isSpecialMana()) {
            for (Player p : tgtPlayers) {
                if (tgt == null || p.canBeTargetedBy(sa)) {
                    String type = abMana.getOrigProduced().split("Special ")[1];

                    if (type.equals("EnchantedManaCost")) {
                        Card enchanted = card.getEnchantingCard();
                        if (enchanted != null ) {
                            
                            StringBuilder sb = new StringBuilder();
                            int generic = enchanted.getManaCost().getGenericCost();
                            if( generic > 0 )
                                sb.append(generic);

                            for (ManaCostShard s : enchanted.getManaCost()) {
                                ColorSet cs = ColorSet.fromMask(s.getColorMask());
                                if(cs.isColorless())
                                    continue;
                                sb.append(' ');
                                if (cs.isMonoColor())
                                    sb.append(MagicColor.toShortString(s.getColorMask()));
                                else /* (cs.isMulticolor()) */ {
                                    byte chosenColor = sa.getActivatingPlayer().getController().chooseColor("Choose a single color from " + s.toString(), sa, cs);
                                    sb.append(MagicColor.toShortString(chosenColor));
                                }
                            }
                            abMana.setExpressChoice(sb.toString().trim());
                        }
                    }


                    if (abMana.getExpressChoice().isEmpty()) {
                        System.out.println("AbilityFactoryMana::manaResolve() - special mana effect is empty for " + sa.getSourceCard().getName());
                    }
                }
            }    
        }

        for (final Player player : tgtPlayers) {
            abMana.produceMana(GameActionUtil.generatedMana(sa), player, sa);
        }

        // Only clear express choice after mana has been produced
        abMana.clearExpressChoice();

        // convert these to SubAbilities when appropriate
        if (sa.hasParam("Stuck")) {
            sa.setUndoable(false);
            card.addHiddenExtrinsicKeyword("This card doesn't untap during your next untap step.");
        }

        final String deplete = sa.getParam("Deplete");
        if (deplete != null) {
            final int num = card.getCounters(CounterType.getType(deplete));
            if (num == 0) {
                sa.setUndoable(false);
                game.getAction().sacrifice(card, null);
            }
        }

        //resolveDrawback(sa);
    }

    /**
     * <p>
     * manaStackDescription.
     * </p>
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param abMana
     *            a {@link forge.card.spellability.AbilityMana} object.
     * @param af
     *            a {@link forge.game.ability.AbilityFactory} object.
     * 
     * @return a {@link java.lang.String} object.
     */

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        String mana = !sa.hasParam("Amount") || StringUtils.isNumeric(sa.getParam("Amount"))
                ? GameActionUtil.generatedMana(sa) : "mana";
        sb.append("Add ").append(mana).append(" to your mana pool.");
        return sb.toString();
    }
}
