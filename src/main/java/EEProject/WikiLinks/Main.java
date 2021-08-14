package EEProject.WikiLinks;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.opencsv.CSVReader;

import weka.attributeSelection.InfoGainAttributeEval;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.LibSVM;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

public class Main {
	private static final boolean testLinks = true;
	private static final String operation = "create train";
	private static final String crawlGoal = "links";
	private static final String processing = "normalize";
	
	private static final String[] crawlStartPage = new String[] {"Gitlab Docs", "https://docs.gitlab.com/ee/", ""};
	private static final String jsonName = "trainingSetLinks.json";
	private static final String sentenceName = "sentences.json";
	private static final String idfName = "idf.csv";
	private static final String trainingDataName = "trainingSet.csv";
	private static final String testingDataName = "testingSet.csv";
	private static final String mlModelName = "ml.model";
	
	private static int counter = 0;
	private static final int maxCount = 10;
	
	private static Remove removeFilter;
	
	//TODO improve TfIdf to give similarity rating (ADD TITLES INTO SEPERATE COUNT), fix low occurrence ratings and edit links dataset
		//Analyze low occurrences and high false positives, remove inaccurate rows
		//Discover source of improper matches, add additional useful differentiators
		//Use word2vec and cosine similarity for increasing synonym comprehension
		//Add support for finding top phrases also, use that for multiple token queries
		//MAYBE just improve title similarity matching
	
	//TODO standardize (minVal = -1, meanVals = 0, maxVal = 1), improve test data set, analyze low similarity
	
    public static void main(String[] args) {
    	
    	if (operation.toLowerCase().equals("crawl")) {
    		System.out.printf("%-6d| %-72s| %-128s| %-128s|", 0, "Title", "URL", "SearchUrl");
    		System.out.println();
    		crawlPage(crawlStartPage);
    	    
    		if (crawlGoal.equals("links")) {
	    	    try {
	    			JSONObject savedPagesJSON = new JSONObject();
	    	    
		    	    int count = 0;
		    	    for (String[] searchLinkPair : Constants.getSavedLinks()) {
		    	    	//add true link
		        	    JSONArray links = new JSONArray();
		        	    links.add(searchLinkPair[0]);
		        	    links.add(searchLinkPair[1]);
		        	    links.add(searchLinkPair[2]);
		        	    links.add(true);
		        	    
		    	    	savedPagesJSON.put(count, links);
		    	    	
		    	    	count++;
		    	    	
		    	    	//add false link
		    	    	links = new JSONArray();
		        	    links.add(searchLinkPair[0]);
		        	    int randIndex = Constants.getSavedLinks().indexOf(searchLinkPair);
		        	    while (randIndex == Constants.getSavedLinks().indexOf(searchLinkPair)) {
		        	    	randIndex = ThreadLocalRandom.current().nextInt(0, Constants.getSavedLinks().size());
		        	    }
		        	    links.add(Constants.getSavedLinks().get(randIndex)[1]);
		        	    links.add(searchLinkPair[2]);
		        	    links.add(false);
		        	    
		    	    	savedPagesJSON.put(count, links);
		    	    	
		    	    	count++;
		    	    }
	    		
	    			Files.write(Paths.get(jsonName), savedPagesJSON.toJSONString().getBytes());
				} catch (Exception e) {
					e.printStackTrace();
				}
    		}
    		else if (crawlGoal.equals("saveSentences")) {
    	    	JSONObject savedSentencesJSON = new JSONObject();
    	    	JSONArray sentences = new JSONArray();

    	    	for (ArrayList<String> sentence : Constants.getSavedSentences()) {
        	    	JSONArray sentenceJSON = new JSONArray();
    	    		for (String stem : sentence) {
    	    			sentenceJSON.add(stem);
    	    		}
    	    		sentences.add(sentenceJSON);
    	    	}

    	    	savedSentencesJSON.put("Sentences:", sentences);
    	    	
    	    	try {
    	    		Files.write(Paths.get(sentenceName), savedSentencesJSON.toJSONString().getBytes());
	    		} 
    	    	catch (IOException e1) {
	    			e1.printStackTrace();
	    		}
    	    }
    	    else if (crawlGoal.equals("idf")) {
	    	    try {
	        		FileWriter writer = new FileWriter(idfName);
	        		writer.append("term,");
	        		writer.append("frequency,");
	        		writer.append("total,");
	        		writer.append("idf\n");
	        		
	    	        for (Map.Entry<String, Integer> entry : Constants.getSavedTerms().entrySet()) {
		        		writer.append(entry.getKey() + ",");
		        		writer.append(entry.getValue() + ",");
		        		writer.append(Constants.getSavedLinks().size() + ",");
		        		writer.append(Math.log((double)Constants.getSavedLinks().size()/entry.getValue()) + "\n");
	    	        }
	    	        writer.flush();
	    	        writer.close();
	    	        
	    		} catch (IOException e1) {
	    			e1.printStackTrace();
	    		}
    	    }
    	}
    	else if (operation.toLowerCase().equals("create train")) createTrainSet();
    	else if (operation.toLowerCase().equals("create test")) createTestSet();
    	else if (operation.toLowerCase().equals("train model")) trainModel();
    	else if (operation.toLowerCase().equals("test model")) testModel();
    	else if (operation.toLowerCase().equals("debug")) {
        	ScoreCompiler score = new ScoreCompiler(new String[] {"Auto DevOps", "https://docs.gitlab.com/ee/user/project/clusters/index.html#auto-devops", ""}, Constants.stem);
        	score.calculateMatch();
        	System.out.println();
        	System.out.println("\nScores:");
        	System.out.println(score.getTokens());
        	System.out.println(score.getTitle());
        	System.out.println(score.getTitleSimilarity());
        	System.out.println(score.getTfIdfScore());
        	System.out.println("\nSummary:");
        	System.out.println(score.getSearchPage().getTopStemsSorted(score.getTokens().size()*3));
        	System.out.println(score.getSearchPage().getTopStemsSortedSections(score.getTokens().size()*3, "auto-devops"));
        	System.out.println(score.getContentSimilarity());
    	}
    }
    
