package com.acertainbookstore.client.workloads;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import com.acertainbookstore.business.StockBook;

/**
 * Helper class to generate stockbooks and isbns modeled similar to Random
 * class
 */
public class BookSetGenerator {

    private Random random;

    public BookSetGenerator() {
        this.random = new Random();
    }

    /**
     * Returns num randomly selected isbns from the input set
     * 
     * @param isbns The input set of ISBNs
     * @param num   The number of ISBNs to select
     * @return A set containing num randomly selected ISBNs
     */
    public Set<Integer> sampleFromSetOfISBNs(Set<Integer> isbns, int num) {
        Set<Integer> selectedISBNs = new HashSet<>();
        if (isbns == null || isbns.isEmpty() || num <= 0) {
            return selectedISBNs;
        }

        Integer[] isbnArray = isbns.toArray(new Integer[0]);
        while (selectedISBNs.size() < num && selectedISBNs.size() < isbns.size()) {
            int randomIndex = random.nextInt(isbnArray.length);
            selectedISBNs.add(isbnArray[randomIndex]);
        }

        return selectedISBNs;
    }

    /**
     * Return num stock books. For now return an ImmutableStockBook
     * 
     * @param num The number of StockBook objects to generate
     * @return A set of randomly generated StockBook objects
     */
    public Set<StockBook> nextSetOfStockBooks(int num) {
        Set<StockBook> stockBooks = new HashSet<>();
        if (num <= 0) {
            return stockBooks;
        }

        for (int i = 0; i < num; i++) {
            int isbn = 100000 + random.nextInt(900000); // Random 6-digit ISBN
            String title = "Book Title " + isbn;
            String author = "Author " + isbn;
            float price = 10.0f + random.nextFloat() * 90.0f; // Random price between 10 and 100
            int numCopies = 1 + random.nextInt(100); // Random number of copies between 1 and 100
            boolean editorPick = random.nextBoolean();

            stockBooks.add(new StockBook() {
                @Override
                public int getISBN() {
                    return isbn;
                }

                @Override
                public String getTitle() {
                    return title;
                }

                @Override
                public String getAuthor() {
                    return author;
                }

                @Override
                public float getPrice() {
                    return price;
                }

                @Override
                public long getTotalRating() {
                    return 0; // Default value
                }

                @Override
                public long getNumTimesRated() {
                    return 0; // Default value
                }

                @Override
                public int getNumCopies() {
                    return numCopies;
                }

                @Override
                public long getNumSaleMisses() {
                    return 0; // Default value
                }

                @Override
                public float getAverageRating() {
                    return 0.0f; // Default value
                }

                @Override
                public boolean isEditorPick() {
                    return editorPick;
                }
            });
        }

        return stockBooks;
    }
}
