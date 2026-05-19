package forge.game.ability.effects;

import static forge.util.TextUtil.toManaString;

import java.util.List;
import java.util.Map;

import forge.game.card.CardUtil;
import forge.util.Lang;
import org.apache.commons.lang3.StringUtils;

import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.mana.ManaAtom;
import forge.card.mana.ManaCostShard;
import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.util.Localizer;
import io.sentry.Breadcrumb;
import io.sentry.Sentry;

public class ManaEffect extends SpellAbilityEffect {

    @Override
    public void buildSpellAbility(SpellAbility sa) {
        super.buildSpellAbility(sa);
        sa.setManaPart(new AbilityManaPart(sa, sa.getMapParams()));
        if (sa.getParent() == null) {
            sa.setUndoable(true); // will try at least
        }
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        final AbilityManaPart abMana = sa.getManaPart();
        final List<Player> tgtPlayers = getDefinedPlayersOrTargeted(sa);
        final Player activator = sa.getActivatingPlayer();

        // Spells are not undoable
        sa.setUndoable(sa.isAbility() && sa.isUndoable() && tgtPlayers.size() < 2 && !sa.hasParam("ActivationLimit"));

        if (sa.hasParam("Optional") && !activator.getController().confirmAction(sa, null, Localizer.getInstance().getMessage("lblDoYouWantAddMana"), null)) {
            return;
        }

        final StringBuilder producedMana = new StringBuilder();

        for (Player p : tgtPlayers) {
            if (!p.isInGame()) {
                continue;
            }

            final Player chooser;
            if (sa.hasParam("Chooser")) {
                chooser = AbilityUtils.getDefinedPlayers(host, sa.getParam("Chooser"), sa).get(0);
            } else {
                chooser = p;
            }

            if (abMana.isComboMana()) {
                int amount = sa.hasParam("Amount") ? AbilityUtils.calculateAmount(host, sa.getParam("Amount"), sa) : 1;
                int each = sa.hasParam("Each") ? AbilityUtils.calculateAmount(host, sa.getParam("Each"), sa) : 1;
                if (amount <= 0 || each <= 0) {
                    continue;
                }

                String combo = abMana.getComboColors(sa);
                if (combo.isBlank()) {
                    return;
                }
                String[] colorsProduced = combo.split(" ");
                ColorSet colorOptions = ColorSet.fromNames(colorsProduced);
                String express = abMana.getExpressChoice();
                String[] colorsNeeded = express.isEmpty() ? null : express.split(" ");
                boolean differentChoice = abMana.getOrigProduced().contains("Different");
                ColorSet fullOptions = colorOptions;
                final StringBuilder choiceString = new StringBuilder();
                final StringBuilder choiceSymbols = new StringBuilder();
                // Use specifyManaCombo if possible
                if (colorsNeeded == null && amount > 1 && !sa.hasParam("Each")) {
                    Map<Byte, Integer> choices = chooser.getController().specifyManaCombo(sa, colorOptions, amount, differentChoice);
                    for (Map.Entry<Byte, Integer> e : choices.entrySet()) {
                        Byte chosenColor = e.getKey();
                        String choice = MagicColor.toShortString(chosenColor);
                        String symbol = MagicColor.toSymbol(chosenColor);
                        Integer count = e.getValue();
                        while (count > 0) {
                            if (choiceString.length() > 0) {
                                choiceString.append(" ");
                            }
                            choiceString.append(choice);
                            choiceSymbols.append(symbol);
                            --count;
                        }
                    }
                } else {
                    for (int nMana = 0; nMana < amount; nMana++) {
                        String choice = "";
                        if (colorsNeeded != null && colorsNeeded.length > nMana) { // select from express choices if possible
                            colorOptions = ColorSet
                                    .fromMask(fullOptions.getColor() & ManaAtom.fromName(colorsNeeded[nMana]));
                        }
                        if (colorOptions.isColorless() && colorsProduced.length > 0) {
                            // If we just need generic mana, no reason to ask the controller for a choice,
                            // just use the first possible color.
                            choice = colorsProduced[differentChoice ? nMana : 0];
                        } else {
                            byte chosenColor = chooser.getController().chooseColor(Localizer.getInstance().getMessage("lblSelectManaProduce"), sa,
                                    differentChoice && (colorsNeeded == null || colorsNeeded.length <= nMana) ? fullOptions : colorOptions);
                            if (chosenColor == 0)
                                throw new RuntimeException("ManaEffect::resolve() /*combo mana*/ - " + p + " color mana choice is empty for " + host.getName());

                            if (differentChoice) {
                                fullOptions = ColorSet.fromMask(fullOptions.getColor() - chosenColor);
                            }
                            choice = MagicColor.toShortString(chosenColor);
                        }

                        String symbol = MagicColor.toSymbol(choice);
                        int count = each;
                        while (count > 0) {
                            if (choiceString.length() > 0) {
                                choiceString.append(" ");
                            }
                            choiceString.append(choice);
                            choiceSymbols.append(symbol);
                            --count;
                        }
                    }
                }

                if (choiceString.toString().isEmpty() && ("Combo ColorIdentity".equals(abMana.getOrigProduced()) || "Combo Spire".equals(abMana.getOrigProduced()))) {
                    // No mana could be produced here (non-EDH match?), so cut short
                    continue;
                }

                game.getAction().notifyOfValue(sa, p, choiceSymbols.toString(), p);
                abMana.setExpressChoice(choiceString.toString());
            }
            else if (abMana.isAnyMana()) {
                // AI color choice is set in ComputerUtils so only human players need to make a choice

                String colorsNeeded = abMana.getExpressChoice();

                byte mask = 0;
                //loop through colors to make menu
                for (int nChar = 0; nChar < colorsNeeded.length(); nChar++) {
                    mask |= MagicColor.fromName(colorsNeeded.charAt(nChar));
                }
                ColorSet colorMenu = mask == 0 ? ColorSet.WUBRG : ColorSet.fromMask(mask);
                byte val = chooser.getController().chooseColor(Localizer.getInstance().getMessage("lblSelectManaProduce"), sa, colorMenu);
                if (0 == val) {
                    throw new RuntimeException("ManaEffect::resolve() /*any mana*/ - " + p + " color mana choice is empty for " + host.getName());
                }

                game.getAction().notifyOfValue(sa, host, MagicColor.toSymbol(val), p);
                abMana.setExpressChoice(MagicColor.toShortString(val));
            }
            else if (abMana.isSpecialMana()) {
                handleSpecialMana(chooser, abMana, sa, true);
            }

            String mana = GameActionUtil.generatedMana(sa);

            // this can happen when mana is based on criteria that didn't match
            if (mana.isEmpty()) {
                String msg = "AbilityFactoryMana::manaResolve() - special mana effect is empty for";

                Breadcrumb bread = new Breadcrumb(msg);
                bread.setData("Card", host.getName());
                bread.setData("SA", sa.toString());
                Sentry.addBreadcrumb(bread);

                continue;
            }

            producedMana.append(abMana.produceMana(mana, p, sa));
        }

        // Only clear express choice after mana has been produced
        abMana.clearExpressChoice();

        abMana.tapsForMana(sa.getRootAbility(), producedMana.toString());

        if (sa.isKeyword(Keyword.FIREBENDING)) {
            activator.triggerElementalBend(TriggerType.Firebend);
        }
    }

