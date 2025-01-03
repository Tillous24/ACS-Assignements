package com.acertainbookstore.client.tests;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.acertainbookstore.client.workloads.BookSetGenerator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.acertainbookstore.business.Book;
import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.BookEditorPick;
import com.acertainbookstore.business.CertainBookStore;
import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.client.BookStoreHTTPProxy;
import com.acertainbookstore.client.StockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;

public class WorkloadTest {

    /** The local test. */
    private static boolean localTest = true;

    /** The store manager. */
    private static StockManager storeManager;

    /** The client. */
    private static BookStore client;

    // The data generator
    private static BookSetGenerator generator = new BookSetGenerator();

    /** The Constant TEST_ISBN. */
    private static final Integer TEST_ISBN = 30345650;


    /**
     * Initializes a new instance.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        try {
            String localTestProperty = System.getProperty(BookStoreConstants.PROPERTY_KEY_LOCAL_TEST);
            localTest = (localTestProperty != null) ? Boolean.parseBoolean(localTestProperty) : localTest;

            if (localTest) {
                CertainBookStore store = new CertainBookStore();
                storeManager = store;
                client = store;
            } else {
                storeManager = new StockManagerHTTPProxy("http://localhost:8081/stock");
                client = new BookStoreHTTPProxy("http://localhost:8081");
            }

            storeManager.removeAllBooks();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetRandomStockBook() {
        StockBook book = generator.getRandomStockBook();
        assertNotNull(book);
    }

    @Test
    public void testNextSetOfStockBooks() {
        int n = 10;
        Set<StockBook> books = generator.nextSetOfStockBooks(n);
        assertEquals(n, books.size());
    }

    @Test
    public void testSampleFromSetOfISBNs() {
        Set<Integer> fullSet = new HashSet<>();

        // size of full set
        int nFull = 10;

        // size of subset
        int nSub = 5;

        for(int i = 0; i < nFull; i++) {
            fullSet.add(TEST_ISBN + i);
        }

        assertEquals(nFull, fullSet.size());

        Set<Integer> subSet = generator.sampleFromSetOfISBNs(fullSet, nSub);

        assertEquals(nSub, subSet.size());
        assertTrue(fullSet.containsAll(subSet));
    }
}
