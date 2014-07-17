package forge.itemmanager;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.card.CardRulesPredicates.Presets;
import forge.card.MagicColor;
import forge.deck.DeckProxy;
import forge.game.GameFormat;
import forge.interfaces.IButton;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.itemmanager.SItemManagerUtil.StatTypes;
import forge.util.BinaryUtil;
import forge.util.PredicateString.StringOp;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;

/** 
 * Static factory; holds blocks of form elements and predicates
 * which are used in various editing environments.
 * <br><br>
 * <i>(S at beginning of class name denotes a static factory.)</i>
 */
public class SFilterUtil {
    /**
     * builds a string search filter
     */
    public static Predicate<PaperCard> buildTextFilter(String text, boolean invert, boolean inName, boolean inType, boolean inText, boolean inCost) {
		
		text = text.trim();
		
        if (text.isEmpty()) {
            return Predicates.alwaysTrue();
        }
		
		if (BooleanExpression.isBooleanExpression(text)) {
			BooleanExpression expression = new BooleanExpression(text);
			try {
				Predicate<CardRules> filter = expression.evaluate(inName, inType, inText, inCost);
				if (filter != null) {
					return Predicates.compose(filter, PaperCard.FN_GET_RULES);
				} //The expression is not valid, let the regular filters work
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
        String[] splitText = text.replaceAll(",", "").replaceAll("  ", " ").split(" ");

        List<Predicate<CardRules>> terms = new ArrayList<Predicate<CardRules>>();
        for (String s : splitText) {
            List<Predicate<CardRules>> subands = new ArrayList<Predicate<CardRules>>();

            if (inName) { subands.add(CardRulesPredicates.name(StringOp.CONTAINS_IC, s));       }
            if (inType) { subands.add(CardRulesPredicates.joinedType(StringOp.CONTAINS_IC, s)); }
			if (inText) { subands.add(CardRulesPredicates.rules(StringOp.CONTAINS_IC, s));      }
			if (inCost) { subands.add(CardRulesPredicates.cost(StringOp.CONTAINS_IC, s));       }

            terms.add(Predicates.or(subands));
        }
        Predicate<CardRules> textFilter = invert ? Predicates.not(Predicates.or(terms)) : Predicates.and(terms);

        return Predicates.compose(textFilter, PaperCard.FN_GET_RULES);
		
    }
	
	private static class BooleanExpression {
		
		private static enum Operation {
			
			AND("&&"), OR("||"), OPEN_PAREN("("), CLOSE_PAREN(")"), VALUE("\""), ESCAPE("\\");
			
			private final String token;
			
			private Operation(String token) {
				this.token = token;
			}
			
		}
		
		private Stack<StackItem> stack = new Stack<StackItem>();
		private String expression;
		
		private BooleanExpression(String expression) {
			this.expression = expression;
		}
		
		private Predicate<CardRules> evaluate(boolean inName, boolean inType, boolean inText, boolean inCost) throws IOException {
			
			StringReader reader = new StringReader(expression);
			String currentItem = "";
			boolean escapeNext = false;
			boolean gettingValue = false;
			
			int character;
			
			while ((character = reader.read()) != -1) {
				
				String token = Character.toString((char) character);
				
				if (token.equals(Operation.OPEN_PAREN.token)) {
					StackOperation operation = new StackOperation();
					operation.operation = Operation.OPEN_PAREN;
					stack.push(operation);
					continue;
				}
				
				if (token.equals(Operation.CLOSE_PAREN.token)) {
					character = reader.read();
					if (character != -1) {
						evaluateStack();
						continue;
					} else {
						break;
					}
				}
				
				if (token.equals("&")) {
					character = reader.read();
					if (character != -1 && character == '&') {
						StackOperation operation = new StackOperation();
						operation.operation = Operation.AND;
						stack.push(operation);
						currentItem = "";
					} else if (gettingValue) {
						currentItem += token + Character.toString((char) character);
						continue;
					}
				}

				if (token.equals("|")) {
					character = reader.read();
					if (character != -1 && character == '|') {
						StackOperation operation = new StackOperation();
						operation.operation = Operation.OR;
						stack.push(operation);
						currentItem = "";
					} else if (gettingValue) {
						currentItem += token + Character.toString((char) character);
						continue;
					}
				}
				
				if (token.equals(Operation.VALUE.token) && !escapeNext) {
					if (gettingValue) {
						StackValue stackValue = new StackValue();
						stackValue.value = evaluateValue(currentItem.trim(), inName, inType, inText, inCost);
						stack.push(stackValue);
						currentItem = "";
						gettingValue = false;
					} else {
						gettingValue = true;
						continue; //Don't add the quotation mark
					}
				}

				if (token.equals(Operation.ESCAPE.token) && !escapeNext) {
					escapeNext = true;
				} else if (gettingValue) {
					currentItem += token;
				}
				
			}
			
			while (stack.size() > 1) {
				evaluateStack();
			}

			if (stack.isEmpty()) {
				return null; //The expression is not valid, let the regular filters work
			}
			
			return ((StackValue) stack.pop()).value;
			
		}
		
		private Predicate<CardRules> evaluateValue(String value, boolean inName, boolean inType, boolean inText, boolean inCost) {
			if (inName) { return CardRulesPredicates.name(StringOp.CONTAINS_IC, value);       }
			if (inType) { return CardRulesPredicates.joinedType(StringOp.CONTAINS_IC, value); }
			if (inText) { return CardRulesPredicates.rules(StringOp.CONTAINS_IC, value);      }
			if (inCost) { return CardRulesPredicates.cost(StringOp.CONTAINS_IC, value);       }
			return Predicates.alwaysTrue();
		}
		
		private void evaluateStack() {

			StackItem stackItem;
			Predicate<CardRules> rules = null;
			Operation operation = null;
			
			boolean finishedStack = false;
			
			while (stack.size() > 0) {
				
				stackItem = stack.pop();
				
				if (stackItem.isOperation()) {
					
					operation = ((StackOperation) stackItem).operation;
					
					if (operation.equals(Operation.OPEN_PAREN)) {
						
						if (rules != null) {
							StackValue value = new StackValue();
							value.value = rules;
							stack.push(value);
						}
						
						break;
					}
					
				} else if (rules == null) {
					rules = ((StackValue) stackItem).value;
				} else {
					
					if (operation == null) {
						return;
					}
					
					StackValue value = new StackValue();
					
					if (operation.equals(Operation.AND)) {
						value.value = Predicates.and(rules, ((StackValue) stackItem).value);
					} else if (operation.equals(Operation.OR)) {
						value.value = Predicates.or(rules, ((StackValue) stackItem).value);
					} else {
						return;
					}
					
					if (stack.size() == 0) {
						finishedStack = true;
					}
					
					stack.push(value);
					
					rules = null;
					
				}
				
				if (finishedStack) {
					return;
				}
				
			}
			
		}
		
		private static boolean isBooleanExpression(String s) {
			return s.contains(Operation.AND.token) || s.contains(Operation.OR.token);
		}
		
		private static abstract class StackItem {
			private boolean isOperation() {
				return this instanceof StackOperation;
			}
		}
		
		private static class StackOperation extends StackItem {
			private Operation operation;
		}
		
		private static class StackValue extends StackItem {
			private Predicate<CardRules> value;
		}
		
		/*private static abstract class BooleanExpressionNode {
			protected abstract Predicate<CardRules> evaluate(boolean inName, boolean inType, boolean inText, boolean inCost);
		}
		
		private static class ExpressionNode extends BooleanExpressionNode {
			
			private BooleanExpressionNode left;
			private BooleanExpressionNode right;
			private Operation operation;

			@Override
			protected Predicate<CardRules> evaluate(boolean inName, boolean inType, boolean inText, boolean inCost) {
				switch (operation) {
					case AND:
						return Predicates.and(left.evaluate(inName, inType, inText, inCost), right.evaluate(inName, inType, inText, inCost));
					case OR:
						return Predicates.or(left.evaluate(inName, inType, inText, inCost), right.evaluate(inName, inType, inText, inCost));
				}
				return Predicates.alwaysTrue();
			}
			
		}
		
		private static class ValueNode extends BooleanExpressionNode {
			
			private String value;
			
			@Override
			protected Predicate<CardRules> evaluate(boolean inName, boolean inType, boolean inText, boolean inCost) {
				if (inName) { return CardRulesPredicates.name(StringOp.CONTAINS_IC, value);       }
				if (inType) { return CardRulesPredicates.joinedType(StringOp.CONTAINS_IC, value); }
				if (inText) { return CardRulesPredicates.rules(StringOp.CONTAINS_IC, value);      }
				if (inCost) { return CardRulesPredicates.cost(StringOp.CONTAINS_IC, value);       }
				return Predicates.alwaysTrue();
			}
			
		}*/
		
	}
    
    public static <T extends InventoryItem> Predicate<T> buildItemTextFilter(String text) {
        if (text.trim().isEmpty()) {
            return Predicates.alwaysTrue();
        }

        return new ItemTextPredicate<T>(text);
    }

    private static class ItemTextPredicate<T extends InventoryItem> implements Predicate<T> {
        private final String[] splitText;

        private ItemTextPredicate(String text) {
            splitText = text.toLowerCase().replaceAll(",", "").replaceAll("  ", " ").split(" ");
        }
        
        @Override
        public boolean apply(T input) {
            String name = input.getName().toLowerCase();
            for (String s : splitText) {
                if (name.contains(s)) {
                    return true;
                }
            }
            return false;
        }
    };
    
    public static Predicate<PaperCard> buildColorFilter(Map<SItemManagerUtil.StatTypes, ? extends IButton> buttonMap) {
        byte colors = 0;

        if (buttonMap.get(StatTypes.WHITE).isSelected()) {
            colors |= MagicColor.WHITE;
        }
        if (buttonMap.get(StatTypes.BLUE).isSelected()) {
            colors |= MagicColor.BLUE;
        }
        if (buttonMap.get(StatTypes.BLACK).isSelected()) {
            colors |= MagicColor.BLACK;
        }
        if (buttonMap.get(StatTypes.RED).isSelected()) {
            colors |= MagicColor.RED;
        }
        if (buttonMap.get(StatTypes.GREEN).isSelected()) {
            colors |= MagicColor.GREEN;
        }

        boolean wantColorless = buttonMap.get(StatTypes.COLORLESS).isSelected();
        boolean wantMulticolor = buttonMap.get(StatTypes.MULTICOLOR).isSelected();

        Predicate<CardRules> preFinal = null;
        if (wantMulticolor) {
            if (colors == 0) { //handle showing all multi-color cards if all 5 colors are filtered
                preFinal = Presets.IS_MULTICOLOR;
                if (wantColorless) {
                    preFinal = Predicates.or(preFinal, Presets.IS_COLORLESS);
                }
            }
            else if (colors != MagicColor.ALL_COLORS) {
                preFinal = CardRulesPredicates.canCastWithAvailable(colors);
            }
        }
        else {
            preFinal = Predicates.not(Presets.IS_MULTICOLOR);
            if (colors != MagicColor.ALL_COLORS) {
                preFinal = Predicates.and(CardRulesPredicates.canCastWithAvailable(colors), preFinal);
            }
        }
        if (!wantColorless) {
            if (colors != 0 && colors != MagicColor.ALL_COLORS) {
                //if colorless filtered out ensure phyrexian cards don't appear
                //unless at least one of their colors is selected
                preFinal = Predicates.and(preFinal, CardRulesPredicates.isColor(colors));
            }
            preFinal = SFilterUtil.optimizedAnd(preFinal, Predicates.not(Presets.IS_COLORLESS));
        }

        if (preFinal == null) {
            return new Predicate<PaperCard>() { //use custom return true delegate to validate the item is a card
                @Override
                public boolean apply(PaperCard card) {
                    return true;
                }
            };
        }
        return Predicates.compose(preFinal, PaperCard.FN_GET_RULES);
    }

    public static Predicate<DeckProxy> buildDeckColorFilter(final Map<StatTypes, ? extends IButton> buttonMap) {
        return new Predicate<DeckProxy>() {
            @Override
            public boolean apply(DeckProxy input) {
                byte colorProfile = input.getColor().getColor();
                if (colorProfile == 0) {
                    return buttonMap.get(StatTypes.DECK_COLORLESS).isSelected();
                }

                boolean wantMulticolor = buttonMap.get(StatTypes.DECK_MULTICOLOR).isSelected();
                if (!wantMulticolor && BinaryUtil.bitCount(colorProfile) > 1) {
                    return false;
                }

                byte colors = 0;
                if (buttonMap.get(StatTypes.DECK_WHITE).isSelected()) {
                    colors |= MagicColor.WHITE;
                }
                if (buttonMap.get(StatTypes.DECK_BLUE).isSelected()) {
                    colors |= MagicColor.BLUE;
                }
                if (buttonMap.get(StatTypes.DECK_BLACK).isSelected()) {
                    colors |= MagicColor.BLACK;
                }
                if (buttonMap.get(StatTypes.DECK_RED).isSelected()) {
                    colors |= MagicColor.RED;
                }
                if (buttonMap.get(StatTypes.DECK_GREEN).isSelected()) {
                    colors |= MagicColor.GREEN;
                }

                if (colors == 0 && wantMulticolor && BinaryUtil.bitCount(colorProfile) > 1) {
                    return true;
                }

                return (colorProfile & colors) == colorProfile;
            }
        };
    }

    public static void showOnlyStat(StatTypes clickedStat, IButton clickedButton, Map<StatTypes, ? extends IButton> buttonMap) {
        boolean foundSelected = false;
        for (Map.Entry<StatTypes, ? extends IButton> btn : buttonMap.entrySet()) {
            if (btn.getKey() != clickedStat) {
                if (btn.getKey() == StatTypes.MULTICOLOR) {
                    switch (clickedStat) {
                    case WHITE:
                    case BLUE:
                    case BLACK:
                    case RED:
                    case GREEN:
                        //ensure multicolor filter selected after right-clicking a color filter
                        if (!btn.getValue().isSelected()) {
                            btn.getValue().setSelected(true);
                        }
                        continue;
                    default:
                        break;
                    }
                }
                else if (btn.getKey() == StatTypes.DECK_MULTICOLOR) {
                    switch (clickedStat) {
                    case DECK_WHITE:
                    case DECK_BLUE:
                    case DECK_BLACK:
                    case DECK_RED:
                    case DECK_GREEN:
                        //ensure multicolor filter selected after right-clicking a color filter
                        if (!btn.getValue().isSelected()) {
                            btn.getValue().setSelected(true);
                        }
                        continue;
                    default:
                        break;
                    }
                }
                if (btn.getValue().isSelected()) {
                    foundSelected = true;
                    btn.getValue().setSelected(false);
                }
            }
        }
        if (!clickedButton.isSelected()) {
            clickedButton.setSelected(true);
        }
        else if (!foundSelected) {
            //if statLabel only label in group selected, re-select all other labels in group
            for (Map.Entry<StatTypes, ? extends IButton> btn : buttonMap.entrySet()) {
                if (btn.getKey() != clickedStat) {
                    if (!btn.getValue().isSelected()) {
                        btn.getValue().setSelected(true);
                    }
                }
            }
        }
    }

    public static Predicate<PaperCard> buildFormatFilter(Set<GameFormat> formats, boolean allowReprints) {
        List<Predicate<PaperCard>> predicates = new ArrayList<Predicate<PaperCard>>();
        for (GameFormat f : formats) {
            predicates.add(allowReprints ? f.getFilterRules() : f.getFilterPrinted());
        }
        return Predicates.or(predicates);
    }

    public static <T> Predicate<T> optimizedAnd(Predicate<T> p1, Predicate<T> p2) {
        return p1 == null ? p2 : (p2 == null ? p1 : Predicates.and(p1, p2));
    }
}
