package EEProject.WikiLinks;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Random;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import weka.attributeSelection.InfoGainAttributeEval;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.LibSVM;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

public class Main {
	private static final boolean testLinks = true;
	private static final String operation = "summarize";
	private static final String[] crawlStartPage = new String[] {"Gitlab Docs", "https://docs.gitlab.com/ee/", ""};
	private static final String jsonName = "trainingSetLinks.json";
	private static final String trainingDataName = "trainingSet.csv";
	private static final String testingDataName = "testingSet.csv";
	private static final String mlModelName = "ml.model";
	
	private static int counter = 0;
	private static final int maxCount = 200;
	
	private static Remove removeFilter;
	
	//TODO modifiy training set, change false to have correct phrase but bad links
	//TODO predict document contents, find match utilizing Word2Vec
    public static void main(String[] args) {
    	if (operation.toLowerCase().equals("crawl")) {
    		System.out.printf("%-6d| %-72s| %-128s| %-128s|", 0, "Title", "URL", "SearchUrl");
    		System.out.println();
    		crawlPage(crawlStartPage);
	    	
    		JSONObject savedPagesJSON = new JSONObject();
    	    
    	    int count = 0;
    	    for (String[] searchLinkPair : Constants.getSavedLinks()) {
        	    JSONArray links = new JSONArray();
        	    links.add(searchLinkPair[0]);
        	    links.add(searchLinkPair[1]);
        	    links.add(searchLinkPair[2]);
        	    links.add(true);
        	    
    	    	savedPagesJSON.put(count, links);
    	    	
    	    	count++;
    	    }
    	    
    	    try {
				Files.write(Paths.get(jsonName), savedPagesJSON.toJSONString().getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
    	else if (operation.toLowerCase().equals("create train")) createDataSet();
    	else if (operation.toLowerCase().equals("create test")) createTestSet();
    	else if (operation.toLowerCase().equals("match")) matchTests();
    	else if (operation.toLowerCase().equals("train model")) train();
    	else if (operation.toLowerCase().equals("test model")) testModel();
    	else if (operation.toLowerCase().equals("summarize")) {
    		Page page = new Page(Constants.stem, "/ee/user/project/clusters/index.html");
    		
    		ArrayList<String> combinedStems = new ArrayList<String>();
    		
    		for (int i = 0; i < page.getStems().size(); i++) {
    			for (int j = 0; j < page.getStems().get(i).size(); j++) {
        			combinedStems.add(page.getStems().get(i).get(j));
    			}
    		}
    		
    		DocumentExtraction extractor = new DocumentExtraction(combinedStems, page.getSections());
    		extractor.getSummary();
    	}
    }
    
    private static void train() {
		try {
			System.out.println("\nTRAINING DATA SET:");
			System.out.println("------------------");
			CSVLoader source = new CSVLoader();
			source.setSource(new File(trainingDataName));
	    	Instances data = source.getDataSet();
	    	data.setClassIndex(data.numAttributes()-1);
	    	
	    	removeFilter = new Remove();
	    	removeFilter.setAttributeIndicesArray(new int[] {0, 1, 2, data.numAttributes()-1});
	    	removeFilter.setInvertSelection(true);
	    	removeFilter.setInputFormat(data);
	    	data = Filter.useFilter(data, removeFilter);
	    	
	    	InfoGainAttributeEval attrEval = new InfoGainAttributeEval();
	    	attrEval.buildEvaluator(data);
	    	
	    	for (int i = 0; i < data.numAttributes(); i++) {
	    	    Attribute attr = data.attribute(i);
	    	    double score  = attrEval.evaluateAttribute(i);
	    	    
	    	    System.out.println(attr.name() + ": " + score);
	    	}
	    	
			LibSVM svm = new LibSVM();
			svm.buildClassifier(data);
			weka.core.SerializationHelper.write(mlModelName, svm);
			
			Evaluation eval = new Evaluation(data);
			eval.crossValidateModel(svm, data, 10, new Random(1));
			System.out.println("Correct: " + eval.pctCorrect() + "%");
			System.out.println("------------------");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
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
	    	
	    	for (int i = 0; i < data.numAttributes(); i++) {
	    	    Attribute attr = data.attribute(i);
	    	    double score  = attrEval.evaluateAttribute(i);
	    	    
	    	    System.out.println(attr.name() + ": " + score);
	    	}
	    	
	    	LibSVM svm = (LibSVM) weka.core.SerializationHelper.read(mlModelName);
	    	Evaluation eval = new Evaluation(data);
			eval.crossValidateModel(svm, data, 10, new Random(1));
			System.out.println("Correct: " + eval.pctCorrect() + "%");
			System.out.println("------------------");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
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
    		if (!Constants.getSavedLinks().contains(linkPair)) {
    			Constants.addSavedLink(linkPair);
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
    
    private static void createDataSet() {
    	ArrayList<String[]> testLinks = new ArrayList<String[]>();
    	ArrayList<Boolean> testLinksCheck = new ArrayList<Boolean>();
    	
		try {
			FileReader reader = new FileReader(jsonName);
	        JSONParser jsonParser = new JSONParser();
			
	        JSONObject obj = ((JSONObject)jsonParser.parse(reader));
	        
			for (int i = 0; i < obj.size(); i++) {
				JSONArray arr = (JSONArray)obj.get(i+"");
				
				testLinks.add(new String[] {(String)arr.get(0), (String)arr.get(1), (String)arr.get(2)});
				testLinksCheck.add((Boolean)arr.get(3));
			}
		} catch (IOException | ParseException e1) {
			e1.printStackTrace();
		}
    	
    	try {
    		FileWriter writer = new FileWriter(trainingDataName);
    		writer.append("Proximity,");
    		writer.append("WordCount,");
    		writer.append("TitleMatch,");
    		writer.append("Reliance,");
    		writer.append("Score,");
    		writer.append("Match\n");
			
			int i = 0;
	        for (String[] linkPair : testLinks) {
	        	NaiveAlgorithm naive = new NaiveAlgorithm(linkPair, Constants.stem);
	        	naive.calculateMatch();
	        	
	        	if (naive.getValid()) {
	        		writer.append(naive.getProximityFactorNoCurve() + ",");
	        		writer.append(naive.getWordCountNoCurve() + ",");
	        		writer.append(naive.getTitleMatch() + ",");
	        		writer.append(naive.getSingleWordReliance() + ",");
	        		writer.append(naive.getFinalScore() + ",");
	        		writer.append(testLinksCheck.get(i) + "\n");
	        	}
		        	
	        	System.out.println();
		        i++;
	        }
	        writer.flush();
	        writer.close();
	        
		} catch (IOException e1) {
			e1.printStackTrace();
		}
    }
    
    private static void matchTests() {
    	ArrayList<String[]> testLinks = new ArrayList<String[]>();
    	ArrayList<Boolean> testLinksCheck = new ArrayList<Boolean>();
    	
		try {
			FileReader reader = new FileReader(jsonName);
	        JSONParser jsonParser = new JSONParser();
			
	        JSONObject obj = ((JSONObject)jsonParser.parse(reader));
	        
			for (int i = 0; i < obj.size(); i++) {
				JSONArray arr = (JSONArray)obj.get(i+"");
				
				testLinks.add(new String[] {(String)arr.get(0), (String)arr.get(1), (String)arr.get(2)});
				testLinksCheck.add((Boolean)arr.get(3));
			}
		} catch (IOException | ParseException e1) {
			e1.printStackTrace();
		}
		
		verifyLinks(testLinks, testLinksCheck);
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
	    	testLinksArr.add(new String[] {"related documentation", "https://docs.gitlab.com/ee/user/analytics/value_stream_analytics.html#permissions", "Find the current permissions on the Value Stream Analytics dashboard, as described in related documentation"});
	    	testLinksCheck.add(true);
	    	testLinksArr.add(new String[] {"Through the API", "https://docs.gitlab.com/ee/api/users.html", "You can also create users through the API as an admin"});
	    	testLinksCheck.add(true);
	    	testLinksArr.add(new String[] {"Elasticsearch.log file", "https://docs.gitlab.com/ee/administration/logs.html#elasticsearchlog", ""});
	    	testLinksCheck.add(true);

	    	testLinksArr.add(new String[] {"Kubernetes Engine", "https://docs.gitlab.com/ee/user/project/clusters/gitlab_managed_clusters.html", ""});
	    	testLinksCheck.add(false);
	    	testLinksArr.add(new String[] {"Kubernetes Pipeline", "https://docs.gitlab.com/ee/user/clusters/management_project.html", ""});
	    	testLinksCheck.add(false);
	    	testLinksArr.add(new String[] {"find users' api tokens", "https://docs.gitlab.com/ee/user/project/settings/project_access_tokens.html", ""});
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
	    	testLinksArr.add(new String[] {"instance", "https://docs.gitlab.com/ee/user/instance/clusters/index.html", "On the group level, to use the same cluster across multiple projects within your group"});
	    	testLinksCheck.add(true);
	    	testLinksArr.add(new String[] {"group", "https://docs.gitlab.com/ee/user/group/clusters/index.html", "On the instance level, to use the same cluster across multiple groups and projects."});
	    	testLinksCheck.add(true);
	    	
	    	testLinksArr.add(new String[] {"developers", "https://docs.gitlab.com/ee/user/permissions.html", "cautionThe whole cluster security is based on a model where developers are trusted, so only trusted users should be allowed to control your clusters"});
	    	testLinksCheck.add(false);
	    	testLinksArr.add(new String[] {"dynamic names", "https://docs.gitlab.com/ee/ci/environments/index.html", ""});
	    	testLinksCheck.add(false);
	    	testLinksArr.add(new String[] {"dependency", "https://docs.gitlab.com/ee/user/packages/index.html", ""});
	    	testLinksCheck.add(false);
    	}
    	
    	try {
    		FileWriter writer = new FileWriter(testingDataName);
    		writer.append("Proximity,");
    		writer.append("WordCount,");
    		writer.append("TitleMatch,");
    		writer.append("Reliance,");
    		writer.append("Score,");
    		writer.append("Match\n");
			
			int i = 0;
	        for (String[] linkPair : testLinksArr) {
	        	NaiveAlgorithm naive = new NaiveAlgorithm(linkPair, Constants.stem);
	        	naive.calculateMatch();
	        	
	        	if (naive.getValid()) {
	        		writer.append(naive.getProximityFactorNoCurve() + ",");
	        		writer.append(naive.getWordCountNoCurve() + ",");
	        		writer.append(naive.getTitleMatch() + ",");
	        		writer.append(naive.getSingleWordReliance() + ",");
	        		writer.append(naive.getFinalScore() + ",");
	        		writer.append(testLinksCheck.get(i) + "\n");
	        	}
		        	
	        	System.out.println();
		        i++;
	        }
	        writer.flush();
	        writer.close();
	        
		} catch (IOException e1) {
			e1.printStackTrace();
		}
    	
//    	verifyLinks(testLinksArr, testLinksCheck);
    }
    
    private static void verifyLinks(ArrayList<String[]> links, ArrayList<Boolean> check) {
    	int matches = 0;
    	
    	double startTime = System.nanoTime();
    	int i = 0;
        for (String[] linkPair : links) {
        	NaiveAlgorithm naive = new NaiveAlgorithm(linkPair, Constants.stem);
        	naive.calculateMatch();
        	
        	double match = naive.getFinalScore();
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