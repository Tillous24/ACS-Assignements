/**
 * 
 */
package com.acertainbookstore.client.workloads;

import java.util.*;
import java.util.concurrent.Callable;

import com.acertainbookstore.business.Book;
import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.BookEditorPick;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.utils.BookStoreException;

/**
 * 
 * Worker represents the workload runner which runs the workloads with
 * parameters using WorkloadConfiguration and then reports the results
 * 
 */
public class Worker implements Callable<WorkerRunResult> {
    private WorkloadConfiguration configuration = null;
    private int numSuccessfulFrequentBookStoreInteraction = 0;
    private int numTotalFrequentBookStoreInteraction = 0;

    public Worker(WorkloadConfiguration config) {
	configuration = config;
    }

    /**
     * Run the appropriate interaction while trying to maintain the configured
     * distributions
     * 
     * Updates the counts of total runs and successful runs for customer
     * interaction
     * 
     * @param chooseInteraction
     * @return
     */
    private boolean runInteraction(float chooseInteraction) {
	try {
	    float percentRareStockManagerInteraction = configuration.getPercentRareStockManagerInteraction();
	    float percentFrequentStockManagerInteraction = configuration.getPercentFrequentStockManagerInteraction();

	    if (chooseInteraction < percentRareStockManagerInteraction) {
		runRareStockManagerInteraction();
	    } else if (chooseInteraction < percentRareStockManagerInteraction
		    + percentFrequentStockManagerInteraction) {
		runFrequentStockManagerInteraction();
	    } else {
		numTotalFrequentBookStoreInteraction++;
		runFrequentBookStoreInteraction();
		numSuccessfulFrequentBookStoreInteraction++;
	    }
	} catch (BookStoreException ex) {
	    return false;
	}
	return true;
    }

    /**
     * Run the workloads trying to respect the distributions of the interactions
     * and return result in the end
     */
    public WorkerRunResult call() throws Exception {
	int count = 1;
	long startTimeInNanoSecs = 0;
	long endTimeInNanoSecs = 0;
	int successfulInteractions = 0;
	long timeForRunsInNanoSecs = 0;

	Random rand = new Random();
	float chooseInteraction;

	// Perform the warmup runs
	while (count++ <= configuration.getWarmUpRuns()) {
	    chooseInteraction = rand.nextFloat() * 100f;
	    runInteraction(chooseInteraction);
	}

	count = 1;
	numTotalFrequentBookStoreInteraction = 0;
	numSuccessfulFrequentBookStoreInteraction = 0;

	// Perform the actual runs
	startTimeInNanoSecs = System.nanoTime();
	while (count++ <= configuration.getNumActualRuns()) {
	    chooseInteraction = rand.nextFloat() * 100f;
	    if (runInteraction(chooseInteraction)) {
		successfulInteractions++;
	    }
	}
	endTimeInNanoSecs = System.nanoTime();
	timeForRunsInNanoSecs += (endTimeInNanoSecs - startTimeInNanoSecs);
	return new WorkerRunResult(successfulInteractions, timeForRunsInNanoSecs, configuration.getNumActualRuns(),
		numSuccessfulFrequentBookStoreInteraction, numTotalFrequentBookStoreInteraction);
    }

    /**
     * Runs the new stock acquisition interaction
     * 
     * @throws BookStoreException
     */
    private void runRareStockManagerInteraction() throws BookStoreException {
	// TODO: Add code for New Stock Acquisition Interaction
		// the number of books to get
		int n = 50;

		// getting books from store
		List<StockBook> booksInStore = configuration.getStockManager().getBooks();

		// getting new random set of books
		Set<StockBook> booksFromRandom = configuration.getBookSetGenerator().nextSetOfStockBooks(n);

		// container for the books to add
		Set<StockBook> booksToAdd = new HashSet<StockBook>();

		// check if books from random already is in stock
		for (StockBook book : booksFromRandom) {
			if (booksInStore.contains(book)) {
				booksToAdd.add(book);
			}
		}

		// Adding books to store
		configuration.getStockManager().addBooks(booksToAdd);
	}

    /**
     * Runs the stock replenishment interaction
     * 
     * @throws BookStoreException
     */
    private void runFrequentStockManagerInteraction() throws BookStoreException {
	// TODO: Add code for Stock Replenishment Interaction

		// Getting the number of books in the random set of books
		int n = configuration.getNumBooksToAdd();

		// the number of copies to add
		int copies = 20;

		// getting books from store
		List<StockBook> booksInStore = configuration.getStockManager().getBooks();

		// sorting list with lambda expression
		booksInStore.sort((book1, book2) -> Integer.compare(book1.getNumCopies(), book2.getNumCopies()));

		Set<BookCopy> copiesToAdd = new HashSet<>();
		for (StockBook book : booksInStore.subList(0, n)) {
			copiesToAdd.add(new BookCopy(book.getISBN(), copies));
		}
		configuration.getStockManager().addCopies(copiesToAdd);
	}

    /**
     * Runs the customer interaction
     * 
     * @throws BookStoreException
     */
    private void runFrequentBookStoreInteraction() throws BookStoreException {
	// TODO: Add code for Customer Interaction

		// getting editor picks
		List<Book> editorPicks = configuration.getBookStore().getEditorPicks(configuration.getNumEditorPicksToGet());

		// creating set of ISBNs
		Set<Integer> isbns = new HashSet<>();
		for (Book book : editorPicks) {
			isbns.add(book.getISBN());
		}

		// getting a random subset
		Set<Integer> sample = configuration.getBookSetGenerator().sampleFromSetOfISBNs(isbns, configuration.getNumBooksToBuy());

		Set<BookCopy> booksToBuy = new HashSet<>();
		for (Integer isbn : sample) {
			booksToBuy.add(new BookCopy(isbn, 1));
		}
		configuration.getStockManager().addCopies(booksToBuy);
    }

}
