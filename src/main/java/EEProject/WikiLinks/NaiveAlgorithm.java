package EEProject.WikiLinks;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.tartarus.snowball.ext.PorterStemmer;

//import edu.stanford.nlp.ling.CoreLabel;
//import edu.stanford.nlp.pipeline.CoreDocument;
//import edu.stanford.nlp.pipeline.StanfordCoreNLP;


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
			"#", "’s", "'s", "<", ">", "--", "=", "\"", "", "#"};
	
	static final boolean testLinks = true;
	static final boolean debugPrint = false;
	static final double threshold = 15;
	static final int maxTokenSeperation = 20;
	static final int maxTokenQuantity = 3;
	static final int wordCountMultiplier = 115;
	static final int occurenceDifferencePenalty = 75;
	static final double titleFactorWeight = 0.4;
	
	static final String stemmerDelims = " |\n|:|, |. |-";
	
	static Hashtable<String, Page> savedPages = new Hashtable<String, Page>();
	
	//TODO
    public static void main(String[] args) {
    	String stem = "https://docs.gitlab.com";
    	String page = "/ee/integration/elasticsearch.html";
    	

		if (!debugPrint) {
			System.out.printf("%-72s| %-128s| %-12s| %-12s| %-22s| %-1s", "Tokens", "URL", "Parse Time", "Algo Time", "Score", "Match?");
			System.out.println();
		}
		
    	if (!testLinks) {
	    	double startTime1 = System.nanoTime();
	        Page newPage = readGitLabPage(stem, page);
	        savedPages.put(stem+page, newPage);
	        
	    	double endTime1 = System.nanoTime();
	    	
			System.out.printf("%-72s| %-128s| %-12s| %-12s| %-22s| %-1s", "Initial Page", stem+page, ((endTime1-startTime1)/Math.pow(10, 9)), "N/A", "N/A", "N/A");
			System.out.println();

	    	double startTime2 = System.nanoTime();
	        for (String[] linkPair : newPage.getLinks()) {
	        	double match = getMatch(linkPair, stem);
	        	if (match != -1 && !debugPrint) {
	            	if (match >= threshold) System.out.printf("| true\n");
	            	else System.out.printf("| false\n");
	        	}
	        }
	    	double endTime2 = System.nanoTime();
	    	
	    	System.out.println("\nTime Elapsed: " + ((endTime2-startTime2)/Math.pow(10, 9)) + " seconds");
    	}
    	else {
	    	ArrayList<String[]> testLinks = new ArrayList<String[]>();
	    	testLinks.add(new String[] {"Auto DevOps", "https://docs.gitlab.com/ee/user/project/clusters/index.html#auto-devops", "Use Auto DevOps to automate the CI/CD process"});
	    	testLinks.add(new String[] {"the Prometheus cluster integration is enabled", "https://docs.gitlab.com/ee/user/clusters/integrations.html#prometheus-cluster-integration", 
	    			"When the Prometheus cluster integration is enabled, GitLab monitors the cluster’s health"});
	    	testLinks.add(new String[] {"Cluster management project", "https://docs.gitlab.com/ee/user/clusters/management_project.html", 
	    			"Attach a Cluster management project to your cluster to manage shared resources requiring cluster-admin privileges for installation, such as an Ingress controller"});
	    	testLinks.add(new String[] {"CI/CD Pipelines", "https://docs.gitlab.com/ee/ci/pipelines/index.html", "Create CI/CD Pipelines to build, test, and deploy to your cluster"});
	    	testLinks.add(new String[] {"cluster integrations", "https://docs.gitlab.com/ee/user/clusters/integrations.html", "Connect GitLab to in-cluster applications using cluster integrations"});
	    	testLinks.add(new String[] {"project access tokens", "https://docs.gitlab.com/ee/user/project/settings/project_access_tokens.html", "project access tokens"});
	    	testLinks.add(new String[] {"GitLab to manage your cluster for you", "https://docs.gitlab.com/ee/user/project/clusters/gitlab_managed_clusters.html", "See how to allow GitLab to manage your cluster for you"});
	    	testLinks.add(new String[] {"Infrastructure as Code", "https://docs.gitlab.com/ee/user/infrastructure", 
	    			"GitLab provides you with great solutions to help you manage your infrastrucure: Infrastructure as Code and GitOps"});
	    	testLinks.add(new String[] {"Deploy Boards", "https://docs.gitlab.com/ee/user/project/deploy_boards.html", "Use Deploy Boards to see the health and status of each CI environment running on your Kubernetes cluster"});
	    	testLinks.add(new String[] {"role-based or attribute-based access controls", "https://docs.gitlab.com/ee/user/project/clusters/cluster_access.html", "Use role-based or attribute-based access controls"});
	    	testLinks.add(new String[] {"Read more about Kubernetes monitoring", "https://docs.gitlab.com/ee/user/project/integrations/prometheus_library/kubernetes.html", "Read more about Kubernetes monitoring"});
	    	testLinks.add(new String[] {"Kubernetes with Knative", "https://docs.gitlab.com/ee/user/project/clusters/serverless/index.html", "Run serverless workloads on Kubernetes with Knative"});
	    	testLinks.add(new String[] {"related documentation", "https://docs.gitlab.com/ee/user/analytics/value_stream_analytics.html#permissions", "Find the current permissions on the Value Stream Analytics dashboard, as described in related documentation"});
	    	testLinks.add(new String[] {"Through the API", "https://docs.gitlab.com/ee/api/users.html", "You can also create users through the API as an admin"});

	    	testLinks.add(new String[] {"Kubernetes Engine", "https://docs.gitlab.com/ee/user/project/clusters/gitlab_managed_clusters.html", ""});
	    	testLinks.add(new String[] {"Kubernetes Pipeline", "https://docs.gitlab.com/ee/user/clusters/management_project.html", ""});
	    	testLinks.add(new String[] {"find users' api tokens", "https://docs.gitlab.com/ee/user/project/settings/project_access_tokens.html", ""});

	    	testLinks.add(new String[] {"package", "https://docs.gitlab.com/ee/user/packages/index.html", ""});
	    	testLinks.add(new String[] {"environment", "https://docs.gitlab.com/ee/ci/environments/index.html", "Use Deploy Boards to see the health and status of each CI environment running on your Kubernetes cluster"});
	    	testLinks.add(new String[] {"NGINX Ingress", "https://docs.gitlab.com/ee/user/project/integrations/prometheus_library/nginx.html", "Automatic monitoring of NGINX Ingress is also supported"});
	    	testLinks.add(new String[] {"Kubernetes podlogs", "https://docs.gitlab.com/ee/user/project/clusters/kubernetes_pod_logs.html", "View your Kubernetes podlogs directly in GitLab"});
	    	testLinks.add(new String[] {"instance", "https://docs.gitlab.com/ee/user/instance/clusters/index.html", "On the group level, to use the same cluster across multiple projects within your group"});
	    	testLinks.add(new String[] {"group", "https://docs.gitlab.com/ee/user/group/clusters/index.html", "On the instance level, to use the same cluster across multiple groups and projects."});
	    	
	    	testLinks.add(new String[] {"developers", "https://docs.gitlab.com/ee/user/permissions.html", "cautionThe whole cluster security is based on a model where developers are trusted, so only trusted users should be allowed to control your clusters"});
	    	testLinks.add(new String[] {"dynamic names", "https://docs.gitlab.com/ee/ci/environments/index.html", ""});
	    	testLinks.add(new String[] {"dependency", "https://docs.gitlab.com/ee/user/packages/index.html", ""});
	    	
	    	double startTime = System.nanoTime();
	    	for (String[] linkPair : testLinks) {
	        	double match = getMatch(linkPair, stem);
	        	
	        	if (match != -1 && !debugPrint) {
	            	if (match >= threshold) System.out.printf("| true\n");
	            	else System.out.printf("| false\n");
	        	}
	        }
	    	double endTime = System.nanoTime();
	    	
	    	System.out.println("\nTime Elapsed: " + ((endTime-startTime)/Math.pow(10, 9)) + " seconds");
    	}
    }
    
    public static double getMatch(String[] linkPair, String stem) {
    	ArrayList<String> tokens = new ArrayList<String>();
    	
    	//tokenize the search phrase
    	if (linkPair[1].length() >= stem.length()) {
	    	if (linkPair[1].substring(0, stem.length()).equals(stem)) {
		    	double startTime = System.nanoTime();
	    		final boolean subSection = linkPair[1].indexOf("#") != -1;
	    		
	    		//link stemming
			    StringTokenizer  tokenizer = new StringTokenizer(linkPair[0].toLowerCase(), stemmerDelims);
    		    PorterStemmer stemmer = new PorterStemmer();
    		    
    		    while (tokenizer.hasMoreElements()) {
    		    	String token = tokenizer.nextToken().toLowerCase().strip();
    		    	token = String.join(" ", token.split("\n"));
    		    	
    		    	stemmer.setCurrent(token);
    		    	stemmer.stem();
    		    	token = stemmer.getCurrent();
    		    	
    		    	if (!contains(stopWords, token) && !tokens.contains(token)) {
    		    		tokens.add(token);
    		    	}
    		    }

				//get lemmatized page contents of search URL
		    	double startTimeSearch = System.nanoTime();
				Page searchPage;
				String searchUrl = linkPair[1].split("#")[0];
				
				//check saved dictionary by url
		    	if (savedPages.containsKey(searchUrl)) {
		    		searchPage = savedPages.get(searchUrl);
		    	}
		    	else {
		    		searchPage = readGitLabPage(stem, linkPair[1].substring(stem.length(), linkPair[1].length()));
		    		savedPages.put(searchUrl, searchPage);
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
	    				
	    				if (debugPrint) System.out.println(section);
	    				
	    				int offset = 0;
	    				for (int i = 0; i < headingTokens.length; i++) {
	    					if (!contains(stopWords, headingTokens[i]) && !headingTokens[i].equals("")) {
	    						stringTitle += String.join(" ", section.get(i-offset).split(" |\\.|-")) + " ";
	    					}
	    					else {
	    						stringTitle += headingTokens[i] + " ";
	    						offset--;
	    					}
	    				}
	    			}
	    			else {
	    				stringTitle = String.join(" ", headingTokens);	   
	    			}
	    		}
	    		
	    		//title lemma
	    		ArrayList<String> title = new ArrayList<String>();
	    		tokenizer = new StringTokenizer(stringTitle.toLowerCase(), stemmerDelims);
    		    stemmer = new PorterStemmer();
    		    
    		    while (tokenizer.hasMoreElements()) {
    		    	String token = tokenizer.nextToken().toLowerCase().strip();
    		    	token = String.join(" ", token.split("\n"));
    		    	
    		    	stemmer.setCurrent(token);
    		    	stemmer.stem();
    		    	token = stemmer.getCurrent();
    		    	
    		    	if (!contains(stopWords, token)) {
    		    		title.add(token);
    		    	}
    		    }
			    
			    if (debugPrint) {
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
						if (!debugPrint) System.out.printf("%-64s| %-128s| %-12s| %-12s", linkPair[0], (stem + linkPair[1].substring(stem.length(), linkPair[1].length())), 
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
									
									score += (maxTokenSeperation/Math.abs((double)ind1Index - ind2Index))/maxTokenSeperation;
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
					double tokenQuantityFactor = (((double)maxTokenQuantity-1)/-indexes.length + maxTokenQuantity)/maxTokenQuantity;
					double tokenProximityFactor = (double)score/scoreCount;
					double wordCountFactor = (-wordCountMultiplier/((double)wordCount+wordCountMultiplier)+1);
					double tokenWordCountFactor = (double)totalOccurences/wordCount*wordCountFactor;
					double titleMatchFactor = (double)titleMatch/title.size();
					double singleWordReliancePenalty = occurenceDifferencePenalty*(totalOccurences/(((double)maxOccurence-secondMaxOccurence)+occurenceDifferencePenalty))/totalOccurences;
					
					//balancing of token proximity weight
					tokenProximityFactor *= 1-(1/(double)indexes.length);
					tokenWordCountFactor *= 1/(double)indexes.length;
					
					double finalScore = ((tokenProximityFactor + tokenWordCountFactor)*(1-titleFactorWeight) + titleMatchFactor*titleFactorWeight) * singleWordReliancePenalty * 100;
					
					if ((finalScore < threshold || indexes.length == 1) && !linkPair[2].equals("")) {
						double contextScore = getMatch(new String[] {linkPair[2], linkPair[1], ""}, stem);
						
						if (contextScore > finalScore) {
							return contextScore;
						}
						else {
							System.out.println();
						}
					}
					
					if (debugPrint) {
						System.out.printf("| %-24s| %-24s| %-24s| %-24s| %-24s| %-24s|", tokenProximityFactor, tokenWordCountFactor, wordCount, titleMatchFactor, singleWordReliancePenalty, finalScore);
						System.out.println();
					}
					
					double endTime = System.nanoTime();
					if (!debugPrint) {
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
		    		
					if (!debugPrint) System.out.printf("%-72s| %-128s| %-12s| %-12s| %-22s", linkPair[0], (stem + linkPair[1].substring(stem.length(), linkPair[1].length())), "N/A", "N/A", "0");
		    		return 0;
		    	}
	    	}
    	}
    	
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
            Hashtable<String, Integer> permaLinks = new Hashtable<String, Integer>();
            
            String currLine = "";
            String output = "";
    		String linkContext = "";
            boolean mainOpen = false;
            boolean articleContent = false;
            int divsOpen = 0;

    		boolean addSection = false;
    		ArrayList<Integer[]> noContext = new ArrayList<Integer[]>();
    		
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
            		int prevPeriod = -2;
            		int prevL = -2;
            		int prevU = -2;
            		boolean linkBracketOpen = false;
            		boolean linkUrlParse = false;
            		boolean linkSentence = false;
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
            			else if (currLine.charAt(i) == 'u' && (bracketPrevOpen == i-1 || bracketPrevClosed == i-1)) {
            				prevU = i;
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
            				linkSentence = true;
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
	    						internalText = "";
	    						ignoreBracketContents = false;
            				}
    					}
            			
            			//handle link text
            			if (!linkURL.equals("")) {
            				if (!linkBracketOpen) {
            					Scanner scan = new Scanner(linkURL);
            					boolean permaLink = false;
            					String link = "";
            					
            					if (pages.contains("#")) pages = pages.split("#")[0];
            					
            					//find link
            					while (scan.hasNext()) {
            						String currStr = scan.next();
            						
            						String[] currTitle = currStr.split("title=");
            						String[] href = currStr.split("href=");
            						
            						if (currTitle.length != 1) {
            							if (currTitle[1].equals("Permalink")) {
            								permaLink = true;
            							}
            						}
            						if (href.length != 1) {
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
	                					if (!link.substring(0, "https://".length()).equals("https://") && !link.substring(0, "http://".length()).equals("http://")) {
	                						if (link.charAt(0) == '#') {
	                							link = stem + pages + link;
	                						}
	                						else {
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
            					}
            					
            					//append final text
	            				if (!permaLink && !linkText.equals("")) {
	            					newLine += linkText;
		            				links.add(new String[] {linkText, link, ""});
		            				
		            				if (addSection) {
		            					noContext.add(new Integer[] {sections.size(), output.length(), links.size()-1});
		            				}
	            				}
	            				else if (permaLink) {
	            					permaLinks.put(link.split("#")[1], sections.size());
	            				}
	            				
	            				linkURL = "";
	            				linkText = "";
            				}
            			}
            			
        				ignoreCurrChar = false;
            		}
        			
            		//System.out.println(currLine + "|");
            		if (!newLine.equals("")) {
            			output += newLine + "\n";
            		}
            		
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
            
            //context
            for (int i = 0; i < noContext.size(); i++) {
            	Integer[] loc = noContext.get(i);
            	
            	if (loc[0] != -1) {
            		loc[1] = loc[1] + sections.get(loc[0]).substring(loc[1]).indexOf(links.get(loc[2])[0]);
            		
            		//find start
            		int sentenceStart = sections.get(loc[0]).substring(0, loc[1]+1).lastIndexOf(".\n")+1;
            		if (sentenceStart == 0) sentenceStart = sections.get(loc[0]).substring(0, loc[1]+1).lastIndexOf("\n")+1;
            		
            		int temp = sections.get(loc[0]).substring(0, loc[1]+1).lastIndexOf(". ")+1;
            		if (temp > sentenceStart) sentenceStart = temp;
            		
            		//find end
            		int sentenceEnd = loc[1] + sections.get(loc[0]).substring(loc[1]).indexOf(".\n");
            		
            		temp = loc[1] + sections.get(loc[0]).substring(loc[1]).indexOf(". ");
            		if (temp < sentenceEnd && temp >= loc[1]) sentenceEnd = temp;
            		
            		
            		String context = "";
            		
            		if (sentenceEnd >= loc[1]) context = sections.get(loc[0]).substring(sentenceStart, sentenceEnd);
            		else context = sections.get(loc[0]).substring(sentenceStart);
            		
            		context = context.strip();
            		context = String.join(" ", context.split("\n"));
            		
            		links.get(loc[2])[2] = context;
            	}
            }
            
            //stemming
            for (String section : sections) {
    	    	ArrayList<String> tokens = new ArrayList<String>();
    	    	
    		    StringTokenizer  tokenizer = new StringTokenizer(section.toLowerCase(), stemmerDelims);
    		    PorterStemmer stemmer = new PorterStemmer();
    		    
    		    while (tokenizer.hasMoreElements()) {
    		    	String token = tokenizer.nextToken().toLowerCase().strip();
    		    	token = String.join(" ", token.split("\n"));
    		    	
    		    	stemmer.setCurrent(token);
    		    	stemmer.stem();
    		    	token = stemmer.getCurrent();
    		    	
    		    	if (!contains(stopWords, token)) {
    		    		tokens.add(token);
    		    	}
    		    }

    	    	lemma.add(tokens);
            }

        	return new Page(title, sections, lemma, links, permaLinks);
        } 
        catch (Exception e) {
            System.out.println(e);
        }
    	
    	return new Page("", new ArrayList<String>(), new ArrayList<ArrayList<String>>(), new ArrayList<String[]>(), new Hashtable<String, Integer>());
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