    public static void handleSpecialMana(Player chooser, AbilityManaPart abMana, SpellAbility sa, boolean resolve) {
        String type = abMana.getOrigProduced().split("Special ")[1];
        Card host = sa.getHostCard();

        if (resolve) {
            if (type.equals("EnchantedManaCost")) {
                Card enchanted = host.getEnchantingCard();
                if (enchanted == null)
                    return;

                StringBuilder sb = new StringBuilder();
                int generic = enchanted.getManaCost().getGenericCost();

                for (ManaCostShard s : enchanted.getManaCost()) {
                    ColorSet cs = ColorSet.fromMask(s.getColorMask());
                    byte chosenColor;
                    if (cs.isColorless())
                        continue;
                    if (s.isOr2Generic()) { // CR 106.8
                        chosenColor = chooser.getController().chooseColorAllowColorless(Localizer.getInstance().getMessage("lblChooseSingleColorFromTarget", s.toString()), host, cs);
                        if (chosenColor == MagicColor.COLORLESS) {
                            generic += 2;
                            continue;
                        }
                    } else if (cs.isMonoColor())
                        chosenColor = s.getColorMask();
                    else /* (cs.isMulticolor()) */ {
                        chosenColor = chooser.getController().chooseColor(Localizer.getInstance().getMessage("lblChooseSingleColorFromTarget", s.toString()), sa, cs);
                    }
                    sb.append(MagicColor.toShortString(chosenColor));
                    sb.append(' ');
                }
                if (generic > 0) {
                    sb.append(generic);
                }

                abMana.setExpressChoice(sb.toString().trim());
            } else if (type.startsWith("EachColoredManaSymbol")) {
                final String res = type.split("_")[1];
                StringBuilder sb = new StringBuilder();
                for (Card c : AbilityUtils.getDefinedCards(host, res, sa)) {
                    for (ManaCostShard s : c.getManaCost()) {
                        ColorSet cs = ColorSet.fromMask(s.getColorMask());
                        if (cs.isColorless())
                            continue;
                        sb.append(' ');
                        if (cs.isMonoColor())
                            sb.append(MagicColor.toShortString(s.getColorMask()));
                        else /* (cs.isMulticolor()) */ {
                            byte chosenColor = chooser.getController().chooseColor(Localizer.getInstance().getMessage("lblChooseSingleColorFromTarget", s.toString()), sa, cs);
                            sb.append(MagicColor.toShortString(chosenColor));
                        }
                    }
                }
                abMana.setExpressChoice(sb.toString().trim());
            } else if (type.startsWith("DoubleManaInPool")) {
                StringBuilder sb = new StringBuilder();
                for (byte color : ManaAtom.MANATYPES) {
                    sb.append(StringUtils.repeat(MagicColor.toShortString(color) + " ", chooser.getManaPool().getAmountOfColor(color)));
                }
                abMana.setExpressChoice(sb.toString().trim());
            }
        } else if (type.equals("LastNotedType")) {
            // Jeweled Lotus
            final StringBuilder sb = new StringBuilder();
            for (Object o : host.getRemembered()) {
                if (o instanceof String) {
                    sb.append(o);
                }
            }
            String mana = sb.toString();
            if (mana.isEmpty()) {
                return;
            }
            abMana.setExpressChoice(mana);
        } else if (type.startsWith("EachColorAmong")) {
            final String res = type.split("_")[1];
            ColorSet colors = CardUtil.getColorsFromCards(AbilityUtils.getDefinedCards(host, res, sa));
            if (colors.isColorless()) return;
            abMana.setExpressChoice(colors);
        }
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
        final List<Player> tgtPlayers = getDefinedPlayersOrTargeted(sa);
        String mana = !sa.hasParam("Amount") || StringUtils.isNumeric(sa.getParam("Amount"))
                ? GameActionUtil.generatedMana(sa) : "mana";
        String manaDesc = "";
        if (mana.equals("mana") && sa.hasParam("Produced") && sa.hasParam("AmountDesc")) {
            mana = sa.getParam("Produced");
            manaDesc = sa.getParam("AmountDesc");
        }
        sb.append(Lang.joinHomogenous(tgtPlayers)).append(tgtPlayers.size() == 1 ? " adds " : " add ");
        sb.append(toManaString(mana)).append(manaDesc).append(".");
        if (sa.hasParam("RestrictValid")) {
            sb.append(" ");
            final String desc = sa.getDescription();
            if (desc.contains("Spend this") && desc.contains(".")) {
                int i = desc.indexOf("Spend this");
                sb.append(desc, i, desc.indexOf(".", i) + 1);
            } else if (desc.contains("This mana can't") && desc.contains(".")) { //for negative restrictions (Jegantha)
                int i = desc.indexOf("This mana can't");
                sb.append(desc, i, desc.indexOf(".", i) + 1);
            } else {
                sb.append("[failed to add RestrictValid to StackDesc]");
            }
        }
        return sb.toString();
    }
}
