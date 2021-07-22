package EEProject.WikiLinks;

import java.util.ArrayList;

public class Page {
	private String title;
	private ArrayList<String> sections;
	private ArrayList<ArrayList<String>> lemma;
	private ArrayList<String[]> links;
	
	public Page() {}
	
	public Page(String title, ArrayList<String> sections, ArrayList<ArrayList<String>> lemma, ArrayList<String[]> links) {
		this.title = title;
		this.sections = sections;
		this.lemma = lemma;
		this.links = links;
	}
	
	public String getTitle() {
		return this.title;
	}
	
	public ArrayList<String> getSections() {
		return this.sections;
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
		
		return output;
	}
}