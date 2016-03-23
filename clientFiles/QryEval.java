/*
 *  Copyright (c) 2016, Carnegie Mellon University.  All Rights Reserved.
 *  Version 3.1.1.
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;

import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;

/**
 * QryEval is a simple application that reads queries from a file,
 * evaluates them against an index, and writes the results to an
 * output file.  This class contains the main method, a method for
 * reading parameter and query files, initialization methods, a simple
 * query parser, a simple query processor, and methods for reporting
 * results.
 * <p>
 * This software illustrates the architecture for the portion of a
 * search engine that evaluates queries.  It is a guide for class
 * homework assignments, so it emphasizes simplicity over efficiency.
 * Everything could be done more efficiently and elegantly.
 * <p>
 * The {@link Qry} hierarchy implements query evaluation using a
 * 'document at a time' (DaaT) methodology.  Initially it contains an
 * #OR operator for the unranked Boolean retrieval model and a #SYN
 * (synonym) operator for any retrieval model.  It is easily extended
 * to support additional query operators and retrieval models.  See
 * the {@link Qry} class for details.
 * <p>
 * The {@link RetrievalModel} hierarchy stores parameters and
 * information required by different retrieval models.  Retrieval
 * models that need these parameters (e.g., BM25 and Indri) use them
 * very frequently, so the RetrievalModel class emphasizes fast access.
 * <p>
 * The {@link Idx} hierarchy provides access to information in the
 * Lucene index.  It is intended to be simpler than accessing the
 * Lucene index directly.
 * <p>
 * As the search engine becomes more complex, it becomes useful to
 * have a standard approach to representing documents and scores.
 * The {@link ScoreList} class provides this capability.
 */
public class QryEval {

	//  --------------- Constants and variables ---------------------

	private static final String USAGE =
			"Usage:  java QryEval paramFile\n\n";

	private static final EnglishAnalyzerConfigurable ANALYZER =
			new EnglishAnalyzerConfigurable(Version.LUCENE_43);

	private static final String[] TEXT_FIELDS =
		{ "body", "title", "url", "inlink" };

	public static Map<String, Integer> queryTermFrequency = new HashMap<>();


	//  --------------- Methods ---------------------------------------

	/**
	 * @param args The only argument is the parameter file name.
	 * @throws Exception Error accessing the Lucene index.
	 */
	public static void main(String[] args) throws Exception {

		//  This is a timer that you may find useful.  It is used here to
		//  time how long the entire program takes, but you can move it
		//  around to time specific parts of your code.
		Timer timer = new Timer();
		timer.start ();

		//  Check that a parameter file is included, and that the required
		//  parameters are present.  Just store the parameters.  They get
		//  processed later during initialization of different system
		//  components.
		if (args.length < 1) {
			throw new IllegalArgumentException (USAGE);
		}
		Map<String, String> parameters = readParameterFile (args[0]);

		//  Configure query lexical processing to match index lexical
		//  processing.  Initialize the index and retrieval model.
		ANALYZER.setLowercase(true);
		ANALYZER.setStopwordRemoval(true);
		ANALYZER.setStemmer(EnglishAnalyzerConfigurable.StemmerType.KSTEM);

		Idx.initialize(parameters.get("indexPath"));
		RetrievalModel model = initializeRetrievalModel(parameters);

		//  Perform experiments.
		processQueryFile(parameters, model);

		//  Clean up.
		timer.stop ();
		System.out.println ("Time:  " + timer);
	}

