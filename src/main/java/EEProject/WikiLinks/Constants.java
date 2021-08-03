package EEProject.WikiLinks;

import java.util.ArrayList;
import java.util.Hashtable;

public class Constants {
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
	
	static final boolean debugPrint = true;
	static final double threshold = 15;
	static final int maxTokenSeperation = 20;
	static final int maxTokenQuantity = 3;
	static final int wordCountMultiplier = 115;
	static final int occurenceDifferencePenalty = 75;
	static final double titleFactorWeight = 0.4;
	
	static final String stemmerDelims = " |\n|:|, |\\. |\\.\n|-";

	static final String stem = "https://docs.gitlab.com";
	static final String page = "/ee/user/project/clusters/index.html";
	
	private static Hashtable<String, Page> savedPages = new Hashtable<String, Page>();
	private static ArrayList<String[]> savedLinks = new ArrayList<String[]>();
	
	public static Hashtable<String, Page> getSavedPages() {
		return savedPages;
	}
	
	public static void putSavedPage(String link, Page page) {
		savedPages.put(link, page);
	}
	
	public static ArrayList<String[]> getSavedLinks() {
		return savedLinks;
	}
	
	public static void addSavedLink(String[] link) {
		savedLinks.add(link);
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