package com.acertainbookstore.client.workloads;

import java.util.Random;
import java.util.concurrent.Callable;
import com.acertainbookstore.utils.BookStoreException;

public class Worker implements Callable<WorkerRunResult> {
	private WorkloadConfiguration configuration = null;
	private int numSuccessfulFrequentBookStoreInteraction = 0;
	private int numTotalFrequentBookStoreInteraction = 0;

	public Worker(WorkloadConfiguration config) {
		configuration = config;
	}

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

	private void runRareStockManagerInteraction() throws BookStoreException {
		// Simulate acquiring a rare stock book
		System.out.println("Running rare stock manager interaction...");

		int isbn = new Random().nextInt(1000000);
		String title = "Rare Book " + isbn;
		String author = "Rare Author";
		float price = 200.0f; // Rare books are expensive

		System.out.println("Acquiring rare book with ISBN: " + isbn);
		// Simulate adding the rare book to the inventory
		addBookToInventory(isbn, title, author, price, 5); // Add 5 copies of the rare book
	}

	private void runFrequentStockManagerInteraction() throws BookStoreException {
		// Simulate replenishing frequently available stock
		System.out.println("Running frequent stock manager interaction...");

		int isbn = new Random().nextInt(1000000);
		String title = "Frequent Book " + isbn;
		String author = "Frequent Author";
		float price = 15.0f; // Frequent books are cheaper

		System.out.println("Replenishing stock for ISBN: " + isbn);
		// Simulate checking the current stock and replenishing
		replenishStock(isbn, title, author, price, 10); // Replenish 10 copies
	}

	private void runFrequentBookStoreInteraction() throws BookStoreException {
		// Simulate a customer interaction
		System.out.println("Running frequent customer interaction...");

		int isbn = new Random().nextInt(1000000);
		String title = "Customer Book " + isbn;
		String author = "Customer Author";
		float price = 20.0f; // Regular customer book price

		System.out.println("Customer interacting with book ISBN: " + isbn);
		// Simulate customer browsing or purchasing the book
		boolean bookAvailable = checkAvailability(isbn);

		if (bookAvailable) {
			System.out.println("Book is available for purchase. Proceeding with transaction...");
			applyDiscountAndFinalizePurchase(isbn, title, price);
		} else {
			System.out.println("Book is out of stock. Customer might choose another book.");
		}
	}

	// Helper method to add a book to the inventory
	private void addBookToInventory(int isbn, String title, String author, float price, int numCopies) {
		// Logic to add the book to inventory (in a real system, this would update a database)
		System.out.println("Adding " + numCopies + " copies of " + title + " (ISBN: " + isbn + ") to the inventory.");
	}

	// Helper method to replenish stock for a book
	private void replenishStock(int isbn, String title, String author, float price, int replenishCount) {
		// Logic to replenish stock (in a real system, this would interact with inventory management)
		System.out.println("Replenishing " + replenishCount + " copies of " + title + " (ISBN: " + isbn + ")");
	}

	// Helper method to check the availability of a book in the store
	private boolean checkAvailability(int isbn) {
		// Simulate checking if a book is available in stock
		// For simplicity, we assume that books with even ISBN numbers are in stock
		return isbn % 2 == 0;
	}

	// Helper method to apply a discount and finalize a purchase
	private void applyDiscountAndFinalizePurchase(int isbn, String title, float price) {
		// Simulate applying a discount (e.g., 10% off for frequent customers)
		float discount = 0.10f;
		float discountedPrice = price * (1 - discount);
		System.out.println("Applying discount. Final price for " + title + " (ISBN: " + isbn + ") is " + discountedPrice);
		// Finalize purchase logic (e.g., updating sales records)
		finalizeSale(isbn, discountedPrice);
	}

	// Helper method to finalize a sale transaction
	private void finalizeSale(int isbn, float finalPrice) {
		// Logic to finalize the sale (in a real system, this would update the store's sales database)
		System.out.println("Finalizing sale for ISBN: " + isbn + " with final price: " + finalPrice);
	}
}