	/**
	 * Allocate the retrieval model and initialize it using parameters
	 * from the parameter file.
	 * @return The initialized retrieval model
	 * @throws IOException Error accessing the Lucene index.
	 */
	private static RetrievalModel initializeRetrievalModel (Map<String, String> parameters)
			throws IOException {

		RetrievalModel model = null;
		String modelString = parameters.get ("retrievalAlgorithm").toLowerCase();

		if (modelString.equals("unrankedboolean")) {
			model = new RetrievalModelUnrankedBoolean();
		} else if (modelString.equals("rankedboolean")) {
			model = new RetrievalModelRankedBoolean();
		} else if (modelString.equals("bm25")) {
			model = new RetrievalModelBM25();
			((RetrievalModelBM25) model).setB(Double.parseDouble(parameters.get("BM25:b")));
			((RetrievalModelBM25) model).setK1(Double.parseDouble(parameters.get("BM25:k_1")));
			((RetrievalModelBM25) model).setK3(Double.parseDouble(parameters.get("BM25:k_3")));
		} else if (modelString.equals("indri")) {
			model = new RetrievalModelIndri();
			((RetrievalModelIndri) model).setMu(Integer.parseInt(parameters.get("Indri:mu")));
			((RetrievalModelIndri) model).setLambda(Double.parseDouble(parameters.get("Indri:lambda")));
		} else {
			throw new IllegalArgumentException
			("Unknown retrieval model " + parameters.get("retrievalAlgorithm"));
		}

		return model;
	}

	/**
	 * Optimize the query by removing degenerate nodes produced during
	 * query parsing, for example '#NEAR/1 (of the)' which turns into 
	 * '#NEAR/1 ()' after stopwords are removed; and unnecessary nodes
	 * or subtrees, such as #AND (#AND (a)), which can be replaced by 'a'.
	 */
	static Qry optimizeQuery(Qry q) {

		//  Term operators don't benefit from optimization.

		if (q instanceof QryIopTerm) {
			return q;
		}

		//  Optimization is a depth-first task, so recurse on query
		//  arguments.  This is done in reverse to simplify deleting
		//  query arguments that become null.

		for (int i = q.args.size() - 1; i >= 0; i--) {

			Qry q_i_before = q.args.get(i);
			Qry q_i_after = optimizeQuery (q_i_before);

			if (q_i_after == null) {
				q.removeArg(i);			// optimization deleted the argument
			} else {
				if (q_i_before != q_i_after) {
					q.args.set (i, q_i_after);	// optimization changed the argument
				}
			}
		}

		//  If the operator now has no arguments, it is deleted.

		if (q.args.size () == 0) {
			return null;
		}

		//  Only SCORE operators can have a single argument.  Other
		//  query operators that have just one argument are deleted.

		if ((q.args.size() == 1) &&
				(! (q instanceof QrySopScore))) {
			q = q.args.get (0);
		}

		return q;

	}

	private static void preProcessCurrentQuery(Qry operator) {
		if (operator instanceof QrySopWand ||
				operator instanceof QrySopWsum) {
			operator.expectingWeights = true;
		}
	}

