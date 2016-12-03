/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ontoSentiment;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import twitter4j.IDs;
import twitter4j.PagableResponseList;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.conf.ConfigurationBuilder;

/**
 *
 * @author Raimundo
 */
public class Amigos extends Thread{
    
    @Override
    public void run(){
        ConfigurationBuilder cb = new ConfigurationBuilder();

        //the following is set without accesstoken- desktop client
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey("FBd5n7dyl8mCz73qyfZ0p4XHb")
                .setOAuthConsumerSecret("vu5Xt5TzBSL9naOZylIYlx5MdcRlhH2LvkpW6KIkxSf9AqwuGt")
                .setOAuthAccessToken("3232400175-lAchtC6ChWMTnJKe3BaWbst8SucIaTjn5gm4Rp2")
                .setOAuthAccessTokenSecret("DnkquBWAS6igYpM8Z4r54hH7ztcfMX6u8OzMXBLwM9Xkh");

        try {
            TwitterFactory tf = new TwitterFactory(cb.build());
            Twitter twitter = tf.getInstance();

            //User u = twitter.showUser("karlaffabiola");            
            User u = twitter.showUser("raythemaster");
            IDs ids;
            System.out.println("Listing followers's ids.");
           
            System.out.println("ID: "+u.getId());
            System.out.println("Nome: "+u.getScreenName());
            
            long cursor = -1;
            PagableResponseList<User> pagableFollowings;
            List<User> listFriends = new ArrayList<>();
            List<User> listFriends2 = new ArrayList<>();
            
            pagableFollowings = twitter.getFriendsList(u.getId(), cursor, 200);
            System.out.println("Qunatidade followers: "+pagableFollowings.size());
            for (User user : pagableFollowings) {
                System.out.println("Id: "+user.getId()+ " Nome: "+user.getScreenName());
                listFriends.add(user); // ArrayList<User>
            }
            
            for (User user : listFriends) {
                System.out.println("Id1: "+user.getId()+ " Nome1: "+user.getScreenName());
                pagableFollowings = twitter.getFriendsList(user.getId(), cursor, 200);
                System.out.println("Qunatidade followers: "+pagableFollowings.size());
                for (User user2 : pagableFollowings) {
                     System.out.println("Id2: "+user2.getId()+ " Nome2: "+user2.getScreenName());
                     listFriends2.add(user2); // ArrayList<User>
                }
            }
            System.out.println("Lista 1:"+listFriends.size());
            System.out.println("Lista 2:"+listFriends2.size());
            System.exit(0);
        } catch (TwitterException te) {
           // te.printStackTrace();
            System.out.println("Failed to get timeline: "+ new Date());
            try {
                Thread.sleep(3 * 60 * 1000);
                run();
            } catch (InterruptedException ex) {
                Logger.getLogger(Amigos.class.getName()).log(Level.SEVERE, null, ex);
            }
        }    
    }
}
