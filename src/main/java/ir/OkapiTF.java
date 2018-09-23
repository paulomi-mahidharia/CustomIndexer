package ir;

import com.fasterxml.jackson.dataformat.yaml.snakeyaml.scanner.Scanner;

import java.io.BufferedReader;
import java.net.InterfaceAddress;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.InetAddress;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import javax.naming.directory.SearchResult;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;

import org.apache.commons.io.FileUtils;

import static ir.Catalog.createCatalog;
import static ir.GetQrelDocsForQuery.getQrelDocs;
import static util.DataFactory.*;
import static util.QueryProcessing.removeStopWordsFromQuery;
import static util.SortMap.sortScoreMap;
import static util.Stemmer.getStem;
import static util.StopWordFactory.getStopWords;

/**
 * Created by paulomimahidharia on 6/8/17.
 */
public class OkapiTF {

    private static final String CATALOG_PATH = "/Users/paulomimahidharia/Desktop/IR/CustomIndexer/FinalCatalog.txt";
    private static final String QUERY_PATH = "/Users/paulomimahidharia/Desktop/IR/resources/AP_DATA/proximityQueries.txt";

    public static void main(String[] args) throws IOException
    {

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
        System.out.println(docIdMap.size());

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
        PrintWriter writer = new PrintWriter("OkapiTF.txt", "UTF-8");

        File QueryFile = new File(QUERY_PATH);
        BufferedReader queryBR = new BufferedReader(new FileReader(QueryFile));
        String query ;
        while ((query = queryBR.readLine()) != null)

        {
            if (query.length() <= 3)
            {break;}

            HashMap<String, Float> oakpiTFMap = new HashMap<>();
            HashMap<String, Float> sortedMap;

            String queryNo = query.substring(0, 3).replace(".", "").trim();

            // Get query and qrel docs
            Set<String> qrelDocs = getQrelDocs(queryNo);

            query = query.substring(5).trim();

            // Remove stop words from the query
            StringBuffer cleanQuery = removeStopWordsFromQuery(query, stopWords);

            //Get each word in the query
            String[] cleanQueryWords = cleanQuery.toString().trim().split(" ");

            for(String term: cleanQueryWords) {

                term = term.replace(".", " ").replace(",", "").trim().toLowerCase();

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

                for(int key : TFmap.keySet()) {

                    int tfwd = TFmap.get(key);
                    int docLen = docLengthMap.get(key);

                    String docNo = String.valueOf(key);

                    Float okapiTFScore = (float) (tfwd / (tfwd + 0.5 + (1.5 * (docLen / avgLength))));
                    oakpiTFMap.put(docNo, oakpiTFMap.get(docNo) == null ? okapiTFScore : oakpiTFMap.get(docNo) + okapiTFScore);
                }
            }

            sortedMap = sortScoreMap(oakpiTFMap);

            int rank = 0;
            String nthValue = "";
            for (Entry m1 : sortedMap.entrySet()) {

                if (rank < 1000) {
                    rank = rank + 1;
                    nthValue = String.valueOf(m1.getValue());
                }
            }

            //85 Q0 AP890922-0008 1000 1.3935003 okapiTF
            System.out.println(queryNo + " " + rank);
            System.out.println(nthValue);

            rank = 0;
            for (String docNo : qrelDocs) {

                rank = rank + 1;
                int d = docIdMapRev.get(docNo);

                if(sortedMap.get(String.valueOf(d)) != null) {
                    writer.println(queryNo + " Q0 " + docNo + " " + rank + " " + sortedMap.get(String.valueOf(d)) + " OkapiTF");
                    sortedMap.remove(String.valueOf(d));
                } else {
                    writer.println(queryNo + " Q0 " + docNo + " " + rank + " " + nthValue + " OkapiTF");
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

            ArrayList<String> keys = new ArrayList<>(top1000.keySet());

            List<String> nonRelDocsForQuery = nonRelDocs.get(queryNo);
            for(String doc: nonRelDocsForQuery) {
                rank = rank + 1;
                int d = docIdMapRev.get(doc);
                writer.println(queryNo + " Q0 " + doc + " " + rank + " " + sortedMap.get(String.valueOf(d)) + " OkapiTF");
            }

//            for(int i=keys.size()-1; i>=0;i--){
//
//                if(rank < 1000) {
//                    rank = rank + 1;
//                    writer.println(queryNo + " Q0 " + keys.get(i) + " " + rank + " " + top1000.get(keys.get(i)) + " OkapiTF");
//                } else
//                    break;
//            }

//            int rank = 0;
//            for(Map.Entry m1:sortedMap.entrySet())
//            {
//                if(rank < 1000){
//                    rank = rank + 1;
//                    int d = Integer.parseInt((String) m1.getKey());
//                    writer.println(queryNo + " Q0 " + docIdMap.get(d) + " " + rank + " " +m1.getValue() + " okapiTF");
//                }
//            }

            top1000.clear();
            sortedMap.clear();
            oakpiTFMap.clear();
        }
        writer.close();
    }
}