	/**
	 * Return a query tree that corresponds to the query.
	 * 
	 * @param qString
	 *          A string containing a query.
	 * @param qTree
	 *          A query tree
	 * @throws IOException Error accessing the Lucene index.
	 */
	static Qry parseQuery(String qString, RetrievalModel model) throws IOException {

		//  Add a default query operator to every query. This is a tiny
		//  bit of inefficiency, but it allows other code to assume
		//  that the query will return document ids and scores.

		String defaultOp = model.defaultQrySopName ();
		qString = defaultOp + "(" + qString + ")";

		//  Simple query tokenization.  Terms like "near-death" are handled later.

		StringTokenizer tokens = new StringTokenizer(qString, "\t\n\r ,()", true);
		String token = null;

		//  This is a simple, stack-based parser.  These variables record
		//  the parser's state.

		Qry currentOp = null;
		Stack<Qry> opStack = new Stack<Qry>();

		//  Each pass of the loop processes one token. The query operator
		//  on the top of the opStack is also stored in currentOp to
		//  make the code more readable.

		while (tokens.hasMoreTokens()) {
			token = tokens.nextToken();
			if (token.matches("[ ,(\t\n\r]")) {
				continue;
			} else if (token.equals(")")) {	// Finish current query op.
				currentOp.expectingWeights = false;
				// If the current query operator is not an argument to another
				// query operator (i.e., the opStack is empty when the current
				// query operator is removed), we're done (assuming correct
				// syntax - see below).

				opStack.pop();

				if (opStack.empty())
					break;

				// Not done yet.  Add the current operator as an argument to
				// the higher-level operator, and shift processing back to the
				// higher-level operator.

				Qry arg = currentOp;
				currentOp = opStack.peek();
				currentOp.appendArg(arg);

			} else if (token.equalsIgnoreCase("#or")) {
				preProcessCurrentQuery(currentOp);
				currentOp = new QrySopOr();
				currentOp.setDisplayName(token);
				opStack.push(currentOp);
			} else if (token.equalsIgnoreCase("#and") || token.equalsIgnoreCase("#combine")) {
				preProcessCurrentQuery(currentOp);
				currentOp = new QrySopAnd();
				currentOp.setDisplayName(token);
				opStack.push(currentOp);
			} else if (token.equalsIgnoreCase("#sum")) {
				preProcessCurrentQuery(currentOp);
				currentOp = new QrySopSum();
				currentOp.setDisplayName(token);
				opStack.push(currentOp);
			} else if (token.toLowerCase().startsWith("#near/")) {
				preProcessCurrentQuery(currentOp);
				currentOp = new QryIopNear(Integer.parseInt(token.substring(6)));
				currentOp.setDisplayName(token);
				opStack.push(currentOp);
			}  else if (token.toLowerCase().startsWith("#window/")) {
				preProcessCurrentQuery(currentOp);
				currentOp = new QryIopWindow(Integer.parseInt(token.substring(8)));
				currentOp.setDisplayName(token);
				opStack.push(currentOp);
			} else if (token.equalsIgnoreCase("#syn")) {
				preProcessCurrentQuery(currentOp);
				currentOp = new QryIopSyn();
				currentOp.setDisplayName(token);
				opStack.push(currentOp);
			} else if (token.equalsIgnoreCase("#wand")) {
				preProcessCurrentQuery(currentOp);
				currentOp = new QrySopWand();
				currentOp.setDisplayName(token);
				opStack.push(currentOp);
				currentOp.expectingWeights = true;
			} else if (token.equalsIgnoreCase("#wsum")) {
				preProcessCurrentQuery(currentOp);
				currentOp = new QrySopWsum();
				currentOp.setDisplayName(token);
				opStack.push(currentOp);
				currentOp.expectingWeights = true;
			} else {

				if (currentOp.expectingWeights == true) {
					currentOp.weights.add(Double.valueOf(token));
					currentOp.expectingWeights = false;
					continue;
				}

				//  Split the token into a term and a field.
				int delimiter = token.indexOf('.');
				String field = null;
				String term = null;

				if (delimiter < 0) {
					field = "body";
					term = token;
				} else {
					field = token.substring(delimiter + 1).toLowerCase();
					term = token.substring(0, delimiter);
				}

				if ((field.compareTo("url") != 0) &&
						(field.compareTo("keywords") != 0) &&
						(field.compareTo("title") != 0) &&
						(field.compareTo("body") != 0) &&
						(field.compareTo("inlink") != 0)) {
					throw new IllegalArgumentException ("Error: Unknown field " + token);
				}

				//  Lexical processing, stopwords, stemming.  A loop is used
				//  just in case a term (e.g., "near-death") gets tokenized into
				//  multiple terms (e.g., "near" and "death").

				String t[] = tokenizeQuery(term);
				int len = currentOp.weights.size();
				if (t.length == 0 && len != 0) {
					currentOp.weights.remove(len - 1);
				} else if (t.length > 1 && len != 0) {
					Double num = currentOp.weights.get(len-1);
					for (int i = 1; i < t.length; i++) {
						currentOp.weights.add(num);
					}
				}

				for (int j = 0; j < t.length; j++) {
					Qry termOp = new QryIopTerm(t[j], field);
					currentOp.appendArg (termOp);

					if(queryTermFrequency.containsKey(t[j])) {
						queryTermFrequency.put(t[j], queryTermFrequency.get(t[j]) + 1);
					} else {
						queryTermFrequency.put(t[j], 1);
					}
				}

				if (currentOp instanceof QryIop) {
					((QryIop) currentOp).setFieldDetails();
				}
				preProcessCurrentQuery(currentOp);
			}
		}


		//  A broken structured query can leave unprocessed tokens on the opStack,

		if (tokens.hasMoreTokens()) {
			throw new IllegalArgumentException
			("Error:  Query syntax is incorrect.  " + qString);
		}

		return currentOp;
	}

