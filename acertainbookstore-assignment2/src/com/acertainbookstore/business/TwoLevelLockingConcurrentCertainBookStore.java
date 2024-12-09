package com.acertainbookstore.business;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;
import com.acertainbookstore.utils.BookStoreUtility;

import java.util.concurrent.locks.ReadWriteLock; // added
import java.util.concurrent.locks.ReentrantReadWriteLock; // added

/** {@link TwoLevelLockingConcurrentCertainBookStore} implements the {@link BookStore} and
 * {@link StockManager} functionalities.
 * 
 * @see BookStore
 * @see StockManager
 */
public class TwoLevelLockingConcurrentCertainBookStore implements BookStore, StockManager {

	/** The mapping of books from ISBN to {@link BookStoreBook}. */
	private final ReadWriteLock hmLock = new ReentrantReadWriteLock(true);
	private Map<Integer, BookStoreBook> bookMap = null;

	/**
	 * Instantiates a new {@link CertainBookStore}.
	 */
	public TwoLevelLockingConcurrentCertainBookStore() {
		// Constructors are not synchronized
		bookMap = new HashMap<>();
	}
	
	private void validate(StockBook book) throws BookStoreException {
		int isbn = book.getISBN();
		String bookTitle = book.getTitle();
		String bookAuthor = book.getAuthor();
		int noCopies = book.getNumCopies();
		float bookPrice = book.getPrice();


		hmLock.readLock().lock(); // added
		try {
			if (BookStoreUtility.isInvalidISBN(isbn)) { // Check if the book has valid ISBN
				throw new BookStoreException(BookStoreConstants.ISBN + isbn + BookStoreConstants.INVALID);
			}

			if (BookStoreUtility.isEmpty(bookTitle)) { // Check if the book has valid title
				throw new BookStoreException(BookStoreConstants.BOOK + book.toString() + BookStoreConstants.INVALID);
			}

			if (BookStoreUtility.isEmpty(bookAuthor)) { // Check if the book has valid author
				throw new BookStoreException(BookStoreConstants.BOOK + book.toString() + BookStoreConstants.INVALID);
			}

			if (BookStoreUtility.isInvalidNoCopies(noCopies)) { // Check if the book has at least one copy
				throw new BookStoreException(BookStoreConstants.BOOK + book.toString() + BookStoreConstants.INVALID);
			}

			if (bookPrice < 0.0) { // Check if the price of the book is valid
				throw new BookStoreException(BookStoreConstants.BOOK + book.toString() + BookStoreConstants.INVALID);
			}

			if (bookMap.containsKey(isbn)) {// Check if the book is not in stock
				throw new BookStoreException(BookStoreConstants.ISBN + isbn + BookStoreConstants.DUPLICATED);
			}
		} finally {
			hmLock.readLock().unlock(); // added
		}
	}	
	
	private void validate(BookCopy bookCopy) throws BookStoreException {
		int isbn = bookCopy.getISBN();
		int numCopies = bookCopy.getNumCopies();

		hmLock.readLock().lock(); // added
		try {
			validateISBNInStock(isbn); // Check if the book has valid ISBN and in stock

			if (BookStoreUtility.isInvalidNoCopies(numCopies)) { // Check if the number of the book copy is larger than zero
				throw new BookStoreException(BookStoreConstants.NUM_COPIES + numCopies + BookStoreConstants.INVALID);
			}
		} finally {
			hmLock.readLock().unlock(); // added
		}
	}
	
	private void validate(BookEditorPick editorPickArg) throws BookStoreException {
		int isbn = editorPickArg.getISBN();
		hmLock.readLock().lock(); // added
		try {
			validateISBNInStock(isbn); // Check if the book has valid ISBN and in stock
		} finally {
			hmLock.readLock().unlock(); // added
		}
	}
	
