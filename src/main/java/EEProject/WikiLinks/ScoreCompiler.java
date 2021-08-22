package EEProject.WikiLinks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.ops.transforms.Transforms;


public class ScoreCompiler {
	private String[] linkPair;
	private String stem;
	
	private boolean valid;

	private boolean subSection;
	private ArrayList<String> tokens = new ArrayList<String>();
	private ArrayList<String> topTokens = new ArrayList<String>();
	private Hashtable<String, Double> topTokensWithValues = new Hashtable<String, Double>();
	private ArrayList<String> title = new ArrayList<String>();
	
	private Page searchPage;
	private ArrayList<ArrayList<Integer>> occurences = new ArrayList<ArrayList<Integer>>();
	
	private double disScore;
	private int disCount;
	private int totalOccurences;
	private int wordCount;
	
	private double startTimeSearch;
	private double endTimeSearch;
	
	private double tokenProximityFactor = (double)this.disScore/this.disCount;
	private double unCurvedProximity;
	private double tokenWordCountFactor;
	private double tfIdfScore;
	private double unCurvedWordCount;
	private double titleMatchFactor;
	private double titleSimilarity;
	private double contentSimilarity;
	private double singleWordReliancePenalty;
	private double finalScore;
	
	public ScoreCompiler(String[] linkPair,  String stem) {
		if (Constants.getSavedIdf().size() == 0) Constants.initIdfDB();
		
		this.linkPair = linkPair;
		this.stem = stem;
		
		this.valid = false;
		if (linkPair[1].length() >= stem.length()) {
	    	if (linkPair[1].substring(0, stem.length()).equals(stem)) {
	    		this.valid = true;
	    	}
    	}
		
		this.subSection = linkPair[1].indexOf("#") != -1;
	}
	
	public ArrayList<String> getTokens() {
		return tokens;
	}
	
	public ArrayList<String> getTitle() {
		return title;
	}
	
	public Page getSearchPage() {
		return searchPage;
	}
	
	public boolean getValid() {
		return valid;
	}
	
	public double getFinalScore() {
		return finalScore;
	}
	
	public double getProximityFactor() {
		return tokenProximityFactor;
	}
	
	public double getProximityFactorNoCurve() {
		return unCurvedProximity;
	}
	
	public double getWordCountNoCurve() {
		return unCurvedWordCount;
	}
	
	public double getWordCountPerc() {
		return tokenWordCountFactor;
	}
	
	public double getTfIdfScore() {
		return tfIdfScore;
	}
	
	public double getTitleMatch() {
		return titleMatchFactor;
	}
	
	public double getTitleSimilarity() {
		return titleSimilarity;
	}
	
	public double getContentSimilarity() {
		return contentSimilarity;
	}
	
	public double getSingleWordReliance() {
		return singleWordReliancePenalty;
	}
	
	public int getTokenAmount() {
		return occurences.size();
	}
	
	public int getTotalOccurences() {
		return totalOccurences;
	}
	
	public int getWordCount() {
		return wordCount;
	}
	
    public void calculateMatch() {
    	//tokenize the search phrase
    	if (valid) {
		    	double startTime = System.nanoTime();
	    		
	    		tokenPreprocessing();
				
				findOccurences();

//				if (occurences.size() != 0) medianFilter();
    			
	    		if (valid) {
					
	    			if (occurences.size() != 0) calcTokenDistance();
					
					calcScores();
					
//					if ((finalScore < Constants.threshold || occurences.size() == 1) && !linkPair[2].equals("")) {
//						double contextScore = getMatch(new String[] {linkPair[2], linkPair[1], ""}, stem);
//						
//						if (contextScore > finalScore) {
//							return contextScore;
//						}
//						else {
//							System.out.println();
//						}
//					}
					
					if (Constants.debugPrint) {
//						System.out.printf("| %-24s| %-24s| %-24s| %-24s| %-24s| %-24s|", tokenProximityFactor, tokenWordCountFactor, wordCount, titleMatchFactor, singleWordReliancePenalty, finalScore);
//						System.out.println();
					}
					
					double endTime = System.nanoTime();
					if (!Constants.debugPrint) {
						System.out.printf("%-72s| %-128s| %-12s| %-12s| %-22s", tokens, (stem + linkPair[1].substring(stem.length(), linkPair[1].length())), 
								((endTimeSearch-startTimeSearch)/Math.pow(10, 9)), ((endTime-startTime - (endTimeSearch-startTimeSearch))/Math.pow(10, 9)), finalScore);
					}
		    	}
		    	else {
//		    		if (!linkPair[2].equals("")) {
//			    		double contextScore = getMatch(new String[] {linkPair[2], linkPair[1], ""}, stem);
//		    			return contextScore;
//		    		}
		    		
					if (!Constants.debugPrint) System.out.printf("%-72s| %-128s| %-12s| %-12s| %-22s", linkPair[0], (stem + linkPair[1].substring(stem.length(), linkPair[1].length())), "N/A", "N/A", "0");
		    		this.valid = false;
		    	}
    	}
    }
    