	/**
	 * Print a message indicating the amount of memory used. The caller
	 * can indicate whether garbage collection should be performed,
	 * which slows the program but reduces memory usage.
	 * 
	 * @param gc
	 *          If true, run the garbage collector before reporting.
	 */
	public static void printMemoryUsage(boolean gc) {

		Runtime runtime = Runtime.getRuntime();

		if (gc)
			runtime.gc();

		System.out.println("Memory used:  "
				+ ((runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L)) + " MB");
	}

	/**
	 * Process one query.
	 * @param qString A string that contains a query.
	 * @param model The retrieval model determines how matching and scoring is done.
	 * @return Search results
	 * @throws IOException Error accessing the index
	 */
	static ScoreList processQuery(String qString, RetrievalModel model)
			throws IOException {

		Qry q = parseQuery(qString, model);
		q = optimizeQuery (q);

		// Show the query that is evaluated

		System.out.println("    --> " + q);

		if (q != null) {

			ScoreList r = new ScoreList ();
			if (q.args.size () > 0) {		// Ignore empty queries
				q.initialize (model);

				while (q.docIteratorHasMatch (model)) {
					int docid = q.docIteratorGetMatch();
					double score = ((QrySop) q).getScore(model, docid);
					r.add(docid, score);
					q.docIteratorAdvancePast(docid);
				}
			}

			return r;
		} else
			return null;
	}

	/**
	 * Process the query file.
	 * @param parameters
	 * @param model
	 * @throws IOException Error accessing the Lucene index.
	 */
	static void processQueryFile(Map<String, String> parameters, RetrievalModel model)
			throws IOException {

		BufferedReader input = null;

		try {
			String qLine = null;

			input = new BufferedReader(new FileReader(parameters.get("queryFilePath")));

			//  Each pass of the loop processes one query.

			while ((qLine = input.readLine()) != null) {
				int d = qLine.indexOf(':');

				if (d < 0) {
					throw new IllegalArgumentException
					("Syntax error:  Missing ':' in query line.");
				}

				printMemoryUsage(false);

				String qid = qLine.substring(0, d);
				String query = qLine.substring(d + 1);

				System.out.println("Query " + qLine);
				boolean queryExpansionNeeded = parameters.get("fb") != null 
						&& parameters.get("fb").equals("true");

				ScoreList r = null;

				if (!queryExpansionNeeded
						|| parameters.get("fbInitialRankingFile") == null) {
					r = processQuery(query, model);
				} else if (queryExpansionNeeded
						&& parameters.get("fbInitialRankingFile") != null) {
					r = extractScoreList(qid, parameters.get("fbInitialRankingFile"));
				}

				if (queryExpansionNeeded) {
					Map<String, Double> termScores = getExpandedQueryTerms(r, parameters);
					printExpandedQuery(qid, termScores, parameters.get("fbExpansionQueryFile"));
					String combinedQuery = getCombinedQuery(query, termScores, Double.valueOf(parameters.get("fbOrigWeight")));
					r = processQuery(combinedQuery, model);
				}

				if (r != null) {
					printResults(qid, r, parameters.get("trecEvalOutputPath"));
					System.out.println();
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			input.close();
		}
	}

	private static void printExpandedQuery(String queryName, Map<String, Double> termScores, String outputFilePath) throws IOException {
		File outputFile = new File(outputFilePath);
		if(!outputFile.exists()) {
			outputFile.createNewFile();
		}

		BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile, true));
		StringBuilder sb = new StringBuilder();
		sb.append(queryName);
		sb.append(": #WAND( ");
		for (String key: termScores.keySet()) {
			sb.append(termScores.get(key));
			sb.append(" ");
			sb.append(key + " ");
		}
		sb.append(")");
		bw.write(sb.toString());
		bw.newLine();
		bw.close();
	}

