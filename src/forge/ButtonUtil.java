package forge;
public class ButtonUtil
{
    public static void reset()
    {
	getOK().setText("OK");
	getCancel().setText("Cancel");
	
	getOK().setSelectable(false);
	getCancel().setSelectable(false);
    }
    public static void enableOnlyOK()
    {
	getOK().setSelectable(true);
	getCancel().setSelectable(false);	
    }
    public static void enableOnlyCancel()
    {
	getOK().setSelectable(false);
	getCancel().setSelectable(true);
    }
    public static void disableAll()
    {
	getOK().setSelectable(false);
	getCancel().setSelectable(false);	
    }
    public static void enableAll()
    {
	getOK().setSelectable(true);
	getCancel().setSelectable(true);	
    }    
    public static void disableOK()
    {
    getOK().setSelectable(false);
    }    
    public static void disableCancel()
    {
    	getCancel().setSelectable(false);
    }
    
    private static MyButton getOK()  {return AllZone.Display.getButtonOK();}
    private static MyButton getCancel()  {return AllZone.Display.getButtonCancel();}
}
