package EEProject.WikiLinks;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Hello world!
 *
 */
public class App {
	static final String[] ignoreTokens = new String[] {"li", "ul", "div", "button"};
	static final int largestLength = "button".length();
	
    public static void main(String[] args) {
        Page newPage = readGitLabPage("https://docs.gitlab.com", "/ee/user/project/clusters/");
        
        System.out.println(newPage);
    }
    
    public static Page readGitLabPage(String stem, String pages) {
    	try {
            URL url = new URL(stem + pages);

            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            
            String title = "";
            ArrayList<String> sections = new ArrayList<String>();
            ArrayList<String[]> links = new ArrayList<String[]>();
            
            String currLine = "";
            String output = "";
            boolean mainOpen = false;
            boolean articleContent = false;
            int divsOpen = 0;

    		boolean addSection = false;
            while ((currLine = reader.readLine()) != null) {
            	currLine = currLine.toString();
            	
            	//closers
            	if (currLine.equals("</main>")) {
            		mainOpen = false;
            	}
            	else if (articleContent) {
            		divsOpen -= countMatches(currLine, "</div");
            		
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
            		boolean linkBracketOpen = false;
            		boolean linkUrlParse = false;
            		boolean endBracketOpen = false;
            		boolean ignoreCurrChar = false;
            		boolean ignoreBracketContents = false;
            		boolean lineTitle = false;
            		String newLine = "";
            		String internalText = "";
            		String linkURL = "";
            		String linkText = "";
            		
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
            			
            			//System.out.print(currLine.charAt(i) + " " + singleBracketOpen);
            			
            			//write currentChar
            			if (!endBracketOpen && !ignoreCurrChar) {
            				if (singleBracketOpen) internalText += currLine.charAt(i);
            				
            				else if (linkUrlParse) linkURL += currLine.charAt(i);
            				
            				else if (linkBracketOpen) linkText += currLine.charAt(i);
            				
            				else newLine += currLine.charAt(i);
            			}
            			
            			//handle internal text
            			if (!internalText.equals("")) {
            				if (internalText.length() <= largestLength) {
            					if (contains(ignoreTokens, internalText)) {
            						ignoreBracketContents = true;
            					}
            				}
            				
            				if (!singleBracketOpen) {
	            				//if (!contains(ignoreTokens, internalText) && !ignoreBracketContents) newLine += "<" + internalText + ">";
	    						internalText = "";
	    						ignoreBracketContents = false;
            				}
    					}
            			
            			//handle link text
            			if (!linkURL.equals("")) {
            				if (!linkBracketOpen) {
            					Scanner scan = new Scanner(linkURL);
            					boolean ignore = false;
            					String link = "";
            					
            					//find link
            					while (scan.hasNext()) {
            						String currStr = scan.next();
            						
            						String[] currTitle = currStr.split("title=");
            						String[] href = currStr.split("href=");
            						
            						if (currTitle.length != 1) {
            							if (currTitle[1].equals("Permalink")) ignore = true;
            						}
            						else if (href.length != 1) {
            							link = href[1];
            						}
            					}
            					
            					//convert "../" into full link
            					String[] splitLink = link.split(Pattern.quote("../"));
            					int count = splitLink.length;
            					
            					if (count != 1) {
	            					String[] pageSlashes = pages.split("/");
	            					String fullLink = stem;
	            					
	            					for (int j = 0; j < pageSlashes.length-count+1; j++) {
	            						if (j != 0) fullLink += "/" + pageSlashes[j];
	            						else fullLink += pageSlashes[j];
	            					}
	            					
	            					fullLink += "/" + splitLink[splitLink.length-1];
	            					
	            					link = fullLink;
            					}
            					else {
                					if (!link.substring(0, "https://".length()).equals("https://")) {
                						String[] pageSlashes = pages.split("/");
    	            					String fullLink = stem;
    	            					
    	            					for (int j = 0; j < pageSlashes.length; j++) {
    	            						if (j != 0) fullLink += "/" + pageSlashes[j];
    	            						else fullLink += pageSlashes[j];
    	            					}
    	            					
    	            					fullLink += "/" + link;
    	            					
    	            					link = fullLink;
                					}
            					}
            					
            					//append final text
	            				if (!ignore && !linkText.equals("")) {
	            					newLine += linkText;
		            				links.add(new String[] {linkText, link});
	            				}
	            				
	            				linkURL = "";
	            				linkText = "";
            				}
            			}
            			
        				ignoreCurrChar = false;
            		}
            		
            		if (!newLine.equals("")) output += newLine + "\n";
            		if (lineTitle) title = newLine;
            	}
            	
            	//openers
            	if (currLine.equals("<main>")) {
            		mainOpen = true;
            	}
            	else if (currLine.equals("<div class=\"article-content js-article-content\" role=main itemscope itemprop=mainContentOfPage>")) {
            		articleContent = true;
            		divsOpen = 1;
            	}
            	else if (articleContent) {
            		divsOpen += countMatches(currLine, "<div");
            	}
            }
            //add trailing section
            if (addSection) {
            	sections.add(output);
            }
            
            reader.close();

        	return new Page(title, sections, links);
        } 
        catch (Exception e) {
            System.out.println(e);
        }
    	
    	return null;
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
