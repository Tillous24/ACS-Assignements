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

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/** {@link TwoLevelLockingConcurrentCertainBookStore} implements the {@link BookStore} and
 * {@link StockManager} functionalities.
 * 
 * This class employs a two-level locking strategy with a read-write lock for the database
 * and individual read-write locks for each book. The top-level lock is acquired in exclusive
 * mode for operations performing inserts or deletes, and in intention (read) mode for all
 * other operations. Each book in the database has its own read-write lock, acquired in shared
 * mode for read operations and in exclusive mode for write operations.
 * 
 * @see BookStore
 * @see StockManager
 */
public class TwoLevelLockingConcurrentCertainBookStore implements BookStore, StockManager {

	/** The mapping of books from ISBN to {@link BookStoreBook}. */
	private final ReadWriteLock dbLock = new ReentrantReadWriteLock(true);
	private Map<Integer, BookStoreBook> bookMap = null;
	private Map<Integer, ReadWriteLock> bookLocks = null;

	/**
	 * Instantiates a new {@link CertainBookStore}.
	 */
	public TwoLevelLockingConcurrentCertainBookStore() {
		// Constructors are not synchronized
		bookMap = new HashMap<>();
		bookLocks = new HashMap<>();
	}
	
	private void validate(StockBook book) throws BookStoreException {
		int isbn = book.getISBN();
		String bookTitle = book.getTitle();
		String bookAuthor = book.getAuthor();
		int noCopies = book.getNumCopies();
		float bookPrice = book.getPrice();

		dbLock.readLock().lock();
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
			dbLock.readLock().unlock();
		}
	}	
	
	private void validate(BookCopy bookCopy) throws BookStoreException {
		int isbn = bookCopy.getISBN();
		int numCopies = bookCopy.getNumCopies();

		dbLock.readLock().lock();
		try {
			validateISBNInStock(isbn); // Check if the book has valid ISBN and in stock

			if (BookStoreUtility.isInvalidNoCopies(numCopies)) { // Check if the number of the book copy is larger than zero
				throw new BookStoreException(BookStoreConstants.NUM_COPIES + numCopies + BookStoreConstants.INVALID);
			}
		} finally {
			dbLock.readLock().unlock();
		}
	}
	
	private void validate(BookEditorPick editorPickArg) throws BookStoreException {
		int isbn = editorPickArg.getISBN();
		dbLock.readLock().lock();
		try {
			validateISBNInStock(isbn); // Check if the book has valid ISBN and in stock
		} finally {
			dbLock.readLock().unlock();
		}
	}
	
	private void validateISBNInStock(Integer ISBN) throws BookStoreException {
		dbLock.readLock().lock();
		try {
			if (BookStoreUtility.isInvalidISBN(ISBN)) { // Check if the book has valid ISBN
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN + BookStoreConstants.INVALID);
			}
			if (!bookMap.containsKey(ISBN)) {// Check if the book is in stock
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN + BookStoreConstants.NOT_AVAILABLE);
			}
		} finally {
			dbLock.readLock().unlock();
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

		dbLock.writeLock().lock();
		try {
			// Check if all are there
			for (StockBook book : bookSet) {
				validate(book);
			}

			for (StockBook book : bookSet) {
				int isbn = book.getISBN();
				bookMap.put(isbn, new BookStoreBook(book));
				bookLocks.put(isbn, new ReentrantReadWriteLock(true));
			}
		} finally {
			dbLock.writeLock().unlock();
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

		dbLock.readLock().lock();
		try {
			for (BookCopy bookCopy : bookCopiesSet) {
				isbn = bookCopy.getISBN();
				numCopies = bookCopy.getNumCopies();
				BookStoreBook book = bookMap.get(isbn);
				ReadWriteLock bookLock = bookLocks.get(isbn);
				bookLock.writeLock().lock();
				try {
					book.addCopies(numCopies);
				} finally {
					bookLock.writeLock().unlock();
				}
			}
		} finally {
			dbLock.readLock().unlock();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.StockManager#getBooks()
	 */
	public List<StockBook> getBooks() {
		dbLock.readLock().lock();
		try {
			Collection<BookStoreBook> bookMapValues = bookMap.values();

			return bookMapValues.stream()
					.map(book -> book.immutableStockBook())
					.collect(Collectors.toList());
		} finally {
			dbLock.readLock().unlock();
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

		dbLock.readLock().lock();
		try {
			for (BookEditorPick editorPickArg : editorPicks) {
				validate(editorPickArg);
			}

			for (BookEditorPick editorPickArg : editorPicks) {
				int isbn = editorPickArg.getISBN();
				BookStoreBook book = bookMap.get(isbn);
				ReadWriteLock bookLock = bookLocks.get(isbn);
				bookLock.writeLock().lock();
				try {
					book.setEditorPick(editorPickArg.isEditorPick());
				} finally {
					bookLock.writeLock().unlock();
				}
			}
		} finally {
			dbLock.readLock().unlock();
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

		dbLock.readLock().lock();
		try {
			for (BookCopy bookCopyToBuy : bookCopiesToBuy) {
				isbn = bookCopyToBuy.getISBN();

				validate(bookCopyToBuy);

				book = bookMap.get(isbn);
				ReadWriteLock bookLock = bookLocks.get(isbn);
				bookLock.readLock().lock();
				try {
					if (!book.areCopiesInStore(bookCopyToBuy.getNumCopies())) {
						// If we cannot sell the copies of the book, it is a miss.
						salesMisses.put(isbn, bookCopyToBuy.getNumCopies() - book.getNumCopies());
						saleMiss = true;
					}
				} finally {
					bookLock.readLock().unlock();
				}
			}

			// We throw exception now since we want to see how many books in the
			// order incurred misses which is used by books in demand
			if (saleMiss) {
				for (Map.Entry<Integer, Integer> saleMissEntry : salesMisses.entrySet()) {
					isbn = saleMissEntry.getKey();
					book = bookMap.get(isbn);
					ReadWriteLock bookLock = bookLocks.get(isbn);
					bookLock.writeLock().lock();
					try {
						book.addSaleMiss(saleMissEntry.getValue());
					} finally {
						bookLock.writeLock().unlock();
					}
				}
				throw new BookStoreException(BookStoreConstants.BOOK + BookStoreConstants.NOT_AVAILABLE);
			}

			// Then make the purchase.
			for (BookCopy bookCopyToBuy : bookCopiesToBuy) {
				book = bookMap.get(bookCopyToBuy.getISBN());
				ReadWriteLock bookLock = bookLocks.get(bookCopyToBuy.getISBN());
				bookLock.writeLock().lock();
				try {
					book.buyCopies(bookCopyToBuy.getNumCopies());
				} finally {
					bookLock.writeLock().unlock();
				}
			}
		} finally {
			dbLock.readLock().unlock();
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

		dbLock.readLock().lock();
		try {
			for (Integer ISBN : isbnSet) {
				validateISBNInStock(ISBN);
			}

			return isbnSet.stream()
					.map(isbn -> {
						BookStoreBook book = bookMap.get(isbn);
						ReadWriteLock bookLock = bookLocks.get(isbn);
						bookLock.readLock().lock();
						try {
							return book.immutableStockBook();
						} finally {
							bookLock.readLock().unlock();
						}
					})
					.collect(Collectors.toList());
		} finally {
			dbLock.readLock().unlock();
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

		dbLock.readLock().lock();
		try {
			// Check that all ISBNs that we rate are there to start with.
			for (Integer ISBN : isbnSet) {
				validateISBNInStock(ISBN);
			}

			return isbnSet.stream()
					.map(isbn -> {
						BookStoreBook book = bookMap.get(isbn);
						ReadWriteLock bookLock = bookLocks.get(isbn);
						bookLock.readLock().lock();
						try {
							return book.immutableBook();
						} finally {
							bookLock.readLock().unlock();
						}
					})
					.collect(Collectors.toList());
		} finally {
			dbLock.readLock().unlock();
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

		dbLock.readLock().lock();
		try {
			List<BookStoreBook> listAllEditorPicks = bookMap.entrySet().stream()
					.map(pair -> pair.getValue())
					.filter(book -> {
						ReadWriteLock bookLock = bookLocks.get(book.getISBN());
						bookLock.readLock().lock();
						try {
							return book.isEditorPick();
						} finally {
							bookLock.readLock().unlock();
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
			dbLock.readLock().unlock();
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
		dbLock.writeLock().lock();
		try {
			bookMap.clear();
			bookLocks.clear();
		} finally {
			dbLock.writeLock().unlock();
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

		dbLock.readLock().lock();
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
			dbLock.readLock().unlock();
		}

		dbLock.writeLock().lock();
		try {
			for (int isbn : isbnSet) {
				bookMap.remove(isbn);
				bookLocks.remove(isbn);
			}
		} finally {
			dbLock.writeLock().unlock();
		}
	}
}
