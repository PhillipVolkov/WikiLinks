package EEProject.WikiLinks;

import java.util.ArrayList;
import java.util.Hashtable;

import org.tartarus.snowball.ext.PorterStemmer;


public class NaiveAlgorithm {
	static Hashtable<String, Page> savedPages = new Hashtable<String, Page>();
	
	//TODO output JSON, read saved pages
    public static void main(String[] args) {
    	

		if (!Constants.debugPrint) {
			System.out.printf("%-72s| %-128s| %-12s| %-12s| %-22s| %-1s", "Tokens", "URL", "Parse Time", "Algo Time", "Score", "Match?");
			System.out.println();
		}
		
    	if (!Constants.testLinks) {
	    	double startTime1 = System.nanoTime();
	        Page newPage = new Page(Constants.stem, Constants.page);
	        savedPages.put(Constants.stem+Constants.page, newPage);
	        
	    	double endTime1 = System.nanoTime();
	    	
			System.out.printf("%-72s| %-128s| %-12s| %-12s| %-22s| %-1s", "Initial Page", Constants.stem+Constants.page, ((endTime1-startTime1)/Math.pow(10, 9)), "N/A", "N/A", "N/A");
			System.out.println();

	    	double startTime2 = System.nanoTime();
	        for (String[] linkPair : newPage.getLinks()) {
	        	double match = getMatch(linkPair, Constants.stem);
	        	if (match != -1 && !Constants.debugPrint) {
	            	if (match >= Constants.threshold) System.out.printf("| true\n");
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
	    	testLinks.add(new String[] {"Elasticsearch.log file", "https://docs.gitlab.com/ee/administration/logs.html#elasticsearchlog", ""});

	    	testLinks.add(new String[] {"Kubernetes Engine", "https://docs.gitlab.com/ee/user/project/clusters/gitlab_managed_clusters.html", ""});
	    	testLinks.add(new String[] {"Kubernetes Pipeline", "https://docs.gitlab.com/ee/user/clusters/management_project.html", ""});
	    	testLinks.add(new String[] {"find users' api tokens", "https://docs.gitlab.com/ee/user/project/settings/project_access_tokens.html", ""});
	    	testLinks.add(new String[] {"gitlab search index", "https://docs.gitlab.com/ee/integration/elasticsearch.html", ""});

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
	        	double match = getMatch(linkPair, Constants.stem);
	        	
	        	if (match != -1 && !Constants.debugPrint) {
	            	if (match >= Constants.threshold) System.out.printf("| true\n");
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
		    	if (savedPages.containsKey(searchUrl)) {
		    		searchPage = savedPages.get(searchUrl);
		    	}
		    	else {
		    		searchPage = new Page(stem, linkPair[1].substring(stem.length(), linkPair[1].length()));
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
