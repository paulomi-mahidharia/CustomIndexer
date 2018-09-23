package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by paulomimahidharia on 6/8/17.
 */
public class StopWordFactory {

    private static final String STOPLIST_FILE = "/Users/paulomimahidharia/Desktop/IR/resources/stoplist.txt";

    public static Set<String> getStopWords() throws IOException {

        File stopListFile = new File(STOPLIST_FILE);
        BufferedReader stopListFileBufferedReader = new BufferedReader(new FileReader(stopListFile));

        Set<String> stopWords = new HashSet<String>();

        String stopListTerm = null;
        while ((stopListTerm = stopListFileBufferedReader.readLine()) != null) {
            stopWords.add(stopListTerm.trim());
        }

        stopListFileBufferedReader.close();
        return stopWords;
    }
}
