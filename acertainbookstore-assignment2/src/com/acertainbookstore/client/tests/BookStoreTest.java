package com.acertainbookstore.client.tests;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors; // added

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.acertainbookstore.business.Book;
import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.SingleLockConcurrentCertainBookStore;
import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.business.TwoLevelLockingConcurrentCertainBookStore;
import com.acertainbookstore.client.BookStoreHTTPProxy;
import com.acertainbookstore.client.StockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;


/**
 * {@link BookStoreTest} tests the {@link BookStore} interface.
 * 
 * @see BookStore
 */
public class BookStoreTest {

	/** The Constant TEST_ISBN. */
	private static final int TEST_ISBN = 3044560;

	/** The Constant NUM_COPIES. */
	private static final int NUM_COPIES = 5;

	/** The local test. */
	private static boolean localTest = true;

	/** Single lock test */
	private static boolean singleLock = true;

	
	/** The store manager. */
	private static StockManager storeManager;

	/** The client. */
	private static BookStore client;

	/**
	 * Sets the up before class.
	 */
	@BeforeClass
	public static void setUpBeforeClass() {
		try {
			String localTestProperty = System.getProperty(BookStoreConstants.PROPERTY_KEY_LOCAL_TEST);
			localTest = (localTestProperty != null) ? Boolean.parseBoolean(localTestProperty) : localTest;
			
			String singleLockProperty = System.getProperty(BookStoreConstants.PROPERTY_KEY_SINGLE_LOCK);
			singleLock = (singleLockProperty != null) ? Boolean.parseBoolean(singleLockProperty) : singleLock;

			if (localTest) {
				if (singleLock) {
					SingleLockConcurrentCertainBookStore store = new SingleLockConcurrentCertainBookStore();
					storeManager = store;
					client = store;
				} else {
					TwoLevelLockingConcurrentCertainBookStore store = new TwoLevelLockingConcurrentCertainBookStore();
					storeManager = store;
					client = store;
				}
			} else {
				storeManager = new StockManagerHTTPProxy("http://localhost:8081/stock");
				client = new BookStoreHTTPProxy("http://localhost:8081");
			}

			storeManager.removeAllBooks();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Helper method to add some books.
	 *
	 * @param isbn
	 *            the isbn
	 * @param copies
	 *            the copies
	 * @throws BookStoreException
	 *             the book store exception
	 */
	public void addBooks(int isbn, int copies) throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		StockBook book = new ImmutableStockBook(isbn, "Test of Thrones", "George RR Testin'", (float) 10, copies, 0, 0,
				0, false);
		booksToAdd.add(book);
		storeManager.addBooks(booksToAdd);
	}

	/**
	 * Helper method to get the default book used by initializeBooks.
	 *
	 * @return the default book
	 */
	public StockBook getDefaultBook() {
		return new ImmutableStockBook(TEST_ISBN, "Harry Potter and JUnit", "JK Unit", (float) 10, NUM_COPIES, 0, 0, 0,
				false);
	}

	/**
	 * Method to add a book, executed before every test case is run.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Before
	public void initializeBooks() throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(getDefaultBook());
		storeManager.addBooks(booksToAdd);
	}

	/**
	 * Method to clean up the book store, execute after every test case is run.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@After
	public void cleanupBooks() throws BookStoreException {
		storeManager.removeAllBooks();
	}

	/**
	 * Tests basic buyBook() functionality.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyAllCopiesDefaultBook() throws BookStoreException {
		// Set of books to buy
		Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, NUM_COPIES));

		// Try to buy books
		client.buyBooks(booksToBuy);

		List<StockBook> listBooks = storeManager.getBooks();
		assertTrue(listBooks.size() == 1);
		StockBook bookInList = listBooks.get(0);
		StockBook addedBook = getDefaultBook();

		assertTrue(bookInList.getISBN() == addedBook.getISBN() && bookInList.getTitle().equals(addedBook.getTitle())
				&& bookInList.getAuthor().equals(addedBook.getAuthor()) && bookInList.getPrice() == addedBook.getPrice()
				&& bookInList.getNumSaleMisses() == addedBook.getNumSaleMisses()
				&& bookInList.getAverageRating() == addedBook.getAverageRating()
				&& bookInList.getNumTimesRated() == addedBook.getNumTimesRated()
				&& bookInList.getTotalRating() == addedBook.getTotalRating()
				&& bookInList.isEditorPick() == addedBook.isEditorPick());
	}

	/**
	 * Tests that books with invalid ISBNs cannot be bought.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyInvalidISBN() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy a book with invalid ISBN.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, 1)); // valid
		booksToBuy.add(new BookCopy(-1, 1)); // invalid

		// Try to buy the books.
		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();

		// Check pre and post state are same.
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that books can only be bought if they are in the book store.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyNonExistingISBN() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy a book with ISBN which does not exist.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, 1)); // valid
		booksToBuy.add(new BookCopy(100000, 10)); // invalid

		// Try to buy the books.
		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();

		// Check pre and post state are same.
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that you can't buy more books than there are copies.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyTooManyBooks() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy more copies than there are in store.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, NUM_COPIES + 1));

		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that you can't buy a negative number of books.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyNegativeNumberOfBookCopies() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy a negative number of copies.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, -1));

		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that all books can be retrieved.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetBooks() throws BookStoreException {
		Set<StockBook> booksAdded = new HashSet<StockBook>();
		booksAdded.add(getDefaultBook());

		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", "Donald Knuth",
				(float) 300, NUM_COPIES, 0, 0, 0, false));
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 0, 0, false));

		booksAdded.addAll(booksToAdd);

		storeManager.addBooks(booksToAdd);

		// Get books in store.
		List<StockBook> listBooks = storeManager.getBooks();

		// Make sure the lists equal each other.
		assertTrue(listBooks.containsAll(booksAdded) && listBooks.size() == booksAdded.size());
	}

	/**
	 * Tests that a list of books with a certain feature can be retrieved.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetCertainBooks() throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", "Donald Knuth",
				(float) 300, NUM_COPIES, 0, 0, 0, false));
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 0, 0, false));

		storeManager.addBooks(booksToAdd);

		// Get a list of ISBNs to retrieved.
		Set<Integer> isbnList = new HashSet<Integer>();
		isbnList.add(TEST_ISBN + 1);
		isbnList.add(TEST_ISBN + 2);

		// Get books with that ISBN.
		List<Book> books = client.getBooks(isbnList);

		// Make sure the lists equal each other
		assertTrue(books.containsAll(booksToAdd) && books.size() == booksToAdd.size());
	}

	/**
	 * Tests that books cannot be retrieved if ISBN is invalid.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetInvalidIsbn() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Make an invalid ISBN.
		HashSet<Integer> isbnList = new HashSet<Integer>();
		isbnList.add(TEST_ISBN); // valid
		isbnList.add(-1); // invalid

		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, -1));

		try {
			client.getBooks(isbnList);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}
	// added
	@Test
	public void testGetEditorPicks() throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();

		StockBook book1 = new ImmutableStockBook(TEST_ISBN + 1, "Clean Code", "Robert C. Martin", (float) 25, NUM_COPIES, 0, 0, 0, true);
		StockBook book2 = new ImmutableStockBook(TEST_ISBN + 2, "The Pragmatic Programmer", "Andrew Hunt", (float) 30, NUM_COPIES, 0, 0, 0, false);
		booksToAdd.add(book1);
		booksToAdd.add(book2);

		storeManager.addBooks(booksToAdd);

		// Retrieve only editor picks
		List<StockBook> editorPicks = client.getEditorPicks(1).stream()
				.map(book -> (StockBook) book)
				.collect(Collectors.toList());

		assertTrue(editorPicks.size() == 1);
		assertTrue(editorPicks.get(0).isEditorPick());
	}

	/**
	 * Tests that books can be added and retrieved by ISBN.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	// added
	@Test
	public void testGetBooksByISBN() throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();

		StockBook book1 = new ImmutableStockBook(TEST_ISBN + 1, "Effective Java", "Joshua Bloch", (float) 45, NUM_COPIES, 0, 0, 0, false);
		StockBook book2 = new ImmutableStockBook(TEST_ISBN + 2, "Design Patterns", "Erich Gamma", (float) 55, NUM_COPIES, 0, 0, 0, false);
		booksToAdd.add(book1);
		booksToAdd.add(book2);

		storeManager.addBooks(booksToAdd);

		Set<Integer> isbns = new HashSet<Integer>();
		isbns.add(TEST_ISBN + 1);
		isbns.add(TEST_ISBN + 2);

		List<StockBook> retrievedBooks = storeManager.getBooksByISBN(isbns);
		assertTrue(retrievedBooks.containsAll(booksToAdd) && retrievedBooks.size() == booksToAdd.size());
	}

	/**
	 * Tests removing all books from the store.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	// added
	@Test
	public void testRemoveAllBooks() throws BookStoreException {
		storeManager.removeAllBooks();
		List<StockBook> booksInStore = storeManager.getBooks();
		assertTrue(booksInStore.isEmpty());
	}

	/**
	 * Tests removing books by ISBN.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	// added
	@Test
	public void testRemoveBooks() throws BookStoreException {
		Set<Integer> isbnsToRemove = new HashSet<Integer>();
		isbnsToRemove.add(TEST_ISBN);

		storeManager.removeBooks(isbnsToRemove);

		List<StockBook> booksInStore = storeManager.getBooks();
		assertTrue(booksInStore.isEmpty());
	}

	/**
	 * Tests adding negative copies of a book.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	// added
	@Test
	public void testAddNegativeCopies() throws BookStoreException {
		try {
			storeManager.addCopies(new HashSet<BookCopy>() {
				{
					add(new BookCopy(TEST_ISBN, -5));
				}
			});
			fail("Expected BookStoreException");
		} catch (BookStoreException ex) {
			// Expected
		}
	}

	/**
	 * Tests adding copies to a non-existing book.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	// added
	@Test
	public void testAddCopiesToNonExistingBook() throws BookStoreException {
		try {
			storeManager.addCopies(new HashSet<BookCopy>() {
				{
					add(new BookCopy(TEST_ISBN + 1000, 5));
				}
			});
			fail("Expected BookStoreException");
		} catch (BookStoreException ex) {
			// Expected
		}
	}

	/**
	 * Tests buying books concurrently.
	 *
	 * @throws InterruptedException
	 *             the interruption exception
	 */
	// added
	@Test
	public void testConcurrentBuyBooks() throws InterruptedException, BookStoreException {
		final Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, 1));

