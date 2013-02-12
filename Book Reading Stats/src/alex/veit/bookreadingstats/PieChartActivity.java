package alex.veit.bookreadingstats;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;

public class PieChartActivity extends Activity {

	private final static String urlGoogleChart = "http://chart.apis.google.com/chart";
	private final static String urlp3Api = "?cht=p3&chs=";
	private final static String urlp3ApiSfterSize = "&chl=";
	private final static String urlp3ApiEnd = "&chd=t:";

	private ArrayList<BookStats> _myBooks;
	private WebView _pieChart;

	private void loadChart() {
		final StringBuffer urlRqs3DPie = new StringBuffer(urlGoogleChart
				+ urlp3Api + 900 + "x" + 250 + urlp3ApiSfterSize);

		final int listSize = _myBooks.size();
		for (int i = 0; i < listSize; i++) {
			urlRqs3DPie.append(_myBooks.get(i)._name);
			if (i < (listSize - 1)) {
				urlRqs3DPie.append("|");
			}
		}
		urlRqs3DPie.append(urlp3ApiEnd);
		for (int i = 0; i < listSize; i++) {
			urlRqs3DPie.append(Integer.toString(Utils.getIntPercentage(i,
					_myBooks)));
			if (i < (listSize - 1)) {
				urlRqs3DPie.append(",");
			}
		}

		_pieChart.loadUrl(urlRqs3DPie.toString());

	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pie_chart);

		_pieChart = (WebView) findViewById(R.id.webView);
		_myBooks = getIntent().getExtras().getParcelableArrayList(
				MainActivity.BOOK_LIST);
		loadChart();
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_pie_chart, menu);
		return true;
	}

	@Override
	protected void onDestroy() {
		_pieChart.destroy();
		_pieChart = null;
		super.onDestroy();
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
			case R.id.cancel :
				onBackPressed();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

}