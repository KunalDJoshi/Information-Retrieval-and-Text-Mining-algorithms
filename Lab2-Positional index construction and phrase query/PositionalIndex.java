import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class PositionalIndex {
    String[] myDocs;
    ArrayList < String > termList; // dictionary
    ArrayList < ArrayList < DocId >> docLists;

    public PositionalIndex(String folderName) {
        File folder = new File(folderName);
        File[] listOfFiles = folder.listFiles();
        myDocs = new String[listOfFiles.length];
        termList = new ArrayList < String > ();
        docLists = new ArrayList < ArrayList < DocId >> (); // postings list
        ArrayList < DocId > docList; // postings for a single term

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

        for (int i = 0; i < myDocs.length; i++) {
            // String[] tokens = myDocs[i].split(	" ");
            String[] tokens = parse(folderName + "/" + myDocs[i]);
            String token;

            for (int j = 0; j < tokens.length; j++) {
                token = tokens[j];

                if (!termList.contains(token)) { // is this term in the dictionary?
                    termList.add(token);
                    docList = new ArrayList < DocId > ();
                    DocId doid = new DocId(i, j); // document ID and position passed in
                    docList.add(doid); // add to postings for this term
                    docLists.add(docList); // add row to postings list
                } else { // term is in dictionary, need to make updates
                    int index = termList.indexOf(token);
                    docList = docLists.get(index);
                    int k = 0;
                    boolean match = false; // did we already see this document?
                    // search the postings for a document id
                    // if match, insert a new position for this document
                    for (DocId doid: docList) {
                        if (doid.docId == i) { // we've seen term in this document before
                            doid.insertPosition(j); // add a position to the position list
                            docList.set(k, doid); // update position list
                            match = true;
                            break;
                        }
                        k++;
                    }

                    // if no match, add new document Id to the list, along with position
                    if (!match) {
                        DocId doid = new DocId(i, j);
                        docList.add(doid);
                    }
                }
            }
        }
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

    public String toString() {
        String matrixString = new String();
        ArrayList < DocId > docList;

        for (int i = 0; i < termList.size(); i++) {
            matrixString += String.format("%-15s", termList.get(i));
            docList = docLists.get(i);

            for (int j = 0; j < docList.size(); j++) {
                matrixString += docList.get(j) + "\t"; // DocId has a toString method
            }

            matrixString += "\n";
        }

        return matrixString;
    }

    //intersect method to find adjacent terms in phrase query in document 
    public ArrayList < Integer > intersect(String q1, String q2) {
        ArrayList < Integer > mergedList = new ArrayList < Integer > ();
        ArrayList < DocId > l1 = docLists.get(termList.indexOf(q1)); // first term's doc list
        ArrayList < DocId > l2 = docLists.get(termList.indexOf(q2)); // second term's doc list
        int id1 = 0, id2 = 0; // doc list pointers

        while (id1 < l1.size() && id2 < l2.size()) {
            // if both terms appear in the same document
            if (l1.get(id1).docId == l2.get(id2).docId) {
                // get the position information for both terms
                ArrayList < Integer > pp1 = l1.get(id1).positionList;
                ArrayList < Integer > pp2 = l2.get(id2).positionList;
                int pid1 = 0, pid2 = 0; // position list pointers

                // determine if the two terms have an adjacency in the current document
                // if it does, stop comparing the position lists and add the document ID
                // to the mergedList

                int k = 0;
                while (pid1 < pp1.size() && pid2 < pp2.size()) {

                    k = pp2.get(pid2) - pp1.get(pid1);

                    if (k == 1) {
                        mergedList.add(l1.get(id1).docId);
                        System.out.println("Found " + q1 + " and " + q2 + " adjacent, saving second position: " + l1.get(id1).docId + ":" + pp2.get(pid2));
                        pid1++;
                        pid2++;

                    } else if (k < 1) {
                        pid2++;
                    } else if (k > 1) {
                        pid1++;
                    }
                }

                id1++;
                id2++;
            } else if (l1.get(id1).docId < l2.get(id2).docId)
                id1++;
            else
                id2++;
        }

        return mergedList;
    }

    //phraseQuery method accepts phrase query with multiple terms and return documents 
    public ArrayList < Integer > phraseQuery(String str) {
        ArrayList < Integer > result = new ArrayList < Integer > ();
        ArrayList < Integer > result1 = new ArrayList < Integer > ();
        ArrayList < Integer > result2 = new ArrayList < Integer > ();
        ArrayList < Integer > mergedList = new ArrayList < Integer > ();
        ArrayList < ArrayList < Integer >> results = new ArrayList < ArrayList < Integer >> ();
        int id1 = 0, id2 = 0;

        String[] query = str.split("[ ]");

        for (int i = 0; i < query.length; i++) {
            System.out.println("Posting of " + query[i] + " is: " + docLists.get(termList.indexOf(query[i])));
        }


        if (query.length == 2) {
            for (int i = 0; i < query.length - 1; i++) {
                result = intersect(query[i], query[i + 1]);
            }

            return result;
        } else {
            for (int i = 0; i < query.length - 1; i++) {
                result = intersect(query[i], query[i + 1]);
                results.add(result);
            }


            for (int i = 0; i < results.size() - 1; i++) {
                result1 = results.get(i);
                result2 = results.get(i + 1);
                while (id1 < result1.size() && id2 < result2.size()) {
                    if (result1.get(id1).intValue() == result2.get(id2).intValue()) { // found a match
                        mergedList.add(result1.get(id1));
                        id1++;
                        id2++;
                    } else if (result1.get(id1) < result2.get(id2)) // result1 docId is smaller, advance result1 pointer
                        id1++;
                    else // result2 docId is smaller, advance result2 pointer
                        id2++;
                }
            }

            return mergedList;
        }
    }

    public static void main(String[] args) {

        String path = "Lab1_Data";

        PositionalIndex pi = new PositionalIndex(path);
        System.out.println(pi);

        String query = "awfully cloying";
      /*  String query = "movies like these";
        String query = "collect dust on video "; 
        String query = "different scenes offering further insight";*/
        ArrayList < Integer > result = pi.phraseQuery(query);

        System.out.println("The phrase query is present in following documents:");

        if (result.size() != 0) {
            for (Integer i: result)
                System.out.println("Document " + i.intValue() + ": " + pi.myDocs[i.intValue()]);
        } else
            System.out.println("No adjacency found!");
    }
}

class DocId {
    int docId;
    ArrayList < Integer > positionList;

    public DocId(int did, int position) {
        docId = did;
        positionList = new ArrayList < Integer > ();
        positionList.add(new Integer(position));
    }

    public void insertPosition(int position) {
        positionList.add(new Integer(position));
    }

    public String toString() {
        String docIdString = "" + docId + ":<";
        for (Integer pos: positionList)
            docIdString += pos + ",";

        // remove extraneous final comma
        docIdString = docIdString.substring(0, docIdString.length() - 1) + ">";
        return docIdString;
    }
}