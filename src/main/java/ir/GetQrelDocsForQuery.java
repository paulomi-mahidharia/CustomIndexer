package ir;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by paulomimahidharia on 8/1/17.
 */
public class GetQrelDocsForQuery {

//    public static void main(String args[]) throws IOException {
//
//        Set<String> docs = getQrelDocs("85");
//        System.out.println(docs.size());
//    }

    public static Set<String> getQrelDocs(String queryNo) throws IOException {
        File fqr = new File("/Users/paulomimahidharia/Desktop/IR/resources/AP_DATA/qrels.adhoc.51-100.AP89.txt");
        BufferedReader br1 = new BufferedReader(new InputStreamReader(new FileInputStream(fqr)));
        Set<String> qrelDocs = new HashSet<>();
        String line1;
        while ((line1 = br1.readLine()) != null) {
            String[] strSp = line1.split("\\s+");
            String qno = strSp[0];

            String DocNo = strSp[2].trim();

            if(qno.equals(queryNo)) {
                qrelDocs.add(DocNo);
            }
        }

        return qrelDocs;
    }
}
