package alex.veit.bookreadingstats;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;

import android.os.Parcel;
import android.os.Parcelable;

public class BookStats implements Parcelable {

	public static final String _CLASS = "BookStats";

	public static final Parcelable.Creator<BookStats> CREATOR = new Parcelable.Creator<BookStats>() {
		@Override
		public BookStats createFromParcel(final Parcel in) {
			return new BookStats(in);
		}

		@Override
		public BookStats[] newArray(final int size) {
			return new BookStats[size];
		}
	};

	public static String getPagesLeft(final ArrayList<BookStats> myBooks) {
		return Integer.toString(getTotalPages(myBooks) - getPagesRead(myBooks));
	}

	public static int getPagesRead(final ArrayList<BookStats> myBooks) {
		int pagesRead = 0;
		for (int i = 0; i < myBooks.size(); i++) {
			for (int j = 0; j < myBooks.get(i)._sessions.size(); j++) {
				pagesRead += myBooks.get(i)._sessions.get(j)._pagesRead;
			}
		}
		return pagesRead;
	}

	public static String getTotalElapsed(final ArrayList<BookStats> books) {
		String hours = "00", minutes = "00", days = "00";
		int totalMinutes = 0, totalHours = 0, totalDays = 0;
		totalMinutes = BookStats.getTotalMinutes(books);;
		do {
			if (totalMinutes >= 60) {
				totalHours++;
				totalMinutes -= 60;
			}
		} while (totalMinutes >= 60);

		do {
			if (totalHours >= 24) {
				totalDays++;
				totalHours -= 24;
			}
		} while (totalHours >= 24);

		days = Integer.toString(totalDays);
		minutes = Integer.toString(totalMinutes);
		hours = Integer.toString(totalHours);

		return days + "d " + hours + "h " + minutes + "m";
	}

	public static String getTotalMinPage(final ArrayList<BookStats> myBooks) {
		double pagesRead = 0;
		double totalSec = 0;
		int totalMin = 0;
		double secPage = 0;

		for (int i = 0; i < myBooks.size(); i++) {
			pagesRead += myBooks.get(i).getPagesReadInt();
			totalSec += myBooks.get(i).getElapsedInt() * 60;
		}
		if (pagesRead > 0) {
			secPage = totalSec / pagesRead;
			do {
				if (secPage >= 60) {
					totalMin++;
					secPage -= 60;
				}
			} while (secPage >= 60);
		}
		return Integer.toString(totalMin) + "m " + (int) secPage + "s";
	}

	public static int getTotalMinutes(final ArrayList<BookStats> books) {
		int totalMinutes = 0;
		for (int i = 0; i < books.size(); i++) {
			totalMinutes += books.get(i).getElapsedInt();
		}
		return totalMinutes;
	}

	public static String getTotalPageMin(final ArrayList<BookStats> myBooks) {
		double pagesRead = 0;
		double totalMin = 0;
		final DecimalFormat df = new DecimalFormat("#.###");

		for (int i = 0; i < myBooks.size(); i++) {
			pagesRead += myBooks.get(i).getPagesReadInt();
			totalMin += myBooks.get(i).getElapsedInt();
		}

		if (totalMin > 0) {
			return df.format((pagesRead / totalMin));
		}

		return "0.000";

	}

	public static int getTotalPages(final ArrayList<BookStats> myBooks) {
		int totalPages = 0;
		for (int i = 0; i < myBooks.size(); i++) {
			totalPages += myBooks.get(i)._totalPages;
		}
		return totalPages;
	}

	Calendar _last;
	boolean _hasNewSessions;
	boolean _alertNewSessions;
	boolean _alertBook;
	long _id;
	int _lastSession;

	String _name;

	boolean _needsUpdate;

	ArrayList<Session> _sessions;

	ArrayList<Session> _deletedSessions;

	int _totalPages;

	public BookStats() {
		_sessions = new ArrayList<Session>();
		_deletedSessions = new ArrayList<Session>();
		_name = "";
		_id = -1;
		_lastSession = 1;
		_totalPages = 0;
		// _pagesRed = 0;
		_hasNewSessions = false;
		_alertNewSessions = false;
		_needsUpdate = false;
		_alertBook = false;
		_last = Calendar.getInstance();
		_last.set(0, 0, 0);

	}

	public BookStats(final BookStats bs) {
		_alertBook = bs._alertBook;
		_alertNewSessions = bs._alertNewSessions;
		_hasNewSessions = bs._hasNewSessions;
		_id = bs._id;
		_lastSession = bs._lastSession;
		_name = bs._name;
		_needsUpdate = bs._needsUpdate;
		_totalPages = bs._totalPages;
		_sessions = new ArrayList<Session>(bs._sessions);
		_deletedSessions = new ArrayList<Session>(bs._deletedSessions);
		_last = bs._last;
	}

