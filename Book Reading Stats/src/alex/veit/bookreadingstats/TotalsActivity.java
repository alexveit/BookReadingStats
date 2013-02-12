package alex.veit.bookreadingstats;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class TotalsActivity extends Activity {

	private ArrayList<BookStats> _myBooks;

	private String getReading() {
		final int read = getReadInt();
		final int reading = _myBooks.size() - read;
		return Integer.toString(reading);
	}

	private int getReadInt() {
		int read = 0;
		for (int i = 0; i < _myBooks.size(); i++) {
			if (_myBooks.get(i).isRead()) {
				read++;
			}
		}
		return read;
	}

	private void launchChart() {
		final Intent intent = new Intent(this, PieChartActivity.class);
		intent.putParcelableArrayListExtra(MainActivity.BOOK_LIST, _myBooks);
		startActivityForResult(intent, 0);

	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_totals);
		_myBooks = getIntent().getExtras().getParcelableArrayList(
				MainActivity.BOOK_LIST);
		populateTextView();
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_totals, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
			case R.id.cancel :
				onBackPressed();
				return true;
			case R.id.chart :
				launchChart();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void populateTextView() {
		final TextView books = (TextView) findViewById(R.id.textViewBooks);
		final TextView read = (TextView) findViewById(R.id.textViewRead);
		final TextView reading = (TextView) findViewById(R.id.textViewReading);
		final TextView totalElapsed = (TextView) findViewById(R.id.textViewTotalElapsed);
		final TextView totalPages = (TextView) findViewById(R.id.textViewTotaPagesB);
		final TextView pagesRead = (TextView) findViewById(R.id.textViewPagesReadB);
		final TextView minPage = (TextView) findViewById(R.id.textViewTMinPage);
		final TextView pageMin = (TextView) findViewById(R.id.textViewTPageMin);
		final TextView pagesLeft = (TextView) findViewById(R.id.textViewPagesLeftB);

		books.setText(Integer.toString(_myBooks.size()));
		read.setText(Integer.toString(getReadInt()));
		reading.setText(getReading());
		totalElapsed.setText(BookStats.getTotalElapsed(_myBooks));
		totalPages.setText(Integer.toString(BookStats.getTotalPages(_myBooks)));
		pagesRead.setText(Integer.toString(BookStats.getPagesRead(_myBooks)));
		pagesLeft.setText(BookStats.getPagesLeft(_myBooks));
		minPage.setText(BookStats.getTotalMinPage(_myBooks));
		pageMin.setText(BookStats.getTotalPageMin(_myBooks));

	}

}
