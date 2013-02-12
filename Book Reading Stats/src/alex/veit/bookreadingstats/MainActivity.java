package alex.veit.bookreadingstats;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private class AscyncThread extends AsyncTask<Void, Void, Void> {

		ProgressDialog _progress;
		String _message = "";

		public AscyncThread(final String msg) {
			_message = msg;
		}

		@Override
		protected Void doInBackground(final Void... params) {
			_myBooks = _db.getAllBooks();
			return null;
		}

		@Override
		protected void onPostExecute(final Void result) {
			super.onPostExecute(result);
			finishSetup();
			_progress.dismiss();
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			_progress = ProgressDialog.show(MainActivity.this, "Books DB",
					_message, true);
		}

	}

	private class AscyncThreadEx extends AsyncTask<Void, Void, Void> {

		ProgressDialog _progress;
		String _message = "";

		public AscyncThreadEx(final String msg) {
			_message = msg;
		}

		@Override
		protected Void doInBackground(final Void... params) {
			doDBOperations();
			exportDB();
			return null;
		}

		@Override
		protected void onPostExecute(final Void result) {
			super.onPostExecute(result);
			_progress.dismiss();
			new AlertDialog.Builder(MainActivity.this)
					.setTitle(getString(R.string.success))
					.setMessage(
							"DataBase file \"BookStatsDB\" has been exported to external storage in a folder called \"BookReadingStats\"")
					.setPositiveButton("OK", null).show();
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			_progress = ProgressDialog.show(MainActivity.this, "Books DB",
					_message, true);
		}

	}

	private class AscyncThreadSearch extends AsyncTask<Void, Void, Void> {

		String _s;

		public AscyncThreadSearch(final String s) {
			_myBooksTemp = new ArrayList<BookStats>();
			_s = s;
		}

		@Override
		protected Void doInBackground(final Void... params) {
			searchBook(_s, _myBooksTemp);
			return null;
		}

		@Override
		protected void onPostExecute(final Void result) {
			_myAdapter.setTempList(_myBooksTemp);
			_myAdapter.notifyDataSetChanged();
			_isDisplayingTemp = true;
			super.onPostExecute(result);
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

	}

	public static final String BOOK_ID = "BOOK_ID";
	private static final int BOOK_REQUEST_CODE = 12;
	private static final int IMPORT_CODE = 13;
	public static final String UPDATE_BOOK = "UPDATE_BOOK";
	public static final String DB_INSTANCE = "DB_INSTANCE";
	public static final String BOOK_LIST = "BOOK_LIST";
	public static final String ADD_SESSION = "ADD";
	private static final String MY_BOOKS = "MY_BOOKS";
	private static final String MY_DELETED_BOOKS = "MY_DELETED_BOOKS";
	private static final String IS_DIPLAYING_TEMP = "IS_DIPLAYING_TEMP";
	private static final String MY_BOOKS_TEMP = "MY_BOOKS_TEMP";

	private BookStatsDatabaseManager _db;
	private ArrayList<BookStats> _myBooks;
	private ArrayList<BookStats> _myBooksTemp;
	private ListView _myListView;
	private ArrayList<BookStats> _deletedBooks;
	private boolean _doDBWork;
	private MyListAdapter _myAdapter;
	private Dialog _dialogSearch;
	private boolean _isDisplayingTemp;
	private EditText _nameSearch;

	private void addBook() {
		_doDBWork = false;
		final Intent intent = new Intent(this, BookActivity.class);
		intent.putExtra(UPDATE_BOOK, false);
		startActivityForResult(intent, BOOK_REQUEST_CODE);
	}

	private void deleteBook(final long id) {
		new AlertDialog.Builder(MainActivity.this)
				.setTitle("Delete this book?")
				.setMessage(
						_myBooks.get((int) (_isDisplayingTemp ? Utils
								.getRightID(_myBooks, _myBooksTemp, id) : id))._name)
				.setPositiveButton("Yes", new OnClickListener() {
					@Override
					public void onClick(final DialogInterface arg0,
							final int arg1) {
						if (_isDisplayingTemp) {
							final long rightID = Utils.getRightID(_myBooks,
									_myBooksTemp, id);
							if (_myBooks.get((int) rightID)._id != -1) {
								_deletedBooks.add(_myBooks.get((int) rightID));
							}
							_myBooksTemp.remove((int) id);
							_myBooks.remove((int) rightID);
						} else {
							if (_myBooks.get((int) id)._id != -1) {
								_deletedBooks.add(_myBooks.get((int) id));
							}
							_myBooks.remove((int) id);
						}
						_myAdapter.notifyDataSetChanged();
					}
				}).setNegativeButton("No", null).show();
	}

	private void doClearList() {
		_nameSearch.setText("");
		_myAdapter.clearTemp();
		/*
		 * _myAdapter = new MyListAdapter(MainActivity.this, _myBooks, null,
		 * MyListAdapter.MAIN); _myListView.setAdapter(_myAdapter);
		 */
		_myAdapter.notifyDataSetChanged();
		_isDisplayingTemp = false;
	}

	private void doDBOperations() {
		boolean updated = false;
		for (int i = 0; i < _myBooks.size(); i++) {
			if (_myBooks.get(i)._id == -1) {
				_db.addBookRow(_myBooks.get(i));
				_myBooks.get(i)._needsUpdate = false;
				updated = true;
			}
			if (_myBooks.get(i)._deletedSessions.size() > 0) {
				for (int k = 0; k < _myBooks.get(i)._deletedSessions.size(); k++) {
					_db.deleteSession(_myBooks.get(i)._deletedSessions.get(k));
					updated = true;
				}
			}
			if (_myBooks.get(i)._needsUpdate) {
				_db.updateBookRow(_myBooks.get(i));
				for (int k = 0; k < _myBooks.get(i)._sessions.size(); k++) {
					_db.updateSession(_myBooks.get(i)._sessions.get(k));
					updated = true;
				}
				updated = true;
			}
			if (_myBooks.get(i)._hasNewSessions) {
				_db.addSessions(_myBooks.get(i));
				updated = true;
			}
		}
		if (_deletedBooks.size() > 0) {
			for (int j = 0; j < _deletedBooks.size(); j++) {
				_db.deleteRow((int) _deletedBooks.get(j)._id);
				updated = true;
			}
		}
		if (updated) {
			final Toast toast = Toast.makeText(this,
					"Books database updated successfully", Toast.LENGTH_SHORT);
			toast.show();
		}
	}

	private void exportDB() {
		try {
			File sd = Environment.getExternalStorageDirectory();

			if (sd.canWrite()) {

				sd = new File(Environment.getExternalStorageDirectory()
						.getAbsolutePath() + "/BookReadingStats");
				if (!sd.exists()) {
					sd.mkdirs();
				}

				final File currentDB = new File(getDatabasePath(
						BookStatsDatabaseManager.getName()).getAbsolutePath());
				final File backupDB = new File(sd,
						BookStatsDatabaseManager.getName());

				if (currentDB.exists()) {
					final MediaScannerHelper msh = new MediaScannerHelper();

					final FileChannel src = new FileInputStream(currentDB)
							.getChannel();
					final FileChannel dst = new FileOutputStream(backupDB)
							.getChannel();
					dst.transferFrom(src, 0, src.size());
					msh.addFile(this, backupDB.getAbsolutePath());
					src.close();
					dst.close();
				}

			}
		} catch (final Exception e) {
			Log.w("Settings Backup", e);
		}

	}

	private void finishSetup() {
		if (_isDisplayingTemp) {
			_myAdapter = new MyListAdapter(MainActivity.this, _myBooksTemp,
					null, MyListAdapter.MAIN);
			_myListView.setAdapter(_myAdapter);
		} else {
			_myAdapter = new MyListAdapter(MainActivity.this, _myBooks, null,
					MyListAdapter.MAIN);
			_myListView.setAdapter(_myAdapter);
		}
		_myAdapter.notifyDataSetChanged();
		setDialogSearch();
	}

	private void importDB() {
		_doDBWork = false;
		final Intent intent = new Intent(MainActivity.this,
				ImportActivity.class);
		startActivityForResult(intent, IMPORT_CODE);
	}

	private void launchTotalsActivity() {
		_doDBWork = false;
		final Intent intent = new Intent(this, TotalsActivity.class);
		intent.putParcelableArrayListExtra(BOOK_LIST, _myBooks);
		startActivityForResult(intent, 0);

	}

	@Override
	protected void onActivityResult(final int requestCode,
			final int resultCode, final Intent data) {
		boolean doWork = true;
		if ((requestCode == BOOK_REQUEST_CODE)
				&& (resultCode == Activity.RESULT_OK)) {
			final long id = data.getExtras().getLong(BOOK_ID);
			final BookStats bs = (BookStats) data
					.getParcelableExtra(BookStats._CLASS);
			if (data.getExtras().getBoolean(UPDATE_BOOK)) {

				if (_isDisplayingTemp) {
					final long myID = Utils.getRightID(_myBooksTemp, _myBooks,
							id);
					_myBooksTemp.set((int) myID, bs);
				}
				_myBooks.set((int) id, bs);
			} else {
				_myBooks.add(bs);
			}
			_myAdapter.notifyDataSetChanged();
		} else if ((requestCode == IMPORT_CODE)
				&& (resultCode == Activity.RESULT_OK)) {
			doWork = false;
			final Intent intent = new Intent(this, MainActivity.class);
			startActivity(intent);
			finish();
		}
		if (doWork) {
			_doDBWork = true;
		}
	}

	@Override
	public void onBackPressed() {
		if (this._isDisplayingTemp) {
			doClearList();
		} else {
			super.onBackPressed();
		}
	}

	@Override
	public boolean onContextItemSelected(final MenuItem item) {

		final AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item
				.getMenuInfo();
		switch (item.getItemId()) {
			case R.id.add_ses :
				if (_isDisplayingTemp) {
					updateFromContext(Utils.getRightID(_myBooks, _myBooksTemp,
							menuInfo.id));
				} else {
					updateFromContext(menuInfo.id);
				}
				return true;
			case R.id.delete_book :
				deleteBook(menuInfo.id);
				return true;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		setTitle(getString(R.string.title_activity_book_list));
		_db = new BookStatsDatabaseManager(this,
				BookStatsDatabaseManager.getName());
		_myListView = (ListView) findViewById(R.id.listViewBooks);
		_myListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(final AdapterView<?> parent,
					final View view, final int position, final long id) {
				if (_isDisplayingTemp) {
					updateBook(Utils.getRightID(_myBooks, _myBooksTemp, id),
							false);
				} else {
					updateBook(id, false);
				}
			}
		});
		_myListView.setEmptyView(findViewById(R.id.empty_list_view));
		_doDBWork = true;
		_isDisplayingTemp = false;
		registerForContextMenu(_myListView);
		if (savedInstanceState != null) {
			_myBooks = savedInstanceState.getParcelableArrayList(MY_BOOKS);
			_myBooksTemp = savedInstanceState
					.getParcelableArrayList(MY_BOOKS_TEMP);
			_isDisplayingTemp = savedInstanceState
					.getBoolean(IS_DIPLAYING_TEMP);
			_deletedBooks = savedInstanceState
					.getParcelableArrayList(MY_DELETED_BOOKS);
			finishSetup();
		} else {
			_deletedBooks = new ArrayList<BookStats>();
			new AscyncThread("Loading...").execute();

		}

	}

	@Override
	public void onCreateContextMenu(final ContextMenu menu, final View v,
			final ContextMenuInfo menuInfo) {
		// Inflate the menu; this adds items to the action bar if it is present.
		super.onCreateContextMenu(menu, v, menuInfo);
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		if (this._isDisplayingTemp) {
			for (int i = 0; i < _myBooks.size(); i++) {
				if (_myBooks.get(i)._id == _myBooksTemp.get((int) info.id)._id) {
					menu.setHeaderTitle(_myBooks.get(i)._name);
				}
			}
		} else {
			menu.setHeaderTitle(_myBooks.get((int) info.id)._name);
		}
		getMenuInflater().inflate(R.menu.activity_main_context, menu);

	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	protected void onDestroy() {
		_db.close();
		_myListView.setAdapter(null);
		super.onDestroy();
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
			case R.id.add_book :
				addBook();
				return true;
			case R.id.close :
				finish();
				return true;
			case R.id.totals :
				launchTotalsActivity();
				return true;
			case R.id.export :
				new AscyncThreadEx("Exporting...").execute();
				return true;
			case R.id.myimport :
				importDB();
				return true;

		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onSaveInstanceState(final Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putParcelableArrayList(MY_BOOKS, _myBooks);
		savedInstanceState.putParcelableArrayList(MY_BOOKS_TEMP, _myBooksTemp);
		savedInstanceState.putParcelableArrayList(MY_DELETED_BOOKS,
				_deletedBooks);
		savedInstanceState.putBoolean(IS_DIPLAYING_TEMP, _isDisplayingTemp);

	}

	@Override
	protected void onStop() {
		if (_doDBWork) {
			doDBOperations();
		}
		super.onStop();
	}

	private void searchBook(final String s, final ArrayList<BookStats> tempList) {
		for (int i = 0; i < _myBooks.size(); i++) {
			if (_myBooks.get(i)._name.toLowerCase(Locale.getDefault())
					.contains(s.toLowerCase(Locale.getDefault()))) {
				tempList.add(_myBooks.get(i));
			}
		}

	}

	private void setDialogSearch() {
		if (_dialogSearch == null) {
			_dialogSearch = new Dialog(this);
			_dialogSearch.setContentView(R.layout.dialog_search);
			_dialogSearch.setTitle("Search Book");
			final Button done = (Button) _dialogSearch
					.findViewById(R.id.buttonSearchDone);
			final Button clear = (Button) _dialogSearch
					.findViewById(R.id.buttonClear);
			_nameSearch = (EditText) _dialogSearch
					.findViewById(R.id.editTextName);;

			done.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(final View v) {
					if (!_nameSearch.getText().toString().isEmpty()) {
						new AscyncThreadSearch(_nameSearch.getText().toString())
								.execute();
					}
					_dialogSearch.dismiss();

				}
			});

			clear.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(final View v) {
					doClearList();
				}
			});
		}
	}

	public void showSearchBook(final View v) {
		_dialogSearch.show();
	}

	private void updateBook(final long id, final boolean add) {
		final Intent intent = new Intent(MainActivity.this, BookActivity.class);
		intent.putExtra(UPDATE_BOOK, true);
		intent.putExtra(BOOK_ID, id);
		intent.putExtra(ADD_SESSION, add);
		intent.putExtra(BookStats._CLASS, _myBooks.get((int) id));
		startActivityForResult(intent, BOOK_REQUEST_CODE);
		_doDBWork = false;

	}

	private void updateFromContext(final long index) {
		if (!_myBooks.get((int) index).isRead()) {
			updateBook(index, true);
		} else {
			new AlertDialog.Builder(MainActivity.this)
					.setTitle("This book is done")
					.setMessage(_myBooks.get((int) index)._name)
					.setPositiveButton("OK", null).show();
		}
	}

}