	public BookStats(final Parcel in) {

		this();
		final int[] intData = new int[2];
		final boolean[] hasNew = new boolean[4];

		_id = in.readLong();
		in.readIntArray(intData);
		_totalPages = intData[0];
		_lastSession = intData[1];
		in.readTypedList(_sessions, Session.CREATOR);
		in.readTypedList(_deletedSessions, Session.CREATOR);
		_name = in.readString();
		in.readBooleanArray(hasNew);
		_hasNewSessions = hasNew[0];
		_needsUpdate = hasNew[1];
		_alertNewSessions = hasNew[2];
		_alertBook = hasNew[3];
		_last = (Calendar) in.readSerializable();

	}

	public void addSession(final long elapsedMillis, final Calendar start,
			final Calendar end, final int pagesRead) {

		int hour = 0, minute = 0;
		String sHour = "", sMinute = "";
		long elapsedSeconds = elapsedMillis;

		do {
			if (elapsedSeconds >= 60) {
				minute++;
				elapsedSeconds -= 60;
			}
		} while (elapsedSeconds >= 60);

		do {
			if (minute >= 60) {
				hour++;
				minute -= 60;
			}
		} while (minute >= 60);

		sHour = Utils.digitFixer(hour);
		sMinute = Utils.digitFixer(minute);

		final Session ses = new Session(-1, "Session #" + _lastSession + " - "
				+ sHour + ":" + sMinute, hour, minute, pagesRead);
		ses.setSart(start, this);
		ses._end = end;
		_sessions.add(ses);
		_lastSession++;
		updateBooleans();

	}

	public String avgMinPageString() {
		final double pagesRead = getPagesReadInt();
		final double totalSec = getElapsedInt() * 60;
		int totalMin = 0;
		double secPage = 0;

		if (pagesRead > 0) {
			secPage = totalSec / pagesRead;
			do {
				if (secPage >= 60) {
					totalMin++;
					secPage -= 60;
				}
			} while (secPage >= 60);
		}

		return Integer.toString(totalMin) + "m " + (int) secPage + "s";
	}

	public String avgPageMinString() {
		final double pagesRead = getPagesReadInt();
		final double totalMin = getElapsedInt();
		final DecimalFormat df = new DecimalFormat("#.###");
		if (totalMin > 0) {
			return df.format((pagesRead / totalMin));
		}
		return "0.000";
	}

	public void deleteSes(final int thisID) {
		if (_sessions.get(thisID)._id != -1) {
			_deletedSessions.add(_sessions.get(thisID));
		}
		_sessions.remove(thisID);
		updateBooleans();

	}

	@Override
	public int describeContents() {
		return 0;
	}

	public String endedOnString() {
		String end = "--";
		if (isRead()) {
			if (_sessions.size() > 0) {
				final Calendar e = _sessions.get(_sessions.size() - 1)._end;
				if (e != null) {
					end = Utils.digitFixer((e.get(Calendar.MONTH)) + 1) + "/"
							+ Utils.digitFixer(e.get(Calendar.DAY_OF_MONTH))
							+ "/" + e.get(Calendar.YEAR);
				}
			}
		}
		return end;
	}

	protected int[] EstimatedReadTimeBundle() {

		double estimate = EstimatedReadTimeDouble();

		int hours = 0, minutes = 0, days = 0;

		do {
			if (estimate >= 60) {
				hours++;
				estimate -= 60;
			}
		} while (estimate >= 60);

		do {
			if (hours >= 24) {
				days++;
				hours -= 24;
			}
		} while (hours >= 24);

		minutes = (int) estimate;

		return new int[]{days, hours, minutes};
	}

	protected double EstimatedReadTimeDouble() {
		double estimate;

		final int read = getPagesReadInt();
		if (read != 0) {
			estimate = (double) (getElapsedInt() * _totalPages) / (double) read;
		} else {
			estimate = 0;
		}
		return estimate;
	}

	public String getDoublePercentageString() {
		if (_totalPages != 0) {
			return Double.toString((double) (getPagesReadInt() * 100)
					/ _totalPages);
		} else {
			return "0";
		}
	}

	private int[] getElapsedBundle() {
		int hours = 0, days = 0;
		int minutes = getTotalMinutes();
		for (int i = 0; i < _sessions.size(); i++) {
			hours += _sessions.get(i)._hours;
		}
		do {
			if (minutes >= 60) {
				hours++;
				minutes -= 60;
			}
		} while (minutes >= 60);

		do {
			if (hours >= 24) {
				days++;
				hours -= 24;
			}
		} while (hours >= 24);
		return new int[]{days, hours, minutes};
	}

	protected int getElapsedInt() {
		int hours = 0, minutes = 0;
		for (int i = 0; i < _sessions.size(); i++) {
			hours += _sessions.get(i)._hours;
			minutes += _sessions.get(i)._minutes;
		}
		return (hours * 60) + minutes;
	}

