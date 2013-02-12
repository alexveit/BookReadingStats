package alex.veit.bookreadingstats;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class MyListAdapter extends BaseAdapter {

	private final Activity activity;
	private final ArrayList<BookStats> _myBooks;
	private ArrayList<BookStats> _myBooksTemp;
	private final ArrayList<Session> _sessions;
	private static LayoutInflater inflater = null;
	private final int _type;

	public static final int MAIN = 1;
	public static final int SESSIONS = 2;

	public MyListAdapter(final Activity a, final ArrayList<BookStats> books,
			final ArrayList<Session> sessions, final int type) {
		activity = a;
		_myBooks = books;
		inflater = (LayoutInflater) activity
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		_type = type;
		_sessions = sessions;
		_myBooksTemp = null;
	}

	public void clearTemp() {
		if (_myBooksTemp != null) {
			_myBooksTemp.clear();
			_myBooksTemp = null;
		}
	}

	private View getBookView(final int position, final View convertView) {
		View vi = convertView;
		ArrayList<BookStats> books = _myBooks;
		int pos = 0;
		if (_myBooksTemp != null) {
			pos = (int) Utils.getRightID(_myBooks, _myBooksTemp, position);
			books = _myBooksTemp;
		}
		if (books.get(position).isRead()) {
			vi = inflater.inflate(R.layout.item_two_green, null);
		} else if (books.get(position).getPercentageReadInt() < 25) {
			vi = inflater.inflate(R.layout.item_two_red, null);
		} else if (books.get(position).getPercentageReadInt() < 50) {
			vi = inflater.inflate(R.layout.item_two_yellow_red, null);
		} else if (books.get(position).getPercentageReadInt() < 75) {
			vi = inflater.inflate(R.layout.item_two_yellow, null);
		} else {
			vi = inflater.inflate(R.layout.item_two_green_yellow, null);
		}
		final TextView title = (TextView) vi.findViewById(R.id.title);
		final TextView info = (TextView) vi.findViewById(R.id.info);
		final TextView info2 = (TextView) vi.findViewById(R.id.info_2);
		title.setText(books.get(position)._name);
		info.setText(books.get(position).getPagesLeftForListString());
		if (_myBooksTemp != null) {
			info2.setText(books.get(position).getElapsedTimeString() + " = "
					+ Utils.getDoublePercentageString(pos, _myBooks) + " of "
					+ BookStats.getTotalElapsed(_myBooks));
		} else {
			info2.setText(books.get(position).getElapsedTimeString() + " = "
					+ Utils.getDoublePercentageString(position, books) + " of "
					+ BookStats.getTotalElapsed(books));
		}
		return vi;
	}

	@Override
	public int getCount() {
		if (_myBooksTemp != null) {
			return _myBooksTemp.size();
		} else if (_myBooks != null) {
			return _myBooks.size();
		} else {
			return _sessions.size();
		}
	}

	@Override
	public Object getItem(final int position) {
		return position;
	}

	@Override
	public long getItemId(final int position) {
		return position;
	}

	private View getSesView(final int position, final View convertView) {
		View vi = convertView;

		if (_sessions.get(position)._isFirst) {
			vi = inflater.inflate(R.layout.item_ses_date, null);
		} else {
			vi = inflater.inflate(R.layout.item_ses, null);
		}

		final TextView value = (TextView) vi.findViewById(R.id.value);
		final TextView start = (TextView) vi.findViewById(R.id.start);
		final TextView end = (TextView) vi.findViewById(R.id.end);
		final TextView pagesRead = (TextView) vi.findViewById(R.id.pages_read);
		value.setText(_sessions.get(position)._value);
		start.setText(_sessions.get(position).getStart());
		end.setText(_sessions.get(position).getEnd());
		pagesRead.setText(_sessions.get(position).getPagesReadString());
		if (_sessions.get(position)._isFirst) {
			final TextView date = (TextView) vi.findViewById(R.id.textViewDate);
			date.setText(_sessions.get(position).getDate());
		}
		return vi;
	}

	@Override
	public View getView(final int position, final View convertView,
			final ViewGroup parent) {
		switch (_type) {
			case SESSIONS :
				return getSesView(position, convertView);
			default :
				return getBookView(position, convertView);

		}
	}

	public void setTempList(final ArrayList<BookStats> books) {
		_myBooksTemp = books;
	}

}