	private static String getCombinedQuery(String originalQuery, Map<String, Double> expandedQuery, double w) {
		Map<String, Double> combinedQueryWeights = new HashMap<>();
		System.out.println("Original Query:" + originalQuery);
		String[] terms = originalQuery.split("\\s+");
		for (String term: terms) {
			if (combinedQueryWeights.containsKey(term)) {
				combinedQueryWeights.put(term, combinedQueryWeights.get(term) + w);
			} else {
				combinedQueryWeights.put(term, w);
			}
		}

		for (String term: expandedQuery.keySet()) {
			Double weight = (1 - w) * expandedQuery.get(term);
			if (combinedQueryWeights.containsKey(term)) {
				combinedQueryWeights.put(term, combinedQueryWeights.get(term) + weight);
			} else {
				combinedQueryWeights.put(term, weight);
			}
		}

		StringBuilder sb = new StringBuilder();
		sb.append("#WAND( ");
		for (String key: combinedQueryWeights.keySet()) {
			sb.append(combinedQueryWeights.get(key));
			sb.append(" ");
			sb.append(key + " ");
		}
		sb.append(")");
		return sb.toString();
		
	}

	private static Map<String, Double> getExpandedQueryTerms(ScoreList scoreList, Map<String, String> parameters) throws IOException {
		int numDocs = Integer.parseInt(parameters.get("fbDocs"));
		int mu = Integer.parseInt(parameters.get("fbMu"));
		double corpusLength = Idx.getSumOfFieldLengths("body");
		Map<String, Double> termScores = new HashMap<>();
		scoreList.sort();

		for (int i = 0; i < numDocs; i++) {
			int relavantDocID = scoreList.getDocid(i);
			int docLength = Idx.getFieldLength("body", relavantDocID);
			double docScore = scoreList.getDocidScore(i);
			TermVector forwardIndex = new TermVector(relavantDocID, "body");
			for (int stemIndex = 0; stemIndex < forwardIndex.stemsLength(); stemIndex++) {
				String stem = forwardIndex.stemString(stemIndex);
				if (stem == null || stem.contains(".") || stem.contains(",")) {
					continue;
				}

				double documentMLE = forwardIndex.totalStemFreq(stemIndex) / (corpusLength + 0.0);
				double idfScore = Math.log(corpusLength / (forwardIndex.totalStemFreq(stemIndex) + 0.0));
				int tf = forwardIndex.stemFreq(stemIndex);
				double termWeight = ((tf + (mu * documentMLE)) / (docLength + mu + 0.0)) * docScore * idfScore;
				if (termScores.containsKey(stem)) {
					termScores.put(stem, termScores.get(stem) + termWeight);
				} else {
					termScores.put(stem, termWeight);
				}
			}
		}

		List<String> terms = getTopTerms(termScores, Integer.parseInt(parameters.get("fbTerms")));
		Map<String, Double> expandedQuery = new HashMap<>();
		for (String term: terms) {
			expandedQuery.put(term, termScores.get(term));
		}
		return expandedQuery;
	}

	private static List<String> getTopTerms(Map<String, Double> termScores, int numTerms) {
		Set<String> set = termScores.keySet();
		List<String> keys = new ArrayList<String>(set);
		Collections.sort(keys, new Comparator<String>() {

			@Override
			public int compare(String s1, String s2) {
				return Double.compare(termScores.get(s2), termScores.get(s1));
			}

		});
		return keys.subList(0, numTerms);
	}

	private static ScoreList extractScoreList(String queryId, String trecFileName) throws Exception {
		ScoreList r = new ScoreList();
		BufferedReader br = new BufferedReader(new FileReader(trecFileName));
		String line = br.readLine();
		
		boolean queryIDFound = false;
		while (line != null) {
			String[] documentScores = line.split("\\s+");
			if (documentScores[0].equals(queryId) && queryIDFound == false) {
				queryIDFound = true;
			} else if (!documentScores[0].equals(queryId) && queryIDFound == true) {
				break;
			} else if (!documentScores[0].equals(queryId) && queryIDFound == false) {
				continue;
			}
			
			int docid = Idx.getInternalDocid(documentScores[2]);
			r.add(docid, Double.valueOf(documentScores[4]));
			line = br.readLine();
		}
		br.close();
		return r;
	}

