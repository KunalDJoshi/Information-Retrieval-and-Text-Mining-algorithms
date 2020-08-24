import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

class Node {
    Node left;
    Node right;
    String data;

    public Node(String string) {
        data = string;
    }
}

public class BinarySearchTree {
    Node root; // root node of the entire tree

    String[] myDocs; // document collection
    ArrayList < String > termList; // dictionary
    ArrayList < ArrayList < Integer >> docLists; // used for each term's postings
    Map < String, ArrayList < Integer >> map = new HashMap < > (); // used for storing term lists and their postings lists
    TreeMap < String, ArrayList < Integer >> sorted = new TreeMap < > ();
    ArrayList < String > treeKeyList = new ArrayList < String > ();
    ArrayList < ArrayList < Integer >> treeValuesList = new ArrayList < ArrayList < Integer >> ();


    public BinarySearchTree(String folderName) {
        File folder = new File(folderName);
        File[] listOfFiles = folder.listFiles();
        myDocs = new String[listOfFiles.length];
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

        // System.out.println("myDocs.length:"+myDocs.length);
        for (int i = 0; i < myDocs.length; i++) {
            String[] tokens = parse(folderName + "/" + myDocs[i]); // perform basic tokenization
            String token;

            for (int j = 0; j < tokens.length; j++) {
                token = tokens[j];
                if (!termList.contains(token)) { // new term
                    termList.add(token); // add term to dictionary
                    docList = new ArrayList < Integer > (); // postings for this term
                    docList.add(new Integer(i)); // create initial posting for the term
                    docLists.add(docList); // add postings list for this term

                    map.put(token, docList);

                } else { // an existing term; update postings list for that term
                    int index = termList.indexOf(token); // find index from term list
                    docList = docLists.get(index);

                    if (!docList.contains(new Integer(i))) { // not already a posting
                        docList.add(new Integer(i)); // add posting to postings
                        docLists.set(index, docList); // update postings for this term

                        map.put(token, docList);
                    }
                }
            }
        }

        // sort keys in ascending order       
        sorted.putAll(map);

        // converting TreeMap keys into ArrayList
        Set < String > keySet = sorted.keySet();
        treeKeyList = new ArrayList < String > (keySet);

        // converting TreeMap values into ArrayList
        Collection < ArrayList < Integer >> values = sorted.values();
        treeValuesList = new ArrayList < ArrayList < Integer >> (values);

        int start = 0;
        int end = treeKeyList.size() - 1;
        int mid = (start + end) / 2;
        root = new Node(treeKeyList.get(mid));

        // left side of array passed to left subtree
        insert(root, treeKeyList, start, mid - 1);
        // right side of array passed to right subtree
        insert(root, treeKeyList, mid + 1, end);

        // calling visualizeTree method
        visualizeTree(root);
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
        ArrayList < Integer > docList;

        for (int i = 0; i < treeKeyList.size(); i++) {
            matrixString += String.format("%-15s", treeKeyList.get(i));
            docList = treeValuesList.get(i);

            for (int j = 0; j < docList.size(); j++) {
                matrixString += docList.get(j) + "\t";
            }

            matrixString += "\n";
        }

        return matrixString;
    }

    public void insert(Node node, ArrayList < String > termList2, int start, int end) {
        int cmp = 0;
        if (start <= end) {
            int mid = (start + end) / 2;
            String s1 = termList2.get(mid);
            String s2 = node.data;
            cmp = s1.compareTo(s2);
            if (cmp < 0) { // left subtree
                node.left = new Node(termList2.get(mid));
                insert(node.left, termList2, start, mid - 1);
                insert(node.left, termList2, mid + 1, end);
            } else { // right subtree
                node.right = new Node(termList2.get(mid));
                insert(node.right, termList2, start, mid - 1);
                insert(node.right, termList2, mid + 1, end);
            }
        }
    }

    public void inorderTraversal(Node node) {
        // print the contents of the tree in increasing order
        if (node != null) {
            inorderTraversal(node.left);
            System.out.println("Traversed " + node.data); // print node's key value
            inorderTraversal(node.right);
        }
    }

