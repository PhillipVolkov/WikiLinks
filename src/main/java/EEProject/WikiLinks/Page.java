package EEProject.WikiLinks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.tartarus.snowball.ext.PorterStemmer;

public class Page {
	private String title;
	private ArrayList<String> sections;
	private ArrayList<ArrayList<String>> stems;
	private ArrayList<String[]> links;
	private Hashtable<String, Integer> permaLinks;
	private boolean failed;
	
	public Page(String stem, String pages) {
		readGitLabPage(stem, pages);
	}
	
	public boolean getFailed() {
		return this.failed;
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
	
	//parse the page into the object
    public void readGitLabPage(String stem, String pages) {
    	this.failed = true;
    	
    	try {
            URL url = new URL(stem + pages);
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            
            String title = "";
            ArrayList<String> sections = new ArrayList<String>();
            ArrayList<ArrayList<String>> stems = new ArrayList<ArrayList<String>>();
            ArrayList<String[]> links = new ArrayList<String[]>();
            Hashtable<String, Integer> permaLinks = new Hashtable<String, Integer>();
            
            String currLine = "";
            String output = "";
    		String linkContext = "";
            boolean mainOpen = false;
            boolean articleContent = false;
            int divsOpen = 0;

    		boolean addSection = false;
    		ArrayList<Integer[]> noContext = new ArrayList<Integer[]>();
    		
            while ((currLine = reader.readLine()) != null) {
            	currLine = currLine.toString();
            	
            	//closers
            	if (currLine.equals("</main>")) {
            		mainOpen = false;
            	}
            	else if (articleContent) {
            		divsOpen -= Constants.countMatches(currLine, "</div");
            		
            		if (divsOpen == 0) {
            			articleContent = false;
            		}
            	}
            	
            	//parsing
            	if (mainOpen && articleContent) {
            		
            		boolean singleBracketOpen = false;
            		int bracketPrevOpen = -2;
            		int bracketPrevClosed = -2;
            		int prevHeading = -2;
            		int prevPeriod = -2;
            		int prevL = -2;
            		int prevU = -2;
            		boolean linkBracketOpen = false;
            		boolean linkUrlParse = false;
            		boolean linkSentence = false;
            		boolean endBracketOpen = false;
            		boolean ignoreCurrChar = false;
            		boolean ignoreBracketContents = false;
            		boolean lineTitle = false;
            		boolean escapeSequence = false;
            		String newLine = "";
            		String internalText = "";
            		String linkURL = "";
            		String linkText = "";
            		String escapeCharacters = "";
            		
            		for (int i = 0; i < currLine.length(); i++) {
            			//detect parses
            			if (currLine.charAt(i) == '<') {
            				singleBracketOpen = true;
            				bracketPrevOpen = i;
            				ignoreCurrChar = true;
            			}
            			else if (currLine.charAt(i) == '/' && bracketPrevOpen == i-1) {
            				singleBracketOpen = false;
            				bracketPrevClosed = i;
            				endBracketOpen = true;
            				ignoreCurrChar = true;
            			}
            			else if (currLine.charAt(i) == 'u' && (bracketPrevOpen == i-1 || bracketPrevClosed == i-1)) {
            				prevU = i;
            			}
            			else if (currLine.charAt(i) == 'h' && (bracketPrevOpen == i-1 || bracketPrevClosed == i-1)) {
            				prevHeading = i;
            			}
            			else if (prevHeading == i-1 && bracketPrevOpen == i-2) {
            				if (currLine.charAt(i) == '1') {
            					lineTitle = true;
            				}
            				
            				if (addSection) {
            					sections.add(output);
            					addSection = false;
            				}
            				
            				output = "";
        					addSection = true;
            			}
            			else if (currLine.charAt(i) == 'a' && bracketPrevOpen == i-1) {
            				singleBracketOpen = false;
            				linkBracketOpen = true;
            				linkUrlParse = true;
            				ignoreCurrChar = true;
            				linkSentence = true;
            			}
            			else if (currLine.charAt(i) == 'a' && bracketPrevClosed == i-1) {
            				linkBracketOpen = false;
            			}
            			else if (currLine.charAt(i) == '>') {
            				if (singleBracketOpen) singleBracketOpen = false;
            				else if (linkUrlParse) linkUrlParse = false;
            				else if (endBracketOpen) endBracketOpen = false;
            				
        					ignoreCurrChar = true;
            			}
            			else if (currLine.charAt(i) == '&') {
            				escapeSequence = true;
            				escapeCharacters = "";
            			}
            			else if (currLine.charAt(i) == ';' && escapeSequence) {
            				escapeSequence = false;
            				ignoreCurrChar = true;
            			}
            			
            			//System.out.print(currLine.charAt(i) + " " + singleBracketOpen);
            			
            			//write currentChar
            			if (!endBracketOpen && !ignoreCurrChar) {
            				if (escapeSequence) escapeCharacters += currLine.charAt(i);
            				
            				else if (singleBracketOpen) internalText += currLine.charAt(i);
            				
            				else if (linkUrlParse) linkURL += currLine.charAt(i);
            				
            				else if (linkBracketOpen) linkText += currLine.charAt(i);
            				
            				else newLine += currLine.charAt(i);
            			}
            			
            			//handle escape sequences
            			if (!escapeCharacters.equals("") && !escapeSequence) {
        					if (escapeCharacters.equals("&lt")) newLine += "<";
        					else if (escapeCharacters.equals("&gt")) newLine += ">";
        					else newLine += escapeCharacters + ";";
        					
        					escapeCharacters = "";
            			}
            			
            			//handle internal text
            			if (!internalText.equals("")) {
            				if (internalText.length() <= Constants.largestLength) {
            					if (Constants.contains(Constants.ignoreTokens, internalText)) {
            						ignoreBracketContents = true;
            					}
            				}
            				
            				if (!singleBracketOpen) {
	    						internalText = "";
	    						ignoreBracketContents = false;
            				}
    					}
            			
            			//handle link text
            			if (!linkURL.equals("")) {
            				if (!linkBracketOpen) {
            					Scanner scan = new Scanner(linkURL);
            					boolean permaLink = false;
            					String link = "";
            					
            					boolean linkCreated = false;
            					
            					if (pages.contains("#")) pages = pages.split("#")[0];
            					
            					//find link
            					while (scan.hasNext()) {
            						String currStr = scan.next();
            						
            						String[] currTitle = currStr.split("title=");
            						String[] href = currStr.split("href=");
            						
            						if (currTitle.length != 1) {
            							if (currTitle[1].equals("Permalink")) {
            								permaLink = true;
            							}
            						}
            						if (href.length != 1) {
            							link = href[1];
            						}
            					}
            					
            					if (link.length() != 0 && !link.contains("#fnref:")) {
	            					if (link.charAt(0) == '"' && link.charAt(link.length()-1) == '"') {
	            						link = link.split("\"")[1];
	            					}
            					
	            					//convert "../" into full link
	            					String[] splitLink = link.split(Pattern.quote("../"));
	            					int count = splitLink.length;
	            					int offset = 0;
	            					
	            					String[] splitLinkSlash = pages.split(Pattern.quote("/"), -1);
	            					if (!splitLinkSlash[splitLinkSlash.length-1].contains(".")) offset = 1;
	            					
	            					if (count != 1) {
		            					String[] pageSlashes = pages.split("/");
		            					String fullLink = stem;
		            					
		            					for (int j = 0; j < pageSlashes.length-count+offset; j++) {
		            						if (j != 0) fullLink += "/" + pageSlashes[j];
		            						else fullLink += pageSlashes[j];
		            					}
		            					
		            					fullLink += "/" + splitLink[splitLink.length-1];
		            					
		            					link = fullLink;
	            					}
	            					else {
	            						boolean canSearch = false;
	            						
	            						if (link.length() > "https://".length()) {
		                					if (!link.substring(0, "https://".length()).equals("https://") && !link.substring(0, "http://".length()).equals("http://") && !link.substring(0, "www.".length()).equals("www.")) canSearch = true;
	                					}
	            						else canSearch = true;
	            						 
	            						if (canSearch) {
		            						if (link.charAt(0) == '#') {
		            							link = stem + pages + link;
		            						}
		            						else if (link.charAt(0) == '/') {
		    	            					link = stem + link;
		            						}
		            						else {
		                						String[] pageSlashes = pages.split("/");
		    	            					String fullLink = stem;
		    	            					
		    	            					for (int j = 0; j < pageSlashes.length-1+offset; j++) {
		    	            						if (j != 0) fullLink += "/" + pageSlashes[j];
		    	            						else fullLink += pageSlashes[j];
		    	            					}
		    	            					
		    	            					fullLink += "/" + link;
		    	            					
//			    	            				System.out.println(link + "\t-->\t" + fullLink);
		    	            					
		    	            					link = fullLink;
		            						}
	            						}
	        						}
	            					
	            					//append final text
		            				if (!permaLink && !linkText.equals("")) {
		            					newLine += linkText;
			            				links.add(new String[] {linkText, link, ""});
			            				
			            				if (addSection) {
			            					noContext.add(new Integer[] {sections.size(), output.length(), links.size()-1});
			            				}
		            				}
		            				else if (permaLink) {
		            					permaLinks.put(link.split("#")[1], sections.size());
		            				}
		            				
		            				linkURL = "";
		            				linkText = "";
		            				
		            				linkCreated = true;
	            					
	//            					if (!linkCreated) {
	//            						if (!permaLink && !linkText.equals("")) {
	//		            					newLine += linkText;
	//            							links.add(new String[] {linkText, linkURL, ""});
	//            						}
	//            						else if (permaLink) {
	//		            					permaLinks.put(link.split("#")[1], sections.size());
	//		            				}
	//            					}
            					}
            				}
            			}
            			
        				ignoreCurrChar = false;
            		}
        			
            		//System.out.println(currLine + "|");
            		if (!newLine.equals("")) {
            			output += newLine + "\n";
            		}
            		
            		if (lineTitle) title = newLine;
            	}
            	
            	//openers
            	if (currLine.equals("<main>") || currLine.equals("<div class=\"main class pl-lg-4\">")) {
            		mainOpen = true;
            	}
            	else if (currLine.equals("<div class=\"article-content js-article-content\" role=main itemscope itemprop=mainContentOfPage>")) {
            		articleContent = true;
            		divsOpen = 1;
            	}
            	else if (articleContent) {
            		divsOpen += Constants.countMatches(currLine, "<div");
            	}
            }
            //add trailing section
            if (addSection) {
            	sections.add(output);
            }
            
            reader.close();
            
            //context
            for (int i = 0; i < noContext.size(); i++) {
            	Integer[] loc = noContext.get(i);
            	
            	if (loc[0] != -1) {
            		loc[1] = loc[1] + sections.get(loc[0]).substring(loc[1]).indexOf(links.get(loc[2])[0]);
            		
            		//find start
            		int sentenceStart = sections.get(loc[0]).substring(0, loc[1]+1).lastIndexOf(".\n")+1;
            		if (sentenceStart == 0) sentenceStart = sections.get(loc[0]).substring(0, loc[1]+1).lastIndexOf("\n")+1;
            		
            		int temp = sections.get(loc[0]).substring(0, loc[1]+1).lastIndexOf(". ")+1;
            		if (temp > sentenceStart) sentenceStart = temp;
            		
            		//find end
            		int sentenceEnd = loc[1] + sections.get(loc[0]).substring(loc[1]).indexOf(".\n");
            		
            		temp = loc[1] + sections.get(loc[0]).substring(loc[1]).indexOf(". ");
            		if (temp < sentenceEnd && temp >= loc[1]) sentenceEnd = temp;
            		
            		
            		String context = "";
            		
            		if (sentenceEnd >= loc[1]) context = sections.get(loc[0]).substring(sentenceStart, sentenceEnd);
            		else context = sections.get(loc[0]).substring(sentenceStart);
            		
            		context = context.strip();
            		context = String.join(" ", context.split("\n"));
            		
            		links.get(loc[2])[2] = context;
            	}
            }
            
            //stemming
            for (String section : sections) {
    	    	ArrayList<String> tokens = new ArrayList<String>();
    	    	
    		    PorterStemmer stemmer = new PorterStemmer();
    		    
    		    String[] tokenized = section.toLowerCase().split(Constants.stemmerDelims);
    		    stemmer = new PorterStemmer();
    		    
    		    for (String token : tokenized) {
    		    	token = token.toLowerCase().strip();
    		    	token = String.join("", token.split("\n"));
    		    	
    		    	stemmer.setCurrent(token);
    		    	stemmer.stem();
    		    	token = stemmer.getCurrent();
    		    	
    		    	for (String removedChar : Constants.removedChars) {
    		    		if (token.indexOf(removedChar) != -1) {
    		    			token = token.replace(removedChar, "");
    		    		}
    		    	}
    		    	
    		    	if (!Constants.contains(Constants.stopWords, token)) {
    		    		tokens.add(token);
    		    	}
    		    }

    		    
    	    	stems.add(tokens);
            }

            this.title = title;
            this.sections = sections;
            this.stems = stems;
            this.links = links;
            this.permaLinks = permaLinks;
            
            this.failed = false;
        } 
        catch (IOException e) {
            System.out.println(e.getStackTrace());
        }
    }
}