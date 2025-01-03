/**
 * 
 */
package com.acertainbookstore.client.workloads;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.acertainbookstore.business.CertainBookStore;
import com.acertainbookstore.client.BookStoreHTTPProxy;
import com.acertainbookstore.client.StockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;

/**
 * 
 * CertainWorkload class runs the workloads by different workers concurrently.
 * It configures the environment for the workers using WorkloadConfiguration
 * objects and reports the metrics
 * 
 */
public class CertainWorkload {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		int numConcurrentWorkloadThreads = 10;
		String serverAddress = "http://localhost:8081";
		boolean localTest = true;
		List<WorkerRunResult> workerRunResults = new ArrayList<WorkerRunResult>();
		List<Future<WorkerRunResult>> runResults = new ArrayList<Future<WorkerRunResult>>();

		// Initialize the RPC interfaces if its not a localTest, the variable is
		// overriden if the property is set
		String localTestProperty = System
				.getProperty(BookStoreConstants.PROPERTY_KEY_LOCAL_TEST);
		localTest = (localTestProperty != null) ? Boolean
				.parseBoolean(localTestProperty) : localTest;

		BookStore bookStore = null;
		StockManager stockManager = null;
		if (localTest) {
			CertainBookStore store = new CertainBookStore();
			bookStore = store;
			stockManager = store;
		} else {
			stockManager = new StockManagerHTTPProxy(serverAddress + "/stock");
			bookStore = new BookStoreHTTPProxy(serverAddress);
		}

		// Generate data in the bookstore before running the workload
		initializeBookStoreData(bookStore, stockManager);

		ExecutorService exec = Executors
				.newFixedThreadPool(numConcurrentWorkloadThreads);

		for (int i = 0; i < numConcurrentWorkloadThreads; i++) {
			WorkloadConfiguration config = new WorkloadConfiguration(bookStore,
					stockManager);
			Worker workerTask = new Worker(config);
			// Keep the futures to wait for the result from the thread
			runResults.add(exec.submit(workerTask));
		}

		// Get the results from the threads using the futures returned
		for (Future<WorkerRunResult> futureRunResult : runResults) {
			WorkerRunResult runResult = futureRunResult.get(); // blocking call
			workerRunResults.add(runResult);
		}

		exec.shutdownNow(); // shutdown the executor

		// Finished initialization, stop the clients if not localTest
		if (!localTest) {
			((BookStoreHTTPProxy) bookStore).stop();
			((StockManagerHTTPProxy) stockManager).stop();
		}

		reportMetric(workerRunResults);
	}

	/**
	 * Computes the metrics and prints them
	 * 
	 * @param workerRunResults
	 */
	public static void reportMetric(List<WorkerRunResult> workerRunResults) {
		// TODO: You should aggregate metrics and output them for plotting here

		int totalRuns = 0;
		int successRuns = 0;
		long elapsedTimeInNanoSecs = 0;
		int totalFrequentBookStoreInteractionRuns = 0;
		int successfulFrequentBookStoreInteractionRuns = 0;
		float aggregatedThroughput = 0f;

		// aggregating results of all workers
		for (WorkerRunResult runResult : workerRunResults) {
			totalRuns += runResult.getTotalRuns();
			successRuns += runResult.getSuccessfulInteractions();
			elapsedTimeInNanoSecs += runResult.getElapsedTimeInNanoSecs();
			totalFrequentBookStoreInteractionRuns += runResult.getTotalFrequentBookStoreInteractionRuns();
			successfulFrequentBookStoreInteractionRuns += runResult.getSuccessfulFrequentBookStoreInteractionRuns();
			aggregatedThroughput += runResult.getSuccessfulInteractions() / (float) runResult.getElapsedTimeInNanoSecs();
		}

		// Calculating key values
		float totalHitRatio = successRuns / (float) totalRuns;
		float totalFrequentBookStoreInteractionHitRatio = successfulFrequentBookStoreInteractionRuns / (float) totalFrequentBookStoreInteractionRuns;
		float customerInteractionRatio = totalFrequentBookStoreInteractionRuns / (float) totalRuns;

		// Printing values to screen
		System.out.println("--------------------------------------------");
		System.out.println("SUMMARY");
		System.out.println("Hit Ratio: " + totalHitRatio);
		System.out.println("Frequent BookStore Interaction Hit Ratio: " + totalFrequentBookStoreInteractionHitRatio);
		System.out.println("Aggregated Throughput: " + aggregatedThroughput);
		System.out.println("Customer Interactions Ratio: " + customerInteractionRatio);
		System.out.println("--------------------------------------------");
		System.out.println("DETAILS");
		System.out.println("Total runs: " + totalRuns);
		System.out.println("Success runs: " + successRuns);
		System.out.println("Elapsed time in Nanoseconds: " + elapsedTimeInNanoSecs);
		System.out.println("Total Frequent BookStore Interaction runs: " + totalFrequentBookStoreInteractionRuns);
		System.out.println("Success Frequent BookStore Interaction runs: " + successfulFrequentBookStoreInteractionRuns);
		System.out.println("--------------------------------------------");
	}

	/**
	 * Generate the data in bookstore before the workload interactions are run
	 * 
	 * Ignores the serverAddress if its a localTest
	 * 
	 */
	public static void initializeBookStoreData(BookStore bookStore,
			StockManager stockManager) throws Exception {

		// TODO: You should initialize data for your bookstore here

		// Getting WorkloadConfiguration
		WorkloadConfiguration config = new WorkloadConfiguration(bookStore, stockManager);

		// the number of books in the initial set
		int n = 1000;
		// adding n random generated books to the store
		stockManager.addBooks(config.getBookSetGenerator().nextSetOfStockBooks(n));
    }
}