	public String getElapsedTimeString() {
		String minutes, hours, days;
		final int bundle[] = getElapsedBundle();

		if (bundle[0] == 0) {
			minutes = Utils.digitFixer(bundle[2]);
			hours = Utils.digitFixer(bundle[1]);
			return hours + ":" + minutes;
		}
		minutes = Integer.toString(bundle[2]);
		hours = Integer.toString(bundle[1]);
		days = Integer.toString(bundle[0]);
		return days + "d " + hours + "h " + minutes + "m ";

	}

	public String getEstimatedReadTimeString() {

		final int[] time = EstimatedReadTimeBundle();
		final int days = time[0], hours = time[1], minutes = time[2];
		String d = "00", h = "00", m = "00";
		if (days == 0) {
			h = Utils.digitFixer(hours);
			m = Utils.digitFixer(minutes);
			return h + ":" + m;
		}

		h = Integer.toString(hours);
		m = Integer.toString(minutes);
		d = Integer.toString(days);
		return d + "d " + h + "h " + m + "m";
	}

	public String getEstimatedTimeLeftString() {

		String h = "00", m = "00", d = "00";
		final int elapsed = getElapsedInt();
		final double estimated = EstimatedReadTimeDouble();

		int left = (int) (estimated - elapsed);

		int hours = 0, minutes = 0, days = 0;

		if (!(left < 0)) {
			do {
				if (left >= 60) {
					hours++;
					left -= 60;
				}
			} while (left >= 60);

			do {
				if (hours >= 24) {
					days++;
					hours -= 24;
				}
			} while (hours >= 24);

			minutes = left;

			if (days == 0) {
				h = Utils.digitFixer(hours);
				m = Utils.digitFixer(minutes);
				return h + ":" + m;
			}

			h = Integer.toString(hours);
			m = Integer.toString(minutes);
			d = Integer.toString(days);
			return d + "d " + h + "h " + m + "m";
		}

		return h + ":" + m;
	}

	public String getIntPercentageString() {
		if (_totalPages != 0) {
			return Integer.toString((getPagesReadInt() * 100) / _totalPages);
		} else {
			return "0";
		}
	}

	public String getPagesLeftForListString() {
		final int pagesLeft = _totalPages - getPagesReadInt();
		if (pagesLeft > 0) {
			return "Pages left: " + pagesLeft;
		}
		return "Finished on " + _sessions.get(_sessions.size() - 1).getFinish();
	}

	public String getPagesLeftString() {
		return Integer.toString(_totalPages - getPagesReadInt());
	}

	public int getPagesReadInt() {
		int read = 0;
		for (int i = 0; i < _sessions.size(); i++) {
			read += _sessions.get(i)._pagesRead;
		}
		return read;
	}

	public String getPagesReadString() {
		return Integer.toString(getPagesReadInt());
	}

	public String getPartialEstimateString(final int pages) {
		final int elapsed = getElapsedInt();
		final int pagesRead = getPagesReadInt();
		if (pagesRead > 0) {
			int estimateMinutes = (pages * elapsed) / pagesRead;
			int hours = 0;

			do {
				if (estimateMinutes >= 60) {
					hours++;
					estimateMinutes -= 60;
				}
			} while (estimateMinutes >= 60);

			return Utils.digitFixer(hours) + ":"
					+ Utils.digitFixer(estimateMinutes);
		}
		return "00:00";
	}

	public int getPercentageReadInt() {
		return (getPagesReadInt() * 100) / _totalPages;
	}

	private int getTotalMinutes() {
		int minutes = 0;
		for (int i = 0; i < _sessions.size(); i++) {
			minutes += _sessions.get(i)._minutes;
		}
		return minutes;
	}

	public boolean isRead() {
		return (getPagesReadInt() == _totalPages);
	}

	public boolean isValidSession(final long elapsedMillis) {
		return (elapsedMillis >= 60);
	}

	public String startedOnString() {
		String start = "--";
		if (_sessions.size() > 0) {
			final Calendar s = _sessions.get(0)._start;
			if (s != null) {
				start = Utils.digitFixer((s.get(Calendar.MONTH)) + 1) + "/"
						+ Utils.digitFixer(s.get(Calendar.DAY_OF_MONTH)) + "/"
						+ s.get(Calendar.YEAR);
			}
		}
		return start;
	}

	@Override
	public String toString() {
		return _name;
	}

	protected void updateBooleans() {
		_hasNewSessions = true;
		_needsUpdate = true;
		_alertNewSessions = true;
		_alertBook = true;
	}

	@Override
	public void writeToParcel(final Parcel dest, final int flags) {

		dest.writeLong(_id);
		dest.writeIntArray(new int[]{_totalPages, _lastSession});
		dest.writeTypedList(_sessions);
		dest.writeTypedList(_deletedSessions);
		dest.writeString(_name);
		dest.writeBooleanArray(new boolean[]{_hasNewSessions, _needsUpdate,
				_alertNewSessions, _alertBook});
		dest.writeSerializable(_last);
	}
}
