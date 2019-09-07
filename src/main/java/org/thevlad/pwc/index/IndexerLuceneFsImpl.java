package org.thevlad.pwc.index;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleFragmenter;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.TextFragment;
import org.apache.lucene.search.highlight.TokenSources;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import edu.uci.ics.crawler4j.url.WebURL;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class IndexerLuceneFsImpl implements Indexer {

	@Value("${swsConfig.storage.luceneIndexRootDir}")
	private String indexDir;
	@Value("${swsConfig.storage.indexedUrlsFilePath}")
	private String indexedUrlsFilePath;
	@Value("${swsConfig.indexer.maxResults}")
	private int maxResults;

	private Analyzer analyzer = new StandardAnalyzer();
//	private KeywordAnalyzer keywordAnalyzer = new KeywordAnalyzer();
	private IndexWriter indexWriter;
	private SearcherManager searchMgr;
	private boolean isOpen = false;
	private int defaultPageSize = 10;
	private ConcurrentHashMap<String, Map<String, Date>> indexedUrls;
	private static final String DOC_BODY_FIELD_NAME = "body";
	private static final String DOC_ID_FIELD_NAME = "id";
	private static final String DOC_TITLE_FIELD_NAME = "title";
	private static final String DOC_DATE_FIELD_NAME = "date";
	private static final String DOMAIN_FIELD_NAME = "domain";
	private static final String DOMAIN_FIELD_HASH_NAME = "domainHash";

	private ObjectMapper mapper;
	
	public String getIndexDir() {
		return indexDir;
	}

	public void setIndexDir(String indexDir) {
		this.indexDir = indexDir;
	}

	public int getDefaultPageSize() {
		return defaultPageSize;
	}

	public void setDefaultPageSize(int defaultPageSize) {
		this.defaultPageSize = defaultPageSize;
	}

	public int getMaxResults() {
		return maxResults;
	}

	public void setMaxResults(int maxResults) {
		this.maxResults = maxResults;
	}

	public String getIndexedUrlsFilePath() {
		return indexedUrlsFilePath;
	}

	public void setIndexedUrlsFilePath(String indexedUrlsFilePath) {
		this.indexedUrlsFilePath = indexedUrlsFilePath;
	}

	@Override
	public Map<String, Map<String, Date>> getIndexedUrls() {
		return indexedUrls;
	}

	@Override
	@PostConstruct
	public void init() throws IOException {
		log.debug("Init Indexer...");
        mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);

		openIndex();
		initSearcherManager();
		isOpen = true;
	}

	@Override
	@PreDestroy
	public void close() throws IOException {
		log.debug("Destroy Indexer...");
		synchronized (this) {
			if (searchMgr != null) {
				searchMgr.close();
			}
			if (indexWriter != null) {
				indexWriter.close();
			}
			searchMgr = null;
			indexWriter = null;
			isOpen = false;
			if (indexedUrlsFilePath != null) {
				File f = new File(indexedUrlsFilePath);
				FileOutputStream fos = new FileOutputStream(f);
				ObjectOutputStream oos = new ObjectOutputStream(fos);

				try {
					mapper.writeValue(fos, indexedUrls);
					String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(indexedUrls);
					Path filePath = Paths.get(indexedUrlsFilePath);
					Files.write(filePath, json.getBytes(), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
//					oos.writeObject(indexedUrls);
				} finally {
					if (oos != null) {
						oos.close();
					}
				}
			}
		}
	}

	@Override
	public void reopen() throws IOException {
		synchronized (this) {
			openIndex();
			initSearcherManager();
			isOpen = true;
		}
	}

	private void initSearcherManager() throws IOException {
		searchMgr = new SearcherManager(indexWriter, true, true, null);
	}

	@SuppressWarnings("unchecked")
	private void openIndex() throws IOException {
		Path path = FileSystems.getDefault().getPath(indexDir);
		Directory dir = NIOFSDirectory.open(path);
		IndexWriterConfig cfg = new IndexWriterConfig(analyzer);
		cfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
		IndexWriter iw = new IndexWriter(dir, cfg);
		iw.commit();
		indexWriter = iw;
		if (indexedUrlsFilePath != null) {
			File f = new File(indexedUrlsFilePath);
			if (f.exists()) {
				FileInputStream fis = new FileInputStream(f);
//				ObjectInputStream ois = new ObjectInputStream(fis);

				try {
					indexedUrls = mapper.readValue(fis, ConcurrentHashMap.class); 
					log.debug("Init Index visited urls...");
//					for (Iterator<String> iterator = indexedUrls.keySet().iterator(); iterator.hasNext();) {
//						String url = iterator.next();
//						logger.debug(url);
//					}
				} finally {
					if (fis != null) {
						fis.close();
					}
				}
			} else {
				indexedUrls = new ConcurrentHashMap<String, Map<String, Date>>();
			}
		}
	}

	@Override
	public void index(String docUrl, String title, String content, String date) throws IOException {
		if (indexedUrls.containsKey(docUrl)) {
			System.out.println("Indexer: index exists for url: " + docUrl);
			return;
		}
		Document doc = new Document();
		WebURL url = new WebURL();
		url.setURL(docUrl);
		String domain = url.getDomain().toLowerCase();
		IndexableField docIdFiled = new StringField(DOC_ID_FIELD_NAME, docUrl, Store.YES);
//		IndexableField domainFiled = new StringField(DOMAIN_FIELD_NAME, domain, Store.YES);
		FieldType domainType = new FieldType();
		domainType.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
		domainType.setStored(true);
		domainType.setStoreTermVectors(true);
		domainType.setTokenized(false);
		domainType.setStoreTermVectorOffsets(false);
		Field domainFiled = new Field(DOMAIN_FIELD_NAME, domain, domainType);

		FieldType domainHashType = new FieldType();
		domainHashType.setIndexOptions(IndexOptions.DOCS);
		domainHashType.setStored(true);
		domainHashType.setStoreTermVectors(true);
		domainHashType.setTokenized(true);
		domainHashType.setStoreTermVectorOffsets(true);
		domainHashType.setDocValuesType(DocValuesType.NUMERIC);
		int hash = domain.hashCode();
		IntPoint domainHashFiled = new IntPoint(DOMAIN_FIELD_HASH_NAME, hash);

//		Field domainHashFiled = new Field(DOMAIN_FIELD_HASH, domain.hashCode(), domainHashType);

		FieldType type = new FieldType();
		type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
		type.setStored(true);
		type.setStoreTermVectors(true);
		type.setTokenized(true);
		type.setStoreTermVectorOffsets(true);
		Field titleField = new Field(DOC_TITLE_FIELD_NAME, title, type);
		Field bodyField = new Field(DOC_BODY_FIELD_NAME, content, type);
		doc.add(titleField);
		doc.add(bodyField);
		doc.add(docIdFiled);
		doc.add(domainFiled);
		doc.add(domainHashFiled);
		doc.add(new StoredField(DOMAIN_FIELD_HASH_NAME, hash));

		if (date != null) {
//			Mon, 02 Sep 2019 18:22:14 GMT
			FieldType dateType = new FieldType();
			dateType.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
			dateType.setStored(true);
			dateType.setStoreTermVectors(true);
			dateType.setTokenized(false);
			dateType.setStoreTermVectorOffsets(false);
			Field dateFiled = new Field(DOC_DATE_FIELD_NAME, date, dateType);
			doc.add(dateFiled);
		}
		indexWriter.addDocument(doc);
		Map<String, Date> urls = null;
		if (indexedUrls.containsKey(domain)) {
			urls = indexedUrls.get(domain);
		} else {
			urls = new HashMap<String, Date>();
			indexedUrls.put(domain, urls);
		}
		
		urls.put(docUrl, new Date());
		indexWriter.commit();

	}

	public int getNumDocs() throws IOException {
		searchMgr.maybeRefreshBlocking();
		IndexSearcher searcher = searchMgr.acquire();
		try {
			return searcher.getIndexReader().numDocs();
		} finally {
			searchMgr.release(searcher);
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public SearchResult search(SearchRequest request)
			throws IOException, ParseException, InvalidTokenOffsetsException {
		searchMgr.maybeRefreshBlocking();
		IndexSearcher searcher = searchMgr.acquire();
		try {
//			List<SearchResult> results = new ArrayList<SearchResult>();
			List<SearchResultItem> items = new ArrayList<SearchResultItem>();
			String searchStr = request.getSearchStr();
			int pageNumber = request.getPage();

			Query titleQuery = createStringFieldQuery(DOC_TITLE_FIELD_NAME, searchStr);
			Query bodyQuery = createStringFieldQuery(DOC_BODY_FIELD_NAME, searchStr);
			BooleanQuery contextQuery = new BooleanQuery.Builder()
					.add(titleQuery, Occur.SHOULD)
					.add(bodyQuery, Occur.SHOULD)
					.build();
			BooleanQuery.Builder rootQueryBuilder = new BooleanQuery.Builder();
			rootQueryBuilder.add(contextQuery, Occur.MUST).build();
			Query domainQuery = null;
			if (request.isOnlySubmittedDomain() && request.getDomain() != null) {
				String domain = request.getDomain().toLowerCase();
				domainQuery = createTermQuery(DOMAIN_FIELD_HASH_NAME, domain.hashCode());
				rootQueryBuilder.add(domainQuery, Occur.FILTER);
			}
			BooleanQuery rootQuery = rootQueryBuilder.build();
//			QueryParser qp = new QueryParser("", analyzer);
//			Query q = qp.parse(rootQuery.toString());
//			Query q = qp.parse(domainQuery.toString());

			TopScoreDocCollector collector = TopScoreDocCollector.create(maxResults, maxResults * 10);

			int startIndex = (pageNumber < 0 ? 0 : pageNumber - 1) * defaultPageSize;
			searcher.search(rootQuery, collector);

			TopDocs topDocs = collector.topDocs(startIndex, defaultPageSize);
			long totalHits = topDocs.totalHits.value;

			QueryParser titleParser = new QueryParser(DOC_TITLE_FIELD_NAME, analyzer);
			
			Query titleHquery = null;
			try {
				titleHquery = titleParser.parse(searchStr);
			} catch (ParseException e) {
				log.error("Error parsing query: {}", searchStr, e);
				searchStr = "\"" + searchStr + "\"";
				titleHquery = titleParser.parse(searchStr);
			}

			QueryParser bodyParser = new QueryParser(DOC_BODY_FIELD_NAME, analyzer);

			Query bodyHquery = bodyParser.parse(searchStr); 

			QueryScorer titleScorer = new QueryScorer(titleHquery);
			QueryScorer bodyScorer = new QueryScorer(bodyHquery);
			for (ScoreDoc doc : topDocs.scoreDocs) {
				Document d = searcher.doc(doc.doc);
				String url = d.get(DOC_ID_FIELD_NAME);
				String title = d.get(DOC_TITLE_FIELD_NAME);
				String body = d.get(DOC_BODY_FIELD_NAME);

				ZonedDateTime date = null;
				String dateStr = d.get(DOC_DATE_FIELD_NAME);
				if (!StringUtils.isEmpty(dateStr)) {
					date = ZonedDateTime.parse(dateStr, DateTimeFormatter.RFC_1123_DATE_TIME);
				}
				if (body.indexOf("<img") >= 0) {
					log.debug(body);
				}
				TokenStream titleStream = TokenSources
						.getAnyTokenStream(searcher.getIndexReader(), doc.doc,
								DOC_TITLE_FIELD_NAME, analyzer);
				Fragmenter titleFragmenter = new SimpleFragmenter(70);
//				SpanGradientFormatter formatter = new SpanGradientFormatter(0.0f, "#000000", "#000000", "#00FFFF",
//						"#00CCFF");
//				SimpleHTMLFormatter formatter = new SimpleHTMLFormatter("<mark>", "</mark>");
				SimpleHTMLFormatter formatter = new SimpleHTMLFormatter("<em>", "</em>");
				Highlighter titleHighlighter = new Highlighter(formatter, titleScorer);
				titleHighlighter.setTextFragmenter(titleFragmenter);

				TextFragment[] frag = titleHighlighter.getBestTextFragments(titleStream, title, false, 1);
				String titleH = "";
				for (int j = 0; j < frag.length; j++) {
					if ((frag[j] != null) && (frag[j].getScore() > 0)) {
						titleH = titleH + frag[j].toString().trim().replaceAll("\\s+", " ");
						if (j > 0 && j < frag.length - 1) {
							titleH = titleH + " ... ";
						}
					}
				}

				if (titleH.isEmpty()) {
					titleH = title;
				}

				TokenStream bodyStream = TokenSources
						.getAnyTokenStream(searcher.getIndexReader(), doc.doc,
								DOC_BODY_FIELD_NAME, analyzer);
				Fragmenter bodyFragmenter = new SimpleFragmenter(140);
				Highlighter bodyHighlighter = new Highlighter(formatter, bodyScorer);
				bodyHighlighter.setTextFragmenter(bodyFragmenter);

				TextFragment[] bodyFrag = bodyHighlighter.getBestTextFragments(bodyStream, body, false, 2);
				String bodyH = "";
				for (int j = 0; j < bodyFrag.length; j++) {
					if ((bodyFrag[j] != null) && (bodyFrag[j].getScore() > 0)) {
						bodyH = bodyH + bodyFrag[j].toString().trim().replaceAll("\\s+", " ");
						if (j > 0 && j < bodyFrag.length - 1) {
							bodyH = bodyH + " ... ";
						}
					}
				}

				if (bodyH.isEmpty()) {
					bodyH = body;
					if (bodyH.length() > 280) {
						bodyH = bodyH.substring(0, 280);
					}
				}
				SearchResultItem item = new SearchResultItem(titleH, url, bodyH, date);
				items.add(item);
			}
			request.setTotal((int) totalHits);
			int reminder = (int) (totalHits % 10);
			int pages;
			if (reminder == 0) {
				pages = (int) (totalHits / 10);
			} else {
				pages = (int) (totalHits / 10) + 1;
			}
			request.setPages(pages);

			SearchResult result = new SearchResult(request, items);

			return result;
		} finally {
			searchMgr.release(searcher);
		}
	}

	private Query createTermQuery(String fieldName, String fieldValue) {
		TermQuery query = new TermQuery(new Term(fieldName, fieldValue));
		return query;
	}

	private Query createTermQuery(String fieldName, int fieldValue) {

		Query prq = IntPoint.newRangeQuery(fieldName, 1, fieldValue);
//		NumericRangeQuery<Integer> nrq = NumericRangeQuery.newIntRange(fieldName, 1,fieldValue, fieldValue, true, true);
		return prq;

//		BytesRefBuilder builder = new BytesRefBuilder();
//		byte[] bytes = ByteBuffer.allocate(4).putInt(fieldValue).array();
//		builder.append(bytes, 0, 4);
//		Term term = new Term(fieldName, builder.toBytesRef());
//		TermQuery query = new TermQuery(term);
//		return query;

	}

	private Query createStringFieldQuery(String fieldName, String value) {
		if (value.matches("\".*\"")) {
			// create phrase query
			String unquotedStr = value.substring(1, value.length() - 1);
			PhraseQuery.Builder phraseBuilder = new PhraseQuery.Builder();
			for (String tok : unquotedStr.split("\\s")) {
				phraseBuilder.add(new Term(fieldName, tok));
			}
			return phraseBuilder.build();
		} else {
			// create a boolean OR query
			BooleanQuery.Builder boolQueryBuildder = new BooleanQuery.Builder();
			for (String tok : value.split("\\s")) {
				boolQueryBuildder.add(new TermQuery(new Term(fieldName, tok)), BooleanClause.Occur.SHOULD);
			}

			return boolQueryBuildder.build();
		}
	}

}
