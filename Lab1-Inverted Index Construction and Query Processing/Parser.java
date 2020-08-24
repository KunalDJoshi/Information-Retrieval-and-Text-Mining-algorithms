import java.util.*;

import javax.sound.sampled.ReverbType;

import java.io.*;

public class Parser {
    String[] myDocs;

    String[] stopList;
    ArrayList < String > arrlist = new ArrayList < String > ();

    ArrayList < String > termList; // dictionary
    ArrayList < ArrayList < Integer >> docLists; // used for each term's postings

    public Parser(String folderName, String spListPath) {
        File folder = new File(folderName);
        File[] listOfFiles = folder.listFiles();
        myDocs = new String[listOfFiles.length]; // store file names

        termList = new ArrayList < String > ();
        docLists = new ArrayList < ArrayList < Integer >> ();
        ArrayList < Integer > docList; // singular postings for a given term

        System.out.println("Unsorted document list");
        for (int i = 0; i < listOfFiles.length; i++) {
            System.out.println(listOfFiles[i].getName());
            myDocs[i] = listOfFiles[i].getName();
        }

        Arrays.sort(myDocs);

        System.out.println("Sorted document list");
        for (int i = 0; i < myDocs.length; i++) {
            System.out.println(myDocs[i]);
        }

        try {
            BufferedReader reader = new BufferedReader(new FileReader(spListPath));
            String line = "";
            line = reader.readLine();
            int count = 0;
            while (line != null) {
                arrlist.add(line);
                count++;
                line = reader.readLine();

            }

            stopList = new String[count];

            for (int j = 0; j < arrlist.size(); j++) {
                stopList[j] = arrlist.get(j);
            }

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        Arrays.sort(stopList); // pass by reference modifies stopList array

        for (int i = 0; i < myDocs.length; i++) {
            String[] tokens = parse(folderName + "/" + myDocs[i]); // parse the first file

            for (String token: tokens) {
                if (searchStopword(token) == -1) {
                    // stemming done here
                    Stemmer st = new Stemmer();
                    st.add(token.toCharArray(), token.length());
                    st.stem();

                    String stemmedToken = st.toString();

                    // add terms to inverted index here

                    if (!termList.contains(stemmedToken)) { // new term
                        termList.add(stemmedToken); // add term to dictionary
                        docList = new ArrayList < Integer > (); // postings for this term
                        docList.add(new Integer(i)); // create initial posting for the term
                        docLists.add(docList); // add postings list for this term
                    } else { // an existing term; update postings list for that term
                        int index = termList.indexOf(stemmedToken); // find index from term list
                        docList = docLists.get(index);

                        if (!docList.contains(new Integer(i))) { // not already a posting
                            docList.add(new Integer(i)); // add posting to postings
                            docLists.set(index, docList); // update postings for this term
                        }
                    }
                }
            }
        }
    }


    public String toString() {
        String matrixString = new String();
        ArrayList < Integer > docList;

        for (int i = 0; i < termList.size(); i++) {
            matrixString += String.format("%-15s", termList.get(i));
            docList = docLists.get(i);

            for (int j = 0; j < docList.size(); j++) {
                matrixString += docList.get(j) + 1 + "\t";
            }

            matrixString += "\n";
        }

        return matrixString;
    }



    public String[] parse(String fileName) {
        String[] tokens = null; // return these tokens at the end

        try {
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            String allLines = new String(); // store all lines in file in this String
            String line = null;

            line = reader.readLine();
            while (line != null) {
                allLines += line.toLowerCase(); // case folding
                line = reader.readLine();
            }

            tokens = allLines.split("[ .\"(),?!:;$%&/-]+");
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return tokens;
    }

    public int searchStopword(String key) {
        int lo = 0;
        int hi = stopList.length - 1;
        while (lo <= hi) {
            // key is in a[lo..hi] or not
            int mid = lo + (hi - lo) / 2;
            int result = key.compareTo(stopList[mid]);

            if (result < 0) // key alphabetically less than current middle stop list term
                hi = mid - 1;
            else if (result > 0) // key alphabetically greater than current middle stop list term
                lo = mid + 1;
            else
                return mid; // found stopword match
        }

        return -1; // no stopword match
    }

    // Method to handle a query with a single keyword
    public ArrayList < Integer > search(String query) {
        int index = termList.indexOf(query);

        if (index < 0) // no documents contain this keyword, return nothing
            return null;
        return docLists.get(index); // return postings for this term
    }


    // Method to handle a query with two keywords having OR operator. 
    public ArrayList < Integer > searchOr(String[] query) {
        ArrayList < Integer > result = search(query[0]); // look for first keyword
        ArrayList < Integer > postNum = new ArrayList < Integer > ();
        ArrayList < Integer > postNum1 = new ArrayList < Integer > ();
        int termId = 1;


        if (result != null) {
            for (Integer i: result) {
                postNum.add(i + 1);
            }
        }

        System.out.println("Posting of " + query[0] + " is " + postNum);
        System.out.println("The presence of " + query[0] + " is in " + postNum);


        if (result != null) {
            for (Integer i: result) {
                System.out.println("Document " + (i + 1) + ":" + myDocs[i.intValue()]);
            }
        } else
            System.out.println("No match!");

        while (termId < query.length) { // look for remaining keywords
            ArrayList < Integer > result1 = search(query[termId]); // look for current keyword

            if (result1 != null) {
                for (Integer i: result1) {
                    postNum1.add(i + 1);
                }
            }

            for (int i = 1; i < query.length; i++) {
                System.out.println("Posting of " + query[i] + " is " + postNum1);
                System.out.println("The presence of " + query[i] + " is in " + postNum1);
            }

            if (result1 != null) {
                for (Integer i: result1) {
                    System.out.println("Document " + (i + 1) + ":" + myDocs[i.intValue()]);
                }
            } else
                System.out.println("No match!");

            result = mergeOr(result, result1); // merge current list with intermediate list
            termId++;
        }

        return result;
    }

    // Method to merge two postings lists for query with two keywords having OR operator.
    private ArrayList < Integer > mergeOr(ArrayList < Integer > l1, ArrayList < Integer > l2) {
        ArrayList < Integer > mergedList = new ArrayList < Integer > ();
        int id1 = 0, id2 = 0; // positions in the respective lists
        while (id1 < l1.size() && id2 < l2.size()) {
            if (l1.get(id1).intValue() == l2.get(id2).intValue()) { // found a match
                mergedList.add(l1.get(id1));
                id1++;
                id2++;
            } else {
                if (!(mergedList.contains(l1.get(id1)))) { // add element in l1 to mergedList
                	while(id1 < l1.size()) {
                    mergedList.add(l1.get(id1));
                    id1++;
                	}
                }
                if (!(mergedList.contains(l2.get(id2)))) { // add element in l2 to mergedList
                	while(id2 < l2.size()) {
                    mergedList.add(l2.get(id2));
                    id2++;
                	}
                }      
            }
        }
        return mergedList;
    }

    // Method to handle a query with multiple keywords having AND operator.
    public ArrayList < Integer > searchAnd(String[] query) {
        int termId = 0;
        ArrayList < Integer > result = new ArrayList < Integer > ();
        ArrayList < ArrayList < Integer >> results = new ArrayList < ArrayList < Integer >> ();
        ArrayList < Integer > postNum = new ArrayList < Integer > ();
        ArrayList < Integer > postNum1 = new ArrayList < Integer > ();
        ArrayList < Integer > result1 = new ArrayList < Integer > ();
        ArrayList < Integer > result2 = new ArrayList < Integer > ();

        while (termId < query.length) {
            result = search(query[termId]); // look for first keyword
            results.add(result);
            termId++;
        }

        int n = query.length;
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                if (results.get(j).size() > results.get(j + 1).size()) {
                    String terms = query[j];
                    query[j] = query[j + 1];
                    query[j + 1] = terms;
                }

                if (results.get(j).size() > results.get(j + 1).size()) {
                    ArrayList < Integer > temp = results.get(j);
                    results.set(j, results.get(j + 1));
                    results.set(j + 1, temp);
                }
            }
        }

        result = results.get(0); // look for first keyword

        if (result != null) {
            for (Integer i: result) {
                postNum.add(i + 1);
            }
        }


        System.out.println("Posting of " + query[0] + " is " + postNum);
        System.out.println("The presence of " + query[0] + " is in " + postNum);


        if (result != null) {
            for (Integer i: result) {
                System.out.println("Document " + (i + 1) + ":" + myDocs[i.intValue()]);
            }
        } else
            System.out.println("No match!");

        int k = 1;
        Integer[] arr = new Integer[results.size()];
        while (k < results.size()) { // look for remaining keywords
            result2 = results.get(k);
            result1 = results.get(k); // look for current keyword

            int p = 0;
            while (p < result1.size()) {
                Integer value = result1.get(p);
                value = value + 1;
                result1.set(p, value);
                p++;
            }

            System.out.println("Posting of " + query[k] + " is " + result1);
            System.out.println("The presence of " + query[k] + " is in " + result1);
            if (result1 != null) {
                for (Integer i: result1) {
                    System.out.println("Document " + (i) + ":" + myDocs[(--i).intValue()]);
                }
            } else
                System.out.println("No match!");
            int q = 0;
            while (q < result1.size()) {
                Integer value = result1.get(q);
                value = value - 1;
                result1.set(q, value);
                q++;
            }

            result = mergeAnd(result, result2); // merge current list with intermediate list
            k++;
        }


        System.out.println("Keywords in AND query combined in following order:");
        for (int i = 0; i < query.length; i++) {
            System.out.println(i + 1 + " " + query[i]);
        }

        return result;
    }

