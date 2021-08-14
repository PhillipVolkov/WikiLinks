package EEProject.WikiLinks;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.Hashtable;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import org.tartarus.snowball.ext.PorterStemmer;

import com.opencsv.CSVReader;

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
	
	

	static final String[] removedChars = new String[] {"'", "\"", "‘", "’", "“", "”", "(", ")", ",", ":", ";", "{", "}", "[", "]", "!", "?", "|", "<", ">", "$", "`", "~", "@", "*", "%", "^", "#"};
	
	static final boolean debugPrint = false;
	static final double threshold = 15;
	static final int maxTokenSeperation = 20;
	static final int maxTokenQuantity = 3;
	static final int wordCountMultiplier = 115;
	static final int occurenceDifferencePenalty = 75;
	static final double titleFactorWeight = 0.4;
	
	static final String stemmerDelims = " |\n|:|, |\\. |\\.\n|-";
	static final String sentenceDelims = "\\. |\\.\n";
	static final String splitChars = "\\-|\\+|&|\\=|_|\\\\";

	static final String stem = "https://docs.gitlab.com";
	static final String page = "/ee/user/project/clusters/index.html";
	
	static final WordVectors word2vecModel = WordVectorSerializer.readWord2VecModel("word2vec.bin");
	
	private static Hashtable<String, Page> savedPages = new Hashtable<String, Page>();
	private static Hashtable<String, Integer> savedTerms = new Hashtable<String, Integer>();
	private static ArrayList<ArrayList<String>> savedSentenceStems = new ArrayList<ArrayList<String>>();
	private static ArrayList<String[]> savedLinks = new ArrayList<String[]>();
	private static Hashtable<String, Double> savedIdf = new Hashtable<String, Double>();
	
	public static Hashtable<String, Page> getSavedPages() {
		return savedPages;
	}

	public static void putSavedPage(String link, Page page) {
		savedPages.put(link, page);
	}
	
	public static Hashtable<String, Integer> getSavedTerms() {
		return savedTerms;
	}
	
	public static void addSavedSentence(ArrayList<String> sentence) {
		savedSentenceStems.add(sentence);
	}
	
	public static ArrayList<ArrayList<String>> getSavedSentences() {
		return savedSentenceStems;
	}
	
	public static void addSavedTerm(String term) {
		savedTerms.put(term, 1);
	}

	public static void incrementSavedTerm(String term) {
		savedTerms.put(term, savedTerms.get(term)+1);
	}
	
	public static ArrayList<String[]> getSavedLinks() {
		return savedLinks;
	}
	
	public static void addSavedLink(String[] link) {
		savedLinks.add(link);
	}
	
	public static Hashtable<String, Double> getSavedIdf() {
		return savedIdf;
	}
	
	public static void putSavedIdf(String word, double idf) {
		savedIdf.put(word, idf);
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
    
    public static void initIdfDB() {
    	//multiply by idf score
		try (CSVReader reader = new CSVReader(new FileReader("idf.csv"))) {
		      String[] row;
		      reader.readNext();
		      while ((row = reader.readNext()) != null) {
		    	  try {
		    		  Constants.putSavedIdf(row[0], Double.parseDouble(row[3]));
		    	  }
		    	  catch (Exception e) {}
		      }
		}
		catch (Exception e) {e.printStackTrace();}
    }
    
    public static ArrayList<String> tokenize(ArrayList<String> stemsTokenized) {
    	ArrayList<String> output = new ArrayList<String>();
    	
	    PorterStemmer stemmer = new PorterStemmer();
	    
	    //add word tokens
	    for (int i = 0; i < stemsTokenized.size(); i++) {
	    	String stemToken = stemsTokenized.get(i);
	    	
	    	stemToken = stemToken.toLowerCase().strip();
	    	stemToken = String.join("", stemToken.split("\n"));
	    	
	    	//System.out.print(stemToken + "\t-->\t");
	    	
	    	for (String removedChar : Constants.removedChars) {
	    		while (stemToken.length() != 0 && stemToken.indexOf(removedChar) != -1) {
	    			stemToken = stemToken.replace(removedChar, "");
	    		}
	    	}
	    	
	    	while (stemToken.length() != 0 && stemToken.charAt(stemToken.length()-1) == '.') {
	    		stemToken = stemToken.substring(0, stemToken.length()-1);
	    	}
	    	
	    	if (stemToken.length() < 2) continue;
	    	
	    	//split by split chars
			String[] split = stemToken.split(Constants.splitChars);
			
			if (split.length != 0) {
    			stemToken = split[0];
    			
    			for (int j = 1; j < split.length; j++) {
    				stemsTokenized.add(i+1, split[j]);
    			}
			}
	    	
	    	stemToken = stemToken.toLowerCase().strip();
	    	
	    	//remove numbers
	    	try {
	    		double d = Double.parseDouble(stemToken);
	    		continue;
	    	}
	    	catch (Exception e) {}
	    	
	    	stemmer.setCurrent(stemToken);
	    	stemmer.stem();
	    	stemToken = stemmer.getCurrent();
	    	
	    	if (!Constants.contains(Constants.stopWords, stemToken)) {
	    		output.add(stemToken);
	    	}
	    	
	    	//System.out.println(stemToken);
	    }
	    
	    return output;
    }
}