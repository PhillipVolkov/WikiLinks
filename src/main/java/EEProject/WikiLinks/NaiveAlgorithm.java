package EEProject.WikiLinks;

import java.util.ArrayList;
import org.tartarus.snowball.ext.PorterStemmer;


public class NaiveAlgorithm {
    public static double getMatch(String[] linkPair, String stem) {
    	ArrayList<String> tokens = new ArrayList<String>();
    	
    	//tokenize the search phrase
    	if (linkPair[1].length() >= stem.length()) {
	    	if (linkPair[1].substring(0, stem.length()).equals(stem)) {
		    	double startTime = System.nanoTime();
	    		final boolean subSection = linkPair[1].indexOf("#") != -1;
	    		
	    		//link stemming
			    String[] tokenized = linkPair[0].toLowerCase().split(Constants.stemmerDelims);
    		    PorterStemmer stemmer = new PorterStemmer();
    		    
    		    for (String token : tokenized) {
    		    	token = token.toLowerCase().strip();
    		    	token = String.join("", token.split("\n"));
    		    	
    		    	stemmer.setCurrent(token);
    		    	stemmer.stem();
    		    	token = stemmer.getCurrent();
    		    	
    		    	if (!Constants.contains(Constants.stopWords, token) && !tokens.contains(token)) {
    		    		tokens.add(token);
    		    	}
    		    }

				//get lemmatized page contents of search URL
		    	double startTimeSearch = System.nanoTime();
				Page searchPage;
				String searchUrl = linkPair[1].split("#")[0];
				
				//check saved dictionary by url
		    	if (Constants.getSavedPages().containsKey(searchUrl)) {
		    		searchPage = Constants.getSavedPages().get(searchUrl);
		    	}
		    	else {
		    		searchPage = new Page(stem, linkPair[1].substring(stem.length(), linkPair[1].length()));
		    		Constants.getSavedPages().put(searchUrl, searchPage);
		    	}
		    	double endTimeSearch = System.nanoTime();
				
				//set up title token list
	    		String stringTitle = searchPage.getTitle().toLowerCase();
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
	    				section = searchPage.getPermaLinkSection(linkPair[1].split("#")[1]);
	    				stringTitle = "";
	    				
	    				if (Constants.debugPrint) System.out.println(section);
	    				
	    				for (int i = 0; i < headingTokens.length; i++) {
	    					if (section.get(i).contains(".")) {
	    						headingTokens[i] = section.get(i);
	    					}
	    				}
	    			}
	    			
	    			stringTitle = String.join(" ", headingTokens);
	    		}
	    		
	    		//title stems
	    		ArrayList<String> title = new ArrayList<String>();
	    		tokenized = stringTitle.toLowerCase().split(Constants.stemmerDelims);
    		    stemmer = new PorterStemmer();
    		    
    		    for (String token : tokenized) {
    		    	token = token.toLowerCase().strip();
    		    	token = String.join("", token.split("\n"));
    		    	
    		    	stemmer.setCurrent(token);
    		    	stemmer.stem();
    		    	token = stemmer.getCurrent();
    		    	
    		    	if (!Constants.contains(Constants.stopWords, token)) {
    		    		title.add(token);
    		    	}
    		    }
    		    
    		    
			    if (Constants.debugPrint) {
			    	System.out.println(linkPair[0]);
			    	System.out.println(tokens);
			    	System.out.println(stringTitle);
			    	System.out.println(title);
			    }
	    				
				//Initialize ArrayList
				ArrayList<ArrayList<Integer>> occurences = new ArrayList<ArrayList<Integer>>();
				for (int i = 0; i < tokens.size(); i++) {
					occurences.add(new ArrayList<Integer>());
				}
				
				//find occurrences
				int wordCount = 0;
				if (subSection) {
					ArrayList<String> section;
					if (searchPage.getPermaLinks().containsKey(linkPair[1].split("#")[1])) section = searchPage.getPermaLinkSection(linkPair[1].split("#")[1]);
					else {
						if (!Constants.debugPrint) System.out.printf("%-64s| %-128s| %-12s| %-12s", linkPair[0], (stem + linkPair[1].substring(stem.length(), linkPair[1].length())), 
								((endTimeSearch-startTimeSearch)/Math.pow(10, 9)), ((System.nanoTime()-startTime - (endTimeSearch-startTimeSearch))/Math.pow(10, 9)));
						return 0;
					}
					
					for (String word : section) {
			    		for (int tokenId = 0; tokenId < tokens.size(); tokenId++) {
			    			if (tokens.get(tokenId).equals(word)) {
								occurences.get(tokenId).add(wordCount);
			    			}
			    		}
			    		wordCount++;
					}
				}
				else {
					for (ArrayList<String> section : searchPage.getStems()) {
						for (String word : section) {
				    		for (int tokenId = 0; tokenId < tokens.size(); tokenId++) {
				    			if (tokens.get(tokenId).equals(word)) {
									occurences.get(tokenId).add(wordCount);
				    			}
				    		}
				    		wordCount++;
						}
					}
				}
				
				//remove zero tokens unless hyper-linked
		    	for (int i = 0; i < occurences.size(); i++) {
		    		if (occurences.get(i).size() == 0) {
		    			occurences.remove(i);
		    			tokens.remove(i);
			    		i--;
		    		}
		    	}
		    	
				double score = 0.0;
				int scoreCount = 0;
	    		if (occurences.size() != 0) {
			    	//get median after zero removal
			    	int[] sizes = new int[occurences.size()];
			    	for (int i = 0; i < sizes.length; i++) {
			    		sizes[i] = occurences.get(i).size();
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
			    			occurences.remove(i);
			    			tokens.remove(i);
				    		i--;
			    		}
			    	}
		    	
					//iterate, find and weigh distances between words
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
									
									score += (Constants.maxTokenSeperation/Math.abs((double)ind1Index - ind2Index))/Constants.maxTokenSeperation;
									scoreCount++;
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
						scoreCount = occurences.get(0).size();
						score = (double)scoreCount;
					}
					
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
					int totalOccurences = 0;
					int secondMaxOccurence = occurences.get(0).size();
					int maxOccurence = occurences.get(0).size();
					for (ArrayList<Integer> occurence : occurences) {
						int size = occurence.size();
						
						if (size > maxOccurence) maxOccurence = size;
						else if (size > secondMaxOccurence) secondMaxOccurence = size;
						
						totalOccurences += size;
					}
	
