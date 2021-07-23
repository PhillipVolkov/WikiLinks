package EEProject.WikiLinks;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;


public class NaiveAlgorithm {
	static final String[] ignoreTokens = new String[] {"li", "ul", "div", "button"};
	static final int largestLength = "button".length();
	
	static final String[] stopWords = new String[] {"a", "about", "above", "after", "again", "against", 
			"all", "am", "an", "and", "any", "are", "aren't", "as", "at", "be", "because", "been", "before", 
			"being", "below", "between", "both", "but", "by", "can't", "cannot", "could", "couldn't", "did", 
			"didn't", "do", "does", "doesn't", "doing", "don't", "down", "during", "each", "few", "for", "from", 
			"further", "had", "hadn't", "has", "hasn't", "have", "haven't", "having", "he", "he'd", "he'll", "he's", 
			"her", "here", "here's", "hers", "herself", "him", "himself", "his", "how", "how's", "i", "i'd", "i'll", 
			"i'm", "i've", "if", "in", "into", "is", "isn't", "it", "it's", "its", "itself", "let's", "me", "more", 
			"most", "mustn't", "my", "myself", "no", "nor", "not", "of", "off", "on", "once", "only", "or", "other", 
			"ought", "our", "ours ourselves", "out", "over", "own", "same", "shan't", "she", "she'd", "she'll", 
			"she's", "should", "shouldn't", "so", "some", "such", "than", "that", "that's", "the", "their", "theirs", 
			"them", "themselves", "then", "there", "there's", "these", "they", "they'd", "they'll", "they're", "they've", 
			"this", "those", "through", "to", "too", "under", "until", "up", "very", "was", "wasn't", "we", "we'd", 
			"we'll", "we're", "we've", "were", "weren't", "what", "what's", "when", "when's", "where", "where's", "which", 
			"while", "who", "who's", "whom", "why", "why's", "with", "won't", "would", "wouldn't", "you", "you'd", "you'll", 
			"you're", "you've", "your", "yours", "yourself", "yourselves", ",", ".", "!", "?", "(", ")", "+", "-", "_", "/", ":",
			"#", "’s", "'s", "<", ">", "--"};
	
	static final boolean testLinks = true;
	static final boolean debugPrint = true;
	static final double threshold = 5f;
	static final int maxTokenSeperation = 20;
	static final int maxTokenQuantity = 3;
	static final double titleFactorWeight = 0.3;
	
    public static void main(String[] args) {
    	String stem = "https://docs.gitlab.com";
    	
    	if (!testLinks) {
	        Page newPage = readGitLabPage(stem, "/ee/user/project/clusters/index.html");
	        
	        
	        for (String[] linkPair : newPage.getLinks()) {
	        	double match = getMatch(linkPair, stem, debugPrint);
	        	if (match != -1) {
	            	System.out.printf("| %-22s",match);
	            	
	            	if (match >= threshold) System.out.printf("| true\n");
	            	else System.out.printf("| false\n");
	        	}
	        }
    	}
    	else {
	    	ArrayList<String[]> testLinks = new ArrayList<String[]>();
	    	testLinks.add(new String[] {"Auto DevOps", "https://docs.gitlab.com/ee/user/project/clusters/#auto-devops"});
	    	testLinks.add(new String[] {"the Prometheus cluster integration is enabled", "https://docs.gitlab.com/ee/user/clusters/integrations.html#prometheus-cluster-integration"});
	    	testLinks.add(new String[] {"Cluster management project", "https://docs.gitlab.com/ee/user/clusters/management_project.html"});
	    	testLinks.add(new String[] {"CI/CD Pipelines", "https://docs.gitlab.com/ee/ci/pipelines/index.html"});
	    	testLinks.add(new String[] {"cluster integrations", "https://docs.gitlab.com/ee/user/clusters/integrations.html"});
	    	testLinks.add(new String[] {"GitLab to manage your cluster for you", "https://docs.gitlab.com/ee/user/project/clusters/gitlab_managed_clusters.html"});
	    	testLinks.add(new String[] {"Infrastructure as Code", "https://docs.gitlab.com/ee/user/infrastructure"});
	    	testLinks.add(new String[] {"Deploy Boards", "https://docs.gitlab.com/ee/user/project/deploy_boards.html"});
	    	testLinks.add(new String[] {"role-based or attribute-based access controls", "https://docs.gitlab.com/ee/user/project/clusters/cluster_access.html"});
	    	testLinks.add(new String[] {"Read more about Kubernetes monitoring", "https://docs.gitlab.com/ee/user/project/integrations/prometheus_library/kubernetes.html"});

	    	testLinks.add(new String[] {"Kubernetes Pipeline", "https://docs.gitlab.com/ee/user/clusters/management_project.html"});

	    	testLinks.add(new String[] {"environment", "https://docs.gitlab.com/ee/ci/environments/index.html"});
	    	testLinks.add(new String[] {"NGINX Ingress", "https://docs.gitlab.com/ee/user/project/integrations/prometheus_library/nginx.html"});
	    	testLinks.add(new String[] {"instance", "https://docs.gitlab.com/ee/user/instance/clusters/index.html"});
	    	testLinks.add(new String[] {"Kubernetes podlogs", "https://docs.gitlab.com/ee/user/project/clusters/kubernetes_pod_logs.html"});
	    	testLinks.add(new String[] {"group", "https://docs.gitlab.com/ee/user/group/clusters/index.html"});
	    	testLinks.add(new String[] {"Kubernetes with Knative", "https://docs.gitlab.com/ee/user/project/clusters/serverless/index.html"});
	    	
	    	testLinks.add(new String[] {"developer", "https://docs.gitlab.com/ee/user/permissions.html"});
	    	
	    	double startTime = System.nanoTime();
	    	for (String[] linkPair : testLinks) {
	        	double match = getMatch(linkPair, stem, debugPrint);
	        	
	        	if (match != -1 && debugPrint) {
	            	System.out.printf("| %-22s",match);
	            	
	            	if (match >= threshold) System.out.printf("| true\n");
	            	else System.out.printf("| false\n");
	        	}
	        }
	    	double endTime = System.nanoTime();
	    	
	    	System.out.println("\nTime Elapsed: " + ((endTime-startTime)/Math.pow(10, 9)) + " seconds");
    	}
    }
    
