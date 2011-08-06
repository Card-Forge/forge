package forge;
import javax.swing.*;

interface CardDetail
{
  public void updateCardDetail(Card c);
}

public class CardDetailUtil
{
  public static void updateCardDetail(Card card, JTextArea area, JPanel cardDetailPanel, JPanel picturePanel, JLabel label[])
  {
    if(card == null)
        return;
    //System.out.println("label length: " + label.length);
    for(int i = 0; i < label.length; i++)
      label[i].setText("");

    area.setText("");

    if(card.isLand())
        label[0].setText(card.getName());
    else
        label[0].setText(card.getName() +"  - " +card.getManaCost());

    label[1].setText(GuiDisplayUtil.formatCardType(card));

    if(card.isCreature())
    {
        String stats = "" +card.getNetAttack() +" / "  +card.getNetDefense();
        label[2].setText(stats);
    }

    if(card.isCreature())
        label[3].setText("Damage: " +card.getDamage() +" Assigned Damage: " +card.getTotalAssignedDamage());

    String uniqueID = card.getUniqueNumber() +" ";
    label[4].setText("Card ID  " +uniqueID);

    area.setText(card.getText());

    cardDetailPanel.setBorder(GuiDisplayUtil.getBorder(card));

    //picture
    picturePanel.removeAll();
    picturePanel.add(GuiDisplayUtil.getPicture(card));
    picturePanel.revalidate();
  }//updateCardDetail
}