    private void tokenPreprocessing() {
		//link stemming
	    String[] tokenized = linkPair[0].toLowerCase().split(Constants.stemmerDelims);
  	    ArrayList<String> stemsTokenized = new ArrayList<String>();
  		for (String stemToken : tokenized) stemsTokenized.add(stemToken);
  	    this.tokens = Constants.tokenize(stemsTokenized);

		//get lemmatized page contents of search URL
    	startTimeSearch = System.nanoTime();
		String searchUrl = linkPair[1].split("#")[0];
		
		//check saved dictionary by url
    	if (Constants.getSavedPages().containsKey(searchUrl)) {
    		this.searchPage = Constants.getSavedPages().get(searchUrl);
    	}
    	else {
    		this.searchPage = new Page(stem, linkPair[1].substring(stem.length(), linkPair[1].length()), true);
    		Constants.getSavedPages().put(searchUrl, searchPage);
    	}
    	endTimeSearch = System.nanoTime();
    	
    	if (subSection && !searchPage.getPermaLinks().containsKey(linkPair[1].split("#")[1])) subSection = false;
    	
    	if (searchPage.getSections().size() == 0) {
    		this.valid = false;
    		return;
    	}
		
		//set up title token list
		String stringTitle = this.searchPage.getTitle().toLowerCase();
		if (subSection) {
			String[] headingTokens = linkPair[1].split("#")[1].split("-");
			
			for (int i = 0; i < headingTokens.length; i++) {
				try {
					Integer.parseInt(headingTokens[i]);
					headingTokens[i] = "";
				}
				catch (Exception e) {}
			}

			ArrayList<String> section;
			if (searchPage.getPermaLinks().containsKey(linkPair[1].split("#")[1])) {
				section = this.searchPage.getPermaLinkSection(linkPair[1].split("#")[1]);
				stringTitle = "";
				
//				if (Constants.debugPrint) System.out.println(section);
				
				for (int i = 0; i < headingTokens.length; i++) {
					if (i == section.size()-1) {
						break;
					}
					
					if (section.get(i).contains(".")) {
						headingTokens[i] = section.get(i);
					}
				}
			}
			
			stringTitle = String.join(" ", headingTokens);
		}
		
		//title stems
		tokenized = stringTitle.toLowerCase().split(Constants.stemmerDelims);
	    stemsTokenized = new ArrayList<String>();
		for (String stemToken : tokenized) stemsTokenized.add(stemToken);
	    this.title = Constants.tokenize(stemsTokenized);
	    
	    for (int i = 0; i < tokens.size(); i++) {
	    	if (tokens.indexOf(tokens.get(i)) != i) {
	    		tokens.remove(tokens.get(i));
	    		i--;
	    	}
	    }
	    
	    for (int i = 0; i < title.size(); i++) {
	    	if (title.indexOf(title.get(i)) != i) {
	    		title.remove(title.get(i));
	    		i--;
	    	}
	    }
	    
	    if (!subSection || !searchPage.getPermaLinks().containsKey(linkPair[1].split("#")[1])) topTokens = this.searchPage.getTopStemsSorted(this.tokens.size()*3);
	    else topTokens = this.searchPage.getTopStemsSortedSections(this.tokens.size()*3, linkPair[1].split("#")[1]);
	    
	    if (!subSection || !searchPage.getPermaLinks().containsKey(linkPair[1].split("#")[1])) topTokensWithValues = this.searchPage.getTopStems();
	    else topTokensWithValues = this.searchPage.getTopStemsSections(linkPair[1].split("#")[1]);
	    //this.searchPage.getTopStemsSortedSections(this.tokens.size()*3, linkPair[1].split("#")[1]);
	    
//	    if (Constants.debugPrint) {
//	    	System.out.println(linkPair[0]);
//	    	System.out.println(tokens);
//	    	System.out.println(stringTitle);
//	    	System.out.println(title);
//	    }
	}
    
