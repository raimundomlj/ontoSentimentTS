package ontoSentiment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.Evaluate;
import edu.stanford.nlp.sentiment.RNNOptions;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations.SentimentAnnotatedTree;
import edu.stanford.nlp.sentiment.SentimentModel;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;

public class Main {

	public static void main(String[] args) {
		//runUsingDatabase();
		runTwitterOnline();
	}
	
	public static void runUsingDatabase(){
		String csvFile = "C:/Users/Raimundo/Desktop/sanders-twitter-0.2/corpus.csv";
        String line = "";
        String cvsSplitBy = ",";
        String fileName = "C:/Users/Raimundo/Desktop/sanders-twitter-0.2/corpus2.csv";

        FileWriter fileWriter =null; 
        Twitter twitter = Util.getTwitter();
        Status status = null;       
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
        	fileWriter = new FileWriter(fileName);
            while ((line = br.readLine()) != null) {
            	
                String[] text = line.split(cvsSplitBy);
                
                status = twitter.showStatus(Long.parseLong(text[2]));        
                System.out.println("code= " + text[2]+" text= "+status.getText()+" label="+text[1]);                
                fileWriter.append(text[2]+","+status.getText()+","+text[1]);
                Thread.sleep(5000);
            }
            fileWriter.flush();
            fileWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	 public static void runTwitterOnline() {
		try {
			String busca = "Pizza"; 
			String lang = "en";
			
			ArrayList<String> tweets = new ArrayList<>();
			Map<Date, ArrayList<String>> tweetsPerDay = new HashMap<>();
			
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			
			Date actualDate = new Date();
			
			FileWriter arquivo = new FileWriter(new File("C:/Users/Raimundo/Desktop/resultadoOntoSentiment.txt"));
			
			int totalTweets = 0;
	        long maxID = -1;			
	        Query q = new Query(busca +" -filter:retweets -filter:links -filter:replies -filter:images");
	        q.setCount(Util.TWEETS_PER_QUERY); 
	        q.resultType(Query.ResultType.recent);
	        q.setMaxId(maxID);
	        //q.setSince("2016-01-01");
	        //q.setUntil(format.format(actualDate));
	        //q.setUntil("2016-11-17");
	        q.setLang(lang);
	        q.setLocale(lang);
	        QueryResult r = Util.getTwitter().search(q);
	        do {
	            for (Status s : r.getTweets()) {
	                totalTweets++;
	                if (maxID == -1 || s.getId() < maxID) {
	                    maxID = s.getId();
	                }	    
	                
	                if(!format.format(actualDate).equals(format.format(s.getCreatedAt()))){
                		actualDate = s.getCreatedAt();
                		tweetsPerDay.put(actualDate, new ArrayList<>(tweets));
                		tweets.clear();
	                	
	                }	                
	                
	                tweets.add(Util.cleanText(s.getText()));
	                
	                if(totalTweets%100 == 0){
	                	if(!tweetsPerDay.isEmpty() && !tweetsPerDay.get(actualDate).isEmpty()){
	                		tweetsPerDay.get(actualDate).addAll(tweets);
	                		tweetsPerDay.put(actualDate, tweetsPerDay.get(actualDate));
	                		tweets.clear();
	                	}	                		
	                	else{
	                		tweetsPerDay.put(actualDate, new ArrayList<>(tweets));
	                		tweets.clear();
	                	}
                	}
	               
	            }
	            q = r.nextQuery();
	            if (q != null) {
	                q.setMaxId(maxID);                                
	                r = Util.getTwitter().search(q);
	                System.out.println("Total tweets: "+totalTweets);
	                System.out.println("Maximo ID: "+maxID);
	                System.out.println("Data tweets: "+format.format(actualDate));
	                Util.imprimirRateLimit(Util.RATE_LIMIT_OPTION_SEARCH_TWEETS);
	            }
	        } //while (q != null ;
	          while (totalTweets <= 4900);
			
	        Properties props = new Properties();
	        //props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref, sentiment");
	        props.setProperty("annotators", "tokenize, ssplit, parse, sentiment");	        
			StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	        
	        ArrayList<String> sentiments = new ArrayList<>();
	        List<Tree> analise = new ArrayList<>();
	        List<Tree> train = new ArrayList<>();
	        //System.out.println("Tamanho hash: "+tweetsPerDay.size());
	               
			if(!tweetsPerDay.isEmpty()){
				tweetsPerDay.forEach((key, value)->{
					String dataAnalisada = format.format(key);
					int qtd = 0;
					int negatives = 0; 
					int positives = 0; 
					int neutral = 0;
					
					for(String text : value){
						//System.out.println("Frase: "+text);
						Annotation document = new Annotation(text);
						pipeline.annotate(document);
						
						List<CoreMap> sentences = document.get(SentencesAnnotation.class);
						
						for(CoreMap sentence : sentences){
							Tree tree = sentence.get(SentimentAnnotatedTree.class);							
							int sentiment = RNNCoreAnnotations.getPredictedClass(tree);							
							sentiments.add(Util.sentimentParserString(sentiment));

						}						
						String sentimentAnalyzed = Util.defineSentiment(sentiments);					
						
						if(sentimentAnalyzed.equals("negative"))
							negatives++;
						if(sentimentAnalyzed.equals("positive"))
							positives++;
						if(sentimentAnalyzed.equals("neutral"))
							neutral++;
						sentiments.clear();
						qtd++;						
						for(int i = 0; i < 50; i++)
						{
						       System.out.println("");
						}
						System.out.println("\n"+((qtd*100)/value.size())+"% de tweets analisados para a data: "+dataAnalisada+"\n");
					}
					try {
						arquivo.write("data: "+dataAnalisada+" tweets coletados:"+value.size()+"\n");
						arquivo.write("Total Positives: "+positives+"\n");
						arquivo.write("Total Negatives: "+negatives+"\n");			
						arquivo.write("Total Neutral: "+neutral+"\n\n");						
						arquivo.flush();
					} catch (Exception e) {					
						e.printStackTrace();
					}
					System.out.println("\n\nTotal Positives: "+positives);
					System.out.println("Total Negatives: "+negatives);			
					System.out.println("Total Neutral: "+neutral);
				});				
				arquivo.close();
			}
			
			
			
			/*			 
			 * 
			 * 1. Sentence Splitter // separador de frases
			2. Tokenization  // separa cada palavra da frase
			3. Lemma  // reduz um verbo ao seu radical, palavras no plural para singular, etc.
			4. Dependency Parsing  // análise sintática da frase como uma grafo (árvore) onde o verbo principal é a raiz
			5. NER  // identifica nomes de pessoas, organizações, lugares, (substantivos com nomes)
			6. POS tagging  // identifica qual é a categoria gramatica de uma palavra


			props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref, sentiment");

			1. ssplit 	SentencesAnnotation
			2. tokenize 	TokenizerAnnotator
			3. lemma 	MorphaAnnotator
			4. depparse 	DependencyParseAnnotator
			5. ner 	NERClassifierCombiner
			6. pos 	POSTaggerAnnotator*/
//			String text = "";
//			//Analise do texto
//			Properties props = new Properties();
//			props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
//			StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
//
//			// create an empty Annotation just with the given text
//			Annotation document = new Annotation(text);
//
//			// run all Annotators on this text
//			pipeline.annotate(document);
//			
//			
//			// these are all the sentences in this document
//			// a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
//			List<CoreMap> sentences = document.get(SentencesAnnotation.class);
//
//			for(CoreMap sentence: sentences) {
//			  // traversing the words in the current sentence
//			  // a CoreLabel is a CoreMap with additional token-specific methods
//			  for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
//			    // this is the text of the token
//			    String word = token.get(TextAnnotation.class);
//			    // this is the POS tag of the token
//			    String pos = token.get(PartOfSpeechAnnotation.class);
//			    // this is the NER label of the token
//			    String ne = token.get(NamedEntityTagAnnotation.class);
//			  }
//
//			  // this is the parse tree of the current sentence
//			  Tree tree = sentence.get(TreeAnnotation.class);
//
//			  // this is the Stanford dependency graph of the current sentence
//			  SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
//			}

			// This is the coreference link graph
			// Each chain stores a set of mentions that link to each other,
			// along with a method for getting the most representative mention
			// Both sentence and token offsets start at 1!
			//Map<Integer, CorefChain> graph =  document.get(CorefChainAnnotation.class);
			
			//Ontologia automatica
//			Client client = Client.create();
//			WebResource webResource = client.resource("http://wit.istc.cnr.it/stlab-tools/fred/");
//			
//			
//			
//			//application/rdf+xml
//			//application/json
//			//image/png 
//			//ClientResponse response = webResource.queryParam("text", "Ele é um bom menino").accept("image/png").get(ClientResponse.class);
//			//ClientResponse response = webResource.queryParam("text", "In computer science and information science, an ontology is a formal naming and definition of the types, properties, and interrelationships of the entities that really or fundamentally exist for a particular domain of discourse. It is thus a practical application of philosophical ontology, with a taxonomy.").accept("image/png").get(ClientResponse.class);
//			//ClientResponse response = webResource.queryParam("text", "He is a good boy").accept("image/png").get(ClientResponse.class);
//			ClientResponse response = webResource.queryParam("text", "He is a good boy").accept("image/png").get(ClientResponse.class);
//			if (response.getStatus() != 200) {
//				throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
//			}
//			//String output = response.getEntity(String.class);
//			Image image = ImageIO.read(response.getEntityInputStream());
//			//Image image2 = ImageIO.read(response2.getEntityInputStream());
//			//System.out.println("Output from Server .... \n");
//			//System.out.println(output);
//			
//			JFrame frame = new JFrame();
//		    JLabel label = new JLabel(new ImageIcon(image));
//		    frame.getContentPane().add(label, BorderLayout.CENTER);
//		    frame.pack();
//		    frame.setVisible(true);
//		    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		    
//		    /*JFrame frame2 = new JFrame();
//		    JLabel label2 = new JLabel(new ImageIcon(image2));
//		    frame2.getContentPane().add(label2, BorderLayout.CENTER);
//		    frame2.pack();
//		    frame2.setVisible(true);
//		    frame2.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);*/
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