	/**
	 * Print the query results in the below format
	 * 
	 * QueryID Q0 DocID Rank Score RunID
	 * 
	 * @param queryName
	 *          Original query.
	 * @param result
	 *          A list of document ids and scores
	 * @param queryResultPath
	 *          path to store the query result
	 * @throws IOException Error accessing the Lucene index.
	 */
	static void printResults(String queryName, ScoreList result, String queryResultPath) throws IOException {
		File outputFile = new File(queryResultPath);

		if(!outputFile.exists()) {
			outputFile.createNewFile();
		}
		BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile, true));

		if (result.size() < 1) {
			//Need to throw a dummy result
			StringBuilder sb = new StringBuilder();
			sb.append(queryName);
			sb.append("\t");
			sb.append("Q0");
			sb.append("\t");
			sb.append("dummy");
			sb.append("\t");
			sb.append("1");
			sb.append("\t");
			sb.append("0");
			sb.append("\t");
			sb.append("run-1");
			bw.write(sb.toString());
		} else {
			// Sort the result Score List
			result.sort();
			int resultSize = Math.min(100, result.size());
			for (int i = 0; i < resultSize; i++) {
				StringBuilder sb = new StringBuilder();
				sb.append(queryName);
				sb.append("\t");
				sb.append("Q0");
				sb.append("\t");
				sb.append(Idx.getExternalDocid(result.getDocid(i)));
				sb.append("\t");
				sb.append(String.valueOf(i+1));
				sb.append("\t");
				sb.append(result.getDocidScore(i));
				sb.append("\t");
				sb.append("run-1");
				bw.write(sb.toString());
				bw.newLine();
			}
		}

		bw.close();
	}

	/**
	 * Read the specified parameter file, and confirm that the required
	 * parameters are present.  The parameters are returned in a
	 * HashMap.  The caller (or its minions) are responsible for
	 * processing them.
	 * @return The parameters, in <key, value> format.
	 */
	private static Map<String, String> readParameterFile (String parameterFileName)
			throws IOException {

		Map<String, String> parameters = new HashMap<String, String>();

		File parameterFile = new File (parameterFileName);

		if (! parameterFile.canRead ()) {
			throw new IllegalArgumentException
			("Can't read " + parameterFileName);
		}

		Scanner scan = new Scanner(parameterFile);
		String line = null;
		do {
			line = scan.nextLine();
			String[] pair = line.split ("=");
			parameters.put(pair[0].trim(), pair[1].trim());
		} while (scan.hasNext());

		scan.close();

		if (! (parameters.containsKey ("indexPath") &&
				parameters.containsKey ("queryFilePath") &&
				parameters.containsKey ("trecEvalOutputPath") &&
				parameters.containsKey ("retrievalAlgorithm"))) {
			throw new IllegalArgumentException
			("Required parameters were missing from the parameter file.");
		} else if (parameters.get("retrievalAlgorithm").equals("BM25")) {
			if (! (parameters.containsKey ("BM25:k_1") &&
					parameters.containsKey ("BM25:b") &&
					parameters.containsKey ("BM25:k_3"))) {
				throw new IllegalArgumentException
				("Required parameters were missing from the parameter file.");
			}
		} else if (parameters.get("retrievalAlgorithm").equals("Indri")) {
			if (! (parameters.containsKey ("Indri:mu") &&
					parameters.containsKey ("Indri:lambda"))) {
				throw new IllegalArgumentException
				("Required parameters were missing from the parameter file.");
			}
		}

		return parameters;
	}

	/**
	 * Given a query string, returns the terms one at a time with stopwords
	 * removed and the terms stemmed using the Krovetz stemmer.
	 * 
	 * Use this method to process raw query terms.
	 * 
	 * @param query
	 *          String containing query
	 * @return Array of query tokens
	 * @throws IOException Error accessing the Lucene index.
	 */
	static String[] tokenizeQuery(String query) throws IOException {

		TokenStreamComponents comp =
				ANALYZER.createComponents("dummy", new StringReader(query));
		TokenStream tokenStream = comp.getTokenStream();

		CharTermAttribute charTermAttribute =
				tokenStream.addAttribute(CharTermAttribute.class);
		tokenStream.reset();

		List<String> tokens = new ArrayList<String>();

		while (tokenStream.incrementToken()) {
			String term = charTermAttribute.toString();
			tokens.add(term);
		}

		return tokens.toArray (new String[tokens.size()]);
	}

}
