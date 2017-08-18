/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ontoSentiment;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Map;
import twitter4j.Location;
import twitter4j.RateLimitStatus;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.OAuth2Token;
import twitter4j.conf.ConfigurationBuilder;

/**
 *
 * @author Raimundo
 */
public class Util {

    private static final String CONSUMER_KEY = "FBd5n7dyl8mCz73qyfZ0p4XHb";
    private static final String CONSUMER_SECRET = "vu5Xt5TzBSL9naOZylIYlx5MdcRlhH2LvkpW6KIkxSf9AqwuGt";
    public static final int TWEETS_PER_QUERY = 100;

    public static final String RATE_LIMIT_OPTION_SEARCH_TWEETS = "/search/tweets";
    public static final String RATE_LIMIT_OPTION_TRENDS_AVAIBLE = "/trends/available";

    private int MAX_QUERIES = 5;

    public static String cleanText(String text) {
        text = text.replace("\n", "\\n");
        text = text.replace("\t", "\\t");
        return text;
    }

    public static OAuth2Token getOAuth2Token() {
        OAuth2Token token = null;
        ConfigurationBuilder cb;
        cb = new ConfigurationBuilder();
        cb.setApplicationOnlyAuthEnabled(true);
        cb.setOAuthConsumerKey(CONSUMER_KEY).setOAuthConsumerSecret(CONSUMER_SECRET);
        try {
            token = new TwitterFactory(cb.build()).getInstance().getOAuth2Token();
        } catch (Exception e) {
            System.out.println("Could not get OAuth2 token");
            e.printStackTrace();
            System.exit(0);
        }
        return token;
    }

    public static Twitter getTwitter() {
        OAuth2Token token;
        token = getOAuth2Token();
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setApplicationOnlyAuthEnabled(true);
        cb.setOAuthConsumerKey(CONSUMER_KEY);
        cb.setOAuthConsumerSecret(CONSUMER_SECRET);
        cb.setOAuth2TokenType(token.getTokenType());
        cb.setOAuth2AccessToken(token.getAccessToken());
        return new TwitterFactory(cb.build()).getInstance();
    }

    public static void imprimeAvaibleTrends() throws TwitterException {
        ResponseList<Location> locations = getTwitter().getAvailableTrends();

        locations.stream().forEach((location) -> {
            System.out.println("Trend name: " + location.getName());
        });
    }

    public static Integer getTrendLocationId(String locationName, boolean imprimeTrendName) throws TwitterException {

        int idTrendLocation = 0;

        ResponseList<Location> locations;
        locations = getTwitter().getAvailableTrends();

        for (Location location : locations) {
            if (imprimeTrendName) {
                System.out.println("Trend name: " + location.getName());
            }
            if (location.getName().toLowerCase().equals(locationName.toLowerCase())) {
                idTrendLocation = location.getWoeid();
                break;
            }
        }

        if (idTrendLocation > 0) {
            return idTrendLocation;
        }

        return null;
    }

    public static void imprimirRateLimit(String tipo) throws TwitterException {
        Map<String, RateLimitStatus> rateLimitStatusSearch = getTwitter().getRateLimitStatus(tipo.split("/")[1]);
        RateLimitStatus searchTweetsRateLimit = rateLimitStatusSearch.get(tipo);
        System.out.printf("Ainda restam %d requisições de %d, O limite reseta em %d minutos\n", searchTweetsRateLimit.getRemaining(), searchTweetsRateLimit.getLimit(), (searchTweetsRateLimit.getSecondsUntilReset() / 60));
    }
    
    public static String sentimentParserString(int sentiment){
    	switch(sentiment){
    		case 0:
    			return "very-negative"; 
    		case 1:
    			return "negative"; 
    		case 2:
    			return "neutral";
    		case 3:
    			return "positive"; 
    		case 4:
    			return "very-positive"; 
    		default:
    			return "undefined";
    		
    	}
    }
    public static String defineSentiment(ArrayList<String> sentiments){
    	int qtdNegative = 0;
    	int qtdNeutral = 0;
    	int qtdPositive = 0;
    	
    	if(sentiments.size() == 2 && !sentiments.get(0).equals(sentiments.get(1)))
    		return "tie";
    	
    	for(String s : sentiments){
    		if(s.equals("Negative") || s.equals("Very Negative"))
    			qtdNegative++;
    		if(s.equals("Neutral"))
    			qtdNeutral++;
    		if(s.equals("Positive") || s.equals("Very Positive"))
    			qtdPositive++;
    	}
    	
    	if(qtdNegative > qtdPositive && qtdNegative > qtdNeutral)
    		return "negative";
    	else if(qtdPositive > qtdNeutral)
    		return "positive";    	   	
    	else
    		return "neutral";
    }
}
