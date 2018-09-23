package ir;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by paulomimahidharia on 6/7/17.
 */
public class Catalog {

    public static void main(String[] args) throws IOException {
        File cat = new File("/Users/paulomimahidharia/Desktop/IR/CustomIndexer/final.txt");
        HashMap<Integer, ArrayList<Integer>> finalCat = createCatalog(cat);

        PrintWriter writer = new PrintWriter("FinalCatalog.txt", "UTF-8");

        for (Map.Entry m : finalCat.entrySet()) {
            ArrayList<Integer> al = (ArrayList<Integer>) m.getValue();
            writer.println(m.getKey() + ":" + al.get(0) + ":" + al.get(1));
        }

        writer.close();
    }


    public static HashMap<Integer,ArrayList<Integer>>  createCatalog(File FileName) {

        HashMap<Integer,ArrayList<Integer>> Offset = new HashMap<Integer,ArrayList<Integer>>();


        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(FileName)));
        } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        String line;

        try {
            while( (line = br.readLine()) != null){

                String[] p = line.split(":");
                int key = Integer.parseInt(p[0]);
                ArrayList<Integer> arraylist = new ArrayList<>();
                int start = Integer.parseInt(p[1]);
                arraylist.add(start);
                int end = Integer.parseInt(p[2]);
                arraylist.add(end);

                Offset.put(key, arraylist);
            }
        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return (HashMap<Integer,ArrayList<Integer>>) Offset;
    }
}