		Thread thread1 = new Thread(() -> {
			try {
				client.buyBooks(booksToBuy);
			} catch (BookStoreException e) {
				fail("Exception in thread1");
			}
		});

		Thread thread2 = new Thread(() -> {
			try {
				client.buyBooks(booksToBuy);
			} catch (BookStoreException e) {
				fail("Exception in thread2");
			}
		});

		thread1.start();
		thread2.start();

		thread1.join();
		thread2.join();

		List<StockBook> booksInStore = storeManager.getBooks();
		assertTrue(booksInStore.get(0).getNumCopies() == NUM_COPIES - 2);
	}


	/**
	 * Tests concurrent buy and add operations.
	 */
	// added
	@Test
	public void testConcurrentBuyAndAddCopies() throws InterruptedException, BookStoreException {
		final Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, 1));

		final Set<BookCopy> booksToAdd = new HashSet<BookCopy>();
		booksToAdd.add(new BookCopy(TEST_ISBN, 1));

		Thread thread1 = new Thread(() -> {
			try {
				for (int i = 0; i < 100; i++) {
					client.buyBooks(booksToBuy);
				}
			} catch (BookStoreException e) {
				fail("Exception in thread1");
			}
		});

		Thread thread2 = new Thread(() -> {
			try {
				for (int i = 0; i < 100; i++) {
					storeManager.addCopies(booksToAdd);
				}
			} catch (BookStoreException e) {
				fail("Exception in thread2");
			}
		});

		thread1.start();
		thread2.start();

		thread1.join();
		thread2.join();

		List<StockBook> booksInStore = storeManager.getBooks();
		assertTrue(booksInStore.get(0).getNumCopies() == NUM_COPIES);
	}

	/**
	 * Tests concurrent buy and get operations.
	 */
	// added
	@Test
	public void testConcurrentBuyAndGetBooks() throws InterruptedException, BookStoreException {
		final Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, 1));

		Thread thread1 = new Thread(() -> {
			try {
				for (int i = 0; i < 100; i++) {
					client.buyBooks(booksToBuy);
					storeManager.addCopies(booksToBuy);
				}
			} catch (BookStoreException e) {
				fail("Exception in thread1");
			}
		});

		Thread thread2 = new Thread(() -> {
			try {
				for (int i = 0; i < 100; i++) {
					List<StockBook> booksInStore = storeManager.getBooks();
					for (StockBook book : booksInStore) {
						assertTrue(book.getNumCopies() == NUM_COPIES || book.getNumCopies() == NUM_COPIES - 1);
					}
				}
			} catch (BookStoreException e) {
				fail("Exception in thread2");
			}
		});

		thread1.start();
		thread2.start();

		thread1.join();
		thread2.join();
	}
	/**
	 * Tests that adding a book with a negative number of copies throws an exception.
	 *
	 * @throws BookStoreException the book store exception
	 */
	// added
	@Test
	public void testAddBookWithNegativeCopies() throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN, "Test Book", "Test Author", (float) 10, -5, 0, 0, 0, false));

		try {
			storeManager.addBooks(booksToAdd);
			fail("Adding a book with negative copies should throw an exception.");
		} catch (BookStoreException ex) {
			// Expected behavior
		}
	}

	/**
	 * Tests the removal of all books.
	 *
	 * @throws BookStoreException the book store exception
	 */
	// added
	@Test
	public void testRemoveAllBooks2() throws BookStoreException {
		// Add some books
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(getDefaultBook());
		storeManager.addBooks(booksToAdd);

		// Verify that the book is added
		List<StockBook> booksInStoreBefore = storeManager.getBooks();
		assertTrue(booksInStoreBefore.size() > 0);

		// Remove all books
		storeManager.removeAllBooks();

		// Verify that no books are left in store
		List<StockBook> booksInStoreAfter = storeManager.getBooks();
		assertTrue(booksInStoreAfter.isEmpty());
	}

	/**
	 * Tests that only available copies are bought (checking available quantity after purchase).
	 *
	 * @throws BookStoreException the book store exception
	 */
	// added
	@Test
	public void testBuyAvailableCopies() throws BookStoreException {
		// Add books to store
		addBooks(TEST_ISBN, 10);

		// Try to buy 5 copies
		Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, 5));
		client.buyBooks(booksToBuy);

		// Verify that only the available books are bought
		List<StockBook> booksInStore = storeManager.getBooks();
		StockBook boughtBook = booksInStore.stream()
				.filter(book -> book.getISBN() == TEST_ISBN)
				.findFirst()
				.orElseThrow(() -> new BookStoreException("Book not found in the store"));

		// Check that the number of copies in the store is reduced by 5
		assertTrue(boughtBook.getNumCopies() == 5);
	}

	/**
	 * Tests that adding books with the same ISBN does not duplicate the entries.
	 *
	 * @throws BookStoreException the book store exception
	 */
	// added
	@Test
	public void testAddDuplicateBooks() throws BookStoreException {
		// Add a book
		addBooks(TEST_ISBN, 5);

		// Try adding the same book again
		addBooks(TEST_ISBN, 5);

		// Verify that the book count is 10, not duplicated
		List<StockBook> booksInStore = storeManager.getBooks();
		StockBook book = booksInStore.stream()
				.filter(b -> b.getISBN() == TEST_ISBN)
				.findFirst()
				.orElseThrow(() -> new BookStoreException("Book not found in the store"));

		assertTrue(book.getNumCopies() == 10);
	}

	/**
	 * Tests that retrieving books with an empty list of ISBNs returns an empty result.
	 *
	 * @throws BookStoreException the book store exception
	 */
	@Test
	// added
	public void testGetBooksWithEmptyIsbnList() throws BookStoreException {
		Set<Integer> emptyIsbnList = new HashSet<>();
		List<Book> books = client.getBooks(emptyIsbnList);

		// Verify that the returned list of books is empty
		assertTrue(books.isEmpty());
	}

	/**
	 * Tests the addition of books with varied prices.
	 *
	 * @throws BookStoreException the book store exception
	 */
	// added
	@Test
	public void testAddBooksWithVariedPrices() throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "Cheap Book", "Author A", (float) 5, 10, 0, 0, 0, false));
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2, "Expensive Book", "Author B", (float) 100, 5, 0, 0, 0, false));

		storeManager.addBooks(booksToAdd);

		// Verify that both books are added and can be retrieved
		Set<Integer> isbnList = new HashSet<>();
		isbnList.add(TEST_ISBN + 1);
		isbnList.add(TEST_ISBN + 2);

		List<Book> books = client.getBooks(isbnList);
		assertTrue(books.size() == 2);
	}

	// Test for dirty read
	@Test
	public void testDirtyRead() throws InterruptedException, ExecutionException {

		// Add initial book to the store
		Set<StockBook> books = new HashSet<>();
		books.add(new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", "Donald Knuth", (float) 300, 10, 0, 0, 0, false));
		try{
			storeManager.addBooks(books);
		}
		catch (BookStoreException ignored) {
			fail("Exception when adding book to store");
		}

		ExecutorService executor = Executors.newFixedThreadPool(2);

		// Writer thread - updates the number of copies
		executor.submit(() -> {
			try {
				Set<BookCopy> copies = new HashSet<>();
				copies.add(new BookCopy(TEST_ISBN + 1, 5)); // Add 5 more copies
				storeManager.addCopies(copies);
			} catch (BookStoreException e) {
				e.printStackTrace();
			}
		});

		// Reader thread - tries to read the book's state during the write
		Future<List<StockBook>> readerResult = executor.submit(() -> {
			try {
				return storeManager.getBooks();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		});

		executor.shutdown();
		executor.awaitTermination(5, TimeUnit.SECONDS);

		// Verify the reader does not see an intermediate state
		List<StockBook> booksAfter = readerResult.get();
		StockBook book = booksAfter.get(0);
		// The reader should either see the original state or the fully updated state
		assertTrue(book.getNumCopies() == 10 || book.getNumCopies() == 15);
	}

	// Test for dirty write
	@Test
	public void testDirtyWrite() throws InterruptedException {

		// Add initial book to the store
		Set<StockBook> books = new HashSet<>();
		books.add(new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", "Donald Knuth", (float) 300, 10, 0, 0, 0, false));
		try{
			storeManager.addBooks(books);
		}
		catch (BookStoreException ignored) {
			fail("Exception when adding book to store");
		}

		ExecutorService executor = Executors.newFixedThreadPool(2);

		// Writer thread 1 - adds 5 copies
		executor.submit(() -> {
			try {
				Set<BookCopy> copies = new HashSet<>();
				copies.add(new BookCopy(TEST_ISBN + 1, 5)); // Add 5 more copies
				storeManager.addCopies(copies);
			} catch (BookStoreException e) {
				e.printStackTrace();
			}
		});

		// Writer thread 2 - removes 3 copies
		executor.submit(() -> {
			try {
				Set<BookCopy> copies = new HashSet<>();
				copies.add(new BookCopy(TEST_ISBN + 1, -3)); // Remove 3 copies
				storeManager.addCopies(copies);
			} catch (BookStoreException e) {
				e.printStackTrace();
			}
		});

		executor.shutdown();
		executor.awaitTermination(5, TimeUnit.SECONDS);

		// Verify the final number of copies is consistent
		try{
			List<StockBook> booksAfter = storeManager.getBooks();
			StockBook book = booksAfter.get(0);
			// Final number of copies should be 10 + 5 - 3 = 12
			assertEquals(12, book.getNumCopies());
		}
		catch (BookStoreException ignored) {
			fail("Exception when asserting");
		}

	}
	/**
	 * Tests the behavior when trying to add the same ISBN with different titles.
	 *
	 * @throws BookStoreException the book store exception
	 */
	// added
	//@Test
	//public void testAddBookWithDifferentTitles() throws BookStoreException {
	//	Set<StockBook> booksToAdd = new HashSet<StockBook>();
	//	booksToAdd.add(new ImmutableStockBook(TEST_ISBN, "Book One", "Author A", (float) 10, 5, 0, 0, 0, false));

	//	storeManager.addBooks(booksToAdd);

		// Try to add the same ISBN with a different title
	//	booksToAdd.clear();
	//	booksToAdd.add(new ImmutableStockBook(TEST_ISBN, "Book Two", "Author B", (float) 10, 5, 0, 0, 0, false));

	//	try {
	//		storeManager.addBooks(booksToAdd);
	//		fail("Adding a book with the same ISBN but different title should throw an exception.");
	//	} catch (BookStoreException ex) {
			// Expected behavior
	//	}
	//}


	/**
	 * Tear down after class.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws BookStoreException {
		storeManager.removeAllBooks();

		if (!localTest) {
			((BookStoreHTTPProxy) client).stop();
			((StockManagerHTTPProxy) storeManager).stop();
		}
	}
}
