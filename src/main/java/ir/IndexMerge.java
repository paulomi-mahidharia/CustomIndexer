package ir;

import java.io.*;
import java.util.*;

/**
 * Created by paulomimahidharia on 6/7/17.
 */
public class IndexMerge {

    public static void main(String[] args) throws IOException {
        String line;

        File file1 = new File("/Users/paulomimahidharia/Desktop/IR/CustomIndexer/InvInd1.txt");
        File file2 = new File("/Users/paulomimahidharia/Desktop/IR/CustomIndexer/InvInd2.txt");

        File file12 = mergeFiles(file1, file2);
        BufferedReader br1 = new BufferedReader(new FileReader(file12));
        PrintWriter writer1 = new PrintWriter("final.txt", "UTF-8");
        while ((line = br1.readLine()) != null) {

            writer1.println(line);
        }
        br1.close();
        writer1.close();

        String filename = "/Users/paulomimahidharia/Desktop/IR/CustomIndexer/InvInd*.txt";
        for (int i = 3; i <= 61; i++) {

            StringBuilder sb = new StringBuilder();
            sb.append("");
            sb.append(i);
            String fileNo = sb.toString();

            String fn = filename.replace("*", fileNo);


            File fileFinal = new File("/Users/paulomimahidharia/Desktop/IR/CustomIndexer/final.txt");
            BufferedReader br3 = new BufferedReader(new FileReader(fileFinal));
            PrintWriter writer3 = new PrintWriter("finalTemp.txt", "UTF-8");
            while ((line = br3.readLine()) != null) {

                writer3.println(line);
            }
            br3.close();
            writer3.close();

            File fileFinalTemp = new File("/Users/paulomimahidharia/Desktop/IR/CustomIndexer/finalTemp.txt");
            File fileFN = new File(fn);

            System.out.println("adding file :" + i);
            File fileM = mergeFiles(fileFN, fileFinalTemp);

            BufferedReader br2 = new BufferedReader(new FileReader(fileM));
            PrintWriter writer2 = new PrintWriter("final.txt", "UTF-8");
            while ((line = br2.readLine()) != null) {

                writer2.println(line);
            }
            br2.close();
            writer2.close();
        }
    }

    private static File mergeFiles(File II1, File II2) {

        HashMap<Integer, ArrayList<Integer>> Offset1 = createCatalog(II1);
        HashMap<Integer, ArrayList<Integer>> Offset2 = createCatalog(II2);

        PrintWriter writer = null;
        try {
            writer = new PrintWriter("mergedOptm.txt", "UTF-8");

        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        for (Map.Entry m : Offset1.entrySet()) {

            int termId = (Integer) m.getKey();
            if (Offset2.get(termId) != null) {

                // Term is contained in both files
                String str1 = getLineOfFileWithtermIdAndCatalog(Offset1, termId, II1);
                String str2 = getLineOfFileWithtermIdAndCatalog(Offset2, termId, II2);
                String str12 = MergeSortOf2Lines(str1, str2);
                str12 = str12.trim();

                writer.println(str12);
            } else {

                // Term is only in 1st file
                String str1 = getLineOfFileWithtermIdAndCatalog(Offset1, termId, II1);
                str1 = str1.trim();
                //str1 = str1.replace(" ", "").replace("[", "").replace("]", "");
                writer.println(str1);

            }

        }


        for (Map.Entry m : Offset2.entrySet()) {

            int termId = (Integer) m.getKey();

            if (Offset1.get(termId) != null) {
                continue;
            } else {
                String str2 = getLineOfFileWithtermIdAndCatalog(Offset2, termId, II2);
                str2 = str2.trim();
                //str2 = str2.replace(" ", "").replace("[", "").replace("]", "");
                writer.println(str2);

            }

        }


        writer.close();
        Offset2.clear();
        Offset1.clear();
        File file = new File("/Users/paulomimahidharia/Desktop/IR/CustomIndexer/mergedOptm.txt");
        return (file);


    }


    private static String MergeSortOf2Lines(String str1, String str2) {
        String str12 = "";
        str1 = str1.trim();
        str2 = str2.trim();

        // Line 1
        String[] term1_IdAndToken = str1.split(":");

        String[] term1_Info = term1_IdAndToken[0].split("_");
        int term1_Id = Integer.parseInt(term1_Info[0]);
        int term1_TTF = Integer.parseInt(term1_Info[1]);
        int term1_DF = Integer.parseInt(term1_Info[2]);
        String term1_token = term1_IdAndToken[1];

        String[] tuples1 = term1_token.split(";");
        int len1 = tuples1.length;

        // Line 2
        String[] term2_IdAndToken = str2.split(":");

        String[] term2_Info = term2_IdAndToken[0].split("_");
        int term2_Id = Integer.parseInt(term2_Info[0]);
        int term2_TTF = Integer.parseInt(term2_Info[1]);
        int term2_DF = Integer.parseInt(term2_Info[2]);

        String token2 = term2_IdAndToken[1];
        String[] tuples2 = token2.split(";");
        int len2 = tuples2.length;


        if (term1_Id == term2_Id) {
            str12 = str12 + term2_Id + "_" + (term1_TTF + term2_TTF) + "_" + (term1_DF + term2_DF) +":";

            String tuple1 = "";
            String tuple2 = "";

            int i = 0, j = 0;
            while (i < len1 && j < len2) {

                tuple1 = tuples1[i];
                tuple2 = tuples2[j];
                int Tf1 = GetTfOfTuple(tuple1);
                int Tf2 = GetTfOfTuple(tuple2);

                if (Tf1 < Tf2) {
                    str12 = str12 + tuple2 + ";";
                    j++;
                } else {
                    str12 = str12 + tuple1 + ";";
                    i++;
                }
            }

            while (i < len1) {
                tuple1 = tuples1[i];
                str12 = str12 + tuple1 + ";";
                i++;

            }

            while (j < len2) {
                tuple2 = tuples2[j];
                str12 = str12 + tuple2 + ";";
                j++;

            }
        } else {
            System.out.println("keys of 2 lines do not match");
        }

        return str12;
    }

    private static Integer GetTfOfTuple(String tuple) {

        String[] DocIdTfLop = tuple.split("_");
        String Tf = DocIdTfLop[1];
        int tfTupple = Integer.parseInt(Tf);
        return tfTupple;
    }

    // Returns Map<TID, [start offset, end offset]>
    public static HashMap<Integer, ArrayList<Integer>> createCatalog(File FileName) {
        System.out.println("creating cat");
        HashMap<Integer, ArrayList<Integer>> Offset = new HashMap<Integer, ArrayList<Integer>>();

        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(FileName)));
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }
        String line;
        int start = 0;
        int end = 0;
        try {
            while ((line = br.readLine()) != null) {
                int lengthOfLine = line.length();
                end = start + lengthOfLine;
                String[] IdAndIl = line.split(":");

                String key1 = IdAndIl[0].split("_")[0];
                int key = Integer.parseInt(key1);

                ArrayList<Integer> arraylist = new ArrayList<>();
                arraylist.add(start);
                arraylist.add(end);

                Offset.put(key, arraylist);
                start = end + 1;
            }
        } catch (NumberFormatException | IOException e) {
            e.printStackTrace();
        }
        System.out.println("creating cat done");
        return Offset;
    }


    private static String getLineOfFileWithtermIdAndCatalog (HashMap<Integer, ArrayList<Integer>> Offset, int termId, File file) {

        ArrayList<Integer> arraylisttemp = Offset.get(termId);
        int start = arraylisttemp.get(0);
        int end = arraylisttemp.get(1);

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
