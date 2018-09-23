package ir;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ir.Catalog.createCatalog;
import static ir.GetQrelDocsForQuery.getQrelDocs;
import static util.DataFactory.*;
import static util.QueryProcessing.removeStopWordsFromQuery;
import static util.SortMap.sortScoreMap;
import static util.Stemmer.getStem;
import static util.StopWordFactory.getStopWords;
import java.util.Map.Entry;


/**
 * Created by paulomimahidharia on 6/8/17.
 */
public class OkapiBM25 {

    private static final float b = (float) 0.75;
    private static final float k1 = (float) 1.2;
    private static final float k2 = (float) 100;
    private static final String CATALOG_PATH = "/Users/paulomimahidharia/Desktop/IR/CustomIndexer/FinalCatalog.txt";
    private static final String QUERY_PATH = "/Users/paulomimahidharia/Desktop/IR/resources/AP_DATA/proximityQueries.txt";

    public static void main(String[] args) throws IOException {
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
        HashMap<Integer, ArrayList<Integer>> catalog = createCatalog(cat);

        // Get list of stop words
        Set<String> stopWords = getStopWords();

        // Prepare output file to write in
        PrintWriter writer = new PrintWriter("OkapiBM25.txt", "UTF-8");
        PrintWriter extraDocs = new PrintWriter("NonRelDocs.txt", "UTF-8");

        File QueryFile = new File(QUERY_PATH);
        BufferedReader queryBR = new BufferedReader(new FileReader(QueryFile));
        String query;
        while ((query = queryBR.readLine()) != null)

        {
            if (query.length() <= 3) {
                break;
            }

            HashMap<String, Float> okapiBM25MAP = new HashMap<>();
            HashMap<String, Float> sortedMap;

            String queryNo = query.substring(0, 3).replace(".", "").trim();

            // Get query and qrel docs
            Set<String> qrelDocs = getQrelDocs(queryNo);

            query = query.substring(5).trim();

            String cleanQuery = removeStopWordsFromQuery(query, stopWords).toString();
            cleanQuery = cleanQuery.toLowerCase();

            //Get each word in the query
            String[] cleanQueryWords = cleanQuery.trim().split(" ");

            for (String term : cleanQueryWords) {

                term = term.replace(".", " ").replace(",", "").trim().toLowerCase();

                int tfwq = 0;
                Pattern p = Pattern.compile(term);
                Matcher m = p.matcher(cleanQuery);
                while (m.find()) {
                    tfwq++;
                }

                term = getStem(term);
                if (term.equals("")) continue;

                Integer termId = termIdMap.get(term);
                if (termId == null) continue;

                // get TF map for that term
                ArrayList<Integer> offsets = catalog.get(termId);

                HashMap<Integer, Integer> TFmap = new HashMap<>();
                float df = 0;

                if (offsets != null) {

                    String termInfo = getLineFromOffset(offsets);
                    df = Float.parseFloat(termInfo.split(":")[0].split("_")[2]);

                    String termDocInfo = termInfo.split(":")[1];
                    String termDocInfoElements[] = termDocInfo.split(";");

                    for (String doc : termDocInfoElements) {
                        String[] docInfo = doc.split("_");

                        int docId = Integer.parseInt(docInfo[0]);
                        int tf = Integer.parseInt(docInfo[1]);
                        TFmap.put(docId, tf);
                    }
                }
                //operation on each word in query for every doc
                for (int key : TFmap.keySet()) {

                    int TFWD = TFmap.get(key);
                    int lenD = docLengthMap.get(key);

                    String docNo = String.valueOf(key);

                    double term1 = Math.log((84678 + 0.5) / (df + 0.5));
                    double term2 = ((TFWD + (k1 * TFWD)) / (TFWD + k1 * ((1 - b) + b * (lenD / avgLength))));
                    double term3 = ((tfwq + (k2 * tfwq)) / (tfwq + k2));

                    float OkapiBM25 = (float) (term1 * term2 * term3);

                    okapiBM25MAP.put(docNo, okapiBM25MAP.get(docNo) == null ? OkapiBM25 : okapiBM25MAP.get(docNo) + OkapiBM25);
                }
            }
            sortedMap = sortScoreMap(okapiBM25MAP);

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

                rank = rank + 1;
                int d = docIdMapRev.get(docNo);

                if(sortedMap.get(String.valueOf(d)) != null) {
                    writer.println(queryNo + " Q0 " + docNo + " " + rank + " " + sortedMap.get(String.valueOf(d)) + " OkapiBM25");
                    sortedMap.remove(String.valueOf(d));
                } else {
                    writer.println(queryNo + " Q0 " + docNo + " " + rank + " " + nthValue + " OkapiBM25");
                }
            }

            int localRank = 0;
            Map<String, Float> top1000 = new HashMap<>();
            for (Entry m1 : sortedMap.entrySet()) {

                if (localRank < 1000) {
                    localRank = localRank + 1;
                    int d = Integer.parseInt((String) m1.getKey());
                    top1000.put(docIdMap.get(d), (float) m1.getValue());
                }
            }

            top1000 = sortScoreMap(top1000);

            ArrayList<String> keys = new ArrayList<>(sortedMap.keySet());



            for(int i=keys.size()-1; i>=0;i--){

                if(rank < 1000) {
                    rank = rank + 1;
                    int docId = Integer.parseInt(keys.get(i));
                    extraDocs.println(queryNo + " " + docIdMap.get(docId));
                    writer.println(queryNo + " Q0 " + docIdMap.get(docId) + " " + rank + " " + sortedMap.get(keys.get(i)) + " OkapiBM25");
                }
            }

//            int rank = 0;
//            for (Entry m1 : sortedMap.entrySet()) {
//
//                if (rank < 1000) {
//                    rank = rank + 1;
//                    int d = Integer.parseInt((String) m1.getKey());
//                    writer.println(queryNo + " Q0 " + docIdMap.get(d) + " " + rank + " " + m1.getValue() + " OkapiBM25");
//
////                    if(qrelDocs.contains(docIdMap.get(d))) {
////                        qrelDocs.remove(docIdMap.get(d));
////                    }
//                }
//            }
            top1000.clear();
            sortedMap.clear();

            okapiBM25MAP.clear();
        }
        extraDocs.close();
        writer.close();
    }
}
