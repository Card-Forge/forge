package forge;
public interface MyButton
{
//  public MyButton(String buttonText, Command command)
  public void select();
  public void setSelectable(boolean b);
  public boolean isSelectable();

  public String getText();
  public void setText(String text);

  public void reset(); //resets the text and calls setSelectable(false)
}