    private static void trainModel() {
		try {
			System.out.println("\nTRAINING DATA SET:");
			System.out.println("------------------");
			CSVLoader source = new CSVLoader();
			source.setSource(new File(trainingDataName));
	    	Instances data = source.getDataSet();
	    	data.setClassIndex(data.numAttributes()-1);
	    	
	    	removeFilter = new Remove();
	    	removeFilter.setAttributeIndicesArray(new int[] {3, 4, 5, data.numAttributes()-1});
	    	removeFilter.setInvertSelection(true);
	    	removeFilter.setInputFormat(data);
	    	data = Filter.useFilter(data, removeFilter);
	    	
	    	InfoGainAttributeEval attrEval = new InfoGainAttributeEval();
	    	attrEval.buildEvaluator(data);
	    	
	    	for (int i = 0; i < data.numAttributes()-1; i++) {
	    	    Attribute attr = data.attribute(i);
	    	    double score  = attrEval.evaluateAttribute(i);
	    	    
	    	    System.out.println(attr.name() + ": " + score);
	    	}
	    	
	    	LibSVM cls = new LibSVM();
			cls.buildClassifier(data);
			weka.core.SerializationHelper.write(mlModelName, cls);
			
			Evaluation eval = new Evaluation(data);
			eval.crossValidateModel(cls, data, 10, new Random(1));
			System.out.println("Correct: " + eval.pctCorrect() + "%");
			System.out.println("------------------");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		getNaiveCorrect(trainingDataName);
		
		testModel();
    }
    
    private static void testModel() {
    	try {
			System.out.println("\nTESTING DATA SET:");
			System.out.println("------------------");
			CSVLoader source = new CSVLoader();
			source.setSource(new File(testingDataName));
	    	Instances data = source.getDataSet();
	    	data.setClassIndex(data.numAttributes()-1);
	    	
	    	if (removeFilter != null) data = Filter.useFilter(data, removeFilter);
	    	
	    	InfoGainAttributeEval attrEval = new InfoGainAttributeEval();
	    	attrEval.buildEvaluator(data);
	    	
	    	for (int i = 0; i < data.numAttributes()-1; i++) {
	    	    Attribute attr = data.attribute(i);
	    	    double score  = attrEval.evaluateAttribute(i);
	    	    
	    	    System.out.println(attr.name() + ": " + score);
	    	}
	    	
	    	LibSVM cls = (LibSVM) weka.core.SerializationHelper.read(mlModelName);
	    	Evaluation eval = new Evaluation(data);
			eval.evaluateModel(cls, data);
			System.out.println("Correct: " + eval.pctCorrect() + "%");
			System.out.println("------------------");
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		getNaiveCorrect(testingDataName);
    }
    
    private static void getNaiveCorrect(String file) {
    	try (CSVReader reader = new CSVReader(new FileReader(file))) {
			String[] row;
			reader.readNext();
			int matches = 0;
			int count = 0;
			
			while ((row = reader.readNext()) != null) {
				double match = Double.parseDouble(row[row.length-4]);
				if (match != -1 && !Constants.debugPrint) {
					if ((match >= Constants.threshold) == Boolean.parseBoolean(row[row.length-1])) matches++;
				}
				
				count++;
			}
			
			System.out.println("Naive Correct: " + ((double)matches/count*100) + "%");
			System.out.println("------------------");
			System.out.println();
		}
		catch (Exception e) {e.printStackTrace();}
    }
    
    //recursion to crawl through all links starting with initial page
    private static void crawlPage(String[] linkPair) {
    	counter++;
    	String searchUrl = linkPair[1].split("#")[0];
    	
    	Page searchPage;
    	if (!Constants.getSavedPages().contains(searchUrl)) {
    		searchPage = new Page(Constants.stem, linkPair[1].substring(Constants.stem.length(), linkPair[1].length()));
	    	Constants.getSavedPages().put(searchUrl, searchPage);
    	}
    	else {
    		searchPage = Constants.getSavedPages().get(searchUrl);
    	}
    	
    	System.out.printf("%-6d| %-72s| %-128s| %-128s|", counter, linkPair[0], linkPair[1], searchUrl);
		System.out.println();
	    
    	if (!searchPage.getFailed() && (counter <= maxCount)) {
    		//if document is unique
    		if (!Constants.getSavedLinks().contains(linkPair)) {
    			//add to saved hastable
    			Constants.addSavedLink(linkPair);
    			
    			if (crawlGoal.equals("saveSentences")) {
    				for (ArrayList<String> sentence : searchPage.getSentenceStems()) {
    					if (!Constants.getSavedSentences().contains(sentence)) {
    						Constants.addSavedSentence(sentence);
    					}
    				}
    			}
    			else if (crawlGoal.equals("idf")) {
	    			//calculate idf
		    		ArrayList<String> foundStems = new ArrayList<String>();
		    		for (ArrayList<String> stemList : searchPage.getStems()) {
		    			for (String stem : stemList) {
		    				if (foundStems.contains(stem)) break;
		    				
		    				if (Constants.getSavedTerms().containsKey(stem)) {
		    					Constants.incrementSavedTerm(stem);
		    				}
		    				else {
		    					Constants.addSavedTerm(stem);
		    				}
		    				
		    				foundStems.add(stem);
		    			}
		    		}
    			}
	    		
	    		
    		}
    		
	    	for (String[] searchLinkPair : searchPage.getLinks()) {
				String linkSearchUrl = searchLinkPair[1].split("#")[0];
				
				if (counter >= maxCount) {
					break;
				}
	    		
				//check saved dictionary by url
				if (searchLinkPair[1].length() >= Constants.stem.length()) {
			    	if (searchLinkPair[1].substring(0, Constants.stem.length()).equals(Constants.stem) && !Constants.getSavedPages().containsKey(linkSearchUrl)) {
			    		crawlPage(searchLinkPair);
			    	}
		    	}
	    	}
    	}
    }
    
    private static void writeDataSet(String fileName, ArrayList<String[]> testLinks, ArrayList<Boolean> testLinksCheck) {
    	try {
    		FileWriter writer = new FileWriter(fileName);
    		writer.append("Proximity,");
    		writer.append("WordCount,");
    		writer.append("TitleMatch,");
    		writer.append("TfIdf,");
    		writer.append("TitleSimilarity,");
    		writer.append("ContentSimilarity,");
    		writer.append("Score,");
    		writer.append("Phrase,");
    		writer.append("Link,");
    		writer.append("Match\n");
			
    		ArrayList<Double[]> scores = new ArrayList<Double[]>();
    		ArrayList<String[]> labels = new ArrayList<String[]>();

    		final int numScores = 7;
    		
    		Double[] scoreMean = new Double[numScores];
    		for (int i = 0; i < scoreMean.length; i++) scoreMean[i] = 0.0;
    		
    		Double[] scoreMin = new Double[numScores];
    		for (int i = 0; i < scoreMin.length; i++) scoreMin[i] = 1.0;
    		
    		Double[] scoreMax = new Double[numScores];
    		for (int i = 0; i < scoreMax.length; i++) scoreMax[i] = 0.0;
    		
    		//store all variables
			int total = 0;
	        for (String[] linkPair : testLinks) {
	        	ScoreCompiler score = new ScoreCompiler(linkPair, Constants.stem);
	        	score.calculateMatch();
	        	
	        	if (score.getValid()) {
	        		Double[] scoreRow = new Double[numScores];
	        		
	        		scoreRow[0] = score.getProximityFactorNoCurve();
	        		scoreRow[1] = score.getWordCountNoCurve();
	        		scoreRow[2] = score.getTitleMatch();
	        		
	        		scoreRow[3] = score.getTfIdfScore();
	        		if (!Double.isNaN(score.getTitleSimilarity())) scoreRow[4] = score.getTitleSimilarity();
	        		else scoreRow[4] = 0.0;
	        		if (!Double.isNaN(score.getContentSimilarity())) scoreRow[5] = score.getContentSimilarity();
	        		else scoreRow[5] = 0.0;
	        		
	        		scoreRow[6] = score.getFinalScore();
	        		
	        		for (int i = 0; i < scoreRow.length; i++) {
	        			scoreMean[i] += scoreRow[i];
	        			
	        			if (scoreRow[i] < scoreMin[i]) scoreMin[i] = scoreRow[i];
	        			else if (scoreRow[i] > scoreMax[i]) scoreMax[i] = scoreRow[i];
	        		}
	        		
	        		scores.add(scoreRow);
	        		
	        		String[] labelRow = new String[2];
	        		labelRow[0] = String.join(" ", linkPair[0].split(","));
	        		labelRow[1] = linkPair[1];
	        		
	        		labels.add(labelRow);
	        		
		        	total++;
	        	}
		        	
	        	System.out.println();
	        }
	        
	        if (processing.equals("normalize")) {
	        	//scale appropriately
	        	for (Double[] scoreRow : scores) {
	    			for (int i = 0; i < scoreRow.length; i++) {
	    				scoreRow[i] = (scoreRow[i]-scoreMin[i])/(scoreMax[i]-scoreMin[i]);
	        		}
	    		}
	        }
	        else if (processing.equals("standardize")) {
		        //find the mean for each score
		        for (int i = 0; i < scoreMean.length; i++) {
	    			scoreMean[i] /= total;
	    		}
	
		        //subtract mean and find the deviation for each score
	    		Double[] scoreStandardDeviation = new Double[numScores];
	    		for (int i = 0; i < scoreStandardDeviation.length; i++) scoreStandardDeviation[i] = 0.0;
		        
	    		for (Double[] scoreRow : scores) {
	    			for (int i = 0; i < scoreRow.length; i++) {
	    				scoreRow[i] -= scoreMean[i];
	    				
	    				scoreStandardDeviation[i] += Math.pow(scoreRow[i], 2);
	        		}
	    		}
	    		
	    		for (int i = 0; i < scoreStandardDeviation.length; i++) {
	    			scoreStandardDeviation[i] /= scores.size();
	    			scoreStandardDeviation[i] = Math.sqrt(scoreStandardDeviation[i]);
	    		}
	    		
	    		//divide by deviation for each score
	    		for (Double[] scoreRow : scores) {
	    			for (int i = 0; i < scoreRow.length; i++) {
	    				scoreRow[i] /= scoreStandardDeviation[i];
	        		}
	    		}
	        }
    		
    		//append scores to csv
    		for (int i = 0; i < scores.size(); i++) {
    			for (int j = 0; j < scores.get(i).length; j++) {
        			writer.append(scores.get(i)[j] + ",");
        		}
    			
    			for (int j = 0; j < labels.get(i).length; j++) {
        			writer.append(labels.get(i)[j] + ",");
        		}
    			
    			writer.append(testLinksCheck.get(i) + "\n");
    		}
    		
	        writer.flush();
	        writer.close();
	        
		} catch (IOException e1) {
			e1.printStackTrace();
		}
    }
    
    private static void createTrainSet() {
    	ArrayList<String[]> testLinksArr = new ArrayList<String[]>();
    	ArrayList<Boolean> testLinksCheck = new ArrayList<Boolean>();
    	
		try {
			FileReader reader = new FileReader(jsonName);
	        JSONParser jsonParser = new JSONParser();
			
	        JSONObject obj = ((JSONObject)jsonParser.parse(reader));
	        
			for (int i = 0; i < obj.size(); i++) {
				JSONArray arr = (JSONArray)obj.get(i+"");
				
				testLinksArr.add(new String[] {(String)arr.get(0), (String)arr.get(1), (String)arr.get(2)});
				testLinksCheck.add((Boolean)arr.get(3));
			}
		} catch (IOException | ParseException e1) {
			e1.printStackTrace();
		}
    	
		writeDataSet(trainingDataName, testLinksArr, testLinksCheck);
    }
    
    private static void createTestSet() {
    	if (!Constants.debugPrint) {
			System.out.printf("%-72s| %-128s| %-12s| %-12s| %-22s| %-1s", "Tokens", "URL", "Parse Time", "Algo Time", "Score", "Match?");
			System.out.println();
		}

    	ArrayList<String[]> testLinksArr;
    	ArrayList<Boolean> testLinksCheck = new ArrayList<Boolean>();
		
    	if (!testLinks) {
	    	double startTime1 = System.nanoTime();
	        Page newPage = new Page(Constants.stem, Constants.page);
	        Constants.putSavedPage(Constants.stem+Constants.page, newPage);
	        
	    	double endTime1 = System.nanoTime();
	    	
			System.out.printf("%-72s| %-128s| %-12s| %-12s| %-22s| %-1s", "Initial Page", Constants.stem+Constants.page, ((endTime1-startTime1)/Math.pow(10, 9)), "N/A", "N/A", "N/A");
			System.out.println();

	    	testLinksArr = newPage.getLinks();
	    	
	    	for (int i = 0; i < testLinksArr.size(); i++) {
	    		testLinksCheck.add(true);
	    	}
    	}
    	else {
    		testLinksArr = new ArrayList<String[]>();
    		
	    	testLinksArr.add(new String[] {"Auto DevOps", "https://docs.gitlab.com/ee/user/project/clusters/index.html#auto-devops", "Use Auto DevOps to automate the CI/CD process"});
	    	testLinksCheck.add(true);
	    	testLinksArr.add(new String[] {"the Prometheus cluster integration is enabled", "https://docs.gitlab.com/ee/user/clusters/integrations.html#prometheus-cluster-integration", 
	    			"When the Prometheus cluster integration is enabled, GitLab monitors the cluster’s health"});
	    	testLinksCheck.add(true);
	    	testLinksArr.add(new String[] {"Cluster management project", "https://docs.gitlab.com/ee/user/clusters/management_project.html", 
	    			"Attach a Cluster management project to your cluster to manage shared resources requiring cluster-admin privileges for installation, such as an Ingress controller"});
	    	testLinksCheck.add(true);
	    	testLinksArr.add(new String[] {"CI/CD Pipelines", "https://docs.gitlab.com/ee/ci/pipelines/index.html", "Create CI/CD Pipelines to build, test, and deploy to your cluster"});
	    	testLinksCheck.add(true);
	    	testLinksArr.add(new String[] {"cluster integrations", "https://docs.gitlab.com/ee/user/clusters/integrations.html", "Connect GitLab to in-cluster applications using cluster integrations"});
	    	testLinksCheck.add(true);
	    	testLinksArr.add(new String[] {"project access tokens", "https://docs.gitlab.com/ee/user/project/settings/project_access_tokens.html", "project access tokens"});
	    	testLinksCheck.add(true);
	    	testLinksArr.add(new String[] {"GitLab to manage your cluster for you", "https://docs.gitlab.com/ee/user/project/clusters/gitlab_managed_clusters.html", "See how to allow GitLab to manage your cluster for you"});
	    	testLinksCheck.add(true);
	    	testLinksArr.add(new String[] {"Infrastructure as Code", "https://docs.gitlab.com/ee/user/infrastructure", 
	    			"GitLab provides you with great solutions to help you manage your infrastrucure: Infrastructure as Code and GitOps"});
	    	testLinksCheck.add(true);
	    	testLinksArr.add(new String[] {"Deploy Boards", "https://docs.gitlab.com/ee/user/project/deploy_boards.html", "Use Deploy Boards to see the health and status of each CI environment running on your Kubernetes cluster"});
	    	testLinksCheck.add(true);
	    	testLinksArr.add(new String[] {"role-based or attribute-based access controls", "https://docs.gitlab.com/ee/user/project/clusters/cluster_access.html", "Use role-based or attribute-based access controls"});
	    	testLinksCheck.add(true);
	    	testLinksArr.add(new String[] {"Read more about Kubernetes monitoring", "https://docs.gitlab.com/ee/user/project/integrations/prometheus_library/kubernetes.html", "Read more about Kubernetes monitoring"});
	    	testLinksCheck.add(true);
	    	testLinksArr.add(new String[] {"Kubernetes with Knative", "https://docs.gitlab.com/ee/user/project/clusters/serverless/index.html", "Run serverless workloads on Kubernetes with Knative"});
	    	testLinksCheck.add(true);
	    	testLinksArr.add(new String[] {"Create users", "https://docs.gitlab.com/ee/api/users.html", "You can also create users through the API as an admin"});
	    	testLinksCheck.add(true);
	    	testLinksArr.add(new String[] {"Elasticsearch.log file", "https://docs.gitlab.com/ee/administration/logs.html#elasticsearchlog", ""});
	    	testLinksCheck.add(true);

	    	testLinksArr.add(new String[] {"Kubernetes Engine", "https://docs.gitlab.com/ee/user/project/clusters/gitlab_managed_clusters.html", ""});
	    	testLinksCheck.add(false);
	    	testLinksArr.add(new String[] {"Kubernetes Pipeline", "https://docs.gitlab.com/ee/user/clusters/management_project.html", ""});
	    	testLinksCheck.add(false);
	    	testLinksArr.add(new String[] {"find users api tokens", "https://docs.gitlab.com/ee/user/project/settings/project_access_tokens.html", ""});
	    	testLinksCheck.add(false);
	    	testLinksArr.add(new String[] {"gitlab search index", "https://docs.gitlab.com/ee/integration/elasticsearch.html", ""});
	    	testLinksCheck.add(false);

	    	testLinksArr.add(new String[] {"package", "https://docs.gitlab.com/ee/user/packages/index.html", ""});
	    	testLinksCheck.add(true);
	    	testLinksArr.add(new String[] {"environment", "https://docs.gitlab.com/ee/ci/environments/index.html", "Use Deploy Boards to see the health and status of each CI environment running on your Kubernetes cluster"});
	    	testLinksCheck.add(true);
	    	testLinksArr.add(new String[] {"NGINX Ingress", "https://docs.gitlab.com/ee/user/project/integrations/prometheus_library/nginx.html", "Automatic monitoring of NGINX Ingress is also supported"});
	    	testLinksCheck.add(true);
	    	testLinksArr.add(new String[] {"Kubernetes podlogs", "https://docs.gitlab.com/ee/user/project/clusters/kubernetes_pod_logs.html", "View your Kubernetes podlogs directly in GitLab"});
	    	testLinksCheck.add(true);
	    	testLinksArr.add(new String[] {"instance level", "https://docs.gitlab.com/ee/user/instance/clusters/index.html", "On the instance level, to use the same cluster across multiple groups and projects."});
	    	testLinksCheck.add(true);
	    	testLinksArr.add(new String[] {"group level", "https://docs.gitlab.com/ee/user/group/clusters/index.html", "On the group level, to use the same cluster across multiple projects within your group"});
	    	testLinksCheck.add(true);
	    	
	    	testLinksArr.add(new String[] {"developers", "https://docs.gitlab.com/ee/user/permissions.html", "The whole cluster security is based on a model where developers are trusted, so only trusted users should be allowed to control your clusters"});
	    	testLinksCheck.add(false);
	    	testLinksArr.add(new String[] {"dynamic names", "https://docs.gitlab.com/ee/ci/environments/index.html", ""});
	    	testLinksCheck.add(false);
	    	testLinksArr.add(new String[] {"dependency", "https://docs.gitlab.com/ee/user/packages/index.html", ""});
	    	testLinksCheck.add(false);
    	}
    	
    	writeDataSet(testingDataName, testLinksArr, testLinksCheck);
    }
    
    private static void verifyLinks(ArrayList<String[]> links, ArrayList<Boolean> check) {
    	int matches = 0;
    	
    	double startTime = System.nanoTime();
    	int i = 0;
        for (String[] linkPair : links) {
        	ScoreCompiler score = new ScoreCompiler(linkPair, Constants.stem);
        	score.calculateMatch();
        	
        	double match = score.getFinalScore();
        	if (match != -1 && !Constants.debugPrint) {
            	if (match >= Constants.threshold) System.out.printf("| true\n");
            	else System.out.printf("| false\n");
            	
            	if ((match >= Constants.threshold) == check.get(i)) matches++;
        	}
        	
        	i++;
        }
    	double endTime = System.nanoTime();
    	
    	System.out.println("\nTime Elapsed: " + ((endTime-startTime)/Math.pow(10, 9)) + " seconds");
    	System.out.println("\nCorrect: " + ((double)matches/links.size()));
    }
}