package alex.veit.bookreadingstats;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class Utils {

	public static String digitFixer(final int number) {
		String myText;
		if (number < 10) {
			myText = "0" + number;
		} else {
			myText = Integer.toString(number);
		}
		return myText;
	}

	public static String getDoublePercentageString(final int i,
			final ArrayList<BookStats> myBooks) {
		final int totalMinutes = BookStats.getTotalMinutes(myBooks);
		final int thisMinutes = myBooks.get(i).getElapsedInt();
		final DecimalFormat dec = new DecimalFormat("#.#");
		double percentage = 0;
		if (totalMinutes > 0) {
			percentage = (double) (thisMinutes * 100) / totalMinutes;
		}
		return dec.format(percentage) + "%";

	}

	public static int getIntPercentage(final int i,
			final ArrayList<BookStats> myBooks) {
		final int totalMinutes = BookStats.getTotalMinutes(myBooks);
		final int thisMinutes = myBooks.get(i).getElapsedInt();
		return (thisMinutes * 100) / totalMinutes;
	}

	public static long getRightID(final ArrayList<BookStats> books,
			final ArrayList<BookStats> booksTemp, final long tempId) {
		return books.indexOf(booksTemp.get((int) tempId));
	}
}