    private void findOccurences() {
    	//Initialize ArrayList
		for (int i = 0; i < tokens.size(); i++) {
			this.occurences.add(new ArrayList<Integer>());
		}
		
		//find occurrences
		this.wordCount = 0;
		if (subSection) {
			ArrayList<String> section = new ArrayList<String>();
			if (searchPage.getPermaLinks().containsKey(linkPair[1].split("#")[1])) section = this.searchPage.getPermaLinkSection(linkPair[1].split("#")[1]);
			else {
//				if (!Constants.debugPrint) System.out.printf("%-64s| %-128s| %-12s| %-12s", linkPair[0], (stem + linkPair[1].substring(stem.length(), linkPair[1].length())), 
//						((endTimeSearch-startTimeSearch)/Math.pow(10, 9)), ((System.nanoTime()-startTime - (endTimeSearch-startTimeSearch))/Math.pow(10, 9)));
				subSection = false;
			}
			
			if (subSection) {
				for (String word : section) {
		    		for (int tokenId = 0; tokenId < tokens.size(); tokenId++) {
		    			if (tokens.get(tokenId).equals(word)) {
		    				this.occurences.get(tokenId).add(wordCount);
		    			}
		    		}
		    		this.wordCount++;
				}
			}
		}
		if (!subSection) {
			for (ArrayList<String> section : searchPage.getStems()) {
				for (String word : section) {
		    		for (int tokenId = 0; tokenId < tokens.size(); tokenId++) {
		    			if (tokens.get(tokenId).equals(word)) {
		    				this.occurences.get(tokenId).add(wordCount);
		    			}
		    		}
		    		this.wordCount++;
				}
			}
		}
		
		//remove zero tokens unless hyper-linked
    	for (int i = 0; i < occurences.size(); i++) {
    		if (occurences.get(i).size() == 0) {
    			this.occurences.remove(i);
	    		i--;
    		}
    	}
    }
    
    private void medianFilter() {
    	//get median after zero removal
    	int[] sizes = new int[occurences.size()];
    	for (int i = 0; i < sizes.length; i++) {
    		sizes[i] = this.occurences.get(i).size();
    	}
    	
    	for (int i = 0; i < sizes.length; i++) {
    		for (int j = i+1; j < sizes.length; j++) {
    			if (sizes[i] > sizes[j]) {
    				int temp = sizes[i];
    				sizes[i] = sizes[j];
    				sizes[j] = temp;
    			}
    		}
    	}
    	
    	double median = 0;
    	if (sizes.length % 2 == 0) {
    		median = ((double)sizes[sizes.length/2] + sizes[sizes.length/2 - 1])/2;
    	}
    	else {
    		median = sizes[(sizes.length-1)/2];
    	}
    	
    	//remove tokens under median*0.2
    	for (int i = 0; i < occurences.size(); i++) {
    		if (occurences.get(i).size() < median*0.2) {
    			this.occurences.remove(i);
    			tokens.remove(i);
	    		i--;
    		}
    	}
    }
    
    private void calcTokenDistance() {
    	//iterate, find and weigh distances between words
    	this.disScore = 0.0;
		this.disCount = 0;
    	
		int[] indexes = new int[occurences.size()];
		int maxLength = occurences.get(0).size();
		for (int token = 0; token < occurences.size(); token++) {
			if (occurences.get(token).size() > maxLength) maxLength = occurences.get(token).size();
		}
		
		if (indexes.length > 1) {
			boolean indexIncreasing = true;
			while (indexIncreasing) {
				for (int ind1 = 0; ind1 < indexes.length; ind1++) {
					for (int ind2 = ind1+1; ind2 < indexes.length; ind2++) {
						int ind1Index = occurences.get(ind1).get(indexes[ind1]);
						int ind2Index = occurences.get(ind2).get(indexes[ind2]);
						
						this.disScore += (Constants.maxTokenSeperation/Math.abs((double)ind1Index - ind2Index))/Constants.maxTokenSeperation;
						this.disCount++;
		    		}
				}
				
				double[] averageAdd = new double[indexes.length];
				boolean canAdd = false;
				for (int ind1 = 0; ind1 < indexes.length; ind1++) {
		    		if (occurences.get(ind1).size()-1 > indexes[ind1]) {
		    			int count = 0;
		
		    			for (int ind2 = 0; ind2 < indexes.length; ind2++) {
		    				if (ind1 != ind2) {
			        			int ind1IndexAdd = occurences.get(ind1).get(indexes[ind1]+1);
			    				int ind2Index = occurences.get(ind2).get(indexes[ind2]);
			        			
			    				averageAdd[ind1] += (Constants.maxTokenSeperation/Math.abs((double)ind1IndexAdd - ind2Index))/Constants.maxTokenSeperation;
			    				count++;
		    				}
		    			}
		    			
		    			if (count != 0) {
		    				averageAdd[ind1] /= count;
		    			}
		    			
		    			canAdd = true;
		    		}
		    	}
				
				if (canAdd) {
		    		double mostAdd = 0;
		    		int mostAddIndex = 0;
		    		for (int av = 0; av < averageAdd.length; av++) {
		    			if (mostAdd < averageAdd[av]) {
		    				mostAdd = averageAdd[av];
		    				mostAddIndex = av;
		    			}
		    		}
				
		    		indexes[mostAddIndex]++;
				}
				else {
					indexIncreasing = false;
				}
			}
		}
		else {
			this.disCount = occurences.get(0).size();
			this.disScore = 0.0;
		}
    }
    
