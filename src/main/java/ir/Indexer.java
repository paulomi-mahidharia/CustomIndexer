package ir;

import org.apache.commons.io.FileUtils;
import org.tartarus.snowball.ext.PorterStemmer;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ir.IndexMerge.createCatalog;
import static util.DataFactory.getReverseDocIdMap;
import static util.SortMap.sortHM;
import static util.Stemmer.getStem;
import static util.StopWordFactory.getStopWords;

/**
 * Created by paulomimahidharia on 6/7/17.
 */
public class Indexer {

    private static final Pattern MY_PATTERN = Pattern.compile("[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*");
    private static final String DOC_PATTERN = "<DOC>\\s(.+?)</DOC>";
    private static final String DOCNO_PATTERN = "<DOCNO>(.+?)</DOCNO>";
    private static final String TEXT_PATTERN = "<TEXT>(.+?)</TEXT>";

    public static void main(String[] args) throws IOException {

        // BEGIN - Get the doc list
        Map<String, Integer> docList = getReverseDocIdMap();

        // BEGIN - Get all stop words
        Set<String> stopWords = getStopWords();

        //--------------------------------------------------------------------------------------------------------------

        // BEGIN - Read all documents
        File docFolder = new File("/Users/paulomimahidharia/Desktop/IR/resources/AP_DATA/ap89_collection");
        File[] listOfFiles = docFolder.listFiles();
        int listOfFilesLength = listOfFiles.length;

        // TermIdMap <Term, Unique id>
        HashMap<String, Integer> TermIdMap = new HashMap<String, Integer>();

        // DocLength <DocId, Length>
        HashMap<String, Integer> DocLength = new HashMap<String, Integer>();

        // batch to track each partial index file
        // s -> start of the batch
        // e -> end of the batch
        // TermId -> a unique id for each term
        int batch = 0;
        int s = 0;
        int e = 6;
        int TermId = 0;

        while (s < listOfFilesLength) {

            batch = batch + 1;

            // Prepare partial inverted index output file
            String filename = "InvInd" + batch + ".txt";
            PrintWriter writer = new PrintWriter(filename, "UTF-8");

            // InvertedIndexTemp <TID, <DOC_ID, TF>>
            HashMap<Integer, HashMap<String, Integer>> InvertedIndexTemp = new HashMap<Integer, HashMap<String, Integer>>();

            // InvertedIndexLstOfPos <TID, <DOC_ID, [pos1, pos2, ...]>>
            HashMap<Integer, HashMap<String, ArrayList<Integer>>> InvertedIndexLstOfPos = new HashMap<Integer, HashMap<String, ArrayList<Integer>>>();

            // For every DOC in batch of 6 documents
            for (int i = s; i < e; i++) {

                System.out.println(i);

                // For each DOC dump in a file
                File mFile = new File(listOfFiles[i].getPath());
                String fileContent = FileUtils.readFileToString(mFile);

                //  Extract DOC
                Pattern pattern = Pattern.compile(DOC_PATTERN, Pattern.DOTALL);
                Matcher matcher = pattern.matcher(fileContent);

                // TermFreqWD <term, TF>
                HashMap<String, Integer> TermFreqWD = new HashMap<String, Integer>();

                // TermPosD <term, [po1, pos2, ...]>
                HashMap<String, ArrayList<Integer>> TermPosD = new HashMap<String, ArrayList<Integer>>();

                String docNo;
                while (matcher.find()) {

                    String docTemp = matcher.group(1);

                    // Extract DOC Number or DOC Id
                    final Pattern pattern1 = Pattern.compile(DOCNO_PATTERN);
                    final Matcher matcher1 = pattern1.matcher(docTemp);
                    matcher1.find();
                    String docNo1 = matcher1.group(1).trim();

                    int docNo11 = docList.get(docNo1);
                    docNo = String.valueOf(docNo11);
                    System.out.println("DOC NO : "+docNo);

                    // Extract TEXT
                    Pattern pattern2 = Pattern.compile(TEXT_PATTERN, Pattern.DOTALL);
                    Matcher matcher2 = pattern2.matcher(docTemp);

                    String textForDoc = "";
                    while (matcher2.find()) {
                        textForDoc = textForDoc.concat(matcher2.group(1));
                    }
                    textForDoc = textForDoc.toLowerCase();

                    Matcher m = MY_PATTERN.matcher(textForDoc);

                    // BEGIN - Calculate term freq and term pos
                    String word1;
                    Integer pos = 0;

                    while (m.find()) {
                        word1 = m.group(0).trim();

                        if (stopWords.contains(word1) || getStem(word1).equals("")) {
                            continue;
                        }

                        // Update TF and Pos map
                        else {
                            //Get stem for the word
                            word1 = getStem(word1);

                            pos = pos + 1;

                            // for TF
                            TermFreqWD.put(word1, (TermFreqWD.get(word1) == null ? 1 : TermFreqWD.get(word1) + 1));

                            // for list of pos
                            ArrayList<Integer> arraylist;

                            if (TermPosD.get(word1) == null) {
                                arraylist = new ArrayList<Integer>();
                                arraylist.add(pos);
                            } else {
                                arraylist = TermPosD.get(word1);
                                arraylist.add(pos);
                            }

                            TermPosD.put(word1, arraylist);
                        }
                    }
                    // END - Calculate term freq and term pos

                    // BEGIN - update TermIdMap
                    Matcher m2 = MY_PATTERN.matcher(textForDoc);
                    int wordCount = 0;
                    String word;

                    while (m2.find()) {
                        word = m2.group(0).trim();

                        if (stopWords.contains(word) || getStem(word).equals("")) {
                            continue;
                        } else {
                            word = getStem(word);
                            wordCount = wordCount + 1;

                            if (TermIdMap.get(word) == null) {
                                TermId = TermId + 1;
                                TermIdMap.put(word, TermId);
                            }

                            int Tid = TermIdMap.get(word);

                            if (InvertedIndexTemp.get(Tid) == null) {

                                HashMap<String, Integer> docIDFreq = new HashMap<String, Integer>();
                                HashMap<String, ArrayList<Integer>> docIDLop = new HashMap<String, ArrayList<Integer>>();

                                docIDFreq.put(docNo, TermFreqWD.get(word));
                                docIDLop.put(docNo, TermPosD.get(word));

                                InvertedIndexTemp.put(Tid, docIDFreq);
                                InvertedIndexLstOfPos.put(Tid, docIDLop);

                            }

                            else if (InvertedIndexTemp.get(Tid) != null) {

                                HashMap<String, Integer> docIDFreq = InvertedIndexTemp.get(Tid);
                                HashMap<String, ArrayList<Integer>> docIDLop = InvertedIndexLstOfPos.get(Tid);

                                docIDFreq.put(docNo, TermFreqWD.get(word));
                                docIDLop.put(docNo, TermPosD.get(word));

                                InvertedIndexTemp.put(Tid, docIDFreq);
                                InvertedIndexLstOfPos.put(Tid, docIDLop);
                            }

                        }

                    }
                    // END - update TermIdMap

                    DocLength.put(docNo, pos);

                    TermFreqWD.clear();
                    TermPosD.clear();
                }
            }
            // FINISHED batch of 6 doc dump

            // BEGIN -
            for (Map.Entry m : InvertedIndexTemp.entrySet()) {

                Integer keyTerm = (Integer) m.getKey();
                HashMap<String, Integer> docIDFreq = InvertedIndexTemp.get(m.getKey());

                writer.print(m.getKey() + "_");

                // Sort all the docIDs in descending order of the TF
                HashMap<String, Integer> SortedMap;
                SortedMap = sortHM(docIDFreq);

                // Calculate and Print TTF
                int TTF = 0;
                for (String key : SortedMap.keySet()) {
                    TTF = TTF + SortedMap.get(key);
                }
                writer.print(TTF + "_");

                // Print DF
                int DF = InvertedIndexLstOfPos.get(keyTerm).size();
                writer.print(DF);
                writer.print(":");

                for (Map.Entry m1 : SortedMap.entrySet()) {

                    String keyDocId = (String) m1.getKey();

                    // Print doc Id
                    writer.print(m1.getKey());
                    writer.print("_");

                    // Print doc frequency
                    writer.print(m1.getValue());
                    writer.print("_");

                    // Print pos in the doc
                    writer.print(InvertedIndexLstOfPos.get(keyTerm).get(keyDocId).toString().replace("[", "").replace("]","").replace(" ", "").trim());
                    writer.print(";");
                }
                writer.print("\n");
            }

            InvertedIndexTemp.clear();
            InvertedIndexLstOfPos.clear();
            writer.close();

            //Output catalog
            File partialFile = new File(filename);
            HashMap<Integer, ArrayList<Integer>> partialCat = createCatalog(partialFile);
            System.out.println("CAT SIZE " + partialCat.size());

            PrintWriter catWriter = new PrintWriter("CAT"+filename, "UTF-8");

            for (Map.Entry m : partialCat.entrySet()) {
                ArrayList<Integer> al = (ArrayList<Integer>) m.getValue();
                catWriter.println(m.getKey() + ":" + al.get(0) + ":" + al.get(1));
            }

            catWriter.close();

            //Get ready for next batch
            s = s + 6;
            e = e + 6;

            if (e >= listOfFilesLength) {
                e = listOfFilesLength;
            }
            // END - parsing all the docs
        }

        // Prepare to write TermIdMap <Term, Unique id>
        PrintWriter writerTID = new PrintWriter("TID.txt", "UTF-8");
        for (Map.Entry TID : TermIdMap.entrySet()) {
            writerTID.println(TID.getKey() + ":" + TID.getValue());
        }

        // Save doc length Map
        PrintWriter writerDocLen = new PrintWriter("DOClen.txt", "UTF-8");
        for (Map.Entry doclen : DocLength.entrySet()) {
            writerDocLen.println(doclen.getKey() + ":" + doclen.getValue());
        }

        DocLength.clear();
        writerTID.close();
        writerDocLen.close();
        System.out.println("DONE");
    }
}