    //TODO word-count curve, OPTIMIZE
    public static double getMatch(String[] linkPair, String stem, boolean print) {
    	ArrayList<String> tokens = new ArrayList<String>();
    	
    	//tokenize the search phrase
    	if (linkPair[1].substring(0, stem.length()).equals(stem)) {
    		boolean subSection = linkPair[1].indexOf("#") != -1;
    		
    		String[] tokenSplit = linkPair[0].split(" ");
    		for (int i = 0; i < tokenSplit.length; i++) {
    			if (contains(stopWords, tokenSplit[i])) {
    				tokenSplit[i] = "";
		    	}
    		}
    		
			Properties props = new Properties();
		    props.setProperty("annotators", "tokenize,ssplit,pos,lemma");
		    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		    CoreDocument document = pipeline.processToCoreDocument(String.join(" ", tokenSplit));
		    
		    for (CoreLabel token : document.tokens()) {
	    		String currToken = token.lemma().toLowerCase();
	    		
	    		if (!tokens.contains(currToken)) tokens.add(currToken);
		    }
			
			//get lemmatized page contents of search URL
			Page searchPage = readGitLabPage(stem, linkPair[1].substring(stem.length(), linkPair[1].length()));
			
			//set up title token list
    		String stringTitle = searchPage.getTitle();
    		if (subSection) {
    			stringTitle = String.join(" ", linkPair[1].split("#")[1].split("-"));
    		}
    		
    		ArrayList<String> title = new ArrayList<String>();
    		document = pipeline.processToCoreDocument(stringTitle);
		    for (CoreLabel token : document.tokens()) {
	    		String currToken = token.lemma().toLowerCase();
		    	if (!contains(stopWords, currToken)) {
		    		title.add(currToken);
		    	}
		    }
    				
			//Initialize ArrayList
			ArrayList<ArrayList<Integer>> occurences = new ArrayList<ArrayList<Integer>>();
			for (int i = 0; i < tokens.size(); i++) {
				occurences.add(new ArrayList<Integer>());
			}
			
			//find occurrences
			int wordCount = 0;
			for (ArrayList<String> section : searchPage.getLemma()) {
				boolean sectionMatches = true;
				
				if (subSection) {
					for (int i = 0; i < title.size(); i++) {
						if (section.size()-1 <= i) {
							sectionMatches = false;
							break;
						}
						
						if (!title.get(i).equals(section.get(i))) {
							sectionMatches = false;
							break;
						}
					}
				}
				
				if (sectionMatches) {
					for (String word : section) {
			    		for (int tokenId = 0; tokenId < tokens.size(); tokenId++) {
			    			if (tokens.get(tokenId).equals(word)) {
								occurences.get(tokenId).add(wordCount);
			    			}
			    		}
			    		wordCount++;
					}
					
					if (subSection) break;
				}
			}
			
			//remove zero tokens
	    	for (int i = 0; i < occurences.size(); i++) {
//	    		System.out.print(occurences.get(i).size() + "\t");
	    		
	    		if (occurences.get(i).size() == 0) {
	    			occurences.remove(i);
	    			tokens.remove(i);
		    		i--;
	    		}
	    	}
	    	
//    		System.out.print("\t|");
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
				
		    	if (print) System.out.printf("%-44s| %-128s", tokens, (stem + linkPair[1].substring(stem.length(), linkPair[1].length())));
	    	
				//iterate, find and weigh distances between words
				int[] indexes = new int[occurences.size()];
				int maxLength = occurences.get(0).size();
				for (int token = 0; token < occurences.size(); token++) {
					if (occurences.get(token).size() > maxLength) maxLength = occurences.get(token).size();
				}
				
				if (indexes.length > 1) {
					boolean indexIncreasing = true;
					while (indexIncreasing) {
						for (int i = 0; i < indexes.length; i++) {
//							System.out.print(indexes[i] + ": " + occurences.get(i).get(indexes[i]) + "\t\t");
						}
						
						for (int ind1 = 0; ind1 < indexes.length; ind1++) {
							for (int ind2 = ind1+1; ind2 < indexes.length; ind2++) {
								int ind1Index = occurences.get(ind1).get(indexes[ind1]);
								int ind2Index = occurences.get(ind2).get(indexes[ind2]);
								
								score += (maxTokenSeperation/Math.abs((double)ind1Index - ind2Index))/maxTokenSeperation;
								scoreCount++;
//								System.out.print("\t\t" + (maxWordSeperation/Math.abs((double)ind1Index - ind2Index))/maxWordSeperation);
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
					        			
					    				averageAdd[ind1] += (maxTokenSeperation/Math.abs((double)ind1IndexAdd - ind2Index))/maxTokenSeperation;
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
//				    	System.out.println();
					}
				}
				else {
					scoreCount = occurences.get(0).size();
					score = (double)scoreCount;
				}
				
				//calculate title ranking
				int titleMatch = 0;
				for (String token : tokens) {
					for (String titleToken : title) {
						if (titleToken.equals(token)) {
							titleMatch += 1;
							break;
						}
					}
				}
				
				//calculate ranking
				int totalOccurences = 0;
				for (ArrayList<Integer> occurence : occurences) {
					totalOccurences += occurence.size();
				}

				//factors impacting score
				double tokenQuantityFactor = (((double)maxTokenQuantity-1)/-indexes.length + maxTokenQuantity)/maxTokenQuantity;
				double tokenProximityFactor = (double)score/scoreCount;
				double tokenWordCountFactor = (double)totalOccurences/wordCount;
				double titleMatchFactor = (double)titleMatch/title.size();
				
				//balancing of token proximity weight
				tokenProximityFactor *= 1-(1/(double)indexes.length);
				tokenWordCountFactor *= 1/(double)indexes.length;
				
				double finalScore = ((tokenProximityFactor + tokenWordCountFactor)*(1-titleFactorWeight) + titleMatchFactor*titleFactorWeight) * 100;

//				System.out.println(tokenProximityFactor + "\t" + tokenWordCountFactor + "\t" + titleMatchFactor + "\t" + finalScore);
				
				return finalScore;
	    	}
	    	else {
	    		if (print) System.out.printf("%-44s| %-128s", tokens, (stem + linkPair[1].substring(stem.length(), linkPair[1].length())));
	    		
	    		return 0;
	    	}
    	}

		if (print) System.out.printf("%-44s| %-128s", tokens, (stem + linkPair[1].substring(stem.length(), linkPair[1].length())));
    	
    	return -1;
    }
    
    //parse the page into the object
    public static Page readGitLabPage(String stem, String pages) {
    	try {
            URL url = new URL(stem + pages);

            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            
            String title = "";
            ArrayList<String> sections = new ArrayList<String>();
            ArrayList<ArrayList<String>> lemma = new ArrayList<ArrayList<String>>();
            ArrayList<String[]> links = new ArrayList<String[]>();
            
            String currLine = "";
            String output = "";
            boolean mainOpen = false;
            boolean articleContent = false;
            int divsOpen = 0;

    		boolean addSection = false;
            while ((currLine = reader.readLine()) != null) {
            	currLine = currLine.toString();
            	
            	//closers
            	if (currLine.equals("</main>")) {
            		mainOpen = false;
            	}
            	else if (articleContent) {
            		divsOpen -= countMatches(currLine, "</div");
            		
            		if (divsOpen == 0) {
            			articleContent = false;
            		}
            	}
            	
            	//parsing
            	if (mainOpen && articleContent) {
            		boolean singleBracketOpen = false;
            		int bracketPrevOpen = -2;
            		int bracketPrevClosed = -2;
            		int prevHeading = -2;
            		boolean linkBracketOpen = false;
            		boolean linkUrlParse = false;
            		boolean endBracketOpen = false;
            		boolean ignoreCurrChar = false;
            		boolean ignoreBracketContents = false;
            		boolean lineTitle = false;
            		boolean escapeSequence = false;
            		String newLine = "";
            		String internalText = "";
            		String linkURL = "";
            		String linkText = "";
            		String escapeCharacters = "";
            		
            		for (int i = 0; i < currLine.length(); i++) {
            			//detect parses
            			if (currLine.charAt(i) == '<') {
            				singleBracketOpen = true;
            				bracketPrevOpen = i;
            				ignoreCurrChar = true;
            			}
            			else if (currLine.charAt(i) == '/' && bracketPrevOpen == i-1) {
            				singleBracketOpen = false;
            				bracketPrevClosed = i;
            				endBracketOpen = true;
            				ignoreCurrChar = true;
            			}
            			else if (currLine.charAt(i) == 'h' && (bracketPrevOpen == i-1 || bracketPrevClosed == i-1)) {
            				prevHeading = i;
            			}
            			else if (prevHeading == i-1 && bracketPrevOpen == i-2) {
            				if (currLine.charAt(i) == '1') {
            					lineTitle = true;
            				}
            				
            				if (addSection) {
            					sections.add(output);
            					addSection = false;
            				}

            				output = "";
        					addSection = true;
            			}
            			else if (currLine.charAt(i) == 'a' && bracketPrevOpen == i-1) {
            				singleBracketOpen = false;
            				linkBracketOpen = true;
            				linkUrlParse = true;
            				ignoreCurrChar = true;
            			}
            			else if (currLine.charAt(i) == 'a' && bracketPrevClosed == i-1) {
            				linkBracketOpen = false;
            			}
            			else if (currLine.charAt(i) == '>') {
            				if (singleBracketOpen) singleBracketOpen = false;
            				else if (linkUrlParse) linkUrlParse = false;
            				else if (endBracketOpen) endBracketOpen = false;
            				
        					ignoreCurrChar = true;
            			}
            			else if (currLine.charAt(i) == '&') {
            				escapeSequence = true;
            				escapeCharacters = "";
            			}
            			else if (currLine.charAt(i) == ';' && escapeSequence) {
            				escapeSequence = false;
            				ignoreCurrChar = true;
            			}
            			
            			//System.out.print(currLine.charAt(i) + " " + singleBracketOpen);
            			
            			//write currentChar
            			if (!endBracketOpen && !ignoreCurrChar) {
            				if (escapeSequence) escapeCharacters += currLine.charAt(i);
            				
            				else if (singleBracketOpen) internalText += currLine.charAt(i);
            				
            				else if (linkUrlParse) linkURL += currLine.charAt(i);
            				
            				else if (linkBracketOpen) linkText += currLine.charAt(i);
            				
            				else newLine += currLine.charAt(i);
            			}
            			
            			//handle escape sequences
            			if (!escapeCharacters.equals("") && !escapeSequence) {
        					if (escapeCharacters.equals("&lt")) newLine += "<";
        					else if (escapeCharacters.equals("&gt")) newLine += ">";
        					else newLine += escapeCharacters + ";";
        					
        					escapeCharacters = "";
            			}
            			
            			//handle internal text
            			if (!internalText.equals("")) {
            				if (internalText.length() <= largestLength) {
            					if (contains(ignoreTokens, internalText)) {
            						ignoreBracketContents = true;
            					}
            				}
            				
            				if (!singleBracketOpen) {
	            				//if (!contains(ignoreTokens, internalText) && !ignoreBracketContents) newLine += "<" + internalText + ">";
	    						internalText = "";
	    						ignoreBracketContents = false;
            				}
    					}
            			
            			//handle link text
            			if (!linkURL.equals("")) {
            				if (!linkBracketOpen) {
            					Scanner scan = new Scanner(linkURL);
            					boolean ignore = false;
            					String link = "";
            					
            					//find link
            					while (scan.hasNext()) {
            						String currStr = scan.next();
            						
            						String[] currTitle = currStr.split("title=");
            						String[] href = currStr.split("href=");
            						
            						if (currTitle.length != 1) {
            							if (currTitle[1].equals("Permalink")) ignore = true;
            						}
            						else if (href.length != 1) {
            							link = href[1];
            						}
            					}
            					
            					//convert "../" into full link
            					String[] splitLink = link.split(Pattern.quote("../"));
            					int count = splitLink.length;
            					
            					if (count != 1) {
	            					String[] pageSlashes = pages.split("/");
	            					String fullLink = stem;
	            					
	            					for (int j = 0; j < pageSlashes.length-count; j++) {
	            						if (j != 0) fullLink += "/" + pageSlashes[j];
	            						else fullLink += pageSlashes[j];
	            					}
	            					
	            					fullLink += "/" + splitLink[splitLink.length-1];
	            					
	            					link = fullLink;
            					}
            					else {
            						if (link.length() > "https://".length()) {
	                					if (!link.substring(0, "https://".length()).equals("https://")) {
	                						String[] pageSlashes = pages.split("/");
	    	            					String fullLink = stem;
	    	            					
	    	            					for (int j = 0; j < pageSlashes.length-1; j++) {
	    	            						if (j != 0) fullLink += "/" + pageSlashes[j];
	    	            						else fullLink += pageSlashes[j];
	    	            					}
	    	            					
	    	            					fullLink += "/" + link;
	    	            					
	    	            					//System.out.println(link + "\t-->\t" + fullLink);
	    	            					
	    	            					link = fullLink;
	                					}
            						}
            					}
            					
            					//append final text
	            				if (!ignore && !linkText.equals("")) {
	            					newLine += linkText;
		            				links.add(new String[] {linkText, link});
	            				}
	            				
	            				linkURL = "";
	            				linkText = "";
            				}
            			}
            			
        				ignoreCurrChar = false;
            		}
            		
            		//System.out.println(currLine + "|");
            		//System.out.println(newLine + "\n");
            		
            		if (!newLine.equals("")) output += newLine + "\n";
            		if (lineTitle) title = newLine;
            	}
            	
            	//openers
            	if (currLine.equals("<main>")) {
            		mainOpen = true;
            	}
            	else if (currLine.equals("<div class=\"article-content js-article-content\" role=main itemscope itemprop=mainContentOfPage>")) {
            		articleContent = true;
            		divsOpen = 1;
            	}
            	else if (articleContent) {
            		divsOpen += countMatches(currLine, "<div");
            	}
            }
            //add trailing section
            if (addSection) {
            	sections.add(output);
            }
            
            reader.close();
            
            for (String section : sections) {
    	    	ArrayList<String> tokens = new ArrayList<String>();
	    		
    			Properties props = new Properties();
    		    props.setProperty("annotators", "tokenize,ssplit,pos,lemma");
    		    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
    		    CoreDocument document = pipeline.processToCoreDocument(section);
    		    
    		    for (CoreLabel token : document.tokens()) {
    		    	if (!contains(stopWords, token.lemma())) {
    		    		tokens.add(token.lemma().toLowerCase());
    		    	}
    		    }
    	    	
    	    	lemma.add(tokens);
            }

        	return new Page(title, sections, lemma, links);
        } 
        catch (Exception e) {
            System.out.println(e);
        }
    	
    	return new Page("", new ArrayList<String>(), new ArrayList<ArrayList<String>>(), new ArrayList<String[]>());
    }
    
    public static int countMatches(String str, String pattern) {
    	if (pattern.length() <= str.length()) {
    		return str.split(pattern).length - 1;
    	}
    	
    	return 0;
    }
    
    public static boolean contains(String[] arr, String str) {
    	for (int i = 0; i < arr.length; i++) {
    		if (arr[i].equals(str)) {
    			return true;
    		}
    	}
    	
    	return false;
    }
}