    private void calcScores() {
    	if (occurences.size() != 0) {
	    	//calculate title ranking
			int titleMatch = 0;
			for (String titleToken : title) {
				for (String token : tokens) {
					if (token.equals(titleToken)) {
						titleMatch += 1;
						break;
					}
				}
			}
			
			//calculate ranking
			totalOccurences = 0;
			int secondMaxOccurence = occurences.get(0).size();
			int maxOccurence = occurences.get(0).size();
			for (ArrayList<Integer> occurence : occurences) {
				int size = occurence.size();
				
				if (size > maxOccurence) maxOccurence = size;
				else if (size > secondMaxOccurence) secondMaxOccurence = size;
				
				totalOccurences += size;
			}
	
			//factors impacting score
			//double tokenQuantityFactor = (((double)Constants.maxTokenQuantity-1)/-indexes.length + Constants.maxTokenQuantity)/Constants.maxTokenQuantity;
			unCurvedProximity = (double)this.disScore/this.disCount;
			tokenProximityFactor = unCurvedProximity;
			double wordCountFactor = (-Constants.wordCountMultiplier/((double)wordCount+Constants.wordCountMultiplier)+1);
			unCurvedWordCount =  (double)totalOccurences/wordCount;
			tokenWordCountFactor = unCurvedWordCount*wordCountFactor;
			titleMatchFactor = (double)titleMatch/title.size();
			singleWordReliancePenalty = Constants.occurenceDifferencePenalty*(totalOccurences/(((double)maxOccurence-secondMaxOccurence)+Constants.occurenceDifferencePenalty))/totalOccurences;
			
			//balancing of token proximity weight
			tokenProximityFactor *= 1-(1/(double)occurences.size());
			unCurvedWordCount *= 1/(double)occurences.size();
			tokenWordCountFactor *= 1/(double)occurences.size();
			
			tfIdfScore = 0;
			int count = 0;
			for (String token : tokens) {
				if (topTokensWithValues.containsKey(token)) {
					tfIdfScore += topTokensWithValues.get(token);
				}
				count ++;
			}
			tfIdfScore /= (double)count;
			
			//Logistic Regression: 	proximity, token word count, token amount, titleMatch
			//SVM: 					word count, each token count, each token's proximity, 
			
			//Deeper NN?: occurence indexes per token, title match, word count
			singleWordReliancePenalty = 1;
			finalScore = ((tokenProximityFactor + tokenWordCountFactor)*(1-Constants.titleFactorWeight) + titleMatchFactor*Constants.titleFactorWeight) * singleWordReliancePenalty * 100;
    	}
		
		if (Double.isNaN(tokenProximityFactor)) tokenProximityFactor = 0;
		if (Double.isNaN(tokenWordCountFactor)) tokenWordCountFactor = 0;
		if (Double.isNaN(tfIdfScore)) tfIdfScore = 0;
		if (Double.isNaN(titleMatchFactor)) titleMatchFactor = 0;
		if (Double.isNaN(finalScore)) finalScore = 0;
		
		calcWord2Vec();
    }
    
    //calculate cosine similarity from vectors
    private void calcWord2Vec() {
    	WordVectors model = Constants.word2vecModel;
		long dimensions = model.getWordVectorMatrix("gitlab").length();
		
		INDArray searchVector = addVector(dimensions, model, this.tokens);
		INDArray headlineVector = addVector(dimensions, model, this.title);
		INDArray contentsVector = addVector(dimensions, model, this.topTokens);
		
		titleSimilarity = Transforms.cosineSim(searchVector, headlineVector);
		contentSimilarity = Transforms.cosineSim(searchVector, contentsVector);
		
		if (titleSimilarity > 1.0) titleSimilarity = 1.0;
		if (contentSimilarity > 1.0) contentSimilarity = 1.0;
		
		//calculate body similarity
    }
    
    private INDArray addVector(long dimensions, WordVectors model, ArrayList<String> tokens) {
    	INDArray searchVector = Nd4j.create(dimensions);
		
		for (int i = 0; i < dimensions; i++) {
			int skipped = 0;
			
			for (String token : tokens) {
				if (model.getWordVector(token) == null) {
					skipped++;
					continue;
				}
				
				searchVector.putScalar(i, searchVector.getDouble(i) + model.getWordVector(token)[i]);
			}
			searchVector.putScalar(i, searchVector.getDouble(i)/(tokens.size()-skipped));
		}
		
		return searchVector;
    }
}