	private void validateISBNInStock(Integer ISBN) throws BookStoreException {
		hmLock.readLock().lock(); // added
		try {
			if (BookStoreUtility.isInvalidISBN(ISBN)) { // Check if the book has valid ISBN
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN + BookStoreConstants.INVALID);
			}
			if (!bookMap.containsKey(ISBN)) {// Check if the book is in stock
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN + BookStoreConstants.NOT_AVAILABLE);
			}
		} finally {
			hmLock.readLock().unlock(); // added
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.acertainbookstore.interfaces.StockManager#addBooks(java.util.Set)
	 */
	public void addBooks(Set<StockBook> bookSet) throws BookStoreException {
		if (bookSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		hmLock.writeLock().lock(); // added
		try {
			// Check if all are there
			for (StockBook book : bookSet) {
				validate(book);
			}

			for (StockBook book : bookSet) {
				int isbn = book.getISBN();
				bookMap.put(isbn, new BookStoreBook(book));
			}
		} finally {
			hmLock.writeLock().unlock(); // added
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.acertainbookstore.interfaces.StockManager#addCopies(java.util.Set)
	 */
	public void addCopies(Set<BookCopy> bookCopiesSet) throws BookStoreException {
		int isbn;
		int numCopies;

		if (bookCopiesSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		for (BookCopy bookCopy : bookCopiesSet) {
			validate(bookCopy);
		}

		// BookStoreBook book;

		hmLock.writeLock().lock(); // added
		try {
		// Update the number of copies
			for (BookCopy bookCopy : bookCopiesSet) {
				isbn = bookCopy.getISBN();
				numCopies = bookCopy.getNumCopies();
				// Get the book from the map and acquire its individual lock
				BookStoreBook book = bookMap.get(isbn); // added
				// Check if the book exists (bookMap.get(isbn) might return null) - added
				if (book == null) {
					throw new BookStoreException(BookStoreConstants.ISBN + isbn + BookStoreConstants.NOT_AVAILABLE);
				}

				// Acquire the book-level lock to modify the number of copies in a thread-safe manner
				// book.getLock().lock();
				try {
					// book = bookMap.get(isbn);
					book.addCopies(numCopies);
				} finally {
					// Release the book-level lock after modifying the book
					// book.getLock().unlock();
				}
			}
		} finally {
			hmLock.writeLock().unlock(); // added
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.StockManager#getBooks()
	 */
	public List<StockBook> getBooks() {
		hmLock.readLock().lock(); // added
		try {
			Collection<BookStoreBook> bookMapValues = bookMap.values();

			return bookMapValues.stream()
					.map(book -> book.immutableStockBook())
					.collect(Collectors.toList());
		} finally {
			hmLock.readLock().unlock(); // added
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.acertainbookstore.interfaces.StockManager#updateEditorPicks(java.util
	 * .Set)
	 */
	public void updateEditorPicks(Set<BookEditorPick> editorPicks) throws BookStoreException {
		// Check that all ISBNs that we add/remove are there first.
		if (editorPicks == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		// int isbnValue;

		hmLock.readLock().lock(); // added
		try {
			for (BookEditorPick editorPickArg : editorPicks) {
				validate(editorPickArg);
			}

			for (BookEditorPick editorPickArg : editorPicks) {
				int isbn = editorPickArg.getISBN(); // added
				BookStoreBook book = bookMap.get(isbn); // added
				// bookMap.get(editorPickArg.getISBN()).setEditorPick(editorPickArg.isEditorPick());
				if (book == null) {
					throw new BookStoreException(BookStoreConstants.ISBN + isbn + BookStoreConstants.NOT_AVAILABLE);
				}

				// book.getLock().writeLock().lock();
				try {
					book.setEditorPick(editorPickArg.isEditorPick());
				} finally {
					// Release the write lock for the individual book after modification
					// book.getLock().writeLock().unlock();
				}
			}
		} finally {
			// Release the read lock for the bookMap after the operation
			hmLock.readLock().unlock(); // added
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.BookStore#buyBooks(java.util.Set)
	 */
	public void buyBooks(Set<BookCopy> bookCopiesToBuy) throws BookStoreException {
		if (bookCopiesToBuy == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		// Check that all ISBNs that we buy are there first.
		int isbn;
		BookStoreBook book;
		Boolean saleMiss = false;

		Map<Integer, Integer> salesMisses = new HashMap<>();

		hmLock.readLock().lock(); // added
		try {
			for (BookCopy bookCopyToBuy : bookCopiesToBuy) {
				isbn = bookCopyToBuy.getISBN();

				validate(bookCopyToBuy);

				book = bookMap.get(isbn);

				if (!book.areCopiesInStore(bookCopyToBuy.getNumCopies())) {
					// If we cannot sell the copies of the book, it is a miss.
					salesMisses.put(isbn, bookCopyToBuy.getNumCopies() - book.getNumCopies());
					saleMiss = true;
				}
			}

			// We throw exception now since we want to see how many books in the
			// order incurred misses which is used by books in demand
			if (saleMiss) {
				for (Map.Entry<Integer, Integer> saleMissEntry : salesMisses.entrySet()) {
					// book = bookMap.get(saleMissEntry.getKey());
					// book.addSaleMiss(saleMissEntry.getValue());
					isbn = saleMissEntry.getKey(); // added
					book = bookMap.get(isbn);  // added
					// book.getLock().writeLock().lock();  // added
					try {
						book.addSaleMiss(saleMissEntry.getValue());
					} finally {
						// book.getLock().writeLock().unlock();
					}
				}
				throw new BookStoreException(BookStoreConstants.BOOK + BookStoreConstants.NOT_AVAILABLE);
			}

			// Then make the purchase.
			for (BookCopy bookCopyToBuy : bookCopiesToBuy) {
				book = bookMap.get(bookCopyToBuy.getISBN());
				// book.getLock().writeLock().lock(); // added
				try {
					book.buyCopies(bookCopyToBuy.getNumCopies());
				} finally {
					// book.getLock().writeLock().unlock(); // added
				}
			}
		} finally {
			hmLock.readLock().unlock(); // added
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.acertainbookstore.interfaces.StockManager#getBooksByISBN(java.util.
	 * Set)
	 */
	public List<StockBook> getBooksByISBN(Set<Integer> isbnSet) throws BookStoreException {
		if (isbnSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		hmLock.readLock().lock(); // added
		try {
			for (Integer ISBN : isbnSet) {
				validateISBNInStock(ISBN);
			}

			return isbnSet.stream()
					.map(isbn -> {
						// For thread safety, we can acquire the individual book lock (if needed)
						BookStoreBook book = bookMap.get(isbn);
						if (book == null) {
							// throw new BookStoreException(BookStoreConstants.ISBN + isbn + BookStoreConstants.NOT_AVAILABLE);
						}

						// Return the immutable representation of the book
						// book.getLock().readLock().lock(); // added
						try {
							// Access the book data
							return book.immutableStockBook();
						} finally {
							// book.getLock().readLock().unlock(); // added
						}

					})
					.collect(Collectors.toList());
		} finally {
			hmLock.readLock().unlock(); // added
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.BookStore#getBooks(java.util.Set)
	 */
	public List<Book> getBooks(Set<Integer> isbnSet) throws BookStoreException {
		if (isbnSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		hmLock.readLock().lock(); // added
		try {
			// Check that all ISBNs that we rate are there to start with.
			for (Integer ISBN : isbnSet) {
				validateISBNInStock(ISBN);
			}

			return isbnSet.stream()
					.map(isbn -> {
						// Retrieve the book from the map
						BookStoreBook book = bookMap.get(isbn);
						if (book == null) {
							// throw new BookStoreException(BookStoreConstants.ISBN + isbn + BookStoreConstants.NOT_AVAILABLE);
						}

						// For thread safety, you can acquire the individual book lock (if needed)
						// If bookMap is only being read, individual book locks may not be necessary
						// book.getLock().readLock().lock(); // added
						try {
							return book.immutableBook();
						} finally {
							// book.getLock().readLock().unlock(); // added
						}

					})
					.collect(Collectors.toList());
		} finally {
			hmLock.readLock().unlock(); // added
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.BookStore#getEditorPicks(int)
	 */
	public List<Book> getEditorPicks(int numBooks) throws BookStoreException {
		if (numBooks < 0) {
			throw new BookStoreException("numBooks = " + numBooks + ", but it must be positive");
		}

		hmLock.readLock().lock(); // added
		try {
			List<BookStoreBook> listAllEditorPicks = bookMap.entrySet().stream()
					.map(pair -> pair.getValue())
					.filter(book -> {
						// book.getLock().readLock().lock();
						try {
							return book.isEditorPick();
						} finally {
							// book.getLock().readLock().unlock(); // added
						}
					})
					.collect(Collectors.toList());

			// Find numBooks random indices of books that will be picked.
			Random rand = new Random();
			Set<Integer> tobePicked = new HashSet<>();
			int rangePicks = listAllEditorPicks.size();

			if (rangePicks <= numBooks) {

				// We need to add all books.
				for (int i = 0; i < listAllEditorPicks.size(); i++) {
					tobePicked.add(i);
				}
			} else {

				// We need to pick randomly the books that need to be returned.
				int randNum;

				while (tobePicked.size() < numBooks) {
					randNum = rand.nextInt(rangePicks);
					tobePicked.add(randNum);
				}
			}

			// Return all the books by the randomly chosen indices.
			return tobePicked.stream()
					.map(index -> listAllEditorPicks.get(index).immutableBook())
					.collect(Collectors.toList());
		} finally {
			hmLock.readLock().unlock(); // added
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.BookStore#getTopRatedBooks(int)
	 */
	@Override
	public List<Book> getTopRatedBooks(int numBooks) throws BookStoreException {
		throw new BookStoreException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.StockManager#getBooksInDemand()
	 */
	@Override
	public List<StockBook> getBooksInDemand() throws BookStoreException {
		throw new BookStoreException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.BookStore#rateBooks(java.util.Set)
	 */
	@Override
	public void rateBooks(Set<BookRating> bookRating) throws BookStoreException {
		throw new BookStoreException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.StockManager#removeAllBooks()
	 */
	public void removeAllBooks() throws BookStoreException {
		hmLock.writeLock().lock(); // added
		try {
			bookMap.clear();
		} finally {
			hmLock.writeLock().unlock(); // added
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.acertainbookstore.interfaces.StockManager#removeBooks(java.util.Set)
	 */
	public void removeBooks(Set<Integer> isbnSet) throws BookStoreException {
		if (isbnSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		hmLock.readLock().lock(); // added
		try {
			for (Integer ISBN : isbnSet) {
				if (BookStoreUtility.isInvalidISBN(ISBN)) {
					throw new BookStoreException(BookStoreConstants.ISBN + ISBN + BookStoreConstants.INVALID);
				}

				if (!bookMap.containsKey(ISBN)) {
					throw new BookStoreException(BookStoreConstants.ISBN + ISBN + BookStoreConstants.NOT_AVAILABLE);
				}
			}
		} finally {
			hmLock.readLock().unlock(); // added
		}

		hmLock.writeLock().lock(); // added
		try {
			for (int isbn : isbnSet) {
				bookMap.remove(isbn);
			}
		} finally {
			hmLock.writeLock().unlock(); // added
		}
	}
}
