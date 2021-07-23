package EEProject.WikiLinks;

import java.util.ArrayList;
import java.util.Hashtable;

public class Page {
	private String title;
	private ArrayList<String> sections;
	private ArrayList<ArrayList<String>> lemma;
	private ArrayList<String[]> links;
	private Hashtable<String, Integer> permaLinks;
	
	public Page() {}
	
	public Page(String title, ArrayList<String> sections, ArrayList<ArrayList<String>> lemma, ArrayList<String[]> links, Hashtable<String, Integer> permaLinks) {
		this.title = title;
		this.sections = sections;
		this.lemma = lemma;
		this.links = links;
		this.permaLinks = permaLinks;
	}
	
	public String getTitle() {
		return this.title;
	}
	
	public ArrayList<String> getSections() {
		return this.sections;
	}
	
	public ArrayList<String> getPermaLinkSection(String permaLink) {
		return this.lemma.get(permaLinks.get(permaLink));
	}
	
	public String getPermaLinkString() {
		String output = "Permalinks:\n";
		for (String key : permaLinks.keySet()) {
			output += key + "\t -> \t" + permaLinks.get(key) + "\n";
		}
		
		return output;
	}
	
	public ArrayList<ArrayList<String>> getLemma() {
		return this.lemma;
	}
	
	public String getLemmaString() {
		String output = "Lemma:\n";
		
		for (ArrayList<String> section : lemma) {
			output += section + "\n";
		}
		
		return output;
	}
	
	public ArrayList<String[]> getLinks() {
		return this.links;
	}
	
	@Override
	public String toString() {
		String output = "Title: " + title + "\n\n";
		
		output += "Contents:\n";
		for (String section : sections) {
			output += section + "\n";
		}
		
		output += "Lemma:\n";
		for (ArrayList<String> section : lemma) {
			output += section + "\n";
		}
		
		output += "\nLinks:\n";
		for (String[] linkPair : links) {
			output += linkPair[0] + "\t -> \t" + linkPair[1] + "\n";
		}
		
		output += "\nPermalinks:\n";
		for (String key : permaLinks.keySet()) {
			output += key + "\t -> \t" + permaLinks.get(key) + "\n";
		}
		
		return output;
	}
}