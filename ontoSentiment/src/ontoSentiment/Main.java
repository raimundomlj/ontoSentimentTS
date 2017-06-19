package ontoSentiment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
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

import com.ibm.watson.developer_cloud.natural_language_understanding.v1.NaturalLanguageUnderstanding;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.AnalysisResults;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.AnalyzeOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.EntitiesOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.Features;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.KeywordsOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.SentimentOptions;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations.SentimentAnnotatedTree;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;
import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.doccat.DocumentSampleStream;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;

public class Main {

	private static DoccatModel model;

	public static void main(String[] args) throws IOException {
		// runTwitterOnline();
		// runUsingDatabase();

		loadLexico("C:/Users/raimundo.martins/Desktop/lexico_v3.0.txt");
		//classifyCoreNLP("C:/Users/raimundo.martins/Desktop/to_classify.csv");

		//trainAndClassifyWithOpenNLP("C:/Users/raimundo.martins/Desktop/to_classify.csv");

		//pt
		//watsonClassify("C:/Users/raimundo.martins/Desktop/to_classify.csv", true);
		//en
		//watsonClassify("C:/Users/raimundo.martins/Desktop/to_classify.csv", false);
		
		runMetricsFromFile("C:/Users/raimundo.martins/Desktop/classyfied.csv");
		
	}
	
	public static void loadLexico(String file) throws FileNotFoundException{
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line = "";
		String cvsSplitBy = ",";
	}

	public static void runMetricsFromFile(String file) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line = "";
		String cvsSplitBy = ";";

		int qtdCertosCore = 0;
		int qtdErrosCore = 0;
		int qtdCertosOpen = 0;
		int qtdErrosOpen = 0;
		int qtdCertosWatsonPT = 0;
		int qtdErrosWatsonPT = 0;
		int qtdCertosWatsonEN = 0;
		int qtdErrosWatsonEN = 0;
		while ((line = br.readLine()) != null) {
			String[] text = line.split(cvsSplitBy);

			if (text[2].equalsIgnoreCase(text[3]))
				qtdCertosCore++;
			else
				qtdErrosCore++;

			if (text[2].equalsIgnoreCase(text[4]))
				qtdCertosOpen++;
			else
				qtdErrosOpen++;
			
			if (text[2].equalsIgnoreCase(text[5]))
				qtdCertosWatsonPT++;
			else
				qtdErrosWatsonPT++;
			
			if (text[2].equalsIgnoreCase(text[6]))
				qtdCertosWatsonEN++;
			else
				qtdErrosWatsonEN++;
		}
		System.out.println("Acertos Core: " + qtdCertosCore);
		System.out.println("Erros Core: " + qtdErrosCore);
		System.out.println((qtdCertosCore * 100) / (qtdCertosCore + qtdErrosCore) + "% de acerto Core\n");

		System.out.println("Acertos Open: " + qtdCertosOpen);
		System.out.println("Erros Open: " + qtdErrosOpen);
		System.out.println((qtdCertosOpen * 100) / (qtdCertosOpen + qtdErrosOpen) + "% de acerto Open\n");
		
		System.out.println("Acertos Open: " + qtdCertosWatsonPT);
		System.out.println("Erros Open: " + qtdErrosWatsonPT);
		System.out.println((qtdCertosWatsonPT * 100) / (qtdCertosWatsonPT + qtdErrosWatsonPT) + "% de acerto Watson PT\n");
		
