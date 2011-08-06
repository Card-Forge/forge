package forge;
import javax.swing.*;

public class CardPanel extends JPanel
{
	private static final long serialVersionUID = 509877513760665415L;
private Card card;

  public CardPanel(Card card)
  {
    this.card = card;
  }
  public Card getCard()
  {
    return card;
  }
  
  //~
  public CardPanel connectedCard;
  //~
}
