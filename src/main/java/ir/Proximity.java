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
public class Proximity {

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

        File cat = new File("/Users/paulomimahidharia/Desktop/IR/CustomIndexer/FinalCatalog.txt");
        HashMap<Integer, ArrayList<Integer>> Catalog = createCatalog(cat);

        PrintWriter writer = new PrintWriter("Proximity.txt", "UTF-8");
        Float Vocabulary = (float) 198196;

        Set<String> stopWords = getStopWords();

        File queryFile = new File("/Users/paulomimahidharia/Desktop/IR/resources/AP_DATA/proximityQueries.txt");
        BufferedReader queryFileBR = new BufferedReader(new FileReader(queryFile));

        String query = "";
        while ((query = queryFileBR.readLine()) != null) {

            HashMap<String , Float> ProximityScoring = new HashMap<>();
            HashMap<String, Float> sortedMap;

            if (query.length() <= 3)
                break;

            System.out.println(query);

            String queryNo = query.substring(0, 3).replace(".", "").trim();
            int querySize = 0;

            // Get query and qrel docs
            Set<String> qrelDocs = getQrelDocs(queryNo);

            Map<String, ArrayList<String>> DocIdAndListOfLopForQuery = new HashMap<>();

            String cleanQuery = removeStopWordsFromQuery(query.substring(5), stopWords).toString().replace("-", " ").replace("\"", "");
            String[] words = cleanQuery.trim().split("\\s+");

            for (String term : words) {

                term = term.replace(".", "").replace(",", "").trim().toLowerCase();

                term = getStem(term);
                if(term.equals("")) continue;

                Integer termId = termIdMap.get(term);
                if(termId == null) continue;

                querySize = querySize + 1;

                ArrayList<Integer> offsets = Catalog.get(termId);
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
                    String termLine = new String(string);
                    String termDocInfo = termLine.split(":")[1];
                    String termDocs[] = termDocInfo.split(";");

                    for (String doc: termDocs) {

                        String[] docInfo = doc.split("_");
                        String docId = docInfo[0];
                        String LOP = docInfo[2];
                        ArrayList<String> lop = new ArrayList<>();
                        if (DocIdAndListOfLopForQuery.get(docId) == null) {
                            lop.add(LOP);
                        } else {
                            lop = DocIdAndListOfLopForQuery.get(docId);
                            lop.add(LOP);
                        }

                        DocIdAndListOfLopForQuery.put(docId, lop);
                    }
                }
            }

            for (Entry<String, ArrayList<String>> m : DocIdAndListOfLopForQuery.entrySet())
            {
                String KEY = m.getKey();
                ArrayList<String> positionListForAllTerms = m.getValue();

                int s = GetMinSpanForListofPositionsForTerms(positionListForAllTerms);
                if (positionListForAllTerms.size() >= querySize / 4 && s != 0) {

                    // k -> number of terms in the doc
                    int k = positionListForAllTerms.size();
                    int DocID = Integer.parseInt(m.getKey());
                    int lenD = docLengthMap.get(DocID);

                    float score = (1000 - s) * k / (lenD + Vocabulary);

                    ProximityScoring.put(KEY, score);
                }
                else {
                    int k = positionListForAllTerms.size();
                    int DocID = Integer.parseInt(m.getKey());
                    int lenD = docLengthMap.get(DocID);
                    float score =  k / (lenD + Vocabulary);

                    ProximityScoring.put(KEY, score);
                }

            }
            sortedMap = sortScoreMap(ProximityScoring);
            ProximityScoring.clear();


            int rank = 0;
            String nthValue = "";
            for (Entry m1 : sortedMap.entrySet()) {

                if (rank < DocIdAndListOfLopForQuery.size()) {
                    rank = rank + 1;
                    nthValue = String.valueOf(m1.getValue());
                }
            }

            System.out.println(queryNo + " " + rank);
            System.out.println(nthValue);

            rank = 0;
            for (String docNo : qrelDocs) {

                int d = docIdMapRev.get(docNo);

                if(sortedMap.get(String.valueOf(d)) != null) {
                    rank = rank + 1;
                    writer.println(queryNo + " Q0 " + docNo + " " + rank + " " + sortedMap.get(String.valueOf(d)) + " Proximity");
                    sortedMap.remove(String.valueOf(d));
                    DocIdAndListOfLopForQuery.remove(String.valueOf(d));
                } else {
                    writer.println(queryNo + " Q0 " + docNo + " " + rank + " " + nthValue + " Proximity");
                }
            }

            int localRank = 0;
            Map<String, Float> top1000 = new HashMap<>();
            for (Entry m1 : sortedMap.entrySet()) {

                if (localRank < DocIdAndListOfLopForQuery.size()) {
                    localRank = localRank + 1;
                    top1000.put((String) m1.getKey(), (float) m1.getValue());
                }
            }

            top1000 = sortScoreMap(top1000);

            ArrayList<String> keys = new ArrayList<>(top1000.keySet());