    private ArrayList < Integer > mergeAnd(ArrayList < Integer > l1, ArrayList < Integer > l2) {
        ArrayList < Integer > mergedList = new ArrayList < Integer > ();
        int id1 = 0, id2 = 0; // positions in the respective lists

        while (id1 < l1.size() && id2 < l2.size()) {
            if (l1.get(id1).intValue() == l2.get(id2).intValue()) { // found a match
                mergedList.add(l1.get(id1));
                id1++;
                id2++;
            } else if (l1.get(id1) < l2.get(id2)) // l1 docId is smaller, advance l1 pointer
                id1++;
            else // l2 docId is smaller, advance l2 pointer
                id2++;
        }

        return mergedList;
    }

    public static void main(String[] args) {
        String path = "Lab1_Data";
        String stopListPath = "stopwords.txt";
        Parser p = new Parser(path, stopListPath);
        ArrayList < Integer > result;
        ArrayList < Integer > postNum = new ArrayList < Integer > ();

        System.out.println(p); //printing inverted index

        Stemmer st = new Stemmer();
        st.add("replacement".toCharArray(), "replacement".length());
        st.stem();
        //  System.out.println("stemmed: " + st.toString());
        st.add("authorization".toCharArray(), "authorization".length());
        st.stem();

        // single keyword query:
           String query = new String("time"); 
           result = p.search(query);
           
           if(result != null) {
               for(Integer i : result) {        	 
              	 postNum.add(i+1);            
               }
            }
           System.out.println("Posting of "+query+" is "+ postNum);
           System.out.println("The presence of "+query+" is in " + postNum); 

        // two keyword query with OR Operator
       /*    String[] queryOr = {"enter","show"}; 
           result = p.searchOr(queryOr);
           
           System.out.print("The presence of ");
           String str=null;
           for(int i=0; i<queryOr.length;i++) {
          	 str= queryOr[i];
          	 System.out.print(str);
          	 if(i==queryOr.length-1)
          		 break;
          	 System.out.print( " OR ");
           }  
           
           if(result != null) {
               for(Integer i : result) {        	 
              	 postNum.add(i+1);            
               }
            }
           System.out.println(" are in "+ postNum);*/
           

        // two keyword query with AND Operator
       /*  String[] queryAnd = {"good","head"}; 
          result = p.searchAnd(queryAnd);  */

        //  three or more keyword query with AND Operator
    /*    String[] queryAnd = {
            "film",
            "forest",
            "break",
            "danger"
        };
        result = p.searchAnd(queryAnd); */

 
      /*  if (result != null) {
            for (Integer i: result) {
                postNum.add(i + 1);
            }
        }

        System.out.print("The presence of ");
        String str = null;
        for (int i = 0; i < queryAnd.length; i++) {
            str = queryAnd[i];
            System.out.print(str);
            if (i == queryAnd.length - 1)
                break;
            System.out.print(" AND ");
        }
        System.out.println(" are in " + postNum); */


        if (result != null) {
            for (Integer i: result) {
                System.out.println("Document " + (i + 1) + ":" + p.myDocs[i.intValue()]);
            }
        } else
            System.out.println("No match!");

    }
}