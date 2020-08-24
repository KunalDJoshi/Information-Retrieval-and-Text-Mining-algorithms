import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class NBClassifier {
    File[] classFiles;
    HashSet < String > vocabulary; // entire vocabuary
    HashMap < String, Double > [] condProb; // one hash map for each class
    int numClasses;
    int[] classTokenCounts; // total number of terms per class (includes duplicate terms)
    double[] priorProbability;
    int[] classDocumentCnt;
    ArrayList < ArrayList < Integer >> LabelPredictions;

    /**
    * Build a Naive Bayes classifier using a training document set  
    * @param trainDataFolder the training document folder  
    */
    public NBClassifier(File trainingDataFolder) {
        vocabulary = new HashSet < String > ();
        File[] listOfFiles = trainingDataFolder.listFiles();
        numClasses = listOfFiles.length;
        condProb = new HashMap[numClasses];
        classTokenCounts = new int[numClasses];
        priorProbability = new double[numClasses];
        classDocumentCnt = new int[numClasses];

        classFiles = new File[listOfFiles.length];
        int totalTrainingDocs = 0;
        for (int i = 0; i < numClasses; i++) {
            if (listOfFiles[i].isDirectory()) {
                classFiles[i] = listOfFiles[i];
                classDocumentCnt[i] = classFiles[i].listFiles().length;
                condProb[i] = new HashMap < String, Double > ();
                totalTrainingDocs += classDocumentCnt[i];
            }
        }

        for (int j = 0; j < classFiles.length; j++) {
            ArrayList < String > tokens = preprocess(classFiles[j], j);
            classTokenCounts[j] = tokens.size();

            // collecting the token counts
            for (String token: tokens) {
                vocabulary.add(token);

                if (condProb[j].containsKey(token)) {
                    double count = condProb[j].get(token);
                    condProb[j].put(token, count + 1);
                } else
                    condProb[j].put(token, 1.0);
            }
        }

        // computing the class conditional probability using Laplace smoothing
        for (int i = 0; i < numClasses; i++) {
            Iterator < Map.Entry < String, Double >> iterator = condProb[i].entrySet().iterator();
            int vSize = vocabulary.size();

            while (iterator.hasNext()) {
                Map.Entry < String, Double > entry = iterator.next();
                String token = entry.getKey();
                Double count = entry.getValue();
                count = (count + 1) / (classTokenCounts[i] + vSize);
                condProb[i].put(token, count);
            }
            priorProbability[i] = (double) classDocumentCnt[i] / totalTrainingDocs;// prior probability of class

        }
    }
    /**	
     * Load the training documents  
     * @param trainDataFolder  
     */
    public ArrayList < String > preprocess(File trainDataFolder, int classNum) {
        File[] fileList = trainDataFolder.listFiles();
        ArrayList < String > tokens = new ArrayList < String > ();
        for (int i = 0; i < fileList.length; i++) {
            ArrayList < String > puretokens = tokenization(fileList[i]);
            tokens.addAll(puretokens);
        }

        return tokens;
    }


    public ArrayList < String > tokenization(File fileName) {
        String[] tokens = null; // return these tokens at the end
        ArrayList < String > pureTokens = new ArrayList < String > ();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            String allLines = new String(); // store all lines in file in this String
            String line = null;

            line = reader.readLine();
            while (line != null) {
                allLines += line;
                line = reader.readLine();
            }

            tokens = allLines.split("[ .\"()_,?!:;$%&/-]+");
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        for (String token: tokens) {
            pureTokens.add(token);
        }
        return pureTokens;
    }

    /** Classify a test doc   
     * @param doc test doc   
     * @return class label   
     */
    public int classify(ArrayList < String > tokens) {
        double[] score = new double[numClasses]; // class likelihood for each class
        int label = 0;
        int vSize = vocabulary.size();
        for (String token: tokens) {
            for (int i = 0; i < numClasses; i++) {
                if (condProb[i].containsKey(token)) {
                	// term's class conditional probability
                    score[i] += Math.log(condProb[i].get(token));
                } else {
                	// previously unknown term, compute its Laplace smoothed class conditional probability
                    score[i] += Math.log(1.0 / (classTokenCounts[i] + vSize));
                }
            }
        }
        for (int i = 0; i < numClasses; i++) {
            score[i] += Math.log(priorProbability[i]);
        }
        double maxScore = score[0];
     // find the largest class likelihood and save its label to return as the class value
        for (int i = 1; i < numClasses; i++) {
            if (score[i] > maxScore) {
                maxScore = score[i];
                label = i;
            }
        }
        return label;
    }


    /**   Classify a set of testing documents and report the accuracy  
     * @param testDataFolder fold that contains the testing documents   
     * @return classification accuracy  
     */
    public double classifyAll(File testingDataFolder) throws IOException {
        File listOfFiles[] = testingDataFolder.listFiles();
        LabelPredictions = new ArrayList < ArrayList < Integer >> ();
        int correctPredictions = 0;
        int totalTestDocs = 0;
        int truePos = 0;
        int trueNeg = 0;
        int falsePos = 0;
        int falseNeg = 0;
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isDirectory()) {
                ArrayList < Integer > classLabelPredictions = new ArrayList < Integer > ();
                File[] docs = listOfFiles[i].listFiles();
                for (int j = 0; j < docs.length; j++) {
                    ArrayList < String > puretokens = tokenization(docs[j]);
                    classLabelPredictions.add(classify(puretokens));
                }
                LabelPredictions.add(classLabelPredictions);
            }
        }
        //Compute the Classification Accuracy

        for (int i = 0; i < LabelPredictions.size(); i++) {
            ArrayList < Integer > classPrediction = LabelPredictions.get(i);
            totalTestDocs += classPrediction.size();
            for (int predictedLabel: classPrediction) {
                if (predictedLabel == i) {
                    correctPredictions++;
                    if (i == 0) {
                        trueNeg++;
                    } else {
                        truePos++;
                    }
                } else {
                    if (predictedLabel == 0) {
                        falseNeg++;
                    } else {
                        falsePos++;
                    }
                }
            }
        }
        double accuracy = (double)(trueNeg + truePos) / totalTestDocs;
        System.out.println("Correctly classified " + correctPredictions + " out of " + totalTestDocs);
        return accuracy;
    }

    public static void main(String[] args) {
        String trainingDocs = "Lab4_Data\\train";
        File trainfile = new File(trainingDocs);

        NBClassifier nb = new NBClassifier(trainfile);

        String testDocs = "Lab4_Data\\test";
        File testfile = new File(testDocs);
        try {
            System.out.println("Classification Accuracy: " + nb.classifyAll(testfile));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}