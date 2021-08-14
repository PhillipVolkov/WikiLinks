package EEProject.WikiLinks;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.tartarus.snowball.ext.PorterStemmer;

import com.opencsv.CSVReader;

import de.jungblut.distance.CosineDistance;
import de.jungblut.glove.GloveRandomAccessReader;
import de.jungblut.glove.impl.GloveBinaryRandomAccessReader;
import de.jungblut.glove.impl.GloveBinaryWriter;
import de.jungblut.glove.impl.GloveTextReader;
import de.jungblut.glove.util.StringVectorPair;
import de.jungblut.math.DoubleVector;

public class DocumentExtraction {
	private ArrayList<String> stems;
	private ArrayList<String> words;
	private ArrayList<String> sentences;

	private Hashtable<String, String> gloveConversions = new Hashtable<String, String>();
	
	public DocumentExtraction(ArrayList<String> stems, ArrayList<String> sentences) {
		this.stems = stems;
		this.sentences = sentences;
		
		parseSentences();
		
		gloveConversions.put("gitlab", "devops");
		gloveConversions.put("kubernetes", "cluster");
		gloveConversions.put("ci/cd", "pipeline");
		gloveConversions.put("knative", "serverless");
		gloveConversions.put("podlogs", "logs");
	}
	
	private void parseSentences() {
		ArrayList<String> sections = sentences;
		this.sentences = new ArrayList<String>();
		this.words = new ArrayList<String>();
	
		for (String section : sections) {
			String[] whiteSpaceSplit = section.split("\n");
			whiteSpaceSplit[0] = "";
			section = String.join(" ", whiteSpaceSplit);
			section = section.toLowerCase().strip();
			
			String[] tokenized = section.split(Constants.sentenceDelims);
			
			for (String token : tokenized) this.sentences.add(token);
		}
		
		for (String sentence : sentences) {
			String[] tokenized = sentence.toLowerCase().split(Constants.stemmerDelims);
		    
		    for (String token : tokenized) {
		    	token = token.toLowerCase().strip();
		    	token = String.join("", token.split("\n"));
		    	
		    	if (token.length() == 0) break;
		    	
		    	if (token.charAt(token.length()-1) == '.') {
	    			token = token.substring(0, token.length()-1);
		    	}
		    	
		    	for (String removedChar : Constants.removedChars) {
		    		if (token.indexOf(removedChar) != -1) {
		    			token = token.replace(removedChar, "");
		    		}
		    	}
		    	
		    	if (!token.equals("") && !Constants.contains(Constants.stopWords, token)) this.words.add(token);
		    }
		}
	}
	
	public void getTopTokens() {
		Hashtable<String, Integer> occurences = new Hashtable<String, Integer>();
		
		for (String word : stems) {
			if (occurences.containsKey(word)) occurences.put(word, occurences.get(word)+1);
			else occurences.put(word, 1);
		}

		//sort stems
		ArrayList<String> sortedStems = new ArrayList<String>();
		int total = 0;
		for (int i = 0; i < stems.size(); i++) {
			if (sortedStems.contains(stems.get(i))) continue;

			total += occurences.get(stems.get(i));
			
			if (sortedStems.size() == 0) {
				sortedStems.add(stems.get(i));
			}
			else {
				boolean appended = false;
				for (int j = 0; j < sortedStems.size(); j++) {
					if (occurences.get(stems.get(i)) > occurences.get(sortedStems.get(j))) {
						sortedStems.add(j, stems.get(i));
						appended = true;
						break;
					}
				}
				
				if (!appended) sortedStems.add(stems.get(i));
			}
		}
		
		int average = (int)Math.round((double)total/occurences.size())*2;
		int maxScore = occurences.get(sortedStems.get(0));
		
		Hashtable<String, Double> tfIdf = new Hashtable<String, Double>();
		ArrayList<String> unFoundStems = new ArrayList<String>();
		
		//add tf score
		for (int i = 0; i < sortedStems.size(); i++) {
			if (occurences.get(sortedStems.get(i)) < average) break;
			
			tfIdf.put(sortedStems.get(i), (double)occurences.get(sortedStems.get(i))/maxScore);
			
			if (Constants.getSavedIdf().containsKey(sortedStems.get(i))) tfIdf.replace(sortedStems.get(i), tfIdf.get(sortedStems.get(i))*(Constants.getSavedIdf().get(sortedStems.get(i))));
			else unFoundStems.add(sortedStems.get(i));
			
			System.out.println(sortedStems.get(i) + ": " + tfIdf.get(sortedStems.get(i)));
		}

		System.out.println();
		
		//multiply by idf score
		try (CSVReader reader = new CSVReader(new FileReader("wiki_tfidf_stems.csv"))) {
		      String[] row;
		      while ((row = reader.readNext()) != null && unFoundStems.size() > 0) {
		    	  if (unFoundStems.contains(row[0])) {
		    		  int index = sortedStems.indexOf(row[0]);
		    		  //System.out.println(index + "\t" + row[0] + "\t" + row[3] + "\t" + tfIdf.get(index)*Double.parseDouble(row[3]));
		    		  double idf = Double.parseDouble(row[3]);
		    		  tfIdf.replace(row[0], tfIdf.get(row[0])*idf);
		    		  unFoundStems.remove(row[0]);
		    		  
		    		  Constants.putSavedIdf(row[0], idf);
		    	  }
		      }
		}
		catch (Exception e) {e.printStackTrace();}
		
		//resort stems
		sortedStems = new ArrayList<String>();
		total = 0;
		for (int i = 0; i < stems.size(); i++) {
			if (sortedStems.contains(stems.get(i)) || !tfIdf.containsKey(stems.get(i))) continue;

			total += tfIdf.get(stems.get(i));
			
			if (sortedStems.size() == 0) {
				sortedStems.add(stems.get(i));
			}
			else {
				boolean appended = false;
				for (int j = 0; j < sortedStems.size(); j++) {
					if (tfIdf.get(stems.get(i)) > tfIdf.get(sortedStems.get(j))) {
						sortedStems.add(j, stems.get(i));
						appended = true;
						break;
					}
				}
				
				if (!appended) sortedStems.add(stems.get(i));
			}
		}

		//normalize scores
		double maxTfIdfScore = tfIdf.get(sortedStems.get(0));
		
		for (int i = 0; i < sortedStems.size(); i++) {
			tfIdf.replace(sortedStems.get(i), (double)tfIdf.get(sortedStems.get(i))/maxTfIdfScore);
			System.out.println(sortedStems.get(i) + ": " + tfIdf.get(sortedStems.get(i)));
		}
	}
}