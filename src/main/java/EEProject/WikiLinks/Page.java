package EEProject.WikiLinks;

import java.util.ArrayList;

public class Page {
	private String title;
	private ArrayList<String> sections;
	private ArrayList<String[]> links;
	
	public Page() {}
	
	public Page(String title, ArrayList<String> sections, ArrayList<String[]> links) {
		this.title = title;
		this.sections = sections;
		this.links = links;
	}
	
	public String getTitle() {
		return this.title;
	}
	
	public ArrayList<String> getSections() {
		return this.sections;
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
		
		output += "Links:\n";
		for (String[] linkPair : links) {
			output += linkPair[0] + "\t -> \t" + linkPair[1] + "\n";
		}
		
		return output;
	}
}