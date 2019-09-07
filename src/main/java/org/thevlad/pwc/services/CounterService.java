package org.thevlad.pwc.services;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import edu.uci.ics.crawler4j.crawler.CrawlController;

@Component
public class CounterService {

	private ConcurrentHashMap<String, CounterContext> counter = new ConcurrentHashMap<String, CounterContext>();
	private CounterState emptyState = new CounterState();
	
	public CounterState getCounter(String key) {
		if (counter.containsKey(key)) {
			CounterState state = new CounterState();
			CounterContext ctx = counter.get(key);
			state.setCount(ctx.getContextCounter().get());
			if (ctx.getContextController() != null) {
				boolean finished = ctx.getContextController().isFinished(); 
				state.setFinished(finished);
			}
			return state;
		} else {
			return emptyState;
		}
	}

	public CounterContext getContext(String key) {
		return counter.get(key);
	}
	
	public int increment(String key) {
		if (counter.containsKey(key)) {
			return counter.get(key).getContextCounter().incrementAndGet();
		} else {
			return -1;
		}
	}

	public void addCounter(String key, CrawlController controller) {
		AtomicInteger newCounter = new AtomicInteger(0);
		CounterContext ctx = new CounterContext();
		ctx.setContextCounter(newCounter);
		ctx.setContextController(controller);
		counter.put(key, ctx);
		
//		Runnable runnable = new Runnable() {
//			@Override
//			public void run() {
//				while (newCounter.incrementAndGet() <= 100) {
//					try {
//						Thread.sleep(1000);
//						System.out.printf("Counter key: %s, value %d\n",key,newCounter.get());
//						System.out.printf("Counter thread ID: %d\n",Thread.currentThread().getId());
//
//					} catch (InterruptedException e) {
//					}
//				}
//			}
//		};
//		
//		Thread thread = new Thread(runnable);
//		thread.start();
	
	}
	
	public static class CounterContext {
		private AtomicInteger contextCounter;
		private CrawlController contextController;

		public AtomicInteger getContextCounter() {
			return contextCounter;
		}

		public void setContextCounter(AtomicInteger contextCounter) {
			this.contextCounter = contextCounter;
		}

		public CrawlController getContextController() {
			return contextController;
		}

		public void setContextController(CrawlController contextController) {
			this.contextController = contextController;
		}

	}
}
