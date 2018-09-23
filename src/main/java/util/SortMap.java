package util;

import java.util.*;

/**
 * Created by paulomimahidharia on 6/8/17.
 */
public class SortMap {

    public static HashMap<String, Float> sortScoreMap(Map<String, Float> aMap) {

        Set<Map.Entry<String,Float>> mapEntries = aMap.entrySet();
        List<Map.Entry<String,Float>> aList = new LinkedList<Map.Entry<String,Float>>(mapEntries);

        Collections.sort(aList, new Comparator<Map.Entry<String,Float>>() {


            public int compare(Map.Entry<String, Float> ele1,
                               Map.Entry<String, Float> ele2) {

                return ele2.getValue().compareTo(ele1.getValue());
            }
        });

        Map<String,Float> aMap2 = new LinkedHashMap<>();
        for(Map.Entry<String,Float> entry: aList) {
            aMap2.put(entry.getKey(), entry.getValue());
        }

        return (HashMap<String, Float>) aMap2;
    }

    public static HashMap<String, Integer> sortHM(Map<String, Integer> aMap) {

        Set<Map.Entry<String, Integer>> mapEntries = aMap.entrySet();

        List<Map.Entry<String, Integer>> aList = new LinkedList<Map.Entry<String, Integer>>(mapEntries);

        Collections.sort(aList, new Comparator<Map.Entry<String, Integer>>() {

            public int compare(Map.Entry<String, Integer> ele1,

                               Map.Entry<String, Integer> ele2) {

                return ele2.getValue().compareTo(ele1.getValue());

            }

        });

        Map<String, Integer> aMap2 = new LinkedHashMap<String, Integer>();

        for (Map.Entry<String, Integer> entry : aList) {

            aMap2.put(entry.getKey(), entry.getValue());
        }
        return (HashMap<String, Integer>) aMap2;
    }
}