		System.out.println("Acertos Open: " + qtdCertosWatsonEN);
		System.out.println("Erros Open: " + qtdErrosWatsonEN);
		System.out.println((qtdCertosWatsonEN * 100) / (qtdCertosWatsonEN + qtdErrosWatsonEN) + "% de acerto Watson EN\n");		
	}

	public static void watsonClassify(String file, boolean pt) throws IOException {
		NaturalLanguageUnderstanding service = new NaturalLanguageUnderstanding(
				NaturalLanguageUnderstanding.VERSION_DATE_2017_02_27, "a347ecc2-54eb-4739-86b6-a045ae281fda",
				"W3o4I7htiZsa");

		SentimentOptions sentiment = new SentimentOptions.Builder().build();

		Features features = new Features.Builder().sentiment(sentiment).build();

		ArrayList<TweetTraduzido> tweetsTraduzidos = new ArrayList<>();
		TweetTraduzido tt;
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line = "";
		String cvsSplitBy = ";";
		int index = 1;
		if(pt)
			index = 0;
		while ((line = br.readLine()) != null) {
			String[] text = line.split(cvsSplitBy);
			AnalyzeOptions parameters = new AnalyzeOptions.Builder().text(text[index]).features(features).build();
			AnalysisResults response = service.analyze(parameters).execute();
			
			tt = new TweetTraduzido();
			tt.setPt(text[0]);
			// tt.setEnGoogle(text[1]);
			if (response.getSentiment().getDocument().getScore() > 0) {
				System.out.println(" POSITIVE ");
				tt.setClassifiedGoogle("positive");
			} else if (response.getSentiment().getDocument().getScore() < 0) {
				System.out.println(" NEGATIVE ");
				tt.setClassifiedGoogle("negative");
			} else {
				System.out.println(" NEUTRAL ");
				tt.setClassifiedGoogle("neutral");
			}

			tweetsTraduzidos.add(tt);
		}
		ImprimeArquivo print = new ImprimeArquivo("classyfied_base", tweetsTraduzidos);
		print.start();
		br.close();

	}

	public static void classifyTweetReadFileWithOpenNLP(String file) throws IOException {

		DocumentCategorizerME myCategorizer = new DocumentCategorizerME(model);
		ArrayList<TweetTraduzido> tweetsTraduzidos = new ArrayList<>();
		TweetTraduzido tt;
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line = "";
		String cvsSplitBy = ";";
		while ((line = br.readLine()) != null) {
			String[] text = line.split(cvsSplitBy);
			String category = myCategorizer.getBestCategory(myCategorizer.categorize(text[1]));
			System.out.print("-----------------------------------------------------\nTWEET :" + text[1] + " ===> ");
			tt = new TweetTraduzido();
			tt.setPt(text[0]);
			// tt.setEnGoogle(text[1]);
			if (category.equalsIgnoreCase("1")) {
				System.out.println(" POSITIVE ");
				tt.setClassifiedGoogle("positive");
			} else if (category.equalsIgnoreCase("0")) {
				System.out.println(" NEUTRAL ");
				tt.setClassifiedGoogle("neutral");
			} else {
				System.out.println(" NEGATIVE ");
				tt.setClassifiedGoogle("negative");
			}

			tweetsTraduzidos.add(tt);
		}
		ImprimeArquivo print = new ImprimeArquivo("classyfied_base", tweetsTraduzidos);
		print.start();
		br.close();

	}

	public static void trainAndClassifyWithOpenNLP(String file) {
		InputStream dataIn = null;
		try {
			dataIn = new FileInputStream("C:/Users/raimundo.martins/Desktop/tweets.txt");
			ObjectStream lineStream = new PlainTextByLineStream(dataIn, "UTF-8");
			ObjectStream sampleStream = new DocumentSampleStream(lineStream);
			// Specifies the minimum number of times a feature must be seen
			int cutoff = 2;
			int trainingIterations = 30;
			model = DocumentCategorizerME.train("en", sampleStream, cutoff, trainingIterations);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (dataIn != null) {
				try {
					dataIn.close();
					classifyTweetReadFileWithOpenNLP(file);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void classifyCoreNLP(String file) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line = "";
		String cvsSplitBy = ";";

		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, parse, sentiment");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		// StanfordCoreNLP pipeline2 = new StanfordCoreNLP(props);
		ArrayList<String> sentimentsGoogle = new ArrayList<>();
		// ArrayList<String> sentimentsYandex = new ArrayList<>();

		TweetTraduzido tt;
		ArrayList<TweetTraduzido> tweetsTraduzidos = new ArrayList<>();

		int qtd = 1;
		while ((line = br.readLine()) != null) {
			String[] text = line.split(cvsSplitBy);
			//if (qtd % 100 == 0)
			System.out.println("Processando: " + qtd);

			Annotation document = new Annotation(text[0]);
			pipeline.annotate(document);

			List<CoreMap> sentences = document.get(SentencesAnnotation.class);
			for (CoreMap sentence : sentences) {
				Tree tree = sentence.get(SentimentAnnotatedTree.class);
				int sentiment = RNNCoreAnnotations.getPredictedClass(tree);
				sentimentsGoogle.add(Util.sentimentParserString(sentiment));

			}
			String sentimentAnalyzedGoogle = Util.defineSentiment(sentimentsGoogle);

			// Annotation document2 = new Annotation(text[2]);
			// pipeline2.annotate(document2);
			//
			// List<CoreMap> sentences2 =
			// document2.get(SentencesAnnotation.class);
			// for (CoreMap sentence2 : sentences2) {
			// Tree tree2 = sentence2.get(SentimentAnnotatedTree.class);
			// int sentiment2 = RNNCoreAnnotations.getPredictedClass(tree2);
			// sentimentsYandex.add(Util.sentimentParserString(sentiment2));
			//
			// }
			// String sentimentAnalyzedYandex =
			// Util.defineSentiment(sentimentsYandex);

			tt = new TweetTraduzido();
			tt.setPt(text[0]);
			//tt.setEnGoogle(text[2]);
			// tt.setEnYandex(text[2]);
			tt.setClassifiedGoogle(sentimentAnalyzedGoogle);
			// tt.setClassifiedYandex(sentimentAnalyzedYandex);

			tweetsTraduzidos.add(tt);
			qtd++;
		}
		ImprimeArquivo print = new ImprimeArquivo("classyfied_base", tweetsTraduzidos);
		print.start();
		br.close();
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
				System.out.print("Falsos Positivos: " + fp + "\n");
				System.out.print("Falsos Negativos: " + fn + "\n");
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

		/*
		 * String ontologieFilePathCam = "src/data/camera.owl"; String prefixCam
		 * = "prefix camera: <http://www.xfront.com/owl/ontologies/camera/#>\n"
		 * + "prefix rdfs: <" + RDFS.getURI() + ">\n" + "prefix owl: <" +
		 * OWL.getURI() + ">\n"; String textQueryCam =
		 * "SELECT ?class WHERE { ?class a owl:Class }"; List<String>
		 * resultsOfOntologieCam = loadOntologie(ontologieFilePathCam,
		 * prefixCam, textQueryCam);
		 */

		/*
		 * String ontologieFilePathPizza = "src/data/pizza.owl.rdf"; String
		 * prefixPizza =
		 * "prefix pizza: <http://www.co-ode.org/ontologies/pizza/pizza.owl#>\n"
		 * + "prefix rdfs: <" + RDFS.getURI() + ">\n" + "prefix owl: <" +
		 * OWL.getURI() +">\n";
		 * 
		 * String textQueryPizza = "SELECT ?class WHERE { ?class a owl:Class }";
		 * 
		 * List<String> resultsOfOntologiePizza =
		 * loadOntologie(ontologieFilePathPizza, prefixPizza, textQueryPizza);
		 */

		/*
		 * String ontologieFilePathWine = "src/data/wine.rdf"; String prefixWine
		 * =
		 * "prefix vin: <http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#>\n"
		 * + "prefix rdfs: <" + RDFS.getURI() + ">\n" + "prefix owl: <" +
		 * OWL.getURI() + ">\n";
		 * 
		 * String textQueryWine = "SELECT ?class WHERE { ?class a owl:Class }";
		 * 
		 * List<String> resultsOfOntologieWine =
		 * loadOntologie(ontologieFilePathWine, prefixWine, textQueryWine);
		 */

		Map<Date, ArrayList<String>> tweetsPerDay = getTweets(busca, lang);

		runWithoutOntologie(tweetsPerDay, "resultsOntologiesOnline_wine", "tweets_wine");
		// runWithOntologie(tweetsPerDay, "resultsOntologiesOnline_wine",
		// "tweets_wine", resultsOfOntologieWine);

	}

	public static void runWithoutOntologie(Map<Date, ArrayList<String>> tweetsPerDay, String nameFile,
			String nameClassified) {
		try {

			FileWriter arquivo = new FileWriter(new File("C:/Users/Raimundo/Desktop/" + nameFile + ".txt"));
			FileWriter arquivoClassificado = new FileWriter(
					new File("C:/Users/Raimundo/Desktop/" + nameClassified + ".csv"));
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

	public static void runWithOntologie(Map<Date, ArrayList<String>> tweetsPerDay, String nameFile,
			String nameClassified, List<String> resultsOfOntologie) {
		try {

			FileWriter arquivo = new FileWriter(new File("C:/Users/Raimundo/Desktop/" + nameFile + ".txt"));
			FileWriter arquivoClassificado = new FileWriter(
					new File("C:/Users/Raimundo/Desktop/" + nameClassified + ".csv"));
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
			} // while (q != null);
			while (totalTweets <= 9900);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return tweetsPerDay;
	}
}
