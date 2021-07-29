package EEProject.WikiLinks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Main {
	private static final String operation = "crawl";
	private static final String[] crawlStartPage = new String[] {"Gitlab Docs", "https://docs.gitlab.com/ee/", ""};
	
	//TODO output JSON, read saved pages
    public static void main(String[] args) {
    	if (operation.toLowerCase().equals("crawl")) {
//    		System.out.println(new Page(Constants.stem, "https://docs.gitlab.com/13.12/ee/user/project/deploy_boards.html"
//    				.substring(Constants.stem.length(), "https://docs.gitlab.com/13.12/ee/user/project/deploy_boards.html".length())));
//    		
    		System.out.printf("%-72s| %-128s| %-128s|", "Title", "URL", "SearchUrl");
    		System.out.println();
    		crawlPage(crawlStartPage);
//    		Page searchPage = new Page(Constants.stem, crawlStartPage.substring(Constants.stem.length(), crawlStartPage.length()));
//	    	Constants.getSavedPages().put(crawlStartPage, searchPage);
//	    	
//	    	String[] linkPair = new String[] {"Gitlab Docs", crawlStartPage, ""};
//	    	if (!Constants.getSavedLinks().contains(linkPair)) {
//    			Constants.addSavedLink(linkPair);
    		}
	    	
    		JSONObject savedPagesJSON = new JSONObject();
    	    JSONArray pages = new JSONArray();
    	    
    	    for (String[] searchLinkPair : Constants.getSavedLinks()) {
        	    JSONArray links = new JSONArray();
        	    links.add(searchLinkPair[1]);
        	    links.add(searchLinkPair[2]);
        	    
    	    	savedPagesJSON.put(searchLinkPair[0] + ": ", links);
    	    }
    	    
//    		for (String key : Constants.getSavedPages().keySet()) {
//        	    JSONArray page = new JSONArray();
//        	    Page currPage = Constants.getSavedPages().get(key);
//        	    
//        	    page.add(currPage.getTitle());
//        	    page.add(currPage.getSections());
//        	    page.add(currPage.getStems());
//        	    
//        	    for (String[] searchLinkPair : currPage.getLinks()) {
//            	    JSONArray links = new JSONArray();
//            	    links.add(searchLinkPair[0]);
//            	    links.add(searchLinkPair[1]);
//            	    links.add(searchLinkPair[2]);
//            	    page.add(links);
//        	    }
//        	    
//        	    
//        	    page.add(currPage.getPermaLinks());
//        	    page.add(currPage.getFailed());
//        	    
//        	    pages.add(page);
//    		}
//
//    	    savedPagesJSON.put("Saved Pages:", pages);
    	    
    	    try {
				Files.write(Paths.get("savedPages.json"), savedPagesJSON.toJSONString().getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
    	else if (operation.toLowerCase().equals("test")) performTests();
    }
    
    //recursion to crawl through all links starting with initial page
    private static void crawlPage(String[] linkPair) {
    	String searchUrl = linkPair[1].split("#")[0];
    	
    	Page searchPage;
    	if (!Constants.getSavedPages().contains(searchUrl)) {
    		searchPage = new Page(Constants.stem, linkPair[1].substring(Constants.stem.length(), linkPair[1].length()));
	    	Constants.getSavedPages().put(searchUrl, searchPage);
    	}
    	else {
    		searchPage = Constants.getSavedPages().get(searchUrl);
    	}
    	
    	System.out.printf("%-72s| %-128s| %-128s|", searchPage.getTitle(), linkPair[1], searchUrl);
		System.out.println();
	    
    	if (!searchPage.getFailed()) {
    		if (!Constants.getSavedLinks().contains(linkPair)) {
    			Constants.addSavedLink(linkPair);
    		}
    		
	    	for (String[] searchLinkPair : searchPage.getLinks()) {
				String linkSearchUrl = linkPair[1].split("#")[0];
				
				//check saved dictionary by url
				if (searchLinkPair[1].length() >= Constants.stem.length()) {
			    	if (searchLinkPair[1].substring(0, Constants.stem.length()).equals(Constants.stem) && !Constants.getSavedPages().containsKey(linkSearchUrl)) {
			    		crawlPage(searchLinkPair);
			    	}
		    	}
	    	}
    	}
    }
    
    private static void performTests() {
    	if (!Constants.debugPrint) {
			System.out.printf("%-72s| %-128s| %-12s| %-12s| %-22s| %-1s", "Tokens", "URL", "Parse Time", "Algo Time", "Score", "Match?");
			System.out.println();
		}

    	ArrayList<String[]> testLinks;
		
    	if (!Constants.testLinks) {
	    	double startTime1 = System.nanoTime();
	        Page newPage = new Page(Constants.stem, Constants.page);
	        Constants.putSavedPage(Constants.stem+Constants.page, newPage);
	        
	    	double endTime1 = System.nanoTime();
	    	
			System.out.printf("%-72s| %-128s| %-12s| %-12s| %-22s| %-1s", "Initial Page", Constants.stem+Constants.page, ((endTime1-startTime1)/Math.pow(10, 9)), "N/A", "N/A", "N/A");
			System.out.println();

	    	testLinks = newPage.getLinks();
    	}
    	else {
    		testLinks = new ArrayList<String[]>();
    		
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
    	}
    	
    	verifyLinks(testLinks);
    }
    
    private static void verifyLinks(ArrayList<String[]> links) {
    	double startTime = System.nanoTime();
        for (String[] linkPair : links) {
        	double match = NaiveAlgorithm.getMatch(linkPair, Constants.stem);
        	if (match != -1 && !Constants.debugPrint) {
            	if (match >= Constants.threshold) System.out.printf("| true\n");
            	else System.out.printf("| false\n");
        	}
        }
    	double endTime = System.nanoTime();
    	
    	System.out.println("\nTime Elapsed: " + ((endTime-startTime)/Math.pow(10, 9)) + " seconds");
    }
}