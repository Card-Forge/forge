package forge.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 
 * Divides file into sections and joins them back to stringlist to save
 *
 */
public class SectionUtil {

    @SuppressWarnings("unchecked")
    public static Map<String,List<String>> parseSections(List<String> source)
    {
        Map<String,List<String>> result = new HashMap<String,List<String>>();
        String currentSection = "";
        List<String> currentList = null;
        
        for( String s : source ) {
            String st = s.trim();
            if( st.length() == 0 ) continue;
            if( st.startsWith("[") && st.endsWith("]") )
            {
                if( currentList != null && currentList.size() > 0 )
                {
                    Object oldVal = result.get(currentSection);
                    if( oldVal != null && oldVal instanceof List<?>)
                    {
                        currentList.addAll((List<String>)oldVal);
                    }
                    result.put(currentSection, currentList);
                }
                
                String newSection = st.substring(1, st.length() - 1);
                currentSection = newSection;
                currentList = null;
            }
            else
            {
                if( currentList == null )
                    currentList = new ArrayList<String>();
                currentList.add(st);
            }
        }
        
        // save final block
        if( currentList != null && currentList.size() > 0 )
        {
            Object oldVal = result.get(currentSection);
            if( oldVal != null && oldVal instanceof List<?>)
            {
                currentList.addAll((List<String>)oldVal);
            }
            result.put(currentSection, currentList);
        }        
        
        return result;
    }
}
