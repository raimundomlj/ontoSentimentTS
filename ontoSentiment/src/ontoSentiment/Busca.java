/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ontoSentiment;


import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Trends;
import twitter4j.TwitterException;

/**
 *
 * @author Raimundo
 */
public class Busca {

    public Trends buscarTrendingTopics(String location) throws TwitterException {
        Integer idTrendLocation = Util.getTrendLocationId(location, false);

        if (idTrendLocation == null) {
            System.out.println("Trend Location Not Found");
            System.exit(0);
        }

        return Util.getTwitter().getPlaceTrends(idTrendLocation);
    }

    public void buscarPorTrendingTopics(String location, String lang) throws TwitterException {
        int totalTweets = 0;
        long maxID = -1;
        Trends trends = buscarTrendingTopics(location);
        for (int i = 0; i < trends.getTrends().length; i++) {
            System.out.println("\n" + trends.getTrends()[i].getName());            
//            Query q = new Query(trends.getTrends()[i].getName());
//            q.setCount(Util.TWEETS_PER_QUERY); 
//            q.resultType(Query.ResultType.recent); 
//            q.setLang(lang);
//
//            QueryResult r = Util.getTwitter().search(q);
//
//            for (Status s : r.getTweets()) {
//                totalTweets++;
//                if (maxID == -1 || s.getId() < maxID) {
//                    maxID = s.getId();
//                }
//
//                System.out.printf("Às %s, @%-20s disse: %s\n", s.getCreatedAt().toString(), s.getUser().getScreenName(), Util.cleanText(s.getText()));
//            }
        }
        System.out.printf("\n\n Um total de %d tweets foram encontrados\n", totalTweets);
    }

    public void buscarPorAssunto(String busca, String lang) throws TwitterException {
        int totalTweets = 0;
        long maxID = -1;
        Query q = new Query(busca + " -filter:retweets -filter:links -filter:replies -filter:images");
        q.setCount(Util.TWEETS_PER_QUERY); // How many tweets, max, to retrieve 
        q.resultType(Query.ResultType.recent); // Get all tweets 
        q.setLang(lang);
        QueryResult r = Util.getTwitter().search(q);
        do {
            for (Status s : r.getTweets()) {
                totalTweets++;
                if (maxID == -1 || s.getId() < maxID) {
                    maxID = s.getId();
                }

                //System.out.printf("O tweet de id %s disse as %s, @%-20s disse: %s\n", new Long(s.getId()).toString(), s.getCreatedAt().toString(), s.getUser().getScreenName(), Util.cleanText(s.getText()));
                System.out.println(Util.cleanText(s.getText()));
            }
            q = r.nextQuery();
            if (q != null) {
                q.setMaxId(maxID);                                
                r = Util.getTwitter().search(q);
                System.out.println("Total tweets: "+totalTweets);
                System.out.println("Maximo ID: "+maxID);
                Util.imprimirRateLimit(Util.RATE_LIMIT_OPTION_SEARCH_TWEETS);
            }
        } while (q != null);
    }
}
