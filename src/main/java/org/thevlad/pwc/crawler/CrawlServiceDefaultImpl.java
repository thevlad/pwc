package org.thevlad.pwc.crawler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thevlad.pwc.index.Indexer;
import org.thevlad.pwc.services.CounterService;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.crawler.CrawlController.WebCrawlerFactory;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.parser.Parser;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;
import edu.uci.ics.crawler4j.url.WebURL;

@Component
public class CrawlServiceDefaultImpl implements CrawlService {

	static final Logger logger = LoggerFactory.getLogger(CrawlServiceDefaultImpl.class);

	@Value("${pwcConfig.storage.luceneIndexRootDir}")
	private String crawlStorageDir;
	@Value("${pwcConfig.crawl.maxDepthOfCrawling}")
	private int maxDepthOfCrawling = 3;
	@Value("${pwcConfig.crawl.maxPagesToFetch}")
	private int maxPagesToFetch = 100;
	@Value("${pwcConfig.crawl.numberOfCrawlers}")
	private int numberOfCrawlers = 3;
	@Value("${pwcConfig.crawl.politenessDelay}")
	private int politenessDelay = 500;
	@Value("${pwcConfig.crawl.controllersPoolSize}")
	private int controllersPoolSize = 5;
	@Autowired
	private Indexer indexer;
	@Autowired
	private CounterService counterService;
	private Map<Integer, CrawlController> pool;
//	private RobotstxtServer robotstxtServer = null;

	public String getCrawlStorageDir() {
		return crawlStorageDir;
	}

	public void setCrawlStorageDir(String crawlStorageDir) {
		this.crawlStorageDir = crawlStorageDir;
	}

	public int getMaxDepthOfCrawling() {
		return maxDepthOfCrawling;
	}

	public void setMaxDepthOfCrawling(int maxDepthOfCrawling) {
		this.maxDepthOfCrawling = maxDepthOfCrawling;
	}

	public int getMaxPagesToFetch() {
		return maxPagesToFetch;
	}

	public void setMaxPagesToFetch(int maxPagesToFetch) {
		this.maxPagesToFetch = maxPagesToFetch;
	}

	public int getNumberOfCrawlers() {
		return numberOfCrawlers;
	}

	public void setNumberOfCrawlers(int numberOfCrawlers) {
		this.numberOfCrawlers = numberOfCrawlers;
	}

	public int getPolitenessDelay() {
		return politenessDelay;
	}

	public void setPolitenessDelay(int politenessDelay) {
		this.politenessDelay = politenessDelay;
	}

	@Override
	public synchronized void crawl(String url) throws CrawlException {
		boolean canRun = false;
		int poolSize = pool.size();
		if (poolSize < controllersPoolSize) {
			try {
				createAndRunCrawler(url, poolSize);
				canRun = true;
			} catch (Exception e) {
				logger.error("Error create CrawlController", e);
				throw new CrawlException("Error when create Crawler # " + poolSize, e);
			}
		} else {
			for (Iterator<Map.Entry<Integer, CrawlController>> iterator = pool.entrySet().iterator(); iterator
					.hasNext();) {
				Map.Entry<Integer, CrawlController> entry = iterator.next();
				Integer key = entry.getKey();
				CrawlController controller = entry.getValue();
				if (controller.isFinished()) {
					try {
						createAndRunCrawler(url, key);
						canRun = true;
						break;
					} catch (Exception e) {
						logger.error("Error create CrawlController", e);
						throw new CrawlException("Error when create Crawler", e);
					}
				}
			}
		}

		if (!canRun) {
			throw new CrawlException("Sorry, all crawlers are busy. Try later.");
		}
	}

	private void createAndRunCrawler(String url, int idx) throws Exception {
		WebURL webURL = new WebURL();
		webURL.setURL(url);
		String domainToCrawl = webURL.getDomain();
		logger.debug("Create crawl controller #{} for domain: {}", idx, domainToCrawl);
		CrawlConfig config = new CrawlConfig();
		config.setCrawlStorageFolder(crawlStorageDir + "/" + idx);
		config.setPolitenessDelay(politenessDelay);
		config.setMaxPagesToFetch(maxPagesToFetch);
		PageFetcher pageFetcher = new PageFetcher(config);
		RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
		RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
		//TODO replace constructor with Parser witch can detect encoding from http header
		Parser parser = new Parser(config, new TikaHtmlParserWithMeta(config));
		CrawlController controller = new CrawlController(config, pageFetcher, parser,robotstxtServer);

		counterService.addCounter(domainToCrawl, controller);
		CrawlController.WebCrawlerFactory<Crawler> factory = new WebCrawlerFactory<Crawler>() {

			@Override
			public Crawler newInstance() throws Exception {
				Crawler crawler = new Crawler();
				crawler.setDomain(domainToCrawl);
				crawler.setIndexer(indexer);
				crawler.setCounterService(counterService);
				return crawler;
			}
		};

		pool.put(idx, controller);
		controller.addSeed(url);
		controller.startNonBlocking(factory, 3);
		logger.info("Create & run a new controller # " + idx);
	}

	@PostConstruct
	public void init() {
		pool = new HashMap<Integer, CrawlController>(controllersPoolSize);
	}

}
