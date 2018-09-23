package util;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by paulomimahidharia on 6/11/17.
 */
public class DataFactory {

    public static Map<Integer, String> getDocIdMap() throws IOException {

        File docListFile = new File("/Users/paulomimahidharia/Desktop/IR/resources/AP_DATA/doclist_new_0609.txt");
        BufferedReader docListBR = new BufferedReader(new FileReader(docListFile));

        HashMap<Integer, String> docIdMap = new HashMap<Integer, String>();
        String line;
        while ((line = docListBR.readLine()) != null) {

            String[] docs = line.split(" ");

            int DocNo = Integer.parseInt(docs[0].trim());
            String DocId = docs[2];

            docIdMap.put(DocNo, DocId);
        }
        docListBR.close();

        return docIdMap;
    }

    public static Map<String, Integer> getTermIdMap() throws IOException {

        File termIdFile = new File("/Users/paulomimahidharia/Desktop/IR/CustomIndexer/TID.txt");
        BufferedReader termIdBR = new BufferedReader(new FileReader(termIdFile));

        HashMap<String, Integer> termIdMap = new HashMap<String, Integer>();
        String line;
        while ((line = termIdBR.readLine()) != null) {

            String[] termIdInfo = line.split(":");
            String term = termIdInfo[0].trim();
            int termId = Integer.parseInt(termIdInfo[1].trim());

            termIdMap.put(term, termId);
        }
        termIdBR.close();

        return termIdMap;
    }

    public static Map<Integer, Integer> getDocLengthMap() throws IOException {

        File docLengthFile = new File("/Users/paulomimahidharia/Desktop/IR/CustomIndexer/DOClen.txt");
        BufferedReader docLengthBR = new BufferedReader(new FileReader(docLengthFile));

        HashMap<Integer, Integer> docLengthMap = new HashMap<>();
        String line;
        while ((line = docLengthBR.readLine()) != null) {

            String[] docLengthInfo = line.split(":");
            int docId = Integer.parseInt(docLengthInfo[0].trim());
            int length = Integer.parseInt(docLengthInfo[1].trim());

            docLengthMap.put(docId, length);
        }
        docLengthBR.close();

        return docLengthMap;
    }

    public static float getAverageDocLength(HashMap<Integer, Integer> docLengthMap) {

        int numOfDocs = 0;
        int totalDocLengths = 0;

        for (Map.Entry m : docLengthMap.entrySet()) {

            int doclen = (Integer) m.getValue();
            numOfDocs = numOfDocs + 1;
            totalDocLengths = totalDocLengths + doclen;
        }

        return (totalDocLengths / numOfDocs);
    }

    public static String getLineFromOffset(ArrayList<Integer> offsets) throws IOException {

        int start1 = offsets.get(0);
        int end1 = offsets.get(1);
        byte[] string = null;
        File file123 = new File("/Users/paulomimahidharia/Desktop/IR/CustomIndexer/final.txt");
        RandomAccessFile randFile = new RandomAccessFile(file123, "rw");

        string = new byte[end1 - start1];
        randFile.seek(start1);
        randFile.readFully(string);
        randFile.close();
        return new String(string);
    }

    public static Map<String, Integer> getReverseDocIdMap() throws IOException {

        File docListFile = new File("/Users/paulomimahidharia/Desktop/IR/resources/AP_DATA/doclist_new_0609.txt");
        HashMap<String, Integer> docList = new HashMap<String, Integer>();
        BufferedReader docListReader = new BufferedReader(new FileReader(docListFile));
        String docListLine = "";
        while ((docListLine = docListReader.readLine()) != null) {

            String[] docs = docListLine.split(" ");
            int DocNo = Integer.parseInt(docs[0].trim());
            String DocId = docs[2];
            docList.put(DocId, DocNo);
        }
        docListReader.close();
        return  docList;
    }
}
