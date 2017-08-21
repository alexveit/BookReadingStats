package com.veit.alex.est.internetLibs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.speech.RecognizerIntent;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;


/**
 * A helper class for speech recognition
 */

//got this from internet
public class SpeechRecognitionHelper {

    /**
     * Running the recognition process. Checks availability of recognition Activity,
     * If Activity is absent, send user to Google Play to install Google Voice Search.
     * If Activity is available, send Intent for running.
     *
     * @param callingActivity = Activity, that initializing recognition process
     */
    public static void run(final Activity callingActivity, final int code, String sentence) {
        // check if there is recognition Activity
        if (isSpeechRecognitionActivityPresented(callingActivity) == true) {
            // if yes – running recognition
            startRecognitionActivity(callingActivity, code, sentence);
        } else {
            // if no, then showing notification to install Voice Search
            Toast.makeText(callingActivity, "In order to activate speech recognition you must install \"Google Voice Search\"", Toast.LENGTH_LONG).show();
            // start installing process
            installGoogleVoiceSearch(callingActivity);
        }
    }

    /**
     * Checks availability of speech recognizing Activity
     *
     * @param callerActivity – Activity that called the checking
     * @return true – if Activity there available, false – if Activity is absent
     */
    private static boolean isSpeechRecognitionActivityPresented(final Activity callerActivity) {
        try {
            // getting an instance of package manager
            PackageManager pm = callerActivity.getPackageManager();
            // a list of activities, which can process speech recognition Intent
            List activities = pm.queryIntentActivities(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);


            if (activities.size() != 0) {    // if list not empty
                return true;                // then we can recognize the speech
            }
        } catch (Exception e) {

        }

        return false; // we have no activities to recognize the speech
    }

    /**
     * Send an Intent with request on speech
     * @param callerActivity  - Activity, that initiated a request
     */
    private static void startRecognitionActivity(final Activity callerActivity, final int code, String sentence) {

        // creating an Intent with “RecognizerIntent.ACTION_RECOGNIZE_SPEECH” action
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        int resultCount = 10;
        // giving additional parameters:
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, sentence);    // user hint
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);    // setting recognition model, optimized for natural talk
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, resultCount);    // quantity of results we want to receive
        intent.putExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES, resultCount);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString());

        // start Activity ant waiting the result
        callerActivity.startActivityForResult(intent, code);
    }

    /**
     * Asking the permission for installing Google Voice Search.
     * If permission granted – sent user to Google Play
     * param ownerActivity – Activity, that initialized installing
     */
    private static void installGoogleVoiceSearch(final Activity ownerActivity) {
        // creating a dialog asking user if he want
        // to install the Voice Search
        Dialog dialog = new AlertDialog.Builder(ownerActivity)
                .setMessage("For recognition it’s necessary to install \"Google Voice Search\"")    // dialog message
                .setTitle("Install Voice Search from Google Play?")    // dialog header
                .setPositiveButton("Install", new DialogInterface.OnClickListener() {    // confirm button
                    // Install Button click handler
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            // creating an Intent for opening applications page in Google Play
                            // Voice Search package name: com.google.android.voicesearch
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.voicesearch"));
                            // setting flags to avoid going in application history (Activity call stack)
                            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
                            // sending an Intent
                            ownerActivity.startActivity(intent);
                        } catch (Exception ex) {
                            // if something going wrong
                            // doing nothing
                            Toast.makeText(ownerActivity, "Something is wrong :(", Toast.LENGTH_LONG);
                        }
                    }})
                .setNegativeButton("Cancel", null)    // cancel button
                .create();

        dialog.show();    // showing dialog
    }

}