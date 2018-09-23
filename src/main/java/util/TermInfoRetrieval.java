package util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ir.Catalog.createCatalog;
import static util.DataFactory.getTermIdMap;
import static util.Stemmer.getStem;

/**
 * Created by paulomimahidharia on 6/15/17.
 */
public class TermInfoRetrieval {

    private static final String CATALOG_PATH = "/Users/paulomimahidharia/Desktop/IR/CustomIndexer/FinalCatalog.txt";


    public static void main(String args[]) throws IOException {

        File cat = new File(CATALOG_PATH);
        HashMap<Integer,ArrayList<Integer>> catalog = createCatalog(cat);

        // TID
        Map<String, Integer> termIdMap = getTermIdMap();

        List<String> terms = new ArrayList<>();
        terms.add("Chudnovsky");
        terms.add("circumference");
        terms.add("Dalhousie");
        terms.add("Borwein");

        //String term = "europe";
        for(String term: terms){
            term = term.replace(".", " ").replace(",", "").trim().toLowerCase();

            // Ignore if term has no stem
            term = getStem(term);
            if(term.equals("")) {
                System.out.println("No stem found");
            }

            Integer termId = termIdMap.get(term);
            if(termId == null) {
                System.out.println("No term ID found");
                System.exit(0);
            }

            File file = new File("/Users/paulomimahidharia/Desktop/IR/CustomIndexer/final.txt");

            String s = getLineOfFileWithtermIdAndCatalog(catalog, termId, file);
            System.out.println(s +"\n");
        }
    }

    private static String getLineOfFileWithtermIdAndCatalog (HashMap<Integer, ArrayList<Integer>> catalog, int termId, File file) {

        ArrayList<Integer> offsets = catalog.get(termId);
        int start = offsets.get(0);
        int end = offsets.get(1);

        RandomAccessFile randFile = null;
        int len = 0;
        byte[] string = null;

        try {

            randFile = new RandomAccessFile(file, "rw");
            len = (int) randFile.length();
            if (end >= len) {
                end = len;
            }
            string = new byte[end - start];
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            if (randFile != null) {
                randFile.seek(start);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            //randFile.setLength(end1);
            if (randFile != null) {
                randFile.readFully(string);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            if (randFile != null) {
                randFile.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return (new String(string));
    }
}
