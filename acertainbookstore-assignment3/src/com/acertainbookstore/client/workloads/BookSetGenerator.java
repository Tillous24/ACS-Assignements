package com.acertainbookstore.client.workloads;

import java.util.*;

import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.StockBook;

/**
 * Helper class to generate stockbooks and isbns modelled similar to Random
 * class
 */
public class BookSetGenerator {

	public BookSetGenerator() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Returns num randomly selected isbns from the input set
	 *
	 * @param isbns : the original set of isbns to choose from
	 * @param num : the number of random isbns to return
	 * @return A set of isbns (integers)
	 */
	public Set<Integer> sampleFromSetOfISBNs(Set<Integer> isbns, int num) {
		if (num > isbns.size()) {
			return isbns;
		}
		else {
			List<Integer> isbnList = new ArrayList<Integer>(isbns);
			Collections.shuffle(isbnList);

			// Select the first n elements from te shuffled list
			return  new HashSet<Integer>(isbnList.subList(0,num));
		}
	}

	/**
	 * Return num stock books. For now return an ImmutableStockBook
	 * 
	 * @param num : the number of books in set
	 * @return Set<StockBook>: a set of random created books
	 */
	public Set<StockBook> nextSetOfStockBooks(int num) {
		Set<StockBook> books = new HashSet<StockBook>();

		for (int i = 0; i < num; i++) {
			books.add(getRandomStockBook());
		}
		return books;
	}

	public StockBook getRandomStockBook() {
		return  new ImmutableStockBook(
				getRandomISBN(),
				getRandomTitle(),
				getRandomAuthor(),
				getRandomPrice(),
				getRandomCopies(),
				0, 0,0,
				getRandomEditorPicks()
		);
	}
	private int getRandomISBN(){
		Random rand = new Random();
		int min = 1000000;
		int max = 9999999;
		return rand.nextInt((max - min) + 1) + min;
	}


	private String getRandomTitle(){
		Random rand = new Random();
		int min = 10;
		int max = 50;
		int length = rand.nextInt((max - min) + 1) + min;
		String title = "";
		for (int i = 0; i < length; i++) {
			title += (char) (rand.nextInt(26) + 'A');
		}
		return title;
	}

	private String getRandomAuthor(){
		Random rand = new Random();
		int min = 10;
		int max = 50;
		int length = rand.nextInt((max - min) + 1) + min;
		String author = "";
		for (int i = 0; i < length; i++) {
			author += (char) (rand.nextInt(26) + 'A');
		}
		return author;
	}

	private float getRandomPrice(){
		Random rand = new Random();
		int min = 500;
		int max = 15000;
		int price = rand.nextInt((max - min) + 1) + min;
		return price / 100f;
	}

	private int getRandomCopies(){
		Random rand = new Random();
		int min = 1;
		int max = 20;
		return rand.nextInt((max - min) + 1) + min;
	}

	/**
	 * Choose editor picks with 10% probability
	 * @return Boolean value
	 */
	private boolean getRandomEditorPicks(){
		Random rand = new Random();
		return rand.nextInt(10) == 0;
	}
}
