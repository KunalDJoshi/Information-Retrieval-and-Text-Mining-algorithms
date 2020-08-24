import java.util.*;

/**
 * Document clustering
 *
 */
public class Clustering {
	   String[] myDocs;
	   ArrayList<String> termList;
	   ArrayList<ArrayList<Doc>> docLists;
	   double[] docLengthVec; // length vector for each document (used for cosine similarity computation)
	   int numOfDocs;
	   Doc[] docList;
	   HashMap<String, Integer> tiMap; //HashMap to store term ids 
	   int numberOfClusters;
	   Doc[] centroids;
	   Doc[] prevCentroids;
	   ArrayList<Doc>[] clusters;
	   int vectorSize;
	//Declare attributes here
	
	/**
	 * Constructor for attribute initialization
	 * @param numC number of clusters
	 */
   public Clustering(int numC)
   {
   	//TO BE COMPLETED
	   tiMap = new HashMap<String, Integer>();
	   numberOfClusters = numC;
	   centroids = new Doc[numberOfClusters];
	   prevCentroids = new Doc[numberOfClusters];
	   clusters = new ArrayList[numberOfClusters];
	   for(int i = 0; i< numberOfClusters;i++) {
		   clusters[i] = new ArrayList<Doc>();
	   }
   }
	
	/**
	 * Load the documents to build the vector representations
	 * @param docs
	 */
	public void preprocess(String[] docs){
		myDocs = docs;
		numOfDocs = myDocs.length;
		docList = new Doc[numOfDocs];
		int termId = 0;
		
		// parse the documents to construct the vector space model
		int docId = 0;
		for(String doc:myDocs){
			String[] tokens = doc.split(" ");
			Doc obj = new Doc(docId);
			for(String token: tokens){
				if(!tiMap.containsKey(token)){
					tiMap.put(token, termId);
					obj.ti.add(termId);
					obj.tw.add(1.0);					
					termId++;
				}
				else{
					Integer tid = tiMap.get(token);
					int index = obj.ti.indexOf(tid);
					if (index >0){
						double tw = obj.tw.get(index);
						obj.tw.add(index, tw+1);
					}
					else{
						obj.ti.add(tiMap.get(token));
						obj.tw.add(1.0);
					}
				}
			}
			docList[docId] = obj;
			docId++;
		}
		
		vectorSize = termId;

		
		// compute the term tf weights and the length vector for each document
		for(Doc doc: docList){
			double[] termVector = new double[vectorSize];
			double docLength = 0;
			for(int i=0;i<doc.ti.size();i++){
				Integer tid = doc.ti.get(i);
				double tf = (1+Math.log(doc.tw.get(i)));				
				doc.tw.set(i, tf);
				docLength += Math.pow(tf, 2);
			}
			docLength = Math.sqrt(docLength);
			//normalize the doc vector			
			for(int i=0;i<doc.ti.size();i++){
				double tw = doc.tw.get(i);
				doc.tw.set(i, tw/docLength);
				termVector[doc.ti.get(i)] = tw/docLength;

			}
			doc.termVector = termVector;
		}
	}

   
	/**
	 * Cluster the documents
	 * For kmeans clustering, use the first and the ninth documents as the initial centroids
	 */
   public void cluster(){
   	//TO BE COMPLETED
	   centroids[0] = docList[0];//assigning docList of 1st document to 1st centroid
	   centroids[1] = docList[8];//assigning docList of 9th document to 2nd centroid
	   double value1,value2=0;
   	   while(terminationCond(prevCentroids,centroids)== false) {
   		   for(int i = 0; i< numberOfClusters;i++) {
   			prevCentroids[i] = centroids[i];
   		   }
   		   
   		   for(int j = 0; j< numOfDocs;j++) {
   			   value1 = calcCosineSimilarity(docList[j].termVector,centroids[0].termVector);
   			   value2 = calcCosineSimilarity(docList[j].termVector,centroids[1].termVector);
   			   
   			   if(value1>=value2) {
   				if (!clusters[0].contains(docList[j])) {
   	               clusters[1].remove(docList[j]);
   	               clusters[0].add(docList[j]);
   	            }
   	         }
   	         else {
   	            if (!clusters[1].contains(docList[j])) {
   	               clusters[0].remove(docList[j]);
   	               clusters[1].add(docList[j]);
   	            }
   			   }
   		   }
   		   
   		   for(int k = 0; k< numberOfClusters; k++) {
   			   Doc doc = new Doc();
   			   double[] vector = new double[vectorSize];
   			   for(int l = 0; l<vectorSize;l++) {
   				double addition = 0;
   				for(int m = 0 ; m< clusters[k].size();m++) {
   					addition+= clusters[k].get(m).termVector[l];
   				}
   				vector[l] = (double)addition / clusters[k].size();
   			   }
   			   
   			doc.setTermVector(vector);
   			centroids[k] = doc;
   		   }
   		   
   		   System.out.println("**************************************************");
   		   
   		   for(int n = 0 ; n<numberOfClusters; n++) {
   			   System.out.println("Number of documents in cluster " + n + " : "+ clusters[n].size());
               System.out.println("Documents are:");
   			   for(Doc doc: clusters[n]) {
                   System.out.print(doc.docId+" ");
                }
               System.out.println();
   		   }
   	   }
   }
	
