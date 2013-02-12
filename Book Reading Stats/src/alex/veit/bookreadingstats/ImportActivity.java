package alex.veit.bookreadingstats;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class ImportActivity extends ListActivity {

	private class AscyncThreadIn extends AsyncTask<Void, Void, Void> {

		ProgressDialog _progress;
		String _message = "";
		File _file;

		public AscyncThreadIn(final String msg, final File file) {
			_message = msg;
			_file = file;
		}

		@Override
		protected Void doInBackground(final Void... params) {
			importDB(_file);
			return null;
		}

		@Override
		protected void onPostExecute(final Void result) {
			super.onPostExecute(result);
			_progress.dismiss();
			new AlertDialog.Builder(ImportActivity.this)
					.setTitle(getString(R.string.success))
					.setMessage("Database imported")
					.setPositiveButton("OK", new OnClickListener() {

						@Override
						public void onClick(final DialogInterface dialog,
								final int which) {
							setResult(Activity.RESULT_OK, null);
							finish();

						}
					}).show();
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			_progress = ProgressDialog.show(ImportActivity.this, "Books DB",
					_message, true);
		}

	}

	private List<String> item = null;
	private List<String> path = null;
	private String root;
	private TextView myPath;

	private void getDir(final String dirPath) {
		myPath.setText("Location: " + dirPath);
		item = new ArrayList<String>();
		path = new ArrayList<String>();
		final File f = new File(dirPath);
		final File[] files = f.listFiles();

		if (!dirPath.equals(root)) {
			item.add(root);
			path.add(root);
			item.add("../");
			path.add(f.getParent());
		}

		for (final File file2 : files) {
			final File file = file2;

			if (!file.isHidden() && file.canRead()) {
				path.add(file.getPath());
				if (file.isDirectory()) {
					item.add(file.getName() + "/");
				} else {
					item.add(file.getName());
				}
			}
		}

		final ArrayAdapter<String> fileList = new ArrayAdapter<String>(this,
				R.layout.row, item);
		setListAdapter(fileList);
	}

	private void importDB(final File file) {
		try {

			final File currentDB = new File(getDatabasePath(
					BookStatsDatabaseManager.getName()).getAbsolutePath());

			if (currentDB.exists()) {
				final FileChannel src = new FileInputStream(file).getChannel();
				final FileChannel dst = new FileOutputStream(currentDB)
						.getChannel();
				dst.transferFrom(src, 0, src.size());
				src.close();
				dst.close();
			}

		} catch (final Exception e) {

		}
	}

	private boolean isValidFile(final File file) {
		if (file.getName().compareTo(BookStatsDatabaseManager.getName()) == 0) {
			return true;
		}
		return false;
	}

	@Override
	public void onBackPressed() {
		if (!(path.get(0) == root)) {
			setResult(Activity.RESULT_CANCELED, null);
			super.onBackPressed();
		}
		getDir(path.get(1));
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_import);
		myPath = (TextView) findViewById(R.id.path);

		root = Environment.getExternalStorageDirectory().getPath();

		getDir(root);
	}

	@Override
	protected void onListItemClick(final ListView l, final View v,
			final int position, final long id) {
		// TODO Auto-generated method stub
		final File file = new File(path.get(position));

		if (file.isDirectory()) {
			if (file.canRead()) {
				getDir(path.get(position));
			} else {
				new AlertDialog.Builder(this)
						.setTitle(
								"[" + file.getName()
										+ "] folder can't be read!")
						.setPositiveButton("OK", null).show();
			}
		} else {
			new AlertDialog.Builder(this)
					.setTitle("Import this file?")
					.setMessage(
							file.getName()
									+ "\nNote: Importing a new database will completely replace the existing one. Do you want to proceed?")
					.setPositiveButton("Yes", new OnClickListener() {

						@Override
						public void onClick(final DialogInterface dialog,
								final int which) {
							if (isValidFile(file)) {
								new AscyncThreadIn("Inporting...", file)
										.execute();
							} else {
								new AlertDialog.Builder(ImportActivity.this)
										.setTitle(getString(R.string.alert))
										.setMessage("Invalid file")
										.setPositiveButton("OK", null).show();
							}

						}
					}).setNegativeButton("No", null).show();

		}
	}

}
