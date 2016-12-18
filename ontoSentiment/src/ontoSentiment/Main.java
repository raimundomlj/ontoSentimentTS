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

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.apache.jena.sparql.util.FmtUtils;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations.SentimentAnnotatedTree;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;

public class Main {

	public static void main(String[] args) {
		//runTwitterOnline();
		runUsingDatabase();
	}

	public static void runUsingDatabase() {
		String csvFile = "C:/Users/Raimundo/Desktop/tweets_cam.csv";
		String line = "";
		String cvsSplitBy = ";";

		try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
			FileWriter arquivo = new FileWriter(
					new File("C:/Users/Raimundo/Desktop/resultadoOntoSentimentBaseOntologie_cam.txt"));
			Properties props = new Properties();
			props.setProperty("annotators", "tokenize, ssplit, parse, sentiment");
			StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
			ArrayList<String> sentiments = new ArrayList<>();

			// String ontologieFilePath = "src/data/pizza.owl.rdf";
			// String prefix = "prefix pizza:
			// <http://www.co-ode.org/ontologies/pizza/pizza.owl#>\n" + "prefix
			// rdfs: <"
			// + RDFS.getURI() + ">\n" + "prefix owl: <" + OWL.getURI() + ">\n";
			// String textQuery = "SELECT ?pizza WHERE {?pizza a owl:Class ; " +
			// " rdfs:subClassOf ?restriction.\n"
			// + " ?restriction owl:onProperty pizza:hasTopping ;"+
			// " owl:someValuesFrom pizza:PeperoniSausageTopping " +
			// " owl:someValuesFrom pizza:GorgonzolaTopping" +
			// "}";

			// String textQuery = "SELECT ?class WHERE { ?class a owl:Class }";

			// List<String> resultsOfOntologie =
			// loadOntologie(ontologieFilePath, prefix, textQuery);

			int qtd = 0;
			int negatives = 0;
			int positives = 0;
			int neutral = 0;
			int acertos = 0;
			int erros = 0;
			int fp = 0;
			int fn = 0;

			while ((line = br.readLine()) != null) {
				String[] text = line.split(cvsSplitBy);
				System.out.println("Analisando: " + text[0]);
				Annotation document = new Annotation(text[0]);
				pipeline.annotate(document);

				List<CoreMap> sentences = document.get(SentencesAnnotation.class);

				// for (String onto : resultsOfOntologie) {
				// if (text[0].contains(onto)) {
				for (CoreMap sentence : sentences) {
					Tree tree = sentence.get(SentimentAnnotatedTree.class);
					int sentiment = RNNCoreAnnotations.getPredictedClass(tree);
					sentiments.add(Util.sentimentParserString(sentiment));

				}
				String sentimentAnalyzed = Util.defineSentiment(sentiments);
				try {
					if (sentimentAnalyzed.equals("negative")) {
						negatives++;
						System.out.println("     negativo");
					}
					if (sentimentAnalyzed.equals("positive")) {
						positives++;
						System.out.println("     positivo");
					}
					if (sentimentAnalyzed.equals("neutral")) {
						neutral++;
						System.out.println("     neutro");
					}

					if (sentimentAnalyzed.equals(text[1])) {
						acertos++;
						System.out.println("     correto: sim");
					} else {
						erros++;
						System.out.println("     correto: não");

						if (sentimentAnalyzed.equals("positive"))
							fp++;
						if (sentimentAnalyzed.equals("negative"))
							fn++;

					}
					sentiments.clear();
					qtd++;
				} catch (Exception e) {
					e.printStackTrace();
				}
				// }
				// }

			}
			try {
				arquivo.write("Tweets Analisados " + qtd + "\n");
				arquivo.write("Total Positives: " + positives + "\n");
				arquivo.write("Total Negatives: " + negatives + "\n");
				arquivo.write("Total Neutral: " + neutral + "\n");
				arquivo.write("Total Acertos: " + acertos + "\n");
				arquivo.write("Total Erros: " + erros + "\n");
				arquivo.write("Falsos Positivos: " + fp);
				arquivo.write("Falsos Negativos: " + fn);
				arquivo.write("Acuracia: " + (double) acertos / ((double) (acertos + erros)));
				arquivo.flush();

				System.out.print("Tweets Analisados " + qtd + "\n");
				System.out.print("Total Positives: " + positives + "\n");
				System.out.print("Total Negatives: " + negatives + "\n");
				System.out.print("Total Neutral: " + neutral + "\n");
				System.out.print("Total Acertos: " + acertos + "\n");
				System.out.print("Total Erros: " + erros + "\n");
				System.out.print("Falsos Positivos: " + fp+ "\n");
				System.out.print("Falsos Negativos: " + fn+ "\n");
				System.out.print("Acuracia: " + (double) acertos / ((double) (acertos + erros)));

			} catch (Exception e) {
				e.printStackTrace();
			}
			arquivo.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void runTwitterOnline() {
		String busca = "Wine";
		String lang = "en";

		/*String ontologieFilePathCam = "src/data/camera.owl";
		String prefixCam = "prefix camera: <http://www.xfront.com/owl/ontologies/camera/#>\n" + "prefix rdfs: <"
				+ RDFS.getURI() + ">\n" + "prefix owl: <" + OWL.getURI() + ">\n";
		String textQueryCam = "SELECT ?class WHERE { ?class a owl:Class }";
		List<String> resultsOfOntologieCam = loadOntologie(ontologieFilePathCam, prefixCam, textQueryCam);*/

		/*String ontologieFilePathPizza = "src/data/pizza.owl.rdf";
		 String prefixPizza = "prefix pizza: <http://www.co-ode.org/ontologies/pizza/pizza.owl#>\n" + "prefix rdfs: <"
		 	+ RDFS.getURI() + ">\n" + "prefix owl: <" + OWL.getURI() +">\n";		

		String textQueryPizza = "SELECT ?class WHERE { ?class a owl:Class }";

		List<String> resultsOfOntologiePizza = loadOntologie(ontologieFilePathPizza, prefixPizza, textQueryPizza);*/

		/*String ontologieFilePathWine = "src/data/wine.rdf";
		String prefixWine = "prefix vin: <http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#>\n" + "prefix rdfs: <"
				+ RDFS.getURI() + ">\n" + "prefix owl: <" + OWL.getURI() + ">\n";
		
		String textQueryWine = "SELECT ?class WHERE { ?class a owl:Class }";
		
		List<String> resultsOfOntologieWine = loadOntologie(ontologieFilePathWine, prefixWine, textQueryWine);*/
		
		Map<Date, ArrayList<String>> tweetsPerDay = getTweets(busca, lang);
		
		runWithoutOntologie(tweetsPerDay, "resultsOntologiesOnline_wine", "tweets_wine");
		//runWithOntologie(tweetsPerDay, "resultsOntologiesOnline_wine", "tweets_wine", resultsOfOntologieWine);
		

	}

	public static void runWithoutOntologie(Map<Date, ArrayList<String>> tweetsPerDay, String nameFile, String nameClassified) {
		try {

			FileWriter arquivo = new FileWriter(new File("C:/Users/Raimundo/Desktop/"+nameFile+".txt"));
			FileWriter arquivoClassificado = new FileWriter(new File("C:/Users/Raimundo/Desktop/"+nameClassified+".csv"));
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			Properties props = new Properties();
			props.setProperty("annotators", "tokenize, ssplit, parse, sentiment");
			StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

			ArrayList<String> sentiments = new ArrayList<>();

			if (!tweetsPerDay.isEmpty()) {
				tweetsPerDay.forEach((key, value) -> {
					String dataAnalisada = format.format(key);
					int qtd = 0;
					int negatives = 0;
					int positives = 0;
					int neutral = 0;

					for (String text : value) {
						Annotation document = new Annotation(text);
						pipeline.annotate(document);

						List<CoreMap> sentences = document.get(SentencesAnnotation.class);

						for (CoreMap sentence : sentences) {
							Tree tree = sentence.get(SentimentAnnotatedTree.class);
							int sentiment = RNNCoreAnnotations.getPredictedClass(tree);
							sentiments.add(Util.sentimentParserString(sentiment));

						}
						String sentimentAnalyzed = Util.defineSentiment(sentiments);
						try {
							if (sentimentAnalyzed.equals("negative")) {
								negatives++;
								// arquivoClassificado.write("'"+text+"';'"+onto+"';'negative'\n");
								arquivoClassificado.write("'" + text + "';'negative'\n");
							}
							if (sentimentAnalyzed.equals("positive")) {
								positives++;
								// arquivoClassificado.write("'"+text+";'"+onto+"';positive'\n");
								arquivoClassificado.write("'" + text + ";'positive'\n");
							}
							if (sentimentAnalyzed.equals("neutral")) {
								neutral++;
								// arquivoClassificado.write("'"+text+"';'"+onto+"';neutral'\n");
								arquivoClassificado.write("'" + text + "';'neutral'\n");
							}
							sentiments.clear();
							qtd++;
							for (int i = 0; i < 50; i++) {
								System.out.println("");
							}
							System.out.println("\n" + ((qtd * 100) / value.size())
									+ "% de tweets analisados para a data: " + dataAnalisada + "\n");
						} catch (Exception e) {
							e.printStackTrace();
						}

					}
					try {
						arquivo.write("data: " + dataAnalisada + " tweets analisados " + qtd + " tweets coletados:"
								+ value.size() + " % de: " + (qtd * 100) / value.size() + "% do todo " + "\n");
						arquivo.write("Total Positives: " + positives + "\n");
						arquivo.write("Total Negatives: " + negatives + "\n");
						arquivo.write("Total Neutral: " + neutral + "\n\n");
						arquivo.flush();
						arquivoClassificado.flush();
					} catch (Exception e) {
						e.printStackTrace();
					}
					System.out.println("\n\nTotal Positives: " + positives);
					System.out.println("Total Negatives: " + negatives);
					System.out.println("Total Neutral: " + neutral);
				});
				arquivo.close();
				arquivoClassificado.close();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void runWithOntologie(Map<Date, ArrayList<String>> tweetsPerDay, String nameFile, String nameClassified, List<String> resultsOfOntologie) {
		try {

			FileWriter arquivo = new FileWriter(new File("C:/Users/Raimundo/Desktop/"+nameFile+".txt"));
			FileWriter arquivoClassificado = new FileWriter(new File("C:/Users/Raimundo/Desktop/"+nameClassified+".csv"));
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			Properties props = new Properties();
			props.setProperty("annotators", "tokenize, ssplit, parse, sentiment");
			StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

			ArrayList<String> sentiments = new ArrayList<>();

			if (!tweetsPerDay.isEmpty()) {
				tweetsPerDay.forEach((key, value) -> {
					String dataAnalisada = format.format(key);
					int qtd = 0;
					int negatives = 0;
					int positives = 0;
					int neutral = 0;

					for (String text : value) {
						for (String onto : resultsOfOntologie) {
							if (text.contains(onto)) {
								Annotation document = new Annotation(text);
								pipeline.annotate(document);

								List<CoreMap> sentences = document.get(SentencesAnnotation.class);

								for (CoreMap sentence : sentences) {
									Tree tree = sentence.get(SentimentAnnotatedTree.class);
									int sentiment = RNNCoreAnnotations.getPredictedClass(tree);
									sentiments.add(Util.sentimentParserString(sentiment));

								}
								String sentimentAnalyzed = Util.defineSentiment(sentiments);
								try {
									if (sentimentAnalyzed.equals("negative")) {
										negatives++;
										// arquivoClassificado.write("'"+text+"';'"+onto+"';'negative'\n");
										arquivoClassificado.write("'" + text + "';'negative'\n");
									}
									if (sentimentAnalyzed.equals("positive")) {
										positives++;
										// arquivoClassificado.write("'"+text+";'"+onto+"';positive'\n");
										arquivoClassificado.write("'" + text + ";'positive'\n");
									}
									if (sentimentAnalyzed.equals("neutral")) {
										neutral++;
										// arquivoClassificado.write("'"+text+"';'"+onto+"';neutral'\n");
										arquivoClassificado.write("'" + text + "';'neutral'\n");
									}
									sentiments.clear();
									qtd++;
									for (int i = 0; i < 50; i++) {
										System.out.println("");
									}
									System.out.println("\n" + ((qtd * 100) / value.size())
											+ "% de tweets analisados para a data: " + dataAnalisada + "\n");
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						}

					}
					try {
						arquivo.write("data: " + dataAnalisada + " tweets analisados " + qtd + " tweets coletados:"
								+ value.size() + " % de: " + (qtd * 100) / value.size() + "% do todo " + "\n");
						arquivo.write("Total Positives: " + positives + "\n");
						arquivo.write("Total Negatives: " + negatives + "\n");
						arquivo.write("Total Neutral: " + neutral + "\n\n");
						arquivo.flush();
						arquivoClassificado.flush();
					} catch (Exception e) {
						e.printStackTrace();
					}
					System.out.println("\n\nTotal Positives: " + positives);
					System.out.println("Total Negatives: " + negatives);
					System.out.println("Total Neutral: " + neutral);
				});
				arquivo.close();
				arquivoClassificado.close();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static List<String> loadOntologie(String ontologieFilePath, String prefix, String textQuery) {
		List<String> retorno = new ArrayList<>();
		OntModel m = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);

		FileManager.get().readModel(m, ontologieFilePath);

		org.apache.jena.query.Query query = QueryFactory.create(prefix + textQuery);

		QueryExecution qexec = QueryExecutionFactory.create(query, m);
		try {
			ResultSet results = qexec.execSelect();
			// ResultSetFormatter.out( results, m );
			String result;
			while (results.hasNext()) {
				QuerySolution rBind = results.nextSolution();
				RDFNode obj = rBind.get(results.getResultVars().get(0));
				try {
					result = FmtUtils.stringForRDFNode(obj, new SerializationContext()).split("#")[1].split(">")[0];
					if (!retorno.contains(result))
						retorno.add(result);
				} catch (Exception e) {
					continue;
				}
			}
			retorno.remove("Thing");
			return retorno;

		} finally {
			qexec.close();
		}
	}

	public static Map<Date, ArrayList<String>> getTweets(String busca, String lang) {
		Map<Date, ArrayList<String>> tweetsPerDay = new HashMap<>();
		ArrayList<String> tweets = new ArrayList<>();

		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		Date actualDate = new Date();

		int totalTweets = 0;
		long maxID = -1;
		try {
			Query q = new Query(busca + " -filter:retweets -filter:links -filter:replies -filter:images");
			q.setCount(Util.TWEETS_PER_QUERY);
			q.resultType(Query.ResultType.recent);
			q.setMaxId(maxID);
			// q.setSince("2016-01-01");
			// q.setUntil(format.format(actualDate));
			// q.setUntil("2016-12-16");
			q.setLang(lang);
			q.setLocale(lang);
			QueryResult r = Util.getTwitter().search(q);
			do {
				for (Status s : r.getTweets()) {
					totalTweets++;
					if (maxID == -1 || s.getId() < maxID) {
						maxID = s.getId();
					}

					if (!format.format(actualDate).equals(format.format(s.getCreatedAt()))) {
						actualDate = s.getCreatedAt();
						tweetsPerDay.put(actualDate, new ArrayList<>(tweets));
						tweets.clear();

					}

					tweets.add(Util.cleanText(s.getText()));

					if (totalTweets % 100 == 0) {
						if (!tweetsPerDay.isEmpty() && !tweetsPerDay.get(actualDate).isEmpty()) {
							tweetsPerDay.get(actualDate).addAll(tweets);
							tweetsPerDay.put(actualDate, tweetsPerDay.get(actualDate));
							tweets.clear();
						} else {
							tweetsPerDay.put(actualDate, new ArrayList<>(tweets));
							tweets.clear();
						}
					}

				}
				q = r.nextQuery();
				if (q != null) {
					q.setMaxId(maxID);
					r = Util.getTwitter().search(q);
					System.out.println("Total tweets: " + totalTweets);
					System.out.println("Maximo ID: " + maxID);
					System.out.println("Data tweets: " + format.format(actualDate));
					Util.imprimirRateLimit(Util.RATE_LIMIT_OPTION_SEARCH_TWEETS);
				}
			} //while (q != null);
			 while (totalTweets <= 9900);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return tweetsPerDay;
	}
}
