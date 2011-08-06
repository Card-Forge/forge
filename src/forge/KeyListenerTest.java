package forge;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyListenerTest implements KeyListener
{

public void keyPressed(KeyEvent arg0)
{
	int code = arg0.getKeyCode();

	if(code == KeyEvent.VK_ENTER)
	{
	//Do something here
	System.out.println("You pressed enter");
	}
	
}
public void keyReleased(KeyEvent arg0)
{
	// TODO Auto-generated method stub
	
}
public void keyTyped(KeyEvent arg0)
{
	// TODO Auto-generated method stub
	
}

}