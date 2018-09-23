package util;

import org.tartarus.snowball.ext.PorterStemmer;

/**
 * Created by paulomimahidharia on 6/9/17.
 */
public class Stemmer {

    public static String getStem(String term){
        PorterStemmer p = new PorterStemmer();
        p.setCurrent(term);
        p.stem();
        String result = p.getCurrent();
        //System.out.println(result);
        return result;
    }
}
