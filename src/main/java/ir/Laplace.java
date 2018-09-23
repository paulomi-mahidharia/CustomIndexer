package ir;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import static ir.Catalog.createCatalog;
import static ir.GetQrelDocsForQuery.getQrelDocs;
import static util.DataFactory.*;
import static util.QueryProcessing.removeStopWordsFromQuery;
import static util.SortMap.sortScoreMap;
import static util.Stemmer.getStem;
import static util.StopWordFactory.getStopWords;

/**
 * Created by paulomimahidharia on 6/10/17.
 */
public class Laplace {

    private static final float vocabulary = (float) 198196;
    private static final String CATALOG_PATH = "/Users/paulomimahidharia/Desktop/IR/CustomIndexer/FinalCatalog.txt";
    private static final String QUERY_PATH = "/Users/paulomimahidharia/Desktop/IR/resources/AP_DATA/proximityQueries.txt";

    public static void main(String[] args) throws IOException {

        // Read NonRel docs
        Map<String, List<String>> nonRelDocs = new HashMap<>();

        File nonRelDocF = new File("/Users/paulomimahidharia/Desktop/IR/CustomIndexer/NonRelDocs.txt");
        BufferedReader nonRelDocsBr = new BufferedReader(new FileReader(nonRelDocF));
        String nonRelDoc ;
        while ((nonRelDoc = nonRelDocsBr.readLine()) != null){

            String qno = nonRelDoc.split(" ")[0];
            String dno = nonRelDoc.split(" ")[1];

            List<String> docs;
            if(nonRelDocs.containsKey(qno)) {
                docs = nonRelDocs.get(qno);
            }else{
                docs = new ArrayList<>();
            }
            docs.add(dno);
            nonRelDocs.put(qno, docs);
        }

        // doc list
        Map<Integer, String> docIdMap = getDocIdMap();

        // doc list
        Map<String, Integer> docIdMapRev = getReverseDocIdMap();

        // TID
        Map<String, Integer> termIdMap = getTermIdMap();

        // Doc Len
        Map<Integer, Integer> docLengthMap = getDocLengthMap();

        // avg length
        float avgLength = getAverageDocLength((HashMap<Integer, Integer>) docLengthMap);

        //Read from Final Catalog
        File cat = new File(CATALOG_PATH);
        HashMap<Integer,ArrayList<Integer>> catalog = createCatalog(cat);

        // Get list of stop words
        Set<String> stopWords = getStopWords();

        // Prepare output file to write in
        PrintWriter writer = new PrintWriter("Laplace.txt", "UTF-8");

        File QueryFile = new File(QUERY_PATH);
        BufferedReader queryBR = new BufferedReader(new FileReader(QueryFile));
        String query ;
        while ((query = queryBR.readLine()) != null) {

            if (query.length() <= 3)
                break;

            HashMap<String, Float> LaplaceMap = new HashMap<>();
            HashMap<String, Float> sortedMap;

            String queryNo = query.substring(0, 3).replace(".", "").trim();

            // Get query and qrel docs
            Set<String> qrelDocs = getQrelDocs(queryNo);
            query = query.substring(5).trim();

            StringBuffer cleanQuery = removeStopWordsFromQuery(query, stopWords);

            String[] queryparams = cleanQuery.toString().trim().split("\\s+");

            for (String term : queryparams) {

                term = term.replace(".", "").replace(",", "").trim().toLowerCase();

                // Ignore if term has no stem
                term = getStem(term);
                if(term.equals("")) continue;

                Integer termId = termIdMap.get(term);
                if(termId == null) continue;

                HashMap<Integer, Integer> TFmap = new HashMap<>();

                // get TF map for that term
                ArrayList<Integer> offsets = catalog.get(termId);
                if (offsets != null) {

                    String termInfo = getLineFromOffset(offsets);
                    // end get TF

                    String termDocInfo = termInfo.split(":")[1];
                    String termDocInfoElements[] = termDocInfo.split(";");

                    for (String doc: termDocInfoElements) {
                        String[] docInfo = doc.split("_");

                        int docId = Integer.parseInt(docInfo[0]);
                        int tf = Integer.parseInt(docInfo[1]);
                        TFmap.put(docId, tf);
                    }
                }

                // Scan through all the docs
                for(int key : docIdMap.keySet()) {

                    String docId = docIdMap.get(key);
                    int docLen = docLengthMap.get(key);

                    int tfwd = 0;
                    if(TFmap.containsKey(key)){
                        tfwd = TFmap.get(key);
                    }

                    float LMScore = (float) Math.log((tfwd + 1)/(docLen + vocabulary));
                    LaplaceMap.put(docId, LaplaceMap.get(docId) == null? LMScore :LaplaceMap.get(docId) + LMScore);
                }
            }
            sortedMap = sortScoreMap(LaplaceMap);

            int rank = 0;
            String nthValue = "";
            for (Entry m1 : sortedMap.entrySet()) {

                if (rank < 1000) {
                    rank = rank + 1;
                    nthValue = String.valueOf(m1.getValue());
                }
            }

            System.out.println(queryNo + " " + rank);
            System.out.println(nthValue);

            rank = 0;
            for (String docNo : qrelDocs) {

                //int d = docIdMapRev.get(docNo);
                rank = rank + 1;

                if(sortedMap.get(docNo) != null) {

                    writer.println(queryNo + " Q0 " + docNo + " " + rank + " " + sortedMap.get(docNo) + " Laplace");
                    sortedMap.remove(docNo);
                } else {
                    writer.println(queryNo + " Q0 " + docNo + " " + rank + " " + nthValue + " Laplace");
                }
            }

            int localRank = 0;
            Map<String, Float> top1000 = new HashMap<>();
            for (Entry m1 : sortedMap.entrySet()) {

                if (localRank < 1000) {
                    localRank = localRank + 1;
                    top1000.put((String) m1.getKey(), (float) m1.getValue());
                }
            }

            top1000 = sortScoreMap(top1000);

            ArrayList<String> keys = new ArrayList<>(top1000.keySet());

//            for(int i=keys.size()-1; i>=0;i--){
//
//                if(rank < 1000) {
//                    rank = rank + 1;
//                    writer.println(queryNo + " Q0 " + keys.get(i) + " " + rank + " " + top1000.get(keys.get(i)) + " Laplace");
//                }
//            }

            List<String> nonRelDocsForQuery = nonRelDocs.get(queryNo);
            for(String doc: nonRelDocsForQuery) {
                rank = rank + 1;

                writer.println(queryNo + " Q0 " + doc + " " + rank + " " + sortedMap.get(doc) + " Laplace");
            }

//            for (Entry sm : sortedMap.entrySet()) {
//
//                if ((rank < 1000))
//                {
//                    rank = rank + 1;
//                    writer.println(queryNo + " Q0 " + sm.getKey() + " " + rank + " " + sm.getValue() + " Laplace");
//                }
//            }
            LaplaceMap.clear();
            sortedMap.clear();
        }
        writer.close();
    }
}





