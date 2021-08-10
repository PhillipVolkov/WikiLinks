package EEProject.WikiLinks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class DocumentExtraction {
	private ArrayList<String> stems;
	private ArrayList<String> sentences;
	
	public DocumentExtraction(ArrayList<String> stems, ArrayList<String> sentences) {
		this.stems = stems;
		this.sentences = sentences;
		
		parseSentences();
	}
	
	private void parseSentences() {
		ArrayList<String> sections = sentences;
		this.sentences = new ArrayList<String>();
	
		for (String section : sections) {
			String[] whiteSpaceSplit = section.split("\n");
			whiteSpaceSplit[0] += ".";
			section = String.join(" ", whiteSpaceSplit);
			section = section.toLowerCase().strip();
			
			String[] tokenized = section.split(Constants.sentenceDelims);
			
			for (String token : tokenized) this.sentences.add(token);
		}
	}
	
	public void getSummary() {
		Hashtable<String, Integer> occurences = new Hashtable<String, Integer>();
		
		for (String stem : stems) {
			if (occurences.containsKey(stem)) occurences.put(stem, occurences.get(stem)+1);
			else occurences.put(stem, 1);
		}
		
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
		
		int average = (int)Math.round((double)total/occurences.size());
		
		for (int i = 0; i < sortedStems.size(); i++) {
			if (occurences.get(sortedStems.get(i)) <= average) {
			}
		}
		
		TreeMap<Integer, String> sentenceScores = new TreeMap<Integer, String>(Collections.reverseOrder());
		for (String sentence : sentences) {
			String[] tokenized = sentence.toLowerCase().split(Constants.stemmerDelims);
			int score = 0;
			
			for (String token : tokenized) {
				if (sortedStems.contains(token)) {
					score += occurences.get(token);
				}
			}
			
			sentenceScores.put(score, sentence);
		}
		
		Set entrySet = sentenceScores.entrySet();
		Iterator i = entrySet.iterator();
		
		String summary = "";
		
		int count = 0;
		int maxCount = Math.round(sentences.size()/3);
		while (i.hasNext()) {
			count++;
			
			Map.Entry<Integer, String> entry = (Map.Entry<Integer, String>) i.next();
			summary += entry.getValue() + "\n";
			
			if (count >= maxCount) break;
		}
		
		System.out.println(summary);
	}
}