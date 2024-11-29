package com.acertainbookstore.client.tests;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.acertainbookstore.business.*;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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

	/**
	 * Test: Rate book with success one time
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 *
	 **/
	@Test
	public void testRateDefaultBookOneTime() throws BookStoreException {
		/* Create a BookRating set */
		HashSet<BookRating> ratings = new HashSet<BookRating>();
		/* Example Rating */
		ratings.add(new BookRating(TEST_ISBN, 4));

		try {
			/* Rate the book */
			client.rateBooks(ratings);

			/* Validate the results */
			Set<Integer> isbnSet = new HashSet<>();
			isbnSet.add(TEST_ISBN);
			List<StockBook> listBooks = storeManager.getBooksByISBN(isbnSet);
			StockBook ratedBook = listBooks.get(0);
			assertEquals(4, ratedBook.getTotalRating());
			assertEquals(1, ratedBook.getNumTimesRated());

		}
		catch (BookStoreException e) {
			fail("Unexpected exception: " + e.getMessage());
		}
	}

	/**
	 * Test: Rate book with success several time
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 *
	 **/
	@Test
	public void testRateDefaultBookSeveralTimes() throws BookStoreException {

		/* Create a BookRating sets */
		Set<BookRating> ratingsSet1 = new HashSet<BookRating>();
		Set<BookRating> ratingsSet2 = new HashSet<BookRating>();
		Set<BookRating> ratingsSet3 = new HashSet<BookRating>();
		Set<BookRating> ratingsSet4 = new HashSet<BookRating>();
		Set<BookRating> ratingsSet5 = new HashSet<BookRating>();
		/* Example Rating */
		ratingsSet1.add(new BookRating(TEST_ISBN, 1));
		ratingsSet2.add(new BookRating(TEST_ISBN, 2));
		ratingsSet3.add(new BookRating(TEST_ISBN, 3));
		ratingsSet4.add(new BookRating(TEST_ISBN, 4));
		ratingsSet5.add(new BookRating(TEST_ISBN, 5));

		try {
			/* Rate the book */
			client.rateBooks(ratingsSet1);
			client.rateBooks(ratingsSet2);
			client.rateBooks(ratingsSet3);
			client.rateBooks(ratingsSet4);
			client.rateBooks(ratingsSet5);

			/* Validate the results */
			Set<Integer> isbnSet = new HashSet<>();
			isbnSet.add(TEST_ISBN);
			List<StockBook> listBooks = storeManager.getBooksByISBN(isbnSet);
			StockBook ratedBook = listBooks.get(0);
			assertEquals(15, ratedBook.getTotalRating());
			assertEquals(5, ratedBook.getNumTimesRated());

		}
		catch (BookStoreException e) {
			fail("Unexpected exception: " + e.getMessage());
		}
	}

	/**
	 * Test: Rate book with non-existing ISBN
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 *
	 **/
	@Test
	public void testRateBookNonExistingISBN() throws BookStoreException {
		/* Create a BookRating set */
		HashSet<BookRating> ratings = new HashSet<BookRating>();
		/* Example Rating */
		ratings.add(new BookRating(TEST_ISBN-1, 4));

		try {
			/* Rate the book */
			client.rateBooks(ratings);
			fail("Expected BookStoreException to be thrown");
		}
		catch (BookStoreException e) {
			assertEquals("Book with ISBN " + (TEST_ISBN - 1) + " not found",e.getMessage());
		}
	}

	/**
	 * Test: Rate book with negative ISBN
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 *
	 **/
	@Test
	public void testRateBookNegativeISBN() throws BookStoreException {
		/* Create a BookRating set */
		HashSet<BookRating> ratings = new HashSet<BookRating>();
		/* Example Rating */
		ratings.add(new BookRating(-1, 4));

		try {
			/* Rate the book */
			client.rateBooks(ratings);
			fail("Expected BookStoreException to be thrown");
		}
		catch (BookStoreException e) {
			assertEquals("Invalid input: ISBN must be positive and rating must be between 0 and 5",e.getMessage());
		}
	}


	/**
	 * Test: Rate book with rating below zero
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 *
	 **/
	@Test
	public void testRateDefaultBookLowRating() throws BookStoreException {
		/* Create a BookRating set */
		HashSet<BookRating> ratings = new HashSet<BookRating>();
		/* Example Rating */
		ratings.add(new BookRating(TEST_ISBN, -1));

		try {
			/* Rate the book */
			client.rateBooks(ratings);
			fail("Expected BookStoreException to be thrown");
		}
		catch (BookStoreException e) {
			assertEquals("Invalid input: ISBN must be positive and rating must be between 0 and 5",e.getMessage());
		}
	}

	/**
	 * Test: Rate book with rating above five
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 *
	 **/
	@Test
	public void testRateDefaultBookHighRating() throws BookStoreException {
		/* Create a BookRating set */
		HashSet<BookRating> ratings = new HashSet<BookRating>();
		/* Example Rating */
		ratings.add(new BookRating(TEST_ISBN, 6));

		try {
			/* Rate the book */
			client.rateBooks(ratings);
			fail("Expected BookStoreException to be thrown");
		}
		catch (BookStoreException e) {
			assertEquals("Invalid input: ISBN must be positive and rating must be between 0 and 5",e.getMessage());
		}
	}

	/**
	 * Test: All-or-nothing semantics
	 *      Rate books with success one time and with failure the next time
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 *
	 **/
	@Test
	public void testRateBooksAllOrNothing() throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", "Donald Knuth",
				(float) 300, NUM_COPIES, 0, 0, 0, false));
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 0, 0, false));

		storeManager.addBooks(booksToAdd);

		/* Create a BookRating sets */
		Set<BookRating> ratingsSet1 = new HashSet<BookRating>();
		Set<BookRating> ratingsSet2 = new HashSet<BookRating>();

		/* Example Rating */
		ratingsSet1.add(new BookRating(TEST_ISBN, 1));
		ratingsSet1.add(new BookRating(TEST_ISBN + 1, 2));
		ratingsSet1.add(new BookRating(TEST_ISBN + 2, 3));
		ratingsSet2.add(new BookRating(TEST_ISBN, 1));
		ratingsSet2.add(new BookRating(TEST_ISBN + 1, 2));
		ratingsSet2.add(new BookRating(TEST_ISBN - 2, 3));

		try {
			/* Rate the book */
			client.rateBooks(ratingsSet1);
			client.rateBooks(ratingsSet2);
			fail("Expected BookStoreException to be thrown");
		}
		catch (BookStoreException e) {
			assertEquals("Book with ISBN " + (TEST_ISBN - 2) + " not found",e.getMessage());

			/* Validate the results */
			Set<Integer> isbnSet1 = new HashSet<>();
			Set<Integer> isbnSet2 = new HashSet<>();
			Set<Integer> isbnSet3 = new HashSet<>();
			isbnSet1.add(TEST_ISBN);
			isbnSet2.add(TEST_ISBN + 1);
			isbnSet3.add(TEST_ISBN + 2);
			List<StockBook> listBooks1 = storeManager.getBooksByISBN(isbnSet1);
			List<StockBook> listBooks2 = storeManager.getBooksByISBN(isbnSet2);
			List<StockBook> listBooks3 = storeManager.getBooksByISBN(isbnSet3);
			assertEquals(1, listBooks1.get(0).getTotalRating());
			assertEquals(1, listBooks1.get(0).getNumTimesRated());
			assertEquals(2, listBooks2.get(0).getTotalRating());
			assertEquals(1, listBooks2.get(0).getNumTimesRated());
			assertEquals(3, listBooks3.get(0).getTotalRating());
			assertEquals(1, listBooks3.get(0).getNumTimesRated());
		}
	}

	/**
	 * Test: Get negative number of top rated books
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 *
	 **/
	@Test
	public void testGetTopRatedBooksNegativeK() throws BookStoreException {
		int k = -1;
		try {
			/* Get top rated books */
			client.getTopRatedBooks(k);
			fail("Expected BookStoreException to be thrown");
		}
		catch (BookStoreException e) {
			assertEquals("Number of books requested must be non-negative.",e.getMessage());
		}
	}

	/**
	 * Test: Get best rated book
	 *			and test if instance of ImmutableBook
	 * @throws BookStoreException
	 *             the book store exception
	 *
	 **/
	@Test
	public void testGetTopRatedBooksNumberOne() throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", "Donald Knuth",
				(float) 300, NUM_COPIES, 0, 10, 40, false));
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 1, 5, false));
		storeManager.addBooks(booksToAdd);

		int k = 1;

		try {
			/* Get best rated book */
			List<Book> booksTopRated = client.getTopRatedBooks(k);
			assertEquals(TEST_ISBN + 2, booksTopRated.get(0).getISBN());
			assertEquals(true, booksTopRated.get(0) instanceof ImmutableBook);
		}
		catch (BookStoreException e) {
			fail("Unexpected exception: " + e.getMessage());
		}
	}

	/**
	 * Test: Get K top rated book
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 *
	 **/
	@Test
	public void testGetTopRatedBooksBestThree() throws BookStoreException {
		// avr rate = 4
		StockBook book1 = new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", "Donald Knuth",
				(float) 300, NUM_COPIES, 0, 10, 40, false);
		// avr rate = 5
		StockBook book2 = new ImmutableStockBook(TEST_ISBN + 2, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 1, 5, false);
		// avr rate = 1
		StockBook book3 = new ImmutableStockBook(TEST_ISBN + 3, "The Art of Computer Programming", "Donald Knuth",
				(float) 300, NUM_COPIES, 0, 10, 10, false);
		// avr rate = 2
		StockBook book4 = new ImmutableStockBook(TEST_ISBN + 4, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 10, 20, false);

		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(book1);
		booksToAdd.add(book2);
		booksToAdd.add(book3);
		booksToAdd.add(book4);
		storeManager.addBooks(booksToAdd);

		int k = 3;

		try {
			/* Get 3 best rated book */
			List<Book> booksTopRated = client.getTopRatedBooks(k);
			assertEquals(k, booksTopRated.size());
			assertEquals(true, booksTopRated.contains(book1));
			assertEquals(true, booksTopRated.contains(book2));
			assertEquals(false, booksTopRated.contains(book3));
			assertEquals(true, booksTopRated.contains(book4));
		}
		catch (BookStoreException e) {
			fail("Unexpected exception: " + e.getMessage());
		}
	}

	/**
	 * Test: Get all books when K>N
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 *
	 **/
	@Test
	public void testGetTopRatedBooksKLargerThanN() throws BookStoreException {
		// avr rate = 4
		StockBook book1 = new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", "Donald Knuth",
				(float) 300, NUM_COPIES, 0, 10, 40, false);
		// avr rate = 5
		StockBook book2 = new ImmutableStockBook(TEST_ISBN + 2, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 1, 5, false);
		// avr rate = 1
		StockBook book3 = new ImmutableStockBook(TEST_ISBN + 3, "The Art of Computer Programming", "Donald Knuth",
				(float) 300, NUM_COPIES, 0, 10, 10, false);
		// avr rate = 2
		StockBook book4 = new ImmutableStockBook(TEST_ISBN + 4, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 10, 20, false);
		// avr rate = 2
		StockBook book5 = new ImmutableStockBook(TEST_ISBN + 5, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 10, 20, false);

		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(book1);
		booksToAdd.add(book2);
		booksToAdd.add(book3);
		booksToAdd.add(book4);
		booksToAdd.add(book5);
		storeManager.addBooks(booksToAdd);

		int n = storeManager.getBooks().size() - 1; // Default book not rated
		int k = n + 10;

		try {
			/* Get all rated book */
			List<Book> booksTopRated = client.getTopRatedBooks(k);
			assertEquals(n, booksTopRated.size());
			assertEquals(true, booksTopRated.contains(book1));
			assertEquals(true, booksTopRated.contains(book2));
			assertEquals(true, booksTopRated.contains(book3));
			assertEquals(true, booksTopRated.contains(book4));
			assertEquals(true, booksTopRated.contains(book5));
			assertEquals(false, booksTopRated.contains(getDefaultBook()));
		}
		catch (BookStoreException e) {
			fail("Unexpected exception: " + e.getMessage());
		}
	}

	/**
	 * Test: Get all books in demand
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 *
	 **/
	@Test
	public void testGetBooksInDemand() throws BookStoreException {
		// Not in demand
		StockBook book1 = new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", "Donald Knuth",
				(float) 300, NUM_COPIES, 0, 10, 40, false);
		// In demand
		StockBook book2 = new ImmutableStockBook(TEST_ISBN + 2, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 2, 1, 5, false);
		// Not in demand
		StockBook book3 = new ImmutableStockBook(TEST_ISBN + 3, "The Art of Computer Programming", "Donald Knuth",
				(float) 300, NUM_COPIES, 0, 10, 10, false);
		// In demand
		StockBook book4 = new ImmutableStockBook(TEST_ISBN + 4, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 1, 10, 20, false);
		// Not in demand
		StockBook book5 = new ImmutableStockBook(TEST_ISBN + 5, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 10, 20, false);

		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(book1);
		booksToAdd.add(book2);
		booksToAdd.add(book3);
		booksToAdd.add(book4);
		booksToAdd.add(book5);
		storeManager.addBooks(booksToAdd);

		try {
			/* Get books in demand */
			List<StockBook> booksInDemand = storeManager.getBooksInDemand();
			assertEquals(2, booksInDemand.size());
			assertEquals(false, booksInDemand.contains(book1));
			assertEquals(true, booksInDemand.contains(book2));
			assertEquals(false, booksInDemand.contains(book3));
			assertEquals(true, booksInDemand.contains(book4));
			assertEquals(false, booksInDemand.contains(book5));
			assertEquals(false, booksInDemand.contains(getDefaultBook()));
			assertEquals(true, booksInDemand.get(0) instanceof ImmutableStockBook);
			assertEquals(true, booksInDemand.get(1) instanceof ImmutableStockBook);
		}
		catch (BookStoreException e) {
			fail("Unexpected exception: " + e.getMessage());
		}
	}


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