    public Node search(Node node, String key) {
        int cmp = 0;
        int index = 0;
        ArrayList < Integer > result = new ArrayList < Integer > ();
        String s1 = node.data;
        String s2 = key;
        cmp = s2.compareTo(s1);
        if (node == null)
            // hitting an empty node means search has failed
            return null;
        if (node.data.equals(key)) {
            // found a match, return the Node's data
            index = treeKeyList.indexOf(key);
            result = treeValuesList.get(index);
            System.out.println("Posting of " + key + " is " + treeValuesList.get(index));

            System.out.println("The term " + key + " is present in " + result);

            if (result != null) {
                for (Integer i: result) {
                    System.out.println("Document " + (i) + ":" + myDocs[i.intValue()]);
                }
            } else
                System.out.println("No match!");

            return node;
        } else if (0 > cmp)
            // need to search the left subtree since key is less than node value
            return search(node.left, key);
        else
            // key value is larger than current node, search right subtree
            return search(node.right, key);
    }

    public Node search(Node node, String[] keys) {
        Node node1 = node;
        Node node2 = null;
        int index = 0;
        ArrayList < Integer > result = new ArrayList < Integer > ();
        ArrayList < Integer > result1 = new ArrayList < Integer > ();
        for (int i = 0; i < keys.length; i++) {
            node2 = search(node1, keys[i]);
        }

        index = treeKeyList.indexOf(keys[0]);
        result = treeValuesList.get(index);

        int termId = 1;
        while (termId < keys.length) {
            index = treeKeyList.indexOf(keys[termId]);
            result1 = treeValuesList.get(index);
            result = merge(result, result1);
            termId++;
        }

        System.out.println("The conjuctive query is present in " + result);

        if (result != null) {
            for (Integer i: result) {
                System.out.println("Document " + (i) + ":" + myDocs[i.intValue()]);
            }
        } else
            System.out.println("No match!");
        return node2;
    }


    public ArrayList < Integer > merge(ArrayList < Integer > l1, ArrayList < Integer > l2) {
        ArrayList < Integer > mergedList = new ArrayList < Integer > ();
        int id1 = 0, id2 = 0;
        while (id1 < l1.size() && id2 < l2.size()) {
            if (l1.get(id1).intValue() == l2.get(id2).intValue()) {
                mergedList.add(l1.get(id1));
                id1++;
                id2++;
            } else if (l1.get(id1) < l2.get(id2))
                id1++;
            else
                id2++;
        }
        return mergedList;
    }

    public void visualizeTree(Node node) {

        try {
            FileWriter myWriter = new FileWriter("tree.txt");

            myWriter.write("                                   " + node.data + "\n");
            myWriter.write("              " + node.left.data);
            myWriter.write("                                   " + node.right.data + "\n");
            myWriter.write("      " + node.left.left.data);
            myWriter.write("              " + node.left.right.data);
            myWriter.write("              " + node.right.left.data);
            myWriter.write("              " + node.right.right.data + "\n");
            myWriter.write("" + node.left.left.left.data);
            myWriter.write("   " + node.left.left.right.data);
            myWriter.write("   " + node.left.right.left.data);
            myWriter.write("    " + node.left.right.right.data);
            myWriter.write("       " + node.right.left.left.data);
            myWriter.write("    " + node.right.left.right.data);
            myWriter.write("   " + node.right.right.left.data);
            myWriter.write("     " + node.right.right.right.data);
            myWriter.close();
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }


    public static void main(String args[]) {
        String path = "Lab1_Data";

        BinarySearchTree bst = new BinarySearchTree(path);

        System.out.println(bst); //printing inverted index

        System.out.println("Inorder tree traversal");
        bst.inorderTraversal(bst.root);
        System.out.println("bst.root is: " + bst.root.data);

        // single keyword query:
        String query = new String("time");
        Node node = bst.search(bst.root, query);
        if (node != null)
            System.out.println("Found " + node.data);
        else
            System.out.println("No match");


        // two keyword query with AND Operator
        String[] queryAnd2 = {
            "scenes",
            "still",
        };
        Node node1 = bst.search(bst.root, queryAnd2);
        
        // three keyword query with AND Operator
        String[] queryAnd3 = {
            "just",
            "know",
            "like"
        };
        Node node2 = bst.search(bst.root, queryAnd3);

    }
}