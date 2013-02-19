package alex.veit.bookreadingstats;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class BookActivity extends Activity {

	private static final int SESSIONS_REQUEST_CODE = 200;
	public static final String SESSIONS_STR = "SESSIONS";
	public static final String TITLE_STR = "TITLE";
	private static final String MY_BOOK = "MY_BOOK";
	private static final String DONE = "DONE";
	private static final String UPDATE = "UPDATE";

	private long _bookID;
	private EditText _bookName;
	private BookStats _bs;
	private boolean _done;
	private TextView _elapsed;
	private TextView _estReadTime;
	private TextView _estTimeLeft;
	private TextView _pagesRed;
	private TextView _percentage;
	private EditText _totalPages;
	private boolean _update;
	private TextView _pagesLeft;
	private TextView _minPage;
	private TextView _pageMin;
	private TextView _sessions;
	private TextView _startedOn;
	private TextView _endedOn;
	private Dialog _dialogPartial;
	private EditText _partialEdit;
	private TextView _partialText;

	private void add(final boolean addSession) {
		final Intent intent = new Intent(this, SessionsActivity.class);
		intent.putExtra(BookStats._CLASS, _bs);
		intent.putExtra(MainActivity.ADD_SESSION, addSession);
		startActivityForResult(intent, BookActivity.SESSIONS_REQUEST_CODE);
	}

	private void addSession() {
		if (retrieveData()) {
			add(false);
		}
	}

	private void doClearPartial() {
		_partialEdit.setText("");
		_partialText.setText(getString(R.string.hhmm));
	}

	private void done() {

		if (!_bookName.getText().toString().isEmpty()) {
			_done = true;
			if (getCalculateBook()) {
				final Intent returnIntent = new Intent();
				if (_update) {
					returnIntent.putExtra(MainActivity.BOOK_ID, _bookID);
					returnIntent.putExtra(MainActivity.UPDATE_BOOK, true);
				}
				_bs._alertBook = false;
				returnIntent.putExtra(BookStats._CLASS, _bs);
				super.setResult(Activity.RESULT_OK, returnIntent);
				super.finish();
			} else {
				_done = false;
			}
		} else {
			Toast.makeText(this, "Input Book Name", Toast.LENGTH_SHORT).show();
		}

	}

	private boolean getCalculateBook() {

		if (retrieveData()) {
			if (!_done) {
				updateControls();
			}
			return true;
		}
		return false;
	}

	@Override
	protected void onActivityResult(final int requestCode,
			final int resultCode, final Intent data) {

		if (resultCode == Activity.RESULT_OK) {
			_bs = (BookStats) data.getParcelableExtra(BookStats._CLASS);
			getCalculateBook();
		}
	}

	@Override
	public void onBackPressed() {
		if (_bs._alertBook) {
			new AlertDialog.Builder(this)
					.setTitle("Save?")
					.setMessage("Keep changes to this book?")
					.setPositiveButton(
							"Yes",
							new android.content.DialogInterface.OnClickListener() {
								@Override
								public void onClick(final DialogInterface arg0,
										final int arg1) {
									done();
								}
							})
					.setNegativeButton(
							"No",
							new android.content.DialogInterface.OnClickListener() {
								@Override
								public void onClick(final DialogInterface arg0,
										final int arg1) {
									BookActivity.super
											.setResult(Activity.RESULT_CANCELED);
									BookActivity.super.onBackPressed();
								}
							}).show();
		} else {
			super.setResult(Activity.RESULT_CANCELED);
			super.onBackPressed();
		}

	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		super.setContentView(R.layout.activity_book);

		_bookName = (EditText) super.findViewById(R.id.editTextBookName);
		_totalPages = (EditText) super.findViewById(R.id.editTextTotalPages);
		_pagesRed = (TextView) super.findViewById(R.id.textViewPagesRead);
		_percentage = (TextView) super.findViewById(R.id.textViewPR);
		_elapsed = (TextView) super.findViewById(R.id.textViewET);
		_estReadTime = (TextView) super.findViewById(R.id.textViewERT);
		_estTimeLeft = (TextView) super.findViewById(R.id.textViewETL);
		_pagesLeft = (TextView) super.findViewById(R.id.textViewPagesLeft);
		_minPage = (TextView) super.findViewById(R.id.textViewMinPage);
		_pageMin = (TextView) super.findViewById(R.id.textViewPageMin);
		_sessions = (TextView) super.findViewById(R.id.textViewSessions);
		_startedOn = (TextView) super.findViewById(R.id.textViewStartedOn);
		_endedOn = (TextView) super.findViewById(R.id.textViewEndedOn);

		_done = false;
		if (!super.getIntent().getExtras().getBoolean(MainActivity.UPDATE_BOOK)) {
			_bs = new BookStats();
			_update = false;
			_bookID = -1;
		} else {
			_update = true;
			_bookID = getIntent().getExtras().getLong(MainActivity.BOOK_ID);
			_bs = (BookStats) super.getIntent().getParcelableExtra(
					BookStats._CLASS);
			if (getIntent().getExtras().getBoolean(MainActivity.ADD_SESSION)) {
				add(true);
			}
			_bookName.setText(_bs._name);
			_pagesRed.setText(_bs.getPagesReadString());
			_totalPages.setText(Integer.toString(_bs._totalPages));
			getCalculateBook();

		}

		if (savedInstanceState == null) {
			setTextWatcher();
		}

		setPartialDialog();

		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		super.getMenuInflater().inflate(R.menu.activity_book, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
			case R.id.sessions :
				addSession();
				return true;
			case R.id.done :
				done();
				return true;
			case R.id.cancel :
				onBackPressed();
				return true;
			case R.id.partial :
				_dialogPartial.show();
				return true;

		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onRestoreInstanceState(final Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		_bs = savedInstanceState.getParcelable(MY_BOOK);
		_done = savedInstanceState.getBoolean(DONE);
		_update = savedInstanceState.getBoolean(UPDATE);
		getCalculateBook();
		setTextWatcher();

	}

	@Override
	public void onSaveInstanceState(final Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putParcelable(MY_BOOK, _bs);
		savedInstanceState.putBoolean(DONE, _done);
		savedInstanceState.putBoolean(UPDATE, _update);

	}

	private boolean retrieveData() {
		int tempTotal = 0;

		_bs._name = _bookName.getText().toString();
		if (!_totalPages.getText().toString().isEmpty()) {
			tempTotal = Integer.parseInt(_totalPages.getText().toString());
			_bs._totalPages = tempTotal;
			return true;

		} else {
			Toast.makeText(this, "Input total pages", Toast.LENGTH_SHORT)
					.show();
		}

		return false;

	}

	private void setPartialDialog() {
		if (_dialogPartial == null) {
			_dialogPartial = new Dialog(this);
			_dialogPartial.setContentView(R.layout.dialog_partial);
			_dialogPartial.setTitle("Partial Estimate");
			_partialEdit = (EditText) _dialogPartial
					.findViewById(R.id.editTextPartial);
			_partialText = (TextView) _dialogPartial
					.findViewById(R.id.textViewPResult);


			final TextWatcher twCalc = new TextWatcher() {

				@Override
				public void afterTextChanged(final Editable s) {
					if (!_partialEdit.getText().toString().isEmpty()) {
						final int pages = Integer.parseInt(_partialEdit
								.getText().toString());
						_partialText.setText(_bs
								.getPartialEstimateString(pages));
					}

				}

				@Override
				public void beforeTextChanged(final CharSequence arg0,
						final int start, final int count, final int after) {
					// TODO Auto-generated method stub

				}

				@Override
				public void onTextChanged(final CharSequence s, final int start,
						final int before, final int count) {
					// TODO Auto-generated method stub

				}

			};

			_partialEdit.addTextChangedListener(twCalc);
			
			final Button clear = (Button) _dialogPartial
					.findViewById(R.id.buttonClear);
			final Button done = (Button) _dialogPartial
					.findViewById(R.id.buttonDone);

			clear.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(final View v) {
					doClearPartial();
				}
			});

			done.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(final View v) {
					_dialogPartial.dismiss();
				}
			});
		}
	}

	private void setTextWatcher() {
		final TextWatcher tw = new TextWatcher() {

			@Override
			public void afterTextChanged(final Editable s) {
				_bs._needsUpdate = true;
				_bs._alertBook = true;
				getCalculateBook();

			}

			@Override
			public void beforeTextChanged(final CharSequence arg0,
					final int start, final int count, final int after) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onTextChanged(final CharSequence s, final int start,
					final int before, final int count) {
				// TODO Auto-generated method stub

			}

		};

		final TextWatcher twName = new TextWatcher() {

			@Override
			public void afterTextChanged(final Editable s) {
				_bs._needsUpdate = true;
				_bs._alertBook = true;

			}

			@Override
			public void beforeTextChanged(final CharSequence arg0,
					final int start, final int count, final int after) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onTextChanged(final CharSequence s, final int start,
					final int before, final int count) {
				// TODO Auto-generated method stub

			}

		};

		_bookName.addTextChangedListener(twName);
		_totalPages.addTextChangedListener(tw);

	}

	private void updateControls() {

		_pagesRed.setText(_bs.getPagesReadString());

		_pagesLeft.setText(_bs.getPagesLeftString());

		_percentage.setText(_bs.getIntPercentageString() + "%");

		_elapsed.setText(_bs.getElapsedTimeString());

		_sessions.setText(Integer.toString(_bs._sessions.size()));

		_estReadTime.setText(_bs.getEstimatedReadTimeString());

		_estTimeLeft.setText(_bs.getEstimatedTimeLeftString());

		_minPage.setText(_bs.avgMinPageString());

		_pageMin.setText(_bs.avgPageMinString());

		_startedOn.setText(_bs.startedOnString());

		_endedOn.setText(_bs.endedOnString());
	}
}
