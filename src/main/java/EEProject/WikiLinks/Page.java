package EEProject.WikiLinks;

import java.util.ArrayList;
import java.util.Hashtable;

public class Page {
	private String title;
	private ArrayList<String> sections;
	private ArrayList<ArrayList<String>> stems;
	private ArrayList<String[]> links;
	private Hashtable<String, Integer> permaLinks;
	
	public Page() {}
	
	public Page(String title, ArrayList<String> sections, ArrayList<ArrayList<String>> lemma, ArrayList<String[]> links, Hashtable<String, Integer> permaLinks) {
		this.title = title;
		this.sections = sections;
		this.stems = lemma;
		this.links = links;
		this.permaLinks = permaLinks;
	}
	
	public String getTitle() {
		return this.title;
	}
	
	public ArrayList<String> getSections() {
		return this.sections;
	}
	
	public Hashtable<String, Integer> getPermaLinks() {
		return this.permaLinks;
	}
	
	public ArrayList<String> getPermaLinkSection(String permaLink) {
		return this.stems.get(permaLinks.get(permaLink));
	}
	
	public String getPermaLinkString() {
		String output = "Permalinks:\n";
		for (String key : permaLinks.keySet()) {
			output += key + "\t -> \t" + permaLinks.get(key) + "\n";
		}
		
		return output;
	}
	
	public ArrayList<ArrayList<String>> getStems() {
		return this.stems;
	}
	
	public String getStemsString() {
		String output = "Lemma:\n";
		
		for (ArrayList<String> section : stems) {
			output += section + "\n";
		}
		
		return output;
	}
	
	public ArrayList<String[]> getLinks() {
		return this.links;
	}
	
	public void printLinks() {
		System.out.printf("%-48s -> %-110s (%-1s)", "linkText", "linkUrl", "linkContext");
		System.out.println();
		
		for (String[] linkPair : links) {
			System.out.printf("%-48s -> %-110s (%-1s)", linkPair[0], linkPair[1], linkPair[2]);
			System.out.println();
		}
	}
	
	@Override
	public String toString() {
		String output = "Title: " + title + "\n\n";
		
		output += "Contents:\n";
		for (String section : sections) {
			output += section + "\n";
		}
		
		output += "Lemma:\n";
		for (ArrayList<String> section : stems) {
			output += section + "\n";
		}
		
		output += "\nLinks:\n";
		for (String[] linkPair : links) {
			output += String.format("%-48s -> %-110s (%-1s)", linkPair[0], linkPair[1], linkPair[2]) + "\n";
		}
		
		output += "\nPermalinks:\n";
		for (String key : permaLinks.keySet()) {
			output += key + "\t -> \t" + permaLinks.get(key) + "\n";
		}
		
		return output;
	}
}