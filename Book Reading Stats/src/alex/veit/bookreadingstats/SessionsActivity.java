package alex.veit.bookreadingstats;

import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class SessionsActivity extends Activity {

	private static final String PAGE_DIALOG_SHOWING = "PAGE_DIALOG_SHOWING";
	private static final String CHRON_DIALOG_SHOWING = "CHRON_DIALOG_SHOWING";
	private static final String MY_BS = "MY_BS";
	private static final String PAGE_STRING = "PAGE_STRING";
	private static final String BASE_STR = "BASE_STR";
	private static final String ELAPSED_STR = "ELAPSED_STR";
	private static final String CHRON_IS_RUNNING = "CHRON_IS_RUNNING";
	private static final String CHRON_HAS_STARTED = "CHRON_HAS_STARTED";
	private static final String MILLISEC_STR = "MILLISEC_STR";
	private static final String UPDATE_MILISEC = "UPDATE_MILISEC";
	private static final String MY_MILISEC = "MY_MILISEC";
	private static final String CHRON_START = "CHRON_START";
	private static final String TRANSFER_MILLISEC = "TRANSFER_MILLISEC";
	private static final String CHRON_END = "CHRON_END";

	private MyListAdapter _adapter;
	private long _base;
	private BookStats _bs;
	private long _elapsed;
	private ListView _myListView;
	private boolean _chronIsRunning;
	private boolean _chronHasStarted;
	private Dialog _dialogPage;
	private Dialog _dialogChron;
	private EditText _page;
	private Chronometer _chronometer;
	private Button _buttonStartChron;
	private Button _buttonStopChron;
	private Button _buttonResetChron;
	private Button _buttonDoneChron;
	private long _myMilisec;
	private long _transferMillisec;
	private boolean _updateMillisec;
	private Calendar _start;
	private Calendar _end;

	private NotificationCompat.Builder _builder;
	private NotificationManager _notificationManager;

	protected void autoAddSession() {
		long elapsedMillis = _elapsed - _chronometer.getBase();
		elapsedMillis /= 1000;
		if (elapsedMillis < 0) {
			elapsedMillis = _myMilisec / 1000;
		}
		if (_bs.isValidSession(elapsedMillis)) {
			_transferMillisec = elapsedMillis;
			setPageDialog();
		} else {
			new AlertDialog.Builder(this).setTitle(getString(R.string.alert))
					.setMessage("Please read for more then 1 minute.")
					.setPositiveButton("OK", null).show();
		}
	}

	private void chronCancel() {
		_chronometer.stop();
		_chronometer.setBase(SystemClock.elapsedRealtime());
		_base = 0;
		dismissDialogChron();
	}

	private void chronDone() {
		_chronometer.setBase(SystemClock.elapsedRealtime());
		_base = 0;
		dismissDialogChron();

	}

	private void chronReset() {
		_chronometer.setBase(SystemClock.elapsedRealtime());
		_base = 0;
		_chronIsRunning = false;
		_chronHasStarted = false;
		_buttonResetChron.setEnabled(false);
		_buttonDoneChron.setEnabled(false);

	}

	private void chronStart(final boolean display) {
		_chronometer.setBase(SystemClock.elapsedRealtime() + _base);
		_chronometer.start();
		_buttonStartChron.setEnabled(false);
		_buttonStopChron.setEnabled(true);
		_buttonDoneChron.setEnabled(false);
		_buttonResetChron.setEnabled(false);
		if (display) {
			Toast.makeText(this, "Start reading!", Toast.LENGTH_SHORT).show();
		}

		_chronIsRunning = true;
		_chronHasStarted = true;
		_updateMillisec = true;
		_builder.setContentText("Reading");
		_notificationManager.notify(0, _builder.build());
	}

	private void chronStop() {
		_elapsed = SystemClock.elapsedRealtime();
		_base = _chronometer.getBase() - _elapsed;
		_chronometer.stop();
		_buttonStartChron.setEnabled(true);
		_buttonStopChron.setEnabled(false);
		_buttonResetChron.setEnabled(true);
		_buttonDoneChron.setEnabled(true);
		_chronIsRunning = false;
		_builder.setContentText("Stopped");
		_notificationManager.notify(0, _builder.build());
		_end = Calendar.getInstance();
	}

	private void dismissDialogChron() {
		_dialogChron.dismiss();
		_dialogChron = null;
		_notificationManager.cancel(0);
	}

	private void done() {

		_bs._alertNewSessions = false;
		final Intent returnIntent = new Intent(this, SessionsActivity.class);
		returnIntent.putExtra(BookStats._CLASS, _bs);
		setResult(Activity.RESULT_OK, returnIntent);
		finish();

	}

	private String getLastChronText(final long millisec) {
		long elapsedMillis = millisec;
		elapsedMillis /= 1000;
		int hour = 0, minute = 0;
		String sHour = "", sMinute = "", sSeconds = "";
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

		sSeconds = Utils.digitFixer((int) elapsedSeconds);
		sHour = Utils.digitFixer(hour);
		sMinute = Utils.digitFixer(minute);

		if (hour > 0) {
			return sHour + ":" + sMinute + ":" + sSeconds;
		} else {
			return sMinute + ":" + sSeconds;
		}
	}

	@Override
	public void onBackPressed() {
		if (_bs._alertNewSessions) {
			new AlertDialog.Builder(this)
					.setTitle("Save?")
					.setMessage("Keep changes to sessions?")
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
									_bs._alertNewSessions = false;
									SessionsActivity.super.onBackPressed();
								}
							}).show();
		} else {
			super.onBackPressed();
		}

	}

	@Override
	public boolean onContextItemSelected(final MenuItem item) {

		final AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item
				.getMenuInfo();
		switch (item.getItemId()) {
			case R.id.change_pages_read :
				displayChangePageDialog((int) menuInfo.id);
				break;
			case R.id.delete_session :
				_bs.deleteSes((int) menuInfo.id);
				_adapter.notifyDataSetChanged();
				break;
		}
		return super.onContextItemSelected(item);
	}

	private void displayChangePageDialog(final int id) {
		final Dialog changePageDialog = new Dialog(this);
		changePageDialog.setContentView(R.layout.dialog_change_pages);
		changePageDialog.setTitle(_bs._sessions.get(id)._value);
		final Button done = (Button) changePageDialog
				.findViewById(R.id.buttonDone);
		
		final EditText pages = (EditText) changePageDialog.findViewById(R.id.editTextNewPages);;

		done.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				int pagesRead = Integer.parseInt(pages.getText().toString());
				
				int limit = _bs.getPagesReadInt() - _bs._sessions.get(id)._pagesRead;
				
				if(pagesRead <= (_bs._totalPages - limit))
				{
					_bs._sessions.get(id)._pagesRead = pagesRead;
					_bs._sessions.get(id)._needsUpdate = true;
					_bs.updateBooleans();
					_adapter.notifyDataSetChanged();
					changePageDialog.dismiss();
				}
				else
				{
					new AlertDialog.Builder(SessionsActivity.this).setTitle(getString(R.string.alert))
					.setMessage("Pages exceeds book's total pages")
					.setPositiveButton("OK", null).show();
				}
				
				

			}
		});
		changePageDialog.show();
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sessions);
		// Show the Up button in the action bar.
		// super.getActionBar().setDisplayHomeAsUpEnabled(true);
		_bs = (BookStats) getIntent().getParcelableExtra(BookStats._CLASS);
		((TextView) findViewById(R.id.textViewBookName)).setText(_bs._name);
		_myListView = (ListView) findViewById(R.id.listViewSessions);
		_myListView.setEmptyView(findViewById(R.id.empty_list_view_ses));
		registerForContextMenu(_myListView);
		setAdapter();
		_base = 0;
		/*
		 * _myListView.setOnItemLongClickListener(new OnItemLongClickListener()
		 * {
		 * 
		 * @Override public boolean onItemLongClick(final AdapterView<?> parent,
		 * final View view, final int position, final long id) {
		 * 
		 * final int thisID = (int) id;
		 * 
		 * new AlertDialog.Builder(SessionsActivity.this)
		 * .setTitle("Delete this session?")
		 * .setMessage(_bs._sessions.get(thisID)._value) .setPositiveButton(
		 * "Yes", new android.content.DialogInterface.OnClickListener() {
		 * 
		 * @Override public void onClick( final DialogInterface arg0, final int
		 * arg1) { _bs.deleteSes(thisID); _adapter.notifyDataSetChanged(); } })
		 * .setNegativeButton( "No", new
		 * android.content.DialogInterface.OnClickListener() {
		 * 
		 * @Override public void onClick( final DialogInterface arg0, final int
		 * arg1) { } }).show();
		 * 
		 * return false; } });
		 */
		_builder = new NotificationCompat.Builder(this)
				.setSmallIcon(R.drawable.ic_book).setContentTitle(_bs._name)
				.setContentText("Stopped").setOngoing(true);
		final Intent contentIntent = new Intent(this, SessionsActivity.class);
		contentIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
				| Intent.FLAG_ACTIVITY_SINGLE_TOP);
		_builder.setContentIntent(PendingIntent.getActivity(
				this.getBaseContext(), 0, contentIntent,
				PendingIntent.FLAG_UPDATE_CURRENT));
		_notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		if (getIntent().getExtras().getBoolean(MainActivity.ADD_SESSION)) {
			startSession();
		}

	}

	@Override
	public void onCreateContextMenu(final ContextMenu menu, final View v,
			final ContextMenuInfo menuInfo) {
		// Inflate the menu; this adds items to the action bar if it is present.
		super.onCreateContextMenu(menu, v, menuInfo);
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		menu.setHeaderTitle("Session");
		menu.setHeaderTitle(_bs._sessions.get((int) info.id)._value);
		getMenuInflater().inflate(R.menu.activity_sessions_context, menu);

	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		if (!_bs.isRead()) {
			getMenuInflater().inflate(R.menu.activity_sessions, menu);
		} else {
			getMenuInflater().inflate(R.menu.activity_sessions_back, menu);
		}
		return true;
	}

	@Override
	protected void onDestroy() {
		_myListView.setAdapter(null);
		if (_dialogChron != null) {
			_dialogChron.dismiss();
		}
		if (_dialogPage != null) {
			_dialogPage.dismiss();
		}
		super.onDestroy();
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
			case R.id.start :
				startSession();
				return true;
			case R.id.done :
				done();
				return true;
			case R.id.cancel :
				onBackPressed();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onRestoreInstanceState(final Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		// Restore UI state from the savedInstanceState.
		// This bundle has also been passed to onCreate.

		_bs = savedInstanceState.getParcelable(MY_BS);
		setAdapter();

		_end = (Calendar) savedInstanceState.getSerializable(CHRON_END);
		_start = (Calendar) savedInstanceState.getSerializable(CHRON_START);
		if (savedInstanceState.getBoolean(CHRON_DIALOG_SHOWING)) {
			_base = savedInstanceState.getLong(BASE_STR);
			_elapsed = savedInstanceState.getLong(ELAPSED_STR);
			_chronIsRunning = savedInstanceState.getBoolean(CHRON_IS_RUNNING);
			_chronHasStarted = savedInstanceState.getBoolean(CHRON_HAS_STARTED);
			_updateMillisec = savedInstanceState.getBoolean(UPDATE_MILISEC);
			setChronDialog();
			if (_chronIsRunning) {
				chronStart(false);
			} else if (_chronHasStarted) {
				_myMilisec = savedInstanceState.getLong(MILLISEC_STR);
				_chronometer.setText(getLastChronText(_myMilisec));
				_buttonStartChron.setEnabled(true);
				_buttonStopChron.setEnabled(false);
				_buttonResetChron.setEnabled(true);
				_buttonDoneChron.setEnabled(true);
			}
		}

		if (savedInstanceState.getBoolean(PAGE_DIALOG_SHOWING)) {
			setPageDialog();
			_page.setText(savedInstanceState.getString(PAGE_STRING));
			_transferMillisec = savedInstanceState.getLong(TRANSFER_MILLISEC);

		}

	}

	@Override
	public void onSaveInstanceState(final Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		// Save UI state changes to the savedInstanceState.
		// This bundle will be passed to onCreate if the process is
		// killed and restarted.

		savedInstanceState.putParcelable(MY_BS, _bs);

		savedInstanceState.putLong(TRANSFER_MILLISEC, this._transferMillisec);

		if ((_dialogPage != null) && _dialogPage.isShowing()) {
			savedInstanceState.putBoolean(PAGE_DIALOG_SHOWING, true);
			savedInstanceState.putString(PAGE_STRING, _page.getText()
					.toString());

		} else {
			savedInstanceState.putBoolean(PAGE_DIALOG_SHOWING, false);
		}

		savedInstanceState.putSerializable(CHRON_START, _start);
		savedInstanceState.putSerializable(CHRON_END, _end);
		if ((_dialogChron != null) && _dialogChron.isShowing()) {
			savedInstanceState.putBoolean(CHRON_DIALOG_SHOWING, true);

			if (_chronIsRunning) {
				_elapsed = SystemClock.elapsedRealtime();
				_base = _chronometer.getBase() - _elapsed;
			}
			if (_updateMillisec) {
				_myMilisec = (_elapsed - _chronometer.getBase());
				savedInstanceState.putLong(MILLISEC_STR, _myMilisec);
				_updateMillisec = false;
			} else {
				savedInstanceState.putLong(MILLISEC_STR, _myMilisec);
			}
			savedInstanceState.putLong(MY_MILISEC, _myMilisec);
			savedInstanceState.putBoolean(UPDATE_MILISEC, _updateMillisec);
			savedInstanceState.putLong(BASE_STR, _base);
			savedInstanceState.putLong(ELAPSED_STR, _elapsed);
			savedInstanceState.putBoolean(CHRON_IS_RUNNING, _chronIsRunning);
			savedInstanceState.putBoolean(CHRON_HAS_STARTED, _chronHasStarted);
		} else {
			savedInstanceState.putBoolean(CHRON_DIALOG_SHOWING, false);
		}

	}

	private void setAdapter() {
		_adapter = new MyListAdapter(SessionsActivity.this, null,
				_bs._sessions, MyListAdapter.SESSIONS);
		_myListView.setAdapter(_adapter);
		_adapter.notifyDataSetChanged();
	}

	private void setChronDialog() {
		if (_dialogChron == null) {
			_dialogChron = new Dialog(this);
			_dialogChron.setContentView(R.layout.dialog_chronometer);
			_dialogChron.setTitle("Session #" + _bs._lastSession);
			_dialogChron.setOnCancelListener(new OnCancelListener() {

				@Override
				public void onCancel(final DialogInterface dialog) {
					if (_chronHasStarted) {
						new AlertDialog.Builder(SessionsActivity.this)
								.setTitle("Cancel?")
								.setMessage(
										"Are you sure you want to cancel this session?")
								.setPositiveButton(
										"Yes",
										new android.content.DialogInterface.OnClickListener() {
											@Override
											public void onClick(
													final DialogInterface arg0,
													final int arg1) {
												dismissDialogChron();
											}
										})
								.setNegativeButton(
										"No",
										new android.content.DialogInterface.OnClickListener() {
											@Override
											public void onClick(
													final DialogInterface arg0,
													final int arg1) {
												_dialogChron.show();
											}
										}).show();
					} else {
						dismissDialogChron();
					}

				}
			});

			_chronometer = (Chronometer) _dialogChron
					.findViewById(R.id.chronometer);
			final Button buttonCancel = (Button) _dialogChron
					.findViewById(R.id.buttonCancel);
			_buttonStartChron = (Button) _dialogChron
					.findViewById(R.id.buttonStart);
			_buttonStopChron = (Button) _dialogChron
					.findViewById(R.id.buttonStop);
			_buttonResetChron = (Button) _dialogChron
					.findViewById(R.id.buttonReset);
			_buttonDoneChron = (Button) _dialogChron
					.findViewById(R.id.buttonDone);

			_buttonStartChron.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					chronStart(true);
				}

			});

			_buttonStopChron.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					chronStop();
				}

			});

			_buttonResetChron.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					new AlertDialog.Builder(SessionsActivity.this)
							.setTitle("Reset?")
							.setMessage("Are you sure you want to reset?")
							.setPositiveButton(
									"Yes",
									new android.content.DialogInterface.OnClickListener() {
										@Override
										public void onClick(
												final DialogInterface arg0,
												final int arg1) {
											chronReset();
										}
									})
							.setNegativeButton(
									"No",
									new android.content.DialogInterface.OnClickListener() {
										@Override
										public void onClick(
												final DialogInterface arg0,
												final int arg1) {
										}
									}).show();

				}

			});

			_buttonDoneChron.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					autoAddSession();
				}

			});

			buttonCancel.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					if (_chronHasStarted) {
						new AlertDialog.Builder(SessionsActivity.this)
								.setTitle("Cancel?")
								.setMessage(
										"Are you sure you want to cancel this session?")
								.setPositiveButton(
										"Yes",
										new android.content.DialogInterface.OnClickListener() {
											@Override
											public void onClick(
													final DialogInterface arg0,
													final int arg1) {
												chronCancel();
											}
										})
								.setNegativeButton(
										"No",
										new android.content.DialogInterface.OnClickListener() {
											@Override
											public void onClick(
													final DialogInterface arg0,
													final int arg1) {
											}
										}).show();
					} else {
						chronCancel();
					}

				}

			});
		}
		_dialogChron.show();
	}

	private void setPageDialog() {
		if (_dialogPage == null) {
			_dialogPage = new Dialog(this);
			_dialogPage.setContentView(R.layout.dialog_page);
			_dialogPage.setTitle("Page");

			final Button buttonOK = (Button) _dialogPage
					.findViewById(R.id.buttonOK);
			_page = (EditText) _dialogPage.findViewById(R.id.editTextPage);

			final TextView total = (TextView) _dialogPage
					.findViewById(R.id.textViewTPages);

			total.setText(getResources().getString(R.string.totalPages) + " "
					+ _bs._totalPages);

			_page.setText(_bs.getPagesReadString());

			buttonOK.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					final int pagesRead = Integer.parseInt(_page.getText()
							.toString());
					if (pagesRead <= _bs._totalPages) {
						final int temp = _bs.getPagesReadInt();
						int sessionRead = 0;
						sessionRead = pagesRead - temp;
						if (sessionRead >= 0) {
							_bs.addSession(_transferMillisec, _start, _end,
									sessionRead);
							chronDone();
							_dialogPage.dismiss();
							_adapter.notifyDataSetChanged();
						} else {
							new AlertDialog.Builder(SessionsActivity.this)
									.setTitle(getString(R.string.alert))
									.setMessage(
											"Pages read can not be less then "
													+ _bs.getPagesReadInt())
									.setPositiveButton("OK", null).show();
							_page.setText(_bs.getPagesReadString());
						}

					} else {
						new AlertDialog.Builder(SessionsActivity.this)
								.setTitle(getString(R.string.alert))
								.setMessage(
										"Pages read can not be grater then total pages.\nTotal pages: "
												+ _bs._totalPages)
								.setPositiveButton("OK", null).show();
						_page.setText(_bs.getPagesReadString());
					}
				}
			});
		}
		_dialogPage.show();
	}

	private void startSession() {
		_notificationManager.notify(0, _builder.build());
		_start = Calendar.getInstance();
		setChronDialog();
	}
}
