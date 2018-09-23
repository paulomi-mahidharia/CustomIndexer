package util;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static ir.Catalog.createCatalog;
import static util.Stemmer.getStem;

/**
 * Created by paulomimahidharia on 6/10/17.
 */
public class Evalutor {

    private static final String INPUT = "/Users/paulomimahidharia/Desktop/IR/resources/AP_DATA/input.txt";
    private static final String OUTPUT = "Output.stopped.stemmed.txt";
    private static final String CATALOG = "/Users/paulomimahidharia/Desktop/IR/CustomIndexer/FinalCatalog.txt";
    private static final String TID = "/Users/paulomimahidharia/Desktop/IR/CustomIndexer/TID.txt";

    public static void main(String args[]) throws IOException {

        BufferedReader inputBR = new BufferedReader(new FileReader(new File(INPUT)));
        BufferedReader tidBR = new BufferedReader(new FileReader(new File(TID)));
        PrintWriter writer = new PrintWriter(OUTPUT);

        File catalogFile = new File(CATALOG);
        HashMap<Integer,ArrayList<Integer>> catalogMap = createCatalog(catalogFile);
        System.out.println(catalogMap.size());

        // Read and store term ids
        HashMap<String, Integer> TIDmap = new HashMap<String, Integer>();
        String line ="";
        while (( line = tidBR.readLine()) != null)  {
            String[] TID = line.split(":");

            String term = TID[0];
            term = term.trim();
            String Tid = TID[1];
            Tid = Tid.trim();
            int tid = Integer.parseInt(Tid);
            TIDmap.put(term, tid);
        }
        tidBR.close();

        String term;
        while((term = inputBR.readLine()) != null){

            term = term.toLowerCase();
            //Get stemmed term
            term = getStem(term.trim());

            int termID = TIDmap.get(term);
            // get TF map for that term
            List<Integer> offsets = catalogMap.get(termID);

            if (offsets != null) {

                int start = offsets.get(0);
                int end = offsets.get(1);
                byte[] string;

                File finalFile = new File("/Users/paulomimahidharia/Desktop/IR/CustomIndexer/final.txt");
                RandomAccessFile randFile = new RandomAccessFile(finalFile, "rw");

                string = new byte[end - start];
                randFile.seek(start);
                randFile.readFully(string);
                randFile.close();

                String termAllInfo = new String(string);
                //System.out.println(termAllInfo);
                String termInfo = termAllInfo.split(":")[0];

                String[] termInfoElements = termInfo.split("_");

                String TTF = termInfoElements[1];
                String DF = termInfoElements[2];

                writer.write(term + " " + DF + " " + TTF + "\n");

            }
        }
        inputBR.close();
        tidBR.close();
        writer.close();
    }
}
