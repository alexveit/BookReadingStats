package alex.veit.bookreadingstats;

import java.util.Calendar;

import android.os.Parcel;
import android.os.Parcelable;

public class Session implements Parcelable {

	public static final Creator<Session> CREATOR = new Creator<Session>() {

		@Override
		public Session createFromParcel(final Parcel source) {
			return new Session(source);
		}

		@Override
		public Session[] newArray(final int size) {
			return new Session[size];
		}
	};

	long _id;
	int _minutes;
	int _hours;
	int _pagesRead;
	String _value;
	Calendar _start;
	Calendar _end;
	boolean _isFirst;
	boolean _needsUpdate;

	public Session(final int id, final String value, final int hrs,
			final int mns, final int pagesRead) {
		_id = id;
		_value = value;
		_hours = hrs;
		_minutes = mns;
		_pagesRead = pagesRead;
		_start = null;
		_end = null;
		_isFirst = false;
		_needsUpdate = false;
	}

	public Session(final Parcel in) {
		final boolean[] bools = new boolean[2];
		_id = in.readLong();
		_hours = in.readInt();
		_minutes = in.readInt();
		_pagesRead = in.readInt();
		_value = in.readString();
		_start = (Calendar) in.readSerializable();
		_end = (Calendar) in.readSerializable();
		in.readBooleanArray(bools);
		_isFirst = bools[0];
		_needsUpdate = bools[1];

	}

	@Override
	public int describeContents() {
		return 0;
	}

	public String getDate() {
		final String date = Utils.digitFixer(_start.get(Calendar.MONTH) + 1)
				+ "/" + Utils.digitFixer(_start.get(Calendar.DAY_OF_MONTH))
				+ "/" + _start.get(Calendar.YEAR);

		return date;
	}

	public String getEnd() {
		if (_end == null) {
			return "--";
		}
		String end, am_pm, month, day, hour, minute;
		final int intHour = _end.get(Calendar.HOUR);
		month = Utils.digitFixer(_end.get(Calendar.MONTH) + 1);
		day = Utils.digitFixer(_end.get(Calendar.DAY_OF_MONTH));
		if (intHour == 0) {
			hour = Integer.toString(12);
		} else {
			hour = Utils.digitFixer(_end.get(Calendar.HOUR));
		}
		minute = Utils.digitFixer(_end.get(Calendar.MINUTE));
		if (_end.get(Calendar.AM_PM) == 1) {
			am_pm = "pm";
		} else {
			am_pm = "am";
		}
		end = month + "/" + day + "/" + _end.get(Calendar.YEAR) + " - " + hour
				+ ":" + minute + am_pm;
		return end;
	}

	public String getEndDB() {
		if (_end == null) {
			return "--";
		}
		String end, am_pm, month, day, hour, minute;

		month = Utils.digitFixer(_end.get(Calendar.MONTH));
		day = Utils.digitFixer(_end.get(Calendar.DAY_OF_MONTH));
		hour = Utils.digitFixer(_end.get(Calendar.HOUR));
		minute = Utils.digitFixer(_end.get(Calendar.MINUTE));
		if (_end.get(Calendar.AM_PM) == 1) {
			am_pm = "1";
		} else {
			am_pm = "0";
		}
		end = month + day + _end.get(Calendar.YEAR) + hour + minute + am_pm;
		return end;
	}

	public String getFinish() {
		if (_end == null) {
			return "--";
		}
		return Utils.digitFixer(_end.get(Calendar.MONTH) + 1) + "/"
				+ Utils.digitFixer(_end.get(Calendar.DAY_OF_MONTH)) + "/"
				+ _end.get(Calendar.YEAR);
	}

	public String getPagesReadString() {
		return "Pages read: " + _pagesRead;
	}

