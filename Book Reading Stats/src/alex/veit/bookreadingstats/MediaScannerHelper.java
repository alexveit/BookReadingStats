package alex.veit.bookreadingstats;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;

public class MediaScannerHelper implements MediaScannerConnectionClient {

	public void addFile(final Context context, final String filename) {
		final String[] paths = new String[1];
		paths[0] = filename;
		MediaScannerConnection.scanFile(context, paths, null, this);
	}

	@Override
	public void onMediaScannerConnected() {

	}

	@Override
	public void onScanCompleted(final String path, final Uri uri) {

	}

}