            List<String> nonRelDocsForQuery = nonRelDocs.get(queryNo);
            for(String doc: nonRelDocsForQuery) {

                if(rank < DocIdAndListOfLopForQuery.size()) {
                    rank = rank + 1;
                    int d = docIdMapRev.get(doc);
                    writer.println(queryNo + " Q0 " + doc + " " + rank + " " + sortedMap.get(String.valueOf(d)) + " Proximity");
                    sortedMap.remove(String.valueOf(d));
                    DocIdAndListOfLopForQuery.remove(String.valueOf(d));
                }
            }


            for(int i=keys.size()-1; i>=0;i--){

                if(rank < DocIdAndListOfLopForQuery.size()) {
                    rank = rank + 1;
                    writer.println(queryNo + " Q0 " + docIdMap.get(Integer.parseInt(keys.get(i))) + " " + rank + " " + top1000.get(keys.get(i)) + " Proximity");
                }
            }

//            int rank = 0;
//
//            for (Entry<String, Float> sm : sortedMap.entrySet()) {
//
//                if ((rank < DocIdAndListOfLopForQuery.size()))
//
//                {
//                    rank = rank + 1;
//                    int d = Integer.parseInt(sm.getKey());
//                    writer.println(queryNo + "  Q0  " + docIdMap.get(d) + "  " + rank + "  " + sm.getValue() + "  Proximity  ");
//                } else break;
//
//            }
            sortedMap.clear();
        }
        writer.close();
    }


    private static Integer GetMinSpanForListofPositionsForTerms(ArrayList<String> ListofPositionsForTerms) {

        HashMap<Integer, HashMap<Integer, Integer>> listofLists = new HashMap<>();

        int NoOfTerms = ListofPositionsForTerms.size();
        int no0fIts = 0;

        // list no followed by position to compare
        HashMap<Integer, Integer> WIndowPositions = new HashMap<>();
        // creating window positions
        for (int i = 1; i <= NoOfTerms; i++) {
            WIndowPositions.put(i, 1);
        }

        // creating list out of string
        int listCount = 0;
        // adding each list of positions into the map for list no, list of positions
        for (int j = 0; j < NoOfTerms; j++) {
            String[] poss = ListofPositionsForTerms.get(j).trim().split(",");

            // count followed by actual position in the doc
            HashMap<Integer, Integer> list = new HashMap<>();
            int count = 0;
            for (int i = 0; i < poss.length; i++) {
                int val = Integer.parseInt(poss[i]);
                count = count + 1;
                list.put(count, val);
            }
            listCount = listCount + 1;
            listofLists.put(listCount, list);
            no0fIts = no0fIts + list.size() - 1;
        }

        // Initialize working window
        HashMap<Integer, Integer> WorkingWindow =
                createWorkingWindow(listofLists, WIndowPositions, NoOfTerms);

        int minSpan = getSpan(WorkingWindow, NoOfTerms);

        Map<Integer, Integer> SortedWorkingWindow;

        //loop
        for (int i = 1; i <= no0fIts; i++) {
            //Iterator
            SortedWorkingWindow = sortHM1(WorkingWindow);

            //sliding
            for (Entry<Integer, Integer> m : SortedWorkingWindow.entrySet()) {

                int lestvalueList = m.getKey();
                if (WIndowPositions.get(lestvalueList) + 1 <= listofLists.get(lestvalueList).size()) {
                    WIndowPositions.put(lestvalueList, WIndowPositions.get(lestvalueList) + 1);
                    break;
                }
            }
            //end sliding

            // update working window
            WorkingWindow =
                    createWorkingWindow(listofLists, WIndowPositions, NoOfTerms);

            int span = getSpan(WorkingWindow, NoOfTerms);
            if (span < minSpan) {
                minSpan = span;
            }
        }


        return minSpan;
    }


    private static HashMap<Integer, Integer> createWorkingWindow
            (HashMap<Integer, HashMap<Integer, Integer>> listofLists,
             HashMap<Integer, Integer> WIndowPositions,
             int NoOfTerms) {

        HashMap<Integer, Integer> WorkingWindow = new HashMap<>();
        for (int i = 1; i <= NoOfTerms; i++) {
            WorkingWindow.put(i, listofLists.get(i).get(WIndowPositions.get(i)));
        }
        return WorkingWindow;
    }

    private static HashMap<Integer, Integer> sortHM1(HashMap<Integer, Integer> aMap) {

        Set<Entry<Integer, Integer>> mapEntries = aMap.entrySet();
        List<Entry<Integer, Integer>> aList = new LinkedList<>(mapEntries);

        aList.sort(Comparator.comparing(Entry::getValue));

        Map<Integer, Integer> aMap2 = new LinkedHashMap<>();
        for (Entry<Integer, Integer> entry : aList) {
            aMap2.put(entry.getKey(), entry.getValue());
        }

        return (HashMap<Integer, Integer>) aMap2;
    }

    private static Integer getSpan(HashMap<Integer, Integer> list, int NoOfTerms) {

        if (list.get(1) != null) {

            int min = list.get(1);
            int max = list.get(1);
            for (int i = 2; i <= NoOfTerms; i++) {
                if (list.get(i) < min) {
                    min = list.get(i);
                }
                if (list.get(i) > max) {
                    max = list.get(i);
                }
            }

            return (max - min);
        } else return 100000;

    }
}