   public boolean terminationCond(Doc[] oldCentroid, Doc[] newCentroid) {
	   boolean centroidEquals = true;
	   double [] termVector1,termVector2;
	   if(oldCentroid[0]==null) {
		   return false;
	   }
	   
	   for(int i = 0; i< numberOfClusters; i++) {
		   termVector1 = oldCentroid[i].termVector;
	        System.out.println("\nCentroid of cluster "+(i) +":");
	        System.out.print("[");
	        for(int m = 0; m < termVector1.length; m++) {
	        	System.out.print(termVector1[m] + ", ");
	    	}
	        System.out.println("]");
		   termVector2 = newCentroid[i].termVector;
		   
		   for(int j = 0; j<termVector1.length; j++) {
			   if(termVector1[j]!=termVector2[j]) {
				   centroidEquals = false;
			   }
		   }
	   }	   
	   return centroidEquals;
   }
   
   /**
    * Calculating the cosine similarity between documents
    */
  
    public double calcCosineSimilarity(double[] vector1, double[] vector2) {
        double dotProduct = 0.0;
        double v1 = 0.0;
        double v2 = 0.0;
        double cosineSimilarity = 0.0;
        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            v1 += Math.pow(vector1[i], 2);
            v2 += Math.pow(vector2[i], 2);
        }
        v1 = Math.sqrt(v1);
        v2 = Math.sqrt(v2);
        if (v1 != 0.0 || v2 != 0.0) {
            cosineSimilarity = dotProduct / (v1 * v2);
        }
        return cosineSimilarity;
    }

	
   public static void main(String[] args){
      String[] docs = {"hot chocolate cocoa beans",
         	 "cocoa ghana africa",
         	 "beans harvest ghana",
         	 "cocoa butter",
         	 "butter truffles",
         	 "sweet chocolate can",
         	 "brazil sweet sugar can",
         	 "suger can brazil",
         	 "sweet cake icing",
         	 "cake black forest"
         	};
      Clustering c = new Clustering(2);
   	
        c.preprocess(docs);
		System.out.println("Vector space representation:");
		for(int i=0;i<c.docList.length;i++){
			System.out.println(c.docList[i]);
		}
   
      c.cluster();
   	/*
   	 * Expected result:
   	 * Cluster: 0
   		0	1	2	3	4	
   	   Cluster: 1
   		5	6	7	8	9	
   	 */
   }
}

/**
 * 
 * Document class for the vector representation of a document
 */
class Doc {

	   int docId;
	   ArrayList<Double> tw; // term's weight in this document
	   ArrayList<Integer> ti;// term ids in this document 
	   double[] termVector;
	   
	   public Doc() {
		   
	   }
	   
	   public Doc(int did) {
	      docId = did;
	      tw = new ArrayList<Double>();
	      ti = new ArrayList<Integer>();
	   }
	  
		public void setTermVector(double[] vec){
			termVector = vec;
		}
		
		public String toString()
		{
			String str = "[";
			for(int i=0;i<termVector.length;i++){
				str += termVector[i] + ",";
			}
			return str+"]";
		}
	
}