					//factors impacting score
					double tokenQuantityFactor = (((double)Constants.maxTokenQuantity-1)/-indexes.length + Constants.maxTokenQuantity)/Constants.maxTokenQuantity;
					double tokenProximityFactor = (double)score/scoreCount;
					double wordCountFactor = (-Constants.wordCountMultiplier/((double)wordCount+Constants.wordCountMultiplier)+1);
					double tokenWordCountFactor = (double)totalOccurences/wordCount*wordCountFactor;
					double titleMatchFactor = (double)titleMatch/title.size();
					double singleWordReliancePenalty = Constants.occurenceDifferencePenalty*(totalOccurences/(((double)maxOccurence-secondMaxOccurence)+Constants.occurenceDifferencePenalty))/totalOccurences;
					
					//balancing of token proximity weight
					tokenProximityFactor *= 1-(1/(double)indexes.length);
					tokenWordCountFactor *= 1/(double)indexes.length;
					
					double finalScore = ((tokenProximityFactor + tokenWordCountFactor)*(1-Constants.titleFactorWeight) + titleMatchFactor*Constants.titleFactorWeight) * singleWordReliancePenalty * 100;
					
					if ((finalScore < Constants.threshold || indexes.length == 1) && !linkPair[2].equals("")) {
						double contextScore = getMatch(new String[] {linkPair[2], linkPair[1], ""}, stem);
						
						if (contextScore > finalScore) {
							return contextScore;
						}
						else {
							System.out.println();
						}
					}
					
					if (Constants.debugPrint) {
						System.out.printf("| %-24s| %-24s| %-24s| %-24s| %-24s| %-24s|", tokenProximityFactor, tokenWordCountFactor, wordCount, titleMatchFactor, singleWordReliancePenalty, finalScore);
						System.out.println();
					}
					
					double endTime = System.nanoTime();
					if (!Constants.debugPrint) {
						System.out.printf("%-72s| %-128s| %-12s| %-12s| %-22s", tokens, (stem + linkPair[1].substring(stem.length(), linkPair[1].length())), 
								((endTimeSearch-startTimeSearch)/Math.pow(10, 9)), ((endTime-startTime - (endTimeSearch-startTimeSearch))/Math.pow(10, 9)), finalScore);
					}
						
					return finalScore;
		    	}
		    	else {
		    		if (!linkPair[2].equals("")) {
			    		double contextScore = getMatch(new String[] {linkPair[2], linkPair[1], ""}, stem);
		    			return contextScore;
		    		}
		    		
					if (!Constants.debugPrint) System.out.printf("%-72s| %-128s| %-12s| %-12s| %-22s", linkPair[0], (stem + linkPair[1].substring(stem.length(), linkPair[1].length())), "N/A", "N/A", "0");
		    		return 0;
		    	}
	    	}
    	}
    	
    	return -1;
    }
}