	public String getStart() {
		if (_start == null) {
			return "--";
		}
		String start, am_pm, month, day, hour, minute;
		final int intHour = _start.get(Calendar.HOUR);
		month = Utils.digitFixer(_start.get(Calendar.MONTH) + 1);
		day = Utils.digitFixer(_start.get(Calendar.DAY_OF_MONTH));
		if (intHour == 0) {
			hour = Integer.toString(12);
		} else {
			hour = Utils.digitFixer(_start.get(Calendar.HOUR));
		}
		minute = Utils.digitFixer(_start.get(Calendar.MINUTE));
		if (_start.get(Calendar.AM_PM) == 1) {
			am_pm = "pm";
		} else {
			am_pm = "am";
		}
		start = month + "/" + day + "/" + _start.get(Calendar.YEAR) + " - "
				+ hour + ":" + minute + am_pm;
		return start;
	}

	public String getStartDB() {
		if (_start == null) {
			return "--";
		}
		String start, am_pm, month, day, hour, minute;

		month = Utils.digitFixer(_start.get(Calendar.MONTH));
		day = Utils.digitFixer(_start.get(Calendar.DAY_OF_MONTH));
		hour = Utils.digitFixer(_start.get(Calendar.HOUR));
		minute = Utils.digitFixer(_start.get(Calendar.MINUTE));
		if (_start.get(Calendar.AM_PM) == 1) {
			am_pm = "1";
		} else {
			am_pm = "0";
		}
		start = month + day + _start.get(Calendar.YEAR) + hour + minute + am_pm;
		return start;
	}

	private boolean isSameDate(final BookStats bs) {

		if (_start.get(Calendar.MONTH) != bs._last.get(Calendar.MONTH)) {
			return false;
		}
		if (_start.get(Calendar.DAY_OF_MONTH) != bs._last
				.get(Calendar.DAY_OF_MONTH)) {
			return false;
		}
		if (_start.get(Calendar.YEAR) != bs._last.get(Calendar.YEAR)) {
			return false;
		}
		return true;
	}

	public void setEndFromDB(final String end) {
		if (end == null) {
			_end = null;
		} else {
			_end = Calendar.getInstance();
			_end.set(Integer.parseInt(end.substring(4, 8)),
					Integer.parseInt(end.substring(0, 2)),
					Integer.parseInt(end.substring(2, 4)));
			_end.set(Calendar.HOUR, Integer.parseInt(end.substring(8, 10)));
			_end.set(Calendar.MINUTE, Integer.parseInt(end.substring(10, 12)));
			_end.set(Calendar.AM_PM, Integer.parseInt(end.substring(12)));
		}
	}

	public void setSart(final Calendar start, final BookStats bs) {
		_start = start;
		updateLast(bs);
	}

	public void setStartFromDB(final String start, final BookStats bs) {

		if (start == null) {
			_start = null;
		} else {
			_start = Calendar.getInstance();
			_start.set(Integer.parseInt(start.substring(4, 8)),
					Integer.parseInt(start.substring(0, 2)),
					Integer.parseInt(start.substring(2, 4)));
			_start.set(Calendar.HOUR, Integer.parseInt(start.substring(8, 10)));
			_start.set(Calendar.MINUTE,
					Integer.parseInt(start.substring(10, 12)));
			_start.set(Calendar.AM_PM, Integer.parseInt(start.substring(12)));
			updateLast(bs);
		}

	}

	@Override
	public String toString() {
		return _value;
	}

	private void updateLast(final BookStats bs) {
		if (!isSameDate(bs)) {
			bs._last = _start;
			_isFirst = true;
		}

	}

	@Override
	public void writeToParcel(final Parcel dest, final int flags) {
		dest.writeLong(_id);
		dest.writeInt(_hours);
		dest.writeInt(_minutes);
		dest.writeInt(_pagesRead);
		dest.writeString(_value);
		dest.writeSerializable(_start);
		dest.writeSerializable(_end);
		dest.writeBooleanArray(new boolean[]{_isFirst,_needsUpdate});

	}
}