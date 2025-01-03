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
 * CertainWorkload class runs the workloads by different workers concurrently.
 * It configures the environment for the workers using WorkloadConfiguration
 * objects and reports the metrics
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
		// overridden if the property is set
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

		// Report the metrics
		reportMetric(workerRunResults);
	}

	/**
	 * Computes the metrics and prints them
	 *
	 * @param workerRunResults
	 */
	public static void reportMetric(List<WorkerRunResult> workerRunResults) {
		// Aggregate the total number of successful interactions, total time taken,
		// and other relevant metrics
		int totalSuccessfulInteractions = 0;
		int totalRuns = 0;
		long totalElapsedTimeInNanoSecs = 0;
		int totalFrequentBookStoreInteractions = 0;
		int successfulFrequentBookStoreInteractions = 0;

		// Loop through each worker's results and aggregate
		for (WorkerRunResult result : workerRunResults) {
			totalSuccessfulInteractions += result.getSuccessfulInteractions();
			totalRuns += result.getTotalRuns();
			totalElapsedTimeInNanoSecs += result.getElapsedTimeInNanoSecs();
			successfulFrequentBookStoreInteractions += result
					.getSuccessfulFrequentBookStoreInteractionRuns();
			totalFrequentBookStoreInteractions += result
					.getTotalFrequentBookStoreInteractionRuns();
		}

		// Calculate throughput (successful interactions / total time taken)
		double throughput = totalRuns == 0 ? 0 : (double) totalSuccessfulInteractions / totalElapsedTimeInNanoSecs;

		// Calculate average latency (total elapsed time / total successful interactions)
		double averageLatency = totalSuccessfulInteractions == 0 ? 0 : (double) totalElapsedTimeInNanoSecs / totalSuccessfulInteractions;

		// Calculate frequency of successful frequent interactions
		double frequentInteractionRate = totalFrequentBookStoreInteractions == 0 ? 0
				: (double) successfulFrequentBookStoreInteractions
				/ totalFrequentBookStoreInteractions;

		// Print out the results
		System.out.println("Total Successful Interactions: " + totalSuccessfulInteractions);
		System.out.println("Total Interactions: " + totalRuns);
		System.out.println("Total Time Taken (ms): " + (totalElapsedTimeInNanoSecs / 1_000_000));
		System.out.println("Throughput (successful interactions per nanosecond): " + throughput);
		System.out.println("Average Latency (nanoseconds per interaction): " + averageLatency);
		System.out.println("Frequent Interaction Success Rate: " + frequentInteractionRate);
	}

	/**
	 * Generate the data in bookstore before the workload interactions are run
	 *
	 * Ignores the serverAddress if its a localTest
	 */
	public static void initializeBookStoreData(BookStore bookStore,
											   StockManager stockManager) throws BookStoreException {

		// TODO: You should initialize data for your bookstore here
	}
}
